 /*
  * To change this template, choose Tools | Templates
  * and open the template in the editor.
  */
 package tareaiiiredes.client.GUI;
 
 import com.hp.hpl.jena.query.ResultSet;
 import java.awt.BorderLayout;
 import tareaiiiredes.QueryRemoteSparql;
 import tareaiiiredes.client.GUI.images.ipanel;
 import tareaiiiredes.Cliente;
 /**
  *
  * @author orlando
  */
 public class SparqlClientn extends javax.swing.JFrame {
      
     Cliente cliente;
      /**
      * Creates new form SparqlClientn
      */
     public SparqlClientn() {
         
         initComponents();
         
         
         //Consulta ejemplo
         mqueryTextArea.setText
                 (   "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n"
                     +"SELECT ?pais\n"
                     +"WHERE {?pais rdf:type <http://dbpedia.org/ontology/Country>}"
                 );
         mEndpointTextField.setText("http://dbpedia.org/sparql");
         ipanel p= new ipanel("CFMatrix.jpg");
         this.add(p,BorderLayout.CENTER);
         
     }
    
     /**
      * This method is called from within the constructor to initialize the form.
      * WARNING: Do NOT modify this code. The content of this method is always
      * regenerated by the Form Editor.
      */
     @SuppressWarnings("unchecked")
     // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
     private void initComponents() {
 
         buttonGroup1 = new javax.swing.ButtonGroup();
         jLabel3 = new javax.swing.JLabel();
         mEndpointTextField = new javax.swing.JTextField();
         jLabel2 = new javax.swing.JLabel();
         jScrollPane1 = new javax.swing.JScrollPane();
         mqueryTextArea = new javax.swing.JTextArea();
         jLabel1 = new javax.swing.JLabel();
         jButton1 = new javax.swing.JButton();
         jComboBox2 = new javax.swing.JComboBox();
         jTextFieldPort = new javax.swing.JTextField();
         jLabel4 = new javax.swing.JLabel();
         mEndpointServerIPTextField1 = new javax.swing.JTextField();
         jLabel5 = new javax.swing.JLabel();
 
         setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
         setTitle("Cliente Sparql");
         setResizable(false);
 
         jLabel3.setForeground(new java.awt.Color(255, 255, 255));
         jLabel3.setText("Sitio web con soporte SPARQL");
 
         jLabel2.setForeground(new java.awt.Color(255, 255, 255));
         jLabel2.setText("Query");
 
         mqueryTextArea.setColumns(20);
         mqueryTextArea.setRows(5);
         jScrollPane1.setViewportView(mqueryTextArea);
         mqueryTextArea.getAccessibleContext().setAccessibleName("queryTextArea");
 
         jLabel1.setForeground(new java.awt.Color(255, 255, 255));
         jLabel1.setText("Format output:");
 
         jButton1.setText("Query");
         jButton1.addActionListener(new java.awt.event.ActionListener() {
             public void actionPerformed(java.awt.event.ActionEvent evt) {
                 jButton1ActionPerformed(evt);
             }
         });
 
         jComboBox2.addItem("CSV");
         jComboBox2.addItem("JSON");
         jComboBox2.addItem("RDF/XML");
         jComboBox2.addItem("TSV");
         jComboBox2.addItem("XML");
 
         jLabel4.setForeground(new java.awt.Color(255, 255, 255));
         jLabel4.setText("Port");
 
         jLabel5.setForeground(new java.awt.Color(255, 255, 255));
         jLabel5.setText("jLabel5");
 
         javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
         getContentPane().setLayout(layout);
         layout.setHorizontalGroup(
             layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
             .addGroup(layout.createSequentialGroup()
                 .addContainerGap()
                 .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                     .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                         .addGap(0, 0, Short.MAX_VALUE)
                         .addComponent(jButton1, javax.swing.GroupLayout.PREFERRED_SIZE, 94, javax.swing.GroupLayout.PREFERRED_SIZE))
                     .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                         .addComponent(mEndpointServerIPTextField1)
                         .addGap(50, 50, 50)
                         .addComponent(jTextFieldPort, javax.swing.GroupLayout.PREFERRED_SIZE, 107, javax.swing.GroupLayout.PREFERRED_SIZE))
                     .addGroup(layout.createSequentialGroup()
                         .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                             .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                 .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                                     .addComponent(jLabel1)
                                     .addGap(4, 4, 4)
                                     .addComponent(jComboBox2, javax.swing.GroupLayout.PREFERRED_SIZE, 94, javax.swing.GroupLayout.PREFERRED_SIZE))
                                 .addGroup(layout.createSequentialGroup()
                                     .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                         .addGroup(layout.createSequentialGroup()
                                             .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                 .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                                                     .addComponent(mEndpointTextField, javax.swing.GroupLayout.Alignment.LEADING)
                                                     .addComponent(jLabel3, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                                                 .addComponent(jLabel5))
                                             .addGap(50, 50, 50)
                                             .addComponent(jLabel4))
                                         .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 368, javax.swing.GroupLayout.PREFERRED_SIZE))
                                     .addGap(2, 2, 2)))
                             .addComponent(jLabel2))
                         .addGap(0, 0, Short.MAX_VALUE)))
                 .addContainerGap())
         );
         layout.setVerticalGroup(
             layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
             .addGroup(layout.createSequentialGroup()
                 .addGap(17, 17, 17)
                 .addComponent(jLabel3)
                 .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                 .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                     .addGroup(layout.createSequentialGroup()
                         .addComponent(mEndpointTextField, javax.swing.GroupLayout.PREFERRED_SIZE, 25, javax.swing.GroupLayout.PREFERRED_SIZE)
                         .addGap(53, 53, 53))
                     .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                         .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                             .addComponent(jLabel4)
                             .addComponent(jLabel5))
                         .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                         .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                             .addComponent(jTextFieldPort, javax.swing.GroupLayout.PREFERRED_SIZE, 25, javax.swing.GroupLayout.PREFERRED_SIZE)
                             .addComponent(mEndpointServerIPTextField1, javax.swing.GroupLayout.PREFERRED_SIZE, 25, javax.swing.GroupLayout.PREFERRED_SIZE))))
                 .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                 .addComponent(jLabel2)
                 .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                 .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 138, javax.swing.GroupLayout.PREFERRED_SIZE)
                 .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                 .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                     .addComponent(jComboBox2)
                     .addComponent(jLabel1))
                 .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                 .addComponent(jButton1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                 .addContainerGap())
         );
 
         pack();
     }// </editor-fold>//GEN-END:initComponents
 
     private void jButton1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton1ActionPerformed
         
         String host = mEndpointServerIPTextField1.getText();
         try{
             int port= Integer.parseInt(jTextFieldPort.getText().toString());
             cliente =new Cliente(host,port);
             cliente.run();
         }catch(Exception e){}
         
        if(cliente!=null&&cliente.GetSocketStatus()!=null){
             String query = mqueryTextArea.getText();
             ResultSet rs=null;
             ResultSet rscopy=null;
             long startTime;
             long totalTime=0;
             try {
                 startTime = System.currentTimeMillis();
                 rs = QueryRemoteSparql.getResults(host, query);
                 rscopy=  QueryRemoteSparql.getResults(host, query);
                 totalTime = System.currentTimeMillis() - startTime;
             } 
             catch (Exception e1) {
                 System.out.println("---INICIO Mensaje de excepción---");
                 System.console().printf(e1.toString());
                 System.out.println("---FIN Mensaje de excepción---");
             }
             if(rs != null)
             {
                 SparqlOutput outputwindow = new SparqlOutput(host,query,totalTime,rs,rscopy,jComboBox2.getSelectedItem().toString());
                 outputwindow.setVisible(true);
             }
             else
                 System.out.println("ResultSet vacío.");
         }
     }//GEN-LAST:event_jButton1ActionPerformed
 
     // Variables declaration - do not modify//GEN-BEGIN:variables
     private javax.swing.ButtonGroup buttonGroup1;
     private javax.swing.JButton jButton1;
     private javax.swing.JComboBox jComboBox2;
     private javax.swing.JLabel jLabel1;
     private javax.swing.JLabel jLabel2;
     private javax.swing.JLabel jLabel3;
     private javax.swing.JLabel jLabel4;
     private javax.swing.JLabel jLabel5;
     private javax.swing.JScrollPane jScrollPane1;
     private javax.swing.JTextField jTextFieldPort;
     private javax.swing.JTextField mEndpointServerIPTextField1;
     private javax.swing.JTextField mEndpointTextField;
     private javax.swing.JTextArea mqueryTextArea;
     // End of variables declaration//GEN-END:variables
 }
