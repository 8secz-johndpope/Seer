 package org.apache.maven.wagon.providers.ssh;
 
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
 
 import com.jcraft.jsch.ChannelSftp;
 import com.jcraft.jsch.JSchException;
 import com.jcraft.jsch.SftpATTRS;
 import com.jcraft.jsch.SftpException;
 import org.apache.maven.wagon.PathUtils;
 import org.apache.maven.wagon.ResourceDoesNotExistException;
 import org.apache.maven.wagon.TransferFailedException;
 import org.apache.maven.wagon.authorization.AuthorizationException;
 import org.apache.maven.wagon.events.TransferEvent;
 import org.apache.maven.wagon.repository.RepositoryPermissions;
 import org.apache.maven.wagon.resource.Resource;
 import org.codehaus.plexus.util.StringUtils;
 
 import java.io.File;
 
 /**
  * SFTP protocol wagon.
  *
  * @author <a href="mailto:brett@apache.org">Brett Porter</a>
  * @version $Id$
  * @todo [BP] add compression flag
  */
 public class SftpWagon
     extends ScpWagon
     implements SshCommandExecutor
 {
     private static final String SFTP_CHANNEL = "sftp";
 
     private static final int S_IFDIR = 0x4000;
 
     // ----------------------------------------------------------------------
     //
     // ----------------------------------------------------------------------
 
     public void put( File source, String resourceName )
         throws TransferFailedException, ResourceDoesNotExistException, AuthorizationException
     {
         String basedir = getRepository().getBasedir();
 
         resourceName = StringUtils.replace( resourceName, "\\", "/" );
         String dir = PathUtils.dirname( resourceName );
         dir = StringUtils.replace( dir, "\\", "/" );
 
         Resource resource = new Resource( resourceName );
 
         firePutInitiated( resource, source );
 
         ChannelSftp channel;
 
         String filename;
 
         if ( resourceName.lastIndexOf( '/' ) > 0 )
         {
             filename = resourceName.substring( resourceName.lastIndexOf( '/' ) + 1 );
         }
         else
         {
             filename = resourceName;
         }
 
         try
         {
             channel = (ChannelSftp) session.openChannel( SFTP_CHANNEL );
 
             channel.connect();
 
             channel.cd( basedir );
 
             RepositoryPermissions permissions = getRepository().getPermissions();
 
             mkdirs( channel, resourceName, getDirectoryMode(permissions));
 
             firePutStarted( resource, source );
 
             channel.put( source.getAbsolutePath(), filename );
 
             postProcessListeners( resource, source, TransferEvent.REQUEST_PUT );
 
             if ( permissions != null && permissions.getGroup() != null )
             {
                 int group;
                 try
                 {
                     group = Integer.valueOf( permissions.getGroup() ).intValue();
                     channel.chgrp( group, filename );
                 }
                 catch ( NumberFormatException e )
                 {
                     // TODO: warning level
                     fireTransferDebug( "Not setting group: must be a numerical GID for SFTP" );
                 }
             }
 
             if ( permissions != null && permissions.getFileMode() != null )
             {
                 int mode;
                 try
                 {
                     mode = Integer.valueOf( permissions.getFileMode() ).intValue();
                     channel.chmod( mode, filename );
                 }
                 catch ( NumberFormatException e )
                 {
                     // TODO: warning level
                     fireTransferDebug( "Not setting mode: must be a numerical mode for SFTP" );
                 }
             }
 
             firePutCompleted( resource, source );
 
             String[] dirs = PathUtils.dirnames( dir );
             for ( int i = 0; i < dirs.length; i++ )
             {
                 channel.cd( ".." );
             }
         }
         catch ( SftpException e )
         {
             String msg = "Error occured while deploying '" + resourceName + "' " +
                          "to remote repository: " + getRepository().getUrl();
 
             throw new TransferFailedException( msg, e );
         }
         catch ( JSchException e )
         {
             String msg = "Error occured while deploying '" + resourceName + "' " +
                          "to remote repository: " + getRepository().getUrl();
 
             throw new TransferFailedException( msg, e );
         }
 
         if ( channel != null )
         {
             channel.disconnect();
         }
     }
 
     /**
      * @param permissions repository's permissions
      * @return the directory mode for the repository or <code>-1</code> if it
      *         wasn't set
      */
     private int getDirectoryMode( RepositoryPermissions permissions )
     {
        try
        {
            return Integer.valueOf( permissions.getDirectoryMode(), 8 ).intValue();
        }
        catch ( NumberFormatException e )
         {
            // TODO: warning level
            fireTransferDebug( "the file mode must be a numerical mode for SFTP" );

            return -1;
         }
     }
 
     private void mkdirs( ChannelSftp channel, String resourceName, int mode )
         throws TransferFailedException, SftpException
     {
         String[] dirs = PathUtils.dirnames( resourceName );
         for ( int i = 0; i < dirs.length; i++ )
         {
             try
             {
                 SftpATTRS attrs = channel.stat( dirs[i] );
                 if ( ( attrs.getPermissions() & S_IFDIR ) == 0 )
                 {
                     throw new TransferFailedException( "Remote path is not a directory:" + PathUtils.dirname( resourceName ) );
                 }
             }
             catch ( SftpException e )
             {
                 // doesn't exist, make it and try again
                 channel.mkdir( dirs[i] );
                 if ( mode != -1 )
                 {
                     try
                     {
                         channel.chmod( mode, dirs[i] );
                     }
                     catch ( final SftpException e1 )
                     {
                         // for some extrange reason we recive this exception,
                         // even when chmod success
                     }
                 }
             }
 
             channel.cd( dirs[i] );
         }
     }
 
     public void get( String resourceName, File destination )
         throws TransferFailedException, ResourceDoesNotExistException, AuthorizationException
     {
         createParentDirectories( destination );
 
         ChannelSftp channel;
 
         resourceName = StringUtils.replace( resourceName, "\\", "/" );
         String dir = PathUtils.dirname( resourceName );
         dir = StringUtils.replace( dir, "\\", "/" );
 
         Resource resource = new Resource( resourceName );
 
         fireGetInitiated( resource, destination );
 
         String filename;
         if ( resourceName.lastIndexOf( '/' ) > 0 )
         {
             filename = resourceName.substring( resourceName.lastIndexOf( '/' ) + 1 );
         }
         else
         {
             filename = resourceName;
         }
 
         try
         {
             channel = (ChannelSftp) session.openChannel( SFTP_CHANNEL );
 
             channel.connect();
 
             channel.cd( repository.getBasedir() );
 
             channel.cd( dir );
 
             fireGetStarted( resource, destination );
 
             channel.get( filename, destination.getAbsolutePath() );
 
             postProcessListeners( resource, destination, TransferEvent.REQUEST_GET );
 
             fireGetCompleted( resource, destination );
 
             String[] dirs = PathUtils.dirnames( dir );
             for ( int i = 0; i < dirs.length; i++ )
             {
                 channel.cd( ".." );
             }
         }
         catch ( SftpException e )
         {
             handleGetException( resource, e, destination );
         }
         catch ( JSchException e )
         {
             handleGetException( resource, e, destination );
         }
     }
 
     public boolean getIfNewer( String resourceName, File destination, long timestamp )
     {
         throw new UnsupportedOperationException( "getIfNewer is scp wagon must be still implemented" );
     }
 }
