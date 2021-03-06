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
 
 import java.net.URISyntaxException;
 import java.util.*;
 import org.eclipse.core.runtime.*;
 import org.eclipse.equinox.app.IApplication;
 import org.eclipse.equinox.app.IApplicationContext;
 import org.eclipse.equinox.internal.p2.core.helpers.ServiceHelper;
 import org.eclipse.equinox.internal.p2.engine.DownloadManager;
 import org.eclipse.equinox.internal.provisional.p2.artifact.repository.IArtifactRepositoryManager;
 import org.eclipse.equinox.internal.provisional.p2.artifact.repository.IArtifactRequest;
 import org.eclipse.equinox.internal.provisional.p2.core.ProvisionException;
 import org.eclipse.equinox.internal.provisional.p2.engine.*;
 import org.eclipse.equinox.internal.provisional.p2.engine.phases.Collect;
 import org.eclipse.equinox.internal.provisional.p2.metadata.IArtifactKey;
 import org.eclipse.equinox.internal.provisional.p2.metadata.IInstallableUnit;
 import org.eclipse.equinox.internal.provisional.p2.metadata.query.Collector;
 import org.eclipse.equinox.internal.provisional.p2.metadata.query.InstallableUnitQuery;
 import org.eclipse.equinox.internal.provisional.p2.metadata.repository.IMetadataRepository;
 
 /**
  * The transformer takes an existing p2 repository (local or remote), iterates over 
  * its list of IUs, and fetches all of the corresponding artifacts to a user-specified location. 
  * Once fetched, the artifacts will be in "runnable" form... that is directory-based bundles will be
  * extracted into folders and packed JAR files will be un-packed.
  * 
  * @since 1.0
  */
 public class Repo2Runnable extends AbstractApplication implements IApplication {
 	private static final String NATIVE_ARTIFACTS = "nativeArtifacts"; //$NON-NLS-1$
 	private static final String NATIVE_TYPE = "org.eclipse.equinox.p2.native"; //$NON-NLS-1$
 	private static final String PARM_OPERAND = "operand"; //$NON-NLS-1$
 
 	protected class CollectNativesAction extends ProvisioningAction {
 		public IStatus execute(Map parameters) {
 			InstallableUnitOperand operand = (InstallableUnitOperand) parameters.get(PARM_OPERAND);
 			IInstallableUnit installableUnit = operand.second();
 
 			IArtifactRepositoryManager manager = null;
 			try {
 				manager = Activator.getArtifactRepositoryManager();
 			} catch (ProvisionException e) {
 				return e.getStatus();
 			}
 
 			IArtifactKey[] toDownload = installableUnit.getArtifacts();
 			if (toDownload == null)
 				return Status.OK_STATUS;
 
 			List artifactRequests = (List) parameters.get(NATIVE_ARTIFACTS);
 
 			for (int i = 0; i < toDownload.length; i++) {
 				IArtifactRequest request = manager.createMirrorRequest(toDownload[i], destinationArtifactRepository, null, null);
 				artifactRequests.add(request);
 			}
 			return Status.OK_STATUS;
 		}
 
 		public IStatus undo(Map parameters) {
 			// nothing to do for now
 			return Status.OK_STATUS;
 		}
 	}
 
 	protected class CollectNativesPhase extends InstallableUnitPhase {
 		public CollectNativesPhase(int weight) {
 			super(NATIVE_ARTIFACTS, weight);
 		}
 
 		protected ProvisioningAction[] getActions(InstallableUnitOperand operand) {
 			IInstallableUnit unit = operand.second();
 			if (unit.getTouchpointType().getId().equals(NATIVE_TYPE)) {
 				return new ProvisioningAction[] {new CollectNativesAction()};
 			}
 			return null;
 		}
 
 		protected IStatus initializePhase(IProgressMonitor monitor, IProfile profile, Map parameters) {
 			parameters.put(NATIVE_ARTIFACTS, new ArrayList());
 			return null;
 		}
 
 		protected IStatus completePhase(IProgressMonitor monitor, IProfile profile, Map parameters) {
 			List artifactRequests = (List) parameters.get(NATIVE_ARTIFACTS);
 			ProvisioningContext context = (ProvisioningContext) parameters.get(PARM_CONTEXT);
 
 			DownloadManager dm = new DownloadManager(context);
 			for (Iterator it = artifactRequests.iterator(); it.hasNext();) {
 				dm.add((IArtifactRequest) it.next());
 			}
 			return dm.start(monitor);
 		}
 	}
 
 	// the list of IUs that we actually transformed... could have come from the repo 
 	// or have been user-specified.
 	private Collection processedIUs = new ArrayList();
 
 	/*
 	 * Perform the transformation.
 	 */
 	public IStatus run(IProgressMonitor monitor) throws ProvisionException {
 		SubMonitor progress = SubMonitor.convert(monitor, 5);
 
 		initializeRepos(progress);
 
 		// ensure all the right parameters are set
 		validate();
 
 		// figure out which IUs we need to process
 		collectIUs(progress.newChild(1));
 
 		// create the operands from the list of IUs
 		InstallableUnitOperand[] operands = new InstallableUnitOperand[processedIUs.size()];
 		int i = 0;
 		for (Iterator iter = processedIUs.iterator(); iter.hasNext();)
 			operands[i++] = new InstallableUnitOperand(null, (IInstallableUnit) iter.next());
 
 		// call the engine with only the "collect" phase so all we do is download
 		IProfile profile = createProfile();
 		try {
 			ProvisioningContext context = new ProvisioningContext();
 			IEngine engine = (IEngine) ServiceHelper.getService(Activator.getBundleContext(), IEngine.SERVICE_NAME);
 			if (engine == null)
 				throw new ProvisionException(Messages.exception_noEngineService);
 
 			IStatus result = engine.perform(profile, getPhaseSet(), operands, context, progress.newChild(1));
 			PhaseSet nativeSet = getNativePhase();
 			if (nativeSet != null)
				engine.perform(profile, nativeSet, operands, context, progress.newChild(1));
 
 			// publish the metadata to a destination - if requested
 			publishMetadata(progress.newChild(1));
 
 			// return the resulting status
 			return result;
 		} finally {
 			// cleanup by removing the temporary profile and unloading the repos which were new
 			removeProfile(profile);
 			finalizeRepositories();
 		}
 	}
 
 	protected PhaseSet getPhaseSet() {
 		return new PhaseSet(new Phase[] {new Collect(100)}) { /* nothing to override */};
 	}
 
 	protected PhaseSet getNativePhase() {
 		return new PhaseSet(new Phase[] {new CollectNativesPhase(100)}) { /*nothing to override */};
 	}
 
 	/*
 	 * Figure out exactly which IUs we have to process.
 	 */
 	private void collectIUs(IProgressMonitor monitor) throws ProvisionException {
 		// if the user told us exactly which IUs to process, then just set it and return.
 		if (sourceIUs != null && !sourceIUs.isEmpty()) {
 			processedIUs = sourceIUs;
 			return;
 		}
 		// get all IUs from the repos
 		if (!hasMetadataSources())
 			throw new ProvisionException(Messages.exception_needIUsOrNonEmptyRepo);
 
 		processedIUs.addAll(getAllIUs(getCompositeMetadataRepository(), monitor).toCollection());
 
 		if (processedIUs.isEmpty())
 			throw new ProvisionException(Messages.exception_needIUsOrNonEmptyRepo);
 	}
 
 	/*
 	 * If there is a destination metadata repository set, then add all our transformed
 	 * IUs to it. 
 	 */
 	private void publishMetadata(IProgressMonitor monitor) {
 		// publishing the metadata is optional
 		if (destinationMetadataRepository == null)
 			return;
 		destinationMetadataRepository.addInstallableUnits((IInstallableUnit[]) processedIUs.toArray(new IInstallableUnit[processedIUs.size()]));
 	}
 
 	/*
 	 * Return a collector over all the IUs contained in the given repository.
 	 */
 	private Collector getAllIUs(IMetadataRepository repository, IProgressMonitor monitor) {
 		SubMonitor progress = SubMonitor.convert(monitor, 2);
 		try {
 			return repository.query(InstallableUnitQuery.ANY, new Collector(), progress.newChild(1));
 		} finally {
 			progress.done();
 		}
 	}
 
 	/*
 	 * Remove the given profile from the profile registry.
 	 */
 	private void removeProfile(IProfile profile) throws ProvisionException {
 		IProfileRegistry registry = Activator.getProfileRegistry();
 		registry.removeProfile(profile.getProfileId());
 	}
 
 	/*
 	 * Create and return a new profile.
 	 */
 	private IProfile createProfile() throws ProvisionException {
 		Map properties = new Properties();
 		properties.put(IProfile.PROP_CACHE, URIUtil.toFile(destinationArtifactRepository.getLocation()).getAbsolutePath());
 		properties.put(IProfile.PROP_INSTALL_FOLDER, URIUtil.toFile(destinationArtifactRepository.getLocation()).getAbsolutePath());
 		IProfileRegistry registry = Activator.getProfileRegistry();
 		return registry.addProfile(System.currentTimeMillis() + "-" + Math.random(), properties); //$NON-NLS-1$
 	}
 
 	/* (non-Javadoc)
 	 * @see org.eclipse.equinox.app.IApplication#start(org.eclipse.equinox.app.IApplicationContext)
 	 */
 	public Object start(IApplicationContext context) throws Exception {
 		String[] args = (String[]) context.getArguments().get(IApplicationContext.APPLICATION_ARGS);
 		processCommandLineArgs(args);
 		// perform the transformation
 		run(null);
 		return IApplication.EXIT_OK;
 	}
 
 	/*
 	 * Iterate over the command-line arguments and prepare the transformer for processing.
 	 */
 	private void processCommandLineArgs(String[] args) throws URISyntaxException {
 		if (args == null)
 			return;
 		for (int i = 0; i < args.length; i++) {
 			String option = args[i];
 			if (i == args.length - 1 || args[i + 1].startsWith("-")) //$NON-NLS-1$
 				continue;
 			String arg = args[++i];
 
 			if (option.equalsIgnoreCase("-source")) { //$NON-NLS-1$
 				RepositoryDescriptor source = new RepositoryDescriptor();
 				source.setLocation(URIUtil.fromString(arg));
 				addSource(source);
 			}
 
 			if (option.equalsIgnoreCase("-destination")) { //$NON-NLS-1$
 				RepositoryDescriptor destination = new RepositoryDescriptor();
 				destination.setLocation(URIUtil.fromString(arg));
 				addDestination(destination);
 			}
 		}
 	}
 
 	/*
 	 * Ensure all mandatory parameters have been set. Throw an exception if there
 	 * are any missing. We don't require the user to specify the artifact repository here,
 	 * we will default to the ones already registered in the manager. (callers are free
 	 * to add more if they wish)
 	 */
 	private void validate() throws ProvisionException {
 		if (!hasMetadataSources() && sourceIUs == null)
 			throw new ProvisionException(Messages.exception_needIUsOrNonEmptyRepo);
 		if (destinationArtifactRepository == null)
 			throw new ProvisionException(Messages.exception_needDestinationRepo);
 	}
 
 	/* (non-Javadoc)
 	 * @see org.eclipse.equinox.app.IApplication#stop()
 	 */
 	public void stop() {
 		// nothing to do
 	}
 }
