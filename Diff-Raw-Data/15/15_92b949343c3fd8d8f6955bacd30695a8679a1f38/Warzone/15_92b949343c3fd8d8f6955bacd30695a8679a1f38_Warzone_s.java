 package com.tommytony.war;
 
 import java.util.ArrayList;
 import java.util.HashMap;
 import java.util.Iterator;
 import java.util.List;
 import java.util.Map;
 import java.util.logging.Level;
 
 import org.bukkit.ChatColor;
 import org.bukkit.GameMode;
 import org.bukkit.Location;
 import org.bukkit.Material;
 import org.bukkit.World;
 import org.bukkit.block.Block;
 import org.bukkit.block.BlockFace;
 import org.bukkit.craftbukkit.entity.CraftItem;
 import org.bukkit.entity.Entity;
 import org.bukkit.entity.Item;
 import org.bukkit.entity.Player;
 import org.bukkit.event.player.PlayerMoveEvent;
 import org.bukkit.inventory.ItemStack;
 import org.bukkit.inventory.PlayerInventory;
 import org.getspout.spoutapi.SpoutManager;
 import org.getspout.spoutapi.player.SpoutPlayer;
 
 import bukkit.tommytony.war.War;
 import bukkit.tommytony.war.WarSpoutListener;
 
 import com.tommytony.war.config.InventoryBag;
 import com.tommytony.war.config.TeamConfig;
 import com.tommytony.war.config.TeamConfigBag;
 import com.tommytony.war.config.WarzoneConfig;
 import com.tommytony.war.config.WarzoneConfigBag;
 import com.tommytony.war.jobs.InitZoneJob;
 import com.tommytony.war.jobs.LoadoutResetJob;
 import com.tommytony.war.jobs.ScoreCapReachedJob;
 import com.tommytony.war.mappers.LoadoutYmlMapper;
 import com.tommytony.war.spout.SpoutMessenger;
 import com.tommytony.war.utils.PlayerState;
 import com.tommytony.war.volumes.ZoneVolume;
 
 /**
  *
  * @author tommytony
  * @package com.tommytony.war
  */
 public class Warzone {
 	private String name;
 	private ZoneVolume volume;
 	private World world;
 	private final List<Team> teams = new ArrayList<Team>();
 	private final List<Monument> monuments = new ArrayList<Monument>();
 	private Location teleport;
 	private ZoneLobby lobby;
 	private Location rallyPoint;
 	
 	private final List<String> authors = new ArrayList<String>();
 	
 	private final int minSafeDistanceFromWall = 6;
 	private List<ZoneWallGuard> zoneWallGuards = new ArrayList<ZoneWallGuard>();
 	private HashMap<String, PlayerState> playerStates = new HashMap<String, PlayerState>();
 	private HashMap<String, Team> flagThieves = new HashMap<String, Team>();
 	private HashMap<String, LoadoutSelection> loadoutSelections = new HashMap<String, LoadoutSelection>();
 	private HashMap<String, PlayerState> deadMenInventories = new HashMap<String, PlayerState>();
 	private final List<Player> respawn = new ArrayList<Player>();
 	
 	private final WarzoneConfigBag warzoneConfig = new WarzoneConfigBag();
 	private final TeamConfigBag teamDefaultConfig = new TeamConfigBag();
 	private InventoryBag defaultInventories = new InventoryBag();
 
 	public Warzone(World world, String name) {
 		this.world = world;
 		this.name = name;
 		this.volume = new ZoneVolume(name, this.getWorld(), this);
 	}
 
 	public static Warzone getZoneByName(String name) {
 		Warzone bestGuess = null;
 		for (Warzone warzone : War.war.getWarzones()) {
 			if (warzone.getName().toLowerCase().equals(name.toLowerCase())) {
 				// perfect match, return right away
 				return warzone;
 			} else if (warzone.getName().toLowerCase().startsWith(name.toLowerCase())) {
 				// perhaps there's a perfect match in the remaining zones, let's take this one aside
 				bestGuess = warzone;
 			}
 		}
 		return bestGuess;
 	}
 
 	public static Warzone getZoneByLocation(Location location) {
 		for (Warzone warzone : War.war.getWarzones()) {
 			if (location.getWorld().getName().equals(warzone.getWorld().getName()) && warzone.getVolume() != null && warzone.getVolume().contains(location)) {
 				return warzone;
 			}
 		}
 		return null;
 	}
 
 	public static Warzone getZoneByLocation(Player player) {
 		return Warzone.getZoneByLocation(player.getLocation());
 	}
 
 	public static Warzone getZoneByPlayerName(String playerName) {
 		for (Warzone warzone : War.war.getWarzones()) {
 			Team team = warzone.getPlayerTeam(playerName);
 			if (team != null) {
 				return warzone;
 			}
 		}
 		return null;
 	}
 
 	public boolean ready() {
 		if (this.volume.hasTwoCorners() && !this.volume.tooSmall() && !this.volume.tooBig()) {
 			return true;
 		}
 		return false;
 	}
 
 	public List<Team> getTeams() {
 		return this.teams;
 	}
 
 	public Team getPlayerTeam(String playerName) {
 		for (Team team : this.teams) {
 			for (Player player : team.getPlayers()) {
 				if (player.getName().equals(playerName)) {
 					return team;
 				}
 			}
 		}
 		return null;
 	}
 
 	public String getTeamInformation() {
 		String teamsMessage = "Teams: ";
 		if (this.getTeams().isEmpty()) {
 			teamsMessage += "none.";
 		} else {
 			for (Team team : this.getTeams()) {
 				teamsMessage += team.getName() + " (" + team.getPoints() + " points, " + team.getRemainingLifes() + "/" 
 					+ team.getTeamConfig().resolveInt(TeamConfig.LIFEPOOL) + " lives left. ";
 				for (Player member : team.getPlayers()) {
 					teamsMessage += member.getName() + " ";
 				}
 				teamsMessage += ")  ";
 			}
 		}
 		return teamsMessage;
 	}
 
 	public String getName() {
 		return this.name;
 	}
 
 	@Override
 	public String toString() {
 		return this.getName();
 	}
 
 	public void setTeleport(Location location) {
 		this.teleport = location;
 	}
 
 	public Location getTeleport() {
 		return this.teleport;
 	}
 
 	public int saveState(boolean clearArtifacts) {
 		if (this.ready()) {
 			if (clearArtifacts) {
 				// removed everything to keep save clean
 				for (ZoneWallGuard guard : this.zoneWallGuards) {
 					guard.deactivate();
 				}
 				this.zoneWallGuards.clear();
 
 				for (Team team : this.teams) {
 					team.getSpawnVolume().resetBlocks();
 					if (team.getTeamFlag() != null) {
 						team.getFlagVolume().resetBlocks();
 					}
 				}
 
 				for (Monument monument : this.monuments) {
 					monument.getVolume().resetBlocks();
 				}
 
 				if (this.lobby != null) {
 					this.lobby.getVolume().resetBlocks();
 				}
 			}
 
 			int saved = this.volume.saveBlocks();
 			if (clearArtifacts) {
 				this.initializeZone(); // bring back stuff
 			}
 			return saved;
 		}
 		return 0;
 	}
 
 	/**
 	 * Goes back to the saved state of the warzone (resets only block types, not physics). Also teleports all players back to their respective spawns.
 	 *
 	 * @return
 	 */
 	public void initializeZone() {
 		this.initializeZone(null);
 	}
 
 	public void initializeZone(Player respawnExempted) {
 		if (this.ready() && this.volume.isSaved()) {
 			// everyone back to team spawn with full health
 			for (Team team : this.teams) {
 				for (Player player : team.getPlayers()) {
 					if (player != respawnExempted) {
 						this.respawnPlayer(team, player);
 					}
 				}
 				team.setRemainingLives(team.getTeamConfig().resolveInt(TeamConfig.LIFEPOOL));
 				team.initializeTeamSpawn();
 				if (team.getTeamFlag() != null) {
 					team.setTeamFlag(team.getTeamFlag());
 				}
 			}
 
 			this.initZone();
 		}
 	}
 
 	public void initializeZoneAsJob(Player respawnExempted) {
 		InitZoneJob job = new InitZoneJob(this, respawnExempted);
 		War.war.getServer().getScheduler().scheduleSyncDelayedTask(War.war, job);
 	}
 
 	public void initializeZoneAsJob() {
 		InitZoneJob job = new InitZoneJob(this);
 		War.war.getServer().getScheduler().scheduleSyncDelayedTask(War.war, job);
 	}
 
 	private void initZone() {
 		// reset monuments
 		for (Monument monument : this.monuments) {
 			monument.getVolume().resetBlocks();
 			monument.addMonumentBlocks();
 		}
 
 		// reset lobby (here be demons)
 		if (this.lobby != null) {
 			this.lobby.initialize();
 		}
 
 		this.flagThieves.clear();
 
 		// nom drops
 		for(Entity entity : (this.getWorld().getEntities())) {
 			if (!(entity instanceof Item) && !(entity instanceof CraftItem)) {
 				continue;
 			}
 			// validate position
 			if (!this.getVolume().contains(entity.getLocation())) {
 				continue;
 			}
 
 			// omnomnomnom
 			entity.remove();
 		}
 	}
 
 	public void endRound() {
 
 	}
 
 	public void respawnPlayer(Team team, Player player) {
 		this.handleRespawn(team, player);
 		// Teleport the player back to spawn
 		player.teleport(team.getTeamSpawn());
 	}
 
 	public void respawnPlayer(PlayerMoveEvent event, Team team, Player player) {
 		this.handleRespawn(team, player);
 		// Teleport the player back to spawn
 		event.setTo(team.getTeamSpawn());
 	}
 	
 	public boolean isRespawning(Player p) {
 		return respawn.contains(p);
 	}
 
 	private void handleRespawn(final Team team, final Player player) {
 		// Fill hp
 		player.setRemainingAir(300);
 		player.setHealth(20);
 		player.setFoodLevel(20);
 		player.setSaturation(team.getTeamConfig().resolveInt(TeamConfig.SATURATION));
 		player.setExhaustion(0);
 		player.setFireTicks(0);		//this works fine here, why put it in LoudoutResetJob...? I'll keep it over there though
 		
 		player.getInventory().clear();
 		
 		if (player.getGameMode() == GameMode.CREATIVE) {
 			player.setGameMode(GameMode.SURVIVAL);
 		}
 		// clear potion effects
 		PotionEffect.clearPotionEffects(player);
 		
 		boolean isFirstRespawn = false;
 		if (!this.getLoadoutSelections().keySet().contains(player.getName())) {
 			isFirstRespawn = true;
 			this.getLoadoutSelections().put(player.getName(), new LoadoutSelection(true, 0));
 		} else {
 			this.getLoadoutSelections().get(player.getName()).setStillInSpawn(true);
 		}
 		
 		// Spout
 		if (War.war.isSpoutServer()) {
 			SpoutManager.getPlayer(player).setTitle(team.getKind().getColor() + player.getName());
 		}
 		
 		final LoadoutResetJob job = new LoadoutResetJob(this, team, player, isFirstRespawn, false);
 		if (team.getTeamConfig().resolveInt(TeamConfig.RESPAWNTIMER) == 0 || isFirstRespawn) {
 			War.war.getServer().getScheduler().scheduleSyncDelayedTask(War.war, job);
 		}			
 		else {
 			// "Respawn" Timer - player will not be able to leave spawn for a few seconds
 			respawn.add(player);
 			
 			War.war.getServer().getScheduler().scheduleSyncDelayedTask(War.war, new Runnable() {
 				public void run() {
 				    respawn.remove(player);
 					War.war.getServer().getScheduler().scheduleSyncDelayedTask(War.war, job);
 				}
 			}, team.getTeamConfig().resolveInt(TeamConfig.RESPAWNTIMER) * 20L); // 20 ticks = 1 second
 		}
 	}
 
 	public void resetInventory(Team team, Player player, HashMap<Integer, ItemStack> loadout) {
 		// Reset inventory to loadout
 		PlayerInventory playerInv = player.getInventory();
 		playerInv.clear();
 		playerInv.clear(playerInv.getSize() + 0);
 		playerInv.clear(playerInv.getSize() + 1);
 		playerInv.clear(playerInv.getSize() + 2);
 		playerInv.clear(playerInv.getSize() + 3); // helmet/blockHead
 		for (Integer slot : loadout.keySet()) {
 			if (slot == 100) {
 				playerInv.setBoots(War.war.copyStack(loadout.get(slot)));
 			} else if (slot == 101) {
 				playerInv.setLeggings(War.war.copyStack(loadout.get(slot)));
 			} else if (slot == 102) {
 				playerInv.setChestplate(War.war.copyStack(loadout.get(slot)));
 			} else {
 				ItemStack item = loadout.get(slot);
 				if (item != null) {
 					playerInv.addItem(War.war.copyStack(item));
 				}
 			}
 		}
 		if (this.getWarzoneConfig().getBoolean(WarzoneConfig.BLOCKHEADS)) {
 			playerInv.setHelmet(new ItemStack(team.getKind().getMaterial(), 1, (short) 1, new Byte(team.getKind().getData())));
 		} else {
 			if (team.getKind() == TeamKind.GOLD) {
 				playerInv.setHelmet(new ItemStack(Material.GOLD_HELMET));
 			} else if (team.getKind() == TeamKind.DIAMOND) {
 				playerInv.setHelmet(new ItemStack(Material.DIAMOND_HELMET));
 			} else if (team.getKind() == TeamKind.IRON) {
 				playerInv.setHelmet(new ItemStack(Material.IRON_HELMET));
 			} else {
 				playerInv.setHelmet(new ItemStack(Material.LEATHER_HELMET));
 			}
 		}
 	}
 
 	public boolean isMonumentCenterBlock(Block block) {
 		for (Monument monument : this.monuments) {
 			int x = monument.getLocation().getBlockX();
 			int y = monument.getLocation().getBlockY() + 1;
 			int z = monument.getLocation().getBlockZ();
 			if (x == block.getX() && y == block.getY() && z == block.getZ()) {
 				return true;
 			}
 		}
 		return false;
 	}
 
 	public Monument getMonumentFromCenterBlock(Block block) {
 		for (Monument monument : this.monuments) {
 			int x = monument.getLocation().getBlockX();
 			int y = monument.getLocation().getBlockY() + 1;
 			int z = monument.getLocation().getBlockZ();
 			if (x == block.getX() && y == block.getY() && z == block.getZ()) {
 				return monument;
 			}
 		}
 		return null;
 	}
 
 	public boolean nearAnyOwnedMonument(Location to, Team team) {
 		for (Monument monument : this.monuments) {
 			if (monument.isNear(to) && monument.isOwner(team)) {
 				return true;
 			}
 		}
 		return false;
 	}
 
 	public List<Monument> getMonuments() {
 		return this.monuments;
 	}
 
 //	public void setLifePool(int lifePool) {
 //		this.lifePool = lifePool;
 //		for (Team team : this.teams) {
 //			team.setRemainingLives(lifePool);
 //		}
 //	}
 
 	public boolean hasPlayerState(String playerName) {
 		return this.playerStates.containsKey(playerName);
 	}
 
 	public void keepPlayerState(Player player) {
 		PlayerInventory inventory = player.getInventory();
 		ItemStack[] contents = inventory.getContents();
 		List<PotionEffect> potionEffects = PotionEffect.getCurrentPotionEffects(player);
 		this.playerStates.put(player.getName(), new PlayerState(player.getGameMode(), 
 																contents, 
 																inventory.getHelmet(), 
 																inventory.getChestplate(), 
 																inventory.getLeggings(), 
 																inventory.getBoots(), 
 																player.getHealth(), 
 																player.getExhaustion(), 
 																player.getSaturation(), 
 																player.getFoodLevel(), 
 																potionEffects,
																SpoutManager.getPlayer(player).getTitle()));
 	}
 
 	public void restorePlayerState(Player player) {
 		PlayerState originalContents = this.playerStates.remove(player.getName());
 		PlayerInventory playerInv = player.getInventory();
 		if (originalContents != null) {
 			this.playerInvFromInventoryStash(playerInv, originalContents);
 			player.setGameMode(originalContents.getGamemode());
 			player.setHealth(originalContents.getHealth());
 			player.setExhaustion(originalContents.getExhaustion());
 			player.setSaturation(originalContents.getSaturation());
 			player.setFoodLevel(originalContents.getFoodLevel());
 			PotionEffect.restorePotionEffects(player, originalContents.getPotionEffects());
			SpoutManager.getPlayer(player).setTitle(originalContents.getPlayerTitle());
 		}
 	}
 
 	private void playerInvFromInventoryStash(PlayerInventory playerInv, PlayerState originalContents) {
 		playerInv.clear();
 		playerInv.clear(playerInv.getSize() + 0);
 		playerInv.clear(playerInv.getSize() + 1);
 		playerInv.clear(playerInv.getSize() + 2);
 		playerInv.clear(playerInv.getSize() + 3); // helmet/blockHead
 		for (ItemStack item : originalContents.getContents()) {
 			if (item != null && item.getTypeId() != 0) {
 				playerInv.addItem(item);
 			}
 		}
 		if (originalContents.getHelmet() != null && originalContents.getHelmet().getType() != Material.AIR) {
 			playerInv.setHelmet(originalContents.getHelmet());
 		}
 		if (originalContents.getChest() != null && originalContents.getChest().getType() != Material.AIR) {
 			playerInv.setChestplate(originalContents.getChest());
 		}
 		if (originalContents.getLegs() != null && originalContents.getLegs().getType() != Material.AIR) {
 			playerInv.setLeggings(originalContents.getLegs());
 		}
 		if (originalContents.getFeet() != null && originalContents.getFeet().getType() != Material.AIR) {
 			playerInv.setBoots(originalContents.getFeet());
 		}
 	}
 
 	public PlayerState getPlayerState(String playerName) {
 		if (this.playerStates.containsKey(playerName)) {
 			return this.playerStates.get(playerName);
 		}
 		return null;
 	}
 
 	public boolean hasMonument(String monumentName) {
 		for (Monument monument : this.monuments) {
 			if (monument.getName().equals(monumentName)) {
 				return true;
 			}
 		}
 		return false;
 	}
 
 	public Monument getMonument(String monumentName) {
 		for (Monument monument : this.monuments) {
 			if (monument.getName().startsWith(monumentName)) {
 				return monument;
 			}
 		}
 		return null;
 	}
 
 	public boolean isImportantBlock(Block block) {
 		if (this.ready()) {
 			for (Monument m : this.monuments) {
 				if (m.getVolume().contains(block)) {
 					return true;
 				}
 			}
 			for (Team t : this.teams) {
 				if (t.getSpawnVolume().contains(block)) {
 					return true;
 				} else if (t.getFlagVolume() != null && t.getFlagVolume().contains(block)) {
 					return true;
 				}
 			}
 			if (this.volume.isWallBlock(block)) {
 				return true;
 			}
 		}
 		return false;
 	}
 
 	public World getWorld() {
 
 		return this.world;
 	}
 
 	public void setWorld(World world) {
 		this.world = world;
 	}
 
 	public ZoneVolume getVolume() {
 		return this.volume;
 	}
 
 	public void setVolume(ZoneVolume zoneVolume) {
 		this.volume = zoneVolume;
 	}
 
 	public Team getTeamByKind(TeamKind kind) {
 		for (Team t : this.teams) {
 			if (t.getKind() == kind) {
 				return t;
 			}
 		}
 		return null;
 	}
 
 	public boolean isNearWall(Location latestPlayerLocation) {
 		if (this.volume.hasTwoCorners()) {
 			if (Math.abs(this.volume.getSoutheastZ() - latestPlayerLocation.getBlockZ()) < this.minSafeDistanceFromWall && latestPlayerLocation.getBlockX() <= this.volume.getSoutheastX() && latestPlayerLocation.getBlockX() >= this.volume.getNorthwestX() && latestPlayerLocation.getBlockY() >= this.volume.getMinY() && latestPlayerLocation.getBlockY() <= this.volume.getMaxY()) {
 				return true; // near east wall
 			} else if (Math.abs(this.volume.getSoutheastX() - latestPlayerLocation.getBlockX()) < this.minSafeDistanceFromWall && latestPlayerLocation.getBlockZ() <= this.volume.getNorthwestZ() && latestPlayerLocation.getBlockZ() >= this.volume.getSoutheastZ() && latestPlayerLocation.getBlockY() >= this.volume.getMinY() && latestPlayerLocation.getBlockY() <= this.volume.getMaxY()) {
 				return true; // near south wall
 			} else if (Math.abs(this.volume.getNorthwestX() - latestPlayerLocation.getBlockX()) < this.minSafeDistanceFromWall && latestPlayerLocation.getBlockZ() <= this.volume.getNorthwestZ() && latestPlayerLocation.getBlockZ() >= this.volume.getSoutheastZ() && latestPlayerLocation.getBlockY() >= this.volume.getMinY() && latestPlayerLocation.getBlockY() <= this.volume.getMaxY()) {
 				return true; // near north wall
 			} else if (Math.abs(this.volume.getNorthwestZ() - latestPlayerLocation.getBlockZ()) < this.minSafeDistanceFromWall && latestPlayerLocation.getBlockX() <= this.volume.getSoutheastX() && latestPlayerLocation.getBlockX() >= this.volume.getNorthwestX() && latestPlayerLocation.getBlockY() >= this.volume.getMinY() && latestPlayerLocation.getBlockY() <= this.volume.getMaxY()) {
 				return true; // near west wall
 			} else if (Math.abs(this.volume.getMaxY() - latestPlayerLocation.getBlockY()) < this.minSafeDistanceFromWall && latestPlayerLocation.getBlockX() <= this.volume.getMaxX() && latestPlayerLocation.getBlockX() >= this.volume.getMinX() && latestPlayerLocation.getBlockZ() <= this.volume.getMaxZ() && latestPlayerLocation.getBlockZ() >= this.volume.getMinZ()) {
 				return true; // near up wall
 			} else if (Math.abs(this.volume.getMinY() - latestPlayerLocation.getBlockY()) < this.minSafeDistanceFromWall && latestPlayerLocation.getBlockX() <= this.volume.getMaxX() && latestPlayerLocation.getBlockX() >= this.volume.getMinX() && latestPlayerLocation.getBlockZ() <= this.volume.getMaxZ() && latestPlayerLocation.getBlockZ() >= this.volume.getMinZ()) {
 				return true; // near down wall
 			}
 		}
 		return false;
 	}
 
 	public List<Block> getNearestWallBlocks(Location latestPlayerLocation) {
 		List<Block> nearestWallBlocks = new ArrayList<Block>();
 		if (Math.abs(this.volume.getSoutheastZ() - latestPlayerLocation.getBlockZ()) < this.minSafeDistanceFromWall && latestPlayerLocation.getBlockX() <= this.volume.getSoutheastX() && latestPlayerLocation.getBlockX() >= this.volume.getNorthwestX() && latestPlayerLocation.getBlockY() >= this.volume.getMinY() && latestPlayerLocation.getBlockY() <= this.volume.getMaxY()) {
 			// near east wall
 			Block eastWallBlock = this.world.getBlockAt(latestPlayerLocation.getBlockX() + 1, latestPlayerLocation.getBlockY() + 1, this.volume.getSoutheastZ());
 			nearestWallBlocks.add(eastWallBlock);
 		}
 
 		if (Math.abs(this.volume.getSoutheastX() - latestPlayerLocation.getBlockX()) < this.minSafeDistanceFromWall && latestPlayerLocation.getBlockZ() <= this.volume.getNorthwestZ() && latestPlayerLocation.getBlockZ() >= this.volume.getSoutheastZ() && latestPlayerLocation.getBlockY() >= this.volume.getMinY() && latestPlayerLocation.getBlockY() <= this.volume.getMaxY()) {
 			// near south wall
 			Block southWallBlock = this.world.getBlockAt(this.volume.getSoutheastX(), latestPlayerLocation.getBlockY() + 1, latestPlayerLocation.getBlockZ());
 			nearestWallBlocks.add(southWallBlock);
 		}
 
 		if (Math.abs(this.volume.getNorthwestX() - latestPlayerLocation.getBlockX()) < this.minSafeDistanceFromWall && latestPlayerLocation.getBlockZ() <= this.volume.getNorthwestZ() && latestPlayerLocation.getBlockZ() >= this.volume.getSoutheastZ() && latestPlayerLocation.getBlockY() >= this.volume.getMinY() && latestPlayerLocation.getBlockY() <= this.volume.getMaxY()) {
 			// near north wall
 			Block northWallBlock = this.world.getBlockAt(this.volume.getNorthwestX(), latestPlayerLocation.getBlockY() + 1, latestPlayerLocation.getBlockZ());
 			nearestWallBlocks.add(northWallBlock);
 		}
 
 		if (Math.abs(this.volume.getNorthwestZ() - latestPlayerLocation.getBlockZ()) < this.minSafeDistanceFromWall && latestPlayerLocation.getBlockX() <= this.volume.getSoutheastX() && latestPlayerLocation.getBlockX() >= this.volume.getNorthwestX() && latestPlayerLocation.getBlockY() >= this.volume.getMinY() && latestPlayerLocation.getBlockY() <= this.volume.getMaxY()) {
 			// near west wall
 			Block westWallBlock = this.world.getBlockAt(latestPlayerLocation.getBlockX(), latestPlayerLocation.getBlockY() + 1, this.volume.getNorthwestZ());
 			nearestWallBlocks.add(westWallBlock);
 		}
 
 		if (Math.abs(this.volume.getMaxY() - latestPlayerLocation.getBlockY()) < this.minSafeDistanceFromWall && latestPlayerLocation.getBlockX() <= this.volume.getMaxX() && latestPlayerLocation.getBlockX() >= this.volume.getMinX() && latestPlayerLocation.getBlockZ() <= this.volume.getMaxZ() && latestPlayerLocation.getBlockZ() >= this.volume.getMinZ()) {
 			// near up wall
 			Block upWallBlock = this.world.getBlockAt(latestPlayerLocation.getBlockX(), this.volume.getMaxY(), latestPlayerLocation.getBlockZ());
 			nearestWallBlocks.add(upWallBlock);
 		}
 
 		if (Math.abs(this.volume.getMinY() - latestPlayerLocation.getBlockY()) < this.minSafeDistanceFromWall && latestPlayerLocation.getBlockX() <= this.volume.getMaxX() && latestPlayerLocation.getBlockX() >= this.volume.getMinX() && latestPlayerLocation.getBlockZ() <= this.volume.getMaxZ() && latestPlayerLocation.getBlockZ() >= this.volume.getMinZ()) {
 			// near down wall
 			Block downWallBlock = this.world.getBlockAt(latestPlayerLocation.getBlockX(), this.volume.getMinY(), latestPlayerLocation.getBlockZ());
 			nearestWallBlocks.add(downWallBlock);
 		}
 		return nearestWallBlocks;
 		// note: y + 1 to line up 3 sided square with player eyes
 	}
 
 	public List<BlockFace> getNearestWalls(Location latestPlayerLocation) {
 		List<BlockFace> walls = new ArrayList<BlockFace>();
 		if (Math.abs(this.volume.getSoutheastZ() - latestPlayerLocation.getBlockZ()) < this.minSafeDistanceFromWall && latestPlayerLocation.getBlockX() <= this.volume.getSoutheastX() && latestPlayerLocation.getBlockX() >= this.volume.getNorthwestX() && latestPlayerLocation.getBlockY() >= this.volume.getMinY() && latestPlayerLocation.getBlockY() <= this.volume.getMaxY()) {
 			// near east wall
 			walls.add(BlockFace.EAST);
 		}
 
 		if (Math.abs(this.volume.getSoutheastX() - latestPlayerLocation.getBlockX()) < this.minSafeDistanceFromWall && latestPlayerLocation.getBlockZ() <= this.volume.getNorthwestZ() && latestPlayerLocation.getBlockZ() >= this.volume.getSoutheastZ() && latestPlayerLocation.getBlockY() >= this.volume.getMinY() && latestPlayerLocation.getBlockY() <= this.volume.getMaxY()) {
 			// near south wall
 			walls.add(BlockFace.SOUTH);
 		}
 
 		if (Math.abs(this.volume.getNorthwestX() - latestPlayerLocation.getBlockX()) < this.minSafeDistanceFromWall && latestPlayerLocation.getBlockZ() <= this.volume.getNorthwestZ() && latestPlayerLocation.getBlockZ() >= this.volume.getSoutheastZ() && latestPlayerLocation.getBlockY() >= this.volume.getMinY() && latestPlayerLocation.getBlockY() <= this.volume.getMaxY()) {
 			// near north wall
 			walls.add(BlockFace.NORTH);
 		}
 
 		if (Math.abs(this.volume.getNorthwestZ() - latestPlayerLocation.getBlockZ()) < this.minSafeDistanceFromWall && latestPlayerLocation.getBlockX() <= this.volume.getSoutheastX() && latestPlayerLocation.getBlockX() >= this.volume.getNorthwestX() && latestPlayerLocation.getBlockY() >= this.volume.getMinY() && latestPlayerLocation.getBlockY() <= this.volume.getMaxY()) {
 			// near west wall
 			walls.add(BlockFace.WEST);
 		}
 
 		if (Math.abs(this.volume.getMaxY() - latestPlayerLocation.getBlockY()) < this.minSafeDistanceFromWall && latestPlayerLocation.getBlockX() <= this.volume.getMaxX() && latestPlayerLocation.getBlockX() >= this.volume.getMinX() && latestPlayerLocation.getBlockZ() <= this.volume.getMaxZ() && latestPlayerLocation.getBlockZ() >= this.volume.getMinZ()) {
 			// near up wall
 			walls.add(BlockFace.UP);
 		}
 
 		if (Math.abs(this.volume.getMinY() - latestPlayerLocation.getBlockY()) < this.minSafeDistanceFromWall && latestPlayerLocation.getBlockX() <= this.volume.getMaxX() && latestPlayerLocation.getBlockX() >= this.volume.getMinX() && latestPlayerLocation.getBlockZ() <= this.volume.getMaxZ() && latestPlayerLocation.getBlockZ() >= this.volume.getMinZ()) {
 			// near down wall
 			walls.add(BlockFace.DOWN);
 		}
 		return walls;
 	}
 
 	public ZoneWallGuard getPlayerZoneWallGuard(String name, BlockFace wall) {
 		for (ZoneWallGuard guard : this.zoneWallGuards) {
 			if (guard.getPlayer().getName().equals(name) && wall == guard.getWall()) {
 				return guard;
 			}
 		}
 		return null;
 	}
 
 	public boolean protectZoneWallAgainstPlayer(Player player) {
 		List<BlockFace> nearestWalls = this.getNearestWalls(player.getLocation());
 		boolean protecting = false;
 		for (BlockFace wall : nearestWalls) {
 			ZoneWallGuard guard = this.getPlayerZoneWallGuard(player.getName(), wall);
 			if (guard != null) {
 				// already protected, need to move the guard
 				guard.updatePlayerPosition(player.getLocation());
 			} else {
 				// new guard
 				guard = new ZoneWallGuard(player, War.war, this, wall);
 				this.zoneWallGuards.add(guard);
 			}
 			protecting = true;
 		}
 		return protecting;
 	}
 
 	public void dropZoneWallGuardIfAny(Player player) {
 		List<ZoneWallGuard> playerGuards = new ArrayList<ZoneWallGuard>();
 		for (ZoneWallGuard guard : this.zoneWallGuards) {
 			if (guard.getPlayer().getName().equals(player.getName())) {
 				playerGuards.add(guard);
 				guard.deactivate();
 			}
 		}
 		// now remove those zone guards
 		for (ZoneWallGuard playerGuard : playerGuards) {
 			this.zoneWallGuards.remove(playerGuard);
 		}
 		playerGuards.clear();
 	}
 
 	public void setLobby(ZoneLobby lobby) {
 		this.lobby = lobby;
 	}
 
 	public ZoneLobby getLobby() {
 		return this.lobby;
 	}
 
 	public Team autoAssign(Player player) {
 		Team lowestNoOfPlayers = null;
 		for (Team t : this.teams) {
 			if (lowestNoOfPlayers == null || (lowestNoOfPlayers != null && lowestNoOfPlayers.getPlayers().size() > t.getPlayers().size())) {
 				lowestNoOfPlayers = t;
 			}
 		}
 		if (lowestNoOfPlayers != null) {
 			lowestNoOfPlayers.addPlayer(player);
 			lowestNoOfPlayers.resetSign();
 			if (!this.hasPlayerState(player.getName())) {
 				this.keepPlayerState(player);
 			}
 			War.war.msg(player, "Your inventory is in storage until you use '/war leave'.");
 			this.respawnPlayer(lowestNoOfPlayers, player);
 			for (Team team : this.teams) {
 				team.teamcast("" + player.getName() + " joined team " + lowestNoOfPlayers.getName() + ".");
 			}
 		}
 		return lowestNoOfPlayers;
 	}
 
 	public void handleDeath(Player player) {
 		Team playerTeam = Team.getTeamByPlayerName(player.getName());
 		Warzone playerWarzone = Warzone.getZoneByPlayerName(player.getName());
 		if (playerTeam != null && playerWarzone != null) {
 			// teleport to team spawn upon death
 
 			playerWarzone.respawnPlayer(playerTeam, player);
 			int remaining = playerTeam.getRemainingLifes();
 			if (remaining == 0) { // your death caused your team to lose
 				List<Team> teams = playerWarzone.getTeams();
 				String scores = "";
 				for (Team t : teams) {
 					if (War.war.isSpoutServer()) {
 						for (Player p : t.getPlayers()) {
 							SpoutPlayer sp = SpoutManager.getPlayer(p);
 							if (sp.isSpoutCraftEnabled()) {
 				                sp.sendNotification(
 				                		SpoutMessenger.cleanForNotification("Round over! " + playerTeam.getKind().getColor() + playerTeam.getName()),
 				                		SpoutMessenger.cleanForNotification("ran out of lives."),
 				                		playerTeam.getKind().getMaterial(),
 				                		playerTeam.getKind().getData(),
 				                		10000);
 							}
 						}
 					}
 					t.teamcast("The battle is over. Team " + playerTeam.getName() + " lost: " + player.getName() + " died and there were no lives left in their life pool.");
 
 					if (t.getPlayers().size() != 0 && !t.getTeamConfig().resolveBoolean(TeamConfig.FLAGPOINTSONLY)) {
 						if (!t.getName().equals(playerTeam.getName())) {
 							// all other teams get a point
 							t.addPoint();
 							t.resetSign();
 						}
 						scores += t.getName() + "(" + t.getPoints() + "/" + t.getTeamConfig().resolveInt(TeamConfig.MAXSCORE) + ") ";
 					}
 				}
 				if (!scores.equals("")) {
 					for (Team t : teams) {
 						t.teamcast("New scores - " + scores);
 					}
 				}
 				// detect score cap
 				List<Team> scoreCapTeams = new ArrayList<Team>();
 				for (Team t : teams) {
 					if (t.getPoints() == t.getTeamConfig().resolveInt(TeamConfig.MAXSCORE)) {
 						scoreCapTeams.add(t);
 					}
 				}
 				if (!scoreCapTeams.isEmpty()) {
 					String winnersStr = "";
 					for (Team winner : scoreCapTeams) {
 						if (winner.getPlayers().size() != 0) {
 							winnersStr += winner.getName() + " ";
 						}
 					}
 
 					playerWarzone.handleScoreCapReached(player, winnersStr);
 					// player.teleport(playerWarzone.getTeleport());
 					// player will die because it took too long :(
 					// we dont restore his inventory in handleScoreCapReached
 					// check out PLAYER_MOVE for the rest of the fix
 
 				} else {
 					// A new battle starts. Reset the zone but not the teams.
 					for (Team t : teams) {
 						t.teamcast("A new battle begins. Resetting warzone...");
 					}
 					playerWarzone.getVolume().resetBlocksAsJob();
 					playerWarzone.initializeZoneAsJob(player);
 				}
 			} else {
 				// player died without causing his team's demise
 				if (playerWarzone.isFlagThief(player.getName())) {
 					// died while carrying flag.. dropped it
 					Team victim = playerWarzone.getVictimTeamForThief(player.getName());
 					victim.getFlagVolume().resetBlocks();
 					victim.initializeTeamFlag();
 					playerWarzone.removeThief(player.getName());
 					
 					if (War.war.isSpoutServer()) {
 						for (Player p : victim.getPlayers()) {
 							SpoutPlayer sp = SpoutManager.getPlayer(p);
 							if (sp.isSpoutCraftEnabled()) {
 				                sp.sendNotification(
 				                		SpoutMessenger.cleanForNotification(playerTeam.getKind().getColor() + player.getName() + ChatColor.YELLOW + " dropped"),
 				                		SpoutMessenger.cleanForNotification(ChatColor.YELLOW + "your flag."),
 				                		playerTeam.getKind().getMaterial(),
 				                		playerTeam.getKind().getData(),
 				                		5000);
 							}
 						}
 					}
 					
 					for (Team t : playerWarzone.getTeams()) {
 						t.teamcast(player.getName() + " died and dropped team " + victim.getName() + "'s flag.");
 					}
 				}
 				playerTeam.setRemainingLives(remaining - 1);
 				if (remaining - 1 == 0) {
 					for (Team t : playerWarzone.getTeams()) {
 						t.teamcast("Team " + playerTeam.getName() + "'s life pool is empty. One more death and they lose the battle!");
 					}
 				}
 			}
 			playerTeam.resetSign();
 		}
 	}
 
 	public void handlePlayerLeave(Player player, Location destination, PlayerMoveEvent event, boolean removeFromTeam) {
 		this.handlePlayerLeave(player, removeFromTeam);
 		event.setTo(destination);
 	}
 
 	public void handlePlayerLeave(Player player, Location destination, boolean removeFromTeam) {
 		this.handlePlayerLeave(player, removeFromTeam);
 		player.teleport(destination);
 	}
 
 	private void handlePlayerLeave(Player player, boolean removeFromTeam) {
 		Team playerTeam = Team.getTeamByPlayerName(player.getName());
 		if (playerTeam != null) {
 			if (removeFromTeam) {
 				playerTeam.removePlayer(player.getName());
 			}
 			for (Team t : this.getTeams()) {
 				t.teamcast(playerTeam.getKind().getColor() + player.getName() + ChatColor.WHITE + " left the zone.");
 			}
 			playerTeam.resetSign();
 			if (this.isFlagThief(player.getName())) {
 				Team victim = this.getVictimTeamForThief(player.getName());
 				victim.getFlagVolume().resetBlocks();
 				victim.initializeTeamFlag();
 				this.removeThief(player.getName());
 				for (Team t : this.getTeams()) {
 					t.teamcast("Team " + victim.getName() + " flag was returned.");
 				}
 			}
 			if (this.getLobby() != null) {
 				this.getLobby().resetTeamGateSign(playerTeam);
 			}
 			if (this.hasPlayerState(player.getName())) {
 				this.restorePlayerState(player);
 			}
 			if (this.getLoadoutSelections().containsKey(player.getName())) {
 				// clear inventory selection
 				this.getLoadoutSelections().remove(player.getName());
 			}
 			player.setFireTicks(0);
 			player.setRemainingAir(300);
 			
 			// To hide stats
 			if (War.war.isSpoutServer()) {
 				War.war.getSpoutMessenger().updateStats(player);
 			}
 			
 			War.war.msg(player, "Your inventory is being restored.");
 			if (War.war.getWarHub() != null) {
 				War.war.getWarHub().resetZoneSign(this);
 			}
 
 			boolean zoneEmpty = true;
 			for (Team team : this.getTeams()) {
 				if (team.getPlayers().size() > 0) {
 					zoneEmpty = false;
 					break;
 				}
 			}
 			if (zoneEmpty && this.getWarzoneConfig().getBoolean(WarzoneConfig.RESETONEMPTY)) {
 				// reset the zone for a new game when the last player leaves
 				for (Team team : this.getTeams()) {
 					team.resetPoints();
 					team.setRemainingLives(team.getTeamConfig().resolveInt(TeamConfig.LIFEPOOL));
 				}
 				this.getVolume().resetBlocksAsJob();
 				this.initializeZoneAsJob();
 				War.war.log("Last player left warzone " + this.getName() + ". Warzone blocks resetting automatically...", Level.INFO);
 			}
 		}
 	}
 
 	public boolean isEnemyTeamFlagBlock(Team playerTeam, Block block) {
 		for (Team team : this.teams) {
 			if (!team.getName().equals(playerTeam.getName()) && team.isTeamFlagBlock(block)) {
 				return true;
 			}
 		}
 		return false;
 	}
 	
 	public boolean isFlagBlock(Block block) {
 		for (Team team : this.teams) {
 			if (team.isTeamFlagBlock(block)) {
 				return true;
 			}
 		}
 		return false;
 	}
 
 	public Team getTeamForFlagBlock(Block block) {
 		for (Team team : this.teams) {
 			if (team.isTeamFlagBlock(block)) {
 				return team;
 			}
 		}
 		return null;
 	}
 
 	public void addFlagThief(Team lostFlagTeam, String flagThief) {
 		this.flagThieves.put(flagThief, lostFlagTeam);
 	}
 
 	public boolean isFlagThief(String suspect) {
 		if (this.flagThieves.containsKey(suspect)) {
 			return true;
 		}
 		return false;
 	}
 
 	public Team getVictimTeamForThief(String thief) {
 		return this.flagThieves.get(thief);
 	}
 
 	public void removeThief(String thief) {
 		this.flagThieves.remove(thief);
 	}
 
 	public void clearFlagThieves() {
 		this.flagThieves.clear();
 	}
 
 	public boolean isTeamFlagStolen(Team team) {
 		for (String playerKey : this.flagThieves.keySet()) {
 			if (this.flagThieves.get(playerKey).getName().equals(team.getName())) {
 				return true;
 			}
 		}
 		return false;
 	}
 
 	public void handleScoreCapReached(Player player, String winnersStr) {
 		// Score cap reached. Reset everything.
 		ScoreCapReachedJob job = new ScoreCapReachedJob(this, winnersStr);
 		War.war.getServer().getScheduler().scheduleSyncDelayedTask(War.war, job);
 		this.getVolume().resetBlocksAsJob();
 		this.initializeZoneAsJob(player);
 		if (War.war.getWarHub() != null) {
 			// TODO: test if warhub sign gives the correct info despite the jobs
 			War.war.getWarHub().resetZoneSign(this);
 		}
 	}
 
 //	public void setSpawnStyle(TeamSpawnStyle spawnStyle) {
 //		this.spawnStyle = spawnStyle;
 //		for (Team team : this.teams) {
 //			team.setTeamSpawn(team.getTeamSpawn());
 //		}
 //	}
 
 	public boolean isDeadMan(String playerName) {
 		if (this.deadMenInventories.containsKey(playerName)) {
 			return true;
 		}
 		return false;
 	}
 
 	public void restoreDeadmanInventory(Player player) {
 		if (this.isDeadMan(player.getName())) {
 			this.playerInvFromInventoryStash(player.getInventory(), this.deadMenInventories.get(player.getName()));
 			this.deadMenInventories.remove(player.getName());
 		}
 
 	}
 
 	public void setRallyPoint(Location location) {
 		this.rallyPoint = location;
 	}
 
 	public Location getRallyPoint() {
 		return this.rallyPoint;
 	}
 
 	public void unload() {
 		War.war.log("Unloading zone " + this.getName() + "...", Level.INFO);
 		for (Team team : this.getTeams()) {
 			for (Player player : team.getPlayers()) {
 				this.handlePlayerLeave(player, this.getTeleport(), false);
 			}
 			team.getPlayers().clear();
 		}
 		if (this.getLobby() != null) {
 			this.getLobby().getVolume().resetBlocks();
 			this.getLobby().getVolume().finalize();
 		}
 		if (this.getWarzoneConfig().getBoolean(WarzoneConfig.RESETONUNLOAD)) {
 			this.getVolume().resetBlocks();
 		}
 		this.getVolume().finalize();
 	}
 
 	public boolean isEnoughPlayers() {
 		int teamsWithEnough = 0;
 		for (Team team : teams) {
 			if (team.getPlayers().size() >= this.getWarzoneConfig().getInt(WarzoneConfig.MINPLAYERS)) {
 				teamsWithEnough++;
 			}
 		}
 		if (teamsWithEnough >= this.getWarzoneConfig().getInt(WarzoneConfig.MINTEAMS)) {
 			return true;
 		}
 		return false;
 	}
 
 	public HashMap<String, LoadoutSelection> getLoadoutSelections() {
 		return loadoutSelections;
 	}
 
 	public boolean isAuthor(Player player) {
 		// if no authors, all zonemakers can edit the zone
 		return authors.size() == 0 || authors.contains(player.getName());
 	}
 		
 	public void addAuthor(String playerName) {
 		authors.add(playerName);
 	}
 	
 	public List<String> getAuthors() {
 		return this.authors;
 	}
 
 	public String getAuthorsString() {
 		String authors = "";
 		for (String author : this.getAuthors()) {
 			authors += author + ",";
 		}
 		return authors;
 	}
 
 	public void equipPlayerLoadoutSelection(Player player, Team playerTeam, boolean isFirstRespawn, boolean isToggle) {
 		LoadoutSelection selection = this.getLoadoutSelections().get(player.getName());
 		if (selection != null && !this.isRespawning(player)) {
 			HashMap<String, HashMap<Integer, ItemStack>> loadouts = playerTeam.getInventories().resolveLoadouts();
 			List<String> sortedNames = LoadoutYmlMapper.sortNames(loadouts);
 			
 			int currentIndex = selection.getSelectedIndex();
 			int i = 0;
 			Iterator<String> it = sortedNames.iterator();
 		    while (it.hasNext()) {
 		        String name = (String)it.next();
 		        if (i == currentIndex) {
 					this.resetInventory(playerTeam, player, loadouts.get(name));
 					if (isFirstRespawn && playerTeam.getInventories().resolveLoadouts().keySet().size() > 1) {
 						War.war.msg(player, "Equipped " + name + " loadout (sneak to switch).");
 					} else if (isToggle) {
 						War.war.msg(player, "Equipped " + name + " loadout.");
 					}
 		        }
 		        i++;
 		    }
 		}
 	}
 
 	public WarzoneConfigBag getWarzoneConfig() {
 		return this.warzoneConfig;
 	}
 	
 	public TeamConfigBag getTeamDefaultConfig() {
 		return this.teamDefaultConfig;
 	}
 
 	public InventoryBag getDefaultInventories() {
 		return this.defaultInventories ;
 	}
 }
