 /*
  * HA-JDBC: High-Availability JDBC
  * Copyright 2004-2009 Paul Ferraro
  * 
  * This program is free software: you can redistribute it and/or modify
  * it under the terms of the GNU Lesser General Public License as published by
  * the Free Software Foundation, either version 3 of the License, or
  * (at your option) any later version.
  * 
  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  * GNU Lesser General Public License for more details.
  * 
  * You should have received a copy of the GNU Lesser General Public License
  * along with this program.  If not, see <http://www.gnu.org/licenses/>.
  */
 package net.sf.hajdbc.sync;
 
 import java.io.Serializable;
 import java.sql.Connection;
 import java.sql.PreparedStatement;
 import java.sql.ResultSet;
 import java.sql.SQLException;
 import java.sql.Statement;
 import java.util.Collection;
 import java.util.Collections;
 import java.util.concurrent.Callable;
 import java.util.concurrent.ExecutionException;
 import java.util.concurrent.ExecutorService;
 import java.util.concurrent.Future;
 
 import net.sf.hajdbc.Database;
 import net.sf.hajdbc.Dialect;
 import net.sf.hajdbc.Messages;
 import net.sf.hajdbc.SynchronizationContext;
 import net.sf.hajdbc.SynchronizationStrategy;
 import net.sf.hajdbc.cache.TableProperties;
 import net.sf.hajdbc.logging.Level;
 import net.sf.hajdbc.logging.Logger;
 import net.sf.hajdbc.logging.LoggerFactory;
 import net.sf.hajdbc.sql.SQLExceptionFactory;
 import net.sf.hajdbc.util.Strings;
 
 /**
  * Database-independent synchronization strategy that does full record transfer between two databases.
  * This strategy is best used when there are <em>many</em> differences between the active database and the inactive database (i.e. very much out of sync).
  * The following algorithm is used:
  * <ol>
  *  <li>Drop the foreign keys on the inactive database (to avoid integrity constraint violations)</li>
  *  <li>For each database table:
  *   <ol>
  *    <li>Delete all rows in the inactive database table</li>
  *    <li>Query all rows on the active database table</li>
  *    <li>For each row in active database table:
  *     <ol>
  *      <li>Insert new row into inactive database table</li>
  *     </ol>
  *    </li>
  *   </ol>
  *  </li>
  *  <li>Re-create the foreign keys on the inactive database</li>
  *  <li>Synchronize sequences</li>
  * </ol>
  * @author  Paul Ferraro
  */
 public class FullSynchronizationStrategy implements SynchronizationStrategy, Serializable
 {
 	private static final long serialVersionUID = 9190347092842178162L;
 
 	private static Logger logger = LoggerFactory.getLogger(FullSynchronizationStrategy.class);
 
 	private int maxBatchSize = 100;
 	private int fetchSize = 0;
 	
 	/**
 	 * @see net.sf.hajdbc.SynchronizationStrategy#synchronize(net.sf.hajdbc.SynchronizationContext)
 	 */
 	@Override
 	public <P, D extends Database<P>> void synchronize(SynchronizationContext<P, D> context) throws SQLException
 	{
 		Connection sourceConnection = context.getConnection(context.getSourceDatabase());
 		Connection targetConnection = context.getConnection(context.getTargetDatabase());
 
 		Dialect dialect = context.getDialect();
 		ExecutorService executor = context.getExecutor();
 		
 		boolean sourceAutoCommit = sourceConnection.getAutoCommit();
 		boolean targetAutoCommit = targetConnection.getAutoCommit();
 		
 		targetConnection.setAutoCommit(true);
 		
 		SynchronizationSupport.dropForeignKeys(context);
 		
 		sourceConnection.setAutoCommit(false);
 		targetConnection.setAutoCommit(false);
 		
 		try
 		{
 			for (TableProperties table: context.getSourceDatabaseProperties().getTables())
 			{
 				String tableName = table.getName();
 				Collection<String> columns = table.getColumns();
 				
 				String commaDelimitedColumns = Strings.join(columns, Strings.PADDED_COMMA);
 				
 				final String selectSQL = "SELECT " + commaDelimitedColumns + " FROM " + tableName; //$NON-NLS-1$ //$NON-NLS-2$
 				
 				final Statement selectStatement = sourceConnection.createStatement();
 				selectStatement.setFetchSize(this.fetchSize);
 				
 				Callable<ResultSet> callable = new Callable<ResultSet>()
 				{
 					public ResultSet call() throws SQLException
 					{
 						return selectStatement.executeQuery(selectSQL);
 					}
 				};
 	
 				Future<ResultSet> future = executor.submit(callable);
 				
 				String deleteSQL = dialect.getTruncateTableSQL(table);
 
 				logger.log(Level.DEBUG, deleteSQL);
 				
 				Statement deleteStatement = targetConnection.createStatement();
 	
 				int deletedRows = deleteStatement.executeUpdate(deleteSQL);
 
 				logger.log(Level.INFO, Messages.DELETE_COUNT.getMessage(), deletedRows, tableName);
 				
 				deleteStatement.close();
 				
 				ResultSet resultSet = future.get();
 				
 				String insertSQL = "INSERT INTO " + tableName + " (" + commaDelimitedColumns + ") VALUES (" + Strings.join(Collections.nCopies(columns.size(), Strings.QUESTION), Strings.PADDED_COMMA) + ")"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
 
 				logger.log(Level.DEBUG, insertSQL);
 				
 				PreparedStatement insertStatement = targetConnection.prepareStatement(insertSQL);
 				int statementCount = 0;
 				
 				while (resultSet.next())
 				{
 					int index = 0;
 					
 					for (String column: columns)
 					{
 						index += 1;
 						
 						int type = dialect.getColumnType(table.getColumnProperties(column));
 						
 						Object object = SynchronizationSupport.getObject(resultSet, index, type);
 						
 						if (resultSet.wasNull())
 						{
 							insertStatement.setNull(index, type);
 						}
 						else
 						{
 							insertStatement.setObject(index, object, type);
 						}
 					}
 					
 					insertStatement.addBatch();
 					statementCount += 1;
 					
 					if ((statementCount % this.maxBatchSize) == 0)
 					{
 						insertStatement.executeBatch();
 						insertStatement.clearBatch();
 					}
 					
 					insertStatement.clearParameters();
 				}
 	
 				if ((statementCount % this.maxBatchSize) > 0)
 				{
 					insertStatement.executeBatch();
 				}
 
 				logger.log(Level.INFO, Messages.INSERT_COUNT.getMessage(), statementCount, tableName);
 				
 				insertStatement.close();
 				selectStatement.close();
 				
 				targetConnection.commit();
 			}
 		}
 		catch (InterruptedException e)
 		{
 			SynchronizationSupport.rollback(targetConnection);
 
 			throw SQLExceptionFactory.getInstance().createException(e);
 		}
 		catch (ExecutionException e)
 		{
 			SynchronizationSupport.rollback(targetConnection);
 
 			throw SQLExceptionFactory.getInstance().createException(e.getCause());
 		}
 		catch (SQLException e)
 		{
 			SynchronizationSupport.rollback(targetConnection);
 			
 			throw e;
 		}
 		
 		targetConnection.setAutoCommit(true);
 		
 		SynchronizationSupport.restoreForeignKeys(context);
 		
 		SynchronizationSupport.synchronizeIdentityColumns(context);
 		SynchronizationSupport.synchronizeSequences(context);
 		
 		sourceConnection.setAutoCommit(sourceAutoCommit);
 		targetConnection.setAutoCommit(targetAutoCommit);
 	}
 
 	/**
 	 * @return the fetchSize.
 	 */
 	public int getFetchSize()
 	{
 		return this.fetchSize;
 	}
 
 	/**
 	 * @param fetchSize the fetchSize to set.
 	 */
 	public void setFetchSize(int fetchSize)
 	{
 		this.fetchSize = fetchSize;
 	}
 	
 	/**
 	 * @return the maxBatchSize.
 	 */
 	public int getMaxBatchSize()
 	{
 		return this.maxBatchSize;
 	}
 
 	/**
 	 * @param maxBatchSize the maxBatchSize to set.
 	 */
 	public void setMaxBatchSize(int maxBatchSize)
 	{
 		this.maxBatchSize = maxBatchSize;
 	}
 }
