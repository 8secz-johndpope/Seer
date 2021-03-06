 package org.mctourney.AutoReferee.listeners;
 
 import java.util.Iterator;
 import java.util.Map;
 
 import org.bukkit.ChatColor;
 import org.bukkit.GameMode;
 import org.bukkit.World;
 import org.bukkit.block.Sign;
 import org.bukkit.entity.EntityType;
 import org.bukkit.entity.Player;
 import org.bukkit.event.EventHandler;
 import org.bukkit.event.EventPriority;
 import org.bukkit.event.Listener;
 import org.bukkit.event.block.Action;
 import org.bukkit.event.entity.EntityDamageEvent;
 import org.bukkit.event.entity.PlayerDeathEvent;
 import org.bukkit.event.player.AsyncPlayerChatEvent;
 import org.bukkit.event.player.PlayerGameModeChangeEvent;
 import org.bukkit.event.player.PlayerInteractEvent;
 import org.bukkit.event.player.PlayerLoginEvent;
 import org.bukkit.event.player.PlayerQuitEvent;
 import org.bukkit.event.player.PlayerRespawnEvent;
 import org.bukkit.plugin.Plugin;
 
 import org.mctourney.AutoReferee.AutoRefMatch;
 import org.mctourney.AutoReferee.AutoRefPlayer;
 import org.mctourney.AutoReferee.AutoRefTeam;
 import org.mctourney.AutoReferee.AutoReferee;
 
 import com.google.common.collect.Maps;
 
 public class TeamListener implements Listener
 {
 	AutoReferee plugin = null;
 
 	// mapping spectators to the matches they died in
 	Map<String, AutoRefMatch> deadSpectators = Maps.newHashMap();
 
 	public TeamListener(Plugin p)
 	{ plugin = (AutoReferee) p; }
 
 	@EventHandler(priority=EventPriority.HIGHEST)
 	public void chatMessage(AsyncPlayerChatEvent event)
 	{
 		// typical chat message format, swap out with colored version
 		Player player = event.getPlayer();
 		AutoRefMatch match = plugin.getMatch(player.getWorld());
 
 		if (match == null) return;
 		event.setFormat("<" + match.getPlayerName(player) + "> " + event.getMessage());
 
 		// if we are currently playing and speaker on a team, restrict recipients
 		boolean speakerReferee = match.isReferee(player);
 		AutoRefTeam team = match.getPlayerTeam(player);
 
 		Iterator<Player> iter = event.getRecipients().iterator();
 		while (iter.hasNext())
 		{
 			Player recipient = iter.next();
 
 			// if the listener is in a different world
 			if (recipient.getWorld() != player.getWorld())
 			{ iter.remove(); continue; }
 
 			// if listener is on a team, and its not the same team as the
 			// speaker, remove them from the recipients list
 			if (AutoReferee.getInstance().isAutoMode() && !speakerReferee &&
 				match.getCurrentState().inProgress() && team != match.getPlayerTeam(recipient))
 					{ iter.remove(); continue; }
 		}
 	}
 
 	@EventHandler(priority=EventPriority.MONITOR)
 	public void spectatorDeath(PlayerDeathEvent event)
 	{
 		World world = event.getEntity().getWorld();
 		AutoRefMatch match = plugin.getMatch(world);
 
 		if (match != null && !match.isPlayer(event.getEntity()))
 		{
 			deadSpectators.put(event.getEntity().getName(), match);
 			event.getDrops().clear();
 		}
 	}
 
 	@EventHandler(priority=EventPriority.MONITOR)
 	public void spectatorDeath(EntityDamageEvent event)
 	{
 		World world = event.getEntity().getWorld();
 		AutoRefMatch match = plugin.getMatch(world);
 
 		if (event.getEntityType() != EntityType.PLAYER) return;
 		Player player = (Player) event.getEntity();
 
 		if (match != null && match.getCurrentState().inProgress() &&
 			!match.isPlayer(player))
 		{
 			event.setCancelled(true);
 			if (player.getLocation().getY() < -64)
 				player.teleport(match.getPlayerSpawn(player));
 			player.setFallDistance(0);
 		}
 	}
 
 	@EventHandler
 	public void playerRespawn(PlayerRespawnEvent event)
 	{
 		String name = event.getPlayer().getName();
 		if (deadSpectators.containsKey(name))
 		{
 			AutoRefMatch match = deadSpectators.get(name);
 			if (match != null) event.setRespawnLocation(match.getWorldSpawn());
 			deadSpectators.remove(name); return;
 		}
 
 		World world = event.getPlayer().getWorld();
 		AutoRefMatch match = plugin.getMatch(world);
 
 		if (match == null) for (AutoRefMatch m : plugin.getMatches())
 			if (m.isPlayer(event.getPlayer())) match = m;
 
 		if (match != null && match.isPlayer(event.getPlayer()))
 		{
 			// does this player have a bed spawn?
 			boolean hasBed = event.getPlayer().getBedSpawnLocation() != null;
 
 			// if the player attempts to respawn in a different world, bring them back
 			if (!hasBed || event.getRespawnLocation().getWorld() != match.getWorld())
 				event.setRespawnLocation(match.getPlayerSpawn(event.getPlayer()));
 
 			// setup respawn for the player
 			match.getPlayer(event.getPlayer()).respawn();
 		}
 	}
 
 	@EventHandler(priority=EventPriority.HIGHEST)
 	public void playerLogin(PlayerLoginEvent event)
 	{
 		Player player = event.getPlayer();
 		if (plugin.isAutoMode())
 		{
 			// if they should be whitelisted, let them in, otherwise, block them
 			if (plugin.playerWhitelisted(player)) event.allow();
 			else
 			{
 				event.disallow(PlayerLoginEvent.Result.KICK_WHITELIST, AutoReferee.NO_LOGIN_MESSAGE);
 				return;
 			}
 		}
 
 		// if this player needs to be in a specific world, put them there
 		AutoRefTeam team = plugin.getExpectedTeam(player);
 		AutoRefMatch match = plugin.getMatch(player.getWorld());
 
 		if (team != null) { team.join(player); match = team.getMatch(); }
		if (match != null && match.isPlayer(player))
			match.messageReferees("player", player.getName(), "login");
 	}
 
 	@EventHandler(priority=EventPriority.MONITOR)
 	public void playerQuit(PlayerQuitEvent event)
 	{
 		Player player = event.getPlayer();
 		AutoRefMatch match = plugin.getMatch(player.getWorld());
 		if (match == null) return;
 
 		// leave the team, if necessary
 		AutoRefTeam team = plugin.getTeam(player);
 		if (team != null) match.messageReferees("player", player.getName(), "logout");
 		if (team != null && !match.getCurrentState().inProgress()) team.leave(player);
 
 		AutoRefPlayer apl = match.getPlayer(player);
 		if (apl != null && player.getLocation() != null)
 			apl.setLastLogoutLocation(player.getLocation());
 
 		// if this player was damaged recently (during the match), notify
 		if (match.getCurrentState().inProgress() && apl != null && apl.wasDamagedRecently())
 		{
 			String message = apl.getName() + ChatColor.GRAY + " logged out during combat " +
 				String.format("with %2.1f hearts remaining", apl.getPlayer().getHealth() / 2.0);
 			for (Player ref : match.getReferees(true)) ref.sendMessage(message);
 		}
 	}
 
 	@EventHandler(priority=EventPriority.HIGHEST)
 	public void signCommand(PlayerInteractEvent event)
 	{
 		Player player = event.getPlayer();
 		AutoRefMatch match = plugin.getMatch(player.getWorld());
 
 		if (match != null && match.getCurrentState().isBeforeMatch() && event.hasBlock() &&
 			event.getClickedBlock().getState() instanceof Sign && match.inStartRegion(event.getClickedBlock().getLocation()))
 		{
 			String[] lines = ((Sign) event.getClickedBlock().getState()).getLines();
 			if (lines[0] == null || !"[AutoReferee]".equals(lines[0])) return;
 
 			// execute the command on the sign (and hope like hell that AutoReferee picks it up)
 			if (event.getAction() == Action.RIGHT_CLICK_BLOCK)
 				player.performCommand(ChatColor.stripColor(lines[1] + " " + lines[2]).trim());
 			event.setCancelled(true);
 		}
 	}
 
 	@EventHandler(priority=EventPriority.HIGHEST)
 	public void changeGamemode(PlayerGameModeChangeEvent event)
 	{
 		Player player = event.getPlayer();
 		AutoRefMatch match = plugin.getMatch(player.getWorld());
 
 		// if there is a match currently in progress on this world...
 		if (match != null && plugin.isAutoMode() &&
 			match.getCurrentState().inProgress())
 		{
 			// cancel the gamemode change if the player is a participant
 			if (event.getNewGameMode() == GameMode.CREATIVE &&
 				match.isPlayer(player)) event.setCancelled(true);
 		}
 	}
 }
