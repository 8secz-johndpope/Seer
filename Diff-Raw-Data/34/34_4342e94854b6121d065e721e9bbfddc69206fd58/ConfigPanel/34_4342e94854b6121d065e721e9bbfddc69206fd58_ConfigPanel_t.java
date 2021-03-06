 /*
  * To change this template, choose Tools | Templates
  * and open the template in the editor.
  */
 
 /*
  * ConfigPanel.java
  *
  * Created on 9-set-2010, 14.16.20
  */
 
 package it.unibz.krdb.obda.protege4.panels;
 
 import it.unibz.krdb.obda.owlapi.ReformulationPlatformPreferences;
 import it.unibz.krdb.obda.owlrefplatform.core.OBDAConstants;
 
 import java.awt.event.ActionEvent;
 import java.awt.event.ActionListener;
 
 import org.slf4j.Logger;
 
 /**
  *
  * @author Manfred Gerstgrasser
  */
 public class ConfigPanel extends javax.swing.JPanel {
 
 	/**
 	 * 
 	 */
 	private static final long serialVersionUID = 2263812414027790329L;
 	private ReformulationPlatformPreferences preference = null;
 	
 	private Logger log = org.slf4j.LoggerFactory.getLogger(ConfigPanel.class);
 	
     /** 
 	 * The constructor.
      */
     public ConfigPanel(ReformulationPlatformPreferences preference) {
     	this.preference = preference;
         initComponents();
         addActionListener();
         setSelections(preference);
     }
 
     private void addActionListener(){
     	
     	jRadioButtonMaterialAbox.addActionListener(new ActionListener() {
 			
 			@Override
 			public void actionPerformed(ActionEvent e) {
 				
 				jRadioButtonDirect.setEnabled(true);
				jRadioButtonDirect.setSelected(true);
 //		        jRadioButtonSemanticIndex.setEnabled(true);
 //		        jRadioButtonUniversal.setEnabled(true);
 //		        jRadioButtonUserProvidedDB.setEnabled(true);
 				jRadioButtonInMemoryDB.setEnabled(true);
				jRadioButtonDirect.setSelected(true);
 				jLabelDataLoc.setEnabled(true);
 				jLabeldbtype.setEnabled(true);
 				
 				preference.setCurrentValueOf(ReformulationPlatformPreferences.ABOX_MODE, OBDAConstants.CLASSIC);
				preference.setCurrentValueOf(ReformulationPlatformPreferences.DATA_LOCATION, OBDAConstants.INMEMORY);

 			}
 		});
     	jRadioButtonVirualABox.addActionListener(new ActionListener() {
 			
 			@Override
 			public void actionPerformed(ActionEvent e) {
 				jRadioButtonDirect.setEnabled(false);
 		        jRadioButtonSemanticIndex.setEnabled(false);
 		        jRadioButtonUniversal.setEnabled(false);
 		        jRadioButtonUserProvidedDB.setEnabled(false);
 				jRadioButtonInMemoryDB.setEnabled(false);
 				jLabelDataLoc.setEnabled(false);
 				jLabeldbtype.setEnabled(false);
 				
 				preference.setCurrentValueOf(ReformulationPlatformPreferences.ABOX_MODE, OBDAConstants.VIRTUAL);
 				preference.setCurrentValueOf(ReformulationPlatformPreferences.DATA_LOCATION, OBDAConstants.PROVIDED);
 			}
 		});
         jRadioButtonDirect.addActionListener(new ActionListener() {
 			
 			@Override
 			public void actionPerformed(ActionEvent e) {
 				preference.setCurrentValueOf(ReformulationPlatformPreferences.DBTYPE, OBDAConstants.DIRECT);
 
 			}
 		});
         jRadioButtonInMemoryDB.addActionListener(new ActionListener() {
 			
 			@Override
 			public void actionPerformed(ActionEvent e) {
 				preference.setCurrentValueOf(ReformulationPlatformPreferences.DATA_LOCATION, OBDAConstants.INMEMORY);
 				
 			}
 		});
         jRadioButtonSemanticIndex.addActionListener(new ActionListener() {
 			
 			@Override
 			public void actionPerformed(ActionEvent e) {
 				preference.setCurrentValueOf(ReformulationPlatformPreferences.DBTYPE, OBDAConstants.SEMANTIC);
 				
 			}
 		});
         jRadioButtonUniversal.addActionListener(new ActionListener() {
 			
 			@Override
 			public void actionPerformed(ActionEvent e) {
 				preference.setCurrentValueOf(ReformulationPlatformPreferences.DBTYPE, OBDAConstants.UNIVERSAL);
 				
 			}
 		});
         jRadioButtonUserProvidedDB.addActionListener(new ActionListener() {
 			
 			@Override
 			public void actionPerformed(ActionEvent e) {
 				preference.setCurrentValueOf(ReformulationPlatformPreferences.DATA_LOCATION, OBDAConstants.PROVIDED);
 				
 			}
 		});
     }
     
     private void setSelections(ReformulationPlatformPreferences p){
 		
     	
 //        String refvalue = (String) cb.getSelectedItem();
 //        preference.setCurrentValueOf(
 //        		ReformulationPlatformPreferences.REFORMULATION_TECHNIQUE, optValue);
 
    	jRadioButtonInMemoryDB.setSelected(true);
         
         
         String refvalue = (String)p.getCurrentValue(ReformulationPlatformPreferences.REFORMULATION_TECHNIQUE);
         if(refvalue.equals(OBDAConstants.PERFECTREFORMULATION)){
         	 cmbReformulationMethods.setSelectedIndex(0);
         }else if(refvalue.equals(OBDAConstants.UCQBASED)){
         	 cmbReformulationMethods.setSelectedIndex(1);
         }      
     	
 		String value = (String)p.getCurrentValue(ReformulationPlatformPreferences.ABOX_MODE);
 		if (value.equals(OBDAConstants.VIRTUAL)) {
 			jRadioButtonVirualABox.setSelected(true);
 			jRadioButtonMaterialAbox.setSelected(false);
 			jRadioButtonDirect.setEnabled(false);
 	        jRadioButtonSemanticIndex.setEnabled(false);
 	        jRadioButtonUniversal.setEnabled(false);
 	        jRadioButtonUserProvidedDB.setEnabled(false);
 			jRadioButtonInMemoryDB.setEnabled(false);
 			jLabelDataLoc.setEnabled(false);
 			jLabeldbtype.setEnabled(false);
 		} else if (value.equals(OBDAConstants.CLASSIC)) {
 			jRadioButtonVirualABox.setSelected(false);
 			jRadioButtonMaterialAbox.setSelected(true);
 			jRadioButtonDirect.setEnabled(true);
 //			jRadioButtonSemanticIndex.setEnabled(true);
 //			jRadioButtonUniversal.setEnabled(true);
 			jRadioButtonUserProvidedDB.setEnabled(false);
 			jRadioButtonInMemoryDB.setEnabled(true);
 			jRadioButtonInMemoryDB.setSelected(true);
 			jLabelDataLoc.setEnabled(true);
 			jLabeldbtype.setEnabled(true);
 
 		} else {
 			log.warn("Unknown ABOX mode: {}", value);
 		}
 		
 		value = (String)p.getCurrentValue(ReformulationPlatformPreferences.DATA_LOCATION);
 		if (value.equals(OBDAConstants.PROVIDED)) {
 			jRadioButtonUserProvidedDB.setSelected(false);
 			jRadioButtonInMemoryDB.setSelected(false);
 		} else if (value.equals(OBDAConstants.INMEMORY)) {
 			jRadioButtonUserProvidedDB.setSelected(false);
 			jRadioButtonInMemoryDB.setSelected(true);
 		} else {
 			log.warn("Unknown data store mode: {}", value);
 		}
 		
 		value = (String)p.getCurrentValue(ReformulationPlatformPreferences.DBTYPE);
 		if (value.equals(OBDAConstants.DIRECT)) {
 			jRadioButtonDirect.setSelected(true);
 			jRadioButtonUniversal.setSelected(false);
 			jRadioButtonSemanticIndex.setSelected(false);
 		} else if (value.equals(OBDAConstants.UNIVERSAL)) {
 			jRadioButtonDirect.setSelected(false);
 			jRadioButtonUniversal.setSelected(true);
 			jRadioButtonSemanticIndex.setSelected(false);
 		} else if (value.equals(OBDAConstants.SEMANTIC)) {
 			jRadioButtonDirect.setSelected(false);
 			jRadioButtonUniversal.setSelected(false);
 			jRadioButtonSemanticIndex.setSelected(true);
 		} else {
 			log.warn("Unknown DB type mode: {}", value);
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
         java.awt.GridBagConstraints gridBagConstraints;
 
         mappingMode = new javax.swing.ButtonGroup();
         mapper = new javax.swing.ButtonGroup();
         datalocationGroup = new javax.swing.ButtonGroup();
         AboxMode = new javax.swing.ButtonGroup();
         pnlTWOption = new javax.swing.JPanel();
         lblTechniqueWrapper = new javax.swing.JLabel();
         cmbTechniqueWrapper = new javax.swing.JComboBox();
         pnlTWConfiguration = new javax.swing.JPanel();
         pnlReformulationMethods = new javax.swing.JPanel();
         lblReformulationTechnique = new javax.swing.JLabel();
         cmbReformulationMethods = new javax.swing.JComboBox();
         pnlABoxConfiguration = new javax.swing.JPanel();
         pnlMappingOptions = new javax.swing.JPanel();
         jRadioButtonUniversal = new javax.swing.JRadioButton();
         jRadioButtonSemanticIndex = new javax.swing.JRadioButton();
         jRadioButtonDirect = new javax.swing.JRadioButton();
         jRadioButtonUserProvidedDB = new javax.swing.JRadioButton();
         jRadioButtonInMemoryDB = new javax.swing.JRadioButton();
         jRadioButtonVirualABox = new javax.swing.JRadioButton();
         jRadioButtonMaterialAbox = new javax.swing.JRadioButton();
         jLabelDataLoc = new javax.swing.JLabel();
         jLabeldbtype = new javax.swing.JLabel();
 
         setMinimumSize(new java.awt.Dimension(500, 490));
         setPreferredSize(new java.awt.Dimension(500, 493));
         setLayout(new java.awt.BorderLayout(0, 15));
 
         pnlTWOption.setBorder(javax.swing.BorderFactory.createTitledBorder(javax.swing.BorderFactory.createLineBorder(java.awt.Color.lightGray), "Technique Wrapper Options"));
         pnlTWOption.setMinimumSize(new java.awt.Dimension(590, 80));
         pnlTWOption.setPreferredSize(new java.awt.Dimension(590, 100));
         pnlTWOption.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.CENTER, 5, 25));
 
         lblTechniqueWrapper.setText("Technique Wrapper:");
         lblTechniqueWrapper.setMinimumSize(new java.awt.Dimension(170, 30));
         lblTechniqueWrapper.setPreferredSize(new java.awt.Dimension(140, 20));
         pnlTWOption.add(lblTechniqueWrapper);
 
         cmbTechniqueWrapper.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Quest Technique Wrapper" }));
         cmbTechniqueWrapper.setMaximumSize(new java.awt.Dimension(180, 32767));
         cmbTechniqueWrapper.setMinimumSize(new java.awt.Dimension(125, 18));
         cmbTechniqueWrapper.setPreferredSize(new java.awt.Dimension(280, 20));
         cmbTechniqueWrapper.addActionListener(new java.awt.event.ActionListener() {
             public void actionPerformed(java.awt.event.ActionEvent evt) {
                 cmbTechniqueWrapperActionPerformed(evt);
             }
         });
         pnlTWOption.add(cmbTechniqueWrapper);
 
         add(pnlTWOption, java.awt.BorderLayout.NORTH);
         pnlTWOption.getAccessibleContext().setAccessibleName("Bolzano Reformulation Technique");
 
         pnlTWConfiguration.setBorder(javax.swing.BorderFactory.createTitledBorder(javax.swing.BorderFactory.createLineBorder(java.awt.Color.lightGray), "Technique Wrapper Configuration"));
         pnlTWConfiguration.setMinimumSize(new java.awt.Dimension(500, 350));
         pnlTWConfiguration.setPreferredSize(new java.awt.Dimension(600, 350));
         pnlTWConfiguration.setLayout(new java.awt.GridBagLayout());
 
         pnlReformulationMethods.setBorder(javax.swing.BorderFactory.createTitledBorder(javax.swing.BorderFactory.createLineBorder(java.awt.Color.lightGray), "Reformulation Methods\n"));
         pnlReformulationMethods.setMinimumSize(new java.awt.Dimension(590, 100));
         pnlReformulationMethods.setPreferredSize(new java.awt.Dimension(590, 100));
         pnlReformulationMethods.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.CENTER, 5, 25));
 
         lblReformulationTechnique.setText("Reformulation Technique: ");
         lblReformulationTechnique.setMinimumSize(new java.awt.Dimension(150, 30));
         lblReformulationTechnique.setPreferredSize(new java.awt.Dimension(180, 20));
         pnlReformulationMethods.add(lblReformulationTechnique);
 
         cmbReformulationMethods.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "PerfectRef", "Quest's reformulation" }));
         cmbReformulationMethods.setPreferredSize(new java.awt.Dimension(220, 20));
         cmbReformulationMethods.addActionListener(new java.awt.event.ActionListener() {
             public void actionPerformed(java.awt.event.ActionEvent evt) {
                 cmbReformulationMethodsActionPerformed(evt);
             }
         });
         pnlReformulationMethods.add(cmbReformulationMethods);
 
         gridBagConstraints = new java.awt.GridBagConstraints();
         gridBagConstraints.gridx = 0;
         gridBagConstraints.gridy = 0;
         gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
         gridBagConstraints.ipadx = 144;
         gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
         gridBagConstraints.weightx = 1.0;
         gridBagConstraints.insets = new java.awt.Insets(10, 6, 0, 6);
         pnlTWConfiguration.add(pnlReformulationMethods, gridBagConstraints);
 
         pnlABoxConfiguration.setBorder(javax.swing.BorderFactory.createTitledBorder(javax.swing.BorderFactory.createLineBorder(java.awt.Color.lightGray), "ABox Configuration"));
         pnlABoxConfiguration.setMinimumSize(new java.awt.Dimension(590, 200));
         pnlABoxConfiguration.setPreferredSize(new java.awt.Dimension(590, 180));
         pnlABoxConfiguration.setLayout(new java.awt.BorderLayout());
 
         pnlMappingOptions.setMaximumSize(new java.awt.Dimension(2147483647, 2147483647));
         pnlMappingOptions.setMinimumSize(new java.awt.Dimension(215, 50));
         pnlMappingOptions.setPreferredSize(new java.awt.Dimension(470, 50));
         pnlMappingOptions.setLayout(new java.awt.GridBagLayout());
 
         mapper.add(jRadioButtonUniversal);
         jRadioButtonUniversal.setText("Universal");
         jRadioButtonUniversal.setEnabled(false);
         gridBagConstraints = new java.awt.GridBagConstraints();
         gridBagConstraints.gridx = 0;
         gridBagConstraints.gridy = 5;
         gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
         gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
         gridBagConstraints.weightx = 1.0;
         gridBagConstraints.insets = new java.awt.Insets(0, 80, 0, 21);
         pnlMappingOptions.add(jRadioButtonUniversal, gridBagConstraints);
 
         mapper.add(jRadioButtonSemanticIndex);
         jRadioButtonSemanticIndex.setText("Semantic Index");
         jRadioButtonSemanticIndex.setEnabled(false);
         gridBagConstraints = new java.awt.GridBagConstraints();
         gridBagConstraints.gridx = 0;
         gridBagConstraints.gridy = 6;
         gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
         gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
         gridBagConstraints.weightx = 1.0;
         gridBagConstraints.insets = new java.awt.Insets(0, 80, 0, 21);
         pnlMappingOptions.add(jRadioButtonSemanticIndex, gridBagConstraints);
 
         mapper.add(jRadioButtonDirect);
         jRadioButtonDirect.setSelected(true);
         jRadioButtonDirect.setText("Direct");
         gridBagConstraints = new java.awt.GridBagConstraints();
         gridBagConstraints.gridx = 0;
         gridBagConstraints.gridy = 4;
         gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
         gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
         gridBagConstraints.weightx = 1.0;
         gridBagConstraints.insets = new java.awt.Insets(0, 80, 0, 21);
         pnlMappingOptions.add(jRadioButtonDirect, gridBagConstraints);
 
         datalocationGroup.add(jRadioButtonUserProvidedDB);
         jRadioButtonUserProvidedDB.setText("User provides JDBC data source with an ABox");
         gridBagConstraints = new java.awt.GridBagConstraints();
         gridBagConstraints.gridx = 0;
         gridBagConstraints.gridy = 8;
         gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
         gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
         gridBagConstraints.weightx = 1.0;
         gridBagConstraints.insets = new java.awt.Insets(0, 80, 0, 21);
         pnlMappingOptions.add(jRadioButtonUserProvidedDB, gridBagConstraints);
 
         datalocationGroup.add(jRadioButtonInMemoryDB);
         jRadioButtonInMemoryDB.setText("Read ABox from ontology and store it in an in-memory data base");
         gridBagConstraints = new java.awt.GridBagConstraints();
         gridBagConstraints.gridx = 0;
         gridBagConstraints.gridy = 9;
         gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
         gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
         gridBagConstraints.weightx = 1.0;
         gridBagConstraints.insets = new java.awt.Insets(0, 80, 0, 21);
         pnlMappingOptions.add(jRadioButtonInMemoryDB, gridBagConstraints);
 
         AboxMode.add(jRadioButtonVirualABox);
         jRadioButtonVirualABox.setText("Virtual ABox");
         gridBagConstraints = new java.awt.GridBagConstraints();
         gridBagConstraints.gridx = 0;
         gridBagConstraints.gridy = 1;
         gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
         gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
         gridBagConstraints.weightx = 1.0;
         gridBagConstraints.insets = new java.awt.Insets(0, 20, 0, 20);
         pnlMappingOptions.add(jRadioButtonVirualABox, gridBagConstraints);
 
         AboxMode.add(jRadioButtonMaterialAbox);
         jRadioButtonMaterialAbox.setText("Classic ABox");
         gridBagConstraints = new java.awt.GridBagConstraints();
         gridBagConstraints.gridx = 0;
         gridBagConstraints.gridy = 2;
         gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
         gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
         gridBagConstraints.weightx = 1.0;
         gridBagConstraints.insets = new java.awt.Insets(0, 20, 0, 20);
         pnlMappingOptions.add(jRadioButtonMaterialAbox, gridBagConstraints);
 
         jLabelDataLoc.setText("Data Location");
         gridBagConstraints = new java.awt.GridBagConstraints();
         gridBagConstraints.gridx = 0;
         gridBagConstraints.gridy = 7;
         gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
         gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
         gridBagConstraints.insets = new java.awt.Insets(0, 40, 0, 21);
         pnlMappingOptions.add(jLabelDataLoc, gridBagConstraints);
 
         jLabeldbtype.setText("DBType");
         gridBagConstraints = new java.awt.GridBagConstraints();
         gridBagConstraints.gridx = 0;
         gridBagConstraints.gridy = 3;
         gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
         gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
         gridBagConstraints.insets = new java.awt.Insets(0, 40, 0, 21);
         pnlMappingOptions.add(jLabeldbtype, gridBagConstraints);
 
         pnlABoxConfiguration.add(pnlMappingOptions, java.awt.BorderLayout.WEST);
 
         gridBagConstraints = new java.awt.GridBagConstraints();
         gridBagConstraints.gridx = 0;
         gridBagConstraints.gridy = 1;
         gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
         gridBagConstraints.ipady = 135;
         gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
         gridBagConstraints.weightx = 1.0;
         gridBagConstraints.weighty = 1.0;
         gridBagConstraints.insets = new java.awt.Insets(15, 6, 7, 6);
         pnlTWConfiguration.add(pnlABoxConfiguration, gridBagConstraints);
 
         add(pnlTWConfiguration, java.awt.BorderLayout.CENTER);
     }// </editor-fold>//GEN-END:initComponents
 
 //    private void applyPreferences() {
 //    	
 //    	String aboxmode = (String)preference.getCurrentValue(ReformulationPlatformPreferences.ABOX_MODE);
 //    	if(aboxmode.equals("virtual")){
 //    		
 //    	    jRadioButtonDirect.setEnabled(false);
 //    	    jRadioButtonInMemoryDB.setEnabled(false);
 //    	    jRadioButtonMaterialAbox.setEnabled(false);
 //    	    jRadioButtonSemanticIndex.setEnabled(false);
 //    	    jRadioButtonUniversal.setEnabled(false);
 //    	    jRadioButtonUserProvidedDB.setEnabled(false);
 //    	    jRadioButtonVirualABox.setEnabled(true);
 //    	    jRadioButtonVirualABox.setSelected(true);
 //    		
 //    	}else{
 //    		
 //    	}
 //    }
     
     private void cmbTechniqueWrapperActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cmbTechniqueWrapperActionPerformed
         // TODO add your handling code here:
     }//GEN-LAST:event_cmbTechniqueWrapperActionPerformed
     
     private void cmbReformulationMethodsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cmbReformulationMethodsActionPerformed
         javax.swing.JComboBox cb = (javax.swing.JComboBox) evt.getSource();
         String optValue = (String) cb.getSelectedItem();
         if(optValue.equals(OBDAConstants.PERFECTREFORMULATION)){
         	 preference.setCurrentValueOf(
              		ReformulationPlatformPreferences.REFORMULATION_TECHNIQUE, OBDAConstants.PERFECTREFORMULATION);
         }else{
         	 preference.setCurrentValueOf(
         			 ReformulationPlatformPreferences.REFORMULATION_TECHNIQUE, OBDAConstants.UCQBASED);
         }
     }//GEN-LAST:event_cmbReformulationMethodsActionPerformed
                
     // Variables declaration - do not modify//GEN-BEGIN:variables
     private javax.swing.ButtonGroup AboxMode;
     private javax.swing.JComboBox cmbReformulationMethods;
     private javax.swing.JComboBox cmbTechniqueWrapper;
     private javax.swing.ButtonGroup datalocationGroup;
     private javax.swing.JLabel jLabelDataLoc;
     private javax.swing.JLabel jLabeldbtype;
     private javax.swing.JRadioButton jRadioButtonDirect;
     private javax.swing.JRadioButton jRadioButtonInMemoryDB;
     private javax.swing.JRadioButton jRadioButtonMaterialAbox;
     private javax.swing.JRadioButton jRadioButtonSemanticIndex;
     private javax.swing.JRadioButton jRadioButtonUniversal;
     private javax.swing.JRadioButton jRadioButtonUserProvidedDB;
     private javax.swing.JRadioButton jRadioButtonVirualABox;
     private javax.swing.JLabel lblReformulationTechnique;
     private javax.swing.JLabel lblTechniqueWrapper;
     private javax.swing.ButtonGroup mapper;
     private javax.swing.ButtonGroup mappingMode;
     private javax.swing.JPanel pnlABoxConfiguration;
     private javax.swing.JPanel pnlMappingOptions;
     private javax.swing.JPanel pnlReformulationMethods;
     private javax.swing.JPanel pnlTWConfiguration;
     private javax.swing.JPanel pnlTWOption;
     // End of variables declaration//GEN-END:variables
 
 }
