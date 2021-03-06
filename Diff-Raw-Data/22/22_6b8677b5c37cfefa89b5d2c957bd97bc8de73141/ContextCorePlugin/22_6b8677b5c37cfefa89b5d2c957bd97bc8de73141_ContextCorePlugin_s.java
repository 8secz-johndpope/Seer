 /*******************************************************************************
  * Copyright (c) 2004 - 2006 University Of British Columbia and others.
  * All rights reserved. This program and the accompanying materials
  * are made available under the terms of the Eclipse Public License v1.0
  * which accompanies this distribution, and is available at
  * http://www.eclipse.org/legal/epl-v10.html
  *
  * Contributors:
  *     University Of British Columbia - initial API and implementation
  *******************************************************************************/
 package org.eclipse.mylyn.context.core;
 
 import java.util.ArrayList;
 import java.util.Collections;
 import java.util.HashMap;
 import java.util.HashSet;
 import java.util.List;
 import java.util.Map;
 import java.util.Set;
 import java.util.concurrent.ConcurrentHashMap;
 import java.util.concurrent.CopyOnWriteArrayList;
 
 import org.eclipse.core.runtime.CoreException;
 import org.eclipse.core.runtime.IConfigurationElement;
 import org.eclipse.core.runtime.IExtension;
 import org.eclipse.core.runtime.IExtensionPoint;
 import org.eclipse.core.runtime.IExtensionRegistry;
 import org.eclipse.core.runtime.Platform;
 import org.eclipse.core.runtime.Plugin;
 import org.eclipse.mylyn.internal.context.core.InteractionContextManager;
 import org.eclipse.mylyn.internal.monitor.core.util.IStatusHandler;
 import org.eclipse.mylyn.internal.monitor.core.util.StatusManager;
 import org.osgi.framework.BundleContext;
 
 /**
  * @author Mik Kersten
  */
 public class ContextCorePlugin extends Plugin {
 
 	public static final String PLUGIN_ID = "org.eclipse.mylyn.core";
 
 	public static final String CONTENT_TYPE_RESOURCE = "resource";
 
 	private Map<String, AbstractContextStructureBridge> bridges = new ConcurrentHashMap<String, AbstractContextStructureBridge>();
 
 	private Map<String, List<String>> childContentTypeMap = new ConcurrentHashMap<String, List<String>>();
 
 	private AbstractContextStructureBridge defaultBridge = null;
 
 	private static ContextCorePlugin INSTANCE;
 
 	private static InteractionContextManager contextManager;
 
 	private static AbstractContextStore contextStore;
 
 	private boolean extensionsLoaded = false;
 
 	private Map<String, Set<AbstractRelationProvider>> relationProviders = new HashMap<String, Set<AbstractRelationProvider>>();
 
 	private static final AbstractContextStructureBridge DEFAULT_BRIDGE = new AbstractContextStructureBridge() {
 
 		@Override
 		public String getContentType() {
 			return null;
 		}
 
 		@Override
 		public String getHandleIdentifier(Object object) {
 			return null;
 //			throw new RuntimeException("null bridge for object: " + object.getClass());
 		}
 
 		@Override
 		public Object getObjectForHandle(String handle) {
 //			MylarStatusHandler.log("null bridge for handle: " + handle, this);
 			return null;
 		}
 
 		@Override
 		public String getParentHandle(String handle) {
 //			MylarStatusHandler.log("null bridge for handle: " + handle, this);
 			return null;
 		}
 
 		@Override
 		public String getName(Object object) {
 //			MylarStatusHandler.log("null bridge for object: " + object.getClass(), this);
 			return "";
 		}
 
 		@Override
 		public boolean canBeLandmark(String handle) {
 			return false;
 		}
 
 		@Override
 		public boolean acceptsObject(Object object) {
 			return false;
 //			throw new RuntimeException("null bridge for object: " + object.getClass());
 		}
 
 		@Override
 		public boolean canFilter(Object element) {
 			return true;
 		}
 
 		@Override
 		public boolean isDocument(String handle) {
 			return false;
 //			throw new RuntimeException("null adapter for handle: " + handle);
 		}
 
 		@Override
 		public String getContentType(String elementHandle) {
 			return getContentType();
 		}
 
 		@Override
 		public String getHandleForOffsetInObject(Object resource, int offset) {
 //			MylarStatusHandler.log("null bridge for marker: " + resource.getClass(), this);
 			return null;
 		}
 
 		@Override
 		public List<String> getChildHandles(String handle) {
 			return Collections.emptyList();
 		}
 	};
 
 	public ContextCorePlugin() {
 		INSTANCE = this;
 	}
 
 	@Override
 	public void start(BundleContext context) throws Exception {
 		super.start(context);
 		contextManager = new InteractionContextManager();
 		lazyLoadExtensions();
 	}
 
 	@Override
 	public void stop(BundleContext context) throws Exception {
 		try {
 			super.stop(context);
 			INSTANCE = null;
 			for (AbstractRelationProvider provider : getRelationProviders()) {
 				provider.stopAllRunningJobs();
 			}
 		} catch (Exception e) {
 			StatusManager.fail(e, "Mylar Core stop failed", false);
 		}
 	}
 
 	private void lazyLoadExtensions() {
 		if (!extensionsLoaded) {
 			ContextStoreExtensionReader.initExtensions();
 			HandlersExtensionPointReader.initExtensions();
 			for (AbstractRelationProvider provider : getRelationProviders()) {
 				getContextManager().addListener(provider);
 			}
 			extensionsLoaded = true;
 		}
 	}
 
 	private void addRelationProvider(String contentType, AbstractRelationProvider provider) {
 		Set<AbstractRelationProvider> providers = relationProviders.get(contentType);
 		if (providers == null) {
 			providers = new HashSet<AbstractRelationProvider>();
 			relationProviders.put(contentType, providers);
 		}
 		providers.add(provider);
 	}
 
 	/**
 	 * @return all relation providers
 	 */
 	public Set<AbstractRelationProvider> getRelationProviders() {
 		Set<AbstractRelationProvider> allProviders = new HashSet<AbstractRelationProvider>();
 		for (Set<AbstractRelationProvider> providers : relationProviders.values()) {
 			allProviders.addAll(providers);
 		}
 		return allProviders;
 	}
 
 	public Set<AbstractRelationProvider> getRelationProviders(String contentType) {
 		return relationProviders.get(contentType);
 	}
 
 	public static ContextCorePlugin getDefault() {
 		return INSTANCE;
 	}
 
 	public static InteractionContextManager getContextManager() {
 		return contextManager;
 	}
 
 	public Map<String, AbstractContextStructureBridge> getStructureBridges() {
 		BridgesExtensionPointReader.initExtensions();
 		return bridges;
 	}
 
 	public AbstractContextStructureBridge getStructureBridge(String contentType) {
 		BridgesExtensionPointReader.initExtensions();
 		if (contentType != null) {
 			AbstractContextStructureBridge bridge = bridges.get(contentType);
 			if (bridge != null) {
 				return bridge;
 			}
 		}
 		return (defaultBridge == null) ? DEFAULT_BRIDGE : defaultBridge;
 	}
 
 	public Set<String> getKnownContentTypes() {
 		BridgesExtensionPointReader.initExtensions();
 		return bridges.keySet();
 	}
 
 	/**
 	 * TODO: cache this to improve performance?
 	 * 
 	 * @return null if there are no bridges loaded, null bridge otherwise
 	 */
 	public AbstractContextStructureBridge getStructureBridge(Object object) {
 		BridgesExtensionPointReader.initExtensions();
 		for (AbstractContextStructureBridge structureBridge : bridges.values()) {
 			if (structureBridge.acceptsObject(object)) {
 				return structureBridge;
 			}
 		}
 
 		// use the default if not found
 		return (defaultBridge != null && defaultBridge.acceptsObject(object)) ? defaultBridge : DEFAULT_BRIDGE;
 	}
 
 	/**
 	 * Recommended bridge registration is via extension point, but bridges can also be added at runtime. Note that only
 	 * one bridge per content type is supported. Overriding content types is not supported.
 	 */
 	public synchronized void addStructureBridge(AbstractContextStructureBridge bridge) {
 		if (bridge.getContentType().equals(CONTENT_TYPE_RESOURCE)) {
 			defaultBridge = bridge;
 		} else {
 			bridges.put(bridge.getContentType(), bridge);
 		}
 		if (bridge.getParentContentType() != null) {
 			List<String> childContentTypes = childContentTypeMap.get(bridge.getParentContentType());
 			if (childContentTypes == null) {
 				// CopyOnWriteArrayList handles concurrent access to the content types
 				childContentTypes = new CopyOnWriteArrayList<String>();
 			} 	
 			
 			childContentTypes.add(bridge.getContentType());
 			childContentTypeMap.put(bridge.getParentContentType(), childContentTypes);
 		}
 	}
 
 	public AbstractContextStore getContextStore() {
 		return contextStore;
 	}
 
 	public static void setContextStore(AbstractContextStore contextStore) {
 		ContextCorePlugin.contextStore = contextStore;
 	}
 
 	static class ContextStoreExtensionReader {
 
 		private static final String ELEMENT_CONTEXT_STORE = "contextStore";
 
 		private static boolean extensionsRead = false;
 
 		public static void initExtensions() {
 			if (!extensionsRead) {
 				IExtensionRegistry registry = Platform.getExtensionRegistry();
 				IExtensionPoint extensionPoint = registry.getExtensionPoint(BridgesExtensionPointReader.EXTENSION_ID_CONTEXT);
 				IExtension[] extensions = extensionPoint.getExtensions();
 				for (int i = 0; i < extensions.length; i++) {
 					IConfigurationElement[] elements = extensions[i].getConfigurationElements();
 					for (int j = 0; j < elements.length; j++) {
 						if (elements[j].getName().compareTo(ELEMENT_CONTEXT_STORE) == 0) {
 							readStore(elements[j]);
 						}
 					}
 				}
 				extensionsRead = true;
 			}
 		}
 
 		private static void readStore(IConfigurationElement element) {
 			// Currently disabled
 //			try {
 //				Object object = element.createExecutableExtension(BridgesExtensionPointReader.ELEMENT_CLASS);
 //
 //				if (!(object instanceof AbstractContextStore)) {
 //					MylarStatusHandler.log("Could not load bridge: " + object.getClass().getCanonicalName()
 //							+ " must implement " + AbstractContextStructureBridge.class.getCanonicalName(), null);
 //					return;
 //				} else {
 //					contextStore = (AbstractContextStore) object;
 //				}
 //			} catch (CoreException e) {
 //				MylarStatusHandler.log(e, "Could not load bridge extension");
 //			}
 		}
 	}
 
 	static class BridgesExtensionPointReader {
 
 		private static final String EXTENSION_ID_CONTEXT = "org.eclipse.mylyn.context.core.bridges";
 
 		private static final String ELEMENT_STRUCTURE_BRIDGE = "structureBridge";
 
 		private static final String ELEMENT_RELATION_PROVIDER = "relationProvider";
 
 		private static final String ATTR_CLASS = "class";
 
 		private static final String ATTR_CONTENT_TYPE = "contentType";
 
 		private static final String ATTR_PARENT_CONTENT_TYPE = "parentContentType";
 
 		private static boolean extensionsRead = false;
 
 		public static void initExtensions() {
 			if (!extensionsRead) {
 				IExtensionRegistry registry = Platform.getExtensionRegistry();
 				IExtensionPoint extensionPoint = registry.getExtensionPoint(BridgesExtensionPointReader.EXTENSION_ID_CONTEXT);
 				IExtension[] extensions = extensionPoint.getExtensions();
 				for (int i = 0; i < extensions.length; i++) {
 					IConfigurationElement[] elements = extensions[i].getConfigurationElements();
 					for (int j = 0; j < elements.length; j++) {
 						if (elements[j].getName().compareTo(BridgesExtensionPointReader.ELEMENT_STRUCTURE_BRIDGE) == 0) {
 							readBridge(elements[j]);
 						} else if (elements[j].getName().compareTo(
 								BridgesExtensionPointReader.ELEMENT_RELATION_PROVIDER) == 0) {
 							readRelationProvider(elements[j]);
 						}
 					}
 				}
 				extensionsRead = true;
 			}
 		}
 
 		@SuppressWarnings("deprecation")
 		private static void readBridge(IConfigurationElement element) {
 			try {
 				Object object = element.createExecutableExtension(BridgesExtensionPointReader.ATTR_CLASS);
 				if (!(object instanceof AbstractContextStructureBridge)) {
 					StatusManager.log("Could not load bridge: " + object.getClass().getCanonicalName()
 							+ " must implement " + AbstractContextStructureBridge.class.getCanonicalName(), null);
 					return;
 				}
 
 				AbstractContextStructureBridge bridge = (AbstractContextStructureBridge) object;
 				if (element.getAttribute(BridgesExtensionPointReader.ATTR_PARENT_CONTENT_TYPE) != null) {
 					String parentContentType = element.getAttribute(BridgesExtensionPointReader.ATTR_PARENT_CONTENT_TYPE);
 					if (parentContentType instanceof String) {
 						bridge.setParentContentType(parentContentType);
 					}
 				}
 				ContextCorePlugin.getDefault().addStructureBridge(bridge);
 			} catch (CoreException e) {
 				StatusManager.log(e, "Could not load bridge extension");
 			}
 		}
 
 		@SuppressWarnings("deprecation")
 		private static void readRelationProvider(IConfigurationElement element) {
 			try {
 				String contentType = element.getAttribute(BridgesExtensionPointReader.ATTR_CONTENT_TYPE);
 				AbstractRelationProvider relationProvider = (AbstractRelationProvider) element.createExecutableExtension(BridgesExtensionPointReader.ATTR_CLASS);
 				if (contentType != null) {
 					ContextCorePlugin.getDefault().addRelationProvider(contentType, relationProvider);
 				}
 			} catch (Exception e) {
 				StatusManager.log(e, "Could not load relation provider");
 			}
 		}
 	}
 
 	static class HandlersExtensionPointReader {
 
 		private static final String EXTENSION_ID_HANDLERS = "org.eclipse.mylyn.context.core.handlers";
 
 		private static final String ELEMENT_STATUS = "status";
 
 		private static final String ELEMENT_CLASS = "class";
 
 		private static boolean extensionsRead = false;
 
 		public static void initExtensions() {
 			if (!extensionsRead) {
 				IExtensionRegistry registry = Platform.getExtensionRegistry();
 				IExtensionPoint extensionPoint = registry.getExtensionPoint(EXTENSION_ID_HANDLERS);
 				IExtension[] extensions = extensionPoint.getExtensions();
 				for (int i = 0; i < extensions.length; i++) {
 					IConfigurationElement[] elements = extensions[i].getConfigurationElements();
 					for (int j = 0; j < elements.length; j++) {
 						if (elements[j].getName().compareTo(ELEMENT_STATUS) == 0) {
 							readHandler(elements[j]);
 						}
 					}
 				}
 				extensionsRead = true;
 			}
 		}
 
 		@SuppressWarnings("deprecation")
 		private static void readHandler(IConfigurationElement element) {
 			try {
 				Object object = element.createExecutableExtension(ELEMENT_CLASS);
 				if (!(object instanceof IStatusHandler)) {
 					StatusManager.log("Could not load handler: " + object.getClass().getCanonicalName()
 							+ " must implement " + AbstractContextStructureBridge.class.getCanonicalName(), null);
 					return;
 				}
 
 				IStatusHandler handler = (IStatusHandler) object;
 				StatusManager.addStatusHandler(handler);
 			} catch (CoreException e) {
 			}
 		}
 	}
 
 	public List<String> getChildContentTypes(String contentType) {
 		List<String> contentTypes = childContentTypeMap.get(contentType);
 		if (contentTypes != null) {
 			return contentTypes;
 		} else {
 			return Collections.emptyList();
 		}
 	}

	/**
	 * TODO: Move to utility class
	 * 
	 * @param text
	 * @return string with all non valid characters removed, if text is null return null
	 */
	public static String cleanXmlString(String text) {
		if (text == null)
			return null;
		StringBuilder builder = new StringBuilder(text.length());
		for (int x = 0; x < text.length(); x++) {
			char ch = text.charAt(x);
			// http://www.w3.org/TR/REC-xml/#charsets
			// Valid unicode characters for xml 1.0
			if ((0x0A == ch || 0x0D == ch || 0x09 == ch) || (ch >= 0x20 && ch <= 0xD7FF)
					|| (ch >= 0xE000 && ch <= 0xFFFD) || (ch >= 0x10000 && ch <= 0x10FFFF)) {
				builder.append(ch);
			}
		}
		return builder.toString();
	}
 }
