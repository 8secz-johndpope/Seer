 /**
  * Flexmojos is a set of maven goals to allow maven users to compile, optimize and test Flex SWF, Flex SWC, Air SWF and Air SWC.
  * Copyright (C) 2008-2012  Marvin Froeder <marvin@flexmojos.net>
  *
  * This program is free software: you can redistribute it and/or modify
  * it under the terms of the GNU General Public License as published by
  * the Free Software Foundation, either version 3 of the License, or
  * (at your option) any later version.
  *
  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  * GNU General Public License for more details.
  *
  * You should have received a copy of the GNU General Public License
  * along with this program.  If not, see <http://www.gnu.org/licenses/>.
  */
 package org.sonatype.flexmojos.tests;
 
 import java.io.File;
 import java.io.FileNotFoundException;
 import java.io.FileReader;
 import java.io.IOException;
 import java.io.InputStream;
 import java.lang.reflect.Method;
 import java.util.Arrays;
 import java.util.Collection;
 import java.util.Properties;
 import java.util.concurrent.locks.ReadWriteLock;
 import java.util.concurrent.locks.ReentrantReadWriteLock;
 
 import org.apache.commons.io.FileUtils;
 import org.apache.commons.io.filefilter.HiddenFileFilter;
 import org.apache.commons.io.filefilter.TrueFileFilter;
 import org.apache.commons.io.filefilter.WildcardFileFilter;
 import org.apache.maven.it.VerificationException;
 import org.apache.maven.it.Verifier;
 import org.apache.maven.model.Dependency;
 import org.apache.maven.model.Model;
 import org.apache.maven.model.building.ModelProcessor;
 import org.apache.maven.model.io.ModelParseException;
 import org.apache.maven.model.io.ModelWriter;
 import org.codehaus.plexus.DefaultPlexusContainer;
 import org.codehaus.plexus.PlexusContainer;
 import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
 import org.codehaus.plexus.util.ReaderFactory;
 import org.codehaus.plexus.util.xml.Xpp3Dom;
 import org.codehaus.plexus.util.xml.Xpp3DomBuilder;
 import org.hamcrest.CoreMatchers;
 import org.hamcrest.MatcherAssert;
 import org.sonatype.flexmojos.test.report.TestCaseReport;
 import org.sonatype.flexmojos.util.PathUtil;
 import org.testng.Assert;
 import org.testng.AssertJUnit;
 import org.testng.annotations.BeforeSuite;
 import org.testng.annotations.Test;
 
 public class AbstractFlexMojosTests
 {
 
     protected static File projectsSource;
 
     protected static File projectsWorkdir;
 
     private static Properties props;
 
     private static File mavenHome;
 
     protected static PlexusContainer container;
 
     private static final ReadWriteLock copyProjectLock = new ReentrantReadWriteLock();
 
     private static final ReadWriteLock downloadArtifactsLock = new ReentrantReadWriteLock();
 
     @BeforeSuite( alwaysRun = true )
     public static void initFolders()
         throws Exception
     {
         if ( props != null )
         {
             return;
         }
         props = new Properties();
         ClassLoader cl = AbstractFlexMojosTests.class.getClassLoader();
         InputStream is = cl.getResourceAsStream( "baseTest.properties" );
         if ( is != null )
         {
             try
             {
                 props.load( is );
             }
             finally
             {
                 is.close();
             }
         }
 
         projectsSource = new File( getProperty( "projects-source" ) );
         projectsWorkdir = new File( getProperty( "projects-target" ) );
         mavenHome = new File( getProperty( "fake-maven" ) );
 
         File mvn = new File( mavenHome, "bin/mvn" );
         updateMavenMemory( mvn, "\nMAVEN_OPTS=\"-Xmx512M -Duser.language=en -Duser.region=US\"\n" );
         File mvnBat = new File( mavenHome, "bin/mvn.bat" );
         updateMavenMemory( mvnBat, "\nset MAVEN_OPTS=-Xmx512M -Duser.language=en -Duser.region=US\n" );
 
         container = new DefaultPlexusContainer();
     }
 
     @BeforeSuite( dependsOnMethods = { "initFolders" }, alwaysRun = true )
     public static void addEmmaToClasshPath()
         throws Exception
     {
         File repo = new File( getProperty( "fake-repo" ) );
         addEmma( new File( repo, "org/sonatype/flexmojos/flexmojos-parent/" + getFlexmojosVersion()
             + "/flexmojos-parent-" + getFlexmojosVersion() + ".pom" ) );
         addEmma( new File( repo, "org/sonatype/flexmojos/flexmojos-maven-plugin/" + getFlexmojosVersion()
                            + "/flexmojos-maven-plugin-" + getFlexmojosVersion() + ".pom" ) );
     }
 
     private static void addEmma( File fmParentPom )
         throws ComponentLookupException, IOException, ModelParseException
     {
         ModelProcessor builder = container.lookup( ModelProcessor.class );
         Model pom = builder.read( fmParentPom, null );
         Dependency emma = new Dependency();
         emma.setGroupId( "emma" );
         emma.setArtifactId( "emma" );
         emma.setVersion( "2.0.5312" );
         pom.addDependency( emma );
 
         ModelWriter writer = container.lookup( ModelWriter.class );
         writer.write( fmParentPom, null, pom );
     }
 
     private static void updateMavenMemory( File mvn, String memString )
         throws IOException
     {
         String mvnContent = org.codehaus.plexus.util.FileUtils.fileRead( mvn );
         if ( mvnContent.contains( memString ) )
         {
             return;
         }
         int i = mvnContent.indexOf( '\n' );
         mvnContent = mvnContent.substring( 0, i ) + memString + mvnContent.substring( i );
         org.codehaus.plexus.util.FileUtils.fileWrite( mvn.getAbsolutePath(), mvnContent );
     }
 
     protected static synchronized String getProperty( String key )
     {
         return props.getProperty( key );
     }
 
     @SuppressWarnings( "unchecked" )
     protected static Verifier test( File projectDirectory, String goal, String... args )
         throws VerificationException
     {
         Verifier verifier = getVerifier( projectDirectory );
         verifier.getCliOptions().addAll( Arrays.asList( args ) );
         verifier.executeGoal( goal );
         // TODO there are some errors logged, but they are not my concern
         // verifier.verifyErrorFreeLog();
         return verifier;
     }
 
     @SuppressWarnings( "unchecked" )
     protected static Verifier getVerifier( File projectDirectory )
         throws VerificationException
     {
         System.setProperty( "maven.home", mavenHome.getAbsolutePath() );
 
         if ( new File( projectDirectory, "pom.xml" ).exists() )
         {
             downloadArtifactsLock.writeLock().lock();
             try
             {
                 Verifier verifier = new Verifier( projectDirectory.getAbsolutePath() );
                 verifier.getVerifierProperties().put( "use.mavenRepoLocal", "true" );
                 verifier.setLocalRepo( getProperty( "fake-repo" ) );
                 verifier.setAutoclean( false );
                 verifier.getCliOptions().add( "-npu" );
                 verifier.getCliOptions().add( "-B" );
                 verifier.getCliOptions().add( "-X" );
                 verifier.setLogFileName( getTestName() + ".resolve.log" );
                 verifier.executeGoal( "dependency:go-offline" );
             }
             catch ( Throwable t )
             {
                 t.printStackTrace();
                 // this is not a real issue
             }
             finally
             {
                 downloadArtifactsLock.writeLock().unlock();
             }
         }
 
         Verifier verifier = new Verifier( projectDirectory.getAbsolutePath() );
         // verifier.getCliOptions().add( "-s" + rootFolder.getAbsolutePath() + "/settings.xml" );
         // verifier.getCliOptions().add( "-o" );
         verifier.getCliOptions().add( "-npu" );
         verifier.getCliOptions().add( "-B" );
         verifier.getCliOptions().add( "-X" );
         verifier.getVerifierProperties().put( "use.mavenRepoLocal", "true" );
         verifier.setLocalRepo( getProperty( "fake-repo" ) );
         Properties sysProps = new Properties();
         sysProps.setProperty( "MAVEN_OPTS", "-Xmx512m" );
         sysProps.setProperty( "apparat.threads", "true" );
         verifier.setSystemProperties( sysProps );
         verifier.setLogFileName( getTestName() + ".log" );
         verifier.setAutoclean( false );
         return verifier;
     }
 
     private static String getTestName()
     {
         StackTraceElement[] stackTrace = new Exception().getStackTrace();
         for ( StackTraceElement stack : stackTrace )
         {
             Class<?> testClass;
             try
             {
                 testClass = Class.forName( stack.getClassName() );
             }
             catch ( ClassNotFoundException e )
             {
                 // nvm, and should never happen
                 continue;
             }
             for ( Method method : testClass.getMethods() )
             {
                 if ( method.getName().equals( stack.getMethodName() ) )
                 {
                     if ( method.getAnnotation( Test.class ) != null )
                     {
                         return method.getName();
                     }
                 }
             }
         }
         return null;
     }
 
     @SuppressWarnings( "unchecked" )
     protected static File getProject( String projectName, String... filesToInterpolate )
         throws IOException
     {
         if ( filesToInterpolate == null || filesToInterpolate.length == 0 )
         {
             filesToInterpolate = new String[] { "pom.xml" };
         }
 
         copyProjectLock.writeLock().lock();
         try
         {
             File projectFolder = new File( projectsSource, projectName );
             AssertJUnit.assertTrue(
                                     "Project " + projectName + " folder not found.\n" + projectFolder.getAbsolutePath(),
                                     projectFolder.isDirectory() );
 
             File destDir = new File( projectsWorkdir, projectName + "_" + getTestName() );
 
             FileUtils.copyDirectory( projectFolder, destDir, HiddenFileFilter.VISIBLE );
 
             // projects filtering
             Collection<File> poms =
                 FileUtils.listFiles( destDir, new WildcardFileFilter( filesToInterpolate ), TrueFileFilter.INSTANCE );
             for ( File pom : poms )
             {
                 String pomContent = FileUtils.readFileToString( pom );
                 pomContent = pomContent.replace( "%{flexmojos.version}", getFlexmojosVersion() );
                 pomContent = pomContent.replace( "%{flex.version}", getFlexSDKVersion() );
                 FileUtils.writeStringToFile( pom, pomContent );
             }
 
             return destDir;
         }
         finally
         {
             copyProjectLock.writeLock().unlock();
         }
     }
 
     protected static String getFlexSDKVersion()
     {
         return getProperty( "flex-version" );
     }
 
     protected static String getFlexmojosVersion()
     {
         return getProperty( "version" );
     }
 
     protected TestCaseReport getTestReport( File testDir, String testClass )
         throws Exception
     {
         File target = new File( testDir, "target" );
 
         File testClasses = new File( target, "test-classes" );
         AssertJUnit.assertTrue( "test-classes folder not created!", testClasses.isDirectory() );
 
         File sureFireReports = new File( target, "surefire-reports" );
         AssertJUnit.assertTrue( "Report folder not created!", sureFireReports.isDirectory() );
 
         File reportFile = new File( sureFireReports, "TEST-" + testClass + ".xml" );
         AssertJUnit.assertTrue( "Report was not created!", reportFile.isFile() );
 
         TestCaseReport report = new TestCaseReport( Xpp3DomBuilder.build( new FileReader( reportFile ) ) );
 
         return report;
     }
 
     protected Xpp3Dom getFlexConfigReport( Verifier verifier, String artifactId )
     {
         return getFlexConfigReport( verifier, artifactId, "1.0-SNAPSHOT" );
     }
 
     protected Xpp3Dom getFlexConfigReport( Verifier verifier, String artifactId, String version )
     {
         File configReport =
             new File( verifier.getBasedir(), "target/" + artifactId + "-" + version + "-config-report.xml" );
         Xpp3Dom configReportDOM;
         try
         {
             configReportDOM = Xpp3DomBuilder.build( ReaderFactory.newXmlReader( configReport ) );
         }
         catch ( Exception e )
         {
             Assert.fail( "Unable to parse \n" + configReport, e );
             throw new RuntimeException( e );
         }
 
         return configReportDOM;
     }
 
    protected void assertSeftExit( File main )
         throws Exception
     {
         if(!main.exists()) {
             throw new FileNotFoundException( PathUtil.getCanonicalPath( main ) );
         }
         
         Process p = null;
         try
         {
             p = Runtime.getRuntime().exec( new String[] { "flashplayer", main.getCanonicalPath() } );
             final Process tp = p;
 
             Thread t = new Thread( new Runnable()
             {
 
                 public void run()
                 {
                     try
                     {
                         tp.waitFor();
                     }
                     catch ( InterruptedException e )
                     {
                     }
                 }
             } );
 
             t.start();
 
             t.join( 10000 );
 
            MatcherAssert.assertThat( p.exitValue(), CoreMatchers.equalTo( 0 ) );
         }
         finally
         {
             if ( p != null )
                 p.destroy();
         }
     }
 
 }
