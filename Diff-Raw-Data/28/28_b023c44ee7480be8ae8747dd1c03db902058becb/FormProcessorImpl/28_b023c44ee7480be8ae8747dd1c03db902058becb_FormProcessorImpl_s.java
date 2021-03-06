 /**
  * Copyright (C) 2012 JBoss Inc
  *
  * Licensed under the Apache License, Version 2.0 (the "License");
  * you may not use this file except in compliance with the License.
  * You may obtain a copy of the License at
  *
  *       http://www.apache.org/licenses/LICENSE-2.0
  *
  * Unless required by applicable law or agreed to in writing, software
  * distributed under the License is distributed on an "AS IS" BASIS,
  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  * See the License for the specific language governing permissions and
  * limitations under the License.
  */
 package org.jbpm.formModeler.core.processing.impl;
 
 import org.apache.commons.logging.Log;
 import org.jbpm.formModeler.api.model.DataHolder;
 import org.jbpm.formModeler.api.processing.*;
 import org.jbpm.formModeler.core.FieldHandlersManager;
 import org.jbpm.formModeler.core.processing.ProcessingMessagedException;
 import org.jbpm.formModeler.core.processing.fieldHandlers.NumericFieldHandler;
 import org.jbpm.formModeler.core.processing.formProcessing.FormChangeProcessor;
 import org.jbpm.formModeler.core.processing.formProcessing.FormChangeResponse;
 import org.jbpm.formModeler.core.processing.formProcessing.NamespaceManager;
 import org.jbpm.formModeler.core.processing.formStatus.FormStatus;
 import org.jbpm.formModeler.core.processing.formStatus.FormStatusManager;
 import org.jbpm.formModeler.api.model.Field;
 import org.jbpm.formModeler.api.model.Form;
 import org.jbpm.formModeler.api.model.i18n.I18nSet;
 import org.apache.commons.lang.StringUtils;
 import org.jbpm.formModeler.core.config.FormManagerImpl;
 import org.jbpm.formModeler.api.util.helpers.CDIHelper;
 import org.jbpm.formModeler.api.processing.FormRenderContext;
 import org.jbpm.formModeler.api.processing.FormRenderContextManager;
 
 import javax.enterprise.context.ApplicationScoped;
 import javax.inject.Inject;
 import java.io.Serializable;
 import java.util.*;
 
 @ApplicationScoped
 public class FormProcessorImpl implements FormProcessor, Serializable {
 
     @Inject
     private Log log;
 
     // TODO: fix formulas
     //@Inject
     private FormChangeProcessor formChangeProcessor;
 
     @Inject
     private FieldHandlersManager fieldHandlersManager;
 
     @Inject
     FormRenderContextManager formRenderContextManager;
 
     protected FormStatus getContextFormStatus(FormRenderContext context) {
         return FormStatusManager.lookup().getFormStatus(context.getForm().getId(), context.getUID());
     }
 
     protected FormStatus getFormStatus(Long formId, String namespace) {
         return getFormStatus(formId, namespace, new HashMap());
     }
 
     protected FormStatus getFormStatus(Long formId, String namespace, Map currentValues) {
         FormStatus formStatus = FormStatusManager.lookup().getFormStatus(formId, namespace);
         return formStatus != null ? formStatus : createFormStatus(formId, namespace, currentValues);
     }
 
     protected boolean existsFormStatus(Long formId, String namespace) {
         FormStatus formStatus = FormStatusManager.lookup().getFormStatus(formId, namespace);
         return formStatus != null;
     }
 
     protected FormStatus createFormStatus(Long formId, String namespace) {
         return createFormStatus(formId, namespace, new HashMap());
     }
 
     protected FormStatus createFormStatus(Long formId, String namespace, Map currentValues) {
         FormStatus fStatus = FormStatusManager.lookup().createFormStatus(formId, namespace);
         setDefaultValues(formId, namespace, currentValues);
         return fStatus;
     }
 
     protected void setDefaultValues(Long formId, String namespace, Map currentValues) {
         Form pf = null;
         try {
             pf = getFormsManager().getFormById(formId);
         } catch (Exception e) {
             log.error("Error recovering Form with id " + formId + ", no field default values will be set", e);
         }
 
         if (pf != null) {
             Set formFields = pf.getFormFields();
             Map params = new HashMap(5);
             for (Iterator iterator = formFields.iterator(); iterator.hasNext();) {
                 Field pField = (Field) iterator.next();
                 Object value = currentValues.get(pField.getFieldName());
                 String inputName = getPrefix(pf, namespace) + pField.getFieldName();
 
                 try {
                     FieldHandler handler = fieldHandlersManager.getHandler(pField.getFieldType());
                     if ((value instanceof Map && !((Map)value).containsKey(FORM_MODE)) && !(value instanceof I18nSet)) ((Map)value).put(FORM_MODE, currentValues.get(FORM_MODE));
                    Map paramValue = handler.getParamValue(inputName, value, pField.getPattern());
                     if (paramValue != null && !paramValue.isEmpty()) params.putAll(paramValue);
 
                 /*
                 TODO: implement again formulas for default values
 
                 Object defaultValue = pField.get("defaultValueFormula");
                 String inputName = getPrefix(pf, namespace) + pField.getFieldName();
                 try {
                     String pattern = (String) pField.get("pattern");
                     Object value = currentValues.get(pField.getFieldName());
                     FieldHandler handler = pField.getFieldType().getManager();
                     if (value == null) {
                         if (defaultValue != null) {
                             if (!defaultValue.toString().startsWith("=")) {
                                 log.error("Incorrect formula specified for field " + pField.getFieldName());
                                 continue;
                             }
                             if (handler instanceof DefaultFieldHandler)
                                 value = ((DefaultFieldHandler) handler).evaluateFormula(pField, defaultValue.toString().substring(1), "", new HashMap(0), "", namespace, new Date());
                         }
                     }
                     if ((value instanceof Map && !((Map)value).containsKey(FormProcessor.FORM_MODE)) && !(value instanceof I18nSet) && !(value instanceof I18nObject)) ((Map)value).put(FormProcessor.FORM_MODE, currentValues.get(FormProcessor.FORM_MODE));
                     Map paramValue = handler.getParamValue(inputName, value, pattern);
                     if (paramValue != null && !paramValue.isEmpty()) params.putAll(paramValue); */
                 } catch (Exception e) {
                     log.error("Error obtaining default values for " + inputName, e);
                 }
             }
             setValues(pf, namespace, params, null, true);
         }
     }
 
     protected void destroyFormStatus(Long formId, String namespace) {
         FormStatusManager.lookup().destroyFormStatus(formId, namespace);
     }
 
     public void setValues(Form form, String namespace, Map parameterMap, Map filesMap) {
         setValues(form, namespace, parameterMap, filesMap, false);
     }
 
     public void setValues(Form form, String namespace, Map parameterMap, Map filesMap, boolean incremental) {
         if (form != null) {
             namespace = StringUtils.defaultIfEmpty(namespace, DEFAULT_NAMESPACE);
             FormStatus formStatus = getFormStatus(form.getId(), namespace);
             //  if (!incremental) formStatus.getWrongFields().clear();
             if (incremental) {
                 Map mergedParameterMap = new HashMap();
                 if (formStatus.getLastParameterMap() != null)
                     mergedParameterMap.putAll(formStatus.getLastParameterMap());
                 if (parameterMap != null)
                     mergedParameterMap.putAll(parameterMap);
                 formStatus.setLastParameterMap(mergedParameterMap);
             } else {
                 formStatus.setLastParameterMap(parameterMap);
             }
             String inputsPrefix = getPrefix(form, namespace);
             
             for (Field field : form.getFormFields()) {
                 setFieldValue(field, formStatus, inputsPrefix, parameterMap, filesMap, incremental);
             }
         }
     }
 
     public void modify(Form form, String namespace, String fieldName, Object value) {
         FormStatus formStatus = getFormStatus(form.getId(), namespace);
         formStatus.getInputValues().put(fieldName, value);
         propagateChangesToParentFormStatuses(formStatus, fieldName, value);
     }
 
     public void setAttribute(Form form, String namespace, String attributeName, Object attributeValue) {
         if (form != null) {
             FormStatus formStatus = getFormStatus(form.getId(), namespace);
             formStatus.getAttributes().put(attributeName, attributeValue);
         }
     }
 
     public Object getAttribute(Form form, String namespace, String attributeName) {
         if (form != null){
             FormStatus formStatus = getFormStatus(form.getId(), namespace);
             return formStatus.getAttributes().get(attributeName);
         }
         return null;
     }
 
     protected void setFieldValue(Field field, FormStatus formStatus, String inputsPrefix, Map parameterMap, Map filesMap, boolean incremental) {
         String fieldName = field.getFieldName();
         String inputName = inputsPrefix + fieldName;
         FieldHandler handler = fieldHandlersManager.getHandler(field.getFieldType());
         try {
             Object previousValue = formStatus.getInputValues().get(fieldName);
             boolean isRequired = field.getFieldRequired().booleanValue();
             
             if (!handler.isEvaluable(inputName, parameterMap, filesMap) && !(handler.isEmpty(previousValue) && isRequired)) return;
 
             Object value = null;
             boolean emptyNumber = false;
             try {
                 value = handler.getValue(field, inputName, parameterMap, filesMap, field.getFieldType().getFieldClass(), previousValue);
             } catch (NumericFieldHandler.EmptyNumberException ene) {
                 //Treat this case in particular, as returning null in a numeric field would make related formulas not working.
                 emptyNumber = true;
             }
             if (incremental && value == null && !emptyNumber) {
                 if (log.isDebugEnabled()) log.debug("Refusing to overwrite input value for parameter " + fieldName);
             } else {
                 formStatus.getInputValues().put(fieldName, value);
                 try {
                     propagateChangesToParentFormStatuses(formStatus, fieldName, value);
                 } catch (Exception e) {
                     log.error("Error modifying formStatus: ", e);
                 }
                 boolean isEmpty = handler.isEmpty(value);
                 if (isRequired && isEmpty && !incremental) {
                     log.debug("Missing required field " + fieldName);
                     formStatus.getWrongFields().add(fieldName);
                 } else {
                     formStatus.removeWrongField(fieldName);
                 }
             }
         } catch (ProcessingMessagedException pme) {
             log.debug("Processing field: ", pme);
             formStatus.addErrorMessages(fieldName, pme.getMessages());
         } catch (Exception e) {
             log.debug("Error setting field value:", e);
             if (!incremental) {
                 formStatus.getInputValues().put(fieldName, null);
                 formStatus.getWrongFields().add(fieldName);
             }
         }
     }
 
     protected void propagateChangesToParentFormStatuses(FormStatus formStatus, String fieldName, Object value) {
         FormStatus parent = FormStatusManager.lookup().getParent(formStatus);
         if (parent != null) {
             String fieldNameInParent = NamespaceManager.lookup().getNamespace(formStatus.getNamespace()).getFieldNameInParent();
             Object valueInParent = parent.getInputValues().get(fieldNameInParent);
             if (valueInParent != null) {
                 Map parentMapObjectRepresentation = null;
                 if (valueInParent instanceof Map) {
                     parentMapObjectRepresentation = (Map) valueInParent;
                 } else if (valueInParent instanceof Map[]) {
                     //Take the correct value
                     Map editFieldPositions = (Map) parent.getAttributes().get(FormStatusData.EDIT_FIELD_POSITIONS);
                     if (editFieldPositions != null) {
                         Integer pos = (Integer) editFieldPositions.get(fieldNameInParent);
                         if (pos != null) {
                             parentMapObjectRepresentation = ((Map[]) valueInParent)[pos.intValue()];
                         }
                     }
                 }
                 if (parentMapObjectRepresentation != null) {
                     //Copy my value to parent
                     parentMapObjectRepresentation.put(fieldName, value);
                     propagateChangesToParentFormStatuses(parent, fieldNameInParent, valueInParent);
                 }
             }
         }
     }
 
     public FormStatusData read(String ctxUid) {
         FormStatusDataImpl data = null;
 
         try {
             FormRenderContext context = formRenderContextManager.getFormRenderContext(ctxUid);
             if (context == null ) return null;
 
             FormStatus formStatus = getContextFormStatus(context);
 
             boolean isNew = formStatus == null;
 
             if (isNew) {
                 formStatus = createContextFormStatus(context);
             }
 
             data = new FormStatusDataImpl(formStatus, isNew);
         } catch (Exception e) {
             log.error("Error: ", e);
         }
 
         return data;
     }
 
     protected FormStatus createContextFormStatus(FormRenderContext context) throws Exception {
         Map values = new HashMap();
 
         Map<String, Object> bindingData = context.getBindingData();
 
         if (bindingData != null && !bindingData.isEmpty()) {
 
             Form form = context.getForm();
             Set<Field> fields = form.getFormFields();
 
             if (fields != null) {
                 for (Field field : form.getFormFields()) {
                     String bindingString = field.getBindingStr();
                     if (!StringUtils.isEmpty(bindingString)) {
                         bindingString = bindingString.substring(1, bindingString.length() - 1);
 
                         boolean canSetValue = bindingString.indexOf("/") > 0;
 
                         if (canSetValue) {
                             String holderId = bindingString.substring(0, bindingString.indexOf("/"));
                             String holderFieldId = bindingString.substring(holderId.length() + 1);
 
                             DataHolder holder = form.getDataHolderById(holderId);
                             if (holder != null && !StringUtils.isEmpty(holderFieldId)) {
 
                                 Object value = bindingData.get(holder.getId());
 
                                 if (value == null) continue;
 
                                 values.put(field.getFieldName(), holder.readValue(value, holderFieldId));
                             }
                         } else {
                             Object value = bindingData.get(bindingString);
                             if (value != null) values.put(bindingString, value);
                         }
                     }
                 }
             }
         }
 
         return getFormStatus(context.getForm().getId(), context.getUID(), values);
     }
 
     public FormStatusData read(Form form, String namespace, Map currentValues) {
         boolean exists = existsFormStatus(form.getId(), namespace);
         if (currentValues == null) currentValues = new HashMap();
         FormStatus formStatus = getFormStatus(form.getId(), namespace, currentValues);
         FormStatusDataImpl data = null;
         try {
             data = new FormStatusDataImpl(formStatus, !exists);
         } catch (Exception e) {
             log.error("Error: ", e);
         }
         return data;
     }
 
     public FormStatusData read(Form form, String namespace) {
         return read(form, namespace, new HashMap<String, Object>());
     }
 
     public void flushPendingCalculations(Form form, String namespace) {
         if (formChangeProcessor != null) {
             formChangeProcessor.process(form, namespace, new FormChangeResponse());//Response is ignored, we just need the session values.
         }
     }
 
     public void persist(String ctxUid) throws Exception {
         ctxUid = StringUtils.defaultIfEmpty(ctxUid, FormProcessor.DEFAULT_NAMESPACE);
         persist(formRenderContextManager.getFormRenderContext(ctxUid));
 
     }
 
     public void persist(FormRenderContext context) throws Exception {
         Form form = context.getForm();
 
         Map mapToPersist = getFilteredMapRepresentationToPersist(form, context.getUID());
 
         for (Iterator it = mapToPersist.keySet().iterator(); it.hasNext();) {
             String fieldName = (String) it.next();
             Field field = form.getField(fieldName);
             if (field != null) {
                 String bindingString = field.getBindingStr();
                 if (!StringUtils.isEmpty(bindingString)) {
                     bindingString = bindingString.substring(1, bindingString.length() - 1);
 
                     boolean canBind = bindingString.indexOf("/") > 0;
 
                     if (canBind) {
                         String holderId = bindingString.substring(0, bindingString.indexOf("/"));
                         String holderFieldId = bindingString.substring(holderId.length() + 1);
                         DataHolder holder = form.getDataHolderById(holderId);
                         if (holder != null && !StringUtils.isEmpty(holderFieldId)) holder.writeValue(context.getBindingData().get(holderId), holderFieldId, mapToPersist.get(fieldName));
                         else canBind = false;
                     }
 
                     if (!canBind) {
                         log.warn("Unable to bind DataHolder for field '" + fieldName + "' to '" + bindingString + "'. This may be caused because bindingString is incorrect or the form doesn't contains the defined DataHolder.");
                         context.getBindingData().put(bindingString, mapToPersist.get(fieldName));
                     }
 
                 }
             }
         }
     }
 
     public Map getMapRepresentationToPersist(Form form, String namespace) throws Exception {
         namespace = StringUtils.defaultIfEmpty(namespace, DEFAULT_NAMESPACE);
         flushPendingCalculations(form, namespace);
         Map m = new HashMap();
         FormStatus formStatus = getFormStatus(form.getId(), namespace);
         if (!formStatus.getWrongFields().isEmpty()) {
             throw new IllegalArgumentException("Validation error.");
         }
 
         fillObjectValues(m, formStatus.getInputValues(), form);
 
         Set s = (Set) m.get(MODIFIED_FIELD_NAMES);
         if (s == null) {
             m.put(MODIFIED_FIELD_NAMES, s = new TreeSet());
         }
         s.addAll(form.getFieldNames());
         return m;
     }
 
     protected Map getFilteredMapRepresentationToPersist(Form form, String namespace) throws Exception {
         Map inputValues = getMapRepresentationToPersist(form, namespace);
         Map mapToPersist = filterMapRepresentationToPersist(inputValues);
         return mapToPersist;
     }
 
     public Map filterMapRepresentationToPersist(Map inputValues) throws Exception {
         Map filteredMap = new HashMap();
         Set keys = inputValues.keySet();
         for (Iterator iterator = keys.iterator(); iterator.hasNext();) {
             String key = (String) iterator.next();
             filteredMap.put(key, inputValues.get(key));
         }
         return filteredMap;
     }
 
     /**
      * Copy to obj values read from status map values
      *
      * @param obj
      * @param values
      * @throws Exception
      */
     protected void fillObjectValues(final Map obj, Map values, Form form) throws Exception {
         Map valuesToSet = new HashMap();
         for (Iterator it = values.keySet().iterator(); it.hasNext();) {
             String propertyName = (String) it.next();
             Object propertyValue = values.get(propertyName);
             valuesToSet.put(propertyName, propertyValue);
         }
         obj.putAll(valuesToSet);
     }
 
     public void load(Long formId, String namespace, Long objIdentifier, String itemClassName) throws Exception {
         load(formId, namespace, objIdentifier, itemClassName, null);
     }
 
     public void load(Long formId, String namespace, Long objIdentifier, String itemClassName, String formMode) throws Exception {
         namespace = StringUtils.defaultIfEmpty(namespace, DEFAULT_NAMESPACE);
         FormStatus formStatus = createFormStatus(formId, namespace);
         formStatus.setLoadedItemId(objIdentifier);
         formStatus.setLoadedItemClass(itemClassName);
         load(formId, namespace, getLoadedObject(formId, namespace), formMode);
     }
 
     public void load(Long formId, String namespace, Object loadObject) throws Exception {
         load(formId, namespace, loadObject, null);
     }
 
     public void load(Long formId, String namespace, Object loadObject, String formMode) throws Exception {
         namespace = StringUtils.defaultIfEmpty(namespace, DEFAULT_NAMESPACE);
         if (loadObject == null) { //Clear loaded object id preserving fields
             FormStatus formStatus = getFormStatus(formId, namespace);
             formStatus.setLoadedItemId(null);
             formStatus.setLoadedItemClass(null);
             // Simulate a fake form submission with no filled in fields, so that all values are properly set internally.
             setValues(getFormsManager().getFormById(formId), namespace, Collections.EMPTY_MAP, Collections.EMPTY_MAP, true);
         } else
             synchronized (loadObject) {
                 FormStatus formStatus = getFormStatus(formId, namespace, (Map) loadObject);
                 if (loadObject instanceof Map) {
                     Map obj = (Map) loadObject;
                     Iterator it = obj.keySet().iterator();
                     while (it.hasNext()) {
                         String key = (String) it.next();
                         Object value = obj.get(key);
                         if (value != null)
                             formStatus.getInputValues().put(key, value);
                     }
                 }
             }
         //Calculate formulas
         /*
         TODO: evaluate formulas
         if (getFormChangeProcessor() != null)
             getFormChangeProcessor().process(getFormsManager().getFormById(formId), namespace, formMode, new FormChangeResponse());//Response is ignored, we just need the session values.
             */
 
     }
 
     protected FormManagerImpl getFormsManager() {
         return (FormManagerImpl) CDIHelper.getBeanByType(FormManagerImpl.class);
     }
 
     public Object getLoadedObject(Long formId, String namespace) throws Exception {
         FormStatus formStatus = getFormStatus(formId, namespace);
         Object loadedObject = null;
         if (formStatus != null) {
             final Serializable objIdentifier = formStatus.getLoadedItemId();
             final String itemClassName = formStatus.getLoadedItemClass();
                     //TODO load data from object here!
         }
         return loadedObject;
     }
 
     @Override
     public void clear(FormRenderContext context) {
         clear(context.getForm().getId(), context.getUID());
     }
 
     @Override
     public void clear(String ctxUID) {
         clear(formRenderContextManager.getFormRenderContext(ctxUID));
     }
 
     public void clear(Long formId, String namespace) {
         if (log.isDebugEnabled())
             log.debug("Clearing form status for form " + formId + " with namespace '" + namespace + "'");
         destroyFormStatus(formId, namespace);
     }
 
     public void clearField(Long formId, String namespace, String fieldName) {
         FormStatus formStatus = getFormStatus(formId, namespace);
         formStatus.getInputValues().remove(fieldName);
     }
 
     public void clearFieldErrors(Form form, String namespace) {
         FormStatusManager.lookup().cascadeClearWrongFields(form.getId(), namespace);
     }
 
     public void forceWrongField(Form form, String namespace, String fieldName) {
         FormStatusManager.lookup().getFormStatus(form.getId(), namespace).getWrongFields().add(fieldName);
     }
 
     // OLD deprecated methods (before namespaces)
 
     public void clear(Long formId) {
         clear(formId, "");
     }
 
     public Object getLoadedObject(Long formId) throws Exception {
         return getLoadedObject(formId, "");
     }
 
     public void load(Long formId, Object loadObject) throws Exception {
         load(formId, "", loadObject);
     }
 
     public void load(Long formId, Long objIdentifier, String itemClassName) throws Exception {
         load(formId, "", objIdentifier, itemClassName);
     }
 
     public void setValues(Form form, Map parameterMap, Map filesMap) {
         setValues(form, "", parameterMap, filesMap);
     }
 
     public void setValues(Form form, Map parameterMap, Map filesMap, boolean incremental) {
         setValues(form, "", parameterMap, filesMap, incremental);
     }
 
     protected String getPrefix(Form form, String namespace) {
         return namespace + NAMESPACE_SEPARATOR + form.getId() + NAMESPACE_SEPARATOR;
     }
 }
