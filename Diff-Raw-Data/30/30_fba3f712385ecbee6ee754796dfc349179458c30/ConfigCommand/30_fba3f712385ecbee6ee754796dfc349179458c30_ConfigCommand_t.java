 /******************************************************************************
  * Multiverse 2 Copyright (c) the Multiverse Team 2011.                       *
  * Multiverse 2 is licensed under the BSD License.                            *
  * For more information please check the README.md file included              *
  * with this project.                                                         *
  ******************************************************************************/
 
 package com.onarandombox.MultiverseCore.commands;
 
 import com.onarandombox.MultiverseCore.MultiverseCore;
 import com.onarandombox.MultiverseCore.api.MVWorldManager;
 import com.onarandombox.MultiverseCore.enums.ConfigProperty;
 import org.bukkit.ChatColor;
 import org.bukkit.command.CommandSender;
 import org.bukkit.permissions.PermissionDefault;
 
 import java.util.List;
 
 public class ConfigCommand extends MultiverseCommand {
     private MVWorldManager worldManager;
 
     public ConfigCommand(MultiverseCore plugin) {
         super(plugin);
         this.setName("Configuration");
         this.setCommandUsage("/mv config " + ChatColor.GREEN + "{PROPERTY} {VALUE}");
         this.setArgRange(2, 2);
         this.addKey("mv config");
         this.addKey("mvconfig");
         this.addKey("mv conf");
         this.addKey("mvconf");
         this.addCommandExample("/mv config " + ChatColor.GREEN + "debug" + ChatColor.AQUA + " 3");
         this.addCommandExample("/mv config " + ChatColor.GREEN + "enforceaccess" + ChatColor.AQUA + " false");
         this.addCommandExample("/mv config " + ChatColor.GREEN + "bedrespawn" + ChatColor.AQUA + " true");
         this.setPermission("multiverse.core.config", "Allows you to set Global MV Variables.", PermissionDefault.OP);
         this.worldManager = this.plugin.getMVWorldManager();
     }
 
     @Override
     public void runCommand(CommandSender sender, List<String> args) {
         if (args.get(0).equalsIgnoreCase("messagecooldown") || args.get(0).equalsIgnoreCase("teleportcooldown") || args.get(0).equalsIgnoreCase("debug")) {
             try {
                 this.plugin.getMVConfiguration().set(args.get(0).toLowerCase(), Integer.parseInt(args.get(1)));
             } catch (NumberFormatException e) {
                 sender.sendMessage(ChatColor.RED + "Sorry, " + ChatColor.AQUA + args.get(0) + ChatColor.WHITE + " must be an integer!");
                 return;
             }
         } else {
            ConfigProperty property = null;
            try {
                property = ConfigProperty.valueOf(args.get(0).toLowerCase());
            } catch (IllegalArgumentException e) {
                sender.sendMessage(ChatColor.RED + "Sorry, " + ChatColor.AQUA + args.get(0) + ChatColor.WHITE + " you can't set " + ChatColor.AQUA + args.get(0));
                sender.sendMessage(ChatColor.GREEN + "Valid values are:");
                sender.sendMessage(ConfigProperty.getAllValues());
                return;
            }

            if (property != null) {
                 try {
                     this.plugin.getMVConfiguration().set(args.get(0).toLowerCase(), Boolean.parseBoolean(args.get(0)));
                 } catch (Exception e) {
                     sender.sendMessage(ChatColor.RED + "Sorry, " + ChatColor.AQUA + args.get(0) + ChatColor.WHITE + " must be true or false!");
                     return;
                 }
 
             }
         }
         if (this.plugin.saveMVConfigs()) {
             sender.sendMessage(ChatColor.GREEN + "SUCCESS!" + ChatColor.WHITE + " Values were updated successfully!");
             this.plugin.loadConfigs();
         } else {
             sender.sendMessage(ChatColor.RED + "FAIL!" + ChatColor.WHITE + " Check your console for details!");
         }
     }
 }
