 package org.eclipse.team.tests.ccvs.core.cvsresources;
 /*
  * (c) Copyright IBM Corp. 2000, 2002.
  * All Rights Reserved.
  */
 import junit.awtui.TestRunner;
 import junit.framework.Test;
 import junit.framework.TestSuite;
 
 
public class AllTests extends TestSuite {


	public static void main(String[] args) {
		
 		TestRunner.run(AllTests.class);
 	}
 	
 	public static Test suite() {	
 		TestSuite suite = new TestSuite();
 		
 		suite.addTest(LocalFileTest.suite());
 		suite.addTest(LocalFolderTest.suite());
 		suite.addTest(ResourceSyncInfoTest.suite());
 		suite.addTest(SynchronizerTest.suite());
     	return suite; 	
 	}	
 	
 	public AllTests(String name) {
 		super(name);
 	}
 	
 	public AllTests() {
 		super();
 	}
 }
 
 
