 /*******************************************************************************
  * Copyright (c) 2014 École Polytechnique de Montréal
  *
  * All rights reserved. This program and the accompanying materials are made
  * available under the terms of the Eclipse Public License v1.0 which
  * accompanies this distribution, and is available at
  * http://www.eclipse.org/legal/epl-v10.html
  *
  * Contributors:
  *   Geneviève Bastien - Initial API and implementation
  *******************************************************************************/
 
 package org.eclipse.linuxtools.tmf.analysis.xml.core.tests.module;
 
 import static org.junit.Assert.assertEquals;
 import static org.junit.Assert.assertFalse;
 import static org.junit.Assert.assertNotNull;
 import static org.junit.Assert.assertTrue;
 import static org.junit.Assert.fail;
 
 import java.io.File;
 import java.util.List;
 
 import org.eclipse.core.resources.IWorkspace;
 import org.eclipse.core.resources.ResourcesPlugin;
 import org.eclipse.core.runtime.IPath;
 import org.eclipse.core.runtime.IStatus;
 import org.eclipse.jdt.annotation.NonNull;
 import org.eclipse.linuxtools.tmf.analysis.xml.core.module.XmlUtils;
 import org.eclipse.linuxtools.tmf.analysis.xml.core.stateprovider.TmfXmlStrings;
 import org.eclipse.linuxtools.tmf.analysis.xml.core.tests.common.TmfXmlTestFiles;
 import org.junit.After;
 import org.junit.Test;
 import org.w3c.dom.Element;
 
 /**
  * Tests for the {@link XmlUtils} class
  *
  * @author Geneviève Bastien
  */
 public class XmlUtilsTest {
 
     /**
      * Empty the XML directory after the test
      */
     @After
     public void emptyXmlFolder() {
         File fFolder = XmlUtils.getXmlFilesPath().toFile();
         if (!(fFolder.isDirectory() && fFolder.exists())) {
             return;
         }
         for (File xmlFile : fFolder.listFiles()) {
             xmlFile.delete();
         }
     }
 
     /**
      * Test the {@link XmlUtils#getXmlFilesPath()} method
      */
     @Test
     public void testXmlPath() {
         IPath xmlPath = XmlUtils.getXmlFilesPath();
 
         IWorkspace workspace = ResourcesPlugin.getWorkspace();
         IPath workspacePath = workspace.getRoot().getRawLocation();
         workspacePath = workspacePath.addTrailingSeparator()
                 .append(".metadata").addTrailingSeparator().append(".plugins")
                 .addTrailingSeparator()
                 .append("org.eclipse.linuxtools.tmf.analysis.xml.core")
                 .addTrailingSeparator().append("xml_files");
 
         assertEquals(xmlPath, workspacePath);
     }
 
     /**
      * test the {@link XmlUtils#xmlValidate(File)} method
      */
     @Test
     public void testXmlValidate() {
         File testXmlFile = TmfXmlTestFiles.VALID_FILE.getFile();
         if ((testXmlFile == null) || !testXmlFile.exists()) {
             fail("XML test file does not exist");
         }
         IStatus status = XmlUtils.xmlValidate(testXmlFile);
         if (!status.isOK()) {
             fail(status.getMessage());
         }
 
         testXmlFile = TmfXmlTestFiles.INVALID_FILE.getFile();
         if ((testXmlFile == null) || !testXmlFile.exists()) {
             fail("XML test file does not exist");
         }
         assertFalse(XmlUtils.xmlValidate(testXmlFile).isOK());
     }
 
     /**
      * test the {@link XmlUtils#addXmlFile(File)} method
      */
     @Test
     public void testXmlAddFile() {
         /* Check the file does not exist */
         IPath xmlPath = XmlUtils.getXmlFilesPath().addTrailingSeparator().append("test_valid.xml");
         File destFile = xmlPath.toFile();
         assertFalse(destFile.exists());
 
         /* Add test_valid.xml file */
         File testXmlFile = TmfXmlTestFiles.VALID_FILE.getFile();
         if ((testXmlFile == null) || !testXmlFile.exists()) {
             fail("XML test file does not exist");
         }
 
         XmlUtils.addXmlFile(testXmlFile);
         assertTrue(destFile.exists());
     }
 
     @NonNull private static final String ANALYSIS_ID = "kernel.linux.sp";
 
     /**
      * Test the {@link XmlUtils#getElementInFile(String, String, String)} method
      */
     @Test
     public void testGetElementInFile() {
         File testXmlFile = TmfXmlTestFiles.VALID_FILE.getFile();
         if ((testXmlFile == null) || !testXmlFile.exists()) {
             fail("XML test file does not exist");
         }
         /*
          * This sounds useless, but I get a potential null pointer warning
          * otherwise
          */
         if (testXmlFile == null) {
             return;
         }
 
         Element analysis = XmlUtils.getElementInFile(testXmlFile.getAbsolutePath(), TmfXmlStrings.STATE_PROVIDER, ANALYSIS_ID);
         assertNotNull(analysis);
     }
 
     /**
      * Test the {@link XmlUtils#getChildElements(Element)} and
      * {@link XmlUtils#getChildElements(Element, String)} methods
      */
     @Test
     public void testGetChildElements() {
         File testXmlFile = TmfXmlTestFiles.VALID_FILE.getFile();
         if ((testXmlFile == null) || !testXmlFile.exists()) {
             fail("XML test file does not exist");
         }
         /*
          * This sounds useless, but I get a potential null pointer warning
          * otherwise
          */
         if (testXmlFile == null) {
             return;
         }
 
         Element analysis = XmlUtils.getElementInFile(testXmlFile.getAbsolutePath(), TmfXmlStrings.STATE_PROVIDER, ANALYSIS_ID);
 
        List<Element> values = XmlUtils.getChildElements(analysis, TmfXmlStrings.DEFINED_VALUE);
        assertEquals(12, values.size());
 
         values = XmlUtils.getChildElements(analysis, TmfXmlStrings.HEAD);
         assertEquals(1, values.size());
 
         Element head = values.get(0);
         values = XmlUtils.getChildElements(head);
         assertEquals(2, values.size());
 
     }
 
 }
