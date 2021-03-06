 /*******************************************************************************
  * Copyright (c) 2007, 2009 IBM Corporation and others.
  * All rights reserved. This program and the accompanying materials
  * are made available under the terms of the Eclipse Public License v1.0
  * which accompanies this distribution, and is available at
  * http://www.eclipse.org/legal/epl-v10.html
  *
  * Contributors:
  *     IBM Corporation - initial API and implementation
  *     Cloudsmith - https://bugs.eclipse.org/bugs/show_bug.cgi?id=226401
  *     EclipseSource - ongoing development
  *******************************************************************************/
 package org.eclipse.equinox.internal.p2.director.app;
 
 import java.io.File;
 import java.net.URI;
 import java.net.URISyntaxException;
 import java.util.*;
 import org.eclipse.core.runtime.*;
 import org.eclipse.equinox.app.IApplication;
 import org.eclipse.equinox.app.IApplicationContext;
 import org.eclipse.equinox.internal.p2.console.ProvisioningHelper;
 import org.eclipse.equinox.internal.p2.core.helpers.LogHelper;
 import org.eclipse.equinox.internal.p2.core.helpers.ServiceHelper;
 import org.eclipse.equinox.internal.provisional.p2.artifact.repository.IArtifactRepositoryManager;
 import org.eclipse.equinox.internal.provisional.p2.core.*;
 import org.eclipse.equinox.internal.provisional.p2.core.Version;
 import org.eclipse.equinox.internal.provisional.p2.director.*;
 import org.eclipse.equinox.internal.provisional.p2.engine.*;
