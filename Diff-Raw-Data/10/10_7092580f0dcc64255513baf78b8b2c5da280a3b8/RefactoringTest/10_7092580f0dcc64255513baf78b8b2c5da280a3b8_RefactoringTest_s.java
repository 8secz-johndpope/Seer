 /*******************************************************************************
  * Copyright (c) 2008 Sonatype, Inc.
  * All rights reserved. This program and the accompanying materials
  * are made available under the terms of the Eclipse Public License v1.0
  * which accompanies this distribution, and is available at
  * http://www.eclipse.org/legal/epl-v10.html
  *******************************************************************************/
 
 package org.maven.ide.eclipse.refactoring;
 
 import com.windowtester.runtime.swt.condition.shell.ShellDisposedCondition;
 import com.windowtester.runtime.swt.condition.shell.ShellShowingCondition;
 import com.windowtester.runtime.swt.internal.condition.NotCondition;
 import com.windowtester.runtime.swt.internal.condition.eclipse.DirtyEditorCondition;
 import com.windowtester.runtime.swt.locator.ButtonLocator;
 import com.windowtester.runtime.swt.locator.CTabItemLocator;
 import com.windowtester.runtime.swt.locator.TableItemLocator;
 import com.windowtester.runtime.swt.locator.TreeItemLocator;
 import com.windowtester.runtime.swt.locator.eclipse.ViewLocator;
