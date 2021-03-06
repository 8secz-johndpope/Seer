 package org.apache.maven.continuum.web.action;
 
 /*
  * Copyright 2004-2005 The Apache Software Foundation.
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
 
 import org.apache.maven.continuum.Continuum;
 import org.apache.maven.continuum.ContinuumException;
 import org.apache.maven.continuum.model.project.BuildDefinition;
 import org.apache.maven.continuum.model.project.Profile;
 import org.apache.maven.continuum.model.project.Project;
 import org.apache.maven.continuum.model.project.Schedule;
 
 import com.opensymphony.xwork.ActionSupport;
 
 import java.util.Collection;
 import java.util.HashMap;
 import java.util.Iterator;
 import java.util.Map;
 
 /**
  * @author <a href="mailto:evenisse@apache.org">Emmanuel Venisse</a>
  * @version $Id$
  */
 public class BuildDefinitionEditAction
     extends ActionSupport
 {
     private Continuum continuum;
 
     private BuildDefinition bd;
 
     private Project project;
 
     private int projectId;
 
     private int buildDefinitionId;
 
     private boolean defaultForProject;
 
     private String goals;
 
     private String arguments;
 
     private String buildFile;
 
     private int scheduleId;
 
     private String scheduleName;
 
     private Map schedulesMap;
 
 //    private Profile profile;
 
     public String execute()
     {
         try
         {
            boolean isNew = false;

             bd = getBuildDefinition();
 
            if ( bd == null || buildDefinitionId == 0 )
            {
                bd = new BuildDefinition();

                isNew = true;
            }

             bd.setGoals( goals );
 
             bd.setBuildFile( buildFile );
 
             bd.setArguments( arguments );
 
             bd.setDefaultForProject( defaultForProject );
 
             Schedule schedule = continuum.getSchedule( scheduleId );
 
             bd.setSchedule( schedule );
 
            if ( !isNew )
            {
                continuum.updateBuildDefinition( bd, projectId );
            }
            else
            {
                continuum.addBuildDefinition( projectId, bd );
            }
         }
         catch ( ContinuumException e )
         {
             addActionMessage( "Can't update build definition (id=" + buildDefinitionId + ") for project " + projectId + " : " + e.getMessage() );
 
             e.printStackTrace();
 
             return ERROR;
         }
 
         return SUCCESS;
     }
 
     public String doDefault()
     {
         try
         {
             project = continuum.getProject( projectId );
 
             bd = getBuildDefinition();
         }
         catch ( ContinuumException e )
         {
             addActionMessage( "Can't get build definition informations (id=" + buildDefinitionId + ") for project " + projectId + " : " + e.getMessage() );
 
             e.printStackTrace();
 
             return ERROR;
         }
 
         defaultForProject = bd.isDefaultForProject();
 
         goals = bd.getGoals();
 
         arguments = bd.getArguments();
 
         buildFile = bd.getBuildFile();
 
         scheduleId = bd.getSchedule().getId();
 
         scheduleName = bd.getSchedule().getName();
 
         try
         {
             initSchedulesMap();
         }
         catch ( ContinuumException e )
         {
             addActionMessage( "Can't get schedules list : " + e.getMessage() );
 
             e.printStackTrace();
 
             return ERROR;
         }
 
 //        profile = bd.getProfile();
 
         return INPUT;
     }
 
     private void initSchedulesMap()
         throws ContinuumException
     {
         Collection schedules = continuum.getSchedules();
 
         for ( Iterator i = schedules.iterator(); i.hasNext(); )
         {
             Schedule schedule = (Schedule) i.next();
 
             if ( schedulesMap == null )
             {
                 schedulesMap = new HashMap();
             }
 
             schedulesMap.put( new Integer( schedule.getId() ), schedule.getName() );
         }
     }
 
     private BuildDefinition getBuildDefinition()
         throws ContinuumException
     {
         return continuum.getBuildDefinition( projectId, buildDefinitionId );
     }
 
     public Project getProject()
     {
         return project;
     }
 
     public int getProjectId()
     {
         return projectId;
     }
 
     public void setProjectId( int projectId )
     {
         this.projectId = projectId;
     }
 
     public int getBuildDefinitionId()
     {
         return buildDefinitionId;
     }
 
     public void setBuildDefinitionId( int buildDefinitionId )
     {
         this.buildDefinitionId = buildDefinitionId;
     }
 
     public boolean isDefaultForProject()
     {
         return defaultForProject;
     }
 
     public void setDefaultForProject( boolean defaultForProject )
     {
         this.defaultForProject = defaultForProject;
     }
 
     public String getGoals()
     {
         return goals;
     }
 
     public void setGoals( String goals )
     {
         this.goals = goals;
     }
 
     public String getArguments()
     {
         return arguments;
     }
 
     public void setArguments( String arguments )
     {
         this.arguments = arguments;
     }
 
     public String getBuildFile()
     {
         return buildFile;
     }
 
     public void setBuildFile( String buildFile )
     {
         this.buildFile = buildFile;
     }
 
     public int getScheduleId()
     {
         return scheduleId;
     }
 
     public void setScheduleId( int scheduleId )
     {
         this.scheduleId = scheduleId;
     }
 
     public String getScheduleName()
     {
         return scheduleName;
     }
 
     public void setScheduleName( String scheduleName )
     {
         this.scheduleName = scheduleName;
     }
 
     public Map getSchedulesMap()
     {
         if ( schedulesMap == null )
         {
             try
             {
                 initSchedulesMap();
             }
             catch( ContinuumException e )
             {
             }
         }
         return schedulesMap;
     }
 }
