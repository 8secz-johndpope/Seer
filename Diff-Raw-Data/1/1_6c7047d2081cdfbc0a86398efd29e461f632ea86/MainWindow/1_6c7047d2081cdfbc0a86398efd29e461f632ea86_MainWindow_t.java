 /*
  * MainWindow.java
  *
  * Created on 2010/05/16, 0:56:28
  */
 
 package main;
 import java.awt.event.KeyEvent;
 
 /**
  *
  * @author yayugu
  */
 public class MainWindow extends javax.swing.JFrame {
     Data data = Data.getInstance();
     boolean ctrlPressed =  false;
 
     /** Creates new form MainWindow */
     public MainWindow() {
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
 
         textArea = new javax.swing.JTextArea();
         labelCount = new javax.swing.JLabel();
 
         setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
         setTitle("そーめん大陸");
 
         textArea.setColumns(20);
         textArea.setLineWrap(true);
         textArea.setRows(5);
        textArea.setMinimumSize(new java.awt.Dimension(1, 1));
         textArea.addKeyListener(new java.awt.event.KeyAdapter() {
             public void keyPressed(java.awt.event.KeyEvent evt) {
                 MainWindow.this.keyPressed(evt);
             }
             public void keyReleased(java.awt.event.KeyEvent evt) {
                 MainWindow.this.keyReleased(evt);
             }
         });
 
         labelCount.setText("140");
         labelCount.addMouseListener(new java.awt.event.MouseAdapter() {
             public void mouseClicked(java.awt.event.MouseEvent evt) {
                 labelCountClick(evt);
             }
         });
 
         javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
         getContentPane().setLayout(layout);
         layout.setHorizontalGroup(
             layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
             .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                 .addContainerGap()
                 .addComponent(labelCount))
             .addComponent(textArea, javax.swing.GroupLayout.DEFAULT_SIZE, 254, Short.MAX_VALUE)
         );
         layout.setVerticalGroup(
             layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
             .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                 .addComponent(textArea, javax.swing.GroupLayout.DEFAULT_SIZE, 75, Short.MAX_VALUE)
                 .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                 .addComponent(labelCount))
         );
 
         pack();
     }// </editor-fold>//GEN-END:initComponents
 
     private void labelCountClick(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_labelCountClick
         if(javax.swing.SwingUtilities.isRightMouseButton(evt)){
             java.awt.EventQueue.invokeLater(new Runnable() {
                 public void run() {
                     new AuthWindow().setVisible(true);
                 }
             });            
         } else if(javax.swing.SwingUtilities.isMiddleMouseButton(evt)){
 
         } else if(javax.swing.SwingUtilities.isLeftMouseButton(evt)){
             this.dispose();
             this.setUndecorated( !this.isUndecorated() );
             this.setVisible( true );
         }
     }//GEN-LAST:event_labelCountClick
 
     private void keyPressed(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_keyPressed
         if(evt.getKeyCode() == KeyEvent.VK_ENTER && ctrlPressed){
             try{
                 data.twitter.updateStatus(textArea.getText());
             }catch(Exception e){
                 e.printStackTrace();
             }
             textArea.setText("");
         } else if(evt.getKeyCode() == KeyEvent.VK_CONTROL) {
             ctrlPressed = true;
         }
         labelCount.setText(Integer.toString(140 - textArea.getText().length()));
     }//GEN-LAST:event_keyPressed
 
     private void keyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_keyReleased
         if(evt.getKeyCode() == KeyEvent.VK_CONTROL) {
             ctrlPressed = false;
         }
     }//GEN-LAST:event_keyReleased
 
 
     // Variables declaration - do not modify//GEN-BEGIN:variables
     private javax.swing.JLabel labelCount;
     private javax.swing.JTextArea textArea;
     // End of variables declaration//GEN-END:variables
 
 }
