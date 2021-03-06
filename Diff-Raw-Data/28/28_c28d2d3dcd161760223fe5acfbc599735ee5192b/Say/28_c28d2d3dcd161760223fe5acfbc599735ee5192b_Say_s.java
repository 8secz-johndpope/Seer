 package org.monstercraft.irc.plugin.command.gamecommands;
 
 import org.bukkit.command.CommandSender;
 import org.bukkit.entity.Player;
 import org.monstercraft.irc.MonsterIRC;
 import org.monstercraft.irc.plugin.command.GameCommand;
 import org.monstercraft.irc.plugin.util.ChatType;
 import org.monstercraft.irc.plugin.util.IRCColor;
 import org.monstercraft.irc.plugin.util.StringUtils;
 import org.monstercraft.irc.plugin.util.Variables;
 import org.monstercraft.irc.plugin.wrappers.IRCChannel;
 
 import com.gmail.nossr50.mcPermissions;
 
 public class Say extends GameCommand {
 
 	@Override
 	public boolean canExecute(CommandSender sender, String[] split) {
 		return split[0].equalsIgnoreCase("irc")
 				&& split[1].equalsIgnoreCase("say");
 	}
 
 	@Override
 	public boolean execute(CommandSender sender, String[] split) {
 		if (sender instanceof Player) {
 			if (MonsterIRC.getHandleManager().getPermissionsHandler() != null) {
 				if (!MonsterIRC.getHandleManager().getPermissionsHandler()
 						.hasCommandPerms(((Player) sender), this)) {
 					sender.sendMessage("[IRC] You don't have permission to preform that command.");
 					return true;
 				}
 			} else {
 				sender.sendMessage("[IRC] PEX not detected, unable to run any IRC commands.");
 				return true;
 			}
 		}
 		if (split.length <= 2) {
 			sender.sendMessage("Invalid usage!");
 			sender.sendMessage("Proper usage: irc say -c:[irc channel] [message]");
 			sender.sendMessage("or");
 			sender.sendMessage("Proper usage: irc say [message]");
 			return true;
 		}
 		String channel = null;
 		int j = 2;
 		if (split[2].startsWith("-c:")) {
 			String s = split[2].toString();
 			channel = s.substring(3);
 			j = 3;
 		}
 		StringBuffer result = new StringBuffer();
 		StringBuffer result2 = new StringBuffer();
 		result.append("<" + sender.getName() + "> ");
 		for (int i = j; i < split.length; i++) {
 			result.append(split[i]);
 			result.append(" ");
 			result2.append(split[i]);
 			result2.append(" ");
 		}
 
 		for (IRCChannel c : Variables.channels) {
 			if (channel != null) {
 				if (c.getChannel().equalsIgnoreCase(channel)) {
 					MonsterIRC.getHandleManager().getIRCHandler()
 							.sendMessage(c.getChannel(), result.toString());
 					handleMessage(c, sender.getName(), result2.toString());
 					break;
 				}
 			} else {
 				if (c.isDefaultChannel()) {
 					MonsterIRC.getHandleManager().getIRCHandler()
 							.sendMessage(c.getChannel(), result.toString());
 					handleMessage(c, sender.getName(), result2.toString());
 				}
 			}
 		}
 		return true;
 	}
 
 	private void handleMessage(IRCChannel c, String name, String message) {
 		if (c.getChatType() == ChatType.ADMINCHAT) {
 			if (MonsterIRC.getHookManager().getmcMMOHook() != null) {
 				String format = "b" + "{" + "f" + "[IRC] " + name + "b"
 						+ "} " + message;
				for (Player p : getServer().getOnlinePlayers()) {
 					if (p.isOp() || mcPermissions.getInstance().adminChat(p))
 						p.sendMessage(format);
 				}
 			}
 		} else if (c.getChatType() == ChatType.HEROCHAT && !Variables.hc4) {
 			c.getHeroChatChannel().announce(
 					Variables.mcformat
 							.replace("{name}", StringUtils.getName(name))
 							.replace("{message}",
 									IRCColor.formatIRCMessage(message))
 							.replace("{prefix}", StringUtils.getPrefix(name))
 							.replace("{suffix}", StringUtils.getSuffix(name))
 							.replace("{groupPrefix}",
 									StringUtils.getGroupPrefix(name))
 							.replace("{groupSuffix}",
 									StringUtils.getGroupSuffix(name))
 							.replace("{world}", StringUtils.getWorld(name))
 							.replace("&", "")
 							+ c.getHeroChatChannel().getColor());
 		} else if (c.getChatType() == ChatType.HEROCHAT
 				&& MonsterIRC.getHookManager().getHeroChatHook() != null
 				&& Variables.hc4) {
 			c.getHeroChatFourChannel().sendMessage(
 					Variables.mcformat
 							.replace("{name}", StringUtils.getName(name))
 							.replace("{message}", "")
 							.replace("{colon}", "")
 							.replace("{prefix}", StringUtils.getPrefix(name))
 							.replace("{suffix}", StringUtils.getSuffix(name))
 							.replace("{groupPrefix}",
 									StringUtils.getGroupPrefix(name))
 							.replace("{groupSuffix}",
 									StringUtils.getGroupSuffix(name))
 							.replace("{world}", StringUtils.getWorld(name))
 							.replace("&", ""),
 					IRCColor.formatIRCMessage(IRCColor
 							.formatIRCMessage(message)),
 					c.getHeroChatFourChannel().getMsgFormat(), false);
 		} else if (c.getChatType() == ChatType.GLOBAL) {
			getServer().broadcastMessage(
 					Variables.mcformat
 							.replace("{name}", StringUtils.getName(name))
 							.replace("{message}",
 									IRCColor.formatIRCMessage(message))
 							.replace("{prefix}", StringUtils.getPrefix(name))
 							.replace("{suffix}", StringUtils.getSuffix(name))
 							.replace("{groupPrefix}",
 									StringUtils.getGroupPrefix(name))
 							.replace("{groupSuffix}",
 									StringUtils.getGroupSuffix(name))
 							.replace("&", "")
 							.replace("{world}", StringUtils.getWorld(name))
 							+ "f");
 		}
 	}
 
 	@Override
 	public String getPermissions() {
 		return "irc.say";
 	}
 
 }
