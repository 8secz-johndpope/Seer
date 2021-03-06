 /*
  * To change this template, choose Tools | Templates
  * and open the template in the editor.
  */
 package littlesmarttool2.GUI;
 
 import java.io.IOException;
 import java.util.concurrent.TimeoutException;
 import javax.swing.JOptionPane;
 import javax.swing.Timer;
 import littlesmarttool2.GUI.components.DotsListener;
 import littlesmarttool2.comm.SerialController;
 import littlesmarttool2.model.CameraModel;
 import littlesmarttool2.model.IRCommand;
 import littlesmarttool2.model.ModelUtil;
 import littlesmarttool2.util.PulseDataRecorder;
 
 /**
  *
  * @author Rasmus
  */
 public class IRRecordDialog extends javax.swing.JDialog {
 
     private final CameraModel model;
     private final PulseDataRecorder recorder;
     private final SerialController controller;
     private int[] pulseData;
     private Timer statusTimer = new Timer(1000,null);
     private boolean editing = false;
     private IRCommand editingCommand;
    private IRCommand result = null;
         
     /**
      * Creates new form IRRecordDialog
      */
     public IRRecordDialog(java.awt.Frame parent, CameraModel model, SerialController controller) {
         super(parent, true);
         initComponents();
         this.model = model;
         this.controller = controller;
         this.recorder = new PulseDataRecorder(controller);
         modelLabel.setText(model.getIdentifier());
     }
     
     public IRRecordDialog(java.awt.Frame parent, CameraModel model, SerialController controller, IRCommand editCommand)
     {
         super(parent, true);
         initComponents();
         this.model = model;
         this.controller = controller;
         this.recorder = new PulseDataRecorder(controller);
         modelLabel.setText(model.getIdentifier());
         
         editing = true;
         nameField.setText(editCommand.getName());
         descriptionArea.setText(editCommand.getDescription());
         titleLabel.setText("Edit IR command");
         recordButton.setEnabled(false);
         recordButton.setVisible(false);
         playButton.setVisible(false);
         statusLabel.setText("Cannot change recording");
         irSequenceLabel.setVisible(false);
         editingCommand = editCommand;
         okButton.setEnabled(true);
         
         //TODO: Enable play button
     }
     
    public IRCommand getResult()
    {
        return result;
    }
    
    private IRCommand generateIRCommand()
     {
         return new IRCommand(nameField.getText(), descriptionArea.getText(), new CameraModel[]{model}, pulseData, 10000, 1, 38, true);
     }
     
      private void doRecord()
     {
         boolean error = false;
         try {
             pulseData = recorder.recordPulseData();
         } catch (IOException | TimeoutException ex) {
             error = true;
         }
         
         recordButton.setEnabled(true);
         statusTimer.stop();
         
         if (error)
         {
             playButton.setEnabled(false);
             statusLabel.setText("Record error. Try again."); //Todo: better error message
         }
         else if (pulseData.length == 0)
         {
             playButton.setEnabled(false);
             statusLabel.setText("Record error. Nothing recorded.");
         }
         else
         {
             playButton.setEnabled(true);
             statusLabel.setText("Recorded OK");
         }
         okButton.setEnabled(nameField.getText().length() > 0 && pulseData != null);
     }
     
     private void doPlayBack()
     {
         boolean error = false;
         try {
             recorder.playbackRecording();
         } catch (TimeoutException | IOException ex) {
             error = true;
         }
         
         recordButton.setEnabled(true);
         playButton.setEnabled(true);
         statusTimer.stop();
         if (error)
             statusLabel.setText("Playback error. Try again");
         else
             statusLabel.setText("Recorded OK");
     }
     
 
     /**
      * This method is called from within the constructor to initialize the form.
      * WARNING: Do NOT modify this code. The content of this method is always
      * regenerated by the Form Editor.
      */
     @SuppressWarnings("unchecked")
     // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
     private void initComponents() {
         java.awt.GridBagConstraints gridBagConstraints;
 
         jPanel1 = new javax.swing.JPanel();
         jLabel3 = new javax.swing.JLabel();
         modelLabel = new javax.swing.JLabel();
         jLabel5 = new javax.swing.JLabel();
         nameField = new javax.swing.JTextField();
         titleLabel = new javax.swing.JLabel();
         jLabel7 = new javax.swing.JLabel();
         jScrollPane1 = new javax.swing.JScrollPane();
         descriptionArea = new javax.swing.JTextArea();
         irSequenceLabel = new javax.swing.JLabel();
         recordButton = new javax.swing.JButton();
         playButton = new javax.swing.JButton();
         statusLabel = new javax.swing.JLabel();
         okButton = new javax.swing.JButton();
         cancelButton = new javax.swing.JButton();
 
         setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
         setTitle("Record IR command");
         setMinimumSize(new java.awt.Dimension(400, 300));
         setResizable(false);
 
         jPanel1.setLayout(new java.awt.GridBagLayout());
 
         jLabel3.setText("Camera Model");
         gridBagConstraints = new java.awt.GridBagConstraints();
         gridBagConstraints.gridx = 0;
         gridBagConstraints.gridy = 1;
         gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_END;
         gridBagConstraints.insets = new java.awt.Insets(0, 0, 10, 10);
         jPanel1.add(jLabel3, gridBagConstraints);
 
         modelLabel.setFont(new java.awt.Font("Tahoma", 1, 11)); // NOI18N
         modelLabel.setText("-");
         gridBagConstraints = new java.awt.GridBagConstraints();
         gridBagConstraints.gridx = 1;
         gridBagConstraints.gridy = 1;
         gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
         gridBagConstraints.insets = new java.awt.Insets(0, 0, 10, 0);
         jPanel1.add(modelLabel, gridBagConstraints);
 
         jLabel5.setText("Command Name");
         gridBagConstraints = new java.awt.GridBagConstraints();
         gridBagConstraints.gridx = 0;
         gridBagConstraints.gridy = 2;
         gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_END;
         gridBagConstraints.insets = new java.awt.Insets(0, 0, 10, 10);
         jPanel1.add(jLabel5, gridBagConstraints);
 
         nameField.addKeyListener(new java.awt.event.KeyAdapter() {
             public void keyReleased(java.awt.event.KeyEvent evt) {
                 nameFieldKeyReleased(evt);
             }
         });
         gridBagConstraints = new java.awt.GridBagConstraints();
         gridBagConstraints.gridx = 1;
         gridBagConstraints.gridy = 2;
         gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
         gridBagConstraints.insets = new java.awt.Insets(0, 0, 10, 0);
         jPanel1.add(nameField, gridBagConstraints);
 
         titleLabel.setFont(new java.awt.Font("Tahoma", 0, 18)); // NOI18N
         titleLabel.setText("Record IR command");
         gridBagConstraints = new java.awt.GridBagConstraints();
         gridBagConstraints.gridx = 0;
         gridBagConstraints.gridy = 0;
         gridBagConstraints.gridwidth = 2;
         gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
         gridBagConstraints.insets = new java.awt.Insets(0, 0, 5, 0);
         jPanel1.add(titleLabel, gridBagConstraints);
 
         jLabel7.setText("Command Description");
         gridBagConstraints = new java.awt.GridBagConstraints();
         gridBagConstraints.gridx = 0;
         gridBagConstraints.gridy = 3;
         gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_END;
         gridBagConstraints.insets = new java.awt.Insets(0, 0, 10, 10);
         jPanel1.add(jLabel7, gridBagConstraints);
 
         descriptionArea.setColumns(20);
         descriptionArea.setRows(3);
         jScrollPane1.setViewportView(descriptionArea);
 
         gridBagConstraints = new java.awt.GridBagConstraints();
         gridBagConstraints.gridx = 1;
         gridBagConstraints.gridy = 3;
         gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
         gridBagConstraints.insets = new java.awt.Insets(0, 0, 10, 0);
         jPanel1.add(jScrollPane1, gridBagConstraints);
 
         irSequenceLabel.setText("IR Sequence");
         gridBagConstraints = new java.awt.GridBagConstraints();
         gridBagConstraints.gridx = 0;
         gridBagConstraints.gridy = 4;
         gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_END;
         gridBagConstraints.insets = new java.awt.Insets(0, 0, 10, 10);
         jPanel1.add(irSequenceLabel, gridBagConstraints);
 
         recordButton.setText("<html>&#9679; Record</html>");
         recordButton.addActionListener(new java.awt.event.ActionListener() {
             public void actionPerformed(java.awt.event.ActionEvent evt) {
                 recordButtonActionPerformed(evt);
             }
         });
         gridBagConstraints = new java.awt.GridBagConstraints();
         gridBagConstraints.gridx = 1;
         gridBagConstraints.gridy = 4;
         gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
         gridBagConstraints.insets = new java.awt.Insets(0, 0, 10, 0);
         jPanel1.add(recordButton, gridBagConstraints);
 
         playButton.setText("<html>&#9658; Play</html>");
         playButton.setEnabled(false);
         playButton.addActionListener(new java.awt.event.ActionListener() {
             public void actionPerformed(java.awt.event.ActionEvent evt) {
                 playButtonActionPerformed(evt);
             }
         });
         gridBagConstraints = new java.awt.GridBagConstraints();
         gridBagConstraints.gridx = 2;
         gridBagConstraints.gridy = 4;
         gridBagConstraints.insets = new java.awt.Insets(0, 0, 10, 0);
         jPanel1.add(playButton, gridBagConstraints);
 
         statusLabel.setText("Nothing recorded");
         gridBagConstraints = new java.awt.GridBagConstraints();
         gridBagConstraints.gridx = 1;
         gridBagConstraints.gridy = 5;
         gridBagConstraints.gridwidth = 2;
         gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
         jPanel1.add(statusLabel, gridBagConstraints);
 
         okButton.setText("Ok");
         okButton.setToolTipText("Must enter a name and record an action");
         okButton.setEnabled(false);
         okButton.addActionListener(new java.awt.event.ActionListener() {
             public void actionPerformed(java.awt.event.ActionEvent evt) {
                 okButtonActionPerformed(evt);
             }
         });
         gridBagConstraints = new java.awt.GridBagConstraints();
         gridBagConstraints.gridx = 1;
         gridBagConstraints.gridy = 6;
         gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
         gridBagConstraints.insets = new java.awt.Insets(15, 0, 0, 0);
         jPanel1.add(okButton, gridBagConstraints);
 
         cancelButton.setText("Cancel");
         cancelButton.addActionListener(new java.awt.event.ActionListener() {
             public void actionPerformed(java.awt.event.ActionEvent evt) {
                 cancelButtonActionPerformed(evt);
             }
         });
         gridBagConstraints = new java.awt.GridBagConstraints();
         gridBagConstraints.gridx = 2;
         gridBagConstraints.gridy = 6;
         gridBagConstraints.insets = new java.awt.Insets(15, 0, 0, 0);
         jPanel1.add(cancelButton, gridBagConstraints);
 
         getContentPane().add(jPanel1, java.awt.BorderLayout.CENTER);
 
         pack();
     }// </editor-fold>//GEN-END:initComponents
 
     private void recordButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_recordButtonActionPerformed
         playButton.setEnabled(false);
         recordButton.setEnabled(false);
         statusTimer.stop();
         statusTimer = new Timer(500,new DotsListener(statusLabel, "Recording"));
         statusTimer.start();
         new Thread(new Runnable() {
             @Override
             public void run() {
                 doRecord();
             }
         }).start();
     }//GEN-LAST:event_recordButtonActionPerformed
 
     private void playButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_playButtonActionPerformed
         playButton.setEnabled(false);
         recordButton.setEnabled(false);
         statusTimer.stop();
         statusTimer = new Timer(500,new DotsListener(statusLabel, "Playing"));
         statusTimer.start();
         new Thread(new Runnable() {
             @Override
             public void run() {
                 doPlayBack();
             }
         }).start();
     }//GEN-LAST:event_playButtonActionPerformed
 
     private void okButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_okButtonActionPerformed
         if (editing)
         {
             ModelUtil.editCustomIRCommand(editingCommand, nameField.getText(), descriptionArea.getText());
            result = editingCommand;
         }
         else
         {
            IRCommand command = generateIRCommand();
             if (command != null)
                 ModelUtil.saveCustomIRCommand(command);
            result = command;
         }
         setVisible(false);
     }//GEN-LAST:event_okButtonActionPerformed
 
     private void cancelButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cancelButtonActionPerformed
         if (editing)
         {
             if (!editingCommand.getName().equals(nameField.getText()) || !editingCommand.getDescription().equals(descriptionArea.getText()))
             {
                 int answer = JOptionPane.showConfirmDialog(this, "Are you sure you want to cancel?\r\nThe changes will not be saved!", "Changes not saved", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
                 if (answer == JOptionPane.NO_OPTION)
                     return;
             }
         }
         else if (pulseData != null)
         {
             int answer = JOptionPane.showConfirmDialog(this, "Are you sure you want to cancel?\r\nThe recorded command will not be saved!", "Command not saved", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
             if (answer == JOptionPane.NO_OPTION)
                 return;
         }
        result = null;
         setVisible(false);
     }//GEN-LAST:event_cancelButtonActionPerformed
 
     private void nameFieldKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_nameFieldKeyReleased
         if (editing)
             okButton.setEnabled(nameField.getText().length() > 0);
         else
             okButton.setEnabled(nameField.getText().length() > 0 && pulseData != null);
     }//GEN-LAST:event_nameFieldKeyReleased
 
     // Variables declaration - do not modify//GEN-BEGIN:variables
     private javax.swing.JButton cancelButton;
     private javax.swing.JTextArea descriptionArea;
     private javax.swing.JLabel irSequenceLabel;
     private javax.swing.JLabel jLabel3;
     private javax.swing.JLabel jLabel5;
     private javax.swing.JLabel jLabel7;
     private javax.swing.JPanel jPanel1;
     private javax.swing.JScrollPane jScrollPane1;
     private javax.swing.JLabel modelLabel;
     private javax.swing.JTextField nameField;
     private javax.swing.JButton okButton;
     private javax.swing.JButton playButton;
     private javax.swing.JButton recordButton;
     private javax.swing.JLabel statusLabel;
     private javax.swing.JLabel titleLabel;
     // End of variables declaration//GEN-END:variables
 }
