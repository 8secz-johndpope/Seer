 /*******************************************************************************
 * Copyright (c) 2004, 2008, 2009 Red Hat, Inc.
  * All rights reserved. This program and the accompanying materials
  * are made available under the terms of the Eclipse Public License v1.0
  * which accompanies this distribution, and is available at
  * http://www.eclipse.org/legal/epl-v10.html
  *
  * Contributors:
  *    Keith Seitz <keiths@redhat.com> - initial API and implementation
  *    Kent Sebastian <ksebasti@redhat.com> - 
  *******************************************************************************/ 
 
 package org.eclipse.linuxtools.oprofile.ui;
 
 import org.eclipse.core.resources.IWorkspace;
 import org.eclipse.core.resources.ResourcesPlugin;
 import org.eclipse.jface.resource.ImageDescriptor;
 import org.eclipse.linuxtools.oprofile.ui.view.OprofileView;
 import org.eclipse.swt.widgets.Shell;
 import org.eclipse.ui.IWorkbenchWindow;
 import org.eclipse.ui.plugin.AbstractUIPlugin;
 import org.osgi.framework.BundleContext;
 
 
 /**
  * The main plugin class to be used in the desktop.
  */
 public class OprofileUiPlugin extends AbstractUIPlugin {
 	//The shared instance.
 	private static OprofileUiPlugin plugin;
 
 	private OprofileView _oprofileview = null;
 	
 	public static final String ID_PLUGIN = "org.eclipse.linuxtools.oprofile.ui"; //$NON-NLS-1$
 	public static final String ID_OPROFILE_VIEW = ID_PLUGIN + ".OProfileView";
 
 	private static final String ICON_PATH = "icons/";
 	public static final String SESSION_ICON = ICON_PATH + "session.gif";
 	public static final String EVENT_ICON = ICON_PATH + "event.gif";
 	public static final String IMAGE_ICON = ICON_PATH + "image.gif";
 	public static final String DEPENDENT_ICON = ICON_PATH + "dependent.gif";
 	public static final String SYMBOL_ICON = ICON_PATH + "symbol.gif";
 	public static final String SAMPLE_ICON = ICON_PATH + "sample.gif";
 	
 	public static final double MINIMUM_SAMPLE_PERCENTAGE = 0.0001;
 	
 	
 	/**
 	 * The constructor.
 	 */
 	public OprofileUiPlugin() {
 		plugin = this;
 	}
 
 	/**
 	 * This method is called upon plug-in activation
 	 */
 	public void start(BundleContext context) throws Exception {
 		super.start(context);
 	}
 
 	/**
 	 * This method is called when the plug-in is stopped
 	 */
 	public void stop(BundleContext context) throws Exception {
 		super.stop(context);
 		plugin = null;
 	}
 	
 	
 	/**
 	 * Returns the shared instance.
 	 */
 	public static OprofileUiPlugin getDefault() {
 		return plugin;
 	}
 
 	/**
 	 * Returns the workspace instance.
 	 */
 	public static IWorkspace getWorkspace() {
 		return ResourcesPlugin.getWorkspace();
 	}
 
 	/**	
 	 * Returns an image descriptor for the image file at the given
 	 * plug-in relative path
 	 *
 	 * @param path the path
 	 * @return the image descriptor
 	 */
 	public static ImageDescriptor getImageDescriptor(String path) {
 		return imageDescriptorFromPlugin(ID_PLUGIN, path);
 	}
 	
 	
 	public OprofileView getOprofileView() {
 		return _oprofileview;
 	}
 
 	public void setOprofileView(OprofileView _oprofileview) {
 		this._oprofileview = _oprofileview;
 	}
 
 	public static IWorkbenchWindow getActiveWorkbenchWindow() {
 		return getDefault().getWorkbench().getActiveWorkbenchWindow();
 	}
 	
 	public static Shell getActiveWorkbenchShell() {
 		IWorkbenchWindow window = getActiveWorkbenchWindow();
 		if (window != null) {
 			return window.getShell();
 		}
 		return null;
 	}
 }
