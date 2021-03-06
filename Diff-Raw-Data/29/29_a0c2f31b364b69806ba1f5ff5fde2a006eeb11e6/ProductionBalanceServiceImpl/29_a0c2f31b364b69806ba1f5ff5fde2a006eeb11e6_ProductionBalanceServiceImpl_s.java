 /**
  * ***************************************************************************
  * Copyright (c) 2010 Qcadoo Limited
  * Project: Qcadoo MES
  * Version: 1.1.5
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
 package com.qcadoo.mes.productionCounting.internal;
 
 import static com.qcadoo.mes.orders.constants.OrderFields.PRODUCTION_LINE;
 import static com.qcadoo.mes.orders.constants.OrderFields.TECHNOLOGY_INSTANCE_OPERATION_COMPONENTS;
 import static com.qcadoo.mes.orders.constants.OrdersConstants.MODEL_ORDER;
 import static com.qcadoo.mes.productionCounting.internal.constants.BalanceOperationProductInComponentFields.BALANCE;
 import static com.qcadoo.mes.productionCounting.internal.constants.BalanceOperationProductInComponentFields.PLANNED_QUANTITY;
 import static com.qcadoo.mes.productionCounting.internal.constants.BalanceOperationProductInComponentFields.PRODUCT;
 import static com.qcadoo.mes.productionCounting.internal.constants.BalanceOperationProductInComponentFields.USED_QUANTITY;
 import static com.qcadoo.mes.productionCounting.internal.constants.CalculateOperationCostsMode.HOURLY;
 import static com.qcadoo.mes.productionCounting.internal.constants.CalculateOperationCostsMode.PIECEWORK;
 import static com.qcadoo.mes.productionCounting.internal.constants.OperationPieceworkComponentFields.CYCLES;
 import static com.qcadoo.mes.productionCounting.internal.constants.OperationPieceworkComponentFields.CYCLES_BALANCE;
 import static com.qcadoo.mes.productionCounting.internal.constants.OperationPieceworkComponentFields.PLANNED_CYCLES;
 import static com.qcadoo.mes.productionCounting.internal.constants.OperationPieceworkComponentFields.TECHNOLOGY_INSTANCE_OPERATION_COMPONENT;
 import static com.qcadoo.mes.productionCounting.internal.constants.OperationTimeComponentFields.LABOR_TIME;
 import static com.qcadoo.mes.productionCounting.internal.constants.OperationTimeComponentFields.LABOR_TIME_BALANCE;
 import static com.qcadoo.mes.productionCounting.internal.constants.OperationTimeComponentFields.MACHINE_TIME;
 import static com.qcadoo.mes.productionCounting.internal.constants.OperationTimeComponentFields.MACHINE_TIME_BALANCE;
 import static com.qcadoo.mes.productionCounting.internal.constants.OperationTimeComponentFields.PLANNED_LABOR_TIME;
 import static com.qcadoo.mes.productionCounting.internal.constants.OperationTimeComponentFields.PLANNED_MACHINE_TIME;
 import static com.qcadoo.mes.productionCounting.internal.constants.OrderFieldsPC.REGISTER_PIECEWORK;
 import static com.qcadoo.mes.productionCounting.internal.constants.OrderFieldsPC.REGISTER_PRODUCTION_TIME;
 import static com.qcadoo.mes.productionCounting.internal.constants.OrderFieldsPC.REGISTER_QUANTITY_IN_PRODUCT;
 import static com.qcadoo.mes.productionCounting.internal.constants.OrderFieldsPC.REGISTER_QUANTITY_OUT_PRODUCT;
 import static com.qcadoo.mes.productionCounting.internal.constants.OrderFieldsPC.TYPE_OF_PRODUCTION_RECORDING;
 import static com.qcadoo.mes.productionCounting.internal.constants.ProductionBalanceFields.BALANCE_OPERATION_PRODUCT_IN_COMPONENTS;
 import static com.qcadoo.mes.productionCounting.internal.constants.ProductionBalanceFields.BALANCE_OPERATION_PRODUCT_OUT_COMPONENTS;
 import static com.qcadoo.mes.productionCounting.internal.constants.ProductionBalanceFields.CALCULATE_OPERATION_COST_MODE;
 import static com.qcadoo.mes.productionCounting.internal.constants.ProductionBalanceFields.DATE;
 import static com.qcadoo.mes.productionCounting.internal.constants.ProductionBalanceFields.FILE_NAME;
 import static com.qcadoo.mes.productionCounting.internal.constants.ProductionBalanceFields.GENERATED;
 import static com.qcadoo.mes.productionCounting.internal.constants.ProductionBalanceFields.INCLUDE_ADDITIONAL_TIME;
 import static com.qcadoo.mes.productionCounting.internal.constants.ProductionBalanceFields.INCLUDE_TPZ;
 import static com.qcadoo.mes.productionCounting.internal.constants.ProductionBalanceFields.OPERATION_PIECEWORK_COMPONENTS;
 import static com.qcadoo.mes.productionCounting.internal.constants.ProductionBalanceFields.OPERATION_TIME_COMPONENTS;
 import static com.qcadoo.mes.productionCounting.internal.constants.ProductionBalanceFields.ORDER;
 import static com.qcadoo.mes.productionCounting.internal.constants.ProductionBalanceFields.RECORDS_NUMBER;
 import static com.qcadoo.mes.productionCounting.internal.constants.ProductionBalanceFields.WORKER;
 import static com.qcadoo.mes.productionCounting.internal.constants.ProductionCountingConstants.MODEL_BALANCE_OPERATION_PRODUCT_IN_COMPONENT;
 import static com.qcadoo.mes.productionCounting.internal.constants.ProductionCountingConstants.MODEL_BALANCE_OPERATION_PRODUCT_OUT_COMPONENT;
 import static com.qcadoo.mes.productionCounting.internal.constants.ProductionRecordFields.EXECUTED_OPERATION_CYCLES;
 import static com.qcadoo.mes.productionCounting.internal.constants.ProductionRecordFields.RECORD_OPERATION_PRODUCT_IN_COMPONENTS;
 import static com.qcadoo.mes.productionCounting.internal.constants.ProductionRecordFields.RECORD_OPERATION_PRODUCT_OUT_COMPONENTS;
 import static com.qcadoo.mes.productionCounting.internal.constants.ProductionRecordFields.STATE;
 import static com.qcadoo.mes.productionCounting.internal.constants.TypeOfProductionRecording.BASIC;
 import static com.qcadoo.mes.productionCounting.internal.constants.TypeOfProductionRecording.CUMULATED;
 import static com.qcadoo.mes.productionCounting.internal.constants.TypeOfProductionRecording.FOR_EACH;
 import static com.qcadoo.mes.productionCounting.internal.states.ProductionCountingStates.ACCEPTED;
 import static com.qcadoo.mes.technologies.constants.TechnologyInstanceOperCompFields.TECHNOLOGY_OPERATION_COMPONENT;
 import static com.qcadoo.mes.timeNormsForOperations.constants.TechnologyOperCompTNFOFields.LABOR_UTILIZATION;
 import static com.qcadoo.mes.timeNormsForOperations.constants.TechnologyOperCompTNFOFields.MACHINE_UTILIZATION;
 import static java.util.Arrays.asList;
 
 import java.io.IOException;
 import java.math.BigDecimal;
 import java.text.SimpleDateFormat;
 import java.util.Date;
 import java.util.List;
 import java.util.Locale;
 import java.util.Map;
 import java.util.Set;
 
 import org.springframework.beans.factory.annotation.Autowired;
 import org.springframework.context.i18n.LocaleContextHolder;
 import org.springframework.stereotype.Service;
 import org.springframework.transaction.annotation.Transactional;
 import org.springframework.util.StringUtils;
 
 import com.google.common.collect.Lists;
 import com.google.common.collect.Maps;
 import com.google.common.collect.Sets;
 import com.lowagie.text.DocumentException;
 import com.qcadoo.localization.api.utils.DateUtils;
 import com.qcadoo.mes.basic.constants.BasicConstants;
 import com.qcadoo.mes.operationTimeCalculations.OrderRealizationTimeService;
 import com.qcadoo.mes.orders.constants.OrdersConstants;
 import com.qcadoo.mes.productionCounting.internal.constants.ProductionCountingConstants;
 import com.qcadoo.mes.productionCounting.internal.constants.ProductionRecordFields;
 import com.qcadoo.mes.productionCounting.internal.print.ProductionBalancePdfService;
 import com.qcadoo.mes.technologies.ProductQuantitiesService;
 import com.qcadoo.mes.technologies.constants.TechnologiesConstants;
 import com.qcadoo.model.api.DataDefinition;
 import com.qcadoo.model.api.DataDefinitionService;
 import com.qcadoo.model.api.Entity;
 import com.qcadoo.model.api.EntityTree;
 import com.qcadoo.model.api.NumberService;
 import com.qcadoo.model.api.file.FileService;
 import com.qcadoo.model.api.search.SearchRestrictions;
 import com.qcadoo.report.api.ReportService;
 import com.qcadoo.security.api.SecurityService;
 import com.qcadoo.view.api.ComponentState;
 import com.qcadoo.view.api.ComponentState.MessageType;
 import com.qcadoo.view.api.ViewDefinitionState;
 
 @Service
 public class ProductionBalanceServiceImpl implements ProductionBalanceService {
 
     private static final String L_PLANNED_MACHINE_TIME = "plannedMachineTime";
 
     private static final String L_PLANNED_LABOR_TIME = "plannedLaborTime";
 
     @Autowired
     private DataDefinitionService dataDefinitionService;
 
     @Autowired
     private SecurityService securityService;
 
     @Autowired
     private ProductionBalancePdfService productionBalancePdfService;
 
     @Autowired
     private ProductionCountingGenerateProductionBalance generateProductionBalance;
 
     @Autowired
     private FileService fileService;
 
     @Autowired
     private ReportService reportService;
 
     @Autowired
     private NumberService numberService;
 
     @Autowired
     private ProductQuantitiesService productQuantitiesService;
 
     @Autowired
     private OrderRealizationTimeService orderRealizationTimeService;
 
     public void updateRecordsNumber(final DataDefinition productionBalanceDD, final Entity productionBalance) {
         Entity order = productionBalance.getBelongsToField(MODEL_ORDER);
 
         if ((order != null) && !isTypeOfProductionRecordingBasic(order)) {
             Integer recordsNumber = dataDefinitionService
                     .get(ProductionCountingConstants.PLUGIN_IDENTIFIER, ProductionCountingConstants.MODEL_PRODUCTION_RECORD)
                     .find().add(SearchRestrictions.eq(STATE, ACCEPTED.getStringValue()))
                     .add(SearchRestrictions.belongsTo(ORDER, order)).list().getEntities().size();
 
             productionBalance.setField(RECORDS_NUMBER, recordsNumber);
         }
     }
 
     public void clearGeneratedOnCopy(final DataDefinition productionBalanceDD, final Entity productionBalance) {
         productionBalance.setField(FILE_NAME, null);
         productionBalance.setField(GENERATED, false);
         productionBalance.setField(DATE, null);
         productionBalance.setField(WORKER, null);
     }
 
     public boolean validateOrder(final DataDefinition productionBalanceDD, final Entity productionBalance) {
         Entity order = productionBalance.getBelongsToField(MODEL_ORDER);
 
         if ((order == null) || isTypeOfProductionRecordingBasic(order)) {
             productionBalance.addError(productionBalanceDD.getField(MODEL_ORDER),
                     "productionCounting.productionBalance.report.error.orderWithoutRecordingType");
 
             return false;
         }
 
         if (!order.getBooleanField(REGISTER_PRODUCTION_TIME)
                 && HOURLY.getStringValue().equals(productionBalance.getField(CALCULATE_OPERATION_COST_MODE))) {
             productionBalance.addError(productionBalanceDD.getField(MODEL_ORDER),
                     "productionCounting.productionBalance.report.error.orderWithoutRegisterProductionTime");
 
             return false;
         } else if (!order.getBooleanField(REGISTER_PIECEWORK)
                 && PIECEWORK.getStringValue().equals(productionBalance.getField(CALCULATE_OPERATION_COST_MODE))) {
             productionBalance.addError(productionBalanceDD.getField(MODEL_ORDER),
                     "productionCounting.productionBalance.report.error.orderWithoutRegisterPiecework");
 
             return false;
         }
 
         List<Entity> productionRecordList = getProductionRecordsFromDB(order);
 
         if (productionRecordList.isEmpty()) {
             productionBalance.addError(productionBalanceDD.getField(MODEL_ORDER),
                     "productionCounting.productionBalance.report.error.orderWithoutProductionRecords");
 
             return false;
         }
 
         return true;
     }
 
     public void printProductionBalance(final ViewDefinitionState viewDefinitionState, final ComponentState state,
             final String[] args) {
         reportService.printGeneratedReport(viewDefinitionState, state, new String[] { args[0],
                 ProductionCountingConstants.PLUGIN_IDENTIFIER, ProductionCountingConstants.MODEL_PRODUCTION_BALANCE, args[1] });
     }
 
     @Transactional
     public void generateProductionBalance(final ViewDefinitionState viewDefinitionState, final ComponentState state,
             final String[] args) {
         state.performEvent(viewDefinitionState, "save", new String[0]);
 
         if (!state.isHasError()) {
             Entity productionBalance = getProductionBalanceFromDB((Long) state.getFieldValue());
 
             if (productionBalance == null) {
                 state.addMessage("qcadooView.message.entityNotFound", MessageType.FAILURE);
                 return;
             } else if (StringUtils.hasText(productionBalance.getStringField(FILE_NAME))) {
                 state.addMessage("productionCounting.productionBalance.report.error.documentsWasGenerated", MessageType.FAILURE);
                 return;
             }
 
             if (!productionBalance.getBooleanField(GENERATED)) {
                 fillReportValues(productionBalance);
 
                 fillFieldsAndGrids(productionBalance);
             }
 
             productionBalance = getProductionBalanceFromDB((Long) state.getFieldValue());
 
             try {
                 generateProductionBalanceDocuments(productionBalance, state.getLocale());
 
                 state.performEvent(viewDefinitionState, "reset", new String[0]);
 
                 state.addMessage(
                         "productionCounting.productionBalanceDetails.window.mainTab.productionBalanceDetails.generatedMessage",
                         MessageType.SUCCESS);
             } catch (IOException e) {
                 throw new IllegalStateException(e.getMessage(), e);
             } catch (DocumentException e) {
                 throw new IllegalStateException(e.getMessage(), e);
             }
         }
     }
 
     public void generateProductionBalanceDocuments(final Entity productionBalance, final Locale locale) throws IOException,
             DocumentException {
 
         String localePrefix = "productionCounting.productionBalance.report.fileName";
 
         Entity productionBalanceWithFileName = fileService.updateReportFileName(productionBalance, DATE, localePrefix);
 
         Entity company = getCompanyFromDB();
 
         try {
             productionBalancePdfService.generateDocument(productionBalanceWithFileName, company, locale);
 
             generateProductionBalance.notifyObserversThatTheBalanceIsBeingGenerated(productionBalance);
         } catch (IOException e) {
             throw new IllegalStateException("Problem with saving productionBalance report");
         } catch (DocumentException e) {
             throw new IllegalStateException("Problem with generating productionBalance report");
         }
     }
 
     private void fillReportValues(final Entity productionBalance) {
         productionBalance.setField(GENERATED, true);
         productionBalance.setField(DATE,
                 new SimpleDateFormat(DateUtils.L_DATE_TIME_FORMAT, LocaleContextHolder.getLocale()).format(new Date()));
         productionBalance.setField(WORKER, securityService.getCurrentUserName());
     }
 
     private void fillFieldsAndGrids(final Entity productionBalance) {
         Entity order = productionBalance.getBelongsToField(ORDER);
 
         if ((order == null) || isTypeOfProductionRecordingBasic(order)) {
             return;
         }
 
         List<Entity> productionRecords = getProductionRecordsFromDB(order);
 
         Map<Long, Entity> productionRecordsWithRegisteredTimes = groupProductionRecordsRegisteredTimes(productionBalance,
                 productionRecords);
 
         if (order.getBooleanField(REGISTER_QUANTITY_IN_PRODUCT)) {
             fillBalanceOperationProductComponents(productionBalance, productionRecords, RECORD_OPERATION_PRODUCT_IN_COMPONENTS,
                     BALANCE_OPERATION_PRODUCT_IN_COMPONENTS, MODEL_BALANCE_OPERATION_PRODUCT_IN_COMPONENT);
         }
 
         if (order.getBooleanField(REGISTER_QUANTITY_OUT_PRODUCT)) {
             fillBalanceOperationProductComponents(productionBalance, productionRecords, RECORD_OPERATION_PRODUCT_OUT_COMPONENTS,
                     BALANCE_OPERATION_PRODUCT_OUT_COMPONENTS, MODEL_BALANCE_OPERATION_PRODUCT_OUT_COMPONENT);
         }
 
         if (isCalculateOperationCostModeHourly(productionBalance) && order.getBooleanField(REGISTER_PRODUCTION_TIME)) {
             Map<Long, Map<String, Integer>> productionRecordsWithPlannedTimes = fillProductionRecordsWithPlannedTimes(
                     productionBalance, productionRecords);
 
             if (isTypeOfProductionRecordingForEach(order)) {
                 fillTimeValues(productionBalance, productionRecordsWithRegisteredTimes, productionRecordsWithPlannedTimes);
                 fillOperationTimeComponents(productionBalance, productionRecordsWithRegisteredTimes,
                         productionRecordsWithPlannedTimes);
             } else if (isTypeOfProductionRecordingCumulated(order)) {
                 fillTimeValues(productionBalance, productionRecordsWithRegisteredTimes, productionRecordsWithPlannedTimes);
             }
         } else if (isCalculateOperationCostModePiecework(productionBalance) && order.getBooleanField(REGISTER_PIECEWORK)) {
             fillOperationPieceworkComponents(productionBalance, productionRecordsWithRegisteredTimes);
         }
     }
 
     public Map<Long, Entity> groupProductionRecordsRegisteredTimes(final Entity productionBalance,
             final List<Entity> productionRecords) {
         Map<Long, Entity> groupedProductionRecords = Maps.newHashMap();
 
         if ((productionBalance != null) && (productionRecords != null)) {
             Entity order = productionBalance.getBelongsToField(ORDER);
 
             for (Entity productionRecord : productionRecords) {
                 Entity technologyInstanceOperationComponent = productionRecord
                         .getBelongsToField(TECHNOLOGY_INSTANCE_OPERATION_COMPONENT);
 
                 if (isTypeOfProductionRecordingForEach(order)) {
                     Long technologyInstanceOperationComponentId = technologyInstanceOperationComponent.getId();
 
                     if (groupedProductionRecords.containsKey(technologyInstanceOperationComponentId)) {
                         updateProductionRecordRegisteredTimes(groupedProductionRecords, productionRecord,
                                 technologyInstanceOperationComponentId);
                     } else {
                         addProductionRecordRegisteredTimes(groupedProductionRecords, productionRecord,
                                 technologyInstanceOperationComponentId);
                     }
                 } else if (isTypeOfProductionRecordingCumulated(order)) {
                     if (groupedProductionRecords.isEmpty()) {
                         addProductionRecordRegisteredTimes(groupedProductionRecords, productionRecord, 0L);
                     } else {
                         updateProductionRecordRegisteredTimes(groupedProductionRecords, productionRecord, 0L);
                     }
                 }
             }
         }
 
         return groupedProductionRecords;
     }
 
     private void addProductionRecordRegisteredTimes(Map<Long, Entity> groupedProductionRecords, final Entity productionRecord,
             Long technologyInstanceOperationComponentId) {
 
         Integer machineTime = getNotNullInteger((Integer) productionRecord.getField(MACHINE_TIME));
         Integer laborTime = getNotNullInteger((Integer) productionRecord.getField(LABOR_TIME));
         BigDecimal executedOperationCycles = getNotNullBigDecimal(productionRecord.getDecimalField(EXECUTED_OPERATION_CYCLES));
 
         productionRecord.setField(MACHINE_TIME, machineTime);
         productionRecord.setField(MACHINE_TIME, laborTime);
         productionRecord.setField(EXECUTED_OPERATION_CYCLES, numberService.setScale(executedOperationCycles));
 
         groupedProductionRecords.put(technologyInstanceOperationComponentId, productionRecord);
     }
 
     private void updateProductionRecordRegisteredTimes(Map<Long, Entity> groupedProductionRecords, final Entity productionRecord,
             final Long technologyInstanceOperationComponentId) {
         Entity addedProductionRecord = groupedProductionRecords.get(technologyInstanceOperationComponentId);
 
         Integer machineTime = (Integer) addedProductionRecord.getField(MACHINE_TIME);
         Integer laborTime = (Integer) addedProductionRecord.getField(LABOR_TIME);
         BigDecimal executedOperationCycles = addedProductionRecord.getDecimalField(EXECUTED_OPERATION_CYCLES);
 
         machineTime += getNotNullInteger((Integer) productionRecord.getField(MACHINE_TIME));
         laborTime += getNotNullInteger((Integer) productionRecord.getField(LABOR_TIME));

         executedOperationCycles = executedOperationCycles
                 .add(getNotNullBigDecimal(productionRecord.getDecimalField(EXECUTED_OPERATION_CYCLES)),
                         numberService.getMathContext());
 
         addedProductionRecord.setField(MACHINE_TIME, machineTime);
         addedProductionRecord.setField(LABOR_TIME, laborTime);
         addedProductionRecord.setField(EXECUTED_OPERATION_CYCLES, numberService.setScale(executedOperationCycles));
 
         groupedProductionRecords.put(technologyInstanceOperationComponentId, addedProductionRecord);
     }
 
     private void fillBalanceOperationProductComponents(final Entity productionBalance, final List<Entity> productionRecords,
             final String recordOperationProductComponentsModel, final String balanceOperationProductComponentsModel,
             final String balanceOperationProductComponentModel) {
         if (productionBalance == null) {
             return;
         }
 
         Entity order = productionBalance.getBelongsToField(ORDER);
 
         Map<Long, Entity> balanceOperationProductComponents = Maps.newHashMap();
         Set<Long> addedTechnologyInstanceOperationComponents = Sets.newHashSet();
 
         boolean shouldAddPlannedQuantity = true;
 
         for (Entity productionRecord : productionRecords) {
             List<Entity> recordOperationProductComponents = productionRecord
                     .getHasManyField(recordOperationProductComponentsModel);
 
             Entity technologyInstanceOperationComponent = productionRecord
                     .getBelongsToField(TECHNOLOGY_INSTANCE_OPERATION_COMPONENT);
 
             if (isTypeOfProductionRecordingForEach(order)) {
                 Long technologyInstanceOperationComponentId = technologyInstanceOperationComponent.getId();
 
                 if (addedTechnologyInstanceOperationComponents.contains(technologyInstanceOperationComponentId)) {
                     shouldAddPlannedQuantity = false;
                 } else {
                     shouldAddPlannedQuantity = true;
                 }
             }
 
             if (recordOperationProductComponents != null) {
                 for (Entity recordOperationProductComponent : recordOperationProductComponents) {
                     Entity product = recordOperationProductComponent.getBelongsToField(PRODUCT);
 
                     if (product != null) {
                         Long productId = product.getId();
 
                         if (balanceOperationProductComponents.containsKey(productId)) {
                             updateBalanceOperationComponent(balanceOperationProductComponents, recordOperationProductComponent,
                                     productId, shouldAddPlannedQuantity);
                         } else {
                             addBalanceOperationComponent(balanceOperationProductComponents,
                                     balanceOperationProductComponentModel, recordOperationProductComponent, productId);
                         }
                     }
                 }
             }
 
             if (isTypeOfProductionRecordingCumulated(order)) {
                 shouldAddPlannedQuantity = false;
             } else {
                 addedTechnologyInstanceOperationComponents.add(technologyInstanceOperationComponent.getId());
             }
         }
 
         productionBalance.setField(balanceOperationProductComponentsModel,
                 Lists.newArrayList(balanceOperationProductComponents.values()));
 
         productionBalance.getDataDefinition().save(productionBalance);
     }
 
     private void addBalanceOperationComponent(Map<Long, Entity> balanceOperationProductComponents,
             final String balanceOperationProductComponentModel, final Entity recordOperationProductComponent, final Long productId) {
         Entity balanceOperationProductComponent = dataDefinitionService.get(ProductionCountingConstants.PLUGIN_IDENTIFIER,
                 balanceOperationProductComponentModel).create();
 
         BigDecimal plannedQuantity = getNotNullBigDecimal(recordOperationProductComponent.getDecimalField(PLANNED_QUANTITY));
         BigDecimal usedQuantity = getNotNullBigDecimal(recordOperationProductComponent.getDecimalField(USED_QUANTITY));
 
         BigDecimal balance = usedQuantity.subtract(plannedQuantity, numberService.getMathContext());
 
         balanceOperationProductComponent.setField(PRODUCT, recordOperationProductComponent.getField(PRODUCT));
 
         balanceOperationProductComponent.setField(PLANNED_QUANTITY, numberService.setScale(plannedQuantity));
         balanceOperationProductComponent.setField(USED_QUANTITY, numberService.setScale(usedQuantity));
         balanceOperationProductComponent.setField(BALANCE, numberService.setScale(balance));
 
         balanceOperationProductComponents.put(productId, balanceOperationProductComponent);
     }
 
     private void updateBalanceOperationComponent(Map<Long, Entity> balanceOperationProductComponents,
             final Entity recordOperationProductComponent, final Long productId, final boolean shouldAddPlannedQuantity) {
         Entity addedBalanceOperationProductInComponent = balanceOperationProductComponents.get(productId);
 
         BigDecimal plannedQuantity = addedBalanceOperationProductInComponent.getDecimalField(PLANNED_QUANTITY);
         BigDecimal usedQuantity = addedBalanceOperationProductInComponent.getDecimalField(USED_QUANTITY);
 
         if (shouldAddPlannedQuantity) {
             plannedQuantity = plannedQuantity.add(
                     getNotNullBigDecimal(recordOperationProductComponent.getDecimalField(PLANNED_QUANTITY)),
                     numberService.getMathContext());
         }
 
         usedQuantity = usedQuantity.add(getNotNullBigDecimal(recordOperationProductComponent.getDecimalField(USED_QUANTITY)),
                 numberService.getMathContext());
 
         BigDecimal balance = usedQuantity.subtract(plannedQuantity, numberService.getMathContext());
 
         addedBalanceOperationProductInComponent.setField(PLANNED_QUANTITY, numberService.setScale(plannedQuantity));
         addedBalanceOperationProductInComponent.setField(USED_QUANTITY, numberService.setScale(usedQuantity));
         addedBalanceOperationProductInComponent.setField(BALANCE, numberService.setScale(balance));
 
         balanceOperationProductComponents.put(productId, addedBalanceOperationProductInComponent);
     }
 
     private void fillTimeValues(final Entity productionBalance, final Map<Long, Entity> productionRecordsWithRegisteredTimes,
             final Map<Long, Map<String, Integer>> productionRecordsWithPlannedTimes) {
         if (productionBalance == null) {
             return;
         }
 
         Integer plannedMachineTime = 0;
         Integer machineTime = 0;
 
         Integer laborTime = 0;
         Integer plannedLaborTime = 0;
 
         if (!productionRecordsWithPlannedTimes.isEmpty()) {
             for (Map.Entry<Long, Entity> productionRecordWithRegisteredTimesEntry : productionRecordsWithRegisteredTimes
                     .entrySet()) {
                 Long technologyInstanceOperationComponentId = productionRecordWithRegisteredTimesEntry.getKey();
                 Entity productionRecordWithRegisteredTimes = productionRecordWithRegisteredTimesEntry.getValue();
 
                 plannedMachineTime += productionRecordsWithPlannedTimes.get(technologyInstanceOperationComponentId).get(
                         L_PLANNED_MACHINE_TIME);
                 machineTime += (Integer) productionRecordWithRegisteredTimes.getField(MACHINE_TIME);
 
                 plannedLaborTime += productionRecordsWithPlannedTimes.get(technologyInstanceOperationComponentId).get(
                         L_PLANNED_LABOR_TIME);
                 laborTime += (Integer) productionRecordWithRegisteredTimes.getField(LABOR_TIME);
             }
         }
 
         Integer machineTimeBalance = machineTime - plannedMachineTime;
         Integer laborTimeBalance = laborTime - plannedLaborTime;
 
         productionBalance.setField(PLANNED_MACHINE_TIME, plannedMachineTime);
         productionBalance.setField(MACHINE_TIME, machineTime);
         productionBalance.setField(MACHINE_TIME_BALANCE, machineTimeBalance);
 
         productionBalance.setField(PLANNED_LABOR_TIME, plannedLaborTime);
         productionBalance.setField(LABOR_TIME, laborTime);
         productionBalance.setField(LABOR_TIME_BALANCE, laborTimeBalance);
 
         productionBalance.getDataDefinition().save(productionBalance);
     }
 
     private void fillOperationTimeComponents(final Entity productionBalance,
             final Map<Long, Entity> productionRecordsWithRegisteredTimes,
             final Map<Long, Map<String, Integer>> productionRecordsWithPlannedTimes) {
         if (productionBalance == null) {
             return;
         }
 
         List<Entity> operationTimeComponents = Lists.newArrayList();
 
         if (!productionRecordsWithPlannedTimes.isEmpty()) {
             for (Map.Entry<Long, Entity> productionRecordWithRegisteredTimesEntry : productionRecordsWithRegisteredTimes
                     .entrySet()) {
                 Long technologyInstanceOperationComponentId = productionRecordWithRegisteredTimesEntry.getKey();
                 Entity productionRecordWithRegisteredTimes = productionRecordWithRegisteredTimesEntry.getValue();
 
                 Entity operationTimeComponent = dataDefinitionService.get(ProductionCountingConstants.PLUGIN_IDENTIFIER,
                         ProductionCountingConstants.MODEL_OPERATION_TIME_COMPONENT).create();
 
                 Integer plannedMachineTime = productionRecordsWithPlannedTimes.get(technologyInstanceOperationComponentId).get(
                         L_PLANNED_MACHINE_TIME);
                 Integer machineTime = (Integer) productionRecordWithRegisteredTimes.getField(MACHINE_TIME);
 
                 Integer machineTimeBalance = machineTime - plannedMachineTime;
 
                 Integer plannedLaborTime = productionRecordsWithPlannedTimes.get(technologyInstanceOperationComponentId).get(
                         L_PLANNED_LABOR_TIME);
                 Integer laborTime = (Integer) productionRecordWithRegisteredTimes.getField(LABOR_TIME);
 
                 Integer laborTimeBalance = laborTime - plannedLaborTime;
 
                 operationTimeComponent.setField(TECHNOLOGY_INSTANCE_OPERATION_COMPONENT,
                         productionRecordWithRegisteredTimes.getBelongsToField(TECHNOLOGY_INSTANCE_OPERATION_COMPONENT));
 
                 operationTimeComponent.setField(PLANNED_MACHINE_TIME, plannedMachineTime);
                 operationTimeComponent.setField(MACHINE_TIME, machineTime);
                 operationTimeComponent.setField(MACHINE_TIME_BALANCE, machineTimeBalance);
 
                 operationTimeComponent.setField(PLANNED_LABOR_TIME, plannedLaborTime);
                 operationTimeComponent.setField(LABOR_TIME, laborTime);
                 operationTimeComponent.setField(LABOR_TIME_BALANCE, laborTimeBalance);
 
                 operationTimeComponents.add(operationTimeComponent);
             }
 
         }
 
         productionBalance.setField(OPERATION_TIME_COMPONENTS, operationTimeComponents);
 
         productionBalance.getDataDefinition().save(productionBalance);
     }
 
     private void fillOperationPieceworkComponents(final Entity productionBalance,
             final Map<Long, Entity> productionRecordsWithRegisteredTimes) {
         if (productionBalance == null) {
             return;
         }
 
         List<Entity> operationPieceworkComponents = Lists.newArrayList();
 
         Map<Entity, BigDecimal> operationRuns = Maps.newHashMap();
 
         Map<Entity, BigDecimal> productComponents = productQuantitiesService.getProductComponentQuantities(
                 asList(productionBalance.getBelongsToField(ORDER)), operationRuns);
 
         if (!productComponents.isEmpty()) {
             for (Map.Entry<Long, Entity> productionRecordWithRegisteredTimesEntry : productionRecordsWithRegisteredTimes
                     .entrySet()) {
                 Entity productionRecordWithRegisteredTimes = productionRecordWithRegisteredTimesEntry.getValue();
 
                 Entity operationPieceworkComponent = dataDefinitionService.get(ProductionCountingConstants.PLUGIN_IDENTIFIER,
                         ProductionCountingConstants.MODEL_OPERATION_PIECEWORK_COMPONENT).create();
 
                 operationPieceworkComponent.setField(TECHNOLOGY_INSTANCE_OPERATION_COMPONENT,
                         productionRecordWithRegisteredTimes.getBelongsToField(TECHNOLOGY_INSTANCE_OPERATION_COMPONENT));
 
                 Entity technologyInstanceOperationComponent = productionRecordWithRegisteredTimes
                         .getBelongsToField(TECHNOLOGY_INSTANCE_OPERATION_COMPONENT);
 
                 Entity proxyTechnologyOperationComponent = technologyInstanceOperationComponent
                         .getBelongsToField(TECHNOLOGY_OPERATION_COMPONENT);
                 Long technologyOperationComponentId = proxyTechnologyOperationComponent.getId();
 
                 Entity technologyOperationComponent = getTechnologyOperationComponentFromDB(technologyOperationComponentId);
 
                 if ((technologyOperationComponent != null) && operationRuns.containsKey(technologyOperationComponent)) {
                     BigDecimal plannedCycles = operationRuns.get(technologyOperationComponent);
 
                     BigDecimal cycles = productionRecordWithRegisteredTimes.getDecimalField(EXECUTED_OPERATION_CYCLES);
 
                     BigDecimal cyclesBalance = cycles.subtract(plannedCycles, numberService.getMathContext());
 
                     operationPieceworkComponent.setField(PLANNED_CYCLES, numberService.setScale(plannedCycles));
                     operationPieceworkComponent.setField(CYCLES, numberService.setScale(cycles));
                     operationPieceworkComponent.setField(CYCLES_BALANCE, numberService.setScale(cyclesBalance));
 
                     operationPieceworkComponents.add(operationPieceworkComponent);
                 }
             }
         }
 
         productionBalance.setField(OPERATION_PIECEWORK_COMPONENTS, operationPieceworkComponents);
 
         productionBalance.getDataDefinition().save(productionBalance);
     }
 
     public Map<Long, Map<String, Integer>> fillProductionRecordsWithPlannedTimes(final Entity productionBalance,
             final List<Entity> productionRecords) {
         Map<Long, Map<String, Integer>> productionRecordsWithPlannedTimes = Maps.newHashMap();
 
         if ((productionBalance != null) && (productionRecords != null)) {
             Entity order = productionBalance.getBelongsToField(ORDER);
 
             for (Entity productionRecord : productionRecords) {
                 Map<String, Integer> plannedTimes = countPlannedTimes(productionBalance, productionRecord);
 
                 if (!plannedTimes.isEmpty()) {
                     Entity technologyInstanceOperationComponent = productionRecord
                             .getBelongsToField(TECHNOLOGY_INSTANCE_OPERATION_COMPONENT);
 
                     if (isTypeOfProductionRecordingForEach(order)) {
                         Long technologyInstanceOperationComponentId = technologyInstanceOperationComponent.getId();
 
                        if (productionRecordsWithPlannedTimes.containsKey(technologyInstanceOperationComponentId)) {
                            updateProductionRecordWithPlannedTimes(productionRecordsWithPlannedTimes, plannedTimes,
                                    technologyInstanceOperationComponentId);
                        } else {
                             addProductionRecordWithPlannedTimes(productionRecordsWithPlannedTimes, plannedTimes,
                                     technologyInstanceOperationComponentId);
                         }
                     } else if (isTypeOfProductionRecordingCumulated(order)) {
                         if (productionRecordsWithPlannedTimes.isEmpty()) {
                             addProductionRecordWithPlannedTimes(productionRecordsWithPlannedTimes, plannedTimes, 0L);
                        } else {
                            updateProductionRecordWithPlannedTimes(productionRecordsWithPlannedTimes, plannedTimes, 0L);
                         }
                     }
                 }
             }
         }
 
         return productionRecordsWithPlannedTimes;
     }
 
     private void addProductionRecordWithPlannedTimes(Map<Long, Map<String, Integer>> productionRecordsWithPlannedTimes,
             final Map<String, Integer> plannedTimes, final Long technologyInstanceOperationComponentId) {
         productionRecordsWithPlannedTimes.put(technologyInstanceOperationComponentId, plannedTimes);
     }
 
    private void updateProductionRecordWithPlannedTimes(Map<Long, Map<String, Integer>> productionRecordsWithPlannedTimes,
            final Map<String, Integer> plannedTimes, final Long technologyInstanceOperationComponentId) {
        Map<String, Integer> productionRecordPlannedTimes = productionRecordsWithPlannedTimes
                .get(technologyInstanceOperationComponentId);

        Integer plannedMachineTime = productionRecordPlannedTimes.get(L_PLANNED_MACHINE_TIME)
                + plannedTimes.get(L_PLANNED_MACHINE_TIME);

        Integer plannedLaborTime = productionRecordPlannedTimes.get(L_PLANNED_LABOR_TIME)
                + plannedTimes.get(L_PLANNED_LABOR_TIME);

        productionRecordPlannedTimes.put(L_PLANNED_MACHINE_TIME, plannedMachineTime);
        productionRecordPlannedTimes.put(L_PLANNED_LABOR_TIME, plannedLaborTime);

        addProductionRecordWithPlannedTimes(productionRecordsWithPlannedTimes, productionRecordPlannedTimes,
                technologyInstanceOperationComponentId);
    }

     private Map<String, Integer> countPlannedTimes(final Entity productionBalance, final Entity productionRecord) {
         Map<String, Integer> plannedTimes = Maps.newHashMap();
 
         if ((productionBalance != null) && (productionRecord != null)) {
             Entity order = productionRecord.getBelongsToField(OrdersConstants.MODEL_ORDER);
 
             if ((order == null) || !order.getBooleanField(REGISTER_PRODUCTION_TIME)) {
                 return plannedTimes;
             }
 
             plannedTimes.put(L_PLANNED_MACHINE_TIME, 0);
             plannedTimes.put(L_PLANNED_LABOR_TIME, 0);
 
             Map<Entity, Integer> durationOperation = orderRealizationTimeService.estimateOperationTimeConsumptions(order,
                     (BigDecimal) order.getField(ProductionRecordFields.PLANNED_QUANTITY),
                     productionBalance.getBooleanField(INCLUDE_TPZ), productionBalance.getBooleanField(INCLUDE_ADDITIONAL_TIME),
                     order.getBelongsToField(PRODUCTION_LINE));
 
             if (isTypeOfProductionRecordingForEach(order)) {
                 countTimeOperation(productionRecord.getBelongsToField(TECHNOLOGY_INSTANCE_OPERATION_COMPONENT), plannedTimes,
                         durationOperation.get(productionRecord.getBelongsToField(TECHNOLOGY_INSTANCE_OPERATION_COMPONENT)
                                 .getBelongsToField(TECHNOLOGY_OPERATION_COMPONENT)));
             } else if (isTypeOfProductionRecordingCumulated(order)) {
                 EntityTree technologyInstanceOperationComponents = order.getTreeField(TECHNOLOGY_INSTANCE_OPERATION_COMPONENTS);
 
                 for (Entity technologyInstanceOperationComponent : technologyInstanceOperationComponents) {
                     countTimeOperation(technologyInstanceOperationComponent, plannedTimes,
                             durationOperation.get(technologyInstanceOperationComponent
                                     .getBelongsToField(TECHNOLOGY_OPERATION_COMPONENT)));
                 }
             }
         }
 
         return plannedTimes;
     }
 
     private void countTimeOperation(final Entity technologyInstanceOperationComponent, final Map<String, Integer> plannedTimes,
             final Integer durationOfOperation) {
         BigDecimal durationOfOperationComponent = BigDecimal.valueOf(durationOfOperation);
 
         Integer plannedMachineTime = plannedTimes.get(L_PLANNED_MACHINE_TIME)
                 + numberService.setScale(
                         durationOfOperationComponent.multiply(
                                 getNotNullBigDecimal(technologyInstanceOperationComponent.getDecimalField(MACHINE_UTILIZATION)),
                                 numberService.getMathContext())).intValue();
 
         Integer plannedLaborTime = plannedTimes.get(L_PLANNED_LABOR_TIME)
                 + numberService.setScale(
                         durationOfOperationComponent.multiply(
                                 getNotNullBigDecimal(technologyInstanceOperationComponent.getDecimalField(LABOR_UTILIZATION)),
                                 numberService.getMathContext())).intValue();
 
         plannedTimes.put(L_PLANNED_MACHINE_TIME, plannedMachineTime);
         plannedTimes.put(L_PLANNED_LABOR_TIME, plannedLaborTime);
     }
 
     private static BigDecimal getNotNullBigDecimal(final BigDecimal value) {
         if (value == null) {
             return BigDecimal.ZERO;
         }
         return value;
     }
 
     private static Integer getNotNullInteger(final Integer value) {
         if (value == null) {
             return 0;
         }
         return value;
     }
 
     public Entity getProductionBalanceFromDB(final Long productionBalanceId) {
         return dataDefinitionService.get(ProductionCountingConstants.PLUGIN_IDENTIFIER,
                 ProductionCountingConstants.MODEL_PRODUCTION_BALANCE).get(productionBalanceId);
     }
 
     public List<Entity> getProductionRecordsFromDB(final Entity order) {
         return dataDefinitionService
                 .get(ProductionCountingConstants.PLUGIN_IDENTIFIER, ProductionCountingConstants.MODEL_PRODUCTION_RECORD).find()
                 .add(SearchRestrictions.eq(STATE, ACCEPTED.getStringValue())).add(SearchRestrictions.belongsTo(ORDER, order))
                 .list().getEntities();
     }
 
     public Entity getTechnologyOperationComponentFromDB(final Long technologyOperationComponentId) {
         return dataDefinitionService.get(TechnologiesConstants.PLUGIN_IDENTIFIER,
                 TechnologiesConstants.MODEL_TECHNOLOGY_OPERATION_COMPONENT).get(technologyOperationComponentId);
     }
 
     public Entity getOrderFromDB(final Long orderId) {
         return dataDefinitionService.get(OrdersConstants.PLUGIN_IDENTIFIER, OrdersConstants.MODEL_ORDER).get(orderId);
     }
 
     public Entity getCompanyFromDB() {
         return dataDefinitionService.get(BasicConstants.PLUGIN_IDENTIFIER, BasicConstants.MODEL_COMPANY).find()
                 .add(SearchRestrictions.eq("owner", true)).setMaxResults(1).uniqueResult();
     }
 
     public boolean isCalculateOperationCostModeHourly(final Entity productionBalance) {
         return HOURLY.getStringValue().equals(productionBalance.getStringField(CALCULATE_OPERATION_COST_MODE));
     }
 
     public boolean isCalculateOperationCostModePiecework(final Entity productionBalance) {
         return PIECEWORK.getStringValue().equals(productionBalance.getStringField(CALCULATE_OPERATION_COST_MODE));
     }
 
     public boolean isTypeOfProductionRecordingBasic(final Entity order) {
         return BASIC.getStringValue().equals(order.getStringField(TYPE_OF_PRODUCTION_RECORDING));
     }
 
     public boolean isTypeOfProductionRecordingForEach(final Entity order) {
         return FOR_EACH.getStringValue().equals(order.getStringField(TYPE_OF_PRODUCTION_RECORDING));
     }
 
     public boolean isTypeOfProductionRecordingCumulated(final Entity order) {
         return CUMULATED.getStringValue().equals(order.getStringField(TYPE_OF_PRODUCTION_RECORDING));
     }
 }
