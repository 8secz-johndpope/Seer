 /*
  * 
  * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
  * 
  * Copyright 2007-2008 Sun Microsystems, Inc. All rights reserved.
  * 
  * The contents of this file are subject to the terms of either the GNU
  * General Public License Version 2 only ("GPL") or the Common Development
  * and Distribution License("CDDL") (collectively, the "License").  You
  * may not use this file except in compliance with the License. You can obtain
  * a copy of the License at https://glassfish.dev.java.net/public/CDDL+GPL.html
  * or glassfish/bootstrap/legal/LICENSE.txt.  See the License for the specific
  * language governing permissions and limitations under the License.
  * 
  * When distributing the software, include this License Header Notice in each
  * file and include the License file at glassfish/bootstrap/legal/LICENSE.txt.
  * Sun designates this particular file as subject to the "Classpath" exception
  * as provided by Sun in the GPL Version 2 section of the License file that
  * accompanied this code.  If applicable, add the following below the License
  * Header, with the fields enclosed by brackets [] replaced by your own
  * identifying information: "Portions Copyrighted [year]
  * [name of copyright owner]"
  * 
  * Contributor(s):
  * 
  * If you wish your version of this file to be governed by only the CDDL or
  * only the GPL Version 2, indicate your decision by adding "[Contributor]
  * elects to include this software in this distribution under the [CDDL or GPL
  * Version 2] license."  If you don't indicate a single choice of license, a
  * recipient has the option to distribute your version of this file under
  * either the CDDL, the GPL Version 2 or to extend the choice of license to
  * its licensees as provided above.  However, if you add GPL Version 2 code
  * and therefore, elected the GPL Version 2 license, then the option applies
  * only if the new code is made subject to such option by the copyright
  * holder.
  */
 package com.sun.hk2.component;
 
 import org.jvnet.hk2.annotations.Extract;
 import org.jvnet.hk2.annotations.Scoped;
 import org.jvnet.hk2.component.ComponentException;
 import org.jvnet.hk2.component.Habitat;
 import org.jvnet.hk2.component.Inhabitant;
 import org.jvnet.hk2.component.MultiMap;
 import org.jvnet.hk2.component.PerLookup;
 import org.jvnet.hk2.component.Scope;
 import org.jvnet.hk2.component.Singleton;
 
 import java.lang.reflect.Field;
 import java.lang.reflect.InvocationTargetException;
 import java.lang.reflect.Method;
 
 /**
  * Creates an object from its constructor.
  * @author Kohsuke Kawaguchi
  */
 public class ConstructorWomb<T> extends AbstractWombImpl<T> {
     private final Habitat habitat;
     private final ScopeInstance singletonScope;
 
 
     public ConstructorWomb(Class<T> type, Habitat habitat, MultiMap<String,String> metadata) {
         super(type,metadata);
         this.habitat = habitat;
         singletonScope = habitat.singletonScope;
     }
 
     public T create(Inhabitant onBehalfOf) throws ComponentException {
         try {
             return type.newInstance();
         } catch (InstantiationException e) {
             throw new ComponentException("Failed to create "+type,e);
         } catch (IllegalAccessException e) {
             throw new ComponentException("Failed to create "+type,e);
         } catch (LinkageError e) {
             throw new ComponentException("Failed to create "+type,e);
         }
     }
 
 
    public void initialize(T t, Inhabitant onBehalfOf) throws ComponentException {
 
         Scoped scoped = t.getClass().getAnnotation(Scoped.class);
        ScopeInstance si = (scoped==null?singletonScope:getScope(scoped));

        inject(habitat, t, onBehalfOf);
 
         if(si!=null)
             // extraction amounts to no-op if this is prototype scope. so skip that.
             extract(t, si);
 
     }
 
     /**
      * Extracts resources identified with {@link org.jvnet.hk2.annotations.Extract} annotations
      * into the given scope.
      *
      * <p>
      * This method is for the use within HK2, and not really meant
      * to be used by application code.
      *
      * @param component the component we should extract resources from
      * @throws ComponentException if the resource extract fail
      */
     public void extract(Object component, ScopeInstance si) throws ComponentException {
         Class<?> currentClass = component.getClass();
         while (!currentClass.equals(Object.class)) {
             for (Field field : currentClass.getDeclaredFields()) {
                 Extract extract = field.getAnnotation(Extract.class);
                 if (extract == null)    continue;
 
                 try {
                     field.setAccessible(true);
                     Object value = field.get(component);
                     Class<?> type = field.getType();
 
 //                    if (LOGGER.isLoggable(Level.FINER)) {
 //                        LOGGER.log(Level.FINER, "Extracting resource " + value + " returned from " + field);
 //                    }
                     if (value!=null) {
                         extractValue(value, si, extract.name(), type);
                     } else {
 //                        if (LOGGER.isLoggable(Level.FINE)) {
 //                            LOGGER.log(Level.FINE, "Resource returned from " + field + " is null");
 //                        }
                     }
                 } catch (IllegalArgumentException ex) {
                     throw new ComponentException("Extraction failed on " + field, ex);
 
                 } catch (IllegalAccessException ex) {
                     throw new ComponentException("Extraction failed on " + field, ex);
                 }
             }
             for (Method method : currentClass.getDeclaredMethods()) {
                 Extract extract = method.getAnnotation(Extract.class);
                 if (extract == null)    continue;
 
                 Class<?> type = method.getReturnType();
                 if (type == null) {
                     throw new ComponentException("Extraction failed : %s has a void return type",method);
                 }
                 if (method.getParameterTypes().length > 0) {
                     throw new ComponentException("Extraction failed : %s takes parameters, it should not",method);
                 }
                 try {
                     method.setAccessible(true);
                     Object value = method.invoke(component);
 //                    if (LOGGER.isLoggable(Level.FINER)) {
 //                        LOGGER.log(Level.FINER, "Extracting resource " + value + " returned from " + method);
 //                    }
                     if (value!=null) {
                         extractValue(value, si, extract.name(), type);
                     } else {
 //                        if (LOGGER.isLoggable(Level.FINE)) {
 //                            LOGGER.log(Level.FINE, "Resource returned from " + method + " is null");
 //                        }
                     }
                 } catch (IllegalArgumentException ex) {
                     throw new ComponentException("Extraction failed on " + method.toGenericString(), ex);
                 } catch (InvocationTargetException ex) {
                     throw new ComponentException("Extraction failed on " + method.toGenericString(), ex);
                 } catch (IllegalAccessException ex) {
                     throw new ComponentException("Extraction failed on " + method.toGenericString(), ex);
                 }
             }
             currentClass = currentClass.getSuperclass();
         }
     }
 
     @SuppressWarnings("unchecked")
     private void extractValue(Object value, ScopeInstance si, String name,  Class type) {
         if(value instanceof Iterable) {
             for (Object o : (Iterable)value) {
                 extractSingleValue(o,name, type, si);
             }
         } else
         if (type.isArray()) {
             Object[] values = (Object[]) value;
             for (Object o : values) {
                 extractSingleValue(o,name, type, si);
             }
         } else {
             extractSingleValue(value, name, type, si);
         }
     }
 
     private <T> void extractSingleValue(T o, String name, Class type, final ScopeInstance si) {
         // TODO: name support. Wouldn't it be nice if Map<String,Object> extracts to named objects?
         // or recognize the "Named" interface?
         Inhabitant<T> i = new AbstractWombImpl<T>((Class<T>)o.getClass(),MultiMap.<String,String>emptyMap()) {
             public T create(Inhabitant onBehalfOf) throws ComponentException {
                 // only look at objects already available in the scope.
                 // TODO: this obviously can return null. think about how to reconcile this semantics with the rest
                 return si.get(this);
             }
         };
         if (name==null) {
             habitat.add(i);
         } else {
             habitat.addIndex(i, type.getName(), name);
         }
         si.put(i,o);
     }
 
     /**
      * Determines the {@link ScopeInstance} that stores the component.
      *
      * @return
      *      null for prototype scope. (Note that in {@link Scope#current()}
      *      null return value is an error.)
      */
     private ScopeInstance getScope(Scoped svc) throws ComponentException {
         Class<? extends Scope> s = svc.value();
         // for performance reason and to avoid infinite recursion,
         // recognize these two fundamental built-in scopes and process them differently.
         if(s==Singleton.class)
             return singletonScope;
         if(s==PerLookup.class)
             return null;
 
         // for all the other scopes, including user-defined ones.
         Scope scope = habitat.getByType(s);
         ScopeInstance si = scope.current();
         if(si==null) // scope is an extension point, so beware for broken implementations
             throw new ComponentException(scope+" returned null from the current() method");
         return si;
     }
     
 }
