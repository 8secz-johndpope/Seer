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
 package org.slim3.jsp;
 
 import java.util.ArrayList;
 import java.util.Arrays;
 import java.util.List;
 import java.util.Locale;
 import java.util.Map;
 import java.util.TimeZone;
 
 import javax.servlet.http.HttpServletRequest;
 
 import org.slim3.controller.ControllerConstants;
 import org.slim3.util.ClassUtil;
 import org.slim3.util.Converter;
 import org.slim3.util.HtmlUtil;
 import org.slim3.util.LocaleLocator;
 import org.slim3.util.RequestLocator;
 import org.slim3.util.ResponseLocator;
 import org.slim3.util.StringUtil;
 import org.slim3.util.TimeZoneLocator;
 
 /**
  * JSP functions of Slim3.
  * 
  * @author higa
  * @since 3.0
  * 
  */
 public final class Functions {
 
     private static String BR = "<br />";
 
     private static String ARRAY_SUFFIX = "Array";
 
     private static List<String> EMPTY_STRING_LIST = new ArrayList<String>(0);
 
     private static Converter textConverter;
 
     static {
         try {
            textConverter =
                (Converter) ClassUtil.newInstance(
                    "org.slim3.util.TextConverter",
                    Thread.currentThread().getContextClassLoader());
         } catch (Throwable ignore) {
         }
     }
 
     /**
      * Escapes string that could be interpreted as HTML.
      * 
      * @param input
      *            the input value
      * @return the escaped value
      */
     public static String h(Object input) {
         if (input == null || "".equals(input)) {
             return "";
         }
         if (input.getClass() == String.class) {
             return HtmlUtil.escape(input.toString());
         }
         if (input.getClass().isArray()) {
             Class<?> clazz = input.getClass().getComponentType();
             if (clazz.isPrimitive()) {
                 if (clazz == boolean.class) {
                     return Arrays.toString((boolean[]) input);
                 } else if (clazz == int.class) {
                     return Arrays.toString((int[]) input);
                 } else if (clazz == long.class) {
                     return Arrays.toString((long[]) input);
                 } else if (clazz == byte.class) {
                     return Arrays.toString((byte[]) input);
                 } else if (clazz == short.class) {
                     return Arrays.toString((short[]) input);
                 } else if (clazz == float.class) {
                     return Arrays.toString((float[]) input);
                 } else if (clazz == double.class) {
                     return Arrays.toString((double[]) input);
                 } else if (clazz == char.class) {
                     return Arrays.toString((char[]) input);
                 }
             } else {
                 return Arrays.toString((Object[]) input);
             }
         }
         if (textConverter != null && textConverter.isTarget(input.getClass())) {
             return HtmlUtil.escape(textConverter.getAsString(input));
         }
         return input.toString();
     }
 
     /**
      * Returns context-relative URL.
      * 
      * @param input
      *            the input value
      * @return context-relative URL
      */
     public static String url(String input) {
         boolean empty = StringUtil.isEmpty(input);
         if (!empty && input.indexOf(':') >= 0) {
             return input;
         }
         HttpServletRequest request = RequestLocator.get();
         String contextPath = request.getContextPath();
         StringBuilder sb = new StringBuilder(50);
         if (contextPath.length() > 1) {
             sb.append(contextPath);
         }
         String path = request.getServletPath();
         int pos = path.lastIndexOf('/');
         path = path.substring(0, pos + 1);
         if (empty) {
             sb.append(path);
         } else if (input.startsWith("/")) {
             sb.append(input);
         } else {
             sb.append(path).append(input);
         }
         return ResponseLocator.get().encodeURL(sb.toString());
     }
 
     /**
      * Converts line break to br tag.
      * 
      * @param input
      *            the input value
      * @return the converted value
      */
     public static String br(String input) {
         if (StringUtil.isEmpty(input)) {
             return "";
         }
         return input.replaceAll("\r\n", BR).replaceAll("\r", BR).replaceAll(
             "\n",
             BR);
     }
 
     /**
      * Returns the current locale.
      * 
      * @return the current locale.
      */
     public static Locale locale() {
         return LocaleLocator.get();
     }
 
     /**
      * Returns the current time zone.
      * 
      * @return the current time zone.
      */
     public static TimeZone timeZone() {
         return TimeZoneLocator.get();
     }
 
     /**
      * Returns the error style class
      * 
      * @param name
      *            the name
      * @param styleClass
      *            the error style class
      * @return the error style class
      */
     @SuppressWarnings("unchecked")
     public static String errorClass(String name, String styleClass) {
         HttpServletRequest request = RequestLocator.get();
         Map<String, String> errors =
             (Map<String, String>) request
                 .getAttribute(ControllerConstants.ERRORS_KEY);
         if (errors != null && errors.containsKey(name)) {
             return styleClass;
         }
         return "";
     }
 
     /**
      * Returns the text tag representation.
      * 
      * @param name
      *            the property name
      * @return the text tag representation
      * @throws IllegalArgumentException
      *             if the property name ends with "Array"
      */
     public static String text(String name) throws IllegalArgumentException {
         if (name.endsWith(ARRAY_SUFFIX)) {
             throw new IllegalArgumentException("The text property name("
                 + name
                 + ") must not end with \"Array\".");
         }
         HttpServletRequest request = RequestLocator.get();
         return "name = \""
             + name
             + "\" value = \""
             + h(request.getAttribute(name))
             + "\"";
     }
 
     /**
      * Returns the hidden tag representation.
      * 
      * @param name
      *            the property name
      * @return the hidden tag representation
      * @throws IllegalArgumentException
      *             if the property name ends with "Array"
      */
     public static String hidden(String name) throws IllegalArgumentException {
         if (name.endsWith(ARRAY_SUFFIX)) {
             throw new IllegalArgumentException("The hidden property name("
                 + name
                 + ") must not end with \"Array\".");
         }
         HttpServletRequest request = RequestLocator.get();
         return "name = \""
             + name
             + "\" value = \""
             + h(request.getAttribute(name))
             + "\"";
     }
 
     /**
      * Returns the checkbox tag representation.
      * 
      * @param name
      *            the property name
      * @return the checkbox tag representation
      * @throws IllegalArgumentException
      *             if the property name ends with "Array"
      */
     public static String checkbox(String name) throws IllegalArgumentException {
         if (name.endsWith(ARRAY_SUFFIX)) {
             throw new IllegalArgumentException("The checkbox property name("
                 + name
                 + ") must not end with \"Array\".");
         }
         HttpServletRequest request = RequestLocator.get();
         return "name = \""
             + name
             + "\""
             + (request.getAttribute(name) != null
                 ? " checked = \"checked\""
                 : "");
     }
 
     /**
      * Returns the multibox tag representation.
      * 
      * @param name
      *            the property name
      * @param value
      *            the value
      * @return the multibox tag representation
      * @throws IllegalArgumentException
      *             if the property name does not end with "Array"
      * @throws IllegalStateException
      *             if the property is not an array or if the property is not a
      *             string array
      */
     public static String multibox(String name, String value)
             throws IllegalArgumentException, IllegalStateException {
         if (!name.endsWith(ARRAY_SUFFIX)) {
             throw new IllegalArgumentException("The multibox property name("
                 + name
                 + ") must end with \"Array\".");
         }
         HttpServletRequest request = RequestLocator.get();
         Object o = request.getAttribute(name);
         List<String> list = null;
         if (o != null) {
             if (!o.getClass().isArray()) {
                 throw new IllegalStateException("The multibox property("
                     + name
                     + ") must be an array.");
             }
             if (o.getClass().getComponentType() != String.class) {
                 throw new IllegalStateException("The multibox property("
                     + name
                     + ") must be a string array.");
             }
             list = Arrays.asList((String[]) o);
         } else {
             list = EMPTY_STRING_LIST;
         }
         return "name = \""
             + name
             + "\" value = \""
             + h(value)
             + "\""
             + (list.contains(value) ? " checked = \"checked\"" : "");
     }
 
     /**
      * Returns the radio tag representation.
      * 
      * @param name
      *            the property name
      * @param value
      *            the value
      * @return the radio tag representation
      * @throws IllegalArgumentException
      *             if the property name ends with "Array"
      */
     public static String radio(String name, String value)
             throws IllegalArgumentException {
         if (name.endsWith(ARRAY_SUFFIX)) {
             throw new IllegalArgumentException("The radio property name("
                 + name
                 + ") must not end with \"Array\".");
         }
         HttpServletRequest request = RequestLocator.get();
         String s = StringUtil.toString(request.getAttribute(name));
         return "name = \""
             + name
             + "\" value = \""
             + h(value)
             + "\""
             + (value == null && s == null || value != null && value.equals(s)
                 ? " checked = \"checked\""
                 : "");
     }
 
     /**
      * Returns the select option tag representation.
      * 
      * @param name
      *            the property name
      * @param value
      *            the value
      * @return the select option tag representation
      * @throws IllegalArgumentException
      *             if the property name ends with "Array"
      */
     public static String select(String name, String value)
             throws IllegalArgumentException {
         if (name.endsWith(ARRAY_SUFFIX)) {
             throw new IllegalArgumentException("The select property name("
                 + name
                 + ") must not end with \"Array\".");
         }
         HttpServletRequest request = RequestLocator.get();
         String s = StringUtil.toString(request.getAttribute(name));
         return "value = \""
             + h(value)
             + "\""
             + (value == null && s == null || value != null && value.equals(s)
                 ? " selected = \"selected\""
                 : "");
     }
 
     /**
      * Returns the multiselect option tag representation.
      * 
      * @param name
      *            the property name
      * @param value
      *            the value
      * @return the multiselect option tag representation
      * @throws IllegalArgumentException
      *             if the property name does not end with "Array"
      * @throws IllegalStateException
      *             if the property is not an array or if the property is not a
      *             string array
      */
     public static String multiselect(String name, String value)
             throws IllegalArgumentException, IllegalStateException {
         if (!name.endsWith(ARRAY_SUFFIX)) {
             throw new IllegalArgumentException("The multiselect property name("
                 + name
                 + ") must end with \"Array\".");
         }
         HttpServletRequest request = RequestLocator.get();
         Object o = request.getAttribute(name);
         List<String> list = null;
         if (o != null) {
             if (!o.getClass().isArray()) {
                 throw new IllegalStateException("The multiselect property("
                     + name
                     + ") must be an array.");
             }
             if (o.getClass().getComponentType() != String.class) {
                 throw new IllegalStateException("The multiselect property("
                     + name
                     + ") must be a string array.");
             }
             list = Arrays.asList((String[]) o);
         } else {
             list = EMPTY_STRING_LIST;
         }
         return "value = \""
             + h(value)
             + "\""
             + (list.contains(value) ? " selected = \"selected\"" : "");
     }
 
     private Functions() {
     }
 }
