 package org.esa.beam.framework.gpf.ui;
 
 import com.bc.ceres.binding.ValueContainer;
 import com.bc.ceres.binding.ValueModel;
 import com.bc.ceres.binding.swing.BindingContext;
 import com.bc.ceres.binding.swing.Binding;
 
 import javax.swing.*;
 import java.awt.*;
 import java.awt.event.ActionEvent;
 import java.awt.event.ActionListener;
 import java.io.File;
 
 /**
  * A utility class used to create a {@link JPanel} containg default Swing components and their corresponding bindings for the
  * {@link ValueContainer} given by the {@link BindingContext}.
  * <p/>
  * <p>If the {@code displayName} property of a {@link com.bc.ceres.binding.ValueDescriptor ValueDescriptor} is set, it will be used as label, otherwise
  * a label is derived from the {@code name} property.</p>
  */
 public class ParametersPane {
     private final BindingContext bindingContext;
 
     public ParametersPane(BindingContext bindingContext) {
         this.bindingContext = bindingContext;
     }
 
     public BindingContext getBindingContext() {
         return bindingContext;
     }
 
     public JPanel createPanel() {
         ValueContainer valueContainer = bindingContext.getValueContainer();
         ValueModel[] models = valueContainer.getModels();
         final JPanel panel = new JPanel(new GridBagLayout());
         GridBagConstraints gbc = new GridBagConstraints();
         gbc.fill = GridBagConstraints.HORIZONTAL;
         gbc.anchor = GridBagConstraints.WEST;
         gbc.insets.top = 1;
         gbc.insets.bottom = 1;
         gbc.insets.right = 1;
         gbc.insets.left = 1;
         gbc.gridy = 0;
         for (ValueModel model : models) {
             JComponent editorComponent;
             final Class<?> type = model.getDescriptor().getType();
             if (isNumericType(type)) {
                 JTextField textField = new JTextField();
                 textField.setHorizontalAlignment(JTextField.RIGHT);
                 int fontSize = textField.getFont().getSize();
                 textField.setFont(new Font("Courier", Font.PLAIN, fontSize));
                 bindingContext.bind(textField, model.getDescriptor().getName());
                 editorComponent = textField;
             } else if (isBooleanType(type)) {
                 JCheckBox checkBox = new JCheckBox();
                 bindingContext.bind(checkBox, model.getDescriptor().getName());
                 editorComponent = checkBox;
             } else if (File.class.isAssignableFrom(type)) {
                 JTextField textField = new JTextField();
                 final Binding binding = bindingContext.bind(textField, model.getDescriptor().getName());
                 JPanel subPanel = new JPanel(new BorderLayout(2, 2));
                 subPanel.add(textField, BorderLayout.CENTER);
                 JButton etcButton = new JButton("...");
                 etcButton.addActionListener(new ActionListener() {
                     public void actionPerformed(ActionEvent e) {
                         JFileChooser fileChooser = new JFileChooser();
                         int i = fileChooser.showDialog(panel, "Select");
                         if (i == JFileChooser.APPROVE_OPTION && fileChooser.getSelectedFile() != null) {
                            binding.setValue(fileChooser.getSelectedFile());
                         }
                     }
                 });
                 subPanel.add(etcButton, BorderLayout.EAST);
                 editorComponent = subPanel;
             } else {
                 if (model.getDescriptor().getValueSet() != null) {
                     JComboBox comboBox = new JComboBox();
                     bindingContext.bind(comboBox, model.getDescriptor().getName());
                     editorComponent = comboBox;
                 } else {
                     JTextField textField = new JTextField();
                     bindingContext.bind(textField, model.getDescriptor().getName());
                     editorComponent = textField;
                 }
             }
             editorComponent.setName(model.getDescriptor().getName());
             editorComponent.setToolTipText(model.getDescriptor().getDescription());
             if (editorComponent instanceof JCheckBox) {
                 JCheckBox checkBox = (JCheckBox) editorComponent  ;
                 checkBox.setText(getDisplayName(model));
                 gbc.gridx = 0;
                 gbc.weightx = 1.0;
                 panel.add(checkBox, gbc);
             } else {
                 JLabel label = new JLabel(getDisplayName(model) + ":");
                 gbc.gridx = 0;
                 gbc.weightx = 0.0;
                 panel.add(label, gbc);
                 gbc.gridx = 1;
                 gbc.weightx = 1.0;
                 panel.add(editorComponent, gbc);
             }
             gbc.gridy++;
         }
         gbc.fill = GridBagConstraints.BOTH;
         gbc.gridx = 0;
         gbc.gridwidth = 2;
         gbc.weightx = 1.0;
         gbc.weighty = 1.0;
         panel.add(new JPanel(), gbc);
         return panel;
     }
 
     private boolean isBooleanType(Class<?> type) {
         return Boolean.TYPE.equals(type)
                 || Boolean.class.isAssignableFrom(type);
     }
 
     private boolean isNumericType(Class<?> type) {
         return Byte.TYPE.equals(type)
                 || Short.TYPE.equals(type)
                 || Integer.TYPE.equals(type)
                 || Long.TYPE.equals(type)
                 || Float.TYPE.equals(type)
                 || Double.TYPE.equals(type)
                 || Number.class.isAssignableFrom(type);
     }
 
     private static String getDisplayName(ValueModel model) {
         Object label = model.getDescriptor().getDisplayName();
         if (label != null) {
             return label.toString();
         }
         String name = model.getDescriptor().getName().replace("_", " ");
         return createDisplayName(name);
     }
 
     public static String createDisplayName(String name) {
         StringBuilder sb = new StringBuilder(name.length());
         for (int i = 0; i < name.length(); i++) {
             char ch = name.charAt(i);
             if (i == 0) {
                 sb.append(Character.toUpperCase(ch));
             } else if (i > 0 && i < name.length() - 1
                     && Character.isUpperCase(ch) &&
                     Character.isLowerCase(name.charAt(i + 1))) {
                 sb.append(' ');
                 sb.append(Character.toLowerCase(ch));
             } else {
                 sb.append(ch);
             }
         }
         return sb.toString();
     }
 
 }
