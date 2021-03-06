 // License: GPL. Copyright 2007 by Immanuel Scholz and others
 package org.openstreetmap.josm.gui.tagging;
 
 import static org.openstreetmap.josm.tools.I18n.tr;
 import static org.openstreetmap.josm.tools.I18n.trn;
 
 import java.awt.GridBagLayout;
 import java.awt.Image;
 import java.awt.event.ActionEvent;
 import java.io.BufferedReader;
 import java.io.FileInputStream;
 import java.io.IOException;
 import java.io.InputStream;
 import java.io.InputStreamReader;
 import java.io.UnsupportedEncodingException;
 import java.net.URL;
 import java.util.Collection;
 import java.util.HashMap;
 import java.util.HashSet;
 import java.util.LinkedHashMap;
 import java.util.LinkedList;
 import java.util.List;
 import java.util.Set;
 import java.util.StringTokenizer;
 
 import javax.swing.AbstractAction;
 import javax.swing.Action;
 import javax.swing.ImageIcon;
 import javax.swing.JComboBox;
 import javax.swing.JComponent;
 import javax.swing.JLabel;
 import javax.swing.JOptionPane;
 import javax.swing.JPanel;
 import javax.swing.JTextField;
 
 import org.openstreetmap.josm.Main;
 import org.openstreetmap.josm.command.ChangePropertyCommand;
 import org.openstreetmap.josm.command.Command;
 import org.openstreetmap.josm.command.SequenceCommand;
 import org.openstreetmap.josm.data.osm.OsmPrimitive;
 import org.openstreetmap.josm.gui.QuadStateCheckBox;
 import org.openstreetmap.josm.tools.GBC;
 import org.openstreetmap.josm.tools.ImageProvider;
 import org.openstreetmap.josm.tools.XmlObjectParser;
 import org.xml.sax.SAXException;
 
 /**
  * This class read encapsulate one tagging preset. A class method can
  * read in all predefined presets, either shipped with JOSM or that are
  * in the config directory.
  * 
  * It is also able to construct dialogs out of preset definitions.
  */
 public class TaggingPreset extends AbstractAction {
 	
 	public static abstract class Item {
 		public boolean focus = false;
 		abstract void addToPanel(JPanel p, Collection<OsmPrimitive> sel);
 		abstract void addCommands(Collection<OsmPrimitive> sel, List<Command> cmds);
 		boolean requestFocusInWindow() {return false;}
 	}
 	
 	public static class Usage {
 		Set<String> values;
 	}
 	
 	public static final String DIFFERENT = tr("<different>");
 	
 	static Usage determineTextUsage(Collection<OsmPrimitive> sel, String key) {
 		Usage returnValue = new Usage();
 		returnValue.values = new HashSet<String>();
 		for (OsmPrimitive s : sel) {
 			String v = s.get(key);
 			/* if (v != null) */ returnValue.values.add(v);
 		}
 		return returnValue;
 	}
 
 	static Usage determineBooleanUsage(Collection<OsmPrimitive> sel, String key) {
 
 		Usage returnValue = new Usage();
 		returnValue.values = new HashSet<String>();
 		for (OsmPrimitive s : sel) {
 			String v = s.get(key);
 			if ("true".equalsIgnoreCase(v)) v = "true";
 			else if ("yes".equalsIgnoreCase(v)) v = "true";
 			else if ("1".equals(v)) v = "true";
 			else if ("false".equalsIgnoreCase(v)) v = "false";
 			else if ("no".equalsIgnoreCase(v)) v = "false";
 			else if ("0".equals(v)) v = "false";			
 			returnValue.values.add(v);
 		}
 		return returnValue;
 	}
 	
 	public static class Text extends Item {
 		
 		public String key;
 		public String text;
 		public String locale_text;
 		public String default_;
 		public String originalValue;
 		public boolean use_last_as_default = false;
 		public boolean delete_if_empty = false;
 
 		private JComponent value;
 		
 		@Override public void addToPanel(JPanel p, Collection<OsmPrimitive> sel) {
 			
 			// find out if our key is already used in the selection.
 			Usage usage = determineTextUsage(sel, key);
 			if (usage.values.size() == 1 && usage.values.toArray()[0] == null) {
 				value = new JTextField();
 				if (use_last_as_default && lastValue.containsKey(key)) {
 					((JTextField)value).setText(lastValue.get(key));
 				} else {
 					((JTextField)value).setText(default_);
 				}
 			} else if (usage.values.size() == 1) {
 				// all objects use the same value
 				value = new JTextField();
 				for (String s : usage.values) ((JTextField) value).setText(s);
 				originalValue = ((JTextField)value).getText();
 			} else {
 				// the objects have different values
 				value = new JComboBox(usage.values.toArray());
 				((JComboBox)value).setEditable(true);
 	            ((JComboBox)value).getEditor().setItem(DIFFERENT);
 	            originalValue = DIFFERENT;
 			}
 			if(locale_text == null)
 				locale_text = tr(text);
 			p.add(new JLabel(locale_text+":"), GBC.std().insets(0,0,10,0));
 			p.add(value, GBC.eol().fill(GBC.HORIZONTAL));
 		}
 		
 		@Override public void addCommands(Collection<OsmPrimitive> sel, List<Command> cmds) {
 			
 			// return if unchanged
 			String v = (value instanceof JComboBox) ? 
 				((JComboBox)value).getEditor().getItem().toString() : 
 				((JTextField)value).getText();
 
 			if (use_last_as_default) lastValue.put(key, v);
 			if (v.equals(originalValue) || (originalValue == null && v.length() == 0)) return;
 
 			if (delete_if_empty && v.length() == 0)
 				v = null;
 			cmds.add(new ChangePropertyCommand(sel, key, v));
 		}
 		@Override boolean requestFocusInWindow() {return value.requestFocusInWindow();}
 	}
 
 	public static class Check extends Item {
 
 		public String key;
 		public String text;
 		public String locale_text;
 		public boolean default_ = false; // not used!
 		public boolean use_last_as_default = false;
 
 		private QuadStateCheckBox check;
 		private QuadStateCheckBox.State initialState;
 		
 		@Override public void addToPanel(JPanel p, Collection<OsmPrimitive> sel) {
 			
 			// find out if our key is already used in the selection.
 			Usage usage = determineBooleanUsage(sel, key);
 
 			if(locale_text == null)
 				locale_text = tr(text);
 
 			String oneValue = null;
 			for (String s : usage.values) oneValue = s;
 			if (usage.values.size() < 2 && (oneValue == null || "true".equals(oneValue) || "false".equals(oneValue))) {
 				// all selected objects share the same value which is either true or false or unset, 
 				// we can display a standard check box.
 				initialState = "true".equals(oneValue) ? 
 							QuadStateCheckBox.State.SELECTED :
 							"false".equals(oneValue) ? 
 							QuadStateCheckBox.State.NOT_SELECTED :
 							QuadStateCheckBox.State.UNSET;
 				check = new QuadStateCheckBox(locale_text, initialState, 
 						new QuadStateCheckBox.State[] { 
 						QuadStateCheckBox.State.SELECTED,
 						QuadStateCheckBox.State.NOT_SELECTED,
 						QuadStateCheckBox.State.UNSET });
 			} else {
 				// the objects have different values, or one or more objects have something
 				// else than true/false. we display a quad-state check box
 				// in "partial" state.
 				initialState = QuadStateCheckBox.State.PARTIAL;
 				check = new QuadStateCheckBox(locale_text, QuadStateCheckBox.State.PARTIAL, 
 						new QuadStateCheckBox.State[] { 
 						QuadStateCheckBox.State.PARTIAL,
 						QuadStateCheckBox.State.SELECTED,
 						QuadStateCheckBox.State.NOT_SELECTED,
 						QuadStateCheckBox.State.UNSET });
 			}
 			p.add(check, GBC.eol().fill(GBC.HORIZONTAL));
 		}
 		
 		@Override public void addCommands(Collection<OsmPrimitive> sel, List<Command> cmds) {
 			// if the user hasn't changed anything, don't create a command.
 			if (check.getState() == initialState) return;
 			
 			// otherwise change things according to the selected value.
 			cmds.add(new ChangePropertyCommand(sel, key, 
 					check.getState() == QuadStateCheckBox.State.SELECTED ? "true" :
 					check.getState() == QuadStateCheckBox.State.NOT_SELECTED ? "false" :
 					null));
 		}
 		@Override boolean requestFocusInWindow() {return check.requestFocusInWindow();}
 	}
 
 	public static class Combo extends Item {
 		
 		public String key;
 		public String text;
 		public String locale_text;
 		public String values;
 		public String display_values;
 		public String locale_display_values;
 		public String default_;
 		public boolean delete_if_empty = false;
 		public boolean editable = true;
 		public boolean use_last_as_default = false;
 
 		private JComboBox combo;
 		private LinkedHashMap<String,String> lhm;
 		private Usage usage;
 		private String originalValue;
 		
 		@Override public void addToPanel(JPanel p, Collection<OsmPrimitive> sel) {
 			
 			// find out if our key is already used in the selection.
 			usage = determineTextUsage(sel, key);
 			
 			String[] value_array = values.split(",");
 			String[] display_array;
 			if(locale_display_values != null)
 				display_array = locale_display_values.split(",");
 			else if(display_values != null)
 				display_array = display_values.split(",");
 			else
 				display_array = value_array;
 
 			lhm = new LinkedHashMap<String,String>();
 			if (usage.values.size() > 1) {
 				lhm.put(DIFFERENT, DIFFERENT);
 			}
 			for (int i=0; i<value_array.length; i++) {
 				lhm.put(value_array[i],
 				(locale_display_values == null) ?
 				tr(display_array[i]) : display_array[i]);
 			}
 			for (String s : usage.values) {
 				if (!lhm.containsKey(s)) lhm.put(s, s);
 			}
 			if ((default_ != null) && (!lhm.containsKey(default_))) lhm.put(default_, default_);
 			
 			combo = new JComboBox(lhm.values().toArray());
 			combo.setEditable(editable);
 			if (usage.values.size() == 1) {
 				for (String s : usage.values) { combo.setSelectedItem(lhm.get(s)); originalValue=s; }
 			} else {
 				combo.setSelectedItem(DIFFERENT); originalValue=DIFFERENT;
 			}
 			if(locale_text == null)
 				locale_text = tr(text);
 			p.add(new JLabel(locale_text+":"), GBC.std().insets(0,0,10,0));
 			p.add(combo, GBC.eol().fill(GBC.HORIZONTAL));
 		}
 		@Override public void addCommands(Collection<OsmPrimitive> sel, List<Command> cmds) {
			String display = combo.getSelectedItem().toString();
 			String value = null;
 			if(display == null && combo.isEditable())
 				display = combo.getEditor().getItem().toString();
 
 			if (display != null)
 			{
 				for (String key : lhm.keySet()) {
 					String k = lhm.get(key);
 					if (k != null && k.equals(display)) value=key;
 				}
 				if(value == null)
 					value = display;
 			}
 
 			// no change if same as before
 			if (value.equals(originalValue) || (originalValue == null && (value == null || value.length() == 0))) return;
 			
 			if (delete_if_empty && value != null && value.length() == 0)
 				value = null;
 			cmds.add(new ChangePropertyCommand(sel, key, value));
 		}
 		@Override boolean requestFocusInWindow() {return combo.requestFocusInWindow();}
 	}
 
 	public static class Label extends Item {
 		public String text;
 		public String locale_text;
 
 		@Override public void addToPanel(JPanel p, Collection<OsmPrimitive> sel) {
 			if(locale_text == null)
 				locale_text = tr(text);
 			p.add(new JLabel(locale_text), GBC.eol());
 		}
 		@Override public void addCommands(Collection<OsmPrimitive> sel, List<Command> cmds) {}
 	}
 
 	public static class Key extends Item {
 		public String key;
 		public String value;
 
 		@Override public void addToPanel(JPanel p, Collection<OsmPrimitive> sel) { }
 		@Override public void addCommands(Collection<OsmPrimitive> sel, List<Command> cmds) {
 			cmds.add(new ChangePropertyCommand(sel, key, value != null && !value.equals("") ? value : null));
 		}
 	}
 
 	/**
 	 * The types as preparsed collection.
 	 */
 	public Collection<Class<?>> types;
 	private List<Item> data = new LinkedList<Item>();
 	private static HashMap<String,String> lastValue = new HashMap<String,String>();
 
 	/**
 	 * Create an empty tagging preset. This will not have any items and
 	 * will be an empty string as text. createPanel will return null.
 	 * Use this as default item for "do not select anything".
 	 */
 	public TaggingPreset() {}
 
 
 	public boolean isEmpty()
 	{
 		return (data.size() == 0);
 	}
 	/**
 	 * Called from the XML parser to set the name of the tagging preset
 	 */
 	public void setName(String name) {
 		setDisplayName(tr(name));
 		putValue("toolbar", "tagging_"+name);
 	}
 	
 	/**
 	 * Change the display name without changing the toolbar value.
 	 */
 	public void setDisplayName(String name) {
 		putValue(Action.NAME, tr(name));
 	}
 
 	/**
 	 * Called from the XML parser to set the icon
 	 * 
 	 * FIXME for Java 1.6 - use 24x24 icons for LARGE_ICON_KEY (button bar)
 	 * and the 16x16 icons for SMALL_ICON.
 	 */
 	public void setIcon(String iconName) {
 		ImageIcon icon = ImageProvider.getIfAvailable(null, iconName);
 		if (icon == null)
 			icon = new ImageIcon(iconName);
 		if (Math.max(icon.getIconHeight(), icon.getIconWidth()) != 16)
 			icon = new ImageIcon(icon.getImage().getScaledInstance(16, 16, Image.SCALE_SMOOTH));
 		putValue(Action.SMALL_ICON, icon);
 	}
 
 	/**
 	 * Called from the XML parser to set the types this preset affects
 	 */
 	public void setType(String types) throws SAXException {
 		try {
 			for (String type : types.split(",")) {
 				type = Character.toUpperCase(type.charAt(0))+type.substring(1);
 				if (this.types == null)
 					this.types = new LinkedList<Class<?>>();
 				this.types.add(Class.forName("org.openstreetmap.josm.data.osm."+type));
 			}
 		} catch (ClassNotFoundException e) {
 			e.printStackTrace();
 			throw new SAXException(tr("Unknown type"));
 		}
 	}
 
 	public static List<TaggingPreset> readAll(InputStream inStream) throws SAXException {
 		BufferedReader in = null;
 		try {
 			in = new BufferedReader(new InputStreamReader(inStream, "UTF-8"));
 		} catch (UnsupportedEncodingException e) {
 			e.printStackTrace();
 			in = new BufferedReader(new InputStreamReader(inStream));
 		}
 		XmlObjectParser parser = new XmlObjectParser();
 		parser.mapOnStart("item", TaggingPreset.class);
 		parser.map("text", Text.class);
 		parser.map("check", Check.class);
 		parser.map("combo", Combo.class);
 		parser.map("label", Label.class);
 		parser.map("key", Key.class);
 		LinkedList<TaggingPreset> all = new LinkedList<TaggingPreset>();
 		parser.start(in);
 		while(parser.hasNext()) {
 			Object o = parser.next();
 			if (o instanceof TaggingPreset) {
 				all.add((TaggingPreset)o);
 				Main.toolbar.register((TaggingPreset)o);
 			} else
 				all.getLast().data.add((Item)o);
 		}
 		return all;
 	}
 
 	public static Collection<TaggingPreset> readFromPreferences() {
 		LinkedList<TaggingPreset> allPresets = new LinkedList<TaggingPreset>();
 		String allTaggingPresets = Main.pref.get("taggingpreset.sources");
 		
 		if (Main.pref.getBoolean("taggingpreset.enable-defaults", true)) {
 			InputStream in = Main.class.getResourceAsStream("/presets/presets.xml");
 			try {
 				allPresets.addAll(TaggingPreset.readAll(in));
 			} catch (SAXException x) {
 				JOptionPane.showMessageDialog(Main.parent, tr("Error parsing presets.xml: ")+x.getMessage());
 			}
 		}
 		
 		StringTokenizer st = new StringTokenizer(allTaggingPresets, ";");
 		while (st.hasMoreTokens()) {
 			InputStream in = null;
 			String source = st.nextToken();
 			try {
 				if (source.startsWith("http") || source.startsWith("ftp") || source.startsWith("file"))
 					in = new URL(source).openStream();
 				else if (source.startsWith("resource://")) 
 					in = Main.class.getResourceAsStream(source.substring("resource:/".length()));
 				else
 					in = new FileInputStream(source);
 				allPresets.addAll(TaggingPreset.readAll(in));
 				in.close();
 			} catch (IOException e) {
 				e.printStackTrace();
 				JOptionPane.showMessageDialog(Main.parent, tr("Could not read tagging preset source: {0}",source));
 			} catch (SAXException e) {
 				e.printStackTrace();
 				JOptionPane.showMessageDialog(Main.parent, tr("Error parsing {0}: ", source)+e.getMessage());
 			}
 		}
 		return allPresets;
 	}
 
 	public JPanel createPanel(Collection<OsmPrimitive> selected) {
 		if (data == null)
 			return null;
 		JPanel p = new JPanel(new GridBagLayout());
 
 		for (Item i : data)
 			i.addToPanel(p, selected);
 		return p;
 	}
 
 	public void actionPerformed(ActionEvent e) {
 		Collection<OsmPrimitive> sel = Main.ds.getSelected();
 		JPanel p = createPanel(sel);
 		if (p == null)
 			return;
 		int answer = JOptionPane.OK_OPTION;
 		if (p.getComponentCount() != 0) {
 			final JOptionPane optionPane = new JOptionPane(p, JOptionPane.PLAIN_MESSAGE, JOptionPane.OK_CANCEL_OPTION){
 				@Override public void selectInitialValue() {
 					for (Item i : data) {
 						if (i.focus) {
 							i.requestFocusInWindow();
 							return;
 						}
 					}
 				}
 			};
 			optionPane.createDialog(Main.parent, trn("Change {0} object", "Change {0} objects", sel.size(), sel.size())).setVisible(true);
 			Object answerObj = optionPane.getValue();
 			if (answerObj == null || answerObj == JOptionPane.UNINITIALIZED_VALUE ||
 					(answerObj instanceof Integer && (Integer)answerObj != JOptionPane.OK_OPTION))
 				answer = JOptionPane.CANCEL_OPTION;
 		}
 		if (answer == JOptionPane.OK_OPTION) {
 			Command cmd = createCommand(Main.ds.getSelected());
 			if (cmd != null)
 				Main.main.undoRedo.add(cmd);
 		}
 		Main.ds.setSelected(Main.ds.getSelected()); // force update
 	}
 
 	private Command createCommand(Collection<OsmPrimitive> participants) {
 		Collection<OsmPrimitive> sel = new LinkedList<OsmPrimitive>();
 		for (OsmPrimitive osm : participants)
 			if (types == null || types.contains(osm.getClass()))
 				sel.add(osm);
 		if (sel.isEmpty())
 			return null;
 
 		List<Command> cmds = new LinkedList<Command>();
 		for (Item i : data)
 			i.addCommands(sel, cmds);
 		if (cmds.size() == 0)
 			return null;
 		else if (cmds.size() == 1)
 			return cmds.get(0);
 		else
 			return new SequenceCommand(tr("Change Properties"), cmds);
 	}
 }
