 /*
  *****************************************************************************
  * Copyright (c) 2004, 2007 Actuate Corporation.
  * All rights reserved. This program and the accompanying materials
  * are made available under the terms of the Eclipse Public License v1.0
  * which accompanies this distribution, and is available at
  * http://www.eclipse.org/legal/epl-v10.html
  *
  * Contributors:
  *  Actuate Corporation - initial API and implementation
  *
  ******************************************************************************
  */ 
 
 package org.eclipse.birt.data.engine.odaconsumer;
 
 import java.math.BigDecimal;
 import java.sql.Date;
 import java.sql.Time;
 import java.sql.Timestamp;
 import java.sql.Types;
 import java.util.ArrayList;
 import java.util.Collection;
 import java.util.HashSet;
 import java.util.Hashtable;
 import java.util.Iterator;
 import java.util.List;
 import java.util.ListIterator;
 import java.util.Set;
 import java.util.logging.Level;
 
 import org.eclipse.birt.data.engine.core.DataException;
 import org.eclipse.birt.data.engine.executor.ResultClass;
 import org.eclipse.birt.data.engine.i18n.DataResourceHandle;
 import org.eclipse.birt.data.engine.i18n.ResourceConstants;
 import org.eclipse.birt.data.engine.odi.IResultClass;
 import org.eclipse.datatools.connectivity.oda.IAdvancedQuery;
 import org.eclipse.datatools.connectivity.oda.IBlob;
 import org.eclipse.datatools.connectivity.oda.IClob;
 import org.eclipse.datatools.connectivity.oda.IParameterMetaData;
 import org.eclipse.datatools.connectivity.oda.IQuery;
 import org.eclipse.datatools.connectivity.oda.IResultSet;
 import org.eclipse.datatools.connectivity.oda.IResultSetMetaData;
 import org.eclipse.datatools.connectivity.oda.OdaException;
 import org.eclipse.datatools.connectivity.oda.SortSpec;
 
 /**
  * <code>PreparedStatement</code> represents a statement query that can be executed without 
  * input parameter values and returns the results and output parameters values it produces.
  * <br>
  * Blob and Clob data types are only supported in output data returned
  * in result columns and output parameters.
  */
 public class PreparedStatement
 {
 	private String m_dataSetType;
 	private Connection m_connection;
 	private String m_queryText;
 	
 	private IQuery m_statement;
 	private ArrayList m_properties;
 	private int m_maxRows;
 	private ArrayList m_sortSpecs;
 	
 	private int m_supportsNamedResults;
 	private int m_supportsOutputParameters;
 	private int m_supportsNamedParameters;
 	private Boolean m_supportsInputParameters;
 	
 	private ArrayList m_parameterHints;
 	// cached Collection of parameter metadata
 	private Collection m_parameterMetaData;
 	
 	private ProjectedColumns m_projectedColumns;
 	private IResultClass m_currentResultClass;
 	private ResultSet m_currentResultSet;
 	private IResultSet m_driverResultSet;
 	
 	// projected columns for the un-named result set needs to be updated 
 	// next time it's needed
 	private boolean m_updateProjectedColumns;
 	
 	// mappings of result set name to their corresponding projected columns 
 	// and result set class
 	private Hashtable m_namedProjectedColumns;
 	private Hashtable m_namedCurrentResultClasses;
 	private Hashtable m_namedCurrentResultSets;
 	// set of named projected columns that need to be updated next time 
 	// it's needed
 	private HashSet m_updateNamedProjectedColumns;
 	
 	private static final int UNKNOWN = -1;
 	private static final int FALSE = 0;
 	private static final int TRUE = 1;
 	
     // trace logging variables
 	private static String sm_className = PreparedStatement.class.getName();
 	private static String sm_loggerName = ConnectionManager.sm_packageName;
 	private static LogHelper sm_logger = LogHelper.getInstance( sm_loggerName );
 	
 	PreparedStatement( IQuery statement, String dataSetType, 
 	                   Connection connection, String query )
 	{
 		String methodName = "PreparedStatement";		 //$NON-NLS-1$
 		if( sm_logger.isLoggingEnterExitLevel() )
 			sm_logger.entering( sm_className, methodName, 
 								new Object[] { statement, dataSetType, 
 											   connection, query } );
 		
 		assert( statement != null && connection != null );
 		m_statement = statement;
 		m_dataSetType = dataSetType;
 		m_connection = connection;
 		m_queryText = query;
 		
 		m_supportsNamedResults = UNKNOWN;
 		m_supportsOutputParameters = UNKNOWN;
 		m_supportsNamedParameters = UNKNOWN;
 		m_supportsInputParameters = null;	// for unknown
 		
 		sm_logger.exiting( sm_className, methodName, this );
 	}
 	
 	/**
 	 * Sets the named property with the specified value.
 	 * @param name	the property name.
 	 * @param value	the property value.
 	 * @throws DataException	if data source error occurs.
 	 */
 	public void setProperty( String name, String value ) throws DataException
 	{
 		String methodName = "setProperty"; //$NON-NLS-1$
 		if( sm_logger.isLoggingEnterExitLevel() )
 			sm_logger.entering( sm_className, methodName, new Object[] { name, value } );
 		
 		doSetProperty( name, value );
 		
 		// save the properties in a list in case we need them later, 
 		// i.e. support clearParameterValues() for drivers that don't support
 		// the ODA operation
 		getPropertiesList().add( new Property( name, value ) );
 		
 		sm_logger.exiting( sm_className, methodName );
 	}
 
 	private void doSetProperty( String name, String value ) throws DataException
 	{
 		String methodName = "doSetProperty"; //$NON-NLS-1$
 		if( sm_logger.isLoggingEnterExitLevel() )
 			sm_logger.entering( sm_className, methodName, new Object[] { name, value } );
 		
 		try
 		{
 			m_statement.setProperty( name, value );
 		}
 		catch( OdaException ex )
 		{
 			sm_logger.logp( Level.SEVERE, sm_className, methodName, 
 							"Cannot set statement property.", ex ); //$NON-NLS-1$
 			
 			throw new DataException( ResourceConstants.CANNOT_SET_STATEMENT_PROPERTY, ex );
 		}
 		catch( UnsupportedOperationException ex )
 		{
 			sm_logger.logp( Level.WARNING, sm_className, methodName, 
 							"Cannot set statement property.", ex );			 //$NON-NLS-1$
 		}
 		
 		sm_logger.exiting( sm_className, methodName );
 	}
 	
 	private ArrayList getPropertiesList()
 	{
 		if( m_properties == null )
 			m_properties = new ArrayList();
 		
 		return m_properties;
 	}
 	
 	/**
 	 * Specifies the sort specification for this <code>Statement</code>.  Must be 
 	 * called prior to <code>Statement.execute</code> for the sort specification to 
 	 * apply to the result set(s) returned.
 	 * 
 	 * @param sortBy	the sort specification to assign to the <code>Statement</code>.
 	 * @throws DataException	if data source error occurs.
 	 */
 	public void setSortSpec( SortSpec sortBy ) throws DataException
 	{
 		String methodName = "setSortSpec";	 //$NON-NLS-1$
 		sm_logger.entering( sm_className, methodName, sortBy );
 		
 		doSetSortSpec( sortBy );		
 		getSortSpecsList().add( sortBy );
 		
 		sm_logger.exiting( sm_className, methodName );
 	}
 
 	private void doSetSortSpec( SortSpec sortBy ) throws DataException
 	{
 		String methodName = "doSetSortSpec"; //$NON-NLS-1$
 		sm_logger.entering( sm_className, methodName, sortBy );
 		
 		try
 		{
 			m_statement.setSortSpec( sortBy );
 		}
 		catch( OdaException ex )
 		{
 			sm_logger.logp( Level.SEVERE, sm_className, methodName, 
 							"Cannot set sort spec.", ex ); //$NON-NLS-1$
 			
 			throw new DataException( ResourceConstants.CANNOT_SET_SORT_SPEC, ex );
 		}
 		catch( UnsupportedOperationException ex )
 		{
 			sm_logger.logp( Level.SEVERE, sm_className, methodName, 
 							"Cannot set sort spec.", ex ); //$NON-NLS-1$
 			throw new DataException( ResourceConstants.CANNOT_SET_SORT_SPEC, ex );
 		}
 		
 		sm_logger.exiting( sm_className, methodName );
 	}
 	
 	private ArrayList getSortSpecsList()
 	{
 		if( m_sortSpecs == null )
 			m_sortSpecs = new ArrayList();
 		
 		return m_sortSpecs;
 	}
 	
 	/**
 	 * Specifies the maximum number of <code>IResultObjects</code> that can be fetched 
 	 * from each <code>ResultSet</code> of this <code>Statement</code>.
 	 * @param max	the maximum number of <code>IResultObjects</code> that can be 
 	 * 				fetched from each <code>ResultSet</code>.
 	 * @throws DataException	if data source error occurs.
 	 */
 	public void setMaxRows( int max ) throws DataException
 	{
 		String methodName = "setMaxRows"; //$NON-NLS-1$
 		sm_logger.entering( sm_className, methodName, max );
 		
 		doSetMaxRows( max );
 		
 		m_maxRows = max;
 		
 		sm_logger.exiting( sm_className, methodName );
 	}
 
 	private void doSetMaxRows( int max ) throws DataException
 	{
 		String methodName = "doSetMaxRows"; //$NON-NLS-1$
 		sm_logger.entering( sm_className, methodName, max );
 		
 		try
 		{
 			m_statement.setMaxRows( max );
 		}
 		catch( OdaException ex )
 		{
 			sm_logger.logp( Level.SEVERE, sm_className, methodName, 
 							"Cannot set max rows.", ex ); //$NON-NLS-1$
 			
 			throw new DataException( ResourceConstants.CANNOT_SET_MAX_ROWS, ex );
 		}
 		catch( UnsupportedOperationException ex )
 		{
 			sm_logger.logp( Level.WARNING, sm_className, methodName, 
 							"Cannot set max rows.", ex ); //$NON-NLS-1$
 			// non-critical operation, ignore and proceed
 		}
 		
 		sm_logger.exiting( sm_className, methodName );
 	}
 	
 	/**
 	 * Returns an <code>IResultClass</code> representing the metadata of the 
 	 * result set for this <code>Statement</code>.
 	 * @return	the <code>IResultClass</code> for the result set.
 	 * @throws DataException	if data source error occurs.
 	 */
 	public IResultClass getMetaData( ) throws DataException
 	{
 		String methodName = "getMetaData"; //$NON-NLS-1$
 		sm_logger.entering( sm_className, methodName );
 		
 		IResultClass ret = null;
 		
 		// we can get the current result set's metadata directly from the 
 		// current result set handle rather than go through ODA
 		if( m_currentResultSet != null )
 			ret = m_currentResultSet.getMetaData();
 		else
 			ret = doGetMetaData();
 		
 		sm_logger.exiting( sm_className, methodName, ret );
 		return ret;
 	}
 	
 	private IResultClass doGetMetaData() throws DataException
 	{	
 		String methodName = "doGetMetaData"; //$NON-NLS-1$
 		sm_logger.entering( sm_className, methodName );
 	
 		if( m_currentResultClass == null )
 		{
 			List projectedColumns = getProjectedColumns().getColumnsMetadata();
 			m_currentResultClass = doGetResultClass( projectedColumns );
 		}
 		
 		sm_logger.exiting( sm_className, methodName, m_currentResultClass );
 		
 		return m_currentResultClass;
 	}
 
 	private ResultClass doGetResultClass( List projectedColumns ) throws DataException 
 	{
 		String methodName = "doGetResultClass"; //$NON-NLS-1$
 		sm_logger.entering( sm_className, methodName, projectedColumns );
 		
 		assert( projectedColumns != null );
 		ResultClass ret = new ResultClass( projectedColumns );
 		
 		sm_logger.exiting( sm_className, methodName, ret );
 		
 		return ret;
 	}
 	
 	private ProjectedColumns getProjectedColumns() 
 		throws DataException
 	{
 		String methodName = "getProjectedColumns"; //$NON-NLS-1$
 		sm_logger.entering( sm_className, methodName );
 		
 		if( m_projectedColumns == null )
 		{
 			IResultSetMetaData odaMetadata = getRuntimeMetaData();
 			m_projectedColumns = doGetProjectedColumns( odaMetadata );
 		}	
 		else if( m_updateProjectedColumns )
 		{
 			// need to update the projected columns of the un-named result 
 			// set with the newest runtime metadata, don't use the cached 
 			// one
 			IResultSetMetaData odaMetadata = getRuntimeMetaData();
 			ProjectedColumns newProjectedColumns = 
 				doGetProjectedColumns( odaMetadata );
 			updateProjectedColumns( newProjectedColumns, m_projectedColumns );
 			m_projectedColumns = newProjectedColumns;
 			
 			// reset the update flag
 			m_updateProjectedColumns = false;
 		}
 		
 		sm_logger.exiting( sm_className, methodName, m_projectedColumns );
 		
 		return m_projectedColumns;
 	}
 	
 	private IResultSetMetaData getRuntimeMetaData() throws DataException
 	{
 		String methodName = "getRuntimeMetaData"; //$NON-NLS-1$
 		sm_logger.entering( sm_className, methodName );
 		
 		try
 		{
 			IResultSetMetaData ret = m_statement.getMetaData();
 			
 			sm_logger.exiting( sm_className, methodName, ret );			
 			return ret;
 		}
 		catch( OdaException ex )
 		{
 			sm_logger.logp( Level.SEVERE, sm_className, methodName, 
 							"Cannot get resultset metadata.", ex ); //$NON-NLS-1$
 			
 			throw new DataException( ResourceConstants.CANNOT_GET_RESULTSET_METADATA, ex );
 		}
 		catch( UnsupportedOperationException ex )
 		{
 			sm_logger.logp( Level.WARNING, sm_className, methodName, 
 							"Cannot get resultset metadata.", ex ); //$NON-NLS-1$
 			
 			throw new DataException( ResourceConstants.CANNOT_GET_RESULTSET_METADATA, ex );
 		}
 	}
 	
 	private ProjectedColumns doGetProjectedColumns( IResultSetMetaData odaMetadata )
 		throws DataException
 	{
 		String methodName = "doGetProjectedColumns"; //$NON-NLS-1$
 		sm_logger.entering( sm_className, methodName, odaMetadata );
 		
 		ResultSetMetaData metadata = 
 			new ResultSetMetaData( odaMetadata, 
 								   m_connection.getDataSourceId(),
 								   m_dataSetType );
 		ProjectedColumns ret = new ProjectedColumns( metadata );
 		
 		sm_logger.exiting( sm_className, methodName, ret );
 		return ret;
 	}
 
 	/**
 	 * Returns an <code>IResultClass</code> representing the metadata of the 
 	 * named result set for this <code>Statement</code>.
 	 * @param resultSetName	the name of the result set.
 	 * @return	the <code>IResultClass</code> for the named result set.
 	 * @throws DataException	if data source error occurs.
 	 */
 	public IResultClass getMetaData( String resultSetName ) throws DataException
 	{
 		String methodName = "getMetaData"; //$NON-NLS-1$
 		sm_logger.entering( sm_className, methodName, resultSetName );
 		
 		checkNamedResultsSupport();
 		
 		// we can get the current result set's metadata directly from the 
 		// current result set handle rather than go through ODA
 		ResultSet resultset = 
 			(ResultSet) getNamedCurrentResultSets().get( resultSetName );
 		
 		IResultClass ret = null;
 		
 		if( resultset != null )
 			ret = resultset.getMetaData();
 		else
 			ret = doGetMetaData( resultSetName );
 		
 		sm_logger.exiting( sm_className, methodName, ret );
 		return ret;
 	}
 	
 	private IResultClass doGetMetaData( String resultSetName ) throws DataException
 	{
 		String methodName = "doGetMetaData"; //$NON-NLS-1$
 		sm_logger.entering( sm_className, methodName, resultSetName );
 		
 		IResultClass resultClass = 
 			(IResultClass) getNamedCurrentResultClasses().get( resultSetName );
 
 		if( resultClass == null )
 		{
 			List projectedColumns = 
 				getProjectedColumns( resultSetName ).getColumnsMetadata();	
 			resultClass = doGetResultClass( projectedColumns );
 			getNamedCurrentResultClasses().put( resultSetName, resultClass );
 		}
 		
 		sm_logger.exiting( sm_className, methodName, resultClass );
 		
 		return resultClass;
 	}
 
 	private ProjectedColumns getProjectedColumns( String resultSetName )
 		throws DataException
 	{
 		String methodName = "getProjectedColumns"; //$NON-NLS-1$
 		sm_logger.entering( sm_className, methodName, resultSetName );
 		
 		ProjectedColumns projectedColumns = 
 			(ProjectedColumns) getNamedProjectedColumns().get( resultSetName );
 		if( projectedColumns == null )
 		{
 			IResultSetMetaData odaMetadata = getRuntimeMetaData( resultSetName );
 			projectedColumns = doGetProjectedColumns( odaMetadata );	
 			getNamedProjectedColumns().put( resultSetName, projectedColumns );
 		}
 		else if( m_updateNamedProjectedColumns != null && 
 				 m_updateNamedProjectedColumns.contains( resultSetName ) )
 		{
 			// there's an existing ProjectedColumns from the same result set, 
 			// and it needs to be updated with the newest runtime metadata
 			IResultSetMetaData odaMetadata = getRuntimeMetaData( resultSetName );
 			ProjectedColumns newProjectedColumns = 
 				doGetProjectedColumns( odaMetadata );
 			updateProjectedColumns( newProjectedColumns, projectedColumns );
 			getNamedProjectedColumns().put( resultSetName, newProjectedColumns );
 			
 			// reset the update flag for this result set name
 			m_updateNamedProjectedColumns.remove( resultSetName );
 		}
 		
 		sm_logger.exiting( sm_className, methodName, projectedColumns );
 		
 		return projectedColumns;
 	}
 	
 	private IResultSetMetaData getRuntimeMetaData( String resultSetName ) 
 		throws DataException
 	{
 		String methodName = "getRuntimeMetaData"; //$NON-NLS-1$
 		sm_logger.entering( sm_className, methodName, resultSetName );
 		
 		try
 		{
 			IResultSetMetaData ret = getAdvancedStatement().getMetaDataOf( resultSetName );
 			
 			sm_logger.exiting( sm_className, methodName, ret );
 			
 			return ret;
 		}
 		catch( OdaException ex )
 		{
 			sm_logger.logp( Level.SEVERE, sm_className, methodName, 
 							"Cannot get metadata for named resultset.", ex ); //$NON-NLS-1$
 			
 			throw new DataException( ResourceConstants.CANNOT_GET_METADATA_FOR_NAMED_RESULTSET, ex, 
 			                         resultSetName );
 		}
 		catch( UnsupportedOperationException ex )
 		{
 			sm_logger.logp( Level.WARNING, sm_className, methodName, 
 							"Cannot get metadata for named resultset.", ex ); //$NON-NLS-1$
 			
 			throw new DataException( ResourceConstants.CANNOT_GET_METADATA_FOR_NAMED_RESULTSET, ex, 
 			                         resultSetName );
 		}
 	}
 
 	/**
 	 * Executes the statement's query.
 	 * @return	true if this has at least one result set; false otherwise
 	 * @throws DataException	if data source error occurs.
 	 */
 	public boolean execute( ) throws DataException
 	{
 		String methodName = "execute"; //$NON-NLS-1$
 		sm_logger.entering( sm_className, methodName );
 		
 		// when the statement is re-executed, then the previous result set(s)
 		// needs to be invalidated.
 		resetCurrentResultSets();
 		
 		// this will get the result set metadata for the ResultSet in a subsequent
 		// getResultSet() call.  Getting the underlying metadata after the statement 
 		// has been executed may reset its state which will cause the result set not 
 		// to have any data
 		doGetMetaData();
 		
 		try
 		{
 		    boolean ret= false;
 
 			if ( isAdvancedQuery() )
 		        ret = getAdvancedStatement().execute();
 			else // simple statement
 			{
 			    // hold onto its returned result set
 			    // for subsequent call to getResultSet()
 			    m_driverResultSet = m_statement.executeQuery( );
 			    ret = true;
 			}
 
 			if( sm_logger.isLoggingEnterExitLevel() )
 				sm_logger.exiting( sm_className, methodName, Boolean.valueOf( ret ) );
 
 		    return ret;
 		}
 		catch( OdaException ex )
 		{
 			sm_logger.logp( Level.SEVERE, sm_className, methodName, 
 							"Cannot execute statement.", ex ); //$NON-NLS-1$
 			
 			throw new DataException( ResourceConstants.CANNOT_EXECUTE_STATEMENT, ex );
 		}
 		catch( UnsupportedOperationException ex )
 		{
 			sm_logger.logp( Level.SEVERE, sm_className, methodName, 
 							"Cannot execute statement.", ex ); //$NON-NLS-1$
 			
 			throw new DataException( ResourceConstants.CANNOT_EXECUTE_STATEMENT, ex );
 		}
 	}
 	
 	// clear all cached references to the current result sets, 
 	// applies to named and un-named result sets
 	private void resetCurrentResultSets()
 	{
 	    m_driverResultSet = null;
 		m_currentResultSet = null;
 		
 		if( m_namedCurrentResultSets != null )
 			m_namedCurrentResultSets.clear();
 	}
 
 	/**
 	 * Returns the <code>ResultSet</code> instance.
 	 * @return	a <code>ResultSet</code> instance.
 	 * @throws DataException	if data source error occurs.
 	 */
 	public ResultSet getResultSet( ) throws DataException
 	{
 		String methodName = "getResultSet"; //$NON-NLS-1$
 		sm_logger.entering( sm_className, methodName );
 		
 		IResultSet resultSet = null;
 		
 		try
 		{
 			if ( isAdvancedQuery() )
 			    resultSet = getAdvancedStatement().getResultSet();
 			else
 			{
 			    resultSet = m_driverResultSet;
 			    m_driverResultSet = null;
 			}	        		
 		}
 		catch( OdaException ex )
 		{
 			sm_logger.logp( Level.SEVERE, sm_className, methodName,
 							"Cannot get resultset.", ex ); //$NON-NLS-1$
 			
 			throw new DataException( ResourceConstants.CANNOT_GET_RESULTSET, ex );
 		}
 		catch( UnsupportedOperationException ex )
 		{
 			sm_logger.logp( Level.SEVERE, sm_className, methodName,
 							"Cannot get resultset.", ex ); //$NON-NLS-1$
 			
 			throw new DataException( ResourceConstants.CANNOT_GET_RESULTSET, ex );
 		}
 		
 		ResultSet rs = 
 			new ResultSet( resultSet, doGetMetaData() );
 		
 		// keep a pointer to the current result set in case the caller wants 
 		// to get the metadata from the current result set of the statement
 		m_currentResultSet = rs;
 		
 		// reset this for the statement since the caller can 
 		// subsequently change this and the changes won't apply 
 		// to the existing result set
 		m_currentResultClass = null;
 		
 		sm_logger.exiting( sm_className, methodName, rs );
 		
 		return rs;
 	}
 	
 	/**
 	 * Returns the specified named <code>ResultSet</code>.
 	 * @param resultSetName	the name of the result set.
 	 * @return	the named <code>ResultSet</code>.
 	 * @throws DataException	if data source error occurs.
 	 */
 	public ResultSet getResultSet( String resultSetName ) throws DataException
 	{
 		String methodName = "getResultSet"; //$NON-NLS-1$
 		sm_logger.entering( sm_className, methodName, resultSetName );
 		
 		checkNamedResultsSupport();
 		
 		IResultSet resultset = null;
 		
 		try
 		{
 			resultset = 
 			    getAdvancedStatement().getResultSet( resultSetName );
 		}
 		catch( OdaException ex )
 		{
 			sm_logger.logp( Level.SEVERE, sm_className, methodName, 
 							"Cannot get named resultset.", ex ); //$NON-NLS-1$
 			
 			throw new DataException( ResourceConstants.CANNOT_GET_NAMED_RESULTSET, ex, 
 			                         resultSetName );
 		}
 		catch( UnsupportedOperationException ex )
 		{
 			sm_logger.logp( Level.SEVERE, sm_className, methodName, 
 							"Cannot get named resultset.", ex ); //$NON-NLS-1$
 			
 			throw new DataException( ResourceConstants.CANNOT_GET_NAMED_RESULTSET, ex, 
 			                         resultSetName );
 		}
 		
 		ResultSet rs = 
 			new ResultSet( resultset, doGetMetaData( resultSetName ) );
 		
 		// keep this as the current named result set, so the caller can 
 		// get the metadata from the result set from the statement
 		getNamedCurrentResultSets().put( resultSetName, rs );
 		
 		// reset the current result class for the given result set name, so 
 		// subsequent changes won't apply to the existing result set
 		getNamedCurrentResultClasses().remove( resultSetName );
 		
 		sm_logger.exiting( sm_className, methodName, rs );
 		
 		return rs;
 	}
 
 	/**
 	 * Returns the 1-based index of the specified output parameter.
 	 * @param paramName	the name of the parameter.
 	 * @return	the 1-based index of the output parameter.
 	 * @throws DataException	if data source error occurs.
 	 */
 	public int findOutParameter( String paramName ) throws DataException
 	{
 		String methodName = "findOutParameter"; //$NON-NLS-1$
 		sm_logger.entering( sm_className, methodName, paramName );
 		
 		checkOutputParameterSupport( );
 		
 		try
 		{
 			int ret = getAdvancedStatement().findOutParameter( paramName );
 			
 			sm_logger.exiting( sm_className, methodName, ret );			
 			return ret;
 		}
 		catch( OdaException ex )
 		{
 			sm_logger.logp( Level.SEVERE, sm_className, methodName, 
 							"Cannot find output parameter by name.", ex ); //$NON-NLS-1$
 			
 			throw new DataException( ResourceConstants.CANNOT_FIND_OUT_PARAMETER, ex, 
 			                         paramName );
 		}
 		catch( UnsupportedOperationException ex )
 		{
             // this is common, and may be ignored by caller
 			sm_logger.logp( Level.INFO, sm_className, methodName, 
 							"Cannot find output parameter by name.", ex ); //$NON-NLS-1$
 			
 			throw new DataException( ResourceConstants.CANNOT_FIND_OUT_PARAMETER, ex, 
 			                         paramName );
 		}
 	}
 	
 	/**
 	 * Returns the effective ODA data type code for the specified parameter.
 	 * @param paramIndex	the 1-based index of the parameter.
 	 * @return	the ODA <code>java.sql.Types</code> code of the parameter.
 	 * @throws DataException	if data source error occurs.
 	 */
 	public int getParameterType( int paramIndex ) throws DataException
 	{
 		final String methodName = "getParameterType( int )"; //$NON-NLS-1$
 		sm_logger.entering( sm_className, methodName, paramIndex );
 
 	    ParameterMetaData paramMD = getParameterMetaData( paramIndex );
 		assert( paramMD != null );	// invalid paramIndex would have thrown exception
 		int ret = paramMD.getDataType();
 		
 		sm_logger.exiting( sm_className, methodName, ret );
 		return ret;
 	}
 	
 	/**
 	 * Returns the effective ODA data type code for the specified parameter.
 	 * @param paramName	the name of the data set parameter in model.
 	 * @return	the ODA <code>java.sql.Types</code> code of the parameter.
 	 * @throws DataException	if data source error occurs.
 	 */
     public int getParameterType( String paramName ) throws DataException
     {
         ParameterName paramNameObj = new ParameterName( paramName, this );
         return getParameterType( paramNameObj, true );
     }
     
 	private int getParameterType( ParameterName paramName, boolean retryByIndex ) throws DataException
 	{
 		final String methodName = "getParameterType( ParameterName )"; //$NON-NLS-1$
 		sm_logger.entering( sm_className, methodName, paramName );
 
 		ParameterMetaData paramMD = getParameterMetaData( paramName );
 		if( paramMD != null )
 		{
 	        int dataTypeCode = paramMD.getDataType();
 	        
 	        sm_logger.exiting( sm_className, methodName, dataTypeCode );
 	        return dataTypeCode;
 		}
 		
 		// couldn't find matching merged parameter metadata directly by name
 		
 		int parameterType = Types.NULL;
 		if( retryByIndex )
 		{
 		    // try to find corresponding position in design hints
 		    int paramPos = getIndexFromParamHints( paramName.getRomName() );
 	        if( paramPos <= 0 )  // invalid position
 	        {
 	            sm_logger.logp( Level.SEVERE, sm_className, methodName, 
 	                    "Unable to get parameter data type by name nor position." ); //$NON-NLS-1$
 	    
 	            throw new DataException( ResourceConstants.CANNOT_GET_PARAMETER_TYPE,
 	                                        paramName );
 	        }
 
 	        parameterType = getParameterType( paramPos );
 		}
 		else  // probably no info available on the 1-based index position
 		{
             // get the data type by name from the parameter design hints
             parameterType = getOdaTypeFromParamHints( paramName.getRomName(), 0 );
 		}
 
         sm_logger.exiting( sm_className, methodName, parameterType );		
 		return parameterType;
 	}
 
 	/**
 	 * Returns the specified output parameter value.
 	 * @param paramIndex	the 1-based index of the parameter.
 	 * @return	the output value for the specified parameter.
 	 * @throws DataException	if data source error occurs.
 	 */
 	public Object getParameterValue( int paramIndex ) throws DataException
 	{
 		final String methodName = "getParameterValue( int )"; //$NON-NLS-1$
 		sm_logger.entering( sm_className, methodName, paramIndex );
 		
 		Object ret = getParameterValue( null /* n/a paramName */, paramIndex );
 		
 		sm_logger.exiting( sm_className, methodName, ret );
 		return ret;
 	}
 	
 	/**
 	 * Returns the specified output parameter value.
 	 * @param paramName	the name of the parameter.
 	 * @return	the output value for the specified parameter.
 	 * @throws DataException	if data source error occurs.
 	 */
 	public Object getParameterValue( String paramName ) throws DataException
 	{
 		final String methodName = "getParameterValue( String )"; //$NON-NLS-1$
 		sm_logger.entering( sm_className, methodName, paramName );
 		
 		Object ret = getParameterValue( paramName, 0 /* n/a paramIndex */ );
 		
 		sm_logger.exiting( sm_className, methodName, ret );
 		return ret;
 	}
 	
 	/**
 	 * Closes this <code>Statement</code>.
 	 * @throws DataException	if data source error occurs.
 	 */
 	public void close( ) throws DataException
 	{
 		String methodName = "close"; //$NON-NLS-1$
 		sm_logger.entering( sm_className, methodName );
 		
 		resetCachedMetadata();
 		
 		try
 		{
 			m_statement.close( );
 		}
 		catch( OdaException ex )
 		{
 			sm_logger.logp( Level.SEVERE, sm_className, methodName, 
 							"Cannot close statement.", ex ); //$NON-NLS-1$
 			
 			throw new DataException( ResourceConstants.CANNOT_CLOSE_STATEMENT, ex );
 		}
 		catch( UnsupportedOperationException ex )
 		{
 			sm_logger.logp( Level.WARNING, sm_className, methodName, 
 							"Cannot close statement.", ex ); //$NON-NLS-1$
 		}
 		
 		sm_logger.exiting( sm_className, methodName );
 	}
 	
 	private void resetCachedMetadata()
 	{
 		String methodName = "resetCachedMetaData"; //$NON-NLS-1$
 		sm_logger.entering( sm_className, methodName );
 		
 		resetCurrentMetaData();
 		
 		if( m_namedCurrentResultSets != null )
 			m_namedCurrentResultSets.clear();
 		
 		if( m_namedCurrentResultClasses != null )
 			m_namedCurrentResultClasses.clear();
 		
 		sm_logger.exiting( sm_className, methodName );
 	}
 
 	/**
 	 * Adds a <code>ColumnHint</code> for this statement to map design time 
 	 * column projections with runtime result set metadata.
 	 * @param columnHint	a <code>ColumnHint</code> instance.
 	 * @throws DataException	if data source error occurs.
 	 */
 	public void addColumnHint( ColumnHint columnHint ) throws DataException
 	{
 		String methodName = "addColumnHint"; //$NON-NLS-1$
 		sm_logger.entering( sm_className, methodName, columnHint );
 		
 		if( columnHint != null )
 		{
 			// no need to reset the current metadata because adding a column 
 			// hint doesn't change the existing columns that are being projected, 
 			// it just updates some of the column metadata
 			getProjectedColumns().addHint( columnHint );
 		}
 		
 		sm_logger.exiting( sm_className, methodName );
 	}
 	
 	/**
 	 * Adds a <code>ColumnHint</code> for this statement to map design time 
 	 * column projections with the named runtime result set metadata.
 	 * @param resultSetName		the name of the result set.
 	 * @param columnHint		a <code>ColumnHint</code> instance.
 	 * @throws DataException	if data source error occurs.
 	 */
 	public void addColumnHint( String resultSetName, ColumnHint columnHint )
 		throws DataException
 	{
 		String methodName = "addColumnHint"; //$NON-NLS-1$
 		if( sm_logger.isLoggingEnterExitLevel() )
 			sm_logger.entering( sm_className, methodName, 
 								new Object[] { resultSetName, columnHint } );
 		
 		checkNamedResultsSupport();
 		
 		if( columnHint != null )
 		{
 			// no need to reset the current metadata because adding a column 
 			// hint doesn't change the existing columns that are being projected, 
 			// it just updates some of the column metadata
 			getProjectedColumns( resultSetName ).addHint( columnHint );
 		}
 		
 		sm_logger.exiting( sm_className, methodName );
 	}
 
 	private ArrayList getParameterHints()
 	{
 		if( m_parameterHints == null )
 			m_parameterHints = new ArrayList();
 		
 		return m_parameterHints;
 	}
 	
 	/**
 	 * Adds a <code>ParameterHint</code> for this statement to map static 
 	 * parameter definitions with the runtime parameter metadata.
 	 * @param paramHint	a <code>ParameterHint</code> instance.
 	 * @throws DataException	if data source error occurs.
 	 */
 	public void addParameterHint( ParameterHint paramHint ) throws DataException
 	{
 		String methodName = "addParameterHint";		 //$NON-NLS-1$
 		sm_logger.entering( sm_className, methodName, paramHint );
 		
 		if( paramHint != null )
 		{
 			validateAndAddParameterHint( paramHint );
 			
 			// if we've successfully added a parameter hint, then we need to invalidate 
 			// previous version of parameter metadata
 			m_parameterMetaData = null;
 		}
 		
 		sm_logger.exiting( sm_className, methodName );
 	}
 
 	private void validateAndAddParameterHint( ParameterHint newParameterHint )
 		throws DataException
 	{
 		String methodName = "validateAndAddParameterHint";		 //$NON-NLS-1$
 		sm_logger.entering( sm_className, methodName, newParameterHint );
 		
 		ArrayList parameterHintsList = getParameterHints();	
 		String newParamHintName = newParameterHint.getName();
 		int newParamHintIndex = newParameterHint.getPosition();
 		for( int i = 0, n = parameterHintsList.size(); i < n; i++ )
 		{
 			ParameterHint existingParamHint = 
 				(ParameterHint) parameterHintsList.get( i );
 			
 			String existingParamHintName = existingParamHint.getName();
 			if( ! existingParamHintName.equals( newParamHintName ) )
 			{
 				int existingParamHintPosition = existingParamHint.getPosition();
 				
 				// different names and parameter index is either 0 or didn't 
 				// match, so keep on looking
 				if( newParamHintIndex == 0 ||
 					existingParamHintPosition != newParamHintIndex )
 					continue;
 
 				// we don't want to allow different parameter hint name with the 
 				// same parameter hint position
 				sm_logger.logp( Level.SEVERE, sm_className, methodName, 
 								"Different parameter name {0} for same position {1}.", //$NON-NLS-1$
 								new Object[] { existingParamHintName, 
 												new Integer( existingParamHintPosition ) } );
 				
 				throw new DataException( ResourceConstants.DIFFERENT_PARAM_NAME_FOR_SAME_POSITION, 
 										 new Object[] { existingParamHintName, 
 														new Integer( existingParamHintPosition ) } );
 			}
 
 			// the name of the existing hint matches the new hint, 
 			// but the parameter index didn't match.  Ignore the parameter 
 			// index mismatch if either index is 0
 			int existingParamHintIndex = existingParamHint.getPosition();
 			if( existingParamHintIndex != newParamHintIndex && 
 				existingParamHintIndex > 0 && newParamHintIndex > 0 )
 			{
 				sm_logger.logp( Level.SEVERE, sm_className, methodName, 
 								"Same parameter name {0} for different hints.", existingParamHintName ); //$NON-NLS-1$
 				
 				throw new DataException( ResourceConstants.SAME_PARAM_NAME_FOR_DIFFERENT_HINTS,
 										 existingParamHintName );
 			}
 			
 			// no validation is done on their native names, even if both are defined, 
 			// as it is considered a hint attribute, and not an unique identifier
 
 			// same parameter hint name and parameter hint index, so we're 
 			// referring to the same hint, just update the existing one with 
 			// the new info
 			existingParamHint.updateHint( newParameterHint );
             sm_logger.logp( Level.FINE, sm_className, methodName, 
                     "Updating parameter hint with attributes in another hint that has the same name ({0}).", 
                     existingParamHintName ); //$NON-NLS-1$
 			
 			sm_logger.exiting( sm_className, methodName );			
 			return;
 		}
 		
 		// new hint name didn't match any of the existing hints, so we'll need to add 
 		// it to the list.
 		parameterHintsList.add( newParameterHint );
 		
 		sm_logger.exiting( sm_className, methodName );
 	}
 	
 	/**
 	 * Sets the names of all projected columns. If this method is not called, 
 	 * then all columns in the runtime metadata are projected. The specified 
 	 * projected names can be either a column name or column alias.
 	 * @param projectedNames	the projected column names.
 	 * @throws DataException	if data source error occurs.
 	 */
 	public void setColumnsProjection( String[] projectedNames ) 
 		throws DataException
 	{
 		String methodName = "setColumnsProjection";		 //$NON-NLS-1$
 		sm_logger.entering( sm_className, methodName, projectedNames );
 		
 		resetCurrentMetaData();
 		getProjectedColumns().setProjectedNames( projectedNames );
 		
 		sm_logger.exiting( sm_className, methodName );
 	}
 	
 	/**
 	 * Sets the names of all projected columns for the specified result set. If 
 	 * this method is not called, then all columns in the specified result set 
 	 * metadata are projected.  The specified projected names can be either a 
 	 * column name or column alias.
 	 * @param resultSetName	the name of the result set.
 	 * @param projectedNames	the projected column names.
 	 * @throws DataException	if data source error occurs.
 	 */
 	public void setColumnsProjection( String resultSetName, String[] projectedNames ) 
 		throws DataException
 	{
 		String methodName = "setColumnsProjection"; //$NON-NLS-1$
 		if( sm_logger.isLoggingEnterExitLevel() )
 			sm_logger.entering( sm_className, methodName, 
 								new Object[] { resultSetName, projectedNames } );
 		
 		checkNamedResultsSupport();
 		resetCurrentMetaData( resultSetName );
 		getProjectedColumns( resultSetName ).setProjectedNames( projectedNames );
 		
 		sm_logger.exiting( sm_className, methodName );
 	}
 	
 	/**
 	 * Declares a new custom column for the corresponding 
 	 * <code>IResultClass</code>.
 	 * @param columnName	the custom column name.
 	 * @param columnType	the custom column type.
 	 * @throws DataException	if data source error occurs.
 	 */
 	public void declareCustomColumn( String columnName, Class columnType )
 		throws DataException
 	{
 		String methodName = "declareCustomColumn";		 //$NON-NLS-1$
 		if( sm_logger.isLoggingEnterExitLevel() )
 			sm_logger.entering( sm_className, methodName, 
 								new Object[] { columnName, columnType } );
 		
 		assert columnName != null;
 		assert columnName.length() != 0;
 
 		// need to reset current metadata because a custom column could be 
 		// declared after we projected all columns, which means we would 
 		// want to project the newly declared custom column as well
 		resetCurrentMetaData();
 		getProjectedColumns().addCustomColumn( columnName, columnType);
 		
 		sm_logger.exiting( sm_className, methodName );
 	}
 	
 	/**
 	 * Declares a new custom column for the <code>IResultClass</code> of the 
 	 * specified result set.
 	 * @param resultSetName	the name of the result set.
 	 * @param columnName	the custom column name.
 	 * @param columnType	the custom column type.
 	 * @throws DataException	if data source error occurs.
 	 */
 	public void declareCustomColumn( String resultSetName, String columnName, 
 									 Class columnType ) throws DataException
 	{
 		String methodName = "declareCustomColumn";		 //$NON-NLS-1$
 		if( sm_logger.isLoggingEnterExitLevel() )
 			sm_logger.entering( sm_className, methodName, 
 								new Object[] { resultSetName, columnName, columnType } );
 		
 		checkNamedResultsSupport();
 		
 		assert columnName != null;
 		assert columnName.length() != 0;
 		
 		// need to reset current metadata because a custom column could be 
 		// declared after we projected all columns, which means we would 
 		// want to project the newly declared custom column as well
 		resetCurrentMetaData( resultSetName );
 		getProjectedColumns( resultSetName ).addCustomColumn( columnName, 
 		                                                      columnType );
 		
 		sm_logger.exiting( sm_className, methodName );
 	}
 	
 	// if a caller tries to add custom columns or sets a new set of 
 	// column projection, then we want to generate a new set of metadata 
 	// for m_currentResultClass or the specified result set name.  we also 
 	// no longer want to keep the reference to the m_currentResultSet or 
 	// the reference associated with the result set name because we would 
 	// no longer be interested in its metadata afterwards.
 	private void resetCurrentMetaData()
 	{
 		String methodName = "resetCurrentMetaData"; //$NON-NLS-1$
 		sm_logger.entering( sm_className, methodName );
 		
 		m_currentResultClass = null;
 		m_currentResultSet = null;
 		
 		sm_logger.exiting( sm_className, methodName );
 	}
 	
 	private void resetCurrentMetaData( String resultSetName )
 	{
 		String methodName = "resetCurrentMetaData"; //$NON-NLS-1$
 		sm_logger.entering( sm_className, methodName, resultSetName );
 		
 		getNamedCurrentResultClasses().remove( resultSetName );
 		getNamedCurrentResultSets().remove( resultSetName );
 		
 		sm_logger.exiting( sm_className, methodName );
 	}
 	
 	private Hashtable getNamedProjectedColumns()
 	{
 		if( m_namedProjectedColumns == null )
 			m_namedProjectedColumns = new Hashtable();
 		
 		return m_namedProjectedColumns;
 	}
 	
 	private Hashtable getNamedCurrentResultClasses()
 	{
 		if( m_namedCurrentResultClasses == null )
 			m_namedCurrentResultClasses = new Hashtable();
 		
 		return m_namedCurrentResultClasses;
 	}
 	
 	private Hashtable getNamedCurrentResultSets()
 	{
 		if( m_namedCurrentResultSets == null )
 			m_namedCurrentResultSets = new Hashtable();
 		
 		return m_namedCurrentResultSets;
 	}
 	
 	private IQuery getStatement( )
 	{
 		return m_statement;
 	}
 	
 	private IAdvancedQuery getAdvancedStatement()
 	{
 	    assert ( isAdvancedQuery() );
 	    return (IAdvancedQuery) m_statement;
 	}
 	
 	private boolean isAdvancedQuery( )
 	{
 		return ( m_statement instanceof IAdvancedQuery );
 	}
 	
 	private boolean supportsNamedResults() throws DataException
 	{
 		String methodName = "supportsNamedResults"; //$NON-NLS-1$
 		sm_logger.entering( sm_className, methodName );
 		
 		if( m_supportsNamedResults != UNKNOWN )
 		{
 			boolean ret = ( m_supportsNamedResults == TRUE );
 			
 			if( sm_logger.isLoggingEnterExitLevel() )
 				sm_logger.exiting( sm_className, methodName, Boolean.valueOf( ret ) );
 			
 			return ret;
 		}
 
 		// else it's unknown right now
 		boolean b = 
 			m_connection.getMetaData( m_dataSetType ).supportsNamedResultSets( );
 		m_supportsNamedResults = b ? TRUE : FALSE;
 
 		if( sm_logger.isLoggingEnterExitLevel() )
 			sm_logger.exiting( sm_className, methodName, Boolean.valueOf( b ) );
 		return b;
 	}
 	
 	private boolean supportsInputParameter() throws DataException
 	{
 		String methodName = "supportsInputParameter"; //$NON-NLS-1$
 		sm_logger.entering( sm_className, methodName );
 		
 		if( m_supportsInputParameters == null )	// unknown
 		{
 			m_supportsInputParameters =
 				Boolean.valueOf( m_connection.getMetaData( m_dataSetType ).supportsInParameters() );
 		}
 		
 		if( sm_logger.isLoggingEnterExitLevel() )
 			sm_logger.exiting( sm_className, methodName, m_supportsInputParameters );
 		
 		return m_supportsInputParameters.booleanValue();
 	}
 
 	private boolean supportsOutputParameter() throws DataException
 	{
 		String methodName = "supportsOutputParameter"; //$NON-NLS-1$
 		sm_logger.entering( sm_className, methodName );
 		
 		if( m_supportsOutputParameters != UNKNOWN )
 		{
 			boolean ret = ( m_supportsOutputParameters == TRUE );
 			
 			if( sm_logger.isLoggingEnterExitLevel() )
 				sm_logger.exiting( sm_className, methodName, Boolean.valueOf( ret ) );
 			
 			return ret;
 		}
 		
 		// else it's unknown
 		boolean b =
 			m_connection.getMetaData( m_dataSetType ).supportsOutParameters();
 		m_supportsOutputParameters = b ? TRUE : FALSE;
 		
 		if( sm_logger.isLoggingEnterExitLevel() )
 			sm_logger.exiting( sm_className, methodName, Boolean.valueOf( b ) );
 		return b;
 	}
 	
 	public boolean supportsNamedParameter() throws DataException
 	{
 		String methodName = "supportsNamedParameter";	 //$NON-NLS-1$
 		sm_logger.entering( sm_className, methodName );
 		
 		if( m_supportsNamedParameters != UNKNOWN )
 		{
 			boolean ret = ( m_supportsNamedParameters == TRUE );
 			
 			if( sm_logger.isLoggingEnterExitLevel() )
 				sm_logger.exiting( sm_className, methodName, Boolean.valueOf( ret ) );
 			
 			return ret;
 		}
 		
 		// else it's unknown
 		boolean b =
 			m_connection.getMetaData( m_dataSetType ).supportsNamedParameters();
 		m_supportsNamedParameters = b ? TRUE : FALSE;
 		
 		if( sm_logger.isLoggingEnterExitLevel() )
 			sm_logger.exiting( sm_className, methodName, Boolean.valueOf( b ) );	
 		return b;
 	}
 	
 	private void checkNamedResultsSupport( ) 
 		throws DataException
 	{
 		String methodName = "checkNamedResultsSupport"; //$NON-NLS-1$
 		// this can only support named result sets if the underlying object is at 
 		// least an IAdvancedQuery
 		if( ! isAdvancedQuery( ) || ! supportsNamedResults() )
 		{
 			sm_logger.logp( Level.WARNING, sm_className, methodName, 
 							"Named resultsets are not supported." ); //$NON-NLS-1$
 			
 			throw new DataException( ResourceConstants.NAMED_RESULTSETS_UNSUPPORTED, 
 									 new UnsupportedOperationException() );
 		}
 	}
 	
 	/**
 	 * Returns a collection of <code>ParameterMetaData</code>, which contains 
 	 * the parameter metadata information for each parameter that is known at 
 	 * the time that <code>getParameterMetaData()</code> is called.  The 
 	 * collection is retrieved from the ODA runtime driver's <code>IParameterMetaData</code>, 
 	 * if available.  In addition, it includes the supplemental metadata defined in the 
 	 * <code>InputParameterHint</code> and <code>OutputParameterHint</code> provided to this 
 	 * <code>PreparedStatement</code>.   
 	 * @return	a collection of <code>ParameterMetaData</code>, or null 
 	 * 			if no parameter metadata is available.
 	 * @throws DataException	if data source error occurs.
 	 */
 	
 	public Collection getParameterMetaData() throws DataException
 	{
 		String methodName = "getParameterMetaData";	 //$NON-NLS-1$
 		sm_logger.entering( sm_className, methodName );
 		
 		if( m_parameterMetaData == null )
 		{
 		    if( ! supportsInputParameter() &&
 		        ! supportsOutputParameter() )
 		    {
 				sm_logger.logp( Level.INFO, sm_className, methodName, 
 						"The ODA driver does not support any type of parameters (IDataSetMetaData); no metadata is available." ); //$NON-NLS-1$
 				sm_logger.exiting( sm_className, methodName, null );	
 				return null;
 		    }
 		    
 		    // the ODA driver supports in/out parameters
 			IParameterMetaData odaParamMetaData = null;
             try
             {
                 odaParamMetaData = getOdaDriverParamMetaData();
             }
             catch( DataException e )
             {
                 // if parameter hints exist, proceed with
                 // returning its metadata; otherwise, throw exception
         		if( m_parameterHints == null || m_parameterHints.size() <= 0 )
         		    throw e;
             }
      
             m_parameterMetaData = ( odaParamMetaData == null ) ?
 								  mergeParamHints() :
 								  mergeParamHintsWithMetaData( odaParamMetaData );
 		}
 		
 		sm_logger.exiting( sm_className, methodName, m_parameterMetaData );	
 		return m_parameterMetaData;
 	}
 	
 	private ParameterMetaData getParameterMetaData( int paramIndex ) throws DataException
 	{
 		final String methodName = "getParameterMetaData( int )";	 //$NON-NLS-1$
 		sm_logger.entering( sm_className, methodName, paramIndex );
 
 		Collection allParamsMetadata = null; 
 	    if ( paramIndex > 0 )	// index is 1-based
 	        allParamsMetadata = getParameterMetaData();
 	    if ( allParamsMetadata != null )
 	    {	    
 		    Iterator paramMDIter = allParamsMetadata.iterator();
 	        while ( paramMDIter.hasNext() )
 	        {
 	            ParameterMetaData aParamMetaData = (ParameterMetaData) paramMDIter.next();
 	            if ( aParamMetaData.getPosition() == paramIndex )
 	            {
 	        		sm_logger.exiting( sm_className, methodName, aParamMetaData );	
 	                return aParamMetaData;
 	            }
 	        }
 	    }
 	    
 	    // no parameters defined, or didn't find matching parameter index position
 		throw new DataException( ResourceConstants.CANNOT_GET_PARAMETER_METADATA, 
 								new Integer( paramIndex ) );
 	}
 	
     private ParameterMetaData getParameterMetaData( ParameterName paramName ) 
         throws DataException
     {
         final String methodName = "getParameterMetaData( ParameterName )";     //$NON-NLS-1$
         sm_logger.entering( sm_className, methodName, paramName );
 
         ParameterMetaData aParamMetaData =
             findParameterMetaDataByName( getParameterMetaData(), paramName );
         
         sm_logger.exiting( sm_className, methodName, aParamMetaData ); 
         return aParamMetaData;
     }
     
     /**
      * Lookup corresponding native name in merged parameter runtime metadata and design hints.
      */
     private String getNativeNameFromParameterMetaData( String romParamName )
     {
         ParameterMetaData effectiveParamMd = null;
         try
         {
             effectiveParamMd = 
                 findParameterMetaDataByName( getParameterMetaData(), romParamName, false );
 
         }
         catch( DataException ex )
         {
             // ignore; up to caller to decide if it should proceed
         }
 
         return ( effectiveParamMd != null ) ?
             effectiveParamMd.getNativeName() : null;
     }
         
 	private IParameterMetaData getOdaDriverParamMetaData() throws DataException
 	{
 		String methodName = "getOdaDriverParamMetaData";	 //$NON-NLS-1$
 		sm_logger.entering( sm_className, methodName );
 			    
 		IParameterMetaData odaParamMetaData = null;
 	    try
 	    {
 	        odaParamMetaData = m_statement.getParameterMetaData();
 	    }
 		catch( OdaException ex )
 		{
 			sm_logger.logp( Level.SEVERE, sm_className, methodName, 
 							"Cannot get driver's parameter metadata.", ex ); //$NON-NLS-1$
 			throw new DataException( ResourceConstants.CANNOT_GET_PARAMETER_METADATA, ex );
 		}
 		catch( UnsupportedOperationException ex )
 		{
 			sm_logger.logp( Level.WARNING, sm_className, methodName, 
 							"The ODA driver is not capable of providing parameter metadata.", ex ); //$NON-NLS-1$
 			// ignore, and continue to return null metadata
 		}
 
 		sm_logger.exiting( sm_className, methodName, odaParamMetaData );
 		return odaParamMetaData;
 	}
 	
 	private Collection mergeParamHints() throws DataException
 	{
 		String methodName = "mergeParamHints";	 //$NON-NLS-1$
 		sm_logger.entering( sm_className, methodName );
 		
 		ArrayList parameterMetaData = null;
 		
 		// add the parameter hints, if any.
 		if( m_parameterHints != null && m_parameterHints.size() > 0 )
 		{
 			parameterMetaData = new ArrayList();
 			addParameterHints( parameterMetaData, m_parameterHints );
 		}
 	
 		sm_logger.exiting( sm_className, methodName, parameterMetaData );
 		
 		return parameterMetaData;
 	}
 
 	private void addParameterHints( List parameterMetaData, List parameterHints )
 	{
 		String methodName = "addParameterHints"; //$NON-NLS-1$
 		if( sm_logger.isLoggingEnterExitLevel() )
 			sm_logger.entering( sm_className, methodName, 
 								new Object[] { parameterMetaData, parameterHints } );
 		
 		ListIterator iter = parameterHints.listIterator();
 		while( iter.hasNext() )
 		{
 			ParameterHint paramHint = (ParameterHint) iter.next();
 			ParameterMetaData paramMd = new ParameterMetaData( paramHint,
                                                 m_connection.getDataSourceId(), 
                                                 m_dataSetType );
 			parameterMetaData.add( paramMd );						
 		}
 		
 		sm_logger.exiting( sm_className, methodName );
 	}
 	
 	private Collection mergeParamHintsWithMetaData( IParameterMetaData runtimeParamMetaData )
 		throws DataException
 	{
 		String methodName = "mergeParamHintsWithMetaData"; //$NON-NLS-1$
 		sm_logger.entering( sm_className, methodName, runtimeParamMetaData );
 		
 		assert( runtimeParamMetaData != null );
 		
         // first create a ParameterMetaData for each parameter,
         // based on runtime metadata
 		int numOfParameters = doGetParameterCount( runtimeParamMetaData );
 		ArrayList paramMetaData = new ArrayList( numOfParameters );
 		
 		for( int i = 1; i <= numOfParameters; i++ )
 		{
 			ParameterMetaData paramMd = 
 				new ParameterMetaData( runtimeParamMetaData, i, 
 				                       m_connection.getDataSourceId(), 
 				                       m_dataSetType );
 			paramMetaData.add( paramMd );
 		}
 		
         // then supplement all parameters' runtime metadata with design hints
 		if( m_parameterHints != null && m_parameterHints.size() > 0 )
 			updateWithParameterHints( paramMetaData, m_parameterHints );
 
 		sm_logger.exiting( sm_className, methodName, paramMetaData );
 		
 		return paramMetaData;
 	}
 	
 	private int doGetParameterCount( IParameterMetaData runtimeParamMetaData )
 		throws DataException
 	{
 		String methodName = "doGetParameterCount";	 //$NON-NLS-1$
 		sm_logger.entering( sm_className, methodName, runtimeParamMetaData );
 		
 		try
 		{
 			int ret = runtimeParamMetaData.getParameterCount();
 			
 			sm_logger.exiting( sm_className, methodName, ret );			
 			return ret;
 		}
 		catch( OdaException ex )
 		{
 			sm_logger.logp( Level.SEVERE, sm_className, methodName, 
 							"Cannot get parameter count.", ex ); //$NON-NLS-1$
 			
 			throw new DataException( ResourceConstants.CANNOT_GET_PARAMETER_COUNT, ex );
 		}
 		catch( UnsupportedOperationException ex )
 		{
 			sm_logger.logp( Level.WARNING, sm_className, methodName, 
 							"Cannot get parameter count.", ex ); //$NON-NLS-1$
 			
 			throw new DataException( ResourceConstants.CANNOT_GET_PARAMETER_COUNT, ex );
 		}
 	}
 	
     /**
      * Supplement runtime parameter metadata with design hints.
      * @param parametersMetaData
      * @param parameterHints
      * @throws DataException
      */
 	private void updateWithParameterHints( List parametersMetaData, 
 									   	   List parameterHints )
 		throws DataException
 	{
 		final String methodName = "updateWithParameterHints"; //$NON-NLS-1$
 		if( sm_logger.isLoggingEnterExitLevel() )
 			sm_logger.entering( sm_className, methodName, 
 								new Object[] { parametersMetaData, parameterHints } );
 		
 		if( parametersMetaData == null || parametersMetaData.isEmpty() ||
 		    parameterHints == null || parameterHints.isEmpty() )
 		{
 	        sm_logger.exiting( sm_className, methodName );
 		    return;   // nothing to update or update with
 		}
 		
 		ListIterator iter = parameterHints.listIterator();
 		while( iter.hasNext() )
 		{
 			ParameterHint paramHint = (ParameterHint) iter.next();
 			
 			// find corresponding parameter metadata to update			
 			ParameterMetaData paramMd = 
 			    findParameterMetaData( parametersMetaData, paramHint );
 			if( paramMd == null )
                 continue;   // can't find a runtime parameter metadata that matches the hint
 
             // found matching runtime parameter metadata and design hint,
             // merge design hint into runtime metadata
             paramMd.updateWith( paramHint,
                                 m_connection.getDataSourceId(), 
                                 m_dataSetType );
 		}
 		
 		sm_logger.exiting( sm_className, methodName );
 	}
 
 	private ParameterMetaData findParameterMetaData( List parametersMetaData, 
 	        ParameterHint paramHint )
         throws DataException
 	{
         String paramHintNativeName = paramHint.getNativeName();
         int position = 0;
         if( hasValue( paramHintNativeName ) )
         {
             ParameterMetaData paramMd = 
                 findParameterMetaDataByName( parametersMetaData, paramHintNativeName, true );
             if( paramMd != null )       // found a match by native name
                 return paramMd;         // done
 
             // next try to get the parameter index by native name from the runtime driver
             if( paramHint.isInputMode() )
                 position = getRuntimeParameterIndexFromName( paramHintNativeName, true /* forInput */ );
 
             if( paramHint.isOutputMode() &&
                 ( position <= 0 || position > parametersMetaData.size() ))
             {
                 position = getRuntimeParameterIndexFromName( paramHintNativeName, false /* forInput */ );
             }
         }
         
         // couldn't find the index by the param native name, 
         // use the position in the hint itself.
         int numOfRuntimeParameters = parametersMetaData.size();
         if( position <= 0 || position > numOfRuntimeParameters )    // position not yet found
             position = paramHint.getPosition();
         
         // can't find a match of the given hint among runtime parameter metadata
         if( position <= 0 || position > numOfRuntimeParameters )    // invalid position value
             return null;   
 
         // has valid 1-based position, return corresponding metadata
         return (ParameterMetaData) parametersMetaData.get( position - 1 );    
 	}
 	
     private static ParameterMetaData findParameterMetaDataByName( Collection parametersMetaData, 
             ParameterName paramName )
     {
         if( paramName == null )    
             return null;    // nothing to match against
         
         // first try to find a match by its native name
         ParameterMetaData paramMd = findParameterMetaDataByName( parametersMetaData,
                     paramName.getNativeName(), true );
         
         // if not found, or no native name defined, 
         // next find a match by its ROM name
         if( paramMd == null )
             paramMd = findParameterMetaDataByName( parametersMetaData,
                     paramName.getRomName(), false );
         
         // still not found, try find a match by its effective name that will be used to
         // interact with underlying ODA driver
         if( paramMd == null )
             paramMd = findParameterMetaDataByName( parametersMetaData,
                     paramName.getEffectiveName(), true );
         
         return paramMd;
     }
     
     private static ParameterMetaData findParameterMetaDataByName( Collection parametersMetaData, 
             String paramName, boolean useNativeName )
     {
         // empty name is not unique and cannot be used to find a unique match
         if( parametersMetaData == null || parametersMetaData.isEmpty() ||
             ! hasValue( paramName ) )    
             return null;    // nothing to match against
         
         Iterator iter = parametersMetaData.iterator();
         while( iter.hasNext() )
         {
             ParameterMetaData paramMd = (ParameterMetaData) iter.next();
             
             if( useNativeName && paramName.equals( paramMd.getNativeName() ))
                 return paramMd;
             if( ! useNativeName && paramName.equals( paramMd.getName() ))
                 return paramMd;
         }
         return null;
     }
 
 	private int getRuntimeParameterIndexFromName( String paramName, boolean forInput )
 		throws DataException
 	{
 		String methodName = "getRuntimeParameterIndexFromName"; //$NON-NLS-1$
 		if( sm_logger.isLoggingEnterExitLevel() )
 			sm_logger.entering( sm_className, methodName, 
 								new Object[] { paramName, Boolean.valueOf( forInput ) } );
 		
 		if( forInput )
 		{
 			try
 			{	
 				int ret = findInParameter( paramName );
 				
 				sm_logger.exiting( sm_className, methodName, ret );				
 				return ret;
 			}
 			catch( DataException ex )
 			{
 				// findInParameter is not supported by underlying ODA driver
 				if( ex.getCause() instanceof UnsupportedOperationException )
 				{
 					sm_logger.exiting( sm_className, methodName, 0 );					
 					return 0;
 				}
 				
 				sm_logger.logp( Level.SEVERE, sm_className, methodName, 
 								"Cannot get runtime parameter index.", ex ); //$NON-NLS-1$
 				
 				throw ex;
 			}
 		}
 		
 		// for output parameter
 		try
 		{
 			int ret = findOutParameter( paramName );
 			
 			sm_logger.exiting( sm_className, methodName, ret );			
 			return ret;
 		}
 		catch( DataException ex )
 		{
 			// findOutParameter is not supported
 			if( ex.getCause() instanceof UnsupportedOperationException )
 			{
 				sm_logger.exiting( sm_className, methodName, 0 );				
 				return 0;
 			}
 			
 			sm_logger.logp( Level.SEVERE, sm_className, methodName, 
 							"Cannot get runtime parameter index.", ex ); //$NON-NLS-1$
 			
 			throw ex;
 		}
 	}
 	
 	private void checkOutputParameterSupport( ) 
 		throws DataException
 	{
 		String methodName = "checkOutputParameterSupport"; //$NON-NLS-1$
 		
 		// this can only support output parameter if the underlying object is at 
 		// least an IAdvancedQuery
 		if( ! isAdvancedQuery( ) || ! supportsOutputParameter() ) 
 		{
 			sm_logger.logp( Level.WARNING, sm_className, methodName, 
 							"Output parameters are not supported." ); //$NON-NLS-1$
 			
 			throw new DataException( ResourceConstants.OUTPUT_PARAMETERS_UNSUPPORTED, 
 									 new UnsupportedOperationException() );
 		}
 	}
 	
 	private Object getParameterValue( String paramName, int paramIndex ) 
 		throws DataException
 	{
 		final String methodName = "getParameterValue( String, int )";	 //$NON-NLS-1$
 		if( sm_logger.isLoggingEnterExitLevel() )
 			sm_logger.entering( sm_className, methodName, 
 								new Object[] { paramName, new Integer( paramIndex ) } );
 		
 		checkOutputParameterSupport( );
 
         // delegate to ParameterName for the proper name to use when
         // interacting with underlying oda runtime driver
         ParameterName paramNameObj = null;
         if( paramName != null )   // getting parameter value by name
         {
             paramNameObj = new ParameterName( paramName, this );
             
             // log if not able to find corresponding native name
             paramNameObj.logNullNativeName();
         }
 		
 		Object paramValue = null;
 		int paramType = ( paramNameObj == null ) ? getParameterType( paramIndex ) :
 												getParameterType( paramNameObj, false );
 
 		switch( paramType )
 		{
 			case Types.INTEGER:
 				int i = ( paramNameObj == null ) ?
 						doGetInt( paramIndex ) :
 						getInt( paramNameObj );
 				if( ! wasNull() )
 					paramValue = new Integer( i );
 				break;
 				
 			case Types.DOUBLE:
 				double d = ( paramNameObj == null ) ?
 						   doGetDouble( paramIndex ) :
 						   getDouble( paramNameObj );
 				if( ! wasNull() )
 					paramValue = new Double( d );
 				break;
 					
 			case Types.CHAR:
 				paramValue = ( paramNameObj == null ) ?
 							 doGetString( paramIndex ) :
 							 getString( paramNameObj );
 				break;
 			
 			case Types.DECIMAL:
 				paramValue = ( paramNameObj == null ) ?
 							 doGetBigDecimal( paramIndex ) :
 							 getBigDecimal( paramNameObj );
 				break;
 				
 			case Types.DATE:
 				paramValue = ( paramNameObj == null ) ?
 							 doGetDate( paramIndex ) :
 							 getDate( paramNameObj );
 				break;
 				
 			case Types.TIME:
 				paramValue = ( paramNameObj == null ) ?
 							 doGetTime( paramIndex ) :
 							 getTime( paramNameObj );
 				break;
 				
 			case Types.TIMESTAMP:
 				paramValue = ( paramNameObj == null ) ?
 							 doGetTimestamp( paramIndex ) :
 							 getTimestamp( paramNameObj );
 				break;
 				
 			case Types.BLOB:
 				paramValue = ( paramNameObj == null ) ?
 							 doGetBlob( paramIndex ) :
 							 getBlob( paramNameObj );
 				break;
 				
 			case Types.CLOB:
 				paramValue = ( paramNameObj == null ) ?
 							 doGetClob( paramIndex ) :
 							 getClob( paramNameObj );
 				break;
                 
             case Types.BOOLEAN:
                 paramValue = ( paramNameObj == null ) ?
                             doGetBoolean( paramIndex ) :
                             getBoolean( paramNameObj );
                 break;
 				
 			default:
 				assert false;	// exception now thrown by DriverManager
 		}
 		
 		Object ret = ( wasNull( ) ) ? null : paramValue;
 		
 		sm_logger.exiting( sm_className, methodName, ret );	
 		return ret;
 	}
 	
 	// the following data type getters are by name and need additional processing in the 
 	// case where a named parameter is not supported by the underlying data source.  
 	// In that case, we will look at the output parameter hints to get the name to 
 	// id mapping
     
 	private int getInt( ParameterName paramName ) throws DataException
 	{
 		final String methodName = "getInt( ParameterName )"; //$NON-NLS-1$
 		sm_logger.entering( sm_className, methodName, paramName );
 		
 		int ret = 0;
 		
 		if( ! supportsNamedParameter() )
 		{
 			int paramIndex = getIndexFromParamHints( paramName.getRomName() );
 			if( paramIndex > 0 )
 				ret = doGetInt( paramIndex );
 		}
 		else
 		{
 			ret = doGetInt( paramName );
 		}
 		
 		sm_logger.exiting( sm_className, methodName, ret );	
 		return ret;
 	}
 
 	private double getDouble( ParameterName paramName ) throws DataException
 	{
 		final String methodName = "getDouble( ParameterName )";	 //$NON-NLS-1$
 		sm_logger.entering( sm_className, methodName, paramName );
 		
 		double ret = 0;
 		
 		if( ! supportsNamedParameter() )
 		{
 			int paramIndex = getIndexFromParamHints( paramName.getRomName() );
 			if( paramIndex > 0 )
 				ret = doGetDouble( paramIndex );
 		}
 		else
 		{
 			ret = doGetDouble( paramName );
 		}
 		
 		if( sm_logger.isLoggingEnterExitLevel() )
 			sm_logger.exiting( sm_className, methodName, new Double( ret ) );	
 		return ret;
 	}
 	
 	private String getString( ParameterName paramName ) throws DataException
 	{
 		final String methodName = "getString( ParameterName )"; //$NON-NLS-1$
 		sm_logger.entering( sm_className, methodName, paramName );
 		
 		String ret = null;
 		
 		if( ! supportsNamedParameter() )
 		{
 			int paramIndex = getIndexFromParamHints( paramName.getRomName() );
 			if( paramIndex > 0 )
 				ret = doGetString( paramIndex );
 		}
 		else
 		{
 			ret = doGetString( paramName );
 		}
 		
 		sm_logger.exiting( sm_className, methodName, ret );
 		return ret;
 	}
 	
 	private BigDecimal getBigDecimal( ParameterName paramName ) throws DataException
 	{
 		final String methodName = "getBigDecimal( ParameterName )"; //$NON-NLS-1$
 		sm_logger.entering( sm_className, methodName, paramName );
 		
 		BigDecimal ret = null;
 		
 		if( ! supportsNamedParameter() )
 		{
 			int paramIndex = getIndexFromParamHints( paramName.getRomName() );
 			if( paramIndex > 0 )
 				ret = doGetBigDecimal( paramIndex );
 		}
 		else
 		{
 			ret = doGetBigDecimal( paramName );
 		}
 		
 		sm_logger.exiting( sm_className, methodName, ret );
 		return ret;
 	}
 
 	private java.util.Date getDate( ParameterName paramName ) throws DataException
 	{
 		final String methodName = "getDate( ParameterName )"; //$NON-NLS-1$
 		sm_logger.entering( sm_className, methodName, paramName );
 		
 		java.util.Date ret = null;
 		
 		if( ! supportsNamedParameter() )
 		{
 			int paramIndex = getIndexFromParamHints( paramName.getRomName() );
 			if( paramIndex > 0 )
 				ret = doGetDate( paramIndex );
 		}
 		else
 		{
 			ret = doGetDate( paramName );
 		}
 		
 		sm_logger.exiting( sm_className, methodName, ret );
 		return ret;
 	}
 	
 	private Time getTime( ParameterName paramName ) throws DataException
 	{
 		final String methodName = "getTime( ParameterName )"; //$NON-NLS-1$
 		sm_logger.entering( sm_className, methodName, paramName );
 		
 		Time ret = null;
 		
 		if( ! supportsNamedParameter() )
 		{
 			int paramIndex = getIndexFromParamHints( paramName.getRomName() );
 			if( paramIndex > 0 )
 				ret = doGetTime( paramIndex );
 		}
 		else
 		{
 			ret = doGetTime( paramName );
 		}
 		
 		sm_logger.exiting( sm_className, methodName, ret );
 		return ret;
 	}
 	
 	private Timestamp getTimestamp( ParameterName paramName ) throws DataException
 	{
 		final String methodName = "getTimestamp( ParameterName )"; //$NON-NLS-1$
 		sm_logger.entering( sm_className, methodName, paramName );
 		
 		Timestamp ret = null;
 		
 		if( ! supportsNamedParameter() )
 		{
 			int paramIndex = getIndexFromParamHints( paramName.getRomName() );
 			if( paramIndex > 0 )
 				ret = doGetTimestamp( paramIndex );
 		}
 		else
 		{
 			ret = doGetTimestamp( paramName );
 		}
 		
 		sm_logger.exiting( sm_className, methodName, ret );
 		return ret;
 	}
 	
 	private IBlob getBlob( ParameterName paramName ) throws DataException
 	{
 		final String methodName = "getBlob( ParameterName )"; //$NON-NLS-1$
 		sm_logger.entering( sm_className, methodName, paramName );
 		
 		IBlob ret = null;
 		
 		if( ! supportsNamedParameter() )
 		{
 			int paramIndex = getIndexFromParamHints( paramName.getRomName() );
 			if( paramIndex > 0 )
 				ret = doGetBlob( paramIndex );
 		}
 		else
 		{
 			ret = doGetBlob( paramName );
 		}
 		
 		sm_logger.exiting( sm_className, methodName, ret );
 		return ret;
 	}
 	
 	private IClob getClob( ParameterName paramName ) throws DataException
 	{
 		final String methodName = "getClob( ParameterName )"; //$NON-NLS-1$
 		sm_logger.entering( sm_className, methodName, paramName );
 		
 		IClob ret = null;
 		
 		if( ! supportsNamedParameter() )
 		{
 			int paramIndex = getIndexFromParamHints( paramName.getRomName() );
 			if( paramIndex > 0 )
 				ret = doGetClob( paramIndex );
 		}
 		else
 		{
 			ret = doGetClob( paramName );
 		}
 		
 		sm_logger.exiting( sm_className, methodName, ret );
 		return ret;
 	}
     
     private Boolean getBoolean( ParameterName paramName ) throws DataException
     {
         final String methodName = "getBoolean( ParameterName )"; //$NON-NLS-1$
         sm_logger.entering( sm_className, methodName, paramName );
         
         Boolean ret = null;
         
         if( ! supportsNamedParameter() )
         {
             int paramIndex = getIndexFromParamHints( paramName.getRomName() );
             if( paramIndex > 0 )
                 ret = doGetBoolean( paramIndex );
         }
         else
         {
             ret = doGetBoolean( paramName );
         }
         
         sm_logger.exiting( sm_className, methodName, ret ); 
         return ret;
     }
 	
 	private int doGetInt( int paramIndex ) throws DataException
 	{
 		final String methodName = "doGetInt( int )"; //$NON-NLS-1$
         final String errorCode = ResourceConstants.CANNOT_GET_INT_FROM_PARAMETER;
 		sm_logger.entering( sm_className, methodName, paramIndex );
 		
 		try
 		{
 			int ret = getAdvancedStatement().getInt( paramIndex );
 			
 			sm_logger.exiting( sm_className, methodName, ret );			
 			return ret;
 		}
 		catch( OdaException ex )
 		{
             handleException( ex, errorCode, methodName, paramIndex );
 		}
 		catch( UnsupportedOperationException ex )
 		{
             handleException( ex, errorCode, methodName, paramIndex );
 		}
         return 0;
 	}
 	
 	private int doGetInt( ParameterName paramName ) throws DataException
 	{
 		final String methodName = "doGetInt( ParameterName )"; //$NON-NLS-1$
         final String errorCode = ResourceConstants.CANNOT_GET_INT_FROM_PARAMETER;
 		sm_logger.entering( sm_className, methodName, paramName );
 		
         String effectiveParamName = paramName.getEffectiveName();
 		try
 		{
 			int ret = getAdvancedStatement().getInt( effectiveParamName );
 			
 			sm_logger.exiting( sm_className, methodName, ret );
 			
 			return ret;
 		}
 		catch( OdaException ex )
 		{
             handleException( ex, errorCode, methodName, effectiveParamName );
 		}
 		catch( UnsupportedOperationException ex )
 		{
             handleException( ex, errorCode, methodName, effectiveParamName );
 		}
         return 0;
 	}
 	
 	private double doGetDouble( int paramIndex ) throws DataException
 	{
 		final String methodName = "doGetDouble( int )";		 //$NON-NLS-1$
         final String errorCode = ResourceConstants.CANNOT_GET_DOUBLE_FROM_PARAMETER;
 		sm_logger.entering( sm_className, methodName, paramIndex );
 		
 		try
 		{
 			double ret = getAdvancedStatement().getDouble( paramIndex );
 			
 			if( sm_logger.isLoggingEnterExitLevel() )
 				sm_logger.exiting( sm_className, methodName, new Double( ret ) );
 			
 			return ret;
 		}
 		catch( OdaException ex )
 		{
             handleException( ex, errorCode, methodName, paramIndex );
 		}
 		catch( UnsupportedOperationException ex )
 		{
             handleException( ex, errorCode, methodName, paramIndex );
 		}
         return 0;
 	}
 	
 	private double doGetDouble( ParameterName paramName ) throws DataException
 	{
 		final String methodName = "doGetDouble( ParameterName )";		 //$NON-NLS-1$
         final String errorCode = ResourceConstants.CANNOT_GET_DOUBLE_FROM_PARAMETER;
 		sm_logger.entering( sm_className, methodName, paramName );
 		
         String effectiveParamName = paramName.getEffectiveName();
 		try
 		{
 			double ret = getAdvancedStatement().getDouble( effectiveParamName );
 			
 			if( sm_logger.isLoggingEnterExitLevel() )
 				sm_logger.exiting( sm_className, methodName, new Double( ret ) );
 			
 			return ret;
 		}
 		catch( OdaException ex )
 		{
             handleException( ex, errorCode, methodName, effectiveParamName );
 		}
 		catch( UnsupportedOperationException ex )
 		{
             handleException( ex, errorCode, methodName, effectiveParamName );
 		}
         return 0;
 	}
 	
 	private String doGetString( int paramIndex ) throws DataException
 	{
 		final String methodName = "doGetString( int )"; //$NON-NLS-1$
         final String errorCode = ResourceConstants.CANNOT_GET_STRING_FROM_PARAMETER;
 		sm_logger.entering( sm_className, methodName, paramIndex );
 		
 		try
 		{
 			String ret = getAdvancedStatement().getString( paramIndex );
 			
 			sm_logger.exiting( sm_className, methodName, ret );
 			
 			return ret;
 		}
 		catch( OdaException ex )
 		{
             handleException( ex, errorCode, methodName, paramIndex );
 		}
 		catch( UnsupportedOperationException ex )
 		{
             handleException( ex, errorCode, methodName, paramIndex );
 		}
         return null;
 	}
 	
 	private String doGetString( ParameterName paramName ) throws DataException
 	{
 		final String methodName = "doGetString( ParameterName )"; //$NON-NLS-1$
         final String errorCode = ResourceConstants.CANNOT_GET_STRING_FROM_PARAMETER;
 		sm_logger.entering( sm_className, methodName, paramName );
 		
         String effectiveParamName = paramName.getEffectiveName();
 		try
 		{
 			String ret = getAdvancedStatement().getString( effectiveParamName );
 			
 			sm_logger.exiting( sm_className, methodName, ret );
 			
 			return ret;
 		}
 		catch( OdaException ex )
 		{
             handleException( ex, errorCode, methodName, effectiveParamName );
 		}
 		catch( UnsupportedOperationException ex )
 		{
             handleException( ex, errorCode, methodName, effectiveParamName );
 		}
         return null;
 	}
 	
 	private BigDecimal doGetBigDecimal( int paramIndex ) throws DataException
 	{
 		final String methodName = "doGetBigDecimal( int )"; //$NON-NLS-1$
         final String errorCode = ResourceConstants.CANNOT_GET_BIGDECIMAL_FROM_PARAMETER;
 		sm_logger.entering( sm_className, methodName, paramIndex );
 		
 		try
 		{
 			BigDecimal ret = getAdvancedStatement().getBigDecimal( paramIndex );
 			
 			sm_logger.exiting( sm_className, methodName, ret );
 			return ret;
 		}
 		catch( OdaException ex )
 		{
             handleException( ex, errorCode, methodName, paramIndex );
 		}
 		catch( UnsupportedOperationException ex )
 		{
             handleException( ex, errorCode, methodName, paramIndex );
 		}
         return null;
 	}
 	
 	private BigDecimal doGetBigDecimal( ParameterName paramName ) throws DataException
 	{
 		final String methodName = "doGetBigDecimal( ParameterName )";  //$NON-NLS-1$
         final String errorCode = ResourceConstants.CANNOT_GET_BIGDECIMAL_FROM_PARAMETER;
 		sm_logger.entering( sm_className, methodName, paramName );
 		
         String effectiveParamName = paramName.getEffectiveName();
 		try
 		{
 			BigDecimal ret = 
 			    getAdvancedStatement().getBigDecimal( effectiveParamName );
 			
 			sm_logger.exiting( sm_className, methodName, ret );			
 			return ret;
 		}
 		catch( OdaException ex )
 		{
             handleException( ex, errorCode, methodName, effectiveParamName );
 		}
 		catch( UnsupportedOperationException ex )
 		{
             handleException( ex, errorCode, methodName, effectiveParamName );
 		}
         return null;
 	}
 	
 	private java.util.Date doGetDate( int paramIndex ) throws DataException
 	{
 		final String methodName = "doGetDate( int )";		 //$NON-NLS-1$
         final String errorCode = ResourceConstants.CANNOT_GET_DATE_FROM_PARAMETER;
 		sm_logger.entering( sm_className, methodName, paramIndex );
 		
 		try
 		{
 			java.util.Date ret = getAdvancedStatement().getDate( paramIndex );
 			
 			sm_logger.exiting( sm_className, methodName, ret );			
 			return ret;
 		}
 		catch( OdaException ex )
 		{
             handleException( ex, errorCode, methodName, paramIndex );
 		}
 		catch( UnsupportedOperationException ex )
 		{
             handleException( ex, errorCode, methodName, paramIndex );
 		}
         return null;
 	}
 	
 	private java.util.Date doGetDate( ParameterName paramName ) throws DataException
 	{
 		final String methodName = "doGetDate( ParameterName )";		 //$NON-NLS-1$
         final String errorCode = ResourceConstants.CANNOT_GET_DATE_FROM_PARAMETER;
 		sm_logger.entering( sm_className, methodName, paramName );
 		
         String effectiveParamName = paramName.getEffectiveName();
 		try
 		{
 			java.util.Date ret = getAdvancedStatement().getDate( effectiveParamName );
 			
 			sm_logger.exiting( sm_className, methodName, ret );
 			return ret;
 		}
 		catch( OdaException ex )
 		{
             handleException( ex, errorCode, methodName, effectiveParamName );
 		}
 		catch( UnsupportedOperationException ex )
 		{
             handleException( ex, errorCode, methodName, effectiveParamName );
 		}
         return null;
 	}
 	
 	private Time doGetTime( int paramIndex ) throws DataException
 	{
 		final String methodName = "doGetTime( int )"; //$NON-NLS-1$
         final String errorCode = ResourceConstants.CANNOT_GET_TIME_FROM_PARAMETER;
 		sm_logger.entering( sm_className, methodName, paramIndex );
 		
 		try
 		{
 			Time ret = getAdvancedStatement().getTime( paramIndex );
 			
 			sm_logger.exiting( sm_className, methodName, ret );
 			return ret;
 		}
 		catch( OdaException ex )
 		{
             handleException( ex, errorCode, methodName, paramIndex );
 		}
 		catch( UnsupportedOperationException ex )
 		{
             handleException( ex, errorCode, methodName, paramIndex );
 		}
         return null;
 	}
 	
 	private Time doGetTime( ParameterName paramName ) throws DataException
 	{
 		final String methodName = "doGetTime( ParameterName )"; //$NON-NLS-1$
         final String errorCode = ResourceConstants.CANNOT_GET_TIME_FROM_PARAMETER;
 		sm_logger.entering( sm_className, methodName, paramName );
 		
         String effectiveParamName = paramName.getEffectiveName();
 		try
 		{
 			Time ret = getAdvancedStatement().getTime( effectiveParamName );
 
 			sm_logger.exiting( sm_className, methodName, ret );
 			return ret;
 		}
 		catch( OdaException ex )
 		{
             handleException( ex, errorCode, methodName, effectiveParamName );
 		}
 		catch( UnsupportedOperationException ex )
 		{
             handleException( ex, errorCode, methodName, effectiveParamName );
 		}
         return null;
 	}
 	
 	private Timestamp doGetTimestamp( int paramIndex ) throws DataException
 	{
 		final String methodName = "doGetTimestamp( int )"; //$NON-NLS-1$
         final String errorCode = ResourceConstants.CANNOT_GET_TIMESTAMP_FROM_PARAMETER;
 		sm_logger.entering( sm_className, methodName, paramIndex );
 
 		try
 		{
 		    Timestamp ret = getAdvancedStatement().getTimestamp( paramIndex );
 
 			sm_logger.exiting( sm_className, methodName, ret );
 			return ret;
 		}
 		catch( OdaException ex )
 		{
             handleException( ex, errorCode, methodName, paramIndex );
 		}
 		catch( UnsupportedOperationException ex )
 		{
             handleException( ex, errorCode, methodName, paramIndex );
 		}
         return null;
 	}
 	
 	private Timestamp doGetTimestamp( ParameterName paramName ) throws DataException
 	{
 		final String methodName = "doGetTimestamp( ParameterName )"; //$NON-NLS-1$
         final String errorCode = ResourceConstants.CANNOT_GET_TIMESTAMP_FROM_PARAMETER;
 		sm_logger.entering( sm_className, methodName, paramName );
 		
         String effectiveParamName = paramName.getEffectiveName();
 		try
 		{
 			Timestamp ret = getAdvancedStatement().getTimestamp( effectiveParamName );
 
 			sm_logger.exiting( sm_className, methodName, ret );
 			return ret;
 		}
 		catch( OdaException ex )
 		{
             handleException( ex, errorCode, methodName, effectiveParamName );
 		}
 		catch( UnsupportedOperationException ex )
 		{
             handleException( ex, errorCode, methodName, effectiveParamName );
 		}
         return null;
 	}
 	
 	private IBlob doGetBlob( int paramIndex ) throws DataException
 	{
 		final String methodName = "doGetBlob( int )"; //$NON-NLS-1$
 		sm_logger.entering( sm_className, methodName, paramIndex );
 
 		try
 		{
 		    IBlob ret = getAdvancedStatement().getBlob( paramIndex );
 
 			sm_logger.exiting( sm_className, methodName, ret );
 			return ret;
 		}
 		catch( OdaException ex )
 		{
 		    logAndThrowGetBlobParamException( methodName, 
 		            			new Integer( paramIndex ), ex );
 		}
 		catch( UnsupportedOperationException ex )
 		{
 		    logAndThrowGetBlobParamException( methodName, 
         						new Integer( paramIndex ), ex );
 		}
         return null;
 	}
 	
 	private IBlob doGetBlob( ParameterName paramName ) throws DataException
 	{
 		final String methodName = "doGetBlob( ParameterName )"; //$NON-NLS-1$
 		sm_logger.entering( sm_className, methodName, paramName );
 		
         String effectiveParamName = paramName.getEffectiveName();
 		try
 		{
 		    IBlob ret = getAdvancedStatement().getBlob( effectiveParamName );
 
 			sm_logger.exiting( sm_className, methodName, ret );
 			return ret;
 		}
 		catch( OdaException ex )
 		{
 		    logAndThrowGetBlobParamException( methodName, effectiveParamName, ex );
 		}
 		catch( UnsupportedOperationException ex )
 		{
 		    logAndThrowGetBlobParamException( methodName, effectiveParamName, ex );
 		}
         return null;
 	}
 	
 	private void logAndThrowGetBlobParamException( String methodName, 
 	        Object parameterId, Exception ex ) throws DataException
 	{
         final String errorCode = ResourceConstants.CANNOT_GET_BLOB_FROM_PARAMETER;
         handleException( ex, errorCode, methodName, parameterId );
 	}
 	
 	private IClob doGetClob( int paramIndex ) throws DataException
 	{
 		final String methodName = "doGetClob( int )"; //$NON-NLS-1$
 		sm_logger.entering( sm_className, methodName, paramIndex );
 
 		try
 		{
 		    IClob ret = getAdvancedStatement().getClob( paramIndex );
 
 			sm_logger.exiting( sm_className, methodName, ret );
 			return ret;
 		}
 		catch( OdaException ex )
 		{
 		    logAndThrowGetClobParamException( methodName, 
 		            			new Integer( paramIndex ), ex );
 		}
 		catch( UnsupportedOperationException ex )
 		{
 		    logAndThrowGetClobParamException( methodName, 
         						new Integer( paramIndex ), ex );
 		}
         return null;
 	}
 	
 	private IClob doGetClob( ParameterName paramName ) throws DataException
 	{
 		final String methodName = "doGetClob( ParameterName )"; //$NON-NLS-1$
 		sm_logger.entering( sm_className, methodName, paramName );
 		
         String effectiveParamName = paramName.getEffectiveName();
 		try
 		{
 		    IClob ret = getAdvancedStatement().getClob( effectiveParamName );
 
 			sm_logger.exiting( sm_className, methodName, ret );
 			return ret;
 		}
 		catch( OdaException ex )
 		{
 		    logAndThrowGetClobParamException( methodName, effectiveParamName, ex );
 		}
 		catch( UnsupportedOperationException ex )
 		{
 		    logAndThrowGetClobParamException( methodName, effectiveParamName, ex );
 		}
         return null;
 	}
 	
 	private void logAndThrowGetClobParamException( String methodName, 
 	        Object parameterId, Exception ex ) throws DataException
 	{
         final String errorCode = ResourceConstants.CANNOT_GET_CLOB_FROM_PARAMETER;
         handleException( ex, errorCode, methodName, parameterId );
 	}
     
     private Boolean doGetBoolean( int paramIndex ) throws DataException
     {
         final String methodName = "doGetBoolean( int )"; //$NON-NLS-1$
         final String errorCode = ResourceConstants.CANNOT_GET_BOOLEAN_FROM_PARAMETER;
         sm_logger.entering( sm_className, methodName, paramIndex );
         
         try
         {
             boolean ret = getAdvancedStatement().getBoolean( paramIndex );
             
             Boolean retObj = wasNull() ? null : new Boolean( ret );
             sm_logger.exiting( sm_className, methodName, retObj );         
             return retObj;
         }
         catch( OdaException ex )
         {
             handleException( ex, errorCode, methodName, paramIndex );
         }
         catch( UnsupportedOperationException ex )
         {
             handleException( ex, errorCode, methodName, paramIndex );
         }
         return null;
     }
     
     private Boolean doGetBoolean( ParameterName paramName ) throws DataException
     {
         final String methodName = "doGetBoolean( ParameterName )"; //$NON-NLS-1$
         final String errorCode = ResourceConstants.CANNOT_GET_BOOLEAN_FROM_PARAMETER;
         sm_logger.entering( sm_className, methodName, paramName );
         
         String effectiveParamName = paramName.getEffectiveName();
         try
         {
             boolean ret = getAdvancedStatement().getBoolean( effectiveParamName );
             
             Boolean retObj = wasNull() ? null : new Boolean( ret );
             sm_logger.exiting( sm_className, methodName, retObj );            
             return retObj;
         }
         catch( OdaException ex )
         {
             handleException( ex, errorCode, methodName, effectiveParamName );
         }
         catch( UnsupportedOperationException ex )
         {
             handleException( ex, errorCode, methodName, effectiveParamName );
         }
         return null;
     }
 	
 	private boolean wasNull() throws DataException
 	{
 		final String methodName = "wasNull"; //$NON-NLS-1$
         final String errorCode = ResourceConstants.CANNOT_DETERMINE_WAS_NULL;
 		
 		try
 		{
 			return getAdvancedStatement().wasNull();
 		}
 		catch( OdaException ex )
 		{
             handleException( ex, errorCode, methodName, null );
 		}
 		catch( UnsupportedOperationException ex )
 		{
             handleException( ex, errorCode, methodName, null );
 		}
         return false;
 	}
 	
 	private int getOdaTypeFromParamHints( String paramName, 
 										  int paramIndex )
 	{
 		if( m_parameterHints == null )
 			return Types.CHAR;
 		
         // first find the parameter hint for the specified parameter
 		ListIterator iter = m_parameterHints.listIterator();
 		boolean useParamName = ( paramName != null );
 		while( iter.hasNext() )
 		{
 			ParameterHint paramHint = (ParameterHint) iter.next();
 			
 			if( ( useParamName && paramHint.getName().equals( paramName ) ) ||
 				( ! useParamName && paramHint.getPosition() == paramIndex ) )
 			{
                 // found parameter's corresponding design hint
                 return paramHint.getEffectiveOdaType( 
                                         m_connection.getDataSourceId(),
                                         m_dataSetType );
 			}
 		}
 		
 		// do not have a design hint for the specified parameter
 		return Types.CHAR;    // default to a String oda type
 	}
 
 	// Returns 0 if the parameter hint doesn't exist for the specified parameter 
 	// name or if the caller didn't specify a position for the specified parameter name
 	private int getIndexFromParamHints( String paramName )
 	{
 		if( m_parameterHints == null )
 			return 0;
 		
 		ListIterator iter = m_parameterHints.listIterator();
 		while( iter.hasNext() )
 		{
 			ParameterHint paramHint = 
 				(ParameterHint) iter.next();
 			
 			if( paramHint.getName().equals( paramName ) )
 				return paramHint.getPosition();
 		}
 		
 		return 0;	// no matching parameter hint to give us the position
 	}
 	
 	/**
 	 * Returns the driver-defined name defined in design hints for
 	 * the specified data set parameter's model name.
 	 * @param paramName
 	 * @return driver-defined parameter name; may be null
 	 */
     private String getNativeNameFromParamHints( String paramName )
     {
         if( m_parameterHints == null )
             return null;
         
         ListIterator iter = m_parameterHints.listIterator();
         while( iter.hasNext() )
         {
             ParameterHint paramHint = (ParameterHint) iter.next();
             
             if( paramHint.getName().equals( paramName ) )
                 return paramHint.getNativeName();
         }
         
         return null;   // no matching parameter hint to give us the native name
     }
 
 	/**
 	 * Clears the current input parameter values immediately.
 	 * @throws DataException	if data source error occurs.
 	 */
 	public void clearParameterValues() throws DataException
 	{
 		String methodName = "clearParameterValues"; //$NON-NLS-1$
 		sm_logger.entering( sm_className, methodName );
 		
 		try
 		{
 			getStatement().clearInParameters();
 		}
 		catch( OdaException ex )
 		{
 			sm_logger.logp( Level.SEVERE, sm_className, methodName,
 							"Cannot clear input parameters.", ex ); //$NON-NLS-1$
 			
 			throw new DataException( ResourceConstants.CANNOT_CLEAR_IN_PARAMETERS, ex );
 		}
 		catch( AbstractMethodError err )
 		{
 			sm_logger.logp( Level.WARNING, sm_className, methodName,
 							"clearInParameters method undefined.", err ); //$NON-NLS-1$
 			
 			handleUnsupportedClearInParameters();
 		}
 		catch( UnsupportedOperationException ex )
 		{
 			sm_logger.logp( Level.WARNING, sm_className, methodName,
 							"clearInParameters is not supported.", ex ); //$NON-NLS-1$
 			
 			handleUnsupportedClearInParameters();
 		}
 		
 		// after clearing the parameter values, the underlying 
 		// metadata may change, so we need to invalidate 
 		// our states that are used to maintain the current 
 		// ResultClass
 		resetCachedMetadata();
 		
 		// optimization to keep the invalidated cached ProjectedColumns, 
 		// rather than getting new ones to replace them all immediately 
 		// If needed, after clearParameterValues() is called, we will go get 
 		// a new set of runtime metadata and incorporate the custom column/
 		// colum hints/projections info from the invalidated ProjectedColumn
 		m_updateProjectedColumns = true;
 		
 		if( m_namedProjectedColumns != null )
 		{
 			Set keys = m_namedProjectedColumns.keySet();
 			if( m_updateNamedProjectedColumns == null )
 				m_updateNamedProjectedColumns = new HashSet( keys );
 			else
 				m_updateNamedProjectedColumns.addAll( keys );
 		}
 
 		sm_logger.exiting( sm_className, methodName );
 	}
 
 	// provide a work-around for older ODA drivers or ODA drivers that 
 	// don't support the clearInParameters call
 	// the workaround involves creating a new instance of the underlying 
 	// ODA statement and setting it back up to the state of the current 
 	// statement
 	private void handleUnsupportedClearInParameters() throws DataException
 	{
 		m_statement = m_connection.prepareOdaQuery( m_queryText, 
 		                                                m_dataSetType );
 
 		// getting the new statement back into the previous statement's
 		// state
 		if( m_properties != null )
 		{
 			ListIterator iter = m_properties.listIterator();
 			while( iter.hasNext() )
 			{
 				Property property = (Property) iter.next();
 				doSetProperty( property.getName(), property.getValue() );
 			}
 		}
 		
 		doSetMaxRows( m_maxRows );
 		
 		if( m_sortSpecs != null )
 		{
 			ListIterator iter = m_sortSpecs.listIterator();
 			while( iter.hasNext() )
 			{
 				SortSpec sortBy = (SortSpec) iter.next();
 				doSetSortSpec( sortBy );
 			}
 		}
 	}
 
 	private void updateProjectedColumns( ProjectedColumns newProjectedColumns,
 										 ProjectedColumns oldProjectedColumns )
 		throws DataException
 	{
 		ArrayList customColumns = oldProjectedColumns.getCustomColumns();
 		ArrayList columnHints = oldProjectedColumns.getColumnHints();
 		String[] projections = oldProjectedColumns.getProjections();
 		
 		if( customColumns != null )
 		{
 			ListIterator iter = customColumns.listIterator();
 			while( iter.hasNext() )
 			{
 				CustomColumn customColumn = (CustomColumn) iter.next();
 				newProjectedColumns.addCustomColumn( customColumn.getName(), 
 				                                     customColumn.getType() );
 			}
 		}
 		
 		if( columnHints != null )
 		{
 			ListIterator iter = columnHints.listIterator();
 			while( iter.hasNext() )
 			{
 				ColumnHint columnHint = (ColumnHint) iter.next();
 				newProjectedColumns.addHint( columnHint );
 			}
 		}
 		
 		newProjectedColumns.setProjectedNames( projections );
 	}
 	
 	/**
 	 * Returns the 1-based index of the specified input parameter.
 	 * @param paramName	the name of the parameter.
 	 * @return	the 1-based index of the input parameter.
 	 * @throws DataException	if data source error occurs.
 	 */
 	public int findInParameter( String paramName ) throws DataException
 	{
 		final String methodName = "findInParameter"; //$NON-NLS-1$
 		sm_logger.entering( sm_className, methodName, paramName );
 		
 		try
 		{
 			int ret = getStatement( ).findInParameter( paramName );
 
 			sm_logger.exiting( sm_className, methodName, ret );			
 			return ret;
 		}
 		catch( OdaException ex )
 		{
 			sm_logger.logp( Level.SEVERE, sm_className, methodName, 
 							"Cannot find input parameter by name.", ex ); //$NON-NLS-1$
 			
 			throw new DataException( ResourceConstants.CANNOT_FIND_IN_PARAMETER, ex, 
 			                         new Object[] { paramName } );
 		}
 		catch( UnsupportedOperationException ex )
 		{
             // this is common, and may be ignored by caller
 			sm_logger.logp( Level.INFO, sm_className, methodName, 
 							"Cannot find input parameter by name.", ex ); //$NON-NLS-1$
 			
 			throw new DataException( ResourceConstants.CANNOT_FIND_IN_PARAMETER, ex, 
 			                         new Object[] { paramName } );
 		}
 	}
 
 	/**
 	 * Sets the value of the specified input parameter.
 	 * @param paramIndex	the 1-based index of the parameter.
 	 * @param paramValue	the input parameter value.
 	 * @throws DataException	if data source error occurs.
 	 */
 	public void setParameterValue( int paramIndex, Object paramValue ) throws DataException
 	{
 		setParameterValue( null /* n/a paramName */, paramIndex, paramValue );
 	}
 
 	/**
 	 * Sets the value of the specified input parameter.
 	 * @param paramName		the name of the parameter.
 	 * @param paramValue	the input parameter value.
 	 * @throws DataException	if data source error occurs.
 	 */
 	public void setParameterValue( String paramName, Object paramValue ) throws DataException
 	{
 		setParameterValue( paramName, 0 /* n/a paramIndex */, paramValue );
 	}
 
 	private void setParameterValue( String paramName, int paramIndex, 
 									Object paramValue ) throws DataException
 	{
 		final String methodName = "setParameterValue( String, int, Object )"; //$NON-NLS-1$
 		
 		// delegate to ParameterName for the proper name to use when
 		// interacting with underlying oda runtime driver
 		ParameterName paramNameObj = null;
 		if( paramName != null )   // setting parameter by name
 		{
 		    paramNameObj = new ParameterName( paramName, this );
 		    
             // log if not able to find corresponding native name
 		    paramNameObj.logNullNativeName();
 		}
 		
 		try
 		{
 
             // the following calls the setters may fail due to a type 
 			// mismatch with the parameter value's type.  so we'll 
 			// catch all RuntimeException's and OdaException's because 
 			// those are the exceptions that the ODA driver may throw in 
 			// such cases.  If we catch one, then we'll try to use alternative 
 			// mappings based on the runtime parameter metadata or the 
 			// parameter hints to call another setter method
 
             if( paramValue == null )
             {
                 setNull( paramNameObj, paramIndex );
                 return;
             }
             
             if( paramValue instanceof Integer )
 			{
 				int i = ( (Integer) paramValue ).intValue( );
 				setInt( paramNameObj, paramIndex, i );
 				return;
 			}
 			
 			if( paramValue instanceof Double )
 			{
 				double d = ( (Double) paramValue ).doubleValue( );
 				setDouble( paramNameObj, paramIndex, d );
 				return;
 			}
 	
 			if( paramValue instanceof String )
 			{
 				String string = (String) paramValue;
 				setString( paramNameObj, paramIndex, string );
 				return;
 			}
 			
 			if( paramValue instanceof BigDecimal )
 			{
 				BigDecimal decimal = (BigDecimal) paramValue;
 				setBigDecimal( paramNameObj, paramIndex, decimal );
 				return;
 			}
 	
             // check for subclasses before its java.util.Date base class type
 			if( paramValue instanceof Time )
 			{
 				Time time = (Time) paramValue;
 				setTime( paramNameObj, paramIndex, time );
 				return;
 			}
 			
 			if( paramValue instanceof Timestamp )
 			{
 				Timestamp timestamp = (Timestamp) paramValue;
 				setTimestamp( paramNameObj, paramIndex, timestamp );
 				return;
 			}
             
 			if( paramValue instanceof java.sql.Date )
 			{
                 Date sqlDate = (Date) paramValue;
                 setDate( paramNameObj, paramIndex, sqlDate );
                 return;
 			}
 			
             // check for all other types of java.util.Date
             if( paramValue instanceof java.util.Date )
             {
                 /* java.util.Date is not a supported ODA data type;
                  * the best ODA data type alternative is a java.sql.Timestamp 
                  * that preserves the time portion of a java.util.Date.
                  * A java.sql.Date has by definition a date portion only 
                  */
                 java.util.Date date = (java.util.Date) paramValue;
                 Timestamp sqlDateTime = new Timestamp( date.getTime() );
 				setTimestamp( paramNameObj, paramIndex, sqlDateTime );
 				return;
             }
             
             if( paramValue instanceof Boolean )
             {
                 boolean val = ( (Boolean) paramValue ).booleanValue();
                 setBoolean( paramNameObj, paramIndex, val );
                 return;
             }
 		}
 		catch( RuntimeException ex )
 		{
 			retrySetParameterValue( paramNameObj, paramIndex, paramValue, ex );
 			return;
 		}
 		catch( DataException ex )
 		{
 			retrySetParameterValue( paramNameObj, paramIndex, paramValue, ex );
 			return;
 		}
 
 		sm_logger.logp( Level.SEVERE, sm_className, methodName,
 						"Unsupported parameter value type." ); //$NON-NLS-1$
 		
 		throw new DataException( ResourceConstants.UNSUPPORTED_PARAMETER_VALUE_TYPE, 
                                  new Object[] { paramValue.getClass() } );
 	}
 
 	// retry setting the parameter value by using an alternate setter method 
 	// using the runtime parameter metadata. Or if the runtime parameter metadata 
 	// is not available, then use the input parameter hints, if available.  
 	// It will default to calling setString() if we can't get the info from 
 	// the runtime parameter metadata or the parameter hints.
 	private void retrySetParameterValue( ParameterName paramName, int paramIndex, 
 										 Object paramValue, 
 										 Exception lastException ) throws DataException
 	{
 		int parameterType = Types.NULL;
 		
 		try
 		{
 			// try to get the effective parameter type
 			parameterType = ( paramName == null ) ?
 							getParameterType( paramIndex ) :
 							getParameterType( paramName, false );
 		}
 		catch( Exception ex )
 		{
 			// data source can't get the type, try to get it from the hints
 		}
 		
 		// if not able to get the effective parameter metadata for any reason,  
 		// try to get the type directly from the parameter design hints
 		if( parameterType == Types.NULL )
 		{
 		    String paramModelName = ( paramName != null ) ? paramName.getRomName() : null;
 			parameterType = getOdaTypeFromParamHints( paramModelName, paramIndex );
 		}
 				
 		// the following conditions of runtime parameter metadata or hint 
 		// would have led us to call the same set<type> method again; 
 		// thus the last exception that got thrown by underlying ODA driver
 		// could be info regarding problems with the data, so throw that
 		if( ( parameterType == Types.INTEGER && paramValue instanceof Integer ) ||
 			( parameterType == Types.DOUBLE && paramValue instanceof Double ) ||
 			( parameterType == Types.CHAR && paramValue instanceof String ) ||
 			( parameterType == Types.DECIMAL && paramValue instanceof BigDecimal ) ||
 			( parameterType == Types.TIME && paramValue instanceof Time ) ||
 			( parameterType == Types.TIMESTAMP && paramValue instanceof Timestamp ) ||
             ( parameterType == Types.DATE && paramValue instanceof java.util.Date ) ||
             ( parameterType == Types.BOOLEAN && paramValue instanceof Boolean ) )
 		{
 			throwSetParamValueLastException( lastException, "retrySetParameterValue" ); //$NON-NLS-1$
 		}
         
         if( paramValue == null )
         {
             retrySetNullParamValue( paramName, paramIndex, parameterType, lastException );
             return;
         }
 		
         if( paramValue instanceof Integer )
 		{
 			retrySetIntegerParamValue( paramName, paramIndex, (Integer) paramValue, 
 			                           parameterType );
 			return;
 		}
 		
         if( paramValue instanceof Double )
 		{
 			retrySetDoubleParamValue( paramName, paramIndex, (Double) paramValue, 
 			                          parameterType );
 			return;
 		}
 		
         if( paramValue instanceof String )
 		{	
 			retrySetStringParamValue( paramName, paramIndex, (String) paramValue, 
 			                          parameterType );
 			return;
 		}
 		
 		if( paramValue instanceof BigDecimal )
 		{
 			retrySetBigDecimalParamValue( paramName, paramIndex, (BigDecimal) paramValue, 
 			                           parameterType );
 			return;
 		}
 		
         // check for subclasses before its java.util.Date base class type
 		if( paramValue instanceof Time )
 		{
 			retrySetTimeParamValue( paramName, paramIndex, (Time) paramValue, 
 			                        parameterType );
 			return;
 		}
 		
 		if( paramValue instanceof Timestamp )
 		{
 			retrySetTimestampParamValue( paramName, paramIndex, (Timestamp) paramValue, 
 			                             parameterType );
 			return;
 		}
         
         // check for all other types of java.util.Date
         if( paramValue instanceof java.util.Date )
         {
             retrySetDateParamValue( paramName, paramIndex, (java.util.Date) paramValue, 
                                     parameterType );
             return;
         }
         
         if( paramValue instanceof Boolean )
         {
             retrySetBooleanParamValue( paramName, paramIndex, (Boolean) paramValue, 
                                        parameterType );
             return;
         }
 		
 		assert false;	// unsupported parameter value type was checked earlier
 	}
 
 	private void retrySetIntegerParamValue( ParameterName paramName, int paramIndex, 
 											Integer paramValue, int parameterType ) 
 		throws DataException
 	{
 		switch( parameterType )
 		{
 			case Types.DOUBLE:
 			{
 				double d = paramValue.doubleValue();
 				setDouble( paramName, paramIndex, d );
 				return;
 			}
 			
 			case Types.CHAR:
 			{
 				String s = paramValue.toString();
 				setString( paramName, paramIndex, s );
 				return;
 			}
 			
 			case Types.DECIMAL:
 			{
 				int i = paramValue.intValue();
 				BigDecimal bd = new BigDecimal( i );
 				setBigDecimal( paramName, paramIndex, bd );
 				return;
 			}
             
             case Types.BOOLEAN:
             {
                 boolean val = ( paramValue.intValue() != 0 );
                 setBoolean( paramName, paramIndex, val );
                 return;
             }
 			
 			default:
 				conversionError( paramName, paramIndex, paramValue, 
 				                 parameterType, null /* cause */ );
 				return;
 		}
 	}
 
 	private void retrySetDoubleParamValue( ParameterName paramName, int paramIndex, 
 										   Double paramValue, int parameterType ) 
 		throws DataException
 	{
 		switch( parameterType )
 		{
 			case Types.INTEGER:
 			{
 				int i = paramValue.intValue();
 				Double intValue = new Double( i );
 				// this could be due to loss of precision or the double is 
 				// outside the range of an integer
 				if( ! paramValue.equals( intValue ) )
 					conversionError( paramName, paramIndex, paramValue, 
 					                 parameterType, null /* cause */ );
 				
 				setInt( paramName, paramIndex, i );
 				return;
 			}
 			
 			case Types.CHAR:
 			{
 				String s = paramValue.toString();
 				setString( paramName, paramIndex, s );
 				return;
 			}
 			
 			case Types.DECIMAL:
 			{
 				double d = paramValue.doubleValue();
 				BigDecimal bd = new BigDecimal( d );
 				setBigDecimal( paramName, paramIndex, bd );
 				return;
 			}
             
             case Types.BOOLEAN:
             {
                 boolean val = ( paramValue.doubleValue() != 0 );
                 setBoolean( paramName, paramIndex, val );
                 return;
             }
 			
 			default:
 				conversionError( paramName, paramIndex, paramValue, 
 				                 parameterType, null /* cause */ );
 				return;
 		}
 	}
 
 	private void retrySetStringParamValue( ParameterName paramName, int paramIndex, 
 										   String paramValue, int parameterType ) 
 		throws DataException
 	{
 		switch( parameterType )
 		{
 			case Types.INTEGER:
 			{
 				try
 				{
 					int i = Integer.parseInt( paramValue );
 					setInt( paramName, paramIndex, i );
 					return;
 				}
 				catch( NumberFormatException ex )
 				{
 					conversionError( paramName, paramIndex, paramValue, 
 					                 parameterType, ex );
 					return;
 				}
 			}
 			
 			case Types.DOUBLE:
 			{
 				try
 				{
 					double d = Double.parseDouble( paramValue );
 					setDouble( paramName, paramIndex, d );
 					return;
 				}
 				catch( NumberFormatException ex )
 				{
 					conversionError( paramName, paramIndex, paramValue, 
 					                 parameterType, ex );
 					return;
 				}
 			}
 			
 			case Types.DECIMAL:
 			{
 				try
 				{
 					BigDecimal bd = new BigDecimal( paramValue );
 					setBigDecimal( paramName, paramIndex, bd );
 					return;
 				}
 				catch( NumberFormatException ex )
 				{
 					conversionError( paramName, paramIndex, paramValue, 
 					                 parameterType, ex );
 					return;
 				}
 			}
 			
 			case Types.DATE:
 			{
 				try
 				{
 					Date d = Date.valueOf( paramValue );
 					setDate( paramName, paramIndex, d );
 					return;
 				}
 				catch( IllegalArgumentException ex )
 				{
 					conversionError( paramName, paramIndex, paramValue, 
 					                 parameterType, ex );
 					return;
 				}
 			}
 			
 			case Types.TIME:
 			{
 				try
 				{
 					Time t = Time.valueOf( paramValue );
 					setTime( paramName, paramIndex, t );
 					return;
 				}
 				catch( IllegalArgumentException ex )
 				{
 					conversionError( paramName, paramIndex, paramValue, 
 					                 parameterType, ex );
 					return;
 				}
 			}
 
 			case Types.TIMESTAMP:
 			{
 				try
 				{
 					Timestamp ts = Timestamp.valueOf( paramValue );
 					setTimestamp( paramName, paramIndex, ts );
 					return;
 				}
 				catch( IllegalArgumentException ex )
 				{
 					conversionError( paramName, paramIndex, paramValue, 
 					                 parameterType, ex );
 					return;
 				}
 			}
             
             case Types.BOOLEAN:
             {
                 boolean val = Boolean.valueOf( paramValue ).booleanValue();
                 setBoolean( paramName, paramIndex, val );
                 return;
             }
 			
 			default:
 				conversionError( paramName, paramIndex, paramValue, 
 				                 parameterType, null /* cause */ );
 				return;
 		}
 	}
 
 	private void retrySetBigDecimalParamValue( ParameterName paramName, int paramIndex, 
                                             BigDecimal paramValue, int parameterType ) 
 		throws DataException
 	{
 		switch( parameterType )
 		{
 			case Types.INTEGER:
 			{
 				int i = paramValue.intValue();
 				BigDecimal intValue = new BigDecimal( i );
 				// this could occur if there is a loss in precision or 
 				// if the BigDecimal value is outside the range of an integer
 				if( ! paramValue.equals( intValue ) )
 					conversionError( paramName, paramIndex, paramValue, 
 					                 parameterType, null /* cause */ );
 				
 				setInt( paramName, paramIndex, i );
 				return;
 			}
 			
 			case Types.DOUBLE:
 			{
 				double d = paramValue.doubleValue();
 				BigDecimal doubleValue = new BigDecimal( d );
 				// this could occur if there is a loss in precision or 
 				// if the BigDecimal value is outside the range of a double
 				if( ! paramValue.equals( doubleValue ) )
 					conversionError( paramName, paramIndex, paramValue, 
 					                 parameterType, null /* cause */ );
 				
 				setDouble( paramName, paramIndex, d );
 				return;
 			}
 			
 			case Types.CHAR:
 			{
 				String s = paramValue.toString();
 				setString( paramName, paramIndex, s );
 				return;
 			}
             
             case Types.BOOLEAN:
             {
                 boolean val = ( paramValue.compareTo( BigDecimal.valueOf( 0 ) ) != 0 );
                 setBoolean( paramName, paramIndex, val );
                 return;
             }
 			
 			default:
 				conversionError( paramName, paramIndex, paramValue, 
 				                 parameterType, null /* cause */ );
 				return;
 		}
 	}
 
 	private void retrySetDateParamValue( ParameterName paramName, int paramIndex, 
                                         java.util.Date paramValue, int parameterType ) 
 		throws DataException
 	{
 		switch( parameterType )
 		{
 			case Types.CHAR:
 			{
 				// need to convert the java.util.Date to a java.sql.Date, 
 				// so that we can get the ISO format date string
 				Date sqlDate = new Date( paramValue.getTime() );
 				String s = sqlDate.toString();
 				setString( paramName, paramIndex, s );
 				return;
 			}
             
             case Types.TIME:
             {
                 // ignores the date portion
                 Time timeValue = new Time( paramValue.getTime() );
                 setTime( paramName, paramIndex, timeValue );
                 return;
             }
 			
 			case Types.TIMESTAMP:
 			{
 				Timestamp ts = new Timestamp( paramValue.getTime() );
 				setTimestamp( paramName, paramIndex, ts );
 				return;
 			}
 			
 			default:
 				conversionError( paramName, paramIndex, paramValue, 
 				                 parameterType, null /* cause */ );
 				return;
 		}
 	}
 
 	private void retrySetTimeParamValue( ParameterName paramName, int paramIndex, 
                                          Time paramValue, int parameterType ) 
 		throws DataException
 	{
 		switch( parameterType )
 		{
 			case Types.CHAR:
 			{
 				String s = paramValue.toString();
 				setString( paramName, paramIndex, s );
 				return;
 			}
             
             case Types.DATE:
             {
                 Date d = new Date( paramValue.getTime() );
                 setDate( paramName, paramIndex, d );
                 return;
             }
             
             case Types.TIMESTAMP:
             {
                 Timestamp ts = new Timestamp( paramValue.getTime() );
                 setTimestamp( paramName, paramIndex, ts );
                 return;
             }
 			
 			default:
 				conversionError( paramName, paramIndex, paramValue, 
 				                 parameterType, null /* cause */ );
 				return;
 		}
 	}
 
 	private void retrySetTimestampParamValue( ParameterName paramName, int paramIndex, 	
                                             Timestamp paramValue, int parameterType ) 
 		throws DataException
 	{
 		switch( parameterType )
 		{
 			case Types.CHAR:
 			{
 				String s = paramValue.toString();
 				setString( paramName, paramIndex, s );
 				return;
 			}
 			
 			case Types.DATE:
 			{
 				long time = paramValue.getTime();
 				Date d = new Date( time );
 				setDate( paramName, paramIndex, d );
 				return;
 			}
             
             case Types.TIME:
             {
                 // ignores the date portion
                 Time timeValue = new Time( paramValue.getTime() );
                 setTime( paramName, paramIndex, timeValue );
                 return;
             }
 			
 			default:
 				conversionError( paramName, paramIndex, paramValue, 
 				                 parameterType, null /* cause */ );
 				return;
 		}
 	}
 
     private void retrySetBooleanParamValue( ParameterName paramName, int paramIndex, 
                                             Boolean paramValue, int parameterType ) 
         throws DataException
     {
         switch( parameterType )
         {
             case Types.INTEGER:
             {
                 int i = paramValue.booleanValue() ? 1 : 0;
                 setInt( paramName, paramIndex, i );
                 return;
             }
             
             case Types.DOUBLE:
             {
                 double d = paramValue.booleanValue() ? 1 : 0;
                 setDouble( paramName, paramIndex, d );
                 return;
             }
             
             case Types.CHAR:
             {
                 String s = paramValue.toString();
                 setString( paramName, paramIndex, s );
                 return;
             }
             
             case Types.DECIMAL:
             {
                 int i = paramValue.booleanValue() ? 1 : 0;
                 BigDecimal bd = new BigDecimal( i );
                 setBigDecimal( paramName, paramIndex, bd );
                 return;
             }
             
             default:
                 conversionError( paramName, paramIndex, paramValue, 
                                  parameterType, null /* cause */ );
                 return;
         }
     }
 
     private void retrySetNullParamValue( ParameterName paramName, int paramIndex, 
                                          int parameterType,
                                          Exception lastException ) 
         throws DataException
     {
         switch( parameterType )
         {            
             case Types.CHAR:
             {
                 setString( paramName, paramIndex, null );
                 return;
             }
             
             case Types.DECIMAL:
             {
                 setBigDecimal( paramName, paramIndex, null );
                 return;
             }
             case Types.DATE:
             {
                 setDate( paramName, paramIndex, null );
                 return;
             }
             
             case Types.TIME:
             {
                 setTime( paramName, paramIndex, null );
                 return;
             }
 
             case Types.TIMESTAMP:
             {
                 setTimestamp( paramName, paramIndex, null );
                 return;
             }
             
             default:
                 // metadata indicates primitive data types or types not supported for input parameter, 
                 // cannot retry with a different ODA API setter to assign 
                 // a null input parameter value
                 
                 sm_logger.logp( Level.SEVERE, sm_className, "retrySetNullParamValue",  //$NON-NLS-1$
                                 "Input parameter value is null; not able to retry, throws exception from underlying ODA driver." ); //$NON-NLS-1$
                 
                 // not able to retry, throw last exception thrown by 
                 // the underlying ODA driver
                 throwSetParamValueLastException( lastException, "retrySetNullParamValue" ); //$NON-NLS-1$
         }
     }
 
 	private void conversionError( ParameterName paramName, int paramIndex, 
 								  Object paramValue, int odaType, 
 								  Exception cause ) throws DataException
 	{
 		final String methodName = "conversionError"; //$NON-NLS-1$
         
 		Object paramValueArg =  ( paramValue == null ) ? (Object) "" : paramValue; //$NON-NLS-1$
         Object paramClassArg =  ( paramValue == null ) ? (Object) "null" : paramValue.getClass(); //$NON-NLS-1$
         final String errorCode = ResourceConstants.CANNOT_CONVERT_INDEXED_PARAMETER_VALUE;
 		DataException exception = null;
 		if( paramName == null )
 			exception = new DataException( errorCode, 
 				                           new Object[] { paramValueArg, new Integer( paramIndex ), 
                                                     paramClassArg, new Integer( odaType ) } );
 		else
 			exception = new DataException( errorCode, 
                                            new Object[] { paramValueArg, paramName, 
                                                     paramClassArg, new Integer( odaType ) } );
 		
 		if( cause != null )
 			exception.initCause( cause );
 		
 		sm_logger.logp( Level.SEVERE, sm_className, methodName, 
 						"Data type conversion error.", exception );		 //$NON-NLS-1$
 		throw exception;
 	}
 
     /**
      * Throws the specified exception last thrown by an underlying ODA driver
      * during a call to set input parameter value.
      */
     private void throwSetParamValueLastException( Exception lastException,
             final String methodName ) 
         throws RuntimeException, DataException, IllegalStateException
     {
         assert( lastException != null );
         
         String logContextMsg = "Cannot set input parameter."; //$NON-NLS-1$
         if( lastException instanceof RuntimeException )
         {
             sm_logger.logp( Level.SEVERE, sm_className, methodName, 
                             logContextMsg, lastException );
             
             throw (RuntimeException) lastException;
         }
         else if( lastException instanceof DataException )
         {
             sm_logger.logp( Level.SEVERE, sm_className, methodName, 
                             logContextMsg, lastException ); //$NON-NLS-1$
             
             throw (DataException) lastException;
         }
         else
         {
             String localizedMessage = 
                 DataResourceHandle.getInstance().getMessage( ResourceConstants.UNKNOWN_EXCEPTION_THROWN );
             IllegalStateException ex = 
                 new IllegalStateException( localizedMessage );
             ex.initCause( lastException );
             
             sm_logger.logp( Level.SEVERE, sm_className, methodName, 
                             logContextMsg, lastException ); //$NON-NLS-1$
             
             throw ex;
         }
     }
 
 	private void setInt( ParameterName paramName, int paramIndex, int i ) throws DataException
 	{
 		if( paramName == null )
 			doSetInt( paramIndex, i );
 		else
 			setInt( paramName, i );
 	}
 
 	private void setInt( ParameterName paramName, int i ) throws DataException
 	{
 		final String methodName = "setInt( ParameterName, int )"; //$NON-NLS-1$
 		
 		if( supportsNamedParameter() )
 		{
 			doSetInt( paramName, i );
 			return;
 		}
 		
 		if( ! setIntUsingHints( paramName, i ) )
 		{
             final String errorCode = ResourceConstants.CANNOT_SET_INT_PARAMETER;
             Object[] msgArgs = new Object[] { new Integer( i ), paramName };                    
             handleError( errorCode, msgArgs, methodName );
 		}
 	}
 	
 	private boolean setIntUsingHints( ParameterName paramName, int i ) throws DataException
 	{
 		int paramIndex = getIndexFromParamHints( paramName.getRomName() );
 		if( paramIndex <= 0 )
 			return false;
 		
 		doSetInt( paramIndex, i );
 		return true;
 	}
 
 	private void setDouble( ParameterName paramName, int paramIndex, double d ) throws DataException
 	{
 		if( paramName == null )
 			doSetDouble( paramIndex, d );
 		else
 			setDouble( paramName, d );
 	}
 	
 	private void setDouble( ParameterName paramName, double d ) throws DataException
 	{
 		final String methodName = "setDouble( ParameterName, double )"; //$NON-NLS-1$
 		
 		if( supportsNamedParameter() )
 		{
 			doSetDouble( paramName, d );
 			return;
 		}
 		
 		if( ! setDoubleUsingHints( paramName, d ) )
 		{
             final String errorCode = ResourceConstants.CANNOT_SET_DOUBLE_PARAMETER;
             Object[] msgArgs = new Object[] { new Double( d ), paramName };                    
             handleError( errorCode, msgArgs, methodName );
 		}
 	}
 	
 	private boolean setDoubleUsingHints( ParameterName paramName, double d ) throws DataException
 	{
 		int paramIndex = getIndexFromParamHints( paramName.getRomName() );
 		if( paramIndex <= 0 )
 			return false;
 		
 		doSetDouble( paramIndex, d );
 		return true;
 	}
 
 	private void setString( ParameterName paramName, int paramIndex, String stringValue ) throws DataException
 	{
 		if( paramName == null )
 			doSetString( paramIndex, stringValue );
 		else
 			setString( paramName, stringValue );
 	}
 
 	private void setString( ParameterName paramName, String stringValue ) throws DataException
 	{
 		final String methodName = "setString( ParameterName, String )"; //$NON-NLS-1$
 		
 		if( supportsNamedParameter() )
 		{
 			doSetString( paramName, stringValue );
 			return;
 		}
 		
 		if( ! setStringUsingHints( paramName, stringValue ) )
 		{
             final String errorCode = ResourceConstants.CANNOT_SET_STRING_PARAMETER;
             Object[] msgArgs = new Object[] { stringValue, paramName };                    
             handleError( errorCode, msgArgs, methodName );
 		}
 	}
 
 	private boolean setStringUsingHints( ParameterName paramName, String stringValue ) throws DataException
 	{
 		int paramIndex = getIndexFromParamHints( paramName.getRomName() );
 		if( paramIndex <= 0 )
 			return false;
 		
 		doSetString( paramIndex, stringValue );
 		return true;
 	}
 	
 	private void setBigDecimal( ParameterName paramName, int paramIndex, BigDecimal decimal ) throws DataException
 	{
 		if( paramName == null )
 			doSetBigDecimal( paramIndex, decimal );
 		else
 			setBigDecimal( paramName, decimal );
 	}
 
 	private void setBigDecimal( ParameterName paramName, BigDecimal decimal ) throws DataException
 	{
 		final String methodName = "setBigDecimal( ParameterName, BigDecimal )"; //$NON-NLS-1$
 		
 		if( supportsNamedParameter() )
 		{
 			doSetBigDecimal( paramName, decimal );
 			return;
 		}
 		
 		if( ! setBigDecimalUsingHints( paramName, decimal ) )
 		{
             final String errorCode = ResourceConstants.CANNOT_SET_BIGDECIMAL_PARAMETER;
             Object[] msgArgs = new Object[] { decimal, paramName };                    
             handleError( errorCode, msgArgs, methodName );
 		}
 	}	
 
 	private boolean setBigDecimalUsingHints( ParameterName paramName, BigDecimal decimal ) throws DataException
 	{
 		int paramIndex = getIndexFromParamHints( paramName.getRomName() );
 		if( paramIndex <= 0 )
 			return false;
 		
 		doSetBigDecimal( paramIndex, decimal );
 		return true;
 	}
 	
 	private void setDate( ParameterName paramName, int paramIndex, Date date ) throws DataException
 	{
 		if( paramName == null )
 			doSetDate( paramIndex, date );
 		else 
 			setDate( paramName, date );
 	}
 
 	private void setDate( ParameterName paramName, Date date ) throws DataException
 	{
 		final String methodName = "setDate( ParameterName, Date )"; //$NON-NLS-1$
 		
 		if( supportsNamedParameter() )
 		{
 			doSetDate( paramName, date );
 			return;
 		}
 		
 		if( ! setDateUsingHints( paramName, date ) )
 		{
             final String errorCode = ResourceConstants.CANNOT_SET_DATE_PARAMETER;
             Object[] msgArgs = new Object[] { date, paramName };                    
             handleError( errorCode, msgArgs, methodName );
 		}
 	}
 
 	private boolean setDateUsingHints( ParameterName paramName, Date date ) throws DataException
 	{
 		int paramIndex = getIndexFromParamHints( paramName.getRomName() );
 		if( paramIndex <= 0 )
 			return false;
 		
 		doSetDate( paramIndex, date );
 		return true;
 	}
 	
 	private void setTime( ParameterName paramName, int paramIndex, Time time ) throws DataException
 	{
 		if( paramName == null )
 			doSetTime( paramIndex, time );
 		else
 			setTime( paramName, time );
 	}
 
 	private void setTime( ParameterName paramName, Time time ) throws DataException
 	{
 		final String methodName = "setTime( ParameterName, Time )"; //$NON-NLS-1$
 		
 		if( supportsNamedParameter() )
 		{
 			doSetTime( paramName, time );
 			return;
 		}
 		
 		if( ! setTimeUsingHints( paramName, time ) )
 		{
             final String errorCode = ResourceConstants.CANNOT_SET_TIME_PARAMETER;
             Object[] msgArgs = new Object[] { time, paramName };                    
             handleError( errorCode, msgArgs, methodName );
 		}
 	}
 
 	private boolean setTimeUsingHints( ParameterName paramName, Time time ) throws DataException
 	{
 		int paramIndex = getIndexFromParamHints( paramName.getRomName() );
 		if( paramIndex <= 0 )
 			return false;
 		
 		doSetTime( paramIndex, time );
 		return true;
 	}
 
 	private void setTimestamp( ParameterName paramName, int paramIndex, Timestamp timestamp ) 
 		throws DataException
 	{
 		if( paramName == null )
 			doSetTimestamp( paramIndex, timestamp );
 		else
 			setTimestamp( paramName, timestamp );
 	}
 
 	private void setTimestamp( ParameterName paramName, Timestamp timestamp ) throws DataException
 	{
 		final String methodName = "setTimestamp( ParameterName, Timestamp )"; //$NON-NLS-1$
 		
 		if( supportsNamedParameter() )
 		{
 			doSetTimestamp( paramName, timestamp );
 			return;
 		}
 		
 		if( ! setTimestampUsingHints( paramName, timestamp ) )
 		{
             final String errorCode = ResourceConstants.CANNOT_SET_TIMESTAMP_PARAMETER;
             Object[] msgArgs = new Object[] { timestamp, paramName };                    
             handleError( errorCode, msgArgs, methodName );
 		}
 	}
 
 	private boolean setTimestampUsingHints( ParameterName paramName, Timestamp timestamp ) 
 		throws DataException
 	{
 		int paramIndex = getIndexFromParamHints( paramName.getRomName() );
 		if( paramIndex <= 0 )
 			return false;
 		
 		doSetTimestamp( paramIndex, timestamp );
 		return true;
 	}
 
     private void setBoolean( ParameterName paramName, int paramIndex, boolean val ) throws DataException
     {
         if( paramName == null )
             doSetBoolean( paramIndex, val );
         else
             setBoolean( paramName, val );
     }
 
     private void setBoolean( ParameterName paramName, boolean val ) throws DataException
     {
         final String methodName = "setBoolean( ParameterName, boolean )"; //$NON-NLS-1$
         
         if( supportsNamedParameter() )
         {
             doSetBoolean( paramName, val );
             return;
         }
         
         if( ! setBooleanUsingHints( paramName, val ) )
         {
             final String errorCode = ResourceConstants.CANNOT_SET_BOOLEAN_PARAMETER;
             Object[] msgArgs = new Object[] { new Boolean( val ), paramName };                    
             handleError( errorCode, msgArgs, methodName );
         }
     }
     
     private boolean setBooleanUsingHints( ParameterName paramName, boolean val ) throws DataException
     {
         int paramIndex = getIndexFromParamHints( paramName.getRomName() );
         if( paramIndex <= 0 )
             return false;
         
         doSetBoolean( paramIndex, val );
         return true;
     }
 
     private void setNull( ParameterName paramName, int paramIndex ) throws DataException
     {
         if( paramName == null )
             doSetNull( paramIndex );
         else
             setNull( paramName );
     }
 
     private void setNull( ParameterName paramName ) throws DataException
     {
         final String methodName = "setNull( ParameterName )"; //$NON-NLS-1$
         
         if( supportsNamedParameter() )
         {
             doSetNull( paramName );
             return;
         }
         
         if( ! setNullUsingHints( paramName ) )
         {
             final String errorCode = ResourceConstants.CANNOT_SET_NULL_PARAMETER;
             Object[] msgArgs = new Object[] { paramName };                    
             handleError( errorCode, msgArgs, methodName );
         }
     }
     
     private boolean setNullUsingHints( ParameterName paramName ) throws DataException
     {
         int paramIndex = getIndexFromParamHints( paramName.getRomName() );
         if( paramIndex <= 0 )
             return false;
         
         doSetNull( paramIndex );
         return true;
     }
 
 	private void doSetInt( int paramIndex, int i ) throws DataException
 	{
 		final String methodName = "doSetInt( int, int )"; //$NON-NLS-1$
         final String errorCode = ResourceConstants.CANNOT_SET_INT_PARAMETER;
 		
 		try
 		{
 			getStatement().setInt( paramIndex, i );
 		}
 		catch( OdaException ex )
 		{
             Object[] msgArgs = new Object[] { new Integer( i ), new Integer( paramIndex ) };                    
             handleException( ex, errorCode, msgArgs, methodName );
 		}
 		catch( UnsupportedOperationException ex )
 		{
             Object[] msgArgs = new Object[] { new Integer( i ), new Integer( paramIndex ) };                    
             handleException( ex, errorCode, msgArgs, methodName );
 		}
 	}
 	
 	private void doSetInt( ParameterName paramName, int i ) throws DataException
 	{
 		final String methodName = "doSetInt( ParameterName, int )"; //$NON-NLS-1$
         final String errorCode = ResourceConstants.CANNOT_SET_INT_PARAMETER;
 		
         String effectiveParamName = paramName.getEffectiveName();
 		try
 		{
 			getStatement().setInt( effectiveParamName, i );
 		}
 		catch( OdaException ex )
 		{
             Object[] msgArgs = new Object[] { new Integer( i ), effectiveParamName };                    
             handleException( ex, errorCode, msgArgs, methodName );
 		}
 		catch( UnsupportedOperationException ex )
 		{
 			// first try to set value by position if the parameter hints provide name-to-position mapping,  
 			// otherwise we need to wrap the UnsupportedOperationException up and throw it
 			if( ! setIntUsingHints( paramName, i ) )
 			{
                 Object[] msgArgs = new Object[] { new Integer( i ), effectiveParamName };                    
                 handleException( ex, errorCode, msgArgs, methodName );
 			}
 		}
 	}
 
 	private void doSetDouble( int paramIndex, double d ) throws DataException
 	{
 		final String methodName = "doSetDouble( int, double )"; //$NON-NLS-1$
         final String errorCode = ResourceConstants.CANNOT_SET_DOUBLE_PARAMETER;
 		
 		try
 		{
 			getStatement().setDouble( paramIndex, d );
 		}
 		catch( OdaException ex )
 		{
             Object[] msgArgs = new Object[] { new Double( d ), new Integer( paramIndex ) };                    
             handleException( ex, errorCode, msgArgs, methodName );
 		}
 		catch( UnsupportedOperationException ex )
 		{
             Object[] msgArgs = new Object[] { new Double( d ), new Integer( paramIndex ) };                    
             handleException( ex, errorCode, msgArgs, methodName );
 		}
 	}
 	
 	
 	private void doSetDouble( ParameterName paramName, double d ) throws DataException
 	{
 		final String methodName = "doSetDouble( ParameterName, double )"; //$NON-NLS-1$
         final String errorCode = ResourceConstants.CANNOT_SET_DOUBLE_PARAMETER;
 		
         String effectiveParamName = paramName.getEffectiveName();
 		try
 		{
 			getStatement().setDouble( effectiveParamName, d );
 		}
 		catch( OdaException ex )
 		{
             Object[] msgArgs = new Object[] { new Double( d ), effectiveParamName };                    
             handleException( ex, errorCode, msgArgs, methodName );
 		}
 		catch( UnsupportedOperationException ex )
 		{
 			// first try to set value by position if the parameter hints provide name-to-position mapping,  
 			// otherwise we need to wrap the UnsupportedOperationException up and throw it
 			if( ! setDoubleUsingHints( paramName, d ) )
 			{
                 Object[] msgArgs = new Object[] { new Double( d ), effectiveParamName };                    
                 handleException( ex, errorCode, msgArgs, methodName );
 			}
 		}
 	}
 	
 	private void doSetString( int paramIndex, String stringValue ) throws DataException
 	{
 		final String methodName = "doSetString( int, String )"; //$NON-NLS-1$
         final String errorCode = ResourceConstants.CANNOT_SET_STRING_PARAMETER;
 		
 		try
 		{
 			getStatement().setString( paramIndex, stringValue );
 		}
 		catch( OdaException ex )
 		{
             Object[] msgArgs = new Object[] { stringValue, new Integer( paramIndex ) };                    
             handleException( ex, errorCode, msgArgs, methodName );
 		}
 		catch( UnsupportedOperationException ex )
 		{
             Object[] msgArgs = new Object[] { stringValue, new Integer( paramIndex ) };                    
             handleException( ex, errorCode, msgArgs, methodName );
 		}
 	}
 	
 	private void doSetString( ParameterName paramName, String stringValue ) throws DataException
 	{
 		final String methodName = "doSetString( ParameterName, String )"; //$NON-NLS-1$
         final String errorCode = ResourceConstants.CANNOT_SET_STRING_PARAMETER;
 		
         String effectiveParamName = paramName.getEffectiveName();
 		try
 		{
 			getStatement().setString( effectiveParamName, stringValue );
 		}
 		catch( OdaException ex )
 		{
             Object[] msgArgs = new Object[] { stringValue, effectiveParamName };                    
             handleException( ex, errorCode, msgArgs, methodName );
 		}
 		catch( UnsupportedOperationException ex )
 		{
 			// first try to set value by position if the parameter hints provide name-to-position mapping,  
 			// otherwise we need to wrap the UnsupportedOperationException up and throw it
 			if( ! setStringUsingHints( paramName, stringValue ) )
 			{
                 Object[] msgArgs = new Object[] { stringValue, effectiveParamName };                    
                 handleException( ex, errorCode, msgArgs, methodName );
 			}
 		}
 	}
 	
 	private void doSetBigDecimal( int paramIndex, BigDecimal decimal ) throws DataException
 	{
 		final String methodName = "doSetBigDecimal( int, BigDecimal )"; //$NON-NLS-1$
         final String errorCode = ResourceConstants.CANNOT_SET_BIGDECIMAL_PARAMETER;
 		
 		try
 		{
 			getStatement().setBigDecimal( paramIndex, decimal );
 		}
 		catch( OdaException ex )
 		{
             Object[] msgArgs = new Object[] { decimal, new Integer( paramIndex ) };                    
             handleException( ex, errorCode, msgArgs, methodName );
 		}
 		catch( UnsupportedOperationException ex )
 		{
             Object[] msgArgs = new Object[] { decimal, new Integer( paramIndex ) };                    
             handleException( ex, errorCode, msgArgs, methodName );
 		}
 	}
 	
 	private void doSetBigDecimal( ParameterName paramName, BigDecimal decimal ) throws DataException
 	{
 		final String methodName = "doSetBigDecimal( ParameterName, BigDecimal )"; //$NON-NLS-1$
         final String errorCode = ResourceConstants.CANNOT_SET_BIGDECIMAL_PARAMETER;
 		
         String effectiveParamName = paramName.getEffectiveName();
 		try
 		{
 			getStatement().setBigDecimal( effectiveParamName, decimal );
 		}
 		catch( OdaException ex )
 		{
             Object[] msgArgs = new Object[] { decimal, effectiveParamName };                    
             handleException( ex, errorCode, msgArgs, methodName );
 		}
 		catch( UnsupportedOperationException ex )
 		{
 			// first try to set value by position if the parameter hints provide name-to-position mapping,  
 			// otherwise we need to wrap the UnsupportedOperationException up and throw it
 			if( ! setBigDecimalUsingHints( paramName, decimal ) )
 			{
                 Object[] msgArgs = new Object[] { decimal, effectiveParamName };                    
                 handleException( ex, errorCode, msgArgs, methodName );
 			}
 		}
 	}
 	
 	private void doSetDate( int paramIndex, Date date ) throws DataException
 	{
 		final String methodName = "doSetDate( int, Date )"; //$NON-NLS-1$
         final String errorCode = ResourceConstants.CANNOT_SET_DATE_PARAMETER;
 		
 		try
 		{
 			getStatement().setDate( paramIndex, date );
 		}
 		catch( OdaException ex )
 		{
             Object[] msgArgs = new Object[] { date, new Integer( paramIndex ) };                    
             handleException( ex, errorCode, msgArgs, methodName );
 		}
 		catch( UnsupportedOperationException ex )
 		{
             Object[] msgArgs = new Object[] { date, new Integer( paramIndex ) };                    
             handleException( ex, errorCode, msgArgs, methodName );
 		}
 	}
 	
 	private void doSetDate( ParameterName paramName, Date date ) throws DataException
 	{
 		final String methodName = "doSetDate( ParameterName, Date )"; //$NON-NLS-1$
         final String errorCode = ResourceConstants.CANNOT_SET_DATE_PARAMETER;
 		
         String effectiveParamName = paramName.getEffectiveName();
 		try
 		{
 			getStatement().setDate( effectiveParamName, date );
 		}
 		catch( OdaException ex )
 		{
             Object[] msgArgs = new Object[] { date, effectiveParamName };                    
             handleException( ex, errorCode, msgArgs, methodName );
 		}
 		catch( UnsupportedOperationException ex )
 		{
 			// first try to set value by position if the parameter hints provide name-to-position mapping,  
 			// otherwise we need to wrap the UnsupportedOperationException up and throw it
 			if( ! setDateUsingHints( paramName, date ) )
 			{
                 Object[] msgArgs = new Object[] { date, effectiveParamName };                    
                 handleException( ex, errorCode, msgArgs, methodName );
 			}
 		}
 	}
 	
 	private void doSetTime( int paramIndex, Time time ) throws DataException
 	{
 		final String methodName = "doSetTime( int, Time )"; //$NON-NLS-1$
         final String errorCode = ResourceConstants.CANNOT_SET_TIME_PARAMETER;
 		
 		try
 		{
 			getStatement().setTime( paramIndex, time );
 		}
 		catch( OdaException ex )
 		{
             Object[] msgArgs = new Object[] { time, new Integer( paramIndex ) };                    
             handleException( ex, errorCode, msgArgs, methodName );
 		}
 		catch( UnsupportedOperationException ex )
 		{
             Object[] msgArgs = new Object[] { time, new Integer( paramIndex ) };                    
             handleException( ex, errorCode, msgArgs, methodName );
 		}
 	}
 	
 	private void doSetTime( ParameterName paramName, Time time ) throws DataException
 	{
 		final String methodName = "doSetTime( ParameterName, Time )"; //$NON-NLS-1$
         final String errorCode = ResourceConstants.CANNOT_SET_TIME_PARAMETER;
 		
         String effectiveParamName = paramName.getEffectiveName();
 		try
 		{
 			getStatement().setTime( effectiveParamName, time );
 		}
 		catch( OdaException ex )
 		{
             Object[] msgArgs = new Object[] { time, effectiveParamName };                    
             handleException( ex, errorCode, msgArgs, methodName );
 		}
 		catch( UnsupportedOperationException ex )
 		{
 			// first try to set value by position if the parameter hints provide name-to-position mapping,  
 			// otherwise we need to wrap the UnsupportedOperationException up and throw it
 			if( ! setTimeUsingHints( paramName, time ) )
 			{
                 Object[] msgArgs = new Object[] { time, effectiveParamName };                    
                 handleException( ex, errorCode, msgArgs, methodName );
 			}
 		}
 	}
 	
 	private void doSetTimestamp( int paramIndex, Timestamp timestamp ) throws DataException
 	{
 		final String methodName = "doSetTimestamp( int, Timestamp )"; //$NON-NLS-1$
         final String errorCode = ResourceConstants.CANNOT_SET_TIMESTAMP_PARAMETER;
 		
 		try
 		{
 			getStatement().setTimestamp( paramIndex, timestamp );
 		}
 		catch( OdaException ex )
 		{
             Object[] msgArgs = new Object[] { timestamp, new Integer( paramIndex ) };                    
             handleException( ex, errorCode, msgArgs, methodName );
 		}
 		catch( UnsupportedOperationException ex )
 		{
             Object[] msgArgs = new Object[] { timestamp, new Integer( paramIndex ) };                    
             handleException( ex, errorCode, msgArgs, methodName );
 		}
 	}
 	
 	private void doSetTimestamp( ParameterName paramName, Timestamp timestamp ) throws DataException
 	{
 		final String methodName = "doSetTimestamp( ParameterName, Timestamp )"; //$NON-NLS-1$
         final String errorCode = ResourceConstants.CANNOT_SET_TIMESTAMP_PARAMETER;
 		
         String effectiveParamName = paramName.getEffectiveName();
 		try
 		{
 			getStatement().setTimestamp( effectiveParamName, timestamp );
 		}
 		catch( OdaException ex )
 		{
             Object[] msgArgs = new Object[] { timestamp, effectiveParamName };                    
             handleException( ex, errorCode, msgArgs, methodName );
 		}
 		catch( UnsupportedOperationException ex )
 		{
 			// first try to set value by position if the parameter hints provide name-to-position mapping,  
 			// otherwise we need to wrap the UnsupportedOperationException up and throw it
 			if( ! setTimestampUsingHints( paramName, timestamp ) )
 			{
                 Object[] msgArgs = new Object[] { timestamp, effectiveParamName };                    
                 handleException( ex, errorCode, msgArgs, methodName );
 			}
 		}
 	}
 
     private void doSetBoolean( int paramIndex, boolean val ) throws DataException
     {
         final String methodName = "doSetBoolean( int, boolean )"; //$NON-NLS-1$
         final String errorCode = ResourceConstants.CANNOT_SET_BOOLEAN_PARAMETER;
         
         try
         {
             getStatement().setBoolean( paramIndex, val );
         }
         catch( OdaException ex )
         {
             Object[] msgArgs = new Object[] { new Boolean( val ), new Integer( paramIndex ) };                    
             handleException( ex, errorCode, msgArgs, methodName );
          }
         catch( UnsupportedOperationException ex )
         {
             Object[] msgArgs = new Object[] { new Boolean( val ), new Integer( paramIndex ) };                    
             handleException( ex, errorCode, msgArgs, methodName );
         }
     }
     
     private void doSetBoolean( ParameterName paramName, boolean val ) throws DataException
     {
         final String methodName = "doSetBoolean( ParameterName, boolean )"; //$NON-NLS-1$
         final String errorCode = ResourceConstants.CANNOT_SET_BOOLEAN_PARAMETER;
         
         String effectiveParamName = paramName.getEffectiveName();
         try
         {
             getStatement().setBoolean( effectiveParamName, val );
         }
         catch( OdaException ex )
         {
             Object[] msgArgs = new Object[] { new Boolean( val ), effectiveParamName };                    
             handleException( ex, errorCode, msgArgs, methodName );
         }
         catch( UnsupportedOperationException ex )
         {
             // first try to set value by position if the parameter hints provide name-to-position mapping,  
             // otherwise we need to wrap the UnsupportedOperationException up and throw it
             if( ! setBooleanUsingHints( paramName, val ) )
             {
                 Object[] msgArgs = new Object[] { new Boolean( val ), effectiveParamName };                    
                 handleException( ex, errorCode, msgArgs, methodName );
             }
         }
     }
 
     private void doSetNull( int paramIndex ) throws DataException
     {
         final String methodName = "doSetNull( int )"; //$NON-NLS-1$
         final String errorCode = ResourceConstants.CANNOT_SET_NULL_PARAMETER;
         
         try
         {
             getStatement().setNull( paramIndex );
         }
         catch( OdaException ex )
         {
             Object[] msgArgs = new Object[] { new Integer( paramIndex ) };                    
             handleException( ex, errorCode, msgArgs, methodName );
         }
         catch( UnsupportedOperationException ex )
         {
             Object[] msgArgs = new Object[] { new Integer( paramIndex ) };                    
             handleException( ex, errorCode, msgArgs, methodName );
         }
     }
     
     private void doSetNull( ParameterName paramName ) throws DataException
     {
         final String methodName = "doSetNull( ParameterName )"; //$NON-NLS-1$
         final String errorCode = ResourceConstants.CANNOT_SET_NULL_PARAMETER;
         
         String effectiveParamName = paramName.getEffectiveName();
         try
         {
             getStatement().setNull( effectiveParamName );
         }
         catch( OdaException ex )
         {
             Object[] msgArgs = new Object[] { effectiveParamName };                    
             handleException( ex, errorCode, msgArgs, methodName );
         }
         catch( UnsupportedOperationException ex )
         {
             // first try to set value by position if the parameter hints provide name-to-position mapping,  
             // otherwise we need to wrap the UnsupportedOperationException up and throw it
             if( ! setNullUsingHints( paramName ) )
             {
                 Object[] msgArgs = new Object[] { effectiveParamName };                    
                 handleException( ex, errorCode, msgArgs, methodName );
             }
         }
     }
 
     private static boolean hasValue( String value )
     {
         return ( value != null && value.length() > 0 );
     }
     
     private void handleError( final String errorCode, Object[] msgArgs,
                                 final String methodName ) throws DataException
     {
         DataException dataEx = new DataException( errorCode, msgArgs );
         sm_logger.logp( Level.SEVERE, sm_className, methodName,
                         dataEx.getLocalizedMessage(), msgArgs );
         throw dataEx;
     }
 
     private void handleException( Throwable ex, String errorCode,
                         final String methodName, int paramIndex ) throws DataException
     {
         Object methodArg = ( paramIndex > 0 ) ? 
                             new Integer( paramIndex ) : null;
         handleException( ex, errorCode, methodName, methodArg );
     }
     
     private void handleException( Throwable ex, String errorCode,
                         final String methodName, Object methodArg ) throws DataException
     {
         DataException dataEx = ( methodArg != null ) ?
                 new DataException( errorCode, ex, methodArg ) :
                 new DataException( errorCode, ex );
         sm_logger.logp( Level.SEVERE, sm_className, methodName,
                         dataEx.getLocalizedMessage(), ex );
         throw dataEx;
     }
 
     private void handleException( Throwable ex, String errorCode, Object[] msgArgs,
                         final String methodName ) throws DataException
     {
         DataException dataEx = new DataException( errorCode, ex, msgArgs );
         sm_logger.logp( Level.SEVERE, sm_className, methodName,
                         dataEx.getLocalizedMessage(), ex );
         throw dataEx;
     }
 	
 	private static final class Property
 	{
 		private String m_name;
 		private String m_value;
 		
 		private Property( String name, String value )
 		{
 			m_name = name;
 			m_value = value;
 		}
 		
 		private String getName()
 		{
 			return m_name;
 		}
 		
 		private String getValue()
 		{
 			return m_value;
 		}
 	}
 	
 	static final class CustomColumn
 	{
 		private String m_name;
 		private Class m_type;
 		
 		CustomColumn( String name, Class type )
 		{
 			m_name = name;
 			m_type = type;
 		}
 		
 		private String getName()
 		{
 			return m_name;
 		}
 		
 		private Class getType()
 		{
 			return m_type;
 		}
 	}
 	
 	private static final class ParameterName
     {
         private String m_romName;
         private String m_nativeName;
         private boolean m_hasCheckedNativeName = false;
         private PreparedStatement m_stmt;
         
         private ParameterName( String romName, PreparedStatement stmt )
         {
             m_romName = romName;
             m_stmt = stmt;
         }
         
         private String getRomName()
         {
             return m_romName;
         }
         
         private String getNativeName()
         {
             if( m_nativeName == null && ! m_hasCheckedNativeName )
             {
                 // first try to get from merged runtime metadata and design hints
                 m_nativeName = m_stmt.getNativeNameFromParameterMetaData( m_romName );
 
                 // if not found, it could be that runtime param metadata has no info;
                 // see if it is available from design hints
                 if( m_nativeName == null )  
                     m_nativeName = m_stmt.getNativeNameFromParamHints( m_romName );
                 
                 m_hasCheckedNativeName = true;  // optimize to avoid repeated checking
             }
             
             return m_nativeName;
         }
         
         private String getEffectiveName()
         {
             String nativeName = getNativeName();
             return ( nativeName != null && nativeName.length() > 0 ) ? 
                     nativeName : getRomName();
         }
         
         private void logNullNativeName()
         {
             if( getNativeName() != null )
                 return;     // exists
             
             // no native name available, log info
            sm_logger.logp( Level.FINE, sm_className + ".ParameterName",  //$NON-NLS-1$
                     "logNullNativeName()",  //$NON-NLS-1$
                     "No native name available for parameter " + getRomName() + "." ); //$NON-NLS-1$  //$NON-NLS-2$
         }
         
         public String toString()
         {
             DataException resourceMsgHandler =
                 new DataException( ResourceConstants.PARAMETER_NAMES_INFO,
                         new Object[] { m_romName, m_nativeName } );
             return resourceMsgHandler.getLocalizedMessage();
         }
     }
 
 }
