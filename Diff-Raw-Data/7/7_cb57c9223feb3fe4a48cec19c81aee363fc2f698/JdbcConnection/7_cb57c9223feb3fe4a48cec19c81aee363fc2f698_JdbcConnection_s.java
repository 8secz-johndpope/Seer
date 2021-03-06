 /*
  * RapidContext JDBC plug-in <http://www.rapidcontext.com/>
  * Copyright (c) 2007-2009 Per Cederberg. All rights reserved.
  *
  * This program is free software: you can redistribute it and/or modify
  * it under the terms of the GNU General Public License as published by
  * the Free Software Foundation, either version 3 of the License, or
  * (at your option) any later version.
  *
  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  * GNU General Public License for more details.
  *
  * You should have received a copy of the GNU General Public License
  * along with this program.  If not, see <http://www.gnu.org/licenses/>.
  */
 
 package org.rapidcontext.app.plugin.jdbc;
 
 import java.io.StringReader;
 import java.sql.Connection;
 import java.sql.Driver;
 import java.sql.DriverManager;
 import java.sql.PreparedStatement;
 import java.sql.ResultSet;
 import java.sql.ResultSetMetaData;
 import java.sql.SQLException;
 import java.sql.Types;
 import java.util.ArrayList;
 import java.util.Properties;
 
 import org.rapidcontext.core.data.Data;
 import org.rapidcontext.core.env.AdapterConnection;
 import org.rapidcontext.core.env.AdapterException;
 import org.rapidcontext.util.DateUtil;
 
 /**
  * A JDBC adapter connection. This class encapsulates a JDBC
  * connection and allows execution of arbitrary SQL queries or
  * statements.
  *
  * @author   Per Cederberg
  * @version  1.0
  */
 class JdbcConnection implements AdapterConnection {
 
     /**
      * The encapsulated JDBC connection.
      */
     private Connection con;
 
     /**
      * The SQL ping query.
      */
     private String sqlPing;
 
     /**
      * The SQL connection and query timeout.
      */
     private int timeout;
 
     /**
      * Creates a new JDBC connection.
      *
      * @param driver            the JDBC driver
      * @param url               the connection URL
      * @param props             the connection properties (user and password)
      * @param sqlPing           the SQL ping query
      * @param autoCommit        the auto-commit flag
      * @param timeout           the request timeout (in secs)
      *
      * @throws AdapterException if a connection couldn't be established
      */
     JdbcConnection(Driver driver,
                    String url,
                    Properties props,
                    String sqlPing,
                    boolean autoCommit,
                    int timeout)
         throws AdapterException {
 
         String  msg;
 
         try {
             DriverManager.setLoginTimeout(timeout);
             con = driver.connect(url, props);
             con.setAutoCommit(autoCommit);
         } catch (SQLException e) {
             msg = "failed to connect to " + url + " with username '" +
                   props.getProperty("user") + "': " + e.getMessage();
             throw new AdapterException(msg);
         }
         this.timeout = timeout;
         this.sqlPing = sqlPing;
     }
 
     /**
      * Activates the connection. This method is called just before a
      * connection is to be used, i.e. when a new connection has been
      * created or when fetched from a resource pool. It can also be
      * called to trigger a "ping" of a connection, if such
      * functionality is implemented by the adapter.
      *
      * @throws AdapterException if the connection couldn't be
      *             activated (connection will be closed)
      */
     public void activate() throws AdapterException {
         String  msg;
 
         if (sqlPing != null) {
             executeQuery(sqlPing);
         } else {
             try {
                 if (con.isClosed()) {
                     msg = "failed to activate, connection already closed";
                     throw new AdapterException(msg);
                 }
             } catch (SQLException e) {
                 msg = "failed to activate: " + e.getMessage();
                 throw new AdapterException(msg);
             }
         }
     }
 
     /**
      * Passivates the connection. This method is called just after a
      * connection has been used and will be returned to the pool.
      * This operation should clear or reset the connection, so that
      * it can safely be used again at a later time without affecting
      * previous results or operations.
      *
      * @throws AdapterException if the connection couldn't be
      *             passivated (connection will be closed)
      */
     public void passivate() throws AdapterException {
         commit();
     }
 
     /**
      * Closes the connection. This method is used to free any
      * resources used by the connection.  After this method has been
      * called, no further calls will be made to this connection.
      *
      * @throws AdapterException if the connection couldn't be closed
      *             properly (connection discarded anyway)
      */
     public void close() throws AdapterException {
         try {
             con.close();
         } catch (SQLException e) {
             throw new AdapterException(e.getMessage());
         }
     }
 
     /**
      * Commits any pending changes. This method is called after each
      * successful procedure tree execution that included this
      * connection. This method may be implemented as a no-op, if
      * the adapter does not support commit and rollback semantics.
      *
      * @throws AdapterException if the pending changes couldn't be
      *             committed to permanent storage (connection will be
      *             closed)
      */
     public void commit() throws AdapterException {
         try {
             con.commit();
         } catch (SQLException e) {
             throw new AdapterException(e.getMessage());
         }
     }
 
     /**
      * Rolls any pending changes back. This method is called after an
      * unsuccessful procedure tree execution that included this
      * connection. This method may be implemented as a no-op, if the
      * adapter does not support commit and rollback semantics.
      *
      * @throws AdapterException if the pending changes couldn't be
      *             rolled back (connection will be closed)
      */
     public void rollback() throws AdapterException {
         try {
             con.rollback();
         } catch (SQLException e) {
             throw new AdapterException(e.getMessage());
         }
     }
 
     /**
      * Executes an SQL statement.
      *
      * @param sql            the SQL statement to execute
      *
      * @return the data list with generated keys
      *
      * @throws AdapterException if the execution failed
      */
     public Data executeStatement(String sql) throws AdapterException {
         return executeStatement(prepare(sql, null));
     }
 
     /**
      * Executes an SQL prepared statement. The statement will be
      * closed by this method.
      *
      * @param stmt           the prepared SQL statement to execute
      *
      * @return the data list with generated keys
      *
      * @throws AdapterException if the execution failed
      */
     public Data executeStatement(PreparedStatement stmt)
         throws AdapterException {
 
         Data       res = new Data(10);
         ResultSet  set = null;
 
         try {
             stmt.executeUpdate();
             try {
                 set = stmt.getGeneratedKeys();
                 while (set.next()) {
                     res.add(set.getString(1));
                 }
             } catch (SQLException ignore) {
                 // Ignore errors on generated keys
             }
             return res;
         } catch (SQLException e) {
             throw new AdapterException("failed to execute statement: " +
                                        e.getMessage());
         } finally {
             try {
                 if (set != null) {
                     set.close();
                 }
                 stmt.close();
             } catch (SQLException ignore) {
                 // Do nothing
             }
         }
     }
 
     /**
      * Executes an SQL query. Default object mapping will be used,
      * which means that column meta-data will be included and column
      * names will be mapped into object properties.
      *
      * @param sql            the SQL query to execute
      *
      * @return the data object with all the result data
      *
      * @throws AdapterException if the execution failed
      */
     public Data executeQuery(String sql) throws AdapterException {
         return executeQuery(prepare(sql, null));
     }
 
     /**
      * Executes a prepared SQL query. The prepared statement will be
      * closed by this method. Default object mapping will be used,
      * which means that column meta-data will be included and column
      * names will be mapped into object properties.
      *
      * @param stmt           the prepared SQL query to execute
      *
      * @return the data object with all the result data
      *
      * @throws AdapterException if the execution failed
      */
     public Data executeQuery(PreparedStatement stmt)
         throws AdapterException {
 
         return executeQuery(stmt, true, true);
     }
 
     /**
      * Executes a prepared SQL query. The prepared statement will be
      * closed by this method.
      *
      * @param stmt           the prepared SQL query to execute
      * @param mapMetadata    the meta-data mapping flag
      * @param mapColumnName  the column name to property mapping flag
      *
      * @return the data object with all the result data
      *
      * @throws AdapterException if the execution failed
      */
     public Data executeQuery(PreparedStatement stmt,
                              boolean mapMetadata,
                              boolean mapColumnName)
         throws AdapterException {
 
         ResultSet  set = null;
 
         try {
             set = stmt.executeQuery();
             return createResults(set, mapMetadata, mapColumnName);
         } catch (SQLException e) {
             throw new AdapterException("failed to execute query: " +
                                        e.getMessage());
         } finally {
             try {
                 if (set != null) {
                     set.close();
                 }
                 stmt.close();
             } catch (SQLException ignore) {
                 // Do nothing
             }
         }
     }
 
     /**
      * Prepares an SQL statement.
      *
      * @param sql            the SQL statement to prepare
      * @param params         the optional list of parameters
      *
      * @return the prepared SQL statement
      *
       @throws AdapterException if the statement couldn't be prepared
      */
     public PreparedStatement prepare(String sql, ArrayList params)
         throws AdapterException {
 
         PreparedStatement  stmt;
         Object             obj;
         String             str;
 
         try {
             stmt = con.prepareStatement(sql,
                                         ResultSet.TYPE_FORWARD_ONLY,
                                         ResultSet.CONCUR_READ_ONLY,
                                         ResultSet.CLOSE_CURSORS_AT_COMMIT);
             for (int i = 0; params != null && i < params.size(); i++) {
                 obj = params.get(i);
                 if (obj instanceof String && ((String) obj).length() > 255) {
                     str = (String) params.get(i);
                     stmt.setCharacterStream(i + 1,
                                             new StringReader(str),
                                             str.length());
                 } else {
                     stmt.setObject(i + 1, obj);
                 }
             }
             stmt.setQueryTimeout(timeout);
             stmt.setFetchSize(Integer.MIN_VALUE);
             return stmt;
         } catch (SQLException e) {
             str = "failed to prepare SQL: " + e.getMessage();
             throw new AdapterException(str);
         }
     }
 
     /**
      * Converts a query result set to a data object.
      *
      * @param rs             the result set to convert
      * @param mapMetadata    the meta-data mapping flag
      * @param mapColumnName  the column name to property mapping flag
      *
      * @return the data object with all the result data
      *
      * @throws AdapterException if the result data couldn't be read
      */
     private Data createResults(ResultSet rs,
                                boolean mapMetadata,
                                boolean mapColumnName)
         throws AdapterException {
 
         Data               res = new Data();
         Data               cols;
         Data               rows = new Data(10);
         Data               obj;
         ResultSetMetaData  meta;
         int                colCount;
         String             value;
 
         try {
             meta = rs.getMetaData();
             colCount = meta.getColumnCount();
             cols = new Data(colCount);
             for (int i = 0; i < colCount; i++) {
                 obj = new Data();
                 obj.set("name", meta.getColumnName(i + 1).toLowerCase());
                 obj.set("catalog", meta.getCatalogName(i + 1));
                 obj.set("type", meta.getColumnTypeName(i + 1));
                 obj.setInt("jdbcType", meta.getColumnType(i + 1));
                 obj.set("schema", meta.getSchemaName(i + 1));
                 obj.set("table", meta.getTableName(i + 1));
                 obj.set("column", meta.getColumnName(i + 1));
                 cols.add(obj);
             }
             while (rs.next()) {
                 obj = mapColumnName ? new Data() : new Data(colCount);
                 for (int i = 0; i < colCount; i++) {
                     switch (meta.getColumnType(i + 1)) {
                     case Types.DATE:
                     case Types.TIMESTAMP:
                        value = DateUtil.formatIsoDateTime(rs.getTimestamp(i + 1));
                         break;
                     default:
                         value = rs.getString(i + 1);
                     }
                     if (mapColumnName) {
                         obj.set(meta.getColumnName(i + 1).toLowerCase(), value);
                     } else {
                         obj.set(i, value);
                     }
                 }
                 rows.add(obj);
             }
             res.set("columns", cols);
             res.set("rows", rows);
         } catch (SQLException e) {
             throw new AdapterException("failed to extract query results: " +
                                        e.getMessage());
         }
         return mapMetadata ? res : rows;
     }
 }
