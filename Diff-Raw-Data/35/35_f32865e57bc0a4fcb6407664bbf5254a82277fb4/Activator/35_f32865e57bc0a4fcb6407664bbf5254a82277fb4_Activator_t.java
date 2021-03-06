 /*
 * Copyright 2009 Google Inc.
  *
  * Licensed under the Apache License, Version 2.0 (the "License"); you may not
  * use this file except in compliance with the License. You may obtain a copy of
  * the License at
  *
  * http://www.apache.org/licenses/LICENSE-2.0
  *
  * Unless required by applicable law or agreed to in writing, software
  * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
  * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
  * License for the specific language governing permissions and limitations under
  * the License.
  */
 package com.google.eclipse.javascript.jstestdriver.ui;
 
 import org.eclipse.ui.plugin.AbstractUIPlugin;
 import org.osgi.framework.BundleContext;
 
import com.google.eclipse.javascript.jstestdriver.ui.prefs.WorkbenchPreferencePage;

 /**
  * The activator class controls the plug-in life cycle
  *
  * @author shyamseshadri@gmail.com (Shyam Seshadri)
  */
 public class Activator extends AbstractUIPlugin {
 
   // The plug-in ID
   public static final String PLUGIN_ID = "com.google.eclipse.javascript.jstestdriver.ui";
 
   // The shared instance
   private static Activator plugin;
 
   private Icons icons = new Icons();
 
   public Icons getIcons() {
     return icons;
   }
 
   @Override
   public void start(BundleContext context) throws Exception {
     super.start(context);
    getPreferenceStore().setDefault(WorkbenchPreferencePage.PREFERRED_SERVER_PORT,
                                    42442);
     plugin = this;
   }
 
   @Override
   public void stop(BundleContext context) throws Exception {
     plugin.getIcons().disposeAllImages();
     plugin = null;
     super.stop(context);
   }
 
   /**
    * Returns the shared instance
    *
    * @return the shared instance
    */
   public static Activator getDefault() {
     return plugin;
   }
 }
