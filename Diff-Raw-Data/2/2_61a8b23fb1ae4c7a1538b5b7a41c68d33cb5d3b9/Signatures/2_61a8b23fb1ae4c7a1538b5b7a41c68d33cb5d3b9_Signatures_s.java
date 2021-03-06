 /*******************************************************************************
  * 
  * Copyright (c) 2008 Thomas Holland (thomas@innot.de) and others
  * 
  * This program and the accompanying materials are made
  * available under the terms of the GNU Public License v3
  * which accompanies this distribution, and is available at
  * http://www.gnu.org/licenses/gpl.html
  * 
  * Contributors:
  *     Thomas Holland - initial API and implementation
  *     
  * $Id$
  *     
  *******************************************************************************/
 package de.innot.avreclipse.core.toolinfo;
 
 import java.io.File;
 import java.io.FileInputStream;
 import java.io.FileOutputStream;
 import java.io.IOException;
 import java.io.InputStream;
 import java.util.HashSet;
 import java.util.Properties;
 import java.util.Set;
 
 import org.eclipse.core.runtime.Assert;
 import org.eclipse.core.runtime.FileLocator;
 import org.eclipse.core.runtime.IPath;
 import org.eclipse.core.runtime.Path;
 import org.eclipse.core.runtime.Status;
 import org.osgi.framework.Bundle;
 
 import de.innot.avreclipse.AVRPlugin;
 import de.innot.avreclipse.core.IMCUProvider;
 
 /**
  * This class handles the list of known MCU signatures.
  * <p>
  * Each AVR MCU is identified by a 3-byte signature. This class handles the
  * mappings between the MCU id and its signature.
  * </p>
  * <p>
  * <ul>
  * <li>To get the Signature for a known MCU Id use
  * {@link #getSignature(String)}</li>
  * <li>To get the MCU ID for a known Signature use {@link #getMCU(String)}</li>
  * </ul>
  * </p>
  * <p>
  * The signatures are stored as Strings with a format of "0x123456" (C style hex format)
  * </p>
  * <p>
  * This class loads a default list of signatures from the signatures.properties
  * file, which is located in the properties folder of the core plugin.
  * Additional / overriding signatures can be added with the
  * {@link #addSignature(String, String)} method. With a call to
  * {@link #storeSignatures()} these additional signatures are persisted in the
  * instance state area (<code>.metadata/.plugins/de.innot.avreclipse.core/signatures.properties</core>)
  * and reloaded at the next start.
  * </p>
  * @author Thomas Holland
  * @since 2.2
  *
  */
 public class Signatures implements IMCUProvider {
 
 	// paths to the default and instance properties files
 	private final static IPath DEFAULTPROPSFILE = new Path("properties/signature.properties");
 	private final static IPath INSTANCEPROPSFILE = new Path("signatures.properties");
 
 	// properties are stored as key=mcuid, value=signature
 	private Properties fProps = new Properties();
 
 	private static Signatures fInstance = null;
 
 	/**
 	 * Get the default instance of the Signatures class
 	 */
 	public static Signatures getDefault() {
 		if (fInstance == null)
 			fInstance = new Signatures();
 		return fInstance;
 	}
 
 	// private constructor to prevent instantiation
 	private Signatures() {
 		// The constructor will first read the default signatures from
 		// the plugin signature.properties file.
 		// Then it tries to load an existing instance signature property file.
 
 		// Load the list of signatures from the signature.properties file
 		// as the default values.
 		Properties mcuDefaultProps = new Properties();
 		Bundle avrplugin = AVRPlugin.getDefault().getBundle();
 		InputStream is = null;
 		try {
 			is = FileLocator.openStream(avrplugin, DEFAULTPROPSFILE, false);
 			mcuDefaultProps.load(is);
 			is.close();
 		} catch (IOException e) {
 			// this should not happen because the signatures.properties is
 			// part of the plugin and always there.
 			AVRPlugin.getDefault().log(
 			        new Status(Status.ERROR, AVRPlugin.PLUGIN_ID,
 			                "Can't find signatures.properties", e));
 			return;
 		}
 
 		// Load any instance signatures from the plugin state location
 		fProps = new Properties(mcuDefaultProps);
 		File propsfile = getInstanceSignatureProperties();
 		if (propsfile.canRead()) {
 			try {
 				is = new FileInputStream(propsfile);
 				fProps.load(is);
 				is.close();
 			} catch (IOException e) {
 				AVRPlugin.getDefault().log(
 				        new Status(Status.ERROR, AVRPlugin.PLUGIN_ID,
 				                "Can't read instance signatures.properties", e));
 				// continue anyway without the instance signatures
 			}
 		}
 	}
 
 	/**
 	 * Get the Signature for the given MCU id.
 	 * 
 	 * @param mcuid
 	 *            String with a MCU id
 	 * @return String with the MCU signature in hex ("0x123456") or
 	 *         <code>null</code> if the given MCU id is unknown.
 	 */
 	public String getSignature(String mcuid) {
 		return fProps.getProperty(mcuid);
 	}
 
 	/**
 	 * Get the MCU id for the given Signature.
 	 * 
 	 * @param signature
 	 *            String with a signature in hex ("0x123456")
 	 * @return String with the corresponding MCU id or * <code>null</code> if
 	 *         the given signature is unknown.
 	 */
 	public String getMCU(String signature) {
 		// iterate over all mcuids to find the one with the given signature
 		// I do not use a reverse lookup map because this method will not be
 		// called often and a reverse map would add code complexity.
 		Set<String> keyset = fProps.stringPropertyNames();
 		for (String mcuid : keyset) {
			if (fProps.getProperty(mcuid).equals(signature)) {
 				return mcuid;
 			}
 		}
 		return null;
 	}
 
 	/**
 	 * Add a MCU signature to the list.
 	 * <p>
 	 * The signature is only added if it differs from the default signature.
 	 * </p>
 	 * 
 	 * @param mcuid
 	 *            String with a MCU id
 	 * @param signature
 	 *            String with the signature in format "0x123456"
 	 */
 	public void addSignature(String mcuid, String signature) {
 		Assert.isNotNull(mcuid);
 		Assert.isNotNull(signature);
 
 		String oldsig = fProps.getProperty(mcuid);
 		if (!signature.equals(oldsig)) {
 			fProps.setProperty(mcuid, signature);
 		}
 	}
 
 	/**
 	 * Stores the signature properties in the Eclipse instance storage area.
 	 * <p>
 	 * The generated properties file only contains additional signatures not in
 	 * the default list.</p>
 	 * <p>
 	 * @throws IOException for any error writing the properties file
 	 */
 	public void storeSignatures() throws IOException {
 		File propsfile = getInstanceSignatureProperties();
 		FileOutputStream os = null;
 		try {
 			os = new FileOutputStream(propsfile);
 			fProps.store(os, "Additional MCU Signature values");
 		} finally {
 			// close the stream if the fProps.store() method failed
 			if (os != null) {
 				os.close();
 			}
 		}
 	}
 
 	//
 	// Methods of the IMCUProvider Interface
 	//
 	
 	/*
 	 * (non-Javadoc)
 	 * 
 	 * @see de.innot.avreclipse.core.IMCUProvider#getMCUInfo(java.lang.String)
 	 */
 	public String getMCUInfo(String mcuid) {
 		return fProps.getProperty(mcuid);
 	}
 
 	/*
 	 * (non-Javadoc)
 	 * 
 	 * @see de.innot.avreclipse.core.IMCUProvider#getMCUList()
 	 */
 	public Set<String> getMCUList() {
 		// Return all keys of the underlying properties (the mcuids)
 		// as a List
 		Set<String> keyset = fProps.stringPropertyNames();
 		return new HashSet<String>(keyset);
 	}
 
 	/*
 	 * (non-Javadoc)
 	 * 
 	 * @see de.innot.avreclipse.core.IMCUProvider#hasMCU(java.lang.String)
 	 */
 	public boolean hasMCU(String mcuid) {
 		String sig = fProps.getProperty(mcuid);
 		return sig != null ? true : false;
 	}
 
 	
 	/**
 	 * @return File pointing to the instance signature properties file
 	 */
 	private File getInstanceSignatureProperties() {
 		IPath propslocation = AVRPlugin.getDefault().getStateLocation().append(
 		        INSTANCEPROPSFILE);
 		return propslocation.toFile();
 
 	}
 }
