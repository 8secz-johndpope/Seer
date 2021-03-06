 package org.codehaus.mojo.osxappbundle;
 
 /*
  * Copyright 2001-2008 The Codehaus.
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
 
 
 import org.apache.commons.io.IOUtils;
 import org.apache.maven.artifact.Artifact;
 import org.apache.maven.artifact.repository.layout.ArtifactRepositoryLayout;
 import org.apache.maven.artifact.repository.layout.DefaultRepositoryLayout;
 import org.apache.maven.plugin.AbstractMojo;
 import org.apache.maven.plugin.MojoExecutionException;
 import org.apache.maven.project.MavenProject;
 import org.apache.maven.project.MavenProjectHelper;
 import org.apache.velocity.VelocityContext;
 import org.apache.velocity.exception.MethodInvocationException;
 import org.apache.velocity.exception.ParseErrorException;
 import org.apache.velocity.exception.ResourceNotFoundException;
 import org.codehaus.mojo.osxappbundle.encoding.DefaultEncodingDetector;
 import org.codehaus.plexus.archiver.ArchiverException;
 import org.codehaus.plexus.archiver.zip.ZipArchiver;
 import org.codehaus.plexus.util.DirectoryScanner;
 import org.codehaus.plexus.util.FileUtils;
 import org.codehaus.plexus.util.cli.CommandLineException;
 import org.codehaus.plexus.util.cli.Commandline;
 import org.codehaus.plexus.velocity.VelocityComponent;
 
 import java.io.ByteArrayInputStream;
 import java.io.File;
 import java.io.FileOutputStream;
 import java.io.IOException;
 import java.io.OutputStreamWriter;
 import java.io.StringWriter;
 import java.io.Writer;
 import java.util.ArrayList;
 import java.util.Arrays;
 import java.util.Collections;
 import java.util.Iterator;
 import java.util.List;
 import java.util.Set;
 
 /**
  * Package dependencies as an Application Bundle for Mac OS X.
  *
  * @goal bundle
  * @phase package
  * @requiresDependencyResolution runtime
  */
 public class CreateApplicationBundleMojo
     extends AbstractMojo
 {
 
     /**
      * Default includes - everything is included.
      */
     private static final String[] DEFAULT_INCLUDES = {"**/**"};
 
     /**
      * The Maven Project Object
      *
      * @parameter default-value="${project}"
      * @readonly
      */
     private MavenProject project;
 
     /**
      * The directory where the application bundle will be created
      *
      * @parameter default-value="${project.build.directory}/${project.build.finalName}";
      */
     private File buildDirectory;
 
     /**
      * The location of the generated disk image file
      *
      * @parameter default-value="${project.build.directory}/${project.build.finalName}.dmg"
      */
     private File diskImageFile;
 
 
     /**
      * The location of the Java Application Stub
      *
      * @parameter default-value="/System/Library/Frameworks/JavaVM.framework/Versions/Current/Resources/MacOS/JavaApplicationStub";
      */
     private File javaApplicationStub;
 
     /**
      * The main class to execute when double-clicking the Application Bundle
      *
      * @parameter expression="${mainClass}"
      * @required
      */
     private String mainClass;
 
     /**
      * The name of the Bundle. This is the name that is given to the application bundle;
      * and it is also what will show up in the application menu, dock etc.
      *
      * @parameter default-value="${project.name}"
      * @required
      */
     private String bundleName;
 
 
     /**
      * The icon file for the bundle
      *
      * @parameter
      */
     private File iconFile;
 
     /**
      * The version of the project. Will be used as the value of the CFBundleVersion key.
      *
      * @parameter default-value="${project.version}"
      */
     private String version;
 
     /**
      * A value for the JVMVersion key.
      *
      * @parameter default-value="1.4+"
      */
     private String jvmVersion;
 
     /**
      * The location of the produced Zip file containing the bundle.
      *
      * @parameter default-value="${project.build.directory}/${project.build.finalName}-app.zip"
      */
     private File zipFile;
 
     /**
      * If set to <code>false</code>, skips generating a Zip file containing the bundle. Default is <code>true</code>.
      *
      * @parameter default-value="true"
      */
     private boolean generateZipFile;
 
     /**
      * Paths to be put on the classpath in addition to the projects dependencies.
      * Might be useful to specifiy locations of dependencies in the provided scope that are not distributed with
      * the bundle but have a known location on the system.
      * {@see http://jira.codehaus.org/browse/MOJO-874}
      *
      * @parameter
      */
     private List additionalClasspath;
 
     /**
      * Additional resources (as a list of FileSet objects) that will be copies into
      * the build directory and included in the .dmg and zip files alongside with the
      * application bundle.
      *
      * @parameter
      */
     private List additionalResources;
 
     /**
      * Velocity Component.
      *
      * @component
      * @readonly
      */
     private VelocityComponent velocity;
 
     /**
      * The location of the template for Info.plist.
      * Classpath is checked before the file system.
      *
      * @parameter default-value="org/codehaus/mojo/osxappbundle/Info.plist.template"
      */
     private String dictionaryFile;
 
     /**
      * Options to the JVM, will be used as the value of VMOptions in Info.plist.
      *
      * @parameter
      */
     private String vmOptions;
 
 
     /**
      * The Zip archiver.
      *
      * @component
      * @readonly
      */
     private MavenProjectHelper projectHelper;
 
     /**
      * The Zip archiver.
      *
      * @parameter expression="${component.org.codehaus.plexus.archiver.Archiver#zip}"
      * @required
      * @readonly
      */
     private ZipArchiver zipArchiver;
 
     /**
      * If this is set to <code>true</code>, the generated DMG file will be internet-enabled.
      * The default is ${false}
      *
      * @parameter default-value="false"
      */
     private boolean internetEnable;
     
     /**
      * If this is set to <code>true</code>, the JVM will be launched with flag -XstartOnFirstThread.
      * Use this property instead of vmOptions for this option.
      * 
      * @parameter default-value="false"
      */
     private boolean startOnFirstThread;
 
     /**
      * As a workaround for a bug, code signing requires the environment variable CODESIGN_ALLOCATE to point to
      * the codesign_allocate binary in Xcode.
      *
      * @parameter default-value="/Applications/Xcode.app/Contents/Developer/usr/bin/codesign_allocate"
      */
     private String codesignAllocateLocation;
 
     /**
      * Name of the keychain to use for code signing
      *
      * @parameter
      */
     private String codesignKeychain = "";
 
     /**
      * Name of the identity in the selected keychain
      *
      * @parameter
      */
     private String codesignIdentity = "";
 
     /**
      * Alternate identifier to sign the app, should resemble the CFBundleIdentifier from Info.plist
      *
      * @parameter
      */
     private String codesignIdentifier = "";
 
     /**
      * The path to the SetFile tool.
      */
    private static final String LEGACY_SET_FILE_PATH = "/Developer/Tools/SetFile";
	private static final String SET_FILE_PATH = "/usr/bin/SetFile";
 
 
     /**
      * Bundle project as a Mac OS X application bundle.
      *
      * @throws MojoExecutionException If an unexpected error occurs during packaging of the bundle.
      */
     public void execute()
         throws MojoExecutionException
     {
 
         // Set up and create directories
         buildDirectory.mkdirs();
 
         File bundleDir = new File( buildDirectory, bundleName + ".app" );
         bundleDir.mkdirs();
 
         File contentsDir = new File( bundleDir, "Contents" );
         contentsDir.mkdirs();
 
         File resourcesDir = new File( contentsDir, "Resources" );
         resourcesDir.mkdirs();
 
         File javaDirectory = new File( resourcesDir, "Java" );
         javaDirectory.mkdirs();
 
         File macOSDirectory = new File( contentsDir, "MacOS" );
         macOSDirectory.mkdirs();
 
         // Copy in the native java application stub
         File stub = new File( macOSDirectory, javaApplicationStub.getName() );
         if(! javaApplicationStub.exists()) {
             String message = "Can't find JavaApplicationStub binary. File does not exist: " + javaApplicationStub;
 
             if(! isOsX() ) {
                 message += "\nNOTICE: You are running the osxappbundle plugin on a non OS X platform. To make this work you need to copy the JavaApplicationStub binary into your source tree. Then configure it with the 'javaApplicationStub' configuration property.\nOn an OS X machine, the JavaApplicationStub is typically located under /System/Library/Frameworks/JavaVM.framework/Versions/Current/Resources/MacOS/JavaApplicationStub";
             }
 
             throw new MojoExecutionException( message);
 
         } else {
             try
             {
                 FileUtils.copyFile( javaApplicationStub, stub );
             }
             catch ( IOException e )
             {
                 throw new MojoExecutionException(
                     "Could not copy file " + javaApplicationStub + " to directory " + macOSDirectory, e );
             }
         }
 
         // Copy icon file to the bundle if specified
         if ( iconFile != null )
         {
             try
             {
                 FileUtils.copyFileToDirectory( iconFile, resourcesDir );
             }
             catch ( IOException e )
             {
                 throw new MojoExecutionException( "Error copying file " + iconFile + " to " + resourcesDir, e );
             }
         }
 
         // Resolve and copy in all dependecies from the pom
         List files = copyDependencies( javaDirectory );
 
         // Create and write the Info.plist file
         File infoPlist = new File( bundleDir, "Contents/Info.plist" );
         writeInfoPlist( infoPlist, files );
 
         // Copy specified additional resources into the top level directory
         if (additionalResources != null && !additionalResources.isEmpty())
         {
             copyResources( additionalResources );
         }
 
         if ( isOsX() )
         {
             // Make the stub executable
             Commandline chmod = new Commandline();
             try
             {
                 chmod.setExecutable( "chmod" );
                 chmod.createArgument().setValue( "755" );
                 chmod.createArgument().setValue( stub.getAbsolutePath() );
 
                 chmod.execute();
             }
             catch ( CommandLineException e )
             {
                 throw new MojoExecutionException( "Error executing " + chmod + " ", e );
             }
 
             // This makes sure that the .app dir is actually registered as an application bundle
			String setFilePath = getSetFilePath();
			if ( setFilePath != null )
             {
                 Commandline setFile = new Commandline();
                 try
                 {
                    setFile.setExecutable(getSetFilePath());
                     setFile.createArgument().setValue( "-a" );
                     setFile.createArgument().setValue( "B" );
                     setFile.createArgument().setValue( bundleDir.getAbsolutePath() );
 
                     setFile.execute();
                 }
                 catch ( CommandLineException e )
                 {
                     throw new MojoExecutionException( "Error executing " + setFile, e );
                 }
             }
             else
             {
                getLog().warn( "Could  not set 'Has Bundle' attribute. Neither " + SET_FILE_PATH + ", nor " + LEGACY_SET_FILE_PATH + " could be found, is Developer Tools installed?" );
             }
 
             // sign the app if codesign identity is given
             if (codesignIdentity.length() > 0) {
                 Commandline codesign = new Commandline();
                 // workaround for a bug which requires the environment variable CODESIGN_ALLOCATE
                 // and an Xcode installation
                 codesign.addEnvironment("CODESIGN_ALLOCATE", codesignAllocateLocation);
                 try {
                     codesign.setExecutable("codesign");
                     codesign.createArgument().setValue("-s");
                     codesign.createArgument().setValue(codesignIdentity);
                     if (codesignIdentifier.length() > 0) {
                         codesign.createArgument().setValue("-i");
                         codesign.createArgument().setValue(codesignIdentifier);
                     }
                     codesign.createArgument().setValue("-f");
                     codesign.createArgument().setValue("-vvvv");
 
                     if (codesignKeychain.length() > 0) {
                         codesign.createArgument().setValue("--keychain");
                         codesign.createArgument().setValue(codesignKeychain);
                     }
 
                     // need to escape spaces
                     codesign.createArgument().setValue(bundleDir.getAbsolutePath().replaceAll(" ", "\\ "));
 
                     getLog().info("executing " + codesign);
                     Process process = codesign.execute();
 
                     process.waitFor();
 
                     String stdout = IOUtils.toString(process.getInputStream());
                     String stdErr = IOUtils.toString(process.getErrorStream());
                     getLog().info("StdOut - " + stdout);
                     getLog().info("StdErr - " + stdErr);
 
                     int result = process.exitValue();
                     if (result == 0) {
                         getLog().info("codesign completed successfully");
                     } else {
                         StringBuffer buffer = new StringBuffer();
                         buffer.append("codesign failed with exit code: ");
                         buffer.append(result);
                         buffer.append("\n");
                         if (getLog().isDebugEnabled()) {
                             buffer.append("Verify that the CFBundleExecutable and other Info.plits properties are correct, also check the availability of your certificates in the keychains.\n");
                         } else {
                             buffer.append("retry with 'mvn -X' to get more info");
                         }
                         buffer.append("Error message: ");
                         buffer.append(stdErr);
 
                         if (getLog().isDebugEnabled()) {
                             Commandline debug = new Commandline();
                             debug.setExecutable("security");
                             debug.createArgument().setValue("list-keychains");
 
                             Process process2 = debug.execute();
                             process.waitFor();
                             buffer.append("\nSearched keychains:\n");
                             buffer.append(IOUtils.toString(process2.getInputStream()));
                         }
 
                         getLog().warn(buffer.toString());
                     }
                 } catch (CommandLineException e) {
                     throw new MojoExecutionException("Error signing the application " + bundleDir.getAbsolutePath() + " with keychain/identity "
                             + codesignKeychain + "/" + codesignIdentity, e);
                 } catch (IOException e) {
                     throw new MojoExecutionException("blah", e);
                 } catch (InterruptedException e) {
                     getLog().warn("codesign failed, process interrupted", e);
                 }
             }
 
             // Create a .dmg file of the app
             Commandline dmg = new Commandline();
             try
             {
                 dmg.setExecutable( "hdiutil" );
                 dmg.createArgument().setValue( "create" );
                 dmg.createArgument().setValue( "-srcfolder" );
                 dmg.createArgument().setValue( buildDirectory.getAbsolutePath() );
                 dmg.createArgument().setValue( diskImageFile.getAbsolutePath() );
                 try
                 {
                     getLog().info("executing " + dmg);
                     dmg.execute().waitFor();
                 }
                 catch ( InterruptedException e )
                 {
                     throw new MojoExecutionException( "Thread was interrupted while creating DMG " + diskImageFile, e );
                 }
             }
             catch ( CommandLineException e )
             {
                 throw new MojoExecutionException( "Error creating disk image " + diskImageFile, e );
             }
             if(internetEnable) {
                 try {
 
                     Commandline internetEnable = new Commandline();
 
                     internetEnable.setExecutable("hdiutil");
                     internetEnable.createArgument().setValue("internet-enable" );
                     internetEnable.createArgument().setValue("-yes");
                     internetEnable.createArgument().setValue(diskImageFile.getAbsolutePath());
 
                     getLog().info("executing " + internetEnable);
                     internetEnable.execute();
                 } catch (CommandLineException e) {
                     throw new MojoExecutionException("Error internet enabling disk image: " + diskImageFile, e);
                 }
             }
             projectHelper.attachArtifact(project, "dmg", null, diskImageFile);
         }
 
         if (generateZipFile) {
 
             zipArchiver.setDestFile( zipFile );
             try
             {
                 String[] stubPattern = {buildDirectory.getName() + "/" + bundleDir.getName() +"/Contents/MacOS/"
                                         + javaApplicationStub.getName()};
 
                 zipArchiver.addDirectory( buildDirectory.getParentFile(), new String[]{buildDirectory.getName() + "/**"},
                         stubPattern);
 
                 DirectoryScanner scanner = new DirectoryScanner();
                 scanner.setBasedir( buildDirectory.getParentFile() );
                 scanner.setIncludes( stubPattern);
                 scanner.scan();
 
                 String[] stubs = scanner.getIncludedFiles();
                 for ( int i = 0; i < stubs.length; i++ )
                 {
                     String s = stubs[i];
                     zipArchiver.addFile( new File( buildDirectory.getParentFile(), s ), s, 0755 );
                 }
 
                 zipArchiver.createArchive();
                 projectHelper.attachArtifact(project, "zip", null, zipFile);
             }
             catch ( ArchiverException e )
             {
                 throw new MojoExecutionException( "Could not create zip archive of application bundle in " + zipFile, e );
             }
             catch ( IOException e )
             {
                 throw new MojoExecutionException( "IOException creating zip archive of application bundle in " + zipFile,
                                                   e );
             }
         }
     }
 
     private boolean isOsX()
     {
         String os = System.getProperty( "os.name" );
         return os != null && os.toLowerCase().contains( "os x" );
     }
 
     /**
      * Copy all dependencies into the $JAVAROOT directory
      *
      * @param javaDirectory where to put jar files
      * @return A list of file names added
      * @throws MojoExecutionException
      */
     private List copyDependencies( File javaDirectory )
         throws MojoExecutionException
     {
 
         // First, check if there are any artifacts to copy
         File artifactFile = project.getArtifact().getFile();
         Set artifacts = project.getArtifacts();
 
         if(artifactFile != null || !artifacts.isEmpty())
         {
 
             ArtifactRepositoryLayout layout = new DefaultRepositoryLayout();
 
             List list = new ArrayList();
 
             File repoDirectory = new File(javaDirectory, "repo");
             repoDirectory.mkdirs();
 
             // Then, copy the project's own artifact
             if (artifactFile != null)
             {
                 list.add( repoDirectory.getName() +"/" +layout.pathOf(project.getArtifact()));
 
                 try
                 {
                 FileUtils.copyFile( artifactFile, new File(repoDirectory, layout.pathOf(project.getArtifact())) );
                 }
                 catch ( IOException e )
                 {
                     throw new MojoExecutionException( "Could not copy artifact file " + artifactFile + " to " + javaDirectory );
                 }
             }
 
             // Finally, copy all project dependencies
             Iterator i = artifacts.iterator();
 
             while ( i.hasNext() )
             {
                 Artifact artifact = (Artifact) i.next();
 
                 File file = artifact.getFile();
                 File dest = new File(repoDirectory, layout.pathOf(artifact));
 
                 getLog().debug( "Adding " + file );
 
                 try
                 {
                     FileUtils.copyFile( file, dest);
                 }
                 catch ( IOException e )
                 {
                     throw new MojoExecutionException( "Error copying file " + file + " into " + javaDirectory, e );
                 }
 
                 list.add( repoDirectory.getName() +"/" + layout.pathOf(artifact) );
             }
 
             return list;
 
         }
 
         return Collections.emptyList();
 
     }
 
     /**
      * Writes an Info.plist file describing this bundle.
      *
      * @param infoPlist The file to write Info.plist contents to
      * @param files     A list of file names of the jar files to add in $JAVAROOT
      * @throws MojoExecutionException
      */
     private void writeInfoPlist( File infoPlist, List files )
         throws MojoExecutionException
     {
 
         VelocityContext velocityContext = new VelocityContext();
 
         velocityContext.put( "mainClass", mainClass );
         velocityContext.put( "cfBundleExecutable", javaApplicationStub.getName());
         velocityContext.put( "vmOptions", vmOptions);
         velocityContext.put( "bundleName", bundleName );
 
         velocityContext.put( "iconFile", iconFile == null ? "GenericJavaApp.icns" : iconFile.getName() );
 
         velocityContext.put( "version", version );
 
         velocityContext.put( "jvmVersion", jvmVersion );
 
         StringBuffer jarFilesBuffer = new StringBuffer();
 
         jarFilesBuffer.append( "<array>" );
         for ( int i = 0; i < files.size(); i++ )
         {
             String name = (String) files.get( i );
             jarFilesBuffer.append( "<string>" );
             jarFilesBuffer.append( "$JAVAROOT/" ).append( name );
             jarFilesBuffer.append( "</string>" );
 
         }
         if ( additionalClasspath != null )
         {
             for ( int i = 0; i < additionalClasspath.size(); i++ )
             {
                 String pathElement = (String) additionalClasspath.get( i );
                 jarFilesBuffer.append( "<string>" );
                 jarFilesBuffer.append( pathElement );
                 jarFilesBuffer.append( "</string>" );
 
             }
         }
         jarFilesBuffer.append( "</array>" );
 
         velocityContext.put( "classpath", jarFilesBuffer.toString() );
         velocityContext.put("startOnFirstThread", Boolean.valueOf(startOnFirstThread));
 
         try
         {
 
             String encoding = detectEncoding(dictionaryFile, velocityContext);
 
             getLog().debug( "Detected encoding " + encoding + " for dictionary file " +dictionaryFile  );
 
             Writer writer = new OutputStreamWriter( new FileOutputStream(infoPlist), encoding );
 
             velocity.getEngine().mergeTemplate( dictionaryFile, encoding, velocityContext, writer );
 
             writer.close();
         }
         catch ( IOException e )
         {
             throw new MojoExecutionException( "Could not write Info.plist to file " + infoPlist, e );
         }
         catch ( ParseErrorException e )
         {
             throw new MojoExecutionException( "Error parsing " + dictionaryFile, e );
         }
         catch ( ResourceNotFoundException e )
         {
             throw new MojoExecutionException( "Could not find resource for template " + dictionaryFile, e );
         }
         catch ( MethodInvocationException e )
         {
             throw new MojoExecutionException(
                 "MethodInvocationException occured merging Info.plist template " + dictionaryFile, e );
         }
         catch ( Exception e )
         {
             throw new MojoExecutionException( "Exception occured merging Info.plist template " + dictionaryFile, e );
         }
 
     }
 
     private String detectEncoding( String dictionaryFile, VelocityContext velocityContext )
         throws Exception
     {
         StringWriter sw = new StringWriter();
         velocity.getEngine().mergeTemplate( dictionaryFile, "utf-8", velocityContext, sw );
         return new DefaultEncodingDetector().detectXmlEncoding( new ByteArrayInputStream(sw.toString().getBytes( "utf-8" )) );
     }
 
     /**
      * Copies given resources to the build directory.
      *
      * @param fileSets A list of FileSet objects that represent additional resources to copy.
      * @throws MojoExecutionException In case af a resource copying error.
      */
     private void copyResources( List fileSets )
         throws MojoExecutionException
     {
         final String[] emptyStrArray = {};
 
         for ( Iterator it = fileSets.iterator(); it.hasNext(); )
         {
             FileSet fileSet = (FileSet) it.next();
 
             File resourceDirectory = new File( fileSet.getDirectory() );
             if ( !resourceDirectory.isAbsolute() )
             {
                 resourceDirectory = new File( project.getBasedir(), resourceDirectory.getPath() );
             }
 
             if ( !resourceDirectory.exists() )
             {
                 getLog().info( "Additional resource directory does not exist: " + resourceDirectory );
                 continue;
             }
 
             DirectoryScanner scanner = new DirectoryScanner();
 
             scanner.setBasedir( resourceDirectory );
             if ( fileSet.getIncludes() != null && !fileSet.getIncludes().isEmpty() )
             {
                 scanner.setIncludes( (String[]) fileSet.getIncludes().toArray( emptyStrArray ) );
             }
             else
             {
                 scanner.setIncludes( DEFAULT_INCLUDES );
             }
 
             if ( fileSet.getExcludes() != null && !fileSet.getExcludes().isEmpty() )
             {
                 scanner.setExcludes( (String[]) fileSet.getExcludes().toArray( emptyStrArray ) );
             }
 
             if (fileSet.isUseDefaultExcludes())
             {
                 scanner.addDefaultExcludes();
             }
 
             scanner.scan();
 
             List includedFiles = Arrays.asList( scanner.getIncludedFiles() );
 
             getLog().info( "Copying " + includedFiles.size() + " additional resource"
                            + ( includedFiles.size() > 1 ? "s" : "" ) );
 
             for ( Iterator j = includedFiles.iterator(); j.hasNext(); )
             {
                 String destination = (String) j.next();
                 File source = new File( resourceDirectory, destination );
                 File destinationFile = new File( buildDirectory, destination );
 
                 if ( !destinationFile.getParentFile().exists() )
                 {
                     destinationFile.getParentFile().mkdirs();
                 }
 
                 try
                 {
                     FileUtils.copyFile(source, destinationFile);
                 }
                 catch ( IOException e )
                 {
                     throw new MojoExecutionException( "Error copying additional resource " + source, e );
                 }
             }
         }
     }
	
	private String getSetFilePath() 
	{
		if ( fileExists( SET_FILE_PATH ) ) 
		{
			return SET_FILE_PATH;
		} 
		else if ( fileExists(LEGACY_SET_FILE_PATH) ) 
		{
			return LEGACY_SET_FILE_PATH;
		} 
		else 
		{
			return null;
		}
	}
	    
	private boolean fileExists(String path) {
	    return new File( path ).exists();
	}
 
 }
