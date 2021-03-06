 /*
  * Copyright 2006 Wyona
  */
 
 package org.wyona.yanel.impl.resources;
 
 import org.apache.log4j.Category;
 import org.wyona.yanel.core.Path;
 import org.wyona.yanel.core.Resource;
 import org.wyona.yanel.core.ResourceConfiguration;
 import org.wyona.yanel.core.api.attributes.ViewableV2;
 import org.wyona.yanel.core.attributes.viewable.View;
 import org.wyona.yanel.core.attributes.viewable.ViewDescriptor;
 import javax.servlet.http.HttpServletRequest;
 
 import org.wyona.yanel.core.transformation.I18nTransformer;
 
 import javax.xml.transform.Transformer;
 import javax.xml.transform.TransformerConfigurationException;
 import javax.xml.transform.TransformerException;
 import javax.xml.transform.TransformerFactory;
 import javax.xml.transform.TransformerFactoryConfigurationError;
 import javax.xml.transform.dom.DOMSource;
 import java.io.ByteArrayOutputStream;
 import java.io.ByteArrayInputStream;
 import java.io.FileInputStream;
 import java.util.ArrayList;
 import java.util.HashMap;
 
 import javax.xml.parsers.DocumentBuilder;
 import javax.xml.parsers.DocumentBuilderFactory;
 import javax.xml.parsers.SAXParser;
 import javax.xml.parsers.SAXParserFactory;
 import java.io.File;
 
 import javax.xml.transform.stream.StreamResult;
 import javax.xml.transform.stream.StreamSource;
 import java.net.URL;
 import java.io.InputStream;
 import org.w3c.dom.Document;
 import org.w3c.dom.Element;
 import org.w3c.dom.NodeList;
 import org.w3c.dom.Node;
 
 
 
 import com.hp.hpl.jena.rdf.model.*;
 
 
 
 /**
  * 
  */
 public class UpdateResource extends Resource implements ViewableV2 {
 
     private static Category log = Category.getInstance(UpdateResource.class);
     private String defaultLanguage;
     private String language = null;
     
     /**
      * 
      */
     public UpdateResource() {
     }
 
     /**
      * 
      */
     public ViewDescriptor[] getViewDescriptors() {
         return null;
     }
 
     /**
      * 
      */
     public View getView(String viewId) throws Exception {
 
         String path = getPath();
        defaultLanguage = getRealm().getDefaultLanguage();
 
         // Get language
         try {
             language = request.getParameter("yanel.meta.language");
         } catch (Exception e) {
             log.debug("language param is not found will use default : " + language);
             language = defaultLanguage;
         }
         if (language == null || ("").equals(language)) {
             log.debug("language param is empty or null : " + language);
             language = defaultLanguage;
         }
 
         Transformer transformer = null;
         I18nTransformer i18nTransformer = new I18nTransformer("update", language, defaultLanguage);
         ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
         View defaultView = new View();
         DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
         
         try {
 
                     //install.rdf
                     
                     InstallInfo installInfo = new InstallInfo(request);
 
                     //just testing
                     System.out.println(installInfo.getId());
                     System.out.println(installInfo.getInstalltype());
                     System.out.println(installInfo.getContextPrefix());
                     System.out.println(installInfo.getOsName());
                     System.out.println(installInfo.getTargetApplicationId());
                     System.out.println(installInfo.getTargetApplicationVersion());
                     System.out.println(installInfo.getUpdateURL());
                     System.out.println(installInfo.getVersion());
                     System.out.println(installInfo.getJavaVersion());
                     for (int i = 0; i < installInfo.getProtectedFiles().size(); i++) {
                         System.out.println("getProtectedFiles --------" + installInfo.getProtectedFiles().get(i));
                     }
                     
                     
                     
                     URL UpdateRdfUrl = new URL(installInfo.getUpdateURL());
                     InputStream updateRdfIn = UpdateRdfUrl.openStream();
                     UpdateInfo updateInfo = new UpdateInfo(updateRdfIn, installInfo);
                     
 
                     
                     if (installInfo.getInstalltype().equals("source")) {
                         StringBuffer message = new StringBuffer();
                         message.append("<p>");
                         message.append("This Yanel was installed from source. You can only use the updater if you installed yanel from binary. Please use svn up, build.sh");
                         message.append("</p>");
                         
                         byteArrayOutputStream = getOutput(message);
                         //transformer = TransformerFactory.newInstance().newTransformer();
 
                         
                     } else if (installInfo.getInstalltype().equals("bin-snapshot")) {
                 if (request.getParameter("update") != null && request.getParameter("update").equals("true")) {
                     WebAppUpdater webAppUpdater = new WebAppUpdater(request, "");
                     
                     StringBuffer message = new StringBuffer();
                     message.append("<p>");
                     if (webAppUpdater.update()) {
                         message.append("Update done.");
                     } else {
                         message.append("Update failed.");
                     }
                     message.append("</p>");
                     byteArrayOutputStream = getOutput(message);
                 } else {
                     StringBuffer message = new StringBuffer();
                     message.append("<p>");
                     message.append("This are the updates which you can get:");
                     message.append("</p>");
                     message.append("<ul>");
                     for (int i = 0; i < updateInfo.getUpdateVersions().size(); i++) {
                         HashMap versionDetails = (HashMap) updateInfo.getUpdateVersions()
                                 .get(i);
                         if (versionDetails.get("version") != installInfo.getVersion()) {
                             message.append("<li>Version: " + versionDetails.get("version") + " ChangeLog: "
                                     + versionDetails.get("changeLog") + " Update Link: " + versionDetails.get("updateLink") + "<form><input type=\"submit\" name=\"update\" value=\"true\"></input></form></li>");
                         }
                     }
                     message.append("</ul>");
                     byteArrayOutputStream = getOutput(message);
                 }
             }
                     // this.installRdf.put("updateURL",
                     // installRdf.getChild("Description").getChild("um:updateURL").getValue());
                     //this.installRdf.put("version", installRdf.getChild("Description").getChild("um:version").getValue()); 
                     //transformer.transform(new StreamSource(InstallRdf), new StreamResult(byteArrayOutputStream));
                     
                     //update.rdf
                     //URL UpdateRdfUrl = new URL(installRdf.getUpdateURL());
                     //InputStream UpdateRdf = UpdateRdfUrl.openStream();
                     
                     //Configuration updateRdf = configBuilder.build(UpdateRdf);
                     //Document UpdateRdfDoc = builder.parse(UpdateRdf);
                     //transformer = TransformerFactory.newInstance().newTransformer();
                     //transformer.transform(new DOMSource(UpdateRdfDoc), new StreamResult(byteArrayOutputStream));
                     
                     
 
             //SAXParser saxParser = SAXParserFactory.newInstance().newSAXParser();
             //saxParser.parse(new ByteArrayInputStream(byteArrayOutputStream.toByteArray()),i18nTransformer);
         } catch (Exception e) {
             log.error(e.getMessage(), e);
         }
 
         defaultView.setMimeType("application/xhtml+xml");
         defaultView.setInputStream(new ByteArrayInputStream(byteArrayOutputStream.toByteArray()));
         return defaultView;
     }
 
     private ByteArrayOutputStream getOutput(StringBuffer insert) throws TransformerConfigurationException {
         ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
         StringBuffer sb = new StringBuffer();
         sb.append("<?xml version=\"1.0\"?>");
         sb.append("<html xmlns=\"http://www.w3.org/1999/xhtml\">");
         sb.append("<head>");
         sb.append("<title>Update Yanel</title>");
         sb.append("</head>");
         sb.append("<body>");
         sb.append(insert);
         sb.append("</body>");
         sb.append("</html>");
         
         try {
             Transformer transformer = TransformerFactory.newInstance().newTransformer();
             transformer.transform(new StreamSource(new java.io.StringBufferInputStream(sb.toString())), new StreamResult(byteArrayOutputStream));
         } catch (Exception e) {
             log.error(e.getMessage(), e);
         }
         return byteArrayOutputStream;
     }
     
     public boolean exists() throws Exception {
         // NOTE does exists() make sense for this resource?
         log.warn("Not implemented yet!");
         return true;
     }
 
     /**
      * 
      */
     public long getSize() throws Exception {
         // NOTE does getSize make sense for this resource?
         return getRealm().getRepository().getSize(new Path(getPath()));
     }
     
     /**
      * Get property value from resource configuration
      */
     private String getResourceProperty(String name) throws Exception {
         ResourceConfiguration rc = getConfiguration();
         if (rc != null) return rc.getProperty(name);
         return getRTI().getProperty(name);
     }
     
     public String getMimeType(String viewId){
         return "application/xml";
     }
 }
