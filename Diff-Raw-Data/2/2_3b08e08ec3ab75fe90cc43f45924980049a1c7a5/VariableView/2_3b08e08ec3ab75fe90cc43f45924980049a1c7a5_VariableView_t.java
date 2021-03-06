 /*******************************************************************************
  * Copyright 2008(c) The OBiBa Consortium. All rights reserved.
  * 
  * This program and the accompanying materials
  * are made available under the terms of the GNU Public License v3.0.
  * 
  * You should have received a copy of the GNU General Public License
  * along with this program.  If not, see <http://www.gnu.org/licenses/>.
  ******************************************************************************/
 package org.obiba.opal.web.gwt.app.client.view;
 
 import org.obiba.opal.web.gwt.app.client.i18n.Translations;
 import org.obiba.opal.web.gwt.app.client.js.JsArrays;
 import org.obiba.opal.web.gwt.app.client.presenter.VariablePresenter;
 import org.obiba.opal.web.model.client.magma.AttributeDto;
 import org.obiba.opal.web.model.client.magma.CategoryDto;
 import org.obiba.opal.web.model.client.math.CategoricalSummaryDto;
 import org.obiba.opal.web.model.client.math.ContinuousSummaryDto;
 import org.obiba.opal.web.model.client.math.SummaryStatisticsDto;
 
 import com.google.gwt.core.client.GWT;
 import com.google.gwt.core.client.JsArray;
 import com.google.gwt.event.logical.shared.SelectionEvent;
 import com.google.gwt.event.logical.shared.SelectionHandler;
 import com.google.gwt.uibinder.client.UiBinder;
 import com.google.gwt.uibinder.client.UiField;
 import com.google.gwt.uibinder.client.UiTemplate;
 import com.google.gwt.user.cellview.client.CellTable;
 import com.google.gwt.user.cellview.client.SimplePager;
 import com.google.gwt.user.cellview.client.TextColumn;
 import com.google.gwt.user.client.Command;
 import com.google.gwt.user.client.ui.Composite;
 import com.google.gwt.user.client.ui.FlowPanel;
 import com.google.gwt.user.client.ui.Image;
 import com.google.gwt.user.client.ui.Label;
 import com.google.gwt.user.client.ui.Panel;
 import com.google.gwt.user.client.ui.TabLayoutPanel;
 import com.google.gwt.user.client.ui.VerticalPanel;
 import com.google.gwt.user.client.ui.Widget;
 import com.google.gwt.view.client.ListView;
 import com.google.gwt.view.client.SingleSelectionModel;
 import com.google.gwt.view.client.ListView.Delegate;
 
 /**
  *
  */
 public class VariableView extends Composite implements VariablePresenter.Display {
 
   @UiTemplate("VariableView.ui.xml")
   interface VariableViewUiBinder extends UiBinder<Widget, VariableView> {
   }
 
   private static final String DEFAULT_LOCALE_NAME = "default";
 
   private static final String LABEL_ATTRIBUTE_NAME = "label";
 
   private static final Integer SUMMARY_TAB_INDEX = 2;
 
   private static VariableViewUiBinder uiBinder = GWT.create(VariableViewUiBinder.class);
 
   private static Translations translations = GWT.create(Translations.class);
 
   //
   // Instance Variables
   //
 
   @UiField
   FlowPanel toolbarPanel;
 
   private NavigatorMenuBar toolbar;
 
   @UiField
   Label variableName;
 
   @UiField
   Label entityType;
 
   @UiField
   Label valueType;
 
   @UiField
   Label mimeType;
 
   @UiField
   Label unit;
 
   @UiField
   Label repeatable;
 
   @UiField
   Label occurrenceGroup;
 
   @UiField
   Label label;
 
   @UiField
   TabLayoutPanel tabs;
 
   @UiField
   Label categoryTableTitle;
 
   @UiField
   CellTable<CategoryDto> categoryTable;
 
   SimplePager<CategoryDto> categoryTablePager;
 
   @UiField
   Label attributeTableTitle;
 
   @UiField
   CellTable<AttributeDto> attributeTable;
 
   SimplePager<AttributeDto> attributeTablePager;
 
   @UiField
   Panel summary;
 
   //
   // Constructors
   //
 
   public VariableView() {
     initWidget(uiBinder.createAndBindUi(this));
     toolbarPanel.add(toolbar = new NavigatorMenuBar());
     initCategoryTable();
     initAttributeTable();
   }
 
   //
   // VariablePresenter.Display Methods
   //
 
   @SuppressWarnings("unchecked")
   public void renderCategoryRows(JsArray<CategoryDto> rows) {
     final JsArray<CategoryDto> categoryRows = (rows != null) ? rows : (JsArray<CategoryDto>) JsArray.createArray();
 
     categoryTableTitle.setText(translations.categoriesLabel() + " (" + categoryRows.length() + ")");
 
     categoryTable.setDelegate(new Delegate<CategoryDto>() {
 
       @Override
       public void onRangeChanged(ListView<CategoryDto> listView) {
         int start = listView.getRange().getStart();
         int length = listView.getRange().getLength();
         listView.setData(start, length, JsArrays.toList(categoryRows, start, length));
       }
     });
 
     categoryTablePager.firstPage();
     categoryTable.setData(0, categoryTable.getPageSize(), JsArrays.toList(categoryRows, 0, categoryTable.getPageSize()));
     categoryTable.setDataSize(categoryRows.length(), true);
     categoryTable.redraw();
   }
 
   @SuppressWarnings("unchecked")
   public void renderAttributeRows(final JsArray<AttributeDto> rows) {
     final JsArray<AttributeDto> attributeRows = (rows != null) ? rows : (JsArray<AttributeDto>) JsArray.createArray();
 
     attributeTableTitle.setText(translations.attributesLabel() + " (" + attributeRows.length() + ")");
 
     attributeTable.setDelegate(new Delegate<AttributeDto>() {
 
       @Override
       public void onRangeChanged(ListView<AttributeDto> listView) {
         int start = listView.getRange().getStart();
         int length = listView.getRange().getLength();
         listView.setData(start, length, JsArrays.toList(attributeRows, start, length));
       }
     });
 
     attributeTablePager.firstPage();
     attributeTable.setData(0, attributeTable.getPageSize(), JsArrays.toList(attributeRows, 0, attributeTable.getPageSize()));
     attributeTable.setDataSize(attributeRows.length(), true);
     attributeTable.redraw();
 
     label.setText(getAttributeValue(attributeRows, LABEL_ATTRIBUTE_NAME));
   }
 
   @Override
   public void setSummaryTabCommand(final Command cmd) {
     tabs.addSelectionHandler(new SelectionHandler<Integer>() {
 
       @Override
       public void onSelection(SelectionEvent<Integer> event) {
         if(event.getSelectedItem() == SUMMARY_TAB_INDEX) {
           cmd.execute();
         }
       }
     });
   }
 
   @Override
   public boolean isSummaryTabSelected() {
     return tabs.getSelectedIndex() == SUMMARY_TAB_INDEX;
   }
 
   @Override
   public void requestingSummary() {
     summary.clear();
    summary.add(new Image("image/loading.gif"));
   }
 
   @Override
   public void renderSummary(SummaryStatisticsDto dto) {
     summary.clear();
     if(dto.getExtension(ContinuousSummaryDto.SummaryStatisticsDtoExtensions.continuous) != null) {
       ContinuousSummaryDto continuous = dto.getExtension(ContinuousSummaryDto.SummaryStatisticsDtoExtensions.continuous).cast();
       summary.add(new ContinuousSummaryView(continuous));
     } else if(dto.getExtension(CategoricalSummaryDto.SummaryStatisticsDtoExtensions.categorical) != null) {
       CategoricalSummaryDto categorical = dto.getExtension(CategoricalSummaryDto.SummaryStatisticsDtoExtensions.categorical).cast();
       summary.add(new CategoricalSummaryView(categorical));
     } else {
       summary.add(new Label(translations.noDataAvailableLabel()));
     }
   }
 
   public Widget asWidget() {
     return this;
   }
 
   public void startProcessing() {
   }
 
   public void stopProcessing() {
   }
 
   //
   // Methods
   //
 
   @Override
   public void setVariableName(String name) {
     variableName.setText(name);
   }
 
   @Override
   public void setEntityType(String text) {
     entityType.setText(text);
   }
 
   @Override
   public void setValueType(String text) {
     valueType.setText(text);
   }
 
   @Override
   public void setMimeType(String text) {
     mimeType.setText(text);
   }
 
   @Override
   public void setUnit(String text) {
     unit.setText(text);
   }
 
   @Override
   public void setRepeatable(boolean repeatable) {
     this.repeatable.setText(repeatable ? translations.yesLabel() : translations.noLabel());
   }
 
   @Override
   public void setOccurrenceGroup(String text) {
     occurrenceGroup.setText(text);
   }
 
   @Override
   public void setParentName(String name) {
     toolbar.setParentName(name);
   }
 
   @Override
   public void setNextName(String name) {
     toolbar.setNextName(name);
   }
 
   @Override
   public void setPreviousName(String name) {
     toolbar.setPreviousName(name);
   }
 
   @Override
   public void setParentCommand(Command cmd) {
     toolbar.setParentCommand(cmd);
   }
 
   @Override
   public void setNextCommand(Command cmd) {
     toolbar.setNextCommand(cmd);
   }
 
   @Override
   public void setPreviousCommand(Command cmd) {
     toolbar.setPreviousCommand(cmd);
   }
 
   private void initCategoryTable() {
 
     categoryTableTitle.setText(translations.categoriesLabel());
     categoryTable.setSelectionEnabled(false);
     categoryTable.setSelectionModel(new SingleSelectionModel<CategoryDto>());
 
     addCategoryTableColumns();
 
     // Add a pager.
     categoryTable.setPageSize(50);
     categoryTablePager = new SimplePager<CategoryDto>(categoryTable);
     categoryTable.setPager(categoryTablePager);
     ((VerticalPanel) categoryTable.getParent()).insert(categoryTablePager, 0);
   }
 
   private void addCategoryTableColumns() {
     categoryTable.addColumn(new TextColumn<CategoryDto>() {
       @Override
       public String getValue(CategoryDto object) {
         return object.getName();
       }
     }, translations.nameLabel());
 
     categoryTable.addColumn(new TextColumn<CategoryDto>() {
       @Override
       public String getValue(CategoryDto object) {
         return getCategoryLabel(object);
       }
     }, translations.labelLabel());
 
     categoryTable.addColumn(new TextColumn<CategoryDto>() {
       @Override
       public String getValue(CategoryDto object) {
         return object.getIsMissing() ? translations.yesLabel() : translations.noLabel();
       }
     }, translations.missingLabel());
   }
 
   private String getCategoryLabel(CategoryDto categoryDto) {
     return getAttributeValue(categoryDto.getAttributesArray(), LABEL_ATTRIBUTE_NAME);
   }
 
   private String getAttributeValue(JsArray<AttributeDto> attributes, String name) {
     AttributeDto attribute = null;
 
     if(attributes != null) {
       for(int i = 0; i < attributes.length(); i++) {
         AttributeDto att = attributes.get(i);
         if(att.getName().equals(name)) {
           if(!att.hasLocale()) {
             attribute = att;
           } else if(att.getLocale().equals(getCurrentLanguage())) {
             attribute = att;
             break;
           }
         }
       }
     }
 
     return attribute != null ? attribute.getValue() : "";
   }
 
   private String getCurrentLanguage() {
     String currentLocaleName = com.google.gwt.i18n.client.LocaleInfo.getCurrentLocale().getLocaleName();
     if(currentLocaleName.equals(DEFAULT_LOCALE_NAME)) {
       // No locale has been specified so the current locale is "default". Return English as the current language.
       return "en";
     }
     int separatorIndex = currentLocaleName.indexOf('_');
 
     return (separatorIndex != -1) ? currentLocaleName.substring(0, separatorIndex) : currentLocaleName;
   }
 
   private void initAttributeTable() {
     attributeTableTitle.setText(translations.attributesLabel());
     attributeTable.setSelectionEnabled(false);
     attributeTable.setSelectionModel(new SingleSelectionModel<AttributeDto>());
 
     addAttributeTableColumns();
 
     // Add a pager.
     attributeTable.setPageSize(50);
     attributeTablePager = new SimplePager<AttributeDto>(attributeTable);
     attributeTable.setPager(attributeTablePager);
     ((VerticalPanel) attributeTable.getParent()).insert(attributeTablePager, 0);
   }
 
   private void addAttributeTableColumns() {
     attributeTable.addColumn(new TextColumn<AttributeDto>() {
       @Override
       public String getValue(AttributeDto object) {
         return object.getName();
       }
     }, translations.nameLabel());
 
     attributeTable.addColumn(new TextColumn<AttributeDto>() {
       @Override
       public String getValue(AttributeDto object) {
         return object.hasLocale() ? object.getLocale() : "";
       }
     }, translations.languageLabel());
 
     attributeTable.addColumn(new TextColumn<AttributeDto>() {
       @Override
       public String getValue(AttributeDto object) {
         return object.getValue();
       }
     }, translations.valueLabel());
   }
 
 }
