 /*
  * JBoss, Home of Professional Open Source
  * Copyright 2008, Red Hat, Inc., and individual contributors
  * by the @authors tag. See the copyright.txt in the distribution for a
  * full listing of individual contributors.
  *
  * Licensed under the Apache License, Version 2.0 (the "License");
  * you may not use this file except in compliance with the License.
  * You may obtain a copy of the License at
  * http://www.apache.org/licenses/LICENSE-2.0
  * Unless required by applicable law or agreed to in writing, software
  * distributed under the License is distributed on an "AS IS" BASIS,
  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  * See the License for the specific language governing permissions and
  * limitations under the License.
  */
 package org.jboss.weld.resolution;
 
 import static org.jboss.weld.logging.messages.BeanManagerMessage.DUPLICATE_QUALIFIERS;
 import static org.jboss.weld.logging.messages.BeanManagerMessage.INVALID_QUALIFIER;
 import static org.jboss.weld.logging.messages.ResolutionMessage.CANNOT_EXTRACT_RAW_TYPE;
 
 import java.lang.annotation.Annotation;
 import java.lang.reflect.Field;
 import java.lang.reflect.Type;
 import java.lang.reflect.TypeVariable;
 import java.util.Collection;
 import java.util.Collections;
 import java.util.HashMap;
 import java.util.HashSet;
 import java.util.Map;
 import java.util.Set;
 
 import javax.enterprise.event.Event;
 import javax.enterprise.inject.Instance;
 import javax.enterprise.inject.New;
 import javax.enterprise.inject.spi.Bean;
 import javax.enterprise.inject.spi.Decorator;
 import javax.enterprise.inject.spi.InjectionPoint;
 import javax.enterprise.inject.spi.Interceptor;
 import javax.inject.Named;
 import javax.inject.Provider;
 
