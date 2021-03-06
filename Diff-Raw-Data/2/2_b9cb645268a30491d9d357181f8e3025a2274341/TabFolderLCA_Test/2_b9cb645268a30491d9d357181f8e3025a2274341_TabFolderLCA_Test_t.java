 /*******************************************************************************
  * Copyright (c) 2007, 2011 Innoopract Informationssysteme GmbH and others.
  * All rights reserved. This program and the accompanying materials
  * are made available under the terms of the Eclipse Public License v1.0
  * which accompanies this distribution, and is available at
  * http://www.eclipse.org/legal/epl-v10.html
  *
  * Contributors:
  *    Innoopract Informationssysteme GmbH - initial API and implementation
  *    EclipseSource - ongoing development
  ******************************************************************************/
 package org.eclipse.swt.internal.widgets.tabfolderkit;
 
 import java.io.IOException;
 import java.util.ArrayList;
 import java.util.Arrays;
 
 import junit.framework.TestCase;
 
 import org.eclipse.rap.rwt.testfixture.Fixture;
 import org.eclipse.rap.rwt.testfixture.Message;
 import org.eclipse.rap.rwt.testfixture.Message.CreateOperation;
 import org.eclipse.rwt.graphics.Graphics;
 import org.eclipse.rwt.internal.lifecycle.DisplayUtil;
 import org.eclipse.rwt.internal.lifecycle.JSConst;
 import org.eclipse.rwt.internal.service.RequestParams;
 import org.eclipse.rwt.lifecycle.IWidgetAdapter;
 import org.eclipse.rwt.lifecycle.WidgetUtil;
 import org.eclipse.swt.SWT;
 import org.eclipse.swt.events.*;
 import org.eclipse.swt.graphics.*;
 import org.eclipse.swt.internal.events.ActivateAdapter;
 import org.eclipse.swt.internal.events.ActivateEvent;
 import org.eclipse.swt.internal.widgets.Props;
 import org.eclipse.swt.layout.FillLayout;
 import org.eclipse.swt.widgets.*;
 
 public class TabFolderLCA_Test extends TestCase {
 
   private Display display;
   private Shell shell;
   private TabFolderLCA lca;
 
   protected void setUp() throws Exception {
     Fixture.setUp();
     display = new Display();
     shell = new Shell( display );
     lca = new TabFolderLCA();
     Fixture.fakeNewRequest( display );
   }
 
   protected void tearDown() throws Exception {
     Fixture.tearDown();
   }
 
   public void testPreserveValues() {
     TabFolder tabfolder = new TabFolder( shell, SWT.NONE );
     Boolean hasListeners;
     Fixture.markInitialized( display );
     //control: enabled
     Fixture.preserveWidgets();
     IWidgetAdapter adapter = WidgetUtil.getAdapter( tabfolder );
     assertEquals( Boolean.TRUE, adapter.getPreserved( Props.ENABLED ) );
     Fixture.clearPreserved();
     tabfolder.setEnabled( false );
     Fixture.preserveWidgets();
     adapter = WidgetUtil.getAdapter( tabfolder );
     assertEquals( Boolean.FALSE, adapter.getPreserved( Props.ENABLED ) );
     Fixture.clearPreserved();
     tabfolder.setEnabled( true );
     //visible
     tabfolder.setSize( 10, 10 );
     Fixture.preserveWidgets();
     adapter = WidgetUtil.getAdapter( tabfolder );
     assertEquals( Boolean.TRUE, adapter.getPreserved( Props.VISIBLE ) );
     Fixture.clearPreserved();
     tabfolder.setVisible( false );
     Fixture.preserveWidgets();
     adapter = WidgetUtil.getAdapter( tabfolder );
     assertEquals( Boolean.FALSE, adapter.getPreserved( Props.VISIBLE ) );
     Fixture.clearPreserved();
     //menu
     Fixture.preserveWidgets();
     adapter = WidgetUtil.getAdapter( tabfolder );
     assertEquals( null, adapter.getPreserved( Props.MENU ) );
     Fixture.clearPreserved();
     Menu menu = new Menu( tabfolder );
     MenuItem item = new MenuItem( menu, SWT.NONE );
     item.setText( "1 Item" );
     tabfolder.setMenu( menu );
     Fixture.preserveWidgets();
     adapter = WidgetUtil.getAdapter( tabfolder );
     assertEquals( menu, adapter.getPreserved( Props.MENU ) );
     Fixture.clearPreserved();
     //bound
     Rectangle rectangle = new Rectangle( 10, 10, 30, 50 );
     tabfolder.setBounds( rectangle );
     Fixture.preserveWidgets();
     adapter = WidgetUtil.getAdapter( tabfolder );
     assertEquals( rectangle, adapter.getPreserved( Props.BOUNDS ) );
     Fixture.clearPreserved();
     //control_listeners
     Fixture.preserveWidgets();
     adapter = WidgetUtil.getAdapter( tabfolder );
     hasListeners = ( Boolean )adapter.getPreserved( Props.CONTROL_LISTENERS );
     assertEquals( Boolean.FALSE, hasListeners );
     Fixture.clearPreserved();
     tabfolder.addControlListener( new ControlAdapter() { } );
     Fixture.preserveWidgets();
     adapter = WidgetUtil.getAdapter( tabfolder );
     hasListeners = ( Boolean )adapter.getPreserved( Props.CONTROL_LISTENERS );
     assertEquals( Boolean.TRUE, hasListeners );
     Fixture.clearPreserved();
     //z-index
     Fixture.preserveWidgets();
     adapter = WidgetUtil.getAdapter( tabfolder );
     assertTrue( adapter.getPreserved( Props.Z_INDEX ) != null );
     Fixture.clearPreserved();
     //foreground background font
     Color background = Graphics.getColor( 122, 33, 203 );
     tabfolder.setBackground( background );
     Color foreground = Graphics.getColor( 211, 178, 211 );
     tabfolder.setForeground( foreground );
     Font font = Graphics.getFont( "font", 12, SWT.BOLD );
     tabfolder.setFont( font );
     Fixture.preserveWidgets();
     adapter = WidgetUtil.getAdapter( tabfolder );
     assertEquals( background, adapter.getPreserved( Props.BACKGROUND ) );
     assertEquals( foreground, adapter.getPreserved( Props.FOREGROUND ) );
     assertEquals( font, adapter.getPreserved( Props.FONT ) );
     Fixture.clearPreserved();
     //tab_index
     Fixture.preserveWidgets();
     adapter = WidgetUtil.getAdapter( tabfolder );
     assertTrue( adapter.getPreserved( Props.Z_INDEX ) != null );
     Fixture.clearPreserved();
     //tooltiptext
     Fixture.preserveWidgets();
     adapter = WidgetUtil.getAdapter( tabfolder );
     assertEquals( null, tabfolder.getToolTipText() );
     Fixture.clearPreserved();
     tabfolder.setToolTipText( "some text" );
     Fixture.preserveWidgets();
     adapter = WidgetUtil.getAdapter( tabfolder );
     assertEquals( "some text", tabfolder.getToolTipText() );
     Fixture.clearPreserved();
     //activate_listeners   Focus_listeners
     Fixture.preserveWidgets();
     adapter = WidgetUtil.getAdapter( tabfolder );
     hasListeners = ( Boolean )adapter.getPreserved( Props.FOCUS_LISTENER );
     assertEquals( Boolean.FALSE, hasListeners );
     Fixture.clearPreserved();
     tabfolder.addFocusListener( new FocusAdapter() { } );
     Fixture.preserveWidgets();
     adapter = WidgetUtil.getAdapter( tabfolder );
     hasListeners = ( Boolean )adapter.getPreserved( Props.FOCUS_LISTENER );
     assertEquals( Boolean.TRUE, hasListeners );
     Fixture.clearPreserved();
     Fixture.preserveWidgets();
     adapter = WidgetUtil.getAdapter( tabfolder );
     hasListeners = ( Boolean )adapter.getPreserved( Props.ACTIVATE_LISTENER );
     assertEquals( Boolean.FALSE, hasListeners );
     Fixture.clearPreserved();
     ActivateEvent.addListener( tabfolder, new ActivateAdapter() {
     } );
     Fixture.preserveWidgets();
     adapter = WidgetUtil.getAdapter( tabfolder );
     hasListeners = ( Boolean )adapter.getPreserved( Props.ACTIVATE_LISTENER );
     assertEquals( Boolean.TRUE, hasListeners );
     Fixture.clearPreserved();
   }
 
   public void testSelectionWithoutListener() {
     shell.setLayout( new FillLayout() );
     TabFolder folder = new TabFolder( shell, SWT.NONE );
     TabItem item0 = new TabItem( folder, SWT.NONE );
     Control control0 = new Button( folder, SWT.PUSH );
     item0.setControl( control0 );
     TabItem item1 = new TabItem( folder, SWT.NONE );
     Control control1 = new Button( folder, SWT.PUSH );
     item1.setControl( control1 );
     shell.open();
 
     String folderId = WidgetUtil.getId( folder );
     String item1Id = WidgetUtil.getId( item1 );
 
     Fixture.fakeNewRequest( display );
     Fixture.fakeRequestParam( JSConst.EVENT_WIDGET_SELECTED, folderId );
     Fixture.fakeRequestParam( JSConst.EVENT_WIDGET_SELECTED_ITEM, item1Id );
     Fixture.readDataAndProcessAction( display );
 
     assertEquals( 1, folder.getSelectionIndex() );
     assertFalse( control0.getVisible() );
     assertTrue( control1.getVisible() );
   }
 
   public void testSelectionWithListener() {
     final java.util.List<SelectionEvent> events = new ArrayList<SelectionEvent>();
     shell.setLayout( new FillLayout() );
     TabFolder folder = new TabFolder( shell, SWT.NONE );
     TabItem item0 = new TabItem( folder, SWT.NONE );
     Control control0 = new Button( folder, SWT.PUSH );
     item0.setControl( control0 );
     TabItem item1 = new TabItem( folder, SWT.NONE );
     Control control1 = new Button( folder, SWT.PUSH );
     item1.setControl( control1 );
     shell.open();
     folder.addSelectionListener( new SelectionListener() {
       public void widgetSelected( SelectionEvent event ) {
         events.add( event );
       }
       public void widgetDefaultSelected( SelectionEvent event ) {
         events.add( event );
       }
     } );
 
     String displayId = DisplayUtil.getAdapter( display ).getId();
     String item1Id = WidgetUtil.getId( item1 );
     String folderId = WidgetUtil.getId( folder );
     Fixture.fakeNewRequest();
     Fixture.fakeRequestParam( RequestParams.UIROOT, displayId );
     Fixture.fakeRequestParam( JSConst.EVENT_WIDGET_SELECTED, folderId );
     Fixture.fakeRequestParam( JSConst.EVENT_WIDGET_SELECTED_ITEM, item1Id );
 
     Fixture.readDataAndProcessAction( display );
     assertEquals( 1, folder.getSelectionIndex() );
     assertFalse( control0.getVisible() );
     assertTrue( control1.getVisible() );
     assertEquals( 1, events.size() );
     SelectionEvent event = events.get( 0 );
     assertSame( item1, event.item );
     assertSame( folder, event.widget );
     assertTrue( event.doit );
     assertEquals( 0, event.x );
     assertEquals( 0, event.y );
     assertEquals( 0, event.width );
     assertEquals( 0, event.height );
     assertEquals( 0, event.detail );
     assertNull( event.text );
   }
 
   public void testRenderCreate() throws IOException {
     TabFolder folder = new TabFolder( shell, SWT.NONE );
 
     lca.renderInitialization( folder );
 
     Message message = Fixture.getProtocolMessage();
     CreateOperation operation = message.findCreateOperation( folder );
     assertEquals( "rwt.widgets.TabFolder", operation.getType() );
     Object[] styles = operation.getStyles();
     assertTrue( Arrays.asList( styles ).contains( "TOP" ) );
   }
 
   public void testRenderCreateOnBottom() throws IOException {
     TabFolder folder = new TabFolder( shell, SWT.BOTTOM );
 
     lca.renderInitialization( folder );
 
     Message message = Fixture.getProtocolMessage();
     CreateOperation operation = message.findCreateOperation( folder );
     assertEquals( "rwt.widgets.TabFolder", operation.getType() );
     Object[] styles = operation.getStyles();
     assertTrue( Arrays.asList( styles ).contains( "BOTTOM" ) );
   }
 
   public void testRenderParent() throws IOException {
     TabFolder folder = new TabFolder( shell, SWT.NONE );
 
     lca.renderInitialization( folder );
 
     Message message = Fixture.getProtocolMessage();
     CreateOperation operation = message.findCreateOperation( folder );
     assertEquals( WidgetUtil.getId( folder.getParent() ), operation.getParent() );
   }
 
   public void testRenderInitialSelectionIndexWithoutItems() throws IOException {
     TabFolder folder = new TabFolder( shell, SWT.NONE );
 
     lca.render( folder );
 
     Message message = Fixture.getProtocolMessage();
     CreateOperation operation = message.findCreateOperation( folder );
     assertTrue( operation.getPropertyNames().indexOf( "selectionIndex" ) == -1 );
   }
 
   public void testRenderInitialSelectionIndexWithItems() throws IOException {
     TabFolder folder = new TabFolder( shell, SWT.NONE );
     new TabItem( folder, SWT.NONE );
     new TabItem( folder, SWT.NONE );
     new TabItem( folder, SWT.NONE );
 
     lca.render( folder );
 
     Message message = Fixture.getProtocolMessage();
     CreateOperation operation = message.findCreateOperation( folder );
     assertEquals( Integer.valueOf( 0 ), operation.getProperty( "selectionIndex" ) );
   }
 
   public void testRenderSelectionIndex() throws IOException {
     TabFolder folder = new TabFolder( shell, SWT.NONE );
     new TabItem( folder, SWT.NONE );
     new TabItem( folder, SWT.NONE );
     new TabItem( folder, SWT.NONE );
 
     folder.setSelection( 1 );
     lca.renderChanges( folder );
 
     Message message = Fixture.getProtocolMessage();
     assertEquals( new Integer( 1 ), message.findSetProperty( folder, "selectionIndex" ) );
   }
 
  public void testRenderSelectionUnchanged() throws IOException {
     TabFolder folder = new TabFolder( shell, SWT.NONE );
     new TabItem( folder, SWT.NONE );
     new TabItem( folder, SWT.NONE );
     new TabItem( folder, SWT.NONE );
     Fixture.markInitialized( display );
     Fixture.markInitialized( folder );
 
     folder.setSelection( 1 );
     Fixture.preserveWidgets();
     lca.renderChanges( folder );
 
     Message message = Fixture.getProtocolMessage();
     assertNull( message.findSetOperation( folder, "selectionIndex" ) );
   }
 }
