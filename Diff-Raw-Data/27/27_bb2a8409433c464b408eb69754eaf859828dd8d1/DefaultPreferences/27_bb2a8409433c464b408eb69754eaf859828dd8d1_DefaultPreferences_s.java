 /*******************************************************************************
  * Copyright (c) 2004, 2006 IBM Corporation and others.
  * All rights reserved. This program and the accompanying materials
  * are made available under the terms of the Eclipse Public License v1.0
  * which accompanies this distribution, and is available at
  * http://www.eclipse.org/legal/epl-v10.html
  * 
  * Contributors:
  *     IBM Corporation - initial API and implementation
  *******************************************************************************/
 package org.eclipse.core.internal.preferences;
 
 import java.io.*;
 import java.net.URL;
 import java.util.*;
 import org.eclipse.core.internal.runtime.RuntimeLog;
 import org.eclipse.core.runtime.*;
 import org.eclipse.core.runtime.preferences.*;
 import org.eclipse.equinox.registry.IConfigurationElement;
 import org.eclipse.equinox.registry.IExtension;
 import org.eclipse.osgi.util.NLS;
 import org.osgi.framework.Bundle;
 import org.osgi.framework.BundleContext;
 import org.osgi.util.tracker.ServiceTracker;
 
 /**
  * @since 3.0
  */
 public class DefaultPreferences extends EclipsePreferences {
 	// cache which nodes have been loaded from disk
 	private static Set loadedNodes = new HashSet();
 	private static final String ELEMENT_INITIALIZER = "initializer"; //$NON-NLS-1$
 	private static final String ATTRIBUTE_CLASS = "class"; //$NON-NLS-1$
 	private static final String KEY_PREFIX = "%"; //$NON-NLS-1$
 	private static final String KEY_DOUBLE_PREFIX = "%%"; //$NON-NLS-1$
 	private static final IPath NL_DIR = new Path("$nl$"); //$NON-NLS-1$
 
 	private static final String PROPERTIES_FILE_EXTENSION = "properties"; //$NON-NLS-1$
 	private static Properties productCustomization;
 	private static Properties productTranslation;
 	private static Properties commandLineCustomization;
 	private EclipsePreferences loadLevel;
 
 	// cached values
 	private String qualifier;
 	private int segmentCount;
	private Object plugin;
 
 	public static String pluginCustomizationFile = null;
 
 	/**
 	 * Default constructor for this class.
 	 */
 	public DefaultPreferences() {
 		this(null, null);
 	}
 
 	private DefaultPreferences(EclipsePreferences parent, String name, Object context) {
 		this(parent, name);
		this.plugin = context;
 	}
 
 	private DefaultPreferences(EclipsePreferences parent, String name) {
 		super(parent, name);
 
 		if (parent instanceof DefaultPreferences)
			this.plugin = ((DefaultPreferences) parent).plugin;
 
 		// cache the segment count
 		String path = absolutePath();
 		segmentCount = getSegmentCount(path);
 		if (segmentCount < 2)
 			return;
 
 		// cache the qualifier
 		qualifier = getSegment(path, 1);
 	}
 
 	/*
 	 * Apply the values set in the bundle's install directory.
 	 * 
 	 * In Eclipse 2.1 this is equivalent to:
 	 *		/eclipse/plugins/<pluginID>/prefs.ini
 	 */
 	private void applyBundleDefaults() {
 		Bundle bundle = PreferencesOSGiUtils.getDefault().getBundle(name());
 		if (bundle == null)
 			return;
 		URL url = BundleFinder.find(bundle, new Path(IPreferencesConstants.PREFERENCES_DEFAULT_OVERRIDE_FILE_NAME));
 		if (url == null) {
 			if (EclipsePreferences.DEBUG_PREFERENCE_GENERAL)
 				PrefsMessages.message("Preference default override file not found for bundle: " + bundle.getSymbolicName()); //$NON-NLS-1$
 			return;
 		}
 		URL transURL = BundleFinder.find(bundle, NL_DIR.append(IPreferencesConstants.PREFERENCES_DEFAULT_OVERRIDE_BASE_NAME).addFileExtension(PROPERTIES_FILE_EXTENSION));
 		if (transURL == null && EclipsePreferences.DEBUG_PREFERENCE_GENERAL)
 			PrefsMessages.message("Preference translation file not found for bundle: " + bundle.getSymbolicName()); //$NON-NLS-1$
 		applyDefaults(name(), loadProperties(url), loadProperties(transURL));
 	}
 
 	/*
 	 * Apply the default values as specified in the file
 	 * as an argument on the command-line.
 	 */
 	private void applyCommandLineDefaults() {
 		// prime the cache the first time
 		if (commandLineCustomization == null) {
 			String filename = pluginCustomizationFile;
 			if (filename == null) {
 				if (EclipsePreferences.DEBUG_PREFERENCE_GENERAL)
 					PrefsMessages.message("Command-line preferences customization file not specified."); //$NON-NLS-1$
 				return;
 			}
 			if (EclipsePreferences.DEBUG_PREFERENCE_GENERAL)
 				PrefsMessages.message("Using command-line preference customization file: " + filename); //$NON-NLS-1$
 			commandLineCustomization = loadProperties(filename);
 		}
 		applyDefaults(null, commandLineCustomization, null);
 	}
 
 	/*
 	 * If the qualifier is null then the file is of the format:
 	 * 	pluginID/key=value
 	 * otherwise the file is of the format:
 	 * 	key=value
 	 */
 	private void applyDefaults(String id, Properties defaultValues, Properties translations) {
 		for (Enumeration e = defaultValues.keys(); e.hasMoreElements();) {
 			String fullKey = (String) e.nextElement();
 			String value = defaultValues.getProperty(fullKey);
 			if (value == null)
 				continue;
 			IPath childPath = new Path(fullKey);
 			String key = childPath.lastSegment();
 			childPath = childPath.removeLastSegments(1);
 			String localQualifier = id;
 			if (id == null) {
 				localQualifier = childPath.segment(0);
 				childPath = childPath.removeFirstSegments(1);
 			}
 			if (name().equals(localQualifier)) {
 				value = translatePreference(value, translations);
 				if (EclipsePreferences.DEBUG_PREFERENCE_SET)
 					PrefsMessages.message("Setting default preference: " + (new Path(absolutePath()).append(childPath).append(key)) + '=' + value); //$NON-NLS-1$
 				((EclipsePreferences) internalNode(childPath.toString(), false, null)).internalPut(key, value);
 			}
 		}
 	}
 
 	private void runInitializer(IConfigurationElement element) {
 		AbstractPreferenceInitializer initializer = null;
 		try {
 			initializer = (AbstractPreferenceInitializer) element.createExecutableExtension(ATTRIBUTE_CLASS);
 			initializer.initializeDefaultPreferences();
 		} catch (ClassCastException e) {
 			IStatus status = new Status(IStatus.ERROR, PrefsMessages.OWNER_NAME, IStatus.ERROR, PrefsMessages.preferences_invalidExtensionSuperclass, e);
 			log(status);
 		} catch (CoreException e) {
 			log(e.getStatus());
 		}
 	}
 
 	public IEclipsePreferences node(String childName, Object context) {
 		return internalNode(childName, true, context);
 	}
 
 	/*
 	 * Runtime defaults are the ones which are specified in code at runtime. 
 	 * 
 	 * In the Eclipse 2.1 world they were the ones which were specified in the
 	 * over-ridden Plugin#initializeDefaultPluginPreferences() method.
 	 * 
 	 * In Eclipse 3.0 they are set in the code which is indicated by the
 	 * extension to the plug-in default customizer extension point.
 	 */
 	private void applyRuntimeDefaults() {
 		IExtension[] extensions = PreferencesService.getPrefExtensions();
 		if (extensions.length == 0) {
 			if (EclipsePreferences.DEBUG_PREFERENCE_GENERAL)
 				PrefsMessages.message("Skipping runtime default preference customization."); //$NON-NLS-1$
 			return;
 		}
 		boolean foundInitializer = false;
 		for (int i = 0; i < extensions.length; i++) {
 			IConfigurationElement[] elements = extensions[i].getConfigurationElements();
 			for (int j = 0; j < elements.length; j++)
 				if (ELEMENT_INITIALIZER.equals(elements[j].getName())) {
 					if (name().equals(elements[j].getNamespace())) {
 						if (EclipsePreferences.DEBUG_PREFERENCE_GENERAL) {
 							IExtension theExtension = elements[j].getDeclaringExtension();
 							String extensionNamespace = theExtension.getNamespace();
 							Bundle underlyingBundle = PreferencesOSGiUtils.getDefault().getBundle(extensionNamespace);
 							String ownerName;
 							if (underlyingBundle != null)
 								ownerName = underlyingBundle.getSymbolicName();
 							else
 								ownerName = extensionNamespace;
 							PrefsMessages.message("Running default preference customization as defined by: " + ownerName); //$NON-NLS-1$
 						}
 						runInitializer(elements[j]);
 						// don't return yet in case we have multiple initializers registered
 						foundInitializer = true;
 					}
 				}
 		}
 		if (foundInitializer)
 			return;
 
 		// Do legacy plugin preference initialization
 		ILegacyPreferences initService = PreferencesOSGiUtils.getDefault().getLegacyPreferences();
 		if (initService != null)
			initService.init(plugin, name());
 	}
 
 	/*
 	 * Apply the default values as specified by the file
 	 * in the product extension.
 	 * 
 	 * In Eclipse 2.1 this is equivalent to the plugin_customization.ini
 	 * file in the primary feature's plug-in directory.
 	 */
 	private void applyProductDefaults() {
 		// prime the cache the first time
 		if (productCustomization == null) {
 			BundleContext context = Activator.getContext();
 			if (context != null) {
 				ServiceTracker productTracker = new ServiceTracker(context, IProductPreferencesService.class.getName(), null);
 				productTracker.open();
 				IProductPreferencesService productSpecials = (IProductPreferencesService) productTracker.getService();
 				if (productSpecials != null) {
 					productCustomization = productSpecials.getProductCustomization();
 					productTranslation = productSpecials.getProductTranslation();
 				}
 				productTracker.close();
 			} else
 				PrefsMessages.message("Product-specified preferences called before plugin is started"); //$NON-NLS-1$
 		}
 		applyDefaults(null, productCustomization, productTranslation);
 	}
 
 	/* (non-Javadoc)
 	 * @see org.osgi.service.prefs.Preferences#flush()
 	 */
 	public void flush() {
 		// default values are not persisted
 	}
 
 	protected IEclipsePreferences getLoadLevel() {
 		if (loadLevel == null) {
 			if (qualifier == null)
 				return null;
 			// Make it relative to this node rather than navigating to it from the root.
 			// Walk backwards up the tree starting at this node.
 			// This is important to avoid a chicken/egg thing on startup.
 			EclipsePreferences node = this;
 			for (int i = 2; i < segmentCount; i++)
 				node = (EclipsePreferences) node.parent();
 			loadLevel = node;
 		}
 		return loadLevel;
 	}
 
 	protected EclipsePreferences internalCreate(EclipsePreferences nodeParent, String nodeName, Object context) {
 		return new DefaultPreferences(nodeParent, nodeName, context);
 	}
 
 	protected boolean isAlreadyLoaded(IEclipsePreferences node) {
 		return loadedNodes.contains(node.name());
 	}
 
 	/* (non-Javadoc)
 	 * @see org.eclipse.core.internal.preferences.EclipsePreferences#load()
 	 */
 	protected void load() {
 		loadDefaults();
 	}
 
 	private void loadDefaults() {
 		applyRuntimeDefaults();
 		applyBundleDefaults();
 		applyProductDefaults();
 		applyCommandLineDefaults();
 	}
 
 	private Properties loadProperties(URL url) {
 		Properties result = new Properties();
 		if (url == null)
 			return result;
 		InputStream input = null;
 		try {
 			input = url.openStream();
 			result.load(input);
 		} catch (IOException e) {
 			if (EclipsePreferences.DEBUG_PREFERENCE_GENERAL) {
 				PrefsMessages.message("Problem opening stream to preference customization file: " + url); //$NON-NLS-1$
 				e.printStackTrace();
 			}
 		} finally {
 			if (input != null)
 				try {
 					input.close();
 				} catch (IOException e) {
 					// ignore
 				}
 		}
 		return result;
 	}
 
 	private Properties loadProperties(String filename) {
 		Properties result = new Properties();
 		InputStream input = null;
 		try {
 			input = new BufferedInputStream(new FileInputStream(filename));
 			result.load(input);
 		} catch (FileNotFoundException e) {
 			if (EclipsePreferences.DEBUG_PREFERENCE_GENERAL)
 				PrefsMessages.message("Preference customization file not found: " + filename); //$NON-NLS-1$
 		} catch (IOException e) {
 			String message = NLS.bind(PrefsMessages.preferences_loadException, filename);
 			IStatus status = new Status(IStatus.ERROR, PrefsMessages.OWNER_NAME, IStatus.ERROR, message, e);
 			RuntimeLog.log(status);
 		} finally {
 			if (input != null)
 				try {
 					input.close();
 				} catch (IOException e) {
 					// ignore
 				}
 		}
 		return result;
 	}
 
 	protected void loaded() {
 		loadedNodes.add(name());
 	}
 
 	/* (non-Javadoc)
 	 * @see org.osgi.service.prefs.Preferences#sync()
 	 */
 	public void sync() {
 		// default values are not persisted
 	}
 
 	/**
 	 * Takes a preference value and a related resource bundle and
 	 * returns the translated version of this value (if one exists).
 	 */
 	private String translatePreference(String value, Properties props) {
 		value = value.trim();
 		if (props == null || value.startsWith(KEY_DOUBLE_PREFIX))
 			return value;
 		if (value.startsWith(KEY_PREFIX)) {
 			int ix = value.indexOf(" "); //$NON-NLS-1$
 			String key = ix == -1 ? value.substring(1) : value.substring(1, ix);
 			String dflt = ix == -1 ? value : value.substring(ix + 1);
 			return props.getProperty(key, dflt);
 		}
 		return value;
 	}
 }
