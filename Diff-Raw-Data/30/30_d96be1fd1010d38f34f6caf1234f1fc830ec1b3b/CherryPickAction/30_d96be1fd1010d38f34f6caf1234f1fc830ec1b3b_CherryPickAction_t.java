 /*
  * Copyright 2013 Urs Wolfer
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
 
 package com.urswolfer.intellij.plugin.gerrit.ui.action;
 
 import com.google.common.base.Optional;
 import com.google.inject.Inject;
 import com.intellij.openapi.actionSystem.AnActionEvent;
 import com.intellij.openapi.actionSystem.PlatformDataKeys;
 import com.intellij.openapi.project.Project;
 import com.urswolfer.intellij.plugin.gerrit.GerritModule;
 import com.urswolfer.intellij.plugin.gerrit.git.GerritGitUtil;
 import com.urswolfer.intellij.plugin.gerrit.rest.bean.ChangeInfo;
 import icons.Git4ideaIcons;
 
 import java.util.concurrent.Callable;
 
 /**
  * @author Urs Wolfer
  */
 public class CherryPickAction extends AbstractChangeAction {
     @Inject
     private GerritGitUtil gerritGitUtil;
     @Inject
     private FetchActionsFactory fetchActionsFactory;
 
     public CherryPickAction() {
         super("Cherry-Pick (No Commit)", "Cherry-Pick change into active changelist without committing", Git4ideaIcons.CherryPick);
     }
 
     @Override
     public void actionPerformed(final AnActionEvent anActionEvent) {
         Optional<ChangeInfo> selectedChange = getSelectedChange(anActionEvent);
         if (!selectedChange.isPresent()) {
             return;
         }
         final Project project = anActionEvent.getData(PlatformDataKeys.PROJECT);
 
         final Optional<ChangeInfo> changeDetails = getChangeDetail(selectedChange.get(), project);
         if (!changeDetails.isPresent()) return;
 
         Callable<Void> successCallable = new Callable<Void>() {
             @Override
             public Void call() throws Exception {
                 final Project project = anActionEvent.getData(PlatformDataKeys.PROJECT);
                 gerritGitUtil.cherryPickChange(project, changeDetails.get());
                 return null;
             }
         };
         fetchActionsFactory.get(successCallable).actionPerformed(anActionEvent);
     }
 
    public static class Proxy extends CherryPickAction {
         private final CherryPickAction delegate;
 
         public Proxy() {
             super();
             delegate = GerritModule.getInstance(CherryPickAction.class);
         }
 
         @Override
         public void actionPerformed(AnActionEvent e) {
             delegate.actionPerformed(e);
         }
     }
 }
