 /*
  * This program is free software; you can redistribute it and/or modify it under the
  * terms of the GNU Lesser General Public License, version 2.1 as published by the Free Software
  * Foundation.
  *
  * You should have received a copy of the GNU Lesser General Public License along with this
  * program; if not, you can obtain a copy at http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
  * or from the Free Software Foundation, Inc.,
  * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
  *
  * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
  * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
  * See the GNU Lesser General Public License for more details.
  *
  * Copyright (c) 2010 Pentaho Corporation..  All rights reserved.
  */
 package org.pentaho.agilebi.spoon.publish;
 
 import org.apache.commons.io.IOUtils;
 import org.dom4j.Attribute;
 import org.dom4j.Document;
 import org.dom4j.Element;
 import org.dom4j.io.SAXReader;
 import org.dom4j.io.XMLWriter;
 import org.pentaho.agilebi.spoon.publish.ModelServerPublish;
 import org.pentaho.agilebi.modeler.ModelerException;
 import org.pentaho.agilebi.modeler.ModelerWorkspace;
 import org.pentaho.agilebi.spoon.XulUI;
 import org.pentaho.di.core.Const;
 import org.pentaho.di.core.database.DatabaseMeta;
 import org.pentaho.di.core.gui.SpoonFactory;
 import org.pentaho.di.i18n.BaseMessages;
 import org.pentaho.di.ui.spoon.Spoon;
 import org.pentaho.reporting.engine.classic.core.MasterReport;
 import org.pentaho.reporting.engine.classic.core.modules.parser.bundle.writer.BundleWriter;
 import org.pentaho.reporting.engine.classic.extensions.datasources.pmd.PmdDataFactory;
 import org.pentaho.reporting.libraries.base.util.StringUtils;
 import org.pentaho.ui.xul.XulException;
 
 import java.io.File;
 import java.io.FileInputStream;
 import java.io.FileOutputStream;
 import java.io.FileWriter;
 import java.util.Iterator;
 
 public class PublisherHelper {
 
   private static String cachedPath;
   private static BiServerConnection cachedServer;
   
   public static String publishAnalysis(ModelerWorkspace workspace, String publishingFile,
       int treeDepth, DatabaseMeta databaseMeta, String fullPathtoFile, boolean checkDatasources, 
       boolean setShowModel, boolean showFolders, boolean showCurrentFolder, String serverPathTemplate, String extension, String databaseName ) throws ModelerException {
     try {
 
       if (StringUtils.isEmpty(publishingFile)) {
         SpoonFactory.getInstance().messageBox(BaseMessages.getString(ModelerWorkspace.class,"ModelServerPublish.Publish.UnsavedModel"), //$NON-NLS-1$
             "Dialog Error", false, Const.ERROR); //$NON-NLS-1$
         throw new ModelerException(BaseMessages.getString(ModelerWorkspace.class,"ModelServerPublish.Publish.UnsavedModel")) ;
       }
 
       ModelServerPublish publisher = new ModelServerPublish();
       publisher.setModel(workspace);
       Spoon spoon = ((Spoon) SpoonFactory.getInstance());
       try {
         XulDialogPublish publishDialog = new XulDialogPublish(spoon.getShell());
         publishDialog.setFolderTreeDepth(treeDepth);
         publishDialog.setDatabaseMeta(databaseMeta);
         
         publishDialog.setFilename(publishingFile);
         publishDialog.setCheckDatasources(checkDatasources);
 
         publishDialog.setFileMode(setShowModel);
         publishDialog.setPathTemplate(serverPathTemplate);
         publishDialog.setPath(cachedPath);
         publishDialog.setSelectedServer(cachedServer);
         publishDialog.setModelName(workspace.getModelName());
         try{
           publishDialog.showDialog();
         } catch(Exception e){
           e.printStackTrace();
         }
         if (publishDialog.isAccepted()) {
           // now try to publish
           String selectedPath = publishDialog.getPath();
           cachedPath = selectedPath;
           // we always publish to {solution}/resources/metadata
           BiServerConnection biServerConnection = publishDialog.getBiServerConnection();
           publisher.setBiServerConnection(biServerConnection);
           cachedServer = biServerConnection;
           boolean publishDatasource = publishDialog.isPublishDataSource();
           String repositoryPath = null;
           if(serverPathTemplate != null) {
             String selectedSolution = null;
             if(selectedPath.indexOf("/") != -1) { //$NON-NLS-1$
               selectedSolution = selectedPath.substring(0, selectedPath.indexOf("/")); //$NON-NLS-1$   
             } else {
               selectedSolution = selectedPath;
             }
             repositoryPath = serverPathTemplate.replace("{path}", selectedSolution); //$NON-NLS-1$
           }
           if(publishingFile.endsWith(".xmi")) { //$NON-NLS-1$
             selectedPath = repositoryPath;
           }
 
          // filename = publishDialog.getFilename();
           
           try{
             File tempDir = new File("tmp");
             if(tempDir.exists() == false){
               tempDir.mkdir();
             }
             File tempF = new File(tempDir, publishDialog.getFilename());
             if(tempF.exists() == false){
               tempF.createNewFile();
             }
 
             tempF.deleteOnExit();
             IOUtils.copy(new FileInputStream(new File(fullPathtoFile)), new FileOutputStream(tempF));
 
             replaceAttributeValue("report", "catalog", workspace.getModelName(), tempF.getAbsolutePath()); //$NON-NLS-1$ //$NON-NLS-2$
                         
             publisher
                 .publishToServer(
                     workspace.getModelName() + ".mondrian.xml", databaseName, workspace.getModelName(), repositoryPath, selectedPath, publishDatasource, true, publishDialog.isExistentDatasource(), tempF.getAbsolutePath());
           
           } finally{
             
           }
         }
       } catch (XulException e) {
         e.printStackTrace();
         SpoonFactory.getInstance().messageBox("Could not create dialog: " + e.getLocalizedMessage(), "Dialog Error", //$NON-NLS-1$ //$NON-NLS-2$
             false, Const.ERROR);
       }
     } catch (Exception e) {
       throw new ModelerException(e);
     }
     return fullPathtoFile;
   }
   
   
   private static String replaceAttributeValue(String aElement, String anAttribute, String aValue, String aFile) throws Exception {
 
     String originalValue = null;
     if(aFile != null) {
       SAXReader reader = new SAXReader();
       Document doc = reader.read(new File(aFile));
       Element root = doc.getRootElement();
 
       for ( Iterator<Element> i = root.elementIterator(); i.hasNext(); ) {
           Element element = i.next();
           if(element.getName().equals(aElement)) {
             Attribute attr = element.attribute(anAttribute);
             originalValue = attr.getValue();
             attr.setValue(aValue);
           }
       }
 
       XMLWriter writer = new XMLWriter(new FileWriter(aFile));
       writer.write(doc);
       writer.close();
     }
     return originalValue;
   }
   
   
   public static String publish(ModelerWorkspace workspace, String publishingFile,
       int treeDepth, DatabaseMeta databaseMeta, String filename, boolean checkDatasources, 
       boolean setShowModel, boolean showFolders, boolean showCurrentFolder, String serverPathTemplate, String extension, String databaseName ) throws ModelerException {
     try {
 
       if (StringUtils.isEmpty(publishingFile)) {
         SpoonFactory.getInstance().messageBox(BaseMessages.getString(ModelerWorkspace.class,"ModelServerPublish.Publish.UnsavedModel"), //$NON-NLS-1$
             "Dialog Error", false, Const.ERROR); //$NON-NLS-1$
         throw new ModelerException(BaseMessages.getString(ModelerWorkspace.class,"ModelServerPublish.Publish.UnsavedModel")) ;
       }
 
       ModelServerPublish publisher = new ModelServerPublish();
       publisher.setModel(workspace);
       Spoon spoon = ((Spoon) SpoonFactory.getInstance());
       try {
         XulDialogPublish publishDialog = new XulDialogPublish(spoon.getShell());
         publishDialog.setFolderTreeDepth(treeDepth);
         publishDialog.setDatabaseMeta(databaseMeta);
         publishDialog.setFilename(filename);
         publishDialog.setCheckDatasources(checkDatasources);
 
         publishDialog.setFileMode(setShowModel);
         publishDialog.setPathTemplate(serverPathTemplate);
         publishDialog.setPath(cachedPath);
         publishDialog.setSelectedServer(cachedServer);
         try{
           publishDialog.showDialog();
         } catch(Exception e){
           e.printStackTrace();
         }
         if (publishDialog.isAccepted()) {
           // now try to publish
           String selectedPath = publishDialog.getPath();
           cachedPath = selectedPath;
           // we always publish to {solution}/resources/metadata
           BiServerConnection biServerConnection = publishDialog.getBiServerConnection();
           cachedServer = biServerConnection;
           publisher.setBiServerConnection(biServerConnection);
           boolean publishDatasource = publishDialog.isPublishDataSource();
           String repositoryPath = null;
           if(serverPathTemplate != null) {
             String selectedSolution = null;
             if(selectedPath.indexOf("/") != -1) { //$NON-NLS-1$
               selectedSolution = selectedPath.substring(0, selectedPath.indexOf("/")); //$NON-NLS-1$   
             } else {
               selectedSolution = selectedPath;
             }
             repositoryPath = serverPathTemplate.replace("{path}", selectedSolution); //$NON-NLS-1$
           }
           if(publishingFile.endsWith(".xmi")) { //$NON-NLS-1$
             selectedPath = repositoryPath;
           }
 
           filename = publishDialog.getFilename();
 
           File tempDir = new File("tmp");
           if(tempDir.exists() == false){
             tempDir.mkdir();
           }
           File tempF = new File(tempDir, publishDialog.getFilename()+extension);
           if(tempF.exists() == false){
             tempF.createNewFile();
           }
           tempF.deleteOnExit();
           IOUtils.copy(new FileInputStream(new File(publishingFile)), new FileOutputStream(tempF));
           
           publisher
               .publishToServer(
                   filename + ".mondrian.xml", databaseName, filename, repositoryPath, selectedPath, publishDatasource, true, publishDialog.isExistentDatasource(), tempF.getAbsolutePath());
         }
       } catch (XulException e) {
         e.printStackTrace();
         SpoonFactory.getInstance().messageBox("Could not create dialog: " + e.getLocalizedMessage(), "Dialog Error", //$NON-NLS-1$ //$NON-NLS-2$
             false, Const.ERROR);
       }
     } catch (Exception e) {
       throw new ModelerException(e);
     }
     return filename;
   }
   
   public static void publishPrpt(MasterReport report, ModelerWorkspace workspace, String modelName, String prpt,
       int treeDepth, DatabaseMeta databaseMeta, String xmiFile, boolean checkDatasources, 
       boolean showServerSelection, boolean showFolders, boolean showCurrentFolder, String serverPathTemplate, String databaseName ) throws ModelerException {
     try {
 
       if (StringUtils.isEmpty(prpt)) {
         SpoonFactory.getInstance().messageBox(BaseMessages.getString(ModelerWorkspace.class,"ModelServerPublish.Publish.UnsavedModel"), //$NON-NLS-1$
             "Dialog Error", false, Const.ERROR); //$NON-NLS-1$
         return;
       }
 
       ModelServerPublish publisher = new ModelServerPublish();
       publisher.setModel(workspace);
       Spoon spoon = ((Spoon) SpoonFactory.getInstance());
       try {
         XulDialogPublish publishDialog = new XulDialogPublish(spoon.getShell());
         publishDialog.setFolderTreeDepth(treeDepth);
         publishDialog.setFileMode(true);
         publishDialog.setDatabaseMeta(databaseMeta);
         String name = prpt.substring(prpt.lastIndexOf(File.separator)+1);
         publishDialog.setFilename(name);
         publishDialog.setCheckDatasources(checkDatasources);
         publishDialog.setPathTemplate(serverPathTemplate);
         publishDialog.setPath(cachedPath);
         publishDialog.setSelectedServer(cachedServer);
         publishDialog.setModelName(workspace.getModelName());
         publishDialog.showDialog();
         if (publishDialog.isAccepted()) {
           // now try to publish
           String thePrptPublishingPath = publishDialog.getPath();
           cachedPath = thePrptPublishingPath;
           // we always publish to {solution}/resources/metadata
           BiServerConnection biServerConnection = publishDialog.getBiServerConnection();
           publisher.setBiServerConnection(biServerConnection);
 
           cachedServer = biServerConnection;
           boolean publishDatasource = publishDialog.isPublishDataSource();
           String theXmiPublishingPath = null;
           if(serverPathTemplate != null) {
             String theSolution = null;
             if(thePrptPublishingPath.indexOf("/") != -1) { //$NON-NLS-1$
               theSolution = thePrptPublishingPath.substring(0, thePrptPublishingPath.indexOf("/")); //$NON-NLS-1$   
             } else {
               theSolution = thePrptPublishingPath;
             }
             theXmiPublishingPath = serverPathTemplate.replace("{path}", theSolution);  //$NON-NLS-1$
           }
           
           // Set the domain id to the xmi.
           String theXmiFile = xmiFile.substring(xmiFile.lastIndexOf(File.separator) + 1, xmiFile.length()); //$NON-NLS-1$
           PmdDataFactory thePmdDataFactory = (PmdDataFactory) report.getDataFactory();
           String theDomainId = theXmiPublishingPath + "/" + theXmiFile; //$NON-NLS-1$
           thePmdDataFactory.setDomainId(theDomainId);
           
           
           // Point the mql query to the xmi instead of default.
           String theMQLQuery = thePmdDataFactory.getQuery("default"); //$NON-NLS-1$
           String theQuery = theMQLQuery.substring(0, theMQLQuery.lastIndexOf("default")) //$NON-NLS-1$
                             + theDomainId + theMQLQuery.substring(theMQLQuery.lastIndexOf("default") + 7, theMQLQuery.length()); //$NON-NLS-1$
           thePmdDataFactory.setQuery("default", theQuery); //$NON-NLS-1$
           
           try {
             File tempDir = new File("tmp");
             if(tempDir.exists() == false){
               tempDir.mkdir();
             }
             File tempF = new File(tempDir, publishDialog.getFilename());
             if(tempF.exists() == false){
               tempF.createNewFile();
             }
             tempF.deleteOnExit();
             BundleWriter.writeReportToZipFile(report, tempF);
             publisher.publishPrptToServer(theXmiPublishingPath, thePrptPublishingPath, publishDatasource, publishDialog.isExistentDatasource(), publishDialog.isPublishXmi(), xmiFile, tempF.getAbsolutePath()); 
             
           } catch (Exception e) {
             throw new ModelerException(e);
           } finally {
             thePmdDataFactory.setQuery("default", theMQLQuery); //$NON-NLS-1$
           }
         }
       } catch (XulException e) {
         e.printStackTrace();
         SpoonFactory.getInstance().messageBox("Could not create dialog: " + e.getLocalizedMessage(), "Dialog Error", //$NON-NLS-1$ //$NON-NLS-2$
             false, Const.ERROR); 
       }
     } catch (Exception e) {
       throw new ModelerException(e);
     }
   }
 
 }
