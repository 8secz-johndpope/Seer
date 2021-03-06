 /* -*- tab-width: 4 -*-
  *
  * Electric(tm) VLSI Design System
  *
  * File: GetInfoExport.java
  *
  * Copyright (c) 2003 Sun Microsystems and Static Free Software
  *
  * Electric(tm) is free software; you can redistribute it and/or modify
  * it under the terms of the GNU General Public License as published by
  * the Free Software Foundation; either version 2 of the License, or
  * (at your option) any later version.
  *
  * Electric(tm) is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  * GNU General Public License for more details.
  *
  * You should have received a copy of the GNU General Public License
  * along with Electric(tm); see the file COPYING.  If not, write to
  * the Free Software Foundation, Inc., 59 Temple Place, Suite 330,
  * Boston, Mass 02111-1307, USA.
  */
 package com.sun.electric.tool.user.dialogs;
 
 import com.sun.electric.database.change.Undo;
 import com.sun.electric.database.geometry.EMath;
 import com.sun.electric.database.hierarchy.Cell;
 import com.sun.electric.database.hierarchy.Export;
 import com.sun.electric.database.prototype.PortProto;
 import com.sun.electric.database.text.TextUtils;
 import com.sun.electric.database.topology.NodeInst;
 import com.sun.electric.database.variable.ElectricObject;
 import com.sun.electric.database.variable.TextDescriptor;
 import com.sun.electric.database.variable.Variable;
 import com.sun.electric.tool.Job;
 import com.sun.electric.tool.user.User;
 import com.sun.electric.tool.user.Highlight;
 import com.sun.electric.tool.user.ui.EditWindow;
 import com.sun.electric.tool.user.ui.TopLevel;
 
 import java.awt.Font;
 import java.awt.GraphicsEnvironment;
 import java.util.List;
 import java.util.Iterator;
 import javax.swing.JFrame;
 
 
 /**
  * Class to handle the "Export Get-Info" dialog.
  */
 public class GetInfoExport extends EDialog
 {
 	private static GetInfoExport theDialog = null;
 	private Export shownExport;
 	private String initialName;
 	private String initialRefName;
 	private PortProto.Characteristic initialCharacteristic;
 	private boolean initialBodyOnly, initialAlwaysDrawn;
 
     private TextInfoPanel textPanel;
 
 	/**
 	 * Method to show the Export Get-Info dialog.
 	 */
 	public static void showDialog()
 	{
 		if (theDialog == null)
 		{
             if (TopLevel.isMDIMode()) {
 			    JFrame jf = TopLevel.getCurrentJFrame();
                 theDialog = new GetInfoExport(jf, false);
             } else {
                 theDialog = new GetInfoExport(null, false);
             }
 		}
 		theDialog.show();
 	}
 
 	/**
 	 * Method to reload the Export Get-Info dialog from the current highlighting.
 	 */
 	public static void load()
 	{
 		if (theDialog == null) return;
 		theDialog.loadExportInfo();
 	}
 
 	private void loadExportInfo()
 	{
 		// must have a single export selected
 		Export pp = null;
 		int exportCount = 0;
 		for(Iterator it = Highlight.getHighlights(); it.hasNext(); )
 		{
 			Highlight h = (Highlight)it.next();
 			if (h.getType() != Highlight.Type.TEXT) continue;
 			if (h.getVar() != null) continue;
 			ElectricObject eobj = h.getElectricObject();
 			if (eobj instanceof Export)
 			{
 				pp = (Export)eobj;
 				exportCount++;
 			}
 		}
 		if (exportCount > 1) pp = null;
 
         boolean enabled = true;
         if (pp == null) enabled = false;
 
         // set enabled state of dialog
         theText.setEditable(enabled);
         bodyOnly.setEnabled(enabled);
         alwaysDrawn.setEnabled(enabled);
         characteristics.setEnabled(enabled);
         refName.setEditable(enabled);
 
         if (!enabled) {
 		    shownExport = null;
             theText.setText("");
             refName.setText("");
             textPanel.setTextDescriptor(null, null, null);
 			return;
 		}
 
         // set name
 		initialName = pp.getProtoName();
 		theText.setText(initialName);
 
         // set Body and Always Drawn check boxes
 		initialBodyOnly = pp.isBodyOnly();
 		bodyOnly.setSelected(initialBodyOnly);
 		initialAlwaysDrawn = pp.isAlwaysDrawn();
 		alwaysDrawn.setSelected(initialAlwaysDrawn);
 
         // set characteristic and reference name
 		initialCharacteristic = pp.getCharacteristic();
 		characteristics.setSelectedItem(initialCharacteristic.getName());
 		initialRefName = "";
 		if (initialCharacteristic == PortProto.Characteristic.REFBASE ||
 			initialCharacteristic == PortProto.Characteristic.REFIN ||
 			initialCharacteristic == PortProto.Characteristic.REFOUT)
 		{
 			Variable var = pp.getVar(Export.EXPORT_REFERENCE_NAME);
 			if (var != null)
 				initialRefName = var.describe(-1, -1);
 			refName.setEditable(true);
 		} else
 		{
 			refName.setEditable(false);
 		}
 		refName.setText(initialRefName);
 
         // set text info panel
         TextDescriptor td = pp.getTextDescriptor();
         textPanel.setTextDescriptor(td, null, pp);
 
 		shownExport = pp;
 	}
 
 	/** Creates new form Export Get-Info */
 	private GetInfoExport(java.awt.Frame parent, boolean modal)
 	{
 		super(parent, modal);
 		initComponents();
         getRootPane().setDefaultButton(ok);
 
         // set characteristic combo box
 		List chars = PortProto.Characteristic.getOrderedCharacteristics();
 		for(Iterator it = chars.iterator(); it.hasNext(); )
 		{
 			PortProto.Characteristic ch = (PortProto.Characteristic)it.next();
 			characteristics.addItem(ch.getName());
 		}
 
         // add textPanel
         textPanel = new TextInfoPanel();
         java.awt.GridBagConstraints gridBagConstraints = new java.awt.GridBagConstraints();
         gridBagConstraints.gridx = 0;
         gridBagConstraints.gridy = 2;
         gridBagConstraints.gridwidth = 4;
         gridBagConstraints.insets = new java.awt.Insets(2, 4, 2, 4);
         gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
         gridBagConstraints.weightx = 1.0;
         getContentPane().add(textPanel, gridBagConstraints);
         pack();
 
 		loadExportInfo();
 	}
 
 	protected static class ChangeExport extends Job
 	{
 		Export pp;
         String newName;
         boolean newBodyOnly, newAlwaysDrawn;
         PortProto.Characteristic newChar;
         String newRefName;
 
 		protected ChangeExport(Export pp,
                 String newName,
                 boolean newBodyOnly, boolean newAlwaysDrawn,
                 PortProto.Characteristic newChar, String newRefName)
 		{
 			super("Modify Export", User.tool, Job.Type.CHANGE, null, null, Job.Priority.USER);
 			this.pp = pp;
 			this.newName = newName;
             this.newBodyOnly = newBodyOnly;
             this.newAlwaysDrawn = newAlwaysDrawn;
             this.newChar = newChar;
             this.newRefName = newRefName;
 			startJob();
 		}
 
 		public boolean doIt()
 		{
 		    // change the name
 			pp.setProtoName(newName);
 
 			// change the body-only
 			if (newBodyOnly) pp.setBodyOnly(); else
 				pp.clearBodyOnly();
             // change always drawn
 			if (newAlwaysDrawn) pp.setAlwaysDrawn(); else
 				pp.clearAlwaysDrawn();
 
 			// change the characteristic
 			pp.setCharacteristic(newChar);
 
             // change reference name
 			if (newChar.isReference())
 				pp.newVar(Export.EXPORT_REFERENCE_NAME, newRefName);
 
 			Undo.redrawObject(pp.getOriginalPort().getNodeInst());
 //				pp.getOriginalPort().getNodeInst().modifyInstance(0, 0, 0, 0, 0);
 			return true;
 		}
 	}
 
     /**
      * Job to trigger update to Attributes dialog.  Type set to CHANGE and priority to USER
      * so that in queues in order behind other Jobs from this class: this assures it will
      * occur after the queued changes
      */
     private static class UpdateDialog extends Job {
         private UpdateDialog() {
             super("Update Attributes Dialog", User.tool, Job.Type.CHANGE, null, null, Job.Priority.USER);
             startJob();
         }
         public boolean doIt() {
             GetInfoExport.load();
 			return true;
         }
     }
 
 	/** This method is called from within the constructor to
 	 * initialize the form.
 	 * WARNING: Do NOT modify this code. The content of this method is
 	 * always regenerated by the Form Editor.
 	 */
     private void initComponents()//GEN-BEGIN:initComponents
     {
         java.awt.GridBagConstraints gridBagConstraints;
 
         grab = new javax.swing.ButtonGroup();
         sizes = new javax.swing.ButtonGroup();
         cancel = new javax.swing.JButton();
         ok = new javax.swing.JButton();
         apply = new javax.swing.JButton();
         leftSide = new javax.swing.JPanel();
         jLabel10 = new javax.swing.JLabel();
         characteristics = new javax.swing.JComboBox();
         jLabel1 = new javax.swing.JLabel();
         refName = new javax.swing.JTextField();
         bodyOnly = new javax.swing.JCheckBox();
         alwaysDrawn = new javax.swing.JCheckBox();
         header = new javax.swing.JLabel();
         theText = new javax.swing.JTextField();
         attributes = new javax.swing.JButton();
 
         getContentPane().setLayout(new java.awt.GridBagLayout());
 
         setTitle("Export Properties");
         setName("");
         addWindowListener(new java.awt.event.WindowAdapter()
         {
             public void windowClosing(java.awt.event.WindowEvent evt)
             {
                 closeDialog(evt);
             }
         });
 
         cancel.setText("Cancel");
         cancel.addActionListener(new java.awt.event.ActionListener()
         {
             public void actionPerformed(java.awt.event.ActionEvent evt)
             {
                 cancelActionPerformed(evt);
             }
         });
 
         gridBagConstraints = new java.awt.GridBagConstraints();
         gridBagConstraints.gridx = 0;
         gridBagConstraints.gridy = 3;
         gridBagConstraints.weightx = 0.25;
         gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
         getContentPane().add(cancel, gridBagConstraints);
 
         ok.setText("OK");
         ok.addActionListener(new java.awt.event.ActionListener()
         {
             public void actionPerformed(java.awt.event.ActionEvent evt)
             {
                 okActionPerformed(evt);
             }
         });
 
         gridBagConstraints = new java.awt.GridBagConstraints();
         gridBagConstraints.gridx = 3;
         gridBagConstraints.gridy = 3;
         gridBagConstraints.weightx = 0.25;
         gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
         getContentPane().add(ok, gridBagConstraints);
 
         apply.setText("Apply");
         apply.addActionListener(new java.awt.event.ActionListener()
         {
             public void actionPerformed(java.awt.event.ActionEvent evt)
             {
                 applyActionPerformed(evt);
             }
         });
 
         gridBagConstraints = new java.awt.GridBagConstraints();
         gridBagConstraints.gridx = 2;
         gridBagConstraints.gridy = 3;
         gridBagConstraints.weightx = 0.25;
         gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
         getContentPane().add(apply, gridBagConstraints);
 
         leftSide.setLayout(new java.awt.GridBagLayout());
 
         leftSide.setBorder(new javax.swing.border.EtchedBorder());
         jLabel10.setText("Characteristics:");
         gridBagConstraints = new java.awt.GridBagConstraints();
         gridBagConstraints.gridx = 0;
         gridBagConstraints.gridy = 0;
         gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
         gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
         leftSide.add(jLabel10, gridBagConstraints);
 
         characteristics.addActionListener(new java.awt.event.ActionListener()
         {
             public void actionPerformed(java.awt.event.ActionEvent evt)
             {
                 characteristicsActionPerformed(evt);
             }
         });
 
         gridBagConstraints = new java.awt.GridBagConstraints();
         gridBagConstraints.gridx = 1;
         gridBagConstraints.gridy = 0;
         gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
         gridBagConstraints.weightx = 0.1;
         gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
         leftSide.add(characteristics, gridBagConstraints);
 
         jLabel1.setText("Reference name:");
         gridBagConstraints = new java.awt.GridBagConstraints();
         gridBagConstraints.gridx = 0;
         gridBagConstraints.gridy = 1;
         gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
         gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
         leftSide.add(jLabel1, gridBagConstraints);
 
         refName.setText(" ");
         gridBagConstraints = new java.awt.GridBagConstraints();
         gridBagConstraints.gridx = 1;
         gridBagConstraints.gridy = 1;
         gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
         gridBagConstraints.weightx = 0.1;
         gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
         leftSide.add(refName, gridBagConstraints);
 
         bodyOnly.setText("Body only");
         gridBagConstraints = new java.awt.GridBagConstraints();
         gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
         gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
         gridBagConstraints.insets = new java.awt.Insets(0, 6, 0, 0);
         leftSide.add(bodyOnly, gridBagConstraints);
 
         alwaysDrawn.setText("Always drawn");
         gridBagConstraints = new java.awt.GridBagConstraints();
         gridBagConstraints.gridx = 2;
         gridBagConstraints.gridy = 1;
         gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
         gridBagConstraints.insets = new java.awt.Insets(0, 6, 0, 0);
         leftSide.add(alwaysDrawn, gridBagConstraints);
 
         gridBagConstraints = new java.awt.GridBagConstraints();
         gridBagConstraints.gridx = 0;
         gridBagConstraints.gridy = 1;
         gridBagConstraints.gridwidth = 4;
         gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
         gridBagConstraints.weightx = 1.0;
         gridBagConstraints.insets = new java.awt.Insets(2, 4, 2, 4);
         getContentPane().add(leftSide, gridBagConstraints);
 
         header.setText("Export name:");
         gridBagConstraints = new java.awt.GridBagConstraints();
         gridBagConstraints.gridx = 0;
         gridBagConstraints.gridy = 0;
         gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
         gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
         getContentPane().add(header, gridBagConstraints);
 
         gridBagConstraints = new java.awt.GridBagConstraints();
         gridBagConstraints.gridx = 1;
         gridBagConstraints.gridy = 0;
         gridBagConstraints.gridwidth = 3;
         gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
         gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
         gridBagConstraints.weightx = 1.0;
         gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
         getContentPane().add(theText, gridBagConstraints);
 
         attributes.setText("Attributes");
         attributes.addActionListener(new java.awt.event.ActionListener()
         {
             public void actionPerformed(java.awt.event.ActionEvent evt)
             {
                 attributesActionPerformed(evt);
             }
         });
 
         gridBagConstraints = new java.awt.GridBagConstraints();
         gridBagConstraints.gridx = 1;
         gridBagConstraints.gridy = 3;
         gridBagConstraints.weightx = 0.25;
         gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
         getContentPane().add(attributes, gridBagConstraints);
 
         pack();
     }//GEN-END:initComponents
 
 	private void characteristicsActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_characteristicsActionPerformed
 	{//GEN-HEADEREND:event_characteristicsActionPerformed
 		String stringNow = (String)characteristics.getSelectedItem();
 		PortProto.Characteristic ch = PortProto.Characteristic.findCharacteristic(stringNow);
 		refName.setEditable(ch.isReference());
 	}//GEN-LAST:event_characteristicsActionPerformed
 
 	private void attributesActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_attributesActionPerformed
 	{//GEN-HEADEREND:event_attributesActionPerformed
 		Attributes.showDialog();
 	}//GEN-LAST:event_attributesActionPerformed
 
 	private void applyActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_applyActionPerformed
 	{//GEN-HEADEREND:event_applyActionPerformed
 		if (shownExport == null) return;
 
         // check if changes to be made
         boolean changed = false;
 
         // check name
         String newName = theText.getText();
         if (!newName.equals(initialName)) changed = true;
         // check body only
         boolean newBodyOnly = bodyOnly.isSelected();
         if (newBodyOnly != initialBodyOnly) changed = true;
         // check always drawn
         boolean newAlwaysDrawn = alwaysDrawn.isSelected();
         if (newAlwaysDrawn != initialAlwaysDrawn) changed = true;
         // check characteristic
         String newCharName = (String)characteristics.getSelectedItem();
         PortProto.Characteristic newChar = PortProto.Characteristic.findCharacteristic(newCharName);
         if (newChar != initialCharacteristic) changed = true;
         // check reference name
         String newRefName = refName.getText();
         if (!newRefName.equals(initialRefName)) changed = true;
 
         if (changed) {
             // generate Job to change export port options
             ChangeExport job = new ChangeExport(
                     shownExport,
                     newName,
                     newBodyOnly, newAlwaysDrawn,
                     newChar, newRefName
                     );
         }
         // possibly generate job to change export text options
         textPanel.applyChanges();
         // update dialog
         UpdateDialog job2 = new UpdateDialog();
 
         initialName = newName;
         initialBodyOnly = newBodyOnly;
         initialAlwaysDrawn = newAlwaysDrawn;
         initialCharacteristic = newChar;
         initialRefName = newRefName;
         
 	}//GEN-LAST:event_applyActionPerformed
 
 	private void okActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_okActionPerformed
 	{//GEN-HEADEREND:event_okActionPerformed
 		applyActionPerformed(evt);
 		closeDialog(null);
 	}//GEN-LAST:event_okActionPerformed
 
 	private void cancelActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_cancelActionPerformed
 	{//GEN-HEADEREND:event_cancelActionPerformed
 		closeDialog(null);
 	}//GEN-LAST:event_cancelActionPerformed
 	
 	/** Closes the dialog */
 	private void closeDialog(java.awt.event.WindowEvent evt)//GEN-FIRST:event_closeDialog
 	{
 		setVisible(false);
 		//theDialog = null;
 		//dispose();
 	}//GEN-LAST:event_closeDialog
 
     // Variables declaration - do not modify//GEN-BEGIN:variables
     private javax.swing.JCheckBox alwaysDrawn;
     private javax.swing.JButton apply;
     private javax.swing.JButton attributes;
     private javax.swing.JCheckBox bodyOnly;
     private javax.swing.JButton cancel;
     private javax.swing.JComboBox characteristics;
     private javax.swing.ButtonGroup grab;
     private javax.swing.JLabel header;
     private javax.swing.JLabel jLabel1;
     private javax.swing.JLabel jLabel10;
     private javax.swing.JPanel leftSide;
     private javax.swing.JButton ok;
     private javax.swing.JTextField refName;
     private javax.swing.ButtonGroup sizes;
     private javax.swing.JTextField theText;
     // End of variables declaration//GEN-END:variables
 	
 }
