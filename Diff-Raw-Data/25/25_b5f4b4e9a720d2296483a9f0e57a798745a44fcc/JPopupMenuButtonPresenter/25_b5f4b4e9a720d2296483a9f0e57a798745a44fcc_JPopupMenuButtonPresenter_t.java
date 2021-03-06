 /*
  * JPopupMenuButtonPresenter.java
  * Copyright (C) 2005 by:
  *
  *----------------------------
  * cismet GmbH
  * Goebenstrasse 40
  * 66117 Saarbruecken
  * http://www.cismet.de
  *----------------------------
  *
  * This library is free software; you can redistribute it and/or
  * modify it under the terms of the GNU Lesser General Public
  * License as published by the Free Software Foundation; either
  * version 2.1 of the License, or (at your option) any later version.
  *
  * This library is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
  * General Public License for more details.
  *
  * You should have received a copy of the GNU General Public
  * License along with this library; if not, write to the Free Software
  * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
  *
  *----------------------------
  * Author:
  * thorsten.hell@cismet.de
  *----------------------------
  *
  * Created on 15. Juli 2005, 16:57
  *
  */
 package de.cismet.tools.gui;
 import javax.swing.JMenuItem;
 import javax.swing.JPopupMenu;
 import javax.swing.UIManager;
 import javax.swing.event.PopupMenuEvent;
 import javax.swing.event.PopupMenuListener;
 
 
 
 /**
  *
  * @author  thorsten.hell@cismet.de
  */
 public class JPopupMenuButtonPresenter extends javax.swing.JApplet {
     private JPopupMenuButton mb;
     /** Initializes the applet JPopupMenuButtonPresenter */
     public void init() {
         try {
             java.awt.EventQueue.invokeAndWait(new Runnable() {
                 public void run() {
                     try {
                         //UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()) ;
                     }
                     catch (Exception e) {}
                     initComponents();
                     
                     mb=new JPopupMenuButton();
                    mb.setIcon(new javax.swing.ImageIcon(getClass().getResource("/de/cismet/tools/gui/res/demo.png")));  //NOI18N
                     mb.setEnabled(true);
                     JPopupMenu popupMenu = new javax.swing.JPopupMenu();
                    popupMenu.add(new JMenuItem("Print",'P'));  //NOI18N
                    popupMenu.add(new JMenuItem("Preview",'v'));  //NOI18N
                    popupMenu.add(new JMenuItem("Properties",'t'));  //NOI18N
                     popupMenu.addPopupMenuListener(new PopupMenuListener() {
                         public void popupMenuCanceled(PopupMenuEvent e) {
                         }
                         public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
                         }
                         public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
                            System.out.println("XXXXXX");  //NOI18N
                         }
                     });
                     mb.setPopupMenu(popupMenu);
                     toolbar.add(mb);
                     toolbar.add(chkEnabled);
                     
                 }
             });
         } catch (Exception ex) {
             ex.printStackTrace();
         }
     }
     
     /** This method is called from within the init() method to
      * initialize the form.
      * WARNING: Do NOT modify this code. The content of this method is
      * always regenerated by the Form Editor.
      */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
     private void initComponents() {

         chkEnabled = new javax.swing.JCheckBox();
         toolbar = new javax.swing.JToolBar();
 
         chkEnabled.setSelected(true);
        chkEnabled.setText(org.openide.util.NbBundle.getMessage(JPopupMenuButtonPresenter.class, "JPopupMenuButtonPresenter.chkEnabled.text")); // NOI18N
         chkEnabled.addActionListener(new java.awt.event.ActionListener() {
             public void actionPerformed(java.awt.event.ActionEvent evt) {
                 chkEnabledActionPerformed(evt);
             }
         });
 
         getContentPane().add(toolbar, java.awt.BorderLayout.NORTH);
    }// </editor-fold>//GEN-END:initComponents
 
     private void chkEnabledActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_chkEnabledActionPerformed
        mb.setEnabled(chkEnabled.isSelected());
     }//GEN-LAST:event_chkEnabledActionPerformed
     
     
     // Variables declaration - do not modify//GEN-BEGIN:variables
     private javax.swing.JCheckBox chkEnabled;
     private javax.swing.JToolBar toolbar;
     // End of variables declaration//GEN-END:variables
     
 }
