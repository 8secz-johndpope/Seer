 package org.eclipse.core.internal.boot;
 
 /*
  * Licensed Materials - Property of IBM,
  * WebSphere Studio Workbench
  * (c) Copyright IBM Corp 2000
  */
 import java.net.*;
 import java.util.*;
 import java.io.*;
 import java.security.CodeSource;
 import java.security.ProtectionDomain;
 import org.eclipse.core.boot.BootLoader;
 import com.ibm.oti.vm.VM;
 
 public abstract class DelegatingURLClassLoader extends URLClassLoader {
 
 	// loader base
 	protected URL base;
 
 	// delegation chain
 	protected DelegateLoader[] imports = null;
 
 	// extra resource class loader
 	protected URLClassLoader resourceLoader = null;
 
 	// filter table
 	private Hashtable filterTable = new Hashtable();
 
 	// development mode class path additions
 	public static String devClassPath = null;
 	// native library loading
 	private static String libPrefix;
 	private static String libSuffix;
 	private static final String WIN_LIBRARY_PREFIX = "";
 	private static final String WIN_LIBRARY_SUFFIX = ".dll";
 	private static final String UNIX_LIBRARY_PREFIX = "lib";
 	private static final String UNIX_LIBRARY_SUFFIX = ".so";
 	private Hashtable nativeLibs = new Hashtable();
 	private static final int BUF_SIZE = 32768;
 
 	// control class load tracing
 	public static boolean DEBUG = false;
 	public static boolean DEBUG_SHOW_CREATE = true;
 	public static boolean DEBUG_SHOW_ACTIVATE = true;
 	public static boolean DEBUG_SHOW_ACTIONS = true;
 	public static boolean DEBUG_SHOW_SUCCESS = true;
 	public static boolean DEBUG_SHOW_FAILURE = true;
 	public static String[] DEBUG_FILTER_CLASS = new String[0];
 	public static String[] DEBUG_FILTER_LOADER = new String[0];
 	public static String[] DEBUG_FILTER_RESOURCE = new String[0];
 	public static String[] DEBUG_FILTER_NATIVE = new String[0];
 	
 	private static boolean isHotSwapEnabled = InternalBootLoader.inDevelopmentMode();
 
 	// DelegateLoader. Represents a single class loader this loader delegates to.
 	protected static class DelegateLoader {
 
 		private DelegatingURLClassLoader loader;
 		private boolean isExported;
 
 		public DelegateLoader(DelegatingURLClassLoader loader, boolean isExported) {
 			this.loader = loader;
 			this.isExported = isExported;
 		}
 
 		public Class loadClass(String name, DelegatingURLClassLoader current, DelegatingURLClassLoader requestor, Vector seen) {
 			if (isExported || current == requestor)
 				return loader.loadClass(name, false, requestor, seen, false);
 			else
 				return null;
 		}
 
 		public URL findResource(String name, DelegatingURLClassLoader current, DelegatingURLClassLoader requestor, Vector seen) {
 			if (isExported || current == requestor)
 				return loader.findResource(name, requestor, seen);
 			else
 				return null;
 		}
 		public Enumeration findResources(String name, DelegatingURLClassLoader current, DelegatingURLClassLoader requestor, Vector seen) {
 			if (isExported || current == requestor)
 				return loader.findResources(name, requestor, seen);
 			else
 				return null;
 		}
 	}
 
 	// unchecked DelegatingLoaderException
 	protected static class DelegatingLoaderException extends RuntimeException {
 		Exception e = null;
 
 		public DelegatingLoaderException() {
 			super();
 		}
 
 		public DelegatingLoaderException(String message) {
 			super(message);
 		}
 
 		public DelegatingLoaderException(String message, Exception e) {
 			super(message);
 			this.e = e;
 		}
 
 		public Throwable getException() {
 			return e;
 		}
 		
 		public void printStackTrace() {
 			printStackTrace(System.err);
 		}
 		
 		public void printStackTrace(PrintStream output) {
 			synchronized (output) {
 				if (e != null) {
 					output.print("org.eclipse.core.internal.boot.DelegatingLoaderException: ");
 					e.printStackTrace(output);
 				} else
 					super.printStackTrace(output);
 			}
 		}
 		
 		public void printStackTrace(PrintWriter output) {
 			synchronized (output) {
 				if (e != null) {
 					output.print("org.eclipse.core.internal.boot.DelegatingLoaderException: ");
 					e.printStackTrace(output);
 				} else
 					super.printStackTrace(output);
 			}
 		}
 	}
 
 public DelegatingURLClassLoader(URL[] codePath, URLContentFilter[] codeFilters, URL[] resourcePath, URLContentFilter[] resourceFilters, ClassLoader parent) {
 	super(codePath, parent);
 	initialize();
 	if (resourcePath != null && resourcePath.length > 0)
 		resourceLoader = new ResourceLoader(resourcePath);
 
 	if (codePath != null) {
 		if (codeFilters == null || codeFilters.length != codePath.length)
 			throw new DelegatingLoaderException();
 		setJ9HotSwapPath(this, codePath);
 		for (int i = 0; i < codePath.length; i++) {
 			if (codeFilters[i] != null)
 				filterTable.put(codePath[i], codeFilters[i]);
 		}
 	}
 	if (resourcePath != null) {
 		if (resourceFilters == null || resourceFilters.length != resourcePath.length)
 			throw new DelegatingLoaderException();
 		for (int i = 0; i < resourcePath.length; i++) {
 			if (resourceFilters[i] != null)
 				filterTable.put(resourcePath[i], resourceFilters[i]);
 		}
 	}
 }
 /**
  * Returns the given class or <code>null</code> if the class is not visible to the
  * given requestor.  The <code>inCache</code> flag controls how this action is
  * reported if in debug mode.
  */
 protected Class checkClassVisibility(Class result, DelegatingURLClassLoader requestor, boolean inCache) {
 	if (result == null)
 		return null;
 	if (isClassVisible(result, requestor)) {
 		if (DEBUG && DEBUG_SHOW_SUCCESS && debugClass(result.getName()))
 			debug("found " + result.getName() + " in " + (inCache ? "cache" : getURLforClass(result).toExternalForm()));
 	} else {
 		if (DEBUG && DEBUG_SHOW_ACTIONS && debugClass(result.getName()))
 			debug("skip " + result.getName() + " in " + (inCache ? "cache" : getURLforClass(result).toExternalForm()));
 		return null;
 	}
 	return result;
 }
 /**
  * Returns the given resource URL or <code>null</code> if the resource is not visible to the
  * given requestor.  
  */
 protected URL checkResourceVisibility(String name, URL result, DelegatingURLClassLoader requestor) {
 	if (result == null)
 		return null;
 	if (isResourceVisible(name, result, requestor)) {
 		if (DEBUG && DEBUG_SHOW_SUCCESS && debugResource(name))
 			debug("found " + result);
 	} else {
 		if (DEBUG && DEBUG_SHOW_ACTIONS && debugResource(name))
 			debug("skip " + result);
 		result = null;
 	}
 	return result;
 }
 protected void debug(String s) {
 
 	System.out.println(toString()+"^"+Integer.toHexString(Thread.currentThread().hashCode())+" "+s);
 }
 protected boolean debugClass(String name) {
 	
 	if (debugLoader()) {
 		return debugMatchesFilter(name,DEBUG_FILTER_CLASS);
 	}
 	return false;
 }
 protected void debugConstruction() {
 	if (DEBUG && DEBUG_SHOW_CREATE && debugLoader()) {
 		URL[] urls = getURLs();
 		debug("Class Loader Created");
 		debug("> baseURL=" + base);
 		if (urls == null || urls.length == 0)
 			debug("> empty search path");
 		else {
 			URLContentFilter filter;
 			for (int i = 0; i < urls.length; i++) {
 				debug("> searchURL=" + urls[i].toString());
 				filter = (URLContentFilter) filterTable.get(urls[i]);
 				if (filter != null)
 					debug(">    export=" + filter.toString());
 			}
 		}
 	}
 }
 protected String debugId() {
 	return "";
 }
 protected boolean debugLoader() {
 	
 	return debugMatchesFilter(debugId(),DEBUG_FILTER_LOADER);
 }
 private boolean debugMatchesFilter(String name, String[] filter) {
 
 	if (filter.length==0) return false;
 	
 	for (int i=0; i<filter.length; i++) {
 		if (filter[i].equals("*")) return true;
 		if (name.startsWith(filter[i])) return true;
 	}
 	return false;
 }
 protected boolean debugNative(String name) {
 	
 	if (debugLoader()) {
 		return debugMatchesFilter(name,DEBUG_FILTER_NATIVE);
 	}
 	return false;
 }
 protected boolean debugResource(String name) {
 	
 	if (debugLoader()) {
 		return debugMatchesFilter(name,DEBUG_FILTER_RESOURCE);
 	}
 	return false;
 }
 /**
  * Looks for the requested class in the parent of this loader using
  * standard Java protocols.  If the parent is null then the system class
  * loader is consulted.  <code>null</code> is returned if the class could
  * not be found.
  */
 protected Class findClassParents(String name, boolean resolve) {
 	try {
 		ClassLoader parent = getParent();
 		if (parent == null)
 			return findSystemClass(name);
 		return parent.loadClass(name);
 	} catch (ClassNotFoundException e) {
 	}
 	return null;
 }
 /**
  * Finds and loads the class with the specified name from the URL search
  * path. Any URLs referring to JAR files are loaded and opened as needed
  * until the class is found.   Search on the parent chain and then self.
  *
  * Subclasses should implement this method.
  *
  * @param name the name of the class
  * @param resolve whether or not to resolve the class if found
  * @param requestor class loader originating the request
  * @param checkParents whether the parent of this loader should be consulted
  * @return the resulting class
  */
 protected abstract Class findClassParentsSelf(String name, boolean resolve, DelegatingURLClassLoader requestor, boolean checkParents);
 /**
  * Finds and loads the class with the specified name from the URL search
  * path. Any URLs referring to JAR files are loaded and opened as needed
  * until the class is found.  This method consults only the platform class loader.
  *
  * @param name the name of the class
  * @param resolve whether or not to resolve the class if found
  * @param requestor class loader originating the request
  * @param checkParents whether the parent of this loader should be consulted
  * @return the resulting class
  */
 protected Class findClassPlatform(String name, boolean resolve, DelegatingURLClassLoader requestor, boolean checkParents) {
 	DelegatingURLClassLoader platform = PlatformClassLoader.getDefault();
 	if (this == platform)
 		return null;
 	return platform.findClassParentsSelf(name, resolve, requestor, false);
 }
 /**
  * Finds and loads the class with the specified name from the URL search
  * path. Any URLs referring to JAR files are loaded and opened as needed
  * until the class is found.  This method considers only the classes loadable
  * by its explicit prerequisite loaders.
  *
  * @param name the name of the class
  * @param requestor class loader originating the request
  * @param seen list of delegated class loaders already searched
  * @return the resulting class
  */
 protected Class findClassPrerequisites(final String name, DelegatingURLClassLoader requestor, Vector seen) {
 	if (imports == null)
 		return null;
 	if (seen == null)
 		seen = new Vector(); // guard against delegation loops
 	seen.addElement(this);
 	// Grab onto the imports value to protect against concurrent write.
 	DelegateLoader[] loaders = imports;
 	for (int i = 0; i < loaders.length; i++) {
 		Class result = loaders[i].loadClass(name, this, requestor, seen);
 		if (result != null)
 			return result;
 	}
 	return null;
 }
 /**
  * Finds the resource with the specified name on the URL search path.
  * This method is used specifically to find the file containing a class to verify
  * that the class exists without having to load it.
  * Returns a URL for the resource.  Searches only this loader's classpath.
  * <code>null</code> is returned if the resource cannot be found.
  *
  * @param name the name of the resource
  */
 protected URL findClassResource(String name) {
 	return super.findResource(name);
 }
 /**
  * Returns the absolute path name of a native library. The VM
  * invokes this method to locate the native libraries that belong
  * to classes loaded with this class loader. If this method returns
  * <code>null</code>, the VM searches the library along the path
  * specified as the <code>java.library.path</code> property.
  *
  * @param      libname   the library name
  * @return     the absolute path of the native library
  */
 protected String basicFindLibrary(String libName) {
 	if (DEBUG && DEBUG_SHOW_ACTIONS && debugNative(libName))
 		debug("findLibrary(" + libName + ")");
 	if (base == null)
 		return null;
 
 	// wrap the last segment of the name with OS appropriate decorations
 	int i = libName.lastIndexOf('/');
 	String first = "";
 	String rest = "";
 	if (i == -1)
		first = libName;
 	else {	
 		first = libName.substring(0, i + 1);
 		rest = libName.substring(i + 1);
 	}
 	String osLibFileName = first + libPrefix + rest + libSuffix;
 
 	File libFile = null;
 	if (base.getProtocol().equals(PlatformURLHandler.FILE) || base.getProtocol().equals(PlatformURLHandler.VA)) {
 		// directly access library	
 		String libFileName = (base.getFile() + osLibFileName).replace('/', File.separatorChar);
 		libFile = new File(libFileName);
 	} else
 		if (base.getProtocol().equals(PlatformURLHandler.PROTOCOL))
 			// access library through eclipse URL
 			libFile = getNativeLibraryAsLocal(osLibFileName);
 
 	if (libFile == null)
 		return null;
 	if (!libFile.exists()) {
 		if (DEBUG && DEBUG_SHOW_FAILURE && debugNative(libName))
 			debug("not found " + libName);
 		return null; // can't find the file
 	}
 
 	if (DEBUG && DEBUG_SHOW_SUCCESS && debugNative(libName))
 		debug("found " + libName + " as " + libFile.getAbsolutePath());
 
 	return libFile.getAbsolutePath();
 }
 
 protected String findLibrary(String libName) {
 	if (libName.length() == 0)
 		return null;
 	if (libName.charAt(0) != '$')
 		return basicFindLibrary(libName);
 	int i = libName.indexOf('/', 1);
 	String first = "";
 	String rest = "";
 	String result = null;
 	if (i == -1)
 		first = libName;
 	else {	
 		first = libName.substring(0, i);
 		rest = libName.substring(i);
 	}
 	if (first.equalsIgnoreCase("$ws$")) 
 		result = basicFindLibrary("ws/" + InternalBootLoader.getWS() + rest);
 	else 
 		if (first.equalsIgnoreCase("$os$"))
 			result = basicFindLibrary("os/" + InternalBootLoader.getOS() + rest);
 		else
 			if (first.equalsIgnoreCase("$nl$"))
 				result = findLibraryNL(rest);
 	return result != null ? result : basicFindLibrary(rest);
 }
 
 /**
  * Scan all the possible NL-based locations in which there might be a 
  * requested library.
  */
 private String findLibraryNL(String libName) {
 	String nl = InternalBootLoader.getNL();
 	String result = null;
 	while (result == null && nl.length() > 0) {
 		String location = "nl/" + nl + libName;
 		result = basicFindLibrary(location);
 		int i = nl.lastIndexOf('_');
 		if (i < 0)
 			nl = "";
 		else
 			nl = nl.substring(0, i);
 	}
 	return result;
 }
 
 /**
  * Finds the resource with the specified name on the URL search path.
  * Returns a URL for the resource. If resource is not found in own 
  * URL search path, delegates search to prerequisite loaders.
  * Null is returned if none of the loaders find the resource.
  *
  * @param name the name of the resource
  */
 public URL findResource(String name) {
 	return findResource(name, this, null);
 }
 /**
  * Delegated resource access call. 
  * Does not check prerequisite loader parent chain.
  */
 protected URL findResource(String name, DelegatingURLClassLoader requestor, Vector seen) {
 	// guard against delegation loops
 	if (seen != null && seen.contains(this))
 		return null;
 
 	if (DEBUG && DEBUG_SHOW_ACTIONS && debugResource(name))
 		debug("findResource(" + name + ")");
 
 	// check the normal class path for self
 	URL result = super.findResource(name);
 	result = checkResourceVisibility(name, result, requestor);
 	if (result != null)
 		return result;
 
 	// check our extra resource path if any
 	if (resourceLoader != null) {
 		result = resourceLoader.findResource(name);
 		result = checkResourceVisibility(name, result, requestor);
 		if (result != null)
 			return result;
 	}
 
 	// delegate down the prerequisite chain if we haven't found anything yet.
 	if (imports != null) {
 		if (seen == null)
 			seen = new Vector(); // guard against delegation loops
 		seen.addElement(this);
 		for (int i = 0; i < imports.length && result == null; i++)
 			result = imports[i].findResource(name, this, requestor, seen);
 	}
 	return result;
 }
 /**
  * Returns an Enumeration of URLs representing all of the resources
  * on the URL search path having the specified name.
  *
  * @param name the resource name
  */
 public Enumeration findResources(String name) throws IOException {
 	return findResources(name, this, null);
 }
 /**
  * Delegated call to locate all named resources. 
  * Does not check prerequisite loader parent chain.
  */
 private Enumeration findResources(String name, DelegatingURLClassLoader requestor, Vector seen) {
 	// guard against delegation loops
 	if (seen != null && seen.contains(this))
 		return null;
 
 	if (DEBUG && DEBUG_SHOW_ACTIONS && debugResource(name))
 		debug("findResources(" + name + ")");
 
 	// check own URL search path
 	Enumeration e = null;
 	try {
 		e = super.findResources(name);
 	} catch (IOException ioe) {
 	}
 	ResourceEnumeration result = new ResourceEnumeration(name, e, this, requestor);
 
 	// delegate down the prerequisite chain
 	if (imports != null) {
 		if (seen == null)
 			seen = new Vector(); // guard against delegation loops
 		seen.addElement(this);
 		for (int i = 0; i < imports.length; i++)
 			result.add(imports[i].findResources(name, this, requestor, seen));
 	}
 
 	return result;
 }
 private File getNativeLibraryAsLocal(String osname) {
 	File result = null;
 	try {
 		URL liburl = new URL(base, osname);
 		PlatformURLConnection c = (PlatformURLConnection) liburl.openConnection();
 		URL localName = c.getURLAsLocal();
 		result = new File(localName.getFile());
 	} catch (IOException e) {
 	}
 	return result;
 }
 public URL getResource(String name) {
 	if (DEBUG && DEBUG_SHOW_ACTIONS && debugResource(name))
 		debug("getResource(" + name + ")");
 
 	URL result = super.getResource(name);
 	if (result == null) {
 		if (DEBUG && DEBUG_SHOW_FAILURE && debugResource(name))
 			debug("not found " + name);
 	}
 	return result;
 }
 private URL getURLforClass(Class clazz) {
 	ProtectionDomain pd = clazz.getProtectionDomain();
 	if (pd != null) {
 		CodeSource cs = pd.getCodeSource();
 		if (cs != null)
 			return cs.getLocation();
 	}
 	if (DEBUG && DEBUG_SHOW_ACTIONS && debugClass(clazz.getName()))
 		debug("*** " + clazz.getName());
 	return null;
 }
 private void initialize() {
 	if (BootLoader.getOS().equals(BootLoader.OS_WIN32)) {
 		libPrefix = WIN_LIBRARY_PREFIX;
 		libSuffix = WIN_LIBRARY_SUFFIX;
 	} else {
 		libPrefix = UNIX_LIBRARY_PREFIX;
 		libSuffix = UNIX_LIBRARY_SUFFIX;
 	}
 }
 public void initializeImportedLoaders() {
 }
 /**
  * check to see if class is visible (exported)
  */
 boolean isClassVisible(Class clazz, DelegatingURLClassLoader requestor) {
 	URL lib = getURLforClass(clazz);
 	if (lib == null)
 		return true; // have a system class (see comment below)
 
 	URLContentFilter filter = (URLContentFilter) filterTable.get(lib);
 	if (filter == null) {
 		// This code path is being executed because some VMs (eg. Sun JVM)
 		// return from the class cache classes that were not loaded
 		// by this class loader. Consequently we do not find the 
 		// corresponding jar filter. This appears to be a performance
 		// optimization that we are defeating with our filtering scheme.
 		// We return the class if it is a system class (see above). Otherwise
 		// we reject the class which caused the load to be
 		// delegated down the prerequisite chain until we find the
 		// correct loader.
 		if (DEBUG && DEBUG_SHOW_ACTIONS && debugClass(clazz.getName()))
 			debug("*** Unable to find library filter for " + clazz.getName() + " from " + lib);
 		return false;
 	} else
 		return filter.isClassVisible(clazz, this, requestor);
 }
 /**
  * check to see if resource is visible (exported)
  */
 boolean isResourceVisible(String name, URL resource, DelegatingURLClassLoader requestor) {
 	URL lib = null;
 	String file = resource.getFile();
 	try {
 		lib = new URL(resource.getProtocol(), resource.getHost(), file.substring(0, file.length() - name.length()));
 	} catch (MalformedURLException e) {
 		if (DEBUG)
 			debug("Unable to determine resource lib for " + name + " from " + resource);
 		return false;
 	}
 
 	URLContentFilter filter = (URLContentFilter) filterTable.get(lib);
 	if (filter == null) {
 		if (DEBUG)
 			debug("Unable to find library filter for " + name + " from " + lib);
 		return false;
 	} else
 		return filter.isResourceVisible(name, this, requestor);
 }
 /**
  * Non-delegated load call.  This method is not synchronized.  Implementations of
  * findClassParentsSelf, and perhaps others, should synchronize themselves as
  * required.  Synchronizing this method is too coarse-grained.   It results in plugin
  * activation being synchronized and may cause deadlock.
  */
 protected Class loadClass(String name, boolean resolve) throws ClassNotFoundException {
 	if (DEBUG && DEBUG_SHOW_ACTIONS && debugClass(name))
 		debug("loadClass(" + name + ")");
 	Class result = loadClass(name, resolve, this, null, true);
 	if (result == null) {
 		if (DEBUG && DEBUG_SHOW_FAILURE && debugClass(name))
 			debug("not found " + name);
 		throw new ClassNotFoundException(name);
 	}
 	return result;
 }
 protected void enableJ9HotSwap(ClassLoader cl, Class clazz) {
 	if (isHotSwapEnabled)
 		VM.enableClassHotSwap(clazz);
 }
 
 
 /**
  * Delegated load call.  This method is not synchronized.  Implementations of
  * findClassParentsSelf, and perhaps others, should synchronize themselves as
  * required.  Synchronizing this method is too coarse-grained.   It results in plugin
  * activation being synchronized and may cause deadlock.
  */
 private Class loadClass(String name, boolean resolve, DelegatingURLClassLoader requestor, Vector seen, boolean checkParents) {
 	// guard against delegation loops
 	if (seen != null && seen.contains(this))
 		return null;
 
 	// look in the parents and self
 	Class result = findClassParentsSelf(name, resolve, requestor, checkParents);
 
 	// search platform
 	if (result == null)
 		result = findClassPlatform(name, resolve, requestor, false);
 
 	// search prerequisites
 	if (result == null)
 		result = findClassPrerequisites(name, requestor, seen);
 
 	// if we found a class, consider resolving it
 	if (result != null && resolve)
 		resolveClass(result);
 
 	return result;
 }
 
 private void setJ9HotSwapPath(ClassLoader cl, URL[] urls) {
 	if (!isHotSwapEnabled)
 		return;
 	StringBuffer path = new StringBuffer();
 	for(int i = 0; i < urls.length; i++) {
 		String file = getFileFromURL (urls[i]);
 		if (file != null) {
 			if (file.charAt(0) == '/')
 				file = file.substring(1, file.length());
 			if (file.charAt(file.length() - 1) == '/')
 				file = file.substring(0, file.length() - 1);
 			if (path.length() > 0)
 				path.append(";");
 			path.append(file);
 		}
 	}
 	if (path.length() > 0)
 		VM.setClassPathImpl(cl, path.toString());
 }
 
 protected String getFileFromURL(URL target) {
 	String protocol = target.getProtocol();
 	if (protocol.equals(PlatformURLHandler.FILE))
 		return target.getFile();
 //	if (protocol.equals(PlatformURLHandler.VA))
 //		return target.getFile();
 	if (protocol.equals(PlatformURLHandler.JAR)) {
 		// strip off the jar separator at the end of the url then do a recursive call
 		// to interpret the sub URL.
 		String file = target.getFile();
 		file = file.substring(0, file.length() - PlatformURLHandler.JAR_SEPARATOR.length());
 		try {
 			return getFileFromURL(new URL(file));
 		} catch (MalformedURLException e) {
 		}
 	}
 	return null;
 }
 
 protected void setImportedLoaders(DelegateLoader[] loaders) {
 	
 	imports = loaders;
 	
 	if(DEBUG && DEBUG_SHOW_CREATE && debugLoader()) {
 		debug("Imports");
 		if (imports==null || imports.length==0) debug("> none");
 		else {
 			for (int i=0; i<imports.length; i++) {
 				debug("> " + imports[i].loader.toString() + " export=" + imports[i].isExported);
 			}
 		}
 	}
 }
 public String toString() {
 	return "Loader [" + debugId() + "]";
 }
 }
