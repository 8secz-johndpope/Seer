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
 
 package org.eclipse.birt.data.engine.executor;
 
 import java.util.ArrayList;
 import java.util.Collection;
 import java.util.Map;
 
 import org.eclipse.birt.data.engine.api.DataEngine;
 import org.eclipse.birt.data.engine.api.DataEngineContext;
 import org.eclipse.birt.data.engine.api.IBaseDataSetDesign;
 import org.eclipse.birt.data.engine.api.IBaseDataSourceDesign;
 import org.eclipse.birt.data.engine.api.IResultMetaData;
 import org.eclipse.birt.data.engine.core.DataException;
 import org.eclipse.birt.data.engine.impl.DataSetCacheUtil;
 import org.eclipse.birt.data.engine.impl.ResultMetaData;
 import org.eclipse.birt.data.engine.odi.IResultClass;
 
 /**
  * Cache manager for ODA data set and Scripted data set. Since connection to
  * data set and retrieve data are expensive operations, it is unnecessary to do
  * them again and again when user just want to see the data in the same
  * configuration of data set. The basic idea is when user want to use cache, the
  * data will be saved into cache in the first time and it will be loaded in the
  * later time. Once the configuration of data set is changed, the data needs to
  * be retrieved again.
  * 
  * Please notice the whole procedure: 1: first check whether data can be loaded
  * from cache 2.1: if yes, then data will be loaded and check whether data needs
  * to be saved into cache will be skipped. 2.2: if no, then data will be retrieved
  * from data set and then whether saving into cache will be checked 2.2.1: if
  * yes, then data will be saved into cache 2.2.2: if no, then nothing will be
  * done
  * 
  * There are three possible value of cacheRowCount: 1: -1, cache all data set 2:
  * 0, don't cache 3: >0, cache the specified value
  * 
  * Here whether data will be loaded from cache can be observed by external
  * caller, but about saving into cache is not.
  */
 public class DataSetCacheManager
 {
 	// whether cache is needed
 	private int cacheOption;
 
 	// how many rows needs to be always cached
 	private int alwaysCacheRowCount;
 
 	// data set id and its cache count
 	private IBaseDataSourceDesign dataSourceDesign;
 	private IBaseDataSetDesign dataSetDesign;
 	private Collection parameterHints;
 	private Map appContext;
 
 	// map manager instance
 	private CacheMapManager cacheMapManager;
 
 	private int mode;
 	
 	/**
 	 * user specified cache root directory.
 	 */
 	private String tempDir;
 	/**
 	 * Construction
 	 */
 	public DataSetCacheManager( String tempDir, DataEngine dataEngine )
 	{
 		this.tempDir = tempDir;
 		this.dataSourceDesign = null;
 		this.dataSetDesign = null;
 		this.cacheOption = DataEngineContext.CACHE_USE_DEFAULT;
 		this.alwaysCacheRowCount = 0;
 
 		this.cacheMapManager = new CacheMapManager( tempDir );
 		this.mode = DataEngineContext.CACHE_MODE_IN_DISK;
 	}
 
 	/**
 	 * 
 	 * @return
 	 */
 	public IBaseDataSourceDesign getCurrentDataSourceDesign( )
 	{
 		return this.dataSourceDesign;
 	}
 	
 	/**
 	 * 
 	 * @return
 	 */
 	public IBaseDataSetDesign getCurrentDataSetDesign()
 	{
 		return this.dataSetDesign;
 	}
 	
 	/**
 	 * 
 	 * @return
 	 */
 	public Collection getCurrentParameterHints()
 	{
 		if ( this.parameterHints != null )
 			return this.parameterHints;
 		else
 			return new ArrayList();
 	}
 	
 	/**
 	 * 
 	 * @return
 	 */
 	public Map getCurrentAppContext()
 	{
 		return this.appContext;
 	}
 	
 	/**
 	 * Enable cache on data set
 	 * 
 	 * @param cacheOption
 	 */
 	public void setCacheOption( int cacheOption )
 	{
 		this.cacheOption = cacheOption;
 	}
 
 	/**
 	 * 
 	 * @return
 	 */
 	public int getCacheOption( )
 	{
 		return this.cacheOption;
 	}
 	/**
 	 * Set the cache mode.
 	 * 
 	 * @param mode
 	 */
 	public void setCacheMode( int mode )
 	{
 		this.mode = mode;
 	}
 	
 	/**
 	 * @return
 	 */
 	public int suspendCache( )
 	{
 		int lastCacheOption = this.cacheOption;
 		this.setCacheOption( DataEngineContext.CACHE_USE_DISABLE );
 		this.setCacheMode( DataEngineContext.CACHE_MODE_IN_DISK );
 		return lastCacheOption;
 	}
 
 	/**
 	 * @param rowCount
 	 */
 	public void setAlwaysCacheRowCount( int rowCount )
 	{
 		this.alwaysCacheRowCount = rowCount;
 	}
 
 	/**
 	 * Remember before requesting any service, this function must be called in
 	 * advance to make sure using current data source and data set.
 	 * 
 	 * @param dataSourceDesign
 	 * @param datasetDesign
 	 */
 	public void setDataSourceAndDataSet(
 			IBaseDataSourceDesign dataSourceDesign,
 			IBaseDataSetDesign dataSetDesign, Collection parameterHints, Map appContext )
 	{
 		this.dataSourceDesign = dataSourceDesign;
 		this.dataSetDesign = dataSetDesign;
 		this.parameterHints = parameterHints;
 		this.appContext = appContext;
 		/*if ( needToMerge )
 		{
 			try
 			{
 				this.mergeDelta( );
 			}
 			catch ( BirtException e )
 			{
 			}
 		}*/
 	}
 
 	/**
 	 * @return
 	 */
 	public boolean doesSaveToCache( )
 	{
 		if ( needsToCache( dataSetDesign, cacheOption, alwaysCacheRowCount ) == false )
 			return false;
 
 		return cacheMapManager.doesSaveToCache( DataSourceAndDataSet.newInstance( this.dataSourceDesign,
 				this.dataSetDesign,
 				this.parameterHints ),
 				this.mode,
 				this.dataSourceDesign,
 				this.dataSetDesign,
 				this.parameterHints,
 				this.appContext );
 	}
 
 	/**
 	 * @return
 	 */
 	public boolean doesLikeToCache( )
 	{
 		return needsToCache( dataSetDesign, cacheOption, alwaysCacheRowCount );
 	}
 
 	/**
 	 * @param dataSourceDesign
 	 * @param dataSetDesign
 	 * @param parameterBindings
 	 * @param cacheOption
 	 * @param alwaysCacheRowCount
 	 * @return
 	 */
 	public boolean doesLoadFromCache( IBaseDataSourceDesign dataSourceDesign,
 			IBaseDataSetDesign dataSetDesign,
 			Collection parameterHints, Map appContext, int cacheOption, int alwaysCacheRowCount )
 	{
 		if ( needsToCache( dataSetDesign, cacheOption, alwaysCacheRowCount ) == false )
 			return false;
 
 		// this.setFirstLoad( false );
 
 		this.setDataSourceAndDataSet( dataSourceDesign,
 				dataSetDesign,
 				parameterHints,
 				appContext);
 		return cacheMapManager.doesLoadFromCache( DataSourceAndDataSet.newInstance( this.dataSourceDesign,
 				dataSetDesign,
 				parameterHints ) );
 	}
 
 	/**
 	 * @param dataSetDesign
 	 * @param cacheOption
 	 * @param alwaysCacheRowCount
 	 * @return
 	 */
 	public boolean needsToCache( IBaseDataSetDesign dataSetDesign,
 			int cacheOption, int alwaysCacheRowCount )
 	{
 		return DataSetCacheUtil.needsToCache( dataSetDesign,
 				cacheOption,
 				alwaysCacheRowCount );
 	}
 
 	/**
 	 * @return
 	 */
 	public int getCacheRowCount( )
 	{
 		return DataSetCacheUtil.getCacheRowCount( cacheOption,
 				alwaysCacheRowCount,
 				this.dataSetDesign == null ? 0
 						: this.dataSetDesign.getCacheRowCount( ) );
 	}
 
 	/**
 	 * Clear cache
 	 * 
 	 * @param dataSourceDesign2
 	 * @param dataSetDesign2
 	 */
 	public void clearCache( IBaseDataSourceDesign dataSourceDesign2,
 			IBaseDataSetDesign dataSetDesign2 )
 	{
 		if ( dataSourceDesign2 == null || dataSetDesign2 == null )
 			return;
 
 		DataSourceAndDataSet ds = DataSourceAndDataSet.newInstance( dataSourceDesign2,
 				dataSetDesign2,
 				null );
 		cacheMapManager.clearCache( ds );
 	}
 
 	/**
 	 * @return
 	 */
 	public IDataSetCacheObject getCacheObject( )
 	{
 		return  cacheMapManager.getCacheObject( DataSourceAndDataSet.newInstance( this.dataSourceDesign,
 				this.dataSetDesign,
 				this.parameterHints ) );
 	}
 
 	/**
 	 * only for test
 	 * 
 	 * @return
 	 */
 	public boolean doesLoadFromCache( )
 	{
 		if ( needsToCache( dataSetDesign, cacheOption, alwaysCacheRowCount ) == false )
 			return false;
 
 		return cacheMapManager.doesLoadFromCache( DataSourceAndDataSet.newInstance( this.dataSourceDesign,
 				this.dataSetDesign,
 				this.parameterHints ) );
 	}
 
 	/**
 	 * Notice, this method is only for test, it can not be called unless its use
 	 * is for test.
 	 */
 	public void resetForTest( )
 	{
 		dataSourceDesign = null;
 		dataSetDesign = null;
 		cacheOption = DataEngineContext.CACHE_USE_DEFAULT;
 		alwaysCacheRowCount = 0;
 
 		this.cacheMapManager.resetForTest( );
 	}
 
 	/**
 	 * Return the cached result metadata. Please note that parameter hint will
 	 * not change the returned metadata.
 	 * 
 	 * @return
 	 * @throws DataException
 	 */
 	public IResultMetaData getCachedResultMetadata(
 			IBaseDataSourceDesign dataSource, IBaseDataSetDesign dataSet )
 			throws DataException
 	{
 		IResultClass resultClass = this.cacheMapManager.getCachedResultClass( DataSourceAndDataSet.newInstance( dataSource,
 				dataSet,
 				null ) );
 		if ( resultClass != null )
 			return new ResultMetaData( resultClass );
 		else
 			return null;
 	}
 	
 	/**
 	 * get the user specified cache root directory.
 	 * @return
 	 */
 	public String getTempDir( )
 	{
 		return tempDir;
 	}
 
 	/**
 	 * 
 	 * @param dataSetDesign
 	 * @param cacheDir
 	 */
 	public void registerCacheDir( IBaseDataSetDesign dataSetDesign, String cacheDir )
 	{
 		cacheMapManager.getCacheDirMap( ).put( dataSetDesign, cacheDir );
 	}
 }
