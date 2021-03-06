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
 
 package org.eclipse.mylar.tasklist.tests;
 
 import junit.framework.Test;
 import junit.framework.TestSuite;
 
import org.eclipse.mylar.tasklist.tests.bugzilla.BugzillaSearchPluginTest;

 /**
  * @author Mik Kersten
  */
 public class AllTasklistTests {
 
 	public static Test suite() {
 		TestSuite suite = new TestSuite(
 				"Test for org.eclipse.mylar.tasklist.bugzilla.tests");
 		//$JUnit-BEGIN$
         suite.addTestSuite(TaskListManagerTest.class);
         suite.addTestSuite(TaskListUiTest.class);
//		suite.addTestSuite(BugzillaStackTraceTest.class);
 		suite.addTestSuite(BugzillaSearchPluginTest.class);
 		//$JUnit-END$
 		return suite;
 	}
 
 }
