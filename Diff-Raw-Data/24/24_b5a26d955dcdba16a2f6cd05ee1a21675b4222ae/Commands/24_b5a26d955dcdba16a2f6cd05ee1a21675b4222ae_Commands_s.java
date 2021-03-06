 package com.zavteam.plugins;
 
 import java.util.ArrayList;
 import java.util.HashMap;
 import java.util.List;
 import java.util.Map;
 
 import org.bukkit.ChatColor;
 import org.bukkit.command.Command;
 import org.bukkit.command.CommandExecutor;
 import org.bukkit.command.CommandSender;
 import org.bukkit.entity.Player;
 import org.bukkit.util.ChatPaginator;
 
 import com.zavteam.plugins.messageshandler.MessagesHandler;
 
 public class Commands implements CommandExecutor {
 	private final static String noPerm = ChatColor.RED + "You do not have permission to do this.";
 	public ZavAutoMessager plugin;
 	public Commands(ZavAutoMessager instance) {
 		plugin = instance;
 	}
 	@Override
 	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
 		String freeVariable;
 		if (args.length == 0 || args[0].equalsIgnoreCase("help")) {
 			if (sender.hasPermission("zavautomessager.view")) {
 				if (args.length == 1 || args.length == 0) {
 					MessagesHandler.listHelpPage(1, sender);
 				} else {
 					try {
 						Integer.parseInt(args[1]);
 					} catch (NumberFormatException e) {
 						sender.sendMessage(ChatColor.RED + "You need to enter a valid page number to do this.");
 					}
 					if (Integer.parseInt(args[1]) > 0 && Integer.parseInt(args[1]) < 4) {
 						MessagesHandler.listHelpPage(Integer.parseInt(args[1]), sender);
 					} else {
 						sender.sendMessage(ChatColor.RED + "That is not a valid page number");
 					}
 				}
 			} else {
 				sender.sendMessage(noPerm);
 			}
 		} else if (args.length >= 1) {
 			if (args[0].equalsIgnoreCase("reload")) {
 				if (sender.hasPermission("zavautomessager.reload")) {
 					plugin.messageIt = 0;
 					plugin.mainConfig.saveConfig();
 					plugin.ignoreConfig.saveConfig();
 					plugin.autoReload();
 					sender.sendMessage(ChatColor.GREEN + "ZavAutoMessager's config has been reloaded.");
 				} else {
 					sender.sendMessage(noPerm);
 				}
 			} else if (args[0].equalsIgnoreCase("on")) {
 				if (sender.hasPermission("zavautomessager.toggle")) {
 					if ((Boolean) plugin.mainConfig.get("enabled", true)) {
 						sender.sendMessage(ChatColor.RED + "Messages are already enabled");
 					} else {
 						plugin.mainConfig.set("enabled", true);
 						plugin.mainConfig.saveConfig();
 						sender.sendMessage(ChatColor.GREEN + "ZavAutoMessager is now on");
 					}
 				} else {
 					sender.sendMessage(noPerm);
 				}
 			} else if (args[0].equalsIgnoreCase("off")) {
 				if (sender.hasPermission("zavautomessager.toggle")) {
 					if (!(Boolean) plugin.mainConfig.get("enabled", true)) {
 						sender.sendMessage(ChatColor.RED + "Messages are already disabled");
 					} else {
 						plugin.mainConfig.set("enabled", false);
 						plugin.mainConfig.saveConfig();
 						sender.sendMessage(ChatColor.GREEN + "ZavAutoMessager is now off");
 					}
 				} else {
 					sender.sendMessage(noPerm);
 				}
 			} else if (args[0].equalsIgnoreCase("add")) {
 				if (sender.hasPermission("zavautomessager.add")) {
 					if (args.length < 2) {
 						sender.sendMessage(ChatColor.RED + "You need to enter a chat message to add.");
 					} else {
 						freeVariable = "";
 						for (int i = 1; i < args.length; i++) {
 							freeVariable = freeVariable + args[i] + " ";
 						}
 						freeVariable = freeVariable.trim();
 						plugin.messageIt = 0;
 						plugin.MessagesHandler.addMessage(freeVariable);
 						sender.sendMessage(ChatColor.GREEN + "Your message has been added to the message list.");
 					}
 				} else {
 					sender.sendMessage(noPerm);
 				}
 			} else if (args[0].equalsIgnoreCase("ignore")) {
 				if (sender instanceof Player) {
 					if (sender.hasPermission("zavautomessager.ignore")) {
 						List<String> ignorePlayers;
 						ignorePlayers = plugin.ignoreConfig.getConfig().getStringList("players");
 						if (ignorePlayers.contains(sender.getName())) {
 							ignorePlayers.remove(sender.getName());
 							sender.sendMessage(ChatColor.GREEN + "You are no longer ignoring automatic messages");
 						} else {
 							ignorePlayers.add(sender.getName());
 							sender.sendMessage(ChatColor.GREEN + "You are now ignoring automatic messages");
 						}
 						plugin.ignoreConfig.set("players", ignorePlayers);
 						plugin.ignoreConfig.saveConfig();
 					} else {
 						sender.sendMessage(noPerm);
 					}
 				} else {
 					ZavAutoMessager.log.info("The console cannot use this command.");
 				}
 			} else if (args[0].equalsIgnoreCase("broadcast")) {
 				String[] cutBroadcastList = new String[1];
 				if (sender.hasPermission("zavautomessager.broadcast")) {
 					if (args.length < 2) {
 						sender.sendMessage(ChatColor.RED + "You must enter a broadcast message");
 					} else {
 						cutBroadcastList[0] = "";
 						for (int i = 1; i < args.length; i++) {
 							cutBroadcastList[0] = cutBroadcastList[0] + args[i] + " ";
 						}
 						cutBroadcastList[0] = cutBroadcastList[0].trim();
 						cutBroadcastList[0] = ((String) plugin.mainConfig.get("chatformat", "[&6AutoMessager&f]: %msg")).replace("%msg", cutBroadcastList[0]);
 						cutBroadcastList[0] = cutBroadcastList[0].replace("&", "\u00A7");
 						cutBroadcastList = ChatPaginator.paginate(cutBroadcastList[0], 1).getLines();
 						plugin.MessagesHandler.handleChatMessage(cutBroadcastList, null);
 					}
 				} else {
 					sender.sendMessage(noPerm);
 				}
 			} else if (args[0].equalsIgnoreCase("about")) {
 				if (sender.hasPermission("zavautomessager.about")) {
 					sender.sendMessage(ChatColor.GOLD + "You are currently running ZavAutoMessage Version " + plugin.getDescription().getVersion() + ".");
					sender.sendMessage(ChatColor.GOLD + "The latest version is currently version " + plugin.updater.getLatestGameVersion() + ".");
 					sender.sendMessage(ChatColor.GOLD + "This plugin was developed by the ZavCodingTeam.");
 					sender.sendMessage(ChatColor.GOLD + "Please visit our Bukkit Dev Page for complete details on this plugin.");
 					sender.sendMessage(ChatColor.GOLD + "http://dev.bukkit.org/server-mods/zavautomessager/");
 				} else {
 					sender.sendMessage(noPerm);
 				}
 			} else if (args[0].equalsIgnoreCase("remove")) {
 				if (sender.hasPermission("zavautomessager.remove")) {
 					if (args.length < 2) {
 						sender.sendMessage(ChatColor.RED + "You need to enter a message number to delete.");
 					} else {
 						try {
 							Integer.parseInt(args[1]);
 						} catch (NumberFormatException e) {
 							sender.sendMessage(ChatColor.RED + "You have to enter a round number to remove.");
 							return false;
 						}
 						if (Integer.parseInt(args[1]) < 0 || Integer.parseInt(args[1]) > plugin.messages.size() || plugin.messages.size() == 1) {
 							sender.sendMessage(ChatColor.RED + "This is not a valid message number");
 							sender.sendMessage(ChatColor.RED + "Use /automessager list for a list of messages");
 						} else {
 							plugin.messages.remove(Integer.parseInt(args[1]) - 1);
 							sender.sendMessage(ChatColor.GREEN + "Your message has been removed.");
 							Map<String, List<String>> list = new HashMap<String, List<String>>();
 							for (ChatMessage cm : plugin.messages) {
 								if (list.containsKey(cm.getPermission())) {
 									list.get(cm.getPermission()).add(cm.getMessage());
 								} else {
 									List<String> mList = new ArrayList<String>();
 									mList.add(cm.getMessage());
 									list.put(cm.getPermission(), mList);
 								}
 							}
 							plugin.mainConfig.set("messages", list);
 							plugin.mainConfig.saveConfig();
 							plugin.messageIt = 0;
 							plugin.autoReload();
 						}
 					}
 				} else {
 					sender.sendMessage(noPerm);
 				}
 			} else if (args[0].equalsIgnoreCase("list")) {
 				if (sender.hasPermission("zavautomessager.list")) {
 					if (args.length < 2) {
 						plugin.MessagesHandler.listPage(1, sender);
 						return true;
 					}
 					try {
 						plugin.messages.get((5 * Integer.parseInt(args[1])) - 5);
 					} catch (IndexOutOfBoundsException e) {
 						sender.sendMessage(ChatColor.RED + "You do not have that any messages on that page");
 						return false;
 					} catch (Exception e) {
 						sender.sendMessage(ChatColor.RED + "You have to enter an invalid number to show help page.");
 						return false;
 					}
 					plugin.MessagesHandler.listPage(Integer.parseInt(args[1]), sender);
 				} else {
 					sender.sendMessage(noPerm);
 				}
 			} else if (args[0].equalsIgnoreCase("set")) { 
 				if (sender.hasPermission("zavautomessager.set")) {
 					if (args.length < 2) {
 						sender.sendMessage(ChatColor.RED + "A minimum of two parameters are required to set any configuration section.");
 						return false;
 					}
 					if (ConfigSection.contains(args[1].toUpperCase())) {
 						Boolean b = false;
 						switch (ConfigSection.valueOf(args[1].toUpperCase())) {
 						case DONTREPEATRANDOMMESSAGES:// Intentional fallthrough since all are pretty much the same
 						case ENABLED:
 						case MESSAGEINRANDOMORDER:
 						case MESSAGESINCONSOLE:
 						case PERMISSIONSENABLED:
 						case REQUIREPLAYERSONLINE:
 						case UPDATECHECKING:
 						case WORDWRAP:
 							try {
 								plugin.mainConfig.set(args[1].toLowerCase(), Boolean.parseBoolean(args[2]));
 								b = true;
 							} catch (Exception e) {
 								sender.sendMessage(ChatColor.RED + "These Config Sections require the type of boolean (true or false).");
 							}
 							break;
 						case DELAY:
 							try {
 								plugin.mainConfig.set(args[1].toLowerCase(), Integer.parseInt(args[2]));
 								b = true;
 							} catch (Exception e) {
 								sender.sendMessage(ChatColor.RED + "These Config Sections require the type of Integer.");
 							}
 							break;
 						default:
 							break;
 
 						}
 						if (b) {
 							sender.sendMessage(ChatColor.GOLD + args[1] + " has been set to " + args[2] + ".");
 						}
 						plugin.mainConfig.saveConfig();
 						plugin.ignoreConfig.saveConfig();
 						plugin.autoReload();
 						return true;
 					} else if (args[1].equalsIgnoreCase("list")) {
 						String s = "";
 						for (ConfigSection cs : ConfigSection.values()) {
 							s = s + " " + cs.name().toLowerCase();
 						}
 						s = s + ".";
 						s = s.trim();
 						sender.sendMessage(ChatColor.GOLD + "Config Sections: " + s);
 					} else {
 						sender.sendMessage(ChatColor.RED + "That is an invalid configuration section");
 						return false;
 					}
 				} else {
 					sender.sendMessage(noPerm);
 				}
 			} else if (args[0].equalsIgnoreCase("raw")) { 
 				String[] cutBroadcastList = new String[1];
 				if (sender.hasPermission("zavautomessager.broadcast")) {
 					if (args.length < 2) {
 						sender.sendMessage(ChatColor.RED + "You must enter a broadcast message");
 					} else {
 						cutBroadcastList[0] = "";
 						for (int i = 1; i < args.length; i++) {
 							cutBroadcastList[0] = cutBroadcastList[0] + args[i] + " ";
 						}
 						cutBroadcastList[0] = cutBroadcastList[0].trim();
 						plugin.MessagesHandler.handleChatMessage(cutBroadcastList, null);
 					}
 				} else {
 					sender.sendMessage(noPerm);
 				}
 			}else {
 				sender.sendMessage(ChatColor.RED + "ZavAutoMessager did not recognize this command.");
 				sender.sendMessage(ChatColor.RED + "Use /automessager help to get a list of commands!");
 			}
 		}
 		return false;
 	}
 }
