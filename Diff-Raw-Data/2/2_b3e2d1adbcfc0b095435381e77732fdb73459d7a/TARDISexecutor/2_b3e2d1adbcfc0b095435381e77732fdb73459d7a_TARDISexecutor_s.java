 package me.eccentric_nz.plugins.TARDIS;
 
 import java.io.BufferedReader;
 import java.io.File;
 import java.io.FileReader;
 import java.io.IOException;
 import java.sql.Connection;
 import java.sql.ResultSet;
 import java.sql.SQLException;
 import java.sql.Statement;
 import java.util.Arrays;
 import java.util.List;
 import java.util.Set;
 import org.apache.commons.lang.StringUtils;
 import org.bukkit.*;
 import org.bukkit.command.Command;
 import org.bukkit.command.CommandExecutor;
 import org.bukkit.command.CommandSender;
 import org.bukkit.entity.Player;
 
 public class TARDISexecutor implements CommandExecutor {
 
     private TARDIS plugin;
     TARDISdatabase service = TARDISdatabase.getInstance();
 
     public TARDISexecutor(TARDIS plugin) {
         this.plugin = plugin;
     }
 
     @Override
     public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
         // If the player typed /tardis then do the following...
         // check there is the right number of arguments
         if (cmd.getName().equalsIgnoreCase("tardis")) {
             Player player = null;
             if (sender instanceof Player) {
                 player = (Player) sender;
             }
             if (args.length == 0) {
                 sender.sendMessage(Constants.COMMANDS.split("\n"));
                 return true;
             }
             // the command list - first argument MUST appear here!
             if (!args[0].equalsIgnoreCase("save") && !args[0].equalsIgnoreCase("list") && !args[0].equalsIgnoreCase("admin") && !args[0].equalsIgnoreCase("help") && !args[0].equalsIgnoreCase("find") && !args[0].equalsIgnoreCase("reload") && !args[0].equalsIgnoreCase("add") && !args[0].equalsIgnoreCase("remove") && !args[0].equalsIgnoreCase("update")) {
                 sender.sendMessage("Do you want to list destinations, save a destination, update the TARDIS, add/remove companions, do some admin stuff or find the TARDIS?");
                 return false;
             }
             if (args[0].equalsIgnoreCase("reload")) {
                 plugin.loadConfig();
                 sender.sendMessage("TARDIS config reloaded.");
             }
             if (args[0].equalsIgnoreCase("admin")) {
                 if (args.length == 1) {
                     sender.sendMessage(Constants.COMMAND_ADMIN.split("\n"));
                     return true;
                 }
                 if (args[1].equalsIgnoreCase("update")) {
                     // put timelords to tardis table
                     Set<String> timelords = plugin.timelords.getKeys(false);
                     for (String p : timelords) {
                         if (!p.equals("dummy_user")) {
                             String c = plugin.timelords.getString(p + ".chunk");
                             String d = plugin.timelords.getString(p + ".direction");
                             String h = plugin.timelords.getString(p + ".home");
                             String s = plugin.timelords.getString(p + ".save");
                             String cur = plugin.timelords.getString(p + ".current");
                             String r = plugin.timelords.getString(p + ".replaced");
                             String chest = plugin.timelords.getString(p + ".chest");
                             String b = plugin.timelords.getString(p + ".button");
                             String r0 = plugin.timelords.getString(p + ".repeater0");
                             String r1 = plugin.timelords.getString(p + ".repeater1");
                             String r2 = plugin.timelords.getString(p + ".repeater2");
                             String r3 = plugin.timelords.getString(p + ".repeater3");
                             String s1 = plugin.timelords.getString(p + ".save1");
                             String s2 = plugin.timelords.getString(p + ".save2");
                             String s3 = plugin.timelords.getString(p + ".save3");
                             String t = plugin.timelords.getString(p + ".travelling");
                             try {
                                 service.getConnection();
                                 service.insertTimelords(p, c, d, h, s, cur, r, chest, b, r0, r1, r2, r3, s1, s2, s3, t);
                             } catch (Exception e) {
                                 System.err.println(Constants.MY_PLUGIN_NAME + " Timelords to DB Error: " + e);
                             }
                         }
                     }
                     // put chunks to chunks table
                     BufferedReader br = null;
                     List<World> worldList = plugin.getServer().getWorlds();
                     for (World w : worldList) {
                         String strWorldName = w.getName();
                         File chunkFile = new File(plugin.getDataFolder() + File.separator + "chunks" + File.separator + strWorldName + ".chunks");
                         if (chunkFile.exists() && w.getEnvironment() == World.Environment.NORMAL) {
                             // read file
                             try {
                                 br = new BufferedReader(new FileReader(chunkFile));
                                 String str;
                                 int cx = 0, cz = 0;
                                 while ((str = br.readLine()) != null) {
                                     String[] chunkData = str.split(":");
                                     try {
                                         cx = Integer.parseInt(chunkData[1]);
                                         cz = Integer.parseInt(chunkData[2]);
                                     } catch (NumberFormatException nfe) {
                                         System.err.println(Constants.MY_PLUGIN_NAME + " Could not convert to number!");
                                     }
                                     try {
                                         service.getConnection();
                                         service.insertChunks(chunkData[0], cx, cz);
                                     } catch (Exception e) {
                                         System.err.println(Constants.MY_PLUGIN_NAME + " Chunk File to DB Error: " + e);
                                     }
                                 }
                             } catch (IOException io) {
                                 System.err.println(Constants.MY_PLUGIN_NAME + " could not create [" + strWorldName + "] world chunk file!");
                             }
                         }
                     }
                     sender.sendMessage(ChatColor.GRAY + Constants.MY_PLUGIN_NAME + ChatColor.RESET + " The config files were successfully inserted into the database.");
                     return true;
                 }
                 if (args.length < 3) {
                     sender.sendMessage("Too few command arguments!");
                     return false;
                 } else {
                     if (!args[1].equalsIgnoreCase("bonus") && !args[1].equalsIgnoreCase("protect") && !args[1].equalsIgnoreCase("max_rad") && !args[1].equalsIgnoreCase("spout") && !args[1].equalsIgnoreCase("default") && !args[1].equalsIgnoreCase("name") && !args[1].equalsIgnoreCase("include") && !args[1].equalsIgnoreCase("key") && !args[1].equalsIgnoreCase("update") && !args[1].equalsIgnoreCase("exclude") && !args[1].equalsIgnoreCase("platform")) {
                         sender.sendMessage(ChatColor.GRAY + Constants.MY_PLUGIN_NAME + ChatColor.RESET + " TARDIS does not recognise that command argument!");
                         return false;
                     }
 
                     if (args[1].equalsIgnoreCase("key")) {
                         String setMaterial = args[2].toUpperCase();
                         if (!Arrays.asList(Materials.MATERIAL_LIST).contains(setMaterial)) {
                             sender.sendMessage(ChatColor.GRAY + Constants.MY_PLUGIN_NAME + ChatColor.RED + "That is not a valid Material! Try checking http://jd.bukkit.org/apidocs/org/bukkit/Material.html");
                             return false;
                         } else {
                             plugin.config.set("key", setMaterial);
                             Constants.TARDIS_KEY = setMaterial;
                         }
                     }
                     if (args[1].equalsIgnoreCase("bonus")) {
                         String tf = args[2].toLowerCase();
                         if (!tf.equals("true") && !tf.equals("false")) {
                             sender.sendMessage(ChatColor.GRAY + Constants.MY_PLUGIN_NAME + ChatColor.RED + "The last argument must be true or false!");
                             return false;
                         }
                         plugin.config.set("bonus_chest", Boolean.valueOf(tf));
                     }
                     if (args[1].equalsIgnoreCase("protect")) {
                         String tf = args[2].toLowerCase();
                         if (!tf.equals("true") && !tf.equals("false")) {
                             sender.sendMessage(ChatColor.GRAY + Constants.MY_PLUGIN_NAME + ChatColor.RED + "The last argument must be true or false!");
                             return false;
                         }
                         plugin.config.set("protect_blocks", Boolean.valueOf(tf));
                     }
                     if (args[1].equalsIgnoreCase("platform")) {
                         String tf = args[2].toLowerCase();
                         if (!tf.equals("true") && !tf.equals("false")) {
                             sender.sendMessage(ChatColor.GRAY + Constants.MY_PLUGIN_NAME + ChatColor.RED + "The last argument must be true or false!");
                             return false;
                         }
                         plugin.config.set("platform", Boolean.valueOf(tf));
                     }
                     if (args[1].equalsIgnoreCase("max_rad")) {
                         String a = args[2];
                         int val;
                         try {
                             val = Integer.parseInt(a);
                         } catch (NumberFormatException nfe) {
                             // not a number
                             sender.sendMessage(ChatColor.GRAY + Constants.MY_PLUGIN_NAME + ChatColor.RED + " The last argument must be a number!");
                             return false;
                         }
                         plugin.config.set("tp_radius", val);
                     }
                     if (args[1].equalsIgnoreCase("spout")) {
                         // check they typed true of false
                         String tf = args[2].toLowerCase();
                         if (!tf.equals("true") && !tf.equals("false")) {
                             sender.sendMessage(ChatColor.GRAY + Constants.MY_PLUGIN_NAME + ChatColor.RED + "The last argument must be true or false!");
                             return false;
                         }
                         plugin.config.set("require_spout", Boolean.valueOf(tf));
                     }
                     if (args[1].equalsIgnoreCase("default")) {
                         // check they typed true of false
                         String tf = args[2].toLowerCase();
                         if (!tf.equals("true") && !tf.equals("false")) {
                             sender.sendMessage(ChatColor.GRAY + Constants.MY_PLUGIN_NAME + ChatColor.RED + "The last argument must be true or false!");
                             return false;
                         }
                         plugin.config.set("default_world", Boolean.valueOf(tf));
                     }
                     if (args[1].equalsIgnoreCase("name")) {
                         // get world name
                         int count = args.length;
                         StringBuilder buf = new StringBuilder();
                         for (int i = 2; i < count; i++) {
                             buf.append(args[i]).append(" ");
                         }
                         String tmp = buf.toString();
                         String t = tmp.substring(0, tmp.length() - 1);
                         // need to make there are no periods(.) in the text
                         String nodots = StringUtils.replace(t, ".", "_");
                         plugin.config.set("default_world_name", nodots);
                     }
                     if (args[1].equalsIgnoreCase("include")) {
                         // check they typed true of false
                         String tf = args[2].toLowerCase();
                         if (!tf.equals("true") && !tf.equals("false")) {
                             sender.sendMessage(ChatColor.GRAY + Constants.MY_PLUGIN_NAME + ChatColor.RED + "The last argument must be true or false!");
                             return false;
                         }
                         plugin.config.set("include_default_world", Boolean.valueOf(tf));
                     }
                     if (args[1].equalsIgnoreCase("exclude")) {
                         // get world name
                         int count = args.length;
                         StringBuilder buf = new StringBuilder();
                         for (int i = 2; i < count; i++) {
                             buf.append(args[i]).append(" ");
                         }
                         String tmp = buf.toString();
                         String t = tmp.substring(0, tmp.length() - 1);
                         // need to make there are no periods(.) in the text
                         String nodots = StringUtils.replace(t, ".", "_");
                         plugin.config.set("worlds." + nodots, false);
                     }
                     try {
                         plugin.config.save(plugin.myconfigfile);
                         sender.sendMessage(Constants.MY_PLUGIN_NAME + " The config was updated!");
                     } catch (IOException e) {
                         sender.sendMessage(ChatColor.GRAY + Constants.MY_PLUGIN_NAME + ChatColor.RESET + " There was a problem saving the config file!");
                     }
                 }
                 return true;
             }
             if (player == null) {
                 sender.sendMessage("This command can only be run by a player");
                 return false;
             } else {
                 if (args[0].equalsIgnoreCase("update")) {
                     if (player.hasPermission("TARDIS.update")) {
                         String[] validBlockNames = {"door", "button", "save-repeater", "x-repeater", "z-repeater", "y-repeater"};
                         if (args.length < 2) {
                             sender.sendMessage(ChatColor.GRAY + Constants.MY_PLUGIN_NAME + ChatColor.RESET + " Too few command arguments!");
                             return false;
                         }
                         if (!Arrays.asList(validBlockNames).contains(args[1].toLowerCase())) {
                             player.sendMessage(ChatColor.GRAY + Constants.MY_PLUGIN_NAME + ChatColor.RESET + " That is not a valid TARDIS block name! Try one of : door|button|save-repeater|x-repeater|z-repeater|y-repeater");
                             return false;
                         }
                         try {
                             Connection connection = service.getConnection();
                             Statement statement = connection.createStatement();
                            String queryInTARDIS = "SELECT tardis.owner, travellers.player FROM tardis, travellers WHERE travellers.player = '" + player.getName() + "' AND traveller.tardis_id = tardis.tardis_id AND travellers.player = tardis.owner";
                             //String queryInTARDIS = "SELECT player FROM travellers WHERE player = '" + player.getName() + "'";
                             ResultSet rs = statement.executeQuery(queryInTARDIS);
                             if (rs == null || !rs.next()) {
                                 sender.sendMessage(ChatColor.GRAY + Constants.MY_PLUGIN_NAME + ChatColor.RESET + " Either you are not the Timelord, or you are not inside your TARDIS. You need to be both to run this command!");
                                 return false;
                             }
                             rs.close();
                             statement.close();
                         } catch (SQLException e) {
                             System.err.println(Constants.MY_PLUGIN_NAME + " List Saves Error: " + e);
                         }
                         plugin.trackPlayers.put(player.getName(), args[1].toLowerCase());
                         player.sendMessage(ChatColor.GRAY + Constants.MY_PLUGIN_NAME + ChatColor.RESET + " Click the TARDIS " + args[1].toLowerCase() + " with to update its position.");
                         return true;
                     } else {
                         sender.sendMessage(ChatColor.GRAY + Constants.MY_PLUGIN_NAME + ChatColor.RESET + Constants.NO_PERMS_MESSAGE);
                         return false;
                     }
                 }
                 if (args[0].equalsIgnoreCase("list")) {
                     if (player.hasPermission("TARDIS.list")) {
                         try {
                             Connection connection = service.getConnection();
                             Statement statement = connection.createStatement();
                             String queryList = "SELECT owner FROM tardis WHERE owner = '" + player.getName() + "'";
                             ResultSet rs = statement.executeQuery(queryList);
                             if (rs == null || !rs.next()) {
                                 sender.sendMessage(ChatColor.GRAY + Constants.MY_PLUGIN_NAME + ChatColor.RESET + " You have not created a TARDIS yet!");
                                 return false;
                             }
                             Constants.list(player);
                             rs.close();
                             statement.close();
                             return true;
                         } catch (SQLException e) {
                             System.err.println(Constants.MY_PLUGIN_NAME + " List Saves Error: " + e);
                         }
                     } else {
                         sender.sendMessage(Constants.NO_PERMS_MESSAGE);
                         return false;
                     }
                 }
                 if (args[0].equalsIgnoreCase("find")) {
                     if (player.hasPermission("TARDIS.find")) {
                         try {
                             Connection connection = service.getConnection();
                             Statement statement = connection.createStatement();
                             String queryList = "SELECT save FROM tardis WHERE owner = '" + player.getName() + "'";
                             ResultSet rs = statement.executeQuery(queryList);
                             if (rs == null || !rs.next()) {
                                 sender.sendMessage(ChatColor.GRAY + Constants.MY_PLUGIN_NAME + ChatColor.RESET + " You have not created a TARDIS yet!");
                                 return false;
                             }
                             String loc = rs.getString("save");
                             String[] findData = loc.split(":");
                             sender.sendMessage(ChatColor.GRAY + Constants.MY_PLUGIN_NAME + ChatColor.RESET + " You you left your TARDIS in " + findData[0] + " at x:" + findData[1] + " y:" + findData[2] + " z:" + findData[3]);
                             rs.close();
                             statement.close();
                             return true;
                         } catch (SQLException e) {
                             System.err.println(Constants.MY_PLUGIN_NAME + " Find TARDIS Error: " + e);
                         }
                     } else {
                         sender.sendMessage(ChatColor.GRAY + Constants.MY_PLUGIN_NAME + ChatColor.RESET + Constants.NO_PERMS_MESSAGE);
                         return false;
                     }
                 }
                 if (args[0].equalsIgnoreCase("add")) {
                     if (player.hasPermission("TARDIS.add")) {
                         try {
                             Connection connection = service.getConnection();
                             Statement statement = connection.createStatement();
                             String queryList = "SELECT tardis_id, companions FROM tardis WHERE owner = '" + player.getName() + "'";
                             ResultSet rs = statement.executeQuery(queryList);
                             String comps;
                             int id;
                             if (rs == null || !rs.next()) {
                                 sender.sendMessage(ChatColor.GRAY + Constants.MY_PLUGIN_NAME + ChatColor.RESET + " You have not created a TARDIS yet!");
                                 return false;
                             } else {
                                 comps = rs.getString("companions");
                                 id = rs.getInt("tardis_id");
                                 rs.close();
                             }
                             if (args.length < 2) {
                                 sender.sendMessage(ChatColor.GRAY + Constants.MY_PLUGIN_NAME + ChatColor.RESET + " Too few command arguments!");
                                 return false;
                             } else {
                                 String queryCompanions;
                                 if (comps != null && !comps.equals("") && !comps.equals("[Null]")) {
                                     // add to the list
                                     String newList = comps + ":" + args[1];
                                     queryCompanions = "UPDATE tardis SET companions = '" + newList + "' WHERE tardis_id = " + id;
                                 } else {
                                     // make a list
                                     queryCompanions = "UPDATE tardis SET companions = '" + args[1] + "' WHERE tardis_id = " + id;
                                 }
                                 statement.executeUpdate(queryCompanions);
                                 player.sendMessage(ChatColor.GRAY + Constants.MY_PLUGIN_NAME + ChatColor.RESET + " You added " + ChatColor.GREEN + args[1] + ChatColor.RESET + " as a TARDIS companion.");
                                 statement.close();
                                 return true;
                             }
                         } catch (SQLException e) {
                             System.err.println(Constants.MY_PLUGIN_NAME + " Companion Save Error: " + e);
                         }
                     } else {
                         sender.sendMessage(Constants.NO_PERMS_MESSAGE);
                         return false;
                     }
                 }
                 if (args[0].equalsIgnoreCase("remove")) {
                     if (player.hasPermission("TARDIS.add")) {
                         try {
                             Connection connection = service.getConnection();
                             Statement statement = connection.createStatement();
                             String queryList = "SELECT tardis_id, companions FROM tardis WHERE owner = '" + player.getName() + "'";
                             ResultSet rs = statement.executeQuery(queryList);
                             String comps;
                             int id;
                             if (rs == null || !rs.next()) {
                                 sender.sendMessage(ChatColor.GRAY + Constants.MY_PLUGIN_NAME + ChatColor.RESET + " You have not created a TARDIS yet!");
                                 return false;
                             } else {
                                 id = rs.getInt("tardis_id");
                                 comps = rs.getString("companions");
                                 rs.close();
                             }
                             if (comps.equals("") || comps.equals("[Null]") || comps == null) {
                                 sender.sendMessage(ChatColor.GRAY + Constants.MY_PLUGIN_NAME + ChatColor.RESET + " You have not added any TARDIS companions yet!");
                                 return false;
                             }
                             if (args.length < 2) {
                                 sender.sendMessage(ChatColor.GRAY + Constants.MY_PLUGIN_NAME + ChatColor.RESET + " Too few command arguments!");
                                 return false;
                             } else {
                                 String[] split = comps.split(":");
                                 String newList = "";
                                 // recompile string without the specified player
                                 for (String c : split) {
                                     if (!c.equals(args[1])) {
                                         // add to new string
                                         newList += c + ":";
                                     }
                                 }
                                 // remove trailing colon
                                 newList = newList.substring(0, newList.length() - 1);
                                 String queryCompanions = "UPDATE tardis SET companions = '" + newList + "' WHERE tardis_id = " + id;
                                 statement.executeUpdate(queryCompanions);
                                 player.sendMessage(ChatColor.GRAY + Constants.MY_PLUGIN_NAME + ChatColor.RESET + " You removed " + ChatColor.GREEN + args[1] + ChatColor.RESET + " as a TARDIS companion.");
                                 statement.close();
                                 return true;
                             }
                         } catch (SQLException e) {
                             System.err.println(Constants.MY_PLUGIN_NAME + " Companion Save Error: " + e);
                         }
                     } else {
                         sender.sendMessage(Constants.NO_PERMS_MESSAGE);
                         return false;
                     }
                 }
                 if (args[0].equalsIgnoreCase("save")) {
                     if (player.hasPermission("TARDIS.save")) {
                         try {
                             Connection connection = service.getConnection();
                             Statement statement = connection.createStatement();
                             String queryList = "SELECT * FROM tardis WHERE owner = '" + player.getName() + "'";
                             ResultSet rs = statement.executeQuery(queryList);
                             if (rs == null || !rs.next()) {
                                 sender.sendMessage(ChatColor.GRAY + Constants.MY_PLUGIN_NAME + ChatColor.RESET + " You have not created a TARDIS yet!");
                                 return false;
                             }
                             if (args.length < 3) {
                                 sender.sendMessage(ChatColor.GRAY + Constants.MY_PLUGIN_NAME + ChatColor.RESET + " Too few command arguments!");
                                 return false;
                             } else {
                                 String cur = rs.getString("current");
                                 String sav = rs.getString("save");
                                 int id = rs.getInt("tardis_id");
                                 int count = args.length;
                                 StringBuilder buf = new StringBuilder();
                                 for (int i = 2; i < count; i++) {
                                     buf.append(args[i]).append(" ");
                                 }
                                 String tmp = buf.toString();
                                 String t = tmp.substring(0, tmp.length() - 1);
                                 // need to make there are no periods(.) in the text
                                 String nodots = StringUtils.replace(t, ".", "_");
                                 String curDest;
                                 // get current destination
                                 String queryTraveller = "SELECT * FROM travellers WHERE player = '" + player.getName() + "'";
                                 ResultSet rsTraveller = statement.executeQuery(queryTraveller);
                                 if (rsTraveller != null && rsTraveller.next()) {
                                     // inside TARDIS
                                     curDest = nodots + ":" + cur;
                                 } else {
                                     // outside TARDIS
                                     curDest = nodots + "~" + sav;
                                 }
                                 String querySave = "UPDATE tardis SET save" + args[1] + " = '" + curDest + "' WHERE tardis_id = " + id;
                                 statement.executeUpdate(querySave);
                                 rs.close();
                                 rsTraveller.close();
                                 statement.close();
                                 return true;
                             }
                         } catch (SQLException e) {
                             System.err.println(Constants.MY_PLUGIN_NAME + " Companion Save Error: " + e);
                         }
                     } else {
                         sender.sendMessage(Constants.NO_PERMS_MESSAGE);
                         return false;
                     }
                 }
                 if (args[0].equalsIgnoreCase("help")) {
                     if (args.length == 1) {
                         sender.sendMessage(Constants.COMMANDS.split("\n"));
                         return true;
                     }
                     if (args.length == 2) {
                         switch (Constants.fromString(args[1])) {
                             case CREATE:
                                 sender.sendMessage(Constants.COMMAND_CREATE.split("\n"));
                                 break;
                             case DELETE:
                                 sender.sendMessage(Constants.COMMAND_DELETE.split("\n"));
                                 break;
                             case TIMETRAVEL:
                                 sender.sendMessage(Constants.COMMAND_TIMETRAVEL.split("\n"));
                                 break;
                             case LIST:
                                 sender.sendMessage(Constants.COMMAND_LIST.split("\n"));
                                 break;
                             case FIND:
                                 sender.sendMessage(Constants.COMMAND_FIND.split("\n"));
                                 break;
                             case SAVE:
                                 sender.sendMessage(Constants.COMMAND_SAVE.split("\n"));
                                 break;
                             case ADD:
                                 sender.sendMessage(Constants.COMMAND_ADD.split("\n"));
                                 break;
                             case ADMIN:
                                 sender.sendMessage(Constants.COMMAND_ADMIN.split("\n"));
                                 break;
                             default:
                                 sender.sendMessage(Constants.COMMANDS.split("\n"));
                         }
                     }
                     return true;
                 }
             }
         }
         //If the above has happened the function will break and return true. if this hasn't happened the a value of false will be returned.
         return false;
     }
 }
