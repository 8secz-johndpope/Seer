 package mc.alk.arena.competition.match;
 
 import java.awt.Color;
 import java.util.ArrayList;
 import java.util.HashMap;
 import java.util.HashSet;
 import java.util.List;
 
 import mc.alk.arena.BattleArena;
 import mc.alk.arena.Defaults;
 import mc.alk.arena.controllers.ArenaClassController;
 import mc.alk.arena.controllers.WorldGuardController;
 import mc.alk.arena.events.matches.MatchClassSelectedEvent;
 import mc.alk.arena.events.matches.MatchPlayersReadyEvent;
 import mc.alk.arena.events.players.ArenaPlayerDeathEvent;
 import mc.alk.arena.events.players.ArenaPlayerKillEvent;
 import mc.alk.arena.events.players.ArenaPlayerReadyEvent;
 import mc.alk.arena.events.teams.TeamDeathEvent;
 import mc.alk.arena.objects.ArenaClass;
 import mc.alk.arena.objects.ArenaPlayer;
 import mc.alk.arena.objects.MatchParams;
 import mc.alk.arena.objects.MatchState;
 import mc.alk.arena.objects.PVPState;
 import mc.alk.arena.objects.arenas.Arena;
 import mc.alk.arena.objects.events.EventPriority;
 import mc.alk.arena.objects.events.MatchEventHandler;
 import mc.alk.arena.objects.options.TransitionOption;
 import mc.alk.arena.objects.options.TransitionOptions;
 import mc.alk.arena.objects.teams.ArenaTeam;
 import mc.alk.arena.util.CommandUtil;
 import mc.alk.arena.util.DmgDeathUtil;
 import mc.alk.arena.util.EffectUtil;
 import mc.alk.arena.util.InventoryUtil;
 import mc.alk.arena.util.Log;
 import mc.alk.arena.util.MessageUtil;
 import mc.alk.arena.util.PermissionsUtil;
 import mc.alk.arena.util.TeamUtil;
 
 import org.bukkit.Bukkit;
 import org.bukkit.ChatColor;
 import org.bukkit.Location;
 import org.bukkit.Material;
 import org.bukkit.World;
 import org.bukkit.block.Block;
 import org.bukkit.block.Sign;
 import org.bukkit.entity.Entity;
 import org.bukkit.entity.Player;
 import org.bukkit.event.block.Action;
 import org.bukkit.event.block.BlockBreakEvent;
 import org.bukkit.event.block.BlockPlaceEvent;
 import org.bukkit.event.entity.EntityDamageByEntityEvent;
 import org.bukkit.event.entity.EntityDamageEvent;
 import org.bukkit.event.entity.PlayerDeathEvent;
 import org.bukkit.event.player.PlayerCommandPreprocessEvent;
 import org.bukkit.event.player.PlayerInteractEvent;
 import org.bukkit.event.player.PlayerMoveEvent;
 import org.bukkit.event.player.PlayerQuitEvent;
 import org.bukkit.event.player.PlayerRespawnEvent;
 import org.bukkit.inventory.ItemStack;
 
 
 /**
  * TODO transfer most of this into their own listeners
  * like ItemDropListener
  */
 public class ArenaMatch extends Match {
 	static HashSet<String> disabledCommands = new HashSet<String>();
 
 	HashMap<String, Long> userTime = new HashMap<String, Long>();
 	HashMap<String, Integer> deathTimer = new HashMap<String, Integer>();
 
 	public ArenaMatch(Arena arena, MatchParams mp) {
 		super(arena, mp);
 	}
 
 	@MatchEventHandler(priority=EventPriority.HIGH)
 	public void onPlayerQuit(PlayerQuitEvent event){
 		/// If they are just in the arena waiting for match to start, or they havent joined yet
 		if (state == MatchState.ONCOMPLETE || state == MatchState.ONCANCEL ||
				!insideArena.contains(event.getPlayer().getName()) ){
 			return;}
 		ArenaPlayer player = BattleArena.toArenaPlayer(event.getPlayer());
 		ArenaTeam t = getTeam(player);
 		if (t==null){
 			return;}
 
 		PerformTransition.transition(this, MatchState.ONCANCEL, player, t, true);
 		checkAndHandleIfTeamDead(t);
 	}
 
 	@MatchEventHandler(suppressCastWarnings=true,priority=EventPriority.HIGH)
 	public void onPlayerDeath(PlayerDeathEvent event, final ArenaPlayer target){
 		if (state == MatchState.ONCANCEL || state == MatchState.ONCOMPLETE ||
 				!insideArena.contains(target.getName())){
 			return;}
 
 		final ArenaTeam t = getTeam(target);
 		if (t==null)
 			return;
 
 		ArenaPlayerDeathEvent apde = new ArenaPlayerDeathEvent(target,t);
 		apde.setPlayerDeathEvent(event);
 		callEvent(apde);
 		ArenaPlayer killer = DmgDeathUtil.getPlayerCause(event);
 		if (killer != null){
 			ArenaTeam killT = getTeam(killer);
 			if (killT != null){ /// they must be in the same match for this to count
 				killT.addKill(killer);
 				callEvent(new ArenaPlayerKillEvent(killer,killT,target));
 			}
 		}
 	}
 
 	@MatchEventHandler(priority=EventPriority.HIGH)
 	public void onPlayerDeath(ArenaPlayerDeathEvent event){
 		final ArenaPlayer target = event.getPlayer();
 		if (state == MatchState.ONCANCEL || state == MatchState.ONCOMPLETE ||
 				!insideArena.contains(target.getName())){
 			return;}
 		final ArenaTeam t = event.getTeam();
 
 		Integer nDeaths = t.addDeath(target);
 		boolean exiting = !respawns || (nDeaths != null && nDeaths >= nLivesPerPlayer);
 		boolean trueDeath = event.getPlayerDeathEvent() != null;
 
 		if (trueDeath){
 			PlayerDeathEvent pde = event.getPlayerDeathEvent();
 			if (cancelExpLoss)
 				pde.setKeepLevel(true);
 
 			/// Handle Drops from bukkitEvent
 			if (clearsInventoryOnDeath || keepsInventory){ /// clear the drops
 				try {pde.getDrops().clear();} catch (Exception e){}
 			} else if (woolTeams){  /// Get rid of the wool from teams so it doesnt drop
 				final int index = teams.indexOf(t);
 				ItemStack teamHead = TeamUtil.getTeamHead(index);
 				List<ItemStack> items = pde.getDrops();
 				for (ItemStack is : items){
 					if (is.getType() == teamHead.getType() && is.getDurability() == teamHead.getDurability()){
 						final int amt = is.getAmount();
 						if (amt > 1)
 							is.setAmount(amt-1);
 						else
 							is.setType(Material.AIR);
 						break;
 					}
 				}
 			}
 			/// If keepInventory is specified, but not restoreAll, then we have a case
 			/// where we need to give them back the current Inventory they have on them
 			/// even if they log out
 			if (keepsInventory){
 				boolean restores = getParams().getTransitionOptions().hasAnyOption(TransitionOption.RESTOREALL);
 				/// Restores and exiting, means clear their match inventory so they won't
 				/// get their match and their already stored inventory
 				if (restores && exiting){
 					psc.clearMatchItems(target);
 				} else { /// keep their current inv
 					psc.storeMatchItems(target);
 				}
 			}
 			/// We can't let them just sit on the respawn screen... schedule them to lose
 			/// We will cancel this onRespawn
 			final ArenaMatch am = this;
 			Integer timer = deathTimer.get(target.getName());
 			if (timer != null){
 				Bukkit.getScheduler().cancelTask(timer);
 			}
 			timer = Bukkit.getScheduler().scheduleSyncDelayedTask(BattleArena.getSelf(), new Runnable(){
 				@Override
 				public void run() {
 					PerformTransition.transition(am, MatchState.ONCOMPLETE, target, t, true);
 					if (t.isDead()){
 						callEvent(new TeamDeathEvent(t));}
 				}
 			}, 15*20L);
 			deathTimer.put(target.getName(), timer);
 		}
 
 		if (exiting){
 			PerformTransition.transition(this, MatchState.ONCOMPLETE, target, t, true);
 			checkAndHandleIfTeamDead(t);
 		}
 	}
 
 	@MatchEventHandler(suppressCastWarnings=true,priority=EventPriority.LOW)
 	public void onEntityDamageByEntity(EntityDamageEvent event) {
 		if (!(event.getEntity() instanceof Player))
 			return;
 		final TransitionOptions to = tops.getOptions(state);
 		if (to == null)
 			return;
 		final PVPState pvp = to.getPVP();
 		if (pvp == null)
 			return;
 		final ArenaPlayer target = BattleArena.toArenaPlayer((Player) event.getEntity());
 		if (pvp == PVPState.INVINCIBLE){
 			/// all damage is cancelled
 			target.setFireTicks(0);
 			event.setDamage(0);
 			event.setCancelled(true);
 			return;
 		}
 		if (!(event instanceof EntityDamageByEntityEvent)){
 			return;}
 
 		final Entity damagerEntity = ((EntityDamageByEntityEvent)event).getDamager();
 		ArenaPlayer damager=null;
 		switch(pvp){
 		case ON:
 			ArenaTeam targetTeam = getTeam(target);
 			if (targetTeam == null || !targetTeam.hasAliveMember(target)) /// We dont care about dead players
 				return;
 			damager = DmgDeathUtil.getPlayerCause(damagerEntity);
 			if (damager == null){ /// damage from some source, its not pvp though. so we dont care
 				return;}
 			ArenaTeam t = getTeam(damager);
 			if (t != null && t.hasMember(target)){ /// attacker is on the same team
 				event.setCancelled(true);
 			} else {/// different teams... lets make sure they can actually hit
 				event.setCancelled(false);
 			}
 			break;
 		case OFF:
 			damager = DmgDeathUtil.getPlayerCause(damagerEntity);
 			if (damager != null){ /// damage done from a player
 				event.setDamage(0);
 				event.setCancelled(true);
 			}
 			break;
 		default:
 			break;
 		}
 	}
 
 //	@MatchEventHandler(suppressCastWarnings=true,priority=EventPriority.HIGHER)
 //	public void onCheckEmulateDeath(EntityDamageEvent event) {
 //		//		Log.debug("############## checking emulate   " + event.getEntity() +"    " + event.isCancelled() +"    " + event.getDamage());
 //		if (event.isCancelled() || event.getDamage() <= 0 || !(event.getEntity() instanceof Player))
 //			return;
 //		Player target = ((Player) event.getEntity());
 //		//		Log.debug("############## checking health   " + event.getDamage() +"    " + target.getHealth());
 //		if (event.getDamage() < target.getHealth()){
 //			return;}
 //
 //		PlayerInventory pinv = target.getInventory();
 //		ArenaPlayer ap = BattleArena.toArenaPlayer(target);
 //		ArenaTeam targetTeam = getTeam(ap);
 //		if (clearsInventoryOnDeath){
 //			pinv.clear();
 //			if (woolTeams){
 //				if (targetTeam != null && targetTeam.getHeadItem() != null){
 //					TeamUtil.setTeamHead(targetTeam.getHeadItem(), target);
 //				}
 //			}
 //		}
 //
 //		Integer nDeaths = targetTeam.getNDeaths(ap);
 //		boolean exiting = !respawns || (nDeaths != null && nDeaths +1 >= nLivesPerPlayer);
 //
 //		ArenaPlayerDeathEvent apde = new ArenaPlayerDeathEvent(ap,targetTeam);
 //		callEvent(apde);
 //		ArenaPlayer killer = DmgDeathUtil.getPlayerCause(event);
 //		if (killer != null){
 //			ArenaTeam killT = getTeam(killer);
 //			if (killT != null){ /// they must be in the same match for this to count
 //				killT.addKill(killer);
 //				callEvent(new ArenaPlayerKillEvent(killer,killT,ap));
 //			}
 //		}
 //		PerformTransition.transition(this, MatchState.ONDEATH, ap, targetTeam , false);
 //		PerformTransition.transition(this, MatchState.ONDEATH, ap, targetTeam , false);
 //
 //		EffectUtil.deEnchantAll(target);
 //		target.closeInventory();
 //		target.setFireTicks(0);
 //		target.setHealth(target.getMaxHealth());
 //		if (!exiting){
 //			final int teamIndex = indexOf(targetTeam);
 //			final Location l = PerformTransition.jitter(getTeamSpawn(teamIndex,false),rand.nextInt(targetTeam.size()));
 //			TeleportController.teleportPlayer(target, l, false, true);
 //		}
 //	}
 
 	@MatchEventHandler(priority=EventPriority.HIGH)
 	public void onPlayerRespawn(PlayerRespawnEvent event, final ArenaPlayer p){
 		if (isWon()){
 			return;}
 		final TransitionOptions mo = tops.getOptions(MatchState.ONDEATH);
 		if (mo == null)
 			return;
 
 		if (respawns){
 			/// Lets cancel our death respawn timer
 			Integer timer = deathTimer.get(p.getName());
 			if (timer != null){
 				Bukkit.getScheduler().cancelTask(timer);}
 			Location loc = getTeamSpawn(getTeam(p), mo.randomRespawn());
 			event.setRespawnLocation(loc);
 			/// For some reason, the player from onPlayerRespawn Event isnt the one in the main thread, so we need to
 			/// resync before doing any effects
 			final Match am = this;
 			Bukkit.getScheduler().scheduleSyncDelayedTask(BattleArena.getSelf(), new Runnable() {
 				public void run() {
 					ArenaTeam t = getTeam(p);
 					try{
 						PerformTransition.transition(am, MatchState.ONDEATH, p, t , false);
 						PerformTransition.transition(am, MatchState.ONSPAWN, p, t, false);
 					} catch(Exception e){}
 					if (respawnsWithClass){
 						try{
 							if (p.getChosenClass() != null){
 								ArenaClass ac = p.getChosenClass();
 								ArenaClassController.giveClass(p.getPlayer(), ac);
 							}
 						} catch(Exception e){}
 					} else {
 						p.setChosenClass(null);
 					}
 					if (keepsInventory){
 						psc.restoreMatchItems(p);
 					}
 					if (woolTeams){
 						try{
 							TeamUtil.setTeamHead(teams.indexOf(t), p);
 						} catch(Exception e){}
 					}
 				}
 			});
 		} else { /// This player is now out of the system now that we have given the ondeath effects
 			Location l = mo.hasOption(TransitionOption.TELEPORTTO) ? mo.getTeleportToLoc() : oldlocs.get(p.getName());
 			if (l != null)
 				event.setRespawnLocation(l);
 			Bukkit.getScheduler().scheduleSyncDelayedTask(BattleArena.getSelf(), new Runnable() {
 				@Override
 				public void run() {
 					stopTracking(p);
 				}
 			});
 		}
 	}
 
 	@MatchEventHandler(priority=EventPriority.HIGH)
 	public void onPlayerBlockBreak(BlockBreakEvent event){
 		if (tops.hasOptionAt(state, TransitionOption.BLOCKBREAKOFF)){
 			event.setCancelled(true);}
 	}
 
 	@MatchEventHandler(priority=EventPriority.HIGH)
 	public void onPlayerBlockPlace(BlockPlaceEvent event){
 		if (tops.hasOptionAt(state, TransitionOption.BLOCKPLACEOFF)){
 			event.setCancelled(true);}
 	}
 
 	@MatchEventHandler(priority=EventPriority.HIGH)
 	public void onPlayerMove(PlayerMoveEvent event){
 		TransitionOptions to = tops.getOptions(state);
 		if (to==null)
 			return;
 		if (arena.hasRegion() && to.hasOption(TransitionOption.WGNOLEAVE) && WorldGuardController.hasWorldGuard()){
 			/// Did we actually even move
 			if (event.getFrom().getBlockX() != event.getTo().getBlockX()
 					|| event.getFrom().getBlockY() != event.getTo().getBlockY()
 					|| event.getFrom().getBlockZ() != event.getTo().getBlockZ()){
 				/// Now check world
 				World w = arena.getWorldGuardRegion().getWorld();
 				if (w==null || w.getUID() != event.getTo().getWorld().getUID())
 					return;
 				if (WorldGuardController.isLeavingArea(event.getFrom(), event.getTo(),arena.getWorldGuardRegion())){
 					event.setCancelled(true);}
 			}
 		}
 	}
 
 	/**
 	 * Factions has slashless commands that get handled and then set to cancelled....
 	 * so we need to act before them
 	 * @param event
 	 */
 	@MatchEventHandler(priority=EventPriority.HIGH, bukkitPriority=org.bukkit.event.EventPriority.LOWEST)
 	public void onPlayerCommandPreprocess1(PlayerCommandPreprocessEvent event){
 		handlePreprocess(event);
 	}
 
 	@MatchEventHandler(priority=EventPriority.HIGH)
 	public void onPlayerCommandPreprocess2(PlayerCommandPreprocessEvent event){
 		if (event.isCancelled() || state == MatchState.ONCOMPLETE || state == MatchState.ONCANCEL){
 			return;}
 		handlePreprocess(event);
 	}
 
 	private void handlePreprocess(PlayerCommandPreprocessEvent event) {
 		if (CommandUtil.shouldCancel(event, disabledCommands)){
 			event.setCancelled(true);
 			event.getPlayer().sendMessage(ChatColor.RED+"You cannot use that command when you are in a match");
 			if (PermissionsUtil.isAdmin(event.getPlayer())){
 				MessageUtil.sendMessage(event.getPlayer(),"&cYou can set &6/bad allowAdminCommands true: &c to change");}
 		}
 	}
 
 	@MatchEventHandler(priority=EventPriority.HIGH)
 	public void onPlayerInteract(PlayerInteractEvent event){
 		if (event.isCancelled())
 			return;
 		final Block b = event.getClickedBlock();
 		if (b == null) /// It's happened.. minecraft is a strange beast
 			return;
 		/// Check to see if it's a sign
 		final Material m = b.getType();
 		if (m.equals(Material.SIGN) || m.equals(Material.SIGN_POST)||m.equals(Material.WALL_SIGN)){ /// Only checking for signs
 			signClick(event);
 		} else if (m.equals(Defaults.READY_BLOCK)) {
 			readyClick(event);
 		}
 	}
 
 	private void readyClick(PlayerInteractEvent event) {
 		if (!Defaults.ENABLE_PLAYER_READY_BLOCK)
 			return;
 		if (!isInWaitRoomState()){
 			return;}
 		final Action action = event.getAction();
 		if (action == Action.LEFT_CLICK_BLOCK){ /// Dont let them break the block
 			event.setCancelled(true);}
 		final ArenaPlayer ap = BattleArena.toArenaPlayer(event.getPlayer());
 		if (readyPlayers != null && readyPlayers.contains(ap)) /// they are already ready
 			return;
 		setReady(ap);
 		MessageUtil.sendMessage(ap, "&2You ready yourself for the arena");
 		int size = getAlivePlayers().size();
 		if (size == readyPlayers.size()){
 			callEvent(new MatchPlayersReadyEvent(this));
 		}
 	}
 
 	private void signClick(PlayerInteractEvent event) {
 		/// Get our sign
 		final Sign sign = (Sign) event.getClickedBlock().getState();
 		/// Check to see if sign has correct format (is more efficient than doing string manipulation )
 		if (!sign.getLine(0).matches("^.[0-9a-fA-F]\\*.*$")){
 			return;}
 
 		final Action action = event.getAction();
 		if (action != Action.LEFT_CLICK_BLOCK && action != Action.RIGHT_CLICK_BLOCK){
 			return;}
 		if (action == Action.LEFT_CLICK_BLOCK){ /// Dont let them break the sign
 			event.setCancelled(true);}
 
 		final ArenaClass ac = ArenaClassController.getClass(MessageUtil.decolorChat(sign.getLine(0)).replace('*',' ').trim());
 		if (ac == null) /// Not a valid class sign
 			return;
 
 		final Player p = event.getPlayer();
 		if (!p.hasPermission("arena.class.use."+ac.getName().toLowerCase())){
 			MessageUtil.sendMessage(p, "&cYou don't have permissions to use the &6 "+ac.getName()+"&c class!");
 			return;
 		}
 
 		final ArenaPlayer ap = BattleArena.toArenaPlayer(p);
 		ArenaClass chosen = ap.getChosenClass();
 		if (chosen != null && chosen.getName().equals(ac.getName())){
 			MessageUtil.sendMessage(p, "&cYou already are a &6" + ac.getName());
 			return;
 		}
 		String playerName = p.getName();
 		if(userTime.containsKey(playerName)){
 			if((System.currentTimeMillis() - userTime.get(playerName)) < Defaults.TIME_BETWEEN_CLASS_CHANGE*1000){
 				MessageUtil.sendMessage(p, "&cYou must wait &6"+Defaults.TIME_BETWEEN_CLASS_CHANGE+"&c seconds between class selects");
 				return;
 			}
 		}
 
 		userTime.put(playerName, System.currentTimeMillis());
 
 		final TransitionOptions mo = tops.getOptions(state);
 		final TransitionOptions ro = tops.getOptions(MatchState.ONSPAWN);
 		if (mo == null && ro == null)
 			return;
 		/// Have They have already selected a class this match, have they changed their inventory since then?
 		/// If so, make sure they can't just select a class, drop the items, then choose another
 		if (chosen != null){
 			List<ItemStack> items = new ArrayList<ItemStack>();
 			if (chosen.getItems()!=null)
 				items.addAll(chosen.getItems());
 			if (ro != null && ro.hasItems()){
 				items.addAll(ro.getGiveItems());}
 			if (!InventoryUtil.sameItems(items, p.getInventory(), woolTeams)){
 				MessageUtil.sendMessage(p,"&cYou can't switch classes after changing items!");
 				return;
 			}
 		}
 		callEvent(new MatchClassSelectedEvent(this,ac));
 
 		/// Clear their inventory first, then give them the class and whatever items were due to them from the config
 		InventoryUtil.clearInventory(p, woolTeams);
 		/// Also debuff them
 		EffectUtil.deEnchantAll(p);
 		Color color = this.armorTeams ? TeamUtil.getTeamColor(getTeamIndex(getTeam(ap))) : null;
 		/// Regive class/items
 		ArenaClassController.giveClass(p, ac);
 		if (mo != null && mo.hasItems()){
 			try{ InventoryUtil.addItemsToInventory(p, mo.getGiveItems(), true,color);} catch(Exception e){Log.printStackTrace(e);}}
 		if (ro != null && ro.hasItems()){
 			try{ InventoryUtil.addItemsToInventory(p, ro.getGiveItems(), true,color);} catch(Exception e){Log.printStackTrace(e);}}
 
 		/// Deal with effects/buffs
 		if (mo != null && mo.getEffects()!=null){
 			EffectUtil.enchantPlayer(p, mo.getEffects());}
 		if (ro != null && ro.getEffects()!=null){
 			EffectUtil.enchantPlayer(p, ro.getEffects());}
 
 		ap.setChosenClass(ac);
 		MessageUtil.sendMessage(p, "&2You have chosen the &6"+ac.getName());
 	}
 
 
 
 	@MatchEventHandler
 	public void onPlayerReady(ArenaPlayerReadyEvent event){
 		if (!Defaults.ENABLE_PLAYER_READY_BLOCK){
 			return;}
 		int tcount = 0;
 		for (ArenaTeam t: teams){
 			if (!t.isReady())
 				return;
 			if (t.size() > 0)
 				tcount++;
 		}
 		if (tcount < neededTeams)
 			return;
 		this.start();
 	}
 
 	public static void setDisabledCommands(List<String> commands) {
 		for (String s: commands){
 			disabledCommands.add("/" + s.toLowerCase());}
 	}
 }
