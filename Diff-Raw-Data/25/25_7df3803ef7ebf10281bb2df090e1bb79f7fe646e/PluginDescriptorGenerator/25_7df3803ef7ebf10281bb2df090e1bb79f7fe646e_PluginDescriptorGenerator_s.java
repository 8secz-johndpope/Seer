 package org.apache.maven.tools.plugin.generator;
 
 /*
  * Copyright 2001-2004 The Apache Software Foundation.
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
 
 import org.apache.maven.plugin.descriptor.MojoDescriptor;
 import org.apache.maven.plugin.descriptor.Parameter;
 import org.apache.maven.plugin.descriptor.PluginDescriptor;
 import org.apache.maven.plugin.descriptor.Requirement;
 import org.apache.maven.tools.plugin.util.PluginUtils;
 import org.codehaus.plexus.util.IOUtil;
 import org.codehaus.plexus.util.StringUtils;
 import org.codehaus.plexus.util.xml.PrettyPrintXMLWriter;
 import org.codehaus.plexus.util.xml.XMLWriter;
 
 import java.io.File;
 import java.io.FileWriter;
 import java.io.IOException;
 import java.util.HashMap;
 import java.util.HashSet;
 import java.util.Iterator;
 import java.util.List;
 import java.util.Map;
 import java.util.Set;
 
 /**
  * @todo add example usage tag that can be shown in the doco
  * @todo need to add validation directives so that systems embedding maven2 can
  * get validation directives to help users in IDEs.
  */
 public class PluginDescriptorGenerator
     implements Generator
 {
     public void execute( File destinationDirectory, PluginDescriptor pluginDescriptor )
         throws IOException
     {
         File f = new File( destinationDirectory, "plugin.xml" );
 
         if ( !f.getParentFile().exists() )
         {
             f.getParentFile().mkdirs();
         }
 
         FileWriter writer = null;
         try
         {
             writer = new FileWriter( f );
 
             XMLWriter w = new PrettyPrintXMLWriter( writer );
 
             w.startElement( "plugin" );
 
             element( w, "groupId", pluginDescriptor.getGroupId() );
 
             element( w, "artifactId", pluginDescriptor.getArtifactId() );
 
             element( w, "version", pluginDescriptor.getVersion() );
 
             element( w, "goalPrefix", pluginDescriptor.getGoalPrefix() );
 
             element( w, "isolatedRealm", "" + pluginDescriptor.isIsolatedRealm() );
 
             element( w, "inheritedByDefault", "" + pluginDescriptor.isInheritedByDefault() );
 
             w.startElement( "mojos" );
 
             if ( pluginDescriptor.getMojos() != null )
             {
                 for ( Iterator it = pluginDescriptor.getMojos().iterator(); it.hasNext(); )
                 {
                     MojoDescriptor descriptor = (MojoDescriptor) it.next();
                     processMojoDescriptor( descriptor, w );
                 }
             }
 
             w.endElement();
 
             PluginUtils.writeDependencies( w, pluginDescriptor );
 
             w.endElement();
 
             writer.flush();
         }
         finally
         {
             IOUtil.close( writer );
         }
     }
 
     protected void processMojoDescriptor( MojoDescriptor mojoDescriptor, XMLWriter w )
     {
         w.startElement( "mojo" );
 
         // ----------------------------------------------------------------------
         //
         // ----------------------------------------------------------------------
 
         w.startElement( "goal" );
 
         w.writeText( mojoDescriptor.getGoal() );
 
         w.endElement();
 
         // ----------------------------------------------------------------------
         //
         // ----------------------------------------------------------------------
 
         if ( mojoDescriptor.isDependencyResolutionRequired() != null )
         {
             element( w, "requiresDependencyResolution", mojoDescriptor.isDependencyResolutionRequired() );
         }
 
         // ----------------------------------------------------------------------
         //
         // ----------------------------------------------------------------------
 
         element( w, "requiresDirectInvocation", "" + mojoDescriptor.isDirectInvocationOnly() );
 
         // ----------------------------------------------------------------------
         //
         // ----------------------------------------------------------------------
 
         element( w, "requiresProject", "" + mojoDescriptor.isProjectRequired() );
 
         // ----------------------------------------------------------------------
         //
         // ----------------------------------------------------------------------
 
         element( w, "requiresReports", "" + mojoDescriptor.isRequiresReports() );
 
         // ----------------------------------------------------------------------
         //
         // ----------------------------------------------------------------------
 
         element( w, "aggregator", "" + mojoDescriptor.isAggregator() );
 
         // ----------------------------------------------------------------------
         //
         // ----------------------------------------------------------------------
 
         element( w, "requiresOnline", "" + mojoDescriptor.isOnlineRequired() );
 
         // ----------------------------------------------------------------------
         //
         // ----------------------------------------------------------------------
 
         element( w, "inheritedByDefault", "" + mojoDescriptor.isInheritedByDefault() );
 
         // ----------------------------------------------------------------------
         //
         // ----------------------------------------------------------------------
 
         if ( mojoDescriptor.getPhase() != null )
         {
             element( w, "phase", mojoDescriptor.getPhase() );
         }
 
         // ----------------------------------------------------------------------
         //
         // ----------------------------------------------------------------------
 
         if ( mojoDescriptor.getExecutePhase() != null )
         {
             element( w, "executePhase", mojoDescriptor.getExecutePhase() );
         }
 
         if ( mojoDescriptor.getExecuteGoal() != null )
         {
             element( w, "executeGoal", mojoDescriptor.getExecuteGoal() );
         }
 
         if ( mojoDescriptor.getExecuteLifecycle() != null )
         {
             element( w, "executeLifecycle", mojoDescriptor.getExecuteLifecycle() );
         }
 
         // ----------------------------------------------------------------------
         //
         // ----------------------------------------------------------------------
 
         w.startElement( "implementation" );
 
         w.writeText( mojoDescriptor.getImplementation() );
 
         w.endElement();
 
         // ----------------------------------------------------------------------
         //
         // ----------------------------------------------------------------------
 
         w.startElement( "language" );
 
         w.writeText( mojoDescriptor.getLanguage() );
 
         w.endElement();
 
         // ----------------------------------------------------------------------
         //
         // ----------------------------------------------------------------------
 
         if ( mojoDescriptor.getComponentConfigurator() != null )
         {
             w.startElement( "configurator" );
 
             w.writeText( mojoDescriptor.getComponentConfigurator() );
 
             w.endElement();
         }
 
         // ----------------------------------------------------------------------
         //
         // ----------------------------------------------------------------------
 
         if ( mojoDescriptor.getComponentComposer() != null )
         {
             w.startElement( "composer" );
 
             w.writeText( mojoDescriptor.getComponentComposer() );
 
             w.endElement();
         }
 
         // ----------------------------------------------------------------------
         //
         // ----------------------------------------------------------------------
 
         w.startElement( "instantiationStrategy" );
 
         w.writeText( mojoDescriptor.getInstantiationStrategy() );
 
         w.endElement();
 
         // ----------------------------------------------------------------------
         // Strategy for handling repeated reference to mojo in
         // the calculated (decorated, resolved) execution stack
         // ----------------------------------------------------------------------
         w.startElement( "executionStrategy" );
 
         w.writeText( mojoDescriptor.getExecutionStrategy() );
 
         w.endElement();
 
         // ----------------------------------------------------------------------
         // Parameters
         // ----------------------------------------------------------------------
 
         List parameters = mojoDescriptor.getParameters();
 
         w.startElement( "parameters" );
 
         Map requirements = new HashMap();
 
         Set configuration = new HashSet();
 
         if ( parameters != null )
         {
             for ( int j = 0; j < parameters.size(); j++ )
             {
                 Parameter parameter = (Parameter) parameters.get( j );
 
                 String expression = parameter.getExpression();
 
                 if ( StringUtils.isNotEmpty( expression ) && expression.startsWith( "${component." ) )
                 {
                     // treat it as a component...a requirement, in other words.
 
                     // remove "component." plus expression delimiters
                     String role = expression.substring( "${component.".length(), expression.length() - 1 );
 
                     String roleHint = null;
 
                     int posRoleHintSeparator;
 
                     if ( ( posRoleHintSeparator = role.indexOf( "#" ) ) > 0 )
                     {
                         roleHint = role.substring( posRoleHintSeparator + 1 );
 
                         role = role.substring( 0, posRoleHintSeparator );
                     }
 
                     // TODO: remove deprecated expression
                     requirements.put( parameter.getName(), new Requirement( role, roleHint ) );
                 }
                 else if ( parameter.getRequirement() != null )
                 {
                     requirements.put( parameter.getName(), parameter.getRequirement() );
                 }
                 else
                 {
                     // treat it as a normal parameter.
 
                     w.startElement( "parameter" );
 
                     element( w, "name", parameter.getName() );
 
                     if ( parameter.getAlias() != null )
                     {
                         element( w, "alias", parameter.getAlias() );
                     }
 
                     element( w, "type", parameter.getType() );
 
                     if ( parameter.getDeprecated() != null )
                     {
                         element( w, "deprecated", parameter.getDeprecated() );
                     }
 
                     element( w, "required", Boolean.toString( parameter.isRequired() ) );
 
                     element( w, "editable", Boolean.toString( parameter.isEditable() ) );
 
                     element( w, "description", parameter.getDescription() );
 
                     if ( StringUtils.isNotEmpty( parameter.getDefaultValue() ) ||
                         StringUtils.isNotEmpty( parameter.getExpression() ) )
                     {
                         configuration.add( parameter );
                     }
 
                     w.endElement();
                 }
 
             }
         }
 
         w.endElement();
 
         // ----------------------------------------------------------------------
         // Coinfiguration
         // ----------------------------------------------------------------------
 
         if ( !configuration.isEmpty() )
         {
             w.startElement( "configuration" );
 
             for ( Iterator i = configuration.iterator(); i.hasNext(); )
             {
                 Parameter parameter = (Parameter) i.next();
 
                 w.startElement( parameter.getName() );
 
                 String type = parameter.getType();
                 if ( type != null )
                 {
                     w.addAttribute( "implementation", type );
                 }
 
                 if ( parameter.getDefaultValue() != null )
                 {
                     w.addAttribute( "default-value", parameter.getDefaultValue() );
                 }
 
                 if ( parameter.getExpression() != null )
                 {
                     w.writeText( parameter.getExpression() );
                 }
 
                 w.endElement();
             }
 
             w.endElement();
         }
 
         // ----------------------------------------------------------------------
         // Requirements
         // ----------------------------------------------------------------------
 
         if ( !requirements.isEmpty() )
         {
             w.startElement( "requirements" );
 
             for ( Iterator i = requirements.keySet().iterator(); i.hasNext(); )
             {
                 String key = (String) i.next();
                 Requirement requirement = (Requirement) requirements.get( key );
 
                 w.startElement( "requirement" );
 
                 element( w, "role", requirement.getRole() );
 
                 if ( requirement.getRoleHint() != null )
                 {
                     element( w, "role-hint", requirement.getRoleHint() );
                 }
 
                 element( w, "field-name", key );
 
                 w.endElement();
             }
 
             w.endElement();
         }
 
         // ----------------------------------------------------------------------
         //
         // ----------------------------------------------------------------------
 
         w.endElement();
     }
 
     public void element( XMLWriter w, String name, String value )
     {
         w.startElement( name );
 
         if ( value == null )
         {
             value = "";
         }
 
         w.writeText( value );
 
         w.endElement();
     }
 }
