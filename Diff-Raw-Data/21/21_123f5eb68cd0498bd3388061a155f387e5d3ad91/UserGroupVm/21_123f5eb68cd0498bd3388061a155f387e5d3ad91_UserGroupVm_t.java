 /**
  * Copyright (C) 2011  JTalks.org Team
  * This library is free software; you can redistribute it and/or
  * modify it under the terms of the GNU Lesser General Public
  * License as published by the Free Software Foundation; either
  * version 2.1 of the License, or (at your option) any later version.
  * This library is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
  * Lesser General Public License for more details.
  * You should have received a copy of the GNU Lesser General Public
  * License along with this library; if not, write to the Free Software
  * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
  */
 package org.jtalks.poulpe.web.controller.group;
 
 import org.jtalks.common.model.entity.Group;
 import org.jtalks.poulpe.model.entity.PoulpeBranch;
 import org.jtalks.poulpe.service.GroupService;
 import org.jtalks.poulpe.web.controller.SelectedEntity;
 import org.jtalks.poulpe.web.controller.WindowManager;
 import org.jtalks.poulpe.web.controller.zkutils.BindUtilsWrapper;
 import org.zkoss.bind.annotation.BindingParam;
 import org.zkoss.bind.annotation.Command;
 import org.zkoss.bind.annotation.NotifyChange;
 import org.zkoss.zul.ListModelList;
 
 import javax.annotation.Nonnull;
 import java.util.HashMap;
 import java.util.List;
 
 /**
  * View-model for 'User Groups' Is used to order to work with page that allows admin to manage groups(add, edit,
  * delete). Also class provides access to Members edit window, presented by {@link EditGroupMembersVm}.
  *
  * @author Leonid Kazancev
  */
 public class UserGroupVm {
     @SuppressWarnings("JpaQlInspection")
     private static final String SHOW_DELETE_CONFIRM_DIALOG = "showDeleteConfirmDialog",
             SHOW_GROUP_DIALOG = "showGroupDialog", SELECTED_GROUP = "selectedGroup",
             SHOW_MODERATOR_GROUP_SELECTION_PART = "showDeleteModeratorGroupDialog",
             MODERATING_BRANCHES = "branches", SELECTED_MODERATOR_GROUP = "selectedModeratorGroup",
             SELECTED_BRANCH = "selectedBranch", GROUP_SERVICE="groupService";
 
     //Injected
     private GroupService groupService;
     private final WindowManager windowManager;
 
     private ListModelList<Group> groups;
     private List<PoulpeBranch> branches;
     private HashMap<PoulpeBranch, Group> branchesMap;
     private Group selectedGroup;
     private Group selectedModeratorGroup;
     private SelectedEntity<Group> selectedEntity;
     private String searchString = "";
 
     private BindUtilsWrapper bindWrapper = new BindUtilsWrapper();
     private boolean showDeleteConfirmDialog, showGroupDialog, showDeleteModeratorGroupDialog;
 
     /**
      * Construct View-Model for 'User groups' view.
      *
      * @param groupService   the group service instance
      * @param selectedEntity the selected entity instance
      * @param windowManager  the window manager instance
      */
     public UserGroupVm(@Nonnull GroupService groupService, @Nonnull SelectedEntity<Group> selectedEntity,
                        @Nonnull WindowManager windowManager) {
         this.groupService = groupService;
         this.selectedEntity = selectedEntity;
         this.windowManager = windowManager;
 
         this.groups = new ListModelList<Group>(groupService.getAll(), true);
         this.branchesMap = new HashMap<PoulpeBranch, Group>(20);
     }
 
     /**
      * Makes group list view actual.
      */
     public void updateView() {
         groups.clear();
         groups.addAll(groupService.getAll());
     }
 
     // -- ZK Command bindings --------------------
 
     /**
      * Look for the users matching specified pattern from the search textbox.
      */
     @Command
     public void searchGroup() {
         groups.clear();
         groups.addAll(groupService.getByName(searchString));
     }
 
     /**
      * Opens edit group members window.
      */
     @Command
     public void showGroupMemberEditWindow() {
         selectedEntity.setEntity(selectedGroup);
         EditGroupMembersVm.showDialog(windowManager);
     }
 
     /**
      * Opens delete group dialog. Dialog style depends on kind of group.
      * If moderator group then shows addition part of delete group dialog.
      * BindWrapper used to prevent extra database hit if not moderator group.
      */
     @Command
     @NotifyChange({SHOW_DELETE_CONFIRM_DIALOG, SHOW_MODERATOR_GROUP_SELECTION_PART,
            SELECTED_MODERATOR_GROUP, GROUP_SERVICE, SELECTED_GROUP})
     public void showGroupDeleteConfirmDialog() {
         showDeleteConfirmDialog = true;
         if (isModeratingGroup()) {
             branches = getModeratedBranches();
             selectedModeratorGroup = selectedGroup;
             showDeleteModeratorGroupDialog = true;
         }
     }
 
 
     /**
      * Deletes selected group if it hasn't moderated comboboxList.
      */
     @Command
     @NotifyChange({SELECTED_GROUP, MODERATING_BRANCHES})
     public void deleteGroup() {
         if (!isModeratingGroup()) {
             groupService.deleteGroup(selectedGroup);
             selectedGroup = null;
             updateView();
             closeDialog();
             bindWrapper.postNotifyChange(UserGroupVm.this, SHOW_DELETE_CONFIRM_DIALOG);
         }
     }
 
     /**
      * Opens group adding dialog.
      */
     @Command
     @NotifyChange({SELECTED_GROUP, SHOW_GROUP_DIALOG})
     public void showNewGroupDialog() {
        groups.clearSelection();
         selectedGroup = new Group();
         showGroupDialog = true;
     }
 
     /**
      * Opens group edit dialog.
      */
     @Command
     @NotifyChange({SELECTED_GROUP, SHOW_GROUP_DIALOG})
     public void showEditDialog() {
         showGroupDialog = true;
     }
 
     /**
      * Saves group, closing group edit(add) dialog and updates view.
      */
     @Command
     @NotifyChange({SHOW_GROUP_DIALOG})
     public void saveGroup() {
         groupService.saveGroup(selectedGroup);
         closeDialog();
         updateView();
     }
 
     /**
      * Close all dialogs by set visibility to false.
      */
     @Command
     @NotifyChange({SHOW_GROUP_DIALOG, SHOW_DELETE_CONFIRM_DIALOG, SHOW_MODERATOR_GROUP_SELECTION_PART})
     public void closeDialog() {
         showDeleteConfirmDialog = false;
         showGroupDialog = false;
         showDeleteModeratorGroupDialog = false;
     }
 
     /**
      * Saves moderator group for all branches, was moderated by group to delete.
      */
     @Command
     @NotifyChange({SELECTED_BRANCH, MODERATING_BRANCHES, SHOW_MODERATOR_GROUP_SELECTION_PART})
     public void saveModeratorForBranches() {
         if (!selectedModeratorGroup.equals(selectedGroup)) {
             for (PoulpeBranch branch : branches) {
                 branch.setModeratorsGroup(selectedModeratorGroup);
             }
             closeDeleteModeratorGroupDialog();
         }
     }
 
     /**
      * Adds {@link org.zkoss.zul.Combobox} selected value to map, witch holds changes as pair branch plus his moderator group.
      *
      * @param branch {@link PoulpeBranch} key field for map
      * @param group  {@link Group} value field for map
      */
     @Command
     public void addToMap(@BindingParam("branch") PoulpeBranch branch, @BindingParam("group") Group group) {
         branchesMap.put(branch, group);
     }
 
     /**
      * If moderator group were changed for branch, given as parameter, saves moderator group for this branch.
      *
      * @param branch {@link PoulpeBranch} key field for map
      */
     @Command
     @NotifyChange({SHOW_MODERATOR_GROUP_SELECTION_PART, MODERATING_BRANCHES, "saveModeratorForBranch"})
     public void saveModeratorForCurrentBranch(@BindingParam("branch") PoulpeBranch branch) {
         if ((branchesMap.containsKey(branch)) && (!branch.getModeratorsGroup().equals(branchesMap.get(branch)))) {
             branch.setModeratorsGroup(branchesMap.get(branch));
             branches.remove(branch);
             closeDeleteModeratorGroupDialog();
         }
     }
 
     /**
      * If group has no moderated branches anymore, close delete dialog part, responsible for branches, moderated
      * by group to delete. In addition clears branchMap.
      */
     private void closeDeleteModeratorGroupDialog() {
         if (!isModeratingGroup()) {
             showDeleteModeratorGroupDialog = false;
             branchesMap.clear();
         }
     }
 
 
     /**
      * @return list of {@link PoulpeBranch} moderated by selected group
      */
     public List<PoulpeBranch> getModeratedBranches() {
         return selectedGroup == null ? null : groupService.getModeratedBranches(selectedGroup);
     }
 
     /**
      * @return true if group are moderating some {@link PoulpeBranch}
      */
     private boolean isModeratingGroup() {
         return selectedGroup != null && groupService.getModeratedBranches(selectedGroup).size() != 0;
     }
 
     // -- Getters/Setters --------------------
 
     /**
      * Gets visibility status of delete group dialog window, boolean show added as fix for onClose action,
      * which don't send anything to the server when closing window because of event.stopPropagation, so during next
      * change notification ZK will think that we need to show that dialog again which is wrong.
      *
      * @return true if dialog is visible
      */
     public boolean isShowDeleteConfirmDialog() {
         boolean show = showDeleteConfirmDialog;
         showDeleteConfirmDialog = false;
         return show;
     }
 
     /**
      * Gets visibility status of group(edit/create) dialog window, boolean show added as fix for onClose action,
      * which don't send anything to the server when closing window because of event.stopPropagation, so during next
      * change notification ZK will think that we need to show that dialog again which is wrong.
      *
      * @return true if dialog is visible
      */
     public boolean isShowGroupDialog() {
         boolean show = showGroupDialog;
         showGroupDialog = false;
         return show;
     }
 
     /**
      * @return true if show comboboxList dialog is visible
      */
     public boolean isShowDeleteModeratorGroupDialog() {
         return showDeleteModeratorGroupDialog;
     }
 
     /**
      * @return groupService instance for creating comboboxes
      */
     public GroupService getGroupService() {
         return groupService;
     }
 
     /**
      * Gets List of groups which shown at UI.
      *
      * @return Groups currently displayed at UI
      */
     public ListModelList<Group> getGroups() {
         updateView();
         return groups;
     }
 
     /**
      * Gets current selected {@link Group}.
      *
      * @return Group selected at UI
      */
     public Group getSelectedGroup() {
         return selectedGroup;
     }
 
     /**
      * Sets current selected {@link Group}.
      *
      * @param group selected at UI
      */
     public void setSelectedGroup(Group group) {
         this.selectedGroup = group;
     }
 
     /**
      * Sets List of groups which shown at UI.
      *
      * @param groups selected at UI
      */
     public void setGroups(ListModelList<Group> groups) {
         this.groups = groups;
     }
 
     /**
      * Sets Search string, used for group search.
      *
      * @param searchString string used for group search
      */
     public void setSearchString(String searchString) {
         this.searchString = searchString;
     }
 
     /**
      * @param showDeleteModeratorGroupDialog determine visible status of moderator group delete dialog part
      *                                       of group delete dialog
      */
     public void setShowDeleteModeratorGroupDialog(boolean showDeleteModeratorGroupDialog) {
         this.showDeleteModeratorGroupDialog = showDeleteModeratorGroupDialog;
     }
 
     /**
      * @return currently selected moderator {@link Group} for {@link PoulpeBranch}es shown at delete dialog
      */
     public Group getSelectedModeratorGroup() {
         return selectedModeratorGroup;
     }
 
     /**
      * @param selectedModeratorGroup {@link Group} currently selected moderator group
      */
     public void setSelectedModeratorGroup(Group selectedModeratorGroup) {
         this.selectedModeratorGroup = selectedModeratorGroup;
     }
 
     /**
      * @return list of branches moderated by group to delete, used as model for {@link org.zkoss.zul.Listbox}
      *         at delete group dialog
      */
     public List<PoulpeBranch> getBranches() {
         return branches;
     }
 
     /**
      * @param branches {@link PoulpeBranch} list of branches, moderated by {@link Group} to delete
      */
     public void setBranches(List<PoulpeBranch> branches) {
         this.branches = branches;
     }
 
     /**
      * @return map of current pairs branch - moderator group, selected by user at delete moderator group
      *         dialog
      */
     public HashMap<PoulpeBranch, Group> getBranchesMap() {
         return branchesMap;
     }
 
     /**
      * @param bindWrapper instance of bindWrapper
      */
     public void setBindWrapper(BindUtilsWrapper bindWrapper) {
         this.bindWrapper = bindWrapper;
     }
 }
