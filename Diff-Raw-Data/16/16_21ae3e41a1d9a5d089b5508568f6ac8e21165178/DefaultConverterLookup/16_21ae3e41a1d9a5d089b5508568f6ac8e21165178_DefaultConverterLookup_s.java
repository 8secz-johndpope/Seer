 /*
  * Copyright 2004-2006 the original author or authors.
  * 
  * Licensed under the Apache License, Version 2.0 (the "License");
  * you may not use this file except in compliance with the License.
  * You may obtain a copy of the License at
  * 
  *      http://www.apache.org/licenses/LICENSE-2.0
  * 
  * Unless required by applicable law or agreed to in writing, software
  * distributed under the License is distributed on an "AS IS" BASIS,
  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  * See the License for the specific language governing permissions and
  * limitations under the License.
  */
 
 package org.compass.core.converter;
 
 import java.io.File;
 import java.io.InputStream;
 import java.io.Reader;
 import java.math.BigDecimal;
 import java.math.BigInteger;
 import java.net.URL;
 import java.util.Calendar;
 import java.util.Date;
 import java.util.HashMap;
 import java.util.Iterator;
 import java.util.Locale;
 import java.util.Map;
 
 import org.apache.commons.logging.Log;
 import org.apache.commons.logging.LogFactory;
 import org.compass.core.CompassException;
 import org.compass.core.config.CompassConfigurable;
 import org.compass.core.config.CompassEnvironment;
 import org.compass.core.config.CompassSettings;
 import org.compass.core.config.ConfigurationException;
 import org.compass.core.converter.basic.*;
 import org.compass.core.converter.dynamic.GroovyDynamicConverter;
 import org.compass.core.converter.dynamic.JakartaElDynamicConverter;
 import org.compass.core.converter.dynamic.JexlDynamicConverter;
 import org.compass.core.converter.dynamic.OgnlDynamicConverter;
 import org.compass.core.converter.dynamic.VelocityDynamicConverter;
 import org.compass.core.converter.extended.FileConverter;
 import org.compass.core.converter.extended.InputStreamConverter;
 import org.compass.core.converter.extended.LocaleConverter;
 import org.compass.core.converter.extended.ObjectByteArrayConverter;
 import org.compass.core.converter.extended.PrimitiveByteArrayConverter;
 import org.compass.core.converter.extended.ReaderConverter;
 import org.compass.core.converter.extended.SqlDateConverter;
 import org.compass.core.converter.extended.SqlTimeConverter;
 import org.compass.core.converter.extended.SqlTimestampConverter;
 import org.compass.core.converter.mapping.osem.ArrayMappingConverter;
 import org.compass.core.converter.mapping.osem.ClassMappingConverter;
 import org.compass.core.converter.mapping.osem.ClassPropertyMappingConverter;
 import org.compass.core.converter.mapping.osem.CollectionMappingConverter;
 import org.compass.core.converter.mapping.osem.ComponentMappingConverter;
 import org.compass.core.converter.mapping.osem.ConstantMappingConverter;
 import org.compass.core.converter.mapping.osem.ParentMappingConverter;
 import org.compass.core.converter.mapping.osem.ReferenceMappingConverter;
 import org.compass.core.converter.mapping.rsem.RawResourceMappingConverter;
 import org.compass.core.converter.mapping.xsem.XmlContentMappingConverter;
 import org.compass.core.converter.mapping.xsem.XmlIdMappingConverter;
 import org.compass.core.converter.mapping.xsem.XmlObjectMappingConverter;
 import org.compass.core.converter.mapping.xsem.XmlPropertyMappingConverter;
 import org.compass.core.mapping.osem.*;
 import org.compass.core.mapping.rsem.RawResourceMapping;
 import org.compass.core.mapping.xsem.XmlContentMapping;
 import org.compass.core.mapping.xsem.XmlIdMapping;
 import org.compass.core.mapping.xsem.XmlObjectMapping;
 import org.compass.core.mapping.xsem.XmlPropertyMapping;
 import org.compass.core.util.ClassUtils;
 
 /**
  * Acts as a <code>Converter</code> registry based on all the converters
  * supplied in the module.
  *
  * @author kimchy
  */
 public class DefaultConverterLookup implements ConverterLookup {
 
     private static final Log log = LogFactory.getLog(DefaultConverterLookup.class);
 
     // not synchronized since the assumption is that no changes are made after
     // theh constructor
     private final HashMap convertersByClass = new HashMap();
 
     private final HashMap cachedConvertersByClassType = new HashMap();
 
     private final HashMap convertersByName = new HashMap();
 
     private final HashMap defaultConveterTypes = new HashMap();
 
     public DefaultConverterLookup() {
         defaultConveterTypes.put(CompassEnvironment.Converter.DefaultTypes.Simple.BIGDECIMAL, BigDecimalConverter.class);
         defaultConveterTypes.put(CompassEnvironment.Converter.DefaultTypes.Simple.BIGINTEGER, BigIntegerConverter.class);
         defaultConveterTypes.put(CompassEnvironment.Converter.DefaultTypes.Simple.BOOLEAN, BooleanConverter.class);
         defaultConveterTypes.put(CompassEnvironment.Converter.DefaultTypes.Simple.BYTE, ByteConverter.class);
         defaultConveterTypes.put(CompassEnvironment.Converter.DefaultTypes.Simple.CALENDAR, CalendarConverter.class);
         defaultConveterTypes.put(CompassEnvironment.Converter.DefaultTypes.Simple.CHAR, CharConverter.class);
         defaultConveterTypes.put(CompassEnvironment.Converter.DefaultTypes.Simple.DATE, DateConverter.class);
         defaultConveterTypes.put(CompassEnvironment.Converter.DefaultTypes.Simple.DOUBLE, DoubleConverter.class);
         defaultConveterTypes.put(CompassEnvironment.Converter.DefaultTypes.Simple.FLOAT, FloatConverter.class);
         defaultConveterTypes.put(CompassEnvironment.Converter.DefaultTypes.Simple.INTEGER, IntConverter.class);
         defaultConveterTypes.put(CompassEnvironment.Converter.DefaultTypes.Simple.LONG, LongConverter.class);
         defaultConveterTypes.put(CompassEnvironment.Converter.DefaultTypes.Simple.SHORT, ShortConverter.class);
         defaultConveterTypes.put(CompassEnvironment.Converter.DefaultTypes.Simple.STRING, StringConverter.class);
         defaultConveterTypes.put(CompassEnvironment.Converter.DefaultTypes.Simple.STRINGBUFFER, StringBufferConverter.class);
         defaultConveterTypes.put(CompassEnvironment.Converter.DefaultTypes.Simple.URL, URLConverter.class);
         defaultConveterTypes.put(CompassEnvironment.Converter.DefaultTypes.Extendend.FILE, FileConverter.class);
         defaultConveterTypes.put(CompassEnvironment.Converter.DefaultTypes.Extendend.INPUT_STREAM, InputStreamConverter.class);
         defaultConveterTypes.put(CompassEnvironment.Converter.DefaultTypes.Extendend.LOCALE, LocaleConverter.class);
         defaultConveterTypes.put(CompassEnvironment.Converter.DefaultTypes.Extendend.PRIMITIVE_BYTE_ARRAY, PrimitiveByteArrayConverter.class);
         defaultConveterTypes.put(CompassEnvironment.Converter.DefaultTypes.Extendend.OBJECT_BYTE_ARRAY, ObjectByteArrayConverter.class);
         defaultConveterTypes.put(CompassEnvironment.Converter.DefaultTypes.Extendend.READER, ReaderConverter.class);
         defaultConveterTypes.put(CompassEnvironment.Converter.DefaultTypes.Extendend.SQL_DATE, SqlDateConverter.class);
         defaultConveterTypes.put(CompassEnvironment.Converter.DefaultTypes.Extendend.SQL_TIME, SqlTimeConverter.class);
         defaultConveterTypes.put(CompassEnvironment.Converter.DefaultTypes.Extendend.SQL_TIMESTAMP, SqlTimestampConverter.class);
     }
 
     public void configure(CompassSettings settings) throws CompassException {
         Map converterGroups = settings.getSettingGroups(CompassEnvironment.Converter.PREFIX);
         // add basic types
         addDefaultConverter(converterGroups, CompassEnvironment.Converter.DefaultTypeNames.Simple.BIGDECIMAL,
                 BigDecimal.class, new BigDecimalConverter());
         addDefaultConverter(converterGroups, CompassEnvironment.Converter.DefaultTypeNames.Simple.BIGINTEGER,
                 BigInteger.class, new BigIntegerConverter());
         addDefaultConverter(converterGroups, CompassEnvironment.Converter.DefaultTypeNames.Simple.BOOLEAN,
                 new Class[]{Boolean.class, boolean.class}, new BooleanConverter());
         addDefaultConverter(converterGroups, CompassEnvironment.Converter.DefaultTypeNames.Simple.BYTE,
                 new Class[]{Byte.class, byte.class}, new ByteConverter());
         addDefaultConverter(converterGroups, CompassEnvironment.Converter.DefaultTypeNames.Simple.CHAR,
                 new Class[]{Character.class, char.class}, new CharConverter());
         addDefaultConverter(converterGroups, CompassEnvironment.Converter.DefaultTypeNames.Simple.DATE,
                 Date.class, new DateConverter());
         addDefaultConverter(converterGroups, CompassEnvironment.Converter.DefaultTypeNames.Simple.CALENDAR,
                 Calendar.class, new CalendarConverter());
         addDefaultConverter(converterGroups, CompassEnvironment.Converter.DefaultTypeNames.Simple.DOUBLE,
                 new Class[]{Double.class, double.class}, new DoubleConverter());
         addDefaultConverter(converterGroups, CompassEnvironment.Converter.DefaultTypeNames.Simple.FLOAT,
                 new Class[]{Float.class, float.class}, new FloatConverter());
         addDefaultConverter(converterGroups, CompassEnvironment.Converter.DefaultTypeNames.Simple.INTEGER,
                 new Class[]{Integer.class, int.class}, new IntConverter());
         addDefaultConverter(converterGroups, CompassEnvironment.Converter.DefaultTypeNames.Simple.LONG,
                 new Class[]{Long.class, long.class}, new LongConverter());
         addDefaultConverter(converterGroups, CompassEnvironment.Converter.DefaultTypeNames.Simple.SHORT,
                 new Class[]{Short.class, short.class}, new ShortConverter());
         addDefaultConverter(converterGroups, CompassEnvironment.Converter.DefaultTypeNames.Simple.STRING,
                 String.class, new StringConverter());
         addDefaultConverter(converterGroups, CompassEnvironment.Converter.DefaultTypeNames.Simple.STRINGBUFFER,
                 StringBuffer.class, new StringBufferConverter());
         addDefaultConverter(converterGroups, CompassEnvironment.Converter.DefaultTypeNames.Simple.URL,
                 URL.class, new URLConverter());
         // add extended types
         addDefaultConverter(converterGroups, CompassEnvironment.Converter.DefaultTypeNames.Extendend.FILE,
                 File.class, new FileConverter());
         addDefaultConverter(converterGroups, CompassEnvironment.Converter.DefaultTypeNames.Extendend.INPUT_STREAM,
                 InputStream.class, new InputStreamConverter());
         addDefaultConverter(converterGroups, CompassEnvironment.Converter.DefaultTypeNames.Extendend.LOCALE,
                 Locale.class, new LocaleConverter());
         addDefaultConverter(converterGroups, CompassEnvironment.Converter.DefaultTypeNames.Extendend.PRIMITIVE_BYTE_ARRAY,
                 byte[].class, new PrimitiveByteArrayConverter());
         addDefaultConverter(converterGroups, CompassEnvironment.Converter.DefaultTypeNames.Extendend.OBJECT_BYTE_ARRAY,
                 Byte[].class, new ObjectByteArrayConverter());
         addDefaultConverter(converterGroups, CompassEnvironment.Converter.DefaultTypeNames.Extendend.READER,
                 Reader.class, new ReaderConverter());
         addDefaultConverter(converterGroups, CompassEnvironment.Converter.DefaultTypeNames.Extendend.SQL_DATE,
                 java.sql.Date.class, new SqlDateConverter());
         addDefaultConverter(converterGroups, CompassEnvironment.Converter.DefaultTypeNames.Extendend.SQL_TIME,
                 java.sql.Time.class, new SqlTimeConverter());
         addDefaultConverter(converterGroups, CompassEnvironment.Converter.DefaultTypeNames.Extendend.SQL_TIMESTAMP,
                 java.sql.Timestamp.class, new SqlTimestampConverter());
         // dynamic converters
         try {
             addDefaultConverter(converterGroups, CompassEnvironment.Converter.DefaultTypeNames.Dynamic.JEXL,
                     DynamicMetaDataMapping.class, new JexlDynamicConverter());
             log.debug("Dynamic converter - JEXL found in the class path, registering it");
        } catch (Exception e) {
             // do nothing
         }
         try {
             addDefaultConverter(converterGroups, CompassEnvironment.Converter.DefaultTypeNames.Dynamic.VELOCITY,
                     DynamicMetaDataMapping.class, new VelocityDynamicConverter());
             log.debug("Dynamic converter - Velocity found in the class path, registering it");
        } catch (Exception e) {
             // do nothing
         }
         try {
             addDefaultConverter(converterGroups, CompassEnvironment.Converter.DefaultTypeNames.Dynamic.JAKARTA_EL,
                     DynamicMetaDataMapping.class, new JakartaElDynamicConverter());
             log.debug("Dynamic converter - Jakarta EL found in the class path, registering it");
        } catch (Exception e) {
             // do nothing
         }
         try {
             addDefaultConverter(converterGroups, CompassEnvironment.Converter.DefaultTypeNames.Dynamic.OGNL,
                     DynamicMetaDataMapping.class, new OgnlDynamicConverter());
             log.debug("Dynamic converter - OGNL found in the class path, registering it");
        } catch (Exception e) {
             // do nothing
         }
         try {
             addDefaultConverter(converterGroups, CompassEnvironment.Converter.DefaultTypeNames.Dynamic.GROOVY,
                     DynamicMetaDataMapping.class, new GroovyDynamicConverter());
            log.debug("Dynamic converter - GRROVY found in the class path, registering it");
        } catch (Exception e) {
             // do nothing
         }
         // add mapping converters
         addDefaultConverter(converterGroups, CompassEnvironment.Converter.DefaultTypeNames.Mapping.RAW_RESOURCE_MAPPING,
                 RawResourceMapping.class, new RawResourceMappingConverter());
         addDefaultConverter(converterGroups, CompassEnvironment.Converter.DefaultTypeNames.Mapping.CLASS_MAPPING,
                 ClassMapping.class, new ClassMappingConverter());
         addDefaultConverter(converterGroups, CompassEnvironment.Converter.DefaultTypeNames.Mapping.CLASS_PROPERTY_MAPPING,
                 ClassPropertyMapping.class, new ClassPropertyMappingConverter());
         addDefaultConverter(converterGroups, CompassEnvironment.Converter.DefaultTypeNames.Mapping.CLASS_ID_PROPERTY_MAPPING,
                 ClassIdPropertyMapping.class, new ClassPropertyMappingConverter());
         addDefaultConverter(converterGroups, CompassEnvironment.Converter.DefaultTypeNames.Mapping.COMPONENT_MAPPING,
                 ComponentMapping.class, new ComponentMappingConverter());
         addDefaultConverter(converterGroups, CompassEnvironment.Converter.DefaultTypeNames.Mapping.COLLECTION_MAPPING,
                 CollectionMapping.class, new CollectionMappingConverter());
         addDefaultConverter(converterGroups, CompassEnvironment.Converter.DefaultTypeNames.Mapping.ARRAY_MAPPING,
                 ArrayMapping.class, new ArrayMappingConverter());
         addDefaultConverter(converterGroups, CompassEnvironment.Converter.DefaultTypeNames.Mapping.REFERENCE_MAPPING,
                 ReferenceMapping.class, new ReferenceMappingConverter());
         addDefaultConverter(converterGroups, CompassEnvironment.Converter.DefaultTypeNames.Mapping.CONSTANT_MAPPING,
                 ConstantMetaDataMapping.class, new ConstantMappingConverter());
         addDefaultConverter(converterGroups, CompassEnvironment.Converter.DefaultTypeNames.Mapping.PARENT_MAPPING,
                 ParentMapping.class, new ParentMappingConverter());
         addDefaultConverter(converterGroups, CompassEnvironment.Converter.DefaultTypeNames.Mapping.XML_OBJECT_MAPPING,
                 XmlObjectMapping.class, new XmlObjectMappingConverter());
         addDefaultConverter(converterGroups, CompassEnvironment.Converter.DefaultTypeNames.Mapping.XML_PROPERTY_MAPPING,
                 XmlPropertyMapping.class, new XmlPropertyMappingConverter());
         addDefaultConverter(converterGroups, CompassEnvironment.Converter.DefaultTypeNames.Mapping.XML_ID_MAPPING,
                 XmlIdMapping.class, new XmlIdMappingConverter());
         addDefaultConverter(converterGroups, CompassEnvironment.Converter.DefaultTypeNames.Mapping.XML_CONTENT_MAPPING,
                 XmlContentMapping.class, new XmlContentMappingConverter());
 
         // now configure all the none default converters
         for (Iterator it = converterGroups.keySet().iterator(); it.hasNext();) {
             String converterName = (String) it.next();
             CompassSettings converterSettings = (CompassSettings) converterGroups.get(converterName);
             if (log.isDebugEnabled()) {
                 log.debug("Conveter [" + converterName + "] building...");
             }
             String converterClassType = converterSettings.getSetting(CompassEnvironment.Converter.TYPE);
             if (converterClassType == null) {
                 throw new ConfigurationException("Must define a class type for converter [" + converterName + "]");
             }
             Converter converter;
             try {
                 Class converterClass = (Class) defaultConveterTypes.get(converterClassType);
                 if (converterClass == null) {
                     converterClass = ClassUtils.forName(converterClassType);
                 }
                 if (log.isDebugEnabled()) {
                     log.debug("Converter [" + converterName + "] is of type [" + converterClass.getName() + "]");
                 }
                 converter = (Converter) converterClass.newInstance();
             } catch (Exception e) {
                 throw new ConfigurationException("Failed to create converter type [" + converterClassType +
                         " for converter [" + converterName + "]", e);
             }
             if (converter instanceof CompassConfigurable) {
                 if (log.isDebugEnabled()) {
                     log.debug("Conveter [" + converterName + "] implements CompassConfigurable, configuring...");
                 }
                 ((CompassConfigurable) converter).configure(converterSettings);
             }
             convertersByName.put(converterName, converter);
             String registerClass = converterSettings.getSetting(CompassEnvironment.Converter.REGISTER_CLASS);
             if (registerClass != null) {
                 try {
                     if (log.isDebugEnabled()) {
                         log.debug("Converter [" + converterName + "] registered under register type [" +
                                 registerClass + "]");
                     }
                     cachedConvertersByClassType.put(ClassUtils.forName(registerClass), converter);
                     convertersByClass.put(registerClass, converter);
                 } catch (Exception e) {
                     throw new ConfigurationException("Failed to create register class [" + registerClass + "] " +
                             " for converter [" + converterName + "]", e);
                 }
             }
         }
     }
 
     private void addDefaultConverter(Map converterGroups, String name, Class type, Converter converter) {
         addDefaultConverter(converterGroups, name, new Class[]{type}, converter);
     }
 
     private void addDefaultConverter(Map converterGroups, String name, Class[] types, Converter converter) {
         CompassSettings converterSettings = (CompassSettings) converterGroups.remove(name);
         if (converterSettings == null) {
             converterSettings = new CompassSettings();
         }
         String converterType = converterSettings.getSetting(CompassEnvironment.Converter.TYPE);
         if (converterType != null) {
             try {
                 if (log.isDebugEnabled()) {
                     log.debug("Converter [" + name + "] (default) configured with a non default type [" +
                             converterType + "]");
                 }
                 converter = (Converter) ClassUtils.forName(converterType).newInstance();
             } catch (Exception e) {
                 throw new ConfigurationException("Failed to create converter type [" + converterType + "] for " +
                         "converter name [" + name + "]");
             }
         }
         if (converter instanceof CompassConfigurable) {
             ((CompassConfigurable) converter).configure(converterSettings);
         }
         convertersByName.put(name, converter);
         for (int i = 0; i < types.length; i++) {
             Class type = types[i];
             convertersByClass.put(type.getName(), converter);
             cachedConvertersByClassType.put(type, converter);
         }
     }
 
     public void registerConverter(String converterName, Converter converter) {
         if (log.isInfoEnabled()) {
             log.info("Converter [" + converterName + "] registered");
         }
         convertersByName.put(converterName, converter);
     }
 
     public void registerConverter(String converterName, Converter converter, Class registerType) {
         if (log.isInfoEnabled()) {
             log.info("Converter [" + converterName + "] registered with type [" + registerType + "]");
         }
         convertersByName.put(converterName, converter);
         convertersByClass.put(registerType.getName(), converter);
         cachedConvertersByClassType.put(registerType, converter);
     }
 
     public Converter lookupConverter(String name) {
         Converter converter = (Converter) convertersByName.get(name);
         if (converter == null) {
             throw new IllegalArgumentException("Failed to find converter by name [" + name + "]");
         }
         return converter;
     }
 
     /**
      * Looks up a converter based on the type. If there is a direct hit, than it
      * is returned, else it checks for a converter based on the interfaces, and
      * than recursive on the super class.
      */
     public Converter lookupConverter(Class type) {
         // not the most thread safe caching, but good enough for us
         // so we don't need to create a thread safe collection.
         Converter c = (Converter) cachedConvertersByClassType.get(type);
         if (c != null) {
             return c;
         }
         synchronized (cachedConvertersByClassType) {
             c = actualConverterLookup(type);
             cachedConvertersByClassType.put(type, c);
             return c;
         }
     }
 
     private Converter actualConverterLookup(Class type) {
         Converter c = (Converter) convertersByClass.get(type.getName());
         if (c != null) {
             return c;
         }
         Class[] interfaces = type.getInterfaces();
         for (int i = 0; i < interfaces.length; i++) {
             c = (Converter) convertersByClass.get(interfaces[i].getName());
             if (c != null) {
                 return c;
             }
         }
         Class superClass = type.getSuperclass();
         if (superClass == null) {
             return null;
         }
         c = lookupConverter(superClass);
         if (c != null) {
             return c;
         }
         return null;
     }
 }
