 /*******************************************************************************
  * Copyright (c) 2000, 2008 IBM Corporation and others.
  * All rights reserved. This program and the accompanying materials
  * are made available under the terms of the Eclipse Public License v1.0
  * which accompanies this distribution, and is available at
  * http://www.eclipse.org/legal/epl-v10.html
  *
  * Contributors:
  *     IBM Corporation - initial API and implementation
  *******************************************************************************/
 package org.eclipse.equinox.internal.p2.metadata.generator.features;
 
 import java.io.*;
 import java.net.URL;
 import java.util.Properties;
 import java.util.jar.JarEntry;
 import java.util.jar.JarFile;
 import javax.xml.parsers.*;
 import org.eclipse.equinox.internal.provisional.p2.metadata.generator.Feature;
 import org.eclipse.equinox.internal.provisional.p2.metadata.generator.FeatureEntry;
 import org.eclipse.osgi.util.NLS;
 import org.xml.sax.*;
 import org.xml.sax.helpers.DefaultHandler;
 
 /**
  * Default feature parser.
  * Parses the feature manifest file as defined by the platform.
  * 
  * @since 3.0
  */
 public class FeatureParser extends DefaultHandler {
 
 	private final static SAXParserFactory parserFactory = SAXParserFactory.newInstance();
 	private SAXParser parser;
 	private Feature result;
 	private URL url;
 	private StringBuffer characters = null;
 
 	private Properties messages = null;
 
 	public FeatureParser() {
 		this(true);
 	}
 
 	protected FeatureParser(boolean createParser) {
 		super();
 		if (!createParser)
 			return;
 		try {
 			parserFactory.setNamespaceAware(true);
 			this.parser = parserFactory.newSAXParser();
 		} catch (ParserConfigurationException e) {
 			System.out.println(e);
 		} catch (SAXException e) {
 			System.out.println(e);
 		}
 	}
 
 	public void characters(char[] ch, int start, int length) throws SAXException {
 		if (characters == null)
 			return;
 		characters.append(ch, start, length);
 	}
 
 	protected Feature createFeature(String id, String version) {
 		return new Feature(id, version);
 	}
 
 	public void endElement(String uri, String localName, String qName) throws SAXException {
 		if (characters == null)
 			return;
 		if ("description".equals(localName)) { //$NON-NLS-1$
 			result.setDescription(localize(characters.toString().trim()));
 		} else if ("license".equals(localName)) { //$NON-NLS-1$
 			result.setLicense(localize(characters.toString().trim()));
 		} else if ("copyright".equals(localName)) { //$NON-NLS-1$
 			result.setCopyright(localize(characters.toString().trim()));
 		}
 		characters = null;
 	}
 
 	public Feature getResult() {
 		return result;
 	}
 
 	private Properties loadProperties(File directory) {
 		//skip directories that don't contain a feature.properties file
 		File file = new File(directory, "feature.properties"); //$NON-NLS-1$
 		if (!file.exists())
 			return null;
 		try {
 			InputStream input = new BufferedInputStream(new FileInputStream(file));
 			try {
 				Properties result = new Properties();
 				result.load(input);
 				return result;
 			} finally {
 				if (input != null)
 					input.close();
 			}
 		} catch (IOException e) {
 			e.printStackTrace();
 		}
 		return null;
 	}
 
 	private Properties loadProperties(JarFile jar) {
 		JarEntry entry = jar.getJarEntry("feature.properties"); //$NON-NLS-1$
 		if (entry == null)
 			return null;
 		try {
 			InputStream input = new BufferedInputStream(jar.getInputStream(entry));
 			try {
 				Properties result = new Properties();
 				result.load(input);
 				return result;
 			} finally {
 				if (input != null)
 					input.close();
 			}
 		} catch (IOException e) {
 			e.printStackTrace();
 		}
 		return null;
 	}
 
 	private String localize(String value) {
 		if (messages == null || value == null)
 			return value;
 		if (!value.startsWith("%")) //$NON-NLS-1$
 			return value;
 		return messages.getProperty(value.substring(1), value);
 	}
 
 	/**
 	 * Parses the specified location and constructs a feature. The given location 
 	 * should be either the location of the feature JAR or the directory containing
 	 * the feature.
 	 * 
 	 * @param location the location of the feature to parse.  
 	 */
 	public Feature parse(File location) {
 		if (!location.exists())
 			return null;
 		if (location.isDirectory()) {
 			//skip directories that don't contain a feature.xml file
 			File file = new File(location, "feature.xml"); //$NON-NLS-1$
 			if (!file.exists())
 				return null;
 			Properties properties = loadProperties(location);
 			try {
 				InputStream input = new BufferedInputStream(new FileInputStream(file));
 				return parse(input, properties);
 			} catch (FileNotFoundException e) {
 				e.printStackTrace();
 			}
 		} else if (location.getName().endsWith(".jar")) { //$NON-NLS-1$
 			JarFile jar = null;
 			try {
 				jar = new JarFile(location);
 				Properties properties = loadProperties(jar);
 				JarEntry entry = jar.getJarEntry("feature.xml"); //$NON-NLS-1$
 				if (entry == null)
 					return null;
 				InputStream input = new BufferedInputStream(jar.getInputStream(entry));
 				return parse(input, properties);
 			} catch (IOException e) {
 				e.printStackTrace();
 			} finally {
 				try {
 					if (jar != null)
 						jar.close();
 				} catch (IOException e) {
 					//
 				}
 			}
 		}
 		return null;
 	}
 
 	/**
 	 * Parse the given input stream and return a feature object
 	 * or null. This method closes the input stream.
 	 */
 	public Feature parse(InputStream in, Properties messages) {
 		this.messages = messages;
 		result = null;
 		try {
 			parser.parse(new InputSource(in), this);
 		} catch (SAXException e) {
 			e.printStackTrace();
 		} catch (IOException e) {
 			e.printStackTrace();
 		} finally {
 			try {
 				in.close();
 			} catch (IOException e1) {
 				//					Utils.log(e1.getLocalizedMessage());
 			}
 		}
 		return result;
 	}
 
 	private void processCopyright(Attributes attributes) {
 		result.setCopyrightURL(attributes.getValue("url")); //$NON-NLS-1$
 		characters = new StringBuffer();
 	}
 
 	private void processDescription(Attributes attributes) {
 		result.setDescriptionURL(attributes.getValue("url")); //$NON-NLS-1$
 		characters = new StringBuffer();
 	}
 
 	private void processDiscoverySite(Attributes attributes) {
 		result.addDiscoverySite(attributes.getValue("url"), attributes.getValue("label")); //$NON-NLS-1$ //$NON-NLS-2$
 	}
 
 	protected void processFeature(Attributes attributes) {
 		String id = attributes.getValue("id"); //$NON-NLS-1$
 		String ver = attributes.getValue("version"); //$NON-NLS-1$
 
 		if (id == null || id.trim().equals("") //$NON-NLS-1$
 				|| ver == null || ver.trim().equals("")) { //$NON-NLS-1$
 			//			System.out.println(NLS.bind(Messages.FeatureParser_IdOrVersionInvalid, (new String[] { id, ver})));
 		} else {
 			result = createFeature(id, ver);
 
 			String os = attributes.getValue("os"); //$NON-NLS-1$
 			String ws = attributes.getValue("ws"); //$NON-NLS-1$
 			String nl = attributes.getValue("nl"); //$NON-NLS-1$
 			String arch = attributes.getValue("arch"); //$NON-NLS-1$
 			result.setEnvironment(os, ws, arch, nl);
 
 			//TODO rootURLs
 			if (url != null && "file".equals(url.getProtocol())) { //$NON-NLS-1$
 				File f = new File(url.getFile().replace('/', File.separatorChar));
 				result.setURL("features" + "/" + f.getParentFile().getName() + "/");// + f.getName()); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
 			} else {
 				// externalized URLs might be in relative form, ensure they are absolute				
 				//				feature.setURL(Utils.makeAbsolute(Utils.getInstallURL(), url).toExternalForm());
 			}
 
 			result.setProviderName(localize(attributes.getValue("provider-name"))); //$NON-NLS-1$
 			result.setLabel(localize(attributes.getValue("label"))); //$NON-NLS-1$
 			result.setImage(attributes.getValue("image")); //$NON-NLS-1$
 
 			//			Utils.debug("End process DefaultFeature tag: id:" +id + " ver:" +ver + " url:" + feature.getURL()); 	 //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
 		}
 	}
 
 	private void processImport(Attributes attributes) {
 		String id = attributes.getValue("feature"); //$NON-NLS-1$
 		FeatureEntry entry = null;
 		if (id != null) {
 			entry = FeatureEntry.createRequires(id, attributes.getValue("version"), attributes.getValue("match"), attributes.getValue("filter"), false); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
 		} else {
 			id = attributes.getValue("plugin"); //$NON-NLS-1$
 			entry = FeatureEntry.createRequires(id, attributes.getValue("version"), attributes.getValue("match"), attributes.getValue("filter"), true); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
 		}
 		result.addEntry(entry);
 	}
 
 	private void processIncludes(Attributes attributes) {
 		FeatureEntry entry = new FeatureEntry(attributes.getValue("id"), attributes.getValue("version"), false); //$NON-NLS-1$ //$NON-NLS-2$
 		String flag = attributes.getValue("optional"); //$NON-NLS-1$
 		if (flag != null)
 			entry.setOptional(Boolean.valueOf(flag).booleanValue());
 		setEnvironment(attributes, entry);
 		result.addEntry(entry);
 	}
 
 	private void processInstallHandler(Attributes attributes) {
 		result.setInstallHandler(attributes.getValue("handler")); //$NON-NLS-1$
 		result.setInstallHandlerLibrary(attributes.getValue("library")); //$NON-NLS-1$
 		result.setInstallHandlerURL(attributes.getValue("url")); //$NON-NLS-1$
 	}
 
 	private void processLicense(Attributes attributes) {
 		result.setLicenseURL(attributes.getValue("url")); //$NON-NLS-1$
 		characters = new StringBuffer();
 	}
 
 	private void processPlugin(Attributes attributes) {
 		String id = attributes.getValue("id"); //$NON-NLS-1$
 		String version = attributes.getValue("version"); //$NON-NLS-1$
 
 		if (id == null || id.trim().equals("") || version == null || version.trim().equals("")) { //$NON-NLS-1$ //$NON-NLS-2$
 			System.out.println(NLS.bind("FeatureParser#processPlugin, ID {0} or version {1} invalid", (new String[] {id, version}))); //$NON-NLS-1$
 		} else {
 			FeatureEntry plugin = new FeatureEntry(id, version, true);
 			setEnvironment(attributes, plugin);
 			String unpack = attributes.getValue("unpack"); //$NON-NLS-1$
 			if (unpack != null)
 				plugin.setUnpack(Boolean.valueOf(unpack).booleanValue());
 			String fragment = attributes.getValue("fragment"); //$NON-NLS-1$
 			if (fragment != null)
 				plugin.setFragment(Boolean.valueOf(fragment).booleanValue());
 			String filter = attributes.getValue("filter"); //$NON-NLS-1$
 			if (filter != null)
 				plugin.setFilter(filter);
 			result.addEntry(plugin);
 
 			//			Utils.debug("End process DefaultFeature tag: id:" + id + " ver:" + ver + " url:" + feature.getURL()); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
 		}
 	}
 
 	private void processUpdateSite(Attributes attributes) {
 		result.setUpdateSiteLabel(attributes.getValue("label")); //$NON-NLS-1$
 		result.setUpdateSiteURL(attributes.getValue("url")); //$NON-NLS-1$
 	}
 
 	private void setEnvironment(Attributes attributes, FeatureEntry entry) {
 		String os = attributes.getValue("os"); //$NON-NLS-1$
 		String ws = attributes.getValue("ws"); //$NON-NLS-1$
 		String nl = attributes.getValue("nl"); //$NON-NLS-1$
 		String arch = attributes.getValue("arch"); //$NON-NLS-1$
 		entry.setEnvironment(os, ws, arch, nl);
 	}
 
 	public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
 		//		Utils.debug("Start Element: uri:" + uri + " local Name:" + localName + " qName:" + qName); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
 		if ("plugin".equals(localName)) { //$NON-NLS-1$
 			processPlugin(attributes);
 		} else if ("description".equals(localName)) { //$NON-NLS-1$
 			processDescription(attributes);
 		} else if ("license".equals(localName)) { //$NON-NLS-1$
 			processLicense(attributes);
 		} else if ("copyright".equals(localName)) { //$NON-NLS-1$
 			processCopyright(attributes);
 		} else if ("feature".equals(localName)) { //$NON-NLS-1$
 			processFeature(attributes);
 		} else if ("import".equals(localName)) { //$NON-NLS-1$
 			processImport(attributes);
 		} else if ("includes".equals(localName)) { //$NON-NLS-1$
 			processIncludes(attributes);
 		} else if ("install-handler".equals(localName)) { //$NON-NLS-1$
 			processInstallHandler(attributes);
 		} else if ("update".equals(localName)) { //$NON-NLS-1$
 			processUpdateSite(attributes);
 		} else if ("discovery".equals(localName)) { //$NON-NLS-1$
 			processDiscoverySite(attributes);
 		}
 	}
 
 }
