 package com.exolius.simplebackup;
 
 import org.bukkit.ChatColor;
 import org.bukkit.World;
 import org.bukkit.entity.Player;
 import org.bukkit.command.Command;
 import org.bukkit.command.CommandSender;
 import org.bukkit.configuration.file.FileConfiguration;
 import org.bukkit.plugin.java.JavaPlugin;
 
 import java.io.File;
 import java.text.SimpleDateFormat;
 import java.util.Date;
 import java.util.List;
 
 public class SimpleBackup extends JavaPlugin {
      /*----------------------
      Variable declerations
      ----------------------*/
     double interval;
     boolean broadcast = true;
     public static String message = "[SimpleBackup]";
    public static String dateFormat = "yyyy-MM-dd-HH-mm-ss";
     public static String backupFile = "backups/";
     public static int intervalBetween = 100;
     List<String> backupWorlds;
     protected FileConfiguration config;
     public static String pluginLocation;
     public boolean firstRunDelay = true;
     public String customMessage = "Backup starting";
     public boolean deleteOldMaps = false;
     public int daysToDelete = 2;
 
 
     public void onDisable() {
         getServer().getScheduler().cancelTasks(this);
         System.out.println("[SimpleBackup] Disabled SimpleBackup");
     }
 
     public void onEnable() {
         // When plugin is enabled, load the "config.yml"
         loadConfiguration();
         pluginLocation = "" + this.getDataFolder();
         // Set the backup interval, 72000.0D is 1 hour, multiplying it by the value interval will change the backup cycle time
         double ticks = 72000.0D * this.interval;
 
         // Add the repeating task, set it to repeat the specified time
         this.getServer().getScheduler().scheduleAsyncRepeatingTask(this, new Runnable() {
 
             public void run() {
                 // When the task is run, start the map backup
                 doBackup();
             }
         }, 60L, (long) ticks);
 
         // After enabling, print to console to say if it was successful
         System.out.println("[SimpleBackup] Enabled. Backup interval " + this.interval + " hours");
         // Shameless self promotion in the source code :D
         System.out.println("[SimpleBackup] Developed by Exolius");
     }
 
     public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
         Player player = null;
         if (sender instanceof Player) {
             player = (Player) sender;
         }
 
         if (cmd.getName().equalsIgnoreCase("sbackup")) {
             if (player == null) {
                 sender.sendMessage("this command can only be run by a player");
             } else {
                 if (player.isOp()) {
                     doBackup();
                 }
             }
             return true;
         }
         return false;
     }
 
     public void loadConfiguration() {
         // Set the config object
         config = getConfig();
 
         // Set default values for variables
         interval        =     config.getDouble("backup-interval-hours", 1.0D);
         intervalBetween =     config.getInt("interval-between", intervalBetween);
         broadcast       =     config.getBoolean("broadcast-message", true);
         backupFile      =     config.getString("backup-file", backupFile);
         backupWorlds    =     config.getStringList("backup-worlds");
         message         =     config.getString("backup-message", message);
         dateFormat      =     config.getString("backup-date-format", dateFormat);
         firstRunDelay   =     config.getBoolean("delay-first-backup", firstRunDelay);
         customMessage   =     config.getString("custom-backup-mesasge", customMessage);
 
         //These haven't been implemented yet.
         //deleteOldMaps = config.getBoolean("delete-old-maps", deleteOldMaps);
         //daysToDelete = config.getInt("days-to-delete", daysToDelete);
 
         if (backupWorlds.size() == 0) {
             // If "backupWorlds" is empty then fill it with the worlds
             backupWorlds.add(((World) getServer().getWorlds().get(0)).getName());
         }
 
         // Generate the config.yml
         config.set("backup-worlds", backupWorlds);
         config.set("interval-between", Integer.valueOf(intervalBetween));
         config.set("backup-interval-hours", Double.valueOf(interval));
         config.set("broadcast-message", Boolean.valueOf(broadcast));
         config.set("backup-file", backupFile);
         config.set("backup-message", message);
         config.set("backup-date-format", dateFormat);
         config.set("delay-first-backup", firstRunDelay);
         config.set("custom-backup-message",customMessage);
 
         //These haven't been implemented yet.
         //config.set("delete-old-maps", deleteOldMaps);
         //config.set("days-to-delete", daysToDelete);
 
         saveConfig();
     }
 
     public void doBackup() {
         if (firstRunDelay) {
             firstRunDelay = false;
         } else {
             // Begin backup of worlds
             // Broadcast the backup initialization if enabled
             SimpleBackup.this.getServer().broadcastMessage(ChatColor.BLUE + message + " " + customMessage);
             // Loop through all the specified worlds and save then to a .zip
             for (World world : SimpleBackup.this.getServer().getWorlds()) {
                 try {
                     world.save();
                     world.setAutoSave(false);
 
                     BackupThread bt = SimpleBackup.this.backupWorld(world, null);
                     if (bt != null) {
                         bt.start();
                         while (bt.isAlive()) ;
                         world.setAutoSave(true);
                     }
                 } catch (Exception e) {
                     e.printStackTrace();
                 }
             }
 
             // Broadcast the backup completion if enabled
             if (SimpleBackup.this.broadcast)
                 SimpleBackup.this.getServer().broadcastMessage(ChatColor.BLUE + message + " Backup completed.");
 
             // Old map file deletion goes here
 
         }
     }
 
     public BackupThread backupWorld(World world, File file)
             throws Exception {
         if (world != null) {
             if ((this.backupWorlds.contains(world.getName()))) {
                 return new BackupThread(new File(world.getName()));
             }
             return null;
         }
         if ((world == null) && (file != null)) {
             return new BackupThread(file);
         }
         return null;
     }
 
     public static String format() {
         //Set the naming format of the backed up file, based on the config values
         Date date = new Date();
         SimpleDateFormat formatter = new SimpleDateFormat(dateFormat);
         return formatter.format(date);
     }
 
     //Not finished yet
     public void printDebug() {
         // Debugging code, prints loaded variables to console
         // To see if the loading works after modification
         System.out.println(backupWorlds);
         System.out.println(intervalBetween);
         System.out.println(interval);
         System.out.println(broadcast);
         System.out.println(backupFile);
         System.out.println(message);
         System.out.println(dateFormat);
     }
 
 }
