 /***************************************************************************
  *   Copyright (C) 2010 by H-Store Project                                 *
  *   Brown University                                                      *
  *   Massachusetts Institute of Technology                                 *
  *   Yale University                                                       *
  *                                                                         *
  *   Permission is hereby granted, free of charge, to any person obtaining *
  *   a copy of this software and associated documentation files (the       *
  *   "Software"), to deal in the Software without restriction, including   *
  *   without limitation the rights to use, copy, modify, merge, publish,   *
  *   distribute, sublicense, and/or sell copies of the Software, and to    *
  *   permit persons to whom the Software is furnished to do so, subject to *
  *   the following conditions:                                             *
  *                                                                         *
  *   The above copyright notice and this permission notice shall be        *
  *   included in all copies or substantial portions of the Software.       *
  *                                                                         *
  *   THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,       *
  *   EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF    *
  *   MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.*
  *   IN NO EVENT SHALL THE AUTHORS BE LIABLE FOR ANY CLAIM, DAMAGES OR     *
  *   OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, *
  *   ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR *
  *   OTHER DEALINGS IN THE SOFTWARE.                                       *
  ***************************************************************************/
 package edu.brown.benchmark.locality;
 
 import java.lang.reflect.Field;
 import java.util.*;
 import java.util.concurrent.atomic.AtomicLong;
 
 import org.apache.log4j.Logger;
 import org.voltdb.*;
 import org.voltdb.benchmark.*;
 
 import edu.brown.statistics.Histogram;
 
 public class LocalityLoader extends ClientMain {
     private static final Logger LOG = Logger.getLogger(LocalityLoader.class.getSimpleName());
     
     // Composite Id
     private static final long COMPOSITE_ID_MASK = 4294967295l; // (2^32)-1
     private static final int COMPOSITE_ID_OFFSET = 32;
     
     // scale all table cardinalities by this factor
     private int m_scalefactor = 1;
     
     // When set to true, all operations will run single-threaded
     private boolean debug = true;
     
     // Histograms
     // TableName -> Histogram for A_ID
     private final Map<String, Histogram> histograms = new HashMap<String, Histogram>();
     
     // Data Generator Classes
     // TableName -> AbstactTableGenerator
     private final Map<String, AbstractTableGenerator> generators = new HashMap<String, AbstractTableGenerator>();
     
     // Table Sizes
     // TableName -> Tuple Count
     private final Map<String, AtomicLong> table_sizes = new HashMap<String, AtomicLong>();
     
     public static void main(String args[]) throws Exception {
         org.voltdb.benchmark.ClientMain.main(LocalityLoader.class, args, true);
     }
     
     /**
      * Constructor
      * @param args
      */
     public LocalityLoader(String[] args) {
         super(args);
         
        for (String arg : args) {
            String[] parts = arg.split("=",2);
            if (parts.length == 1)
                continue;
            
            if (parts[1].startsWith("${"))
                continue;
            
            if (parts[0].equals("scalefactor")) {
                m_scalefactor = Integer.parseInt(parts[1]);
            } else if (parts[0].equals("debug")) {
                this.debug = Boolean.getBoolean(parts[1]);
             }
         } // FOR
         
         // Histograms + Table Sizes + Generators
         for (String tableName : LocalityConstants.TABLENAMES) {
             this.histograms.put(tableName, new Histogram());
             this.table_sizes.put(tableName, new AtomicLong(0l));
             
             if (tableName.equals(LocalityConstants.TABLENAME_TABLEA)) {
                 this.generators.put(tableName, new TABLEAGenerator());
             } else if (tableName.equals(LocalityConstants.TABLENAME_TABLEB)) {
                 this.generators.put(tableName, new TABLEBGenerator());
             }
         } // FOR
     }
     
     @Override
     public String[] getTransactionDisplayNames() {
         return new String[] {};
     }
 
     /**
      * Main execution loop for invoking all the data generator threads
      */
     @Override
     public void runLoop() {
         List<Thread> load_threads = new ArrayList<Thread>();
         for (final String tableName : LocalityConstants.TABLENAMES) {
             load_threads.add(new Thread() { 
                 @Override
                 public void run() {
                     generateTableData(tableName);
                 }
             });
         } // FOR
         
         try {
             for (Thread thread : load_threads) {
                 thread.start();
                 if (this.debug) thread.join();
             }
             if (!this.debug) {
                 for (Thread thread : load_threads) thread.join();
             }
         } catch (InterruptedException e) {
             e.printStackTrace();
             System.exit(-1);
         }
 
         System.err.println("Finished generating data for all tables");
     }
     
     /**
      * Load the tuples for the given table name
      * @param tableName
      */
     protected void generateTableData(String tableName) {
         System.out.println("Starting data generator for '" + tableName + "'");
         final AbstractTableGenerator generator = this.generators.get(tableName);
         assert(generator != null);
         long tableSize = generator.getTableSize();
         long batchSize = generator.getBatchSize();
         VoltTable table = generator.getVoltTable();
         
         System.out.println("Loading " + tableSize + " tuples for table '" + tableName + "'");
         while (generator.hasMore()) {
             generator.addRow();
             if (table.getRowCount() >= batchSize) {
                 System.err.println(String.format(tableName + ": loading %d rows (id %d of %d)", table.getRowCount(), generator.getCount(), tableSize));
                 loadTable(tableName, table);
                 this.table_sizes.get(tableName).addAndGet(table.getRowCount());
                 table.clearRowData();
             }
         } // WHILE
         if (table.getRowCount() > 0) {
             System.err.println(tableName + ": loading final " + table.getRowCount() + " rows.");
             loadTable(tableName, table);
             this.table_sizes.get(tableName).addAndGet(table.getRowCount());
             table.clearRowData();
         }
         System.out.println(tableName + ": Inserted " + this.table_sizes.get(tableName) + " tuples");
     }
     
     /**
      * 
      * @param debug
      */
     protected void setDebug(boolean debug) {
         this.debug = debug;
     }
     
     protected static Long encodeCompositeId(long a_id, long id) {
         return (a_id | id<<COMPOSITE_ID_OFFSET);
     }
     
     /**
      * Returns the pieces of a composite id
      * The first element of the returned array will be the A_ID portion of the 
      * composite and the second element will be the ID portion 
      * @param composite_id
      * @return
      */
     protected static long[] decodeCompositeId(long composite_id) {
         long values[] = { composite_id & COMPOSITE_ID_MASK,
                           composite_id>>COMPOSITE_ID_OFFSET };
         return (values);
     }
 
     // ----------------------------------------------------------------
     // DATA GENERATION
     // ----------------------------------------------------------------
     
     protected abstract class AbstractTableGenerator {
         protected final String tableName;
         protected final VoltTable table;
         protected final Histogram hist;
         protected Long tableSize;
         protected Long batchSize;
         
         protected final Object[] row;
         protected long count = 0;
         
         public AbstractTableGenerator(String tableName, VoltTable table) {
             this.tableName = tableName;
             this.table = table;
             this.hist = LocalityLoader.this.histograms.get(this.tableName);
             assert(hist != null);
             
             this.row = new Object[this.table.getColumnCount()];
             
             // Initialize dynamic parameters
             try {
                 String field_name = "TABLESIZE_" + tableName;
                 Field field_handle = LocalityConstants.class.getField(field_name);
                 assert(field_handle != null);
                 this.tableSize = (Long)field_handle.get(null) / LocalityLoader.this.m_scalefactor;
 
                 field_name = "BATCHSIZE_" + tableName;
                 field_handle = LocalityConstants.class.getField(field_name);
                 assert(field_handle != null);
                 this.batchSize = (Long)field_handle.get(null);
             } catch (Exception ex) {
                 LOG.error(ex);
                 System.exit(1);
             }
             System.out.println("Preparing to load " + this.tableSize + " tuples for '" + this.tableName + "' [batchSize=" + this.batchSize + "]");
         }
         
         public boolean hasMore() {
             return (this.count < this.tableSize);
         }
         
         public VoltTable getVoltTable() {
             return this.table;
         }
         
         public Long getTableSize() {
             return this.tableSize;
         }
         
         public Long getBatchSize() {
             return this.batchSize;
         }
         
         public String getTableName() {
             return this.tableName;
         }
         
         public long getCount() {
             return this.count;
         }
         
         /**
          * Invoked by generateTableData() to create a new row in our temporary
          * table. We don't need to worry about batches, counts, or anything else. Just makin' tuples
          */
         public void addRow() {
             this.populateRow();
             this.count++;
             this.table.addRow(this.row);
         }
         
         protected abstract void populateRow();
     } // END CLASS
     
     /**
      * TABLEA Generator
      */
     protected class TABLEAGenerator extends AbstractTableGenerator {
         
         public TABLEAGenerator() {
             super(LocalityConstants.TABLENAME_TABLEA, LocalityTables.initializeTableA());
         }
         
         @Override
         protected void populateRow() {
             int col = 0;
             
             // A_ID
             row[col++] = new Integer((int)this.count);
             
             // A_VALUE
             row[col++] = "ABC123"; // FIXME
 
             assert (col == this.table.getColumnCount());
         }
     } // END CLASS
 
     /**
      * TABLEB Generator
      */
     protected class TABLEBGenerator extends AbstractTableGenerator {
     	private long current_a_id = 0;
     	private long current_b_id = 0;
     	
         public TABLEBGenerator() {
             super(LocalityConstants.TABLENAME_TABLEB, LocalityTables.initializeTableB());
         }
         
         @Override
         protected void populateRow() {
         	int col = 0;
         	
             // B_ID
             row[col++] = new Integer((int)this.current_b_id);
         	
             // B_A_ID
             row[col++] = new Integer((int)this.current_a_id);
 
             // B_VALUE
             row[col++] = "DEF456"; // FIXME
             
         	assert (col == this.table.getColumnCount());
         	
         	if (++this.current_b_id > LocalityConstants.TABLESIZE_TABLEB_MULTIPLIER) {
         		this.current_b_id = 0;
         		this.current_a_id++;
         	}
         }
     } // END CLASS
         
     /**
      * 
      * @param tablename
      * @param table
      */
     protected void loadTable(String tablename, VoltTable table) {
         // System.out.println("Loading " + table.getRowCount() + " tuples for table " + tablename + " [bytes=" + table.getUnderlyingBufferSize() + "]");
     
 //    	System.err.println(table);
 //    	System.exit(1);
     	
         // Load up this dirty mess...
         try {
             m_voltClient.callProcedure("@LoadMultipartitionTable", tablename, table);
         } catch (Exception e) {
             e.printStackTrace();
             System.exit(-1);
         }
     }
     
     @Override
     public String getApplicationName() {
         return "Locality Benchmark";
     }
 
     @Override
     public String getSubApplicationName() {
         return "Loader";
     }
 }
 
