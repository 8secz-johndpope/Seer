 /*
  * (c) Copyright 2010-2011 AgileBirds
  *
  * This file is part of OpenFlexo.
  *
  * OpenFlexo is free software: you can redistribute it and/or modify
  * it under the terms of the GNU General Public License as published by
  * the Free Software Foundation, either version 3 of the License, or
  * (at your option) any later version.
  *
  * OpenFlexo is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
  * GNU General Public License for more details.
  *
  * You should have received a copy of the GNU General Public License
  * along with OpenFlexo. If not, see <http://www.gnu.org/licenses/>.
  *
  */
 package org.openflexo.fib.model;
 
 import java.awt.event.MouseEvent;
 import java.lang.reflect.Type;
 import java.util.Enumeration;
 import java.util.List;
 
 import javax.swing.Icon;
 import javax.swing.tree.TreeNode;
 
 import org.openflexo.antar.binding.Bindable;
 import org.openflexo.antar.binding.BindingDefinition;
 import org.openflexo.antar.binding.BindingDefinition.BindingDefinitionType;
 import org.openflexo.antar.binding.BindingModel;
 import org.openflexo.antar.binding.BindingVariableImpl;
 import org.openflexo.antar.binding.ParameterizedTypeImpl;
 import org.openflexo.fib.controller.FIBComponentDynamicModel;
 import org.openflexo.fib.model.validation.FixProposal;
 import org.openflexo.fib.model.validation.ValidationIssue;
 import org.openflexo.fib.model.validation.ValidationReport;
 import org.openflexo.fib.model.validation.ValidationRule;
 import org.openflexo.fib.model.validation.ValidationWarning;
 import org.openflexo.toolbox.StringUtils;
 
 public abstract class FIBWidget extends FIBComponent {
 
 	public static final BindingDefinition TOOLTIP = new BindingDefinition("tooltip", String.class, BindingDefinitionType.GET, false);
 	public static final BindingDefinition ENABLE = new BindingDefinition("enable", Boolean.class, BindingDefinitionType.GET, false);
 	public static final BindingDefinition FORMAT = new BindingDefinition("format", String.class, BindingDefinitionType.GET, false);
 	public static final BindingDefinition ICON = new BindingDefinition("icon", Icon.class, BindingDefinitionType.GET, false);
 	public static final BindingDefinition VALUE_VALIDATOR = new BindingDefinition("valueValidator", Boolean.class,
 			BindingDefinitionType.GET, false);
 	public static final BindingDefinition VALUE_CHANGED_ACTION = new BindingDefinition("valueChangedAction", Void.class,
 			BindingDefinitionType.EXECUTE, false);
 	public static final BindingDefinition CLICK_ACTION = new BindingDefinition("clickAction", Void.class, BindingDefinitionType.EXECUTE,
 			false);
 	public static final BindingDefinition DOUBLE_CLICK_ACTION = new BindingDefinition("doubleClickAction", Void.class,
 			BindingDefinitionType.EXECUTE, false);
 	public static final BindingDefinition RIGHT_CLICK_ACTION = new BindingDefinition("rightClickAction", Void.class,
 			BindingDefinitionType.EXECUTE, false);
 	public static final BindingDefinition ENTER_PRESSED_ACTION = new BindingDefinition("enterPressedAction", Void.class,
 			BindingDefinitionType.EXECUTE, false);
 
 	public static enum Parameters implements FIBModelAttribute {
 		enable,
 		format,
 		icon,
 		tooltip,
 		tooltipText,
 		localize,
 		manageDynamicModel,
 		readOnly,
 		clickAction,
 		doubleClickAction,
 		rightClickAction,
 		enterPressedAction,
 		valueChangedAction,
 		valueTransform,
 		valueValidator
 	}
 
 	private DataBinding tooltip;
 
 	private DataBinding enable;
 	private DataBinding format;
 	private DataBinding icon;
 
 	private Boolean manageDynamicModel = false;
 	private Boolean readOnly = false;
 	private Boolean localize = true;
 	private String tooltipText;
 	private DataBinding clickAction;
 	private DataBinding doubleClickAction;
 	private DataBinding rightClickAction;
 	private DataBinding enterPressedAction;
 	private DataBinding valueChangedAction;
 	private DataBinding valueValidator;
 
 	private BindingDefinition VALUE_TRANSFORMER;
 
 	private final FIBFormatter formatter;
 	private final FIBValueBindable valueBindable;
 	private final FIBEventListener eventListener;
 	private DataBinding valueTransform;
 
 	public FIBWidget() {
 		super();
 		formatter = new FIBFormatter();
 		valueBindable = new FIBValueBindable();
 		eventListener = new FIBEventListener();
 	}
 
 	@Override
 	public String getIdentifier() {
 		return null;
 	}
 
 	@Override
 	public Enumeration children() {
 		return null;
 	}
 
 	@Override
 	public boolean getAllowsChildren() {
 		return false;
 	}
 
 	@Override
 	public TreeNode getChildAt(int childIndex) {
 		return null;
 	}
 
 	@Override
 	public int getChildCount() {
 		return 0;
 	}
 
 	@Override
 	public int getIndex(TreeNode node) {
 		return -1;
 	}
 
 	@Override
 	public boolean isLeaf() {
 		return true;
 	}
 
 	public DataBinding getTooltip() {
 		if (tooltip == null) {
 			tooltip = new DataBinding(this, Parameters.tooltip, TOOLTIP);
 		}
 		return tooltip;
 	}
 
 	public void setTooltip(DataBinding tooltip) {
 		tooltip.setOwner(this);
 		tooltip.setBindingAttribute(Parameters.tooltip);
 		tooltip.setBindingDefinition(TOOLTIP);
 		this.tooltip = tooltip;
 	}
 
 	public DataBinding getEnable() {
 		if (enable == null) {
 			enable = new DataBinding(this, Parameters.enable, ENABLE);
 		}
 		return enable;
 	}
 
 	public void setEnable(DataBinding enable) {
 		enable.setOwner(this);
 		enable.setBindingAttribute(Parameters.enable);
 		enable.setBindingDefinition(ENABLE);
 		this.enable = enable;
 	}
 
 	@Override
 	public void finalizeDeserialization() {
 		super.finalizeDeserialization();
 		getEventListener().createEventListenerBindingModel();
 		if (enable != null) {
 			enable.finalizeDeserialization();
 		}
 		if (format != null) {
 			format.finalizeDeserialization();
 		}
 		if (icon != null) {
 			icon.finalizeDeserialization();
 		}
 		if (tooltip != null) {
 			tooltip.finalizeDeserialization();
 		}
 		if (clickAction != null) {
 			clickAction.finalizeDeserialization();
 		}
 		if (doubleClickAction != null) {
 			doubleClickAction.finalizeDeserialization();
 		}
 		if (rightClickAction != null) {
 			rightClickAction.finalizeDeserialization();
 		}
 		if (enterPressedAction != null) {
 			enterPressedAction.finalizeDeserialization();
 		}
 		if (valueChangedAction != null) {
 			valueChangedAction.finalizeDeserialization();
 		}
 		if (valueTransform != null) {
 			valueTransform.finalizeDeserialization();
 		}
 		if (valueValidator != null) {
 			valueValidator.finalizeDeserialization();
 		}
 	}
 
 	@Override
 	public Type getDataType() {
 		if (getData() != null && getData().getBinding() != null) {
 			return getData().getBinding().getAccessedType();
 		}
 		return getDefaultDataClass();
 
 	}
 
 	@Override
 	public abstract Type getDefaultDataClass();
 
 	// Default behaviour: only data is managed
 	@Override
 	public Type getDynamicAccessType() {
 		if (getManageDynamicModel()) {
 			if (getData() != null && getData().isSet()) {
 				return super.getDynamicAccessType();
 			} else {
 				Type[] args = new Type[1];
 				args[0] = getDataType();
 				return new ParameterizedTypeImpl(FIBComponentDynamicModel.class, args);
 			}
 		}
 		return null;
 	}
 
 	public Boolean getManageDynamicModel() {
 		return manageDynamicModel;
 	}
 
 	public void setManageDynamicModel(Boolean manageDynamicModel) {
 		FIBAttributeNotification<Boolean> notification = requireChange(Parameters.manageDynamicModel, manageDynamicModel);
 		if (notification != null) {
 			this.manageDynamicModel = manageDynamicModel;
 			updateBindingModel();
 			hasChanged(notification);
 		}
 	}
 
 	public Boolean getReadOnly() {
 		return readOnly;
 	}
 
 	public void setReadOnly(Boolean readOnly) {
 		FIBAttributeNotification<Boolean> notification = requireChange(Parameters.readOnly, readOnly);
 		if (notification != null) {
 			this.readOnly = readOnly;
 			hasChanged(notification);
 		}
 	}
 
 	public String getTooltipText() {
 		return tooltipText;
 	}
 
 	public void setTooltipText(String tooltipText) {
 		FIBAttributeNotification<String> notification = requireChange(Parameters.tooltipText, tooltipText);
 		if (notification != null) {
 			this.tooltipText = tooltipText;
 			hasChanged(notification);
 		}
 	}
 
 	@Override
 	public void setDataClass(Class<?> dataClass) {
 		super.setDataClass(dataClass);
 		VALUE_TRANSFORMER = null;
 		getValueTransform().setBindingDefinition(getValueTransformBindingDefinition());
 	}
 
 	public DataBinding getValueTransform() {
 		if (valueTransform == null) {
 			valueTransform = new DataBinding(valueBindable, Parameters.valueTransform, getValueTransformBindingDefinition());
 		}
 		return valueTransform;
 	}
 
 	public void setValueTransform(DataBinding valueTransform) {
 		FIBAttributeNotification<DataBinding> notification = requireChange(Parameters.valueTransform, valueTransform);
 		if (notification != null) {
 			valueTransform.setOwner(valueBindable);
 			valueTransform.setBindingAttribute(Parameters.valueTransform);
 			valueTransform.setBindingDefinition(getValueTransformBindingDefinition());
 			this.valueTransform = valueTransform;
 			hasChanged(notification);
 		}
 	}
 
 	public BindingDefinition getValueTransformBindingDefinition() {
 		if (VALUE_TRANSFORMER == null) {
 			VALUE_TRANSFORMER = new BindingDefinition("valueTransform", getDataType(), BindingDefinitionType.GET, false);
 		}
 		return VALUE_TRANSFORMER;
 	}
 
 	public FIBValueBindable getValueBindable() {
 		return valueBindable;
 	}
 
 	public DataBinding getFormat() {
 		if (format == null) {
 			format = new DataBinding(formatter, Parameters.format, FORMAT);
 		}
 		return format;
 	}
 
 	public void setFormat(DataBinding format) {
 		FIBAttributeNotification<DataBinding> notification = requireChange(Parameters.format, format);
 		if (notification != null) {
 			format.setOwner(formatter);
 			format.setBindingAttribute(Parameters.format);
 			format.setBindingDefinition(FORMAT);
 			this.format = format;
 			hasChanged(notification);
 		}
 	}
 
 	public DataBinding getIcon() {
 		if (icon == null) {
 			icon = new DataBinding(formatter, Parameters.icon, ICON);
 		}
 		return icon;
 	}
 
 	public void setIcon(DataBinding icon) {
 		FIBAttributeNotification<DataBinding> notification = requireChange(Parameters.icon, icon);
 		if (notification != null) {
 			icon.setOwner(formatter);
 			icon.setBindingAttribute(Parameters.icon);
 			icon.setBindingDefinition(ICON);
 			this.icon = icon;
 			hasChanged(notification);
 		}
 	}
 
 	public Boolean getLocalize() {
 		return localize;
 	}
 
 	public void setLocalize(Boolean localize) {
 		FIBAttributeNotification<Boolean> notification = requireChange(Parameters.localize, localize);
 		if (notification != null) {
 			this.localize = localize;
 			hasChanged(notification);
 		}
 	}
 
 	public FIBFormatter getFormatter() {
 		return formatter;
 	}
 
 	public Type getFormattedObjectType() {
 		return getDataType();
 	}
 
 	private class FIBFormatter extends FIBModelObject implements Bindable {
 		private BindingModel formatterBindingModel = null;
 
 		@Override
 		public BindingModel getBindingModel() {
 			if (formatterBindingModel == null) {
 				createFormatterBindingModel();
 			}
 			return formatterBindingModel;
 		}
 
 		private void createFormatterBindingModel() {
 			formatterBindingModel = new BindingModel(FIBWidget.this.getBindingModel());
 			formatterBindingModel.addToBindingVariables(new BindingVariableImpl<Object>(this, "object", Object.class) {
 				@Override
 				public Type getType() {
 					return getFormattedObjectType();
 				}
 			});
 		}
 
 		@Override
 		public FIBComponent getRootComponent() {
 			return FIBWidget.this.getRootComponent();
 		}
 
 		@Override
 		public String toString() {
 			if (FIBWidget.this instanceof FIBDropDown) {
 				return "FIBFormatter[" + FIBWidget.this + "] iteratorClass=" + ((FIBDropDown) FIBWidget.this).getIteratorClass()
 						+ " dataType=" + ((FIBDropDown) FIBWidget.this).getDataType() + " obtained from "
 						+ ((FIBDropDown) FIBWidget.this).getDescription();
 			}
 			return "FIBFormatter[" + FIBWidget.this + "]" + " dataType=" + FIBWidget.this.getDataType();
 		}
 
 		@Override
 		public void notifyBindingChanged(DataBinding binding) {
 			if (binding == getFormat()) {
 				FIBWidget.this.notifyBindingChanged(binding);
 			}
 			super.notifyBindingChanged(binding);
 		}
 
 		@Override
 		public List<? extends FIBModelObject> getEmbeddedObjects() {
 			return null;
 		}
 	}
 
 	private class FIBValueBindable extends FIBModelObject implements Bindable {
 		private BindingModel valueTransformerBindingModel = null;
 
 		@Override
 		public BindingModel getBindingModel() {
 			if (valueTransformerBindingModel == null) {
 				createValueTransformerBindingModel();
 			}
 			return valueTransformerBindingModel;
 		}
 
 		private void createValueTransformerBindingModel() {
 			valueTransformerBindingModel = new BindingModel(FIBWidget.this.getBindingModel());
 			valueTransformerBindingModel.addToBindingVariables(new BindingVariableImpl<Object>(this, "value", Object.class) {
 				@Override
 				public Type getType() {
 					return getDataType();
 				}
 			});
 		}
 
 		@Override
 		public FIBComponent getRootComponent() {
 			return FIBWidget.this.getRootComponent();
 		}
 
 		@Override
 		public String toString() {
 			if (FIBWidget.this instanceof FIBDropDown) {
 				return "FIBValueBindable[" + FIBWidget.this + "] iteratorClass=" + ((FIBDropDown) FIBWidget.this).getIteratorClass()
 						+ " dataType=" + ((FIBDropDown) FIBWidget.this).getDataType() + " obtained from "
 						+ ((FIBDropDown) FIBWidget.this).getDescription();
 			}
 			return "FIBValueBindable[" + FIBWidget.this + "]" + " dataType=" + FIBWidget.this.getDataType();
 		}
 
 		@Override
 		public void notifyBindingChanged(DataBinding binding) {
 			if (binding == getValueTransform()) {
 				FIBWidget.this.notifyBindingChanged(binding);
 			} else if (binding == getValueValidator()) {
 				FIBWidget.this.notifyBindingChanged(binding);
 			}
 			super.notifyBindingChanged(binding);
 		}
 
 		@Override
 		public List<? extends FIBModelObject> getEmbeddedObjects() {
 			return null;
 		}
 	}
 
 	@Override
 	public void updateBindingModel() {
 		super.updateBindingModel();
 		getEventListener().createEventListenerBindingModel();
		getFormatter().createFormatterBindingModel();
 	}
 
 	public FIBEventListener getEventListener() {
 		return eventListener;
 	}
 
 	private class FIBEventListener extends FIBModelObject implements Bindable {
 		private BindingModel eventListenerBindingModel = null;
 
 		@Override
 		public BindingModel getBindingModel() {
 			if (eventListenerBindingModel == null) {
 				createEventListenerBindingModel();
 			}
 			return eventListenerBindingModel;
 		}
 
 		private void createEventListenerBindingModel() {
 			eventListenerBindingModel = new BindingModel(FIBWidget.this.getBindingModel());
 			eventListenerBindingModel.addToBindingVariables(new BindingVariableImpl<Object>(this, "event", MouseEvent.class));
 		}
 
 		@Override
 		public FIBComponent getRootComponent() {
 			return FIBWidget.this.getRootComponent();
 		}
 
 		@Override
 		public String toString() {
 			return "FIBEventListener[" + FIBWidget.this + "]";
 		}
 
 		@Override
 		public void notifyBindingChanged(DataBinding binding) {
 			if (binding == getClickAction() || binding == getDoubleClickAction() || binding == getRightClickAction()) {
 				FIBWidget.this.notifyBindingChanged(binding);
 			}
 			super.notifyBindingChanged(binding);
 		}
 
 		@Override
 		public List<? extends FIBModelObject> getEmbeddedObjects() {
 			return null;
 		}
 	}
 
 	public DataBinding getValueValidator() {
 		if (valueValidator == null) {
 			valueValidator = new DataBinding(this, Parameters.valueValidator, VALUE_VALIDATOR);
 		}
 		return valueValidator;
 	}
 
 	public void setValueValidator(DataBinding valueValidator) {
 		valueValidator.setOwner(this);
 		valueValidator.setBindingAttribute(Parameters.valueValidator);
 		valueValidator.setBindingDefinition(VALUE_VALIDATOR);
 		this.valueValidator = valueValidator;
 	}
 
 	public DataBinding getValueChangedAction() {
 		if (valueChangedAction == null) {
 			valueChangedAction = new DataBinding(this, Parameters.valueChangedAction, VALUE_CHANGED_ACTION);
 		}
 		return valueChangedAction;
 	}
 
 	public void setValueChangedAction(DataBinding valueChangedAction) {
 		valueChangedAction.setOwner(this);
 		valueChangedAction.setBindingAttribute(Parameters.valueChangedAction);
 		valueChangedAction.setBindingDefinition(VALUE_CHANGED_ACTION);
 		this.valueChangedAction = valueChangedAction;
 	}
 
 	public boolean hasClickAction() {
 		return clickAction != null && clickAction.isValid();
 	}
 
 	public final DataBinding getClickAction() {
 		if (clickAction == null) {
 			clickAction = new DataBinding(eventListener, Parameters.clickAction, CLICK_ACTION);
 		}
 		return clickAction;
 	}
 
 	public final void setClickAction(DataBinding clickAction) {
 		clickAction.setOwner(eventListener);
 		clickAction.setBindingAttribute(Parameters.clickAction);
 		clickAction.setBindingDefinition(CLICK_ACTION);
 		this.clickAction = clickAction;
 	}
 
 	public boolean hasDoubleClickAction() {
 		return doubleClickAction != null && doubleClickAction.isValid();
 	}
 
 	public DataBinding getDoubleClickAction() {
 		if (doubleClickAction == null) {
 			doubleClickAction = new DataBinding(eventListener, Parameters.doubleClickAction, DOUBLE_CLICK_ACTION);
 		}
 		return doubleClickAction;
 	}
 
 	public void setDoubleClickAction(DataBinding doubleClickAction) {
 		doubleClickAction.setOwner(eventListener);
 		doubleClickAction.setBindingAttribute(Parameters.doubleClickAction);
 		doubleClickAction.setBindingDefinition(DOUBLE_CLICK_ACTION);
 		this.doubleClickAction = doubleClickAction;
 	}
 
 	public boolean hasRightClickAction() {
 		return rightClickAction != null && rightClickAction.isValid();
 	}
 
 	public DataBinding getRightClickAction() {
 		if (rightClickAction == null) {
 			rightClickAction = new DataBinding(eventListener, Parameters.rightClickAction, RIGHT_CLICK_ACTION);
 		}
 		return rightClickAction;
 	}
 
 	public void setRightClickAction(DataBinding rightClickAction) {
 		rightClickAction.setOwner(eventListener);
 		rightClickAction.setBindingAttribute(Parameters.rightClickAction);
 		rightClickAction.setBindingDefinition(RIGHT_CLICK_ACTION);
 		this.rightClickAction = rightClickAction;
 	}
 
 	public boolean hasEnterPressedAction() {
 		return enterPressedAction != null && enterPressedAction.isValid();
 	}
 
 	public DataBinding getEnterPressedAction() {
 		if (enterPressedAction == null) {
 			enterPressedAction = new DataBinding(this, Parameters.enterPressedAction, ENTER_PRESSED_ACTION);
 		}
 		return enterPressedAction;
 	}
 
 	public void setEnterPressedAction(DataBinding enterPressedAction) {
 		enterPressedAction.setOwner(this);
 		enterPressedAction.setBindingAttribute(Parameters.enterPressedAction);
 		enterPressedAction.setBindingDefinition(ENTER_PRESSED_ACTION);
 		this.enterPressedAction = enterPressedAction;
 	}
 
 	public boolean isPaletteElement() {
 		return getParameter("isPaletteElement") != null && getParameter("isPaletteElement").equalsIgnoreCase("true");
 	}
 
 	@Override
 	public List<? extends FIBModelObject> getEmbeddedObjects() {
 		return null;
 	}
 
 	@Override
 	protected void applyValidation(ValidationReport report) {
 		super.applyValidation(report);
 		performValidation(FIBWidgetDeclaredAsDynamicShouldHaveAName.class, report);
 		performValidation(TooltipBindingMustBeValid.class, report);
 		performValidation(EnableBindingMustBeValid.class, report);
 		performValidation(FormatBindingMustBeValid.class, report);
 		performValidation(IconBindingMustBeValid.class, report);
 		performValidation(ClickActionBindingMustBeValid.class, report);
 		performValidation(DoubleClickActionBindingMustBeValid.class, report);
 		performValidation(RightClickActionBindingMustBeValid.class, report);
 		performValidation(ValueChangeActionBindingMustBeValid.class, report);
 	}
 
 	public static class FIBWidgetDeclaredAsDynamicShouldHaveAName extends
 			ValidationRule<FIBWidgetDeclaredAsDynamicShouldHaveAName, FIBWidget> {
 		public FIBWidgetDeclaredAsDynamicShouldHaveAName() {
 			super(FIBWidget.class, "widgets_declaring_managing_dynamic_model_should_have_a_name");
 		}
 
 		@Override
 		public ValidationIssue<FIBWidgetDeclaredAsDynamicShouldHaveAName, FIBWidget> applyValidation(FIBWidget object) {
 			if (object.getManageDynamicModel() && StringUtils.isEmpty(object.getName())) {
 				GenerateDefaultName fixProposal1 = new GenerateDefaultName();
 				DisableDynamicModelManagement fixProposal2 = new DisableDynamicModelManagement();
 				return new ValidationWarning<FIBWidgetDeclaredAsDynamicShouldHaveAName, FIBWidget>(this, object,
 						"widget_($object.toString)_declares_managing_dynamic_model_but_does_not_have_a_name", fixProposal1, fixProposal2);
 			}
 			return null;
 		}
 
 		protected static class GenerateDefaultName extends FixProposal<FIBWidgetDeclaredAsDynamicShouldHaveAName, FIBWidget> {
 
 			public GenerateDefaultName() {
 				super("generate_default_name_:_($defaultName)");
 			}
 
 			@Override
 			protected void fixAction() {
 				getObject().setName(getDefaultName());
 			}
 
 			public String getDefaultName() {
 				return getObject().generateUniqueName(getObject().getBaseName());
 			}
 
 		}
 
 		protected static class DisableDynamicModelManagement extends FixProposal<FIBWidgetDeclaredAsDynamicShouldHaveAName, FIBWidget> {
 
 			public DisableDynamicModelManagement() {
 				super("disable_dynamic_model_management");
 			}
 
 			@Override
 			protected void fixAction() {
 				getObject().setManageDynamicModel(false);
 			}
 
 		}
 	}
 
 	public static class TooltipBindingMustBeValid extends BindingMustBeValid<FIBWidget> {
 		public TooltipBindingMustBeValid() {
 			super("'tooltip'_binding_is_not_valid", FIBWidget.class);
 		}
 
 		@Override
 		public DataBinding getBinding(FIBWidget object) {
 			return object.getTooltip();
 		}
 
 		@Override
 		public BindingDefinition getBindingDefinition(FIBWidget object) {
 			return TOOLTIP;
 		}
 	}
 
 	public static class EnableBindingMustBeValid extends BindingMustBeValid<FIBWidget> {
 		public EnableBindingMustBeValid() {
 			super("'enable'_binding_is_not_valid", FIBWidget.class);
 		}
 
 		@Override
 		public DataBinding getBinding(FIBWidget object) {
 			return object.getEnable();
 		}
 
 		@Override
 		public BindingDefinition getBindingDefinition(FIBWidget object) {
 			return ENABLE;
 		}
 	}
 
 	public static class FormatBindingMustBeValid extends BindingMustBeValid<FIBWidget> {
 		public FormatBindingMustBeValid() {
 			super("'format'_binding_is_not_valid", FIBWidget.class);
 		}
 
 		@Override
 		public DataBinding getBinding(FIBWidget object) {
 			return object.getFormat();
 		}
 
 		@Override
 		public BindingDefinition getBindingDefinition(FIBWidget object) {
 			return FORMAT;
 		}
 	}
 
 	public static class IconBindingMustBeValid extends BindingMustBeValid<FIBWidget> {
 		public IconBindingMustBeValid() {
 			super("'icon'_binding_is_not_valid", FIBWidget.class);
 		}
 
 		@Override
 		public DataBinding getBinding(FIBWidget object) {
 			return object.getIcon();
 		}
 
 		@Override
 		public BindingDefinition getBindingDefinition(FIBWidget object) {
 			return ICON;
 		}
 	}
 
 	public static class ClickActionBindingMustBeValid extends BindingMustBeValid<FIBWidget> {
 		public ClickActionBindingMustBeValid() {
 			super("'click_action'_binding_is_not_valid", FIBWidget.class);
 		}
 
 		@Override
 		public DataBinding getBinding(FIBWidget object) {
 			return object.getClickAction();
 		}
 
 		@Override
 		public BindingDefinition getBindingDefinition(FIBWidget object) {
 			return CLICK_ACTION;
 		}
 	}
 
 	public static class DoubleClickActionBindingMustBeValid extends BindingMustBeValid<FIBWidget> {
 		public DoubleClickActionBindingMustBeValid() {
 			super("'double_click_action'_binding_is_not_valid", FIBWidget.class);
 		}
 
 		@Override
 		public DataBinding getBinding(FIBWidget object) {
 			return object.getDoubleClickAction();
 		}
 
 		@Override
 		public BindingDefinition getBindingDefinition(FIBWidget object) {
 			return DOUBLE_CLICK_ACTION;
 		}
 	}
 
 	public static class RightClickActionBindingMustBeValid extends BindingMustBeValid<FIBWidget> {
 		public RightClickActionBindingMustBeValid() {
 			super("'right_click_action'_binding_is_not_valid", FIBWidget.class);
 		}
 
 		@Override
 		public DataBinding getBinding(FIBWidget object) {
 			return object.getRightClickAction();
 		}
 
 		@Override
 		public BindingDefinition getBindingDefinition(FIBWidget object) {
 			return RIGHT_CLICK_ACTION;
 		}
 	}
 
 	public static class ValueChangeActionBindingMustBeValid extends BindingMustBeValid<FIBWidget> {
 		public ValueChangeActionBindingMustBeValid() {
 			super("'value_change_acion'_binding_is_not_valid", FIBWidget.class);
 		}
 
 		@Override
 		public DataBinding getBinding(FIBWidget object) {
 			return object.getValueChangedAction();
 		}
 
 		@Override
 		public BindingDefinition getBindingDefinition(FIBWidget object) {
 			return VALUE_CHANGED_ACTION;
 		}
 	}
 
 }
