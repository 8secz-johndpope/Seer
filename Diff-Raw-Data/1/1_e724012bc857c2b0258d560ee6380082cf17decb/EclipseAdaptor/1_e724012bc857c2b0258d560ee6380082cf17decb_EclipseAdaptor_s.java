 /*******************************************************************************
  * Copyright (c) 2003, 2004 IBM Corporation and others.
  * All rights reserved. This program and the accompanying materials 
  * are made available under the terms of the Common Public License v1.0
  * which accompanies this distribution, and is available at
  * http://www.eclipse.org/legal/cpl-v10.html
  * 
  * Contributors:
  *     IBM Corporation - initial API and implementation
  *******************************************************************************/
 package org.eclipse.core.runtime.adaptor;
 
 import java.io.*;
 import java.net.MalformedURLException;
 import java.util.*;
 import javax.xml.parsers.DocumentBuilderFactory;
 import javax.xml.parsers.SAXParserFactory;
 import org.eclipse.osgi.framework.adaptor.*;
 import org.eclipse.osgi.framework.adaptor.core.*;
 import org.eclipse.osgi.framework.console.CommandProvider;
 import org.eclipse.osgi.framework.debug.Debug;
 import org.eclipse.osgi.framework.debug.DebugOptions;
 import org.eclipse.osgi.framework.log.FrameworkLog;
 import org.eclipse.osgi.framework.log.FrameworkLogEntry;
 import org.eclipse.osgi.framework.stats.StatsManager;
 import org.eclipse.osgi.internal.resolver.StateImpl;
 import org.eclipse.osgi.internal.resolver.StateManager;
 import org.eclipse.osgi.service.datalocation.FileManager;
 import org.eclipse.osgi.service.datalocation.Location;
 import org.eclipse.osgi.service.pluginconversion.PluginConverter;
 import org.eclipse.osgi.service.resolver.*;
 import org.eclipse.osgi.service.urlconversion.URLConverter;
 import org.osgi.framework.*;
 
 /**
  * Internal class.
  */
 public class EclipseAdaptor extends AbstractFrameworkAdaptor {
 	public static final String PROP_CLEAN = "osgi.clean"; //$NON-NLS-1$
 
 	public static final String PROP_EXITONERROR = "eclipse.exitOnError"; //$NON-NLS-1$
 
 	static final String F_LOG = ".log"; //$NON-NLS-1$
 
 	// TODO rename it to Eclipse-PluginClass
 	public static final String PLUGIN_CLASS = "Plugin-Class"; //$NON-NLS-1$
 
 	public static final String ECLIPSE_AUTOSTART = "Eclipse-AutoStart"; //$NON-NLS-1$
 
 	// TODO rename constant to ECLIPSE_AUTOSTART_EXCEPTIONS
 	public static final String ECLIPSE_AUTOSTART_EXCEPTIONS = "exceptions"; //$NON-NLS-1$
 
 	public static final String SAXFACTORYNAME = "javax.xml.parsers.SAXParserFactory"; //$NON-NLS-1$
 
 	public static final String DOMFACTORYNAME = "javax.xml.parsers.DocumentBuilderFactory"; //$NON-NLS-1$
 
 	private static final String RUNTIME_ADAPTOR = FRAMEWORK_SYMBOLICNAME + "/eclipseadaptor"; //$NON-NLS-1$
 
 	private static final String OPTION_STATE_READER = RUNTIME_ADAPTOR + "/state/reader";//$NON-NLS-1$
 
 	private static final String OPTION_RESOLVER = RUNTIME_ADAPTOR + "/resolver/timing"; //$NON-NLS-1$
 
 	private static final String OPTION_PLATFORM_ADMIN = RUNTIME_ADAPTOR + "/debug/platformadmin"; //$NON-NLS-1$
 
 	private static final String OPTION_PLATFORM_ADMIN_RESOLVER = RUNTIME_ADAPTOR + "/debug/platformadmin/resolver"; //$NON-NLS-1$
 
 	private static final String OPTION_MONITOR_PLATFORM_ADMIN = RUNTIME_ADAPTOR + "/resolver/timing"; //$NON-NLS-1$
 
 	private static final String OPTION_RESOLVER_READER = RUNTIME_ADAPTOR + "/resolver/reader/timing"; //$NON-NLS-1$
 
 	private static final String OPTION_CONVERTER = RUNTIME_ADAPTOR + "/converter/debug"; //$NON-NLS-1$
 
 	private static final String OPTION_LOCATION = RUNTIME_ADAPTOR + "/debug/location"; //$NON-NLS-1$	
 
 	public static final byte BUNDLEDATA_COMPATIBLE_VERSION = 10;
 	public static final byte BUNDLEDATA_VERSION_11 = 11;
 	public static final byte BUNDLEDATA_VERSION = 11;
 
 	public static final byte NULL = 0;
 
 	public static final byte OBJECT = 1;
 
 	private static EclipseAdaptor instance;
 
 	private byte cacheVersion;
 
 	private long timeStamp = 0;
 
 	private String installURL = null;
 
 	private boolean exitOnError = true;
 
 	private BundleStopper stopper;
 
 	private FileManager fileManager;
 
 	/*
 	 * Should be instantiated only by the framework (through reflection).
 	 */
 	public EclipseAdaptor(String[] args) {
 		super(args);
 		instance = this;
 		setDebugOptions();
 	}
 
 	public static EclipseAdaptor getDefault() {
 		return instance;
 	}
 
 	public void initialize(EventPublisher eventPublisher) {
 		if (Boolean.getBoolean(EclipseAdaptor.PROP_CLEAN))
 			cleanOSGiCache();
 		fileManager = initFileManager(LocationManager.getOSGiConfigurationDir(), LocationManager.getConfigurationLocation().isReadOnly() ? "none" : null); //$NON-NLS-1$
 		readHeaders();
 		checkLocationAndReinitialize();
 		super.initialize(eventPublisher);
 	}
 
 	public void initializeMetadata() {
 		// do nothing here; metadata is already initialized by readHeaders.
 	}
 
 	protected void initBundleStoreRootDir() {
 		File configurationLocation = LocationManager.getOSGiConfigurationDir();
 		if (configurationLocation != null) {
 			bundleStoreRootDir = new File(configurationLocation, LocationManager.BUNDLES_DIR);
 			bundleStore = bundleStoreRootDir.getAbsolutePath();
 		} else {
 			// last resort just default to "bundles"
 			bundleStore = LocationManager.BUNDLES_DIR;
 			bundleStoreRootDir = new File(bundleStore);
 		}
 
 		/* store bundleStore back into adaptor properties for others to see */
 		properties.put(BUNDLE_STORE, bundleStoreRootDir.getAbsolutePath());
 	}
 
 	protected FrameworkLog createFrameworkLog() {
 		if (frameworkLog != null)
 			return frameworkLog;
 		return EclipseStarter.createFrameworkLog();
 	}
 
 	protected StateManager createStateManager() {
 		File stateLocation = null;
 		try {
 			stateLocation = fileManager.lookup(LocationManager.STATE_FILE, false);
 		} catch (IOException ex) {
 			if (Debug.DEBUG && Debug.DEBUG_GENERAL) {
 				Debug.println("Error reading state file " + ex.getMessage()); //$NON-NLS-1$
 				Debug.printStackTrace(ex);
 			}
 		}
 		//if it does not exist, try to read it from the parent
 		if (stateLocation == null || !stateLocation.isFile()) { // NOTE this check is redundant since it
 			// is done in StateManager, however it
 			// is more convenient to have it here
 			Location parentConfiguration = null;
 			Location currentConfiguration = LocationManager.getConfigurationLocation();
 			if (currentConfiguration != null && (parentConfiguration = currentConfiguration.getParentLocation()) != null) {
 				try {
 					File stateLocationDir = new File(parentConfiguration.getURL().getFile(), FrameworkAdaptor.FRAMEWORK_SYMBOLICNAME);
 					FileManager newFileManager = initFileManager(stateLocationDir, parentConfiguration.isReadOnly() ? "none" : null); //$NON-NLS-1$);
 					stateLocation = newFileManager.lookup(LocationManager.STATE_FILE, true);
 				} catch (IOException ex) {
 					if (Debug.DEBUG && Debug.DEBUG_GENERAL) {
 						Debug.println("Error reading state file " + ex.getMessage()); //$NON-NLS-1$
 						Debug.printStackTrace(ex);
 					}
 				}
 			} else {
 				try {
 					//it did not exist in either place, so create it in the original location
 					stateLocation = fileManager.lookup(LocationManager.STATE_FILE, true);
 				} catch (IOException ex) {
 					if (Debug.DEBUG && Debug.DEBUG_GENERAL) {
 						Debug.println("Error reading state file " + ex.getMessage()); //$NON-NLS-1$
 						Debug.printStackTrace(ex);
 					}
 				}
 			}
 		}
 		stateManager = new StateManager(stateLocation, timeStamp);
 		stateManager.setInstaller(new EclipseBundleInstaller());
 		StateImpl systemState = null;
 		if (!invalidState) {
 			systemState = stateManager.readSystemState(context);
 			if (systemState != null)
 				return stateManager;
 		}
 		systemState = stateManager.createSystemState(context);
 		Bundle[] installedBundles = context.getBundles();
 		if (installedBundles == null)
 			return stateManager;
 		StateObjectFactory factory = stateManager.getFactory();
 		for (int i = 0; i < installedBundles.length; i++) {
 			Bundle toAdd = installedBundles[i];
 			try {
 				Dictionary toAddManifest = toAdd.getHeaders(""); //$NON-NLS-1$
 				// if this is a cached manifest need to get the real one
 				if (toAddManifest instanceof CachedManifest)
 					toAddManifest = ((CachedManifest) toAddManifest).getManifest();
 				BundleDescription newDescription = factory.createBundleDescription(toAddManifest, toAdd.getLocation(), toAdd.getBundleId());
 				systemState.addBundle(newDescription);
 			} catch (BundleException be) {
 				// just ignore bundle datas with invalid manifests
 			}
 		}
 		// we need the state resolved
 		systemState.setTimeStamp(timeStamp);
 		systemState.resolve();
 		invalidState = false;
 		return stateManager;
 	}
 
 	public void shutdownStateManager() {
 		if (timeStamp == stateManager.getSystemState().getTimeStamp())
 			return;
 		try {
 			File stateLocationTmpFile = File.createTempFile(LocationManager.STATE_FILE, ".new", LocationManager.getOSGiConfigurationDir()); //$NON-NLS-1$
 			stateManager.shutdown(stateLocationTmpFile); //$NON-NLS-1$
 			fileManager.update(new String[] {LocationManager.STATE_FILE}, new String[] {stateLocationTmpFile.getName()});
 		} catch (IOException e) {
 			frameworkLog.log(new FrameworkEvent(FrameworkEvent.ERROR, context.getBundle(), e));
 		}
 	}
 
 	private void cleanOSGiCache() {
 		File osgiConfig = LocationManager.getOSGiConfigurationDir();
 		if (!rm(osgiConfig)) {
 			// TODO log error?
 		}
 	}
 
 	private void checkLocationAndReinitialize() {
 		if (installURL == null) {
 			installURL = EclipseStarter.getSysPath(); // TODO This reference to the starter should be avoided
 			return;
 		}
 		if (!EclipseStarter.getSysPath().equals(installURL)) {
 			// delete the metadata file and the framework file when the location of the basic bundles has changed
 
 			try {
 				fileManager.remove(LocationManager.BUNDLE_DATA_FILE);
 				fileManager.remove(LocationManager.STATE_FILE);
 			} catch (IOException ex) {
 				if (Debug.DEBUG && Debug.DEBUG_GENERAL) {
 					Debug.println("Error deleting framework metadata: " + ex.getMessage()); //$NON-NLS-1$
 					Debug.printStackTrace(ex);
 				}
 			}
 
 			installURL = EclipseStarter.getSysPath();
 		}
 	}
 
 	private void readHeaders() {
 		InputStream bundleDataStream = findBundleDataFile();
 		if (bundleDataStream == null)
 			return;
 
 		try {
 			DataInputStream in = new DataInputStream(new BufferedInputStream(bundleDataStream));
 			try {
 				cacheVersion = in.readByte();
 				if (cacheVersion >= BUNDLEDATA_COMPATIBLE_VERSION) {
 					timeStamp = in.readLong();
 					installURL = in.readUTF();
 					initialBundleStartLevel = in.readInt();
 					nextId = in.readLong();
 				}
 			} finally {
 				in.close();
 			}
 		} catch (IOException e) {
 			if (Debug.DEBUG && Debug.DEBUG_GENERAL) {
 				Debug.println("Error reading framework metadata: " + e.getMessage()); //$NON-NLS-1$
 				Debug.printStackTrace(e);
 			}
 		}
 	}
 
 	public AdaptorElementFactory getElementFactory() {
 		if (elementFactory == null)
 			elementFactory = new EclipseElementFactory();
 		return elementFactory;
 	}
 
 	public void frameworkStart(BundleContext context) throws BundleException {
 		// must register the xml parser and initialize the plugin converter
 		// instance first because we may need it when creating the statemanager
 		// in super.frameworkStart(context)
 		registerEndorsedXMLParser(context);
 		PluginConverter converter = new PluginConverterImpl(context);
 		super.frameworkStart(context);
 		Bundle bundle = context.getBundle();
 		Location location;
 
 		// Less than optimal reference to EclipseStarter here. Not sure how we
 		// can make the location
 		// objects available. They are needed very early in EclipseStarter but
 		// these references tie
 		// the adaptor to that starter.
 		location = LocationManager.getUserLocation();
 		Hashtable properties = new Hashtable(1);
 		if (location != null) {
 			properties.put("type", LocationManager.PROP_USER_AREA); //$NON-NLS-1$
 			context.registerService(Location.class.getName(), location, properties);
 		}
 		location = LocationManager.getInstanceLocation();
 		if (location != null) {
 			properties.put("type", LocationManager.PROP_INSTANCE_AREA); //$NON-NLS-1$
 			context.registerService(Location.class.getName(), location, properties);
 		}
 		location = LocationManager.getConfigurationLocation();
 		if (location != null) {
 			properties.put("type", LocationManager.PROP_CONFIG_AREA); //$NON-NLS-1$
 			context.registerService(Location.class.getName(), location, properties);
 		}
 		location = LocationManager.getInstallLocation();
 		if (location != null) {
 			properties.put("type", LocationManager.PROP_INSTALL_AREA); //$NON-NLS-1$
 			context.registerService(Location.class.getName(), location, properties);
 		}
 
 		register(org.eclipse.osgi.service.environment.EnvironmentInfo.class.getName(), EnvironmentInfo.getDefault(), bundle);
 		register(PlatformAdmin.class.getName(), stateManager, bundle);
 		register(PluginConverter.class.getName(), converter, bundle);
 		register(URLConverter.class.getName(), new URLConverterImpl(), bundle);
 		register(CommandProvider.class.getName(), new EclipseCommandProvider(context), bundle);
 		register(FrameworkLog.class.getName(), getFrameworkLog(), bundle);
 		register(org.eclipse.osgi.service.localization.BundleLocalization.class.getName(), new BundleLocalizationImpl(), bundle);
 	}
 
 	private void setDebugOptions() {
 		DebugOptions options = DebugOptions.getDefault();
 		// may be null if debugging is not enabled
 		if (options == null)
 			return;
 		StateManager.DEBUG = options != null;
 		StateManager.DEBUG_READER = options.getBooleanOption(OPTION_RESOLVER_READER, false);
 		StateManager.MONITOR_PLATFORM_ADMIN = options.getBooleanOption(OPTION_MONITOR_PLATFORM_ADMIN, false);
 		StateManager.DEBUG_PLATFORM_ADMIN = options.getBooleanOption(OPTION_PLATFORM_ADMIN, false);
 		StateManager.DEBUG_PLATFORM_ADMIN_RESOLVER = options.getBooleanOption(OPTION_PLATFORM_ADMIN_RESOLVER, false);
 		PluginConverterImpl.DEBUG = options.getBooleanOption(OPTION_CONVERTER, false);
 		BasicLocation.DEBUG = options.getBooleanOption(OPTION_LOCATION, false);
 	}
 
 	private void registerEndorsedXMLParser(BundleContext bc) {
 		try {
 			Class.forName(SAXFACTORYNAME);
 			bc.registerService(SAXFACTORYNAME, new SaxParsingService(), new Hashtable());
 			Class.forName(DOMFACTORYNAME);
 			bc.registerService(DOMFACTORYNAME, new DomParsingService(), new Hashtable());
 		} catch (ClassNotFoundException e) {
 			// In case the JAXP API is not on the boot classpath
 			String message = EclipseAdaptorMsg.formatter.getString("ECLIPSE_ADAPTOR_ERROR_XML_SERVICE"); //$NON-NLS-1$
 			getFrameworkLog().log(new FrameworkLogEntry(FrameworkAdaptor.FRAMEWORK_SYMBOLICNAME, message, 0, e, null));
 		}
 	}
 
 	private class SaxParsingService implements ServiceFactory {
 		public Object getService(Bundle bundle, ServiceRegistration registration) {
 			return SAXParserFactory.newInstance();
 		}
 
 		public void ungetService(Bundle bundle, ServiceRegistration registration, Object service) {
 			// Do nothing.
 		}
 	}
 
 	private class DomParsingService implements ServiceFactory {
 		public Object getService(Bundle bundle, ServiceRegistration registration) {
 			return DocumentBuilderFactory.newInstance();
 		}
 
 		public void ungetService(Bundle bundle, ServiceRegistration registration, Object service) {
 			// Do nothing.
 		}
 	}
 
 	public void frameworkStop(BundleContext context) throws BundleException {
 		saveMetaData();
 		super.frameworkStop(context);
 		printStats();
 		PluginParser.releaseXMLParsing();
 		fileManager.close();
 	}
 
 	private void printStats() {
 		DebugOptions debugOptions = DebugOptions.getDefault();
 		if (debugOptions == null)
 			return;
 		String registryParsing = debugOptions.getOption("org.eclipse.core.runtime/registry/parsing/timing/value"); //$NON-NLS-1$
 		if (registryParsing != null)
 			EclipseAdaptorMsg.debug("Time spent in registry parsing: " + registryParsing); //$NON-NLS-1$
 		String packageAdminResolution = debugOptions.getOption("debug.packageadmin/timing/value"); //$NON-NLS-1$
 		if (packageAdminResolution != null)
 			System.out.println("Time spent in package admin resolve: " + packageAdminResolution); //$NON-NLS-1$			
 		String constraintResolution = debugOptions.getOption("org.eclipse.core.runtime.adaptor/resolver/timing/value"); //$NON-NLS-1$
 		if (constraintResolution != null)
 			System.out.println("Time spent resolving the dependency system: " + constraintResolution); //$NON-NLS-1$ 
 	}
 
 	private InputStream findBundleDataFile() {
 		File metadata = null;
 		try {
 			metadata = fileManager.lookup(LocationManager.BUNDLE_DATA_FILE, false);
 		} catch (IOException ex) {
 			if (Debug.DEBUG && Debug.DEBUG_GENERAL) {
 				Debug.println("Error reading framework metadata: " + ex.getMessage()); //$NON-NLS-1$
 				Debug.printStackTrace(ex);
 			}
 		}
 		InputStream bundleDataStream = null;
 		if (metadata != null && metadata.isFile()) {
 			try {
 				bundleDataStream = new FileInputStream(metadata);
 			} catch (FileNotFoundException e1) {
 				// this can not happen since it is tested before entering here.
 			}
 		} else {
 			Location currentConfiguration = LocationManager.getConfigurationLocation();
 			Location parentConfiguration = null;
 			if (currentConfiguration != null && (parentConfiguration = currentConfiguration.getParentLocation()) != null) {
 				try {
 					File bundledataLocationDir = new File(parentConfiguration.getURL().getFile(), FrameworkAdaptor.FRAMEWORK_SYMBOLICNAME);
 					FileManager newFileManager = initFileManager(bundledataLocationDir, parentConfiguration.isReadOnly() ? "none" : null); //$NON-NLS-1$
 					File bundleData = newFileManager.lookup(LocationManager.BUNDLE_DATA_FILE, true);
 					bundleDataStream = new FileInputStream(bundleData);
 				} catch (MalformedURLException e1) {
 					// This will not happen since all the URLs are derived by us
 					// and we are GODS!
 				} catch (IOException e1) {
 					// That's ok we will regenerate the .bundleData
 				}
 			}
 		}
 		return bundleDataStream;
 	}
 
 	/**
 	 * @see org.eclipse.osgi.framework.adaptor.FrameworkAdaptor#getInstalledBundles()
 	 */
 	public BundleData[] getInstalledBundles() {
 		InputStream bundleDataStream = findBundleDataFile();
 		if (bundleDataStream == null)
 			return null;
 
 		try {
 			DataInputStream in = new DataInputStream(new BufferedInputStream(bundleDataStream));
 			try {
 				if (in.readByte() < BUNDLEDATA_COMPATIBLE_VERSION)
 					return null;
 				// skip timeStamp - was read by readHeaders
 				in.readLong();
 				in.readUTF();
 				in.readInt();
 				in.readLong();
 
 				int bundleCount = in.readInt();
 				ArrayList result = new ArrayList(bundleCount);
 				long id = -1;
 
 				for (int i = 0; i < bundleCount; i++) {
 					try {
 						try {
 							id = in.readLong();
 							if (id != 0) {
 								EclipseBundleData data = (EclipseBundleData) getElementFactory().createBundleData(this, id);
 								loadMetaDataFor(data, in);
 								data.initializeExistingBundle();
 								if (Debug.DEBUG && Debug.DEBUG_GENERAL)
 									Debug.println("BundleData created: " + data); //$NON-NLS-1$ 
 								result.add(data);
 							}
 						} catch (NumberFormatException e) {
 							// should never happen
 						}
 					} catch (IOException e) {
 						if (Debug.DEBUG && Debug.DEBUG_GENERAL) {
 							Debug.println("Error reading framework metadata: " + e.getMessage()); //$NON-NLS-1$ 
 							Debug.printStackTrace(e);
 						}
 					}
 				}
 				return (BundleData[]) result.toArray(new BundleData[result.size()]);
 			} finally {
 				in.close();
 			}
 		} catch (IOException e) {
 			if (Debug.DEBUG && Debug.DEBUG_GENERAL) {
 				Debug.println("Error reading framework metadata: " + e.getMessage()); //$NON-NLS-1$ 
 				Debug.printStackTrace(e);
 			}
 		}
 		return null;
 	}
 
 	protected void loadMetaDataFor(EclipseBundleData data, DataInputStream in) throws IOException {
 		byte flag = in.readByte();
 		if (flag == NULL)
 			return;
 		data.setLocation(readString(in, false));
 		data.setFileName(readString(in, false));
 		data.setSymbolicName(readString(in, false));
 		data.setVersion(Version.parseVersion(readString(in, false)));
 		data.setActivator(readString(in, false));
 		data.setAutoStart(in.readBoolean());
 		int exceptionsCount = in.readInt();
 		String[] autoStartExceptions = exceptionsCount > 0 ? new String[exceptionsCount] : null;
 		for (int i = 0; i < exceptionsCount; i++)
 			autoStartExceptions[i] = in.readUTF();
 		data.setAutoStartExceptions(autoStartExceptions);
 		data.setPluginClass(readString(in, false));
 		data.setClassPath(readString(in, false));
 		data.setNativePaths(readString(in, false));
 		data.setExecutionEnvironment(readString(in, false));
 		data.setDynamicImports(readString(in, false));
 		data.setGeneration(in.readInt());
 		data.setStartLevel(in.readInt());
 		data.setStatus(in.readInt());
 		data.setReference(in.readBoolean());
 		data.setFragment(in.readBoolean());
 		data.setManifestTimeStamp(in.readLong());
 		data.setManifestType(in.readByte());
 		if (cacheVersion >= BUNDLEDATA_VERSION_11)
 			data.setLastModified(in.readLong());
 	}
 
 	public void saveMetaDataFor(EclipseBundleData data) throws IOException {
 		if (!data.isAutoStartable()) {
 			timeStamp--; // Change the value of the timeStamp, as a marker
 			// that something changed.
 		}
 	}
 
 	public void persistInitialBundleStartLevel(int value) {
 		// Change the value of the timeStamp, as a marker that something
 		// changed.
 		timeStamp--;
 	}
 
 	public void persistNextBundleID(long value) {
 		// Do nothing the timeStamp will have changed because the state will be
 		// updated.
 	}
 
 	protected void saveMetaDataFor(BundleData data, DataOutputStream out) throws IOException {
 		if (data.getBundleID() == 0 || !(data instanceof AbstractBundleData)) {
 			out.writeByte(NULL);
 			return;
 		}
 		EclipseBundleData bundleData = (EclipseBundleData) data;
 		out.writeByte(OBJECT);
 		writeStringOrNull(out, bundleData.getLocation());
 		writeStringOrNull(out, bundleData.getFileName());
 		writeStringOrNull(out, bundleData.getSymbolicName());
 		writeStringOrNull(out, bundleData.getVersion().toString());
 		writeStringOrNull(out, bundleData.getActivator());
 		out.writeBoolean(bundleData.isAutoStart());
 		String[] autoStartExceptions = bundleData.getAutoStartExceptions();
 		if (autoStartExceptions == null)
 			out.writeInt(0);
 		else {
 			out.writeInt(autoStartExceptions.length);
 			for (int i = 0; i < autoStartExceptions.length; i++)
 				out.writeUTF(autoStartExceptions[i]);
 		}
 		writeStringOrNull(out, bundleData.getPluginClass());
 		writeStringOrNull(out, bundleData.getClassPath());
 		writeStringOrNull(out, bundleData.getNativePathsString());
 		writeStringOrNull(out, bundleData.getExecutionEnvironment());
 		writeStringOrNull(out, bundleData.getDynamicImports());
 		out.writeInt(bundleData.getGeneration());
 		out.writeInt(bundleData.getStartLevel());
 		out.writeInt(bundleData.getPersistentStatus());
 		out.writeBoolean(bundleData.isReference());
 		out.writeBoolean(bundleData.isFragment());
 		out.writeLong(bundleData.getManifestTimeStamp());
 		out.writeByte(bundleData.getManifestType());
 		out.writeLong(bundleData.getLastModified());
 	}
 
 	private String readString(DataInputStream in, boolean intern) throws IOException {
 		byte type = in.readByte();
 		if (type == NULL)
 			return null;
 		if (intern)
 			return in.readUTF().intern();
 		else
 			return in.readUTF();
 	}
 
 	private void writeStringOrNull(DataOutputStream out, String string) throws IOException {
 		if (string == null)
 			out.writeByte(NULL);
 		else {
 			out.writeByte(OBJECT);
 			out.writeUTF(string);
 		}
 	}
 
 	public void saveMetaData() {
 		// the cache and the state match
 		if (timeStamp == stateManager.getSystemState().getTimeStamp())
 			return;
 		File metadata = null;
 		try {
 			metadata = File.createTempFile(LocationManager.BUNDLE_DATA_FILE, ".new", LocationManager.getOSGiConfigurationDir());//$NON-NLS-1$
 			DataOutputStream out = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(metadata)));
 			try {
 				out.writeByte(BUNDLEDATA_VERSION);
 				out.writeLong(stateManager.getSystemState().getTimeStamp());
 				out.writeUTF(installURL);
 				out.writeInt(initialBundleStartLevel);
 				out.writeLong(nextId);
 				Bundle[] bundles = context.getBundles();
 				out.writeInt(bundles.length);
 				for (int i = 0; i < bundles.length; i++) {
 					long id = bundles[i].getBundleId();
 					out.writeLong(id);
 					if (id != 0) {
 						BundleData data = ((org.eclipse.osgi.framework.internal.core.AbstractBundle) bundles[i]).getBundleData();
 						saveMetaDataFor(data, out);
 					}
 				}
 			} finally {
 				out.close();
 			}
 			fileManager.lookup(LocationManager.BUNDLE_DATA_FILE, true); //the BundleData file may not have been created at this point.  
 			fileManager.update(new String[] {LocationManager.BUNDLE_DATA_FILE}, new String[] {metadata.getName()});
 		} catch (IOException e) {
 			frameworkLog.log(new FrameworkEvent(FrameworkEvent.ERROR, context.getBundle(), e));
 			return;
 		}
 	}
 
 	public BundleWatcher getBundleWatcher() {
 		return StatsManager.getDefault();
 	}
 
 	protected BundleContext getContext() {
 		return context;
 	}
 
 	public void frameworkStopping(BundleContext context) {
 		super.frameworkStopping(context);
 		stopper = new BundleStopper();
 		stopper.stopBundles();
 	}
 
 	private boolean isFatalException(Throwable error) {
 		if (error instanceof VirtualMachineError) {
 			return true;
 		}
 		if (error instanceof ThreadDeath) {
 			return true;
 		}
 		return false;
 	}
 
 	public void handleRuntimeError(Throwable error) {
 		try {
 			// check the prop each time this happens (should NEVER happen!)
 			exitOnError = Boolean.valueOf(System.getProperty(PROP_EXITONERROR, "true")).booleanValue(); //$NON-NLS-1$
 			String message = EclipseAdaptorMsg.formatter.getString("ECLIPSE_ADAPTOR_RUNTIME_ERROR"); //$NON-NLS-1$
 			if (exitOnError && isFatalException(error))
 				message += ' ' + EclipseAdaptorMsg.formatter.getString("ECLIPSE_ADAPTOR_EXITING"); //$NON-NLS-1$
 			FrameworkLogEntry logEntry = new FrameworkLogEntry(FrameworkAdaptor.FRAMEWORK_SYMBOLICNAME, message, 0, error, null);
 			frameworkLog.log(logEntry);
 		} catch (Throwable t) {
 			// we may be in a currupted state and must be able to handle any
 			// errors (ie OutOfMemoryError)
 			// that may occur when handling the first error; this is REALLY the
 			// last resort.
 			try {
 				error.printStackTrace();
 				t.printStackTrace();
 			} catch (Throwable t1) {
 				// if we fail that then we are beyond help.
 			}
 		} finally {
 			// do the exit outside the try block just incase another runtime
 			// error was thrown while logging
 			if (exitOnError && isFatalException(error))
 				System.exit(13);
 		}
 	}
 
 	protected void setLog(FrameworkLog log) {
 		frameworkLog = log;
 	}
 
 	public BundleStopper getBundleStopper() {
 		return stopper;
 	}
 
 	private FileManager initFileManager(File baseDir, String lockMode) {
 		FileManager fManager = new FileManager(baseDir, lockMode);
 		if (!baseDir.exists())
 			baseDir.mkdirs();
 		try {
 			fManager.open(true);
 		} catch (IOException ex) {
 			if (Debug.DEBUG && Debug.DEBUG_GENERAL) {
 				Debug.println("Error reading framework metadata: " + ex.getMessage()); //$NON-NLS-1$
 				Debug.printStackTrace(ex);
 			}
 		}
 		return fManager;
 	}
 }
