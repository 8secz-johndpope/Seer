 /*******************************************************************************
  * Copyright (c) 2004 - 2005 University Of British Columbia and others.
  * All rights reserved. This program and the accompanying materials
  * are made available under the terms of the Eclipse Public License v1.0
  * which accompanies this distribution, and is available at
  * http://www.eclipse.org/legal/epl-v10.html
  *
  * Contributors:
  *     University Of British Columbia - initial API and implementation
  *******************************************************************************/
 package org.eclipse.mylar.core;
 
 import java.io.File;
 import java.io.PrintStream;
 import java.io.PrintWriter;
 import java.io.StringWriter;
 import java.util.ArrayList;
 import java.util.Arrays;
 import java.util.Collections;
 import java.util.HashMap;
 import java.util.List;
 import java.util.Map;
 import java.util.MissingResourceException;
 import java.util.ResourceBundle;
import java.util.Set;
 
 import org.eclipse.core.resources.IProject;
 import org.eclipse.core.resources.ResourcesPlugin;
 import org.eclipse.core.runtime.CoreException;
 import org.eclipse.core.runtime.IConfigurationElement;
 import org.eclipse.core.runtime.IExtension;
 import org.eclipse.core.runtime.IExtensionPoint;
 import org.eclipse.core.runtime.IExtensionRegistry;
 import org.eclipse.core.runtime.IStatus;
 import org.eclipse.core.runtime.Platform;
 import org.eclipse.core.runtime.Status;
 import org.eclipse.jface.dialogs.ErrorDialog;
 import org.eclipse.jface.resource.ImageDescriptor;
 import org.eclipse.mylar.core.internal.MylarContextManager;
 import org.eclipse.mylar.core.search.MylarWorkingSetUpdater;
 import org.eclipse.mylar.core.util.DateUtil;
 import org.eclipse.mylar.core.util.IInteractionEventListener;
 import org.eclipse.ui.internal.Workbench;
 import org.eclipse.ui.internal.WorkbenchPlugin;
 import org.eclipse.ui.plugin.AbstractUIPlugin;
 import org.osgi.framework.BundleContext;
 
 /**
  * @author Mik Kersten
  */
 public class MylarPlugin extends AbstractUIPlugin {
 	
 	public static final String EXTENSION_ID_CONTEXT = "org.eclipse.mylar.core.context";
 	public static final String ELEMENT_STRUCTURE_BRIDGE = "structureBridge";
 	public static final String ELEMENT_STRUCTURE_BRIDGE_CLASS = "class";
 	public static final String ELEMENT_STRUCTURE_BRIDGE_PARENT = "parent";
 	public static final String ELEMENT_STRUCTURE_BRIDGE_SEARCH_ICON = "activeSearchIcon";
 	public static final String ELEMENT_STRUCTURE_BRIDGE_SEARCH_LABEL = "activeSearchLabel";
 	
 	public static final String CONTENT_TYPE_ANY = "*";
 		
     private static final String ERROR_MESSAGE = "Please report the following error by following the link at:\n"
     	+ "https://eclipse.org/mylar\n\n"
     	+ "For details on this error please open the PDE Runtime -> Error Log view"; 
 
 	private Map<String, IMylarStructureBridge> bridges = new HashMap<String, IMylarStructureBridge>();
     private Map<IMylarStructureBridge, ImageDescriptor> activeSearchIcons = new HashMap<IMylarStructureBridge, ImageDescriptor>();
     private Map<IMylarStructureBridge, String> activeSearchLabels = new HashMap<IMylarStructureBridge, String>();
 	
     private IMylarStructureBridge defaultBridge = null;
     
     private List<AbstractSelectionMonitor> selectionMonitors = new ArrayList<AbstractSelectionMonitor>();
     private List<AbstractCommandMonitor> commandMonitors = new ArrayList<AbstractCommandMonitor>();
     
     private List<MylarWorkingSetUpdater> workingSetUpdaters = null; 
     
     /**
      * TODO: this could be merged with context interaction events rather than
      * requiring update from the monitor.
      */
     private List<IInteractionEventListener> interactionListeners = new ArrayList<IInteractionEventListener>();
 	      
     public static final String USER_ID = "org.eclipse.mylar.user.id";
     public static boolean started = false;
     
 	private static MylarPlugin INSTANCE;
     private static MylarContextManager contextManager;
     private ResourceBundle resourceBundle;
     public static final String IDENTIFIER = "org.eclipse.mylar.core";
     public static final String LOG_FILE_NAME = "mylar-log.txt";
     private PrintStream logStream = null;
 
 	public static final String TASKLIST_EDITORS_CLOSE = "org.eclipse.mylar.tasklist.activation.editors.close.all";
 	public static final String WORK_OFFLINE = "org.eclipse.mylar.tasklist.work.offline";
     
     public static final String MYLAR_DIR = "org.eclipse.mylar.model.dir";
     public static final String MYLAR_DIR_NAME = ".mylar";
     
     private static final IMylarStructureBridge DEFAULT_BRIDGE = new IMylarStructureBridge() {
 
         public String getContentType() {
             return null;
         }
         
         public String getHandleIdentifier(Object object) {
 //            MylarPlugin.log(this, "null bridge for object: " + object.getClass());
 //            return null; 
             throw new RuntimeException("null bridge for object: " + object.getClass());
         }
 
         public Object getObjectForHandle(String handle) {
             MylarPlugin.log("null bridge for handle: " + handle, this);
             return null;
 //            throw new RuntimeException("null adapter for handle: " + handle);
         }
 
         public String getParentHandle(String handle) {
             MylarPlugin.log("null bridge for handle: " + handle, this);
             return null;
         }
 
         public String getName(Object object) {
             MylarPlugin.log("null bridge for object: " + object.getClass(), this);
             return "";
 //            throw new RuntimeException("null adapter");
         }
 
         /**
          * TODO: this behavir is depended on, move?
          */
         public boolean canBeLandmark(String handle) {
         	return false;
 //            return false;
 //            throw new RuntimeException("null bridge: " + element);
         }
 
         public boolean acceptsObject(Object object) {
 //            return false;
             throw new RuntimeException("null bridge for object: " + object.getClass());
         }
 
         public boolean canFilter(Object element) {
 //            MylarPlugin.log(this, "null bridge for element: " + element.getClass());
             return false;
         } 
 
         public boolean isDocument(String handle) {
 //            return false;
             throw new RuntimeException("null adapter for handle: " + handle);
         }
         
 		public IProject getProjectForObject(Object object) {
 //			return null;
             throw new RuntimeException("null brige for object: " + object);
 		}
 
         public String getContentType(String elementHandle) {
             return getContentType();
         }
 
 		public List<AbstractRelationProvider> getRelationshipProviders() {
 			return Collections.emptyList();
 		}
 
 		public List<IDegreeOfSeparation> getDegreesOfSeparation() {
 			return Collections.emptyList();
 		}
 
 		public String getHandleForOffsetInObject(Object resource, int offset) {
             MylarPlugin.log("null bridge for marker: " + resource.getClass(), this);
             return null;
 		}
 
 		public void setParentBridge(IMylarStructureBridge bridge) {
 			// TODO Auto-generated method stub
 			
 		}
 
 		public boolean containsProblem(IMylarElement node) {
 			// TODO Auto-generated method stub
 			return false;
 		}
     };
 
     public MylarPlugin() {  
 		INSTANCE = this;  
 	}
 
     
     /**
      * Initialization order is critical.
      */
 	@Override
     public void start(BundleContext context) throws Exception {
         super.start(context);
         getPreferenceStore().setDefault(MYLAR_DIR, getDefaultStoreDirectory());
         if (contextManager == null) contextManager = new MylarContextManager();
 	}
 
 	private String getDefaultStoreDirectory() {
 		return ResourcesPlugin.getWorkspace().getRoot().getLocation().toString()
 		+ File.separator
 		+ MYLAR_DIR_NAME;
 	}
 
     /**
      * This method is called when the plug-in is stopped
      */
     @Override
     public void stop(BundleContext context) throws Exception {
         super.stop(context);
         INSTANCE = null;
         resourceBundle = null;
 
         // Stop all running jobs when we exit if the plugin didn't do it
         Map<String, IMylarStructureBridge> bridges = getStructureBridges();
         for (String extension : bridges.keySet()) {
             IMylarStructureBridge bridge = bridges.get(extension);
             List<AbstractRelationProvider> providers = bridge.getRelationshipProviders();
             if(providers == null) continue;
             for(AbstractRelationProvider provider: providers){
             	provider.stopAllRunningJobs();
             }
         }
     }
     
     public String getMylarDataDirectory() {
         return getPreferenceStore().getString(MylarPlugin.MYLAR_DIR);
     }
     
     public static MylarPlugin getDefault() {
 		return INSTANCE;
 	}
 	
     public static MylarContextManager getContextManager() {
 //    	if (INSTANCE == null) {
 //    		Thread.dumpStack();
 //    	}
 //    	if (contextManager == null) contextManager = new MylarContextManager();
         return contextManager;
     }
 
     public List<AbstractSelectionMonitor> getSelectionMonitors() {
         return selectionMonitors;
     }
 
     /**
      * Logs the specified status with this plug-in's log.
      * 
      * @param status status to log
      */
     public static void log(IStatus status) {
         StringBuffer buffer = new StringBuffer();
         buffer.append("[");
         buffer.append(DateUtil.getFormattedDate());
         buffer.append(", "); 
         buffer.append(DateUtil.getFormattedTime());
         buffer.append("] ");
         
         if (WorkbenchPlugin.getDefault() != null) {
         	buffer.append("version: " + WorkbenchPlugin.getDefault().getBundle().getLocation() + ", ");
         }
 
         buffer.append(status.toString() + ", ");
         
         if (status.getException() != null) {
         	buffer.append("exception: ");
         	buffer.append(printStrackTrace(status.getException()));
         }
         
         if (getDefault() != null) {
         	getDefault().getLog().log(status);
         	if (getDefault().logStream != null) getDefault().logStream.println(buffer.toString());
         }
     }
     
     private static String printStrackTrace(Throwable t) {
     	StringWriter writer = new StringWriter();
     	t.printStackTrace(new PrintWriter(writer));
     	return writer.toString();
     }
 
     public static void log(String message, Object source) {
         if (source != null) message += ", source: " + source.getClass().getName();
     	
         log(new Status(IStatus.INFO, MylarPlugin.IDENTIFIER, IStatus.OK, message, null));    	
     }
     
     public static void log(Throwable throwable, String message) {
     	fail(throwable, message, false);
     }
 
     /**
          * Log a failure
          * @param throwable  can be null
          * @param message The message to include
          * @param informUser if true dialog box will be popped up
          */
     public static void fail(Throwable throwable, String message, boolean informUser) {
         if (message == null) message = "no message";
         message += "\n";
 //        message += printStrackTrace(throwable);
 //        final String messageToUser = message + "\n" + printStrackTrace(throwable);
         
         final Status status= new Status(
                 Status.ERROR,
                 MylarPlugin.IDENTIFIER, 
                 IStatus.OK,
                 message,
                 throwable);
         log(status);
         
         if (informUser) {
           Workbench.getInstance().getDisplay().syncExec(new Runnable() {
               public void run() { 
                   ErrorDialog.openError(
                           Workbench.getInstance().getActiveWorkbenchWindow().getShell(),
                           "Mylar error",
                           ERROR_MESSAGE,
                           status);
               }
           });
         }
     }
 
     public Map<String, IMylarStructureBridge> getStructureBridges() {
     	if (!CoreExtensionPointReader.extensionsRead) CoreExtensionPointReader.initExtensions();
 		return bridges;
 	}
     
     /**
      * TODO: performance issue?
      */
     public IMylarStructureBridge getStructureBridge(String contentType) {
     	if (!CoreExtensionPointReader.extensionsRead) CoreExtensionPointReader.initExtensions();
 		IMylarStructureBridge adapter = bridges.get(contentType);
         if (adapter != null) {
             return adapter;
         } else if (defaultBridge != null) {
             return defaultBridge;
         } else {
         	return DEFAULT_BRIDGE;
         }
     }
    
    public Set<String> getKnownContentTypes() {
    	return bridges.keySet();
    }
 
     private void setActiveSearchIcon(IMylarStructureBridge bridge, ImageDescriptor descriptor) {
     	activeSearchIcons.put(bridge, descriptor);
     }
     
     public ImageDescriptor getActiveSearchIcon(IMylarStructureBridge bridge) {
     	if (!CoreExtensionPointReader.extensionsRead) CoreExtensionPointReader.initExtensions();
 		return activeSearchIcons.get(bridge);
     }
     
     private void setActiveSearchLabel(IMylarStructureBridge bridge, String label) {
     	activeSearchLabels.put(bridge, label);
     }
     
     public String getActiveSearchLabel(IMylarStructureBridge bridge) {
     	if (!CoreExtensionPointReader.extensionsRead) CoreExtensionPointReader.initExtensions();
 		return activeSearchLabels.get(bridge);
     }
     
     /**
      * TODO: cache this to improve performance?
      * 
      * @return null if there are no bridges loaded, null bridge otherwise
      */
     public IMylarStructureBridge getStructureBridge(Object object) {
     	if (!CoreExtensionPointReader.extensionsRead) CoreExtensionPointReader.initExtensions();
 		
         IMylarStructureBridge bridge = null;
         if (bridges.size() == 0) return null;
         for (IMylarStructureBridge structureBridge : bridges.values()) {
             if (structureBridge.acceptsObject(object)) {
                 bridge = structureBridge; 
                 break;
             }
         }
         if (bridge != null) {
             return bridge;
         } else {
             if (defaultBridge != null && defaultBridge.acceptsObject(object)) {
                 return defaultBridge;
             } else {
                 return DEFAULT_BRIDGE;
             }
         }
     }
 
     private void addBridge(IMylarStructureBridge bridge) {
     	if(bridge.getRelationshipProviders() != null){
     		for(AbstractRelationProvider provider: bridge.getRelationshipProviders()){
     			getContextManager().addListener(provider);
     		}
     	}
         bridges.put(bridge.getContentType(), bridge);
         if (bridge.getContentType().equals(CONTENT_TYPE_ANY)) defaultBridge = bridge;
     }
     
     public List<AbstractCommandMonitor> getCommandMonitors() {
         return commandMonitors;
     }
     
     /**
      * Returns the string from the plugin's resource bundle,
      * or 'key' if not found.
      */
     public static String getResourceString(String key) {
         ResourceBundle bundle = MylarPlugin.getDefault().getResourceBundle();
         try {
             return (bundle != null) ? bundle.getString(key) : key;
         } catch (MissingResourceException e) {
             return key;
         }
     }
 
     /**
      * Returns the plugin's resource bundle,
      */
     public ResourceBundle getResourceBundle() {
         try {
             if (resourceBundle == null)
                 resourceBundle = ResourceBundle.getBundle("org.eclipse.mylar.core.MylarPluginResources");
         } catch (MissingResourceException x) {
             resourceBundle = null;
         }
         return resourceBundle;
     }   
 
 	public void setLogStream(PrintStream logStream) {
 		this.logStream = logStream;
 	}
 
 	public PrintStream getLogStream() {
 		return logStream;
 	}
 
 	public boolean suppressWizardsOnStartup() {
     	List<String> commandLineArgs = Arrays.asList(Platform.getCommandLineArgs());
     	if (commandLineArgs.contains("-showmylarwizards")) {
     		return false;
     	} else {
     		return commandLineArgs.contains("-pdelaunch");
     	}
 	}
 
 	/**
 	 * TODO: remove
 	 */
 	public void setDefaultBridge(IMylarStructureBridge defaultBridge) {
 		this.defaultBridge = defaultBridge;
 	}
 	
 	public void addInteractionListener(IInteractionEventListener listener) {
 		interactionListeners.add(listener);
 	}
 	
 	public void removeInteractionListener(IInteractionEventListener listener) {
 		interactionListeners.remove(listener);
 	}
 
     public void notifyInteractionObserved(InteractionEvent interactionEvent) {
 		for (IInteractionEventListener listener : interactionListeners) {
 			listener.interactionObserved(interactionEvent);
 		}
     }
 	
 	public List<IInteractionEventListener> getInteractionListeners() {
 		return interactionListeners;
 	}
 	
 	public void addWorkingSetUpdater(MylarWorkingSetUpdater updater) {
 		if(workingSetUpdaters == null)
 			workingSetUpdaters = new ArrayList<MylarWorkingSetUpdater>();
 		workingSetUpdaters.add(updater);
 		MylarPlugin.getContextManager().addListener(updater);
 	}
 
 	public MylarWorkingSetUpdater getWorkingSetUpdater() {
 		if(workingSetUpdaters == null)
 			return null;
 		else
 			return workingSetUpdaters.get(0);
 	}
 	
 	static class CoreExtensionPointReader {
 		
 		private static boolean extensionsRead = false;
 		private static CoreExtensionPointReader thisReader = new CoreExtensionPointReader();
 		
 		// read the extensions and load the required plugins
 		public static void initExtensions() {
 			// code from "contributing to eclipse" with modifications for deprecated code
 			if(!extensionsRead){
 				IExtensionRegistry registry = Platform.getExtensionRegistry();
 				IExtensionPoint extensionPoint = registry.getExtensionPoint(MylarPlugin.EXTENSION_ID_CONTEXT);
 				IExtension[] extensions = extensionPoint.getExtensions();
 				for(int i = 0; i < extensions.length; i++) {
 					IConfigurationElement[] elements = extensions[i].getConfigurationElements();
 					for(int j = 0; j < elements.length; j++){
 						if(elements[j].getName().compareTo(MylarPlugin.ELEMENT_STRUCTURE_BRIDGE) == 0){
 							readBridge(elements[j]);
 						} 
 					}
 				}
 				extensionsRead = true;
 			}
 		}
 
 		private static void readBridge(IConfigurationElement element) {
 			try{
 				Object object = element.createExecutableExtension(MylarPlugin.ELEMENT_STRUCTURE_BRIDGE_CLASS);
 				if (object instanceof IMylarStructureBridge) {
 					IMylarStructureBridge bridge = (IMylarStructureBridge)object;
 					MylarPlugin.getDefault().addBridge(bridge);
 					if (element.getAttribute(MylarPlugin.ELEMENT_STRUCTURE_BRIDGE_PARENT) != null) {
 						Object parent = element.createExecutableExtension(MylarPlugin.ELEMENT_STRUCTURE_BRIDGE_PARENT);
 						if (parent instanceof IMylarStructureBridge) {
 							((IMylarStructureBridge)bridge).setParentBridge(((IMylarStructureBridge)parent));
 						} else {
 							MylarPlugin.log("Could not load parent bridge: " + parent.getClass().getCanonicalName() + " must implement " + IMylarStructureBridge.class.getCanonicalName(), thisReader);	
 						}
 					}
 					String iconPath = element.getAttribute(MylarPlugin.ELEMENT_STRUCTURE_BRIDGE_SEARCH_ICON);
 					if (iconPath != null) {
 						ImageDescriptor descriptor = AbstractUIPlugin.imageDescriptorFromPlugin(element.getNamespace(), iconPath);
 						if (descriptor != null) {
 							MylarPlugin.getDefault().setActiveSearchIcon(bridge, descriptor);
 						}
 					}
 					String label = element.getAttribute(MylarPlugin.ELEMENT_STRUCTURE_BRIDGE_SEARCH_LABEL);
 					if (label != null) {
 						MylarPlugin.getDefault().setActiveSearchLabel(bridge, label);
 					}
 				} else {
 					MylarPlugin.log("Could not load bridge: " + object.getClass().getCanonicalName() + " must implement " + IMylarStructureBridge.class.getCanonicalName(), thisReader);	
 				}
 			} catch (CoreException e){
 				MylarPlugin.log(e, "Could not load bridge extension");
 			}
 		}
 	}
 }
