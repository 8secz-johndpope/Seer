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
 package org.osmorc.maven.facet;
 
 import com.intellij.openapi.module.Module;
 import com.intellij.openapi.util.text.StringUtil;
 import com.intellij.util.containers.ContainerUtil;
 import org.jdom.Element;
 import org.jetbrains.annotations.NotNull;
 import org.jetbrains.idea.maven.importing.FacetImporter;
 import org.jetbrains.idea.maven.importing.MavenModifiableModelsProvider;
 import org.jetbrains.idea.maven.importing.MavenRootModelAdapter;
 import org.jetbrains.idea.maven.model.MavenId;
 import org.jetbrains.idea.maven.model.MavenPlugin;
 import org.jetbrains.idea.maven.project.*;
 import org.jetbrains.jps.osmorc.model.OutputPathType;
 import org.osgi.framework.Constants;
 import org.osmorc.facet.OsmorcFacet;
 import org.osmorc.facet.OsmorcFacetConfiguration;
 import org.osmorc.facet.OsmorcFacetType;
 
 import java.io.File;
 import java.util.Collection;
 import java.util.List;
 import java.util.Map;
 import java.util.Set;
 
 /**
  * The OsmorcFacetImporter reads Maven metadata and import OSGi-specific settings as an Osmorc facet.
  *
  * @author <a href="mailto:janthomae@janthomae.de">Jan Thom&auml;</a>
  */
 public class OsmorcFacetImporter extends FacetImporter<OsmorcFacet, OsmorcFacetConfiguration, OsmorcFacetType> {
   private static final String INCLUDE_MANIFEST = "_include";
   private static final Set<String> INSTRUCTIONS_TO_SKIP = ContainerUtil.newHashSet(
     Constants.BUNDLE_SYMBOLICNAME, Constants.BUNDLE_VERSION, Constants.BUNDLE_ACTIVATOR, INCLUDE_MANIFEST);
 
   public OsmorcFacetImporter() {
     super("org.apache.felix", "maven-bundle-plugin", OsmorcFacetType.getInstance());
   }
 
   @Override
   public boolean isApplicable(MavenProject mavenProjectModel) {
     MavenPlugin p = mavenProjectModel.findPlugin(myPluginGroupID, myPluginArtifactID);
     // fixes: IDEA-56021
     String packaging = mavenProjectModel.getPackaging();
     return p != null && "bundle".equals(packaging);
   }
 
   @Override
   protected void setupFacet(OsmorcFacet osmorcFacet, MavenProject mavenProjectModel) { }
 
   @Override
   protected void reimportFacet(MavenModifiableModelsProvider modelsProvider, Module module,
                                MavenRootModelAdapter mavenRootModelAdapter, OsmorcFacet osmorcFacet,
                                MavenProjectsTree mavenProjectsTree, MavenProject mavenProject,
                                MavenProjectChanges changes, Map<MavenProject, String> mavenProjectStringMap,
                                List<MavenProjectsProcessorTask> mavenProjectsProcessorPostConfigurationTasks) {
     OsmorcFacetConfiguration conf = osmorcFacet.getConfiguration();
     if (conf.isDoNotSynchronizeWithMaven()) {
       return; // do nothing.
     }
 
     // first off, we get the defaults
     MavenId id = mavenProject.getMavenId();
     conf.setBundleSymbolicName(id.getGroupId() + "." + id.getArtifactId());
    String defaultVersion = ImporterUtil.cleanupVersion(id.getVersion());
    conf.setBundleVersion(defaultVersion);
 
     MavenPlugin plugin = mavenProject.findPlugin(myPluginGroupID, myPluginArtifactID);
     if (plugin == null) {
       return;
     }
 
     // Check if there are any overrides set up in the maven plugin settings
     conf.setBundleSymbolicName(computeSymbolicName(mavenProject)); // IDEA-63243
    conf.setBundleVersion(findConfigValue(mavenProject, "instructions." + Constants.BUNDLE_VERSION, defaultVersion));
     conf.setBundleActivator(findConfigValue(mavenProject, "instructions." + Constants.BUNDLE_ACTIVATOR));
 
     Map<String, String> props = ContainerUtil.newLinkedHashMap();  // to preserve the order of elements
     Map<String, String> modelMap = mavenProject.getModelMap();
 
     String description = modelMap.get("description");
     if (!StringUtil.isEmptyOrSpaces(description)) {
       props.put(Constants.BUNDLE_DESCRIPTION, description);
     }
 
     String licenses = modelMap.get("licenses");
     if (!StringUtil.isEmptyOrSpaces(licenses)) {
       props.put("Bundle-License", licenses);
     }
 
     String vendor = modelMap.get("organization.name");
     if (!StringUtil.isEmpty(vendor)) {
       props.put(Constants.BUNDLE_VENDOR, vendor);
     }
 
     String docUrl = modelMap.get("organization.url");
     if (!StringUtil.isEmptyOrSpaces(docUrl)) {
       props.put(Constants.BUNDLE_DOCURL, docUrl);
     }
 
     // now find any additional properties that might have been set up:
     Element instructionsNode = getConfig(mavenProject, "instructions");
     if (instructionsNode != null) {
       boolean useExistingManifest = false;
 
       for (Element child : instructionsNode.getChildren()) {
         String name = child.getName();
         String value = child.getValue();
 
         if (INCLUDE_MANIFEST.equals(name)) {
           conf.setManifestLocation(value);
           conf.setManifestGenerationMode(OsmorcFacetConfiguration.ManifestGenerationMode.Manually);
           conf.setUseProjectDefaultManifestFileLocation(false);
           useExistingManifest = true;
         }
 
         // sanitize instructions
         if (StringUtil.startsWithChar(name, '_')) {
           name = "-" + name.substring(1);
         }
 
         if (value != null) {
           value = value.replaceAll("\\p{Blank}*[\r\n]\\p{Blank}*", "");
         }
 
         if (!StringUtil.isEmpty(value) && !INSTRUCTIONS_TO_SKIP.contains(name)) {
           props.put(name, value);
         }
       }
 
       if (!useExistingManifest) {
         conf.setManifestLocation("");
         conf.setManifestGenerationMode(OsmorcFacetConfiguration.ManifestGenerationMode.OsmorcControlled);
         conf.setUseProjectDefaultManifestFileLocation(true);
       }
 
       // check if bundle name exists, if not compute it (IDEA-63244)
       if (!props.containsKey(Constants.BUNDLE_NAME)) {
         props.put(Constants.BUNDLE_NAME, computeBundleName(mavenProject));
       }
     }
 
     // now post-process the settings, to make Embed-Dependency work
     ImporterUtil.postProcessAdditionalProperties(props, mavenProject);
 
     // Fix for IDEA-63242 - don't merge it with the existing settings, overwrite them
     conf.importAdditionalProperties(props, true);
 
     // Fix for IDEA-66235 - inherit jar filename from maven
     String jarFileName = mavenProject.getFinalName() + ".jar";
 
     // FiX for IDEA-67088, preserve existing output path settings on reimport.
     switch (conf.getOutputPathType()) {
       case OsgiOutputPath:
         conf.setJarFileLocation(jarFileName, OutputPathType.OsgiOutputPath);
         break;
       case SpecificOutputPath:
         String path = new File(conf.getJarFilePath(), jarFileName).getPath();
         conf.setJarFileLocation(path, OutputPathType.SpecificOutputPath);
         break;
       default:
         conf.setJarFileLocation(jarFileName, OutputPathType.CompilerOutputPath);
     }
   }
 
   /**
    * Computes the Bundle-Name value from the data given in the maven project.
    */
   @NotNull
   private String computeBundleName(MavenProject mavenProject) {
     String bundleName = findConfigValue(mavenProject, "instructions." + Constants.BUNDLE_NAME);
     if (!StringUtil.isEmpty(bundleName)) {
       return bundleName;
     }
 
     // when no name is set, use the symbolic name
     String mavenProjectName = mavenProject.getName();
     return mavenProjectName != null ? mavenProjectName : computeSymbolicName(mavenProject);
   }
 
   /**
    * Computes the Bundle-SymbolicName value from the data given in the maven project.
    */
   @NotNull
   private String computeSymbolicName(MavenProject mavenProject) {
     String bundleSymbolicName = findConfigValue(mavenProject, "instructions." + Constants.BUNDLE_SYMBOLICNAME);
     if (!StringUtil.isEmpty(bundleSymbolicName)) {
       return bundleSymbolicName;
     }
 
     MavenId mavenId = mavenProject.getMavenId();
     String groupId = mavenId.getGroupId();
     String artifactId = mavenId.getArtifactId();
     if (groupId == null || artifactId == null) {
       return "";
     }
 
     // if artifactId is equal to last section of groupId then groupId is returned (org.apache.maven:maven -> org.apache.maven)
     String lastSectionOfGroupId = groupId.substring(groupId.lastIndexOf(".") + 1);
     if (lastSectionOfGroupId.endsWith(artifactId)) {
       return groupId;
     }
 
     // if artifactId starts with last section of groupId that portion is removed (org.apache.maven:maven-core -> org.apache.maven.core)
     String doubledNamePart = lastSectionOfGroupId + "-";
     if (artifactId.startsWith(doubledNamePart) && artifactId.length() > doubledNamePart.length()) {
       return groupId + "." + artifactId.substring(doubledNamePart.length());
     }
 
     return groupId + "." + artifactId;
   }
 
   @Override
   public void getSupportedDependencyTypes(Collection<String> result, SupportedRequestType type) {
     result.add("bundle");
   }
 }
