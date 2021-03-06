 /***********************************************************************
  * Copyright (c) 2009 Actuate Corporation.
  * All rights reserved. This program and the accompanying materials
  * are made available under the terms of the Eclipse Public License v1.0
  * which accompanies this distribution, and is available at
  * http://www.eclipse.org/legal/epl-v10.html
  *
  * Contributors:
  * Actuate Corporation - initial API and implementation
  ***********************************************************************/
 
 package org.eclipse.birt.report.engine.nLayout.area.impl;
 
 import java.util.Iterator;
 
 import org.eclipse.birt.core.exception.BirtException;
 import org.eclipse.birt.report.engine.content.IContent;
 import org.eclipse.birt.report.engine.nLayout.LayoutContext;
 import org.eclipse.birt.report.engine.nLayout.area.IArea;
 
 public class RowArea extends ContainerArea
 {
 
 	protected transient CellArea[] cells;
 
 	protected transient TableArea table;
 
 	protected int rowID;
 
 	public RowArea( ContainerArea parent, LayoutContext context,
 			IContent content )
 	{
 		super( parent, context, content );
 		cells = new CellArea[getTable().getColumnCount( )];
 	}
 
 	RowArea( int colCount )
 	{
 		super( );
 		cells = new CellArea[colCount];
 	}
 
 	RowArea( RowArea row )
 	{
 		super( row );
 		this.rowID = row.getRowID( );
 		this.cells = new CellArea[row.getColumnCount()];
 	}
 	
 	public int getColumnCount( )
 	{
 		TableArea table = getTableArea();
 		if ( table != null )
 		{
 			return table.getColumnCount( );
 		}
 		if ( cells != null )
 		{
 			return cells.length;
 		}
 		return 0;
 	}
 
 	public void setCell( CellArea cell )
 	{
 		int col = cell.getColumnID( );
 		int colSpan = cell.getColSpan( );
 		for ( int i = col; i < col + colSpan; i++ )
 		{
 			cells[i] = cell;
 		}
 	}
 
 	public CellArea getCell( int columnID )
 	{
 		if ( columnID >= 0 && columnID < cells.length )
 		{
 			return cells[columnID];
 		}
 		return null;
 	}
 
 	public void replace( CellArea origin, CellArea dest )
 	{
 		children.remove( origin );
 		children.add( dest );
 		dest.setParent( this );
 	}
 
 	public void setRowID( int rowID )
 	{
 		this.rowID = rowID;
 	}
 
 	public int getRowID( )
 	{
 		return rowID;
 	}
 
 	public RowArea cloneArea( )
 	{
 		return new RowArea( this );
 	}
 	
 	public RowArea deepClone( )
 	{
 		RowArea result = (RowArea) cloneArea( );
 		Iterator iter = children.iterator( );
 		while ( iter.hasNext( ) )
 		{
 			CellArea child = (CellArea) iter.next( );
 			CellArea cloneChild =(CellArea) child.deepClone( );
 			result.children.add( cloneChild  );
 			cloneChild.setParent( result );
 			result.setCell( cloneChild );
 		}
 		return result;
 	}
 
 	protected TableArea getTableArea()
 	{
 		if(table==null)
 		{
 			table = getTable();
 		}
 		return table;
 	}
 	
 
 	public void close( ) throws BirtException
 	{
 		getTableArea().addRow( this );
 		if ( !isInInlineStacking && context.isAutoPageBreak( ) )
 		{
 			int aHeight = getAllocatedHeight( );
 			while ( aHeight + parent.getAbsoluteBP( ) >= context.getMaxBP( ) )
 			{
 				parent.autoPageBreak( );
 				aHeight = getAllocatedHeight( );
 			}
 		}
 		parent.update( this );
 		finished = true;
 	}
 
 	public void initialize( ) throws BirtException
 	{
 		calculateSpecifiedHeight( content );
 		width = parent.getMaxAvaWidth( );
 		
 		buildLogicContainerProperties( content, context );
 		parent.add( this );
 	}
 
 	protected boolean isRowEmpty( )
 	{
 		Iterator iter = getChildren( );
 		while ( iter.hasNext( ) )
 		{
 			ContainerArea area = (ContainerArea) iter.next( );
 			if ( area.getChildrenCount( ) > 0 )
 			{
 				return false;
 			}
 		}
 		return true;
 	}
 
 	public void update( AbstractArea area ) throws BirtException
 	{
 		CellArea cArea = (CellArea) area;
 		int columnID = cArea.getColumnID( );
 		int colSpan = cArea.getColSpan( );
 		// Retrieve direction from the top-level content.
 		if ( colSpan > 1 && content.isRTL( ) )
 		{
 			columnID += colSpan - 1;
 		}
 		cArea.setPosition( getTableArea().getXPos( columnID ), 0 );
 	}
 	
 	public  void add(AbstractArea area)
 	{
 		addChild(area);
 		CellArea cArea = (CellArea) area;
 		int columnID = cArea.getColumnID( );
 		int colSpan = cArea.getColSpan( );
 		// Retrieve direction from the top-level content.
 		if ( colSpan > 1 && content.isRTL( ) )
 		{
 			columnID += colSpan - 1;
 		}
 		cArea.setPosition( getTableArea().getXPos( columnID ), 0 );
 	}
 	
 	public void addChild( IArea area )
 	{
 		children.add( area );
 		this.setCell( ( CellArea)area);
 	}
 	
 	public SplitResult split( int height, boolean force ) throws BirtException
 	{
 		if ( force )
 		{
 			return _split( height, force );
 		}
 		else if ( isPageBreakInsideAvoid( ) )
 		{
 			if ( isPageBreakBeforeAvoid( ) )
 			{
 				return SplitResult.BEFORE_AVOID_WITH_NULL;
 			}
 			else
 			{
 				_splitSpanCell( height , force);
 				return SplitResult.SUCCEED_WITH_NULL;
 			}
 		}
 		else
 		{
 			return _split( height, force );
 		}
 	}
 	
 	protected void _splitSpanCell(int height, boolean force) throws BirtException
 	{
 		if ( cells.length != children.size( ) )
 		{
 			// split dummy cell
 			for ( int i = 0; i < cells.length; i++ )
 			{
 				if ( cells[i] instanceof DummyCell )
 				{
 					SplitResult splitCell = cells[i].split( 0, force );
 					CellArea cell = (CellArea)splitCell.getResult( );
 					if ( cell != null )
 					{
 						cell
 								.setHeight( ( (DummyCell) cells[i] )
 										.getDelta( ) );
 						CellArea org = ( (DummyCell) cells[i] ).getCell( );
 						RowArea row = (RowArea) org.getParent( );
 						row.replace( org, cell );
 						cell.setParent( row );
 					}
 					i = i + cells[i].getColSpan( ) - 1;
 				}
 			}
 		}
 	}
 
 
 	protected SplitResult _split( int height, boolean force )
 			throws BirtException
 	{
 		RowArea result = null;
 		for ( int i = 0; i < cells.length; i++ )
 		{
 			if(cells[i]!=null)
 			{
 				SplitResult splitCell = cells[i].split( height, force );
 				CellArea cell = (CellArea) splitCell.getResult( );
 	
 				if ( cell != null )
 				{
 					if ( result == null )
 					{
 						result = cloneArea( );
 					}
 					result.addChild( cell );
 					result.setCell( cell );
 				}
 				i = cells[i].getColSpan( ) + i - 1;
 			}
 		}
 		if ( result != null )
 		{
 			result.updateRow( this );
 			updateRow();
 			return new SplitResult( result, SplitResult.SPLIT_SUCCEED_WITH_PART );
 		}
 		else
 		{
 			return SplitResult.SUCCEED_WITH_NULL;
 		}
 	}
 	
 	protected void updateRow()
 	{
 		int height = 0;
 		for(int i=0; i<children.size( ); i++)
 		{
 			CellArea cell = (CellArea) children.get( i );
 			height = Math.max( height, cell.getHeight( ) );
 		}
 		this.height = height;
 		for(int i=0; i<children.size( ); i++)
 		{
 			CellArea cell = (CellArea) children.get( i );
 			cell.setHeight( height );
 			setCell(cell);
 		}
 	}
 
 	public void updateRow( RowArea original )
 	{
 		int height = 0;
 		Iterator iter = children.iterator( );
 		while ( iter.hasNext( ) )
 		{
 			CellArea cell = (CellArea) iter.next( );
 			height = Math.max( height, cell.getHeight( ) );
 		}
 		this.height = height;
 		for ( int i = 0; i < cells.length; i++ )
 		{
 			if ( cells[i] == null )
 			{
 				CellArea oCell = original.getCell( i );
 				if ( oCell!=null &&!( oCell instanceof DummyCell ) )
 				{
 					CellArea nCell = oCell.cloneArea( );
 					nCell.setHeight( height );
 					nCell.setParent( this );
 					children.add( nCell );
 					i = i + oCell.getColSpan( ) - 1;
 				}
 			}
 			else
 			{
 				cells[i].setHeight( height );
 			}
 		}
 	}
 	
 	public boolean isPageBreakInsideAvoid()
 	{
 		if( getTableArea().isGridDesign( ))
 		{
 			return super.isPageBreakInsideAvoid( );
 		}
 		else
 		{
 			return true;
 		}
 	}
 
 	public SplitResult splitLines( int lineCount ) throws BirtException
 	{
		if ( isPageBreakBeforeAvoid( ) )
		{
			return SplitResult.BEFORE_AVOID_WITH_NULL;
		}
 		return SplitResult.SUCCEED_WITH_NULL;
 	}
 
 }
