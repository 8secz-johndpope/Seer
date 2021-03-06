 /*******************************************************************************
  * Copyright (c) 2005, 2008 IBM Corporation and others.
  * All rights reserved. This program and the accompanying materials
  * are made available under the terms of the Eclipse Public License v1.0
  * which accompanies this distribution, and is available at
  * http://www.eclipse.org/legal/epl-v10.html
  *
  * Contributors:
  *     IBM Corporation - initial API and implementation
  *******************************************************************************/
 package org.eclipse.equinox.internal.provisional.p2.engine;
 
 import java.util.Map;
 import org.eclipse.core.runtime.IProgressMonitor;
 import org.eclipse.equinox.internal.provisional.p2.metadata.IInstallableUnit;
 import org.eclipse.equinox.internal.provisional.p2.query.*;
 
 public interface IProfile extends IQueryable {
 
 	/**
 	 * Profile property constant indicating the flavor for the profile.
 	 */
 	public static final String PROP_FLAVOR = "org.eclipse.equinox.p2.flavor"; //$NON-NLS-1$
 	/**
 	 * Profile property constant indicating the install folder for the profile.
 	 */
 	public static final String PROP_INSTALL_FOLDER = "org.eclipse.equinox.p2.installFolder"; //$NON-NLS-1$
 	/**
 	 * Profile property constant indicating the configuration folder for the profile.
 	 */
 	public static final String PROP_CONFIGURATION_FOLDER = "org.eclipse.equinox.p2.configurationFolder"; //$NON-NLS-1$
 	/**
 	 * Profile property constant indicating the location of the launcher configuration file for the profile.
 	 */
 	public static final String PROP_LAUNCHER_CONFIGURATION = "org.eclipse.equinox.p2.launcherConfiguration"; //$NON-NLS-1$
 
 	/**
 	 * Profile property constant indicating the installed language(s) for the profile.
 	 */
 	public static final String PROP_NL = "org.eclipse.equinox.p2.nl"; //$NON-NLS-1$
 	/**
 	 * Profile property constant for a string property indicating a user visible short 
 	 * textual description of this profile. May be empty or <code>null</code>, and 
 	 * generally will be for non-top level install contexts.
 	 */
 	public static final String PROP_DESCRIPTION = "org.eclipse.equinox.p2.description"; //$NON-NLS-1$
 	/**
 	 * Profile property constant for a string property indicating a user visible name of this profile.
 	 * May be empty or <code>null</code>, and generally will be for non-top level
 	 * install contexts.
 	 */
 	public static final String PROP_NAME = "org.eclipse.equinox.p2.name"; //$NON-NLS-1$
 	/**
 	 * Profile property constant indicating the list of environments
 	 * (e.g., OS, WS, ...) in which a profile can operate. The value of the property
 	 * is a comma-delimited string of key/value pairs.
 	 */
 	public static final String PROP_ENVIRONMENTS = "org.eclipse.equinox.p2.environments"; //$NON-NLS-1$
 	/**
 	 * Profile property constant for a boolean property indicating if the profiling
 	 * is roaming.  A roaming profile is one whose physical install location varies
 	 * and is updated whenever it runs.
 	 */
 	public static final String PROP_ROAMING = "org.eclipse.equinox.p2.roaming"; //$NON-NLS-1$
 	/**
 	 * Profile property constant indicating the bundle pool cache location.
 	 */
 	public static final String PROP_CACHE = "org.eclipse.equinox.p2.cache"; //$NON-NLS-1$
 
 	/**
 	 * Profile property constant indicating a shared read-only bundle pool cache location.
 	 */
 	public static final String PROP_SHARED_CACHE = "org.eclipse.equinox.p2.cache.shared"; //$NON-NLS-1$
 
 	/**
 	 * Profile property constant for a boolean property indicating if update features should
 	 * be installed in this profile
 	 */
 	public static final String PROP_INSTALL_FEATURES = "org.eclipse.update.install.features"; //$NON-NLS-1$
 
	/**
	 * Profile property constant for a String property indicating additional metadata repositories 
	 * associated with this profile. The format of the property value is a comma-separated 
	 * list of repository location URIs, where any literal comma characters are encoded 
	 * with the corresponding unicode escape sequence (${#44}).
	 */
	public static final String PROP_METADATA_REPOSITORIES = "org.eclipse.equinox.p2.metadataRepositories"; //$NON-NLS-1$

	/**
	 * Profile property constant for a String property indicating additional artifact repositories 
	 * associated with this profile. The format of the property value is a comma-separated 
	 * list of repository location URIs, where any literal comma characters are encoded 
	 * with the corresponding unicode escape sequence (${#44}).
	 */
	public static final String PROP_ARTIFACT_REPOSITORIES = "org.eclipse.equinox.p2.artifactRepositories"; //$NON-NLS-1$

 	public String getProfileId();
 
 	public IProfile getParentProfile();
 
 	/*
 	 * 	A profile is a root profile if it is not a sub-profile
 	 * 	of another profile.
 	 */
 	public boolean isRootProfile();
 
 	public boolean hasSubProfiles();
 
 	public String[] getSubProfileIds();
 
 	/**
 	 * Get the stored value associated with the given key.
 	 * If the profile is a sub-profile and there is no value
 	 * locally associated with the key, then the chain
 	 * of parent profiles will be traversed to get an associated
 	 * value from the nearest ancestor.
 	 *  
 	 * <code>null</code> is return if none of this profile
 	 * or its ancestors associates a value with the key.
 	 */
 	public String getProperty(String key);
 
 	/**
 	 * Get the stored value associated with the given key in this profile.
 	 * No traversal of the ancestor hierarchy is done for sub-profiles.
 	 */
 	public String getLocalProperty(String key);
 
 	public String getInstallableUnitProperty(IInstallableUnit iu, String key);
 
 	/**
 	 * Get an <i>unmodifiable copy</i> of the local properties
 	 * associated with the profile.
 	 * 
 	 * @return an <i>unmodifiable copy</i> of the Profile properties.
 	 */
 	public Map getLocalProperties();
 
 	public Map getProperties();
 
 	public Map getInstallableUnitProperties(IInstallableUnit iu);
 
 	public long getTimestamp();
 
 	public Collector available(Query query, Collector collector, IProgressMonitor monitor);
 
 }
