 /*
  * Copyright 2004-2009 the original author or authors.
  *
  * Licensed under the Apache License, Version 2.0 (the "License");
  * you may not use this file except in compliance with the License.
  * You may obtain a copy of the License at
  *
  *     http://www.apache.org/licenses/LICENSE-2.0
  *
  * Unless required by applicable law or agreed to in writing, software
  * distributed under the License is distributed on an "AS IS" BASIS,
  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
  * either express or implied. See the License for the specific language
  * governing permissions and limitations under the License.
  */
 package org.slim3.util;
 
 import java.text.DecimalFormat;
 import java.text.SimpleDateFormat;
 import java.util.ArrayList;
 import java.util.Date;
 import java.util.HashMap;
 import java.util.List;
 import java.util.Map;
 
 /**
  * A copy options.
  * 
  * @author higa
  * @since 3.0
  * 
  */
 public class CopyOptions {
 
     /**
      * The empty strings.
      */
     protected static final String[] EMPTY_STRINGS = new String[0];
 
     /**
      * The text converter.
      */
     protected static Converter textConverter;
 
     /**
      * The included property names.
      */
     protected String[] includedPropertyNames = EMPTY_STRINGS;
 
     /**
      * The excluded property names.
      */
     protected String[] excludedPropertyNames = EMPTY_STRINGS;
 
     /**
      * Whether null is copied.
      */
     protected boolean copyNull = false;
 
     /**
      * Whether empty string is copied.
      */
     protected boolean copyEmptyString = false;
 
     /**
      * The converters that are bound to the specific property.
      */
     protected Map<String, Converter> converterMap =
         new HashMap<String, Converter>();
 
     /**
      * The converters that are not bound to any properties.
      */
     protected List<Converter> converters = new ArrayList<Converter>();
 
     static {
         try {
            ClassUtil.forName("com.google.appengine.api.datastore.Text", Thread
                .currentThread()
                .getContextClassLoader());
            textConverter = new TextConverter();
         } catch (Throwable ignore) {
         }
     }
 
     /**
      * Specifies the included property names.
      * 
      * @param propertyNames
      *            the included property names
      * @return this instance
      */
     public CopyOptions include(String... propertyNames) {
         includedPropertyNames = propertyNames;
         return this;
     }
 
     /**
      * Specifies the excluded property names.
      * 
      * @param propertyNames
      *            the excluded property names
      * @return this instance
      */
     public CopyOptions exclude(String... propertyNames) {
         excludedPropertyNames = propertyNames;
         return this;
     }
 
     /**
      * Specifies whether null is copied.
      * 
      * @return this instance
      */
     public CopyOptions copyNull() {
         copyNull = true;
         return this;
     }
 
     /**
      * Specifies whether empty string is copied.
      * 
      * @return this instance
      */
     public CopyOptions copyEmptyString() {
         copyEmptyString = true;
         return this;
     }
 
     /**
      * Specifies the converter.
      * 
      * @param converter
      *            the converter
      * @param propertyNames
      *            the property names
      * @return this instance
      * @throws NullPointerException
      *             if the converter parameter is null
      */
     public CopyOptions converter(Converter converter, String... propertyNames)
             throws NullPointerException {
         if (converter == null) {
             throw new NullPointerException("The converter parameter is null.");
         }
         if (propertyNames.length == 0) {
             converters.add(converter);
         } else {
             for (String name : propertyNames) {
                 converterMap.put(name, converter);
             }
         }
         return this;
     }
 
     /**
      * Specifies the converter for {@link Date}.
      * 
      * @param pattern
      *            the pattern for {@link SimpleDateFormat}
      * @param propertyNames
      *            the property names
      * @return this instance
      */
     public CopyOptions dateConverter(String pattern, String... propertyNames) {
         return converter(new DateConverter(pattern), propertyNames);
     }
 
     /**
      * Specifies the number converter.
      * 
      * @param pattern
      *            the pattern for {@link DecimalFormat}
      * @param propertyNames
      *            the property names
      * @return this instance
      */
     public CopyOptions numberConverter(String pattern, String... propertyNames) {
         return converter(new NumberConverter(pattern), propertyNames);
     }
 
     /**
      * Determines if the property is target.
      * 
      * @param name
      *            the property name
      * @return whether the property is target
      */
     protected boolean isTargetProperty(String name) {
         if (includedPropertyNames.length > 0) {
             for (String s : includedPropertyNames) {
                 if (s.equals(name)) {
                     for (String s2 : excludedPropertyNames) {
                         if (s2.equals(name)) {
                             return false;
                         }
                     }
                     return true;
                 }
             }
             return false;
         }
         if (excludedPropertyNames.length > 0) {
             for (String s : excludedPropertyNames) {
                 if (s.equals(name)) {
                     return false;
                 }
             }
             return true;
         }
         return true;
     }
 
     /**
      * Determines if the value is target.
      * 
      * @param value
      *            the value
      * @return whether the value is target
      */
     protected boolean isTargetValue(Object value) {
         if (value == null) {
             return copyNull;
         }
         if ("".equals(value)) {
             return copyEmptyString;
         }
         return true;
     }
 
     /**
      * Converts the value.
      * 
      * @param value
      *            the value
      * @param propertyName
      *            the property name
      * @param destPropertyClass
      *            the destination property class
      * @return the converted value
      * @throws IllegalArgumentException
      *             if an exception occurred while converting
      * 
      */
     protected Object convertValue(Object value, String propertyName,
             Class<?> destPropertyClass) throws IllegalArgumentException {
         if (value == null) {
             return null;
         }
         try {
             if (value.getClass() == String.class) {
                 return convertString(
                     (String) value,
                     propertyName,
                     destPropertyClass);
             }
             return convertObject(value, propertyName, destPropertyClass);
         } catch (Throwable cause) {
             throw new IllegalArgumentException("The value("
                 + value
                 + ") of the property("
                 + propertyName
                 + ") can not be converted. Error message: "
                 + cause.getMessage(), cause);
         }
     }
 
     /**
      * Converts string value.
      * 
      * @param value
      *            string value
      * @param propertyName
      *            the property name
      * @param destPropertyClass
      *            the destination property class
      * @return converted value
      */
     protected Object convertString(String value, String propertyName,
             Class<?> destPropertyClass) {
         Converter converter = converterMap.get(propertyName);
         if (converter != null) {
             return converter.getAsObject(value);
         }
         if (destPropertyClass == null) {
             return value;
         }
         if (destPropertyClass == String.class) {
             return value;
         }
         converter = findConverter(destPropertyClass);
         if (converter != null) {
             return converter.getAsObject(value);
         }
         return value;
     }
 
     /**
      * Converts object value.
      * 
      * @param value
      *            object value
      * @param propertyName
      *            the property name
      * @param destPropertyClass
      *            the destination property class
      * @return converted value
      */
     protected Object convertObject(Object value, String propertyName,
             Class<?> destPropertyClass) {
         Converter converter = converterMap.get(propertyName);
         if (converter != null) {
             return converter.getAsString(value);
         }
         if (destPropertyClass == null) {
             return value;
         }
         if (destPropertyClass != String.class) {
             return value;
         }
         converter = findConverter(value.getClass());
         if (converter != null) {
             return converter.getAsString(value);
         }
         return value;
     }
 
     /**
      * Finds the converter for the target class.
      * 
      * @param targetClass
      *            the target class.
      * @return converter
      */
     protected Converter findConverter(Class<?> targetClass) {
         for (Class<?> clazz = targetClass; clazz != null
             && clazz != Object.class; clazz = clazz.getSuperclass()) {
             for (Converter c : converters) {
                 if (c.isTarget(clazz)) {
                     return c;
                 }
             }
         }
         if (textConverter != null && textConverter.isTarget(targetClass)) {
             return textConverter;
         }
         return null;
     }
 }
