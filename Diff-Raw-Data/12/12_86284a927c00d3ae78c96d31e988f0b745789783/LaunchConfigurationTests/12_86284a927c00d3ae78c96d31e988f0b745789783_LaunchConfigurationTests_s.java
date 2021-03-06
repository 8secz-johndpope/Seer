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
 package org.eclipse.jdt.debug.tests.launching;
 
 import java.io.File;
 import java.net.URI;
 import java.net.URISyntaxException;
 import java.util.ArrayList;
 import java.util.HashMap;
 import java.util.HashSet;
 import java.util.Iterator;
 import java.util.List;
 import java.util.Map;
 import java.util.Set;
 
 import org.eclipse.core.filesystem.EFS;
 import org.eclipse.core.filesystem.IFileSystem;
 import org.eclipse.core.resources.IContainer;
 import org.eclipse.core.resources.IFile;
 import org.eclipse.core.resources.IFolder;
 import org.eclipse.core.resources.IProject;
 import org.eclipse.core.resources.IProjectDescription;
 import org.eclipse.core.resources.IResource;
 import org.eclipse.core.resources.ResourcesPlugin;
 import org.eclipse.core.runtime.CoreException;
 import org.eclipse.core.runtime.IPath;
 import org.eclipse.core.runtime.Path;
 import org.eclipse.debug.core.ILaunchConfiguration;
 import org.eclipse.debug.core.ILaunchConfigurationListener;
 import org.eclipse.debug.core.ILaunchConfigurationType;
 import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
 import org.eclipse.debug.core.ILaunchManager;
 import org.eclipse.debug.internal.core.LaunchManager;
 import org.eclipse.jdt.core.IJavaProject;
 import org.eclipse.jdt.debug.testplugin.JavaProjectHelper;
 import org.eclipse.jdt.debug.testplugin.JavaTestPlugin;
 import org.eclipse.jdt.debug.tests.AbstractDebugTest;
 import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;
 import org.eclipse.jdt.launching.IVMInstall;
 import org.eclipse.jdt.launching.JavaRuntime;
 
 /**
  * Tests for launch configurations
  */
 public class LaunchConfigurationTests extends AbstractDebugTest implements ILaunchConfigurationListener {
 	
 	/**
 	 * The from/to handles during rename operations
 	 */
 	protected ILaunchConfiguration fFrom;
 	protected ILaunchConfiguration fTo;
 	
 	protected Object fLock = new Object();
 	protected ILaunchConfiguration fAdded;
 	protected ILaunchConfiguration fRemoved;
 	
 	class Listener implements ILaunchConfigurationListener {
 		
 		private List addedList = new ArrayList();
 		private List removedList = new ArrayList();
 		private List changedList = new ArrayList();
 
 		/* (non-Javadoc)
 		 * @see org.eclipse.debug.core.ILaunchConfigurationListener#launchConfigurationAdded(org.eclipse.debug.core.ILaunchConfiguration)
 		 */
 		public void launchConfigurationAdded(ILaunchConfiguration configuration) {
 			addedList.add(configuration);
 		}
 
 		/* (non-Javadoc)
 		 * @see org.eclipse.debug.core.ILaunchConfigurationListener#launchConfigurationChanged(org.eclipse.debug.core.ILaunchConfiguration)
 		 */
 		public void launchConfigurationChanged(ILaunchConfiguration configuration) {
 			changedList.add(configuration);
 		}
 
 		/* (non-Javadoc)
 		 * @see org.eclipse.debug.core.ILaunchConfigurationListener#launchConfigurationRemoved(org.eclipse.debug.core.ILaunchConfiguration)
 		 */
 		public void launchConfigurationRemoved(ILaunchConfiguration configuration) {
 			removedList.add(configuration);
 		}
 		
 		public List getAdded() {
 			return addedList;
 		}
 		public List getChanged() {
 			return changedList;
 		}
 		public List getRemoved() {
 			return removedList;
 		}
 		
 	}
 	
 	/**
 	 * Constructor
 	 * @param name
 	 */
 	public LaunchConfigurationTests(String name) {
 		super(name);
 	}
 	
 	/** 
 	 * Creates and returns a new launch config the given name, local
 	 * or shared, with 4 attributes:
 	 *  - String1 = "String1"
 	 *  - Int1 = 1
 	 *  - Boolean1 = true
 	 *  - Boolean2 = false
 	 */
 	protected ILaunchConfigurationWorkingCopy newConfiguration(IContainer container, String name) throws CoreException {
 		 ILaunchConfigurationType type = getLaunchManager().getLaunchConfigurationType(IJavaLaunchConfigurationConstants.ID_JAVA_APPLICATION);
 		 assertTrue("Should support debug mode", type.supportsMode(ILaunchManager.DEBUG_MODE)); //$NON-NLS-1$
 		 assertTrue("Should support run mode", type.supportsMode(ILaunchManager.RUN_MODE)); //$NON-NLS-1$
 		 ILaunchConfigurationWorkingCopy wc = type.newInstance(container, name);
 		 wc.setAttribute("String1", "String1"); //$NON-NLS-1$ //$NON-NLS-2$
 		 wc.setAttribute("Int1", 1); //$NON-NLS-1$
 		 wc.setAttribute("Boolean1", true); //$NON-NLS-1$
 		 wc.setAttribute("Boolean2", false); //$NON-NLS-1$
 		 assertTrue("Should need saving", wc.isDirty()); //$NON-NLS-1$
 		 return wc;
 	}
 		
 	/**
 	 * Returns whether the given handle is contained in the specified
 	 * array of handles.
 	 */
 	protected boolean existsIn(ILaunchConfiguration[] configs, ILaunchConfiguration config) {
 		for (int i = 0; i < configs.length; i++) {
 			if (configs[i].equals(config)) {
 				return true;
 			}
 		}
 		return false;
 	}
 	
 	/**
 	 * Creates a local working copy configuration, sets some attributes,
 	 * and saves the working copy, and retrieves the attributes.
 	 * 
 	 * @throws CoreException
 	 */
 	public void testCreateLocalConfiguration() throws CoreException {
 		 ILaunchConfigurationWorkingCopy wc = newConfiguration(null, "config1"); //$NON-NLS-1$
 		 IPath location = wc.getLocation();
 		 ILaunchConfiguration handle = wc.doSave();
 		 File file = location.toFile();
 		 assertTrue("Configuration file should exist", file.exists()); //$NON-NLS-1$
 		 
 		 // retrieve attributes
 		 assertEquals("String1 should be String1", handle.getAttribute("String1", "Missing"), "String1"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
 		 assertEquals("Int1 should be 1", handle.getAttribute("Int1", 0), 1); //$NON-NLS-1$ //$NON-NLS-2$
 		 assertTrue("Boolean1 should be true", handle.getAttribute("Boolean1", false));  //$NON-NLS-1$//$NON-NLS-2$
 		 assertTrue("Boolean2 should be false", !handle.getAttribute("Boolean2", true));  //$NON-NLS-1$//$NON-NLS-2$
 		 
 		 // ensure new handle is the index
 		 ILaunchConfiguration[] configs = getLaunchManager().getLaunchConfigurations();
 		 assertTrue("Configuration should exist in project index", existsIn(configs, handle)); //$NON-NLS-1$
 		 
 		 // cleanup
 		 handle.delete();
 		 assertTrue("Config should not exist after deletion", !handle.exists()); //$NON-NLS-1$
 	}
 	
 	/**
 	 * Creates a local working copy configuration and tests its name.
 	 * 
 	 * @throws CoreException
 	 */
 	public void testLocalName() throws CoreException {
 		 ILaunchConfigurationWorkingCopy wc = newConfiguration(null, "localName"); //$NON-NLS-1$
 		 ILaunchConfiguration handle = wc.doSave();
 		 assertTrue("Configuration should exist", handle.exists()); //$NON-NLS-1$
 		 
 		 // retrieve attributes
 		 assertEquals("Wrong name", handle.getName(), "localName"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
 
 		 // cleanup
 		 handle.delete();
 		 assertTrue("Config should not exist after deletion", !handle.exists()); //$NON-NLS-1$
 	}	
 	
 	/**
 	 * Creates a shared working copy configuration and tests is name.
 	 */
 	public void testSharedName() throws CoreException {
 		 ILaunchConfigurationWorkingCopy wc = newConfiguration(getJavaProject().getProject(), "sharedName"); //$NON-NLS-1$
 		 ILaunchConfiguration handle = wc.doSave();
 		 assertTrue("Configuration should exist", handle.exists()); //$NON-NLS-1$
 		 
 		 // retrieve attributes
 		 assertEquals("Wrong name", handle.getName(), "sharedName"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
 		 
  		 // cleanup
 		 handle.delete();
 		 assertTrue("Config should not exist after deletion", !handle.exists()); //$NON-NLS-1$
 	}	
 	
 	/**
 	 * Ensures that a launch configuration returns a complete attribute map
 	 * @throws CoreException
 	 */
 	public void testGetAttributes() throws CoreException {
 		 ILaunchConfigurationWorkingCopy wc = newConfiguration(null, "config1"); //$NON-NLS-1$
 		 IPath location = wc.getLocation();
 		 ILaunchConfiguration handle = wc.doSave();
 		 File file = location.toFile();
 		 assertTrue("Configuration file should exist", file.exists()); //$NON-NLS-1$
 
 		 Map attributes = handle.getAttributes();
 		 // retrieve attributes
 		 assertEquals("String1 should be String1", "String1", attributes.get("String1")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
 		 assertEquals("Int1 should be 1", new Integer(1), attributes.get("Int1")); //$NON-NLS-1$ //$NON-NLS-2$
 		 assertEquals("Boolean1 should be true", Boolean.valueOf(true), attributes.get("Boolean1")); //$NON-NLS-1$ //$NON-NLS-2$
 		 assertEquals("Boolean2 should be false", Boolean.valueOf(false), attributes.get("Boolean2")); //$NON-NLS-1$ //$NON-NLS-2$
 
 		 // cleanup
 		 handle.delete();
 		 assertTrue("Config should not exist after deletion", !handle.exists()); //$NON-NLS-1$
 	}	
 	
 	/**
 	 * Ensures that set attributes works
 	 * @throws CoreException
 	 */
 	public void testSetAttributes() throws CoreException {
 		 ILaunchConfigurationWorkingCopy wc = newConfiguration(null, "config1"); //$NON-NLS-1$
 		 Map map = new HashMap();
 		 map.put("ATTR1", "ONE"); //$NON-NLS-1$ //$NON-NLS-2$
 		 map.put("ATTR2", "TWO"); //$NON-NLS-1$ //$NON-NLS-2$
 		 wc.setAttributes(map);
 		 IPath location = wc.getLocation();
 		 ILaunchConfiguration handle = wc.doSave();
 		 File file = location.toFile();
 		 assertTrue("Configuration file should exist", file.exists()); //$NON-NLS-1$
 
 		 Map attributes = handle.getAttributes();
 		 assertEquals("should have two attributes", 2, attributes.size()); //$NON-NLS-1$
 		 // retrieve attributes
 		 assertEquals("ATTR1 should be ONE", "ONE", attributes.get("ATTR1")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
 		 assertEquals("ATTR2 should be TWO", "TWO", attributes.get("ATTR2")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
 
 		 // cleanup
 		 handle.delete();
 		 assertTrue("Config should not exist after deletion", !handle.exists()); //$NON-NLS-1$
 	}	
 	
 	/**
 	 * Ensures that set attributes to <code>null</code> works
 	 * @throws CoreException
 	 */
 	public void testSetNullAttributes() throws CoreException {
 		 ILaunchConfigurationWorkingCopy wc = newConfiguration(null, "config1"); //$NON-NLS-1$
 		 wc.setAttributes(null);
 		 IPath location = wc.getLocation();
 		 ILaunchConfiguration handle = wc.doSave();
 		 File file = location.toFile();
 		 assertTrue("Configuration file should exist", file.exists()); //$NON-NLS-1$
 
 		 Map attributes = handle.getAttributes();
 		 assertEquals("should have no attributes", 0, attributes.size()); //$NON-NLS-1$
 		 // cleanup
 		 handle.delete();
 		 assertTrue("Config should not exist after deletion", !handle.exists()); //$NON-NLS-1$
 	}
 		
 	/**
 	 * Creates a local working copy configuration, sets some attributes,
 	 * and saves the working copy, and retrieves the attributes.
 	 * Copy the configuration and ensure the original still exists.
 	 * @throws CoreException
 	 */
 	public void testLocalCopy() throws CoreException {
 		 ILaunchConfigurationWorkingCopy wc = newConfiguration(null, "configToCopy"); //$NON-NLS-1$
 		 IPath location = wc.getLocation();
 		 ILaunchConfiguration handle = wc.doSave();
 		 File file = location.toFile();
 		 assertTrue("Configuration file should exist", file.exists()); //$NON-NLS-1$
 		 
 		 // retrieve attributes
 		 assertTrue("String1 should be String1", handle.getAttribute("String1", "Missing").equals("String1")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
 		 assertTrue("Int1 should be 1", handle.getAttribute("Int1", 0) == 1); //$NON-NLS-1$ //$NON-NLS-2$
 		 assertTrue("Boolean1 should be true", handle.getAttribute("Boolean1", false)); //$NON-NLS-1$ //$NON-NLS-2$
 		 assertTrue("Boolean2 should be false", !handle.getAttribute("Boolean2", true)); //$NON-NLS-1$ //$NON-NLS-2$
 		 
 		 // ensure new handle is the index
 		 ILaunchConfiguration[] configs = getLaunchManager().getLaunchConfigurations();
 		 assertTrue("Configuration should exist in project index", existsIn(configs, handle)); //$NON-NLS-1$
 		 
 		 ILaunchConfigurationWorkingCopy softCopy = handle.copy("CopyOf" + handle.getName()); //$NON-NLS-1$
 		 assertNull("Original in copy should be null", softCopy.getOriginal()); //$NON-NLS-1$
 		 ILaunchConfiguration hardCopy = softCopy.doSave();
 
 		 // retrieve attributes
 		 assertTrue("String1 should be String1", hardCopy.getAttribute("String1", "Missing").equals("String1")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
 		 assertTrue("Int1 should be 1", hardCopy.getAttribute("Int1", 0) == 1); //$NON-NLS-1$ //$NON-NLS-2$
 		 assertTrue("Boolean1 should be true", hardCopy.getAttribute("Boolean1", false)); //$NON-NLS-1$ //$NON-NLS-2$
 		 assertTrue("Boolean2 should be false", !hardCopy.getAttribute("Boolean2", true));		  //$NON-NLS-1$ //$NON-NLS-2$
 		 
 		 assertTrue("Original should still exist", handle.exists()); //$NON-NLS-1$
 		 
 		 // cleanup
 		 handle.delete();
 		 assertTrue("Config should not exist after deletion", !handle.exists()); //$NON-NLS-1$
 		 hardCopy.delete();
 		 assertTrue("Config should not exist after deletion", !hardCopy.exists());		 		  //$NON-NLS-1$
 	}
 		
 	/**
 	 * Create a config and save it twice, ensuring it only
 	 * ends up in the index once.
 	 * @throws CoreException
 	 */
 	public void testDoubleSave() throws CoreException {
 		 ILaunchConfigurationWorkingCopy wc = newConfiguration(null, "configDoubleSave"); //$NON-NLS-1$
 		 IPath location = wc.getLocation();
 		 ILaunchConfiguration handle = wc.doSave();
 		 File file = location.toFile();
 		 assertTrue("Configuration file should exist", file.exists()); //$NON-NLS-1$
 		 
 		 // retrieve attributes
 		 assertTrue("String1 should be String1", handle.getAttribute("String1", "Missing").equals("String1")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
 		 assertTrue("Int1 should be 1", handle.getAttribute("Int1", 0) == 1); //$NON-NLS-1$ //$NON-NLS-2$
 		 assertTrue("Boolean1 should be true", handle.getAttribute("Boolean1", false)); //$NON-NLS-1$ //$NON-NLS-2$
 		 assertTrue("Boolean2 should be false", !handle.getAttribute("Boolean2", true)); //$NON-NLS-1$ //$NON-NLS-2$
 		 
 		 // ensure new handle is the index
 		 ILaunchConfiguration[] configs = getLaunchManager().getLaunchConfigurations();
 		 assertTrue("Configuration should exist in project index", existsIn(configs, handle)); //$NON-NLS-1$
 		 
 		String name = wc.getName();
 		wc.rename("newName"); //$NON-NLS-1$
 		wc.rename(name);
 		assertTrue("Should be dirty", wc.isDirty()); //$NON-NLS-1$
 		wc.doSave();
 		
 		ILaunchConfiguration[] newConfigs = getLaunchManager().getLaunchConfigurations();
 		assertTrue("Should be the same number of configs", newConfigs.length == configs.length); //$NON-NLS-1$
 		
 		 // cleanup
 		 handle.delete();
 		 assertTrue("Config should not exist after deletion", !handle.exists()); //$NON-NLS-1$
 		
 	}
 		
 	/**
 	 * Creates a local working copy configuration, sets some attributes,
 	 * and saves the working copy, and retrieves the attributes. Deletes
 	 * the configuration and ensures it no longer exists.
 	 * @throws CoreException
 	 */
 	public void testDeleteLocalConfiguration() throws CoreException {
 		 ILaunchConfigurationWorkingCopy wc = newConfiguration(null, "config2delete"); //$NON-NLS-1$
 		 ILaunchConfiguration handle = wc.doSave();
 		 File file = wc.getLocation().toFile();
 		 assertTrue("Configuration file should exist", file.exists()); //$NON-NLS-1$
 		 
 		 // retrieve attributes
 		 assertTrue("String1 should be String1", handle.getAttribute("String1", "Missing").equals("String1")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
 		 assertTrue("Int1 should be 1", handle.getAttribute("Int1", 0) == 1); //$NON-NLS-1$ //$NON-NLS-2$
 		 assertTrue("Boolean1 should be true", handle.getAttribute("Boolean1", false)); //$NON-NLS-1$ //$NON-NLS-2$
 		 assertTrue("Boolean2 should be false", !handle.getAttribute("Boolean2", true)); //$NON-NLS-1$ //$NON-NLS-2$
 		 
 		 // delete 
 		 handle.delete();		 
 		 assertTrue("Config should no longer exist", !handle.exists()); //$NON-NLS-1$
 		 
 		 // ensure handle is not in the index
 		 ILaunchConfiguration[] configs = getLaunchManager().getLaunchConfigurations();
 		 assertTrue("Configuration should not exist in project index", !existsIn(configs, handle));		  //$NON-NLS-1$
 	}	
 	
 	/**
 	 * Creates a local working copy configuration, sets some attributes,
 	 * and saves the working copy, and retrieves the attributes. Renames
 	 * the configuration and ensures it's old config no longer exists,
 	 * and that attributes are retrievable from the new (renamed) config.
 	 * @throws CoreException
 	 */
 	public void testRenameLocalConfiguration() throws CoreException {
 		 ILaunchConfigurationWorkingCopy wc = newConfiguration(null, "config2rename"); //$NON-NLS-1$
 		 IPath location = wc.getLocation();
 		 ILaunchConfiguration handle = wc.doSave();
 		 File file = location.toFile();
 		 assertTrue("Configuration file should exist", file.exists()); //$NON-NLS-1$
 		 
 		 // retrieve attributes
 		 assertTrue("String1 should be String1", handle.getAttribute("String1", "Missing").equals("String1")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
 		 assertTrue("Int1 should be 1", handle.getAttribute("Int1", 0) == 1); //$NON-NLS-1$ //$NON-NLS-2$
 		 assertTrue("Boolean1 should be true", handle.getAttribute("Boolean1", false)); //$NON-NLS-1$ //$NON-NLS-2$
 		 assertTrue("Boolean2 should be false", !handle.getAttribute("Boolean2", true)); //$NON-NLS-1$ //$NON-NLS-2$
 		 
 		 // rename
 		 wc = handle.getWorkingCopy();
 		 wc.rename("config-2-rename"); //$NON-NLS-1$
 		 addConfigListener();
 		 ILaunchConfiguration newHandle = wc.doSave();
 		 removeConfigListener();
 		 assertTrue("Config should no longer exist", !handle.exists()); //$NON-NLS-1$
 		 assertEquals("From should be original", handle, fFrom); //$NON-NLS-1$
 		 assertEquals("To should be new handle", newHandle, fTo); //$NON-NLS-1$
 		 
 		 // retrieve new attributes
 		 assertTrue("String1 should be String1", newHandle.getAttribute("String1", "Missing").equals("String1")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
 		 assertTrue("Int1 should be 1", newHandle.getAttribute("Int1", 0) == 1); //$NON-NLS-1$ //$NON-NLS-2$
 		 assertTrue("Boolean1 should be true", newHandle.getAttribute("Boolean1", false));  //$NON-NLS-1$//$NON-NLS-2$
 		 assertTrue("Boolean2 should be false", !newHandle.getAttribute("Boolean2", true));		  //$NON-NLS-1$ //$NON-NLS-2$
 
 		 // ensure new handle is in the index
 		 ILaunchConfiguration[] configs = getLaunchManager().getLaunchConfigurations();
 		 assertTrue("Renamed configuration should exist in project index", existsIn(configs, newHandle));		  //$NON-NLS-1$
 		 assertTrue("Original configuration should NOT exist in project index", !existsIn(configs, handle));	 //$NON-NLS-1$
 		 
 		 // cleanup
 		 newHandle.delete();
 		 assertTrue("Config should not exist after deletion", !newHandle.exists());		 	  //$NON-NLS-1$
 	}	
 	
 	/**
 	 * Moves a local configuration to a shared location
 	 * @throws CoreException
 	 */
 	public void testMoveLocalToSharedConfiguration() throws CoreException {
 		 ILaunchConfigurationWorkingCopy wc = newConfiguration(null, "config2share"); //$NON-NLS-1$
 		 IPath location = wc.getLocation();
 		 ILaunchConfiguration handle = wc.doSave();
 		 File file = location.toFile();
 		 assertTrue("Configuration file should exist", file.exists()); //$NON-NLS-1$
 
 		 // retrieve attributes
 		 assertTrue("String1 should be String1", handle.getAttribute("String1", "Missing").equals("String1")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
 		 assertTrue("Int1 should be 1", handle.getAttribute("Int1", 0) == 1); //$NON-NLS-1$ //$NON-NLS-2$
 		 assertTrue("Boolean1 should be true", handle.getAttribute("Boolean1", false)); //$NON-NLS-1$ //$NON-NLS-2$
 		 assertTrue("Boolean2 should be false", !handle.getAttribute("Boolean2", true)); //$NON-NLS-1$ //$NON-NLS-2$
 
 		 // move
 		 wc = handle.getWorkingCopy();
 		 wc.setContainer(getJavaProject().getProject());
 		 addConfigListener();
 		 ILaunchConfiguration newHandle = wc.doSave();
 		 removeConfigListener();
 		 assertTrue("Config should no longer exist", !handle.exists()); //$NON-NLS-1$
 		 assertEquals("From should be original", handle, fFrom); //$NON-NLS-1$
 		 assertEquals("To should be new handle", newHandle, fTo); //$NON-NLS-1$
 
 		 // retrieve new attributes
 		 assertTrue("String1 should be String1", newHandle.getAttribute("String1", "Missing").equals("String1")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
 		 assertTrue("Int1 should be 1", newHandle.getAttribute("Int1", 0) == 1); //$NON-NLS-1$ //$NON-NLS-2$
 		 assertTrue("Boolean1 should be true", newHandle.getAttribute("Boolean1", false)); //$NON-NLS-1$ //$NON-NLS-2$
 		 assertTrue("Boolean2 should be false", !newHandle.getAttribute("Boolean2", true)); //$NON-NLS-1$ //$NON-NLS-2$
 
 		 // ensure new handle is in the index
 		 ILaunchConfiguration[] configs = getLaunchManager().getLaunchConfigurations();
 		 assertTrue("Renamed configuration should exist in project index", existsIn(configs, newHandle)); //$NON-NLS-1$
 		 assertTrue("Original configuration should NOT exist in project index", !existsIn(configs, handle)); //$NON-NLS-1$
 
 		 // cleanup
 		 newHandle.delete();
 		 assertTrue("Config should not exist after deletion", !newHandle.exists()); //$NON-NLS-1$
 	}	
 	
 	/**
 	 * Moves a local configuration to a shared location
 	 * @throws CoreException
 	 */
 	public void testMoveSharedToLocalConfiguration() throws CoreException {
 		 ILaunchConfigurationWorkingCopy wc = newConfiguration(getJavaProject().getProject(), "config2local"); //$NON-NLS-1$
 		 IPath location = wc.getLocation();
 		 ILaunchConfiguration handle = wc.doSave();
 		 File file = location.toFile();
 		 assertTrue("Configuration file should exist", file.exists()); //$NON-NLS-1$
 
 		 // retrieve attributes
 		 assertTrue("String1 should be String1", handle.getAttribute("String1", "Missing").equals("String1")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
 		 assertTrue("Int1 should be 1", handle.getAttribute("Int1", 0) == 1); //$NON-NLS-1$ //$NON-NLS-2$
 		 assertTrue("Boolean1 should be true", handle.getAttribute("Boolean1", false)); //$NON-NLS-1$ //$NON-NLS-2$
 		 assertTrue("Boolean2 should be false", !handle.getAttribute("Boolean2", true)); //$NON-NLS-1$ //$NON-NLS-2$
 
 		 // move
 		 wc = handle.getWorkingCopy();
 		 wc.setContainer(null);
 		 addConfigListener();
 		 ILaunchConfiguration newHandle = wc.doSave();
 		 removeConfigListener();
 		 assertTrue("Config should no longer exist", !handle.exists()); //$NON-NLS-1$
 		 assertEquals("From should be original", handle, fFrom); //$NON-NLS-1$
 		 assertEquals("To should be new handle", newHandle, fTo); //$NON-NLS-1$
 
 		 // retrieve new attributes
 		 assertTrue("String1 should be String1", newHandle.getAttribute("String1", "Missing").equals("String1")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
 		 assertTrue("Int1 should be 1", newHandle.getAttribute("Int1", 0) == 1); //$NON-NLS-1$ //$NON-NLS-2$
 		 assertTrue("Boolean1 should be true", newHandle.getAttribute("Boolean1", false)); //$NON-NLS-1$ //$NON-NLS-2$
 		 assertTrue("Boolean2 should be false", !newHandle.getAttribute("Boolean2", true)); //$NON-NLS-1$ //$NON-NLS-2$
 
 		 // ensure new handle is in the index
 		 ILaunchConfiguration[] configs = getLaunchManager().getLaunchConfigurations();
 		 assertTrue("Renamed configuration should exist in project index", existsIn(configs, newHandle)); //$NON-NLS-1$
 		 assertTrue("Original configuration should NOT exist in project index", !existsIn(configs, handle)); //$NON-NLS-1$
 
 		 // cleanup
 		 newHandle.delete();
 		 assertTrue("Config should not exist after deletion", !newHandle.exists()); //$NON-NLS-1$
 	}		
 	
 	/**
 	 * Creates a shared working copy configuration, sets some attributes,
 	 * and saves the working copy, and retrieves the attributes.
 	 * @throws CoreException
 	 */
 	public void testCreateSharedConfiguration() throws CoreException {
 		 ILaunchConfigurationWorkingCopy wc = newConfiguration(getJavaProject().getProject(), "config2"); //$NON-NLS-1$
 		 ILaunchConfiguration handle = wc.doSave();
 		 assertTrue("Configuration should exist", handle.exists()); //$NON-NLS-1$
 		 
 		 // retrieve attributes
 		 assertTrue("String1 should be String1", handle.getAttribute("String1", "Missing").equals("String1")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
 		 assertTrue("Int1 should be 1", handle.getAttribute("Int1", 0) == 1); //$NON-NLS-1$ //$NON-NLS-2$
 		 assertTrue("Boolean1 should be true", handle.getAttribute("Boolean1", false)); //$NON-NLS-1$ //$NON-NLS-2$
 		 assertTrue("Boolean2 should be false", !handle.getAttribute("Boolean2", true)); //$NON-NLS-1$ //$NON-NLS-2$
 		 
  		 // ensure new handle is in the index
 		 ILaunchConfiguration[] configs = getLaunchManager().getLaunchConfigurations();
 		 assertTrue("Configuration should exist in project index", existsIn(configs, handle));  //$NON-NLS-1$
 		 
  		 // cleanup
 		 handle.delete();
 		 assertTrue("Config should not exist after deletion", !handle.exists()); //$NON-NLS-1$
 	}	
 	
 	/**
 	 * Creates a shared working copy configuration, sets some attributes,
 	 * and saves the working copy, and retrieves the attributes.
 	 * Copies the configuration and ensures the original still exists.
 	 * @throws CoreException
 	 */
 	public void testSharedCopy() throws CoreException {
 		 ILaunchConfigurationWorkingCopy wc = newConfiguration(getJavaProject().getProject(), "config2Copy"); //$NON-NLS-1$
 		 ILaunchConfiguration handle = wc.doSave();
 		 assertTrue("Configuration should exist", handle.exists()); //$NON-NLS-1$
 		 
 		 // retrieve attributes
 		 assertTrue("String1 should be String1", handle.getAttribute("String1", "Missing").equals("String1")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
 		 assertTrue("Int1 should be 1", handle.getAttribute("Int1", 0) == 1); //$NON-NLS-1$ //$NON-NLS-2$
 		 assertTrue("Boolean1 should be true", handle.getAttribute("Boolean1", false)); //$NON-NLS-1$ //$NON-NLS-2$
 		 assertTrue("Boolean2 should be false", !handle.getAttribute("Boolean2", true)); //$NON-NLS-1$ //$NON-NLS-2$
 		 
  		 // ensure new handle is in the index
 		 ILaunchConfiguration[] configs = getLaunchManager().getLaunchConfigurations();
 		 assertTrue("Configuration should exist in project index", existsIn(configs, handle));  //$NON-NLS-1$
 		 
 		 // copy 
 		 ILaunchConfigurationWorkingCopy softCopy = handle.copy("CopyOf" + handle.getName()); //$NON-NLS-1$
 		 ILaunchConfiguration hardCopy = softCopy.doSave();
 		 
 		 // retrieve attributes
 		 assertTrue("String1 should be String1", hardCopy.getAttribute("String1", "Missing").equals("String1")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
 		 assertTrue("Int1 should be 1", hardCopy.getAttribute("Int1", 0) == 1); //$NON-NLS-1$ //$NON-NLS-2$
 		 assertTrue("Boolean1 should be true", hardCopy.getAttribute("Boolean1", false)); //$NON-NLS-1$ //$NON-NLS-2$
 		 assertTrue("Boolean2 should be false", !hardCopy.getAttribute("Boolean2", true));		  //$NON-NLS-1$ //$NON-NLS-2$
 		 
 		 assertTrue("Original should still exist", handle.exists()); //$NON-NLS-1$
 		 
 		 // cleanup
 		 handle.delete();
 		 assertTrue("Config should not exist after deletion", !handle.exists()); //$NON-NLS-1$
 		 hardCopy.delete();
 		 assertTrue("Config should not exist after deletion", !hardCopy.exists());		 		 		  //$NON-NLS-1$
 	}		
 	
 
 	/**
 	 * Creates a shared working copy configuration, sets some attributes,
 	 * and saves the working copy, and retrieves the attributes. Deletes
 	 * the configuration and ensures it no longer exists.
 	 * @throws CoreException
 	 */
 	public void testDeleteSharedConfiguration() throws CoreException {
  		 ILaunchConfigurationWorkingCopy wc = newConfiguration(getJavaProject().getProject(), "shared2delete"); //$NON-NLS-1$
 		 ILaunchConfiguration handle = wc.doSave();
 		 assertTrue("Configuration should exist", handle.exists()); //$NON-NLS-1$
 		 
 		 // retrieve attributes
 		 assertTrue("String1 should be String1", handle.getAttribute("String1", "Missing").equals("String1")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
 		 assertTrue("Int1 should be 1", handle.getAttribute("Int1", 0) == 1); //$NON-NLS-1$ //$NON-NLS-2$
 		 assertTrue("Boolean1 should be true", handle.getAttribute("Boolean1", false)); //$NON-NLS-1$ //$NON-NLS-2$
 		 assertTrue("Boolean2 should be false", !handle.getAttribute("Boolean2", true)); //$NON-NLS-1$ //$NON-NLS-2$
 		 
 		 // delete 
 		 handle.delete();		 
 		 assertTrue("Config should no longer exist", !handle.exists()); //$NON-NLS-1$
 		 
 		 // ensure handle is not in the index
 		 ILaunchConfiguration[] configs = getLaunchManager().getLaunchConfigurations();
 		 assertTrue("Configuration should not exist in project index", !existsIn(configs, handle));		  //$NON-NLS-1$
 	}	
 	
 	/**
 	 * Creates a shared working copy configuration, sets some attributes,
 	 * and saves the working copy, and retrieves the attributes. Renames
 	 * the configuration and ensures it's old config no longer exists,
 	 * and that attributes are retrievable from the new (renamed) config.
 	 * @throws CoreException
 	 */
 	public void testRenameSharedConfiguration() throws CoreException {
 		 ILaunchConfigurationWorkingCopy wc = newConfiguration(getJavaProject().getProject(), "shared2rename"); //$NON-NLS-1$
 		 ILaunchConfiguration handle = wc.doSave();
 		 assertTrue("Configuration should exist", handle.exists()); //$NON-NLS-1$
 		 
 		 // retrieve attributes
 		 assertTrue("String1 should be String1", handle.getAttribute("String1", "Missing").equals("String1")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
 		 assertTrue("Int1 should be 1", handle.getAttribute("Int1", 0) == 1); //$NON-NLS-1$ //$NON-NLS-2$
 		 assertTrue("Boolean1 should be true", handle.getAttribute("Boolean1", false)); //$NON-NLS-1$ //$NON-NLS-2$
 		 assertTrue("Boolean2 should be false", !handle.getAttribute("Boolean2", true)); //$NON-NLS-1$ //$NON-NLS-2$
 		 
 		 // rename
 		 wc = handle.getWorkingCopy();
 		 wc.rename("shared-2-rename"); //$NON-NLS-1$
 		 addConfigListener();
 		 ILaunchConfiguration newHandle = wc.doSave();
 		 removeConfigListener();
 		 assertTrue("Config should no longer exist", !handle.exists()); //$NON-NLS-1$
 		 assertEquals("From should be original", handle, fFrom); //$NON-NLS-1$
 		 assertEquals("To should be new handle", newHandle, fTo);		  //$NON-NLS-1$
 		 
 		 // retrieve new attributes
 		 assertTrue("String1 should be String1", newHandle.getAttribute("String1", "Missing").equals("String1")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
 		 assertTrue("Int1 should be 1", newHandle.getAttribute("Int1", 0) == 1); //$NON-NLS-1$ //$NON-NLS-2$
 		 assertTrue("Boolean1 should be true", newHandle.getAttribute("Boolean1", false)); //$NON-NLS-1$ //$NON-NLS-2$
 		 assertTrue("Boolean2 should be false", !newHandle.getAttribute("Boolean2", true));		  //$NON-NLS-1$ //$NON-NLS-2$
 
 		 // ensure new handle is in the index
 		 ILaunchConfiguration[] configs = getLaunchManager().getLaunchConfigurations();
 		 assertTrue("Renamed configuration should exist in project index", existsIn(configs, newHandle));		  //$NON-NLS-1$
 		 assertTrue("Original configuration should NOT exist in project index", !existsIn(configs, handle));		  //$NON-NLS-1$
 		 
 		 // cleanup
 		 newHandle.delete();
 		 assertTrue("Config should not exist after deletion", !newHandle.exists());		  //$NON-NLS-1$
 	}
 	
 	/** 
 	 * Creates a few configs, closes the project and re-opens the
 	 * project to ensure the config index is persisted properly
 	 * @throws CoreException
 	 */
 	public void testPersistIndex() throws CoreException {
 		// close all editors before closing project: @see bug 204023
 		closeAllEditors();
 		
 		ILaunchConfigurationWorkingCopy wc1 = newConfiguration(null, "persist1local"); //$NON-NLS-1$
 		ILaunchConfigurationWorkingCopy wc2 = newConfiguration(getJavaProject().getProject(), "persist2shared"); //$NON-NLS-1$
 		ILaunchConfiguration lc1 = wc1.doSave();
 		ILaunchConfiguration lc2 = wc2.doSave();
 		
 		IProject project = getJavaProject().getProject();
 		ILaunchConfiguration[] before = getLaunchManager().getLaunchConfigurations();
 		assertTrue("config should be in index", existsIn(before, lc1)); //$NON-NLS-1$
 		assertTrue("config should be in index", existsIn(before, lc2)); //$NON-NLS-1$
 		
 		project.close(null);
 		ILaunchConfiguration[] during = getLaunchManager().getLaunchConfigurations();
 		boolean local = true;
 		for (int i = 0; i < during.length; i++) {
 			local = local && (during[i].isLocal() ||
 					!project.getName().equals(during[i].getAttribute(IJavaLaunchConfigurationConstants.ATTR_PROJECT_NAME, (String)null))) ;
 		}		
 		project.open(null);
 		assertTrue("Should only be local configs when closed", local); //$NON-NLS-1$
 		ILaunchConfiguration[] after = getLaunchManager().getLaunchConfigurations();
 		assertTrue("Should be same number of configs after openning", after.length == before.length); //$NON-NLS-1$
 		for (int i = 0; i < before.length; i++) {
 			assertTrue("Config should exist after openning", existsIn(after, before[i])); //$NON-NLS-1$
 		}
 
 		 // cleanup
 		 lc1.delete();
 		 assertTrue("Config should not exist after deletion", !lc1.exists()); //$NON-NLS-1$
 		 lc2.delete();
 		 assertTrue("Config should not exist after deletion", !lc2.exists());		  //$NON-NLS-1$
 		 
 		
 	}	
 		
 		
 	/**
 	 * @see org.eclipse.debug.core.ILaunchConfigurationListener#launchConfigurationAdded(org.eclipse.debug.core.ILaunchConfiguration)
 	 */
 	public void launchConfigurationAdded(ILaunchConfiguration configuration) {
 		fFrom = getLaunchManager().getMovedFrom(configuration);
 		synchronized (fLock) {
 		    fAdded = configuration;
 		    fLock.notifyAll();
         }
 	}
 
 	/**
 	 * @see org.eclipse.debug.core.ILaunchConfigurationListener#launchConfigurationChanged(org.eclipse.debug.core.ILaunchConfiguration)
 	 */
 	public void launchConfigurationChanged(ILaunchConfiguration configuration) {
 	}
 
 	/**
 	 * @see org.eclipse.debug.core.ILaunchConfigurationListener#launchConfigurationRemoved(org.eclipse.debug.core.ILaunchConfiguration)
 	 */
 	public void launchConfigurationRemoved(ILaunchConfiguration configuration) {
 		fTo = getLaunchManager().getMovedTo(configuration);
 		synchronized (fLock) {
 		    fRemoved = configuration;
 		    fLock.notifyAll();
         }
 	}
 
 	protected void addConfigListener() {
 		getLaunchManager().addLaunchConfigurationListener(this);
 	}
 	
 	protected void removeConfigListener() {
 		getLaunchManager().removeLaunchConfigurationListener(this);
 	}
 	
 	/**
 	 * Ensures that a removal notification is sent for a shared config in a project
 	 * that is deleted.
 	 *  
 	 * @throws Exception
 	 */
 	public void testDeleteProjectWithSharedConfig() throws Exception {
 	   IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject("DeleteSharedConfig"); //$NON-NLS-1$
 	   try {
 		   assertFalse("project should not exist yet", project.exists()); //$NON-NLS-1$
 		   project.create(null);
 		   assertTrue("project should now exist", project.exists()); //$NON-NLS-1$
 		   project.open(null);
 		   assertTrue("project should be open", project.isOpen()); //$NON-NLS-1$
 		   ILaunchConfigurationWorkingCopy wc = newConfiguration(project, "ToBeDeleted"); //$NON-NLS-1$
 		   
 		   addConfigListener();
 		   ILaunchConfiguration configuration = wc.doSave();
 		   assertEquals(configuration, fAdded);
 		   
 		   synchronized (fLock) {
 		       fRemoved = null;
 		       project.delete(true, false, null);
 		       if (fRemoved == null) {
 		           fLock.wait(10000);
 		       }
 		   }
 		   assertEquals(configuration, fRemoved);
 	   } finally {
 	       if (project.exists()) {
 	           project.delete(true, false, null);
 	       }
 	       removeConfigListener();
 	   }
 	}
 	
 	/**
 	 * Tests a nested working copy.
 	 * 
 	 * @throws CoreException
 	 */
 	public void testNestedWorkingCopyLocalConfiguration() throws CoreException {
 		 ILaunchConfigurationWorkingCopy wc = newConfiguration(null, "config123"); //$NON-NLS-1$
 		 IPath location = wc.getLocation();
 		 ILaunchConfiguration handle = wc.doSave();
 		 File file = location.toFile();
 		 assertTrue("Configuration file should exist", file.exists()); //$NON-NLS-1$
 		 
 		 // retrieve attributes
 		 assertEquals("String1 should be String1", handle.getAttribute("String1", "Missing"), "String1"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
 		 assertEquals("Int1 should be 1", handle.getAttribute("Int1", 0), 1); //$NON-NLS-1$ //$NON-NLS-2$
 		 assertTrue("Boolean1 should be true", handle.getAttribute("Boolean1", false)); //$NON-NLS-1$ //$NON-NLS-2$
 		 assertTrue("Boolean2 should be false", !handle.getAttribute("Boolean2", true)); //$NON-NLS-1$ //$NON-NLS-2$
 		 
 		 // ensure new handle is the index
 		 ILaunchConfiguration[] configs = getLaunchManager().getLaunchConfigurations();
 		 assertTrue("Configuration should exist in project index", existsIn(configs, handle)); //$NON-NLS-1$
 		 
 		 // get a working copy
 		 wc = handle.getWorkingCopy();
 		 ILaunchConfigurationWorkingCopy nested = wc.getWorkingCopy();
 		 
 		 // verify nested is same as original
 		 assertEquals("String1 should be String1", nested.getAttribute("String1", "Missing"), "String1"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
 		 assertEquals("Int1 should be 1", nested.getAttribute("Int1", 0), 1); //$NON-NLS-1$ //$NON-NLS-2$
 		 assertTrue("Boolean1 should be true", nested.getAttribute("Boolean1", false)); //$NON-NLS-1$ //$NON-NLS-2$
 		 assertTrue("Boolean2 should be false", !nested.getAttribute("Boolean2", true)); //$NON-NLS-1$ //$NON-NLS-2$
 		 
 		 // change an attribute in the nested working copy
 		 nested.setAttribute("String1", "StringOne"); //$NON-NLS-1$ //$NON-NLS-2$
 		 assertEquals("Wrong attribute value", nested.getAttribute("String1", "Missing"), "StringOne"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
 		 assertEquals("Wrong attribute value", wc.getAttribute("String1", "Missing"), "String1"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
 		 assertEquals("Wrong attribute value", handle.getAttribute("String1", "Missing"), "String1"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
 		 
 		 // save back to parent
 		 ILaunchConfigurationWorkingCopy parent = nested.getParent();
 		 assertEquals("Wrong parent", wc, parent); //$NON-NLS-1$
 		 assertNull("Should have no parent", wc.getParent()); //$NON-NLS-1$
 		 nested.doSave();
 		 assertEquals("Wrong attribute value", wc.getAttribute("String1", "Missing"), "StringOne");  //$NON-NLS-1$ //$NON-NLS-2$//$NON-NLS-3$ //$NON-NLS-4$
 		 assertEquals("Wrong attribute value", handle.getAttribute("String1", "Missing"), "String1"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
 		 
 		 // check originals
 		 assertEquals("Wrong original config" , handle, wc.getOriginal()); //$NON-NLS-1$
 		 assertEquals("Wrong original config" , handle, nested.getOriginal()); //$NON-NLS-1$
 		 
 		 // cleanup
 		 handle.delete();
 		 assertTrue("Config should not exist after deletion", !handle.exists()); //$NON-NLS-1$
 	}	
 	
 	/**
 	 * Creates a configuration in an EFS linked folder. Deletes configuration directly.
 	 * 
 	 * @throws CoreException
 	 * @throws URISyntaxException
 	 */	
 	public void testCreateDeleteEFS() throws CoreException, URISyntaxException {
 		IFileSystem fileSystem = EFS.getFileSystem("debug");
 		assertNotNull("Missing debug EFS", fileSystem);
 		
 		// create folder in EFS
 		IFolder folder = getJavaProject().getProject().getFolder("efs");
 		folder.createLink(new URI("debug", Path.ROOT.toString(), null), 0, null);
 		
 		// create configuration
 		ILaunchConfigurationWorkingCopy wc = newConfiguration(folder, "efsConfig"); //$NON-NLS-1$
 		ILaunchConfiguration handle = wc.doSave();
 		assertTrue("Configuration should exist", handle.exists()); //$NON-NLS-1$
 		
 		 // retrieve attributes
 		 assertTrue("String1 should be String1", handle.getAttribute("String1", "Missing").equals("String1")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
 		 assertTrue("Int1 should be 1", handle.getAttribute("Int1", 0) == 1); //$NON-NLS-1$ //$NON-NLS-2$
 		 assertTrue("Boolean1 should be true", handle.getAttribute("Boolean1", false)); //$NON-NLS-1$ //$NON-NLS-2$
 		 assertTrue("Boolean2 should be false", !handle.getAttribute("Boolean2", true)); //$NON-NLS-1$ //$NON-NLS-2$		
 		
 		// delete configuration
 		handle.delete();
 		assertTrue("Configuration should not exist", !handle.exists()); //$NON-NLS-1$
 		
 		// cleanup
 		folder.delete(IResource.NONE, null);
 	}
 	
 	/**
 	 * Creates a configuration in an EFS linked folder. Deletes the folder to ensure the
 	 * configuration is also deleted.
 	 * 
 	 * @throws CoreException
 	 * @throws URISyntaxException
 	 */
 	public void testCreateDeleteEFSLink() throws CoreException, URISyntaxException {
 		IFileSystem fileSystem = EFS.getFileSystem("debug");
 		assertNotNull("Missing debug EFS", fileSystem);
 		
 		// create folder in EFS
 		IFolder folder = getJavaProject().getProject().getFolder("efs2");
 		folder.createLink(new URI("debug", Path.ROOT.toString(), null), 0, null);
 		
 		// create configuration
 		ILaunchConfigurationWorkingCopy wc = newConfiguration(folder, "efsConfig"); //$NON-NLS-1$
 		ILaunchConfiguration handle = wc.doSave();
 		assertTrue("Configuration should exist", handle.exists()); //$NON-NLS-1$
 		
 		 // retrieve attributes
 		 assertTrue("String1 should be String1", handle.getAttribute("String1", "Missing").equals("String1")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
 		 assertTrue("Int1 should be 1", handle.getAttribute("Int1", 0) == 1); //$NON-NLS-1$ //$NON-NLS-2$
 		 assertTrue("Boolean1 should be true", handle.getAttribute("Boolean1", false)); //$NON-NLS-1$ //$NON-NLS-2$
 		 assertTrue("Boolean2 should be false", !handle.getAttribute("Boolean2", true)); //$NON-NLS-1$ //$NON-NLS-2$		
 				
 		// cleanup
 		folder.delete(IResource.NONE, null);
 		assertTrue("Configuration should not exist", !handle.exists()); //$NON-NLS-1$
 	}	
 	
 	/**
 	 * Test that renaming a project with a linked EFS folder containing a shared
 	 * launch configuration is properly updated.
 	 * 
 	 * @throws Exception
 	 */
 	public void testEFSProjectRename() throws Exception {
         // create test project
         IProject pro = ResourcesPlugin.getWorkspace().getRoot().getProject("RenameEFS");
         if (pro.exists()) {
             pro.delete(true, true, null);
         }
         // create project with source folders and output location
         IJavaProject javaProject = JavaProjectHelper.createJavaProject("RenameEFS");
         JavaProjectHelper.addSourceContainer(javaProject, "src", "bin");
 
         // add rt.jar
         IVMInstall vm = JavaRuntime.getDefaultVMInstall();
         assertNotNull("No default JRE", vm);
         JavaProjectHelper.addContainerEntry(javaProject, new Path(JavaRuntime.JRE_CONTAINER));	
         
 		IFileSystem fileSystem = EFS.getFileSystem("debug");
 		assertNotNull("Missing debug EFS", fileSystem);
 		
 		// create folder in EFS
 		IFolder folder = javaProject.getProject().getFolder("efs2");
 		folder.createLink(new URI("debug", Path.ROOT.toString(), null), 0, null);
 		
 		// create configuration
 		ILaunchConfigurationWorkingCopy wc = newConfiguration(folder, "efsConfig"); //$NON-NLS-1$
 		ILaunchConfiguration handle = wc.doSave();
 		assertTrue("Configuration should exist", handle.exists()); //$NON-NLS-1$
 		
 		// retrieve attributes
 		assertTrue("String1 should be String1", handle.getAttribute("String1", "Missing").equals("String1")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
 		assertTrue("Int1 should be 1", handle.getAttribute("Int1", 0) == 1); //$NON-NLS-1$ //$NON-NLS-2$
 		assertTrue("Boolean1 should be true", handle.getAttribute("Boolean1", false)); //$NON-NLS-1$ //$NON-NLS-2$
 		assertTrue("Boolean2 should be false", !handle.getAttribute("Boolean2", true)); //$NON-NLS-1$ //$NON-NLS-2$		
 		
 		// rename project
 		IProject project = javaProject.getProject();
 		IProjectDescription description = project.getDescription();
 		description.setName("SFEemaneR"); // reverse name
 		project.move(description, false, null);
 		
 		// original configuration should no longer exist - handle out of date
 		assertTrue("Configuration should not exist", !handle.exists()); //$NON-NLS-1$
 		
 		// get the new handle
 		project = ResourcesPlugin.getWorkspace().getRoot().getProject("SFEemaneR");
 		assertTrue("Project should exist", project.exists());
 		IFile file = project.getFile(new Path("efs2/efsConfig.launch"));
 		assertTrue("launch config file should exist", file.exists());
 		handle = getLaunchManager().getLaunchConfiguration(file);
 		assertTrue("launch config should exist", handle.exists());
 		
 		// retrieve attributes
 		assertTrue("String1 should be String1", handle.getAttribute("String1", "Missing").equals("String1")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
 		assertTrue("Int1 should be 1", handle.getAttribute("Int1", 0) == 1); //$NON-NLS-1$ //$NON-NLS-2$
 		assertTrue("Boolean1 should be true", handle.getAttribute("Boolean1", false)); //$NON-NLS-1$ //$NON-NLS-2$
 		assertTrue("Boolean2 should be false", !handle.getAttribute("Boolean2", true)); //$NON-NLS-1$ //$NON-NLS-2$		
 		
 		// validate shared location
 		assertEquals("Shared location should be updated", file, handle.getFile());
 		
 		// cleanup
 		project.delete(IResource.NONE, null);
 		assertTrue("Configuration should not exist", !handle.exists()); //$NON-NLS-1$
         
 	}
 	
 	/**
 	 * Tests launch configuration import.
 	 * 
 	 * @throws Exception
 	 */
 	public void testImport() throws Exception {
 		// create a shared configuration "Import4" in the workspace to be overwritten on import
 		 ILaunchConfigurationWorkingCopy wc = newConfiguration(getJavaProject().getProject(), "Import4"); //$NON-NLS-1$
 		 ILaunchConfiguration handle = wc.doSave();
 		 assertTrue("Configuration should exist", handle.exists()); //$NON-NLS-1$
 		 
 		 File dir = JavaTestPlugin.getDefault().getFileInPlugin(new Path("test-import"));
 		 assertTrue("Import directory does not exist", dir.exists());
 		 LaunchManager manager = (LaunchManager) getLaunchManager();
 
 		 Listener listener = new Listener();
 		 try {
 			 manager.addLaunchConfigurationListener(listener);
 			 // import
			 manager.importConfigurations(dir.listFiles(), null);
 		 
 			 // should be one removed
 			 List removed = listener.getRemoved();
 			 assertEquals("Should be one removed config", 1, removed.size());
 			 assertTrue("Import4 should be removed", removed.contains(handle));
 			 
 			 // should be 5 added
 			 List added = listener.getAdded();
 			 assertEquals("Should be 5 added configs", 5, added.size());
 			 Set names = new HashSet();
 			 Iterator iterator = added.iterator();
 			 while (iterator.hasNext()) {
 				ILaunchConfiguration lc = (ILaunchConfiguration) iterator.next();
 				names.add(lc.getName());
 			}
 			assertTrue("Missing Name", names.contains("Import1"));
 			assertTrue("Missing Name", names.contains("Import2"));
 			assertTrue("Missing Name", names.contains("Import3"));
 			assertTrue("Missing Name", names.contains("Import4"));
 			assertTrue("Missing Name", names.contains("Import5"));
 			
 			// should be one changed
 			List changed = listener.getChanged();
 			assertEquals("Should be 1 changed config", 1, changed.size());
 			assertEquals("Wrong changed config", "Import4", ((ILaunchConfiguration)changed.get(0)).getName());
 		 } finally {
 			 manager.removeLaunchConfigurationListener(listener);
 		 }
 		 
 	}
 }
 
 
