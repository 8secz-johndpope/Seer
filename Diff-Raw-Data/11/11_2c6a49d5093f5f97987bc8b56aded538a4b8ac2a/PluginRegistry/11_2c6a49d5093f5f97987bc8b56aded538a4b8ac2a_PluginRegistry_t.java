 /********************************************************************************
  * CruiseControl, a Continuous Integration Toolkit
  * Copyright (c) 2001, ThoughtWorks, Inc.
  * 651 W Washington Ave. Suite 600
  * Chicago, IL 60661 USA
  * All rights reserved.
  *
  * Redistribution and use in source and binary forms, with or without
  * modification, are permitted provided that the following conditions
  * are met:
  *
  *     + Redistributions of source code must retain the above copyright
  *       notice, this list of conditions and the following disclaimer.
  *
  *     + Redistributions in binary form must reproduce the above
  *       copyright notice, this list of conditions and the following
  *       disclaimer in the documentation and/or other materials provided
  *       with the distribution.
  *
  *     + Neither the name of ThoughtWorks, Inc., CruiseControl, nor the
  *       names of its contributors may be used to endorse or promote
  *       products derived from this software without specific prior
  *       written permission.
  *
  * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
  * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
  * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
  * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE REGENTS OR
  * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
  * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
  * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
  * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
  * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
  * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
  * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
  ********************************************************************************/
 package net.sourceforge.cruisecontrol;
 
 import java.io.IOException;
 import java.util.HashMap;
 import java.util.Iterator;
 import java.util.Map;
 import java.util.Properties;
 
 import org.apache.log4j.Logger;
 
 
 /**
  * Handles "registering" plugins that will be used by the CruiseControl
  * configuration file.
 
  * A PluginRegistry can have a parent registry, which it will query for
  * a plugin if it's not defined in the registry itself. This is used to 
  * enable projects to have their own plugins and override the classname 
  * for a specific plugin, like the labelincrementer.
  * 
  * The root-registry contains the default list of plugins, i.e. those
  * that are already registered like AntBuilder that don't have to be registered
  * seperately in the configuration file.
  */
 public final class PluginRegistry {
 
     private static final Logger LOG = Logger.getLogger(PluginRegistry.class);
     
     /**
      * The only instance of the root plugin registry.
      * This contains the default plugins and the plugins that are defined 
      * external to projects.
      */
    private static final PluginRegistry ROOTREGISTRY = getDefaultPluginRegistry();    
     
     /**
     * @return PluginRegistry with the ROOTREGISTRY as its parent.
      */
     public static final PluginRegistry createRegistry() {
        return new PluginRegistry(ROOTREGISTRY);
     }
     
     /**
      * The parent registry that will be searched for plugin definitions
      * if they're not defined in the registry itself. May be null.
      */
     private final PluginRegistry parentRegistry;    
     
     /**
      * Map of plugins where the key is the plugin name (e.g. ant) and the value is
      * the fully qualified classname
      * (e.g. net.sourceforge.cruisecontrol.builders.AntBuilder).
      */
     private final Map plugins = new HashMap();
 
     /**
      * Creates a new PluginRegistry with no plugins registered, with the given parent registry. 
      * Only used internally for now, Projects should call createRegistry instead.
      */
     private PluginRegistry(PluginRegistry parentRegistry) {
         this.parentRegistry = parentRegistry;
     }
   
     /**
      * @param pluginName The name for the plugin, e.g. ant. Note that plugin
      * names are always treated as case insensitive, so Ant, ant, and AnT are
      * all treated as the same plugin.
      *
      * @param pluginClassname The fully qualified classname for the
      * plugin class, e.g. net.sourceforge.cruisecontrol.builders.AntBuilder.
      */
     public void register(String pluginName, String pluginClassname) {
         plugins.put(pluginName.toLowerCase(), pluginClassname);
     }
 
     /**
      * Registers the given plugin in the root registry, so it will be
      * available to all projects.
      * 
      */
     static void registerToRoot(String pluginName, String pluginClassname) {
         LOG.debug("registering plugin '" + pluginName + "' to root register as " + pluginClassname);
        ROOTREGISTRY.register(pluginName, pluginClassname);
     }
 
     /**
      * @return Returns null if no plugin has been registered with the specified
      * name, otherwise a String representing the fully qualified classname
      * for the plugin class. Note that plugin
      * names are always treated as case insensitive, so Ant, ant, and AnT are
      * all treated as the same plugin.
      */
     public String getPluginClassname(String pluginName) {
         if (!isPluginRegistered(pluginName)) {
             return null;
         }
         String className = (String) plugins.get(pluginName.toLowerCase());
         if (className != null) {
             return className;
         } else {
             // must be registered in our parent, then
             return parentRegistry.getPluginClassname(pluginName); 
         }
     }
 
     /**
      * @return Returns null if no plugin has been registered with the specified
      * name, otherwise the Class representing the the plugin class. Note that
      * plugin names are always treated as case insensitive, so Ant, ant,
      * and AnT are all treated as the same plugin.
      *
      * @throws CruiseControlException If the class provided cannot be loaded.
      */
     public Class getPluginClass(String pluginName)
             throws CruiseControlException {
         if (!isPluginRegistered(pluginName)) {
             return null;
         }
         String pluginClassname = getPluginClassname(pluginName);
 
         Class pluginClass = null;
         try {
             pluginClass = Class.forName(pluginClassname);
         } catch (ClassNotFoundException e) {
             e.printStackTrace();
             throw new CruiseControlException(
                     "Attemping to load plugin named [" + pluginName
                     + "], but couldn't load corresponding class ["
                     + pluginClassname + "].");
         }
         return pluginClass;
     }
 
     /**
      * @return True if this registry or its parent contains 
      * an entry for the plugin specified by the name. 
      * The name is the short name for the plugin, not
      * the classname, e.g. ant. Note that plugin
      * names are always treated as case insensitive, so Ant, ant, and AnT are
      * all treated as the same plugin.
      *
      * @throws NullPointerException If a null pluginName is passed, then
      * a NullPointerException will occur. It's recommended to not pass a
      * null pluginName.
      */
     public boolean isPluginRegistered(String pluginName) {
         boolean knownToSelf = plugins.containsKey(pluginName.toLowerCase());
         if (knownToSelf) {
             return true;
         }
         if (parentRegistry == null) {
             return false;
         }
         return parentRegistry.isPluginRegistered(pluginName);
     }
 
     /**
      * Returns a PluginRegistry containing all the default plugins.
      * The key is the plugin name (e.g. ant) and the value is
      * the fully qualified classname
      * (e.g. net.sourceforge.cruisecontrol.builders.AntBuilder).
      * @throws RuntimeException in case of IOException during the reading of the properties-file
      */
     private static PluginRegistry getDefaultPluginRegistry() {
         PluginRegistry rootRegistry = new PluginRegistry(null);
         Properties pluginDefinitions = new Properties();
         try {
             pluginDefinitions.load(PluginRegistry.class.getResourceAsStream("default-plugins.properties"));
         } catch (IOException e) {
             throw new RuntimeException("Failed to load plugin-definitions from default-plugins.properties: " + e);
         }
         for (Iterator iter = pluginDefinitions.entrySet().iterator(); iter.hasNext(); ) {
             Map.Entry entry = (Map.Entry) iter.next();
             rootRegistry.register((String) entry.getKey(), (String) entry.getValue());
         }
         return rootRegistry;
     }
 }
