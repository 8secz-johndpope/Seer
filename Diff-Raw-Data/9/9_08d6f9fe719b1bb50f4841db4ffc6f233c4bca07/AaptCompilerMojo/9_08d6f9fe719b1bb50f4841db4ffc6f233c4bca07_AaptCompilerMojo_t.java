 /*
  * Copyright (C) 2007-2008 JVending Masa
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
 package org.jvending.masa.plugin.aapt;
 
 import org.apache.maven.artifact.Artifact;
 import org.apache.maven.artifact.factory.ArtifactFactory;
 import org.apache.maven.artifact.repository.layout.ArtifactRepositoryLayout;
 import org.apache.maven.artifact.repository.layout.DefaultRepositoryLayout;
 import org.apache.maven.plugin.AbstractMojo;
 import org.apache.maven.plugin.MojoExecutionException;
 import org.apache.maven.plugin.MojoFailureException;
 import org.apache.maven.project.MavenProject;
 import org.apache.maven.model.Profile;
 import org.jvending.masa.CommandExecutor;
 import org.jvending.masa.ExecutionException;
 import org.codehaus.plexus.util.FileUtils;
 
 import java.io.File;
 import java.util.ArrayList;
 import java.util.List;
 
 /**
  * @goal compile
  * @phase compile
  * @requiresDependencyResolution compile
  * @description
  */
 public class AaptCompilerMojo extends AbstractMojo {
 
     /**
      * The maven project.
      *
      * @parameter expression="${project}"
      * @required
      * @readonly
      */
     private MavenProject project;
 
     /**
      * @parameter expression="${settings.localRepository}"
      * @required
      */
     private File localRepository;
 
     /**
      * @component
      */
     private ArtifactFactory artifactFactory;
 
     /**
     * @parameter default-value = "${androidVersion}"
      */
     private String androidVersion;
 
     /**
      * @parameter default-value="res"
      */
     private File resourceDirectory;
 
     /**
      * @parameter
      */
     private File androidManifestFile;
 
     /**
      * @parameter default-value=true
      */
     private boolean createPackageDirectories;
 
     /**
      * @parameter default-value=tests
      */
   //  private File platformUnitTestDirectory;
 
     public void execute() throws MojoExecutionException, MojoFailureException {
         // System.out.println("RS = " + resourceDirectory.getAbsolutePath());
         CommandExecutor executor = CommandExecutor.Factory.createDefaultCommmandExecutor();
         executor.setLogger(this.getLog());
 
         //Get rid of this annoying Thumbs.db problem on windows
 
         File thumbs = new File(resourceDirectory, "drawable/Thumbs.db");
         if (thumbs.exists()) {
             getLog().info("Deleting thumbs.db from resource directory");
             thumbs.delete();
         }
         
         if (androidManifestFile == null) {
             androidManifestFile = new File(resourceDirectory.getParent(), "AndroidManifest.xml");
         }
 
         String generatedSourceDirectoryName = project.getBuild().getDirectory() + File.separator + "generated-sources";
         if(!new File(generatedSourceDirectoryName).exists()) {
             new File(generatedSourceDirectoryName).mkdirs();
         }
         Artifact artifact = artifactFactory.createArtifact("android", "android", androidVersion, "jar", "jar");
         ArtifactRepositoryLayout defaultLayout = new DefaultRepositoryLayout();
         System.out.println(defaultLayout.pathOf(artifact));
         File androidJar = new File(localRepository, defaultLayout.pathOf(artifact));
 
         List<String> commands = new ArrayList<String>();
         commands.add("package");
         if (createPackageDirectories) {
             commands.add("-m");
         }
         commands.add("-J");
         commands.add(generatedSourceDirectoryName);
         commands.add("-M");
         commands.add(androidManifestFile.getAbsolutePath());
         if (resourceDirectory.exists()) {
             commands.add("-S");
             commands.add(resourceDirectory.getAbsolutePath());
         }
         commands.add("-I");
         commands.add(androidJar.getAbsolutePath());
         getLog().info("aapt " + commands.toString());
         try {
             executor.executeCommand("aapt", commands, project.getBasedir(), false);
         } catch (ExecutionException e) {
             throw new MojoExecutionException("", e);
         }
 
         project.addCompileSourceRoot(generatedSourceDirectoryName);
 
 //        if(System.getProperty("masa.debug") != null && platformUnitTestDirectory.exists())
 //        {
 //            project.addCompileSourceRoot(platformUnitTestDirectory.getAbsolutePath());
 //        }
     }
 }
