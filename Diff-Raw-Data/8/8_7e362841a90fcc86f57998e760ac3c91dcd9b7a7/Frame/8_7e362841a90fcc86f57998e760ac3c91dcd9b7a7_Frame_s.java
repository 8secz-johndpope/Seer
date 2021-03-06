 /*
  * Copyright (C) 2012 Gyver
  *
  * This program is free software: you can redistribute it and/or modify
  * it under the terms of the GNU General Public License as published by
  * the Free Software Foundation, either version 3 of the License, or
  * (at your option) any later version.
  *
  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  * GNU General Public License for more details.
  *
  * You should have received a copy of the GNU General Public License
  * along with this program.  If not, see <http://www.gnu.org/licenses/>.
  */
 package com.gyver.matrixmover.gui;
 
 import com.gyver.matrixmover.gui.component.FileTreeNode;
 import com.gyver.matrixmover.core.Controller;
 import com.gyver.matrixmover.core.MatrixData;
 import com.gyver.matrixmover.core.SceneReader;
 import com.gyver.matrixmover.core.VisualSetup;
 import com.gyver.matrixmover.effect.Effect.EffectName;
 import com.gyver.matrixmover.generator.enums.GeneratorName;
 import com.gyver.matrixmover.mixer.Mixer.MixerName;
 import com.gyver.matrixmover.properties.PropertiesHelper;
 import com.gyver.matrixmover.gui.listener.*;
 import java.io.File;
 import javax.swing.JFileChooser;
 import javax.swing.JOptionPane;
 import javax.swing.JTree;
 import javax.swing.event.TreeExpansionListener;
 import javax.swing.filechooser.FileFilter;
 import javax.swing.tree.DefaultTreeModel;
 import javax.swing.tree.TreeNode;
 
 /**
  * This is a singelton. The Main frame of the MatrixMover gui. Holds all the panels
  * 
  * @author Gyver
  */
 public class Frame extends javax.swing.JFrame {
 
     private PropertiesHelper ph = null;
     private MatrixData md = null;
     private static Frame instance = null;
     private DefaultTreeModel treemodel = null;
 
     /** Creates new form Frame */
     private Frame() {
     }
 
     /**
      * Returns the instance of this.
      * @return
      */
     public static Frame getFrameInstance() {
         if (instance == null) {
             instance = new Frame();
         }
         return instance;
     }
 
     /**
      * Initiates this frame instance
      * @param ph the properties helper
      * @param cont the controller
      */
     public void initFrame(PropertiesHelper ph, Controller cont) {
         this.ph = ph;
         this.md = cont.getMatrixData();
 
         String dir = ph.getScenesDir();
         FileTreeNode root = (FileTreeNode) buildTree(dir);
         treemodel = new DefaultTreeModel(root);
 
         initComponents();
 
         leftLedScreen.init(ph, md);
         rightLedScreen.init(ph, md);
         masterLedScreen.init(ph, md);
         cont.setLedScreens(leftLedScreen, rightLedScreen, masterLedScreen);
 
         setUpComboBoxes();
         generatorSetupLeft.setup(Controller.LEFT_SIDE);
         generatorSetupRight.setup(Controller.RIGHT_SIDE);
 
 
         jTree1.setDoubleBuffered(true);
         PopClickListener pcl = new PopClickListener(jTree1);
         jTree1.addMouseListener(pcl);
 
         HierarchyBrowserUpdater upd = new HierarchyBrowserUpdater(jTree1, root, treemodel, this);
         jTree1.addTreeExpansionListener(upd);
 
     }
 
     /** This method is called from within the constructor to
      * initialize the form.
      * WARNING: Do NOT modify this code. The content of this method is
      * always regenerated by the Form Editor.
      */
     @SuppressWarnings("unchecked")
     // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
     private void initComponents() {
         java.awt.GridBagConstraints gridBagConstraints;
 
         masterSettings1 = new com.gyver.matrixmover.gui.MasterSettings();
         jSplitPane1 = new javax.swing.JSplitPane();
         panelMasterSide = new javax.swing.JPanel();
         logo = new javax.swing.JLabel();
         masterLedScreen = new com.gyver.matrixmover.gui.LedScreen();
         masterPanel = new com.gyver.matrixmover.gui.MasterPanel();
         masterSettings = new com.gyver.matrixmover.gui.MasterSettings();
         jScrollPane1 = new javax.swing.JScrollPane();
         jTree1 = new JTree(treemodel);
         panelGeneratorSide = new javax.swing.JPanel();
         panelLeftGenerator = new javax.swing.JPanel();
         leftLedScreen = new com.gyver.matrixmover.gui.LedScreen();
         generatorSetupLeft = new com.gyver.matrixmover.gui.GeneratorSetup();
         bLoadLeft = new javax.swing.JButton();
         bSaveLeft = new javax.swing.JButton();
         bClearLeft = new javax.swing.JButton();
         bRemoveGeneratorLeft = new javax.swing.JButton();
         bAddGeneratorLeft = new javax.swing.JButton();
         panelRightGenerator = new javax.swing.JPanel();
         rightLedScreen = new com.gyver.matrixmover.gui.LedScreen();
         generatorSetupRight = new com.gyver.matrixmover.gui.GeneratorSetup();
         bLoadRight1 = new javax.swing.JButton();
         bSaveRight1 = new javax.swing.JButton();
         bClearRight1 = new javax.swing.JButton();
         bRemoveGeneratorRight1 = new javax.swing.JButton();
         bAddGeneratorRight1 = new javax.swing.JButton();
 
         setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
         setTitle("MatrixMover");
         setMinimumSize(new java.awt.Dimension(1150, 700));
         setName("FrameMatrixMover"); // NOI18N
         addWindowListener(new java.awt.event.WindowAdapter() {
             public void windowClosing(java.awt.event.WindowEvent evt) {
                 formWindowClosing(evt);
             }
         });
         addComponentListener(new java.awt.event.ComponentAdapter() {
             public void componentResized(java.awt.event.ComponentEvent evt) {
                 formComponentResized(evt);
             }
         });
         getContentPane().setLayout(new java.awt.GridBagLayout());
 
         jSplitPane1.setDividerSize(7);
         jSplitPane1.setResizeWeight(0.22);
         jSplitPane1.setContinuousLayout(true);
         jSplitPane1.setDoubleBuffered(true);
 
         panelMasterSide.setLayout(new java.awt.GridBagLayout());
 
         logo.setIcon(new javax.swing.ImageIcon(getClass().getResource("/logo.png"))); // NOI18N
         gridBagConstraints = new java.awt.GridBagConstraints();
         gridBagConstraints.gridx = 0;
         gridBagConstraints.gridy = 0;
         gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
         panelMasterSide.add(logo, gridBagConstraints);
 
         masterLedScreen.setMinimumSize(new java.awt.Dimension(120, 120));
         masterLedScreen.setPreferredSize(new java.awt.Dimension(120, 120));
         gridBagConstraints = new java.awt.GridBagConstraints();
         gridBagConstraints.gridx = 0;
         gridBagConstraints.gridy = 1;
         gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
         gridBagConstraints.weightx = 1.0;
         gridBagConstraints.weighty = 0.6;
         gridBagConstraints.insets = new java.awt.Insets(0, 5, 0, 5);
         panelMasterSide.add(masterLedScreen, gridBagConstraints);
 
         masterPanel.setMaximumSize(new java.awt.Dimension(270, 130));
         masterPanel.setMinimumSize(new java.awt.Dimension(270, 130));
         masterPanel.setPreferredSize(new java.awt.Dimension(270, 130));
         gridBagConstraints = new java.awt.GridBagConstraints();
         gridBagConstraints.gridx = 0;
         gridBagConstraints.gridy = 2;
         gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
         gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
         panelMasterSide.add(masterPanel, gridBagConstraints);
 
         masterSettings.setMinimumSize(new java.awt.Dimension(111, 150));
         gridBagConstraints = new java.awt.GridBagConstraints();
         gridBagConstraints.gridx = 0;
         gridBagConstraints.gridy = 3;
         gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
         gridBagConstraints.insets = new java.awt.Insets(0, 5, 0, 5);
         panelMasterSide.add(masterSettings, gridBagConstraints);
 
         jScrollPane1.setViewportView(jTree1);
 
         gridBagConstraints = new java.awt.GridBagConstraints();
         gridBagConstraints.gridx = 0;
         gridBagConstraints.gridy = 4;
         gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
         gridBagConstraints.weightx = 1.0;
         gridBagConstraints.weighty = 1.0;
         gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
         panelMasterSide.add(jScrollPane1, gridBagConstraints);
 
         jSplitPane1.setLeftComponent(panelMasterSide);
 
         panelGeneratorSide.setLayout(new java.awt.GridBagLayout());
 
         panelLeftGenerator.setLayout(new java.awt.GridBagLayout());
 
         leftLedScreen.setMinimumSize(new java.awt.Dimension(120, 120));
         leftLedScreen.setPreferredSize(new java.awt.Dimension(120, 120));
         gridBagConstraints = new java.awt.GridBagConstraints();
         gridBagConstraints.gridx = 0;
         gridBagConstraints.gridy = 0;
         gridBagConstraints.gridwidth = 5;
         gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
         gridBagConstraints.weightx = 1.0;
         gridBagConstraints.weighty = 0.6;
         gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
         panelLeftGenerator.add(leftLedScreen, gridBagConstraints);
         gridBagConstraints = new java.awt.GridBagConstraints();
         gridBagConstraints.gridx = 0;
         gridBagConstraints.gridy = 1;
         gridBagConstraints.gridwidth = 5;
         gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
         gridBagConstraints.weightx = 1.0;
         gridBagConstraints.weighty = 1.0;
         gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
         panelLeftGenerator.add(generatorSetupLeft, gridBagConstraints);
 
         bLoadLeft.setIcon(new javax.swing.ImageIcon(getClass().getResource("/folder.png"))); // NOI18N
         bLoadLeft.setMaximumSize(new java.awt.Dimension(32, 32));
         bLoadLeft.setMinimumSize(new java.awt.Dimension(32, 32));
         bLoadLeft.setOpaque(false);
         bLoadLeft.setPreferredSize(new java.awt.Dimension(32, 32));
         bLoadLeft.addActionListener(new java.awt.event.ActionListener() {
             public void actionPerformed(java.awt.event.ActionEvent evt) {
                 bLoadLeftActionPerformed(evt);
             }
         });
         gridBagConstraints = new java.awt.GridBagConstraints();
         gridBagConstraints.gridx = 1;
         gridBagConstraints.gridy = 2;
         gridBagConstraints.insets = new java.awt.Insets(5, 10, 5, 0);
         panelLeftGenerator.add(bLoadLeft, gridBagConstraints);
 
         bSaveLeft.setIcon(new javax.swing.ImageIcon(getClass().getResource("/save_as.png"))); // NOI18N
         bSaveLeft.setMaximumSize(new java.awt.Dimension(32, 32));
         bSaveLeft.setMinimumSize(new java.awt.Dimension(32, 32));
         bSaveLeft.setPreferredSize(new java.awt.Dimension(32, 32));
         bSaveLeft.addActionListener(new java.awt.event.ActionListener() {
             public void actionPerformed(java.awt.event.ActionEvent evt) {
                 bSaveLeftActionPerformed(evt);
             }
         });
         gridBagConstraints = new java.awt.GridBagConstraints();
         gridBagConstraints.gridx = 2;
         gridBagConstraints.gridy = 2;
         gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
         panelLeftGenerator.add(bSaveLeft, gridBagConstraints);
 
         bClearLeft.setIcon(new javax.swing.ImageIcon(getClass().getResource("/delete_2.png"))); // NOI18N
         bClearLeft.setMaximumSize(new java.awt.Dimension(32, 32));
         bClearLeft.setMinimumSize(new java.awt.Dimension(32, 32));
         bClearLeft.setPreferredSize(new java.awt.Dimension(32, 32));
         bClearLeft.addActionListener(new java.awt.event.ActionListener() {
             public void actionPerformed(java.awt.event.ActionEvent evt) {
                 bClearLeftActionPerformed(evt);
             }
         });
         gridBagConstraints = new java.awt.GridBagConstraints();
         gridBagConstraints.gridx = 0;
         gridBagConstraints.gridy = 2;
         gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_END;
         gridBagConstraints.weightx = 1.0;
         gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
         panelLeftGenerator.add(bClearLeft, gridBagConstraints);
 
         bRemoveGeneratorLeft.setIcon(new javax.swing.ImageIcon(getClass().getResource("/minus_2.png"))); // NOI18N
         bRemoveGeneratorLeft.setMaximumSize(new java.awt.Dimension(32, 32));
         bRemoveGeneratorLeft.setMinimumSize(new java.awt.Dimension(32, 32));
         bRemoveGeneratorLeft.setPreferredSize(new java.awt.Dimension(32, 32));
         bRemoveGeneratorLeft.addActionListener(new java.awt.event.ActionListener() {
             public void actionPerformed(java.awt.event.ActionEvent evt) {
                 bRemoveGeneratorLeftActionPerformed(evt);
             }
         });
         gridBagConstraints = new java.awt.GridBagConstraints();
         gridBagConstraints.gridx = 4;
         gridBagConstraints.gridy = 2;
         gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
         gridBagConstraints.weightx = 1.0;
         gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
         panelLeftGenerator.add(bRemoveGeneratorLeft, gridBagConstraints);
 
         bAddGeneratorLeft.setIcon(new javax.swing.ImageIcon(getClass().getResource("/plus_2.png"))); // NOI18N
         bAddGeneratorLeft.setMaximumSize(new java.awt.Dimension(32, 32));
         bAddGeneratorLeft.setMinimumSize(new java.awt.Dimension(32, 32));
         bAddGeneratorLeft.setPreferredSize(new java.awt.Dimension(32, 32));
         bAddGeneratorLeft.addActionListener(new java.awt.event.ActionListener() {
             public void actionPerformed(java.awt.event.ActionEvent evt) {
                 bAddGeneratorLeftActionPerformed(evt);
             }
         });
         gridBagConstraints = new java.awt.GridBagConstraints();
         gridBagConstraints.gridx = 3;
         gridBagConstraints.gridy = 2;
         gridBagConstraints.insets = new java.awt.Insets(5, 10, 5, 0);
         panelLeftGenerator.add(bAddGeneratorLeft, gridBagConstraints);
 
         gridBagConstraints = new java.awt.GridBagConstraints();
         gridBagConstraints.gridx = 0;
         gridBagConstraints.gridy = 0;
         gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
         gridBagConstraints.weightx = 1.0;
         gridBagConstraints.weighty = 1.0;
         panelGeneratorSide.add(panelLeftGenerator, gridBagConstraints);
 
         panelRightGenerator.setLayout(new java.awt.GridBagLayout());
 
         rightLedScreen.setMinimumSize(new java.awt.Dimension(120, 120));
         rightLedScreen.setPreferredSize(new java.awt.Dimension(120, 120));
         gridBagConstraints = new java.awt.GridBagConstraints();
         gridBagConstraints.gridx = 0;
         gridBagConstraints.gridy = 0;
         gridBagConstraints.gridwidth = 5;
         gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
         gridBagConstraints.weightx = 1.0;
         gridBagConstraints.weighty = 0.6;
         gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
         panelRightGenerator.add(rightLedScreen, gridBagConstraints);
         gridBagConstraints = new java.awt.GridBagConstraints();
         gridBagConstraints.gridx = 0;
         gridBagConstraints.gridy = 1;
         gridBagConstraints.gridwidth = 5;
         gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
         gridBagConstraints.weightx = 1.0;
         gridBagConstraints.weighty = 1.0;
         gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
         panelRightGenerator.add(generatorSetupRight, gridBagConstraints);
 
         bLoadRight1.setIcon(new javax.swing.ImageIcon(getClass().getResource("/folder.png"))); // NOI18N
         bLoadRight1.setMaximumSize(new java.awt.Dimension(32, 32));
         bLoadRight1.setMinimumSize(new java.awt.Dimension(32, 32));
         bLoadRight1.setOpaque(false);
         bLoadRight1.setPreferredSize(new java.awt.Dimension(32, 32));
         bLoadRight1.addActionListener(new java.awt.event.ActionListener() {
             public void actionPerformed(java.awt.event.ActionEvent evt) {
                 bLoadRight1ActionPerformed(evt);
             }
         });
         gridBagConstraints = new java.awt.GridBagConstraints();
         gridBagConstraints.gridx = 1;
         gridBagConstraints.gridy = 2;
         gridBagConstraints.insets = new java.awt.Insets(5, 10, 5, 0);
         panelRightGenerator.add(bLoadRight1, gridBagConstraints);
 
         bSaveRight1.setIcon(new javax.swing.ImageIcon(getClass().getResource("/save_as.png"))); // NOI18N
         bSaveRight1.setMaximumSize(new java.awt.Dimension(32, 32));
         bSaveRight1.setMinimumSize(new java.awt.Dimension(32, 32));
         bSaveRight1.setPreferredSize(new java.awt.Dimension(32, 32));
         bSaveRight1.addActionListener(new java.awt.event.ActionListener() {
             public void actionPerformed(java.awt.event.ActionEvent evt) {
                 bSaveRight1ActionPerformed(evt);
             }
         });
         gridBagConstraints = new java.awt.GridBagConstraints();
         gridBagConstraints.gridx = 2;
         gridBagConstraints.gridy = 2;
         gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
         panelRightGenerator.add(bSaveRight1, gridBagConstraints);
 
         bClearRight1.setIcon(new javax.swing.ImageIcon(getClass().getResource("/delete_2.png"))); // NOI18N
         bClearRight1.setMaximumSize(new java.awt.Dimension(32, 32));
         bClearRight1.setMinimumSize(new java.awt.Dimension(32, 32));
         bClearRight1.setPreferredSize(new java.awt.Dimension(32, 32));
         bClearRight1.addActionListener(new java.awt.event.ActionListener() {
             public void actionPerformed(java.awt.event.ActionEvent evt) {
                 bClearRight1ActionPerformed(evt);
             }
         });
         gridBagConstraints = new java.awt.GridBagConstraints();
         gridBagConstraints.gridx = 0;
         gridBagConstraints.gridy = 2;
         gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_END;
         gridBagConstraints.weightx = 1.0;
         gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
         panelRightGenerator.add(bClearRight1, gridBagConstraints);
 
         bRemoveGeneratorRight1.setIcon(new javax.swing.ImageIcon(getClass().getResource("/minus_2.png"))); // NOI18N
         bRemoveGeneratorRight1.setMaximumSize(new java.awt.Dimension(32, 32));
         bRemoveGeneratorRight1.setMinimumSize(new java.awt.Dimension(32, 32));
         bRemoveGeneratorRight1.setPreferredSize(new java.awt.Dimension(32, 32));
         bRemoveGeneratorRight1.addActionListener(new java.awt.event.ActionListener() {
             public void actionPerformed(java.awt.event.ActionEvent evt) {
                 bRemoveGeneratorRight1ActionPerformed(evt);
             }
         });
         gridBagConstraints = new java.awt.GridBagConstraints();
         gridBagConstraints.gridx = 4;
         gridBagConstraints.gridy = 2;
         gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
         gridBagConstraints.weightx = 1.0;
         gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
         panelRightGenerator.add(bRemoveGeneratorRight1, gridBagConstraints);
 
         bAddGeneratorRight1.setIcon(new javax.swing.ImageIcon(getClass().getResource("/plus_2.png"))); // NOI18N
         bAddGeneratorRight1.setMaximumSize(new java.awt.Dimension(32, 32));
         bAddGeneratorRight1.setMinimumSize(new java.awt.Dimension(32, 32));
         bAddGeneratorRight1.setPreferredSize(new java.awt.Dimension(32, 32));
         bAddGeneratorRight1.addActionListener(new java.awt.event.ActionListener() {
             public void actionPerformed(java.awt.event.ActionEvent evt) {
                 bAddGeneratorRight1ActionPerformed(evt);
             }
         });
         gridBagConstraints = new java.awt.GridBagConstraints();
         gridBagConstraints.gridx = 3;
         gridBagConstraints.gridy = 2;
         gridBagConstraints.insets = new java.awt.Insets(5, 10, 5, 0);
         panelRightGenerator.add(bAddGeneratorRight1, gridBagConstraints);
 
         gridBagConstraints = new java.awt.GridBagConstraints();
         gridBagConstraints.gridx = 1;
         gridBagConstraints.gridy = 0;
         gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
         gridBagConstraints.weightx = 1.0;
         gridBagConstraints.weighty = 1.0;
         panelGeneratorSide.add(panelRightGenerator, gridBagConstraints);
 
         jSplitPane1.setRightComponent(panelGeneratorSide);
 
         gridBagConstraints = new java.awt.GridBagConstraints();
         gridBagConstraints.gridx = 0;
         gridBagConstraints.gridy = 0;
         gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
         gridBagConstraints.weightx = 1.0;
         gridBagConstraints.weighty = 1.0;
         getContentPane().add(jSplitPane1, gridBagConstraints);
 
         pack();
     }// </editor-fold>//GEN-END:initComponents
 
     private void formComponentResized(java.awt.event.ComponentEvent evt) {//GEN-FIRST:event_formComponentResized
         leftLedScreen.recomputePixelSize();
         leftLedScreen.repaint();
         rightLedScreen.recomputePixelSize();
         rightLedScreen.repaint();
         masterLedScreen.recomputePixelSize();
         masterLedScreen.repaint();
     }//GEN-LAST:event_formComponentResized
 
     private void formWindowClosing(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_formWindowClosing
 //        Controller.getControllerInstance().saveScenes();
     }//GEN-LAST:event_formWindowClosing
 
     private void bAddGeneratorLeftActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_bAddGeneratorLeftActionPerformed
         Controller.getControllerInstance().getActiveVisualSetup(Controller.LEFT_SIDE).addGeneratorSetup(md);
         generatorSetupLeft.buildGuiFromVisualSetup();
     }//GEN-LAST:event_bAddGeneratorLeftActionPerformed
 
     private void bRemoveGeneratorLeftActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_bRemoveGeneratorLeftActionPerformed
         Controller.getControllerInstance().getActiveVisualSetup(Controller.LEFT_SIDE).removeLastVisualSetup();
         generatorSetupLeft.buildGuiFromVisualSetup();
     }//GEN-LAST:event_bRemoveGeneratorLeftActionPerformed
 
     private void bAddGeneratorRight1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_bAddGeneratorRight1ActionPerformed
         Controller.getControllerInstance().getActiveVisualSetup(Controller.RIGHT_SIDE).addGeneratorSetup(md);
        generatorSetupLeft.buildGuiFromVisualSetup();
     }//GEN-LAST:event_bAddGeneratorRight1ActionPerformed
 
     private void bRemoveGeneratorRight1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_bRemoveGeneratorRight1ActionPerformed
         Controller.getControllerInstance().getActiveVisualSetup(Controller.RIGHT_SIDE).removeLastVisualSetup();
        generatorSetupLeft.buildGuiFromVisualSetup();
     }//GEN-LAST:event_bRemoveGeneratorRight1ActionPerformed
 
     private void bClearLeftActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_bClearLeftActionPerformed
         Object[] options = {"Yes", "no"};
         int n = JOptionPane.showOptionDialog(this, "Clear the whole scene?", "Clear Scene?", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, options[1]);
         if (n == JOptionPane.YES_OPTION) {
             Controller.getControllerInstance().getActiveVisualSetup(Controller.LEFT_SIDE).clear();
             generatorSetupLeft.buildGuiFromVisualSetup();
         }
     }//GEN-LAST:event_bClearLeftActionPerformed
 
     private void bClearRight1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_bClearRight1ActionPerformed
         Object[] options = {"Yes", "no"};
         int n = JOptionPane.showOptionDialog(this, "Clear the whole scene?", "Clear Scene?", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, options[1]);
         if (n == JOptionPane.YES_OPTION) {
             Controller.getControllerInstance().getActiveVisualSetup(Controller.RIGHT_SIDE).clear();
            generatorSetupLeft.buildGuiFromVisualSetup();
         }
     }//GEN-LAST:event_bClearRight1ActionPerformed
 
     private void bSaveLeftActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_bSaveLeftActionPerformed
         saveVisualSetup(Controller.LEFT_SIDE);
     }//GEN-LAST:event_bSaveLeftActionPerformed
 
     private void bLoadLeftActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_bLoadLeftActionPerformed
         loadVisualSetup(Controller.LEFT_SIDE);
     }//GEN-LAST:event_bLoadLeftActionPerformed
 
     private void bSaveRight1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_bSaveRight1ActionPerformed
         saveVisualSetup(Controller.RIGHT_SIDE);
     }//GEN-LAST:event_bSaveRight1ActionPerformed
 
     private void bLoadRight1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_bLoadRight1ActionPerformed
         loadVisualSetup(Controller.RIGHT_SIDE);
     }//GEN-LAST:event_bLoadRight1ActionPerformed
 
     // Variables declaration - do not modify//GEN-BEGIN:variables
     private javax.swing.JButton bAddGeneratorLeft;
     private javax.swing.JButton bAddGeneratorRight1;
     private javax.swing.JButton bClearLeft;
     private javax.swing.JButton bClearRight1;
     private javax.swing.JButton bLoadLeft;
     private javax.swing.JButton bLoadRight1;
     private javax.swing.JButton bRemoveGeneratorLeft;
     private javax.swing.JButton bRemoveGeneratorRight1;
     private javax.swing.JButton bSaveLeft;
     private javax.swing.JButton bSaveRight1;
     private com.gyver.matrixmover.gui.GeneratorSetup generatorSetupLeft;
     private com.gyver.matrixmover.gui.GeneratorSetup generatorSetupRight;
     private javax.swing.JScrollPane jScrollPane1;
     private javax.swing.JSplitPane jSplitPane1;
     private javax.swing.JTree jTree1;
     private com.gyver.matrixmover.gui.LedScreen leftLedScreen;
     private javax.swing.JLabel logo;
     private com.gyver.matrixmover.gui.LedScreen masterLedScreen;
     private com.gyver.matrixmover.gui.MasterPanel masterPanel;
     private com.gyver.matrixmover.gui.MasterSettings masterSettings;
     private com.gyver.matrixmover.gui.MasterSettings masterSettings1;
     private javax.swing.JPanel panelGeneratorSide;
     private javax.swing.JPanel panelLeftGenerator;
     private javax.swing.JPanel panelMasterSide;
     private javax.swing.JPanel panelRightGenerator;
     private com.gyver.matrixmover.gui.LedScreen rightLedScreen;
     // End of variables declaration//GEN-END:variables
 
     private void setUpComboBoxes() {
 //        generatorSetupLeft.setGeneratorListToComboBoxes(GeneratorName.values());
 //        generatorSetupRight.setGeneratorListToComboBoxes(GeneratorName.values());
 //
 //        generatorSetupLeft.setEffectListToComboBoxes(EffectName.values());
 //        generatorSetupRight.setEffectListToComboBoxes(EffectName.values());
 //
 //        generatorSetupLeft.setMixerListToComboBoxes(MixerName.values());
 //        generatorSetupRight.setMixerListToComboBoxes(MixerName.values());
     }
 
     /**
      * Sets the combo boxes to the correct indices if the scene has changed
      * @param side the side of the gui where to set the comboboxes
      * @param newActiveVisualSetup the visual setup to set the comboboxes indices to
      */
     public void setComboBoxesForChangedScene(int side, VisualSetup newActiveVisualSetup) {
 //        if (side == Controller.BOTTOM_SIDE) {
 //            setComboBoxes(generatorSetupLeft, newActiveVisualSetup);
 //        } else if (side == Controller.TOP_SIDE) {
 //            setComboBoxes(generatorSetupRight, newActiveVisualSetup);
 //        }
     }
 
     private void setComboBoxes(GeneratorSetup generatorSetup, VisualSetup newActiveVisualSetup) {
         //remove all from actionlistener when setting a new scene
         //else scene is marked as changed, due to actionlistener fired!
 //        generatorSetup.removeAllFromActionListener();
 
         //FIXME: FIX THIS MESS!!!!
 
 //        generatorSetup.getCbGenerator1().setSelectedItem(newActiveVisualSetup.getGenerator(0).getName());
 //        generatorSetup.getCbGenerator2().setSelectedItem(newActiveVisualSetup.getGenerator(0).getName());
 //        generatorSetup.getCbGenerator3().setSelectedItem(newActiveVisualSetup.getGenerator(0).getName());
 //        generatorSetup.getCbGenerator4().setSelectedItem(newActiveVisualSetup.getGenerator(0).getName());
 //        generatorSetup.getCbGenerator5().setSelectedItem(newActiveVisualSetup.getGenerator(0).getName());
 //        
 //        generatorSetup.getCbEffect1().setSelectedItem(newActiveVisualSetup.getEffect(0).getName());
 //        generatorSetup.getCbEffect2().setSelectedItem(newActiveVisualSetup.getEffect(0).getName());
 //        generatorSetup.getCbEffect3().setSelectedItem(newActiveVisualSetup.getEffect(0).getName());
 //        generatorSetup.getCbEffect4().setSelectedItem(newActiveVisualSetup.getEffect(0).getName());
 //        generatorSetup.getCbEffect5().setSelectedItem(newActiveVisualSetup.getEffect(0).getName());
 //        
 //        generatorSetup.getCbMixer2().setSelectedItem(newActiveVisualSetup.getMixer(0).getName());
 //        generatorSetup.getCbMixer3().setSelectedItem(newActiveVisualSetup.getMixer(0).getName());
 //        generatorSetup.getCbMixer4().setSelectedItem(newActiveVisualSetup.getMixer(0).getName());
 //        generatorSetup.getCbMixer5().setSelectedItem(newActiveVisualSetup.getMixer(0).getName());
 //        
 //        generatorSetup.getIntensitySlider1().setValue(newActiveVisualSetup.getGeneratorIntensity(0));
 //        generatorSetup.getIntensitySlider2().setValue(newActiveVisualSetup.getGeneratorIntensity(0));
 //        generatorSetup.getIntensitySlider3().setValue(newActiveVisualSetup.getGeneratorIntensity(0));
 //        generatorSetup.getIntensitySlider4().setValue(newActiveVisualSetup.getGeneratorIntensity(0));
 //        generatorSetup.getIntensitySlider5().setValue(newActiveVisualSetup.getGeneratorIntensity(0));
 //        
 //        generatorSetup.addAllToActionListener();
 
     }
 
     /**
      * Returns the left generator panel
      * @return the panel
      */
     public GeneratorPanel getLeftGeneratorPanel() {
         return null;//this.effectPanelLeft;
     }
 
     /**
      * Returns the right generator panel
      * @return the panel
      */
     public GeneratorPanel getRightGeneratorPanel() {
         return null;//this.effectPanelRight;
     }
 
     /**
      * Returns the left generator setup panel
      * @return the panel
      */
     public GeneratorSetup getLeftGeneratorSetup() {
         return this.generatorSetupLeft;
     }
 
     /**
      * Returns the right generator setup panel
      * @return the panel
      */
     public GeneratorSetup getRightGeneratorSetup() {
         return this.generatorSetupRight;
     }
 
     /**
      * Returns the master panel
      * @return the panel
      */
     public com.gyver.matrixmover.gui.MasterPanel getMasterPanel() {
         return masterPanel;
     }
 
     /**
      * Sets the audio level
      * @param level the level
      */
     public void setAudioLevel(float[] level) {
         this.masterSettings.setAudioLevel(level);
     }
 
     /**
      * Shows a warning to the user.
      * @param string the warning to display
      */
     public void showWarning(String string) {
         JOptionPane.showMessageDialog(this, string, "Warning", JOptionPane.WARNING_MESSAGE);
     }
 
     private TreeNode buildTree(String dir) {
         FileTreeNode root = new FileTreeNode(new File(dir));
         root.readTree(false);
         return (TreeNode) root;
     }
     
     private void saveVisualSetup(int side){
         JFileChooser chooser = new JFileChooser(ph.getScenesDir());
         chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
         chooser.addChoosableFileFilter(new FileFilter() {
             @Override
             public boolean accept(File f) {
                 if (f.isDirectory()) {
                     return true;
                 }
                 return f.getName().toLowerCase().endsWith(".mms");
             }
 
             @Override
             public String getDescription() {
                 return "MatrixMover Scene";
             }
         });
         chooser.setMultiSelectionEnabled(false);
         if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
             VisualSetup vs = Controller.getControllerInstance().getActiveVisualSetup(side);
             File f = chooser.getSelectedFile();
             System.out.println(f);
             if(!f.toString().endsWith(".mms")){
                 f = new File(f.toString()+".mms");
                 System.out.println("renamed to: "+f);
             }
             SceneReader.saveVisualSetup(vs, f);
             String dir = ph.getScenesDir();
             FileTreeNode root = (FileTreeNode) buildTree(dir);
             treemodel = new DefaultTreeModel(root);
             jTree1.setModel(treemodel);
             treemodel.reload();
             
             for (TreeExpansionListener tel : jTree1.getTreeExpansionListeners()) {
                 jTree1.removeTreeExpansionListener(tel);
             }
             
             HierarchyBrowserUpdater upd = new HierarchyBrowserUpdater(jTree1, root, treemodel, this);
             jTree1.addTreeExpansionListener(upd);
         }
     }
 
     private void loadVisualSetup(int side) {
                 JFileChooser chooser = new JFileChooser(ph.getScenesDir());
         chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
         chooser.addChoosableFileFilter(new FileFilter() {
             @Override
             public boolean accept(File f) {
                 if (f.isDirectory()) {
                     return true;
                 }
                 return f.getName().toLowerCase().endsWith(".mms");
             }
 
             @Override
             public String getDescription() {
                 return "MatrixMover Scene";
             }
         });
         chooser.setMultiSelectionEnabled(false);
         if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
             File f = chooser.getSelectedFile();
             Controller.getControllerInstance().setVisualSetup(SceneReader.loadVisualSetup(f, md), side);
         }
     }
 }
