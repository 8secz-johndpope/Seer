 /*
  * To change this template, choose Tools | Templates
  * and open the template in the editor.
  */
 package employeesdatabase;
 
 import javax.swing.UIManager;
 import javax.swing.WindowConstants;
 
import javax.swing.JList;

 /**
  *
  * @author Colin Lee
  */
 public class SearchItemForm extends javax.swing.JFrame {
 
    private JList listEmployees;
     /**
      * Creates new form searchItemForm
      */
     public SearchItemForm() {
         initComponents();
     }
 
    public void setListEmployees(JList listEmp) {
        listEmployees = listEmp;
    }

     /**
      * This method is called from within the constructor to initialize the form.
      * WARNING: Do NOT modify this code. The content of this method is always
      * regenerated by the Form Editor.
      */
     @SuppressWarnings("unchecked")
     // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
     private void initComponents() {
 
         searchButton = new javax.swing.JButton();
         searchField = new javax.swing.JTextField();
         jLabel1 = new javax.swing.JLabel();
         searchFoundLabel = new javax.swing.JLabel();
 
         setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
         setTitle("Search Menu");
         setResizable(false);
         addWindowListener(new java.awt.event.WindowAdapter() {
             public void windowOpened(java.awt.event.WindowEvent evt) {
                 formWindowOpened(evt);
             }
         });
 
         searchButton.setText("Search");
         searchButton.addActionListener(new java.awt.event.ActionListener() {
             public void actionPerformed(java.awt.event.ActionEvent evt) {
                 searchButtonActionPerformed(evt);
             }
         });
 
         jLabel1.setText("Emp. Number:");
 
         searchFoundLabel.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
 
         javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
         getContentPane().setLayout(layout);
         layout.setHorizontalGroup(
             layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
             .addGroup(layout.createSequentialGroup()
                 .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                     .addGroup(layout.createSequentialGroup()
                         .addContainerGap()
                         .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                             .addComponent(searchButton, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                             .addGroup(layout.createSequentialGroup()
                                 .addComponent(jLabel1)
                                 .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                 .addComponent(searchField, javax.swing.GroupLayout.PREFERRED_SIZE, 86, javax.swing.GroupLayout.PREFERRED_SIZE))))
                     .addGroup(layout.createSequentialGroup()
                         .addGap(72, 72, 72)
                         .addComponent(searchFoundLabel)))
                 .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
         );
         layout.setVerticalGroup(
             layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
             .addGroup(layout.createSequentialGroup()
                 .addContainerGap()
                 .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                     .addComponent(searchField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                     .addComponent(jLabel1))
                 .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                 .addComponent(searchFoundLabel)
                 .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 21, Short.MAX_VALUE)
                 .addComponent(searchButton)
                 .addContainerGap())
         );
 
         pack();
     }// </editor-fold>//GEN-END:initComponents
     //Searches for employee num in text field, if not found, says not found, if found, selects it in the employee jlist
     private void searchButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_searchButtonActionPerformed
         if (MainForm.employees.contains(Integer.parseInt(searchField.getText()))) { //If the employee is in the database
            listEmployees.setSelectedValue(Integer.parseInt(searchField.getText()), rootPaneCheckingEnabled); //Select it
             
         } else {
             searchFoundLabel.setText("Employee not found"); //Else change the label to employee not found
         }
         
     }//GEN-LAST:event_searchButtonActionPerformed
     //Sets the close operation to disposal of the window, instead of application exit
     private void formWindowOpened(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_formWindowOpened
                 this.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
     }//GEN-LAST:event_formWindowOpened
 
     /**
      * @param args the command line arguments
      */
     public static void main(String args[]) {
         try {
             UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
         } catch (ClassNotFoundException ex) {
             java.util.logging.Logger.getLogger(MainForm.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
         } catch (InstantiationException ex) {
             java.util.logging.Logger.getLogger(MainForm.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
         } catch (IllegalAccessException ex) {
             java.util.logging.Logger.getLogger(MainForm.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
         } catch (javax.swing.UnsupportedLookAndFeelException ex) {
             java.util.logging.Logger.getLogger(MainForm.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
         }
         /* Create and display the form */
         java.awt.EventQueue.invokeLater(new Runnable() {
             public void run() {
                 new SearchItemForm().setVisible(true);
             }
         });
     }
     // Variables declaration - do not modify//GEN-BEGIN:variables
     private javax.swing.JLabel jLabel1;
     private javax.swing.JButton searchButton;
     private javax.swing.JTextField searchField;
     private javax.swing.JLabel searchFoundLabel;
     // End of variables declaration//GEN-END:variables
 }
