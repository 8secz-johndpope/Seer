 /*
  * Copyright 2013 Rimero Solutions
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
 package com.rimerosolutions.ant.git.tasks;
 
 import java.io.IOException;
 
 import org.apache.tools.ant.BuildException;
 import org.eclipse.jgit.api.Status;
 import org.eclipse.jgit.lib.Constants;
 import org.eclipse.jgit.lib.IndexDiff;
 import org.eclipse.jgit.lib.Repository;
 import org.eclipse.jgit.treewalk.FileTreeIterator;
 
 import com.rimerosolutions.ant.git.AbstractGitRepoAwareTask;
 import com.rimerosolutions.ant.git.GitBuildException;
 
 /**
  * Checks whether or not the Git Tree is up to date.
  *
  * <pre>{@code
  * <git:git localDirectory="${testLocalRepo}" verbose="true" settingsRef="git.testing">
  *  <git:uptodate failOnError="true"/>
  * </git:git>}</pre>
  *
  * <p><a href="http://www.kernel.org/pub/software/scm/git/docs/git-status.html">Git documentation about status</a></p>
  * <p><a href="http://download.eclipse.org/jgit/docs/latest/apidocs/org/eclipse/jgit/api/StatusCommand.html">JGit StatusCommand</a></p>
  *
  * @author Yves Zoundi
  */
 public class UpToDateTask extends AbstractGitRepoAwareTask {
 
         private String modificationExistProperty;
         private static final String TASK_NAME = "git-status";
         private static final String MESSAGE_UPTODATE_FAILED = "IO Error when checking repository status";
         private static final String MESSAGE_UPTODATE_SUCCESS = "The Git tree is up to date!";
        private static final String STATUS_NOT_CLEAN_TEMPLATE = "Status is not clean:'%s'";
 
         @Override
         public String getName() {
                 return TASK_NAME;
         }
 
         /**
          * Sets a given project property if the tree is modified
          *
          * @antdoc.notrequired
          * @param p Property name to set
          */
         public void setModificationExistProperty(String p) {
                 this.modificationExistProperty = p;
         }
 
         @Override
         protected void doExecute() throws BuildException {
                 Repository repo = git.getRepository();
 
                 FileTreeIterator workingTreeIterator = new FileTreeIterator(repo);
 
                 try {
                         IndexDiff diff = new IndexDiff(repo, Constants.HEAD, workingTreeIterator);
                         diff.diff();
 
                         Status status = new Status(diff);
 
                         if (!status.isClean()) {
                                 if (modificationExistProperty != null) {
                                         getProject().setProperty(modificationExistProperty, "true");
                                 }
 
                                 if (isFailOnError()) {
                                         StringBuilder msg = new StringBuilder();
                                         msg.append("The Git tree was modified.");
                                         msg.append("\n").append("Changed:").append(status.getChanged());
                                         msg.append("\n").append("Added:").append(status.getAdded());
                                         msg.append("\n").append("Modified:").append(status.getModified());
                                         msg.append("\n").append("Missing:").append(status.getMissing());
                                         msg.append("\n").append("Removed:").append(status.getRemoved());
                                         msg.append("\n").append("Untracked:").append(status.getUntracked());
 
                                         throw new GitBuildException(String.format(STATUS_NOT_CLEAN_TEMPLATE, msg.toString()));
                                 }
                         } else {
                                 log(MESSAGE_UPTODATE_SUCCESS);
                         }
                 } catch (IOException ioe) {
                         throw new GitBuildException(MESSAGE_UPTODATE_FAILED, ioe);
                 }
 
         }
 }
