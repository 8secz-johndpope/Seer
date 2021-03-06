 /*
  * To change this template, choose Tools | Templates
  * and open the template in the editor.
  */
 
 /*
  * medewerkerInfo.java
  *
  * Created on 8-jan-2011, 14:25:19
  */
 
 package view;
 
 import java.util.Calendar;
 import javax.swing.table.DefaultTableModel;
 import model.Employee;
 import model.WorkHours;
 import roosterprogramma.RoosterProgramma;
 
 /**
  *
  * @author Dark
  */
 public class EmployeeInfo extends javax.swing.JPanel {
 
     private Employee employee;
     private DefaultTableModel model;
 
     /** Creates new form medewerkerInfo */
     public EmployeeInfo(Employee employee) {
         this.employee = employee;
         initComponents();
         fillInfoTable();
         fillVerantwoordingTable();
     }
 
     private void fillInfoTable() {
         ((DefaultTableModel) tblEmployeeInformation.getModel()).addRow(new Object[] {
             employee.getFirstName(),
             employee.getFamilyName(),
             employee.getEmployeeNumber(),
             employee.isFullTime(),
             employee.isPartTime(),
             employee.isCallWorker()
         });
     }
 
     private void fillVerantwoordingTable() {
         model = (DefaultTableModel) tblTimeSheet.getModel();
         Calendar calendar = Calendar.getInstance();
         int daysOfMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH);
         for (int i = 1; i <= daysOfMonth; i++)
         {
             calendar.set(Calendar.DAY_OF_MONTH, i);
             String year = Integer.toString(calendar.get(Calendar.YEAR));
             String month = Integer.toString(calendar.get(Calendar.MONTH)+1).length() < 2 ? "0" + Integer.toString(calendar.get(Calendar.MONTH)+1) : Integer.toString(calendar.get(Calendar.MONTH)+1);
             String day = Integer.toString(calendar.get(Calendar.DAY_OF_MONTH)).length() < 2 ? "0" + Integer.toString(calendar.get(Calendar.DAY_OF_MONTH)) : Integer.toString(calendar.get(Calendar.DAY_OF_MONTH));
             WorkHours hour = employee.getWorkHours(year + "-" + month + "-" + day);
             model.addRow(new Object[] {
                 calendar.get(Calendar.DAY_OF_MONTH),
                 hour.getWorked(),
                 hour.getCompensation150(),
                 hour.getCompensation200(),
                 hour.getVacation(),
                 hour.getADV(),
                 hour.getIllness(),
                 hour.getVerlof(),
                 hour.getProject(),
                 0
             });
         }
         model.addRow(new Object[] {
             "Totaal",
             0,
             0,
             0,
             0,
             0,
             0,
             0,
             0,
             0
         });
         calculateTotal();
     }
 
     private void calculateTotal() {
         for (int i = 0; i < model.getRowCount(); i++)
         {
             double totalHours = 0;
             for (int j = 1; j < model.getColumnCount()-1; j++)
             {
                totalHours += Double.parseDouble(model.getValueAt(i, j).toString());
             }
             model.setValueAt(totalHours, i, model.getColumnCount()-1);
         }
         for (int k = 1; k < model.getColumnCount(); k++)
         {
             double totalHours = 0;
             for (int l = 0; l < model.getRowCount()-1; l++)
             {
                totalHours += Double.parseDouble(model.getValueAt(l, k).toString());
             }
             model.setValueAt(totalHours, model.getRowCount()-1, k);
         }
     }
 
     /** This method is called from within the constructor to
      * initialize the form.
      * WARNING: Do NOT modify this code. The content of this method is
      * always regenerated by the Form Editor.
      */
     @SuppressWarnings("unchecked")
     // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
     private void initComponents() {
 
         btnSave = new javax.swing.JButton();
         btnBack = new javax.swing.JButton();
         jScrollPane2 = new javax.swing.JScrollPane();
         tblEmployeeInformation = new javax.swing.JTable();
         jScrollPane3 = new javax.swing.JScrollPane();
         tblTimeSheet = new javax.swing.JTable();
         lblMonth = new javax.swing.JLabel();
 
         btnSave.setText("Wijzigingen opslaan");
         btnSave.addActionListener(new java.awt.event.ActionListener() {
             public void actionPerformed(java.awt.event.ActionEvent evt) {
                 btnSaveActionPerformed(evt);
             }
         });
 
         btnBack.setText("Vorige");
         btnBack.addActionListener(new java.awt.event.ActionListener() {
             public void actionPerformed(java.awt.event.ActionEvent evt) {
                 btnBackActionPerformed(evt);
             }
         });
 
         tblEmployeeInformation.setModel(new javax.swing.table.DefaultTableModel(
             new Object [][] {
 
             },
             new String [] {
                 "Voornaam", "Achternaam", "Personeelsnummer", "Fulltime", "Parttime", "Oproepkracht", "Noodhulp"
             }
         ) {
             Class[] types = new Class [] {
                 java.lang.String.class, java.lang.String.class, java.lang.Integer.class, java.lang.Boolean.class, java.lang.Boolean.class, java.lang.Boolean.class, java.lang.Boolean.class
             };
             boolean[] canEdit = new boolean [] {
                 false, false, false, false, false, false, false
             };
 
             public Class getColumnClass(int columnIndex) {
                 return types [columnIndex];
             }
 
             public boolean isCellEditable(int rowIndex, int columnIndex) {
                 return canEdit [columnIndex];
             }
         });
         jScrollPane2.setViewportView(tblEmployeeInformation);
 
         tblTimeSheet.setModel(new javax.swing.table.DefaultTableModel(
             new Object [][] {
 
             },
             new String [] {
                 "Dag vd maand", "Gewerkt", "Compensatie 150", "Compensatie 200", "Vakantie", "ADV", "Ziek", "Speciaal verlof", "Project", "Totaal"
             }
         ) {
             Class[] types = new Class [] {
                 java.lang.String.class, java.lang.Double.class, java.lang.Double.class, java.lang.Double.class, java.lang.Double.class, java.lang.Double.class, java.lang.Double.class, java.lang.Double.class, java.lang.Double.class, java.lang.Double.class
             };
             boolean[] canEdit = new boolean [] {
                 false, true, true, true, true, true, true, true, true, true
             };
 
             public Class getColumnClass(int columnIndex) {
                 return types [columnIndex];
             }
 
             public boolean isCellEditable(int rowIndex, int columnIndex) {
                 return canEdit [columnIndex];
             }
         });
         tblTimeSheet.setRowSelectionAllowed(false);
         tblTimeSheet.addFocusListener(new java.awt.event.FocusAdapter() {
             public void focusGained(java.awt.event.FocusEvent evt) {
                 tblTimeSheetFocusGained(evt);
             }
         });
         jScrollPane3.setViewportView(tblTimeSheet);
 
         javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
         this.setLayout(layout);
         layout.setHorizontalGroup(
             layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
             .addGroup(layout.createSequentialGroup()
                 .addContainerGap()
                 .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                     .addComponent(jScrollPane3, javax.swing.GroupLayout.DEFAULT_SIZE, 1307, Short.MAX_VALUE)
                     .addGroup(layout.createSequentialGroup()
                         .addComponent(btnBack)
                         .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 1117, Short.MAX_VALUE)
                         .addComponent(btnSave))
                     .addComponent(jScrollPane2, javax.swing.GroupLayout.DEFAULT_SIZE, 1307, Short.MAX_VALUE)
                     .addComponent(lblMonth))
                 .addContainerGap())
         );
         layout.setVerticalGroup(
             layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
             .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                 .addContainerGap()
                 .addComponent(jScrollPane2, javax.swing.GroupLayout.PREFERRED_SIZE, 43, javax.swing.GroupLayout.PREFERRED_SIZE)
                 .addGap(18, 18, 18)
                 .addComponent(lblMonth)
                 .addGap(18, 18, 18)
                 .addComponent(jScrollPane3, javax.swing.GroupLayout.DEFAULT_SIZE, 386, Short.MAX_VALUE)
                 .addGap(18, 18, 18)
                 .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                     .addComponent(btnSave)
                     .addComponent(btnBack))
                 .addContainerGap())
         );
     }// </editor-fold>//GEN-END:initComponents
 
     private void btnBackActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnBackActionPerformed
         RoosterProgramma.getInstance().showPanel(new EmployeeOverview());
     }//GEN-LAST:event_btnBackActionPerformed
 
     private void tblTimeSheetFocusGained(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_tblTimeSheetFocusGained
         calculateTotal();
     }//GEN-LAST:event_tblTimeSheetFocusGained
 
     private void btnSaveActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnSaveActionPerformed
         Calendar calendar = Calendar.getInstance();
         for (int i = 0; i < model.getRowCount(); i++)
         {
             String year = Integer.toString(calendar.get(Calendar.YEAR));
             String month = Integer.toString(calendar.get(Calendar.MONTH)+1).length() < 2 ? "0" + Integer.toString(calendar.get(Calendar.MONTH)+1) : Integer.toString(calendar.get(Calendar.MONTH)+1);
             WorkHours hour = employee.getWorkHours(year + "-" + month + "-" + model.getValueAt(i, 0).toString());
             hour.setWorked(Double.parseDouble(model.getValueAt(i, 1).toString()));
             hour.setCompensation150(Double.parseDouble(model.getValueAt(i, 2).toString()));
             hour.setCompensation200(Double.parseDouble(model.getValueAt(i, 3).toString()));
             hour.setVacation(Double.parseDouble(model.getValueAt(i, 4).toString()));
             hour.setADV(Double.parseDouble(model.getValueAt(i, 5).toString()));
             hour.setIllness(Double.parseDouble(model.getValueAt(i, 6).toString()));
             hour.setVerlof(Double.parseDouble(model.getValueAt(i, 7).toString()));
             hour.setProject(Double.parseDouble(model.getValueAt(i, 8).toString()));
             hour.update();
         }
     }//GEN-LAST:event_btnSaveActionPerformed
 
 
     // Variables declaration - do not modify//GEN-BEGIN:variables
     private javax.swing.JButton btnBack;
     private javax.swing.JButton btnSave;
     private javax.swing.JScrollPane jScrollPane2;
     private javax.swing.JScrollPane jScrollPane3;
     private javax.swing.JLabel lblMonth;
     private javax.swing.JTable tblEmployeeInformation;
     private javax.swing.JTable tblTimeSheet;
     // End of variables declaration//GEN-END:variables
 
 }
