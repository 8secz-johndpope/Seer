 /*
  * The contents of this file are subject to the terms
  * of the Common Development and Distribution License
  * (the License). You may not use this file except in
  * compliance with the License.
  *
  * You can obtain a copy of the License at
  * https://javaserverfaces.dev.java.net/CDDL.html or
  * legal/CDDLv1.0.txt.
  * See the License for the specific language governing
  * permission and limitations under the License.
  *
  * When distributing Covered Code, include this CDDL
  * Header Notice in each file and include the License file
  * at legal/CDDLv1.0.txt.
  * If applicable, add the following below the CDDL Header,
  * with the fields enclosed by brackets [] replaced by
  * your own identifying information:
  * "Portions Copyrighted [year] [name of copyright owner]"
  *
  * [Name of File] [ver.__] [Date]
  *
  * Copyright 2006 Sun Microsystems Inc. All Rights Reserved
  */
 
 package com.sun.faces.application;
 
 import javax.faces.FacesException;
 import javax.faces.application.StateManager;
 import javax.faces.component.NamingContainer;
 import javax.faces.component.UIComponent;
 import javax.faces.component.UIViewRoot;
 import javax.faces.context.ExternalContext;
 import javax.faces.context.FacesContext;
 import javax.faces.render.ResponseStateManager;
 
 import java.io.Externalizable;
 import java.io.IOException;
 import java.io.ObjectInput;
 import java.io.ObjectOutput;
 import java.util.ArrayList;
 import java.util.HashMap;
 import java.util.HashSet;
 import java.util.Iterator;
 import java.util.List;
 import java.util.Map;
 import java.util.Map.Entry;
 import java.util.Set;
 import java.util.concurrent.ConcurrentHashMap;
 import java.util.logging.Level;
 import java.util.logging.Logger;
 
 import com.sun.faces.RIConstants;
 import com.sun.faces.io.FastStringWriter;
 import com.sun.faces.config.WebConfiguration;
 import com.sun.faces.config.WebConfiguration.WebContextInitParameter;
 import com.sun.faces.renderkit.RenderKitUtils;
 import com.sun.faces.util.LRUMap;
 import com.sun.faces.util.MessageUtils;
 import com.sun.faces.util.Util;
 import com.sun.faces.util.DebugUtil;
 
 public class StateManagerImpl extends StateManager {
 
     private static final Logger LOGGER =
               Util.getLogger(Util.FACES_LOGGER + Util.APPLICATION_LOGGER);
 
     private HashMap<String, Boolean> responseStateManagerInfo = null;
     
     private char requestIdSerial = 0;
 
     /** Number of views in logical view to be saved in session. */
     private int noOfViews = 0;
     private int noOfViewsInLogicalView = 0;
     private Map<String,Class<?>> classMap = 
           new ConcurrentHashMap<String,Class<?>>(32);
 
 
 
     // ---------------------------------------------------------- Public Methods
 
     @SuppressWarnings("Deprecation")
     public UIViewRoot restoreView(FacesContext context, String viewId,
                                   String renderKitId) {
 
         UIViewRoot viewRoot = null;
         if (isSavingStateInClient(context)) {
             viewRoot = restoreTree(context, viewId, renderKitId);
 
             if (viewRoot != null) {
                 restoreState(context, viewRoot, renderKitId);
             }
         } else {
             // restore tree from session.
             // The ResponseStateManager implementation may be using the new 
             // methods or deprecated methods.  We need to know which one 
             // to call.
             Object id;
             ResponseStateManager rsm =
                   RenderKitUtils.getResponseStateManager(context, renderKitId);
             if (hasDeclaredMethod(renderKitId,
                                   rsm,
                                   "getState")) {
                 Object[] stateArray = (Object[]) rsm.getState(context, viewId);
                 id = stateArray[0];
             } else {
                 id = rsm.getTreeStructureToRestore(context, viewId);
             }
 
             if (null != id) {
                 if (LOGGER.isLoggable(Level.FINE)) {
                     LOGGER.fine("Begin restoring view in session for viewId "
                                 + viewId);
                 }
                 String idString = (String) id;
                 String idInLogicalMap;
                 String idInActualMap;
 
                 int sep = idString.indexOf(NamingContainer.SEPARATOR_CHAR);
                 assert(-1 != sep);
                 assert(sep < idString.length());
 
                 idInLogicalMap = idString.substring(0, sep);
                 idInActualMap = idString.substring(sep + 1);
 
                 ExternalContext externalCtx = context.getExternalContext();
                 Object sessionObj = externalCtx.getSession(false);
 
                 // stop evaluating if the session is not available
                 if (sessionObj == null) {
                     if (LOGGER.isLoggable(Level.FINE)) {
                         LOGGER.fine(
                               "Can't Restore Server View State, session expired for viewId: "
                               + viewId);
                     }
                     return null;
                 }
 
                 Object [] stateArray = null;
                 synchronized (sessionObj) {
                     Map logicalMap = (Map) externalCtx.getSessionMap()
                           .get(RIConstants.LOGICAL_VIEW_MAP);
                     if (logicalMap != null) {
                         Map actualMap = (Map) logicalMap.get(idInLogicalMap);
                         if (actualMap != null) {
                             Map<String,Object> requestMap = 
                                   context.getExternalContext().getRequestMap();
                             requestMap.put(RIConstants.LOGICAL_VIEW_MAP,
                                            idInLogicalMap);
                             if (rsm.isPostback(context)) {
                                 requestMap.put(RIConstants.ACTUAL_VIEW_MAP,
                                                idInActualMap);
                             }
                             stateArray =
                                   (Object[]) actualMap.get(idInActualMap);
                         }
                     }
                 }
                 if (stateArray == null) {
                     if (LOGGER.isLoggable(Level.FINE)) {
                         LOGGER.fine(
                               "Session Available, but View State does not exist for viewId: "
                               + viewId);
                     }
                     return null;
                 }
 
                // We need to clone the tree, otherwise we run the risk
                // of being left in a state where the restored
                // UIComponent instances are in the session instead
                // of the TreeNode instances.  This is a problem 
                // for servers that persist session data since 
                // UIComponent instances are not serializable.
                viewRoot = restoreTree(((Object[]) stateArray[0]).clone());
                 viewRoot.processRestoreState(context, stateArray[1]);
 
                 if (LOGGER.isLoggable(Level.FINE)) {
                     LOGGER.fine("End restoring view in session for viewId "
                                 + viewId);
                 }
             }
         }
 
         return viewRoot;
     }
 
 
     @SuppressWarnings("Deprecation")
     @Override
     public SerializedView saveSerializedView(FacesContext context) {
 
 
         SerializedView result = null;
        
         // irrespective of method to save the tree, if the root is transient
         // no state information needs to  be persisted.
         UIViewRoot viewRoot = context.getViewRoot();
         if (viewRoot.isTransient()) {
             return result;
         }
 
         // honor the requirement to check for id uniqueness
         checkIdUniqueness(context, viewRoot, new HashSet<String>());
 
 
         if (LOGGER.isLoggable(Level.FINE)) {
             LOGGER.fine("Begin creating serialized view for "
                         + viewRoot.getViewId());
         }
         List<TreeNode> treeList = new ArrayList<TreeNode>(32);
         captureChild(treeList, 0, viewRoot);
         Object state = viewRoot.processSaveState(context);
         Object[] tree = treeList.toArray();      
         
         if (LOGGER.isLoggable(Level.FINE)) {
             LOGGER.fine("End creating serialized view " + viewRoot.getViewId());
         }
         if (!isSavingStateInClient(context)) {
             //
             // Server Side state saving is handled stored in two nested LRU maps
             // in the session.
             //
             // The first map is called the LOGICAL_VIEW_MAP.  A logical view
             // is a top level view that may have one or more actual views inside
             // of it.  This will be the case when you have a frameset, or an
             // application that has multiple windows operating at the same time.
             // The LOGICAL_VIEW_MAP map contains 
             // an entry for each logical view, up to the limit specified by the
             // numberOfViewsParameter.  Each entry in the LOGICAL_VIEW_MAP
             // is an LRU Map, configured with the numberOfViewsInLogicalView
             // parameter.  
             //
             // The motivation for this is to allow better memory tuning for 
             // apps that need this multi-window behavior.                                                     
             int logicalMapSize = getNumberOfViewsParameter(context);
             int actualMapSize = getNumberOfViewsInLogicalViewParameter(context);
         
             ExternalContext externalContext = context.getExternalContext();
             Object sessionObj = externalContext.getSession(true);
             Map<String, Object> sessionMap = Util.getSessionMap(context);
 
 
             synchronized (sessionObj) {
                 LRUMap<String, LRUMap<String, Object[]>> logicalMap =
                       (LRUMap<String, LRUMap<String, Object[]>>) sessionMap
                             .get(RIConstants.LOGICAL_VIEW_MAP);
                 if (logicalMap == null) {
                     logicalMap = new LRUMap<String, LRUMap<String, Object[]>>(
                           logicalMapSize);
                     sessionMap.put(RIConstants.LOGICAL_VIEW_MAP, logicalMap);
                 }
 
                 Map<String, Object> requestMap =
                       externalContext.getRequestMap();
                 String idInLogicalMap = (String)
                       requestMap.get(RIConstants.LOGICAL_VIEW_MAP);
                 if (idInLogicalMap == null) {
                     idInLogicalMap = createUniqueRequestId();
                 }
                 assert(null != idInLogicalMap);
 
                 // this value will not be null if this is a post
                 // back
                 String idInActualMap = (String)
                       requestMap.get(RIConstants.ACTUAL_VIEW_MAP);
                 if (idInActualMap == null) {
                     idInActualMap = createUniqueRequestId();
                 }
                 LRUMap<String, Object[]> actualMap =
                       logicalMap.get(idInLogicalMap);
                 if (actualMap == null) {
                     actualMap = new LRUMap<String, Object[]>(actualMapSize);
                     logicalMap.put(idInLogicalMap, actualMap);
                 }
 
                 String id = idInLogicalMap + NamingContainer.SEPARATOR_CHAR +
                             idInActualMap;
                 result = new SerializedView(id, null);                
                 Object[] stateArray = actualMap.get(idInActualMap);
                 // reuse the array if possible
                 if (stateArray != null) {                    
                     stateArray[0] = tree;
                     stateArray[1] = state;
                 } else {
                     actualMap.put(idInActualMap, new Object[] { tree, state });
                 }                
             }
         } else {
             result = new SerializedView(tree, state);
         }
 
         return result;           
 
     }
     
 
     @SuppressWarnings("Deprecation")
     @Override
     public void writeState(FacesContext context, SerializedView state)
           throws IOException {
 
         String renderKitId = context.getViewRoot().getRenderKitId();
         ResponseStateManager rsm =
               RenderKitUtils.getResponseStateManager(context, renderKitId);
         if (hasDeclaredMethod(renderKitId,
                               rsm,
                               "getState")) {
             Object[] stateArray = new Object[2];
             stateArray[0] = state.getStructure();
             stateArray[1] = state.getState();
             rsm.writeState(context, stateArray);
         } else {
             rsm.writeState(context, state);
         }
 
     }    
 
 
     // ------------------------------------------------------- Protected Methods
 
 
     protected void checkIdUniqueness(FacesContext context,
                                      UIComponent component,
                                      Set<String> componentIds)
           throws IllegalStateException {
 
         // deal with children/facets that are marked transient.        
         for (Iterator<UIComponent> kids = component.getFacetsAndChildren();
              kids.hasNext();) {
 
             UIComponent kid = kids.next();
             // check for id uniqueness
             String id = kid.getClientId(context);
             if (componentIds.add(id)) {
                 checkIdUniqueness(context, kid, componentIds);
             } else {
                 if (LOGGER.isLoggable(Level.SEVERE)) {
                     LOGGER.log(Level.SEVERE,
                                "jsf.duplicate_component_id_error",
                                id);
                 }
                 FastStringWriter writer = new FastStringWriter(128);
                 DebugUtil.simplePrintTree(context.getViewRoot(), id, writer);
                 String message = MessageUtils.getExceptionMessageString(
                             MessageUtils.DUPLICATE_COMPONENT_ID_ERROR_ID, id) 
                       + '\n'
                       + writer.toString();
                 throw new IllegalStateException(message);
             }
         }
 
     }
 
 
     // --------------------------------------------------------- Private Methods
 
     /**
          * Returns the value of ServletContextInitParameter that specifies the
          * maximum number of views to be saved in this logical view. If none is specified
          * returns <code>DEFAULT_NUMBER_OF_VIEWS_IN_LOGICAL_VIEW_IN_SESSION</code>.
          * @param context the FacesContext
          * @return number of logical views
          */
         protected int getNumberOfViewsInLogicalViewParameter(FacesContext context) {
 
             if (noOfViewsInLogicalView != 0) {
                 return noOfViewsInLogicalView;
             }
             WebConfiguration webConfig = 
                   WebConfiguration.getInstance(context.getExternalContext());
             String noOfViewsStr = webConfig
                   .getContextInitParameter(WebContextInitParameter.NumberOfLogicalViews);
             String defaultValue =
                   WebContextInitParameter.NumberOfLogicalViews.getDefaultValue();
             try {
                 noOfViewsInLogicalView = Integer.valueOf(noOfViewsStr);
             } catch (NumberFormatException nfe) {
                 if (LOGGER.isLoggable(Level.FINE)) {
                         LOGGER.fine("Error parsing the servetInitParameter "
                                     +
                                     WebContextInitParameter.NumberOfLogicalViews.getQualifiedName() 
                                     + ". Using default "
                                     +
                                     noOfViewsInLogicalView);
                 }
                 try {
                     noOfViewsInLogicalView = Integer.valueOf(defaultValue);
                 } catch (NumberFormatException ne) {
                     // won't occur
                 }
             }        
        
             return noOfViewsInLogicalView;
 
         }
 
 
         /**
          * Returns the value of ServletContextInitParameter that specifies the
          * maximum number of logical views to be saved in session. If none is specified
          * returns <code>DEFAULT_NUMBER_OF_VIEWS_IN_SESSION</code>.
          * @param context the FacesContext
          * @return number of logical views
          */
         protected int getNumberOfViewsParameter(FacesContext context) {
         
             if (noOfViews != 0) {
                 return noOfViews;
             }
             WebConfiguration webConfig = 
                   WebConfiguration.getInstance(context.getExternalContext());
             String noOfViewsStr = webConfig
                   .getContextInitParameter(WebContextInitParameter.NumberOfViews);
             String defaultValue =
                   WebContextInitParameter.NumberOfViews.getDefaultValue();
             try {
                 noOfViews = Integer.valueOf(noOfViewsStr);
             } catch (NumberFormatException nfe) {
                 if (LOGGER.isLoggable(Level.FINE)) {
                         LOGGER.fine("Error parsing the servetInitParameter "
                                     +
                                     WebContextInitParameter.NumberOfViews.getQualifiedName() 
                                     + ". Using default "
                                     +
                                     noOfViews);
                 }
                 try {
                     noOfViews = Integer.valueOf(defaultValue);
                 } catch (NumberFormatException ne) {
                     // won't occur
                 }
             }        
        
             return noOfViews;        
 
         }
     
     
     
     private static void captureChild(List<TreeNode> tree, int parent,
                                      UIComponent c) {
 
         if (!c.isTransient()) {
             TreeNode n = new TreeNode(parent, c);
             int pos = tree.size();
             tree.add(n);
             captureRest(tree, pos, c);
         }
 
     }
 
 
     private static void captureFacet(List<TreeNode> tree, int parent, String name,
                                      UIComponent c) {
 
         if (!c.isTransient()) {
             FacetNode n = new FacetNode(parent, name, c);
             int pos = tree.size();
             tree.add(n);
             captureRest(tree, pos, c);
         }
 
     }
 
 
     private static void captureRest(List<TreeNode> tree, int pos, UIComponent c) {
 
         // store children
         int sz = c.getChildCount();
         if (sz > 0) {
             List<UIComponent> child = c.getChildren();
             for (int i = 0; i < sz; i++) {
                 captureChild(tree, pos, child.get(i));
             }
         }
 
         // store facets
         sz = c.getFacetCount();
         if (sz > 0) {
             for (Entry<String, UIComponent> entry : c.getFacets().entrySet()) {
                 captureFacet(tree,
                              pos,
                              entry.getKey(),
                              entry.getValue());
             }
         }
 
     }
 
 
     /**
      * Looks for the presence of a declared method (by name) in the specified 
      * class and returns a <code>boolean</code> outcome (true, if the method 
      * exists).
      *
      * @param renderKitId  The object that will be used for the lookup.  This key 
      *  will also be stored in the <code>Map</code> with a corresponding 
      *  <code>Boolean</code> value indicating the result of the search.
      * @param instance The instance of the class that will be used as 
      *  the search domain.
      * @param methodName The name of the method we are looking for.
      * 
      * @return <code>true</code> if the method exists, otherwise 
      *  <code>false</code>
      */
     private boolean hasDeclaredMethod(String renderKitId,
                                       ResponseStateManager instance,
                                       String methodName) {
 
         boolean result;
         if (responseStateManagerInfo == null) {
             responseStateManagerInfo = new HashMap<String, Boolean>();
         }
         Boolean value = responseStateManagerInfo.get(renderKitId);
         if (value != null) {
             return value;
         }
         result = Util.hasDeclaredMethod(instance, methodName);
         responseStateManagerInfo.put(renderKitId, result);
         return result;
 
     }
 
 
     private UIComponent newInstance(TreeNode n) throws FacesException {
 
         try {
             Class<?> t = classMap.get(n.componentType);
             if (t == null) {
                 t = Util.loadClass(n.componentType, n);
                 if (t != null) {
                     classMap.put(n.componentType, t);
                 } else {
                     throw new NullPointerException();
                 }
             }
             
             UIComponent c = (UIComponent) t.newInstance();
             c.setId(n.id);
             return c;
         } catch (Exception e) {
             throw new FacesException(e);
         }
 
     }
 
     @SuppressWarnings("Deprecation")
     private void restoreState(FacesContext context,
                               UIViewRoot root,
                               String renderKitId) {
         ResponseStateManager rsm =
               RenderKitUtils.getResponseStateManager(context, renderKitId);
         Object state;
         if (hasDeclaredMethod(renderKitId,
                               rsm,
                               "getState")) {
             Object[] stateArray =
                   (Object[]) rsm.getState(context, root.getViewId());
             state = stateArray[1];
         } else {
             state = rsm.getComponentStateToRestore(context);
         }
         root.processRestoreState(context, state);
     }
 
 
     @SuppressWarnings("Deprecation")
     private UIViewRoot restoreTree(FacesContext context,
                                    String viewId,
                                    String renderKitId) {
 
         ResponseStateManager rsm =
               RenderKitUtils.getResponseStateManager(context, renderKitId);
         Object[] treeStructure;
          if (hasDeclaredMethod(renderKitId,
                                rsm,
                                "getState")) {
 
             Object[] stateArray = (Object[]) rsm.getState(context, viewId);
             treeStructure = (Object[]) stateArray[0];
         } else {
            treeStructure = (Object[]) rsm
                   .getTreeStructureToRestore(context, viewId);
         }
 
         if (treeStructure == null) {
             return null;
         }
 
         return restoreTree(treeStructure);
     }
     
      private String createUniqueRequestId() {
 
         if (requestIdSerial++ == Character.MAX_VALUE) {
             requestIdSerial = 0;
         }
         return UIViewRoot.UNIQUE_ID_PREFIX + ((int) requestIdSerial);
 
     }
 
 
     private UIViewRoot restoreTree(Object[] tree) throws FacesException {
 
         UIComponent c;
         FacetNode fn;
         TreeNode tn;
         for (int i = 0; i < tree.length; i++) {
             if (tree[i]instanceof FacetNode) {
                 fn = (FacetNode) tree[i];
                 c = newInstance(fn);
                 tree[i] = c;
                 if (i != fn.parent) {
                     ((UIComponent) tree[fn.parent]).getFacets()
                           .put(fn.facetName, c);
                 }
 
             } else {
                 tn = (TreeNode) tree[i];
                 c = newInstance(tn);
                 tree[i] = c;
                 if (i != tn.parent) {
                     ((UIComponent) tree[tn.parent]).getChildren().add(c);
                 }
             }
         }
         return (UIViewRoot) tree[0];
 
     }
 
 
     private static class TreeNode implements Externalizable {
 
 
         public String componentType;
         public String id;
 
         public int parent;
 
         private static final long serialVersionUID = -835775352718473281L;
 
 
     // ------------------------------------------------------------ Constructors
 
 
         public TreeNode() { }
 
 
         public TreeNode(int parent, UIComponent c) {
 
             this.parent = parent;
             this.id = c.getId();
             this.componentType = c.getClass().getName();
 
         }
 
 
     // --------------------------------------------- Methods From Externalizable
 
         public void writeExternal(ObjectOutput out) throws IOException {
 
             out.writeInt(this.parent);
             out.writeUTF(this.componentType);
             if (this.id != null) {
                 out.writeUTF(this.id);
             }
 
         }
 
 
         public void readExternal(ObjectInput in)
               throws IOException, ClassNotFoundException {
 
             this.parent = in.readInt();
             this.componentType = in.readUTF();
             if (in.available() > 0) {
                 this.id = in.readUTF();
             }
 
         }
 
     }
 
     private static final class FacetNode extends TreeNode {
 
 
         public String facetName;
 
         private static final long serialVersionUID = -3777170310958005106L;
 
 
     // ------------------------------------------------------------ Constructors
         
         public FacetNode() { }
 
         public FacetNode(int parent, String name, UIComponent c) {
 
             super(parent, c);
             this.facetName = name;
 
         }
 
 
     // ---------------------------------------------------------- Public Methods
 
         @Override
         public void readExternal(ObjectInput in)
               throws IOException, ClassNotFoundException {
 
             super.readExternal(in);
             this.facetName = in.readUTF();
 
         }
 
         @Override
         public void writeExternal(ObjectOutput out) throws IOException {
 
             super.writeExternal(out);
             out.writeUTF(this.facetName);
 
         }
 
     }
 
 } // END StateManagerImpl
