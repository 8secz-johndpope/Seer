 package net.aufdemrand.denizen.scripts.containers.core;
 
 import net.aufdemrand.denizen.Settings;
 import net.aufdemrand.denizen.objects.*;
 import net.aufdemrand.denizen.objects.aH.Argument;
 import net.aufdemrand.denizen.scripts.ScriptBuilder;
 import net.aufdemrand.denizen.scripts.ScriptEntry;
 import net.aufdemrand.denizen.scripts.commands.core.DetermineCommand;
 import net.aufdemrand.denizen.scripts.queues.ScriptQueue;
 import net.aufdemrand.denizen.scripts.queues.core.InstantQueue;
 import net.aufdemrand.denizen.tags.TagManager;
 import net.aufdemrand.denizen.utilities.Conversion;
 import net.aufdemrand.denizen.utilities.DenizenAPI;
 import net.aufdemrand.denizen.utilities.debugging.dB;
 import net.aufdemrand.denizen.utilities.entity.Position;
 import net.citizensnpcs.api.CitizensAPI;
 
 import org.bukkit.Bukkit;
 import org.bukkit.Material;
 import org.bukkit.World;
 import org.bukkit.block.Block;
 import org.bukkit.block.Sign;
 import org.bukkit.entity.*;
 import org.bukkit.event.EventHandler;
 import org.bukkit.event.EventPriority;
 import org.bukkit.event.Listener;
 import org.bukkit.event.block.Action;
 import org.bukkit.event.block.BlockBreakEvent;
 import org.bukkit.event.block.BlockBurnEvent;
 import org.bukkit.event.block.BlockCanBuildEvent;
 import org.bukkit.event.block.BlockDamageEvent;
 import org.bukkit.event.block.BlockFadeEvent;
 import org.bukkit.event.block.BlockFromToEvent;
 import org.bukkit.event.block.BlockIgniteEvent;
 import org.bukkit.event.block.BlockPhysicsEvent;
 import org.bukkit.event.block.BlockPlaceEvent;
 import org.bukkit.event.block.BlockRedstoneEvent;
 import org.bukkit.event.block.SignChangeEvent;
 import org.bukkit.event.entity.*;
 import org.bukkit.event.hanging.HangingBreakEvent;
 import org.bukkit.event.hanging.HangingBreakByEntityEvent;
 import org.bukkit.event.inventory.InventoryClickEvent;
 import org.bukkit.event.inventory.InventoryCloseEvent;
 import org.bukkit.event.inventory.InventoryDragEvent;
 import org.bukkit.event.inventory.InventoryOpenEvent;
 import org.bukkit.event.player.AsyncPlayerChatEvent;
 import org.bukkit.event.player.PlayerBedEnterEvent;
 import org.bukkit.event.player.PlayerBedLeaveEvent;
 import org.bukkit.event.player.PlayerBucketEmptyEvent;
 import org.bukkit.event.player.PlayerBucketFillEvent;
 import org.bukkit.event.player.PlayerChangedWorldEvent;
 import org.bukkit.event.player.PlayerChatEvent;
 import org.bukkit.event.player.PlayerCommandPreprocessEvent;
 import org.bukkit.event.player.PlayerFishEvent;
 import org.bukkit.event.player.PlayerGameModeChangeEvent;
 import org.bukkit.event.player.PlayerInteractEntityEvent;
 import org.bukkit.event.player.PlayerInteractEvent;
 import org.bukkit.event.player.PlayerItemConsumeEvent;
 import org.bukkit.event.player.PlayerJoinEvent;
 import org.bukkit.event.player.PlayerLevelChangeEvent;
 import org.bukkit.event.player.PlayerLoginEvent;
 import org.bukkit.event.player.PlayerMoveEvent;
 import org.bukkit.event.player.PlayerPickupItemEvent;
 import org.bukkit.event.player.PlayerQuitEvent;
 import org.bukkit.event.player.PlayerRespawnEvent;
 import org.bukkit.event.server.ServerCommandEvent;
 import org.bukkit.event.vehicle.VehicleDamageEvent;
 import org.bukkit.event.vehicle.VehicleDestroyEvent;
 import org.bukkit.event.vehicle.VehicleEnterEvent;
 import org.bukkit.event.vehicle.VehicleExitEvent;
 import org.bukkit.event.weather.LightningStrikeEvent;
 import org.bukkit.event.weather.WeatherChangeEvent;
 import org.bukkit.inventory.ItemStack;
 import org.bukkit.util.BlockIterator;
 
 import java.util.ArrayList;
 import java.util.Arrays;
 import java.util.HashMap;
 import java.util.List;
 import java.util.Map;
 import java.util.concurrent.Callable;
 import java.util.concurrent.ConcurrentHashMap;
 import java.util.concurrent.ExecutionException;
 
 @SuppressWarnings("deprecation")
 public class WorldScriptHelper implements Listener {
 
     public static Map<String, WorldScriptContainer> world_scripts = new ConcurrentHashMap<String, WorldScriptContainer>(8, 0.9f, 1);
 
     public WorldScriptHelper() {
         DenizenAPI.getCurrentInstance().getServer().getPluginManager()
                 .registerEvents(this, DenizenAPI.getCurrentInstance());
     }
 
 
     /////////////////////
     //   EVENT HANDLER
     /////////////////
 
     public static String doEvents(List<String> eventNames, dNPC npc, Player player, Map<String, dObject> context) {
 
         String determination = "none";
 
         if (dB.showEventsFiring) dB.log("Fired for '" + eventNames.toString() + "'");
 
         for (WorldScriptContainer script : world_scripts.values()) {
 
             if (script == null) continue;
 
             for (String eventName : eventNames) {
 
                 if (!script.contains("EVENTS.ON " + eventName.toUpperCase())) continue;
 
                 // Fetch script from Event
                 //
                 // Note: a "new dPlayer(null)" will not be null itself,
                 //       so keep a ternary operator here
                 List<ScriptEntry> entries = script.getEntries
                         (player != null ? new dPlayer(player) : null,
                                 npc, "events.on " + eventName);
                 if (entries.isEmpty()) continue;
 
                 dB.report("Event",
                         aH.debugObj("Type", "On " + eventName)
                                 + script.getAsScriptArg().debug()
                                 + (npc != null ? aH.debugObj("NPC", npc.toString()) : "")
                                 + (player != null ? aH.debugObj("Player", player.getName()) : "")
                                 + (context != null ? aH.debugObj("Context", context.toString()) : ""));
 
                 dB.echoDebug(dB.DebugElement.Header, "Building event 'On " + eventName.toUpperCase() + "' for " + script.getName());
 
                 // Create new ID -- this is what we will look for when determining an outcome
                 long id = DetermineCommand.getNewId();
 
                 // Add the reqId to each of the entries for the determine command (this may be slightly outdated, add to TODO)
                 ScriptBuilder.addObjectToEntries(entries, "ReqId", id);
 
                 // Add entries and context to the queue
                 ScriptQueue queue = InstantQueue.getQueue(null).addEntries(entries);
 
                 if (context != null) {
                     for (Map.Entry<String, dObject> entry : context.entrySet()) {
                         queue.addContext(entry.getKey(), entry.getValue());
                     }
                 }
 
                 // Start the queue!
                 queue.start();
 
                 // Check the determination
                 if (DetermineCommand.hasOutcome(id))
                     determination =  DetermineCommand.getOutcome(id);
             }
         }
 
         return determination;
     }
 
 
     /////////////////////
     //   BLOCK EVENTS
     /////////////////
 
     // <--[event]
     // @Events
     // player breaks block
     // player breaks <material>
     // player breaks block with <item>
     // player breaks <material> with <item>
     //
     // @Triggers when a player breaks a block.
     // @Context
     // <context.location> will return the location the block was broken at.
     // <context.material> will return the material of the block that was broken.
     //
     // @Determine
     // "CANCELLED" to stop the block from breaking.
     // "NOTHING" to make the block drop no items.
     // dList(dItem) to make the block drop a specified list of items.
     //
     // -->
     @EventHandler
     public void blockBreak(BlockBreakEvent event) {
 
         Map<String, dObject> context = new HashMap<String, dObject>();
         Block block = event.getBlock();
         dMaterial material = new dMaterial(event.getBlock().getType());
 
         context.put("location", new dLocation(block.getLocation()));
         context.put("material", material);
 
         dItem item = new dItem(event.getPlayer().getItemInHand());
 
         List<String> events = new ArrayList<String>();
         events.add("player breaks block");
         events.add("player breaks " + material.name());
         events.add("player breaks block with " + item.identify());
         events.add("player breaks " + material.name() + " with " + item.identify());
 
         if (!item.identify().equals(item.identify().split(":")[0])) {
             events.add("player breaks block with " +
                     item.identify().split(":")[0]);
             events.add("player breaks " + material.name() + " with " +
                     item.identify().split(":")[0]);
         }
         if (item.isItemscript()) {
             events.add("player breaks block with itemscript "
                     + item.getMaterial());
             events.add("player breaks " + material.name() + " with itemscript "
                     + item.getMaterial());
         }
 
         String determination = doEvents(events, null, event.getPlayer(), context);
 
         if (determination.toUpperCase().startsWith("CANCELLED"))
             event.setCancelled(true);
 
             // Make nothing drop, usually used as "drop:nothing"
         else if (determination.toUpperCase().startsWith("NOTHING")) {
             event.setCancelled(true);
             block.setType(Material.AIR);
         }
 
         // Get a dList of dItems to drop
         else if (Argument.valueOf(determination).matchesArgumentList(dItem.class)) {
 
             // Cancel the event
             event.setCancelled(true);
             block.setType(Material.AIR);
 
             // Get the list of items
             Object list = dList.valueOf(determination).filter(dItem.class);
 
             @SuppressWarnings("unchecked")
             List<dItem> newItems = (List<dItem>) list;
 
             for (dItem newItem : newItems) {
 
                 block.getWorld().dropItemNaturally(block.getLocation(),
                         newItem.getItemStack()); // Drop each item
             }
         }
     }
 
     // <--[event]
     // @Events
     // block burns
     // <block> burns
     //
     // @Triggers when a block is destroyed by fire.
     // @Context
     // <context.location> will return the location the block was burned at.
     // <context.material> will return the material of the block that was burned.
     //
     // @Determine
     // "CANCELLED" to stop the block from being destroyed.
     //
     // -->
     @EventHandler
     public void blockBurn(BlockBurnEvent event) {
 
         Map<String, dObject> context = new HashMap<String, dObject>();
 
         context.put("location", new dLocation(event.getBlock().getLocation()));
         dMaterial material = new dMaterial(event.getBlock().getType());
 
         String determination = doEvents(Arrays.asList
                 ("block burns",
                         material.name() + " burns"),
                 null, null, context);
 
         if (determination.toUpperCase().startsWith("CANCELLED"))
             event.setCancelled(true);
     }
 
     // <--[event]
     // @Events
     // block being built
     // block being built on <material>
     // <material> being built
     // <material> being built on <material>
     //
     // @Triggers when an attempt is made to build a block on another block. Not necessarily caused by players.
     // @Context
     // <context.location> will return the location of the block the player is trying to build on.
     // <context.old_material> will return the material of the block the player is trying to build on.
     // <context.new_material> will return the material of the block the player is trying to build.
     //
     // @Determine
     // "BUILDABLE" to allow the building.
     // "CANCELLED" to cancel the building.
     //
     // -->
     @EventHandler
     public void blockCanBuild(BlockCanBuildEvent event) {
 
         Map<String, dObject> context = new HashMap<String, dObject>();
         dMaterial oldMaterial = new dMaterial(event.getBlock().getType());
         dMaterial newMaterial = new dMaterial(event.getMaterial());
 
         context.put("location", new dLocation(event.getBlock().getLocation()));
         context.put("old_material", oldMaterial);
         context.put("new_material", newMaterial);
 
         String determination = doEvents(Arrays.asList
                 ("block being built",
                         "block being built on " + oldMaterial.identify(),
                         newMaterial.name() + " being built",
                         newMaterial.name() + " being built on " +
                                 oldMaterial.name()),
                 null, null, context);
 
         if (determination.toUpperCase().startsWith("BUILDABLE"))
             event.setBuildable(true);
 
         if (determination.toUpperCase().startsWith("CANCELLED"))
             event.setBuildable(false);
     }
 
     // <--[event]
     // @Events
     // player damages block
     // player damages <material>
     //
     // @Triggers when a block is damaged by a player.
     // @Context
     // <context.location> will return the location the block that was damaged.
     // <context.material> will return the material of the block that was damaged.
     //
     // @Determine
     // "CANCELLED" to stop the block from being damaged.
     // "INSTABREAK" to make the block get broken instantly.
     //
     // -->
     @EventHandler
     public void blockDamage(BlockDamageEvent event) {
 
         Map<String, dObject> context = new HashMap<String, dObject>();
         dMaterial material = new dMaterial(event.getBlock().getType());
 
         context.put("location", new dLocation(event.getBlock().getLocation()));
         context.put("material", material);
 
         String determination = doEvents(Arrays.asList
                 ("player damages block",
                         "player damages " + material.name()),
                 null, event.getPlayer(), context);
 
         if (determination.toUpperCase().startsWith("CANCELLED"))
             event.setCancelled(true);
 
         if (determination.toUpperCase().startsWith("INSTABREAK"))
             event.setInstaBreak(true);
     }
 
     // <--[event]
     // @Events
     // block fades
     // <block> fades
     //
     // @Triggers when a block fades, melts or disappears based on world conditions.
     // @Context
     // <context.location> will return the location the block faded at.
     // <context.material> will return the material of the block that faded.
     //
     // @Determine
     // "CANCELLED" to stop the block from fading.
     //
     // -->
     @EventHandler
     public void blockFade(BlockFadeEvent event) {
 
         Map<String, dObject> context = new HashMap<String, dObject>();
         dMaterial material = new dMaterial(event.getBlock().getType());
 
         context.put("location", new dLocation(event.getBlock().getLocation()));
         context.put("material", material);
 
         String determination = doEvents(Arrays.asList
                 ("block fades",
                         material.name() + " fades"),
                 null, null, context);
 
         if (determination.toUpperCase().startsWith("CANCELLED"))
             event.setCancelled(true);
 
     }
 
     // <--[event]
     // @Events
     // block ignites
     // <block> ignites
     //
     // @Triggers when a block is set on fire.
     // @Context
     // <context.location> will return the location the block was set on fire at.
     // <context.material> will return the material of the block that was set on fire.
     //
     // @Determine
     // "CANCELLED" to stop the block from being ignited.
     //
     // -->
     @EventHandler
     public void blockIgnite(BlockIgniteEvent event) {
 
         Map<String, dObject> context = new HashMap<String, dObject>();
         dMaterial material = new dMaterial(event.getBlock().getType());
 
         context.put("location", new dLocation(event.getBlock().getLocation()));
         context.put("material", material);
 
         String determination = doEvents(Arrays.asList
                 ("block ignites",
                         material.name() + " ignites"),
                 null, event.getPlayer(), context);
 
         if (determination.toUpperCase().startsWith("CANCELLED"))
             event.setCancelled(true);
     }
 
     // <--[event]
     // @Events
     // block moves
     // <block> moves
     //
     // @Triggers when a block moves.
     // @Context
     // <context.location> will return the location the block moved to.
     // <context.material> will return the material of the block that moved.
     //
     // @Determine
     // "CANCELLED" to stop the block from being moved.
     //
     // -->
     @EventHandler
     public void blockPhysics(BlockPhysicsEvent event) {
 
         Map<String, dObject> context = new HashMap<String, dObject>();
         dMaterial material = new dMaterial(event.getBlock().getType());
 
         context.put("location", new dLocation(event.getBlock().getLocation()));
         context.put("material", material);
 
         String determination = doEvents(Arrays.asList
                 ("block moves",
                         material.name() + " moves"),
                 null, null, context);
 
         if (determination.toUpperCase().startsWith("CANCELLED"))
             event.setCancelled(true);
     }
 
     // <--[event]
     // @Events
     // player places block
     // player places <block>
     //
     // @Triggers when a player places a block.
     // @Context
     // <context.location> will return the location the block that was placed.
     // <context.material> will return the material of the block that was placed.
     //
     // @Determine
     // "CANCELLED" to stop the block from being placed.
     //
     // -->
     @EventHandler
     public void blockPlace(BlockPlaceEvent event) {
 
         Map<String, dObject> context = new HashMap<String, dObject>();
         dMaterial material = new dMaterial(event.getBlock().getType());
 
         context.put("location", new dLocation(event.getBlock().getLocation()));
         context.put("material", material);
 
         String determination = doEvents(Arrays.asList
                 ("player places block",
                         "player places " + material.name()),
                 null, event.getPlayer(), context);
 
         if (determination.toUpperCase().startsWith("CANCELLED"))
             event.setCancelled(true);
     }
 
     // <--[event]
     // @Events
     // block powered
     // <block> powered
     // block unpowered
     // <block> unpowered
     //
     // @Triggers when a block is (un)powered.
     // @Context
     // <context.location> will return the location of the block that was (un)powered.
     // <context.type> will return the material of the block that was (un)powered.
     //
     // @Determine
     // "CANCELLED" to stop the block from being (un)powered.
     //
     // -->
     @EventHandler
     public void blockRedstone(BlockRedstoneEvent event) {
 
         Map<String, dObject> context = new HashMap<String, dObject>();
         dMaterial material = new dMaterial(event.getBlock().getType());
 
         context.put("location", new dLocation(event.getBlock().getLocation()));
         context.put("material", material);
 
         List<String> events = new ArrayList<String>();
 
         if (event.getNewCurrent() > 0) {
             events.add("block powered");
             events.add(material.name() + " powered");
         }
         else {
             events.add("block unpowered");
             events.add(material.name() + " unpowered");
         }
 
         String determination = doEvents(events, null, null, context);
 
         if (determination.toUpperCase().startsWith("CANCELLED"))
             event.setNewCurrent(event.getOldCurrent());
     }
 
     // <--[event]
     // @Events
     // block spreads
     // <block> spreads
     //
     // @Triggers when a liquid block spreads.
     // @Context
     // <context.destination> will return the location the block spread to.
     // <context.location> will return the location the block spread from.
     // <context.type> will return the material of the block that spread.
     //
     // @Determine
     // "CANCELLED" to stop the block from spreading.
     //
     // -->
     @EventHandler
     public void blockFromTo(BlockFromToEvent event) {
 
         Map<String, dObject> context = new HashMap<String, dObject>();
         dMaterial material = new dMaterial(event.getBlock().getType());
 
         context.put("location", new dLocation(event.getBlock().getLocation()));
         context.put("destination", new dLocation(event.getToBlock().getLocation()));
         context.put("material", material);
 
         String determination = doEvents(Arrays.asList
                 ("block spreads",
                         material.name() + " spreads"),
                 null, null, context);
 
         if (determination.toUpperCase().startsWith("CANCELLED"))
             event.setCancelled(true);
     }
 
     // <--[event]
     // @Events
     // player changes sign
     // player changes wall_sign
     // player changes sign_post
     //
     // @Triggers when a player changes a sign.
     // @Context
     // <context.location> will return the location of the sign.
     // <context.new> will return the new sign text as a dList.
     // <context.old> will return the old sign text as a dList.
     //
     // @Determine
     // "CANCELLED" to stop the sign from being changed.
     //
     // -->
     @EventHandler
     public void signChange(final SignChangeEvent event) {
 
         final Map<String, dObject> context = new HashMap<String, dObject>();
 
         final Player player = event.getPlayer();
         final Block block = event.getBlock();
         Sign sign = (Sign) block.getState();
 
         context.put("old", new dList(Arrays.asList(sign.getLines())));
         context.put("new", new dList(Arrays.asList(event.getLines())));
         context.put("location", new dLocation(block.getLocation()));
 
         String determination = doEvents(Arrays.asList
                 ("player changes sign",
                         "player changes " + block.getType().name()),
                 null, player, context);
 
         if (determination.toUpperCase().startsWith("CANCELLED"))
             event.setCancelled(true);
     }
 
 
     /////////////////////
     //   CUSTOM EVENTS
     /////////////////
 
     // <--[event]
     // @Events
     // server start
     //
     // @Triggers when the server starts
     //
     // @Determine "CANCELLED" to save all plugins and cancel server startup.
     //
     // -->
     public void serverStartEvent() {
         // Start the 'timeEvent'
         Bukkit.getScheduler().scheduleSyncRepeatingTask(DenizenAPI.getCurrentInstance(),
                 new Runnable() {
                     @Override
                     public void run() {
                         timeEvent();
                     }
                 }, Settings.WorldScriptTimeEventFrequency().getTicks(), Settings.WorldScriptTimeEventFrequency().getTicks());
 
         // Fire the 'Server Start' event
         String determination = doEvents(Arrays.asList("server start"),
                 null, null, null);
 
         if (determination.toUpperCase().startsWith("CANCELLED"))
             Bukkit.getServer().shutdown();
     }
 
     private Map<String, Integer> current_time = new HashMap<String, Integer>();
 
     // <--[event]
     // @Events
     // time changes in <world>
     // <0-23>:00 in <world>
     // time <0-23> in <world>
     //
     // @Triggers when a block is set on fire.
     // @Context
     // <context.time> will return the current time.
     // <context.world> will return the world.
     //
     // -->
     public void timeEvent() {
         for (World world : Bukkit.getWorlds()) {
             int hour = Double.valueOf(world.getTime() / 1000).intValue();
             hour = hour + 6;
             // Get the hour
             if (hour >= 24) hour = hour - 24;
 
             if (!current_time.containsKey(world.getName())
                     || current_time.get(world.getName()) != hour) {
                 Map<String, dObject> context = new HashMap<String, dObject>();
 
                 context.put("time", new Element(hour));
                 context.put("world", new dWorld(world));
 
                 doEvents(Arrays.asList("time changes in " + world.getName(),
                         String.valueOf(hour) + ":00 in " + world.getName(),
                         "time " + String.valueOf(hour) + " in " + world.getName()),
                         null, null, context);
 
                 current_time.put(world.getName(), hour);
             }
         }
     }
 
 
     /////////////////////
     //   HANGING EVENTS
     /////////////////
 
     // <--[event]
     // @Events
     // hanging breaks
     // hanging breaks because <cause>
     // <entity> breaks
     // <entity> breaks because <cause>
     //
     // @Triggers when a hanging block is broken.
     // @Context
     // <context.cause> will return the cause of the block breaking.
     // <context.entity> will return the entity that broke the hanging block, if any.
     // <context.hanging> will return the hanging block as a dEntity.
     //
     // @Determine
     // "CANCELLED" to stop the hanging block from being broken.
     //
     // -->
     @EventHandler
     public void hangingBreak(HangingBreakEvent event) {
 
         Map<String, dObject> context = new HashMap<String, dObject>();
 
         Player player = null;
         dNPC npc = null;
 
         String hangingType = event.getEntity().getType().name();
         String cause =  event.getCause().name();
 
         context.put("hanging", new dEntity(event.getEntity()));
         context.put("cause", new Element(cause));
 
         List<String> events = new ArrayList<String>();
         events.add("hanging breaks");
         events.add("hanging breaks because " + cause);
         events.add(hangingType + " breaks");
         events.add(hangingType +
                 " breaks because " + cause);
 
         if (event instanceof HangingBreakByEntityEvent) {
 
             // <--[event]
             // @Events
             // <entity> breaks hanging
             // <entity> breaks hanging because <cause>
             // <entity> breaks <hanging>
             // <entity> breaks <hanging> because <cause>
             //
             // @Triggers when a hanging block is broken by an entity.
             // @Context
             // <context.cause> will return the cause of the block breaking.
             // <context.entity> will return the entity that broke the hanging block.
             // <context.hanging> will return the hanging block as a dEntity.
             //
             // @Determine
             // "CANCELLED" to stop the hanging block from being broken.
             //
             // -->
 
             HangingBreakByEntityEvent subEvent = (HangingBreakByEntityEvent) event;
 
             Entity entity = subEvent.getRemover();
             String entityType = entity.getType().name();
 
             if (CitizensAPI.getNPCRegistry().isNPC(entity)) {
                 npc = DenizenAPI.getDenizenNPC(CitizensAPI.getNPCRegistry().getNPC(entity));
                 context.put("entity", npc);
                 entityType = "npc";
             }
             else if (entity instanceof Player) {
                 player = (Player) entity;
                 context.put("entity", new dPlayer((Player) entity));
             }
             else {
                 context.put("entity", new dEntity(entity));
             }
 
             events.add("entity breaks hanging");
             events.add("entity breaks hanging because " + cause);
             events.add("entity breaks " + hangingType);
             events.add("entity breaks " + hangingType + " because " + cause);
             events.add(entityType + " breaks hanging");
             events.add(entityType + " breaks hanging because " + cause);
             events.add(entityType + " breaks " + hangingType);
             events.add(entityType + " breaks " + hangingType + " because " + cause);
         }
 
         String determination = doEvents(events, npc, player, context);
 
         if (determination.toUpperCase().startsWith("CANCELLED"))
             event.setCancelled(true);
     }
 
 
     /////////////////////
     //   ENTITY EVENTS
     /////////////////
 
     // <--[event]
     // @Events
     // entity spawns
     // entity spawns because <cause>
     // <entity> spawns
     // <entity> spawns because <cause>
     //
     // @Triggers when an entity spawns.
     // @Context
     // <context.entity> will return the entity that spawned.
     // <context.reason> will return the reason the entity spawned.
     //
     // @Determine
     // "CANCELLED" to stop the entity from spawning.
     //
     // -->
     @EventHandler
     public void creatureSpawn(CreatureSpawnEvent event) {
 
         Map<String, dObject> context = new HashMap<String, dObject>();
         Entity entity = event.getEntity();
 
         context.put("entity", new dEntity(entity));
         context.put("reason", new Element(event.getSpawnReason().name()));
 
         String determination = doEvents(Arrays.asList
                 ("entity spawns",
                         "entity spawns because " + event.getSpawnReason().name(),
                         entity.getType().name() + " spawns",
                         entity.getType().name() + " spawns because " +
                                 event.getSpawnReason().name()),
                 null, null, context);
 
         if (determination.toUpperCase().startsWith("CANCELLED"))
             event.setCancelled(true);
     }
 
     // <--[event]
     // @Events
     // entity combusts
     // <entity> combusts
     //
     // @Triggers when an entity combusts.
     // @Context
     // <context.duration> will return how long the entity takes to combust.
     // <context.entity> will return the entity that combusted.
     //
     // @Determine
     // "CANCELLED" to stop the entity from combusting.
     //
     // -->
     @EventHandler
     public void entityCombust(EntityCombustEvent event) {
 
         Map<String, dObject> context = new HashMap<String, dObject>();
         Entity entity = event.getEntity();
 
         context.put("entity", new dEntity(entity));
         context.put("duration", new Duration((long) event.getDuration()));
 
         String determination = doEvents(Arrays.asList
                 ("entity combusts",
                         entity.getType().name() + " combusts"),
                 null, null, context);
 
         if (determination.toUpperCase().startsWith("CANCELLED"))
             event.setCancelled(true);
     }
 
     // <--[event]
     // @Events
     // entity damaged
     // entity damaged by <cause>
     // <entity> damaged
     // <entity> damaged by <cause>
     //
     // @Triggers when an entity is damaged.
     // @Context
     // <context.cause> will return the reason the entity was damaged.
     // <context.damage> will return the amount of damage dealt.
     // <context.entity> will return the entity that was damaged.
     //
     // @Determine
     // "CANCELLED" to stop the entity from being damaged.
     // Element(Number) to set the amount of damage the entity receives.
     //
     // -->
     @EventHandler
     public void entityDamage(EntityDamageEvent event) {
 
         Map<String, dObject> context = new HashMap<String, dObject>();
         boolean isFatal = false;
         dEntity entity = new dEntity(event.getEntity());
         String entityType = entity.getEntityType().name();
         String cause = event.getCause().name();
 
         String determination;
 
         dPlayer player = null;
         dNPC npc = null;
 
         if (entity.isNPC()) {
             npc = new dNPC(entity.getNPC());
             context.put("entity", npc);
             entityType = "npc";
         }
         else if (entity.isPlayer()) {
             player = new dPlayer(entity.getPlayer());
             context.put("entity", player);
         }
         else {
             context.put("entity", entity);
         }
 
         context.put("damage", new Element(event.getDamage()));
         context.put("cause", new Element(event.getCause().name()));
 
         if (entity.getLivingEntity() != null) {
             if (event.getDamage() >= entity.getLivingEntity().getHealth()) {
                 isFatal = true;
             }
         }
 
         List<String> events = new ArrayList<String>();
         events.add("entity damaged");
         events.add("entity damaged by " + cause);
         events.add(entityType + " damaged");
         events.add(entityType + " damaged by " + cause);
 
         if (isFatal) {
 
             // <--[event]
             // @Events
             // entity killed
             // entity killed by <cause>
             // <entity> killed
             // <entity> killed by <cause>
             //
             // @Triggers when an entity is killed.
             // @Context
             // <context.cause> will return the reason the entity was killed.
             // <context.entity> will return the entity that was killed.
             // <context.damage> will return the amount of damage dealt.
             // <context.shooter> will return the shooter of the entity, if any.
             //
             // @Determine
             // "CANCELLED" to stop the entity from being killed.
             // Element(Number) to set the amount of damage the entity receives, instead of dying.
             //
             // -->
 
             events.add("entity killed");
             events.add("entity killed by " + cause);
             events.add(entityType + " killed");
             events.add(entityType + " killed by " + cause);
         }
 
         if (event instanceof EntityDamageByEntityEvent) {
 
             // <--[event]
             // @Events
             // entity damages entity
             // entity damages <entity>
             // <entity> damages entity
             // <entity> damages <entity>
             //
             // @Triggers when an entity damages another entity.
             // @Context
             // <context.cause> will return the reason the entity was damaged.
             // <context.entity> will return the entity that was damaged.
             // <context.damage> will return the amount of damage dealt.
             // <context.damager> will return the entity damaging the other entity.
             // <context.shooter> will return the shooter of the entity, if any.
             //
             // @Determine
             // "CANCELLED" to stop the entity from being damaged.
             // Element(Number) to set the amount of damage the entity receives.
             //
             // -->
 
             EntityDamageByEntityEvent subEvent = (EntityDamageByEntityEvent) event;
 
             // Have a different set of player and NPC contexts for events
             // like "player damages player" from the one we have for
             // "player damaged by player"
 
             dPlayer subPlayer = null;
             dNPC subNPC = null;
 
             dEntity damager = new dEntity(subEvent.getDamager());
             String damagerType = damager.getEntityType().name();
 
             if (damager.isNPC()) {
                 subNPC = new dNPC(damager.getNPC());
                 context.put("damager", subNPC);
                 damagerType = "npc";
 
                 // If we had no NPC in our regular context, use this one
                 if (npc == null) npc = subNPC;
             }
 
             else if (damager.isPlayer()) {
                 subPlayer = new dPlayer(damager.getPlayer());
                 context.put("damager", subPlayer);
 
                 // If we had no player in our regular context, use this one
                 if (player == null) player = subPlayer;
             }
 
             else {
                 context.put("damager", damager);
 
                 if (damager.getBukkitEntity() instanceof Projectile) {
                     if (((Projectile) damager.getBukkitEntity()).getShooter() != null) {
 
                         dEntity shooter = new dEntity(((Projectile) damager.getBukkitEntity()).getShooter());
 
                         if (shooter.isNPC()) {
                             context.put("shooter", new dNPC(shooter.getNPC()));
                         }
                         else if (shooter.isPlayer()) {
                             context.put("shooter", new dPlayer(shooter.getPlayer()));
                         }
                         else {
                             context.put("shooter", shooter);
                         }
                     }
                     else {
                         context.put("shooter", new Element("null"));
                     }
                 }
             }
 
             events.add("entity damaged by entity");
             events.add("entity damaged by " + damagerType);
             events.add(entityType + " damaged by entity");
             events.add(entityType + " damaged by " + damagerType);
 
             // Have a new list of events for the subContextPlayer
             // and subContextNPC
 
             List<String> subEvents = new ArrayList<String>();
 
             subEvents.add("entity damages entity");
             subEvents.add("entity damages " + entityType);
             subEvents.add(damagerType + " damages entity");
             subEvents.add(damagerType + " damages " + entityType);
 
             if (isFatal) {
                 events.add("entity killed by entity");
                 events.add("entity killed by " + damagerType);
                 events.add(entityType + " killed by entity");
                 events.add(entityType + " killed by " + damagerType);
 
                 // <--[event]
                 // @Events
                 // entity kills entity
                 // entity kills <entity>
                 // <entity> kills entity
                 // <entity> kills <entity>
                 //
                 // @Triggers when an entity kills another entity.
                 // @Context
                 // <context.cause> will return the reason the entity was killed.
                 // <context.entity> will return the entity that was killed.
                 // <context.damager> will return the entity killing the other entity.
                 // <context.shooter> will return the shooter of the entity, if any.
                 //
                 // @Determine
                 // "CANCELLED" to stop the entity from being killed.
                 // Element(Number) to set the amount of damage the entity receives, instead of dying.
                 //
                 // -->
 
                 subEvents.add("entity kills entity");
                 subEvents.add("entity kills " + entityType);
                 subEvents.add(damagerType + " kills entity");
                 subEvents.add(damagerType + " kills " + entityType);
             }
 
             determination = doEvents(subEvents, (subNPC != null ? subNPC : null), (subPlayer != null?subPlayer.getPlayerEntity():null), context);
 
             if (determination.toUpperCase().startsWith("CANCELLED"))
                 event.setCancelled(true);
 
             else if (Argument.valueOf(determination)
                     .matchesPrimitive(aH.PrimitiveType.Double)) {
                 event.setDamage(aH.getDoubleFrom(determination));
             }
         }
 
         determination = doEvents(events, (npc != null ? npc : null),
                 (player != null && player.isOnline() ? player.getPlayerEntity() : null), context);
 
         if (determination.toUpperCase().startsWith("CANCELLED"))
             event.setCancelled(true);
         else if (Argument.valueOf(determination)
                 .matchesPrimitive(aH.PrimitiveType.Double)) {
             event.setDamage(aH.getDoubleFrom(determination));
         }
     }
 
     // <--[event]
     // @Events
     // projectile hits block
     // <projectile> hits block
     //
     // @Triggers when a projectile hits a block.
     // @Context
     // <context.entity> will return the projectile.
     // <context.shooter> will return the shooter of the projectile, if any.
     //
     // -->
     @EventHandler
     public void projectileHit(ProjectileHitEvent event) {
         Map<String, dObject> context = new HashMap<String, dObject>();
         Entity entity = event.getEntity();
         Entity shooter = ((Projectile)entity).getShooter();
         Player player = null;
 
         context.put("entity", new dEntity(entity));
         context.put("location", new dLocation(entity.getLocation()));
 
         if (shooter != null) {
             context.put("shooter", new dEntity(shooter));
             if (shooter instanceof Player) {
                 player = (Player)shooter;
             }
         }
         Block hit = null;
         BlockIterator bi = new BlockIterator(entity.getLocation().getWorld(), entity.getLocation().toVector(), entity.getLocation().getDirection().normalize(), 0, 4);
         while(bi.hasNext()) {
             hit = bi.next();
             if(hit.getTypeId() != 0) {
                 break;
             }
         }
         doEvents(Arrays.asList("projectile hits block",
                 entity.getType().name() + " hits block",
                 "projectile hits " + hit.getType().name(),
                 entity.getType().name() + " hits " + hit.getType().name()),
                 null, player, context);
     }
 
     // <--[event]
     // @Events
     // entity explodes
     // <entity> explodes
     //
     // @Triggers when an entity explodes.
     // @Context
     // <context.blocks> will return a dList of blocks that the entity blew up.
     // <context.entity> will return the entity that exploded.
     // <context.location> will return the location the entity blew up at.
     //
     // @Determine
     // "CANCELLED" to stop the entity from exploding.
     //
     // -->
     @EventHandler
     public void entityExplode(EntityExplodeEvent event) {
 
         Map<String, dObject> context = new HashMap<String, dObject>();
         Entity entity = event.getEntity();
 
         if (entity == null) return;
 
         context.put("entity", new dEntity(entity));
         context.put("location", new dLocation(event.getLocation()));
         String blocks = "";
         for (Block block : event.blockList()) {
             blocks = blocks + new dLocation(block.getLocation()) + "|";
         }
         context.put("blocks", blocks.length() > 0 ? new dList(blocks) : null);
 
         String determination = doEvents(Arrays.asList
                 ("entity explodes",
                         entity.getType().name() + " explodes"),
                 null, null, context);
 
         if (determination.toUpperCase().startsWith("CANCELLED"))
             event.setCancelled(true);
     }
 
     // <--[event]
     // @Events
     // entity heals
     // entity heals because <cause>
     // <entity> heals
     // <entity> heals because <cause>
     //
     // @Triggers when an entity heals.
     // @Context
     // <context.amount> will return the amount the entity healed.
     // <context.entity> will return the entity that healed.
     // <context.reason> will return the cause of the entity healing.
     //
     // @Determine
     // "CANCELLED" to stop the entity from healing.
     // Element(Number) to set the amount of health the entity receives.
     //
     // -->
     @EventHandler
     public void entityRegainHealth(EntityRegainHealthEvent event) {
 
         Map<String, dObject> context = new HashMap<String, dObject>();
         Entity entity = event.getEntity();
         String entityType = entity.getType().name();
 
         context.put("reason", new Element(event.getRegainReason().name()));
         context.put("amount", new Element(event.getAmount()));
 
         Player player = null;
         dNPC npc = null;
 
         if (CitizensAPI.getNPCRegistry().isNPC(entity)) {
             npc = DenizenAPI.getDenizenNPC(CitizensAPI.getNPCRegistry().getNPC(entity));
             context.put("entity", npc);
             entityType = "npc";
         }
         else if (entity instanceof Player) {
             player = (Player) entity;
             context.put("entity", new dPlayer(player));
         }
         else {
             context.put("entity", new dEntity(entity));
         }
 
         String determination = doEvents(Arrays.asList
                 ("entity heals",
                         "entity heals because " + event.getRegainReason().name(),
                         entityType + " heals",
                         entityType + " heals because " + event.getRegainReason().name()),
                 npc, player, context);
 
         if (determination.toUpperCase().startsWith("CANCELLED"))
             event.setCancelled(true);
 
         else if (Argument.valueOf(determination)
                 .matchesPrimitive(aH.PrimitiveType.Double)) {
             event.setAmount(aH.getDoubleFrom(determination));
         }
     }
 
     // <--[event]
     // @Events
     // entity shoots bow
     // <entity> shoots bow
     //
     // @Triggers when an entity shoots something out of a bow.
     // @Context
     // <context.entity> will return the entity that shot the bow.
     // <context.projectile> will return the projectile as a dEntity.
     //
     // @Determine
     // "CANCELLED" to stop the entity from shooting the bow.
     // dList(dEntity) to change the projectile(s) being shot.
     //
     // -->
     @EventHandler
     public void entityShootBow(EntityShootBowEvent event) {
 
         Map<String, dObject> context = new HashMap<String, dObject>();
         LivingEntity entity = event.getEntity();
         String entityType = entity.getType().name();
         Entity projectile = event.getProjectile();
 
         Player player = null;
         dNPC npc = null;
 
         if (CitizensAPI.getNPCRegistry().isNPC(entity)) {
             npc = DenizenAPI.getDenizenNPC(CitizensAPI.getNPCRegistry().getNPC(entity));
             context.put("entity", npc);
             entityType = "npc";
         }
         else if (entity instanceof Player) {
             player = (Player) entity;
             context.put("entity", new dPlayer((Player) entity));
         }
         else {
             context.put("entity", new dEntity(entity));
         }
 
         context.put("bow", new dItem(event.getBow()));
         context.put("projectile", new dEntity(projectile));
 
         String determination = doEvents(Arrays.asList
                 ("entity shoots bow",
                         entityType + " shoots bow"),
                 npc, player, context);
 
         if (determination.toUpperCase().startsWith("CANCELLED")) {
             event.setCancelled(true);
         }
 
         // Don't use event.setProjectile() because it doesn't work
         else if (Argument.valueOf(determination).matchesArgumentList(dEntity.class)) {
 
             event.setCancelled(true);
 
             // Get the list of entities
             Object list = dList.valueOf(determination).filter(dEntity.class);
 
             @SuppressWarnings("unchecked")
             List<dEntity> newProjectiles = (List<dEntity>) list;
 
             // Go through all the entities, spawning/teleporting them
             for (dEntity newProjectile : newProjectiles) {
 
                 if (!newProjectile.isSpawned()) {
                     newProjectile.spawnAt(projectile.getLocation());
                 }
                 else {
                     newProjectile.teleport(projectile.getLocation());
                 }
 
                 // Set the entity as the shooter of the projectile
                 if (newProjectile.getBukkitEntity() instanceof Projectile) {
                     ((Projectile) newProjectile.getBukkitEntity())
                             .setShooter((LivingEntity) entity);
                 }
             }
 
             // Mount the projectiles on top of each other
             Position.mount(Conversion.convert(newProjectiles));
 
             // Get the last entity on the list, i.e. the one at the bottom
             // if there are many mounted on top of each other
             Entity lastProjectile = newProjectiles.get
                     (newProjectiles.size() - 1).getBukkitEntity();
 
             // Give it the same velocity as the arrow that would
             // have been shot by the bow
             lastProjectile.setVelocity(projectile.getVelocity());
         }
     }
 
     // <--[event]
     // @Events
     // entity tamed
     // <entity> tamed
     // player tames entity
     // player tames <entity>
     //
     // @Triggers when an entity is tamed.
     // @Context
     // <context.entity> will return the tamed entity.
     //
     // @Determine
     // "CANCELLED" to stop the entity from being tamed.
     //
     // -->
     @EventHandler
     public void entityTame(EntityTameEvent event) {
 
         Map<String, dObject> context = new HashMap<String, dObject>();
         Entity entity = event.getEntity();
         context.put("entity", new dEntity(entity));
         Player player = null;
 
         List<String> events = new ArrayList<String>();
         events.add("entity tamed");
         events.add(entity.getType().name() + " tamed");
 
         if (event.getOwner() instanceof Player) {
             player = (Player) event.getOwner();
             events.add("player tames entity");
             events.add("player tames " + entity.getType().name());
         }
 
         String determination = doEvents(events, null, player, context);
 
         if (determination.toUpperCase().startsWith("CANCELLED"))
             event.setCancelled(true);
     }
 
     // <--[event]
     // @Events
     // entity targets (<entity>)
     // entity targets (<entity>) because <cause>
     // <entity> targets (<entity>)
     // <entity> targets (<entity>) because <cause>
     //
     // @Triggers when an entity targets a new entity.
     // @Context
     // <context.entity> will return the targeting entity.
     // <context.reason> will return the reason the entity changed targets.
     // <context.target> will return the targeted entity.
     //
     // @Determine
     // "CANCELLED" to stop the entity from being targeted.
     // dEntity to make the entity change targets to a different entity instead.
     //
     // -->
     @EventHandler
     public void entityTarget(EntityTargetEvent event) {
 
         Map<String, dObject> context = new HashMap<String, dObject>();
         Entity entity = event.getEntity();
         Entity target = event.getTarget();
         Player player = null;
 
         String reason = event.getReason().name();
         String entityType = entity.getType().name();
 
         context.put("entity", new dEntity(entity));
         context.put("reason", new Element(reason));
 
         List<String> events = new ArrayList<String>();
         events.add("entity targets");
         events.add("entity targets because " + reason);
         events.add(entityType + " targets");
         events.add(entityType + " targets because " + reason);
 
         if (target != null) {
 
             if (event.getTarget() instanceof Player) {
                 player = (Player) target;
                 context.put("target", new dPlayer(player));
             }
             else {
                 context.put("target", new dEntity(target));
             }
 
             String targetType = target.getType().name();
 
             events.add("entity targets entity");
             events.add("entity targets entity because " + reason);
             events.add("entity targets " + targetType);
             events.add("entity targets " + targetType + " because " + reason);
             events.add(entityType + " targets entity");
             events.add(entityType + " targets entity because " + reason);
             events.add(entityType + " targets " + targetType);
             events.add(entityType + " targets " + targetType + " because " + reason);
         }
 
         String determination = doEvents(events, null, player, context);
 
         if (determination.toUpperCase().startsWith("CANCELLED"))
             event.setCancelled(true);
 
             // If the determination matches a dEntity, change the event's target
             //
             // Note: this only works with a handful of monster types, like spiders
             //       and endermen for instance
 
         else if (dEntity.matches(determination)) {
             final dEntity attacker = new dEntity(entity);
             final dEntity newTarget = dEntity.valueOf(determination);
 
             Bukkit.getScheduler().scheduleSyncDelayedTask(DenizenAPI.getCurrentInstance(), new Runnable() {
                 public void run() {
                     attacker.target(newTarget.getLivingEntity());
                 }
             }, 1);
         }
     }
 
     @EventHandler
     public void entityTeleport(EntityTeleportEvent event) {
 
         Map<String, dObject> context = new HashMap<String, dObject>();
         Entity entity = event.getEntity();
 
         context.put("entity", new dEntity(entity));
         context.put("origin", new dLocation(event.getFrom()));
         context.put("destination", new dLocation(event.getTo()));
 
         String determination = doEvents(Arrays.asList
                 ("entity teleports",
                         entity.getType().name() + " teleports"),
                 null, null, context);
 
         if (determination.toUpperCase().startsWith("CANCELLED"))
             event.setCancelled(true);
     }
 
     @EventHandler
     public void explosionPrimeEvent(ExplosionPrimeEvent event) {
 
         Map<String, dObject> context = new HashMap<String, dObject>();
         Entity entity = event.getEntity();
 
         context.put("entity", new dEntity(entity));
         context.put("radius", new Element(event.getRadius()));
         context.put("fire", new Element(event.getFire()));
 
         String determination = doEvents(Arrays.asList
                 ("entity explosion primes",
                         entity.getType().name() + " explosion primes"),
                 null, null, context);
 
         if (determination.toUpperCase().startsWith("CANCELLED"))
             event.setCancelled(true);
     }
 
     @EventHandler
     public void foodLevelChange(FoodLevelChangeEvent event) {
 
         Map<String, dObject> context = new HashMap<String, dObject>();
         Entity entity = event.getEntity();
 
         context.put("entity", entity instanceof Player ?
                 new dPlayer((Player) entity) :
                 new dEntity(entity));
 
         String determination = doEvents(Arrays.asList
                 (entity.getType().name() + " changes food level"),
                 null, null, context);
 
         if (determination.toUpperCase().startsWith("CANCELLED"))
             event.setCancelled(true);
         else if (Argument.valueOf(determination)
                 .matchesPrimitive(aH.PrimitiveType.Double)) {
             event.setFoodLevel(aH.getIntegerFrom(determination));
         }
     }
 
     @EventHandler
     public void itemDespawn(ItemDespawnEvent event) {
 
         Map<String, dObject> context = new HashMap<String, dObject>();
         dItem item = new dItem(event.getEntity().getItemStack());
 
         context.put("item", item);
         context.put("entity", new dEntity(event.getEntity()));
 
         List<String> events = new ArrayList<String>();
         events.add("item despawns");
         events.add(item.identify() + " despawns");
 
         if (!item.identify().equals(item.identify().split(":")[0])) {
             events.add(item.identify().split(":")[0] + " despawns");
         }
         if (item.isItemscript()) {
             events.add("itemscript " + item.getMaterial() + " despawns");
         }
 
         String determination = doEvents(events, null, null, context);
 
         if (determination.toUpperCase().startsWith("CANCELLED"))
             event.setCancelled(true);
     }
 
     @EventHandler
     public void itemSpawn(ItemSpawnEvent event) {
 
         Map<String, dObject> context = new HashMap<String, dObject>();
         dItem item = new dItem(event.getEntity().getItemStack());
 
         context.put("item", item);
         context.put("entity", new dEntity(event.getEntity()));
 
         List<String> events = new ArrayList<String>();
         events.add("item spawns");
         events.add(item.identify() + " spawns");
 
         if (!item.identify().equals(item.identify().split(":")[0])) {
             events.add(item.identify().split(":")[0] + " spawns");
         }
         if (item.isItemscript()) {
             events.add("itemscript " + item.getMaterial() + " spawns");
         }
 
         String determination = doEvents(events, null, null, context);
 
         if (determination.toUpperCase().startsWith("CANCELLED"))
             event.setCancelled(true);
     }
 
 
     /////////////////////
     //   INVENTORY EVENTS
     /////////////////
 
     @EventHandler
     public void inventoryClickEvent(InventoryClickEvent event) {
 
         Map<String, dObject> context = new HashMap<String, dObject>();
         dItem item = new dItem(event.getCurrentItem());
 
         Player player = (Player) event.getWhoClicked();
         String type = event.getInventory().getType().name();
         String click = event.getClick().name();
 
         context.put("item", item);
         context.put("inventory", new dInventory(event.getInventory()));
         context.put("click", new Element(click));
 
         List<String> events = new ArrayList<String>();
         events.add("player clicks in inventory");
         events.add("player clicks in " + type + " inventory");
 
         String interaction = "player " + click + " clicks";
 
         events.add(interaction + " in inventory");
         events.add(interaction + " in " + type + " inventory");
 
         if (item.getItemStack() != null) {
 
             events.add("player clicks " +
                     item.identify() + " in inventory");
             events.add(interaction +
                     item.identify() + " in inventory");
             events.add(interaction +
                     item.identify() + " in " + type + " inventory");
 
             if (!item.identify().equals(item.identify().split(":")[0])) {
                 events.add("player clicks " +
                         item.identify().split(":")[0] + " in inventory");
                 events.add(interaction +
                         item.identify().split(":")[0] + " in inventory");
                 events.add(interaction +
                         item.identify().split(":")[0] + " in " + type + " inventory");
             }
             if (item.isItemscript()) {
                 events.add("player clicks " +
                         item.getMaterial() + " in inventory");
                 events.add(interaction +
                         item.getMaterial() + " in inventory");
                 events.add(interaction +
                         item.getMaterial() + " in " + type + " inventory");
             }
         }
 
         String determination = doEvents(events, null, player, context);
 
         if (determination.toUpperCase().startsWith("CANCELLED"))
             event.setCancelled(true);
     }
 
     @EventHandler
     public void inventoryCloseEvent(InventoryCloseEvent event) {
 
         Map<String, dObject> context = new HashMap<String, dObject>();
 
         Player player = (Player) event.getPlayer();
         String type = event.getInventory().getType().name();
 
         context.put("inventory", new dInventory(event.getInventory()));
 
         doEvents(Arrays.asList
                 ("player closes inventory",
                         "player closes " + type + " inventory"),
                 null, player, context);
     }
 
     @EventHandler
     public void inventoryDragEvent(InventoryDragEvent event) {
 
         Map<String, dObject> context = new HashMap<String, dObject>();
         dItem item = new dItem(event.getOldCursor());
 
         Player player = (Player) event.getWhoClicked();
         String type = event.getInventory().getType().name();
 
         context.put("item", item);
         context.put("inventory", new dInventory(event.getInventory()));
 
         List<String> events = new ArrayList<String>();
         events.add("player drags");
         events.add("player drags in inventory");
         events.add("player drags in " + type + " inventory");
 
         if (item.getItemStack() != null) {
 
             events.add("player drags " +
                     item.identify());
             events.add("player drags " +
                     item.identify() + " in inventory");
             events.add("player drags " +
                     item.identify() + " in " + type + " inventory");
 
             if (!item.identify().equals(item.identify().split(":")[0])) {
                 events.add("player drags " +
                         item.identify().split(":")[0]);
                 events.add("player drags " +
                         item.identify().split(":")[0] + " in inventory");
                 events.add("player drags " +
                         item.identify().split(":")[0] + " in " + type + " inventory");
             }
             if (item.isItemscript()) {
                 events.add("player drags itemscript " +
                         item.getMaterial());
                 events.add("player drags itemscript " +
                         item.getMaterial() + " in inventory");
                 events.add("player drags itemscript " +
                         item.getMaterial() + " in " + type + " inventory");
             }
         }
 
         String determination = doEvents(events, null, player, context);
 
         if (determination.toUpperCase().startsWith("CANCELLED"))
             event.setCancelled(true);
     }
 
     @EventHandler
     public void inventoryOpenEvent(InventoryOpenEvent event) {
 
         Map<String, dObject> context = new HashMap<String, dObject>();
 
         Player player = (Player) event.getPlayer();
         String type = event.getInventory().getType().name();
 
         context.put("inventory", new dInventory(event.getInventory()));
 
         String determination = doEvents(Arrays.asList
                 ("player opens inventory",
                         "player opens " + type + " inventory"),
                 null, player, context);
 
         if (determination.toUpperCase().startsWith("CANCELLED"))
             event.setCancelled(true);
     }
 
 
 
     /////////////////////
     //   PLAYER EVENTS
     /////////////////
 
     @EventHandler(priority = EventPriority.LOWEST)
     public void asyncPlayerChat(final AsyncPlayerChatEvent event) {
 
         // Return if "Use asynchronous event" is false in config file
         if (!Settings.WorldScriptChatEventAsynchronous()) return;
 
         final Map<String, dObject> context = new HashMap<String, dObject>();
         context.put("message", new Element(event.getMessage()));
 
         Callable<String> call = new Callable<String>() {
             public String call() {
                 return doEvents(Arrays.asList("player chats"),
                         null, event.getPlayer(), context);
             }
         };
         String determination = null;
         try {
             determination = event.isAsynchronous() ? Bukkit.getScheduler().callSyncMethod(DenizenAPI.getCurrentInstance(), call).get() : call.call();
         } catch (InterruptedException e) {
             // TODO: Need to find a way to fix this eventually
             // e.printStackTrace();
         } catch (ExecutionException e) {
             e.printStackTrace();
         } catch (Exception e) {
             e.printStackTrace();
         }
 
         if (determination == null)
             return;
         if (determination.toUpperCase().startsWith("CANCELLED"))
             event.setCancelled(true);
         else if (!determination.equals("none")) {
             event.setMessage(determination);
         }
     }
 
     @EventHandler
     public void syncPlayerChat(final PlayerChatEvent event) {
 
         // Return if "Use asynchronous event" is true in config file
         if (Settings.WorldScriptChatEventAsynchronous()) return;
 
         final Map<String, dObject> context = new HashMap<String, dObject>();
         context.put("message", new Element(event.getMessage()));
 
         String determination = doEvents(Arrays.asList("player chats"),
                 null, event.getPlayer(), context);
 
         if (determination.toUpperCase().startsWith("CANCELLED"))
             event.setCancelled(true);
         else if (!determination.equals("none")) {
             event.setMessage(determination);
         }
     }
 
     @EventHandler
     public void bedEnterEvent(PlayerBedEnterEvent event) {
 
         Map<String, dObject> context = new HashMap<String, dObject>();
         context.put("location", new dLocation(event.getBed().getLocation()));
 
         String determination = doEvents
                 (Arrays.asList("player enters bed"),
                         null, event.getPlayer(), context);
 
         if (determination.toUpperCase().startsWith("CANCELLED"))
             event.setCancelled(true);
     }
 
     @EventHandler
     public void bedLeaveEvent(PlayerBedLeaveEvent event) {
 
         Map<String, dObject> context = new HashMap<String, dObject>();
         context.put("location", new dLocation(event.getBed().getLocation()));
 
         doEvents(Arrays.asList
                 ("player leaves bed"),
                 null, event.getPlayer(), context);
     }
 
     @EventHandler
     public void playerBucketEmpty(PlayerBucketEmptyEvent event) {
 
         Map<String, dObject> context = new HashMap<String, dObject>();
         context.put("item", new dItem(event.getBucket()));
         context.put("location", new dLocation(event.getBlockClicked().getLocation()));
 
         String determination = doEvents(Arrays.asList
                 ("player empties bucket"),
                 null, event.getPlayer(), context);
 
         // Handle message
         if (determination.toUpperCase().startsWith("CANCELLED"))
             event.setCancelled(true);
         if (!determination.equals("none")) {
             ItemStack is = dItem.valueOf(determination).getItemStack();
             event.setItemStack( is != null ? is : new ItemStack(Material.AIR));
         }
 
     }
 
     @EventHandler
     public void playerBucketFill(PlayerBucketFillEvent event) {
 
         Map<String, dObject> context = new HashMap<String, dObject>();
         context.put("item", new dItem(event.getBucket()));
         context.put("location", new dLocation(event.getBlockClicked().getLocation()));
 
         String determination = doEvents(Arrays.asList
                 ("player fills bucket"),
                 null, event.getPlayer(), context);
 
         // Handle message
         if (determination.toUpperCase().startsWith("CANCELLED"))
             event.setCancelled(true);
         if (!determination.equals("none")) {
             ItemStack is = dItem.valueOf(determination).getItemStack();
             event.setItemStack( is != null ? is : new ItemStack(Material.AIR));
         }
     }
 
     // <--[example]
     // @Title On Command Event tutorial
     // @Description
     // Denizen contains the ability to run script entries in the form
     // of a bukkit /command. Here's an example script that shows basic usage.
     //
     // @Code
     // # +--------------------
     // # | On Command Event tutorial
     //
     // # | Denizen contains the ability to run script entries in the form
     // # | of a Bukkit /command. Here's an example script that shows basic usage.
     //
     // On Command Event Tutorial:
     //  type: world
     //
     // # +-- EVENTS: Node --+
     // # To 'hook' into the on command event, just create a 'on <command_name> command'
     // # node as a child of the events node in any world script. Change out <command_name>
     // # with the desired name of the command. This can only be one word.
     //
     //   events:
     //
     //     # The following example will trigger on the use of '/testcommand'
     //     on testcommand command:
     //
     //     # Why not state the obvious? Just to be sure!
     //     - narrate 'You just used the /testcommand command!'
     //
     //     # You can utilize any arguments that come along with the command, too!
     //     # <c.args> returns a list of the arguments, run through the Denizen argument
     //     # interpreter. Using quotes will allow the use of multiple word arguments,
     //     # just like Denizen!
     //     # Just need what was typed after the command? Use <c.raw_args> for a String
     //     # Element containing the uninterpreted arguments.
     //     - define arg_size <c.args.size>
     //     - narrate "'%arg_size%' arguments were used."
     //     - if %arg_size% > 0 {
    //       - narrate "'<c.args.get[1]>' was the first argument."
    //       - narrate "Here's a list of all the arguments<&co> <c.args.as_cslist>"
     //       }
     //
     //     # When a command isn't found, Bukkit reports an error. To let bukkit know
     //     # that the command was handled, use the 'determine fulfilled' command/arg.
     //     - determine fulfilled
     //
     // -->
 
     // <--[event]
     // @Events
     // command
     // <command_name> command
     //
     // @Triggers when a player or console runs a Bukkit command. This happens before
     // any code of established commands allowing scripters to 'override' existing commands.
     // @Context
     // <context.command> will return the command name as an Element.
     // <context.raw_args> will return any args used as an Element.
     // <context.args> will return a dList of the arguments, parsed with Denizen's
     //   argument parser. Just like any Denizen Command, quotes and tags can be used.
     // <context.server> will return true if the command was run from the console.
     //
     // @Determine
     // "FULFILLED" to tell Bukkit the command was handled.
     //
     // -->
     @EventHandler
     public void playerChangedWorldEvent(PlayerChangedWorldEvent event) {
 
         Map<String, dObject> context = new HashMap<String, dObject>();
         dWorld originWorld = new dWorld(event.getFrom());
         context.put("origin_world", originWorld);
 
         doEvents(Arrays.asList
                 ("player changes world",
                  "player changes world from " + originWorld.identify()),
                  null, event.getPlayer(), context);
     }
 
     @EventHandler
     public void playerCommandEvent(PlayerCommandPreprocessEvent event) {
         Map<String, dObject> context = new HashMap<String, dObject>();
 
         dPlayer player = dPlayer.valueOf(event.getPlayer().getName());
 
         // Well, this is ugly :(
         // Fill tags in any arguments
         List<String> args = Arrays.asList(
                 aH.buildArgs(
                         TagManager.tag(player, null,
                                 (event.getMessage().split(" ").length > 1 ? event.getMessage().split(" ", 2)[1] : ""))));
 
         dList args_list = new dList(args);
 
         String command = event.getMessage().split(" ")[0].replace("/", "").toUpperCase();
 
         // Fill context
         context.put("args", args_list);
         context.put("command", new Element(command));
         context.put("raw_args", new Element((event.getMessage().split(" ").length > 1
                 ? event.getMessage().split(" ", 2)[1] : "")));
         context.put("server", Element.FALSE);
         String determination;
 
         // Run any event scripts and get the determination.
         determination = doEvents(Arrays.asList
                 ("command",
                         command + " command"), null, event.getPlayer(), context).toUpperCase();
 
         // If a script has determined fulfilled, cancel this event so the player doesn't
         // receive the default 'Invalid command' gibberish from bukkit.
         if (determination.equals("FULFILLED") || determination.equals("CANCELLED"))
             event.setCancelled(true);
     }
 
     @EventHandler
     public void playerDeath(PlayerDeathEvent event) {
 
         Map<String, dObject> context = new HashMap<String, dObject>();
         context.put("message", new Element(event.getDeathMessage()));
 
         String determination = doEvents(Arrays.asList
                 ("player dies",
                         "player death"),
                 null, event.getEntity(), context);
 
         // Handle message
         if (!determination.equals("none")) {
             event.setDeathMessage(determination);
         }
     }
 
     @EventHandler
     public void playerFish(PlayerFishEvent event) {
 
         Entity entity = event.getCaught();
         String state = event.getState().name();
         dNPC npc = null;
 
         Map<String, dObject> context = new HashMap<String, dObject>();
         context.put("hook", new dEntity(event.getHook()));
         context.put("state", new Element(state));
 
         List<String> events = new ArrayList<String>();
         events.add("player fishes");
         events.add("player fishes while " + state);
 
         if (entity != null) {
 
             String entityType = entity.getType().name();
 
             if (CitizensAPI.getNPCRegistry().isNPC(entity)) {
                 npc = DenizenAPI.getDenizenNPC(CitizensAPI.getNPCRegistry().getNPC(entity));
                 context.put("entity", npc);
                 entityType = "npc";
             }
             else if (entity instanceof Player) {
                 context.put("entity", new dPlayer((Player) entity));
             }
             else {
                 context.put("entity", new dEntity(entity));
             }
 
             events.add("player fishes " + entityType);
             events.add("player fishes " + entityType + " while " + state);
         }
 
         String determination = doEvents(events, npc, event.getPlayer(), context);
 
         // Handle message
         if (determination.toUpperCase().startsWith("CANCELLED"))
             event.setCancelled(true);
     }
 
     @EventHandler
     public void playerGameModeChange(PlayerGameModeChangeEvent event) {
 
         Map<String, dObject> context = new HashMap<String, dObject>();
         context.put("gamemode", new Element(event.getNewGameMode().name()));
 
         String determination = doEvents(Arrays.asList
                 ("player changes gamemode",
                         "player changes gamemode to " + event.getNewGameMode().name()),
                 null, event.getPlayer(), context);
 
         // Handle message
         if (determination.toUpperCase().startsWith("CANCELLED"))
             event.setCancelled(true);
     }
 
     @EventHandler
     public void playerInteract(PlayerInteractEvent event) {
 
         Map<String, dObject> context = new HashMap<String, dObject>();
         Action action = event.getAction();
         dItem item = null;
 
         List<String> events = new ArrayList<String>();
 
         String interaction;
 
         if (action == Action.LEFT_CLICK_AIR || action == Action.LEFT_CLICK_BLOCK)
             interaction = "player left clicks";
         else if (action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK)
             interaction = "player right clicks";
             // The only other action is PHYSICAL, which is triggered when a player
             // stands on a pressure plate
         else interaction = "player stands";
 
         events.add(interaction);
 
         if (event.hasItem()) {
             item = new dItem(event.getItem());
             context.put("item", item);
 
             events.add(interaction + " with item");
             events.add(interaction + " with " + item.identify());
 
             if (!item.identify().equals(item.identify().split(":")[0])) {
                 events.add(interaction + " with " + item.identify().split(":")[0]);
             }
             if (item.isItemscript()) {
                 events.add(interaction + " with itemscript " + item.getMaterial());
             }
         }
 
         if (event.hasBlock()) {
             Block block = event.getClickedBlock();
             context.put("location", new dLocation(block.getLocation()));
 
             interaction = interaction + " on " + block.getType().name();
             events.add(interaction);
 
             if (event.hasItem()) {
                 events.add(interaction + " with item");
                 events.add(interaction + " with " + item.identify());
 
                 if (!item.identify().equals(item.identify().split(":")[0])) {
                     events.add(interaction + " with " + item.identify().split(":")[0]);
                 }
                 if (item.isItemscript()) {
                     events.add(interaction + " with itemscript " + item.getMaterial());
                 }
             }
         }
 
         String determination = doEvents(events, null, event.getPlayer(), context);
 
         if (determination.toUpperCase().startsWith("CANCELLED"))
             event.setCancelled(true);
     }
 
     @EventHandler
     public void playerInteractEntity(PlayerInteractEntityEvent event) {
 
         Entity entity = event.getRightClicked();
         String entityType = entity.getType().name();
         dNPC npc = null;
         dItem item = new dItem(event.getPlayer().getItemInHand());
 
         String determination;
         Map<String, dObject> context = new HashMap<String, dObject>();
         context.put("location", new dLocation(event.getRightClicked().getLocation()));
 
         if (CitizensAPI.getNPCRegistry().isNPC(entity)) {
             npc = DenizenAPI.getDenizenNPC
                     (CitizensAPI.getNPCRegistry().getNPC(entity));
             context.put("entity", npc);
             entityType = "npc";
         }
         else if (entity instanceof Player) {
             context.put("entity", new dPlayer((Player) entity));
         }
         else {
             context.put("entity", new dEntity(entity));
         }
 
         List<String> events = new ArrayList<String>();
         events.add("player right clicks entity");
         events.add("player right clicks " + entityType);
         events.add("player right clicks entity with " +
                 item.identify());
         events.add("player right clicks " + entityType + " with " +
                 item.identify());
 
         if (!item.identify().equals(item.identify().split(":")[0])) {
             events.add("player right clicks entity with " +
                     item.identify().split(":")[0]);
             events.add("player right clicks " + entityType + " with " +
                     item.identify().split(":")[0]);
         }
         if (item.isItemscript()) {
             events.add("player right clicks entity with itemscript " +
                     item.getMaterial());
             events.add("player right clicks " + entityType + " with itemscript " +
                     item.getMaterial());
         }
 
         if (entity instanceof ItemFrame) {
             dItem itemFrame = new dItem(((ItemFrame) entity).getItem());
             context.put("itemframe", itemFrame);
 
             events.add("player right clicks " + entityType + " " +
                     itemFrame.identify());
 
             if (!itemFrame.identify().equals(itemFrame.identify().split(":")[0])) {
 
                 events.add("player right clicks " + entityType + " " +
                         itemFrame.identify().split(":")[0]);
             }
             if (itemFrame.isItemscript()) {
                 events.add("player right clicks " + entityType +
                         " itemscript " + item.getMaterial());
             }
         }
 
         determination = doEvents(events, npc, event.getPlayer(), context);
 
         if (determination.toUpperCase().startsWith("CANCELLED"))
             event.setCancelled(true);
     }
 
     @EventHandler
     public void playerItemConsume(PlayerItemConsumeEvent event) {
 
         dItem item = new dItem(event.getItem());
 
         Map<String, dObject> context = new HashMap<String, dObject>();
         context.put("item", item);
 
         List<String> events = new ArrayList<String>();
         events.add("player consumes " + item.identify());
 
         if (!item.identify().equals(item.identify().split(":")[0])) {
             events.add("player consumes " + item.identify().split(":")[0]);
         }
         if (item.isItemscript()) {
             events.add("player consumes itemscript " + item.getMaterial());
         }
 
         String determination = doEvents(events, null, event.getPlayer(), context);
 
         if (determination.toUpperCase().startsWith("CANCELLED"))
             event.setCancelled(true);
     }
 
     @EventHandler
     public void playerMoveEvent(PlayerMoveEvent event) {
         if (event.getFrom().getBlock().equals(event.getTo().getBlock())) return;
 
         String name = dLocation.getSaved(event.getPlayer().getLocation());
 
         if (name != null) {
             Map<String, dObject> context = new HashMap<String, dObject>();
             context.put("notable", new Element(name));
 
             String determination = doEvents(Arrays.asList
                     ("player walks over notable",
                             "player walks over " + name,
                             "walked over notable",
                             "walked over " + name),
                     null, event.getPlayer(), context);
 
             if (determination.toUpperCase().startsWith("CANCELLED") ||
                     determination.toUpperCase().startsWith("FROZEN"))
                 event.setCancelled(true);
         }
     }
 
     @EventHandler
     public void playerPickupItem(PlayerPickupItemEvent event) {
         Map<String, dObject> context = new HashMap<String, dObject>();
         dItem item = new dItem(event.getItem().getItemStack());
         context.put("item", item);
         context.put("entity", new dEntity(event.getItem()));
         context.put("location", new dLocation(event.getItem().getLocation()));
 
         List<String> events = new ArrayList<String>();
 
         events.add("player picks up item");
         events.add("player takes item");
         events.add("player picks up " + item.identify());
         events.add("player takes " + item.identify());
 
         String determination = doEvents(events, null, event.getPlayer(), context);
 
         if (determination.toUpperCase().startsWith("CANCELLED"))
             event.setCancelled(true);
     }
 
     @EventHandler
     public void joinEvent(PlayerJoinEvent event) {
         Map<String, dObject> context = new HashMap<String, dObject>();
         context.put("message", new Element(event.getJoinMessage()));
 
         String determination = doEvents(Arrays.asList
                 ("player joins",
                         "player join"),
                 null, event.getPlayer(), context);
 
         // Handle message
         if (!determination.equals("none")) {
             event.setJoinMessage(determination);
         }
     }
 
     @EventHandler
     public void levelChangeEvent(PlayerLevelChangeEvent event) {
 
         Map<String, dObject> context = new HashMap<String, dObject>();
         context.put("level", new Element(event.getNewLevel()));
 
         doEvents(Arrays.asList
                 ("player levels up",
                         "player levels up to " + event.getNewLevel(),
                         "player levels up from " + event.getOldLevel()),
                 null, event.getPlayer(), context);
     }
 
     @EventHandler
     public void loginEvent(PlayerLoginEvent event) {
 
         Map<String, dObject> context = new HashMap<String, dObject>();
         context.put("hostname", new Element(event.getHostname()));
 
         String determination = doEvents(Arrays.asList
                 ("player logs in", "player login"),
                 null, event.getPlayer(), context);
 
         // Handle determine kicked
         if (determination.toUpperCase().startsWith("KICKED"))
             event.disallow(PlayerLoginEvent.Result.KICK_OTHER, determination);
     }
 
     @EventHandler
     public void quitEvent(PlayerQuitEvent event) {
 
         Map<String, dObject> context = new HashMap<String, dObject>();
         context.put("message", new Element(event.getQuitMessage()));
 
         String determination = doEvents(Arrays.asList
                 ("player quits",
                         "player quit"),
                 null, event.getPlayer(), context);
 
         // Handle determine message
         if (!determination.equals("none")) {
             event.setQuitMessage(determination);
         }
     }
 
     @EventHandler
     public void respawnEvent(PlayerRespawnEvent event) {
         Map<String, dObject> context = new HashMap<String, dObject>();
         context.put("location", new dLocation(event.getRespawnLocation()));
 
         List<String> events = new ArrayList<String>();
         events.add("player respawns");
 
         if (event.isBedSpawn()) {
             events.add("player respawns at bed");
         }
         else {
             events.add("player respawns elsewhere");
         }
 
         String determination = doEvents(events, null, event.getPlayer(), context);
 
         // Handle determine message
         if (dLocation.matches(determination)) {
             dLocation location = dLocation.valueOf(determination);
 
             if (location != null) event.setRespawnLocation(location);
         }
     }
 
 
     /////////////////////
     //   SERVER EVENTS
     /////////////////
 
     @EventHandler
     public void serverCommandEvent(ServerCommandEvent event) {
         Map<String, dObject> context = new HashMap<String, dObject>();
 
         List<String> args = Arrays.asList(
                 aH.buildArgs(
                         TagManager.tag(null, null,
                                 (event.getCommand().split(" ").length > 1 ? event.getCommand().split(" ", 2)[1] : ""))));
 
         dList args_list = new dList(args);
 
         String command = event.getCommand().split(" ")[0].replace("/", "").toUpperCase();
 
         // Fill context
         context.put("args", args_list);
         context.put("command", new Element(command));
         context.put("raw_args", new Element((event.getCommand().split(" ").length > 1 ? event.getCommand().split(" ", 2)[1] : "")));
         context.put("server", Element.TRUE);
 
         doEvents(Arrays.asList("command",
                 command + " command"),
                 null, null, context);
     }
 
 
     /////////////////////
     //   VEHICLE EVENTS
     /////////////////
 
     @EventHandler
     public void vehicleDamage(VehicleDamageEvent event) {
 
         Map<String, dObject> context = new HashMap<String, dObject>();
 
         Entity entity = event.getAttacker();
         Vehicle vehicle = event.getVehicle();
 
         if (entity == null || vehicle == null)
             return;
 
         String entityType = entity.getType().name();
         String vehicleType = vehicle.getType().name();
 
         Player player = null;
 
         context.put("damage", new Element(event.getDamage()));
         context.put("vehicle", new dEntity(vehicle));
 
         if (entity instanceof Player) {
             context.put("entity", new dPlayer((Player) entity));
             player = (Player) entity;
         }
         else {
             context.put("entity", new dEntity(entity));
         }
 
         String determination = doEvents(Arrays.asList
                 ("entity damages vehicle",
                         entityType + " damages vehicle",
                         "entity damages " + vehicleType,
                         entityType + " damages " + vehicleType),
                 null, player, context);
 
         if (determination.toUpperCase().startsWith("CANCELLED"))
             event.setCancelled(true);
 
         else if (Argument.valueOf(determination)
                 .matchesPrimitive(aH.PrimitiveType.Double)) {
             event.setDamage(aH.getDoubleFrom(determination));
         }
     }
 
     @EventHandler
     public void vehicleDestroy(VehicleDestroyEvent event) {
 
         Map<String, dObject> context = new HashMap<String, dObject>();
 
         Entity entity = event.getAttacker();
         Vehicle vehicle = event.getVehicle();
 
         if (entity == null || vehicle == null)
             return;
 
         String entityType = entity.getType().name();
         String vehicleType = vehicle.getType().name();
 
         Player player = null;
 
         context.put("vehicle", new dEntity(vehicle));
 
         if (entity instanceof Player) {
             context.put("entity", new dPlayer((Player) entity));
             player = (Player) entity;
         }
         else {
             context.put("entity", new dEntity(entity));
         }
 
         String determination = doEvents(Arrays.asList
                 ("entity destroys vehicle",
                         entityType + " destroys vehicle",
                         "entity destroys " + vehicleType,
                         entityType + " destroys " + vehicleType),
                 null, player, context);
 
         if (determination.toUpperCase().startsWith("CANCELLED"))
             event.setCancelled(true);
     }
 
     @EventHandler
     public void vehicleEnter(VehicleEnterEvent event) {
 
         Map<String, dObject> context = new HashMap<String, dObject>();
 
         Entity entity = event.getEntered();
         Vehicle vehicle = event.getVehicle();
 
         if (entity == null || vehicle == null)
             return;
 
         String entityType = entity.getType().name();
         String vehicleType = vehicle.getType().name();
 
         Player player = null;
 
         context.put("vehicle", new dEntity(vehicle));
 
         if (entity instanceof Player) {
             context.put("entity", new dPlayer((Player) entity));
             player = (Player) entity;
         }
         else {
             context.put("entity", new dEntity(entity));
         }
 
         String determination = doEvents(Arrays.asList
                 ("entity enters vehicle",
                         entityType + " enters vehicle",
                         "entity enters " + vehicleType,
                         entityType + " enters " + vehicleType),
                 null, player, context);
 
         if (determination.toUpperCase().startsWith("CANCELLED"))
             event.setCancelled(true);
     }
 
     @EventHandler
     public void vehicleExit(VehicleExitEvent event) {
 
         Map<String, dObject> context = new HashMap<String, dObject>();
 
         Entity entity = event.getExited();
         Vehicle vehicle = event.getVehicle();
 
         if (entity == null || vehicle == null)
             return;
 
         String entityType = entity.getType().name();
         String vehicleType = vehicle.getType().name();
 
         Player player = null;
 
         context.put("vehicle", new dEntity(vehicle));
 
         if (entity instanceof Player) {
             context.put("entity", new dPlayer((Player) entity));
             player = (Player) entity;
         }
         else {
             context.put("entity", new dEntity(entity));
         }
 
         String determination = doEvents(Arrays.asList
                 ("entity exits vehicle",
                         entityType + " exits vehicle",
                         "entity exits " + vehicleType,
                         entityType + " exits " + vehicleType),
                 null, player, context);
 
         if (determination.toUpperCase().startsWith("CANCELLED"))
             event.setCancelled(true);
     }
 
 
     /////////////////////
     //   WEATHER EVENTS
     /////////////////
 
     @EventHandler
     public void lightningStrike(LightningStrikeEvent event) {
 
         Map<String, dObject> context = new HashMap<String, dObject>();
         String world = event.getWorld().getName();
         context.put("world", new dWorld(event.getWorld()));
         context.put("location", new dLocation(event.getLightning().getLocation()));
 
         String determination = doEvents(Arrays.asList
                 ("lightning strikes",
                         "lightning strikes in " + world),
                 null, null, context);
 
         if (determination.toUpperCase().startsWith("CANCELLED"))
             event.setCancelled(true);
     }
 
     @EventHandler
     public void weatherChange(WeatherChangeEvent event) {
 
         Map<String, dObject> context = new HashMap<String, dObject>();
         String world = event.getWorld().getName();
         context.put("world", new dWorld(event.getWorld()));
 
         List<String> events = new ArrayList<String>();
         events.add("weather changes");
         events.add("weather changes in " + world);
 
         if (event.toWeatherState()) {
             context.put("weather", new Element("rain"));
             events.add("weather rains");
             events.add("weather rains in " + world);
         }
         else {
             context.put("weather", new Element("clear"));
             events.add("weather clears");
             events.add("weather clears in " + world);
         }
 
         String determination = doEvents(events, null, null, context);
 
         if (determination.toUpperCase().startsWith("CANCELLED"))
             event.setCancelled(true);
     }
 }
