 package com.psddev.dari.util;
 
 import java.io.InputStream;
 import java.io.IOException;
 import java.lang.reflect.Constructor;
 import java.lang.reflect.InvocationTargetException;
 import java.lang.reflect.Type;
 import java.util.Collections;
 import java.util.Enumeration;
 import java.util.Map;
 import java.util.HashMap;
 import java.util.TreeMap;
 import java.util.Properties;
 
 import javax.naming.Binding;
 import javax.naming.Context;
 import javax.naming.InitialContext;
 import javax.naming.NamingException;
 
 import org.slf4j.Logger;
 import org.slf4j.LoggerFactory;
 
 /** Global map of settings. */
 public class Settings {
 
     /**
      * Sub-key used to identify the implementation class for
      * {@link #newInstance}.
      */
     public static final String CLASS_SUB_SETTING = "class";
 
     /** Key used to toggle debug mode. */
     public static final String DEBUG_SETTING = "DEBUG";
 
     /** Key used to indicate when running in a production environment. */
     public static final String PRODUCTION_SETTING = "PRODUCTION";
 
     /** Default properties file that contains all settings. */
     public static final String SETTINGS_FILE = "/settings.properties";
 
     private static final Logger LOGGER = LoggerFactory.getLogger(Settings.class);
     private static final String JNDI_PREFIX = "java:comp/env";
 
     private static final ThreadLocal<Map<String, Object>> OVERRIDES = new ThreadLocal<Map<String, Object>>();
 
     private static final PullThroughValue<Map<String, Object>> SETTINGS = new PullThroughValue<Map<String, Object>>() {
 
         @Override
         protected Map<String, Object> produce() {
             return new PeriodicCache<String, Object>(0.0, 10.0) {
 
                 @Override
                 protected Map<String, Object> update() {
 
                     // Optional file.
                     Map<String, Object> settings = new TreeMap<String, Object>();
                     InputStream input = Settings.class.getResourceAsStream(SETTINGS_FILE);
                     if (input != null) {
                         try {
                             try {
                                 Properties properties = new Properties();
                                 properties.load(input);
                                 putAllMap(settings, properties);
                             } finally {
                                 input.close();
                             }
                         } catch (IOException ex) {
                             LOGGER.warn(String.format("Cannot read [%s] file!", SETTINGS_FILE), ex);
                         }
                     }
 
                     putAllMap(settings, System.getenv());
                     putAllMap(settings, System.getProperties());
 
                     // JNDI.
                     try {
                         putAllContext(settings, new InitialContext(), JNDI_PREFIX);
                    } catch (Throwable error) {
                     }
 
                     return Collections.unmodifiableMap(settings);
                 }
 
                 private void putAllMap(Map<String, Object> map, Map<?, ?> other) {
                     for (Map.Entry<?, ?> entry : other.entrySet()) {
                         Object key = entry.getKey();
                         if (key != null) {
                             CollectionUtils.putByPath(map, key.toString(), entry.getValue());
                         }
                     }
                 }
 
                 private void putAllContext(Map<String, Object> map, Context context, String path) throws NamingException {
 
                     String pathWithSlash;
                     if (path.endsWith("/")) {
                         pathWithSlash = path;
                         path = path.substring(0, path.length() - 1);
                     } else {
                         pathWithSlash = path + "/";
                     }
 
                     for (Enumeration<Binding> e = context.listBindings(path); e.hasMoreElements(); ) {
                         Binding binding = e.nextElement();
 
                         String name = binding.getName();
                         if (name.startsWith(pathWithSlash)) {
                             name = name.substring(pathWithSlash.length());
                         }
 
                         if (!ObjectUtils.isBlank(name)) {
                             String fullName = pathWithSlash + name;
                             Object value = binding.getObject();
                             if (value instanceof Context) {
                                 putAllContext(map, context, fullName);
                             } else {
                                 CollectionUtils.putByPath(map, fullName.substring(JNDI_PREFIX.length() + 1), value);
                             }
                         }
                     }
                 }
             };
         }
     };
 
     /**
      * Returns the value associated with the given {@code key}, or if not
      * found or is blank, the given {@code defaultValue}.
      *
      * @param key Can be {@code null}.
      * @param defaultValue Can be {@code null}.
      */
     public static Object getOrDefault(String key, Object defaultValue) {
         Object value = null;
         Map<String, Object> overrides = OVERRIDES.get();
 
         if (overrides != null) {
             value = CollectionUtils.getByPath(overrides, key);
         }
 
         if (ObjectUtils.isBlank(value)) {
             value = CollectionUtils.getByPath(SETTINGS.get(), key);
         }
 
         if (ObjectUtils.isBlank(value)) {
             value = defaultValue;
         }
 
         return value;
     }
 
     /**
      * Returns the value associated with the given {@code key} as an instance
      * of the given {@code returnType}, or if not found or is blank, the given
      * {@code defaultValue}.
      *
      * @param returnType Can't be {@code null}.
      * @param key Can be {@code null}.
      * @param defaultValue Can be {@code null}.
      */
     public static Object getOrDefault(Type returnType, String key, Object defaultValue) {
         return ObjectUtils.to(returnType, getOrDefault(key, defaultValue));
     }
 
     /**
      * Returns the value associated with the given {@code key} as an instance
      * of the given {@code returnClass}, or if not found or is blank, the given
      * {@code defaultValue}.
      *
      * @param returnClass Can't be {@code null}.
      * @param key Can be {@code null}.
      * @param defaultValue Can be {@code null}.
      */
     public static <T> T getOrDefault(Class<T> returnClass, String key, T defaultValue) {
         return ObjectUtils.to(returnClass, getOrDefault(key, defaultValue));
     }
 
     /**
      * Returns the value associated with the given {@code key} as an instance
      * of the type referenced by the given {@code returnTypeReference},
      * or if not found or is blank, the given {@code defaultValue}.
      *
      * @param returnTypeReference Can't be {@code null}.
      * @param key Can be {@code null}.
      * @param defaultValue Can be {@code null}.
      */
     public static <T> T getOrDefault(TypeReference<T> returnTypeReference, String key, T defaultValue) {
         return ObjectUtils.to(returnTypeReference, getOrDefault(key, defaultValue));
     }
 
     /**
      * Returns the value associated with the given {@code key}, or if not
      * found or is blank, the given {@code defaultValue}.
      *
      * @param key Can be {@code null}.
      */
     public static Object get(String key) {
         return get(key, null);
     }
 
     /**
      * Returns the value, as an instance of the given {@code returnType},
      * associated with the given {@code key}, or if not found or is blank,
      * the given {@code defaultValue}.
      *
      * @param returnType Can't be {@code null}.
      * @param key Can be {@code null}.
      */
     public static Object get(Type returnType, String key) {
         return get(returnType, key, null);
     }
 
     /**
      * Returns the value, as an instance of the given {@code returnClass},
      * associated with the given {@code key}, or if not found or is blank,
      * the given {@code defaultValue}.
      *
      * @param returnClass Can't be {@code null}.
      * @param key Can be {@code null}.
      */
     public static <T> T get(Class<T> returnClass, String key) {
         return get(returnClass, key, null);
     }
 
     /**
      * Returns the value, as an instance of the type referenced by the given
      * {@code returnTypeReference}, associated with the given {@code key},
      * or if not found or is blank, the given {@code defaultValue}.
      *
      * @param returnTypeReference Can't be {@code null}.
      * @param key Can be {@code null}.
      */
     public static <T> T get(TypeReference<T> returnTypeReference, String key) {
         return get(returnTypeReference, key, null);
     }
 
     private static <T> T checkValue(T value, String key, String message) {
         if (ObjectUtils.isBlank(value)) {
             throw new SettingsException(key, message != null ? message : "[" + key + "] can't be blank!");
         } else {
             return value;
         }
     }
 
     /**
      * Returns the value associated with the given {@code key}, or throws
      * a {@link SettingsException} with the given {@code message} if not
      * found or the value is blank.
      *
      * @param key Can be {@code null}.
      * @param message Can be {@code null}.
      * @throws SettingsException If the value associated with the given
      * {@code key} isn't found or is blank.
      */
     public static Object getOrError(String key, String message) {
         return checkValue(get(key), key, message);
     }
 
     /**
      * Returns the value associated with the given {@code key} as an instance
      * of the given {@code returnType}, or throws a {@link SettingsException}
      * with the given {@code message} if not found or the value is blank.
      *
      * @param returnType Can't be {@code null}.
      * @param key Can be {@code null}.
      * @param message Can be {@code null}.
      * @throws SettingsException If the value associated with the given
      * {@code key} isn't found or is blank.
      */
     public static Object getOrError(Type returnType, String key, String message) {
         return checkValue(get(returnType, key), key, message);
     }
 
     /**
      * Returns the value associated with the given {@code key} as an instance
      * of the given {@code returnClass}, or throws a {@link SettingsException}
      * with the given {@code message} if not found or the value is blank.
      *
      * @param returnClass Can't be {@code null}.
      * @param key Can be {@code null}.
      * @param message Can be {@code null}.
      * @throws SettingsException If the value associated with the given
      * {@code key} isn't found or is blank.
      */
     public static <T> T getOrError(Class<T> returnClass, String key, String message) {
         return checkValue(get(returnClass, key), key, message);
     }
 
     /**
      * Returns the value associated with the given {@code key} as an instance
      * of the type referenced by the given {@code returnTypeReference},
      * or throws a {@link SettingsException} with the given {@code message}
      * if not found or the value is blank.
      *
      * @param returnTypeReference Can't be {@code null}.
      * @param key Can be {@code null}.
      * @param message Can be {@code null}.
      * @throws SettingsException If the value associated with the given
      * {@code key} isn't found or is blank.
      */
     public static <T> T getOrError(TypeReference<T> returnTypeReference, String key, String message) {
         return checkValue(get(returnTypeReference, key), key, message);
     }
 
     /**
      * Temporarily overrides the value associated with the given
      * {@code key} in the current thread.
      */
     public static void setOverride(String key, Object value) {
         Map<String, Object> overrides = OVERRIDES.get();
 
         if (value != null) {
             if (overrides == null) {
                 overrides = new HashMap<String, Object>();
                 OVERRIDES.set(overrides);
             }
             CollectionUtils.putByPath(overrides, key, value);
 
         } else {
             if (overrides != null) {
                 CollectionUtils.putByPath(overrides, key, null);
             }
         }
     }
 
     /** Returns {@code true} if running in debug mode. */
     public static boolean isDebug() {
         return get(boolean.class, DEBUG_SETTING);
     }
 
     /** Returns {@code true} if running in a production environment. */
     public static boolean isProduction() {
         return get(boolean.class, PRODUCTION_SETTING);
     }
 
     /**
      * Returns a view of all settings as a map.
      *
      * @return Never {@code null}. Immutable.
      */
     public static Map<String, Object> asMap() {
         return SETTINGS.get();
     }
 
     /**
      * Creates an instance of the given {@code interfaceClass} based on the
      * values associated with the given {@code key}.
      *
      * @param interfaceClass Can't be {@code null}.
      * @param key Can't be blank.
      * @return Never {@code null}.
      * @throws SettingsException If the values associated with the given
      * {@code key} can't be used to create the instance.
      */
     @SuppressWarnings("unchecked")
     public static <T extends SettingsBackedObject> T newInstance(Class<T> interfaceClass, String key) {
         Object instanceSettings = get(key);
 
         if (!(instanceSettings instanceof Map)) {
             throw new SettingsException(key, String.format(
                     "[%s] settings must be a map!",
                     interfaceClass.getName()));
         }
 
         String classKey = key + "/" + CLASS_SUB_SETTING;
         String instanceClassName = ObjectUtils.to(String.class, get(classKey));
 
         if (ObjectUtils.isBlank(instanceClassName)) {
             throw new SettingsException(classKey, String.format(
                     "Implementation class for [%s] is missing!",
                     interfaceClass.getName()));
         }
 
         Class<?> instanceClass = null;
 
         try {
             instanceClass = Class.forName(instanceClassName);
         } catch (ClassNotFoundException ex) {
             throw new SettingsException(classKey, String.format(
                     "[%s] isn't a valid class!",
                     instanceClassName));
         }
 
         for (Class<?> requiredClass : new Class<?>[] {
                 interfaceClass,
                 SettingsBackedObject.class }) {
             if (!requiredClass.isAssignableFrom(instanceClass)) {
                 throw new SettingsException(classKey, String.format(
                         "[%s] doesn't implement [%s]!",
                         instanceClass.getName(),
                         requiredClass.getName()));
             }
         }
 
         Constructor<?> constructor = null;
 
         try {
             constructor = instanceClass.getDeclaredConstructor();
         } catch (NoSuchMethodException ex) {
             throw new SettingsException(classKey, String.format(
                     "[%s] doesn't have a nullary constructor!"));
         }
 
         T object;
 
         try {
             constructor.setAccessible(true);
             object = (T) constructor.newInstance();
 
         } catch (IllegalAccessException ex) {
             throw new IllegalStateException(ex);
 
         } catch (InstantiationException ex) {
             throw new IllegalStateException(ex);
 
         } catch (InvocationTargetException ex) {
             Throwable cause = ex.getCause();
             throw cause instanceof RuntimeException
                     ? (RuntimeException) cause
                     : new RuntimeException(String.format(
                             "Unexpected error trying to create [%s]!",
                             instanceClassName), cause);
         }
 
         object.initialize(key, (Map<String, Object>) instanceSettings);
 
         return object;
     }
 
     // --- Deprecated ---
 
     /** @deprecated Use {@link #getOrDefault(String, Object)} instead. */
     @Deprecated
     public static Object get(String key, Object defaultValue) {
         return getOrDefault(key, defaultValue);
     }
 
     /** @deprecated Use {@link #getOrDefault(Type, String, Object)} instead. */
     @Deprecated
     public static Object get(Type returnType, String key, Object defaultValue) {
         return getOrDefault(returnType, key, defaultValue);
     }
 
     /** @deprecated Use {@link #getOrDefault(Class, String, Object)} instead. */
     @Deprecated
     public static <T> T get(Class<T> returnClass, String key, T defaultValue) {
         return getOrDefault(returnClass, key, defaultValue);
     }
 
     /** @deprecated Use {@link #getOrDefault(TypeReference, String, Object)} instead. */
     @Deprecated
     public static <T> T get(TypeReference<T> returnTypeReference, String key, T defaultValue) {
         return getOrDefault(returnTypeReference, key, defaultValue);
     }
 }
