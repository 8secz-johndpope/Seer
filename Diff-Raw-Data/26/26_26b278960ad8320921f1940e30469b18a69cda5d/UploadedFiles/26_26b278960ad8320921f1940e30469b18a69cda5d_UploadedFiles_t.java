 /*
  * Copyright 2009-2015 European Molecular Biology Laboratory
  *
  * Licensed under the Apache License, Version 2.0 (the "License");
  * you may not use this file except in compliance with the License.
  * You may obtain a copy of the License at
  *
  * http://www.apache.org/licenses/LICENSE-2.0
  *
  * Unless required by applicable law or agreed to in writing, software
  * distributed under the License is distributed on an "AS IS" BASIS,
  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or impl
  * See the License for the specific language governing permissions and
  * limitations under the License.
  */
 
 package uk.ac.ebi.fg.annotare2.web.server.services;
 
 import org.apache.commons.fileupload.FileItem;
 
 import javax.servlet.http.HttpSession;
 import java.io.FileNotFoundException;
 import java.util.List;
 
 /**
  * @author Olga Melnichuk
  */
 public class UploadedFiles {
 
    private static final String GWTUPLOAD_FILES_ATTRIBUTE = "FILES";
    private static final String GWTUPLOAD_LAST_FILES_ATTRIBUTE = "LAST_FILES";
 
     @SuppressWarnings("unchecked")
     public static FileItem get(HttpSession session, String fieldName) throws FileNotFoundException {
        List<FileItem> items = (List<FileItem>) session.getAttribute(GWTUPLOAD_FILES_ATTRIBUTE);
         for (FileItem item : items) {
             if (item.getFieldName().equals(fieldName)) {
                 return item;
             }
         }
         throw new FileNotFoundException("Unable to locate uploaded file");
     }
 
     @SuppressWarnings("unchecked")
     public static FileItem getFirst(HttpSession session) throws FileNotFoundException {
        List<FileItem> items = (List<FileItem>) session.getAttribute(GWTUPLOAD_FILES_ATTRIBUTE);
         if (items.size() > 0) {
             return items.get(0);
         }
         throw new FileNotFoundException("Unable to locate uploaded file");
     }
 
     @SuppressWarnings("unchecked")
     public static void removeSessionFiles(HttpSession session) {
        List<FileItem> items = (List<FileItem>) session.getAttribute(GWTUPLOAD_FILES_ATTRIBUTE);
         for (FileItem item : items) {
             if (null != item && !item.isFormField()) {
                 item.delete();
             }
         }
        session.removeAttribute(GWTUPLOAD_FILES_ATTRIBUTE);
        session.removeAttribute(GWTUPLOAD_LAST_FILES_ATTRIBUTE);
     }
 
 }
