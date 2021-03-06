 /*
  * To change this template, choose Tools | Templates
  * and open the template in the editor.
  */
 
 /*
  * PaiementForm.java
  *
  * Created on Jun 27, 2012, 8:26:39 PM
  */
 
 package hotel.gui;
 
 import java.io.FileOutputStream;
 import java.io.IOException;
 import java.util.Date;
 
 import javax.swing.JOptionPane;
 import javax.swing.JTable;
 import javax.swing.event.ChangeEvent;
 import javax.swing.event.ChangeListener;
 
 import com.itextpdf.text.Document;
 import com.itextpdf.text.DocumentException;
 import com.itextpdf.text.Paragraph;
 import com.itextpdf.text.pdf.PdfWriter;
 
 import hotel.Reservation;
 import hotel.TaxesSystem;
 
 /**
  *
  * @author AJ86290
  */
 public class PaiementForm extends javax.swing.JFrame {
 
     /** Creates new form PaiementForm */
    public PaiementForm(int total, JTable model) {
     	GUI.initLookAndFeel();
         initComponents();
         this.total = total;
         this.model = model;
     }
 
     /** This method is called from within the constructor to
      * initialize the form.
      * WARNING: Do NOT modify this code. The content of this method is
      * always regenerated by the Form Editor.
      */
     @SuppressWarnings("unchecked")
     // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
     private void initComponents() {
 
         ComboBoxType = new javax.swing.JComboBox();
         SpinnerAmount = new javax.swing.JSpinner(new javax.swing.SpinnerNumberModel(Reservation.Detail.QUANTITY_MIN_VALUE, Reservation.Detail.QUANTITY_MIN_VALUE, Reservation.Detail.QUANTITY_MAX_VALUE, 1));
         jLabel1 = new javax.swing.JLabel();
         jLabel2 = new javax.swing.JLabel();
         TextChange = new javax.swing.JTextField();
         jLabel3 = new javax.swing.JLabel();
         ButtonCancel = new javax.swing.JButton();
         ButtonOk = new javax.swing.JButton();
         ButtonPrint = new javax.swing.JButton();
 
         setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
 
         ComboBoxType.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Comptant", "Visa", "Master Card", "American Express", "Debit" }));
         ComboBoxType.addActionListener(new java.awt.event.ActionListener() {
             public void actionPerformed(java.awt.event.ActionEvent evt) {
                 ComboBoxTypeActionPerformed(evt);
             }
         });
 
         jLabel1.setText("Type paiement:");
 
         jLabel2.setText("Montant:");
 
         jLabel3.setText("Monnaie:");
         
         TextChange.setEditable(false);
         TextChange.setText("0$");
 
         ButtonCancel.setText("Annuler");
         ButtonCancel.addActionListener(new java.awt.event.ActionListener() {
             public void actionPerformed(java.awt.event.ActionEvent evt) {
                 jButton1ActionPerformed(evt);
             }
         });
 
         ButtonOk.setText("Ok");
         ButtonOk.addActionListener(new java.awt.event.ActionListener() {
             public void actionPerformed(java.awt.event.ActionEvent evt) {
                 jButton2ActionPerformed(evt);
             }
         });
 
         ButtonPrint.setText("Imprimer");
         ButtonPrint.addActionListener(new java.awt.event.ActionListener() {
             public void actionPerformed(java.awt.event.ActionEvent evt) {
                 jButton3ActionPerformed(evt);
             }
         });
         
         SpinnerAmount.addChangeListener(new ChangeListener() {
             @Override
             public void stateChanged(ChangeEvent e) {
                 updateChange(Integer.parseInt(SpinnerAmount.getValue().toString()));
             }
         });
 
         javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
         getContentPane().setLayout(layout);
         layout.setHorizontalGroup(
             layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
             .addGroup(layout.createSequentialGroup()
                 .addContainerGap()
                 .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                     .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                         .addComponent(ButtonPrint)
                         .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 94, Short.MAX_VALUE)
                         .addComponent(ButtonOk, javax.swing.GroupLayout.PREFERRED_SIZE, 74, javax.swing.GroupLayout.PREFERRED_SIZE)
                         .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                         .addComponent(ButtonCancel, javax.swing.GroupLayout.PREFERRED_SIZE, 71, javax.swing.GroupLayout.PREFERRED_SIZE))
                     .addGroup(layout.createSequentialGroup()
                         .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                             .addComponent(jLabel1)
                             .addComponent(jLabel2)
                             .addComponent(jLabel3))
                         .addGap(18, 18, 18)
                         .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                             .addComponent(SpinnerAmount, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, 198, Short.MAX_VALUE)
                             .addComponent(ComboBoxType, 0, 198, Short.MAX_VALUE)
                             .addComponent(TextChange, javax.swing.GroupLayout.DEFAULT_SIZE, 198, Short.MAX_VALUE))))
                 .addContainerGap())
         );
         layout.setVerticalGroup(
             layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
             .addGroup(layout.createSequentialGroup()
                 .addContainerGap()
                 .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                     .addComponent(ComboBoxType, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                     .addComponent(jLabel1))
                 .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                 .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                     .addComponent(SpinnerAmount, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                     .addComponent(jLabel2, javax.swing.GroupLayout.PREFERRED_SIZE, 17, javax.swing.GroupLayout.PREFERRED_SIZE))
                 .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                 .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                     .addComponent(TextChange, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                     .addComponent(jLabel3))
                 .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                 .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                     .addComponent(ButtonCancel)
                     .addComponent(ButtonOk)
                     .addComponent(ButtonPrint))
                 .addContainerGap())
         );
 
         pack();
     }// </editor-fold>//GEN-END:initComponents
     
    private void updateChange(int cash) {
    	int due = cash - total;
     	if (due < 0)
     		due = 0;
     	TextChange.setText(due + "$");
     }
 
     private void ComboBoxTypeActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_ComboBoxTypeActionPerformed
     	this.dispose();
     }//GEN-LAST:event_ComboBoxTypeActionPerformed
 
     private void jButton3ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton3ActionPerformed
     	try {
     		createPdf();
     	} catch (Exception e) {
     		
     	}
     	JOptionPane.showMessageDialog(null, "Impression en cours...");
     }//GEN-LAST:event_jButton3ActionPerformed
 
     private void jButton2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton2ActionPerformed
     	JOptionPane.showMessageDialog(null, "Le paiement a bel et bien t effectu.");
     	this.dispose();
     }//GEN-LAST:event_jButton2ActionPerformed
 
     private void jButton1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton1ActionPerformed
     	this.dispose();
     }//GEN-LAST:event_jButton1ActionPerformed
 
     private void createPdf() throws DocumentException, IOException {
     	        Document document = new Document();
     	        PdfWriter.getInstance(document, new FileOutputStream("bill.pdf"));
     	        document.open();
     	        
     	        document.add(new Paragraph(new Date().toString()));
 
     	        document.add(new Paragraph("No Chambre | Categorie | Nb Jours | Total"));
     	        for (int i = 0; i < model.getModel().getRowCount(); ++i) {
     	        	String line = "";
     	        	for (int j = 0; j < model.getModel().getColumnCount(); ++j)
     	        		line += model.getModel().getValueAt(i, j).toString() + "                 ";
         	        document.add(new Paragraph(line));
     			}
 
    	        int subTotal = 0;
     			for (int i = 0; i < model.getModel().getRowCount(); ++i) {
    				subTotal = ((Integer)model.getModel().getValueAt(i, 3)).intValue();
     			}
     			
     			document.add(new Paragraph("------------------"));
     			
     			subTotal = Math.abs(subTotal);
     			document.add(new Paragraph("Sous-total: " + subTotal + "$"));
     			document.add(new Paragraph("TPS: " + Math.round(TaxesSystem.calculateTPS(subTotal)) + "$"));
     			subTotal += TaxesSystem.calculateTPS(subTotal);
     			document.add(new Paragraph("TVQ: " + Math.round(TaxesSystem.calculateTVQ(subTotal)) + "$"));
     			subTotal += TaxesSystem.calculateTVQ(subTotal);
     			document.add(new Paragraph("Total: " + (subTotal + "$")));
     	        
     	        document.close();
     }
 
     // Variables declaration - do not modify//GEN-BEGIN:variables
     private javax.swing.JComboBox ComboBoxType;
     private javax.swing.JSpinner SpinnerAmount;
     private javax.swing.JTextField TextChange;
     private javax.swing.JButton ButtonCancel;
     private javax.swing.JButton ButtonOk;
     private javax.swing.JButton ButtonPrint;
     private javax.swing.JLabel jLabel1;
     private javax.swing.JLabel jLabel2;
     private javax.swing.JLabel jLabel3;
     // End of variables declaration//GEN-END:variables
    private int total = 0;
     private JTable model = null;
 }
