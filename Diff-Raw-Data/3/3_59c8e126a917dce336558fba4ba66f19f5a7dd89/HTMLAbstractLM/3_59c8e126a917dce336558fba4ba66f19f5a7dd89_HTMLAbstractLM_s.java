 /***********************************************************************
  * Copyright (c) 2004 Actuate Corporation.
  * All rights reserved. This program and the accompanying materials
  * are made available under the terms of the Eclipse Public License v1.0
  * which accompanies this distribution, and is available at
  * http://www.eclipse.org/legal/epl-v10.html
  *
  * Contributors:
  * Actuate Corporation - initial API and implementation
  ***********************************************************************/
 
 package org.eclipse.birt.report.engine.layout.html;
 
 import java.util.logging.Logger;
 
 import org.eclipse.birt.report.engine.content.ICellContent;
 import org.eclipse.birt.report.engine.content.IColumn;
 import org.eclipse.birt.report.engine.content.IContent;
 import org.eclipse.birt.report.engine.content.IReportContent;
 import org.eclipse.birt.report.engine.content.IStyle;
 import org.eclipse.birt.report.engine.css.engine.value.birt.BIRTConstants;
 import org.eclipse.birt.report.engine.emitter.ContentEmitterUtil;
 import org.eclipse.birt.report.engine.emitter.IContentEmitter;
 import org.eclipse.birt.report.engine.extension.IReportItemExecutor;
 import org.eclipse.birt.report.engine.ir.EngineIRConstants;
 import org.eclipse.birt.report.engine.ir.MasterPageDesign;
 import org.eclipse.birt.report.engine.ir.PageSetupDesign;
 import org.eclipse.birt.report.engine.layout.ILayoutManager;
 import org.w3c.dom.css.CSSValue;
 
 public abstract class HTMLAbstractLM implements ILayoutManager
 {
 
 	protected final static int LAYOUT_MANAGER_UNKNOW = -1;
 	protected final static int LAYOUT_MANAGER_LEAF = 0;
 	protected final static int LAYOUT_MANAGER_BLOCK = 1;
 	protected final static int LAYOUT_MANAGER_PAGE = 2;
 	protected final static int LAYOUT_MANAGER_TABLE = 3;
 	protected final static int LAYOUT_MANAGER_TABLE_BAND = 4;
 	protected final static int LAYOUT_MANAGER_ROW = 5;
 	protected final static int LAYOUT_MANAGER_LIST = 6;
 	protected final static int LAYOUT_MANAGER_LIST_BAND = 7;
 	protected final static int LAYOUT_MANAGER_GROUP = 8;
 
 	// identy the status of layout manager
 	protected final static int STATUS_INTIALIZE = 0;
 	protected final static int STATUS_START = 1;
 	protected final static int STATUS_INPROGRESS = 2;
 	protected final static int STATUS_END = 3;
 	protected final static int STATUS_END_WITH_PAGE_BREAK = 4;
 	
 	protected static Logger logger = Logger.getLogger( HTMLAbstractLM.class
 			.getName( ) );
 
 	protected HTMLReportLayoutEngine engine;
 
 	protected HTMLLayoutContext context;
 
 	protected HTMLAbstractLM parent;
 
 	protected IContent content;
 
 	protected IReportItemExecutor executor;
 
 	protected IContentEmitter emitter;
 
 	protected int status = STATUS_INTIALIZE;
 
 	protected boolean isOutput = false;
 	
 	public HTMLAbstractLM( HTMLLayoutManagerFactory factory )
 	{
 		this.engine = factory.getLayoutEngine( );
 		this.context = engine.getContext( );
 	}
 
 	public int getType( )
 	{
 		return LAYOUT_MANAGER_UNKNOW;
 	}
 
 	public void initialize( HTMLAbstractLM parent, IContent content,
 			IReportItemExecutor executor, IContentEmitter emitter )
 	{
 		this.parent = parent;
 		this.content = content;
 		this.executor = executor;
 		this.emitter = emitter;
 		status = STATUS_INTIALIZE;
 	}
 
 	public HTMLAbstractLM getParent( )
 	{
 		return parent;
 	}
 
 	/**
 	 * output the content.
 	 */
 	protected abstract void start( );
 
 	/**
 	 * output the content.
 	 */
 	protected void startContent( )
 	{
 		if ( parent != null && !parent.isOutput )
 		{
 			parent.startContent( );
 		}
 		if ( emitter != null )
 		{
 			ContentEmitterUtil.startContent( content, emitter );
 		}
 		isOutput = true;
 	}
 
 	/**
 	 * end output the content
 	 */
 	protected void end( )
 	{
 		if ( emitter != null )
 		{
 			ContentEmitterUtil.endContent( content, emitter );
 		}
 	}
 
 	/**
 	 * layout the content and its children.
 	 * 
 	 * It can be called in three status: 1. start, the first time it is called,
 	 * in this status, it first check if it need page-break-before,
 	 * 
 	 * 2. inprogress, the second or more time it is called. In this status, it
 	 * tries to layout the content and its children to the current page.
 	 * 
 	 * 3. end, the last time it is called. In this status, it means all the
 	 * content has been layout, it is the time to handle the page-break-after.
 	 */
 	public boolean layout( )
 	{
 		switch ( status )
 		{
 			case STATUS_INTIALIZE:
 				// this element is in-visible, just as it doesn't exits.
 				// we must tranverse all its children (to let the generate
 				// engine create all the content).
 				if ( handleVisibility( ) )
 				{
 					status = STATUS_END;
 					return false;
 				}
 				// we need put it in the new page or there is no
 				// space for the content.
 				if ( isPageBreakBefore( ) )
 				{
 					status = STATUS_START;
 					context.endContentWithPageBreak( null );
 					return true;
 				}				
 			case STATUS_START :
 				//it is the first time we handle the content
 				context.startContent(content);
 			case STATUS_INPROGRESS :
 				if (status == STATUS_INPROGRESS)
 				{
 					context.continueContent(content);
 				}
 				start( );
 				boolean hasNext = layoutChildren( );
 				end( );
 				
 				if (isChildrenFinished())
 				{
 					status = STATUS_END;
 				}
 				else
 				{
 					status = STATUS_INPROGRESS;
 				}
 				if ( hasNext )
 				{
 					// there are sill some content to output,
 					// return to caller to creat the new page.
 					context.endContentWithPageBreak(content);
 					return true;
 				}
 				context.endContent(content);
 				// We need create an extra page for the following elements, so
 				// return true for next element.
 				if ( isPageBreakAfter( ) )
 				{
 					return true;
 				}
 				return false;
 		}
 		return false;
 	}
 
 	protected abstract boolean layoutChildren( );
 	
 	protected abstract boolean isChildrenFinished();
 
 	public boolean isFinished( )
 	{
 		return status == STATUS_END;
 	}
 
 	protected IContentEmitter getEmitter( )
 	{
 		return this.emitter;
 	}
 
 	protected boolean isPageBreakBefore( )
 	{
 		if ( canPageBreak( ) )
 		{
 			return needPageBreakBefore( );
 		}
 		return false;
 	}
 
 	protected boolean isPageBreakAfter( )
 	{
 		if ( canPageBreak( ) )
 		{
 			return needPageBreakAfter( );
 		}
 		return false;
 	}
 
 	protected boolean allowPageBreak( )
 	{
 		return true;
 	}
 
 	
 	protected boolean canPageBreak( )
 	{
 		//if the context disable the page-break, return directly
 		if ( !context.allowPageBreak( ) )
 		{
 			return false;
 		}
 
 		//test if it allow page break.
 		if ( !allowPageBreak( ) )
 		{
 			return false;
 		}
 		
 		//then test it's parent
 		HTMLAbstractLM p = parent;
 		while ( p != null )
 		{
 			if ( !p.allowPageBreak( ) )
 			{
 				return false;
 			}
 			p = p.getParent( );
 		}
 		return true;
 	}
 
 	protected boolean needPageBreakBefore( )
 	{
 		if ( content == null )
 		{
 			return false;
 		}
 		boolean ret = hasMasterPageChanged( );
 
 		IStyle style = content.getStyle( );
 		CSSValue pageBreak = style.getProperty( IStyle.STYLE_PAGE_BREAK_BEFORE );
 		if ( IStyle.ALWAYS_VALUE.equals( pageBreak )
 				|| IStyle.RIGHT_VALUE.equals( pageBreak )
 				|| IStyle.LEFT_VALUE.equals( pageBreak )
 				|| IStyle.SOFT_VALUE.equals( pageBreak ) )
 		{
 			//style.setProperty( IStyle.STYLE_PAGE_BREAK_BEFORE, IStyle.AUTO_VALUE );
 			return true;
 		}
 
 		return ret;
 	}
 
 	protected boolean needPageBreakAfter( )
 	{
 		if ( content == null )
 		{
 			return false;
 		}
 		IStyle style = content.getStyle( );
 		CSSValue pageBreak = style.getProperty( IStyle.STYLE_PAGE_BREAK_AFTER);
 		if ( IStyle.ALWAYS_VALUE.equals( pageBreak )
 				|| IStyle.RIGHT_VALUE.equals( pageBreak )
 				|| IStyle.LEFT_VALUE.equals( pageBreak ))
 		{
 			//style.setProperty( IStyle.STYLE_PAGE_BREAK_BEFORE, IStyle.AUTO_VALUE );
 			return true;
 		}
 		return false;
 	}
 
 	protected boolean hasMasterPageChanged( )
 	{
 		if ( content == null )
 		{
 			return false;
 		}
 		IStyle style = content.getStyle( );
 		if ( style == null )
 		{
 			return false;
 		}
 		String newMasterPage = style.getMasterPage( );
 		if ( newMasterPage == null || "".equals( newMasterPage ) ) //$NON-NLS-1$
 		{
 			return false;
 		}
 		String masterPage = context.getMasterPage( );
 		if ( !newMasterPage.equalsIgnoreCase( masterPage ) )
 		{
 			// check if this master exist
 			PageSetupDesign pageSetup = content.getReportContent( ).getDesign( )
 					.getPageSetup( );
 			if ( pageSetup.getMasterPageCount( ) > 0 )
 			{
 				MasterPageDesign masterPageDesign = pageSetup
 						.findMasterPage( newMasterPage );
 				if ( masterPageDesign != null )
 				{
 					context.setMasterPage( newMasterPage );
 					return true;
 				}
 			}
 		}
 		return false;
 	}
 
 	protected MasterPageDesign getMasterPage( IReportContent report )
 	{
 		String masterPage = context.getMasterPage( );
 		MasterPageDesign pageDesign = null;
 		if ( masterPage != null && !"".equals( masterPage ) ) //$NON-NLS-1$
 		{
 			pageDesign = report.getDesign( ).findMasterPage( masterPage );
 			if ( pageDesign != null )
 			{
 				return pageDesign;
 			}
 		}
 		return getDefaultMasterPage( report );
 	}
 
 	private MasterPageDesign getDefaultMasterPage( IReportContent report )
 	{
 		PageSetupDesign pageSetup = report.getDesign( ).getPageSetup( );
 		int pageCount = pageSetup.getMasterPageCount( );
 		if ( pageCount > 0 )
 		{
 			MasterPageDesign pageDesign =  pageSetup.getMasterPage( 0 );
 			context.setMasterPage( pageDesign.getName( ) );
 			return pageDesign;
 		}
 		return null;
 	}
 
 	/**
 	 * if the content is hidden
 	 * 
 	 * @return
 	 */
 	private boolean isHidden( IContent content )
 	{
 		assert content != null;
 		IStyle style = content.getStyle( );
 		String formats = style.getVisibleFormat( );
 		if ( formats != null
 				&& ( formats.indexOf( EngineIRConstants.FORMAT_TYPE_VIEWER ) >= 0 || formats
						.indexOf( BIRTConstants.BIRT_ALL_VALUE ) >= 0 ) )
 		{
 			return true;
 		}
 		if ( content.getContentType( ) == IContent.CELL_CONTENT )
 		{
 			ICellContent cell = (ICellContent) content;
 			IColumn column = cell.getColumnInstance( );
 			if ( column != null )
 			{
 				formats = column.getVisibleFormat( );
 				if ( formats != null
 						&& ( formats
 								.indexOf( EngineIRConstants.FORMAT_TYPE_VIEWER ) >= 0 || formats
 								.indexOf( BIRTConstants.BIRT_ALL_VALUE ) >= 0 ) )
 				{
 					return true;
 				}
 			}
 		}
 		return false;
 	}
 
 	protected boolean handleVisibility( )
 	{
 		assert content != null;
 		assert executor != null;
 		if ( isHidden( content ) )
 		{
 			traverse( executor );
 			return true;
 		}
 		return false;
 	}
 
 	/**
 	 * execute the executor, drip all its children contents.
 	 * 
 	 * @param executor
 	 */
 	private void traverse( IReportItemExecutor executor )
 	{
 		assert executor != null;
 		while ( executor.hasNextChild( ) )
 		{
 			IReportItemExecutor child = (IReportItemExecutor) executor
 					.getNextChild( );
 			if ( child != null )
 			{
 				child.execute( );
 				traverse( child );
 				child.close( );
 			}
 		}
 	}
 
 	/**
 	 * execute the report and add all its contents into the content.
 	 * 
 	 * @param content
 	 * @param executor
 	 */
 	protected void execute( IContent content, IReportItemExecutor executor )
 	{
 		assert executor != null;
 		assert content != null;
 
 		while ( executor.hasNextChild( ) )
 		{
 			IReportItemExecutor childExecutor = executor.getNextChild( );
 			if ( childExecutor != null )
 			{
 				IContent childContent = childExecutor.execute( );
 				if ( childContent != null )
 				{
 					if ( !content.getChildren( ).contains( childContent ) )
 					{
 						content.getChildren( ).add( childContent );
 					}
 				}
 				execute( childContent, childExecutor );
 				childExecutor.close( );
 			}
 		}
 	}
 
 	public void close( )
 	{
 
 	}
 
 	public void cancel( )
 	{
 		status = STATUS_END;
 	}
 }