import org.eclipse.equinox.internal.provisional.p2.engine.phases.Property;
 import org.eclipse.equinox.internal.provisional.p2.metadata.IInstallableUnit;
 import org.eclipse.equinox.internal.provisional.p2.metadata.query.InstallableUnitQuery;
 import org.eclipse.equinox.internal.provisional.p2.metadata.query.LatestIUVersionQuery;
 import org.eclipse.equinox.internal.provisional.p2.metadata.repository.IMetadataRepositoryManager;
 import org.eclipse.equinox.internal.provisional.p2.query.*;
 import org.eclipse.osgi.util.NLS;
 import org.osgi.framework.*;
 import org.osgi.service.packageadmin.PackageAdmin;
 
 public class Application implements IApplication {
 	private static final Integer EXIT_ERROR = new Integer(13);
 	static private final String ANT_PROPERTY_PREFIX = "${"; //$NON-NLS-1$
 	static private final String FLAVOR_DEFAULT = "tooling"; //$NON-NLS-1$
 	static private final String EXEMPLARY_SETUP = "org.eclipse.equinox.p2.exemplarysetup"; //$NON-NLS-1$
 	static private final String FRAMEWORKADMIN_EQUINOX = "org.eclipse.equinox.frameworkadmin.equinox"; //$NON-NLS-1$
 	static private final String SIMPLE_CONFIGURATOR_MANIPULATOR = "org.eclipse.equinox.simpleconfigurator.manipulator"; //$NON-NLS-1$
 
 	public static final int COMMAND_INSTALL = 0;
 	public static final int COMMAND_UNINSTALL = 1;
 	public static final int COMMAND_LIST = 2;
 
 	public static final String[] COMMAND_NAMES = {"-installIU", "-uninstallIU", "-list"}; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
 
 	private Path destination;
 
 	private URI[] artifactRepositoryLocations;
 	private URI[] metadataRepositoryLocations;
 
 	private URI[] metadataReposForRemoval;
 	private URI[] artifactReposForRemoval;
 	private IArtifactRepositoryManager artifactManager;
 	private IMetadataRepositoryManager metadataManager;
 
 	private String root;
 	private Version version = null;
 	private String flavor;
 	private String profileId;
 	private String profileProperties; // a comma-separated list of property pairs "tag=value"
 	private String bundlePool = null;
 	private String nl;
 	private String os;
 	private String arch;
 	private String ws;
 	private boolean roamingProfile = false;
 	private IPlanner planner;
 	private IEngine engine;
 
 	private int command = -1;
 
 	private ServiceReference packageAdminRef;
 	private PackageAdmin packageAdmin;
 
 	private void ambigousCommand(int cmd1, int cmd2) throws CoreException {
 		throw new CoreException(new Status(IStatus.ERROR, Activator.ID, NLS.bind(Messages.Ambigous_Command, new Object[] {COMMAND_NAMES[cmd1], COMMAND_NAMES[cmd2]})));
 	}
 
 	private ProfileChangeRequest buildProvisioningRequest(IProfile profile, Collector roots, boolean install) {
 		ProfileChangeRequest request = new ProfileChangeRequest(profile);
 		markRoots(request, roots);
 		if (install) {
 			request.addInstallableUnits((IInstallableUnit[]) roots.toArray(IInstallableUnit.class));
 		} else {
 			request.removeInstallableUnits((IInstallableUnit[]) roots.toArray(IInstallableUnit.class));
 		}
 		return request;
 	}
 
 	synchronized Bundle getBundle(String symbolicName) {
 		if (packageAdmin == null)
 			return null;
 
 		Bundle[] bundles = packageAdmin.getBundles(symbolicName, null);
 		if (bundles == null)
 			return null;
 		//Return the first bundle that is not installed or uninstalled
 		for (int i = 0; i < bundles.length; i++) {
 			if ((bundles[i].getState() & (Bundle.INSTALLED | Bundle.UNINSTALLED)) == 0) {
 				return bundles[i];
 			}
 		}
 		return null;
 	}
 
 	private String getEnvironmentProperty() {
 		Properties values = new Properties();
 		if (os != null)
 			values.put("osgi.os", os); //$NON-NLS-1$
 		if (nl != null)
 			values.put("osgi.nl", nl); //$NON-NLS-1$
 		if (ws != null)
 			values.put("osgi.ws", ws); //$NON-NLS-1$
 		if (arch != null)
 			values.put("osgi.arch", arch); //$NON-NLS-1$
 		if (values.isEmpty())
 			return null;
 		return toString(values);
 	}
 
 	private IProfile initializeProfile() throws CoreException {
 		if (profileId == null)
 			profileId = IProfileRegistry.SELF;
 		IProfile profile = ProvisioningHelper.getProfile(profileId);
 		if (profile == null) {
 			if (destination == null)
 				missingArgument("destination"); //$NON-NLS-1$
 			if (flavor == null)
 				flavor = System.getProperty("eclipse.p2.configurationFlavor", FLAVOR_DEFAULT); //$NON-NLS-1$
 
 			Properties props = new Properties();
 			props.setProperty(IProfile.PROP_INSTALL_FOLDER, destination.toOSString());
 			props.setProperty(IProfile.PROP_FLAVOR, flavor);
 			if (bundlePool != null)
 				if (bundlePool.equals(Messages.destination_commandline))
 					props.setProperty(IProfile.PROP_CACHE, destination.toOSString());
 				else
 					props.setProperty(IProfile.PROP_CACHE, bundlePool);
 			if (roamingProfile)
 				props.setProperty(IProfile.PROP_ROAMING, Boolean.TRUE.toString());
 
 			String env = getEnvironmentProperty();
 			if (env != null)
 				props.setProperty(IProfile.PROP_ENVIRONMENTS, env);
 			if (profileProperties != null) {
 				putProperties(profileProperties, props);
 			}
 			profile = ProvisioningHelper.addProfile(profileId, props);
 			String currentFlavor = profile.getProperty(IProfile.PROP_FLAVOR);
 			if (currentFlavor != null && !currentFlavor.endsWith(flavor))
 				throw new RuntimeException(Messages.Inconsistent_flavor);
 		}
 		return profile;
 	}
 
 	private void initializeRepositories(boolean throwException) throws CoreException {
 		if (artifactRepositoryLocations == null) {
 			if (throwException)
 				missingArgument("artifactRepository"); //$NON-NLS-1$
 		} else {
 			artifactManager = (IArtifactRepositoryManager) ServiceHelper.getService(Activator.getContext(), IArtifactRepositoryManager.class.getName());
 			if (artifactManager == null) {
 				if (throwException)
 					throw new ProvisionException(Messages.Application_NoManager);
 			} else {
 				int removalIdx = 0;
 				boolean anyValid = false; // do we have any valid repos or did they all fail to load?
 				artifactReposForRemoval = new URI[artifactRepositoryLocations.length];
 				for (int i = 0; i < artifactRepositoryLocations.length; i++) {
 					try {
 						if (!artifactManager.contains(artifactRepositoryLocations[i])) {
 							artifactManager.loadRepository(artifactRepositoryLocations[i], null);
 							artifactReposForRemoval[removalIdx++] = artifactRepositoryLocations[i];
 						}
 						anyValid = true;
 					} catch (ProvisionException e) {
 						//one of the repositories did not load
 						LogHelper.log(new Status(IStatus.ERROR, Activator.ID, artifactRepositoryLocations[i].toString() + " failed to load", e)); //$NON-NLS-1$
 					}
 				}
 				if (throwException && !anyValid)
 					//all repositories failed to load
 					throw new ProvisionException(Messages.Application_NoRepositories);
 			}
 		}
 
 		if (metadataRepositoryLocations == null) {
 			if (throwException)
 				missingArgument("metadataRepository"); //$NON-NLS-1$
 		} else {
 			metadataManager = (IMetadataRepositoryManager) ServiceHelper.getService(Activator.getContext(), IMetadataRepositoryManager.class.getName());
 			if (metadataManager == null) {
 				if (throwException)
 					throw new ProvisionException(Messages.Application_NoManager);
 			} else {
 				int removalIdx = 0;
 				boolean anyValid = false; // do we have any valid repos or did they all fail to load?
 				metadataReposForRemoval = new URI[metadataRepositoryLocations.length];
 				for (int i = 0; i < metadataRepositoryLocations.length; i++) {
 					try {
 						if (!metadataManager.contains(metadataRepositoryLocations[i])) {
 							metadataManager.loadRepository(metadataRepositoryLocations[i], null);
 							metadataReposForRemoval[removalIdx++] = metadataRepositoryLocations[i];
 						}
 						anyValid = true;
 					} catch (ProvisionException e) {
 						//one of the repositories did not load
 						LogHelper.log(new Status(IStatus.ERROR, Activator.ID, metadataRepositoryLocations[i].toString() + " failed to load", e)); //$NON-NLS-1$
 					}
 				}
 				if (throwException && !anyValid)
 					//all repositories failed to load
 					throw new ProvisionException(Messages.Application_NoRepositories);
 			}
 		}
 	}
 
 	private void initializeServices() {
 		IDirector director = (IDirector) ServiceHelper.getService(Activator.getContext(), IDirector.class.getName());
 		if (director == null)
 			throw new RuntimeException(Messages.Missing_director);
 
 		planner = (IPlanner) ServiceHelper.getService(Activator.getContext(), IPlanner.class.getName());
 		if (planner == null)
 			throw new RuntimeException(Messages.Missing_planner);
 
 		engine = (IEngine) ServiceHelper.getService(Activator.getContext(), IEngine.SERVICE_NAME);
 		if (engine == null)
 			throw new RuntimeException(Messages.Missing_Engine);
 	}
 
 	private void markRoots(ProfileChangeRequest request, Collector roots) {
 		for (Iterator iterator = roots.iterator(); iterator.hasNext();) {
 			request.setInstallableUnitProfileProperty((IInstallableUnit) iterator.next(), IInstallableUnit.PROP_PROFILE_ROOT_IU, Boolean.TRUE.toString());
 		}
 	}
 
 	private void missingArgument(String argumentName) throws CoreException {
 		throw new CoreException(new Status(IStatus.ERROR, Activator.ID, NLS.bind(Messages.Missing_Required_Argument, argumentName)));
 	}
 
 	private IStatus planAndExecute(IProfile profile, ProvisioningContext context, ProfileChangeRequest request) {
 		ProvisioningPlan result;
 		IStatus operationStatus;
 		result = planner.getProvisioningPlan(request, context, new NullProgressMonitor());
 		if (!result.getStatus().isOK())
 			operationStatus = result.getStatus();
 		else {
 			operationStatus = engine.perform(profile, new DefaultPhaseSet(), result.getOperands(), context, new NullProgressMonitor());
 		}
 		return operationStatus;
 	}
 
 	private void printRequest(ProfileChangeRequest request) {
 		IInstallableUnit[] toAdd = request.getAddedInstallableUnits();
 		IInstallableUnit[] toRemove = request.getRemovedInstallableUnits();
 		for (int i = 0; i < toAdd.length; i++) {
 			System.out.println(NLS.bind(Messages.Installing, toAdd[i].getId(), toAdd[i].getVersion()));
 		}
 		for (int i = 0; i < toRemove.length; i++) {
 			System.out.println(NLS.bind(Messages.Uninstalling, toRemove[i].getId(), toRemove[i].getVersion()));
 		}
 	}
 
 	public void processArguments(String[] args) throws Exception {
 		if (args == null)
 			return;
 		for (int i = 0; i < args.length; i++) {
 
 			String opt = args[i];
 			if (opt.equals("-roaming")) { //$NON-NLS-1$
 				roamingProfile = true;
 			}
 
 			if (opt.equals(COMMAND_NAMES[COMMAND_LIST])) {
 				if (command != -1)
 					ambigousCommand(COMMAND_LIST, command);
 				command = COMMAND_LIST;
 			}
 
 			// check for args without parameters (i.e., a flag arg)
 
 			// check for args with parameters. If we are at the last
 			// argument or
 			// if the next one
 			// has a '-' as the first character, then we can't have an arg
 			// with
 			// a parm so continue.
 			if (i == args.length - 1 || args[i + 1].startsWith("-")) //$NON-NLS-1$
 				continue;
 
 			String arg = args[++i];
 
 			if (opt.equalsIgnoreCase("-profile")) //$NON-NLS-1$
 				profileId = arg;
 
 			if (opt.equalsIgnoreCase("-profileProperties") || opt.equalsIgnoreCase("-props")) //$NON-NLS-1$ //$NON-NLS-2$
 				profileProperties = arg;
 
 			// we create a path object here to handle ../ entries in the middle of paths
 			if (opt.equalsIgnoreCase("-destination") || opt.equalsIgnoreCase("-dest")) //$NON-NLS-1$ //$NON-NLS-2$
 				destination = new Path(arg);
 
 			// we create a path object here to handle ../ entries in the middle of paths
 			if (opt.equalsIgnoreCase("-bundlepool") || opt.equalsIgnoreCase("-bp")) //$NON-NLS-1$ //$NON-NLS-2$
 				bundlePool = new Path(arg).toOSString();
 
 			if (opt.equalsIgnoreCase("-metadataRepository") || opt.equalsIgnoreCase("-metadataRepositories") || opt.equalsIgnoreCase("-mr")) //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
 				metadataRepositoryLocations = getURIs(arg);
 
 			if (opt.equalsIgnoreCase("-artifactRepository") || opt.equalsIgnoreCase("-artifactRepositories") || opt.equalsIgnoreCase("-ar")) //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
 				artifactRepositoryLocations = getURIs(arg);
 
 			if (opt.equalsIgnoreCase("-flavor")) //$NON-NLS-1$
 				flavor = arg;
 
 			if (opt.equalsIgnoreCase(COMMAND_NAMES[COMMAND_INSTALL])) {
 				if (command != -1)
 					ambigousCommand(COMMAND_INSTALL, command);
 				root = arg;
 				command = COMMAND_INSTALL;
 			}
 
 			if (opt.equalsIgnoreCase("-version")) { //$NON-NLS-1$
 				if (arg != null && !arg.startsWith(ANT_PROPERTY_PREFIX))
 					version = new Version(arg);
 			}
 
 			if (opt.equalsIgnoreCase(COMMAND_NAMES[COMMAND_UNINSTALL])) {
 				if (command != -1)
 					ambigousCommand(COMMAND_UNINSTALL, command);
 				root = arg;
 				command = COMMAND_UNINSTALL;
 			}
 
 			if (opt.equalsIgnoreCase("-p2.os")) { //$NON-NLS-1$
 				os = arg;
 			}
 			if (opt.equalsIgnoreCase("-p2.ws")) { //$NON-NLS-1$
 				ws = arg;
 			}
 			if (opt.equalsIgnoreCase("-p2.nl")) { //$NON-NLS-1$
 				nl = arg;
 			}
 			if (opt.equalsIgnoreCase("-p2.arch")) { //$NON-NLS-1$
 				arch = arg;
 			}
 		}
 
 	}
 
 	/**
 	 * @param pairs	a comma separated list of tag=value pairs
 	 * @param properties the collection into which the pairs are put
 	 */
 	private void putProperties(String pairs, Properties properties) {
 		StringTokenizer tok = new StringTokenizer(pairs, ",", true); //$NON-NLS-1$
 		while (tok.hasMoreTokens()) {
 			String next = tok.nextToken().trim();
 			int i = next.indexOf('=');
 			if (i > 0 && i < next.length() - 1) {
 				String tag = next.substring(0, i).trim();
 				String value = next.substring(i + 1, next.length()).trim();
 				if (tag.length() > 0 && value.length() > 0) {
 					properties.put(tag, value);
 				}
 			}
 		}
 	}
 
 	public Object run(String[] args) throws Exception {
 		long time = -System.currentTimeMillis();
 		initializeServices();
 		processArguments(args);
 
 		IStatus operationStatus = Status.OK_STATUS;
 		InstallableUnitQuery query;
 		Collector roots;
 		try {
 			initializeRepositories(command == COMMAND_INSTALL);
 			switch (command) {
 				case COMMAND_INSTALL :
 				case COMMAND_UNINSTALL :
 
 					IProfile profile = initializeProfile();
 					query = new InstallableUnitQuery(root, version == null ? VersionRange.emptyRange : new VersionRange(version, true, version, true));
 					roots = collectRootIUs(metadataRepositoryLocations, new CompositeQuery(new Query[] {query, new LatestIUVersionQuery()}), new Collector());
 					if (roots.size() <= 0)
 						roots = profile.query(query, roots, new NullProgressMonitor());
 					if (roots.size() <= 0) {
 						LogHelper.log(new Status(IStatus.ERROR, Activator.ID, NLS.bind(Messages.Missing_IU, root)));
 						System.out.println(NLS.bind(Messages.Missing_IU, root));
 						return EXIT_ERROR;
 					}
 					if (!updateRoamingProperties(profile).isOK()) {
 						LogHelper.log(new Status(IStatus.ERROR, Activator.ID, NLS.bind(Messages.Cant_change_roaming, profile.getProfileId())));
 						System.out.println(NLS.bind(Messages.Cant_change_roaming, profile.getProfileId()));
 						return EXIT_ERROR;
 					}
 					ProvisioningContext context = new ProvisioningContext(metadataRepositoryLocations);
 					context.setArtifactRepositories(artifactRepositoryLocations);
 					ProfileChangeRequest request = buildProvisioningRequest(profile, roots, command == COMMAND_INSTALL);
 					printRequest(request);
 					operationStatus = planAndExecute(profile, context, request);
 					break;
 				case COMMAND_LIST :
 					query = new InstallableUnitQuery(null, VersionRange.emptyRange);
 					if (metadataRepositoryLocations == null)
 						missingArgument("metadataRepository"); //$NON-NLS-1$
 
 					roots = collectRootIUs(metadataRepositoryLocations, query, null);
 					Iterator unitIterator = roots.iterator();
 					while (unitIterator.hasNext()) {
 						IInstallableUnit iu = (IInstallableUnit) unitIterator.next();
 						System.out.println(iu.getId());
 					}
 					break;
 			}
 		} finally {
 			cleanupRepositories();
 		}
 
 		time += System.currentTimeMillis();
 		if (operationStatus.isOK())
 			System.out.println(NLS.bind(Messages.Operation_complete, new Long(time)));
 		else {
 			System.out.println(Messages.Operation_failed);
 			LogHelper.log(operationStatus);
 			return EXIT_ERROR;
 		}
 		return IApplication.EXIT_OK;
 	}
 
 	private void cleanupRepositories() {
 		if (artifactReposForRemoval != null && artifactManager != null) {
 			for (int i = 0; i < artifactReposForRemoval.length && artifactReposForRemoval[i] != null; i++) {
 				artifactManager.removeRepository(artifactReposForRemoval[i]);
 			}
 		}
 		if (metadataReposForRemoval != null && metadataManager != null) {
 			for (int i = 0; i < metadataReposForRemoval.length && metadataReposForRemoval[i] != null; i++) {
 				metadataManager.removeRepository(metadataReposForRemoval[i]);
 			}
 		}
 	}
 
 	class LocationQueryable implements IQueryable {
 		private URI location;
 
 		public LocationQueryable(URI location) {
 			this.location = location;
 		}
 
 		public Collector query(Query query, Collector collector, IProgressMonitor monitor) {
 			return ProvisioningHelper.getInstallableUnits(location, query, collector, monitor);
 		}
 	}
 
 	private Collector collectRootIUs(URI[] locations, Query query, Collector collector) {
 		IProgressMonitor nullMonitor = new NullProgressMonitor();
 
 		if (locations == null || locations.length == 0)
 			return ProvisioningHelper.getInstallableUnits(null, query, collector, nullMonitor);
 
 		Collector result = collector != null ? collector : new Collector();
 		IQueryable[] locationQueryables = new IQueryable[locations.length];
 		for (int i = 0; i < locations.length; i++) {
 			locationQueryables[i] = new LocationQueryable(locations[i]);
 		}
 		return new CompoundQueryable(locationQueryables).query(query, result, nullMonitor);
 	}
 
 	private synchronized void setPackageAdmin(PackageAdmin service) {
 		packageAdmin = service;
 	}
 
 	private boolean startEarly(String bundleName) throws BundleException {
 		Bundle bundle = getBundle(bundleName);
 		if (bundle == null)
 			return false;
 		bundle.start(Bundle.START_TRANSIENT);
 		return true;
 	}
 
 	public Object start(IApplicationContext context) throws Exception {
 		packageAdminRef = Activator.getContext().getServiceReference(PackageAdmin.class.getName());
 		setPackageAdmin((PackageAdmin) Activator.getContext().getService(packageAdminRef));
 		if (!startEarly(EXEMPLARY_SETUP)) {
 			LogHelper.log(new Status(IStatus.ERROR, Activator.ID, NLS.bind(Messages.Missing_bundle, EXEMPLARY_SETUP)));
 			return EXIT_ERROR;
 		}
 		if (!startEarly(SIMPLE_CONFIGURATOR_MANIPULATOR)) {
 			LogHelper.log(new Status(IStatus.ERROR, Activator.ID, NLS.bind(Messages.Missing_bundle, SIMPLE_CONFIGURATOR_MANIPULATOR)));
 			return EXIT_ERROR;
 		}
 		if (!startEarly(FRAMEWORKADMIN_EQUINOX)) {
 			LogHelper.log(new Status(IStatus.ERROR, Activator.ID, NLS.bind(Messages.Missing_bundle, FRAMEWORKADMIN_EQUINOX)));
 			return EXIT_ERROR;
 		}
 
 		return run((String[]) context.getArguments().get("application.args")); //$NON-NLS-1$
 	}
 
 	public void stop() {
 		setPackageAdmin(null);
 		Activator.getContext().ungetService(packageAdminRef);
 	}
 
 	private String toString(Properties context) {
 		StringBuffer result = new StringBuffer();
 		for (Enumeration iter = context.keys(); iter.hasMoreElements();) {
 			String key = (String) iter.nextElement();
 			result.append(key);
 			result.append('=');
 			result.append(context.get(key));
 			if (iter.hasMoreElements())
 				result.append(',');
 		}
 		return result.toString();
 	}
 
 	private IStatus updateRoamingProperties(IProfile profile) {
 		ProfileChangeRequest request = new ProfileChangeRequest(profile);
 		if (!Boolean.valueOf(profile.getProperty(IProfile.PROP_ROAMING)).booleanValue())
 			return Status.OK_STATUS;
 		File destinationFile = destination.toFile();
 		if (!destinationFile.equals(new File(profile.getProperty(IProfile.PROP_INSTALL_FOLDER))))
 			request.setProfileProperty(IProfile.PROP_INSTALL_FOLDER, destination.toOSString());
 		if (!destinationFile.equals(new File(profile.getProperty(IProfile.PROP_CACHE))))
 			request.setProfileProperty(IProfile.PROP_CACHE, destination.toOSString());
 		if (request.getProfileProperties().size() == 0)
 			return Status.OK_STATUS;
 
		ProvisioningContext context = new ProvisioningContext(new URI[0]);
		context.setArtifactRepositories(new URI[0]);
		ProvisioningPlan result = planner.getProvisioningPlan(request, context, new NullProgressMonitor());
 		if (!result.getStatus().isOK())
 			return result.getStatus();
 
		return engine.perform(profile, new PhaseSet(new Phase[] {new Property(1)}) {}, result.getOperands(), context, new NullProgressMonitor());
 	}
 
 	private static URI[] getURIs(String spec) {
 		if (spec == null)
 			return null;
 		String[] urlSpecs = getArrayFromString(spec, ","); //$NON-NLS-1$
 		ArrayList result = new ArrayList(urlSpecs.length);
 		for (int i = 0; i < urlSpecs.length; i++) {
 			try {
 				result.add(URIUtil.fromString(urlSpecs[i]));
 			} catch (URISyntaxException e) {
 				LogHelper.log(new Status(IStatus.WARNING, Activator.ID, NLS.bind(Messages.Ignored_repo, urlSpecs[i])));
 			}
 		}
 		if (result.size() == 0)
 			return null;
 		return (URI[]) result.toArray(new URI[result.size()]);
 	}
 
 	/**
 	 * Convert a list of tokens into an array. The list separator has to be
 	 * specified.
 	 */
 	public static String[] getArrayFromString(String list, String separator) {
 		if (list == null || list.trim().equals("")) //$NON-NLS-1$
 			return new String[0];
 		List result = new ArrayList();
 		for (StringTokenizer tokens = new StringTokenizer(list, separator); tokens.hasMoreTokens();) {
 			String token = tokens.nextToken().trim();
 			if (!token.equals("")) //$NON-NLS-1$
 				result.add(token);
 		}
 		return (String[]) result.toArray(new String[result.size()]);
 	}
 
 }
