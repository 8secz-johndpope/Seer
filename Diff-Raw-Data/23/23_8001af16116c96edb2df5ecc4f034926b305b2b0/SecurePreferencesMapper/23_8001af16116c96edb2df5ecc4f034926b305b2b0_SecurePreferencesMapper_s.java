 /*******************************************************************************
  * Copyright (c) 2008 IBM Corporation and others.
  * All rights reserved. This program and the accompanying materials
  * are made available under the terms of the Eclipse Public License v1.0
  * which accompanies this distribution, and is available at
  * http://www.eclipse.org/legal/epl-v10.html
  * 
  * Contributors:
  *     IBM Corporation - initial API and implementation
  *******************************************************************************/
 package org.eclipse.equinox.internal.security.storage;
 
 import java.io.*;
 import java.net.URL;
 import java.util.*;
 import javax.crypto.spec.PBEKeySpec;
 import org.eclipse.equinox.internal.security.auth.AuthPlugin;
 import org.eclipse.equinox.internal.security.auth.nls.SecAuthMessages;
 import org.eclipse.equinox.security.storage.ISecurePreferences;
 import org.eclipse.equinox.security.storage.provider.IProviderHints;
 import org.eclipse.osgi.service.environment.EnvironmentInfo;
 import org.eclipse.osgi.util.NLS;
 
 public class SecurePreferencesMapper {
 
 	/**
 	 * Command line argument specifying default location
 	 */
 	final private static String KEYRING_ARGUMENT = "-eclipse.keyring"; //$NON-NLS-1$
 
 	/**
 	 * Command line argument specifying default password
 	 */
 	final private static String PASSWORD_ARGUMENT = "-eclipse.password"; //$NON-NLS-1$
 
 	static private ISecurePreferences defaultPreferences = null;
 
 	static private Map preferences = new HashMap(); // URL.toString() -> SecurePreferencesRoot
 
 	static public ISecurePreferences getDefault() {
 		if (defaultPreferences == null) {
 			try {
 				defaultPreferences = open(null, null);
 			} catch (IOException e) {
 				AuthPlugin.getDefault().logError(SecAuthMessages.keyringNotAvailable, e);
 			}
 		}
 		return defaultPreferences;
 	}
 
 	static public void clearDefault() {
 		if (defaultPreferences == null)
 			return;
 
 		try {
 			defaultPreferences.flush();
 		} catch (IOException e) {
 			// ignore in this context
 		}
 		close((((SecurePreferencesWrapper) defaultPreferences).getContainer().getRootData()));
 		defaultPreferences = null;
 	}
 
 	static public ISecurePreferences open(URL location, Map options) throws IOException {
 		// 1) find if there are any command line arguments that need to be added
 		EnvironmentInfo infoService = AuthPlugin.getDefault().getEnvironmentInfoService();
 		if (infoService != null) {
 			String[] args = infoService.getNonFrameworkArgs();
 			if (args != null && args.length != 0) {
 				for (int i = 0; i < args.length - 1; i++) {
 					if (args[i + 1].startsWith(("-"))) //$NON-NLS-1$
 						continue;
 					if (location == null && KEYRING_ARGUMENT.equalsIgnoreCase(args[i])) {
 						location = new File(args[i + 1]).toURL(); // don't use File.toURI().toURL()
 						continue;
 					}
 					if (PASSWORD_ARGUMENT.equalsIgnoreCase(args[i])) {
 						options = processPassword(options, args[i + 1]);
 						continue;
 					}
 				}
 			}
 		}
 
 		// 2) process location
 		if (location == null)
 			location = StorageUtils.getDefaultLocation();
 		if (!StorageUtils.isFile(location))
 			// at this time we only accept file URLs; check URL type right away
 			throw new IOException(NLS.bind(SecAuthMessages.loginFileURL, location.toString()));
 
 		// 3) see if there is already SecurePreferencesRoot at that location; if not open a new one
 		String key = location.toString();
 		SecurePreferencesRoot root;
 		if (preferences.containsKey(key))
 			root = (SecurePreferencesRoot) preferences.get(key);
 		else {
 			root = new SecurePreferencesRoot(location);
 			preferences.put(key, root);
 		}
 
 		// 4) create container with the options passed in
 		SecurePreferencesContainer container = new SecurePreferencesContainer(root, options);
 		return container.getPreferences();
 	}
 
 	static public void stop() {
 		synchronized (preferences) {
 			for (Iterator i = preferences.values().iterator(); i.hasNext();) {
 				SecurePreferencesRoot provider = (SecurePreferencesRoot) i.next();
 				try {
 					provider.flush();
 				} catch (IOException e) {
 					// use FrameworkLog directly for shutdown messages - RuntimeLog
 					// is empty by this time
					AuthPlugin.getDefault().frameworkLogError(SecAuthMessages.errorOnSave, e);
 				}
 			}
 			preferences.clear();
 		}
 	}
 
 	static public void clearPasswordCache() {
 		synchronized (preferences) {
 			for (Iterator i = preferences.values().iterator(); i.hasNext();) {
 				SecurePreferencesRoot provider = (SecurePreferencesRoot) i.next();
 				provider.clearPasswordCache();
 			}
 		}
 	}
 
 	// Not exposed as API; mostly intended for testing
 	static public void close(SecurePreferencesRoot root) {
 		if (root == null)
 			return;
 		synchronized (preferences) {
 			for (Iterator i = preferences.values().iterator(); i.hasNext();) {
 				SecurePreferencesRoot provider = (SecurePreferencesRoot) i.next();
 				if (!root.equals(provider))
 					continue;
 				i.remove();
 				break;
 			}
 		}
 	}
 
 	static private Map processPassword(Map options, String arg) {
 		if (arg == null || arg.length() == 0)
 			return options;
 		File file = new File(arg);
 		if (!file.canRead()) {
 			String msg = NLS.bind(SecAuthMessages.unableToReadPswdFile, arg);
 			AuthPlugin.getDefault().logError(msg, null);
 			return options;
 		}
 		BufferedReader is = null;
 		try {
 			is = new BufferedReader(new FileReader(file));
 			StringBuffer buffer = new StringBuffer();
 			for (;;) { // this eliminates new line characters but that's fine
 				String tmp = is.readLine();
 				if (tmp == null)
 					break;
 				buffer.append(tmp);
 			}
 			if (buffer.length() == 0)
 				return options;
 			if (options == null)
 				options = new HashMap(1);
 			if (!options.containsKey(IProviderHints.DEFAULT_PASSWORD))
 				options.put(IProviderHints.DEFAULT_PASSWORD, new PBEKeySpec(buffer.toString().toCharArray()));
 		} catch (IOException e) {
 			String msg = NLS.bind(SecAuthMessages.unableToReadPswdFile, arg);
 			AuthPlugin.getDefault().logError(msg, e);
 		} finally {
 			if (is != null) {
 				try {
 					is.close();
 				} catch (IOException e) {
 					String msg = NLS.bind(SecAuthMessages.unableToReadPswdFile, arg);
 					AuthPlugin.getDefault().logError(msg, e);
 				}
 			}
 		}
 		return options;
 	}
 
 }
