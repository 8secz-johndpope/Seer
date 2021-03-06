 /*
  * To change this template, choose Tools | Templates
  * and open the template in the editor.
  */
 
 /*
  * Users.java
  *
  * Created on 23/02/2013, 09:28:53
  */
 
 package restaurante.UI;
 
 import restaurante.UI.CustomComponents.JButton;
 import restaurante.beans.BasicRegister;
 
 /**
  *
  * @author aluno
  */
 public abstract class FrmNavToolbar<Register extends BasicRegister> extends javax.swing.JFrame {
  
     /** Creates new form Users */
     public FrmNavToolbar() {
         initComponents();
     }
 
     /** This method is called from within the constructor to
      * initialize the form.
      * WARNING: Do NOT modify this code. The content of this method is
      * always regenerated by the Form Editor.
      */
     @SuppressWarnings("unchecked")
     // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
     private void initComponents() {
 
         jToolBar1 = new javax.swing.JToolBar();
         btnFirst = new JButton();
         btnPrevious = new JButton();
         btnNext = new restaurante.UI.CustomComponents.JButton();
         btnLast = new restaurante.UI.CustomComponents.JButton();
         jSeparator1 = new javax.swing.JToolBar.Separator();
         btnSave = new restaurante.UI.CustomComponents.JButton();
         btnNew = new restaurante.UI.CustomComponents.JButton();
         btnDelete = new restaurante.UI.CustomComponents.JButton();
 
         setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
 
         jToolBar1.setRollover(true);
         jToolBar1.setMaximumSize(new java.awt.Dimension(238, 33));
         jToolBar1.setMinimumSize(new java.awt.Dimension(232, 33));
         jToolBar1.setPreferredSize(new java.awt.Dimension(238, 33));
 
         btnFirst.setIcon(new javax.swing.ImageIcon(getClass().getResource("/restaurante/Images/Primeiro.png"))); // NOI18N
         btnFirst.setDebugGraphicsOptions(javax.swing.DebugGraphics.BUFFERED_OPTION);
         btnFirst.setDoubleBuffered(true);
         btnFirst.setFocusable(false);
         btnFirst.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
         btnFirst.setMaximumSize(new java.awt.Dimension(32, 32));
         btnFirst.setMinimumSize(new java.awt.Dimension(32, 32));
         btnFirst.setPreferredSize(new java.awt.Dimension(32, 32));
         btnFirst.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
         btnFirst.addActionListener(new java.awt.event.ActionListener() {
             public void actionPerformed(java.awt.event.ActionEvent evt) {
                 btnFirstActionPerformed(evt);
             }
         });
         jToolBar1.add(btnFirst);
 
         btnPrevious.setIcon(new javax.swing.ImageIcon(getClass().getResource("/restaurante/Images/Voltar.png"))); // NOI18N
         btnPrevious.setDebugGraphicsOptions(javax.swing.DebugGraphics.BUFFERED_OPTION);
         btnPrevious.setDoubleBuffered(true);
         btnPrevious.setFocusable(false);
         btnPrevious.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
         btnPrevious.setMaximumSize(new java.awt.Dimension(32, 32));
         btnPrevious.setMinimumSize(new java.awt.Dimension(32, 32));
         btnPrevious.setPreferredSize(new java.awt.Dimension(32, 32));
         btnPrevious.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
         btnPrevious.addActionListener(new java.awt.event.ActionListener() {
             public void actionPerformed(java.awt.event.ActionEvent evt) {
                 btnPreviousActionPerformed(evt);
             }
         });
         jToolBar1.add(btnPrevious);
 
         btnNext.setIcon(new javax.swing.ImageIcon(getClass().getResource("/restaurante/Images/Pr├│ximo.png"))); // NOI18N
         btnNext.setFocusable(false);
         btnNext.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
         btnNext.setMaximumSize(new java.awt.Dimension(32, 32));
         btnNext.setMinimumSize(new java.awt.Dimension(32, 32));
         btnNext.setPreferredSize(new java.awt.Dimension(32, 32));
         btnNext.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
         btnNext.addActionListener(new java.awt.event.ActionListener() {
             public void actionPerformed(java.awt.event.ActionEvent evt) {
                 btnNextActionPerformed(evt);
             }
         });
         jToolBar1.add(btnNext);
 
         btnLast.setIcon(new javax.swing.ImageIcon(getClass().getResource("/restaurante/Images/├Ültimo.png"))); // NOI18N
         btnLast.setFocusable(false);
         btnLast.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
         btnLast.setMaximumSize(new java.awt.Dimension(32, 32));
         btnLast.setMinimumSize(new java.awt.Dimension(32, 32));
         btnLast.setPreferredSize(new java.awt.Dimension(32, 32));
         btnLast.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
         btnLast.addActionListener(new java.awt.event.ActionListener() {
             public void actionPerformed(java.awt.event.ActionEvent evt) {
                 btnLastActionPerformed(evt);
             }
         });
         jToolBar1.add(btnLast);
         jToolBar1.add(jSeparator1);
 
         btnSave.setIcon(new javax.swing.ImageIcon(getClass().getResource("/restaurante/Images/Salvar.png"))); // NOI18N
         btnSave.setFocusable(false);
         btnSave.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
         btnSave.setMaximumSize(new java.awt.Dimension(32, 32));
         btnSave.setMinimumSize(new java.awt.Dimension(32, 32));
         btnSave.setPreferredSize(new java.awt.Dimension(32, 32));
         btnSave.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
         btnSave.addActionListener(new java.awt.event.ActionListener() {
             public void actionPerformed(java.awt.event.ActionEvent evt) {
                 btnSaveActionPerformed(evt);
             }
         });
         jToolBar1.add(btnSave);
 
         btnNew.setIcon(new javax.swing.ImageIcon(getClass().getResource("/restaurante/Images/1362250667_New_File.png"))); // NOI18N
         btnNew.setFocusable(false);
         btnNew.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
         btnNew.setMaximumSize(new java.awt.Dimension(32, 32));
         btnNew.setMinimumSize(new java.awt.Dimension(32, 32));
         btnNew.setPreferredSize(new java.awt.Dimension(32, 32));
         btnNew.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
         btnNew.addActionListener(new java.awt.event.ActionListener() {
             public void actionPerformed(java.awt.event.ActionEvent evt) {
                 btnNewActionPerformed(evt);
             }
         });
         jToolBar1.add(btnNew);
 
         btnDelete.setIcon(new javax.swing.ImageIcon(getClass().getResource("/restaurante/Images/Excluir.png"))); // NOI18N
         btnDelete.setFocusable(false);
         btnDelete.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
         btnDelete.setMaximumSize(new java.awt.Dimension(32, 32));
         btnDelete.setMinimumSize(new java.awt.Dimension(32, 32));
         btnDelete.setPreferredSize(new java.awt.Dimension(32, 32));
         btnDelete.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
         btnDelete.addActionListener(new java.awt.event.ActionListener() {
             public void actionPerformed(java.awt.event.ActionEvent evt) {
                 btnDeleteActionPerformed(evt);
             }
         });
         jToolBar1.add(btnDelete);
 
         javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
         getContentPane().setLayout(layout);
         layout.setHorizontalGroup(
             layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
             .addGroup(layout.createSequentialGroup()
                 .addComponent(jToolBar1, javax.swing.GroupLayout.PREFERRED_SIZE, 359, javax.swing.GroupLayout.PREFERRED_SIZE)
                 .addContainerGap(291, Short.MAX_VALUE))
         );
         layout.setVerticalGroup(
             layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
             .addGroup(layout.createSequentialGroup()
                 .addComponent(jToolBar1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                 .addContainerGap(65, Short.MAX_VALUE))
         );
 
         pack();
     }// </editor-fold>//GEN-END:initComponents
 
     private void btnFirstActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnFirstActionPerformed
         goToFirstRegister();
     }//GEN-LAST:event_btnFirstActionPerformed
 
 private void btnPreviousActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnPreviousActionPerformed
         goToPreviousRegister();
 }//GEN-LAST:event_btnPreviousActionPerformed
 
 private void btnDeleteActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnDeleteActionPerformed
         deleteRegister();
 }//GEN-LAST:event_btnDeleteActionPerformed
 
 private void btnNextActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnNextActionPerformed
         goToNextRegister();
 }//GEN-LAST:event_btnNextActionPerformed
 
 private void btnLastActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnLastActionPerformed
         goToLastRegister();
 }//GEN-LAST:event_btnLastActionPerformed
 
 private void btnSaveActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnSaveActionPerformed
         saveRegister();
 }//GEN-LAST:event_btnSaveActionPerformed
 
 private void btnNewActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnNewActionPerformed
         newRegister();
 }//GEN-LAST:event_btnNewActionPerformed
 
     // Variables declaration - do not modify//GEN-BEGIN:variables
     private javax.swing.JButton btnDelete;
     private javax.swing.JButton btnFirst;
     private javax.swing.JButton btnLast;
     private javax.swing.JButton btnNew;
     private javax.swing.JButton btnNext;
     private javax.swing.JButton btnPrevious;
     private javax.swing.JButton btnSave;
     private javax.swing.JToolBar.Separator jSeparator1;
     private javax.swing.JToolBar jToolBar1;
     // End of variables declaration//GEN-END:variables
     
     private Register[] list;
     
    public void setListOfRegisters(Register[] registerList) {
        this.list = registerList;
    }
    
     private int getListSize() {
         return list.length;
     }
     
     private int currentRegisterPosition = -1;
     
     public abstract boolean saveRegister();
     
     public abstract boolean deleteRegister();
     
     public void newRegister() {
         currentRegisterPosition = -1;
     }
     
     public void goToFirstRegister() {
         currentRegisterPosition = 0;
         getValues(list[currentRegisterPosition]);
     }
     
     public void goToPreviousRegister() {
         if(currentRegisterPosition > 0) {
             currentRegisterPosition--;
         }
         getValues(list[currentRegisterPosition]);
     }
     
     public void goToNextRegister() {
         if(currentRegisterPosition < getListSize()) {
             currentRegisterPosition++;
         }
         getValues(list[currentRegisterPosition]);
     }
     
     public void goToLastRegister() {
         currentRegisterPosition = getListSize() - 1;
         getValues(list[currentRegisterPosition]);
     }
 
     public abstract void getValues(Register register);
 
     public abstract void cleanForm();
 }
