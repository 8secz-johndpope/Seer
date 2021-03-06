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
 
 package net.pms;
 
 import java.awt.GraphicsEnvironment;
 import java.awt.Toolkit;
 import java.io.BufferedReader;
 import java.io.ByteArrayInputStream;
 import java.io.File;
 import java.io.FileInputStream;
 import java.io.IOException;
 import java.io.InputStreamReader;
 import java.io.LineNumberReader;
 import java.io.PrintStream;
 import java.net.BindException;
 import java.net.InetAddress;
 import java.net.NetworkInterface;
 import java.net.URI;
 import java.net.URLDecoder;
 import java.text.SimpleDateFormat;
 import java.util.ArrayList;
 import java.util.HashMap;
 import java.util.Iterator;
 import java.util.Locale;
 import java.util.Map;
 import java.util.Map.Entry;
 import java.util.StringTokenizer;
 import java.util.UUID;
 import java.util.logging.LogManager;
 
 import net.pms.configuration.MapFileConfiguration;
 import net.pms.configuration.PmsConfiguration;
 import net.pms.configuration.RendererConfiguration;
 import net.pms.dlna.AudiosFeed;
 import net.pms.dlna.DLNAMediaDatabase;
 import net.pms.dlna.DLNAResource;
 import net.pms.dlna.ImagesFeed;
 import net.pms.dlna.RealFile;
 import net.pms.dlna.RootFolder;
 import net.pms.dlna.VideosFeed;
 import net.pms.dlna.WebAudioStream;
 import net.pms.dlna.WebVideoStream;
 import net.pms.dlna.virtual.MediaLibrary;
 import net.pms.dlna.virtual.VirtualFolder;
 import net.pms.dlna.virtual.VirtualVideoAction;
 import net.pms.encoders.FFMpegAudio;
 import net.pms.encoders.FFMpegDVRMSRemux;
 import net.pms.encoders.FFMpegVideo;
 import net.pms.encoders.MEncoderAviSynth;
 import net.pms.encoders.MEncoderVideo;
 import net.pms.encoders.MEncoderWebVideo;
 import net.pms.encoders.MPlayerAudio;
 import net.pms.encoders.MPlayerWebAudio;
 import net.pms.encoders.MPlayerWebVideoDump;
 import net.pms.encoders.Player;
 import net.pms.encoders.RAWThumbnailer;
 import net.pms.encoders.TSMuxerVideo;
 import net.pms.encoders.TsMuxerAudio;
 import net.pms.encoders.VideoLanAudioStreaming;
 import net.pms.encoders.VideoLanVideoStreaming;
 import net.pms.external.AdditionalFolderAtRoot;
 import net.pms.external.AdditionalFoldersAtRoot;
 import net.pms.external.ExternalFactory;
 import net.pms.external.ExternalListener;
 import net.pms.formats.DVRMS;
 import net.pms.formats.FLAC;
 import net.pms.formats.Format;
 import net.pms.formats.GIF;
 import net.pms.formats.ISO;
 import net.pms.formats.JPG;
 import net.pms.formats.M4A;
 import net.pms.formats.MKV;
 import net.pms.formats.MP3;
 import net.pms.formats.MPG;
 import net.pms.formats.OGG;
 import net.pms.formats.PNG;
 import net.pms.formats.RAW;
 import net.pms.formats.TIF;
 import net.pms.formats.WEB;
 import net.pms.gui.DummyFrame;
 import net.pms.gui.IFrame;
 import net.pms.io.OutputParams;
 import net.pms.io.OutputTextConsumer;
 import net.pms.io.ProcessWrapperImpl;
 import net.pms.io.WinUtils;
 import net.pms.logging.LoggingConfigFileLoader;
 import net.pms.network.HTTPServer;
 import net.pms.network.ProxyServer;
 import net.pms.network.UPNPHelper;
 import net.pms.newgui.LooksFrame;
 import net.pms.newgui.NetworkTab;
 import net.pms.update.AutoUpdater;
 import net.pms.util.PMSUtil;
 import net.pms.util.ProcessUtil;
 import net.pms.util.SystemErrWrapper;
 import net.pms.xmlwise.Plist;
 
 import org.slf4j.Logger;
 import org.slf4j.LoggerFactory;
 
 import org.apache.commons.configuration.ConfigurationException;
 import org.apache.commons.lang.StringUtils;
 
 import com.sun.jna.Platform;
 
 public class PMS {
 
 	private static final String SCROLLBARS = "scrollbars"; //$NON-NLS-1$
 	private static final String NATIVELOOK = "nativelook"; //$NON-NLS-1$
 	private static final String CONSOLE = "console"; //$NON-NLS-1$
 	private static final String NOCONSOLE = "noconsole"; //$NON-NLS-1$
 
 	/**
 	 * Update URL used in the {@link AutoUpdater}.
 	 */
 	private static final String UPDATE_SERVER_URL = "http://ps3mediaserver.googlecode.com/svn/trunk/ps3mediaserver/update.data"; //$NON-NLS-1$
 	/**
 	 * Version showed in the UPnP XML descriptor and logs.
 	 */
 	public static final String VERSION = "1.30.0"; //$NON-NLS-1$
 	public static final String AVS_SEPARATOR = "\1"; //$NON-NLS-1$
 
 	// (innot): The logger used for all logging.
 	private static final Logger logger = LoggerFactory.getLogger(PMS.class);
 	// TODO(tcox):  This shouldn't be static
 	private static PmsConfiguration configuration;
 
 	/**Returns a pointer to the main PMS GUI.
 	 * @return {@link IFrame} Main PMS window.
 	 */
 	public IFrame getFrame() {
 		return frame;
 	}
 
 	/**getRootFolder returns the Root Folder for a given renderer. There could be the case
 	 * where a given media renderer needs a different root structure.
 	 * @param renderer {@link RendererConfiguration} is the renderer for which to get the RootFolder structure. If <b>null</b>, then
 	 * the default renderer is used.
 	 * @return {@link RootFolder} The root folder structure for a given renderer
 	 */
 	public RootFolder getRootFolder(RendererConfiguration renderer) {
 		return getRootFolder(renderer, true);
 	}
 
 	private RootFolder getRootFolder(RendererConfiguration renderer, boolean initialize) {
 		// something to do here for multiple directories views for each renderer
 		if (renderer == null) {
 			renderer = RendererConfiguration.getDefaultConf();
 		}
 		return renderer.getRootFolder(initialize);
 	}
 	/**
 	 * Pointer to a running PMS server.
 	 */
 	private static PMS instance = null;
 	/**
 	 * Semaphore used in order to not create two PMS instances at the same time.
 	 */
 	private static byte[] lock = null;
 
 	static {
 		lock = new byte[0];
 		sdfHour = new SimpleDateFormat("HH:mm:ss.SSS", Locale.US); //$NON-NLS-1$
 		sdfDate = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US); //$NON-NLS-1$
 	}
 	/**
 	 * Array of {@link RendererConfiguration} that have been found by PMS.
 	 */
 	private final ArrayList<RendererConfiguration> foundRenderers = new ArrayList<RendererConfiguration>();
 
 	/**Adds a {@link RendererConfiguration} to the list of media renderers found. The list is being used, for
 	 * example, to give the user a graphical representation of the found media renderers.
 	 * @param mediarenderer {@link RendererConfiguration}
 	 */
 	public void setRendererfound(RendererConfiguration mediarenderer) {
 		if (!foundRenderers.contains(mediarenderer) && !mediarenderer.isFDSSDP()) {
 			foundRenderers.add(mediarenderer);
 			frame.addRendererIcon(mediarenderer.getRank(), mediarenderer.getRendererNameWithAddress(), mediarenderer.getRendererIcon());
 			if (mediarenderer.isPS3()) {
 				frame.setStatusCode(0, Messages.getString("PMS.5"), "clients/ps3slim_220.png"); //$NON-NLS-1$ //$NON-NLS-2$
 			}
 		}
 		/*if (mediarenderer == HTTPResource.PS3) {
 		frame.setStatusCode(0, Messages.getString("PMS.5"), "PS3_2.png"); //$NON-NLS-1$ //$NON-NLS-2$
 		} else if (mediarenderer == HTTPResource.XBOX && !foundRenderers.contains(HTTPResource.PS3)) {
 		frame.setStatusCode(0, "Xbox found", "xbox360.png"); //$NON-NLS-1$ //$NON-NLS-2$
 		}*/
 	}
 	/**
 	 * HTTP server that serves the XML files needed by UPnP server and the media files.
 	 */
 	private HTTPServer server;
 	/**
 	 * User friendly name for the server.
 	 */
 	private String serverName;
 	private ArrayList<Format> extensions;
 	/**
 	 * List of registered {@link Player}s.
 	 */
 	private ArrayList<Player> players;
 	private ArrayList<Player> allPlayers;
 
 	/**
 	 * @return ArrayList of {@link Player}s.
 	 */
 	public ArrayList<Player> getAllPlayers() {
 		return allPlayers;
 	}
 	private ProxyServer proxyServer;
 
 	public ProxyServer getProxy() {
 		return proxyServer;
 	}
 	public static SimpleDateFormat sdfDate;
 	public static SimpleDateFormat sdfHour;
 	public ArrayList<Process> currentProcesses = new ArrayList<Process>();
 
 	private PMS() {
 	}
 	/**
 	 * {@link IFrame} object that represents PMS GUI.
 	 */
 	IFrame frame;
 
 	/**
 	 * @see Platform#isWindows()
 	 */
 	public boolean isWindows() {
 		return Platform.isWindows();
 	}
 	private int proxy;
 	/**Interface to Windows specific functions, like Windows Registry. registry is set by {@link #init()}.
 	 * @see WinUtils
 	 */
 	private WinUtils registry;
 
 	/**
 	 * @see WinUtils
 	 */
 	public WinUtils getRegistry() {
 		return registry;
 	}
 
 	/**Executes a new Process and creates a fork that waits for its results. 
 	 * TODO:Extend explanation on where this is being used.
 	 * @param name Symbolic name for the process to be launched, only used in the trace log
 	 * @param error (boolean) Set to true if you want PMS to add error messages to the trace pane
 	 * @param workDir (File) optional working directory to run the process in
 	 * @param params (array of Strings) array containing the command to call and its arguments
 	 * @return Returns true if the command exited as expected
 	 * @throws Exception TODO: Check which exceptions to use
 	 */
 	private boolean checkProcessExistence(String name, boolean error, File workDir, String... params) throws Exception {
 		logger.debug("launching: " + params[0]); //$NON-NLS-1$
 
 		try {
 			ProcessBuilder pb = new ProcessBuilder(params);
 			if (workDir != null) {
 				pb.directory(workDir);
 			}
 			final Process process = pb.start();
 
 			OutputTextConsumer stderrConsumer = new OutputTextConsumer(process.getErrorStream(), false);
 			stderrConsumer.start();
 
 			OutputTextConsumer outConsumer = new OutputTextConsumer(process.getInputStream(), false);
 			outConsumer.start();
 
 			Runnable r = new Runnable() {
 
 				public void run() {
 					ProcessUtil.waitFor(process);
 				}
 			};
 
 			Thread checkThread = new Thread(r);
 			checkThread.start();
 			checkThread.join(60000);
 			checkThread.interrupt();
 			checkThread = null;
 
 			// XXX no longer used
 			if (params[0].equals("vlc") && stderrConsumer.getResults().get(0).startsWith("VLC")) //$NON-NLS-1$ //$NON-NLS-2$
 			{
 				return true;
 			}
 			// XXX no longer used
 			if (params[0].equals("ffmpeg") && stderrConsumer.getResults().get(0).startsWith("FF")) //$NON-NLS-1$ //$NON-NLS-2$
 			{
 				return true;
 			}
 			int exit = process.exitValue();
 			if (exit != 0) {
 				if (error) {
 					logger.info("[" + exit + "] Cannot launch " + name + " / Check the presence of " + params[0] + " ..."); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
 				}
 				return false;
 			}
 			return true;
 		} catch (Exception e) {
 			if (error) {
 				logger.error("Cannot launch " + name + " / Check the presence of " + params[0] + " ...", e); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
 			}
 			return false;
 		}
 	}

 	/**
 	 * @see System#err
 	 */
 	@SuppressWarnings("unused")
 	private final PrintStream stderr = System.err;

 	/**Main resource database that supports search capabilities. Also known as media cache.
 	 * @see DLNAMediaDatabase
 	 */
 	private DLNAMediaDatabase database;
 
 	/**Used to get the database. Needed in the case of the Xbox 360, that requires a database.
 	 * for its queries.
 	 * @return (DLNAMediaDatabase) If there exists a database register with the program, a pointer to it is returned.
 	 * If there is no database in memory, a new one is created. If the option to use a "cache" is deactivated, returns <b>null</b>.
 	 */
 	public synchronized DLNAMediaDatabase getDatabase() {
 		if (configuration.getUseCache()) {
 			if (database == null) {
 				database = new DLNAMediaDatabase("medias"); //$NON-NLS-1$
 				database.init(false);
 				/*try {
 				org.h2.tools.Server server = org.h2.tools.Server.createWebServer(null);
 				server.start();
 				logger.info("Starting H2 console on port " + server.getPort());
 				} catch (Exception e) {
 				e.printStackTrace();
 				}*/
 			}
 			return database;
 		}
 		return null;
 	}
 
 	/**Initialisation procedure for PMS.
 	 * @return true if the server has been initialized correctly. false if the server could
 	 * not be set to listen on the UPnP port.
 	 * @throws Exception
 	 */
 	private boolean init() throws Exception {
 		registry = new WinUtils();
 
 		AutoUpdater autoUpdater = new AutoUpdater(UPDATE_SERVER_URL, VERSION);
 		if (System.getProperty(CONSOLE) == null) {//$NON-NLS-1$
 			frame = new LooksFrame(autoUpdater, configuration);
 			autoUpdater.pollServer();
 		} else {
 			System.out.println("GUI environment not available"); //$NON-NLS-1$
 			System.out.println("Switching to console mode"); //$NON-NLS-1$
 			frame = new DummyFrame();
 		}
 
 		frame.setStatusCode(0, Messages.getString("PMS.130"), "connect_no-220.png"); //$NON-NLS-1$ //$NON-NLS-2$
 		proxy = -1;
 
 		logger.info("Starting PS3 Media Server " + VERSION); //$NON-NLS-1$
 		logger.info("by shagrath / 2008-2011"); //$NON-NLS-1$
 		logger.info("http://ps3mediaserver.org"); //$NON-NLS-1$
 		logger.info("http://code.google.com/p/ps3mediaserver"); //$NON-NLS-1$
 		logger.info("http://ps3mediaserver.blogspot.com"); //$NON-NLS-1$
 		logger.info(""); //$NON-NLS-1$
 		logger.info("Java: " + System.getProperty("java.version") + "-" + System.getProperty("java.vendor")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
 		logger.info("OS: " + System.getProperty("os.name") + " " + System.getProperty("os.arch") + " " + System.getProperty("os.version")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$
 		logger.info("Encoding: " + System.getProperty("file.encoding")); //$NON-NLS-1$ //$NON-NLS-2$
 
 		String cwd = new File("").getAbsolutePath();
 		logger.info("Working directory: " + cwd);
 
 		logger.info("Temp folder: " + configuration.getTempFolder()); //$NON-NLS-1$
 		logger.info("Logging config file: " + LoggingConfigFileLoader.getConfigFilePath()); //$NON-NLS-1$
 		HashMap<String, String> lfps = LoggingConfigFileLoader.getLogFilePaths();
 		if(lfps != null && lfps.size() > 0) {
 			if(lfps.size() == 1) {
 				Entry<String, String> entry = lfps.entrySet().iterator().next();
 				logger.info(String.format("%s: %s", entry.getKey(), entry.getValue()));
 			} else {
 				logger.info("Logging to multiple files:");
 				Iterator<Entry<String, String>> logsIterator = lfps.entrySet().iterator();
 				Entry<String, String> entry;
 				while(logsIterator.hasNext()) {
 					entry = logsIterator.next();
 					logger.info(String.format("%s: %s", entry.getKey(), entry.getValue()));
 				}
 			}
 		}
 		logger.info(""); //$NON-NLS-1$
 
 		logger.info("Profile directory: " + configuration.getProfileDirectory());
 		String profilePath = configuration.getProfilePath();
 		logger.info("Profile path: " + profilePath);
 
 		File profileFile =  new File(profilePath);
 
 		if (profileFile.exists()) {
 			String status = String.format("%s%s",
 				profileFile.canRead()    ? "r" : "-",
 				profileFile.canWrite()   ? "w" : "-"
 			);
 			logger.info("Profile status: " + status);
 		} else {
 			logger.info("Profile status: no such file");
 		}
 
 		logger.info("Profile name: " + configuration.getProfileName()); //$NON-NLS-1$
 		logger.info(""); //$NON-NLS-1$
 
 		RendererConfiguration.loadRendererConfigurations();
 
 		logger.info("Checking MPlayer font cache. It can take a minute or so.");
 		checkProcessExistence("MPlayer", true, null, configuration.getMplayerPath(), "dummy");
       		if(isWindows()) {
 			checkProcessExistence("MPlayer", true, configuration.getTempFolder(), configuration.getMplayerPath(), "dummy");
 		}
 		logger.info("Done!");
 
 		// check the existence of Vsfilter.dll
 		if (registry.isAvis() && registry.getAvsPluginsDir() != null) {
 			logger.info("Found AviSynth plugins dir: " + registry.getAvsPluginsDir().getAbsolutePath()); //$NON-NLS-1$
 			File vsFilterdll = new File(registry.getAvsPluginsDir(), "VSFilter.dll"); //$NON-NLS-1$
 			if (!vsFilterdll.exists()) {
 				logger.info("VSFilter.dll is not in the AviSynth plugins directory. This can cause problems when trying to play subtitled videos with AviSynth"); //$NON-NLS-1$
 			}
 		}
 
 		if (registry.getVlcv() != null && registry.getVlcp() != null) {
 			logger.info("Found VideoLAN version " + registry.getVlcv() + " at: " + registry.getVlcp()); //$NON-NLS-1$ //$NON-NLS-2$
 		}
 
 		//check if Kerio is installed
 		if (registry.isKerioFirewall()) {
 			//todo: Warning message
 		}
 
 		// force use of specific dvr ms muxer when it's installed in the right place
 		File dvrsMsffmpegmuxer = new File("win32/dvrms/ffmpeg_MPGMUX.exe"); //$NON-NLS-1$
 		if (dvrsMsffmpegmuxer.exists()) {
 			configuration.setFfmpegAlternativePath(dvrsMsffmpegmuxer.getAbsolutePath());
 		}
 
 		// disable jaudiotagger logging
 		LogManager.getLogManager().readConfiguration(new ByteArrayInputStream("org.jaudiotagger.level=OFF".getBytes())); //$NON-NLS-1$
 
 		// wrap System.err
 		System.setErr(new PrintStream(new SystemErrWrapper(), true));
 
 		extensions = new ArrayList<Format>();
 		players = new ArrayList<Player>();
 		allPlayers = new ArrayList<Player>();
 		server = new HTTPServer(configuration.getServerPort());
 
 		registerExtensions();
 
 		/*
 		 * XXX: keep this here (i.e. after registerExtensions and before registerPlayers) so that plugins
 		 * can register custom players correctly (e.g. in the GUI) and/or add/replace custom formats
 		 *
 		 * XXX: if a plugin requires initialization/notification even earlier than
 		 * this, then a new external listener implementing a new callback should be added
 		 * e.g. StartupListener.registeredExtensions()
 		 */
 		try {
 			ExternalFactory.lookup();
 		} catch (Exception e) {
 			logger.error("Error loading plugins", e);
 		}
 
 		// a static block in Player doesn't work (i.e. is called too late).
 		// this must always be called *after* the plugins have loaded.
 		// here's as good a place as any
 		Player.initializeFinalizeTranscoderArgsListeners();
 		registerPlayers();
 
 		getRootFolder(RendererConfiguration.getDefaultConf());
 
 		boolean binding = false;
 		try {
 			binding = server.start();
 		} catch (BindException b) {
 			logger.info("FATAL ERROR: Unable to bind on port: " + configuration.getServerPort() + ", because: " + b.getMessage()); //$NON-NLS-1$ //$NON-NLS-2$
 			logger.info("Maybe another process is running or the hostname is wrong."); //$NON-NLS-1$
 		}
 		new Thread() {
 
 			@Override
 			public void run() {
 				try {
 					Thread.sleep(7000);
 				} catch (InterruptedException e) {
 				}
 				boolean ps3found = false;
 				for (RendererConfiguration r : foundRenderers) {
 					if (r.isPS3()) {
 						ps3found = true;
 					}
 				}
 				if (!ps3found) {
 					if (foundRenderers.isEmpty()) {
 						frame.setStatusCode(0, Messages.getString("PMS.0"), "messagebox_critical-220.png"); //$NON-NLS-1$ //$NON-NLS-2$
 					} else {
 						frame.setStatusCode(0, Messages.getString("PMS.15"), "messagebox_warning-220.png"); //$NON-NLS-1$ //$NON-NLS-2$
 					}
 				}
 			}
 		}.start();
 		if (!binding) {
 			return false;
 		}
 		if (proxy > 0) {
 			logger.info("Starting HTTP Proxy Server on port: " + proxy); //$NON-NLS-1$
 			proxyServer = new ProxyServer(proxy);
 		}
 
 		if (getDatabase() != null) {
 			logger.info("A tiny media library admin interface is available at: http://" + server.getHost() + ":" + server.getPort() + "/console/home");
 		}
 
 		frame.serverReady();
 
 		//UPNPHelper.sendByeBye();
 		Runtime.getRuntime().addShutdownHook(new Thread() {
 
 			@Override
 			public void run() {
 				try {
 					for (ExternalListener l : ExternalFactory.getExternalListeners()) {
 						l.shutdown();
 					}
 					UPNPHelper.shutDownListener();
 					UPNPHelper.sendByeBye();
 					logger.debug("Forcing shutdown of all active processes"); //$NON-NLS-1$
 					for (Process p : currentProcesses) {
 						try {
 							p.exitValue();
 						} catch (IllegalThreadStateException ise) {
 							logger.trace("Forcing shutdown of process: " + p); //$NON-NLS-1$
 							ProcessUtil.destroy(p);
 						}
 					}
 					get().getServer().stop();
 					Thread.sleep(500);
 				} catch (Exception e) {
 				}
 			}
 		});
 
 		UPNPHelper.sendAlive();
 		logger.trace("Waiting 250 milliseconds..."); //$NON-NLS-1$
 		Thread.sleep(250);
 		UPNPHelper.listen();
 
 		return true;
 	}
 
 	/**Creates a new Root folder for a given configuration. It adds following folders in this order:
 	 * <ol><li>Directory folders as stated in the configuration pane
 	 * <li>Web nodes
 	 * <li>iPhoto
 	 * <li>iTunes
 	 * <li>Media Library
 	 * <li>Folders created by plugins
 	 * <li>Video settings
 	 * </ol>
 	 * @param renderer {@link RendererConfiguration} to be managed.
 	 * @throws IOException
 	 */
 	public void manageRoot(RendererConfiguration renderer) throws IOException {
 		File files[] = loadFoldersConf(configuration.getFolders(), true);
 		if (files == null || files.length == 0) {
 			files = File.listRoots();
 		}
 		/*
 		 * initialize == false: make sure renderer.getRootFolder() doesn't call back into this method
 		 * if it creates a new root folder
 		 *
 		 * this avoids a redundant call to this method, and prevents loadFoldersConf()
 		 * being called twice for each renderer
 		 */
 		RootFolder rootFolder = getRootFolder(renderer, false);
 
 		rootFolder.browse(files);
 		rootFolder.browse(MapFileConfiguration.parse(configuration.getVirtualFolders()));
 
 		// FIXME: this (the WEB.conf path) should be fully configurable
 		File webConf = new File(configuration.getProfileDirectory(), "WEB.conf"); //$NON-NLS-1$
 
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
 									DLNAResource currentRoot = rootFolder;
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
 									parent = rootFolder;
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
 
 		if (Platform.isMac()) {
 			if (configuration.getIphotoEnabled()) {
 				addiPhotoFolder(renderer);
 			}
 		}
 
 		if (Platform.isMac() || Platform.isWindows()) {
 			if (configuration.getItunesEnabled()) {
 				addiTunesFolder(renderer);
 			}
 		}
 
 		addMediaLibraryFolder(renderer);
 
 		addAdditionalFoldersAtRoot(renderer);
 
 		addVideoSettingssFolder(renderer);
 
 		rootFolder.closeChildren(0, false);
 	}
 
 	/**Adds iPhoto folder. Used by manageRoot, so it is usually used as a folder at the
 	 * root folder. Only works when PMS is run on MacOsX.
 	 * TODO: Requirements for iPhoto.
 	 * @param renderer
 	 */
 	public void addiPhotoFolder(RendererConfiguration renderer) {
 		if (Platform.isMac()) {
 
 			Map<String, Object> iPhotoLib;
 			ArrayList<?> ListofRolls;
 			HashMap<?, ?> Roll;
 			HashMap<?, ?> PhotoList;
 			HashMap<?, ?> Photo;
 			ArrayList<?> RollPhotos;
 
 			try {
 				Process prc = Runtime.getRuntime().exec("defaults read com.apple.iApps iPhotoRecentDatabases");
 				BufferedReader in = new BufferedReader(
 					new InputStreamReader(prc.getInputStream()));
 				String line = null;
 				if ((line = in.readLine()) != null) {
 					line = in.readLine();		//we want the 2nd line
 					line = line.trim();		//remove extra spaces
 					line = line.substring(1, line.length() - 1); // remove quotes and spaces
 				}
 				in.close();
 				if (line != null) {
 					URI tURI = new URI(line);
 					iPhotoLib = Plist.load(URLDecoder.decode(tURI.toURL().getFile(), System.getProperty("file.encoding")));    // loads the (nested) properties.
 					PhotoList = (HashMap<?, ?>) iPhotoLib.get("Master Image List");	// the list of photos
 					ListofRolls = (ArrayList<?>) iPhotoLib.get("List of Rolls");	// the list of events (rolls)
 					VirtualFolder vf = new VirtualFolder("iPhoto Library", null); //$NON-NLS-1$
 					for (Object item : ListofRolls) {
 						Roll = (HashMap<?, ?>) item;
 						VirtualFolder rf = new VirtualFolder(Roll.get("RollName").toString(), null); //$NON-NLS-1$
 						RollPhotos = (ArrayList<?>) Roll.get("KeyList");	// list of photos in an event (roll)
 						for (Object p : RollPhotos) {
 							Photo = (HashMap<?, ?>) PhotoList.get(p);
 							RealFile file = new RealFile(new File(Photo.get("ImagePath").toString()));
 							rf.addChild(file);
 						}
 						vf.addChild(rf); //$NON-NLS-1$
 					}
 					getRootFolder(renderer).addChild(vf);
 				} else {
 					logger.info("iPhoto folder not found !?");
 				}
 			} catch (Exception e) {
 				logger.error("Something went wrong with the iPhoto Library scan: ", e); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
 			}
 		}
 	}
 
 	/**Returns the iTunes XML file. This file has all the information of the iTunes
 	 * database. The methods used in this function depends on whether PMS runs on 
 	 * MacOsX or Windows.
 	 * @param isOsx deprecated and not used
 	 * @return (String) Absolute path to the iTunes XML file.
 	 * @throws Exception
 	 * @see {@link PMS#addiTunesFolder(RendererConfiguration)}
 	 */
 	@SuppressWarnings("deprecation")
 	private String getiTunesFile(boolean isOsx) throws Exception {
 		String line = null;
 		String iTunesFile = null;
 		if (Platform.isMac()) {
 			Process prc = Runtime.getRuntime().exec("defaults read com.apple.iApps iTunesRecentDatabases");
 			BufferedReader in = new BufferedReader(new InputStreamReader(prc.getInputStream()));
 			if ((line = in.readLine()) != null) {
 				line = in.readLine();           //we want the 2nd line
 				line = line.trim();             //remove extra spaces
 				line = line.substring(1, line.length() - 1); // remove quotes and spaces
 				URI tURI = new URI(line);
 				iTunesFile = URLDecoder.decode(tURI.toURL().getFile());
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
 
 	/**Adds iTunes folder. Used by manageRoot, so it is usually used as a folder at the
 	 * root folder. Only works when PMS is run on MacOsX or Windows.<p>
 	 * The iTunes XML is parsed fully when this method is called, so it can take some time for
 	 * larger (+1000 albums) databases. TODO: Check if only music is being added.<P>
 	 * This method does not support genius playlists and does not provide a media library.
 	 * @param renderer {@link RendererConfiguration} Which media renderer to add this folder to.
 	 * @see PMS#manageRoot(RendererConfiguration)
 	 * @see PMS#getiTunesFile(boolean)
 	 */
 	public void addiTunesFolder(RendererConfiguration renderer) {
 		if (Platform.isMac() || Platform.isWindows()) {
 			Map<String, Object> iTunesLib;
 			ArrayList<?> Playlists;
 			HashMap<?, ?> Playlist;
 			HashMap<?, ?> Tracks;
 			HashMap<?, ?> Track;
 			ArrayList<?> PlaylistTracks;
 
 			try {
 				String iTunesFile = getiTunesFile(Platform.isMac());
 				if (iTunesFile != null && (new File(iTunesFile)).exists()) {
 					iTunesLib = Plist.load(URLDecoder.decode(iTunesFile, System.getProperty("file.encoding")));     // loads the (nested) properties.
 					Tracks = (HashMap<?, ?>) iTunesLib.get("Tracks");       // the list of tracks
 					Playlists = (ArrayList<?>) iTunesLib.get("Playlists");       // the list of Playlists
 					VirtualFolder vf = new VirtualFolder("iTunes Library", null); //$NON-NLS-1$
 					for (Object item : Playlists) {
 						Playlist = (HashMap<?, ?>) item;
 						VirtualFolder pf = new VirtualFolder(Playlist.get("Name").toString(), null); //$NON-NLS-1$
 						PlaylistTracks = (ArrayList<?>) Playlist.get("Playlist Items");   // list of tracks in a playlist
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
 						vf.addChild(pf); //$NON-NLS-1$
 					}
 					getRootFolder(renderer).addChild(vf);
 				} else {
 					logger.info("Could not find the iTunes file");
 				}
 			} catch (Exception e) {
 				logger.error("Something went wrong with the iTunes Library scan: ", e); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
 			}
 		}
 	}
 
 	/**Adds Video Settings folder. Used by manageRoot, so it is usually used as a folder at the
 	 * root folder. Child objects are created when this folder is created.
 	 * @param renderer {@link RendererConfiguration} Which media renderer to add this folder to.
 	 * @see PMS#manageRoot(RendererConfiguration)
 	 */
 	public void addVideoSettingssFolder(RendererConfiguration renderer) {
 		if (!configuration.getHideVideoSettings()) {
 			VirtualFolder vf = new VirtualFolder(Messages.getString("PMS.37"), null); //$NON-NLS-1$
 			VirtualFolder vfSub = new VirtualFolder(Messages.getString("PMS.8"), null); //$NON-NLS-1$
 			vf.addChild(vfSub);
 
 			vf.addChild(new VirtualVideoAction(Messages.getString("PMS.3"), configuration.isMencoderNoOutOfSync()) { //$NON-NLS-1$
 
 				@Override
 				public boolean enable() {
 					configuration.setMencoderNoOutOfSync(!configuration.isMencoderNoOutOfSync());
 					return configuration.isMencoderNoOutOfSync();
 				}
 			});
 
 			vf.addChild(new VirtualVideoAction(Messages.getString("PMS.14"), configuration.isMencoderMuxWhenCompatible()) {  //$NON-NLS-1$
 
 				@Override
 				public boolean enable() {
 					configuration.setMencoderMuxWhenCompatible(!configuration.isMencoderMuxWhenCompatible());
 
 					return configuration.isMencoderMuxWhenCompatible();
 				}
 			});
 
 			vf.addChild(new VirtualVideoAction("  !!-- Fix 23.976/25fps A/V Mismatch --!!", configuration.isFix25FPSAvMismatch()) { //$NON-NLS-1$
 
 				@Override
 				public boolean enable() {
 					configuration.setMencoderForceFps(!configuration.isFix25FPSAvMismatch());
 					configuration.setFix25FPSAvMismatch(!configuration.isFix25FPSAvMismatch());
 					return configuration.isFix25FPSAvMismatch();
 				}
 			});
 
 
 			vf.addChild(new VirtualVideoAction(Messages.getString("PMS.4"), configuration.isMencoderYadif()) { //$NON-NLS-1$
 
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
 
 			vf.addChild(new VirtualVideoAction(Messages.getString("PMS.7"), configuration.getSkipLoopFilterEnabled()) { //$NON-NLS-1$
 
 				@Override
 				public boolean enable() {
 					configuration.setSkipLoopFilterEnabled(!configuration.getSkipLoopFilterEnabled());
 					return configuration.getSkipLoopFilterEnabled();
 				}
 			});
 
 			vf.addChild(new VirtualVideoAction(Messages.getString("PMS.27"), true) { //$NON-NLS-1$
 
 				@Override
 				public boolean enable() {
 					try {
 						configuration.save();
 					} catch (ConfigurationException e) {
 					}
 					return true;
 				}
 			});
 
 			vf.addChild(new VirtualVideoAction(Messages.getString("LooksFrame.12"), true) { //$NON-NLS-1$
 
 				@Override
 				public boolean enable() {
 					try {
 						get().reset();
 					} catch (IOException e) {
 					}
 					return true;
 				}
 			});
 			//vf.closeChildren(0, false);
 			getRootFolder(renderer).addChild(vf);
 		}
 	}
 
 	/**Adds as many folders as plugins providing root folders are loaded into memory (need to implement AdditionalFolderAtRoot)
 	 * @param renderer {@link RendererConfiguration} Which media renderer to add this folder to.
 	 * @see PMS#manageRoot(RendererConfiguration)
 	 */
 	private void addAdditionalFoldersAtRoot(RendererConfiguration renderer) {
 		RootFolder rootFolder = getRootFolder(renderer);
 		for (ExternalListener listener : ExternalFactory.getExternalListeners()) {
 			if (listener instanceof AdditionalFolderAtRoot) {
 				rootFolder.addChild(((AdditionalFolderAtRoot) listener).getChild());
 			} else if (listener instanceof AdditionalFoldersAtRoot) {
 				java.util.Iterator<DLNAResource> folders = ((AdditionalFoldersAtRoot) listener).getChildren();
 				while (folders.hasNext()) {
 					rootFolder.addChild(folders.next());
 				}
 			}
 		}
 	}
 	//private boolean mediaLibraryAdded = false;
 	private MediaLibrary library;
 
 	/**Returns the MediaLibrary used by PMS.
 	 * @return (MediaLibrary) Used library, if any. null if none is in use.
 	 */
 	public MediaLibrary getLibrary() {
 		return library;
 	}
 
 	/**Creates a new MediaLibrary object and adds the Media Library folder at root. This method
 	 * can only be run once, or the previous MediaLibrary object can be lost.<P>
 	 * @param renderer {@link RendererConfiguration} Which media renderer to add this folder to.
 	 * @return true if the settings allow to have a MediaLibrary.
 	 * @see PMS#manageRoot(RendererConfiguration)
 	 */
 	public boolean addMediaLibraryFolder(RendererConfiguration renderer) {
 		if (configuration.getUseCache() && !renderer.isMediaLibraryAdded()) {
 			library = new MediaLibrary();
 			if (!configuration.isHideMediaLibraryFolder()) {
 				getRootFolder(renderer).addChild(library);
 			}
 			renderer.setMediaLibraryAdded(true);
 			return true;
 		}
 		return false;
 	}
 
 	/**Executes the needed commands in order to make PMS a Windows service that starts whenever the machine is started.
 	 * This function is called from the Network tab.
 	 * @return true if PMS could be installed as a Windows service.
 	 * @see NetworkTab#build()
 	 */
 	public boolean installWin32Service() {
 		logger.info(Messages.getString("PMS.41")); //$NON-NLS-1$
 		String cmdArray[] = new String[]{"win32/service/wrapper.exe", "-r", "wrapper.conf"}; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
 		OutputParams output = new OutputParams(configuration);
 		output.noexitcheck = true;
 		ProcessWrapperImpl pwuninstall = new ProcessWrapperImpl(cmdArray, output);
 		pwuninstall.run();
 		cmdArray = new String[]{"win32/service/wrapper.exe", "-i", "wrapper.conf"}; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
 		ProcessWrapperImpl pwinstall = new ProcessWrapperImpl(cmdArray, new OutputParams(configuration));
 		pwinstall.run();
 		return pwinstall.isSuccess();
 	}
 
 	/**Splits the first part of a WEB.conf spec into a pair of Strings representing the resource type and its DLNA folder.<P>
 	 * Used by {@link PMS#manageRoot(RendererConfiguration)} in the WEB.conf section.
 	 * @param spec (String) to be split
 	 * @return Array of (String) that represents the tokenized entry.
 	 * @see PMS#manageRoot(RendererConfiguration)
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
 
 	/**Splits the second part of a WEB.conf spec into a triple of Strings representing the DLNA path, resource URI and optional thumbnail URI.<P>
 	 * Used by {@link PMS#manageRoot(RendererConfiguration)} in the WEB.conf section.
 	 * @param spec (String) to be split
 	 * @return Array of (String) that represents the tokenized entry.
 	 * @see PMS#manageRoot(RendererConfiguration)
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
 
 	/**Add a known set of extensions to the extensions list.
 	 * @see PMS#init()
 	 */
 	private void registerExtensions() {
 		extensions.add(new WEB());
 		extensions.add(new MKV());
 		extensions.add(new M4A());
 		extensions.add(new MP3());
 		extensions.add(new ISO());
 		extensions.add(new MPG());
 		extensions.add(new JPG());
 		extensions.add(new OGG());
 		extensions.add(new PNG());
 		extensions.add(new GIF());
 		extensions.add(new TIF());
 		extensions.add(new FLAC());
 		extensions.add(new DVRMS());
 		extensions.add(new RAW());
 	}
 
 	/**Register a known set of audio/video transcoders (known as {@link Player}s). Used in PMS#init().
 	 * @see PMS#init()
 	 */
 	private void registerPlayers() {
 		if (Platform.isWindows()) {
 			registerPlayer(new FFMpegVideo());
 		}
 		registerPlayer(new FFMpegAudio(configuration));
 		registerPlayer(new MEncoderVideo(configuration));
 		if (Platform.isWindows()) {
 			registerPlayer(new MEncoderAviSynth(configuration));
 		}
 		registerPlayer(new MPlayerAudio(configuration));
 		registerPlayer(new MEncoderWebVideo(configuration));
 		registerPlayer(new MPlayerWebVideoDump(configuration));
 		registerPlayer(new MPlayerWebAudio(configuration));
 		registerPlayer(new TSMuxerVideo(configuration));
 		registerPlayer(new TsMuxerAudio(configuration));
 		registerPlayer(new VideoLanAudioStreaming(configuration));
 		registerPlayer(new VideoLanVideoStreaming(configuration));
 		if (Platform.isWindows()) {
 			registerPlayer(new FFMpegDVRMSRemux());
 		}
 		registerPlayer(new RAWThumbnailer());
 		frame.addEngines();
 	}
 
 	/**Adds a single {@link Player} to the list of Players. Used by {@link PMS#registerPlayers()}.
 	 * @param p (Player) to be added to the list
 	 * @see Player
 	 * @see PMS#registerPlayers()
 	 */
 	public void registerPlayer(Player p) {
 		allPlayers.add(p);
 		boolean ok = false;
 		if (Player.NATIVE.equals(p.executable())) {
 			ok = true;
 		} else {
 			if (isWindows()) {
 				if (p.executable() == null) {
 					logger.info("Executable of transcoder profile " + p + " not found"); //$NON-NLS-1$ //$NON-NLS-2$
 					return;
 				}
 				File executable = new File(p.executable());
 				File executable2 = new File(p.executable() + ".exe"); //$NON-NLS-1$
 
 				if (executable.exists() || executable2.exists()) {
 					ok = true;
 				} else {
 					logger.info("Executable of transcoder profile " + p + " not found"); //$NON-NLS-1$ //$NON-NLS-2$
 					return;
 				}
 				if (p.avisynth()) {
 					ok = false;
 					if (registry.isAvis()) {
 						ok = true;
 					} else {
 						logger.info("Transcoder profile " + p + " will not be used because AviSynth was not found"); //$NON-NLS-1$ //$NON-NLS-2$
 					}
 				}
 			} else if (!p.avisynth()) {
 				ok = true;
 			}
 		}
 		if (ok) {
 			logger.info("Registering transcoding engine: " + p /*+ (p.avisynth()?(" with " + (forceMPlayer?"MPlayer":"AviSynth")):"")*/); //$NON-NLS-1$
 			players.add(p);
 		}
 	}
 
 	/**Transforms a comma separated list of directory entries into an array of {@link String}.
 	 * Checks that the directory exists and is a valid directory.
 	 * @param folders {@link String} Comma separated list of directories.
 	 * @param log whether to output log information
 	 * @return {@link File}[] Array of directories.
 	 * @throws IOException
 	 * @see {@link PMS#manageRoot(RendererConfiguration)}
 	 */
 
 	// this is called *way* too often (e.g. a dozen times with 1 renderer and 1 shared folder),
 	// so log it by default so we can fix it.
 	// BUT it's also called when the GUI is initialized (to populate the list of shared folders),
 	// and we don't want this message to appear *before* the PMS banner, so allow that call to suppress logging
 	public File[] loadFoldersConf(String folders, boolean log) throws IOException {
 		if (folders == null || folders.length() == 0) {
 			return null;
 		}
 		ArrayList<File> directories = new ArrayList<File>();
 		String[] foldersArray = folders.split(","); //$NON-NLS-1$
 		for (String folder : foldersArray) {
 			// unescape embedded commas. note: backslashing isn't safe as it conflicts with
 			// Windows path separators:
 			// http://ps3mediaserver.org/forum/viewtopic.php?f=14&t=8883&start=250#p43520
 			folder = folder.replaceAll("&comma;", ","); //$NON-NLS-1$ //$NON-NLS-2$
 			if (log) {
 				logger.trace("Checking shared folder: " + folder); //$NON-NLS-1$
 			}
 			File file = new File(folder);
 			if (file.exists()) {
 				if (!file.isDirectory()) {
 					logger.warn("The file " + folder + " is not a directory! Please remove it from your Shared folders list on the Navigation/Share Settings tab"); //$NON-NLS-1$ //$NON-NLS-2$
 				}
 			} else {
 				logger.warn("The directory " + folder + " does not exist. Please remove it from your Shared folders list on the Navigation/Share Settings tab"); //$NON-NLS-1$ //$NON-NLS-2$
 			}
 
 			// add the file even if there are problems so that the user can update the shared folders as required.
 			directories.add(file);
 		}
 		File f[] = new File[directories.size()];
 		directories.toArray(f);
 		return f;
 	}
 
 	/**Restarts the servers. The trigger is either a button on the main PMS window or via
 	 * an action item added via {@link PMS#addVideoSettingssFolder(RendererConfiguration).
 	 * @throws IOException
 	 */
 	// XXX: don't try to optimize this by reusing the same server instance.
 	// see the comment above HTTPServer.stop()
 	public void reset() throws IOException {
 		logger.trace("Waiting 1 second..."); //$NON-NLS-1$
 		UPNPHelper.sendByeBye();
 		server.stop();
 		server = null;
 		RendererConfiguration.resetAllRenderers();
 		manageRoot(RendererConfiguration.getDefaultConf());
 		try {
 			Thread.sleep(1000);
 		} catch (InterruptedException e) {
 			e.printStackTrace();
 		}
 		server = new HTTPServer(configuration.getServerPort());
 		server.start();
 		frame.setReloadable(false);
 		UPNPHelper.sendAlive();
 	}
 
 	// Cannot remove these methods because of backwards compatibility;
 	// none of the PMS code uses it, but some plugins still do.
 	
 	/**
 	 * @deprecated Use the SLF4J logging API instead.
 	 * Adds a message to the debug stream, or {@link System#out} in case the
 	 * debug stream has not been set up yet.
 	 * @param msg {@link String} to be added to the debug stream.
 	 */
 	public static void debug(String msg) {
 		logger.trace(msg);
 	}
 
 	/**
 	 * @deprecated Use the SLF4J logging API instead.
 	 * Adds a message to the info stream.
 	 * @param msg {@link String} to be added to the info stream.
 	 */
 	public static void info(String msg) {
 		logger.debug(msg);
 	}
 
 	/**
 	 * @deprecated Use the SLF4J logging API instead.
 	 * Adds a message to the minimal stream. This stream is also
 	 * shown in the Trace tab.
 	 * @param msg {@link String} to be added to the minimal stream.
 	 */
 	public static void minimal(String msg) {
 		logger.info(msg);
 	}
 
 	/**
 	 * @deprecated Use the SLF4J logging API instead.
 	 * Adds a message to the error stream. This is usually called by
 	 * statements that are in a try/catch block.
 	 * @param msg {@link String} to be added to the error stream
 	 * @param t {@link Throwable} comes from an {@link Exception} 
 	 */
 	public static void error(String msg, Throwable t) {
 		logger.error(msg, t);
 	}
 	
 	/**Universally Unique Identifier used in the UPnP server.
 	 * 
 	 */
 	private String uuid;
 
 	/**Creates a new {@link #uuid} for the UPnP server to use. Tries to follow the RFCs for creating the UUID based on the link MAC address.
 	 * Defaults to a random one if that method is not available.
 	 * @return {@link String} with an Universally Unique Identifier.
 	 */
 	public String usn() {
 		if (uuid == null) {
 			boolean uuidBasedOnMAC = false;
 			NetworkInterface ni = null;
 			try {
 				if (configuration.getServerHostname() != null && configuration.getServerHostname().length() > 0) {
 					try {
 						ni = NetworkInterface.getByInetAddress(InetAddress.getByName(configuration.getServerHostname()));
 					} catch (Exception e) {
 					}
 				} else if (get().getServer().getNi() != null) {
 					ni = get().getServer().getNi();
 				}
 				if (ni != null) {
 
 					byte[] addr = PMSUtil.getHardwareAddress(ni); // return null when java.net.preferIPv4Stack=true
 					if (addr != null) {
 						uuid = UUID.nameUUIDFromBytes(addr).toString();
 						uuidBasedOnMAC = true;
 					} else {
 						logger.info("Unable to retrieve MAC address for UUID creation: using a random one..."); //$NON-NLS-1$
 					}
 				}
 			} catch (Throwable e) {
 				logger.info("Switching to random UUID cause there's an error in getting UUID from MAC address: " + e.getMessage()); //$NON-NLS-1$
 			}
 
 			if (!uuidBasedOnMAC) {
 				if (ni != null && (ni.getDisplayName() != null || ni.getName() != null)) {
 					uuid = UUID.nameUUIDFromBytes((ni.getDisplayName() != null ? ni.getDisplayName() : (ni.getName() != null ? ni.getName() : "dummy")).getBytes()).toString(); //$NON-NLS-1$
 				} else {
 					uuid = UUID.randomUUID().toString();
 				}
 			}
 			logger.info("Using the following UUID: " + uuid); //$NON-NLS-1$
 		}
 		return "uuid:" + uuid; //$NON-NLS-1$ //$NON-NLS-2$
 		//return "uuid:1234567890TOTO::";
 	}
 
 	/**Returns the user friendly name of the UPnP server. 
 	 * @return {@link String} with the user friendly name.
 	 */
 	public String getServerName() {
 		if (serverName == null) {
 			StringBuilder sb = new StringBuilder();
 			sb.append(System.getProperty("os.name").replace(" ", "_")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
 			sb.append("-"); //$NON-NLS-1$
 			sb.append(System.getProperty("os.arch").replace(" ", "_")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
 			sb.append("-"); //$NON-NLS-1$
 			sb.append(System.getProperty("os.version").replace(" ", "_")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
 			sb.append(", UPnP/1.0, PMS/" + VERSION); //$NON-NLS-1$
 			serverName = sb.toString();
 		}
 		return serverName;
 	}
 
 	/**Returns the PMS instance. New instance is created if needed.
 	 * @return {@link PMS}
 	 */
 	public static PMS get() {
 		if (instance == null) {
 			synchronized (lock) {
 				if (instance == null) {
 					instance = new PMS();
 					try {
 						if (instance.init()) {
 							logger.info("The server should now appear on your renderer"); //$NON-NLS-1$
 						} else {
 							logger.error("A serious error occurred during PMS init"); //$NON-NLS-1$
 						}
 					} catch (Exception e) {
 						e.printStackTrace();
 					}
 				}
 			}
 		}
 		return instance;
 	}
 
 	/**
 	 * @param filename
 	 * @return
 	 */
 	public Format getAssociatedExtension(String filename) {
 		logger.trace("Search extension for " + filename); //$NON-NLS-1$
 		for (Format ext : extensions) {
 			if (ext.match(filename)) {
 				logger.trace("Found 1! " + ext.getClass().getName()); //$NON-NLS-1$
 				return ext.duplicate();
 			}
 		}
 		return null;
 	}
 
 	public Player getPlayer(Class<? extends Player> profileClass, Format ext) {
 		for (Player p : players) {
 			if (p.getClass().equals(profileClass) && p.type() == ext.getType() && !p.excludeFormat(ext)) {
 				return p;
 			}
 		}
 		return null;
 	}
 
 	public ArrayList<Player> getPlayers(ArrayList<Class<? extends Player>> profileClasses, int type) {
 		ArrayList<Player> compatiblePlayers = new ArrayList<Player>();
 		for (Player p : players) {
 			if (profileClasses.contains(p.getClass()) && p.type() == type) {
 				compatiblePlayers.add(p);
 			}
 		}
 		return compatiblePlayers;
 	}
 
 	public static void main(String args[]) throws IOException, ConfigurationException {
 		if (args.length > 0) {
 			for (int a = 0; a < args.length; a++) {
 				if (args[a].equals(CONSOLE)) {
 					System.setProperty(CONSOLE, Boolean.toString(true));
 				} else if (args[a].equals(NATIVELOOK)) {
 					System.setProperty(NATIVELOOK, Boolean.toString(true));
 				} else if (args[a].equals(SCROLLBARS)) {
 					System.setProperty(SCROLLBARS, Boolean.toString(true));
 				} else if (args[a].equals(NOCONSOLE)) {
 					System.setProperty(NOCONSOLE, Boolean.toString(true));
 				}
 			}
 		}
 
 		try {
 			Toolkit.getDefaultToolkit();
 			if (GraphicsEnvironment.isHeadless() && System.getProperty(NOCONSOLE) == null) {
 				System.setProperty(CONSOLE, Boolean.toString(true));
 			}
 		} catch (Throwable t) {
 			System.err.println("Toolkit error: " + t.getMessage());
 			if (System.getProperty(NOCONSOLE) == null) {
 				System.setProperty(CONSOLE, Boolean.toString(true));
 			}
 		}
 
 		try {
 			configuration = new PmsConfiguration();
 		} catch (Throwable t) {
 			System.err.println("Configuration error: " + t.getMessage());
 		}
 
 		assert configuration != null;
 
 		// Load the (optional) logback config file. This has to be called after 'new PmsConfiguration'
 		// as the logging starts immediately and some filters need the PmsConfiguration.
 		LoggingConfigFileLoader.load();
 
 		get();
 
 		try {
 			// let's allow us time to show up serious errors in the GUI before quitting
 			Thread.sleep(60000);
 		} catch (InterruptedException e) {}
 	}
 
 	public HTTPServer getServer() {
 		return server;
 	}
 
 	public ArrayList<Format> getExtensions() {
 		return extensions;
 	}
 
 	public ArrayList<Player> getPlayers() {
 		return players;
 	}
 
 	public void save() {
 		try {
 			configuration.save();
 		} catch (ConfigurationException e) {
 			logger.error("Could not save configuration", e); //$NON-NLS-1$
 		}
 	}
 
 	public static PmsConfiguration getConfiguration() {
 		return configuration;
 	}
 }
