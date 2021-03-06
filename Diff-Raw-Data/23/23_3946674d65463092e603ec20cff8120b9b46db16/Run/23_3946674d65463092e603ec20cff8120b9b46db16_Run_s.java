 /*
  * Copyright 2006 Niclas Hedhman.
  *
  * Licensed  under the  Apache License,  Version 2.0  (the "License");
  * you may not use  this file  except in  compliance with the License.
  * You may obtain a copy of the License at
  *
  *   http://www.apache.org/licenses/LICENSE-2.0
  *
  * Unless required by applicable law or agreed to in writing, software
  * distributed  under the  License is distributed on an "AS IS" BASIS,
  * WITHOUT  WARRANTIES OR CONDITIONS  OF ANY KIND, either  express  or
  * implied.
  *
  * See the License for the specific language governing permissions and
  * limitations under the License. 
  */
 package org.ops4j.pax.runner;
 
 import java.io.File;
 import java.io.IOException;
 import java.io.InputStream;
 import java.io.OutputStream;
 import java.lang.reflect.Method;
 import java.net.Authenticator;
 import java.net.PasswordAuthentication;
 import java.util.ArrayList;
 import java.util.List;
 import java.util.Properties;
 import java.util.logging.FileHandler;
 import java.util.logging.Level;
 import java.util.logging.Logger;
 import javax.xml.parsers.ParserConfigurationException;
 import org.ops4j.pax.runner.pom.BundleManager;
 import org.ops4j.pax.runner.pom.PomManager;
 import org.ops4j.pax.runner.provisioning.Provisioning;
 import org.ops4j.pax.runner.util.NullArgumentException;
 import org.xml.sax.SAXException;
 
 /**
  * This class will download a Maven POM run that definition inside a OSGi container.
  */
 public class Run
 {
 
     private static final Logger LOGGER;
 
     static
     {
         LOGGER = Logger.getLogger( Run.class.getName() );
         LOGGER.setParent( Logger.global );
     }
 
     public static File WORK_DIR;
 
     private static CmdLine m_cmdLine;
     private static String[] m_vmopts;
 
     public static void main( String[] args )
         throws IOException,
                ParserConfigurationException,
                SAXException
     {
         try
         {
             m_cmdLine = new CmdLine( args );
             if( m_cmdLine.isSet( "help" ) )
             {
                 help( null );
             }
         }
         catch( IllegalArgumentException e )
         {
             help( e );
         }
         catch( ArrayIndexOutOfBoundsException e )
         {
             help( e );
         }
 
         System.out.println( "    ______  ________  __  __" );
         System.out.println( "   / __  / /  __   / / / / /" );
         System.out.println( "  /  ___/ /  __   / _\\ \\ _/" );
         System.out.println( " /  /    /  / /  / / _\\ \\" );
         System.out.println( "/__/    /__/ /__/ /_/ /_/" );
         System.out.println();
         System.out.println( "Pax Runner from OPS4J - http://www.ops4j.org" );
         System.out.println( "--------------------------------------------" );
         System.out.println();
 
         String workDir = m_cmdLine.getValue( "dir" );
         WORK_DIR = new File( workDir );
         WORK_DIR.mkdirs();
         File logDir = new File( WORK_DIR, "logs/" );
         logDir.mkdirs();
         FileHandler fileHandler = new FileHandler( logDir.getAbsolutePath() + "/runner-%g.xml", 100000, 3, false );
         Logger.global.addHandler( fileHandler );
         Logger.global.setLevel( Level.ALL );
         System.out.println( "Working Dir: " + WORK_DIR );
 
         Authenticator auth = new Authenticator()
         {
             protected PasswordAuthentication getPasswordAuthentication()
             {
                 if( tryGetRequestorType() == PROXY )
                 {
                     String userName = m_cmdLine.getValue( "proxy-username" );
                     char[] password = m_cmdLine.getValue( "proxy-password" ).toCharArray();
                     return new PasswordAuthentication( userName, password );
                 }
                 if( tryGetRequestorType() == SERVER )
                 {
                     String userName = m_cmdLine.getValue( "repository-username" );
                     char[] password = m_cmdLine.getValue( "repository-password" ).toCharArray();
                     return new PasswordAuthentication( userName, password );
                 }
                 return null;
             }
 
             {
                 init();
             }
 
             Class requestorTypeClass;
             Method getRequestorTypeMethod;
 
             Object PROXY;
             Object SERVER;
 
             protected void init()
             {
                 try
                 {
                     requestorTypeClass = Class.forName( "java.net.Authenticator$RequestorType" );
                     getRequestorTypeMethod = Authenticator.class.getDeclaredMethod( "getRequestorType", null );
                     PROXY = requestorTypeClass.getDeclaredField( "PROXY" ).get( null );
                     SERVER = requestorTypeClass.getDeclaredField( "SERVER" ).get( null );
                 }
                 catch( Exception e )
                 {
                     // must be on a non-Java5 runtime
                     PROXY = "PROXY";
                     SERVER = "SERVER";
                 }
             }
 
             protected Object tryGetRequestorType()
             {
                 try
                 {
                     // use indirect access to Java5 specific method
                     return getRequestorTypeMethod.invoke( this, null );
                 }
                 catch( Exception e )
                 {
                     return "SERVER";
                 }
             }
         };
         Authenticator.setDefault( auth );
 
         List repositoryList = parseRepositories( m_cmdLine );
         String localRepository = m_cmdLine.getValue( "localRepository" );
         System.out.println( " Local Repo: " + localRepository );
         boolean noCheckMD5 = m_cmdLine.isSet( "no-md5" );
         Downloader downloader = new Downloader( repositoryList, localRepository, noCheckMD5, true );
         List bundles;
         Properties props;
         String urlValue = m_cmdLine.getValue( "url" );
         boolean useProvisioning = urlValue != null && urlValue.endsWith( ".zip" );
         if( useProvisioning )
         {
             Provisioning provisioning = new Provisioning( downloader );
             bundles = provisioning.getBundles( m_cmdLine );
             props = provisioning.getProperties( m_cmdLine );
         }
         else
         {
             PomManager pomManager = new PomManager( downloader );
             bundles = pomManager.getBundles( m_cmdLine );
             props = pomManager.getProperties( m_cmdLine );
         }
         BundleManager bundleManager = new BundleManager( downloader );
         String platform = m_cmdLine.getValue( "platform" ).toLowerCase();
         System.out.println( "\n   Platform: " + platform );
         String classpath = m_cmdLine.getValue( "classpath" );
         if( classpath.length() > 0 )
         {
             System.out.println( "\n  Classpath: " + classpath );
             System.out.println();
         }
         handleVmOptions();
         if( "equinox".equals( platform ) )
         {
             Runnable wrapper = new EquinoxRunner( m_cmdLine, props, bundles, bundleManager, classpath );
             wrapper.run();
         }
         else if( "felix".equals( platform ) )
         {
             Runnable wrapper = new FelixRunner( m_cmdLine, props, bundles, bundleManager, classpath );
             wrapper.run();
         }
         else if( "knopflerfish".equals( platform ) )
         {
             Runnable wrapper = new KnopflerfishRunner( m_cmdLine, props, bundles, bundleManager, classpath );
             wrapper.run();
         }
         else
         {
             System.err.println( "Platform '" + platform + "' is currently not supported." );
             System.exit( 2 );
         }
         System.exit( 0 );
     }
 
     private static void handleVmOptions()
     {
         String value = m_cmdLine.getValue( "vmopts" );
         System.out.print( " VM Options: " );
         if( "".equals( value ) )
         {
             m_vmopts = new String[0];
             System.out.println( "default" );
         }
         else
         {
             m_vmopts = value.trim().split( " " );
             for( int i = 0; i < m_vmopts.length; i++ )
             {
                 System.out.print( "[" + m_vmopts[ i ] + "] " );
             }
             System.out.println();
         }
     }
 
     public static void help( Exception e )
     {
         if( e != null )
         {
             System.err.println( e );
         }
 
         System.err.println();
         System.err.println( "java -jar pax-runner.jar [options] <groupId> <artifactId> <version>" );
         System.err.println( "  or" );
         System.err.println( "java -jar pax-runner.jar [options] <pom-URL>" );
         System.err.println( "  or" );
         System.err.println( "java -jar pax-runner.jar [options] <provisioning-URL> (not supported yet)" );
         System.err.println( "  or" );
         System.err.println( "java -jar pax-runner.jar [options]" );
         System.err.println( "\nOptions;" );
         System.err.println( "--platform=<platform>       -  The OSGi platform to use. Default: equinox" );
         System.err.println( "--startlevel=<startlevel>   -  Startlevel to be set. Default:6" );
         System.err.println( "--bundlelevel=<startlevel>  -  Startlevel of user bundles found in POM. Default: 5" );
         System.err.println( "--clean                     -  Do not load persisted state." );
         System.err.println( "--gui                       -  Load GUI (if supported by platform)" );
         System.err.println( "--no-md5                    -  Disable MD5 checksum checks for downloads." );
         System.err.println( "--dir=<workdir>             -  Which directory to use. Default: runner/" );
         System.err.println( "--profile=<profile>         -  Which profile to run (if supported by platform)" );
         System.err.println( "--repository=<repos>        -  Which repositories to download from." );
         System.err.println( "--localRepository=<repo>    -  Which local repository to use. Default: ~/.m2/repository" );
         System.err.println( "--proxy-username=<pwd>      -  Username for the proxy." );
         System.err.println( "--proxy-password=<pwd>      -  Username for the proxy." );
         System.err.println( "--repository-username=<pwd> -  Username for the repository server." );
         System.err.println( "--repository-password=<pwd> -  Username for the repository server." );
         System.err.println( "--vmopts=<options>          -  Additional JVM options." );
         System.err.println( "--classpath=<cp>            -  The JVM classpath to be set when launching framework." );
         System.err.println( "--systempackages=<packages> -  The packages to be exported by the system bundle." );
         System.err.println();
 
         System.exit( 1 );
     }
 
     private static List parseRepositories( CmdLine commandLine )
         throws IllegalArgumentException
     {
         NullArgumentException.validateNotNull( commandLine, "commandLine" );
 
         String repo = commandLine.getValue( "repository" );
         String[] repositories = repo.split( "," );
         List repositoryList = new ArrayList();
         for( int i = 0; i < repositories.length; i++ )
         {
             if( !repositories[ i ].endsWith( "/" ) )
             {
                 repositories[ i ] = repositories[ i ] + "/";
             }
             repositoryList.add( repositories[ i ] );
         }
         return repositoryList;
     }
 
     // helper function to ensure shutdown of framework VM on Windows when Pax-Runner is Ctrl-C'd
     protected static void destroyFrameworkOnExit( final Process frameworkVM, final Pipe[] pipes )
     {
         Runtime.getRuntime().addShutdownHook( new Thread( new Runnable()
         {
             public void run()
             {
                 try
                 {
                     for( int i = 0; i < pipes.length; i++ )
                     {
                         pipes[ i ].stop();
                     }
                 }
                 finally
                 {
                     frameworkVM.destroy();
                 }
             }
         }
         )
         );
     }
 
     public static void execute( String[] commands )
         throws IOException, InterruptedException
     {
         String[] frameworkOpts = { };
         String frameworkOptsString = System.getProperty( "FRAMEWORK_OPTS" );
         if( frameworkOptsString != null )
         {
             //get framework opts
             frameworkOpts = frameworkOptsString.split( " " );
         }
         String javaHome = System.getProperty( "JAVA_HOME" );
         if( javaHome == null )
         {
             try
             {
                 javaHome = (String) System.getenv( "JAVA_HOME" );
             }
             catch( Error e )
             {
                 // fallback when running under Java 1.4.x
                 javaHome = System.getProperty( "java.home" );
             }
         }
         if( javaHome == null )
         {
             System.err.println( "JAVA_HOME is not set." );
         }
 
         String[] totalCommandLine = new String[commands.length + frameworkOpts.length + m_vmopts.length + 1];
         int pos = 0;
         totalCommandLine[ pos++ ] = javaHome + "/bin/java";
         System.arraycopy( frameworkOpts, 0, totalCommandLine, pos, frameworkOpts.length );
         pos = pos + frameworkOpts.length;
         System.arraycopy( m_vmopts, 0, totalCommandLine, pos, m_vmopts.length );
         pos = pos + m_vmopts.length;
         System.arraycopy( commands, 0, totalCommandLine, pos, commands.length );
         Runtime runtime = Runtime.getRuntime();
         StringBuffer buffer = new StringBuffer();
         for( int i = 0; i < totalCommandLine.length; i++ )
         {
             buffer.append( totalCommandLine[ i ] );
             buffer.append( " " );
         }
         LOGGER.fine( "Starting Framework: \n    " + buffer.toString() );
        Process process = runtime.exec( totalCommandLine, null );
         InputStream err = process.getErrorStream();
         InputStream out = process.getInputStream();
         OutputStream in = process.getOutputStream();
         Pipe errPipe = new Pipe( err, System.err );
         errPipe.start();
         Pipe outPipe = new Pipe( out, System.out );
         outPipe.start();
         Pipe inPipe = new Pipe( in, System.in );
         inPipe.start();
         destroyFrameworkOnExit( process, new Pipe[]{ inPipe, outPipe, errPipe } );
         process.waitFor();
         inPipe.stop();
         outPipe.stop();
         errPipe.stop();
     }
 }
