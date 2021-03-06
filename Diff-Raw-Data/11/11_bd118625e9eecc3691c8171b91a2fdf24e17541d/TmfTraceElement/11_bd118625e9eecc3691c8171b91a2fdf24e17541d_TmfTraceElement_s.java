 /*******************************************************************************
  * Copyright (c) 2010, 2011 Ericsson
  *
  * All rights reserved. This program and the accompanying materials are
  * made available under the terms of the Eclipse Public License v1.0 which
  * accompanies this distribution, and is available at
  * http://www.eclipse.org/legal/epl-v10.html
  *
  * Contributors:
  *   Francois Chouinard - Initial API and implementation
  *******************************************************************************/
 
 package org.eclipse.linuxtools.tmf.ui.project.model;
 
 import java.util.Arrays;
 import java.util.HashMap;
 import java.util.Map;
 
 import org.eclipse.core.resources.IFolder;
 import org.eclipse.core.resources.IResource;
 import org.eclipse.core.runtime.CoreException;
 import org.eclipse.core.runtime.IConfigurationElement;
 import org.eclipse.core.runtime.NullProgressMonitor;
 import org.eclipse.core.runtime.Platform;
 import org.eclipse.linuxtools.internal.tmf.ui.TmfUiPlugin;
 import org.eclipse.linuxtools.internal.tmf.ui.parsers.custom.CustomTxtEvent;
 import org.eclipse.linuxtools.internal.tmf.ui.parsers.custom.CustomTxtTrace;
 import org.eclipse.linuxtools.internal.tmf.ui.parsers.custom.CustomTxtTraceDefinition;
 import org.eclipse.linuxtools.internal.tmf.ui.parsers.custom.CustomXmlEvent;
 import org.eclipse.linuxtools.internal.tmf.ui.parsers.custom.CustomXmlTrace;
 import org.eclipse.linuxtools.internal.tmf.ui.parsers.custom.CustomXmlTraceDefinition;
 import org.eclipse.linuxtools.tmf.core.TmfCommonConstants;
 import org.eclipse.linuxtools.tmf.core.event.ITmfEvent;
 import org.eclipse.linuxtools.tmf.core.trace.ITmfTrace;
 import org.eclipse.linuxtools.tmf.ui.editors.TmfEventsEditor;
 import org.eclipse.ui.IActionFilter;
 import org.eclipse.ui.views.properties.IPropertyDescriptor;
 import org.eclipse.ui.views.properties.IPropertySource2;
 import org.eclipse.ui.views.properties.TextPropertyDescriptor;
 
 /**
  * <b><u>TmfTraceElement</u></b>
  * <p>
  */
 public class TmfTraceElement extends TmfProjectModelElement implements IActionFilter, IPropertySource2 {
 
     // ------------------------------------------------------------------------
     // Constants
     // ------------------------------------------------------------------------
 
     // Other attributes
     public static final String BUNDLE = "bundle"; //$NON-NLS-1$
     public static final String IS_LINKED = "isLinked"; //$NON-NLS-1$
 
     // Property View stuff
     private static final String sfInfoCategory = "Info"; //$NON-NLS-1$
     private static final String sfName = "name"; //$NON-NLS-1$
     private static final String sfPath = "path"; //$NON-NLS-1$
     private static final String sfLocation = "location"; //$NON-NLS-1$
     private static final String sfEventType = "type"; //$NON-NLS-1$
     private static final String sfIsLinked = "linked"; //$NON-NLS-1$
 
     private static final TextPropertyDescriptor sfNameDescriptor = new TextPropertyDescriptor(sfName, sfName);
     private static final TextPropertyDescriptor sfPathDescriptor = new TextPropertyDescriptor(sfPath, sfPath);
     private static final TextPropertyDescriptor sfLocationDescriptor = new TextPropertyDescriptor(sfLocation, sfLocation);
     private static final TextPropertyDescriptor sfTypeDescriptor = new TextPropertyDescriptor(sfEventType, sfEventType);
     private static final TextPropertyDescriptor sfIsLinkedDescriptor = new TextPropertyDescriptor(sfIsLinked, sfIsLinked);
 
     private static final IPropertyDescriptor[] sfDescriptors = { sfNameDescriptor, sfPathDescriptor, sfLocationDescriptor,
             sfTypeDescriptor, sfIsLinkedDescriptor };
 
     static {
         sfNameDescriptor.setCategory(sfInfoCategory);
         sfPathDescriptor.setCategory(sfInfoCategory);
         sfLocationDescriptor.setCategory(sfInfoCategory);
         sfTypeDescriptor.setCategory(sfInfoCategory);
         sfIsLinkedDescriptor.setCategory(sfInfoCategory);
     }
     
     // ------------------------------------------------------------------------
     // Attributes
     // ------------------------------------------------------------------------
 
     // This trace type ID as defined in plugin.xml
     private String fTraceTypeId = null;
 
     // ------------------------------------------------------------------------
     // Static initialization
     // ------------------------------------------------------------------------
 
     // The mapping of available trace type IDs to their corresponding configuration element
     private static final Map<String, IConfigurationElement> sfTraceTypeAttributes = new HashMap<String, IConfigurationElement>();
     private static final Map<String, IConfigurationElement> sfTraceCategories = new HashMap<String, IConfigurationElement>();
 
     // Initialize statically at startup
     public static void init() {
         IConfigurationElement[] config = Platform.getExtensionRegistry().getConfigurationElementsFor(TmfTraceType.TMF_TRACE_TYPE_ID);
         for (IConfigurationElement ce : config) {
             String elementName = ce.getName();
             if (elementName.equals(TmfTraceType.TYPE_ELEM)) {
                 String traceTypeId = ce.getAttribute(TmfTraceType.ID_ATTR);
                 sfTraceTypeAttributes.put(traceTypeId, ce);
             } else if (elementName.equals(TmfTraceType.CATEGORY_ELEM)) {
                 String categoryId = ce.getAttribute(TmfTraceType.ID_ATTR);
                 sfTraceCategories.put(categoryId, ce);
             }
         }
     }
 
     // ------------------------------------------------------------------------
     // Constructors
     // ------------------------------------------------------------------------
 
     public TmfTraceElement(String name, IResource trace, TmfTraceFolder parent) {
         this(name, trace, (TmfProjectModelElement) parent);
     }
 
     public TmfTraceElement(String name, IResource trace, TmfExperimentElement parent) {
         this(name, trace, (TmfProjectModelElement) parent);
     }
 
     private TmfTraceElement(String name, IResource trace, TmfProjectModelElement parent) {
         super(name, trace, parent);
         parent.addChild(this);
         refreshTraceType();
     }
 
     // ------------------------------------------------------------------------
     // Operations
     // ------------------------------------------------------------------------
 
     public String getTraceType() {
         return fTraceTypeId;
     }
 
     public void refreshTraceType() {
         try {
             fTraceTypeId = getResource().getPersistentProperty(TmfCommonConstants.TRACETYPE);
         } catch (CoreException e) {
             TmfUiPlugin.getDefault().logError("Error refreshing trace type pesistent property for trace " + getName(), e); //$NON-NLS-1$
         }
     }
 
     public ITmfTrace<?> instantiateTrace() {
         try {
 
             // make sure that supplementary folder exists
             refreshSupplementaryFolder();
 
             if (fTraceTypeId != null) {
                 if (fTraceTypeId.startsWith(CustomTxtTrace.class.getCanonicalName())) {
                     for (CustomTxtTraceDefinition def : CustomTxtTraceDefinition.loadAll()) {
                         if (fTraceTypeId.equals(CustomTxtTrace.class.getCanonicalName() + ":" + def.definitionName)) { //$NON-NLS-1$
                             return new CustomTxtTrace(def);
                         }
                     }
                 }
                 if (fTraceTypeId.startsWith(CustomXmlTrace.class.getCanonicalName())) {
                     for (CustomXmlTraceDefinition def : CustomXmlTraceDefinition.loadAll()) {
                         if (fTraceTypeId.equals(CustomXmlTrace.class.getCanonicalName() + ":" + def.definitionName)) { //$NON-NLS-1$
                             return new CustomXmlTrace(def);
                         }
                     }
                 }
                 IConfigurationElement ce = sfTraceTypeAttributes.get(fTraceTypeId);
                 ITmfTrace<?> trace = (ITmfTrace<?>) ce.createExecutableExtension(TmfTraceType.TRACE_TYPE_ATTR);
                 return trace;
             }
         } catch (CoreException e) {
             TmfUiPlugin.getDefault().logError("Error instantiating ITmfTrace object for trace " + getName(), e); //$NON-NLS-1$
         }
         return null;
     }
 
     public ITmfEvent instantiateEvent() {
         try {
             if (fTraceTypeId != null) {
                 if (fTraceTypeId.startsWith(CustomTxtTrace.class.getCanonicalName())) {
                     for (CustomTxtTraceDefinition def : CustomTxtTraceDefinition.loadAll()) {
                         if (fTraceTypeId.equals(CustomTxtTrace.class.getCanonicalName() + ":" + def.definitionName)) { //$NON-NLS-1$
                             return new CustomTxtEvent(def);
                         }
                     }
                 }
                 if (fTraceTypeId.startsWith(CustomXmlTrace.class.getCanonicalName())) {
                     for (CustomXmlTraceDefinition def : CustomXmlTraceDefinition.loadAll()) {
                         if (fTraceTypeId.equals(CustomXmlTrace.class.getCanonicalName() + ":" + def.definitionName)) { //$NON-NLS-1$
                             return new CustomXmlEvent(def);
                         }
                     }
                 }
                 IConfigurationElement ce = sfTraceTypeAttributes.get(fTraceTypeId);
                 ITmfEvent event = (ITmfEvent) ce.createExecutableExtension(TmfTraceType.EVENT_TYPE_ATTR);
                 return event;
             }
         } catch (CoreException e) {
             TmfUiPlugin.getDefault().logError("Error instantiating ITmfEvent object for trace " + getName(), e); //$NON-NLS-1$
         }
         return null;
     }
 
     public String getEditorId() {
         if (fTraceTypeId != null) {
             if (fTraceTypeId.startsWith(CustomTxtTrace.class.getCanonicalName())) {
                 return TmfEventsEditor.ID;
             }
             if (fTraceTypeId.startsWith(CustomXmlTrace.class.getCanonicalName())) {
                 return TmfEventsEditor.ID;
             }
             IConfigurationElement ce = sfTraceTypeAttributes.get(fTraceTypeId);
             IConfigurationElement[] defaultEditorCE = ce.getChildren(TmfTraceType.DEFAULT_EDITOR_ELEM);
             if (defaultEditorCE.length == 1) {
                 return defaultEditorCE[0].getAttribute(TmfTraceType.ID_ATTR);
             }
         }
         return null;
     }
 
     /**
      * Returns the <code>TmfTraceElement</code> located under the <code>TmfTracesFolder</code>.
      * 
      * @return <code>this</code> if this element is under the <code>TmfTracesFolder</code> 
      *         else the corresponding <code>TmfTraceElement</code> if this element is under 
      *         <code>TmfExperimentElement</code>.
      */
     public TmfTraceElement getElementUnderTraceFolder() {
 
         // If trace is under an experiment, return original trace from the traces folder
         if (getParent() instanceof TmfExperimentElement) {
             for (TmfTraceElement aTrace : getProject().getTracesFolder().getTraces()) {
                 if (aTrace.getName().equals(getName())) {
                     return aTrace;
                 }
             }
         }
         return this;
     }
     
     /**
      * Deletes the trace specific supplementary folder.
      */
     public void deleteSupplementaryFolder() {
         IFolder supplFolder = getTraceSupplementaryFolder(fResource.getName());
         if (supplFolder.exists()) {
             try {
                 supplFolder.delete(true, new NullProgressMonitor());
             } catch (CoreException e) {
                 TmfUiPlugin.getDefault().logError("Error deleting supplementary folder " + supplFolder, e); //$NON-NLS-1$
             }
         }
     }
 
     /**
      * Renames the trace specific supplementary folder according to the new trace name.
      * 
      * @param newTraceName The new trace name 
      */
     public void renameSupplementaryFolder(String newTraceName) {
         IFolder oldSupplFolder = getTraceSupplementaryFolder(fResource.getName());
         IFolder newSupplFolder =  getTraceSupplementaryFolder(newTraceName);
 
         // Rename supplementary folder
         if (oldSupplFolder.exists()) {
             try {
                 oldSupplFolder.move(newSupplFolder.getFullPath(), true, new NullProgressMonitor());
             } catch (CoreException e) {
                 TmfUiPlugin.getDefault().logError("Error renaming supplementary folder " + oldSupplFolder, e); //$NON-NLS-1$
             }
         }
     }
 
     /**
      * Copies the trace specific supplementary folder to the new trace name.
      * 
      * @param newTraceName The new trace name 
      */
     public void copySupplementaryFolder(String newTraceName) {
         IFolder oldSupplFolder = getTraceSupplementaryFolder(fResource.getName());
         IFolder newSupplFolder = getTraceSupplementaryFolder(newTraceName);
 
         // copy supplementary folder
         if (oldSupplFolder.exists()) {
             try {
                 oldSupplFolder.copy(newSupplFolder.getFullPath(), true, new NullProgressMonitor());
             } catch (CoreException e) {
                 TmfUiPlugin.getDefault().logError("Error renaming supplementary folder " + oldSupplFolder, e); //$NON-NLS-1$
             }
         }
     }
 
     /**
      * Copies the trace specific supplementary folder a new folder.
      * 
      * @param destination The destination folder to copy to. 
      */
     public void copySupplementaryFolder(IFolder destination) {
         IFolder oldSupplFolder = getTraceSupplementaryFolder(fResource.getName());
 
         // copy supplementary folder
         if (oldSupplFolder.exists()) {
             try {
                 oldSupplFolder.copy(destination.getFullPath(), true, new NullProgressMonitor());
             } catch (CoreException e) {
                TmfUiPlugin.getDefault().logError("Error renaming supplementary folder " + oldSupplFolder, e); //$NON-NLS-1$
             }
         }
     }
 
     
     /**
      * Refreshes the trace specific supplementary folder information. It creates the folder if not exists.
      * It sets the persistence property of the trace resource  
      */
     public void refreshSupplementaryFolder() {
         createSupplementaryDirectory();
     }
 
     /**
      * Checks if supplementary resource exist or not.
      *  
      * @return <code>true</code> if one or more files are under the trace supplementary folder
      */
     public boolean hasSupplementaryResources() {
         IResource[] resources = getSupplementaryResources();
         return (resources.length > 0);
     }
     
     /**
      * Returns the supplementary resources under the trace supplementary folder.
      *  
      * @return array of resources under the trace supplementary folder.
      */
     public IResource[] getSupplementaryResources() {
         IFolder supplFolder = getTraceSupplementaryFolder(fResource.getName());
         if (supplFolder.exists()) {
             try {
                 return supplFolder.members();
             } catch (CoreException e) {
                 TmfUiPlugin.getDefault().logError("Error deleting supplementary folder " + supplFolder, e); //$NON-NLS-1$
             }
         }
         return new IResource[0];
     }
 
     /**
      * Deletes the given resources.  
      * 
      * @param resources array of resources to delete.
      */
     public void deleteSupplementaryResources(IResource[] resources) {
         
         for (int i = 0; i < resources.length; i++) {
             try {
                 resources[i].delete(true, new NullProgressMonitor());
             } catch (CoreException e) {
                 TmfUiPlugin.getDefault().logError("Error deleting supplementary resource " + resources[i], e); //$NON-NLS-1$
             }
         }
     }
 
     private void createSupplementaryDirectory() {
         IFolder supplFolder = getTraceSupplementaryFolder(fResource.getName());
         if (!supplFolder.exists()) {
             try {
                 supplFolder.create(true, true, new NullProgressMonitor());
             } catch (CoreException e) {
                 TmfUiPlugin.getDefault().logError("Error creating resource supplementary file " + supplFolder, e); //$NON-NLS-1$
             }
         }
 
         try {
             fResource.setPersistentProperty(TmfCommonConstants.TRACE_SUPPLEMENTARY_FOLDER, supplFolder.getLocationURI().getPath());
         } catch (CoreException e) {
             TmfUiPlugin.getDefault().logError("Error setting persistant property " + TmfCommonConstants.TRACE_SUPPLEMENTARY_FOLDER, e); //$NON-NLS-1$
         }
         
     }
 
     // ------------------------------------------------------------------------
     // IActionFilter
     // ------------------------------------------------------------------------
 
     @Override
     public boolean testAttribute(Object target, String name, String value) {
         if (name.equals(IS_LINKED)) {
             boolean isLinked = getResource().isLinked();
             return Boolean.toString(isLinked).equals(value);
         }
         return false;
     }
 
     // ------------------------------------------------------------------------
     // TmfTraceElement
     // ------------------------------------------------------------------------
 
     @Override
     public TmfProjectElement getProject() {
         if (getParent() instanceof TmfTraceFolder) {
             TmfTraceFolder folder = (TmfTraceFolder) getParent();
             TmfProjectElement project = (TmfProjectElement) folder.getParent();
             return project;
         }
         if (getParent() instanceof TmfExperimentElement) {
             TmfExperimentElement experiment = (TmfExperimentElement) getParent();
             TmfExperimentFolder folder = (TmfExperimentFolder) experiment.getParent();
             TmfProjectElement project = (TmfProjectElement) folder.getParent();
             return project;
         }
         return null;
     }
 
     // ------------------------------------------------------------------------
     // IPropertySource2
     // ------------------------------------------------------------------------
 
     @Override
     public Object getEditableValue() {
         return null;
     }
 
     @Override
     public IPropertyDescriptor[] getPropertyDescriptors() {
         return (sfDescriptors != null) ? Arrays.copyOf(sfDescriptors, sfDescriptors.length) : null;
     }
 
     @Override
     public Object getPropertyValue(Object id) {
 
         if (sfName.equals(id)) {
             return getName();
         }
 
         if (sfPath.equals(id)) {
             return getPath().toString();
         }
 
         if (sfLocation.equals(id)) {
             return getLocation().toString();
         }
 
         if (sfIsLinked.equals(id)) {
             return Boolean.valueOf(getResource().isLinked()).toString();
         }
 
         if (sfEventType.equals(id)) {
             if (fTraceTypeId != null) {
                 IConfigurationElement ce = sfTraceTypeAttributes.get(fTraceTypeId);
                 return (ce != null) ? (getCategory(ce) + " : " + ce.getAttribute(TmfTraceType.NAME_ATTR)) : ""; //$NON-NLS-1$ //$NON-NLS-2$
             }
         }
 
         return null;
     }
 
     private String getCategory(IConfigurationElement ce) {
         String categoryId = ce.getAttribute(TmfTraceType.CATEGORY_ATTR);
         if (categoryId != null) {
             IConfigurationElement category = sfTraceCategories.get(categoryId);
             if (category != null) {
                 return category.getAttribute(TmfTraceType.NAME_ATTR);
             }
         }
         return "[no category]"; //$NON-NLS-1$
     }
 
     @Override
     public void resetPropertyValue(Object id) {
     }
 
     @Override
     public void setPropertyValue(Object id, Object value) {
     }
 
     @Override
     public boolean isPropertyResettable(Object id) {
         return false;
     }
 
     @Override
     public boolean isPropertySet(Object id) {
         return false;
     }
 
 }
