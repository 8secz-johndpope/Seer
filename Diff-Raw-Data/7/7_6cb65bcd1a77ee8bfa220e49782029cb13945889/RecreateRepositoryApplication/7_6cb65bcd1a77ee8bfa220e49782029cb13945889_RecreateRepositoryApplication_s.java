 /*******************************************************************************
  * Copyright (c) 2009 IBM Corporation and others.
  * All rights reserved. This program and the accompanying materials
  * are made available under the terms of the Eclipse Public License v1.0
  * which accompanies this distribution, and is available at
  * http://www.eclipse.org/legal/epl-v10.html
  *
  * Contributors:
  *     IBM Corporation - initial API and implementation
  *******************************************************************************/
 
 package org.eclipse.equinox.p2.internal.repository.tools;
 
 import java.io.File;
 import java.io.IOException;
 import java.net.URI;
 import java.util.*;
 import org.eclipse.core.runtime.*;
 import org.eclipse.equinox.internal.p2.artifact.repository.simple.SimpleArtifactRepository;
 import org.eclipse.equinox.internal.provisional.p2.artifact.repository.*;
 import org.eclipse.equinox.internal.provisional.p2.artifact.repository.processing.ProcessingStepDescriptor;
 import org.eclipse.equinox.internal.provisional.p2.core.ProvisionException;
 import org.eclipse.equinox.internal.provisional.p2.metadata.IArtifactKey;
 import org.eclipse.equinox.internal.provisional.p2.repository.IRepository;
 import org.eclipse.equinox.internal.provisional.p2.repository.IRepositoryManager;
 import org.eclipse.osgi.util.NLS;
 
 public class RecreateRepositoryApplication {
 	static final private String PUBLISH_PACK_FILES_AS_SIBLINGS = "publishPackFilesAsSiblings"; //$NON-NLS-1$
 	private RepositoryDescriptor descriptor;
 	private String repoName = null;
 	boolean removeArtifactRepo = true;
 	private Map repoProperties = null;
 	private Map repoMap = null;
 
 	public IStatus run(IProgressMonitor monitor) throws ProvisionException, IOException {
 
 		try {
 			IArtifactRepository repository = initialize(monitor);
 			removeRepository(repository, monitor);
 			recreateRepository(monitor);
 		} finally {
 			if (removeArtifactRepo) {
 				IArtifactRepositoryManager repositoryManager = Activator.getArtifactRepositoryManager();
 				repositoryManager.removeRepository(descriptor.getRepoLocation());
 			}
 		}
 
 		return Status.OK_STATUS;
 	}
 
 	public void setArtifactRepository(RepositoryDescriptor descriptor) {
 		this.descriptor = descriptor;
 	}
 
 	private IArtifactRepository initialize(IProgressMonitor monitor) throws ProvisionException {
 		IArtifactRepositoryManager repositoryManager = Activator.getArtifactRepositoryManager();
 		removeArtifactRepo = !repositoryManager.contains(descriptor.getRepoLocation());
 
 		IArtifactRepository repository = repositoryManager.loadRepository(descriptor.getRepoLocation(), IRepositoryManager.REPOSITORY_HINT_MODIFIABLE, monitor);
 
 		if (repository == null || !repository.isModifiable())
 			throw new ProvisionException(NLS.bind(Messages.exception_destinationNotModifiable, repository.getLocation()));
 		if (!(repository instanceof IFileArtifactRepository))
 			throw new ProvisionException(NLS.bind(Messages.exception_notLocalFileRepo, repository.getLocation()));
 
 		repoName = repository.getName();
 		repoProperties = repository.getProperties();
 
 		repoMap = new HashMap();
 		IArtifactKey[] keys = repository.getArtifactKeys();
 		for (int i = 0; i < keys.length; i++) {
 			IArtifactDescriptor[] descriptors = repository.getArtifactDescriptors(keys[i]);
 			repoMap.put(keys[i], descriptors);
 		}
 
 		return repository;
 	}
 
 	private void removeRepository(IArtifactRepository repository, IProgressMonitor monitor) throws ProvisionException, IOException {
 		IArtifactRepositoryManager manager = Activator.getArtifactRepositoryManager();
 		manager.removeRepository(repository.getLocation());
 
 		boolean compressed = Boolean.valueOf((String) repoProperties.get(IRepository.PROP_COMPRESSED)).booleanValue();
 		URI realLocation = SimpleArtifactRepository.getActualLocation(repository.getLocation(), compressed);
 		File realFile = URIUtil.toFile(realLocation);
 
 		if (!realFile.exists() || !realFile.delete())
 			throw new ProvisionException(NLS.bind(Messages.exception_unableToRemoveRepo, realFile.toString()));
 	}
 
 	private void recreateRepository(IProgressMonitor monitor) throws ProvisionException {
 		IArtifactRepositoryManager manager = Activator.getArtifactRepositoryManager();
 
 		//add pack200 mappings, the existing repoProperties is not modifiable 
 		Map newProperties = new HashMap(repoProperties);
 		newProperties.put(PUBLISH_PACK_FILES_AS_SIBLINGS, "true"); //$NON-NLS-1$
 		IArtifactRepository repository = manager.createRepository(descriptor.getRepoLocation(), repoName, IArtifactRepositoryManager.TYPE_SIMPLE_REPOSITORY, newProperties);
 		if (!(repository instanceof IFileArtifactRepository))
 			throw new ProvisionException(NLS.bind(Messages.exception_notLocalFileRepo, repository.getLocation()));
 
 		IFileArtifactRepository simple = (IFileArtifactRepository) repository;
 		for (Iterator iterator = repoMap.keySet().iterator(); iterator.hasNext();) {
 			IArtifactKey key = (IArtifactKey) iterator.next();
 			IArtifactDescriptor[] descriptors = (IArtifactDescriptor[]) repoMap.get(key);
 
 			String unpackedSize = null;
 			File packFile = null;
 			Set files = new HashSet();
 			for (int i = 0; i < descriptors.length; i++) {
 				File artifactFile = simple.getArtifactFile(descriptors[i]);
 				files.add(artifactFile);
 
 				String size = Long.toString(artifactFile.length());
 
 				ArtifactDescriptor newDescriptor = new ArtifactDescriptor(descriptors[i]);
 				newDescriptor.setProperty(IArtifactDescriptor.ARTIFACT_SIZE, size);
 				newDescriptor.setProperty(IArtifactDescriptor.DOWNLOAD_SIZE, size);
				//only set an MD5 if there was one to start with
				if (newDescriptor.getProperties().containsKey(IArtifactDescriptor.DOWNLOAD_MD5))
					newDescriptor.setProperty(IArtifactDescriptor.DOWNLOAD_MD5, RepositoryUtilities.computeMD5(artifactFile));
 
 				File temp = new File(artifactFile.getParentFile(), artifactFile.getName() + ".pack.gz"); //$NON-NLS-1$
 				if (temp.exists()) {
 					packFile = temp;
 					unpackedSize = size;
 				}
 
 				repository.addDescriptor(newDescriptor);
 			}
 			if (packFile != null && !files.contains(packFile) && packFile.length() > 0) {
 				ArtifactDescriptor packDescriptor = createPack200ArtifactDescriptor(key, packFile, unpackedSize);
 				repository.addDescriptor(packDescriptor);
 			}
 		}
 	}
 
 	private ArtifactDescriptor createPack200ArtifactDescriptor(IArtifactKey key, File packFile, String installSize) {
 		final String PACKED_FORMAT = "packed"; //$NON-NLS-1$
 
 		if (packFile != null && packFile.exists()) {
 			ArtifactDescriptor result = new ArtifactDescriptor(key);
 			result.setProperty(IArtifactDescriptor.ARTIFACT_SIZE, installSize);
 			result.setProperty(IArtifactDescriptor.DOWNLOAD_SIZE, Long.toString(packFile.length()));
 			ProcessingStepDescriptor[] steps = new ProcessingStepDescriptor[] {new ProcessingStepDescriptor("org.eclipse.equinox.p2.processing.Pack200Unpacker", null, true)}; //$NON-NLS-1$
 			result.setProcessingSteps(steps);
 			result.setProperty(IArtifactDescriptor.FORMAT, PACKED_FORMAT);
 			return result;
 		}
 		return null;
 	}
 }
