 /*******************************************************************************
  * Copyright (c) 2002, 2008 Innoopract Informationssysteme GmbH.
  * All rights reserved. This program and the accompanying materials
  * are made available under the terms of the Eclipse Public License v1.0
  * which accompanies this distribution, and is available at
  * http://www.eclipse.org/legal/epl-v10.html
  *
  * Contributors:
  *     Innoopract Informationssysteme GmbH - initial API and implementation
  ******************************************************************************/
 package org.eclipse.swt.internal.widgets.buttonkit;
 
 import java.io.IOException;
 
 import org.eclipse.rwt.internal.lifecycle.JSConst;
 import org.eclipse.rwt.lifecycle.*;
import org.eclipse.swt.SWT;
 import org.eclipse.swt.events.SelectionEvent;
 import org.eclipse.swt.internal.widgets.Props;
 import org.eclipse.swt.widgets.Button;
 
 
 final class RadioButtonDelegateLCA extends ButtonDelegateLCA {
 
   static final String TYPE_POOL_ID
     = RadioButtonDelegateLCA.class.getName();
   private static final String QX_TYPE = "qx.ui.form.RadioButton";
 
   private static final String JS_PROP_MANAGER = "manager";
   private static final String REGISTER_RADIO_BUTTON
     = "org.eclipse.swt.ButtonUtil.registerRadioButton";
   private static final String UNREGISTER_RADIO_BUTTON
     = "org.eclipse.swt.ButtonUtil.unregisterRadioButton";
   private static final String WIDGET_SELECTED
     = "org.eclipse.swt.ButtonUtil.radioSelected";
 
   private final JSListenerInfo JS_LISTENER_INFO
     = new JSListenerInfo( JSConst.QX_EVENT_CHANGE_SELECTED,
                           WIDGET_SELECTED,
                           JSListenerType.STATE_AND_ACTION );
 
   void preserveValues( final Button button ) {
     ButtonLCAUtil.preserveValues( button );
     WidgetLCAUtil.preserveCustomVariant( button );
   }
 
   void readData( final Button button ) {
     ButtonLCAUtil.readSelection( button );
     ControlLCAUtil.processSelection( button, null, true );
     ControlLCAUtil.processMouseEvents( button );
     ControlLCAUtil.processKeyEvents( button );
   }
 
   void renderInitialization( final Button button ) throws IOException {
     JSWriter writer = JSWriter.getWriterFor( button );
     writer.newWidget( QX_TYPE );
     ButtonLCAUtil.writeLabelMode( button );
     Object[] args = new Object[] { button };
     writer.callStatic( REGISTER_RADIO_BUTTON, args );
     ControlLCAUtil.writeStyleFlags( button );
    WidgetLCAUtil.writeStyleFlag( button, SWT.RADIO, "RADIO" );
   }
 
   // TODO [rh] qooxdoo radioButton cannot display images, should we ignore
   //      setImage() calls when style is SWT.RADIO?
   void renderChanges( final Button button ) throws IOException {
     JSWriter writer = JSWriter.getWriterFor( button );
     // TODO [rh] the JSConst.JS_WIDGET_SELECTED does unnecessarily send
     // bounds of the widget that was clicked -> In the SelectionEvent
     // for Button the bounds are undefined
     writer.updateListener( JS_PROP_MANAGER ,
                            JS_LISTENER_INFO,
                            Props.SELECTION_LISTENERS,
                            SelectionEvent.hasListener( button ) );
     ControlLCAUtil.writeChanges( button );
     ButtonLCAUtil.writeSelection( button );
     ButtonLCAUtil.writeText( button );
     ButtonLCAUtil.writeAlignment( button );
     WidgetLCAUtil.writeCustomVariant( button );
   }
 
   void renderDispose( final Button button ) throws IOException {
     JSWriter writer = JSWriter.getWriterFor( button );
     writer.callStatic( UNREGISTER_RADIO_BUTTON, new Object[] { button } );
     writer.dispose();
   }
 
   String getTypePoolId( final Button button ) {
 //    return TYPE_POOL_ID;
     return null;
   }
 
   void createResetHandlerCalls( final String typePoolId ) throws IOException {
 // TODO [fappel]: check why removal of listener doesn't work. Seems as if
 //                manager is removed already (maybe dispose call?)...
 //    JSWriter writer = JSWriter.getWriterForResetHandler();
 //    writer.removeListener( JS_PROP_MANAGER,
 //                           JS_LISTENER_INFO.getEventType(),
 //                           JS_LISTENER_INFO.getJSListener() );
     ButtonLCAUtil.resetAlignment();
     ButtonLCAUtil.resetText();
     ButtonLCAUtil.resetSelection();
     ControlLCAUtil.resetChanges();
     ControlLCAUtil.resetStyleFlags();
   }
 }
