 package com.reinvent.synergy.data.mapping;
 
 import com.google.gson.Gson;
 import com.google.gson.reflect.TypeToken;
 import org.apache.hadoop.hbase.client.Delete;
 import org.apache.hadoop.hbase.client.Put;
 import org.apache.hadoop.hbase.client.Result;
 import org.apache.hadoop.hbase.util.Bytes;
 import org.apache.log4j.Logger;
 
 import java.lang.reflect.Field;
 import java.lang.reflect.Type;
 import java.util.HashMap;
 import java.util.List;
 import java.util.Map;
 import java.util.NavigableMap;
 
 /**
  * @author Bohdan Mushkevych
  * date: 02/09/11
  * Description: module takes care of serialization and deserialization of Data Model
  * while working with the HBase Map/Reduce
  */
 public class EntityService<T> {
     public static final Type MAP_STR_STR = new TypeToken<Map<String, String>>(){}.getType();
     public static final Type MAP_STR_INT = new TypeToken<Map<String, Integer>>(){}.getType();
     public static final Type MAP_INT_INT = new TypeToken<Map<Integer, Integer>>(){}.getType();
     public static final Type MAP_STR_DBL = new TypeToken<Map<String, Double>>(){}.getType();
 
     private static Logger log = Logger.getLogger(EntityService.class.getName());
     private Gson gson = new Gson();
     private Class<T> clazz;
 
     public EntityService(Class<T> clazz) {
         this.clazz = clazz;
     }
 
     /**
      * Attempts to convert the value object from a byte array to actual field/property type
      * @param raw presentation of the result object
      * @param type of property object
      * @return - converted object or null if input parameter is null
      * @throws IllegalArgumentException if conversion is unsupported
      */
     private Object convertFromBytes(byte[] raw, Type type) {
         if (raw == null) {
             return null;
         }
 
         if (type == Integer.class || type == Integer.TYPE) {
             return Bytes.toInt(raw);
         } else if (type == String.class) {
             return Bytes.toString(raw);
         } else if (type == EntityService.MAP_STR_STR
                 || type == EntityService.MAP_STR_INT
                 || type == EntityService.MAP_STR_DBL
                 || type == EntityService.MAP_INT_INT) {
             String deserialized = Bytes.toString(raw);
             return gson.fromJson(deserialized, type);
         } else if (type == Float.class || type == Float.TYPE) {
             return Bytes.toFloat(raw);
         } else if (type == Double.class || type == Double.TYPE) {
             return Bytes.toDouble(raw);
         } else if (type == Long.class || type == Long.TYPE) {
             return Bytes.toLong(raw);
         } else if (type instanceof Class && ((Class)type).isArray() && ((Class)type).getComponentType() == Byte.TYPE) {
             return raw;
         } else if (type == Map.class) {
             // must be handled by specifying additionally HNestedMap annotation
             throw new IllegalArgumentException("HNestedMap or HMapProperties annotation is missing");
         } else {
             // not handled
             throw new IllegalArgumentException(String.format("Unsupported type %s for conversion", type.toString()));
         }
     }
 
     /**
      * method inspects key and value types and looks for matching Map Type
      * @param keyType Class of the map's key
      * @param valueType Class of the map's value
      * @return Type of MAP_STR_STR or other defined in EntityService
      * @throws IllegalArgumentException if key/value types can not be matched to known Map Type
      */
     protected static Type getMapType(Class keyType, Class valueType) {
         Type type = null;
         if (keyType == String.class && valueType == Integer.class) {
             type = MAP_STR_INT;
         } else if (keyType == String.class && valueType == String.class) {
             type = MAP_STR_STR;
         } else if (keyType == String.class && valueType == Double.class) {
             type = MAP_STR_DBL;
         } else if (keyType == Integer.class && valueType == Integer.class) {
             type = MAP_INT_INT;
         } else {
             throw new IllegalArgumentException(String.format("Unsupported key/value combination %s/%s",
                     keyType.getName(), valueType.getName()));
         }
 
         return type;
     }
 
 
     /**
      * method parses Result and tries to read regular map from it
      * @param row from HBase
      * @param annotation from synergy model
      * @return Map in format <key: value>, where value is a primitive
      */
    private Map parseMap(Result row, HMapFamily annotation) {
         Map result = new HashMap();
         NavigableMap<byte[], byte[]> sourceMap = row.getFamilyMap(annotation.family().getBytes());
         for (Map.Entry<byte[], byte[]> pairs : sourceMap.entrySet()) {
             Object key = convertFromBytes(pairs.getKey(), annotation.keyType());
             Object value = convertFromBytes(pairs.getValue(), annotation.valueType());
             result.put(key, value);
         }
         return result;
     }
 
     /**
      * method parses Result and tries to read complex map from it
      * (i.e. value in the map are maps itself)
      * @param row from HBase
      * @param annotation from synergy model describing outer map
      * @param nestedMap annotation from synergy model describing embedded map
      * @return Map in format <key: <key: value>>, where value is a primitive
      */
     private Map parseComplexMap(Result row, HMapFamily annotation, HNestedMap nestedMap) {
         Type type = EntityService.getMapType(nestedMap.keyType(), nestedMap.valueType());
         Map result = new HashMap();
         NavigableMap<byte[], byte[]> sourceMap = row.getFamilyMap(annotation.family().getBytes());
         for (Map.Entry<byte[], byte[]> pairs : sourceMap.entrySet()) {
             Object key = convertFromBytes(pairs.getKey(), annotation.keyType());
             Object value = convertFromBytes(pairs.getValue(), type);
             result.put(key, value);
         }
         return result;
     }
 
     public T parseResult(Result row) {
         if (row == null) {
             return null;
         }
 
         try {
             T instance = clazz.newInstance();
             Field[] fields = clazz.getDeclaredFields();
             for (Field f : fields) {
                 if (f.isAnnotationPresent(HRowKey.class)) {
                     f.set(instance, row.getRow());
                 } else if (f.isAnnotationPresent(HMapFamily.class)) {
                     HMapFamily annotation = f.getAnnotation(HMapFamily.class);
                     Map parsed = null;
                     if (f.isAnnotationPresent(HNestedMap.class)) {
                         HNestedMap nestedMap = f.getAnnotation(HNestedMap.class);
                         parsed = parseComplexMap(row, annotation, nestedMap);
                     } else {
                        parsed = parseMap(row, annotation);
                     }
                     f.set(instance, parsed);
                 } else if (f.isAnnotationPresent(HProperty.class)) {
                     HProperty annotation = f.getAnnotation(HProperty.class);
                     byte[] raw = row.getValue(annotation.family().getBytes(), annotation.identifier().getBytes());
                     Object value = convertFromBytes(raw, f.getType());
                     if (value != null) {
                         f.set(instance, value);
                     }
                 } else if (f.isAnnotationPresent(HMapProperty.class)) {
                     HMapProperty annotation = f.getAnnotation(HMapProperty.class);
                     byte[] raw = row.getValue(annotation.family().getBytes(), annotation.identifier().getBytes());
                     Type type = EntityService.getMapType(annotation.keyType(), annotation.valueType());
                     Object value = convertFromBytes(raw, type);
                     if (value != null) {
                         f.set(instance, value);
                     }
                 } else {
                     log.debug(String.format("Skipping field %s as it has no supported annotations", f.getName()));
                 }
             }
             return instance;
         } catch (InstantiationException e) {
             log.error("Exception on parsing the HBase Result", e);
             return null;
         } catch (IllegalAccessException e) {
             log.error("Exception on reflection level", e);
             return null;
         }
     }
 
     public<T> Delete delete(byte[] key) {
         return null;
     }
 
     /**
      * Attempts to convert the value object to a byte array for storage
      * @param obj which should be serialized to byte[]
      * @return byte[] presentation of the object or null if input parameter is null
      * @throws IllegalArgumentException if conversion is unsupported
      */
     private byte[] convertToBytes(Object obj) {
         if (obj == null) {
             return null;
         }
 
         if (obj.getClass().isArray() && obj.getClass().getComponentType() == Byte.TYPE) {
             return (byte[]) obj;
         } else if (obj instanceof Integer) {
             int value = (Integer) obj;
             return Bytes.toBytes(value);
         } else if (obj instanceof String) {
             String value = (String) obj;
             return Bytes.toBytes(value);
         } else if (obj instanceof Map) {
             // convert map to JSON object, serialize to String and convert to byte[]
             String serialized = gson.toJson(obj);
             return Bytes.toBytes(serialized);
         } else if (obj instanceof Float) {
             float value = (Float) obj;
             return Bytes.toBytes(value);
         } else if (obj instanceof Double) {
             double value = (Double) obj;
             return Bytes.toBytes(value);
         } else if (obj instanceof Long) {
             long value = (Long) obj;
             return Bytes.toBytes(value);
         } else {
             // not handled
             throw new IllegalArgumentException(String.format("Unknown conversion to bytes for property value type %s",
                     obj.getClass().getName()));
         }
     }
 
     private Put insertMapFamily(Put put, Map m, HMapFamily annotation) {
         for (Object o : m.entrySet()) {
             Map.Entry pairs = (Map.Entry) o;
             byte[] key = convertToBytes(pairs.getKey());
             byte[] value = convertToBytes(pairs.getValue());
             put.add(annotation.family().getBytes(), key, value);
         }
 
         return put;
     }
 
     /**
      * Method iterates thru the instance and prepares on its base HBase Put object
      * Note: all fields and properties of the instance will be translated to Put
      * (this also includes fields that have default values and that were not changed by program)
      * @param instance to put into HBase
      * @param <T> any type from com.reinvent.synergy.data.model
      * @return prepared HBase Put object
      */
     public<T> Put insert(T instance) {
         try {
             Field key = clazz.getField("key");
             byte[] keyRow = (byte[]) key.get(instance);
             Put update = new Put(keyRow);
 
             Field[] fields = clazz.getDeclaredFields();
             for (Field f : fields) {
                 if (f.isAnnotationPresent(HRowKey.class)) {
                     continue;
                 } else if (f.isAnnotationPresent(HMapFamily.class)) {
                     Map m = (Map) f.get(instance);
                     HMapFamily annotation = f.getAnnotation(HMapFamily.class);
                     update = insertMapFamily(update, m, annotation);
                 } else if (f.isAnnotationPresent(HProperty.class)) {
                     byte[] value = convertToBytes(f.get(instance));
                     HProperty annotation = f.getAnnotation(HProperty.class);
                     update.add(annotation.family().getBytes(), annotation.identifier().getBytes(), value);
                 } else if (f.isAnnotationPresent(HMapProperty.class)) {
                     byte[] value = convertToBytes(f.get(instance));
                     HMapProperty annotation = f.getAnnotation(HMapProperty.class);
                     update.add(annotation.family().getBytes(), annotation.identifier().getBytes(), value);
                 } else {
                     log.debug(String.format("Skipping field %s as it has no supported annotations", f.getName()));
                 }
             }
             return update;
         } catch (NoSuchFieldException e) {
             log.error("Field not found", e);
             return null;
         } catch (IllegalAccessException e) {
             log.error("Access exception", e);
             return null;
         }
     }
 
     /**
      * Method iterates thru the instance and prepares on its base HBase Put object
      * Note: only fields marked for update will be be translated to Put
      * @param instance to put into HBase
      * @param updateFields map in format <column_family : <field1, field2...>>
      * @param <T> any type from com.reinvent.synergy.data.model
      * @return prepared HBase Put object
      */
     public<T> Put update(T instance, Map<String, List<String>> updateFields) {
         try {
             Field key = clazz.getField("key");
             byte[] keyRow = (byte[]) key.get(instance);
             Put update = new Put(keyRow);
 
             Field[] fields = clazz.getDeclaredFields();
             for (Field f : fields) {
                 if (f.isAnnotationPresent(HRowKey.class)) {
                     continue;
                 } else if (f.isAnnotationPresent(HMapFamily.class)) {
                     HMapFamily annotation = f.getAnnotation(HMapFamily.class);
                     if (updateFields.containsKey(annotation.family())) {
                         Map m = (Map) f.get(instance);
                         update = insertMapFamily(update, m, annotation);
                     }
                 } else if (f.isAnnotationPresent(HProperty.class)) {
                     HProperty annotation = f.getAnnotation(HProperty.class);
                     if (updateFields.containsKey(annotation.family())) {
                         List<String> list = updateFields.get(annotation.family());
                         if (list != null && list.contains(annotation.identifier())) {
                             byte[] value = convertToBytes(f.get(instance));
                             update.add(annotation.family().getBytes(), annotation.identifier().getBytes(), value);
                         }
                     }
                 } else if (f.isAnnotationPresent(HMapProperty.class)) {
                     HMapProperty annotation = f.getAnnotation(HMapProperty.class);
                     if (updateFields.containsKey(annotation.family())) {
                         List<String> list = updateFields.get(annotation.family());
                         if (list != null && list.contains(annotation.identifier())) {
                             byte[] value = convertToBytes(f.get(instance));
                             update.add(annotation.family().getBytes(), annotation.identifier().getBytes(), value);
                         }
                     }
                 }
             }
             return update;
         } catch (NoSuchFieldException e) {
             log.error("Field not found", e);
             return null;
         } catch (IllegalAccessException e) {
             log.error("Access exception", e);
             return null;
         }
     }
 }
