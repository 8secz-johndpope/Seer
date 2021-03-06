 // Copyright (C) 2008 The Android Open Source Project
 //
 // Licensed under the Apache License, Version 2.0 (the "License");
 // you may not use this file except in compliance with the License.
 // You may obtain a copy of the License at
 //
 // http://www.apache.org/licenses/LICENSE-2.0
 //
 // Unless required by applicable law or agreed to in writing, software
 // distributed under the License is distributed on an "AS IS" BASIS,
 // WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 // See the License for the specific language governing permissions and
 // limitations under the License.
 
 package com.google.gerrit.client.patches;
 
 import com.google.gerrit.client.Gerrit;
 import com.google.gerrit.client.changes.PatchTable;
 import com.google.gerrit.client.changes.Util;
 import com.google.gerrit.client.data.PatchScript;
 import com.google.gerrit.client.reviewdb.AccountGeneralPreferences;
 import com.google.gerrit.client.reviewdb.Change;
 import com.google.gerrit.client.reviewdb.Patch;
 import com.google.gerrit.client.reviewdb.PatchSet;
 import com.google.gerrit.client.rpc.GerritCallback;
 import com.google.gerrit.client.rpc.NoDifferencesException;
 import com.google.gerrit.client.ui.ChangeLink;
 import com.google.gerrit.client.ui.Screen;
 import com.google.gwt.user.client.ui.DisclosurePanel;
 import com.google.gwt.user.client.ui.FlowPanel;
 import com.google.gwt.user.client.ui.Grid;
 import com.google.gwt.user.client.ui.HasHorizontalAlignment;
 import com.google.gwt.user.client.ui.Label;
 import com.google.gwt.user.client.ui.Widget;
 import com.google.gwt.user.client.ui.HTMLTable.CellFormatter;
 import com.google.gwtexpui.safehtml.client.SafeHtml;
 import com.google.gwtjsonrpc.client.RemoteJsonException;
 
 public abstract class PatchScreen extends Screen {
   public static class SideBySide extends PatchScreen {
     public SideBySide(final Patch.Key id, final int patchIndex,
         final PatchTable patchTable) {
       super(id, patchIndex, patchTable);
     }
 
     @Override
     protected SideBySideTable createContentTable() {
       return new SideBySideTable();
     }
 
     @Override
     protected PatchScreen.Type getPatchScreenType() {
       return PatchScreen.Type.SIDE_BY_SIDE;
     }
   }
 
   public static class Unified extends PatchScreen {
     public Unified(final Patch.Key id, final int patchIndex,
         final PatchTable patchTable) {
       super(id, patchIndex, patchTable);
     }
 
     @Override
     protected UnifiedDiffTable createContentTable() {
       return new UnifiedDiffTable();
     }
 
     @Override
     protected PatchScreen.Type getPatchScreenType() {
       return PatchScreen.Type.UNIFIED;
     }
   }
 
   protected final Patch.Key patchKey;
   protected PatchTable fileList;
   protected PatchSet.Id idSideA;
   protected PatchSet.Id idSideB;
   protected int contextLines;
 
   private DisclosurePanel historyPanel;
   private HistoryTable historyTable;
   private Label noDifference;
   private AbstractPatchContentTable contentTable;
 
   private int rpcSequence;
   private PatchScript script;
   private CommentDetail comments;
 
   /** The index of the file we are currently looking at among the fileList */
   private int patchIndex;
 
   /**
    * How this patch should be displayed in the patch screen.
    */
   public static enum Type {
     UNIFIED, SIDE_BY_SIDE
   }
 
   protected PatchScreen(final Patch.Key id, final int patchIndex,
       final PatchTable patchTable) {
     patchKey = id;
     fileList = patchTable;
     idSideA = null;
     idSideB = id.getParentKey();
     this.patchIndex = patchIndex;
 
     if (Gerrit.isSignedIn()) {
       final AccountGeneralPreferences p =
           Gerrit.getUserAccount().getGeneralPreferences();
       contextLines = p.getDefaultContext();
     } else {
       contextLines = AccountGeneralPreferences.DEFAULT_CONTEXT;
     }
   }
 
   @Override
   protected void onInitUI() {
     super.onInitUI();
 
     final Change.Id changeId = patchKey.getParentKey().getParentKey();
     final String path = patchKey.get();
     String fileName = path;
     final int last = fileName.lastIndexOf('/');
     if (last >= 0) {
       fileName = fileName.substring(last + 1);
     }
 
     setWindowTitle(PatchUtil.M.patchWindowTitle(changeId.get(), fileName));
     setPageTitle(PatchUtil.M.patchPageTitle(changeId.get(), path));
 
     historyTable = new HistoryTable(this);
     historyPanel = new DisclosurePanel(PatchUtil.C.patchHistoryTitle());
     historyPanel.setContent(historyTable);
     historyPanel.setOpen(false);
     historyPanel.setVisible(false);
     add(historyPanel);
 
     noDifference = new Label(PatchUtil.C.noDifference());
     noDifference.setStyleName("gerrit-PatchNoDifference");
     noDifference.setVisible(false);
 
     contentTable = createContentTable();
     contentTable.fileList = fileList;
 
     add(createNextPrevLinks());
    final FlowPanel fp = new FlowPanel();
    fp.setStyleName("gerrit-SideBySideScreen-SideBySideTable");
    fp.add(noDifference);
    fp.add(contentTable);
    add(fp);
     add(createNextPrevLinks());
   }
 
   private Widget createNextPrevLinks() {
     final Grid table = new Grid(1, 3);
     final CellFormatter fmt = table.getCellFormatter();
     table.setStyleName("gerrit-SideBySideScreen-LinkTable");
     fmt.setHorizontalAlignment(0, 0, HasHorizontalAlignment.ALIGN_LEFT);
     fmt.setHorizontalAlignment(0, 1, HasHorizontalAlignment.ALIGN_CENTER);
     fmt.setHorizontalAlignment(0, 2, HasHorizontalAlignment.ALIGN_RIGHT);
 
     if (fileList != null) {
       table.setWidget(0, 0, fileList.getPreviousPatchLink(patchIndex,
           getPatchScreenType()));
       table.setWidget(0, 2, fileList.getNextPatchLink(patchIndex,
           getPatchScreenType()));
     }
 
     final ChangeLink up =
         new ChangeLink("", patchKey.getParentKey().getParentKey());
     SafeHtml.set(up, SafeHtml.asis(Util.C.upToChangeIconLink()));
     table.setWidget(0, 1, up);
 
     return table;
   }
 
   @Override
   protected void onLoad() {
     super.onLoad();
     refresh(true);
   }
 
   @Override
   public void registerKeys() {
     super.registerKeys();
     contentTable.setRegisterKeys(contentTable.isVisible());
   }
 
   protected abstract AbstractPatchContentTable createContentTable();
 
   protected abstract PatchScreen.Type getPatchScreenType();
 
   protected void refresh(final boolean isFirst) {
     final int rpcseq = ++rpcSequence;
     script = null;
     comments = null;
 
     PatchUtil.DETAIL_SVC.patchScript(patchKey, idSideA, idSideB, contextLines,
         new GerritCallback<PatchScript>() {
           public void onSuccess(final PatchScript result) {
             if (rpcSequence == rpcseq) {
               script = result;
               onResult();
             }
           }
 
           @Override
           public void onFailure(final Throwable caught) {
             if (rpcSequence == rpcseq) {
               if (isNoDifferences(caught) && !isFirst) {
                 historyTable.enableAll(true);
                 showPatch(false);
               } else {
                 super.onFailure(caught);
               }
             }
           }
 
           private boolean isNoDifferences(final Throwable caught) {
             if (caught instanceof NoDifferencesException) {
               return true;
             }
             return caught instanceof RemoteJsonException
                 && caught.getMessage().equals(NoDifferencesException.MESSAGE);
           }
         });
 
     PatchUtil.DETAIL_SVC.patchComments(patchKey, idSideA, idSideB,
         new GerritCallback<CommentDetail>() {
           public void onSuccess(final CommentDetail result) {
             if (rpcSequence == rpcseq) {
               comments = result;
               onResult();
             }
           }
 
           @Override
           public void onFailure(Throwable caught) {
             // Ignore no such entity, the patch script RPC above would
             // also notice the problem and report it.
             //
             if (!isNoSuchEntity(caught) && rpcSequence == rpcseq) {
               super.onFailure(caught);
             }
           }
         });
   }
 
   private void onResult() {
     if (script != null && comments != null) {
       if (comments.getHistory().size() > 1) {
         historyTable.display(comments.getHistory());
         historyPanel.setVisible(true);
       } else {
         historyPanel.setVisible(false);
       }
 
       contentTable.display(patchKey, idSideA, idSideB, script);
       contentTable.display(comments);
       contentTable.finishDisplay();
       showPatch(true);
 
       script = null;
       comments = null;
 
       if (!isCurrentView()) {
         display();
       }
     }
   }
 
   private void showPatch(final boolean showPatch) {
     noDifference.setVisible(!showPatch);
     contentTable.setVisible(showPatch);
     contentTable.setRegisterKeys(isCurrentView() && showPatch);
   }
 }
