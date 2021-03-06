 /*******************************************************************************
  * Copyright (c) 2004 Actuate Corporation.
  * All rights reserved. This program and the accompanying materials
  * are made available under the terms of the Eclipse Public License v1.0
  * which accompanies this distribution, and is available at
  * http://www.eclipse.org/legal/epl-v10.html
  *
  * Contributors:
  *  Actuate Corporation  - initial API and implementation
  *******************************************************************************/
 
 package org.eclipse.birt.data.engine.executor.transform;
 
 import java.io.IOException;
 import java.util.ArrayList;
 import java.util.List;
 import java.util.logging.Level;
 import java.util.logging.Logger;
 
 import org.eclipse.birt.core.data.DataType;
 import org.eclipse.birt.data.engine.api.DataEngineContext;
 import org.eclipse.birt.data.engine.api.IComputedColumn;
 import org.eclipse.birt.data.engine.api.IQueryDefinition;
 import org.eclipse.birt.data.engine.core.DataException;
 import org.eclipse.birt.data.engine.executor.BaseQuery;
 import org.eclipse.birt.data.engine.executor.ResultClass;
 import org.eclipse.birt.data.engine.executor.ResultFieldMetadata;
 import org.eclipse.birt.data.engine.executor.cache.ResultSetCache;
 import org.eclipse.birt.data.engine.executor.dscache.DataSetResultCache;
 import org.eclipse.birt.data.engine.impl.ComputedColumnHelper;
 import org.eclipse.birt.data.engine.impl.DataEngineSession;
 import org.eclipse.birt.data.engine.impl.IExecutorHelper;
 import org.eclipse.birt.data.engine.impl.document.StreamWrapper;
 import org.eclipse.birt.data.engine.impl.document.stream.StreamManager;
 import org.eclipse.birt.data.engine.impl.document.stream.VersionManager;
 import org.eclipse.birt.data.engine.odaconsumer.ResultSet;
 import org.eclipse.birt.data.engine.odi.AggrHolderManager;
 import org.eclipse.birt.data.engine.odi.IAggrValueHolder;
 import org.eclipse.birt.data.engine.odi.ICustomDataSet;
 import org.eclipse.birt.data.engine.odi.IDataSetPopulator;
 import org.eclipse.birt.data.engine.odi.IEventHandler;
 import org.eclipse.birt.data.engine.odi.IResultClass;
 import org.eclipse.birt.data.engine.odi.IResultIterator;
 import org.eclipse.birt.data.engine.odi.IResultObject;
 
 /**
  * OdiResultSet is responsible for accessing data sources and some processing
  * like sorting and filtering on the rows returned. It provide APIs for the
  * upper layer to fetch data rows and get group information, etc.
  * 
  */
 public class CachedResultSet implements IResultIterator
 {
 
 	private ResultSetPopulator resultSetPopulator;
 	private IEventHandler handler;
 	private ResultSet resultSet;
 	private static String className = CachedResultSet.class.getName( );
 	private static Logger logger = Logger.getLogger( className ); 
 	private AggrHolderManager aggrHolderManager = new AggrHolderManager();
 	/**
 	 * Nothing, only for new an instance, needs to be used with care. Currently
 	 * it is only used in report document saving when there is no data set.
 	 */
 	public CachedResultSet( )
 	{
 	}
 	
 	/**
 	 * Constructs and intializes OdiResultSet based on data in a ODA result set
 	 * @param query
 	 * @param meta
 	 * @param odaResultSet
 	 * @param eventHandler
 	 * @param session
 	 * @param stopSign
 	 * @throws DataException
 	 */
 	public CachedResultSet( BaseQuery query, IResultClass meta,
 			ResultSet odaResultSet, IEventHandler eventHandler,
 			DataEngineSession session ) throws DataException
 	{
 		this.handler = eventHandler;
 		this.resultSetPopulator = new ResultSetPopulator( query,
 				meta,
 				this,
 				session,
 				eventHandler );
 		resultSetPopulator.populateResultSet( new OdiResultSetWrapper( odaResultSet) );
 	}
 
 	/**
 	 * Constructs and intializes OdiResultSet based on data in an IJointDataSetPopulator
 	 * @param query
 	 * @param meta
 	 * @param odaResultSet
 	 * @param eventHandler
 	 * @param session
 	 * @param stopSign
 	 * @throws DataException
 	 */
 	public CachedResultSet( BaseQuery query, IResultClass meta,
 			IDataSetPopulator odaResultSet, IEventHandler eventHandler,
 			DataEngineSession session ) throws DataException
 	{
 		this.handler = eventHandler;
 		this.resultSetPopulator = new ResultSetPopulator( query,
 				meta,
 				this,
 				session,
 				eventHandler);
 		resultSetPopulator.populateResultSet( new OdiResultSetWrapper( odaResultSet));
 	}
 	
 	/**
 	 * @param query
 	 * @param meta
 	 * @param odaCacheResultSet
 	 * @param stopSign
 	 * @throws DataException
 	 */
 	public CachedResultSet( BaseQuery query, IResultClass meta,
 			DataSetResultCache odaCacheResultSet, IEventHandler eventHandler,
 			DataEngineSession session )
 			throws DataException
 	{
 		this.handler = eventHandler;
 		this.resultSetPopulator = new ResultSetPopulator( query,
 				meta,
 				this,
 				session,
 				eventHandler
 				);
 		resultSetPopulator.populateResultSet( new OdiResultSetWrapper( odaCacheResultSet ));
 		odaCacheResultSet.close( );
 	}
 
 	/**
 	 * 
 	 * @param query
 	 * @param customDataSet
 	 * @param eventHandler
 	 * @param session
 	 * @param stopSign
 	 * @throws DataException
 	 */
 	public CachedResultSet( BaseQuery query, ICustomDataSet customDataSet,
 			IEventHandler eventHandler,
 			DataEngineSession session ) throws DataException
 	{
 		this.handler = eventHandler;
 		assert customDataSet != null;
 		this.resultSetPopulator = new ResultSetPopulator( query,
 				customDataSet.getResultClass( ),
 				this,
 				session,
 				eventHandler);
 		resultSetPopulator.populateResultSet(new OdiResultSetWrapper( customDataSet));
 	}
 
 	/**
 	 * 
 	 * @param query
 	 * @param meta
 	 * @param parentResultIterator
 	 * @param groupLevel
 	 * @param eventHandler
 	 * @param session
 	 * @param stopSign
 	 * @throws DataException
 	 */
 	public CachedResultSet( BaseQuery query, IResultClass meta,
 			IResultIterator parentResultIterator, int groupLevel, IEventHandler eventHandler,
 			DataEngineSession session )
 			throws DataException
 	{
 		this.handler = eventHandler;
 		assert parentResultIterator instanceof CachedResultSet;
 		CachedResultSet parentResultSet = (CachedResultSet) parentResultIterator;
 
 		// this.resultSetPopulator.getGroupCalculationUtil( ).setResultSetCache(
 		// parentResultSet.resultSetPopulator.getCache( ));
 		int[] groupInfo = parentResultSet.getCurrentGroupInfo( groupLevel );
 		this.resultSetPopulator = new ResultSetPopulator( query,
 				createCustomDataSetMetaData(query, meta ),
 				this,
 				session,
 				eventHandler);
 		this.resultSetPopulator.populateResultSet( new OdiResultSetWrapper( new Object[]{
 				parentResultSet.resultSetPopulator.getCache( ), groupInfo
 		} ));
 	}
 
 	private IResultClass createCustomDataSetMetaData(BaseQuery query,
 			IResultClass meta) throws DataException 
 	{
 
 		List projectedColumns = new ArrayList();
 		if (query.getFetchEvents() != null) {
 			for (int i = 0; i < meta.getFieldCount(); i++) {
 				ResultFieldMetadata rfMeta = new ResultFieldMetadata(i, meta
 						.getFieldName(i + 1), meta.getFieldLabel(i + 1), meta
 						.getFieldValueClass(i + 1), meta
 						.getFieldNativeTypeName(i + 1), false);
 				rfMeta.setAlias( meta.getFieldAlias( i+1 ) );
 				projectedColumns.add( rfMeta );
 			}
 			for (int j = 0; j < query.getFetchEvents().size(); j++) {
 				if (query.getFetchEvents().get(j) instanceof ComputedColumnHelper) {
 					ComputedColumnHelper helper = (ComputedColumnHelper) query
 							.getFetchEvents().get(j);
 					helper.setModel(TransformationConstants.RESULT_SET_MODEL);
 					for (int i = 0; i < helper.getComputedColumnList().size(); i++) {
 						projectedColumns.add(new ResultFieldMetadata(i + 1
 								+ meta.getFieldCount(),
 								((IComputedColumn) helper
 										.getComputedColumnList().get(i))
 										.getName(), ((IComputedColumn) helper
 										.getComputedColumnList().get(i))
 										.getName(),
 								DataType.getClass(((IComputedColumn) helper
 										.getComputedColumnList().get(i))
 										.getDataType()), null, true));
 					}
 				}
 				meta = new ResultClass(projectedColumns);
 			}
 		}
 		return meta;
 	}
 
 	/**
 	 * Returns all rows in the current group at the specified group level, as an
 	 * array of ResultObject objects.
 	 * 
 	 * @param groupLevel
 	 * @return int[], group star index and end index
 	 * @throws DataException
 	 */
 	private int[] getCurrentGroupInfo( int groupLevel ) throws DataException
 	{
 		return this.resultSetPopulator.getGroupProcessorManager( )
 				.getGroupCalculationUtil( )
 				.getGroupInformationUtil( )
 				.getCurrentGroupInfo( groupLevel );
 	}
 
 	/*
 	 * @see org.eclipse.birt.data.engine.odi.IResultIterator#doSave(org.eclipse.birt.data.engine.impl.document.StreamWrapper,
 	 *      boolean)
 	 */
 	public void doSave( StreamWrapper streamsWrapper, boolean isSubQuery )
 			throws DataException
 	{
 		if ( streamsWrapper.getStreamForGroupInfo( ) != null )
 		{
 			// save group info
 			this.resultSetPopulator.getGroupProcessorManager( )
 					.getGroupCalculationUtil( )
 					.doSave( streamsWrapper.getStreamForGroupInfo( ) );
 		}
 
 		// save result class
		if ( isSubQuery == false && (!( (IQueryDefinition) this.resultSetPopulator.getQuery( )
				.getQueryDefinition( ) ).isSummaryQuery( )) && 
 				streamsWrapper.getStreamForResultClass( ) != null )
 		{
 			( (ResultClass) this.resultSetPopulator.getResultSetMetadata( ) ).doSave( streamsWrapper.getStreamForResultClass( ),
 					resultSetPopulator.getEventHandler( )
 							.getAllColumnBindings( ) );
 			try
 			{
 				streamsWrapper.getStreamForResultClass( ).close( );
 				if ( streamsWrapper.getStreamForDataSet( ) != null )
 				{
 					this.resultSetPopulator.getCache( )
 								.doSave( streamsWrapper.getStreamForDataSet( ),
 										streamsWrapper.getStreamForDataSetRowLens( ),
 										resultSetPopulator.getEventHandler( )
 												.getAllColumnBindings( ) );
 				}
 				streamsWrapper.getStreamForDataSet( ).close( );
 				streamsWrapper.getStreamForDataSetRowLens( ).close( );
 			}
 			catch ( IOException e )
 			{
 				logger.log( Level.FINE, e.getMessage( ), e );
 			}
 		}
 		
 		if ( streamsWrapper.getStreamManager( ).getVersion( ) >= VersionManager.VERSION_2_5_1_0 )
 		{
 			if ( !aggrHolderManager.isEmpty( ) )
 			{
 				aggrHolderManager.doSave( streamsWrapper.getStreamManager( )
 						.getOutStream( DataEngineContext.AGGR_INDEX_STREAM,
 								StreamManager.ROOT_STREAM,
 								StreamManager.BASE_SCOPE ),
 						streamsWrapper.getStreamManager( )
 								.getOutStream( DataEngineContext.AGGR_VALUE_STREAM,
 										StreamManager.ROOT_STREAM,
 										StreamManager.BASE_SCOPE ) );
 			}
 		}
 	}
 	
 	/*
 	 * Close this data set
 	 * 
 	 * @see org.eclipse.birt.data.engine.odi.IResultIterator#close()
 	 */
 	public void close( ) throws DataException
 	{
 		if ( this.resultSetPopulator == null
 				|| this.resultSetPopulator.getCache( ) == null )
 			return; // already closed
 
 		this.resultSetPopulator.getCache( ).close( );
 
 		resultSetPopulator = null;
 		
 		try
 		{
 			if ( resultSet != null )
 				resultSet.close( );
 		}
 		catch ( DataException e )
 		{
 			logger.logp( Level.FINE,
 					className,
 					"closeOdaResultSet",
 					"Exception at CachedResultSet.close()",
 					e );
 		}
 	}
 	
 	/*
 	 * @see org.eclipse.birt.data.engine.odi.IResultIterator#getCurrentResult()
 	 */
 	public IResultObject getCurrentResult( ) throws DataException
 	{
 		assert this.resultSetPopulator != null
 				&& this.resultSetPopulator.getCache( ) != null;
 		return this.resultSetPopulator.getCache( ).getCurrentResult( );
 	}
 
 	/*
 	 * Advances row cursor, return false if no more rows.
 	 * 
 	 * @see org.eclipse.birt.data.engine.odi.IResultIterator#next()
 	 */
 	public boolean next( ) throws DataException
 	{
 		// Make sure that the result set has been opened.
 		assert this.resultSetPopulator != null
 				&& this.resultSetPopulator.getCache( ) != null;
 		boolean hasNext = this.resultSetPopulator.getCache( ).next( );
 
 		this.resultSetPopulator.getGroupProcessorManager( )
 				.getGroupCalculationUtil( )
 				.getGroupInformationUtil( )
 				.next( hasNext );
 
 		return hasNext;
 	}
 	
 	/*
 	 * @see org.eclipse.birt.data.engine.odi.IResultIterator#getEndingGroupLevel()
 	 */
 	public int getEndingGroupLevel( ) throws DataException
 	{
 		return this.resultSetPopulator.getEndingGroupLevel( );
 	}
 
 	/**
 	 * @see org.eclipse.birt.data.engine.odi.IResultIterator#getStartingGroupLevel()
 	 */
 	public int getStartingGroupLevel( ) throws DataException
 	{
 		return this.resultSetPopulator.getStartingGroupLevel( );
 	}
 
 	/*
 	 * Rewinds row cursor to the first row at the specified group level
 	 * 
 	 * @see org.eclipse.birt.data.engine.odi.IResultIterator#first(int)
 	 */
 	public void first( int groupLevel ) throws DataException
 	{
 		this.resultSetPopulator.first( groupLevel );
 	}
 	
 	/*
 	 * Advances row cursor to the last row at the specified group level
 	 * 
 	 * @see org.eclipse.birt.data.engine.odi.IResultIterator#last(int)
 	 */
 	public void last( int groupLevel ) throws DataException
 	{
 		this.resultSetPopulator.last( groupLevel );
 	}
 
 	/*
 	 * @see org.eclipse.birt.data.engine.odi.IResultIterator#getResultClass()
 	 */
 	public IResultClass getResultClass( ) throws DataException
 	{
 		return this.resultSetPopulator.getResultSetMetadata( );
 	}
 
 	/*
 	 * @see org.eclipse.birt.data.engine.odi.IResultIterator#getCurrentResultIndex()
 	 */
 	public int getCurrentResultIndex( ) throws DataException
 	{
 		assert this.resultSetPopulator != null
 				&& this.resultSetPopulator.getCache( ) != null;
 		return this.resultSetPopulator.getCache( ).getCurrentIndex( );
 	}
 
 	/*
 	 * @see org.eclipse.birt.data.engine.odi.IResultIterator#getCurrentGroupIndex(int)
 	 */
 	public int getCurrentGroupIndex( int groupLevel ) throws DataException
 	{
 		return this.resultSetPopulator.getCurrentGroupIndex( groupLevel );
 	}
 
 	/*
 	 * @see org.eclipse.birt.data.engine.odi.IResultIterator#getGroupStartAndEndIndex(int)
 	 */
 	public int[] getGroupStartAndEndIndex( int groupLevel )
 			throws DataException
 	{
 		return this.resultSetPopulator.getGroupStartAndEndIndex( groupLevel );
 	}
 	
 	/*
 	 * @see org.eclipse.birt.data.engine.odi.IResultIterator#getRowCount()
 	 */
 	public int getRowCount( ) throws DataException
 	{
 		return this.resultSetPopulator.getCache( ).getCount( );
 	}
 
 	/*
 	 * (non-Javadoc)
 	 * @see org.eclipse.birt.data.engine.odi.IResultIterator#getResultSetCache()
 	 */
 	public ResultSetCache getResultSetCache( )
 	{
 		return this.resultSetPopulator.getCache( );
 	}
 
 	/*
 	 * (non-Javadoc)
 	 * @see org.eclipse.birt.data.engine.odi.IResultIterator#getExecutorHelper()
 	 */
 	public IExecutorHelper getExecutorHelper( )
 	{
 		if( handler!= null )
 			return this.handler.getExecutorHelper( );
 		else
 			return null;
 	}
 	
 	/**
 	 * Set Oda ResultSet so that it could be closed when closing IResultIterator
 	 * 
 	 * @param resultSet
 	 */
 	public void setOdaResultSet( ResultSet resultSet )
 	{
 		this.resultSet = resultSet;
 	}
 
 	/*
 	 * (non-Javadoc)
 	 * @see org.eclipse.birt.data.engine.odi.IResultIterator#getAggrValue(java.lang.String)
 	 */
 	public Object getAggrValue( String aggrName )
 			throws DataException
 	{
 		return this.aggrHolderManager.getAggrValue( aggrName );
 	}
 	
 	/**
 	 * Set the aggregation value holder from which the aggregation values are
 	 * fetched.
 	 * @param holder
 	 * @throws DataException 
 	 */
 	public void addAggrValueHolder( IAggrValueHolder holder ) throws DataException
 	{
 		this.aggrHolderManager.addAggrValueHolder( holder );
 	}
 	
 	/**
 	 * Clear the aggregation value holder for the re-calculation.
 	 * @throws DataException 
 	 */
 	public void clearAggrValueHolder( ) throws DataException
 	{
 		this.aggrHolderManager.clear( );
 	}
 
 }
