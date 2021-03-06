 /*******************************************************************************
  * Copyright (c) 2003, 2004 IBM Corporation and others.
  * All rights reserved. This program and the accompanying materials 
  * are made available under the terms of the Common Public License v1.0
  * which accompanies this distribution, and is available at
  * http://www.eclipse.org/legal/cpl-v10.html
  * 
  * Contributors:
  *     IBM Corporation - initial API and implementation
  *******************************************************************************/
 package org.eclipse.core.internal.runtime;
 
 import java.lang.reflect.Method;
 import org.eclipse.core.runtime.IPluginDescriptor;
 import org.eclipse.core.runtime.Plugin;
 import org.osgi.framework.Bundle;
 
 //This class isolates calls to the backward compatibility layer.
 //It uses reflection so it can be loaded with success even in the absence of the compatibility plugin.
 public class CompatibilityHelper {
 
 	public static final String PI_RUNTIME_COMPATIBILITY = "org.eclipse.core.runtime.compatibility"; //$NON-NLS-1$
 	public static Bundle compatibility = null;
 	
 	public static Bundle getCompatibility() {
 		if (compatibility == null)
 			compatibility = org.eclipse.core.internal.runtime.InternalPlatform.getDefault().getBundle(PI_RUNTIME_COMPATIBILITY);
 		return compatibility;
 	}
 
 	public static void setPlugin(IPluginDescriptor descriptor, Plugin plugin) {
 		//Here we use reflection so the runtime code can run without the compatibility plugin
 		if (getCompatibility() == null)
 			throw new IllegalStateException();
 
 		try {
 			Method setPlugin = descriptor.getClass().getMethod("setPlugin", new Class[] {Plugin.class}); //$NON-NLS-1$
 			setPlugin.invoke(descriptor, new Object[] {plugin});
 		} catch (Exception e) {
 			//Ignore the exceptions
 		}
 	}
 
 	public static IPluginDescriptor getPluginDescriptor(String pluginId) {
 		//Here we use reflection so the runtime code can run without the compatibility
 		Bundle compatibility = getCompatibility();
 		if (compatibility == null)
 			throw new IllegalStateException();
 
 		Class oldInternalPlatform = null;
 		try {
 			oldInternalPlatform = compatibility.loadClass("org.eclipse.core.internal.plugins.InternalPlatform"); //$NON-NLS-1$
 			Method getPluginDescriptor = oldInternalPlatform.getMethod("getPluginDescriptor", new Class[] {String.class}); //$NON-NLS-1$
 			return (IPluginDescriptor) getPluginDescriptor.invoke(oldInternalPlatform, new Object[] {pluginId});
 		} catch (Exception e) {
 			//Ignore the exceptions, return false
 		}
 		return null;
 	}
 
 	public static void setActive(IPluginDescriptor descriptor) {
 		Bundle compatibility = getCompatibility();
 		if (compatibility == null)
 			throw new IllegalStateException();
 
 		try {
 			Method setPlugin = descriptor.getClass().getMethod("setActive", null); //$NON-NLS-1$
 			setPlugin.invoke(descriptor, null);
 		} catch (Exception e) {
 			//Ignore the exceptions
 		}
 	}
 	
	public static boolean activationPending(IPluginDescriptor descriptor) {
 		Bundle compatibility = getCompatibility();
 		if (compatibility == null)
 			throw new IllegalStateException();
 
 		Boolean result = new Boolean(false);
 		try{
			Method setPlugin = descriptor.getClass().getMethod("isActivating", null); //$NON-NLS-1$
 			result = (Boolean) setPlugin.invoke(descriptor, null);
 		} catch(Exception e) {
 			//Ignore the exceptions			
 		}
 		return result.booleanValue();
 	}
 }
