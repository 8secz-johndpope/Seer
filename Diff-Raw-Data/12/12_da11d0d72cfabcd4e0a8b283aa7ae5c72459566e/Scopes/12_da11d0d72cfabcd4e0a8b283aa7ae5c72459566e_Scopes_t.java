 /**
  * Copyright (C) 2006 Google Inc.
  *
  * Licensed under the Apache License, Version 2.0 (the "License");
  * you may not use this file except in compliance with the License.
  * You may obtain a copy of the License at
  *
  * http://www.apache.org/licenses/LICENSE-2.0
  *
  * Unless required by applicable law or agreed to in writing, software
  * distributed under the License is distributed on an "AS IS" BASIS,
  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  * See the License for the specific language governing permissions and
  * limitations under the License.
  */
 
 package com.google.inject;
 
 import com.google.inject.util.StackTraceElements;
 import java.lang.annotation.Annotation;
 import java.util.Map;
 
 /**
  * Built in scope implementations.
  *
  * @author crazybob@google.com (Bob Lee)
  */
 public class Scopes {
 
   private Scopes() {}
 
   /**
    * One instance per {@link Injector}. Also see {@code @}{@link Singleton}.
    */
   public static final Scope SINGLETON = new Scope() {
     public <T> Provider<T> scope(Key<T> key, final Provider<T> creator) {
       return new Provider<T>() {
 
         private volatile T instance;
 
         // DCL on a volatile is safe as of Java 5, which we obviously require.
         @SuppressWarnings("DoubleCheckedLocking")
         public T get() {
           if (instance == null) {
             /*
              * Use a pretty coarse lock. We don't want to run into deadlocks
              * when two threads try to load circularly-dependent objects.
              * Maybe one of these days we will identify independent graphs of
              * objects and offer to load them in parallel.
              */
             synchronized (Injector.class) {
               if (instance == null) {
                 instance = creator.get();
               }
             }
           }
           return instance;
         }
 
         public String toString() {
          return String.format("%s[%s]", creator, SINGLETON);
         }
       };
     }
 
     public String toString() {
       return "Scopes.SINGLETON";
     }
   };
 
   /**
    * No scope; the same as not applying any scope at all.  Each time the
    * Injector obtains an instance of an object with "no scope", it injects this
    * instance then immediately forgets it.  When the next request for the same
    * binding arrives it will need to obtain the instance over again.
    *
    * <p>This exists only in case a class has been annotated with a scope
    * annotation such as {@link Singleton @Singleton}, and you need to override
    * this to "no scope" in your binding.
    */
   public static final Scope NO_SCOPE = new Scope() {
     public <T> Provider<T> scope(Key<T> key, Provider<T> unscoped) {
       return unscoped;
     }
     public String toString() {
       return "Scopes.NO_SCOPE";
     }
   };
 
   /**
    * Gets the scope for a type based on its annotations. Returns {@code null}
    * if none specified.
    *
    * @param implementation type
    * @param scopes map of scope names to scopes
    * @param errorHandler handles errors
    */
   static Scope getScopeForType(Class<?> implementation,
       Map<Class<? extends Annotation>, Scope> scopes,
       ErrorHandler errorHandler) {
     Class<? extends Annotation> found = null;
     for (Annotation annotation : implementation.getAnnotations()) {
       if (isScopeAnnotation(annotation)) {
         if (found != null) {
           errorHandler.handle(
               StackTraceElements.forType(implementation),
               ErrorMessages.DUPLICATE_SCOPE_ANNOTATIONS,
               "@" + found.getSimpleName(),
               "@" + annotation.annotationType().getSimpleName()
           );
         } else {
           found = annotation.annotationType();
         }
       }
     }
 
     return scopes.get(found);
   }
 
   static boolean isScopeAnnotation(Annotation annotation) {
     return isScopeAnnotation(annotation.annotationType());
   }
 
   static boolean isScopeAnnotation(Class<? extends Annotation> annotationType) {
     return annotationType.isAnnotationPresent(ScopeAnnotation.class);
   }
 
   /**
    * Scopes an internal factory.
    */
   static <T> InternalFactory<? extends T> scope(Key<T> key,
       InjectorImpl injector, InternalFactory<? extends T> creator,
       Scope scope) {
     // No scope does nothing.
     if (scope == null) {
       return creator;
     }
     Provider<T> scoped = scope.scope(key,
         new ProviderToInternalFactoryAdapter<T>(injector, creator));
     return new InternalFactoryToProviderAdapter<T>(scoped);
   }
 }
