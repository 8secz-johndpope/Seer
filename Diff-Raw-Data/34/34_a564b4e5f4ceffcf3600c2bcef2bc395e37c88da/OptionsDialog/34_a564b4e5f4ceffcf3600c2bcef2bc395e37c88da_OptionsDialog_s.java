 /**
  *  Copyright (C) 2002-2011  The FreeCol Team
  *
  *  This file is part of FreeCol.
  *
  *  FreeCol is free software: you can redistribute it and/or modify
  *  it under the terms of the GNU General Public License as published by
  *  the Free Software Foundation, either version 2 of the License, or
  *  (at your option) any later version.
  *
  *  FreeCol is distributed in the hope that it will be useful,
  *  but WITHOUT ANY WARRANTY; without even the implied warranty of
  *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  *  GNU General Public License for more details.
  *
  *  You should have received a copy of the GNU General Public License
  *  along with FreeCol.  If not, see <http://www.gnu.org/licenses/>.
  */
 
 
 package net.sf.freecol.client.gui.panel;
 
 import java.awt.Component;
 import java.awt.Dimension;
 import java.awt.event.ActionEvent;
 import java.io.File;
 import java.io.FileInputStream;
 import java.util.ArrayList;
 import java.util.List;
 import java.util.logging.Logger;
 
 import javax.swing.JButton;
 import javax.swing.JPanel;
 import javax.swing.JScrollPane;
 import javax.swing.filechooser.FileFilter;
 
 import net.sf.freecol.FreeCol;
 import net.sf.freecol.client.gui.Canvas;
 import net.sf.freecol.client.gui.i18n.Messages;
 import net.sf.freecol.client.gui.option.OptionGroupUI;
 import net.sf.freecol.common.option.OptionGroup;
 
 import net.miginfocom.swing.MigLayout;
 
 /**
  * Dialog for changing the {@link net.sf.freecol.client.ClientOptions}.
  */
 public class OptionsDialog extends FreeColDialog<OptionGroup>  {
 
     private static final Logger logger = Logger.getLogger(OptionsDialog.class.getName());
 
     private static final String RESET = "RESET";
     private static final String SAVE = "SAVE";
    private static final String LOAD = "LOAD";
 
     private OptionGroupUI ui;
     private OptionGroup group;
     private JButton reset = new JButton(Messages.message("reset"));
     private JButton load = new JButton(Messages.message("load"));
     private JButton save = new JButton(Messages.message("save"));
     private JPanel optionPanel;
 
     private List<JButton> buttons = new ArrayList<JButton>();
     private boolean editable = true;
 
    private static final FileFilter[] filters = new FileFilter[] {
         new FileFilter() {
             public boolean accept(File pathname) {
                 return pathname.getName().endsWith(".xml");
             }
             public String getDescription() {
                 return Messages.message("filter.xml");
             }
         }
     };
 
     /**
      * The constructor that will add the items to this panel.
      * @param parent The parent of this panel.
      */
     public OptionsDialog(Canvas parent, boolean editable) {
         super(parent);
         this.editable = editable;
         setLayout(new MigLayout("wrap 1, fill"));
 
         reset.setActionCommand(RESET);
         reset.addActionListener(this);
 
         load.setActionCommand(LOAD);
         load.addActionListener(this);
 
         save.setActionCommand(SAVE);
         save.addActionListener(this);
 
         buttons.add(reset);
         buttons.add(load);
         buttons.add(save);
 
         setCancelComponent(cancelButton);
 
         setSize(850, 600);
 
     }
 
     protected void initialize(OptionGroup group, String header, Component component) {
 
         this.group = group;
 
         removeAll();
 
         // Header:
         add(getDefaultHeader(header), "center");
 
         // Additional component, if any
         if (component != null) {
             add(component);
         }
 
         // Options:
         ui = new OptionGroupUI(group, isGroupEditable());
         optionPanel = new JPanel() {
             @Override
             public String getUIClassID() {
                 return "ReportPanelUI";
             }
         };
         optionPanel.setOpaque(true);
         optionPanel.add(ui);
         JScrollPane scrollPane = new JScrollPane(optionPanel,
                                                  JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                                                  JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
         scrollPane.getVerticalScrollBar().setUnitIncrement( 16 );
         add(scrollPane, "height 100%, width 100%");
 
         // Buttons:
         if (editable) {
             int cells = buttons.size() + 2;
             add(okButton, "newline 20, tag ok, split " + cells);
             add(cancelButton, "tag cancel");
             for (JButton button : buttons) {
                 add(button);
             }
         } else {
             add(okButton, "newline 20, tag ok");
         }
     }
 
     @Override
     public Dimension getMinimumSize() {
         return new Dimension(850, 700);
     }
 
     @Override
     public Dimension getPreferredSize() {
         return getMinimumSize();
     }
 
     protected OptionGroupUI getOptionUI() {
         return ui;
     }
 
     protected boolean isGroupEditable() {
         return editable;
     }
 
     protected List<JButton> getButtons() {
         return buttons;
     }
 
     protected void updateUI(OptionGroup group) {
         this.group = group;
         optionPanel.removeAll();
         optionPanel.add(new OptionGroupUI(group, isGroupEditable()));
         revalidate();
         repaint();
     }
 
     protected OptionGroup getGroup() {
         return group;
     }
 
     public String getDefaultFileName() {
         return "options.xml";
     }
 
     /**
      * This function analyses an event and calls the right methods to take
      * care of the user's requests.
      * @param event The incoming ActionEvent.
      */
     public void actionPerformed(ActionEvent event) {
         String command = event.getActionCommand();
         if (OK.equals(command)) {
             ui.updateOption();
             ui.unregister();
             getCanvas().remove(this);
             setResponse(group);
         } else if (CANCEL.equals(command)) {
             ui.rollback();
             ui.unregister();
             getCanvas().remove(this);
             setResponse(null);
         } else if (RESET.equals(command)) {
             ui.reset();
         } else if (SAVE.equals(command)) {
             File saveFile = getCanvas().showSaveDialog(FreeCol.getSaveDirectory(), ".xml", filters,
                                                        getDefaultFileName());
             if (saveFile != null) {
                 ui.updateOption();
                 group.save(saveFile);
             }
         } else if (LOAD.equals(command)) {
             File loadFile = getCanvas().showLoadDialog(FreeCol.getSaveDirectory(), filters);
             if (loadFile != null) {
                 try {
                     FileInputStream in = new FileInputStream(loadFile);
                     getGame().getSpecification().loadFragment(in);
                     in.close();
                 } catch(Exception e) {
                     logger.warning("Failed to load game options from " + loadFile.getName());
                 }
             }
         } else {
             logger.warning("Invalid ActionCommand: " + command);
         }
     }
 }
