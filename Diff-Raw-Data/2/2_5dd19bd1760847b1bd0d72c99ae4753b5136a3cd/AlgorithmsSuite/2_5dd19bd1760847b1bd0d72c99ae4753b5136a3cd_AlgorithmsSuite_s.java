 /*
  * ProjectionTestSuite.java
  * JUnit based test
  *
  * Created on February 22, 2002, 3:58 PM
  */                
 
 package org.geotools.algorithms;
 
 import junit.framework.*;
 
 /**
  *
  * @author jamesm
  */                                
 public class AlgorithmsSuite extends TestCase {
     
     public AlgorithmsSuite(java.lang.String testName) {
         super(testName);
     }        
     
     public static void main(java.lang.String[] args) {
         junit.textui.TestRunner.run(suite());
     }
     
     public static Test suite() {
         TestSuite suite = new TestSuite("All Tests");
        //suite.addTestSuite(GeometryProperitesTest.class);
         return suite;
     }
 }
