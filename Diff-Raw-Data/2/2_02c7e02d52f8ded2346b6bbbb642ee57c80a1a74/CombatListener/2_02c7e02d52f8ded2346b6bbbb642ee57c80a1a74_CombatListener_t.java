 package org.mctourney.AutoReferee.listeners;
 
 import java.util.Map;
 import java.util.UUID;
 
 import org.bukkit.block.Block;
 import org.bukkit.entity.Arrow;
 import org.bukkit.entity.EnderPearl;
 import org.bukkit.entity.Entity;
 import org.bukkit.entity.EntityType;
 import org.bukkit.entity.LivingEntity;
 import org.bukkit.entity.Player;
 import org.bukkit.entity.Projectile;
 import org.bukkit.event.EventHandler;
 import org.bukkit.event.EventPriority;
 import org.bukkit.event.Listener;
 import org.bukkit.event.entity.EntityDamageByEntityEvent;
 import org.bukkit.event.entity.EntityDamageEvent;
 import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
 import org.bukkit.event.entity.EntityDeathEvent;
 import org.bukkit.event.entity.EntityShootBowEvent;
 import org.bukkit.event.entity.ExplosionPrimeEvent;
 import org.bukkit.event.entity.FoodLevelChangeEvent;
 import org.bukkit.event.entity.PlayerDeathEvent;
 import org.bukkit.event.entity.EntityExplodeEvent;
 import org.bukkit.plugin.Plugin;
 import org.bukkit.Location;
 import org.bukkit.Material;
 
 import org.mctourney.AutoReferee.AutoRefMatch;
 import org.mctourney.AutoReferee.AutoRefPlayer;
 import org.mctourney.AutoReferee.AutoRefTeam;
 import org.mctourney.AutoReferee.AutoReferee;
 import org.mctourney.AutoReferee.util.AchievementPoints;
 
 import com.google.common.collect.Maps;
 
 public class CombatListener implements Listener
 {
 	private static final int TNT_PRIME_RANGE = 10;
 
 	AutoReferee plugin = null;
 
 	public Map<UUID, AutoRefPlayer> tntOwner;
 	public Map<Location, AutoRefPlayer> tntPropagation;
 
 	public CombatListener(Plugin p)
 	{
 		plugin = (AutoReferee) p;
 
 		tntOwner = Maps.newHashMap();
 		tntPropagation = Maps.newHashMap();
 	}
 
 	@EventHandler(priority=EventPriority.HIGHEST)
 	public void playerDeath(PlayerDeathEvent event)
 	{
 		AutoRefMatch match = plugin.getMatch(event.getEntity().getWorld());
 		if (match != null)
 		{
 			// get victim, and killer (maybe null) of this player
 			Player victim = (Player) event.getEntity();
 			AutoRefPlayer vapl = match.getPlayer(victim);
 
 			Player killer = victim.getKiller();
 			AutoRefPlayer kapl = match.getPlayer(killer);
 
 			String dmsg = event.getDeathMessage();
 			EntityDamageEvent lastDmg = victim.getLastDamageCause();
 
 			// if the death was due to intervention by the plugin
 			// let's change the death message to reflect this fact
 			if (lastDmg.getCause() == DamageCause.VOID)
 			{
 				dmsg = victim.getName() + " entered the void lane";
 				event.getDrops().clear();
 			}
 
 			if (lastDmg instanceof EntityDamageByEntityEvent)
 			{
 				EntityDamageByEntityEvent ed = (EntityDamageByEntityEvent) lastDmg;
 				switch (ed.getDamager().getType())
 				{
 					case CREEPER: dmsg = victim.getName() + " was blown up by Creeper"; break;
 					case PRIMED_TNT:
 						dmsg = victim.getName() + " was blown up by TNT";
 						if (tntOwner.get(ed.getDamager().getUniqueId()) != vapl)
 						{
 							kapl = tntOwner.get(ed.getDamager().getUniqueId());
							if (kapl != null) dmsg = victim.getName() + " was blown up by " + kapl.getName();
 						}
 					break;
 				}
 			}
 
 			// update the death message with the changes
 			event.setDeathMessage(dmsg);
 
 			if (match.getCurrentState().inProgress())
 			{
 				// register the death and kill
 				if (vapl != null) vapl.registerDeath(event);
 				if (kapl != null && kapl != vapl) kapl.registerKill(event);
 			}
 
 			// clear all active enderpearls for this player
 			if (AutoReferee.getInstance().isAutoMode())
 				for (Entity e : match.getWorld().getEntitiesByClass(EnderPearl.class))
 					if (((EnderPearl) e).getShooter() == victim) e.remove();
 
 			// handle respawn modes
 			if (vapl != null)
 			{
 				respawn: switch (match.getRespawnMode())
 				{
 					case BEDSONLY:
 						// INTENTIONAL FALL-THROUGH HERE!
 						for (AutoRefPlayer mate : vapl.getTeam().getPlayers()) if (mate != vapl)
 						{
 							boolean couldRespawn = mate.isOnline() &&
 								mate.getPlayer().getBedSpawnLocation() != null;
 							if (!mate.isDead() || couldRespawn) break respawn;
 						}
 						if (victim.getBedSpawnLocation() != null) break respawn;
 
 					case DISALLOW:
 						if (match.getCurrentState().inProgress())
 							match.eliminatePlayer((Player) event.getEntity());
 						break;
 
 					// typically, no action should be taken
 					default: break;
 				}
 			}
 		}
 		else for (Player pl : event.getEntity().getWorld().getPlayers())
 			pl.sendMessage(event.getDeathMessage());
 
 		// remove the death message (so we can control who receives it)
 		event.setDeathMessage(null);
 	}
 
 	public static Player entityToPlayer(Entity e)
 	{
 		// damaging entity is an actual player, easy!
 		if ((e instanceof Player)) return (Player) e;
 
 		// damaging entity is an arrow, then who was bow?
 		if ((e instanceof Projectile))
 		{
 			LivingEntity shooter = ((Projectile) e).getShooter();
 			if ((shooter instanceof Player)) return (Player) shooter;
 		}
 		return null;
 	}
 
 	@EventHandler(priority=EventPriority.HIGHEST)
 	public void damageDealt(EntityDamageEvent event)
 	{
 		AutoRefMatch match = plugin.getMatch(event.getEntity().getWorld());
 		if (match == null || !match.getCurrentState().inProgress()) return;
 
 		if ((event instanceof EntityDamageByEntityEvent))
 		{
 			EntityDamageByEntityEvent ed = (EntityDamageByEntityEvent) event;
 			Player damaged = entityToPlayer(ed.getEntity());
 
 			// enderpearls are a special case!
 			if (ed.getDamager().getType() == EntityType.ENDER_PEARL) return;
 
 			Player damager = entityToPlayer(ed.getDamager());
 			if (null != damager && ed.getDamager() instanceof Arrow)
 			{
 				AutoRefPlayer apl = match.getPlayer(damager);
 				if (apl != null) apl.incrementShotsHit();
 			}
 
 			// spectators cannot cause damage to any entity
 			if (match.getCurrentState().inProgress() &&
 				null != damager && match.isSpectator(damager))
 			{ event.setCancelled(true); return; }
 
 			// if either of these aren't players, nothing to do here
 			if (null == damager || null == damaged) return;
 
 			// if the damaged entity was a player
 			if (null != damaged)
 			{
 				AutoRefPlayer apl = match.getPlayer(damaged);
 
 				// if the match is in progress and player is in start region
 				// cancel any damage dealt to the player
 				if (match.getCurrentState().inProgress() && apl != null && !apl.isActive())
 				{ event.setCancelled(true); return; }
 			}
 
 			// get team affiliations of these players (maybe null)
 			AutoRefTeam d1team = plugin.getTeam(damager);
 			AutoRefTeam d2team = plugin.getTeam(damaged);
 			if (d1team == null && d2team == null) return;
 
 			// if the attacked isn't on a team, or same team (w/ no FF), cancel
 			if (d2team == null || (d1team == d2team && !match.allowFriendlyFire()))
 			{ event.setCancelled(true); return; }
 		}
 	}
 
 	@EventHandler(priority=EventPriority.MONITOR, ignoreCancelled=true)
 	public void damageDealtMonitor(EntityDamageEvent event)
 	{
 		AutoRefMatch match = plugin.getMatch(event.getEntity().getWorld());
 		if (match == null || !match.getCurrentState().inProgress()) return;
 
 		// save player data if the damaged entity was a player
 		if (event.getEntityType() == EntityType.PLAYER)
 		{
 			AutoRefPlayer pdata = match.getPlayer((Player) event.getEntity());
 			if (pdata != null)
 			{
 				Player damager = (event instanceof EntityDamageByEntityEvent) ?
 					entityToPlayer(((EntityDamageByEntityEvent) event).getDamager()) : null;
 				pdata.registerDamage(event, damager);
 			}
 		}
 	}
 
 	@EventHandler(priority=EventPriority.MONITOR)
 	public void entityDeath(EntityDeathEvent event)
 	{
 		AutoRefMatch match = plugin.getMatch(event.getEntity().getWorld());
 		if (match == null || !match.getCurrentState().inProgress()) return;
 
 		EntityDamageEvent cause = event.getEntity().getLastDamageCause();
 		if (cause instanceof EntityDamageByEntityEvent)
 		{
 			Player dmgr = entityToPlayer(((EntityDamageByEntityEvent) cause).getDamager());
 			AchievementPoints ach = AchievementPoints.getMonsterKill(event.getEntityType());
 			if (match.isPlayer(dmgr)) match.getPlayer(dmgr).addPoints(ach);
 		}
 	}
 
 	@EventHandler
 	public void playerBowFire(EntityShootBowEvent event)
 	{
 		// if the entity is not a player, we don't care
 		if (event.getEntityType() != EntityType.PLAYER) return;
 
 		Player player = (Player) event.getEntity();
 		AutoRefMatch match = plugin.getMatch(player.getWorld());
 		if (match == null || !match.getCurrentState().inProgress()) return;
 
 		AutoRefPlayer apl = match.getPlayer(player);
 		if (apl != null) apl.incrementShotsFired();
 	}
 
 	@EventHandler(priority=EventPriority.HIGHEST)
 	public void hungerChange(FoodLevelChangeEvent event)
 	{
 		AutoRefMatch match = plugin.getMatch(event.getEntity().getWorld());
 		if (match != null && !match.getCurrentState().inProgress())
 			event.setFoodLevel(20);
 	}
 
 	@EventHandler(priority=EventPriority.MONITOR)
 	public void explosionPrime(ExplosionPrimeEvent event)
 	{
 		AutoRefMatch match = plugin.getMatch(event.getEntity().getWorld());
 		if (!AutoReferee.hasSportBukkitApi() || match == null) return;
 
 		if (event.getEntityType() == EntityType.PRIMED_TNT)
 		{
 			Location tntLocation = event.getEntity().getLocation().getBlock().getLocation();
 			AutoRefPlayer apl = tntPropagation.remove(tntLocation);
 
 			// if there was no propagation chain
 			if (apl == null)
 			{
 				// try to determine if this was the first tnt in a chain
 				if ((apl = match.getNearestPlayer(tntLocation)) == null) return;
 
 				Location plLocation = apl.getLocation();
 				if (plLocation.distanceSquared(tntLocation) > TNT_PRIME_RANGE * TNT_PRIME_RANGE) return;
 			}
 
 			// add an owner for this tnt object
 			if (apl != null) tntOwner.put(event.getEntity().getUniqueId(), apl);
 		}
 	}
 
 	@EventHandler(priority=EventPriority.HIGHEST)
 	public void entityExplode(EntityExplodeEvent event)
 	{
 		// remove this entity from the table if present
 		AutoRefPlayer apl = tntOwner.remove(event.getEntity().getUniqueId());
 
 		if (apl != null) for (Block b : event.blockList())
 			if (b.getType() == Material.TNT) tntPropagation.put(b.getLocation(), apl);
 	}
 }