import com.windowtester.runtime.util.ScreenCapture;
 
 
 /**
  * @author Eugene Kuleshov
  * @author Anton Kraev 
  */
 public class RefactoringTest extends RefactoringTestBase {
 
   private static final String _0_0_ZQ_SNAPSHOT = "0.0.zq-SNAPSHOT";
   private static final String _0_0_ZZQ_SNAPSHOT = "0.0.zzq-SNAPSHOT";
   private static final String ARTIFACT_ID = "artifactId";
   private static final String VERSION = "version";
  private static final String REFACTOR_RENAME_MAVEN_ARTIFACT = "Refactor/Rename Maven artifact...";
   private static final String OK = "OK";
   private static final String SAVE_ALL_MODIFIED_RESOURCES = "Save All Modified Resources";
   private static final String ORG_ECLIPSE_JDT_UI_PACKAGE_EXPLORER = "org.eclipse.jdt.ui.PackageExplorer";
  private static final String RENAME_ARTIFACT = "Rename artifact";
   private static final String PROGRESS_INFORMATION = "Progress Information";
   private static final String CHILD_POM_XML = "child/pom.xml";
   private static final String MINE_POM_XML = "mine/pom.xml";
 
   //tests version refactoring
   public void testRefactoringVersion() throws Exception {
     //open 2 editors
     ui.click(2, new TreeItemLocator(MINE_POM_XML, new ViewLocator(
         ORG_ECLIPSE_JDT_UI_PACKAGE_EXPLORER)));
     ui.click(2, new TreeItemLocator(CHILD_POM_XML, new ViewLocator(
         ORG_ECLIPSE_JDT_UI_PACKAGE_EXPLORER)));
    ui.click(new CTabItemLocator(MINE_POM_XML));
     ui.click(new TreeItemLocator(MINE_POM_XML, new ViewLocator(
         ORG_ECLIPSE_JDT_UI_PACKAGE_EXPLORER)));
     
     //click cancel after preview
     ui.contextClick(new TreeItemLocator(MINE_POM_XML, new ViewLocator(
         ORG_ECLIPSE_JDT_UI_PACKAGE_EXPLORER)),
         REFACTOR_RENAME_MAVEN_ARTIFACT);
     ui.wait(new ShellDisposedCondition(PROGRESS_INFORMATION));
     ui.wait(new ShellShowingCondition(RENAME_ARTIFACT));
     setTextValue(VERSION, _0_0_ZQ_SNAPSHOT);
     ui.click(new ButtonLocator("Previe&w >"));
     ui.wait(new ShellDisposedCondition(PROGRESS_INFORMATION));
     ui.wait(new ShellDisposedCondition(PROGRESS_INFORMATION));
     ui.click(new ButtonLocator("Cancel"));
 
     //refactor version
     ui.contextClick(new TreeItemLocator(MINE_POM_XML, new ViewLocator(
         ORG_ECLIPSE_JDT_UI_PACKAGE_EXPLORER)),
         REFACTOR_RENAME_MAVEN_ARTIFACT);
     ui.wait(new ShellDisposedCondition(PROGRESS_INFORMATION));
     ui.wait(new ShellShowingCondition(RENAME_ARTIFACT));
     setTextValue(VERSION, _0_0_ZQ_SNAPSHOT);
     ui.click(new ButtonLocator("Previe&w >"));
     ui.wait(new ShellDisposedCondition(PROGRESS_INFORMATION));
     ui.wait(new ShellDisposedCondition(PROGRESS_INFORMATION));
     ui.click(new ButtonLocator(OK));
     ui.wait(new ShellDisposedCondition(RENAME_ARTIFACT));
     assertTextValue(VERSION, _0_0_ZQ_SNAPSHOT);
 
     //check editor is not dirty
     ui.assertThat(new NotCondition(new DirtyEditorCondition()));
     
     //make it dirty
     setTextValue(VERSION, "");
     setTextValue(VERSION, _0_0_ZQ_SNAPSHOT);
     ui.assertThat(new DirtyEditorCondition());
     
     //see if the save dialog is displayed
     ui.contextClick(new TreeItemLocator(MINE_POM_XML, new ViewLocator(
         ORG_ECLIPSE_JDT_UI_PACKAGE_EXPLORER)),
         REFACTOR_RENAME_MAVEN_ARTIFACT);
     ui.wait(new ShellShowingCondition(SAVE_ALL_MODIFIED_RESOURCES));
     ui.click(new ButtonLocator(OK));
     ui.wait(new ShellDisposedCondition(SAVE_ALL_MODIFIED_RESOURCES));
 
     //rename again
     ui.wait(new ShellShowingCondition(RENAME_ARTIFACT));
     setTextValue(VERSION, _0_0_ZZQ_SNAPSHOT);
     ui.click(new ButtonLocator(OK));
     
     //check to see the child pom has proper parent
     ui.click(new CTabItemLocator(CHILD_POM_XML));
     ui.assertThat(new NotCondition(new DirtyEditorCondition()));
     assertTextValue("parentVersion", _0_0_ZZQ_SNAPSHOT);
   }
 
   //tests artifactId refactoring (tests version wildcard)
   public void testRefactoringArtifactId() throws Exception {
     ui.click(2, new TreeItemLocator(CHILD_POM_XML, new ViewLocator(
         ORG_ECLIPSE_JDT_UI_PACKAGE_EXPLORER)));
 
     //refactor artifactId
     ui.contextClick(new TreeItemLocator(MINE_POM_XML, new ViewLocator(
         ORG_ECLIPSE_JDT_UI_PACKAGE_EXPLORER)),
         REFACTOR_RENAME_MAVEN_ARTIFACT);
     ui.wait(new ShellDisposedCondition(PROGRESS_INFORMATION));
     ui.wait(new ShellShowingCondition(RENAME_ARTIFACT));
     setTextValue(ARTIFACT_ID, "parent");
     ui.click(new ButtonLocator("Previe&w >"));
     ui.wait(new ShellDisposedCondition(PROGRESS_INFORMATION));
     ui.wait(new ShellDisposedCondition(PROGRESS_INFORMATION));
     ui.click(new ButtonLocator(OK));
     ui.wait(new ShellDisposedCondition(RENAME_ARTIFACT));
     assertTextValue("parentArtifactId", "parent");
 
     //check editor is not dirty
     ui.assertThat(new NotCondition(new DirtyEditorCondition()));
     
     //check to see the child pom has proper parent
     ui.click(new CTabItemLocator(CHILD_POM_XML));
     ui.assertThat(new NotCondition(new DirtyEditorCondition()));
     assertTextValue("parentArtifactId", "parent");
     
     //test that dependencies were refactored (including wildcard)
     ui.click(new CTabItemLocator("Dependencies"));
     ui.click(new TableItemLocator("mine : parent"));
     ui.click(new TableItemLocator("mine : parent : 0.0.zzq-SNAPSHOT"));
   }
 }
