 /*
  * Copyright (C) 2010 Evgeny Mandrikov
  *
  * Sonar-IDE is free software; you can redistribute it and/or
  * modify it under the terms of the GNU Lesser General Public
  * License as published by the Free Software Foundation; either
  * version 3 of the License, or (at your option) any later version.
  *
  * Sonar-IDE is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
  * Lesser General Public License for more details.
  *
  * You should have received a copy of the GNU Lesser General Public
  * License along with Sonar-IDE; if not, write to the Free Software
  * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
  */
 
 package org.sonar.ide.eclipse.jobs;
 
 import java.util.ArrayList;
 import java.util.HashMap;
 import java.util.List;
 import java.util.Map;
 
 import org.apache.commons.lang.StringUtils;
 import org.eclipse.core.resources.IProject;
 import org.eclipse.core.runtime.IProgressMonitor;
 import org.eclipse.core.runtime.IStatus;
 import org.eclipse.core.runtime.Status;
 import org.eclipse.core.runtime.jobs.Job;
 import org.eclipse.jdt.core.IJavaProject;
 import org.eclipse.jdt.core.JavaCore;
 import org.sonar.ide.eclipse.SonarPlugin;
 import org.sonar.ide.eclipse.properties.ProjectProperties;
 import org.sonar.wsclient.Host;
 import org.sonar.wsclient.Sonar;
 import org.sonar.wsclient.services.Resource;
 import org.sonar.wsclient.services.ResourceQuery;
 
 /**
  * Auto configuration of projects by searching the equivalent on the server.
  * 
  * @link http://jira.codehaus.org/browse/SONARIDE-47
  * 
  * @author Jérémie Lagarde
  */
 public class AutoConfigureProjectJob extends Job {
 
   private final IProject[]                  projects;
   private final Map<String, List<Resource>> resourcesByServerMap = new HashMap<String, List<Resource>>();
 
   public AutoConfigureProjectJob(IProject project) {
     super(project.getName());
     this.projects = new IProject[] { project };
     setPriority(Job.LONG);
   }
 
   public AutoConfigureProjectJob(IProject[] projects) {
     super("Retrieve project information in sonar server"); // TODO put in
     // messages.properties
     this.projects = projects;
     setPriority(Job.LONG);
   }
 
   @Override
   protected IStatus run(IProgressMonitor monitor) {
     IStatus status = null;
     try {
       for (int i = 0; i < projects.length; i++) {
         if (projects[i].isOpen() && !monitor.isCanceled()) {
           retrieveProjectConfiguration(JavaCore.create(projects[i]), monitor);
         }
       }
       if (!monitor.isCanceled())
         status = Status.OK_STATUS;
       else
         status = Status.CANCEL_STATUS;
     } catch (Exception e) {
       status = new Status(IStatus.ERROR, SonarPlugin.PLUGIN_ID, IStatus.ERROR, e.getLocalizedMessage(), e);
     } finally {
       monitor.done();
     }
     return status;
   }
 
   private void retrieveProjectConfiguration(IJavaProject project, IProgressMonitor monitor) throws Exception {
     final ProjectProperties properties = ProjectProperties.getInstance(project.getResource());
     if (properties != null && properties.isProjectConfigured())
       return;
     String serverUrl = properties.getUrl();
     if (StringUtils.isNotBlank(serverUrl)) {
       retrieveProjectConfiguration(project, serverUrl, monitor);
     } else {
       for (Host host : SonarPlugin.getServerManager().getServers()) {
         retrieveProjectConfiguration(project, host.getHost(), monitor);
         if (properties.isProjectConfigured())
           return;
       }
     }
   }
 
   private void retrieveProjectConfiguration(IJavaProject project, String serverUtl, IProgressMonitor monitor) throws Exception {
     ProjectProperties properties = ProjectProperties.getInstance(project.getResource());
     if (properties != null && properties.isProjectConfigured())
       return;
     List<Resource> resources = retrieveResources(serverUtl, monitor);
     for (Resource resource : resources) {
       if (resource.getKey().endsWith(":" + project.getElementName())) {
         SonarPlugin.getDefault().getConsole().logResponse("Configure");
         properties.setUrl(serverUtl);
         properties.setArtifactId(project.getElementName());
         properties.setGroupId(StringUtils.substringBefore(resource.getKey(), ":"));
         properties.save();
       }
     }
   }
 
   private List<Resource> retrieveResources(String serverUrl, IProgressMonitor monitor) throws Exception {
     if (StringUtils.isBlank(serverUrl))
       return new ArrayList<Resource>();
     if (resourcesByServerMap.containsKey(serverUrl))
       return resourcesByServerMap.get(serverUrl);
     try {
       monitor.beginTask("Retrieve projects on " + serverUrl, 1); // TODO put it
                                                                  // in
                                                                  // messages.properties
       ResourceQuery query = new ResourceQuery();
       query.setScopes(Resource.SCOPE_SET);
       query.setQualifiers(Resource.QUALIFIER_PROJECT, Resource.QUALIFIER_MODULE);
       Sonar sonar = SonarPlugin.getServerManager().getSonar(serverUrl);
       List<Resource> resources = sonar.findAll(query);
       resourcesByServerMap.put(serverUrl, resources);
     } catch (Exception ex) {
       SonarPlugin.getDefault().getConsole().logError("Error in retrieving projects list on " + serverUrl, ex);
       resourcesByServerMap.put(serverUrl, new ArrayList<Resource>());
     } finally {
       monitor.done();
     }
     return resourcesByServerMap.get(serverUrl);
   }
 }
