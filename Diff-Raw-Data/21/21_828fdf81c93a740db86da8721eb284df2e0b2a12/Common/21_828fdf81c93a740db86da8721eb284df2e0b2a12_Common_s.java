 package com.github.zathrus_writer.commandsex.helpers;
 
 import static com.github.zathrus_writer.commandsex.Language._;
 
 import java.sql.ResultSet;
 import java.util.ArrayList;
 import java.util.Collections;
 import java.util.HashMap;
 import java.util.List;
 import java.util.Map;
 import java.util.Set;
 
 import net.minecraft.server.EntityPlayer;
 import net.minecraft.server.Packet201PlayerInfo;
 
 import org.bukkit.Bukkit;
 import org.bukkit.ChatColor;
 import org.bukkit.GameMode;
 import org.bukkit.Location;
 import org.bukkit.command.CommandSender;
 import org.bukkit.craftbukkit.CraftServer;
 import org.bukkit.craftbukkit.entity.CraftPlayer;
 import org.bukkit.entity.EntityType;
 import org.bukkit.entity.Player;
 import org.bukkit.event.EventHandler;
 import org.bukkit.event.EventPriority;
 import org.bukkit.event.HandlerList;
 import org.bukkit.event.Listener;
 import org.bukkit.event.block.BlockBreakEvent;
 import org.bukkit.event.block.BlockDamageEvent;
 import org.bukkit.event.block.BlockIgniteEvent;
 import org.bukkit.event.block.BlockPlaceEvent;
 import org.bukkit.event.entity.EntityDamageEvent;
 import org.bukkit.event.entity.PlayerDeathEvent;
 import org.bukkit.event.inventory.InventoryOpenEvent;
 import org.bukkit.event.player.AsyncPlayerChatEvent;
 import org.bukkit.event.player.PlayerBucketEmptyEvent;
 import org.bukkit.event.player.PlayerDropItemEvent;
 import org.bukkit.event.player.PlayerInteractEvent;
 import org.bukkit.event.player.PlayerJoinEvent;
 import org.bukkit.event.player.PlayerMoveEvent;
 import org.bukkit.event.player.PlayerQuitEvent;
 import org.bukkit.event.player.PlayerTeleportEvent;
 import org.bukkit.util.Vector;
 import org.kitteh.vanish.staticaccess.VanishNoPacket;
 
 import com.github.zathrus_writer.commandsex.CommandsEX;
 import com.github.zathrus_writer.commandsex.SQLManager;
 import com.github.zathrus_writer.commandsex.handlers.Handler_condensejoins;
 
 /***
  * Contains set of functions and event listeners used to handle the freeze command.
  * @author zathrus-writer
  *
  */
 public class Common implements Listener {
 	
 	public static List<String> frozenPlayers = new ArrayList<String>();
 	public static Common plugin = null;
 	public static Boolean ignoreTpEvent = false;
 	public static List<String> godPlayers = new ArrayList<String>();
 	public static List<String> slappedPlayers = new ArrayList<String>();
 	public static List<String> invisiblePlayers = new ArrayList<String>();
 	public static List<String> keepInventoryDisabledPlayers = new ArrayList<String>();
 	
 	
 	public static Map<String, Location> slappedLastLocations = new HashMap<String, Location>();
 	public static Map<String, Integer> slappedUnslapTasks = new HashMap<String, Integer>();
 
 	/***
 	 * simply tell Bukkit we want to listen
 	 */
 	public Common() {
 		plugin = this;
 	}
 	
 	/***
 	 * Insurance that will teleport a slapped player back to their original position if a damage event does not occur in 25 seconds.
 	 */
 	public static class Unslap implements Runnable {
     	private Player p;
     	private String pName;
     	
     	public Unslap(Player p, String pName) {
     		this.p = p;
     		this.pName = pName;
     	}
     	
     	public void run() {
     		if (!Common.slappedPlayers.contains(pName)) return;
     		try {
 	    		// restore player's original position
 				p.teleport(Common.slappedLastLocations.get(pName));
 				LogHelper.showInfo("playerSlapReturned", p);
     		} catch (Throwable e) {
     			// player might log out, so our teleport request may fail with error, which can be safely ignored here
     		}
 			
 			// remove records
     		Common.slappedPlayers.remove(pName);
     		Common.slappedLastLocations.remove(pName);
     	}
     }
 	
 	/***
 	 * FREEZE - freezes a player, making him unable to perform physical actions on the server
 	 * @param sender
 	 * @param args
 	 * @param command
 	 * @param alias
 	 * @return
 	 */
 	public static Boolean freeze(CommandSender sender, String[] args, String command, String alias, Boolean... omitMessage) {
 		// create Common class instance, if not instantiated yet to allow for events listening
 		if (plugin == null) {
 			new Common();
 		}
 
 		Boolean showMessages = (omitMessage.length == 0);
 		// check if requested player is online
 		Player p = Bukkit.getServer().getPlayer(args[0]);
 		String pName = "";
 		if (p != null) {
 			pName = p.getName();
 		} else {
 			pName = args[0];
 		}
 				
 		// check if requested player is frozen
 		if ((frozenPlayers.size() > 0) && frozenPlayers.contains(pName)) {
 			// unfreeze player
 			frozenPlayers.remove(pName);
 			// inform the command sender and the player
 			if (showMessages) {
 				LogHelper.showInfo("[" + pName + " #####freezePlayerUnfrozen", sender);
 				if (p != null) {
 					LogHelper.showInfo("freezeYouAreUnfrozen", p);
 				}
 			}
 			
 			// if there are no more frozen players left, we don't need our event listeners, so unregister them
 			if ((frozenPlayers.size() == 0) && (godPlayers.size() == 0) && (slappedPlayers.size() == 0) && (invisiblePlayers.size() == 0)) {
 				HandlerList.unregisterAll(Common.plugin);
 			}
 			
 			return true;
 		}
 
 		// we are trying to freeze a player
 		if (p == null) {
 			// requested player not found
 			LogHelper.showWarning("invalidPlayer", sender);
 			return true;
 		}
 
 		// insert player's name into frozen players' list
 		frozenPlayers.add(pName);
 		// if we have only this player, add activate event listeners, since they're not active yet
 		if ((frozenPlayers.size() == 1) && (godPlayers.size() == 0) && (slappedPlayers.size() == 0) && (invisiblePlayers.size() == 0)) {
 			CommandsEX.plugin.getServer().getPluginManager().registerEvents(Common.plugin, CommandsEX.plugin);
 		}
 		
 		if (showMessages) {
 			// inform both players
 			LogHelper.showInfo("[" + Nicknames.getNick(pName) + " #####freezePlayerFrozen", sender);
 			LogHelper.showInfo("freezeYouAreFrozen1", p);
 			LogHelper.showInfo("freezeYouAreFrozen2", p);
 		}
 		
 		return true;
 	}
 	
 	/***
 	 * KICK - kicks a player out from the server, optionally providing a custom reason
 	 * @param sender
 	 * @param args
 	 * @param command
 	 * @param alias
 	 * @return
 	 */
 	public static Boolean kick(CommandSender sender, String[] args, String command, String alias) {
 		// check if requested player is online
 		Player p = Bukkit.getServer().getPlayer(args[0]);
 		String pName = "";
 		String leaveReason = "";
 		if (p == null) {
 			// requested player not found
 			LogHelper.showWarning("invalidPlayer", sender);
 			return true;
 		} else if (p.hasPermission("cex.bypass.kick")){
 			LogHelper.showInfo("kickCannotKick", sender, ChatColor.RED);
 			return true;
 		} else {
 			pName = p.getName();
 		}
 		
 		// if we have more than 1 argument, build up the leave string
 		if (args.length > 1) {
 			for (Integer i = 1; i < args.length; i++) {
 				leaveReason = leaveReason + " " + args[i];
 			}
 		}
 		
 		// kick player and tell everyone if set up in the config file
 		p.kickPlayer(ChatColor.RED + (leaveReason.equals("") ? _("kickGenericReason", "") : leaveReason));
 		if (!CommandsEX.getConf().getBoolean("silentKicks")) {
 			CommandsEX.plugin.getServer().broadcastMessage(ChatColor.LIGHT_PURPLE + (!leaveReason.equals("") ? (Nicknames.getNick(pName) + " " + _("kickBeingKickedForMessage", "") + leaveReason) : Nicknames.getNick(pName) + " " + _("kickBeingKickedMessage", "")));
 		}
 		
 		return true;
 	}
 	
 	/***
 	 * PLAYTIME - tells a player or admin how long they played on the server
 	 * @param sender
 	 * @param args
 	 * @param command
 	 * @param alias
 	 * @return
 	 */
 	public static Boolean playtime(CommandSender sender, String[] args, String command, String alias) {
 		if (!CommandsEX.sqlEnabled) {
 			LogHelper.showWarning("playTimeNoSQL", sender);
 			return false;
 		}
 		
 		// check who we want to see the playtime for
 		String pName;
 		Boolean online = false;
 		if (args.length > 0) {
 			pName = args[0];
 			if (Bukkit.getServer().getPlayer(pName) != null) {
 				online = true;
 			}
 		} else {
 			pName = sender.getName();
 		}
 		
 		// load playtime from database if the player is not online, otherwise load it from stored values in main class
 		Integer playtime = 0;
 		if (online && CommandsEX.playTimes.containsKey(pName) && (CommandsEX.playTimes.get(pName) > -1)) {
 			playtime = CommandsEX.playTimes.get(pName);
 			Map<String, Integer> m = Utils.parseTimeStamp(playtime);
 			LogHelper.showInfo("playTimeIs#####[" + (m.get("days") + " #####days#####[, ") + (m.get("hours") + " #####hours#####[, ") + (m.get("minutes") + " #####minutes#####[, ") + (m.get("seconds") + " #####seconds"), sender);
 			return true;
 		} else {
 			String playerForSQL = pName.toLowerCase();
 			String foundPlayerName = "";
 			Integer numPlayers = 0;
 			ResultSet res = SQLManager.query_res("SELECT * FROM " + SQLManager.prefix + "playtime WHERE (player_name = ? OR player_name LIKE ?)", playerForSQL, playerForSQL + "%");
 			
 			try {
 				while (res.next()) {
 					numPlayers++;
 					foundPlayerName = res.getString("player_name");
 					playtime = res.getInt("seconds_played");
 	
 					// if the name matches exactly what we've been looking for, just exit the loop
 					if (foundPlayerName.toLowerCase().equals(playerForSQL)) {
 						numPlayers = 1;
 						playtime = res.getInt("seconds_played");
 						break;
 					}
 				}
 				res.close();
 				
 				if (numPlayers > 1) {
 					// too many players match the selection
 					numPlayers = 0;
 					foundPlayerName = pName;
 				} else if (numPlayers == 1) {
 					// show playtime
 					Map<String, Integer> m = Utils.parseTimeStamp(playtime);
 					LogHelper.showInfo("playTimeIs#####[" + (m.get("days") + " #####days#####[, ") + (m.get("hours") + " #####hours#####[, ") + (m.get("minutes") + " #####minutes#####[, ") + (m.get("seconds") + " #####seconds"), sender);
 					return true;
 				}
 				
 				// if player's playtime was not found, show message
 				if (numPlayers == 0) {
 					LogHelper.showInfo("playTimeNotFound", sender, ChatColor.YELLOW);
 				}
 			} catch (Throwable e) {
 				// unable to load players' playtime
 				LogHelper.showWarning("internalError", sender);
 				LogHelper.logSevere("[CommandsEX] " + _("dbReadError", ""));
 				LogHelper.logDebug("Message: " + e.getMessage() + ", cause: " + e.getCause());
 				return false;
 			}
 		}
 		
 		return true;
 	}
 	
 	/***
 	 * GOD - makes player invincible to damage
 	 * @param sender
 	 * @param args
 	 * @param command
 	 * @param alias
 	 * @return
 	 */
 	public static Boolean god(CommandSender sender, String[] args, String command, String alias, Boolean... omitMessage) {
 		// create Common class instance, if not instantiated yet to allow for events listening
 		if (plugin == null) {
 			new Common();
 		}
 
 		// if we have a player to make god, check if he's online
 		Player p;
 		if (args.length > 0) {
 			p = Bukkit.getServer().getPlayer(args[0]);
 
 			if (p == null) {
 				// requested player not found
 				LogHelper.showWarning("invalidPlayer", sender);
 				return true;
 			}
 		} else {
 			p = (Player)sender;
 		}
 		
 		String pName = p.getName();
 		Boolean showMessages = (omitMessage.length == 0);
 				
 		// check if requested player is in god mode now
 		if ((godPlayers.size() > 0) && godPlayers.contains(pName)) {
 			// cancel god mode for the player
 			godPlayers.remove(pName);
 			// inform the command sender and the player
 			if (showMessages) {
 				if (pName.equals(sender.getName())) {
 					LogHelper.showInfo("godModeCancelledForYou", sender);
 				} else {
 					LogHelper.showInfo("godModeCancelledForYou", p);
 					LogHelper.showInfo("[" + pName + " #####godModeCancelled", sender);
 				}
 			}
 			
 			// if there are no more frozen or god players left, we don't need our event listeners, so unregister them
 			if ((frozenPlayers.size() == 0) && (godPlayers.size() == 0) && (slappedPlayers.size() == 0) && (invisiblePlayers.size() == 0)) {
 				HandlerList.unregisterAll(Common.plugin);
 			}
 			
 			return true;
 		}
 
 		// we are trying to set a god mode to a player
 		// insert player's name into god players' list
 		godPlayers.add(pName);
 		// if we have only this player, activate event listeners, since they're not active yet
 		if ((frozenPlayers.size() == 0) && (slappedPlayers.size() == 0) && (godPlayers.size() == 1) && (invisiblePlayers.size() == 0)) {
 			CommandsEX.plugin.getServer().getPluginManager().registerEvents(Common.plugin, CommandsEX.plugin);
 		}
 		
 		if (showMessages) {
 			// inform both players
 			if (pName.equals(sender.getName())) {
 				LogHelper.showInfo("godModeEnabledForYou", sender);
 			} else {
 				LogHelper.showInfo("godModeEnabledForYou", p);
 				LogHelper.showInfo("[" + Nicknames.getNick(pName) + " #####godModeEnabled", sender);
 			}
 		}
 		
 		return true;
 	}
 	
 	/***
 	 * SLAP - slaps a player, making him fly up in the air and then fall back down
 	 * @param sender
 	 * @param args
 	 * @param command
 	 * @param alias
 	 * @return
 	 */
 	public static Boolean slap(CommandSender sender, String[] args, String command, String alias, Boolean... omitMessage) {
 		// create Common class instance, if not instantiated yet to allow for events listening
 		if (plugin == null) {
 			new Common();
 		}
 
 		// if we have a player to make god, check if he's online
 		Player target = Bukkit.getServer().getPlayer(args[0]);
 		if (target == null) {
 			// requested player not found
 			LogHelper.showWarning("invalidPlayer", sender);
 			return true;
 		}
 		
 		String tName = target.getName();
 		Boolean showMessages = (omitMessage.length == 0);
 
 		// insert player's name into slapped players' list
 		slappedPlayers.add(tName);
 		// if we have only this player, activate event listeners, since they're not active yet
 		if ((frozenPlayers.size() == 0) && (slappedPlayers.size() == 1) && (godPlayers.size() == 0) && (invisiblePlayers.size() == 0)) {
 			CommandsEX.plugin.getServer().getPluginManager().registerEvents(Common.plugin, CommandsEX.plugin);
 		}
 		
 		// now slap him! :)
 		Integer slapHeight = CommandsEX.getConf().getInt("defaultSlapHeight", 20);
 		if (args.length > 1) {
 			try {
 				slapHeight = Integer.parseInt(args[1]);
 			} catch (Throwable e) {}
 		}
 		
 		// store player's original position, so we can return him safely back (in case he's underground and another player slaps him to heavens)
 		Location loc = target.getLocation();
 		slappedLastLocations.put(tName, loc);
 		
 		target.setVelocity(new Vector(0, slapHeight, 0));
 		
 		// insure player's safe return home even if they fall into deep water and no damage is done
 		slappedUnslapTasks.put(tName,  Bukkit.getServer().getScheduler().scheduleSyncDelayedTask(CommandsEX.plugin, new Unslap(target, tName), (20 * 25)));
 
 		if (showMessages) {
 			// inform both players
 			if (!sender.getName().equalsIgnoreCase(target.getName())){
 				LogHelper.showInfo("playerYouWereSlapped#####[" + Nicknames.getNick(sender.getName()), target, ChatColor.YELLOW);
 			}
 
 			// Only do this if the command was slap
 			if (command.equalsIgnoreCase("slap")){
 				LogHelper.showInfo("playerSlapped#####[" + Nicknames.getNick(tName), sender);
 			}
 		}
 
 		return true;
 	}
 
 	/***
 	 * INV - makes a player invisible
 	 * @param sender
 	 * @param args
 	 * @param command
 	 * @param alias
 	 * @return
 	 */
 	public static Boolean inv(CommandSender sender, String[] args, String command, String alias, Boolean... omitMessage) {
 		// create Common class instance, if not instantiated yet to allow for events listening
 		if (plugin == null) {
 			new Common();
 		}
 
 		Boolean showMessages = (omitMessage.length == 0);
 		Player player = (Player) sender;
 		String pName = player.getName();
 		Boolean isInvisible = invisiblePlayers.contains(pName);
 		
 		// use VanishNoPacket to handle invisibility if found
 		if (CommandsEX.vanishNoPacketPresent){
 			try {
 				VanishNoPacket.toggleVanishSilent(player);
 				isInvisible = VanishNoPacket.isVanished(pName);
 			} catch (Exception e) {
 				if (CommandsEX.getConf().getBoolean("debugMode")){
 					e.printStackTrace();
 				}
 			}
 		} else {
 			EntityPlayer ePlayer = ((CraftPlayer) player).getHandle();
 			
 			if (!isInvisible) {
 				invisiblePlayers.add(pName);
 				((CraftServer) player.getServer()).getHandle().sendAll(
 						new Packet201PlayerInfo(ePlayer.listName, false, 9999));
 				
 				for (Player p : Bukkit.getOnlinePlayers()){
 					p.hidePlayer(player);
 				}
 			} else {
 				invisiblePlayers.remove(pName);
 				((CraftServer) player.getServer()).getHandle().sendAll(
 						new Packet201PlayerInfo(ePlayer.listName, true, ePlayer.ping));
 				for (Player p : Bukkit.getOnlinePlayers()){
 					p.showPlayer(player);
 				}
 			}
 			
 			// if there are no more frozen players left, we don't need our event listeners, so unregister them
 			if ((frozenPlayers.size() == 0) && (godPlayers.size() == 0) && (slappedPlayers.size() == 0) && (invisiblePlayers.size() == 0)) {
 				HandlerList.unregisterAll(Common.plugin);
 			} else {
 				CommandsEX.plugin.getServer().getPluginManager().registerEvents(Common.plugin, CommandsEX.plugin);
 			}
 
 			if (showMessages) {
 				// inform the player
 				if (isInvisible) {
 					LogHelper.showInfo("invYouAreVisible", sender);
 				} else {
 					LogHelper.showInfo("invYouAreInvisible", sender);
 				}
 			}
 		}
 		
 		// send fake messages
 		if (isInvisible){
			if (CommandsEX.getConf().getBoolean("fakeQuitMessage", true)) {
 				for (Player p : Bukkit.getOnlinePlayers()){
 					try {
						Handler_condensejoins.fakeLeaves.add(pName);
						Handler_condensejoins.handleLeave(pName);
 					} catch (Exception ex){
 						// if condense joins is not found, send it manually
 						if (p.hasPermission("cex.seejoins")){
							p.sendMessage(ChatColor.WHITE + pName + " " + ChatColor.YELLOW + _("chatLeaves", p.getName()));
 						}
 					}
 				}
 			}
 		} else {
			if (CommandsEX.getConf().getBoolean("fakeJoinMessage", true)) {
 				for (Player p : Bukkit.getOnlinePlayers()){
 					try {
						Handler_condensejoins.fakeJoins.add(pName);
						Handler_condensejoins.handleJoin(pName);
 					} catch (Exception ex){
 						// if condense joins is not found, send it manually
 						if (p.hasPermission("cex.seeleaves")){
							p.sendMessage(ChatColor.WHITE + pName + " " + ChatColor.YELLOW + _("chatJoins", p.getName()));
 						}
 					}
 				}
 			}
 		}
 		
 		return true;
 	}
 
 	/***
 	 * Function to set a players fly mode without breaking Creative Mode
 	 * @author iKeirNez
 	 * @param target
 	 */
 	
 	public static void setFlyMode(Player target){
 		// make sure we don't break creative flying
 		if (target.getGameMode() != GameMode.CREATIVE){
 			if (target.getAllowFlight()){
 				target.setAllowFlight(false);
 			} else {
 				target.setAllowFlight(true);
 			}
 		} else {
 			target.setAllowFlight(true);
 		}
 	}
 
 	/***
 	 * Function to teleport a player to their original slap location if they leave while being slapped
 	 * @param e
 	 */
 	
 	@EventHandler (priority = EventPriority.LOW)
 	public void slapPlayerQuit(PlayerQuitEvent e){
 		Player p = e.getPlayer();
 		if (slappedPlayers.contains(p.getName())){
 			p.teleport(slappedLastLocations.get(p.getName()));
 			
 			slappedLastLocations.remove(p.getName());
 			slappedPlayers.remove(p.getName());
 		}
 	}
 	
 	/***
 	 * Functions below listen to all events that should be prevented when a player is frozen.
 	 * @param e
 	 * @return
 	 */
 	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
 	public void checkFrozenState(BlockBreakEvent e) {
 		if ((Common.frozenPlayers.size() > 0) && Common.frozenPlayers.contains(e.getPlayer().getName())) {
 			e.setCancelled(true);
 		}
 	}
 	
 	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
 	public void checkFrozenState(BlockDamageEvent e) {
 		if ((Common.frozenPlayers.size() > 0) && Common.frozenPlayers.contains(e.getPlayer().getName())) {
 			e.setCancelled(true);
 		}
 	}
 	
 	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
 	public void checkFrozenState(BlockIgniteEvent e) {
 		if ((Common.frozenPlayers.size() > 0) && Common.frozenPlayers.contains(e.getPlayer().getName())) {
 			e.setCancelled(true);
 		}
 	}
 	
 	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
 	public void checkFrozenState(BlockPlaceEvent e) {
 		if ((Common.frozenPlayers.size() > 0) && Common.frozenPlayers.contains(e.getPlayer().getName())) {
 			e.setCancelled(true);
 		}
 	}
 	
 	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
 	public void checkFrozenState(PlayerBucketEmptyEvent e) {
 		if ((Common.frozenPlayers.size() > 0) && Common.frozenPlayers.contains(e.getPlayer().getName())) {
 			e.setCancelled(true);
 		}
 	}
 	
 	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
 	public void checkFrozenState(PlayerDropItemEvent e) {
 		if ((Common.frozenPlayers.size() > 0) && Common.frozenPlayers.contains(e.getPlayer().getName())) {
 			e.setCancelled(true);
 		}
 	}
 	
 	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
 	public void checkFrozenState(PlayerInteractEvent e) {
 		if ((Common.frozenPlayers.size() > 0) && Common.frozenPlayers.contains(e.getPlayer().getName())) {
 			e.setCancelled(true);
 		}
 	}
 	
 	@EventHandler(priority = EventPriority.LOWEST)
 	public void checkFrozenState(PlayerJoinEvent e) {
 		// if the joining player is invisible, tell them and cancel out the join message
 		if (invisiblePlayers.contains(e.getPlayer().getName())) {
 			LogHelper.showWarning("invYouAreStillInvisible", e.getPlayer());
 			e.setJoinMessage("");
 		}
 		
 		// make all invisible players invisible to our joining player as well
 		for (String p : Common.invisiblePlayers) {
 			Player player = Bukkit.getPlayer(p);
 			if (player != null) {
 				e.getPlayer().hidePlayer(player);
 				// Hide the player from the player list
 				((CraftServer) player.getServer()).getHandle().sendAll(
 						new Packet201PlayerInfo(((CraftPlayer) player).getHandle().listName, false, 9999));
 			}
 		}
 		
 		if ((Common.frozenPlayers.size() > 0) && Common.frozenPlayers.contains(e.getPlayer().getName())) {
 			// if the player is frozen, remind them
 			Player p = e.getPlayer();
 			LogHelper.showInfo("freezeYouAreFrozen1", p);
 			LogHelper.showInfo("freezeYouAreFrozen2", p);
 		}
 	}
 	
 	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
 	public void checkFrozenState(InventoryOpenEvent e) {
 		if ((Common.frozenPlayers.size() > 0) && Common.frozenPlayers.contains(e.getPlayer().getName())) {
 			e.setCancelled(true);
 		}
 	}
 	
 	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
 	public void checkFrozenState(PlayerMoveEvent e) {
 		if ((Common.frozenPlayers.size() > 0) && Common.frozenPlayers.contains(e.getPlayer().getName())) {
 			Common.ignoreTpEvent = true;
 			e.setTo(e.getFrom());
 		}
 	}
 	
 	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
 	public void checkFrozenState(PlayerTeleportEvent e) {
 		if (Common.ignoreTpEvent) {
 			Common.ignoreTpEvent = false;
 			return;
 		}
 		
 		if ((Common.frozenPlayers.size() > 0) && Common.frozenPlayers.contains(e.getPlayer().getName())) {
 			e.setCancelled(true);
 		}
 	}
 	
 	@EventHandler(priority = EventPriority.NORMAL)
 	public void checkDamage(EntityDamageEvent e) {
 		if (!e.getEntityType().equals(EntityType.PLAYER)) return;
 
 		Player p = (Player)e.getEntity();
 		String pName = p.getName();
 		if ((Common.godPlayers.size() > 0) && Common.godPlayers.contains(pName)) {
 			e.setCancelled(true);
 		}
 		
 		if ((Common.slappedPlayers.size() > 0) && Common.slappedPlayers.contains(pName)) {
 			// remove UnSlap task for this player
 			Bukkit.getServer().getScheduler().cancelTask(slappedUnslapTasks.get(pName));
 			slappedUnslapTasks.remove(pName);
 			
 			if (CommandsEX.getConf().getBoolean("slapPreventDamage", true)) {
 				e.setCancelled(true);
 			}
 			
 			// restore player's original position
 			p.teleport(slappedLastLocations.get(pName));
 			
 			// remove records
 			slappedPlayers.remove(pName);
 			slappedLastLocations.remove(pName);
 
 			// stop listening if nobody else is frozen, god or slapped
 			if ((frozenPlayers.size() == 0) && (godPlayers.size() == 0) && (slappedPlayers.size() == 0)) {
 				HandlerList.unregisterAll(Common.plugin);
 			}
 		}
 	}
 	
 	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
 	public void noChatToInvisiblePlayers(AsyncPlayerChatEvent e) {
 		if (Common.invisiblePlayers.size() > 0) return;
 
 		String pName = e.getPlayer().getName();
 		String msg = e.getMessage();
 		Set<Player> removals = Collections.emptySet();
 		for (Player p : e.getRecipients()) {
 			if (Common.invisiblePlayers.contains(e.getPlayer().getName())) {
 				removals.add(p);
 			}
 		}
 		
 		if (removals.size() > 0) {
 			for (Player p : removals) {
 				// show message to the player, but remove him from recipients, so the sender doesn't know he's online
 				LogHelper.showInfo("chatMessageFrom#####[" + pName + ": " + ChatColor.WHITE + msg, p);
 				e.getRecipients().remove(p);
 			}
 		}
 	}
 	
 	@EventHandler (priority = EventPriority.LOW, ignoreCancelled = true)
 	public void onPlayerDeath(PlayerDeathEvent e){
 		Player player = (Player) e.getEntity();
 		// If the player is invisible, don't show their death message
 		if (Common.invisiblePlayers.contains(player.getName())){
 			e.setDeathMessage("");
 		}
 	}
 }
