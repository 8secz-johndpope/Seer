 package me.chaseoes.tf2;
 
 import java.util.HashMap;
 
 import me.chaseoes.tf2.utilities.LocationStore;
 
 import org.bukkit.ChatColor;
 import org.bukkit.Location;
 import org.bukkit.entity.Player;
 
 public class Schedulers {
 
     private TF2 plugin;
     static Schedulers instance = new Schedulers();
     Integer afkchecker;
     public HashMap<String, Integer> redcounter = new HashMap<String, Integer>();
     public HashMap<String, Integer> countdowns = new HashMap<String, Integer>();
     public HashMap<String, Integer> timelimitcounter = new HashMap<String, Integer>();
 
     private Schedulers() {
 
     }
 
     public static Schedulers getSchedulers() {
         return instance;
     }
 
     public void setup(TF2 p) {
         plugin = p;
     }
 
     public void startAFKChecker() {
         final Integer afklimit = plugin.getConfig().getInt("afk-timer");
         afkchecker = plugin.getServer().getScheduler().scheduleSyncRepeatingTask(plugin, new Runnable() {
             @Override
             public void run() {
                 try {
                     for (String map : MapUtilities.getUtilities().getEnabledMaps()) {
                         for (String p : GameUtilities.getUtilities().getIngameList(map)) {
                             Player player = plugin.getServer().getPlayerExact(p);
                             Integer afktime = LocationStore.getAFKTime(player);
                             Location lastloc = LocationStore.getLastLocation(player);
                             Location currentloc = new Location(player.getWorld(), player.getLocation().getBlockX(), player.getLocation().getBlockY(), player.getLocation().getBlockZ());
                             if (lastloc != null) {
                                 if (lastloc.getWorld().getName() == currentloc.getWorld().getName() && lastloc.getBlockX() == currentloc.getBlockX() && lastloc.getBlockY() == currentloc.getBlockY() && lastloc.getBlockZ() == currentloc.getBlockZ()) {
 
                                     if (afktime == null) {
                                         LocationStore.setAFKTime(player, 1);
                                     } else {
                                         LocationStore.setAFKTime(player, afktime + 1);
                                     }
 
                                     if (afklimit == afktime) {
                                         GameUtilities.getUtilities().leaveCurrentGame(player);
                                         player.sendMessage(ChatColor.YELLOW + "[TF2] You have been kicked from the map for being AFK.");
                                         LocationStore.setAFKTime(player, null);
                                         LocationStore.unsetLastLocation(player);
                                     }
                                 } else {
                                     LocationStore.setAFKTime(player, null);
                                     LocationStore.unsetLastLocation(player);
                                 }
                                 LocationStore.setLastLocation(player);
                             } else {
                                 LocationStore.setLastLocation(player);
                             }
 
                         }
                     }
                 } catch (Exception e) {
                     e.printStackTrace();
                 }
             }
         }, 0L, 20L);
     }
 
     public void stopAFKChecker() {
         if (afkchecker != null) {
             plugin.getServer().getScheduler().cancelTask(afkchecker);
         }
         afkchecker = null;
     }
 
     public void startRedTeamCountdown(final String map) {
         redcounter.put(map, plugin.getServer().getScheduler().scheduleSyncRepeatingTask(plugin, new Runnable() {
             int secondsleft = plugin.getMap(map).getRedTeamTeleportTime();
 
             @Override
             public void run() {
                 if (secondsleft != 0) {
                     if (secondsleft % 10 == 0 || secondsleft < 6) {
                         for (String playe : GameUtilities.getUtilities().getIngameList(map)) {
                             if (GameUtilities.getUtilities().getTeam(plugin.getServer().getPlayerExact(playe)).equalsIgnoreCase("red")) {
                                 plugin.getServer().getPlayerExact(playe).sendMessage(ChatColor.YELLOW + "[TF2] " + ChatColor.DARK_RED + ChatColor.BOLD + "Red " + ChatColor.RESET + ChatColor.YELLOW + "team, you will be teleported in " + secondsleft + " seconds.");
                             }
                         }
                     }
                 }
                 secondsleft--;
             }
         }, 0L, 20L));
     }
 
     public void startCountdown(final String map) {
         GameUtilities.getUtilities().setStatus(map, GameStatus.STARTING);
         countdowns.put(map, plugin.getServer().getScheduler().scheduleSyncRepeatingTask(plugin, new Runnable() {
             int secondsLeft = plugin.getConfig().getInt("countdown");
 
             @Override
             public void run() {
                 if (secondsLeft != 0) {
                     if (secondsLeft % 10 == 0 || secondsLeft < 6) {
                         GameUtilities.getUtilities().broadcast(map, ChatColor.BLUE + "Game starting in " + ChatColor.AQUA + secondsLeft + " " + ChatColor.BLUE + "seconds!");
                     }
                     secondsLeft--;
                 } else {
                     GameUtilities.getUtilities().gamestarting.remove(map);
                     GameUtilities.getUtilities().startMatch(map);
                     GameUtilities.getUtilities().setStatus(map, GameStatus.INGAME);
                     stopCountdown(map);
                 }
             }
         }, 0L, 20L));
     }
 
     public void startTimeLimitCounter(final String map) {
         final int limit = plugin.getMap(map).getTimelimit();
         timelimitcounter.put(map, plugin.getServer().getScheduler().scheduleSyncRepeatingTask(plugin, new Runnable() {
             int current = 0;
             int secondsleft = plugin.getMap(map).getTimelimit();
 
             @Override
             public void run() {
                 try {
                     GameUtilities.getUtilities().games.get(map).time = current;
                     if (secondsleft != 0) {
                         if (secondsleft % 60 == 0 || secondsleft < 10) {
                             GameUtilities.getUtilities().broadcast(map, ChatColor.BLUE + "Game ending in " + ChatColor.AQUA + GameUtilities.getUtilities().getTimeLeftPretty(map) + ChatColor.BLUE + "!");
                         }
                     }
                     secondsleft--;
                     if (current >= limit) {
                         GameUtilities.getUtilities().winGame(map, "blue");
                         stopTimeLimitCounter(map);
                         GameUtilities.getUtilities().gametimes.remove(map);
                     }
                     current++;
                 } catch (Exception e) {
                     e.printStackTrace();
                 }
             }
         }, 0L, 20L));
     }
 
     public void stopRedTeamCountdown(String map) {
         if (redcounter.get(map) != null) {
             plugin.getServer().getScheduler().cancelTask(redcounter.get(map));
         }
     }
 
     public void stopTimeLimitCounter(String map) {
         if (timelimitcounter.get(map) != null) {
             plugin.getServer().getScheduler().cancelTask(timelimitcounter.get(map));
         }
     }
 
     public void stopCountdown(String map) {
        plugin.getServer().getScheduler().cancelTask(countdowns.get(map));
     }
 
 }
