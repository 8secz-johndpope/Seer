 /**
  * Copyright (C) 2011 Morgan Humes <morgan@lanaddict.com>
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
  * 
  */
 
 package net.milkbowl.vault.permission.plugins;
 
 import net.milkbowl.vault.Vault;
 import net.milkbowl.vault.permission.Permission;
 
 import org.bukkit.entity.Player;
 import org.bukkit.event.Event.Priority;
 import org.bukkit.event.Event.Type;
 import org.bukkit.event.server.PluginDisableEvent;
 import org.bukkit.event.server.PluginEnableEvent;
 import org.bukkit.event.server.ServerListener;
 import org.bukkit.plugin.Plugin;
 import org.bukkit.plugin.PluginManager;
 
 import ru.tehkode.permissions.PermissionGroup;
 import ru.tehkode.permissions.PermissionUser;
 import ru.tehkode.permissions.bukkit.PermissionsEx;
 
 public class Permission_PermissionsEx extends Permission {
 
     private final String name = "PermissionsEx";
     private PluginManager pluginManager = null;
     private PermissionsEx permission = null;
     private PermissionServerListener permissionServerListener = null;
 
     public Permission_PermissionsEx(Vault plugin) {
         this.plugin = plugin;
         pluginManager = this.plugin.getServer().getPluginManager();
 
         permissionServerListener = new PermissionServerListener(this);
 
         this.pluginManager.registerEvent(Type.PLUGIN_ENABLE, permissionServerListener, Priority.Monitor, plugin);
         this.pluginManager.registerEvent(Type.PLUGIN_DISABLE, permissionServerListener, Priority.Monitor, plugin);
 
         // Load Plugin in case it was loaded before
         if (permission == null) {
             Plugin perms = plugin.getServer().getPluginManager().getPlugin("PermissionsEx");
             if (perms != null) {
                 if (perms.isEnabled()) {
                    try {
                        if (Double.valueOf(perms.getDescription().getVersion()) < 1.16) {
                            log.info(String.format("[%s][Permission] %s below 1.16 is not compatible with Vault! Falling back to SuperPerms only mode. PLEASE UPDATE!", plugin.getDescription().getName(), name));
                            return;
                        }
                    } catch (NumberFormatException e) {
                        // Do nothing
                    }
                     permission = (PermissionsEx) perms;
                     log.info(String.format("[%s][Permission] %s hooked.", plugin.getDescription().getName(), name));
                 }
             }
         }
     }
 
     @Override
     public boolean isEnabled() {
         if (permission == null) {
             return false;
         } else {
             return permission.isEnabled();
         }
     }
 
     @Override
     public boolean has(Player player, String permission) {
         return playerHas(player, permission);
     }
 
     @Override
     public boolean playerHas(Player player, String permission) {
         return this.permission.has(player, permission);
     }
 
     @Override
     public boolean playerInGroup(String worldName, String playerName, String groupName) {
     	return PermissionsEx.getPermissionManager().getUser(playerName).inGroup(groupName);
     }
 
     private class PermissionServerListener extends ServerListener {
         Permission_PermissionsEx permission = null;
 
         public PermissionServerListener(Permission_PermissionsEx permission) {
             this.permission = permission;
         }
 
         public void onPluginEnable(PluginEnableEvent event) {
             if (permission.permission == null) {
                 Plugin perms = plugin.getServer().getPluginManager().getPlugin("PermissionsEx");
 
                 if (perms != null) {
                     if (perms.isEnabled()) {
                        try {
                            if (Double.valueOf(perms.getDescription().getVersion()) < 1.16) {
                                log.info(String.format("[%s][Permission] %s below 1.16 is not compatible with Vault! Falling back to SuperPerms only mode. PLEASE UPDATE!", plugin.getDescription().getName(), name));
                                return;
                            }
                        } catch (NumberFormatException e) {
                            // Do nothing
                        }
                         permission.permission = (PermissionsEx) perms;
                         log.info(String.format("[%s][Permission] %s hooked.", plugin.getDescription().getName(), permission.name));
                     }
                 }
             }
         }
 
         public void onPluginDisable(PluginDisableEvent event) {
             if (permission.permission != null) {
                 if (event.getPlugin().getDescription().getName().equals("PermissionsEx")) {
                     permission.permission = null;
                     log.info(String.format("[%s][Permission] %s un-hooked.", plugin.getDescription().getName(), permission.name));
                 }
             }
         }
     }
 
     @Override
     public String getName() {
         return name;
     }
 
     @Override
     public boolean playerAddGroup(String worldName, String playerName, String groupName) {
         PermissionGroup group = PermissionsEx.getPermissionManager().getGroup(groupName);
         PermissionUser user = PermissionsEx.getPermissionManager().getUser(playerName);
         if (group == null || user == null) {
             return false;
         } else {
             user.addGroup(group);
             return true;
         }
     }
 
     @Override
     public boolean playerRemoveGroup(String worldName, String playerName, String groupName) {
     	PermissionsEx.getPermissionManager().getUser(playerName).removeGroup(groupName);
     	return true;
     }
 
     @Override
     public boolean playerAdd(String worldName, String playerName, String permission) {
         PermissionUser user = PermissionsEx.getPermissionManager().getUser(playerName);
         if (user == null) {
             return false;
         } else {
             user.addPermission(permission, worldName);
             return true;
         }
     }
 
     @Override
     public boolean playerRemove(String worldName, String playerName, String permission) {
         PermissionUser user = PermissionsEx.getPermissionManager().getUser(playerName);
         if (user == null) {
             return false;
         } else {
             user.removePermission(permission, worldName);
             return true;
         }
     }
 
     @Override
     public boolean groupAdd(String worldName, String groupName, String permission) {
         PermissionGroup group = PermissionsEx.getPermissionManager().getGroup(groupName);
         if (group == null) {
             return false;
         } else {
             group.addPermission(permission, worldName);
             return true;
         }
     }
 
     @Override
     public boolean groupRemove(String worldName, String groupName, String permission) {
         PermissionGroup group = PermissionsEx.getPermissionManager().getGroup(groupName);
         if (group == null) {
             return false;
         } else {
             group.removePermission(permission, worldName);
             return true;
         }
     }
 
     @Override
     public boolean groupHas(String worldName, String groupName, String permission) {
         PermissionGroup group = PermissionsEx.getPermissionManager().getGroup(groupName);
         if (group == null) {
             return false;
         } else {
             return group.has(permission, worldName);
         }
     }
 
     @Override
     public String[] getPlayerGroups(String world, String playerName) {
         return PermissionsEx.getPermissionManager().getUser(playerName).getGroupsNames();
     }
 
     @Override
     public String getPrimaryGroup(String world, String playerName) {
         if (PermissionsEx.getPermissionManager().getUser(playerName).getGroupsNames().length > 0)
             return PermissionsEx.getPermissionManager().getUser(playerName).getGroupsNames()[0];
         else
             return null;
     }
 
     @Override
     public boolean playerHas(String worldName, String playerName, String permission) {
         PermissionUser user = PermissionsEx.getPermissionManager().getUser(playerName);
         if (user != null) {
             return user.has(permission, worldName);
         } else {
             return false;
         }
     }
     
     @Override
     public boolean playerAddTransient(String worldName, String player, String permission) {
         PermissionUser pPlayer = PermissionsEx.getPermissionManager().getUser(player);
         if (pPlayer != null) {
             pPlayer.addTimedPermission(permission, worldName, 0);
             return true;
         } else {
             return false;
         }
     }
     
     @Override
     public boolean playerAddTransient(String worldName, Player player, String permission) {
     	return playerAddTransient(worldName, player.getName(), permission);
     }
     
     @Override
     public boolean playerAddTransient(String player, String permission) {
     	return playerAddTransient(null, player, permission);
     }
     
     @Override
     public boolean playerAddTransient(Player player, String permission) {
     	return playerAddTransient(null, player.getName(), permission);
     }
 
     @Override
     public boolean playerRemoveTransient(String worldName, String player, String permission) {
 		PermissionUser pPlayer = PermissionsEx.getPermissionManager().getUser(player);
 		if (pPlayer != null) {
 			pPlayer.removeTimedPermission(permission, worldName);
 			return true;
 		} else {
 			return false;
 		}
     }
     
     @Override
     public boolean playerRemoveTransient(Player player, String permission) {
     	return playerRemoveTransient(null, player.getName(), permission);
     }
     
     @Override
     public boolean playerRemoveTransient(String worldName, Player player, String permission) {
     	return playerRemoveTransient(worldName, player.getName(), permission);
     }
     
 	@Override
 	public boolean playerRemoveTransient(String player, String permission) {
 		return playerRemoveTransient(null, player, permission);
 	}
 
 	@Override
 	public String[] getGroups() {
 		PermissionGroup[] groups = PermissionsEx.getPermissionManager().getGroups();
 		if (groups == null || groups.length == 0)
 			return null;
 		String[] groupNames = new String[groups.length];
 		for (int i = 0; i < groups.length; i++) {
 			groupNames[i] = groups[i].getName();
 		}
 		return groupNames;
 	}
 
 	@Override
 	public boolean hasSuperPermsCompat() {
 		return true;
 	}
 }
