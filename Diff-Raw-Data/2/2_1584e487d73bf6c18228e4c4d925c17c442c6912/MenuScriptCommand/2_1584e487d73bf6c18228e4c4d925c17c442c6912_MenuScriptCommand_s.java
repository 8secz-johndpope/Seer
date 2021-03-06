 /*
  * To change this template, choose Tools | Templates
  * and open the template in the editor.
  */
 package me.xhawk87.CreateYourOwnMenus.commands.menu;
 
 import java.util.Arrays;
 import java.util.HashMap;
 import java.util.Map;
 import me.xhawk87.CreateYourOwnMenus.CreateYourOwnMenus;
 import me.xhawk87.CreateYourOwnMenus.commands.IMenuCommand;
 import me.xhawk87.CreateYourOwnMenus.commands.menu.script.MenuScriptAppendCommand;
 import me.xhawk87.CreateYourOwnMenus.commands.menu.script.MenuScriptClearCommand;
 import me.xhawk87.CreateYourOwnMenus.commands.menu.script.MenuScriptDeleteCommand;
 import me.xhawk87.CreateYourOwnMenus.commands.menu.script.MenuScriptHideCommand;
 import me.xhawk87.CreateYourOwnMenus.commands.menu.script.MenuScriptInsertCommand;
 import me.xhawk87.CreateYourOwnMenus.commands.menu.script.MenuScriptReplaceCommand;
 import me.xhawk87.CreateYourOwnMenus.commands.menu.script.MenuScriptShowCommand;
 import org.bukkit.command.Command;
 import org.bukkit.command.CommandSender;
 
 /**
  *
  * @author XHawk87
  */
 public class MenuScriptCommand implements IMenuCommand {
 
     /**
      * All subcommands of the menu command, stored by their name
      */
     private Map<String, IMenuCommand> subCommands = new HashMap<>();
 
     public MenuScriptCommand(CreateYourOwnMenus plugin) {
         subCommands.put("clear", new MenuScriptClearCommand());
         subCommands.put("show", new MenuScriptShowCommand());
         subCommands.put("hide", new MenuScriptHideCommand());
         subCommands.put("append", new MenuScriptAppendCommand());
         subCommands.put("insert", new MenuScriptInsertCommand());
         subCommands.put("replace", new MenuScriptReplaceCommand());
         subCommands.put("delete", new MenuScriptDeleteCommand());
         
         // Aliases
         subCommands.put("add", subCommands.get("append"));
         subCommands.put("remove", subCommands.get("delete"));
         subCommands.put("set", subCommands.get("replace"));
         subCommands.put("commands", subCommands.get("show"));
         subCommands.put("comments", subCommands.get("hide"));
         subCommands.put("reset", subCommands.get("clear"));
     }
 
     @Override
     public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
         // It is assumed that entering the menu command without parameters is an
         // attempt to get information about it. So let's give it to them.
         if (args.length == 0) {
             for (IMenuCommand menuCommand : subCommands.values()) {
                 String permission = menuCommand.getPermission();
                 if (permission != null && sender.hasPermission(permission)) {
                     sender.sendMessage(menuCommand.getUsage());
                 }
             }
             return true;
         }
 
         String subCommandName = args[0];
         IMenuCommand menuScriptCommand = subCommands.get(subCommandName.toLowerCase());
         if (menuScriptCommand == null) {
             return false; // They mistyped or entered an invalid subcommand
         }
         // Handle the permissions check
         String permission = menuScriptCommand.getPermission();
         if (permission != null && !sender.hasPermission(permission)) {
             sender.sendMessage("You do not have permission to use this command");
             return true;
         }
         // Remove the sub-command from the args list and pass along the rest
         if (!menuScriptCommand.onCommand(sender, command, label, Arrays.copyOfRange(args, 1, args.length))) {
             // A sub-command returning false should display the usage information for that sub-command
             sender.sendMessage(menuScriptCommand.getUsage());
         }
         return true;
     }
 
     @Override
     public String getUsage() {
        return "/menu script [clear|command] - Add a command to a held menu item or clear all commands from it";
     }
 
     @Override
     public String getPermission() {
         return null;
     }
 }
