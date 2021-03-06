 /**
  * Project Wonderland
  *
  * Copyright (c) 2004-2008, Sun Microsystems, Inc., All Rights Reserved
  *
  * Redistributions in source code form must reproduce the above
  * copyright and this condition.
  *
  * The contents of this file are subject to the GNU General Public
  * License, Version 2 (the "License"); you may not use this file
  * except in compliance with the License. A copy of the License is
  * available at http://www.opensource.org/licenses/gpl-license.php.
  *
  * $Revision$
  * $Date$
  * $State$
  */
 package org.jdesktop.wonderland.client.jme;
 
 import java.awt.BorderLayout;
 import java.awt.Canvas;
 import java.awt.GridBagConstraints;
 import java.awt.GridBagLayout;
 import java.awt.Insets;
 import java.util.Locale;
 import java.util.ResourceBundle;
 import java.util.logging.Level;
 import java.util.logging.Logger;
 import javax.swing.JLabel;
 import javax.swing.JPanel;
 import org.jdesktop.mtgame.FrameRateListener;
 import org.jdesktop.mtgame.WorldManager;
 import org.jdesktop.wonderland.client.ClientContext;
 import org.jdesktop.wonderland.client.comms.WonderlandSession;
 import org.jdesktop.wonderland.client.help.WebBrowserLauncher;
 import org.jdesktop.wonderland.client.jme.artimport.CellViewerFrame;
 import org.jdesktop.wonderland.client.jme.artimport.ImportSessionFrame;
 import org.jdesktop.wonderland.common.LogControl;
 
 /**
  * The Main JFrame for the wonderland jme client
  * 
  * @author  paulby
  */
 public class MainFrame extends javax.swing.JFrame {
     private static final ResourceBundle bundle = java.util.ResourceBundle.getBundle("org/jdesktop/wonderland/client/jme/resources/bundle", Locale.getDefault());
 
     JPanel mainPanel = new JPanel();
     Canvas canvas = null;
     JLabel fpsLabel = new JLabel("FPS: ");
     private JPanel contentPane;
     
     private ImportSessionFrame importSessionFrame = null;
     private CellViewerFrame cellViewerFrame = null;
     
     static {
         new LogControl(MainFrame.class, "/org/jdesktop/wonderland/client/jme/resources/logging.properties");
     }
     
     /** Creates new form MainFrame */
     public MainFrame(WorldManager wm, int width, int height) {
         initComponents();
         
         // make the canvas:
         canvas = wm.getRenderManager().createCanvas(width, height);
         canvas.setVisible(true);
         wm.getRenderManager().setFrameRateListener(new FrameRateListener() {
             public void currentFramerate(float framerate) {
                 fpsLabel.setText("FPS: "+framerate);
             }                
         }, 100);
         wm.getRenderManager().setCurrentCanvas(canvas);
 
         contentPane = (JPanel) this.getContentPane();
         contentPane.setLayout(new BorderLayout());
         mainPanel.setLayout(new GridBagLayout());
        setTitle(java.util.ResourceBundle.getBundle("org/jdesktop/wonderland/client/jme/resources/bundle").getString("Wonderland"));
 
         contentPane.add(mainPanel, BorderLayout.NORTH);
         mainPanel.add(fpsLabel,
                 new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0,
                 GridBagConstraints.CENTER,
                 GridBagConstraints.HORIZONTAL, new Insets(5, 5, 0,
                 5), 0, 0));
 
         canvas.setBounds(0, 0, width, height);
         contentPane.add(canvas, BorderLayout.CENTER);
 
         pack();
     }
 
     /**
      * Returns the canvas of the frame.
      */
     public Canvas getCanvas () {
 	return canvas;
     }
 
     /** This method is called from within the constructor to
      * initialize the form.
      * WARNING: Do NOT modify this code. The content of this method is
      * always regenerated by the Form Editor.
      */
     @SuppressWarnings("unchecked")
     // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
     private void initComponents() {
 
         jMenuBar2 = new javax.swing.JMenuBar();
         jMenu3 = new javax.swing.JMenu();
         exitMI = new javax.swing.JMenuItem();
         jMenu4 = new javax.swing.JMenu();
         toolsMenu = new javax.swing.JMenu();
         modelImportMI = new javax.swing.JMenuItem();
         cellViewerMI = new javax.swing.JMenuItem();
         jMenu1 = new javax.swing.JMenu();
         jMenuItem1 = new javax.swing.JMenuItem();
 
         setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
 
        jMenu3.setText(bundle.getString("File")); // NOI18N
 
        exitMI.setText(bundle.getString("Exit")); // NOI18N
         exitMI.addActionListener(new java.awt.event.ActionListener() {
             public void actionPerformed(java.awt.event.ActionEvent evt) {
                 exitMIActionPerformed(evt);
             }
         });
         jMenu3.add(exitMI);
 
         jMenuBar2.add(jMenu3);
 
        jMenu4.setText(bundle.getString("Edit")); // NOI18N
         jMenuBar2.add(jMenu4);
 
        toolsMenu.setText(bundle.getString("Tools")); // NOI18N
 
        modelImportMI.setText(bundle.getString("Model_Importer...")); // NOI18N
         modelImportMI.addActionListener(new java.awt.event.ActionListener() {
             public void actionPerformed(java.awt.event.ActionEvent evt) {
                 modelImportMIActionPerformed(evt);
             }
         });
         toolsMenu.add(modelImportMI);
 
        java.util.ResourceBundle bundle = java.util.ResourceBundle.getBundle("org/jdesktop/wonderland/client/jme/resources/bundle"); // NOI18N
        cellViewerMI.setText(bundle.getString("Cell_Viewer...")); // NOI18N
         cellViewerMI.addActionListener(new java.awt.event.ActionListener() {
             public void actionPerformed(java.awt.event.ActionEvent evt) {
                 cellViewerMIActionPerformed(evt);
             }
         });
         toolsMenu.add(cellViewerMI);
 
         jMenuBar2.add(toolsMenu);
 
        jMenu1.setText(bundle.getString("Help")); // NOI18N
 
        jMenuItem1.setText(bundle.getString("User_Guide")); // NOI18N
         jMenuItem1.addActionListener(new java.awt.event.ActionListener() {
             public void actionPerformed(java.awt.event.ActionEvent evt) {
                 help(evt);
             }
         });
         jMenu1.add(jMenuItem1);
 
         jMenuBar2.add(jMenu1);
 
         setJMenuBar(jMenuBar2);
 
         pack();
     }// </editor-fold>//GEN-END:initComponents
 
 private void exitMIActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_exitMIActionPerformed
 // TODO add your handling code here:
     System.exit(0);
 }//GEN-LAST:event_exitMIActionPerformed
 
 private void modelImportMIActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_modelImportMIActionPerformed
 if (importSessionFrame==null) 
         importSessionFrame = new ImportSessionFrame();
     importSessionFrame.setVisible(true);
 }//GEN-LAST:event_modelImportMIActionPerformed
 
 private void cellViewerMIActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cellViewerMIActionPerformed
     if (cellViewerFrame==null) {
         // TODO handle multiple sessions
         WonderlandSession session = ClientContextJME.getWonderlandSessionManager().getSessions().next();
         cellViewerFrame = new CellViewerFrame(session);
     }
     cellViewerFrame.setVisible(true);
 }//GEN-LAST:event_cellViewerMIActionPerformed
 
 private void help(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_help
     try {
         /* Just launch a browser for now */
         WebBrowserLauncher.openURL("http://wonderland.dev.java.net");//GEN-LAST:event_help
     } catch (Exception ex) {
         Logger.getLogger(MainFrame.class.getName()).log(Level.WARNING, null, ex);
     }
 }
 
 
     // Variables declaration - do not modify//GEN-BEGIN:variables
     private javax.swing.JMenuItem cellViewerMI;
     private javax.swing.JMenuItem exitMI;
     private javax.swing.JMenu jMenu1;
     private javax.swing.JMenu jMenu3;
     private javax.swing.JMenu jMenu4;
     private javax.swing.JMenuBar jMenuBar2;
     private javax.swing.JMenuItem jMenuItem1;
     private javax.swing.JMenuItem modelImportMI;
     private javax.swing.JMenu toolsMenu;
     // End of variables declaration//GEN-END:variables
 
 }
