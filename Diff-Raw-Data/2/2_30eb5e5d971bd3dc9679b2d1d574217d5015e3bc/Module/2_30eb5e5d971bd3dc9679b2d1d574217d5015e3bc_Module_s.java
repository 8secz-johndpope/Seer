 /*
  * JBoss, Home of Professional Open Source.
  * Copyright 2010, Red Hat Middleware LLC, and individual contributors
  * as indicated by the @author tags. See the copyright.txt file in the
  * distribution for a full listing of individual contributors.
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
 
 package org.jboss.modules;
 
 import java.io.IOException;
 import java.lang.reflect.InvocationTargetException;
 import java.lang.reflect.Method;
 import java.lang.reflect.Modifier;
 import java.security.AccessController;
 import java.security.PrivilegedAction;
 import java.util.List;
 
 /**
  * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
  * @author <a href="mailto:jbailey@redhat.com">John Bailey</a>
  */
 public final class Module {
     static {
         AccessController.doPrivileged(new PrivilegedAction<Void>() {
             public Void run() {
                 // Set up URL handler, if it isn't already
                 final String pkgs = System.getProperty("java.protocol.handler.pkgs");
                 final String newPkgs;
                 if (pkgs == null || pkgs.length() == 0) {
                     newPkgs = "org.jboss.modules.protocol";
                     System.setProperty("java.protocol.handler.pkgs", newPkgs);
                 } else if (! pkgs.contains("org.jboss.modules.protocol")) {
                     newPkgs = pkgs + "|org.jboss.modules.protocol";
                     System.setProperty("java.protocol.handler.pkgs", newPkgs);
                 }
                 return null;
             }
         });
     }
 
     private final ModuleIdentifier identifier;
     private final List<Dependency> dependencies;
     private final ModuleContentLoader contentLoader;
     private final String mainClassName;
     private final ModuleClassLoader moduleClassLoader;
 
     Module(final ModuleSpec spec, final List<Dependency> dependencies) {
         this.identifier = spec.getIdentifier();
         this.contentLoader = spec.getContentLoader();
         mainClassName = spec.getMainClass();
         this.dependencies = dependencies;
 
         this.moduleClassLoader = new ModuleClassLoader(this, false); // TODO: Use flags to determine child first
     }
 
     public final Class<?> getExportedClass(String className) {
         try {
             return moduleClassLoader.loadClass(className);
             // TODO: Need to make sure we should export the class (Maybe use Module.forClass(class)
         } catch (ClassNotFoundException e) {
             return null;
         }
     }
 
     Class<?> getImportedClass(final String className) {
         for(Dependency dependency : dependencies) {
             final Module module = dependency.getModule();
             Class<?> importedClass = module.getExportedClass(className);
             if(importedClass != null)
                 return importedClass;
         }
         return null;
     }
 
     ClassSpec getLocalClassSpec(String className) throws IOException {
         return contentLoader.getClassSpec(className);
     }
 
     public final Resource getExportedResource(final String resourcePath) {
         return contentLoader.getResource(resourcePath);
     }
 
     public final Iterable<Resource> getExportedResources(final String resourcePath) {
         // todo filter...
         return contentLoader.getResources(resourcePath);
     }
 
     public final Resource getExportedResource(final String rootPath, final String resourcePath) {
         // todo filter...
         return contentLoader.getResource(rootPath, resourcePath);
     }
 
     public final void runMain(final String[] args) throws NoSuchMethodException, InvocationTargetException {
         try {
             final Class<?> mainClass = getExportedClass(mainClassName);
             if (mainClass == null) {
                 throw new NoSuchMethodException("No main class defined for " + this);
             }
             final Method mainMethod = mainClass.getMethod("main", String[].class);
             final int modifiers = mainMethod.getModifiers();
             if (! Modifier.isStatic(modifiers)) {
                 throw new NoSuchMethodException("Main method is not static for " + this);
             }
             // ignore the return value
            mainMethod.invoke(null, args);
         } catch (IllegalAccessException e) {
             // unexpected; should be public
             throw new IllegalAccessError(e.getMessage());
         }
     }
 
     public ModuleIdentifier getIdentifier() {
         return identifier;
     }
 
     public static enum Flag {
         // flags here
     }
 
     public String toString() {
         return "Module " + identifier;
     }
 }
