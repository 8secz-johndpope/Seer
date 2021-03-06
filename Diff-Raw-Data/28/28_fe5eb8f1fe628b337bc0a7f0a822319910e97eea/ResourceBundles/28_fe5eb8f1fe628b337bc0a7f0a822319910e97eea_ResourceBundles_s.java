 /*
  *  Freeplane - mind map editor
  *  Copyright (C) 2008 Joerg Mueller, Daniel Polansky, Christian Foltin, Dimitry Polivaev
  *
  *  This file is modified by Dimitry Polivaev in 2008.
  *
  *  This program is free software: you can redistribute it and/or modify
  *  it under the terms of the GNU General Public License as published by
  *  the Free Software Foundation, either version 2 of the License, or
  *  (at your option) any later version.
  *
  *  This program is distributed in the hope that it will be useful,
  *  but WITHOUT ANY WARRANTY; without even the implied warranty of
  *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  *  GNU General Public License for more details.
  *
  *  You should have received a copy of the GNU General Public License
  *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
  */
 package org.freeplane.core.resources;
 
 import java.io.BufferedInputStream;
 import java.io.IOException;
 import java.io.InputStream;
 import java.net.URL;
 import java.util.Enumeration;
 import java.util.HashMap;
 import java.util.Iterator;
 import java.util.Locale;
 import java.util.Map;
 import java.util.Properties;
 import java.util.ResourceBundle;
 
 import org.freeplane.core.util.LogUtils;
 import org.freeplane.core.util.collection.MultipleValueMap;
 
 /**
  * Class for managing localized resources. See translation property files.
  */
 public class ResourceBundles extends ResourceBundle {
 	public static final String LANGUAGE_AUTOMATIC = "automatic";
 	private static final String DEFAULT_LANGUAGE = "en";
 	public static final String POSTFIX_TRANSLATE_ME = "[translate me]";
 	public static final String RESOURCE_LANGUAGE = "language";
 	/**
 	 *
 	 */
 	final private ResourceController controller;
 	private Map<String, String> defaultResources;
 	private final MultipleValueMap<String, URL> externalResources;
 	private String lang;
 	private Map<String, String> languageResources;
 
 	ResourceBundles(final ResourceController controller) {
 		this.controller = controller;
 		externalResources = new MultipleValueMap<String, URL>();
 		try {
 			loadLocalLanguageResources();
			defaultResources = getLanguageResources(DEFAULT_LANGUAGE);
 		}
 		catch (final Exception ex) {
 			LogUtils.severe(ex);
 			LogUtils.severe("Error loading Resources");
 		}
 	}
 
 	public void addResources(final String language, final URL url) {
 		try {
 			if (language.equalsIgnoreCase(DEFAULT_LANGUAGE)) {
 				defaultResources.putAll(getLanguageResources(url));
 			}
 			else if (language.equalsIgnoreCase(lang)) {
 				languageResources.putAll(getLanguageResources(url));
 			}
 			externalResources.put(language, url);
 		}
 		catch (final IOException e) {
 			LogUtils.severe(e);
 		}
 	}
 
 	@SuppressWarnings({ "rawtypes", "unchecked" })
     @Override
 	public Enumeration getKeys() {
 		final Iterator<String> iterator = defaultResources.keySet().iterator();
 		return new Enumeration() {
 			public boolean hasMoreElements() {
 				return iterator.hasNext();
 			}
 
 			public Object nextElement() {
 				return iterator.next();
 			}
 		};
 	}
 
 	public String getLanguageCode() {
 		return lang;
 	}
 
 	/**
 	 * @throws IOException
 	 */
 	private Map<String, String> getLanguageResources(final String lang) throws IOException {
 		String resourceName = "/translations/Resources" + "_" + lang + ".properties";
 		final URL systemResource = ResourceController.getResourceController().getResource(
 		    resourceName);
 		if (systemResource == null) {
 			System.out.println("core resource " + resourceName + " not found");
 			return null;
 		}
 		final Map<String, String> resources = getLanguageResources(systemResource);
 		final Iterator<URL> iterator = externalResources.get(lang).iterator();
 		while (iterator.hasNext()) {
 			resources.putAll(getLanguageResources(iterator.next()));
 		}
 		return resources;
 	}
 
 	@SuppressWarnings({ "unchecked", "rawtypes" })
     private Map<String, String> getLanguageResources(final URL systemResource) throws IOException {
 		final InputStream in = new BufferedInputStream(systemResource.openStream());
 		final Properties bundle = new Properties();
 		bundle.load(in);
 		in.close();
 		return new HashMap(bundle);
 	}
 
 	public String getResourceString(final String key) {
 		final String resourceString = getResourceString(key, key);
 		if (resourceString == key) {
 			System.err.println("missing key " + key);
 			return '[' + key + ']';
 		}
 		return resourceString;
 	}
 
 	public String getResourceString(final String key, final String resource) {
 		String value = languageResources.get(key);
 		if (value != null) {
 			return value;
 		}
 		value = defaultResources.get(key);
 		if (value != null) {
 			return value + ResourceBundles.POSTFIX_TRANSLATE_ME;
 		}
 		return resource;
 	}
 
 	public String putResourceString(final String key, final String resource) {
 		return languageResources.put(key, resource);
 	}
 	
 	@Override
 	protected Object handleGetObject(final String key) {
 		try {
 			return languageResources.get(key);
 		}
 		catch (final Exception ex) {
 			LogUtils.severe("Warning - resource string not found:" + key);
 			return defaultResources.get(key) + ResourceBundles.POSTFIX_TRANSLATE_ME;
 		}
 	}
 
 	private void loadLocalLanguageResources() throws IOException {
 		lang = controller.getProperty(ResourceBundles.RESOURCE_LANGUAGE);
 		if (lang == null || lang.equals(LANGUAGE_AUTOMATIC)) {
 			final String country = Locale.getDefault().getCountry();
 			if(! country.equals("")){
 				lang = Locale.getDefault().getLanguage() + "_" + country;
 				languageResources = getLanguageResources(lang);
 				if (languageResources != null) {
 					return;
 				}
 				LogUtils.info("language resources for " + lang + " not found");
 			}
 			lang = Locale.getDefault().getLanguage();
 			languageResources = getLanguageResources(lang);
 			if (languageResources != null) {
 				return;
 			}
 			LogUtils.info("language resources for " + lang + " not found");
 		}
 		if ("no".equals(lang)) {
 			lang = "nb";
 		}
 		languageResources = getLanguageResources(lang);
 		if (languageResources != null) {
 			return;
 		}
 		LogUtils.info("language resources for " + lang + " not found");
 		lang = DEFAULT_LANGUAGE;
 		languageResources = getLanguageResources(lang);
 		if (languageResources != null) {
 			return;
 		}
 		LogUtils.severe("language resources for " + lang + " not found, aborting");
 		System.exit(1);
 	}
 
	public void reloadLanguage() {
 		try {
			loadLocalLanguageResources();
 		}
 		catch (final IOException e) {
 			LogUtils.severe(e);
 		}
 	}
 }
