 /*
  * Copyright 2002-2004 the original author or authors.
  * 
  * Licensed under the Apache License, Version 2.0 (the "License"); you may not
  * use this file except in compliance with the License. You may obtain a copy of
  * the License at
  * 
  * http://www.apache.org/licenses/LICENSE-2.0
  * 
  * Unless required by applicable law or agreed to in writing, software
  * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
  * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
  * License for the specific language governing permissions and limitations under
  * the License.
  */
 package org.springframework.richclient.filechooser;
 
 import java.awt.event.ActionEvent;
 import java.awt.event.ActionListener;
 import java.io.File;
 
 import javax.swing.JButton;
 import javax.swing.JComponent;
 import javax.swing.JFileChooser;
 import javax.swing.JFormattedTextField;
 import javax.swing.JLabel;
 import javax.swing.JPanel;
 import javax.swing.SwingUtilities;
 
 import org.apache.commons.logging.Log;
 import org.apache.commons.logging.LogFactory;
 import org.springframework.richclient.factory.AbstractControlFactory;
 import org.springframework.richclient.forms.SwingFormModel;
 import org.springframework.rules.values.ValidationListener;
 
 import com.jgoodies.forms.layout.CellConstraints;
 import com.jgoodies.forms.layout.FormLayout;
 
 /**
  * A combo box that allows you to type and/or select files, as well as click a
  * Browse button to navigate to the file you wish to work with.
  * 
  * @author Keith Donald
  */
 public class FileChooserComboBox extends AbstractControlFactory {
     private static final Log logger = LogFactory
             .getLog(FileChooserComboBox.class);
 
     private JFileChooser fileChooser;
 
     private String fileChooserLabel = "fileChooserLabel";
 
     private JFormattedTextField fileNameSelector;
 
     private JButton browseButton;
 
     private File startDirectory;
 
     private SwingFormModel formModel;
 
     private String formProperty;
 
     public FileChooserComboBox() {
 
     }
 
     public FileChooserComboBox(SwingFormModel formModel, String formProperty) {
         this.formModel = formModel;
         this.formProperty = formProperty;
     }
 
     public void addValidationListener(ValidationListener listener) {
         formModel.addValidationListener(listener);
     }
 
     public void removeValidationListener(ValidationListener listener) {
         formModel.removeValidationListener(listener);
     }
 
     public void setLabelMessageCode(String labelKey) {
         this.fileChooserLabel = labelKey;
     }
 
     public void setStartDirectory(File file) {
         this.startDirectory = file;
     }
 
     public File getStartDirectory() {
         if (startDirectory != null) {
             return startDirectory;
         }
         else {
             return getSelectedFile();
         }
     }
 
     public File getSelectedFile() {
        return (File)fileNameSelector.getValue();
     }
 
     public void setEnabled(boolean enabled) {
         fileNameSelector.setEnabled(enabled);
         browseButton.setEnabled(false);
     }
 
     protected JComponent createControl() {
         this.fileNameSelector = formModel.createBoundTextField(formProperty);
         JLabel fileToProcess = getComponentFactory().createLabelFor(
                 fileChooserLabel, fileNameSelector);
         this.browseButton = getComponentFactory().createButton("button.browse");
         browseButton.addActionListener(new ActionListener() {
             public void actionPerformed(ActionEvent event) {
                 if (fileChooser == null) {
                     fileChooser = new JFileChooser(getStartDirectory());
                 }
                 else {
                     fileChooser.setCurrentDirectory(getStartDirectory());
                 }
                 int returnVal = fileChooser.showOpenDialog(SwingUtilities
                         .getWindowAncestor(browseButton));
                 if (returnVal == JFileChooser.APPROVE_OPTION) {
                     fileNameSelector.setValue(fileChooser.getSelectedFile());
                 }
             }
         });
         FormLayout layout = new FormLayout("pref:grow, 6dlu:none, min",
                 "pref, 3dlu, pref");
         JPanel panel = new JPanel(layout);
         CellConstraints cc = new CellConstraints();
         panel.add(fileToProcess, cc.xyw(1, 1, 3));
         panel.add(fileNameSelector, cc.xy(1, 3));
         panel.add(browseButton, cc.xy(3, 3));
         return panel;
     }
 
 }
