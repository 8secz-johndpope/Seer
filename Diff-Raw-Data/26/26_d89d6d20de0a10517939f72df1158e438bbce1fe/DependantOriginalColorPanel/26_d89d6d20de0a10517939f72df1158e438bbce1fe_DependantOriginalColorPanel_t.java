 /*
 Copyright 2008-2010 Gephi
 Authors : Jeremy Subtil <jeremy.subtil@gephi.org>
 Website : http://www.gephi.org
 
 This file is part of Gephi.
 
 Gephi is free software: you can redistribute it and/or modify
 it under the terms of the GNU Affero General Public License as
 published by the Free Software Foundation, either version 3 of the
 License, or (at your option) any later version.
 
 Gephi is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU Affero General Public License for more details.
 
 You should have received a copy of the GNU Affero General Public License
 along with Gephi.  If not, see <http://www.gnu.org/licenses/>.
  */
 package org.gephi.desktop.preview.propertyeditors;
 
 import java.awt.Color;
 import java.awt.event.ItemEvent;
 import java.awt.event.ItemListener;
 import java.beans.PropertyChangeEvent;
 import java.beans.PropertyChangeListener;
 import org.gephi.preview.types.DependantOriginalColor;
 import org.gephi.ui.components.JColorButton;
 
 /**
  *
  * @author Mathieu Bastian
  */
 public class DependantOriginalColorPanel extends javax.swing.JPanel implements ItemListener {
 
     /** Creates new form DependantOriginalColorPanel */
     public DependantOriginalColorPanel() {
         initComponents();
         colorButton.addPropertyChangeListener(JColorButton.EVENT_COLOR, new PropertyChangeListener() {
 
             public void propertyChange(PropertyChangeEvent evt) {
                 Color newColor = (Color) evt.getNewValue();
                 propertyEditor.setValue(new DependantOriginalColor(newColor));
             }
         });
 
         originalRadio.addItemListener(this);
         parentRadio.addItemListener(this);
         customRadio.addItemListener(this);
     }
 
     public void itemStateChanged(ItemEvent e) {
         if (customRadio.isSelected()) {
             colorButton.setEnabled(true);
         } else {
             colorButton.setEnabled(false);
         }
         DependantOriginalColor.Mode selectedMode = null;
         if (originalRadio.isSelected()) {
             selectedMode = DependantOriginalColor.Mode.ORIGINAL;
         } else if (parentRadio.isSelected()) {
             selectedMode = DependantOriginalColor.Mode.PARENT;
         } else if (customRadio.isSelected()) {
             selectedMode = DependantOriginalColor.Mode.CUSTOM;
         }
         propertyEditor.setValue(new DependantOriginalColor(selectedMode));
     }
     private DependantOriginalColorPropertyEditor propertyEditor;
 
     public void setup(DependantOriginalColorPropertyEditor propertyEditor) {
         this.propertyEditor = propertyEditor;
         DependantOriginalColor dependantOriginalColor = (DependantOriginalColor) propertyEditor.getValue();
         if (dependantOriginalColor.getMode().equals(DependantOriginalColor.Mode.CUSTOM)) {
             customRadio.setSelected(true);
             ((JColorButton) colorButton).setColor(dependantOriginalColor.getCustomColor());
         } else if (dependantOriginalColor.getMode().equals(DependantOriginalColor.Mode.ORIGINAL)) {
             originalRadio.setSelected(true);
         } else if (dependantOriginalColor.getMode().equals(DependantOriginalColor.Mode.PARENT)) {
             parentRadio.setSelected(true);
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
 
         buttonGroup1 = new javax.swing.ButtonGroup();
         jXHeader1 = new org.jdesktop.swingx.JXHeader();
         colorButton = new JColorButton(Color.BLACK);
         customRadio = new javax.swing.JRadioButton();
         originalRadio = new javax.swing.JRadioButton();
         parentRadio = new javax.swing.JRadioButton();
 
         jXHeader1.setDescription(org.openide.util.NbBundle.getMessage(DependantOriginalColorPanel.class, "DependantOriginalColorPanel.jXHeader1.description")); // NOI18N
         jXHeader1.setTitle(org.openide.util.NbBundle.getMessage(DependantOriginalColorPanel.class, "DependantOriginalColorPanel.jXHeader1.title")); // NOI18N
 
         buttonGroup1.add(customRadio);
         customRadio.setText(org.openide.util.NbBundle.getMessage(DependantOriginalColorPanel.class, "DependantOriginalColorPanel.customRadio.text")); // NOI18N
 
         buttonGroup1.add(originalRadio);
         originalRadio.setText(org.openide.util.NbBundle.getMessage(DependantOriginalColorPanel.class, "DependantOriginalColorPanel.originalRadio.text")); // NOI18N
 
         buttonGroup1.add(parentRadio);
         parentRadio.setText(org.openide.util.NbBundle.getMessage(DependantOriginalColorPanel.class, "DependantOriginalColorPanel.parentRadio.text")); // NOI18N
 
         javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
         this.setLayout(layout);
         layout.setHorizontalGroup(
             layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
             .addGroup(layout.createSequentialGroup()
                 .addContainerGap()
                 .addComponent(originalRadio)
                .addContainerGap(360, Short.MAX_VALUE))
             .addGroup(layout.createSequentialGroup()
                 .addContainerGap()
                 .addComponent(parentRadio)
                .addContainerGap(365, Short.MAX_VALUE))
            .addComponent(jXHeader1, javax.swing.GroupLayout.DEFAULT_SIZE, 435, Short.MAX_VALUE)
             .addGroup(layout.createSequentialGroup()
                 .addContainerGap()
                 .addComponent(customRadio)
                 .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                 .addComponent(colorButton)
                .addContainerGap(326, Short.MAX_VALUE))
         );
         layout.setVerticalGroup(
             layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
             .addGroup(layout.createSequentialGroup()
                 .addComponent(jXHeader1, javax.swing.GroupLayout.PREFERRED_SIZE, 77, javax.swing.GroupLayout.PREFERRED_SIZE)
                 .addGap(18, 18, 18)
                 .addComponent(originalRadio)
                 .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                 .addComponent(parentRadio)
                 .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                 .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                     .addComponent(customRadio)
                     .addComponent(colorButton))
                 .addGap(47, 47, 47))
         );
     }// </editor-fold>//GEN-END:initComponents
     // Variables declaration - do not modify//GEN-BEGIN:variables
     private javax.swing.ButtonGroup buttonGroup1;
     private javax.swing.JButton colorButton;
     private javax.swing.JRadioButton customRadio;
     private org.jdesktop.swingx.JXHeader jXHeader1;
     private javax.swing.JRadioButton originalRadio;
     private javax.swing.JRadioButton parentRadio;
     // End of variables declaration//GEN-END:variables
 }
