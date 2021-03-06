 package jDistsim.ui.dialog;
 
 import jDistsim.application.designer.common.UIConfiguration;
 
 import javax.swing.*;
import java.awt.*;
 
 /**
  * Author: Jirka Pénzeš
  * Date: 24.2.13
  * Time: 16:55
  */
 public class DialogComponentFactory implements IDialogComponentFactory {
 
     @Override
     public JLabel makeLabel(String text) {
         JLabel label = new JLabel(text);
         label.setFont(UIConfiguration.getInstance().getDefaultFont(true, 12));
         return label;
     }
 
     @Override
     public JButton makeButton(String text) {
         return new JButton(text);
     }
 
     @Override
     public JTextField makeTextField() {
         return new JTextField(10);
     }

    @Override
    public JTextField makeTextField(int size) {
        return new JTextField(size);
    }


    @Override
    public JCheckBox makeCheckBox(String text) {
        JCheckBox checkBox = new JCheckBox(text);
        checkBox.setBackground(Color.white);
        return checkBox;
    }
 }
