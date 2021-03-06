 /**
  * Copyright 2013 by ATLauncher and Contributors
  *
  * ATLauncher is licensed under CC BY-NC-ND 3.0 which allows others you to
  * share this software with others as long as you credit us by linking to our
  * website at http://www.atlauncher.com. You also cannot modify the application
  * in any way or make commercial use of this software.
  *
  * Link to license: http://creativecommons.org/licenses/by-nc-nd/3.0/
  */
 package com.atlauncher.gui;
 
 import java.awt.BorderLayout;
 import java.awt.Dimension;
 import java.awt.FlowLayout;
 import java.awt.GridBagConstraints;
 import java.awt.GridBagLayout;
 import java.awt.Insets;
 import java.awt.event.ActionEvent;
 import java.awt.event.ActionListener;
 import java.awt.event.MouseAdapter;
 import java.awt.event.MouseEvent;
 
 import javax.swing.ImageIcon;
 import javax.swing.JButton;
 import javax.swing.JCheckBox;
 import javax.swing.JComboBox;
 import javax.swing.JLabel;
 import javax.swing.JOptionPane;
 import javax.swing.JPanel;
 import javax.swing.JTextField;
 
 import com.atlauncher.App;
 import com.atlauncher.data.Language;
 import com.atlauncher.data.Server;
 
 @SuppressWarnings("serial")
 public class SettingsPanel extends JPanel {
 
     private JPanel topPanel;
     private JPanel bottomPanel;
     private JButton saveButton;
     private ImageIcon helpIcon;
 
     private JLabel languageLabel;
     private JComboBox<Language> language;
 
     private JLabel downloadServerLabel;
     private JComboBox<Server> server;
 
     private JLabel memoryLabel;
     private JComboBox<String> memory;
 
     private JLabel permGenLabel;
     private JTextField permGen;
 
     private JPanel windowSizePanel;
     private JLabel windowSizeLabel;
     private JTextField widthField;
     private JTextField heightField;
 
     private JLabel javaParametersLabel;
     private JTextField javaParameters;
 
     private JLabel enableConsoleLabel;
     private JCheckBox enableConsole;
 
     private JLabel enableLeaderboardsLabel;
     private JCheckBox enableLeaderboards;
 
     private JLabel enableLoggingLabel;
     private JCheckBox enableLogs;
 
     private final Insets LABEL_INSETS = new Insets(3, 0, 3, 10);
     private final Insets FIELD_INSETS = new Insets(3, 0, 3, 0);
     private final Insets LABEL_INSETS_SMALL = new Insets(0, 0, 0, 10);
     private final Insets FIELD_INSETS_SMALL = new Insets(0, 0, 0, 0);
 
     public SettingsPanel() {
         setLayout(new BorderLayout());
         topPanel = new JPanel();
         topPanel.setLayout(new GridBagLayout());
         GridBagConstraints gbc = new GridBagConstraints();
 
         helpIcon = Utils.getIconImage("/resources/Help.png");
 
         // Language
         gbc.gridx = 0;
         gbc.gridy = 0;
         gbc.insets = LABEL_INSETS;
         gbc.anchor = GridBagConstraints.BASELINE_TRAILING;
         languageLabel = new JLabel(App.settings.getLocalizedString("settings.language") + ":");
         languageLabel.setIcon(helpIcon);
         languageLabel.addMouseListener(new MouseAdapter() {
             @Override
             public void mouseClicked(MouseEvent e) {
                 if (e.getButton() == MouseEvent.BUTTON1) {
                     if (e.getX() < 16 && e.getY() < 16) {
                         JOptionPane.showMessageDialog(App.settings.getParent(),
                                 App.settings.getLocalizedString("settings.languagehelp"),
                                 App.settings.getLocalizedString("settings.help"),
                                 JOptionPane.PLAIN_MESSAGE);
                     }
                 }
             }
         });
         topPanel.add(languageLabel, gbc);
 
         gbc.gridx++;
         gbc.insets = FIELD_INSETS;
         gbc.anchor = GridBagConstraints.BASELINE_LEADING;
         language = new JComboBox<Language>();
         for (Language languagee : App.settings.getLanguages()) {
             language.addItem(languagee);
         }
         language.setSelectedItem(App.settings.getLanguage());
         topPanel.add(language, gbc);
 
         // Download Server
         gbc.gridx = 0;
         gbc.gridy++;
         gbc.insets = LABEL_INSETS;
         gbc.anchor = GridBagConstraints.BASELINE_TRAILING;
         downloadServerLabel = new JLabel(App.settings.getLocalizedString("settings.downloadserver")
                 + ":");
         downloadServerLabel.setIcon(helpIcon);
         downloadServerLabel.addMouseListener(new MouseAdapter() {
             @Override
             public void mouseClicked(MouseEvent e) {
                 if (e.getButton() == MouseEvent.BUTTON1) {
                     if (e.getX() < 16 && e.getY() < 16) {
                         JOptionPane.showMessageDialog(App.settings.getParent(),
                                 App.settings.getLocalizedString("settings.downloadserverhelp"),
                                 App.settings.getLocalizedString("settings.help"),
                                 JOptionPane.PLAIN_MESSAGE);
                     }
                 }
             }
         });
         topPanel.add(downloadServerLabel, gbc);
 
         gbc.gridx++;
         gbc.insets = FIELD_INSETS;
         gbc.anchor = GridBagConstraints.BASELINE_LEADING;
         server = new JComboBox<Server>();
         for (Server serverr : App.settings.getServers()) {
             server.addItem(serverr);
         }
         server.setSelectedItem(App.settings.getOriginalServer());
         topPanel.add(server, gbc);
 
         // Memory Settings
         gbc.gridx = 0;
         gbc.gridy++;
         gbc.insets = LABEL_INSETS;
         gbc.anchor = GridBagConstraints.BASELINE_TRAILING;
         memoryLabel = new JLabel(App.settings.getLocalizedString("settings.memory") + ":");
         memoryLabel.setIcon(helpIcon);
         memoryLabel.addMouseListener(new MouseAdapter() {
             @Override
             public void mouseClicked(MouseEvent e) {
                 if (e.getButton() == MouseEvent.BUTTON1) {
                     if (e.getX() < 16 && e.getY() < 16) {
                         if (!Utils.is64Bit()) {
                             JOptionPane.showMessageDialog(
                                     App.settings.getParent(),
                                     "<html><center>"
                                             + App.settings.getLocalizedString(
                                                     "settings.memoryhelp32bit", "<br/>")
                                             + "</center></html>", App.settings
                                             .getLocalizedString("settings.help"),
                                     JOptionPane.PLAIN_MESSAGE);
                         } else {
                             JOptionPane.showMessageDialog(App.settings.getParent(),
                                     App.settings.getLocalizedString("settings.memoryhelp"),
                                     App.settings.getLocalizedString("settings.help"),
                                     JOptionPane.PLAIN_MESSAGE);
                         }
                     }
                 }
             }
         });
         topPanel.add(memoryLabel, gbc);
 
         gbc.gridx++;
         gbc.insets = FIELD_INSETS;
         gbc.anchor = GridBagConstraints.BASELINE_LEADING;
         memory = new JComboBox<String>();
         String[] memoryOptions = Utils.getMemoryOptions();
         for (int i = 0; i < memoryOptions.length; i++) {
             memory.addItem(memoryOptions[i]);
         }
         memory.setSelectedItem(App.settings.getMemory() + " MB");
         topPanel.add(memory, gbc);
 
         // Perm Gen Settings
         gbc.gridx = 0;
         gbc.gridy++;
         gbc.insets = LABEL_INSETS;
         gbc.anchor = GridBagConstraints.BASELINE_TRAILING;
         permGenLabel = new JLabel(App.settings.getLocalizedString("settings.permgen") + ":");
         permGenLabel.setIcon(helpIcon);
         permGenLabel.addMouseListener(new MouseAdapter() {
             @Override
             public void mouseClicked(MouseEvent e) {
                 if (e.getButton() == MouseEvent.BUTTON1) {
                     if (e.getX() < 16 && e.getY() < 16) {
                         JOptionPane.showMessageDialog(App.settings.getParent(),
                                 App.settings.getLocalizedString("settings.permgenhelp"),
                                 App.settings.getLocalizedString("settings.help"),
                                 JOptionPane.PLAIN_MESSAGE);
                     }
                 }
             }
         });
         topPanel.add(permGenLabel, gbc);
 
         gbc.gridx++;
         gbc.insets = FIELD_INSETS;
         gbc.anchor = GridBagConstraints.BASELINE_LEADING;
         permGen = new JTextField(4);
         permGen.setText(App.settings.getPermGen() + "");
         topPanel.add(permGen, gbc);
 
         // Window Size
         gbc.gridx = 0;
         gbc.gridy++;
         gbc.gridwidth = 1;
         gbc.insets = LABEL_INSETS_SMALL;
         gbc.anchor = GridBagConstraints.BASELINE_TRAILING;
         windowSizeLabel = new JLabel(App.settings.getLocalizedString("settings.windowsize") + ":");
         windowSizeLabel.setIcon(helpIcon);
         windowSizeLabel.addMouseListener(new MouseAdapter() {
             @Override
             public void mouseClicked(MouseEvent e) {
                 if (e.getButton() == MouseEvent.BUTTON1) {
                     if (e.getX() < 16 && (e.getY() > 10 && e.getY() < 26)) {
                         JOptionPane.showMessageDialog(App.settings.getParent(),
                                 App.settings.getLocalizedString("settings.windowsizehelp"),
                                 App.settings.getLocalizedString("settings.help"),
                                 JOptionPane.PLAIN_MESSAGE);
                     }
                 }
             }
         });
         topPanel.add(windowSizeLabel, gbc);
 
         gbc.gridx++;
         gbc.insets = FIELD_INSETS_SMALL;
         gbc.anchor = GridBagConstraints.BASELINE_LEADING;
         windowSizePanel = new JPanel();
         windowSizePanel.setLayout(new FlowLayout());
         widthField = new JTextField(4);
         widthField.setText(App.settings.getWindowWidth() + "");
         heightField = new JTextField(4);
         heightField.setText(App.settings.getWindowHeight() + "");
         windowSizePanel.add(widthField);
         windowSizePanel.add(new JLabel("x"));
         windowSizePanel.add(heightField);
         topPanel.add(windowSizePanel, gbc);
         windowSizePanel.setPreferredSize(new Dimension(137,
                 windowSizePanel.getPreferredSize().height));
         windowSizeLabel.setPreferredSize(new Dimension(windowSizeLabel.getPreferredSize().width,
                 windowSizePanel.getPreferredSize().height));
 
         // Java Paramaters
 
         gbc.gridx = 0;
         gbc.gridy++;
         gbc.insets = LABEL_INSETS;
         gbc.anchor = GridBagConstraints.BASELINE_TRAILING;
         javaParametersLabel = new JLabel(App.settings.getLocalizedString("settings.javaparameters")
                 + ":");
         javaParametersLabel.setIcon(helpIcon);
         javaParametersLabel.addMouseListener(new MouseAdapter() {
             @Override
             public void mouseClicked(MouseEvent e) {
                 if (e.getButton() == MouseEvent.BUTTON1) {
                     if (e.getX() < 16 && e.getY() < 16) {
                         JOptionPane.showMessageDialog(App.settings.getParent(),
                                 App.settings.getLocalizedString("settings.javaparametershelp"),
                                 App.settings.getLocalizedString("settings.help"),
                                 JOptionPane.PLAIN_MESSAGE);
                     }
                 }
             }
         });
         topPanel.add(javaParametersLabel, gbc);
 
         gbc.gridx++;
         gbc.insets = FIELD_INSETS;
         gbc.anchor = GridBagConstraints.BASELINE_LEADING;
         javaParameters = new JTextField(20);
         javaParameters.setText(App.settings.getJavaParameters());
         topPanel.add(javaParameters, gbc);
 
         // Enable Console
 
         gbc.gridx = 0;
         gbc.gridy++;
         gbc.insets = LABEL_INSETS;
         gbc.anchor = GridBagConstraints.BASELINE_TRAILING;
         enableConsoleLabel = new JLabel(App.settings.getLocalizedString("settings.console") + "?");
         enableConsoleLabel.setIcon(helpIcon);
         enableConsoleLabel.addMouseListener(new MouseAdapter() {
             @Override
             public void mouseClicked(MouseEvent e) {
                 if (e.getButton() == MouseEvent.BUTTON1) {
                     if (e.getX() < 16 && e.getY() < 16) {
                         JOptionPane.showMessageDialog(App.settings.getParent(),
                                 App.settings.getLocalizedString("settings.consolehelp"),
                                 App.settings.getLocalizedString("settings.help"),
                                 JOptionPane.PLAIN_MESSAGE);
                     }
                 }
             }
         });
         topPanel.add(enableConsoleLabel, gbc);
 
         gbc.gridx++;
         gbc.insets = FIELD_INSETS;
         gbc.anchor = GridBagConstraints.BASELINE_LEADING;
         enableConsole = new JCheckBox();
         if (App.settings.enableConsole()) {
             enableConsole.setSelected(true);
         }
         topPanel.add(enableConsole, gbc);
 
         // Enable Leaderboards
 
         gbc.gridx = 0;
         gbc.gridy++;
         gbc.insets = LABEL_INSETS;
         gbc.anchor = GridBagConstraints.BASELINE_TRAILING;
         enableLeaderboardsLabel = new JLabel(
                 App.settings.getLocalizedString("settings.leaderboards") + "?");
         enableLeaderboardsLabel.setIcon(helpIcon);
         enableLeaderboardsLabel.addMouseListener(new MouseAdapter() {
             @Override
             public void mouseClicked(MouseEvent e) {
                 if (e.getButton() == MouseEvent.BUTTON1) {
                     if (e.getX() < 16 && e.getY() < 16) {
                         JOptionPane.showMessageDialog(App.settings.getParent(),
                                 App.settings.getLocalizedString("settings.leaderboardshelp"),
                                 App.settings.getLocalizedString("settings.help"),
                                 JOptionPane.PLAIN_MESSAGE);
                     }
                 }
             }
         });
         topPanel.add(enableLeaderboardsLabel, gbc);
 
         gbc.gridx++;
         gbc.insets = FIELD_INSETS;
         gbc.anchor = GridBagConstraints.BASELINE_LEADING;
         enableLeaderboards = new JCheckBox();
         if (App.settings.enableLeaderboards()) {
             enableLeaderboards.setSelected(true);
         }
         topPanel.add(enableLeaderboards, gbc);
 
         // Enable Logging
 
         gbc.gridx = 0;
         gbc.gridy++;
         gbc.insets = LABEL_INSETS;
         gbc.anchor = GridBagConstraints.BASELINE_TRAILING;
         enableLoggingLabel = new JLabel(App.settings.getLocalizedString("settings.logging") + "?");
         enableLoggingLabel.setIcon(helpIcon);
         enableLoggingLabel.addMouseListener(new MouseAdapter() {
             @Override
             public void mouseClicked(MouseEvent e) {
                 if (e.getButton() == MouseEvent.BUTTON1) {
                     if (e.getX() < 16 && e.getY() < 16) {
                         JOptionPane.showMessageDialog(App.settings.getParent(), "<html><center>"
                                 + App.settings.getLocalizedString("settings.logginghelp", "<br/>")
                                 + "</center></html>",
                                 App.settings.getLocalizedString("settings.help"),
                                 JOptionPane.PLAIN_MESSAGE);
                     }
                 }
             }
         });
         topPanel.add(enableLoggingLabel, gbc);
 
         gbc.gridx++;
         gbc.insets = FIELD_INSETS;
         gbc.anchor = GridBagConstraints.BASELINE_LEADING;
         enableLogs = new JCheckBox();
         if (App.settings.enableLogs()) {
             enableLogs.setSelected(true);
         }
         topPanel.add(enableLogs, gbc);
 
         // End Components
 
         bottomPanel = new JPanel();
         saveButton = new JButton(App.settings.getLocalizedString("common.save"));
         saveButton.addActionListener(new ActionListener() {
             public void actionPerformed(ActionEvent arg0) {
                 boolean reboot = false;
                 if (language.getSelectedItem() != App.settings.getLanguage()) {
                     reboot = true;
                 }
                 App.settings.setLanguage((Language) language.getSelectedItem());
                 App.settings.setServer((Server) server.getSelectedItem());
                 App.settings.setMemory(Integer.parseInt(((String) memory.getSelectedItem())
                         .replace(" MB", "")));
                 App.settings.setPermGen(Integer
                         .parseInt(permGen.getText().replaceAll("[^0-9]", "")));
                 App.settings.setWindowWidth(Integer.parseInt(widthField.getText().replaceAll(
                         "[^0-9]", "")));
                 App.settings.setWindowHeight(Integer.parseInt(heightField.getText().replaceAll(
                         "[^0-9]", "")));
                 App.settings.setJavaParameters(javaParameters.getText());
                 App.settings.setEnableConsole(enableConsole.isSelected());
                 App.settings.setEnableLeaderboards(enableLeaderboards.isSelected());
                 App.settings.setEnableLogs(enableLogs.isSelected());
                 App.settings.saveProperties();
                 App.settings.getConsole().log("Settings Saved!");
                 if (reboot) {
                     App.settings.restartLauncher();
                 }
                 String[] options = { App.settings.getLocalizedString("common.ok") };
                 JOptionPane.showOptionDialog(App.settings.getParent(),
                         App.settings.getLocalizedString("settings.saved"),
                         App.settings.getLocalizedString("settings.saved"),
                         JOptionPane.DEFAULT_OPTION, JOptionPane.INFORMATION_MESSAGE, null, options,
                         options[0]);
             }
         });
         bottomPanel.add(saveButton);
 
         add(topPanel, BorderLayout.CENTER);
         add(bottomPanel, BorderLayout.SOUTH);
     }
 
 }
