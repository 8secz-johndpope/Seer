 /**
  * ***************************************************************************
  * Copyright (c) 2010 Qcadoo Limited
  * Project: Qcadoo MES
  * Version: 1.1.0
  *
  * This file is part of Qcadoo.
  *
  * Qcadoo is free software; you can redistribute it and/or modify
  * it under the terms of the GNU Affero General Public License as published
  * by the Free Software Foundation; either version 3 of the License,
  * or (at your option) any later version.
  *
  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty
  * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
  * See the GNU Affero General Public License for more details.
  *
  * You should have received a copy of the GNU Affero General Public License
  * along with this program; if not, write to the Free Software
  * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
  * ***************************************************************************
  */
 package com.qcadoo.mes.technologies;
 
 import static com.qcadoo.mes.basic.constants.BasicConstants.MODEL_PRODUCT;
 import static com.qcadoo.mes.technologies.constants.TechnologiesConstants.MODEL_TECHNOLOGY_OPERATION_COMPONENT;
 
 import java.math.BigDecimal;
 import java.util.ArrayList;
 import java.util.Calendar;
 import java.util.Date;
 import java.util.HashSet;
 import java.util.LinkedHashMap;
 import java.util.List;
 import java.util.Map;
 import java.util.Set;
 
 import org.springframework.beans.factory.annotation.Autowired;
 import org.springframework.stereotype.Service;
 import org.springframework.util.StringUtils;
 
 import com.qcadoo.localization.api.TranslationService;
 import com.qcadoo.mes.basic.constants.BasicConstants;
 import com.qcadoo.mes.technologies.constants.TechnologiesConstants;
 import com.qcadoo.mes.technologies.constants.TechnologyState;
 import com.qcadoo.mes.technologies.print.ReportDataService;
 import com.qcadoo.mes.technologies.states.TechnologyStateUtils;
 import com.qcadoo.model.api.DataDefinition;
 import com.qcadoo.model.api.DataDefinitionService;
 import com.qcadoo.model.api.Entity;
 import com.qcadoo.model.api.EntityList;
 import com.qcadoo.model.api.EntityTree;
 import com.qcadoo.model.api.EntityTreeNode;
 import com.qcadoo.model.api.search.SearchCriteriaBuilder;
 import com.qcadoo.model.api.search.SearchRestrictions;
 import com.qcadoo.model.api.utils.TreeNumberingService;
 import com.qcadoo.view.api.ComponentState;
 import com.qcadoo.view.api.ViewDefinitionState;
 import com.qcadoo.view.api.components.FieldComponent;
 import com.qcadoo.view.api.components.FormComponent;
 import com.qcadoo.view.api.components.GridComponent;
 import com.qcadoo.view.api.components.TreeComponent;
 import com.qcadoo.view.api.utils.NumberGeneratorService;
 
 @Service
 public class TechnologyService {
 
     public static final String WASTE = "04waste";
 
     public static final String PRODUCT = "03product";
 
     public static final String COMPONENT = "01component";
 
     public static final String INTERMEDIATE = "02intermediate";
 
     public static final String UNRELATED = "00unrelated";
 
     private static final String CONST_TECHNOLOGY = "technology";
 
     private static final String CONST_MASTER = "master";
 
     private static final String CONST_PRODUCT = "product";
 
     private static final String CONST_ENTITY_TYPE = "entityType";
 
     private static final String CONST_OPERATION = "operation";
 
     private static final String CONST_OPERATION_COMPONENTS = "operationComponents";
 
     private static final String CONST_OPERATION_COMPONENT = "operationComponent";
 
     private static final String CONST_OPERATION_COMP_PRODUCT_IN = "operationProductInComponents";
 
     private static final String CONST_OPERATION_COMP_PRODUCT_OUT = "operationProductOutComponents";
 
     private static final String CONST_REFERENCE_MODE = "referenceMode";
 
     private static final String CONST_STATE = "state";
 
     @Autowired
     private DataDefinitionService dataDefinitionService;
 
     @Autowired
     private NumberGeneratorService numberGeneratorService;
 
     @Autowired
     private ReportDataService reportDataService;
 
     @Autowired
     private TreeNumberingService treeNumberingService;
 
     @Autowired
     private TranslationService translationService;
 
     private enum ProductDirection {
         IN, OUT;
     }
 
     public boolean clearMasterOnCopy(final DataDefinition dataDefinition, final Entity entity) {
         entity.setField(CONST_MASTER, false);
         return true;
     }
 
     public void setFirstTechnologyAsDefault(final DataDefinition dataDefinition, final Entity entity) {
         if ((Boolean) entity.getField(CONST_MASTER)) {
             return;
         }
         SearchCriteriaBuilder searchCriteria = dataDefinition.find();
         searchCriteria.add(SearchRestrictions.belongsTo(CONST_PRODUCT, entity.getBelongsToField(CONST_PRODUCT)));
         entity.setField(CONST_MASTER, searchCriteria.list().getTotalNumberOfEntities() == 0);
     }
 
     public boolean checkTechnologyDefault(final DataDefinition dataDefinition, final Entity entity) {
         if (!((Boolean) entity.getField(CONST_MASTER))) {
             return true;
         }
 
         SearchCriteriaBuilder searchCriteries = dataDefinition.find();
         searchCriteries.add(SearchRestrictions.eq(CONST_MASTER, true));
         searchCriteries.add(SearchRestrictions.belongsTo(CONST_PRODUCT, entity.getBelongsToField(CONST_PRODUCT)));
 
         if (entity.getId() != null) {
             searchCriteries.add(SearchRestrictions.idNe(entity.getId()));
         }
 
         if (searchCriteries.list().getTotalNumberOfEntities() == 0) {
             return true;
         }
         entity.addError(dataDefinition.getField(CONST_MASTER), "orders.validate.global.error.default");
         return false;
     }
 
     public void loadProductsForReferencedTechnology(final ViewDefinitionState viewDefinitionState, final ComponentState state,
             final String[] args) {
         if (!(state instanceof TreeComponent)) {
             return;
         }
 
         TreeComponent tree = (TreeComponent) state;
 
         if (tree.getSelectedEntityId() == null) {
             return;
         }
 
         Entity operationComponent = dataDefinitionService.get(TechnologiesConstants.PLUGIN_IDENTIFIER,
                 TechnologiesConstants.MODEL_TECHNOLOGY_OPERATION_COMPONENT).get(tree.getSelectedEntityId());
 
         GridComponent outProductsGrid = (GridComponent) viewDefinitionState.getComponentByReference("outProducts");
         GridComponent inProductsGrid = (GridComponent) viewDefinitionState.getComponentByReference("inProducts");
 
         if (!"referenceTechnology".equals(operationComponent.getStringField(CONST_ENTITY_TYPE))) {
             inProductsGrid.setEditable(true);
             outProductsGrid.setEditable(true);
             return;
         }
 
         Entity technology = operationComponent.getBelongsToField("referenceTechnology");
         EntityTree operations = technology.getTreeField(CONST_OPERATION_COMPONENTS);
         Entity rootOperation = operations.getRoot();
 
         if (rootOperation != null) {
             outProductsGrid.setEntities(rootOperation.getHasManyField(CONST_OPERATION_COMP_PRODUCT_OUT));
         }
 
         Map<Entity, BigDecimal> inProductsWithCount = new LinkedHashMap<Entity, BigDecimal>();
         List<Entity> inProducts = new ArrayList<Entity>();
 
         reportDataService.countQuantityForProductsIn(inProductsWithCount, technology, BigDecimal.ONE, false);
 
         for (Map.Entry<Entity, BigDecimal> inProductWithCount : inProductsWithCount.entrySet()) {
             Entity inProduct = dataDefinitionService.get(TechnologiesConstants.PLUGIN_IDENTIFIER,
                     TechnologiesConstants.MODEL_OPERATION_PRODUCT_IN_COMPONENT).create();
             inProduct.setField(CONST_OPERATION_COMPONENT, rootOperation);
             inProduct.setField(CONST_PRODUCT, inProductWithCount.getKey());
             inProduct.setField("quantity", inProductWithCount.getValue());
             inProducts.add(inProduct);
         }
 
         inProductsGrid.setEntities(inProducts);
         inProductsGrid.setEnabled(false);
         inProductsGrid.setEditable(false);
         outProductsGrid.setEnabled(false);
         outProductsGrid.setEditable(false);
     }
 
     public void checkQualityControlType(final ViewDefinitionState viewDefinitionState, final ComponentState state,
             final String[] args) {
         if (!(state instanceof FieldComponent)) {
             throw new IllegalStateException("component is not select");
         }
 
         FieldComponent qualityControlType = (FieldComponent) state;
 
         FieldComponent unitSamplingNr = (FieldComponent) viewDefinitionState.getComponentByReference("unitSamplingNr");
 
         if (qualityControlType.getFieldValue() != null) {
             if (qualityControlType.getFieldValue().equals("02forUnit")) {
                 unitSamplingNr.setRequired(true);
                 unitSamplingNr.setVisible(true);
             } else {
                 unitSamplingNr.setRequired(false);
                 unitSamplingNr.setVisible(false);
             }
         }
     }
 
     public void generateTechnologyNumber(final ViewDefinitionState state, final ComponentState componentState, final String[] args) {
         if (!(componentState instanceof FieldComponent)) {
             throw new IllegalStateException("component is not FieldComponentState");
         }
         FieldComponent number = (FieldComponent) state.getComponentByReference("number");
         FieldComponent productState = (FieldComponent) componentState;
 
         if (!numberGeneratorService.checkIfShouldInsertNumber(state, "form", "number") || productState.getFieldValue() == null) {
             return;
         }
 
         Entity product = getProductById((Long) productState.getFieldValue());
 
         if (product == null) {
             return;
         }
 
         String numberValue = product.getField("number") + "-"
                 + numberGeneratorService.generateNumber("technologies", CONST_TECHNOLOGY, 3);
         number.setFieldValue(numberValue);
     }
 
     public void generateTechnologyName(final ViewDefinitionState state, final ComponentState componentState, final String[] args) {
         if (!(componentState instanceof FieldComponent)) {
             throw new IllegalStateException("component is not FieldComponentState");
         }
         FieldComponent name = (FieldComponent) state.getComponentByReference("name");
         FieldComponent productState = (FieldComponent) componentState;
 
         if (StringUtils.hasText((String) name.getFieldValue()) || productState.getFieldValue() == null) {
             return;
         }
 
         Entity product = getProductById((Long) productState.getFieldValue());
 
         if (product == null) {
             return;
         }
 
         Calendar cal = Calendar.getInstance(state.getLocale());
         cal.setTime(new Date());
 
         name.setFieldValue(translationService.translate("technologies.operation.name.default", state.getLocale(),
                 product.getStringField("name"), product.getStringField("number"),
                 cal.get(Calendar.YEAR) + "." + cal.get(Calendar.MONTH)));
     }
 
     public void hideReferenceMode(final ViewDefinitionState viewDefinitionState) {
         FormComponent form = (FormComponent) viewDefinitionState.getComponentByReference("form");
         if (form.getEntityId() != null) {
             ComponentState referenceModeComponent = viewDefinitionState.getComponentByReference(CONST_REFERENCE_MODE);
             referenceModeComponent.setFieldValue("01reference");
             referenceModeComponent.setVisible(false);
         }
     }
 
     private Entity getProductById(final Long productId) {
         return dataDefinitionService.get(BasicConstants.PLUGIN_IDENTIFIER, MODEL_PRODUCT).get(productId);
     }
 
     public boolean copyReferencedTechnology(final DataDefinition dataDefinition, final Entity entity) {
         if (!"referenceTechnology".equals(entity.getField(CONST_ENTITY_TYPE)) && entity.getField("referenceTechnology") == null) {
             return true;
         }
 
         boolean copy = "02copy".equals(entity.getField(CONST_REFERENCE_MODE));
 
         Entity technology = entity.getBelongsToField(CONST_TECHNOLOGY);
         Entity referencedTechnology = entity.getBelongsToField("referenceTechnology");
 
         Set<Long> technologies = new HashSet<Long>();
         technologies.add(technology.getId());
 
         boolean cyclic = checkForCyclicReferences(technologies, referencedTechnology, copy);
 
         if (cyclic) {
             entity.addError(dataDefinition.getField("referenceTechnology"),
                     "technologies.technologyReferenceTechnologyComponent.error.cyclicDependency");
             return false;
         }
 
         if (copy) {
             EntityTreeNode root = referencedTechnology.getTreeField(CONST_OPERATION_COMPONENTS).getRoot();
 
             Entity copiedRoot = copyReferencedTechnologyOperations(root, entity.getBelongsToField(CONST_TECHNOLOGY));
 
             entity.setField(CONST_ENTITY_TYPE, CONST_OPERATION);
             entity.setField("referenceTechnology", null);
             entity.setField("qualityControlRequired", copiedRoot.getField("qualityControlRequired"));
             entity.setField(CONST_OPERATION, copiedRoot.getField(CONST_OPERATION));
             entity.setField("children", copiedRoot.getField("children"));
             entity.setField(CONST_OPERATION_COMP_PRODUCT_IN, copiedRoot.getField(CONST_OPERATION_COMP_PRODUCT_IN));
             entity.setField(CONST_OPERATION_COMP_PRODUCT_OUT, copiedRoot.getField(CONST_OPERATION_COMP_PRODUCT_OUT));
         }
 
         return true;
     }
 
     private boolean checkForCyclicReferences(final Set<Long> technologies, final Entity referencedTechnology, final boolean copy) {
         if (!copy && technologies.contains(referencedTechnology.getId())) {
             return true;
         }
 
         technologies.add(referencedTechnology.getId());
 
         for (Entity operationComponent : referencedTechnology.getTreeField(CONST_OPERATION_COMPONENTS)) {
             if ("referenceTechnology".equals(operationComponent.getField(CONST_ENTITY_TYPE))) {
                 boolean cyclic = checkForCyclicReferences(technologies,
                         operationComponent.getBelongsToField("referenceTechnology"), false);
 
                 if (cyclic) {
                     return true;
                 }
             }
         }
 
         return false;
     }
 
     private Entity copyReferencedTechnologyOperations(final EntityTreeNode node, final Entity technology) {
         Entity copy = node.copy();
         copy.setId(null);
         copy.setField("parent", null);
         copy.setField(CONST_TECHNOLOGY, technology);
         copy.setField("children", copyOperationsChildren(node.getChildren(), technology));
         copy.setField(CONST_OPERATION_COMP_PRODUCT_IN,
                 copyProductComponents(copy.getHasManyField(CONST_OPERATION_COMP_PRODUCT_IN)));
         copy.setField(CONST_OPERATION_COMP_PRODUCT_OUT,
                 copyProductComponents(copy.getHasManyField(CONST_OPERATION_COMP_PRODUCT_OUT)));
         return copy;
     }
 
     private List<Entity> copyProductComponents(final EntityList entities) {
         List<Entity> copies = new ArrayList<Entity>();
         for (Entity entity : entities) {
             Entity copy = entity.copy();
             copy.setId(null);
             copy.setField(CONST_OPERATION_COMPONENT, null);
             copies.add(copy);
         }
         return copies;
     }
 
     private List<Entity> copyOperationsChildren(final List<EntityTreeNode> entities, final Entity technology) {
         List<Entity> copies = new ArrayList<Entity>();
         for (EntityTreeNode entity : entities) {
             copies.add(copyReferencedTechnologyOperations(entity, technology));
         }
         return copies;
     }
 
     public boolean validateTechnologyOperationComponent(final DataDefinition dataDefinition, final Entity entity) {
         boolean isValid = true;
         if (CONST_OPERATION.equals(entity.getStringField(CONST_ENTITY_TYPE))) {
             if (entity.getField(CONST_OPERATION) == null) {
                 entity.addError(dataDefinition.getField(CONST_OPERATION), "qcadooView.validate.field.error.missing");
                 isValid = false;
             }
         } else if ("referenceTechnology".equals(entity.getStringField(CONST_ENTITY_TYPE))) {
             if (entity.getField("referenceTechnology") == null) {
                 entity.addError(dataDefinition.getField("referenceTechnology"), "qcadooView.validate.field.error.missing");
                 isValid = false;
             }
             if (entity.getField(CONST_REFERENCE_MODE) == null) {
                 entity.setField(CONST_REFERENCE_MODE, "01reference");
             }
         } else {
             throw new IllegalStateException("unknown entityType");
         }
         return isValid;
     }
 
     public boolean checkIfUnitSampligNrIsReq(final DataDefinition dataDefinition, final Entity entity) {
         String qualityControlType = (String) entity.getField("qualityControlType");
         if (qualityControlType != null && qualityControlType.equals("02forUnit")) {
             BigDecimal unitSamplingNr = (BigDecimal) entity.getField("unitSamplingNr");
             if (unitSamplingNr == null || unitSamplingNr.scale() > 3 || unitSamplingNr.compareTo(BigDecimal.ZERO) < 0
                     || unitSamplingNr.precision() > 7) {
                 entity.addGlobalError("qcadooView.validate.global.error.custom");
                 entity.addError(dataDefinition.getField("unitSamplingNr"),
                         "technologies.technology.validate.global.error.unitSamplingNr");
                 return false;
             }
         }
         return true;
     }
 
     public void setLookupDisableInTechnologyOperationComponent(final ViewDefinitionState viewDefinitionState) {
         FormComponent form = (FormComponent) viewDefinitionState.getComponentByReference("form");
         FieldComponent operationLookup = (FieldComponent) viewDefinitionState.getComponentByReference(CONST_OPERATION);
 
         operationLookup.setEnabled(form.getEntityId() == null);
     }
 
     public final void performTreeNumbering(final DataDefinition dd, final Entity technology) {
         DataDefinition technologyOperationDD = dataDefinitionService.get(TechnologiesConstants.PLUGIN_IDENTIFIER,
                 MODEL_TECHNOLOGY_OPERATION_COMPONENT);
         treeNumberingService.generateNumbersAndUpdateTree(technologyOperationDD, CONST_TECHNOLOGY, technology.getId());
 
     }
 
     public void setParentIfRootNodeAlreadyExists(final DataDefinition dd, final Entity technologyOperation) {
         Entity technology = technologyOperation.getBelongsToField(CONST_TECHNOLOGY);
         EntityTreeNode rootNode = technology.getTreeField(CONST_OPERATION_COMPONENTS).getRoot();
         if (rootNode == null || technologyOperation.getBelongsToField("parent") != null) {
             return;
         }
         technologyOperation.setField("parent", rootNode);
     }
 
     public void toggleDetailsViewEnabled(final ViewDefinitionState view) {
         view.getComponentByReference("state").performEvent(view, "toggleEnabled");
     }
 
     public boolean invalidateIfBelongsToAcceptedTechnology(final DataDefinition dataDefinition, final Entity entity) {
         Entity technology = null;
         String errorMessageKey = "technologies.technology.state.error.modifyBelongsToAcceptedTechnology";
         if (CONST_TECHNOLOGY.equals(dataDefinition.getName())) {
             technology = entity;
             errorMessageKey = "technologies.technology.state.error.modifyAcceptedTechnology";
         } else if ("technologyOperationComponent".equals(dataDefinition.getName())) {
             technology = entity.getBelongsToField(CONST_TECHNOLOGY);
         } else if ("operationProductOutComponent".equals(dataDefinition.getName())
                 || "operationProductInComponent".equals(dataDefinition.getName())) {
             technology = entity.getBelongsToField(CONST_OPERATION_COMPONENT).getBelongsToField(CONST_TECHNOLOGY);
         }
 
         if (technology == null || technology.getId() == null) {
             return true;
         }
 
         Entity existingTechnology = technology.getDataDefinition().get(technology.getId());
         if (isTechnologyIsAlreadyAccepted(technology, existingTechnology)) {
             entity.addGlobalError(errorMessageKey, technology.getStringField("name"));
             return false;
         }
 
         return true;
     }
 
     private boolean isTechnologyIsAlreadyAccepted(final Entity technology, final Entity existingTechnology) {
         if (technology == null || existingTechnology == null) {
             return false;
         }
         TechnologyState technologyState = TechnologyStateUtils.getStateFromField(technology.getStringField(CONST_STATE));
         TechnologyState existingTechnologyState = TechnologyStateUtils.getStateFromField(existingTechnology
                 .getStringField(CONST_STATE));
 
         return TechnologyState.ACCEPTED == technologyState && technologyState == existingTechnologyState;
     }
 
     private boolean productComponentsContainProduct(List<Entity> components, Entity product) {
         boolean contains = false;
 
         for (Entity entity : components) {
             if (entity.getBelongsToField(CONST_PRODUCT).getId().equals(product.getId())) {
                 contains = true;
                 break;
             }
         }
 
         return contains;
     }
 
     private SearchCriteriaBuilder createSearchCriteria(Entity product, Entity technology, ProductDirection direction) {
         String model = direction.equals(ProductDirection.IN) ? TechnologiesConstants.MODEL_OPERATION_PRODUCT_IN_COMPONENT
                 : TechnologiesConstants.MODEL_OPERATION_PRODUCT_OUT_COMPONENT;
 
         DataDefinition dd = dataDefinitionService.get(TechnologiesConstants.PLUGIN_IDENTIFIER, model);
 
         SearchCriteriaBuilder search = dd.find();
         search.add(SearchRestrictions.eq("product.id", product.getId()));
         search.createAlias(CONST_OPERATION_COMPONENT, CONST_OPERATION_COMPONENT);
         search.add(SearchRestrictions.belongsTo("operationComponent.technology", technology));
 
         return search;
     }
 
     public String getProductType(Entity product, Entity technology) {
         SearchCriteriaBuilder searchIns = createSearchCriteria(product, technology, ProductDirection.IN);
         SearchCriteriaBuilder searchOuts = createSearchCriteria(product, technology, ProductDirection.OUT);
         SearchCriteriaBuilder searchOutsForRoots = createSearchCriteria(product, technology, ProductDirection.OUT);
         searchOutsForRoots.add(SearchRestrictions.isNull("operationComponent.parent"));
 
         boolean goesIn = productComponentsContainProduct(searchIns.list().getEntities(), product);
         boolean goesOut = productComponentsContainProduct(searchOuts.list().getEntities(), product);
         boolean goesOutInAroot = productComponentsContainProduct(searchOutsForRoots.list().getEntities(), product);
 
         if (goesOutInAroot) {
             return PRODUCT;
         }
 
         if (goesIn && !goesOut) {
             return COMPONENT;
         }
 
         if (goesIn && goesOut) {
             return INTERMEDIATE;
         }
 
         if (!goesIn && goesOut) {
             return WASTE;
         }
 
         return UNRELATED;
     }

    public void switchStateToDraftOnCopy(final DataDefinition technologyDataDefinition, final Entity technology) {
        technology.setField(CONST_STATE, TechnologyState.DRAFT.getStringValue());
    }
 }
