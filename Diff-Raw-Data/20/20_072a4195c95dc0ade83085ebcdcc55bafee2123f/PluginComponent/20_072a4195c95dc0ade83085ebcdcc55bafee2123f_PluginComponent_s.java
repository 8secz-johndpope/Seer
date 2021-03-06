 /*
  * Copyright (c) 2006-2008 Chris Smith, Shane Mc Cormack, Gregory Holmes
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
 
 package com.dmdirc.updater.components;
 
 import com.dmdirc.plugins.PluginInfo;
 import com.dmdirc.updater.UpdateChecker;
 import com.dmdirc.updater.UpdateComponent;
 
 import java.io.File;
 
 /**
  * An update component for plugins.
  * 
  * @author chris
  */
 public class PluginComponent implements UpdateComponent {
     
     /** The plugin this component is for. */
     private final PluginInfo plugin;
 
     /**
      * Creates a new PluginComponent for the specified plugin, to enable it to
      * be updated automatically.
      * 
      * @param plugin The plugin to be added to the updater
      */
     public PluginComponent(final PluginInfo plugin) {
         this.plugin = plugin;
         
        if (plugin.getAddonID() > 0 && plugin.getVersion() > -1) {
             UpdateChecker.removeComponent(getName());
             UpdateChecker.registerComponent(this);
        }        
     }
 
     /** {@inheritDoc} */
     @Override
     public String getName() {
        return "addon-" + plugin.getAddonID();
     }
 
     /** {@inheritDoc} */
     @Override    
     public String getFriendlyName() {
         return plugin.getNiceName();
     }
 
     /** {@inheritDoc} */
     @Override    
     public int getVersion() {
         return plugin.getVersion();
     }
 
     /** {@inheritDoc} */
     @Override    
     public boolean doInstall(final String path) throws Throwable {
         final File target = new File(plugin.getFullFilename());
         
         if (target.exists()) {
             target.delete();
         }
         
         if (!new File(path).renameTo(target)) {
             // Windows rocks!
             final File newTarget = new File(plugin.getFullFilename() + ".update");
             
             if (newTarget.exists()) {
                 newTarget.delete();
             }
             
             new File(path).renameTo(newTarget);
             return true;
         }
         
         if (plugin.isLoaded()) {
             plugin.unloadPlugin();
             plugin.loadPlugin();
         }
         
         return false;
     }
 
 }
