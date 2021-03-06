 package net.croxis.plugins.research;
 
 import java.util.ArrayList;
 import java.util.Arrays;
 import java.util.HashMap;
 import java.util.Iterator;
 import java.util.List;
 
 import org.bukkit.entity.Player;
 import org.bukkit.permissions.PermissionAttachment;
 
 /**
  * @author croxis
  *
  */
 public class TechManager {
 	public static HashMap <String, Tech> techs = new HashMap<String, Tech>();
 	public static HashMap <Player, RPlayer> players = new HashMap<Player, RPlayer>();
 	private static Research plugin;
 	
 	public TechManager(Research plugin){
 		TechManager.plugin = plugin;
 	}
 	
 	/**
 	 * Creates a new player if and only if they do not already exist in memory
 	 * 
 	 * @param player
 	 */
 	public static void initPlayer(Player player){
 		if(players.containsKey(player))
 			return;
 		RPlayer rplayer = new RPlayer();
 		rplayer.name = player.getName();
 		rplayer.cantPlace.addAll(plugin.cantPlace);
 		rplayer.cantBreak.addAll(plugin.cantBreak);
 		rplayer.cantCraft.addAll(plugin.cantCraft);
 		rplayer.permissions.addAll(plugin.permissions);
 		players.put(player, rplayer);
 		SQLPlayer sqlplayer = getSQLPlayer(player);
 		for(String techName : sqlplayer.getResearched().split(",")){
 			applyLearnedTech(player, techs.get(techName));
 		}
 	}
 	
 	public static void unloadPlayer(Player player){
 		SQLPlayer sqlplayer = getSQLPlayer(player);
 		PermissionAttachment att = player.addAttachment(plugin);
 		for(String techName : sqlplayer.getResearched().split(",")){
 			Tech tech = techs.get(techName);
 			for (String perm : tech.permissions){
 				att.setPermission(perm, false);
 			}
 		}
 		player.recalculatePermissions();
 		players.remove(player);
 	}
 	
 	
 	/**
 	 * Adds points to a player existing total. If enough pointed are earned the tech is learned.
 	 * Returns true if tech learned, return false if not. 
 	 * @param player
 	 * @param points
 	 * @return
 	 */
 	public static boolean addPoints(Player player, int points){
 		SQLPlayer sqlplayer = getSQLPlayer(player);
 		int currentPoints = sqlplayer.getCurrentPoints();
 		currentPoints += points;
 		String techName = sqlplayer.getCurrentResearch();
 		Tech tech = techs.get(techName);
 		if(currentPoints >= tech.cost){
 			applyLearnedTech(player, tech);
 			sqlplayer.setCurrentResearch(sqlplayer.getCurrentResearch() + "," + tech.name);
 			sqlplayer.setCurrentPoints(currentPoints - tech.cost);
 			sqlplayer.setCurrentResearch(null);
 			plugin.getDatabase().save(sqlplayer);
 			return true;
 		}
 		return false;
 	}
 	
 	/**
 	 * Sets points to a player existing total. If enough pointed are earned the tech is learned.
 	 * Returns true if tech learned, return false if not. 
 	 * @param player
 	 * @param points
 	 * @return
 	 */
 	public static boolean setPoints(Player player, int points){
 		SQLPlayer sqlplayer = getSQLPlayer(player);
 		String techName = sqlplayer.getCurrentResearch();
 		Tech tech = techs.get(techName);
 		sqlplayer.setCurrentPoints(points);
 		plugin.getDatabase().save(sqlplayer);
 		if(points >= tech.cost){
 			applyLearnedTech(player, tech);
 			sqlplayer.setResearched(sqlplayer.getResearched() + "," + tech.name);
 			sqlplayer.setCurrentPoints(points - tech.cost);
 			sqlplayer.setCurrentResearch(null);
 			plugin.getDatabase().save(sqlplayer);
 			return true;
 		}
 		return false;
 	}
 	
 	/**
 	 * Applies effect of learned tech. Does not persist it.
 	 * @param player
 	 * @param tech
 	 */
 	public static void applyLearnedTech(Player player, Tech tech){
 		RPlayer rplayer = players.get(player);
 		rplayer.cantPlace.removeAll(tech.canPlace);
 		rplayer.cantBreak.removeAll(tech.canPlace);
 		rplayer.cantCraft.removeAll(tech.canPlace);
 		rplayer.permissions.addAll(tech.permissions);
 		//TODO: Process permission nodes
 		PermissionAttachment att = player.addAttachment(plugin);
 		for(String perm : tech.permissions){
 			att.setPermission(perm, true);
 		}
 		player.recalculatePermissions();
 	}
 	
 	/**
 	 * Manually adds and implaments a new tech.
 	 * @param player
 	 * @param tech
 	 */
 	public static void addTech(Player player, Tech tech){
 		applyLearnedTech(player, tech);
 		SQLPlayer sqlplayer = getSQLPlayer(player);
 		sqlplayer.setResearched(sqlplayer.getResearched() + "," + tech.name);
 		plugin.getDatabase().save(sqlplayer);
 	}
 	
 	/**
 	 * Replaces existing player tech knowledge. If only adding tech please use addTech() instead.
 	 * @param player
 	 * @param techs
 	 */
 	public static void setTech(Player player, Tech[] techs){
 		SQLPlayer sqlplayer = getSQLPlayer(player);
 		String techNames = "";
 		Iterator<Tech> techi = Arrays.asList(techs).iterator();
 		while(techi.hasNext()){
 			techNames += techi.next().name;
 			if(techi.hasNext())
 				techNames += ",";
 		}
 		sqlplayer.setResearched(techNames);
 		plugin.getDatabase().save(sqlplayer);
 		unloadPlayer(player);
 		initPlayer(player);
 	}
 	
 	
 	/**
 	 * Returns persistance of a player.
 	 * Not intended for public consumption.
 	 * @param player
 	 * @return
 	 */
 	public static SQLPlayer getSQLPlayer(Player player){
 		SQLPlayer sqlplayer = plugin.getDatabase().find(SQLPlayer.class).where().ieq("player_name", player.getName()).findUnique();
 		if (sqlplayer == null){
 			sqlplayer = new SQLPlayer();
 			sqlplayer.setPlayerName(player.getName());
 			sqlplayer.setCurrentPoints(0);
 			sqlplayer.setResearched("");
 			plugin.getDatabase().save(sqlplayer);
 		}
 		return sqlplayer;
 	}
 	
 	public static boolean startResearch(Player player, String techName){
 		if(!techs.containsKey(techName))
 			return false;
 		return startResearch(player, techs.get(techName));
 	}
 
 	public static boolean startResearch(Player player, Tech tech) {
 		SQLPlayer sqlplayer = getSQLPlayer(player);
 		String learned = sqlplayer.getResearched();
 		String[] ll = learned.split(",");
 		List<String> learnedList = Arrays.asList(ll);
 		
 		for(Tech parent : tech.parents){
 			if(!learnedList.contains(parent.name))
 				return false;
 		}
 		
 		if(sqlplayer.getCurrentResearch() != null)
 			sqlplayer.setCurrentPoints(0);
 		
 		sqlplayer.setCurrentResearch(tech.name);
 		plugin.getDatabase().save(sqlplayer);
 		//TODO: Check if enough points to complete tech.
 		
 		return true;
 	}
 	
 	public static boolean canResearch(Player player, Tech tech){
 		return getAvailableTech(player).contains(tech);
 	}
 	
 	/**
 	 * Returns a list of Techs that a player can research next
 	 * @param player
 	 * @return
 	 */
 	public static ArrayList<Tech> getAvailableTech(Player player){
 		ArrayList<Tech> unknowns = new ArrayList<Tech>();
 		ArrayList<Tech> available = new ArrayList<Tech>();
 		ArrayList<Tech> researched = getResearched(player);
 		for(Tech t : techs.values()){
 			if(!researched.contains(t))
 				unknowns.add(t);
 		}
 		
 		for(Tech t : unknowns){
 			boolean avail = true;
 			for(Tech parent : t.parents){
 				if(!available.contains(parent));
 				avail = false;
 			}
 			if(avail)
 				available.add(t);
 		}
 		return available;
 		
 	}
 	
 	/**
 	 * Returns a list of technologies the player has learned
 	 * @param player
 	 * @return
 	 */
 	public static ArrayList<Tech> getResearched(Player player){
 		SQLPlayer sqlplayer = getSQLPlayer(player);
 		String learned = sqlplayer.getResearched();
 		ArrayList<Tech> ts = new ArrayList<Tech>();
 		String[] ll = learned.split(",");
 		for (String techName : ll){
 			ts.add(techs.get(techName));
 		}
 		return ts;
 	}
 	
 	public static int getPoints(Player player){
 		SQLPlayer sqlplayer = getSQLPlayer(player);
 		return sqlplayer.getCurrentPoints();
 	}
 	
 	public static Tech getCurrentResearch(Player player){
 		System.out.println("testestest");
 		SQLPlayer sqlplayer = getSQLPlayer(player);
 		return techs.get(sqlplayer.getCurrentResearch());
 	}
 
 }
