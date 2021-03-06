 
 /*
  * Carrot2 project.
  *
  * Copyright (C) 2002-2008, Dawid Weiss, Stanisław Osiński.
  * Portions (C) Contributors listed in "carrot2.CONTRIBUTORS" file.
  * All rights reserved.
  *
  * Refer to the full license file "carrot2.LICENSE"
  * in the root folder of the repository checkout or at:
  * http://www.carrot2.org/carrot2.LICENSE
  */
 
 package org.carrot2.util.attribute;
 
 import java.io.File;
 import java.lang.annotation.Annotation;
 import java.lang.reflect.Field;
 import java.lang.reflect.Method;
 import java.util.*;
 
 import org.apache.commons.lang.ClassUtils;
 import org.carrot2.util.Pair;
 import org.carrot2.util.attribute.constraint.*;
 import org.carrot2.util.resource.Resource;
 
 import com.google.common.collect.*;
 
 /**
  * Provides methods for binding (setting and collecting) values of attributes defined by
  * the {@link Attribute} annotation.
  */
 public class AttributeBinder
 {
     /** Consistency checks to be applied before binding */
     private final static ConsistencyCheck [] CONSISTENCY_CHECKS = new ConsistencyCheck []
     {
         new ConsistencyCheckRequiredAnnotations(),
         new ConsistencyCheckImplementingClasses()
     };
 
     /**
      * Performs binding (setting or collecting) of {@link Attribute} values on the
      * provided <code>instance</code>. The direction of binding, i.e. whether attributes
      * will be set or collected from the <code>object</code> depends on the provided
      * <code>bindingDirectionAnnotation</code>, which can be either {@link Input} or
      * {@link Output} for setting and collecting attribute values of the
      * <code>object</code>, respectively.
      * <p>
      * Binding will be performed for all attributes of the provided <code>object</code>,
      * no matter where in the <code>object</code>'s hierarchy the attribute is declared.
      * Binding will recursively descend into all fields of the <code>object</code> whose
      * types are marked with {@link Bindable}, no matter whether these fields are
      * attributes or not.
      * <p>
      * Keys of the <code>values</code> map are interpreted as attribute keys as defined by
      * {@link Attribute#key()}. When setting attribute values, the map must contain non-
      * <code>null</code> mappings for all {@link Required} attributes that have not yet
      * been set on the <code>object</code> to a non-<code>null</code> value. Otherwise an
      * {@link AttributeBindingException} will be thrown. If the map has no mapping for
      * some non-{@link Required} attribute, the value of that attribute will not be
      * changed. However, if the map contains a <code>null</code> mapping for some non-
      * {@link Required} attribute, the value that attribute will be set to
      * <code>null</code>.
      * <p>
      * When setting attributes, values will be transferred from the map without any
      * conversion with two exceptions.
      * <ol>
      * <li>If the type of the value is {@link String} and the type of the attribute field
      * is not {@link String}, the {@link AttributeTransformerFromString} will be applied
      * to the value prior to transferring it to the attribute field. If you want to bypass
      * this conversion, use
      * {@link #bind(Object, AttributeBinderAction[], Class, Class...)}.</li>
      * <li>If the type of the attribute field is not {@link Class} and the corresponding
      * value in the <code>values</code> map is of type {@link Class}, an attempt will be
      * made to coerce the class to a corresponding instance by calling its parameterless
      * constructor. If the created type is {@link Bindable}, an attempt will also be made
      * to bind attributes of the newly created object using the <code>values</code> map,
      * current <code>bindingDirectionAnnotation</code> and
      * <code>filteringAnnotations</code>.</li>
      * </ol>
      * <p>
      * Before value of an attribute is set, the new value is checked against all
      * constraints defined for the attribute and must meet all these constraints.
      * Otherwise, the {@link ConstraintViolationException} will be thrown.
      * <p>
      * 
      * @param object the object to set or collect attributes from. The type of the
      *            provided object must be annotated with {@link Bindable}.
      * @param values the values of {@link Input} attributes to be set or a placeholder for
      *            {@link Output} attributes to be collected. If attribute values are to be
      *            collected, the provided Map must be modifiable.
      * @param bindingDirectionAnnotation {@link Input} if attribute values are to be set
      *            on the provided <code>object</code>, or {@link Output} if attribute
      *            values are to be collected from the <code>object</code>.
      * @param filteringAnnotations additional domain-specific annotations that the
      *            attribute fields must have in order to be bound. This parameter can be
      *            used to selectively bind different set of attributes depending, e.g. on
      *            the life cycle of the <code>object</code>.
      * @return entries from the <code>values</code> map that did not get bound to any of
      *         the {@link Input} attributes.
      * @throws InstantiationException if coercion of a class attribute value to an
      *             instance fails, e.g. because the parameterless constructor is not
      *             present/ visible.
      * @throws AttributeBindingException if in the <code>values</code> map there are no or
      *             <code>null</code> values provided for one or more {@link Required}
      *             attributes.
      * @throws AttributeBindingException reflection-based setting or reading field values
      *             fails.
      * @throws IllegalArgumentException if <code>bindingDirectionAnnotation</code> is
      *             different from {@link Input} or {@link Output}.
      * @throws IllegalArgumentException if <code>object</code>'s type is not
      *             {@link Bindable}.
      * @throws IllegalArgumentException for debugging purposes, if an attribute field is
      *             found that is missing some of the required annotations.
      * @throws UnsupportedOperationException if an attempt is made to bind values of
      *             attributes with circular references.
      */
     public static <T> Map<String, Object> bind(T object, Map<String, Object> values,
         Class<? extends Annotation> bindingDirectionAnnotation,
         Class<? extends Annotation>... filteringAnnotations)
         throws InstantiationException, AttributeBindingException
     {
         return bind(object, values, true, bindingDirectionAnnotation,
             filteringAnnotations);
     }
 
     /**
      * A version of {@link #bind(Object, Map, Class, Class...)} that can optionally skip
      * {@link Required} attribute checking. For experts only.
      */
     public static <T> Map<String, Object> bind(T object, Map<String, Object> values,
         boolean checkRequired, Class<? extends Annotation> bindingDirectionAnnotation,
         Class<? extends Annotation>... filteringAnnotations)
         throws InstantiationException, AttributeBindingException
     {
         final AttributeBinderActionBind attributeBinderActionBind = new AttributeBinderActionBind(
             Input.class, values, checkRequired, AttributeTransformerFromString.INSTANCE);
         final AttributeBinderAction [] actions = new AttributeBinderAction []
         {
             attributeBinderActionBind,
             new AttributeBinderActionCollect(Output.class, values),
         };
 
         bind(object, actions, bindingDirectionAnnotation, filteringAnnotations);
 
         return attributeBinderActionBind.remainingValues;
     }
 
     /**
      * A complementary version of the {@link #bind(Object, Map, Class, Class...)} method.
      * This method <strong>collects</strong> values of {@link Input} attributes and
      * <strong>sets</strong> values of {@link Output} attributes.
      * 
      * @return entries from the <code>values</code> map that did not get bound to any of
      *         the {@link Output} attributes.
      */
     public static <T> Map<String, Object> unbind(T object, Map<String, Object> values,
         Class<? extends Annotation> bindingDirectionAnnotation,
         Class<? extends Annotation>... filteringAnnotations)
         throws InstantiationException, AttributeBindingException
     {
         final AttributeBinderActionBind attributeBinderActionBind = new AttributeBinderActionBind(
             Output.class, values, true, AttributeTransformerFromString.INSTANCE);
         final AttributeBinderAction [] actions = new AttributeBinderAction []
         {
             new AttributeBinderActionCollect(Input.class, values),
             attributeBinderActionBind,
         };
 
         bind(object, actions, bindingDirectionAnnotation, filteringAnnotations);
 
         return attributeBinderActionBind.remainingValues;
     }
 
     /**
      * A more flexible version of {@link #bind(Object, Map, Class, Class...)} that accepts
      * custom {@link AttributeBinderAction}s. For experts only.
      */
     public static <T> void bind(T object,
         AttributeBinderAction [] attributeBinderActions,
         Class<? extends Annotation> bindingDirectionAnnotation,
         Class<? extends Annotation>... filteringAnnotations)
         throws InstantiationException, AttributeBindingException
     {
         bind(new HashSet<Object>(), new BindingTracker(), 0, object,
             attributeBinderActions, bindingDirectionAnnotation, filteringAnnotations);
     }
 
     /**
      * Internal implementation that tracks object that have already been bound.
      */
     static <T> void bind(Set<Object> boundObjects, BindingTracker bindingTracker,
         int level, T object, AttributeBinderAction [] attributeBinderActions,
         Class<? extends Annotation> bindingDirectionAnnotation,
         Class<? extends Annotation>... filteringAnnotations)
         throws InstantiationException, AttributeBindingException
     {
         // Binding direction can be either @Input or @Output
         if (!Input.class.equals(bindingDirectionAnnotation)
             && !Output.class.equals(bindingDirectionAnnotation))
         {
             throw new IllegalArgumentException(
                 "bindingDirectionAnnotation must either be "
                     + Input.class.getSimpleName() + " or " + Output.class.getSimpleName());
         }
 
         // We can only bind values on classes that are @Bindable
         if (object.getClass().getAnnotation(Bindable.class) == null)
         {
             throw new IllegalArgumentException("Class is not bindable: "
                 + object.getClass().getName());
         }
 
         // For keeping track of circular references
         boundObjects.add(object);
 
         // Get all fields (including those from bindable super classes)
         final Collection<Field> fieldSet = BindableUtils
             .getFieldsFromBindableHierarchy(object.getClass());
 
         for (final Field field : fieldSet)
         {
             final String key = BindableUtils.getKey(field);
             Object value = null;
 
             // Get the @Bindable value to perform a recursive call on it later on
             try
             {
                 field.setAccessible(true);
                 value = field.get(object);
             }
             catch (final Exception e)
             {
                 throw new AttributeBindingException(key, "Could not get field value "
                     + object.getClass().getName() + "#" + field.getName());
             }
 
             // Apply consistency checks
             boolean consistent = true;
             for (int i = 0; consistent && i < CONSISTENCY_CHECKS.length; i++)
             {
                 consistent &= CONSISTENCY_CHECKS[i].check(field,
                     bindingDirectionAnnotation, filteringAnnotations);
             }
 
             // We skip fields that do not have all the required annotations
             if (consistent)
             {
                 // Apply binding actions provided
                 for (int i = 0; i < attributeBinderActions.length; i++)
                 {
                     attributeBinderActions[i].performAction(bindingTracker, level,
                         object, key, field, value, bindingDirectionAnnotation,
                         filteringAnnotations);
                 }
             }
 
             // If value is not null and its class is @Bindable, we must descend into it
             if (value != null && value.getClass().getAnnotation(Bindable.class) != null)
             {
                 // Check for circular references
                 if (boundObjects.contains(value))
                 {
                     throw new UnsupportedOperationException(
                         "Circular references are not supported");
                 }
 
                 // Recursively descend into other types.
                 bind(boundObjects, bindingTracker, level + 1, value,
                     attributeBinderActions, bindingDirectionAnnotation,
                     filteringAnnotations);
             }
         }
     }
 
     /**
      * An action to be applied during attribute binding.
      */
     public static interface AttributeBinderAction
     {
         public <T> void performAction(BindingTracker bindingTracker, int level, T object,
             String key, Field field, Object value,
             Class<? extends Annotation> bindingDirectionAnnotation,
             Class<? extends Annotation>... filteringAnnotations)
             throws InstantiationException;
     }
 
     /**
      * Transforms attribute values.
      */
     public static interface AttributeTransformer
     {
         public Object transform(Object value, String key, Field field,
             Class<? extends Annotation> bindingDirectionAnnotation,
             Class<? extends Annotation>... filteringAnnotations);
     }
 
     /**
      * Transforms {@link String} attribute values to the types required by the target
      * field by:
      * <ol>
      * <li>Leaving non-{@link String} typed values unchanged.</li> <li>Looking for a
      * static <code>valueOf(String)</code> in the target type and using it for conversion.
      * </li> <li>If the method is not available, trying to load a class named as the value
      * of the attribute, so that this class can be further coerced to the class instance.
      * </li> <li>If the class cannot be loaded, leaving the the value unchanged.</li>
      * </ol>
      */
     public static class AttributeTransformerFromString implements AttributeTransformer
     {
         /** Shared instance of the transformer. */
         public static final AttributeTransformerFromString INSTANCE = new AttributeTransformerFromString();
 
         /**
          * Private constructor, use {{@link #INSTANCE}.
          */
         private AttributeTransformerFromString()
         {
         }
 
         public Object transform(Object value, String key, Field field,
             Class<? extends Annotation> bindingDirectionAnnotation,
             Class<? extends Annotation>... filteringAnnotations)
         {
             if (!(value instanceof String))
             {
                 return value;
             }
 
             final String stringValue = (String) value;
             final Class<?> fieldType = ClassUtils.primitiveToWrapper(field.getType());
             if (String.class.equals(fieldType))
             {
                 // Return Strings unchanged
                 return stringValue;
             }
             else
             {
                 // Try valueOf(String)
                 try
                 {
                     final Method valueOfMethod = fieldType.getMethod("valueOf",
                         String.class);
                     return valueOfMethod.invoke(null, stringValue);
                 }
                 catch (RuntimeException e)
                 {
                     throw e;
                 }
                 catch (Exception e)
                 {
                     // Just skip this possibility
                 }
 
                 // Try if we can assign anyway. If the attribute is of a non-primitive
                 // type, it must have an ImplementingClasses annotation, see
                 // ConsistencyCheckImplementingClasses. If the value meets the constraint,
                 // we'll return the original value.
                 final ImplementingClasses implementingClasses = field
                     .getAnnotation(ImplementingClasses.class);
                 if (implementingClasses != null
                     && ConstraintValidator.isMet(stringValue, implementingClasses).length == 0)
                 {
                     return stringValue;
                 }
 
                 // Try loading the class
                 try
                 {
                     return Thread.currentThread().getContextClassLoader().loadClass(
                         stringValue);
                 }
                 catch (ClassNotFoundException e)
                 {
                     // Just skip this possibility
                 }
 
                 return stringValue;
             }
         }
     }
 
     /**
      * An action that binds all {@link Input} attributes.
      */
     public static class AttributeBinderActionBind implements AttributeBinderAction
     {
         private final Map<String, Object> values;
         public final Map<String, Object> remainingValues;
         private final Class<?> bindingDirectionAnnotation;
         private final boolean checkRequired;
         private final AttributeTransformer [] transformers;
 
         public AttributeBinderActionBind(Class<?> bindingDirectionAnnotation,
             Map<String, Object> values, boolean checkRequired,
             AttributeTransformer... transformers)
         {
             this.values = values;
             this.bindingDirectionAnnotation = bindingDirectionAnnotation;
             this.checkRequired = checkRequired;
             this.transformers = transformers;
             this.remainingValues = Maps.newHashMap(values);
         }
 
         public <T> void performAction(BindingTracker bindingTracker, int level, T object,
             String key, Field field, Object value,
             Class<? extends Annotation> bindingDirectionAnnotation,
             Class<? extends Annotation>... filteringAnnotations)
             throws InstantiationException
         {
             if (this.bindingDirectionAnnotation.equals(bindingDirectionAnnotation)
                 && field.getAnnotation(bindingDirectionAnnotation) != null)
             {
                 final boolean required = field.getAnnotation(Required.class) != null
                     && checkRequired;
                 final Object currentValue = value;
 
                 // Transfer values from the map to the fields. If the input map
                 // doesn't contain an entry for this key, do nothing. Otherwise,
                 // perform binding as usual. This will allow to set null values
                 if (!values.containsKey(key))
                 {
                     if (currentValue == null && required)
                     {
                         // Throw exception only if the current value is null
                         throw new AttributeBindingException(key,
                             "No value for required attribute: " + key);
                     }
                     return;
                 }
 
                 // Note that the value can still be null here
                 value = values.get(key);
 
                 if (required)
                 {
                     if (value == null)
                     {
                         throw new AttributeBindingException(key,
                             "Not allowed to set required attribute to null: " + key);
                     }
                 }
 
                 // Apply value transformers before any other checks, conversions
                 // to allow type-changing transformations as well.
                 for (AttributeTransformer transformer : transformers)
                 {
                     value = transformer.transform(value, key, field,
                         bindingDirectionAnnotation, filteringAnnotations);
                 }
 
                 // Try to coerce from class to its instance first
                 // Notice that if some extra annotations are provided, the newly
                 // created instance will get only those attributes bound that
                 // match any of the extra annotations.
                 if (value instanceof Class && !field.getType().equals(Class.class))
                 {
                     final Class<?> clazz = ((Class<?>) value);
                     try
                     {
                         value = clazz.newInstance();
                         if (clazz.isAnnotationPresent(Bindable.class))
                         {
                             bind(value, values, Input.class, filteringAnnotations);
                         }
                     }
                     catch (final InstantiationException e)
                     {
                         throw new InstantiationException(
                             "Could not create instance of class: " + clazz.getName()
                                 + " for attribute " + key);
                     }
                     catch (final IllegalAccessException e)
                     {
                         throw new InstantiationException(
                             "Could not create instance of class: " + clazz.getName()
                                 + " for attribute " + key);
                     }
                 }
 
                 if (value != null)
                 {
                     // Check constraints
                     final Annotation [] unmetConstraints = ConstraintValidator.isMet(
                         value, field.getAnnotations());
                     if (unmetConstraints.length > 0)
                     {
                         throw new ConstraintViolationException(key, value,
                             unmetConstraints);
                     }
                 }
 
                 // Finally, set the field value
                 try
                 {
                     field.setAccessible(true);
                     field.set(object, value);
                 }
                 catch (final Exception e)
                 {
                     throw new AttributeBindingException(key, "Could not assign field "
                         + object.getClass().getName() + "#" + field.getName()
                         + " with value " + value, e);
                 }
 
                 remainingValues.remove(key);
             }
         }
     }
 
     /**
      * An action that binds all {@link Output} attributes.
      */
     public static class AttributeBinderActionCollect implements AttributeBinderAction
     {
         final private Map<String, Object> values;
         final private Class<?> bindingDirectionAnnotation;
         final AttributeTransformer [] transformers;
 
         public AttributeBinderActionCollect(Class<?> bindingDirectionAnnotation,
             Map<String, Object> values, AttributeTransformer... transformers)
         {
             this.values = values;
             this.bindingDirectionAnnotation = bindingDirectionAnnotation;
             this.transformers = transformers;
         }
 
         public <T> void performAction(BindingTracker bindingTracker, int level, T object,
             String key, Field field, Object value,
             Class<? extends Annotation> bindingDirectionAnnotation,
             Class<? extends Annotation>... filteringAnnotations)
             throws InstantiationException
         {
             if (this.bindingDirectionAnnotation.equals(bindingDirectionAnnotation)
                 && field.getAnnotation(bindingDirectionAnnotation) != null)
             {
                 try
                 {
                     field.setAccessible(true);
 
                     // Apply transforms
                     for (AttributeTransformer transformer : transformers)
                     {
                         value = transformer.transform(value, key, field,
                             bindingDirectionAnnotation, filteringAnnotations);
                     }
 
                     if (bindingTracker.canBind(object, key, level))
                     {
                         values.put(key, value);
                     }
                 }
                 catch (final Exception e)
                 {
                     throw new AttributeBindingException(key, "Could not get field value "
                        + object.getClass().getName() + "#" + field.getName(), e);
                 }
             }
         }
     }
 
     /**
      * Checks individual attribute definitions for consistency, e.g. whether they have all
      * required annotations.
      */
     static abstract class ConsistencyCheck
     {
         /**
          * Checks an attribute's annotations.
          * 
          * @param bindingDirection
          * @return <code>true</code> if the attribute passed the check and can be bound,
          *         <code>false</code> if the attribute did not pass the check and cannot
          *         be bound.
          * @throws IllegalArgumentException when attribute's annotations are inconsistent
          */
         abstract boolean check(Field field, Class<? extends Annotation> bindingDirection,
             Class<? extends Annotation>... filteringAnnotations);
     }
 
     /**
      * Checks if all required attribute annotations are provided.
      */
     static class ConsistencyCheckRequiredAnnotations extends ConsistencyCheck
     {
         @Override
         boolean check(Field field, Class<? extends Annotation> bindingDirection,
             Class<? extends Annotation>... filteringAnnotations)
         {
             final boolean hasAttribute = field.getAnnotation(Attribute.class) != null;
             boolean hasBindingDirection = field.getAnnotation(Input.class) != null
                 || field.getAnnotation(Output.class) != null;
 
             boolean hasRequiredExtraAnnotations = false;
             for (Class<? extends Annotation> filteringAnnotation : filteringAnnotations)
             {
                 hasRequiredExtraAnnotations |= field.getAnnotation(filteringAnnotation) != null;
             }
 
             if (hasAttribute)
             {
                 if (!hasBindingDirection)
                 {
                     throw new IllegalArgumentException(
                         "Define binding direction annotation (@"
                             + Input.class.getSimpleName() + " or @"
                             + Output.class.getSimpleName() + ") for field "
                             + field.getClass().getName() + "#" + field.getName());
                 }
             }
             else
             {
                 if (hasBindingDirection || hasRequiredExtraAnnotations)
                 {
                     throw new IllegalArgumentException(
                         "Binding time or direction defined for a field ("
                             + field.getClass() + "#" + field.getName()
                             + ") that does not have an @"
                             + Attribute.class.getSimpleName() + " annotation");
                 }
             }
 
             return hasAttribute
                 && (filteringAnnotations.length == 0 || hasRequiredExtraAnnotations);
         }
     }
 
     /**
      * Checks whether attributes of non-primitive types have the
      * {@link ImplementingClasses} constraint.
      */
     static class ConsistencyCheckImplementingClasses extends ConsistencyCheck
     {
         static Set<Class<?>> ALLOWED_PLAIN_TYPES = ImmutableSet.<Class<?>> of(Byte.class,
             Short.class, Integer.class, Long.class, Float.class, Double.class,
             Boolean.class, String.class, Character.class, Class.class, Resource.class,
             Collection.class, Map.class, File.class);
 
         static Set<Class<?>> ALLOWED_ASSIGNABLE_TYPES = ImmutableSet.<Class<?>> of(
             Enum.class, Resource.class, Collection.class, Map.class);
 
         @Override
         boolean check(Field field, Class<? extends Annotation> bindingDirection,
             Class<? extends Annotation>... filteringAnnotations)
         {
             if (!Input.class.equals(bindingDirection))
             {
                 return true;
             }
 
             final Class<?> attributeType = ClassUtils.primitiveToWrapper(field.getType());
 
             if (!ALLOWED_PLAIN_TYPES.contains(attributeType)
                 && !isAllowedAssignableType(attributeType)
                 && field.getAnnotation(ImplementingClasses.class) == null)
             {
                 throw new IllegalArgumentException("Non-primitive typed attribute "
                     + field.getDeclaringClass().getName() + "#" + field.getName()
                     + " must have the @" + ImplementingClasses.class.getSimpleName()
                     + " constraint.");
             }
 
             return true;
         }
 
         private static boolean isAllowedAssignableType(Class<?> attributeType)
         {
             for (Class<?> clazz : ALLOWED_ASSIGNABLE_TYPES)
             {
                 if (clazz.isAssignableFrom(attributeType))
                 {
                     return true;
                 }
             }
 
             return false;
         }
     }
 
     /**
      * Tracks which attributes have already been collected and prevents overwriting of
      * collected values.
      */
     private static class BindingTracker
     {
         /**
          * The lowest nesting level from which the attribute has been collected.
          */
         private Map<String, Integer> bindingLevel = Maps.newHashMap();
 
         /**
          * Containing instance + attribute key pairs that have already been collected.
          */
         private Set<Pair<Object, String>> boundInstances = Sets.newHashSet();
 
         boolean canBind(Object instance, String key, int level)
         {
             final Pair<Object, String> pair = new Pair<Object, String>(instance, key);
             if (boundInstances.contains(pair))
             {
                 throw new AttributeBindingException(
                     "Collecting values of multiple attributes with the same key (" + key
                         + ") in the same instance of class ("
                         + instance.getClass().getName() + ") is not allowed");
             }
             boundInstances.add(pair);
 
             // We can collect this attribute if:
             // 1) it has not yet been collected or
             // 2) it has been collected at a deeper level of the nesting hierarchy
             // but we found another value for it found closer to the root object for
             // which binding is performed.
             final Integer boundAtLevel = bindingLevel.get(key);
             final boolean canBind = boundAtLevel == null
                 || (boundAtLevel != null && boundAtLevel > level);
             if (canBind)
             {
                 bindingLevel.put(key, level);
             }
             return canBind;
         }
     }
 }
