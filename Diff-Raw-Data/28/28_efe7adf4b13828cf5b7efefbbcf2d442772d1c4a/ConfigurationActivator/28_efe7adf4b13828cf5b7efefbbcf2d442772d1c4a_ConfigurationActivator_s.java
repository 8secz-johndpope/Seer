 /*******************************************************************************
  * Copyright (c) 2003 IBM Corporation and others.
  * All rights reserved. This program and the accompanying materials 
  * are made available under the terms of the Common Public License v1.0
  * which accompanies this distribution, and is available at
  * http://www.eclipse.org/legal/cpl-v10.html
  * 
  * Contributors:
  *     IBM Corporation - initial API and implementation
  *******************************************************************************/
 package org.eclipse.update.internal.configurator;
 
 import java.io.*;
 import java.net.*;
 import java.util.*;
 
 import org.eclipse.core.runtime.*;
 import org.eclipse.osgi.service.debug.*;
 import org.eclipse.osgi.service.environment.*;
 import org.eclipse.update.configurator.*;
 import org.osgi.framework.*;
 import org.osgi.service.packageadmin.*;
 import org.osgi.service.startlevel.*;
 import org.osgi.util.tracker.*;
 
 public class ConfigurationActivator implements BundleActivator {
 
 	public static String PI_CONFIGURATOR = "org.eclipse.update.configurator";
 	// debug options
 	public static String OPTION_DEBUG = PI_CONFIGURATOR + "/debug";
 	// debug values
 	public static boolean DEBUG = false;
 	// os
 	private static boolean isWindows = System.getProperty("os.name").startsWith("Win");
 	
 	private static BundleContext context;
 	private ServiceTracker platformTracker;
 	private ServiceRegistration configurationFactorySR;
 	private String[] allArgs;
 	private Set ignore;
 	private BundleListener reconcilerListener;
 	private IPlatform platform;
 	private PlatformConfiguration configuration;
 	
 	// Location of the configuration data
 	private String configArea;
 	
 	//Need to store that because it is not provided by the platformConfiguration
 	private long lastTimeStamp;
 
 	public void start(BundleContext ctx) throws Exception {
 		context = ctx;
 		obtainArgs();
 		initialize();
 		//Short cut, if the configuration has not changed
 		String application = configuration.getApplicationIdentifier();
 
 		//TODO Hack until PDE changes the default application that they are setting
 		if("org.eclipse.ui.workbench".equals(System.getProperties().get("eclipse.application"))) { //$NON-NLS-1$ //$NON-NLS-2$
 			System.setProperty("eclipse.application", "org.eclipse.ui.ide.workbench"); //$NON-NLS-1$ //$NON-NLS-2$
 		}
 	
		if (lastTimeStamp==configuration.getChangeStamp() && !(application.equals(PlatformConfiguration.RECONCILER_APP) || System.getProperties().get("osgi.dev") != null))
 			if (System.getProperty("eclipse.application") == null) {
 				System.setProperty("eclipse.application", application);
 				return;
 			}
 		loadOptions();
 		if (DEBUG)
 			System.out.println("Starting update configurator...");
 		computeIgnoredBundles();
 		installBundles();
 	}
 
 
 	private void initialize() throws Exception {
 		platform = acquirePlatform();
 		if (platform==null)
 			throw new Exception("Can not start"); 
 		URL installURL = platform.getInstallURL();
 		configArea = platform.getConfigurationMetadataLocation().toOSString();
 		configurationFactorySR = context.registerService(IPlatformConfigurationFactory.class.getName(), new PlatformConfigurationFactory(), null);
 		configuration = getPlatformConfiguration(allArgs, installURL, configArea);
 
 		try {
 			DataInputStream stream = new DataInputStream(new FileInputStream(configArea + "/last.config.stamp"));
 			lastTimeStamp = stream.readLong();
 		} catch (FileNotFoundException e) {
 			lastTimeStamp = configuration.getChangeStamp() - 1;
 		} catch (IOException e) {
 			lastTimeStamp = configuration.getChangeStamp() - 1;
 		}
 	}
 
 	private void computeIgnoredBundles() {
 		String ignoreList = System.getProperty("eclipse.ignore", "org.eclipse.osgi,org.eclipse.core.boot,org.eclipse.core.runtime.adaptor");
 		ignore = new HashSet();
 		StringTokenizer tokenizer = new StringTokenizer(ignoreList, ",");
 		while (tokenizer.hasMoreTokens())
 			ignore.add(tokenizer.nextToken().trim());
 	}
 
 
 	private void obtainArgs() {
 		// all this is only to get the application args		
 		EnvironmentInfo envInfo = null;
 		ServiceReference envInfoSR = context.getServiceReference(EnvironmentInfo.class.getName());
 		if (envInfoSR != null)
 			envInfo = (EnvironmentInfo) context.getService(envInfoSR);
 		if (envInfo == null)
 			throw new IllegalStateException();
 		this.allArgs = envInfo.getAllArgs();
 		// we have what we want - release the service
 		context.ungetService(envInfoSR);
 	}
 
 	public void stop(BundleContext ctx) throws Exception {
 		// quick fix (hack) for bug 47861
 		try {
 			PlatformConfiguration.shutdown();
 		} catch (IOException e) {
 			// TODO Auto-generated catch block
 			e.printStackTrace();
 		}
 		
 		platform = null;
 		releasePlatform();
		writePlatformConfigurationTimeStamp();
 		configurationFactorySR.unregister();
 	}
 
 	private void writePlatformConfigurationTimeStamp() {
 		try {
 			DataOutputStream stream = new DataOutputStream(new FileOutputStream(configArea + "/last.config.stamp"));
 			stream.writeLong(configuration.getChangeStamp());
 		} catch (FileNotFoundException e) {
			lastTimeStamp = configuration.getChangeStamp() - 1;
 		} catch (IOException e) {
			lastTimeStamp = configuration.getChangeStamp() - 1;
 		}
		
 	}
 
 	private void releasePlatform() {
 		if (platformTracker == null)
 			return;
 		platformTracker.close();
 		platformTracker = null;
 	}
 	
 	private IPlatform acquirePlatform() {
 		if (platformTracker == null) {
 			platformTracker = new ServiceTracker(context, IPlatform.class.getName(), null);
 			platformTracker.open();
 		}
 		IPlatform result = (IPlatform) platformTracker.getService();
 		return result;
 	}
 
 	private void installBundles() {
 		ServiceReference reference = context.getServiceReference(StartLevel.class.getName());
 		StartLevel start = null;
 		if (reference != null)
 			start = (StartLevel) context.getService(reference);
 		try {
 			// Get the list of cached bundles and compare with the ones to be installed.
 			// Uninstall all the cached bundles that do not appear on the new list
 			Bundle[] cachedBundles = context.getBundles();
 			URL[] plugins = configuration.getPluginPath();
 			Bundle[] bundlesToUninstall = getBundlesToUninstall(cachedBundles, plugins);
 			for (int i=0; i<bundlesToUninstall.length; i++) {
 				try {
 					bundlesToUninstall[i].uninstall();
 				} catch (Exception e) {
 					System.err.println("Could not uninstall unused bundle " + bundlesToUninstall[i].getLocation());
 				}
 			}
 			ArrayList installed = new ArrayList(plugins.length);
 			for (int i = 0; i < plugins.length; i++) {
 				String location = plugins[i].toExternalForm();
 				try {
 					location = "reference:" + location.substring(0, location.lastIndexOf('/'));
 					if (!isInstalled(location)) {
 						Bundle target = context.installBundle(location);
 						installed.add(target);
 						if (start != null)
 							start.setBundleStartLevel(target, 4);
 					}
 				} catch (Exception e) {
 					if ((location.indexOf("org.eclipse.core.boot") == -1) && (location.indexOf("org.eclipse.osgi") == -1)) {
 						System.err.println("Ignoring bundle at: " + location);
 						System.err.println(e.getMessage());
 					}
 				}
 			}
 			context.ungetService(reference);
 			refreshPackages((Bundle[]) installed.toArray(new Bundle[installed.size()]));
 			if (System.getProperty("eclipse.application") == null || System.getProperty("eclipse.application").equals(PlatformConfiguration.RECONCILER_APP))
 				System.setProperty("eclipse.application", configuration.getApplicationIdentifier());
 			//			if (config.getApplicationIdentifier().equals(PlatformConfiguration.RECONCILER_APP) ) {
 			//				reconcilerListener = reconcilerListener();
 			//				context.addBundleListener(reconcilerListener);
 			//			}
 		} catch (Exception e) {
 			// TODO Auto-generated catch block
 			e.printStackTrace();
 		} finally {
 			releasePlatform();
 		}
 	}
 
 
 	private Bundle[] getBundlesToUninstall(Bundle[] cachedBundles, URL[] newPlugins) {
 		ArrayList bundlesToUninstall = new ArrayList();
 		for (int i=0; i<cachedBundles.length; i++) {
 			if (cachedBundles[i].getBundleId() == 0)
 				continue; // skip the system bundle
 			String location1 = cachedBundles[i].getLocation();
 			boolean found = false;
 			for (int j=0; !found && j<newPlugins.length; j++) {
 				String location2 = newPlugins[j].toExternalForm();
 				location2 = "reference:" + location2.substring(0, location2.lastIndexOf('/'));
 				if (isWindows) {
 					if (location2.equalsIgnoreCase(location1))
 						found = true;
 					// may need to add a trailing /
 					else if ((location2+'/').equalsIgnoreCase(location1))
 						found = true;
 				} else {
 					if (location2.equals(location1))
 						found = true;
 					// may need to add a trailing /
 					else if ((location2+'/').equals(location1))
 						found = true;
 				}
 			}
 			if (!found)
 				bundlesToUninstall.add(cachedBundles[i]);
 		}
 		return (Bundle[])bundlesToUninstall.toArray(new Bundle[bundlesToUninstall.size()]);
 	}
 			
 	private BundleListener reconcilerListener() {
 		return new BundleListener() {
 			public void bundleChanged(BundleEvent event) {
 				String buid = event.getBundle().getGlobalName();
 				if (event.getType() == BundleEvent.STOPPED && buid != null && buid.equals("org.eclipse.update.core"))
 					runPostReconciler();
 			}
 		};
 	}
 
 	private void runPostReconciler() {
 		Runnable postReconciler = new Runnable() {
 			public void run() {
 				try {
 					Bundle apprunner = context.getBundles("org.eclipse.core.applicationrunner")[0];
 					apprunner.stop();
 					context.removeBundleListener(reconcilerListener);
 					try {
 						PlatformConfiguration.shutdown();
 					} catch (IOException e) {
 						// TODO Auto-generated catch block
 						e.printStackTrace();
 					}
 					installBundles();
 					apprunner.start();
 				} catch (BundleException be) {
 					be.printStackTrace();
 				}
 			}
 		};
 		new Thread(postReconciler, "Post reconciler").start();
 	}
 
 	/**
 	 * This is a major hack to try to get the reconciler application running. However we should find a way to not run it.
 	 * @param args
 	 * @param metaPath
 	 * @return
 	 */
 	private PlatformConfiguration getPlatformConfiguration(String[] args, URL installURL, String configPath) {
 		try {
 			PlatformConfiguration.startup(args, null, installURL, configPath);
 		} catch (Exception e) {
 			if (platformTracker != null) {
 				String message = e.getMessage();
 				if (message == null)
 					message = "";
 				IStatus status = new Status(IStatus.ERROR, IPlatform.PI_RUNTIME, IStatus.OK, message, e);
 				((IPlatform) platformTracker.getService()).getLog(context.getBundle()).log(status);
 			}
 		}
 		return PlatformConfiguration.getCurrent();
 
 	}
 
 	/**
 	 * Do PackageAdmin.refreshPackages() in a synchronous way.  After installing
 	 * all the requested bundles we need to do a refresh and want to ensure that 
 	 * everything is done before returning.
 	 * @param bundles
 	 */
 	private void refreshPackages(Bundle[] bundles) {
 		if (bundles.length == 0)
 			return;
 		ServiceReference packageAdminRef = context.getServiceReference(PackageAdmin.class.getName());
 		PackageAdmin packageAdmin = null;
 		if (packageAdminRef != null) {
 			packageAdmin = (PackageAdmin) context.getService(packageAdminRef);
 			if (packageAdmin == null)
 				return;
 		}
 		// TODO this is such a hack it is silly.  There are still cases for race conditions etc
 		// but this should allow for some progress...
 		final Object semaphore = new Object();
 		FrameworkListener listener = new FrameworkListener() {
 			public void frameworkEvent(FrameworkEvent event) {
 				if (event.getType() == FrameworkEvent.PACKAGES_REFRESHED)
 					synchronized (semaphore) {
 						semaphore.notifyAll();
 					}
 			}
 		};
 		context.addFrameworkListener(listener);
 		packageAdmin.refreshPackages(bundles);
 		synchronized (semaphore) {
 			try {
 				semaphore.wait();
 			} catch (InterruptedException e) {
 			}
 		}
 		context.removeFrameworkListener(listener);
 		context.ungetService(packageAdminRef);
 	}
 
 	private boolean isInstalled(String location) {
 		Bundle[] installed = context.getBundles();
 		for (int i = 0; i < installed.length; i++) {
 			Bundle bundle = installed[i];
 			String bundleLocation = bundle.getLocation();
 			// On Windows, do case insensitive test
 			if (isWindows) {
 				if (location.equalsIgnoreCase(bundleLocation))
 					return true;
 				// may need to add a trailing slash to the location
 				if ((location+'/').equalsIgnoreCase(bundleLocation))
 					return true;
 			} else {
 				if (location.equals(bundleLocation))
 					return true;
 				// may need to add a trailing slash to the location
 				if ((location+'/').equals(bundleLocation))
 					return true;
 			}
 		}
 		return false;
 	}
 
 	private void loadOptions() {
 		// all this is only to get the application args		
 		DebugOptions service = null;
 		ServiceReference reference = context.getServiceReference(DebugOptions.class.getName());
 		if (reference != null)
 			service = (DebugOptions) context.getService(reference);
 		if (service == null)
 			return;
 		try {
 			DEBUG = service.getBooleanOption(OPTION_DEBUG, false);
 		} finally {
 			// we have what we want - release the service
 			context.ungetService(reference);
 		}
 	}
 	public static BundleContext getBundleContext() {
 		return context;
 	}
 	
 	
 }
