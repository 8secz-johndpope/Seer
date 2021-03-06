 package org.apache.jcs.auxiliary.disk.jdbc.mysql;
 
 import java.sql.Connection;
 import java.sql.ResultSet;
 import java.sql.SQLException;
 import java.sql.Statement;
 
 import org.apache.commons.logging.Log;
 import org.apache.commons.logging.LogFactory;
 import org.apache.jcs.auxiliary.disk.jdbc.JDBCDiskCacheAttributes;
 import org.apache.jcs.auxiliary.disk.jdbc.JDBCDiskCachePoolAccess;
 import org.apache.jcs.auxiliary.disk.jdbc.TableState;
 
 /**
  * The MySQL Table Optimizer can optimize MySQL tables. It knows how to optimize
  * for MySQL datbases in particular and how to repari the table if it is
  * corrupted in the process.
  * <p>
  * We will probably be able to abstract out a generic optimizer interface from
  * this class in the future.
  * <p>
  * @author Aaron Smuts
  */
 public class MySQLTableOptimizer
 {
     private final static Log log = LogFactory.getLog( MySQLTableOptimizer.class );
 
     private JDBCDiskCachePoolAccess poolAccess = null;
 
     private String tableName = null;
 
     private TableState tableState;
 
     /**
      * This constructs an optimizer with the disk cacn properties.
      * <p>
      * @param attributes
      * @param tableState
      *            We mark the table status as optimizing when this is happening.
      */
     public MySQLTableOptimizer( MySQLDiskCacheAttributes attributes, TableState tableState )
     {
         setTableName( attributes.getTableName() );
 
         this.tableState = tableState;
         /**
          * This initializes the pool access.
          */
         initializePoolAccess( attributes );
     }
 
     /**
      * Register the driver and create a pool.
      * <p>
      * @param cattr
      */
     protected void initializePoolAccess( JDBCDiskCacheAttributes cattr )
     {
         try
         {
             try
             {
                 // org.gjt.mm.mysql.Driver
                 Class.forName( cattr.getDriverClassName() );
             }
             catch ( ClassNotFoundException e )
             {
                 log.error( "Couldn't find class for driver [" + cattr.getDriverClassName() + "]", e );
             }
 
             poolAccess = new JDBCDiskCachePoolAccess( cattr.getName() );
 
             poolAccess.setupDriver( cattr.getUrl() + cattr.getDatabase(), cattr.getUserName(), cattr.getPassword(),
                                     cattr.getMaxActive() );
         }
         catch ( Exception e )
         {
             log.error( "Problem getting connection.", e );
         }
     }
 
     /**
      * A scheduler will call this method. When it is called the table state is
      * marked as optimizing. TODO we need to verify that no deletions are
      * running before we call optimize. We should wait if a deletion is in
      * progress.
      * <p>
      * This restores when there is an optimization error. The error output looks
      * like this:
      * 
      * <pre>
      *           mysql&gt; optimize table JCS_STORE_FLIGHT_OPTION_ITINERARY;
      *               +---------------------------------------------+----------+----------+---------------------+
      *               | Table                                       | Op       | Msg_type | Msg_text            |
      *               +---------------------------------------------+----------+----------+---------------------+
      *               | jcs_cache.JCS_STORE_FLIGHT_OPTION_ITINERARY | optimize | error    | 2 when fixing table |
      *               | jcs_cache.JCS_STORE_FLIGHT_OPTION_ITINERARY | optimize | status   | Operation failed    |
      *               +---------------------------------------------+----------+----------+---------------------+
      *               2 rows in set (51.78 sec)
      * </pre>
      * 
      * A successful repair response looks like this:
      * 
      * <pre>
      *        mysql&gt; REPAIR TABLE JCS_STORE_FLIGHT_OPTION_ITINERARY;
      *            +---------------------------------------------+--------+----------+----------------------------------------------+
      *            | Table                                       | Op     | Msg_type | Msg_text                                     |
      *            +---------------------------------------------+--------+----------+----------------------------------------------+
      *            | jcs_cache.JCS_STORE_FLIGHT_OPTION_ITINERARY | repair | error    | 2 when fixing table                          |
      *            | jcs_cache.JCS_STORE_FLIGHT_OPTION_ITINERARY | repair | warning  | Number of rows changed from 131276 to 260461 |
      *            | jcs_cache.JCS_STORE_FLIGHT_OPTION_ITINERARY | repair | status   | OK                                           |
      *            +---------------------------------------------+--------+----------+----------------------------------------------+
      *            3 rows in set (3 min 5.94 sec)
      * </pre>
      * 
      * A successful optimization looks like this:
      * 
      * <pre>
      *       mysql&gt; optimize table JCS_STORE_DEFAULT;
      *           +-----------------------------+----------+----------+----------+
      *           | Table                       | Op       | Msg_type | Msg_text |
      *           +-----------------------------+----------+----------+----------+
      *           | jcs_cache.JCS_STORE_DEFAULT | optimize | status   | OK       |
      *           +-----------------------------+----------+----------+----------+
      *           1 row in set (1.10 sec)
      * </pre>
      * 
      * @return
      */
     public boolean optimizeTable()
     {
         long start = System.currentTimeMillis();
         boolean success = false;
 
         if ( tableState.getState() == TableState.OPTIMIZATION_RUNNING )
         {
             log
                 .warn( "Skipping optimization.  Optimize was called, but the table state indicates that an optimization is currently running." );
             return false;
         }
 
         try
         {
             tableState.setState( TableState.OPTIMIZATION_RUNNING );
             if ( log.isInfoEnabled() )
             {
                 log.debug( "Optimizing table [" + this.getTableName() + "]" );
             }
 
             Connection con;
             try
             {
                 con = poolAccess.getConnection();
             }
             catch ( SQLException e )
             {
                 log.error( "Problem getting connection.", e );
                 return false;
             }
 
             try
             {
                 // TEST
                 Statement sStatement = null;
                 try
                 {
                     sStatement = con.createStatement();
 
                     ResultSet rs = sStatement.executeQuery( "optimize table " + this.getTableName() );
 
                     // first row is error, then status
                     // if there is only one row in the result set, everything
                     // should be fine.
                     // This may be mysql version specific.
                     if ( rs.next() )
                     {
                         String status = rs.getString( "Msg_type" );
                         String message = rs.getString( "Msg_text" );
 
                         if ( log.isInfoEnabled() )
                         {
                             log.info( "Message Type: " + status );
                             log.info( "Message: " + message );
                         }
 
                         if ( "error".equals( status ) )
                         {
                             log.warn( "Optimization was in erorr.  Will attempt to repair the table.  Message: "
                                 + message );
 
                             // try to repair the table.
                             success = repairTable( sStatement );
                         }
                     }
 
                     // log the table status
                     String statusString = getTableStatus( sStatement );
                     if ( log.isInfoEnabled() )
                     {
                         log.info( "Table status after optimizing table [" + this.getTableName() + "]\n" + statusString );
                     }
                 }
                 catch ( SQLException e )
                 {
                     log.error( "Problem optimizing table [" + this.getTableName() + "]", e );
                     return false;
                 }
                 finally
                 {
                     try
                     {
                         sStatement.close();
                     }
                     catch ( SQLException e )
                     {
                         log.error( "Problem closing statement.", e );
                     }
                 }
             }
             finally
             {
                 try
                 {
                     con.close();
                 }
                 catch ( SQLException e )
                 {
                     log.error( "Problem closing connection.", e );
                 }
             }
         }
         finally
         {
             tableState.setState( TableState.FREE );
 
             long end = System.currentTimeMillis();
             if ( log.isInfoEnabled() )
             {
                 log.info( "Optimization of table [" + this.getTableName() + "] took " + ( end - start ) + " ms." );
             }
         }
 
         return success;
     }
 
     /**
      * This calls show table status and returns the result as a String.
      * <p>
      * @param sStatement
      * @return String
      * @throws SQLException
      */
     protected String getTableStatus( Statement sStatement )
         throws SQLException
     {
         ResultSet statusResultSet = sStatement.executeQuery( "show table status" );
         StringBuffer statusString = new StringBuffer();
         int numColumns = statusResultSet.getMetaData().getColumnCount();
         while ( statusResultSet.next() )
         {
             statusString.append( "\n" );
             for ( int i = 1; i <= numColumns; i++ )
             {
                 statusString.append( statusResultSet.getMetaData().getColumnLabel( i ) + " ["
                     + statusResultSet.getString( i ) + "]  |  " );
             }
         }
         return statusString.toString();
     }
 
     /**
      * This is called if the optimizatio is in error.
      * <p>
      * It looks for "OK" in response. If it find "OK" as a message in any result
      * set row, it returns true. Otherwise we assume that the repair failed.
      * <p>
      * @param sStatement
      * @return true if successful
      * @throws SQLException
      */
     protected boolean repairTable( Statement sStatement )
         throws SQLException
     {
         boolean success = false;
 
         // if( message != null && message.indexOf( ) )
         ResultSet repairResult = sStatement.executeQuery( "repair table " + this.getTableName() );
         StringBuffer repairString = new StringBuffer();
         int numColumns = repairResult.getMetaData().getColumnCount();
         while ( repairResult.next() )
         {
             for ( int i = 1; i <= numColumns; i++ )
             {
                 repairString.append( repairResult.getMetaData().getColumnLabel( i ) + " [" + repairResult.getString( i )
                     + "]  |  " );
             }
 
             String message = repairResult.getString( "Msg_text" );
             if ( "OK".equals( message ) )
             {
                 success = true;
             }
         }
         if ( log.isInfoEnabled() )
         {
             log.info( repairString );
         }
 
         if ( !success )
         {
             log.warn( "Failed to repair the table. " + repairString );
         }
         return success;
     }
 
     /**
      * @param tableName
      *            The tableName to set.
      */
     public void setTableName( String tableName )
     {
         this.tableName = tableName;
     }
 
     /**
      * @return Returns the tableName.
      */
     public String getTableName()
     {
         return tableName;
     }
 }
