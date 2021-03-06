 /*******************************************************************************
  * Copyright (c) 2000, 2007 IBM Corporation and others.
  * All rights reserved. This program and the accompanying materials
  * are made available under the terms of the Eclipse Public License v1.0
  * which accompanies this distribution, and is available at
  * http://www.eclipse.org/legal/epl-v10.html
  *
  * Contributors:
  *     IBM Corporation - initial API and implementation
  *     Markus Schorn (Wind River Systems)
  *     Andrew Ferguson (Symbian)
  *******************************************************************************/
 
 package org.eclipse.cdt.core;
 
 import java.io.IOException;
 import java.text.MessageFormat;
 import java.util.ArrayList;
 import java.util.Collection;
 import java.util.HashMap;
 import java.util.HashSet;
 import java.util.Iterator;
 import java.util.List;
 import java.util.Map;
 import java.util.MissingResourceException;
 import java.util.ResourceBundle;
 
 import org.eclipse.cdt.core.cdtvariables.ICdtVariableManager;
 import org.eclipse.cdt.core.dom.CDOM;
 import org.eclipse.cdt.core.dom.IPDOMManager;
 import org.eclipse.cdt.core.envvar.IEnvironmentVariableManager;
 import org.eclipse.cdt.core.index.IIndexManager;
 import org.eclipse.cdt.core.model.CoreModel;
 import org.eclipse.cdt.core.model.IWorkingCopy;
 import org.eclipse.cdt.core.parser.IScannerInfoProvider;
 import org.eclipse.cdt.core.resources.IConsole;
 import org.eclipse.cdt.core.resources.IPathEntryVariableManager;
 import org.eclipse.cdt.core.settings.model.ICConfigurationDescription;
 import org.eclipse.cdt.core.settings.model.ICProjectDescription;
 import org.eclipse.cdt.core.settings.model.WriteAccessException;
 import org.eclipse.cdt.core.settings.model.util.CDataUtil;
 import org.eclipse.cdt.internal.core.CConfigBasedDescriptorManager;
 import org.eclipse.cdt.internal.core.CContentTypes;
 import org.eclipse.cdt.internal.core.CDTLogWriter;
 import org.eclipse.cdt.internal.core.CdtVarPathEntryVariableManager;
 import org.eclipse.cdt.internal.core.PositionTrackerManager;
 import org.eclipse.cdt.internal.core.cdtvariables.CdtVariableManager;
 import org.eclipse.cdt.internal.core.envvar.EnvironmentVariableManager;
 import org.eclipse.cdt.internal.core.model.BufferManager;
 import org.eclipse.cdt.internal.core.model.CModelManager;
 import org.eclipse.cdt.internal.core.model.DeltaProcessor;
 import org.eclipse.cdt.internal.core.model.IBufferFactory;
 import org.eclipse.cdt.internal.core.model.Util;
 import org.eclipse.cdt.internal.core.pdom.PDOMManager;
 import org.eclipse.cdt.internal.core.pdom.indexer.fast.PDOMFastIndexer;
 import org.eclipse.cdt.internal.core.settings.model.CProjectDescriptionManager;
 import org.eclipse.core.resources.IProject;
 import org.eclipse.core.resources.IProjectDescription;
 import org.eclipse.core.resources.IResource;
 import org.eclipse.core.resources.IWorkspace;
 import org.eclipse.core.resources.IWorkspaceRunnable;
 import org.eclipse.core.resources.ResourcesPlugin;
 import org.eclipse.core.runtime.CoreException;
 import org.eclipse.core.runtime.IConfigurationElement;
 import org.eclipse.core.runtime.IExtension;
 import org.eclipse.core.runtime.IExtensionPoint;
 import org.eclipse.core.runtime.IProgressMonitor;
 import org.eclipse.core.runtime.IStatus;
 import org.eclipse.core.runtime.NullProgressMonitor;
 import org.eclipse.core.runtime.OperationCanceledException;
 import org.eclipse.core.runtime.Platform;
 import org.eclipse.core.runtime.Plugin;
 import org.eclipse.core.runtime.Preferences;
 import org.eclipse.core.runtime.Status;
 import org.eclipse.core.runtime.SubProgressMonitor;
 import org.eclipse.core.runtime.content.IContentType;
 import org.osgi.framework.BundleContext;
 
 public class CCorePlugin extends Plugin {
 
 	public static final int STATUS_CDTPROJECT_EXISTS = 1;
 	public static final int STATUS_CDTPROJECT_MISMATCH = 2;
 	public static final int CDT_PROJECT_NATURE_ID_MISMATCH = 3;
 
 	public static final String PLUGIN_ID = "org.eclipse.cdt.core"; //$NON-NLS-1$
 
 	public static final String BUILDER_MODEL_ID = PLUGIN_ID + ".CBuildModel"; //$NON-NLS-1$
 	public static final String BINARY_PARSER_SIMPLE_ID = "BinaryParser"; //$NON-NLS-1$
 	public final static String BINARY_PARSER_UNIQ_ID = PLUGIN_ID + "." + BINARY_PARSER_SIMPLE_ID; //$NON-NLS-1$
 	public final static String PREF_BINARY_PARSER = "binaryparser"; //$NON-NLS-1$
 	public final static String DEFAULT_BINARY_PARSER_SIMPLE_ID = "ELF"; //$NON-NLS-1$
 	public final static String DEFAULT_BINARY_PARSER_UNIQ_ID = PLUGIN_ID + "." + DEFAULT_BINARY_PARSER_SIMPLE_ID; //$NON-NLS-1$
 	public final static String PREF_USE_STRUCTURAL_PARSE_MODE = "useStructualParseMode"; //$NON-NLS-1$
 	public final static String PREF_USE_NEW_MODEL_BUILDER = "useNewModelBuilder"; //$NON-NLS-1$
 	
 	public static final String INDEX_SIMPLE_ID = "CIndex"; //$NON-NLS-1$
 	public static final String INDEX_UNIQ_ID = PLUGIN_ID + "." + INDEX_SIMPLE_ID; //$NON-NLS-1$
 	 		
 	public static final String INDEXER_SIMPLE_ID = "CIndexer"; //$NON-NLS-1$
 	public static final String INDEXER_UNIQ_ID = PLUGIN_ID + "." + INDEXER_SIMPLE_ID; //$NON-NLS-1$
 	public static final String PREF_INDEXER = "indexer"; //$NON-NLS-1$
 	public static final String DEFAULT_INDEXER = PDOMFastIndexer.ID;
 	
 	public final static String ERROR_PARSER_SIMPLE_ID = "ErrorParser"; //$NON-NLS-1$
 	public final static String ERROR_PARSER_UNIQ_ID = PLUGIN_ID + "." + ERROR_PARSER_SIMPLE_ID; //$NON-NLS-1$
 
 	// default store for pathentry
 	public final static String DEFAULT_PATHENTRY_STORE_ID = PLUGIN_ID + ".cdtPathEntryStore"; //$NON-NLS-1$
 
 	// Build Model Interface Discovery
 	public final static String BUILD_SCANNER_INFO_SIMPLE_ID = "ScannerInfoProvider"; //$NON-NLS-1$
 	public final static String BUILD_SCANNER_INFO_UNIQ_ID = PLUGIN_ID + "." + BUILD_SCANNER_INFO_SIMPLE_ID; //$NON-NLS-1$
 
 	public static final String DEFAULT_PROVIDER_ID = CCorePlugin.PLUGIN_ID + ".defaultConfigDataProvider"; //$NON-NLS-1$
 
 	/**
 	 * Name of the extension point for contributing a source code formatter
 	 */
 	public static final String FORMATTER_EXTPOINT_ID = "CodeFormatter" ; //$NON-NLS-1$
 
 	/**
 	 * Possible configurable option value for TRANSLATION_TASK_PRIORITIES.
 	 * @see #getDefaultOptions
 	 */
 	public static final String TRANSLATION_TASK_PRIORITY_NORMAL = "NORMAL"; //$NON-NLS-1$	    
     /**
      * Possible configurable option value for TRANSLATION_TASK_PRIORITIES.
      * @see #getDefaultOptions
      */
     public static final String TRANSLATION_TASK_PRIORITY_HIGH = "HIGH"; //$NON-NLS-1$
     /**
      * Possible configurable option value for TRANSLATION_TASK_PRIORITIES.
      * @see #getDefaultOptions
      */
     public static final String TRANSLATION_TASK_PRIORITY_LOW = "LOW"; //$NON-NLS-1$
     /**
      * Possible  configurable option ID.
      * @see #getDefaultOptions
      */
     public static final String CORE_ENCODING = PLUGIN_ID + ".encoding"; //$NON-NLS-1$
 	
 	/**
 	 * IContentType id for C Source Unit
 	 */
 	public final static String CONTENT_TYPE_CSOURCE =  "org.eclipse.cdt.core.cSource"; //$NON-NLS-1$
 	/**
 	 * IContentType id for C Header Unit
 	 */
 	public final static String CONTENT_TYPE_CHEADER =  "org.eclipse.cdt.core.cHeader"; //$NON-NLS-1$
 	/**
 	 * IContentType id for C++ Source Unit
 	 */
 	public final static String CONTENT_TYPE_CXXSOURCE = "org.eclipse.cdt.core.cxxSource"; //$NON-NLS-1$
 	/**
 	 * IContentType id for C++ Header Unit
 	 */
 	public final static String CONTENT_TYPE_CXXHEADER = "org.eclipse.cdt.core.cxxHeader"; //$NON-NLS-1$
 	/**
 	 * IContentType id for ASM Unit
 	 */
 	public final static String CONTENT_TYPE_ASMSOURCE = "org.eclipse.cdt.core.asmSource"; //$NON-NLS-1$
 
 	/**
 	 * Possible  configurable option value.
 	 * @see #getDefaultOptions()
 	 */
 	public static final String INSERT = "insert"; //$NON-NLS-1$
 	/**
 	 * Possible  configurable option value.
 	 * @see #getDefaultOptions()
 	 */
 	public static final String DO_NOT_INSERT = "do not insert"; //$NON-NLS-1$
 	/**
 	 * Possible  configurable option value.
 	 * @see #getDefaultOptions()
 	 */
 	public static final String TAB = "tab"; //$NON-NLS-1$
 	/**
 	 * Possible  configurable option value.
 	 * @see #getDefaultOptions()
 	 */
 	public static final String SPACE = "space"; //$NON-NLS-1$
 
     public CDTLogWriter cdtLog = null;
 
 	private static CCorePlugin fgCPlugin;
 	private static ResourceBundle fgResourceBundle;
 
 	private CConfigBasedDescriptorManager/*CDescriptorManager*/ fDescriptorManager;// = new CDescriptorManager();
 
 	private CProjectDescriptionManager fNewCProjectDescriptionManager = CProjectDescriptionManager.getInstance();
 
 	private CoreModel fCoreModel;
 	
 	private PDOMManager pdomManager;
 
 	private CdtVarPathEntryVariableManager fPathEntryVariableManager;
 
 	// -------- static methods --------
 
 	static {
 		try {
 			fgResourceBundle = ResourceBundle.getBundle("org.eclipse.cdt.internal.core.CCorePluginResources"); //$NON-NLS-1$
 		} catch (MissingResourceException x) {
 			fgResourceBundle = null;
 		}
 	}
 
 	/**
 	 * Answers the shared working copies currently registered for this buffer factory. 
 	 * Working copies can be shared by several clients using the same buffer factory,see 
 	 * <code>IWorkingCopy.getSharedWorkingCopy</code>.
 	 * 
 	 * @param factory the given buffer factory
 	 * @return the list of shared working copies for a given buffer factory
 	 * @see IWorkingCopy
 	 */
 	public static IWorkingCopy[] getSharedWorkingCopies(IBufferFactory factory){
 		
 		// if factory is null, default factory must be used
 		if (factory == null) factory = BufferManager.getDefaultBufferManager().getDefaultBufferFactory();
 		Map sharedWorkingCopies = CModelManager.getDefault().sharedWorkingCopies;
 		
 		Map perFactoryWorkingCopies = (Map) sharedWorkingCopies.get(factory);
 		if (perFactoryWorkingCopies == null) return CModelManager.NoWorkingCopy;
 		Collection copies = perFactoryWorkingCopies.values();
 		IWorkingCopy[] result = new IWorkingCopy[copies.size()];
 		copies.toArray(result);
 		return result;
 	}
 	
 	public static String getResourceString(String key) {
 		try {
 			return fgResourceBundle.getString(key);
 		} catch (MissingResourceException e) {
 			return "!" + key + "!"; //$NON-NLS-1$ //$NON-NLS-2$
 		} catch (NullPointerException e) {
 			return "#" + key + "#"; //$NON-NLS-1$ //$NON-NLS-2$
 		}
 	}
 
 	public static IWorkspace getWorkspace() {
 		return ResourcesPlugin.getWorkspace();
 	}
 
 	public static String getFormattedString(String key, String arg) {
 		return MessageFormat.format(getResourceString(key), new String[] { arg });
 	}
 
 	public static String getFormattedString(String key, String[] args) {
 		return MessageFormat.format(getResourceString(key), args);
 	}
 
 	public static ResourceBundle getResourceBundle() {
 		return fgResourceBundle;
 	}
 
     public static IPositionTrackerManager getPositionTrackerManager() {
         return PositionTrackerManager.getInstance();
     }
     
 	public static CCorePlugin getDefault() {
 		return fgCPlugin;
 	}
 
 	public static void log(String e) {
 		log(createStatus(e));
 	}
 	
 	public static void log(Throwable e) {
 		if ( e instanceof CoreException ) {
 			log(((CoreException)e).getStatus());
 		} else {
 			log(new Status(IStatus.ERROR, PLUGIN_ID, IStatus.ERROR, "Error", e)); //$NON-NLS-1$
 		}
 	}
 
 	public static IStatus createStatus(String msg) {
 		return createStatus(msg, null);
 	}
 
 	public static IStatus createStatus(String msg, Throwable e) {
 		return new Status(IStatus.ERROR, PLUGIN_ID, IStatus.ERROR, msg, e);
 	}
 	
 	public static void log(IStatus status) {
 		((Plugin) getDefault()).getLog().log(status);
 	}
 
 	// ------ CPlugin
 
 	public CCorePlugin() {
 		super();
 		fgCPlugin = this;
 	}
 
 	/**
 	 * @see Plugin#shutdown
 	 */
 	public void stop(BundleContext context) throws Exception {
 		try {
             PositionTrackerManager.getInstance().uninstall();
             
 //			if (fDescriptorManager != null) {
 //				fDescriptorManager.shutdown();
 //			}
             
 			if (fCoreModel != null) {
 				fCoreModel.shutdown();
 			}
 			
 			if (cdtLog != null) {
 				cdtLog.shutdown();
 			}
 
 			if (fPathEntryVariableManager != null) {
 				fPathEntryVariableManager.shutdown();
 			}
 
             fNewCProjectDescriptionManager.shutdown();
             fDescriptorManager = null;
 
 			savePluginPreferences();
 		} finally {
 			super.stop(context);
 		}
 	}
 
 	/**
 	 * @see Plugin#startup
 	 */
 	public void start(BundleContext context) throws Exception {
 		super.start(context);
 
 		fNewCProjectDescriptionManager.startup();
 		fDescriptorManager = fNewCProjectDescriptionManager.getDescriptorManager();
 
 		// Start file type manager first !!
 		fPathEntryVariableManager = new CdtVarPathEntryVariableManager();
 		fPathEntryVariableManager.startup();
 
 		cdtLog = new CDTLogWriter(CCorePlugin.getDefault().getStateLocation().append(".log").toFile()); //$NON-NLS-1$
 		
 		//Set debug tracing options
 		configurePluginDebugOptions();
 		
 //		fDescriptorManager.startup();
 //		CProjectDescriptionManager.getInstance().startup();
 
 		// Fired up the model.
 		fCoreModel = CoreModel.getDefault();
 		fCoreModel.startup();
 
 		// Fire up the PDOM
 		pdomManager = new PDOMManager();
 		pdomManager.startup();
 
 		// Set the default for using the structual parse mode to build the CModel
 		getPluginPreferences().setDefault(PREF_USE_STRUCTURAL_PARSE_MODE, false);
 		// Set the default for using the new model builder to build the CModel
 		getPluginPreferences().setDefault(PREF_USE_NEW_MODEL_BUILDER, true);
 
         PositionTrackerManager.getInstance().install();
 	}
     
     
     /**
      * TODO: Add all options here
      * Returns a table of all known configurable options with their default values.
      * These options allow to configure the behaviour of the underlying components.
      * The client may safely use the result as a template that they can modify and
      * then pass to <code>setOptions</code>.
      * 
      * Helper constants have been defined on CCorePlugin for each of the option ID and 
      * their possible constant values.
      * 
      * Note: more options might be added in further releases.
      * <pre>
      * RECOGNIZED OPTIONS:
      * TRANSLATION / Define the Automatic Task Tags
      *    When the tag list is not empty, translation will issue a task marker whenever it encounters
      *    one of the corresponding tags inside any comment in C/C++ source code.
      *    Generated task messages will include the tag, and range until the next line separator or comment ending.
      *    Note that tasks messages are trimmed. If a tag is starting with a letter or digit, then it cannot be leaded by
      *    another letter or digit to be recognized ("fooToDo" will not be recognized as a task for tag "ToDo", but "foo#ToDo"
      *    will be detected for either tag "ToDo" or "#ToDo"). Respectively, a tag ending with a letter or digit cannot be followed
      *    by a letter or digit to be recognized ("ToDofoo" will not be recognized as a task for tag "ToDo", but "ToDo:foo" will
      *    be detected either for tag "ToDo" or "ToDo:").
      *     - option id:         "org.eclipse.cdt.core.translation.taskTags"
      *     - possible values:   { "<tag>[,<tag>]*" } where <tag> is a String without any wild-card or leading/trailing spaces 
      *     - default:           ""
      * 
      * TRANSLATION / Define the Automatic Task Priorities
      *    In parallel with the Automatic Task Tags, this list defines the priorities (high, normal or low)
      *    of the task markers issued by the translation.
      *    If the default is specified, the priority of each task marker is "NORMAL".
      *     - option id:         "org.eclipse.cdt.core.transltaion.taskPriorities"
      *     - possible values:   { "<priority>[,<priority>]*" } where <priority> is one of "HIGH", "NORMAL" or "LOW"
      *     - default:           ""
      * 
      * CORE / Specify Default Source Encoding Format
      *    Get the encoding format for translated sources. This setting is read-only, it is equivalent
      *    to 'ResourcesPlugin.getEncoding()'.
      *     - option id:         "org.eclipse.cdt.core.encoding"
      *     - possible values:   { any of the supported encoding names}.
      *     - default:           <platform default>
      * </pre>
      * 
      * @return a mutable map containing the default settings of all known options
      *   (key type: <code>String</code>; value type: <code>String</code>)
      * @see #setOptions
      */
     
     public static HashMap getDefaultOptions()
     {
         HashMap defaultOptions = new HashMap(10);
 
         // see #initializeDefaultPluginPreferences() for changing default settings
         Preferences preferences = getDefault().getPluginPreferences();
         HashSet optionNames = CModelManager.OptionNames;
         
         // get preferences set to their default
         String[] defaultPropertyNames = preferences.defaultPropertyNames();
         for (int i = 0; i < defaultPropertyNames.length; i++){
             String propertyName = defaultPropertyNames[i];
             if (optionNames.contains(propertyName)) {
                 defaultOptions.put(propertyName, preferences.getDefaultString(propertyName));
             }
         }       
         // get preferences not set to their default
         String[] propertyNames = preferences.propertyNames();
         for (int i = 0; i < propertyNames.length; i++){
             String propertyName = propertyNames[i];
             if (optionNames.contains(propertyName)) {
                 defaultOptions.put(propertyName, preferences.getDefaultString(propertyName));
             }
         }       
         // get encoding through resource plugin
         defaultOptions.put(CORE_ENCODING, ResourcesPlugin.getEncoding()); 
         
         return defaultOptions;
     }
 
    
     /**
      * Helper method for returning one option value only. Equivalent to <code>(String)CCorePlugin.getOptions().get(optionName)</code>
      * Note that it may answer <code>null</code> if this option does not exist.
      * <p>
      * For a complete description of the configurable options, see <code>getDefaultOptions</code>.
      * </p>
      * 
      * @param optionName the name of an option
      * @return the String value of a given option
      * @see CCorePlugin#getDefaultOptions
      */
     public static String getOption(String optionName) {
         
         if (CORE_ENCODING.equals(optionName)){
             return ResourcesPlugin.getEncoding();
         }
         if (CModelManager.OptionNames.contains(optionName)){
             Preferences preferences = getDefault().getPluginPreferences();
             return preferences.getString(optionName).trim();
         }
         return null;
     }
     
     /**
      * Returns the table of the current options. Initially, all options have their default values,
      * and this method returns a table that includes all known options.
      * <p>
      * For a complete description of the configurable options, see <code>getDefaultOptions</code>.
      * </p>
      * 
      * @return table of current settings of all options 
      *   (key type: <code>String</code>; value type: <code>String</code>)
      * @see CCorePlugin#getDefaultOptions
      */
     public static HashMap getOptions() {
         
         HashMap options = new HashMap(10);
 
         // see #initializeDefaultPluginPreferences() for changing default settings
         Plugin plugin = getDefault();
         if (plugin != null) {
             Preferences preferences = plugin.getPluginPreferences();
             HashSet optionNames = CModelManager.OptionNames;
             
             // get preferences set to their default
             String[] defaultPropertyNames = preferences.defaultPropertyNames();
             for (int i = 0; i < defaultPropertyNames.length; i++){
                 String propertyName = defaultPropertyNames[i];
                 if (optionNames.contains(propertyName)){
                     options.put(propertyName, preferences.getDefaultString(propertyName));
                 }
             }       
             // get preferences not set to their default
             String[] propertyNames = preferences.propertyNames();
             for (int i = 0; i < propertyNames.length; i++){
                 String propertyName = propertyNames[i];
                 if (optionNames.contains(propertyName)){
                     options.put(propertyName, preferences.getString(propertyName).trim());
                 }
             }       
             // get encoding through resource plugin
             options.put(CORE_ENCODING, ResourcesPlugin.getEncoding());
         }
         return options;
     }
 
     /**
      * Sets the current table of options. All and only the options explicitly included in the given table 
      * are remembered; all previous option settings are forgotten, including ones not explicitly
      * mentioned.
      * <p>
      * For a complete description of the configurable options, see <code>getDefaultOptions</code>.
      * </p>
      * 
      * @param newOptions the new options (key type: <code>String</code>; value type: <code>String</code>),
      *   or <code>null</code> to reset all options to their default values
      * @see CCorePlugin#getDefaultOptions
      */
     public static void setOptions(HashMap newOptions) {
     
         // see #initializeDefaultPluginPreferences() for changing default settings
         Preferences preferences = getDefault().getPluginPreferences();
 
         if (newOptions == null){
             newOptions = getDefaultOptions();
         }
         Iterator keys = newOptions.keySet().iterator();
         while (keys.hasNext()){
             String key = (String)keys.next();
             if (!CModelManager.OptionNames.contains(key)) continue; // unrecognized option
             if (key.equals(CORE_ENCODING)) continue; // skipped, contributed by resource prefs
             String value = (String)newOptions.get(key);
             preferences.setValue(key, value);
         }
     
         // persist options
         getDefault().savePluginPreferences();
     }    
     
 
 	public IConsole getConsole(String id) {
 		try {
 	        IExtensionPoint extension = Platform.getExtensionRegistry().getExtensionPoint(CCorePlugin.PLUGIN_ID, "CBuildConsole"); //$NON-NLS-1$
 			if (extension != null) {
 				IExtension[] extensions = extension.getExtensions();
 				for (int i = 0; i < extensions.length; i++) {
 					IConfigurationElement[] configElements = extensions[i].getConfigurationElements();
 					for (int j = 0; j < configElements.length; j++) {
 						String consoleID = configElements[j].getAttribute("id"); //$NON-NLS-1$
 						if ((id == null && consoleID == null) || (id != null && id.equals(consoleID))) {
 							return (IConsole) configElements[j].createExecutableExtension("class"); //$NON-NLS-1$
 						}
 					}
 				}
 			}
 		} catch (CoreException e) {
 			log(e);
 		}
 		return new IConsole() { // return a null console
 			private ConsoleOutputStream nullStream = new ConsoleOutputStream() {
 			    public void write(byte[] b) throws IOException {
 			    }			    
 				public void write(byte[] b, int off, int len) throws IOException {
 				}					
 				public void write(int c) throws IOException {
 				}
 			};
 			
 			public void start(IProject project) {
 			}
 		    // this can be a null console....
 			public ConsoleOutputStream getOutputStream() {
 				return nullStream;
 			}
 			public ConsoleOutputStream getInfoStream() {
 				return nullStream; 
 			}
 			public ConsoleOutputStream getErrorStream() {
 				return nullStream;
 			}
 		};
 	}
 
 	public IConsole getConsole() {
 		String consoleID = System.getProperty("org.eclipse.cdt.core.console"); //$NON-NLS-1$
 		return getConsole(consoleID);
 	}
 
 	public ICExtensionReference[] getBinaryParserExtensions(IProject project) throws CoreException {
 		ICExtensionReference ext[] = new ICExtensionReference[0];
 		if (project != null) {
 			try {
 				ICDescriptor cdesc = getCProjectDescription(project);
 				ICExtensionReference[] cextensions = cdesc.get(BINARY_PARSER_UNIQ_ID, true);
 				if (cextensions.length > 0) {
 					ArrayList list = new ArrayList(cextensions.length);
 					for (int i = 0; i < cextensions.length; i++) {
 						list.add(cextensions[i]);
 					}
 					ext = (ICExtensionReference[])list.toArray(ext);
 				}
 			} catch (CoreException e) {
 				log(e);
 			}
 		}
 		return ext;
 	}
 
 	/**
 	 * @param project
 	 * @return
 	 * @throws CoreException
 	 * @deprecated - use getBinaryParserExtensions(IProject project)
 	 */
 	public IBinaryParser[] getBinaryParser(IProject project) throws CoreException {
 		IBinaryParser parsers[] = null;
 		if (project != null) {
 			try {
 				ICDescriptor cdesc = getCProjectDescription(project);
 				ICExtensionReference[] cextensions = cdesc.get(BINARY_PARSER_UNIQ_ID, true);
 				if (cextensions.length > 0) {
 					ArrayList list = new ArrayList(cextensions.length);
 					for (int i = 0; i < cextensions.length; i++) {
 						IBinaryParser parser = null;
 						try {
 							parser = (IBinaryParser) cextensions[i].createExtension();
 						} catch (ClassCastException e) {
 							//
 						}
 						if (parser != null) {
 							list.add(parser);
 						}
 					}
 					parsers = new IBinaryParser[list.size()];
 					list.toArray(parsers);
 				}
 			} catch (CoreException e) {
 				// ignore since we fall back to a default....
 			}
 		}
 		if (parsers == null) {
 			IBinaryParser parser = getDefaultBinaryParser();
 			if (parser != null) {
 				parsers = new IBinaryParser[] {parser};
 			}
 		}
 		return parsers;
 	}
 
 	public IBinaryParser getDefaultBinaryParser() throws CoreException {
 		IBinaryParser parser = null;
 		String id = getPluginPreferences().getDefaultString(PREF_BINARY_PARSER);
 		if (id == null || id.length() == 0) {
 			id = DEFAULT_BINARY_PARSER_UNIQ_ID;
 		}
         IExtensionPoint extensionPoint = Platform.getExtensionRegistry().getExtensionPoint(CCorePlugin.PLUGIN_ID, BINARY_PARSER_SIMPLE_ID);
 		IExtension extension = extensionPoint.getExtension(id);
 		if (extension != null) {
 			IConfigurationElement element[] = extension.getConfigurationElements();
 			for (int i = 0; i < element.length; i++) {
 				if (element[i].getName().equalsIgnoreCase("cextension")) { //$NON-NLS-1$
 					parser = (IBinaryParser) element[i].createExecutableExtension("run"); //$NON-NLS-1$
 					break;
 				}
 			}
 		} else {
 			IStatus s = new Status(IStatus.ERROR, CCorePlugin.PLUGIN_ID, -1, CCorePlugin.getResourceString("CCorePlugin.exception.noBinaryFormat"), null); //$NON-NLS-1$
 			throw new CoreException(s);
 		}
 		return parser;
 	}
 
 	public CoreModel getCoreModel() {
 		return fCoreModel;
 	}
 
 	/**
 	 * @deprecated use getIndexManager().
 	 */
 	public static IPDOMManager getPDOMManager() {
 		return getDefault().pdomManager;
 	}
 
 	public static IIndexManager getIndexManager() {
 		return getDefault().pdomManager;
 	}
 	
 	public IPathEntryVariableManager getPathEntryVariableManager() {
 		return fPathEntryVariableManager;
 	}
 
 	/**
 	 * @param project
 	 * @return
 	 * @throws CoreException
 	 * @deprecated use getCProjetDescription(IProject project, boolean create)
 	 */
 	public ICDescriptor getCProjectDescription(IProject project) throws CoreException {
 		return fDescriptorManager.getDescriptor(project);
 	}
 
 	/**
 	 * Get the ICDescriptor for the given project, if <b>create</b> is <b>true</b> then a descriptor will be created
 	 * if one does not exist.
 	 * 
 	 * @param project
 	 * @param create
 	 * @return ICDescriptor or <b>null</b> if <b>create</b> is <b>false</b> and no .cdtproject file exists on disk.
 	 * @throws CoreException
 	 */
 	public ICDescriptor getCProjectDescription(IProject project, boolean create) throws CoreException {
 		return fDescriptorManager.getDescriptor(project, create);
 	}
 
 	public void mapCProjectOwner(IProject project, String id, boolean override) throws CoreException {
 		if (!override) {
 			fDescriptorManager.configure(project, id);
 		} else {
 			fDescriptorManager.convert(project, id);
 		}
 	}
 	
 	public ICDescriptorManager getCDescriptorManager() {
 		return fDescriptorManager;
 	}
 
 	/**
 	 * Creates a C project resource given the project handle and description.
 	 *
 	 * @param description the project description to create a project resource for
 	 * @param projectHandle the project handle to create a project resource for
 	 * @param monitor the progress monitor to show visual progress with
 	 * @param projectID required for mapping the project to an owner
 	 *
 	 * @exception CoreException if the operation fails
 	 * @exception OperationCanceledException if the operation is canceled
 	 */
 	public IProject createCProject(
 		final IProjectDescription description,
 		final IProject projectHandle,
 		IProgressMonitor monitor,
 		final String projectID)
 		throws CoreException, OperationCanceledException {
 
 		getWorkspace().run(new IWorkspaceRunnable() {
 			public void run(IProgressMonitor monitor) throws CoreException {
 				try {
 					if (monitor == null) {
 						monitor = new NullProgressMonitor();
 					}
 					monitor.beginTask("Creating C Project...", 3); //$NON-NLS-1$
 					if (!projectHandle.exists()) {
 						projectHandle.create(description, new SubProgressMonitor(monitor, 1));
 					}
 					
 					if (monitor.isCanceled()) {
 						throw new OperationCanceledException();
 					}
 					
 					// Open first.
 					projectHandle.open(IResource.BACKGROUND_REFRESH, new SubProgressMonitor(monitor, 1));
 
 					mapCProjectOwner(projectHandle, projectID, false);
 
 					// Add C Nature ... does not add duplicates
 					CProjectNature.addCNature(projectHandle, new SubProgressMonitor(monitor, 1));
 				} finally {
 					monitor.done();
 				}
 			}
 		}, getWorkspace().getRoot(), 0, monitor);
 		return projectHandle;
 	}
 
 	public IProject createCDTProject(
 			final IProjectDescription description,
 			final IProject projectHandle,
 			IProgressMonitor monitor) throws CoreException, OperationCanceledException{
 		return createCDTProject(description, projectHandle, null, monitor);
 	}
 
 	public IProject createCDTProject(
 			final IProjectDescription description,
 			final IProject projectHandle,
 			final String bsId,
 			IProgressMonitor monitor)
 			throws CoreException, OperationCanceledException {
 
 			getWorkspace().run(new IWorkspaceRunnable() {
 				public void run(IProgressMonitor monitor) throws CoreException {
 					try {
 						if (monitor == null) {
 							monitor = new NullProgressMonitor();
 						}
 						monitor.beginTask("Creating C Project...", 3); //$NON-NLS-1$
 						if (!projectHandle.exists()) {
 							projectHandle.create(description, new SubProgressMonitor(monitor, 1));
 						}
 						
 						if (monitor.isCanceled()) {
 							throw new OperationCanceledException();
 						}
 						
 						// Open first.
 						projectHandle.open(IResource.BACKGROUND_REFRESH, new SubProgressMonitor(monitor, 1));
 
 //						mapCProjectOwner(projectHandle, projectID, false);
 
 						// Add C Nature ... does not add duplicates
 						CProjectNature.addCNature(projectHandle, new SubProgressMonitor(monitor, 1));
 						
 						if(bsId != null){
 							ICProjectDescription projDes = createProjectDescription(projectHandle, true);
 							ICConfigurationDescription cfgs[] = projDes.getConfigurations();
 							ICConfigurationDescription cfg = null;
 							for(int i = 0; i < cfgs.length; i++){
 								if(bsId.equals(cfgs[i].getBuildSystemId())){
 									cfg = cfgs[i];
 									break;
 								}
 							}
 							
 							if(cfg == null){
 								ICConfigurationDescription prefCfg = getPreferenceConfiguration(bsId);
 								if(prefCfg != null){
 									cfg = projDes.createConfiguration(CDataUtil.genId(prefCfg.getId()), prefCfg.getName(), prefCfg);
 								}
 							}
 							
 							if(cfg != null){
 								setProjectDescription(projectHandle, projDes);
 							}
 						}
 					} finally {
 						monitor.done();
 					}
 				}
 			}, getWorkspace().getRoot(), 0, monitor);
 			return projectHandle;
 		}
 
 	/**
 	 * Method convertProjectFromCtoCC converts
 	 * a C Project to a C++ Project
 	 * The newProject MUST, not be null, already have a C Nature 
 	 * && must NOT already have a C++ Nature
 	 * 
 	 * @param projectHandle
 	 * @param monitor
 	 * @throws CoreException
 	 */
 
 	public void convertProjectFromCtoCC(IProject projectHandle, IProgressMonitor monitor) throws CoreException {
 		if ((projectHandle != null)
 			&& projectHandle.hasNature(CProjectNature.C_NATURE_ID)
 			&& !projectHandle.hasNature(CCProjectNature.CC_NATURE_ID)) {
 			// Add C++ Nature ... does not add duplicates        
 			CCProjectNature.addCCNature(projectHandle, monitor);
 		}
 	}
 
 	/**
 	 * Method to convert a project to a C nature 
 	 * All checks should have been done externally
 	 * (as in the Conversion Wizards). 
 	 * This method blindly does the conversion.
 	 * 
 	 * @param project
 	 * @param String targetNature
 	 * @param monitor
 	 * @param projectID
 	 * @exception CoreException
 	 */
 
 	public void convertProjectToC(IProject projectHandle, IProgressMonitor monitor, String projectID)
 		throws CoreException {
 		if ((projectHandle == null) || (monitor == null) || (projectID == null)) {
 			return;
 		}
 		IWorkspace workspace = ResourcesPlugin.getWorkspace();
 		IProjectDescription description = workspace.newProjectDescription(projectHandle.getName());
 		description.setLocation(projectHandle.getFullPath());
 		createCProject(description, projectHandle, monitor, projectID);
 	}
 
 	public void convertProjectToNewC(IProject projectHandle, String bsId, IProgressMonitor monitor)
 		throws CoreException {
 		if ((projectHandle == null) || (monitor == null) || (bsId == null)) {
 			throw new NullPointerException();
 		}
 		IWorkspace workspace = ResourcesPlugin.getWorkspace();
 		IProjectDescription description = workspace.newProjectDescription(projectHandle.getName());
 		description.setLocation(projectHandle.getFullPath());
 		createCDTProject(description, projectHandle, bsId, monitor);
 	}
 
 	/**
 	 * Method to convert a project to a C++ nature 
 	 * 
 	 * @param project
 	 * @param String targetNature
 	 * @param monitor
 	 * @param projectID
 	 * @exception CoreException
 	 */
 
 	public void convertProjectToCC(IProject projectHandle, IProgressMonitor monitor, String projectID)
 		throws CoreException {
 		if ((projectHandle == null) || (monitor == null) || (projectID == null)) {
 			return;
 		}
 		createCProject(projectHandle.getDescription(), projectHandle, monitor, projectID);
 		// now add C++ nature
 		convertProjectFromCtoCC(projectHandle, monitor);
 	}
 
 	public void convertProjectToNewCC(IProject projectHandle, String bsId, IProgressMonitor monitor)
 		throws CoreException {
 		if ((projectHandle == null) || (monitor == null) || (bsId == null)) {
 			throw new NullPointerException();
 		}
 		createCDTProject(projectHandle.getDescription(), projectHandle, bsId, monitor);
 		// now add C++ nature
 		convertProjectFromCtoCC(projectHandle, monitor);
 	}
 
 	/**
 	 * Get the IProcessList contributed interface for the platform.
 	 * @return IProcessList
 	 */
 	public IProcessList getProcessList() throws CoreException {
         IExtensionPoint extension = Platform.getExtensionRegistry().getExtensionPoint(CCorePlugin.PLUGIN_ID, "ProcessList"); //$NON-NLS-1$
 		if (extension != null) {
 			IExtension[] extensions = extension.getExtensions();
 			IConfigurationElement defaultContributor = null;
 			for (int i = 0; i < extensions.length; i++) {
 				IConfigurationElement[] configElements = extensions[i].getConfigurationElements();
 				for (int j = 0; j < configElements.length; j++) {
 					if (configElements[j].getName().equals("processList")) { //$NON-NLS-1$
 						String platform = configElements[j].getAttribute("platform"); //$NON-NLS-1$
 						if (platform == null ) { // first contrbutor found with not platform will be default.
 							if (defaultContributor == null) {
 								defaultContributor = configElements[j];
 							}
 						} else if (platform.equals(Platform.getOS())) {
 							// found explicit contributor for this platform.
 							return (IProcessList) configElements[0].createExecutableExtension("class"); //$NON-NLS-1$
 						}
 					}
 				}
 			}
 			if ( defaultContributor != null) { 
 				return (IProcessList) defaultContributor.createExecutableExtension("class"); //$NON-NLS-1$
 			}
 		}
 		return null;
 		
 	}
 	
 	/**
 	 * Array of error parsers ids.
 	 * @return
 	 */
 	public String[] getAllErrorParsersIDs() {
         IExtensionPoint extension = Platform.getExtensionRegistry().getExtensionPoint(CCorePlugin.PLUGIN_ID, ERROR_PARSER_SIMPLE_ID);
 		String[] empty = new String[0];
 		if (extension != null) {
 			IExtension[] extensions = extension.getExtensions();
 			ArrayList list = new ArrayList(extensions.length);
 			for (int i = 0; i < extensions.length; i++) {
 				list.add(extensions[i].getUniqueIdentifier());
 			}
 			return (String[]) list.toArray(empty);
 		}
 		return empty;
 	}
 
 	public IErrorParser[] getErrorParser(String id) {
 		IErrorParser[] empty = new IErrorParser[0];
 		try {
 	        IExtensionPoint extension = Platform.getExtensionRegistry().getExtensionPoint(CCorePlugin.PLUGIN_ID, ERROR_PARSER_SIMPLE_ID);
 			if (extension != null) {
 				IExtension[] extensions = extension.getExtensions();
 				List list = new ArrayList(extensions.length);
 				for (int i = 0; i < extensions.length; i++) {
 					String parserID = extensions[i].getUniqueIdentifier();
 					if ((id == null && parserID != null) || (id != null && id.equals(parserID))) {
 						IConfigurationElement[] configElements = extensions[i]. getConfigurationElements();
 						for (int j = 0; j < configElements.length; j++) {
 							IErrorParser parser = (IErrorParser)configElements[j].createExecutableExtension("class"); //$NON-NLS-1$
 							list.add(parser);
 						}
 					}
 				}
 				return (IErrorParser[]) list.toArray(empty);
 			}
 		} catch (CoreException e) {
 			log(e);
 		}
 		return empty;
 	}
 
 	public IScannerInfoProvider getScannerInfoProvider(IProject project) {
 		return fNewCProjectDescriptionManager.getScannerInfoProviderProxy(project);
 //		IScannerInfoProvider provider = null;
 //		if (project != null) {
 //			try {
 //				ICDescriptor desc = getCProjectDescription(project);
 //				ICExtensionReference[] extensions = desc.get(BUILD_SCANNER_INFO_UNIQ_ID, true);
 //				if (extensions.length > 0)
 //					provider = (IScannerInfoProvider) extensions[0].createExtension();
 //			} catch (CoreException e) {
 //				// log(e);
 //			}
 //			if ( provider == null) {
 //				return getDefaultScannerInfoProvider(project);
 //			}
 //		}
 //		return provider;
 	}
 	
 //	private IScannerInfoProvider getDefaultScannerInfoProvider(IProject project){
 //		if(fNewCProjectDescriptionManager.isNewStyleIndexCfg(project))
 //			return fNewCProjectDescriptionManager.getScannerInfoProvider(project);
 //		return ScannerProvider.getInstance();
 //	}
 
 	/**
	 * Helper function, returning the contenttype for a filename
	 * Same as: <p><p>
 	 * 	getContentType(null, filename)
	 * <br>
	 * @param project
	 * @param name
	 * @return
 	 */
 	public static IContentType getContentType(String filename) {
 		return CContentTypes.getContentType(null, filename);
 	}
 	
 	/**
	 * Returns the content type for a filename. The method respects project
 	 * project specific content type definitions. The lookup prefers case-
 	 * sensitive matches over the others.
 	 * @param project a project with possible project specific settings. Can be <code>null</code>
 	 * @param filename a filename to compute the content type for
 	 * @return the content type found or <code>null</code>
 	 */
 	public static IContentType getContentType(IProject project, String filename) {
 		return CContentTypes.getContentType(project, filename);
 	}
 	
 	/**
 	 * Tests whether the given project uses its project specific content types.
 	 */
 	public static boolean usesProjectSpecificContentTypes(IProject project) {
 		return CContentTypes.usesProjectSpecificContentTypes(project);
 	}
 
 	/**
 	 * Enables or disables the project specific content types.
 	 */
 	public static void setUseProjectSpecificContentTypes(IProject project, boolean val) {
 		CContentTypes.setUseProjectSpecificContentTypes(project, val);
 	}
 
 	
 
 	private static final String MODEL = CCorePlugin.PLUGIN_ID + "/debug/model" ; //$NON-NLS-1$
 	private static final String PARSER = CCorePlugin.PLUGIN_ID + "/debug/parser" ; //$NON-NLS-1$
 	private static final String PARSER_EXCEPTIONS = CCorePlugin.PLUGIN_ID + "/debug/parser/exceptions" ; //$NON-NLS-1$
 	private static final String SCANNER = CCorePlugin.PLUGIN_ID + "/debug/scanner"; //$NON-NLS-1$
 	private static final String DELTA = CCorePlugin.PLUGIN_ID + "/debug/deltaprocessor" ; //$NON-NLS-1$
 	//private static final String CONTENTASSIST = CCorePlugin.PLUGIN_ID + "/debug/contentassist" ; //$NON-NLS-1$
 
 	/**
 	 * Configure the plugin with respect to option settings defined in ".options" file
 	 */
 	public void configurePluginDebugOptions() {
 		
 		if(CCorePlugin.getDefault().isDebugging()) {
 			String option = Platform.getDebugOption(PARSER);
 			if(option != null) Util.VERBOSE_PARSER = option.equalsIgnoreCase("true") ; //$NON-NLS-1$
 
 			option = Platform.getDebugOption(PARSER_EXCEPTIONS);
 			if( option != null ) Util.PARSER_EXCEPTIONS = option.equalsIgnoreCase("true"); //$NON-NLS-1$
 
 			option = Platform.getDebugOption(SCANNER);
 			if( option != null ) Util.VERBOSE_SCANNER = option.equalsIgnoreCase("true"); //$NON-NLS-1$
 			
 			option = Platform.getDebugOption(MODEL);
 			if(option != null) Util.VERBOSE_MODEL = option.equalsIgnoreCase("true") ; //$NON-NLS-1$
 			
 			option = Platform.getDebugOption(DELTA);
 			if(option != null) DeltaProcessor.VERBOSE = option.equalsIgnoreCase("true") ; //$NON-NLS-1$
 			
 		}
 	}
 
 	// Preference to turn on/off the use of structural parse mode to build the CModel
 	public void setStructuralParseMode(boolean useNewParser) {
 		getPluginPreferences().setValue(PREF_USE_STRUCTURAL_PARSE_MODE, useNewParser);
 		savePluginPreferences();
 	}
 
 	public boolean useStructuralParseMode() {
 		return getPluginPreferences().getBoolean(PREF_USE_STRUCTURAL_PARSE_MODE);
 	}
 	
 	public CDOM getDOM() {
 	    return CDOM.getInstance();
 	}
 
 	public ICdtVariableManager getCdtVariableManager(){
 		return CdtVariableManager.getDefault();
 	}
 	
 	public IEnvironmentVariableManager getBuildEnvironmentManager(){
 		return EnvironmentVariableManager.getDefault();
 	}
 	
 	public ICConfigurationDescription getPreferenceConfiguration(String buildSystemId) throws CoreException{
 		return fNewCProjectDescriptionManager.getPreferenceConfiguration(buildSystemId);
 	}
 
 	public ICConfigurationDescription getPreferenceConfiguration(String buildSystemId, boolean write) throws CoreException{
 		return fNewCProjectDescriptionManager.getPreferenceConfiguration(buildSystemId, write);
 	}
 	
 	public void setPreferenceConfiguration(String buildSystemId, ICConfigurationDescription des) throws CoreException {
 		fNewCProjectDescriptionManager.setPreferenceConfiguration(buildSystemId, des);
 	}
 	
 	/**
 	 * the method creates and returns a writable project description
 	 * 
 	 * @param project project for which the project description is requested
 	 * @param loadIfExists if true the method first tries to load and return the project description
 	 * from the settings file (.cproject)
 	 * if false, the stored settings are ignored and the new (empty) project description is created
 	 * NOTE: changes made to the returned project description will not be applied untill the {@link #setProjectDescription(IProject, ICProjectDescription)} is called 
 	 * @return {@link ICProjectDescription}
 	 * @throws CoreException
 	 */
 	public ICProjectDescription createProjectDescription(IProject project, boolean loadIfExists) throws CoreException{
 		return fNewCProjectDescriptionManager.createProjectDescription(project, loadIfExists);
 	}
 	
 	/**
 	 * returns the project description associated with this project
 	 * this is a convenience method fully equivalent to getProjectDescription(project, true)
 	 * see {@link #getProjectDescription(IProject, boolean)} for more detail
 	 * @param project
 	 * @return a writable copy of the ICProjectDescription or null if the project does not contain the
 	 * CDT data associated with it. 
 	 * Note: changes to the project description will not be reflected/used by the core
 	 * untill the {@link #setProjectDescription(IProject, ICProjectDescription)} is called
 	 * 
 	 * @see #getProjectDescription(IProject, boolean)
 	 */
 	public ICProjectDescription getProjectDescription(IProject project){
 		return fNewCProjectDescriptionManager.getProjectDescription(project);
 	}
 	
 	/**
 	 * this method is called to save/apply the project description
 	 * the method should be called to apply changes made to the project description
 	 * returned by the {@link #getProjectDescription(IProject, boolean)} or {@link #createProjectDescription(IProject, boolean)} 
 	 * 
 	 * @param project
 	 * @param des
 	 * @throws CoreException
 	 * 
 	 * @see {@link #getProjectDescription(IProject, boolean)}
 	 * @see #createProjectDescription(IProject, boolean)
 	 */
 	public void setProjectDescription(IProject project, ICProjectDescription des) throws CoreException {
 		fNewCProjectDescriptionManager.setProjectDescription(project, des);
 	}
 
 	public void setProjectDescription(IProject project, ICProjectDescription des, boolean force, IProgressMonitor monitor) throws CoreException {
 		fNewCProjectDescriptionManager.setProjectDescription(project, des, force, monitor);
 	}
 
 	/**
 	 * returns the project description associated with this project
 	 * 
 	 * @param project project for which the description is requested
 	 * @param write if true, the writable description copy is returned. 
 	 * If false the cached read-only description is returned.
 	 * 
 	 * CDT core maintains the cached project description settings. If only read access is needed to description,
 	 * then the read-only project description should be obtained.
 	 * This description always operates with cached data and thus it is better to use it for performance reasons
 	 * All set* calls to the read-only description result in the {@link WriteAccessException}
 	 * 
 	 * When the writable description is requested, the description copy is created.
 	 * Changes to this description will not be reflected/used by the core and Build System untill the
 	 * {@link #setProjectDescription(IProject, ICProjectDescription)} is called
 	 *
 	 * Each getProjectDescription(project, true) returns a new copy of the project description 
 	 * 
 	 * The writable description uses the cached data untill the first set call
 	 * after that the description communicates directly to the Build System
 	 * i.e. the implementer of the org.eclipse.cdt.core.CConfigurationDataProvider extension
 	 * This ensures the Core<->Build System settings integrity
 	 * 
 	 * @return {@link ICProjectDescription}
 	 */
 	public ICProjectDescription getProjectDescription(IProject project, boolean write){
 		return fNewCProjectDescriptionManager.getProjectDescription(project, write);
 	}
 
 	/**
 	 * forces the cached data of the specified projects to be re-calculated.
 	 * if the <code>projects</code> argument is <code>null</code> al projects 
 	 * within the workspace are updated
 	 * 
 	 * @param projects
 	 * @param monitor
 	 * @throws CoreException 
 	 */
 	public void updateProjectDescriptions(IProject projects[], IProgressMonitor monitor) throws CoreException{
 		fNewCProjectDescriptionManager.updateProjectDescriptions(projects, monitor);
 	}
 	
 	/**
 	 * aswers whether the given project is a new-style project, i.e. CConfigurationDataProvider-driven
 	 * @param project
 	 * @return
 	 */
 	public boolean isNewStyleProject(IProject project){
 		return fNewCProjectDescriptionManager.isNewStyleProject(project);
 	}
 
 	/**
 	 * aswers whether the given project is a new-style project, i.e. CConfigurationDataProvider-driven
 	 * @param des
 	 * @return
 	 */
 	public boolean isNewStyleProject(ICProjectDescription des){
 		return fNewCProjectDescriptionManager.isNewStyleProject(des);
 	}
 }
