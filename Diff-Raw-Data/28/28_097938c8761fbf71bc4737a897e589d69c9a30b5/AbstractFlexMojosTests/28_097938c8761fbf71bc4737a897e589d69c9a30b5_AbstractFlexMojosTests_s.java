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
 import org.testng.AssertJUnit;
 import org.testng.annotations.BeforeSuite;
 import org.testng.annotations.Test;
 
 public class AbstractFlexMojosTests
 {
 
     protected static File projectsSource;
 
     protected static File projectsWorkdir;
 
     private static Properties props;
 
     private static final ReadWriteLock copyProjectLock = new ReentrantReadWriteLock();
 
     private static final ReadWriteLock downloadArtifactsLock = new ReentrantReadWriteLock();
 
     @BeforeSuite( alwaysRun = true )
     public static void initFolders()
         throws IOException
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
         System.setProperty( "maven.home", getProperty( "fake-maven" ) );
 
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
     protected static File getProject( String projectName )
         throws IOException
     {
         copyProjectLock.writeLock().lock();
         try
         {
             File projectFolder = new File( projectsSource, projectName );
             AssertJUnit.assertTrue(
                                     "Project " + projectName + " folder not found.\n" + projectFolder.getAbsolutePath(),
                                     projectFolder.isDirectory() );
 
             File destDir = new File( projectsWorkdir, projectName );
            if ( destDir.exists() )
            {
                FileUtils.forceDelete( destDir );
            }
 
             FileUtils.copyDirectory( projectFolder, destDir, HiddenFileFilter.VISIBLE );
 
             // projects filtering
             Collection<File> poms =
                 FileUtils.listFiles( destDir, new WildcardFileFilter( "pom.xml" ), TrueFileFilter.INSTANCE );
             for ( File pom : poms )
             {
                 String pomContent = FileUtils.readFileToString( pom );
                 pomContent = pomContent.replace( "%{flexmojos.version}", getProperty( "version" ) );
                 pomContent = pomContent.replace( "%{flex.version}", getProperty( "flex-version" ) );
                 FileUtils.writeStringToFile( pom, pomContent );
             }
 
             return destDir;
         }
         finally
         {
             copyProjectLock.writeLock().unlock();
         }
     }
 
 }
