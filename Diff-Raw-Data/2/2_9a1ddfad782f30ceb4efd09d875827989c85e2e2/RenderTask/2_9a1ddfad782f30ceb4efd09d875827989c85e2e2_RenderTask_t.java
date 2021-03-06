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
 
 package org.eclipse.birt.report.engine.api.impl;
 
 import java.util.ArrayList;
 import java.util.Collection;
 import java.util.Iterator;
 import java.util.List;
 import java.util.logging.Level;
 
 import org.eclipse.birt.report.engine.api.EngineConfig;
 import org.eclipse.birt.report.engine.api.EngineException;
 import org.eclipse.birt.report.engine.api.IRenderTask;
 import org.eclipse.birt.report.engine.api.IReportDocument;
 import org.eclipse.birt.report.engine.api.IReportEngine;
 import org.eclipse.birt.report.engine.api.IReportRunnable;
 import org.eclipse.birt.report.engine.api.InstanceID;
 import org.eclipse.birt.report.engine.emitter.EngineEmitterServices;
 import org.eclipse.birt.report.engine.emitter.IContentEmitter;
 import org.eclipse.birt.report.engine.executor.ReportExecutor;
 import org.eclipse.birt.report.engine.extension.internal.ExtensionManager;
 import org.eclipse.birt.report.engine.i18n.MessageConstants;
 import org.eclipse.birt.report.engine.internal.document.ReportContentLoader;
 import org.eclipse.birt.report.engine.presentation.LocalizedEmitter;
 
 public class RenderTask extends EngineTask implements IRenderTask
 {
 	IReportDocument reportDoc;
 	String emitterID;
 	boolean bodyOnly;
 
 	private InnerRender innerRender;
 
 	/**
 	 * @param engine
 	 *            the report engine
 	 * @param runnable
 	 *            the report runnable object
 	 * @param reportDoc
 	 *            the report document instance
 	 */
 	public RenderTask( IReportEngine engine, IReportRunnable runnable,
 			IReportDocument reportDoc )
 	{
 		super( engine, runnable );
 
 		executionContext.setFactoryMode( false );
 		executionContext.setPresentationMode( true );
 
 		// open the report document
 		openReportDocument( reportDoc );
 		
 		// load report design
 		loadDesign( );
 
 		innerRender = new PageRangeRender( new long[]{1, this.reportDoc.getPageCount( )} );
 	}
 
 	protected void openReportDocument( IReportDocument reportDoc )
 	{
 		this.reportDoc = reportDoc;
 		executionContext.setReportDocument( reportDoc );
 
 		// load the informationf rom the report document
 		setParameterValues( reportDoc.getParameterValues( ) );
 		executionContext.registerGlobalBeans( reportDoc
 				.getGlobalVariables( null ) );
 	}
 
 	protected void closeReportDocument( )
 	{
 		//the report document is shared by mutiple render task, 
 		//it is open by the caller, it should be closed by the caller.
 		//reportDoc.close( );
 	}
 
 	/*
 	 * (non-Javadoc)
 	 * 
 	 * @see org.eclipse.birt.report.engine.api.IRenderTask#render(long)
 	 */
 	public void render( long pageNumber ) throws EngineException
 	{
 		setPageNumber( pageNumber );
 		render( );
 	}
 
 	protected IContentEmitter createContentEmitter( ReportExecutor executor )
 			throws EngineException
 	{
 		// create the emitter services object that is needed in the emitters.
 		EngineEmitterServices services = new EngineEmitterServices( this );
 
 		EngineConfig config = engine.getConfig( );
 		if ( config != null )
 		{
 			services.setEmitterConfig( config.getEmitterConfigs( ) );
 		}
 		services.setRenderOption( renderOptions );
 		services.setExecutor( executor );
 		services.setRenderContext( appContext );
 		services.setReportRunnable( runnable );
 
 		String format = executionContext.getOutputFormat( );
 		if ( format == null )
 		{
 			format = "html"; //$NON-NLS-1$
 		}
 		
 		if ("html".equalsIgnoreCase( format ))
 		{
 			bodyOnly = false;
 		}
 		else
 		{
 			bodyOnly = true;
 		}
 
 		ExtensionManager extManager = ExtensionManager.getInstance( );
 		boolean supported = false;
 		Collection supportedFormats = extManager.getSupportedFormat( );
 		Iterator iter = supportedFormats.iterator( );
 		while ( iter.hasNext( ) )
 		{
 			String supportedFormat = (String) iter.next( );
 			if ( supportedFormat != null
 					&& supportedFormat.equalsIgnoreCase( format ) )
 			{
 				supported = true;
 				break;
 			}
 		}
 
 		if ( !supported )
 		{
 			log.log( Level.SEVERE,
 					MessageConstants.FORMAT_NOT_SUPPORTED_EXCEPTION, format );
 			throw new EngineException(
 					MessageConstants.FORMAT_NOT_SUPPORTED_EXCEPTION, format );
 		}
 
 		IContentEmitter emitter = extManager.createEmitter( format, emitterID );
 		if ( emitter == null )
 		{
 			log.log( Level.SEVERE, "Report engine can not create {0} emitter.", //$NON-NLS-1$
 					format ); // $NON-NLS-1$
 			throw new EngineException(
 					MessageConstants.CANNOT_CREATE_EMITTER_EXCEPTION );
 		}
 
 		// localized emitter
 		emitter = new LocalizedEmitter( executionContext, emitter );
 
 		// emitter is not null
 		emitter.initialize( services );
 
 		return emitter;
 	}
 
 	/**
 	 * @param pageNumber
 	 *            the page to be rendered
 	 * @throws EngineException
 	 *             throws exception if there is a rendering error
 	 */
 	protected void doRender( long pageNumber ) throws EngineException
 	{
 		try
 		{
 			// start the render
 			ReportContentLoader loader = new ReportContentLoader(
 					executionContext );
 			ReportExecutor executor = new ReportExecutor( executionContext );
 			executionContext.setExecutor( executor );
 			IContentEmitter emitter = createContentEmitter( executor );
 			startRender( );
 			loader.loadPage( pageNumber, bodyOnly, emitter );
 			closeRender( );
 		}
 		catch ( Exception ex )
 		{
 			log.log( Level.SEVERE,
 					"An error happened while running the report. Cause:", ex ); //$NON-NLS-1$
 		}
 		catch ( OutOfMemoryError err )
 		{
 			log.log( Level.SEVERE,
 					"An OutOfMemory error happened while running the report." ); //$NON-NLS-1$
 			throw err;
 		}
 	}
 
 	/**
 	 * @param pageNumber
 	 *            the page to be rendered
 	 * @throws EngineException
 	 *             throws exception if there is a rendering error
 	 */
 	protected void doRender( List pageSequences ) throws EngineException
 	{
 		if ( pageSequences.size( ) == 0 )
 		{
 			return;
 		}
 		try
 		{
 			// start the render
 
 			ReportContentLoader loader = new ReportContentLoader(
 					executionContext );
 			ReportExecutor executor = new ReportExecutor( executionContext );
 			executionContext.setExecutor( executor );
 			IContentEmitter emitter = createContentEmitter( executor );
 			startRender( );
 			loader.loadPageRange( pageSequences, bodyOnly, emitter );
 			closeRender( );
 		}
 		catch ( Exception ex )
 		{
 			log.log( Level.SEVERE,
 					"An error happened while running the report. Cause:", ex ); //$NON-NLS-1$
 		}
 		catch ( OutOfMemoryError err )
 		{
 			log.log( Level.SEVERE,
 					"An OutOfMemory error happened while running the report." ); //$NON-NLS-1$
 			throw err;
 		}
 	}
 	
 	/**
 	 * @param pageNumber
 	 *            the page to be rendered
 	 * @throws EngineException
 	 *             throws exception if there is a rendering error
 	 */
 	protected void doRender( InstanceID iid) throws EngineException
 	{
 		try
 		{
 			long offset = reportDoc.getInstanceOffset( iid );
 			if (offset != -1)
 			{
 				// start the render
 				ReportContentLoader loader = new ReportContentLoader(
 						executionContext );
 				ReportExecutor executor = new ReportExecutor( executionContext );
 				executionContext.setExecutor( executor );
 				IContentEmitter emitter = createContentEmitter( executor );
 				startRender( );
 				loader.loadReportlet(offset, emitter );
 				closeRender( );
 			}
 		}
 		catch ( Exception ex )
 		{
 			log.log( Level.SEVERE,
 					"An error happened while running the report. Cause:", ex ); //$NON-NLS-1$
 		}
 		catch ( OutOfMemoryError err )
 		{
 			log.log( Level.SEVERE,
 					"An OutOfMemory error happened while running the report." ); //$NON-NLS-1$
 			throw err;
 		}
 	}	
 
 	/*
 	 * (non-Javadoc)
 	 * 
 	 * @see org.eclipse.birt.report.engine.api.IRenderTask#setEmitterID(java.lang.String)
 	 */
 	public void setEmitterID( String id )
 	{
 		this.emitterID = id;
 	}
 
 	/**
 	 * @return the emitter ID to be used to render this report. Could be null,
 	 *         in which case the engine will choose one emitter that matches the
 	 *         requested output format.
 	 */
 	public String getEmitterID( )
 	{
 		return this.emitterID;
 	}
 
 	public void close( )
 	{
 		closeReportDocument( );
 		super.close( );
 	}
 
 	public void render( String pageRange ) throws EngineException
 	{
 		setPageRange( pageRange );
 		render( );
 	}
 
 	private List parsePageSequence( String pageRange, long totalPage )
 	{
 		ArrayList list = new ArrayList( );
 		if ( null == pageRange
 				|| "".equals( pageRange ) || pageRange.toUpperCase( ).indexOf( "ALL" ) >= 0 ) //$NON-NLS-1$ //$NON-NLS-2$
 		{
 			list.add( new long[]{1, totalPage} );
 			return list;
 		}
 		String[] ps = pageRange.split( "," ); //$NON-NLS-1$
 		for ( int i = 0; i < ps.length; i++ )
 		{
 			try
 			{
 				if ( ps[i].indexOf( "-" ) > 0 ) //$NON-NLS-1$
 				{
 					String[] psi = ps[i].split( "-" ); //$NON-NLS-1$
 					if ( psi.length == 2 )
 					{
 						long start = Long.parseLong( psi[0].trim( ) );
 						long end = Long.parseLong( psi[1].trim( ) );
 						if ( end > start )
 						{
 							list.add( new long[]{Math.max( start, 1 ),
 									Math.min( end, totalPage )} );
 						}
 					}
 					else
 					{
 						log.log( Level.SEVERE,
 								"error page number range: {0}", ps[i] ); //$NON-NLS-1$
 					}
 				}
 				else
 				{
 					long number = Long.parseLong( ps[i].trim( ) );
 					if ( number > 0 && number <= totalPage )
 					{
 						list.add( new long[]{number, number} );
 					}
 					else
 					{
 						log.log( Level.SEVERE,
 								"error page number range: {0}", ps[i] ); //$NON-NLS-1$
 					}
 
 				}
 			}
 			catch ( NumberFormatException ex )
 			{
 				log.log( Level.SEVERE, "error page number rang:", ps[i] ); //$NON-NLS-1$
 			}
 		}
 		return sort( list );
 	}
 
 	private List sort( List list )
 	{
 		for ( int i = 0; i < list.size( ); i++ )
 		{
 			long[] currentI = (long[]) list.get( i );
 			int minIndex = i;
 			long[] min = currentI;
 			for ( int j = i + 1; j < list.size( ); j++ )
 			{
 				long[] currentJ = (long[]) list.get( j );
 				if ( currentJ[0] < min[0] )
 				{
 					minIndex = j;
 					min = currentJ;
 				}
 			}
 			if ( minIndex != i )
 			{
 				// swap
 				list.set( i, min );
 				list.set( minIndex, currentI );
 			}
 		}
 		long[] current = null;
 		long[] last = null;
 		ArrayList ret = new ArrayList( );
 		for ( int i = 0; i < list.size( ); i++ )
 		{
 			current = (long[]) list.get( i );
 			if ( last != null )
 			{
 				if ( current[1] <= last[1] )
 					continue;
 				if ( current[0] <= last[1] )
 					current[0] = last[1];
 					ret.add( current );
 			}
 			else
 			{
 				ret.add( current );
 			}
 			last = current;
 		}
 		return ret;
 	}
 
 	/* (non-Javadoc)
 	 * @see org.eclipse.birt.report.engine.api.IRenderTask#render(org.eclipse.birt.report.engine.api.InstanceID)
 	 */
 	public void render(InstanceID iid) throws EngineException {
 		setInstanceID( iid );
 		render( );
 	}
 
 	/* (non-Javadoc)
 	 * @see org.eclipse.birt.report.engine.api.IRenderTask#render()
 	 */
 	public void render() throws EngineException {
 		if ( renderOptions == null )
 		{
 			throw new EngineException(
 					"Render options have to be specified to render a report." ); //$NON-NLS-1$
 		}
 		innerRender.render( );
 	}
 
 	/* (non-Javadoc)
 	 * @see org.eclipse.birt.report.engine.api.IRenderTask#render()
 	 */
 	public void setPageNumber( long pageNumber ) throws EngineException
 	{
 		long totalPage = reportDoc.getPageCount( );
 		if ( pageNumber <= 0 || pageNumber > totalPage )
 		{
 			throw new EngineException( "Can't find page hints :{0}", new Long( //$NON-NLS-1$
 					pageNumber ) );
 		}
 		innerRender = new PageRender( pageNumber);
 	}
 
 	/* (non-Javadoc)
 	 * @see org.eclipse.birt.report.engine.api.IRenderTask#setInstanceID( InstanceID iid )
 	 */
 	public void setInstanceID( InstanceID iid ) throws EngineException
 	{
 		//TODO: replace the default implementation.
 		innerRender = new ReportletRender( iid );
 	}
 
 	/* (non-Javadoc)
 	 * @see org.eclipse.birt.report.engine.api.IRenderTask#setPageRange( String pageRange )
 	 */
 	public void setPageRange( String pageRange ) throws EngineException
 	{
 		innerRender = new PageRangeRender( parsePageSequence( pageRange, reportDoc.getPageCount( )) );
 	}
 
 	/* (non-Javadoc)
 	 * @see org.eclipse.birt.report.engine.api.IRenderTask#setBookmark( String bookmark )
 	 */
 	public void setBookmark( String bookmark ) throws EngineException
 	{
 		long pageNumber = reportDoc.getPageNumber( bookmark );
		if ( pageNumber <= 0 ) {
 			throw new EngineException( "Can't find bookmark :{0}", bookmark ); //$NON-NLS-1$
 		}
 		innerRender = new PageRender( pageNumber );
 	}
 
 	private interface InnerRender
 	{
 		void render() throws EngineException;
 	}
 
 	/**
 	 * Renders a page with a page number. 
 	 */
 	private class PageRender implements InnerRender
 	{
 		private long pageNumber;
 
 		public PageRender( long pageNumber )
 		{
 			this.pageNumber = pageNumber;
 		}
 
 		public void render( ) throws EngineException
 		{
 			RenderTask.this.doRender( pageNumber );
 		}
 	}
 
 	/**
 	 * Renders a range of pages. 
 	 */
 	private class PageRangeRender implements InnerRender
 	{
 		private List pageRange;
 
 		public PageRangeRender( long[] arrayRange )
 		{
 			this.pageRange = new ArrayList();
 			pageRange.add( arrayRange );
 		}
 
 		public PageRangeRender( List pageRange )
 		{
 			this.pageRange = pageRange;
 		}
 
 		public void render( ) throws EngineException
 		{
 			RenderTask.this.doRender( pageRange );
 		}
 	}
 	
 	private class ReportletRender implements InnerRender
 	{
 		private InstanceID iid;
 		ReportletRender(InstanceID iid)
 		{
 			this.iid = iid;
 		}
 		
 		public void render( ) throws EngineException
 		{
 			RenderTask.this.doRender( iid );
 		}
 	}
 }
