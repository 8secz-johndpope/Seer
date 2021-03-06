 /**
  * $Revision: $
  * $Date: $
  *
  * Copyright (C) 2006 Jive Software. All rights reserved.
  *
  * This software is published under the terms of the GNU Lesser Public License (LGPL),
  * a copy of which is included in this distribution.
  */
 
 package org.jivesoftware.sparkimpl.settings.local;
 
 import java.io.File;
 import java.io.FileInputStream;
 import java.io.FileOutputStream;
 import java.io.IOException;
 import java.util.ArrayList;
 import java.util.List;
 import java.util.Properties;
 
 import org.jivesoftware.Spark;
 import org.jivesoftware.spark.util.WinRegistry;
 import org.jivesoftware.spark.util.log.Log;
 
 
 /**
  * Responsbile for the loading and persisting of LocalSettings.
  */
 public class SettingsManager {
     private static LocalPreferences localPreferences;
 
     private static List<PreferenceListener> listeners = new ArrayList<PreferenceListener>();
 
     private static boolean fileExists = false;
 
     private SettingsManager() {
     }
 
     /**
      * Returns the LocalPreferences for this user.
      *
      * @return the LocalPreferences for this user.
      */
     public static LocalPreferences getLocalPreferences() {
         if(localPreferences != null){
             return localPreferences;
         }
 
         if (!fileExists) {
             fileExists = exists();
         }
 
         if (!fileExists && localPreferences == null) {
             localPreferences = new LocalPreferences();
             saveSettings();
         }
 
         if (localPreferences == null) {
             // Do Initial Load from FileSystem.
             getSettingsFile();
             localPreferences = load();
         }
 
         return localPreferences;
     }
 
     /**
      * Persists the settings to the local file system.
      */
     public static void saveSettings() {
         final Properties props = localPreferences.getProperties();
 
         try {
             props.store(new FileOutputStream(getSettingsFile()), "Spark Settings");
         }
         catch (Exception e) {
             Log.error("Error saving settings.", e);
         }
         
         if (localPreferences.getStartOnStartup())
         {
         	try	{
         		if (Spark.isWindows())
         		{
         			String PROGDIR = Spark.getBinDirectory().getParent();
         			File file = new File(PROGDIR + "\\" + "Spark.exe");
         			if (file.exists())
         			{
 		        		WinRegistry.createKey(
 		        				WinRegistry.HKEY_CURRENT_USER, 
 		        				"SOFTWARE\\Microsoft\\Windows\\CurrentVersion\\Run");
         				WinRegistry.writeStringValue(
         					WinRegistry.HKEY_CURRENT_USER, 
         					"SOFTWARE\\Microsoft\\Windows\\CurrentVersion\\Run", 
         					"Spark", 
         					file.getAbsolutePath());
         			}
         		}        	
         	} 
         	catch (Exception e) {
         		e.printStackTrace();
         	}
         }
         else
         {
 
     		if (Spark.isWindows())
     		{
             	try	{
            		WinRegistry.deleteValue(
            	          WinRegistry.HKEY_CURRENT_USER, 
            	          "SOFTWARE\\Microsoft\\Windows\\CurrentVersion\\Run", "Spark");
             	}
             	catch (Exception e) {
            		e.printStackTrace();
             	}
     		}
         }
     }
 
     /**
      * Return true if the settings file exists.
      *
      * @return true if the settings file exists.('settings.xml')
      */
     public static boolean exists() {
         return getSettingsFile().exists();
     }
 
     /**
      * Returns the settings file.
      *
      * @return the settings file.
      */
     public static File getSettingsFile() {
         File file = new File(Spark.getSparkUserHome());
         if (!file.exists()) {
             file.mkdirs();
         }
         return new File(file, "spark.properties");
     }
 
 
     private static LocalPreferences load() {
         final Properties props = new Properties();
         try {
             props.load(new FileInputStream(getSettingsFile()));
         }
         catch (IOException e) {
             Log.error(e);
             return new LocalPreferences();
         }
 
         // Override with global settings file
         File globalSettingsFile = new File("spark.properties");
         if (globalSettingsFile.exists()) {
             try {
                 props.load(new FileInputStream(globalSettingsFile));
             } catch (IOException e) {
                 Log.error(e);
             }
         }
 
         return new LocalPreferences(props);
     }
 
     public static void addPreferenceListener(PreferenceListener listener) {
         listeners.add(listener);
     }
 
     public static void removePreferenceListener(PreferenceListener listener) {
         listeners.remove(listener);
     }
 
     public static void fireListeners() {
         for (PreferenceListener listener : listeners) {
             listener.preferencesChanged(localPreferences);
         }
     }
 }
