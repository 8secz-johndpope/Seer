 /*
  * Copyright (c) 2010, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
  * 
  * Licensed under the Apache License, Version 2.0 (the "License");
  * you may not use this file except in compliance with the License.
  * You may obtain a copy of the License at
  * 
  * http://www.apache.org/licenses/LICENSE-2.0
  * 
  * Unless required by applicable law or agreed to in writing, software
  * distributed under the License is distributed on an "AS IS" BASIS,
  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  * See the License for the specific language governing permissions and
  * limitations under the License.
  */
 
 package org.wso2.maven.registry;
 
 import org.apache.maven.model.Plugin;
 import org.apache.maven.plugin.MojoExecutionException;
 import org.apache.maven.plugin.MojoFailureException;
 import org.apache.maven.project.MavenProject;
 import org.apache.maven.project.MavenProjectHelper;
 import org.codehaus.plexus.util.xml.Xpp3Dom;
 import org.wso2.developerstudio.eclipse.utils.file.FileUtils;
 import org.wso2.maven.capp.model.Artifact;
 import org.wso2.maven.capp.mojo.AbstractPOMGenMojo;
 import org.wso2.maven.capp.utils.WSO2MavenPluginConstantants;
 import org.wso2.maven.core.utils.MavenUtils;
 import org.wso2.maven.registry.beans.RegistryCollection;
 import org.wso2.maven.registry.beans.RegistryElement;
 import org.wso2.maven.registry.beans.RegistryItem;
 import org.wso2.maven.registry.utils.GeneralProjectMavenUtils;
 
 import java.io.File;
 import java.io.IOException;
 import java.util.ArrayList;
 import java.util.Iterator;
 import java.util.List;
 
 /**
  * This is the Maven Mojo used for generating a pom for a sequence artifact 
  * from the old CApp project structure
  * 
  * @goal pom-gen
  * 
  */
 public class RegistryResourcePOMGenMojo extends AbstractPOMGenMojo {
 
 	/**
 	 * @parameter default-value="${project}"
 	 */
 	private MavenProject project;
 
 	/**
 	 * Maven ProjectHelper.
 	 * 
 	 * @component
 	 */
 	private MavenProjectHelper projectHelper;
 
 	/**
 	 * The path of the location to output the pom
 	 * 
 	 * @parameter expression="${project.build.directory}/artifacts"
 	 */
 	private File outputLocation;
 
 	/**
 	 * The resulting extension of the file
 	 * 
 	 * @parameter
 	 */
 	private File artifactLocation;
 	
 	/**
 	 * POM location for the module project
 	 * 
 	 * @parameter expression="${project.build.directory}/pom.xml"
 	 */
 	private File moduleProject;
 	
 	/**
 	 * Group id to use for the generated pom
 	 * 
 	 * @parameter
 	 */
 	private String groupId;
 	
 	private MavenProject mavenModuleProject;
 
 	private static final String ARTIFACT_TYPE="registry/resource";
 	
 	private List<RegistryArtifact> retrieveArtifacts() {
 		return GeneralProjectMavenUtils.retrieveArtifacts(getArtifactLocation());
 	}
 	
 	public void execute() throws MojoExecutionException, MojoFailureException {
 		//Retrieving all the existing ESB Artifacts for the given Maven project 
 		List<RegistryArtifact> artifacts = retrieveArtifacts();
 		
 		//Artifact list
 		List<Artifact> mappedArtifacts=new ArrayList<Artifact>();
 		
 		//Mapping ESBArtifacts to C-App artifacts so that we can reuse the maven-sequence-plugin
 		for (RegistryArtifact registryArtifact : artifacts) {
 	        Artifact artifact=new Artifact();
 	        artifact.setName(registryArtifact.getName());
 	        artifact.setVersion(registryArtifact.getVersion());
 	        artifact.setType(registryArtifact.getType());
 	        artifact.setServerRole(registryArtifact.getServerRole());
 	        artifact.setFile("registry-info.xml");
 	        artifact.setSource(new File(getArtifactLocation(),"artifact.xml"));
 	        mappedArtifacts.add(artifact);
         }
 		//Calling the process artifacts method of super type to continue the sequence.
 		super.processArtifacts(mappedArtifacts);
 
 	}
 	
 	
 	protected void copyResources(MavenProject project, File projectLocation, Artifact artifact)throws IOException {
 		//POM file and Registry-info.xml in the outside
 		
 		//Creating the registry info file outdide
 		File regInfoFile = new File(projectLocation, "registry-info.xml");
 		RegistryInfo regInfo=new RegistryInfo();
 		regInfo.setSource(regInfoFile);
 		
 		//Filling info sections
 		List<RegistryArtifact> artifacts = retrieveArtifacts();
 		for (Iterator iterator = artifacts.iterator(); iterator.hasNext();) {
 	        RegistryArtifact registryArtifact = (RegistryArtifact) iterator.next();
 	        if(registryArtifact.getName().equalsIgnoreCase(artifact.getName()) && 
 	        		registryArtifact.getVersion().equalsIgnoreCase(artifact.getVersion()) && 
 	        		registryArtifact.getType().equalsIgnoreCase(artifact.getType()) && 
 	        		registryArtifact.getServerRole().equalsIgnoreCase(artifact.getServerRole())){
 	        	//This is the correct registry artifact for this artifact:Yes this is reverse artifact to registry artifact mapping
 	        	List<RegistryElement> allRegistryItems = registryArtifact.getAllRegistryItems();
 	        	for (RegistryElement registryItem : allRegistryItems) {
 	                regInfo.addESBArtifact(registryItem);
                 }
 	        	break;
 	        }
         }
 		
 		try {
 	        regInfo.toFile();
         } catch (Exception e) {
         }
 		
 		List<RegistryElement> allESBArtifacts = regInfo.getAllESBArtifacts();
 		for (RegistryElement registryItem : allESBArtifacts) {
 			File file = null;
 			if (registryItem instanceof RegistryItem) {
 				file =
 				       new File(artifact.getSource().getParentFile().getPath(),
 				                ((RegistryItem) registryItem).getFile());
 			} else if (registryItem instanceof RegistryCollection) {
 				file =
 				       new File(artifact.getSource().getParentFile().getPath(),
 				                ((RegistryCollection) registryItem).getDirectory());
 			}
 			if (file.isFile()) {
 				FileUtils.copy(file,
				               new File(projectLocation, "resources" + File.separator + file.getName()));
 			} else {
 				FileUtils.copyDirectory(file,
 				                        new File(projectLocation, "resources" + File.separator +
				                        		file.getName()));
 			}
         }
 		
		for (RegistryElement registryElement : allESBArtifacts) {
			File file = null;
			if (registryElement instanceof RegistryItem) {
				file =
				       new File(artifact.getSource().getParentFile().getPath(),
				                ((RegistryItem) registryElement).getFile());
				((RegistryItem) registryElement).setFile(file.getName());
			} else if (registryElement instanceof RegistryCollection) {
				file =
				       new File(artifact.getSource().getParentFile().getPath(),
				                ((RegistryCollection) registryElement).getDirectory());
				((RegistryCollection) registryElement).setDirectory(file.getName());
			}
			try {
				regInfo.toFile();
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
 //		if(artifact.getFile().isDirectory()){
 //			File file = new File(artifact.getFile().getParentFile(),"resources");
 //			FileUtils.copyDirectory(file, new File(projectLocation, file.getName()));
 //		}
 	}
 	
 	protected void addPlugins(MavenProject artifactMavenProject, Artifact artifact) {
 		Plugin plugin = MavenUtils.createPluginEntry(artifactMavenProject,"org.wso2.maven","maven-registry-plugin",WSO2MavenPluginConstantants.MAVEN_REGISTRY_PLUGIN_VERSION,true);
 		Xpp3Dom configuration = (Xpp3Dom)plugin.getConfiguration();
 		//add configuration
 		Xpp3Dom aritfact = MavenUtils.createConfigurationNode(configuration,"artifact");
 		aritfact.setValue(artifact.getFile().getName());
 	}
 
 	protected String getArtifactType() {
 		return ARTIFACT_TYPE;
 	}
 
 	
 }
