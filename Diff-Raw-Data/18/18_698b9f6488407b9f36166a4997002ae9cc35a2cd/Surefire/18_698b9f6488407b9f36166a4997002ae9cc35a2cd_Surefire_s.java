 package org.codehaus.surefire;
 
 import org.codehaus.surefire.battery.Battery;
 import org.codehaus.surefire.battery.JUnitBattery;
 import org.codehaus.surefire.report.Report;
 import org.codehaus.surefire.report.ReportEntry;
 import org.codehaus.surefire.report.ReportManager;
 
 import java.lang.reflect.Constructor;
 import java.util.ArrayList;
 import java.util.Iterator;
 import java.util.List;
 import java.util.ResourceBundle;
 
 public class Surefire
 {
     private static ResourceBundle resources = ResourceBundle.getBundle( "org.codehaus.surefire.surefire" );
 
     private List batteryHolders;
 
     private List reports;
 
     private ReportManager reportManager;
 
     private ClassLoader classLoader;
 
     public Surefire()
     {
     }
 
     public static ResourceBundle getResources()
     {
         return resources;
     }
 
     public boolean run( List reports, List batteryHolders, ClassLoader classLoader )
         throws Exception
     {
         if ( reports == null || batteryHolders == null || classLoader == null )
         {
             throw new NullPointerException();
         }
 
         if ( batteryHolders.size() == 0 )
         {
             throw new IllegalArgumentException();
         }
 
         this.batteryHolders = batteryHolders;
 
         this.reports = reports;
 
         this.classLoader = classLoader;
 
         return run();
     }
 
     public boolean run()
         throws Exception
     {
         List batts = instantiateBatteries( batteryHolders, classLoader );
 
         reportManager = new ReportManager( instantiateReports( reports, classLoader ) );
 
         try
         {
             reportManager.runStarting( 100 );
 
             if ( batts.size() > 0 )
             {
                 int nbTests = 0;
 
                 for ( Iterator i = batts.iterator(); i.hasNext(); )
                 {
                     Battery battery = (Battery) i.next();
 
                     if ( battery.getTestCount() > 0 )
                     {
                        nbTests += battery.getTestCount();

                         executeBattery( battery, reportManager );
                     }
 
                     List list = new ArrayList();
 
                     for ( Iterator j = battery.getSubBatteryClassNames().iterator(); j.hasNext(); )
                     {
                         String s = (String) j.next();
 
                         list.add( new Object[]{s, null} );
                     }
 
                     List subBatteries = instantiateBatteries( list, classLoader );
 
                     for ( Iterator j = subBatteries.iterator(); j.hasNext(); )
                     {
                         Battery b = (Battery) j.next();
     
                         if (b.getTestCount() > 0 )
                         {
                            nbTests += battery.getTestCount();

                             executeBattery( b, reportManager );
                         }
                     }
                 }
 
                 if ( nbTests == 0 )
                 {
                     reportManager.writeMessage( "There are no test to run." );
                 }
             }
             else
             {
                 reportManager.writeMessage( "There are no battery to run." );
             }
 
             reportManager.runCompleted();
         }
         catch ( Throwable ex )
         {
             ReportEntry report = new ReportEntry( ex, "org.codehaus.surefire.Runner", Surefire.getResources().getString( "bigProblems" ), ex );
 
             reportManager.runAborted( report );
         }
 
         reportManager.resume();
 
         if ( reportManager.getNbErrors() > 0 || reportManager.getNbFailures() > 0 )
         {
             return false;
         }
         return true;
     }
 
     public void executeBattery( Battery battery, ReportManager reportManager )
         throws Exception
     {
         try
         {
             String rawString = Surefire.getResources().getString( "suiteExecutionStarting" );
 
             ReportEntry report = new ReportEntry( this, battery.getBatteryName(), rawString );
 
             reportManager.batteryStarting( report );
 
             try
             {
                 battery.execute( reportManager );
 
                 rawString = Surefire.getResources().getString( "suiteCompletedNormally" );
 
                 report = new ReportEntry( this, battery.getBatteryName(), rawString );
 
                 reportManager.batteryCompleted( report );
             }
             catch ( RuntimeException e )
             {
                 rawString = Surefire.getResources().getString( "executeException" );
 
                 report = new ReportEntry( this, battery.getBatteryName(), rawString, e );
 
                 reportManager.batteryAborted( report );
             }
 
             reportManager.runCompleted();
         }
 
         catch ( Throwable ex )
         {
             ReportEntry report = new ReportEntry( ex, "org.codehaus.surefire.Runner", Surefire.getResources().getString( "bigProblems" ), ex );
 
             reportManager.runAborted( report );
         }
     }
 
     public static List instantiateBatteries( List batteryHolders, ClassLoader loader )
         throws Exception
     {
         List batteries = new ArrayList();
 
         // Check to see if junit.jar is available. If not, then don't attempt to
         // cast t
         Class testCaseClass = null;
 
         try
         {
             testCaseClass = loader.loadClass( "junit.framework.TestCase" );
         }
         catch ( ClassNotFoundException e )
         {
             // testCaseClass remains as null;
         }
 
         for ( int i = 0; i < batteryHolders.size(); i++ )
         {
             Object[] holder = (Object[]) batteryHolders.get( i );
 
             Class testClass = null;
 
             try
             {
                 testClass = loader.loadClass( (String) holder[0] );
             }
             catch ( Exception e )
             {
                 e.printStackTrace();
 
                 continue;
             }
 
             Object battery = null;
 
             if ( Battery.class.isAssignableFrom( testClass ) )
             {
                 if ( holder[1] != null )
                 {
                     Object[] params = (Object[]) holder[1];
 
                     Class[] paramTypes = new Class[params.length];
 
                     for ( int j = 0; j < params.length; j++ )
                     {
                         paramTypes[j] = params[j].getClass();
                     }
 
                     Constructor constructor = testClass.getConstructor( paramTypes );
 
                     battery = constructor.newInstance( params );
                 }
                 else
                 {
                     battery = testClass.newInstance();
                 }
             }
             else if ( testCaseClass != null && testCaseClass.isAssignableFrom( testClass ) )
             {
                 battery = new JUnitBattery( testClass, loader );
             }
             else
             {
                 throw new Exception( "Class " + testClass + " was neither a Suite nor a TestCase" );
             }
 
             batteries.add( battery );
         }
 
         return batteries;
     }
 
     protected List instantiateReports( List reportClassNames, ClassLoader classLoader )
         throws Exception
     {
         List reports = new ArrayList();
 
         for ( Iterator i = reportClassNames.iterator(); i.hasNext(); )
         {
             String reportClass = (String) i.next();
 
             try
             {
                 Report report = (Report) classLoader.loadClass( reportClass ).newInstance();
 
                 reports.add( report );
             }
             catch ( Exception e )
             {
                 e.printStackTrace();
             }
         }
 
         return reports;
     }
 }
 
 
