 /*******************************************************************************
  * Copyright (c) 2004 IBM Corporation and others.
  * All rights reserved. This program and the accompanying materials 
  * are made available under the terms of the Common Public License v1.0
  * which accompanies this distribution, and is available at
  * http://www.eclipse.org/legal/cpl-v10.html
  * 
  * Contributors:
  *     IBM Corporation - initial API and implementation
  *******************************************************************************/
 
 package org.eclipse.osgi.framework.internal.core;
 
 import java.io.IOException;
 import java.net.URL;
 import java.security.*;
 import java.util.*;
 import org.eclipse.osgi.framework.adaptor.*;
 import org.eclipse.osgi.framework.debug.Debug;
 import org.eclipse.osgi.framework.util.SecureAction;
 import org.eclipse.osgi.service.resolver.*;
 import org.eclipse.osgi.util.ManifestElement;
 import org.osgi.framework.*;
 
 /**
  * This object is created when the bundle
  * loaded at framework launch or bundle install or update.
  *
  * It represents the loaded state of the bundle.
  * 
  */
 public class BundleLoader implements ClassLoaderDelegate {
 	private static String DEFAULT_PACKAGE = ".";
 
 	/** Bundle object */
 	protected BundleHost bundle;
 
 	/** The is the BundleClassLoader for the bundle */
 	protected BundleClassLoader classloader;
 
 	/** Single object for permission checks */
 	protected BundleResourcePermission resourcePermission;
 
 	/**
 	 *  Hashtable of imported packages. Key is packagename, Value is BundleLoader
 	 */
 	protected KeyedHashSet importedPackages;
 
 	protected boolean hasDynamicImports = false;
 	/**
 	 * If true, import all packages dynamically.
 	 */
 	protected boolean dynamicImportPackageAll;
 	/**
 	 * If not null, list of package stems to import dynamically.
 	 */
 	protected String[] dynamicImportPackageStems;
 	/**
 	 * If not null, list of package names to import dynamically.
 	 */
 	protected String[] dynamicImportPackages;
 
 	protected KeyedHashSet providedPackages;
 	protected KeyedHashSet requiredPackagesCache;
 	protected BundleLoaderProxy[] requiredBundles;
 	protected int[] reexportTable;
 
 	/**
 	 * Returns the package name from the specified class name.
 	 * The returned package is dot seperated.
 	 *
 	 * @param name   Name of a class.
 	 * @return Dot separated package name or null if the class
 	 *         has no package name.
 	 */
 	protected static String getPackageName(String name) {
 		if (name != null) {
 			int index = name.lastIndexOf('.'); /* find last period in class name */
 			if (index > 0)
 				return name.substring(0, index);
 		}
 		return null;
 	}
 
 	/**
 	 * Returns the package name from the specified resource name.
 	 * The returned package is dot seperated.
 	 *
 	 * @param name   Name of a resource.
 	 * @return Dot separated package name or null if the resource
 	 *         has no package name.
 	 */
 	protected static String getResourcePackageName(String name) {
 		if (name != null) {
 			/* check for leading slash*/
 			int begin = ((name.length() > 1) && (name.charAt(0) == '/')) ? 1 : 0;
 			int end = name.lastIndexOf('/'); /* index of last slash */
 			if (end > begin)
 				return name.substring(begin, end).replace('/', '.');
 		}
 		return null;
 	}
 
 	/**
 	 * Bundle runtime constructor. This object is created when the bundle is
 	 * loaded at framework launch or bundle install or update.
 	 *
 	 * @param bundle Bundle object for this loader.
 	 * @param file BundleFile for this object
 	 * @param manifest Bundle's manifest
 	 * @exception org.osgi.framework.BundleException
 	 */
 	protected BundleLoader(BundleHost bundle, org.eclipse.osgi.service.resolver.BundleDescription description) throws BundleException {
 		this.bundle = bundle;
 		try {
 			bundle.getBundleData().open(); /* make sure the BundleData is open */
 		} catch (IOException e) {
 			throw new BundleException(Msg.formatter.getString("BUNDLE_READ_EXCEPTION"), e);
 		}
 		initialize(description);
 	}
 
 	protected void initialize(BundleDescription description) {
 		hasDynamicImports = SystemBundleLoader.getSystemPackages() != null;
 
 		//This is the fastest way to access to the description for fragments since the hostdescription.getFragments() is slow
 		org.osgi.framework.Bundle[] fragmentObjects = bundle.getFragments();
 		BundleDescription[] fragments = new BundleDescription[fragmentObjects == null ? 0 : fragmentObjects.length];
 		for (int i = 0; i < fragments.length; i++) {
 			fragments[i] = ((Bundle) fragmentObjects[i]).getBundleDescription();
 		}
 
 		// init the imported packages list taking the bundle...
 		addImportedPackages(description.getPackages());
 		// ...and its fragments
 		for (int i = 0; i < fragments.length; i++)
 			if (fragments[i].isResolved())
 				addImportedPackages(fragments[i].getPackages());
 
 		// init the require bundles list.  Need to account for optional bundles so bundles with
 		// no supplier should be skipped.
 		BundleSpecification[] required = description.getRequiredBundles();
 		ArrayList bundles = new ArrayList(Arrays.asList(required == null ? new BundleSpecification[0] : required));
 		for (int i = 0; i < fragments.length; i++)
 			if (fragments[i].isResolved()) {
 				BundleSpecification[] fragmentRequires = fragments[i].getRequiredBundles();
 				if (fragmentRequires != null)
 					bundles.addAll(Arrays.asList(fragmentRequires));
 			}
 		if (bundles.size() > 0) {
 			ArrayList bound = new ArrayList(bundles.size());
 			int[] reexported = new int[bundles.size()];
 			int reexportIndex = 0;
 			for (int i = 0; i < bundles.size(); i++) {
 				BundleSpecification spec = (BundleSpecification) bundles.get(i);
 				if (spec.isResolved()) {
 					String bundleKey = new StringBuffer(spec.getName()).append("_").append(spec.getActualVersion().toString()).toString();
 
 					BundleLoaderProxy loaderProxy = (BundleLoaderProxy) bundle.framework.packageAdmin.exportedBundles.getByKey(bundleKey);
 					if (loaderProxy != null) {
 						bound.add(loaderProxy);
 						if (spec.isExported())
 							reexported[reexportIndex++] = i;
 					} else {
 						// TODO log error
 						System.out.println("Could not find loaderProxy: " + bundleKey);
 					}
 				}
 			}
 			requiredBundles = (BundleLoaderProxy[]) bound.toArray(new BundleLoaderProxy[bound.size()]);
 			if (reexportIndex > 0) {
 				reexportTable = new int[reexportIndex];
 				System.arraycopy(reexported, 0, reexportTable, 0, reexportIndex);
 			}
 		}
 
 		// init the provided packages
 		String[] provides = description.getProvidedPackages();
 		ArrayList packages = new ArrayList(Arrays.asList(required == null ? new String[0] : provides));
 		for (int i = 0; i < fragments.length; i++)
 			if (fragments[i].isResolved()) {
 				String[] fragmentProvides = fragments[i].getProvidedPackages();
 				if (fragmentProvides != null)
 					packages.addAll(Arrays.asList(fragmentProvides));
 			}
 		if (packages.size() > 0) {
 			providedPackages = new KeyedHashSet(packages.size());
 			for (int i = 0; i < packages.size(); i++)
 				providedPackages.add(new SingleSourcePackage((String) packages.get(i), bundle.getLoaderProxy()));
 		}
 
 		// init the dynamic imports tables
 		try {
 			String spec = bundle.getBundleData().getDynamicImports();
 			ManifestElement[] imports = ManifestElement.parseHeader(Constants.DYNAMICIMPORT_PACKAGE,spec);
 			addDynamicImportPackage(imports);
 			// ...and its fragments
 			for (int i = 0; i < fragments.length; i++)
 				if (fragments[i].isResolved()) {
 					spec = ((Bundle) fragmentObjects[i]).getBundleData().getDynamicImports();
 					imports = ManifestElement.parseHeader(Constants.DYNAMICIMPORT_PACKAGE,spec);
 					addDynamicImportPackage(imports);
 				}
 		} catch (BundleException e) {
 			// TODO log an error
 		}
 	}
 
 	protected void initializeFragment(Bundle fragment) throws BundleException {
 		BundleDescription description = fragment.getBundleDescription();
 		// if the fragment imports a package not already imported throw an exception
 		PackageSpecification[] packages = description.getPackages();
 		if (packages != null && packages.length > 0)
 			for (int i = 0; i < packages.length; i++)
 				if (importedPackages == null || importedPackages.getByKey(packages[i].getName()) == null)
 					throw new BundleException(Msg.formatter.getString("BUNDLE_FRAGMENT_IMPORT_CONFLICT",packages[i].getName()));
 
 
 
 		// if the fragment requires a bundle not aready required throw an exception
 		BundleSpecification[] fragReqBundles = description.getRequiredBundles();
 		if (fragReqBundles != null && fragReqBundles.length > 0) {
 			if (requiredBundles == null) 
 				throw new BundleException(Msg.formatter.getString("BUNDLE_FRAGMENT_REQUIRE_CONFLICT",fragReqBundles[0].getName()));
 
 			for (int i = 0; i < fragReqBundles.length; i++){
 				boolean found = false;
 				for (int j = 0; j < requiredBundles.length; j++){
 					String fragReqKey = new StringBuffer(fragReqBundles[i].getName()).append("_").append(fragReqBundles[i].getActualVersion().toString()).toString();
 					if (fragReqKey.equals(requiredBundles[j].getKey()))
 						found = true;
 				}
 				if (!found)
 					throw new BundleException(Msg.formatter.getString("BUNDLE_FRAGMENT_REQUIRE_CONFLICT",fragReqBundles[i].getName()));
 			}
 		}
 
 
 		// if the fragment dynamically imports a package not aready 
 		// dynamically imported throw an exception.
 		try {
 			String spec = fragment.getBundleData().getDynamicImports();
 			ManifestElement[] imports = ManifestElement.parseHeader(Constants.DYNAMICIMPORT_PACKAGE,spec);
 			if (imports != null && imports.length > 0){
 				for (int i = 0; i < imports.length; i++) {
 					String name = imports[i].getValue();
 					if (!isDynamicallyImported(name))
 						throw new BundleException(Msg.formatter.getString("BUNDLE_FRAGMENT_DYNAMICIMPORT_CONFLICT",imports[i]));
 				}
 			}
 		} catch (BundleException e) {
 			// TODO log an error
 		}		
 
 		// init the provided packages
 		String[] provides = description.getProvidedPackages();
 		if (provides != null) {
 			if (providedPackages == null)
 				providedPackages = new KeyedHashSet(provides.length);
 			for (int i = 0; i < provides.length; i++)
 				if (providedPackages.getByKey(provides[i]) == null)
 					providedPackages.add(new SingleSourcePackage((String) provides[i], bundle.getLoaderProxy()));
 		}
 
 	}
 
 	private void addImportedPackages(PackageSpecification[] packages) {
 		if (packages != null && packages.length > 0) {
 			if (importedPackages == null) {
 				importedPackages = new KeyedHashSet();
 			}
 			for (int i = 0; i < packages.length; i++) {
 				SingleSourcePackage packagesource = (SingleSourcePackage) bundle.framework.packageAdmin.exportedPackages.getByKey(packages[i].getName());
 				if (packagesource != null) {
 					importedPackages.add(packagesource);
 				}
 			}
 		}
 	}
 
 	/**
 	 * Close the the BundleLoader.
 	 *
 	 */
 	protected void close() {
 		if (bundle == null)
 			return;
 		importedPackages = null;
 
 		if (classloader != null)
 			classloader.close();
 		classloader = null;
 		bundle = null; /* This indicates the BundleLoader is destroyed */
 	}
 
 	/**
 	 * This method loads a class from the bundle.  The class is searched for in the
 	 * same manner as it would if it was being loaded from a bundle (i.e. all
 	 * hosts, fragments, import, required bundles and local resources are searched.
 	*
 	 * @param      name     the name of the desired Class.
 	 * @return     the resulting Class
 	 * @exception  java.lang.ClassNotFoundException  if the class definition was not found.
 	 */
 	protected Class loadClass(String name) throws ClassNotFoundException {
 		return createClassLoader().loadClass(name);
 	}
 
 	/**
 	 * This method gets a resource from the bundle.  The resource is searched 
 	 * for in the same manner as it would if it was being loaded from a bundle 
 	 * (i.e. all hosts, fragments, import, required bundles and 
 	 * local resources are searched).
 	 *
 	 * @param name the name of the desired resource.
 	 * @return the resulting resource URL or null if it does not exist.
 	 */
 	protected URL getResource(String name) {
 		return createClassLoader().getResource(name);
 	}
 
 	/**
 	 * Handle the lookup where provided classes can also be imported.
 	 * In this case the exporter need to be consulted. 
 	 */ 
 	protected Class requireClass(String name, String packageName){
 		Class result = null;
 		try {
 			result = findImportedClass(name, packageName);
 		} catch (ImportClassNotFoundException e) {
 			//Capture the exception and return null because we want to continue the lookup.
 			return null; 
 		}
 		if (result == null)
 			result = findLocalClass(name);
 		return result;
 	}
 	
 	protected BundleClassLoader createClassLoader() {
 		if (classloader != null)
 			return classloader;
 		synchronized (this) {
 			if (classloader != null)
 				return classloader;
 
 			try {
 				String[] classpath = getClassPath(bundle, SecureAction.getProperties());
 				if (classpath != null) {
 					classloader = createBCLPrevileged(bundle.getProtectionDomain(), classpath);
 				} else {
 					bundle.framework.publishFrameworkEvent(FrameworkEvent.ERROR, bundle, new BundleException(Msg.formatter.getString("BUNDLE_NO_CLASSPATH_MATCH")));
 				}
 			} catch (BundleException e) {
 				bundle.framework.publishFrameworkEvent(FrameworkEvent.ERROR, bundle, e);
 			}
 
 		}
 		return classloader;
 	}
 
 	/**
 	 * Finds a class local to this bundle.  Only the classloader for this bundle is searched.
 	 * @param name The name of the class to find.
 	 * @return The loaded Class or null if the class is not found.
 	 */
 	protected Class findLocalClass(String name) {
 		if (Debug.DEBUG && Debug.DEBUG_LOADER)
 			Debug.println("BundleLoader[" + this +"].findLocalClass(" + name + ")");
 		try {
 			Class clazz = createClassLoader().findLocalClass(name);
 			if (Debug.DEBUG && Debug.DEBUG_LOADER && clazz != null)
 				Debug.println("BundleLoader[" + this +"] found local class " + name);
 			return clazz;
 		} catch (ClassNotFoundException e) {
 			return null;
 		}
 	}
 
 	/**
 	 * Finds the class for a bundle.  This method is used for delegation by the bundle's classloader.
 	 */
 	public Class findClass(String name) throws ClassNotFoundException {
 		if (isClosed())
 			throw new ClassNotFoundException(name);
 
 		if (Debug.DEBUG && Debug.DEBUG_LOADER) {
 			Debug.println("BundleLoader[" + this +"].loadBundleClass(" + name + ")");
 		}
 
 		String packageName = getPackageName(name);
 
 		Class result = null;
 		if (packageName != null)
 			result = findImportedClass(name, packageName);
 
 		// Allow default package lookups from required bundles.
 		if (result == null)
 			result = findRequiredClass(name, packageName);
 
 		if (result == null) {
 			result = findLocalClass(name);
 			if (result == null) {
 				throw new ClassNotFoundException(name);
 			}
 		}
 
 		return result;
 	}
 
 	boolean isClosed() {
 		return bundle == null;
 	}
 	/**
 	 * Finds the resource for a bundle.  This method is used for delegation by the bundle's classloader.
 	 */
 	public URL findResource(String name) {
 		if (isClosed())
 			return null;
 		if ((name.length() > 1) && (name.charAt(0) == '/')) /* if name has a leading slash */
 			name = name.substring(1); /* remove leading slash before search */
 
 		try {
 			checkResourcePermission();
 		} catch (SecurityException e) {
 			try {
 				bundle.framework.checkAdminPermission();
 			} catch (SecurityException ee) {
 				return null;
 			}
 		}
 		String packageName = getResourcePackageName(name);
 
 		URL resource = null;
 		if (packageName != null)
 			resource = findImportedResource(name, packageName);
 
 		// Allow default package lookups from required bundles.
 		if (resource == null)
 			resource = findRequiredResource(name, packageName);
 
 		if (resource == null)
 			resource = findLocalResource(name);
 
 		return resource;
 	}
 
 	/**
 	 * Finds the resources for a bundle.  This  method is used for delegation by the bundle's classloader.
 	 */
 	public Enumeration findResources(String name) throws IOException {
 		if (isClosed())
 			return null;
 		if ((name.length() > 1) && (name.charAt(0) == '/')) /* if name has a leading slash */
 			name = name.substring(1); /* remove leading slash before search */
 
 		try {
 			checkResourcePermission();
 		} catch (SecurityException e) {
 			try {
 				bundle.framework.checkAdminPermission();
 			} catch (SecurityException ee) {
 				return null;
 			}
 		}
 		String packageName = getResourcePackageName(name);
 
 		Enumeration result = null;
 		if (packageName != null)
 			result = findImportedResources(name, packageName);
 
 		// Allow default package lookups from required bundles.
 		if (result == null)
 			result = findRequiredResources(name, packageName);
 
 		if (result == null)
 			result = findLocalResources(name);
 
 		return result;
 	}
 
 	/**
 	 * Handle the lookup where provided resources can also be imported.
 	 * In this case the exporter need to be consulted. 
 	 */ 
 	protected URL requireResource(String name, String packageName){
 		URL result = null;
 		try {
 			result = findImportedResource(name, packageName);
 		} catch (ImportResourceNotFoundException e) {
 			//Capture the exception and return null because we want to continue the lookup.
 			return null; 
 		}
 		if (result == null)
 			result = findLocalResource(name);
 		return result;
 	}
 
 	/**
 	 * Finds a resource local to this bundle.  Only the classloader for this bundle is searched.
 	 * @param name The name of the resource to find.
 	 * @return The URL to the resource or null if the resource is not found.
 	 */
 	protected URL findLocalResource(final String name) {
 		if (System.getSecurityManager() == null)
 			return createClassLoader().findLocalResource(name);
 		return (URL) AccessController.doPrivileged(new PrivilegedAction() {
 			public Object run() {
 				return createClassLoader().findLocalResource(name);
 			}
 		});
 	}
 
 	/**
 	 * Handle the lookup where provided resources can also be imported.
 	 * In this case the exporter need to be consulted. 
 	 */ 
 	protected Enumeration requireResources(String name, String packageName){
 		Enumeration result = null;
 		try {
 			result = findImportedResources(name, packageName);
 		} catch (ImportResourceNotFoundException e) {
 			//Capture the exception and return null because we want to continue the lookup.
 			return null; 
 		}
 		if (result == null)
 			result = findLocalResources(name);
 		return result;
 	}
 
 	/**
 	 * Returns an Enumeration of URLs representing all the resources with
 	 * the given name. Only the classloader for this bundle is searched.
 	 *
 	 * @param  name the resource name
 	 * @return an Enumeration of URLs for the resources
 	 * @throws IOException if I/O errors occur
 	 */
 	protected Enumeration findLocalResources(String name) {
 		if ((name.length() > 1) && (name.charAt(0) == '/')) /* if name has a leading slash */
 			name = name.substring(1);
 		try {
 			checkResourcePermission();
 		} catch (SecurityException e) {
 			return null;
 		}
 		return createClassLoader().findLocalResources(name);
 	}
 
 	/**
 	 * Finds the object for a bundle.  This method is used for delegation by the bundle's classloader.
 	 */
 	public Object findObject(String object) {
 		if (isClosed())
 			return null;
 		if ((object.length() > 1) && (object.charAt(0) == '/')) /* if name has a leading slash */
 			object = object.substring(1); /* remove leading slash before search */
 
 		try {
 			checkResourcePermission();
 		} catch (SecurityException e) {
 			try {
 				bundle.framework.checkAdminPermission();
 			} catch (SecurityException ee) {
 				return null;
 			}
 		}
 		String packageName = getResourcePackageName(object);
 
 		Object result = null;
 		if (packageName != null)
 			result = findImportedObject(object, packageName);
 
 		// Allow default package lookups from required bundles.
 		if (result == null)
 			result = findRequiredObject(object, packageName);
 
 		if (result == null)
 			result = findLocalObject(object);
 
 		return result;
 	}
 
 	/**
 	 * Handle the lookup where provided resources can also be imported.
 	 * In this case the exporter need to be consulted. 
 	 */ 
 	protected Object requireObject(String object, String packageName){
 		Object result = null;
 		try {
 			result = findImportedObject(object, packageName);
 		} catch (ImportResourceNotFoundException e) {
 			//Capture the exception and return null because we want to continue the lookup.
 			return null; 
 		}
 		if (result == null)
 			result = findLocalObject(object);
 		return result;
 	}
 	
 	protected Object findLocalObject(String object) {
 		return createClassLoader().findLocalObject(object);
 	}
 
 	/**
 	 * Returns the absolute path name of a native library.
 	 *
 	 * @param      name   the library name
 	 * @return     the absolute path of the native library or null if not found
 	 */
 	public String findLibrary(final String name) {
 		if (isClosed())
 			return null;
 		if (System.getSecurityManager() == null)
 			return findLocalLibrary(name);
 		return (String) AccessController.doPrivileged(new PrivilegedAction() {
 			public Object run() {
 				return findLocalLibrary(name);
 			}
 		});
 	}
 
 	protected String findLocalLibrary(final String name) {
 		String result = bundle.getBundleData().findLibrary(name);
 		if (result != null)
 			return result;
 
 		org.osgi.framework.Bundle[] fragments = bundle.getFragments();
 		if (fragments == null || fragments.length == 0)
 			return null;
 
 		// look in fragments imports ...
 		for (int i = 0; i < fragments.length; i++) {
 			result = ((Bundle) fragments[i]).getBundleData().findLibrary(name);
 			if (result != null)
 				return result;
 		}
 		return result;
 	}
 
 	/**
 	 * Return the bundle we are associated with.
 	 *
 	 */
 	protected Bundle getBundle() {
 		return bundle;
 	}
 
 	private BundleClassLoader createBCLPrevileged(final ProtectionDomain pd, final String[] cp) {
 		// Create the classloader as previleged code if security manager is present.
 		if (System.getSecurityManager() == null)
 			return createBCL(pd,cp);
 		else
 			return (BundleClassLoader)AccessController.doPrivileged(new PrivilegedAction() {
 				public Object run() {
 					return createBCL(pd,cp);
 				}
 			});
 
 	}
 
 	private BundleClassLoader createBCL(final ProtectionDomain pd, final String[] cp) {
 		BundleClassLoader bcl = bundle.getBundleData().createClassLoader(BundleLoader.this, pd, cp);
 		// attach existing fragments to classloader
 		org.osgi.framework.Bundle[] fragments = bundle.getFragments();
 		if (fragments != null)
 			for (int i = 0; i < fragments.length; i++) {
 				Bundle fragment = (Bundle) fragments[i];
 				try {
 					bcl.attachFragment(fragment.getBundleData(), fragment.domain, getClassPath(fragment, SecureAction.getProperties()));
 				}
 				catch (BundleException be) {
 					bundle.framework.publishFrameworkEvent(FrameworkEvent.ERROR, bundle, be);
 				}
 			}
 
 		// finish the initialization of the classloader.
 		bcl.initialize();
 		
 		return bcl;
 	}
 
 	/**
 	 * Return a string representation of this loader.
 	 *
 	 * @return String
 	 */
 	public String toString() {
 		BundleData result = bundle.getBundleData();
 		return result == null ? "BundleLoader.bundledata == null!" : result.toString();
 	}
 
 	protected void checkResourcePermission() {
 		SecurityManager sm = System.getSecurityManager();
 		if (sm != null) {
 			if (resourcePermission == null)
 				resourcePermission = new BundleResourcePermission(bundle.getBundleId());
 			sm.checkPermission(resourcePermission);
 		}
 	}
 
 	/**
 	 * Get the BundleLoader for the package if it is imported.
 	 *
 	 * @param pkgname The name of the package to import.
 	 * @return BundleLoader to load from or null if the package is not imported.
 	 */
 	protected BundleLoader getPackageExporter(String pkgname) {
 		if (importedPackages != null) {
 			PackageSource exporter = (PackageSource) importedPackages.getByKey(pkgname);
 			if (exporter != null)
 				return exporter.getSupplier().getBundleLoader();
 		}
 
 		if (isDynamicallyImported(pkgname)) {
 			PackageSource exporter = (PackageSource) bundle.framework.packageAdmin.exportedPackages.getByKey(pkgname);
 			if (exporter != null) {
 				exporter.getSupplier().markUsed(bundle.getLoaderProxy());
 				importedPackages.add(exporter);
 				return exporter.getSupplier().getBundleLoader();
 			}
 		}
 		return null;
 	}
 
 	/**
 	 * Return true if the target package name matches
 	 * a name in the DynamicImport-Package manifest header.
 	 *
 	 * @param pkgname The name of the requested class' package.
 	 * @return true if the package should be imported.
 	 */
 	protected boolean isDynamicallyImported(String pkgname) {
 		// must check for startsWith("java.") to satisfy R3 section 4.7.2
 		if (pkgname.startsWith("java."))
 			return true;
 
 		/* quick shortcut check */
 		if (!hasDynamicImports) {
 			return false;
 		}
 
 		/* "*" shortcut */
 		if (dynamicImportPackageAll)
 			return true;
 
 		/* 
 		 * If including the system bundle packages by default, dynamically import them.
 		 * Most OSGi framework implementations assume the system bundle packages
 		 * are on the VM classpath.  As a result some bundles neglect to import
 		 * framework packages (e.g. org.osgi.framework).
 		 */
 		String[] systemPackages = SystemBundleLoader.getSystemPackages();
 		if (systemPackages != null) {
 			for (int i = 0; i < systemPackages.length; i++)
 				if (pkgname.equals(systemPackages[i]))
 					return true;
 		}
 
 		/* match against specific names */
 		if (dynamicImportPackages != null)
 			for (int i = 0; i < dynamicImportPackages.length; i++)
 				if (pkgname.equals(dynamicImportPackages[i]))
 					return true;
 
 		/* match against names with trailing wildcards */
 		if (dynamicImportPackageStems != null)
 			for (int i = 0; i < dynamicImportPackageStems.length; i++)
 				if (pkgname.startsWith(dynamicImportPackageStems[i]))
 					return true;
 
 		return false;
 	}
 
 	/**
 	 * Find a class using the imported packages for this bundle.  Only the 
 	 * ImportClassLoader is used for the search. 
 	 * @param name The name of the class to find.
 	 * @return The loaded class or null if the class does not belong to a package
 	 * that is imported by the bundle.
 	 * @throws ImportClassNotFoundException If the class does belong to a package
 	 * that is imported by the bundle but the class is not found.
 	 */
 	protected Class findImportedClass(String name, String packageName) throws ImportClassNotFoundException {
 		if (Debug.DEBUG && Debug.DEBUG_LOADER)
 			Debug.println("ImportClassLoader[" + this +"].findImportedClass(" + name + ")");
 
 		Class result = null;
 
 		try {
 			BundleLoader exporter = getPackageExporter(packageName);
 			if (exporter != null) {
 				result = exporter.findLocalClass(name);
 				if (result == null)
 					throw new ImportClassNotFoundException(name);
 			}
 		} finally {
 			if (result == null) {
 				if (Debug.DEBUG && Debug.DEBUG_LOADER)
 					Debug.println("ImportClassLoader[" + this +"] class " + name + " not found in imported package " + packageName);
 			} else {
 				if (Debug.DEBUG && Debug.DEBUG_LOADER)
 					Debug.println("BundleLoader[" + this +"] found imported class " + name);
 			}
 		}
 		return result;
 	}
 
 	protected void addExportedProvidersFor(String packageName, ArrayList result, KeyedHashSet visited) {
 		if (!visited.add(bundle))
 			return;
 
 		// See if we locally provide the package.
 		PackageSource local = getProvidedPackage(packageName);
 		
 		// Must search required bundles that are exported first.
 		if (requiredBundles != null) {
 			int size = reexportTable == null ? 0 : reexportTable.length;
 			int reexportIndex = 0;
 			for (int i = 0; i < requiredBundles.length; i++) {
 				if (local != null) {
 					// always add required bundles first if we locally provide the package
 					// This allows a bundle to provide a package from a required bundle without 
 					// re-exporting the whole required bundle.
 					requiredBundles[i].getBundleLoader().addExportedProvidersFor(packageName, result, visited);
 				}
 				else if (reexportIndex < size && reexportTable[reexportIndex] == i) {
 					reexportIndex++;
 					requiredBundles[i].getBundleLoader().addExportedProvidersFor(packageName, result, visited);
 				}
 			}
 		}
 
 		// now add the locally provided package.
 		if (local != null)
 			result.add(local.getSupplier());
 	}
 
 	/**
 	 * Find a class using the required bundles for this bundle.  Only the
 	 * required bundles are used to search for the class.
 	 * @param name The name of the class to find.
 	 * @return The loaded class or null if the class is not found.
 	 */
 	protected PackageSource getProvidersFor(String packageName) {
 		if (packageName == null)
 			packageName = DEFAULT_PACKAGE;
 		// first look in the required packages cache
 		if (requiredPackagesCache != null) {
 			PackageSource result = (PackageSource) requiredPackagesCache.getByKey(packageName);
 
 			if (result != null) {
 				if (result.isNullSource()) {
 					return null;
 				} else {
 					return result;
 				}
 			}
 		}
 
 		// didn't find it in the cache search the actual required bundles
 		if (requiredBundles == null)
 			return null;
 		KeyedHashSet visited = new KeyedHashSet(false);
 		ArrayList result = new ArrayList(3);
 		for (int i = 0; i < requiredBundles.length; i++) {
 			BundleLoader requiredLoader = requiredBundles[i].getBundleLoader();
 			requiredLoader.addExportedProvidersFor(packageName, result, visited);
 		}
 
 		// found some so cache the result for next time and return
 		if (requiredPackagesCache == null)
 			requiredPackagesCache = new KeyedHashSet();
 		if (result.size() == 0) {
 			// did not find it in our required bundles lets record the failure
 			// so we do not have to do the search again for this package.
 			requiredPackagesCache.add(new NullPackageSource(packageName));
 			return null;
 		} else if (result.size() == 1) {
 			// if there is just one source, remember just the single source
 			BundleLoaderProxy bundle = (BundleLoaderProxy) result.get(0);
 			PackageSource source = new SingleSourcePackage(packageName, bundle);
 			requiredPackagesCache.add(source);
 			return source;
 		} else {
 			// if there was more than one source, build a multisource and cache that.
 			BundleLoaderProxy[] bundles = (BundleLoaderProxy[]) result.toArray(new BundleLoaderProxy[result.size()]);
 			MultiSourcePackage source = new MultiSourcePackage(packageName, bundles);
 			requiredPackagesCache.add(source);
 			return source;
 		}
 	}
 
 	/**
 	 * Find a class using the required bundles for this bundle.  Only the
 	 * required bundles are used to search for the class.
 	 * @param name The name of the class to find.
 	 * @return The loaded class or null if the class is not found.
 	 */
 	protected Class findRequiredClass(String name, String packageName) {
 		if (Debug.DEBUG && Debug.DEBUG_LOADER)
 			Debug.println("ImportClassLoader[" + this +"].findRequiredClass(" + name + ")");
 		PackageSource source = getProvidersFor(packageName);
 		if (source == null)
 			return null;
 		if (source.isMultivalued()) {
 			BundleLoaderProxy[] bundles = source.getSuppliers();
 			for (int i = 0; i < bundles.length; i++) {
 				Class result = bundles[i].getBundleLoader().requireClass(name,packageName);
 				if (result != null)
 					return result;
 			}
 		} else
 			return source.getSupplier().getBundleLoader().requireClass(name,packageName);
 		return null;
 	}
 
 	protected PackageSource getProvidedPackage(String name) {
 		return providedPackages == null ? null : (PackageSource) providedPackages.getByKey(name);
 	}
 
 	/**
 	 * Find a resource using the imported packages for this bundle.  Only the 
 	 * ImportClassLoader is used for the search. 
 	 * @param name The name of the resource to find.
 	 * @return The URL of the resource or null if the resource does not belong to a package
 	 * that is imported by the bundle.
 	 * @throws ImportResourceNotFoundException If the resource does belong to a package
 	 * that is imported by the bundle but the resource is not found.
 	 */
 	protected URL findImportedResource(String name, String packageName) {
 		if (Debug.DEBUG && Debug.DEBUG_LOADER)
 			Debug.println("ImportClassLoader[" + this +"].findImportedResource(" + name + ")");
 
 		BundleLoader exporter = getPackageExporter(packageName);
 		if (exporter != null) {
 			URL url = exporter.findLocalResource(name);
 			if (url != null)
 				return url;
 			if (Debug.DEBUG && Debug.DEBUG_LOADER)
 				Debug.println("ImportClassLoader[" + this +"] resource " + name + " not found in imported package " + packageName);
 			throw new ImportResourceNotFoundException(name);
 		}
 		return null;
 	}
 
 	/**
 	 * Find a resource using the required bundles for this bundle.  Only the
 	 * required bundles are used to search.
 	 * @param name The name of the resource to find.
 	 * @return The URL for the resource or null if the resource is not found.
 	 */
 	protected URL findRequiredResource(String name, String packageName) {
 		if (Debug.DEBUG && Debug.DEBUG_LOADER)
 			Debug.println("ImportClassLoader[" + this +"].findRequiredResource(" + name + ")");
 		PackageSource source = getProvidersFor(packageName);
 		if (source == null)
 			return null;
 		if (source.isMultivalued()) {
 			BundleLoaderProxy[] bundles = source.getSuppliers();
 			for (int i = 0; i < bundles.length; i++) {
 				URL result = bundles[i].getBundleLoader().requireResource(name,packageName);
 				if (result != null)
 					return result;
 			}
 		} else
 			return source.getSupplier().getBundleLoader().requireResource(name,packageName);
 		return null;
 	}
 
 	/**
 	 * Returns an Enumeration of URLs representing all the resources with
 	 * the given name.
 	 *
 	 * If the resource is in a package that is imported, call the exporting
 	 * bundle. Otherwise return null.
 	 *
 	 * @param  name the resource name
 	 * @return an Enumeration of URLs for the resources if the package is
 	 * imported, null otherwise.
 	 * @throws IOException if I/O errors occur
 	 */
 	protected Enumeration findImportedResources(String name, String packageName) {
 		if (Debug.DEBUG && Debug.DEBUG_LOADER)
 			Debug.println("ImportClassLoader[" + this +"].findImportedResources(" + name + ")");
 
 		BundleLoader exporter = getPackageExporter(packageName);
 		if (exporter != null)
 			return exporter.findLocalResources(name);
 		return null;
 	}
 
 	/**
 	 * Returns an Enumeration of URLs representing all the resources with
 	 * the given name.
 	 * Find the resources using the required bundles for this bundle.  Only the
 	 * required bundles are used to search.
 	 *
 	 * If the resource is in a package that is imported, call the exporting
 	 * bundle. Otherwise return null.
 	 *
 	 * @param  name the resource name
 	 * @return an Enumeration of URLs for the resources if the package is
 	 * imported, null otherwise.
 	 * @throws IOException if I/O errors occur
 	 */
 	protected Enumeration findRequiredResources(String name, String packageName) {
 		if (Debug.DEBUG && Debug.DEBUG_LOADER)
 			Debug.println("ImportClassLoader[" + this +"].findRequiredResources(" + name + ")");
 		PackageSource source = getProvidersFor(packageName);
 		if (source == null)
 			return null;
 		if (source.isMultivalued()) {
 			BundleLoaderProxy[] bundles = source.getSuppliers();
 			for (int i = 0; i < bundles.length; i++) {
 				Enumeration result = bundles[i].getBundleLoader().requireResources(name,packageName);
 				if (result != null)
 					return result;
 			}
 		} else
 			return source.getSupplier().getBundleLoader().requireResources(name,packageName);
 		return null;
 	}
 
 	/**
 	 * Find an object using the imported packages for this bundle.  Only the 
 	 * ImportClassLoader is used for the search. 
 	 * @param object The name of the object to find.
 	 * @return The Object or null if the object does not belong to a package
 	 * that is imported by the bundle.
 	 * @throws ImportResourceNotFoundException If the object does belong to a package
 	 * that is imported by the bundle but the resource is not found.
 	 */
 	protected Object findImportedObject(String object, String packageName) {
 		if (Debug.DEBUG && Debug.DEBUG_LOADER)
 			Debug.println("ImportClassLoader[" + this +"].findImportedObject(" + object + ")");
 
 		BundleLoader exporter = getPackageExporter(packageName);
 		if (exporter != null) {
 			Object result = exporter.findLocalObject(object);
 			if (result != null)
 				return result;
 			if (Debug.DEBUG && Debug.DEBUG_LOADER)
 				Debug.println("ImportClassLoader[" + this +"] object " + object + " not found in imported package " + packageName);
 			throw new ImportResourceNotFoundException(object);
 		}
 		return null;
 	}
 
 	/**
 	 * Find an object using the required bundles for this bundle.  Only the
 	 * required bundles are used to search.
 	 * @param name The name of the object to find.
 	 * @return The Object or null if the object is not found.
 	 */
 	protected Object findRequiredObject(String name, String packageName) {
 		if (Debug.DEBUG && Debug.DEBUG_LOADER)
 			Debug.println("ImportClassLoader[" + this +"].findRequiredResource(" + name + ")");
 		PackageSource source = getProvidersFor(packageName);
 		if (source == null)
 			return null;
 		if (source.isMultivalued()) {
 			BundleLoaderProxy[] bundles = source.getSuppliers();
 			for (int i = 0; i < bundles.length; i++) {
 				Object result = bundles[i].getBundleLoader().requireObject(name,packageName);
 				if (result != null)
 					return result;
 			}
 		} else
 			return source.getSupplier().getBundleLoader().requireObject(name,packageName);
 		return null;
 	}
 
 	/**
 	 * Adds a list of DynamicImport-Package manifest elements to the dynamic
 	 * import tables of this BundleLoader.  Duplicate packages are checked and
 	 * not added again.  This method is not thread safe.  Callers should ensure
 	 * synchronization when calling this method.
 	 * @param packages the DynamicImport-Package elements to add.
 	 */
 	public void addDynamicImportPackage(ManifestElement[] packages) {
 		if (packages == null && SystemBundleLoader.getSystemPackages() == null)
 			return;
 
 		hasDynamicImports = true;
 		// make sure importedPackages is not null;
 		if (importedPackages == null) {
 			importedPackages = new KeyedHashSet();
 		}
 
 		if (packages == null)
 			return;
 
 		int size = packages.length;
 		ArrayList stems;
 		if (dynamicImportPackageStems == null) {
 			stems = new ArrayList(size);
 		} else {
 			stems = new ArrayList(size + dynamicImportPackageStems.length);
 			for (int i = 0; i < dynamicImportPackageStems.length; i++) {
 				stems.add(dynamicImportPackageStems[i]);
 			}
 		}
 
 		ArrayList names;
 		if (dynamicImportPackages == null) {
 			names = new ArrayList(size);
 		} else {
 			names = new ArrayList(size + dynamicImportPackages.length);
 			for (int i = 0; i < dynamicImportPackages.length; i++) {
 				names.add(dynamicImportPackages[i]);
 			}
 		}
 
 		for (int i = 0; i < size; i++) {
 			String name = packages[i].getValue();
 			if (isDynamicallyImported(name))
 				continue;
 			if (name.equals("*")) { /* shortcut */
 				dynamicImportPackageAll = true;
 				return;
 			}
 
 			if (name.endsWith(".*"))
 				stems.add(name.substring(0, name.length() - 1));
 			else
 				names.add(name);
 		}
 
 		size = stems.size();
 		if (size > 0)
 			dynamicImportPackageStems = (String[]) stems.toArray(new String[size]);
 
 		size = names.size();
 		if (size > 0)
 			dynamicImportPackages = (String[]) names.toArray(new String[size]);
 	}
 
 	protected void clear() {
 		providedPackages = null;
 		requiredBundles = null;
 		importedPackages = null;
 		dynamicImportPackages = null;
 		dynamicImportPackageStems = null;
 	}
 
 	protected void attachFragment(BundleFragment fragment, Properties props) throws BundleException{
 		initializeFragment(fragment);
 		if (classloader == null)
 			return;
 
 		try {
 			String[] classpath = getClassPath(fragment, props);
 			if (classpath != null)
 				classloader.attachFragment(fragment.getBundleData(), fragment.domain, classpath);
 			else
 				bundle.framework.publishFrameworkEvent(FrameworkEvent.ERROR, bundle, new BundleException(Msg.formatter.getString("BUNDLE_NO_CLASSPATH_MATCH")));
 		} catch (BundleException e) {
 			bundle.framework.publishFrameworkEvent(FrameworkEvent.ERROR, bundle, e);
 		}
 
 	}
 
 	protected String[] getClassPath(Bundle bundle, Properties props) throws BundleException {
 		String spec = bundle.getBundleData().getClassPath();
 		ManifestElement[] classpathElements = ManifestElement.parseHeader(Constants.BUNDLE_CLASSPATH,spec);
 		return matchClassPath(classpathElements, props);
 	}
 
 	protected String[] matchClassPath(ManifestElement[] classpath, Properties props) {
 		if (classpath == null) {
 			if (Debug.DEBUG && Debug.DEBUG_LOADER)
 				Debug.println("  no classpath");
 			/* create default BundleClassPath */
 			return new String[] { "." };
 		}
 
 		ArrayList result = new ArrayList(10);
 		for (int i = 0; i < classpath.length; i++) {
 			Filter filter;
 			try {
 				filter = createFilter(classpath[i].getAttribute("selection-filter"));
 				if (filter == null || filter.match(props)) {
 					if (Debug.DEBUG && Debug.DEBUG_LOADER)
 						Debug.println("  found match for classpath entry " + classpath[i].getValueComponents());
 					String[] matchPaths = classpath[i].getValueComponents();
 					for(int j=0; j<matchPaths.length; j++){
 						result.add(matchPaths[j]);
 					}
 				}
 			} catch (InvalidSyntaxException e) {
 				bundle.framework.publishFrameworkEvent(FrameworkEvent.ERROR, bundle, e);
 			} catch (BundleException ex) {
 				bundle.framework.publishFrameworkEvent(FrameworkEvent.ERROR, bundle, ex);
 			}
 		}
 		return (String[]) result.toArray(new String[result.size()]);
 	}
 
 	protected Filter createFilter(String filterString) throws InvalidSyntaxException, BundleException {
 		if (filterString == null)
 			return null;
 		int length = filterString.length();
 		if (length <= 2) {
 			throw new BundleException(Msg.formatter.getString("MANIFEST_INVALID_HEADER_EXCEPTION", Constants.BUNDLE_CLASSPATH, filterString));
 		}
 		return new Filter(filterString);
 	}
 
 }
