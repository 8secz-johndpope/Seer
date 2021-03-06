 /*
  * @(#)BasicJidePopupMenuUI.java 12/13/2006
  *
  * Copyright 2002 - 2006 JIDE Software Inc. All rights reserved.
  */
 
 package com.jidesoft.plaf.basic;
 
 import com.jidesoft.swing.SimpleScrollPane;
 
 import javax.swing.*;
 import javax.swing.plaf.ComponentUI;
 import javax.swing.plaf.basic.BasicPopupMenuUI;
import javax.swing.plaf.basic.DefaultMenuLayout;
 
 public class BasicJidePopupMenuUI extends BasicPopupMenuUI {
     public BasicJidePopupMenuUI() {
     }
 
    @SuppressWarnings({"UnusedDeclaration"})
     public static ComponentUI createUI(JComponent c) {
         return new BasicJidePopupMenuUI();
     }
 
     @Override
     public Popup getPopup(JPopupMenu popupMenu, int x, int y) {
        if (popupMenu.getLayout() instanceof DefaultMenuLayout) {
            popupMenu.setLayout(new BoxLayout(popupMenu, ((DefaultMenuLayout) popupMenu.getLayout()).getAxis()));
        }
         PopupFactory popupFactory = PopupFactory.getSharedInstance();
         SimpleScrollPane contents = new SimpleScrollPane(popupMenu, SimpleScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, SimpleScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
         contents.getScrollUpButton().setOpaque(true);
         contents.getScrollDownButton().setOpaque(true);
         contents.setBorder(BorderFactory.createEmptyBorder());
         return popupFactory.getPopup(popupMenu.getInvoker(), contents, x, y);
     }
 }
