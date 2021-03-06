 package de.codeinfection.quickwango.AntiGuest;
 
 import java.util.HashMap;
 import org.bukkit.Material;
 import org.bukkit.block.Block;
 import org.bukkit.entity.Player;
 import org.bukkit.event.block.Action;
 import org.bukkit.event.player.PlayerBedEnterEvent;
 import org.bukkit.event.player.PlayerBucketEmptyEvent;
 import org.bukkit.event.player.PlayerBucketFillEvent;
 import org.bukkit.event.player.PlayerChatEvent;
 import org.bukkit.event.player.PlayerDropItemEvent;
 import org.bukkit.event.player.PlayerFishEvent;
 import org.bukkit.event.player.PlayerInteractEvent;
 import org.bukkit.event.player.PlayerListener;
 import org.bukkit.event.player.PlayerPickupItemEvent;
 
 /**
  *
  * @author CodeInfection
  */
 public class AntiGuestPlayerListener extends PlayerListener
 {
     protected final AntiGuest plugin;
     protected final HashMap<Player, Long> chatTimestamps;
     protected final HashMap<Player, Long> pickupTimestamps;
     protected final HashMap<Player, Long> pressureTimestamps;
 
     protected final boolean lever;
     protected final boolean button;
     protected final boolean door;
     protected final boolean pressureplate;
     protected final boolean chest;
     protected final boolean workbench;
     protected final boolean furnace;
     protected final boolean dispenser;
     protected final boolean placeblock;
     protected final boolean cake;
    protected final boolean chat;
    protected final boolean spam;
 
 
     public AntiGuestPlayerListener(AntiGuest plugin)
     {
         this.plugin = plugin;
         this.chatTimestamps = new HashMap<Player, Long>();
         this.pickupTimestamps = new HashMap<Player, Long>();
         this.pressureTimestamps = new HashMap<Player, Long>();
 
         this.lever = this.plugin.preventions.get("lever");
         this.button = this.plugin.preventions.get("button");
         this.door = this.plugin.preventions.get("door");
         this.pressureplate = this.plugin.preventions.get("pressureplate");
         this.chest = this.plugin.preventions.get("chest");
         this.workbench = this.plugin.preventions.get("workbench");
         this.furnace = this.plugin.preventions.get("furnace");
         this.dispenser = this.plugin.preventions.get("dispenser");
         this.placeblock = this.plugin.preventions.get("placeblock");
         this.cake = this.plugin.preventions.get("cake");
        this.chat = this.plugin.preventions.get("chat");
        this.spam = this.plugin.preventions.get("spam");
     }
 
     protected void noPickupMessage(Player player)
     {
         Long lastTime = this.pickupTimestamps.get(player);
         long currentTime = System.currentTimeMillis();
         if (lastTime == null || lastTime + AntiGuest.messageWaitTime < currentTime)
         {
             this.plugin.message(player, "pickup");
             this.pickupTimestamps.put(player, currentTime);
         }
     }
 
     protected void pressureMessage(Player player)
     {
         Long lastTime = this.pressureTimestamps.get(player);
         long currentTime = System.currentTimeMillis();
         if (lastTime == null || lastTime + AntiGuest.messageWaitTime < currentTime)
         {
             this.plugin.message(player, "pressureplate");
             this.pressureTimestamps.put(player, currentTime);
         }
     }
 
     protected boolean isPlayerChatLocked(Player player)
     {
         if (this.plugin.can(player, "spam"))
         {
             return false;
         }
         else
         {
             Long lastTime = this.chatTimestamps.get(player);
             long currentTime = System.currentTimeMillis();
             if (lastTime == null || lastTime + (this.plugin.chatLockDuration * 1000) < currentTime)
             {
                 this.chatTimestamps.put(player, currentTime);
                 return false;
             }
             else
             {
                 return true;
             }
         }
     }
 
     @Override
     public void onPlayerInteract(PlayerInteractEvent event)
     {
         final Player player = event.getPlayer();
         Action action = event.getAction();
         Block block = event.getClickedBlock();
         if (block == null)
         {
             return;
         }
         Material material = block.getType();
         Material itemInHand = player.getItemInHand().getType();
         AntiGuest.debug("Player interacted with " + material.toString());
         if (action == Action.RIGHT_CLICK_BLOCK || action == Action.LEFT_CLICK_BLOCK)
         {
             if (this.door && (material == Material.WOODEN_DOOR || material == Material.IRON_DOOR || material == Material.TRAP_DOOR)) // doors
             {
                 if (!this.plugin.can(player, "door"))
                 {
                     this.plugin.message(player, "door");
                     event.setCancelled(true);
                     return;
                 }
             }
             else if (this.lever && material == Material.LEVER) // lever
             {
                 if (!this.plugin.can(player, "lever"))
                 {
                     this.plugin.message(player, "lever");
                     event.setCancelled(true);
                     return;
                 }
             }
             else if (this.button && material == Material.STONE_BUTTON) // buttons
             {
                 if (!this.plugin.can(player, "button"))
                 {
                     this.plugin.message(player, "button");
                     event.setCancelled(true);
                     return;
                 }
             }
         }
         if (action == Action.RIGHT_CLICK_BLOCK)
         {
             if (this.chest && material == Material.CHEST) // chests
             {
                 if (!this.plugin.can(player, "chest"))
                 {
                     this.plugin.message(player, "chest");
                     event.setCancelled(true);
                     return;
                 }
             }
             else if (this.workbench && material == Material.WORKBENCH) // workbenches
             {
                 if (!this.plugin.can(player, "workbench"))
                 {
                     this.plugin.message(player, "workbench");
                     event.setCancelled(true);
                     return;
                 }
             }
             else if (this.furnace && material == Material.FURNACE) // furnaces
             {
                 if (!this.plugin.can(player, "furnace"))
                 {
                     this.plugin.message(player, "furnace");
                     event.setCancelled(true);
                     return;
                 }
             }
             else if (this.dispenser && material == Material.DISPENSER) // dispensers
             {
                 if (!this.plugin.can(player, "dispenser"))
                 {
                     this.plugin.message(player, "dispenser");
                     event.setCancelled(true);
                     return;
                 }
             }
             else if (this.cake && material == Material.CAKE_BLOCK) // cakes
             {
                 if (!this.plugin.can(player, "cake"))
                 {
                     this.plugin.message(player, "cake");
                     event.setCancelled(true);
                     return;
                 }
             }
             if (this.placeblock)
             {
                 boolean allowed = this.plugin.can(player, "placeblock");
                 if ((itemInHand == Material.MINECART || itemInHand == Material.STORAGE_MINECART))
                 {
                     if ((material == Material.RAILS || material == Material.POWERED_RAIL || material == Material.DETECTOR_RAIL) && !allowed)
                     {
                         event.setCancelled(true);
                         this.plugin.message(player, "placeblock");
                         return;
                     }
                 }
                 else if (itemInHand == Material.BOAT && !allowed)
                 {
                     event.setCancelled(true);
                     this.plugin.message(player, "placeblock");
                     return;
                 }
             }
         }
         else if (this.pressureplate && action == Action.PHYSICAL)
         {
             if (material == Material.WOOD_PLATE || material == Material.STONE_PLATE) // pressure plates
             {
                 if (!this.plugin.can(player, "pressureplate"))
                 {
                     this.pressureMessage(player);
                     event.setCancelled(true);
                     return;
                 }
             }
         }
     }
 
     @Override
     public void onPlayerPickupItem(PlayerPickupItemEvent event)
     {
         final Player player = event.getPlayer();
         if (!this.plugin.can(player, "pickup"))
         {
             event.setCancelled(true);
             this.noPickupMessage(player);
         }
     }
 
     @Override
     public void onPlayerChat(PlayerChatEvent event)
     {
         final Player player = event.getPlayer();
        if (this.chat && !this.plugin.can(player, "chat"))
         {
             event.setCancelled(true);
             this.plugin.message(player, "chat");
         }
        else if (this.spam && this.isPlayerChatLocked(player))
         {
             event.setCancelled(true);
             this.plugin.message(player, "spam");
         }
     }
 
     @Override
     public void onPlayerBucketFill(PlayerBucketFillEvent event)
     {
         final Player player = event.getPlayer();
         if (!this.plugin.can(player, "bucket"))
         {
             event.setCancelled(true);
             this.plugin.message(player, "bucket");
         }
     }
 
     @Override
     public void onPlayerBucketEmpty(PlayerBucketEmptyEvent event)
     {
         final Player player = event.getPlayer();
         if (!this.plugin.can(player, "bucket"))
         {
             event.setCancelled(true);
             this.plugin.message(player, "bucket");
         }
     }
 
     @Override
     public void onPlayerDropItem(PlayerDropItemEvent event)
     {
         final Player player = event.getPlayer();
         if (!this.plugin.can(player, "drop"))
         {
             event.setCancelled(true);
             this.plugin.message(player, "drop");
         }
     }
 
     @Override
     public void onPlayerBedEnter(PlayerBedEnterEvent event)
     {
         final Player player = event.getPlayer();
         if (!this.plugin.can(player, "bed"))
         {
             event.setCancelled(true);
             this.plugin.message(player, "bed");
         }
     }
 
     @Override
     public void onPlayerFish(PlayerFishEvent event)
     {
         final Player player = event.getPlayer();
         if (!this.plugin.can(player, "fish"))
         {
             event.setCancelled(true);
             this.plugin.message(player, "fish");
         }
     }
 }
