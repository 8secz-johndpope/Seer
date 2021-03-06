 /*******************************************************************************
  * Copyright (c) 2007 IBM Corporation and others.
  * All rights reserved. This program and the accompanying materials
  * are made available under the terms of the Eclipse Public License v1.0
  * which accompanies this distribution, and is available at
  * http://www.eclipse.org/legal/epl-v10.html
  *
  * Contributors:
  *     IBM Corporation - initial API and implementation
  *******************************************************************************/
 package org.eclipse.equinox.p2.artifact.repository;
 
 import java.io.OutputStream;
 import org.eclipse.core.runtime.IProgressMonitor;
 import org.eclipse.core.runtime.IStatus;
 import org.eclipse.equinox.p2.core.repository.IRepository;
 import org.eclipse.equinox.p2.metadata.IArtifactKey;
 import org.eclipse.equinox.spi.p2.artifact.repository.AbstractArtifactRepository;
 
 /**
  * A repository containing artifacts.
  * <p>
  * This interface is not intended to be implemented by clients.  Artifact repository
  * implementations must subclass {@link AbstractArtifactRepository} rather than 
  * implementing this interface directly.
  * </p>
  */
 public interface IArtifactRepository extends IRepository {
 	/**
 	 * Add the given descriptor to the set of descriptors in this repository.  This is 
 	 * a relatively low-level operation that should be used only when the actual related 
 	 * content is in this repository and the given descriptor accurately describes 
 	 * that content.
 	 * @param descriptor the descriptor to add.
 	 */
 	public void addDescriptor(IArtifactDescriptor descriptor);
 
 	/** 
 	 * Returns true if this repository contains the given descriptor.
 	 * @param descriptor the descriptor to query
 	 * @return true if the given descriptor is already in this repository
 	 */
 	public boolean contains(IArtifactDescriptor descriptor);
 
 	/** 
 	 * Returns true if this repository contains the given artifact key.
 	 * @param key the key to query
 	 * @return true if the given key is already in this repository
 	 */
 	public boolean contains(IArtifactKey key);
 
 	/**
 	 * Fill the given stream with the described artifact. Sets status accordingly. 
 	 */
 	public IStatus getArtifact(IArtifactDescriptor descriptor, OutputStream destination, IProgressMonitor monitor);
 
 	/**
 	 * Return the set of artifact descriptors describing the ways that this repository
 	 * can supply the artifact associated with the given artifact key
 	 * @param key the artifact key to lookup
 	 * @return the descriptors associated with the given key
 	 */
 	public IArtifactDescriptor[] getArtifactDescriptors(IArtifactKey key);
 
 	/**
 	 * Returns the list of artifact keys managed by this repository
 	 * @return list of artifact keys
 	 */
 	public IArtifactKey[] getArtifactKeys();
 
 	/**
 	 * Executes the given artifact requests on this byte server.
 	 * @param requests The artifact requests
 	 * @param monitor
 	 * @return a status object that is <code>OK</code> if requests were
 	 * processed successfully. Otherwise, a status indicating information,
 	 * warnings, or errors that occurred while executing the artifact requests
 	 */
 	public IStatus getArtifacts(IArtifactRequest[] requests, IProgressMonitor monitor);
 
 	/**
 	 * Open an output stream to which a client can write the data for the given 
 	 * artifact descriptor.
 	 * @param descriptor the descriptor describing the artifact data to be written to the 
 	 * resultant stream
 	 * @return the stream to which the artifact content can be written
 	 */
	public OutputStream getOutputStream(IArtifactDescriptor descriptor);
 
 	/**
 	 * Remove the all keys, descriptors, and contents from this repository.
 	 */
 	public void removeAll();
 
 	/**
 	 * Remove the given descriptor and its corresponding content in this repository.  
 	 * @param descriptor the descriptor to remove.
 	 */
 	public void removeDescriptor(IArtifactDescriptor descriptor);
 
 	/**
 	 * Remove the given key and all related content and descriptors from this repository.  
 	 * @param key the key to remove.
 	 */
 	public void removeDescriptor(IArtifactKey key);
 
 }
