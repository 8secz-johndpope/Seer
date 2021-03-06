 /*
  *  Freeplane - mind map editor
  *  Copyright (C) 2008 Joerg Mueller, Daniel Polansky, Christian Foltin, Dimitry Polivaev
  *
  *  This file is created by Dimitry Polivaev in 2008.
  *
  *  This program is free software: you can redistribute it and/or modify
  *  it under the terms of the GNU General Public License as published by
  *  the Free Software Foundation, either version 2 of the License, or
  *  (at your option) any later version.
  *
  *  This program is distributed in the hope that it will be useful,
  *  but WITHOUT ANY WARRANTY; without even the implied warranty of
  *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  *  GNU General Public License for more details.
  *
  *  You should have received a copy of the GNU General Public License
  *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
  */
 package org.freeplane.core.url;
 
 import java.io.BufferedInputStream;
 import java.io.BufferedReader;
 import java.io.File;
 import java.io.FileInputStream;
 import java.io.FileNotFoundException;
 import java.io.FileReader;
 import java.io.IOException;
 import java.io.InputStream;
 import java.io.InputStreamReader;
 import java.io.Reader;
 import java.io.StringReader;
 import java.io.StringWriter;
 import java.net.MalformedURLException;
 import java.net.URI;
 import java.net.URISyntaxException;
 import java.net.URL;
 import java.security.AccessControlException;
 
 import javax.swing.JFileChooser;
 import javax.swing.JOptionPane;
 import javax.swing.filechooser.FileFilter;
 import javax.xml.transform.Result;
 import javax.xml.transform.Source;
 import javax.xml.transform.Transformer;
 import javax.xml.transform.TransformerException;
 import javax.xml.transform.TransformerFactory;
 import javax.xml.transform.stream.StreamResult;
 import javax.xml.transform.stream.StreamSource;
 
 import org.freeplane.core.controller.Controller;
 import org.freeplane.core.extension.IExtension;
 import org.freeplane.core.io.MapWriter.Mode;
 import org.freeplane.core.modecontroller.MapController;
 import org.freeplane.core.modecontroller.ModeController;
 import org.freeplane.core.model.MapModel;
 import org.freeplane.core.model.NodeModel;
 import org.freeplane.core.resources.FpStringUtils;
 import org.freeplane.core.resources.ResourceBundles;
 import org.freeplane.core.resources.ResourceController;
 import org.freeplane.core.ui.components.UITools;
 import org.freeplane.core.util.LogTool;
 import org.freeplane.n3.nanoxml.XMLParseException;
 
 /**
  * @author Dimitry Polivaev
  */
 public class UrlManager implements IExtension {
 	public static final String FREEPLANE_FILE_EXTENSION_WITHOUT_DOT = "mm";
 	public static final String FREEPLANE_FILE_EXTENSION = "." + FREEPLANE_FILE_EXTENSION_WITHOUT_DOT;
 	private static File lastCurrentDir = null;
 	public static final String MAP_URL = "map_url";
 
 	/**
 	 * Creates a default reader that just reads the given file.
 	 *
 	 * @throws FileNotFoundException
 	 */
 	protected static Reader getActualReader(final InputStream file) throws FileNotFoundException {
 		return new InputStreamReader(file);
 	}
 
 	public static UrlManager getController(final ModeController modeController) {
 		return (UrlManager) modeController.getExtension(UrlManager.class);
 	}
 
 	/**
 	 * Returns the lowercase of the extension of a file.
 	 */
 	public static String getExtension(final File f) {
 		return UrlManager.getExtension(f.toString());
 	}
 
 	/**
 	 * Returns the lowercase of the extension of a file.
 	 */
 	public static String getExtension(final String s) {
 		final int i = s.lastIndexOf('.');
 		return (i > 0 && i < s.length() - 1) ? s.substring(i + 1).toLowerCase().trim() : "";
 	}
 
 	public static Reader getUpdateReader(final File file, final String xsltScript) throws FileNotFoundException,
 	        IOException {
 		return UrlManager.getUpdateReader(new BufferedInputStream( new FileInputStream(file)), xsltScript);
 	}
 
 	/**
 	 * Creates a reader that pipes the input file through a XSLT-Script that
 	 * updates the version to the current.
 	 *
 	 * @throws IOException
 	 */
 	public static Reader getUpdateReader(final InputStream file, final String xsltScript) throws IOException {
 		StringWriter writer = null;
 		InputStream inputStream = null;
 		try {
 			URL updaterUrl = null;
 			updaterUrl = ResourceController.getResourceController().getResource(xsltScript);
 			if (updaterUrl == null) {
 				throw new IllegalArgumentException(xsltScript + " not found.");
 			}
 			inputStream = new BufferedInputStream(updaterUrl.openStream());
 			final Source xsltSource = new StreamSource(inputStream);
 			writer = new StringWriter();
 			final Result result = new StreamResult(writer);
 			class TransformerRunnable implements Runnable {
 				private Throwable thrownException = null;
 
 				public void run() {
 					final TransformerFactory transFact = TransformerFactory.newInstance();
 					Transformer trans;
 					try {
 						trans = transFact.newTransformer(xsltSource);
 						InputStream cleanedInput = new CleaningInputStream(file);
 						trans.transform(new StreamSource(cleanedInput), result);
 					}
 					catch (final Exception ex) {
 						LogTool.warn(ex);
 						thrownException = ex;
 					}
 				}
 
 				public Throwable thrownException() {
 					return thrownException;
 				}
 			}
 			final TransformerRunnable transformer = new TransformerRunnable();
 			final Thread transformerThread = new Thread(transformer, "XSLT");
 			transformerThread.start();
 			transformerThread.join();
 			final Throwable thrownException = transformer.thrownException();
 			if (thrownException != null) {
 				throw new TransformerException(thrownException);
 			}
 			return new StringReader(writer.getBuffer().toString());
 		}
 		catch (final Exception ex) {
 			LogTool.severe(ex);
 			return UrlManager.getActualReader(file);
 		}
 		finally {
 			if (inputStream != null) {
 				inputStream.close();
 			}
 			if (writer != null) {
 				writer.close();
 			}
 		}
 	}
 
 	/**
 	 * Returns the same URL as input with the addition, that the reference part
 	 * "#..." is filtered out.
 	 *
 	 * @throws MalformedURLException
 	 */
 	public static URL getURLWithoutReference(final URL input) throws MalformedURLException {
 		return new URL(input.toString().replaceFirst("#.*", ""));
 	}
 
 	public static void install(final ModeController modeController, final UrlManager urlManager) {
 		modeController.addExtension(UrlManager.class, urlManager);
 	}
 
 	public static boolean isAbsolutePath(final String path) {
 		final String osNameStart = System.getProperty("os.name").substring(0, 3);
 		final String fileSeparator = System.getProperty("file.separator");
 		if (osNameStart.equals("Win")) {
 			return ((path.length() > 1) && path.substring(1, 2).equals(":")) || path.startsWith(fileSeparator);
 		}
 		else if (osNameStart.equals("Mac")) {
 			return path.startsWith(fileSeparator);
 		}
 		else {
 			return path.startsWith(fileSeparator);
 		}
 	}
 
 	/**
 	 * In case of trouble, the method returns null.
 	 *
 	 * @param pInputFile
 	 *            the file to read.
 	 * @return the complete content of the file. or null if an exception has
 	 *         occured.
 	 */
 	public static String readFile(final File pInputFile) {
 		final StringBuilder lines = new StringBuilder();
 		BufferedReader bufferedReader = null;
 		try {
 			bufferedReader = new BufferedReader(new FileReader(pInputFile));
 			final String endLine = System.getProperty("line.separator");
 			String line;
 			while ((line = bufferedReader.readLine()) != null) {
 				lines.append(line).append(endLine);
 			}
 			bufferedReader.close();
 		}
 		catch (final Exception e) {
 			LogTool.severe(e);
 			if (bufferedReader != null) {
 				try {
 					bufferedReader.close();
 				}
 				catch (final Exception ex) {
 					LogTool.severe(ex);
 				}
 			}
 			return null;
 		}
 		return lines.toString();
 	}
 
 	public static String removeExtension(final String s) {
 		final int i = s.lastIndexOf('.');
 		return (i > 0 && i < s.length() - 1) ? s.substring(0, i) : s;
 	}
 
 	public static void setHidden(final File file, final boolean hidden, final boolean synchronously) {
 		final String osNameStart = System.getProperty("os.name").substring(0, 3);
 		if (osNameStart.equals("Win")) {
 			try {
 				Controller.exec("attrib " + (hidden ? "+" : "-") + "H \"" + file.getAbsolutePath() + "\"");
 				if (!synchronously) {
 					return;
 				}
 				int timeOut = 10;
 				while (file.isHidden() != hidden && timeOut > 0) {
 					Thread.sleep(10/* miliseconds */);
 					timeOut--;
 				}
 			}
 			catch (final Exception e) {
 				LogTool.severe(e);
 			}
 		}
 	}
 
 	final private Controller controller;
 	final private ModeController modeController;
 
 	public UrlManager(final ModeController modeController) {
 		super();
 		this.modeController = modeController;
 		controller = modeController.getController();
 		createActions();
 	}
 
 	/**
 	 *
 	 */
 	private void createActions() {
 	}
 
 	public Controller getController() {
 		return controller;
 	}
 
 	/**
 	 * Creates a file chooser with the last selected directory as default.
 	 */
 	public JFileChooser getFileChooser(final FileFilter filter) {
 		final JFileChooser chooser = new JFileChooser();
 		final File parentFile = getMapsParentFile();
 		if (parentFile != null && getLastCurrentDir() == null) {
 			setLastCurrentDir(parentFile);
 		}
 		if (getLastCurrentDir() != null) {
 			chooser.setCurrentDirectory(getLastCurrentDir());
 		}
 		if (filter != null) {
 			chooser.addChoosableFileFilter(filter);
 			chooser.setFileFilter(filter);
 		}
 		return chooser;
 	}
 
 	public File getLastCurrentDir() {
 		return lastCurrentDir;
 	}
 
 	protected File getMapsParentFile() {
 		final MapModel map = getController().getMap();
 		if ((map != null) && (map.getFile() != null) && (map.getFile().getParentFile() != null)) {
 			return map.getFile().getParentFile();
 		}
 		return null;
 	}
 
 	public ModeController getModeController() {
 		return modeController;
 	}
 
 	public void handleLoadingException(final Exception ex) {
 		final String exceptionType = ex.getClass().getName();
 		if (exceptionType.equals("freeplane.main.XMLParseException")) {
 			final int showDetail = JOptionPane.showConfirmDialog(getController().getViewController().getMapView(),
 			    ResourceBundles.getText("map_corrupted"), "Freeplane", JOptionPane.YES_NO_OPTION,
 			    JOptionPane.ERROR_MESSAGE);
 			if (showDetail == JOptionPane.YES_OPTION) {
 				UITools.errorMessage(ex);
 			}
 		}
 		else if (exceptionType.equals("java.io.FileNotFoundException")) {
 			UITools.errorMessage(ex.getMessage());
 		}
 		else {
 			LogTool.severe(ex);
 			UITools.errorMessage(ex);
 		}
 	}
 
 	public void load(final URL url, final MapModel map) throws FileNotFoundException, IOException, XMLParseException,
 	        URISyntaxException {
 		setURL(map, url);
 		InputStreamReader urlStreamReader = null;
 		try {
 			urlStreamReader = new InputStreamReader(url.openStream());
 		}
 		catch (final AccessControlException ex) {
 			UITools.errorMessage("Could not open URL " + url + ". Access Denied.");
 			System.err.println(ex);
 			return;
 		}
 		catch (final Exception ex) {
 			UITools.errorMessage("Could not open URL " + url + ".");
 			System.err.println(ex);
 			return;
 		}
 		try {
 			final NodeModel root = modeController.getMapController().getMapReader().createNodeTreeFromXml(map,
 			    urlStreamReader, Mode.FILE);
 			urlStreamReader.close();
 			if (root != null) {
 				map.setRoot(root);
 			}
 			else {
 				throw new IOException();
 			}
 		}
 		catch (final Exception ex) {
 			LogTool.severe(ex);
 			return;
 		}
 	}
 
 	public void loadURL(final URI uri) {
 		final String uriString = uri.toString();
 		if (uriString.startsWith("#")) {
 			final String target = uri.getFragment();
 			try {
 				final MapController mapController = modeController.getMapController();
 				mapController.select(mapController.getNodeFromID(target));
 			}
 			catch (final Exception e) {
 				LogTool.warn("link " + target + " not found", e);
 				UITools.errorMessage(FpStringUtils.formatText("link_not_found", target));
 			}
 			return;
 		}
 		try {
 			URL url = getAbsoluteUrl(uri);
			final String extension = UrlManager.getExtension(url.toString());
 			try {
 				if ((extension != null)
 				        && extension.equals(org.freeplane.core.url.UrlManager.FREEPLANE_FILE_EXTENSION_WITHOUT_DOT)) {
 					final String ref = url.getRef();
 					if (ref != null) {
 						url = UrlManager.getURLWithoutReference(url);
 					}
 					modeController.getMapController().newMap(url);
 					if (ref != null) {
 						final ModeController newModeController = getController().getModeController();
 						final MapController newMapController = newModeController.getMapController();
 						newMapController.select(newMapController.getNodeFromID(ref));
 					}
 				}
 				else {
 					getController().getViewController().openDocument(url);
 				}
 			}
 			catch (final Exception e) {
 				LogTool.warn("link " + uri + " not found", e);
 				UITools.errorMessage(FpStringUtils.formatText("link_not_found", uri.toString()));
 			}
 		}
 		catch (final MalformedURLException ex) {
 			/*
 			 * It's not a file, it's not a URL, it still might be a URI
 			 * (e.g. link to Lotus Notes via notes://... etc. 
 			 */
 			try {
 				getController().getViewController().openDocument(uri);
 			}
 			catch (final Exception e) {
 				LogTool.warn("URL " + uriString + " not found", e);
 				UITools.errorMessage(FpStringUtils.formatText("link_not_found", uriString));
 			}
 		}
 	}
 
 	public URL getAbsoluteUrl(final URI uri) throws MalformedURLException {
 		final MapModel map = getController().getMap();
 		return getAbsoluteUrl(map, uri);
 	}
 
 	public URI getAbsoluteUri(final MapModel map, final URI uri) throws MalformedURLException {
 		if (uri.isAbsolute()) {
 			return uri;
 		}
 		final String path = uri.getPath();
 		URL url = new URL(map.getURL(), path);
 		try {
			return new URI(url.getProtocol(), null, url.getPath(), null);
 		} catch (URISyntaxException e) {
 			e.printStackTrace();
 			return null;
 		}
 	}
 	public URL getAbsoluteUrl(final MapModel map, final URI uri) throws MalformedURLException {
 	    URL url;
 		final String path = uri.getPath();
 		if (!uri.isAbsolute() || uri.isOpaque()) {
			url = new URL(map.getURL(), path);
 		}
 		else {
			StringBuilder sb = new StringBuilder(path);
			final String query = uri.getQuery();
			if(query != null){
				sb.append('?');
				sb.append(query);
			}
			final String fragment = uri.getFragment();
			if(fragment != null){
				sb.append('#');
				sb.append(fragment);
			}
 			url = new URL(uri.getScheme(), uri.getHost(), uri.getPort(), sb.toString());
 		}
 		return url;
     }
 
 	public void setLastCurrentDir(final File lastCurrentDir) {
 		UrlManager.lastCurrentDir = lastCurrentDir;
 	}
 
 	protected void setURL(final MapModel map, final URL url) {
 		map.setURL(url);
 	}
 
 	public void startup() {
 	}
 }
