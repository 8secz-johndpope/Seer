 /*
  * To change this template, choose Tools | Templates
  * and open the template in the editor.
  */
 
 /*
  * DiagPrimero.java
  *
  * Created on Aug 10, 2012, 9:05:00 PM
  */
 package org.grupoia.main.tpdos.ptoa.primero.gui;
 
 import java.util.List;
 import javax.swing.table.DefaultTableModel;
 import javax.swing.tree.DefaultMutableTreeNode;
 import org.grupoia.main.tpdos.common.Hoja;
 import org.grupoia.main.tpdos.common.NodoArbol;
 import org.grupoia.main.tpdos.mockedobjects.MockedArbol;
 import org.grupoia.main.tpdos.ptoa.primero.AlgoritmoSolucion;
 import org.grupoia.main.tpdos.ptoa.primero.PrimeroAnchuraSolucion;
 import org.grupoia.main.tpdos.ptoa.primero.PrimeroProfundidadSolucion;
 
 /**
  *
  * @author fanky
  */
 public class DiagPrimero extends javax.swing.JDialog {
 
     private static final String TITLE = "Primero Anchura / Profundidad";
     private static final Boolean DEBUG = true;
     private MyDynamicTree dynamicTree = null;
     private NodoArbol raiz = MockedArbol.generaArbol();
 
     /** Creates new form DiagPrimero */
     public DiagPrimero(java.awt.Frame parent, boolean modal) {
         super(parent, modal);
         initComponents();
         init();
         setTitle(TITLE);
         setLocationRelativeTo(null);
     }
 
     private void init() {
         PrimeroProfundidadSolucion.DEBUG = false;
         PrimeroAnchuraSolucion.DEBUG = false;
         dynamicTree = new MyDynamicTree(new DefaultMutableTreeNode(raiz));
         addNodos(dynamicTree);
         treePanel.add(dynamicTree);
         refrescaTabla(null);
     }
 
     private void refrescaTabla(List<NodoArbol> visitados) {
         DefaultTableModel tableModel = new DefaultTableModel();
         if (visitados == null || visitados.isEmpty()) {
             tableModel.setColumnIdentifiers(new String[]{"Busque un nodo"});
         } else {
             tableModel.setColumnIdentifiers(new String[]{"Nodo Visitado"});
             for (NodoArbol na : visitados) {
                 tableModel.addRow(new Object[]{na});
             }
         }
         tblRuta.setModel(tableModel);
     }
 
     //Agrego mis nodos customizados
     private void addNodos(MyDynamicTree treePanel) {
 
         if (!raiz.getNodosHijos().isEmpty()) {
             for (NodoArbol na : raiz.getNodosHijos()) {
                 addObject(treePanel, na, null);//default
             }
         }
     }
 
     private void addObject(MyDynamicTree dynTree, NodoArbol nodo, DefaultMutableTreeNode parent) {
         if (!nodo.getNodosHijos().isEmpty()) {
             parent = dynTree.addObject(parent, nodo);
             for (NodoArbol child : nodo.getNodosHijos()) {
                 addObject(dynTree, child, parent);
             }
         } else {
             dynTree.addObject(parent, nodo);//addObject(dynTree, nodo, parent);
         }
 
     }
 
     private void eliminarNodo() {
         dynamicTree.removeCurrentNode();
     }
 
     private void agregarNodo() {
         // primero buscarlo y seleccionarlo (?
         // si no lo encuentro, agregarlo jeje
        dynamicTree.addObject(txtNodoNuevo.getText());
     }
 
     private void buscarNodo() {
         Integer value = Integer.parseInt(txtNodoObjetivo.getText());
         NodoArbol objetivo = new Hoja(value);
         AlgoritmoSolucion solucion = null;
         if (rbAnchura.isSelected()) {
             solucion = new PrimeroAnchuraSolucion();
         } else if (rbProfundidad.isSelected()) {
             solucion = new PrimeroProfundidadSolucion();
         } else {
             throw new IllegalArgumentException("radio button invalido (? jaja");
         }
         NodoArbol encontrado = solucion.buscarObjetivo(raiz, objetivo);
         System.out.println("encontrado!: " + encontrado);
 
         refrescaTabla(solucion.getVisitedList());
         // TODO: print visited
         if (encontrado != null) {
             javax.swing.JOptionPane.showMessageDialog(rootPane, "Objetivo encontrado!: " + encontrado);
         } else {
             javax.swing.JOptionPane.showMessageDialog(rootPane, "Objetivo no encontrado :( ");
         }
     }
 
     private void cerrar() {
         this.dispose();
     }
 
     /** This method is called from within the constructor to
      * initialize the form.
      * WARNING: Do NOT modify this code. The content of this method is
      * always regenerated by the Form Editor.
      */
     @SuppressWarnings("unchecked")
     // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
     private void initComponents() {
 
         buttonGroup1 = new javax.swing.ButtonGroup();
         jPanel1 = new javax.swing.JPanel();
         jPanel2 = new javax.swing.JPanel();
         btnAgregarNodo = new javax.swing.JButton();
         btnEliminarNodo = new javax.swing.JButton();
         txtNodoNuevo = new javax.swing.JTextField();
         jPanel3 = new javax.swing.JPanel();
         txtNodoObjetivo = new javax.swing.JTextField();
         btnBuscarNodo = new javax.swing.JButton();
         jPanel4 = new javax.swing.JPanel();
         jButton4 = new javax.swing.JButton();
         treePanel = new javax.swing.JPanel();
         jPanel5 = new javax.swing.JPanel();
         rbProfundidad = new javax.swing.JRadioButton();
         rbAnchura = new javax.swing.JRadioButton();
         jScrollPane1 = new javax.swing.JScrollPane();
         tblRuta = new javax.swing.JTable();
 
         setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
 
         jPanel1.setBorder(new javax.swing.border.SoftBevelBorder(javax.swing.border.BevelBorder.LOWERED));
 
         jPanel2.setBorder(javax.swing.BorderFactory.createTitledBorder("Nodos"));
 
         btnAgregarNodo.setText("Agregar");
         btnAgregarNodo.addActionListener(new java.awt.event.ActionListener() {
             public void actionPerformed(java.awt.event.ActionEvent evt) {
                 btnAgregarNodoActionPerformed(evt);
             }
         });
 
         btnEliminarNodo.setText("Eliminar");
         btnEliminarNodo.addActionListener(new java.awt.event.ActionListener() {
             public void actionPerformed(java.awt.event.ActionEvent evt) {
                 btnEliminarNodoActionPerformed(evt);
             }
         });
 
         txtNodoNuevo.setText("Nodo");
         txtNodoNuevo.addActionListener(new java.awt.event.ActionListener() {
             public void actionPerformed(java.awt.event.ActionEvent evt) {
                 txtNodoNuevoActionPerformed(evt);
             }
         });
         txtNodoNuevo.addFocusListener(new java.awt.event.FocusAdapter() {
             public void focusGained(java.awt.event.FocusEvent evt) {
                 txtNodoNuevoFocusGained(evt);
             }
         });
 
         org.jdesktop.layout.GroupLayout jPanel2Layout = new org.jdesktop.layout.GroupLayout(jPanel2);
         jPanel2.setLayout(jPanel2Layout);
         jPanel2Layout.setHorizontalGroup(
             jPanel2Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
             .add(txtNodoNuevo, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 228, Short.MAX_VALUE)
             .add(jPanel2Layout.createSequentialGroup()
                 .addContainerGap()
                 .add(btnAgregarNodo)
                 .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                 .add(btnEliminarNodo))
         );
         jPanel2Layout.setVerticalGroup(
             jPanel2Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
             .add(jPanel2Layout.createSequentialGroup()
                 .add(txtNodoNuevo, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                 .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                 .add(jPanel2Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                     .add(btnAgregarNodo)
                     .add(btnEliminarNodo))
                 .addContainerGap(org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
         );
 
         jPanel3.setBorder(javax.swing.BorderFactory.createTitledBorder("Busqueda"));
 
         txtNodoObjetivo.setText("Nodo Objetivo");
         txtNodoObjetivo.addActionListener(new java.awt.event.ActionListener() {
             public void actionPerformed(java.awt.event.ActionEvent evt) {
                 txtNodoObjetivoActionPerformed(evt);
             }
         });
         txtNodoObjetivo.addFocusListener(new java.awt.event.FocusAdapter() {
             public void focusGained(java.awt.event.FocusEvent evt) {
                 txtNodoObjetivoFocusGained(evt);
             }
         });
 
         btnBuscarNodo.setText("Buscar");
         btnBuscarNodo.addActionListener(new java.awt.event.ActionListener() {
             public void actionPerformed(java.awt.event.ActionEvent evt) {
                 btnBuscarNodoActionPerformed(evt);
             }
         });
 
         org.jdesktop.layout.GroupLayout jPanel3Layout = new org.jdesktop.layout.GroupLayout(jPanel3);
         jPanel3.setLayout(jPanel3Layout);
         jPanel3Layout.setHorizontalGroup(
             jPanel3Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
             .add(org.jdesktop.layout.GroupLayout.TRAILING, txtNodoObjetivo, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 228, Short.MAX_VALUE)
             .add(jPanel3Layout.createSequentialGroup()
                 .add(btnBuscarNodo)
                 .addContainerGap())
         );
         jPanel3Layout.setVerticalGroup(
             jPanel3Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
             .add(jPanel3Layout.createSequentialGroup()
                 .add(txtNodoObjetivo, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                 .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                 .add(btnBuscarNodo))
         );
 
         org.jdesktop.layout.GroupLayout jPanel1Layout = new org.jdesktop.layout.GroupLayout(jPanel1);
         jPanel1.setLayout(jPanel1Layout);
         jPanel1Layout.setHorizontalGroup(
             jPanel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
             .add(jPanel1Layout.createSequentialGroup()
                 .add(jPanel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.TRAILING, false)
                     .add(org.jdesktop.layout.GroupLayout.LEADING, jPanel3, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                     .add(org.jdesktop.layout.GroupLayout.LEADING, jPanel2, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                 .addContainerGap(org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
         );
         jPanel1Layout.setVerticalGroup(
             jPanel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
             .add(jPanel1Layout.createSequentialGroup()
                 .add(jPanel2, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 100, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                 .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                 .add(jPanel3, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                 .addContainerGap(203, Short.MAX_VALUE))
         );
 
         jButton4.setText("Cerrar");
         jButton4.addActionListener(new java.awt.event.ActionListener() {
             public void actionPerformed(java.awt.event.ActionEvent evt) {
                 jButton4ActionPerformed(evt);
             }
         });
         jPanel4.add(jButton4);
 
         treePanel.setBorder(javax.swing.BorderFactory.createEtchedBorder());
         treePanel.setLayout(new java.awt.BorderLayout());
 
         buttonGroup1.add(rbProfundidad);
         rbProfundidad.setSelected(true);
         rbProfundidad.setText("Primero Profundidad");
         jPanel5.add(rbProfundidad);
 
         buttonGroup1.add(rbAnchura);
         rbAnchura.setText("Primero Anchura");
         jPanel5.add(rbAnchura);
 
         tblRuta.setModel(new javax.swing.table.DefaultTableModel(
             new Object [][] {
                 {null, null, null, null},
                 {null, null, null, null},
                 {null, null, null, null},
                 {null, null, null, null}
             },
             new String [] {
                 "Title 1", "Title 2", "Title 3", "Title 4"
             }
         ));
         jScrollPane1.setViewportView(tblRuta);
 
         org.jdesktop.layout.GroupLayout layout = new org.jdesktop.layout.GroupLayout(getContentPane());
         getContentPane().setLayout(layout);
         layout.setHorizontalGroup(
             layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
             .add(layout.createSequentialGroup()
                 .addContainerGap()
                 .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.TRAILING, false)
                     .add(org.jdesktop.layout.GroupLayout.LEADING, jPanel5, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                     .add(org.jdesktop.layout.GroupLayout.LEADING, jPanel4, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                     .add(org.jdesktop.layout.GroupLayout.LEADING, layout.createSequentialGroup()
                         .add(jPanel1, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                         .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                         .add(treePanel, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 324, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                         .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                         .add(jScrollPane1, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 310, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)))
                 .addContainerGap())
         );
         layout.setVerticalGroup(
             layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
             .add(layout.createSequentialGroup()
                 .addContainerGap()
                 .add(jPanel5, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                 .addPreferredGap(org.jdesktop.layout.LayoutStyle.UNRELATED)
                 .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                     .add(jScrollPane1, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 401, Short.MAX_VALUE)
                     .add(treePanel, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 401, Short.MAX_VALUE)
                     .add(jPanel1, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                 .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                 .add(jPanel4, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
         );
 
         pack();
     }// </editor-fold>//GEN-END:initComponents
 
     private void btnEliminarNodoActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnEliminarNodoActionPerformed
         eliminarNodo();
     }//GEN-LAST:event_btnEliminarNodoActionPerformed
 
     private void btnAgregarNodoActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnAgregarNodoActionPerformed
         agregarNodo();
     }//GEN-LAST:event_btnAgregarNodoActionPerformed
 
     private void txtNodoNuevoFocusGained(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_txtNodoNuevoFocusGained
         txtNodoNuevo.selectAll();
     }//GEN-LAST:event_txtNodoNuevoFocusGained
 
     private void txtNodoNuevoActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_txtNodoNuevoActionPerformed
         agregarNodo();
     }//GEN-LAST:event_txtNodoNuevoActionPerformed
 
     private void txtNodoObjetivoActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_txtNodoObjetivoActionPerformed
         buscarNodo();
     }//GEN-LAST:event_txtNodoObjetivoActionPerformed
 
     private void btnBuscarNodoActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnBuscarNodoActionPerformed
         buscarNodo();
     }//GEN-LAST:event_btnBuscarNodoActionPerformed
 
     private void txtNodoObjetivoFocusGained(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_txtNodoObjetivoFocusGained
         txtNodoObjetivo.selectAll();
     }//GEN-LAST:event_txtNodoObjetivoFocusGained
 
     private void jButton4ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton4ActionPerformed
         cerrar();
     }//GEN-LAST:event_jButton4ActionPerformed
 
     /**
      * @param args the command line arguments
      */
     public static void main(String args[]) {
         /* Set the Nimbus look and feel */
         //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
         /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
          * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html 
          */
         try {
             for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                 if ("Nimbus".equals(info.getName())) {
                     javax.swing.UIManager.setLookAndFeel(info.getClassName());
                     break;
                 }
             }
         } catch (ClassNotFoundException ex) {
             java.util.logging.Logger.getLogger(DiagPrimero.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
         } catch (InstantiationException ex) {
             java.util.logging.Logger.getLogger(DiagPrimero.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
         } catch (IllegalAccessException ex) {
             java.util.logging.Logger.getLogger(DiagPrimero.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
         } catch (javax.swing.UnsupportedLookAndFeelException ex) {
             java.util.logging.Logger.getLogger(DiagPrimero.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
         }
         //</editor-fold>
 
         /* Create and display the dialog */
         java.awt.EventQueue.invokeLater(new Runnable() {
 
             public void run() {
                 DiagPrimero dialog = new DiagPrimero(new javax.swing.JFrame(), true);
                 dialog.addWindowListener(new java.awt.event.WindowAdapter() {
 
                     @Override
                     public void windowClosing(java.awt.event.WindowEvent e) {
                         System.exit(0);
                     }
                 });
                 dialog.setVisible(true);
                 dialog.dispose();
                 System.exit(0);
             }
         });
     }
     // Variables declaration - do not modify//GEN-BEGIN:variables
     private javax.swing.JButton btnAgregarNodo;
     private javax.swing.JButton btnBuscarNodo;
     private javax.swing.JButton btnEliminarNodo;
     private javax.swing.ButtonGroup buttonGroup1;
     private javax.swing.JButton jButton4;
     private javax.swing.JPanel jPanel1;
     private javax.swing.JPanel jPanel2;
     private javax.swing.JPanel jPanel3;
     private javax.swing.JPanel jPanel4;
     private javax.swing.JPanel jPanel5;
     private javax.swing.JScrollPane jScrollPane1;
     private javax.swing.JRadioButton rbAnchura;
     private javax.swing.JRadioButton rbProfundidad;
     private javax.swing.JTable tblRuta;
     private javax.swing.JPanel treePanel;
     private javax.swing.JTextField txtNodoNuevo;
     private javax.swing.JTextField txtNodoObjetivo;
     // End of variables declaration//GEN-END:variables
 }
