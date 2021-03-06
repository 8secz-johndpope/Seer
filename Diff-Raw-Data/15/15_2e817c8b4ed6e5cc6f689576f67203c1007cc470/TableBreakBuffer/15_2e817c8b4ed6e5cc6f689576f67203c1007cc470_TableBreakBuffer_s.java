 /***********************************************************************
  * Copyright (c) 2008 Actuate Corporation.
  * All rights reserved. This program and the accompanying materials
  * are made available under the terms of the Eclipse Public License v1.0
  * which accompanies this distribution, and is available at
  * http://www.eclipse.org/legal/epl-v10.html
  *
  * Contributors:
  * Actuate Corporation - initial API and implementation
  ***********************************************************************/
 
 package org.eclipse.birt.report.engine.layout.html.buffer;
 
 import java.util.ArrayList;
 import java.util.Iterator;
 import java.util.List;
 
 import org.eclipse.birt.report.engine.content.ICellContent;
 import org.eclipse.birt.report.engine.content.IColumn;
 import org.eclipse.birt.report.engine.content.IContent;
 import org.eclipse.birt.report.engine.content.IStyle;
 import org.eclipse.birt.report.engine.content.ITableContent;
 import org.eclipse.birt.report.engine.emitter.IContentEmitter;
 import org.eclipse.birt.report.engine.layout.html.HTMLLayoutContext;
 import org.eclipse.birt.report.engine.presentation.TableColumnHint;
 import org.w3c.dom.css.CSSValue;
 
 public class TableBreakBuffer implements IPageBuffer
 {
 
 	IPageBuffer currentBuffer = null;
 	IPageBuffer[] buffers = null;
 	HTMLLayoutContext context;
 	int nestCount = 0;
 	int currentTableIndex = -1;
 	int[] pageBreakIndexs;
 	int currentIndex = 0;
 	boolean hasRepeatedColumn = true;
 	int repeatStart = 0;
 	int repeatEnd = 0;
 	boolean isRepeatStatus = false;
 	ArrayList<ContentEvent> repeatEvent = new ArrayList<ContentEvent>();
 
 	public TableBreakBuffer( IPageBuffer parentBuffer, HTMLLayoutContext context )
 	{
 		if ( parentBuffer != null )
 		{
 			currentBuffer = parentBuffer;
 		}
 		else
 		{
 			currentBuffer = context.getBufferFactory( ).createBuffer( );
 		}
 		this.context = context;
 	}
 
 	public void startContainer( IContent content, boolean isFirst,
 			IContentEmitter emitter, boolean visible )
 	{
 		switch ( content.getContentType( ) )
 		{
 			case IContent.TABLE_CONTENT :
 				nestCount++;
 				ITableContent table = (ITableContent) content;
 				boolean hasPageBreak = hasPageBreak( table );
 				if ( hasPageBreak )
 				{
 					if ( currentTableIndex < 0 )
 					{
 						INode[] nodeList = currentBuffer
 								.getNodeStack( );
 						pageBreakIndexs = getPageBreakIndex( table );
 						repeatEnd = getRepeatEnd( table );
 						currentBuffer
 								.startContainer( createTable( table,
 										pageBreakIndexs, 0 ), isFirst, emitter,
 										visible );
 						currentTableIndex = nestCount;
 
 						buffers = new IPageBuffer[pageBreakIndexs.length];
 						buffers[0] = currentBuffer;
 						String tableId = table.getInstanceID( )
 								.toUniqueString( );
 						currentBuffer.addTableColumnHint( new TableColumnHint(
 								tableId, 0, pageBreakIndexs[0] + 1 ) );
 						for ( int i = 1; i < pageBreakIndexs.length; i++ )
 						{
 							buffers[i] = new TableBreakBuffer( null, context );
 							INode[] list = new INode[nodeList.length + 1];
 							ITableContent newTable = createTable( table, pageBreakIndexs, i );
 							list[0] = new ContainerBufferNode(newTable, emitter, null, true);
 							for ( int j = 0; j < nodeList.length; j++ )
 							{
 								list[j + 1] = nodeList[j];
 							}
 
 							buffers[i].openPage( list );
 							if ( hasRepeatedColumn )
 							{
 								buffers[i]
 										.addTableColumnHint( new TableColumnHint(
 												tableId, repeatStart, repeatEnd
 														- repeatStart ) );
 							}
 							buffers[i]
 									.addTableColumnHint( new TableColumnHint(
 											tableId,
 											pageBreakIndexs[i - 1] + 1,
 											pageBreakIndexs[i]
 													- pageBreakIndexs[i - 1] ) );
 						}
 					}
 					else
 					{
 						currentBuffer.startContainer( content, isFirst,
 								emitter, visible );
 					}
 				}
 				else
 				{
 					currentBuffer.startContainer( content, isFirst, emitter,
 							visible );
 				}
 				break;
 			case IContent.TABLE_GROUP_CONTENT :
 			case IContent.TABLE_BAND_CONTENT :
 			case IContent.ROW_CONTENT :
 				if ( currentTableIndex == nestCount && currentTableIndex > 0 )
 				{
 					currentIndex = 0;
 					currentBuffer = buffers[0];
 					startContainerInPages( content, isFirst, emitter, visible );
 					if(hasRepeatedColumn)
 					{
 						repeatEvent.clear();
 					}
 				}
 				else
 				{
 					currentBuffer.startContainer( content, isFirst, emitter,
 							visible );
 				}
 				break;
 			case IContent.CELL_CONTENT :
 				if ( currentTableIndex == nestCount && currentTableIndex > 0 )
 				{
 					if ( hasRepeatedColumn
 							&& isRepeatedCell( (ICellContent) content ) )
 					{
 						isRepeatStatus = true;
 					}
 					int index = getPageIndex( (ICellContent) content );
 					if ( index != currentIndex )
 					{
 						currentIndex = index;
 						repeatCells( emitter);
 					}
 					currentBuffer = buffers[currentIndex];
 				}
 				currentBuffer.startContainer( content, isFirst, emitter,
 						visible );
 				break;
 			default :
 				currentBuffer.startContainer( content, isFirst, emitter,
 						visible );
 				break;
 
 		}
 		if(isRepeatStatus)
 		{
 			repeatEvent.add(new ContentEvent(content, visible, ContentEvent.START_CONTAINER_EVENT));
 		}
 	}
 	
 	protected void repeatCells( IContentEmitter emitter )
 	{
 		int size = repeatEvent.size( );
 		if(size>1)
 		{
 			
 			ContentEvent last = repeatEvent.get( size-1 );
 			last.isFirst = false;
 			
 			Iterator iter = repeatEvent.iterator( );
 			while ( iter.hasNext( ) )
 			{
 				ContentEvent child = (ContentEvent) iter.next( );
 				visitEvent( child, emitter );
 			}
 		}
 	}
 	
 	void visitEvent( ContentEvent event, IContentEmitter emitter )
 	{
 		switch ( event.eventType )
 		{
 			
 			case ContentEvent.START_LEAF_EVENT:
 				currentBuffer.startContent( event.content, emitter, event.visible );
 				break;
 			case ContentEvent.START_CONTAINER_EVENT:
 				currentBuffer.startContainer( event.content, event.isFirst, emitter, event.visible );
 				break;
 			case ContentEvent.END_CONTAINER_EVENT:
 				currentBuffer.endContainer( event.content, event.isFirst, emitter, event.visible );
 				break;
 
 		}
 	}
 
 	protected void startContainerInPages( IContent content, boolean isFirst,
 			IContentEmitter emitter, boolean visible )
 	{
 		buffers[0].startContainer( content, isFirst, emitter, visible );
 		for ( int i = 1; i < buffers.length; i++ )
 		{
 			buffers[i].startContainer( content, false, emitter, visible );
 		}
 	}
 
 	public void startContent( IContent content, IContentEmitter emitter,
 			boolean visible )
 	{
 		currentBuffer.startContent( content, emitter, visible );
 		if(isRepeatStatus)
 		{
 			repeatEvent.add(new ContentEvent(content, visible, ContentEvent.START_LEAF_EVENT));
 		}
 	}
 
 	public void endContainer( IContent content, boolean finished,
 			IContentEmitter emitter, boolean visible )
 	{
 		switch ( content.getContentType( ) )
 		{
 			case IContent.TABLE_CONTENT :
 				// FIXME wrap the table content
 				INode[] nodeList = currentBuffer.getNodeStack( );
 
 				nestCount--;
 				if ( currentTableIndex == nestCount + 1
 						&& currentTableIndex > 0 )
 				{
 					assert ( buffers != null );
 					for ( int i = 0; i < buffers.length - 1; i++ )
 					{
 						buffers[i].closePage( nodeList );
 					}
 					buffers[buffers.length - 1].endContainer( content,
 							finished, emitter, visible );
 					context.getBufferFactory( ).refresh( );
 					currentBuffer = buffers[buffers.length - 1];
 
 					buffers = null;
 					currentTableIndex = -1;
 				}
 				else
 				{
 					currentBuffer.endContainer( content, finished, emitter,
 							visible );
 				}
 				break;
 			case IContent.TABLE_GROUP_CONTENT :
 			case IContent.TABLE_BAND_CONTENT :
 			case IContent.ROW_CONTENT :
 				if ( currentTableIndex == nestCount )
 				{
 					endContainerInPages( content, finished, emitter, visible );
 				}
 				else
 				{
 					currentBuffer.endContainer( content, finished, emitter,
 							visible );
 				}
 				break;
 			case IContent.CELL_CONTENT :
 				if ( currentTableIndex == nestCount && currentTableIndex > 0 )
 				{
 					int pageIndex = needPageBreak( (ICellContent) content );
 					if ( pageIndex >= 0 )
 					{
 						currentBuffer.endContainer( content, false, emitter,
 								visible );
 						currentBuffer = buffers[pageIndex];
 					}
 					else
 					{
 						currentBuffer.endContainer( content, finished, emitter,
 								visible );
 					}
 					if ( isRepeatStatus )
 					{
 						repeatEvent.add(new ContentEvent(content, visible, ContentEvent.END_CONTAINER_EVENT));
 						isRepeatStatus = false;
 					}
 				}
 				else
 				{
 					currentBuffer.endContainer( content, finished, emitter,
 							visible );
 				}
 				break;
 			case IContent.PAGE_CONTENT :
 				currentBuffer
 						.endContainer( content, finished, emitter, visible );
 				context.getBufferFactory( ).refresh( );
 				break;
 			default :
 				currentBuffer
 						.endContainer( content, finished, emitter, visible );
 				break;
 		}
 		
 		if(isRepeatStatus)
 		{
 			repeatEvent.add(new ContentEvent(content, visible, ContentEvent.END_CONTAINER_EVENT));
 		}
 
 	}
 
 	protected void endContainerInPages( IContent content, boolean finished,
 			IContentEmitter emitter, boolean visible )
 	{
 		if ( currentTableIndex == nestCount && currentTableIndex > 0 )
 		{
 			for ( int i = 0; i < buffers.length - 1; i++ )
 			{
 				buffers[i].endContainer( content, finished, emitter, visible );
 			}
 			buffers[buffers.length - 1].endContainer( content, finished,
 					emitter, visible );
 		}
 		else
 		{
 			currentBuffer.endContainer( content, finished, emitter, visible );
 		}
 	}
 
 	public void flush( )
 	{
 		for ( int i = 0; i < buffers.length; i++ )
 		{
 			buffers[i].flush( );
 		}
 	}
 
 	public boolean isRepeated( )
 	{
 		if ( currentBuffer != null )
 		{
 			return currentBuffer.isRepeated( );
 		}
 		if ( buffers != null )
 		{
 			return buffers[0].isRepeated( );
 		}
 		return false;
 	}
 
 	public void setRepeated( boolean isRepeated )
 	{
 		if ( currentBuffer != null )
 		{
 			currentBuffer.setRepeated( isRepeated );
 		}
 		if ( buffers != null )
 		{
 			for ( int i = 0; i < buffers.length; i++ )
 			{
 				buffers[i].setRepeated( isRepeated );
 			}
 		}
 	}
 
 	protected boolean hasPageBreak( ITableContent table )
 	{
 		int count = table.getColumnCount( );
 		for ( int i = 0; i < count; i++ )
 		{
 			IColumn column = table.getColumn( i );
 			IStyle style = column.getStyle( );
 			CSSValue pageBreak = style
 					.getProperty( IStyle.STYLE_PAGE_BREAK_BEFORE );
 			if ( i > 0 && IStyle.ALWAYS_VALUE == pageBreak )
 			{
 				return true;
 			}
 			pageBreak = style.getProperty( IStyle.STYLE_PAGE_BREAK_AFTER );
 			if ( i < count - 1 && IStyle.ALWAYS_VALUE == pageBreak )
 			{
 				return true;
 			}
 		}
 		return false;
 	}
 	
 	protected int getRepeatEnd( ITableContent table )
 	{
 		int count = table.getColumnCount( );
 		for ( int i = 0; i < count; i++ )
 		{
 			IColumn column = table.getColumn( i );
 			if(!column.isRepeated( ))
 			{
 				return i;
 			}
 		}
 		return  0;
 	}
 
 	protected int[] getPageBreakIndex( ITableContent table )
 	{
 		List<Integer> indexs = new ArrayList<Integer>( );
 		int count = table.getColumnCount( );
 		for ( int i = 0; i < count; i++ )
 		{
 			IColumn column = table.getColumn( i );
 			IStyle style = column.getStyle( );
 			CSSValue pageBreak = style
 					.getProperty( IStyle.STYLE_PAGE_BREAK_BEFORE );
 			if ( i > 0 && IStyle.ALWAYS_VALUE == pageBreak )
 			{
 				if ( !indexs.contains( i - 1 ) )
 				{
 					indexs.add( i - 1 );
 				}
 			}
 			pageBreak = style.getProperty( IStyle.STYLE_PAGE_BREAK_AFTER );
 			if ( i < count - 1 && IStyle.ALWAYS_VALUE == pageBreak )
 			{
 				if ( !indexs.contains( i ) )
 				{
 					indexs.add( i );
 				}
 			}
 		}
 		if ( !indexs.contains( count - 1 ) )
 		{
 			indexs.add( count - 1 );
 		}
 		int[] values = new int[indexs.size( )];
 		for ( int i = 0; i < indexs.size( ); i++ )
 		{
 			values[i] = indexs.get( i ).intValue( );
 		}
 		return values;
 	}
 	
 	public boolean isRepeatedCell(ICellContent cell)
 	{
 		int start = cell.getColumn();
 		int end = start + cell.getColSpan();
 		if(!(start>=repeatEnd || end<=repeatStart))
 		{
 			return true;
 		}
 		return false;
 	}
 
 	public int getPageIndex( ICellContent cell )
 	{
 		int start = cell.getColumn( );
 		int current = currentIndex;
 		if ( start > pageBreakIndexs[current] )
 		{
 			while ( start > pageBreakIndexs[current] )
 			{
 				current++;
 				if ( current == pageBreakIndexs.length )
 				{
 					current = 0;
 					break;
 				}
 			}
 		}
 		return current;
 	}
 
 	public int needPageBreak( ICellContent cell )
 	{
 		int current = currentIndex;
 		int end = cell.getColumn( ) + cell.getColSpan( );
 		if ( end > pageBreakIndexs[current] )
 		{
 			while ( pageBreakIndexs[current] < end )
 			{
 				current++;
 				if ( current == pageBreakIndexs.length )
 				{
					current = 0;
 					break;
 				}
 			}
 			return current;
 		}
 		return -1;
 
 	}
 
 	public boolean finished( )
 	{
 		return currentBuffer.finished( );
 	}
 
 	public void closePage( INode[] nodeList )
 	{
 		currentBuffer.closePage( nodeList );
 	}
 
 	public void openPage( INode[] nodeList )
 	{
 		currentBuffer.openPage( nodeList );
 	}
 
 	public INode[] getNodeStack( )
 	{
 		return currentBuffer.getNodeStack( );
 	}
 
 	protected ITableContent createTable( ITableContent table,
 			int[] pageBreakIndex, int index )
 	{
 		return table;
 		/*List columns = new ArrayList( );
 		int start = 0;
 		int end = pageBreakIndex[index];
 		if ( index != 0 )
 		{
 			start = pageBreakIndex[index - 1] + 1;
 		}
 
 		for ( int i = start; i <= end; i++ )
 		{
 			IColumn column = table.getColumn( i );
 			columns.add( column );
 		}
 		return new TableContentWrapper( table, columns );
 */	}
 
 	public void addTableColumnHint( TableColumnHint hint )
 	{
 		if ( currentBuffer != null )
 		{
 			currentBuffer.addTableColumnHint( hint );
 		}
 
 	}
 	
 	public static class ContentEvent
 	{
 		public static final int START_CONTAINER_EVENT = 0;
 		public static final int END_CONTAINER_EVENT = 1;
 		public static final int START_LEAF_EVENT = 2;
 		IContent content;
 		int eventType;
 		boolean visible;
 		boolean isFirst = true;
 		
 		public ContentEvent(IContent content, boolean visible, int eventType)
 		{
 			this.content = content;
 			this.visible = visible;
 			this.eventType = eventType;
 		}
 	}
 
 }
