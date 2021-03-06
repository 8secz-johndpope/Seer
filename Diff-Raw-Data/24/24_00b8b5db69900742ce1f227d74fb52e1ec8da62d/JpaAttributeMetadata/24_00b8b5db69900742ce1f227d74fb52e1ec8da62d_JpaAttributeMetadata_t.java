 package org.springframework.data.rest.repository.jpa;
 
 import java.beans.PropertyDescriptor;
 import java.lang.annotation.Annotation;
 import java.lang.reflect.Field;
 import java.lang.reflect.Method;
 import java.util.Collection;
 import java.util.Map;
 import java.util.Set;
 import javax.persistence.ManyToOne;
 import javax.persistence.OneToOne;
 import javax.persistence.metamodel.Attribute;
 import javax.persistence.metamodel.EntityType;
 import javax.persistence.metamodel.MapAttribute;
 import javax.persistence.metamodel.PluralAttribute;
 
 import org.springframework.beans.BeanUtils;
 import org.springframework.data.rest.repository.AttributeMetadata;
 import org.springframework.util.ReflectionUtils;
 
 /**
  * Implementation of {@link AttributeMetadata} for JPA.
  *
  * @author Jon Brisbin
  */
 public class JpaAttributeMetadata implements AttributeMetadata {
 
   private String    name;
   private Attribute attribute;
   private Class<?>  type;
   private Field     field;
   private Method    getter;
   private Method    setter;
 
   public JpaAttributeMetadata(EntityType<?> entityType, Attribute attribute) {
     this.attribute = attribute;
     name = attribute.getName();
     type = attribute.getJavaType();
 
     field = ReflectionUtils.findField(entityType.getJavaType(), name);
     ReflectionUtils.makeAccessible(field);
 
     PropertyDescriptor property = BeanUtils.getPropertyDescriptor(entityType.getJavaType(), name);
     if(null != property) {
       getter = property.getReadMethod();
       if(null != getter) {
         ReflectionUtils.makeAccessible(getter);
       }
 
       setter = property.getWriteMethod();
       if(null != setter) {
         ReflectionUtils.makeAccessible(setter);
       }
     }
   }
 
   @Override public String name() {
     return name;
   }
 
   @Override public Class<?> type() {
     return type;
   }
 
   @Override public Class<?> keyType() {
     return (attribute instanceof MapAttribute
             ? ((MapAttribute)attribute).getKeyJavaType()
             : null);
   }
 
   @Override public Class<?> elementType() {
     return (attribute instanceof PluralAttribute
             ? ((PluralAttribute)attribute).getElementType().getJavaType()
             : null);
   }
 
   @Override public boolean isNullable() {
     if(hasAnnotation(ManyToOne.class)) {
       return annotation(ManyToOne.class).optional();
     }
 
     if(hasAnnotation(OneToOne.class)) {
       return annotation(OneToOne.class).optional();
     }
 
     return true;
   }
 
   @Override public boolean isCollectionLike() {
     if(attribute instanceof PluralAttribute) {
       PluralAttribute plattr = (PluralAttribute)attribute;
       switch(plattr.getCollectionType()) {
         case COLLECTION:
         case LIST:
           return true;
         default:
           return false;
       }
     } else {
       return false;
     }
   }
 
   @Override public Collection<?> asCollection(Object target) {
     return (Collection<?>)get(target);
   }
 
   @Override public boolean isSetLike() {
     if(attribute instanceof PluralAttribute) {
       PluralAttribute plattr = (PluralAttribute)attribute;
       switch(plattr.getCollectionType()) {
         case SET:
           return true;
         default:
           return false;
       }
     } else {
       return false;
     }
   }
 
   @Override public Set<?> asSet(Object target) {
     return (Set<?>)get(target);
   }
 
   @Override public boolean isMapLike() {
     if(attribute instanceof PluralAttribute) {
       PluralAttribute plattr = (PluralAttribute)attribute;
       switch(plattr.getCollectionType()) {
         case MAP:
           return true;
         default:
           return false;
       }
     } else {
       return false;
     }
   }
 
   @Override public Map asMap(Object target) {
     return (Map)get(target);
   }
 
   @Override public boolean hasAnnotation(Class<? extends Annotation> annoType) {
     return field.isAnnotationPresent(annoType);
   }
 
   @Override public <A extends Annotation> A annotation(Class<A> annoType) {
     return field.getAnnotation(annoType);
   }
 
   @Override public Object get(Object target) {
    if(null != getter) {
      return ReflectionUtils.invokeMethod(getter, target);
    } else {
      return ReflectionUtils.getField(field, target);
     }
   }
 
   @Override public AttributeMetadata set(Object value, Object target) {
    if(null != setter) {
      ReflectionUtils.invokeMethod(setter, target, value);
    } else {
      ReflectionUtils.setField(field, target, value);
     }
     return this;
   }
 
   @Override public String toString() {
     return "JpaAttributeMetadata{" +
         "name='" + name + '\'' +
         ", attribute=" + attribute +
         ", type=" + type +
         ", field=" + field +
         ", getter=" + getter +
         ", setter=" + setter +
         '}';
   }
 
 }
