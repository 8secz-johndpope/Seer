 /*******************************************************************************
  * Copyright (c) 2000, 2003 IBM Corporation and others.
  * All rights reserved. This program and the accompanying materials 
  * are made available under the terms of the Common Public License v1.0
  * which accompanies this distribution, and is available at
  * http://www.eclipse.org/legal/cpl-v10.html
  * 
  * Contributors:
  *     IBM Corporation - initial API and implementation
  *******************************************************************************/
 package org.eclipse.team.internal.ccvs.core.resources;
 
 import java.util.HashMap;
 import java.util.Iterator;
 import java.util.Map;
 
 import org.eclipse.core.resources.IContainer;
 import org.eclipse.core.resources.IFolder;
 import org.eclipse.core.resources.IProject;
 import org.eclipse.core.resources.IResource;
 import org.eclipse.core.resources.ISynchronizer;
 import org.eclipse.core.resources.ResourcesPlugin;
 import org.eclipse.core.runtime.CoreException;
 import org.eclipse.core.runtime.IPath;
 import org.eclipse.team.internal.ccvs.core.CVSException;
 import org.eclipse.team.internal.ccvs.core.ICVSFolder;
 import org.eclipse.team.internal.ccvs.core.ICVSResource;
 import org.eclipse.team.internal.ccvs.core.syncinfo.FolderSyncInfo;
 import org.eclipse.team.internal.ccvs.core.syncinfo.ResourceSyncInfo;
 import org.eclipse.team.internal.ccvs.core.util.Util;
 
 /**
  * This cache uses session properties to hold the bytes representing the sync
  * info
  */
 /*package*/ class SynchronizerSyncInfoCache extends SyncInfoCache {
 	
 	// ap of sync bytes that were set without a scheduling rule
 	Map pendingCacheWrites = new HashMap();
 	private static final Object BYTES_REMOVED = new byte[0];
 
 	public SynchronizerSyncInfoCache() {
 		getWorkspaceSynchronizer().add(FOLDER_SYNC_KEY);
 		getWorkspaceSynchronizer().add(RESOURCE_SYNC_KEY);
 	}
 	/**
 	 * Return the Eclipse Workspace Synchronizer (from org.eclipse.core.resources)
 	 */
 	private ISynchronizer getWorkspaceSynchronizer() {
 		return ResourcesPlugin.getWorkspace().getSynchronizer();
 	}
 	
 	/*package*/ void flush(IProject project) throws CVSException {
 		purgeCache(project, true);
 	}
 	
 	/**
 	 * Method flush.
 	 * @param folder
 	 */
 	/*package*/ void flush(IFolder folder) throws CVSException {
 		purgeCache(folder, false);
 	}
 	
 	/**
 	 * Returns the folder sync info for the container; null if none.
 	 * Folder must exist and must not be the workspace root.
 	 * The folder sync info for the container MUST ALREADY BE CACHED.
 	 *
 	 * @param container the container
 	 * @return the folder sync info for the folder, or null if none.
 	 * @see #cacheFolderSync
 	 */
 	FolderSyncInfo getCachedFolderSync(IContainer container) throws CVSException {
 		try {
 			byte[] bytes = getWorkspaceSynchronizer().getSyncInfo(FOLDER_SYNC_KEY, container);
 			if (bytes == null) return null;
 			return FolderSyncInfo.getFolderSyncInfo(bytes);
 		} catch (CoreException e) {
 			throw CVSException.wrapException(e);
 		}
 	}
 	
 	/**
 	 * Sets the folder sync info for the container; if null, deletes it.
 	 * Folder must exist and must not be the workspace root.
 	 * The folder sync info for the container need not have previously been
 	 * cached.
 	 *
 	 * @param container the container
 	 * @param info the new folder sync info
 	 */
 	void setCachedFolderSync(IContainer container, FolderSyncInfo info, boolean canModifyWorkspace) throws CVSException {
 		try {
 			if (info == null) {
 				if (container.exists() || container.isPhantom()) {
 					getWorkspaceSynchronizer().flushSyncInfo(FOLDER_SYNC_KEY, container, IResource.DEPTH_ZERO);
 				}
 			} else {
 				getWorkspaceSynchronizer().setSyncInfo(FOLDER_SYNC_KEY, container, info.getBytes());
 			}
 		} catch (CoreException e) {
 			throw CVSException.wrapException(e);
 		}
 	}
 
 	/**
 	 * @see org.eclipse.team.internal.ccvs.core.resources.SyncInfoCache#getCachedSyncBytes(org.eclipse.core.resources.IResource)
 	 */
 	byte[] getCachedSyncBytes(IResource resource) throws CVSException {
 		try {
 			byte[] bytes;
 			if (pendingCacheWrites.containsKey(resource)) {
 				bytes = (byte[])pendingCacheWrites.get(resource);
 				if (bytes == BYTES_REMOVED) {
 					bytes = null;
 				}
 			} else {
 				bytes = getWorkspaceSynchronizer().getSyncInfo(RESOURCE_SYNC_KEY, resource);
 			}
 			if (bytes != null && resource.getType() == IResource.FILE) {
 				if (ResourceSyncInfo.isAddition(bytes)) {
 					// The local file has been deleted but was an addition
 					// Therefore, ignoe the sync bytes
 					bytes = null;
 				} else if (!ResourceSyncInfo.isDeletion(bytes)) {
 					// Ensure the bytes indicate an outgoing deletion
 					bytes = ResourceSyncInfo.convertToDeletion(bytes);
 				}
 			}
 			return bytes;
 		} catch (CoreException e) {
 			throw CVSException.wrapException(e);
 		}
 	}
 	
 	/**
 	 * @see org.eclipse.team.internal.ccvs.core.resources.SyncInfoCache#setCachedSyncBytes(org.eclipse.core.resources.IResource, byte[])
 	 */
 	void setCachedSyncBytes(IResource resource, byte[] syncBytes, boolean canModifyWorkspace) throws CVSException {
 		byte[] oldBytes = getCachedSyncBytes(resource);
 		try {
 			if (syncBytes == null) {
 				if (oldBytes != null && (resource.exists() || resource.isPhantom())) {
 					if (canModifyWorkspace) {
 						getWorkspaceSynchronizer().flushSyncInfo(RESOURCE_SYNC_KEY, resource, IResource.DEPTH_ZERO);
 						pendingCacheWrites.remove(resource);
 					} else {
 						pendingCacheWrites.put(resource, BYTES_REMOVED);
 					}
 				}
 			} else {
 				// ensure that the sync info is not already set to the same thing.
 				// We do this to avoid causing a resource delta when the sync info is 
 				// initially loaded (i.e. the synchronizer has it and so does the Entries file
 				// Ignore the 
				if (oldBytes == null 
						|| !Util.equals(
								ResourceSyncInfo.convertToDeletion(syncBytes), 
								ResourceSyncInfo.convertToDeletion(oldBytes))) {
 					if (canModifyWorkspace) {
 						getWorkspaceSynchronizer().setSyncInfo(RESOURCE_SYNC_KEY, resource, syncBytes);
 						pendingCacheWrites.remove(resource);
 					} else {
 						pendingCacheWrites.put(resource, syncBytes);
 					}
 				}
 			}
 		} catch (CoreException e) {
 			throw CVSException.wrapException(e);
 		}
 	}
 
 	/* (non-Javadoc)
 	 * @see org.eclipse.team.internal.ccvs.core.resources.SyncInfoCache#getDirtyIndicator(org.eclipse.core.resources.IResource)
 	 */
 	String getDirtyIndicator(IResource resource) throws CVSException {		
 		if (resource.getType() == IResource.FILE) {
 			// a phantom file is dirty if it was managed before it was deleted			 
 			return getCachedSyncBytes(resource) != null ? 
 							IS_DIRTY_INDICATOR : 
 							NOT_DIRTY_INDICATOR;
 		} else {
 			return calculateDirtyCountForPhantomFolder((IContainer)resource);
 		}
 	}
 
 	/* (non-Javadoc)
 	 * @see org.eclipse.team.internal.ccvs.core.resources.SyncInfoCache#setDirtyIndicator(org.eclipse.core.resources.IResource, java.lang.String)
 	 */
 	void setDirtyIndicator(IResource resource, String indicator) throws CVSException {
 		// We don't cache the dirty count for folders because it would cause
 		// resource delta's in the decorator thread and possible deadlock.
 	}
 		
 	/*package*/ void flushDirtyCache(IResource container) throws CVSException {
 		// Dirty state is not cached
 	}
 	
 	/*package*/ boolean isSyncInfoLoaded(IContainer parent) throws CVSException {
 		return true;
 	}
 	
 	/**
 	 * @see org.eclipse.team.internal.ccvs.core.resources.SyncInfoCache#isResourceSyncInfoCached(org.eclipse.core.resources.IContainer)
 	 */
 	boolean isResourceSyncInfoCached(IContainer container) throws CVSException {
 		// the sync info is always cahced when using the synchronizer
 		return true;
 	}
 	
 	/**
 	 * @see org.eclipse.team.internal.ccvs.core.resources.SyncInfoCache#setResourceSyncInfoCached(org.eclipse.core.resources.IContainer)
 	 */
 	void setResourceSyncInfoCached(IContainer container) throws CVSException {
 		// do nothing
 	}
 	/**
 	 * @see org.eclipse.team.internal.ccvs.core.resources.SyncInfoCache#isFolderSyncInfoCached(org.eclipse.core.resources.IContainer)
 	 */
 	boolean isFolderSyncInfoCached(IContainer container) throws CVSException {
 		return true;
 	}
 	
 	/* (non-Javadoc)
 	 * @see org.eclipse.team.internal.ccvs.core.resources.SyncInfoCache#isDirtyCacheFlushed(org.eclipse.core.resources.IContainer)
 	 */
 	boolean isDirtyCacheFlushed(IContainer resource) throws CVSException {
 		return false;
 	}
 	
 	/*
 	 * Calculate the dirty count for the given phantom folder, performing any
 	 * necessary calculations on the childen as well
 	 */
 	private String calculateDirtyCountForPhantomFolder(IContainer parent) throws CVSException {
 		ICVSFolder cvsFolder = CVSWorkspaceRoot.getCVSFolderFor(parent);
 		if(getCachedFolderSync(parent) == null) {
 			return NOT_DIRTY_INDICATOR;
 		}
 		
 		String indicator = NOT_DIRTY_INDICATOR;
 		ICVSResource[] children = cvsFolder.members(ICVSFolder.MANAGED_MEMBERS | ICVSFolder.PHANTOM_MEMBERS);
 		for (int i = 0; i < children.length; i++) {
 			ICVSResource resource = children[i];
 			// keep looking into phantom folders until a managed phantom file 
 			// is found.
 			if (resource.isFolder()) {
 				indicator = calculateDirtyCountForPhantomFolder((IContainer)resource.getIResource());
 			} else {
 				// Any non-existant managed files are dirty (outgoing deletion)
 				indicator = IS_DIRTY_INDICATOR;
 				break;
 			}
 		}
 		return indicator;
 	}
 	
 	/**
 	 * @param root
 	 * @param deep
 	 */
 	public void purgeCache(IContainer root, boolean deep) throws CVSException {
 		int depth = deep ? IResource.DEPTH_INFINITE : IResource.DEPTH_ZERO;
 		try {
 			if (root.exists() || root.isPhantom()) {
 				getWorkspaceSynchronizer().flushSyncInfo(RESOURCE_SYNC_KEY, root, depth);
 			}
 			if (root.exists() || root.isPhantom()) {
 				getWorkspaceSynchronizer().flushSyncInfo(FOLDER_SYNC_KEY, root, depth);
 			}
 			if (deep) {
 				IPath fullPath = root.getFullPath();
 				for (Iterator iter = pendingCacheWrites.keySet().iterator(); iter.hasNext();) {
 					IResource resource = (IResource) iter.next();
 					if (fullPath.isPrefixOf(resource.getFullPath())) {
 						iter.remove();
 					}
 				}
 			} else {
 				pendingCacheWrites.remove(root);
 			}
 		} catch (CoreException e) {
 			throw CVSException.wrapException(e);
 		}
 		
 	}
 }
