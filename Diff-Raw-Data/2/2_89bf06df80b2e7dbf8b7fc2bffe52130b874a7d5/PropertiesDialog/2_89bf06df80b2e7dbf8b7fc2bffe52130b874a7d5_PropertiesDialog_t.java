 // License: GPL. See LICENSE file for details.
 
 package org.openstreetmap.josm.gui.dialogs;
 
 import static org.openstreetmap.josm.tools.I18n.marktr;
 import static org.openstreetmap.josm.tools.I18n.tr;
 import static org.openstreetmap.josm.tools.I18n.trn;
 
 import java.awt.BorderLayout;
 import java.awt.Component;
 import java.awt.Font;
 import java.awt.GridBagLayout;
 import java.awt.GridLayout;
 import java.awt.event.ActionEvent;
 import java.awt.event.ActionListener;
 import java.awt.event.FocusAdapter;
 import java.awt.event.FocusEvent;
 import java.awt.event.KeyEvent;
 import java.awt.event.MouseAdapter;
 import java.awt.event.MouseEvent;
 import java.lang.String;
 import java.util.Collection;
 import java.util.Collections;
 import java.util.HashMap;
 import java.util.HashSet;
 import java.util.LinkedList;
 import java.util.Map;
 import java.util.TreeMap;
 import java.util.TreeSet;
 import java.util.Vector;
 import java.util.Map.Entry;
 
 import javax.swing.Box;
 import javax.swing.DefaultComboBoxModel;
 import javax.swing.DefaultListCellRenderer;
 import javax.swing.JComboBox;
 import javax.swing.JDialog;
 import javax.swing.JLabel;
 import javax.swing.JList;
 import javax.swing.JOptionPane;
 import javax.swing.JPanel;
 import javax.swing.JScrollPane;
 import javax.swing.JTable;
 import javax.swing.ListSelectionModel;
 import javax.swing.table.DefaultTableCellRenderer;
 import javax.swing.table.DefaultTableModel;
 import javax.swing.text.JTextComponent;
 
 import org.openstreetmap.josm.Main;
 import org.openstreetmap.josm.command.ChangeCommand;
 import org.openstreetmap.josm.command.ChangePropertyCommand;
 import org.openstreetmap.josm.command.Command;
 import org.openstreetmap.josm.command.SequenceCommand;
 import org.openstreetmap.josm.data.SelectionChangedListener;
 import org.openstreetmap.josm.data.osm.DataSet;
 import org.openstreetmap.josm.data.osm.Node;
 import org.openstreetmap.josm.data.osm.OsmPrimitive;
 import org.openstreetmap.josm.data.osm.Relation;
 import org.openstreetmap.josm.data.osm.RelationMember;
 import org.openstreetmap.josm.data.osm.Way;
 import org.openstreetmap.josm.data.osm.visitor.NameVisitor;
 import org.openstreetmap.josm.gui.ExtendedDialog;
 import org.openstreetmap.josm.gui.JMultilineLabel;
 import org.openstreetmap.josm.gui.MapFrame;
 import org.openstreetmap.josm.gui.SideButton;
 import org.openstreetmap.josm.gui.preferences.TaggingPresetPreference;
 import org.openstreetmap.josm.gui.tagging.TaggingPreset;
 import org.openstreetmap.josm.tools.AutoCompleteComboBox;
 import org.openstreetmap.josm.tools.GBC;
 import org.openstreetmap.josm.tools.Shortcut;
 
 /**
  * This dialog displays the properties of the current selected primitives.
  *
  * If no object is selected, the dialog list is empty.
  * If only one is selected, all properties of this object are selected.
  * If more than one object are selected, the sum of all properties are displayed. If the
  * different objects share the same property, the shared value is displayed. If they have
  * different values, all of them are put in a combo box and the string "&lt;different&gt;"
  * is displayed in italic.
  *
  * Below the list, the user can click on an add, modify and delete property button to
  * edit the table selection value.
  *
  * The command is applied to all selected entries.
  *
  * @author imi
  */
 public class PropertiesDialog extends ToggleDialog implements SelectionChangedListener {
 
     /**
      * Used to display relation names in the membership table
      */
     private NameVisitor nameVisitor = new NameVisitor();
 
     /**
      * Watches for double clicks and from editing or new property, depending on the
      * location, the click was.
      * @author imi
      */
     public class DblClickWatch extends MouseAdapter {
         @Override public void mouseClicked(MouseEvent e) {
             if (e.getClickCount() < 2)
             {
                 if (e.getSource() == propertyTable)
                     membershipTable.clearSelection();
                 else if (e.getSource() == membershipTable)
                     propertyTable.clearSelection();
             }
             else if (e.getSource() == propertyTable)
             {
                 int row = propertyTable.rowAtPoint(e.getPoint());
                 if (row > -1)
                     propertyEdit(row);
             } else if (e.getSource() == membershipTable) {
                 int row = membershipTable.rowAtPoint(e.getPoint());
                 if (row > -1)
                     membershipEdit(row);
             }
             else
             {
                 add();
             }
         }
     }
 
     private final Map<String, Map<String, Integer>> valueCount = new TreeMap<String, Map<String, Integer>>();
     /**
      * Edit the value in the properties table row
      * @param row The row of the table from which the value is edited.
      */
     void propertyEdit(int row) {
         Collection<OsmPrimitive> sel = Main.ds.getSelected();
         if (sel.isEmpty()) return;
 
         String key = propertyData.getValueAt(row, 0).toString();
         objKey=key;
 
         String msg = "<html>"+trn("This will change up to {0} object.", "This will change up to {0} objects.", sel.size(), sel.size())+"<br><br>("+tr("An empty value deletes the key.", key)+")</html>";
 
         JPanel panel = new JPanel(new BorderLayout());
         panel.add(new JLabel(msg), BorderLayout.NORTH);
 
         final TreeMap<String, TreeSet<String>> allData = createAutoCompletionInfo(true);
 
         JPanel p = new JPanel(new GridBagLayout());
         panel.add(p, BorderLayout.CENTER);
 
         final AutoCompleteComboBox keys = new AutoCompleteComboBox();
         keys.setPossibleItems(allData.keySet());
         keys.setEditable(true);
         keys.setSelectedItem(key);
 
         p.add(new JLabel(tr("Key")), GBC.std());
         p.add(Box.createHorizontalStrut(10), GBC.std());
         p.add(keys, GBC.eol().fill(GBC.HORIZONTAL));
 
         final AutoCompleteComboBox values = new AutoCompleteComboBox();
         values.setRenderer(new DefaultListCellRenderer() {
             @Override public Component getListCellRendererComponent(JList list,  Object value, int index, boolean isSelected,  boolean cellHasFocus) {
                 Component c = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                 if (c instanceof JLabel) {
                     String str = null;
                         str=(String) value;
                         if (valueCount.containsKey(objKey)){
                             Map<String, Integer> m=valueCount.get(objKey);
                             if (m.containsKey(str)) {
                                 str+="("+m.get(str)+")";
                                 c.setFont(c.getFont().deriveFont(Font.ITALIC+Font.BOLD));
                             }
                         }
                     ((JLabel)c).setText(str);
                 }
                 return c;
             }
         });
         values.setEditable(true);
         updateListData(key, allData, values);
         Map<String, Integer> m=(Map<String, Integer>)propertyData.getValueAt(row, 1);
         final String selection= m.size()!=1?tr("<different>"):m.entrySet().iterator().next().getKey();
         values.setSelectedItem(selection);
         values.getEditor().setItem(selection);
         p.add(new JLabel(tr("Value")), GBC.std());
         p.add(Box.createHorizontalStrut(10), GBC.std());
         p.add(values, GBC.eol().fill(GBC.HORIZONTAL));
         addFocusAdapter(row, allData, keys, values);
 
         final JOptionPane optionPane = new JOptionPane(panel, JOptionPane.QUESTION_MESSAGE, JOptionPane.OK_CANCEL_OPTION) {
             @Override public void selectInitialValue() {
                 values.requestFocusInWindow();
                 values.getEditor().selectAll();
             }
         };
         final JDialog dlg = optionPane.createDialog(Main.parent, tr("Change values?"));
 
         values.getEditor().addActionListener(new ActionListener() {
             public void actionPerformed(ActionEvent e) {
                 dlg.setVisible(false);
                 optionPane.setValue(JOptionPane.OK_OPTION);
             }
         });
 
         String oldValue = values.getEditor().getItem().toString();
         dlg.setVisible(true);
 
         Object answer = optionPane.getValue();
         if (answer == null || answer == JOptionPane.UNINITIALIZED_VALUE ||
                 (answer instanceof Integer && (Integer)answer != JOptionPane.OK_OPTION)) {
             values.getEditor().setItem(oldValue);
             return;
         }
 
         String value = values.getEditor().getItem().toString().trim();
         // is not Java 1.5
         //value = java.text.Normalizer.normalize(value, java.text.Normalizer.Form.NFC);
         if (value.equals(""))
             value = null; // delete the key
         String newkey = keys.getEditor().getItem().toString().trim();
         //newkey = java.text.Normalizer.normalize(newkey, java.text.Normalizer.Form.NFC);
         if (newkey.equals("")) {
             newkey = key;
             value = null; // delete the key instead
         }
         if (newkey.equals("created_by"))
         {
             // we don't allow created_by to be changed.
             return;
         }
         if (key.equals(newkey) || value == null)
             Main.main.undoRedo.add(new ChangePropertyCommand(sel, newkey, value));
         else {
             Collection<Command> commands=new Vector<Command>();
             commands.add(new ChangePropertyCommand(sel, key, null));
             if (value.equals(tr("<different>"))) {
                 HashMap<String, Vector<OsmPrimitive>> map=new HashMap<String, Vector<OsmPrimitive>>();
                 for (OsmPrimitive osm: sel) {
                     if(osm.keys != null)
                     {
                         String val=osm.keys.get(key);
                         if(val != null)
                         {
                             if (map.containsKey(val)) {
                                 map.get(val).add(osm);
                             } else {
                                 Vector<OsmPrimitive> v = new Vector<OsmPrimitive>();
                                 v.add(osm);
                                 map.put(val, v);
                             }
                         }
                     }
                 }
                 for (Entry<String, Vector<OsmPrimitive>> e: map.entrySet()) {
                     commands.add(new ChangePropertyCommand(e.getValue(), newkey, e.getKey()));
                 }
             } else {
                 commands.add(new ChangePropertyCommand(sel, newkey, value));
             }
             Main.main.undoRedo.add(new SequenceCommand(trn("Change properties of up to {0} object", "Change properties of up to {0} objects", sel.size(), sel.size()), commands));
         }
 
         Main.ds.fireSelectionChanged(sel);
         selectionChanged(sel); // update whole table
         Main.parent.repaint(); // repaint all - drawing could have been changed
 
         if(!key.equals(newkey)) {
             for(int i=0; i < propertyTable.getRowCount(); i++)
                 if(propertyData.getValueAt(i, 0).toString() == newkey) {
                     row=i;
                     break;
                 }
         }
         propertyTable.changeSelection(row, 0, false, false);
     }
 
     /**
      * @param key
      * @param allData
      * @param values
      */
     private void updateListData(String key, final TreeMap<String, TreeSet<String>> allData, final AutoCompleteComboBox values) {
         Collection<String> newItems;
         if (allData.containsKey(key)) {
             newItems = allData.get(key);
         } else {
             newItems = Collections.emptyList();
         }
         values.setPossibleItems(newItems);
     }
 
     /**
      * This simply fires up an relation editor for the relation shown; everything else
      * is the editor's business.
      *
      * @param row
      */
     void membershipEdit(int row) {
         final RelationEditor editor = new RelationEditor((Relation)membershipData.getValueAt(row, 0),
                 (Collection<RelationMember>) membershipData.getValueAt(row, 1) );
         editor.setVisible(true);
     }
 
     /**
      * Open the add selection dialog and add a new key/value to the table (and
      * to the dataset, of course).
      */
     void add() {
         Collection<OsmPrimitive> sel = Main.ds.getSelected();
         if (sel.isEmpty()) return;
 
         JPanel p = new JPanel(new BorderLayout());
         p.add(new JLabel("<html>"+trn("This will change up to {0} object.","This will change up to {0} objects.", sel.size(),sel.size())+"<br><br>"+tr("Please select a key")),
                 BorderLayout.NORTH);
         final TreeMap<String, TreeSet<String>> allData = createAutoCompletionInfo(false);
         final AutoCompleteComboBox keys = new AutoCompleteComboBox();
         keys.setPossibleItems(allData.keySet());
         keys.setEditable(true);
 
         p.add(keys, BorderLayout.CENTER);
 
         JPanel p2 = new JPanel(new BorderLayout());
         p.add(p2, BorderLayout.SOUTH);
         p2.add(new JLabel(tr("Please select a value")), BorderLayout.NORTH);
         final AutoCompleteComboBox values = new AutoCompleteComboBox();
         values.setEditable(true);
         p2.add(values, BorderLayout.CENTER);
 
         addFocusAdapter(-1, allData, keys, values);
         JOptionPane pane = new JOptionPane(p, JOptionPane.PLAIN_MESSAGE, JOptionPane.OK_CANCEL_OPTION){
             @Override public void selectInitialValue() {
                 keys.requestFocusInWindow();
                 keys.getEditor().selectAll();
             }
         };
         pane.createDialog(Main.parent, tr("Change values?")).setVisible(true);
         if (!Integer.valueOf(JOptionPane.OK_OPTION).equals(pane.getValue()))
             return;
         String key = keys.getEditor().getItem().toString().trim();
         String value = values.getEditor().getItem().toString().trim();
         if (value.equals(""))
             return;
         if (key.equals("created_by"))
             return;
         Main.main.undoRedo.add(new ChangePropertyCommand(sel, key, value));
         Main.ds.fireSelectionChanged(sel);
         selectionChanged(sel); // update table
         Main.parent.repaint(); // repaint all - drawing could have been changed
     }
 
     /**
      * @param allData
      * @param keys
      * @param values
      */
     private void addFocusAdapter(final int row, final TreeMap<String, TreeSet<String>> allData,final AutoCompleteComboBox keys, final AutoCompleteComboBox values) {
         // get the combo box' editor component
         JTextComponent editor = (JTextComponent)values.getEditor()
                 .getEditorComponent();
         // Refresh the values model when focus is gained
         editor.addFocusListener(new FocusAdapter() {
             @Override public void focusGained(FocusEvent e) {
                 String key = keys.getEditor().getItem().toString();
                 updateListData(key, allData, values);
                 objKey=key;
             }
         });
     }
     private String objKey;
     /**
      * @return
      */
     private TreeMap<String, TreeSet<String>> createAutoCompletionInfo(
             boolean edit) {
         final TreeMap<String, TreeSet<String>> allData = new TreeMap<String, TreeSet<String>>();
         for (OsmPrimitive osm : Main.ds.allNonDeletedPrimitives()) {
             for (String key : osm.keySet()) {
                 TreeSet<String> values = null;
                 if (allData.containsKey(key))
                     values = allData.get(key);
                 else {
                     values = new TreeSet<String>();
                     allData.put(key, values);
                 }
                 values.add(osm.get(key));
             }
         }
         if (!edit) {
             for (int i = 0; i < propertyData.getRowCount(); ++i)
                 allData.remove(propertyData.getValueAt(i, 0));
         }
         return allData;
     }
 
     /**
      * Delete the keys from the given row.
      * @param row   The row, which key gets deleted from the dataset.
      */
     private void delete(int row) {
         String key = propertyData.getValueAt(row, 0).toString();
         Collection<OsmPrimitive> sel = Main.ds.getSelected();
         Main.main.undoRedo.add(new ChangePropertyCommand(sel, key, null));
         Main.ds.fireSelectionChanged(sel);
         selectionChanged(sel); // update table
 
         int rowCount = propertyTable.getRowCount();
         propertyTable.changeSelection((row < rowCount ? row : (rowCount-1)), 0, false, false);
     }
 
     /**
      * The property data.
      */
     private final DefaultTableModel propertyData = new DefaultTableModel() {
         @Override public boolean isCellEditable(int row, int column) {
             return false;
         }
         @Override public Class<?> getColumnClass(int columnIndex) {
             return String.class;
         }
     };
 
     /**
      * The membership data.
      */
     private final DefaultTableModel membershipData = new DefaultTableModel() {
         @Override public boolean isCellEditable(int row, int column) {
             return false;
         }
         @Override public Class<?> getColumnClass(int columnIndex) {
             return String.class;
         }
     };
 
     /**
      * The properties list.
      */
     private final JTable propertyTable = new JTable(propertyData);
     private final JTable membershipTable = new JTable(membershipData);
 
     public JComboBox taggingPresets = new JComboBox();
 
     /**
      * The Add/Edit/Delete buttons (needed to be able to disable them)
      */
     private final SideButton btnAdd;
     private final SideButton btnEdit;
     private final SideButton btnDel;
     private final JMultilineLabel presets = new JMultilineLabel("");
 
     private final JLabel selectSth = new JLabel("<html><p>" + tr("Please select the objects you want to change properties for.") + "</p></html>");
 
     /**
      * Create a new PropertiesDialog
      */
     public PropertiesDialog(MapFrame mapFrame) {
         super(tr("Properties/Memberships"), "propertiesdialog", tr("Properties for selected objects."),
         Shortcut.registerShortcut("subwindow:properties", tr("Toggle: {0}", tr("Properties/Memberships")), KeyEvent.VK_P,
         Shortcut.GROUP_LAYER, Shortcut.SHIFT_DEFAULT), 150);
 
         // setting up the properties table
         propertyData.setColumnIdentifiers(new String[]{tr("Key"),tr("Value")});
         propertyTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
 
         propertyTable.getColumnModel().getColumn(1).setCellRenderer(new DefaultTableCellRenderer(){
             @Override public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                 Component c = super.getTableCellRendererComponent(table, value, isSelected, false, row, column);
                 if (c instanceof JLabel) {
                     String str = null;
                     switch (column) {
                     case 0:
                         str = (String) value;
                     break;
                     case 1:
                         Map<String, Integer> v = (Map<String,Integer>) value;
                         if (v.size()!=1) {
                             str=tr("<different>");
                             c.setFont(c.getFont().deriveFont(Font.ITALIC));
                         } else {
                             str=v.entrySet().iterator().next().getKey();
                         }
                     break;
                     }
                     ((JLabel)c).setText(str);
                 }
                 return c;
             }
         });
 
         // setting up the membership table
 
         membershipData.setColumnIdentifiers(new String[]{tr("Member Of"),tr("Role")});
         membershipTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
 
         membershipTable.getColumnModel().getColumn(0).setCellRenderer(new DefaultTableCellRenderer() {
             @Override public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                 Component c = super.getTableCellRendererComponent(table, value, isSelected, false, row, column);
                 if (c instanceof JLabel) {
                     nameVisitor.visit((Relation)value);
                     ((JLabel)c).setText(nameVisitor.name);
                 }
                 return c;
             }
         });
 
         membershipTable.getColumnModel().getColumn(1).setCellRenderer(new DefaultTableCellRenderer() {
             @Override public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                 Component c = super.getTableCellRendererComponent(table, value, isSelected, false, row, column);
                 if (c instanceof JLabel) {
                     Collection<RelationMember> col = (Collection<RelationMember>) value;
 
                     String text = null;
                     for (RelationMember r : col) {
                         if (text == null) {
                             text = r.role;
                         }
                         else if (!text.equals(r.role)) {
                             text = tr("<different>");
                             break;
                         }
                     }
 
                     ((JLabel)c).setText(text);
                 }
                 return c;
             }
         });
 
         // combine both tables and wrap them in a scrollPane
         JPanel bothTables = new JPanel();
         bothTables.setLayout(new GridBagLayout());
         bothTables.add(selectSth, GBC.eol().fill().insets(10, 10, 10, 10));
         bothTables.add(propertyTable.getTableHeader(), GBC.eol().fill(GBC.HORIZONTAL));
         bothTables.add(propertyTable, GBC.eol().fill(GBC.BOTH));
         bothTables.add(membershipTable.getTableHeader(), GBC.eol().fill(GBC.HORIZONTAL));
         bothTables.add(membershipTable, GBC.eol().fill(GBC.BOTH));
         bothTables.add(presets, GBC.eol().fill().insets(5, 2, 5, 2));
 
         DblClickWatch dblClickWatch = new DblClickWatch();
         propertyTable.addMouseListener(dblClickWatch);
         membershipTable.addMouseListener(dblClickWatch);
         JScrollPane scrollPane = new JScrollPane(bothTables);
         scrollPane.addMouseListener(dblClickWatch);
         add(scrollPane, BorderLayout.CENTER);
 
         selectSth.setPreferredSize(scrollPane.getSize());
         presets.setPreferredSize(scrollPane.getSize());
 
         JPanel buttonPanel = new JPanel(new GridLayout(1,3));
         ActionListener buttonAction = new ActionListener(){
             public void actionPerformed(ActionEvent e) {
                 int row = membershipTable.getSelectedRow();
                 if (e.getActionCommand().equals("Add"))
                     add();
                 else if(row >= 0)
                 {
                     if (e.getActionCommand().equals("Edit"))
                         membershipEdit(row);
                     else if (e.getActionCommand().equals("Delete")) {
                         Relation cur = (Relation)membershipData.getValueAt(row, 0);
                         NameVisitor n = new NameVisitor();
                         cur.visit(n);
 
                         int result = new ExtendedDialog(Main.parent,
                             tr("Change relation"),
                             tr("Really delete selection from relation {0}?", n.name),
                             new String[] {tr("Delete from relation"), tr("Cancel")},
                             new String[] {"dialogs/delete.png", "cancel.png"}).getValue();
 
                         if(result == 1)
                         {
                             Relation rel = new Relation(cur);
                             Collection<OsmPrimitive> sel = Main.ds.getSelected();
                             for (RelationMember rm : cur.members) {
                                 for (OsmPrimitive osm : sel) {
                                     if (rm.member == osm)
                                     {
                                         RelationMember mem = new RelationMember();
                                         mem.role = rm.role;
                                         mem.member = rm.member;
                                         rel.members.remove(mem);
                                         break;
                                     }
                                 }
                             }
                             Main.main.undoRedo.add(new ChangeCommand(cur, rel));
                             Main.ds.fireSelectionChanged(sel);
                             selectionChanged(sel); // update whole table
                         }
 
                     }
                 }
                 else
                 {
                     int sel = propertyTable.getSelectedRow();
                     // Although we might edit/delete the wrong tag here, chances are still better
                     // than just displaying an error message (which always "fails").
                     if (e.getActionCommand().equals("Edit"))
                         propertyEdit(sel >= 0 ? sel : 0);
                     else if (e.getActionCommand().equals("Delete"))
                         delete(sel >= 0 ? sel : 0);
                 }
             }
         };
 
         Shortcut s = Shortcut.registerShortcut("properties:add", tr("Add Properties"), KeyEvent.VK_B,
         Shortcut.GROUP_MNEMONIC);
         this.btnAdd = new SideButton(marktr("Add"),"add","Properties",
                 tr("Add a new key/value pair to all objects"), s, buttonAction);
         buttonPanel.add(this.btnAdd);
 
         s = Shortcut.registerShortcut("properties:edit", tr("Edit Properties"), KeyEvent.VK_I,
         Shortcut.GROUP_MNEMONIC);
         this.btnEdit = new SideButton(marktr("Edit"),"edit","Properties",
                 tr("Edit the value of the selected key for all objects"), s, buttonAction);
         buttonPanel.add(this.btnEdit);
 
         s = Shortcut.registerShortcut("properties:delete", tr("Delete Properties"), KeyEvent.VK_Q,
         Shortcut.GROUP_MNEMONIC);
         this.btnDel = new SideButton(marktr("Delete"),"delete","Properties",
                 tr("Delete the selected key in all objects"), s, buttonAction);
         buttonPanel.add(this.btnDel);
         add(buttonPanel, BorderLayout.SOUTH);
 
         DataSet.selListeners.add(this);
     }
 
     @Override public void setVisible(boolean b) {
         super.setVisible(b);
         if (b)
             selectionChanged(Main.ds.getSelected());
     }
 
     private void checkPresets(int nodes, int ways, int relations, int closedways)
     {
         LinkedList<TaggingPreset> p = new LinkedList<TaggingPreset>();
         int total = nodes+ways+relations+closedways;
         if(total != 0)
         {
             for(TaggingPreset t : TaggingPresetPreference.taggingPresets)
             {
                if(t.types == null || !((relations > 0 && !t.types.contains("relation")) &&
                 (nodes > 0 && !t.types.contains("node")) &&
                 (ways+closedways > 0 && !t.types.contains("way")) &&
                 (closedways > 0 && !t.types.contains("closedway"))))
                 {
                     int found = 0;
                     for(TaggingPreset.Item i : t.data)
                     {
                         if(i instanceof TaggingPreset.Key)
                         {
                             String val = ((TaggingPreset.Key)i).value;
                             String key = ((TaggingPreset.Key)i).key;
                             // we subtract 100 if not found and add 1 if found
                             found -= 100;
                             if(valueCount.containsKey(key))
                             {
                                 Map<String, Integer> v = valueCount.get(key);
                                 if(v.size() == 1 && v.containsKey(val) && v.get(val) == total)
                                 {
                                     found += 101;
                                 }
                             }
                         }
                     }
                     if(found > 0)
                         p.add(t);
                 }
             }
         }
         String t = "";
         for(TaggingPreset tp : p)
         {
             if(t.length() > 0)
                 t += "\n";
             t += tp.getName();
         }
         presets.setText(t);
         presets.setVisible(t.length() > 0);
     }
 
     public void selectionChanged(Collection<? extends OsmPrimitive> newSelection) {
         if (!isVisible())
             return;
         if (propertyTable == null)
             return; // selection changed may be received in base class constructor before init
         if (propertyTable.getCellEditor() != null)
             propertyTable.getCellEditor().cancelCellEditing();
 
         // re-load property data
         propertyData.setRowCount(0);
         int nodes = 0;
         int ways = 0;
         int relations = 0;
         int closedways = 0;
 
         Map<String, Integer> keyCount = new HashMap<String, Integer>();
         valueCount.clear();
         for (OsmPrimitive osm : newSelection) {
             if(osm instanceof Node) ++nodes;
             else if(osm instanceof Relation) ++relations;
             else if(((Way)osm).isClosed()) ++closedways;
             else ++ways;
             for (Entry<String, String> e : osm.entrySet()) {
                 keyCount.put(e.getKey(), keyCount.containsKey(e.getKey()) ? keyCount.get(e.getKey())+1 : 1);
                 if (valueCount.containsKey(e.getKey())) {
                     Map<String, Integer> v = valueCount.get(e.getKey());
                     v.put(e.getValue(), v.containsKey(e.getValue())? v.get(e.getValue())+1 : 1 );
                 } else {
                     TreeMap<String,Integer> v = new TreeMap<String, Integer>();
                     v.put(e.getValue(), 1);
                     valueCount.put(e.getKey(), v);
                 }
             }
         }
         for (Entry<String, Map<String, Integer>> e : valueCount.entrySet()) {
             int count=0;
             for (Entry<String, Integer> e1: e.getValue().entrySet()) {
                 count+=e1.getValue();
             }
             if (count < newSelection.size()) {
                 e.getValue().put("", newSelection.size()-count);
             }
             propertyData.addRow(new Object[]{e.getKey(), e.getValue()});
         }
 
         boolean hasTags = !newSelection.isEmpty() && propertyData.getRowCount() > 0;
         boolean hasSelection = !newSelection.isEmpty();
         btnAdd.setEnabled(hasSelection);
         btnEdit.setEnabled(hasTags);
         btnDel.setEnabled(hasTags);
         propertyTable.setVisible(hasSelection);
         propertyTable.getTableHeader().setVisible(hasSelection);
         selectSth.setVisible(!hasSelection);
         if(hasTags) propertyTable.changeSelection(0, 0, false, false);
 
         checkPresets(nodes, ways, relations, closedways);
 
         // re-load membership data
         // this is rather expensive since we have to walk through all members of all existing relationships.
         // could use back references here for speed if necessary.
 
         membershipData.setRowCount(0);
 
         Map<Relation, Collection<RelationMember>> roles = new HashMap<Relation, Collection<RelationMember>>();
         for (Relation r : Main.ds.relations) {
             if (!r.deleted && !r.incomplete) {
                 for (RelationMember m : r.members) {
                     if (newSelection.contains(m.member)) {
                         Collection<RelationMember> value = roles.get(r);
                         if (value == null) {
                             value = new HashSet<RelationMember>();
                             roles.put(r, value);
                         }
                         value.add(m);
                     }
                 }
             }
         }
 
         for (Entry<Relation, Collection<RelationMember>> e : roles.entrySet()) {
             membershipData.addRow(new Object[]{e.getKey(), e.getValue()});
         }
 
         membershipTable.getTableHeader().setVisible(membershipData.getRowCount() > 0);
         membershipTable.setVisible(membershipData.getRowCount() > 0);
 
         if(propertyData.getRowCount() != 0 || membershipData.getRowCount() != 0) {
             setTitle(tr("Properties: {0} / Memberships: {1}",
                 propertyData.getRowCount(), membershipData.getRowCount()), true);
         } else {
             setTitle(tr("Properties / Memberships"), false);
         }
     }
 }
