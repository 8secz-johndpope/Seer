 /*******************************************************************************
  * Copyright (c) 2003 IBM Corporation and others.
  * All rights reserved. This program and the accompanying materials 
  * are made available under the terms of the Common Public License v1.0
  * which accompanies this distribution, and is available at
  * http://www.eclipse.org/legal/cpl-v10.html
  * 
  * Contributors:
  *     IBM Corporation - initial API and implementation
  *******************************************************************************/
 package org.eclipse.core.runtime.adaptor;
 
 import java.util.Dictionary;
 import java.util.Enumeration;
 import org.eclipse.osgi.framework.adaptor.Version;
 import org.eclipse.osgi.framework.internal.core.Constants;
 import org.osgi.framework.BundleException;
 
 public class CachedManifest extends Dictionary {
 
 	Dictionary manifest = null;
 	EclipseBundleData bundledata;
 	
 	public CachedManifest(EclipseBundleData bundledata) {
 		this.bundledata = bundledata;
 	}
 	
 	protected Dictionary getManifest() {
 		if (manifest == null)
 			try {
 				manifest = bundledata.loadManifest();
 			} catch (BundleException e) {
 				return null;
 			}
 		return manifest;
 	}
 
 	public int size() {
 		return getManifest().size();
 	}
 
 	public boolean isEmpty() {
 		return false;
 	}
 
 	public Enumeration elements() {
 		return getManifest().elements();
 	}
 
 	public Enumeration keys() {
 		return getManifest().keys();
 	}
 
 	public Object get(Object key) {
		String keyString = (String) key; 
		if (Constants.BUNDLE_VERSION.equalsIgnoreCase(keyString)) {
 			Version result = bundledata.getVersion();
 			return result == null ? null : result.toString();
 		}
		if ("plugin-class".equalsIgnoreCase(keyString))
 			return bundledata.getPluginClass();
		if ("legacy".equalsIgnoreCase(keyString))
 			return bundledata.isLegacy();
		if (Constants.BUNDLE_GLOBALNAME.equalsIgnoreCase(keyString))
			return bundledata.getUniqueId();
 		return getManifest().get(key);
 	}
 
 	public Object remove(Object key) {
 		return getManifest().remove(key);
 	}
 
 	public Object put(Object key, Object value) {
 		return getManifest().put(key, value);
 	}
 
 }
