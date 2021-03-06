 /*
  * To change this template, choose Tools | Templates
  * and open the template in the editor.
  */
 
 package edu.harvard.iq.dvn.core.web.study;
 
 import edu.harvard.iq.dvn.core.study.DataTable;
 import com.icesoft.faces.component.ext.HtmlDataTable;
 import com.icesoft.faces.component.ext.HtmlInputText;
 import com.icesoft.faces.component.ext.HtmlCommandLink;
 import com.icesoft.faces.component.ext.HtmlSelectOneMenu;
 import edu.harvard.iq.dvn.core.study.DataVariable;
 import edu.harvard.iq.dvn.core.study.EditStudyFilesService;
 import edu.harvard.iq.dvn.core.study.Metadata;
 import edu.harvard.iq.dvn.core.study.Study;
 import edu.harvard.iq.dvn.core.study.StudyFile;
 import edu.harvard.iq.dvn.core.study.VariableServiceLocal;
 import edu.harvard.iq.dvn.core.visualization.DataVariableMapping;
 import edu.harvard.iq.dvn.core.visualization.VarGroup;
 import edu.harvard.iq.dvn.core.visualization.VarGroupType;
 import edu.harvard.iq.dvn.core.visualization.VarGrouping;
 import edu.harvard.iq.dvn.core.visualization.VarGrouping.GroupingType;
 import edu.harvard.iq.dvn.core.visualization.VisualizationServiceLocal;
 import edu.harvard.iq.dvn.core.web.VarGroupTypeUI;
 import edu.harvard.iq.dvn.core.web.VarGroupUI;
 import edu.harvard.iq.dvn.core.web.VarGroupingUI;
 import edu.harvard.iq.dvn.core.web.common.VDCBaseBean;
 import java.util.ArrayList;
 import java.util.Iterator;
 import java.util.List;
 import javax.ejb.EJB;
 import javax.faces.application.FacesMessage;
 import javax.faces.context.FacesContext;
 import javax.faces.event.ValueChangeEvent;
 import javax.faces.model.SelectItem;
 import javax.naming.Context;
 import javax.naming.InitialContext;
 import javax.naming.NamingException;
 
 /**
  *
  * @author skraffmiller
  */
 @EJB(name="visualization", beanInterface=edu.harvard.iq.dvn.core.visualization.VisualizationServiceBean.class)
 public class SetUpDataExplorationPage extends VDCBaseBean implements java.io.Serializable {
     @EJB
     VisualizationServiceLocal      visualizationService;
     @EJB
     VariableServiceLocal varService;
     private EditStudyFilesService editStudyFilesService;
     private List <VarGrouping> varGroupings = new ArrayList();;
     private List <DataVariable> dvList = new ArrayList();;
     private List <VarGroup> measureGroups = new ArrayList();;
     private List <VarGroupType> measureGroupTypes = new ArrayList();;
     private VarGroupingUI measureGrouping = new VarGroupingUI();
     private List <VarGroupingUI> filterGroupings = new ArrayList();
     private List <SelectItem> measureGroupTypesUI = new ArrayList();
     private List <SelectItem> filterGroupTypesUI = new ArrayList();
     private List <SelectItem> dataVariableSelectItems = new ArrayList();
     private List <SelectItem> studyFileIdSelectItems = new ArrayList();
 
     private DataVariable xAxisVariable = new DataVariable();
     private Long xAxisVariableId =  new Long(0);
 
 
     private Study study;
     private Long studyFileId = new Long(0);
     private Long studyId ;
     private Metadata metadata;
     private String currentTitle;
     DataTable dataTable;
 
     public SetUpDataExplorationPage(){
         
     }
 
     public void init() {
         super.init();
 
          studyId = new Long( getVDCRequestBean().getRequestParam("studyId"));
 
 
         try {
             Context ctx = new InitialContext();
             editStudyFilesService = (EditStudyFilesService) ctx.lookup("java:comp/env/editStudyFiles");
 
         } catch (NamingException e) {
             e.printStackTrace();
             FacesContext context = FacesContext.getCurrentInstance();
             FacesMessage errMessage = new FacesMessage(FacesMessage.SEVERITY_ERROR, e.getMessage(), null);
             context.addMessage(null, errMessage);
 
         }
         if (getStudyId() != null) {
             editStudyFilesService.setStudyVersion(studyId);
             study = editStudyFilesService.getStudyVersion().getStudy();
 
             metadata = editStudyFilesService.getStudyVersion().getMetadata();
             currentTitle = metadata.getTitle();
 
             setFiles(editStudyFilesService.getCurrentFiles());
         }
         else {
 
             FacesContext context = FacesContext.getCurrentInstance();
 
             FacesMessage errMessage = new FacesMessage(FacesMessage.SEVERITY_ERROR, "The Study ID is null", null);
             context.addMessage(null, errMessage);
             //Should not get here.
             //Must always be in a study to get to this page.
         }
 
 
          studyFileIdSelectItems = loadStudyFileSelectItems();
 /*
 
  * 
  */
     }
 
 
     public void resetStudyFileId(){
 
         Object value= this.selectStudyFile.getValue();
         studyFileId = (Long) value;
         if (!studyFileId.equals(new Long(0))) {
             loadDataTable();
         }
 
     }
 
     private void loadDataTable(){
 
     dataTable = new DataTable();
          visualizationService.setDataTableFromStudyFileId(studyFileId);
          dataTable = visualizationService.getDataTable();
          varGroupings = dataTable.getVarGroupings();
          study = visualizationService.getStudyFromStudyFileId(studyFileId);
          dvList = dataTable.getDataVariables();
          measureGroupTypes = loadSelectMeasureGroupTypes();
          measureGroupTypesUI = loadSelectItemGroupTypes(GroupingType.MEASURE);
          loadMeasureGroupUIs();
          xAxisVariable = visualizationService.getXAxisVariable(dataTable.getId());
          xAxisVariableId = xAxisVariable.getId();
          if (measureGrouping.getVarGrouping() == null){
              addMeasureGrouping();
          }
          dataVariableSelectItems = loadSelectItemDataVariables();
          loadFilterGroupings();
     }
 
 
 
 
     public List<VarGrouping> getVarGroupings() {
         return varGroupings;
     }
 
     public void setVarGroupings(List<VarGrouping> varGroupings) {
         this.varGroupings = varGroupings;        
     }
     
 
     public List<VarGroupType> loadSelectMeasureGroupTypes() {
 
         Iterator iterator = varGroupings.iterator();
         while (iterator.hasNext() ){
             VarGrouping varGrouping = (VarGrouping) iterator.next();
             // Don't show OAISets that have been created for dataverse-level Lockss Harvesting
             if (varGrouping.getGroupingType().equals(GroupingType.MEASURE)){
                 measureGrouping.setVarGrouping(varGrouping);
                 return (List<VarGroupType>) varGrouping.getVarGroupTypes();
             }
         }
         
         return null;
     }
 
 
     public void loadFilterGroupings() {
         filterGroupings.clear();
         Iterator iterator = varGroupings.iterator();
         while (iterator.hasNext() ){
             VarGrouping varGrouping = (VarGrouping) iterator.next();
             
             if (varGrouping.getGroupingType().equals(GroupingType.FILTER)){
                 VarGroupingUI varGroupingUI = new VarGroupingUI();
                 varGroupingUI.setVarGrouping(varGrouping);
                 varGroupingUI.setVarGroupTypesUI(varGroupingUI);
                 setVarGroupUI(varGroupingUI);
                 filterGroupings.add(varGroupingUI);
             }
         }
       
     }
 
     public void setMeasureGroups(){
         
     }
 
     public void setMeasureGroupTypes(){
 
     }
 
     public List<VarGroupType> getMeasureGroupTypes() {
         if (varGroupings == null) return null;
         Iterator iterator = varGroupings.iterator();
         while (iterator.hasNext() ){
             VarGrouping varGrouping = (VarGrouping) iterator.next();
             // Don't show OAISets that have been created for dataverse-level Lockss Harvesting
             if (varGrouping.getGroupingType().equals(GroupingType.MEASURE)){
 
                 return (List<VarGroupType>) varGrouping.getVarGroupTypes();
             }
         }
         return null;
     }
 
     public void loadMeasureGroupUIs (){
         List <VarGroupUI> returnList = new ArrayList();
         Iterator iterator = varGroupings.iterator();
         while (iterator.hasNext() ){
             VarGrouping varGrouping = (VarGrouping) iterator.next();
            
             if (varGrouping.getGroupingType().equals(GroupingType.MEASURE)){
                 List <VarGroup> localMeasureGroups = (List<VarGroup>) varGrouping.getVarGroups();
                 for(VarGroup varGroup: localMeasureGroups) {
 
                    VarGroupUI varGroupUI = new VarGroupUI();
                    varGroupUI.setVarGroup(varGroup);
                    varGroupUI.setVarGroupTypesSelected(new ArrayList());
                    varGroupUI.setVarGroupTypesSelectItems(new ArrayList());
                    varGroupUI.setDataVariablesSelected(new ArrayList());
                    List <VarGroupType> varGroupTypes = new ArrayList();
                    varGroupTypes = (List<VarGroupType>) varGroupUI.getVarGroup().getGroupTypes();
                     if (varGroupTypes !=null ) {
                        for(VarGroupType varGroupType: varGroupTypes){
                            VarGroupTypeUI varGroupTypeUI = new VarGroupTypeUI();
                            varGroupTypeUI.setVarGroupType(varGroupType);
                            varGroupTypeUI.setEnabled(true);
                            varGroupTypeUI.getVarGroupType().getName();
                            varGroupType.getName();
                            varGroupUI.getVarGroupTypesSelected().add(varGroupTypeUI);
                            varGroupUI.getVarGroupTypesSelectItems().add(new Long(varGroupType.getId()));
 
                        }
                     }
 
                     List <DataVariable> dataVariables = new ArrayList();
                     dataVariables = (List<DataVariable>) visualizationService.getDataVariableMappingsFromGroupId(varGroupUI.getVarGroup().getId());
                     if (dataVariables !=null ) {
                        for(DataVariable dataVariable: dataVariables){
 
                            varGroupUI.getDataVariablesSelected().add(new Long(dataVariable.getId()));
 
                        }
                     }
 
 
 
                    returnList.add(varGroupUI);
 
                 }
                 measureGrouping.setVarGroupUI(returnList);
 
             }
         }
 
     }
 
     public List<VarGroupUI> getMeasureGroups() {
         return (List) measureGrouping.getVarGroupUI();
     }
 
     public VarGroupingUI getMeasureGrouping() {
         return measureGrouping;
     }
 
     public void deleteMeasureGroup(){
         HtmlDataTable dataTable2 = dataTableVarGroup;
        if (dataTable2.getRowCount()>0) {
             VarGroupUI varGroupUI2 = (VarGroupUI) dataTable2.getRowData();
             VarGroup varGroup = varGroupUI2.getVarGroup();
             List varGroupList = (List) dataTable2.getValue();
             Iterator iterator = varGroupList.iterator();
             List deleteList = new ArrayList();
             while (iterator.hasNext() ){
                 VarGroupUI varGroupUI = (VarGroupUI) iterator.next();
                 VarGroup data = varGroupUI.getVarGroup();
                 deleteList.add(data);
             }
             visualizationService.removeCollectionElement(deleteList,dataTable2.getRowIndex());
             measureGrouping.getVarGroupUI().remove(varGroupUI2);
             measureGrouping.getVarGrouping().getVarGroups().remove(varGroup);
         }
     }
 
     public void deleteFilterGroup(){
         HtmlDataTable dataTable2 = dataTableFilterGroup;
        if (dataTable2.getRowCount()>0) {
             VarGroupUI varGroupUI2 = (VarGroupUI) dataTable2.getRowData();
             VarGroup varGroup = varGroupUI2.getVarGroup();
             List varGroupList = (List) dataTable2.getValue();
             Iterator iterator = varGroupList.iterator();
             List deleteList = new ArrayList();
             while (iterator.hasNext() ){
                 VarGroupUI varGroupUI = (VarGroupUI) iterator.next();
                 VarGroup data = varGroupUI.getVarGroup();
                 deleteList.add(data);
             }
             visualizationService.removeCollectionElement(deleteList,dataTable2.getRowIndex());
                       for(VarGroupingUI varGroupingUI: filterGroupings){
              if (varGroupingUI.getVarGrouping().equals(varGroup.getGroupAssociation())){
                 varGroupingUI.getVarGroupUI().remove(varGroupUI2);
                 varGroupingUI.getVarGrouping().getVarGroups().remove(varGroup);
 
              }
          }
 
         }
     }
 
     public void deleteFilterGroupType(){
         HtmlDataTable dataTable2 = dataTableFilterGroupType;
        if (dataTable2.getRowCount()>0) {
             VarGroupTypeUI varGroupTypeUI2 = (VarGroupTypeUI) dataTable2.getRowData();
             VarGroupType varGroupType = varGroupTypeUI2.getVarGroupType();
             List varGroupTypeList = (List) dataTable2.getValue();
             Iterator iterator = varGroupTypeList.iterator();
             List deleteList = new ArrayList();
             while (iterator.hasNext() ){
                 VarGroupTypeUI varGroupTypeUI = (VarGroupTypeUI) iterator.next();
                 VarGroupType data = varGroupTypeUI.getVarGroupType();
                 deleteList.add(data);
             }
             visualizationService.removeCollectionElement(deleteList,dataTable2.getRowIndex());
              for(VarGroupingUI varGroupingUI: filterGroupings){
              if (varGroupingUI.getVarGrouping().equals(varGroupType.getVarGrouping())){
                 varGroupingUI.getVarGroupTypesUI().remove(varGroupTypeUI2);
                 varGroupingUI.getVarGrouping().getVarGroupTypes().remove(varGroupType);
 
              }
          }
 
         }
     }
 
     public void deleteMeasureGroupType(){
         HtmlDataTable dataTable2 = dataTableVarGroupType;
        if (dataTable2.getRowCount()>0) {
             List data = (List)dataTable2.getValue();
             visualizationService.removeCollectionElement(data,dataTable2.getRowIndex());
         }
     }
 
     public void addMeasureGroup() {
 
         VarGroupUI newElem = new VarGroupUI();
         newElem.setVarGroup(new VarGroup());
         newElem.getVarGroup().setGroupAssociation(measureGrouping.getVarGrouping());
         newElem.setDvMappings(null);
         newElem.getVarGroup().setGroupTypes(new ArrayList());
         int i = measureGrouping.getVarGrouping().getVarGroups().size();
         measureGrouping.getVarGrouping().getVarGroups().add(i,  newElem.getVarGroup());
         measureGrouping.getVarGroupUI().add(newElem);
         
     }
 
     public void addMeasureGroupType() {
         VarGroupType newElem = new VarGroupType();
         newElem.setVarGrouping(measureGrouping.getVarGrouping());
         measureGrouping.getVarGrouping().getVarGroupTypes().add(newElem);
     }
 
     public void addFilterGroup() {
 
         VarGroupUI newElem = new VarGroupUI();
         newElem.setVarGroup(new VarGroup());
         newElem.getVarGroup().setGroupTypes(new ArrayList());
         Long varGroupingId = (Long) getAddFilterGroupLink().getValue();
 
         for(VarGrouping varGrouping: varGroupings){
              if (varGrouping.getId() == varGroupingId){
                 newElem.getVarGroup().setGroupAssociation(varGrouping);
                 newElem.setDvMappings(null);
                 int i = varGrouping.getVarGroups().size();
                 varGrouping.getVarGroups().add(i,  newElem.getVarGroup());
                 
              }
          }
           for(VarGroupingUI varGroupingUI: filterGroupings){
              if (varGroupingUI.getVarGrouping().getId() == varGroupingId){
                 varGroupingUI.getVarGroupUI().add(newElem);
 
              }
          }
     }
 
     public void addFilterGroupType() {
         VarGroupType newElem = new VarGroupType();
 
         Long varGroupingId = (Long) getAddFilterGroupLink().getValue();
         for(VarGrouping varGrouping: varGroupings){
              if (varGrouping.getId() == varGroupingId){
                 newElem.setVarGrouping(varGrouping);
 
                 int i = varGrouping.getVarGroupTypes().size();
                 varGrouping.getVarGroupTypes().add(newElem);
 
              }
          }
          for(VarGroupingUI varGroupingUI: filterGroupings){
              if (varGroupingUI.getVarGrouping().getId() == varGroupingId){
                  VarGroupTypeUI varGroupTypeUI = new VarGroupTypeUI();
                  varGroupTypeUI.setVarGroupType(newElem);
                 varGroupingUI.getVarGroupTypesUI().add(varGroupTypeUI);
 
              }
          }
     }
 
     public void addMeasureGrouping() {
         VarGrouping varGrouping = new VarGrouping();
         varGrouping.setGroupingType(GroupingType.MEASURE);
         varGrouping.setDataTable(dataTable);
         varGrouping.setDataVariableMappings(new ArrayList());
         varGrouping.setGroups(new ArrayList());
         varGrouping.setVarGroupTypes(new ArrayList());
 
         measureGrouping.setVarGrouping(varGrouping);
         measureGrouping.setSelectedGroupId(new Long(0));
         measureGrouping.setVarGroupTypesUI(new ArrayList());
         measureGrouping.setVarGroupUI(new ArrayList());
         measureGroupTypes = loadSelectMeasureGroupTypes();
         measureGroupTypesUI = loadSelectItemGroupTypes(GroupingType.MEASURE);
 
         dataTable.getVarGroupings().add(varGrouping);
 
         measureGrouping.setVarGrouping(varGrouping);
 
 
     }
 
     public void addFilterGrouping() {
         VarGrouping varGrouping = new VarGrouping();
 
         varGrouping.setGroupingType(GroupingType.FILTER);
         varGrouping.setDataTable(dataTable);
         varGrouping.setDataVariableMappings(new ArrayList());
         varGrouping.setGroups(new ArrayList());
         varGrouping.setVarGroupTypes(new ArrayList());
 
         dataTable.getVarGroupings().add(varGrouping);
         loadFilterGroupings();
     }
 
 
 
     public String cancel(){
         visualizationService.cancel();
         return "editStudyFiles";
     }
 
     public String save() {
 
         
         List <DataVariableMapping> removeList = new ArrayList();
         List <DataVariable> tempList = new ArrayList(dvList);
            for(DataVariable dataVariable: tempList){
                List <DataVariableMapping> deleteList = (List <DataVariableMapping>) dataVariable.getDataVariableMappings();
                 for (DataVariableMapping dataVariableMapping : deleteList ){
                     removeList.add(dataVariableMapping);
                      }
              }
 
 
            for(DataVariableMapping dataVarMappingRemove : removeList){
                
                visualizationService.removeCollectionElement(dataVarMappingRemove.getDataVariable().getDataVariableMappings(),dataVarMappingRemove);
            }
 
            if(!xAxisVariableId.equals(new Long(0))){
 
                for(DataVariable dataVariable: dvList){
                     if (dataVariable.getId() !=null &&  dataVariable.getId().equals(xAxisVariableId)){
                          DataVariableMapping dataVariableMapping = new DataVariableMapping();
                          dataVariableMapping.setDataTable(dataTable);
                          dataVariableMapping.setDataVariable(dataVariable);
                          dataVariableMapping.setGroup(null);
                          dataVariableMapping.setVarGrouping(null);
                          dataVariableMapping.setX_axis(true);
                          dataVariable.getDataVariableMappings().add(dataVariableMapping);
                      }
 
               }
 
            }
 
         List <VarGroupUI> measureVarGroupsUI  = (List) measureGrouping.getVarGroupUI();
         if (measureVarGroupsUI !=null && measureGroupTypes != null ) {
          for(VarGroupUI varGroupUI: measureVarGroupsUI){
              VarGroup varGroup = varGroupUI.getVarGroup();
              List  groupTypeIds = varGroupUI.getVarGroupTypesSelectItems();
              varGroup.getGroupTypes().clear();
              for(Object stringUI: groupTypeIds){
                  String id = stringUI.toString();
                  for (VarGroupType varGroupType: measureGroupTypes){
 
                      if (varGroupType.getId() !=null &&  varGroupType.getId().equals(new Long(id))){
                          varGroup.getGroupTypes().add(varGroupType);
                      }                        
                  }
              }
              List  dataVariableIds = varGroupUI.getDataVariablesSelected();
 
              for(Object dataVariableId:  dataVariableIds){
                  String id = dataVariableId.toString();
                 for(DataVariable dataVariable: dvList){
                     if (dataVariable.getId() !=null &&  dataVariable.getId().equals(new Long(id))){
                          DataVariableMapping dataVariableMapping = new DataVariableMapping();
                          dataVariableMapping.setDataTable(dataTable);
                          dataVariableMapping.setDataVariable(dataVariable);
                          dataVariableMapping.setGroup(varGroup);
                          dataVariableMapping.setVarGrouping(measureGrouping.getVarGrouping());
                          dataVariableMapping.setX_axis(false);
                          dataVariable.getDataVariableMappings().add(dataVariableMapping);
                      }
 
               }
              }
 
          }
         }
         for(VarGroupingUI varGroupingUI: filterGroupings){
             List <VarGroupUI> filterVarGroupsUI  = (List) varGroupingUI.getVarGroupUI();
             if (filterVarGroupsUI !=null && varGroupingUI.getVarGroupTypesUI() != null ) {
             for(VarGroupUI varGroupUI: filterVarGroupsUI){
                 VarGroup varGroup = varGroupUI.getVarGroup();
                 List  groupTypeIds = varGroupUI.getVarGroupTypesSelectItems();
                 varGroup.getGroupTypes().clear();
                 for(Object stringUI: groupTypeIds){
                     String id = stringUI.toString();
                     for (VarGroupType varGroupType: varGroupingUI.getVarGrouping().getVarGroupTypes()){
                         if (varGroupType.getId() !=null &&  varGroupType.getId().equals(new Long(id))){
                             varGroup.getGroupTypes().add(varGroupType);
                         }
                     }
                 }
 
                              List  dataVariableIds = varGroupUI.getDataVariablesSelected();
 
              for(Object dataVariableId:  dataVariableIds){
                  String id = dataVariableId.toString();
                 for(DataVariable dataVariable: dvList){
                     if (dataVariable.getId() !=null &&  dataVariable.getId().equals(new Long(id))){
                          DataVariableMapping dataVariableMapping = new DataVariableMapping();
                          dataVariableMapping.setDataTable(dataTable);
                          dataVariableMapping.setDataVariable(dataVariable);
                          dataVariableMapping.setGroup(varGroup);
                          dataVariableMapping.setVarGrouping(varGroupingUI.getVarGrouping());
                          dataVariableMapping.setX_axis(false);
                          dataVariable.getDataVariableMappings().add(dataVariableMapping);
                      }
 
               }
              }
             }
             }
         }
 
 
         visualizationService.saveAll();
        return "editStudyFiles";
     }
 
     private HtmlInputText inputMeasureName;
 
     public HtmlInputText getInputMeasureName() {
         return this.inputMeasureName;
     }
     public void setInputMeasureName(HtmlInputText inputMeasureName) {
         this.inputMeasureName = inputMeasureName;
     }
 
     private HtmlInputText inputMeasureUnits;
 
     public HtmlInputText getInputMeasureUnits() {
         return this.inputMeasureUnits;
     }
     public void setInputMeasureUnits(HtmlInputText inputMeasureUnits) {
         this.inputMeasureUnits = inputMeasureUnits;
     }
 
 
     private HtmlInputText inputGroupTypeName;
 
     public HtmlInputText getInputGroupTypeName() {
         return this.inputGroupTypeName;
     }
     public void setInputGroupTypeName(HtmlInputText inputGroupTypeName) {
         this.inputGroupTypeName = inputGroupTypeName;
     }
 
     public List<SelectItem> loadSelectItemGroupTypes(GroupingType groupingType){
         List selectItems = new ArrayList<SelectItem>();
 
         Iterator iterator = varGroupings.iterator();
         while (iterator.hasNext() ){
             VarGrouping varGrouping = (VarGrouping) iterator.next();
             // Don't show OAISets that have been created for dataverse-level Lockss Harvesting
             if (varGrouping.getGroupingType().equals(groupingType)){
                 selectItems.add(new SelectItem(0, "Select an Issue..."));
                 List <VarGroupType> varGroupTypes = (List<VarGroupType>) varGrouping.getVarGroupTypes();
                 for(VarGroupType varGroupType: varGroupTypes) {
                     selectItems.add(new SelectItem(varGroupType.getId(), varGroupType.getName()));
                 }
             }
         }
         return selectItems;
     }
 
         public List<SelectItem> loadStudyFileSelectItems(){
         List selectItems = new ArrayList<SelectItem>();
         selectItems.add(new SelectItem(0, "Select a File"));
         Iterator iterator = study.getStudyFiles().iterator();
         while (iterator.hasNext() ){
             StudyFile studyFile = (StudyFile) iterator.next();
             // Don't show OAISets that have been created for dataverse-level Lockss Harvesting
             if (studyFile.isSubsettable()){
 
                     selectItems.add(new SelectItem(studyFile.getId(), studyFile.getFileName()));
             }
         }
         return selectItems;
     }
 
     private void setVarGroupUI(VarGroupingUI varGroupingUI) {
         List<VarGroupUI> varGroupUIList = new ArrayList();
         VarGrouping varGroupingIn = varGroupingUI.getVarGrouping();
         varGroupingIn.getVarGroupTypes();
 
                    List <VarGroup> varGroups = new ArrayList();
                    varGroups = (List<VarGroup>) varGroupingIn.getVarGroups();
                     if (varGroups !=null ) {
                        for(VarGroup varGroup: varGroups){
                            VarGroupUI varGroupUILoc = new VarGroupUI();
                            varGroupUILoc.setVarGroup(varGroup);
                            varGroupUILoc.getVarGroup().getName();
                            varGroupUILoc.setVarGroupTypesSelected(new ArrayList());
                            varGroupUILoc.setVarGroupTypesSelectItems(new ArrayList());
                            varGroupUILoc.setDataVariablesSelected(new ArrayList());
                            List <VarGroupType> varGroupTypes = new ArrayList();
 
                            List <DataVariable> dataVariables = new ArrayList();
                            if (visualizationService.getDataVariableMappingsFromGroupId(varGroup.getId()) != null)
                             dataVariables = (List<DataVariable>) visualizationService.getDataVariableMappingsFromGroupId(varGroup.getId());
                             if (dataVariables !=null ) {
                                 for(DataVariable dataVariable: dataVariables){
 
                                     varGroupUILoc.getDataVariablesSelected().add(new Long(dataVariable.getId()));
 
                                 }
                             }
                            varGroupTypes = (List<VarGroupType>) varGroupUILoc.getVarGroup().getGroupTypes();
                     if (varGroupTypes !=null ) {
                        for(VarGroupType varGroupType: varGroupTypes){
                            VarGroupTypeUI varGroupTypeUI = new VarGroupTypeUI();
                            varGroupTypeUI.setVarGroupType(varGroupType);
                            varGroupTypeUI.setEnabled(true);
                            varGroupTypeUI.getVarGroupType().getName();
                            varGroupType.getName();
                            varGroupUILoc.getVarGroupTypesSelected().add(varGroupTypeUI);
                            varGroupUILoc.getVarGroupTypesSelectItems().add(new Long(varGroupType.getId()));
 
                        }
                     }
                            varGroupUIList.add(varGroupUILoc);
                        }
                     }
 
         varGroupingUI.setVarGroupUI(varGroupUIList) ;
     }
 
     public List<SelectItem> loadSelectItemDataVariables(){
         List selectItems = new ArrayList<SelectItem>();
 
         Iterator iterator = dvList.iterator();
         while (iterator.hasNext() ){
             DataVariable dataVariable = (DataVariable) iterator.next();
             selectItems.add(new SelectItem(dataVariable.getId(), dataVariable.getName()));
         }
         return selectItems;
     }
     public List<SelectItem> getSelectMeasureGroupTypes() {
 
         return measureGroupTypesUI;
     }
 
     public List<SelectItem> getSelectFilterGroupTypes() {
 
         return filterGroupTypesUI;
     }
 
     private HtmlDataTable dataTableVarGroup;
 
     public HtmlDataTable getDataTableVarGroup() {
         return this.dataTableVarGroup;
     }
 
     public void setDataTableVarGroup(HtmlDataTable dataTableVarGroup) {
         this.dataTableVarGroup = dataTableVarGroup;
     }
 
     private HtmlDataTable dataTableXAxis;
 
     public HtmlDataTable getDataTableXAxis() {
         return this.dataTableXAxis;
     }
 
     public void setDataTableXAxis(HtmlDataTable dataTableXAxisXAxis) {
         this.dataTableXAxis = dataTableXAxis;
     }
 
     private HtmlDataTable dataTableFilterGroup;
 
     public HtmlDataTable getDataTableFilterGroup() {
         return this.dataTableFilterGroup;
     }
 
     public void setDataTableFilterGroup(HtmlDataTable dataTableFilterGroup) {
         this.dataTableFilterGroup = dataTableFilterGroup;
     }
 
         /**
      * Holds value of property dataTableVarGroup.
      */
     private HtmlDataTable dataTableVarGroupType;
 
     /**
      * Getter for property dataTableVarGroup.
      * @return Value of property dataTableVarGroup.
      */
     public HtmlDataTable getDataTableVarGroupType() {
         return this.dataTableVarGroupType;
     }
 
 
     public void setDataTableVarGroupType(HtmlDataTable dataTableVarGroupType) {
         this.dataTableVarGroupType = dataTableVarGroupType;
     }
 
     private HtmlDataTable dataTableFilterGroupType;
 
 
     public HtmlDataTable getDataTableFilterGroupType() {
         return this.dataTableFilterGroupType;
     }
 
 
     public void setDataTableFilterGroupType(HtmlDataTable dataTableFilterGroupType) {
         this.dataTableFilterGroupType = dataTableFilterGroupType;
     }
     public List<VarGroupingUI> getFilterGroupings() {
         return filterGroupings;
     }
 
     public void setFilterGroupings(List<VarGroupingUI> filterGroupings) {
         this.filterGroupings = filterGroupings;
     }
 
 
     private HtmlDataTable dataTableFilterGroupings;
 
     public HtmlDataTable getDataTableFilterGroupings() {
         return dataTableFilterGroupings;
     }
 
     public void setDataTableFilterGroupings(HtmlDataTable dataTableFilterGroupings) {
         this.dataTableFilterGroupings = dataTableFilterGroupings;
     }
 
     private HtmlInputText inputFilterGroupingName;
 
     public HtmlInputText getInputFilterGroupingName() {
         return this.inputFilterGroupingName;
     }
     public void setInputFilterGroupingName(HtmlInputText inputFilterGroupingName) {
         this.inputFilterGroupingName = inputFilterGroupingName;
     }
 
 
 
     private HtmlDataTable dataTableFilterGroups;
 
     public HtmlDataTable getDataTableFilterGroups() {
         return dataTableFilterGroups;
     }
 
     public void setDataTableFilterGroups(HtmlDataTable dataTableFilterGroups) {
         this.dataTableFilterGroups = dataTableFilterGroups;
     }
 
     private HtmlDataTable dataTableFilterGroupTypes;
 
     public HtmlDataTable getDataTableFilterGroupTypes() {
         return dataTableFilterGroupTypes;
     }
 
     public void setDataTableFilterGroupTypes(HtmlDataTable dataTableFilterGroupTypes) {
         this.dataTableFilterGroupTypes = dataTableFilterGroupTypes;
     }
 
     private HtmlInputText inputFilterGroupTypeName;
 
     public HtmlInputText getInputFilterGroupTypeName() {
         return this.inputFilterGroupTypeName;
     }
     public void setInputFilterGroupTypeName(HtmlInputText inputFilterGroupTypeName) {
         this.inputFilterGroupTypeName = inputFilterGroupTypeName;
     }
 
     private HtmlInputText inputFilterGroupName;
 
     public HtmlInputText getInputFilterGroupName() {
         return this.inputFilterGroupName;
     }
     public void setInputFilterGroupName(HtmlInputText inputFilterGroupName) {
         this.inputFilterGroupName = inputFilterGroupName;
     }
 
     private HtmlCommandLink addFilterGroupLink;
 
     public HtmlCommandLink getAddFilterGroupLink() {
         return this.addFilterGroupLink;
     }
     public void setAddFilterGroupLink(HtmlCommandLink addFilterGroupLink) {
         this.addFilterGroupLink = addFilterGroupLink;
     }
     private HtmlCommandLink addFilterGroupTypeLink;
 
     public HtmlCommandLink getAddFilterGroupTypeLink() {
         return this.addFilterGroupTypeLink;
     }
     public void setAddFilterGroupTypeLink(HtmlCommandLink addFilterGroupTypeLink) {
         this.addFilterGroupTypeLink = addFilterGroupTypeLink;
     }
 
     public List<SelectItem> getDataVariableSelectItems() {
         return dataVariableSelectItems;
     }
 
     public void setDataVariableSelectItems(List<SelectItem> dataVariableSelectItems) {
         this.dataVariableSelectItems = dataVariableSelectItems;
     }
 
     public DataVariable getxAxisVariable() {
         return xAxisVariable;
     }
 
     public void setxAxisVariable(DataVariable xAxisVariable) {
         this.xAxisVariable = xAxisVariable;
     }
 
     public Study getStudy() {
         return study;
     }
 
     public void setStudy(Study study) {
         this.study = study;
     }
 
     public Long getStudyFileId() {
         return studyFileId;
     }
 
     public void setStudyFileId(Long studyFileId) {
         this.studyFileId = studyFileId;
     }
 
     private List files;
 
     public List getFiles() {
         return files;
     }
 
     public void setFiles(List files) {
         this.files = files;
     }
 
     public List<SelectItem> getStudyFileIdSelectItems() {
         return studyFileIdSelectItems;
     }
 
     public void setStudyFileIdSelectItems(List<SelectItem> studyFileIdSelectItems) {
         this.studyFileIdSelectItems = studyFileIdSelectItems;
     }
 
     public Long getStudyId() {
         return studyId;
     }
 
     public void setStudyId(Long studyId) {
         this.studyId = studyId;
     }
 
     public Long getxAxisVariableId() {
         return xAxisVariableId;
     }
 
     public void setxAxisVariableId(Long xAxisVariableId) {
         this.xAxisVariableId = xAxisVariableId;
     }
 
     HtmlSelectOneMenu selectStudyFile;
 
     public HtmlSelectOneMenu getSelectStudyFile() {
         return selectStudyFile;
     }
 
     public void setSelectStudyFile(HtmlSelectOneMenu selectStudyFile) {
         this.selectStudyFile = selectStudyFile;
     }
 
 }
