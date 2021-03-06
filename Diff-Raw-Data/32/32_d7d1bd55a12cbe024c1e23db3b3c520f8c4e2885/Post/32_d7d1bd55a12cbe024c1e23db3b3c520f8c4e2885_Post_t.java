 /*
  * Copyright (c) 1998-2004 Caucho Technology -- all rights reserved
  *
  * This file is part of Resin(R) Open Source
  *
  * Each copy or derived work must preserve the copyright notice and this
  * notice unmodified.
  *
  * Resin Open Source is free software; you can redistribute it and/or modify
  * it under the terms of the GNU General Public License as published by
  * the Free Software Foundation; either version 2 of the License, or
  * (at your option) any later version.
  *
  * Resin Open Source is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, or any warranty
  * of NON-INFRINGEMENT.  See the GNU General Public License for more
  * details.
  *
  * You should have received a copy of the GNU General Public License
  * along with Resin Open Source; if not, write to the
  *
  *   Free Software Foundation, Inc.
  *   59 Temple Place, Suite 330
  *   Boston, MA 02111-1307  USA
  *
  * @author Scott Ferguson
  */
 
 package com.caucho.quercus.env;
 
 import java.io.InputStream;
 import java.io.IOException;
 
 import javax.servlet.http.HttpServletRequest;
 
 import com.caucho.vfs.Vfs;
 import com.caucho.vfs.Path;
 import com.caucho.vfs.ReadStream;
 import com.caucho.vfs.WriteStream;
 import com.caucho.vfs.MultipartStream;
 import com.caucho.quercus.lib.QuercusStringModule;
 
 /**
  * Handling of POST requests.
  */
 public class Post {
   static void fillPost(Env env,
                        ArrayValue post, ArrayValue files,
                        HttpServletRequest request,
                        boolean addSlashesToValues)
   {
     InputStream is = null;
 
     try {
       if (! request.getMethod().equals("POST"))
         return;
 
       String contentType = request.getHeader("Content-Type");
 
       if (contentType == null ||
           ! contentType.startsWith("multipart/form-data"))
         return;
 
       String boundary = getBoundary(contentType);
 
       is = request.getInputStream();
 
       MultipartStream ms = new MultipartStream(Vfs.openRead(is), boundary);
       // ms.setEncoding(javaEncoding);
 
       readMultipartStream(env, ms, post, files, addSlashesToValues);
     } catch (IOException e) {
       e.printStackTrace();
     } finally {
       try {
         if (is != null)
           is.close();
       } catch (IOException e) {
       }
     }
   }
 
   private static void readMultipartStream(Env env,
                                           MultipartStream ms,
                                           ArrayValue post,
                                           ArrayValue files,
                                           boolean addSlashesToValues)
     throws IOException
   {
     ReadStream is;
 
     while ((is = ms.openRead()) != null) {
       String attr = (String) ms.getAttribute("content-disposition");
 
       if (attr == null || ! attr.startsWith("form-data")) {
         // XXX: is this an error?
         continue;
       }
 
       String name = getAttribute(attr, "name");
       String filename = getAttribute(attr, "filename");
 
       if (filename == null) {
         StringBuilder value = new StringBuilder();
         int ch;
 
         while ((ch = is.read()) >= 0) {
           value.append((char) ch);
         }
 
         addFormValue(post, name, new StringValue(value.toString()), null, addSlashesToValues);
       }
       else {
         Path tmpName = env.getUploadDirectory().createTempFile("php", "tmp");
 
         env.addRemovePath(tmpName);
 
         WriteStream os = tmpName.openWrite();
         try {
           os.writeStream(is);
         } finally {
           os.close();
         }
 
         ArrayValue entry = new ArrayValueImpl();
         entry.put("name", filename);
         String mimeType = getAttribute(attr, "mime-type");
         if (mimeType != null)
           entry.put("type", mimeType);
         entry.put("size", tmpName.getLength());
         entry.put("tmp_name", tmpName.getTail());
 
         // XXX: error
 
         addFormValue(files, name, entry, null, addSlashesToValues);
       }
     }
   }
 
   public static void addFormValue(ArrayValue array,
                                   String key,
                                   String []formValueList,
                                   boolean addSlashesToValues)
   {
    // php/081b
    Value value = new StringValue(formValueList[formValueList.length - 1]);
    
    addFormValue(array, key,
		 value,
		 formValueList,
		 addSlashesToValues);
   }
 
   public static void addFormValue(ArrayValue array,
                                   String key,
                                   Value formValue,
                                   String []formValueList,
                                   boolean addSlashesToValues)
   {
     int p = key.indexOf('[');
     int q = key.indexOf(']', p);
 
     if (p > 0 && p < q) {
       String index = key;
 
       key = key.substring(0, p);
 
       Value keyValue = new StringValue(key);
       Value existingValue = array.get(keyValue);
 
       if (existingValue == null || ! existingValue.isset()) {
         existingValue = new ArrayValueImpl();
         array.put(keyValue, existingValue);
       }
       else if (! existingValue.isArray()) {
         existingValue = new ArrayValueImpl().put(existingValue);
         array.put(keyValue, existingValue);
       }
 
       array = (ArrayValue) existingValue;
 
       int p1;
       while ((p1 = index.indexOf('[', q)) > 0) {
         key = index.substring(p + 1, q);
 
         if (key.equals("")) {
           existingValue = new ArrayValueImpl();
           array.put(existingValue);
         }
         else {
           keyValue = new StringValue(key);
           existingValue = array.get(keyValue);
 
           if (existingValue == null || ! existingValue.isset()) {
             existingValue = new ArrayValueImpl();
             array.put(keyValue, existingValue);
           }
           else if (! existingValue.isArray()) {
             existingValue = new ArrayValueImpl().put(existingValue);
             array.put(keyValue, existingValue);
           }
         }
 
         array = (ArrayValue) existingValue;
 
         p = p1;
         q = index.indexOf(']', p);
       }
 
       if (q > 0)
         index = index.substring(p + 1, q);
       else
         index = index.substring(p + 1);
 
       if (index.equals("")) {
         if (formValueList != null) {
           for (int i = 0; i < formValueList.length; i++) {
             put(array, null, new StringValue(formValueList[i]), addSlashesToValues);
           }
         }
         else
           array.put(formValue);
       }
       else if ('0' <= index.charAt(0) && index.charAt(0) <= '9')
         put(array, new LongValue(StringValue.toLong(index)), formValue, addSlashesToValues);
       else
         put(array, new StringValue(index), formValue, addSlashesToValues);
     }
     else {
       put(array, new StringValue(key), formValue, addSlashesToValues);
     }
   }
 
   private static void put(ArrayValue array, Value key, Value value, boolean addSlashes)
   {
     if (addSlashes && (value instanceof StringValue)) {
       value = QuercusStringModule.addslashes(value.toString());
     }
 
     if (key == null)
       array.put(value);
     else
       array.put(key, value);
   }
 
   private static String getBoundary(String contentType)
   {
     int i = contentType.indexOf("boundary=");
     if (i < 0)
       return null;
 
     i += "boundary=".length();
 
     int length = contentType.length();
 
     char ch;
 
     if (length <= i)
       return null;
     else if ((ch = contentType.charAt(i)) == '\'') {
       StringBuilder sb = new StringBuilder();
 
       for (i++; i < length && (ch = contentType.charAt(i)) != '\''; i++) {
         sb.append(ch);
       }
 
       return sb.toString();
     }
     else if (ch == '"') {
       StringBuilder sb = new StringBuilder();
 
       for (i++; i < length && (ch = contentType.charAt(i)) != '"'; i++) {
         sb.append(ch);
       }
 
       return sb.toString();
     }
     else {
       StringBuilder sb = new StringBuilder();
 
       for (; i < length && (ch = contentType.charAt(i)) != ' ' && ch != ';'; i++) {
         sb.append(ch);
       }
 
       return sb.toString();
     }
   }
 
   private static String getAttribute(String attr, String name)
   {
     if (attr == null)
       return null;
 
     int length = attr.length();
     int i = attr.indexOf(name);
     if (i < 0)
       return null;
 
     for (i += name.length(); i < length && attr.charAt(i) != '='; i++) {
     }
 
     for (i++; i < length && attr.charAt(i) == ' '; i++) {
     }
 
     StringBuilder value = new StringBuilder();
 
     if (i < length && attr.charAt(i) == '\'') {
       for (i++; i < length && attr.charAt(i) != '\''; i++)
         value.append(attr.charAt(i));
     }
     else if (i < length && attr.charAt(i) == '"') {
       for (i++; i < length && attr.charAt(i) != '"'; i++)
         value.append(attr.charAt(i));
     }
     else if (i < length) {
       char ch;
       for (; i < length && (ch = attr.charAt(i)) != ' ' && ch != ';'; i++)
         value.append(ch);
     }
 
     return value.toString();
   }
 }
 
