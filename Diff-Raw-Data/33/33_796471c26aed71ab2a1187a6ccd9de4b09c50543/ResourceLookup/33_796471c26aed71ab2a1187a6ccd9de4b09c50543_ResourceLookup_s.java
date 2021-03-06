 /*******************************************************************************
  * Copyright (c) 2008 Wind River Systems, Inc. and others.
  * All rights reserved. This program and the accompanying materials
  * are made available under the terms of the Eclipse Public License v1.0
  * which accompanies this distribution, and is available at
  * http://www.eclipse.org/legal/epl-v10.html
  *
  * Contributors:
  *    Markus Schorn - initial API and implementation
  *******************************************************************************/ 
 package org.eclipse.cdt.internal.core.resources;
 
 import java.net.URI;
 
 import org.eclipse.core.resources.IFile;
 import org.eclipse.core.resources.IProject;
 import org.eclipse.core.runtime.IPath;
 
 /**
  * Allows for looking up resources by location or name.
  */
 public class ResourceLookup {
 	private static ResourceLookupTree lookupTree= new ResourceLookupTree();
 
 	public static void startup() {
 		lookupTree.startup();
 	}
 	
 	public static void shutdown() {
 		lookupTree.shutdown();
 	}
 	
 	/**
 	 * Searches for files with the given location suffix. 
 	 * 
 	 * At this point the method works for sources and headers (no other content types), only. 
 	 * This is done to use less memory and can be changed if necessary.
 	 * 
 	 * @param locationSuffix the suffix to match, always used as relative path.
 	 * @param projects the projects to search
 	 * @param ignoreCase whether or not to ignore case when comparing the suffix.
 	 */
 	public static IFile[] findFilesByName(IPath locationSuffix, IProject[] projects, boolean ignoreCase) {
 		return lookupTree.findFilesByName(locationSuffix, projects, ignoreCase);
 	}
 	
 	/**
 	 * Uses a lookup-tree that finds resources for locations using the canonical representation
 	 * of the path. The method does not work for files where the name (last segment) of the 
 	 * resources differs from the name of the location.
 	 */
 	public static IFile[] findFilesForLocationURI(URI location) {
 		return lookupTree.findFilesForLocationURI(location);
 	}
 
 	/**
 	 * Uses a lookup-tree that finds resources for locations using the canonical representation
 	 * of the path. The method does not work for files where the name (last segment) of the 
 	 * resources differs from the name of the location.
 	 */
 	public static IFile[] findFilesForLocation(IPath location) {
 		return lookupTree.findFilesForLocation(location);
 	}
 
 	/**
 	 * Uses {@link #findFilesForLocationURI(URI)} and selects the most relevant file
 	 * from the result. Files form the first project, from cdt-projects and those on source
 	 * roots are preferred, see {@link FileRelevance}. 
 	 * @param location an URI for the location of the files to search for.
 	 * @param preferredProject a project to be preferred over others, or <code>null</code>.
 	 * @return a file for the location in one of the given projects, or <code>null</code>.
 	 */
 	public static IFile selectFileForLocationURI(URI location, IProject preferredProject) {
 		return selectFile(findFilesForLocationURI(location), preferredProject);
 	}
 
 	/**
 	 * Uses {@link #findFilesForLocation(IPath)} and selects the most relevant file
 	 * from the result. Files form the first project, from cdt-projects and those on source
 	 * roots are preferred, see {@link FileRelevance}. 
 	 * @param location a path for the location of the files to search for.
 	 * @param preferredProject a project to be preferred over others, or <code>null</code>.
 	 * @return a file for the location in one of the given projects, or <code>null</code>.
 	 */
 	public static IFile selectFileForLocation(IPath location, IProject preferredProject) {
 		return selectFile(findFilesForLocation(location), preferredProject);
 	}
 
 	private static IFile selectFile(IFile[] files, IProject preferredProject) {
 		if (files.length == 0)
 			return null;
 		
 		if (files.length == 1) {
 			final IFile file= files[0];
 			if (file.isAccessible())
 				return file;
 		}
 		
 		IFile best= null;
 		int bestRelevance= -1;
 		
 		for (int i = 1; i < files.length; i++) {
 			IFile file = files[i];
 			if (file.isAccessible()) {
 				int relevance= FileRelevance.getRelevance(file, preferredProject);
 				if (best == null || relevance > bestRelevance ||
 						(relevance == bestRelevance && 
 								best.getFullPath().toString().compareTo(file.getFullPath().toString()) > 0)) {
 					bestRelevance= relevance;
 					best= file;
 				}
 			}
 		}
 		return best;
 	}
 
 	/** 
 	 * For testing, only.
 	 */
 	public static void dump() {
 		lookupTree.dump();
 	}
 	/** 
 	 * For testing, only.
 	 */
 	public static void unrefNodeMap() {
 		lookupTree.unrefNodeMap();
 	}
 	/** 
 	 * For testing, only.
 	 */
 	public static void simulateNodeMapCollection() {
 		lookupTree.simulateNodeMapCollection();
 	}
 }
