 /*******************************************************************************
  * Copyright (c) 2011 IBM Corporation
  * All rights reserved. This program and the accompanying materials
  * are made available under the terms of the Eclipse Public License v1.0
  * which accompanies this distribution, and is available at
  * http://www.eclipse.org/legal/epl-v10.html
  *
  * Contributors:
  *    Otavio Busatto Pontes <obusatto@br.ibm.com> - initial API and implementation
  *******************************************************************************/
 
 package org.eclipse.linuxtools.tools.launch.core.properties;
 
 import org.eclipse.core.resources.IProject;
 import org.eclipse.core.resources.ProjectScope;
 import org.eclipse.core.runtime.IConfigurationElement;
 import org.eclipse.core.runtime.IExtensionPoint;
 import org.eclipse.core.runtime.Platform;
 import org.eclipse.linuxtools.tools.launch.core.Activator;
 import org.eclipse.ui.preferences.ScopedPreferenceStore;
 
 public class LinuxtoolsPathProperty {
 	public static final String LINUXTOOLS_PATH_NAME = Activator.PLUGIN_ID + ".LinuxtoolsPath"; //$NON-NLS-1$
 	public static final String LINUXTOOLS_PATH_SYSTEM_NAME = Activator.PLUGIN_ID + ".LinuxtoolsSystemEnvPath"; //$NON-NLS-1$
 	private static final String LINUXTOOLS_PATH_EXT_POINT = "LinuxtoolsPathOptions"; //$NON-NLS-1$
 	private static final String LINUXTOOLS_PATH_OPTION = "option"; //$NON-NLS-1$
 	private static final String LINUXTOOLS_PATH_OPTION_PATH = "path"; //$NON-NLS-1$
 	private static final String LINUXTOOLS_PATH_OPTION_DEFAULT = "default"; //$NON-NLS-1$
 	private String linuxtoolsPathDefault = ""; //$NON-NLS-1$
 	private boolean linuxtoolsPathSystemDefault = true;
 	private static LinuxtoolsPathProperty instance = null;
 
 
 	private void fillLinuxtoolsPath(String path) {
 		if (path == null)
 			return;
 		if (!path.equals("")) {
 			linuxtoolsPathSystemDefault = false;
 			linuxtoolsPathDefault = path;
 		}
 	}
 
 	private LinuxtoolsPathProperty() {
 		IExtensionPoint extPoint = Platform.getExtensionRegistry().getExtensionPoint(Activator.UI_PLUGIN_ID, LINUXTOOLS_PATH_EXT_POINT);
 		if (extPoint != null) {
 			IConfigurationElement[] configs = extPoint.getConfigurationElements();
 			for (IConfigurationElement config : configs)
 				if (config.getName().equals(LINUXTOOLS_PATH_OPTION)) {
 					String sdefault = config.getAttribute(LINUXTOOLS_PATH_OPTION_DEFAULT);
 					if (sdefault != null && sdefault.equals(Boolean.toString(true))) {
 						fillLinuxtoolsPath(config.getAttribute(LINUXTOOLS_PATH_OPTION_PATH));
 						break;
 					}
 				}
 		}
 	}
 
 	public static LinuxtoolsPathProperty getInstance() {
 		if (instance == null)
 			instance = new LinuxtoolsPathProperty();
 		return instance;
 	}
 
 	public String getLinuxtoolsPath(IProject project) {
 		if (project == null)
 			return null;
 
 		ScopedPreferenceStore store = new ScopedPreferenceStore(
 				new ProjectScope(project),
 				Activator.PLUGIN_ID);
 
 		//If the value is not stored we use the default
 		boolean systemPathSelected;
 		if (store.contains(LINUXTOOLS_PATH_SYSTEM_NAME))
 			systemPathSelected = store.getBoolean(LINUXTOOLS_PATH_SYSTEM_NAME);
 		else
 			systemPathSelected = getLinuxtoolsPathSystemDefault();
 
 		if (systemPathSelected)
			return getLinuxtoolsPathDefault();
 
		String path = store.getString(LINUXTOOLS_PATH_NAME);
		if (path == null || path.equals(""))
 			return getLinuxtoolsPathDefault();
 		return path;
 	}
 
 	public String getLinuxtoolsPathDefault() {
 		return linuxtoolsPathDefault;
 	}
 
 	public boolean getLinuxtoolsPathSystemDefault() {
 		return linuxtoolsPathSystemDefault;
 	}
 }
