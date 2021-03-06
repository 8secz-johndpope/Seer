 package org.apache.accumulo.accismus;
 
 import java.io.UnsupportedEncodingException;
 import java.util.ArrayList;
 import java.util.Collections;
 import java.util.HashMap;
 import java.util.HashSet;
 import java.util.Iterator;
 import java.util.Map;
 import java.util.Map.Entry;
 import java.util.Set;
 
 import org.apache.accumulo.accismus.impl.ByteUtil;
 import org.apache.accumulo.accismus.impl.ColumnUtil;
 import org.apache.accumulo.accismus.impl.DelLockValue;
 import org.apache.accumulo.accismus.impl.LockValue;
 import org.apache.accumulo.accismus.impl.OracleClient;
 import org.apache.accumulo.accismus.impl.SnapshotScanner;
 import org.apache.accumulo.accismus.impl.TxStatus;
 import org.apache.accumulo.accismus.iterators.PrewriteIterator;
 import org.apache.accumulo.core.client.AccumuloException;
 import org.apache.accumulo.core.client.AccumuloSecurityException;
 import org.apache.accumulo.core.client.BatchWriter;
 import org.apache.accumulo.core.client.BatchWriterConfig;
 import org.apache.accumulo.core.client.ConditionalWriter;
 import org.apache.accumulo.core.client.ConditionalWriter.Result;
 import org.apache.accumulo.core.client.ConditionalWriter.Status;
 import org.apache.accumulo.core.client.Connector;
 import org.apache.accumulo.core.client.IteratorSetting;
 import org.apache.accumulo.core.client.MutationsRejectedException;
 import org.apache.accumulo.core.client.TableNotFoundException;
 import org.apache.accumulo.core.data.ArrayByteSequence;
 import org.apache.accumulo.core.data.ByteSequence;
 import org.apache.accumulo.core.data.Condition;
 import org.apache.accumulo.core.data.ConditionalMutation;
 import org.apache.accumulo.core.data.Mutation;
 import org.apache.accumulo.core.data.Range;
 import org.apache.accumulo.core.util.ArgumentChecker;
 import org.apache.commons.lang.mutable.MutableLong;
 
 
 public class Transaction {
   
   public static final byte[] EMPTY = new byte[0];
   public static final ByteSequence EMPTY_BS = new ArrayByteSequence(EMPTY);
   
   private static final ByteSequence DELETE = new ArrayByteSequence("special delete object");
   
   private long startTs;
   private Connector conn;
   private String table;
   
   private Map<ByteSequence,Map<Column,ByteSequence>> updates;
   private ByteSequence observer;
   private ByteSequence triggerRow;
   private Column triggerColumn;
   private Set<Column> observedColumns;
   private boolean commitStarted = false;
   private Configuration config;
   
   public static byte[] toBytes(String s) {
     try {
       return s.getBytes("UTF-8");
     } catch (UnsupportedEncodingException e) {
       throw new RuntimeException(e);
     }
   }
 
   public static byte[] concat(Column c) {
     return ByteUtil.concat(c.getFamily(), c.getQualifier());
   }
 
   Transaction(Configuration config, ByteSequence triggerRow, Column tiggerColumn) throws Exception {
     this.config = config;
     this.table = config.getTable();
     this.conn = config.getConnector();
     this.observedColumns = config.getObservers().keySet();
     this.updates = new HashMap<ByteSequence,Map<Column,ByteSequence>>();
     
     this.triggerRow = triggerRow;
     this.triggerColumn = tiggerColumn;
     
     this.startTs = OracleClient.getInstance(config).getTimestamp();
     
     if (triggerRow != null) {
       Map<Column,ByteSequence> colUpdates = new HashMap<Column,ByteSequence>();
       colUpdates.put(tiggerColumn, null);
       updates.put(triggerRow, colUpdates);
       observer = new ArrayByteSequence("oid");
     }
   }
   
   public Transaction(Configuration config) throws Exception {
     this(config, null, null);
   }
   
 
   public ByteSequence get(String row, Column column) throws Exception {
     return get(new ArrayByteSequence(toBytes(row)), column);
   }
   
   public ByteSequence get(byte[] row, Column column) throws Exception {
     return get(new ArrayByteSequence(row), column);
   }
 
   public ByteSequence get(ByteSequence row, Column column) throws Exception {
     // TODO cache? precache?
     return get(row, Collections.singleton(column)).get(column);
   }
   
   public Map<Column,ByteSequence> get(String row, Set<Column> columns) throws Exception {
     return get(new ArrayByteSequence(toBytes(row)), columns);
   }
   
   public Map<Column,ByteSequence> get(byte[] row, Set<Column> columns) throws Exception {
     return get(new ArrayByteSequence(row), columns);
   }
 
   public Map<Column,ByteSequence> get(ByteSequence row, Set<Column> columns) throws Exception {
     // TODO push visibility filtering to server side?
 
     ScannerConfiguration config = new ScannerConfiguration();
     config.setRange(new Range(ByteUtil.toText(row)));
     for (Column column : columns) {
       config.fetchColumn(column.getFamily(), column.getQualifier());
     }
     
     RowIterator iter = get(config);
     
     Map<Column,ByteSequence> ret = new HashMap<Column,ByteSequence>();
 
     while (iter.hasNext()) {
       Entry<ByteSequence,ColumnIterator> entry = iter.next();
       ColumnIterator citer = entry.getValue();
       while (citer.hasNext()) {
         Entry<Column,ByteSequence> centry = citer.next();
         if (columns.contains(centry.getKey())) {
           ret.put(centry.getKey(), centry.getValue());
         }
       }
     }
     
     return ret;
   }
 
   // TODO add a get that uses the batch scanner
 
   public RowIterator get(ScannerConfiguration config) throws Exception {
     if (commitStarted)
       throw new IllegalStateException("transaction committed");
 
     return new RowIterator(new SnapshotScanner(this.config, config, startTs));
   }
   
   public void set(String row, Column col, String value) {
     ArgumentChecker.notNull(row, col, value);
     set(new ArrayByteSequence(toBytes(row)), col, new ArrayByteSequence(toBytes(value)));
   }
   
   public void set(byte[] row, Column col, byte[] value) {
     ArgumentChecker.notNull(row, col, value);
     set(new ArrayByteSequence(row), col, new ArrayByteSequence(value));
   }
   
   public void set(ByteSequence row, Column col, ByteSequence value) {
     if (commitStarted)
       throw new IllegalStateException("transaction committed");
 
     ArgumentChecker.notNull(row, col, value);
     
     if (col.getFamily().equals(Constants.NOTIFY_CF)) {
       throw new IllegalArgumentException(Constants.NOTIFY_CF + " is a reserved family");
     }
 
     // TODO copy?
 
     Map<Column,ByteSequence> colUpdates = updates.get(row);
     if (colUpdates == null) {
       colUpdates = new HashMap<Column,ByteSequence>();
       updates.put(row, colUpdates);
     }
     
     if (colUpdates.get(col) != null) {
       throw new IllegalStateException("Value already set " + row + " " + col);
     }
     colUpdates.put(col, value);
   }
   
   public void delete(String row, Column col) {
     ArgumentChecker.notNull(row, col);
     set(new ArrayByteSequence(toBytes(row)), col, DELETE);
   }
   
   public void delete(byte[] row, Column col) {
     ArgumentChecker.notNull(row, col);
     set(new ArrayByteSequence(row), col, DELETE);
   }
   
   public void delete(ByteSequence row, Column col) {
     ArgumentChecker.notNull(row, col);
     set(row, col, DELETE);
   }
   
   private ConditionalMutation prewrite(ConditionalMutation cm, ByteSequence row, Column col, ByteSequence val, ByteSequence primaryRow, Column primaryColumn,
       boolean isTriggerRow) {
     IteratorSetting iterConf = new IteratorSetting(10, PrewriteIterator.class);
     PrewriteIterator.setSnaptime(iterConf, startTs);
     boolean isTrigger = isTriggerRow && col.equals(triggerColumn);
     if (isTrigger)
       PrewriteIterator.enableAckCheck(iterConf);
     
     Condition cond = new Condition(col.getFamily(), col.getQualifier()).setIterators(iterConf).setVisibility(col.getVisibility());
     
     if (cm == null)
       cm = new ConditionalMutation(row, cond);
     else
       cm.addCondition(cond);
     
     if (val != null && val != DELETE)
       cm.put(col.getFamily().toArray(), col.getQualifier().toArray(), col.getVisibility(), ColumnUtil.DATA_PREFIX | startTs, val.toArray());
     
     cm.put(col.getFamily().toArray(), col.getQualifier().toArray(), col.getVisibility(), ColumnUtil.LOCK_PREFIX | startTs,
         LockValue.encode(primaryRow, primaryColumn, val != null, isTrigger ? observer : EMPTY_BS));
     
     return cm;
   }
   
   private ConditionalMutation prewrite(ByteSequence row, Column col, ByteSequence val, ByteSequence primaryRow, Column primaryColumn, boolean isTriggerRow) {
     return prewrite(null, row, col, val, primaryRow, primaryColumn, isTriggerRow);
   }
 
   private void prewrite(ConditionalMutation cm, Column col, ByteSequence val, ByteSequence primaryRow, Column primaryColumn, boolean isTriggerRow) {
     prewrite(cm, null, col, val, primaryRow, primaryColumn, isTriggerRow);
   }
 
 
   static class CommitData {
     ConditionalWriter cw;
     private ByteSequence prow;
     private Column pcol;
     private ByteSequence pval;
     private int rejectedCount;
     private HashSet<ByteSequence> acceptedRows;
     
     public String toString() {
       return prow + " " + pcol + " " + pval + " " + rejectedCount;
     }
 
   }
 
   boolean preCommit(CommitData cd) throws TableNotFoundException, AccumuloException, AccumuloSecurityException {
     if (commitStarted)
       throw new IllegalStateException();
 
     commitStarted = true;
 
     // get a primary column
     cd.prow = updates.keySet().iterator().next();
     Map<Column,ByteSequence> colSet = updates.get(cd.prow);
     cd.pcol = colSet.keySet().iterator().next();
     cd.pval = colSet.remove(cd.pcol);
     if (colSet.size() == 0)
       updates.remove(cd.prow);
     
     // try to lock primary column
     ConditionalMutation pcm = prewrite(cd.prow, cd.pcol, cd.pval, cd.prow, cd.pcol, cd.prow.equals(triggerRow));
     
     Status mutationStatus = cd.cw.write(pcm).getStatus();
     
     while (mutationStatus == Status.UNKNOWN) {
       
       MutableLong mcts = new MutableLong(-1);
       TxStatus txStatus = TxStatus.getTransactionStatus(config, cd.prow, cd.pcol, startTs, mcts);
       
       switch (txStatus) {
         case LOCKED:
           mutationStatus = Status.ACCEPTED;
           break;
         case ROLLED_BACK:
           mutationStatus = Status.REJECTED;
           break;
         case UNKNOWN:
           mutationStatus = cd.cw.write(pcm).getStatus();
           break;
         case COMMITTED:
         default:
           throw new IllegalStateException("unexpected tx state " + txStatus + " " + cd.prow + " " + cd.pcol);
           
       }
     }
     
     if (mutationStatus != Status.ACCEPTED) {
       return false;
     }
     
     // try to lock other columns
     ArrayList<ConditionalMutation> mutations = new ArrayList<ConditionalMutation>();
     
     for (Entry<ByteSequence,Map<Column,ByteSequence>> rowUpdates : updates.entrySet()) {
       ConditionalMutation cm = null;
       boolean isTriggerRow = rowUpdates.getKey().equals(triggerRow);
       
       for (Entry<Column,ByteSequence> colUpdates : rowUpdates.getValue().entrySet()) {
         if (cm == null)
           cm = prewrite(rowUpdates.getKey(), colUpdates.getKey(), colUpdates.getValue(), cd.prow, cd.pcol, isTriggerRow);
         else
           prewrite(cm, colUpdates.getKey(), colUpdates.getValue(), cd.prow, cd.pcol, isTriggerRow);
       }
       
       mutations.add(cm);
     }
     
     cd.acceptedRows = new HashSet<ByteSequence>();
     cd.rejectedCount = 0;
     
     Iterator<Result> resultsIter = cd.cw.write(mutations.iterator());
     while (resultsIter.hasNext()) {
       Result result = resultsIter.next();
       if (result.getStatus() == Status.ACCEPTED)
         cd.acceptedRows.add(new ArrayByteSequence(result.getMutation().getRow()));
       else
         cd.rejectedCount++;
     }
     return true;
   }
 
   boolean commitPrimaryColumn(CommitData cd, long commitTs) throws AccumuloException, AccumuloSecurityException {
     // try to delete lock and add write for primary column
     IteratorSetting iterConf = new IteratorSetting(10, PrewriteIterator.class);
     PrewriteIterator.setSnaptime(iterConf, startTs);
     boolean isTrigger = cd.prow.equals(triggerRow) && cd.pcol.equals(triggerColumn);
     Condition lockCheck = new Condition(cd.pcol.getFamily(), cd.pcol.getQualifier()).setIterators(iterConf).setVisibility(cd.pcol.getVisibility())
         .setValue(LockValue.encode(cd.prow, cd.pcol, cd.pval != null, isTrigger ? observer : EMPTY_BS));
     ConditionalMutation delLockMutation = new ConditionalMutation(cd.prow, lockCheck);
     ColumnUtil.commitColumn(isTrigger, true, cd.pcol, cd.pval != null, startTs, commitTs, observedColumns, delLockMutation);
     
     Status mutationStatus = cd.cw.write(delLockMutation).getStatus();
     
     while (mutationStatus == Status.UNKNOWN) {
       
       MutableLong mcts = new MutableLong(-1);
       TxStatus txStatus = TxStatus.getTransactionStatus(config, cd.prow, cd.pcol, startTs, mcts);
       
       switch (txStatus) {
         case COMMITTED:
           if (mcts.longValue() != commitTs)
             throw new IllegalStateException(cd.prow + " " + cd.pcol + " " + mcts.longValue() + "!=" + commitTs);
           mutationStatus = Status.ACCEPTED;
           break;
         case LOCKED:
           mutationStatus = cd.cw.write(delLockMutation).getStatus();
           break;
         default:
           mutationStatus = Status.REJECTED;
       }
     }
 
     if (mutationStatus != Status.ACCEPTED) {
       return false;
     }
     
     return true;
   }
   
   boolean rollback(CommitData cd) throws TableNotFoundException, MutationsRejectedException {
     // roll back locks
     
     // TODO let rollback be done lazily? this makes GC more difficult
     
     BatchWriter bw = conn.createBatchWriter(table, new BatchWriterConfig());
     
     Mutation m;
 
     for (ByteSequence row : cd.acceptedRows) {
       m = new Mutation(row.toArray());
       for (Column col : updates.get(row).keySet()) {
         m.put(col.getFamily().toArray(), col.getQualifier().toArray(), col.getVisibility(), ColumnUtil.DEL_LOCK_PREFIX | startTs,
             DelLockValue.encode(startTs, false, true));
       }
       bw.addMutation(m);
     }
     
     bw.flush();
     
     // mark transaction as complete for garbage collection purposes
     m = new Mutation(cd.prow.toArray());
     // TODO timestamp?
     // TODO writing the primary column with a batch writer is iffy
     m.put(cd.pcol.getFamily().toArray(), cd.pcol.getQualifier().toArray(), cd.pcol.getVisibility(), ColumnUtil.DEL_LOCK_PREFIX | startTs,
         DelLockValue.encode(startTs, false, true));
     m.put(cd.pcol.getFamily().toArray(), cd.pcol.getQualifier().toArray(), cd.pcol.getVisibility(), ColumnUtil.TX_DONE_PREFIX | startTs, EMPTY);
     bw.addMutation(m);
 
     bw.close();
     
     return false;
   }
   
   boolean finishCommit(CommitData cd, long commitTs) throws TableNotFoundException, MutationsRejectedException {
     // delete locks and add writes for other columns
     // TODO use shared batch writer
     BatchWriter bw = conn.createBatchWriter(table, new BatchWriterConfig());
     for (Entry<ByteSequence,Map<Column,ByteSequence>> rowUpdates : updates.entrySet()) {
       Mutation m = new Mutation(rowUpdates.getKey().toArray());
       boolean isTriggerRow = rowUpdates.getKey().equals(triggerRow);
       for (Entry<Column,ByteSequence> colUpdates : rowUpdates.getValue().entrySet()) {
         ColumnUtil.commitColumn(isTriggerRow && colUpdates.getKey().equals(triggerColumn), false, colUpdates.getKey(), colUpdates.getValue() != null, startTs,
             commitTs, observedColumns, m);
       }
       
       bw.addMutation(m);
     }
     
     bw.flush();
     
     // mark transaction as complete for garbage collection purposes
     Mutation m = new Mutation(cd.prow.toArray());
     m.put(cd.pcol.getFamily().toArray(), cd.pcol.getQualifier().toArray(), cd.pcol.getVisibility(), ColumnUtil.TX_DONE_PREFIX | commitTs, EMPTY);
     bw.addMutation(m);
 
     bw.close();
     
     return true;
   }
 
   CommitData createCommitData() throws TableNotFoundException {
     CommitData cd = new CommitData();
     // TODO use shared writer
     cd.cw = config.createConditionalWriter();
     return cd;
   }
 
   public boolean commit() throws Exception {
     // TODO can optimize a tx that modifies a single row, can be done with a single conditional mutation
     // TODO throw exception instead of return boolean
     // TODO synchronize or detect concurrent use
     CommitData cd = createCommitData();
     
     try {
       if (!preCommit(cd))
         return false;
       if (cd.rejectedCount == 0) {
         long commitTs = OracleClient.getInstance(config).getTimestamp();
         if (commitPrimaryColumn(cd, commitTs)) {
           return finishCommit(cd, commitTs);
         } else {
           // TODO write TX_DONE
           return false;
         }
       } else {
         return rollback(cd);
       }
     } finally {
       cd.cw.close();
     }
   }
 
   long getStartTs() {
     return startTs;
   }
 }
