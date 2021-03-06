 /*
  * To change this template, choose Tools | Templates
  * and open the template in the editor.
  */
 package com.github.kayak.ui.messageview;
 
 import com.github.kayak.core.Bus;
 import java.awt.dnd.DropTarget;
 import java.util.logging.Level;
 import java.util.logging.Logger;
import javax.swing.DropMode;
 import org.openide.util.NbBundle;
 import org.openide.windows.TopComponent;
 import org.netbeans.api.settings.ConvertAsProperties;
 import org.openide.explorer.ExplorerManager;
 import org.openide.explorer.ExplorerUtils;
 import org.openide.nodes.AbstractNode;
 import org.openide.nodes.Children;
 
 /**
  * Top component which displays something.
  */
 @ConvertAsProperties(dtd = "-//com.github.kayak.ui.messageview//MessageView//EN",
 autostore = false)
 @TopComponent.Description(preferredID = "MessageViewTopComponent",
 //iconBase="SET/PATH/TO/ICON/HERE", 
 persistenceType = TopComponent.PERSISTENCE_NEVER)
 @TopComponent.Registration(mode = "editor", openAtStartup = false)
 @TopComponent.OpenActionRegistration(displayName = "#CTL_MessageViewAction",
 preferredID = "MessageViewTopComponent")
 public final class MessageViewTopComponent extends TopComponent implements ExplorerManager.Provider {
 
     private static final Logger logger = Logger.getLogger(MessageViewTopComponent.class.getCanonicalName());
 
     private Bus bus;
     private AbstractNode root;
     private ExplorerManager manager = new ExplorerManager();
     private SignalTableModel model = new SignalTableModel();
     
     public MessageViewTopComponent() {
 	initComponents();
 	setName(NbBundle.getMessage(MessageViewTopComponent.class, "CTL_MessageViewTopComponent"));
 	setToolTipText(NbBundle.getMessage(MessageViewTopComponent.class, "HINT_MessageViewTopComponent"));
 	
	DropTarget dt = new DropTarget(jButton1, new MessageSignalDropAdapter(model));
	jButton1.setDropTarget(dt);
 	
     }
 
     /** This method is called from within the constructor to
      * initialize the form.
      * WARNING: Do NOT modify this code. The content of this method is
      * always regenerated by the Form Editor.
      */
         // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
         private void initComponents() {
 
                 jSplitPane1 = new javax.swing.JSplitPane();
                 jPanel1 = new javax.swing.JPanel();
                 jScrollPane1 = new javax.swing.JScrollPane();
                 jTable1 = new javax.swing.JTable();
                 jToolBar1 = new javax.swing.JToolBar();
                 jButton1 = new javax.swing.JButton();
                 jPanel2 = new javax.swing.JPanel();
                 jLabel1 = new javax.swing.JLabel();
                 jTextField2 = new javax.swing.JTextField();
                 beanTreeView1 = new org.openide.explorer.view.BeanTreeView();
 
                 jSplitPane1.setDividerLocation(300);
 
                 jTable1.setModel(model);
                 jTable1.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
                 jScrollPane1.setViewportView(jTable1);
 
                 jToolBar1.setFloatable(false);
                 jToolBar1.setRollover(true);
 
                 org.openide.awt.Mnemonics.setLocalizedText(jButton1, org.openide.util.NbBundle.getMessage(MessageViewTopComponent.class, "MessageViewTopComponent.jButton1.text")); // NOI18N
                 jButton1.setFocusable(false);
                 jButton1.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
                 jButton1.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
                 jButton1.addActionListener(new java.awt.event.ActionListener() {
                         public void actionPerformed(java.awt.event.ActionEvent evt) {
                                 jButton1ActionPerformed(evt);
                         }
                 });
                 jToolBar1.add(jButton1);
 
                 javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
                 jPanel1.setLayout(jPanel1Layout);
                 jPanel1Layout.setHorizontalGroup(
                         jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                         .addComponent(jToolBar1, javax.swing.GroupLayout.DEFAULT_SIZE, 531, Short.MAX_VALUE)
                         .addComponent(jScrollPane1, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 531, Short.MAX_VALUE)
                 );
                 jPanel1Layout.setVerticalGroup(
                         jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                         .addGroup(jPanel1Layout.createSequentialGroup()
                                 .addComponent(jToolBar1, javax.swing.GroupLayout.PREFERRED_SIZE, 25, javax.swing.GroupLayout.PREFERRED_SIZE)
                                 .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                 .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 522, Short.MAX_VALUE))
                 );
 
                 jSplitPane1.setRightComponent(jPanel1);
 
                 org.openide.awt.Mnemonics.setLocalizedText(jLabel1, org.openide.util.NbBundle.getMessage(MessageViewTopComponent.class, "MessageViewTopComponent.jLabel1.text")); // NOI18N
 
                 jTextField2.setText(org.openide.util.NbBundle.getMessage(MessageViewTopComponent.class, "MessageViewTopComponent.jTextField2.text")); // NOI18N
                 jTextField2.addKeyListener(new java.awt.event.KeyAdapter() {
                         public void keyReleased(java.awt.event.KeyEvent evt) {
                                 jTextField2KeyReleased(evt);
                         }
                         public void keyTyped(java.awt.event.KeyEvent evt) {
                                 jTextField2KeyTyped(evt);
                         }
                 });
 
                 beanTreeView1.setRootVisible(false);
 
                 javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
                 jPanel2.setLayout(jPanel2Layout);
                 jPanel2Layout.setHorizontalGroup(
                         jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                         .addGroup(jPanel2Layout.createSequentialGroup()
                                 .addGap(6, 6, 6)
                                 .addComponent(jLabel1)
                                 .addGap(18, 18, 18)
                                 .addComponent(jTextField2, javax.swing.GroupLayout.DEFAULT_SIZE, 224, Short.MAX_VALUE))
                         .addComponent(beanTreeView1, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 300, Short.MAX_VALUE)
                 );
                 jPanel2Layout.setVerticalGroup(
                         jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                         .addGroup(jPanel2Layout.createSequentialGroup()
                                 .addContainerGap()
                                 .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                         .addComponent(jTextField2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                         .addComponent(jLabel1))
                                 .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                 .addComponent(beanTreeView1, javax.swing.GroupLayout.DEFAULT_SIZE, 506, Short.MAX_VALUE))
                 );
 
                 jSplitPane1.setLeftComponent(jPanel2);
 
                 javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
                 this.setLayout(layout);
                 layout.setHorizontalGroup(
                         layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                         .addComponent(jSplitPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 837, Short.MAX_VALUE)
                 );
                 layout.setVerticalGroup(
                         layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                         .addComponent(jSplitPane1, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 553, Short.MAX_VALUE)
                 );
         }// </editor-fold>//GEN-END:initComponents
 
 	private void jTextField2KeyTyped(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_jTextField2KeyTyped
 	}//GEN-LAST:event_jTextField2KeyTyped
 
 	private void jTextField2KeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_jTextField2KeyReleased
 	    String filter = jTextField2.getText();
 	    logger.log(Level.INFO, "filtering: {0}", filter);
 	    if(filter.equals("")) {
 		manager.setRootContext(root);		    
 	    } else {
 		SearchFilteredNode filteredNode = new SearchFilteredNode(root, filter);
 	        manager.setRootContext(filteredNode);
 	    }
 	}//GEN-LAST:event_jTextField2KeyReleased
 
 	private void jButton1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton1ActionPerformed
 	    int[] cols = jTable1.getSelectedColumns();
 	    
 	    for(Integer col : cols) {
 		model.remove(col);
 	    }
 	}//GEN-LAST:event_jButton1ActionPerformed
 
         // Variables declaration - do not modify//GEN-BEGIN:variables
         private org.openide.explorer.view.BeanTreeView beanTreeView1;
         private javax.swing.JButton jButton1;
         private javax.swing.JLabel jLabel1;
         private javax.swing.JPanel jPanel1;
         private javax.swing.JPanel jPanel2;
         private javax.swing.JScrollPane jScrollPane1;
         private javax.swing.JSplitPane jSplitPane1;
         private javax.swing.JTable jTable1;
         private javax.swing.JTextField jTextField2;
         private javax.swing.JToolBar jToolBar1;
         // End of variables declaration//GEN-END:variables
     @Override
     public void componentOpened() {
 	// TODO add custom code on component opening
     }
 
     @Override
     public void componentClosed() {
 	// TODO add custom code on component closing
     }
 
     public void setBus(Bus bus) {
 	this.bus = bus;	
 	setName(NbBundle.getMessage(MessageViewTopComponent.class, "CTL_MessageViewTopComponent") + " - " + bus.getName());		
 	MessageNodeFactory factory = new MessageNodeFactory(bus.getDescription());
 	root = new AbstractNode(Children.create(factory, true));
 	manager.setRootContext(root);
         associateLookup(ExplorerUtils.createLookup(manager, getActionMap()));
     }
 
     void writeProperties(java.util.Properties p) {
 	// better to version settings since initial version as advocated at
 	// http://wiki.apidesign.org/wiki/PropertyFiles
 	p.setProperty("version", "1.0");
 	// TODO store your settings
     }
 
     void readProperties(java.util.Properties p) {
 	String version = p.getProperty("version");
 	// TODO read your settings according to their version
     }
 
     @Override
     public ExplorerManager getExplorerManager() {
 	return manager;
     }
 }
