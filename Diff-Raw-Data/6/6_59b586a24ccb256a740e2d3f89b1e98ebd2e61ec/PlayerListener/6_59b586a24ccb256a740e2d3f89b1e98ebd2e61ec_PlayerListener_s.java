 package net.slipcor.pvparena.listeners;
 
 import java.util.Iterator;
 import java.util.List;
 
 import net.slipcor.pvparena.PVPArena;
 import net.slipcor.pvparena.core.Debug;
 import net.slipcor.pvparena.core.Language;
 import net.slipcor.pvparena.core.Update;
 import net.slipcor.pvparena.definitions.Arena;
 import net.slipcor.pvparena.definitions.ArenaBoard;
 import net.slipcor.pvparena.definitions.ArenaPlayer;
 import net.slipcor.pvparena.definitions.Powerup;
 import net.slipcor.pvparena.definitions.PowerupEffect;
 import net.slipcor.pvparena.managers.Arenas;
 import net.slipcor.pvparena.managers.Dominate;
 import net.slipcor.pvparena.managers.Flags;
 import net.slipcor.pvparena.managers.Players;
 import net.slipcor.pvparena.managers.Regions;
 import net.slipcor.pvparena.managers.Spawns;
 
 import org.bukkit.ChatColor;
 import org.bukkit.Location;
 import org.bukkit.Material;
 import org.bukkit.block.Block;
 import org.bukkit.block.Sign;
 import org.bukkit.entity.Player;
 import org.bukkit.event.EventHandler;
 import org.bukkit.event.EventPriority;
 import org.bukkit.event.Listener;
 import org.bukkit.event.block.Action;
 import org.bukkit.event.player.PlayerChatEvent;
 import org.bukkit.event.player.PlayerCommandPreprocessEvent;
 import org.bukkit.event.player.PlayerDropItemEvent;
 import org.bukkit.event.player.PlayerInteractEvent;
 import org.bukkit.event.player.PlayerJoinEvent;
 import org.bukkit.event.player.PlayerMoveEvent;
 import org.bukkit.event.player.PlayerPickupItemEvent;
 import org.bukkit.event.player.PlayerQuitEvent;
 import org.bukkit.event.player.PlayerRespawnEvent;
 import org.bukkit.event.player.PlayerTeleportEvent;
 import org.bukkit.event.player.PlayerVelocityEvent;
 
 /**
  * player listener class
  * 
  * -
  * 
  * PVP Arena Player Listener
  * 
  * @author slipcor
  * 
  * @version v0.6.30
  * 
  */
 
 public class PlayerListener implements Listener {
 	private Debug db = new Debug(21);
 
 	@EventHandler(priority = EventPriority.LOWEST)
 	public void onPlayerChat(PlayerChatEvent event) {
 
 		Player player = event.getPlayer();
 
 		Arena arena = Arenas.getArenaByPlayer(player);
 		if (arena == null || Players.getTeam(player).equals("")) {
 			return; // no fighting player => OUT
 		}
 		db.i("fighting player chatting!");
 
 		if (!arena.cfg.getBoolean("messages.chat")) {
 			return; // no chat editing
 		}
 
 		if (!arena.paChat.contains(player.getName())) {
 			return; // player not chatting
 		}
 		String sTeam = Players.getTeam(player);
 		Players.tellTeam(arena, sTeam, event.getMessage(),
 				ChatColor.valueOf(arena.paTeams.get(sTeam)), event.getPlayer());
 		event.setCancelled(true);
 	}
 
 	@EventHandler(priority = EventPriority.LOWEST)
 	public void onPlayerCommandPreprocess(PlayerCommandPreprocessEvent event) {
 		Player player = event.getPlayer();
 
 		Arena arena = Arenas.getArenaByPlayer(player);
 		if (arena == null) {
 			return; // no fighting player => OUT
 		}
 
 		List<String> list = PVPArena.instance.getConfig().getStringList(
 				"whitelist");
 		list.add("pa");
 		db.i("checking command whitelist");
 
 		for (String s : list) {
 			if (event.getMessage().startsWith("/" + s)) {
 				db.i("command allowed: " + s);
 				return;
 			}
 		}
 		db.i("command blocked: " + event.getMessage());
 		Arenas.tellPlayer(player, ChatColor.RED + event.getMessage());
 		event.setCancelled(true);
 	}
 
 	@EventHandler(priority = EventPriority.HIGHEST)
 	public void onPlayerDropItem(PlayerDropItemEvent event) {
 		Player player = event.getPlayer();
 
 		Arena arena = Arenas.getArenaByPlayer(player);
 		if (arena == null)
 			return; // no fighting player => OUT
 
 		db.i("onPlayerDropItem: fighting player");
 		Arenas.tellPlayer(player, (Language.parse("dropitem")));
 		event.setCancelled(true);
 		// cancel the drop event for fighting players, with message
 	}
 
 	@EventHandler(priority = EventPriority.NORMAL)
 	public void onPlayerInteract(PlayerInteractEvent event) {
 		Player player = event.getPlayer();
 
 		if (ArenaBoard.checkInteract(event, player)) {
 			return;
 		}
 
 		/*
 		 * //TODO
 		 * 
 		 * if (arena.cfg.getBoolean("arenatype.flags") &&
 		 * arena.cfg.getBoolean("join.inbattle")) {
 		 */
 
 		if (Regions.checkRegionSetPosition(event, player)) {
 			return;
 		}
 
 		if (Flags.checkSetFlag(event.getClickedBlock(), player)) {
 			return;
 		}
 
 		Arena arena = Arenas.getArenaByPlayer(player);
 		if (arena == null) {
 			Arenas.tryJoin(event, player);
 			return;
 		}
 
 		if (arena.cfg.getBoolean("arenatype.flags")) {
 			Flags.checkInteract(arena, player, event.getClickedBlock());
 		}
 
 		if (arena.fightInProgress && !arena.cfg.getBoolean("arenatype.flags")) {
 			db.i("exiting! fight in progress AND no flag arena!");
 			return; // no flag arena and fight already in progress => OUT
 		}

 		// fighting player inside the lobby!
 		event.setCancelled(true);
 
 		if (event.getAction().equals(Action.LEFT_CLICK_BLOCK)) {
 			Block block = event.getClickedBlock();
 			db.i("player team: " + Players.getTeam(player));
 			if (block.getState() instanceof Sign) {
 				db.i("sign click!");
 				Sign sign = (Sign) block.getState();
 
 				if ((arena.paClassItems.containsKey(sign.getLine(0)) || (sign
 						.getLine(0).equalsIgnoreCase("custom")))
 						&& (!Players.getTeam(player).equals(""))) {
 
 					Players.chooseClass(arena, player, sign, sign.getLine(0));
 				}
 				return;
 			}
 
 			db.i("block click!");
 
 			Material mMat = Material.IRON_BLOCK;
 			if (arena.cfg.get("ready.block") != null) {
 				db.i("reading ready block");
 				try {
 					mMat = Material
 							.getMaterial(arena.cfg.getInt("ready.block"));
 					if (mMat == Material.AIR)
 						mMat = Material.getMaterial(arena.cfg
 								.getString("ready.block"));
 					db.i("mMat now is " + mMat.name());
 				} catch (Exception e) {
 					db.i("exception reading ready block");
 					String sMat = arena.cfg.getString("ready.block");
 					try {
 						mMat = Material.getMaterial(sMat);
 						db.i("mMat now is " + mMat.name());
 					} catch (Exception e2) {
 						Language.log_warning("matnotfound", sMat);
 					}
 				}
 			}
 			db.i("clicked " + block.getType().name() + ", is it " + mMat.name()
 					+ "?");
 			if (block.getTypeId() == mMat.getId()) {
 				db.i("clicked ready block!");
 				if (Players.getTeam(player).equals("")) {
 					return; // not a fighting player => OUT
 				}
 				if (Players.getClass(player).equals("")) {
 					return; // not chosen class => OUT
 				}
 
 				if (!arena.fightInProgress) {
 
 					if (arena.cfg.getBoolean("join.forceEven", false)) {
 						if (!Players.checkEven(arena)) {
 							Arenas.tellPlayer(player,
 									Language.parse("waitequal"));
 							return; // even teams desired, not done => announce
 						}
 					}
 
 					if (!Regions.checkRegions(arena)) {
 						Arenas.tellPlayer(player,
 								Language.parse("checkregionerror"));
 						return;
 					}
 
 					arena.paReady.add(player.getName());
 
 					int ready = Players.ready(arena);
 					if (ready == 0) {
 						Arenas.tellPlayer(player, Language.parse("notready"));
 						return; // team not ready => announce
 					} else if (ready == -1) {
 						Arenas.tellPlayer(player, Language.parse("notready1"));
 						return; // team not ready => announce
 					} else if (ready == -2) {
 						Arenas.tellPlayer(player, Language.parse("notready2"));
 						return; // team not ready => announce
 					} else if (ready == -3) {
 						Arenas.tellPlayer(player, Language.parse("notready3"));
 						return; // team not ready => announce
 					} else if (ready == -4) {
 						Arenas.tellPlayer(player, Language.parse("notready4"));
 						return; // arena not ready => announce
 					} else if (ready == -5) {
 						Arenas.tellPlayer(player, Language.parse("notready5"));
 						return; // arena not ready => announce
 					} else if (ready == -6) {
 						arena.countDown();
 						return; // arena ready => countdown
 					}
 					arena.start();
 					return;
 				}
 
 				if (!arena.cfg.getBoolean("arenatype.randomSpawn", false)) {
 					arena.tpPlayerToCoordName(
 							player,
 							Players.getPlayerTeamMap(arena).get(
 									player.getName())
 									+ "spawn");
 				} else {
 					arena.tpPlayerToCoordName(player, "spawn");
 				}
 				arena.setPermissions(player);
 				arena.playerCount++;
 
 				arena.teamCount = arena.countActiveTeams();
 			}
 		}
 	}
 
 	@EventHandler(priority = EventPriority.LOWEST)
 	public void onPlayerJoin(PlayerJoinEvent event) {
 		Player player = event.getPlayer();
 
 		if (!player.isOp()) {
 			return; // no OP => OUT
 		}
 		db.i("OP joins the game");
 		Update.message(player);
 	}
 
 	@EventHandler(priority = EventPriority.NORMAL)
 	public void onPlayerMove(PlayerMoveEvent event) {
 		Player player = event.getPlayer();
 
 		Arena arena = Arenas.getArenaByPlayer(player);
 		if (arena == null) {
 			return; // no fighting player => OUT
 		}
 
 		if (arena.cfg.getBoolean("arenatype.domination")) {
 			Dominate.parseMove(arena, player);
 		}
 
 		// db.i("onPlayerMove: fighting player!");
 		if (arena.pum != null) {
 			Powerup p = arena.pum.puActive.get(player);
 			if (p != null) {
 				if (p.canBeTriggered()) {
 					if (p.isEffectActive(PowerupEffect.classes.FREEZE)) {
 						db.i("freeze in effect, cancelling!");
 						event.setCancelled(true);
 					}
 					if (p.isEffectActive(PowerupEffect.classes.SPRINT)) {
 						db.i("sprint in effect, sprinting!");
 						event.getPlayer().setSprinting(true);
 					}
 					if (p.isEffectActive(PowerupEffect.classes.SLIP)) {
 						//
 					}
 				}
 			}
 		}
 	}
 
 	@EventHandler(priority = EventPriority.HIGHEST)
 	public void onPlayerPickupItem(PlayerPickupItemEvent event) {
 		if (event.isCancelled())
 			return;
 
 		Player player = event.getPlayer();
 
 		Arena arena = Arenas.getArenaByPlayer(player);
 		if ((arena == null) || (arena.pum == null)
 				|| (arena.pum.puTotal.size() < 1))
 			return; // no fighting player or no powerups => OUT
 
 		db.i("onPlayerPickupItem: fighting player");
 		Iterator<Powerup> pi = arena.pum.puTotal.iterator();
 		while (pi.hasNext()) {
 			Powerup p = pi.next();
 			if (event.getItem().getItemStack().getType().equals(p.item)) {
 				Powerup newP = new Powerup(p);
 				if (arena.pum.puActive.containsKey(player)) {
 					arena.pum.puActive.get(player).disable();
 				}
 				arena.pum.puActive.put(player, newP);
 				Players.tellEveryone(arena, Language.parse("playerpowerup",
 						player.getName(), newP.name));
 				event.setCancelled(true);
 				event.getItem().remove();
 				if (newP.canBeTriggered())
 					newP.activate(player); // activate for the first time
 
 				return;
 			}
 		}
 	}
 
 	@EventHandler(priority = EventPriority.HIGHEST)
 	public void onPlayerQuit(PlayerQuitEvent event) {
 		Player player = event.getPlayer();
 		Arena arena = Arenas.getArenaByPlayer(player);
 		if (arena == null)
 			return; // no fighting player => OUT
 		Players.playerLeave(arena, player);
 	}
 
 	@EventHandler(priority = EventPriority.HIGHEST)
 	public void onPlayerRespawn(PlayerRespawnEvent event) {
 		Player player = event.getPlayer();
 
 		Arena arena = Arenas.getArenaByPlayer(player);
 		if (arena == null && !Players.isDead(player)) {
 			return; // no fighting player => OUT
 		}
 		db.i("onPlayerRespawn: fighting player");
 
 		if (Players.isDead(player)) {
 			db.i("respawning dead player");
 
 			event.setRespawnLocation(Players.getDeadLocation(arena, player));
 			Players.removeDeadPlayer(arena, player);
 			return;
 		}
 
 		if (Players.parsePlayer(player) != null) {
 			return; // no respawning player => OUT
 		}
 		db.i("respawning player");
 		Location l;
 
 		if (arena.cfg.getString("tp.death", "spectator").equals("old")) {
 			db.i("=> old location");
 			l = arena.getPlayerOldLocation(player);
 		} else {
 			db.i("=> 'config=>death' location");
 			l = Spawns.getCoords(arena,
 					arena.cfg.getString("tp.death", "spectator"));
 		}
 		event.setRespawnLocation(l);
 
 		arena.removePlayer(player, arena.cfg.getString("tp.death", "spectator"));
 		try {
 			ArenaPlayer p = Players.parsePlayer(player);
 			p.respawn = "";
 		} catch (Exception e) {
 
 		}
 	}
 
 	@EventHandler(priority = EventPriority.HIGHEST)
 	public void onPlayerTeleport(PlayerTeleportEvent event) {
 		Player player = event.getPlayer();
 		Arena arena = Arenas.getArenaByPlayer(player);
 		if (arena == null)
 			return; // no fighting player => OUT
 
 		db.i("onPlayerTeleport: fighting player (uncancel)");
 		event.setCancelled(false); // fighting player - first recon NOT to
 									// cancel!
 
 		if (arena.cfg.getBoolean("game.hideName")) {
 			player.setSneaking(true);
 			arena.colorizePlayer(player, null);
 		}
 
 		if (Players.getTelePass(player)
 				|| PVPArena.hasPerms(player, "pvparena.telepass"))
 			return; // if allowed => OUT
 
 		if (arena.regions.containsKey("battlefield")) {
 			if (arena.regions.get("battlefield").contains(event.getFrom())
 					&& arena.regions.get("battlefield").contains(event.getTo())) {
 				return; // teleporting inside the arena: allowed!
 			}
 		}
 
 		db.i("onPlayerTeleport: no tele pass, cancelling!");
 		event.setCancelled(true); // cancel and tell
 		Arenas.tellPlayer(player, Language.parse("usepatoexit"));
 	}
 
 	@EventHandler(priority = EventPriority.HIGHEST)
 	public void onPlayerVelocity(PlayerVelocityEvent event) {
 		if (event.isCancelled())
 			return;
 
 		Player player = event.getPlayer();
 
 		Arena arena = Arenas.getArenaByPlayer(player);
 		if (arena == null)
 			return; // no fighting player or no powerups => OUT
 
 		db.i("inPlayerVelocity: fighting player");
 		if (arena.pum != null) {
 			Powerup p = arena.pum.puActive.get(player);
 			if (p != null) {
 				if (p.canBeTriggered()) {
 					if (p.isEffectActive(PowerupEffect.classes.JUMP)) {
 						p.commit(event);
 					}
 				}
 			}
 		}
 	}
 
 }
