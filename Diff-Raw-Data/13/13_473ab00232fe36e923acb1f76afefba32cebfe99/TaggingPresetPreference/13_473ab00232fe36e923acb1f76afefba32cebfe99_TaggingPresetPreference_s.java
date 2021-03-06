 // License: GPL. Copyright 2007 by Immanuel Scholz and others
 package org.openstreetmap.josm.gui.preferences;
 
 import static org.openstreetmap.josm.tools.I18n.tr;
 
 import java.awt.GridBagLayout;
 import java.util.Collection;
 import java.util.HashMap;
 
 import javax.swing.BorderFactory;
 import javax.swing.JCheckBox;
 import javax.swing.JMenu;
 import javax.swing.JMenuItem;
 import javax.swing.JPanel;
 import javax.swing.JScrollPane;
 import javax.swing.JSeparator;
 
 import org.openstreetmap.josm.Main;
 import org.openstreetmap.josm.gui.tagging.TaggingPreset;
 import org.openstreetmap.josm.gui.tagging.TaggingPresetMenu;
 import org.openstreetmap.josm.gui.tagging.TaggingPresetSeparator;
 import org.openstreetmap.josm.tools.GBC;
 
 public class TaggingPresetPreference implements PreferenceSetting {
 
     public static class Factory implements PreferenceSettingFactory {
         public PreferenceSetting createPreferenceSetting() {
             return new TaggingPresetPreference();
         }
     }
 
     public static Collection<TaggingPreset> taggingPresets;
     private StyleSources sources;
     private JCheckBox sortMenu;
     private JCheckBox enableDefault;
 
     public void addGui(final PreferenceDialog gui) {
         sortMenu = new JCheckBox(tr("Sort presets menu"),
                 Main.pref.getBoolean("taggingpreset.sortmenu", false));
         enableDefault = new JCheckBox(tr("Enable built-in defaults"),
                 Main.pref.getBoolean("taggingpreset.enable-defaults", true));
 
         JPanel panel = new JPanel(new GridBagLayout());
         JScrollPane scrollpane = new JScrollPane(panel);
         panel.setBorder(BorderFactory.createEmptyBorder( 0, 0, 0, 0 ));
         panel.add(sortMenu, GBC.eol().insets(5,5,5,0));
         panel.add(enableDefault, GBC.eol().insets(5,0,5,0));
         sources = new StyleSources("taggingpreset.sources", "taggingpreset.icon.sources",
         "http://josm.openstreetmap.de/presets", false, tr("Tagging Presets"));
         panel.add(sources, GBC.eol().fill(GBC.BOTH));
         gui.mapcontent.addTab(tr("Tagging Presets"), scrollpane);
     }
 
     public boolean ok() {
         boolean restart = Main.pref.put("taggingpreset.enable-defaults",
         enableDefault.getSelectedObjects() != null);
         if(Main.pref.put("taggingpreset.sortmenu", sortMenu.getSelectedObjects() != null))
             restart = true;
         if(sources.finish())
             restart = true;
         return restart;
     }
 
     /**
      * Initialize the tagging presets (load and may display error)
      */
     public static void initialize() {
         taggingPresets = TaggingPreset.readFromPreferences();
         if (taggingPresets.isEmpty()) {
             Main.main.menu.presetsMenu.setVisible(false);
         }
         else
         {
             HashMap<TaggingPresetMenu,JMenu> submenus = new HashMap<TaggingPresetMenu,JMenu>();
             for (final TaggingPreset p : taggingPresets)
             {
                 JMenu m = p.group != null ? submenus.get(p.group) : Main.main.menu.presetsMenu;
                 if (p instanceof TaggingPresetSeparator)
                     m.add(new JSeparator());
                 else if (p instanceof TaggingPresetMenu)
                 {
                     JMenu submenu = new JMenu(p);
                     ((TaggingPresetMenu)p).menu = submenu;
                     submenus.put((TaggingPresetMenu)p, submenu);
                     m.add(submenu);
                 }
                 else
                 {
                     JMenuItem mi = new JMenuItem(p);
                     mi.setText(p.getLocaleName());
                     m.add(mi);
                 }
             }
         }
         if(Main.pref.getBoolean("taggingpreset.sortmenu"))
             TaggingPresetMenu.sortMenu(Main.main.menu.presetsMenu);
     }
 }
