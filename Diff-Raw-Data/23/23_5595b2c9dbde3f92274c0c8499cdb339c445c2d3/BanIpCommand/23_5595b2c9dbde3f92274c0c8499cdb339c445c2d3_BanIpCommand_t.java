 package me.confuserr.banmanager.Commands;
 
 import java.util.List;
 
 import me.confuserr.banmanager.BanManager;
 
 import org.bukkit.OfflinePlayer;
 import org.bukkit.command.Command;
 import org.bukkit.command.CommandExecutor;
 import org.bukkit.command.CommandSender;
 import org.bukkit.entity.Player;
 
 public class BanIpCommand implements CommandExecutor {
 
 	private BanManager plugin;
 
 	public BanIpCommand(BanManager instance) {
 		plugin = instance;
 	}
 
 	@Override
 	public boolean onCommand(final CommandSender sender, Command command, String commandLabel, String args[]) {
 		if (args.length < 2)
 			return false;
 
 		Player player = null;
 		String playerName = plugin.banMessages.get("consoleName");
 
 		if (sender instanceof Player) {
 			player = (Player) sender;
 			playerName = player.getName();
 			if (!player.hasPermission("bm.banip")) {
 				plugin.sendMessage(player, plugin.banMessages.get("commandPermissionError"));
 				return true;
 			}
 		}
 
 		final String reason = plugin.getReason(args, 1);
 		final String viewReason = plugin.viewReason(reason);
 
 		if (BanManager.ValidateIPAddress(args[0])) {
 			// Its an IP
 			String ip = args[0];
 
 			ban(sender, ip, playerName, reason, viewReason);
 
 		} else {
 
 			final String byName = playerName;
 
 			// Its a player!
 			if (!plugin.usePartialNames) {
 				if (plugin.getServer().getPlayerExact(args[0]) == null) {
 					// Offline player
 					OfflinePlayer offlinePlayer = plugin.getServer().getOfflinePlayer(args[0]);
 
 					final String pName = offlinePlayer.getName();
 
 					plugin.getServer().getScheduler().scheduleAsyncDelayedTask(plugin, new Runnable() {
 
 						public void run() {
 							String ip = plugin.dbLogger.getIP(pName);
 
 							if (ip.isEmpty())
 								plugin.sendMessage(sender, plugin.banMessages.get("ipPlayerOfflineError").replace("[name]", pName));
 							else {
 								// Ok, we have their IP, lets ban it
 								ban(sender, ip, byName, reason, viewReason);
 							}
 						}
 					});
 				} else {
 					// Online
 					Player target = plugin.getServer().getPlayerExact(args[0]);
 
 					final String targetIp = target.getAddress().getAddress().toString();
 
 					plugin.getServer().getScheduler().scheduleAsyncDelayedTask(plugin, new Runnable() {
 
 						public void run() {
 							String ip = plugin.getIp(targetIp);
 
 							ban(sender, ip, byName, reason, viewReason);
 						}
 					});
 				}
 				return true;
 			}
 
 			List<Player> list = plugin.getServer().matchPlayer(args[0]);
 			if (list.size() == 1) {
 				Player target = list.get(0);
 				if (target.getName().equals(playerName)) {
 					plugin.sendMessage(sender, plugin.banMessages.get("ipSelfError"));
 				} else if (target.hasPermission("bm.exempt.banip")) {
 					plugin.sendMessage(sender, plugin.banMessages.get("banExemptError"));
 				} else {
 
 					final String targetIp = target.getAddress().getAddress().toString();
 
 					plugin.getServer().getScheduler().scheduleAsyncDelayedTask(plugin, new Runnable() {
 
 						public void run() {
 							String ip = plugin.getIp(targetIp);
 
 							ban(sender, ip, byName, reason, viewReason);
 						}
 					});
 				}
 			} else if (list.size() > 1) {
 				plugin.sendMessage(sender, plugin.banMessages.get("multiplePlayersFoundError"));
 				return false;
 			} else {
 				// They're offline, lets check the database
 				OfflinePlayer offlinePlayer = plugin.getServer().getOfflinePlayer(args[0]);
 
 				final String pName = offlinePlayer.getName();

 				plugin.getServer().getScheduler().scheduleAsyncDelayedTask(plugin, new Runnable() {
 
 					public void run() {
 						String ip = plugin.dbLogger.getIP(pName);
 
 						if (ip.isEmpty())
 							plugin.sendMessage(sender, plugin.banMessages.get("ipPlayerOfflineError").replace("[name]", pName));
 						else {
 							// Ok, we have their IP, lets ban it
 							ban(sender, ip, byName, reason, viewReason);
 						}
 					}
 				});
 			}
 
 		}
 
 		return true;
 	}
 
	private void ban(CommandSender sender, final String ip, String bannedByName, String reason, String viewReason) {
 
		final String kick = plugin.banMessages.get("ipBanKick").replace("[ip]", ip).replace("[reason]", viewReason).replace("[by]", bannedByName);
 
		plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, new Runnable() {
			public void run() {
				for (Player onlinePlayer : plugin.getServer().getOnlinePlayers()) {
					if (plugin.getIp(onlinePlayer.getAddress().toString()).equals(ip)) {

						onlinePlayer.kickPlayer(kick);
					}
				}
			}
		});
 
 		if (plugin.bukkitBan)
 			plugin.getServer().banIP(ip);
 
 		plugin.dbLogger.logIpBan(ip, bannedByName, reason);
 		plugin.logger.info(plugin.banMessages.get("ipBanned").replace("[ip]", ip));
 
 		if (!sender.hasPermission("bm.notify"))
 			plugin.sendMessage(sender, plugin.banMessages.get("ipBanned").replace("[ip]", ip));
 
 		String message = plugin.banMessages.get("ipBan").replace("[ip]", ip).replace("[reason]", viewReason).replace("[by]", bannedByName);
 		plugin.sendMessageWithPerm(message, "bm.notify");
 	}
 }
