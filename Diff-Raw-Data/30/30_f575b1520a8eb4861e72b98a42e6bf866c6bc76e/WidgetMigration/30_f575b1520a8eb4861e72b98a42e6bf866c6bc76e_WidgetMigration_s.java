 /**
  * Copyright (C) 2012 BonitaSoft S.A.
  * BonitaSoft, 32 rue Gustave Eiffel - 38000 Grenoble
  * This program is free software: you can redistribute it and/or modify
  * it under the terms of the GNU General Public License as published by
  * the Free Software Foundation, either version 2.0 of the License, or
  * (at your option) any later version.
  *
  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  * GNU General Public License for more details.
  *
  * You should have received a copy of the GNU General Public License
  * along with this program.  If not, see <http://www.gnu.org/licenses/>.
  */
 package org.bonitasoft.studio.importer.bar.custom.migration;
 
 import java.util.Date;
 import java.util.HashMap;
 import java.util.List;
 import java.util.Map;
 
 import org.bonitasoft.studio.common.ExpressionConstants;
 import org.bonitasoft.studio.importer.bar.i18n.Messages;
 import org.bonitasoft.studio.migration.migrator.ReportCustomMigration;
 import org.bonitasoft.studio.migration.utils.StringToExpressionConverter;
 import org.eclipse.core.runtime.IStatus;
 import org.eclipse.emf.edapt.migration.Instance;
 import org.eclipse.emf.edapt.migration.Metamodel;
 import org.eclipse.emf.edapt.migration.MigrationException;
 import org.eclipse.emf.edapt.migration.Model;
 
 /**
  * @author Romain Bioteau
  *
  */
 public class WidgetMigration extends ReportCustomMigration {
 
 	private Map<String, Instance> widgetActions = new HashMap<String,Instance>();
 	private Map<String, String> widgetInputs = new HashMap<String,String>();
 	private Map<String, String> widgetDisplayLabels = new HashMap<String,String>();
 	private Map<String, String> widgetTooltips = new HashMap<String,String>();
 	private Map<String, String> displayDependentWidgetOnlyAfterFirstEventTriggeredAndConditions = new HashMap<String,String>();
 	private Map<String, String> scriptAfterEvents = new HashMap<String,String>();
 	private Map<String, String> displayAfterEventDependsOnConditionScripts = new HashMap<String,String>();
 	private Map<String, String> helpMessages = new HashMap<String,String>();
 	private Map<String, String> injectWidgetScripts = new HashMap<String,String>();
 
 	@Override
 	public void migrateBefore(Model model, Metamodel metamodel)
 			throws MigrationException {
 		for(Instance widget : model.getAllInstances("form.Widget")){
 			if(!(widget.getContainer().instanceOf("expression.Expression"))){
 				final Instance action = widget.get("script");
 				if(action != null){
 					final String inputScript = action.get("inputScript");
 					final StringToExpressionConverter converter = getConverter(model,getScope(widget));
 					final Instance operation = converter.parseOperation(action, String.class.getName(), false);
 					model.delete(action);
 					widgetActions.put(widget.getUuid(), operation);
 					if(inputScript != null && !inputScript.trim().isEmpty()){
 						widgetInputs.put(widget.getUuid(), inputScript);
 					}
 				}
 				storeDisplayLabels(widget);
 				storeTooltips(widget);
 				storeDisplayDependentWidgetOnlyAfterFirstEventTriggeredAndConditions(widget);
 				storeScriptAfterEvents(widget, model);
 				storeDisplayAfterEventDependsOnConditionScripts(widget);
 				storeHelpMessages(widget);
 				storeInjectWidgetScripts(widget);
 			}
 		}
 	}
 
 	private String getDefaultReturnTypeForWidget(Instance widget) {
 		if(widget.instanceOf("form.CheckBoxMultipleFormField")
 				|| widget.instanceOf("form.DynamicTable")){
 			return List.class.getName();
 		}
 		if(widget.instanceOf("form.CheckBoxSingleFormField")){
 			return Boolean.class.getName();
 		}
 		if(widget.instanceOf("form.DateFormField")){
 			return Date.class.getName();
 		}
 		return String.class.getName();
 	}
 
 	private void storeDisplayLabels(Instance widget) {
 		final String displayLabel = widget.get("displayLabel");
 		widget.set("displayLabel", null);
 		if(displayLabel != null && !displayLabel.trim().isEmpty()){
 			widgetDisplayLabels.put(widget.getUuid(), displayLabel);
 		}
 	}
 
 	private void storeHelpMessages(Instance widget) {
 		final String helpMessage = widget.get("helpMessage");
 		widget.set("helpMessage", null);
 		if(helpMessage != null && !helpMessage.trim().isEmpty()){
 			helpMessages.put(widget.getUuid(), helpMessage);
 		}
 	}
 
 	private void storeInjectWidgetScripts(Instance widget) {
 		final String injectWidgetScript = widget.get("injectWidgetScript");
 		widget.set("injectWidgetScript", null);
 		if(injectWidgetScript != null && !injectWidgetScript.trim().isEmpty()){
 			injectWidgetScripts.put(widget.getUuid(), injectWidgetScript);
 		}
 	}
 
 	private void storeTooltips(Instance widget) {
 		final String tooltip = widget.get("tooltip");
 		widget.set("tooltip", null);
 		if(tooltip != null && !tooltip.trim().isEmpty()){
 			widgetTooltips.put(widget.getUuid(), tooltip);
 		}
 	}
 
 	private void storeDisplayAfterEventDependsOnConditionScripts(Instance widget) {
 		final String displayAfterEventDependsOnConditionScript = widget.get("displayAfterEventDependsOnConditionScript");
 		widget.set("displayAfterEventDependsOnConditionScript", null);
 		if(displayAfterEventDependsOnConditionScript != null && !displayAfterEventDependsOnConditionScript.trim().isEmpty()){
 			displayAfterEventDependsOnConditionScripts.put(widget.getUuid(), displayAfterEventDependsOnConditionScript);
 		}
 	}
 
 	private void storeScriptAfterEvents(Instance widget,Model model) {
 		final Instance scriptAfterEventGroovyScript = widget.get("scriptAfterEvent");
 		if(scriptAfterEventGroovyScript != null){
 			final String scriptAfterEvent = scriptAfterEventGroovyScript.get("inputScript");
 			model.delete(scriptAfterEventGroovyScript);
 			if(scriptAfterEvent != null && !scriptAfterEvent.trim().isEmpty()){
 				scriptAfterEvents.put(widget.getUuid(), scriptAfterEvent);
 			}
 		}
 	}
 
 	private void storeDisplayDependentWidgetOnlyAfterFirstEventTriggeredAndConditions(Instance widget) {
 		final String displayDependentWidgetOnlyAfterFirstEventTriggeredAndCondition = widget.get("displayDependentWidgetOnlyAfterFirstEventTriggeredAndCondition");
 		widget.set("displayDependentWidgetOnlyAfterFirstEventTriggeredAndCondition", null);
 		if(displayDependentWidgetOnlyAfterFirstEventTriggeredAndCondition != null && !displayDependentWidgetOnlyAfterFirstEventTriggeredAndCondition.trim().isEmpty()){
 			displayDependentWidgetOnlyAfterFirstEventTriggeredAndConditions.put(widget.getUuid(), displayDependentWidgetOnlyAfterFirstEventTriggeredAndCondition);
 		}
 	}
 
 
 
 	@Override
 	public void migrateAfter(Model model, Metamodel metamodel)
 			throws MigrationException {
 		for(Instance widget : model.getAllInstances("form.Widget")){
 			if(!(widget.getContainer().instanceOf("expression.Expression"))){
 				setWidgetActions(widget);
 				setReturnTypeModifier(widget);
 				setInputScripts(widget, model);
 				setDisplayLabels(widget, model);
 				setTooltips(widget, model);
 				setHelpMessages(widget, model);
 				setDisplayAfterEventDependsOnConditionScripts(widget,model);
 				setDisplayDependentWidgetOnlyAfterFirstEventTriggeredAndConditions(widget, model);
 				setInjectWidgetScripts(widget, model);
 				setScriptAfterEvents(widget,model);
 			}
 		}
 	}
 
 	private void setScriptAfterEvents(Instance widget, Model model) {
 		Instance expression = null ;
 		if(scriptAfterEvents.containsKey(widget.getUuid())){
 			expression = getConverter(model,getScope(widget)).parse(scriptAfterEvents.get(widget.getUuid()), String.class.getName(), false);
 			String description = Messages.updateValueMigrationDescription;
 			if(ExpressionConstants.SCRIPT_TYPE.equals(expression.get("type"))){
 				if(widget.instanceOf("form.MultipleValuatedFormField")){
 					expression.set("name", "updateAvailableValueScript");
 					description =  Messages.updateAvailableValueMigrationDescription;
 				}else{
 					expression.set("name", "updateValueScript");
 				}
 			}
 			addReportChange((String) widget.get("name"),widget.getEClass().getName(), widget.getUuid(), description, Messages.contingencyProperty, StringToExpressionConverter.getStatusForExpression(expression));
 		}else{
 			expression = StringToExpressionConverter.createExpressionInstance(model, "", "", String.class.getName(), ExpressionConstants.CONSTANT_TYPE, false);
 		}
 		widget.set("afterEventExpression", expression);
 	}
 
 	private void setInjectWidgetScripts(Instance widget, Model model) {
 		Instance expression = null ;
 		if(injectWidgetScripts.containsKey(widget.getUuid())){
 			expression = getConverter(model,getScope(widget)).parse(injectWidgetScripts.get(widget.getUuid()), Boolean.class.getName(), true);
 			if(ExpressionConstants.SCRIPT_TYPE.equals(expression.get("type"))){
 				expression.set("name", "insertWidgetScript");
 			}
 			addReportChange((String) widget.get("name"),widget.getEClass().getName(), widget.getUuid(), Messages.helpMessageMigrationDescription, Messages.optionsProperty, StringToExpressionConverter.getStatusForExpression(expression));
 		}else{
 			expression = StringToExpressionConverter.createExpressionInstance(model, "", "", Boolean.class.getName(), ExpressionConstants.CONSTANT_TYPE, true);
 		}
 		widget.set("injectWidgetScript", expression);
 	}
 
 	private void setDisplayDependentWidgetOnlyAfterFirstEventTriggeredAndConditions(Instance widget, Model model) {
 		Instance expression = null;
 		if(displayDependentWidgetOnlyAfterFirstEventTriggeredAndConditions.containsKey(widget.getUuid())){
 			expression = getConverter(model,getScope(widget)).parse(displayDependentWidgetOnlyAfterFirstEventTriggeredAndConditions.get(widget.getUuid()), Boolean.class.getName(), true);
 			if(ExpressionConstants.SCRIPT_TYPE.equals(expression.get("type"))){
 				expression.set("name", "showImmediatelyScript");
 			}
 			addReportChange((String) widget.get("name"),widget.getEClass().getName(), widget.getUuid(), Messages.showImmediatelyConditionMigrationDescription, Messages.contingencyProperty, StringToExpressionConverter.getStatusForExpression(expression));
 		}else{
 			expression = StringToExpressionConverter.createExpressionInstance(model, "true", "true", Boolean.class.getName(), ExpressionConstants.CONSTANT_TYPE, true);
 		}
 		widget.set("displayDependentWidgetOnlyAfterFirstEventTriggeredAndCondition", expression);
 	}
 
 	private void setDisplayAfterEventDependsOnConditionScripts(Instance widget, Model model) {
 		Instance expression = null;
 		if(displayAfterEventDependsOnConditionScripts.containsKey(widget.getUuid())){
 			expression = getConverter(model,getScope(widget)).parse(displayAfterEventDependsOnConditionScripts.get(widget.getUuid()), Boolean.class.getName(), true);
 			if(ExpressionConstants.SCRIPT_TYPE.equals(expression.get("type"))){
 				expression.set("name", "showWhenContingentFieldChangedScript");
 			}
 			addReportChange((String) widget.get("name"),widget.getEClass().getName(), widget.getUuid(), Messages.displayAfterEventConditionMigrationDescription, Messages.contingencyProperty, StringToExpressionConverter.getStatusForExpression(expression));
 		}else{
 			expression = StringToExpressionConverter.createExpressionInstance(model, "true", "true", Boolean.class.getName(), ExpressionConstants.CONSTANT_TYPE, true);
 		}
 		widget.set("displayAfterEventDependsOnConditionScript", expression);
 	}
 
 	private void setHelpMessages(Instance widget, Model model) {
 		Instance expression = null;
 		if(helpMessages.containsKey(widget.getUuid())){
 			expression = getConverter(model,getScope(widget)).parse(helpMessages.get(widget.getUuid()), String.class.getName(), true);
 			if(ExpressionConstants.SCRIPT_TYPE.equals(expression.get("type"))){
 				expression.set("name", "helpMessageScript");
 			}
 			addReportChange((String) widget.get("name"),widget.getEClass().getName(), widget.getUuid(), Messages.helpMessageMigrationDescription, Messages.userAidsProperty, StringToExpressionConverter.getStatusForExpression(expression));
 		}else{
 			expression = StringToExpressionConverter.createExpressionInstance(model, "", "", String.class.getName(), ExpressionConstants.CONSTANT_TYPE, true);
 		}
 		widget.set("helpMessage", expression);
 	}
 
 	private void setTooltips(Instance widget, Model model) {
 		Instance expression = null ;
 		if(widgetTooltips.containsKey(widget.getUuid())){
 			expression = getConverter(model,getScope(widget)).parse(widgetTooltips.get(widget.getUuid()), String.class.getName(), true);
 			if(ExpressionConstants.SCRIPT_TYPE.equals(expression.get("type"))){
 				expression.set("name", "tooltipScript");
 			}
 			addReportChange((String) widget.get("name"),widget.getEClass().getName(), widget.getUuid(), Messages.tooltipMigrationDescription, Messages.userAidsProperty, StringToExpressionConverter.getStatusForExpression(expression));
 		}else{
 			expression = StringToExpressionConverter.createExpressionInstance(model, "", "", String.class.getName(), ExpressionConstants.CONSTANT_TYPE, true);
 		}
 		widget.set("tooltip", expression);
 	}
 
 	private void setDisplayLabels(Instance widget, Model model) {
 		Instance expression = null;
 		if(widgetDisplayLabels.containsKey(widget.getUuid())){
 			expression = getConverter(model,getScope(widget)).parse(widgetDisplayLabels.get(widget.getUuid()), String.class.getName(), true);
 			if(ExpressionConstants.SCRIPT_TYPE.equals(expression.get("type"))){
 				expression.set("name", "displayLabelScript");
 			}
 			addReportChange((String) widget.get("name"),widget.getEClass().getName(), widget.getUuid(), Messages.displayLabelMigrationDescription, Messages.generalProperty, StringToExpressionConverter.getStatusForExpression(expression));
 		}else{
 			expression = StringToExpressionConverter.createExpressionInstance(model, "", "", String.class.getName(), ExpressionConstants.CONSTANT_TYPE, true);
 		}
 		widget.set("displayLabel", expression);
 	}
 
 	private void setInputScripts(Instance widget,Model model) {
 		Instance expression = null;
 		if(widgetInputs.containsKey(widget.getUuid())){
 			expression = getConverter(model,getScope(widget)).parse(widgetInputs.get(widget.getUuid()), String.class.getName(), false);
 			if(ExpressionConstants.VARIABLE_TYPE.equals(expression.get("type"))){
 				expression.set("returnType", StringToExpressionConverter.getDataReturnType(((List<Instance>) expression.get("referencedElements")).get(0)));
 				if(widget.getContainer().getContainer().instanceOf("process.AbstractProcess")){
 					addReportChange((String) widget.get("name"),widget.getEClass().getName(), widget.getUuid(), Messages.widgetDataInputAtProcessLevelMigrationDescription, Messages.dataProperty, IStatus.ERROR);
 				}else{
 					addReportChange((String) widget.get("name"),widget.getEClass().getName(), widget.getUuid(), Messages.widgetDataInputMigrationDescription, Messages.dataProperty, IStatus.OK);
 				}
 			}else if(ExpressionConstants.SCRIPT_TYPE.equals(expression.get("type"))){
 				if(widget.instanceOf("form.MultipleValuatedFormField")){
 					expression.set("returnType",List.class.getName());
 					expression.set("name","availableValuesScript");
 				}
 			}
 			addReportChange((String) widget.get("name"),widget.getEClass().getName(), widget.getUuid(), Messages.widgetDataInputMigrationDescription, Messages.dataProperty, StringToExpressionConverter.getStatusForExpression(expression));	
 		}else{
 			expression = StringToExpressionConverter.createExpressionInstance(model, "", "", String.class.getName(), ExpressionConstants.CONSTANT_TYPE, false);
 		}
 		widget.set("inputExpression", expression);
 	}
 
 	private void setReturnTypeModifier(Instance widget) {
 		if(!widget.instanceOf("form.TextFormField")){
 			return;
 		}
 		final Instance operation = widget.get("action");
 		boolean added = false ; 
 		if(operation != null){
 			final Instance leftOperand = operation.get("leftOperand");
 			final Instance rightOperand = operation.get("rightOperand");
 			if(leftOperand != null){
 				final List<Instance> elements = leftOperand.get("referencedElements");
 				if(!elements.isEmpty()){
 					final Instance dataInstance = elements.get(0);
 					if(dataInstance.instanceOf("process.Data")){
 						String returnType = StringToExpressionConverter.getDataReturnType(dataInstance);
 						widget.set("returnTypeModifier", returnType);
 						if(rightOperand != null){
 							rightOperand.set("returnType", returnType);
 						}
 						added = true ;
 						addReportChange((String) widget.get("name"),widget.getType().getEClass().getName(), widget.getUuid(), Messages.widgetModifierMigrationDescription, Messages.dataProperty, IStatus.OK);
 					}
 				}
 			}
 		}
 		if(!added){
 			widget.set("returnTypeModifier", String.class.getName());
 		}
 	}
 
 	private void setWidgetActions(Instance widget) {
 		if(widgetActions.containsKey(widget.getUuid())){
 			Instance action = widgetActions.get(widget.getUuid());
 			Instance actionExp = action.get("rightOperand");
 			if(actionExp != null){
 				if(ExpressionConstants.FORM_FIELD_TYPE.equals(actionExp.get("type"))){
 					actionExp.set("returnType", getDefaultReturnTypeForWidget(widget));
 				}
 			}
 			widget.set("action", action);
 			if(!widget.instanceOf("form.Info")){
 				addReportChange((String) widget.get("name"),widget.getType().getEClass().getName(), widget.getUuid(), Messages.widgetActionsMigrationDescription, Messages.dataProperty, StringToExpressionConverter.getStatusForExpression((Instance) action.get("rightOperand")));
 			}
 		}
 	}
 
 }
