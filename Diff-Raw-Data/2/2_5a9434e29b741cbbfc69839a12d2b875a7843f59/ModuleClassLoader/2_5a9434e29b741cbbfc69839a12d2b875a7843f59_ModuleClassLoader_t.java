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
 import java.io.InputStream;
 import java.lang.reflect.Method;
 import java.net.URL;
 import java.security.SecureClassLoader;
 import java.util.ArrayDeque;
 import java.util.Enumeration;
 import java.util.HashMap;
 import java.util.Iterator;
 import java.util.Map;
 import java.util.Queue;
 import java.util.Set;
 
 /**
  * @author <a href="mailto:jbailey@redhat.com">John Bailey</a>
  * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
  */
 public final class ModuleClassLoader extends SecureClassLoader {
 
     static {
         try {
             final Method method = ClassLoader.class.getMethod("registerAsParallelCapable");
             method.invoke(null);
         } catch (Exception e) {
             // ignore
         }
     }
 
     private final Module module;
     private final Set<Module.Flag> flags;
     private final Map<String, Class<?>> cache = new HashMap<String, Class<?>>(256);
 
     ModuleClassLoader(final Module module, final Set<Module.Flag> flags, final AssertionSetting setting) {
         this.module = module;
         this.flags = flags;
         if (setting != AssertionSetting.INHERIT) {
             setDefaultAssertionStatus(setting == AssertionSetting.ENABLED);
         }
     }
 
     @Override
     public Class<?> loadClass(final String name) throws ClassNotFoundException {
         return loadClass(name, false);
     }
 
     @Override
     protected Class<?> loadClass(String className, boolean resolve) throws ClassNotFoundException {
         if (className.startsWith("java.")) {
             // always delegate to super
             return super.loadClass(className, resolve);
         }
         if (Thread.holdsLock(this) && Thread.currentThread() != LoaderThreadHolder.LOADER_THREAD) {
             // Only the classloader thread may take this lock; use a condition to relinquish it
             final LoadRequest req = new LoadRequest(className, resolve, this);
             final Queue<LoadRequest> queue = LoaderThreadHolder.REQUEST_QUEUE;
             synchronized (LoaderThreadHolder.REQUEST_QUEUE) {
                 queue.add(req);
                 queue.notify();
             }
             boolean intr = false;
             try {
                 while (!req.done) try {
                     wait();
                 } catch (InterruptedException e) {
                     intr = true;
                 }
             } finally {
                 if (intr) Thread.currentThread().interrupt();
             }
 
             return req.result;
         } else {
             // no deadlock risk!  Either the lock isn't held, or we're inside the class loader thread.
             if (className == null) {
                 throw new IllegalArgumentException("name is null");
             }
             // Check if we have already loaded it..
             Class<?> loadedClass = findLoadedClass(className);
             if (loadedClass != null) {
                 return loadedClass;
             }
             final Map<String, Class<?>> cache = this.cache;
             final boolean missing;
             synchronized (this) {
                 missing = cache.containsKey(className);
                 loadedClass = cache.get(className);
             }
             if (loadedClass != null) {
                 return loadedClass;
             }
             if (missing) {
                 throw new ClassNotFoundException(className);
             }
             if (flags.contains(Module.Flag.CHILD_FIRST)) {
                 loadedClass = loadClassLocal(className);
                 if (loadedClass == null) {
                     loadedClass = module.getImportedClass(className);
                 }
                 if (loadedClass == null) try {
                     loadedClass = findSystemClass(className);
                 } catch (ClassNotFoundException e) {
                 }
             } else {
                 loadedClass = module.getImportedClass(className);
                 if (loadedClass == null) try {
                     loadedClass = findSystemClass(className);
                 } catch (ClassNotFoundException e) {
                 }
                 if (loadedClass == null) {
                     loadedClass = loadClassLocal(className);
                 }
             }
             if (loadedClass == null) {
                 if (! flags.contains(Module.Flag.NO_BLACKLIST)) {
                     synchronized (this) {
                        cache.put(className, null);
                     }
                 }
                 throw new ClassNotFoundException(className);
             }
             synchronized (this) {
                 cache.put(className, loadedClass);
             }
             if (loadedClass != null && resolve) resolveClass(loadedClass);
             return loadedClass;
         }
     }
 
     private Class<?> loadClassLocal(String name) throws ClassNotFoundException {
         // Check to see if we can load it
         ClassSpec classSpec = null;
         try {
             classSpec = module.getLocalClassSpec(name);
         } catch (IOException e) {
             throw new ClassNotFoundException(name, e);
         }
         if (classSpec == null)
             return null;
         return defineClass(name, classSpec);
     }
 
     private Class<?> defineClass(final String name, final ClassSpec classSpec) {
         // Ensure that the package is loaded
         final int lastIdx = name.lastIndexOf('.');
         if (lastIdx != -1) {
             // there's a package name; get the Package for it
             final String packageName = name.substring(0, lastIdx);
             final Package pkg = getPackage(packageName);
             if (pkg != null) {
                 // Package is defined already
                 if (pkg.isSealed() && ! pkg.isSealed(classSpec.getCodeSource().getLocation())) {
                     // use the same message as the JDK
                     throw new SecurityException("sealing violation: package " + packageName + " is sealed");
                 }
             } else {
                 final PackageSpec spec;
                 try {
                     spec = getModule().getLocalPackageSpec(name);
                     definePackage(name, spec);
                 } catch (IOException e) {
                     definePackage(name, null);
                 }
             }
         }
         final Class<?> newClass = defineClass(name, classSpec.getBytes(), 0, classSpec.getBytes().length, classSpec.getCodeSource());
         final AssertionSetting setting = classSpec.getAssertionSetting();
         if (setting != AssertionSetting.INHERIT) {
             setClassAssertionStatus(name, setting == AssertionSetting.ENABLED);
         }
         return newClass;
     }
 
     private Package definePackage(final String name, final PackageSpec spec) {
         if (spec == null) {
             return definePackage(name, null, null, null, null, null, null, null);
         } else {
             final Package pkg = definePackage(name, spec.getSpecTitle(), spec.getSpecVersion(), spec.getSpecVendor(), spec.getImplTitle(), spec.getImplVersion(), spec.getImplVendor(), spec.getSealBase());
             final AssertionSetting setting = spec.getAssertionSetting();
             if (setting != AssertionSetting.INHERIT) {
                 setPackageAssertionStatus(name, setting == AssertionSetting.ENABLED);
             }
             return pkg;
         }
     }
 
     @Override
     protected String findLibrary(final String libname) {
         return module.getLocalLibrary(libname);
     }
 
     @Override
     public URL getResource(String name) {
         return module.getExportedResource(name).getURL();
     }
 
     @Override
     public Enumeration<URL> getResources(String name) throws IOException {
         final Iterable<Resource> resources = module.getExportedResources(name);
         final Iterator<Resource> iterator = resources.iterator();
 
         return new Enumeration<URL>() {
             @Override
             public boolean hasMoreElements() {
                 return iterator.hasNext();
             }
 
             @Override
             public URL nextElement() {
                 return iterator.next().getURL();
             }
         };
     }
 
     @Override
     public InputStream getResourceAsStream(final String name) {
         try {
             return module.getExportedResource(name).openStream();
         } catch (IOException e) {
             return null;
         }
     }
 
     /**
      * Get the module for this class loader.
      *
      * @return the module
      */
     public Module getModule() {
         return module;
     }
 
     private static final class LoaderThreadHolder {
 
         private static final Thread LOADER_THREAD;
         private static final Queue<LoadRequest> REQUEST_QUEUE = new ArrayDeque<LoadRequest>();
 
         static {
             Thread thr = new LoaderThread();
             thr.setName("Module ClassLoader Thread");
             // This thread will always run as long as the VM is alive.
             thr.setDaemon(true);
             thr.start();
             LOADER_THREAD = thr;
         }
 
         private LoaderThreadHolder() {
         }
     }
 
     static class LoadRequest {
         private final String className;
         private final ModuleClassLoader requester;
         private Class<?> result;
         private boolean resolve;
         private boolean done;
 
         public LoadRequest(final String className, final boolean resolve, final ModuleClassLoader requester) {
             this.className = className;
             this.resolve = resolve;
             this.requester = requester;
         }
     }
 
     static class LoaderThread extends Thread {
         @Override
         public void run() {
             final Queue<LoadRequest> queue = LoaderThreadHolder.REQUEST_QUEUE;
             for (; ;) {
                 try {
                     LoadRequest request;
                     synchronized (queue) {
                         while ((request = queue.poll()) == null) {
                             try {
                                 queue.wait();
 
                             } catch (InterruptedException e) {
                             }
                         }
                     }
 
                     final ModuleClassLoader loader = request.requester;
                     Class<?> result = null;
                     synchronized (loader) {
                         try {
                             result = loader.loadClass(request.className, request.resolve);
                         }
                         finally {
                             // no matter what, the requester MUST be notified
                             request.result = result;
                             request.done = true;
                             loader.notifyAll();
                         }
                     }
                 } catch (Throwable t) {
                     // ignore
                 }
             }
         }
     }
 }
