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
 package org.eclipse.team.internal.ui.mapping;
 
 import java.util.*;
 
 import org.eclipse.core.resources.*;
 import org.eclipse.core.resources.mapping.*;
 import org.eclipse.core.runtime.IPath;
 import org.eclipse.core.runtime.IProgressMonitor;
 import org.eclipse.jface.util.PropertyChangeEvent;
 import org.eclipse.jface.viewers.*;
 import org.eclipse.swt.widgets.Control;
 import org.eclipse.swt.widgets.Tree;
 import org.eclipse.team.core.diff.*;
 import org.eclipse.team.core.mapping.*;
 import org.eclipse.team.core.mapping.provider.ResourceDiffTree;
 import org.eclipse.team.internal.ui.*;
 import org.eclipse.team.ui.mapping.SynchronizationContentProvider;
 import org.eclipse.team.ui.synchronize.ISynchronizePageConfiguration;
 import org.eclipse.ui.model.WorkbenchContentProvider;
 import org.eclipse.ui.navigator.ICommonContentExtensionSite;
 
 /**
  * This content provider displays the mappings as a flat list 
  * of elements.
  * <p>
  * There are three use-cases we need to consider. The first is when there
  * are resource level mappings to be displayed. The second is when there
  * are mappings from a model provider that does not have a content provider
  * registered. The third is for the case where a resource mapping does not
  * have a model provider registered (this may be considered an error case).
  *
  */
 public class ResourceModelContentProvider extends SynchronizationContentProvider implements ITreePathContentProvider {
 
 	private WorkbenchContentProvider provider;
 
 	/* (non-Javadoc)
 	 * @see org.eclipse.team.internal.ui.mapping.AbstractTeamAwareContentProvider#getDelegateContentProvider()
 	 */
 	protected ITreeContentProvider getDelegateContentProvider() {
 		if (provider == null)
 			provider = new WorkbenchContentProvider();
 		return provider;
 	}
 
 	/* (non-Javadoc)
 	 * @see org.eclipse.team.internal.ui.mapping.AbstractTeamAwareContentProvider#getModelProviderId()
 	 */
 	protected String getModelProviderId() {
 		return ModelProvider.RESOURCE_MODEL_PROVIDER_ID;
 	}
 
 	/* (non-Javadoc)
 	 * @see org.eclipse.team.internal.ui.mapping.AbstractTeamAwareContentProvider#getModelRoot()
 	 */
 	protected Object getModelRoot() {
 		return ResourcesPlugin.getWorkspace().getRoot();
 	}
 
 	/* (non-Javadoc)
 	 * @see org.eclipse.team.ui.mapping.SynchronizationContentProvider#isInScope(org.eclipse.team.core.mapping.IResourceMappingScope, java.lang.Object, java.lang.Object)
 	 */
 	protected boolean isInScope(ISynchronizationScope scope, Object parent, Object elementOrPath) {
 		Object object = internalGetElement(elementOrPath);
 		if (object instanceof IResource) {
 			IResource resource = (IResource) object;
 			if (resource == null)
 				return false;
 			if (!resource.getProject().isAccessible())
 				return false;
 			if (scope.contains(resource))
 				return true;
 			if (hasChildrenInScope(scope, object, resource)) {
 				return true;
 			}
 		}
 		return false;
 	}
 	
 	private boolean hasChildrenInScope(ISynchronizationScope scope, Object object, IResource resource) {
 		if (!resource.isAccessible())
 			return false;
 		IResource[] roots = scope.getRoots();
 		for (int i = 0; i < roots.length; i++) {
 			IResource root = roots[i];
 			if (resource.getFullPath().isPrefixOf(root.getFullPath()))
 				return true;
 		}
 		return false;
 	}
 	
 	/* (non-Javadoc)
 	 * @see org.eclipse.team.ui.mapping.SynchronizationContentProvider#init(org.eclipse.ui.navigator.ICommonContentExtensionSite)
 	 */
 	public void init(ICommonContentExtensionSite site) {
 		super.init(site);
 		TeamUIPlugin.getPlugin().getPreferenceStore().addPropertyChangeListener(this);
 		ISynchronizePageConfiguration configuration = getConfiguration();
 		if (configuration != null)
 			configuration.setProperty(ResourceModelTraversalCalculator.PROP_TRAVERSAL_CALCULATOR, new ResourceModelTraversalCalculator());
 	}
 	
 	/* (non-Javadoc)
 	 * @see org.eclipse.team.internal.ui.mapping.AbstractTeamAwareContentProvider#dispose()
 	 */
 	public void dispose() {
 		provider.dispose();
 		super.dispose();
 		TeamUIPlugin.getPlugin().getPreferenceStore().removePropertyChangeListener(this);
 	}
 	
 	public void propertyChange(PropertyChangeEvent event) {
 		if (event.getProperty().equals(IPreferenceIds.SYNCVIEW_DEFAULT_LAYOUT)) {
 			refresh();
 		}
 		super.propertyChange(event);
 	}
 
 	/* (non-Javadoc)
 	 * @see org.eclipse.team.ui.mapping.SynchronizationContentProvider#getChildrenInContext(org.eclipse.team.core.mapping.ISynchronizationContext, java.lang.Object, java.lang.Object[])
 	 */
 	protected Object[] getChildrenInContext(ISynchronizationContext context, Object parentOrPath, Object[] children) {
 		Object parent = internalGetElement(parentOrPath);
 		if (parent instanceof IResource) {
 			IResource resource = (IResource) parent;
 			if (resource.getType() == IResource.PROJECT && !resource.getProject().isAccessible())
 				return new Object[0];
 			IResourceDiffTree diffTree = context.getDiffTree();
 			//TODO: pass path to traversal calculator
 			Object[] allChildren = getTraversalCalculator().filterChildren(diffTree, resource, parentOrPath, children);
 			return super.getChildrenInContext(context, parentOrPath, allChildren);
 		}
 		return super.getChildrenInContext(context, parentOrPath, children);
 	}
 
 	protected ResourceTraversal[] getTraversals(ISynchronizationContext context, Object elementOrPath) {
 		Object object = internalGetElement(elementOrPath);
 		ISynchronizationScope scope = context.getScope();
 		// First see if the object is a root of the scope
 		ResourceMapping mapping = scope.getMapping(object);
 		if (mapping != null)
 			return scope.getTraversals(mapping);
 		// Next, check if the object is within the scope
 		if (object instanceof IResource) {
 			IResource resource = (IResource) object;
 			if (scope.contains(resource)) {
 				List result = new ArrayList();
 				ResourceTraversal[] traversals = scope.getTraversals();
 				for (int i = 0; i < traversals.length; i++) {
 					ResourceTraversal traversal = traversals[i];
 					if (traversal.contains(resource)) {
 						boolean include = false;
 						int depth = traversal.getDepth();
 						if (depth == IResource.DEPTH_INFINITE) {
 							include = true;
 						} else {
 							IResource[] roots = traversal.getResources();
 							for (int j = 0; j < roots.length; j++) {
 								IResource root = roots[j];
 								if (root.equals(resource)) {
 									include = true;
 									break;
 								}
 								if (root.getFullPath().equals(resource.getFullPath().removeLastSegments(1)) && depth == IResource.DEPTH_ONE) {
 									include = true;
 									depth = IResource.DEPTH_ZERO;
 									break;
 								}
 							}
 						}
 						if (include)
 							result.add(new ResourceTraversal(new IResource[] { resource}, depth, IResource.NONE));
 					}
 				}
 				return (ResourceTraversal[]) result.toArray(new ResourceTraversal[result.size()]);
 			} else {
 				// The resource is a parent of an in-scope resource
 				// TODO: fails due to se of roots
 				ResourceMapping[] mappings = scope.getMappings(ModelProvider.RESOURCE_MODEL_PROVIDER_ID);
 				List result = new ArrayList();
 				for (int i = 0; i < mappings.length; i++) {
 					ResourceMapping resourceMapping = mappings[i];
 					if (resourceMapping.getModelObject() instanceof IResource) {
 						IResource root = (IResource) resourceMapping.getModelObject();
 						if (resource.getFullPath().isPrefixOf(root.getFullPath())) {
 							mapping = scope.getMapping(root);
 							if (mapping != null) {
 								ResourceTraversal[] traversals = scope.getTraversals(mapping);
 								result.addAll(Arrays.asList(traversals));
 							}
 						}
 					}
 				}
 				return (ResourceTraversal[]) result.toArray(new ResourceTraversal[result.size()]);
 			}
 		}
 		return new ResourceTraversal[0];
 	}
 	
 	/* (non-Javadoc)
 	 * @see org.eclipse.team.ui.mapping.SynchronizationContentProvider#hasChildrenInContext(org.eclipse.team.core.mapping.ISynchronizationContext, java.lang.Object)
 	 */
 	protected boolean hasChildrenInContext(ISynchronizationContext context, Object elementOrPath) {
 		Object element = internalGetElement(elementOrPath);
 		if (element instanceof IContainer) {
 			IContainer container = (IContainer) element;
 			// For containers check to see if the delta contains any children
 			if (context != null) {
 				IDiffTree tree = context.getDiffTree();
 				if (tree.getChildren(container.getFullPath()).length > 0) {
 					return true;
 				}
 			}
 		}
 		return false;
 	}
 	
 	/* (non-Javadoc)
 	 * @see org.eclipse.team.ui.mapping.SynchronizationContentProvider#propertyChanged(int, org.eclipse.core.runtime.IPath[])
 	 */
 	public void propertyChanged(IDiffTree tree, final int property, final IPath[] paths) {
 		Utils.syncExec(new Runnable() {
 			public void run() {
 				ISynchronizationContext context = getContext();
 				if (context != null) {
 					updateLabels(context, paths);
 				}
 			}
 		}, (StructuredViewer)getViewer());
 	}
 
 	private IResource[] getResources(ISynchronizationContext context, IPath[] paths) {
 		List resources = new ArrayList();
 		for (int i = 0; i < paths.length; i++) {
 			IPath path = paths[i];
 			IResource resource = getResource(context, path);
 			if (resource != null)
 				resources.add(resource);
 		}
 		return (IResource[]) resources.toArray(new IResource[resources.size()]);
 	}
 
 	private IResource getResource(ISynchronizationContext context, IPath path) {
 		// Does the resource exist locally
 		IResource resource = ResourcesPlugin.getWorkspace().getRoot().findMember(path);
 		if (resource != null) {
 			return resource;
 		}
 		// Look in the diff tree for a phantom
 		if (context != null) {
 			IResourceDiffTree diffTree = context.getDiffTree();
 			// Is there a diff for the path
 			IDiff node = diffTree.getDiff(path);
 			if (node != null) {
 				return diffTree.getResource(node);
 			}
 			// Is there any descendants of the path
 			if (diffTree.getChildren(path).length > 0) {
 				if (path.segmentCount() == 1) {
 					return ResourcesPlugin.getWorkspace().getRoot().getProject(path.segment(0));
 				} else if (path.segmentCount() > 1) {
 					return ResourcesPlugin.getWorkspace().getRoot().getFolder(path);
 				}
 			}
 		}
 		return null;
 	}
 	
 	protected StructuredViewer getStructuredViewer() {
 		return (StructuredViewer)getViewer();
 	}
 	
 	public Object[] getChildren(Object parent) {
 		if (parent instanceof ISynchronizationContext) {
 			// Put the resource projects directly under the context
 			parent = getModelRoot();
 		}
 		return super.getChildren(parent);
 	}
 	
 	public boolean hasChildren(Object element) {
 		if (element instanceof ISynchronizationContext) {
 			// Put the resource projects directly under the context
 			element = getModelRoot();
 		}
 		return super.hasChildren(element);
 	}
 	
 	public Object[] getElements(Object parent) {
 		if (parent instanceof ISynchronizationContext) {
 			// Put the resource projects directly under the context
 			parent = getModelRoot();
 		}
 		return super.getElements(parent);
 	}
 	
 	public Object getParent(Object elementOrPath) {
 		Object element = internalGetElement(elementOrPath);
 		if (element instanceof IProject) {
 			ISynchronizationContext context = getContext();
 			if (context != null)
 				return context;
 		}
 		return super.getParent(elementOrPath);
 	}
 	
 	protected void refresh() {
 		Utils.syncExec(new Runnable() {
 			public void run() {
 				TreeViewer treeViewer = ((TreeViewer)getViewer());
 				treeViewer.refresh();
 			}
 		
 		}, getViewer().getControl());
 	}
 
 	protected void updateLabels(ISynchronizationContext context, final IPath[] paths) {
 		IResource[] resources = getResources(context, paths);
 		if (resources.length > 0)
 			((AbstractTreeViewer)getViewer()).update(resources, null);
 	}
 	
 	protected ResourceModelTraversalCalculator getTraversalCalculator() {
 		return (ResourceModelTraversalCalculator)getConfiguration().getProperty(ResourceModelTraversalCalculator.PROP_TRAVERSAL_CALCULATOR);
 	}
 	
 	protected boolean isVisible(IDiff diff) {
 		return super.isVisible(diff);
 	}
 
 	public Object[] getChildren(TreePath parentPath) {
 		return getChildren((Object)parentPath);
 	}
 
 	public boolean hasChildren(TreePath path) {
 		return hasChildren((Object)path);
 	}
 
 	public TreePath[] getParents(Object element) {
 		if (element instanceof IResource) {
 			IResource resource = (IResource) element;
 			IResource[] resourcePath = new IResource[resource.getFullPath().segmentCount()];
 			for (int i = resourcePath.length - 1; i >= 0; i--) {
 				resourcePath[i] = resource;
 				resource = resource.getParent();
 			}
 			TreePath treePath = TreePath.EMPTY;
 			for (int i = 0; i < resourcePath.length; i++) {
 				IResource r = resourcePath[i];
 				treePath = treePath.createChildPath(r);
 			}
 			return new TreePath[] { treePath };
 		}
 		return null;
 	}
 	
 	private Object internalGetElement(Object elementOrPath) {
 		if (elementOrPath instanceof TreePath) {
 			TreePath tp = (TreePath) elementOrPath;
 			return tp.getLastSegment();
 		}
 		return elementOrPath;
 	}
 	
 	public void diffsChanged(final IDiffChangeEvent event, IProgressMonitor monitor) {
 		Utils.syncExec(new Runnable() {
 			public void run() {
 				handleChange(event);
 			}
 		}, (StructuredViewer)getViewer());
 	}
 
 	private void handleChange(IDiffChangeEvent event) {
 		Set existingProjects = getVisibleProjects();
 		IProject[] changedProjects = getChangedProjects(event);
 		List refreshes = new ArrayList(changedProjects.length);
 		List additions = new ArrayList(changedProjects.length);
 		List removals = new ArrayList(changedProjects.length);
 		for (int i = 0; i < changedProjects.length; i++) {
 			IProject project = changedProjects[i];
 			if (hasDiffs(event.getTree(), project)) {
 				if (existingProjects.contains(project)) {
 					refreshes.add(project);
				} else if (hasVisibleChanges(event.getTree(), project)){
 					additions.add(project);
 				}
 			} else if (existingProjects.contains(project)) {
 				removals.add(project);
 				
 			}
 		}
 		if (!removals.isEmpty() || !additions.isEmpty() || !refreshes.isEmpty()) {
 			TreeViewer viewer = (TreeViewer)getViewer();
 			Tree tree = viewer.getTree();
 			try {
 				tree.setRedraw(false);
 				if (!additions.isEmpty())
 					viewer.add(viewer.getInput(), additions.toArray());
 				if (!removals.isEmpty())
 					viewer.remove(viewer.getInput(), removals.toArray());
 				if (!refreshes.isEmpty())
 					viewer.remove(refreshes.toArray());
 			} finally {
 				tree.setRedraw(true);
 			}
 		}
 	}
 
	private boolean hasVisibleChanges(IDiffTree tree, IProject project) {
		return tree.hasMatchingDiffs(project.getFullPath(), new FastDiffFilter() {
			public boolean select(IDiff diff) {
				return isVisible(diff);
			}
		});
	}

 	private boolean hasDiffs(IDiffTree tree, IProject project) {
 		return tree.getChildren(project.getFullPath()).length > 0;
 	}
 
 	private IProject[] getChangedProjects(IDiffChangeEvent event) {
 		Set result = new HashSet();
 		IDiff[] changes = event.getChanges();
 		for (int i = 0; i < changes.length; i++) {
 			IDiff diff = changes[i];
 			IResource resource = ResourceDiffTree.getResourceFor(diff);
 			if (resource != null) {
 				result.add(resource.getProject());
 			}
 		}
 		IDiff[] additions = event.getAdditions();
 		for (int i = 0; i < additions.length; i++) {
 			IDiff diff = additions[i];
 			IResource resource = ResourceDiffTree.getResourceFor(diff);
 			if (resource != null) {
 				result.add(resource.getProject());
 			}
 		}
 		IPath[] removals = event.getRemovals();
 		for (int i = 0; i < removals.length; i++) {
 			IPath path = removals[i];
 			if (path.segmentCount() > 0) {
 				IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(path.segment(0));
 				result.add(project);
 			}
 		}
 		return (IProject[]) result.toArray(new IProject[result.size()]);
 	}
 
 	private Set getVisibleProjects() {
 		TreeViewer viewer = (TreeViewer)getViewer();
 		Tree tree = viewer.getTree();
 		Control[] children = tree.getChildren();
 		Set result = new HashSet();
 		for (int i = 0; i < children.length; i++) {
 			Control control = children[i];
 			Object data = control.getData();
 			IResource resource = Utils.getResource(data);
 			if (resource.getType() == IResource.PROJECT) {
 				result.add(resource);
 			}
 		}
 		return result;
 	}
 }
