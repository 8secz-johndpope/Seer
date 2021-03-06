 /*
  * PS3 Media Server, for streaming any medias to your PS3.
  * Copyright (C) 2008  A.Brochard
  *
  * This program is free software; you can redistribute it and/or
  * modify it under the terms of the GNU General Public License
  * as published by the Free Software Foundation; version 2
  * of the License only.
  *
  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  * GNU General Public License for more details.
  *
  * You should have received a copy of the GNU General Public License
  * along with this program; if not, write to the Free Software
  * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
  */
 package net.pms.dlna;
 
 import java.io.BufferedReader;
 import java.io.File;
 import java.io.FileInputStream;
 import java.io.IOException;
 import java.io.InputStream;
 import java.io.InputStreamReader;
 import java.io.LineNumberReader;
 import java.net.URI;
 import java.net.URLDecoder;
 import java.util.ArrayList;
 import java.util.HashMap;
 import java.util.List;
 import java.util.Map;
 import java.util.StringTokenizer;
 
 import net.pms.Messages;
 import net.pms.PMS;
 import net.pms.configuration.MapFileConfiguration;
 import net.pms.configuration.PmsConfiguration;
 import net.pms.configuration.RendererConfiguration;
 import net.pms.dlna.virtual.VirtualFolder;
 import net.pms.dlna.virtual.VirtualVideoAction;
 import net.pms.external.AdditionalFolderAtRoot;
 import net.pms.external.AdditionalFoldersAtRoot;
 import net.pms.external.ExternalFactory;
 import net.pms.external.ExternalListener;
 import net.pms.gui.IFrame;
 import net.pms.newgui.LooksFrame;
 import net.pms.xmlwise.Plist;
 
 import org.apache.commons.configuration.ConfigurationException;
 import org.apache.commons.lang.StringUtils;
 import org.slf4j.Logger;
 import org.slf4j.LoggerFactory;
 
 import com.sun.jna.Platform;
 
 public class RootFolder extends DLNAResource {
 	private static final Logger logger = LoggerFactory.getLogger(RootFolder.class);
 	private PmsConfiguration configuration = PMS.getConfiguration();
 	private boolean running;
 
 	public RootFolder() {
 		id = "0";
 	}
 
 	@Override
 	public InputStream getInputStream() {
 		return null;
 	}
 
 	@Override
 	public String getName() {
 		return "root";
 	}
 
 	@Override
 	public boolean isFolder() {
 		return true;
 	}
 
 	@Override
 	public long length() {
 		return 0;
 	}
 
 	@Override
 	public String getSystemName() {
 		return getName();
 	}
 
 	@Override
 	public boolean isValid() {
 		return true;
 	}
 
 	@Override
 	public void discoverChildren() {
 		for (DLNAResource r : getConfiguredFolders()) {
 			addChild(r);
 		}
 		for (DLNAResource r : getVirtualFolders()) {
 			addChild(r);
 		}
 		File webConf = new File(configuration.getProfileDirectory(), "WEB.conf"); //$NON-NLS-1$
 		if (webConf.exists()) {
 			addWebFolder(webConf);
 		}
 		if (Platform.isMac() && configuration.getIphotoEnabled()) {
 			DLNAResource iPhotoRes = getiPhotoFolder();
 			if (iPhotoRes != null) {
 				addChild(iPhotoRes);
 			}
 		}
 		if ((Platform.isMac() || Platform.isWindows()) && configuration.getItunesEnabled()) {
 			DLNAResource iTunesRes = getiTunesFolder();
 			if (iTunesRes != null) {
 				addChild(iTunesRes);
 			}
 		}
 		if (!configuration.isHideMediaLibraryFolder()) {
 			DLNAResource libraryRes = PMS.get().getLibrary();
 			if (libraryRes != null) {
 				addChild(libraryRes);
 			}
 		}
 		for (DLNAResource r : getAdditionalFoldersAtRoot()) {
 			addChild(r);
 		}
 		if (!configuration.getHideVideoSettings()) {
 			DLNAResource videoSettingsRes = getVideoSettingssFolder();
 			if (videoSettingsRes != null) {
 				addChild(videoSettingsRes);
 			}
 		}
 	}
 
 	public void scan() {
 		running = true;
		refreshChildren();
 		defaultRenderer = RendererConfiguration.getDefaultConf();
 		scan(this);
 		IFrame frame = PMS.get().getFrame();
 		if (frame instanceof LooksFrame) {
 			LooksFrame looksframe = (LooksFrame) frame;
 			looksframe.getFt().setScanLibraryEnabled(true);
 		}
 		PMS.get().getDatabase().cleanup();
 		PMS.get().getFrame().setStatusLine(null);
 	}
 
 	public void stopscan() {
 		running = false;
 	}
 
 	private synchronized void scan(DLNAResource resource) {
 		if (running) {
 			for (DLNAResource child : resource.children) {
 				if (running && child.allowScan()) {
 					child.defaultRenderer = resource.defaultRenderer;
 					String trace = null;
 					if (child instanceof RealFile) {
 						trace = "Scanning Folder: " + child.getName();
 					}
 					if (trace != null) {
 						logger.debug(trace);
 						PMS.get().getFrame().setStatusLine(trace);
 					}
 					if (child.discovered) {
 						child.refreshChildren();
 						child.closeChildren(child.childrenNumber(), true);
 					} else {
 						if (child instanceof DVDISOFile || child instanceof DVDISOTitle) // ugly hack
 						{
 							child.resolve();
 						}
 						child.discoverChildren();
 						child.analyzeChildren(-1);
 						child.closeChildren(0, false);
 						child.discovered = true;
 					}
 					int count = child.children.size();
 					if (count == 0) {
 						continue;
 					}
 					scan(child);
 					child.children.clear();
 				}
 			}
 		}
 	}
 
 	private List<RealFile> getConfiguredFolders() {
 		List<RealFile> res = new ArrayList<RealFile>();
 		File files[];
 		try {
 			files = PMS.get().loadFoldersConf(configuration.getFolders(), true);
 			if (files == null || files.length == 0) {
 				files = File.listRoots();
 			}
 			for (File f : files) {
 				res.add(new RealFile(f));
 			}
 		} catch (IOException e) {
 			logger.error("Failed to load configured folders", e);
 		}
 		return res;
 	}
 
 	private List<DLNAResource> getVirtualFolders() {
 		List<DLNAResource> res = new ArrayList<DLNAResource>();
 		List<MapFileConfiguration> mapFileConfs = MapFileConfiguration.parse(configuration.getVirtualFolders());
 		if (mapFileConfs != null)
 			for (MapFileConfiguration f : mapFileConfs) {
 				res.add(new MapFile(f));
 			}
 		return res;
 	}
 
 	private void addWebFolder(File webConf) {
 		if (webConf.exists()) {
 			try {
 				LineNumberReader br = new LineNumberReader(new InputStreamReader(new FileInputStream(webConf), "UTF-8")); //$NON-NLS-1$
 				String line = null;
 				while ((line = br.readLine()) != null) {
 					line = line.trim();
 					if (line.length() > 0 && !line.startsWith("#") && line.indexOf("=") > -1) { //$NON-NLS-1$ //$NON-NLS-2$
 						String key = line.substring(0, line.indexOf("=")); //$NON-NLS-1$
 						String value = line.substring(line.indexOf("=") + 1); //$NON-NLS-1$
 						String keys[] = parseFeedKey(key);
 						try {
 							if (keys[0].equals("imagefeed")
 									|| keys[0].equals("audiofeed")
 									|| keys[0].equals("videofeed")
 									|| keys[0].equals("audiostream")
 									|| keys[0].equals("videostream")) { //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
 								String values[] = parseFeedValue(value);
 								DLNAResource parent = null;
 								if (keys[1] != null) {
 									StringTokenizer st = new StringTokenizer(keys[1], ","); //$NON-NLS-1$
 									DLNAResource currentRoot = this;
 									while (st.hasMoreTokens()) {
 										String folder = st.nextToken();
 										parent = currentRoot.searchByName(folder);
 										if (parent == null) {
 											parent = new VirtualFolder(folder, ""); //$NON-NLS-1$
 											currentRoot.addChild(parent);
 										}
 										currentRoot = parent;
 									}
 								}
 								if (parent == null) {
 									parent = this;
 								}
 								if (keys[0].equals("imagefeed")) { //$NON-NLS-1$
 									parent.addChild(new ImagesFeed(values[0]));
 								} else if (keys[0].equals("videofeed")) { //$NON-NLS-1$
 									parent.addChild(new VideosFeed(values[0]));
 								} else if (keys[0].equals("audiofeed")) { //$NON-NLS-1$
 									parent.addChild(new AudiosFeed(values[0]));
 								} else if (keys[0].equals("audiostream")) { //$NON-NLS-1$
 									parent.addChild(new WebAudioStream(values[0], values[1], values[2]));
 								} else if (keys[0].equals("videostream")) { //$NON-NLS-1$
 									parent.addChild(new WebVideoStream(values[0], values[1], values[2]));
 								}
 							}
 							// catch exception here and go with parsing
 						} catch (ArrayIndexOutOfBoundsException e) {
 							logger.info("Error at line " + br.getLineNumber() + " of WEB.conf: " + e.getMessage()); //$NON-NLS-1$
 						}
 					}
 				}
 				br.close();
 			} catch (Exception e) {
 				e.printStackTrace();
 				logger.info("Unexpected error in WEB.conf: " + e.getMessage()); //$NON-NLS-1$
 			}
 		}
 	}
 
 	/**
 	 * Splits the first part of a WEB.conf spec into a pair of Strings
 	 * representing the resource type and its DLNA folder.
 	 * 
 	 * @param spec
 	 *            (String) to be split
 	 * @return Array of (String) that represents the tokenized entry.
 	 */
 	private String[] parseFeedKey(String spec) {
 		String[] pair = StringUtils.split(spec, ".", 2);
 
 		if (pair == null || pair.length < 2) {
 			pair = new String[2];
 		}
 
 		if (pair[0] == null) {
 			pair[0] = "";
 		}
 
 		return pair;
 	}
 
 	/**
 	 * Splits the second part of a WEB.conf spec into a triple of Strings
 	 * representing the DLNA path, resource URI and optional thumbnail URI.
 	 * 
 	 * @param spec
 	 *            (String) to be split
 	 * @return Array of (String) that represents the tokenized entry.
 	 */
 	private String[] parseFeedValue(String spec) {
 		StringTokenizer st = new StringTokenizer(spec, ","); //$NON-NLS-1$
 		String triple[] = new String[3];
 		int i = 0;
 		while (st.hasMoreTokens()) {
 			triple[i++] = st.nextToken();
 		}
 		return triple;
 	}
 
 	/**
 	 * Returns iPhoto folder. Used by manageRoot, so it is usually used as a
 	 * folder at the root folder. Only works when PMS is run on MacOsX. TODO:
 	 * Requirements for iPhoto.
 	 */
 	private DLNAResource getiPhotoFolder() {
 		VirtualFolder res = null;
 		if (Platform.isMac()) {
 
 			Map<String, Object> iPhotoLib;
 			ArrayList<?> ListofRolls;
 			HashMap<?, ?> Roll;
 			HashMap<?, ?> PhotoList;
 			HashMap<?, ?> Photo;
 			ArrayList<?> RollPhotos;
 
 			try {
 				Process prc = Runtime.getRuntime().exec("defaults read com.apple.iApps iPhotoRecentDatabases");
 				BufferedReader in = new BufferedReader(new InputStreamReader(prc.getInputStream()));
 				String line = null;
 				if ((line = in.readLine()) != null) {
 					line = in.readLine(); // we want the 2nd line
 					line = line.trim(); // remove extra spaces
 					line = line.substring(1, line.length() - 1); // remove quotes and spaces
 				}
 				in.close();
 				if (line != null) {
 					URI tURI = new URI(line);
 					iPhotoLib = Plist.load(URLDecoder.decode(tURI.toURL().getFile(), System.getProperty("file.encoding"))); // loads the (nested) properties.
 					PhotoList = (HashMap<?, ?>) iPhotoLib.get("Master Image List"); // the list of photos
 					ListofRolls = (ArrayList<?>) iPhotoLib.get("List of Rolls"); // the list of events (rolls)
 					res = new VirtualFolder("iPhoto Library", null); //$NON-NLS-1$
 					for (Object item : ListofRolls) {
 						Roll = (HashMap<?, ?>) item;
 						VirtualFolder rf = new VirtualFolder(Roll.get("RollName").toString(), null); //$NON-NLS-1$
 						RollPhotos = (ArrayList<?>) Roll.get("KeyList"); // list of photos in an event (roll)
 						for (Object p : RollPhotos) {
 							Photo = (HashMap<?, ?>) PhotoList.get(p);
 							RealFile file = new RealFile(new File(Photo.get("ImagePath").toString()));
 							rf.addChild(file);
 						}
 						res.addChild(rf); //$NON-NLS-1$
 					}
 				} else {
 					logger.info("iPhoto folder not found !?");
 				}
 			} catch (Exception e) {
 				logger.error("Something went wrong with the iPhoto Library scan: ", e); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
 			}
 		}
 		return res;
 	}
 
 	/**
 	 * Returns the iTunes XML file. This file has all the information of the
 	 * iTunes database. The methods used in this function depends on whether PMS
 	 * runs on MacOsX or Windows.
 	 * 
 	 * @return (String) Absolute path to the iTunes XML file.
 	 * @throws Exception
 	 */
 	private String getiTunesFile() throws Exception {
 		String line = null;
 		String iTunesFile = null;
 		if (Platform.isMac()) {
 			Process prc = Runtime.getRuntime().exec("defaults read com.apple.iApps iTunesRecentDatabases");
 			BufferedReader in = new BufferedReader(new InputStreamReader(prc.getInputStream()));
 			if ((line = in.readLine()) != null) {
 				line = in.readLine(); // we want the 2nd line
 				line = line.trim(); // remove extra spaces
 				line = line.substring(1, line.length() - 1); // remove quotes and spaces
 				URI tURI = new URI(line);
 				iTunesFile = URLDecoder.decode(tURI.toURL().getFile(), "UTF8");
 			}
 			if (in != null) {
 				in.close();
 			}
 		} else if (Platform.isWindows()) {
 			Process prc = Runtime.getRuntime().exec("reg query \"HKCU\\Software\\Microsoft\\Windows\\CurrentVersion\\Explorer\\Shell Folders\" /v \"My Music\"");
 			BufferedReader in = new BufferedReader(new InputStreamReader(prc.getInputStream()));
 			String location = null;
 			while ((line = in.readLine()) != null) {
 				final String LOOK_FOR = "REG_SZ";
 				if (line.contains(LOOK_FOR)) {
 					location = line.substring(line.indexOf(LOOK_FOR) + LOOK_FOR.length()).trim();
 				}
 			}
 			if (in != null) {
 				in.close();
 			}
 			if (location != null) {
 				// add the itunes folder to the end
 				location = location + "\\iTunes\\iTunes Music Library.xml";
 				iTunesFile = location;
 			} else {
 				logger.info("Could not find the My Music folder");
 			}
 		}
 
 		return iTunesFile;
 	}
 
 	/**
 	 * Returns iTunes folder. Used by manageRoot, so it is usually used as a
 	 * folder at the root folder. Only works when PMS is run on MacOsX or
 	 * Windows.
 	 * <p>
 	 * The iTunes XML is parsed fully when this method is called, so it can take
 	 * some time for larger (+1000 albums) databases. TODO: Check if only music
 	 * is being added.
 	 * <P>
 	 * This method does not support genius playlists and does not provide a
 	 * media library.
 	 * 
 	 * @see RootFolder#getiTunesFile(boolean)
 	 */
 	private DLNAResource getiTunesFolder() {
 		DLNAResource res = null;
 		if (Platform.isMac() || Platform.isWindows()) {
 			Map<String, Object> iTunesLib;
 			ArrayList<?> Playlists;
 			HashMap<?, ?> Playlist;
 			HashMap<?, ?> Tracks;
 			HashMap<?, ?> Track;
 			ArrayList<?> PlaylistTracks;
 
 			try {
 				String iTunesFile = getiTunesFile();
 				if (iTunesFile != null && (new File(iTunesFile)).exists()) {
 					iTunesLib = Plist.load(URLDecoder.decode(iTunesFile, System.getProperty("file.encoding"))); // loads the (nested) properties.
 					Tracks = (HashMap<?, ?>) iTunesLib.get("Tracks"); // the list of tracks
 					Playlists = (ArrayList<?>) iTunesLib.get("Playlists"); // the list of Playlists
 					res = new VirtualFolder("iTunes Library", null); //$NON-NLS-1$
 					for (Object item : Playlists) {
 						Playlist = (HashMap<?, ?>) item;
 						VirtualFolder pf = new VirtualFolder(Playlist.get("Name").toString(), null); //$NON-NLS-1$
 						PlaylistTracks = (ArrayList<?>) Playlist.get("Playlist Items"); // list of tracks in a playlist
 						if (PlaylistTracks != null) {
 							for (Object t : PlaylistTracks) {
 								HashMap<?, ?> td = (HashMap<?, ?>) t;
 								Track = (HashMap<?, ?>) Tracks.get(td.get("Track ID").toString());
 								if (Track.get("Location").toString().startsWith("file://")) {
 									URI tURI2 = new URI(Track.get("Location").toString());
 									RealFile file = new RealFile(new File(URLDecoder.decode(tURI2.toURL().getFile(), "UTF-8")));
 									pf.addChild(file);
 								}
 							}
 						}
 						res.addChild(pf); //$NON-NLS-1$
 					}
 				} else {
 					logger.info("Could not find the iTunes file");
 				}
 			} catch (Exception e) {
 				logger.error("Something went wrong with the iTunes Library scan: ", e); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
 			}
 		}
 		return res;
 	}
 
 	/**
 	 * Returns Video Settings folder. Used by manageRoot, so it is usually used
 	 * as a folder at the root folder. Child objects are created when this
 	 * folder is created.
 	 */
 	private DLNAResource getVideoSettingssFolder() {
 		DLNAResource res = null;
 		if (!configuration.getHideVideoSettings()) {
 			res = new VirtualFolder(Messages.getString("PMS.37"), null); //$NON-NLS-1$
 			VirtualFolder vfSub = new VirtualFolder(Messages.getString("PMS.8"), null); //$NON-NLS-1$
 			res.addChild(vfSub);
 
 			res.addChild(new VirtualVideoAction(Messages.getString("PMS.3"), configuration.isMencoderNoOutOfSync()) { //$NON-NLS-1$
 				@Override
 				public boolean enable() {
 					configuration.setMencoderNoOutOfSync(!configuration
 							.isMencoderNoOutOfSync());
 					return configuration.isMencoderNoOutOfSync();
 				}
 			});
 
 			res.addChild(new VirtualVideoAction(Messages.getString("PMS.14"), configuration.isMencoderMuxWhenCompatible()) { //$NON-NLS-1$
 				@Override
 				public boolean enable() {
 					configuration.setMencoderMuxWhenCompatible(!configuration.isMencoderMuxWhenCompatible());
 
 					return configuration.isMencoderMuxWhenCompatible();
 				}
 			});
 
 			res.addChild(new VirtualVideoAction("  !!-- Fix 23.976/25fps A/V Mismatch --!!", configuration.isFix25FPSAvMismatch()) { //$NON-NLS-1$
 				@Override
 				public boolean enable() {
 					configuration.setMencoderForceFps(!configuration.isFix25FPSAvMismatch());
 					configuration.setFix25FPSAvMismatch(!configuration.isFix25FPSAvMismatch());
 					return configuration.isFix25FPSAvMismatch();
 				}
 			});
 
 			res.addChild(new VirtualVideoAction(Messages.getString("PMS.4"), configuration.isMencoderYadif()) { //$NON-NLS-1$
 				@Override
 				public boolean enable() {
 					configuration.setMencoderYadif(!configuration.isMencoderYadif());
 
 					return configuration.isMencoderYadif();
 				}
 			});
 
 			vfSub.addChild(new VirtualVideoAction(Messages.getString("PMS.10"), configuration.isMencoderDisableSubs()) { //$NON-NLS-1$
 				@Override
 				public boolean enable() {
 					boolean oldValue = configuration.isMencoderDisableSubs();
 					boolean newValue = !oldValue;
 					configuration.setMencoderDisableSubs(newValue);
 					return newValue;
 				}
 			});
 
 			vfSub.addChild(new VirtualVideoAction(Messages.getString("PMS.6"), configuration.getUseSubtitles()) { //$NON-NLS-1$
 				@Override
 				public boolean enable() {
 					boolean oldValue = configuration.getUseSubtitles();
 					boolean newValue = !oldValue;
 					configuration.setUseSubtitles(newValue);
 					return newValue;
 				}
 			});
 
 			vfSub.addChild(new VirtualVideoAction(Messages.getString("MEncoderVideo.36"), configuration.isMencoderAssDefaultStyle()) { //$NON-NLS-1$
 				@Override
 				public boolean enable() {
 					boolean oldValue = configuration.isMencoderAssDefaultStyle();
 					boolean newValue = !oldValue;
 					configuration.setMencoderAssDefaultStyle(newValue);
 					return newValue;
 				}
 			});
 
 			res.addChild(new VirtualVideoAction(Messages.getString("PMS.7"), configuration.getSkipLoopFilterEnabled()) { //$NON-NLS-1$
 				@Override
 				public boolean enable() {
 					configuration.setSkipLoopFilterEnabled(!configuration.getSkipLoopFilterEnabled());
 					return configuration.getSkipLoopFilterEnabled();
 				}
 			});
 
 			res.addChild(new VirtualVideoAction(Messages.getString("PMS.27"), true) { //$NON-NLS-1$
 				@Override
 				public boolean enable() {
 					try {
 						configuration.save();
 					} catch (ConfigurationException e) {
 					}
 					return true;
 				}
 			});
 
 			res.addChild(new VirtualVideoAction(Messages.getString("LooksFrame.12"), true) { //$NON-NLS-1$
 				@Override
 				public boolean enable() {
 					try {
 						PMS.get().reset();
 					} catch (IOException e) {
 					}
 					return true;
 				}
 			});
 		}
 		return res;
 	}
 
 	/**
 	 * Returns as many folders as plugins providing root folders are loaded into
 	 * memory (need to implement AdditionalFolder(s)AtRoot)
 	 */
 	private List<DLNAResource> getAdditionalFoldersAtRoot() {
 		List<DLNAResource> res = new ArrayList<DLNAResource>();
 		for (ExternalListener listener : ExternalFactory.getExternalListeners()) {
 			if (listener instanceof AdditionalFolderAtRoot) {
 				res.add(((AdditionalFolderAtRoot) listener).getChild());
 			} else if (listener instanceof AdditionalFoldersAtRoot) {
 				java.util.Iterator<DLNAResource> folders = ((AdditionalFoldersAtRoot) listener).getChildren();
 				while (folders.hasNext()) {
 					res.add(folders.next());
 				}
 			}
 		}
 		return res;
 	}
 }
