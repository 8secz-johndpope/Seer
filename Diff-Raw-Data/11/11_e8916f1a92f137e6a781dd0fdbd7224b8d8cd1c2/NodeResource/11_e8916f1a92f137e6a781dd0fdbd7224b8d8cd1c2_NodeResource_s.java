 /*
  * Copyright 2006 Wyona
  *
  *  Licensed under the Apache License, Version 2.0 (the "License");
  *  you may not use this file except in compliance with the License.
  *  You may obtain a copy of the License at
  *
  *      http://www.wyona.org/licenses/APACHE-LICENSE-2.0
  *
  *  Unless required by applicable law or agreed to in writing, software
  *  distributed under the License is distributed on an "AS IS" BASIS,
  *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  *  See the License for the specific language governing permissions and
  *  limitations under the License.
  */
 
 package org.wyona.yanel.impl.resources;
 
 import org.wyona.yanel.core.Path;
 import org.wyona.yanel.core.Resource;
 import org.wyona.yanel.core.ResourceConfiguration;
 import org.wyona.yanel.core.Topic;
 import org.wyona.yanel.core.api.attributes.CreatableV2;
 import org.wyona.yanel.core.api.attributes.IntrospectableV1;
 import org.wyona.yanel.core.api.attributes.ModifiableV2;
 import org.wyona.yanel.core.api.attributes.VersionableV2;
 import org.wyona.yanel.core.api.attributes.ViewableV2;
 import org.wyona.yanel.core.attributes.versionable.RevisionInformation;
 import org.wyona.yanel.core.attributes.viewable.View;
 import org.wyona.yanel.core.attributes.viewable.ViewDescriptor;
 import org.wyona.yanel.core.util.PathUtil;
 
 import org.wyona.yarep.core.Node;
 import org.wyona.yarep.core.Repository;
 import org.wyona.yarep.core.RepositoryFactory;
 import org.wyona.yarep.core.Revision;
 import org.wyona.yarep.util.RepoPath;
 
 import javax.servlet.http.HttpServletRequest;
 
 import java.io.InputStream;
 import java.io.InputStreamReader;
 import java.io.OutputStream;
 import java.io.Reader;
 import java.io.Writer;
 import java.util.Date;
 
 import org.apache.log4j.Category;
 
 /**
  * Generic Node Resource
  */
 public class NodeResource extends Resource implements ViewableV2, ModifiableV2, VersionableV2, IntrospectableV1 {
 //public class NodeResource extends Resource implements ViewableV2, ModifiableV2, VersionableV2, CreatableV2 {
 
     private static Category log = Category.getInstance(NodeResource.class);
 
     /**
      *
      */
     public NodeResource() {
     }
 
     /**
      *
      */
     public ViewDescriptor[] getViewDescriptors() {
         return null;
     }
 
     public View getView(String viewId, String revisionName) throws Exception {
         View defaultView = new View();
         
         defaultView.setInputStream(getRealm().getRepository().getNode(getPath())
                 .getRevision(revisionName).getInputStream());
         defaultView.setMimeType(getMimeType(viewId));
 
         return defaultView;
     }
 
     /**
      *
      */
     public View getView(String viewId) throws Exception {
         View defaultView = new View();
         
         defaultView.setInputStream(getRealm().getRepository().getNode(getPath()).getInputStream());
         defaultView.setMimeType(getMimeType(viewId));
 
         return defaultView;
     }
 
     /**
      *
      */
     public String getMimeType(String viewId) throws Exception {
         
         String mimeType = null;
         ResourceConfiguration resConfig = getConfiguration();
         if (resConfig != null) {
             mimeType = resConfig.getProperty("mime-type");
         } else {
             log.warn("Depracted: " + getPath());
             mimeType = getRTI().getProperty("mime-type");
         }
         
         if (mimeType != null) return mimeType;
 
         // TODO: Load config mime.types ...
         String suffix = PathUtil.getSuffix(getPath());
         if (suffix != null) {
             log.debug("SUFFIX: " + suffix);
             if (suffix.equals("html")) {
                 mimeType = "text/html";
             } else if (suffix.equals("htm")) {
                 mimeType = "text/html";
             } else if (suffix.equals("xhtml")) {
                 mimeType = "application/xhtml+xml";
             } else if (suffix.equals("xml")) {
                 mimeType = "application/xml";
             } else if (suffix.equals("css")) {
                 mimeType = "text/css";
             } else if (suffix.equals("js")) {
                 mimeType = "application/x-javascript";
             } else if (suffix.equals("png")) {
                 mimeType = "image/png";
             } else if (suffix.equals("jpg")) {
                 mimeType = "image/jpeg";
 	    } else if (suffix.equals("gif")) {
                 mimeType = "image/gif";
 	    } else if (suffix.equals("pdf")) {
                 mimeType = "application/pdf";
 	    } else if (suffix.equals("doc")) {
                 mimeType = "application/msword";
 	    } else if (suffix.equals("odt")) {
                 mimeType = "application/vnd.oasis.opendocument.text";
 	    } else if (suffix.equals("sxc")) {
                 mimeType = "application/vnd.sun.xml.calc";
 	    } else if (suffix.equals("xpi")) {
                 mimeType = "application/x-xpinstall";
 	    } else if (suffix.equals("zip")) {
                 mimeType = "application/zip";
 	    } else if (suffix.equals("txt")) {
                 mimeType = "text/plain";
 	    } else if (suffix.equals("mov")) {
                 mimeType = "video/quicktime";
 	    } else if (suffix.equals("ico")) {
                 mimeType = "image/x-icon";
             } else {
                 log.warn("Could not determine mime-type from suffix (suffix: " + suffix + ")!");
                 mimeType = "application/octet-stream";
             }
         } else {
             log.warn("mime-type will be set to application/octet-stream, because no suffix for " + getPath());
             mimeType = "application/octet-stream";
         }
         return mimeType;
     }
 
     /**
      *
      */
     public Reader getReader() throws Exception {
         return new InputStreamReader(getInputStream(), "UTF-8");
     }
 
     /**
      *
      */
     public InputStream getInputStream() throws Exception {
         return getRealm().getRepository().getNode(getPath()).getInputStream();
     }
 
     /**
      *
      */
     public Writer getWriter() throws Exception {
         log.error("Not implemented yet!");
         return null;
     }
 
     /**
      *
      */
     public OutputStream getOutputStream() throws Exception {
         return getRealm().getRepository().getNode(getPath()).getOutputStream();
     }
 
     /**
      *
      */
     public void write(InputStream in) throws Exception {
         log.warn("Not implemented yet!");
     }
 
     /**
      *
      */
     public long getLastModified() throws Exception {
        Node node = getRealm().getRepository().getNode(getPath());
        long lastModified;
        if (node.isResource()) {
            lastModified = node.getLastModified();
        } else {
            lastModified = 0;
        }
 
        return lastModified;
    }
 
     /**
      * Delete data of node resource
      */
     public boolean delete() throws Exception {
         getRealm().getRepository().getNode(getPath()).delete();
         return true;
     }
     
     /**
      *
      */
     public RevisionInformation[] getRevisions() throws Exception {
         Revision[] revisions = getRealm().getRepository().getNode(getPath()).getRevisions();
         RevisionInformation[] revisionInfos = new RevisionInformation[revisions.length];
        
        for (int i=0; i<revisions.length; i++) {
             revisionInfos[i] = new RevisionInformation(revisions[i]);
         }
         return revisionInfos; 
     }
    
     public void checkin(String comment) throws Exception {
         Node node = getRealm().getRepository().getNode(getPath());
         node.checkin(comment);
         /*
         if (node.isCheckedOut()) {
             String checkoutUserID = node.getCheckoutUserID(); 
             if (checkoutUserID.equals(userID)) {
                 node.checkin();
             } else {
                 throw new Exception("Resource is checked out by another user: " + checkoutUserID);
             }
         } else {
             throw new Exception("Resource is not checked out.");
         }
         */
     }
 
     public void checkout(String userID) throws Exception {
         Node node = getRealm().getRepository().getNode(getPath());
         node.checkout(userID);
         /*
         if (node.isCheckedOut()) {
             String checkoutUserID = node.getCheckoutUserID(); 
             if (checkoutUserID.equals(userID)) {
                 log.warn("Resource " + getPath() + " is already checked out by this user: " + checkoutUserID);
             } else {
                 throw new Exception("Resource is already checked out by another user: " + checkoutUserID);
             }
         } else {
             node.checkout(userID);
         }
         */
     }
 
     public void restore(String revisionName) throws Exception {
         getRealm().getRepository().getNode(getPath()).restore(revisionName);
     }
 
     public Date getCheckoutDate() throws Exception {
         Node node = getRealm().getRepository().getNode(getPath());
         // return node.getCheckoutDate();
         return null;
     }
 
     public String getCheckoutUserID() throws Exception {
         Node node = getRealm().getRepository().getNode(getPath());
         return node.getCheckoutUserID();
     }
 
     public boolean isCheckedOut() throws Exception {
         Node node = getRealm().getRepository().getNode(getPath());
         return node.isCheckedOut();
     }
 
     public boolean exists() throws Exception {
         return getRealm().getRepository().existsNode(getPath());
     }
 
     /**
      * 
      */
     public long getSize() throws Exception {
         Node node = getRealm().getRepository().getNode(getPath());
         long size;
         if (node.isResource()) {
             size = node.getSize();
         } else {
             size = 0;
         }
         return size;
     }
 
     /**
      *
      */
     public Object getProperty(String name) {
         log.warn("No implemented yet!");
         return null;
     }
 
     /**
      *
      */
     public String[] getPropertyNames() {
         String[] props = {"data"};
         return props;
     }
 
     /**
      *
      */
     public void setProperty(String name, Object value) {
         log.warn("No implemented yet!");
     }
 
     /**
      *
      */
     public void create(HttpServletRequest request) {
         log.warn("No implemented yet!");
     }
 
     /**
      *
      */
     public java.util.HashMap createRTIProperties(HttpServletRequest request) {
         log.warn("No implemented yet!");
         return null;
     }
 
     /**
      *
      */
     public String getPropertyType(String name) {
         return CreatableV2.TYPE_UPLOAD;
     }
 
     /**
      * Get introspection document
      */
     public String getIntrospection() throws Exception {
         String name = PathUtil.getName(getPath());
         StringBuffer buf = new StringBuffer();
         buf.append("<?xml version=\"1.0\"?>");
         buf.append("<introspection xmlns=\"http://www.wyona.org/neutron/2.0\">");
         buf.append("<resource name=\"" + name + "\">");
         buf.append("<edit mime-type=\"" + this.getMimeType(null) + "\">");
         buf.append("<checkout url=\"?yanel.resource.usecase=checkout\" method=\"GET\"/>");
         buf.append("<checkin  url=\"?yanel.resource.usecase=checkin\"  method=\"PUT\"/>");
         buf.append("</edit>");
 
         RevisionInformation[] revisions = getRevisions();
         if (revisions != null && revisions.length > 0) {
             buf.append("<versions>");
            for (int i = 0; i < revisions.length; i++) {
                 buf.append("<version url=\"?yanel.resource.revision=" + revisions[i].getName() + "\">");
                 buf.append("<comment>" + revisions[i].getComment() + "</comment>");
                 buf.append("<date>" + revisions[i].getDate() + "</date>");
                 buf.append("<user>" + revisions[i].getUser() + "</user>");
                 buf.append("<revision>" + revisions[i].getName() + "</revision>");
 
                 buf.append("<workflow>");
                 buf.append("<transitions>");
                 buf.append("<transition id=\"publish\" to=\"LIVE\" url=\"?yanel.resource.workflow.transition=publish\" method=\"POST\">");
                 buf.append("<description>Publish</description>");
                 buf.append("</transition>");
                 buf.append("</transitions>");
                 buf.append("</workflow>");
                 buf.append("</version>");
             }
             buf.append("</versions>");
         }
 
         buf.append("</resource>");
         buf.append("</introspection>");
         return buf.toString();
     }
 }
