 /*******************************************************************************
  * Copyright (c) 2000, 2005 IBM Corporation and others.
  * All rights reserved. This program and the accompanying materials
  * are made available under the terms of the Eclipse Public License v1.0
  * which accompanies this distribution, and is available at
  * http://www.eclipse.org/legal/epl-v10.html
  * 
  * Contributors:
  *     IBM Corporation - initial API and implementation
  *******************************************************************************/
 package org.eclipse.core.internal.resources;
 
 import java.io.IOException;
 import java.net.MalformedURLException;
 import java.net.URL;
 import org.eclipse.core.internal.boot.PlatformURLConnection;
 import org.eclipse.core.internal.boot.PlatformURLHandler;
 import org.eclipse.core.internal.utils.Messages;
 import org.eclipse.core.resources.IProject;
 import org.eclipse.core.resources.ResourcesPlugin;
 import org.eclipse.core.runtime.IPath;
 import org.eclipse.core.runtime.Path;
 import org.eclipse.osgi.util.NLS;
 
 /**
  * Platform URL support
  * platform:/resource/<path>/<resource>  maps to resource in current workspace
  */
 public class PlatformURLResourceConnection extends PlatformURLConnection {
 
 	// resource/ protocol
 	public static final String RESOURCE = "resource"; //$NON-NLS-1$
 	public static final String RESOURCE_URL_STRING = PlatformURLHandler.PROTOCOL + PlatformURLHandler.PROTOCOL_SEPARATOR + "/" + RESOURCE + "/"; //$NON-NLS-1$ //$NON-NLS-2$
 	private static URL rootURL;
 
 	public PlatformURLResourceConnection(URL url) {
 		super(url);
 	}
 
 	protected boolean allowCaching() {
 		return false; // don't cache, workspace is local
 	}
 
 	protected URL resolve() throws IOException {
 		IPath spec = new Path(url.getFile().trim()).makeRelative();
 		if (!spec.segment(0).equals(RESOURCE))
 			throw new IOException(NLS.bind(Messages.url_badVariant, url));
 		int count = spec.segmentCount();
 		// if there is only one segment then we are talking about the workspace root.
 		if (count == 1)
 			return rootURL;
 		// if there are two segments then the second is a project name.
 		IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(spec.segment(1));
 		if (!project.exists()) {
 			String message = NLS.bind(Messages.url_couldNotResolve, project.getName(), url.toExternalForm());
 			throw new IOException(message);
 		}
 		IPath result = null;
 		if (count == 2)
 			result = project.getLocation();
 		else {
 			spec = spec.removeFirstSegments(2);
 			result = project.getFile(spec).getLocation();
 		}
 		return new URL("file", "", result.toString()); //$NON-NLS-1$ //$NON-NLS-2$
 	}
 
 	/**
 	 * This method is called during resource plugin startup() initialization.
 	 * 
 	 * @param root URL to the root of the current workspace.
 	 */
 	public static void startup(IPath root) {
 		// register connection type for platform:/resource/ handling
 		if (rootURL != null)
 			return;
 		try {
			rootURL = root.toFile().toURL();
 		} catch (MalformedURLException e) {
 			// should never happen but if it does, the resource URL cannot be supported.
 			return;
 		}
 		PlatformURLHandler.register(RESOURCE, PlatformURLResourceConnection.class);
 	}
 }
