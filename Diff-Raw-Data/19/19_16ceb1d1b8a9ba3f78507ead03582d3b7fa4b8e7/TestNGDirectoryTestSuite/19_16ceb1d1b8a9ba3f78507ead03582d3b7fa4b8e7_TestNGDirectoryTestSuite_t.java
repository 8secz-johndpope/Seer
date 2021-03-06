 package org.apache.maven.surefire.testng;
 
 /*
  * Copyright 2001-2006 The Apache Software Foundation.
  *
  * Licensed under the Apache License, Version 2.0 (the "License");
  * you may not use this file except in compliance with the License.
  * You may obtain a copy of the License at
  *
  *      http://www.apache.org/licenses/LICENSE-2.0
  *
  * Unless required by applicable law or agreed to in writing, software
  * distributed under the License is distributed on an "AS IS" BASIS,
  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  * See the License for the specific language governing permissions and
  * limitations under the License.
  */
 
 import org.apache.maven.surefire.report.ReporterException;
 import org.apache.maven.surefire.report.ReporterManager;
 import org.apache.maven.surefire.suite.AbstractDirectoryTestSuite;
 import org.apache.maven.surefire.testset.SurefireTestSet;
 import org.apache.maven.surefire.testset.TestSetFailedException;
 import org.testng.ISuiteListener;
 import org.testng.ITestListener;
 import org.testng.TestNG;
 import org.testng.internal.TestNGClassFinder;
 import org.testng.internal.annotations.IAnnotationFinder;
 import org.testng.xml.XmlClass;
 import org.testng.xml.XmlSuite;
 import org.testng.xml.XmlTest;
 
 import java.io.File;
 import java.util.ArrayList;
import java.util.Arrays;
 import java.util.Collections;
 import java.util.Iterator;
 import java.util.Map;
 
 /**
  * Test suite for TestNG based on a directory of Java test classes. Can also execute JUnit tests.
  *
  * @author <a href="mailto:brett@apache.org">Brett Porter</a>
  */
 public class TestNGDirectoryTestSuite
     extends AbstractDirectoryTestSuite
 {
     private String groups;
 
     private String excludedGroups;
 
     private boolean parallel;
 
     private int threadCount;
 
     private String testSourceDirectory;
 
     private IAnnotationFinder annotationFinder;
 
     public TestNGDirectoryTestSuite( File basedir, ArrayList includes, ArrayList excludes, String groups,
                                      String excludedGroups, Boolean parallel, Integer threadCount,
                                      String testSourceDirectory )
         throws IllegalAccessException, InstantiationException, ClassNotFoundException
     {
         super( basedir, includes, excludes );
 
         this.groups = groups;
 
         this.excludedGroups = excludedGroups;
 
         this.parallel = parallel.booleanValue();
 
         this.threadCount = threadCount.intValue();
 
         this.testSourceDirectory = testSourceDirectory;
 
         Class annotationClass;
         try
         {
             annotationClass = Class.forName( "org.testng.internal.annotations.JDK15AnnotationFinder" );
         }
         catch ( ClassNotFoundException e )
         {
             annotationClass = Class.forName( "org.testng.internal.annotations.JDK14AnnotationFinder" );
         }
 
         annotationFinder = (IAnnotationFinder) annotationClass.newInstance();
     }
 
     public Map locateTestSets( ClassLoader classLoader )
         throws TestSetFailedException
     {
         // TODO: fix
         // override classloader. That keeps us all together for now, which makes it work, but could pose problems of
         // classloader separation if the tests use plexus-utils.
         return super.locateTestSets( getClass().getClassLoader() );
     }
 
     protected SurefireTestSet createTestSet( Class testClass, ClassLoader classLoader )
     {
         return new TestNGTestSet( testClass );
     }
 
     public void execute( ReporterManager reporterManager, ClassLoader classLoader )
         throws ReporterException, TestSetFailedException
     {
         if ( testSets == null )
         {
             throw new IllegalStateException( "You must call locateTestSets before calling execute" );
         }
 
         XmlSuite suite = new XmlSuite();
         suite.setParallel( parallel );
         suite.setThreadCount( threadCount );
 
         for ( Iterator i = testSets.values().iterator(); i.hasNext(); )
         {
             SurefireTestSet testSet = (SurefireTestSet) i.next();
 
             XmlTest xmlTest = new XmlTest( suite );
             xmlTest.setName( testSet.getName() );
             xmlTest.setXmlClasses( Collections.singletonList( new XmlClass( testSet.getTestClass() ) ) );
            if ( groups != null )
            {
                xmlTest.setIncludedGroups( Arrays.asList( groups.split( "," ) ) );
            }
            if ( excludedGroups != null )
            {
                xmlTest.setExcludedGroups( Arrays.asList( excludedGroups.split( "," ) ) );
            }
 
             if ( !TestNGClassFinder.isTestNGClass( testSet.getTestClass(), annotationFinder ) )
             {
                 xmlTest.setJUnit( true );
             }
         }
 
         TestNG testNG = new TestNG();
         // turn off all TestNG output
         testNG.setVerbose( 0 );
 
         // TODO: check these work, otherwise put them in the xmlTest instances
         testNG.setXmlSuites( Collections.singletonList( suite ) );
 
         testNG.setListenerClasses( new ArrayList() );
 
         TestNGReporter reporter = new TestNGReporter( reporterManager, this );
         testNG.addListener( (ITestListener) reporter );
         testNG.addListener( (ISuiteListener) reporter );
 
         // Set source path so testng can find javadoc annotations if not in 1.5 jvm
         if ( testSourceDirectory != null )
         {
             testNG.setSourcePath( testSourceDirectory );
         }
 
         testNG.runSuitesLocally();
     }
 }
