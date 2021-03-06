 package org.codehaus.surefire.battery.jython;
 
 import org.codehaus.plexus.util.DirectoryScanner;
 import org.codehaus.surefire.report.ReportEntry;
 import org.codehaus.surefire.report.ReportManager;
import org.codehaus.surefire.AbstractBattery;
 import org.python.core.PyList;
 import org.python.core.PyStringMap;
 import org.python.util.PythonInterpreter;
 
 import java.io.File;
 import java.util.ArrayList;
 import java.util.Iterator;
 
 public class JythonBattery
     extends AbstractBattery
 {
     private PythonInterpreter interp;
 
     private ArrayList testMethods;
 
     private DirectoryScanner scanner;
 
     private File directory;
 
     public JythonBattery()
     {
         interp = new PythonInterpreter();
 
         testMethods = new ArrayList();
 
         scanner = new DirectoryScanner();
 
         directory = new File( "py" );
     }
 
     public void execute( ReportManager reportManager )
     {
         scanner.setBasedir( directory );
 
         scanner.setIncludes( new String[]{ "**/*Test.py" } );
 
         scanner.scan();
 
         String files[] = scanner.getIncludedFiles();
 
         for ( int i = 0; i < files.length; i++ )
         {
             String f = files[i];
 
             process( f, reportManager );
         }
     }
 
     public void process( String pyScript, ReportManager reportManager )
     {
         interp.execfile( ( new File( directory, pyScript ) ).getPath() );
 
         interp.exec( "battery = " + pyScript.substring( 0, pyScript.indexOf( "." ) ) + "()" );
 
         PyList l = ( (PyStringMap) interp.get( "battery" ).__findattr__( "__class__" ).__findattr__( "__dict__" ) ).keys();
 
         int j = l.__len__();
 
         for ( int i = 0; i < j; i++ )
         {
             String s = l.pop().toString();
 
             if ( s.startsWith( "test" ) )
             {
                 testMethods.add( s );
             }
         }
 
         executeTestMethods( reportManager );
     }
 
     protected void executeTestMethods( ReportManager ReportManager )
     {
         String testMethod;
 
         for ( Iterator i = testMethods.iterator(); i.hasNext(); )
         {
             testMethod = "battery." + (String) i.next() + "()";
 
             testMethod( ReportManager, testMethod );
         }
 
         testMethods.clear();
     }
 
     protected void testMethod( ReportManager reportManager, String method )
     {
         ReportEntry reportEntry = new ReportEntry( this, method, "starting" );
 
         reportManager.testStarting( reportEntry );
 
         try
         {
             interp.exec( method );
 
             reportEntry = new ReportEntry( this, method, "succeeded" );
 
             reportManager.testSucceeded( reportEntry );
         }
         catch ( Exception e )
         {
             e.printStackTrace();
 
             reportEntry = new ReportEntry( this, method, "failed: " + e.getMessage() );
 
             reportManager.testFailed( reportEntry );
         }
     }
 }
