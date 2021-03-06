 /**
  * This file is part of the Harmony package.
  *
  * (c) Mickael Gaillard <mickael.gaillard@tactfactory.com>
  *
  * For the full copyright and license information, please view the LICENSE
  * file that was distributed with this source code.
  */
 package com.tactfactory.mda;
 
 import java.io.BufferedReader;
 import java.io.File;
 import java.io.FileInputStream;
 import java.io.IOException;
 import java.io.InputStreamReader;
 import java.net.URI;
 import java.net.URISyntaxException;
 import java.util.Collection;
 import java.util.HashMap;
 import java.util.List;
 import java.util.Locale;
 import java.util.Map;
 import java.util.regex.Pattern;
 
 import net.xeoh.plugins.base.PluginManager;
 import net.xeoh.plugins.base.impl.PluginManagerFactory;
 import net.xeoh.plugins.base.util.JSPFProperties;
 import net.xeoh.plugins.base.util.PluginManagerUtil;
 
 import org.apache.commons.io.filefilter.FileFilterUtils;
 import org.apache.commons.io.filefilter.IOFileFilter;
 import org.jdom2.Document;
 import org.jdom2.Element;
 import org.jdom2.JDOMException;
 import org.jdom2.input.SAXBuilder;
 
 import com.google.common.base.Strings;
 import com.tactfactory.mda.command.Command;
 import com.tactfactory.mda.command.GeneralCommand;
 import com.tactfactory.mda.meta.ApplicationMetadata;
 import com.tactfactory.mda.template.TagConstant;
 import com.tactfactory.mda.utils.ConsoleUtils;
 import com.tactfactory.mda.utils.TactFileUtils;
 import com.tactfactory.mda.utils.OsUtil;
 
 /** Harmony main class. */
 public final class Harmony {
 	
 	/** Harmony version. */
 	public static final String VERSION = "0.4.0-DEV";
 	
 	/** Singleton of console. */
 	private static Harmony instance;
 		
 	/** Path of Harmony base. */
	private static String pathBase = new File("./").getAbsolutePath();
 	
 	/** Path of harmony.jar. or Binary */
 	private static String harmonyPath =  
 			Harmony.class
 			.getProtectionDomain()
 			.getCodeSource()
 			.getLocation()
 			.toString()
 			.substring("file:".length()); // Ommit "file:"
 	//PATH_BASE + "vendor/tact-core";
 	
 	/** Project space. */
 	private static String projectFolderPath = "android";
 	
 	/** Delimiter. */
 	private static final String DELIMITER = "/";
 	
 	/** Android SDK version. */
 	private static String androidSdkVersion;
 
 	/** Symfony path. */
 	public static final String SYMFONY_PATH = "D:/Site/wamp/www/Symfony";
 	
 	/** Default project name. */
 	private static final String DEFAULT_PROJECT_NAME = "demact";
 	
 	/** Default project NameSpace. */
 	private static final String DEFAULT_PROJECT_NAMESPACE = 
 			"com.tactfactory.mda.test.demact";
 	
 	/** Path of project (app folder in Harmony root). */
 	private static String projectPath = "";	// /app/
 	
 	/** Path of bundles. */
 	private static String bundlesPath;		// /vendor/**/*.jar
 	
 	/** Path of libraries. */
 	private static String libsPath;		// /vendor/**/lib/
 	
 	/** Path of templates. */
 	private static String templatesPath;	// /vendor/**/tpl/
 	
 	/** PluginManager. */
 	private final PluginManager pluginManager =
 			PluginManagerFactory.createPluginManager(new JSPFProperties());
 	
 	/** Bootstrap. */
 	private final Map<Class<?>, Command> bootstrap = 
 			new HashMap<Class<?>, Command>();
 
 	/** Constructor.
 	 * @throws Exception PluginManager failure
 	 */
 	private Harmony() throws Exception {		
 		// Clean binary case (for /bin and /vendor/**/bin)
 		if (harmonyPath.endsWith("bin/")) {
			//bundlesPath = harmonyPath;
 			harmonyPath = new File(harmonyPath).getParentFile().toString();
 		}
 		
 		if (harmonyPath.endsWith("harmony.jar")) {
 			harmonyPath = new File(harmonyPath).getParentFile().toString();
 		}
 		
 		this.detectBasePath();
 		
		projectPath 	= pathBase + "app/";
		libsPath 		= pathBase + "lib/";
 		
 		if (Strings.isNullOrEmpty(bundlesPath)) {
			bundlesPath = pathBase + "vendor/";
 		}
 		
 		templatesPath 	= "tpl/";
 				//TactFileUtils.absoluteToRelativePath(
 				//PATH_BASE + "tpl", PATH_BASE);
 		
 		this.loadPlugins(new File(bundlesPath));
 	
 		Locale.setDefault(Locale.US);
 		Harmony.instance = this;
 	}
 
 	/**
 	 * 
 	 */
 	private void detectBasePath() {
 		// For root case
 		File baseDir = this.detectAppTree(new File(harmonyPath));
 		ConsoleUtils.displayDebug("Detect app on " + harmonyPath);
 		
 		// For vendor/tact-core case
 		if (baseDir == null) {
 			File predictiveBaseDir = 
 					new File(harmonyPath).getParentFile().getParentFile();
 			ConsoleUtils.displayDebug(
 					"Detect app on " + predictiveBaseDir.getAbsolutePath());
 			
 			baseDir = this.detectAppTree(predictiveBaseDir);
 		}
 		
 		if (baseDir != null) {
 			// Transform PATH_BASE !!!
 			pathBase = baseDir.getPath().toString() + "/";
 		} else {
 			// For any other case
 			ConsoleUtils.displayError(new Exception(
 					"INVALID FOLDERS TREE. APP FOLDER MISSING."));
 			System.exit(-1);
 		}
 	}
 
 	/**
 	 * @param pluginBaseDirectory 
 	 */
 	private void loadPlugins(final File pluginBaseDirectory) {
 		//final JSPFProperties props =;
 		/* props.setProperty(PluginManager.class, "cache.enabled", "true");
 		
 		//optional
 		props.setProperty(PluginManager.class, "cache.mode",    "weak"); 
 		props.setProperty(PluginManager.class, "cache.file",    "jspf.cache");*/
 		
 		// Filters
 		final IOFileFilter includeFilter = 
 				FileFilterUtils.suffixFileFilter(".jar");
 		final IOFileFilter excludeFilter = 
 				FileFilterUtils.notFileFilter(
 							FileFilterUtils.nameFileFilter("lib"));
 		
 		// Check list of Bundles .jar
 		Collection<File> plugins = TactFileUtils.listFiles(
 				pluginBaseDirectory,
 				includeFilter, 
 				excludeFilter);
 		
 		ConsoleUtils.displayDebug("Load plugins from classpath");
 		try {
 			this.pluginManager.addPluginsFrom(new URI("classpath://*"));
 		} catch (URISyntaxException e) {
 			ConsoleUtils.displayError(e);
 		}
 		
 		// Add Bundles to Plugin Manager
 		for (File plugin : plugins) {
 			if (!plugin.getName().equals("harmony.jar")) {
 				ConsoleUtils.displayDebug(
 						"Load plugins from " + plugin.getName());
 				this.pluginManager.addPluginsFrom(plugin.toURI());
 			}
 		}
 		
 		// Process extensions commands
 		final PluginManagerUtil pmu = new PluginManagerUtil(this.pluginManager);
 		final Collection<Command> commands = pmu.getPlugins(Command.class);
 		
 		// Bootstrap all commands
 		for (final Command command : commands) {
 			this.bootstrap.put(command.getClass(), command);
 		}
 	}
 	
 	/**
 	 * Check if the given folder contains the App folder.
 	 * @param checkPath The path to check.
 	 * @return The path if App was found, null if not.
 	 */
 	private File detectAppTree(final File checkPath) {
 		File result = null;
 		File[] list = checkPath.listFiles();
 		
 		if (list != null) {
 			for (File dir : list) {
 				if (dir.getPath().endsWith("app")) {
 					result = checkPath;
 					break;
 				}
 			}
 		}
 		
 		return result;
 	}
 	
 	
 	/**
 	 * Get Harmony instance (Singleton).
 	 * @return The harmony instance
 	 */
 	public static Harmony getInstance() {
 		if (Harmony.instance == null) {
 			try {
 				new Harmony();
 			} catch (Exception e) {
 				ConsoleUtils.displayError(e);
 			}
 		}
 		return Harmony.instance;
 	}
 
 	/** Initialize Harmony. 
 	 * @throws Exception 
 	 */
 	protected void initialize() throws Exception {
 		ConsoleUtils.display(
 				"Current Working Path: " + new File(".").getCanonicalPath());
 
 		// Check name space
 		if (Strings.isNullOrEmpty(
 				ApplicationMetadata.INSTANCE.getProjectNameSpace())) {
 			
 			// get project namespace and project name from AndroidManifest.xml
 			final File manifest = new File(String.format("%s/%s/%s",
 					projectPath,
 					Harmony.projectFolderPath,
 					"AndroidManifest.xml"));
 			
 			final File config = new File(String.format("%s/%s/%s",
 					projectPath,
 					Harmony.projectFolderPath, 
 					"/res/values/configs.xml")); //FIXME path by adapter
 			
 			if (manifest.exists()) {
 				ApplicationMetadata.INSTANCE.setProjectNameSpace(
 						Harmony.getNameSpaceFromManifest(manifest));
 
 				ApplicationMetadata.INSTANCE.setName(
 						Harmony.getProjectNameFromConfig(config));
 			}
 			
 			final String projectProp = String.format("%s/%s/%s",
 					projectPath,
 					Harmony.projectFolderPath, 
 					"local.properties");
 			
 			ApplicationMetadata.setAndroidSdkPath(
 					Harmony.getSdkDirFromPropertiesFile(projectProp));
 			Harmony.androidSdkVersion =
 					getAndroidSdkVersion(
 							ApplicationMetadata.getAndroidSdkPath());
 		} else {
 			final String[] projectNameSpaceData =
 					ApplicationMetadata.INSTANCE.getProjectNameSpace()
 							.split(DELIMITER);
 			ApplicationMetadata.INSTANCE.setName(
 					projectNameSpaceData[projectNameSpaceData.length - 1]);
 		}
 		
 		// Debug Log
 		ConsoleUtils.display(
 				"Current Project : " + ApplicationMetadata.INSTANCE.getName() 
 					+ "\n" 
 				+ "Current NameSpace : " 
 						+ ApplicationMetadata.INSTANCE.getProjectNameSpace()
 						+ "\n" 
 				+ "Current Android SDK Path : "
 						+ ApplicationMetadata.getAndroidSdkPath() + "\n" 
 				+ "Current Android SDK Revision : "
 						+ Harmony.androidSdkVersion);
 	}
 	
 	/**
 	 * Select the proper command class.
 	 * 
 	 * @param commandName Class command name
 	 * @return BaseCommand object
 	 */
 	public Command getCommand(final Class<?> commandName) {
 		return this.bootstrap.get(commandName);
 	}
 	
 	/**
 	 * Get all command class.
 	 * 
 	 * @return The command class
 	 */
 	public Collection<Command> getCommands() {
 		return this.bootstrap.values();
 	}
 
 	/**
 	 * Check and launch command in proper command class.
 	 * 
 	 * @param action Command to execute
 	 * @param args Commands arguments
 	 * @param option Console option (ANSI, Debug, ...)
 	 */
 	public void findAndExecute(final String action,
 			final String[] args,
 			final String option) {
 		boolean isfindAction = false;
 		
 		// Select Action and launch
 		for (final Command baseCommand : this.bootstrap.values()) {
 			if (baseCommand.isAvailableCommand(action)) {
 				baseCommand.execute(action, args, option);
 				isfindAction = true;
 			}
 		}
 
 		// No found action
 		if (!isfindAction) {
 			ConsoleUtils.display("Command not found...");
 			
 			this.getCommand(GeneralCommand.class).execute(
 					GeneralCommand.LIST,
 					null, 
 					null);
 		}
 		
 		this.pluginManager.shutdown();
 	}
 
 	/**
 	 * Generic user console prompt.
 	 * 
 	 * @param promptMessage message to display
 	 * @return input user input
 	 */
 	public static String getUserInput(final String promptMessage) {
 		String input = null;
 		try {
 			ConsoleUtils.display(promptMessage);
 			final BufferedReader br = 
 					new BufferedReader(
 							new InputStreamReader(
 									System.in, 
 									TactFileUtils.DEFAULT_ENCODING));
 
 		
 			input = br.readLine();
 		} catch (final IOException e) {
 			ConsoleUtils.displayError(e);
 		}
 		
 		return input;
 	}
 
 	/**
 	 * Prompt Project Name to the user.
 	 */
 	public static void initProjectName() {
 		if (Strings.isNullOrEmpty(ApplicationMetadata.INSTANCE.getName())) {
 			final String projectName = 
 					Harmony.getUserInput("Please enter your Project Name ["
 						+ DEFAULT_PROJECT_NAME 
 						+ "]:");
 			
 			if (Strings.isNullOrEmpty(projectName)) {
 				ApplicationMetadata.INSTANCE.setName(DEFAULT_PROJECT_NAME);
 			} else {
 				ApplicationMetadata.INSTANCE.setName(projectName);
 			}
 		}
 	}
 	
 	/**
 	 * Prompt Project Name Space to the user.
 	 */
 	public static void initProjectNameSpace() {
 		if (Strings.isNullOrEmpty(
 				ApplicationMetadata.INSTANCE.getProjectNameSpace())) {
 			boolean good = false;
 			
 			while (!good) {
 				final String projectNameSpace 
 						= Harmony.getUserInput(
 								"Please enter your Project NameSpace [" 
 										+ DEFAULT_PROJECT_NAMESPACE
 										+ "]:");
 				
 				if (Strings.isNullOrEmpty(projectNameSpace)) {
 					ApplicationMetadata.INSTANCE.setProjectNameSpace(
 						DEFAULT_PROJECT_NAMESPACE
 								.replaceAll("\\.", DELIMITER));
 					good = true;
 					
 				} else {
 					if (projectNameSpace.toLowerCase(Locale.ENGLISH)
 							.endsWith(ApplicationMetadata.INSTANCE.getName()
 									.toLowerCase())) {
 						
 						String namespaceForm = 
 								"^(((([a-z0-9_]+)\\.)*)([a-z0-9_]+))$";
 						
 						if (Pattern.matches(namespaceForm, projectNameSpace)) {
 							ApplicationMetadata.INSTANCE.setProjectNameSpace(
 								projectNameSpace.replaceAll("\\.", DELIMITER));
 							good = true;
 						} else {
 							ConsoleUtils.display(
 									"You can't use special characters "
 									+ "except '.' in the NameSpace.");
 						}
 					} else {
 						ConsoleUtils.display(
 								"The NameSpace has to end with Project Name !");
 					}
 				}
 			}
 		}
 	}
 
 	/**
 	 * Prompt Project Android SDK Path to the user.
 	 */
 	public static void initProjectAndroidSdkPath() {
 		if (Strings.isNullOrEmpty(ApplicationMetadata.getAndroidSdkPath())) {
 			final String sdkPath = 
 					Harmony.getUserInput("Please enter AndroidSDK " 
 							+ "full path [/root/android-sdk/]:");
 			
 			if (!Strings.isNullOrEmpty(sdkPath)) {
 				ApplicationMetadata.setAndroidSdkPath(sdkPath);
 				Harmony.androidSdkVersion 
 					= getAndroidSdkVersion(
 							ApplicationMetadata.getAndroidSdkPath());
 			} else {
 				String osMessage = "Detected OS: ";
 				
 				if (OsUtil.isWindows()) {
 					if (OsUtil.isX64()) {
 						osMessage += "Windows x64";
 						ApplicationMetadata.setAndroidSdkPath(String.format(
 								"%s/%s/",
 								"C:/Program Files", 
 								"android-sdk"));
 						
 					} else if (!OsUtil.isX64()) {
 						osMessage += "Windows x86";
 						ApplicationMetadata.setAndroidSdkPath(
 								String.format("%s/%s/", 
 										"C:/Program Files (x86)", 
 										"android-sdk"));
 					} else {
 						osMessage += "Windows x??";
 						ApplicationMetadata.setAndroidSdkPath(
 								String.format("%s/%s/", 
 										"C:/Program Files", 
 										"android-sdk"));
 					}
 				} else if (OsUtil.isLinux()) {
 					osMessage += "Linux";
 					ApplicationMetadata.setAndroidSdkPath("/opt/android-sdk/");
 				}
 				
 				// Debug Log
 				ConsoleUtils.displayDebug(osMessage);
 			}
 		}
 	}
 
 	/**
 	 * Check initialization of project 
 	 * by searching nameSpace in androidmanifest.xml.
 	 *
 	 * @return true if success
 	 */
 	public static boolean isProjectInit() {
 		boolean result = false;
 		final File projectFolder = new File(Harmony.projectFolderPath);
 		
 		if (projectFolder.exists() && projectFolder.listFiles().length != 0) {
 			final File manifest = 
 					new File(Harmony.projectFolderPath + "AndroidManifest.xml");
 			final String namespace = Harmony.getNameSpaceFromManifest(manifest);
 			
 			if (namespace != null && !namespace.equals("${namespace}")) {
 				result = true;
 			}
 		}
 		
 		return result;
 	}
 	
 	/**
 	 * Extract Project NameSpace from AndroidManifest file.
 	 * 
 	 * @param manifest Manifest File
 	 * @return Project Name Space
 	 */
 	public static String getNameSpaceFromManifest(final File manifest) {
 		String projnamespace = null;
 		SAXBuilder builder;
 		Document doc;
 		
 		if (manifest.exists()) {
 			// Make engine
 			builder = new SAXBuilder();
 			try {
 				// Load XML File
 				doc = builder.build(manifest);
 				
 				// Load Root element
 				final Element rootNode = doc.getRootElement();
 
 				// Get Name Space from package declaration
 				projnamespace = rootNode.getAttributeValue("package"); 
 				projnamespace = projnamespace.replaceAll("\\.", DELIMITER);
 			} catch (final JDOMException e) {
 				// TODO Auto-generated catch block
 				ConsoleUtils.displayError(e);
 			} catch (final IOException e) {
 				// TODO Auto-generated catch block
 				ConsoleUtils.displayError(e);
 			}
 		}
 		
 		return projnamespace;
 	}
 	
 	/**
 	 * Extract Project Name from config file.
 	 * 
 	 * @param config Config file
 	 * @return Project Name Space
 	 */
 	public static String getProjectNameFromConfig(final File config) {
 		String projname = null;
 		SAXBuilder builder;
 		Document doc;
 		
 		if (config.exists()) {
 			// Make engine
 			builder = new SAXBuilder();	
 			try {
 				// Load XML File
 				doc = builder.build(config);			
 				// Load Root element
 				final Element rootNode = doc.getRootElement(); 			
 				// Load Name space (required for manipulate attributes)
 				//Namespace ns = rootNode.getNamespace("android");	
 
 				for (final Element element : rootNode.getChildren("string")) {
 					if (element.getAttribute("name").getValue()
 							.equals("app_name")) {
 						projname = element.getValue();
 						break;
 					}
 					
 				}
 			} catch (final JDOMException e) {
 				// TODO Auto-generated catch block
 				ConsoleUtils.displayError(e);
 			} catch (final IOException e) {
 				// TODO Auto-generated catch block
 				ConsoleUtils.displayError(e);
 			}
 		}
 		
 		return projname;
 	}
 	
 	/**
 	 * Extract Android SDK Path from .properties file.
 	 * 
 	 * @param fileProp .properties File
 	 * @return Android SDK Path
 	 */
 	public static String getSdkDirFromPropertiesFile(final File fileProp) {
 		String result = null;
 		
 		if (fileProp.exists()) {
 			final List<String> lines = 
 					TactFileUtils.fileToStringArray(fileProp);
 			
 			for (int i = 0; i < lines.size(); i++) {
 				if (lines.get(i).startsWith("sdk.dir=")) {
 					if (lines.get(i).contains(TagConstant.ANDROID_SDK_DIR)) {
 						ConsoleUtils.displayWarning(
 								"Android SDK Dir not defined," 
 								+ " please init project...");
 					} else {
 						result = lines.get(i).replace("sdk.dir=", "");
 					}
 					break;
 				}
 			}
 		}
 		
 		return result;
 	}
 	
 	/**
 	 * Extract Android SDK Path from .properties file.
 	 * 
 	 * @param filePath .properties File path
 	 * @return Android SDK Path
 	 */
 	public static String getSdkDirFromPropertiesFile(final String filePath) {
 		String result = null;
 		
 		final File fileProp = new File(filePath);
 		result = getSdkDirFromPropertiesFile(fileProp);
 		
 		return result;
 	}
 	
 	/**
 	 * Extract Android SDK Path from local.properties file.
 	 * 
 	 * @param sdkPath The sdk path
 	 * @return Android SDK Path
 	 */
 	public static String getAndroidSdkVersion(final String sdkPath) {
 		String result = null;
 		
 		final File sdkProperties = 
 				new File(sdkPath + "/tools/source.properties");
 		if (sdkProperties.exists()) {
 			try {
 				final FileInputStream fis = new FileInputStream(sdkProperties);
 				final InputStreamReader isr =
 						new InputStreamReader(fis, 
 								TactFileUtils.DEFAULT_ENCODING);
 				final BufferedReader br = new BufferedReader(isr);
 				String line = br.readLine();
 				while (line != null) {
 					if (line.startsWith("Pkg.Revision")) {
 						result = line.substring(line.lastIndexOf('=') + 1);
 					}
 					line = br.readLine();
 				}
 				br.close();
 			} catch (final IOException e) {
 				ConsoleUtils.displayError(e);
 			}
 		}
 		return result;
 	}
 	
 	/**
 	 * Get the android SDK version.
 	 * @return the android SDK version
 	 */
 	public static String getAndroidSDKVersion() {
 		return androidSdkVersion;
 	}
 	
 	/**
 	 * Get the android project folder.
 	 * @return the android project folder
 	 */
 	public static String getProjectFolder() {
 		return projectFolderPath;
 	}
 	
 	/**
 	 * Get the android project path.
 	 * @return the android project path
 	 */
 	public static String getProjectPath() {
 		return projectPath;
 	}
 	
 	/**
 	 * Get the harmony base path.
 	 * @return the harmony base path
 	 */
 	public static String getPathBase() {
 		return pathBase;
 	}
 	
 	/**
 	 * Get the harmony path.
 	 * @return the harmony path
 	 */
 	public static String getHarmonyPath() {
 		return harmonyPath;
 	}
 	
 	
 	/**
 	 * Get the libraries path.
 	 * @return the libraries path
 	 */
 	public static String getLibsPath() {
 		return libsPath;
 	}
 	
 	
 	/**
 	 * Get the templates path.
 	 * @return the template path
 	 */
 	public static String getTemplatesPath() {
 		return templatesPath;
 	}
 	
 	
 }
