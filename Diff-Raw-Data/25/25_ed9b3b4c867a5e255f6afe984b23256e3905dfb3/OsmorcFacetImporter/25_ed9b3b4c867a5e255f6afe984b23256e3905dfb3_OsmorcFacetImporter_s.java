 /*
  * Copyright (c) 2007-2009, Osmorc Development Team
  * All rights reserved.
  *
  * Redistribution and use in source and binary forms, with or without modification,
  * are permitted provided that the following conditions are met:
  *     * Redistributions of source code must retain the above copyright notice, this list
  *       of conditions and the following disclaimer.
  *     * Redistributions in binary form must reproduce the above copyright notice, this
  *       list of conditions and the following disclaimer in the documentation and/or other
  *       materials provided with the distribution.
  *     * Neither the name of 'Osmorc Development Team' nor the names of its contributors may be
  *       used to endorse or promote products derived from this software without specific
  *       prior written permission.
  * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY
  * EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
  * MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL
  * THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
  * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT
  * OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
  * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR
  * TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE,
  * EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
  */
 
 package org.osmorc.facet.maven;
 
 import com.intellij.openapi.diagnostic.Logger;
 import com.intellij.openapi.module.Module;
 import com.jgoodies.binding.beans.BeanUtils;
 import org.jdom.Element;
 import org.jetbrains.annotations.NotNull;
 import org.jetbrains.idea.maven.importing.FacetImporter;
 import org.jetbrains.idea.maven.importing.MavenModifiableModelsProvider;
 import org.jetbrains.idea.maven.importing.MavenRootModelAdapter;
 import org.jetbrains.idea.maven.model.MavenId;
 import org.jetbrains.idea.maven.model.MavenPlugin;
 import org.jetbrains.idea.maven.project.*;
 import org.osgi.framework.Constants;
 import org.osmorc.facet.OsmorcFacet;
 import org.osmorc.facet.OsmorcFacetConfiguration;
 import org.osmorc.facet.OsmorcFacetType;
 
 import java.util.Collection;
 import java.util.HashMap;
 import java.util.List;
 import java.util.Map;
 
 /**
  * The OsmorcFacetImporter tries to read maven metadata and import OSGi specific settings as an Osmorc facet.
  *
  * @author <a href="mailto:janthomae@janthomae.de">Jan Thom&auml;</a>
  * @version $Id:$
  */
 public class OsmorcFacetImporter extends FacetImporter<OsmorcFacet, OsmorcFacetConfiguration, OsmorcFacetType> {
 
   private final Logger logger = Logger.getInstance("#org.osmorc.facet.maven.OsmorcFacetImporter");
 
   public OsmorcFacetImporter() {
     super("org.apache.felix", "maven-bundle-plugin", OsmorcFacetType.getInstance(), "OSGi");
   }
 
   public boolean isApplicable(MavenProject mavenProjectModel) {
     MavenPlugin p = mavenProjectModel.findPlugin(myPluginGroupID, myPluginArtifactID);
     // fixes: IDEA-56021
     String packaging = mavenProjectModel.getPackaging();
     return p != null && packaging != null && "bundle".equals(packaging);
   }
 
   protected void setupFacet(OsmorcFacet osmorcFacet, MavenProject mavenProjectModel) {
 
   }
 
   protected void reimportFacet(MavenModifiableModelsProvider modelsProvider, Module module,
                                MavenRootModelAdapter mavenRootModelAdapter, OsmorcFacet osmorcFacet,
                                MavenProjectsTree mavenProjectsTree, MavenProject mavenProject,
                                MavenProjectChanges changes, Map<MavenProject, String> mavenProjectStringMap,
                                List<MavenProjectsProcessorTask> mavenProjectsProcessorPostConfigurationTasks) {
 
     OsmorcFacetConfiguration conf = osmorcFacet.getConfiguration();
     MavenPlugin p = mavenProject.findPlugin(myPluginGroupID, myPluginArtifactID);
     // TODO: check if there is a manifest, in which case use this manifest!
 
     // first off, we get the defaults, that is
     // Symbolic name == groupId + "." + artifactId
     // TODO: we should use DefaultMaven2OsgiConverter to do this, so we get the same results as the plugin would get
     MavenId id = mavenProject.getMavenId();
     conf.setBundleSymbolicName(id.getGroupId() + "." + id.getArtifactId());
     // version == project version
     conf.setBundleVersion(id.getVersion());
 
     if (p != null) {
       logger.debug("Plugin found.");
 
       // Check if there are any overrides set up in the maven plugin settings
       conf.setBundleSymbolicName(computeSymbolicName(mavenProject)); // IDEA-63243
       setConfigProperty(mavenProject, conf, "bundleVersion", "instructions." + Constants.BUNDLE_VERSION);
       setConfigProperty(mavenProject, conf, "bundleActivator", "instructions." + Constants.BUNDLE_ACTIVATOR);
 
       // now find any additional properties that might have been set up:
       Element instructionsNode = getConfig(mavenProject, "instructions");
       // Fix for IDEADEV-38685, NPE when the element is not set.
       if (instructionsNode != null) {
         Map<String, String> props = new HashMap<String, String>();
         @SuppressWarnings({"unchecked"})
         List<Element> children = instructionsNode.getChildren();
         for (Element child : children) {
           String name = child.getName();
           String value = child.getValue();
           if (value != null && !"".equals(value) && !Constants.BUNDLE_SYMBOLICNAME.equals(name) &&
               !Constants.BUNDLE_VERSION.equals(name) && !Constants.BUNDLE_ACTIVATOR.equals(name)) {
             // ok its an additional setting:
             props.put(name, value);
           }
         }
 
         // Fix for IDEA-63242 - don't merge it with the existing settings, overwrite them
         conf.importAdditionalProperties(props, true);
       }
     }
   }
 
   /**
    * Computes the Bundle-SymbolicName value from the data given in the maven project.
    * @param mavenProject the maven project
    * @return the bundle symbolic name.
    */
   @NotNull
   private String computeSymbolicName(MavenProject mavenProject) {
 
     String bundleSymbolicName = findConfigValue(mavenProject, "instructions." + Constants.BUNDLE_SYMBOLICNAME);
     // if it's not set compute it
     if (bundleSymbolicName == null || bundleSymbolicName.length() == 0) {
       // Get the symbolic name as groupId + "." + artifactId, with the following exceptions:
 
       MavenId mavenId = mavenProject.getMavenId();
       String groupId = mavenId.getGroupId();
       String artifactId = mavenId.getArtifactId();
       if (groupId == null || artifactId == null) {
         return "";
       }
 
       String lastSectionOfGroupId = groupId.substring(groupId.lastIndexOf(".") + 1);
       // if artifactId is equal to last section of groupId then groupId is returned. eg. org.apache.maven:maven -> org.apache.maven
       if (lastSectionOfGroupId.endsWith(artifactId)) {
         return groupId;
       }
 
       // if artifactId starts with last section of groupId that portion is removed. eg. org.apache.maven:maven-core -> org.apache.maven.core "
       String doubledNamePart = lastSectionOfGroupId + "-";
       if (artifactId.startsWith(doubledNamePart) && artifactId.length() > doubledNamePart.length()) {
         return groupId + "." + artifactId.substring(doubledNamePart.length());
       }
 
       return groupId + "." + artifactId;
     }
     else {
       return bundleSymbolicName;
     }
   }
 
   private void setConfigProperty(MavenProject mavenProjectModel, OsmorcFacetConfiguration conf,
                                  String confProperty, String mavenConfProperty) {
     String value = findConfigValue(mavenProjectModel, mavenConfProperty);
     // Fix for IDEA-63242 - don't merge it with the existing settings, overwrite them
     if (value == null) {
       value = "";
     }
     try {
       BeanUtils.setValue(conf, BeanUtils.getPropertyDescriptor(OsmorcFacetConfiguration.class, confProperty),
                          value);
     }
     catch (Exception e) {
       logger.error("Problem when setting property", e);
     }
   }
 
   @Override
   public void getSupportedDependencyTypes(Collection<String> result, SupportedRequestType type) {
     result.add("bundle");
   }
 }
