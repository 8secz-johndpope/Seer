 /*******************************************************************************
  * Copyright (c) 2007, 2008 IBM Corporation and others. All rights reserved. This
  * program and the accompanying materials are made available under the terms of
  * the Eclipse Public License v1.0 which accompanies this distribution, and is
  * available at http://www.eclipse.org/legal/epl-v10.html
  * 
  * Contributors: IBM Corporation - initial API and implementation
  ******************************************************************************/
 package org.eclipse.equinox.internal.p2.engine;
 
 import java.io.*;
 import java.lang.ref.SoftReference;
 import java.net.MalformedURLException;
 import java.net.URL;
 import java.util.*;
 import java.util.Map.Entry;
 import javax.xml.parsers.ParserConfigurationException;
 import org.eclipse.core.runtime.IStatus;
 import org.eclipse.core.runtime.Status;
 import org.eclipse.equinox.internal.p2.core.helpers.*;
 import org.eclipse.equinox.internal.p2.installregistry.*;
 import org.eclipse.equinox.internal.p2.persistence.XMLWriter;
 import org.eclipse.equinox.p2.core.eventbus.ProvisioningEventBus;
 import org.eclipse.equinox.p2.core.location.AgentLocation;
 import org.eclipse.equinox.p2.engine.*;
 import org.eclipse.equinox.p2.metadata.IInstallableUnit;
 import org.eclipse.equinox.p2.metadata.query.InstallableUnitQuery;
 import org.eclipse.equinox.p2.query.Collector;
 import org.eclipse.osgi.service.datalocation.Location;
 import org.eclipse.osgi.service.resolver.VersionRange;
 import org.eclipse.osgi.util.NLS;
 import org.osgi.framework.BundleContext;
 import org.osgi.framework.Version;
 import org.xml.sax.*;
 
 public class SimpleProfileRegistry implements IProfileRegistry {
 	private static String STORAGE = "profileRegistry.xml"; //$NON-NLS-1$
 
 	/**
 	 * Reference to Map of String(Profile id)->Profile. 
 	 */
 	SoftReference profiles;
 
 	OrderedProperties properties = new OrderedProperties();
 
 	private String self;
 
 	//Whether the registry has been loaded at all in this session
 	private boolean restored = false;
 
 	public SimpleProfileRegistry() {
 		self = EngineActivator.getContext().getProperty("eclipse.p2.profile"); //$NON-NLS-1$
 	}
 
 	/**
 	 * If the current profile for self is marked as a roaming profile, we need
 	 * to update its install and bundle pool locations.
 	 */
 	private void updateRoamingProfile(Map profileMap) {
 		if (profileMap == null)
 			return;
 		Profile selfProfile = (Profile) profileMap.get(self);
 		if (selfProfile == null)
 			return;
 		//only update if self is a roaming profile
 		if (!Boolean.valueOf(selfProfile.getProperty(IProfile.PROP_ROAMING)).booleanValue())
 			return;
 		Location installLocation = (Location) ServiceHelper.getService(EngineActivator.getContext(), Location.class.getName(), Location.INSTALL_FILTER);
 		File location = new File(installLocation.getURL().getPath());
 		boolean changed = false;
 		if (!location.equals(new File(selfProfile.getProperty(IProfile.PROP_INSTALL_FOLDER)))) {
 			selfProfile.setProperty(IProfile.PROP_INSTALL_FOLDER, location.getAbsolutePath());
 			changed = true;
 		}
 		if (!location.equals(new File(selfProfile.getProperty(IProfile.PROP_CACHE)))) {
 			selfProfile.setProperty(IProfile.PROP_CACHE, location.getAbsolutePath());
 			changed = true;
 		}
 		if (changed)
 			persist();
 	}
 
 	public synchronized String toString() {
 		return this.profiles.toString();
 	}
 
 	public synchronized IProfile getProfile(String id) {
 		if (SELF.equals(id))
 			id = self;
 		IProfile profile = (IProfile) getProfileMap().get(id);
 		if (profile == null)
 			return null;
 		return copyProfile(profile);
 	}
 
 	public synchronized IProfile[] getProfiles() {
 		Map profileMap = getProfileMap();
 		Profile[] result = new Profile[profileMap.size()];
 		int i = 0;
 		for (Iterator it = profileMap.values().iterator(); it.hasNext(); i++) {
 			IProfile profile = (IProfile) it.next();
 			result[i] = copyProfile(profile);
 		}
 		return result;
 	}
 
 	/**
 	 * Returns an initialized map of String(Profile id)->Profile. 
 	 */
 	protected Map getProfileMap() {
 		if (profiles != null) {
 			Map result = (Map) profiles.get();
 			if (result != null)
 				return result;
 		}
 		Map result = restore();
 		if (result == null)
 			result = new LinkedHashMap(8);
 		profiles = new SoftReference(result);
		if (!restored) {
			//update roaming profile on first load
			restored = true;
			updateRoamingProfile(result);
		}
 		return result;
 	}
 
 	public synchronized void updateProfile(Profile toUpdate) {
 		String id = toUpdate.getProfileId();
 		if (SELF.equals(id))
 			id = self;
 		Map profileMap = getProfileMap();
 		if (profileMap.get(id) == null)
 			throw new IllegalArgumentException("Profile to be updated does not exist:" + id); //$NON-NLS-1$
 		doUpdateProfile(toUpdate, profileMap);
 		broadcastChangeEvent(id, ProfileEvent.CHANGED);
 	}
 
 	public IProfile addProfile(String id) {
 		return addProfile(id, null, null);
 	}
 
 	public IProfile addProfile(String id, Map properties) {
 		return addProfile(id, properties, null);
 	}
 
 	public synchronized IProfile addProfile(String id, Map properties, String parentId) {
 		if (SELF.equals(id))
 			id = self;
 		Map profileMap = getProfileMap();
 		if (profileMap.get(id) != null)
 			throw new IllegalArgumentException(NLS.bind(Messages.Profile_Duplicate_Root_Profile_Id, id));
 
 		Profile parent = null;
 		if (parentId != null) {
 			if (SELF.equals(parentId))
 				parentId = self;
 			parent = (Profile) profileMap.get(parentId);
 			if (parent == null)
 				throw new IllegalArgumentException(NLS.bind(Messages.Profile_Parent_Not_Found, parentId));
 		}
 
 		InstallRegistry installRegistry = (InstallRegistry) ServiceHelper.getService(EngineActivator.getContext(), IInstallRegistry.class.getName());
 		if (installRegistry == null)
 			throw new IllegalStateException("InstallRegisty not available");
 		IProfileInstallRegistry profileInstallRegistry = installRegistry.createProfileInstallRegistry(id);
 
 		IProfile toAdd = new Profile(id, parent, properties);
 
 		installRegistry.addProfileInstallRegistry(profileInstallRegistry);
 		profileMap.put(id, toAdd);
 		persist();
 		broadcastChangeEvent(id, ProfileEvent.ADDED);
 		return copyProfile(toAdd);
 	}
 
 	private void doUpdateProfile(Profile toUpdate, Map profileMap) {
 		InstallRegistry installRegistry = (InstallRegistry) ServiceHelper.getService(EngineActivator.getContext(), IInstallRegistry.class.getName());
 		if (installRegistry == null)
 			return;
 
 		IProfileInstallRegistry profileInstallRegistry = installRegistry.createProfileInstallRegistry(toUpdate.getProfileId());
 		Iterator it = toUpdate.query(InstallableUnitQuery.ANY, new Collector(), null).iterator();
 		while (it.hasNext()) {
 			IInstallableUnit iu = (IInstallableUnit) it.next();
 			profileInstallRegistry.addInstallableUnits(iu);
 			OrderedProperties iuProperties = toUpdate.getInstallableUnitProperties(iu);
 			for (Iterator propIt = iuProperties.entrySet().iterator(); propIt.hasNext();) {
 				Entry propertyEntry = (Entry) propIt.next();
 				String key = (String) propertyEntry.getKey();
 				String value = (String) propertyEntry.getValue();
 				profileInstallRegistry.setInstallableUnitProfileProperty(iu, key, value);
 			}
 		}
 		installRegistry.addProfileInstallRegistry(profileInstallRegistry);
 
 		Profile current = (Profile) profileMap.get(toUpdate.getProfileId());
 		if (current == null) {
 			Profile parent = (Profile) toUpdate.getParentProfile();
 			if (parent != null)
 				parent = (Profile) profileMap.get(parent.getProfileId());
 			profileMap.put(toUpdate.getProfileId(), new Profile(toUpdate.getProfileId(), parent, toUpdate.getLocalProperties()));
 		} else {
 			current.clearLocalProperties();
 			current.addProperties(toUpdate.getLocalProperties());
 		}
 
 		// TODO: persists should be grouped some way to ensure they are consistent
 		persist();
 	}
 
 	public synchronized void removeProfile(String profileId) {
 		InstallRegistry installRegistry = (InstallRegistry) ServiceHelper.getService(EngineActivator.getContext(), IInstallRegistry.class.getName());
 		if (installRegistry == null)
 			return;
 
 		//note we need to maintain a reference to the profile map until it is persisted to prevent gc
 		Map profileMap = getProfileMap();
 
 		if (profileMap.remove(profileId) == null)
 			return;
 		installRegistry.removeProfileInstallRegistry(profileId);
 		persist();
 		broadcastChangeEvent(profileId, ProfileEvent.REMOVED);
 	}
 
 	private Profile copyProfile(IProfile profile) {
 		Profile parent = (Profile) profile.getParentProfile();
 		if (parent != null)
 			parent = copyProfile(parent);
 
 		Profile copy = new Profile(profile.getProfileId(), parent, profile.getLocalProperties());
 		String[] subProfileIds = profile.getSubProfileIds();
 		for (int i = 0; i < subProfileIds.length; i++) {
 			copy.addSubProfile(subProfileIds[i]);
 		}
 		return copy;
 	}
 
 	private void broadcastChangeEvent(String profileId, byte reason) {
 		((ProvisioningEventBus) ServiceHelper.getService(EngineActivator.getContext(), ProvisioningEventBus.class.getName())).publishEvent(new ProfileEvent(profileId, reason));
 	}
 
 	private URL getRegistryLocation() {
 		AgentLocation agent = (AgentLocation) ServiceHelper.getService(EngineActivator.getContext(), AgentLocation.class.getName());
 		try {
 			return new URL(agent.getDataArea(EngineActivator.ID), STORAGE);
 		} catch (MalformedURLException e) {
 			//this is not possible because we know the above URL is valid
 		}
 		return null;
 	}
 
 	/**
 	 * Restores the profile registry from disk, and returns the loaded profile map.
 	 * Returns <code>null</code> if unable to read the registry.
 	 */
 	private Map restore() {
 		Map loadedMap = null;
 		try {
 			BufferedInputStream bif = null;
 			try {
 				bif = new BufferedInputStream(getRegistryLocation().openStream());
 				Parser parser = new Parser(EngineActivator.getContext(), EngineActivator.ID);
 				parser.parse(bif);
 				loadedMap = parser.getProfileMap();
 				IStatus result = parser.getStatus();
 				if (!result.isOK())
 					LogHelper.log(result);
 			} finally {
 				if (bif != null)
 					bif.close();
 			}
 		} catch (FileNotFoundException e) {
 			//This is ok.
 		} catch (IOException e) {
 			LogHelper.log(new Status(IStatus.ERROR, EngineActivator.ID, "Error restoring profile registry", e)); //$NON-NLS-1$
 		}
 		return loadedMap;
 	}
 
 	private void persist() {
 		OutputStream os;
 		try {
 			Location agent = (Location) ServiceHelper.getService(EngineActivator.getContext(), AgentLocation.class.getName());
 			if (agent == null) {
 				LogHelper.log(new Status(IStatus.ERROR, EngineActivator.ID, "Unable to persist profile registry due to missing AgentLocation")); //$NON-NLS-1$
 				return;
 			}
 			URL registryLocation = getRegistryLocation();
 			if (!registryLocation.getProtocol().equals("file")) //$NON-NLS-1$
 				throw new IOException(NLS.bind(Messages.SimpleProfileRegistry_Persist_To_Non_File_URL_Error, registryLocation));
 
 			File outputFile = new File(registryLocation.toExternalForm().substring(5));
 			if (!outputFile.getParentFile().exists() && !outputFile.getParentFile().mkdirs())
 				throw new RuntimeException(NLS.bind(Messages.SimpleProfileRegistry_Cannot_Create_File_Error, outputFile));
 			os = new BufferedOutputStream(new FileOutputStream(outputFile));
 			try {
 				Writer writer = new Writer(os);
 				writer.write(this);
 			} finally {
 				os.close();
 			}
 		} catch (IOException e) {
 			LogHelper.log(new Status(IStatus.ERROR, EngineActivator.ID, "Error persisting profile registry", e)); //$NON-NLS-1$
 		}
 
 	}
 
 	public synchronized Map getProperties() {
 		return properties;
 	}
 
 	public synchronized String getProperty(String key) {
 		return properties.getProperty(key);
 	}
 
 	public synchronized void setProperty(String key, String value) {
 		properties.setProperty(key, value);
 	}
 
 	public synchronized void removeProperty(String key) {
 		properties.remove(key);
 	}
 
 	private interface XMLConstants extends org.eclipse.equinox.internal.p2.persistence.XMLConstants {
 
 		// Constants defining the structure of the XML for a SimpleProfileRegistry
 
 		// A format version number for simple profile registry XML.
 		public static final Version CURRENT_VERSION = new Version(0, 0, 2);
 		public static final Version COMPATIBLE_VERSION = new Version(0, 0, 1);
 		public static final VersionRange XML_TOLERANCE = new VersionRange(COMPATIBLE_VERSION, true, new Version(2, 0, 0), false);
 
 		// Constants for processing instructions
 		public static final String PI_REPOSITORY_TARGET = "profileRegistry"; //$NON-NLS-1$
 		public static XMLWriter.ProcessingInstruction[] PI_DEFAULTS = new XMLWriter.ProcessingInstruction[] {XMLWriter.ProcessingInstruction.makeClassVersionInstruction(PI_REPOSITORY_TARGET, SimpleProfileRegistry.class, CURRENT_VERSION)};
 
 		// Constants for profile registry elements
 		public static final String REGISTRY_ELEMENT = "profileRegistry"; //$NON-NLS-1$
 
 	}
 
 	protected class Writer extends ProfileWriter implements XMLConstants {
 
 		public Writer(OutputStream output) throws IOException {
 			super(output, PI_DEFAULTS);
 		}
 
 		/**
 		 * Write the given SimpleProfileRegistry to the output stream.
 		 */
 		public void write(SimpleProfileRegistry registry) {
 			start(REGISTRY_ELEMENT);
 			writeProperties(registry.getProperties());
 			writeProfiles(registry.getProfiles());
 			end(REGISTRY_ELEMENT);
 			flush();
 		}
 	}
 
 	/*
 	 * 	Parser for the contents of a SimpleProfileRegistry,
 	 * 	as written by the Writer class.
 	 */
 	private class Parser extends ProfileParser implements XMLConstants {
 		Map profileMap = new LinkedHashMap(8);
 
 		public Parser(BundleContext context, String bundleId) {
 			super(context, bundleId);
 		}
 
 		public void parse(File file) throws IOException {
 			parse(new FileInputStream(file));
 		}
 
 		public synchronized void parse(InputStream stream) throws IOException {
 			this.status = null;
 			try {
 				// TODO: currently not caching the parser since we make no assumptions
 				//		 or restrictions on concurrent parsing
 				getParser();
 				RegistryHandler registryHandler = new RegistryHandler();
 				xmlReader.setContentHandler(new ProfileRegistryDocHandler(REGISTRY_ELEMENT, registryHandler));
 				xmlReader.parse(new InputSource(stream));
 			} catch (SAXException e) {
 				throw new IOException(e.getMessage());
 			} catch (ParserConfigurationException e) {
 				throw new IOException(e.getMessage());
 			} finally {
 				stream.close();
 			}
 		}
 
 		protected Object getRootObject() {
 			return SimpleProfileRegistry.this;
 		}
 
 		/**
 		 * Returns the map of profiles that was parsed.
 		 */
 		protected Map getProfileMap() {
 			return profileMap;
 		}
 
 		private final class ProfileRegistryDocHandler extends DocHandler {
 
 			public ProfileRegistryDocHandler(String rootName, RootHandler rootHandler) {
 				super(rootName, rootHandler);
 			}
 
 			public void ProcessingInstruction(String target, String data) throws SAXException {
 				if (PI_REPOSITORY_TARGET.equals(target)) {
 					// TODO: should the root handler be constructed based on class
 					// 		 via an extension registry mechanism?
 					// String clazz = extractPIClass(data);
 					// and
 					// TODO: version tolerance by extension
 					Version repositoryVersion = extractPIVersion(target, data);
 					if (!XMLConstants.XML_TOLERANCE.isIncluded(repositoryVersion)) {
 						throw new SAXException(NLS.bind(Messages.SimpleProfileRegistry_Parser_Has_Incompatible_Version, repositoryVersion, XMLConstants.XML_TOLERANCE));
 					}
 				}
 			}
 
 		}
 
 		private final class RegistryHandler extends RootHandler {
 
 			private ProfilesHandler profilesHandler = null;
 			private PropertiesHandler propertiesHandler = null;
 
 			public RegistryHandler() {
 				super();
 			}
 
 			protected void handleRootAttributes(Attributes attributes) {
 				parseRequiredAttributes(attributes, noAttributes);
 			}
 
 			public void startElement(String name, Attributes attributes) {
 				if (PROPERTIES_ELEMENT.equals(name)) {
 					if (propertiesHandler == null) {
 						propertiesHandler = new PropertiesHandler(this, attributes);
 					} else {
 						duplicateElement(this, name, attributes);
 					}
 				} else if (PROFILES_ELEMENT.equals(name)) {
 					if (profilesHandler == null) {
 						profilesHandler = new ProfilesHandler(this, attributes);
 					} else {
 						duplicateElement(this, name, attributes);
 					}
 				} else {
 					invalidElement(name, attributes);
 				}
 			}
 
 			protected void finished() {
 				if (isValidXML()) {
 					IProfile[] profyles = (profilesHandler == null ? new IProfile[0] //
 							: profilesHandler.getProfiles());
 					for (int i = 0; i < profyles.length; i++) {
 						IProfile nextProfile = profyles[i];
 						profileMap.put(nextProfile.getProfileId(), nextProfile);
 					}
 					properties = (propertiesHandler == null ? new OrderedProperties(0) //
 							: propertiesHandler.getProperties());
 				}
 			}
 		}
 
 		protected String getErrorMessage() {
 			return Messages.SimpleProfileRegistry_Parser_Error_Parsing_Registry;
 		}
 
 		public String toString() {
 			// TODO:
 			return null;
 		}
 
 	}
 }
