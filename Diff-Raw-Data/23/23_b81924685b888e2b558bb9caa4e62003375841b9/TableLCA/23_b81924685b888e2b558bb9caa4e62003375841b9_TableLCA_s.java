 /*******************************************************************************
  * Copyright (c) 2002, 2011 Innoopract Informationssysteme GmbH.
  * All rights reserved. This program and the accompanying materials
  * are made available under the terms of the Eclipse Public License v1.0
  * which accompanies this distribution, and is available at
  * http://www.eclipse.org/legal/epl-v10.html
  *
  * Contributors:
  *     Innoopract Informationssysteme GmbH - initial API and implementation
  *     EclipseSource - ongoing development
  ******************************************************************************/
 package org.eclipse.swt.internal.widgets.tablekit;
 
 import java.io.IOException;
 
 import javax.servlet.http.HttpServletRequest;
 
 import org.eclipse.rwt.internal.lifecycle.JSConst;
 import org.eclipse.rwt.internal.service.ContextProvider;
 import org.eclipse.rwt.internal.theme.JsonArray;
 import org.eclipse.rwt.internal.theme.JsonObject;
 import org.eclipse.rwt.internal.util.NumberFormatUtil;
 import org.eclipse.rwt.lifecycle.*;
 import org.eclipse.swt.SWT;
 import org.eclipse.swt.events.SelectionEvent;
 import org.eclipse.swt.graphics.Rectangle;
 import org.eclipse.swt.internal.events.EventLCAUtil;
 import org.eclipse.swt.internal.widgets.*;
 import org.eclipse.swt.widgets.*;
 
 
 public final class TableLCA extends AbstractWidgetLCA {
 
   // Property names to preserve values
   static final String PROP_HEADER_HEIGHT = "headerHeight";
   static final String PROP_HEADER_VISIBLE = "headerVisible";
   static final String PROP_LINES_VISIBLE = "linesVisible";
   static final String PROP_ITEM_HEIGHT = "itemHeight";
   static final String PROP_COLUMN_COUNT = "columnCount";
   static final String PROP_TOP_ITEM_INDEX = "topIndex";
   static final String PROP_SELECTION_LISTENERS = "selectionListeners";
   static final String PROP_DEFAULT_COLUMN_WIDTH = "defaultColumnWidth";
   static final String PROP_ITEM_COUNT = "itemCount";
   static final String PROP_ALWAYS_HIDE_SELECTION = "alwaysHideSelection";
   static final String PROP_HAS_H_SCROLL_BAR = "hasHScrollBar";
   static final String PROP_HAS_V_SCROLL_BAR = "hasVScrollBar";
   static final String PROP_LEFT_OFFSET = "leftOffset";
   static final String PROP_SCROLLBARS_SELECTION_LISTENER = "scrollBarsSelectionListeners";
   static final String PROP_ENABLE_CELL_TOOLTIP = "enableCellToolTip";
 
   private static final Integer DEFAULT_TOP_INDEX = new Integer( 0 );
   private static final Integer DEFAULT_ITEM_COUNT = new Integer( 0 );
   private static final Integer DEFAUT_ITEM_HEIGHT = new Integer( 0 );
   private static final Integer DEFAULT_LEFT_OFFSET = new Integer( 0 );
   private static final Integer DEFAULT_COLUMN_COUNT = new Integer( 0 );
 
   public void preserveValues( final Widget widget ) {
     Table table = ( Table )widget;
     ControlLCAUtil.preserveValues( table );
     IWidgetAdapter adapter = WidgetUtil.getAdapter( table );
     adapter.preserve( PROP_HEADER_HEIGHT, new Integer( table.getHeaderHeight() ) );
     adapter.preserve( PROP_HEADER_VISIBLE, Boolean.valueOf( table.getHeaderVisible() ) );
     adapter.preserve( PROP_LINES_VISIBLE, Boolean.valueOf( table.getLinesVisible() ) );
     adapter.preserve( PROP_ITEM_HEIGHT, new Integer( table.getItemHeight() ) );
     adapter.preserve( PROP_COLUMN_COUNT, new Integer( table.getColumnCount() ) );
     TableLCAUtil.preserveItemMetrics( table );
     adapter.preserve( PROP_ITEM_COUNT, new Integer( table.getItemCount() ) );
     adapter.preserve( PROP_TOP_ITEM_INDEX, new Integer( table.getTopIndex() ) );
     adapter.preserve( PROP_SELECTION_LISTENERS,
                       Boolean.valueOf( SelectionEvent.hasListener( table ) ) );
     TableLCAUtil.preserveFocusIndex( table );
     WidgetLCAUtil.preserveCustomVariant( table );
     adapter.preserve( PROP_ALWAYS_HIDE_SELECTION, alwaysHideSelection( table ) );
     adapter.preserve( PROP_HAS_H_SCROLL_BAR, hasHScrollBar( table ) );
     adapter.preserve( PROP_HAS_V_SCROLL_BAR, hasVScrollBar( table ) );
     adapter.preserve( PROP_LEFT_OFFSET, getLeftOffset( table ) );
     adapter.preserve( PROP_SCROLLBARS_SELECTION_LISTENER, hasScrollBarsSelectionListener( table ) );
     adapter.preserve( PROP_ENABLE_CELL_TOOLTIP,
                       new Boolean( CellToolTipUtil.isEnabledFor( table ) ) );
   }
 
   public void readData( final Widget widget ) {
     Table table = ( Table )widget;
     readTopItemIndex( table );
     readScrollLeft( table );
     readSelection( table );
     readFocusIndex( table ); // must be called *after* readSelection
     readWidgetSelected( table );
     readWidgetDefaultSelected( table );
     readCellToolTipTextRequested( table );
     ControlLCAUtil.processMouseEvents( table );
     ControlLCAUtil.processKeyEvents( table );
     ControlLCAUtil.processMenuDetect( table );
   }
 
   public void renderInitialization( final Widget widget ) throws IOException {
     Table table = ( Table )widget;
     ITableAdapter adapter = ( ITableAdapter )table.getAdapter( ITableAdapter.class );
     JSWriter writer = JSWriter.getWriterFor( table );
     JsonObject argsMap = new JsonObject();
     argsMap.append( "appearance", "table" );
     argsMap.append( "fullSelection", true );
     if( ( table.getStyle() & SWT.NO_SCROLL ) != 0 ) {
       argsMap.append( "noScroll", true );
     }
     if( ( table.getStyle() & SWT.MULTI ) != 0 ) {
       argsMap.append( "multiSelection", true );
     }
     if( ( table.getStyle() & SWT.HIDE_SELECTION ) != 0 ) {
       argsMap.append( "hideSelection", true );
     }
     if( ( table.getStyle() & SWT.VIRTUAL ) != 0 ) {
       argsMap.append( "virtual", true );
     }
     if( ( table.getStyle() & SWT.CHECK ) != 0 ) {
       int[] checkMetrics = new int[] { adapter.getCheckLeft(), adapter.getCheckWidth() };
       argsMap.append( "check", true );
       argsMap.append( "checkBoxMetrics", JsonArray.valueOf( checkMetrics ) );
     }
     argsMap.append( "indentionWidth", 0 );
     Object[] args = new Object[]{ new JSVar( argsMap.toString() ) };
     writer.newWidget( "org.eclipse.rwt.widgets.Tree", args );
     ControlLCAUtil.writeStyleFlags( table );
     writer.set( "treeColumn", -1 );
   }
 
   public void renderChanges( final Widget widget ) throws IOException {
     Table table = ( Table )widget;
     // Important: Order matters. Always call writeItemCount first. See bug 326941.
     writeItemCount( table );
     ControlLCAUtil.writeChanges( table );
     writeItemHeight( table );
     TableLCAUtil.writeItemMetrics( table );
     writeColumnCount( table );
     writeLinesVisible( table );
     writeTopIndex( table );
     writeScrollBarsVisible( table );
     writeSelectionListener( table );
     writeScrollBarsSelectionListener( table );
     writeHeaderHeight( table );
     writeHeaderVisible( table );
     writeFocusItem( table );
     writeLeftOffset( table );
     writeEnableCellToolTip( table );
     writeCellToolTipText( table );
     WidgetLCAUtil.writeCustomVariant( table );
   }
 
   public void renderDispose( final Widget widget ) throws IOException {
     JSWriter writer = JSWriter.getWriterFor( widget );
     writer.dispose();
   }
 
   public void doRedrawFake( final Control control ) {
     Table table = ( Table )control;
     Object adapter = table.getAdapter( ITableAdapter.class );
     ITableAdapter tableAdapter = ( ITableAdapter )adapter;
     tableAdapter.checkData();
   }
 
   ////////////////////////////////////////////////////
   // Helping methods to read client-side state changes
 
   private static void readSelection( final Table table ) {
     String value = WidgetLCAUtil.readPropertyValue( table, "selection" );
     if( value != null ) {
       int[] newSelection;
       if( "".equals( value ) ) {
         newSelection = new int[ 0 ];
       } else {
         String[] selectedItems = value.split( "," );
         newSelection = new int[ selectedItems.length ];
         for( int i = 0; i < selectedItems.length; i++ ) {
           TableItem item = null;
           String itemId = selectedItems[ i ];
           item = getItem( table, itemId );
           newSelection[ i ] = table.indexOf( item );
         }
       }
       table.deselectAll();
       table.select( newSelection );
     }
   }
 
   private static void readTopItemIndex( final Table table ) {
     String value = WidgetLCAUtil.readPropertyValue( table, "topItemIndex" );
     if( value != null ) {
       int topIndex = NumberFormatUtil.parseInt( value );
       int topOffset = topIndex * table.getItemHeight();
       table.setTopIndex( topIndex );
       processScrollBarSelection( table.getVerticalBar(), topOffset );
     }
   }
 
   private static void readFocusIndex( final Table table ) {
     String value = WidgetLCAUtil.readPropertyValue( table, "focusItem" );
     if( value != null ) {
       TableItem item = getItem( table, value );
       ITableAdapter adapter = ( ITableAdapter )table.getAdapter( ITableAdapter.class );
       adapter.setFocusIndex( table.indexOf( item ) );
     }
   }
 
   private static void readScrollLeft( final Table table ) {
     String value = WidgetLCAUtil.readPropertyValue( table, "scrollLeft" );
     if( value != null ) {
       int leftOffset = NumberFormatUtil.parseInt( value );
       Object adapter = table.getAdapter( ITableAdapter.class );
       ITableAdapter tableAdapter = ( ITableAdapter )adapter;
       tableAdapter.setLeftOffset( leftOffset );
       processScrollBarSelection( table.getHorizontalBar(), leftOffset );
     }
   }
 
   private static void readWidgetSelected( final Table table ) {
     if( WidgetLCAUtil.wasEventSent( table, JSConst.EVENT_WIDGET_SELECTED ) ) {
       HttpServletRequest request = ContextProvider.getRequest();
       String selectionId = request.getParameter( JSConst.EVENT_WIDGET_SELECTED_ITEM );
       TableItem item = getItem( table, selectionId );
       // Bugfix: check if index is valid before firing event to avoid problems with fast scrolling
       // TODO [tb] : Still useful? bugzilla id?
       if( item != null ) {
         int detail = getWidgetSelectedDetail();
         int id = SelectionEvent.WIDGET_SELECTED;
         Rectangle bounds = new Rectangle( 0, 0, 0, 0 );
         int stateMask = EventLCAUtil.readStateMask( JSConst.EVENT_WIDGET_SELECTED_MODIFIER );
         SelectionEvent event
           = new SelectionEvent( table, item, id, bounds, stateMask, "", true, detail );
         event.processEvent();
       }
     }
   }
 
   private static void readWidgetDefaultSelected( final Table table ) {
     String defaultSelectedParam = JSConst.EVENT_WIDGET_DEFAULT_SELECTED;
     if( WidgetLCAUtil.wasEventSent( table, defaultSelectedParam ) ) {
       // A default-selected event can occur without a selection being present.
       // In this case the event.item field points to the focused item
       TableItem item = getFocusedItem( table );
       HttpServletRequest request = ContextProvider.getRequest();
       String selectionId = request.getParameter( defaultSelectedParam + ".item" );
       TableItem selectedItem = getItem( table, selectionId );
       if( selectedItem != null ) {
         // TODO [rh] do something about when index points to unresolved item!
         item = selectedItem;
       }
       int id = SelectionEvent.WIDGET_DEFAULT_SELECTED;
       SelectionEvent event = new SelectionEvent( table, item, id );
       event.stateMask = EventLCAUtil.readStateMask( JSConst.EVENT_WIDGET_SELECTED_MODIFIER );
       event.processEvent();
     }
   }
 
   private static int getWidgetSelectedDetail() {
     HttpServletRequest request = ContextProvider.getRequest();
     String value = request.getParameter( JSConst.EVENT_WIDGET_SELECTED_DETAIL );
     return "check".equals( value ) ? SWT.CHECK : SWT.NONE;
   }
 
   private static TableItem getFocusedItem( final Table table ) {
     TableItem result = null;
     ITableAdapter tableAdapter = ( ITableAdapter )table.getAdapter( ITableAdapter.class );
     int focusIndex = tableAdapter.getFocusIndex();
     if( focusIndex != -1 ) {
       // TODO [rh] do something about when index points to unresolved item!
       result = table.getItem( focusIndex );
     }
     return result;
   }
 
   ///////////////////////////////////////////
   // Helping methods to write JavaScript code
 
   private static void writeHeaderHeight( final Table table ) throws IOException {
     JSWriter writer = JSWriter.getWriterFor( table );
     Integer newValue = new Integer( table.getHeaderHeight() );
     writer.set( PROP_HEADER_HEIGHT, "headerHeight", newValue, null );
   }
 
   private static void writeHeaderVisible( final Table table ) throws IOException {
     JSWriter writer = JSWriter.getWriterFor( table );
     Boolean newValue = Boolean.valueOf( table.getHeaderVisible() );
     writer.set( PROP_HEADER_VISIBLE, "headerVisible", newValue, Boolean.FALSE );
   }
 
   private static void writeItemHeight( final Table table ) throws IOException {
     JSWriter writer = JSWriter.getWriterFor( table );
     Integer newValue = new Integer( table.getItemHeight( ) );
     writer.set( PROP_ITEM_HEIGHT, "itemHeight", newValue, DEFAUT_ITEM_HEIGHT );
   }
 
   private static void writeColumnCount( final Table table ) throws IOException {
     JSWriter writer = JSWriter.getWriterFor( table );
     Integer newValue = new Integer( table.getColumnCount() );
     if( WidgetLCAUtil.hasChanged( table, PROP_COLUMN_COUNT, newValue ) ) {
       writer.set( PROP_COLUMN_COUNT, "columnCount", newValue, DEFAULT_COLUMN_COUNT );
     }
   }
 
   private static void writeItemCount( final Table table ) throws IOException {
     JSWriter writer = JSWriter.getWriterFor( table );
     Integer newValue = new Integer( table.getItemCount() );
     writer.set( PROP_ITEM_COUNT, "itemCount", newValue, DEFAULT_ITEM_COUNT );
   }
 
   private static void writeTopIndex( final Table table ) throws IOException {
     JSWriter writer = JSWriter.getWriterFor( table );
     Integer newValue = new Integer( table.getTopIndex() );
     writer.set( PROP_TOP_ITEM_INDEX, "topItemIndex", newValue, DEFAULT_TOP_INDEX );
   }
 
   private static void writeFocusItem( final Table table ) throws IOException {
     if( TableLCAUtil.hasFocusIndexChanged( table ) ) {
       ITableAdapter adapter = ( ITableAdapter )table.getAdapter( ITableAdapter.class );
       // TableItemLCA renders focusIndex in case != -1
       if( adapter.getFocusIndex() == -1 ) {
         JSWriter writer = JSWriter.getWriterFor( table );
         writer.set( "focusItem", new Object[]{ null } );
       }
     }
   }
 
   private static void writeLinesVisible( final Table table ) throws IOException {
     JSWriter writer = JSWriter.getWriterFor( table );
     Boolean newValue = Boolean.valueOf( table.getLinesVisible() );
     writer.set( PROP_LINES_VISIBLE, "linesVisible", newValue, Boolean.FALSE );
   }
 
   private static void writeSelectionListener( final Table table ) throws IOException {
     Boolean newValue = Boolean.valueOf( SelectionEvent.hasListener( table ) );
     String prop = PROP_SELECTION_LISTENERS;
     if( WidgetLCAUtil.hasChanged( table, prop, newValue, Boolean.FALSE ) ) {
       JSWriter writer = JSWriter.getWriterFor( table );
       writer.set( "hasSelectionListeners", newValue );
     }
   }
 
   private static void writeScrollBarsSelectionListener( final Table table ) throws IOException {
     Boolean newValue = hasScrollBarsSelectionListener( table );
     String prop = PROP_SCROLLBARS_SELECTION_LISTENER;
     if( WidgetLCAUtil.hasChanged( table, prop, newValue, Boolean.FALSE ) ) {
       JSWriter writer = JSWriter.getWriterFor( table );
       writer.set( "hasScrollBarsSelectionListener", newValue );
     }
   }
 
   private static void writeScrollBarsVisible( final Table table ) throws IOException {
     boolean hasHChanged = WidgetLCAUtil.hasChanged( table,
                                                     PROP_HAS_H_SCROLL_BAR,
                                                     hasHScrollBar( table ),
                                                     Boolean.TRUE );
     boolean hasVChanged = WidgetLCAUtil.hasChanged( table,
                                                     PROP_HAS_V_SCROLL_BAR,
                                                     hasVScrollBar( table ),
                                                     Boolean.TRUE );
     if( hasHChanged || hasVChanged ) {
       JSWriter writer = JSWriter.getWriterFor( table );
       Object[] args = new Object[]{
         hasHScrollBar( table ),
         hasVScrollBar( table )
       };
       writer.set( "scrollBarsVisible", args );
     }
   }
 
   private static void writeLeftOffset( final Table table ) throws IOException {
     JSWriter writer = JSWriter.getWriterFor( table );
     Integer newValue = getLeftOffset( table );
     writer.set( PROP_LEFT_OFFSET, "scrollLeft", newValue, DEFAULT_LEFT_OFFSET );
   }
 
   ////////////////
   // Cell tooltips
 
   private static void writeEnableCellToolTip( final Table table ) throws IOException {
     JSWriter writer = JSWriter.getWriterFor( table );
     String prop = PROP_ENABLE_CELL_TOOLTIP;
     Boolean newValue = new Boolean( CellToolTipUtil.isEnabledFor( table ) );
     writer.set( prop, "enableCellToolTip", newValue, Boolean.FALSE );
   }
 
   private static void readCellToolTipTextRequested( final Table table ) {
     ICellToolTipAdapter adapter = CellToolTipUtil.getAdapter( table );
     adapter.setToolTipText( null );
     String event = JSConst.EVENT_CELL_TOOLTIP_REQUESTED;
     if( WidgetLCAUtil.wasEventSent( table, event ) ) {
       ICellToolTipProvider provider = adapter.getCellToolTipProvider();
       if( provider != null ) {
         HttpServletRequest request = ContextProvider.getRequest();
         String cell = request.getParameter( JSConst.EVENT_CELL_TOOLTIP_DETAILS );
         String[] details = cell.split( "," );
         String itemId = details[ 0 ];
         int columnIndex = NumberFormatUtil.parseInt( details[ 1 ] );
         TableItem item = getItemById( table, itemId );
         // Bug 321119: Sometimes the client can request tooltips for already
         //             disposed cells.
         if( item != null && ( columnIndex == 0 || columnIndex < table.getColumnCount() ) ) {
           provider.getToolTipText( item, columnIndex );
         }
       }
     }
   }
 
   private static void writeCellToolTipText( final Table table ) throws IOException {
     ICellToolTipAdapter adapter = CellToolTipUtil.getAdapter( table );
     String text = adapter.getToolTipText();
     if( text != null ) {
       JSWriter writer = JSWriter.getWriterFor( table );
       text = WidgetLCAUtil.escapeText( text, false );
       text = WidgetLCAUtil.replaceNewLines( text, "<br/>" );
       writer.call( "setCellToolTipText", new String[]{ text } );
     }
   }
 
   private static TableItem getItemById( final Table table, final String itemId )
   {
     TableItem result = null;
     TableItem[] items = table.getItems();
     for( int i = 0; i < items.length && result == null; i++ ) {
       if( WidgetUtil.getId( items[ i ] ).equals( itemId ) ) {
         result = items[ i ];
       }
     }
     return result;
   }
 
   //////////////////
   // Helping methods
 
   private static TableItem getItem( Table table, String itemId ) {
     TableItem item;
     String[] idParts = itemId.split( "#" );
     if( idParts.length == 2 ) {
       int index = Integer.parseInt( idParts[ 1 ] );
       item = table.getItem( index );
     } else {
       item = ( TableItem )WidgetUtil.find( table, itemId );
     }
     return item;
   }
 
   private static Boolean hasHScrollBar( final Table table ) {
     Object adapter = table.getAdapter( ITableAdapter.class );
     ITableAdapter tableAdapter = ( ITableAdapter )adapter;
     return Boolean.valueOf( tableAdapter.hasHScrollBar() );
   }
 
   private static Boolean hasVScrollBar( final Table table ) {
     Object adapter = table.getAdapter( ITableAdapter.class );
     ITableAdapter tableAdapter = ( ITableAdapter )adapter;
     return Boolean.valueOf( tableAdapter.hasVScrollBar() );
   }
 
   private static Integer getLeftOffset( final Table table ) {
     Object adapter = table.getAdapter( ITableAdapter.class );
     ITableAdapter tableAdapter = ( ITableAdapter )adapter;
     return new Integer( tableAdapter.getLeftOffset() );
   }
 
   private static Boolean hasScrollBarsSelectionListener( final Table table ) {
     boolean result = false;
     ScrollBar horizontalBar = table.getHorizontalBar();
     if( horizontalBar != null ) {
       result = result || SelectionEvent.hasListener( horizontalBar );
     }
     ScrollBar verticalBar = table.getVerticalBar();
     if( verticalBar != null ) {
       result = result || SelectionEvent.hasListener( verticalBar );
     }
     return Boolean.valueOf( result );
   }
 
   private static void processScrollBarSelection( final ScrollBar scrollBar,
                                                  final int selection )
   {
     if( scrollBar != null ) {
       scrollBar.setSelection( selection );
       if( SelectionEvent.hasListener( scrollBar ) ) {
         int eventId = SelectionEvent.WIDGET_SELECTED;
         SelectionEvent evt = new SelectionEvent( scrollBar, null, eventId );
         evt.stateMask
           = EventLCAUtil.readStateMask( JSConst.EVENT_WIDGET_SELECTED_MODIFIER );
         evt.processEvent();
       }
     }
   }
 
   static Boolean alwaysHideSelection( final Table table ) {
     Boolean result = Boolean.FALSE;
     Object data = table.getData( Table.ALWAYS_HIDE_SELECTION );
     if( Boolean.TRUE.equals( data ) ) {
       result = Boolean.TRUE;
     }
     return result;
   }
 }
