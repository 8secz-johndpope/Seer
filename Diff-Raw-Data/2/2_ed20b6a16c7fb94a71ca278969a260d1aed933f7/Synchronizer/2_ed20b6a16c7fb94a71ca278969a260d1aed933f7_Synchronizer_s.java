 package org.eclipse.core.internal.resources;
 
 /*
  * (c) Copyright IBM Corp. 2000, 2001.
  * All Rights Reserved.
  */
 
 import org.eclipse.core.resources.*;
 import org.eclipse.core.runtime.*;
 import org.eclipse.core.internal.localstore.*;
 import org.eclipse.core.internal.utils.*;
 //
 import java.io.*;
 import java.util.*;
 //
 public class Synchronizer implements ISynchronizer {
 	protected Workspace workspace;
 	protected SyncInfoWriter writer;
 	
 	// Registry of sync partners. Set of qualified names.
 	protected Set registry = new HashSet(5);
 
 	// version number used for serialization
 	protected static int VERSION = 2;
 
 	//
 	protected static final int INT_CONSTANT = 1;
 	protected static final int QNAME_CONSTANT = 2;
 public Synchronizer(Workspace workspace) {
 	super();
 	this.workspace = workspace;
 	this.writer = new SyncInfoWriter(workspace, this);
 }
 /**
  * @see ISynchronizer#accept
  */
 public void accept(QualifiedName partner, IResource resource, IResourceVisitor visitor, int depth) throws CoreException {
 	Assert.isLegal(partner != null);
 	Assert.isLegal(resource != null);
 	Assert.isLegal(visitor != null);
 
 	// if we don't have sync info for the given identifier, then skip it
 	if (getSyncInfo(partner, resource) != null) {
 		// visit the resource and if the visitor says to stop the recursion then return
 		if (!visitor.visit(resource))
 			return;
 	}
 
 	// adjust depth if necessary
 	if (depth == IResource.DEPTH_ZERO || resource.getType() == IResource.FILE)
 		return;
 	if (depth == IResource.DEPTH_ONE)
 		depth = IResource.DEPTH_ZERO;
 
 	// otherwise recurse over the children
 	IResource[] children = ((IContainer) resource).members();
 	for (int i = 0; i < children.length; i++)
 		accept(partner, children[i], visitor, depth);
 }
 /**
  * @see ISynchronizer#add
  */
 public void add(QualifiedName partner) {
 	Assert.isLegal(partner != null);
 	registry.add(partner);
 }
 /**
  * @see ISynchronizer#flushSyncInfo
  */
 public void flushSyncInfo(final QualifiedName partner, final IResource root, final int depth) throws CoreException {
 	Assert.isLegal(partner != null);
 	Assert.isLegal(root != null);
 
 	IWorkspaceRunnable body = new IWorkspaceRunnable() {
 		public void run(IProgressMonitor monitor) throws CoreException {
 			IResourceVisitor visitor = new IResourceVisitor() {
 				public boolean visit(IResource resource) throws CoreException {
 					setSyncInfo(partner, resource, null);
 					return true;
 				}
 			};
 			root.accept(visitor, depth, true);
 		}
 	};
 	workspace.run(body, null);
 }
 /**
  * @see ISynchronizer#getPartners
  */
 public QualifiedName[] getPartners() {
 	return (QualifiedName[]) registry.toArray(new QualifiedName[registry.size()]);
 }
 /**
  * For use by the serialization code.
  */
 protected Set getRegistry() {
 	return registry;
 }
 /**
  * @see ISynchronizer#getSyncInfo
  */
 public byte[] getSyncInfo(QualifiedName partner, IResource resource) throws CoreException {
 	Assert.isLegal(partner != null);
 	Assert.isLegal(resource != null);
 
 	if (!isRegistered(partner))
 		throw new ResourceException(new ResourceStatus(IResourceStatus.PARTNER_NOT_REGISTERED, partner.toString()));
 
 	// namespace check, if the resource doesn't exist then return null
 	ResourceInfo info = workspace.getResourceInfo(resource.getFullPath(), true, false);
 	return (info == null) ? null : info.getSyncInfo(partner, true);
 }
 protected boolean isRegistered(QualifiedName partner) {
 	Assert.isLegal(partner != null);
 	return registry.contains(partner);
 }
 /**
  * @see #writePartners
  */
 public void readPartners(DataInputStream input) throws CoreException {
 	SyncInfoReader reader = new SyncInfoReader(workspace, this);
 	reader.readPartners(input);
 }
 public void restore(IResource resource, IProgressMonitor monitor) throws CoreException {
 	// first restore from the last save and then apply any snapshots
 	restoreFromSave(resource);
 	restoreFromSnap(resource);
 }
 protected void restoreFromSave(IResource resource) throws CoreException {
 	IPath sourceLocation = workspace.getMetaArea().getSyncInfoLocationFor(resource);
 	IPath tempLocation = workspace.getMetaArea().getBackupLocationFor(sourceLocation);
 	try {
 		DataInputStream input = new DataInputStream(new SafeFileInputStream(sourceLocation.toOSString(), tempLocation.toOSString()));
 		try {
 			SyncInfoReader reader = new SyncInfoReader(workspace, this);
 			reader.readSyncInfo(input);
 		} finally {
 			input.close();
 		}
 	} catch (FileNotFoundException e) {
 		// ignore if no sync info saved
 	} catch (IOException e) {
 		String msg = Policy.bind("resources.readMeta", sourceLocation.toString());
 		throw new ResourceException(IResourceStatus.FAILED_READ_METADATA, sourceLocation, msg, e);
 	}
 }
 protected void restoreFromSnap(IResource resource) {
 	IPath sourceLocation = workspace.getMetaArea().getSyncInfoSnapshotLocationFor(resource);
 	try {
 		DataInputStream input = new DataInputStream(new SafeChunkyInputStream(sourceLocation.toOSString()));
 		try {
 			SyncInfoSnapReader reader = new SyncInfoSnapReader(workspace, this);
 			while (true)
 				reader.readSyncInfo(input);
 		} catch (EOFException eof) {
 			// ignore end of file
 		} finally {
 			input.close();
 		}
 	} catch (FileNotFoundException e) {
 		// ignore if no sync info saved.
 	} catch (Exception e) {
 		// only log the exception, we should not fail restoring the snapshot
 		String msg = Policy.bind("resources.readMeta", sourceLocation.toString());
 		ResourcesPlugin.getPlugin().getLog().log(new ResourceStatus(IResourceStatus.FAILED_READ_METADATA, sourceLocation, msg, e));
 	}
 }
 /**
  * @see ISynchronizer#remove
  */
 public void remove(QualifiedName partner) {
 	Assert.isLegal(partner != null);
 	if (isRegistered(partner)) {
 		// remove all sync info for this partner
 		try {
 			flushSyncInfo(partner, workspace.getRoot(), IResource.DEPTH_INFINITE);
 			registry.remove(partner);
 		} catch (CoreException e) {
 			// XXX: flush needs to be more resilient and not throw exceptions all the time
 		}
 	}
 }
 public void savePartners(DataOutputStream output) throws IOException {
 	writer.savePartners(output);
 }
 public void saveSyncInfo(IResource resource, DataOutputStream output, List writtenPartners) throws IOException {
 	writer.saveSyncInfo(resource, output, writtenPartners);
 }
 protected void setRegistry(Set registry) {
 	this.registry = registry;
 }
 /**
  * @see ISynchronizer#setSyncInfo
  */
 public void setSyncInfo(QualifiedName partner, IResource resource, byte[] info) throws CoreException {
 	Assert.isLegal(partner != null);
 	Assert.isLegal(resource != null);
 	try {
 		workspace.prepareOperation();
 		workspace.beginOperation(true);
 		if (!isRegistered(partner))
 			throw new ResourceException(new ResourceStatus(IResourceStatus.PARTNER_NOT_REGISTERED, partner.toString()));
 		// we do not store sync info on the workspace root
 		if (resource.getType() == IResource.ROOT)
 			return;
 		// if the resource doesn't yet exist then create a phantom so we can set the sync info on it
 		Resource target = (Resource) resource;
 		ResourceInfo resourceInfo = workspace.getResourceInfo(target.getFullPath(), true, false);
 		int flags = target.getFlags(resourceInfo);
 		if (!target.exists(flags, false)) {
 			if (info == null)
 				return;
 			else
 				workspace.createResource(resource, true);
 		}
 		resourceInfo = target.getResourceInfo(true, true);
 		resourceInfo.setSyncInfo(partner, info);
 		resourceInfo.incrementSyncInfoGenerationCount();
 		resourceInfo.set(ICoreConstants.M_SYNCINFO_SNAP_DIRTY);
 		flags = target.getFlags(resourceInfo);
 		if (target.isPhantom(flags) && resourceInfo.getSyncInfo(false) == null) {
			MultiStatus status = new MultiStatus(ResourcesPlugin.PI_RESOURCES, Status.OK, Policy.bind("ok"), null);
 			((Resource) resource).deleteResource(false, status);
 			if (!status.isOK())
 				throw new ResourceException(status);
 		}
 	} finally {
 		workspace.endOperation(false, null);
 	}
 }
 public void snapSyncInfo(IResource resource, DataOutputStream output) throws IOException {
 	writer.snapSyncInfo(resource, output);
 }
 }
