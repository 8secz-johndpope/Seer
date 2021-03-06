 /*
  * To change this template, choose Tools | Templates
  * and open the template in the editor.
  */
 package view;
 
 import controller.PrimerDesign;
 import javax.swing.JLabel;
 
 /**
  *
  * @author ross
  */
 public class PrimerSelectionPanel extends javax.swing.JPanel {
 
     /**
      * Creates new form PrimerSelectionPanel
      */
     public PrimerSelectionPanel() {
         initComponents();
     }
 
     /**
      * This method is called from within the constructor to initialize the form.
      * WARNING: Do NOT modify this code. The content of this method is always
      * regenerated by the Form Editor.
      */
     @SuppressWarnings("unchecked")
     // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
     private void initComponents() {
 
         titleLabel = new javax.swing.JLabel();
         jScrollPane2 = new javax.swing.JScrollPane();
         instructionTextPane = new javax.swing.JTextPane();
         jScrollPane4 = new javax.swing.JScrollPane();
        sequenceTextArea = new javax.swing.JTextArea(PrimerDesign.start.getInSequence().toString('o', 80));
         forwardPrimerTextField = new javax.swing.JTextField();
         reversePrimerTextField = new javax.swing.JTextField();
         forwardPrimerLabel = new javax.swing.JLabel();
         reversePrimerLabel = new javax.swing.JLabel();
         backButton = new javax.swing.JButton();
         nextButton = new javax.swing.JButton();
         showRulesButton = new javax.swing.JButton();
 
         setToolTipText("");
         setPreferredSize(new java.awt.Dimension(800, 600));
 
         titleLabel.setFont(new java.awt.Font("DejaVu Sans", 0, 24)); // NOI18N
         titleLabel.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
         titleLabel.setText("Primer Design Stage 3");
 
         instructionTextPane.setEditable(false);
         instructionTextPane.setText("Enter the primer pair in the text fields below");
         jScrollPane2.setViewportView(instructionTextPane);
 
         sequenceTextArea.setEditable(false);
         sequenceTextArea.setColumns(20);
         sequenceTextArea.setFont(new java.awt.Font("DejaVu Sans Mono", 0, 13)); // NOI18N
         sequenceTextArea.setRows(5);
         sequenceTextArea.setTabSize(6);
         jScrollPane4.setViewportView(sequenceTextArea);
 
         forwardPrimerTextField.setText("Forward Primer");
 
         reversePrimerTextField.setText("Reverse Primer");
 
         forwardPrimerLabel.setText("Forward Primer:");
 
         reversePrimerLabel.setText("Reverse Primer:");
 
         backButton.setText("Back");
         backButton.addActionListener(new java.awt.event.ActionListener() {
             public void actionPerformed(java.awt.event.ActionEvent evt) {
                 backButtonActionPerformed(evt);
             }
         });
 
         nextButton.setText("Next/Go");
         nextButton.addActionListener(new java.awt.event.ActionListener() {
             public void actionPerformed(java.awt.event.ActionEvent evt) {
                 nextButtonActionPerformed(evt);
             }
         });
 
         showRulesButton.setText("Show Primer Design Rules");
         showRulesButton.addActionListener(new java.awt.event.ActionListener() {
             public void actionPerformed(java.awt.event.ActionEvent evt) {
                 showRulesButtonActionPerformed(evt);
             }
         });
 
         javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
         this.setLayout(layout);
         layout.setHorizontalGroup(
             layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
             .addGroup(layout.createSequentialGroup()
                 .addContainerGap()
                 .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                     .addComponent(titleLabel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                     .addComponent(jScrollPane2, javax.swing.GroupLayout.Alignment.TRAILING)
                     .addComponent(jScrollPane4)
                     .addGroup(layout.createSequentialGroup()
                         .addComponent(forwardPrimerLabel)
                         .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                         .addComponent(forwardPrimerTextField, javax.swing.GroupLayout.PREFERRED_SIZE, 240, javax.swing.GroupLayout.PREFERRED_SIZE)
                         .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 71, Short.MAX_VALUE)
                         .addComponent(reversePrimerLabel)
                         .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                         .addComponent(reversePrimerTextField, javax.swing.GroupLayout.PREFERRED_SIZE, 154, javax.swing.GroupLayout.PREFERRED_SIZE))
                     .addGroup(layout.createSequentialGroup()
                         .addComponent(backButton, javax.swing.GroupLayout.PREFERRED_SIZE, 83, javax.swing.GroupLayout.PREFERRED_SIZE)
                         .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                         .addComponent(showRulesButton)
                         .addGap(191, 191, 191)
                         .addComponent(nextButton, javax.swing.GroupLayout.PREFERRED_SIZE, 83, javax.swing.GroupLayout.PREFERRED_SIZE)))
                 .addContainerGap())
         );
 
         layout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {forwardPrimerTextField, reversePrimerTextField});
 
         layout.setVerticalGroup(
             layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
             .addGroup(layout.createSequentialGroup()
                 .addGap(20, 20, 20)
                 .addComponent(titleLabel)
                 .addGap(18, 18, 18)
                 .addComponent(jScrollPane2, javax.swing.GroupLayout.PREFERRED_SIZE, 50, javax.swing.GroupLayout.PREFERRED_SIZE)
                 .addGap(11, 11, 11)
                 .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                     .addComponent(reversePrimerTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                     .addComponent(forwardPrimerLabel)
                     .addComponent(reversePrimerLabel)
                     .addComponent(forwardPrimerTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                 .addGap(18, 18, 18)
                 .addComponent(jScrollPane4, javax.swing.GroupLayout.PREFERRED_SIZE, 384, javax.swing.GroupLayout.PREFERRED_SIZE)
                 .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                 .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                     .addComponent(backButton)
                     .addComponent(nextButton)
                     .addComponent(showRulesButton))
                 .addContainerGap())
         );
     }// </editor-fold>//GEN-END:initComponents
 
     private void backButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_backButtonActionPerformed
         PrimerDesign.window.remove(PrimerDesign.primerSelect);
         PrimerDesign.window.setVisible(false);
 
         PrimerDesign.window.getContentPane().add(PrimerDesign.area);
         PrimerDesign.window.pack();
         PrimerDesign.window.setVisible(true);
     }//GEN-LAST:event_backButtonActionPerformed
 
     private void nextButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_nextButtonActionPerformed
         PrimerDesign.window.remove(PrimerDesign.primerSelect);
         PrimerDesign.window.setVisible(false);
 
         PrimerDesign.window.getContentPane().add(new JLabel("Next Stage"));
         PrimerDesign.window.pack();
         PrimerDesign.window.setVisible(true);
     }//GEN-LAST:event_nextButtonActionPerformed
 
     private void showRulesButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_showRulesButtonActionPerformed
         // should create a jdialog(?) showing the rules for primer design
     }//GEN-LAST:event_showRulesButtonActionPerformed
 
     // Variables declaration - do not modify//GEN-BEGIN:variables
     private javax.swing.JButton backButton;
     private javax.swing.JLabel forwardPrimerLabel;
     private javax.swing.JTextField forwardPrimerTextField;
     private javax.swing.JTextPane instructionTextPane;
     private javax.swing.JScrollPane jScrollPane2;
     private javax.swing.JScrollPane jScrollPane4;
     private javax.swing.JButton nextButton;
     private javax.swing.JLabel reversePrimerLabel;
     private javax.swing.JTextField reversePrimerTextField;
     private javax.swing.JTextArea sequenceTextArea;
     private javax.swing.JButton showRulesButton;
     private javax.swing.JLabel titleLabel;
     // End of variables declaration//GEN-END:variables
 }
