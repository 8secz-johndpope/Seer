 /*******************************************************************************
  * Copyright (c) 2003, 2004 IBM Corporation and others.
  * All rights reserved. This program and the accompanying materials
  * are made available under the terms of the Eclipse Public License v1.0
  * which accompanies this distribution, and is available at
  * http://www.eclipse.org/legal/epl-v10.html
  * 
  * Contributors:
  * IBM Corporation - initial API and implementation
  *******************************************************************************/
 package org.eclipse.wst.common.componentcore.internal.util;
 
 import java.util.ArrayList;
 import java.util.Arrays;
 import java.util.List;
 
 import org.eclipse.core.resources.IContainer;
 import org.eclipse.core.resources.IFile;
 import org.eclipse.core.resources.IFolder;
 import org.eclipse.core.resources.IProject;
 import org.eclipse.core.resources.IResource;
 import org.eclipse.core.resources.ResourceAttributes;
 import org.eclipse.core.resources.ResourcesPlugin;
 import org.eclipse.core.runtime.CoreException;
 import org.eclipse.core.runtime.IPath;
 import org.eclipse.core.runtime.Path;
 import org.eclipse.emf.ecore.EObject;
 import org.eclipse.emf.ecore.resource.Resource;
 import org.eclipse.jem.util.emf.workbench.ProjectUtilities;
 import org.eclipse.wst.common.componentcore.ArtifactEdit;
 import org.eclipse.wst.common.componentcore.ComponentCore;
 import org.eclipse.wst.common.componentcore.ModuleCoreNature;
 import org.eclipse.wst.common.componentcore.datamodel.properties.ICreateReferenceComponentsDataModelProperties;
 import org.eclipse.wst.common.componentcore.internal.operation.CreateReferenceComponentsDataModelProvider;
 import org.eclipse.wst.common.componentcore.internal.operation.CreateReferenceComponentsOp;
 import org.eclipse.wst.common.componentcore.internal.operation.RemoveReferenceComponentOperation;
 import org.eclipse.wst.common.componentcore.internal.operation.RemoveReferenceComponentsDataModelProvider;
 import org.eclipse.wst.common.componentcore.internal.resources.VirtualComponent;
 import org.eclipse.wst.common.componentcore.resources.IVirtualComponent;
 import org.eclipse.wst.common.componentcore.resources.IVirtualFolder;
 import org.eclipse.wst.common.componentcore.resources.IVirtualResource;
 import org.eclipse.wst.common.frameworks.datamodel.DataModelFactory;
 import org.eclipse.wst.common.frameworks.datamodel.IDataModel;
 import org.eclipse.wst.common.internal.emfworkbench.WorkbenchResourceHelper;
 
 public class ComponentUtilities {
 
 	/**
 	 * Ensure the container is not read-only.
 	 * <p>
 	 * For Linux, a Resource cannot be created in a ReadOnly folder. This is only necessary for new
 	 * files.
 	 * 
 	 * @param resource
 	 *            workspace resource to make read/write
 	 * @plannedfor 1.0.0
 	 */
 	public static void ensureContainerNotReadOnly(IResource resource) {
 		if (resource != null && !resource.exists()) { // it must be new
 			IContainer container = resource.getParent();
 			if (container != null) {
 				ResourceAttributes attr = container.getResourceAttributes();
 				if (!attr.isReadOnly())
 					container = container.getParent();
 				attr.setReadOnly(false);
 			}
 		}
 	}
 
 	public static IFolder createFolderInComponent(IVirtualComponent component, String folderName) throws CoreException {
 		if (folderName != null) {
 			IVirtualFolder rootfolder = component.getRootFolder();
 			IFolder folder = ResourcesPlugin.getWorkspace().getRoot().getFolder(new Path(rootfolder.getProject().getName()).append(folderName));
 			if (!folder.exists()) {
 				ProjectUtilities.ensureContainerNotReadOnly(folder);
 				folder.create(true, true, null);
 			}
 			return folder;
 		}
 		return null;
 	}
 
 
 
 	public static ArtifactEdit getArtifactEditForRead(IVirtualComponent comp) {
 		if (comp != null) {
			ArtifactEditRegistryReader reader = ArtifactEditRegistryReader.instance();
 			IArtifactEditFactory factory = reader.getArtifactEdit(comp.getProject());
			if (factory != null)
				return factory.createArtifactEditForRead(comp);
 		}
 		return null;
 	}
 
 
 
 	public static IFile findFile(IVirtualComponent comp, IPath aPath) throws CoreException {
 		if (comp == null || aPath == null)
 			return null;
 		IVirtualFolder root = comp.getRootFolder();
 		IVirtualResource file = root.findMember(aPath);
 		if (file != null)
 			return (IFile) file.getUnderlyingResource();
 		return null;
 	}
 
 	public static IVirtualComponent findComponent(IResource res) {
 
 		return (IVirtualComponent)res.getAdapter(IVirtualComponent.class);
 	}
 	
 	/**
 	 * **********************Please read java doc before using this api*******************
 	 * This is a very expensive api from a performance point as it does a structure edit
 	 * access and release for each component in the workspace. Use this api very sparingly
 	 * and if used cached the information returned by this api for further processing
 	 * @return - A an array of all virtual components in the workspace
 	 * ***********************************************************************************
 	 */
 
 	public static IVirtualComponent[] getAllWorkbenchComponents() {
 		List components = new ArrayList();
 		List projects = Arrays.asList(ResourcesPlugin.getWorkspace().getRoot().getProjects());
 		for (int i = 0; i < projects.size(); i++) {
 			if( ModuleCoreNature.isFlexibleProject((IProject)projects.get(i))){
 				IVirtualComponent wbComp = ComponentCore.createComponent((IProject)projects.get(i));
 				components.add(wbComp);
 			}
 		}
 		VirtualComponent[] temp = (VirtualComponent[]) components.toArray(new VirtualComponent[components.size()]);
 		return temp;
 	}
 	
 	/**
 	 * **********************Please read java doc before using this api*******************
 	 * This is a very expensive api from a performance point as it does a structure edit
 	 * access and release for each component in the workspace. Use this api very sparingly
 	 * and if used cached the information returned by this api for further processing.
 	 * 
 	 * @return - A virtual component in the workspace
 	 * ***********************************************************************************
 	 */
 	public static IVirtualComponent getComponent(String componentName) {
 		IVirtualComponent[] allComponents = getAllWorkbenchComponents();
 		for (int i = 0; i < allComponents.length; i++) {
 			if (allComponents[i].getName().equals(componentName))
 				return allComponents[i];
 		}
 		return null;
 	}
 
 
 
 	public static ArtifactEdit getArtifactEditForWrite(IVirtualComponent comp) {
		if (comp != null) {
			ArtifactEditRegistryReader reader = ArtifactEditRegistryReader.instance();
			IArtifactEditFactory factory = reader.getArtifactEdit(comp.getProject());
			if (factory != null)
				return factory.createArtifactEditForWrite(comp);
		}
		return null;
 	}
 
 	public static IVirtualComponent findComponent(EObject anObject) {
 		Resource res = anObject.eResource();
 		return findComponent(res);
 	}
 
 	public static IVirtualComponent findComponent(Resource aResource) {
 		return (IVirtualComponent)WorkbenchResourceHelper.getFile(aResource).getAdapter(IVirtualComponent.class);
 	}
 
 //	public static JavaProjectMigrationOperation createFlexJavaProjectForProjectOperation(IProject project) {
 //		IDataModel model = DataModelFactory.createDataModel(new JavaProjectMigrationDataModelProvider());
 //		model.setProperty(IJavaProjectMigrationDataModelProperties.PROJECT_NAME, project.getName());
 //		return new JavaProjectMigrationOperation(model);
 //	}
 
 	public static CreateReferenceComponentsOp createReferenceComponentOperation(IVirtualComponent sourceComponent, List targetComponentProjects) {
 		IDataModel model = DataModelFactory.createDataModel(new CreateReferenceComponentsDataModelProvider());
 		model.setProperty(ICreateReferenceComponentsDataModelProperties.SOURCE_COMPONENT, sourceComponent);
 		List modHandlesList = (List) model.getProperty(ICreateReferenceComponentsDataModelProperties.TARGET_COMPONENT_LIST);
 		modHandlesList.addAll(targetComponentProjects);
 		model.setProperty(ICreateReferenceComponentsDataModelProperties.TARGET_COMPONENT_LIST, modHandlesList);
 		
 		return new CreateReferenceComponentsOp(model);
 	}
 	
 	public static CreateReferenceComponentsOp createWLPReferenceComponentOperation(IVirtualComponent sourceComponent, List targetComponentProjects) {
 		IDataModel model = DataModelFactory.createDataModel(new CreateReferenceComponentsDataModelProvider());
 		model.setProperty(ICreateReferenceComponentsDataModelProperties.SOURCE_COMPONENT, sourceComponent);
 		List modHandlesList = (List) model.getProperty(ICreateReferenceComponentsDataModelProperties.TARGET_COMPONENT_LIST);
 		modHandlesList.addAll(targetComponentProjects);
 		model.setProperty(ICreateReferenceComponentsDataModelProperties.TARGET_COMPONENT_LIST, modHandlesList);
 		model.setProperty(ICreateReferenceComponentsDataModelProperties.TARGET_COMPONENTS_DEPLOY_PATH,"/WEB-INF/lib"); //$NON-NLS-1$
 		return new CreateReferenceComponentsOp(model);
 	}
 
 	public static RemoveReferenceComponentOperation removeReferenceComponentOperation(IVirtualComponent sourceComponent, List targetComponentProjects) {
 		IDataModel model = DataModelFactory.createDataModel(new RemoveReferenceComponentsDataModelProvider());
 		model.setProperty(ICreateReferenceComponentsDataModelProperties.SOURCE_COMPONENT, sourceComponent);
 		List modHandlesList = (List) model.getProperty(ICreateReferenceComponentsDataModelProperties.TARGET_COMPONENT_LIST);
 		modHandlesList.addAll(targetComponentProjects);
 		model.setProperty(ICreateReferenceComponentsDataModelProperties.TARGET_COMPONENT_LIST, modHandlesList);
 		return new RemoveReferenceComponentOperation(model);
 
 	}
 	
 	public static IVirtualComponent[] getComponents(IProject[] projects) {
 		List result = new ArrayList();
 		if (projects!=null) {
 			for (int i=0; i<projects.length; i++) {
 				if (projects[i] != null) {
 					IVirtualComponent comp = ComponentCore.createComponent(projects[i]);
 					if (comp != null && comp.exists())
 						result.add(comp);
 				}
 			}
 		}
 		return (IVirtualComponent[]) result.toArray(new IVirtualComponent[result.size()]);
 	}
 
 }
