 /*
  * See the NOTICE file distributed with this work for additional
  * information regarding copyright ownership.
  *
  * This is free software; you can redistribute it and/or modify it
  * under the terms of the GNU Lesser General Public License as
  * published by the Free Software Foundation; either version 2.1 of
  * the License, or (at your option) any later version.
  *
  * This software is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
  * Lesser General Public License for more details.
  *
  * You should have received a copy of the GNU Lesser General Public
  * License along with this software; if not, write to the Free
  * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
  * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
  */
 package org.xwiki.component.annotation;
 
 import java.io.BufferedReader;
 import java.io.IOException;
 import java.io.InputStream;
 import java.io.InputStreamReader;
 import java.lang.annotation.Annotation;
 import java.net.URL;
 import java.util.ArrayList;
 import java.util.Arrays;
 import java.util.Enumeration;
 import java.util.HashMap;
 import java.util.LinkedHashSet;
 import java.util.List;
 import java.util.Map;
 import java.util.Set;
 
 import javax.inject.Provider;
 
 import org.slf4j.Logger;
 import org.slf4j.LoggerFactory;
 import org.xwiki.component.descriptor.ComponentDescriptor;
 import org.xwiki.component.internal.RoleHint;
 import org.xwiki.component.manager.ComponentManager;
 
 /**
  * Dynamically loads all components defined using Annotations and declared in META-INF/components.txt files.
  * 
  * @version $Id$
  * @since 1.8.1
  */
 public class ComponentAnnotationLoader
 {
     /**
      * Location in the classloader of the file defining the list of component implementation class to parser for
      * annotations.
      */
     public static final String COMPONENT_LIST = "META-INF/components.txt";
 
     /**
      * Location in the classloader of the file specifying which component implementation to use when several components
      * with the same role/hint are found.
      *
      * @deprecated starting with 3.3M1 use the notion of priorities instead (see {@link ComponentDeclaration}).
      */
     public static final String COMPONENT_OVERRIDE_LIST = "META-INF/component-overrides.txt";
 
     /**
      * The encoding used to parse component list files.
      */
     private static final String COMPONENT_LIST_ENCODING = "UTF-8";
 
     /**
      * Default priorities under which components are registered when no priority is defined in COMPONENT_LIST.
      */
     private static final int COMPONENT_DEFAULT_PRIORITY = 1000;
 
     /**
      * Factory to create a Component Descriptor from an annotated class.
      */
     private ComponentDescriptorFactory factory = new ComponentDescriptorFactory();
 
     /**
      * Logger to use for logging...
      */
     private static final Logger LOGGER = LoggerFactory.getLogger(ComponentAnnotationLoader.class);
 
     /**
      * Loads all components defined using annotations.
      * 
      * @param manager the component manager to use to dynamically register components
      * @param classLoader the classloader to use to look for the Component list declaration file (
      *            {@code META-INF/components.txt})
      */
     public void initialize(ComponentManager manager, ClassLoader classLoader)
     {
         try {
             // Find all declared components by retrieving the list defined in COMPONENT_LIST.
             List<ComponentDeclaration> componentDeclarations = getDeclaredComponents(classLoader, COMPONENT_LIST);
 
             // Find all the Component overrides and adds them to the bottom of the list as component declarations with
             // the highest priority of 0. This is purely for backward compatibility since the override files is now
             // deprecated.
             List<ComponentDeclaration> componentOverrideDeclarations = getDeclaredComponents(classLoader,
                 COMPONENT_OVERRIDE_LIST);
             for (ComponentDeclaration componentOverrideDeclaration : componentOverrideDeclarations) {
                 componentDeclarations.add(
                     new ComponentDeclaration(componentOverrideDeclaration.getImplementationClassName(), 0));
             }
 
             initialize(manager, classLoader, componentDeclarations);
         } catch (Exception e) {
             // Make sure we make the calling code fail in order to fail fast and prevent the application to start
             // if something is amiss.
             throw new RuntimeException("Failed to get the list of components to load", e);
         }
     }
 
     /**
      * @param manager the component manager to use to dynamically register components
      * @param classLoader the classloader to use to look for the Component list declaration file (
      *            {@code META-INF/components.txt})
      * @param componentDeclarations the declarations of components to register
      * @since 3.3M1
      */
     public void initialize(ComponentManager manager, ClassLoader classLoader,
         List<ComponentDeclaration> componentDeclarations)
     {
         try {
             // 2) For each component class name found, load its class and use introspection to find the necessary
             // annotations required to create a Component Descriptor.
             Map<RoleHint, ComponentDescriptor> descriptorMap = new HashMap<RoleHint, ComponentDescriptor>();
             Map<RoleHint, Integer> priorityMap = new HashMap<RoleHint, Integer>();
 
             for (ComponentDeclaration componentDeclaration : componentDeclarations) {
                 Class< ? > componentClass = classLoader.loadClass(componentDeclaration.getImplementationClassName());
 
                 // Look for ComponentRole annotations and register one component per ComponentRole found
                 for (Class< ? > componentRoleClass : findComponentRoleClasses(componentClass)) {
                     for (ComponentDescriptor descriptor : this.factory.createComponentDescriptors(componentClass,
                         componentRoleClass))
                     {
                         // If there's already a existing role/hint in the list of descriptors then decide which one
                         // to keep by looking at their priorities. Highest priority wins (i.e. lowest integer value).
                         RoleHint roleHint = new RoleHint(componentRoleClass, descriptor.getRoleHint());
                         if (descriptorMap.containsKey(roleHint)) {
                             // Compare priorites
                             int currentPriority = priorityMap.get(roleHint);
                             if (componentDeclaration.getPriority() < currentPriority) {
                                 // Override!
                                 descriptorMap.put(roleHint, descriptor);
                                 priorityMap.put(roleHint, componentDeclaration.getPriority());
                             } else if (componentDeclaration.getPriority() == currentPriority) {
                                 // Warning that we're not overwriting since they have the same priorities
                                 LOGGER.warn("Component [{}] which implements [{}] tried to overwrite component [{}]."
                                     + "However, no action was taken since both components have the same priority "
                                     + "level of [{}].", new Object[] {componentDeclaration.getImplementationClassName(),
                                    roleHint, descriptorMap.get(roleHint).getImplementation().getName()});
                             } else {
                                 LOGGER.debug("Ignored component [{}] since its priority level of [{}] is lower than "
                                     + "the currently registered component [{}] which has a priority of [{}]",
                                     new Object[] {componentDeclaration.getImplementationClassName(),
                                     componentDeclaration.getPriority(), currentPriority});
                             }
                         } else {
                             descriptorMap.put(roleHint, descriptor);
                             priorityMap.put(roleHint, componentDeclaration.getPriority());
                         }
                     }
                 }
             }
 
             // 3) Activate all component descriptors
             for (ComponentDescriptor descriptor : descriptorMap.values()) {
                 manager.registerComponent(descriptor);
             }
 
         } catch (Exception e) {
             // Make sure we make the calling code fail in order to fail fast and prevent the application to start
             // if something is amiss.
             throw new RuntimeException("Failed to dynamically load components with annotations", e);
         }
     }
 
     public List<ComponentDescriptor> getComponentsDescriptors(Class< ? > componentClass)
     {
         List<ComponentDescriptor> descriptors = new ArrayList<ComponentDescriptor>();
 
         // Look for ComponentRole annotations and register one component per ComponentRole found
         for (Class< ? > componentRoleClass : findComponentRoleClasses(componentClass)) {
             descriptors.addAll(this.factory.createComponentDescriptors(componentClass, componentRoleClass));
         }
 
         return descriptors;
     }
 
     /**
      * Finds the interfaces that implement component roles by looking recursively in all interfaces of the passed
      * component implementation class. If the roles annotation value is specified then use the specified list instead of
      * doing auto-discovery. Also note that we support component classes implementing JSR 330's
      * {@link javax.inject.Provider} (and thus without a component role annotation).
      * 
      * @param componentClass the component implementation class for which to find the component roles it implements
      * @return the list of component role classes implemented
      */
     public Set<Class< ? >> findComponentRoleClasses(Class< ? > componentClass)
     {
         // Note: We use a Set to ensure that we don't register duplicate roles.
         Set<Class< ? >> classes = new LinkedHashSet<Class< ? >>();
 
         Component component = componentClass.getAnnotation(Component.class);
         if (component != null && component.roles().length > 0) {
             classes.addAll(Arrays.asList(component.roles()));
         } else {
             // Look in both superclass and interfaces for @ComponentRole or javax.inject.Provider
             for (Class< ? > interfaceClass : componentClass.getInterfaces()) {
                 // Handle superclass of interfaces
                 classes.addAll(findComponentRoleClasses(interfaceClass));
                 // Handle interfaces directly declared in the passed component class
                 for (Annotation annotation : interfaceClass.getDeclaredAnnotations()) {
                     if (annotation.annotationType().getName().equals(ComponentRole.class.getName())) {
                         classes.add(interfaceClass);
                     }
                 }
                 // Handle javax.inject.Provider
                 if (Provider.class.isAssignableFrom(interfaceClass)) {
                     classes.add(interfaceClass);
                 }
             }
 
             // Note that we need to look into the superclass since the super class can itself implements an interface
             // that has the @ComponentRole annotation.
             Class< ? > superClass = componentClass.getSuperclass();
             if (superClass != null && !superClass.getName().equals(Object.class.getName())) {
                 classes.addAll(findComponentRoleClasses(superClass));
             }
 
         }
 
         return classes;
     }
 
     /**
      * Get all components listed in the passed resource file.
      * 
      * @param classLoader the classloader to use to find the resources
      * @param location the name of the resources to look for
      * @return the list of component implementation class names
      * @throws IOException in case of an error loading the component list resource
      * @since 3.3M1
      */
     private List<ComponentDeclaration> getDeclaredComponents(ClassLoader classLoader, String location)
         throws IOException
     {
         List<ComponentDeclaration> annotatedClassNames = new ArrayList<ComponentDeclaration>();
         Enumeration<URL> urls = classLoader.getResources(location);
         while (urls.hasMoreElements()) {
             URL url = urls.nextElement();
 
             InputStream componentListStream = url.openStream();
 
             try {
                 annotatedClassNames.addAll(getDeclaredComponents(componentListStream));
             } finally {
                 componentListStream.close();
             }
         }
 
         return annotatedClassNames;
     }
 
     /**
      * Get all components listed in the passed resource stream. The format is:
      * {@code (priority level):(fully qualified component implementation name)}.
      *
      * @param componentListStream the stream to parse
      * @return the list of component declaration (implementation class names and priorities)
      * @throws IOException in case of an error loading the component list resource
      * @since 3.3M1
      */
     public List<ComponentDeclaration> getDeclaredComponents(InputStream componentListStream) throws IOException
     {
         List<ComponentDeclaration> annotatedClassNames = new ArrayList<ComponentDeclaration>();
 
         // Read all components definition from the URL
         // Always force UTF-8 as the encoding, since these files are read from the official jars, and those are
         // generated on an 8-bit system.
         BufferedReader in = new BufferedReader(new InputStreamReader(componentListStream, COMPONENT_LIST_ENCODING));
         String inputLine;
         while ((inputLine = in.readLine()) != null) {
             // Make sure we don't add empty lines
             if (inputLine.trim().length() > 0) {
                 try {
                     String[] chunks = inputLine.split(":");
                     if (chunks.length > 1) {
                         annotatedClassNames.add(new ComponentDeclaration(chunks[1], Integer.parseInt(chunks[0])));
                     } else {
                         annotatedClassNames.add(new ComponentDeclaration(chunks[0], COMPONENT_DEFAULT_PRIORITY));
                     }
                 } catch (Exception e) {
                     LOGGER.error("Failed to parse component declaration from [{}]", inputLine, e);
                 }
             }
         }
 
         return annotatedClassNames;
     }
 }
