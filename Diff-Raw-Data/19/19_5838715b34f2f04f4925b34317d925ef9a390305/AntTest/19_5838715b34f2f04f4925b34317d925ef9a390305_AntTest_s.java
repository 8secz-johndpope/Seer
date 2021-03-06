 /**
  * <copyright>
  *
  * Copyright (c) 2005 IBM Corporation and others.
  * All rights reserved.   This program and the accompanying materials
  * are made available under the terms of the Common Public License v1.0
  * which accompanies this distribution, and is available at
  * http://www.eclipse.org/legal/cpl-v10.html
  * 
  * Contributors: 
  *   IBM - Initial API and implementation
  *
  * </copyright>
  *
 * $Id: AntTest.java,v 1.3 2005/02/11 04:41:21 marcelop Exp $
  */
 package org.eclipse.emf.test.tools.ant;
 
 import java.io.File;
 
 import junit.framework.Test;
 import junit.framework.TestCase;
 import junit.framework.TestSuite;
 
 import org.eclipse.core.runtime.CoreException;
 import org.eclipse.core.runtime.Path;
 
 import org.eclipse.emf.test.tools.TestUtil;
 
 public class AntTest extends TestCase
 {
   public static final String TEST_TOKEN = "@TEST_TOKEN@";
   
   private static final File EMF_ANT_PLUGIN_DIR = new File(TestUtil.getPluginDirectory("org.eclipse.emf.ant"));
   private static final File EXAMPLES_COPY_DIR = new File(TestUtil.getPluginDirectory() + "/ant.example.tmp");
   private static final File EXPECTED_DIR = new File(TestUtil.getPluginDirectory() + "/data/ant.expected");
   
   
   /**
    * @param name
    */
   public AntTest(String name)
   {
     super(name);
   }
 
   public static Test suite()
   {
     TestSuite ts = new TestSuite("EMFAntTest");
     ts.addTest(new AntTest("suiteSetUp"));
     ts.addTest(new AntTest("testJET"));
     ts.addTest(new AntTest("testJMerger"));
     ts.addTest(new AntTest("testRose"));
     ts.addTest(new AntTest("testXSD"));
     ts.addTest(new AntTest("testXSDs"));
     ts.addTest(new AntTest("suiteTearDown"));
     return ts;
   }
   
   public void suiteSetUp() throws Exception
   {
     assertTrue(EMF_ANT_PLUGIN_DIR.getAbsolutePath() + " doesn't exist", EMF_ANT_PLUGIN_DIR.isDirectory());
     assertTrue(EXPECTED_DIR.getAbsolutePath() + " doesn't exist", EXPECTED_DIR.isDirectory());
     
     File examplesDir = new File(EMF_ANT_PLUGIN_DIR, "examples");
     assertTrue(examplesDir.getAbsolutePath() + " doesn't exist", examplesDir.isDirectory());
 
     TestUtil.delete(EXAMPLES_COPY_DIR);
     assertFalse(EXAMPLES_COPY_DIR.exists());
     assertTrue(EXAMPLES_COPY_DIR.mkdir());
     
     TestUtil.copyFiles(examplesDir, EXAMPLES_COPY_DIR);
 
     File libraryDir = new File(EXAMPLES_COPY_DIR, "library");
     TestUtil.copyFiles(libraryDir, new File(EXAMPLES_COPY_DIR, "library.xsd"));
     TestUtil.copyFiles(libraryDir, new File(EXAMPLES_COPY_DIR, "library.xsds"));
     assertTrue(libraryDir.renameTo(new File(EXAMPLES_COPY_DIR, "library.rose")));
   }
   
   public void suiteTearDown() throws Exception
   {
     TestUtil.delete(EXAMPLES_COPY_DIR);
   }
   
   public void testJET() throws Exception
   {
     File rootDir = new File(EXAMPLES_COPY_DIR, "jet");
     File rootExpectedDir = new File(EXPECTED_DIR, "jet");
     File antScript = new File(rootDir, "build.xml");
     runAntAndTest(rootDir, rootExpectedDir, antScript, "genClasses", null);
   }
   
   public void testJMerger() throws Exception
   {
     File rootDir = new File(EXAMPLES_COPY_DIR, "jmerger");
     File rootExpectedDir = new File(EXPECTED_DIR, "jmerger");
     File antScript = new File(rootDir, "build.xml");
     
     TestUtil.runAnt(antScript, "createNewFile");
     assertGeneratedFile(rootDir, rootExpectedDir, "NewFile.java", null);
     TestUtil.runAnt(antScript, "overwriteTarget");
     assertGeneratedFile(rootDir, rootExpectedDir, "Target.java", null);
   }
 
   public void testRose() throws Exception
   {
     File rootDir = new File(EXAMPLES_COPY_DIR, "library.rose");
     File rootExpectedDir = new File(EXPECTED_DIR, "library.rose");
     File antScript = new File(rootDir, "build/codeGenFromRose.xml");
     
     String[] testTokenReplacements = new String[2];
     testTokenReplacements[0] = new Path(EXAMPLES_COPY_DIR.getAbsolutePath()).toString();
     testTokenReplacements[1] = new File(EXAMPLES_COPY_DIR, "library.rose/model/library.mdl").getAbsolutePath();
            
     runAntAndTest(rootDir, rootExpectedDir, antScript, null, testTokenReplacements);
   }
 
   public void testXSD() throws Exception
   {
     File rootDir = new File(EXAMPLES_COPY_DIR, "library.xsd");
     File rootExpectedDir = new File(EXPECTED_DIR, "library.xsd");
     File antScript = new File(rootDir, "build/codeGenFromXSD.xml");
 
     String[] testTokenReplacements = new String[1];
     testTokenReplacements[0] = new Path(EXAMPLES_COPY_DIR.getAbsolutePath()).toString();
            
     runAntAndTest(rootDir, rootExpectedDir, antScript, null, testTokenReplacements);
   }
 
   public void testXSDs() throws Exception
   {
     File rootDir = new File(EXAMPLES_COPY_DIR, "library.xsds");
     File rootExpectedDir = new File(EXPECTED_DIR, "library.xsds");
     File antScript = new File(rootDir, "build/codeGenFromMultipleXSD.xml");
 
     String[] testTokenReplacements = new String[1];
     testTokenReplacements[0] = new Path(EXAMPLES_COPY_DIR.getAbsolutePath()).toString();
            
     runAntAndTest(rootDir, rootExpectedDir, antScript, null, testTokenReplacements);
   }
   
   private void runAntAndTest(File rootDir, File rootExpectedDir, File antScript, String antScriptArguments, String[] testTokenReplacements) throws CoreException
   {
     assertTrue(rootDir.getAbsolutePath() + " doesn't exist", rootDir.isDirectory());
     assertTrue(rootExpectedDir.getAbsolutePath() + " doesn't exist", rootExpectedDir.isDirectory());
     assertTrue(antScript.getAbsolutePath() + " doesn't exist", antScript.isFile());
     
     TestUtil.runAnt(antScript, antScriptArguments);   
     assertGeneratedFiles(rootDir, rootExpectedDir, null, testTokenReplacements);    
   }
    
   /**
    * Compares each expected file in rootExpectedDir with the equivalent file in  rootDir.
    * @param rootDir
    * @param rootExpectedDir
    * @param expectedDir
    */
   private void assertGeneratedFiles(File rootDir, File rootExpectedDir, File expectedDir, String[] testTokenReplacements)
   {
     if (expectedDir == null) expectedDir = rootExpectedDir;
       
     File[] children = expectedDir.listFiles();
     for (int i = 0, maxi = children.length; i < maxi; i++)
     {
       if (children[i].isFile())
       {
         String file = children[i].getAbsolutePath().substring(rootExpectedDir.getAbsolutePath().length());
         assertGeneratedFile(rootDir, rootExpectedDir, file, testTokenReplacements);
       }
       else if (children[i].isDirectory())
       {
         if (!"CVS".equals(children[i].getName()))
         {
           assertGeneratedFiles(rootDir, rootExpectedDir, children[i], testTokenReplacements);
         }
       }
     }    
   }
   
   private void assertGeneratedFile(File dir, File expectedDir, String file, String[] testTokenReplacements)
   {
     File expectedFile = new File(expectedDir, file);
     File generatedFile = new File(dir, file);
     
     assertTrue("File: " + expectedFile, expectedFile.isFile());
     assertTrue("File: " + generatedFile, generatedFile.isFile());
     
     String expectedContent = TestUtil.readFile(expectedFile, true);
     if (testTokenReplacements != null)
     {
       for (int i=testTokenReplacements.length-1; i >= 0; i--)
       {
         String replacement = testTokenReplacements[i];
         expectedContent = expectedContent.replaceAll(TEST_TOKEN+i, replacement.replaceAll("\\\\", "\\\\\\\\"));
       }
     }
     String generatedContent = TestUtil.readFile(generatedFile, true);
     
     //Remove CVS tags
     expectedContent = expectedContent.replaceAll("\\$Id.*\\$", "");
     generatedContent = generatedContent.replaceAll("\\$Id.*\\$", "");
     
     assertEquals("File: " + file, expectedContent, generatedContent);
   }  
 }
