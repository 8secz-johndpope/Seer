 /*
  * Copyright 2010 Google Inc.
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
 package com.google.gwt.requestfactory.server;
 
 import com.google.gwt.requestfactory.shared.DataTransferObject;
 import com.google.gwt.requestfactory.shared.RequestFactory;
 import com.google.gwt.requestfactory.shared.impl.JsonRequestDataUtil;
 import com.google.gwt.valuestore.shared.Property;
 import com.google.gwt.valuestore.shared.Record;
 import com.google.gwt.valuestore.shared.WriteOperation;
 
 import org.json.JSONArray;
 import org.json.JSONException;
 import org.json.JSONObject;
 
 import java.lang.reflect.Field;
 import java.lang.reflect.InvocationTargetException;
 import java.lang.reflect.Method;
 import java.lang.reflect.Modifier;
 import java.math.BigDecimal;
 import java.math.BigInteger;
 import java.util.Collection;
 import java.util.Collections;
 import java.util.Date;
 import java.util.HashMap;
 import java.util.HashSet;
 import java.util.Iterator;
 import java.util.List;
 import java.util.Map;
 import java.util.Set;
 import java.util.logging.Logger;
 
 import javax.validation.ConstraintViolation;
 import javax.validation.Validation;
 import javax.validation.Validator;
 import javax.validation.ValidatorFactory;
 
 /**
  * An implementation of RequestProcessor for JSON encoded payloads.
  */
 public class JsonRequestProcessor implements RequestProcessor<String> {
   // TODO should we consume String, InputStream, or JSONObject?
   /**
    * A class representing the pair of a domain entity and its corresponding
    * record class on the client side.
    */
   public static class EntityRecordPair {
     public final Class<?> entity;
     public final Class<? extends Record> record;
 
     EntityRecordPair(Class<?> entity, Class<? extends Record> record) {
       this.entity = entity;
       this.record = record;
     }
   }
 
   public static final Set<String> BLACK_LIST = initBlackList();
 
   public static Set<String> initBlackList() {
     Set<String> blackList = new HashSet<String>();
     for (String str : new String[]{"password"}) {
       blackList.add(str);
     }
     return Collections.unmodifiableSet(blackList);
   }
 
   private OperationRegistry operationRegistry;
 
   public Collection<Property<?>> allProperties(Class<? extends Record> clazz) {
     Set<Property<?>> rtn = new HashSet<Property<?>>();
     for (Field f : clazz.getFields()) {
       if (Modifier.isStatic(f.getModifiers())
           && Property.class.isAssignableFrom(f.getType())) {
         try {
           rtn.add((Property<?>) f.get(null));
         } catch (IllegalAccessException e) {
           throw new RuntimeException(e);
         }
       }
     }
     return rtn;
   }
 
   public String decodeAndInvokeRequest(String encodedRequest) throws Exception {
     try {
       return processJsonRequest(encodedRequest).toString();
     } catch (ClassNotFoundException e) {
       throw new IllegalArgumentException(e);
     } catch (IllegalAccessException e) {
       throw new IllegalArgumentException(e);
     } catch (InvocationTargetException e) {
       throw new IllegalArgumentException(e);
     } catch (SecurityException e) {
       throw new IllegalArgumentException(e);
     } catch (JSONException e) {
       throw new IllegalArgumentException(e);
     } catch (NoSuchMethodException e) {
       throw new IllegalArgumentException(e);
     } catch (IllegalArgumentException e) {
       throw new RuntimeException(e);
     } catch (InstantiationException e) {
       throw new RuntimeException(e);
     }
   }
 
   /**
    * Encodes parameter value.
    * 
    */
   public Object decodeParameterValue(Class<?> parameterType,
       String parameterValue) {
     if (String.class == parameterType) {
       return parameterValue;
     }
     if (Integer.class == parameterType || int.class == parameterType) {
       return new Integer(parameterValue);
     }
     if (Byte.class == parameterType || byte.class == parameterType) {
       return new Byte(parameterValue);
     }
     if (Short.class == parameterType || short.class == parameterType) {
       return new Short(parameterValue);
     }
     if (Float.class == parameterType || float.class == parameterType) {
       return new Float(parameterValue);
     }
     if (Double.class == parameterType || double.class == parameterType) {
       return new Double(parameterValue);
     }
     if (Long.class == parameterType || long.class == parameterType) {
       return new Long(parameterValue);
     }
     if (Character.class == parameterType || char.class == parameterType) {
       return parameterValue.charAt(0);
     }
     if (BigInteger.class == parameterType) {
       return new BigInteger(parameterValue);
     }
     if (BigDecimal.class == parameterType) {
       return new BigDecimal(parameterValue);
     }
     if (parameterType.isEnum()) {
       try {
         int ordinal = Integer.parseInt(parameterValue);
         Method valuesMethod = parameterType.getDeclaredMethod("values",
             new Class[0]);
         Logger.getLogger(this.getClass().getName()).severe(
             "Type is " + parameterType + " valuesMethod " + valuesMethod);
 
         valuesMethod.setAccessible(true);
         if (valuesMethod != null) {
           Enum[] values = (Enum[]) valuesMethod.invoke(null);
           // we use ordinal serialization instead of name since future compiler
           // opts may remove names
           for (Enum e : values) {
             if (ordinal == e.ordinal()) {
               return e;
             }
           }
         }
         throw new IllegalArgumentException("Can't decode enum " + parameterType
             + " no matching ordinal " + ordinal);
       } catch (NoSuchMethodException e) {
         throw new IllegalArgumentException("Can't decode enum " + parameterType);
       } catch (InvocationTargetException e) {
         throw new IllegalArgumentException("Can't decode enum " + parameterType);
       } catch (IllegalAccessException e) {
         throw new IllegalArgumentException("Can't decode enum " + parameterType);
       }
     }
     if (Date.class == parameterType) {
       return new Date(Long.parseLong(parameterValue));
     }
     throw new IllegalArgumentException("Unknown parameter type: "
         + parameterType);
   }
 
   public Object encodePropertyValue(Object value) {
     if (value == null) {
       return value;
     }
     Class<?> type = value.getClass();
     if (Date.class == type) {
       return String.valueOf(((Date) value).getTime());
     }
     if (Enum.class.isAssignableFrom(type)) {
       return Double.valueOf(((Enum) value).ordinal());
     }
     if (BigDecimal.class == type || BigInteger.class == type
         || Long.class == type) {
       return String.valueOf(value);
     }
     if (Number.class.isAssignableFrom(type)) {
       return ((Number) value).doubleValue();
     }
     return String.valueOf(value);
   }
 
   /**
    * Returns the propertyValue in the right type, from the DataStore. The value
    * is sent into the response.
    */
   public Object encodePropertyValueFromDataStore(Object entityElement,
       String propertyName) throws SecurityException, NoSuchMethodException,
       IllegalAccessException, InvocationTargetException {
     String methodName = getMethodNameFromPropertyName(propertyName, "get");
     Method method = entityElement.getClass().getMethod(methodName);
     Object returnValue = method.invoke(entityElement);
     return encodePropertyValue(returnValue);
   }
 
   /**
    * Generate an ID for a new record. The default behavior is to return null and
    * let the data store generate the ID automatically.
    * 
    * @param key the key of the record field
    * @return the ID of the new record, or null to auto generate
    */
   public Long generateIdForCreate(String key) {
     // ignored. id is assigned by default.
     return null;
   }
 
   public Class<Object> getEntityFromRecordAnnotation(
       Class<? extends Record> record) {
     DataTransferObject dtoAnn = record.getAnnotation(DataTransferObject.class);
     if (dtoAnn != null) {
       return (Class<Object>) dtoAnn.value();
     }
     throw new IllegalArgumentException("Record class " + record.getName()
         + " missing DataTransferObject annotation");
   }
 
   public Object getEntityInstance(WriteOperation writeOperation,
       Class<?> entity, Object idValue, Class<?> idType)
       throws SecurityException, InstantiationException, IllegalAccessException,
       InvocationTargetException, NoSuchMethodException {
 
     if (writeOperation == WriteOperation.CREATE) {
       return entity.getConstructor().newInstance();
     }
     // TODO: check "version" validity.
     return entity.getMethod("find" + entity.getSimpleName(), idType).invoke(
         null, decodeParameterValue(idType, idValue.toString()));
   }
 
   /**
    * Converts the returnValue of a 'get' method to a JSONArray.
    * 
    * @param resultList object returned by a 'get' method, must be of type
    *          List<?>
    * @param entityKeyClass the class type of the contained value
    * @return the JSONArray
    */
   public JSONArray getJsonArray(List<?> resultList,
       Class<? extends Record> entityKeyClass) throws IllegalArgumentException,
       SecurityException, IllegalAccessException, JSONException,
       NoSuchMethodException, InvocationTargetException {
     JSONArray jsonArray = new JSONArray();
     if (resultList.size() == 0) {
       return jsonArray;
     }
 
     for (Object entityElement : resultList) {
       jsonArray.put(getJsonObject(entityElement, entityKeyClass));
     }
     return jsonArray;
   }
 
   public JSONObject getJsonObject(Object entityElement,
       Class<? extends Record> entityKeyClass) throws JSONException,
       NoSuchMethodException, IllegalAccessException, InvocationTargetException {
     JSONObject jsonObject = new JSONObject();
     for (Property<?> p : allProperties(entityKeyClass)) {
 
       if (requestedProperty(p)) {
         String propertyName = p.getName();
         jsonObject.put(propertyName, encodePropertyValueFromDataStore(
             entityElement, propertyName));
       }
     }
     return jsonObject;
   }
 
   /**
    * Returns methodName corresponding to the propertyName that can be invoked on
    * an entity.
    * 
    * Example: "userName" returns prefix + "UserName". "version" returns prefix +
    * "Version"
    */
   public String getMethodNameFromPropertyName(String propertyName, String prefix) {
     if (propertyName == null) {
       throw new NullPointerException("propertyName must not be null");
     }
 
     StringBuffer methodName = new StringBuffer(prefix);
     methodName.append(propertyName.substring(0, 1).toUpperCase());
     methodName.append(propertyName.substring(1));
     return methodName.toString();
   }
 
   public Object[] getObjectsFromParameterMap(Map<String, String> parameterMap,
       Class<?> parameterClasses[]) {
     assert parameterClasses != null;
     Object args[] = new Object[parameterClasses.length];
     for (int i = 0; i < parameterClasses.length; i++) {
       args[i] = decodeParameterValue(parameterClasses[i],
           parameterMap.get("param" + i));
     }
     return args;
   }
 
   public RequestDefinition getOperation(String operationName) {
     RequestDefinition operation;
     operation = operationRegistry.getOperation(operationName);
     if (null == operation) {
       throw new IllegalArgumentException("Unknown operation " + operationName);
     }
     return operation;
   }
 
   /**
    * @param jsonObject
    * @return
    * @throws org.json.JSONException
    */
   public Map<String, String> getParameterMap(JSONObject jsonObject)
       throws JSONException {
     Map<String, String> parameterMap = new HashMap<String, String>();
     Iterator<?> keys = jsonObject.keys();
     while (keys.hasNext()) {
       String key = keys.next().toString();
       if (key.startsWith(JsonRequestDataUtil.PARAM_TOKEN)) {
         parameterMap.put(key, jsonObject.getString(key));
       }
     }
     return parameterMap;
   }
 
   /**
    * Returns the property fields (name => type) for a record.
    */
   public Map<String, Class<?>> getPropertiesFromRecord(
       Class<? extends Record> record) throws SecurityException,
       IllegalAccessException, InvocationTargetException, NoSuchMethodException {
     Map<String, Class<?>> properties = new HashMap<String, Class<?>>();
     for (Field f : record.getFields()) {
       if (Property.class.isAssignableFrom(f.getType())) {
         Class<?> propertyType = (Class<?>) f.getType().getMethod("getType").invoke(
             f.get(null));
         properties.put(f.getName(), propertyType);
       }
     }
     return properties;
   }
 
   /**
    * Returns the property value, in the specified type, from the request object.
    * The value is put in the DataStore.
    */
   public Object getPropertyValueFromRequest(JSONObject recordObject,
       String key, Class<?> propertyType) throws JSONException {
     return decodeParameterValue(propertyType, recordObject.get(key).toString());
   }
 
   public Class<Record> getRecordFromClassToken(String recordToken) {
     try {
       Class<?> clazz = Class.forName(recordToken, false,
           getClass().getClassLoader());
       if (Record.class.isAssignableFrom(clazz)) {
         return (Class<Record>) clazz;
       }
       throw new SecurityException("Attempt to access non-record class "
           + recordToken);
     } catch (ClassNotFoundException e) {
       throw new IllegalArgumentException("Non-existent record class "
           + recordToken);
     }
   }
 
   public JSONObject getReturnRecord(WriteOperation writeOperation,
       Object entityInstance, JSONObject recordObject,
       Set<ConstraintViolation<Object>> violations) throws SecurityException,
       JSONException, IllegalAccessException, InvocationTargetException,
       NoSuchMethodException {
     // id/futureId, the identifying field is sent back from the incoming record.
     JSONObject returnObject = new JSONObject();
     final boolean hasViolations = violations != null && !violations.isEmpty();
     if (hasViolations) {
       returnObject.put("violations", getViolationsAsJson(violations));
     }
     switch (writeOperation) {
       case CREATE:
         returnObject.put("futureId", recordObject.getString("id"));
         if (!hasViolations) {
           returnObject.put("id", encodePropertyValueFromDataStore(
               entityInstance, "id"));
           returnObject.put("version", encodePropertyValueFromDataStore(
               entityInstance, "version"));
         }
         break;
       case DELETE:
         returnObject.put("id", recordObject.getString("id"));
         break;
       case UPDATE:
         returnObject.put("id", recordObject.getString("id"));
         if (!hasViolations) {
           returnObject.put("version", encodePropertyValueFromDataStore(
               entityInstance, "version"));
         }
         break;
     }
     return returnObject;
   }
 
   public JSONObject getReturnRecordForException(WriteOperation writeOperation,
       JSONObject recordObject, Exception ex) {
     JSONObject returnObject = new JSONObject();
     try {
       if (writeOperation == WriteOperation.DELETE
           || writeOperation == WriteOperation.UPDATE) {
         returnObject.put("id", recordObject.getString("id"));
       } else {
         returnObject.put("futureId", recordObject.getString("id"));
       }
       // expecting violations to be a JSON object.
       JSONObject violations = new JSONObject();
       if (ex instanceof NumberFormatException) {
         violations.put("Expected a number instead of String", ex.getMessage());
       } else {
         violations.put("", "unexpected server error");
       }
       returnObject.put("violations", violations);
     } catch (JSONException e) {
       // ignore.
       e.printStackTrace();
     }
     return returnObject;
   }
 
   public JSONObject getViolationsAsJson(
       Set<ConstraintViolation<Object>> violations) throws JSONException {
     JSONObject violationsAsJson = new JSONObject();
     for (ConstraintViolation<Object> violation : violations) {
       violationsAsJson.put(violation.getPropertyPath().toString(),
           violation.getMessage());
     }
     return violationsAsJson;
   }
 
   public Object invokeStaticDomainMethod(Method domainMethod, Object args[])
       throws IllegalAccessException, InvocationTargetException {
     return domainMethod.invoke(null, args);
   }
 
   public Object processJsonRequest(String jsonRequestString)
       throws JSONException, NoSuchMethodException, IllegalAccessException,
       InvocationTargetException, InstantiationException, ClassNotFoundException {
     RequestDefinition operation;
     JSONObject topLevelJsonObject = new JSONObject(jsonRequestString);
 
     String operationName = topLevelJsonObject.getString(JsonRequestDataUtil.OPERATION_TOKEN);
     if (operationName.equals(RequestFactory.SYNC)) {
       return sync(topLevelJsonObject.getString(JsonRequestDataUtil.CONTENT_TOKEN));
     } else {
       operation = getOperation(operationName);
       Class<?> domainClass = Class.forName(operation.getDomainClassName());
       Method domainMethod = domainClass.getMethod(
           operation.getDomainMethodName(), operation.getParameterTypes());
       if (!Modifier.isStatic(domainMethod.getModifiers())) {
         throw new IllegalArgumentException("the " + domainMethod.getName()
             + " is not static");
       }
       Object args[] = getObjectsFromParameterMap(
           getParameterMap(topLevelJsonObject), domainMethod.getParameterTypes());
       Object result = invokeStaticDomainMethod(domainMethod, args);
       if ((result instanceof List<?>) != operation.isReturnTypeList()) {
         throw new IllegalArgumentException(String.format(
             "Type mismatch, expected %s%s, but %s returns %s",
             operation.isReturnTypeList() ? "list of " : "",
             operation.getReturnType(), domainMethod,
             domainMethod.getReturnType()));
       }
 
       if (result instanceof List<?>) {
         JSONArray jsonArray = getJsonArray((List<?>) result,
             (Class<? extends Record>) operation.getReturnType());
         return jsonArray;
       } else if (result instanceof Number
           && !(result instanceof BigDecimal || result instanceof BigInteger)) {
         return result;
       } else {
 
         JSONObject jsonObject = getJsonObject(result,
             (Class<? extends Record>) operation.getReturnType());
         return jsonObject;
       }
     }
   }
 
   /**
    * returns true if the property has been requested. TODO: use the properties
    * that should be coming with the request.
    * 
    * @param p the field of entity ref
    * @return has the property value been requested
    */
   public boolean requestedProperty(Property<?> p) {
     return !BLACK_LIST.contains(p.getName());
   }
 
   public void setOperationRegistry(OperationRegistry operationRegistry) {
     this.operationRegistry = operationRegistry;
   }
 
   public JSONObject sync(String content) throws SecurityException,
       NoSuchMethodException, IllegalAccessException, InvocationTargetException,
       InstantiationException {
 
     try {
       JSONObject jsonObject = new JSONObject(content);
       JSONObject returnJsonObject = new JSONObject();
       for (WriteOperation writeOperation : WriteOperation.values()) {
         if (!jsonObject.has(writeOperation.name())) {
           continue;
         }
         JSONArray reportArray = new JSONArray(
             jsonObject.getString(writeOperation.name()));
         JSONArray returnArray = new JSONArray();
 
         int length = reportArray.length();
         if (length == 0) {
           throw new IllegalArgumentException("No json array for "
               + writeOperation.name() + " should have been sent");
         }
         for (int i = 0; i < length; i++) {
           JSONObject recordWithSchema = reportArray.getJSONObject(i);
           Iterator<?> iterator = recordWithSchema.keys();
           String recordToken = (String) iterator.next();
           if (iterator.hasNext()) {
             throw new IllegalArgumentException(
                 "There cannot be more than one record token");
           }
           JSONObject recordObject = recordWithSchema.getJSONObject(recordToken);
           JSONObject returnObject = updateRecordInDataStore(recordToken,
               recordObject, writeOperation);
           returnArray.put(returnObject);
         }
         returnJsonObject.put(writeOperation.name(), returnArray);
       }
       return returnJsonObject;
     } catch (JSONException e) {
       throw new IllegalArgumentException("sync failed: ", e);
     }
   }
 
   /**
    * Update propertiesInRecord based on the types of entity.
    */
   public void updatePropertyTypes(Map<String, Class<?>> propertiesInRecord,
       Class<?> entity) {
     for (Field field : entity.getDeclaredFields()) {
       Class<?> fieldType = propertiesInRecord.get(field.getName());
       if (fieldType != null) {
         propertiesInRecord.put(field.getName(), field.getType());
       }
     }
   }
 
   /**
    * Persist a recordObject of token "recordToken" and return useful information
    * as a JSONObject to return back.
    * <p>
    * Example: recordToken = "Employee", entity = Employee.class, record =
    * EmployeeRecord.class
    *<p>
    * Steps:
    * <ol>
    * <li>assert that each property is present in "EmployeeRecord"
    * <li>invoke "findEmployee (id)" OR new Employee()
    * <li>set various fields on the attached entity and persist OR remove()
    * <li>return data
    * </ol>
    */
   public JSONObject updateRecordInDataStore(String recordToken,
       JSONObject recordObject, WriteOperation writeOperation) {
 
     try {
       Class<? extends Record> record = getRecordFromClassToken(recordToken);
       Class<?> entity = getEntityFromRecordAnnotation(record);
 
       Map<String, Class<?>> propertiesInRecord = getPropertiesFromRecord(record);
       validateKeys(recordObject, propertiesInRecord.keySet());
       updatePropertyTypes(propertiesInRecord, entity);
 
       // get entityInstance
       Object entityInstance = getEntityInstance(writeOperation, entity,
           recordObject.get("id"), propertiesInRecord.get("id"));
 
       // persist
       Set<ConstraintViolation<Object>> violations = null;
       if (writeOperation == WriteOperation.DELETE) {
         entity.getMethod("remove").invoke(entityInstance);
       } else {
         Iterator<?> keys = recordObject.keys();
         while (keys.hasNext()) {
           String key = (String) keys.next();
           Class<?> propertyType = propertiesInRecord.get(key);
           if (writeOperation == WriteOperation.CREATE && ("id".equals(key))) {
             Long id = generateIdForCreate(key);
             if (id != null) {
               entity.getMethod(getMethodNameFromPropertyName(key, "set"),
                   propertyType).invoke(entityInstance, id);
             }
           } else {
             Object propertyValue = getPropertyValueFromRequest(recordObject,
                 key, propertyType);
             entity.getMethod(getMethodNameFromPropertyName(key, "set"),
                 propertyType).invoke(entityInstance, propertyValue);
           }
         }
 
         // validations check..
         ValidatorFactory validatorFactory = Validation.buildDefaultValidatorFactory();
         Validator validator = validatorFactory.getValidator();
 
         violations = validator.validate(entityInstance);
         if (violations.isEmpty()) {
           entity.getMethod("persist").invoke(entityInstance);
         }
       }
 
       // return data back.
       return getReturnRecord(writeOperation, entityInstance, recordObject,
           violations);
     } catch (Exception ex) {
       return getReturnRecordForException(writeOperation, recordObject, ex);
     }
   }
 
   public void validateKeys(JSONObject recordObject,
       Set<String> declaredProperties) {
     Iterator<?> keys = recordObject.keys();
     while (keys.hasNext()) {
       String key = (String) keys.next();
       if (!declaredProperties.contains(key)) {
         throw new IllegalArgumentException("key " + key
             + " is not permitted to be set");
       }
     }
   }
 
   @SuppressWarnings("unchecked")
   private void ensureConfig() {
     operationRegistry = new ReflectionBasedOperationRegistry(
         new DefaultSecurityProvider());
   }
 }
