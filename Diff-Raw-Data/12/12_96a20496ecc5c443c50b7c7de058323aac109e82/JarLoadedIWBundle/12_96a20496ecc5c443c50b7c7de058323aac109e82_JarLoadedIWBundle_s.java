 /**
  * 
  */
 package com.idega.idegaweb;
 
 import java.io.File;
 import java.io.FileNotFoundException;
 import java.io.IOException;
 import java.io.InputStream;
 import java.util.Locale;
 import java.util.Properties;
 import java.util.jar.JarEntry;
 import java.util.logging.Level;
 import java.util.logging.Logger;
 import com.idega.util.SortedProperties;
 
 
 /**
  * <p>
  * Implementation of an IWBundle loaded from a jar file instead of a folder
  * </p>
 *  Last modified: $Date: 2008/01/23 09:35:25 $ by $Author: alexis $
  * 
  * @author <a href="mailto:tryggvil@idega.com">tryggvil</a>
 * @version $Revision: 1.10 $
  */
 public class JarLoadedIWBundle extends DefaultIWBundle {
 
 	private static final Logger LOGGER = Logger.getLogger(JarLoadedIWBundle.class.getName());
 	private JarModule jarModule;
 	
 	/**
 	 * @param rootRealPath
 	 * @param bundleIdentifier
 	 * @param superApplication
 	 */
 	public JarLoadedIWBundle(JarModule module, IWMainApplication superApplication) {
 		this.jarModule=module;
 		String realPath = superApplication.getBundlesRealPath() + File.separator + module.getModuleIdentifier()+".bundle";
 		String virtualPath = "/idegaweb/bundles/"+module.getModuleIdentifier()+".bundle";
 		initialize(realPath, virtualPath, module.getModuleIdentifier(), superApplication, false);
 	}
 	
 	/**
 	 * <p>
 	 * Initializes a IWPropertyList relative to the 'properties' folder within the bundle
 	 * Overrided from superclass to fetch the file within the jar file.
 	 * </p>
 	 * @param pathWithinPropertiesFolder
 	 * @return
 	 */
 	protected IWPropertyList initializePropertyList(String pathWithinPropertiesFolder, boolean autocreate) {
 		IWPropertyList propList = null;
 		String filePathWithinBundle = "properties/"+pathWithinPropertiesFolder;
 		InputStream inStream = null;
 		try {
 			inStream = getResourceInputStream(filePathWithinBundle);
 		} catch (IOException e) {
 			LOGGER.warning(e.getMessage());
 		}
 		if (inStream == null) {
 			propList = new IWPropertyList(getPropertiesRealPath(), pathWithinPropertiesFolder, autocreate);
 		}
 		else {
 			propList = new IWPropertyList(inStream);
 		}
 		return propList;
 	}
 
 	public boolean doesResourceExist(String pathWithinBundle){
 		JarEntry entry = jarModule.getJarEntry(pathWithinBundle);
 		if (entry != null) {
 			return true;
 		}
 		return false;
 	}
 	
 	public InputStream getResourceInputStream(String pathWithinBundle) throws IOException {
 		JarEntry entry = jarModule.getJarEntry(pathWithinBundle);
 		
 		if (entry == null) {
 			throw new FileNotFoundException("File not found inside jar module " + jarModule.getModuleIdentifier() + ": " + pathWithinBundle);
 		}
 		InputStream inStream = jarModule.getInputStream(entry);
 		return inStream;
 	}
 	
 	/**
 	 * Returns time of jar entry identified by <code>pathWithinBundle</code>.
 	 * @param pathWithinBundle resource path within jar file
 	 * @return modification time of an entry, 0 if not found, or -1 if not specified
 	 */
 	public long getResourceTime(String pathWithinBundle) {
 		JarEntry entry = jarModule.getJarEntry(pathWithinBundle);
 		return (entry != null ? entry.getTime() : 0);
 	}
 
 	/*
 	 * (non-Javadoc)
 	 * @see com.idega.idegaweb.DefaultIWBundle#unload(boolean)
 	 */
 	public synchronized void unload(boolean storeState) {
 		super.unload(storeState);
 		this.jarModule=null;	
 	}
 
 	protected String getLocalizedResourcePath(Locale locale){
 		return "resources/" + locale.toString() + ".locale";
 	}
 	
 	/*
 	 * (non-Javadoc)
 	 * @see com.idega.idegaweb.DefaultIWBundle#initializeResourceBundle(java.util.Locale)
 	 */
 	protected IWResourceBundle initializeResourceBundle(Locale locale) throws IOException {
 		IWResourceBundle theReturn;
 		try {
 			InputStream defaultInputStream = getResourceInputStream(getLocalizedResourcePath(locale) + "/" + getLocalizedStringsFileName());
 			IWResourceBundle defaultLocalizedResourceBundle = new IWResourceBundle(this, defaultInputStream, locale);
 			if (isUsingLocalVariants()) {
 				String variantPath = getLocalizedResourcePath(locale)+"/"+getLocalizedStringsVariantFileName();
 				if (doesResourceExist(variantPath)) {
 					InputStream variantStream = getResourceInputStream(variantPath);
 					theReturn = new IWResourceBundle(defaultLocalizedResourceBundle, variantStream, locale);
 				}
 				else {
					File file = new File(getResourcesRealPath(locale), getLocalizedStringsFileName());
					theReturn = new IWResourceBundle(defaultLocalizedResourceBundle, file, locale);
 				}
 			}
 			else {
				theReturn = defaultLocalizedResourceBundle;
 			}
 		}
 		catch (IOException e) {
 			// if any error occurs, try default way (autocreated resources in webapp's bundle directory)
 			theReturn = super.initializeResourceBundle(locale);
 		}
 		return theReturn;
 	}
 	
 	/*
 	 * (non-Javadoc)
 	 * @see com.idega.idegaweb.DefaultIWBundle#initializeLocalizableStrings()
 	 */
 	protected Properties initializeLocalizableStrings() {
 		Properties locProps = new SortedProperties();
 		try {
 			locProps.load(getResourceInputStream("resources/" + getLocalizableStringsFileName()));
 			// localizableStringsMap = new TreeMap(localizableStringsProperties);
 		}
 		catch (IOException ex) {
 			LOGGER.log(Level.WARNING, null, ex);
 		}
 		return locProps;
 	}
 	
 }
