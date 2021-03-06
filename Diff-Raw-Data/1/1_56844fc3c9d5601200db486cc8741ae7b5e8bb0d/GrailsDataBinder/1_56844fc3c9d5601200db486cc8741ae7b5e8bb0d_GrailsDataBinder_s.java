 /* Copyright 2004-2005 the original author or authors.
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
 package org.codehaus.groovy.grails.web.binding;
 
 import groovy.lang.*;
 import org.apache.commons.logging.Log;
 import org.apache.commons.logging.LogFactory;
 import org.codehaus.groovy.grails.commons.ApplicationHolder;
 import org.codehaus.groovy.grails.commons.DomainClassArtefactHandler;
 import org.codehaus.groovy.grails.commons.GrailsApplication;
 import org.codehaus.groovy.grails.commons.metaclass.CreateDynamicMethod;
 import org.codehaus.groovy.grails.validation.ConstrainedProperty;
 import org.codehaus.groovy.grails.web.context.ServletContextHolder;
 import org.codehaus.groovy.grails.web.servlet.mvc.GrailsParameterMap;
 import org.codehaus.groovy.runtime.InvokerHelper;
 import org.springframework.beans.*;
 import org.springframework.beans.PropertyValue;
 import org.springframework.beans.propertyeditors.CustomDateEditor;
 import org.springframework.beans.propertyeditors.CustomNumberEditor;
 import org.springframework.beans.propertyeditors.LocaleEditor;
 import org.springframework.validation.BeanPropertyBindingResult;
 import org.springframework.web.bind.ServletRequestDataBinder;
 import org.springframework.web.bind.ServletRequestParameterPropertyValues;
 import org.springframework.web.context.WebApplicationContext;
 import org.springframework.web.context.support.WebApplicationContextUtils;
 import org.springframework.web.multipart.MultipartHttpServletRequest;
 import org.springframework.web.multipart.support.ByteArrayMultipartFileEditor;
 import org.springframework.web.multipart.support.StringMultipartFileEditor;
 import org.springframework.web.servlet.support.RequestContextUtils;
 
 import javax.servlet.ServletRequest;
 import javax.servlet.ServletContext;
 import javax.servlet.http.HttpServletRequest;
 import java.beans.PropertyEditor;
 import java.math.BigDecimal;
 import java.math.BigInteger;
 import java.net.URI;
 import java.text.DateFormat;
 import java.text.NumberFormat;
 import java.util.*;
 
 /**
  * A data binder that handles binding dates that are specified with a "struct"-like syntax in request parameters.
  * For example for a set of fields defined as:
  *
  * <code>
      * <input type="hidden" name="myDate_year" value="2005" />
      * <input type="hidden" name="myDate_month" value="6" />
      * <input type="hidden" name="myDate_day" value="12" />
      * <input type="hidden" name="myDate_hour" value="13" />
      * <input type="hidden" name="myDate_minute" value="45" />
  * </code>
  *
  * This would set the property "myDate" of type java.util.Date with the specified values.
  *
  * @author Graeme Rocher
  * @since 05-Jan-2006
  */
 public class GrailsDataBinder extends ServletRequestDataBinder {
     private static final Log LOG = LogFactory.getLog(GrailsDataBinder.class);
 
     protected ConfigurablePropertyAccessor bean;
 
     public static final String[] GROOVY_DISALLOWED = new String[] { "metaClass", "properties" };
     public static final String[] DOMAINCLASS_DISALLOWED = new String[] { "id", "version" };
     public static final String[] GROOVY_DOMAINCLASS_DISALLOWED = new String[] { "metaClass", "properties", "id", "version" };
     public static final String   NULL_ASSOCIATION = "null";
     private static final String PREFIX_SEPERATOR = ".";
     private static final String[] ALL_OTHER_FIELDS_ALLOWED_BY_DEFAULT = new String[0];
     private static final String CONSTRAINTS_PROPERTY = "constraints";
     private static final String BLANK = "";
     private static final String STRUCTURED_PROPERTY_SEPERATOR = "_";
     private static final char PATH_SEPARATOR = '.';
 
     /**
      * Create a new GrailsDataBinder instance.
      *
      * @param target     target object to bind onto
      * @param objectName objectName of the target object
      */
     public GrailsDataBinder(Object target, String objectName) {
         super(target, objectName);
 
         bean = ((BeanPropertyBindingResult)super.getBindingResult()).getPropertyAccessor();
         
         String[] disallowed = new String[0];
         GrailsApplication grailsApplication = ApplicationHolder.getApplication();
         if (grailsApplication!=null && grailsApplication.isArtefactOfType(DomainClassArtefactHandler.TYPE, target.getClass())) {
             if (target instanceof GroovyObject) {
                 disallowed = GROOVY_DOMAINCLASS_DISALLOWED;
             } else {
                 disallowed = DOMAINCLASS_DISALLOWED;
             }
         } else if (target instanceof GroovyObject) {
             disallowed = GROOVY_DISALLOWED;
         }
         setDisallowedFields(disallowed);
         setAllowedFields(ALL_OTHER_FIELDS_ALLOWED_BY_DEFAULT);
         registerCustomEditors();
     }
 
     /**
      * Collects all PropertyEditorRegistrars in the application context and
      * calls them to register their custom editors
      */
     private void registerCustomEditors() {
         final ServletContext servletContext = ServletContextHolder.getServletContext();
         if(servletContext != null) {
             WebApplicationContext context = WebApplicationContextUtils.getWebApplicationContext(servletContext);
             if(context != null) {
                 Map editors = context.getBeansOfType(PropertyEditorRegistrar.class);
                 for (Iterator it = editors.entrySet().iterator(); it.hasNext(); ) {
                     PropertyEditorRegistrar editorRegistrar = (PropertyEditorRegistrar) ((Map.Entry) it.next()).getValue();
                     editorRegistrar.registerCustomEditors(getPropertyEditorRegistry());
                 }
             }
         }
     }
 
     /**
      * Utility method for creating a GrailsDataBinder instance
      *
      * @param target The target object to bind to
      * @param objectName The name of the object
      * @param request A request instance
      * @return A GrailsDataBinder instance
      */
     public static GrailsDataBinder createBinder(Object target, String objectName, HttpServletRequest request) {
         GrailsDataBinder binder = createBinder(target,objectName);
         Locale locale = RequestContextUtils.getLocale(request);
 
         // Formatters for the different number types.
         NumberFormat floatFormat = NumberFormat.getInstance(locale);
         NumberFormat integerFormat = NumberFormat.getIntegerInstance(locale);
 
         binder.registerCustomEditor( Date.class, new CustomDateEditor(DateFormat.getDateInstance( DateFormat.SHORT, locale),true) );
 		binder.registerCustomEditor( BigDecimal.class, new CustomNumberEditor(BigDecimal.class, floatFormat, true));
         binder.registerCustomEditor( BigInteger.class, new CustomNumberEditor(BigInteger.class, floatFormat, true));
         binder.registerCustomEditor( Double.class, new CustomNumberEditor(Double.class, floatFormat, true));
         binder.registerCustomEditor( Float.class, new CustomNumberEditor(Float.class, floatFormat, true));
         binder.registerCustomEditor( Long.class, new CustomNumberEditor(Long.class, integerFormat, true));
         binder.registerCustomEditor( Integer.class, new CustomNumberEditor(Integer.class, integerFormat, true));
         binder.registerCustomEditor( Short.class, new CustomNumberEditor(Short.class, integerFormat, true));
         binder.registerCustomEditor( Date.class, new StructuredDateEditor(DateFormat.getDateInstance( DateFormat.SHORT, locale),true));
         binder.registerCustomEditor( Calendar.class, new StructuredDateEditor(DateFormat.getDateInstance( DateFormat.SHORT, locale),true));
 
         return binder;
     }
 
     /**
      * Utility method for creating a GrailsDataBinder instance
      *
      * @param target The target object to bind to
      * @param objectName The name of the object
      * @return A GrailsDataBinder instance
      */
     public static GrailsDataBinder createBinder(Object target, String objectName) {
         GrailsDataBinder binder = new GrailsDataBinder(target,objectName);
         binder.registerCustomEditor( byte[].class, new ByteArrayMultipartFileEditor());
         binder.registerCustomEditor( String.class, new StringMultipartFileEditor());
         binder.registerCustomEditor( Currency.class, new CurrencyEditor());
         binder.registerCustomEditor( Locale.class, new LocaleEditor());
         binder.registerCustomEditor( TimeZone.class, new TimeZoneEditor());
         binder.registerCustomEditor( URI.class, new UriEditor());
 
 		return binder;
     }
 
     public void bind(PropertyValues propertyValues) {
         bind(propertyValues, null);
     }
 
     /**
      * Binds from a GrailsParameterMap object
      *
      * @param params The GrailsParameterMap object
      */
     public void bind(GrailsParameterMap params) {
         bindWithRequestAndPropertyValues(params.getRequest(), new MutablePropertyValues(params));        
     }
 
     public void bind(PropertyValues propertyValues, String prefix) {
         PropertyValues values = filterPropertyValues(propertyValues, prefix);
         if(propertyValues instanceof MutablePropertyValues) {
             MutablePropertyValues mutablePropertyValues = (MutablePropertyValues) propertyValues;
             preProcessMutablePropertyValues(mutablePropertyValues);
         }
         super.bind(values);
     }
 
     public void bind(ServletRequest request) {
         bind(request, null);
     }
 
     public void bind(ServletRequest request, String prefix) {
     	MutablePropertyValues mpvs;
     	if (prefix != null) {
     		mpvs = new ServletRequestParameterPropertyValues(request, prefix, PREFIX_SEPERATOR);
     	} else {
             mpvs = new ServletRequestParameterPropertyValues(request);
     	}
 
         bindWithRequestAndPropertyValues(request, mpvs);
     }
 
     private void bindWithRequestAndPropertyValues(ServletRequest request, MutablePropertyValues mpvs) {
         preProcessMutablePropertyValues(mpvs);
 
         if (request instanceof MultipartHttpServletRequest) {
 			MultipartHttpServletRequest multipartRequest = (MultipartHttpServletRequest) request;
 			bindMultipartFiles(multipartRequest.getFileMap(), mpvs);
 		}
         doBind(mpvs);
     }
 
     private void preProcessMutablePropertyValues(MutablePropertyValues mpvs) {
         checkStructuredProperties(mpvs);
         autoCreateIfPossible(mpvs);
         bindAssociations(mpvs);
     }
 
     protected void doBind(MutablePropertyValues mpvs) {
         filterNestedParameterMaps(mpvs);
        autoCreateIfPossible(mpvs);
         filterBlankValuesWhenTargetIsNullable(mpvs);
         super.doBind(mpvs);
     }
 
     private void filterBlankValuesWhenTargetIsNullable(MutablePropertyValues mpvs) {
         Object target = getTarget();
         MetaClass mc = GroovySystem.getMetaClassRegistry().getMetaClass(target.getClass());
         if(mc.hasProperty(target, CONSTRAINTS_PROPERTY) != null) {
             Map constrainedProperties = (Map)mc.getProperty(target, CONSTRAINTS_PROPERTY);
             PropertyValue[] valueArray = mpvs.getPropertyValues();
             for (int i = 0; i < valueArray.length; i++) {
                 PropertyValue propertyValue = valueArray[i];
                 ConstrainedProperty cp = getConstrainedPropertyForPropertyValue(constrainedProperties, propertyValue);
                 if(shouldNullifyBlankString(propertyValue, cp)) {
                    propertyValue.setConvertedValue(null); 
                 }
             }
         }
     }
 
     private ConstrainedProperty getConstrainedPropertyForPropertyValue(Map constrainedProperties, PropertyValue propertyValue) {
 
         final String propertyName = propertyValue.getName();
         if(propertyName.indexOf(PATH_SEPARATOR) > -1) {
             String[] propertyNames = propertyName.split("\\.");
             Object target = getTarget();
             Object value = getPropertyValueForPath(target, propertyNames);
             if(value != null) {
                 MetaClass mc = GroovySystem.getMetaClassRegistry().getMetaClass(value.getClass());
                 if(mc.hasProperty(value, CONSTRAINTS_PROPERTY) != null) {
                     Map nestedConstrainedProperties = (Map)mc.getProperty(value, CONSTRAINTS_PROPERTY);
                     return (ConstrainedProperty)nestedConstrainedProperties.get(propertyNames[propertyNames.length-1]);
                 }
             }
 
         }
         else {
             return (ConstrainedProperty)constrainedProperties.get(propertyName);
         }
         return null;
     }
 
     private Object getPropertyValueForPath(Object target, String[] propertyNames) {
         BeanWrapper bean = new BeanWrapperImpl(target);
         Object obj = target;
         for (int i = 0; i < propertyNames.length-1; i++) {
 
             String propertyName = propertyNames[i];
             if(bean.isReadableProperty(propertyName)) {
                 obj = bean.getPropertyValue(propertyName);
                 if(obj == null) break;
                 bean = new BeanWrapperImpl(obj);
             }
         }
 
         return obj;
     }
 
     private boolean shouldNullifyBlankString(PropertyValue propertyValue, ConstrainedProperty cp) {
         return cp!= null && cp.isNullable() && BLANK.equals(propertyValue.getValue());
     }
 
     private void filterNestedParameterMaps(MutablePropertyValues mpvs) {
         PropertyValue[] values = mpvs.getPropertyValues();
         for (int i = 0; i < values.length; i++) {
             PropertyValue pv = values[i];
             if(pv.getValue() instanceof GrailsParameterMap) {
                 mpvs.removePropertyValue(pv);
             }
         }
     }
 
     private PropertyValues filterPropertyValues(PropertyValues propertyValues, String prefix) {
         if(prefix == null || prefix.length() == 0) return propertyValues;
         PropertyValue[] valueArray = propertyValues.getPropertyValues();
         MutablePropertyValues newValues = new MutablePropertyValues();
         for (int i = 0; i < valueArray.length; i++) {
             PropertyValue propertyValue = valueArray[i];
             if(propertyValue.getName().startsWith(prefix + PREFIX_SEPERATOR)){
                 newValues.addPropertyValue(propertyValue.getName().replaceFirst(prefix + PREFIX_SEPERATOR, ""), propertyValue.getValue());
             }
         }
         return newValues;
     }
 
     /**
      * Method that auto-creates the a type if it is null and is possible to auto-create
      *
      * @param mpvs A MutablePropertyValues instance
      */
     protected void autoCreateIfPossible(MutablePropertyValues mpvs) {
         PropertyValue[] pvs = mpvs.getPropertyValues();
         for (int i = 0; i < pvs.length; i++) {
             PropertyValue pv = pvs[i];
 
             String propertyName = pv.getName();
             //super.
             
             if(propertyName.indexOf(PATH_SEPARATOR) > -1) {
                 String[] propertyNames = propertyName.split("\\.");
                 ConfigurablePropertyAccessor currentBean = bean;
 
                 for (int j = 0; j < propertyNames.length; j++) {
                     String name = propertyNames[j];
                     Object created = autoCreatePropertyIfPossible(currentBean, name);
                     if(created!=null)
                         currentBean = new BeanWrapperImpl(created);
                     else
                         break;
                 }
 
             }
             else {
 
                 autoCreatePropertyIfPossible(bean,propertyName);
             }
         }
     }
 
     private Object autoCreatePropertyIfPossible(ConfigurablePropertyAccessor bean,String propertyName) {
         Class type = bean.getPropertyType(propertyName);
         Object val = bean.isReadableProperty(propertyName) ? bean.getPropertyValue(propertyName) : null; 
         LOG.debug("Checking if auto-create is possible for property ["+propertyName+"] and type ["+type+"]");
         if(type != null && val == null) {
             if(GroovyObject.class.isAssignableFrom(type)) {
                 if(bean.getPropertyValue(propertyName) == null) {
                     if(bean.isWritableProperty(propertyName)) {
                         try {
                             MetaClass mc = GroovySystem.getMetaClassRegistry()
                                     .getMetaClass(type);
                             if(mc!=null) {
                                 Object created = mc.invokeStaticMethod(type, CreateDynamicMethod.METHOD_NAME, new Object[0]);
 
                                 bean.setPropertyValue(propertyName,created);
                                 val = created;
                             }
                         }
                         catch(MissingMethodException mme) {
                             LOG.warn("Unable to auto-create type, 'create' method not found");
                         }
                         catch(GroovyRuntimeException gre) {
                             LOG.warn("Unable to auto-create type, Groovy Runtime error: " + gre.getMessage(),gre) ;
                         }
                     }
                 }
             }
         }
         return val;
     }
 
     /**
      * Interrogates the specified properties looking for properites that represent associations to other 
      * classes (e.g., 'author.id').  If such a property is found, this method attempts to load the specified 
      * instance of the association (by ID) and set it on the target object.  
      * 
      * @param mpvs the <code>MutablePropertyValues</code> object holding the parameters from the request
      */
     protected void bindAssociations(MutablePropertyValues mpvs) {
         PropertyValue[] pvs = mpvs.getPropertyValues();
         for (int i = 0; i < pvs.length; i++) {
             PropertyValue pv = pvs[i];
 
             String propertyName = pv.getName();
             int index = propertyName.indexOf(".id");
             if (index > -1) {
                 propertyName = propertyName.substring(0, index);
                 if (bean.isReadableProperty(propertyName) && bean.isWritableProperty(propertyName)) {
                     if( NULL_ASSOCIATION.equals(pv.getValue())) {
                         bean.setPropertyValue(propertyName, null);
                         mpvs.removePropertyValue(pv);
                     } else {
 	                    Class type = bean.getPropertyType(propertyName);
 	
 	                    // In order to load the association instance using InvokerHelper below, we need to 
 	                    // temporarily change this thread's ClassLoader to use the Grails ClassLoader.
 	                    // (Otherwise, we'll get a ClassNotFoundException.)
 	                    ClassLoader currentClassLoader = getClass().getClassLoader();
 	                    ClassLoader grailsClassLoader = getTarget().getClass().getClassLoader();
 	                    try {
 	                        Thread.currentThread().setContextClassLoader(grailsClassLoader);
 							Object persisted;
 	                        	
 	                       	persisted = InvokerHelper.invokeStaticMethod(type, "get", pv.getValue());
 	                        
 	                        if (persisted != null) {
 	                            bean.setPropertyValue(propertyName, persisted);
 	                        }
 	                    } finally {
 	                        Thread.currentThread().setContextClassLoader(currentClassLoader);
 	                    }
                     }
                 }
             }
         }
     }    
     
     private String getNameOf(PropertyValue propertyValue) {
         String name = propertyValue.getName();
         if (name.indexOf(STRUCTURED_PROPERTY_SEPERATOR) == -1) {
             return name;
         }
         return name.substring(0, name.indexOf(STRUCTURED_PROPERTY_SEPERATOR));
     }
 
     private boolean isStructured(PropertyValue propertyValue) {
         String name = propertyValue.getName();
         return name.indexOf(STRUCTURED_PROPERTY_SEPERATOR) != -1;
     }
 
     /**
      * Checks for structured properties. Structured properties are properties with a name
      * containg a "_"
      * @param propertyValues
      */
     private void checkStructuredProperties(MutablePropertyValues propertyValues) {
         PropertyValue[] pvs = propertyValues.getPropertyValues();
         for (int i = 0; i < pvs.length; i++) {
             PropertyValue propertyValue = pvs[i];
             if (!isStructured(propertyValue)) {
                 continue;
             }
             String propertyName = getNameOf(propertyValue);
             Class type = bean.getPropertyType(propertyName);
             if (type != null) {
                 PropertyEditor editor = findCustomEditor(type, propertyName);
                 if (null != editor && StructuredPropertyEditor.class.isAssignableFrom(editor.getClass())) {
                     StructuredPropertyEditor structuredEditor = (StructuredPropertyEditor) editor;
                     List fields = new ArrayList();
                     fields.addAll(structuredEditor.getRequiredFields());
                     fields.addAll(structuredEditor.getOptionalFields());
                     Map fieldValues = new HashMap();
                     try {
                         for (int j = 0; j < fields.size(); j++) {
                             String field = (String) fields.get(j);
                             PropertyValue requiredProperty = propertyValues.getPropertyValue(propertyName + STRUCTURED_PROPERTY_SEPERATOR + field);
                             if (requiredProperty == null && structuredEditor.getRequiredFields().contains(field)) {
                                 break;
                             }
                             else if(requiredProperty == null)
                                 continue;
                             fieldValues.put(field, getStringValue(requiredProperty));
                         }
                         try {
                             Object value = structuredEditor.assemble(type, fieldValues);
                             if (null != value) {
                                 for (int j = 0; j < fields.size(); j++) {
                                     String requiredField = (String) fields.get(j);
                                     PropertyValue requiredProperty = propertyValues.getPropertyValue(propertyName + STRUCTURED_PROPERTY_SEPERATOR + requiredField);
                                     if (null != requiredProperty) {
                                         requiredProperty.setConvertedValue(getStringValue(requiredProperty));
                                     }
                                 }
                                 propertyValues.addPropertyValue(new PropertyValue(propertyName, value));
                             }
                         }
                         catch (IllegalArgumentException iae) {
                             LOG.warn("Unable to parse structured date from request for date ["+propertyName+"]",iae);
                         }
                     }
                     catch(InvalidPropertyException ipe) {
                         // ignore
                     }
                 }
             }
         }
     }
 
     private String getStringValue(PropertyValue yearProperty) {
         Object value = yearProperty.getValue();
         if(value == null) return null;
         else if(value.getClass().isArray()) {
             return ((String[])value)[0];
         }
         return (String)value ;
     }
 }
