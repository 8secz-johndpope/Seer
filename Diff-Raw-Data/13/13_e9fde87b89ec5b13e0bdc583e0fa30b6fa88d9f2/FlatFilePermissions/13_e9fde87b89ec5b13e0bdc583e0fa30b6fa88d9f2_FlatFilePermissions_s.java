 package de.hydrox.bukkit.DroxPerms.data.flatfile;
 
 import java.io.File;
 import java.util.ArrayList;
 import java.util.HashMap;
 import java.util.Iterator;
 import java.util.List;
 import java.util.Map;
 
 import org.bukkit.ChatColor;
 import org.bukkit.command.CommandSender;
 import org.bukkit.entity.Player;
 import org.bukkit.plugin.Plugin;
 import org.bukkit.util.config.Configuration;
 import org.bukkit.util.config.ConfigurationNode;
 
 import de.hydrox.bukkit.DroxPerms.data.IDataProvider;
 
 /**
  * 
  * @author Matthias Söhnholz
  *
  */
 public class FlatFilePermissions implements IDataProvider {
 
 	public static final String NODE = "FlatFile";
 	protected static Plugin plugin = null;
 
 	private Configuration groupsConfig;
 	private Configuration usersConfig;
 	private Configuration tracksConfig;
 
 
 	public FlatFilePermissions() {
 		groupsConfig = new Configuration(new File("groupsConfig.yml"));
 		usersConfig = new Configuration(new File("usersConfig.yml"));
 		tracksConfig = new Configuration(new File("tracksConfig.yml"));
 	}
 
 	public FlatFilePermissions(Plugin plugin) {
 		FlatFilePermissions.plugin = plugin;
 		// Write some default configuration
 
 		groupsConfig = new Configuration(new File(plugin.getDataFolder(), "groups.yml"));
 		usersConfig = new Configuration(new File(plugin.getDataFolder(), "users.yml"));
 		tracksConfig = new Configuration(new File(plugin.getDataFolder(), "tracks.yml"));
 		if (!new File(plugin.getDataFolder(), "groups.yml").exists()) {
 			plugin.getServer().getLogger().info("[DroxPerms] Generating default groups.yml");
 			HashMap<String,Object> tmp = new HashMap<String,Object>();
 			tmp.put("default", new Group("default").toConfigurationNode());
 
 			groupsConfig.setProperty("groups", tmp);
 			groupsConfig.save();
 		}
 
 		groupsConfig.load();
 		//		System.out.println(groupsConfig.getKeys().toString());
 		Map<String, ConfigurationNode> groups = groupsConfig.getNodes("groups");
 		Iterator<String> iter = groups.keySet().iterator();
 		while (iter.hasNext()) {
 			String key = iter.next();
 			plugin.getServer().getLogger().fine("load group: " + key);
 			ConfigurationNode conf = groups.get(key);
 			Group newGroup = new Group(key, conf);
 			Group.addGroup(newGroup);
 		}
 
 		if (!new File(plugin.getDataFolder(), "users.yml").exists()) {
 			plugin.getServer().getLogger().info("[DroxPerms] Generating default users.yml");
 			HashMap<String,Object> tmp = new HashMap<String,Object>();
 			usersConfig.setProperty("users", tmp);
 			usersConfig.save();
 		}
 
 		usersConfig.load();
 		//		System.out.println(usersConfig.getKeys().toString());
 		String fileVersion = usersConfig.getString("fileversion");
 		if (fileVersion == null || !fileVersion.equals(plugin.getDescription().getVersion())) {
 			plugin.getServer().getLogger().info("[DroxPerms] users.yml-version to old or unknown. Doing full conversion");
 			usersConfig.setProperty("fileversion", plugin.getDescription().getVersion());
 			Map<String, ConfigurationNode> users = usersConfig.getNodes("users");
 			iter = users.keySet().iterator();
 			while (iter.hasNext()) {
 				String key = iter.next();
 				plugin.getServer().getLogger().fine("load user: " + key);
 				ConfigurationNode conf = users.get(key);
 				User newUser = new User(key, conf);
 				newUser.dirty();
 				User.addUser(newUser);
 			}
 		}
 
 		tracksConfig.load();
 		Map<String, ConfigurationNode> tracks = tracksConfig.getNodes("tracks");
 		if(tracks !=null){
 			iter = tracks.keySet().iterator();
 			while (iter.hasNext()) {
 				String key = iter.next();
 				plugin.getServer().getLogger().fine("load track: " + key);
 				ConfigurationNode conf = tracks.get(key);
 				Track newTrack = new Track(key, conf);
 				Track.addTrack(newTrack);
 			}
 		}
 	}
 
 	public void save() {
 		HashMap<String,Object> tmp = new HashMap<String,Object>();
 		Iterator<Group> iter = Group.iter();
 		while (iter.hasNext()) {
 			Group group = iter.next();
 			tmp.put(group.getName().toLowerCase(), group.toConfigurationNode());
 		}
 
 		groupsConfig.setProperty("groups", tmp);
 		groupsConfig.save();
 
 		tmp = new HashMap<String,Object>();
 		Iterator<User> iter2 = User.iter(); 
 		while (iter2.hasNext()) {
 			User user = iter2.next();
 			if (user.isDirty()) {
 				usersConfig.getNode("users").setProperty(user.getName(), user.toConfigurationNode());
 				user.clean();
 			}
 		}
 		usersConfig.save();
 	}
 
 	public boolean createPlayer(String name) {
 		if (User.existUser(name)) {
 			return false;
 		} else {
 			User.addUser(new User(name));
 			return true;
 		}
 	}
 
 	public boolean deletePlayer(CommandSender sender, String name) {
 		Player player = plugin.getServer().getPlayerExact(name);
 		if (player != null) {
 			sender.sendMessage(ChatColor.RED + "Can't delete online Player.");
 			return false;
 		}
 		User user = getExactUser(name);
 		if (user != null) {
 			User.removeUser(name);
 			usersConfig.getNode("users").removeProperty(name);
 			sender.sendMessage(ChatColor.GREEN + "Deleted Player " + name + ".");
 			return true;
 		}
 		sender.sendMessage(ChatColor.RED + "No Player with this exact name found.");
 
 		return false;
 	}
 
 	public boolean createGroup(CommandSender sender, String name) {
 		if (Group.existGroup(name)) {
 			return false;
 		} else {
 			Group.addGroup(new Group(name));
 			return true;
 		}
 	}
 
 	public String getPlayerGroup(String player) {
 		User user = getUser(player, true);
 		if (user != null) {
 			return user.getGroup();
 		} else {
 			return "";
 		}
 	}
 
 	public boolean setPlayerGroup(CommandSender sender, String player, String group) {
 		User user = getUser(player, true);
 		if (user != null) {
 			boolean result = user.setGroup(group);
 			if (result) {
 				sender.sendMessage(ChatColor.GREEN + "Set group of player " + user.getName() + " to " + group);
 				return true;
 			} else {
 				sender.sendMessage(ChatColor.RED + "Couldn't set group of player " + user.getName());
 				return false;
 			}
 		} else {
 			return false;
 		}
 	}
 
 	public ArrayList<String> getPlayerSubgroups(String player) {
 		User user = getUser(player, true);
 		if (user != null) {
 			ArrayList<String> result = new ArrayList<String>(user.getSubgroups());
 			result.add(user.getGroup());
 			result = calculateSubgroups(result);
 			result.remove(user.getGroup());
 			return result;
 		} else {
 			return null;
 		}
 	}
 
 	public ArrayList<String> getPlayerSubgroupsSimple(String player) {
 		User user = getUser(player, true);
 		if (user != null) {
 			ArrayList<String> result = new ArrayList<String>(user.getSubgroups());
 			return result;
 		} else {
 			return null;
 		}
 	}
 
 	private ArrayList<String> calculateSubgroups(ArrayList<String> input) {
 		ArrayList<String> result = new ArrayList<String>(input);
 		ArrayList<String> toTest = new ArrayList<String>(input);
 
 		while (toTest.size()!=0) {
 			String string = toTest.get(0);
			ArrayList<String> subgroups = Group.getGroup(string).getSubgroups();
			for (String string2 : subgroups) {
				if (!result.contains(string2)) {
					result.add(string2);
					toTest.add(string2);
 				}
 			}
 			toTest.remove(string);
 		}
 		return result;
 	}
 
 	public boolean addPlayerSubgroup(CommandSender sender, String player, String subgroup) {
 		User user = getUser(player, true);
 		if (user != null) {
 			boolean result = user.addSubgroup(subgroup);
 			if (result) {
 				sender.sendMessage(ChatColor.GREEN + "Added " + subgroup + " to subgrouplist of player " + user.getName());
 				return true;
 			} else {
 				sender.sendMessage(ChatColor.RED + "Couldn't add subgroup to player " + user.getName());
 				return false;
 			}
 		} else {
 			return false;
 		}
 	}
 
 	public boolean removePlayerSubgroup(CommandSender sender, String player, String subgroup) {
 		User user = getUser(player, true);
 		if (user != null) {
 			boolean result = user.removeSubgroup(subgroup);
 			if (result) {
 				sender.sendMessage(ChatColor.GREEN + "removed " + subgroup + " from subgrouplist of player " + user.getName());
 				return true;
 			} else {
 				sender.sendMessage(ChatColor.RED + "Couldn't remove subgroup from player " + user.getName());
 				return false;
 			}
 		} else {
 			return false;
 		}
 	}
 
 	public boolean addPlayerPermission(CommandSender sender, String player, String world, String node) {
 		User user = getUser(player, true);
 		if (user != null) {
 			boolean result = user.addPermission(world, node);
 			if (result) {
 				sender.sendMessage(ChatColor.GREEN + "Added " + node + " to permissionslist of player " + user.getName());
 				return true;
 			} else {
 				sender.sendMessage(ChatColor.RED + "Couldn't add permission to player " + user.getName());
 				return false;
 			}
 		} else {
 			return false;
 		}
 	}
 
 	public boolean removePlayerPermission(CommandSender sender, String player, String world, String node) {
 		User user = getUser(player, true);
 		if (user != null) {
 			boolean result = user.removePermission(world, node);
 			if (result) {
 				sender.sendMessage(ChatColor.GREEN + "removed " + node + " from permissionslist of player " + user.getName());
 				return true;
 			} else {
 				sender.sendMessage(ChatColor.RED + "Couldn't remove permission from player " + user.getName());
 				return false;
 			}
 		} else {
 			return false;
 		}
 	}
 
 	public HashMap<String, ArrayList<String>> getPlayerPermissions(String player, String world, boolean partialMatch) {
 		User user = getUser(player, partialMatch);
 		if (user == null) {
 			if (partialMatch) {
 				return null;
 			}
 			plugin.getServer().getLogger().info("[DroxPerms] User " + player + " doesn't exist yet. Creating ...");
 			user = new User(player);
 			User.addUser(user);
 			return user.getPermissions(world);
 		}
 		return user.getPermissions(world);
 	}
 
 	public boolean setPlayerInfo(CommandSender sender, String player, String node, String data) {
 		User user = getUser(player, true);
 		if (user != null) {
 			boolean result = user.setInfo(node, data);
 			if (result) {
 				sender.sendMessage(ChatColor.GREEN + "set info-node " + node + " of player " + user.getName());
 				return true;
 			} else {
 				sender.sendMessage(ChatColor.RED + "Couldn't set info-node of player " + user.getName());
 				return false;
 			}
 		} else {
 			return false;
 		}
 	}
 
 	public String getPlayerInfo(CommandSender sender, String player, String node) {
 		User user = getUser(player, true);
 		if (user != null) {
 			return user.getInfo(node);
 		} else {
 			return null;
 		}
 	}
 
 	public boolean addGroupPermission(CommandSender sender, String group, String world, String node) {
 		if (Group.existGroup(group)) {
 			boolean result = Group.getGroup(group).addPermission(world, node);
 			if (result) {
 				sender.sendMessage(ChatColor.GREEN + "Added " + node + " to permissionslist of group " + group);
 				return true;
 			} else {
 				sender.sendMessage(ChatColor.RED + "Couldn't add permission to group " + group);
 				return false;
 			}
 		} else {
 			sender.sendMessage(ChatColor.RED + "Group " + group + " doesn't exist.");
 			return false;
 		}
 	}
 
 	public boolean removeGroupPermission(CommandSender sender, String group, String world, String node) {
 		if (Group.existGroup(group)) {
 			boolean result = Group.getGroup(group).removePermission(world, node);
 			if (result) {
 				sender.sendMessage(ChatColor.GREEN + "removed " + node + " from permissionslist of group " + group);
 				return true;
 			} else {
 				sender.sendMessage(ChatColor.RED + "Couldn't remove permission from group " + group);
 				return false;
 			}
 		} else {
 			sender.sendMessage(ChatColor.RED + "Group " + group + " doesn't exist.");
 			return false;
 		}
 	}
 
 	public ArrayList<String> getGroupSubgroups(String groupName) {
 		Group group = Group.getGroup(groupName);
 		if (group != null) {
 			ArrayList<String> result = calculateSubgroups(group.getSubgroups());
 			return result;
 		} else {
 			return null;
 		}
 	}
 
 	public boolean addGroupSubgroup(CommandSender sender, String group, String subgroup) {
 		if (Group.existGroup(group)) {
 			boolean result = Group.getGroup(group).addSubgroup(subgroup);
 			if (result) {
 				sender.sendMessage(ChatColor.GREEN + "Added " + subgroup + " to subgrouplist of group " + group);
 				return true;
 			} else {
 				sender.sendMessage(ChatColor.RED + "Couldn't add subgroup to group " + group);
 				return false;
 			}
 		} else {
 			sender.sendMessage(ChatColor.RED + "Group " + group + " doesn't exist.");
 			return false;
 		}
 	}
 
 	public boolean removeGroupSubgroup(CommandSender sender, String group, String subgroup) {
 		if (Group.existGroup(group)) {
 			boolean result = Group.getGroup(group).removeSubgroup(subgroup);
 			if (result) {
 				sender.sendMessage(ChatColor.GREEN + "removed " + subgroup + " from subgrouplist of group " + group);
 				return true;
 			} else {
 				sender.sendMessage(ChatColor.RED + "Couldn't remove subgroup from group " + group);
 				return false;
 			}
 		} else {
 			sender.sendMessage(ChatColor.RED + "Group " + group + " doesn't exist.");
 			return false;
 		}
 	}
 
 	public HashMap<String, ArrayList<String>> getGroupPermissions(String groupName, String world) {
 		Group group = Group.getGroup(groupName);
 		if (group == null) {
 			return null;
 		}
 		return group.getPermissions(world);
 	}
 
 	public boolean setGroupInfo(CommandSender sender, String group, String node, String data) {
 		if (Group.existGroup(group)) {
 			boolean result = Group.getGroup(group).setInfo(node, data);
 			if (result) {
 				sender.sendMessage(ChatColor.GREEN + "set info-node " + node + " for group " + group);
 				return true;
 			} else {
 				sender.sendMessage(ChatColor.RED + "Couldn't set info-node for group " + group);
 				return false;
 			}
 		} else {
 			sender.sendMessage(ChatColor.RED + "Group " + group + " doesn't exist.");
 			return false;
 		}
 	}
 
 	public String getGroupInfo(CommandSender sender, String group, String node) {
 		if (Group.existGroup(group)) {
 			return Group.getGroup(group).getInfo(node);
 		} else {
 			sender.sendMessage(ChatColor.RED + "Group " + group + " doesn't exist.");
 			return null;
 		}
 	}
 
 	private User getUser(String name, boolean checkPartial) {
 		if (checkPartial) {
 			return getPartialUser(name);
 		}
 		return getExactUser(name);
 	}
 	private User getExactUser(String name) {
 		User user = null;
 		if (User.existUser(name)) {
 			user = User.getUser(name);
 			return user;
 		} else {
 			ConfigurationNode node = usersConfig.getNode("users." + name);
 			if (node != null) {
 				user = new User(name, node);
 				User.addUser(user);
 				return user;
 			}
 		}
 		return null;
 	}
 
 	private User getPartialUser(String name) {
 		boolean unsureOnline = false;
 		User user = null;
 		String onlineplayer = null;
 		Player player = plugin.getServer().getPlayer(name);
 		if (player != null) {
 			onlineplayer = player.getName();
 		}
 		if (onlineplayer != null && User.existUser(onlineplayer)) {
 			user = User.getUser(onlineplayer);
 		} else if (User.existUser(name)) {
 			user = User.getUser(name);
 		} else {
 			Iterator<User> iter = User.iter();
 			while (iter.hasNext()) {
 				User user2 = iter.next();
 				if (user2.getName().toLowerCase().contains(name.toLowerCase())) {
 					if (user == null) {
 						user = user2;
 					} else {
 						unsureOnline = true;;
 					}
 				}
 			}
 			if (user == null || unsureOnline) {
 				ConfigurationNode node = usersConfig.getNode("users." + name);
 				if (node != null) {
 					user = new User(name, node);
 					User.addUser(user);
 				} else {
 					List<String> users = usersConfig.getNode("users").getKeys();
 					for (String potentialUser : users) {
 						if(potentialUser.toLowerCase().contains(name.toLowerCase())) {
 							if (user == null) {
 								node = usersConfig.getNode("users." + potentialUser);
 								user = new User(potentialUser, node);
 								User.addUser(user);
 							} else {
 								return null;
 							}
 						}
 					}
 				}
 			}
 		}
 		if (user == null)
 			return null;
 		return user;
 	}
 
 	@Override
 	public boolean promotePlayer(CommandSender sender, String player,
 			String track) {
 		Track selectedTrack = Track.getTrack(track);
 		if (selectedTrack == null) {
 			sender.sendMessage(ChatColor.RED + "Could not find Track " + track + ".");
 			return false;
 		}
 		User user = getUser(player, true);
 		if (user != null) {
 			String newGroup = selectedTrack.getPromoteGroup(user.getGroup());
 			if (newGroup == null) {
 				sender.sendMessage(ChatColor.RED + "Could not promote on Track " + track + ".");
 				return false;
 			}
 			return setPlayerGroup(sender, player, newGroup);
 		} else {
 			sender.sendMessage(ChatColor.RED + "Could not find User " + player + ".");
 			return false;
 		}
 	}
 
 	@Override
 	public boolean demotePlayer(CommandSender sender, String player,
 			String track) {
 		Track selectedTrack = Track.getTrack(track);
 		if (selectedTrack == null) {
 			sender.sendMessage(ChatColor.RED + "Could not find Track " + track + ".");
 			return false;
 		}
 		User user = getUser(player, true);
 		if (user != null) {
 			String newGroup = selectedTrack.getDemoteGroup(user.getGroup());
 			if (newGroup == null) {
 				sender.sendMessage(ChatColor.RED + "Could not demote on Track " + track + ".");
 				return false;
 			}
 			return setPlayerGroup(sender, player, newGroup);
 		} else {
 			sender.sendMessage(ChatColor.RED + "Could not find User " + player + ".");
 			return false;
 		}
 	}
 
 	@Override
 	public HashMap<String, ArrayList<String>> getGroupMembers() {
 		HashMap<String, ArrayList<String>> result = new HashMap<String, ArrayList<String>>();
 		Iterator<Group> groups = Group.iter();
 		while (groups.hasNext()) {
 			Group group = (Group) groups.next();
 			result.put(group.getName(), new ArrayList<String>());
 		}
 
 		Map<String, ConfigurationNode> users = usersConfig.getNodes("users");
 		Iterator<String> iter = users.keySet().iterator();
 		while (iter.hasNext()) {
 			String key = iter.next();
 			ConfigurationNode conf = users.get(key);
 			User user = new User(key, conf);
 			result.get(user.getGroup()).add(user.getName());
 		}
 		return result;
 	}
 
 	@Override
 	public HashMap<String, ArrayList<String>> getSubgroupMembers() {
 		HashMap<String, ArrayList<String>> result = new HashMap<String, ArrayList<String>>();
 		Iterator<Group> groups = Group.iter();
 		while (groups.hasNext()) {
 			Group group = (Group) groups.next();
 			result.put(group.getName(), new ArrayList<String>());
 		}
 
 		Map<String, ConfigurationNode> users = usersConfig.getNodes("users");
 		Iterator<String> iter = users.keySet().iterator();
 		while (iter.hasNext()) {
 			String key = iter.next();
 			ConfigurationNode conf = users.get(key);
 			User user = new User(key, conf);
 			ArrayList<String> subgroups = user.getSubgroups();
 			for (String subgroup : subgroups) {
 				result.get(subgroup).add(user.getName());
 			}
 		}
 		return result;
 	}
 
 	public String getUserNameFromPart(String partialName) {
 		User user = getUser(partialName, true);
 		if (user != null) {
 			return user.getName();
 		}
 		return null;
 	}
 }
