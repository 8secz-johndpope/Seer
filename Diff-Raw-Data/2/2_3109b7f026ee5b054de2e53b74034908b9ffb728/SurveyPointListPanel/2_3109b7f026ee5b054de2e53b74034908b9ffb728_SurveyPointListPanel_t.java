 /**
  * ******************************************************************************************
  * Copyright (C) 2012 - Food and Agriculture Organization of the United Nations (FAO).
  * All rights reserved.
  *
  * Redistribution and use in source and binary forms, with or without modification,
  * are permitted provided that the following conditions are met:
  *
  *    1. Redistributions of source code must retain the above copyright notice,this list
  *       of conditions and the following disclaimer.
  *    2. Redistributions in binary form must reproduce the above copyright notice,this list
  *       of conditions and the following disclaimer in the documentation and/or other
  *       materials provided with the distribution.
  *    3. Neither the name of FAO nor the names of its contributors may be used to endorse or
  *       promote products derived from this software without specific prior written permission.
  *
  * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY
  * EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
  * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT
  * SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
  * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,PROCUREMENT
  * OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
  * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT,STRICT LIABILITY,OR TORT
  * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE,
  * EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
  * *********************************************************************************************
  */
 /*
  * To change this template, choose Tools | Templates
  * and open the template in the editor.
  */
 package org.sola.clients.swing.gis.ui.control;
 
 import java.beans.PropertyChangeEvent;
 import java.beans.PropertyChangeListener;
 import javax.swing.event.ListSelectionEvent;
 import javax.swing.event.ListSelectionListener;
 import org.sola.clients.swing.gis.Messaging;
 import org.sola.clients.swing.gis.beans.AbstractListSpatialBean;
 import org.sola.clients.swing.gis.beans.SpatialBean;
 import org.sola.clients.swing.gis.beans.SurveyPointBean;
 import org.sola.clients.swing.gis.beans.SurveyPointListBean;
 import org.sola.clients.swing.gis.data.PojoDataAccess;
 import org.sola.common.messaging.GisMessage;
 
 /**
  * A User Interface component that handles the management of the survey points.
  *
  * @author Elton Manoku
  */
 public class SurveyPointListPanel extends javax.swing.JPanel {
 
     private SurveyPointListBean theBean;
 
     /**
      * This constructor must be used to initialize the bean.
      * 
      * @param listBean 
      */
     public SurveyPointListPanel(SurveyPointListBean listBean) {
         this.theBean = listBean;
         initComponents();
         this.txtAcceptableShift.setText(
                 optionRural.isSelected()
                 ? this.getAcceptanceShift(true).toString()
                 : this.getAcceptanceShift(false).toString());
         
         // Add a listner to the bean property of selected bean
         theBean.addPropertyChangeListener(new PropertyChangeListener() {
             @Override
             public void propertyChange(PropertyChangeEvent evt) {
                 if (evt.getPropertyName().equals(AbstractListSpatialBean.SELECTED_BEAN_PROPERTY)) {
                     customizeButtons((SpatialBean) evt.getNewValue());
                 }
             }
         });
     }
 
     /**
      * It changes the availability of buttons based in the selected bean
      * @param selectedSource 
      */
     private void customizeButtons(SpatialBean selectedSource) {
         cmdRemove.setEnabled(selectedSource != null);
     }
 
     /**
      * This constructor is only for the designer.
      */
     public SurveyPointListPanel() {
         initComponents();
     }
 
     /**
      * It creates the bean. It is called from the generated code.
      * @return 
      */
     private SurveyPointListBean createBean() {
         if (this.theBean == null) {
             return new SurveyPointListBean();
         }
         return this.theBean;
     }
 
     /**
      * Gets the accepted shift for survey points shifts from their original position
      *
      * @param forRuralArea
      * @return
      */
     private Double getAcceptanceShift(boolean forRuralArea) {
         if (forRuralArea) {
             return PojoDataAccess.getInstance().getMapDefinition().getSurveyPointShiftRuralArea();
         }
         return PojoDataAccess.getInstance().getMapDefinition().getSurveyPointShiftUrbanArea();
     }
 
     /**
      * This method is called from within the constructor to initialize the form. WARNING: Do NOT
      * modify this code. The content of this method is always regenerated by the Form Editor.
      */
     @SuppressWarnings("unchecked")
     // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
     private void initComponents() {
         bindingGroup = new org.jdesktop.beansbinding.BindingGroup();
 
         surveyPointListBean = createBean();
         urbanRural = new javax.swing.ButtonGroup();
         optionRural = new javax.swing.JRadioButton();
         txtY = new javax.swing.JTextField();
         optionUrban = new javax.swing.JRadioButton();
         cmdAdd = new javax.swing.JButton();
         cmdRemove = new javax.swing.JButton();
         txtAcceptableShift = new javax.swing.JTextField();
         txtStandardDeviation = new javax.swing.JTextField();
         txtMeanShift = new javax.swing.JTextField();
         txtX = new javax.swing.JTextField();
         jScrollPane1 = new javax.swing.JScrollPane();
         tablePointList = new javax.swing.JTable();
         jLabel1 = new javax.swing.JLabel();
         jLabel2 = new javax.swing.JLabel();
         jLabel3 = new javax.swing.JLabel();
         jLabel4 = new javax.swing.JLabel();
         jLabel5 = new javax.swing.JLabel();
 
         urbanRural.add(optionRural);
         optionRural.setSelected(true);
         java.util.ResourceBundle bundle = java.util.ResourceBundle.getBundle("org/sola/clients/swing/gis/ui/control/Bundle"); // NOI18N
         optionRural.setText(bundle.getString("SurveyPointListPanel.optionRural.text")); // NOI18N
         optionRural.addActionListener(new java.awt.event.ActionListener() {
             public void actionPerformed(java.awt.event.ActionEvent evt) {
                 optionRuralActionPerformed(evt);
             }
         });
 
         urbanRural.add(optionUrban);
         optionUrban.setText(bundle.getString("SurveyPointListPanel.optionUrban.text")); // NOI18N
         optionUrban.addActionListener(new java.awt.event.ActionListener() {
             public void actionPerformed(java.awt.event.ActionEvent evt) {
                 optionUrbanActionPerformed(evt);
             }
         });
 
         cmdAdd.setText(bundle.getString("SurveyPointListPanel.cmdAdd.text")); // NOI18N
         cmdAdd.addActionListener(new java.awt.event.ActionListener() {
             public void actionPerformed(java.awt.event.ActionEvent evt) {
                 cmdAddActionPerformed(evt);
             }
         });
 
         cmdRemove.setText(bundle.getString("SurveyPointListPanel.cmdRemove.text")); // NOI18N
         cmdRemove.setEnabled(false);
         cmdRemove.addActionListener(new java.awt.event.ActionListener() {
             public void actionPerformed(java.awt.event.ActionEvent evt) {
                 cmdRemoveActionPerformed(evt);
             }
         });
 
         txtAcceptableShift.setEditable(false);
 
         txtStandardDeviation.setEditable(false);
 
         org.jdesktop.beansbinding.Binding binding = org.jdesktop.beansbinding.Bindings.createAutoBinding(org.jdesktop.beansbinding.AutoBinding.UpdateStrategy.READ_WRITE, surveyPointListBean, org.jdesktop.beansbinding.ELProperty.create("${standardDeviation}"), txtStandardDeviation, org.jdesktop.beansbinding.BeanProperty.create("text"));
         bindingGroup.addBinding(binding);
 
         txtMeanShift.setEditable(false);
 
         binding = org.jdesktop.beansbinding.Bindings.createAutoBinding(org.jdesktop.beansbinding.AutoBinding.UpdateStrategy.READ_WRITE, surveyPointListBean, org.jdesktop.beansbinding.ELProperty.create("${mean}"), txtMeanShift, org.jdesktop.beansbinding.BeanProperty.create("text"));
         bindingGroup.addBinding(binding);
 
         org.jdesktop.beansbinding.ELProperty eLProperty = org.jdesktop.beansbinding.ELProperty.create("${beanList}");
         org.jdesktop.swingbinding.JTableBinding jTableBinding = org.jdesktop.swingbinding.SwingBindings.createJTableBinding(org.jdesktop.beansbinding.AutoBinding.UpdateStrategy.READ_WRITE, surveyPointListBean, eLProperty, tablePointList);
         org.jdesktop.swingbinding.JTableBinding.ColumnBinding columnBinding = jTableBinding.addColumnBinding(org.jdesktop.beansbinding.ELProperty.create("${id}"));
         columnBinding.setColumnName("Id");
         columnBinding.setColumnClass(String.class);
         columnBinding.setEditable(false);
         columnBinding = jTableBinding.addColumnBinding(org.jdesktop.beansbinding.ELProperty.create("${x}"));
         columnBinding.setColumnName("X");
         columnBinding.setColumnClass(Double.class);
         columnBinding.setEditable(false);
         columnBinding = jTableBinding.addColumnBinding(org.jdesktop.beansbinding.ELProperty.create("${y}"));
         columnBinding.setColumnName("Y");
         columnBinding.setColumnClass(Double.class);
         columnBinding.setEditable(false);
         columnBinding = jTableBinding.addColumnBinding(org.jdesktop.beansbinding.ELProperty.create("${boundary}"));
         columnBinding.setColumnName("Boundary");
         columnBinding.setColumnClass(Boolean.class);
         columnBinding = jTableBinding.addColumnBinding(org.jdesktop.beansbinding.ELProperty.create("${linked}"));
         columnBinding.setColumnName("Linked");
         columnBinding.setColumnClass(Boolean.class);
         columnBinding.setEditable(false);
         columnBinding = jTableBinding.addColumnBinding(org.jdesktop.beansbinding.ELProperty.create("${shiftDistance}"));
         columnBinding.setColumnName("Shift Distance");
         columnBinding.setColumnClass(Double.class);
         columnBinding.setEditable(false);
         bindingGroup.addBinding(jTableBinding);
         jTableBinding.bind();binding = org.jdesktop.beansbinding.Bindings.createAutoBinding(org.jdesktop.beansbinding.AutoBinding.UpdateStrategy.READ_WRITE, surveyPointListBean, org.jdesktop.beansbinding.ELProperty.create("${selectedBean}"), tablePointList, org.jdesktop.beansbinding.BeanProperty.create("selectedElement"));
         bindingGroup.addBinding(binding);
 
         jScrollPane1.setViewportView(tablePointList);
         tablePointList.getColumnModel().getColumn(0).setPreferredWidth(5);
         tablePointList.getColumnModel().getColumn(0).setHeaderValue(bundle.getString("SurveyPointListPanel.tablePointList.columnModel.title0")); // NOI18N
         tablePointList.getColumnModel().getColumn(1).setPreferredWidth(20);
         tablePointList.getColumnModel().getColumn(1).setHeaderValue(bundle.getString("SurveyPointListPanel.tablePointList.columnModel.title1")); // NOI18N
         tablePointList.getColumnModel().getColumn(2).setPreferredWidth(20);
         tablePointList.getColumnModel().getColumn(2).setHeaderValue(bundle.getString("SurveyPointListPanel.tablePointList.columnModel.title2")); // NOI18N
         tablePointList.getColumnModel().getColumn(3).setPreferredWidth(20);
         tablePointList.getColumnModel().getColumn(3).setHeaderValue(bundle.getString("SurveyPointListPanel.tablePointList.columnModel.title3")); // NOI18N
         tablePointList.getColumnModel().getColumn(4).setPreferredWidth(20);
         tablePointList.getColumnModel().getColumn(4).setHeaderValue(bundle.getString("SurveyPointListPanel.tablePointList.columnModel.title4")); // NOI18N
         tablePointList.getColumnModel().getColumn(5).setHeaderValue(bundle.getString("SurveyPointListPanel.tablePointList.columnModel.title5")); // NOI18N
 
         jLabel1.setText(bundle.getString("SurveyPointListPanel.jLabel1.text")); // NOI18N
 
         jLabel2.setText(bundle.getString("SurveyPointListPanel.jLabel2.text")); // NOI18N
 
         jLabel3.setText(bundle.getString("SurveyPointListPanel.jLabel3.text")); // NOI18N
 
         jLabel4.setText(bundle.getString("SurveyPointListPanel.jLabel4.text")); // NOI18N
 
         jLabel5.setText(bundle.getString("SurveyPointListPanel.jLabel5.text")); // NOI18N
 
         javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
         this.setLayout(layout);
         layout.setHorizontalGroup(
             layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
             .addGroup(layout.createSequentialGroup()
                 .addContainerGap()
                 .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                     .addGroup(layout.createSequentialGroup()
                         .addComponent(jLabel1)
                         .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                         .addComponent(txtMeanShift, javax.swing.GroupLayout.PREFERRED_SIZE, 57, javax.swing.GroupLayout.PREFERRED_SIZE)
                         .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                         .addComponent(jLabel2)
                         .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                         .addComponent(txtStandardDeviation, javax.swing.GroupLayout.PREFERRED_SIZE, 53, javax.swing.GroupLayout.PREFERRED_SIZE)
                         .addGap(18, 18, 18)
                         .addComponent(optionUrban, javax.swing.GroupLayout.PREFERRED_SIZE, 68, javax.swing.GroupLayout.PREFERRED_SIZE)
                         .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                         .addComponent(optionRural)
                         .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                         .addComponent(jLabel5)
                         .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                         .addComponent(txtAcceptableShift, javax.swing.GroupLayout.PREFERRED_SIZE, 53, javax.swing.GroupLayout.PREFERRED_SIZE))
                     .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 564, Short.MAX_VALUE)
                     .addGroup(layout.createSequentialGroup()
                         .addGap(0, 0, Short.MAX_VALUE)
                         .addComponent(jLabel3)
                         .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                         .addComponent(txtX, javax.swing.GroupLayout.PREFERRED_SIZE, 96, javax.swing.GroupLayout.PREFERRED_SIZE)
                         .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                         .addComponent(jLabel4)
                         .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                         .addComponent(txtY, javax.swing.GroupLayout.PREFERRED_SIZE, 94, javax.swing.GroupLayout.PREFERRED_SIZE)
                         .addGap(18, 18, 18)
                         .addComponent(cmdAdd)
                         .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                         .addComponent(cmdRemove)))
                 .addContainerGap())
         );
         layout.setVerticalGroup(
             layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
             .addGroup(layout.createSequentialGroup()
                 .addContainerGap()
                 .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                     .addComponent(txtMeanShift, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                     .addComponent(txtStandardDeviation, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                     .addComponent(optionUrban)
                     .addComponent(optionRural)
                     .addComponent(txtAcceptableShift, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                     .addComponent(jLabel1)
                     .addComponent(jLabel2)
                     .addComponent(jLabel5))
                 .addGap(18, 18, 18)
                 .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                     .addComponent(txtY, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                     .addComponent(txtX, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                     .addComponent(cmdAdd)
                     .addComponent(cmdRemove)
                     .addComponent(jLabel3)
                     .addComponent(jLabel4))
                 .addGap(6, 6, 6)
                 .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 280, Short.MAX_VALUE)
                 .addContainerGap())
         );
 
         bindingGroup.bind();
     }// </editor-fold>//GEN-END:initComponents
 
     private void optionRuralActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_optionRuralActionPerformed
         this.txtAcceptableShift.setText(this.getAcceptanceShift(true).toString());
     }//GEN-LAST:event_optionRuralActionPerformed
 
     private void optionUrbanActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_optionUrbanActionPerformed
         this.txtAcceptableShift.setText(this.getAcceptanceShift(false).toString());
     }//GEN-LAST:event_optionUrbanActionPerformed
 
     private void cmdAddActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cmdAddActionPerformed
         try {
             Double x = Double.valueOf(this.txtX.getText());
             Double y = Double.valueOf(this.txtY.getText());
             SurveyPointBean bean = new SurveyPointBean();
             bean.setX(x);
             bean.setY(y);
            // Ticket 98  - Set the id for the manually entered coordinate
            bean.setId(Integer.toString(theBean.getBeanList().size() + 1));
             this.theBean.getBeanList().add(bean);
         } catch (NumberFormatException ex) {
             Messaging.getInstance().show(GisMessage.CADASTRE_SURVEY_ADD_POINT);
         }
     }//GEN-LAST:event_cmdAddActionPerformed
 
     private void cmdRemoveActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cmdRemoveActionPerformed
         if (theBean.getSelectedBean() != null) {
             theBean.getBeanList().remove((SurveyPointBean)theBean.getSelectedBean());
             theBean.setSelectedBean(null);
         }
         
     }//GEN-LAST:event_cmdRemoveActionPerformed
 
     // Variables declaration - do not modify//GEN-BEGIN:variables
     private javax.swing.JButton cmdAdd;
     private javax.swing.JButton cmdRemove;
     private javax.swing.JLabel jLabel1;
     private javax.swing.JLabel jLabel2;
     private javax.swing.JLabel jLabel3;
     private javax.swing.JLabel jLabel4;
     private javax.swing.JLabel jLabel5;
     private javax.swing.JScrollPane jScrollPane1;
     private javax.swing.JRadioButton optionRural;
     private javax.swing.JRadioButton optionUrban;
     private org.sola.clients.swing.gis.beans.SurveyPointListBean surveyPointListBean;
     private javax.swing.JTable tablePointList;
     private javax.swing.JTextField txtAcceptableShift;
     private javax.swing.JTextField txtMeanShift;
     private javax.swing.JTextField txtStandardDeviation;
     private javax.swing.JTextField txtX;
     private javax.swing.JTextField txtY;
     private javax.swing.ButtonGroup urbanRural;
     private org.jdesktop.beansbinding.BindingGroup bindingGroup;
     // End of variables declaration//GEN-END:variables
 }
