 package org.apache.maven.scm.plugin;
 
 /*
  * Copyright 2001-2006 The Apache Software Foundation.
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
 
 import org.apache.maven.plugin.MojoExecutionException;
 import org.apache.maven.scm.ScmException;
 import org.apache.maven.scm.ScmFileSet;
 import org.apache.maven.scm.command.checkout.CheckOutScmResult;
 import org.apache.maven.scm.repository.ScmRepository;
 import org.codehaus.plexus.util.FileUtils;
 
 import java.io.File;
 import java.io.IOException;
 
 /**
  * @author <a href="evenisse@apache.org">Emmanuel Venisse</a>
  * @version $Id$
  * @goal checkout
  * @description Check out a project
  * @requiresProject false
  */
 public class CheckoutMojo
     extends AbstractScmMojo
 {
     /**
      * @parameter expression="${branch}
      */
     private String branch;
 
     /**
      * The tag to use when checking out or tagging a project.
      *
      * @parameter expression="${tag}
      */
     private String tag;
 
     /**
      * The directory to checkout the sources to for the bootstrap and checkout goals
      *
      * @parameter expression="${checkoutDirectory}" default-value="${project.build.directory}/checkout"
      */
     private File checkoutDirectory;
 
     /**
      * Skip checkout if checkoutDirectory exists.
      *
      * @parameter expression="${skipCheckoutIfExists}" default-value="false"
      */
     private boolean skipCheckoutIfExists = false;
 
     public void execute()
         throws MojoExecutionException
     {
         //skip checkout if checkout directory is already created. See SCM-201
         if ( ! getCheckoutDirectory().isDirectory() || ! this.skipCheckoutIfExists )
         {
             checkout();
         }
     }
 
     protected File getCheckoutDirectory()
     {
         return this.checkoutDirectory;
     }
 
    public void setCheckoutDirectory( File checkoutDirectory )
    {
        this.checkoutDirectory = checkoutDirectory;
    }

     protected void checkout()
         throws MojoExecutionException
     {
         try
         {
             ScmRepository repository = getScmRepository();
 
             String currentTag = null;
 
             if ( branch != null )
             {
                 currentTag = branch;
             }
 
             if ( tag != null )
             {
                 currentTag = tag;
             }
 
             try
             {
                 this.getLog().info( "Removing " + getCheckoutDirectory() );
 
                 FileUtils.deleteDirectory( getCheckoutDirectory() );
             }
             catch ( IOException e )
             {
                 throw new MojoExecutionException( "Cannot remove " + getCheckoutDirectory() );
             }
 
             if ( ! getCheckoutDirectory().mkdirs() )
             {
                 throw new MojoExecutionException( "Cannot create " + getCheckoutDirectory() );
             }
 
             CheckOutScmResult result = getScmManager().getProviderByRepository( repository ).checkOut( repository,
                                                                                                        new ScmFileSet(
                                                                                                            getCheckoutDirectory().getAbsoluteFile() ),
                                                                                                        currentTag );
 
             checkResult( result );
         }
         catch ( ScmException e )
         {
             throw new MojoExecutionException( "Cannot run checkout command : ", e );
         }
     }
 }
