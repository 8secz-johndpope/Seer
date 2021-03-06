 /**
  *
  * Copyright (c) 2009-2013 Freedomotic team http://freedomotic.com
  *
  * This file is part of Freedomotic
  *
  * This Program is free software; you can redistribute it and/or modify it under
  * the terms of the GNU General Public License as published by the Free Software
  * Foundation; either version 2, or (at your option) any later version.
  *
  * This Program is distributed in the hope that it will be useful, but WITHOUT
  * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
  * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
  * details.
  *
  * You should have received a copy of the GNU General Public License along with
  * Freedomotic; see the file COPYING. If not, see
  * <http://www.gnu.org/licenses/>.
  */
 package it.freedomotic.util.I18n;
 
 import com.google.inject.Inject;
 import com.google.inject.Singleton;
 import it.freedomotic.api.Plugin;
 import it.freedomotic.app.AppConfig;
 import it.freedomotic.util.Info;
 import java.io.File;
 import java.io.FilenameFilter;
 import java.io.IOException;
 import java.io.InputStream;
 import java.io.InputStreamReader;
 import java.net.MalformedURLException;
 import java.net.URL;
 import java.net.URLClassLoader;
 import java.net.URLConnection;
 import java.util.HashMap;
 import java.util.Locale;
 import java.util.MissingResourceException;
 import java.util.PropertyResourceBundle;
 import java.util.ResourceBundle;
 import java.util.Vector;
 import java.util.logging.Level;
 import java.util.logging.Logger;
 
 /**
  *
  * @author Matteo Mazzoni <matteo@bestmazzo.it>
  */
 @Singleton
 public class I18nImpl implements I18n {
 
     private Locale currentLocale;
     private HashMap<String, ResourceBundle> messages;
     private UTF8control RB_Control;
     private static Vector<ComboLanguage> languages;
     private final AppConfig config;
     private HashMap<String, File> packageBundleDir;
 
     @Inject
     public I18nImpl(AppConfig config) {
         this.config = config;
         currentLocale = Locale.getDefault();
         messages = new HashMap<String, ResourceBundle>();
         RB_Control = new UTF8control();
         languages = new Vector<ComboLanguage>();
         packageBundleDir = new HashMap<String, File>();
         // add mapping for base strings
         packageBundleDir.put("it.freedomotic", new File(Info.getApplicationPath() + "/i18n"));
     }
 
     /*
      * For Freedomotic core: translations are inside /i18n/Freedomotic.properties
      * For Plugin: translations are inside plugins/_plugin_type_/_plugin_package_/i18n/_package_last_part_.properties
      */
     protected String msg(String packageName, String key, Object[] fields) {
         // scorri packageNamefino a trovarne uno presente in packageBundleDir
 
         File folder = null;
         String workingPackage = packageName;
         while (workingPackage.lastIndexOf('.') != -1) {
             folder = packageBundleDir.get(workingPackage);
             if (folder == null) {
                 workingPackage = workingPackage.substring(0, workingPackage.lastIndexOf('.'));
             } else {
                 // prova ad estrarre la stringa, altrimenti esci dal ciclo e cerca nelle traduzioni base
                 String value = msg(folder, workingPackage, key, fields, true);
                 if (value != null) {
                     return value;
                 }
                 break;
             }
         }
         if (folder == null) {
             folder = packageBundleDir.get("it.freedomotic");
         }
         return msg(folder, "it.freedomotic", key, fields, false);
     }
 
     private String msg(File folder, String packageName, String key, Object[] fields, boolean reThrow) {
         // estrai locale corrente
 
         if (currentLocale == null) {
             return key;
         }
         String loc = currentLocale.toString(); //currentLocale.getLanguage() + "_" + currentLocale.getCountry();
 
         // cerca in messages (ed eventualmente aggiungi) la risorsa per il package e la lingua
 
         if (!messages.containsKey(packageName + ":" + loc)) {
             try {
 
                 ClassLoader loader;
                 URL[] urls = {folder.toURI().toURL()};
                 loader = new URLClassLoader(urls);
 
                 int lastSize = packageName.split("\\.").length;
                 String baseName = packageName.split("\\.")[lastSize - 1];
 
                 messages.put(packageName + ":" + loc, ResourceBundle.getBundle(baseName, currentLocale, loader, RB_Control));
                 LOG.log(Level.INFO, "Adding resourceBundle: package={0}, locale={1}. pointing at {2}", new Object[]{packageName, loc, folder.getAbsolutePath()});
             } catch (MalformedURLException ex) {
                 LOG.log(Level.SEVERE, "Cannot find folder while loading resourceBundle for package{0}", packageName);
             } catch (MissingResourceException ex) {
                 LOG.log(Level.SEVERE, "Cannot find resourceBundle files inside folder {1} for package{0}", new Object[]{packageName, folder.getAbsolutePath()});
             }
         }
 
         // estrai stringa
 
         if (messages.containsKey(packageName + ":" + loc)) {
             try {
                 if (messages.get(packageName + ":" + loc).containsKey(key)) {
                     return java.text.MessageFormat.format(messages.get(packageName + ":" + loc).getString(key), fields) + " ";
                 } else {
                     if (reThrow) {
                         return null;
                     }
                 }
             } catch (MissingResourceException ex) {
                 LOG.log(Level.SEVERE, "Cannot find resourceBundle files inside folder {0} for package{1}", new Object[]{folder.getAbsolutePath(), packageName});
             }
         }
         return key;
     }
 
     @Override
     public String msg(String key) {
        String caller = sun.reflect.Reflection.getCallerClass().getPackage().getName();
         return msg(caller, key, null);
     }
 
     @Override
     public String msg(String key, Object[] fields) {
        String caller = sun.reflect.Reflection.getCallerClass().getPackage().getName();
         return msg(caller, key, fields);
     }
 
     private void registerBundleDir(String packageName, File path) {
         if (!packageBundleDir.containsKey(packageName)) {
             packageBundleDir.put(packageName, path);
         }
     }
 
     @Override
     public void registerPluginBundleDir(Plugin plug) {
         registerBundleDir(plug.getClass().getPackage().getName(), new File(plug.getFile().getParentFile() + "/data/i18n"));
     }
 
     /**
      *
      */
     protected class UTF8control
             extends ResourceBundle.Control {
 
         protected static final String BUNDLE_EXTENSION = "properties";
 
         @Override
         public ResourceBundle newBundle(String baseName, Locale locale, String format, ClassLoader loader,
                 boolean reload)
                 throws IllegalAccessException, InstantiationException, IOException {
             // The below code is copied from default Control#newBundle() implementation.
             // Only the PropertyResourceBundle line is changed to read the file as UTF-8.
             String bundleName = toBundleName(baseName, locale);
             String resourceName = toResourceName(bundleName, BUNDLE_EXTENSION);
             ResourceBundle bundle = null;
             InputStream stream = null;
 
             if (reload) {
                 URL url = loader.getResource(resourceName);
 
                 if (url != null) {
                     URLConnection connection = url.openConnection();
 
                     if (connection != null) {
                         connection.setUseCaches(false);
                         stream = connection.getInputStream();
                     }
                 }
             } else {
                 stream = loader.getResourceAsStream(resourceName);
             }
 
             if (stream != null) {
                 try {
                     bundle = new PropertyResourceBundle(new InputStreamReader(stream, "UTF-8"));
                 } finally {
                     stream.close();
                 }
             }
 
             return bundle;
         }
     }
 
     @Override
     public void setDefaultLocale(String loc) {
         //if (loc.equals("no") || loc.equals("false")){
         currentLocale = null;
         if (loc == null || loc.isEmpty() || loc.equals("auto") || loc.equals("yes") || loc.equals("true")) {
             currentLocale = Locale.getDefault();
             config.put("KEY_ENABLE_I18N", "auto");
         } else if (loc.length() >= 5) {
             currentLocale = new Locale(loc.substring(0, 2), loc.substring(3, 5));
             config.put("KEY_ENABLE_I18N", currentLocale.toString());
         }
     }
 
     // should be replaced by user specific Locale
     @Deprecated
     @Override
     public String getDefaultLocale() {
         return currentLocale.toString();
     }
 
     @Override
     public Vector<ComboLanguage> getAvailableLocales() {
         final String bundlename = "Freedomotic";
         languages.clear();
         File root = new File(Info.getApplicationPath() + File.separator + "i18n");
         File[] files = root.listFiles(new FilenameFilter() {
             @Override
             public boolean accept(File dir, String name) {
                 return name.matches("^" + bundlename + "(_\\w{2}(_\\w{2})?)?\\.properties$");
             }
         });
 
 
         for (File file : files) {
             String value = file.getName().replaceAll("^" + bundlename + "(_)?|\\.properties$", "");
 
             if (!value.isEmpty()) {
                 Locale loc = new Locale(value.substring(0, 2), value.substring(3, 5));
                 languages.add(new ComboLanguage(loc.getDisplayLanguage(loc), value));
             }
         }
         languages.add(new ComboLanguage("Automatic", "auto"));
         return languages;
     }
     private static final Logger LOG = Logger.getLogger(I18n.class.getName());
 }
