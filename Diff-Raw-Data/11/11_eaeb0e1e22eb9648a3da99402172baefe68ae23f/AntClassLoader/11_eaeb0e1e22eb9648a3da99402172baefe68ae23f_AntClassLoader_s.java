 package org.eclipse.ant.internal.core;
 
 /**********************************************************************
 Copyright (c) 2000, 2002 IBM Corp.  All rights reserved.
 This file is made available under the terms of the Common Public License v1.0
 which accompanies this distribution, and is available at
 http://www.eclipse.org/legal/cpl-v10.html
 **********************************************************************/
 
 import java.net.URL;
 import java.net.URLClassLoader;
 
 public class AntClassLoader extends URLClassLoader {
 
 	protected ClassLoader[] pluginLoaders;
 
 	public AntClassLoader(URL[] urls, ClassLoader[] pluginLoaders) {
 		super(urls, null);
 		this.pluginLoaders = pluginLoaders;
 	}
 
 	public Class loadClass(String name) throws ClassNotFoundException {
 		Class result = loadClassURLs(name);
		if (result == null) {
 			result = loadClassPlugins(name);
 		}
 		if (result == null) {
 			throw new ClassNotFoundException(name);
 		}
 		return result;
 	}
 
 	protected Class loadClassURLs(String name) {
 		try {
 			return super.loadClass(name);
 		} catch (ClassNotFoundException e) {
 			// Ignore exception now. If necessary we'll throw
 			// a ClassNotFoundException in loadClass(String)
 		}
 		return null;
 	}
 
 	protected Class loadClassPlugins(String name) {
 		Class result = null;
 		if (pluginLoaders != null) {
			for (int i = 0;
				(i < pluginLoaders.length) && (result == null);
				i++) {
 				try {
 					result = pluginLoaders[i].loadClass(name);
 				} catch (ClassNotFoundException e) {
 					// Ignore exception now. If necessary we'll throw
 					// a ClassNotFoundException in loadClass(String)
 				}
 			}
 		}
 		return result;
 	}
 }
