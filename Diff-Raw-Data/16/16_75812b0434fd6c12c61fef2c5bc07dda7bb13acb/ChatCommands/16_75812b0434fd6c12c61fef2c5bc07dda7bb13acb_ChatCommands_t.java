 package ben_dude56.plugins.bencmd.chat;
 
 import java.util.logging.Logger;
 
 import net.minecraft.server.EntityHuman;
 
 import org.bukkit.ChatColor;
 import org.bukkit.command.Command;
 import org.bukkit.command.CommandSender;
 import org.bukkit.craftbukkit.entity.CraftPlayer;
 import org.bukkit.entity.Player;
 
 import ben_dude56.plugins.bencmd.BenCmd;
 import ben_dude56.plugins.bencmd.Commands;
 import ben_dude56.plugins.bencmd.User;
 import ben_dude56.plugins.bencmd.permissions.PermissionUser;
 
 public class ChatCommands implements Commands {
 	BenCmd plugin;
 	Logger log = Logger.getLogger("minecraft");
 
 	public ChatCommands(BenCmd instance) {
 		plugin = instance;
 	}
 
 	public boolean channelsEnabled() {
 		return plugin.mainProperties.getBoolean("channelsEnabled", false);
 	}
 
 	public boolean onCommand(CommandSender sender, Command command,
 			String commandLabel, String[] args) {
 		User user;
 		try {
 			user = User.getUser(plugin, (Player) sender);
 		} catch (ClassCastException e) {
 			user = User.getUser(plugin);
 		}
 		if (commandLabel.equalsIgnoreCase("tell")) {
 			tell(args, user);
 			return true;
 		} else if (commandLabel.equalsIgnoreCase("list")
 				&& user.hasPerm("canListPlayers")) {
 			list(args, user);
 			return true;
 		} else if (commandLabel.equalsIgnoreCase("display")
 				&& user.hasPerm("canChangeDisplayName")) {
 			user.sendMessage("TEST");
 			User user2 = User.matchUser(args[0], plugin);
 			String message = "";
 			for (int i = 1; i < args.length; i++) {
 				String word = args[i];
 				if (message == "") {
 					message += word;
 				} else {
 					message += " " + word;
 				}
 			}
 			user2.getHandle().setDisplayName(message);
 			((EntityHuman)((CraftPlayer)user2.getHandle()).getHandle()).name = message;
 		}
 		if (channelsEnabled()) {
 			return false;
 		}
 		if (commandLabel.equalsIgnoreCase("slow")
 				&& user.hasPerm("canSlowMode")) {
 			slowMode(args, user);
 			return true;
 		} else if (commandLabel.equalsIgnoreCase("mute")
 				&& user.hasPerm("canMute")) {
 			mute(args, user);
 			return true;
 		} else if (commandLabel.equalsIgnoreCase("unmute")
 				&& user.hasPerm("canMute")) {
 			unmute(args, user);
 			return true;
 		} else if (commandLabel.equalsIgnoreCase("me")) {
 			me(args, user);
 			return true;
 		}
 		return false;
 	}
 
 	public void slowMode(String[] args, User user) {
 		if ((!plugin.chatListen.slow.isEnabled()) && args.length > 0) {
 			int millis;
 			try {
 				millis = Integer.parseInt(args[0]);
 			} catch (NumberFormatException e) {
 				user.sendMessage(ChatColor.RED + "Invalid delay!");
 				return;
 			}
 			plugin.chatListen.slow.EnableSlow(millis);
 		} else {
 			plugin.chatListen.ToggleSlow(user);
 		}
 	}
 
 	public void mute(String[] args, User user) {
 		if (args.length != 1) {
 			user.sendMessage(ChatColor.YELLOW + "Proper usage: /mute <player>");
 			return;
 		}
 		PermissionUser user2;
 		if ((user2 = PermissionUser.matchUserIgnoreCase(args[0], plugin)) == null) {
 			user.sendMessage(ChatColor.RED + "That user doesn't exist!");
 			return;
 		}
 		if(user.hasPerm("isMuted", false)) {
 			user.sendMessage(ChatColor.RED + "That user is already muted!");
 		} else {
			user2.addPermission("isMuted");
 			user.sendMessage(ChatColor.GREEN + "That user was muted!");
 			log.info("User " + args[0] + " has been muted by " + user.getName()
 					+ ".");
 			User user3;
 			if ((user3 = User.matchUser(user2.getName(), plugin)) != null) {
 				user3.sendMessage(ChatColor.RED + "You have been muted.");
 			}
 		}
 	}
 
 	public void list(String[] args, User user) {
 		Player[] playerList = plugin.getServer().getOnlinePlayers();
 		if (playerList.length == 1 && !user.isServer()) {
 			user.sendMessage(ChatColor.GREEN
 					+ "You are the only one online. :(");
 		} else {
 			String playerString = "";
 			for (Player player2 : playerList) {
 				User user2 = User.getUser(plugin, player2);
 				if (user2.isOffline() && !user.isServer()) {
 					continue;
 				}
 				playerString += user2.getColor() + user2.getName()
 						+ ChatColor.WHITE + ", ";
 			}
 			user.sendMessage("The following players are online: "
 					+ playerString);
 		}
 	}
 
 	public void unmute(String[] args, User user) {
 		if (args.length != 1) {
 			user.sendMessage(ChatColor.YELLOW
 					+ "Proper usage: /unmute <player>");
 			return;
 		}
 		PermissionUser user2;
 		if ((user2 = PermissionUser.matchUserIgnoreCase(args[0], plugin)) == null) {
 			user.sendMessage(ChatColor.RED + "That user doesn't exist!");
 			return;
 		}
 		if(user2.hasPerm("isMuted", false)) {
 			user.sendMessage(ChatColor.GREEN + "That user was unmuted!");
 			log.info("User " + args[0] + " has been unmuted by "
 					+ user.getDisplayName() + ".");
 			User user3;
 			if ((user3 = User.matchUser(user2.getName(), plugin)) != null) {
 				user3.sendMessage(ChatColor.GREEN + "You have been unmuted.");
 			}
 		} else {
 			user.sendMessage(ChatColor.RED + "That user isn't muted!");
 		}
 	}
 
 	public void me(String[] args, User user) {
 		if (args.length == 0) {
 			user.sendMessage(ChatColor.YELLOW + "Proper usage: /me <message>");
 			return;
 		}
 		if (user.isMuted()) {
 			user.sendMessage(ChatColor.GRAY
 					+ plugin.mainProperties.getString("muteMessage",
 							"You are muted..."));
 			return;
 		}
 		String message = "";
 		for (String word : args) {
 			if (message == "") {
 				message += word;
 			} else {
 				message += " " + word;
 			}
 		}
 		boolean blocked = ChatChecker.checkBlocked(message, plugin);
 		if (blocked) {
 			user.sendMessage(ChatColor.GRAY
 					+ plugin.mainProperties.getString("blockMessage",
 							"You used a blocked word..."));
 			return;
 		}
 		long slowTimeLeft = plugin.chatListen.slow
 				.playerBlocked(user.getName());
 		if (!user.hasPerm("ignoreSlowMode")
 				&& plugin.chatListen.slow.isEnabled()) {
 			if (slowTimeLeft > 0) {
 				user.sendMessage(ChatColor.GRAY
 						+ "Slow mode is enabled! You must wait "
 						+ (int) Math.ceil(slowTimeLeft / 1000)
 						+ " more second(s) before you can talk again.");
 				return;
 			} else {
 				plugin.chatListen.slow.playerAdd(user.getName());
 			}
 		}
 		message = ChatColor.WHITE + "*" + user.getColor() + user.getDisplayName()
 				+ " " + ChatColor.WHITE + message;
 		plugin.getServer().broadcastMessage(message);
 		User.getUser(plugin).sendMessage(message);
 	}
 
 	public void tell(String[] args, User user) {
 		if (args.length <= 1) {
 			user.sendMessage(ChatColor.YELLOW
 					+ "Proper usage: /tell <player> <message>");
 			return;
 		}
 		if (user.isMuted()) {
 			user.sendMessage(ChatColor.GRAY
 					+ plugin.mainProperties.getString("muteMessage",
 							"You are muted..."));
 			return;
 		}
 		User user2;
 		if ((user2 = User.matchUser(args[0], plugin)) == null) {
 			user.sendMessage(ChatColor.RED + "That user doesn't exist!");
 			return;
 		}
 		if (user2.getName().equalsIgnoreCase(user.getName())) {
 			user.sendMessage(ChatColor.RED
 					+ "Are you trying to talk to yourself!?");
 			return;
 		}
 		String message = "";
 		for (int i = 0; i < args.length; i++) {
 			if (i == 0) {
 				continue;
 			}
 			String word = args[i];
 			if (message == "") {
 				message += word;
 			} else {
 				message += " " + word;
 			}
 		}
 		boolean blocked = ChatChecker.checkBlocked(message, plugin);
 		if (blocked) {
 			user.sendMessage(ChatColor.GRAY
 					+ plugin.mainProperties.getString("blockMessage",
 							"You used a blocked word..."));
 			return;
 		}
 		long slowTimeLeft = plugin.chatListen.slow
 				.playerBlocked(user.getName());
 		if (!user.hasPerm("ignoreSlowMode")
 				&& plugin.chatListen.slow.isEnabled()) {
 			if (slowTimeLeft > 0) {
 				user.sendMessage(ChatColor.GRAY
 						+ "Slow mode is enabled! You must wait "
 						+ (int) Math.ceil(slowTimeLeft / 1000)
 						+ " more second(s) before you can talk again.");
 				return;
 			} else {
 				plugin.chatListen.slow.playerAdd(user.getName());
 			}
 		}
 		user2.sendMessage(user.getColor() + user.getDisplayName() + ChatColor.GRAY
 				+ " has whispered: " + message);
 		user.sendMessage(ChatColor.GREEN + "Your PM to " + user2.getDisplayName()
 				+ " was sent!");
 		log.info("(" + user.getDisplayName() + " => " + user2.getDisplayName() + ") "
 				+ message);
 	}
 }
