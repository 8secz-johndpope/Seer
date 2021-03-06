 /*
  * HA-JDBC: High-Availability JDBC
  * Copyright (c) 2004-2006 Paul Ferraro
  * 
  * This library is free software; you can redistribute it and/or modify it 
  * under the terms of the GNU Lesser General Public License as published by the 
  * Free Software Foundation; either version 2.1 of the License, or (at your 
  * option) any later version.
  * 
  * This library is distributed in the hope that it will be useful, but WITHOUT
  * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or 
  * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License 
  * for more details.
  * 
  * You should have received a copy of the GNU Lesser General Public License
  * along with this library; if not, write to the Free Software Foundation, 
  * Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
  * 
  * Contact: ferraro@users.sourceforge.net
  */
 package net.sf.hajdbc.sync;
 
 import java.sql.Connection;
 import java.sql.ResultSet;
 import java.sql.SQLException;
 import java.sql.Statement;
 import java.sql.Types;
 import java.util.ArrayList;
 import java.util.Collection;
 import java.util.HashMap;
 import java.util.Map;
 import java.util.concurrent.Callable;
 import java.util.concurrent.ExecutionException;
 import java.util.concurrent.ExecutorService;
 import java.util.concurrent.Future;
 
 import net.sf.hajdbc.Database;
 import net.sf.hajdbc.Dialect;
 import net.sf.hajdbc.ForeignKeyConstraint;
 import net.sf.hajdbc.Messages;
 import net.sf.hajdbc.SynchronizationContext;
 import net.sf.hajdbc.TableProperties;
 import net.sf.hajdbc.UniqueConstraint;
 
 import org.slf4j.Logger;
 import org.slf4j.LoggerFactory;
 
 /**
  * @author Paul Ferraro
  *
  */
 public final class SynchronizationSupport
 {
 	private static Logger logger = LoggerFactory.getLogger(SynchronizationSupport.class);
 	
 	private SynchronizationSupport()
 	{
 		// Hide
 	}
 	
 	/**
 	 * Drop all foreign key constraints on the target database
 	 * @param context a synchronization context
 	 * @throws SQLException if database error occurs
 	 */
 	public static void dropForeignKeys(SynchronizationContext context) throws SQLException
 	{
 		Connection connection = context.getConnection(context.getTargetDatabase());
 		
 		Collection<TableProperties> tables = context.getDatabaseMetaDataCache().getDatabaseProperties(connection).getTables();
 		
 		Dialect dialect = context.getDialect();
 		
 		Statement statement = connection.createStatement();
 		
 		for (TableProperties table: tables)
 		{
 			for (ForeignKeyConstraint constraint: table.getForeignKeyConstraints())
 			{
 				String sql = dialect.getDropForeignKeyConstraintSQL(constraint);
 				
 				logger.debug(sql);
 				
 				statement.addBatch(sql);
 			}
 		}
 		
 		statement.executeBatch();
 		statement.close();
 	}
 	
 	/**
 	 * Restores all foreign key constraints on the target database
 	 * @param context a synchronization context
 	 * @throws SQLException if database error occurs
 	 */
 	public static void restoreForeignKeys(SynchronizationContext context) throws SQLException
 	{
 		Connection connection = context.getConnection(context.getTargetDatabase());
 		
 		Collection<TableProperties> tables = context.getDatabaseMetaDataCache().getDatabaseProperties(connection).getTables();
 		
 		Dialect dialect = context.getDialect();
 		
 		Statement statement = connection.createStatement();
 		
 		for (TableProperties table: tables)
 		{
 			for (ForeignKeyConstraint constraint: table.getForeignKeyConstraints())
 			{
 				String sql = dialect.getCreateForeignKeyConstraintSQL(constraint);
 				
 				logger.debug(sql);
 				
 				statement.addBatch(sql);
 			}
 		}
 		
 		statement.executeBatch();
 		statement.close();
 	}
 	
 	/**
 	 * Synchronizes the sequences on the target database with the source database.
 	 * @param context a synchronization context
 	 * @throws SQLException if database error occurs
 	 */
 	public static void synchronizeSequences(final SynchronizationContext context) throws SQLException
 	{
 		Database sourceDatabase = context.getSourceDatabase();		
 		Connection sourceConnection = context.getConnection(sourceDatabase);
 		
 		Dialect dialect = context.getDialect();
 		
 		Map<String, Long> sequenceMap = new HashMap<String, Long>();
 
 		Collection<String> sequences = dialect.getSequences(sourceConnection);
 		Collection<Database> databases = context.getActiveDatabases();
 
 		ExecutorService executor = context.getExecutor();
 		
 		Map<Database, Future<Long>> futureMap = new HashMap<Database, Future<Long>>();
 
 		for (String sequence: sequences)
 		{
 			final String sql = dialect.getNextSequenceValueSQL(sequence);
 			
 			logger.debug(sql);
 
 			for (final Database database: databases)
 			{
 				Callable<Long> task = new Callable<Long>()
 				{
 					public Long call() throws SQLException
 					{
 						Statement statement = context.getConnection(database).createStatement();
 						ResultSet resultSet = statement.executeQuery(sql);
 						
 						resultSet.next();
 						
 						long value = resultSet.getLong(1);
 						
 						resultSet.close();
 						statement.close();
 						
 						return value;
 					}
 				};
 				
 				futureMap.put(database, executor.submit(task));				
 			}
 
 			try
 			{
 				Long sourceValue = futureMap.get(sourceDatabase).get();
 				
 				sequenceMap.put(sequence, sourceValue);
 				
 				for (Database database: databases)
 				{
 					if (!database.equals(sourceDatabase))
 					{
 						Long value = futureMap.get(database).get();
 						
 						if (!value.equals(sourceValue))
 						{
 							throw new SQLException(Messages.getMessage(Messages.SEQUENCE_OUT_OF_SYNC, sequence, database, value, sourceDatabase, sourceValue));
 						}
 					}
 				}
 			}
 			catch (InterruptedException e)
 			{
 				throw new net.sf.hajdbc.SQLException(e);
 			}
 			catch (ExecutionException e)
 			{
 				throw new net.sf.hajdbc.SQLException(e);
 			}
 		}
 		
 		Connection targetConnection = context.getConnection(context.getTargetDatabase());
 		Statement targetStatement = targetConnection.createStatement();
 
 		for (String sequence: sequences)
 		{
 			String sql = dialect.getAlterSequenceSQL(sequence, sequenceMap.get(sequence) + 1);
 			
 			logger.debug(sql);
 			
 			targetStatement.addBatch(sql);
 		}
 		
 		targetStatement.executeBatch();		
 		targetStatement.close();
 	}
 	
 	/**
 	 * Read-locks all of the tables in each active database.
 	 * @param context a synchronization context
 	 * @throws SQLException if database error occurs
 	 */
 	public static void lock(final SynchronizationContext context) throws SQLException
 	{
 		logger.info(Messages.getMessage(Messages.TABLE_LOCK_ACQUIRE));
 		
 		Connection targetConnection = context.getConnection(context.getTargetDatabase());
 		
 		Collection<TableProperties> tables = context.getDatabaseMetaDataCache().getDatabaseProperties(targetConnection).getTables();
 		
 		Dialect dialect = context.getDialect();
 		
 		Collection<Database> databases = context.getActiveDatabases();
 		
 		ExecutorService executor = context.getExecutor();
 		
 		Collection<Future<Void>> futures = new ArrayList<Future<Void>>(databases.size());
 		
 		for (TableProperties table: tables)
 		{
 			final String sql = dialect.getLockTableSQL(table);
 			
 			for (final Database database: databases)
 			{
 				Callable<Void> task = new Callable<Void>()
 				{
 					public Void call() throws SQLException
 					{
 						Connection connection = context.getConnection(database);
 						
 						connection.setAutoCommit(false);
 						connection.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
 						
 						Statement statement = connection.createStatement();
 						
 						statement.execute(sql);
 						
 						statement.close();
 						
 						return null;
 					}
 				};
 				
 				futures.add(executor.submit(task));
 			}
 			
 			try
 			{
 				for (Future<Void> future: futures)
 				{
 					future.get();
 				}
 			}
 			catch (InterruptedException e)
 			{
 				throw new net.sf.hajdbc.SQLException(e);
 			}
 			catch (ExecutionException e)
 			{
 				throw new net.sf.hajdbc.SQLException(e);
 			}
 		}
 	}
 	
 	public static void unlock(final SynchronizationContext context)
 	{
 		Collection<Database> databases = context.getActiveDatabases();
 		
 		ExecutorService executor = context.getExecutor();
 		
 		Collection<Future<Void>> futures = new ArrayList<Future<Void>>(databases.size());
 		
		for (final Database database: databases)
 		{
 			Callable<Void> task = new Callable<Void>()
 			{
 				public Void call() throws SQLException
 				{
 					Connection connection = context.getConnection(database);
 					
 					SynchronizationSupport.rollback(connection);
 					
 					return null;
 				}
 			};
 			
 			futures.add(executor.submit(task));
 		}
 		
 		try
 		{
 			for (Future<Void> future: futures)
 			{
 				future.get();
 			}
 		}
 		catch (InterruptedException e)
 		{
 			logger.warn(e.toString(), e);
 		}
 		catch (ExecutionException e)
 		{
 			logger.warn(e.toString(), e);
 		}
 	}
 	
 	public static void dropUniqueConstraints(SynchronizationContext context, TableProperties table) throws SQLException
 	{
 		Collection<UniqueConstraint> constraints = table.getUniqueConstraints();
 		
 		constraints.remove(table.getPrimaryKey());
 
 		Dialect dialect = context.getDialect();
 
 		Connection connection = context.getConnection(context.getTargetDatabase());
 		
 		Statement statement = connection.createStatement();
 		
 		// Drop unique constraints on the current table
 		for (UniqueConstraint constraint: constraints)
 		{
 			String sql = dialect.getDropUniqueConstraintSQL(constraint);
 			
 			logger.debug(sql);
 			
 			statement.addBatch(sql);
 		}
 		
 		statement.executeBatch();
 		statement.close();
 	}
 	
 	public static void restoreUniqueConstraints(SynchronizationContext context, TableProperties table) throws SQLException
 	{
 		Collection<UniqueConstraint> constraints = table.getUniqueConstraints();
 		
 		constraints.remove(table.getPrimaryKey());
 
 		Dialect dialect = context.getDialect();
 
 		Connection connection = context.getConnection(context.getTargetDatabase());
 		
 		Statement statement = connection.createStatement();
 		
 		// Drop unique constraints on the current table
 		for (UniqueConstraint constraint: constraints)
 		{
 			String sql = dialect.getCreateUniqueConstraintSQL(constraint);
 			
 			logger.debug(sql);
 			
 			statement.addBatch(sql);
 		}
 		
 		statement.executeBatch();
 		statement.close();
 	}
 	
 	public static void rollback(Connection connection)
 	{
 		try
 		{
 			connection.rollback();
 			connection.setAutoCommit(true);
 		}
 		catch (java.sql.SQLException e)
 		{
 			logger.warn(e.toString(), e);
 		}
 	}
 	
 	public static Object getObject(ResultSet resultSet, int index, int type) throws SQLException
 	{
 		switch (type)
 		{
 			case Types.BLOB:
 			{
 				return resultSet.getBlob(index);
 			}
 			case Types.CLOB:
 			{
 				return resultSet.getClob(index);
 			}
 			default:
 			{
 				return resultSet.getObject(index);
 			}
 		}
 	}
 }