import org.jboss.weld.Container;
 import org.jboss.weld.exceptions.IllegalArgumentException;
 import org.jboss.weld.literal.AnyLiteral;
 import org.jboss.weld.literal.DefaultLiteral;
 import org.jboss.weld.literal.NamedLiteral;
 import org.jboss.weld.literal.NewLiteral;
 import org.jboss.weld.manager.BeanManagerImpl;
 import org.jboss.weld.metadata.cache.MetaAnnotationStore;
 import org.jboss.weld.util.reflection.Reflections;
 
 public class ResolvableBuilder {
 
     private static final Class<?>[] FACADE_TYPES = new Class<?>[] { Event.class, Instance.class, Provider.class };
     private static final Class<?>[] METADATA_TYPES = new Class<?>[] { Interceptor.class, Decorator.class, Bean.class };
 
     protected Class<?> rawType;
     protected final Set<Type> types;
     protected final Set<Annotation> qualifiers;
     protected final Map<Class<? extends Annotation>, Annotation> mappedQualifiers;
     protected Bean<?> declaringBean;
     private final BeanManagerImpl beanManager;
 
     public ResolvableBuilder(final BeanManagerImpl beanManager) {
         this.beanManager = beanManager;
         this.types = new HashSet<Type>();
         this.qualifiers = new HashSet<Annotation>();
         this.mappedQualifiers = new HashMap<Class<? extends Annotation>, Annotation>();
     }
 
     public ResolvableBuilder(Type type, final BeanManagerImpl beanManager) {
         this(beanManager);
         if (type != null) {
             this.rawType = Reflections.getRawType(type);
             if (rawType == null || type instanceof TypeVariable<?>) {
                 throw new IllegalArgumentException(CANNOT_EXTRACT_RAW_TYPE, type);
             }
             this.types.add(type);
         }
     }
 
     public ResolvableBuilder(InjectionPoint injectionPoint, final BeanManagerImpl manager) {
         this(injectionPoint.getType(), manager);
         addQualifiers(injectionPoint.getQualifiers());
         if (mappedQualifiers.containsKey(Named.class) && injectionPoint.getMember() instanceof Field) {
             Named named = (Named) mappedQualifiers.get(Named.class);
             if (named.value().equals("")) {
                 qualifiers.remove(named);
                 // This is field injection point with an @Named qualifier, with no value specified, we need to assume the name of the field is the value
                 named = new NamedLiteral(injectionPoint.getMember().getName());
                 qualifiers.add(named);
                 mappedQualifiers.put(Named.class, named);
             }
         }
         setDeclaringBean(injectionPoint.getBean());
     }
 
     public ResolvableBuilder setDeclaringBean(Bean<?> declaringBean) {
         this.declaringBean = declaringBean;
         return this;
     }
 
     public ResolvableBuilder addType(Type type) {
         this.types.add(type);
         return this;
     }
 
     public ResolvableBuilder addTypes(Set<Type> types) {
         this.types.addAll(types);
         return this;
     }
 
     public Resolvable create() {
         if (qualifiers.size() == 0) {
             this.qualifiers.add(DefaultLiteral.INSTANCE);
         }
         for (Class<?> facadeType : FACADE_TYPES) {
             if (Reflections.isAssignableFrom(facadeType, types)) {
                 return createFacade(facadeType);
             }
         }
         for (Class<?> metadataType : METADATA_TYPES) {
             if (Reflections.isAssignableFrom(metadataType, types)) {
                 return createMetadataProvider(metadataType);
             }
         }
         return new ResolvableImpl(rawType, types, mappedQualifiers, declaringBean, qualifiers(qualifiers));
     }
 
     private Resolvable createFacade(Class<?> rawType) {
         Set<Annotation> qualifiers = Collections.<Annotation>singleton(AnyLiteral.INSTANCE);
         Set<Type> types = Collections.<Type>singleton(rawType);
         return new ResolvableImpl(rawType, types, mappedQualifiers, declaringBean, qualifiers(qualifiers));
     }
 
     // just as facade but we keep the qualifiers so that we can recognize Bean from @Intercepted Bean.
     private Resolvable createMetadataProvider(Class<?> rawType) {
         Set<Type> types = Collections.<Type>singleton(rawType);
         return new ResolvableImpl(rawType, types, mappedQualifiers, declaringBean, qualifiers(qualifiers));
     }
 
     public ResolvableBuilder addQualifier(Annotation qualifier) {
         // Handle the @New qualifier special case
         final Class<? extends Annotation> annotationType = qualifier.annotationType();
         if (annotationType.equals(New.class)) {
             New newQualifier = New.class.cast(qualifier);
             if (newQualifier.value().equals(New.class) && rawType == null) {
                 throw new IllegalStateException("Cannot transform @New when there is no known raw type");
             } else if (newQualifier.value().equals(New.class)) {
                 qualifier = new NewLiteral() {
 
                     private static final long serialVersionUID = 1L;
 
                     @Override
                     public Class<?> value() {
                         return rawType;
                     }
 
                 };
             }
         }
 
         checkQualifier(qualifier, annotationType);
         this.qualifiers.add(qualifier);
         this.mappedQualifiers.put(annotationType, qualifier);
         return this;
     }
 
     public ResolvableBuilder addQualifierIfAbsent(Annotation qualifier) {
         if (!qualifiers.contains(qualifier)) {
             addQualifier(qualifier);
         }
         return this;
     }
 
     public ResolvableBuilder addQualifiers(Annotation[] qualifiers) {
         for (Annotation qualifier : qualifiers) {
             addQualifier(qualifier);
         }
         return this;
     }
 
     public ResolvableBuilder addQualifiers(Collection<Annotation> qualifiers) {
         for (Annotation qualifier : qualifiers) {
             addQualifier(qualifier);
         }
         return this;
     }
 
     protected void checkQualifier(Annotation qualifier, Class<? extends Annotation> annotationType) {
        if (!Container.instance().services().get(MetaAnnotationStore.class).getBindingTypeModel(annotationType).isValid()) {
             throw new IllegalArgumentException(INVALID_QUALIFIER, qualifier);
         }
         if (qualifiers.contains(qualifier)) {
             throw new IllegalArgumentException(DUPLICATE_QUALIFIERS, qualifiers);
         }
     }
 
     protected static class ResolvableImpl implements Resolvable {
 
         private final Set<QualifierInstance> qualifierInstances;
         private final Map<Class<? extends Annotation>, Annotation> mappedQualifiers;
         private final Set<Type> typeClosure;
         private final Class<?> rawType;
         private final Bean<?> declaringBean;
 
         protected ResolvableImpl(Class<?> rawType, Set<Type> typeClosure, Map<Class<? extends Annotation>, Annotation> mappedQualifiers, Bean<?> declaringBean, final Set<QualifierInstance> qualifierInstances) {
             this.mappedQualifiers = mappedQualifiers;
             this.typeClosure = typeClosure;
             this.rawType = rawType;
             this.declaringBean = declaringBean;
             this.qualifierInstances = qualifierInstances;
         }
 
         public Set<QualifierInstance> getQualifiers() {
             return qualifierInstances;
         }
 
         public boolean isAnnotationPresent(Class<? extends Annotation> annotationType) {
             return mappedQualifiers.containsKey(annotationType);
         }
 
         public Set<Type> getTypes() {
             return typeClosure;
         }
 
         public boolean isAssignableTo(Class<?> clazz) {
             return Reflections.isAssignableFrom(clazz, typeClosure);
         }
 
         public <A extends Annotation> A getAnnotation(Class<A> annotationType) {
             return Reflections.<A>cast(mappedQualifiers.get(annotationType));
         }
 
         public Class<?> getJavaClass() {
             return rawType;
         }
 
         public Bean<?> getDeclaringBean() {
             return declaringBean;
         }
 
         @Override
         public String toString() {
             return "Types: " + getTypes() + "; Bindings: " + getQualifiers();
         }
 
         public int hashCode() {
             int result = 17;
             result = 31 * result + this.getTypes().hashCode();
             result = 31 * result + this.qualifierInstances.hashCode();
             return result;
         }
 
         public boolean equals(Object o) {
             if (o instanceof ResolvableImpl) {
                 ResolvableImpl r = (ResolvableImpl) o;
                 return this.getTypes().equals(r.getTypes()) && this.qualifierInstances.equals(r.qualifierInstances);
             }
             return false;
         }
     }
 
     protected Set<QualifierInstance> qualifiers(Set<Annotation> annotations) {
         if(annotations.isEmpty()) {
             return Collections.emptySet();
         }
         final MetaAnnotationStore store = beanManager.getServices().get(MetaAnnotationStore.class);
         final Set<QualifierInstance> ret = new HashSet<QualifierInstance>();
         for(Annotation a : annotations) {
             ret.add(new QualifierInstance(a, store.getBindingTypeModel(a.annotationType())));
         }
         return ret;
     }
 }
