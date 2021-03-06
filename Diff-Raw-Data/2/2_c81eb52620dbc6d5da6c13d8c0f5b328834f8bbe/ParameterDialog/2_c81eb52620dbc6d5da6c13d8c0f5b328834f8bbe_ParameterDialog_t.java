 //----------------------------------------------------------------------------
 // $Id$
 // $Source$
 //----------------------------------------------------------------------------
 
 package gui;
 
 import java.awt.*;
 import java.awt.event.*;
 import java.io.*;
 import java.util.*;
 import javax.swing.*;
 import gtp.GtpError;
 import utils.GuiUtils;
 import utils.StringUtils;
 
 //----------------------------------------------------------------------------
 
 public class ParameterDialog
 {
     public static void editParameters(String paramCommand, Frame owner,
                                       String title, String response,
                                       CommandThread commandThread)
     {
         Vector parameters = parseResponse(response);
         JPanel panel = new JPanel(new GridLayout(0, 1));
         for (int i = 0; i < parameters.size(); ++i)
             panel.add(((Parameter)parameters.get(i)).getComponent());
         Object options[] = { "Ok", "Cancel" };
         int r =
             JOptionPane.showOptionDialog(owner, panel, title,
                                          JOptionPane.OK_CANCEL_OPTION,
                                          JOptionPane.PLAIN_MESSAGE, null,
                                          options, options[0]);
         if (r != 0)
             return;
         for (int i = 0; i < parameters.size(); ++i)
         {
             Parameter parameter = (Parameter)parameters.get(i);
             if (! parameter.isChanged())
                 continue;
             String command =
                 paramCommand + " " + parameter.getKey() + " "
                 + parameter.getNewValue();
             try
             {
                 commandThread.sendCommand(command);
             }
             catch (GtpError e)
             {
                 String message =
                     "Could not change parameter " + parameter.getKey()
                     + ":\n" + StringUtils.capitalize(e.getMessage());
                 SimpleDialogs.showError(owner, message);
             }
         }
     }
 
     private static abstract class Parameter
     {
         public Parameter(String key, String value)
         {
             m_key = key;
             m_value = value;
             m_label = key.replace('_', ' ');
         }
 
         public String getKey()
         {
             return m_key;
         }
 
         public String getLabel()
         {
             return m_label;
         }
 
         public String getValue()
         {
             return m_value;
         }
 
         public abstract String getNewValue();
 
         public abstract boolean isChanged();
 
         public abstract Component getComponent();
 
         private String m_key;
 
         private String m_label;
 
         private String m_value;
     }
     
     private static class BoolParameter
         extends Parameter
     {
         public BoolParameter(String key, String value)
         {
             super(key, value);
             m_bool = false;
             try
             {
                 if (Integer.parseInt(value) != 0)
                     m_bool = true;
             }
             catch (NumberFormatException e)
             {
             }
             m_panel = new JPanel(new GridLayout(1, 0, GuiUtils.PAD, 0));
             m_checkBox = new JCheckBox(getLabel(), m_bool);
             m_panel.add(m_checkBox);
         }
 
         public String getNewValue()
         {
             if (m_checkBox.isSelected())
                 return "1";
             return "0";
         }
 
         public boolean isChanged()
         {
             return m_checkBox.isSelected() != m_bool;
         }
 
         public Component getComponent()
         {
             return m_panel;
         }
 
         private boolean m_bool;
 
         private JCheckBox m_checkBox;
 
         private JPanel m_panel;
     }
 
     private static class StringParameter
         extends Parameter
     {
         public StringParameter(String key, String value)
         {
             super(key, value);
             m_panel = new JPanel(new GridLayout(1, 0, GuiUtils.PAD, 0));
             m_panel.add(new JLabel(getLabel()));
             m_textField = new JTextField(value);
             m_panel.add(m_textField);
         }
 
         public String getNewValue()
         {
             return m_textField.getText().trim();
         }
 
         public boolean isChanged()
         {
             return ! getNewValue().equals(getValue());
         }
 
         public Component getComponent()
         {
             return m_panel;
         }
 
         private JTextField m_textField;
 
         private JPanel m_panel;
     }
 
     private static Vector parseResponse(String response)
     {
         Vector parameters = new Vector();
         BufferedReader reader =
             new BufferedReader(new StringReader(response));
         while (true)
         {
             String line = null;
             try
             {
                 line = reader.readLine();
             }
             catch (IOException e)
             {
             }
             if (line == null)
                 break;
             line = line.trim();
             final int STRING = 0;
             final int BOOL = 1;
             int type = STRING;
             if (line.startsWith("[bool]"))
             {
                 type = BOOL;
                line = line.replaceFirst("\\[bool\\]", "").trim();
             }
             int pos = line.indexOf(' ');
             String key;
             String value;
             if (pos > 0)
             {
                 key = line.substring(0, pos).trim();
                 value = line.substring(pos + 1).trim();
             }
             else
             {
                 key = line;
                 value = "";
             }
             if (type == BOOL)
                 parameters.add(new BoolParameter(key, value));
             else
                 parameters.add(new StringParameter(key, value));
         }
         return parameters;
     }
 }
