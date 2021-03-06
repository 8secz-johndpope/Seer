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
 import org.apache.maven.artifact.resolver.ArtifactResolutionException;
 import org.apache.maven.artifact.resolver.ArtifactResolutionResult;
 import org.apache.maven.artifact.resolver.ArtifactResolver;
 import org.apache.maven.artifact.resolver.filter.ArtifactFilter;
 import org.apache.maven.artifact.resolver.filter.ExclusionSetFilter;
 import org.apache.maven.artifact.resolver.filter.ScopeArtifactFilter;
 import org.apache.maven.artifact.transform.ReleaseArtifactTransformation;
 import org.apache.maven.execution.MavenSession;
 import org.apache.maven.monitor.event.EventDispatcher;
 import org.apache.maven.monitor.event.MavenEvents;
 import org.apache.maven.monitor.logging.DefaultLog;
 import org.apache.maven.plugin.descriptor.MojoDescriptor;
 import org.apache.maven.plugin.descriptor.Parameter;
 import org.apache.maven.plugin.descriptor.PluginDescriptor;
 import org.apache.maven.plugin.descriptor.PluginDescriptorBuilder;
 import org.apache.maven.plugin.logging.Log;
 import org.apache.maven.project.MavenProject;
 import org.apache.maven.project.MavenProjectBuilder;
 import org.apache.maven.project.artifact.MavenMetadataSource;
 import org.apache.maven.project.path.PathTranslator;
 import org.codehaus.plexus.ArtifactEnabledContainer;
 import org.codehaus.plexus.ArtifactEnabledContainerException;
 import org.codehaus.plexus.PlexusConstants;
 import org.codehaus.plexus.PlexusContainer;
 import org.codehaus.plexus.component.configurator.ComponentConfigurationException;
 import org.codehaus.plexus.component.configurator.ComponentConfigurator;
 import org.codehaus.plexus.component.configurator.expression.ExpressionEvaluationException;
 import org.codehaus.plexus.component.configurator.expression.ExpressionEvaluator;
 import org.codehaus.plexus.component.discovery.ComponentDiscoveryEvent;
 import org.codehaus.plexus.component.discovery.ComponentDiscoveryListener;
 import org.codehaus.plexus.component.repository.ComponentSetDescriptor;
 import org.codehaus.plexus.component.repository.exception.ComponentLifecycleException;
 import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
 import org.codehaus.plexus.configuration.PlexusConfiguration;
 import org.codehaus.plexus.configuration.xml.XmlPlexusConfiguration;
 import org.codehaus.plexus.context.Context;
 import org.codehaus.plexus.context.ContextException;
 import org.codehaus.plexus.logging.AbstractLogEnabled;
 import org.codehaus.plexus.logging.LoggerManager;
 import org.codehaus.plexus.personality.plexus.lifecycle.phase.Contextualizable;
 import org.codehaus.plexus.personality.plexus.lifecycle.phase.Initializable;
 import org.codehaus.plexus.util.StringUtils;
 import org.codehaus.plexus.util.xml.Xpp3Dom;
 
 import java.lang.reflect.Field;
 import java.util.ArrayList;
 import java.util.HashMap;
 import java.util.HashSet;
 import java.util.Iterator;
 import java.util.List;
 import java.util.Map;
 import java.util.Set;
 
 public class DefaultPluginManager
     extends AbstractLogEnabled
     implements PluginManager, ComponentDiscoveryListener, Initializable, Contextualizable
 {
     protected Map pluginDescriptors;
 
     protected Map pluginDescriptorsByPrefix;
 
     protected PlexusContainer container;
 
     protected PluginDescriptorBuilder pluginDescriptorBuilder;
 
     protected ArtifactFilter artifactFilter;
 
     protected PathTranslator pathTranslator;
 
     private ArtifactFactory artifactFactory;
 
     private Set pluginsInProcess = new HashSet();
 
     private Log mojoLogger;
 
     public DefaultPluginManager()
     {
         pluginDescriptors = new HashMap();
 
         pluginDescriptorsByPrefix = new HashMap();
 
         pluginDescriptorBuilder = new PluginDescriptorBuilder();
     }
 
     // ----------------------------------------------------------------------
     // Mojo discovery
     // ----------------------------------------------------------------------
 
     public void componentDiscovered( ComponentDiscoveryEvent event )
     {
         ComponentSetDescriptor componentSetDescriptor = event.getComponentSetDescriptor();
 
         if ( componentSetDescriptor instanceof PluginDescriptor )
         {
             PluginDescriptor pluginDescriptor = (PluginDescriptor) componentSetDescriptor;
 
             if ( pluginDescriptor.getVersion() == null )
             {
                 // TODO: temporary - until we're done testing that version is always written
                 throw new NullPointerException( "Version was null - check your plugin '" + pluginDescriptor.getId() +
                                                 "' was built with Maven 2.0 Alpha 2" );
             }
 
 //            String key = pluginDescriptor.getId();
             // TODO: see comment in getPluginDescriptor
             String key = pluginDescriptor.getGroupId() + ":" + pluginDescriptor.getArtifactId();
 
             if ( !pluginsInProcess.contains( key ) )
             {
                 pluginsInProcess.add( key );
 
                 pluginDescriptors.put( key, pluginDescriptor );
 
                 // TODO: throw an (not runtime) exception if there is a prefix overlap - means doing so elsewhere
                 if ( !pluginDescriptorsByPrefix.containsKey( pluginDescriptor.getGoalPrefix() ) )
                 {
                     pluginDescriptorsByPrefix.put( pluginDescriptor.getGoalPrefix(), pluginDescriptor );
                 }
             }
         }
     }
 
     // ----------------------------------------------------------------------
     //
     // ----------------------------------------------------------------------
 
     private PluginDescriptor getPluginDescriptor( String groupId, String artifactId, String version )
     {
 //        String key = PluginDescriptor.constructPluginKey( groupId, artifactId, version );
         // TODO: include version, but can't do this in the plugin manager as it is not resolved to the right version
         // at that point. Instead, move the duplication check to the artifact container, or store it locally based on
         // the unresolved version?
         String key = groupId + ":" + artifactId;
         return (PluginDescriptor) pluginDescriptors.get( key );
     }
 
     private PluginDescriptor getPluginDescriptor( String prefix )
     {
         return (PluginDescriptor) pluginDescriptorsByPrefix.get( prefix );
     }
 
     private boolean isPluginInstalled( String groupId, String artifactId, String version )
     {
 //        String key = PluginDescriptor.constructPluginKey( groupId, artifactId, version );
         // TODO: see comment in getPluginDescriptor
         String key = groupId + ":" + artifactId;
         return pluginDescriptors.containsKey( key );
     }
 
     private boolean isPluginInstalled( String prefix )
     {
         return pluginDescriptorsByPrefix.containsKey( prefix );
     }
 
     public PluginDescriptor verifyPlugin( String prefix )
     {
         if ( !isPluginInstalled( prefix ) )
         {
             // TODO: lookup remotely
         }
         return getPluginDescriptor( prefix );
     }
 
     public PluginDescriptor verifyPlugin( String groupId, String artifactId, String version, MavenSession session )
         throws ArtifactResolutionException, PluginManagerException
     {
         // TODO: this should be possibly outside
         if ( version == null )
         {
             MavenProject project = session.getProject();
 
             org.apache.maven.model.Plugin pluginConfig = null;
 
             for ( Iterator it = project.getBuildPlugins().iterator(); it.hasNext(); )
             {
                 org.apache.maven.model.Plugin plugin = (org.apache.maven.model.Plugin) it.next();
 
                 if ( groupId.equals( plugin.getGroupId() ) && artifactId.equals( plugin.getArtifactId() ) )
                 {
                     pluginConfig = plugin;
 
                     break;
                 }
             }
 
             if ( project.getReports() != null )
             {
                 for ( Iterator it = project.getReports().getPlugins().iterator(); it.hasNext(); )
                 {
                     org.apache.maven.model.Plugin plugin = (org.apache.maven.model.Plugin) it.next();
 
                     if ( groupId.equals( plugin.getGroupId() ) && artifactId.equals( plugin.getArtifactId() ) )
                     {
                         pluginConfig = plugin;
 
                         break;
                     }
                 }
             }
 
             if ( pluginConfig != null )
             {
                 if ( StringUtils.isEmpty( pluginConfig.getVersion() ) )
                 {
                     version = ReleaseArtifactTransformation.RELEASE_VERSION;
                 }
                 else
                 {
                     version = pluginConfig.getVersion();
                 }
             }
         }
 
         // TODO: this might result in an artifact "RELEASE" being resolved continuously
         if ( !isPluginInstalled( groupId, artifactId, version ) )
         {
             try
             {
                 Artifact pluginArtifact = artifactFactory.createArtifact( groupId, artifactId, version, null,
                                                                           MojoDescriptor.MAVEN_PLUGIN, null );
                 addPlugin( pluginArtifact, session );
 
                 version = pluginArtifact.getBaseVersion();
             }
             catch ( ArtifactEnabledContainerException e )
             {
                 throw new PluginManagerException( "Error occurred in the artifact container attempting to download plugin " +
                                                   groupId + ":" + artifactId, e );
             }
             catch ( ArtifactResolutionException e )
             {
                 if ( groupId.equals( e.getGroupId() ) && artifactId.equals( e.getArtifactId() ) &&
                     version.equals( e.getVersion() ) && "maven-plugin".equals( e.getType() ) )
                 {
                     throw new PluginNotFoundException( e );
                 }
                 else
                 {
                     throw e;
                 }
             }
             catch ( ComponentLookupException e )
             {
                 throw new PluginManagerException( "Internal configuration error while retrieving " + groupId + ":" +
                                                   artifactId, e );
             }
         }
         return getPluginDescriptor( groupId, artifactId, version );
     }
 
     protected void addPlugin( Artifact pluginArtifact, MavenSession session )
         throws ArtifactEnabledContainerException, ArtifactResolutionException, ComponentLookupException
     {
         ArtifactResolver artifactResolver = null;
         MavenProjectBuilder mavenProjectBuilder = null;
 
         try
         {
             artifactResolver = (ArtifactResolver) container.lookup( ArtifactResolver.ROLE );
 
             mavenProjectBuilder = (MavenProjectBuilder) container.lookup( MavenProjectBuilder.ROLE );
 
             MavenMetadataSource metadataSource = new MavenMetadataSource( artifactResolver, mavenProjectBuilder );
 
             ( (ArtifactEnabledContainer) container ).addComponent( pluginArtifact, artifactResolver,
                                                                    session.getPluginRepositories(),
                                                                    session.getLocalRepository(), metadataSource,
                                                                    artifactFilter );
         }
         finally
         {
             if ( artifactResolver != null )
             {
                 releaseComponent( artifactResolver );
             }
             if ( mavenProjectBuilder != null )
             {
                 releaseComponent( mavenProjectBuilder );
             }
         }
     }
 
     private void releaseComponent( Object component )
     {
         try
         {
             container.release( component );
         }
         catch ( ComponentLifecycleException e )
         {
             getLogger().error( "Error releasing component - ignoring", e );
         }
     }
 
     // ----------------------------------------------------------------------
     // Mojo execution
     // ----------------------------------------------------------------------
 
     public void executeMojo( MavenSession session, MojoDescriptor mojoDescriptor )
         throws ArtifactResolutionException, PluginManagerException, MojoExecutionException
     {
         if ( mojoDescriptor.isDependencyResolutionRequired() != null )
         {
 
             ArtifactResolver artifactResolver = null;
             MavenProjectBuilder mavenProjectBuilder = null;
 
             try
             {
                 artifactResolver = (ArtifactResolver) container.lookup( ArtifactResolver.ROLE );
                 mavenProjectBuilder = (MavenProjectBuilder) container.lookup( MavenProjectBuilder.ROLE );
 
                 resolveTransitiveDependencies( session, artifactResolver, mavenProjectBuilder,
                                                mojoDescriptor.isDependencyResolutionRequired() );
                 downloadDependencies( session, artifactResolver );
             }
             catch ( ComponentLookupException e )
             {
                 throw new PluginManagerException( "Internal configuration error in plugin manager", e );
             }
             finally
             {
                 if ( artifactResolver != null )
                 {
                     releaseComponent( artifactResolver );
                 }
                 if ( mavenProjectBuilder != null )
                 {
                     releaseComponent( mavenProjectBuilder );
                 }
             }
         }
 
         Mojo plugin = null;
 
         String goalName = mojoDescriptor.getFullGoalName();
 
         try
         {
             plugin = (Mojo) container.lookup( Mojo.ROLE, mojoDescriptor.getRoleHint() );
             plugin.setLog( mojoLogger );
 
             String goalId = mojoDescriptor.getGoal();
 
             // TODO: can probable refactor these a little when only the new plugin technique is in place
             PluginDescriptor pluginDescriptor = mojoDescriptor.getPluginDescriptor();
             Xpp3Dom dom = session.getProject().getGoalConfiguration( pluginDescriptor.getGroupId(),
                                                                      pluginDescriptor.getArtifactId(), goalId );
 
             PlexusConfiguration pomConfiguration;
             if ( dom == null )
             {
                 pomConfiguration = new XmlPlexusConfiguration( "configuration" );
             }
             else
             {
                 pomConfiguration = new XmlPlexusConfiguration( dom );
 
                 // Validate against non-editable (@readonly) parameters, to make sure users aren't trying to 
                 // override in the POM.
                 // TODO: currently breaks it0014, as an expression is in pomConfiguration
 //                validatePomConfiguration( mojoDescriptor, pomConfiguration );
             }
 
             ExpressionEvaluator expressionEvaluator = new PluginParameterExpressionEvaluator( session, pathTranslator,
                                                                                               getLogger() );
 
             PlexusConfiguration mergedConfiguration = mergeConfiguration( pomConfiguration,
                                                                           mojoDescriptor.getMojoConfiguration() );
 
             // TODO: Go back to this when we get the container ready to configure mojos...
 //            PlexusConfiguration mergedConfiguration = mergeConfiguration( pomConfiguration,
 //                                                                          mojoDescriptor.getConfiguration() );
 
             try
             {
                 getPluginConfigurationFromExpressions( plugin, mojoDescriptor, mergedConfiguration,
                                                        expressionEvaluator );
 
                 populatePluginFields( plugin, mojoDescriptor, mergedConfiguration, expressionEvaluator );
             }
             catch ( ExpressionEvaluationException e )
             {
                 throw new MojoExecutionException( "Unable to configure plugin", e );
             }
 
             // !! This is ripe for refactoring to an aspect.
             // Event monitoring.
             String event = MavenEvents.MOJO_EXECUTION;
             EventDispatcher dispatcher = session.getEventDispatcher();
 
             dispatcher.dispatchStart( event, goalName );
             try
             {
                 plugin.execute();
 
                 dispatcher.dispatchEnd( event, goalName );
             }
             catch ( MojoExecutionException e )
             {
                 session.getEventDispatcher().dispatchError( event, goalName, e );
                 throw e;
             }
             // End event monitoring.
 
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
         finally
         {
             releaseComponent( plugin );
         }
     }
 
     private void validatePomConfiguration( MojoDescriptor goal, PlexusConfiguration pomConfiguration )
         throws PluginConfigurationException
     {
         List parameters = goal.getParameters();
 
         for ( int i = 0; i < parameters.size(); i++ )
         {
             Parameter parameter = (Parameter) parameters.get( i );
 
             boolean editable = parameter.isEditable();
 
             // the key for the configuration map we're building.
             String key = parameter.getName();
 
             // the key used to lookup the parameter in the config from the POM, etc.
             String lookupKey = parameter.getAlias();
 
             if ( StringUtils.isEmpty( lookupKey ) )
             {
                 lookupKey = key;
             }
 
             // Make sure the parameter is either editable/configurable, or else is NOT specified in the POM 
             if ( !editable && ( pomConfiguration.getChild( lookupKey, false ) != null ||
                 pomConfiguration.getChild( key, false ) != null ) )
             {
                 StringBuffer errorMessage = new StringBuffer().append( "ERROR: Cannot override read-only parameter: " ).append(
                     key );
 
                 if ( !lookupKey.equals( key ) )
                 {
                     errorMessage.append( " (with alias: " ).append( lookupKey ).append( ")" );
                 }
 
                 errorMessage.append( " in goal: " ).append( goal.getFullGoalName() );
 
                 throw new PluginConfigurationException( errorMessage.toString() );
             }
         }
     }
 
     private PlexusConfiguration mergeConfiguration( PlexusConfiguration dominant, PlexusConfiguration configuration )
     {
         // TODO: share with mergeXpp3Dom
         PlexusConfiguration[] children = configuration.getChildren();
         for ( int i = 0; i < children.length; i++ )
         {
             PlexusConfiguration child = children[i];
             PlexusConfiguration childDom = (XmlPlexusConfiguration) dominant.getChild( child.getName(), false );
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
                                        ExpressionEvaluator expressionEvaluator )
         throws PluginConfigurationException
     {
         ComponentConfigurator configurator = null;
 
         try
         {
             String configuratorId = mojoDescriptor.getComponentConfigurator();
 
             // TODO: should this be known to the component factory instead? And if so, should configuration be part of lookup?
             if ( StringUtils.isNotEmpty( configuratorId ) )
             {
                 configurator = (ComponentConfigurator) container.lookup( ComponentConfigurator.ROLE, configuratorId );
             }
             else
             {
                 configurator = (ComponentConfigurator) container.lookup( ComponentConfigurator.ROLE );
             }
 
             configurator.configureComponent( plugin, configuration, expressionEvaluator );
 
         }
         catch ( ComponentConfigurationException e )
         {
             throw new PluginConfigurationException( "Unable to parse the created DOM for plugin configuration", e );
         }
         catch ( ComponentLookupException e )
         {
             throw new PluginConfigurationException(
                 "Unable to retrieve component configurator for plugin configuration", e );
         }
         finally
         {
             if ( configurator != null )
             {
                 try
                 {
                     container.release( configurator );
                 }
                 catch ( ComponentLifecycleException e )
                 {
                 }
             }
         }
     }
 
     private Field findPluginField( Class clazz, String key )
         throws NoSuchFieldException
     {
         try
         {
             return clazz.getDeclaredField( key );
         }
         catch ( NoSuchFieldException e )
         {
             Class superclass = clazz.getSuperclass();
             if ( superclass != Object.class )
             {
                 return findPluginField( superclass, key );
             }
             else
             {
                 throw e;
             }
         }
     }
 
     /**
      * @deprecated [JC] in favor of what?
      */
     private void getPluginConfigurationFromExpressions( Mojo plugin, MojoDescriptor goal,
                                                         PlexusConfiguration mergedConfiguration,
                                                         ExpressionEvaluator expressionEvaluator )
         throws ExpressionEvaluationException, PluginConfigurationException
     {
         List parameters = goal.getParameters();
 
         List invalidParameters = new ArrayList();
 
         if ( parameters == null || parameters.isEmpty() )
         {
             return;
         }
 
         for ( int i = 0; i < parameters.size(); i++ )
         {
             Parameter parameter = (Parameter) parameters.get( i );
 
             boolean editable = parameter.isEditable();
 
             // the key for the configuration map we're building.
             String key = parameter.getName();
 
             // the key used to lookup the parameter in the config from the POM, etc.
             String lookupKey = parameter.getAlias();
 
             if ( StringUtils.isEmpty( lookupKey ) )
             {
                 lookupKey = key;
             }
 
             String expression;
 
             boolean foundInConfiguration = false;
 
             if ( mergedConfiguration.getChild( lookupKey, false ) != null )
             {
                 expression = mergedConfiguration.getChild( lookupKey, false ).getValue( null );
                 foundInConfiguration = true;
             }
             else if ( mergedConfiguration.getChild( key, false ) != null )
             {
                 expression = mergedConfiguration.getChild( key, false ).getValue( null );
                 foundInConfiguration = true;
             }
             else
             {
                 expression = parameter.getExpression();
             }
 
             if ( foundInConfiguration && expression != null && parameter.getDeprecated() != null )
             {
                 PlexusConfiguration goalConfiguration = goal.getMojoConfiguration();
                 
                 // TODO: Go back to this when we get the container ready to configure mojos...
 //                PlexusConfiguration goalConfiguration = goal.getConfiguration();
 
                 if ( !expression.equals( goalConfiguration.getChild( lookupKey, false ).getValue( null ) ) &&
                     !expression.equals( goalConfiguration.getChild( key, false ).getValue( null ) ) )
                 {
                     StringBuffer message = new StringBuffer().append( "DEPRECATED: " ).append( key );
 
                     if ( !lookupKey.equals( key ) )
                     {
                         message.append( " (aliased to " ).append( lookupKey ).append( ")" );
                     }
 
                     message.append( " is deprecated.\n\t" ).append( parameter.getDeprecated() );
 
                     getLogger().warn( message.toString() );
                 }
             }
 
             Object value = expressionEvaluator.evaluate( expression );
 
             getLogger().debug( "Evaluated mojo parameter expression: \'" + expression + "\' to: " + value +
                                " for parameter: \'" + key + "\'" );
 
             // TODO: remove. If there is a default value, required should have been removed by the descriptor generator
             if ( value == null && goal.getComponentConfigurator() == null )
             {
                 Object defaultValue;
                 try
                 {
                     Field pluginField = findPluginField( plugin.getClass(), parameter.getName() );
                     boolean accessible = pluginField.isAccessible();
                     if ( !accessible )
                     {
                         pluginField.setAccessible( true );
                     }
                     defaultValue = pluginField.get( plugin );
                     if ( !accessible )
                     {
                         pluginField.setAccessible( false );
                     }
                 }
                 catch ( IllegalAccessException e )
                 {
                     String message = "Error finding field for parameter '" + parameter.getName() + "'";
                     throw new PluginConfigurationException( message, e );
                 }
                 catch ( NoSuchFieldException e )
                 {
                     String message = "Error finding field for parameter '" + parameter.getName() + "'";
                     throw new PluginConfigurationException( message, e );
                 }
                 if ( defaultValue != null )
                 {
                     // TODO: allow expressions?
                     value = defaultValue;
                 }
             }
 
             // ----------------------------------------------------------------------
             // We will perform a basic check here for parameters values that are
             // required. Required parameters can't be null so we throw an
             // Exception in the case where they are. We probably want some
             // pluggable
             // mechanism here but this will catch the most obvious of
             // misconfigurations.
             // ----------------------------------------------------------------------
 
             if ( value == null && parameter.isRequired() )
             {
                 invalidParameters.add( parameter );
             }
 
         }
 
         if ( !invalidParameters.isEmpty() )
         {
             throw new PluginParameterException( goal, invalidParameters );
         }
     }
 
     public static String createPluginParameterRequiredMessage( MojoDescriptor mojo, Parameter parameter,
                                                                String expression )
     {
         StringBuffer message = new StringBuffer();
 
         message.append( "The '" + parameter.getName() );
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
 
         LoggerManager manager = null;
 
         try
         {
             manager = (LoggerManager) container.lookup( LoggerManager.ROLE );
 
             mojoLogger = new DefaultLog( manager.getLoggerForComponent( Mojo.ROLE ) );
         }
         catch ( ComponentLookupException e )
         {
             throw new ContextException( "Error locating a logger manager", e );
         }
         finally
         {
             if ( manager != null )
             {
                 try
                 {
                     container.release( manager );
                 }
                 catch ( ComponentLifecycleException e )
                 {
                     getLogger().error( "Error releasing the logger manager - ignoring", e );
                 }
             }
         }
     }
 
     public void initialize()
     {
         // TODO: configure this from bootstrap or scan lib
         // TODO: Note: maven-plugin just re-added until all plugins are switched over...
         artifactFilter = new ExclusionSetFilter( new String[]{"maven-core", "maven-artifact", "maven-model",
                                                               "maven-settings", "maven-monitor", "maven-plugin-api",
                                                               "maven-plugin-descriptor", "plexus-container-default",
                                                               "maven-project", "plexus-container-artifact",
                                                               "maven-reporting-api", "doxia-core",
                                                               "wagon-provider-api", "classworlds", "maven-plugin",
                                                               "plexus-marmalade-factory", "maven-script-marmalade",
                                                               "maven-script-beanshell", "plexus-bsh-factory", "bsh",
                                                              "marmalade-core"} );
     }
 
     // ----------------------------------------------------------------------
     // Artifact resolution
     // ----------------------------------------------------------------------
 
     private void resolveTransitiveDependencies( MavenSession context, ArtifactResolver artifactResolver,
                                                 MavenProjectBuilder mavenProjectBuilder, String scope )
         throws ArtifactResolutionException
     {
         MavenProject project = context.getProject();
 
         MavenMetadataSource sourceReader = new MavenMetadataSource( artifactResolver, mavenProjectBuilder );
 
         ArtifactFilter filter = new ScopeArtifactFilter( scope );
 
         boolean systemOnline = !context.getSettings().getActiveProfile().isOffline();
 
         ArtifactResolutionResult result = artifactResolver.resolveTransitively( project.getArtifacts(),
                                                                                 context.getRemoteRepositories(),
                                                                                 context.getLocalRepository(),
                                                                                 sourceReader, filter );
 
         project.addArtifacts( result.getArtifacts().values(), artifactFactory );
     }
 
     // ----------------------------------------------------------------------
     // Artifact downloading
     // ----------------------------------------------------------------------
 
     private void downloadDependencies( MavenSession context, ArtifactResolver artifactResolver )
         throws ArtifactResolutionException
     {
         for ( Iterator it = context.getProject().getArtifacts().iterator(); it.hasNext(); )
         {
             Artifact artifact = (Artifact) it.next();
 
             // TODO: should I get the modified artifacts back into the project?
             artifactResolver.resolve( artifact, context.getRemoteRepositories(), context.getLocalRepository() );
         }
 
         Set pluginArtifacts = new HashSet();
         for ( Iterator it = context.getProject().getPluginArtifacts().iterator(); it.hasNext(); )
         {
             Artifact artifact = (Artifact) it.next();
 
             artifactResolver.resolve( artifact, context.getRemoteRepositories(), context.getLocalRepository() );
 
             pluginArtifacts.add( artifact );
         }
         context.getProject().setPluginArtifacts( pluginArtifacts );
 
         artifactResolver.resolve( context.getProject().getParentArtifact(), context.getRemoteRepositories(), context.getLocalRepository() );
     }
 
 }
