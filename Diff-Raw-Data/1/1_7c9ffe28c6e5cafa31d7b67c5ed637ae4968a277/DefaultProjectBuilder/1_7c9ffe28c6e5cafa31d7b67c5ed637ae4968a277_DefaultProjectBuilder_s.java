 package org.apache.maven.project;
 
 /*
  * Licensed to the Apache Software Foundation (ASF) under one or more contributor license
  * agreements. See the NOTICE file distributed with this work for additional information regarding
  * copyright ownership. The ASF licenses this file to you under the Apache License, Version 2.0 (the
  * "License"); you may not use this file except in compliance with the License. You may obtain a
  * copy of the License at
  * 
  * http://www.apache.org/licenses/LICENSE-2.0
  * 
  * Unless required by applicable law or agreed to in writing, software distributed under the License
  * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
  * or implied. See the License for the specific language governing permissions and limitations under
  * the License.
  */
 
 import java.io.File;
 import java.io.IOException;
 import java.util.ArrayList;
 import java.util.Arrays;
 import java.util.List;
 
 import org.apache.maven.Maven;
 import org.apache.maven.artifact.Artifact;
 import org.apache.maven.artifact.ArtifactUtils;
 import org.apache.maven.artifact.repository.DefaultRepositoryRequest;
 import org.apache.maven.artifact.repository.RepositoryRequest;
 import org.apache.maven.artifact.resolver.ArtifactResolutionException;
 import org.apache.maven.artifact.resolver.ArtifactResolutionRequest;
 import org.apache.maven.artifact.resolver.ArtifactResolutionResult;
 import org.apache.maven.artifact.resolver.ResolutionErrorHandler;
 import org.apache.maven.lifecycle.LifecycleExecutionException;
 import org.apache.maven.lifecycle.LifecycleExecutor;
 import org.apache.maven.model.Build;
 import org.apache.maven.model.Model;
 import org.apache.maven.model.Profile;
 import org.apache.maven.model.building.DefaultModelBuildingRequest;
 import org.apache.maven.model.building.FileModelSource;
 import org.apache.maven.model.building.ModelBuilder;
 import org.apache.maven.model.building.ModelBuildingException;
 import org.apache.maven.model.building.ModelBuildingRequest;
 import org.apache.maven.model.building.ModelBuildingResult;
 import org.apache.maven.model.building.ModelProblem;
 import org.apache.maven.model.building.UrlModelSource;
 import org.apache.maven.model.resolution.ModelResolver;
 import org.apache.maven.project.artifact.ProjectArtifact;
 import org.apache.maven.repository.RepositorySystem;
 import org.codehaus.plexus.component.annotations.Component;
 import org.codehaus.plexus.component.annotations.Requirement;
 import org.codehaus.plexus.logging.Logger;
 import org.codehaus.plexus.util.Os;
 import org.codehaus.plexus.util.StringUtils;
 
 /**
  * @version $Id$
  */
 @Component(role = ProjectBuilder.class)
 public class DefaultProjectBuilder
     implements ProjectBuilder
 {
     @Requirement
     private Logger logger;
 
     @Requirement
     private ModelBuilder modelBuilder;
 
     @Requirement
     private ProjectBuildingHelper projectBuildingHelper;
 
     @Requirement
     private LifecycleExecutor lifecycle;
 
     @Requirement
     private RepositorySystem repositorySystem;
 
     @Requirement
     private ResolutionErrorHandler resolutionErrorHandler;
 
     // ----------------------------------------------------------------------
     // MavenProjectBuilder Implementation
     // ----------------------------------------------------------------------
 
     public MavenProject build( File pomFile, ProjectBuildingRequest configuration )
         throws ProjectBuildingException
     {
         return build( pomFile, true, configuration );
     }
 
     private MavenProject build( File pomFile, boolean localProject, ProjectBuildingRequest configuration )
         throws ProjectBuildingException
     {
         ModelBuildingRequest request = getModelBuildingRequest( configuration, null );
 
         DefaultModelBuildingListener listener = new DefaultModelBuildingListener( projectBuildingHelper, configuration );
         request.setModelBuildingListeners( Arrays.asList( listener ) );
 
         if ( localProject )
         {
             request.setPomFile( pomFile );
         }
         else
         {
             request.setModelSource( new FileModelSource( pomFile ) );
         }
 
         ClassLoader oldContextClassLoader = Thread.currentThread().getContextClassLoader();
 
         try
         {
             ModelBuildingResult result;
             try
             {
                 result = modelBuilder.build( request );
             }
             catch ( ModelBuildingException e )
             {
                 throw new ProjectBuildingException( e.getModelId(), "Encountered POM errors", pomFile, e );
             }
 
             Model model = result.getEffectiveModel();
 
             if ( localProject && !result.getProblems().isEmpty() && logger.isWarnEnabled() )
             {
                 logger.warn( "" );
                 logger.warn( "Some problems were encountered while building the effective model for " + model.getId() );
 
                 for ( ModelProblem problem : result.getProblems() )
                 {
                     logger.warn( problem.getMessage() );
                 }
 
                 logger.warn( "" );
                 logger.warn( "It is highly recommended to fix these problems"
                     + " because they threaten the stability of your build." );
                 logger.warn( "" );
                 logger.warn( "For this reason, future Maven versions will no"
                     + " longer support building such malformed projects." );
                 logger.warn( "" );
             }
 
             MavenProject project = toProject( result, configuration, listener );
 
             return project;
         }
         finally
         {
             Thread.currentThread().setContextClassLoader( oldContextClassLoader );
         }
     }
 
     private List<String> getProfileIds( List<Profile> profiles )
     {
         List<String> ids = new ArrayList<String>( profiles.size() );
 
         for ( Profile profile : profiles )
         {
             ids.add( profile.getId() );
         }
 
         return ids;
     }
 
     private ModelBuildingRequest getModelBuildingRequest( ProjectBuildingRequest configuration,
                                                           ReactorModelPool reactorModelPool )
     {
         RepositoryRequest repositoryRequest = new DefaultRepositoryRequest();
         repositoryRequest.setCache( configuration.getRepositoryCache() );
         repositoryRequest.setLocalRepository( configuration.getLocalRepository() );
         repositoryRequest.setOffline( configuration.isOffline() );
 
         ModelResolver resolver =
             new RepositoryModelResolver( repositorySystem, resolutionErrorHandler, repositoryRequest,
                                          configuration.getRemoteRepositories(), reactorModelPool );
 
         ModelBuildingRequest request = new DefaultModelBuildingRequest();
 
         request.setValidationLevel( configuration.getValidationLevel() );
         request.setProcessPlugins( configuration.isProcessPlugins() );
         request.setProfiles( configuration.getProfiles() );
         request.setActiveProfileIds( configuration.getActiveProfileIds() );
         request.setInactiveProfileIds( configuration.getInactiveProfileIds() );
         request.setSystemProperties( configuration.getSystemProperties() );
         request.setUserProperties( configuration.getUserProperties() );
         request.setBuildStartTime( configuration.getBuildStartTime() );
         request.setModelResolver( resolver );
 
         return request;
     }
 
     public MavenProject build( Artifact artifact, ProjectBuildingRequest configuration )
         throws ProjectBuildingException
     {
         if ( !artifact.getType().equals( "pom" ) )
         {
             artifact = repositorySystem.createProjectArtifact( artifact.getGroupId(), artifact.getArtifactId(), artifact.getVersion() );
         }
 
         ArtifactResolutionRequest request = new ArtifactResolutionRequest()
             .setArtifact( artifact )
             .setCache( configuration.getRepositoryCache() )
             .setLocalRepository( configuration.getLocalRepository() )
             .setRemoteRepositories( configuration.getRemoteRepositories() );
         // FIXME setTransferListener
         ArtifactResolutionResult result = repositorySystem.resolve( request );
 
         try
         {
             resolutionErrorHandler.throwErrors( request, result );
         }
         catch ( ArtifactResolutionException e )
         {
             throw new ProjectBuildingException( artifact.getId(), "Error resolving project artifact.", e );
         }
 
         return build( artifact.getFile(), false, configuration );
     }
 
     /**
      * This is used for pom-less execution like running archetype:generate.
      * 
      * I am taking out the profile handling and the interpolation of the base directory until we
      * spec this out properly.
      */
     public MavenProject buildStandaloneSuperProject( ProjectBuildingRequest config )
         throws ProjectBuildingException
     {
         ModelBuildingRequest request = getModelBuildingRequest( config, null );
 
         DefaultModelBuildingListener listener = new DefaultModelBuildingListener( projectBuildingHelper, config );
         request.setModelBuildingListeners( Arrays.asList( listener ) );
 
         request.setModelSource( new UrlModelSource( getClass().getResource( "standalone.xml" ) ) );
 
         ModelBuildingResult result;
         try
         {
             result = modelBuilder.build( request );
         }
         catch ( ModelBuildingException e )
         {
             throw new ProjectBuildingException( "[standalone]", "Failed to build standalone project", e );
         }
 
         MavenProject standaloneProject = new MavenProject( result.getEffectiveModel(), repositorySystem, this, config );
 
         standaloneProject.setActiveProfiles( result.getActiveExternalProfiles() );
         standaloneProject.setInjectedProfileIds( "external", getProfileIds( result.getActiveExternalProfiles() ) );
         standaloneProject.setRemoteArtifactRepositories( listener.getRemoteRepositories() );
         standaloneProject.setPluginArtifactRepositories( listener.getPluginRepositories() );
 
         standaloneProject.setExecutionRoot( true );
 
         return standaloneProject;
     }
 
     public MavenProjectBuildingResult buildProjectWithDependencies( File pomFile, ProjectBuildingRequest request )
         throws ProjectBuildingException
     {
         MavenProject project = build( pomFile, request );
 
         Artifact artifact = new ProjectArtifact( project );                     
         
         ArtifactResolutionRequest artifactRequest = new ArtifactResolutionRequest()
             .setArtifact( artifact )
             .setResolveRoot( false )
             .setResolveTransitively( true )
             .setCache( request.getRepositoryCache() )
             .setLocalRepository( request.getLocalRepository() )
             .setRemoteRepositories( project.getRemoteArtifactRepositories() )
             .setManagedVersionMap( project.getManagedVersionMap() );
         // FIXME setTransferListener
         ArtifactResolutionResult result;
 
         ClassLoader oldContextClassLoader = Thread.currentThread().getContextClassLoader();
 
         try
         {
             if ( project.getClassRealm() != null )
             {
                 Thread.currentThread().setContextClassLoader( project.getClassRealm() );
             }
 
             result = repositorySystem.resolve( artifactRequest );
         }
         finally
         {
             Thread.currentThread().setContextClassLoader( oldContextClassLoader );
         }
 
         if ( result.hasExceptions() )
         {
             Exception e = result.getExceptions().get( 0 );
 
             throw new ProjectBuildingException( safeVersionlessKey( project.getGroupId(), project.getArtifactId() ), "Unable to build project due to an invalid dependency version: " + e.getMessage(),
                                                 pomFile, e );
         }
 
         project.setArtifacts( result.getArtifacts() );
         
         return new MavenProjectBuildingResult( project, result );
     }
 
     public List<ProjectBuildingResult> build( List<File> pomFiles, boolean recursive, ProjectBuildingRequest config )
         throws ProjectBuildingException
     {
         List<ProjectBuildingResult> results = new ArrayList<ProjectBuildingResult>();
 
         List<InterimResult> interimResults = new ArrayList<InterimResult>();
 
         ReactorModelPool reactorModelPool = new ReactorModelPool();
 
         ReactorModelCache modelCache = new ReactorModelCache();
 
         boolean errors = build( results, interimResults, pomFiles, recursive, config, reactorModelPool, modelCache );
 
         for ( InterimResult interimResult : interimResults )
         {
             Model model = interimResult.result.getEffectiveModel();
             reactorModelPool.put( model.getGroupId(), model.getArtifactId(), model.getVersion(), model.getPomFile() );
         }
 
         ClassLoader oldContextClassLoader = Thread.currentThread().getContextClassLoader();
 
         try
         {
             for ( InterimResult interimResult : interimResults )
             {
                 try
                 {
                     ModelBuildingResult result = modelBuilder.build( interimResult.request, interimResult.result );
 
                     MavenProject project = toProject( result, config, interimResult.listener );
 
                     results.add( new DefaultProjectBuildingResult( project, result.getProblems() ) );
                 }
                 catch ( ModelBuildingException e )
                 {
                     results.add( new DefaultProjectBuildingResult( e.getModelId(), interimResult.pomFile,
                                                                    e.getProblems() ) );
 
                     errors = true;
                 }
             }
         }
         finally
         {
             Thread.currentThread().setContextClassLoader( oldContextClassLoader );
         }
 
         if ( errors )
         {
             throw new ProjectBuildingException( results );
         }
 
         return results;
     }
 
     private boolean build( List<ProjectBuildingResult> results, List<InterimResult> interimResults,
                            List<File> pomFiles, boolean recursive, ProjectBuildingRequest config,
                            ReactorModelPool reactorModelPool, ReactorModelCache modelCache )
     {
         boolean errors = false;
 
         for ( File pomFile : pomFiles )
         {
             ModelBuildingRequest request = getModelBuildingRequest( config, reactorModelPool );
 
             request.setPomFile( pomFile );
             request.setTwoPhaseBuilding( true );
             request.setModelCache( modelCache );
 
             DefaultModelBuildingListener listener = new DefaultModelBuildingListener( projectBuildingHelper, config );
             request.setModelBuildingListeners( Arrays.asList( listener ) );
 
             try
             {
                 ModelBuildingResult result = modelBuilder.build( request );
 
                 Model model = result.getEffectiveModel();
 
                 interimResults.add( new InterimResult( pomFile, request, result, listener ) );
 
                 if ( recursive && !model.getModules().isEmpty() )
                 {
                     File basedir = pomFile.getParentFile();
 
                     List<File> moduleFiles = new ArrayList<File>();
 
                     for ( String module : model.getModules() )
                     {
                         if ( StringUtils.isEmpty( module ) )
                         {
                             continue;
                         }
 
                         File moduleFile = new File( basedir, module );
 
                         if ( moduleFile.isDirectory() )
                         {
                             moduleFile = new File( moduleFile, Maven.POMv4 );
                         }
 
                         if ( !moduleFile.isFile() )
                         {
                             String source = toSourceHint( model );
                             ModelProblem problem =
                                 new ModelProblem( "Child module " + moduleFile + " of " + source + " does not exist",
                                                   ModelProblem.Severity.ERROR, source );
                             result.getProblems().add( problem );
 
                             errors = true;
 
                             continue;
                         }
 
                         if ( Os.isFamily( Os.FAMILY_WINDOWS ) )
                         {
                             // we don't canonicalize on unix to avoid interfering with symlinks
                             try
                             {
                                 moduleFile = moduleFile.getCanonicalFile();
                             }
                             catch ( IOException e )
                             {
                                 moduleFile = moduleFile.getAbsoluteFile();
                             }
                         }
                         else
                         {
                             moduleFile = new File( moduleFile.toURI().normalize() );
                         }
 
                         moduleFiles.add( moduleFile );
                     }
 
                     errors =
                         build( results, interimResults, moduleFiles, recursive, config, reactorModelPool, modelCache )
                             || errors;
                 }
             }
             catch ( ModelBuildingException e )
             {
                 results.add( new DefaultProjectBuildingResult( e.getModelId(), pomFile, e.getProblems() ) );
 
                 errors = true;
             }
         }
 
         return errors;
     }
 
     static class InterimResult
     {
 
         File pomFile;
 
         ModelBuildingRequest request;
 
         ModelBuildingResult result;
 
         DefaultModelBuildingListener listener;
 
         InterimResult( File pomFile, ModelBuildingRequest request, ModelBuildingResult result,
                        DefaultModelBuildingListener listener )
         {
             this.pomFile = pomFile;
             this.request = request;
             this.result = result;
             this.listener = listener;
         }
 
     }
 
     private MavenProject toProject( ModelBuildingResult result, ProjectBuildingRequest configuration,
                                     DefaultModelBuildingListener listener )
         throws ProjectBuildingException
     {
         Model model = result.getEffectiveModel();
 
         MavenProject project = new MavenProject( model, repositorySystem, this, configuration );
 
         project.setFile( model.getPomFile() );
 
         File parentPomFile = result.getRawModel( result.getModelIds().get( 1 ) ).getPomFile();
         project.setParentFile( parentPomFile );
 
         Artifact projectArtifact =
             repositorySystem.createArtifact( project.getGroupId(), project.getArtifactId(), project.getVersion(), null,
                                              project.getPackaging() );
         project.setArtifact( projectArtifact );
 
         project.setOriginalModel( result.getRawModel() );
 
         project.setRemoteArtifactRepositories( listener.getRemoteRepositories() );
         project.setPluginArtifactRepositories( listener.getPluginRepositories() );
 
         project.setClassRealm( listener.getProjectRealm() );
 
         try
         {
             if ( configuration.isProcessPlugins() && configuration.isProcessPluginConfiguration() )
             {
                 RepositoryRequest repositoryRequest = new DefaultRepositoryRequest();
                 repositoryRequest.setLocalRepository( configuration.getLocalRepository() );
                 repositoryRequest.setRemoteRepositories( project.getPluginArtifactRepositories() );
                 repositoryRequest.setCache( configuration.getRepositoryCache() );
                 repositoryRequest.setOffline( configuration.isOffline() );
 
                 lifecycle.populateDefaultConfigurationForPlugins( model.getBuild().getPlugins(), repositoryRequest );
             }
         }
         catch ( LifecycleExecutionException e )
         {
             throw new ProjectBuildingException( project.getId(), e.getMessage(), e );
         }
 
         Build build = project.getBuild();
         project.addScriptSourceRoot( build.getScriptSourceDirectory() );
         project.addCompileSourceRoot( build.getSourceDirectory() );
         project.addTestCompileSourceRoot( build.getTestSourceDirectory() );
 
         List<Profile> activeProfiles = new ArrayList<Profile>();
         activeProfiles.addAll( result.getActivePomProfiles( result.getModelIds().get( 0 ) ) );
         activeProfiles.addAll( result.getActiveExternalProfiles() );
         project.setActiveProfiles( activeProfiles );
 
         project.setInjectedProfileIds( "external", getProfileIds( result.getActiveExternalProfiles() ) );
         for ( String modelId : result.getModelIds() )
         {
             project.setInjectedProfileIds( modelId, getProfileIds( result.getActivePomProfiles( modelId ) ) );
         }
 
         return project;
     }
 
     private static String safeVersionlessKey( String groupId, String artifactId )
     {
         String gid = groupId;
 
         if ( StringUtils.isEmpty( gid ) )
         {
             gid = "unknown";
         }
 
         String aid = artifactId;
 
         if ( StringUtils.isEmpty( aid ) )
         {
             aid = "unknown";
         }
 
         return ArtifactUtils.versionlessKey( gid, aid );
     }
 
     private String toSourceHint( Model model )
     {
         StringBuilder buffer = new StringBuilder( 192 );
 
         buffer.append( model.getGroupId() );
         buffer.append( ':' );
         buffer.append( model.getArtifactId() );
         buffer.append( ':' );
         buffer.append( model.getVersion() );
 
         File pomFile = model.getPomFile();
         if ( pomFile != null )
         {
             buffer.append( " (" ).append( pomFile ).append( ")" );
         }
 
         return buffer.toString();
     }
 
 }
