 /*
  * This program is free software; you can redistribute it and/or modify it under the
  * terms of the GNU Lesser General Public License, version 2.1 as published by the Free Software
  * Foundation.
  *
  * You should have received a copy of the GNU Lesser General Public License along with this
  * program; if not, you can obtain a copy at http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
  * or from the Free Software Foundation, Inc.,
  * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
  *
  * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
  * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
  * See the GNU Lesser General Public License for more details.
  *
  * Copyright (c) 2009 Pentaho Corporation..  All rights reserved.
  */
 package org.pentaho.agilebi.pdi.modeler;
 
 import java.io.BufferedReader;
 import java.io.File;
 import java.io.FileReader;
 import java.util.ArrayList;
 import java.util.HashMap;
 import java.util.List;
 import java.util.Map;
 
 import org.apache.commons.logging.Log;
 import org.apache.commons.logging.LogFactory;
 import org.eclipse.swt.SWT;
 import org.eclipse.swt.widgets.MessageBox;
 import org.pentaho.agilebi.pdi.perspective.PublisherHelper;
 import org.pentaho.agilebi.pdi.visualizations.IVisualization;
 import org.pentaho.agilebi.pdi.visualizations.VisualizationManager;
 import org.pentaho.di.core.EngineMetaInterface;
 import org.pentaho.di.core.database.DatabaseMeta;
 import org.pentaho.di.i18n.BaseMessages;
 import org.pentaho.di.trans.HasDatabasesInterface;
 import org.pentaho.di.ui.core.dialog.EnterSelectionDialog;
 import org.pentaho.di.ui.spoon.MainSpoonPerspective;
 import org.pentaho.di.ui.spoon.Spoon;
 import org.pentaho.di.ui.spoon.SpoonPerspective;
 import org.pentaho.di.ui.spoon.SpoonPerspectiveManager;
 import org.pentaho.di.ui.spoon.delegates.SpoonDBDelegate;
 import org.pentaho.metadata.model.IPhysicalModel;
 import org.pentaho.metadata.model.IPhysicalTable;
 import org.pentaho.metadata.model.LogicalColumn;
 import org.pentaho.reporting.libraries.base.util.StringUtils;
 import org.pentaho.ui.xul.XulComponent;
 import org.pentaho.ui.xul.XulException;
 import org.pentaho.ui.xul.binding.Binding;
 import org.pentaho.ui.xul.binding.BindingConvertor;
 import org.pentaho.ui.xul.binding.BindingFactory;
 import org.pentaho.ui.xul.binding.DefaultBindingFactory;
 import org.pentaho.ui.xul.binding.Binding.Type;
 import org.pentaho.ui.xul.components.XulConfirmBox;
 import org.pentaho.ui.xul.components.XulLabel;
 import org.pentaho.ui.xul.components.XulMenuList;
 import org.pentaho.ui.xul.components.XulMessageBox;
 import org.pentaho.ui.xul.components.XulPromptBox;
 import org.pentaho.ui.xul.components.XulTextbox;
 import org.pentaho.ui.xul.containers.XulDeck;
 import org.pentaho.ui.xul.containers.XulDialog;
 import org.pentaho.ui.xul.containers.XulEditpanel;
 import org.pentaho.ui.xul.containers.XulTree;
 import org.pentaho.ui.xul.dnd.DropEvent;
 import org.pentaho.ui.xul.impl.AbstractXulEventHandler;
 import org.pentaho.ui.xul.util.AbstractModelNode;
 import org.pentaho.ui.xul.util.XulDialogCallback;
 
 /**
  * XUL Event Handler for the modeling interface. This class interacts with a ModelerModel to store state.
  * 
  * @author nbaker
  *
  */
 public class ModelerController extends AbstractXulEventHandler{
 
   private static final String NEW_DIMENSION_NAME = "newDimensionName"; //$NON-NLS-1$
 
   private static final String NEW_DIMESION_DIALOG = "newDimesionDialog"; //$NON-NLS-1$
 
 
   private static final String MY_TAB_LIST_ID = "myTabList"; //$NON-NLS-1$
 
   private static final String FIELD_LIST_ID = "fieldList"; //$NON-NLS-1$
 
   private static final String IN_PLAY_TABLE_ID = "fieldTable"; //$NON-NLS-1$
 
   private static final String MODEL_NAME_FIELD_ID = "modelname"; //$NON-NLS-1$
   
   private static final String SOURCE_NAME_LABEL_ID = "source_name"; //$NON-NLS-1$
 
   private static final String MODEL_NAME_PROPERTY = "modelName"; //$NON-NLS-1$
 
   private static final String VALUE_PROPERTY = "value"; //$NON-NLS-1$
 
   private static final String ELEMENTS_PROPERTY = "elements"; //$NON-NLS-1$
 
   private static final String FIELD_NAMES_PROPERTY = "fieldNames"; //$NON-NLS-1$
 
   private static Log logger = LogFactory.getLog(ModelerController.class);
   
   private ModelerWorkspace workspace;
   
   private String fieldTypesDesc[];
   private Integer fieldTypesCode[];
   
   private XulDialog newDimensionDialog;
   private XulTextbox newDimensionName;
   private XulTree dimensionTree;
   private XulMenuList visualizationList;
   private XulDeck propDeck;
   private Object[] selectedFields;
   private ModelerControllerDBRegistry databaseInterface;
   
   private BindingFactory bf = new DefaultBindingFactory();
  
   private List<String> visualizationNames;
   private Map<Class<? extends ModelerNodePropertiesForm>, ModelerNodePropertiesForm> propertiesForms = new HashMap<Class<? extends ModelerNodePropertiesForm>, ModelerNodePropertiesForm>();
   
   private ColResolverController colController;
   
   private XulEditpanel propPanel;
   
   public ModelerController(){
     this.workspace = new ModelerWorkspace();
     this.databaseInterface = new ModelerControllerDBRegistry();
   }
   
   public ModelerController(ModelerWorkspace workspace){
     this.workspace = workspace;
     this.databaseInterface = new ModelerControllerDBRegistry();
   }
   
   public String getName(){
     return "modeler";
   }
   
   public void onFieldListDrag(DropEvent event) {
     // nothing to do here
   }
 
   public void onDimensionTreeDrag(DropEvent event) {
     // todo, disable dragging of Root elements once we've updated the tree UI
   }
   
   public void onDimensionTreeDrop(DropEvent event) {
     List<Object> data = event.getDataTransfer().getData();
     List<Object> newdata = new ArrayList<Object>();
     for (Object obj : data) {
       if (obj instanceof AvailableField) {
         AvailableField availableField = (AvailableField) obj;
         // depending on the parent
         if (event.getDropParent() == null) {
           // null - cannot add fields at this level
         } else if (event.getDropParent() instanceof MeasuresCollection) {
           // measure collection - add as a measure
           newdata.add(workspace.createMeasureForNode(availableField));
         } else if (event.getDropParent() instanceof DimensionMetaDataCollection) {
           // dimension collection - add as a dimension
           newdata.add(workspace.createDimensionFromNode(availableField));
         } else if (event.getDropParent() instanceof DimensionMetaData) {
           // dimension - add as a hierarchy
           newdata.add(workspace.createHierarchyForParentWithNode((DimensionMetaData)event.getDropParent(), availableField));
         } else if (event.getDropParent() instanceof HierarchyMetaData) {
           // hierarchy - add as a level
           newdata.add(workspace.createLevelForParentWithNode((HierarchyMetaData)event.getDropParent(), availableField));
         } else if (event.getDropParent() instanceof LevelMetaData) {
           // level - cannot drop into a level
           event.setAccepted(false);
           return;
         }
       } else if (obj instanceof LevelMetaData) {
         LevelMetaData level = (LevelMetaData)obj;
         if (event.getDropParent() instanceof HierarchyMetaData) {
           // rebind to workspace, including logical column and actual parent
           level.setParent((HierarchyMetaData)event.getDropParent());
           newdata.add(level);
         } else if (event.getDropParent() instanceof DimensionMetaData) {
           // add as a new hierarchy
           HierarchyMetaData hier = workspace.createHierarchyForParentWithNode((DimensionMetaData)event.getDropParent(), level);
           hier.setName(level.getName());
           hier.get(0).setName(level.getName());
           newdata.add(hier);
         } else if (event.getDropParent() == null) {
           DimensionMetaData dim = workspace.createDimensionWithName(level.getColumnName());
           dim.setName(level.getName());
           dim.get(0).setName(level.getName());
           dim.get(0).get(0).setName(level.getName());
           newdata.add(dim);
         }
       } else if (obj instanceof HierarchyMetaData) {
         HierarchyMetaData hierarchy = (HierarchyMetaData)obj;
         if (event.getDropParent() == null) {
           DimensionMetaData dim = new DimensionMetaData(hierarchy.getName());
           dim.add(hierarchy);
           hierarchy.setParent(dim);
           // TODO: this will also need to resolve the level LogicalColumns
           newdata.add(dim);
         } else if (event.getDropParent() instanceof DimensionMetaData) {
           DimensionMetaData dim = (DimensionMetaData)event.getDropParent();
           hierarchy.setParent(dim);
           // TODO: this will also need to resolve the level LogicalColumns
           newdata.add(hierarchy);
         }
       } else if (obj instanceof DimensionMetaData) {
         if (event.getDropParent() == null) {
           newdata.add((DimensionMetaData)obj);
           // TODO: this will also need to resolve level LogicalColumns
         }
       } else if (obj instanceof MeasureMetaData) {
         if (event.getDropParent() instanceof MeasuresCollection) {
           MeasureMetaData measure = (MeasureMetaData)obj;
           LogicalColumn col = workspace.findLogicalColumn(obj.toString());
           measure.setLogicalColumn(col);
           newdata.add(measure);
         }
       }
       
     }
     if (newdata.size() == 0) {
       event.setAccepted(false);
     } else {
       event.getDataTransfer().setData(newdata);
     }
   }
 
   public void addField() {
   	AbstractMetaDataModelNode theNode = null;
     Object[] selectedItems = getSelectedFields();
     for (Object obj : selectedItems) {
       if (obj instanceof AvailableField) {
         AvailableField availableField = (AvailableField) obj;
         // depending on the parent
         if (selectedTreeItem == null) {
           // null - cannot add fields at this level
         } else if (selectedTreeItem instanceof MeasuresCollection) {
           // measure collection - add as a measure
         	MeasuresCollection theMesaures = (MeasuresCollection) selectedTreeItem;
         	theNode = workspace.createMeasureForNode(availableField);
         	theMesaures.add((MeasureMetaData) theNode);
         } else if (selectedTreeItem instanceof DimensionMetaDataCollection) {
           // dimension collection - add as a dimension
         	theNode = workspace.createDimensionFromNode(availableField);
         	DimensionMetaDataCollection theDimensions = (DimensionMetaDataCollection) selectedTreeItem;
         	theDimensions.add((DimensionMetaData) theNode);
         } else if (selectedTreeItem instanceof DimensionMetaData) {
           // dimension - add as a hierarchy
         	theNode = workspace.createHierarchyForParentWithNode((DimensionMetaData)selectedTreeItem, availableField);
         	DimensionMetaData theDimension = (DimensionMetaData) selectedTreeItem;
         	theDimension.add((HierarchyMetaData) theNode);
         } else if (selectedTreeItem instanceof HierarchyMetaData) {
           // hierarchy - add as a level
         	theNode = workspace.createLevelForParentWithNode((HierarchyMetaData)selectedTreeItem, availableField);
         	HierarchyMetaData theHierarchy = (HierarchyMetaData) selectedTreeItem;
         	theHierarchy.add((LevelMetaData) theNode);
         } 
         if(theNode != null) {
         	theNode.setParent((AbstractMetaDataModelNode) selectedTreeItem);
         } 
       } 
     }
   }
   
   public void editDataSource() {
     try {
       Spoon theSpoon = Spoon.getInstance();
   
       EngineMetaInterface theMeta = null;
       HasDatabasesInterface theDatabasesInterface = null;
       List<SpoonPerspective> thePerspectives = SpoonPerspectiveManager.getInstance().getPerspectives();
       for (SpoonPerspective thePerspective : thePerspectives) {
         if(thePerspective instanceof MainSpoonPerspective) {
            theMeta = thePerspective.getActiveMeta();
            break;
         } 
       }
       
       List<DatabaseMeta> theDatabases = new ArrayList<DatabaseMeta>();
       if(theMeta != null) {
         theDatabasesInterface = (HasDatabasesInterface) theMeta; 
       } else {
         theDatabasesInterface = this.databaseInterface;
       }
       theDatabases.addAll(theDatabasesInterface.getDatabases());
   
       String theSelectedTable = null;
       IModelerSource theModelerSource = this.workspace.getModelSource();
       if(theModelerSource != null) {
         theSelectedTable = theModelerSource.getDatabaseName();
       }
       int[] theSelectedIndexes = new int[1];
       String[] theNames = new String[theDatabases.size()];
       for (int i = 0; i < theDatabases.size(); i++) {
         theNames[i] = theDatabases.get(i).getName();
         if(theSelectedTable != null && theNames[i].equals(theSelectedTable)) {
           theSelectedIndexes[0] = i;
         }
       }
       
       EnterSelectionDialog theDialog = new EnterSelectionDialog(theSpoon.getShell(), theNames, BaseMessages.getString(Spoon.class ,"Spoon.ExploreDB.SelectDB.Title"), BaseMessages.getString(Spoon.class, "Spoon.ExploreDB.SelectDB.Message"), theDatabasesInterface);
       theDialog.setSelectedNrs(theSelectedIndexes);
       String theDBName = theDialog.open();
       if (theDBName != null) {
         SpoonDBDelegate theDelegate = new SpoonDBDelegate(theSpoon);
         DatabaseMeta theDBMeta = DatabaseMeta.findDatabase(theDatabasesInterface.getDatabases(), theDBName);
         String theTable = theDelegate.exploreDB(theDBMeta, false);
         boolean refresh = this.workspace.getAvailableFields().isEmpty();
         if(!StringUtils.isEmpty(theTable) && !this.workspace.getAvailableFields().isEmpty()) {
           MessageBox theMessageBox = new MessageBox(theSpoon.getShell(), SWT.ICON_WARNING | SWT.OK | SWT.CANCEL);
           theMessageBox.setText(BaseMessages.getString(Spoon.class, "Spoon.Message.Warning.Warning"));
           theMessageBox.setMessage(BaseMessages.getString(Spoon.class, "Spoon.Message.Model.Warning"));
           int theVal = theMessageBox.open();
           switch (theVal) 
           {
             case SWT.OK:
              refresh = true;
               break;
             case SWT.CANCEL:
               refresh = false;
                break;
           }
         }
         if(refresh) {
           TableModelerSource theSource = new TableModelerSource(theDBMeta, theTable, null);
           ModelerWorkspaceUtil.populateModelFromSource(this.workspace, theSource);
         }
       }
     } catch (Exception e) {
       logger.equals(e); 
     }
   }
   
   public void init() throws ModelerException{
 
     bf.setDocument(document);
     
     newDimensionDialog = (XulDialog) document.getElementById(NEW_DIMESION_DIALOG);
     newDimensionName = (XulTextbox) document.getElementById(NEW_DIMENSION_NAME);
     dimensionTree = (XulTree) document.getElementById("dimensionTree");
     visualizationList = (XulMenuList)document.getElementById("visualizationlist");
     propDeck = (XulDeck) document.getElementById("propertiesdeck");
     propPanel = (XulEditpanel) document.getElementById("propertiesPanel");
     
     XulLabel sourceLabel = (XulLabel) document.getElementById(SOURCE_NAME_LABEL_ID);
     String connectionName = "";
     String tableName = "";
     
     //TODO: migrate this code elsewhere or remove it entirely
     if( workspace.getModelSource() != null && workspace.getModelSource() instanceof OutputStepModelerSource) {
       // for now just list the first table in the first physical workspace
       DatabaseMeta databaseMeta = workspace.getModelSource().getDatabaseMeta();
       if( databaseMeta != null ) {
         connectionName = databaseMeta.getName();
       }
       List<IPhysicalModel> physicalModels = workspace.getDomain().getPhysicalModels();
       if( physicalModels != null && physicalModels.size() > 0 ) {
         List<? extends IPhysicalTable> tables = physicalModels.get(0).getPhysicalTables();
         if( tables != null && tables.size() > 0 ) {
           // TODO where is the locale coming from? And why do we need one here?
           tableName = tables.get(0).getName("en_US");
         }
       }
     }
     sourceLabel.setValue( "Table : " + tableName );
     bf.createBinding(workspace, "sourceName", sourceLabel, "value");
 
     bf.setBindingType(Type.ONE_WAY);
     fieldListBinding = bf.createBinding(workspace, "availableFields", FIELD_LIST_ID, ELEMENTS_PROPERTY);
     selectedFieldsBinding = bf.createBinding(FIELD_LIST_ID, "selectedItems", this, "selectedFields");
     
     bf.createBinding(workspace, "selectedVisualization", visualizationList, "selectedItem");    
     visualizationsBinding = bf.createBinding(this, "visualizationNames", visualizationList, "elements");
     
     modelTreeBinding = bf.createBinding(workspace, "model", dimensionTree, "elements");
     bf.createBinding(dimensionTree, "selectedItem", this, "dimTreeSelectionChanged");    
     
     bf.createBinding("fieldList", "selectedItem", "addField", "disabled", new BindingConvertor<Object, Boolean>() {
     	public Boolean sourceToTarget(Object value) {
     		return getSelectedFields().length == 0 || selectedTreeItem == null || selectedTreeItem instanceof LevelMetaData || selectedTreeItem instanceof MainModelNode;
     	}
 
       public Object targetToSource(Boolean value) {
       	return null;
       }
     });
     
     bf.createBinding(dimensionTree, "selectedItem", "addField", "disabled", new BindingConvertor<Object, Boolean>() {
     	public Boolean sourceToTarget(Object value) {
     		return getSelectedFields().length == 0 || selectedTreeItem == null || selectedTreeItem instanceof LevelMetaData  || selectedTreeItem instanceof MainModelNode;
     	}
 
       public Object targetToSource(Boolean value) {
       	return null;
       }
     });      
     
     bf.createBinding(dimensionTree, "selectedItem", "measureBtn", "disabled", new ButtonConvertor(MeasuresCollection.class));    
     bf.createBinding(dimensionTree, "selectedItem", "dimensionBtn", "disabled", new ButtonConvertor(DimensionMetaDataCollection.class));
     bf.createBinding(dimensionTree, "selectedItem", "hierarchyBtn", "disabled", new ButtonConvertor(DimensionMetaData.class));
     bf.createBinding(dimensionTree, "selectedItem", "levelBtn", "disabled", new ButtonConvertor(HierarchyMetaData.class));
     
     bf.setBindingType(Type.BI_DIRECTIONAL);
     modelNameBinding = bf.createBinding(workspace, MODEL_NAME_PROPERTY, MODEL_NAME_FIELD_ID, VALUE_PROPERTY);
     
     bf.createBinding(this.propPanel, "visible", this, "propVisible");
     
     fireBindings();
     
     dimensionTree.expandAll();
   }
   
   public void showAutopopulatePrompt() {
     try{
       MainModelNode model = workspace.getModel();
       if(model.getDimensions().isEmpty() && model.getMeasures().isEmpty()) {
         autoPopulate();
       } else {
           XulConfirmBox confirm = (XulConfirmBox) document.createElement("confirmbox");
           confirm.setTitle(BaseMessages.getString(this.getClass(), "auto_populate_title"));
           confirm.setMessage(BaseMessages.getString(this.getClass(), "auto_populate_msg"));
           confirm.setAcceptLabel(BaseMessages.getString(this.getClass(), "yes"));
           confirm.setCancelLabel(BaseMessages.getString(this.getClass(), "no"));
           confirm.addDialogCallback(new XulDialogCallback(){
             public void onClose(XulComponent sender, Status returnCode, Object retVal) {
               if(returnCode == Status.ACCEPT){
                 autoPopulate();
               } 
             }
             public void onError(XulComponent sender, Throwable t) {}
           });
           confirm.open();
       }
     } catch(XulException e){
       logger.error(e);
     }
   }
   
   private void fireBindings() throws ModelerException{
     try {
       fieldListBinding.fireSourceChanged();
       selectedFieldsBinding.fireSourceChanged();
       modelTreeBinding.fireSourceChanged();
       modelNameBinding.fireSourceChanged();
       visualizationsBinding.fireSourceChanged();
     } catch (Exception e) {
       logger.info("Error firing off initial bindings", e);
       throw new ModelerException(e);
     }
   }
   
   public void setSelectedDims(List<Object> selectedDims) {
     List<Object> prevSelected = null; // this.selectedColumns;
     if (selectedDims != null) {
       System.out.println(selectedDims.get(0));
     }
     // this.selectedColumns = selectedColumns;
     // this.firePropertyChange("selectedColumns", prevSelected , selectedColumns);
   }
   
 //  public void moveFieldIntoPlay() {
 //    XulListbox fieldsList = (XulListbox) document.getElementById(FIELD_LIST_ID);
 //    Object[] selectedItems = fieldsList.getSelectedItems();
 //    int tabIndex = ((XulTabbox) document.getElementById(MY_TAB_LIST_ID)).getSelectedIndex();
 //    if (tabIndex == 0) {
 //      for (Object obj : selectedItems) {
 //        workspace.addFieldIntoPlay(obj);
 //      }
 //    } else if (tabIndex == 1){
 //      // if a dimension or hierarchy is selected, add the field as a level
 //      // otherwise add a new dimension
 //      for (Object obj : selectedItems) {
 //        if (selectedTreeItem == null) {
 //          workspace.addDimension(obj);
 //        } else {
 //          workspace.addToHeirarchy(selectedTreeItem, obj);
 //        }
 //      }
 //    }
 //  }
 
   
   public void visualize() throws ModelerException{
     try{
       openVisualizer();
     } catch(Exception e){
       logger.info(e);
       throw new ModelerException(e);
     }
   }
   
   public void publish() throws ModelerException{
     PublisherHelper.publish(workspace, workspace.getFileName());
   }
   
   /**
    * Goes back to the source of the metadata and see if anything has changed.
    * Updates the UI accordingly
    */
   public void refreshFields() throws ModelerException {
     workspace.refresh();
   }
   
   public void setFileName(String fileName){
     workspace.setFileName(fileName);
   }
   
 
   
   public void showNewMeasureDialog() {
     try {
       XulPromptBox prompt = (XulPromptBox) document.createElement("promptbox");
       prompt.setTitle("New Measure");
       prompt.setMessage("Enter new Measure name");
       prompt.addDialogCallback(new XulDialogCallback(){
   
         public void onClose(XulComponent sender, Status returnCode, Object retVal) {
           if(returnCode == Status.ACCEPT){
           	MeasuresCollection theMesaures = (MeasuresCollection) selectedTreeItem;
           	MeasureMetaData theMeasure = new MeasureMetaData("" + retVal, "", "" + retVal);
           	theMeasure.setParent(theMesaures);
           	theMesaures.add(theMeasure);
           }
         }
   
         public void onError(XulComponent sender, Throwable t) {
           logger.error(t);
         }
         
       });
       int theResult = prompt.open();
       if(theResult == 0) {
         refreshFields();
       }
     } catch (Exception e) {
       logger.error(e);
     }
   }
   
   public void showNewHierarchyDialog() {
   	 try {
        XulPromptBox prompt = (XulPromptBox) document.createElement("promptbox");
        prompt.setTitle("New Hierarchy");
        prompt.setMessage("Enter new Hierarcy name");
        prompt.addDialogCallback(new XulDialogCallback(){
 
       	 public void onClose(XulComponent sender, Status returnCode, Object retVal) {
            if(returnCode == Status.ACCEPT){
           	DimensionMetaData theDimension = (DimensionMetaData) selectedTreeItem;
           	HierarchyMetaData theHieararchy = new HierarchyMetaData("" + retVal);
           	theHieararchy.setParent(theDimension);
           	theDimension.add(theHieararchy);
            }
          }
    
          public void onError(XulComponent sender, Throwable t) {
            logger.error(t);
          }
        });
        prompt.open();
      } catch (XulException e) {
        logger.error(e);
      }
   }
   
   public void showNewLevelDialog() {
   	
   	try {
       XulPromptBox prompt = (XulPromptBox) document.createElement("promptbox");
       prompt.setTitle("New Level");
       prompt.setMessage("Enter new Level name");
       prompt.addDialogCallback(new XulDialogCallback(){
 
      	 public void onClose(XulComponent sender, Status returnCode, Object retVal) {
           if(returnCode == Status.ACCEPT){
           	HierarchyMetaData theHierarchy = (HierarchyMetaData) selectedTreeItem;
           	LevelMetaData theLevel = new LevelMetaData(theHierarchy, "" + retVal);
           	theHierarchy.add(theLevel);
           }
         }
   
         public void onError(XulComponent sender, Throwable t) {
           logger.error(t);
         }
       });
       int theResult = prompt.open();
       if(theResult == 0) {
         refreshFields();
       }
     } catch (Exception e) {
       logger.error(e);
     }
   	
   }
   
   public void showNewDimensionDialog(){
     
     try {
       XulPromptBox prompt = (XulPromptBox) document.createElement("promptbox");
       //prompt.setModalParent(((Spoon) SpoonFactory.getInstance()).getShell());
       prompt.setTitle("New Dimension");
       prompt.setMessage("Enter new Dimension name");
       prompt.addDialogCallback(new XulDialogCallback(){
   
         public void onClose(XulComponent sender, Status returnCode, Object retVal) {
           if(returnCode == Status.ACCEPT){
   
             DimensionMetaData dimension = new DimensionMetaData(""+retVal);
             HierarchyMetaData hierarchy = new HierarchyMetaData(""+retVal);
             hierarchy.setParent(dimension);
             dimension.add(hierarchy);
             workspace.addDimension(dimension);
           }
         }
   
         public void onError(XulComponent sender, Throwable t) {
           logger.error(t);
         }
         
       });
       prompt.open();
     } catch (XulException e) {
       logger.error(e);
     }
   }
   
   public void moveFieldUp() {
     if(selectedTreeItem == null){
       return;
     }
     ((AbstractModelNode) selectedTreeItem).getParent().moveChildUp(selectedTreeItem);
     
   }
   
   public void moveFieldDown() {
     if(selectedTreeItem == null){
       return;
     }
     ((AbstractModelNode) selectedTreeItem).getParent().moveChildDown(selectedTreeItem);
     
   }
 
   Object selectedTreeItem;
 
   private Binding fieldListBinding;
   private Binding selectedFieldsBinding;
 
   private Binding visualizationsBinding;
 
   private Binding modelTreeBinding;
 
   private Binding modelNameBinding;
   
   
   public void setDimTreeSelectionChanged(Object selection){
     selectedTreeItem = selection;
     if(selection != null && selection instanceof AbstractMetaDataModelNode){
       AbstractMetaDataModelNode node = (AbstractMetaDataModelNode) selection;
       ModelerNodePropertiesForm form = propertiesForms.get(node.getPropertiesForm());
       if(form != null){
         form.activate((AbstractMetaDataModelNode) selection);
         return;
       }
     }
     if(this.propDeck != null) {
     this.propDeck.setSelectedIndex(0);
     }
   }
     
   
   public void removeField() {
     if(selectedTreeItem instanceof DimensionMetaDataCollection 
         || selectedTreeItem instanceof MeasuresCollection 
         || selectedTreeItem instanceof MainModelNode
         || selectedTreeItem == null){
       return;
     }
     ((AbstractModelNode) selectedTreeItem).getParent().remove(selectedTreeItem);
     setDimTreeSelectionChanged(null);
   }
 
   public List<String> getVisualizationNames() {
   	if(this.visualizationNames == null) {
   		VisualizationManager theManager = VisualizationManager.getInstance();
   		this.visualizationNames = theManager.getVisualizationNames();
   	}
   	return this.visualizationNames;
   }
   
   public ModelerWorkspace getModel() {
     return workspace;
   }
 
   public void setModel(ModelerWorkspace model) throws ModelerException{
     this.workspace = model;
     fireBindings();
   }
   
 
   public void openVisualizer() {
     if(workspace.isDirty()){
      /*try{
         XulMessageBox box = (XulMessageBox) document.createElement("messagebox");
         box.setTitle("Warning");
         box.setMessage("You must save your workspace before visualizing.");
         box.open();
       } catch(XulException e){
         e.printStackTrace();
         logger.error(e);
       }
      return;*/
      
      try {
        ModelerHelper theHelper = ModelerHelper.getInstance();
        theHelper.quickVisualize(workspace);
      } catch (ModelerException e) {
        return;
      }
     }
     workspace.getModel().validateTree();
     if(workspace.isValid() == false){
       showValidationMessages();
       return;
     }
     
  	/*VisualizationManager theManager = VisualizationManager.getInstance();
   	IVisualization theVisualization = theManager.getVisualization(visualizationList.getSelectedItem());
   	if(theVisualization != null) {
   	  if (workspace.getFileName() != null) {
   	    // TODO: Find a better name for the cube, maybe just workspace name?
   	    theVisualization.createVisualizationFromModel(workspace);
   	  } else {
   	    throw new UnsupportedOperationException("TODO: prompt to save workspace before visualization");
   	  }
  	}*/
   }
   
   private void showValidationMessages(){
 
     StringBuffer validationErrors = new StringBuffer(BaseMessages.getString(this.getClass(), "model_contains_errors"));
     for (String msg : workspace.getValidationMessages()) {
       validationErrors.append(msg);
       validationErrors.append("\n");
       logger.info(msg);
     }
     try{
       XulMessageBox msg = (XulMessageBox) document.createElement("messagebox");
       msg.setTitle(BaseMessages.getString(this.getClass(), "model_not_valid"));
       msg.setMessage(validationErrors.toString());
       msg.open();
     } catch(XulException e){
       logger.error(e);
     }
   }
   
   public boolean saveWorkspace(String fileName) throws ModelerException {
     workspace.getModel().validateTree();
     if(workspace.isValid() == false){
       showValidationMessages();
       return false;
     }
   	ModelerWorkspaceUtil.saveWorkspace(workspace, fileName);
     workspace.setFileName(fileName);
     workspace.setDirty(false);
     return true;
   }
   
   public void resolveMissingColumn() {
     if(selectedTreeItem instanceof ColumnBackedNode 
         && ((AbstractMetaDataModelNode) selectedTreeItem).isValid() == false){
         changeColumn();
     }
   }
   
   public void changeColumn(){ 
     colController.show(this.workspace, (ColumnBackedNode) selectedTreeItem); 
   }
   
   public void loadWorkspace() throws ModelerException {
   	
   	try {
 	  	StringBuffer theStringBuffer = new StringBuffer();
 	  	FileReader theReader = new FileReader(new File("my_metadata.xml"));
 	  	BufferedReader theBuffer = new BufferedReader(theReader);
 	  	String theLine = null;
 	  	while((theLine = theBuffer.readLine()) != null) {
 	  		theStringBuffer.append(theLine);
 	  	}
 	  	ModelerWorkspaceUtil.loadWorkspace("my_metadata.xml", theStringBuffer.toString(), getModel());
 	  	
   	} catch(Exception e) {
   		logger.info(e.getLocalizedMessage());
   		throw new ModelerException(e);
   	}
   }
   
   public void loadPerspective(String id){
    // document.loadPerspective(id);
   }
   
   public void addPropertyForm(AbstractModelerNodeForm form){
     propertiesForms.put(form.getClass(), form);
   }
 
   public void setColResolver(ColResolverController controller){
     this.colController = controller;
   }
   
   public void autoPopulate(){
     try {
       ModelerWorkspaceUtil.autoModelFlat(this.workspace);
       this.dimensionTree.expandAll();
     } catch (ModelerException e) {
       logger.error(e.getLocalizedMessage());
     }
   }
   
   public void togglePropertiesPanel(){
     setPropVisible(! isPropVisible());
   }
   
   
   private boolean propVisible = true;
   public boolean isPropVisible(){
     return propVisible;
   }
   
   public void setPropVisible(boolean vis){
     boolean prevVal = propVisible;
     this.propVisible = vis;
     this.firePropertyChange("propVisible", prevVal, vis);
   }
   
   private static class ButtonConvertor extends BindingConvertor<Object, Boolean> {
   	
   	private Class type;
   	
   	public ButtonConvertor(Class aClass) {
   		type = aClass;
   	}
   	
   	public Boolean sourceToTarget(Object value) {
   		return value == null || !(value.getClass() == type);
   	}
 
     public Object targetToSource(Boolean value) {
     	return null;
     }
   }
   
   public void setSelectedFields(Object[] aFields) {
     selectedFields = aFields;
   }
   
   public Object[] getSelectedFields() {
     if(selectedFields == null) {
       selectedFields = new Object[]{};
     }
     return selectedFields;
   } 
 }
