 /*
  * Copyright 2007 Google Inc.
  * 
  * Licensed under the Apache License, Version 2.0 (the "License"); you may not
  * use this file except in compliance with the License. You may obtain a copy of
  * the License at
  * 
  * http://www.apache.org/licenses/LICENSE-2.0
  * 
  * Unless required by applicable law or agreed to in writing, software
  * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
  * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
  * License for the specific language governing permissions and limitations under
  * the License.
  */
 package com.google.gwt.dev.util;
 
 import com.google.gwt.core.ext.TreeLogger;
 import com.google.gwt.core.ext.UnableToCompleteException;
 import com.google.gwt.core.ext.typeinfo.TypeOracle;
 import com.google.gwt.util.tools.Utility;
 
 import org.w3c.dom.Attr;
 import org.w3c.dom.DOMException;
 import org.w3c.dom.Document;
 import org.w3c.dom.Element;
 import org.w3c.dom.NamedNodeMap;
 import org.w3c.dom.Node;
 import org.w3c.dom.Text;
 
 import java.io.BufferedReader;
 import java.io.BufferedWriter;
 import java.io.File;
 import java.io.FileInputStream;
 import java.io.FileNotFoundException;
 import java.io.FileOutputStream;
 import java.io.FilenameFilter;
 import java.io.IOException;
 import java.io.InputStream;
 import java.io.InputStreamReader;
 import java.io.OutputStream;
 import java.io.OutputStreamWriter;
 import java.io.PrintWriter;
 import java.io.RandomAccessFile;
 import java.io.Reader;
 import java.io.StringWriter;
 import java.io.UnsupportedEncodingException;
 import java.lang.reflect.Array;
 import java.lang.reflect.InvocationTargetException;
 import java.lang.reflect.Method;
 import java.net.MalformedURLException;
 import java.net.URL;
 import java.net.URLConnection;
 import java.net.URLDecoder;
 import java.security.MessageDigest;
 import java.security.NoSuchAlgorithmException;
 import java.util.Collection;
 import java.util.Iterator;
 import java.util.jar.JarEntry;
 
 /**
  * A smattering of useful methods. Methods in this class are candidates for
  * being moved to {@link com.google.gwt.util.tools.Utility} if they would be
  * generally useful to tool writers, and don't involve TreeLogger.
  */
 public final class Util {
 
   public static String DEFAULT_ENCODING = "UTF-8";
 
   public static final File[] EMPTY_ARRAY_FILE = new File[0];
 
   public static final String[] EMPTY_ARRAY_STRING = new String[0];
 
   public static char[] HEX_CHARS = new char[] {
       '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D',
       'E', 'F'};
 
   public static <T> void addAll(Collection<T> c, T[] a) {
     for (T val : a) {
       c.add(val);
     }
   }
 
   public static byte[] append(byte[] xs, byte x) {
     int n = xs.length;
     byte[] t = new byte[n + 1];
     System.arraycopy(xs, 0, t, 0, n);
     t[n] = x;
     return t;
   }
 
   public static <T> T[] append(T[] xs, T x) {
     int n = xs.length;
     T[] t = (T[]) Array.newInstance(xs.getClass().getComponentType(), n + 1);
     System.arraycopy(xs, 0, t, 0, n);
     t[n] = x;
     return t;
   }
 
   public static <T> T[] append(T[] appendToThis, T[] these) {
     if (appendToThis == null) {
       throw new NullPointerException("attempt to append to a null array");
     }
 
     if (these == null) {
       throw new NullPointerException("attempt to append a null array");
     }
 
     T[] result;
     int newSize = appendToThis.length + these.length;
     Class<?> componentType = appendToThis.getClass().getComponentType();
     result = (T[]) Array.newInstance(componentType, newSize);
     System.arraycopy(appendToThis, 0, result, 0, appendToThis.length);
     System.arraycopy(these, 0, result, appendToThis.length, these.length);
     return result;
   }
 
   /**
    * Computes the MD5 hash for the specified byte array.
    * 
    * @return a big fat string encoding of the MD5 for the content, suitably
    *         formatted for use as a file name
    */
   public static String computeStrongName(byte[] content) {
     MessageDigest md5;
     try {
       md5 = MessageDigest.getInstance("MD5");
     } catch (NoSuchAlgorithmException e) {
       throw new RuntimeException("Error initializing MD5", e);
     }
 
     md5.update(content);
     return toHexString(md5.digest());
   }
 
   public static boolean copy(TreeLogger logger, File in, File out)
       throws UnableToCompleteException {
     FileInputStream fis = null;
     try {
       if (in.lastModified() > out.lastModified()) {
         fis = new FileInputStream(in);
         copy(logger, fis, out);
         return true;
       } else {
         return false;
       }
     } catch (FileNotFoundException e) {
       logger.log(TreeLogger.ERROR, "Unable to open file '"
           + in.getAbsolutePath() + "'", e);
       throw new UnableToCompleteException();
     } finally {
       Utility.close(fis);
     }
   }
 
   public static void copy(TreeLogger logger, InputStream is, File out)
       throws UnableToCompleteException {
     FileOutputStream fos = null;
     try {
       out.getParentFile().mkdirs();
       fos = new FileOutputStream(out);
       copy(logger, is, fos);
     } catch (FileNotFoundException e) {
       logger.log(TreeLogger.ERROR, "Unable to create file '"
           + out.getAbsolutePath() + "'", e);
       throw new UnableToCompleteException();
     } finally {
       Utility.close(fos);
     }
   }
 
   public static void copy(TreeLogger logger, InputStream is, OutputStream os)
       throws UnableToCompleteException {
     try {
       byte[] buf = new byte[8 * 1024];
       int i = 0;
       while ((i = is.read(buf)) != -1) {
         os.write(buf, 0, i);
       }
     } catch (IOException e) {
       logger.log(TreeLogger.ERROR, "Error during copy", e);
       throw new UnableToCompleteException();
     }
   }
 
   public static boolean copy(TreeLogger logger, URL in, File out)
       throws UnableToCompleteException {
     InputStream is = null;
     try {
       URLConnection conn = in.openConnection();
       if (conn.getLastModified() > out.lastModified()) {
         is = in.openStream();
         copy(logger, is, out);
         return true;
       } else {
         return false;
       }
     } catch (IOException e) {
       logger.log(TreeLogger.ERROR, "Unable to open '" + in.toExternalForm()
           + "'", e);
       throw new UnableToCompleteException();
     } finally {
       Utility.close(is);
     }
   }
 
   public static Reader createReader(TreeLogger logger, URL url)
       throws UnableToCompleteException {
     try {
       return new InputStreamReader(url.openStream());
     } catch (IOException e) {
       logger.log(TreeLogger.ERROR, "Unable to open resource: " + url, e);
       throw new UnableToCompleteException();
     }
   }
 
   public static void deleteFilesInDirectory(File dir) {
     File[] files = dir.listFiles();
     if (files != null) {
       for (int i = 0; i < files.length; i++) {
         File file = files[i];
         if (file.isFile()) {
           file.delete();
         }
       }
     }
   }
 
   /**
    * Deletes all files have the same base name as the specified file.
    */
   public static void deleteFilesStartingWith(File dir, final String prefix) {
     File[] toDelete = dir.listFiles(new FilenameFilter() {
       public boolean accept(File dir, String name) {
         return name.startsWith(prefix);
       }
     });
 
     if (toDelete != null) {
       for (int i = 0; i < toDelete.length; i++) {
         toDelete[i].delete();
       }
     }
   }
 
   /**
    * Converts a URL "jar:file:aaa.jar!bbb" to the filename "aaa.jar". This also
    * does URLDecoding if needed.
    * 
    * @param location the URL pointing at a jar location
    * @return the path to the jar file
    */
   public static String findFileName(String location) {
     String newLocation = location;
     if ((location.startsWith("zip:file:"))
         || (location.startsWith("jar:file:"))) {
       int lastIndexOfExclam = newLocation.lastIndexOf('!');
       // This file lives in a jar or zipfile
       // we pretend the location is the jar or zip file
       if (lastIndexOfExclam != -1) {
         newLocation = newLocation.substring(0, lastIndexOfExclam);
       }
       newLocation = newLocation.substring(9);
       // If the file does not exist that may be because the path is
       // URLEncoded. Therefore, decode it.
       if (!new File(newLocation).exists()) {
         try {
           newLocation = URLDecoder.decode(newLocation, DEFAULT_ENCODING);
         } catch (UnsupportedEncodingException e) {
           // An unsupported encoding indicates confusion; do nothing.
           return location;
         }
       }
     }
     return newLocation;
   }
 
   public static URL findSourceInClassPath(ClassLoader cl, String sourceTypeName) {
     String toTry = sourceTypeName.replace('.', '/') + ".java";
     URL foundURL = cl.getResource(toTry);
     if (foundURL != null) {
       return foundURL;
     }
     int i = sourceTypeName.lastIndexOf('.');
     if (i != -1) {
       return findSourceInClassPath(cl, sourceTypeName.substring(0, i));
     } else {
       return null;
     }
   }
 
   /**
    * @param cls A class whose name you want.
    * @return The base name for the specified class.
    */
   public static String getClassName(Class<?> cls) {
     return getClassName(cls.getName());
   }
 
   /**
    * @param className A fully-qualified class name whose name you want.
    * @return The base name for the specified class.
    */
   public static String getClassName(String className) {
     return className.substring(className.lastIndexOf('.') + 1);
   }
 
   /**
    * Gets the contents of a file.
    * 
    * @param relativePath relative path within the install directory
    * @return the contents of the file, or null if an error occurred
    */
   public static String getFileFromInstallPath(String relativePath) {
     String installPath = Utility.getInstallPath();
     File file = new File(installPath + '/' + relativePath);
     return readFileAsString(file);
   }
 
   /**
    * A 4-digit hex result.
    */
   public static void hex4(char c, StringBuffer sb) {
     sb.append(HEX_CHARS[(c & 0xF000) >> 12]);
     sb.append(HEX_CHARS[(c & 0x0F00) >> 8]);
     sb.append(HEX_CHARS[(c & 0x00F0) >> 4]);
     sb.append(HEX_CHARS[c & 0x000F]);
   }
 
   /**
    * This method invokes an inaccessible method in another class.
    * 
    * @param targetClass the class owning the method
    * @param methodName the name of the method
    * @param argumentTypes the types of the parameters to the method call
    * @param target the receiver of the method call
    * @param arguments the parameters to the method call
    */
   public static void invokeInaccessableMethod(Class<?> targetClass,
       String methodName, Class<?>[] argumentTypes, TypeOracle target,
       Object[] arguments) {
     String failedReflectErrMsg = "The definition of " + targetClass.getName()
         + "." + methodName + " has changed in an " + "incompatible way.";
     try {
       Method m = targetClass.getDeclaredMethod(methodName, argumentTypes);
       m.setAccessible(true);
       m.invoke(target, arguments);
     } catch (NoSuchMethodException e) {
       throw new RuntimeException(failedReflectErrMsg, e);
     } catch (IllegalArgumentException e) {
       throw new RuntimeException(failedReflectErrMsg, e);
     } catch (IllegalAccessException e) {
       throw new RuntimeException(failedReflectErrMsg, e);
     } catch (InvocationTargetException e) {
       throw new RuntimeException(e.getTargetException());
     }
   }
 
   public static boolean isCompilationUnitOnDisk(String loc) {
     try {
       if (new File(loc).exists()) {
         return true;
       }
 
       URL url = new URL(loc);
       String s = url.toExternalForm();
       if (s.startsWith("file:") || s.startsWith("jar:")) {
         return true;
       }
     } catch (MalformedURLException e) {
       // Probably not really on disk.
     }
     return false;
   }
 
   public static boolean isValidJavaIdent(String token) {
     if (token.length() == 0) {
       return false;
     }
 
     if (!Character.isJavaIdentifierStart(token.charAt(0))) {
       return false;
     }
 
     for (int i = 1, n = token.length(); i < n; i++) {
       if (!Character.isJavaIdentifierPart(token.charAt(i))) {
         return false;
       }
     }
 
     return true;
   }
 
   public static void logMissingTypeErrorWithHints(TreeLogger logger,
       String missingType) {
     logger = logger.branch(TreeLogger.ERROR, "Unable to find type '"
         + missingType + "'", null);
 
     ClassLoader cl = Thread.currentThread().getContextClassLoader();
 
     URL sourceURL = findSourceInClassPath(cl, missingType);
     if (sourceURL != null) {
       if (missingType.indexOf(".client.") != -1) {
         Messages.HINT_PRIOR_COMPILER_ERRORS.log(logger, null);
         Messages.HINT_CHECK_MODULE_INHERITANCE.log(logger, null);
       } else {
         // Give the best possible hint here.
         //
         if (findSourceInClassPath(cl, missingType) == null) {
           Messages.HINT_CHECK_MODULE_NONCLIENT_SOURCE_DECL.log(logger, null);
         } else {
           Messages.HINT_PRIOR_COMPILER_ERRORS.log(logger, null);
         }
       }
     } else if (!missingType.equals("java.lang.Object")) {
       Messages.HINT_CHECK_TYPENAME.log(logger, missingType, null);
       Messages.HINT_CHECK_CLASSPATH_SOURCE_ENTRIES.log(logger, null);
     }
 
     // For Object in particular, there's a special warning.
     //
     if (missingType.indexOf("java.lang.") == 0) {
       Messages.HINT_CHECK_INHERIT_CORE.log(logger, null);
     } else if (missingType.indexOf("com.google.gwt.core.") == 0) {
       Messages.HINT_CHECK_INHERIT_CORE.log(logger, null);
     } else if (missingType.indexOf("com.google.gwt.user.") == 0) {
       Messages.HINT_CHECK_INHERIT_USER.log(logger, null);
     }
   }
 
   // /**
   // * Reads the file as an array of strings.
   // */
   // public static String[] readURLAsStrings(URL url) {
   // ArrayList lines = new ArrayList();
   // String contents = readURLAsString(url);
   // if (contents != null) {
   // StringReader sr = new StringReader(contents);
   // BufferedReader br = new BufferedReader(sr);
   // String line;
   // while (null != (line = readNextLine(br)))
   // lines.add(line);
   // }
   // return (String[]) lines.toArray(new String[lines.size()]);
   // }
 
   /**
    * Attempts to make a path relative to a particular directory.
    * 
    * @param from the directory from which 'to' should be relative
    * @param to an absolute path which will be returned so that it is relative to
    *          'from'
    * @return the relative path, if possible; null otherwise
    */
   public static File makeRelativeFile(File from, File to) {
 
     // Keep ripping off directories from the 'from' path until the 'from' path
     // is a prefix of the 'to' path.
     //
     String toPath = tryMakeCanonical(to).getAbsolutePath();
     File currentFrom = tryMakeCanonical(from.isDirectory() ? from
         : from.getParentFile());
 
     int numberOfBackups = 0;
     while (currentFrom != null) {
       String currentFromPath = currentFrom.getPath();
       if (toPath.startsWith(currentFromPath)) {
         // Found a prefix!
         //
         break;
       } else {
         ++numberOfBackups;
         currentFrom = currentFrom.getParentFile();
       }
     }
 
     if (currentFrom == null) {
       // Cannot make it relative.
       //
       return null;
     }
 
     // Find everything to the right of the common prefix.
     //
     String trailingToPath = toPath.substring(currentFrom.getAbsolutePath().length());
     if (currentFrom.getParentFile() != null && trailingToPath.length() > 0) {
       trailingToPath = trailingToPath.substring(1);
     }
 
     File relativeFile = new File(trailingToPath);
     for (int i = 0; i < numberOfBackups; ++i) {
       relativeFile = new File("..", relativeFile.getPath());
     }
 
     return relativeFile;
   }
 
   public static String makeRelativePath(File from, File to) {
     File f = makeRelativeFile(from, to);
     return (f != null ? f.getPath() : null);
   }
 
   public static String makeRelativePath(File from, String to) {
     File f = makeRelativeFile(from, new File(to));
     return (f != null ? f.getPath() : null);
   }
 
   /**
    * Give the developer a chance to see the in-memory source that failed.
    */
   public static String maybeDumpSource(TreeLogger logger, String location,
       char[] source, String optionalTypeName) {
 
     if (isCompilationUnitOnDisk(location)) {
       // Don't write another copy.
       //
       return null;
     }
 
     File tmpSrc;
     Throwable caught = null;
     try {
       String prefix = "gen";
       if (optionalTypeName != null) {
         prefix = optionalTypeName;
       }
       tmpSrc = File.createTempFile(prefix, ".java");
       writeCharsAsFile(logger, tmpSrc, source);
       String dumpPath = tmpSrc.getAbsolutePath();
       logger.log(TreeLogger.ERROR, "Compilation problem due to '" + location
           + "'; see snapshot " + dumpPath, null);
       return dumpPath;
     } catch (IOException e) {
       caught = e;
     } catch (UnableToCompleteException e) {
       caught = e;
     }
     logger.log(TreeLogger.ERROR,
         "Compilation problem due to in-memory source '" + location
             + "', but unable to dump to disk", caught);
     return null;
   }
 
   public static byte[] readFileAsBytes(File file) {
     FileInputStream fileInputStream = null;
     try {
       fileInputStream = new FileInputStream(file);
       int length = (int) file.length();
       byte[] data = new byte[length];
       fileInputStream.read(data);
       return data;
     } catch (IOException e) {
       return null;
     } finally {
       Utility.close(fileInputStream);
     }
   }
 
   public static char[] readFileAsChars(File file) {
     if (!file.exists()) {
       return null;
     }
     Reader fileReader = null;
     try {
       fileReader = new InputStreamReader(new FileInputStream(file),
           DEFAULT_ENCODING);
       int length = (int) file.length();
       if (length < 0) {
         return null;
       }
       char[] fileContents = new char[length];
       int charsRead = fileReader.read(fileContents);
       if (charsRead < fileContents.length) {
         /*
          * Calling functions expect returned char[] to be fully populated. This
          * happens because some UTF-8 chars take more than one byte to
          * represent.
          */
         char[] trimmed = new char[charsRead];
         System.arraycopy(fileContents, 0, trimmed, 0, charsRead);
         fileContents = trimmed;
       }
       return fileContents;
     } catch (IOException e) {
       return null;
     } finally {
       Utility.close(fileReader);
     }
   }
 
   public static String readFileAsString(File file) {
     try {
       URL toURL = file.toURI().toURL();
       char[] buf = readURLAsChars(toURL);
       if (buf == null) {
         return null;
       }
       return String.valueOf(buf);
     } catch (MalformedURLException e) {
       return null;
     }
   }
 
   /**
    * Reads the next non-empty line.
    * 
    * @return a non-empty string that has been trimmed or null if the reader is
    *         exhausted
    */
   public static String readNextLine(BufferedReader br) {
     try {
       String line = br.readLine();
       while (line != null) {
         line = line.trim();
         if (line.length() > 0) {
           break;
         }
         line = br.readLine();
       }
       return line;
     } catch (IOException e) {
       return null;
     }
   }
 
   /**
    * @return null if the file could not be read
    */
   public static byte[] readURLAsBytes(URL url) {
     // ENH: add a weak cache that has an additional check against the file date
     InputStream input = null;
     try {
       URLConnection connection = url.openConnection();
       connection.setUseCaches(false);
       input = connection.getInputStream();
       int contentLength = connection.getContentLength();
       if (contentLength < 0) {
         return null;
       }
       byte[] data = new byte[contentLength];
       input.read(data);
       return data;
     } catch (IOException e) {
       return null;
     } finally {
       Utility.close(input);
     }
   }
 
   /**
    * @return null if the file could not be read
    */
   public static char[] readURLAsChars(URL url) {
     // ENH: add a weak cache that has an additional check against the file date
     InputStreamReader reader = null;
     try {
       URLConnection connection = url.openConnection();
       connection.setUseCaches(false);
       reader = new InputStreamReader(connection.getInputStream(),
           DEFAULT_ENCODING);
       int contentLength = connection.getContentLength();
       if (contentLength < 0) {
         return null;
       }
       char[] fileContents = new char[contentLength];
       int charsRead = reader.read(fileContents);
       if (charsRead < fileContents.length) {
         /*
          * Calling functions expect returned char[] to be fully populated. This
          * happens because some UTF-8 chars take more than one byte to
          * represent.
          */
         char[] trimmed = new char[charsRead];
         System.arraycopy(fileContents, 0, trimmed, 0, charsRead);
         fileContents = trimmed;
       }
       return fileContents;
     } catch (IOException e) {
       return null;
     } finally {
       Utility.close(reader);
     }
   }
 
   /**
    * Deletes a file or recursively deletes a directory.
    * 
    * @param file the file to delete, or if this is a directory, the directory
    *          that serves as the root of a recursive deletion
    * @param childrenOnly if <code>true</code>, only the children of a
    *          directory are recursively deleted but the specified directory
    *          itself is spared; if <code>false</code>, the specified
    *          directory is also deleted; ignored if <code>file</code> is not a
    *          directory
    */
   public static void recursiveDelete(File file, boolean childrenOnly) {
     if (file.isDirectory()) {
       File[] children = file.listFiles();
       if (children != null) {
         for (int i = 0; i < children.length; i++) {
           recursiveDelete(children[i], false);
         }
       }
       if (childrenOnly) {
         // Do not delete the specified directory itself.
         //
         return;
       }
     }
     file.delete();
   }
 
   public static File removeExtension(File file) {
     String name = file.getName();
     int lastDot = name.lastIndexOf('.');
     if (lastDot != -1) {
       name = name.substring(0, lastDot);
     }
     return new File(file.getParentFile(), name);
   }
 
   public static <T> T[] removeNulls(T[] a) {
     int n = a.length;
     for (int i = 0; i < a.length; i++) {
       if (a[i] == null) {
         --n;
       }
     }
 
     Class<?> componentType = a.getClass().getComponentType();
     T[] t = (T[]) Array.newInstance(componentType, n);
     int out = 0;
     for (int in = 0; in < t.length; in++) {
       if (a[in] != null) {
         t[out++] = a[in];
       }
     }
     return t;
   }
 
   /**
    * @param path The path to slashify.
    * @return The path with any directory separators replaced with '/'.
    */
   public static String slashify(String path) {
     path = path.replace(File.separatorChar, '/');
     if (path.endsWith("/")) {
       path = path.substring(0, path.length() - 1);
     }
     return path;
   }
 
   /**
    * Creates an array from a collection of the specified component type and
    * size. You can definitely downcast the result to T[] if T is the specified
    * component type.
    * 
    * Class<? super T> is used to allow creation of generic types, such as
    * Map.Entry<K,V> since we can only pass in Map.Entry.class.
    */
   public static <T> T[] toArray(Class<? super T> componentType,
       Collection<? extends T> coll) {
     int n = coll.size();
     T[] a = (T[]) Array.newInstance(componentType, n);
     return coll.toArray(a);
   }
 
   /**
    * Like {@link #toArray(Class, Collection)}, but the option of having the
    * array reversed.
    */
   public static <T> T[] toArrayReversed(Class<? super T> componentType,
       Collection<? extends T> coll) {
     int n = coll.size();
     T[] a = (T[]) Array.newInstance(componentType, n);
     int i = n - 1;
     for (Iterator<? extends T> iter = coll.iterator(); iter.hasNext(); --i) {
       a[i] = iter.next();
     }
     return a;
   }
 
   /**
    * Returns a string representation of the byte array as a series of
    * hexadecimal characters.
    * 
    * @param bytes byte array to convert
    * @return a string representation of the byte array as a series of
    *         hexadecimal characters
    */
   public static String toHexString(byte[] bytes) {
     char[] hexString = new char[2 * bytes.length];
     int j = 0;
     for (int i = 0; i < bytes.length; i++) {
       hexString[j++] = Util.HEX_CHARS[(bytes[i] & 0xF0) >> 4];
       hexString[j++] = Util.HEX_CHARS[bytes[i] & 0x0F];
     }
 
     return new String(hexString);
   }
 
   /**
    * Creates a string array from the contents of a collection.
    */
   public static String[] toStringArray(Collection<String> coll) {
     return toArray(String.class, coll);
   }
 
   public static URL toURL(File f) {
     try {
       return f.toURI().toURL();
     } catch (MalformedURLException e) {
       throw new RuntimeException("Failed to convert a File to a URL", e);
     }
   }
 
   public static URL toURL(URL jarUrl, JarEntry jarEntry) {
     return toURL(jarUrl, jarEntry.toString());
   }
 
   public static URL toURL(URL jarUrl, String path) {
     try {
       return new URL("jar" + ":" + jarUrl.toString() + "!/" + path);
     } catch (MalformedURLException e) {
       throw new RuntimeException("Failed to convert a jar path to a URL", e);
     }
   }
 
   public static String toXml(Document doc) {
     Throwable caught = null;
     try {
       byte[] bytes = toXmlUtf8(doc);
       return new String(bytes, DEFAULT_ENCODING);
     } catch (UnsupportedEncodingException e) {
       caught = e;
     }
     throw new RuntimeException("Unable to encode xml string as utf-8", caught);
   }
 
   public static byte[] toXmlUtf8(Document doc) {
     Throwable caught = null;
     try {
       StringWriter sw = new StringWriter();
       PrintWriter pw = new PrintWriter(sw);
       writeDocument(pw, doc);
       return sw.toString().getBytes(DEFAULT_ENCODING);
     } catch (UnsupportedEncodingException e) {
       caught = e;
     } catch (IOException e) {
       caught = e;
     }
     throw new RuntimeException(
         "Unable to encode xml document object as a string", caught);
 
     // THE COMMENTED-OUT CODE BELOW IS THE WAY I'D LIKE TO GENERATE XML,
     // BUT IT SEEMS TO BLOW UP WHEN YOU CHANGE JRE VERSIONS AND/OR RUN
     // IN TOMCAT. INSTEAD, I JUST SLAPPED TOGETHER THE MINIMAL STUFF WE
     // NEEDED TO WRITE CACHE ENTRIES.
 
     // Throwable caught = null;
     // try {
     // TransformerFactory transformerFactory = TransformerFactory.newInstance();
     // Transformer transformer = transformerFactory.newTransformer();
     // transformer.setOutputProperty(javax.xml.transform.OutputKeys.INDENT,
     // "yes");
     // transformer.setOutputProperty(
     // "{http://xml.apache.org/xslt}indent-amount", "4");
     // ByteArrayOutputStream baos = new ByteArrayOutputStream();
     // OutputStreamWriter osw = new OutputStreamWriter(baos, "UTF-8");
     // StreamResult result = new StreamResult(osw);
     // DOMSource domSource = new DOMSource(doc);
     // transformer.transform(domSource, result);
     // byte[] bytes = baos.toByteArray();
     // return bytes;
     // } catch (TransformerConfigurationException e) {
     // caught = e;
     // } catch (UnsupportedEncodingException e) {
     // caught = e;
     // } catch (TransformerException e) {
     // caught = e;
     // }
     // throw new RuntimeException(
     // "Unable to encode xml document object as a string", caught);
   }
 
   public static File tryCombine(File parentMaybeIgnored, File childMaybeAbsolute) {
     if (childMaybeAbsolute == null) {
       return parentMaybeIgnored;
     } else if (childMaybeAbsolute.isAbsolute()) {
       return childMaybeAbsolute;
     } else {
       return new File(parentMaybeIgnored, childMaybeAbsolute.getPath());
     }
   }
 
   public static File tryCombine(File parentMaybeIgnored,
       String childMaybeAbsolute) {
     return tryCombine(parentMaybeIgnored, new File(childMaybeAbsolute));
   }
 
   /**
    * Attempts to find the canonical form of a file path.
    * 
    * @return the canonical version of the file path, if it could be computed;
    *         otherwise, the original file is returned unmodified
    */
   public static File tryMakeCanonical(File file) {
     try {
       return file.getCanonicalFile();
     } catch (IOException e) {
       return file;
     }
   }
 
   public static void writeBytesToFile(TreeLogger logger, File where, byte[] what)
       throws UnableToCompleteException {
     writeBytesToFile(logger, where, new byte[][] {what});
   }
 
   /**
    * Gathering write.
    */
   public static void writeBytesToFile(TreeLogger logger, File where,
       byte[][] what) throws UnableToCompleteException {
     RandomAccessFile f = null;
     Throwable caught;
     try {
       where.getParentFile().mkdirs();
       f = new RandomAccessFile(where, "rwd");
       long newLen = 0;
       for (int i = 0; i < what.length; i++) {
         newLen += what[i].length;
         f.write(what[i]);
       }
       f.setLength(newLen);
       return;
     } catch (FileNotFoundException e) {
       caught = e;
     } catch (IOException e) {
       caught = e;
     } finally {
       Utility.close(f);
     }
     String msg = "Unable to write file '" + where + "'";
     logger.log(TreeLogger.ERROR, msg, caught);
     throw new UnableToCompleteException();
   }
 
   public static void writeCharsAsFile(TreeLogger logger, File file, char[] chars)
       throws UnableToCompleteException {
     FileOutputStream stream = null;
     OutputStreamWriter writer = null;
     BufferedWriter buffered = null;
     try {
       file.getParentFile().mkdirs();
       stream = new FileOutputStream(file);
       writer = new OutputStreamWriter(stream, DEFAULT_ENCODING);
       buffered = new BufferedWriter(writer);
       buffered.write(chars);
     } catch (IOException e) {
       logger.log(TreeLogger.ERROR, "Unable to write file: "
           + file.getAbsolutePath(), e);
       throw new UnableToCompleteException();
     } finally {
       Utility.close(buffered);
       Utility.close(writer);
       Utility.close(stream);
     }
   }
 
   public static boolean writeStringAsFile(File file, String string) {
     FileOutputStream stream = null;
     OutputStreamWriter writer = null;
     BufferedWriter buffered = null;
     try {
       stream = new FileOutputStream(file);
       writer = new OutputStreamWriter(stream, DEFAULT_ENCODING);
       buffered = new BufferedWriter(writer);
       file.getParentFile().mkdirs();
       buffered.write(string);
     } catch (IOException e) {
       return false;
     } finally {
       Utility.close(buffered);
       Utility.close(writer);
       Utility.close(stream);
     }
     return true;
   }
 
   /**
    * Escapes '&', '<', '>', '"', and '\'' to their XML entity equivalents.
    */
  public static String escapeXml(String unescaped) {
     String escaped = unescaped.replaceAll("\\&", "&amp;");
     escaped = escaped.replaceAll("\\<", "&lt;");
     escaped = escaped.replaceAll("\\>", "&gt;");
     escaped = escaped.replaceAll("\\\"", "&quot;");
     escaped = escaped.replaceAll("\\'", "&apos;");
     return escaped;
   }
 
   private static void writeAttribute(PrintWriter w, Attr attr, int depth)
       throws IOException {
     w.write(attr.getName());
     w.write('=');
     Node c = attr.getFirstChild();
     while (c != null) {
       w.write('"');
       writeNode(w, c, depth);
       w.write('"');
       c = c.getNextSibling();
     }
   }
 
   private static void writeDocument(PrintWriter w, Document d)
       throws IOException {
     w.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
     Node c = d.getFirstChild();
     while (c != null) {
       writeNode(w, c, 0);
       c = c.getNextSibling();
     }
   }
 
   private static void writeElement(PrintWriter w, Element el, int depth)
       throws IOException {
     String tagName = el.getTagName();
 
     writeIndent(w, depth);
     w.write('<');
     w.write(tagName);
     NamedNodeMap attrs = el.getAttributes();
     for (int i = 0, n = attrs.getLength(); i < n; ++i) {
       w.write(' ');
       writeNode(w, attrs.item(i), depth);
     }
 
     Node c = el.getFirstChild();
     if (c != null) {
       // There is at least one child.
       //
       w.println('>');
 
       // Write the children.
       //
       while (c != null) {
         writeNode(w, c, depth + 1);
         w.println();
         c = c.getNextSibling();
       }
 
       // Write the closing tag.
       //
       writeIndent(w, depth);
       w.write("</");
       w.write(tagName);
       w.print('>');
     } else {
       // There are no children, so just write the short form close.
       //
       w.print("/>");
     }
   }
 
   private static void writeIndent(PrintWriter w, int depth) {
     for (int i = 0; i < depth; ++i) {
       w.write('\t');
     }
   }
 
   private static void writeNode(PrintWriter w, Node node, int depth)
       throws IOException {
     short nodeType = node.getNodeType();
     switch (nodeType) {
       case Node.ELEMENT_NODE:
         writeElement(w, (Element) node, depth);
         break;
       case Node.ATTRIBUTE_NODE:
         writeAttribute(w, (Attr) node, depth);
         break;
       case Node.DOCUMENT_NODE:
         writeDocument(w, (Document) node);
         break;
       case Node.TEXT_NODE:
         writeText(w, (Text) node);
         break;
 
       case Node.COMMENT_NODE:
       case Node.CDATA_SECTION_NODE:
       case Node.ENTITY_REFERENCE_NODE:
       case Node.ENTITY_NODE:
       case Node.PROCESSING_INSTRUCTION_NODE:
       default:
         throw new RuntimeException("Unsupported DOM node type: " + nodeType);
     }
   }
 
   private static void writeText(PrintWriter w, Text text) throws DOMException {
     String nodeValue = text.getNodeValue();
     String escaped = escapeXml(nodeValue);
     w.write(escaped);
   }
 
   /**
    * Not instantiable.
    */
   private Util() {
   }
 
 }
