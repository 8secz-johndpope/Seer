 /*
  * To change this template, choose Tools | Templates
  * and open the template in the editor.
  */
 package interiores.presentation.views;
 
 import interiores.presentation.SwingFrame;
 import java.beans.PropertyChangeEvent;
 
 /**
  *
  * @author hector0193
  */
 public class MainApp extends SwingFrame
 {
 
     /**
      * Creates new form MainView
      */
     public MainApp()
     {
         initComponents();
     }
 
     /**
      * This method is called from within the constructor to initialize the form.
      * WARNING: Do NOT modify this code. The content of this method is always
      * regenerated by the Form Editor.
      */
     @SuppressWarnings("unchecked")
     // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
     private void initComponents()
     {
 
         jTextField1 = new javax.swing.JTextField();
         jButton2 = new javax.swing.JButton();
 
         setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setTitle("Diseño de interiores");
 
         jButton2.setText("Send");
         jButton2.addActionListener(new java.awt.event.ActionListener()
         {
             public void actionPerformed(java.awt.event.ActionEvent evt)
             {
                 jButton2ActionPerformed(evt);
             }
         });
 
         org.jdesktop.layout.GroupLayout layout = new org.jdesktop.layout.GroupLayout(getContentPane());
         getContentPane().setLayout(layout);
         layout.setHorizontalGroup(
             layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
             .add(layout.createSequentialGroup()
                .addContainerGap()
                .add(jTextField1, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 272, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                 .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                 .add(jButton2, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 104, Short.MAX_VALUE)
                 .addContainerGap())
         );
         layout.setVerticalGroup(
             layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
             .add(org.jdesktop.layout.GroupLayout.TRAILING, layout.createSequentialGroup()
                 .addContainerGap(264, Short.MAX_VALUE)
                 .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                     .add(jTextField1, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                     .add(jButton2))
                 .addContainerGap())
         );
 
         pack();
     }// </editor-fold>//GEN-END:initComponents
 
     private void jButton2ActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_jButton2ActionPerformed
     {//GEN-HEADEREND:event_jButton2ActionPerformed
         exec(jTextField1.getText());
         jTextField1.setText("");
     }//GEN-LAST:event_jButton2ActionPerformed
 
     /**
      * @param args the command line arguments
      */
     public static void main(String args[])
     {
         /* Set the Nimbus look and feel */
         //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
         /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
          * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html 
          */
         try
         {
             for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels())
             {
                 if ("Nimbus".equals(info.getName()))
                 {
                     javax.swing.UIManager.setLookAndFeel(info.getClassName());
                     break;
                 }
             }
         } catch (ClassNotFoundException ex)
         {
             java.util.logging.Logger.getLogger(MainApp.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
         } catch (InstantiationException ex)
         {
             java.util.logging.Logger.getLogger(MainApp.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
         } catch (IllegalAccessException ex)
         {
             java.util.logging.Logger.getLogger(MainApp.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
         } catch (javax.swing.UnsupportedLookAndFeelException ex)
         {
             java.util.logging.Logger.getLogger(MainApp.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
         }
         //</editor-fold>
 
         /* Create and display the form */
         java.awt.EventQueue.invokeLater(new Runnable()
         {
             public void run()
             {
                 new MainApp().setVisible(true);
             }
         });
     }
     // Variables declaration - do not modify//GEN-BEGIN:variables
     private javax.swing.JButton jButton2;
     private javax.swing.JTextField jTextField1;
     // End of variables declaration//GEN-END:variables
 
     @Override
     public void propertyChange(PropertyChangeEvent pce)
     {
         throw new UnsupportedOperationException("Not supported yet.");
     }
 }
