 /*******************************************************************************
  * Copyright (c) 2002 IBM Corporation and others.
  * All rights reserved.   This program and the accompanying materials
  * are made available under the terms of the Common Public License v0.5
  * which accompanies this distribution, and is available at
  * http://www.eclipse.org/legal/cpl-v05.html
  * 
  * Contributors:
  * IBM - Initial implementation
  ******************************************************************************/
 package org.eclipse.team.internal.ccvs.ui.actions;
 
 import java.lang.reflect.InvocationTargetException;
 import java.util.ArrayList;
 import java.util.Arrays;
 import java.util.Collections;
 import java.util.Comparator;
 import java.util.HashSet;
 import java.util.Hashtable;
 import java.util.Iterator;
 import java.util.List;
 import java.util.Set;
 
 import org.eclipse.core.resources.IProject;
 import org.eclipse.core.resources.IResource;
 import org.eclipse.core.resources.IResourceStatus;
 import org.eclipse.core.runtime.CoreException;
 import org.eclipse.core.runtime.IPath;
 import org.eclipse.core.runtime.IProgressMonitor;
 import org.eclipse.core.runtime.IStatus;
 import org.eclipse.core.runtime.SubProgressMonitor;
 import org.eclipse.jface.action.IAction;
 import org.eclipse.jface.dialogs.MessageDialog;
 import org.eclipse.swt.widgets.Display;
 import org.eclipse.swt.widgets.Shell;
 import org.eclipse.team.core.RepositoryProvider;
 import org.eclipse.team.core.TeamException;
 import org.eclipse.team.internal.ccvs.core.CVSException;
 import org.eclipse.team.internal.ccvs.core.CVSProviderPlugin;
 import org.eclipse.team.internal.ccvs.core.CVSTag;
 import org.eclipse.team.internal.ccvs.core.CVSTeamProvider;
 import org.eclipse.team.internal.ccvs.core.ICVSFolder;
 import org.eclipse.team.internal.ccvs.core.ICVSResource;
 import org.eclipse.team.internal.ccvs.core.resources.CVSWorkspaceRoot;
 import org.eclipse.team.internal.ccvs.core.resources.EclipseSynchronizer;
 import org.eclipse.team.internal.ccvs.core.syncinfo.FolderSyncInfo;
 import org.eclipse.team.internal.ccvs.core.syncinfo.ResourceSyncInfo;
 import org.eclipse.team.internal.ccvs.ui.Policy;
