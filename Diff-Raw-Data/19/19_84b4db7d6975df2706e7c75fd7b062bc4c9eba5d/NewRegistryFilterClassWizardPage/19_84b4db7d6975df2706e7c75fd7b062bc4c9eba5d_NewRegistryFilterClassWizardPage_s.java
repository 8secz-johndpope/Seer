 /*
  * Copyright (c) 2010, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
 
 package org.wso2.developerstudio.eclipse.greg.registry.filter.ui.wizard;
 
 import java.io.File;
 
 import org.eclipse.core.resources.IFile;
 import org.eclipse.core.resources.IProject;
 import org.eclipse.core.runtime.CoreException;
 import org.eclipse.core.runtime.NullProgressMonitor;
 import org.eclipse.jdt.core.IJavaProject;
 import org.eclipse.jdt.core.JavaModelException;
 import org.eclipse.jdt.ui.wizards.NewClassWizardPage;
 import org.eclipse.jface.viewers.IStructuredSelection;
 import org.eclipse.ui.PlatformUI;
 import org.eclipse.ui.ide.IDE;
 import org.wso2.developerstudio.eclipse.greg.registry.filter.utils.RegistryFilterConstants;
 import org.wso2.developerstudio.eclipse.greg.registry.filter.utils.RegistryFilterUtils;
 import org.wso2.developerstudio.eclipse.libraries.utils.LibraryUtils;
 import org.wso2.developerstudio.eclipse.utils.jdt.JavaUtils;
 
 public class NewRegistryFilterClassWizardPage extends NewClassWizardPage{
 	
 	private String selectedProject;
 	private String[] depedencyList = {"abdera_1.0.0.wso2v3.jar",
 										"axiom_1.2.11.wso2v3.jar",
 										"axis2_1.6.1.wso2v6.jar",
 										"commons-codec_1.4.0.wso2v1.jar",
 										"commons-httpclient_3.1.0.wso2v2.jar",
 										"commons-io_2.0.0.wso2v2.jar",
 										"commons-logging-1.1.1.jar",
 										"geronimo-stax-api_1.0_spec_1.0.1.wso2v2.jar",
 										"httpcore_4.1.0.wso2v1.jar",
 										"neethi_2.0.4.wso2v4.jar",
 										"not-yet-commons-ssl-0.3.9.jar",
 										"woden-1.0.0.M8-wso2v1.jar",
 										"wsdl4j_1.6.2.wso2v4.jar",
 										"XmlSchema_1.4.7.wso2v2.jar"};
 	
 	public String getSelectedProject() {
 		return selectedProject;
 	}
 
 	public void setSelectedProject(String selectedProject) {
 		this.selectedProject = selectedProject;
 	}
 
 	public NewRegistryFilterClassWizardPage() {
 		setDescription("Create New Registry Filter Class");
 		setTitle("WSO2 Registry Filter Java Class");
 	}
 	
 	public void init(IStructuredSelection selection) {
 	    super.init(selection);
 		setSuperClass(RegistryFilterConstants.REGISTRY_FILTER_ABSTRACT_CLASS_NAME,true);
 		setMethodStubSelection(false, false, true, true);
 	}
 	
 	public String createClass() throws CoreException, InterruptedException{
		addSynapseLibrary();
 		addOtherDependencies();
 		createType(new NullProgressMonitor());
 		IDE.openEditor(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage(), (IFile)getCreatedType().getResource());
 		setSelectedProject(getCreatedType().getJavaProject().getElementName());
 		return getCreatedType().getFullyQualifiedName();
 	}
 
 	public void addOtherDependencies() throws JavaModelException{
 		IJavaProject javaProject = getJavaProject();
 		for (String dependencyName : depedencyList) {
 			File dependencyPath = LibraryUtils.getDependencyPath(dependencyName);
 			JavaUtils.addJarLibraryToProject(javaProject, dependencyPath);
 		}
 	}
 	
	private void addSynapseLibrary() throws JavaModelException {
 	    IJavaProject javaProject = getJavaProject();
 		JavaUtils.addJarLibraryToProject(javaProject, RegistryFilterUtils.getRegistryCoreLibraryPath());
     }
 
 	public  IProject getProject() {
 	    return getJavaProject().getProject();
     }
 	
 }
