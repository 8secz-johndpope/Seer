 /*******************************************************************************
  * Copyright 2008(c) The OBiBa Consortium. All rights reserved.
  * 
  * This program and the accompanying materials
  * are made available under the terms of the GNU Public License v3.0.
  * 
  * You should have received a copy of the GNU General Public License
  * along with this program.  If not, see <http://www.gnu.org/licenses/>.
  ******************************************************************************/
 package org.obiba.opal.web.gwt.app.client.wizard.importvariables.view;
 
 import java.util.ArrayList;
 import java.util.List;
 
 import org.obiba.opal.web.gwt.app.client.i18n.Translations;
 import org.obiba.opal.web.gwt.app.client.js.JsArrayDataProvider;
 import org.obiba.opal.web.gwt.app.client.js.JsArrays;
 import org.obiba.opal.web.gwt.app.client.widgets.celltable.VariableAttributeColumn;
 import org.obiba.opal.web.gwt.app.client.wizard.importvariables.presenter.ComparedDatasourcesReportStepPresenter;
 import org.obiba.opal.web.gwt.app.client.workbench.view.BreadCrumbTabLayout;
 import org.obiba.opal.web.gwt.app.client.workbench.view.HorizontalTabLayout;
 import org.obiba.opal.web.gwt.app.client.workbench.view.Table;
 import org.obiba.opal.web.model.client.magma.ConflictDto;
 import org.obiba.opal.web.model.client.magma.TableCompareDto;
 import org.obiba.opal.web.model.client.magma.VariableDto;
 
 import com.google.common.collect.ImmutableList;
 import com.google.gwt.cell.client.CheckboxCell;
 import com.google.gwt.cell.client.ClickableTextCell;
 import com.google.gwt.cell.client.FieldUpdater;
 import com.google.gwt.cell.client.ValueUpdater;
 import com.google.gwt.core.client.GWT;
 import com.google.gwt.core.client.JavaScriptObject;
 import com.google.gwt.core.client.JsArray;
 import com.google.gwt.core.client.JsArrayString;
 import com.google.gwt.safehtml.shared.SafeHtml;
 import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
 import com.google.gwt.safehtml.shared.SafeHtmlUtils;
 import com.google.gwt.uibinder.client.UiBinder;
 import com.google.gwt.uibinder.client.UiField;
 import com.google.gwt.uibinder.client.UiTemplate;
 import com.google.gwt.user.cellview.client.CellTable;
 import com.google.gwt.user.cellview.client.Column;
 import com.google.gwt.user.cellview.client.Header;
 import com.google.gwt.user.cellview.client.SimplePager;
 import com.google.gwt.user.cellview.client.TextColumn;
 import com.google.gwt.user.client.ui.CheckBox;
 import com.google.gwt.user.client.ui.Composite;
 import com.google.gwt.user.client.ui.FlowPanel;
 import com.google.gwt.user.client.ui.HTMLPanel;
 import com.google.gwt.user.client.ui.ScrollPanel;
 import com.google.gwt.user.client.ui.VerticalPanel;
 import com.google.gwt.user.client.ui.Widget;
 import com.google.gwt.view.client.ListDataProvider;
 import com.google.gwt.view.client.MultiSelectionModel;
 import com.google.gwt.view.client.ProvidesKey;
 import com.google.gwt.view.client.SelectionModel;
 
 public class ComparedDatasourcesReportStepView extends Composite implements ComparedDatasourcesReportStepPresenter.Display {
   //
   // Static Variables
   //
 
   private static ViewUiBinder uiBinder = GWT.create(ViewUiBinder.class);
 
   private static Translations translations = GWT.create(Translations.class);
 
   //
   // Instance Variables
   //
 
   @UiField
   CheckBox ignoreAllModifications;
 
   @UiField
   HTMLPanel help;
 
   @UiField
   BreadCrumbTabLayout tableTabs;
 
   @UiField
   CellTable<TableComparision> tableList;
 
   private List<TableComparision> tableComparisions = new ArrayList<TableComparision>();
 
   private ListDataProvider<TableComparision> tableComparisionsProvider;
 
   //
   // Constructors
   //
 
   public ComparedDatasourcesReportStepView() {
     initWidget(uiBinder.createAndBindUi(this));
 
     initTableList();
   }
 
   private void initTableList() {
     tableList.setPageSize(100);
     tableComparisionsProvider = new ListDataProvider<TableComparision>(tableComparisions);
     tableComparisionsProvider.addDataDisplay(tableList);
     tableList.setEmptyTableWidget(tableList.getLoadingIndicator());
 
     SelectionModel<TableComparision> selectionModel = new MultiSelectionModel<TableComparision>(new ProvidesKey<TableComparision>() {
 
       @Override
       public Object getKey(TableComparision item) {
         return item.getTableName();
       }
     });
     tableList.setSelectionModel(selectionModel);
 
     initTableListColumns();
   }
 
   private void initTableListColumns() {
     initTableListCheckColumn();
     initTableListTableNameColumn();
     initTableListCountColumns();
   }
 
   private void initTableListCountColumns() {
     tableList.addColumn(new TextColumn<TableComparision>() {
 
       @Override
       public String getValue(TableComparision object) {
         return Integer.toString(object.getUnmodifiedVariablesCount());
       }
     }, translations.unmodifiedVariablesLabel());
 
     tableList.addColumn(new TextColumn<TableComparision>() {
 
       @Override
       public String getValue(TableComparision object) {
         int conflicts = object.getNewVariablesConflictsCount();
         if(conflicts > 0) {
           return Integer.toString(object.getNewVariablesCount()) + " (" + conflicts + ")";
         } else {
           return Integer.toString(object.getNewVariablesCount());
         }
       }
     }, translations.newVariablesLabel());
 
     tableList.addColumn(new TextColumn<TableComparision>() {
 
       @Override
       public String getValue(TableComparision object) {
         int conflicts = object.getModifiedVariablesConflictsCount();
         if(conflicts > 0) {
           return Integer.toString(object.getModifiedVariablesCount()) + " (" + conflicts + ")";
         } else {
           return Integer.toString(object.getModifiedVariablesCount());
         }
       }
     }, translations.modifiedVariablesLabel());
 
     tableList.addColumn(new TextColumn<TableComparision>() {
 
       @Override
       public String getValue(TableComparision object) {
         return Integer.toString(object.getConflictsCount());
       }
     }, translations.conflictedVariablesLabel());
   }
 
   private void initTableListCheckColumn() {
     Column<TableComparision, Boolean> checkColumn = new Column<TableComparision, Boolean>(new CheckboxCell(true, true) {
       @Override
       public void render(Context context, Boolean value, SafeHtmlBuilder sb) {
         // check if forbidden
         TableComparision tc = (TableComparision) context.getKey();
         if(tc.isForbidden()) {
           sb.append(SafeHtmlUtils.fromSafeConstant("<input type=\"checkbox\" disabled=\"true\" tabindex=\"-1\"/>"));
         } else {
           super.render(context, value, sb);
         }
       }
     }) {
 
       @Override
       public Boolean getValue(TableComparision object) {
         // Get the value from the selection model.
         return tableList.getSelectionModel().isSelected(object);
       }
 
     };
     checkColumn.setFieldUpdater(new FieldUpdater<ComparedDatasourcesReportStepView.TableComparision, Boolean>() {
 
       @Override
       public void update(int index, TableComparision object, Boolean value) {
         tableList.getSelectionModel().setSelected(object, value);
       }
     });

    tableList.addColumn(checkColumn, createTableListCheckColumnHeader());
  }

  private Header<Boolean> createTableListCheckColumnHeader() {
     Header<Boolean> checkHeader = new Header<Boolean>(new CheckboxCell(true, true)) {
 
       @Override
       public Boolean getValue() {
         if(tableComparisions.size() == 0) return false;
         boolean allSelected = true;
         for(TableComparision tc : tableComparisions) {
           if(tc.isForbidden() == false && tableList.getSelectionModel().isSelected(tc) == false) {
             return false;
           }
         }
         return allSelected;
       }
     };
     checkHeader.setUpdater(new ValueUpdater<Boolean>() {
 
       @Override
       public void update(Boolean value) {
         for(TableComparision tc : tableComparisions) {
           if(tc.isForbidden() == false) {
             tableList.getSelectionModel().setSelected(tc, value);
           }
         }
       }
     });
    return checkHeader;
   }
 
   private void initTableListTableNameColumn() {
     Column<TableComparision, String> tableNameColumn;
     tableList.addColumn(tableNameColumn = new Column<TableComparision, String>(new ClickableTextCell() {
       @Override
       public void render(Context context, SafeHtml value, SafeHtmlBuilder sb) {
         if(value != null) {
           TableComparision tc = (TableComparision) context.getKey();
           sb.appendHtmlConstant("<a class=\"" + tc.getStatusStyle() + "\" title=\"" + tc.getStatus() + "\">").append(value).appendHtmlConstant("</a>");
         }
       }
     }) {
 
       @Override
       public String getValue(TableComparision object) {
         return object.getTableName();
       }
     }, translations.tableLabel());
     tableNameColumn.setFieldUpdater(new FieldUpdater<ComparedDatasourcesReportStepView.TableComparision, String>() {
 
       @Override
       public void update(int index, TableComparision object, String value) {
         tableTabs.addAndSelect(getTableCompareTabContent(object.getTableCompareDto()), object.getTableName());
       }
     });
   }
 
   //
   // UploadVariablesStepPresenter.Display Methods
   //
 
   @Override
   public List<String> getSelectedTables() {
     ImmutableList.Builder<String> builder = ImmutableList.<String> builder();
     for(TableComparision tc : tableComparisions) {
       if(tableList.getSelectionModel().isSelected(tc)) {
         builder.add(tc.getTableName());
       }
     }
     return builder.build();
   }
 
   @Override
   public void clearDisplay() {
     ignoreAllModifications.setValue(false);
     ignoreAllModifications.setEnabled(false);
     tableComparisions.clear();
     tableComparisionsProvider.refresh();
   }
 
   @Override
   public void addTableComparision(TableCompareDto tableCompareData, ComparisonResult comparisonResult) {
     tableComparisions.add(new TableComparision(tableCompareData, comparisonResult));
     tableComparisionsProvider.refresh();
     tableList.setVisible(true);
   }
 
   @Override
   public boolean ignoreAllModifications() {
     return ignoreAllModifications.getValue();
   }
 
   @Override
   public void setIgnoreAllModificationsVisible(boolean visible) {
     ignoreAllModifications.setVisible(visible);
   }
 
   @Override
   public void setIgnoreAllModificationsEnabled(boolean enabled) {
     ignoreAllModifications.setEnabled(enabled);
   }
 
   private FlowPanel getTableCompareTabContent(TableCompareDto tableCompareData) {
     FlowPanel tableComparePanel = new FlowPanel();
     HorizontalTabLayout variableChangesPanel = initVariableChangesPanel(tableCompareData, tableComparePanel);
     tableComparePanel.add(variableChangesPanel);
     return tableComparePanel;
   }
 
   @SuppressWarnings("unchecked")
   private HorizontalTabLayout initVariableChangesPanel(TableCompareDto tableCompareData, FlowPanel tableComparePanel) {
     HorizontalTabLayout variableChangesPanel = new HorizontalTabLayout();
 
     JsArray<VariableDto> newVariables = JsArrays.toSafeArray(tableCompareData.getNewVariablesArray());
     JsArray<VariableDto> unmodifiedVariables = JsArrays.toSafeArray(tableCompareData.getUnmodifiedVariablesArray());
     JsArray<VariableDto> modifiedVariables = JsArrays.toSafeArray(tableCompareData.getModifiedVariablesArray());
     JsArray<ConflictDto> conflicts = JsArrays.toSafeArray(tableCompareData.getConflictsArray());
 
     if(unmodifiedVariables.length() > 0) {
       addVariablesTab(unmodifiedVariables, variableChangesPanel, translations.unmodifiedVariablesLabel());
     }
 
     if(newVariables.length() > 0) {
       addVariablesTab(newVariables, variableChangesPanel, translations.newVariablesLabel());
     }
 
     if(modifiedVariables.length() > 0) {
       addVariablesTab(modifiedVariables, variableChangesPanel, translations.modifiedVariablesLabel());
     }
 
     if(conflicts.length() > 0) {
       addConflictsTab(conflicts, variableChangesPanel);
     }
 
     variableChangesPanel.setVisible(variableChangesPanel.getTabCount() > 0);
 
     return variableChangesPanel;
   }
 
   private void addConflictsTab(JsArray<ConflictDto> conflicts, HorizontalTabLayout variableChangesPanel) {
     CellTable<ConflictDto> variableConflictsDetails = setupColumnsForConflicts();
     SimplePager variableConflictsPager = prepareVariableChangesTab(variableChangesPanel, translations.conflictedVariablesLabel(), variableConflictsDetails);
 
     JsArrayDataProvider<ConflictDto> dataProvider = new JsArrayDataProvider<ConflictDto>();
     dataProvider.addDataDisplay(variableConflictsDetails);
     populateVariableChangesTable(conflicts, dataProvider, variableConflictsPager);
   }
 
   private void addVariablesTab(JsArray<VariableDto> variables, HorizontalTabLayout variableChangesPanel, String tabTitle) {
     CellTable<VariableDto> variablesDetails = setupColumnsForVariables();
     SimplePager variableDetailsPager = prepareVariableChangesTab(variableChangesPanel, tabTitle, variablesDetails);
 
     JsArrayDataProvider<VariableDto> dataProvider = new JsArrayDataProvider<VariableDto>();
     dataProvider.addDataDisplay(variablesDetails);
     populateVariableChangesTable(variables, dataProvider, variableDetailsPager);
   }
 
   private <T extends JavaScriptObject> SimplePager prepareVariableChangesTab(HorizontalTabLayout variableChangesTabPanel, String tabTitle, CellTable<T> variableChangesTable) {
     variableChangesTable.addStyleName("variableChangesDetails");
     ScrollPanel variableChangesDetails = new ScrollPanel();
     VerticalPanel variableChangesDetailsVert = new VerticalPanel();
     variableChangesDetailsVert.setWidth("100%");
     SimplePager pager = new SimplePager();
     pager.setDisplay(variableChangesTable);
     variableChangesDetailsVert.add(initVariableChangesPager(variableChangesTable, pager));
     variableChangesDetailsVert.add(variableChangesTable);
     variableChangesDetailsVert.addStyleName("variableChangesDetailsVert");
     variableChangesDetails.add(variableChangesDetailsVert);
     variableChangesTabPanel.add(variableChangesDetails, tabTitle);
     return pager;
   }
 
   FlowPanel initVariableChangesPager(CellTable<? extends JavaScriptObject> table, SimplePager pager) {
     table.setPageSize(20);
     FlowPanel pagerPanel = new FlowPanel();
     pagerPanel.addStyleName("variableChangesPager");
     pagerPanel.add(pager);
     return pagerPanel;
   }
 
   private <T extends JavaScriptObject> void populateVariableChangesTable(final JsArray<T> conflicts, JsArrayDataProvider<T> dataProvider, SimplePager pager) {
     dataProvider.setArray(conflicts);
     pager.firstPage();
     dataProvider.refresh();
   }
 
   private CellTable<ConflictDto> setupColumnsForConflicts() {
     CellTable<ConflictDto> table = new Table<ConflictDto>();
     table.addColumn(new TextColumn<ConflictDto>() {
       @Override
       public String getValue(ConflictDto conflict) {
         return conflict.getVariable().getName();
       }
     }, translations.nameLabel());
     table.addColumn(new TextColumn<ConflictDto>() {
       @Override
       public String getValue(ConflictDto conflict) {
 
         StringBuilder builder = new StringBuilder();
         JsArrayString arguments = (JsArrayString) (conflict.getArgumentsArray() != null ? conflict.getArgumentsArray() : JsArrayString.createArray());
         for(int i = 0; i < arguments.length(); i++) {
           builder.append(arguments.get(i).toString() + " ");
         }
 
         return translations.datasourceComparisonErrorMap().get(conflict.getCode()) + ", " + builder.toString();
       }
     }, translations.messageLabel());
 
     return table;
   }
 
   private CellTable<VariableDto> setupColumnsForVariables() {
 
     CellTable<VariableDto> table = new Table<VariableDto>();
     table.addColumn(new TextColumn<VariableDto>() {
       @Override
       public String getValue(VariableDto variable) {
         return variable.getName();
       }
     }, translations.nameLabel());
 
     table.addColumn(new TextColumn<VariableDto>() {
       @Override
       public String getValue(VariableDto variable) {
         return variable.getValueType();
       }
     }, translations.valueTypeLabel());
 
     table.addColumn(new TextColumn<VariableDto>() {
       @Override
       public String getValue(VariableDto variable) {
         return variable.getUnit();
       }
     }, translations.unitLabel());
 
     table.addColumn(new VariableAttributeColumn("label"), translations.labelLabel());
 
     return table;
   }
 
   public Widget asWidget() {
     return this;
   }
 
   public void startProcessing() {
   }
 
   public void stopProcessing() {
   }
 
   //
   // Inner Classes / Interfaces
   //
 
   @UiTemplate("ComparedDatasourcesReportStepView.ui.xml")
   interface ViewUiBinder extends UiBinder<Widget, ComparedDatasourcesReportStepView> {
   }
 
   @Override
   public Widget getStepHelp() {
     return help;
   }
 
   private static class TableComparision {
     private final TableCompareDto dto;
 
     private final ComparisonResult result;
 
     public TableComparision(TableCompareDto dto, ComparisonResult result) {
       super();
       this.dto = dto;
       this.result = result;
     }
 
     public String getTableName() {
       return dto.getCompared().getName();
     }
 
     public String getStatus() {
       return result.toString();
     }
 
     public String getStatusStyle() {
       switch(result) {
       case FORBIDDEN:
         return "iconb i-disapprove";
       case CONFLICT:
         return "iconb i-alert";
       case CREATION:
         return "iconb i-plus";
       case MODIFICATION:
         return "iconb i-reblog";
       default:
         return "iconb i-done";
       }
     }
 
     public boolean isForbidden() {
       return result == ComparisonResult.FORBIDDEN;
     }
 
     public TableCompareDto getTableCompareDto() {
       return dto;
     }
 
     public int getNewVariablesCount() {
       return JsArrays.toSafeArray(dto.getNewVariablesArray()).length();
     }
 
     public int getModifiedVariablesCount() {
       return JsArrays.toSafeArray(dto.getModifiedVariablesArray()).length();
     }
 
     public int getUnmodifiedVariablesCount() {
       return JsArrays.toSafeArray(dto.getUnmodifiedVariablesArray()).length();
     }
 
     public int getConflictsCount() {
       return JsArrays.toSafeArray(dto.getConflictsArray()).length();
     }
 
     public int getNewVariablesConflictsCount() {
       return getConflictsCounts(JsArrays.toSafeArray(dto.getConflictsArray()))[0];
     }
 
     public int getModifiedVariablesConflictsCount() {
       return getConflictsCounts(JsArrays.toSafeArray(dto.getConflictsArray()))[1];
     }
 
     private int[] getConflictsCounts(JsArray<ConflictDto> conflicts) {
       int conflictsCount[] = { 0, 0 };
       for(int i = 0; i < conflicts.length(); i++) {
         if(conflicts.get(i).getVariable().getIsNewVariable()) {
           conflictsCount[0]++;
         } else {
           conflictsCount[1]++;
         }
       }
       return conflictsCount;
     }
   }
 
 }
