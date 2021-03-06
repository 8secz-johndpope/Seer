 package com.untamedears.mustercull;
 
 import java.util.ArrayList;
 import java.util.Collections;
 import java.util.HashSet;
 import java.util.List;
 import java.util.Set;
 import java.util.Stack;
 
 import org.bukkit.World;
 import org.bukkit.entity.Ageable;
 import org.bukkit.entity.Entity;
 import org.bukkit.entity.EntityType;
 import org.bukkit.entity.LivingEntity;
 import org.bukkit.entity.Player;
 import org.bukkit.plugin.java.JavaPlugin;
 
 /**
  * This is the main class for the MusterCull Bukkit plug-in.
  * @author Celdecea
  *
  */
 public class MusterCull extends JavaPlugin {
 
 	/**
 	 * Holds a list of entities to monitor.
 	 */
 	private Stack<EntityLimitPair> knownEntities = new Stack<EntityLimitPair>();
 	
 	/**
 	 * Holds a count of entities remaining for the status checker
 	 */
 	private int knownEntitiesRemaining = 0;
 	
 	/**
 	 * Flags whether or not we should clear knownEntities list next time around
 	 */
 	private boolean clearKnownEntities = false;
 	
 	/**
 	 * Whether or not we are returning a new entity to process (concurrency protection) 
 	 */
 	private boolean returningKnownEntity = false;
 	
 	/**
 	 * Buffer for keeping track of the parallel Laborer task.
 	 */
 	private int laborTask = -1;
 	
 	/**
 	 * Buffer for holding configuration information for this plug-in.
 	 */
 	private Configuration config = null;
 	
 	/**
 	 * Stores any paused culling types we may have.
 	 */
 	private Set<CullType> pausedCullTypes = new HashSet<CullType>();
 	
 	/**
 	 * Called when the plug-in is enabled by Bukkit.
 	 */
 	public void onEnable() {
 		
 		this.config = new Configuration(this);
 		this.config.load();
         
 		if (this.config.hasDamageLimits()) {
 			
 			this.laborTask = getServer().getScheduler().scheduleSyncRepeatingTask(this, new Laborer(this), this.config.getTicksBetweenDamage(), this.config.getTicksBetweenDamage());
 
 			if (this.laborTask == -1) {
 				getLogger().severe("Failed to start MusterCull laborer.");
 			}	
 		}
 		
 		if (this.config.hasSpawnLimits() || this.config.hasDamageLimits()) {
 			getServer().getPluginManager().registerEvents(new EntityListener(this), this);
 		}
 		else {
 			getLogger().info("MusterCull doesn't appear to have anything to do.");
 		}
 		
 		Commander commander = new Commander(this);
 		
 		for (String command : getDescription().getCommands().keySet()) {
 			getCommand(command).setExecutor(commander);
 		}
     }
      
 	/**
 	 * Called when the plug-in is disabled by Bukkit.
 	 */
     public void onDisable() { 
     	if (this.laborTask != -1) {
     		getServer().getScheduler().cancelTask(this.laborTask);
     	}
     	
     	this.config.save();
     }
 
     
     
     
     
     
 
     /**
      * Return a limit from the config file for the provided entityType.
      * @param entityType A Bukkit entityType to return a limit for.
      * @return The ConfigurationLimit for the entityType, or null if none is defined.
      */
     public ConfigurationLimit getLimit(EntityType entityType, CullType cullType) {
     	
     	List<ConfigurationLimit> limits = this.config.getLimits(entityType);
     	
     	if (limits == null) {
     		return null;
     	}
     	
     	for (ConfigurationLimit limit : limits) {
     		if (cullType == limit.getCulling()) {
     			return limit;
     		}
     	}
     	
     	return null;
     }
 
 
 	/**
 	 * Sets the ConfigurationLimit for the specified mob type. Don't add 
 	 * limits you don't need.
 	 * 
 	 * @param type The type of entity to set a ConfigurationLimit for.
 	 * @param limit The limit for the entity type.
 	 */
 	public void setLimit(EntityType type, ConfigurationLimit limit) {
 		this.config.setLimit(type, limit);
 	}
 	
     
     
 	
 	/**
 	 * Returns whether or not we have limits with CullType DAMAGE.
 	 * @return Whether or not we have limits with CullType DAMAGE.
 	 */
 	public boolean hasDamageLimits() {
 		return this.config.hasDamageLimits();
 	}
 	
 	
 	/**
 	 * Returns whether or not we have limits with CullType SPAWN.
 	 * @return Whether or not we have limits with CullType SPAWN.
 	 */
 	public boolean hasSpawnLimits() {
 		return this.config.hasSpawnLimits();
 	}
 	
 	
 	
 	
 	/**
 	 * Returns the amount of damage to apply to a crowded mob.
 	 * @return The amount of damage to apply to a crowded mob. 
 	 */
 	public int getDamage() {
 		return this.config.getDamage();
 	}
 	
 	
 	
 	/**
 	 * Returns the next entity for monitoring.
 	 * @return A reference to an EntityLimitPair.
 	 */
 	public EntityLimitPair getNextEntity() {
 		
 		synchronized(this.knownEntities) {
 			if (this.returningKnownEntity) {
 				return null;
 			}
 			
 			this.returningKnownEntity = true;
 		}
 		
 		EntityLimitPair entityLimitPair = null;
 		
 		boolean clearEntities = false;
 		
 		synchronized(this) {
 			clearEntities = this.clearKnownEntities;
 			this.clearKnownEntities = false;
 		}
 		
 		if (this.knownEntitiesRemaining <= 0 || clearEntities) {
 			
 			if (clearEntities) {
 				getLogger().info("Forcing entity list to clear...");
 			}
 			
 			this.knownEntitiesRemaining = 0;
 			this.knownEntities.clear();
 			
 			for (World world : getServer().getWorlds()) {
 				for (Entity entity : world.getEntities()) {
 					ConfigurationLimit limit = this.getLimit(entity.getType(), CullType.DAMAGE);
 					
 					if (limit != null) {
 						this.knownEntities.push(new EntityLimitPair(entity, limit));
 						this.knownEntitiesRemaining++;
 					}
 				}
 			}
 			
 			getLogger().info("Grabbed " + this.knownEntitiesRemaining + " entities this round.");
 		}
 		else {
 			entityLimitPair = this.knownEntities.pop();
 			this.knownEntitiesRemaining--;
 		}
 	
 		synchronized(this.knownEntities) {
 			this.returningKnownEntity = false;
 			return entityLimitPair;
 		}
 	}
 	
 	
 	
 	/**
 	 * Returns information about mobs surrounding players
 	 * @return Information about mobs surrounding players
 	 */
 	public List<StatusItem> getStats() {
 		
 		List<StatusItem> stats = new ArrayList<StatusItem>();
 		
 		for (World world : getServer().getWorlds()) {
 			for (Player player : world.getPlayers()) {
 				stats.add(new StatusItem(player));
 			}
 		}
 		
 		Collections.sort(stats, new StatusItemComparator());
 		Collections.reverse(stats);
 		return stats;
 	}
 	
 	
 	
 	
 	
 	
 	
 	
 	/**
 	 * Returns the percent chance that a mob will be damaged when crowded.
 	 * @return Percent chance that a mob will be damaged when crowded.
 	 */
 	public int getDamageChance() {
 		return this.config.getDamageChance();
 	}
 	
 	
 	
 	
 	
 	
 
 	/**
 	 * Returns the number of entities left to check for damage in this round.
 	 * @return The size of the stack of Bukkit entities left to check.
 	 */
 	public int getRemainingDamageEntities() {
 		return this.knownEntitiesRemaining;
 	}
 	
 	/**
 	 * Clears the entities that may be waiting for damage.
 	 */
 	public void clearRemainingDamageEntities() {
 		getLogger().info("Flagging damage list for clearing...");
 		synchronized(this) {
 			this.clearKnownEntities = true;
 		}
 	}
 
 
 
 	/**
 	 * Returns nearby entities to a player by name.
 	 * @param playerName The name of a player to look up
	 * @param rangeX Distance along the x plane to look from player
	 * @param rangeY Distance along the y plane to look from player
	 * @param rangeZ Distance along the z plane to look from player
 	 * @return The list of entities surrounding the player
 	 */
 	public List<Entity> getNearbyEntities(String playerName, int rangeX, int rangeY, int rangeZ) {
 		
 		for (World world : getServer().getWorlds()) {
 			for (Player player : world.getPlayers()) {
 				if (0 == player.getName().compareToIgnoreCase(playerName)) {
 					return player.getNearbyEntities(rangeX, rangeY, rangeZ);
 				}
 			}
 		}
 		
 		return null;
 	}
 
 	
 	/**
 	 * Causes damage to entities of a certain type surrounding a given player.
 	 * @param playerName The name of the player to search around
 	 * @param entityType The type of entity to damage around the player
 	 * @param damage The amount of damage to deal to the player
 	 * @param range The range from the player to check
 	 * @return The number of entities damage may have been applied to
 	 */
 	public int damageEntitiesAroundPlayer(String playerName, EntityType entityType, int damage, int range) {
 	
 		int count = 0;
 		
 		List<Entity> nearbyEntities = getNearbyEntities(playerName, range, range, range);
 		
 		if (nearbyEntities == null) {
 			return 0;
 		}		
 		
 		for (Entity entity : nearbyEntities) {
 			if (entity.getType() == entityType) {
 				this.damageEntity(entity, damage);
 				count++;
 			}
 		}
 		
 		return count;
 	}
 	
 	
 	
 	/**
 	 * Causes a specified amount of damage to an entity.
 	 * @param entity The bukkit entity to cause damage to
 	 * @param damage The amount of damage to cause to the entity
 	 */
 	public void damageEntity(Entity entity, int damage) {
 		
 		if (Ageable.class.isAssignableFrom(entity.getClass())) {
 			Ageable agingEntity = (Ageable)entity;
 			
 			if (agingEntity.isAdult()) {
 				agingEntity.damage(damage);
 			}
 			else {
 				agingEntity.damage(2 * damage);
 			}
 		}
 		else if (LivingEntity.class.isAssignableFrom(entity.getClass())) {
 			LivingEntity livingEntity = (LivingEntity)entity;
 			livingEntity.damage(damage);
 		}
 		else {
 			getLogger().warning("Attempt to damage non-living entity detected.");
 		}
 		
 	}
 	
 	
 	
 	/**
 	 * Performs configured culling operations on the given entity.
 	 * @param entity The bukkit entity to perform culling operations for.
 	 * @param limit The limit to run for this entity.
 	 * @return Whether the entity check was successful (i.e. we need to damage/kill something)
 	 */
 	public boolean runEntityChecks(Entity entity, ConfigurationLimit limit) {
 			
 		// If the limit is 0, prevent all of this entity type from spawning 
 		if (limit.getLimit() <= 0) {
 			return true;
 		}
 		
 		// Loop through entities in range and count similar entities.
 		int count = 0;
 		
 		for (Entity otherEntity : entity.getNearbyEntities(limit.getRange(), limit.getRange(), limit.getRange())) {
 			if (0 == otherEntity.getType().compareTo(entity.getType())) {
 				count += 1;
 				
 				// If we've reached a limit for this entity, prevent it from spawning.
 				if (count >= limit.getLimit()) {
 					return true;
 				}
 			}
 		}
 		
 		return false;
 	}
 	
 	
 	
 
 	/**
 	 * Pauses all culling.
 	 */
 	public void pauseAllCulling() {
 		getLogger().info("Pausing all culling types...");
 		synchronized(this.pausedCullTypes) {
 			for (CullType cullType : CullType.values()) {
 				this.pausedCullTypes.add(cullType);
 			}
 		}
 	}
 	
 	/**
 	 * Resumes all culling.
 	 */
 	public void resumeAllCulling() {
 		getLogger().info("Resuming all paused culling types...");
 		synchronized(this.pausedCullTypes) {
 			this.pausedCullTypes.clear();
 		}
 	}
 	
 	/**
 	 * Pauses a specific CullType blocking its functionality.
 	 * @param cullType The CullType to disable temporarily.
 	 */
 	public void pauseCulling(CullType cullType) {
 		getLogger().info("Pausing culling type " + cullType.toString() + "...");
 		synchronized(this.pausedCullTypes) {
 			this.pausedCullTypes.add(cullType);
 		}
 	}
 	
 	/**
 	 * Resumes a specific CullType which was paused.
 	 * @param cullType The CullType to reenable.
 	 */
 	public void resumeCulling(CullType cullType) {
 		getLogger().info("Resuming culling type " + cullType.toString() + "...");
 		synchronized(this.pausedCullTypes) {
 			this.pausedCullTypes.remove(cullType);
 		}
 	}
 	
 	/**
 	 * Returns whether or not a CullType is paused.
 	 * @param cullType The CullType to test the status of.
 	 * @return Whether or not the CullType is paused.
 	 */
 	public boolean isPaused(CullType cullType) {
 		synchronized(this.pausedCullTypes) {
 			return this.pausedCullTypes.contains(cullType);
 		}
 	}
 
 }
