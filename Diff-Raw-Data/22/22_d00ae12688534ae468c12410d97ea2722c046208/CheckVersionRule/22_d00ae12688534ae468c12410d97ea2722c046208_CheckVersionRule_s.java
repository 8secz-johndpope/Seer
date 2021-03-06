 /**
  * This software is licensed under the Apache 2 license, quoted below.
  *
  * Copyright 2010 Julien Eluard
  *
  * Licensed under the Apache License, Version 2.0 (the "License"); you may not
  * use this file except in compliance with the License. You may obtain a copy of
  * the License at
  *
  *     [http://www.apache.org/licenses/LICENSE-2.0]
  *
  * Unless required by applicable law or agreed to in writing, software
  * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
  * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
  * License for the specific language governing permissions and limitations under
  * the License.
  */
 
 package org.semver.enforcer;
 
 import java.io.File;
 import java.io.IOException;
 import java.util.Arrays;
 import java.util.Collections;
 import java.util.HashSet;
 import java.util.Set;
 
 import org.apache.maven.artifact.Artifact;
 import org.apache.maven.artifact.factory.ArtifactFactory;
 import org.apache.maven.artifact.repository.ArtifactRepository;
 import org.apache.maven.artifact.resolver.ArtifactResolver;
 import org.apache.maven.enforcer.rule.api.EnforcerRule;
 import org.apache.maven.enforcer.rule.api.EnforcerRuleException;
 import org.apache.maven.enforcer.rule.api.EnforcerRuleHelper;
 import org.apache.maven.project.MavenProject;
 import org.codehaus.plexus.util.StringUtils;
 import org.semver.Comparer;
 import org.semver.Delta;
 import org.semver.Dumper;
 import org.semver.Version;
 
 /**
  * 
  * Checks {@link Version} for current {@link Artifact} compared to a previous {@link Artifact}.
  * <br />
  * Fails if {@link Version} semantic is not respected.
  * 
  */
 public final class CheckVersionRule implements EnforcerRule {
 
     /**
      * Version number of artifact to be checked.
      *
      * @parameter
      */
     private String previousVersion;        
     
     /**
      * Class names to be included.
      *
      * @parameter
      */
     private String[] includes;    
     
     /**
      * Class names to be excluded.
      *
      * @parameter
      */
     private String[] excludes;
     
     /**
      * Dump change details.
      *
      * @parameter
      */ 
     private boolean dumpDetails = false;
     
     private Set<String> extractFilters(final String[] filtersAsStringArray) {
         if (filtersAsStringArray == null) {
             return Collections.emptySet();
         }
         return new HashSet<String>(Arrays.asList(filtersAsStringArray));
     }
     
     @Override
     public void execute(final EnforcerRuleHelper helper) throws EnforcerRuleException {
         if (StringUtils.isEmpty(this.previousVersion)) {
             throw new EnforcerRuleException("previousVersion can't be empty");
         }
         final Artifact previousArtifact;
         final Artifact currentArtifact;
         try {
            final MavenProject project = (MavenProject) helper.evaluate("${project}");
             final ArtifactFactory artifactFactory = (ArtifactFactory) helper.getComponent(ArtifactFactory.class);
            previousArtifact = artifactFactory.createArtifact(project.getGroupId(), project.getArtifactId(), this.previousVersion, null, "jar");
             final ArtifactResolver resolver = (ArtifactResolver) helper.getComponent(ArtifactResolver.class );
             final ArtifactRepository localRepository = (ArtifactRepository) helper.evaluate("${localRepository}");
             resolver.resolve(previousArtifact, project.getRemoteArtifactRepositories(), localRepository);
             currentArtifact = project.getArtifact();
             
             validateArtifact(previousArtifact);
             validateArtifact(currentArtifact);
         } catch (Exception e) {
            throw new EnforcerRuleException("Exception while accessing artifacts: "+e.toString(), e);
         }     
             
         final Version previous = Version.parse(previousArtifact.getVersion());
         final File previousJar = previousArtifact.getFile();
         final Version current = Version.parse(currentArtifact.getVersion());
         final File currentJar = currentArtifact.getFile();
         helper.getLog().info("Using <"+previousJar+"> as previous JAR");
         helper.getLog().info("Using <"+currentJar+"> as current JAR");
 
         try {
             final Comparer comparer = new Comparer(previousJar, currentJar, extractFilters(this.includes), extractFilters(this.excludes));
             final Delta delta = comparer.diff();
             final boolean compatible = delta.validate(previous, current);
             if (!compatible) {
                 if (this.dumpDetails) {
                     Dumper.dump(delta);
                 }
                 throw new EnforcerRuleException("Current codebase incompatible with version <"+current+">. Version should be at least <"+delta.infer(previous)+">.");
             }
         } catch (IOException e) {
             throw new EnforcerRuleException("Exception while checking compatibility: "+e.toString(), e);
         }
     }
 
     /**
      * Validates that specified {@link Artifact} is a JAR file.
      * @param artifact
      */
     private void validateArtifact(final Artifact artifact) {
         if (!artifact.getFile().isFile()) {
             throw new IllegalArgumentException("<"+artifact.getFile()+"> is not a file");
         }
         if (!artifact.getType().equalsIgnoreCase("jar")) {
             throw new IllegalArgumentException("<"+artifact.getFile()+"> is not a JAR");
         }
     }
 
     @Override
     public boolean isCacheable() {
         return false;
     }
 
     @Override
     public boolean isResultValid(final EnforcerRule cachedRule) {
         return false;
     }
 
     @Override
     public String getCacheId() {
         return "0";
     }
 
 }
