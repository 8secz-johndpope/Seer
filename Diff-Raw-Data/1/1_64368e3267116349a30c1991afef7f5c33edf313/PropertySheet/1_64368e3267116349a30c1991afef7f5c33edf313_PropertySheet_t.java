 package edu.cmu.sphinx.util.props;
 
 import java.lang.annotation.Annotation;
 import java.lang.reflect.Field;
 import java.lang.reflect.Modifier;
 import java.lang.reflect.Proxy;
 import java.util.*;
 import java.util.logging.Level;
 import java.util.logging.Logger;
 
 /**
  * A property sheet which  defines a collection of properties for a single component in the system.
  *
  * @author Holger Brandl
  */
 public class PropertySheet implements Cloneable {
 
     public enum PropertyType {
 
         INT, DOUBLE, BOOL, COMP, STRING, COMPLIST
     }
 
     private Map<String, S4PropWrapper> registeredProperties = new HashMap<String, S4PropWrapper>();
     private Map<String, Object> propValues = new HashMap<String, Object>();
 
     /**
      * Maps the names of the component properties to their (possibly unresolved) values.
      * <p/>
      * Example: <code>frontend</code> to <code>${myFrontEnd}</code>
      */
     private Map<String, Object> rawProps = new HashMap<String, Object>();
 
     private ConfigurationManager cm;
     private Configurable owner;
     private final Class<? extends Configurable> ownerClass;
 
     private String instanceName;
 
 
     public PropertySheet(Configurable configurable, String name, RawPropertyData rpd, ConfigurationManager ConfigurationManager) {
         this(configurable.getClass(), name, ConfigurationManager, rpd);
         owner = configurable;
     }
 
 
     public PropertySheet(Class<? extends Configurable> confClass, String name, ConfigurationManager cm, RawPropertyData rpd) {
         ownerClass = confClass;
         this.cm = cm;
         this.instanceName = name;
 
         processAnnotations(this, confClass);
 
         // now apply all xml properties
         Map<String, Object> flatProps = rpd.flatten(cm).getProperties();
         rawProps = new HashMap<String, Object>(rpd.getProperties());
 
         for (String propName : rawProps.keySet())
             propValues.put(propName, flatProps.get(propName));
     }
 
 
     /**
      * Registers a new property which type and default value are defined by the given sphinx property.
      *
      * @param propName The name of the property to be registered.
      * @param property The property annoation masked by a proxy.
      */
     private void registerProperty(String propName, S4PropWrapper property) {
         if (property == null || propName == null)
             throw new InternalConfigurationException(getInstanceName(), propName, "property or its value is null");
 
         registeredProperties.put(propName, property);
         propValues.put(propName, null);
         rawProps.put(propName, null);
     }
 
 
     /** Returns the property names <code>name</code> which is still wrapped into the annotation instance. */
     public S4PropWrapper getProperty(String name, Class propertyClass) throws PropertyException {
         if (!propValues.containsKey(name))
             throw new InternalConfigurationException(getInstanceName(), name,
                     "Unknown property '" + name + "' ! Make sure that you've annotated it.");
 
         S4PropWrapper s4PropWrapper = registeredProperties.get(name);
 
         try {
             propertyClass.cast(s4PropWrapper.getAnnotation());
         } catch (ClassCastException e) {
             throw new InternalConfigurationException(e, getInstanceName(), name, name + " is not an annotated sphinx property of '" + getConfigurableClass().getName() + "' !");
         }
 
         return s4PropWrapper;
     }
 
 
     /**
      * Gets the value associated with this name
      *
      * @param name the name
      * @return the value
      */
     public String getString(String name) throws PropertyException {
         S4PropWrapper s4PropWrapper = getProperty(name, S4String.class);
         S4String s4String = ((S4String) s4PropWrapper.getAnnotation());
 
         if (propValues.get(name) == null) {
             boolean isDefDefined = !s4String.defaultValue().equals(S4String.NOT_DEFINED);
 
             if (s4String.mandatory()) {
                 if (!isDefDefined)
                     throw new InternalConfigurationException(getInstanceName(), name, "mandatory property is not set!");
             }
 //            else if(!isDefDefined)
 //                throw new InternalConfigurationException(getInstanceName(), name, "no default value for non-mandatory property");
 
             propValues.put(name, isDefDefined ? s4String.defaultValue() : null);
         }
 
         String propValue = flattenProp(name);
 
         //check range
         List<String> range = Arrays.asList(s4String.range());
         if (!range.isEmpty() && !range.contains(propValue))
             throw new InternalConfigurationException(getInstanceName(), name, " is not in range (" + range + ")");
 
         return propValue;
     }
 
 
     private String flattenProp(String name) {
         Object value = propValues.get(name);
         return value instanceof String ? (String) value : (value instanceof GlobalProperty ? (String) ((GlobalProperty) value).getValue() : null);
     }
 
 
     /**
      * Gets the value associated with this name
      *
      * @param name the name
      * @return the value
      * @throws edu.cmu.sphinx.util.props.PropertyException
      *          if the named property is not of this type
      */
     public int getInt(String name) throws PropertyException {
         S4PropWrapper s4PropWrapper = getProperty(name, S4Integer.class);
         S4Integer s4Integer = (S4Integer) s4PropWrapper.getAnnotation();
 
         if (propValues.get(name) == null) {
             boolean isDefDefined = !(s4Integer.defaultValue() == S4Integer.NOT_DEFINED);
 
             if (s4Integer.mandatory()) {
                 if (!isDefDefined)
                     throw new InternalConfigurationException(getInstanceName(), name, "mandatory property is not set!");
             } else if (!isDefDefined)
                 throw new InternalConfigurationException(getInstanceName(), name, "no default value for non-mandatory property");
 
             propValues.put(name, s4Integer.defaultValue());
         }
 
         Object propObject = propValues.get(name);
         Integer propValue = propObject instanceof Integer ? (Integer) propObject : Integer.decode(flattenProp(name));
 
         int[] range = s4Integer.range();
         if (range.length != 2)
             throw new InternalConfigurationException(getInstanceName(), name, range + " is not of expected range type, which is {minValue, maxValue)");
 
         if (propValue < range[0] || propValue > range[1])
             throw new InternalConfigurationException(getInstanceName(), name, " is not in range (" + range + ")");
 
         return propValue;
     }
 
 
     /**
      * Gets the value associated with this name
      *
      * @param name the name
      * @return the value
      * @throws edu.cmu.sphinx.util.props.PropertyException
      *          if the named property is not of this type
      */
     public float getFloat(String name) throws PropertyException {
         return ((Double) getDouble(name)).floatValue();
     }
 
 
     /**
      * Gets the value associated with this name
      *
      * @param name the name
      * @return the value
      * @throws edu.cmu.sphinx.util.props.PropertyException
      *          if the named property is not of this type
      */
     public double getDouble(String name) throws PropertyException {
         S4PropWrapper s4PropWrapper = getProperty(name, S4Double.class);
         S4Double s4Double = (S4Double) s4PropWrapper.getAnnotation();
 
         if (propValues.get(name) == null) {
             boolean isDefDefined = !(s4Double.defaultValue() == S4Double.NOT_DEFINED);
 
             if (s4Double.mandatory()) {
                 if (!isDefDefined)
                     throw new InternalConfigurationException(getInstanceName(), name, "mandatory property is not set!");
             } else if (!isDefDefined)
                 throw new InternalConfigurationException(getInstanceName(), name, "no default value for non-mandatory property");
 
             propValues.put(name, s4Double.defaultValue());
         }
 
         Object propObject = propValues.get(name);
         Double propValue = propObject instanceof Double ? (Double) propObject : Double.valueOf(flattenProp(name));
 
         double[] range = s4Double.range();
         if (range.length != 2)
             throw new InternalConfigurationException(getInstanceName(), name, range + " is not of expected range type, which is {minValue, maxValue)");
 
         if (propValue < range[0] || propValue > range[1])
             throw new InternalConfigurationException(getInstanceName(), name, " is not in range (" + range + ")");
 
         return propValue;
     }
 
 
     /**
      * Gets the value associated with this name
      *
      * @param name the name
      * @return the value
      * @throws edu.cmu.sphinx.util.props.PropertyException
      *          if the named property is not of this type
      */
     public Boolean getBoolean(String name) throws PropertyException {
         S4PropWrapper s4PropWrapper = getProperty(name, S4Boolean.class);
         S4Boolean s4Boolean = (S4Boolean) s4PropWrapper.getAnnotation();
 
         if (propValues.get(name) == null && !s4Boolean.isNotDefined())
             propValues.put(name, s4Boolean.defaultValue());
 
         Object propValue = propValues.get(name);
         if (propValue instanceof String)
             propValue = Boolean.valueOf((String) propValue);
 
         return (Boolean) propValue;
     }
 
 
     /**
      * Gets a component associated with the given parameter name
      *
      * @param name the parameter name
      * @return the component associated with the name
      * @throws edu.cmu.sphinx.util.props.PropertyException
      *          if the component does not exist or is of the wrong type.
      */
     public Configurable getComponent(String name) throws PropertyException {
         S4PropWrapper s4PropWrapper = getProperty(name, S4Component.class);
 
         S4Component s4Component = (S4Component) s4PropWrapper.getAnnotation();
         Class expectedType = s4Component.type();
 
         Object propVal = propValues.get(name);
         if (propVal == null || propVal instanceof String || propVal instanceof GlobalProperty) {
             Configurable configurable = null;
 
             try {
                 if (propValues.get(name) != null) {
                     PropertySheet ps = cm.getPropertySheet(flattenProp(name));
                     if (ps != null)
                         configurable = ps.getOwner();
                 }
 
                 if (configurable != null && !expectedType.isInstance(configurable))
                     throw new InternalConfigurationException(getInstanceName(), name, "mismatch between annoation and component type");
 
                 if (configurable == null) {
                     Class<? extends Configurable> defClass;
 
                     if (propValues.get(name) != null)
                         defClass = (Class<? extends Configurable>) Class.forName((String) propValues.get(name));
                     else
                         defClass = s4Component.defaultClass();
 
                     if (defClass.equals(Configurable.class) && s4Component.mandatory()) {
                         throw new InternalConfigurationException(getInstanceName(), name, "mandatory property is not set!");
 
                     } else {
                         if (Modifier.isAbstract(defClass.getModifiers()) && s4Component.mandatory())
                             throw new InternalConfigurationException(getInstanceName(), name, defClass.getName() + " is abstract!");
 
                         // because we're forced to use the default type, make sure that it is set
                         if (defClass.equals(Configurable.class)) {
                             if (s4Component.mandatory()) {
                                 throw new InternalConfigurationException(getInstanceName(), name, instanceName + ": no default class defined for " + name);
                             } else {
                                 return null;
                             }
                         }
 
                         configurable = ConfigurationManager.getInstance(defClass);
                         if (configurable == null) {
                             throw new InternalConfigurationException(getInstanceName(), name,
                                     "instantiation of referenenced Configurable failed");
                         }
                     }
                 }
 
             } catch (ClassNotFoundException e) {
                 throw new PropertyException(e, getInstanceName(), null, null);
             }
 
             propValues.put(name, configurable);
         }
 
         return (Configurable) propValues.get(name);
     }
 
 
     /** Returns the class of of a registered component property without instantiating it. */
     public Class<? extends Configurable> getComponentClass(String propName) {
         Class<? extends Configurable> defClass = null;
 
         if (propValues.get(propName) != null)
             try {
                 defClass = (Class<? extends Configurable>) Class.forName((String) propValues.get(propName));
             } catch (ClassNotFoundException e) {
                 e.printStackTrace();
             }
         else {
             S4Component comAnno = (S4Component) registeredProperties.get(propName).getAnnotation();
             defClass = comAnno.defaultClass();
             if (comAnno.mandatory())
                 defClass = null;
         }
 
         return defClass;
     }
 
 
     /**
      * Gets a list of components associated with the given parameter name
      *
      * @param name the parameter name
      * @return the component associated with the name
      * @throws edu.cmu.sphinx.util.props.PropertyException
      *          if the component does not exist or is of the wrong type.
      */
     public List<? extends Configurable> getComponentList(String name) throws InternalConfigurationException {
         getProperty(name, S4ComponentList.class);
 
         List components = (List) propValues.get(name);
 
         assert registeredProperties.get(name).getAnnotation() instanceof S4ComponentList;
         S4ComponentList annoation = (S4ComponentList) registeredProperties.get(name).getAnnotation();
 
         // no componets names are available and no comp-list was yet loaded
         // therefore load the default list of components from the annoation
         if (components == null) {
             List<Class<? extends Configurable>> defClasses = Arrays.asList(annoation.defaultList());
 
 //            if (annoation.mandatory() && defClasses.isEmpty())
 //                throw new InternalConfigurationException(getInstanceName(), name, "mandatory property is not set!");
 
             components = new ArrayList();
 
             for (Class<? extends Configurable> defClass : defClasses) {
                 components.add(ConfigurationManager.getInstance(defClass));
             }
 
             propValues.put(name, components);
         }
 
         if (!components.isEmpty() && !(components.get(0) instanceof Configurable)) {
 
             List<Configurable> list = new ArrayList<Configurable>();
 
             for (Object componentName : components) {
                 Configurable configurable = cm.lookup((String) componentName);
 
                 if (configurable != null) {
                     list.add(configurable);
                 } else if (!annoation.beTolerant()) {
                     throw new InternalConfigurationException(name, (String) componentName,
                             "lookup of list-element '" + componentName + "' failed!");
                 }
             }
 
             propValues.put(name, list);
         }
 
         return (List<? extends Configurable>) propValues.get(name);
     }
 
 
     public String getInstanceName() {
         return instanceName;
     }
 
 
     public void setInstanceName(String newInstanceName) {
         this.instanceName = newInstanceName;
     }
 
 
     /** Returns true if the owner of this property sheet is already instanciated. */
     public boolean isInstanciated() {
         return !(owner == null);
     }
 
 
     /**
      * Returns the owner of this property sheet. In most cases this will be the configurable instance which was
      * instrumented by this property sheet.
      */
     public synchronized Configurable getOwner() {
         try {
 
             if (!isInstanciated()) {
                 owner = ownerClass.newInstance();
                 owner.newProperties(this);
             }
         } catch (IllegalAccessException e) {
             throw new InternalConfigurationException(e, getInstanceName(), null, "Can't access class " + ownerClass);
         } catch (InstantiationException e) {
             throw new InternalConfigurationException(e, getInstanceName(), null, "Can't instantiate class " + ownerClass);
         }
 
         return owner;
     }
 
 
     /** Returns the class of the owner configurable of this property sheet. */
     public Class<? extends Configurable> getConfigurableClass() {
         return ownerClass;
     }
 
 
     /**
      * Sets the given property to the given name
      *
      * @param name the simple property name
      */
     public void setString(String name, String value) throws PropertyException {
         // ensure that there is such a property
         if (!registeredProperties.keySet().contains(name))
             throw new InternalConfigurationException(getInstanceName(), name, "'" + name +
                     "' is not a registered string-property");
 
         Proxy annotation = registeredProperties.get(name).getAnnotation();
         if (!(annotation instanceof S4String))
             throw new InternalConfigurationException(getInstanceName(), name, "'" + name + "' is of type string");
 
         applyConfigurationChange(name, value, value);
     }
 
 
     /**
      * Sets the given property to the given name
      *
      * @param name  the simple property name
      * @param value the value for the property
      */
     public void setInt(String name, int value) throws PropertyException {
         // ensure that there is such a property
         if (!registeredProperties.keySet().contains(name))
             throw new InternalConfigurationException(getInstanceName(), name, "'" + name +
                     "' is not a registered int-property");
 
         Proxy annotation = registeredProperties.get(name).getAnnotation();
         if (!(annotation instanceof S4Integer))
             throw new InternalConfigurationException(getInstanceName(), name, "'" + name + "' is of type int");
 
         applyConfigurationChange(name, value, value);
     }
 
 
     /**
      * Sets the given property to the given name
      *
      * @param name  the simple property name
      * @param value the value for the property
      */
     public void setDouble(String name, double value) throws PropertyException {
         // ensure that there is such a property
         if (!registeredProperties.keySet().contains(name))
             throw new InternalConfigurationException(getInstanceName(), name, "'" + name +
                     "' is not a registered double-property");
 
         Proxy annotation = registeredProperties.get(name).getAnnotation();
         if (!(annotation instanceof S4Double))
             throw new InternalConfigurationException(getInstanceName(), name, "'" + name + "' is of type double");
 
         applyConfigurationChange(name, value, value);
     }
 
 
     /**
      * Sets the given property to the given name
      *
      * @param name  the simple property name
      * @param value the value for the property
      */
     public void setBoolean(String name, Boolean value) throws PropertyException {
         if (!registeredProperties.keySet().contains(name))
             throw new InternalConfigurationException(getInstanceName(), name, "'" + name +
                     "' is not a registered boolean-property");
 
         Proxy annotation = registeredProperties.get(name).getAnnotation();
         if (!(annotation instanceof S4Boolean))
             throw new InternalConfigurationException(getInstanceName(), name, "'" + name + "' is of type boolean");
 
         applyConfigurationChange(name, value, value);
     }
 
 
     /**
      * Sets the given property to the given name
      *
      * @param name   the simple property name
      * @param cmName the name of the configurable within the configuration manager (required for serialization only)
      * @param value  the value for the property
      */
     public void setComponent(String name, String cmName, Configurable value) throws PropertyException {
         if (!registeredProperties.keySet().contains(name))
             throw new InternalConfigurationException(getInstanceName(), name, "'" + name +
                     "' is not a registered compontent");
 
         Proxy annotation = registeredProperties.get(name).getAnnotation();
         if (!(annotation instanceof S4Component))
             throw new InternalConfigurationException(getInstanceName(), name, "'" + name + "' is of type component");
 
 
         applyConfigurationChange(name, cmName, value);
     }
 
 
     /**
      * Sets the given property to the given name
      *
      * @param name       the simple property name
      * @param valueNames the list of names of the configurables within the configuration manager (required for
      *                   serialization only)
      * @param value      the value for the property
      */
     public void setComponentList(String name, List<String> valueNames, List<Configurable> value) throws PropertyException {
         if (!registeredProperties.keySet().contains(name))
             throw new InternalConfigurationException(getInstanceName(), name, "'" + name +
                     "' is not a registered component-list");
 
         Proxy annotation = registeredProperties.get(name).getAnnotation();
         if (!(annotation instanceof S4ComponentList))
             throw new InternalConfigurationException(getInstanceName(), name, "'" + name + "' is of type component-list");
 
         rawProps.put(name, valueNames);
         propValues.put(name, value);
 
         applyConfigurationChange(name, valueNames, value);
     }
 
 
     private void applyConfigurationChange(String propName, Object cmName, Object value) throws PropertyException {
         rawProps.put(propName, cmName);
         propValues.put(propName, value);
 
         if (getInstanceName() != null)
             cm.fireConfChanged(getInstanceName(), propName);
 
         if (owner != null)
             owner.newProperties(this);
     }
 
 
     /**
      * Sets the raw property to the given name
      *
      * @param key the simple property name
      * @param val the value for the property
      */
     public void setRaw(String key, Object val) {
         rawProps.put(key, val);
        propValues.put(key, val);
     }
 
 
     /**
      * Gets the raw value associated with this name
      *
      * @param name the name
      * @return the value as an object (it could be a String or a String[] depending upon the property type)
      */
     public Object getRaw(String name) {
         return rawProps.get(name);
     }
 
 
     /**
      * Gets the raw value associated with this name, no global symbol replacement is performed.
      *
      * @param name the name
      * @return the value as an object (it could be a String or a String[] depending upon the property type)
      */
     public Object getRawNoReplacement(String name) {
         return rawProps.get(name);
     }
 
 
     /** Returns the type of the given property. */
     public PropertyType getType(String propName) {
         Proxy annotation = registeredProperties.get(propName).getAnnotation();
         if (annotation instanceof S4Component)
             return PropertyType.COMP;
         else if (annotation instanceof S4ComponentList)
             return PropertyType.COMPLIST;
         else if (annotation instanceof S4Integer)
             return PropertyType.INT;
         else if (annotation instanceof S4Double)
             return PropertyType.DOUBLE;
         else if (annotation instanceof S4Boolean)
             return PropertyType.BOOL;
         else if (annotation instanceof S4String)
             return PropertyType.STRING;
         else
             throw new RuntimeException("Unknown property type");
     }
 
 
     /**
      * Gets the owning property manager
      *
      * @return the property manager
      */
     ConfigurationManager getPropertyManager() {
         return cm;
     }
 
 
     /**
      * Returns a logger to use for this configurable component. The logger can be configured with the property:
      * 'logLevel' - The default logLevel value is defined (within the xml configuration file by the global property
      * 'defaultLogLevel' (which defaults to WARNING).
      * <p/>
      * implementation note: the logger became configured within the constructor of the parenting configuration manager.
      *
      * @return the logger for this component
      * @throws edu.cmu.sphinx.util.props.PropertyException
      *          if an error occurs
      */
     public Logger getLogger() {
         Logger logger;
 
         if (instanceName != null) {
             logger = Logger.getLogger(ownerClass.getName() + "." + instanceName);
         } else
             logger = Logger.getLogger(ownerClass.getName());
 
         // if there's a logLevel set for component apply to the logger
         if (rawProps.get("logLevel") != null)
             logger.setLevel(Level.parse((String) rawProps.get("logLevel")));
 
         return logger;
 
     }
 
 
     /** Returns the names of registered properties of this PropertySheet object. */
     public Collection<String> getRegisteredProperties() {
         return Collections.unmodifiableCollection(registeredProperties.keySet());
     }
 
 
     public void setCM(ConfigurationManager cm) {
         this.cm = cm;
     }
 
 
     /**
      * Returns true if two property sheet define the same object in terms of configuration. The owner (and the parent
      * configuration manager) are not expected to be the same.
      */
     public boolean equals(Object obj) {
         if (obj == null || !(obj instanceof PropertySheet))
             return false;
 
         PropertySheet ps = (PropertySheet) obj;
         if (!rawProps.keySet().equals(ps.rawProps.keySet()))
             return false;
 
         // maybe we could test a little bit more here. suggestions?
         return true;
     }
 
 
     @Override
     public String toString() {
         return getInstanceName() + "; isInstantiated=" + isInstanciated() + "; props=" + rawProps.keySet().toString();
     }
 
 
     protected Object clone() throws CloneNotSupportedException {
         PropertySheet ps = (PropertySheet) super.clone();
 
         ps.registeredProperties = new HashMap<String, S4PropWrapper>(this.registeredProperties);
         ps.propValues = new HashMap<String, Object>(this.propValues);
 
         ps.rawProps = new HashMap<String, Object>(this.rawProps);
 
         // make deep copy of raw-lists
         for (String regProp : ps.getRegisteredProperties()) {
             if (getType(regProp).equals(PropertyType.COMPLIST)) {
                 ps.rawProps.put(regProp, new ArrayList<String>((Collection<? extends String>) rawProps.get(regProp)));
                 ps.propValues.put(regProp, null);
             }
         }
 
         ps.cm = cm;
         ps.owner = null;
         ps.instanceName = this.instanceName;
 
         return ps;
     }
 
 
     /**
      * use annotation based class parsing to detect the configurable properties of a <code>Configurable</code>-class
      *
      * @param propertySheet of type PropertySheet
      * @param configurable  of type Class<? extends Configurable>
      */
     public static void processAnnotations(PropertySheet propertySheet, Class<? extends Configurable> configurable) {
         Field[] classFields = configurable.getFields();
 
         for (Field field : classFields) {
             Annotation[] annotations = field.getAnnotations();
 
             for (Annotation annotation : annotations) {
                 Annotation[] superAnnotations = annotation.annotationType().getAnnotations();
 
                 for (Annotation superAnnotation : superAnnotations) {
                     if (superAnnotation instanceof S4Property) {
                         int fieldModifiers = field.getModifiers();
                         assert Modifier.isStatic(fieldModifiers) : "property fields are assumed to be static";
                         assert Modifier.isPublic(fieldModifiers) : "property fields are assumed to be public";
                         assert field.getType().equals(String.class) : "properties fields are assumed to be instances of java.lang.String";
 
                         try {
                             String propertyName = (String) field.get(null);
                             propertySheet.registerProperty(propertyName, new S4PropWrapper((Proxy) annotation));
                         } catch (IllegalAccessException e) {
                             e.printStackTrace();
                         }
 
                     }
                 }
             }
         }
     }
 }
