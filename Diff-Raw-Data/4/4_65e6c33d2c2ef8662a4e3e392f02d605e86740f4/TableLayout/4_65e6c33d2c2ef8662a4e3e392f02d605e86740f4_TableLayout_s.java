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
 
 package org.eclipse.birt.report.engine.layout.pdf.emitter;
 
 import java.util.ArrayList;
 import java.util.Iterator;
 
 import org.eclipse.birt.report.engine.content.IBandContent;
 import org.eclipse.birt.report.engine.content.ICellContent;
 import org.eclipse.birt.report.engine.content.IColumn;
 import org.eclipse.birt.report.engine.content.IContent;
 import org.eclipse.birt.report.engine.content.ILabelContent;
 import org.eclipse.birt.report.engine.content.IReportContent;
 import org.eclipse.birt.report.engine.content.IRowContent;
 import org.eclipse.birt.report.engine.content.IStyle;
 import org.eclipse.birt.report.engine.content.ITableBandContent;
 import org.eclipse.birt.report.engine.content.ITableContent;
 import org.eclipse.birt.report.engine.css.engine.StyleConstants;
 import org.eclipse.birt.report.engine.ir.DimensionType;
 import org.eclipse.birt.report.engine.ir.EngineIRConstants;
 import org.eclipse.birt.report.engine.layout.area.impl.AbstractArea;
 import org.eclipse.birt.report.engine.layout.area.impl.AreaFactory;
 import org.eclipse.birt.report.engine.layout.area.impl.CellArea;
 import org.eclipse.birt.report.engine.layout.area.impl.RowArea;
 import org.eclipse.birt.report.engine.layout.area.impl.TableArea;
 import org.eclipse.birt.report.engine.layout.pdf.util.PropertyUtil;
 
 
 public class TableLayout extends RepeatableLayout
 {
 
 	
 	
 	/**
 	 * table content
 	 */
 	private ITableContent tableContent;
 
 
 	/**
 	 * number of table column
 	 */
 	protected int columnNumber;
 	
 	/**
 	 * the first visible column id of the table.
 	 */
 	protected int startCol = -1;
 	
 	/**
 	 * the last visible column id of the table.
 	 */
 	protected int endCol = -1;
 
 	/**
 	 * table width
 	 */
 	protected int tableWidth;
 
 	protected TableLayoutInfo layoutInfo = null;
 
 	protected TableContext tableContext = null;
 	
 	protected ColumnWidthResolver columnWidthResolver;
 
 	
 	protected TableAreaLayout regionLayout = null;
 	
 	public TableLayout( LayoutEngineContext context,
 			ContainerLayout parent, IContent content )
 	{
 		super( context, parent, content );
 		tableContent = (ITableContent) content;
 		columnWidthResolver = new ColumnWidthResolver( tableContent );
 		columnNumber = tableContent.getColumnCount( );
 		boolean isBlock = !PropertyUtil.isInlineElement(content);
 		isInBlockStacking &= isBlock;
 	}
 
 	protected void createRoot( )
 	{
 		currentContext.root = AreaFactory.createTableArea( (ITableContent) content );
 		currentContext.root.setWidth( tableWidth );
 	}
 
 	public TableLayoutInfo getLayoutInfo( )
 	{
 		return layoutInfo;
 	}
 
 	protected void buildTableLayoutInfo( )
 	{
 		this.layoutInfo = resolveTableFixedLayout((TableArea)currentContext.root );
 
 	}
 	
 	public int getColumnCount()
 	{
 		if(tableContent!=null)
 		{
 			return tableContent.getColumnCount();
 		}
 		return 0;
 	}
 
 	protected void initialize( )
 	{
 		currentContext = new TableContext( );
 		contextList.add( currentContext );
 		tableContext = (TableContext)currentContext;
 		createRoot( );
 		buildTableLayoutInfo( );
 		currentContext.root.setWidth( layoutInfo.getTableWidth( ) );
 		currentContext.maxAvaWidth = layoutInfo.getTableWidth( );
 
 		if ( parent != null )
 		{
 			currentContext.root.setAllocatedHeight( parent.getCurrentMaxContentHeight( ) );
 		}
 		else
 		{
 			currentContext.root.setAllocatedHeight( context.getMaxHeight( ) );
 		}
 		if ( tableContext.layout == null )
 		{
 			int start = 0;
 			int end = tableContent.getColumnCount( ) -1;
 			tableContext.layout = new TableAreaLayout( tableContent, layoutInfo, start,
 					end );
 			//layout.initTableLayout( context.getUnresolvedRowHint( tableContent ) );
 		}
 		currentContext.maxAvaHeight = currentContext.root.getContentHeight( ) - getBottomBorderWidth( );
 		repeatHeader();
 		addCaption( tableContent.getCaption( ) );
 	}
 
 	protected void closeLayout( ContainerContext currentContext, int index, boolean finished )
 	{
 		/*
 		 * 1. resolve all unresolved cell 2. resolve table bottom border 3.
 		 * update height of Root area 4. update the status of TableAreaLayout
 		 */
 		TableContext tableContext = (TableContext)currentContext;
 		int borderHeight = 0;
 		if ( tableContext.layout != null )
 		{
 			int height = tableContext.layout.resolveAll( );
 			if ( 0 != height)
 			{
 				currentContext.currentBP = currentContext.currentBP + height;
 			}
 			borderHeight = tableContext.layout.resolveBottomBorder( );
 			tableContext.layout.remove( (TableArea) currentContext.root );
 		}
 		currentContext.root.setHeight( currentContext.currentBP + getOffsetY( ) + borderHeight );
 		parent.addToRoot( currentContext.root, index );
 	}
 
 	private int getBottomBorderWidth( )
 	{
 		IStyle style = currentContext.root.getContent( ).getComputedStyle( );
 		int borderHeight = PropertyUtil.getDimensionValue( style
 				.getProperty( StyleConstants.STYLE_BORDER_BOTTOM_WIDTH ) );
 		return borderHeight;
 	}
 
 	public int getColumnNumber( )
 	{
 		return columnNumber;
 	}
 
 	/**
 	 * resolve cell border conflict
 	 * 
 	 * @param cellArea
 	 */
 	public void resolveBorderConflict( CellArea cellArea, boolean isFirst )
 	{
 		if ( tableContext.layout != null )
 		{
 			tableContext.layout.resolveBorderConflict( cellArea, isFirst );
 		}
 	}
 
 	private class ColumnWidthResolver
 	{
 		
 		ITableContent table;
 
 		public ColumnWidthResolver( ITableContent table )
 		{
 			this.table = table;
 		}
 		
 		protected void formalize(DimensionType[] columns, int tableWidth)
 		{
 			ArrayList percentageList = new ArrayList();
 			ArrayList unsetList = new ArrayList();
 			double total = 0.0f;
 			int fixedLength = 0;
 			for(int i=0; i<columns.length; i++)
 			{
 				if(columns[i]==null)
 				{
 					unsetList.add(new Integer(i));
 				}
 				else if( EngineIRConstants.UNITS_PERCENTAGE.equals(columns[i].getUnits()))
 				{
 					percentageList.add(new Integer(i));
 					total += columns[i].getMeasure();
 				}
 				else if( EngineIRConstants.UNITS_EM.equals(columns[i].getUnits())
 						||EngineIRConstants.UNITS_EX.equals(columns[i].getUnits()) )
 				{
 					int len = TableLayout.this.getDimensionValue(columns[i], 
 							PropertyUtil.getDimensionValue( table.getComputedStyle().getProperty( StyleConstants.STYLE_FONT_SIZE ) ) );
 					fixedLength += len;
 				}
 				else
 				{
 					int len = TableLayout.this.getDimensionValue(columns[i], tableWidth);
 					fixedLength += len;
 				}
 			}
 			
 			if(fixedLength>=tableWidth)
 			{
 				for(int i=0; i<unsetList.size(); i++)
 				{
 					Integer index = (Integer)unsetList.get(i);
 					columns[index.intValue()] = new DimensionType(0d, EngineIRConstants.UNITS_PT);
 				}
 				for(int i=0; i<percentageList.size(); i++)
 				{
 					Integer index = (Integer)percentageList.get(i);
 					columns[index.intValue()] = new DimensionType(0d, EngineIRConstants.UNITS_PT);
 				}
 			}
 			else
 			{
 				float leftPercentage = (((float)(tableWidth - fixedLength)) /tableWidth)*100.0f;
 				if(unsetList.isEmpty())
 				{
 					double ratio = leftPercentage/total;
 					for(int i=0; i<percentageList.size(); i++)
 					{
 						Integer index = (Integer)percentageList.get(i);
 						columns[index.intValue()] = new DimensionType(columns[index
 								.intValue()].getMeasure()
 								* ratio, columns[index.intValue()].getUnits());
 					}
 				}
 				else
 				{
 					
 					if(total<leftPercentage)
 					{
 						double delta = leftPercentage - total;
 						for(int i=0; i<unsetList.size(); i++)
 						{
 							Integer index = (Integer)unsetList.get(i);
 							columns[index.intValue()] = new DimensionType(delta
 									/ (double) unsetList.size(),
 									EngineIRConstants.UNITS_PERCENTAGE);
 						}
 					}
 					else
 					{
 						double ratio = leftPercentage/total;
 						for(int i=0; i<unsetList.size(); i++)
 						{
 							Integer index = (Integer)unsetList.get(i);
 							columns[index.intValue()] = new DimensionType(0d, EngineIRConstants.UNITS_PT);
 						}
 						for(int i=0; i<percentageList.size(); i++)
 						{
 							Integer index = (Integer)percentageList.get(i);
 							columns[index.intValue()] = new DimensionType(columns[index
 									.intValue()].getMeasure()
 									* ratio, columns[index.intValue()].getUnits());
 						}
 					}
 				}
 			}
 		}
 		
 		protected int[] resolve(int tableWidth, DimensionType[] columns)
 		{
 			int[] cols = new int[columns.length];
 			int total = 0;
 			for(int i=0; i<columns.length; i++)
 			{
 				if(!EngineIRConstants.UNITS_PERCENTAGE.equals(columns[i].getUnits()))
 				{
 					if( EngineIRConstants.UNITS_EM.equals(columns[i].getUnits())
 							||EngineIRConstants.UNITS_EX.equals(columns[i].getUnits()) )
 					{
 						cols[i]= TableLayout.this.getDimensionValue(columns[i], 
 								PropertyUtil.getDimensionValue( table.getComputedStyle().getProperty( StyleConstants.STYLE_FONT_SIZE ) ) );
 					}
 					else
 					{
 						cols[i] = TableLayout.this.getDimensionValue(columns[i], tableWidth);
 					}
 					total += cols[i];
 				}
 			}
 			
 			if(total > tableWidth)
 			{
 				for(int i=0; i<columns.length; i++)
 				{
 					if(EngineIRConstants.UNITS_PERCENTAGE.equals(columns[i].getUnits()))
 					{
 						cols[i] = 0;
 					}
 				}
 			}
 			else
 			{
 				int delta = tableWidth - total;
 				boolean hasPercentage = false;
 				for(int i=0; i<columns.length; i++)
 				{
 					if(EngineIRConstants.UNITS_PERCENTAGE.equals(columns[i].getUnits()))
 					{
 						cols[i] = (int)(tableWidth * columns[i].getMeasure()/100.0d);
 						hasPercentage = true;
 					}
 				}
 				if(!hasPercentage)
 				{
 					int size = 0;
 					for(int i=0; i<columns.length; i++)
 					{
 						if(cols[i]>0)
 						{
 							size++;
 						}
 					}
 					for(int i=0; i<columns.length; i++)
 					{
 						if(cols[i]>0)
 						{
 							cols[i] += delta/size;
 						}
 					}
 				}
 			}
 			return cols;
 		}
 		
 		public int[] resolveFixedLayout(int maxWidth)
 		{
 		
 			int columnNumber = table.getColumnCount( );
 			DimensionType[] columns = new DimensionType[columnNumber];
 			
 			//handle visibility
 			for(int i=0; i<columnNumber; i++)
 			{
 				IColumn column = table.getColumn( i );
 				DimensionType w = column.getWidth();
 				if ( startCol < 0 )
 				{
 					startCol = i;
 				}
 				endCol = i;
 				if(w==null)
 				{
 					columns[i] = null;
 				}
 				else
 				{
 					columns[i] = new DimensionType(w.getMeasure(), w.getUnits());
 					
 				}
 			}
 			if ( startCol < 0 )
 				startCol = 0;
 			if ( endCol < 0 )
 				endCol = 0;
 			
 			int specifiedWidth = getDimensionValue( tableContent.getWidth( ), maxWidth );
 			int tableWidth;
 			if(specifiedWidth>0)
 			{
 				tableWidth = specifiedWidth;
 			}
 			else
 			{
 				tableWidth = maxWidth;
 			}
 			formalize(columns, tableWidth);
 			return resolve(tableWidth, columns);
 		}
 		
 
 
 		public int[] resolve( int specifiedWidth, int maxWidth )
 		{
 			assert ( specifiedWidth <= maxWidth );
 			int columnNumber = table.getColumnCount( );
 			int[] columns = new int[columnNumber];
 			int columnWithWidth = 0;
 			int colSum = 0;
 
 			for ( int j = 0; j < table.getColumnCount( ); j++ )
 			{
 				IColumn column = table.getColumn( j );
 				int columnWidth = getDimensionValue( column.getWidth( ),
 						tableWidth );
 				if ( columnWidth > 0 )
 				{
 					columns[j] = columnWidth;
 					colSum += columnWidth;
 					columnWithWidth++;
 				}
 				else
 				{
 					columns[j] = -1;
 				}
 			}
 
 			if ( columnWithWidth == columnNumber )
 			{
 				if ( colSum <= maxWidth )
 				{
 					return columns;
 				}
 				else
 				{
 					float delta = colSum - maxWidth;
 					for ( int i = 0; i < columnNumber; i++ )
 					{
 						columns[i] -= (int) ( delta * columns[i] / colSum );
 					}
 					return columns;
 				}
 			}
 			else
 			{
 				if ( specifiedWidth == 0 )
 				{
 					if ( colSum < maxWidth )
 					{
 						distributeLeftWidth( columns, ( maxWidth - colSum )
 								/ ( columnNumber - columnWithWidth ) );
 					}
 					else
 					{
 						redistributeWidth( columns, colSum - maxWidth
 								+ ( columnNumber - columnWithWidth ) * maxWidth
 								/ columnNumber, maxWidth, colSum );
 					}
 				}
 				else
 				{
 					if ( colSum < specifiedWidth )
 					{
 						distributeLeftWidth( columns,
 								( specifiedWidth - colSum )
 										/ ( columnNumber - columnWithWidth ) );
 					}
 					else
 					{
 						if ( colSum < maxWidth )
 						{
 							distributeLeftWidth( columns, ( maxWidth - colSum )
 									/ ( columnNumber - columnWithWidth ) );
 						}
 						else
 						{
 							redistributeWidth( columns, colSum - specifiedWidth
 									+ ( columnNumber - columnWithWidth )
 									* specifiedWidth / columnNumber,
 									specifiedWidth, colSum );
 						}
 					}
 
 				}
 
 			}
 			return columns;
 		}
 
 		private void redistributeWidth( int cols[], int delta, int sum,
 				int currentSum )
 		{
 			int avaWidth = sum / cols.length;
 			for ( int i = 0; i < cols.length; i++ )
 			{
 				if ( cols[i] < 0 )
 				{
 					cols[i] = avaWidth;
 				}
 				else
 				{
 					cols[i] -= (int) ( ( (float) cols[i] ) * delta / currentSum );
 				}
 			}
 
 		}
 
 		private void distributeLeftWidth( int cols[], int avaWidth )
 		{
 			for ( int i = 0; i < cols.length; i++ )
 			{
 				if ( cols[i] < 0 )
 				{
 					cols[i] = avaWidth;
 				}
 			}
 		}
 	}
 
 
 
 	
 	private TableLayoutInfo resolveTableFixedLayout(TableArea area)
 	{
 		assert(parent!=null);
 		int parentMaxWidth = parent.currentContext.maxAvaWidth;
 		IStyle style = area.getStyle( );
 		int marginWidth = getDimensionValue( style
 				.getProperty( StyleConstants.STYLE_MARGIN_LEFT ) )
 				+ getDimensionValue( style
 						.getProperty( StyleConstants.STYLE_MARGIN_RIGHT ) );
 
 		return new TableLayoutInfo(
 				 columnWidthResolver.resolveFixedLayout(
 						parentMaxWidth - marginWidth )  );
 	}
 	
 
 
 	private TableLayoutInfo resolveTableLayoutInfo( TableArea area )
 	{
 		assert ( parent != null );
 		int avaWidth = parent.getCurrentMaxContentWidth( )
 				- parent.currentContext.currentIP;
 		int parentMaxWidth = parent.getCurrentMaxContentWidth( );
 		IStyle style = area.getStyle( );
 		int marginWidth = getDimensionValue( style
 				.getProperty( StyleConstants.STYLE_MARGIN_LEFT ) )
 				+ getDimensionValue( style
 						.getProperty( StyleConstants.STYLE_MARGIN_RIGHT ) );
 		int specifiedWidth = getDimensionValue( tableContent.getWidth( ),
 				parentMaxWidth );
 		if ( specifiedWidth + marginWidth > parentMaxWidth )
 		{
 			specifiedWidth = 0;
 		}
 
 		boolean isInline = PropertyUtil.isInlineElement( content );
 		if ( specifiedWidth == 0 )
 		{
 			if ( isInline )
 			{
 				if ( avaWidth - marginWidth > parentMaxWidth / 4 )
 				{
 					tableWidth = avaWidth - marginWidth;
 				}
 				else
 				{
 					tableWidth = parentMaxWidth - marginWidth;
 				}
 			}
 			else
 			{
 				tableWidth = avaWidth - marginWidth;
 			}
 			return new TableLayoutInfo(
 					 columnWidthResolver.resolve(
 							tableWidth, tableWidth ) ) ;
 		}
 		else
 		{
 			if ( !isInline )
 			{
 				tableWidth = Math.min( specifiedWidth, avaWidth - marginWidth );
 				return new TableLayoutInfo(
 						columnWidthResolver.resolve(
 								tableWidth, avaWidth - marginWidth ) ) ;
 			}
 			else
 			{
 				tableWidth = Math.min( specifiedWidth, parentMaxWidth
 						- marginWidth );
 				return new TableLayoutInfo(
 						 columnWidthResolver.resolve(
 								tableWidth, parentMaxWidth - marginWidth ) ) ;
 			}
 		}
 	}
 
 
 	/**
 	 * update row height
 	 * 
 	 * @param row
 	 */
 	public void updateRow( RowArea row, int specifiedHeight, int index )
 	{
 		
 		
 		tableContext = (TableContext)contextList.get(index);
 		
 		if ( tableContext.layout != null )
 		{
 			tableContext.layout.updateRow( row, specifiedHeight );
 		}
 	}
 
 	public void addRow( RowArea row, int index )
 	{
 		tableContext = (TableContext)contextList.get(index);
 		if ( tableContext.layout != null )
 		{
 			tableContext.layout.addRow( row );
 		}
 	}
 
 	public int getXPos( int columnID )
 	{
 		if ( layoutInfo != null )
 		{
 			return layoutInfo.getXPosition( columnID );
 		}
 		return 0;
 	}
 
 	public int getCellWidth( int startColumn, int endColumn )
 	{
 		if ( layoutInfo != null )
 		{
 			return layoutInfo.getCellWidth( startColumn, endColumn );
 		}
 		return 0;
 	}
 	
 	public TableRegionLayout getTableRegionLayout()
 	{
 		if(regionLayout==null)
 		{
 			regionLayout = new TableAreaLayout( tableContent, layoutInfo, startCol,
 					endCol );
 		}
 		return  new TableRegionLayout(context, tableContent, layoutInfo, regionLayout);
 		
 	}
 	
 	protected IContent generateCaptionRow(String caption)
 	{
 		IReportContent report = tableContent.getReportContent( );
 		ILabelContent captionLabel = report.createLabelContent( );
 		captionLabel.setText( caption );
 		captionLabel.getStyle( ).setProperty( IStyle.STYLE_TEXT_ALIGN,
 				IStyle.CENTER_VALUE );
 		ICellContent cell = report.createCellContent( );
 		cell.setColSpan( tableContent.getColumnCount( ) );
 		cell.setRowSpan( 1 );
 		cell.setColumn( 0 );
 		cell.getStyle( ).setProperty( IStyle.STYLE_BORDER_TOP_STYLE,
 				IStyle.HIDDEN_VALUE );
 		cell.getStyle( ).setProperty( IStyle.STYLE_BORDER_BOTTOM_STYLE,
 				IStyle.HIDDEN_VALUE );
 		cell.getStyle( ).setProperty( IStyle.STYLE_BORDER_LEFT_STYLE,
 				IStyle.HIDDEN_VALUE );
 		cell.getStyle( ).setProperty( IStyle.STYLE_BORDER_RIGHT_STYLE,
 				IStyle.HIDDEN_VALUE );
 		captionLabel.setParent( cell );
 		cell.getChildren( ).add( captionLabel );
 		IRowContent row = report.createRowContent( );
 		row.getChildren( ).add( cell );
 		cell.setParent( row );
 		row.setParent( tableContent );
 		return row;
 	}
 	
 	protected void repeatHeader()
 	{
 		if ( bandStatus == IBandContent.BAND_HEADER )
 		{
 			return;
 		}
 		ITableBandContent header = (ITableBandContent) tableContent.getHeader( );
 		if ( !tableContent.isHeaderRepeat( ) || header == null )
 		{
 			return;
 		}
 		if ( header.getChildren( ).isEmpty( ) )
 		{
 			return;
 		}
 		
 		TableRegionLayout rLayout = getTableRegionLayout();
 		rLayout.initialize( header );
 
 		rLayout.layout( );
 		TableArea tableRegion = (TableArea) header
 				.getExtension( IContent.LAYOUT_EXTENSION );
 		if ( tableRegion != null
 				&& tableRegion.getAllocatedHeight( ) < getCurrentMaxContentHeight( ) )
 		{
 			// add to root
 			Iterator iter = tableRegion.getChildren( );
 			while ( iter.hasNext( ) )
 			{
 				AbstractArea area = (AbstractArea) iter.next( );
 				addArea( area );
 			}
 		}
 		content.setExtension( IContent.LAYOUT_EXTENSION, null );
 
 	}
 	
 	
 	protected void addCaption( String caption )
 	{
 		if ( caption == null || "".equals( caption ) ) //$NON-NLS-1$
 		{
 			return;
 		}
 		TableRegionLayout rLayout = getTableRegionLayout();
 		IContent row = generateCaptionRow(tableContent.getCaption( ));
 		rLayout.initialize( row );
 
 		rLayout.layout( );
 		TableArea tableRegion = (TableArea) row
 				.getExtension( IContent.LAYOUT_EXTENSION );
 		if ( tableRegion != null )
 		{
 			// add to root
 			Iterator iter = tableRegion.getChildren( );
 			while ( iter.hasNext( ) )
 			{
 				RowArea rowArea = (RowArea) iter.next( );
 				addArea( rowArea );
 			}
 		}
 		content.setExtension( IContent.LAYOUT_EXTENSION, null );
 	}
 
 
 	public class TableLayoutInfo
 	{
 
 		public TableLayoutInfo( int[] colWidth )
 		{
 			this.colWidth = colWidth;
 			this.columnNumber = colWidth.length;
 			this.xPositions = new int[columnNumber];
 			this.tableWidth = 0;
 
 			if ( tableContent.isRTL( ) ) // bidi_hcg
 			{
 				for ( int i = 0; i < columnNumber; i++ )
 				{
 					xPositions[i] = parent.getCurrentMaxContentWidth( ) - tableWidth
  						- colWidth[i];
 					tableWidth += colWidth[i];
 				}
 			}
 			else // ltr
 			{
 				for ( int i = 0; i < columnNumber; i++ )
 				{
 					xPositions[i] = tableWidth;
 					tableWidth += colWidth[i];
 				}
 			}
 		}
 
 		public int getTableWidth( )
 		{
 			return this.tableWidth;
 		}
 
 		public int getXPosition( int index )
 		{
 			return xPositions[index];
 		}
 
 		/**
 		 * get cell width
 		 * 
 		 * @param startColumn
 		 * @param endColumn
 		 * @return
 		 */
 		public int getCellWidth( int startColumn, int endColumn )
 		{
 			assert ( startColumn < endColumn );
 			assert ( colWidth != null );
 			int sum = 0;
 			for ( int i = startColumn; i < endColumn; i++ )
 			{
 				sum += colWidth[i];
 			}
 			return sum;
 		}
 
 		protected int columnNumber;
 
 		protected int tableWidth;
 		/**
 		 * Array of column width
 		 */
 		protected int[] colWidth = null;
 
 		/**
 		 * array of position for each column
 		 */
 		protected int[] xPositions = null;
 
 	}
 
 
 	public boolean addArea( AbstractArea area )
 	{
 		return super.addArea( area );
 	}
 	
 	class TableContext extends ContainerContext
 	{
 		TableAreaLayout layout;
 
 	}
 
 }
