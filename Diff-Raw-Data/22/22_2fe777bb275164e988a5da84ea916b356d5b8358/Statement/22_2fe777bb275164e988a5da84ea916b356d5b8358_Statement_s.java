 /*******************************************************************************
  * Copyright (c) 2004, 2005 Actuate Corporation.
  * All rights reserved. This program and the accompanying materials
  * are made available under the terms of the Eclipse Public License v1.0
  * which accompanies this distribution, and is available at
  * http://www.eclipse.org/legal/epl-v10.html
  *
  * Contributors:
  *  Actuate Corporation  - initial API and implementation
  *******************************************************************************/
 
 package org.eclipse.birt.report.data.oda.jdbc;
 
 import java.math.BigDecimal;
 import java.sql.Date;
 import java.sql.PreparedStatement;
 import java.sql.SQLException;
 import java.sql.Time;
 import java.sql.Timestamp;
 import java.util.Properties;
 
 import org.eclipse.birt.data.oda.IParameterMetaData;
 import org.eclipse.birt.data.oda.IResultSet;
 import org.eclipse.birt.data.oda.IResultSetMetaData;
 import org.eclipse.birt.data.oda.IStatement;
 import org.eclipse.birt.data.oda.OdaException;
 import org.eclipse.birt.data.oda.SortSpec;
 import org.eclipse.birt.data.oda.util.logging.Level;
 
 /**
  * 
  * The class implements the org.eclipse.birt.data.oda.IStatement interface.
  * 
  */
 public class Statement implements IStatement
 {
 
 	/** the JDBC preparedStatement object */
 	private PreparedStatement preStat;
 
 	/** the JDBC Connection object */
 	private java.sql.Connection conn;
 
 	/** remember the max row value, default 0. */
 	private int maxrows;
 	
 	/** indicates if need to call JDBC setMaxRows before execute statement */
 	private boolean maxRowsUpToDate = false;
 
 	/**
 	 * assertNull(Object o)
 	 * 
 	 * @param o
 	 *            the object that need to be tested null or not. if null, throw
 	 *            exception
 	 */
 	private void assertNotNull( Object o ) throws OdaException
 	{
 		if ( o == null )
 		{
 			throw new DriverException( DriverException.ERRMSG_NO_STATEMENT,
 					DriverException.ERROR_NO_STATEMENT );
 
 		}
 	}
 
 	/**
 	 * 
 	 * Constructor Statement(java.sql.Connection connection) use JDBC's
 	 * Connection to construct it.
 	 *  
 	 */
 	Statement( java.sql.Connection connection ) throws OdaException
 	{
 		if ( connection != null )
 
 		{
 			/* record down the JDBC Connection object */
 			this.preStat = null;
 			this.conn = connection;
 			maxrows = 0;
 		}
 		else
 		{
 			throw new DriverException( DriverException.ERRMSG_NO_CONNECTION,
 					DriverException.ERROR_NO_CONNECTION );
 		}
 
 	}
 
 	/*
 	 * @see org.eclipse.birt.data.oda.IStatement#prepare(java.lang.String)
 	 */
 	public void prepare( String command ) throws OdaException
 	{
 		JDBCConnectionFactory.log( Level.INFO_LEVEL, "Statement.prepare( \""
 				+ command + "\" )" );
 		try
 		{
 			/*
 			 * call the JDBC Connection.prepareStatement(String) method to get
 			 * the preparedStatement
 			 */
 			this.preStat = conn.prepareStatement( command );
 		}
 		catch ( SQLException e )
 		{
 			throw new JDBCException( e );
 		}
 
 	}
 
 	/*
 	 * @see org.eclipse.birt.data.oda.IStatement#setProperty(java.lang.String,
 	 *      java.lang.String)
 	 */
 	public void setProperty( String name, String value ) throws OdaException
 	{
 		/* not supported */
 		throw new UnsupportedOperationException(
 				"setProperty is not supported." );
 	}
 
 	/*
 	 * @see org.eclipse.birt.data.oda.IStatement#setPropertyInfo(java.util.Properties)
 	 */
 	public void setPropertyInfo( Properties info ) throws OdaException
 	{
 		/* not supported */
 		throw new UnsupportedOperationException(
 				"setPropertyInfo is not supported." );
 
 	}
 
 	/*
 	 * @see org.eclipse.birt.data.oda.IStatement#close()
 	 */
 	public void close( ) throws OdaException
 	{
 		JDBCConnectionFactory.log( Level.INFO_LEVEL, "Statement.close( )" );
 
 		try
 		{
 			if ( preStat != null )
 			{
 				/* redirect the call to JDBC preparedStatement.close() */
 				this.preStat.close( );
 			}
 		}
 		catch ( SQLException e )
 		{
 			throw new JDBCException( e );
 		}
 
 	}
 
 	/*
 	 * @see org.eclipse.birt.data.oda.IStatement#setMaxRows(int)
 	 */
 	public void setMaxRows( int max )
 	{
 		JDBCConnectionFactory.log( Level.FINE_LEVEL, "Statement.setMaxRows( "
 				+ max + " )" );
 		if ( max != maxrows && max >= 0 )
 		{
 			maxrows = (int) max;
 			maxRowsUpToDate = false;
 		}
 	}
 
 	/*
 	 * @see org.eclipse.birt.data.oda.IStatement#getMaxRows()
 	 */
 	public int getMaxRows( )
 	{
 		JDBCConnectionFactory.log( Level.FINE_LEVEL, "Statement.getMaxRows( )" );
 		return this.maxrows;
 
 	}
 
 	/*
 	 * @see org.eclipse.birt.data.oda.IStatement#getMetaData()
 	 */
 	public IResultSetMetaData getMetaData( ) throws OdaException
 	{
 		JDBCConnectionFactory
 				.log( Level.FINE_LEVEL, "Statement.getMetaData( )" );
 		assertNotNull( preStat );
 		try
 		{
 			/* redirect the call to JDBC preparedStatement.getMetaData() */
			return new ResultSetMetaData( this.preStat.getMetaData( ) );
 		}
 		catch ( SQLException e )
 		{
			throw new JDBCException( e );
 		}
 
 	}
 
 	/*
 	 * @see org.eclipse.birt.data.oda.IStatement#getMetaData(java.lang.String)
 	 */
 	public IResultSetMetaData getMetaData( String command ) throws OdaException
 	{
 		this.prepare( command );
 		return this.getMetaData( );
 	}
 
 	/*
 	 * @see org.eclipse.birt.data.oda.IStatement#executeQuery()
 	 */
 	public IResultSet executeQuery( ) throws OdaException
 	{
 		JDBCConnectionFactory.log( Level.INFO_LEVEL,
 				"Statement.executeQuery( )" );
 		assertNotNull( preStat );
 		try
 		{
 			if (!maxRowsUpToDate)
 			{
 				preStat.setMaxRows( maxrows );
 				maxRowsUpToDate = true;
 			}
 			/* redirect the call to JDBC preparedStatement.executeQuery() */
 			return new ResultSet( this.preStat.executeQuery( ) );
 		}
 		catch ( SQLException e )
 		{
 			throw new JDBCException( e );
 		}
 
 	}
 
 	/*
 	 * @see org.eclipse.birt.data.oda.IStatement#executeQuery(java.lang.String)
 	 */
 	public IResultSet executeQuery( String command ) throws OdaException
 	{
 		JDBCConnectionFactory.log( Level.INFO_LEVEL,
 				"Statement.executeQuery( \"" + command + "\" )" );
 		/* use the parepare and executeQuery instead */
 		this.prepare( command );
 		return this.executeQuery( );
 	}
 
 	/*
 	 * @see org.eclipse.birt.data.oda.IStatement#execute()
 	 */
 	public boolean execute( ) throws OdaException
 	{
 		JDBCConnectionFactory.log( Level.FINE_LEVEL, "Statement.execute( )" );
 		assertNotNull( preStat );
 		try
 		{
 			if (!maxRowsUpToDate)
 			{
 				preStat.setMaxRows( maxrows );
 				maxRowsUpToDate = true;
 			}
 			/* redirect the call to JDBC preparedStatement.execute() */
 			return preStat.execute( );
 		}
 		catch ( SQLException e )
 		{
 			throw new JDBCException( e );
 		}
 
 	}
 
 	/*
 	 * @see org.eclipse.birt.data.oda.IStatement#execute(java.lang.String)
 	 */
 	public boolean execute( String command ) throws OdaException
 	{
 		JDBCConnectionFactory.log( Level.FINE_LEVEL, "Statement.execute( \""
 				+ command + "\" )" );
 		/* use the parepare and execute instead */
 		this.prepare( command );
 		return this.execute( );
 
 	}
 
 	/*
 	 * @see org.eclipse.birt.data.oda.IStatement#getResultSet()
 	 */
 	public IResultSet getResultSet( ) throws OdaException
 	{
 		JDBCConnectionFactory.log( Level.FINE_LEVEL,
 				"Statement.getResultSet( )" );
 		assertNotNull( preStat );
 		try
 		{
 			/* redirect the call to JDBC preparedStatement.getResultSet() */
 			return new ResultSet( this.preStat.getResultSet( ) );
 		}
 		catch ( SQLException e )
 		{
 			throw new JDBCException( e );
 		}
 
 	}
 
 	/*
 	 * @see org.eclipse.birt.data.oda.IStatement#getMoreResults()
 	 */
 	public boolean getMoreResults( ) throws OdaException
 	{
 		JDBCConnectionFactory.log( Level.FINE_LEVEL,
 				"Statement.getMoreResults( )" );
 		assertNotNull( preStat );
 		try
 		{
 			/* redirect the call to JDBC preparedStatement.getMoreResults() */
 			return this.preStat.getMoreResults( );
 		}
 		catch ( SQLException e )
 		{
 			throw new JDBCException( e );
 		}
 	}
 
 	/*
 	 * @see org.eclipse.birt.data.oda.IStatement#setInt(java.lang.String, int)
 	 */
 	public void setInt( String parameterName, int value ) throws OdaException
 	{
 		/* not supported */
 		throw new UnsupportedOperationException(
 				"No named Parameter supported." );
 	}
 
 	/*
 	 * @see org.eclipse.birt.data.oda.IStatement#setInt(int, int)
 	 */
 	public void setInt( int parameterId, int value ) throws OdaException
 	{
 		JDBCConnectionFactory.log( Level.FINE_LEVEL, "Statement.setInt( "
 				+ parameterId + " , " + value + " )" );
 		assertNotNull( preStat );
 		try
 		{
 			/* redirect the call to JDBC preparedStatement.setInt(int,int) */
 			this.preStat.setInt( parameterId, value );
 		}
 		catch ( SQLException e )
 		{
 			throw new JDBCException( e );
 		}
 
 	}
 
 	/*
 	 * @see org.eclipse.birt.data.oda.IStatement#setDouble(java.lang.String,
 	 *      double)
 	 */
 	public void setDouble( String parameterName, double value )
 			throws OdaException
 	{
 		/* not supported */
 		throw new UnsupportedOperationException(
 				"No named Parameter supported." );
 
 	}
 
 	/*
 	 * @see org.eclipse.birt.data.oda.IStatement#setDouble(int, double)
 	 */
 	public void setDouble( int parameterId, double value ) throws OdaException
 	{
 		JDBCConnectionFactory.log( Level.FINE_LEVEL, "Statement.setDouble( "
 				+ parameterId + " , " + value + " )" );
 		assertNotNull( preStat );
 		try
 		{
 			/* redirect the call to JDBC preparedStatement.setDouble(int,double) */
 			this.preStat.setDouble( parameterId, value );
 		}
 		catch ( SQLException e )
 		{
 			throw new JDBCException( e );
 		}
 
 	}
 
 	/*
 	 * @see org.eclipse.birt.data.oda.IStatement#setBigDecimal(java.lang.String,
 	 *      java.math.BigDecimal)
 	 */
 	public void setBigDecimal( String parameterName, BigDecimal value )
 			throws OdaException
 	{
 		/* not supported */
 		throw new UnsupportedOperationException(
 				"No named Parameter supported." );
 
 	}
 
 	/*
 	 * @see org.eclipse.birt.data.oda.IStatement#setBigDecimal(int,
 	 *      java.math.BigDecimal)
 	 */
 	public void setBigDecimal( int parameterId, BigDecimal value )
 			throws OdaException
 	{
 		JDBCConnectionFactory.log( Level.FINE_LEVEL,
 				"Statement.setBigDecimal( " + parameterId + " , " + value
 						+ " )" );
 		assertNotNull( preStat );
 		try
 		{
 			/*
 			 * redirect the call to JDBC
 			 * preparedStatement.setBigDecimal(int,BigDecimal)
 			 */
 			this.preStat.setBigDecimal( parameterId, value );
 		}
 		catch ( SQLException e )
 		{
 			throw new JDBCException( e );
 		}
 
 	}
 
 	/*
 	 * @see org.eclipse.birt.data.oda.IStatement#setString(java.lang.String,
 	 *      java.lang.String)
 	 */
 	public void setString( String parameterName, String value )
 			throws OdaException
 	{
 		/* not supported */
 		throw new UnsupportedOperationException(
 				"No named Parameter supported." );
 	}
 
 	/*
 	 * @see org.eclipse.birt.data.oda.IStatement#setString(int,
 	 *      java.lang.String)
 	 */
 	public void setString( int parameterId, String value ) throws OdaException
 	{
 		JDBCConnectionFactory.log( Level.FINE_LEVEL, "Statement.setString( "
 				+ parameterId + " , \"" + value + "\" )" );
 		assertNotNull( preStat );
 		try
 		{
 			/* redirect the call to JDBC preparedStatement.setString(int,String) */
 			this.preStat.setString( parameterId, value );
 		}
 		catch ( SQLException e )
 		{
 			throw new JDBCException( e );
 		}
 
 	}
 
 	/*
 	 * @see org.eclipse.birt.data.oda.IStatement#setDate(java.lang.String,
 	 *      java.sql.Date)
 	 */
 	public void setDate( String parameterName, Date value ) throws OdaException
 	{
 		/* not supported */
 		throw new UnsupportedOperationException(
 				"No named Parameter supported." );
 	}
 
 	/*
 	 * @see org.eclipse.birt.data.oda.IStatement#setDate(int, java.sql.Date)
 	 */
 	public void setDate( int parameterId, Date value ) throws OdaException
 	{
 		JDBCConnectionFactory.log( Level.FINE_LEVEL, "Statement.setDate( "
 				+ parameterId + " , " + value + " )" );
 		assertNotNull( preStat );
 		try
 		{
 			/* redirect the call to JDBC preparedStatement.setDate(int,Date) */
 			this.preStat.setDate( parameterId, value );
 		}
 		catch ( SQLException e )
 		{
 			throw new JDBCException( e );
 		}
 
 	}
 
 	/*
 	 * @see org.eclipse.birt.data.oda.IStatement#setTime(java.lang.String,
 	 *      java.sql.Time)
 	 */
 	public void setTime( String parameterName, Time value ) throws OdaException
 	{
 		/* not supported */
 		throw new UnsupportedOperationException(
 				"No named Parameter supported." );
 
 	}
 
 	/*
 	 * @see org.eclipse.birt.data.oda.IStatement#setTime(int, java.sql.Time)
 	 */
 	public void setTime( int parameterId, Time value ) throws OdaException
 	{
 		JDBCConnectionFactory.log( Level.FINE_LEVEL, "Statement.setTime( "
 				+ parameterId + " , " + value + " )" );
 		assertNotNull( preStat );
 		try
 		{
 			/* redirect the call to JDBC preparedStatement.setTime(int,Time) */
 			this.preStat.setTime( parameterId, value );
 		}
 		catch ( SQLException e )
 		{
 			throw new JDBCException( e );
 		}
 
 	}
 
 	/*
 	 * @see org.eclipse.birt.data.oda.IStatement#setTimestamp(java.lang.String,
 	 *      java.sql.Timestamp)
 	 */
 	public void setTimestamp( String parameterName, Timestamp value )
 			throws OdaException
 	{
 		/* not supported */
 		throw new UnsupportedOperationException(
 				"No named Parameter supported." );
 
 	}
 
 	/*
 	 * @see org.eclipse.birt.data.oda.IStatement#setTimestamp(int,
 	 *      java.sql.Timestamp)
 	 */
 	public void setTimestamp( int parameterId, Timestamp value )
 			throws OdaException
 	{
 		JDBCConnectionFactory.log( Level.FINE_LEVEL, "Statement.setTimestamp( "
 				+ parameterId + " , " + value + " )" );
 		assertNotNull( preStat );
 		try
 		{
 			/*
 			 * redirect the call to JDBC
 			 * preparedStatement.setTimestamp(int,Timestamp)
 			 */
 			this.preStat.setTimestamp( parameterId, value );
 		}
 		catch ( SQLException e )
 		{
 			throw new JDBCException( e );
 		}
 
 	}
 
 	/*
 	 * @see org.eclipse.birt.data.oda.IStatement#findInParameter(java.lang.String)
 	 */
 	public int findInParameter( String parameterName ) throws OdaException
 	{
 		/* not supported */
 		throw new UnsupportedOperationException(
 				"No named Parameter supported." );
 	}
 
 	/*
 	 * @see org.eclipse.birt.data.oda.IStatement#getParameterType(java.lang.String)
 	 */
 	public int getParameterType( String parameterName ) throws OdaException
 	{
 		/* not supported */
 		throw new UnsupportedOperationException( "No named Parameter upported." );
 	}
 
 	/*
 	 * @see org.eclipse.birt.data.oda.IStatement#getParameterType(int)
 	 */
 	public int getParameterType( int parameterId ) throws OdaException
 	{
 		JDBCConnectionFactory.log( Level.FINE_LEVEL,
 				"Statement.getParameterType( " + parameterId + " )" );
 		assertNotNull( preStat );
 
 		try
 		{
 			/*
 			 * redirect the call to JDBC preparedStatement.getParameterMetaData(
 			 * ).getParameterType(int)
 			 */
 			return this.preStat.getParameterMetaData( ).getParameterType(
 					parameterId );
 		}
 		catch ( SQLException e )
 		{
 			throw new JDBCException( e );
 		}
 	}
 
 	/*
 	 * @see org.eclipse.birt.data.oda.IStatement#getParameterMetaData()
 	 */
 	public IParameterMetaData getParameterMetaData( ) throws OdaException
 	{
 		JDBCConnectionFactory.log( Level.FINE_LEVEL,
 				"Statement.getParameterMetaData( )" );
 		assertNotNull( preStat );
 		try
 		{
 			/* redirect the call to JDBC preparedStatement.getParameterMetaData */
 			return new ParameterMetaData( this.preStat.getParameterMetaData( ) );
 		}
 		catch ( SQLException e )
 		{
 			throw new JDBCException( e );
 		}
 	}
 
 	/*
 	 * @see org.eclipse.birt.data.oda.IStatement#setSortSpec(org.eclipse.birt.data.oda.SortSpec)
 	 */
 	public void setSortSpec( SortSpec sortBy ) throws OdaException
 	{
 		/* not supported */
 		throw new UnsupportedOperationException(
 				"setSortSpec is not supported." );
 
 	}
 
 	/*
 	 * @see org.eclipse.birt.data.oda.IStatement#getSortSpec()
 	 */
 	public SortSpec getSortSpec( ) throws OdaException
 	{
 		/* not supported */
 		throw new UnsupportedOperationException(
 				"getSortSpec is not supported." );
 	}
 
 	public void clearInParameters() throws OdaException
 	{
 		JDBCConnectionFactory.log( Level.INFO_LEVEL, "Statement.clearInParameters( )" );
 		assertNotNull( preStat );
 		try
 		{
 			preStat.clearParameters();
 		}
 		catch( SQLException ex )
 		{
 			throw new JDBCException( ex );
 		}
 	}
 }
