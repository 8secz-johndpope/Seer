 package com.untamedears.PrisonPearl;
 
 import java.io.File;
 import java.io.FileNotFoundException;
 import java.io.IOException;
 import java.lang.reflect.Method;
 import java.util.Map.Entry;
import java.util.concurrent.Callable;
 
 import org.bukkit.Bukkit;
 import org.bukkit.Location;
 import org.bukkit.Material;
 import org.bukkit.World;
 import org.bukkit.command.Command;
 import org.bukkit.command.CommandExecutor;
 import org.bukkit.command.CommandSender;
 import org.bukkit.entity.Player;
 import org.bukkit.event.EventHandler;
 import org.bukkit.event.EventPriority;
 import org.bukkit.event.Listener;
 import org.bukkit.event.player.PlayerJoinEvent;
 import org.bukkit.event.player.PlayerRespawnEvent;
 import org.bukkit.inventory.Inventory;
 import org.bukkit.inventory.ItemStack;
 import org.bukkit.plugin.java.JavaPlugin;
 
 public class PrisonPearlPlugin extends JavaPlugin implements Listener, CommandExecutor {
 	@SuppressWarnings("unused")
 	private PearlTagManager tagman;
 	private PrisonPearlManager pearlman;
 	private PrisonPearlStorage pearls;
 	private SummonManager summonman;
 
 	public void onEnable() {
 		getConfig().options().copyDefaults(true);
 		saveConfig();
 		
 		pearls = new PrisonPearlStorage();
 		
 		File ppfile = getPrisonPearlsFile();
 		try {
 			pearls.load(ppfile);
 		} catch (FileNotFoundException e) {
 			System.out.println("Prison pearls data file does not exist, creating.");
 
 			try {
 				pearls.save(ppfile);
 			} catch (IOException e2) {
 				throw new RuntimeException("Failed to create " + ppfile.getAbsolutePath(), e2);
 			}
 		} catch (IOException e) {
 			throw new RuntimeException("Failed to load prison pearls from " + ppfile.getAbsolutePath(), e);
 		}
 		
 		tagman = new PearlTagManager(this);
 		pearlman = new PrisonPearlManager(this, pearls);
 		summonman = new SummonManager(this, pearls);
 		
 		File summonfile = getSummonFile();
 		try {
 			summonman.load(summonfile);
 		} catch (FileNotFoundException e) {
 			System.out.println("Prison pearls data file does not exist, creating.");
 
 			try {
 				pearls.save(summonfile);
 			} catch (IOException e2) {
 				throw new RuntimeException("Failed to create " + ppfile.getAbsolutePath(), e2);
 			}
 		} catch (IOException e) {
 			throw new RuntimeException("Failed to load prison pearls from " + ppfile.getAbsolutePath(), e);
 		}
 		
 		getServer().getPluginManager().registerEvents(this, this);
 		getCommand("pplocate").setExecutor(this);
 		getCommand("pplocateany").setExecutor(this);
 		getCommand("ppfree").setExecutor(this);
 		getCommand("ppfreeany").setExecutor(this);
 		getCommand("ppsummon").setExecutor(this);
 		getCommand("ppreturn").setExecutor(this);
 		getCommand("ppkill").setExecutor(this);
 
 		// shamelessly swiped from bookworm, not sure why there isn't a Bukkit API for this
 		// this causes items to be stacked by their durability value
 		try {
 			Method method = net.minecraft.server.Item.class.getDeclaredMethod("a", boolean.class);
 			if (method.getReturnType() == net.minecraft.server.Item.class) {
 				method.setAccessible(true);
 				method.invoke(net.minecraft.server.Item.ENDER_PEARL, true);
 			}
 		} catch (Exception e) {
 			e.printStackTrace();
 		}
 	}
 
 	public void onDisable() {
 		try {
 			File file = getPrisonPearlsFile();
 			if (file.exists())
 				file.renameTo(new File(file.getAbsolutePath() + ".bak"));
 			pearls.save(file);
 		} catch (IOException e) {
 			throw new RuntimeException("Failed to save prison pearls to " + getPrisonPearlsFile().getAbsolutePath(), e);
 		}
 		
 		try {
 			File file = getSummonFile();
 			if (file.exists())
 				file.renameTo(new File(file.getAbsolutePath() + ".bak"));
 			summonman.save(file);
 		} catch (IOException e) {
 			throw new RuntimeException("Failed to save prison pearls to " + getPrisonPearlsFile().getAbsolutePath(), e);
 		}
 	}
 
 	private File getPrisonPearlsFile() {
 		return new File(getDataFolder(), "prisonpearls.txt");
 	}
 	
 	private File getSummonFile() {
 		return new File(getDataFolder(), "summons.txt");
 	}
 	
 	@EventHandler(priority=EventPriority.HIGHEST)
 	public void onPlayerJoin(PlayerJoinEvent event) {
		final Player player = event.getPlayer();
 		if (player.isDead())
 			return;
 		
		final Location newloc = playerSpawn(player, player.getLocation());
 		if (newloc != null) {
			// under very specific situations it seems like teleport directly in onPlayerJoin causes duplicate entities
			Bukkit.getScheduler().callSyncMethod(this, new Callable<Void>() {
				public Void call() {
					player.teleport(newloc);
					return null;
				}
			});
			
 		}
 	}
 	
 	@EventHandler(priority=EventPriority.HIGHEST)
 	public void onPlayerRespawn(PlayerRespawnEvent event) {
 		Location newloc = playerSpawn(event.getPlayer(), event.getRespawnLocation());
 		if (newloc != null)
 			event.setRespawnLocation(newloc);
 	}
 	
 	private Location playerSpawn(Player player, Location spawnloc) {
 		World respawn = Bukkit.getWorld(getConfig().getString("respawn_world"));
 		World prison = Bukkit.getWorld(getConfig().getString("prison_world"));
 		Location newloc=null;
 		
 		System.out.println("playerSpawn");
 		if (pearls.isImprisoned(player)) { // if player is imprisoned
 			System.out.println("isImprisoned");
 			if (!summonman.isSummoned(player)) { // and not summoned
 				System.out.println("!isSummoned");
 				if (spawnloc.getWorld() != prison) // make sure he spawns in the prison world
 					newloc = prison.getSpawnLocation();
 				
 				for (String line : getConfig().getStringList("prison_motd")) // give him prison_motd
 					player.sendMessage(line);
 			}
 		} else if (spawnloc.getWorld() == prison) { // not imprisoned, but spawning in prison?
 			newloc = respawn.getSpawnLocation(); // he was free'd while offline, change location to spawn
 			player.sendMessage("You've been freed!");
 		}		
 		
 		return newloc;
 	}	
 
 	// Announce pearl tag events
 	@EventHandler(priority=EventPriority.MONITOR)
 	public void onPearlTagEvent(PearlTagEvent event) {
 		Player tagger = event.getTag().getTaggerPlayer();
 		Player tagged = event.getTag().getTaggedPlayer();
 		Player other = event.getOtherPlayer();
 
 		switch (event.getType()) {
 		case NEW:
 			tagger.sendMessage("You've tagged " + tagged.getDisplayName() + " for imprisonment");
 			tagged.sendMessage("You've been tagged for imprisonment by " + tagger.getDisplayName());
 			break;
 			
 		case EXPIRED:
 			tagger.sendMessage("Your tag expired for " + tagged.getDisplayName());
 			tagged.sendMessage("You are no longer tagged by " + tagger.getDisplayName());
 			break;
 			
 		case SWITCHED:
 			tagger.sendMessage("Your tag for " + tagged.getDisplayName() + " is now owned by " + other.getDisplayName());
 			break;
 			
 		case KILLED:
 			if (pearlman.imprisonPlayer(tagged, tagger)) {
 				tagger.sendMessage("You've bound " + tagged.getDisplayName() + " to a prison pearl!");
 				tagged.sendMessage("You've been bound to a prison pearl owned by " + tagger.getDisplayName());
 			} else {
 				tagger.sendMessage("You failed to imprison " + tagger.getDisplayName());
 			}
 			break;
 		}
 	}
 
 	// Announce prison pearl events
 	@EventHandler(priority=EventPriority.MONITOR)
 	public void onPrisonPearlEvent(PrisonPearlEvent event) {
 		PrisonPearl pp = event.getPrisonPearl();
 		Player player = pp.getImprisonedPlayer();
 		if (player == null || player.isDead()) // player not online or dead?
 			return; // gets no intel then
 		
 		switch (event.getType()) {
 		case HELD:
 		case DROPPED:
 			player.sendMessage("Your prison pearl is " + pp.describeLocation());
 			break;
 			
 		case FREED:
 			player.sendMessage("You've been freed!");
 			break;
 		}
 	}
 	
 	// Announce summon events
 	@EventHandler(priority=EventPriority.MONITOR)
 	public void onSummonEvent(SummonEvent event) {
 		PrisonPearl pp = event.getPrisonPearl();
 		Player player = pp.getImprisonedPlayer();
 		if (player == null)
 			return;
 
 		switch (event.getType()) {
 		case SUMMONED:
 			player.sendMessage("You've been summoned to your prison pearl!");
 			break;
 			
 		case RETURNED:
 			player.sendMessage("You've been returned to your prison");
 			break;
 			
 		case KILLED:
 			player.sendMessage("You've been struck down by your pearl!");
 			break;
 		}
 	}
 	
 	@Override
 	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
 		if (label.equalsIgnoreCase("pplocate")) {
 			return locateCmd(sender, args, false);
 		} else if (label.equalsIgnoreCase("pplocateany")) {
 			return locateCmd(sender, args, true);
 		} else if (label.equalsIgnoreCase("ppfree")) {
 			return freeCmd(sender, args, false);
 		} else if (label.equalsIgnoreCase("ppfreeany")) {
 			return freeCmd(sender, args, true);
 		} else if (label.equalsIgnoreCase("ppsummon")) {
 			return summonCmd(sender, args);
 		} else if (label.equalsIgnoreCase("ppreturn")) {
 			return returnCmd(sender, args);
 		} else if (label.equalsIgnoreCase("ppkill")) {
 			return killCmd(sender, args);
 		}
 
 		return false;
 	}
 	
 	private boolean locateCmd(CommandSender sender, String args[], boolean any) {
 		String name_is;
 		String name_possesive;
 		PrisonPearl pp;
 		
 		if (!any) {
 			if (args.length != 0)
 				return false;
 			
 			if (!(sender instanceof Player)) {
 				sender.sendMessage("Must use pplocateany at the console");
 				return true;
 			}
 				
 			name_is = "You are";
 			name_possesive = "Your";
 			pp = pearls.getByImprisoned((Player)sender);
 		} else {
 			if (args.length != 1)
 				return false;
 			
 			name_is = args[0] + " is";
 			name_possesive = args[0] + "'s";
 			pp = pearls.getByImprisoned(args[0]);
 		}
 		
 		if (pp != null) {
 			sender.sendMessage(name_possesive + " prison pearl is " + pp.describeLocation());
 		} else {
 			sender.sendMessage(name_is + " not imprisoned");
 		}
 		
 		return true;
 	}
 	
 	private boolean freeCmd(CommandSender sender, String args[], boolean any) {
 		PrisonPearl pp;
 		Location loc = null;
 		
 		if (!any) {
 			if (args.length > 1)
 				return false;
 			
 			if (!(sender instanceof Player)) {
 				sender.sendMessage("Must use freeany at console");
 				return true;
 			}
 			
 			Player player = (Player)sender;
 			loc = player.getLocation();
 			
 			int slot = getCommandPearlSlot(player, args);
 			if (slot == -1)
 				return true;
 			
 			pp = pearls.getByItemStack(player.getInventory().getItem(slot));
 			player.getInventory().setItem(slot, null);		
 		} else {
 			if (args.length != 1)
 				return false;
 			
 			pp = pearls.getByImprisoned(args[0]);
 			
 			if (pp == null) {
 				sender.sendMessage(args[0] + " is not imprisoned");
 				return true;
 			}
 		}
 		
 		if (pp.getImprisonedPlayer() != sender)
 			sender.sendMessage("You've freed " + pp.getImprisonedName());
 		
 		pearlman.freePearl(pp, loc);	
 		return true;
 	}
 	
 	private boolean summonCmd(CommandSender sender, String args[]) {
 		if (args.length > 1)
 			return false;
 		
 		if (!(sender instanceof Player)) {
 			sender.sendMessage("Command cannot be used at console");
 			return true;
 		}
 		
 		Player player = (Player)sender;
 		PrisonPearl pp = getCommandPearl(player, args);
 		if (pp == null)
 			return true;
 	
 		if (pp.getImprisonedPlayer() == null || pp.getImprisonedPlayer().isDead()) {
 			sender.sendMessage(pp.getImprisonedName() + " cannot be summoned");
 			return true;
 		} else if (pp.getImprisonedPlayer() == player) {
 			sender.sendMessage("You cannot summon yourself!");
 			return true;
 		} else if (summonman.isSummoned(pp)) {
 			sender.sendMessage(pp.getImprisonedName() + " is already summoned");
 			return true;
 		}
 			
 		sender.sendMessage("You've summoned " + pp.getImprisonedName());
 		summonman.summonPearl(pp, player.getLocation());
 		return true;		
 	}
 	
 	private boolean returnCmd(CommandSender sender, String args[]) {
 		if (args.length > 1)
 			return false;
 		
 		if (!(sender instanceof Player)) {
 			sender.sendMessage("Command cannot be used at console");
 			return true;
 		}
 		
 		Player player = (Player)sender;
 		PrisonPearl pp = getCommandPearl(player, args); 
 		if (pp == null)
 			return true;
 		
		if (pp.getImprisonedName().equals(player.getName())) {
 			sender.sendMessage("You cannot return yourself!");
 			return true;
 		} else if (!summonman.isSummoned(pp)) {
 			sender.sendMessage(pp.getImprisonedName() + " has not been summoned!");
 			return true;
 		}
 			
 		if (summonman.returnPearl(pp))
 			sender.sendMessage("You've returned " + pp.getImprisonedName());
 		else
 			sender.sendMessage("You failed to return " + pp.getImprisonedName());
 		return true;		
 	}
 	
 	private boolean killCmd(CommandSender sender, String args[]) {
 		if (args.length > 1)
 			return false;
 		
 		if (!(sender instanceof Player)) {
 			sender.sendMessage("Command cannot be used at console");
 			return true;
 		}
 		
 		Player player = (Player)sender;
 		PrisonPearl pp = getCommandPearl(player, args);
 		if (pp == null)
 			return true;
 	
 		if (!summonman.isSummoned(pp)) {
 			sender.sendMessage(pp.getImprisonedName() + " has not been summoned!");
 			return true;
 		}
 		
 		if (summonman.killPearl(pp))
 			sender.sendMessage("You've killed " + pp.getImprisonedName());
 		else
 			sender.sendMessage("You failed to kill " + pp.getImprisonedName());
 		return true;		
 	}
 	
 	private PrisonPearl getCommandPearl(Player player, String args[]) {
 		int slot = getCommandPearlSlot(player, args);
 		if (slot != -1)
 			return pearls.getByItemStack(player.getInventory().getItem(slot));
 		else
 			return null;
 	}
 	
 	private int getCommandPearlSlot(Player player, String args[]) {
 		if (args.length == 0) {
 			ItemStack item = player.getItemInHand();
 			if (item.getType() != Material.ENDER_PEARL) {
 				player.sendMessage("You must hold a pearl or supply the player's name to use this command");
 				return -1;
 			}
 			
 			if (pearls.getByItemStack(item) == null) {
 				player.sendMessage("This is an ordinary ender pearl");
 				return -1;
 			}
 			
 			return player.getInventory().getHeldItemSlot();
 		} else {		
 			PrisonPearl pp = pearls.getByImprisoned(args[0]);
 			if (pp != null) {
 				Inventory inv = player.getInventory();
 				for (Entry<Integer, ? extends ItemStack> entry : inv.all(Material.ENDER_PEARL).entrySet()) {
 					if (entry.getValue().getDurability() == pp.getID())
 						return entry.getKey();
 				}
 			}
 			
 			player.sendMessage("You don't possess " + args[0] + "'s prison pearl");
 			return -1;
 		}
 	}
 }
