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
 package org.sonatype.flexmojos.optimizer;
 
 import java.io.File;
 import java.io.FileInputStream;
 import java.io.FileNotFoundException;
 import java.io.FileOutputStream;
 import java.io.IOException;
 import java.io.InputStream;
 import java.io.OutputStream;
 import java.util.Collection;
 import java.util.zip.ZipEntry;
 import java.util.zip.ZipException;
 import java.util.zip.ZipFile;
 
 import org.apache.maven.model.Build;
 import org.apache.maven.plugin.AbstractMojo;
 import org.apache.maven.plugin.MojoExecutionException;
 import org.apache.maven.plugin.MojoFailureException;
 import org.apache.maven.project.MavenProject;
 import org.apache.maven.project.MavenProjectHelper;
 import org.codehaus.plexus.util.FileUtils;
 import org.codehaus.plexus.util.IOUtil;
 import org.sonatype.flexmojos.utilities.MavenUtils;
 
 import flex2.compiler.swc.Digest;
 import flex2.compiler.swc.Swc;
 import flex2.compiler.swc.SwcCache;
 import flex2.compiler.swc.SwcGroup;
 
 /**
  * Goal which run post-link SWF optimization on swc files. This goal is used to produce RSL files.
  * 
  * @author Marvin Herman Froeder (velo.br@gmail.com)
  * @author Marla Bonar (Marla_Bonar@Intuit.com) - added ability to optimize a swf
  * @since 2.0
  * @goal optimize
  * @phase package
  */
 public class OptimizerMojo
     extends AbstractMojo
 {
 
     /**
      * The maven project.
      * 
      * @parameter expression="${project}"
      * @required
      * @readonly
      */
     protected MavenProject project;
 
     /**
      * @parameter expression="${project.build}"
      * @required
      * @readonly
      */
     protected Build build;
 
     /**
      * @component
      */
     protected MavenProjectHelper projectHelper;
 
     public void execute()
         throws MojoExecutionException, MojoFailureException
     {
         getLog().info(
                        "flexmojos " + MavenUtils.getFlexMojosVersion()
                            + " - GNU GPL License (NO WARRANTY) - See COPYRIGHT file" );
 
         String packaging = project.getPackaging();
         getLog().debug( "project.getPackaging = " + packaging );
 
         if ( !"swc".equals( packaging ) && !"swf".equals( packaging ) )
         {
             getLog().warn( "Optimizer mojo can only be used on SWC or SWF projects." );
             return;
         }
 
         File originalFile = project.getArtifact().getFile();
 
         if ( originalFile == null || !originalFile.exists() )
         {
             throw new MojoExecutionException( "Library file not found." );
         }
 
         getLog().debug( "attempting to optimize: " + originalFile.getName() );
         InputStream inputSWF = null;
         ZipFile zipFile = null;
         OutputStream outputSWF = null;
         try
         {
             if ( "swf".equals( packaging ) )
             {
                inputSWF = new FileInputStream(  backupOriginalSWF( originalFile ) );
                 zipFile = null;
             }
             else
             {
                 zipFile = newZipFile( originalFile );
                 inputSWF = readLibrarySwf( originalFile, zipFile );
             }
 
             File optimizedSWFFile;
             if ( "swf".equals( packaging ) )
             {
                 originalFile.delete();
                 optimizedSWFFile = originalFile;
             }
             else
             {
                 optimizedSWFFile = new File( build.getDirectory(), build.getFinalName() + ".swf" );
             }
             outputSWF = new FileOutputStream( optimizedSWFFile );
 
             getLog().info( "Original file is: " + originalFile.length() / 1024 + " k bytes" );
 
             optimize( inputSWF, outputSWF );
 
             getLog().info( "optimized swf is: " + optimizedSWFFile.length() / 1024 + " k bytes" );
 
             if ( "swc".equals( packaging ) )
             {
                 getLog().debug( "updating digest " );
                 updateDigest( optimizedSWFFile, originalFile );
 
                 getLog().debug( "attaching Artifact " );
                 projectHelper.attachArtifact( project, "swf", optimizedSWFFile );
             }
         }
         catch ( FileNotFoundException e )
         {
             // don't expect that
             throw new MojoExecutionException( e.getMessage(), e );
         }
         finally
         {
             IOUtil.close( inputSWF );
             IOUtil.close( outputSWF );
             if ( zipFile != null )
             {
                 try
                 {
                     zipFile.close();
                 }
                 catch ( IOException e )
                 {
                     // ignore
                 }
             }
         }
     }
 
     private void optimize( InputStream inputSWF, OutputStream outputSWF )
         throws MojoExecutionException
     {
         try
         {
             // optimize
             flex2.tools.API.optimize( inputSWF, outputSWF );
             outputSWF.flush();
         }
         catch ( IOException e )
         {
             throw new MojoExecutionException( "An error happen while trying to optimize.", e );
         }
     }
 
     private InputStream readLibrarySwf( File originalFile, ZipFile zipFile )
         throws MojoFailureException, MojoExecutionException
     {
         InputStream inputSWF;
         ZipEntry entry = zipFile.getEntry( "library.swf" );
         if ( entry == null )
         {
             throw new MojoFailureException( "Invalid SWC file. Library.swf not found. " + originalFile );
         }
         try
         {
             inputSWF = zipFile.getInputStream( entry );
         }
         catch ( IOException e )
         {
             throw new MojoExecutionException( "Unable to read library.swf entry!", e );
         }
         return inputSWF;
     }
 
    private File backupOriginalSWF( File originalFile )
         throws MojoExecutionException
     {
         File bkpOriginalFile = new File( build.getOutputDirectory(), "orig-library.swf" );
         try
         {
             FileUtils.copyFile( originalFile, bkpOriginalFile );
         }
         catch ( IOException e )
         {
             throw new MojoExecutionException( "Unable to backup SWF file.", e );
         }
 
         getLog().debug( "attaching original swf" );
         projectHelper.attachArtifact( project, "swf", "original", bkpOriginalFile );
		return bkpOriginalFile;
     }
 
     private ZipFile newZipFile( File originalFile )
         throws MojoExecutionException
     {
         ZipFile zipFile = null;
         try
         {
             zipFile = new ZipFile( originalFile );
         }
         catch ( ZipException e )
         {
             throw new MojoExecutionException( "Invalid SWC file, is not a valid ZIP file.", e );
         }
         catch ( IOException e )
         {
             throw new MojoExecutionException( "Unable to read SWC file. " + originalFile, e );
         }
         return zipFile;
     }
 
     @SuppressWarnings( "unchecked" )
     private void updateDigest( File optimizedSWF, File originalFile )
         throws MojoExecutionException
     {
         Digest digest = computeDigest( optimizedSWF );
         getLog().debug( "Updating digest on " + originalFile );
         SwcCache swcCache = new SwcCache();
         String[] paths = { originalFile.getAbsolutePath() };
         SwcGroup group = swcCache.getSwcGroup( paths );
         Collection<Swc> swcs = group.getSwcs().values();
         Swc swc = swcs.iterator().next();
         swc.setDigest( Swc.LIBRARY_SWF, digest );
 
         // export SWC
         try
         {
             swcCache.export( swc );
         }
         catch ( IOException e )
         {
             throw new MojoExecutionException( "Unable to update digest information.", e );
         }
         finally
         {
             swc.close();
         }
 
     }
 
     private Digest computeDigest( File optimizedSwf )
         throws MojoExecutionException
     {
         getLog().debug( "Computing optimized swf digest" );
         InputStream input = null;
         byte[] bytes;
         try
         {
             input = new FileInputStream( optimizedSwf );
             bytes = IOUtil.toByteArray( input );
         }
         catch ( IOException e )
         {
             throw new MojoExecutionException( "Error reading optimized SWF", e );
         }
         finally
         {
             IOUtil.close( input );
         }
 
         Digest d = new Digest();
         getLog().debug( d.computeDigest( bytes ) );
         return d;
     }
 
 }
