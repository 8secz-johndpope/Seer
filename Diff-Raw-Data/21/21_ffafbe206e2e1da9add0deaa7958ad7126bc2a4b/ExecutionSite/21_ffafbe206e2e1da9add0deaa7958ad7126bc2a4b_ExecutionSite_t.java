 /* This file is part of VoltDB.
  * Copyright (C) 2008-2010 VoltDB Inc.
  *
  * VoltDB is free software: you can redistribute it and/or modify
  * it under the terms of the GNU General Public License as published by
  * the Free Software Foundation, either version 3 of the License, or
  * (at your option) any later version.
  *
  * VoltDB is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  * GNU General Public License for more details.
  *
  * You should have received a copy of the GNU General Public License
  * along with VoltDB.  If not, see <http://www.gnu.org/licenses/>.
  */
 
 package org.voltdb;
 
 import java.io.IOException;
 import java.nio.ByteBuffer;
 import java.util.*;
 import java.util.concurrent.Semaphore;
 import java.util.concurrent.atomic.AtomicInteger;
 import java.util.concurrent.atomic.AtomicLong;
 
 import org.voltdb.RecoverySiteProcessor.MessageHandler;
 import org.voltdb.SnapshotSiteProcessor.SnapshotTableTask;
 import org.voltdb.VoltProcedure.VoltAbortException;
 import org.voltdb.catalog.*;
 import org.voltdb.client.ClientResponse;
 import org.voltdb.client.ConnectionUtil;
 import org.voltdb.debugstate.ExecutorContext;
 import org.voltdb.dtxn.*;
 import org.voltdb.dtxn.RestrictedPriorityQueue.QueueState;
 import org.voltdb.exceptions.*;
 import org.voltdb.export.ExportManager;
 import org.voltdb.export.ExportProtoMessage;
 import org.voltdb.export.processors.RawProcessor;
 import org.voltdb.export.processors.RawProcessor.ExportInternalMessage;
 import org.voltdb.fault.*;
 import org.voltdb.fault.FaultDistributorInterface.PPDPolicyDecision;
 import org.voltdb.fault.VoltFault.FaultType;
 import org.voltdb.jni.*;
 import org.voltdb.logging.Level;
 import org.voltdb.logging.VoltLogger;
 import org.voltdb.messaging.*;
 import org.voltdb.utils.*;
 
 /**
  * The main executor of transactional work in the system. Controls running
  * stored procedures and manages the execution engine's running of plan
  * fragments. Interacts with the DTXN system to get work to do. The thread might
  * do other things, but this is where the good stuff happens.
  */
 public class ExecutionSite
 implements Runnable, DumpManager.Dumpable, SiteTransactionConnection, SiteProcedureConnection
 {
     private VoltLogger m_txnlog;
     private VoltLogger m_recoveryLog = new VoltLogger("RECOVERY");
     private static final VoltLogger log = new VoltLogger(ExecutionSite.class.getName());
     private static final VoltLogger hostLog = new VoltLogger("HOST");
     private static final AtomicInteger siteIndexCounter = new AtomicInteger(0);
     static final AtomicInteger recoveringSiteCount = new AtomicInteger(0);
     private final int siteIndex = siteIndexCounter.getAndIncrement();
     private final ExecutionSiteNodeFailureFaultHandler m_faultHandler =
         new ExecutionSiteNodeFailureFaultHandler();
 
     final HashMap<String, VoltProcedure> procs = new HashMap<String, VoltProcedure>(16, (float) .1);
     private final Mailbox m_mailbox;
     final ExecutionEngine ee;
     final HsqlBackend hsql;
     public volatile boolean m_shouldContinue = true;
 
     /*
      * Recover a site at a time to make the interval in which other sites
      * are blocked as small as possible. The permit will be generated once.
      * The permit is only acquired by recovering partitions and not the source
      * partitions.
      */
     private static final Semaphore m_recoveryPermit = new Semaphore(Integer.MAX_VALUE);
 
     private boolean m_recovering = false;
     private boolean m_haveRecoveryPermit = false;
     private long m_recoveryStartTime = 0;
     private static AtomicLong m_recoveryBytesTransferred = new AtomicLong();
 
     // Catalog
     public CatalogContext m_context;
     Site getCatalogSite() {
         return m_context.cluster.getSites().get(Integer.toString(getSiteId()));
     }
 
     final int m_siteId;
     public final int getSiteId() {
         return m_siteId;
     }
 
     HashMap<Long, TransactionState> m_transactionsById = new HashMap<Long, TransactionState>();
     private final RestrictedPriorityQueue m_transactionQueue;
 
     // The time in ms since epoch of the last call to tick()
     long lastTickTime = 0;
     long lastCommittedTxnId = 0;
 
     /*
      * Due to failures we may find out about commited multi-part txns
      * before running the commit fragment. Handle node fault will generate
      * the fragment, but it is possible for a new failure to be detected
      * before the fragment can be run due to the order messages are pulled
      * from subjects. Maintain and send this value when discovering/sending
      * failure data.
      *
      * This value only gets updated on multi-partition transactions that are
      * not read-only.
      */
     long lastKnownGloballyCommitedMultiPartTxnId = 0;
 
     public final static long kInvalidUndoToken = -1L;
     private long latestUndoToken = 0L;
 
     public long getNextUndoToken() {
         return ++latestUndoToken;
     }
 
     // store the id used by the DumpManager to identify this execution site
     public final String m_dumpId;
     public long m_currentDumpTimestamp = 0;
 
     // Each execution site manages snapshot using a SnapshotSiteProcessor
     private final SnapshotSiteProcessor m_snapshotter;
 
     private RecoverySiteProcessor m_recoveryProcessor = null;
 
     // Trigger if shutdown has been run already.
     private boolean haveShutdownAlready;
 
     private final TableStats m_tableStats;
     private final StarvationTracker m_starvationTracker;
     private final Watchdog m_watchdog;
     private class Watchdog extends Thread {
         private volatile boolean m_shouldContinue = true;
         private volatile boolean m_petted = false;
         private final int m_siteIndex;
         private final int m_siteId;
         private Thread m_watchThread = null;
         public Watchdog(final int siteIndex, final int siteId) {
             super(null, null, "ExecutionSite " + siteIndex + " siteId: " + siteId + " watchdog ", 262144);
             m_siteIndex = siteIndex;
             m_siteId = siteId;
         }
 
         public void pet() {
             m_petted = true;
         }
 
         @Override
         public void run() {
             if (m_watchThread == null) {
                 throw new RuntimeException("Use start(Thread watchThread) not Thread.start()");
             }
             try {
                 Thread.sleep(30000);
             } catch (final InterruptedException e) {
                 return;
             }
             while (m_shouldContinue) {
                 try {
                     Thread.sleep(5000);
                 } catch (final InterruptedException e) {
                     return;
                 }
                 if (!m_petted) {
                     final StackTraceElement trace[] = m_watchThread.getStackTrace();
                     final Throwable throwable = new Throwable();
                     throwable.setStackTrace(trace);
                     log.l7dlog( Level.WARN, LogKeys.org_voltdb_ExecutionSite_Watchdog_possibleHang.name(), new Object[]{ m_siteIndex, m_siteId}, throwable);
                 }
                 m_petted = false;
             }
         }
 
         @Override
         public void start() {
             throw new UnsupportedOperationException("Use start(Thread watchThread)");
         }
 
         public void start(final Thread thread) {
             m_watchThread = thread;
             super.start();
         }
     }
 
     // This message is used to start a local snapshot. The snapshot
     // is *not* automatically coordinated across the full node set.
     // That must be arranged separately.
     static class ExecutionSiteLocalSnapshotMessage extends VoltMessage
     {
         ExecutionSiteLocalSnapshotMessage(long roadblocktxnid) {
             m_roadblockTransactionId = roadblocktxnid;
         }
 
         @Override
         protected void flattenToBuffer(DBBPool pool) {
             // can be empty if only used locally
         }
 
         @Override
         protected void initFromBuffer() {
             // can be empty if only used locally
         }
 
         @Override
         public byte getSubject() {
             return Subject.FAILURE.getId();
         }
 
         @Override
         protected boolean requiresDurabilityP() {
             return false;
         }
 
         long m_roadblockTransactionId;
     }
 
     // This message is used locally to schedule a node failure event's
     // required  processing at an execution site.
     static class ExecutionSiteNodeFailureMessage extends VoltMessage
     {
         final HashSet<NodeFailureFault> m_failedHosts;
         ExecutionSiteNodeFailureMessage(HashSet<NodeFailureFault> failedHosts)
         {
             m_failedHosts = failedHosts;
         }
 
         @Override
         protected void flattenToBuffer(DBBPool pool) {} // can be empty if only used locally
 
         @Override
         protected void initFromBuffer() {} // can be empty if only used locally
 
         @Override
         public byte getSubject() {
             return Subject.FAILURE.getId();
         }
 
         @Override
         protected boolean requiresDurabilityP() {
             return false;
         }
     }
 
     /**
      * Generated when a snapshot buffer is discarded. Reminds the EE thread
      * that there is probably more snapshot work to do.
      */
     static class PotentialSnapshotWorkMessage extends VoltMessage
     {
         @Override
         protected void flattenToBuffer(DBBPool pool) {} // can be empty if only used locally
         @Override
         protected void initFromBuffer() {} // can be empty if only used locally
 
         @Override
         public byte getSubject() {
             return Subject.DEFAULT.getId();
         }
         @Override
         protected boolean requiresDurabilityP() {
             return false;
         }
     }
 
     // This message is used locally to get the currently active TransactionState
     // to check whether or not its WorkUnit's dependencies have been satisfied.
     // Necessary after handling a node failure.
     static class CheckTxnStateCompletionMessage extends VoltMessage
     {
         final long m_txnId;
         CheckTxnStateCompletionMessage(long txnId)
         {
             m_txnId = txnId;
         }
 
         @Override
         protected void flattenToBuffer(DBBPool pool) {} // can be empty if only used locally
         @Override
         protected void initFromBuffer() {} // can be empty if only used locally
 
         @Override
         protected boolean requiresDurabilityP() {
             return false;
         }
     }
 
     private class ExecutionSiteNodeFailureFaultHandler implements FaultHandler
     {
         @Override
         public void faultOccured(Set<VoltFault> faults)
         {
             if (m_shouldContinue == false) {
                 return;
             }
             HashSet<NodeFailureFault> failedNodes = new HashSet<NodeFailureFault>();
             for (VoltFault fault : faults) {
                 if (fault instanceof NodeFailureFault)
                 {
                     NodeFailureFault node_fault = (NodeFailureFault)fault;
                     failedNodes.add(node_fault);
                 }
                 else
                 {
                     VoltDB.instance().getFaultDistributor().reportFaultHandled(this, fault);
                 }
             }
             if (!failedNodes.isEmpty()) {
                 m_mailbox.deliver(new ExecutionSiteNodeFailureMessage(failedNodes));
             }
         }
 
         @Override
         public void faultCleared(Set<VoltFault> faults) {
         }
     }
 
     private final HashMap<Long, VoltSystemProcedure> m_registeredSysProcPlanFragments =
         new HashMap<Long, VoltSystemProcedure>();
 
 
     /**
      * Log settings changed. Signal EE to update log level.
      */
     public void updateBackendLogLevels() {
         ee.setLogLevels(org.voltdb.jni.EELoggers.getLogLevels());
     }
 
     void startShutdown() {
         m_shouldContinue = false;
     }
 
     /**
      * Shutdown all resources that need to be shutdown for this <code>ExecutionSite</code>.
      * May be called twice if recursing via recursableRun(). Protected against that..
      */
     public void shutdown() {
         if (haveShutdownAlready) {
             return;
         }
         haveShutdownAlready = true;
         m_shouldContinue = false;
 
         boolean finished = false;
         while (!finished) {
             try {
                 if (m_watchdog.isAlive()) {
                     m_watchdog.m_shouldContinue = false;
                     m_watchdog.interrupt();
                     m_watchdog.join();
                 }
 
                 m_transactionQueue.shutdown();
 
                 ProcedureProfiler.flushProfile();
                 if (hsql != null) {
                     hsql.shutdown();
                 }
                 if (ee != null) {
                     ee.release();
                 }
                 finished = true;
             } catch (final InterruptedException e) {
                 e.printStackTrace();
             }
         }
 
         m_snapshotter.shutdown();
     }
 
     /**
      * Passed to recovery processors which forward non-recovery messages to this handler.
      */
     private final MessageHandler m_messageHandler = new MessageHandler() {
         @Override
         public void handleMessage(VoltMessage message) {
             handleMailboxMessage(message);
         }
     };
 
     /**
      * This is invoked after all recovery data has been received/sent. The processor can be nulled out for GC.
      */
     private final Runnable m_onRecoveryCompletion = new Runnable() {
         @Override
         public void run() {
             final long now = System.currentTimeMillis();
             final long transferred = m_recoveryProcessor.bytesTransferred();
             final long bytesTransferredTotal = m_recoveryBytesTransferred.addAndGet(transferred);
             final long megabytes = transferred / (1024 * 1024);
             final double megabytesPerSecond = megabytes / ((now - m_recoveryStartTime) / 1000.0);
             m_recoveryProcessor = null;
             m_recovering = false;
             if (m_haveRecoveryPermit) {
                 m_haveRecoveryPermit = false;
                 m_recoveryPermit.release();
                 m_recoveryLog.info(
                         "Destination recovery complete for site " + m_siteId +
                         " partition " + m_context.siteTracker.getPartitionForSite(m_siteId) +
                         " after " + ((now - m_recoveryStartTime) / 1000) + " seconds " +
                         " with " + megabytes + " megabytes transferred " +
                         " at a rate of " + megabytesPerSecond + " megabytes/sec");
                 int remaining = recoveringSiteCount.decrementAndGet();
                 if (remaining == 0) {
                     ee.toggleProfiler(0);
                     VoltDB.instance().onRecoveryCompletion(bytesTransferredTotal);
                 }
             } else {
                 m_recoveryLog.info("Source recovery complete for site " + m_siteId +
                         " partition " + m_context.siteTracker.getPartitionForSite(m_siteId) +
                         " after " + ((now - m_recoveryStartTime) / 1000) + " seconds " +
                         " with " + megabytes + " megabytes transferred " +
                         " at a rate of " + megabytesPerSecond + " megabytes/sec");
             }
         }
     };
 
     public void tick() {
         // invoke native ee tick if at least one second has passed
         final long time = EstTime.currentTimeMillis();
         final long prevLastTickTime = lastTickTime;
         if ((time - lastTickTime) >= 1000) {
             if ((lastTickTime != 0) && (ee != null)) {
                 ee.tick(time, lastCommittedTxnId);
             }
             lastTickTime = time;
         }
 
         // do other periodic work
         m_snapshotter.doSnapshotWork(ee);
         m_watchdog.pet();
 
         /*
          * grab the table statistics from ee and put it into the statistics
          * agent if at least 1/3 of the statistics broadcast interval has past.
          * This ensures that when the statistics are broadcasted, they are
          * relatively up-to-date.
          */
         if (m_tableStats != null
             && (time - prevLastTickTime) >= StatsManager.POLL_INTERVAL * 2) {
             CatalogMap<Table> tables = m_context.database.getTables();
             int[] tableIds = new int[tables.size()];
             int i = 0;
             for (Table table : tables) {
                 tableIds[i++] = table.getRelativeIndex();
             }
             final VoltTable[] s =
                 ee.getStats(SysProcSelector.TABLE, tableIds, false, time);
             if (s != null) {
                 m_tableStats.setStatsTable(s[0]);
             }
         }
     }
 
 
     /**
      * SystemProcedures are "friends" with ExecutionSites and granted
      * access to internal state via m_systemProcedureContext.
      */
     public interface SystemProcedureExecutionContext {
         public Database getDatabase();
         public Cluster getCluster();
         public Site getSite();
         public ExecutionEngine getExecutionEngine();
         public long getLastCommittedTxnId();
         public long getNextUndo();
         public ExecutionSite getExecutionSite();
     }
 
     protected class SystemProcedureContext implements SystemProcedureExecutionContext {
         @Override
         public Database getDatabase()               { return m_context.database; }
         @Override
         public Cluster getCluster()                 { return m_context.cluster; }
         @Override
         public Site getSite()                       { return getCatalogSite(); }
         @Override
         public ExecutionEngine getExecutionEngine() { return ee; }
         @Override
         public long getLastCommittedTxnId()         { return lastCommittedTxnId; }
         @Override
         public long getNextUndo()                   { return getNextUndoToken(); }
         @Override
         public ExecutionSite getExecutionSite()     { return ExecutionSite.this; }
     }
 
     SystemProcedureContext m_systemProcedureContext;
 
     /**
      * Dummy ExecutionSite useful to some tests that require Mock/Do-Nothing sites.
      * @param siteId
      */
     ExecutionSite(int siteId) {
         m_siteId = siteId;
         m_systemProcedureContext = new SystemProcedureContext();
         m_watchdog = null;
         ee = null;
         hsql = null;
         m_dumpId = "MockExecSite";
         m_snapshotter = null;
         m_mailbox = null;
         m_transactionQueue = null;
         m_starvationTracker = null;
         m_tableStats = null;
     }
 
     ExecutionSite(VoltDBInterface voltdb, Mailbox mailbox,
                   final int siteId, String serializedCatalog,
                   RestrictedPriorityQueue transactionQueue,
                   boolean recovering,
                   HashSet<Integer> failedHostIds)
     {
         hostLog.l7dlog( Level.TRACE, LogKeys.host_ExecutionSite_Initializing.name(),
                 new Object[] { String.valueOf(siteId) }, null);
 
         m_siteId = siteId;
         String txnlog_name = ExecutionSite.class.getName() + "." + m_siteId;
         m_txnlog = new VoltLogger(txnlog_name);
         m_recovering = recovering;
         m_context = voltdb.getCatalogContext();
 
         for (Integer failedHostId : failedHostIds) {
             m_knownFailedSites.addAll(m_context.siteTracker.getAllSitesForHost(failedHostId));
         }
         m_handledFailedSites.addAll(m_knownFailedSites);
 
         VoltDB.instance().getFaultDistributor().
         registerFaultHandler(NodeFailureFault.NODE_FAILURE_EXECUTION_SITE,
                              m_faultHandler,
                              FaultType.NODE_FAILURE);
 
         if (voltdb.getBackendTargetType() == BackendTarget.NONE) {
             ee = new MockExecutionEngine();
             hsql = null;
         }
         else if (voltdb.getBackendTargetType() == BackendTarget.HSQLDB_BACKEND) {
             hsql = initializeHSQLBackend();
             ee = new MockExecutionEngine();
         }
         else {
             if (serializedCatalog == null) {
                 serializedCatalog = voltdb.getCatalogContext().catalog.serialize();
             }
             hsql = null;
             ee = initializeEE(voltdb.getBackendTargetType(), serializedCatalog);
         }
 
         // Should pass in the watchdog class to allow sleepy dogs..
         m_watchdog = new Watchdog(siteId, siteIndex);
 
         m_dumpId = "ExecutionSite." + String.valueOf(getSiteId());
         DumpManager.register(m_dumpId, this);
 
         m_systemProcedureContext = new SystemProcedureContext();
         m_mailbox = mailbox;
 
         // allow dependency injection of the transaction queue implementation
         m_transactionQueue =
             (transactionQueue != null) ? transactionQueue : initializeTransactionQueue(siteId);
 
         loadProceduresFromCatalog(voltdb.getBackendTargetType());
         m_snapshotter = new SnapshotSiteProcessor(new Runnable() {
             @Override
             public void run() {
                 m_mailbox.deliver(new PotentialSnapshotWorkMessage());
             }
         });
 
         final StatsAgent statsAgent = VoltDB.instance().getStatsAgent();
         m_starvationTracker = new StarvationTracker(String.valueOf(getCorrespondingSiteId()), getCorrespondingSiteId());
         statsAgent.registerStatsSource(SysProcSelector.STARVATION,
                                        Integer.parseInt(getCorrespondingCatalogSite().getTypeName()),
                                        m_starvationTracker);
         m_tableStats = new TableStats(String.valueOf(getCorrespondingSiteId()), getCorrespondingSiteId());
         statsAgent.registerStatsSource(SysProcSelector.TABLE,
                                        Integer.parseInt(getCorrespondingCatalogSite().getTypeName()),
                                        m_tableStats);
 
     }
 
     private RestrictedPriorityQueue initializeTransactionQueue(final int siteId)
     {
         // build an array of all the initiators
         int initiatorCount = 0;
         for (final Site s : m_context.siteTracker.getUpSites())
             if (s.getIsexec() == false)
                 initiatorCount++;
         final int[] initiatorIds = new int[initiatorCount];
         int index = 0;
         for (final Site s : m_context.siteTracker.getUpSites())
             if (s.getIsexec() == false)
                 initiatorIds[index++] = Integer.parseInt(s.getTypeName());
 
         assert(m_mailbox != null);
         RestrictedPriorityQueue retval = new RestrictedPriorityQueue(initiatorIds, siteId, m_mailbox);
         return retval;
     }
 
     private HsqlBackend initializeHSQLBackend()
     {
         HsqlBackend hsqlTemp = null;
         try {
             hsqlTemp = new HsqlBackend(getSiteId());
             final String hexDDL = m_context.database.getSchema();
             final String ddl = Encoder.hexDecodeToString(hexDDL);
             final String[] commands = ddl.split("\n");
             for (String command : commands) {
                 String decoded_cmd = Encoder.hexDecodeToString(command);
                 decoded_cmd = decoded_cmd.trim();
                 if (decoded_cmd.length() == 0) {
                     continue;
                 }
                 hsqlTemp.runDDL(decoded_cmd);
             }
         }
         catch (final Exception ex) {
             hostLog.l7dlog( Level.FATAL, LogKeys.host_ExecutionSite_FailedConstruction.name(),
                             new Object[] { getSiteId(), siteIndex }, ex);
             VoltDB.crashVoltDB();
         }
         return hsqlTemp;
     }
 
     private ExecutionEngine
     initializeEE(BackendTarget target, String serializedCatalog)
     {
         String hostname = ConnectionUtil.getHostnameOrAddress();
 
         ExecutionEngine eeTemp = null;
         try {
             if (target == BackendTarget.NATIVE_EE_JNI) {
                 Site site = getCatalogSite();
                 eeTemp =
                     new ExecutionEngineJNI(
                         this,
                         m_context.cluster.getRelativeIndex(),
                         getSiteId(),
                         Integer.valueOf(site.getPartition().getTypeName()),
                         Integer.valueOf(site.getHost().getTypeName()),
                         hostname);
                 eeTemp.loadCatalog(serializedCatalog);
                 lastTickTime = EstTime.currentTimeMillis();
                 eeTemp.tick( lastTickTime, 0);
             }
             else {
                 // set up the EE over IPC
                 Site site = getCatalogSite();
                 eeTemp =
                     new ExecutionEngineIPC(
                             this,
                             m_context.cluster.getRelativeIndex(),
                             getSiteId(),
                             Integer.valueOf(site.getPartition().getTypeName()),
                             Integer.valueOf(site.getHost().getTypeName()),
                             hostname,
                             target,
                             VoltDB.instance().getConfig().m_ipcPorts.remove(0));
                 eeTemp.loadCatalog(serializedCatalog);
                 lastTickTime = EstTime.currentTimeMillis();
                 eeTemp.tick( lastTickTime, 0);
             }
         }
         // just print error info an bail if we run into an error here
         catch (final Exception ex) {
             hostLog.l7dlog( Level.FATAL, LogKeys.host_ExecutionSite_FailedConstruction.name(),
                             new Object[] { getSiteId(), siteIndex }, ex);
             VoltDB.crashVoltDB();
         }
         return eeTemp;
     }
 
     public boolean updateClusterState(String catalogDiffCommands) {
         m_context = VoltDB.instance().getCatalogContext();
         m_knownFailedSites.removeAll(m_context.siteTracker.getAllLiveSites());
         m_handledFailedSites.removeAll(m_context.siteTracker.getAllLiveSites());
 
         // make sure the restricted priority queue knows about all of the up initiators
         // for most catalog changes this will do nothing
         // for rejoin, it will matter
         int newInitiators = 0;
         for (Site s : m_context.catalog.getClusters().get("cluster").getSites()) {
             if (s.getIsexec() == false && s.getIsup()) {
                 newInitiators += m_transactionQueue.ensureInitiatorIsKnown(Integer.parseInt(s.getTypeName()));
             }
         }
 
         return true;
     }
 
     public boolean updateCatalog(String catalogDiffCommands, CatalogContext context) {
         m_context = context;
         loadProceduresFromCatalog(VoltDB.getEEBackendType());
         ee.updateCatalog(catalogDiffCommands, m_context.catalogVersion);
 
         return true;
     }
 
     void loadProceduresFromCatalog(BackendTarget backendTarget) {
         m_registeredSysProcPlanFragments.clear();
         procs.clear();
         // load up all the stored procedures
         final CatalogMap<Procedure> catalogProcedures = m_context.database.getProcedures();
         for (final Procedure proc : catalogProcedures) {
             VoltProcedure wrapper = null;
             if (proc.getHasjava()) {
                 final String className = proc.getClassname();
                 Class<?> procClass = null;
                 try {
                     procClass = m_context.classForProcedure(className);
                 }
                 catch (final ClassNotFoundException e) {
                     hostLog.l7dlog(
                             Level.WARN,
                             LogKeys.host_ExecutionSite_GenericException.name(),
                             new Object[] { getSiteId(), siteIndex },
                             e);
                     VoltDB.crashVoltDB();
                 }
                 try {
                     wrapper = (VoltProcedure) procClass.newInstance();
                 }
                 catch (final InstantiationException e) {
                     hostLog.l7dlog( Level.WARN, LogKeys.host_ExecutionSite_GenericException.name(),
                                     new Object[] { getSiteId(), siteIndex }, e);
                 }
                 catch (final IllegalAccessException e) {
                     hostLog.l7dlog( Level.WARN, LogKeys.host_ExecutionSite_GenericException.name(),
                                     new Object[] { getSiteId(), siteIndex }, e);
                 }
             }
             else {
                 wrapper = new VoltProcedure.StmtProcedure();
             }
 
             wrapper.init(m_context.cluster.getPartitions().size(),
                          this, proc, backendTarget, hsql, m_context.cluster);
             procs.put(proc.getTypeName(), wrapper);
         }
     }
 
     /**
      * Primary run method that is invoked a single time when the thread is started.
      * Has the opportunity to do startup config.
      */
     @Override
     public void run() {
         // enumerate site id (pad to 4 digits for sort)
         String name = "ExecutionSite:";
         if (getSiteId() < 10) name += "0";
         if (getSiteId() < 100) name += "0";
         if (getSiteId() < 1000) name += "0";
         name += String.valueOf(getSiteId());
         Thread.currentThread().setName(name);
 
         // Commenting this out when making logging more abstrace (is that ok?)
         //NDC.push("ExecutionSite - " + getSiteId() + " index " + siteIndex);
         if (VoltDB.getUseWatchdogs()) {
             m_watchdog.start(Thread.currentThread());
         }
 
         try {
             // Only poll messaging layer if necessary. Allow the poll
             // to block if the execution site is truly idle.
             while (m_shouldContinue) {
                 /*
                  * If this partition is recovering, check for a permit and RPQ
                  * readiness. If it is time, create a recovery processor and send
                  * the initiate message.
                  */
                 if (m_recovering && !m_haveRecoveryPermit) {
                     Long safeTxnId = m_transactionQueue.safeToRecover();
                     if (safeTxnId != null && m_recoveryPermit.tryAcquire()) {
                         m_haveRecoveryPermit = true;
                         m_recoveryStartTime = System.currentTimeMillis();
                         m_recoveryProcessor =
                             RecoverySiteProcessorDestination.createProcessor(
                                     m_context.database,
                                     m_context.siteTracker,
                                     ee,
                                     m_mailbox,
                                     m_siteId,
                                     m_onRecoveryCompletion,
                                     m_messageHandler);
                     }
                 }
 
                 TransactionState currentTxnState = m_transactionQueue.poll();
                 if (currentTxnState == null) {
                     // poll the messaging layer for a while as this site has nothing to do
                     // this will likely have a message/several messages immediately in a heavy workload
                     // Before blocking record the starvation
                     VoltMessage message = m_mailbox.recv();
                     if (message == null) {
                         m_starvationTracker.beginStarvation();
                         message = m_mailbox.recvBlocking(5);
                     }
                     // do periodic work
                     tick();
                     if (message != null) {
                         m_starvationTracker.endStarvation();
                         handleMailboxMessage(message);
                     }
                 }
                 if (currentTxnState != null) {
                     /*
                      * Before doing a transaction check if it is time to start recovery
                      * or do recovery work. The recovery processor checks
                      * if the txn is greater than X
                      */
                     if (m_recoveryProcessor != null) {
                         m_recoveryProcessor.doRecoveryWork(currentTxnState.txnId);
                     }
                     recursableRun(currentTxnState);
                 }
                 else if (m_recoveryProcessor != null) {
                     /*
                      * If there is no work in the system the minimum safe txnId is used to move
                      * recovery forward. This works because heartbeats will move the minimum safe txnId
                      * up even when there is no work for this partition.
                      */
                     Long foo = m_transactionQueue.safeToRecover();
                     if (foo != null) {
                         m_recoveryProcessor.doRecoveryWork(foo);
                     }
                 }
             }
         }
         catch (final RuntimeException e) {
             hostLog.l7dlog( Level.ERROR, LogKeys.host_ExecutionSite_RuntimeException.name(), e);
             throw e;
         }
         shutdown();
     }
 
     /**
      * Run the execution site execution loop, for tests currently.
      * Will integrate this in to the real run loop soon.. ish.
      */
     public void runLoop() {
         while (m_shouldContinue) {
             TransactionState currentTxnState = m_transactionQueue.poll();
             if (currentTxnState == null) {
                 // poll the messaging layer for a while as this site has nothing to do
                 // this will likely have a message/several messages immediately in a heavy workload
                 VoltMessage message = m_mailbox.recv();
                 tick();
                 if (message != null) {
                     handleMailboxMessage(message);
                 }
                 else {
                     // Terminate run loop on empty mailbox AND no currentTxnState
                     return;
                 }
             }
             if (currentTxnState != null) {
                 recursableRun(currentTxnState);
             }
         }
     }
 
     private void completeTransaction(TransactionState txnState) {
         if (m_txnlog.isTraceEnabled())
         {
             m_txnlog.trace("FUZZTEST completeTransaction " + txnState.txnId);
         }
         if (!txnState.isReadOnly()) {
             assert(latestUndoToken != kInvalidUndoToken);
             assert(latestUndoToken >= txnState.getBeginUndoToken());
 
             if (txnState.getBeginUndoToken() == kInvalidUndoToken) {
                 if (m_recovering == false) {
                     throw new AssertionError("Non-recovering write txn has invalid undo state.");
                 }
             }
             // release everything through the end of the current window.
             else if (latestUndoToken > txnState.getBeginUndoToken()) {
                 ee.releaseUndoToken(latestUndoToken);
             }
 
             // reset for error checking purposes
             txnState.setBeginUndoToken(kInvalidUndoToken);
         }
 
         // advance the committed transaction point. Necessary for both Export
         // commit tracking and for fault detection transaction partial-transaction
         // resolution.
         if (!txnState.needsRollback())
         {
             if (txnState.txnId > lastCommittedTxnId) {
                 lastCommittedTxnId = txnState.txnId;
                 if (!txnState.isSinglePartition() && !txnState.isReadOnly())
                 {
                     lastKnownGloballyCommitedMultiPartTxnId =
                         Math.max(txnState.txnId, lastKnownGloballyCommitedMultiPartTxnId);
                 }
             }
         }
     }
 
     private void handleMailboxMessage(VoltMessage message)
     {
         if (message instanceof TransactionInfoBaseMessage) {
             TransactionInfoBaseMessage info = (TransactionInfoBaseMessage)message;
             assertTxnIdOrdering(info);
 
             // Special case heartbeats which only update RPQ
             if (info instanceof HeartbeatMessage) {
                 // use the heartbeat to unclog the priority queue if clogged
                 long lastSeenTxnFromInitiator = m_transactionQueue.noteTransactionRecievedAndReturnLastSeen(
                         info.getInitiatorSiteId(), info.getTxnId(),
                         true, ((HeartbeatMessage) info).getLastSafeTxnId());
 
                 // respond to the initiator with the last seen transaction
                 HeartbeatResponseMessage response = new HeartbeatResponseMessage(
                         m_siteId, lastSeenTxnFromInitiator,
                         m_transactionQueue.getQueueState() == QueueState.BLOCKED_SAFETY);
                 try {
                     m_mailbox.send(info.getInitiatorSiteId(), VoltDB.DTXN_MAILBOX_ID, response);
                 } catch (MessagingException e) {
                     // hope this never happens... it doesn't right?
                     throw new RuntimeException(e);
                 }
                 // we're done here (in the case of heartbeats)
                 return;
             }
             else if (info instanceof InitiateTaskMessage) {
                 m_transactionQueue.noteTransactionRecievedAndReturnLastSeen(info.getInitiatorSiteId(),
                                                   info.getTxnId(),
                                                   false,
                                                   ((InitiateTaskMessage) info).getLastSafeTxnId());
             }
             // FragmentTasks aren't sent by initiators and shouldn't update
             // transaction queue initiator states.
             else if (info instanceof MultiPartitionParticipantMessage) {
                 m_transactionQueue.noteTransactionRecievedAndReturnLastSeen(info.getInitiatorSiteId(),
                                                   info.getTxnId(),
                                                   false,
                                                   DtxnConstants.DUMMY_LAST_SEEN_TXN_ID);
             }
 
             // Every non-heartbeat notice requires a transaction state.
             TransactionState ts = m_transactionsById.get(info.getTxnId());
             if (info instanceof CompleteTransactionMessage)
             {
                 CompleteTransactionMessage complete = (CompleteTransactionMessage)info;
                 if (ts != null)
                 {
                     ts.processCompleteTransaction(complete);
                 }
                 else
                 {
                     // if we're getting a CompleteTransactionMessage
                     // and there's no transaction state, it's because
                     // we were the cause of the rollback and we bailed
                     // as soon as we signaled our failure to the coordinator.
                     // Just generate an ack to keep the coordinator happy.
                     if (complete.requiresAck())
                     {
                         CompleteTransactionResponseMessage ctrm =
                             new CompleteTransactionResponseMessage(complete, m_siteId);
                         try
                         {
                             m_mailbox.send(complete.getCoordinatorSiteId(), 0, ctrm);
                         }
                         catch (MessagingException e) {
                             throw new RuntimeException(e);
                         }
                     }
                 }
                 return;
             }
 
             if (ts == null) {
                 if (info.isSinglePartition()) {
                     ts = new SinglePartitionTxnState(m_mailbox, this, info);
                 }
                 else {
                     ts = new MultiPartitionParticipantTxnState(m_mailbox, this, info);
                 }
                 m_transactionQueue.add(ts);
                 m_transactionsById.put(ts.txnId, ts);
             }
 
             if (ts != null)
             {
                 if (message instanceof FragmentTaskMessage) {
                     ts.createLocalFragmentWork((FragmentTaskMessage)message, false);
                 }
             }
         } else if (message instanceof RecoveryMessage) {
             RecoveryMessage rm = (RecoveryMessage)message;
             if (rm.recoveryMessagesAvailable()) {
                 return;
             }
             assert(!m_recovering);
             assert(m_recoveryProcessor == null);
             final long recoveringPartitionTxnId = rm.txnId();
             m_recoveryStartTime = System.currentTimeMillis();
             m_recoveryLog.info(
                     "Recovery initiate received at site " + m_siteId +
                     " from site " + rm.sourceSite() + " requesting recovery start before txnid " +
                     recoveringPartitionTxnId);
             m_recoveryProcessor = RecoverySiteProcessorSource.createProcessor(
                     rm,
                     m_context.database,
                     m_context.siteTracker,
                     ee,
                     m_mailbox,
                     m_siteId,
                     m_onRecoveryCompletion,
                     m_messageHandler);
         }
         else if (message instanceof FragmentResponseMessage) {
             FragmentResponseMessage response = (FragmentResponseMessage)message;
             TransactionState txnState = m_transactionsById.get(response.getTxnId());
             // possible in rollback to receive an unnecessary response
             if (txnState != null) {
                 assert (txnState instanceof MultiPartitionParticipantTxnState);
                 txnState.processRemoteWorkResponse(response);
             }
         }
         else if (message instanceof CompleteTransactionResponseMessage)
         {
             CompleteTransactionResponseMessage response =
                 (CompleteTransactionResponseMessage)message;
             TransactionState txnState = m_transactionsById.get(response.getTxnId());
             // I believe a null txnState should eventually be impossible, let's
             // check for null for now
             if (txnState != null)
             {
                 assert (txnState instanceof MultiPartitionParticipantTxnState);
                 txnState.processCompleteTransactionResponse(response);
             }
         }
         else if (message instanceof DebugMessage) {
             DebugMessage dmsg = (DebugMessage) message;
             if (dmsg.shouldDump)
                 DumpManager.putDump(m_dumpId, m_currentDumpTimestamp, true, getDumpContents());
         }
         else if (message instanceof ExecutionSiteNodeFailureMessage) {
             discoverGlobalFaultData((ExecutionSiteNodeFailureMessage)message);
         }
         else if (message instanceof CheckTxnStateCompletionMessage) {
             long txn_id = ((CheckTxnStateCompletionMessage)message).m_txnId;
             TransactionState txnState = m_transactionsById.get(txn_id);
             if (txnState != null)
             {
                 assert(txnState instanceof MultiPartitionParticipantTxnState);
                 ((MultiPartitionParticipantTxnState)txnState).checkWorkUnits();
             }
         }
         else if (message instanceof RawProcessor.ExportInternalMessage) {
             RawProcessor.ExportInternalMessage exportm =
                 (RawProcessor.ExportInternalMessage) message;
             ExportProtoMessage response =
                 ee.exportAction(exportm.m_m.isAck(),
                                 exportm.m_m.isPoll(),
                                 exportm.m_m.isClose(),
                                 exportm.m_m.isSync(),
                                 exportm.m_m.getAckOffset(),
                                 0,
                                 exportm.m_m.getPartitionId(),
                                 exportm.m_m.getTableId());
             // not all actions generate a response
             if (response != null) {
                 ExportInternalMessage mbp = new ExportInternalMessage(exportm.m_sb, response);
                 ExportManager.instance().queueMessage(mbp);
             }
         } else if (message instanceof PotentialSnapshotWorkMessage) {
             m_snapshotter.doSnapshotWork(ee);
         }
         else if (message instanceof ExecutionSiteLocalSnapshotMessage) {
            hostLog.info("Executing local snapshot. Completing any on-going snapshots.");
 
             // first finish any on-going snapshot
             try {
                HashSet<Exception> completeSnapshotWork = m_snapshotter.completeSnapshotWork(ee);
                if (completeSnapshotWork != null && !completeSnapshotWork.isEmpty()) {
                    for (Exception e : completeSnapshotWork) {
                        hostLog.error("Error completing in progress snapshot.", e);
                    }
                }
             } catch (InterruptedException e) {
                 hostLog.warn("Interrupted during snapshot completion", e);
             }
 
            hostLog.info("Executing local snapshot. Creating new snapshot.");
 
             // then initiate the local snapshot
             SnapshotSchedule schedule = m_context.cluster.getFaultsnapshots().get("CLUSTER_PARTITION");
             SnapshotSaveAPI saveAPI = new SnapshotSaveAPI();
            VoltTable startSnapshotting = saveAPI.startSnapshotting(schedule.getPath(),
                                       schedule.getPrefix(),
                                       (byte) 0x1,
                                       ((ExecutionSiteLocalSnapshotMessage) message).m_roadblockTransactionId,
                                       m_systemProcedureContext,
                                       ConnectionUtil.getHostnameOrAddress());
            hostLog.info("Executing local snapshot. Finished final snapshot. Shutting down. " +
                    "Result: " + startSnapshotting.toString());
             if (SnapshotSiteProcessor.ExecutionSitesCurrentlySnapshotting.get() == -1) {
                 VoltDB.crashVoltDB();
             }
         }
         else {
             hostLog.l7dlog(Level.FATAL, LogKeys.org_voltdb_dtxn_SimpleDtxnConnection_UnkownMessageClass.name(),
                            new Object[] { message.getClass().getName() }, null);
             VoltDB.crashVoltDB();
         }
     }
 
     private void assertTxnIdOrdering(final TransactionInfoBaseMessage notice) {
         // Because of our rollback implementation, fragment tasks can arrive
         // late. This participant can have aborted and rolled back already,
         // for example.
         //
         // Additionally, commit messages for read-only MP transactions can
         // arrive after sneaked-in SP transactions have advanced the last
         // committed transaction point. A commit message is a fragment task
         // with a null payload.
         if (notice instanceof FragmentTaskMessage ||
             notice instanceof CompleteTransactionMessage)
         {
             return;
         }
 
         if (notice.getTxnId() < lastCommittedTxnId) {
             StringBuilder msg = new StringBuilder();
             msg.append("Txn ordering deadlock (DTXN) at site ").append(m_siteId).append(":\n");
             msg.append("   txn ").append(lastCommittedTxnId).append(" (");
             msg.append(TransactionIdManager.toString(lastCommittedTxnId)).append(" HB: ?");
             msg.append(") before\n");
             msg.append("   txn ").append(notice.getTxnId()).append(" (");
             msg.append(TransactionIdManager.toString(notice.getTxnId())).append(" HB:");
             msg.append(notice instanceof HeartbeatMessage).append(").\n");
 
             TransactionState txn = m_transactionsById.get(notice.getTxnId());
             if (txn != null) {
                 msg.append("New notice transaction already known: " + txn.toString() + "\n");
             }
             else {
                 msg.append("New notice is for new or completed transaction.\n");
             }
             msg.append("New notice of type: " + notice.getClass().getName());
             log.fatal(msg);
             VoltDB.crashVoltDB();
         }
 
         if (notice instanceof InitiateTaskMessage) {
             InitiateTaskMessage task = (InitiateTaskMessage)notice;
             assert (task.getInitiatorSiteId() != getSiteId());
         }
     }
 
     /**
      * Find the global multi-partition commit point and the global initiator point for the
      * failed host.
      *
      * @param failedHostId the host id of the failed node.
      */
     private void discoverGlobalFaultData(ExecutionSiteNodeFailureMessage message)
     {
         //Keep it simple and don't try to recover on the recovering node.
         if (m_recovering) {
             m_recoveryLog.fatal("Aborting recovery due to a remote node failure. Retry again.");
             VoltDB.crashVoltDB();
         }
         HashSet<NodeFailureFault> failures = message.m_failedHosts;
 
         // Fix context and associated site tracker first - need
         // an accurate topology to perform discovery.
         m_context = VoltDB.instance().getCatalogContext();
 
         HashSet<Integer> failedSiteIds = new HashSet<Integer>();
         for (NodeFailureFault fault : failures) {
             failedSiteIds.addAll(m_context.siteTracker.getAllSitesForHost(fault.getHostId()));
         }
         m_knownFailedSites.addAll(failedSiteIds);
 
         int expectedResponses = discoverGlobalFaultData_send();
         long[] commit_and_safe = discoverGlobalFaultData_rcv(expectedResponses);
 
         if (commit_and_safe == null) {
             return;
         }
 
 
         // Agreed on a fault set.
 
         // Do the work of patching up the execution site.
         // Do a little work to identify the newly failed site ids and only handle those
 
         HashSet<Integer> newFailedSiteIds = new HashSet<Integer>(failedSiteIds);
         newFailedSiteIds.removeAll(m_handledFailedSites);
 
         // Use this agreed new-fault set to make PPD decisions.
         // Since this agreement process should eventually be moved to
         // the fault distributor - this is written with some intentional
         // feature envy.
 
         PPDPolicyDecision makePPDPolicyDecisions =
             VoltDB.instance().getFaultDistributor().makePPDPolicyDecisions(newFailedSiteIds);
 
         if (makePPDPolicyDecisions == PPDPolicyDecision.NodeFailure) {
             handleSiteFaults(false,
                     newFailedSiteIds,
                     commit_and_safe[0],
                     commit_and_safe[1]);
         }
         else if (makePPDPolicyDecisions == PPDPolicyDecision.PartitionDetection) {
             handleSiteFaults(true,
                     newFailedSiteIds,
                     commit_and_safe[0],
                     commit_and_safe[1]);
         }
 
         m_handledFailedSites.addAll(failedSiteIds);
         for (NodeFailureFault fault : failures) {
             if (newFailedSiteIds.containsAll(m_context.siteTracker.getAllSitesForHost(fault.getHostId()))) {
                 VoltDB.instance().getFaultDistributor().
                 reportFaultHandled(m_faultHandler, fault);
             }
         }
     }
 
     /**
      * The list of failed sites we know about. Included with all failure messages
      * to identify what the information was used to generate commit points
      */
     private final HashSet<Integer> m_knownFailedSites = new HashSet<Integer>();
 
     /**
      * Failed sites for which agreement has been reached.
      */
     private final HashSet<Integer> m_handledFailedSites = new HashSet<Integer>();
 
     /**
      * Store values from older failed nodes. They are repeated with every failure message
      */
     private final HashMap<Integer, HashMap<Integer, Long>> m_newestSafeTransactionForInitiatorLedger =
         new HashMap<Integer, HashMap<Integer, Long>>();
 
     /**
      * Send one message to each surviving execution site providing this site's
      * multi-partition commit point and this site's safe txnid
      * (the receiver will filter the later for its
      * own partition). Do this once for each failed initiator that we know about.
      * Sends all data all the time to avoid a need for request/response.
      */
     private int discoverGlobalFaultData_send()
     {
         int expectedResponses = 0;
         int[] survivors = m_context.siteTracker.getUpExecutionSites();
         HashSet<Integer> survivorSet = new HashSet<Integer>();
         for (int survivor : survivors) {
             survivorSet.add(survivor);
         }
         m_recoveryLog.info("Sending fault data " + m_knownFailedSites.toString() + " to "
                 + survivorSet.toString() + " survivors with lastKnownGloballyCommitedMultiPartTxnId "
                 + lastKnownGloballyCommitedMultiPartTxnId);
         try {
             for (Integer site : m_knownFailedSites) {
                 Integer hostId = m_context.siteTracker.getHostForSite(site);
                 HashMap<Integer, Long> siteMap = m_newestSafeTransactionForInitiatorLedger.get(hostId);
                 if (siteMap == null) {
                     siteMap = new HashMap<Integer, Long>();
                     m_newestSafeTransactionForInitiatorLedger.put(hostId, siteMap);
                 }
 
                 if (m_context.siteTracker.getSiteForId(site).getIsexec() == false) {
                     /*
                      * Check the queue for the data and get it from the ledger if necessary.\
                      * It might not even be in the ledger if the site has been failed
                      * since recovery of this node began.
                      */
                     Long txnId = m_transactionQueue.getNewestSafeTransactionForInitiator(site);
                     if (txnId == null) {
                         txnId = siteMap.get(site);
                         //assert(txnId != null);
                     } else {
                         siteMap.put(site, txnId);
                     }
 
                     FailureSiteUpdateMessage srcmsg =
                         new FailureSiteUpdateMessage(m_siteId,
                                                      m_knownFailedSites,
                                                      site,
                                                      txnId != null ? txnId : Long.MIN_VALUE,
                                                      //txnId,
                                                      lastKnownGloballyCommitedMultiPartTxnId);
 
                     m_mailbox.send(survivors, 0, srcmsg);
                     expectedResponses += (survivors.length);
                 }
             }
         }
         catch (MessagingException e) {
             // TODO: unsure what to do with this. maybe it implies concurrent failure?
             e.printStackTrace();
             VoltDB.crashVoltDB();
         }
         m_recoveryLog.info("Sent fault data. Expecting " + expectedResponses + " responses.");
         return expectedResponses;
     }
 
     /**
      * Collect the failure site update messages from all sites This site sent
      * its own mailbox the above broadcast the maximum is local to this site.
      * This also ensures at least one response.
      *
      * Concurrent failures can be detected by additional reports from the FaultDistributor
      * or a mismatch in the set of failed hosts reported in a message from another site
      */
     private long[] discoverGlobalFaultData_rcv(int expectedResponses)
     {
         final int localPartitionId =
             m_context.siteTracker.getPartitionForSite(m_siteId);
         int responses = 0;
         int responsesFromSamePartition = 0;
         long commitPoint = Long.MIN_VALUE;
         long safeInitPoint = Long.MIN_VALUE;
         java.util.ArrayList<FailureSiteUpdateMessage> messages = new java.util.ArrayList<FailureSiteUpdateMessage>();
         do {
             VoltMessage m = m_mailbox.recvBlocking(new Subject[] { Subject.FAILURE, Subject.FAILURE_SITE_UPDATE }, 5);
             //Invoke tick periodically to ensure that the last snapshot continues in the event that the failure
             //process does not complete
             if (m == null) {
                 tick();
                 continue;
             }
             FailureSiteUpdateMessage fm = null;
 
             if (m.getSubject() == Subject.FAILURE_SITE_UPDATE.getId()) {
                 fm = (FailureSiteUpdateMessage)m;
                 messages.add(fm);
             } else if (m.getSubject() == Subject.FAILURE.getId()) {
                 /*
                  * If the fault distributor reports a new fault, assert that the fault currently
                  * being handled is included, redeliver the message to ourself and then abort so
                  * that the process can restart.
                  */
                 HashSet<NodeFailureFault> faults = ((ExecutionSiteNodeFailureMessage)m).m_failedHosts;
                 HashSet<Integer> newFailedSiteIds = new HashSet<Integer>();
                 for (NodeFailureFault fault : faults) {
                     newFailedSiteIds.addAll(m_context.siteTracker.getAllSitesForHost(fault.getHostId()));
                 }
                 m_mailbox.deliverFront(m);
                 m_recoveryLog.info("Detected a concurrent failure from FaultDistributor, new failed sites "
                         + newFailedSiteIds);
                 return null;
             }
 
             /*
              * If the other surviving host saw a different set of failures
              */
             if (!m_knownFailedSites.equals(fm.m_failedSiteIds)) {
                 if (!m_knownFailedSites.containsAll(fm.m_failedSiteIds)) {
                     /*
                      * In this case there is a new failed host we didn't know about. Time to
                      * start the process again from square 1 with knowledge of the new failed hosts
                      * First fail all the ones we didn't know about.
                      */
                     HashSet<Integer> difference = new HashSet<Integer>(fm.m_failedSiteIds);
                     difference.removeAll(m_knownFailedSites);
                     HashSet<Integer> differenceHosts = new HashSet<Integer>();
                     for (Integer siteId : difference) {
                         differenceHosts.add(m_context.siteTracker.getHostForSite(siteId));
                     }
                     for (Integer hostId : differenceHosts) {
                         String hostname = String.valueOf(hostId);
                         if (VoltDB.instance() != null) {
                             if (VoltDB.instance().getHostMessenger() != null) {
                                 String hostnameTemp = VoltDB.instance().getHostMessenger().getHostnameForHostID(hostId);
                                 if (hostnameTemp != null) hostname = hostnameTemp;
                             }
                         }
                         VoltDB.instance().getFaultDistributor().
                             reportFault(new NodeFailureFault( hostId, hostname));
                     }
                     m_recoveryLog.info("Detected a concurrent failure from " +
                             fm.m_sourceSiteId + " with new failed sites " + difference.toString());
                     m_mailbox.deliver(m);
                     /*
                      * Return null and skip handling the fault for now. Will try again
                      * later once the other failed hosts are detected and can be dealt with at once.
                      */
                     return null;
                 } else {
                     /*
                      * In this instance they are not equal because the message is missing some
                      * failed sites. Drop the message. The sender will detect the fault and resend
                      * the message later with the correct information.
                      */
                     HashSet<Integer> difference = new HashSet<Integer>(m_knownFailedSites);
                     difference.removeAll(fm.m_failedSiteIds);
                     m_recoveryLog.info("Discarding failure message from " +
                             fm.m_sourceSiteId + " because it was missing failed sites " + difference.toString());
                     continue;
                 }
             }
 
             ++responses;
             m_recoveryLog.info("Received failure message " + responses + " of " + expectedResponses
                     + " from " + fm.m_sourceSiteId + " for failed sites " + fm.m_failedSiteIds +
                     " with commit point " + fm.m_committedTxnId + " safe txn id " + fm.m_safeTxnId);
             commitPoint =
                 Math.max(commitPoint, fm.m_committedTxnId);
 
             final int remotePartitionId =
                 m_context.siteTracker.getPartitionForSite(fm.m_sourceSiteId);
 
             if (remotePartitionId == localPartitionId) {
                 safeInitPoint =
                     Math.max(safeInitPoint, fm.m_safeTxnId);
                 responsesFromSamePartition++;
             }
         } while(responses < expectedResponses);
 
         assert(commitPoint != Long.MIN_VALUE);
         assert(safeInitPoint != Long.MIN_VALUE);
         return new long[] {commitPoint, safeInitPoint};
     }
 
 
     /**
      * Process a node failure detection.
      *
      * Different sites can process UpdateCatalog sysproc and handleNodeFault()
      * in different orders. UpdateCatalog changes MUST be commutative with
      * handleNodeFault.
      * @param partitionDetected
      *
      * @param siteIds Hashset<Integer> of host ids of failed nodes
      * @param globalCommitPoint the surviving cluster's greatest committed multi-partition transaction id
      * @param globalInitiationPoint the greatest transaction id acknowledged as globally
      * 2PC to any surviving cluster execution site by the failed initiator.
      *
      */
     void handleSiteFaults(boolean partitionDetected,
             HashSet<Integer> failedSites,
             long globalMultiPartCommitPoint,
             long globalInitiationPoint)
     {
         HashSet<Integer> failedHosts = new HashSet<Integer>();
         for (Integer siteId : failedSites) {
             failedHosts.add(m_context.siteTracker.getHostForSite(siteId));
         }
 
         StringBuffer sb = new StringBuffer();
         for (Integer hostId : failedHosts) {
             sb.append(hostId).append(' ');
         }
         if (m_txnlog.isTraceEnabled())
         {
             m_txnlog.trace("FUZZTEST handleNodeFault " + sb.toString() +
                     " with globalMultiPartCommitPoint " + globalMultiPartCommitPoint + " and globalInitiationPoint "
                     + globalInitiationPoint);
         } else {
             m_recoveryLog.info("Handling node faults " + sb.toString() +
                     " with globalMultiPartCommitPoint " + globalMultiPartCommitPoint + " and globalInitiationPoint "
                     + globalInitiationPoint);
         }
         lastKnownGloballyCommitedMultiPartTxnId = globalMultiPartCommitPoint;
 
         // If possibly partitioned, run through the safe initiated transaction and stall
         if (partitionDetected) {
             m_recoveryLog.info("Scheduling snapshot after txnId " + globalInitiationPoint +
                                " for cluster partition fault. Current commit point: " + this.lastCommittedTxnId);
             m_transactionQueue.makeRoadBlock(
                 globalInitiationPoint,
                 QueueState.BLOCKED_CLOSED,
                 new ExecutionSiteLocalSnapshotMessage(globalInitiationPoint));
         }
 
 
         // Fix safe transaction scoreboard in transaction queue
         for (Integer i : failedSites)
         {
             if (m_context.siteTracker.getSiteForId(i).getIsexec() == false) {
                 m_transactionQueue.gotFaultForInitiator(i);
             }
         }
 
         // Correct transaction state internals and commit
         // or remove affected transactions from RPQ and txnId hash.
         Iterator<Long> it = m_transactionsById.keySet().iterator();
         while (it.hasNext())
         {
             final long tid = it.next();
             TransactionState ts = m_transactionsById.get(tid);
             ts.handleSiteFaults(failedSites);
 
             // Fault a transaction that was not globally initiated
             if (ts.txnId > globalInitiationPoint &&
                 failedSites.contains(ts.initiatorSiteId))
             {
                 m_recoveryLog.info("Faulting non-globally initiated transaction " + ts.txnId);
                 it.remove();
                 m_transactionQueue.faultTransaction(ts);
             }
 
             // Multipartition transaction without a surviving coordinator:
             // Commit a txn that is in progress and committed elsewhere.
             // (Must have lost the commit message during the failure.)
             // Otherwise, without a coordinator, the transaction can't
             // continue. Must rollback, if in progress, or fault it
             // from the queues if not yet started.
             else if (ts instanceof MultiPartitionParticipantTxnState &&
                      failedSites.contains(ts.coordinatorSiteId))
             {
                 MultiPartitionParticipantTxnState mpts = (MultiPartitionParticipantTxnState) ts;
                 if (ts.isInProgress() && ts.txnId <= globalMultiPartCommitPoint)
                 {
                     m_recoveryLog.info("Committing in progress multi-partition txn " + ts.txnId +
                             " even though coordinator was on a failed host because the txnId <= " +
                             "the global multi-part commit point");
                     CompleteTransactionMessage ft =
                         mpts.createCompleteTransactionMessage(false, false);
                     m_mailbox.deliverFront(ft);
                 }
                 else if (ts.isInProgress() && ts.txnId > globalMultiPartCommitPoint) {
                     m_recoveryLog.info("Rolling back in progress multi-partition txn " + ts.txnId +
                             " because the coordinator was on a failed host and the txnId > " +
                             "the global multi-part commit point");
                     CompleteTransactionMessage ft =
                         mpts.createCompleteTransactionMessage(true, false);
                     m_mailbox.deliverFront(ft);
                 }
                 else
                 {
                     m_recoveryLog.info("Faulting multi-part transaction " + ts.txnId +
                             " because the coordinator was on a failed node");
                     it.remove();
                     m_transactionQueue.faultTransaction(ts);
                 }
             }
             // If we're the coordinator, then after we clean up our internal
             // state due to a failed node, we need to poke ourselves to check
             // to see if all the remaining dependencies are satisfied.  Do this
             // with a message to our mailbox so that happens in the
             // execution site thread
             else if (ts instanceof MultiPartitionParticipantTxnState &&
                      ts.coordinatorSiteId == m_siteId)
             {
                 if (ts.isInProgress())
                 {
                     m_mailbox.deliverFront(new CheckTxnStateCompletionMessage(ts.txnId));
                 }
             }
         }
         if (m_recoveryProcessor != null) {
             m_recoveryProcessor.handleSiteFaults( failedSites, m_context.siteTracker);
         }
     }
 
 
     private FragmentResponseMessage processSysprocFragmentTask(
             final TransactionState txnState,
             final HashMap<Integer,List<VoltTable>> dependencies,
             final long fragmentId, final FragmentResponseMessage currentFragResponse,
             final ParameterSet params)
     {
         // assume success. errors correct this assumption as they occur
         currentFragResponse.setStatus(FragmentResponseMessage.SUCCESS, null);
 
         VoltSystemProcedure proc = null;
         synchronized (m_registeredSysProcPlanFragments) {
             proc = m_registeredSysProcPlanFragments.get(fragmentId);
         }
 
         try {
             // set transaction state for non-coordinator snapshot restore sites
             proc.setTransactionState(txnState);
             final DependencyPair dep
                 = proc.executePlanFragment(dependencies,
                                            fragmentId,
                                            params,
                                            m_systemProcedureContext);
 
             sendDependency(currentFragResponse, dep.depId, dep.dependency);
         }
         catch (final EEException e)
         {
             hostLog.l7dlog( Level.TRACE, LogKeys.host_ExecutionSite_ExceptionExecutingPF.name(), new Object[] { fragmentId }, e);
             currentFragResponse.setStatus(FragmentResponseMessage.UNEXPECTED_ERROR, e);
         }
         catch (final SQLException e)
         {
             hostLog.l7dlog( Level.TRACE, LogKeys.host_ExecutionSite_ExceptionExecutingPF.name(), new Object[] { fragmentId }, e);
             currentFragResponse.setStatus(FragmentResponseMessage.UNEXPECTED_ERROR, e);
         }
         catch (final Exception e)
         {
             // Just indicate that we failed completely
             currentFragResponse.setStatus(FragmentResponseMessage.UNEXPECTED_ERROR, new SerializableException(e));
         }
 
         return currentFragResponse;
     }
 
 
     private void sendDependency(
             final FragmentResponseMessage currentFragResponse,
             final int dependencyId,
             final VoltTable dependency)
     {
         if (log.isTraceEnabled()) {
             log.l7dlog(Level.TRACE,
                        LogKeys.org_voltdb_ExecutionSite_SendingDependency.name(),
                        new Object[] { dependencyId }, null);
         }
         currentFragResponse.addDependency(dependencyId, dependency);
     }
 
 
     /*
      * Do snapshot work exclusively until there is no more. Also blocks
      * until the syncing and closing of snapshot data targets has completed.
      */
     public void initiateSnapshots(Deque<SnapshotTableTask> tasks) {
         m_snapshotter.initiateSnapshots(ee, tasks);
     }
 
     public HashSet<Exception> completeSnapshotWork() throws InterruptedException {
         return m_snapshotter.completeSnapshotWork(ee);
     }
 
 
     /*
      *  SiteConnection Interface (VoltProcedure -> ExecutionSite)
      */
 
     @Override
     public void registerPlanFragment(final long pfId, final VoltSystemProcedure proc) {
         synchronized (m_registeredSysProcPlanFragments) {
             assert(m_registeredSysProcPlanFragments.containsKey(pfId) == false);
             m_registeredSysProcPlanFragments.put(pfId, proc);
         }
     }
 
     @Override
     public Site getCorrespondingCatalogSite() {
         return getCatalogSite();
     }
 
     @Override
     public int getCorrespondingSiteId() {
         return m_siteId;
     }
 
     @Override
     public int getCorrespondingPartitionId() {
         return Integer.valueOf(getCatalogSite().getPartition().getTypeName());
     }
 
     @Override
     public int getCorrespondingHostId() {
         return Integer.valueOf(getCatalogSite().getHost().getTypeName());
     }
 
     @Override
     public void loadTable(
             long txnId,
             String clusterName,
             String databaseName,
             String tableName,
             VoltTable data,
             int allowExport)
     throws VoltAbortException
     {
         Cluster cluster = m_context.cluster;
         if (cluster == null) {
             throw new VoltAbortException("cluster '" + clusterName + "' does not exist");
         }
         Database db = cluster.getDatabases().get(databaseName);
         if (db == null) {
             throw new VoltAbortException("database '" + databaseName + "' does not exist in cluster " + clusterName);
         }
         Table table = db.getTables().getIgnoreCase(tableName);
         if (table == null) {
             throw new VoltAbortException("table '" + tableName + "' does not exist in database " + clusterName + "." + databaseName);
         }
 
         ee.loadTable(table.getRelativeIndex(), data,
                      txnId,
                      lastCommittedTxnId,
                      getNextUndoToken(),
                      allowExport != 0);
     }
 
     @Override
     public VoltTable[] executeQueryPlanFragmentsAndGetResults(
             long[] planFragmentIds,
             int numFragmentIds,
             ParameterSet[] parameterSets,
             int numParameterSets,
             long txnId,
             boolean readOnly) throws EEException
     {
         return ee.executeQueryPlanFragmentsAndGetResults(
             planFragmentIds,
             numFragmentIds,
             parameterSets,
             numParameterSets,
             txnId,
             lastCommittedTxnId,
             readOnly ? Long.MAX_VALUE : getNextUndoToken());
     }
 
     @Override
     public void simulateExecutePlanFragments(long txnId, boolean readOnly) {
         if (!readOnly) {
             // pretend real work was done
             getNextUndoToken();
         }
     }
 
     /**
      * Continue doing runnable work for the current transaction.
      * If doWork() returns true, the transaction is over.
      * Otherwise, the procedure may have more java to run
      * or a dependency or fragment to collect from the network.
      *
      * doWork() can sneak in a new SP transaction. Maybe it would
      * be better if transactions didn't trigger other transactions
      * and those optimization decisions where made somewhere closer
      * to this code?
      */
     @Override
     public Map<Integer, List<VoltTable>>
     recursableRun(TransactionState currentTxnState)
     {
         do
         {
             if (currentTxnState.doWork(m_recovering)) {
                 if (currentTxnState.needsRollback())
                 {
                     rollbackTransaction(currentTxnState);
                 }
                 completeTransaction(currentTxnState);
                 TransactionState ts = m_transactionsById.remove(currentTxnState.txnId);
                 assert(ts != null);
                 return null;
             }
             else if (currentTxnState.shouldResumeProcedure()){
                 Map<Integer, List<VoltTable>> retval =
                     currentTxnState.getPreviousStackFrameDropDependendencies();
                 assert(retval != null);
                 return retval;
             }
             // This is a bit ugly; more or less a straight-forward
             // extraction of the logic that used to be in
             // MultiPartitionParticipantTxnState.doWork()
             else if (currentTxnState.isBlocked() &&
                      !currentTxnState.isDone() &&
                      currentTxnState.isCoordinator() &&
                      currentTxnState.isReadOnly() &&
                      !currentTxnState.hasTransactionalWork())
             {
                 assert(!currentTxnState.isSinglePartition());
                 tryToSneakInASinglePartitionProcedure();
             }
             else
             {
                 VoltMessage message = m_mailbox.recvBlocking(5);
                 tick();
                 if (message != null) {
                     handleMailboxMessage(message);
                 }
             }
         } while (true);
     }
 
     /*
      *
      *  SiteTransactionConnection Interface (TransactionState -> ExecutionSite)
      *
      */
 
     @Override
     public SiteTracker getSiteTracker() {
         return m_context.siteTracker;
     }
 
     /**
      * Set the txn id from the WorkUnit and set/release undo tokens as
      * necessary. The DTXN currently has no notion of maintaining undo
      * tokens beyond the life of a transaction so it is up to the execution
      * site to release the undo data in the EE up until the current point
      * when the transaction ID changes.
      */
     @Override
     public final void beginNewTxn(TransactionState txnState)
     {
         if (m_txnlog.isTraceEnabled())
         {
             m_txnlog.trace("FUZZTEST beginNewTxn " + txnState.txnId + " " +
                            (txnState.isSinglePartition() ? "single" : "multi") + " " +
                            (txnState.isReadOnly() ? "readonly" : "readwrite") + " " +
                            (txnState.isCoordinator() ? "coord" : "part"));
         }
         if (!txnState.isReadOnly()) {
             assert(txnState.getBeginUndoToken() == kInvalidUndoToken);
             txnState.setBeginUndoToken(latestUndoToken);
             assert(txnState.getBeginUndoToken() != kInvalidUndoToken);
         }
     }
 
     public final void rollbackTransaction(TransactionState txnState)
     {
         if (m_txnlog.isTraceEnabled())
         {
             m_txnlog.trace("FUZZTEST rollbackTransaction " + txnState.txnId);
         }
         if (!txnState.isReadOnly()) {
             assert(latestUndoToken != kInvalidUndoToken);
             assert(txnState.getBeginUndoToken() != kInvalidUndoToken);
             assert(latestUndoToken >= txnState.getBeginUndoToken());
 
             // don't go to the EE if no work was done
             if (latestUndoToken > txnState.getBeginUndoToken()) {
                 ee.undoUndoToken(txnState.getBeginUndoToken());
             }
         }
     }
 
 
     @Override
     public FragmentResponseMessage processFragmentTask(
             TransactionState txnState,
             final HashMap<Integer,List<VoltTable>> dependencies,
             final VoltMessage task)
     {
         ParameterSet params = null;
         final FragmentTaskMessage ftask = (FragmentTaskMessage) task;
         assert(ftask.getFragmentCount() == 1);
         final long fragmentId = ftask.getFragmentId(0);
         final int outputDepId = ftask.getOutputDepId(0);
 
         final FragmentResponseMessage currentFragResponse = new FragmentResponseMessage(ftask, getSiteId());
 
         // this is a horrible performance hack, and can be removed with small changes
         // to the ee interface layer.. (rtb: not sure what 'this' encompasses...)
         final ByteBuffer paramData = ftask.getParameterDataForFragment(0);
         if (paramData != null) {
             final FastDeserializer fds = new FastDeserializer(paramData);
             try {
                 params = fds.readObject(ParameterSet.class);
             }
             catch (final IOException e) {
                 hostLog.l7dlog( Level.FATAL,
                                 LogKeys.host_ExecutionSite_FailedDeserializingParamsForFragmentTask.name(), e);
                 VoltDB.crashVoltDB();
             }
         }
         else {
             params = new ParameterSet();
         }
 
         if (ftask.isSysProcTask()) {
             return processSysprocFragmentTask(txnState, dependencies, fragmentId,
                                               currentFragResponse, params);
         }
         else {
             // start the clock on this statement
             ProcedureProfiler.startStatementCounter(fragmentId);
 
             if (dependencies != null) {
                 ee.stashWorkUnitDependencies(dependencies);
             }
             final int inputDepId = ftask.getOnlyInputDepId(0);
 
             /*
              * Currently the error path when executing plan fragments
              * does not adequately distinguish between fatal errors and
              * abort type errors that should result in a roll back.
              * Assume that it is ninja: succeeds or doesn't return.
              * No roll back support.
              */
             currentFragResponse.setStatus(FragmentResponseMessage.SUCCESS, null);
             try {
                 final DependencyPair dep = ee.executePlanFragment(fragmentId,
                                                                   outputDepId,
                                                                   inputDepId,
                                                                   params,
                                                                   txnState.txnId,
                                                                   lastCommittedTxnId,
                                                                   txnState.isReadOnly() ? Long.MAX_VALUE : getNextUndoToken());
 
                 sendDependency(currentFragResponse, dep.depId, dep.dependency);
 
             } catch (final EEException e) {
                 hostLog.l7dlog( Level.TRACE, LogKeys.host_ExecutionSite_ExceptionExecutingPF.name(), new Object[] { fragmentId }, e);
                 currentFragResponse.setStatus(FragmentResponseMessage.UNEXPECTED_ERROR, e);
             } catch (final SQLException e) {
                 hostLog.l7dlog( Level.TRACE, LogKeys.host_ExecutionSite_ExceptionExecutingPF.name(), new Object[] { fragmentId }, e);
                 currentFragResponse.setStatus(FragmentResponseMessage.UNEXPECTED_ERROR, e);
             }
 
             ProcedureProfiler.stopStatementCounter();
             return currentFragResponse;
         }
     }
 
 
     @Override
     public InitiateResponseMessage processInitiateTask(
             TransactionState txnState,
             final VoltMessage task)
     {
         final InitiateTaskMessage itask = (InitiateTaskMessage)task;
         final VoltProcedure wrapper = procs.get(itask.getStoredProcedureName());
 
         final InitiateResponseMessage response = new InitiateResponseMessage(itask);
 
         // feasible to receive a transaction initiated with an earlier catalog.
         if (wrapper == null) {
             response.setResults(
                 new ClientResponseImpl(ClientResponse.GRACEFUL_FAILURE,
                                        new VoltTable[] {},
                                        "Procedure does not exist: " + itask.getStoredProcedureName()));
         }
         else {
             try {
                 if (wrapper instanceof VoltSystemProcedure) {
                     Object[] callerParams = itask.getParameters();
                     Object[] combinedParams = new Object[callerParams.length + 1];
                     combinedParams[0] = m_systemProcedureContext;
                     for (int i=0; i < callerParams.length; ++i) combinedParams[i+1] = callerParams[i];
                     final ClientResponseImpl cr = wrapper.call(txnState, combinedParams);
                     response.setResults(cr, itask);
                 }
                 else {
                     final ClientResponseImpl cr = wrapper.call(txnState, itask.getParameters());
                     response.setResults(cr, itask);
                 }
             }
             catch (final ExpectedProcedureException e) {
                 log.l7dlog( Level.TRACE, LogKeys.org_voltdb_ExecutionSite_ExpectedProcedureException.name(), e);
                 response.setResults(
                                     new ClientResponseImpl(
                                                            ClientResponse.GRACEFUL_FAILURE,
                                                            new VoltTable[]{},
                                                            e.toString()));
             }
             catch (final Exception e) {
                 // Should not be able to reach here. VoltProcedure.call caught all invocation target exceptions
                 // and converted them to error responses. Java errors are re-thrown, and not caught by this
                 // exception clause. A truly unexpected exception reached this point. Crash. It's a defect.
                 hostLog.l7dlog( Level.ERROR, LogKeys.host_ExecutionSite_UnexpectedProcedureException.name(), e);
                 VoltDB.crashVoltDB();
             }
         }
         log.l7dlog( Level.TRACE, LogKeys.org_voltdb_ExecutionSite_SendingCompletedWUToDtxn.name(), null);
         return response;
     }
 
     /**
      * Try to execute a single partition procedure if one is available in the
      * priority queue.
      *
      * @return false if there is no possibility for speculative work.
      */
     public boolean tryToSneakInASinglePartitionProcedure() {
         // poll for an available message. don't block
         VoltMessage message = m_mailbox.recv();
         tick(); // unclear if this necessary (rtb)
         if (message != null) {
             handleMailboxMessage(message);
             return true;
         }
         else {
             TransactionState nextTxn = m_transactionQueue.peek();
 
             // only sneak in single partition work
             if (nextTxn instanceof SinglePartitionTxnState)
             {
                 boolean success = nextTxn.doWork(m_recovering);
                 assert(success);
                 return true;
             }
             else {
                 // multipartition is next or no work
                 return false;
             }
         }
     }
 
 
 
     /*
      *
      * Dump manager interface
      *
      */
 
     @Override
     public void goDumpYourself(final long timestamp) {
         m_currentDumpTimestamp = timestamp;
         DebugMessage dmsg = new DebugMessage();
         dmsg.shouldDump = true;
         try {
             m_mailbox.send(getSiteId(), 0, dmsg);
         }
         catch (org.voltdb.messaging.MessagingException e) {
             e.printStackTrace();
         }
     }
 
     /**
      * Get the actual file contents for a dump of state reachable by
      * this thread. Can be called unsafely or safely.
      */
     public ExecutorContext getDumpContents() {
         final ExecutorContext context = new ExecutorContext();
         context.siteId = getSiteId();
 
         // messaging log window stored in mailbox history
         if (m_mailbox instanceof SiteMailbox)
             context.mailboxHistory = ((SiteMailbox) m_mailbox).getHistory();
 
         // restricted priority queue content
         m_transactionQueue.getDumpContents(context);
 
         // TODO:
         // m_transactionsById.getDumpContents(context);
 
         return context;
     }
 }
