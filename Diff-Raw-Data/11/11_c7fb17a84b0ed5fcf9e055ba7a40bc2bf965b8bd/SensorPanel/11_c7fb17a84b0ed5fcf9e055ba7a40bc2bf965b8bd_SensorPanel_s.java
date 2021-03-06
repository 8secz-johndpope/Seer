 /**
  * Copyright (C) 2013 SINTEF <franck.fleurey@sintef.no>
  *
  * Licensed under the GNU LESSER GENERAL PUBLIC LICENSE, Version 3, 29 June 2007;
  * you may not use this file except in compliance with the License.
  * You may obtain a copy of the License at
  *
  * 	http://www.gnu.org/licenses/lgpl-3.0.txt
  *
  * Unless required by applicable law or agreed to in writing, software
  * distributed under the License is distributed on an "AS IS" BASIS,
  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  * See the License for the specific language governing permissions and
  * limitations under the License.
  */
 package org.thingml.sensgui.desktop;
 
 import java.io.File;
 import java.util.logging.Level;
 import java.util.logging.Logger;
 import javax.swing.ImageIcon;
 import org.thingml.sensgui.adapters.SensGUI;
 import org.thingml.sensgui.adapters.SensGUIAdapter;
 import org.thingml.traale.desktop.TraaleFrame;
 
 public class SensorPanel extends javax.swing.JPanel implements SensGUI {
 
     public static ImageIcon icon_red = new ImageIcon(SensorPanel.class.getResource("/red16.png"));
     public static ImageIcon icon_green = new ImageIcon(SensorPanel.class.getResource("/green16.png"));
     public static ImageIcon icon_yellow = new ImageIcon(SensorPanel.class.getResource("/yellow16.png"));
     
     protected SensGUIAdapter sensor;
 
     public SensGUIAdapter getSensor() {
         return sensor;
     }
     
     public boolean includeInLog() {
         return (sensor != null && sensor.isConnected() && jCheckBoxLog.isSelected());
     }
     
     public void startLogging(File base_folder) {
         if (!includeInLog()) return;
         String sName = sensor.getSensorName().replace(" ", "_").trim();
         File folder = new File(base_folder, sName);
         
         // To avoid overwriting an exiting folder (in case several sensors have the same names)
         int i=1;
         while (folder.exists()) {
             folder = new File(base_folder, sName + "-" + i);
             i++;
         }
 
         folder.mkdir();
         sensor.startLogging(folder);
         jCheckBoxLog.setEnabled(false);
     }
     
      public void stopLogging() {
         if (sensor != null) sensor.stopLogging();
         jCheckBoxLog.setEnabled(true);
     }
     
     public SensorPanel(SensGUIAdapter sensor) {
         this.sensor = sensor;
         initComponents();
         jLabelSensorName.setText(sensor.getSensorName());
         jLabelIcon.setIcon(sensor.getIcon());
         jProgressBarBW.setMaximum(sensor.getMaxBandwidth());
         new BitRateCounter().start();
     }
 
     public void refresh() {
         if (sensor == null || !sensor.isConnected()) {
             jButtonDisconnect.setEnabled(false);
             jButtonGUI.setEnabled(false);
             jLabelActivity.setIcon(icon_red);
         }
         else {
             jLabelSensorName.setText(sensor.getSensorName());
             jLabelIcon.setIcon(sensor.getIcon());
             jLabelActivity.setIcon(icon_green);
         }
     }
     
     /**
      * This method is called from within the constructor to initialize the form.
      * WARNING: Do NOT modify this code. The content of this method is always
      * regenerated by the Form Editor.
      */
     @SuppressWarnings("unchecked")
     // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
     private void initComponents() {
 
         jButtonDisconnect = new javax.swing.JButton();
         jLabelSensorName = new javax.swing.JLabel();
         jButtonGUI = new javax.swing.JButton();
         jLabelIcon = new javax.swing.JLabel();
         jCheckBoxLog = new javax.swing.JCheckBox();
         jLabel1 = new javax.swing.JLabel();
         jProgressBarBW = new javax.swing.JProgressBar();
         jLabel2 = new javax.swing.JLabel();
         jLabelPing = new javax.swing.JLabel();
         jLabelActivity = new javax.swing.JLabel();
         jLabel3 = new javax.swing.JLabel();
 
         setBorder(javax.swing.BorderFactory.createBevelBorder(javax.swing.border.BevelBorder.RAISED));
         setMaximumSize(new java.awt.Dimension(32767, 68));
         setMinimumSize(new java.awt.Dimension(600, 68));
         setPreferredSize(new java.awt.Dimension(600, 68));
 
         jButtonDisconnect.setText("Disconnect");
         jButtonDisconnect.setPreferredSize(new java.awt.Dimension(98, 20));
         jButtonDisconnect.addActionListener(new java.awt.event.ActionListener() {
             public void actionPerformed(java.awt.event.ActionEvent evt) {
                 jButtonDisconnectActionPerformed(evt);
             }
         });
 
         jLabelSensorName.setFont(new java.awt.Font("Dialog", 1, 14)); // NOI18N
         jLabelSensorName.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
         jLabelSensorName.setText("Sensor Name");
 
        jButtonGUI.setText("Configure...");
         jButtonGUI.setActionCommand("");
         jButtonGUI.setPreferredSize(new java.awt.Dimension(95, 20));
         jButtonGUI.addActionListener(new java.awt.event.ActionListener() {
             public void actionPerformed(java.awt.event.ActionEvent evt) {
                 jButtonGUIActionPerformed(evt);
             }
         });
 
         jLabelIcon.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
         jLabelIcon.setIcon(new javax.swing.ImageIcon(getClass().getResource("/unknown48.png"))); // NOI18N
         jLabelIcon.setToolTipText("Click to identify sensor");
         jLabelIcon.setBorder(javax.swing.BorderFactory.createEtchedBorder());
         jLabelIcon.setMaximumSize(new java.awt.Dimension(64, 64));
         jLabelIcon.setMinimumSize(new java.awt.Dimension(64, 64));
         jLabelIcon.setPreferredSize(new java.awt.Dimension(64, 64));
         jLabelIcon.addMouseListener(new java.awt.event.MouseAdapter() {
             public void mouseClicked(java.awt.event.MouseEvent evt) {
                 jLabelIconMouseClicked(evt);
             }
         });
 
         jCheckBoxLog.setSelected(true);
         jCheckBoxLog.setText("Include in log");
         jCheckBoxLog.setActionCommand("Log");
         jCheckBoxLog.addActionListener(new java.awt.event.ActionListener() {
             public void actionPerformed(java.awt.event.ActionEvent evt) {
                 jCheckBoxLogActionPerformed(evt);
             }
         });
 
         jLabel1.setText("  Bandwidth :");
 
         jProgressBarBW.setMaximum(11000);
         jProgressBarBW.setMaximumSize(new java.awt.Dimension(32767, 16));
         jProgressBarBW.setMinimumSize(new java.awt.Dimension(10, 16));
         jProgressBarBW.setPreferredSize(new java.awt.Dimension(148, 16));
         jProgressBarBW.setString("0 B/s");
         jProgressBarBW.setStringPainted(true);
 
         jLabel2.setText("Ping :");
 
         jLabelPing.setText("??? ms");
 
         jLabelActivity.setIcon(new javax.swing.ImageIcon(getClass().getResource("/red16.png"))); // NOI18N
 
         jLabel3.setText("Activity :");
 
         javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
         this.setLayout(layout);
         layout.setHorizontalGroup(
             layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
             .addGroup(layout.createSequentialGroup()
                 .addComponent(jLabelIcon, javax.swing.GroupLayout.PREFERRED_SIZE, 64, javax.swing.GroupLayout.PREFERRED_SIZE)
                 .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                 .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                     .addGroup(layout.createSequentialGroup()
                         .addComponent(jLabel1)
                         .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                         .addComponent(jProgressBarBW, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                         .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                         .addComponent(jLabel2)
                         .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                         .addComponent(jLabelPing, javax.swing.GroupLayout.PREFERRED_SIZE, 80, javax.swing.GroupLayout.PREFERRED_SIZE)
                         .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 84, Short.MAX_VALUE)
                         .addComponent(jLabel3)
                         .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                         .addComponent(jLabelActivity))
                     .addGroup(layout.createSequentialGroup()
                         .addComponent(jLabelSensorName, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                         .addGap(14, 14, 14)
                         .addComponent(jCheckBoxLog)
                         .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                         .addComponent(jButtonDisconnect, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                         .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                         .addComponent(jButtonGUI, javax.swing.GroupLayout.PREFERRED_SIZE, 107, javax.swing.GroupLayout.PREFERRED_SIZE)))
                 .addGap(6, 6, 6))
         );
         layout.setVerticalGroup(
             layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
             .addGroup(layout.createSequentialGroup()
                 .addComponent(jLabelIcon, javax.swing.GroupLayout.PREFERRED_SIZE, 64, javax.swing.GroupLayout.PREFERRED_SIZE)
                 .addGap(0, 0, Short.MAX_VALUE))
             .addGroup(layout.createSequentialGroup()
                 .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                     .addComponent(jLabelSensorName, javax.swing.GroupLayout.PREFERRED_SIZE, 26, javax.swing.GroupLayout.PREFERRED_SIZE)
                     .addComponent(jCheckBoxLog)
                     .addComponent(jButtonDisconnect, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                     .addComponent(jButtonGUI, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                 .addGap(0, 0, Short.MAX_VALUE)
                 .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                     .addComponent(jLabel1, javax.swing.GroupLayout.Alignment.TRAILING)
                     .addComponent(jProgressBarBW, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                     .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                         .addComponent(jLabelActivity, javax.swing.GroupLayout.Alignment.TRAILING)
                         .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                             .addComponent(jLabel2)
                             .addComponent(jLabelPing)
                             .addComponent(jLabel3))))
                 .addContainerGap())
         );
     }// </editor-fold>//GEN-END:initComponents
 
     private void jButtonGUIActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonGUIActionPerformed
         sensor.showgui();
     }//GEN-LAST:event_jButtonGUIActionPerformed
 
     boolean disconneting = false;
     
     public void do_disconnect() {
         disconneting = true;
         if (sensor != null && sensor.isConnected()) sensor.disconnect();
         jButtonDisconnect.setEnabled(false);
         jButtonGUI.setEnabled(false);
         jCheckBoxLog.setSelected(false);
         jCheckBoxLog.setEnabled(false);
         jLabelActivity.setIcon(icon_red);
         sensor = null;
     }
     
     private void jButtonDisconnectActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonDisconnectActionPerformed
         do_disconnect();
     }//GEN-LAST:event_jButtonDisconnectActionPerformed
 
     private void jCheckBoxLogActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckBoxLogActionPerformed
         // TODO add your handling code here:
     }//GEN-LAST:event_jCheckBoxLogActionPerformed
 
     private void jLabelIconMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jLabelIconMouseClicked
         if (sensor != null && sensor.isConnected()) sensor.identify();
     }//GEN-LAST:event_jLabelIconMouseClicked
 
     // Variables declaration - do not modify//GEN-BEGIN:variables
     private javax.swing.JButton jButtonDisconnect;
     private javax.swing.JButton jButtonGUI;
     private javax.swing.JCheckBox jCheckBoxLog;
     private javax.swing.JLabel jLabel1;
     private javax.swing.JLabel jLabel2;
     private javax.swing.JLabel jLabel3;
     private javax.swing.JLabel jLabelActivity;
     private javax.swing.JLabel jLabelIcon;
     private javax.swing.JLabel jLabelPing;
     private javax.swing.JLabel jLabelSensorName;
     private javax.swing.JProgressBar jProgressBarBW;
     // End of variables declaration//GEN-END:variables
 
     @Override
     public void refreshSensorView() {
         refresh();
     }
 
     @Override
     public void setPing(int value) {
         jLabelPing.setText(value + " ms");
     }
 
     public void setBandwidth(int bandwidth) {
         jProgressBarBW.setValue(bandwidth);
         jProgressBarBW.setString(bandwidth + " B/s");
     }
 
     boolean act_toggle = true;
     
     @Override
     public void activity() {
         if (disconneting) return;
         if (act_toggle) {
             jLabelActivity.setIcon(icon_green);
         }
         else {
             jLabelActivity.setIcon(icon_yellow);
         }
         act_toggle = !act_toggle;
     }
     
     class BitRateCounter extends Thread {
     
         private int update_rate = 2000; // 1000 ms
 
         private boolean stop = false;
         
         public void request_stop() {
             stop = true;
         }
 
         public void run() {
 
            long old_time = System.currentTimeMillis();
            long old_bytes = sensor.getReceivedByteCount();
 
            while (sensor != null && !stop) {
                 try {
                     Thread.sleep(update_rate);
                 
                     long new_time = System.currentTimeMillis();
                     long new_bytes = sensor.getReceivedByteCount();
 
                     int bitrate = (int)(((new_bytes - old_bytes) * 1000) / (new_time - old_time));
 
                     setBandwidth(bitrate);
 
                     old_time = new_time;
                     old_bytes = new_bytes;
                 } catch (Exception ex) {
                     ex.printStackTrace();
                 }
            }
         }
     }
     
 }
