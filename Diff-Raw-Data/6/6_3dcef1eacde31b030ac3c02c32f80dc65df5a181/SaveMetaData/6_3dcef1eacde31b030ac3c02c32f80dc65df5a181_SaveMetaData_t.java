 /*
  * $Id$
  *
  * Copyright (c) 2008 by Brent Easton and Joel Uckelman
  *
  * This library is free software; you can redistribute it and/or
  * modify it under the terms of the GNU Library General Public
  * License (LGPL) as published by the Free Software Foundation.
  *
  * This library is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
  * Library General Public License for more details.
  *
  * You should have received a copy of the GNU Library General Public
  * License along with this library; if not, copies are available
  * at http://www.opensource.org.
  */
 package VASSAL.build.module;
 
 import java.io.IOException;
 import java.io.InputStream;
 import java.util.zip.ZipEntry;
 import java.util.zip.ZipFile;
 
 import javax.swing.JOptionPane;
 
 import org.w3c.dom.Document;
 import org.w3c.dom.Element;
 import org.xml.sax.InputSource;
 import org.xml.sax.SAXException;
 import org.xml.sax.XMLReader;
 import org.xml.sax.helpers.XMLReaderFactory;
 
 import VASSAL.build.GameModule;
 import VASSAL.i18n.Resources;
 import VASSAL.tools.ArchiveWriter;
 
 /**
  * 
  * Class representing the metadata for a Save Game/Log File. Details
  * about the module this savegame was created with are saved in a
  * seperate moduledata file in the saved game zip.
  * 
  * @author Brent Easton
  * @since 3.1.0
  *
  */
 public class SaveMetaData extends AbstractMetaData {
 
   public static final String ZIP_ENTRY_NAME = "savedata";
   public static final String DATA_VERSION = "1";
 
   protected ModuleMetaData moduleData;
 
   public SaveMetaData() {
     String comments  = (String) JOptionPane.showInputDialog(
         GameModule.getGameModule().getFrame(),
         Resources.getString("BasicLogger.enter_comments"),
         Resources.getString("BasicLogger.log_file_comments"),
         JOptionPane.PLAIN_MESSAGE,
         null,
         null,
         "");
     setDescription(new Attribute(DESCRIPTION_ELEMENT, comments));
   }
   
   public SaveMetaData(ZipFile zip) {
     read(zip);
   }
   
   public String getModuleName() {
     return moduleData == null ? "" : moduleData.getName();
   }
   
   public String getModuleVersion() {
     return moduleData == null ? "" : moduleData.getVersion();
   }
   
   public String getZipEntryName() {
     return ZIP_ENTRY_NAME;
   }
   
   public String getMetaDataVersion() {
     return DATA_VERSION;
   }
 
   /**
    * Write Save Game metadata to the specified Archive
    * @param archive Save game Archive
    * @throws IOException If anything goes wrong
    */
   public void save(ArchiveWriter archive) throws IOException {
     super.save(archive);
     
     // Also save a copy of the current module metadata in the save file. Copy
     // module metadata from the module archive as it will contain full i18n
     // information.
     InputStream in = null;
     try {
       in = GameModule.getGameModule().getDataArchive().getFileStream(ModuleMetaData.ZIP_ENTRY_NAME);
       archive.addFile(ModuleMetaData.ZIP_ENTRY_NAME, in);
    }
    catch (IOException e) {
      // No Metatdata in source module, create a fresh copy
      ModuleMetaData moduleData = new ModuleMetaData(GameModule.getGameModule());
      moduleData.save(archive);
     }
     finally {
       if (in != null) {
         in.close();
       }
     }
   }
   
   /**
    * Add Elements specific to SaveMetaData
    */
   protected void addElements(Document doc, Element root) {
     return;
   }
 
   /**
    * Read and validate a Saved Game/Log file.
    *  - Check it has a Zip Entry named savedgame
    *  - If it has a metadata file, read and parse it.
    *  - 
    * @param file Saved Game File
    */
   public void read(ZipFile zip) {
 
     InputStream is = null;
     try {
       // Try to parse the metadata. Failure is not catastrophic, we can
       // treat it like an old-style save with no metadata.
       try {
         final ZipEntry data = zip.getEntry(SaveMetaData.ZIP_ENTRY_NAME);
         if (data == null) return;
 
         final XMLReader parser = XMLReaderFactory.createXMLReader();
 
         // set up the handler
         final XMLHandler handler = new XMLHandler();
         parser.setContentHandler(handler);
         parser.setDTDHandler(handler);
         parser.setEntityResolver(handler);
         parser.setErrorHandler(handler);
 
         // parse! parse!
         is = zip.getInputStream(data);
         parser.parse(new InputSource(is));
         
         // read the matching Module data
         moduleData = new ModuleMetaData(zip); 
       }
       catch (IOException e) {
         e.printStackTrace();
       }
       catch (SAXException e) {
         e.printStackTrace();
       }
     }
     finally {
       if (zip != null) {
         try {
           zip.close();
         }
         catch (IOException e) {
           e.printStackTrace();
         }
       }
       
       if (is != null) {
         try {
           is.close();
         }
         catch (IOException e) {
           e.printStackTrace();
         }
       }
     }
   }
 }
