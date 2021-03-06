 package org.apache.maven.artifact.resolver;
 
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
 
 import java.util.List;
 
 /**
  * @author <a href="mailto:jason@maven.org">Jason van Zyl</a>
  * @version $Id$
  */
 public class ArtifactNotFoundException
     extends AbstractArtifactResolutionException
 {
     private String downloadUrl;
 
    public ArtifactNotFoundException( String message, Artifact artifact )
     {
         this( message, artifact.getGroupId(), artifact.getArtifactId(), artifact.getVersion(), artifact.getType(), null,
               null );
     }
 
     protected ArtifactNotFoundException( String message, Artifact artifact, List remoteRepositories, Throwable t )
     {
         this( message, artifact.getGroupId(), artifact.getArtifactId(), artifact.getVersion(), artifact.getType(),
               remoteRepositories, null, t );
     }
 
     public ArtifactNotFoundException( String message, String groupId, String artifactId, String version, String type,
                                       List remoteRepositories, String downloadUrl, Throwable t )
     {
         super( constructMessage( message, groupId, artifactId, version, type, downloadUrl ), groupId, artifactId,
                version, type, remoteRepositories, null, t );
 
         this.downloadUrl = downloadUrl;
     }
 
     private ArtifactNotFoundException( String message, String groupId, String artifactId, String version, String type,
                                        List remoteRepositories, String downloadUrl )
     {
         super( constructMessage( message, groupId, artifactId, version, type, downloadUrl ), groupId, artifactId,
                version, type, remoteRepositories, null );
 
         this.downloadUrl = downloadUrl;
     }
 
     private static String constructMessage( String message, String groupId, String artifactId, String version,
                                             String type, String downloadUrl )
     {
         StringBuffer sb = new StringBuffer( message );
 
         if ( downloadUrl != null && !"pom".equals( type ) )
         {
             sb.append( LS );
             sb.append( LS );
             sb.append( "Try downloading the file manually from" );
             sb.append( LS );
             sb.append( "  " );
             sb.append( downloadUrl );
             sb.append( LS );
             sb.append( "and install it using the command: " );
             sb.append( LS );
             sb.append( "  m2 install:install-file -DgroupId=" );
             sb.append( groupId );
             sb.append( " -DartifactId=" );
             sb.append( artifactId );
             sb.append( " -Dversion=" );
             sb.append( version );
             sb.append( " -Dpackaging=" );
             sb.append( type );
             sb.append( " -Dfile=/path/to/file" );
         }
 
         return sb.toString();
     }
 
     public String getDownloadUrl()
     {
         return downloadUrl;
     }
 
 }
