 /*******************************************************************************
  * Copyright (c) 2004 - 2005 University Of British Columbia and others.
  * All rights reserved. This program and the accompanying materials
  * are made available under the terms of the Eclipse Public License v1.0
  * which accompanies this distribution, and is available at
  * http://www.eclipse.org/legal/epl-v10.html
  *
  * Contributors:
  *     University Of British Columbia - initial API and implementation
  *******************************************************************************/
 
 package org.eclipse.mylar.tests;
 
 import junit.framework.Test;
 import junit.framework.TestSuite;
 
 import org.eclipse.mylar.bugzilla.test.AllBugzillaTests;
 import org.eclipse.mylar.core.tests.AllCoreTests;
 import org.eclipse.mylar.java.tests.AllJavaTests;
 import org.eclipse.mylar.monitor.tests.AllMonitorTests;
 import org.eclipse.mylar.tasklist.tests.AllTasklistTests;
 import org.eclipse.mylar.xml.tests.AllXmlTests;
 	
 /**
  * @author Mik Kersten
  */
 public class AllTests {
 
     public static Test suite() {
         TestSuite suite = new TestSuite("Test for org.eclipse.mylar.tests");
         //$JUnit-BEGIN$
         
         // NOTE: the order of these tests matters
         // TODO: make tests clear workbench state on completion
        suite.addTest(AllMonitorTests.suite());
         suite.addTest(AllXmlTests.suite());  // HACK: first because it doesn't clean up properly
         suite.addTest(AllJavaTests.suite());
         suite.addTest(AllCoreTests.suite());
         suite.addTest(AllTasklistTests.suite());
         suite.addTest(AllBugzillaTests.suite());
         suite.addTest(MiscTests.suite());
         //$JUnit-END$
         return suite;
     }
 }
