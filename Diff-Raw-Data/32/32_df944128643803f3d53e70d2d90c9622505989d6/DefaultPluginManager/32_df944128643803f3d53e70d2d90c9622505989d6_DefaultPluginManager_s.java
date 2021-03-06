 package org.apache.maven.plugin;
 
 /*
  * Copyright 2001-2005 The Apache Software Foundation.
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
 
 import org.apache.maven.artifact.Artifact;
 import org.apache.maven.artifact.factory.ArtifactFactory;
 import org.apache.maven.artifact.metadata.ArtifactMetadataRetrievalException;
 import org.apache.maven.artifact.metadata.ArtifactMetadataSource;
 import org.apache.maven.artifact.metadata.ResolutionGroup;
 import org.apache.maven.artifact.repository.ArtifactRepository;
 import org.apache.maven.artifact.resolver.ArtifactResolutionException;
 import org.apache.maven.artifact.resolver.ArtifactResolutionResult;
 import org.apache.maven.artifact.resolver.ArtifactResolver;
 import org.apache.maven.artifact.resolver.filter.ArtifactFilter;
 import org.apache.maven.artifact.resolver.filter.ExclusionSetFilter;
 import org.apache.maven.artifact.resolver.filter.ScopeArtifactFilter;
 import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
 import org.apache.maven.artifact.versioning.InvalidVersionSpecificationException;
 import org.apache.maven.artifact.versioning.VersionRange;
 import org.apache.maven.execution.MavenSession;
 import org.apache.maven.execution.RuntimeInformation;
 import org.apache.maven.model.Plugin;
 import org.apache.maven.model.ReportPlugin;
 import org.apache.maven.monitor.event.EventDispatcher;
 import org.apache.maven.monitor.event.MavenEvents;
 import org.apache.maven.monitor.logging.DefaultLog;
 import org.apache.maven.plugin.descriptor.MojoDescriptor;
 import org.apache.maven.plugin.descriptor.Parameter;
 import org.apache.maven.plugin.descriptor.PluginDescriptor;
 import org.apache.maven.plugin.descriptor.PluginDescriptorBuilder;
 import org.apache.maven.plugin.logging.Log;
 import org.apache.maven.plugin.version.PluginVersionManager;
 import org.apache.maven.plugin.version.PluginVersionResolutionException;
 import org.apache.maven.project.MavenProject;
 import org.apache.maven.project.MavenProjectBuilder;
 import org.apache.maven.project.ProjectBuildingException;
 import org.apache.maven.project.artifact.ActiveProjectArtifact;
 import org.apache.maven.project.artifact.MavenMetadataSource;
 import org.apache.maven.project.path.PathTranslator;
 import org.apache.maven.reporting.MavenReport;
 import org.apache.maven.settings.Settings;
 import org.codehaus.plexus.PlexusConstants;
 import org.codehaus.plexus.PlexusContainer;
 import org.codehaus.plexus.PlexusContainerException;
 import org.codehaus.plexus.component.configurator.ComponentConfigurationException;
 import org.codehaus.plexus.component.configurator.ComponentConfigurator;
 import org.codehaus.plexus.component.configurator.expression.ExpressionEvaluationException;
 import org.codehaus.plexus.component.configurator.expression.ExpressionEvaluator;
 import org.codehaus.plexus.component.repository.exception.ComponentLifecycleException;
 import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
 import org.codehaus.plexus.configuration.PlexusConfiguration;
 import org.codehaus.plexus.configuration.xml.XmlPlexusConfiguration;
 import org.codehaus.plexus.context.Context;
 import org.codehaus.plexus.context.ContextException;
 import org.codehaus.plexus.logging.AbstractLogEnabled;
 import org.codehaus.plexus.personality.plexus.lifecycle.phase.Contextualizable;
 import org.codehaus.plexus.personality.plexus.lifecycle.phase.Initializable;
 import org.codehaus.plexus.util.StringUtils;
 import org.codehaus.plexus.util.xml.Xpp3Dom;
 
 import java.io.File;
 import java.io.IOException;
 import java.lang.reflect.Field;
 import java.net.URL;
 import java.util.ArrayList;
 import java.util.Collection;
 import java.util.Collections;
 import java.util.HashMap;
 import java.util.HashSet;
 import java.util.Iterator;
 import java.util.List;
 import java.util.Map;
 import java.util.Set;
 
 public class DefaultPluginManager
     extends AbstractLogEnabled
     implements PluginManager, Initializable, Contextualizable
 {
     protected PlexusContainer container;
 
     protected PluginDescriptorBuilder pluginDescriptorBuilder;
 
     protected ArtifactFilter artifactFilter;
 
     private Log mojoLogger;
 
     private Map resolvedCoreArtifactFiles = new HashMap();
 
     // component requirements
     protected PathTranslator pathTranslator;
 
     protected MavenPluginCollector pluginCollector;
 
     protected PluginVersionManager pluginVersionManager;
 
     protected ArtifactFactory artifactFactory;
 
     protected ArtifactResolver artifactResolver;
 
     protected ArtifactMetadataSource artifactMetadataSource;
 
     protected RuntimeInformation runtimeInformation;
 
     protected MavenProjectBuilder mavenProjectBuilder;
 
     protected PluginMappingManager pluginMappingManager;
 
     // END component requirements
 
     public DefaultPluginManager()
     {
         pluginDescriptorBuilder = new PluginDescriptorBuilder();
     }
 
     // ----------------------------------------------------------------------
     //
     // ----------------------------------------------------------------------
 
     public PluginDescriptor getPluginDescriptorForPrefix( String prefix )
     {
         return pluginCollector.getPluginDescriptorForPrefix( prefix );
     }
 
     public Plugin getPluginDefinitionForPrefix( String prefix, MavenSession session, MavenProject project )
         throws PluginManagerException
     {
         // TODO: since this is only used in the lifecycle executor, maybe it should be moved there? There is no other
         // use for the mapping manager in here
         return pluginMappingManager.getByPrefix( prefix, session.getSettings().getPluginGroups(),
                                                  project.getPluginArtifactRepositories(),
                                                  session.getLocalRepository() );
     }
 
     public PluginDescriptor verifyPlugin( Plugin plugin, MavenProject project, Settings settings,
                                           ArtifactRepository localRepository )
         throws ArtifactResolutionException, PluginManagerException, PluginVersionResolutionException
     {
         // TODO: this should be possibly outside
         // All version-resolution logic has been moved to DefaultPluginVersionManager.
         if ( plugin.getVersion() == null )
         {
             String version = pluginVersionManager.resolvePluginVersion( plugin.getGroupId(), plugin.getArtifactId(),
                                                                         project, settings, localRepository );
             plugin.setVersion( version );
         }
 
         return verifyVersionedPlugin( plugin, project, localRepository );
     }
 
     private PluginDescriptor verifyVersionedPlugin( Plugin plugin, MavenProject project,
                                                     ArtifactRepository localRepository )
         throws PluginVersionResolutionException, PluginManagerException, ArtifactResolutionException
     {
         // TODO: this might result in an artifact "RELEASE" being resolved continuously
         // FIXME: need to find out how a plugin gets marked as 'installed'
         // and no ChildContainer exists. The check for that below fixes
         // the 'Can't find plexus container for plugin: xxx' error.
         if ( !pluginCollector.isPluginInstalled( plugin ) || container.getChildContainer( plugin.getKey() ) == null )
         {
             try
             {
                 VersionRange versionRange = VersionRange.createFromVersionSpec( plugin.getVersion() );
 
                 List remoteRepositories = new ArrayList();
                 remoteRepositories.addAll( project.getPluginArtifactRepositories() );
                 remoteRepositories.addAll( project.getRemoteArtifactRepositories() );
 
                 checkRequiredMavenVersion( plugin, localRepository, remoteRepositories );
 
                 Artifact pluginArtifact = artifactFactory.createPluginArtifact( plugin.getGroupId(),
                                                                                 plugin.getArtifactId(), versionRange );
                 addPlugin( plugin, pluginArtifact, project, localRepository );
 
                 project.addPlugin( plugin );
             }
             catch ( ArtifactResolutionException e )
             {
                 String groupId = plugin.getGroupId();
                 String artifactId = plugin.getArtifactId();
                 String version = plugin.getVersion();
 
                 if ( groupId == null || artifactId == null || version == null )
                 {
                     throw new PluginNotFoundException( e );
                 }
                 else if ( groupId.equals( e.getGroupId() ) && artifactId.equals( e.getArtifactId() ) &&
                     version.equals( e.getVersion() ) && "maven-plugin".equals( e.getType() ) )
                 {
                     throw new PluginNotFoundException( e );
                 }
                 else
                 {
                     throw e;
                 }
             }
             catch ( InvalidVersionSpecificationException e )
             {
                 throw new PluginVersionResolutionException( plugin.getGroupId(), plugin.getArtifactId(),
                                                             "Invalid version specification", e );
             }
         }
 
         return pluginCollector.getPluginDescriptor( plugin );
     }
 
     /**
      * @todo would be better to store this in the plugin descriptor, but then it won't be available to the version
      * manager which executes before the plugin is instantiated
      */
     private void checkRequiredMavenVersion( Plugin plugin, ArtifactRepository localRepository, List remoteRepositories )
         throws PluginVersionResolutionException, PluginManagerException
     {
         try
         {
             Artifact artifact = artifactFactory.createProjectArtifact( plugin.getGroupId(), plugin.getArtifactId(),
                                                                        plugin.getVersion() );
             MavenProject project = mavenProjectBuilder.buildFromRepository( artifact, remoteRepositories,
                                                                             localRepository );
             // if we don't have the required Maven version, then ignore an update
             if ( project.getPrerequisites() != null && project.getPrerequisites().getMaven() != null )
             {
                 DefaultArtifactVersion requiredVersion = new DefaultArtifactVersion(
                     project.getPrerequisites().getMaven() );
                 if ( runtimeInformation.getApplicationVersion().compareTo( requiredVersion ) < 0 )
                 {
                     throw new PluginVersionResolutionException( plugin.getGroupId(), plugin.getArtifactId(),
                                                                 "Plugin requires Maven version " + requiredVersion );
                 }
             }
         }
         catch ( ProjectBuildingException e )
         {
             throw new PluginVersionResolutionException( plugin.getGroupId(), plugin.getArtifactId(),
                                                         "Unable to build project for plugin", e );
         }
         catch ( IOException e )
         {
             throw new PluginManagerException( "Unable to determine Maven version for comparison", e );
         }
     }
 
     protected void addPlugin( Plugin plugin, Artifact pluginArtifact, MavenProject project,
                               ArtifactRepository localRepository )
         throws ArtifactResolutionException, PluginManagerException
     {
         // TODO: share with MMS? Not sure if it belongs here
         if ( project.getProjectReferences() != null && !project.getProjectReferences().isEmpty() )
         {
             // TODO: use MavenProject getProjectReferenceId
             String refId = plugin.getGroupId() + ":" + plugin.getArtifactId();
             MavenProject ref = (MavenProject) project.getProjectReferences().get( refId );
             if ( ref != null && ref.getArtifact() != null )
             {
                 // TODO: if not matching, we should get the correct artifact from that project (attached)
                 if ( ref.getArtifact().getDependencyConflictId().equals( pluginArtifact.getDependencyConflictId() ) )
                 {
                     // if the project artifact doesn't exist, don't use it. We haven't built that far.
                    if ( project.getArtifact().getFile() != null && project.getArtifact().getFile().exists() )
                     {
                         pluginArtifact = new ActiveProjectArtifact( ref, pluginArtifact );
                     }
                     else
                     {
                         getLogger().warn( "Plugin found in the reactor has not been built when it's use was attempted" +
                             " - resolving from the repository instead" );
                     }
                 }
             }
         }
 
         artifactResolver.resolve( pluginArtifact, project.getPluginArtifactRepositories(), localRepository );
 
         if ( !pluginArtifact.isResolved() )
         {
             throw new PluginContainerException( plugin, "Cannot resolve artifact for plugin." );
         }
 
         PlexusContainer child;
         try
         {
             child = container.createChildContainer( plugin.getKey(),
                                                     Collections.singletonList( pluginArtifact.getFile() ),
                                                     Collections.EMPTY_MAP,
                                                     Collections.singletonList( pluginCollector ) );
         }
         catch ( PlexusContainerException e )
         {
             throw new PluginContainerException( plugin, "Failed to create plugin container.", e );
         }
 
         // this plugin's descriptor should have been discovered in the child creation, so we should be able to
         // circle around and set the artifacts and class realm
         PluginDescriptor addedPlugin = pluginCollector.getPluginDescriptor( plugin );
 
         addedPlugin.setClassRealm( child.getContainerRealm() );
 
         // we're only setting the plugin's artifact itself as the artifact list, to allow it to be retrieved
         // later when the plugin is first invoked. Retrieving this artifact will in turn allow us to
         // transitively resolve its dependencies, and add them to the plugin container...
         addedPlugin.setArtifacts( Collections.singletonList( pluginArtifact ) );
 
         try
         {
             Set artifacts = MavenMetadataSource.createArtifacts( artifactFactory, plugin.getDependencies(), null, null,
                                                                  project.getProjectReferences() );
             addedPlugin.setIntroducedDependencyArtifacts( artifacts );
         }
         catch ( InvalidVersionSpecificationException e )
         {
             throw new PluginManagerException( "Unable to get one of the plugins additional dependencies", e );
         }
     }
 
     // ----------------------------------------------------------------------
     // Mojo execution
     // ----------------------------------------------------------------------
 
     public void executeMojo( MavenProject project, MojoExecution mojoExecution, MavenSession session )
         throws ArtifactResolutionException, PluginManagerException, MojoExecutionException, MojoFailureException
     {
         MojoDescriptor mojoDescriptor = mojoExecution.getMojoDescriptor();
 
         // NOTE: I'm putting these checks in here, since this is the central point of access for
         // anything that wants to execute a mojo.
         if ( mojoDescriptor.isProjectRequired() && !session.isUsingPOMsFromFilesystem() )
         {
             throw new MojoExecutionException( "Cannot execute mojo: " + mojoDescriptor.getGoal() +
                 ". It requires a project, but the build is not using one." );
         }
 
         if ( mojoDescriptor.isOnlineRequired() && session.getSettings().isOffline() )
         {
             // TODO: Should we error out, or simply warn and skip??
             throw new MojoExecutionException( "Mojo: " + mojoDescriptor.getGoal() +
                 " requires online mode for execution. Maven is currently offline." );
         }
 
         if ( mojoDescriptor.isDependencyResolutionRequired() != null )
         {
             Collection projects;
 
             if ( mojoDescriptor.isAggregator() )
             {
                 projects = session.getSortedProjects();
             }
             else
             {
                 projects = Collections.singleton( project );
             }
 
             for ( Iterator i = projects.iterator(); i.hasNext(); )
             {
                 MavenProject p = (MavenProject) i.next();
                 resolveTransitiveDependencies( session, artifactResolver,
                                                mojoDescriptor.isDependencyResolutionRequired(), artifactFactory, p );
             }
 
             downloadDependencies( project, session, artifactResolver );
         }
 
         String goalName = mojoDescriptor.getFullGoalName();
 
         Mojo plugin;
 
         try
         {
             PluginDescriptor pluginDescriptor = mojoDescriptor.getPluginDescriptor();
             String goalId = mojoDescriptor.getGoal();
             String groupId = pluginDescriptor.getGroupId();
             String artifactId = pluginDescriptor.getArtifactId();
             String executionId = mojoExecution.getExecutionId();
             Xpp3Dom dom = project.getGoalConfiguration( groupId, artifactId, executionId, goalId );
             Xpp3Dom reportDom = project.getReportConfiguration( groupId, artifactId, executionId );
             dom = Xpp3Dom.mergeXpp3Dom( dom, reportDom );
             if ( mojoExecution.getConfiguration() != null )
             {
                 dom = Xpp3Dom.mergeXpp3Dom( dom, mojoExecution.getConfiguration() );
             }
 
             plugin = getConfiguredMojo( session, dom, project, false, mojoExecution );
         }
         catch ( PluginConfigurationException e )
         {
             String msg = "Error configuring plugin for execution of '" + goalName + "'.";
             throw new MojoExecutionException( msg, e );
         }
         catch ( ComponentLookupException e )
         {
             throw new MojoExecutionException( "Error looking up plugin: ", e );
         }
 
         // Event monitoring.
         String event = MavenEvents.MOJO_EXECUTION;
         EventDispatcher dispatcher = session.getEventDispatcher();
 
         String goalExecId = goalName;
 
         if ( mojoExecution.getExecutionId() != null )
         {
             goalExecId += " {execution: " + mojoExecution.getExecutionId() + "}";
         }
 
         dispatcher.dispatchStart( event, goalExecId );
 
         ClassLoader oldClassLoader = Thread.currentThread().getContextClassLoader();
 
         try
         {
             Thread.currentThread().setContextClassLoader(
                 mojoDescriptor.getPluginDescriptor().getClassRealm().getClassLoader() );
 
             plugin.execute();
 
             dispatcher.dispatchEnd( event, goalExecId );
         }
         catch ( MojoExecutionException e )
         {
             session.getEventDispatcher().dispatchError( event, goalExecId, e );
 
             throw e;
         }
         catch ( MojoFailureException e )
         {
             session.getEventDispatcher().dispatchError( event, goalExecId, e );
 
             throw e;
         }
         finally
         {
 
             Thread.currentThread().setContextClassLoader( oldClassLoader );
 
             try
             {
                 PlexusContainer pluginContainer = getPluginContainer( mojoDescriptor.getPluginDescriptor() );
 
                 pluginContainer.release( plugin );
             }
             catch ( ComponentLifecycleException e )
             {
                 if ( getLogger().isErrorEnabled() )
                 {
                     getLogger().error( "Error releasing plugin - ignoring.", e );
                 }
             }
         }
     }
 
     public MavenReport getReport( MavenProject project, MojoExecution mojoExecution, MavenSession session )
         throws PluginManagerException
     {
         MojoDescriptor mojoDescriptor = mojoExecution.getMojoDescriptor();
         PluginDescriptor descriptor = mojoDescriptor.getPluginDescriptor();
         Xpp3Dom dom = project.getReportConfiguration( descriptor.getGroupId(), descriptor.getArtifactId(),
                                                       mojoExecution.getExecutionId() );
         if ( mojoExecution.getConfiguration() != null )
         {
             dom = Xpp3Dom.mergeXpp3Dom( dom, mojoExecution.getConfiguration() );
         }
 
         MavenReport reportMojo;
         try
         {
             reportMojo = (MavenReport) getConfiguredMojo( session, dom, project, true, mojoExecution );
         }
         catch ( ComponentLookupException e )
         {
             throw new PluginManagerException( "Error looking up report: ", e );
         }
         catch ( PluginConfigurationException e )
         {
             throw new PluginManagerException( "Error configuring report: ", e );
         }
         return reportMojo;
     }
 
     public PluginDescriptor verifyReportPlugin( ReportPlugin reportPlugin, MavenProject project, MavenSession session )
         throws PluginVersionResolutionException, ArtifactResolutionException, PluginManagerException
     {
         String version = reportPlugin.getVersion();
 
         if ( version == null )
         {
             version = pluginVersionManager.resolveReportPluginVersion( reportPlugin.getGroupId(),
                                                                        reportPlugin.getArtifactId(), project,
                                                                        session.getSettings(),
                                                                        session.getLocalRepository() );
             reportPlugin.setVersion( version );
         }
 
         Plugin forLookup = new Plugin();
 
         forLookup.setGroupId( reportPlugin.getGroupId() );
         forLookup.setArtifactId( reportPlugin.getArtifactId() );
         forLookup.setVersion( version );
 
         return verifyVersionedPlugin( forLookup, project, session.getLocalRepository() );
     }
 
     private PlexusContainer getPluginContainer( PluginDescriptor pluginDescriptor )
         throws PluginManagerException
     {
         String pluginKey = pluginDescriptor.getPluginLookupKey();
 
         PlexusContainer pluginContainer = container.getChildContainer( pluginKey );
 
         if ( pluginContainer == null )
         {
             throw new PluginManagerException( "Cannot find PlexusContainer for plugin: " + pluginKey );
         }
         return pluginContainer;
     }
 
     private Mojo getConfiguredMojo( MavenSession session, Xpp3Dom dom, MavenProject project, boolean report,
                                     MojoExecution mojoExecution )
         throws ComponentLookupException, PluginConfigurationException, PluginManagerException
     {
         MojoDescriptor mojoDescriptor = mojoExecution.getMojoDescriptor();
 
         PluginDescriptor pluginDescriptor = mojoDescriptor.getPluginDescriptor();
 
         PlexusContainer pluginContainer = getPluginContainer( pluginDescriptor );
 
         // if this is the first time this plugin has been used, the plugin's container will only
         // contain the plugin's artifact in isolation; we need to finish resolving the plugin's
         // dependencies, and add them to the container.
         ensurePluginContainerIsComplete( pluginDescriptor, pluginContainer, project, session );
 
         Mojo plugin = (Mojo) pluginContainer.lookup( Mojo.ROLE, mojoDescriptor.getRoleHint() );
         if ( report && !( plugin instanceof MavenReport ) )
         {
             // TODO: the mojoDescriptor should actually capture this information so we don't get this far
             return null;
         }
 
         if ( plugin instanceof ContextEnabled )
         {
             Map pluginContext = session.getPluginContext( pluginDescriptor, project );
 
             ( (ContextEnabled) plugin ).setPluginContext( pluginContext );
         }
 
         plugin.setLog( mojoLogger );
 
         XmlPlexusConfiguration pomConfiguration;
         if ( dom == null )
         {
             pomConfiguration = new XmlPlexusConfiguration( "configuration" );
         }
         else
         {
             pomConfiguration = new XmlPlexusConfiguration( dom );
         }
 
         // Validate against non-editable (@readonly) parameters, to make sure users aren't trying to
         // override in the POM.
         validatePomConfiguration( mojoDescriptor, pomConfiguration );
 
         PlexusConfiguration mergedConfiguration = mergeMojoConfiguration( pomConfiguration, mojoDescriptor );
 
         // TODO: plexus changes to make this more like the component descriptor so this can be used instead
         //            PlexusConfiguration mergedConfiguration = mergeConfiguration( pomConfiguration,
         //                                                                          mojoDescriptor.getConfiguration() );
 
         ExpressionEvaluator expressionEvaluator = new PluginParameterExpressionEvaluator( session, mojoExecution,
                                                                                           pathTranslator, getLogger(),
                                                                                           project,
                                                                                           session.getExecutionProperties() );
 
         PlexusConfiguration extractedMojoConfiguration = extractMojoConfiguration( mergedConfiguration,
                                                                                    mojoDescriptor );
 
        checkRequiredParameters( mojoDescriptor, extractedMojoConfiguration, expressionEvaluator, plugin );
 
         populatePluginFields( plugin, mojoDescriptor, extractedMojoConfiguration, pluginContainer,
                               expressionEvaluator );
         return plugin;
     }
 
     private void ensurePluginContainerIsComplete( PluginDescriptor pluginDescriptor, PlexusContainer pluginContainer,
                                                   MavenProject project, MavenSession session )
         throws PluginConfigurationException
     {
         // if the plugin's already been used once, don't re-do this step...
         // otherwise, we have to finish resolving the plugin's classpath and start the container.
         if ( pluginDescriptor.getArtifacts() != null && pluginDescriptor.getArtifacts().size() == 1 )
         {
             Artifact pluginArtifact = (Artifact) pluginDescriptor.getArtifacts().get( 0 );
 
             try
             {
                 ArtifactRepository localRepository = session.getLocalRepository();
 
                 ResolutionGroup resolutionGroup = artifactMetadataSource.retrieve( pluginArtifact, localRepository,
                                                                                    project.getPluginArtifactRepositories() );
 
                 Set dependencies = new HashSet( resolutionGroup.getArtifacts() );
 
                 dependencies.addAll( pluginDescriptor.getIntroducedDependencyArtifacts() );
 
                 ArtifactResolutionResult result = artifactResolver.resolveTransitively( dependencies, pluginArtifact,
                                                                                         localRepository,
                                                                                         resolutionGroup.getResolutionRepositories(),
                                                                                         artifactMetadataSource,
                                                                                         artifactFilter );
 
                 Set resolved = result.getArtifacts();
 
                 for ( Iterator it = resolved.iterator(); it.hasNext(); )
                 {
                     Artifact artifact = (Artifact) it.next();
 
                     if ( !artifact.equals( pluginArtifact ) )
                     {
                         pluginContainer.addJarResource( artifact.getFile() );
                     }
                 }
 
                 pluginDescriptor.setClassRealm( pluginContainer.getContainerRealm() );
 
                 List unresolved = new ArrayList( dependencies );
 
                 unresolved.removeAll( resolved );
 
                 resolveCoreArtifacts( unresolved, localRepository, resolutionGroup.getResolutionRepositories() );
 
                 List allResolved = new ArrayList( resolved.size() + unresolved.size() );
 
                 allResolved.addAll( resolved );
                 allResolved.addAll( unresolved );
 
                 pluginDescriptor.setArtifacts( allResolved );
             }
             catch ( ArtifactResolutionException e )
             {
                 throw new PluginConfigurationException( pluginDescriptor, "Cannot resolve plugin dependencies", e );
             }
             catch ( PlexusContainerException e )
             {
                 throw new PluginConfigurationException( pluginDescriptor, "Cannot start plugin container", e );
             }
             catch ( ArtifactMetadataRetrievalException e )
             {
                 throw new PluginConfigurationException( pluginDescriptor, "Cannot resolve plugin dependencies", e );
             }
         }
     }
 
     private void resolveCoreArtifacts( List unresolved, ArtifactRepository localRepository,
                                        List resolutionRepositories )
         throws ArtifactResolutionException
     {
         for ( Iterator it = unresolved.iterator(); it.hasNext(); )
         {
             Artifact artifact = (Artifact) it.next();
 
             File artifactFile = (File) resolvedCoreArtifactFiles.get( artifact.getId() );
 
             if ( artifactFile == null )
             {
                 String resource = "/META-INF/maven/" + artifact.getGroupId() + "/" + artifact.getArtifactId() +
                     "/pom.xml";
 
                 URL resourceUrl = container.getContainerRealm().getResource( resource );
 
                 if ( resourceUrl == null )
                 {
                     artifactResolver.resolve( artifact, resolutionRepositories, localRepository );
 
                     artifactFile = artifact.getFile();
                 }
                 else
                 {
                     String artifactPath = resourceUrl.getPath();
 
                     if ( artifactPath.startsWith( "file:" ) )
                     {
                         artifactPath = artifactPath.substring( "file:".length() );
                     }
 
                     artifactPath = artifactPath.substring( 0, artifactPath.length() - resource.length() );
 
                     if ( artifactPath.endsWith( "/" ) )
                     {
                         artifactPath = artifactPath.substring( 0, artifactPath.length() - 1 );
                     }
 
                     if ( artifactPath.endsWith( "!" ) )
                     {
                         artifactPath = artifactPath.substring( 0, artifactPath.length() - 1 );
                     }
 
                     artifactFile = new File( artifactPath ).getAbsoluteFile();
                 }
 
                 resolvedCoreArtifactFiles.put( artifact.getId(), artifactFile );
             }
 
             artifact.setFile( artifactFile );
         }
     }
 
     private PlexusConfiguration extractMojoConfiguration( PlexusConfiguration mergedConfiguration,
                                                           MojoDescriptor mojoDescriptor )
     {
         Map parameterMap = mojoDescriptor.getParameterMap();
 
         PlexusConfiguration[] mergedChildren = mergedConfiguration.getChildren();
 
         XmlPlexusConfiguration extractedConfiguration = new XmlPlexusConfiguration( "configuration" );
 
         for ( int i = 0; i < mergedChildren.length; i++ )
         {
             PlexusConfiguration child = mergedChildren[i];
 
             if ( parameterMap.containsKey( child.getName() ) )
             {
                 extractedConfiguration.addChild( copyConfiguration( child ) );
             }
             else
             {
                 // TODO: I defy anyone to find these messages in the '-X' output! Do we need a new log level?
                 // ideally, this would be elevated above the true debug output, but below the default INFO level...
                 // [BP] (2004-07-18): need to understand the context more but would prefer this could be either WARN or
                 // removed - shouldn't need DEBUG to diagnose a problem most of the time.
                 getLogger().debug( "*** WARNING: Configuration \'" + child.getName() + "\' is not used in goal \'" +
                     mojoDescriptor.getFullGoalName() + "; this may indicate a typo... ***" );
             }
         }
 
         return extractedConfiguration;
     }
 
     private void checkRequiredParameters( MojoDescriptor goal, PlexusConfiguration configuration,
                                          ExpressionEvaluator expressionEvaluator, Mojo plugin )
         throws PluginConfigurationException
     {
         // TODO: this should be built in to the configurator, as we presently double process the expressions
 
         List parameters = goal.getParameters();
 
         if ( parameters == null )
         {
             return;
         }
 
         List invalidParameters = new ArrayList();
 
         for ( int i = 0; i < parameters.size(); i++ )
         {
             Parameter parameter = (Parameter) parameters.get( i );
 
             if ( parameter.isRequired() )
             {
                 // the key for the configuration map we're building.
                 String key = parameter.getName();
 
                 Object fieldValue = null;
                 String expression = null;
                 PlexusConfiguration value = configuration.getChild( key, false );
                 try
                 {
                     if ( value != null )
                     {
                         expression = value.getValue( null );
 
                         fieldValue = expressionEvaluator.evaluate( expression );
 
                         if ( fieldValue == null )
                         {
                             fieldValue = value.getAttribute( "default-value", null );
                         }
                     }
 
                     if ( fieldValue == null && StringUtils.isNotEmpty( parameter.getAlias() ) )
                     {
                         value = configuration.getChild( parameter.getAlias(), false );
                         if ( value != null )
                         {
                             expression = value.getValue( null );
                             fieldValue = expressionEvaluator.evaluate( expression );
                             if ( fieldValue == null )
                             {
                                 fieldValue = value.getAttribute( "default-value", null );
                             }
                         }
                     }
                 }
                 catch ( ExpressionEvaluationException e )
                 {
                     throw new PluginConfigurationException( goal.getPluginDescriptor(), "Bad expression", e );
                 }
 
                 // only mark as invalid if there are no child nodes
                 if ( fieldValue == null && ( value == null || value.getChildCount() == 0 ) )
                 {
                     parameter.setExpression( expression );
                     invalidParameters.add( parameter );
                 }
             }
         }
 
         if ( !invalidParameters.isEmpty() )
         {
             throw new PluginParameterException( goal, invalidParameters );
         }
     }
 
     private void validatePomConfiguration( MojoDescriptor goal, PlexusConfiguration pomConfiguration )
         throws PluginConfigurationException
     {
         List parameters = goal.getParameters();
 
         if ( parameters == null )
         {
             return;
         }
 
         for ( int i = 0; i < parameters.size(); i++ )
         {
             Parameter parameter = (Parameter) parameters.get( i );
 
             // the key for the configuration map we're building.
             String key = parameter.getName();
 
             PlexusConfiguration value = pomConfiguration.getChild( key, false );
 
             if ( value == null && StringUtils.isNotEmpty( parameter.getAlias() ) )
             {
                 key = parameter.getAlias();
                 value = pomConfiguration.getChild( key, false );
             }
 
             if ( value != null )
             {
                 // Make sure the parameter is either editable/configurable, or else is NOT specified in the POM
                 if ( !parameter.isEditable() )
                 {
                     StringBuffer errorMessage = new StringBuffer()
                         .append( "ERROR: Cannot override read-only parameter: " );
                     errorMessage.append( key );
                     errorMessage.append( " in goal: " ).append( goal.getFullGoalName() );
 
                     throw new PluginConfigurationException( goal.getPluginDescriptor(), errorMessage.toString() );
                 }
 
                 String deprecated = parameter.getDeprecated();
                 if ( StringUtils.isNotEmpty( deprecated ) )
                 {
                     getLogger().warn( "DEPRECATED [" + parameter.getName() + "]: " + deprecated );
                 }
             }
         }
     }
 
     private PlexusConfiguration mergeMojoConfiguration( XmlPlexusConfiguration fromPom, MojoDescriptor mojoDescriptor )
     {
         XmlPlexusConfiguration result = new XmlPlexusConfiguration( fromPom.getName() );
         result.setValue( fromPom.getValue( null ) );
 
         if ( mojoDescriptor.getParameters() != null )
         {
             PlexusConfiguration fromMojo = mojoDescriptor.getMojoConfiguration();
 
             for ( Iterator it = mojoDescriptor.getParameters().iterator(); it.hasNext(); )
             {
                 Parameter parameter = (Parameter) it.next();
 
                 String paramName = parameter.getName();
                 String alias = parameter.getAlias();
 
                 PlexusConfiguration pomConfig = fromPom.getChild( paramName );
                 PlexusConfiguration aliased = null;
 
                 if ( alias != null )
                 {
                     aliased = fromPom.getChild( alias );
                 }
 
                 PlexusConfiguration mojoConfig = fromMojo.getChild( paramName, false );
 
                 // first we'll merge configurations from the aliased and real params.
                 // TODO: Is this the right thing to do?
                 if ( aliased != null )
                 {
                     if ( pomConfig == null )
                     {
                         pomConfig = new XmlPlexusConfiguration( paramName );
                     }
 
                     pomConfig = buildTopDownMergedConfiguration( pomConfig, aliased );
                 }
 
                 boolean addedPomConfig = false;
 
                 if ( pomConfig != null )
                 {
                     pomConfig = buildTopDownMergedConfiguration( pomConfig, mojoConfig );
 
                     if ( StringUtils.isNotEmpty( pomConfig.getValue( null ) ) || pomConfig.getChildCount() > 0 )
                     {
                         result.addChild( pomConfig );
 
                         addedPomConfig = true;
                     }
                 }
 
                 if ( !addedPomConfig && mojoConfig != null )
                 {
                     result.addChild( copyConfiguration( mojoConfig ) );
                 }
             }
         }
         return result;
     }
 
     private XmlPlexusConfiguration buildTopDownMergedConfiguration( PlexusConfiguration dominant,
                                                                     PlexusConfiguration recessive )
     {
         XmlPlexusConfiguration result = new XmlPlexusConfiguration( dominant.getName() );
 
         String value = dominant.getValue( null );
 
         if ( StringUtils.isEmpty( value ) && recessive != null )
         {
             value = recessive.getValue( null );
         }
 
         if ( StringUtils.isNotEmpty( value ) )
         {
             result.setValue( value );
         }
 
         String[] attributeNames = dominant.getAttributeNames();
 
         for ( int i = 0; i < attributeNames.length; i++ )
         {
             String attributeValue = dominant.getAttribute( attributeNames[i], null );
 
             result.setAttribute( attributeNames[i], attributeValue );
         }
 
         if ( recessive != null )
         {
             attributeNames = recessive.getAttributeNames();
 
             for ( int i = 0; i < attributeNames.length; i++ )
             {
                 String attributeValue = recessive.getAttribute( attributeNames[i], null );
                 // TODO: recessive seems to be dominant here?
                 result.setAttribute( attributeNames[i], attributeValue );
             }
         }
 
         PlexusConfiguration[] children = dominant.getChildren();
 
         for ( int i = 0; i < children.length; i++ )
         {
             PlexusConfiguration childDom = children[i];
             PlexusConfiguration childRec = recessive == null ? null : recessive.getChild( childDom.getName(), false );
 
             if ( childRec != null )
             {
                 result.addChild( buildTopDownMergedConfiguration( childDom, childRec ) );
             }
             else
             {   // FIXME: copy, or use reference?
                 result.addChild( copyConfiguration( childDom ) );
             }
         }
 
         return result;
     }
 
     private PlexusConfiguration mergeConfiguration( PlexusConfiguration dominant, PlexusConfiguration configuration )
     {
         // TODO: share with mergeXpp3Dom
         PlexusConfiguration[] children = configuration.getChildren();
         for ( int i = 0; i < children.length; i++ )
         {
             PlexusConfiguration child = children[i];
             PlexusConfiguration childDom = dominant.getChild( child.getName(), false );
             if ( childDom != null )
             {
                 mergeConfiguration( childDom, child );
             }
             else
             {
                 dominant.addChild( copyConfiguration( child ) );
             }
         }
         return dominant;
     }
 
     public static PlexusConfiguration copyConfiguration( PlexusConfiguration src )
     {
         // TODO: shouldn't be necessary
         XmlPlexusConfiguration dom = new XmlPlexusConfiguration( src.getName() );
         dom.setValue( src.getValue( null ) );
 
         String[] attributeNames = src.getAttributeNames();
         for ( int i = 0; i < attributeNames.length; i++ )
         {
             String attributeName = attributeNames[i];
             dom.setAttribute( attributeName, src.getAttribute( attributeName, null ) );
         }
 
         PlexusConfiguration[] children = src.getChildren();
         for ( int i = 0; i < children.length; i++ )
         {
             dom.addChild( copyConfiguration( children[i] ) );
         }
 
         return dom;
     }
 
     // ----------------------------------------------------------------------
     // Mojo Parameter Handling
     // ----------------------------------------------------------------------
 
     private void populatePluginFields( Mojo plugin, MojoDescriptor mojoDescriptor, PlexusConfiguration configuration,
                                        PlexusContainer pluginContainer, ExpressionEvaluator expressionEvaluator )
         throws PluginConfigurationException
     {
         ComponentConfigurator configurator = null;
 
         try
         {
             String configuratorId = mojoDescriptor.getComponentConfigurator();
 
             // TODO: could the configuration be passed to lookup and the configurator known to plexus via the descriptor
             // so that this meethod could entirely be handled by a plexus lookup?
             if ( StringUtils.isNotEmpty( configuratorId ) )
             {
                 configurator = (ComponentConfigurator) pluginContainer.lookup( ComponentConfigurator.ROLE,
                                                                                configuratorId );
             }
             else
             {
                 configurator = (ComponentConfigurator) pluginContainer.lookup( ComponentConfigurator.ROLE );
             }
 
             configurator.configureComponent( plugin, configuration, expressionEvaluator,
                                              pluginContainer.getContainerRealm() );
         }
         catch ( ComponentConfigurationException e )
         {
             throw new PluginConfigurationException( mojoDescriptor.getPluginDescriptor(),
                                                     "Unable to parse the created DOM for plugin configuration", e );
         }
         catch ( ComponentLookupException e )
         {
             throw new PluginConfigurationException( mojoDescriptor.getPluginDescriptor(),
                                                     "Unable to retrieve component configurator for plugin configuration",
                                                     e );
         }
         finally
         {
             if ( configurator != null )
             {
                 try
                 {
                     pluginContainer.release( configurator );
                 }
                 catch ( ComponentLifecycleException e )
                 {
                     getLogger().debug( "Failed to release plugin container - ignoring." );
                 }
             }
         }
     }
 
    private Field findPluginField( Class clazz, String key )
        throws NoSuchFieldException
    {
        Field field = null;

        while ( field == null )
        {
            try
            {
                field = clazz.getDeclaredField( key );
            }
            catch ( NoSuchFieldException e )
            {
                clazz = clazz.getSuperclass();
                if ( clazz.equals( Object.class ) )
                {
                    throw e;
                }
            }
        }
        return field;
    }

     public static String createPluginParameterRequiredMessage( MojoDescriptor mojo, Parameter parameter,
                                                                String expression )
     {
         StringBuffer message = new StringBuffer();
 
         message.append( "The '" );
         message.append( parameter.getName() );
         message.append( "' parameter is required for the execution of the " );
         message.append( mojo.getFullGoalName() );
         message.append( " mojo and cannot be null." );
         if ( expression != null )
         {
             message.append( " The retrieval expression was: " ).append( expression );
         }
 
         return message.toString();
     }
 
     // ----------------------------------------------------------------------
     // Lifecycle
     // ----------------------------------------------------------------------
 
     public void contextualize( Context context )
         throws ContextException
     {
         container = (PlexusContainer) context.get( PlexusConstants.PLEXUS_KEY );
 
         mojoLogger = new DefaultLog( container.getLoggerManager().getLoggerForComponent( Mojo.ROLE ) );
     }
 
     public void initialize()
     {
         // TODO: configure this from bootstrap or scan lib
         Set artifacts = new HashSet();
         artifacts.add( "classworlds" );
         artifacts.add( "commons-cli" );
         artifacts.add( "doxia-sink-api" );
         artifacts.add( "jsch" );
         artifacts.add( "maven-artifact" );
         artifacts.add( "maven-artifact-manager" );
         artifacts.add( "maven-core" );
         artifacts.add( "maven-model" );
         artifacts.add( "maven-monitor" );
         artifacts.add( "maven-plugin-api" );
         artifacts.add( "maven-plugin-descriptor" );
         artifacts.add( "maven-plugin-parameter-documenter" );
         artifacts.add( "maven-plugin-registry" );
         artifacts.add( "maven-profile" );
         artifacts.add( "maven-project" );
         artifacts.add( "maven-reporting-api" );
         artifacts.add( "maven-repository-metadata" );
         artifacts.add( "maven-settings" );
         artifacts.add( "plexus-container-default" );
         artifacts.add( "plexus-input-handler-api" );
         artifacts.add( "plexus-utils" );
         artifacts.add( "wagon-provider-api" );
         artifacts.add( "wagon-file" );
         artifacts.add( "wagon-http-lightweight" );
         artifacts.add( "wagon-ssh" );
         artifactFilter = new ExclusionSetFilter( artifacts );
     }
 
     // ----------------------------------------------------------------------
     // Artifact resolution
     // ----------------------------------------------------------------------
 
     private void resolveTransitiveDependencies( MavenSession context, ArtifactResolver artifactResolver, String scope,
                                                 ArtifactFactory artifactFactory, MavenProject project )
         throws ArtifactResolutionException
     {
         ArtifactFilter filter = new ScopeArtifactFilter( scope );
 
         // TODO: such a call in MavenMetadataSource too - packaging not really the intention of type
         Artifact artifact = artifactFactory.createBuildArtifact( project.getGroupId(), project.getArtifactId(),
                                                                  project.getVersion(), project.getPackaging() );
 
         // TODO: we don't need to resolve over and over again, as long as we are sure that the parameters are the same
         // check this with yourkit as a hot spot.
         try
         {
             // Don't recreate if already created - for effeciency, and because clover plugin adds to it
             if ( project.getDependencyArtifacts() == null )
             {
                 project.setDependencyArtifacts( project.createArtifacts( artifactFactory, null, null ) );
             }
         }
         catch ( InvalidVersionSpecificationException e )
         {
             throw new ArtifactResolutionException( "Error in dependency version", e );
         }
         ArtifactResolutionResult result = artifactResolver.resolveTransitively( project.getDependencyArtifacts(),
                                                                                 artifact, context.getLocalRepository(),
                                                                                 project.getRemoteArtifactRepositories(),
                                                                                 artifactMetadataSource, filter );
 
         project.setArtifacts( result.getArtifacts() );
     }
 
     // ----------------------------------------------------------------------
     // Artifact downloading
     // ----------------------------------------------------------------------
 
     private void downloadDependencies( MavenProject project, MavenSession context, ArtifactResolver artifactResolver )
         throws ArtifactResolutionException
     {
         ArtifactRepository localRepository = context.getLocalRepository();
         List remoteArtifactRepositories = project.getRemoteArtifactRepositories();
 
         for ( Iterator it = project.getArtifacts().iterator(); it.hasNext(); )
         {
             Artifact artifact = (Artifact) it.next();
 
             artifactResolver.resolve( artifact, remoteArtifactRepositories, localRepository );
         }
     }
 
     public Object getPluginComponent( Plugin plugin, String role, String roleHint )
         throws ComponentLookupException, PluginManagerException
     {
         PluginDescriptor pluginDescriptor = pluginCollector.getPluginDescriptor( plugin );
 
         PlexusContainer pluginContainer = getPluginContainer( pluginDescriptor );
 
         return pluginContainer.lookup( role, roleHint );
     }
 
     public Map getPluginComponents( Plugin plugin, String role )
         throws ComponentLookupException, PluginManagerException
     {
         PluginDescriptor pluginDescriptor = pluginCollector.getPluginDescriptor( plugin );
 
         PlexusContainer pluginContainer = getPluginContainer( pluginDescriptor );
 
         return pluginContainer.lookupMap( role );
     }
 
 }
