 /*
  * To change this template, choose Tools | Templates
  * and open the template in the editor.
  */
 
 /*
  * StreamPanel.java
  *
  * Created on 4-Apr-2012, 4:07:51 PM
  */
 package webcamstudio.components;
 
 import java.awt.BorderLayout;
 import java.awt.Color;
 import java.awt.image.BufferedImage;
 import java.util.Timer;
 import java.util.TimerTask;
 import javax.swing.ImageIcon;
 import javax.swing.SpinnerNumberModel;
 import webcamstudio.streams.Stream;
 
 /**
  *
  * @author patrick
  */
 public class StreamPanel extends javax.swing.JPanel implements Stream.Listener{
 
     Stream stream = null;
     Viewer viewer = new Viewer();
     Timer timer = new Timer();
 
     /** Creates new form StreamPanel */
     public StreamPanel(Stream stream) {
 
         initComponents();
         
         viewer.setOpaque(true);
         viewer.setVisible(true);
         viewer.setBackground(Color.red);
         panPreview.add(viewer, BorderLayout.CENTER);
         this.stream = stream;
         spinX.setValue(stream.getX());
         spinY.setValue(stream.getY());
         spinW.setValue(stream.getWidth());
         spinH.setValue(stream.getHeight());
        spinOpacity.setModel(new SpinnerNumberModel(50, 0, 100, 1));
         spinOpacity.setValue(stream.getOpacity());
        spinVolume.setModel(new SpinnerNumberModel(0, 0, 100, 1));
         spinVolume.setValue(stream.getVolume() * 100);
         spinZOrder.setValue(stream.getZOrder());
         spinH1.setValue(stream.getCaptureHeight());
         spinW1.setValue(stream.getCaptureWidth());
         timer.scheduleAtFixedRate(new RefreshPanel(this), 0, 200);
         stream.setListener(this);
     }
     public Viewer detachViewer(){
         panPreview.remove(viewer);
         panPreview.revalidate();
         return viewer;
     }
     public Viewer attachViewer(){
         panPreview.add(viewer, BorderLayout.CENTER);
         panPreview.revalidate();
         return viewer;
     }
     public ImageIcon getIcon(){
         ImageIcon icon = null;
         if (stream.getPreview()!=null){
             icon = new ImageIcon(stream.getPreview().getScaledInstance(32, 32, BufferedImage.SCALE_FAST));
         }
         
         return icon;
     }
     public void remove() {
         timer.cancel();
         timer = null;
         stream.stop();
         stream = null;
 
     }
 
     @Override
     public void sourceUpdated(Stream stream){
         tglActiveStream.setSelected(stream.isPlaying());
         spinX.setValue(stream.getX());
         spinY.setValue(stream.getY());
         spinW.setValue(stream.getWidth());
         spinH.setValue(stream.getHeight());
         spinOpacity.setModel(new SpinnerNumberModel(50, 0, 100, 1));
         spinOpacity.setValue(stream.getOpacity());
         spinVolume.setModel(new SpinnerNumberModel(0, 0, 100, 1));
         spinVolume.setValue(stream.getVolume() * 100);
         spinZOrder.setValue(stream.getZOrder());
         
     }
     /** This method is called from within the constructor to
      * initialize the form.
      * WARNING: Do NOT modify this code. The content of this method is
      * always regenerated by the Form Editor.
      */
     @SuppressWarnings("unchecked")
     // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
     private void initComponents() {
 
         panPreview = new javax.swing.JPanel();
         spinX = new javax.swing.JSpinner();
         spinY = new javax.swing.JSpinner();
         spinW = new javax.swing.JSpinner();
         spinH = new javax.swing.JSpinner();
         spinOpacity = new javax.swing.JSpinner();
         spinVolume = new javax.swing.JSpinner();
         tglActiveStream = new javax.swing.JToggleButton();
         spinZOrder = new javax.swing.JSpinner();
         labelX = new javax.swing.JLabel();
         labelY = new javax.swing.JLabel();
         labelW = new javax.swing.JLabel();
         labelH = new javax.swing.JLabel();
         labelO = new javax.swing.JLabel();
         labelV = new javax.swing.JLabel();
         labelZ = new javax.swing.JLabel();
         labelCW = new javax.swing.JLabel();
         spinW1 = new javax.swing.JSpinner();
         labelCH = new javax.swing.JLabel();
         spinH1 = new javax.swing.JSpinner();
 
         setBorder(javax.swing.BorderFactory.createEtchedBorder(javax.swing.border.EtchedBorder.RAISED));
         setMaximumSize(new java.awt.Dimension(138, 350));
         setMinimumSize(new java.awt.Dimension(138, 350));
         setPreferredSize(new java.awt.Dimension(138, 350));
         setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());
 
         panPreview.setBackground(new java.awt.Color(113, 113, 113));
         panPreview.setBorder(javax.swing.BorderFactory.createEtchedBorder(javax.swing.border.EtchedBorder.RAISED));
         panPreview.setMaximumSize(new java.awt.Dimension(90, 60));
         panPreview.setMinimumSize(new java.awt.Dimension(90, 60));
         panPreview.setName("panPreview"); // NOI18N
         panPreview.setPreferredSize(new java.awt.Dimension(90, 60));
         panPreview.setLayout(new java.awt.BorderLayout());
         add(panPreview, new org.netbeans.lib.awtextra.AbsoluteConstraints(10, 10, 120, 101));
 
         spinX.setFont(new java.awt.Font("Tahoma", 0, 8)); // NOI18N
         spinX.setName("spinX"); // NOI18N
         spinX.addChangeListener(new javax.swing.event.ChangeListener() {
             public void stateChanged(javax.swing.event.ChangeEvent evt) {
                 spinXStateChanged(evt);
             }
         });
         add(spinX, new org.netbeans.lib.awtextra.AbsoluteConstraints(70, 120, 60, -1));
 
         spinY.setFont(new java.awt.Font("Tahoma", 0, 8)); // NOI18N
         spinY.setName("spinY"); // NOI18N
         spinY.addChangeListener(new javax.swing.event.ChangeListener() {
             public void stateChanged(javax.swing.event.ChangeEvent evt) {
                 spinYStateChanged(evt);
             }
         });
         add(spinY, new org.netbeans.lib.awtextra.AbsoluteConstraints(70, 140, 60, -1));
 
         spinW.setFont(new java.awt.Font("Tahoma", 0, 8)); // NOI18N
         spinW.setName("spinW"); // NOI18N
         spinW.addChangeListener(new javax.swing.event.ChangeListener() {
             public void stateChanged(javax.swing.event.ChangeEvent evt) {
                 spinWStateChanged(evt);
             }
         });
         add(spinW, new org.netbeans.lib.awtextra.AbsoluteConstraints(70, 200, 60, -1));
 
         spinH.setFont(new java.awt.Font("Tahoma", 0, 8)); // NOI18N
         spinH.setName("spinH"); // NOI18N
         spinH.addChangeListener(new javax.swing.event.ChangeListener() {
             public void stateChanged(javax.swing.event.ChangeEvent evt) {
                 spinHStateChanged(evt);
             }
         });
         add(spinH, new org.netbeans.lib.awtextra.AbsoluteConstraints(70, 220, 60, -1));
 
         spinOpacity.setFont(new java.awt.Font("Tahoma", 0, 8)); // NOI18N
         spinOpacity.setName("spinOpacity"); // NOI18N
         spinOpacity.addChangeListener(new javax.swing.event.ChangeListener() {
             public void stateChanged(javax.swing.event.ChangeEvent evt) {
                 spinOpacityStateChanged(evt);
             }
         });
         add(spinOpacity, new org.netbeans.lib.awtextra.AbsoluteConstraints(70, 240, 60, -1));
 
         spinVolume.setFont(new java.awt.Font("Tahoma", 0, 8)); // NOI18N
         spinVolume.setName("spinVolume"); // NOI18N
         spinVolume.addChangeListener(new javax.swing.event.ChangeListener() {
             public void stateChanged(javax.swing.event.ChangeEvent evt) {
                 spinVolumeStateChanged(evt);
             }
         });
         add(spinVolume, new org.netbeans.lib.awtextra.AbsoluteConstraints(70, 260, 60, -1));
 
         tglActiveStream.setIcon(new javax.swing.ImageIcon(getClass().getResource("/webcamstudio/resources/tango/media-playback-start.png"))); // NOI18N
         tglActiveStream.setName("tglActiveStream"); // NOI18N
         tglActiveStream.setRolloverEnabled(false);
         tglActiveStream.setSelectedIcon(new javax.swing.ImageIcon(getClass().getResource("/webcamstudio/resources/tango/media-playback-stop.png"))); // NOI18N
         tglActiveStream.addActionListener(new java.awt.event.ActionListener() {
             public void actionPerformed(java.awt.event.ActionEvent evt) {
                 tglActiveStreamActionPerformed(evt);
             }
         });
         add(tglActiveStream, new org.netbeans.lib.awtextra.AbsoluteConstraints(10, 310, 120, -1));
 
         spinZOrder.setFont(new java.awt.Font("Tahoma", 0, 8)); // NOI18N
         spinZOrder.setName("spinZOrder"); // NOI18N
         spinZOrder.addChangeListener(new javax.swing.event.ChangeListener() {
             public void stateChanged(javax.swing.event.ChangeEvent evt) {
                 spinZOrderStateChanged(evt);
             }
         });
         add(spinZOrder, new org.netbeans.lib.awtextra.AbsoluteConstraints(70, 280, 60, -1));
 
         labelX.setFont(new java.awt.Font("Tahoma", 0, 8)); // NOI18N
         java.util.ResourceBundle bundle = java.util.ResourceBundle.getBundle("webcamstudio/Languages"); // NOI18N
         labelX.setText(bundle.getString("X")); // NOI18N
         labelX.setName("labelX"); // NOI18N
         add(labelX, new org.netbeans.lib.awtextra.AbsoluteConstraints(10, 120, 60, -1));
 
         labelY.setFont(new java.awt.Font("Tahoma", 0, 8)); // NOI18N
         labelY.setText(bundle.getString("Y")); // NOI18N
         labelY.setName("labelY"); // NOI18N
         add(labelY, new org.netbeans.lib.awtextra.AbsoluteConstraints(10, 150, 50, -1));
 
         labelW.setFont(new java.awt.Font("Tahoma", 0, 8)); // NOI18N
         labelW.setText(bundle.getString("WIDTH")); // NOI18N
         labelW.setName("labelW"); // NOI18N
         add(labelW, new org.netbeans.lib.awtextra.AbsoluteConstraints(10, 210, 52, -1));
 
         labelH.setFont(new java.awt.Font("Tahoma", 0, 8)); // NOI18N
         labelH.setText(bundle.getString("HEIGHT")); // NOI18N
         labelH.setName("labelH"); // NOI18N
         add(labelH, new org.netbeans.lib.awtextra.AbsoluteConstraints(10, 230, 40, -1));
 
         labelO.setFont(new java.awt.Font("Tahoma", 0, 8)); // NOI18N
         labelO.setText(bundle.getString("OPACITY")); // NOI18N
         labelO.setName("labelO"); // NOI18N
         add(labelO, new org.netbeans.lib.awtextra.AbsoluteConstraints(10, 250, 40, -1));
 
         labelV.setFont(new java.awt.Font("Tahoma", 0, 8)); // NOI18N
         labelV.setText(bundle.getString("VOLUME")); // NOI18N
         labelV.setName("labelV"); // NOI18N
         add(labelV, new org.netbeans.lib.awtextra.AbsoluteConstraints(10, 270, 40, 9));
 
         labelZ.setFont(new java.awt.Font("Tahoma", 0, 8)); // NOI18N
         labelZ.setText(bundle.getString("LAYER")); // NOI18N
         labelZ.setName("labelZ"); // NOI18N
         add(labelZ, new org.netbeans.lib.awtextra.AbsoluteConstraints(10, 290, 30, -1));
 
         labelCW.setFont(new java.awt.Font("Tahoma", 0, 8)); // NOI18N
         labelCW.setText(bundle.getString("CAPTUREWIDTH")); // NOI18N
         labelCW.setName("labelCW"); // NOI18N
         add(labelCW, new org.netbeans.lib.awtextra.AbsoluteConstraints(10, 170, 50, -1));
 
         spinW1.setFont(new java.awt.Font("Tahoma", 0, 8)); // NOI18N
         spinW1.setName("spinW1"); // NOI18N
         spinW1.addChangeListener(new javax.swing.event.ChangeListener() {
             public void stateChanged(javax.swing.event.ChangeEvent evt) {
                 spinW1StateChanged(evt);
             }
         });
         add(spinW1, new org.netbeans.lib.awtextra.AbsoluteConstraints(70, 160, 60, -1));
 
         labelCH.setFont(new java.awt.Font("Tahoma", 0, 8)); // NOI18N
         labelCH.setText(bundle.getString("CAPTUREHEIGHT")); // NOI18N
         labelCH.setName("labelCH"); // NOI18N
         add(labelCH, new org.netbeans.lib.awtextra.AbsoluteConstraints(10, 190, 60, -1));
 
         spinH1.setFont(new java.awt.Font("Tahoma", 0, 8)); // NOI18N
         spinH1.setName("spinH1"); // NOI18N
         spinH1.addChangeListener(new javax.swing.event.ChangeListener() {
             public void stateChanged(javax.swing.event.ChangeEvent evt) {
                 spinH1StateChanged(evt);
             }
         });
         add(spinH1, new org.netbeans.lib.awtextra.AbsoluteConstraints(70, 180, 60, -1));
     }// </editor-fold>//GEN-END:initComponents
 
     private void tglActiveStreamActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_tglActiveStreamActionPerformed
         if (tglActiveStream.isSelected()) {
             spinW1.setEnabled(false);
             spinH1.setEnabled(false);
             stream.read();
         } else {
             spinW1.setEnabled(true);
             spinH1.setEnabled(true);
             stream.stop();
         }
     }//GEN-LAST:event_tglActiveStreamActionPerformed
 
     private void spinOpacityStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_spinOpacityStateChanged
         stream.setOpacity((Integer) spinOpacity.getValue());
     }//GEN-LAST:event_spinOpacityStateChanged
 
     private void spinZOrderStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_spinZOrderStateChanged
         stream.setZOrder((Integer) spinZOrder.getValue());
     }//GEN-LAST:event_spinZOrderStateChanged
 
     private void spinWStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_spinWStateChanged
         stream.setWidth((Integer)spinW.getValue());
     }//GEN-LAST:event_spinWStateChanged
 
     private void spinHStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_spinHStateChanged
         stream.setHeight((Integer)spinH.getValue());
     }//GEN-LAST:event_spinHStateChanged
 
     private void spinXStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_spinXStateChanged
         stream.setX((Integer)spinX.getValue());
     }//GEN-LAST:event_spinXStateChanged
 
     private void spinYStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_spinYStateChanged
         stream.setY((Integer)spinY.getValue());
     }//GEN-LAST:event_spinYStateChanged
 
     private void spinVolumeStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_spinVolumeStateChanged
         Object value = spinVolume.getValue();
         float v = 0;
         if (value instanceof Float){
             v = (Float)value;
         } else if (value instanceof Integer){
             v = ((Integer)value).floatValue();
         }
         stream.setVolume(v/100f);
         
     }//GEN-LAST:event_spinVolumeStateChanged
 
     private void spinW1StateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_spinW1StateChanged
        stream.setCaptureWidth((Integer)spinW1.getValue());
     }//GEN-LAST:event_spinW1StateChanged
 
     private void spinH1StateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_spinH1StateChanged
         stream.setCaptureHeight((Integer)spinH1.getValue());
     }//GEN-LAST:event_spinH1StateChanged
 
     // Variables declaration - do not modify//GEN-BEGIN:variables
     private javax.swing.JLabel labelCH;
     private javax.swing.JLabel labelCW;
     private javax.swing.JLabel labelH;
     private javax.swing.JLabel labelO;
     private javax.swing.JLabel labelV;
     private javax.swing.JLabel labelW;
     private javax.swing.JLabel labelX;
     private javax.swing.JLabel labelY;
     private javax.swing.JLabel labelZ;
     private javax.swing.JPanel panPreview;
     private javax.swing.JSpinner spinH;
     private javax.swing.JSpinner spinH1;
     private javax.swing.JSpinner spinOpacity;
     private javax.swing.JSpinner spinVolume;
     private javax.swing.JSpinner spinW;
     private javax.swing.JSpinner spinW1;
     private javax.swing.JSpinner spinX;
     private javax.swing.JSpinner spinY;
     private javax.swing.JSpinner spinZOrder;
     private javax.swing.JToggleButton tglActiveStream;
     // End of variables declaration//GEN-END:variables
 }
 
 class RefreshPanel extends TimerTask {
 
     StreamPanel panel = null;
 
     public RefreshPanel(StreamPanel p) {
         panel = p;
     }
 
     @Override
     public void run() {
         if (panel != null) {
             panel.viewer.setImage(panel.stream.getPreview());
             panel.viewer.setAudioLevel(panel.stream.getAudioLevelLeft(), panel.stream.getAudioLevelRight());
             panel.viewer.repaint();
         }
     }
 }
