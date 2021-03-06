 /*******************************************************************************
  * Copyright (c) 2011 EclipseSource and others.
  * All rights reserved. This program and the accompanying materials
  * are made available under the terms of the Eclipse Public License v1.0
  * which accompanies this distribution, and is available at
  * http://www.eclipse.org/legal/epl-v10.html
  *
  * Contributors:
  *    EclipseSource - initial API and implementation
  ******************************************************************************/
 package org.eclipse.rwt.internal.protocol;
 
 import static org.eclipse.rwt.internal.resources.TestUtil.assertArrayEquals;
 import junit.framework.TestCase;
 
 import org.eclipse.rap.rwt.testfixture.Fixture;
 import org.eclipse.swt.SWT;
 import org.eclipse.swt.custom.SashForm;
 import org.eclipse.swt.widgets.*;
 
 
 public class RWTStylesUtil_Test extends TestCase {
 
   private Shell shell;
 
   @Override
   protected void setUp() throws Exception {
     Fixture.setUp();
     Display display = new Display();
     shell = new Shell( display );
   }
 
   @Override
   protected void tearDown() throws Exception {
     Fixture.tearDown();
   }
 
   public void testGetStylesForShell() {
     String[] shellStyles = new String[]{
       "BORDER",
       "CLOSE",
       "MIN",
       "MAX",
       "NO_TRIM",
       "RESIZE",
       "TITLE",
       "ON_TOP",
       "TOOL",
       "SHEET",
       "APPLICATION_MODAL",
       "MODELESS",
       "PRIMARY_MODAL",
       "SYSTEM_MODAL"
     };
     String[] allowedStyles = RWTStylesUtil.getAllowedStylesForWidget( shell );
     assertArrayEquals( shellStyles, allowedStyles );
   }
 
   public void testGetStylesForCustomWidgetComposite() {
     SashForm form = new SashForm( shell, SWT.NONE );
 
     String[] allowedStyles = RWTStylesUtil.getAllowedStylesForWidget( form );
 
     String[] compositeStyles = new String[] {
       "NO_FOCUS",
       "NO_RADIO_GROUP",
       "H_SCROLL",
       "V_SCROLL",
       "BORDER"
     };
     assertArrayEquals( compositeStyles, allowedStyles );
   }
 
   public void testGetStylesForSubclassedControl() {
     SubclassedControl control = new SubclassedControl( shell, SWT.NONE );
 
     String[] allowedStyles = RWTStylesUtil.getAllowedStylesForWidget( control );
 
     String[] labelStyles = new String[] {
       "SEPARATOR",
       "HORIZONTAL",
       "VERTICAL",
       "SHADOW_IN",
       "SHADOW_OUT",
       "SHADOW_NONE",
       "CENTER",
       "LEFT",
       "RIGHT",
       "WRAP",
       "BORDER"
     };
     assertArrayEquals( labelStyles, allowedStyles );
   }
 
   public void testGetStylesForSubclassedWidget() {
     SubclassedWidget widget = new SubclassedWidget( shell, SWT.NONE );
 
     String[] allowedStyles = RWTStylesUtil.getAllowedStylesForWidget( widget );
 
     assertArrayEquals( new String[ 0 ], allowedStyles );
   }
 
   public void testGetStylesForButton() {
     String[] buttonStyles = new String[]{
       "CHECK",
       "PUSH",
       "RADIO",
       "TOGGLE",
       "FLAT",
       "WRAP",
       "LEFT",
       "RIGHT",
       "CENTER",
       "BORDER"
     };
     Button button = new Button( shell, SWT.PUSH );
     String[] allowedStyles = RWTStylesUtil.getAllowedStylesForWidget( button );
     assertArrayEquals( buttonStyles, allowedStyles );
   }
 
  public void testGetStylesForText() {
    String[] textStyles = new String[]{
      "CENTER",
      "LEFT",
      "MULTI",
      "PASSWORD",
      "SEARCH",
      "SINGLE",
      "RIGHT",
      "READ_ONLY",
      "WRAP",
      "H_SCROLL",
      "V_SCROLL",
      "BORDER"
    };
    Text text = new Text( shell, SWT.NONE );
    String[] allowedStyles = RWTStylesUtil.getAllowedStylesForWidget( text );
    assertArrayEquals( textStyles, allowedStyles );
  }

   public final class SubclassedControl extends Label {
     public SubclassedControl( Composite parent, int style ) {
       super( parent, style );
     }
   }
 
   public final class SubclassedWidget extends Widget {
     public SubclassedWidget( Widget parent, int style ) {
       super( parent, style );
     }
   }
 }
