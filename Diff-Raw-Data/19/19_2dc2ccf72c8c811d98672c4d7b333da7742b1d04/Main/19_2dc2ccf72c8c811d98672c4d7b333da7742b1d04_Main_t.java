 /*
  * To change this template, choose Tools | Templates
  * and open the template in the editor.
  */
 package my.triviagame.gui;
 
 import java.io.FileNotFoundException;
 import java.io.IOException;
 import java.util.logging.Level;
 import java.util.logging.Logger;
 import javax.swing.JOptionPane;
 import my.triviagame.bll.Database;
 import my.triviagame.dal.DALException;
 
 /**
  *
  * @author Yuval
  */
 public class Main extends javax.swing.JFrame {
     /**
      * Creates new form Main
      */
    public Main() throws DALException {
         initComponents();
         setLocationRelativeTo(null);
        try {
             Database.loadProperties();
        } catch (FileNotFoundException ex) {
            JOptionPane.showMessageDialog(this, "Oops.. The database configuration file is missing.");
            System.exit(1);
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "Oops.. Failed to open the database configuration file.\r\nPlease check the log file for more details.");
            System.exit(2);
        }

        try{
             Database.openConnection();
         }
         catch (Exception ex)
         {
             JOptionPane.showMessageDialog(this, "Oops.. We couldn't connect to the database.\r\nPlease check the log file for more details.");
            System.exit(3);
         }
     }
 
     /**
      * This method is called from within the constructor to initialize the form.
      * WARNING: Do NOT modify this code. The content of this method is always
      * regenerated by the Form Editor.
      */
     @SuppressWarnings("unchecked")
     // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
     private void initComponents() {
 
         jPanel1 = new javax.swing.JPanel();
         jLabel2 = new javax.swing.JLabel();
         jButton1 = new javax.swing.JButton();
         jButton2 = new javax.swing.JButton();
         jButton3 = new javax.swing.JButton();
         jMenuBar1 = new javax.swing.JMenuBar();
         jMenu1 = new javax.swing.JMenu();
         jMenuItem1 = new javax.swing.JMenuItem();
         jMenuItem3 = new javax.swing.JMenuItem();
         jMenu2 = new javax.swing.JMenu();
         jMenuItem4 = new javax.swing.JMenuItem();
         jMenuItem5 = new javax.swing.JMenuItem();
         jMenu3 = new javax.swing.JMenu();
         jMenuItem6 = new javax.swing.JMenuItem();
 
         setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
         setTitle("Trivia Challenge");
         setBackground(new java.awt.Color(255, 255, 255));
         setForeground(java.awt.Color.white);
         setResizable(false);
 
         jPanel1.setBackground(new java.awt.Color(255, 255, 255));
 
         jLabel2.setIcon(new javax.swing.ImageIcon(getClass().getResource("/my/triviagame/gui/resources/Logo.jpg"))); // NOI18N
         jLabel2.setText("jLabel2");
 
         jButton1.setText("New Game");
         jButton1.addActionListener(new java.awt.event.ActionListener() {
             public void actionPerformed(java.awt.event.ActionEvent evt) {
                 jButton1ActionPerformed(evt);
             }
         });
 
         jButton2.setText("Discover");
         jButton2.addActionListener(new java.awt.event.ActionListener() {
             public void actionPerformed(java.awt.event.ActionEvent evt) {
                 jButton2ActionPerformed(evt);
             }
         });
 
         jButton3.setText("Import Files");
         jButton3.addActionListener(new java.awt.event.ActionListener() {
             public void actionPerformed(java.awt.event.ActionEvent evt) {
                 jButton3ActionPerformed(evt);
             }
         });
 
         javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
         jPanel1.setLayout(jPanel1Layout);
         jPanel1Layout.setHorizontalGroup(
             jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
             .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                 .addContainerGap(100, Short.MAX_VALUE)
                 .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                     .addComponent(jButton3, javax.swing.GroupLayout.PREFERRED_SIZE, 661, javax.swing.GroupLayout.PREFERRED_SIZE)
                     .addComponent(jButton2, javax.swing.GroupLayout.PREFERRED_SIZE, 661, javax.swing.GroupLayout.PREFERRED_SIZE)
                     .addComponent(jButton1, javax.swing.GroupLayout.PREFERRED_SIZE, 661, javax.swing.GroupLayout.PREFERRED_SIZE)
                     .addComponent(jLabel2, javax.swing.GroupLayout.PREFERRED_SIZE, 661, javax.swing.GroupLayout.PREFERRED_SIZE))
                 .addGap(94, 94, 94))
         );
         jPanel1Layout.setVerticalGroup(
             jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
             .addGroup(jPanel1Layout.createSequentialGroup()
                 .addContainerGap()
                 .addComponent(jLabel2, javax.swing.GroupLayout.PREFERRED_SIZE, 287, javax.swing.GroupLayout.PREFERRED_SIZE)
                 .addGap(39, 39, 39)
                 .addComponent(jButton1, javax.swing.GroupLayout.PREFERRED_SIZE, 55, javax.swing.GroupLayout.PREFERRED_SIZE)
                 .addGap(35, 35, 35)
                 .addComponent(jButton2, javax.swing.GroupLayout.PREFERRED_SIZE, 52, javax.swing.GroupLayout.PREFERRED_SIZE)
                 .addGap(35, 35, 35)
                 .addComponent(jButton3, javax.swing.GroupLayout.PREFERRED_SIZE, 53, javax.swing.GroupLayout.PREFERRED_SIZE)
                 .addContainerGap(40, Short.MAX_VALUE))
         );
 
         jMenu1.setText("Game");
 
         jMenuItem1.setText("New");
         jMenuItem1.addActionListener(new java.awt.event.ActionListener() {
             public void actionPerformed(java.awt.event.ActionEvent evt) {
                 jMenuItem1ActionPerformed(evt);
             }
         });
         jMenu1.add(jMenuItem1);
 
         jMenuItem3.setText("Highscores");
         jMenu1.add(jMenuItem3);
 
         jMenuBar1.add(jMenu1);
 
         jMenu2.setText("Tools");
 
         jMenuItem4.setText("Import Files...");
         jMenuItem4.addActionListener(new java.awt.event.ActionListener() {
             public void actionPerformed(java.awt.event.ActionEvent evt) {
                 jMenuItem4ActionPerformed(evt);
             }
         });
         jMenu2.add(jMenuItem4);
 
         jMenuItem5.setText("Discover...");
         jMenuItem5.addActionListener(new java.awt.event.ActionListener() {
             public void actionPerformed(java.awt.event.ActionEvent evt) {
                 jMenuItem5ActionPerformed(evt);
             }
         });
         jMenu2.add(jMenuItem5);
 
         jMenuBar1.add(jMenu2);
 
         jMenu3.setText("Help");
 
         jMenuItem6.setText("About");
         jMenuItem6.addActionListener(new java.awt.event.ActionListener() {
             public void actionPerformed(java.awt.event.ActionEvent evt) {
                 jMenuItem6ActionPerformed(evt);
             }
         });
         jMenu3.add(jMenuItem6);
 
         jMenuBar1.add(jMenu3);
 
         setJMenuBar(jMenuBar1);
 
         javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
         getContentPane().setLayout(layout);
         layout.setHorizontalGroup(
             layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
             .addGroup(layout.createSequentialGroup()
                 .addContainerGap()
                 .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                 .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
         );
         layout.setVerticalGroup(
             layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
             .addGroup(layout.createSequentialGroup()
                 .addContainerGap()
                 .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                 .addContainerGap())
         );
 
         pack();
     }// </editor-fold>//GEN-END:initComponents
 
     private void jMenuItem6ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem6ActionPerformed
         About aboutForm = new About();
         aboutForm.show(true);
     }//GEN-LAST:event_jMenuItem6ActionPerformed
 
     private void jMenuItem4ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem4ActionPerformed
         Import importForm = new Import();
         importForm.show(true);
     }//GEN-LAST:event_jMenuItem4ActionPerformed
 
     private void jMenuItem5ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem5ActionPerformed
         Discover discoverForm = new Discover();
         discoverForm.show(true);
     }//GEN-LAST:event_jMenuItem5ActionPerformed
 
     private void jMenuItem1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem1ActionPerformed
         NewGame newGameForm = new NewGame();
         newGameForm.show(true);
     }//GEN-LAST:event_jMenuItem1ActionPerformed
 
     private void jButton1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton1ActionPerformed
         NewGame newGameForm = new NewGame();
         newGameForm.show(true);
     }//GEN-LAST:event_jButton1ActionPerformed
 
     private void jButton2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton2ActionPerformed
         Discover discoverForm = new Discover();
         discoverForm.show(true);
     }//GEN-LAST:event_jButton2ActionPerformed
 
     private void jButton3ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton3ActionPerformed
         Import importForm = new Import();
         importForm.show(true);
     }//GEN-LAST:event_jButton3ActionPerformed
 
     /**
      * @param args the command line arguments
      */
     public static void main(String args[]) {
         /*
          * Set the Nimbus look and feel
          */
         //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
         /*
          * If Nimbus (introduced in Java SE 6) is not available, stay with the
          * default look and feel. For details see
          * http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html
          */
         try {
             for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                 if ("Nimbus".equals(info.getName())) {
                     javax.swing.UIManager.setLookAndFeel(info.getClassName());
                     break;
                 }
             }
         } catch (ClassNotFoundException ex) {
             java.util.logging.Logger.getLogger(Main.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
         } catch (InstantiationException ex) {
             java.util.logging.Logger.getLogger(Main.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
         } catch (IllegalAccessException ex) {
             java.util.logging.Logger.getLogger(Main.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
         } catch (javax.swing.UnsupportedLookAndFeelException ex) {
             java.util.logging.Logger.getLogger(Main.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
         }
         //</editor-fold>
 
         /*
          * Create and display the form
          */
         java.awt.EventQueue.invokeLater(new Runnable() {
 
             public void run() {
                 try {
                     new Main().setVisible(true);
                 } catch (Exception ex) {
                     Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
                 }
             }
         });
     }
     // Variables declaration - do not modify//GEN-BEGIN:variables
     private javax.swing.JButton jButton1;
     private javax.swing.JButton jButton2;
     private javax.swing.JButton jButton3;
     private javax.swing.JLabel jLabel2;
     private javax.swing.JMenu jMenu1;
     private javax.swing.JMenu jMenu2;
     private javax.swing.JMenu jMenu3;
     private javax.swing.JMenuBar jMenuBar1;
     private javax.swing.JMenuItem jMenuItem1;
     private javax.swing.JMenuItem jMenuItem3;
     private javax.swing.JMenuItem jMenuItem4;
     private javax.swing.JMenuItem jMenuItem5;
     private javax.swing.JMenuItem jMenuItem6;
     private javax.swing.JPanel jPanel1;
     // End of variables declaration//GEN-END:variables
 }
