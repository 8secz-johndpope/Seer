 /*
  * Copyright 2002-2005 the original author or authors.
  * 
  * Licensed under the Apache License, Version 2.0 (the "License"); you may not
  * use this file except in compliance with the License. You may obtain a copy of
  * the License at
  * 
  * http://www.apache.org/licenses/LICENSE-2.0
  * 
  * Unless required by applicable law or agreed to in writing, software
  * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
  * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
  * License for the specific language governing permissions and limitations under
  * the License.
  */
 package org.springframework.binding.form.support;
 
 import java.beans.PropertyChangeEvent;
 import java.beans.PropertyChangeListener;
 import java.util.HashMap;
 import java.util.Map;
 
 import org.springframework.binding.MutablePropertyAccessStrategy;
 import org.springframework.binding.form.BindingErrorMessageProvider;
 import org.springframework.binding.form.ValidatingFormModel;
 import org.springframework.binding.validation.DefaultValidationResults;
 import org.springframework.binding.validation.DefaultValidationResultsModel;
 import org.springframework.binding.validation.RichValidator;
 import org.springframework.binding.validation.ValidationMessage;
 import org.springframework.binding.validation.ValidationResultsModel;
 import org.springframework.binding.validation.Validator;
 import org.springframework.binding.validation.support.RulesValidator;
 import org.springframework.binding.value.ValueModel;
 import org.springframework.binding.value.support.AbstractValueModelWrapper;
 import org.springframework.core.style.ToStringCreator;
 import org.springframework.richclient.util.Assert;
 
 /**
  * Default form model implementation. Is configurable, hierarchical and validating.
  * <p>
  * If you need this form model to use validation rules that are specific to a given
  * context (such as a specific form), then you will need to call {@link #setValidator(Validator)}
  * with a validator configured with the required context id.  Like this:
  * <code>
  * RulesValidator validator = myValidatingFormModel.getValidator();
  * validator.setRulesContextId( "mySpecialFormId" );
  * </code>
  * Along with this you will need to register your rules using the context id.  See
  * {@link DefaultRulesSource#addRules(String, Rules)}.
  * 
  * @author  Keith Donald
  * @author  Oliver Hutchison 
  */
 public class DefaultFormModel extends AbstractFormModel implements ValidatingFormModel {
 
     private final DefaultValidationResultsModel validationResultsModel = new DefaultValidationResultsModel();
     
     private final DefaultValidationResults additionalValidationResults = new DefaultValidationResults();
     
     private final Map bindingErrorMessages = new HashMap();
 
     private boolean validating = true;
    
     private boolean oldValidating = true;
 
     private Validator validator;
 
     private BindingErrorMessageProvider bindingErrorMessageProvider = new DefaultBindingErrorMessageProvider();
 
     public DefaultFormModel() {
         init();
     }
 
     public DefaultFormModel(Object domainObject) {
         super(domainObject);
         init();
     }
     
     public DefaultFormModel(Object domainObject, boolean buffered) {
         super(domainObject, buffered);
         init();
     }
 
     public DefaultFormModel(ValueModel domainObjectHolder) {
         super(domainObjectHolder);
         init();
     }
     
     public DefaultFormModel(ValueModel domainObjectHolder, boolean buffered) {
         super(domainObjectHolder, buffered);
         init();
     }
 
     public DefaultFormModel(MutablePropertyAccessStrategy domainObjectAccessStrategy) {
         super(domainObjectAccessStrategy, true);
         init();
     }
 
     public DefaultFormModel(MutablePropertyAccessStrategy domainObjectAccessStrategy, boolean bufferChanges) {
         super(domainObjectAccessStrategy, bufferChanges);
         init();
     }
     
     /**
      * Initialization of DefaultFormModel.
      * Adds a listener on the Enabled property in order to switch validating state
      * on or off. When disabling a formModel, no validation will happen.
      */
     protected void init()
     {
         addPropertyChangeListener(ENABLED_PROPERTY, new PropertyChangeListener(){
 
             public void propertyChange(PropertyChangeEvent evt)
             {
                validatingUpdated();                
             }
             
         });
        validationResultsModel.addPropertyChangeListener(ValidationResultsModel.HAS_ERRORS_PROPERTY, new PropertyChangeListener(){

            public void propertyChange(PropertyChangeEvent evt)
            {
                committableUpdated();
            }});
     }
 
     public boolean isValidating() {
        return validating && isEnabled();
     }
 
     public void setValidating(boolean validating) {
         this.validating = validating;
         validatingUpdated();
     }
 
     protected void validatingUpdated() {
         boolean validating = isValidating();
         if (hasChanged(oldValidating, validating)) {
             if (validating) {
                 validate();
             }
             else {
                 validationResultsModel.clearAllValidationResults();
             }
             oldValidating = validating;
             firePropertyChange(VALIDATING_PROPERTY, !validating, validating);
         }
     }
 
     public ValidationResultsModel getValidationResults() {
         return validationResultsModel;
     }
 
     public void validate() {
        if (isValidating()) {
             validateAfterPropertyChanged(null);
         }
     }
 
     public Validator getValidator() {
         if (validator == null) {
             validator = new RulesValidator(this);
         }
         return validator;
     }
 
     public void setValidator(Validator validator) {
         Assert.required(validator, "validator");
         this.validator = validator;
     }
 
     public boolean isCommittable() {
         final boolean superIsCommittable = super.isCommittable();
         final boolean hasNoErrors = !getValidationResults().getHasErrors();
         return superIsCommittable && hasNoErrors;
     }
 
     protected ValueModel preProcessNewValueModel(String formProperty, ValueModel formValueModel) {
         if (!(formValueModel instanceof ValidatingFormValueModel)) {
             return new ValidatingFormValueModel(formProperty, formValueModel, true);
         }
         return formValueModel;
     }
 
     protected void postProcessNewValueModel(String formProperty, ValueModel valueModel) {
         validateAfterPropertyChanged(formProperty);
     }
 
     protected ValueModel preProcessNewConvertingValueModel(String formProperty, Class targetClass,
             ValueModel formValueModel) {
         return new ValidatingFormValueModel(formProperty, formValueModel, false);
     }
 
     protected void postProcessNewConvertingValueModel(String formProperty, Class targetClass, ValueModel valueModel) {
     }
 
     protected void formPropertyValueChanged(String formProperty) {
         validateAfterPropertyChanged(formProperty);
     }
 
     /**
      * 
      * @param formProperty the name of the only property that has changed since the 
      * last call to validateAfterPropertyChange or <code>null</code> if this is not
      * known/available.
      */
     protected void validateAfterPropertyChanged(String formProperty) {
        if (isValidating()) {
             Validator validator = getValidator();
             if (validator != null) {
                 DefaultValidationResults validationResults = new DefaultValidationResults(bindingErrorMessages.values());
                 if (formProperty != null && validator instanceof RichValidator) {
                     validationResults.addAllMessages(((RichValidator)validator).validate(getFormObject(), formProperty));
                 }
                 else {
                     validationResults.addAllMessages(validator.validate(getFormObject()));
                 }
                 validationResults.addAllMessages(additionalValidationResults);
                 validationResultsModel.updateValidationResults(validationResults);
             }
         }
     }
 
     protected void raiseBindingError(ValidatingFormValueModel valueModel, Object valueBeingSet, Exception e) {
         ValidationMessage oldValidationMessage = (ValidationMessage)bindingErrorMessages.get(valueModel);
         ValidationMessage newValidationMessage = getBindingErrorMessage(valueModel.getFormProperty(), valueBeingSet, e);
         bindingErrorMessages.put(valueModel, newValidationMessage);
        if (isValidating()) {
             validationResultsModel.replaceMessage(oldValidationMessage, newValidationMessage);
         }
     }
     
     protected void clearBindingError(ValidatingFormValueModel valueModel) {
         ValidationMessage validationMessage = (ValidationMessage)bindingErrorMessages.remove(valueModel);
         if (validationMessage != null) {            
             validationResultsModel.removeMessage(validationMessage);
         }
     }
     
     /**
      * Allows subclasses to provide validation messages that are generated by
      * a process separate from the standard Validator.
      * <p>
      * All error messages that are raised using this method must be cleared using the
      * method @link #cleanValdationMessage(ValidationMessage) before the form model 
      * can be commited.
      * @param validationMessage the message to raise
      */
     protected void raiseValidationMessage(ValidationMessage validationMessage) {
         additionalValidationResults.addMessage(validationMessage);
        if (isValidating()) {
             validationResultsModel.addMessage(validationMessage);
         }
     }
     
     /**
      * Allows subclasses to clear validation messages that are generated by
      * a process separate from the standard Validator. 
      * @param validationMessage the message to clear
      */
     protected void clearValidationMessage(ValidationMessage validationMessage) {
         additionalValidationResults.removeMessage(validationMessage);
        if (isValidating()) {
             validationResultsModel.removeMessage(validationMessage);
         }
     }
 
     protected ValidationMessage getBindingErrorMessage(String propertyName, Object valueBeingSet, Exception e) {
         return bindingErrorMessageProvider.getErrorMessage(this, propertyName, valueBeingSet, e);
     }
     
     public void setBindingErrorMessageProvider(BindingErrorMessageProvider bindingErrorMessageProvider) {
         Assert.required(bindingErrorMessageProvider, "bindingErrorMessageProvider");
         this.bindingErrorMessageProvider = bindingErrorMessageProvider;
     }
 
     public String toString() {
         return new ToStringCreator(this).append("id", getId()).append("buffered", isBuffered()).append("enabled", isEnabled()).append(
                 "dirty", isDirty()).append("validating", isValidating()).append("validationResults",
                 getValidationResults()).toString();
     }
 
     protected class ValidatingFormValueModel extends AbstractValueModelWrapper {
         private final String formProperty;
 
         private final ValueChangeHandler valueChangeHander;
 
         public ValidatingFormValueModel(String formProperty, ValueModel model, boolean validateOnChange) {
             super(model);
             this.formProperty = formProperty;
             if (validateOnChange) {
                 this.valueChangeHander = new ValueChangeHandler();
                 addValueChangeListener(valueChangeHander);
             }
             else {
                 this.valueChangeHander = null;
             }
         }
 
         public String getFormProperty() {
             return formProperty;
         }
 
         public void setValueSilently(Object value, PropertyChangeListener listenerToSkip) {
             try {
                 if (logger.isDebugEnabled()) {
                     Class valueClass = (value != null ? value.getClass() : null);
                     logger.debug("Setting '" + formProperty + "' value to convert/validate '" + value + "', class="
                             + valueClass);
                 }
                 super.setValueSilently(value, listenerToSkip);
                 clearBindingError(this);
             }
             catch (Exception e) {
                 logger.debug("Exception occurred setting value", e);
                 raiseBindingError(this, value, e);
             }
         }
 
         public class ValueChangeHandler implements PropertyChangeListener {
             public void propertyChange(PropertyChangeEvent evt) {
                 formPropertyValueChanged(formProperty);
             }
         }
     }
 }
