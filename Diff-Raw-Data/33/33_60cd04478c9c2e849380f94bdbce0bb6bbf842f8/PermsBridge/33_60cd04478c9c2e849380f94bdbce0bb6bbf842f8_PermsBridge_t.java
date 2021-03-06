 package com.titankingdoms.dev.TitanIRC;
 
 import org.bukkit.entity.Player;
 import org.bukkit.event.EventHandler;
 import org.bukkit.event.Listener;
 import org.bukkit.event.server.PluginEnableEvent;
 import org.bukkit.plugin.Plugin;
 
 /**
 * Copyright (C) 2012 Chris Ward
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
  */
 public class PermsBridge implements Listener {
     private TitanIRC instance;
     public PermsBridge(TitanIRC instance)
     {
         this.instance = instance;
         this.instance = instance;
     }
     public static String PermissionsPluginName = null;
     public static Plugin PermissionsPlugin = null;
 
     @EventHandler
     public void onPluginEnable(PluginEnableEvent e)
     {
         if(PermissionsPluginName == null || PermissionsPluginName == "Vault")
         {
             if(e.getPlugin() instanceof de.bananaco.bpermissions.imp.Permissions)
             {
                 PermissionsPluginName = "bPermissions";
             }
             else if(e.getPlugin() instanceof org.anjocaido.groupmanager.GroupManager)
             {
                 PermissionsPluginName = "GroupManager";
             }
             else if(e.getPlugin() instanceof com.platymuus.bukkit.permissions.PermissionsPlugin)
             {
                 PermissionsPluginName = "PermissionsBukkit";
             }
             else if(e.getPlugin() instanceof ru.tehkode.permissions.bukkit.PermissionsEx)
             {
                 PermissionsPluginName = "PermissionsEx";
             }
             else if(e.getPlugin() instanceof org.tyrannyofheaven.bukkit.zPermissions.ZPermissionsPlugin)
             {
                 PermissionsPluginName = "zPermissions";
             }
             else if(e.getPlugin() instanceof net.milkbowl.vault.Vault && PermissionsPluginName == null)
             {
                 PermissionsPluginName = "Vault";
             }
             else
             {
                 return;
             }
             PermissionsPlugin = e.getPlugin();
             instance.debug("Hooked in to " + PermissionsPluginName);
         }
     }
 
     public String getPrefix(Player player)
     {
         if(PermissionsPluginName.equals("GroupManager"))
             return ((org.anjocaido.groupmanager.GroupManager)PermissionsPlugin).getWorldsHolder().getWorldData(player.getWorld().getName()).getUser(player.getName()).getVariables().getVarString("prefix");
         else if(PermissionsPluginName.equals("bPermissions"))
             return de.bananaco.bpermissions.api.WorldManager.getInstance().getWorld(player.getWorld().getName()).getGroup(player.getName()).getMeta().get("prefix");
         return null;
     }
 
     public String getSuffix(Player player)
     {
         if(PermissionsPluginName.equals("GroupManager"))
             return ((org.anjocaido.groupmanager.GroupManager)PermissionsPlugin).getWorldsHolder().getWorldData(player.getWorld().getName()).getUser(player.getName()).getVariables().getVarString("suffix");
         else if(PermissionsPluginName.equals("bPermissions"))
             return de.bananaco.bpermissions.api.WorldManager.getInstance().getWorld(player.getWorld().getName()).getGroup(player.getName()).getMeta().get("sufffix");
         return null;
     }
 
     public String formatPlayerName(Player player)
     {
         return getPrefix(player) + player.getDisplayName() + getSuffix(player);
     }
 
 
 }
