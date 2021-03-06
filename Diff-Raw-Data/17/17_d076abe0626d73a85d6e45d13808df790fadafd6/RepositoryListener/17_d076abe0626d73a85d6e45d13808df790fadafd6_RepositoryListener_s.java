 /*******************************************************************************
  * Copyright (c) 2007, 2008 IBM Corporation and others. All rights reserved.
  * This program and the accompanying materials are made available under the
  * terms of the Eclipse Public License v1.0 which accompanies this distribution,
  * and is available at http://www.eclipse.org/legal/epl-v10.html
  * 
  * Contributors: 
  * IBM Corporation - initial implementation and ideas 
  ******************************************************************************/
 package org.eclipse.equinox.internal.provisional.p2.directorywatcher;
 
 import java.io.File;
 import java.net.MalformedURLException;
 import java.net.URL;
 import java.util.*;
 import org.eclipse.equinox.internal.p2.core.helpers.LogHelper;
 import org.eclipse.equinox.internal.p2.metadata.generator.features.FeatureParser;
 import org.eclipse.equinox.internal.provisional.p2.artifact.repository.*;
 import org.eclipse.equinox.internal.provisional.p2.core.ProvisionException;
 import org.eclipse.equinox.internal.provisional.p2.core.repository.IRepository;
 import org.eclipse.equinox.internal.provisional.p2.metadata.IArtifactKey;
 import org.eclipse.equinox.internal.provisional.p2.metadata.IInstallableUnit;
 import org.eclipse.equinox.internal.provisional.p2.metadata.generator.*;
 import org.eclipse.equinox.internal.provisional.p2.metadata.repository.IMetadataRepository;
 import org.eclipse.equinox.internal.provisional.p2.metadata.repository.IMetadataRepositoryManager;
 import org.eclipse.equinox.internal.provisional.p2.query.Query;
 import org.eclipse.osgi.service.resolver.*;
 import org.osgi.framework.BundleContext;
 import org.osgi.framework.ServiceReference;
 
 public class RepositoryListener extends DirectoryChangeListener {
 
 	private static final String ARTIFACT_FOLDER = "artifact.folder"; //$NON-NLS-1$
 	private static final String ARTIFACT_REFERENCE = "artifact.reference"; //$NON-NLS-1$
 	private static final String FILE_LAST_MODIFIED = "file.lastModified"; //$NON-NLS-1$
 	private static final String FILE_NAME = "file.name"; //$NON-NLS-1$
 	private final IMetadataRepository metadataRepository;
 	private final IArtifactRepository artifactRepository;
 	private final BundleDescriptionFactory bundleDescriptionFactory;
 	private final Map currentFiles = new HashMap();
 
 	/**
 	 * Create a repository listener that watches the specified folder and generates repositories
 	 * for its content.
 	 * @param context the bundle context
 	 * @param repositoryName the repository name to use for the repository
 	 * @param repositoryFolder the target folder for the repository, or <code>null</code> if a folder based on the
 	 * bundle's data location should be used.
 	 * @param hidden <code>true</code> if the repository should be hidden, <code>false</code> if not.
 	 */
 	public RepositoryListener(BundleContext context, String repositoryName, File repositoryFolder, boolean hidden) {
 
 		File stateDir;
 		if (repositoryFolder == null) {
 			String stateDirName = "listener_" + repositoryName.hashCode();
 			stateDir = context.getDataFile(stateDirName);
 			stateDir.mkdirs();
 		} else {
 			stateDir = repositoryFolder;
 		}
 
 		URL stateDirURL;
 		try {
 			stateDirURL = stateDir.toURL();
 		} catch (MalformedURLException e) {
 			throw new IllegalStateException(e.getMessage());
 		}
 
 		metadataRepository = initializeMetadataRepository(context, repositoryName, stateDirURL, hidden);
 		artifactRepository = initializeArtifactRepository(context, repositoryName, stateDirURL, hidden);
 		bundleDescriptionFactory = initializeBundleDescriptionFactory(context);
 	}
 
 	public RepositoryListener(BundleContext context, String repositoryName) {
 		this(context, repositoryName, null, false);
 	}
 
 	public RepositoryListener(BundleContext context, IMetadataRepository metadataRepository, IArtifactRepository artifactRepository) {
 		this.artifactRepository = artifactRepository;
 		this.metadataRepository = metadataRepository;
 		bundleDescriptionFactory = initializeBundleDescriptionFactory(context);
 	}
 
 	private BundleDescriptionFactory initializeBundleDescriptionFactory(BundleContext context) {
 
 		ServiceReference reference = context.getServiceReference(PlatformAdmin.class.getName());
 		if (reference == null)
 			throw new IllegalStateException("PlatformAdmin not registered.");
 		PlatformAdmin platformAdmin = (PlatformAdmin) context.getService(reference);
 		if (platformAdmin == null)
 			throw new IllegalStateException("PlatformAdmin not registered.");
 
 		try {
 			StateObjectFactory stateObjectFactory = platformAdmin.getFactory();
 			return new BundleDescriptionFactory(stateObjectFactory, null);
 		} finally {
 			context.ungetService(reference);
 		}
 	}
 
 	private IArtifactRepository initializeArtifactRepository(BundleContext context, String repositoryName, URL stateDirURL, boolean hidden) {
 		ServiceReference reference = context.getServiceReference(IArtifactRepositoryManager.class.getName());
 		IArtifactRepositoryManager manager = null;
 		if (reference != null)
 			manager = (IArtifactRepositoryManager) context.getService(reference);
 		if (manager == null)
 			throw new IllegalStateException("ArtifactRepositoryManager not registered."); //$NON-NLS-1$
 
 		try {
 			try {
 				return manager.loadRepository(stateDirURL, null);
 			} catch (ProvisionException e) {
 				//fall through and create a new repository
 			}
 			try {
 				IArtifactRepository repository;
 				if (hidden) {
 					repository = manager.createRepository(stateDirURL, "artifact listener " + repositoryName, IArtifactRepositoryManager.TYPE_SIMPLE_REPOSITORY);
 					manager.addRepository(repository.getLocation());
 					repository.setProperty(IRepository.PROP_SYSTEM, Boolean.TRUE.toString());
 				} else {
 					repository = manager.createRepository(stateDirURL, repositoryName, IArtifactRepositoryManager.TYPE_SIMPLE_REPOSITORY);
 					manager.addRepository(repository.getLocation());
 				}
 				return repository;
 			} catch (ProvisionException e) {
 				LogHelper.log(e);
 				throw new IllegalStateException("Couldn't create artifact repository for: " + stateDirURL);
 			}
 		} finally {
 			context.ungetService(reference);
 		}
 	}
 
 	private IMetadataRepository initializeMetadataRepository(BundleContext context, String repositoryName, URL stateDirURL, boolean hidden) {
 		ServiceReference reference = context.getServiceReference(IMetadataRepositoryManager.class.getName());
 		IMetadataRepositoryManager manager = null;
 		if (reference != null)
 			manager = (IMetadataRepositoryManager) context.getService(reference);
 		if (manager == null)
 			throw new IllegalStateException("MetadataRepositoryManager not registered.");
 
 		try {
 			try {
 				return manager.loadRepository(stateDirURL, null);
 			} catch (ProvisionException e) {
 				//fall through and create new repository
 			}
 			IMetadataRepository repository;
 			if (hidden) {
 				repository = manager.createRepository(stateDirURL, "Metadata listener " + repositoryName, IMetadataRepositoryManager.TYPE_SIMPLE_REPOSITORY);
 				repository.setProperty(IRepository.PROP_SYSTEM, Boolean.TRUE.toString());
 			} else {
 				repository = manager.createRepository(stateDirURL, repositoryName, IMetadataRepositoryManager.TYPE_SIMPLE_REPOSITORY);
 			}
 			manager.addRepository(stateDirURL);
 			return repository;
 		} catch (ProvisionException e) {
 			LogHelper.log(e);
 			throw new IllegalStateException("Couldn't create metadata repository for: " + stateDirURL);
 		} finally {
 			context.ungetService(reference);
 		}
 	}
 
 	/* (non-Javadoc)
 	 * @see org.eclipse.equinox.internal.provisional.p2.directorywatcher.IDirectoryChangeListener#added(java.io.File)
 	 */
 	public boolean added(File file) {
 		currentFiles.put(file, new Long(file.lastModified()));
 		return true;
 	}
 
 	/* (non-Javadoc)
 	 * @see org.eclipse.equinox.internal.provisional.p2.directorywatcher.IDirectoryChangeListener#changed(java.io.File)
 	 */
 	public boolean changed(File file) {
 		currentFiles.put(file, new Long(file.lastModified()));
 		return true;
 	}
 
 	/* (non-Javadoc)
 	 * @see org.eclipse.equinox.internal.provisional.p2.directorywatcher.IDirectoryChangeListener#removed(java.io.File)
 	 */
 	public boolean removed(File file) {
 		// note that we can't call #isInteresting here because we can't tell if the file handle
 		// points to a directory because its already been removed.
 		currentFiles.remove(file);
 		return true;
 	}
 
 	private boolean isBundle(File file) {
 		if (file.isDirectory() || file.getName().endsWith(".jar")) {
 			BundleDescription bundleDescription = bundleDescriptionFactory.getBundleDescription(file);
 			return bundleDescription != null;
 		}
 		return false;
 	}
 
	private boolean isFeature(File file) {
 		return file.isDirectory() && new File(file, "feature.xml").exists();
 	}
 
 	/* (non-Javadoc)
 	 * @see org.eclipse.equinox.internal.provisional.p2.directorywatcher.DirectoryChangeListener#isInterested(java.io.File)
 	 */
 	public boolean isInterested(File file) {
 		return isFeature(file) || isBundle(file);
 	}
 
 	/* (non-Javadoc)
 	 * @see org.eclipse.equinox.internal.provisional.p2.directorywatcher.IDirectoryChangeListener#getSeenFile(java.io.File)
 	 */
 	public Long getSeenFile(File file) {
 		return (Long) currentFiles.get(file);
 	}
 
 	/* (non-Javadoc)
 	 * @see org.eclipse.equinox.internal.provisional.p2.directorywatcher.IDirectoryChangeListener#startPoll()
 	 */
 	public void startPoll() {
 		// do nothing
 	}
 
 	/* (non-Javadoc)
 	 * @see org.eclipse.equinox.internal.provisional.p2.directorywatcher.IDirectoryChangeListener#stopPoll()
 	 */
 	public void stopPoll() {
 		if (metadataRepository != null)
 			synchronizeMetadataRepository();
 
 		if (artifactRepository != null)
 			synchronizeArtifactRepository();
 	}
 
 	private void synchronizeMetadataRepository() {
 		final Map snapshot = new HashMap(currentFiles);
 		Query removeQuery = new Query() {
 			public boolean isMatch(Object candidate) {
 				if (!(candidate instanceof IInstallableUnit))
 					return false;
 				IInstallableUnit iu = (IInstallableUnit) candidate;
 				File iuFile = new File(iu.getProperty(FILE_NAME));
 				Long iuLastModified = new Long(iu.getProperty(FILE_LAST_MODIFIED));
 				Long snapshotLastModified = (Long) snapshot.get(iuFile);
 				if (snapshotLastModified == null || !snapshotLastModified.equals(iuLastModified))
 					return true;
 
				// match found. Remove from snapshot to prevent it from being added.
 				snapshot.remove(iuFile);
 				return false;
 			}
 		};
 		metadataRepository.removeInstallableUnits(removeQuery, null);
 
 		if (!snapshot.isEmpty()) {
 			IInstallableUnit[] iusToAdd = generateIUs(snapshot.keySet());
 			metadataRepository.addInstallableUnits(iusToAdd);
 		}
 	}
 
 	private void synchronizeArtifactRepository() {
 		final Map snapshot = new HashMap(currentFiles);
 		final List keys = new ArrayList(Arrays.asList(artifactRepository.getArtifactKeys()));
 
 		for (Iterator it = keys.iterator(); it.hasNext();) {
 			IArtifactKey key = (IArtifactKey) it.next();
 			IArtifactDescriptor[] descriptors = artifactRepository.getArtifactDescriptors(key);
 			for (int i = 0; i < descriptors.length; i++) {
 				ArtifactDescriptor descriptor = (ArtifactDescriptor) descriptors[i];
 				File artifactFile = new File(descriptor.getRepositoryProperty(FILE_NAME));
 				Long artifactLastModified = new Long(descriptor.getRepositoryProperty(FILE_LAST_MODIFIED));
 				Long snapshotLastModified = (Long) snapshot.get(artifactFile);
 				if (snapshotLastModified == null || !snapshotLastModified.equals(artifactLastModified))
 					artifactRepository.removeDescriptor(descriptor);
 				else
 					snapshot.remove(key);
 			}
 		}
 
 		for (Iterator it = snapshot.keySet().iterator(); it.hasNext();) {
 			File file = (File) it.next();
 			IArtifactDescriptor descriptor = generateArtifactDescriptor(file);
 			if (descriptor != null)
 				artifactRepository.addDescriptor(descriptor);
 		}
 	}
 
 	protected IArtifactDescriptor generateArtifactDescriptor(File candidate) {
 
 		IArtifactDescriptor basicDescriptor = generateBasicDescriptor(candidate);
 		ArtifactDescriptor pathDescriptor = new ArtifactDescriptor(basicDescriptor);
 		try {
 			pathDescriptor.setRepositoryProperty(ARTIFACT_REFERENCE, candidate.toURL().toExternalForm());
 		} catch (MalformedURLException e) {
 			// unexpected
 			e.printStackTrace();
 			return null;
 		}
 		if (candidate.isDirectory())
 			pathDescriptor.setRepositoryProperty(ARTIFACT_FOLDER, Boolean.TRUE.toString());
 
 		pathDescriptor.setRepositoryProperty(FILE_NAME, candidate.getAbsolutePath());
 		pathDescriptor.setRepositoryProperty(FILE_LAST_MODIFIED, Long.toString(candidate.lastModified()));
 
 		return pathDescriptor;
 	}
 
 	private IArtifactDescriptor generateBasicDescriptor(File candidate) {
 
 		if (isFeature(candidate)) {
 			FeatureParser parser = new FeatureParser();
 			Feature feature = parser.parse(candidate);
 			IArtifactKey featureKey = MetadataGeneratorHelper.createFeatureArtifactKey(feature.getId(), feature.getVersion());
 			return new ArtifactDescriptor(featureKey);
 		}
 
 		BundleDescription bundleDescription = bundleDescriptionFactory.getBundleDescription(candidate);
 		IArtifactKey key = MetadataGeneratorHelper.createBundleArtifactKey(bundleDescription.getSymbolicName(), bundleDescription.getVersion().toString());
 		return MetadataGeneratorHelper.createArtifactDescriptor(key, candidate, true, false);
 	}
 
 	private IInstallableUnit[] generateIUs(Collection files) {
 		List ius = new ArrayList();
 		for (Iterator it = files.iterator(); it.hasNext();) {
 			File candidate = (File) it.next();
 
 			Properties props = new Properties();
 			props.setProperty(FILE_NAME, candidate.getAbsolutePath());
 			props.setProperty(FILE_LAST_MODIFIED, Long.toString(candidate.lastModified()));
 
 			if (isFeature(candidate)) {
 				IInstallableUnit[] featureIUs = generateFeatureIUs(candidate, props);
 				if (featureIUs != null)
 					ius.addAll(Arrays.asList(featureIUs));
 			} else {
 				IInstallableUnit[] bundleIUs = generateBundleIU(candidate, props);
 				if (bundleIUs != null) {
 					for (int i = 0; i < bundleIUs.length; i++) {
 						ius.add(bundleIUs[i]);
 					}
 				}
 			}
 		}
 		return (IInstallableUnit[]) ius.toArray(new IInstallableUnit[ius.size()]);
 	}
 
 	private IInstallableUnit[] generateFeatureIUs(File featureFile, Properties props) {
 
 		FeatureParser parser = new FeatureParser();
 		Feature feature = parser.parse(featureFile);
 
 		IInstallableUnit featureIU = MetadataGeneratorHelper.createFeatureJarIU(feature, true, props);
 		IInstallableUnit groupIU = MetadataGeneratorHelper.createGroupIU(feature, featureIU, props);
 
 		return new IInstallableUnit[] {featureIU, groupIU};
 	}
 
 	private IInstallableUnit[] generateBundleIU(File bundleFile, Properties props) {
 
 		BundleDescription bundleDescription = bundleDescriptionFactory.getBundleDescription(bundleFile);
 		if (bundleDescription == null)
 			return null;
 
 		IArtifactKey key = MetadataGeneratorHelper.createBundleArtifactKey(bundleDescription.getSymbolicName(), bundleDescription.getVersion().toString());
 		IInstallableUnit[] ius = MetadataGeneratorHelper.createEclipseIU(bundleDescription, (Map) bundleDescription.getUserObject(), false, key, props);
 		return ius;
 	}
 
 	public IMetadataRepository getMetadataRepository() {
 		return metadataRepository;
 	}
 
 	public IArtifactRepository getArtifactRepository() {
 		return artifactRepository;
 	}
 }