import org.eclipse.team.internal.ui.IPromptCondition;
 import org.eclipse.team.internal.ui.PromptingDialog;
 
 /**
  * This class represents an action performed on a local CVS workspace
  */
 public abstract class WorkspaceAction extends CVSAction {
 
 	public interface IProviderAction {
 		public IStatus execute(CVSTeamProvider provider, IResource[] resources, IProgressMonitor monitor) throws CVSException;
 	}
 	
 	/**
 	 * @see org.eclipse.team.internal.ccvs.ui.actions.CVSAction#beginExecution(IAction)
 	 */
 	protected boolean beginExecution(IAction action) throws TeamException {
 		if (super.beginExecution(action)) {
 			// Ensure that the required sync info is loaded
 			if (requiresLocalSyncInfo()) {
 				// Check enablement just in case the sync info wasn't loaded
 				if (!isEnabled()) {
 					MessageDialog.openInformation(getShell(), Policy.bind("CVSAction.disabledTitle"), Policy.bind("CVSAction.disabledMessage")); //$NON-NLS-1$ //$NON-NLS-2$
 					return false;
 				}
 			}
 			return true;
 		} else {
 			return false;
 		}
 	}
 
 	/**
 	 * Return true if the sync info is loaded for all selected resources.
 	 * The purpose of this method is to allow enablement code to be as fast
 	 * as possible. If the sync info is not loaded, the menu should be enabled
 	 * and, if choosen, the action will verify that it is indeed enabled before
 	 * performing the associated operation
 	 */
 	protected boolean isSyncInfoLoaded(IResource[] resources) throws CVSException {
 		return EclipseSynchronizer.getInstance().isSyncInfoLoaded(resources, getEnablementDepth());
 	}
 
 	/**
 	 * Returns the resource depth of the action for use in determining if the required
 	 * sync info is loaded. The default is IResource.DEPTH_INFINITE. Sunclasses can override
 	 * as required.
 	 */
 	protected int getActionDepth() {
 		return IResource.DEPTH_INFINITE;
 	}
 
 	/**
 	 * Returns the resource depth of the action enablement for use in determining if the required
 	 * sync info is loaded. The default is IResource.DEPTH_ZERO. Sunclasses can override
 	 * as required.
 	 */
 	protected int getEnablementDepth() {
 		return IResource.DEPTH_ZERO;
 	}
 	
 	/**
 	 * Ensure that the sync info for all the provided resources has been loaded.
 	 * If an out-of-sync resource is found, prompt to refresh all the projects involved.
 	 */
 	protected boolean ensureSyncInfoLoaded(IResource[] resources) throws CVSException {
 		boolean keepTrying = true;
 		while (keepTrying) {
 			try {
 				EclipseSynchronizer.getInstance().ensureSyncInfoLoaded(resources, getActionDepth());
 				keepTrying = false;
 			} catch (CVSException e) {
 				if (e.getStatus().getCode() == IResourceStatus.OUT_OF_SYNC_LOCAL) {
 					// determine the projects of the resources involved
 					Set projects = new HashSet();
 					for (int i = 0; i < resources.length; i++) {
 						IResource resource = resources[i];
 						projects.add(resource.getProject());
 					}
 					// prompt to refresh
 					if (promptToRefresh(getShell(), (IResource[]) projects.toArray(new IResource[projects.size()]), e.getStatus())) {
 						for (Iterator iter = projects.iterator();iter.hasNext();) {
 							IProject project = (IProject) iter.next();
 							try {
 								project.refreshLocal(IResource.DEPTH_INFINITE, null);
 							} catch (CoreException coreException) {
 								throw CVSException.wrapException(coreException);
 							}
 						}
 					} else {
 						return false;
 					}
 				} else {
 					throw e;
 				}
 			}
 		}
 		return true;
 	}
 	
 	/**
 	 * Override to ensure that the sync info is available before performing the
 	 * real <code>isEnabled()</code> test.
 	 * 
 	 * @see org.eclipse.team.internal.ui.actions.TeamAction#setActionEnablement(IAction)
 	 */
 	protected void setActionEnablement(IAction action) {
 		try {
 			boolean requires = requiresLocalSyncInfo();
 			if (!requires || (requires && isSyncInfoLoaded(getSelectedResources()))) {
 				super.setActionEnablement(action);
 			} else {
 				// If the sync info is not loaded, enable the menu item
 				// Performing the action will ensure that the action should really
 				// be enabled before anything else is done
 				action.setEnabled(true);
 			}
 		} catch (CVSException e) {
 			// We couldn't determine if the sync info was loaded.
 			// Enable the action so that performing the action will
 			// reveal the error to the user.
 			action.setEnabled(true);
 		}
 	}
 
 	/**
 	 * Return true if the action requires the sync info for the selected resources.
 	 * If the sync info is required, the real enablement code will only be run if
 	 * the sync info is loaded from disc. Otherwise, the action is enabled and
 	 * performing the action will load the sync info and verify that the action is truely
 	 * enabled before doing anything else.
 	 * 
 	 * This implementation returns <code>true</code>. Subclasses must override if they do
 	 * not require the sync info of the selected resources.
 	 * 
 	 * @return boolean
 	 */
 	protected boolean requiresLocalSyncInfo() {
 		return true;
 	}
 
 	protected boolean promptToRefresh(final Shell shell, final IResource[] resources, final IStatus status) {
 		final boolean[] result = new boolean[] { false};
 		Runnable runnable = new Runnable() {
 			public void run() {
 				Shell shellToUse = shell;
 				if (shell == null) {
 					shellToUse = new Shell(Display.getCurrent());
 				}
 				String question;
 				if (resources.length == 1) {
 					question = Policy.bind("CVSAction.refreshQuestion", status.getMessage(), resources[0].getFullPath().toString()); //$NON-NLS-1$
 				} else {
 					question = Policy.bind("CVSAction.refreshMultipleQuestion", status.getMessage()); //$NON-NLS-1$
 				}
 				result[0] = MessageDialog.openQuestion(shellToUse, Policy.bind("CVSAction.refreshTitle"), question); //$NON-NLS-1$
 			}
 		};
 		Display.getDefault().syncExec(runnable);
 		return result[0];
 	}
 
 	/**
 	 * Most CVS workspace actions modify the workspace and thus should
 	 * save dirty editors.
 	 * @see org.eclipse.team.internal.ccvs.ui.actions.CVSAction#needsToSaveDirtyEditors()
 	 */
 	protected boolean needsToSaveDirtyEditors() {
 		return true;
 	}
 
 	/**
 	 * The action is enabled for the appropriate resources. This method checks
 	 * that:
 	 * <ol>
 	 * <li>there is no overlap between a selected file and folder (overlapping
 	 * folders is allowed because of logical vs. physical mapping problem in
 	 * views)
 	 * <li>the state of the resources match the conditions provided by:
 	 * <ul>
 	 * <li>isEnabledForIgnoredResources()
 	 * <li>isEnabledForManagedResources()
 	 * <li>isEnabledForUnManagedResources() (i.e. not ignored and not managed)
 	 * </ul>
 	 * </ol>
 	 * @see TeamAction#isEnabled()
 	 */
 	protected boolean isEnabled() throws TeamException {
 		
 		// invoke the inherited method so that overlaps are maintained
 		IResource[] resources = super.getSelectedResources();
 		
 		// disable if no resources are selected
 		if(resources.length==0) return false;
 		
 		// disable properly for single resource enablement
 		if (!isEnabledForMultipleResources() && resources.length != 1) return false;
 		
 		// validate enabled for each resource in the selection
 		List folderPaths = new ArrayList();
 		List filePaths = new ArrayList();
 		for (int i = 0; i < resources.length; i++) {
 			IResource resource = resources[i];
 			
 			// only enable for accessible resources
 			if (! resource.isAccessible()) return false;
 			
 			// no CVS actions are enabled if the selection contains a linked resource
 			if (CVSWorkspaceRoot.isLinkedResource(resource)) return false;
 			
 			// only enable for resources in a project shared with CVS
 			if(RepositoryProvider.getProvider(resource.getProject(), CVSProviderPlugin.getTypeId()) == null) {
 				return false;
 			}
 			
 			// collect files and folders separately to check for overlap later	
 			IPath resourceFullPath = resource.getFullPath();
 			if(resource.getType() == IResource.FILE) {
 				filePaths.add(resourceFullPath);
 			} else {
 				folderPaths.add(resourceFullPath);
 			}
 			
 			// ensure that resource management state matches what the action requires
 			ICVSResource cvsResource = CVSWorkspaceRoot.getCVSResourceFor(resource);
 			if (!isEnabledForCVSResource(cvsResource)) {
 				return false;
 			}
 		}
 		// Ensure that there is no overlap between files and folders
 		// NOTE: folder overlap must be allowed because of logical vs. physical
 		if(!folderPaths.isEmpty()) {
 			for (Iterator fileIter = filePaths.iterator(); fileIter.hasNext();) {
 				IPath resourcePath = (IPath) fileIter.next();
 				for (Iterator it = folderPaths.iterator(); it.hasNext();) {
 					IPath folderPath = (IPath) it.next();
 					if (folderPath.isPrefixOf(resourcePath)) {
 						return false;
 					}
 				}
 			}
 		}
 		return true;
 	}
 	
 	/**
 	 * Method isEnabledForCVSResource.
 	 * @param cvsResource
 	 * @return boolean
 	 */
 	protected boolean isEnabledForCVSResource(ICVSResource cvsResource) throws CVSException {
 		boolean managed = false;
 		boolean ignored = false;
 		boolean added = false;
 		if (cvsResource.isIgnored()) {
 			ignored = true;
 		} else if (cvsResource.isFolder()) {
 			managed = ((ICVSFolder)cvsResource).isCVSFolder();
 		} else {
 			ResourceSyncInfo info = cvsResource.getSyncInfo();
 			managed = info != null;
 			if (managed) added = info.isAdded();
 		}
 		if (managed && ! isEnabledForManagedResources()) return false;
 		if ( ! managed && ! isEnabledForUnmanagedResources()) return false;
 		if ( ignored && ! isEnabledForIgnoredResources()) return false;
 		if (added && ! isEnabledForAddedResources()) return false;
 		return true;
 	}
 	
 	/**
 	 * Method isEnabledForIgnoredResources.
 	 * @return boolean
 	 */
 	protected boolean isEnabledForIgnoredResources() {
 		return false;
 	}
 	
 	/**
 	 * Method isEnabledForUnmanagedResources.
 	 * @return boolean
 	 */
 	protected boolean isEnabledForUnmanagedResources() {
 		return false;
 	}
 	
 	/**
 	 * Method isEnabledForManagedResources.
 	 * @return boolean
 	 */
 	protected boolean isEnabledForManagedResources() {
 		return true;
 	}
 
 	/**
 	 * Method isEnabledForAddedResources.
 	 * @return boolean
 	 */
 	protected boolean isEnabledForAddedResources() {
 		return true;
 	}
 	
 	/**
 	 * Method isEnabledForAddedResources.
 	 * @return boolean
 	 */
 	protected boolean isEnabledForMultipleResources() {
 		return true;
 	}
 	
 	/**
 	 * Method getNonOverlapping ensures that a resource is not covered more than once.
 	 * @param resources
 	 * @return IResource[]
 	 */
 	public static IResource[] getNonOverlapping(IResource[] resources) {
 		// Sort the resources so the shortest paths are first
 		List sorted = new ArrayList();
 		sorted.addAll(Arrays.asList(resources));
 		Collections.sort(sorted, new Comparator() {
 			public int compare(Object arg0, Object arg1) {
 				IResource resource0 = (IResource) arg0;
 				IResource resource1 = (IResource) arg1;
 				return resource0.getFullPath().segmentCount() - resource1.getFullPath().segmentCount();
 			}
 			public boolean equals(Object arg0) {
 				return false;
 			}
 		});
 		// Collect all non-overlapping resources
 		List coveredPaths = new ArrayList();
 		for (Iterator iter = sorted.iterator(); iter.hasNext();) {
 			IResource resource = (IResource) iter.next();
 			IPath resourceFullPath = resource.getFullPath();
 			boolean covered = false;
 			for (Iterator it = coveredPaths.iterator(); it.hasNext();) {
 				IPath path = (IPath) it.next();
 				if(path.isPrefixOf(resourceFullPath)) {
 					covered = true;
 				}
 			}
 			if (covered) {
 				// if the resource is covered by a parent, remove it
 				iter.remove();
 			} else {
 				// if the resource is a non-covered folder, add it to the covered paths
 				if (resource.getType() == IResource.FOLDER) {
 					coveredPaths.add(resource.getFullPath());
 				}
 			}
 		}
 		return (IResource[]) sorted.toArray(new IResource[sorted.size()]);
 	}
 	
 	/**
 	 * Override to ensure that the selected resources so not overlap.
 	 * This method assumes that all actions are deep.
 	 * 
 	 * @see org.eclipse.team.internal.ui.actions.TeamAction#getSelectedResources()
 	 */
 	protected IResource[] getSelectedResources() {
 		return getNonOverlapping(super.getSelectedResources());
 	}
 
 	protected void executeProviderAction(IProviderAction action, IResource[] resources, IProgressMonitor monitor) throws InvocationTargetException {
 		Hashtable table = getProviderMapping(resources);
 		Set keySet = table.keySet();
 		monitor.beginTask(null, keySet.size() * 1000);
 		Iterator iterator = keySet.iterator();
 
 		while (iterator.hasNext()) {
 			IProgressMonitor subMonitor = new SubProgressMonitor(monitor, 1000);
 			CVSTeamProvider provider = (CVSTeamProvider)iterator.next();
 			List list = (List)table.get(provider);
 			IResource[] providerResources = (IResource[])list.toArray(new IResource[list.size()]);
 			try {
 				addStatus(action.execute(provider, providerResources, subMonitor));
 			} catch (CVSException e) {
 				throw new InvocationTargetException(e);
 			}
 
 		}
 	}
 	
 	protected void executeProviderAction(IProviderAction action, IProgressMonitor monitor) throws InvocationTargetException {
 		executeProviderAction(action, getSelectedResources(), monitor);
 	}
 
 	/**
 	 * Given the current selection this method returns a text label that can
 	 * be shown to the user that reflects the tags in the current selection.
 	 * These can be used in the <b>Compare With</b> and <b>Replace With</b> actions.
 	 */
 	protected String calculateActionTagValue() {
 		try {
 			IResource[] resources = getSelectedResources();
 			CVSTag commonTag = null;
 			boolean sameTagType = true; 
 			boolean multipleSameNames = true;
 			
 			for (int i = 0; i < resources.length; i++) {
 				ICVSResource cvsResource = CVSWorkspaceRoot.getCVSResourceFor(resources[i]);
 				CVSTag tag = null;
 				if(cvsResource.isFolder()) {
 					FolderSyncInfo info = ((ICVSFolder)cvsResource).getFolderSyncInfo();
 					if(info != null) {
 						tag = info.getTag();									
 					}
 				} else {
 					ResourceSyncInfo info = cvsResource.getSyncInfo();
 					if(info != null) {
 						tag = info.getTag();
 					}
 					// This magic is required because of a bug in CVS which doesn't store the
 					// type of tag for files correctly in the Entries file. They will always appear
 					// as branch tags "Tv1". By comparing the revision number to the tag name
 					// you can determine if the tag is a branch or version.
 					FolderSyncInfo parentInfo = cvsResource.getParent().getFolderSyncInfo();
 					CVSTag parentTag = null;
 					if(parentInfo != null) {
 						parentTag = parentInfo.getTag();
 					}
 					if(tag != null) {
 						if(tag.getName().equals(info.getRevision())) {
 							tag = new CVSTag(tag.getName(), CVSTag.VERSION);
 						} else if(parentTag != null){
 							tag = new CVSTag(tag.getName(), parentTag.getType());
 						}
 					} else {
 						// if a file doesn't have tag info, very possible for example
 						// when the file is in HEAD, use the parents.
 						tag = parentTag;
 					}
 				}
 				if(tag == null) {
 					tag = new CVSTag();
 				}
 				if(commonTag == null) {
 					commonTag = tag;
 				} else if(!commonTag.equals(tag)) {					
 					if(commonTag.getType() != tag.getType()) {
 						sameTagType = false;
 					}
 					if(!commonTag.getName().equals(tag.getName())) {
 						multipleSameNames = false;
 					}
 				}
 			}
 			
 			// set text to default
 			String actionText = Policy.bind("ReplaceWithLatestAction.multipleTags"); //$NON-NLS-1$
 			if(commonTag != null) {
 				int tagType = commonTag.getType();
 				String tagName = commonTag.getName();
 				// multiple tag names but of the same type
 				if(sameTagType && !multipleSameNames) {
 					if(tagType == CVSTag.BRANCH) {
 						actionText = Policy.bind("ReplaceWithLatestAction.multipleBranches"); //$NON-NLS-1$					
 					} else {
 						actionText = Policy.bind("ReplaceWithLatestAction.multipleVersions"); //$NON-NLS-1$
 					}
 				// same tag names and types
 				} else if(sameTagType && multipleSameNames) {
 					if(tagType == CVSTag.BRANCH) {
 						actionText = Policy.bind("ReplaceWithLatestAction.singleBranch", tagName); //$NON-NLS-1$					
 					} else if(tagType == CVSTag.VERSION){
 						actionText = Policy.bind("ReplaceWithLatestAction.singleVersion", tagName); //$NON-NLS-1$
 					} else if(tagType == CVSTag.HEAD) {
 						actionText = Policy.bind("ReplaceWithLatestAction.singleHEAD", tagName); //$NON-NLS-1$
 					}
 				}
 			}
 			
 			return actionText;
 		} catch (CVSException e) {
 			// silently ignore
 			return Policy.bind("ReplaceWithLatestAction.multipleTags"); //$NON-NLS-1$ 
 		}
 	}
 
 	protected IResource[] checkOverwriteOfDirtyResources(IResource[] resources, IProgressMonitor monitor) throws CVSException, InterruptedException {
 		List dirtyResources = new ArrayList();
 		IResource[] selectedResources = getSelectedResources();
 		
 		try {
 			monitor = Policy.monitorFor(monitor);
 			monitor.beginTask(null, selectedResources.length * 100);
 			monitor.setTaskName(Policy.bind("ReplaceWithAction.calculatingDirtyResources")); //$NON-NLS-1$
 			for (int i = 0; i < selectedResources.length; i++) {
 				IResource resource = selectedResources[i];
 				ICVSResource cvsResource = CVSWorkspaceRoot.getCVSResourceFor(resource);
 				if(cvsResource.isModified(Policy.subMonitorFor(monitor, 100))) {
 					dirtyResources.add(resource);
 				}			
 			}
 		} finally {
 			monitor.done();		
 		}
 		
 		PromptingDialog dialog = new PromptingDialog(getShell(), selectedResources, 
				getPromptCondition((IResource[]) dirtyResources.toArray(new IResource[dirtyResources.size()])), Policy.bind("ReplaceWithAction.confirmOverwrite"));//$NON-NLS-1$
 		return dialog.promptForMultiple();
 	}

	/**
	 * This is a helper for the CVS UI automated tests. It allows the tests to ignore prompting dialogs.
	 * @param resources
	 */
	protected IPromptCondition getPromptCondition(IResource[] resources) {
		return getOverwriteLocalChangesPrompt(resources);
	}
 }
