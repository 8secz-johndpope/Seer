 package nextapp.echo.extras.webcontainer.sync.component;
 
 import java.util.ArrayList;
 import java.util.Arrays;
 import java.util.Collections;
 import java.util.Comparator;
 import java.util.HashSet;
 import java.util.Iterator;
 import java.util.LinkedList;
 import java.util.List;
 import java.util.Set;
 
 import nextapp.echo.app.Component;
 import nextapp.echo.app.ImageReference;
 import nextapp.echo.app.ResourceImageReference;
 import nextapp.echo.app.serial.SerialException;
 import nextapp.echo.app.serial.SerialPropertyPeer;
 import nextapp.echo.app.update.ClientUpdateManager;
 import nextapp.echo.app.update.ServerComponentUpdate;
 import nextapp.echo.app.util.Context;
 import nextapp.echo.extras.app.Tree;
 import nextapp.echo.extras.app.event.TreeExpansionEvent;
 import nextapp.echo.extras.app.event.TreeExpansionListener;
 import nextapp.echo.extras.app.tree.TreeModel;
 import nextapp.echo.extras.app.tree.TreePath;
 import nextapp.echo.extras.app.tree.TreeSelectionModel;
 import nextapp.echo.extras.webcontainer.service.CommonService;
 import nextapp.echo.webcontainer.AbstractComponentSynchronizePeer;
 import nextapp.echo.webcontainer.RenderState;
 import nextapp.echo.webcontainer.ServerMessage;
 import nextapp.echo.webcontainer.Service;
 import nextapp.echo.webcontainer.UserInstance;
 import nextapp.echo.webcontainer.WebContainerServlet;
 import nextapp.echo.webcontainer.service.ImageService;
 import nextapp.echo.webcontainer.service.JavaScriptService;
 import nextapp.echo.webcontainer.util.MultiIterator;
 
 import org.w3c.dom.Document;
 import org.w3c.dom.Element;
 
 public class TreePeer 
 extends AbstractComponentSynchronizePeer {
 
     private static class TreeSelectionUpdate {
         boolean clear = false;
         List addedSelections = new LinkedList();
         List removedSelections = new LinkedList();
     }
     
     public static class TreeSelectionUpdatePeer 
     implements SerialPropertyPeer {
 
         public Object toProperty(Context context, Class objectClass,
                 Element propertyElement) throws SerialException {
             TreeSelectionUpdate update = new TreeSelectionUpdate();
             String cStr = propertyElement.getAttribute("c");
             update.clear = Boolean.valueOf(cStr).booleanValue();
             if (propertyElement.hasAttribute("r")) {
                 String rStr = propertyElement.getAttribute("r");
                 String[] rTokens = rStr.split(",");
                 for (int i = 0; i < rTokens.length; i++) {
                     update.removedSelections.add(Integer.valueOf(rTokens[i]));
                 }
             }
             if (propertyElement.hasAttribute("a")) {
                 String aStr = propertyElement.getAttribute("a");
                 String[] aTokens = aStr.split(",");
                 for (int i = 0; i < aTokens.length; i++) {
                     update.addedSelections.add(Integer.valueOf(aTokens[i]));
                 }
             }
             return update;
         }
 
         public void toXml(Context context, Class objectClass,
                 Element propertyElement, Object propertyValue)
                 throws SerialException {
             throw new UnsupportedOperationException();
         }
         
     }
     
     private static class TreeRenderState 
     implements RenderState {
         private Set sentPaths = new HashSet();
         private Set changedPaths = new HashSet();
         private Set unsentSelections = new HashSet();
         private TreePath clientPath;
         private boolean fullRender = true;
         private final Tree tree;
         
         private TreeExpansionListener expansionListener = new TreeExpansionListener() {
             public void treeCollapsed(TreeExpansionEvent event) {
                 if (!event.getPath().equals(clientPath)) {
                     changedPaths.add(event.getPath());
                 }
             }
             
             public void treeExpanded(TreeExpansionEvent event) {
                 if (!event.getPath().equals(clientPath)) {
                     changedPaths.add(event.getPath());
                 }
             }
         };
         
         public TreeRenderState(Tree tree) {
             this.tree = tree;
             tree.addTreeExpansionListener(expansionListener);
         }
         
         public void setClientPath(TreePath path) {
             this.clientPath = null;
             if (isSent(path)) {
                 this.clientPath = path;
             }
         }
         
         public boolean isFullRender() {
             return fullRender;
         }
         
         public void setFullRender(boolean newValue) {
             fullRender = newValue;
         }
         
         public void addSentPath(TreePath path) {
             sentPaths.add(path);
         }
         
         public void removeSentPath(TreePath path) {
             sentPaths.remove(path);
         }
         
         public boolean isSent(TreePath path) {
             return sentPaths.contains(path);
         }
         
         public Iterator changedPaths() {
             ArrayList list = new ArrayList(changedPaths);
             Collections.sort(list, new Comparator() {
                 public int compare(Object obj1, Object obj2) {
                     TreePath path1 = (TreePath) obj1;
                     TreePath path2 = (TreePath) obj2;
                     if (path1 == path2 || path1.equals(path2)) {
                         return 0;
                     }
                     int row1 = tree.getRowForPath(path1);
                     int row2 = tree.getRowForPath(path2);
                     if (row1 < row2) {
                         return -1;
                     } else if (row1 > row2) {
                         return 1;
                     } else {
                         return 0;
                     }
                 }
             });
             return list.iterator();
         }
         
         public void clearChangedPaths() {
             clientPath = null;
             changedPaths.clear();
         }
         
         public boolean hasChangedPaths() {
             return clientPath != null || !changedPaths.isEmpty();
         }
         
         public boolean isPathChanged(TreePath path) {
             return changedPaths.contains(path);
         }
         
         public boolean hasUnsentSelections() {
             return !unsentSelections.isEmpty();
         }
         
         public void addUnsentSelection(TreePath path) {
             unsentSelections.add(path);
         }
         
         public void removeUnsentSelection(TreePath path) {
             unsentSelections.remove(path);
         }
     }
     
     private class TreeStructure {
         
         Tree tree;
         
         TreeStructure(Tree tree) {
             this.tree = tree;
         }
     }
     
     public static class TreeStructurePeer 
     implements SerialPropertyPeer {
     
         /**
          * @see nextapp.echo.app.serial.SerialPropertyPeer#toProperty(nextapp.echo.app.util.Context, 
          *      java.lang.Class, org.w3c.dom.Element)
          */
         public Object toProperty(Context context, Class objectClass, Element propertyElement) 
         throws SerialException {
             throw new UnsupportedOperationException();
         }
     
         /**
          * @see nextapp.echo.app.serial.SerialPropertyPeer#toXml(nextapp.echo.app.util.Context, 
          *      java.lang.Class, org.w3c.dom.Element, java.lang.Object)
          */
         public void toXml(Context context, Class objectClass, Element propertyElement, Object propertyValue) 
         throws SerialException {
             Tree tree = ((TreeStructure) propertyValue).tree;
             TreeStructureRenderer renderer = new TreeStructureRenderer(propertyElement, tree);
             propertyElement.setAttribute("t", "ExtrasSerial.TreeStructure");
             UserInstance userInstance = (UserInstance) context.get(UserInstance.class);
             TreeRenderState renderState = (TreeRenderState) userInstance.getRenderState(tree);
             if (renderState == null) {
                 renderState = new TreeRenderState(tree);
                 userInstance.setRenderState(tree, renderState);
             }
             renderer.render(context, renderState);
             renderState.clearChangedPaths();
         }
     }
     
     private static class TreeStructureRenderer {
         private Tree tree;
         private TreeModel model;
         private int columnCount;
         private Element propertyElement;
         private Document document;
         private Set renderedPaths = new HashSet();
         private TreeRenderState renderState;
         
         TreeStructureRenderer(Element propertyElement, Tree tree) {
             this.propertyElement = propertyElement;
             document = propertyElement.getOwnerDocument();
             this.tree = tree;
             columnCount = getColumnCount(tree);
             model = tree.getModel();
         }
         
         private void render(Context context, TreeRenderState renderState) {
             this.renderState = renderState;
             if (renderState.isFullRender()) {
                 if (tree.isHeaderVisible()) {
                     // header
                     renderNode(context, null, null, false);
                 }
                 Object value = model.getRoot();
                 renderNode(context, value, new TreePath(value), true);
                 renderState.setFullRender(false);
                 propertyElement.setAttribute("fr", "1");
             } else if (renderState.hasChangedPaths()) {
                 for (Iterator iterator = renderState.changedPaths(); iterator.hasNext();) {
                     TreePath path = (TreePath) iterator.next();
                     renderNode(context, path.getLastPathComponent(), path, true);
                     renderedPaths.add(path);
                 }
             }
         }
         
         private void renderNode(Context context, Object value, TreePath path, boolean root) {
             if (renderedPaths.contains(path)) {
                 return;
             }
             if (renderState.isSent(path) && !renderState.isPathChanged(path)) {
                 return;
             }
             
             UserInstance userInstance = (UserInstance) context.get(UserInstance.class);
             
             renderedPaths.add(path);
             Component component = tree.getComponent(path, 0);
             boolean expanded = tree.isExpanded(path);
             String id = userInstance.getClientRenderId(component);
             Element eElement = document.createElement("e");
             eElement.setAttribute("i", id);
             if (path != null) {
                 TreePath parentPath = path.getParentPath();
                 if (parentPath != null) {
                     eElement.setAttribute("p", userInstance.getClientRenderId(tree.getComponent(parentPath, 0)));
                 }
             }
             boolean leaf = value != null && model.isLeaf(value);
             if (path == null) {
                 eElement.setAttribute("h", "1");
             } else {
                 if (expanded) {
                     eElement.setAttribute("ex", "1");
                 } else {
                     if (leaf) {
                         eElement.setAttribute("l", "1");
                     }
                 }
                 if (root) {
                     eElement.setAttribute("r", "1");
                 }
             }
             
             if (!renderState.isSent(path)) {
                 for (int i = 1; i < columnCount; ++i) {
                     Component columnComponent = tree.getComponent(path, i);
                     Element columnElement = document.createElement("c");
                     columnElement.setAttribute("i", userInstance.getClientRenderId(columnComponent));
                     eElement.appendChild(columnElement);
                 }
             }
             
             propertyElement.appendChild(eElement);
             
             if (value == null) {
                 return;
             }
             if (expanded) {
                 int childCount = model.getChildCount(value);
                 for (int i = 0; i < childCount; ++i) {
                     Object childValue = model.getChild(value, i);
                     renderNode(context, childValue, path.pathByAddingChild(childValue), false);
                 }
             }
             if (expanded || leaf) {
                 renderState.addSentPath(path);
             }
         }
     }
     
     private static int getColumnCount(Tree tree) {
         return tree.getColumnModel().getColumnCount();
     }
     
     private static String getSelectionString(Context context, TreeSelectionModel selectionModel, Tree tree) {
         UserInstance userInstance = (UserInstance) context.get(UserInstance.class);
         TreeRenderState renderState = (TreeRenderState) userInstance.getRenderState(tree);
         
         StringBuffer selection = new StringBuffer();
         TreePath[] paths = selectionModel.getSelectionPaths();
         for (int i = 0; i < paths.length; i++) {
             TreePath path = paths[i];
             Component component = tree.getComponent(path, 0);
             if (component == null) {
                renderState.addUnsentSelection(path);
             } else {
                 String id = userInstance.getClientRenderId(component);
                 if (renderState != null) {
                     renderState.removeUnsentSelection(path);
                 }
                 if (selection.length() > 0) {
                     selection.append(",");
                 }
                 selection.append(id);
             }
         }
         return selection.toString();
     }
     
     private static final String IMAGE_PREFIX = "/nextapp/echo/extras/webcontainer/resource/image/";
     private static final ImageReference DEFAULT_ICON_LINE_SOLID = new ResourceImageReference(IMAGE_PREFIX + "Dot.gif");
     private static final ImageReference DEFAULT_ICON_VERTICAL_LINE_DOTTED = new ResourceImageReference(IMAGE_PREFIX + "TreeVerticalLineDotted.gif");
     private static final ImageReference DEFAULT_ICON_HORIZONTAL_LINE_DOTTED = new ResourceImageReference(IMAGE_PREFIX + "TreeHorizontalLineDotted.gif");
     private static final ImageReference DEFAULT_ICON_NODE_CLOSED = new ResourceImageReference(IMAGE_PREFIX + "TreeClosed.gif");
     private static final ImageReference DEFAULT_ICON_NODE_OPEN = new ResourceImageReference(IMAGE_PREFIX + "TreeOpen.gif");
     
     private static final String IMAGE_ID_LINE_VERTICAL_SOLID = "EchoExtras.Tree.lineVerticalSolid";
     private static final String IMAGE_ID_LINE_HORIZONTAL_SOLID = "EchoExtras.Tree.lineHorizontalSolid";
     private static final String IMAGE_ID_LINE_VERTICAL_DOTTED = "EchoExtras.Tree.lineVerticalDotted";
     private static final String IMAGE_ID_LINE_HORIZONTAL_DOTTED = "EchoExtras.Tree.lineHorizontalDotted";
     private static final String IMAGE_ID_NODE_CLOSED = "EchoExtras.Tree.nodeClosed";
     private static final String IMAGE_ID_NODE_OPEN = "EchoExtras.Tree.nodeOpen";
 
     private static final String PROPERTY_TREE_STRUCTURE = "treeStructure";
     private static final String PROPERTY_COLUMN_COUNT = "columnCount";
     private static final String PROPERTY_SELECTION_MODE = "selectionMode";
     
     private static final String EXPANSION_PROPERTY = "expansion"; 
     private static final String SELECTION_PROPERTY = "selectionUpdate";
     
     private static final String[] MODEL_CHANGED_UPDATE_PROPERTIES = new String[] { PROPERTY_TREE_STRUCTURE,
             PROPERTY_COLUMN_COUNT};
     
     private static final Service TREE_SERVICE = JavaScriptService.forResources("EchoExtras.RemoteTree",  
             new String[]{ "/nextapp/echo/extras/webcontainer/resource/js/Application.RemoteTree.js",
                     "/nextapp/echo/extras/webcontainer/resource/js/Serial.RemoteTree.js",
                     "/nextapp/echo/extras/webcontainer/resource/js/Render.RemoteTree.js" });
     
     static {
         ImageService.install();
         ImageService.addGlobalImage(IMAGE_ID_LINE_HORIZONTAL_SOLID, DEFAULT_ICON_LINE_SOLID);
         ImageService.addGlobalImage(IMAGE_ID_LINE_VERTICAL_SOLID, DEFAULT_ICON_LINE_SOLID);
         ImageService.addGlobalImage(IMAGE_ID_LINE_HORIZONTAL_DOTTED, DEFAULT_ICON_HORIZONTAL_LINE_DOTTED);
         ImageService.addGlobalImage(IMAGE_ID_LINE_VERTICAL_DOTTED, DEFAULT_ICON_VERTICAL_LINE_DOTTED);
         ImageService.addGlobalImage(IMAGE_ID_NODE_CLOSED, DEFAULT_ICON_NODE_CLOSED);
         ImageService.addGlobalImage(IMAGE_ID_NODE_OPEN, DEFAULT_ICON_NODE_OPEN);
         
         WebContainerServlet.getServiceRegistry().add(TREE_SERVICE);
     }
     
     public TreePeer() {
         super();
         addOutputProperty(PROPERTY_TREE_STRUCTURE);
         addOutputProperty(PROPERTY_COLUMN_COUNT);
         addOutputProperty(PROPERTY_SELECTION_MODE);
         addOutputProperty(Tree.SELECTION_CHANGED_PROPERTY);
         
         addEvent(new AbstractComponentSynchronizePeer.EventPeer(Tree.INPUT_ACTION, Tree.ACTION_LISTENERS_CHANGED_PROPERTY));
     }
     
     /**
      * @see nextapp.echo.webcontainer.AbstractComponentSynchronizePeer#getComponentClass()
      */
     public Class getComponentClass() {
         return Tree.class;
     }
     
     /**
      * @see nextapp.echo.webcontainer.AbstractComponentSynchronizePeer#getClientComponentType()
      */
     public String getClientComponentType() {
         return "nextapp.echo.extras.app.RemoteTree";
     }
     
     /**
      * @see nextapp.echo.webcontainer.AbstractComponentSynchronizePeer#getOutputProperty(
      *      nextapp.echo.app.util.Context, nextapp.echo.app.Component, java.lang.String, int)
      */
     public Object getOutputProperty(Context context, Component component, String propertyName, int propertyIndex) {
         Tree tree = (Tree) component;
         if (PROPERTY_TREE_STRUCTURE.equals(propertyName)) {
             return new TreeStructure(tree);
         } else if (PROPERTY_COLUMN_COUNT.equals(propertyName)) {
             return new Integer(getColumnCount(tree));
         } else if (PROPERTY_SELECTION_MODE.equals(propertyName)) {
             return new Integer(tree.getSelectionModel().getSelectionMode());
         } else if (Tree.SELECTION_CHANGED_PROPERTY.equals(propertyName)) {
             return getSelectionString(context, tree.getSelectionModel(), tree);
         }
         return super.getOutputProperty(context, component, propertyName, propertyIndex);
     }
     
     /**
      * @see nextapp.echo.webcontainer.AbstractComponentSynchronizePeer#getUpdatedOutputPropertyNames(
      *      nextapp.echo.app.util.Context,
      *      nextapp.echo.app.Component,
      *      nextapp.echo.app.update.ServerComponentUpdate)
      */
     public Iterator getUpdatedOutputPropertyNames(Context context, Component component, 
             ServerComponentUpdate update) {
         UserInstance userInstance = (UserInstance) context.get(UserInstance.class);
         
         Iterator normalPropertyIterator = super.getUpdatedOutputPropertyNames(context, component, update);
         HashSet extraProperties = new HashSet();
         
         // FIXME change when issue #45 is solved.
         if (update.hasUpdatedProperty("valid")) {
             System.out.println("remove render state");
             userInstance.removeRenderState(component);
             extraProperties.add(PROPERTY_TREE_STRUCTURE);
             extraProperties.add(Tree.SELECTION_CHANGED_PROPERTY);
         }
         
         if (update.hasUpdatedProperty(Tree.MODEL_CHANGED_PROPERTY)) {
             extraProperties.addAll(Arrays.asList(MODEL_CHANGED_UPDATE_PROPERTIES));
         } 
         if (update.hasUpdatedProperty(Tree.EXPANSION_STATE_CHANGED_PROPERTY)) {
             TreeRenderState renderState = (TreeRenderState) userInstance.getRenderState(component);
             if (renderState == null || renderState.hasChangedPaths()) {
                 extraProperties.add(PROPERTY_TREE_STRUCTURE);
             }
             if (renderState == null || renderState.hasUnsentSelections()) {
                 extraProperties.add(Tree.SELECTION_CHANGED_PROPERTY);
             } /*else {
                 renderState.clearChangedPaths();
             }*/
         }
         return new MultiIterator(new Iterator[] { normalPropertyIterator, extraProperties.iterator() });
     }
     
     /**
      * @see nextapp.echo.webcontainer.AbstractComponentSynchronizePeer#storeInputProperty(nextapp.echo.app.util.Context, nextapp.echo.app.Component, java.lang.String, int, java.lang.Object)
      */
     public void storeInputProperty(Context context, Component component,
             String propertyName, int index, Object newValue) {
         Tree tree = (Tree) component;
         if (EXPANSION_PROPERTY.equals(propertyName)) {
             int row = ((Integer)newValue).intValue();
             UserInstance userInstance = (UserInstance) context.get(UserInstance.class);
             TreeRenderState renderState = (TreeRenderState) userInstance.getRenderState(component);
             if (renderState == null) {
                 renderState = new TreeRenderState(tree);
                 userInstance.setRenderState(component, renderState);
             }
             TreePath path = tree.getPathForRow(row);
             renderState.setClientPath(path);
             renderState.removeSentPath(path);
             
             ClientUpdateManager clientUpdateManager = (ClientUpdateManager) context.get(ClientUpdateManager.class);
             clientUpdateManager.setComponentProperty(component, Tree.EXPANSION_STATE_CHANGED_PROPERTY, newValue);
         } else if (SELECTION_PROPERTY.equals(propertyName)) {
             TreeSelectionUpdate update = (TreeSelectionUpdate) newValue;
             TreeSelectionModel selectionModel = tree.getSelectionModel();
             if (!update.removedSelections.isEmpty()) {
                 TreePath[] paths = new TreePath[update.removedSelections.size()];
                 int i = 0;
                 for (Iterator iterator = update.removedSelections.iterator(); iterator.hasNext();) {
                     Integer row = (Integer) iterator.next();
                     paths[i++] = tree.getPathForRow(row.intValue());
                 }
                 selectionModel.removeSelectionPaths(paths);
             }
             if (!update.addedSelections.isEmpty()) {
                 TreePath[] paths = new TreePath[update.addedSelections.size()];
                 int i = 0;
                 for (Iterator iterator = update.addedSelections.iterator(); iterator.hasNext();) {
                     Integer row = (Integer) iterator.next();
                     paths[i++] = tree.getPathForRow(row.intValue());
                 }
                 if (update.clear) {
                     selectionModel.setSelectionPaths(paths);
                 } else {
                     selectionModel.addSelectionPaths(paths);
                 }
             }
         } else {
             super.storeInputProperty(context, component, propertyName, index, newValue);
         }
     }
     
     public Class getInputPropertyClass(String propertyName) {
         if (EXPANSION_PROPERTY.equals(propertyName)) {
             return Integer.class;
         } else if (SELECTION_PROPERTY.equals(propertyName)) {
             return TreeSelectionUpdate.class;
         }
         return super.getInputPropertyClass(propertyName);
     }
     
     /**
      * @see nextapp.echo.webcontainer.ComponentSynchronizePeer#init(nextapp.echo.app.util.Context)
      */
     public void init(Context context) {
         ServerMessage serverMessage = (ServerMessage) context.get(ServerMessage.class);
         serverMessage.addLibrary(CommonService.INSTANCE.getId());
         serverMessage.addLibrary(TREE_SERVICE.getId());
     }
 }
