 package com.kierdavis.ultracommand;
 
 import java.io.File;
 import java.io.IOException;
import java.util.Arrays;
 import java.util.ArrayList;
 import java.util.HashMap;
 import java.util.Iterator;
 import java.util.List;
 import java.util.Map;
 import java.util.Set;
import org.bukkit.ChatColor;
 import org.bukkit.configuration.ConfigurationSection;
 import org.bukkit.configuration.file.FileConfiguration;
 import org.bukkit.configuration.file.YamlConfiguration;
 import org.bukkit.entity.Player;
 import org.bukkit.plugin.java.JavaPlugin;
 import org.bukkit.scheduler.BukkitTask;
 
 public class UltraCommand extends JavaPlugin {
     private File commandsFile;
     private FileConfiguration commandsConfig;
     private BukkitTask saveCommandsTask;
     private boolean dirty;
     
     public void onEnable() {
         commandsFile = new File(getDataFolder(), "commands.yml");
         loadCustomCommands();
         
         getServer().getPluginManager().registerEvents(new PlayerListener(this), this);
         
         UltraCommandExecutor cmdExec = new UltraCommandExecutor(this);
         getCommand("ultracommand").setExecutor(cmdExec);
         getCommand("uc").setExecutor(cmdExec);
         
         dirty = false;
         saveCommandsTask = new SaveCommandsTask(this).runTaskTimer(this, 20 * 60, 20 * 60); // Check every minute.
     }
     
     public void onDisable() {
         saveCommandsTask.cancel();
         saveCustomCommands();
     }
     
     public void loadCustomCommands() {
         if (!commandsFile.exists()) {
             createCommandsFile();
             return;
         }
         
         commandsConfig = YamlConfiguration.loadConfiguration(commandsFile);
         getLogger().info("Loaded " + commandsFile.toString());
     }
     
     public void saveCustomCommands() {
         try {
             commandsConfig.save(commandsFile);
             dirty = false;
             getLogger().info("Saved " + commandsFile.toString());
         }
         
         catch (IOException e) {
             getLogger().severe("Could not save " + commandsFile.toString() + ": " + e.toString());
         }
     }
     
     public Set<String> getCustomCommands() {
         return getCommandsSection().getKeys(false);
     }
     
     public CustomCommand getCustomCommand(String name) {
         ConfigurationSection commandSection = getCommandSection(name);
         if (commandSection == null) return null;
         
         CustomCommand cmd = new CustomCommand();
         List<String> l;
         
         l = commandSection.getStringList("text");
         if (l != null && l.size() > 0) cmd.setText(l);
         
         l = commandSection.getStringList("chat");
         if (l != null && l.size() > 0) cmd.setText(l);
         
         l = commandSection.getStringList("playerCommands");
         if (l != null && l.size() > 0) cmd.setPlayerCommands(l);
         
         l = commandSection.getStringList("consoleCommands");
         if (l != null && l.size() > 0) cmd.setConsoleCommands(l);
         
         return cmd;
     }
     
     public boolean addCustomCommand(String name) {
         ConfigurationSection commandsSection = getCommandsSection();
         name = name.toLowerCase();
         
         if (commandsSection.contains(name)) {
             return false;
         }
         
         ConfigurationSection commandSection = commandsSection.createSection(name);
         commandSection.set("text", new ArrayList<String>());
         commandSection.set("chat", new ArrayList<String>());
         commandSection.set("playerCommands", new ArrayList<String>());
         commandSection.set("consoleCommands", new ArrayList<String>());
         
         dirty = true;
         return true;
     }
     
     public boolean hasCustomCommand(String name) {
         return getCommandSection(name) != null;
     }
     
     public boolean removeCustomCommand(String name) {
         ConfigurationSection commandsSection = getCommandsSection();
         name = name.toLowerCase();
         
         if (!commandsSection.contains(name)) {
             return false;
         }
         
         commandsSection.set(name, null);
         
         dirty = true;
         return true;
     }
     
     public boolean addText(String name, String s) {
         ConfigurationSection commandSection = getCommandSection(name);
         if (commandSection == null) return false;
         
         List<String> l = commandSection.getStringList("text");
         l.add(s);
         commandSection.set("text", l);
         
         dirty = true;
         return true;
     }
     
     public boolean addChat(String name, String s) {
         ConfigurationSection commandSection = getCommandSection(name);
         if (commandSection == null) return false;
         
         List<String> l = commandSection.getStringList("chat");
         l.add(s);
         commandSection.set("chat", l);
         
         dirty = true;
         return true;
     }
     
     public boolean addPlayerCommand(String name, String s) {
         ConfigurationSection commandSection = getCommandSection(name);
         if (commandSection == null) return false;
         
         List<String> l = commandSection.getStringList("playerCommands");
         l.add(s);
         commandSection.set("playerCommands", l);
         
         dirty = true;
         return true;
     }
     
     public boolean addConsoleCommand(String name, String s) {
         ConfigurationSection commandSection = getCommandSection(name);
         if (commandSection == null) return false;
         
         List<String> l = commandSection.getStringList("consoleCommands");
         l.add(s);
         commandSection.set("consoleCommands", l);
         
         dirty = true;
         return true;
     }
     
     public List<String> getText(String name) {
         ConfigurationSection commandSection = getCommandSection(name);
         if (commandSection == null) return null;
         
         return commandSection.getStringList("text");
     }
     
     public List<String> getChat(String name) {
         ConfigurationSection commandSection = getCommandSection(name);
         if (commandSection == null) return null;
         
         return commandSection.getStringList("chat");
     }
     
     public List<String> getPlayerCommands(String name) {
         ConfigurationSection commandSection = getCommandSection(name);
         if (commandSection == null) return null;
         
         return commandSection.getStringList("playerCommands");
     }
     
     public List<String> getConsoleCommands(String name) {
         ConfigurationSection commandSection = getCommandSection(name);
         if (commandSection == null) return null;
         
         return commandSection.getStringList("consoleCommands");
     }
     
     public boolean clearText(String name) {
         ConfigurationSection commandSection = getCommandSection(name);
         if (commandSection == null) return false;
         
         commandSection.set("text", new ArrayList<String>());
         
         dirty = true;
         return true;
     }
     
     public boolean clearChat(String name) {
         ConfigurationSection commandSection = getCommandSection(name);
         if (commandSection == null) return false;
         
         commandSection.set("chat", new ArrayList<String>());
         
         dirty = true;
         return true;
     }
     
     public boolean clearPlayerCommands(String name) {
         ConfigurationSection commandSection = getCommandSection(name);
         if (commandSection == null) return false;
         
         commandSection.set("playerCommands", new ArrayList<String>());
         
         dirty = true;
         return true;
     }
     
     public boolean clearConsoleCommands(String name) {
         ConfigurationSection commandSection = getCommandSection(name);
         if (commandSection == null) return false;
         
         commandSection.set("consoleCommands", new ArrayList<String>());
         
         dirty = true;
         return true;
     }
     
     private ConfigurationSection getCommandsSection() {
         ConfigurationSection commandsSection = commandsConfig.getConfigurationSection("commands");
         if (commandsSection == null) {
             commandsSection = commandsConfig.createSection("commands");
         }
         
         return commandsSection;
     }
     
     private ConfigurationSection getCommandSection(String name) {
         return getCommandsSection().getConfigurationSection(name.toLowerCase());
     }
     
     private void createCommandsFile() {
         File parent = commandsFile.getParentFile();
         
         try {
             if (!parent.exists()) {
                 parent.mkdirs();
             }
             
             if (!commandsFile.exists()) {
                 boolean b = commandsFile.createNewFile();
                 if (b) {
                     getLogger().info("Created " + commandsFile.toString());
                 }
             }
         }
         
         catch (IOException e) {
             getLogger().warning("Could not create " + commandsFile.toString() + ": " + e.toString());
         }
     }
     
     public boolean isDirty() {
         return dirty;
     }
     
     public boolean doCommand(Player player, String[] parts) {
         String cmdName = parts[0];
         CustomCommand cmd = getCustomCommand(cmdName);
         
         if (cmdName.startsWith("/")) cmdName = cmdName.substring(1);
         
         if (cmd != null) {
             String perm = "ultracommand.commands." + cmdName;
             if (!player.hasPermission(perm) && !player.hasPermission("ultracommand.commands.*")) {
                 player.sendMessage(ChatColor.YELLOW + "You don't have permission for this command (" + perm + ")");
                 return true;
             }
             
             cmd.execute(player, Arrays.copyOfRange(parts, 1, parts.length));
             return true;
         }
         
         return false;
     }
 }
