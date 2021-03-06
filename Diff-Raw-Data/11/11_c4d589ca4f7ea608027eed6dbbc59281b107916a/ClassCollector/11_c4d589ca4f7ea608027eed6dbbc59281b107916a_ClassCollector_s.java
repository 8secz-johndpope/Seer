 package com.vandalsoftware.tools.classfile;
 
 import java.io.File;
 import java.io.FilenameFilter;
 import java.io.IOException;
 import java.util.Collection;
 import java.util.Collections;
 import java.util.HashMap;
 import java.util.HashSet;
 import java.util.LinkedList;
 import java.util.Map;
 
 /**
  * This class collects class files. Once classes have been collected, the methods for analysis can
  * be used: {@link #subclasses(String)}, {@link #usages(String)}, etc.
  *
  * <ol>
  *     <li>collect</li>
  *     <li>usages, subclasses, etc.</li>
  * </ol>
  * @author Jonathan Le
  */
 public class ClassCollector {
     private final ClassFileReader classFileReader = new ClassFileReader();
     private HashMap<File, ClassInfo> infoMap;
 
     public ClassCollector() {
         this.infoMap = new HashMap<>();
     }
 
     static Collection<File> listFiles(File rootDir) {
         if (!rootDir.isDirectory()) {
             return Collections.emptyList();
         }
         final LinkedList<File> dirs = new LinkedList<>();
         dirs.add(rootDir);
         final HashSet<File> fileSet = new HashSet<>();
         final ClassFileFilter filter = new ClassFileFilter();
         do {
             final File dir = dirs.removeFirst();
             if (dir.exists() && dir.isDirectory()) {
                 final File[] files = dir.listFiles(filter);
                 for (File f : files) {
                     if (f.isDirectory()) {
                         dirs.add(f);
                     }
                     fileSet.add(f);
                 }
             }
         } while (!dirs.isEmpty());
         return fileSet;
     }
 
     /**
     * Collect class file.
      */
     public ClassInfo collectFile(File f) {
         if (f.isFile()) {
             ClassInfo info = this.infoMap.get(f);
             if (info == null) {
                 try {
                     info = classFileReader.readFile(f);
                     this.infoMap.put(f, info);
                 } catch (IOException e) {
                     e.printStackTrace();
                 }
             }
             return info;
         }
         return null;
     }
 
     /**
     * Collect class files in a directory.
      */
     public void collect(File dir) {
         final Collection<File> files = listFiles(dir);
         for (File f : files) {
             collectFile(f);
         }
     }
 
     /**
      * Collect class files in a path.
      */
     public void collect(String path) {
         collect(new File(path));
     }
 
     /**
     * Check for usages of a single class name.
      *
      * @see #collect(java.io.File)
      * @see #collect(String)
      * @see #collectFile(java.io.File)
      */
     public File[] usages(String className) {
         final HashSet<File> usages = new HashSet<>();
         for (Map.Entry<File, ClassInfo> entry : this.infoMap.entrySet()) {
             if (entry.getValue().check(className)) {
                 usages.add(entry.getKey());
             }
         }
         return usages.toArray(new File[usages.size()]);
     }
 
     /**
     * Check for usages of collection of class names.
      *
      * @see #collect(java.io.File)
      * @see #collect(String)
      * @see #collectFile(java.io.File)
      */
     public File[] usages(Collection<String> classNames) {
         final HashSet<File> usages = new HashSet<>();
         for (String className : classNames) {
             for (Map.Entry<File, ClassInfo> entry : this.infoMap.entrySet()) {
                 final ClassInfo info = entry.getValue();
                 final File file = entry.getKey();
                 if (info.check(className)) {
                     usages.add(file);
                 }
             }
         }
         return usages.toArray(new File[usages.size()]);
     }
 
     /**
      * @return collected subclasses for a class given its name
      * @see #collect(java.io.File)
      * @see #collect(String)
      * @see #collectFile(java.io.File)
      */
     public Collection<String> subclasses(String className) {
         final HashSet<String> subclasses = new HashSet<>();
         for (ClassInfo info : this.infoMap.values()) {
             if (info.superClassName.equals(className)) {
                 subclasses.add(info.thisClassName);
             }
         }
         return subclasses;
     }
 
     private static class ClassFileFilter implements FilenameFilter {
         @Override
         public boolean accept(File file, String s) {
             final File f = new File(file, s);
             return f.isDirectory() || s.endsWith(".class");
         }
     }
 }
