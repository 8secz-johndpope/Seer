 /*******************************************************************************
  * Copyright (c) 2007, 2008 IBM Corporation and others.
  * All rights reserved. This program and the accompanying materials
  * are made available under the terms of the Eclipse Public License v1.0
  * which accompanies this distribution, and is available at
  * http://www.eclipse.org/legal/epl-v10.html
  *
  * Contributors:
  *     IBM Corporation - initial API and implementation
  *		compeople AG (Stefan Liebig) - various ongoing maintenance
  *   	Genuitec LLC - various bug fixes
  *******************************************************************************/
 package org.eclipse.equinox.internal.p2.artifact.repository;
 
 import java.io.*;
 import java.util.Properties;
 import org.eclipse.core.runtime.*;
 import org.eclipse.equinox.internal.provisional.p2.artifact.repository.*;
 import org.eclipse.equinox.internal.provisional.p2.artifact.repository.processing.ProcessingStepDescriptor;
 import org.eclipse.equinox.internal.provisional.p2.artifact.repository.processing.ProcessingStepHandler;
 import org.eclipse.equinox.internal.provisional.p2.core.ProvisionException;
 import org.eclipse.equinox.internal.provisional.p2.metadata.IArtifactKey;
 import org.eclipse.osgi.util.NLS;
 
 /**
  * A request to mirror (copy) an artifact into a given destination artifact repository.
  */
 public class MirrorRequest extends ArtifactRequest {
 	private static final ProcessingStepDescriptor[] EMPTY_STEPS = new ProcessingStepDescriptor[0];
 
 	private final IArtifactRepository target;
 
 	private final Properties targetDescriptorProperties;
 	private final Properties targetRepositoryProperties;
 	protected IArtifactDescriptor descriptor;
 
 	public MirrorRequest(IArtifactKey key, IArtifactRepository targetRepository, Properties targetDescriptorProperties, Properties targetRepositoryProperties) {
 		super(key);
 		target = targetRepository;
 		if (targetDescriptorProperties == null || targetDescriptorProperties.isEmpty()) {
 			this.targetDescriptorProperties = null;
 		} else {
 			this.targetDescriptorProperties = new Properties();
 			this.targetDescriptorProperties.putAll(targetDescriptorProperties);
 		}
 
 		if (targetRepositoryProperties == null || targetRepositoryProperties.isEmpty()) {
 			this.targetRepositoryProperties = null;
 		} else {
 			this.targetRepositoryProperties = new Properties();
 			this.targetRepositoryProperties.putAll(targetRepositoryProperties);
 		}
 	}
 
 	public void perform(IProgressMonitor monitor) {
 		monitor.subTask(NLS.bind(Messages.downloading, getArtifactKey().getId()));
 		// Do we already have the artifact in the target?
 		if (target.contains(getArtifactKey())) {
 			setResult(new Status(IStatus.OK, Activator.ID, NLS.bind(Messages.available_already_in, getArtifactKey())));
 			return;
 		}
 
 		// if the request does not have a descriptor then try to fill one in by getting
 		// the list of all and randomly picking one that appears to be optimized.
 		IArtifactDescriptor optimized = null;
 		IArtifactDescriptor canonical = null;
 		if (descriptor == null) {
 			IArtifactDescriptor[] descriptors = source.getArtifactDescriptors(getArtifactKey());
 			if (descriptors.length > 0) {
 				for (int i = 0; i < descriptors.length; i++) {
 					if (descriptors[i].getProperty(IArtifactDescriptor.FORMAT) == null)
 						canonical = descriptors[i];
 					else if (ProcessingStepHandler.canProcess(descriptors[i]))
 						optimized = descriptors[i];
 				}
 				boolean chooseCanonical = source.getLocation().getScheme().equals("file"); //$NON-NLS-1$
 				// If the source repo is local then look for a canonical descriptor so we don't waste processing time.
 				descriptor = chooseCanonical ? canonical : optimized;
 				// if the descriptor is still null then we could not find our first choice of format so switch the logic.
 				if (descriptor == null)
 					descriptor = !chooseCanonical ? canonical : optimized;
 			}
 		}
 
 		// if the descriptor is not set now then the repo does not have the requested artifact
 		// TODO improve the reporting here.  It may be the case that the repo has the artifact
 		// but the client does not have a processor
 		if (descriptor == null) {
 			setResult(new Status(IStatus.ERROR, Activator.ID, NLS.bind(Messages.artifact_not_found, getArtifactKey())));
 			return;
 		}
 
 		ArtifactDescriptor destinationDescriptor = getDestinationDescriptor(descriptor);
 		IStatus status = transfer(destinationDescriptor, descriptor, monitor);
 		// if ok, cancelled or transfer has already been done with the canonical form return with status set 
 		if (status.getSeverity() == IStatus.CANCEL) {
 			setResult(status);
 			return;
 		}
 		if (monitor.isCanceled()) {
 			setResult(Status.CANCEL_STATUS);
 			return;
 		}
 		if (status.isOK()) {
 			setResult(status);
 			return;
 		}
 
 		// failed, first remove possibly erroneously added descriptor
 		if (target.contains(destinationDescriptor))
 			target.removeDescriptor(destinationDescriptor);
 
 		if (descriptor == canonical || canonical == null) {
 			setResult(status);
 			return;
 		}
 
 		// try with canonical
 		setResult(transfer(getDestinationDescriptor(canonical), canonical, monitor));
 	}
 
 	private ArtifactDescriptor getDestinationDescriptor(IArtifactDescriptor sourceDescriptor) {
 		// Get the descriptor to use to store the artifact
 		// Since we are mirroring, ensure we clear out data from the original descriptor that may
 		// not apply in the new repo location.
 		// TODO this is brittle.  perhaps the repo itself should do this?  there are cases where
 		// we really do need to give the repo the actual descriptor to use however...
 		ArtifactDescriptor destinationDescriptor = new ArtifactDescriptor(sourceDescriptor);
 		destinationDescriptor.setProcessingSteps(EMPTY_STEPS);
 		destinationDescriptor.setProperty(IArtifactDescriptor.DOWNLOAD_MD5, null);
 		destinationDescriptor.setProperty(IArtifactDescriptor.DOWNLOAD_CONTENTTYPE, null);
 		destinationDescriptor.setProperty(IArtifactDescriptor.FORMAT, null);
 		if (targetDescriptorProperties != null)
 			destinationDescriptor.addProperties(targetDescriptorProperties);
 		if (targetRepositoryProperties != null)
 			destinationDescriptor.addRepositoryProperties(targetRepositoryProperties);
 		return destinationDescriptor;
 	}
 
 	/**
 	 * Keep retrying the source repository until it reports back that it will be impossible
 	 * to get the artifact from it.
 	 * @param destinationDescriptor
 	 * @param sourceDescriptor
 	 * @param monitor
 	 * @return the status of the transfer operation
 	 */
 	private IStatus transfer(IArtifactDescriptor destinationDescriptor, IArtifactDescriptor sourceDescriptor, IProgressMonitor monitor) {
 		IStatus status = Status.OK_STATUS;
 		// go until we get one (OK), there are no more mirrors to consider or the operation is cancelled.
 		// TODO this needs to be redone with a much better mirror management scheme.
 		do {
 			status = transferSingle(destinationDescriptor, sourceDescriptor, monitor);
 		} while (status.getSeverity() == IStatus.ERROR && status.getCode() == IArtifactRepository.CODE_RETRY);
 		return status;
 	}
 
 	private IStatus transferSingle(IArtifactDescriptor destinationDescriptor, IArtifactDescriptor sourceDescriptor, IProgressMonitor monitor) {
 		OutputStream destination;
 		try {
 			destination = target.getOutputStream(destinationDescriptor);
 		} catch (ProvisionException e) {
 			return e.getStatus();
 		}
 
 		IStatus status = null;
 		// Do the actual transfer
 		try {
 			status = getSourceRepository().getArtifact(sourceDescriptor, destination, monitor);
 			if (destination instanceof IStateful && status != null && !status.isOK()) {
 				IStatus destStatus = ((IStateful) destination).getStatus();
 				IStatus root = extractRootCause(status);
 				Throwable e = root != null ? root.getException() : null;
 				((IStateful) destination).setStatus(new MultiStatus(Activator.ID, status.getCode(), new IStatus[] {status, destStatus}, status.getMessage(), e));
 			}
 		} finally {
 			try {
 				destination.close();
 			} catch (IOException e) {
 				if (status != null && status.getSeverity() == IStatus.ERROR && status.getCode() == IArtifactRepository.CODE_RETRY)
 					return new MultiStatus(Activator.ID, status.getCode(), new IStatus[] {status}, NLS.bind(Messages.error_closing_stream, getArtifactKey(), target.getLocation()), e);
 				return new Status(IStatus.ERROR, Activator.ID, NLS.bind(Messages.error_closing_stream, getArtifactKey(), target.getLocation()), e);
 			}
 			if (status != null && status.getSeverity() == IStatus.ERROR) {
 				IStatus root = extractRootCause(status);
 				if (root != null && FileNotFoundException.class == root.getException().getClass())
					return new Status(IStatus.ERROR, Activator.ID, NLS.bind(Messages.artifact_not_found, getArtifactKey()), root.getException());
 			}
 		}
 		return status;
 	}
 
 	/**
 	 * Extract the root cause. The root cause is the first severe non-MultiStatus status 
 	 * containing an exception when searching depth first otherwise null.
 	 * @param status
 	 * @return root cause
 	 */
 	private static IStatus extractRootCause(IStatus status) {
 		if (status == null)
 			return null;
 		if (!status.isMultiStatus())
 			return constraintStatus(status);
 
 		IStatus[] children = ((MultiStatus) status).getChildren();
 		if (children == null)
 			return constraintStatus(status);
 
 		for (int i = 0; i < children.length; i++) {
 			IStatus deeper = extractRootCause(children[i]);
 			if (deeper != null)
 				return deeper;
 		}
 
 		return constraintStatus(status);
 	}
 
 	private static IStatus constraintStatus(IStatus status) {
 		return status.getSeverity() == IStatus.ERROR && status.getException() != null ? status : null;
 	}
 
 	public String toString() {
 		return Messages.mirroring + getArtifactKey() + " into " + target; //$NON-NLS-1$
 	}
 }
