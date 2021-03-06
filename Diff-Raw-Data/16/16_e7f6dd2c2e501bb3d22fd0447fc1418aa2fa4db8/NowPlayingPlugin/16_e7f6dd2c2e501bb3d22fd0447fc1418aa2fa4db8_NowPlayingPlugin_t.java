 /*
  * Copyright (c) 2006-2007 Chris Smith, Shane Mc Cormack, Gregory Holmes
  *
  * Permission is hereby granted, free of charge, to any person obtaining a copy
  * of this software and associated documentation files (the "Software"), to deal
  * in the Software without restriction, including without limitation the rights
  * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
  * copies of the Software, and to permit persons to whom the Software is
  * furnished to do so, subject to the following conditions:
  *
  * The above copyright notice and this permission notice shall be included in
  * all copies or substantial portions of the Software.
  *
  * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
  * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
  * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
  * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
  * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
  * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
  * SOFTWARE.
  */
 
 package com.dmdirc.addons.nowplaying.plugin;
 
 import com.dmdirc.actions.ActionType;
 import com.dmdirc.actions.CoreActionType;
 import com.dmdirc.addons.nowplaying.MediaSource;
 import com.dmdirc.addons.nowplaying.MediaSourceManager;
 import com.dmdirc.commandparser.CommandManager;
 import com.dmdirc.plugins.EventPlugin;
 import com.dmdirc.plugins.Plugin;
 import com.dmdirc.plugins.PluginManager;
 import com.dmdirc.ui.components.PreferencesInterface;
 import com.dmdirc.ui.components.PreferencesPanel;
 
 import java.util.LinkedList;
 import java.util.List;
 import java.util.Properties;
 
 public class NowPlayingPlugin extends Plugin implements EventPlugin,
         PreferencesInterface  {
     
     /** Config domain. */
     private static final String MY_DOMAIN = "nowplaying";
     
     /** The sources that we know of. */
     private List<MediaSource> sources;
     
     /** The now playing command we're registering. */
     private NowPlayingCommand command;
     
     /** Config panel. */
     private ConfigPanel configPanel;
     
     /** {@inheritDoc} */
     public boolean onLoad() {
         return true;
     }
     
     /** {@inheritDoc} */
     @Override
     protected void onActivate() {
         sources = new LinkedList<MediaSource>();
         
         PluginManager.getPluginManager().addPlugin(
                 "com.dmdirc.addons.nowplaying.dcop.DcopMediaSourcePlugin");
         
         for (Plugin target : PluginManager.getPluginManager().getPlugins()) {
             if (target.isActive()) {
                 addPlugin(target);
             }
         }
         
         command = new NowPlayingCommand(this);
     }
     
     /** {@inheritDoc} */
     @Override
     protected void onDeactivate() {
         sources = null;
         
         PluginManager.getPluginManager().delPlugin(
                 "com.dmdirc.addons.nowplaying.dcop.DcopMediaSourcePlugin");
         
         CommandManager.unregisterCommand(command);
     }
     
     /** {@inheritDoc} */
     public String getVersion() {
         return "0.2";
     }
     
     /** {@inheritDoc} */
     public String getAuthor() {
         return "Chris <chris@dmdirc.com>";
     }
     
     /** {@inheritDoc} */
     public String getDescription() {
         return "Adds a nowplaying command";
     }
     
     /** {@inheritDoc} */
     public String toString() {
         return "Now playing command";
     }
     
     /** {@inheritDoc} */
     public boolean isConfigurable() { return true; }
     
     /** {@inheritDoc} */
     public void showConfig() {
         final PreferencesPanel preferencesPanel = new PreferencesPanel(this, "Now playing Plugin - Config");
         configPanel = new ConfigPanel(sources);
         
         preferencesPanel.addCategory("General", "General options for the plugin.");
         
         preferencesPanel.replaceOptionPanel("General", configPanel);
         
         preferencesPanel.display();
     }
     
     /** {@inheritDoc} */
     public void configClosed(final Properties properties) {
         //save settings
     }
     
     /** {@inheritDoc} */
     public void configCancelled() {
         //Ignore
     }
     
     /** {@inheritDoc} */
     public void processEvent(final ActionType type, final StringBuffer format,
             final Object... arguments) {
         if (type == CoreActionType.PLUGIN_ACTIVATED) {
             addPlugin((Plugin) arguments[0]);
         } else if (type == CoreActionType.PLUGIN_DEACTIVATED) {
             removePlugin((Plugin) arguments[0]);
         }
     }
     
     /**
      * Checks to see if a plugin implements one of the Media Source interfaces
      * and if it does, adds the source(s) to our list.
      *
      * @param target The plugin to be tested
      */
     private void addPlugin(final Plugin target) {
         if (target instanceof MediaSource) {
             sources.add((MediaSource) target);
         }
         
         if (target instanceof MediaSourceManager) {
             sources.addAll(((MediaSourceManager) target).getSources());
         }
     }
     
     /**
      * Checks to see if a plugin implements one of the Media Source interfaces
      * and if it does, removes the source(s) from our list.
      *
      * @param target The plugin to be tested
      */
     private void removePlugin(final Plugin target) {
         if (target instanceof MediaSource) {
             sources.remove((MediaSource) target);
         }
         
         if (target instanceof MediaSourceManager) {
             sources.removeAll(((MediaSourceManager) target).getSources());
         }
     }
     
     /**
      * Determines if there are any valid sources (paused or not).
      *
      * @return True if there are running sources, false otherwise
      */
     public boolean hasRunningSource() {
         for (MediaSource source : sources) {
             if (source.isRunning()) {
                 return true;
             }
         }
         
         return false;
     }
     
     /**
      * Retrieves the "best" source to use for displaying media information.
      * The best source is defined as the earliest in the list that is running
      * and not paused, or, if no such source exists, the earliest in the list
      * that is running and paused. If neither condition is satisified returns
      * null.
      *
      * @return The best source to use for media info
      */
     public MediaSource getBestSource() {
         MediaSource paused = null;
         
         for (MediaSource source : sources) {
             if (source.isRunning()) {
                 if (source.isPlaying()) {
                     return source;
                 } else if (paused == null) {
                     paused = source;
                 }
             }
         }
         
         return paused;
     }
     
     /**
      * Retrieves a source based on its name.
      *
      * @param name The name to search for
      * @return The source with the specified name or null if none were found.
      */
     public MediaSource getSource(final String name) {
         for (MediaSource source : sources) {
             if (source.getName().equalsIgnoreCase(name)) {
                 return source;
             }
         }
         
         return null;
     }
     
     /**
      * Retrieves all the sources registered with this plugin.
      *
      * @return All known media sources
      */
     public List<MediaSource> getSources() {
         return sources;
     }
 }
