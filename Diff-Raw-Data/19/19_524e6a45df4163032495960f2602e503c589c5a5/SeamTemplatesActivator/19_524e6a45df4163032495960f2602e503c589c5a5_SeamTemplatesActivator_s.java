 /*******************************************************************************
  * Copyright (c) 2007 Exadel, Inc. and Red Hat, Inc.
  * Distributed under license by Red Hat, Inc. All rights reserved.
  * This program is made available under the terms of the
  * Eclipse Public License v1.0 which accompanies this distribution,
  * and is available at http://www.eclipse.org/legal/epl-v10.html
  *
  * Contributors:
  *     Exadel, Inc. and Red Hat, Inc. - initial API and implementation
  ******************************************************************************/ 
 package org.jboss.tools.jsf.vpe.seam;
 
 import java.io.IOException;
 import java.net.URL;
 
 import org.eclipse.core.runtime.FileLocator;
 import org.eclipse.core.runtime.Platform;
 import org.jboss.tools.common.log.BaseUIPlugin;
 import org.jboss.tools.common.log.IPluginLog;
 import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
 
 /**
  * @author Vitali (vyemialyanchyk@exadel.com)
  * 
  * The activator class controls the plug-in life cycle
  */
 public class SeamTemplatesActivator extends BaseUIPlugin {
 
 	// The plug-in ID
 	public static final String PLUGIN_ID = "org.jboss.tools.jsf.vpe.seam"; //$NON-NLS-1$
 
 	// The shared instance
 	private static SeamTemplatesActivator plugin;
 
 	/**
 	 * The constructor
 	 */
 	public SeamTemplatesActivator() {
 		plugin = this;
 	}
 
	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#start(org.osgi.framework.BundleContext)
	 */
	public void start(BundleContext context) throws Exception {
		super.start(context);
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#stop(org.osgi.framework.BundleContext)
	 */
	public void stop(BundleContext context) throws Exception {
		plugin = null;
		super.stop(context);
	}

 	/**
 	 * Returns the shared instance
 	 *
 	 * @return the shared instance
 	 */
 	public static SeamTemplatesActivator getDefault() {
 		return plugin;
 	}
 	
 	public static IPluginLog getPluginLog() {
 		return getDefault();
 	}
 	
 	public static String getPluginResourcePath() {
 		Bundle bundle = Platform.getBundle(PLUGIN_ID);
 		URL url = null;
 		try {
 			url = bundle == null ? null : FileLocator.resolve(bundle.getEntry("/resources")); //$NON-NLS-1$
 		} catch (IOException e) {
 			url = bundle.getEntry("/resources"); //$NON-NLS-1$
 		}
 		return (url == null) ? null : url.getPath();
 	}
 }
