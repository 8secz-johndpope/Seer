 /***************************************************************************************************
  * Copyright (c) 2003, 2004 IBM Corporation and others. All rights reserved. This program and the
  * accompanying materials are made available under the terms of the Eclipse Public License v1.0
  * which accompanies this distribution, and is available at
  * http://www.eclipse.org/legal/epl-v10.html
  * 
  * Contributors: IBM Corporation - initial API and implementation
  **************************************************************************************************/
 package org.eclipse.wst.web.internal.deployables;
 
 import java.util.ArrayList;
 import java.util.Iterator;
 
 import org.eclipse.core.resources.IProject;
 import org.eclipse.wst.server.core.IModule;
 import org.eclipse.wst.server.core.model.ModuleDelegate;
 import org.eclipse.wst.server.core.util.ProjectModuleFactoryDelegate;
 import org.eclipse.wst.web.internal.ISimpleWebNatureConstants;
 import org.eclipse.wst.web.internal.operation.IBaseWebNature;
 
 public class StaticWebDeployableFactory extends ProjectModuleFactoryDelegate {
 	private static final String ID = "org.eclipse.jst.j2ee.internal.web.deployables.static"; //$NON-NLS-1$
 	protected ArrayList moduleDelegates = new ArrayList();
 
 	/*
 	 * @see DeployableProjectFactoryDelegate#getFactoryID()
 	 */
 	public String getFactoryId() {
 		return ID;
 	}
 
 	/**
 	 * Returns true if the project represents a deployable project of this type.
 	 * 
 	 * @param project
 	 *            org.eclipse.core.resources.IProject
 	 * @return boolean
 	 */
 	protected boolean isValidModule(IProject project) {
 		return false;
 	}
 
 	/**
 	 * Creates the deployable project for the given project.
 	 * 
 	 * @param project
 	 *            org.eclipse.core.resources.IProject
 	 * @return com.ibm.etools.server.core.model.IDeployableProject
 	 */
 	protected IModule createModule(IProject project) {
 		try {
 			IModule deployable = null;
 			StaticWebDeployable projectModule = null;
 			IBaseWebNature nature = (IBaseWebNature) project.getNature(ISimpleWebNatureConstants.STATIC_NATURE_ID);
 			deployable = nature.getModule();
 			if (deployable == null) {
 				projectModule = new StaticWebDeployable(nature.getProject());
 				deployable = createModule(projectModule.getId(), projectModule.getName(), projectModule.getType(), projectModule.getVersion(), projectModule.getProject());
 				nature.setModule(deployable);
 				projectModule.initialize(deployable);
 				//deployable = projectModule.getModule();
 			}
 			moduleDelegates.add(projectModule);
 			return deployable;
 		} catch (Exception e) {
 			//Ignore
 		}
 		return null;
 	}
 
 	/*
 	 * (non-Javadoc)
 	 * 
 	 * @see org.eclipse.wst.server.core.model.ModuleFactoryDelegate#getModuleDelegate(org.eclipse.wst.server.core.IModule)
 	 */
 	public ModuleDelegate getModuleDelegate(IModule module) {
 		for (Iterator iter = moduleDelegates.iterator(); iter.hasNext();) {
 			ModuleDelegate element = (ModuleDelegate) iter.next();
 			if (module == element.getModule())
 				return element;
 		}
 		return null;
 
 	}
 
 	/*
 	 * (non-Javadoc)
 	 * 
 	 * @see org.eclipse.wst.server.core.model.ModuleFactoryDelegate#getModules()
 	 */
 	public IModule[] getModules() {
 		if (projects == null || projects.isEmpty())
 			cacheModules();
 		int i = 0;
 		Iterator modules = projects.values().iterator();
 		IModule[] modulesArray = new IModule[projects.values().size()];
 		while (modules.hasNext()) {
 			IModule element = (IModule) modules.next();
 			modulesArray[i++] = element;
 
 		}
 		return modulesArray;
 
 	}
 
 	/* (non-Javadoc)
 	 * @see org.eclipse.wst.server.core.util.ProjectModuleFactoryDelegate#createModules(org.eclipse.core.resources.IProject)
 	 */
 	protected IModule[] createModules(IProject project) {
 		// TODO Auto-generated method stub
		return new IModule[] {createModule(project)};
 	}
 }
