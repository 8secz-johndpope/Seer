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
 package org.pentaho.agilebi.modeler;
 
 import org.pentaho.agilebi.modeler.nodes.*;
 import org.pentaho.metadata.model.*;
 import org.pentaho.metadata.model.concept.types.AggregationType;
 import org.pentaho.metadata.model.olap.*;
 import org.pentaho.ui.xul.XulEventSourceAdapter;
 import org.pentaho.ui.xul.stereotype.Bindable;
 
 import java.beans.PropertyChangeEvent;
 import java.beans.PropertyChangeListener;
 import java.io.Serializable;
 import java.util.*;
 
 
 /**
  * UI model behind the XUL-based interface. This class contains a reference from the context in
  * which the modeling was initiated through an IModelerSource which also provides model generation.
  *
  * @author nbaker
  */
 @SuppressWarnings("unchecked")
 public class ModelerWorkspace extends XulEventSourceAdapter implements Serializable{
 
   private AvailableFieldCollection availableFields = new AvailableFieldCollection();
   private AvailableFieldCollection availableOlapFields = new AvailableFieldCollection();
 
   private MainModelNode model;
   private RelationalModelNode relationalModel;
 
   private String sourceName;
 
   private transient IModelerSource source;
 
   private String selectedServer;
 
   private String selectedVisualization;
 
   private String schemaName;
 
   private Domain domain;
 
   private boolean dirty = true;
 
   // full path to file
   private String fileName;
 
   private boolean modelIsChanging;
 
   private boolean isTemporary;
 
   private AbstractMetaDataModelNode selectedNode;
   private IModelerWorkspaceHelper workspaceHelper;
   private AbstractMetaDataModelNode selectedRelationalNode;
 
   private transient ModelerMode currentModellingMode = ModelerMode.ANALYSIS_AND_REPORTING;
   private transient ModelerPerspective currentModelerPerspective = ModelerPerspective.ANALYSIS;
 
   public ModelerWorkspace(IModelerWorkspaceHelper helper) {
 
     this.isTemporary = true;
     this.workspaceHelper = helper;
 
     setModel(new MainModelNode());
     setRelationalModel(new RelationalModelNode());
   }
 
   @Bindable
   public MainModelNode getModel() {
     return model;
   }
 
   @Bindable
   public void setModel( MainModelNode model ) {
     this.model = model;
     model.addPropertyChangeListener("children", new PropertyChangeListener() { //$NON-NLS-1$
 
       public void propertyChange( PropertyChangeEvent arg0 ) {
         if (!modelIsChanging) {
           fireModelChanged();
         }
       }
     });
   }
 
   @Bindable
   public RelationalModelNode getRelationalModel() {
     return relationalModel;
   }
 
   @Bindable
   public void setRelationalModel( RelationalModelNode model ) {
     this.relationalModel = model;
     relationalModel.addPropertyChangeListener("children", new PropertyChangeListener() {
       public void propertyChange(PropertyChangeEvent evt) {
         if (!modelIsChanging) {
           fireRelationalModelChanged();
         }
       }
     });
   }
 
 
   @Bindable
   public void setFileName( String fileName ) {
     String prevVal = this.fileName;
     String prevFriendly = getShortFileName();
 
     this.fileName = fileName;
     firePropertyChange("fileName", prevVal, fileName); //$NON-NLS-1$
     firePropertyChange("shortFileName", prevFriendly, getShortFileName()); //$NON-NLS-1$
   }
 
   @Bindable
   public String getShortFileName() {
 
     if (fileName == null) {
       return null;
     }
     int extensionPos = fileName.lastIndexOf('.');
     if (extensionPos == -1) {
       extensionPos = fileName.length();
     }
     int sepPos = fileName.replace('\\', '/').lastIndexOf('/');
     if (sepPos == -1) {
       sepPos = 0;
     } else {
       sepPos++;
     }
     return fileName.substring(sepPos, extensionPos);
   }
 
   @Bindable
   public String getFileName() {
     return fileName;
   }
 
   //transMeta.getFilename()
 
   @Bindable
   public String getSourceName() {
     return sourceName;
   }
 
   @Bindable
   public void setSourceName( String sourceName ) {
     this.sourceName = sourceName;
   }
 
   @Bindable
   public String getModelName() {
     return model.getName();
   }
 
   @Bindable
   public void setModelName( String modelName ) {
     String prevVal = model.getName();
     model.setName(modelName);
     setDirty(true);
     this.firePropertyChange("modelName", prevVal, modelName); //$NON-NLS-1$
   }
 
   @Bindable
   public String getRelationalModelName() {
     return relationalModel.getName();
   }
   @Bindable
   public void setRelationalModelName(String modelName) {
     String prevVal = model.getName();
     relationalModel.setName(modelName);
     setDirty(true);
     this.firePropertyChange("relationalModelName", prevVal, modelName); //$NON-NLS-1$
   }
 
   @Bindable
   public boolean isDirty() {
     return dirty;
   }
 
   @Bindable
   public boolean isValid() {
     boolean valid = false;
     switch (getCurrentModellingMode()) {
       case ANALYSIS_AND_REPORTING:
         valid = this.model.isValid() && relationalModel.isValid();
         break;
       case REPORTING_ONLY:
         valid = relationalModel.isValid();
     }
     firePropertyChange("valid", null, valid);
     return valid;
   }
 
   @Bindable
   public List<String> getValidationMessages() {
     List<String> modelMsg = model.getValidationMessages();
     List<String> relModelMsg = relationalModel.getValidationMessages();
     modelMsg.addAll(relModelMsg);
     return modelMsg;
   }
 
   @Bindable
   public void setDirty( boolean dirty ) {
     boolean prevVal = this.dirty;
     this.dirty = dirty;
     this.firePropertyChange("dirty", prevVal, this.dirty); //$NON-NLS-1$
   }
 
   @Bindable
   public AvailableFieldCollection getAvailableFields() {
     return availableFields;
   }
 
   @Bindable
   public AvailableFieldCollection getAvailableOlapFields() {
     return availableOlapFields;
   }
 
 
   @Bindable
   public void setSelectedVisualization( String aVisualization ) {
     this.selectedVisualization = aVisualization;
   }
 
   @Bindable
   public String getSelectedVisualization() {
     return this.selectedVisualization;
   }
 
   public DimensionMetaData createDimensionFromNode( ColumnBackedNode obj ) {
     DimensionMetaData dimension = new DimensionMetaData(obj.getName());
     dimension.setExpanded(true);
     HierarchyMetaData hierarchy = createHierarchyForParentWithNode(dimension, obj);
     hierarchy.setParent(dimension);
     hierarchy.setExpanded(true);
     dimension.add(hierarchy);
     return dimension;
   }
 
   public DimensionMetaData createDimensionWithName( String dimName ) {
     DimensionMetaData dimension = new DimensionMetaData(dimName);
     dimension.setExpanded(true);
     HierarchyMetaData hierarchy = createHierarchyForParentWithNode(dimension, null);
     hierarchy.setParent(dimension);
     hierarchy.setExpanded(true);
     dimension.add(hierarchy);
     return dimension;
   }
 
 
   public void addDimensionFromNode( ColumnBackedNode obj ) {
     addDimension(createDimensionFromNode(obj));
   }
 
   public void addDimension( DimensionMetaData dim ) {
     boolean prevChangeState = this.modelIsChanging;
     this.setModelIsChanging(true);
     this.model.getDimensions().add(dim);
     this.setModelIsChanging(prevChangeState);
   }
 
   public void addCategory( CategoryMetaData cat ) {
     boolean prevChangeState = this.modelIsChanging;
     this.setRelationalModelIsChanging(true);
     this.relationalModel.getCategories().add(cat);
     this.setRelationalModelIsChanging(prevChangeState);
   }
 
 
   public LevelMetaData createLevelForParentWithNode( HierarchyMetaData parent, ColumnBackedNode obj ) {
     LevelMetaData level = new LevelMetaData(parent, obj.getName());
     level.setParent(parent);
     level.setLogicalColumn(obj.getLogicalColumn());
     return level;
   }
 
   public LevelMetaData createLevelForParentWithNode( HierarchyMetaData parent, String name ) {
     LevelMetaData level = new LevelMetaData(parent, name);
     level.setParent(parent);
     level.setLogicalColumn(findLogicalColumn(name));
     return level;
   }
 
   public FieldMetaData createFieldForParentWithNode( CategoryMetaData parent, AvailableField selectedField ) {
     FieldMetaData field = new FieldMetaData(parent, selectedField.getName(), "",
         selectedField.getDisplayName(), workspaceHelper.getLocale()); //$NON-NLS-1$
     field.setLogicalColumn(selectedField.getLogicalColumn());
     field.setFieldTypeDesc(selectedField.getLogicalColumn().getDataType().getName());
     return field;
   }
 
   public HierarchyMetaData createHierarchyForParentWithNode( DimensionMetaData parent, ColumnBackedNode obj ) {
     HierarchyMetaData hier = new HierarchyMetaData(obj.getName());
     hier.setParent(parent);
     hier.setExpanded(true);
     if (obj != null) {
       LevelMetaData level = createLevelForParentWithNode(hier, obj);
       hier.add(level);
     }
     return hier;
   }
 
   private void fireFieldsChanged() {
     firePropertyChange("availableFields", null, this.availableFields); //$NON-NLS-1$
   }
 
   private void fireModelChanged() {
     firePropertyChange("model", null, model); //$NON-NLS-1$
     setDirty(true);
   }
 
   private void fireRelationalModelChanged() {
     firePropertyChange("relationalModel", null, relationalModel); //$NON-NLS-1$
     setDirty(true);
   }
 
   public MeasureMetaData createMeasureForNode( AvailableField selectedField ) {
 
     MeasureMetaData meta = new MeasureMetaData(selectedField.getName(), "",
         selectedField.getDisplayName(), workspaceHelper.getLocale()); //$NON-NLS-1$
     meta.setLogicalColumn(selectedField.getLogicalColumn());
 
     return meta;
   }
 
   public void addMeasure( MeasureMetaData measure) {
 
     boolean prevChangeState = isModelChanging();
     this.setModelIsChanging(true);
     this.model.getMeasures().add(measure);
     this.setModelIsChanging(prevChangeState);
   }
 
   public LogicalColumn findLogicalColumn( String id ) {
     LogicalColumn col = null;
     for (LogicalColumn c : domain.getLogicalModels().get(0).getLogicalTables().get(0).getLogicalColumns()) {
       if (c.getName(workspaceHelper.getLocale()).equals(id)) {
         col = c;
         break;
       }
     }
     return col;
   }
 
   public void setModelSource( IModelerSource source ) {
     this.source = source;
   }
 
   public IModelerSource getModelSource() {
     return source;
   }
 
   public void setFields( List<MeasureMetaData> fields ) {
     this.model.getMeasures().clear();
     this.model.getMeasures().addAll(fields);
   }
 
   public void refresh(ModelerMode mode) throws ModelerException {
     if (source == null) {
       return;
     }
 
     Domain newDomain = source.generateDomain(mode == ModelerMode.ANALYSIS_AND_REPORTING);
     refresh(newDomain);
   }
   public void refresh(Domain newDomain) throws ModelerException {
 
     Comparator<AvailableField> fieldComparator = new Comparator<AvailableField>() {
           public int compare( AvailableField arg0, AvailableField arg1 ) {
             return arg0.getLogicalColumn().getId().compareTo(arg1.getLogicalColumn().getId());
           }
     };
 
     LogicalModel logicalModel = newDomain.getLogicalModels().get(0);
 
     // Add in new logicalColumns
     for (LogicalTable table : logicalModel.getLogicalTables()) {
      if (table.getId().endsWith(BaseModelerWorkspaceHelper.OLAP_SUFFIX)) {
         for (LogicalColumn lc : table.getLogicalColumns()) {
           boolean exists = false;
           inner:
           for (AvailableField fmd : this.availableFields) {
             if (fmd.getLogicalColumn().getId().equals(lc.getId()) || fmd.getLogicalColumn().getName(workspaceHelper.getLocale()).equals(lc.getName(workspaceHelper.getLocale()))) {
               fmd.setLogicalColumn(lc);
               exists = true;
               break inner;
             }
           }
           if (!exists) {
             AvailableField fm = new AvailableField();
             fm.setLogicalColumn(lc);
             fm.setName(lc.getName(workspaceHelper.getLocale()));
             fm.setDisplayName(lc.getName(workspaceHelper.getLocale()));
             availableFields.add(fm);
             availableOlapFields.add(DimensionTreeHelper.convertToOlapField(fm, logicalModel));
 
             Collections.sort(availableFields, fieldComparator);
             Collections.sort(availableOlapFields, fieldComparator);
           }
         }
       }
     }
 
 
     // Remove logicalColumns that no longer exist.
     List<AvailableField> toRemove = new ArrayList<AvailableField>();
     List<AvailableField> olapToRemove = new ArrayList<AvailableField>();
     for (AvailableField fm : availableFields) {
       boolean exists = false;
       LogicalColumn fmlc = fm.getLogicalColumn();
       inner:
       for(LogicalTable lt : logicalModel.getLogicalTables()) {
         if (!lt.getId().endsWith(BaseModelerWorkspaceHelper.OLAP_SUFFIX)) {
           for (LogicalColumn lc : lt.getLogicalColumns()) {
             if (lc.getId().equals(fmlc.getId()) || lc.getName(workspaceHelper.getLocale()).equals(fmlc.getName(workspaceHelper.getLocale()))) {
               exists = true;
               break inner;
             }
           }
         }
       }
       if (!exists) {
         toRemove.add(fm);
         olapToRemove.add(DimensionTreeHelper.convertToOlapField(fm, logicalModel));
       }
     }
     availableFields.removeAll(toRemove);
     availableOlapFields.removeAll(olapToRemove);
     workspaceHelper.sortFields(availableFields);
     workspaceHelper.sortFields(availableOlapFields);
 
     fireFieldsChanged();
 
 
     for (MeasureMetaData measure : model.getMeasures()) {
       boolean found = false;
       if (measure.getLogicalColumn() != null) {
         for (AvailableField fm : availableOlapFields) {
           if (fm.getLogicalColumn().getId().equals(measure.getLogicalColumn().getId()) || fm.getLogicalColumn().getName(workspaceHelper.getLocale()).equals(measure.getLogicalColumn().getName(workspaceHelper.getLocale()))) {
             found = true;
           } else {
             if (fm.getLogicalColumn().getProperty(SqlPhysicalColumn.TARGET_COLUMN).equals(
                 measure.getLogicalColumn().getProperty(SqlPhysicalColumn.TARGET_COLUMN))) {
               // clone the logical column into the new model
               // this is necessary because a model may contain
               // multiple measures, each with their own
               // default aggregation and name
               LogicalColumn lCol = (LogicalColumn) fm.getLogicalColumn().clone();
               lCol.setId(measure.getLogicalColumn().getId());
               newDomain.getLogicalModels().get(0).getLogicalTables().get(0).addLogicalColumn(lCol);
               found = true;
             }
           }
         }
       }
       if (!found) {
         measure.setLogicalColumn(null);
       }
     }
 
     try{
       for (DimensionMetaData dm : model.getDimensions()) {
         for (HierarchyMetaData hm : dm) {
           for (LevelMetaData lm : hm) {
             boolean found = false;
             if (lm.getLogicalColumn() != null) {
               inner:
               for (AvailableField fm : availableOlapFields) {
                 if (fm.getLogicalColumn().getId().equals(lm.getLogicalColumn().getId()) || fm.getLogicalColumn().getName(workspaceHelper.getLocale()).equals(lm.getLogicalColumn().getName(workspaceHelper.getLocale()))) {
                   found = true;
                   break inner;
                 }
               }
             }
             if (!found) {
               lm.setLogicalColumn(null);
             }
           }
         }
       }
     } catch(Exception e){
       e.printStackTrace();
     }
 
     // If the new model was previously "auto-modeled" we need to clean that now
     LogicalModel newLModel = newDomain.getLogicalModels().get(0);
     if (newLModel != null) {
       List<OlapDimension> theDimensions = (List) newLModel.getProperty("olap_dimensions"); //$NON-NLS-1$
       if (theDimensions != null) {
         theDimensions.clear();
       }
       List<OlapCube> theCubes = (List) newLModel.getProperty("olap_cubes"); //$NON-NLS-1$
       if (theCubes != null) {
         theCubes.clear();
       }
     }
     
     // replace the domain with the new domain, which
     // makes sure the physical and logical columns are accurate
     domain = newDomain;
 
     model.validateTree();
     relationalModel.validateTree();
   }
 
 
   public String getDatabaseName() {
     return source.getDatabaseName();
   }
 
   @Bindable
   public String getSchemaName() {
     return schemaName;
   }
 
   @Bindable
   public void setSchemaName( String schemaName ) {
     this.schemaName = schemaName;
   }
 
   public void setAvailableFields(AvailableFieldCollection fields){
     this.availableFields = fields;
     firePropertyChange("availableFields", null, getAvailableFields()); //$NON-NLS-1$
 
   }
 
   public void setDomain( Domain d ) {
     setDomain(d, true);
   }
 
   // this method signature is intended to provide a simpler path for unit testing the upConvert method on it's own
   protected void setDomain(Domain d, boolean upConvertDesired) {
     this.domain = d;
     this.setModelIsChanging(true);
     this.setRelationalModelIsChanging(true);
     this.model.getDimensions().clear();
     this.model.getMeasures().clear();
     this.relationalModel.getCategories().clear();
     this.availableFields.clear();
     this.availableOlapFields.clear();
 
     boolean needsUpConverted = false;
     if (upConvertDesired) needsUpConverted = upConvertLegacyModel();
 
     // only show the columns from the non-olap specific tables (they are just duplicates)
     for (LogicalTable table : domain.getLogicalModels().get(0).getLogicalTables()) {
       for (LogicalColumn c : table.getLogicalColumns()) {
         AvailableField fm = new AvailableField();
         fm.setLogicalColumn(c);
         fm.setName(c.getPhysicalColumn().getName(workspaceHelper.getLocale()));
         fm.setDisplayName(c.getName(workspaceHelper.getLocale()));
         fm.setAggTypeDesc(c.getAggregationType().toString());
         if (table.getId().endsWith(BaseModelerWorkspaceHelper.OLAP_SUFFIX)) {
           availableOlapFields.add(fm);
         } else {
           availableFields.add(fm);
         }
       }
     }
 
     workspaceHelper.sortFields(availableFields);
     workspaceHelper.sortFields(availableOlapFields);
 
     firePropertyChange("availableFields", null, getAvailableFields()); //$NON-NLS-1$
 
     LogicalModel lModel = domain.getLogicalModels().get(0);
 
     setModelName(lModel.getName(workspaceHelper.getLocale()));
     setRelationalModelName(lModel.getName(workspaceHelper.getLocale()));
 
     List<OlapDimension> theDimensions = (List) lModel.getProperty(LogicalModel.PROPERTY_OLAP_DIMS); //$NON-NLS-1$
     if (theDimensions != null) {
       Iterator<OlapDimension> theDimensionItr = theDimensions.iterator();
       while (theDimensionItr.hasNext()) {
         OlapDimension theDimension = theDimensionItr.next();
 
         DimensionMetaData theDimensionMD = new DimensionMetaData(theDimension.getName());
 
         List<OlapHierarchy> theHierarchies = (List) theDimension.getHierarchies();
         Iterator<OlapHierarchy> theHierarchiesItr = theHierarchies.iterator();
         while (theHierarchiesItr.hasNext()) {
           OlapHierarchy theHierarchy = theHierarchiesItr.next();
           HierarchyMetaData theHierarchyMD = new HierarchyMetaData(theHierarchy.getName());
 
           List<OlapHierarchyLevel> theLevels = theHierarchy.getHierarchyLevels();
           Iterator<OlapHierarchyLevel> theLevelsItr = theLevels.iterator();
           while (theLevelsItr.hasNext()) {
             OlapHierarchyLevel theLevel = theLevelsItr.next();
             LevelMetaData theLevelMD = new LevelMetaData(theHierarchyMD, theLevel.getName());
 
             theLevelMD.setParent(theHierarchyMD);
             theLevelMD.setLogicalColumn(theLevel.getReferenceColumn());
             theHierarchyMD.add(theLevelMD);
           }
 
           theHierarchyMD.setParent(theDimensionMD);
           theDimensionMD.add(theHierarchyMD);
         }
         this.model.getDimensions().add(theDimensionMD);
       }
     }
 
     List<OlapCube> theCubes = (List) lModel.getProperty(LogicalModel.PROPERTY_OLAP_CUBES); //$NON-NLS-1$
     if (theCubes != null) {
       Iterator<OlapCube> theCubeItr = theCubes.iterator();
       while (theCubeItr.hasNext()) {
         OlapCube theCube = theCubeItr.next();
 
         List<OlapMeasure> theMeasures = theCube.getOlapMeasures();
         Iterator<OlapMeasure> theMeasuresItr = theMeasures.iterator();
         while (theMeasuresItr.hasNext()) {
           OlapMeasure theMeasure = theMeasuresItr.next();
 
           MeasureMetaData theMeasureMD = new MeasureMetaData(workspaceHelper.getLocale());
           theMeasureMD.setName(
               theMeasure.getLogicalColumn().getName(workspaceHelper.getLocale()));
           theMeasureMD.setFormat((String) theMeasure.getLogicalColumn().getProperty("mask")); //$NON-NLS-1$
           theMeasureMD.setAggTypeDesc(theMeasure.getLogicalColumn().getAggregationType().toString());
 
           theMeasureMD.setLogicalColumn(theMeasure.getLogicalColumn());
           this.model.getMeasures().add(theMeasureMD);
         }
       }
     }
 
     int i = 1;
 
     for (Category cat : this.getDomain().getLogicalModels().get(0).getCategories()) {
       String catName = cat.getName() != null ? cat.getName().getString(workspaceHelper.getLocale()) : "Category " + i++;
       CategoryMetaData catMeta = new CategoryMetaData(catName);
       for (LogicalColumn col : cat.getLogicalColumns()) {
         Object formatMask = col.getProperty("mask");
         String colName = col.getName(workspaceHelper.getLocale());
         AggregationType aggType = col.getAggregationType();
         FieldMetaData field = new FieldMetaData(catMeta,
             colName,
             formatMask == null ? null : formatMask.toString(),
             colName,
             workspaceHelper.getLocale());
         if (aggType != null) {
           field.setAggTypeDesc(aggType.name());
         } else {
           field.setAggTypeDesc(AggregationType.NONE.name());
         }
         field.setLogicalColumn(col);
         catMeta.add(field);
       }
       this.getRelationalModel().getCategories().add(catMeta);
     }
 
     if (needsUpConverted) upConvertMeasuresAndDimensions();
 
     this.setModelIsChanging(false, true);
 
   }
 
   private void upConvertMeasuresAndDimensions() {
     LogicalModel model = domain.getLogicalModels().get(0);
 
     // set the dimension logical column references to the new olap columns
     for (DimensionMetaData dim : getModel().getDimensions()) {
       for (HierarchyMetaData hier : dim) {
         for (LevelMetaData level : hier) {
           String olapColumnId = BaseModelerWorkspaceHelper.getCorrespondingOlapColumnId(level.getLogicalColumn());
           LogicalColumn olapCol = model.findLogicalColumn(olapColumnId);
           level.setLogicalColumn(olapCol);
         }
       }
     }
 
     // set the measure logical column references to the new olap columns
     for (MeasureMetaData measure : getModel().getMeasures()) {
       String olapColumnId = BaseModelerWorkspaceHelper.getCorrespondingOlapColumnId(measure.getLogicalColumn());
       LogicalColumn olapCol = model.findLogicalColumn(olapColumnId);
       measure.setLogicalColumn(olapCol);
     }
 
     return;
   }
 
   protected boolean upConvertLegacyModel() {
     // first, determine if we need to up-convert models created before
     // the separation of OLAP and Reporting models to the new style
     int olapTableCount=0, reportingTableCount=0;
     LogicalModel model = domain.getLogicalModels().get(0);
     for (LogicalTable table : model.getLogicalTables()) {
       if (table.getId().endsWith(BaseModelerWorkspaceHelper.OLAP_SUFFIX)) {
         olapTableCount++;
       } else {
         reportingTableCount++;
       }
     }
     if (olapTableCount == 0) {
       // need to forward port this model
       BaseModelerWorkspaceHelper.duplicateLogicalTablesForDualModelingMode(domain.getLogicalModels().get(0));
       return true;
     }
 
     return false;
   }
 
   public void resolveConnectionFromDomain() {
     // set up the datasource
     if (domain != null && source != null) {
       SqlPhysicalModel physicalModel = (SqlPhysicalModel) domain.getPhysicalModels().get(0);
       //TODO: resolve GWT DatabaseMeta databaseMeta = ThinModelConverter.convertToLegacy(physicalModel.getId(), physicalModel.getDatasource());
       //TODO: resolve GWT source.setDatabaseMeta(databaseMeta);
     }
 
   }
 
   public Domain getDomain() {
     return updateDomain();
   }
 
   private Domain updateDomain() {
     // TODO: update domain with changes
     return domain;
   }
 
   public void setModelIsChanging( boolean changing ) {
     setModelIsChanging(changing, true);
   }
 
   public void setModelIsChanging( boolean changing, boolean fireChanged ) {
     this.modelIsChanging = changing;
     if (!changing && fireChanged) {
       fireFieldsChanged();
       model.validateTree();
       isValid();
       fireModelChanged();
     }
     model.setSupressEvents(changing);
   }
 
   public void setRelationalModelIsChanging( boolean changing ) {
     setRelationalModelIsChanging(changing, true);
   }
 
   public void setRelationalModelIsChanging( boolean changing, boolean fireChanged ) {
     this.modelIsChanging = changing;
     if (!changing && fireChanged) {
       fireFieldsChanged();
       relationalModel.validateTree();
       isValid();
       fireRelationalModelChanged();
     }
     relationalModel.setSupressEvents(changing);
   }
 
   @Bindable
   public boolean isModelChanging() {
     return modelIsChanging;
   }
 
   @Bindable
   public void setTemporary( boolean isTempoarary ) {
     this.isTemporary = isTempoarary;
   }
 
   @Bindable
   public boolean isTemporary() {
     return this.isTemporary;
   }
 
   @Bindable
   public AbstractMetaDataModelNode getSelectedNode() {
     return selectedNode;
   }
 
   @Bindable
   public void setSelectedNode( AbstractMetaDataModelNode node ) {
     AbstractMetaDataModelNode prevVal = this.selectedNode;
     this.selectedNode = node;
     firePropertyChange("selectedNode", prevVal, node); //$NON-NLS-1$
   }
 
   @Bindable
   public AbstractMetaDataModelNode getSelectedRelationalNode() {
     return selectedRelationalNode;
   }
 
   @Bindable
   public void setSelectedRelationalNode( AbstractMetaDataModelNode node) {
     AbstractMetaDataModelNode prevVal = this.selectedRelationalNode;
     this.selectedRelationalNode = node;
     firePropertyChange("selectedRelationalNode", prevVal, node); //$NON-NLS-1$
   }
 
   public IModelerWorkspaceHelper getWorkspaceHelper() {
     return workspaceHelper;
   }
 
   public void setWorkspaceHelper( IModelerWorkspaceHelper workspaceHelper ) {
     this.workspaceHelper = workspaceHelper;
   }
 
   public ModelerMode getCurrentModellingMode() {
     return currentModellingMode;
   }
 
   public void setCurrentModellingMode(ModelerMode currentModellingMode) {
     this.currentModellingMode = currentModellingMode;
   }
 
   public ModelerPerspective getCurrentModelerPerspective() {
     return currentModelerPerspective;
   }
 
   public void setCurrentModelerPerspective(ModelerPerspective currentModelerPerspective) {
     this.currentModelerPerspective = currentModelerPerspective;
   }
 }
