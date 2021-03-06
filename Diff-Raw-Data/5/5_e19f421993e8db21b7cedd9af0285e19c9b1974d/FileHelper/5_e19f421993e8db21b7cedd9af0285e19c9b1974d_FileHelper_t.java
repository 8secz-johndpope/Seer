 package net.dmulloy2.ultimatearena;
 
 import java.io.File;
 import java.util.ArrayList;
 import java.util.List;
 import java.util.Map;
 import java.util.Map.Entry;
 
 import org.bukkit.Location;
 import org.bukkit.World;
 import org.bukkit.configuration.file.YamlConfiguration;
 import org.bukkit.enchantments.Enchantment;
 import org.bukkit.entity.Player;
 import org.bukkit.inventory.ItemStack;
 import org.bukkit.inventory.meta.ItemMeta;
 import org.bukkit.material.MaterialData;
 
 import net.dmulloy2.ultimatearena.arenas.objects.ArenaSign;
 import net.dmulloy2.ultimatearena.arenas.objects.ArenaZone;
 import net.dmulloy2.ultimatearena.arenas.objects.SavedArenaPlayer;
 
 /**
  * @author dmulloy2
  */
 
 public class FileHelper 
 {
 	private final UltimateArena plugin;
 	public FileHelper(final UltimateArena plugin)
 	{
 		this.plugin = plugin;
 	}
 	
 	/**Generate Whitelisted Commands File**/
 	public void generateWhitelistedCmds()
 	{
 		File file = new File(plugin.getDataFolder(), "whiteListedCommands.yml");
 		if (file.exists())
 			return;
 		
 		try
 		{
 			file.createNewFile();
 		
 			YamlConfiguration fc = YamlConfiguration.loadConfiguration(file);
 		
 			List<String> words = new ArrayList<String>();
 			words.add("/f c");
 			words.add("/msg");
 			words.add("/r");
 			words.add("/who");
 			words.add("/gms");
 			words.add("/god");
 			words.add("/list");
 			words.add("/t");
 			words.add("/msg");
 			words.add("/tell");
 		
 			fc.set("whiteListedCmds", words);
 		
 			fc.save(file);
 		}
 		catch (Exception e)
 		{
 			plugin.getLogger().severe("Error saving whitelisted cmds file: " + e.getMessage());
 		}
 	}
 	
 	/**Generate Arena Configurations**/
 	public void generateArenaConfig(String field)
 	{
 		File folder = new File(plugin.getDataFolder(), "configs");
 		File file = new File(folder, field + "Config.yml");
 		if (file.exists())
 			return;
 		
 		try
 		{
 			file.createNewFile();
 			
 			YamlConfiguration fc = YamlConfiguration.loadConfiguration(file);
 			if (field.equals("bomb"))
 			{
 				fc.set("gameTime", 900);
 				fc.set("lobbyTime", 70);
 				fc.set("maxDeaths", 990);
 				fc.set("allowTeamKilling", false);
 				fc.set("cashReward", 100);
 				
 				List<String> rewards = new ArrayList<String>();
 				rewards.add("266,1");
 				rewards.add("46,4");
 				
 				fc.set("rewards", rewards);
 				
 				fc.save(file);
 			}
 			
 			if (field.equals("cq"))
 			{
 				fc.set("gameTime", 1200);
 				fc.set("lobbyTime", 180);
 				fc.set("maxDeaths", 900);
 				fc.set("allowTeamKilling", false);
 				fc.set("cashReward", 100);
 				
 				List<String> rewards = new ArrayList<String>();
 				rewards.add("266,3");
 				
 				fc.set("rewards", rewards);
 				
 				fc.save(file);
 			}
 			
 			if (field.equals("ctf"))
 			{
 				fc.set("gameTime", 440);
 				fc.set("lobbyTime", 90);
 				fc.set("maxDeaths", 999);
 				fc.set("allowTeamKilling", false);
 				fc.set("cashReward", 100);
 				
 				List<String> rewards = new ArrayList<String>();
 				rewards.add("266,4");
 				
 				fc.set("rewards", rewards);
 				
 				fc.save(file);
 			}
 			
 			if (field.equals("ffa"))
 			{
 				fc.set("gameTime", 600);
 				fc.set("lobbyTime", 70);
 				fc.set("maxDeaths", 3);
 				fc.set("allowTeamKilling", true);
 				fc.set("cashReward", 100);
 				
 				List<String> rewards = new ArrayList<String>();
 				rewards.add("266,9");
 				rewards.add("46,3");
 				
 				fc.set("rewards", rewards);
 				
 				fc.save(file);
 			}
 			
 			if (field.equals("hunger"))
 			{
 				fc.set("gameTime", 9000);
 				fc.set("lobbyTime", 70);
 				fc.set("maxDeaths", 1);
 				fc.set("allowTeamKilling", true);
 				fc.set("cashReward", 1000);
 				
 				List<String> rewards = new ArrayList<String>();
 				rewards.add("266,3");
 				rewards.add("46,3");
 				
 				fc.set("rewards", rewards);
 				
 				fc.save(file);
 			}
 			
 			if (field.equals("infect"))
 			{
 				fc.set("gameTime", 180);
 				fc.set("lobbyTime", 90);
 				fc.set("maxDeaths", 2);
 				fc.set("allowTeamKilling", false);
 				fc.set("cashReward", 100);
 				
 				List<String> rewards = new ArrayList<String>();
 				rewards.add("266,6");
 				rewards.add("46,2");
 				
 				fc.set("rewards", rewards);
 				
 				fc.save(file);
 			}
 			
 			if (field.equals("koth"))
 			{
 				fc.set("gameTime", 1200);
 				fc.set("lobbyTime", 80);
 				fc.set("maxDeaths", 900);
 				fc.set("allowTeamKilling", true);
 				fc.set("cashReward", 100);
 				
 				List<String> rewards = new ArrayList<String>();
 				rewards.add("266,3");
 				rewards.add("46,3");
 				
 				fc.set("rewards", rewards);
 				
 				fc.save(file);
 			}
 			
 			if (field.equals("mob"))
 			{
 				fc.set("gameTime", 1200);
 				fc.set("lobbyTime", 90);
 				fc.set("maxDeaths", 0);
 				fc.set("maxWave", 15);
 				fc.set("allowTeamKilling", false);
 				fc.set("cashReward", 15);
 				
 				List<String> rewards = new ArrayList<String>();
 				rewards.add("266,3");
 				rewards.add("46,2");
 				
 				fc.set("rewards", rewards);
 				
 				fc.save(file);
 			}
 			
 			if (field.equals("pvp"))
 			{
 				fc.set("gameTime", 600);
 				fc.set("lobbyTime", 90);
 				fc.set("maxDeaths", 3);
 				fc.set("allowTeamKilling", false);
 				fc.set("cashReward", 100);
 				
 				List<String> rewards = new ArrayList<String>();
 				rewards.add("266,3");
 				rewards.add("46,2");
 				
 				fc.set("rewards", rewards);
 				
 				fc.save(file);
 			}
 			
 			if (field.equals("spleef"))
 			{
 				fc.set("gameTime", 600);
 				fc.set("lobbyTime", 80);
 				fc.set("maxDeaths", 2);
 				fc.set("allowTeamKilling", true);
 				fc.set("cashReward", 100);
 				
 				List<String> rewards = new ArrayList<String>();
 				rewards.add("266,3");
 				rewards.add("46,2");
 				
 				fc.set("rewards", rewards);
 				
 				fc.save(file);
 			}
 		}
 		catch (Exception e)
 		{
 			plugin.getLogger().severe("Error generating config \"" + field + "\": " + e.getMessage());
 		}
 	}
 
 	/**Generate Stock Classes**/
 	public void generateStockClasses() 
 	{
 		File dir = new File(plugin.getDataFolder(), "classes");
 		if (!dir.exists())
 			dir.mkdir();
 		
 		File archerFile = new File(dir, "archer.yml");
 		if (!archerFile.exists())
 			try
 		{ 
 				archerFile.createNewFile(); 
 		}
 		catch (Exception e)
 		{
 			plugin.getLogger().severe("Error saving archer file: " + e.getMessage()); 
 		}
 		generateClass(archerFile, "archer");
 		
 		File bruteFile = new File(dir, "brute.yml");
 		if (!bruteFile.exists())
 			try
 		{ 
 				bruteFile.createNewFile(); 
 		}
 		catch (Exception e)
 		{
 			plugin.getLogger().severe("Error saving brute file: " + e.getMessage()); 
 		}
 		generateClass(bruteFile, "brute");
 		
 		File dumbassFile = new File(dir, "dumbass.yml");
 		if (!dumbassFile.exists())
 			try
 		{ 
 				dumbassFile.createNewFile(); 
 		}
 		catch (Exception e)
 		{
 			plugin.getLogger().severe("Error saving dumbass file: " + e.getMessage()); 
 		}
 		generateClass(dumbassFile, "dumbass");
 		
 		File gunnerFile = new File(dir, "gunner.yml");
 		if (!gunnerFile.exists())
 			try
 		{ 
 				gunnerFile.createNewFile(); 
 		}
 		catch (Exception e)
 		{
 			plugin.getLogger().severe("Error saving gunner file: " + e.getMessage()); 
 		}
 		generateClass(gunnerFile, "gunner");
 		
 		File healerFile = new File(dir, "healer.yml");
 		if (!healerFile.exists())
 			try
 		{ 
 				healerFile.createNewFile(); 
 		}
 		catch (Exception e)
 		{
 			plugin.getLogger().severe("Error saving healer file: " + e.getMessage()); 
 		}
 		generateClass(healerFile, "healer");
 		
 		File shotgunFile = new File(dir, "shotgun.yml");
 		if (!shotgunFile.exists())
 			try
 		{ 
 				shotgunFile.createNewFile(); 
 		}
 		catch (Exception e)
 		{
 			plugin.getLogger().severe("Error saving shotgun file: " + e.getMessage()); 
 		}
 		generateClass(shotgunFile, "shotgun");
 		
 		File sniperFile = new File(dir, "sniper.yml");
 		if (!sniperFile.exists())
 			try
 		{ 
 				sniperFile.createNewFile(); 
 		}
 		catch (Exception e)
 		{
 			plugin.getLogger().severe("Error saving sniper file: " + e.getMessage()); 
 		}
 		generateClass(sniperFile, "sniper");
 		
 		File spleefFile = new File(dir, "spleef.yml");
 		if (!spleefFile.exists())
 			try
 		{ 
 				spleefFile.createNewFile(); 
 		}
 		catch (Exception e)
 		{
 			plugin.getLogger().severe("Error saving spleef file: " + e.getMessage()); 
 		}
 		generateClass(spleefFile, "spleef");
 	}
 	
 	/**Generates a Class File**/
 	public void generateClass(File file, String type)
 	{
 		YamlConfiguration fc = YamlConfiguration.loadConfiguration(file);
 		try
 		{
 			if (type.equals("archer"))
 			{
 				fc.set("armor.chestplate", "307");
 				fc.set("armor.leggings", "308");
 				fc.set("armor.boots", "309");
 				fc.set("tools.1", "261");
 				fc.set("tools.2", "262,1024");
 				fc.set("tools.3", "267");
 			}
 			
 			if (type.equals("brute"))
 			{
 				fc.set("armor.chestplate", "307");
 				fc.set("armor.leggings", "308");
 				fc.set("armor.boots", "309");
 				fc.set("tools.1", "276");
 				fc.set("tools.2", "333:1,2");
 				fc.set("tools.3", "341,24");
 			}
 			
 			if (type.equals("dumbass"))
 			{
 				fc.set("armor.chestplate", "307");
 				fc.set("armor.leggings", "308");
 				fc.set("armor.boots", "309");
 				fc.set("tools.1", "283");
 				fc.set("tools.2", "259");
 			}
 			
 			if (type.equals("gunner"))
 			{
 				fc.set("armor.chestplate", "307");
 				fc.set("armor.leggings", "308");
 				fc.set("armor.boots", "309");
 				fc.set("tools.1", "292");
 				fc.set("tools.2", "318,7070");
 				fc.set("tools.3", "341,24");
 				fc.set("tools.4", "322,2");
 				fc.set("tools.5", "261,1,sharp:1");
 			}
 			
 			if (type.equals("healer"))
 			{
 				fc.set("armor.chestplate", "307");
 				fc.set("armor.leggings", "308");
 				fc.set("armor.boots", "309");
 				fc.set("tools.1", "267");
 				fc.set("tools.2", "373:8261");
 				fc.set("tools.3", "373:16453");
 			}
 			
 			if (type.equals("shotgun"))
 			{
 				fc.set("armor.chestplate", "307");
 				fc.set("armor.leggings", "308");
 				fc.set("armor.boots", "309");
 				fc.set("tools.1", "291");
 				fc.set("tools.2", "295,1024");
 				fc.set("tools.3", "341,24");
 				fc.set("tools.4", "322,2");
 				fc.set("tools.5", "267");
 			}
 			
 			if (type.equals("sniper"))
 			{
 				fc.set("armor.chestplate", "307");
 				fc.set("armor.leggings", "308");
 				fc.set("armor.boots", "309");
 				fc.set("tools.1", "294");
 				fc.set("tools.2", "337,1024");
 				fc.set("tools.3", "341,24");
 				fc.set("tools.4", "322,2");
 				fc.set("tools.5", "267");
 			}
 			
 			if (type.equals("spleef"))
 			{
 				fc.set("armor.chestplate", "307");
 				fc.set("armor.leggings", "308");
 				fc.set("armor.boots", "309");
 				fc.set("tools.1", "277");
 			}
 			
 			fc.set("useEssentials", false);
 			fc.set("essentialsKit", "");
 			
 			fc.set("useHelmet", true);
 			
 			fc.set("permissionNode", "");
 			
 			fc.save(file);
 		}
 		catch (Exception e)
 		{
 			plugin.getLogger().severe("Error saving \"" + type + "\" file: " + e.getMessage());
 		}
 	}
 
 	/**Save an ArenaZone**/
 	public void save(ArenaZone az)
 	{
 		try
 		{
 			File folder = new File(plugin.getDataFolder(), "arenas");
 			File file = new File(folder, az.getArenaName() + ".dat");
 			if (!file.exists())
 				file.createNewFile();
 
 			YamlConfiguration fc = YamlConfiguration.loadConfiguration(file);
 			
 			fc.set("type", az.getArenaType());
 			fc.set("world", az.getWorld().getName());
 			
 			Location lobby1 = az.getLobby1();
 			fc.set("lobby1.x", lobby1.getBlockX());
 			fc.set("lobby1.z", lobby1.getBlockZ());
 			
 			Location lobby2 = az.getLobby2();
 			fc.set("lobby2.x", lobby2.getBlockX());
 			fc.set("lobby2.z", lobby2.getBlockZ());
 			
 			Location arena1 = az.getArena1();
 			fc.set("arena1.x", arena1.getBlockX());
 			fc.set("arena1.z", arena1.getBlockZ());
 			
 			Location arena2 = az.getArena2();
 			fc.set("arena2.x", arena2.getBlockX());
 			fc.set("arena2.z", arena2.getBlockZ());
 			
 			String arenaType = az.getArenaType();
 			if (arenaType.equals("pvp"))
 			{
 				Location lobbyRed = az.getLobbyREDspawn();
 				fc.set("lobbyRed.x", lobbyRed.getBlockX());
 				fc.set("lobbyRed.y", lobbyRed.getBlockY());
 				fc.set("lobbyRed.z", lobbyRed.getBlockZ());
 				
 				Location lobbyBlue = az.getLobbyBLUspawn();
 				fc.set("lobbyBlue.x", lobbyBlue.getBlockX());
 				fc.set("lobbyBlue.y", lobbyBlue.getBlockY());
 				fc.set("lobbyBlue.z", lobbyBlue.getBlockZ());
 				
 				Location team1 = az.getTeam1spawn();
 				fc.set("team1.x", team1.getBlockX());
 				fc.set("team1.y", team1.getBlockY());
 				fc.set("team1.z", team1.getBlockZ());
 				
 				Location team2 = az.getTeam2spawn();
 				fc.set("team2.x", team2.getBlockX());
 				fc.set("team2.y", team2.getBlockY());
 				fc.set("team2.z", team2.getBlockZ());
 			}
 			if (arenaType.equals("mob"))
 			{
 				Location lobbyRed = az.getLobbyREDspawn();
 				fc.set("lobbyRed.x", lobbyRed.getBlockX());
 				fc.set("lobbyRed.y", lobbyRed.getBlockY());
 				fc.set("lobbyRed.z", lobbyRed.getBlockZ());
 				
 				Location team1 = az.getTeam1spawn();
 				fc.set("team1.x", team1.getBlockX());
 				fc.set("team1.y", team1.getBlockY());
 				fc.set("team1.z", team1.getBlockZ());
 				
 				fc.set("spawnsAmt", az.getSpawns().size());
 				for (int i = 0; i < az.getSpawns().size(); i++) 
 				{
 					Location loc = az.getSpawns().get(i);
 					String path = "spawns." + i + ".";
 
 					fc.set(path + "x", loc.getBlockX());
 					fc.set(path + "y", loc.getBlockY());
 					fc.set(path + "z", loc.getBlockZ());
 				}
 			}
 			if (arenaType.equals("cq")) 
 			{
 				Location lobbyRed = az.getLobbyREDspawn();
 				fc.set("lobbyRed.x", lobbyRed.getBlockX());
 				fc.set("lobbyRed.y", lobbyRed.getBlockY());
 				fc.set("lobbyRed.z", lobbyRed.getBlockZ());
 				
 				Location lobbyBlue = az.getLobbyBLUspawn();
 				fc.set("lobbyBlue.x", lobbyBlue.getBlockX());
 				fc.set("lobbyBlue.y", lobbyBlue.getBlockY());
 				fc.set("lobbyBlue.z", lobbyBlue.getBlockZ());
 				
 				Location team1 = az.getTeam1spawn();
 				fc.set("team1.x", team1.getBlockX());
 				fc.set("team1.y", team1.getBlockY());
 				fc.set("team1.z", team1.getBlockZ());
 				
 				Location team2 = az.getTeam2spawn();
 				fc.set("team2.x", team2.getBlockX());
 				fc.set("team2.y", team2.getBlockY());
 				fc.set("team2.z", team2.getBlockZ());
 				
 				fc.set("flagsAmt", az.getFlags().size());
 				for (int i = 0; i < az.getFlags().size(); i++) 
 				{
 					Location loc = az.getFlags().get(i);
 					String path = "flags." + i + ".";
 
 					fc.set(path + "x", loc.getBlockX());
 					fc.set(path + "y", loc.getBlockY());
 					fc.set(path + "z", loc.getBlockZ());
 				}
 			}
 			if (arenaType.equals("koth"))
 			{
 				Location lobbyRed = az.getLobbyREDspawn();
 				fc.set("lobbyRed.x", lobbyRed.getBlockX());
 				fc.set("lobbyRed.y", lobbyRed.getBlockY());
 				fc.set("lobbyRed.z", lobbyRed.getBlockZ());
 				
 				fc.set("spawnsAmt", az.getSpawns().size());
 				for (int i = 0; i < az.getSpawns().size(); i++) 
 				{
 					Location loc = az.getSpawns().get(i);
 					String path = "spawns." + i + ".";
 
 					fc.set(path + "x", loc.getBlockX());
 					fc.set(path + "y", loc.getBlockY());
 					fc.set(path + "z", loc.getBlockZ());
 				}
 				
 				fc.set("flag.x", az.getFlags().get(0).getBlockX());
 				fc.set("flag.y", az.getFlags().get(0).getBlockY());
 				fc.set("flag.z", az.getFlags().get(0).getBlockZ());
 			}
 			if (arenaType.equals("ffa") || arenaType.equals("hunger"))
 			{
 				Location lobbyRed = az.getLobbyREDspawn();
 				fc.set("lobbyRed.x", lobbyRed.getBlockX());
 				fc.set("lobbyRed.y", lobbyRed.getBlockY());
 				fc.set("lobbyRed.z", lobbyRed.getBlockZ());
 				
 				fc.set("spawnsAmt", az.getSpawns().size());
 				for (int i = 0; i < az.getSpawns().size(); i++) 
 				{
 					Location loc = az.getSpawns().get(i);
 					String path = "spawns." + i + ".";
 
 					fc.set(path + "x", loc.getBlockX());
 					fc.set(path + "y", loc.getBlockY());
 					fc.set(path + "z", loc.getBlockZ());
 				}
 				
 			}
 			if (arenaType.equals("spleef"))
 			{
 				Location lobbyRed = az.getLobbyREDspawn();
 				fc.set("lobbyRed.x", lobbyRed.getBlockX());
 				fc.set("lobbyRed.y", lobbyRed.getBlockY());
 				fc.set("lobbyRed.z", lobbyRed.getBlockZ());
 				
 				fc.set("specialType", 80);
 				
 				for (int i = 0; i < 4; i++) 
 				{
 					Location loc = az.getFlags().get(i);
 					String path = "flags." + i + ".";
 
 					fc.set(path + "x", loc.getBlockX());
 					fc.set(path + "y", loc.getBlockY());
 					fc.set(path + "z", loc.getBlockZ());
 				}
 			}
 			if (arenaType.equals("bomb"))
 			{
 				Location lobbyRed = az.getLobbyREDspawn();
 				fc.set("lobbyRed.x", lobbyRed.getBlockX());
 				fc.set("lobbyRed.y", lobbyRed.getBlockY());
 				fc.set("lobbyRed.z", lobbyRed.getBlockZ());
 				
 				Location lobbyBlue = az.getLobbyBLUspawn();
 				fc.set("lobbyBlue.x", lobbyBlue.getBlockX());
 				fc.set("lobbyBlue.y", lobbyBlue.getBlockY());
 				fc.set("lobbyBlue.z", lobbyBlue.getBlockZ());
 				
 				Location team1 = az.getTeam1spawn();
 				fc.set("team1.x", team1.getBlockX());
 				fc.set("team1.y", team1.getBlockY());
 				fc.set("team1.z", team1.getBlockZ());
 				
 				Location team2 = az.getTeam2spawn();
 				fc.set("team2.x", team2.getBlockX());
 				fc.set("team2.y", team2.getBlockY());
 				fc.set("team2.z", team2.getBlockZ());
 				
 				fc.set("flag0.x", az.getFlags().get(0).getBlockX());
 				fc.set("flag0.y", az.getFlags().get(0).getBlockY());
 				fc.set("flag0.z", az.getFlags().get(0).getBlockZ());
 				
 				fc.set("flag1.x", az.getFlags().get(1).getBlockX());
 				fc.set("flag1.y", az.getFlags().get(1).getBlockY());
 				fc.set("flag1.z", az.getFlags().get(1).getBlockZ());
 			}
 			if (arenaType.equals("ctf")) 
 			{
 				Location lobbyRed = az.getLobbyREDspawn();
 				fc.set("lobbyRed.x", lobbyRed.getBlockX());
 				fc.set("lobbyRed.y", lobbyRed.getBlockY());
 				fc.set("lobbyRed.z", lobbyRed.getBlockZ());
 				
 				Location lobbyBlue = az.getLobbyBLUspawn();
 				fc.set("lobbyBlue.x", lobbyBlue.getBlockX());
 				fc.set("lobbyBlue.y", lobbyBlue.getBlockY());
 				fc.set("lobbyBlue.z", lobbyBlue.getBlockZ());
 				
 				Location team1 = az.getTeam1spawn();
 				fc.set("team1.x", team1.getBlockX());
 				fc.set("team1.y", team1.getBlockY());
 				fc.set("team1.z", team1.getBlockZ());
 				
 				Location team2 = az.getTeam2spawn();
 				fc.set("team2.x", team2.getBlockX());
 				fc.set("team2.y", team2.getBlockY());
 				fc.set("team2.z", team2.getBlockZ());
 				
 				fc.set("flag0.x", az.getFlags().get(0).getBlockX());
 				fc.set("flag0.y", az.getFlags().get(0).getBlockY());
 				fc.set("flag0.z", az.getFlags().get(0).getBlockZ());
 				
 				fc.set("flag1.x", az.getFlags().get(1).getBlockX());
 				fc.set("flag1.y", az.getFlags().get(1).getBlockY());
 				fc.set("flag1.z", az.getFlags().get(1).getBlockZ());
 			}
 			if (arenaType.equals("infect"))
 			{
 				Location lobbyRed = az.getLobbyREDspawn();
 				fc.set("lobbyRed.x", lobbyRed.getBlockX());
 				fc.set("lobbyRed.y", lobbyRed.getBlockY());
 				fc.set("lobbyRed.z", lobbyRed.getBlockZ());
 				
 				Location lobbyBlue = az.getLobbyBLUspawn();
 				fc.set("lobbyBlue.x", lobbyBlue.getBlockX());
 				fc.set("lobbyBlue.y", lobbyBlue.getBlockY());
 				fc.set("lobbyBlue.z", lobbyBlue.getBlockZ());
 				
 				Location team1 = az.getTeam1spawn();
 				fc.set("team1.x", team1.getBlockX());
 				fc.set("team1.y", team1.getBlockY());
 				fc.set("team1.z", team1.getBlockZ());
 				
 				Location team2 = az.getTeam2spawn();
 				fc.set("team2.x", team2.getBlockX());
 				fc.set("team2.y", team2.getBlockY());
 				fc.set("team2.z", team2.getBlockZ());
 			}
 			
 			fc.set("maxPlayers", 24);
 			fc.set("defaultClass", plugin.classes.get(0).getName());
 			
 			fc.save(file);
 		}
 		catch (Exception e)
 		{
 			plugin.getLogger().severe("Error saving arena \"" + az.getArenaName() + "\": " + e.getMessage());
 		}
 	}
 	
 	/**Load an ArenaZone**/
 	public void load(ArenaZone az)
 	{
 		try
 		{
 			File folder = new File(plugin.getDataFolder(), "arenas");
 			File file = new File(folder, az.getArenaName() + ".dat");
 	
 			YamlConfiguration fc = YamlConfiguration.loadConfiguration(file);
 			
 			String arenaType = fc.getString("type");
 			az.setArenaType(arenaType);
 			
 			World world = plugin.getServer().getWorld(fc.getString("world"));
 			az.setWorld(world);
 			
 			az.setLobby1(new Location(world, fc.getInt("lobby1.x"), 0, fc.getInt("lobby1.z")));
 			az.setLobby2(new Location(world, fc.getInt("lobby2.x"), 0, fc.getInt("lobby2.z")));
 			
 			az.setArena1(new Location(world, fc.getInt("arena1.x"), 0, fc.getInt("arena1.z")));
 			az.setArena2(new Location(world, fc.getInt("arena2.x"), 0, fc.getInt("arena2.z")));
 	
 			if (arenaType.equals("pvp"))
 			{
 				az.setLobbyREDspawn(new Location(world, fc.getInt("lobbyRed.x"), fc.getInt("lobbyRed.y"), fc.getInt("lobbyRed.z")));
 				
 				az.setLobbyBLUspawn(new Location(world, fc.getInt("lobbyBlue.x"), fc.getInt("lobbyBlue.y"), fc.getInt("lobbyBlue.z")));
 				
 				az.setTeam1spawn(new Location(world, fc.getInt("team1.x"), fc.getInt("team1.y"), fc.getInt("team1.z")));
 				
 				az.setTeam2spawn(new Location(world, fc.getInt("team2.x"), fc.getInt("team2.y"), fc.getInt("team2.z")));
 			}
 			if (arenaType.equals("mob"))
 			{
 				az.setLobbyREDspawn(new Location(world, fc.getInt("lobbyRed.x"), fc.getInt("lobbyRed.y"), fc.getInt("lobbyRed.z")));
 				
 				az.setTeam1spawn(new Location(world, fc.getInt("team1.x"), fc.getInt("team1.y"), fc.getInt("team1.z")));
 				
 				int spawnsAmt = fc.getInt("spawnsAmt");
 				for (int i = 0; i < spawnsAmt; i++) 
 				{
 					String path = "spawns." + i + ".";
 	
 					Location loc = new Location(world, fc.getInt(path + "x"),fc.getInt(path + "y"), fc.getInt(path + "z"));
 					
 					az.getSpawns().add(loc);
 				}
 			}
 			if (arenaType.equals("cq")) 
 			{
 				az.setLobbyREDspawn(new Location(world, fc.getInt("lobbyRed.x"), fc.getInt("lobbyRed.y"), fc.getInt("lobbyRed.z")));
 				
 				az.setLobbyBLUspawn(new Location(world, fc.getInt("lobbyBlue.x"), fc.getInt("lobbyBlue.y"), fc.getInt("lobbyBlue.z")));
 				
 				az.setTeam1spawn(new Location(world, fc.getInt("team1.x"), fc.getInt("team1.y"), fc.getInt("team1.z")));
 				
 				az.setTeam2spawn(new Location(world, fc.getInt("team2.x"), fc.getInt("team2.y"), fc.getInt("team2.z")));
 				
 				int flagsAmt = fc.getInt("flagsAmt");
 				for (int i = 0; i < flagsAmt; i++) 
 				{
 					String path = "flags." + i + ".";
 	
 					Location loc = new Location(world, fc.getInt(path + "x"),fc.getInt(path + "y"), fc.getInt(path + "z"));
 					
 					az.getFlags().add(loc);
 				}
 			}
 			if (arenaType.equals("koth"))
 			{
 				az.setLobbyREDspawn(new Location(world, fc.getInt("lobbyRed.x"), fc.getInt("lobbyRed.y"), fc.getInt("lobbyRed.z")));
 				
 				int spawnsAmt = fc.getInt("spawnsAmt");
 				for (int i = 0; i < spawnsAmt; i++) 
 				{
 					String path = "spawns." + i + ".";
 	
 					Location loc = new Location(world, fc.getInt(path + "x"),fc.getInt(path + "y"), fc.getInt(path + "z"));
 					
 					az.getSpawns().add(loc);
 				}
 				
 				Location loc = new Location(world, fc.getInt("flag.x"), fc.getInt("flag.y"), fc.getInt("flag.z"));
 				az.getFlags().add(loc);
 			}
 			if (arenaType.equals("ffa") || arenaType.equals("hunger"))
 			{
 				az.setLobbyREDspawn(new Location(world, fc.getInt("lobbyRed.x"), fc.getInt("lobbyRed.y"), fc.getInt("lobbyRed.z")));
 				
 				int spawnsAmt = fc.getInt("spawnsAmt");
 				for (int i = 0; i < spawnsAmt; i++) 
 				{
 					String path = "spawns." + i + ".";
 	
 					Location loc = new Location(world, fc.getInt(path + "x"),fc.getInt(path + "y"), fc.getInt(path + "z"));
 					
 					az.getSpawns().add(loc);
 				}
 				
 			}
 			if (arenaType.equals("spleef"))
 			{
 				az.setLobbyREDspawn(new Location(world, fc.getInt("lobbyRed.x"), fc.getInt("lobbyRed.y"), fc.getInt("lobbyRed.z")));
 				
 				az.setSpecialType(fc.getInt("specialType"));
 				
 				fc.set("specialType", 80);
 				
 				for (int i = 0; i < 4; i++) 
 				{
 					String path = "flags." + i + ".";
 	
 					Location loc = new Location(world, fc.getInt(path + "x"),fc.getInt(path + "y"), fc.getInt(path + "z"));
 					
 					az.getFlags().add(loc);
 				}
 			}
 			if (arenaType.equals("bomb"))
 			{
 				az.setLobbyREDspawn(new Location(world, fc.getInt("lobbyRed.x"), fc.getInt("lobbyRed.y"), fc.getInt("lobbyRed.z")));
 				
 				az.setLobbyBLUspawn(new Location(world, fc.getInt("lobbyBlue.x"), fc.getInt("lobbyBlue.y"), fc.getInt("lobbyBlue.z")));
 				
 				az.setTeam1spawn(new Location(world, fc.getInt("team1.x"), fc.getInt("team1.y"), fc.getInt("team1.z")));
 				
 				az.setTeam2spawn(new Location(world, fc.getInt("team2.x"), fc.getInt("team2.y"), fc.getInt("team2.z")));
 				
 				az.getFlags().add(new Location(world, fc.getInt("flag0.x"),fc.getInt("flag0.y"), fc.getInt("flag0.z")));
 				az.getFlags().add(new Location(world, fc.getInt("flag1.x"),fc.getInt("flag1.y"), fc.getInt("flag1.z")));
 			}
 			if (arenaType.equals("ctf")) 
 			{
 				az.setLobbyREDspawn(new Location(world, fc.getInt("lobbyRed.x"), fc.getInt("lobbyRed.y"), fc.getInt("lobbyRed.z")));
 				
 				az.setLobbyBLUspawn(new Location(world, fc.getInt("lobbyBlue.x"), fc.getInt("lobbyBlue.y"), fc.getInt("lobbyBlue.z")));
 				
 				az.setTeam1spawn(new Location(world, fc.getInt("team1.x"), fc.getInt("team1.y"), fc.getInt("team1.z")));
 				
 				az.setTeam2spawn(new Location(world, fc.getInt("team2.x"), fc.getInt("team2.y"), fc.getInt("team2.z")));
 				
 				az.getFlags().add(new Location(world, fc.getInt("flag0.x"),fc.getInt("flag0.y"), fc.getInt("flag0.z")));
 				az.getFlags().add(new Location(world, fc.getInt("flag1.x"),fc.getInt("flag1.y"), fc.getInt("flag1.z")));
 			}
 			if (arenaType.equals("infect"))
 			{
 				az.setLobbyREDspawn(new Location(world, fc.getInt("lobbyRed.x"), fc.getInt("lobbyRed.y"), fc.getInt("lobbyRed.z")));
 				
 				az.setLobbyBLUspawn(new Location(world, fc.getInt("lobbyBlue.x"), fc.getInt("lobbyBlue.y"), fc.getInt("lobbyBlue.z")));
 				
 				az.setTeam1spawn(new Location(world, fc.getInt("team1.x"), fc.getInt("team1.y"), fc.getInt("team1.z")));
 				
 				az.setTeam2spawn(new Location(world, fc.getInt("team2.x"), fc.getInt("team2.y"), fc.getInt("team2.z")));
 			}
 			
 			az.setMaxPlayers(fc.getInt("maxPlayers"));
 			az.setDefaultClass(fc.getString("defaultClass"));
 			
 			az.setLoaded(true);
 		}
 		catch (Exception e)
 		{
 			plugin.getLogger().severe("Error loading arena \"" + az.getArenaName() + "\": " + e.getMessage());
 			az.setLoaded(false);
 		}
 	}
 	
 	/**Saves a SavedArenaPlayer**/
 	public void savePlayer(SavedArenaPlayer player)
 	{
 		try
 		{
 			File folder = new File(plugin.getDataFolder(), "players");
 			File file = new File(folder, player.getName() + ".dat");
 			if (file.exists()) file.delete();
 			
 			file.createNewFile();
 			
 			YamlConfiguration fc = YamlConfiguration.loadConfiguration(file);
 			fc.set("name", player.getName());
 			
 			int level = player.getLevels();
 			fc.set("level", level);
 			
 			Location loc = player.getLocation();
 
 			int x = loc.getBlockX();
 			int y = loc.getBlockY();
 			int z = loc.getBlockZ();
 			World world = loc.getWorld();
 							
 			fc.set("loc.world", world.getName());
 			fc.set("loc.x", x);
 			fc.set("loc.y", y);
 			fc.set("loc.z", z);
 			
 			int i=0;
 			for (ItemStack stack : player.getSavedInventory())
 			{
 				String path = "item." + i + ".";
 				fc.set(path + "type", stack.getTypeId());
 				fc.set(path + "amt", stack.getAmount());
 				fc.set(path + "dat", stack.getData().getData());
 				
 				Map<Enchantment, Integer> enchantments = stack.getEnchantments();
 				boolean hasEnchants = enchantments.size() > 0;
 				int enchantsAmt = enchantments.size();
 				
 				fc.set(path + "hasEnchants", hasEnchants);
 				fc.set(path + "enchantsAmt", enchantsAmt);
 				
 				
 				int ii=0;
 				for (Entry<Enchantment, Integer> entrySet : enchantments.entrySet())
 				{
 					String epath = path + "enchantments." + ii + ".";
 					fc.set(epath + "name", entrySet.getKey().getName());
 					fc.set(epath + "level", entrySet.getValue());
 					ii++; 
 				}
 				
 				ItemMeta meta = stack.getItemMeta();
 				fc.set(path + "hasName", meta.hasDisplayName());
 				if (meta.hasDisplayName())
 				{
 					fc.set(path + "name", meta.getDisplayName());
 				}
 				
				fc.set(path + "hasLore", meta.hasLore());
 				if (meta.hasLore())
 				{
 					int iii = 0;
 					for (String lore : meta.getLore())
 					{
 						fc.set(path + "lore." + iii, lore);
 						iii++;
 					}
 					fc.set("loreamt", iii);
 				}
 				i++;
 			}
 			fc.set("itemAmt", i);
 			
 			int a=0;
 			for (ItemStack stack : player.getSavedArmor())
 			{
 				String path = "item." + a + ".";
 				fc.set(path + "type", stack.getTypeId());
 				fc.set(path + "amt", stack.getAmount());
 				fc.set(path + "dat", stack.getData().getData());
 				
 				Map<Enchantment, Integer> enchantments = stack.getEnchantments();
 				boolean hasEnchants = enchantments.size() > 0;
 				int enchantsAmt = enchantments.size();
 				
 				fc.set(path + "hasEnchants", hasEnchants);
 				fc.set(path + "enchantsAmt", enchantsAmt);
 				
 				
 				int aa=0;
 				for (Entry<Enchantment, Integer> entrySet : enchantments.entrySet())
 				{
 					String epath = path + "enchantments." + aa + ".";
 					fc.set(epath + "name", entrySet.getKey().getName());
 					fc.set(epath + "level", entrySet.getValue());
 					aa++; 
 				}
 				
 				ItemMeta meta = stack.getItemMeta();
 				fc.set(path + "hasName", meta.hasDisplayName());
 				if (meta.hasDisplayName())
 				{
 					fc.set(path + "name", meta.getDisplayName());
 				}
 				
				fc.set(path + "hasLore", meta.hasLore());
 				if (meta.hasLore())
 				{
 					int aaa=0;
 					for (String lore : meta.getLore())
 					{
 						fc.set(path + "lore." + aaa, lore);
 						aaa++;
 					}
 					fc.set("loreamt", aaa);
 				}
 				a++;
 			}
 			fc.set("armorAmt", a);
 			
 			fc.save(file);
 		}
 		catch (Exception e)
 		{
 			plugin.getLogger().severe("Error saving player " + player.getName() + ": " + e.getMessage());
 		}
 	}
 	
 	/**Removes a player from the saved file**/
 	public void deletePlayer(Player player)
 	{
 		File folder = new File(plugin.getDataFolder(), "players");
 		File file = new File(folder, player.getName() + ".dat");
 		
 		if (file.exists())
 			file.delete();
 	}
 	
 	/**Normalize Players on enable**/
 	public List<SavedArenaPlayer> getSavedPlayers()
 	{
 		List<SavedArenaPlayer> players = new ArrayList<SavedArenaPlayer>();
 
 		File folder = new File(plugin.getDataFolder(), "players");
 		if (!folder.exists())
 		{
 			plugin.createDirectories();
 			return players;
 		}
 		
 		File[] children = folder.listFiles();
 		for (File file : children)
 		{
 			YamlConfiguration fc = YamlConfiguration.loadConfiguration(file);
 			
 			String name = fc.getString("name");
 
 			int level = 0;
 			if (fc.get("exp") != null)
 			{
 				fc.set("exp", null);
 				level = 1;
 				
 			}
 			else
 			{
 				level = fc.getInt("level");
 			}
 			
 			World world = plugin.getServer().getWorld(fc.getString("loc.world"));
 			int x = fc.getInt("loc.x");
 			int y = fc.getInt("loc.y");
 			int z = fc.getInt("loc.z");
 			
 			Location loc = new Location(world, x, y, z);
 			
 			List<ItemStack> items = new ArrayList<ItemStack>();
 			for (int i=0; i<fc.getInt("itemAmt"); i++)
 			{
 				String path = "item." + i + ".";
 				int id = fc.getInt(path + "type");
 				int amt = fc.getInt(path + "amt");
 				int dat = fc.getInt(path + "dat");
 
 				ItemStack stack = new ItemStack(id, amt);
 				if (dat > 0)
 				{
 					MaterialData data = stack.getData();
 					data.setData((byte)dat);
 					stack.setData(data);
 				}
 				
 				if (fc.getBoolean(path + "hasEnchants"))
 				{
 					int enchantsAmt = fc.getInt(path + "enchantsAmt");
 					for (int e=0; e<enchantsAmt; e++)
 					{
 						String epath = path + "enchantment" + e;
 						Enchantment enchantment = Enchantment.getByName(fc.getString(epath + "name"));
 						int elevel = fc.getInt(epath + "level");
 						
 						stack.addUnsafeEnchantment(enchantment, elevel);
 					}
 				}
 				
 				ItemMeta meta = stack.getItemMeta();
 				if (fc.getBoolean(path + "hasName"))
 				{
 					meta.setDisplayName(fc.getString(path + "name"));
 				}
 				
 				if (fc.getBoolean(path + "hasLore"))
 				{
 					List<String> lore = new ArrayList<String>();
 					
 					int loreAmt = fc.getInt(path + "loreAmt");
 					for (int m=0; m<loreAmt; m++)
 					{
 						String mpath = path + "lore." + m;
 						lore.add(fc.getString(mpath));
 					}
 					
 					meta.setLore(lore);
 				}
 				
 				stack.setItemMeta(meta);
 				
 				items.add(stack);
 			}
 			
 			List<ItemStack> armor = new ArrayList<ItemStack>();
 			for (int i=0; i<fc.getInt("armorAmt"); i++)
 			{
 				String path = "armor" + i;
 				int id = fc.getInt(path + "type");
 				int amt = fc.getInt(path + "amt");
 				int dat = fc.getInt(path + "dat");
 				
 				ItemStack stack = new ItemStack(id, amt);
 				if (dat > 0)
 				{
 					MaterialData data = stack.getData();
 					data.setData((byte)dat);
 					stack.setData(data);
 				}
 				
 				if (fc.getBoolean(path + "hasEnchants"))
 				{
 					int enchantsAmt = fc.getInt(path + "enchantsAmt");
 					for (int e=0; e<enchantsAmt; e++)
 					{
 						String epath = path + "enchantment" + e;
 						Enchantment enchantment = Enchantment.getByName(fc.getString(epath + "name"));
 						int elevel = fc.getInt(epath + "level");
 						
 						stack.addUnsafeEnchantment(enchantment, elevel);
 					}
 				}
 				
 				ItemMeta meta = stack.getItemMeta();
 				if (fc.getBoolean(path + "hasName"))
 				{
 					meta.setDisplayName(fc.getString(path + "name"));
 				}
 				
 				if (fc.getBoolean(path + "hasLore"))
 				{
 					List<String> lore = new ArrayList<String>();
 					
 					int loreAmt = fc.getInt(path + "loreAmt");
 					for (int m=0; m<loreAmt; m++)
 					{
 						String mpath = path + "lore." + m;
 						lore.add(fc.getString(mpath));
 					}
 					
 					meta.setLore(lore);
 				}
 				
 				stack.setItemMeta(meta);
 				
 				armor.add(stack);
 			}
 			
 			SavedArenaPlayer sp = new SavedArenaPlayer(name, level, loc, items, armor);
 			players.add(sp);
 		}
 		
 		return players;
 	}
 	
 	public void saveSign(ArenaSign sign)
 	{
 		try
 		{
 			File signFile = new File(plugin.getDataFolder(), "signs.yml");
 			if (!signFile.exists()) signFile.createNewFile();
 			
 			YamlConfiguration fc = YamlConfiguration.loadConfiguration(signFile);
 			
 			String path = "signs." + sign.getId() + ".";
 			String locpath = path + "location.";
 			Location loc = sign.getLocation();
 			
 			fc.set(locpath + "world", loc.getWorld().getName());
 			fc.set(locpath + "x", loc.getBlockX());
 			fc.set(locpath + "y", loc.getBlockY());
 			fc.set(locpath + "z", loc.getBlockZ());
 			
 			if (fc.get(path + "autoAssign") != null)
 				fc.set(path + "autoAssign", null);
 			
 			fc.set(path + "isJoin", sign.isJoinSign());
 			fc.set(path + "isStatus", sign.isStatusSign());
 			fc.set(path + "name", sign.getArena());
 			
 			fc.set("total", plugin.arenaSigns.size());
 			
 			fc.save(signFile);
 		}
 		catch (Exception e)
 		{
 			plugin.getLogger().severe("Error saving signs: " + e.getMessage());
 		}
 	}
 	
 	public List<ArenaSign> loadSigns()
 	{
 		List<ArenaSign> signs = new ArrayList<ArenaSign>();
 		
 		File signFile = new File(plugin.getDataFolder(), "signs.yml");
 		if (!signFile.exists())
 		{
 			return signs;
 		}
 		
 		YamlConfiguration fc = YamlConfiguration.loadConfiguration(signFile);
 		for (int i=0; i<fc.getInt("total"); i++)
 		{
 			String path = "signs." + i + ".";
 			String locpath = path + "location.";
 			
 			World world = plugin.getServer().getWorld(fc.getString(locpath + "world"));
 			Location loc = new Location(world, fc.getInt(locpath + "x"), fc.getInt(locpath + "y"), fc.getInt(locpath + "z"));
 			
 			ArenaZone az = plugin.getArenaZone(fc.getString(path + "name"));
 			if (az != null)
 			{
 				if (fc.get("isJoin") == null)
 				{
 					fc.set("isJoin", true);
 					fc.set("isStatus", false);
 					fc.set("autoAssign", null);
 				}
 				
 				boolean isJoin = fc.getBoolean(path + "isJoin");
 				ArenaSign sign = new ArenaSign(plugin, loc, az, isJoin, i);
 				signs.add(sign);
 			}
 		}
 		
 		try { fc.save(signFile); }
 		catch (Exception e) {}
 		
 		return signs;
 	}
 	
 	public void refreshSignSave()
 	{
 		File signFile = new File(plugin.getDataFolder(), "signs.yml");
 		if (!signFile.exists())
 			return;
 		
 		signFile.delete();
 		
 		try { signFile.createNewFile(); }
 		catch (Exception e) {}
 		
 		for (ArenaSign sign : plugin.arenaSigns)
 		{
 			saveSign(sign);
 		}
 	}
 }
