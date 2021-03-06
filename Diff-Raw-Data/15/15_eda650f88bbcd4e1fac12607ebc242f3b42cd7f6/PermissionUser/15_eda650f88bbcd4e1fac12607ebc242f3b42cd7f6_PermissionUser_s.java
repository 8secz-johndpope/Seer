 /*
  * PermissionsEx - Permissions plugin for Bukkit
  * Copyright (C) 2011 t3hk0d3 http://www.tehkode.ru
  *
  * This program is free software; you can redistribute it and/or
  * modify it under the terms of the GNU General Public License
  * as published by the Free Software Foundation; either version 2
  * of the License, or (at your option) any later version.
  *
  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  * GNU General Public License for more details.
  *
  * You should have received a copy of the GNU General Public License
  * along with this program; if not, write to the Free Software
  * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
  */
 package ru.tehkode.permissions;
 
 import java.util.Arrays;
 import java.util.HashMap;
 import java.util.LinkedHashSet;
 import java.util.LinkedList;
 import java.util.List;
 import java.util.Set;
 
 /**
  *
  * @author code
  */
 public abstract class PermissionUser extends PermissionEntity {
 
     protected PermissionGroup[] cachedGroups = null;
    protected String[] cachedPermissions = null;
     protected String cachedPrefix = null;
     protected String cachedSuffix = null;
     protected HashMap<String, String> cachedAnwsers = new HashMap<String, String>();
 
     public PermissionUser(String playerName, PermissionManager manager) {
         super(playerName, manager);
     }
 
     public boolean inGroup(PermissionGroup group) {
         return this.inGroup(group.getName());
     }
 
     public boolean inGroup(String groupName) {
         for (String matchingGroupName : this.getGroupsNames()) {
             if (groupName.equalsIgnoreCase(matchingGroupName)) {
                 return true;
             }
         }
 
         return false;
     }
 
     public PermissionGroup[] getGroups() {
         if (this.cachedGroups == null) {
             Set<PermissionGroup> groups = new LinkedHashSet<PermissionGroup>();
 
             for (String group : this.getGroupsNamesImpl()) {
                 groups.add(this.manager.getGroup(group.trim()));
             }
 
             if (groups.isEmpty()) {
                 groups.add(this.manager.getDefaultGroup());
             }
 
             this.cachedGroups = groups.toArray(new PermissionGroup[]{});
         }
 
         return this.cachedGroups;
     }
 
     public String[] getGroupsNames() {
         List<String> groups = new LinkedList<String>();
         for (PermissionGroup group : this.getGroups()) {
             groups.add(group.getName());
         }
 
         return groups.toArray(new String[0]);
     }
 
     public abstract String[] getOwnPermissions(String world);
 
     @Override
     public String[] getPermissions(String world) {
        if (this.cachedPermissions == null) {
             List<String> permissions = new LinkedList<String>();
             this.getInheritedPermissions(world, permissions);
 
            this.cachedPermissions = permissions.toArray(new String[0]);
         }
 
        return this.cachedPermissions;
     }
 
     protected void getInheritedPermissions(String world, List<String> permissions) {
         permissions.addAll(Arrays.asList(this.getOwnPermissions(world)));
 
         for (PermissionGroup group : this.getGroups()) {
             group.getInheritedPermissions(world, permissions);
         }
     }
 
     public void addGroup(String groupName) {
         if (groupName == null || groupName.isEmpty()) {
             return;
         }
 
         this.addGroup(this.manager.getGroup(groupName));
     }
 
     public void addGroup(PermissionGroup group) {
         if (group == null) {
             return;
         }
 
         List<PermissionGroup> groups = new LinkedList<PermissionGroup>(Arrays.asList(this.getGroups()));
 
         if (this.getGroupsNamesImpl().length == 0 && groups.size() == 1 && groups.contains(this.manager.getDefaultGroup())) {
             groups.clear(); // clean out default group
         }
 
         if (group.isVirtual()) {
             group.save();
         }
 
         if (!groups.contains(group)) {
             groups.add(group);
             this.clearCache();
             this.setGroups(groups.toArray(new PermissionGroup[0]));
         }
     }
 
     public void removeGroup(String groupName) {
         if (groupName == null || groupName.isEmpty()) {
             return;
         }
 
         this.removeGroup(this.manager.getGroup(groupName));
 
         this.clearCache();
     }
 
     public void removeGroup(PermissionGroup group) {
         if (group == null) {
             return;
         }
 
         List<PermissionGroup> groups = Arrays.asList(this.getGroups());
 
         if (groups.contains(group)) {
             groups.remove(group);
             this.clearCache();
             this.setGroups(groups.toArray(new PermissionGroup[]{}));
         }
     }
 
     @Override
     public String getPrefix() {
         if (this.cachedPrefix == null) {
             String prefix = super.getPrefix();
             if (prefix == null || prefix.isEmpty()) {
                 for (PermissionGroup group : this.getGroups()) {
                     prefix = group.getPrefix();
                     if (prefix != null && !prefix.isEmpty()) {
                         break;
                     }
                 }
             }
 
             if (prefix == null) { // just for NPE safety
                 prefix = "";
             }
 
             this.cachedPrefix = prefix;
         }
 
         return this.cachedPrefix;
     }
 
     @Override
     public String getSuffix() {
         if (this.cachedSuffix == null) {
             String suffix = super.getSuffix();
             if (suffix == null || suffix.isEmpty()) {
                 for (PermissionGroup group : this.getGroups()) {
                     suffix = group.getSuffix();
                     if (suffix != null && !suffix.isEmpty()) {
                         break;
                     }
                 }
             }
 
             if (suffix == null) { // just for NPE safety
                 suffix = "";
             }
             this.cachedSuffix = suffix;
         }
 
         return this.cachedSuffix;
     }
 
     public abstract void setGroups(PermissionGroup[] groups);
 
     protected abstract String[] getGroupsNamesImpl();
 
     @Override
     protected String getMatchingExpression(String permission, String world) {
         String cacheId = world + ":" + permission;
         if (!this.cachedAnwsers.containsKey(cacheId)) {
             this.cachedAnwsers.put(cacheId, super.getMatchingExpression(permission, world));
         }
         
         return this.cachedAnwsers.get(cacheId);
     }
 
     protected void clearCache() {
         this.cachedGroups = null;
        this.cachedPermissions = null;
         this.cachedPrefix = null;
         this.cachedSuffix = null;
 
         this.cachedAnwsers.clear();
     }
 
     public String getOwnPrefix() {
         return this.prefix;
     }
 
     public String getOwnSuffix() {
         return this.suffix;
     }
 
     @Override
     public void setPrefix(String prefix) {
         super.setPrefix(prefix);
         this.clearCache();
     }
 
     @Override
     public void setSuffix(String postfix) {
         super.setSuffix(postfix);
         this.clearCache();
     }
 }
