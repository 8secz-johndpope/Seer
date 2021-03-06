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
 package org.eclipse.team.internal.core.target;
 
 import java.io.ByteArrayInputStream;
 import java.io.ByteArrayOutputStream;
 import java.io.DataInputStream;
 import java.io.DataOutputStream;
 import java.io.File;
 import java.io.IOException;
 import java.net.URL;
 
 import org.eclipse.core.resources.IResource;
 import org.eclipse.core.resources.ResourcesPlugin;
 import org.eclipse.core.runtime.CoreException;
 import org.eclipse.core.runtime.IPath;
 import org.eclipse.core.runtime.IProgressMonitor;
 import org.eclipse.core.runtime.IStatus;
 import org.eclipse.core.runtime.Platform;
 import org.eclipse.core.runtime.QualifiedName;
 import org.eclipse.team.core.TeamException;
 import org.eclipse.team.core.target.Site;
 import org.eclipse.team.internal.core.Policy;
 import org.eclipse.team.internal.core.TeamPlugin;
 
 /**
  * This abstract class implements the state of a local and corresponding remote resource,
  * and behavior of those resources.
  * <p>
  * Common state for all 'managed' resources includes:
  * <ul>
  * 	<li>the local resource and corresponding remote resource objects</li>
  * 	<li>the timestamp of the local resource as it was last in synch with the provider</li>
  * 	<li>an identifier for the remote resource as it was last in synch with the provider</li>
  */
 public abstract class ResourceState {
 
 	/*
 	 * Serialization format identifier.
 	 * see toBytes() and fromBytes()
 	 */
 	private static final int BYTES_FORMAT = 0;
 
 	/*
 	 * These constants are used to indicate uninitialized values for the local
 	 * base timestamp and remote base identifier.
 	 */
 	protected static final long EMPTY_LOCALBASETS = -1L;
 	protected static final String EMPTY_REMOTEBASEID = "Undefined:"; //$NON-NLS-1$
 
 	/*
 	 * The base state of the resource.  The 'base' is the state of the resource
 	 * state that was fetched from (or put in) the provider.
 	 */
 	protected long localBaseTimestamp = EMPTY_LOCALBASETS;
 	protected String remoteBaseIdentifier = EMPTY_REMOTEBASEID;
 
 	protected boolean checkedOut = true;
 
 	/*
 	 * This is the local resource that the receiver represents.  It is initialized by
 	 * the constructor.  (The remote resource is maintained by specific subclasses
 	 * as it is type-dependent.)
 	 */
 	protected IResource localResource;
 	
 	protected QualifiedName stateKey = new QualifiedName("org.eclipse.team.target", "state_info"); //$NON-NLS-1$ //$NON-NLS-2$
 
 	protected URL rootUrl;
 
 	/**
 	 * Constructor for a resource state given a local resource.
 	 * Remember which local resource this state represents.
 	 * 
 	 * @param localResource the local part of a synchronized pair of resources.
 	 */
 	public ResourceState(IResource localResource, URL rootUrl) {
 		super();
 		this.rootUrl = rootUrl;
 		SynchronizedTargetProvider.getSynchronizer().add(stateKey);
 		this.localResource = localResource;
 	}
 	
 	/**
 	 * Get the timestamp that represents the base state of the local resource, that is
 	 * the state that the local resource had when it was initially fetched from the repository.
 	 * 
 	 * @return the timestamp of the local state of the resource (as reported by
 	 * java.io.File.getLastModified()) at the point the resource was downloaded to the
 	 * workspace.
 	 * @throws BaseIdentifierNotInitializedException if the resource has not yet been
 	 * downloaded.
 	 */
 	public long getLocalBaseTimestamp()
 		throws BaseIdentifierNotInitializedException {
 		if (localBaseTimestamp == EMPTY_LOCALBASETS)
 			throw new BaseIdentifierNotInitializedException();
 		return localBaseTimestamp;
 	}
 
 	
 	/**
 	 * Get the identifier that represents the base state of the remote resource, that is
 	 * the state of the remote resource when it was fetched as the base state of the
 	 * local resource.
 	 * <p>
 	 * In general, repositories have arbitrary ways to distinguish resource states.
 	 * The result should only be used for equality comparison, there should be no
 	 * ordering or other information implied from the value returned.  For example,
 	 * the value may be a version identifier, timestamp, ETag, etc.  To ensure
 	 * schemes do not inadvertantly test equal it is recommended that the identifier
 	 * be a URI where the scheme denotes the value type,
 	 * e.g., date-rfc1123:Fri, 16 Nov 2001 06:25:24 GMT</p>
 	 * 
 	 * @return an opaque identifier to the base state of the resource in the provider.
 	 * @throws BaseIdentifierNotInitializedException if the resource has not yet been
 	 * downloaded.
 	 */
 	public String getRemoteBaseIdentifier()
 		throws BaseIdentifierNotInitializedException {
 		if (remoteBaseIdentifier.equals(EMPTY_REMOTEBASEID))
 			throw new BaseIdentifierNotInitializedException();
 		return remoteBaseIdentifier;
 	}
 
 	
 	/**
 	 * Get the identifier that represents the released state of the resource,
 	 * that is the state that it currently has in the repository.
 	 * <p>
 	 * In general, repositories have arbitrary ways to distinguish resource states.
 	 * The result should only be used for equality comparison, there should be no
 	 * ordering or other information implied from the value returned.  For example,
 	 * the value may be a version identifier, timestamp, ETag, etc.  To ensure
 	 * schemes do not inadvertantly test equal it is recommended that the identifier
 	 * be a URI where the scheme denotes the value type,
 	 * e.g., date-rfc1123:Fri, 16 Nov 2001 06:25:24 GMT</p>
 	 * 
 		 * @param progress a progress monitor to indicate the duration of the operation, or
 	 * <code>null</code> if progress reporting is not required.
 	 * @return an opaque identifier to the current released state of the resource in
 	 * the provider.
 	 * @throws TeamException if there is a problem getting the released state
 	 * identifier from the provider.  Valid exception status codes include:
 	 * <ul>
 	 * 		<li>IO_FAILED</li>
 	 * 		<li>NO_REMOTE_RESOURCE</li>
 	 * </ul></p>
 	 */
 	public abstract String getReleasedIdentifier(IProgressMonitor monitor) throws TeamException;
 
 	/**
 	 * Check out the receiver. Return a status if the receiver is in the wrong state for the operation to be performed.
 	 * 
 	 * @throws TeamException if there is a error communicating with the resource from the server.
 	 */
 	public void checkout(IProgressMonitor progress) throws TeamException {
 		progress.beginTask(null, 100);
 		try {
 			// Not going to allow branching.
 			if (isOutOfDate(Policy.subMonitorFor(progress, 50)))
 				throw new TeamException(ITeamStatusConstants.CONFLICT_STATUS);
 			
 			// Sanity check.
 			if (!hasRemote(Policy.subMonitorFor(progress, 50)))
 				throw new TeamException(ITeamStatusConstants.NO_REMOTE_RESOURCE_STATUS);
 			
 			// Legally, the resource must be checked in before it can be checked out.
 			if (isCheckedOut())
 				throw new TeamException(ITeamStatusConstants.NOT_CHECKED_IN_STATUS);
 			
 			// Do the provider specific action for check-out.
 			basicCheckout(progress);
 		} finally {
 			progress.done();
 		}
 	}
 	
 	/**
 	 * A basic checkout is provider specific.
 	 * Unless overridden, work in an optimistic mode.
 	 */
 	protected void basicCheckout(IProgressMonitor progress) throws TeamException {
 		checkedOut = true;
 	}
 	
 	
 	/**
 	 * Check in the receiver. Return a status if the receiver is in the wrong state for the operation to be performed.
 	 * 
 	 * @throws TeamException if there is a error communicating with the resource from the server.
 	 */
 	public void checkin(IProgressMonitor progress) throws TeamException {
		progress = Policy.monitorFor(progress);
 		progress.beginTask(null, 100);
 		try {
 			// The resource must be checked out before it can be checked in.
 			if (!isCheckedOut())
 				throw new TeamException(ITeamStatusConstants.NOT_CHECKED_OUT_STATUS);
 			
 			// Check to see if we can do this without conflict.
 			if (isOutOfDate(Policy.subMonitorFor(progress, 10)))
 				throw new TeamException(ITeamStatusConstants.CONFLICT_STATUS);
 				
 			// Copy from the local resource to the repository.	
 			upload(Policy.subMonitorFor(progress, 90));
 			//if we got to here the upload succeeded (didn't throw)
 			checkedOut = false;
 		} finally {
 			progress.done();
 		}
 	}
 	
 	/**
 	 * Uncheckout the receiver.
 	 */
 	public void uncheckout(IProgressMonitor progress) throws TeamException {
 		// Has to be checked-out before it can be reversed.
 		if (!isCheckedOut())
 			throw new TeamException(ITeamStatusConstants.NOT_CHECKED_OUT_STATUS);
 
 		// Nothing interesting to do since the API spec. requires that we do not reverse
 		// any local changes.
 		checkedOut = false;
 	}
 
 	/**
 	 * Answer whether the receiver is checked out or not.
 	 * <p>
 	 * Note that this is a quick operation that will be called from the UI, so providers are required
 	 * to cache information that is expensive to compute.  Where the cache may get stale users
 	 * have the opportunity to force a refresh using ITeamProvider.refreshState().
 	 * 
 	 * @return <code>true</code> if the receiver is checked in, and <code>false</code>
 	 * if it is not.
 	 * @see ITeamProvider#isCheckedOut(IResource)
 	 * @see ITeamProvider#refreshState(IResource[], int, IProgressMonitor)
 	 */
 	public boolean isCheckedOut() {
 		return checkedOut;
 	}
 
 	/**
 	 * Answer if the local resource currently has a different timestamp to the
 	 * base timestamp for this resource.
 	 * 
 	 * @param resource the resource to test.
 	 * @return <code>true</code> if the resource has a different modification
 	 * timestamp, and <code>false</code> otherwise.
 	 * @see ITeamProvider#isDirty(IResource)
 	 */
 	public boolean isDirty(IResource resource) {
 		if (!hasLocal())
 			return false; // by API definition.
 		if (localBaseTimestamp == EMPTY_LOCALBASETS)
 			return false; // by API definition.
 		return localBaseTimestamp != resource.getModificationStamp();
 	}
 
 	/**
 	 * Answers true if the base identifier of the given resource is different to the
 	 * current released state of the resource.
 	 */
 	public boolean isOutOfDate(IProgressMonitor monitor) throws TeamException {
 		if (remoteBaseIdentifier.equals(EMPTY_REMOTEBASEID))
 			return false; // by definition.
 
 		String releasedIdentifier = null;
 		releasedIdentifier = getReleasedIdentifier(monitor);
 		return !remoteBaseIdentifier.equals(releasedIdentifier);
 	}
 
 	/**
 	 * Download the remote resource represented by the receiver state to the location
 	 * represented by the local resource (i.e., resource.getLocation().toFile()).
 	 * This copies from the provider to the workspace, <em>and</em> sets the local
 	 * base timestamp and remote base identifier.
 	 * The provider may (and should wherever possible) optimize the case where it
 	 * knows the local resource is identical to the remote resource.
 	 */
 	public abstract void download(IProgressMonitor progress) throws TeamException;
 
 	/**
 	 * Upload the resource represented by the local resource to the remote
 	 * resource represented by the receiver.  This copies from the workspace to
 	 * the provider <em>and</em> sets the local base timestamp and remote base
 	 * identifier.
 	 */
 	public abstract void upload(IProgressMonitor progress) throws TeamException;
 
 	/**
 	 * Delete the remote resource.
 	 */
 	public abstract void delete(IProgressMonitor progress) throws TeamException;
 
 	/**
 	 * Answer if the remote resource exists.
 	 */
 	public abstract boolean hasRemote(IProgressMonitor monitor) throws TeamException;
 
 	/**
 	 * Answer the type of the remote resource (if it exists).
 	 * The type should correspond to the IResource enumerated types.
 	 */
 	public abstract int getRemoteType();
 
 	/**
 	 * Answer the array of resource states for each member of the receiver.
 	 * If the receiver has no members (or is incapable of having members)
 	 * answer an empty array.
 	 */
 	public abstract ResourceState[] getRemoteChildren() throws TeamException;
 
 	/**
 	 */
 	public IResource getLocal() {
 		return localResource;
 	}
 
 	/**
 	 * Get the file underlying the local resource.
 	 */
 	protected File getLocalFile() {
 		return localResource.getLocation().toFile();
 	}
 
 	/**
 	 * Answer if the local resource exists.
 	 */
 	public boolean hasLocal() {
 		return localResource.exists();
 	}
 
 	/**
 	 * Initializes the resource state instance from the given serialized state.
 	 * The format of the serialized state is that produced by <code>toBytes()</code>.
 	 * 
 	 * @param bytes the serialized resource state.
 	 */
 	public final void loadState() {
 		try {
 			byte[] storedState =
 				SynchronizedTargetProvider.getSynchronizer().getSyncInfo(stateKey, localResource);
 			if (storedState != null)
 				fromBytes(storedState);
 		} catch (CoreException e) {
 			// Problem loading the stored state.
 			e.printStackTrace();
 			throw new RuntimeException();
 		}
 	}
 
 	/**
 	 * Initializes the resource state instance from the given serialized state.
 	 * The format of the serialized state is that produced by <code>toBytes()</code>.
 	 * 
 	 * @param bytes the serialized resource state.
 	 */
 	protected void fromBytes(byte[] bytes) {
 		try {
 			DataInputStream dataStream =
 				new DataInputStream(new ByteArrayInputStream(bytes));
 			if (BYTES_FORMAT != dataStream.readByte())
 				return;
 
 			// Restore common resource state values.
 			remoteBaseIdentifier = dataStream.readUTF();
 			localBaseTimestamp = dataStream.readLong();
 
 		} catch (IOException e) {
 			// Problem parsing the stored state.
 			e.printStackTrace();
 			throw new RuntimeException();
 		}
 	};
 
 	public final void storeState() {
 		try {
 			SynchronizedTargetProvider.getSynchronizer().setSyncInfo(
 				stateKey,
 				localResource,
 				toBytes());
 			ResourcesPlugin.getWorkspace().save(false, null);
 		} catch (CoreException e) {
 			e.printStackTrace();
 			throw new RuntimeException();
 		}
 	}
 
 	/**
 	 * Answer the resource state as a sequence of bytes, in a format that can be used to 
 	 * reconstruct an equivalent resource state using the <code>fromBytes(byte[])</code>
 	 * method.
 	 * <p>
 	 * Subclasses should implement <code>storeState(DataOutputStream)</code> to
 	 * store provider specific state information.</p>
 	 * 
 	 * @return the resource state as a byte array.
 	 * @see #storeState(DataOutputStream)
 	 * @see fromBytes(byte[])
 	 */
 	protected byte[] toBytes() {
 		try {
 			// Create a stream to store the byte representation of the receiver's state.
 			ByteArrayOutputStream byteStream = new ByteArrayOutputStream(32);
 			// Guess ~32 bytes
 			DataOutputStream dataStream = new DataOutputStream(byteStream);
 			dataStream.writeByte(BYTES_FORMAT);
 
 			// Store data common to all resource states.
 			dataStream.writeUTF(remoteBaseIdentifier);
 			dataStream.writeLong(localBaseTimestamp);
 
 			dataStream.close();
 			return byteStream.toByteArray();
 		} catch (IOException e) {
 			e.printStackTrace();
 			throw new RuntimeException();
 		}
 	}
 
 	final public void removeState() {
 		try {
 			SynchronizedTargetProvider.getSynchronizer().flushSyncInfo(
 				stateKey,
 				localResource,
 				IResource.DEPTH_INFINITE);
 		} catch (CoreException e) {
 			e.printStackTrace();
 			throw new RuntimeException();
 		}
 	}
 
 	/**
 	 * @see org.eclipse.core.runtime.IAdaptable#getAdapter(Class)
 	 */
 	public Object getAdapter(Class adapter) {
 		return Platform.getAdapterManager().getAdapter(this, adapter);
 	}
 
 	/**
 	 * Method getRoot.
 	 * @return URL of this resource's parent
 	 */
 	public URL getRoot() {
 		return rootUrl;
 	}
 }
