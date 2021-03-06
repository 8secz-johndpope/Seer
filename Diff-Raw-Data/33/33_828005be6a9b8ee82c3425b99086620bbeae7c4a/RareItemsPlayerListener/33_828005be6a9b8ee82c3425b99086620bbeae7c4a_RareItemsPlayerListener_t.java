 package com.gmail.ne0nx3r0.rareitems;
 
 import com.gmail.ne0nx3r0.rareitems.inventory.VirtualChest;
 import com.gmail.ne0nx3r0.rareitems.item.RareItem;
 import org.bukkit.Material;
 import org.bukkit.entity.Player;
 import org.bukkit.event.EventHandler;
 import org.bukkit.event.EventPriority;
 import org.bukkit.event.Listener;
 import org.bukkit.event.block.Action;
 import org.bukkit.event.entity.EntityDamageByEntityEvent;
 import org.bukkit.event.entity.EntityDamageEvent;
 import org.bukkit.event.entity.EntityDeathEvent;
 import org.bukkit.event.inventory.InventoryClickEvent;
 import org.bukkit.event.inventory.InventoryType.SlotType;
 import org.bukkit.event.player.PlayerChangedWorldEvent;
 import org.bukkit.event.player.PlayerInteractEvent;
 import org.bukkit.event.player.PlayerJoinEvent;
 import org.bukkit.event.player.PlayerPickupItemEvent;
 import org.bukkit.event.player.PlayerQuitEvent;
 import org.bukkit.inventory.ItemStack;
 
 public class RareItemsPlayerListener implements Listener
 {
     
     @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
     public void onEntityDeath(EntityDeathEvent e)
     {
         if(e.getEntity() instanceof Player)
         {
             String sPlayerName = ((Player) e.getEntity()).getName();
             for(ItemStack isDrop : e.getDrops())
             {
                 if(RareItems.rig.getRareItem(sPlayerName, isDrop) != null)
                 {
                     e.getDrops().remove(isDrop);
                 }
             }
         }
     }
     
     
     @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
     public void onPlayerInteract(PlayerInteractEvent e)
     {
         if((e.getAction() == Action.LEFT_CLICK_AIR || e.getAction() == Action.LEFT_CLICK_BLOCK)
         && e.getPlayer().getItemInHand().getType() != Material.AIR)
         {
             RareItem ri = RareItems.rig.getRareItem(e.getPlayer().getName(),e.getPlayer().getItemInHand());
 
             if(ri != null)
             {
                 ri.onInteract(e);
             }
         }
     }
     
     @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
     public void onPlayerInventoryClick(InventoryClickEvent e)
     {
         if(e.getSlotType() == SlotType.ARMOR)
         {
             if(e.getCursor() != null && e.getCursor().getType() != Material.AIR)//equipped item
             {
                 RareItem riEquipped = RareItems.rig.getRareItem(((Player) e.getWhoClicked()).getName(),e.getCursor());
                 
                 if(riEquipped != null)
                 {
                     riEquipped.onEquipped(((Player) e.getWhoClicked()));
                 }
             }
             if(e.getCurrentItem().getType() != Material.AIR)//unequipped item
             {                
                 RareItem riUnequipped = RareItems.rig.getRareItem(((Player) e.getWhoClicked()).getName(),e.getCurrentItem());
                 
                 if(riUnequipped != null)
                 {
                     riUnequipped.onUnequipped(((Player) e.getWhoClicked()));
                 }
             }
         }
         
         if(e.getInventory().getHolder() instanceof VirtualChest)
         {
             Player p = (Player) e.getWhoClicked();
             
             if(e.getRawSlot() < 54)
             {
                 if(e.getCursor() != null && e.getCursor().getType() != Material.AIR)//putting down
                 {
                    RareItem riPuttingDown = RareItems.rig.getRareItem(((Player) e.getWhoClicked()).getName(),e.getCursor(),true);
 
                     if(riPuttingDown == null 
                    || !riPuttingDown.getOwner().equalsIgnoreCase(p.getName()))
                     {
                         p.sendMessage("You can only check in your own RareItems!");
 
                         e.setCancelled(true);
 
                         return;
                     }
                     else
                     {
                         if(!RareItems.vcm.checkIn(riPuttingDown.getId(),p))
                         {
                             p.sendMessage("You cannot check in that item");
 
                             e.setCancelled(true);
 
                             return;
                         }
                     }
                      
                 }
 
                 if(e.getCurrentItem() != null && e.getCurrentItem().getType() != Material.AIR)//picking up
                 {
                    RareItem riPickingUp = RareItems.rig.getRareItem((p).getName(),e.getCurrentItem(),true);
 
                     if(riPickingUp == null 
                    || !riPickingUp.getOwner().equalsIgnoreCase(p.getName()))
                     {
                         p.sendMessage("You can only check out your own RareItems!");
 
                         e.setCancelled(true);
 
                         return;
                     }
                     else
                     {
                         if(!RareItems.vcm.checkOut(riPickingUp,p))
                         {
                             p.sendMessage("You cannot check out that item");
 
                             e.setCancelled(true);
 
                             return;
                         }
                     }
                 }  
             }
         }
     }
     /*
     @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
     public void onPlayerItemHeldChange(PlayerItemHeldEvent e)
     {
         RareItem riHeld = RareItems.rig.getRareItem(e.getPlayer().getName(),e
                 .getPlayer().getInventory().getItem(e.getNewSlot()));
         if(riHeld != null)
         {
             riHeld.onHeld(e);
         }
         
         RareItem riUnheld = RareItems.rig.getRareItem(e.getPlayer().getName(),e
                 .getPlayer().getInventory().getItem(e.getPreviousSlot()));
         if(riUnheld != null)
         {
             riUnheld.onUnheld(e);
         }
     }
     */
     
     @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
     public void onPlayerItemPickUp(PlayerPickupItemEvent e)
     {
         if(RareItems.rig.isAnotherPlayersRareItem(e.getItem().getItemStack(),e.getPlayer().getName()))
         {
             e.getItem().setItemStack(new ItemStack(e.getItem().getItemStack().getType()));
         }
     }
     
     @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
     public void onEntityDamagedByEntity(EntityDamageByEntityEvent e)
     {
          if(e.getDamager() instanceof Player)
          {
             Player attacker = (Player) e.getDamager();
              
             //Strength Ability
             if(RareItems.rig.playerHasItemProperty(attacker.getName(),5))//Strength
             {
                 e.setDamage(e.getDamage()
                     +RareItems.rig.getPlayerEffectLevel(attacker.getName(),5));//Strength
             }
             
             RareItem ri = RareItems.rig.getRareItem(attacker.getName(), attacker.getItemInHand());
             
             if(ri != null)
             {
                 ri.onDamagedOther(e);
             }
          }
     }
 
     @EventHandler(priority=EventPriority.NORMAL, ignoreCancelled=true)
     public void onEntityDamaged(EntityDamageEvent e)
     {
         if(e.getEntity() instanceof Player)
         {
            if(e.getCause().equals(EntityDamageEvent.DamageCause.DROWNING))
            {
                if(RareItems.rig.playerHasItemProperty(((Player) e.getEntity()).getName(),7))//Water Breathing
                {
                    e.setCancelled(true);
                }
            }
            else if(RareItems.rig.playerHasItemProperty(((Player) e.getEntity()).getName(),5))//Hardy
            {
                e.setDamage(e.getDamage()-1);
            }
         }
     }
     
     @EventHandler(priority=EventPriority.LOWEST, ignoreCancelled=true)
     public void onPlayerJoin(PlayerChangedWorldEvent e)
     {
         
     }
      
     
     @EventHandler(priority=EventPriority.LOWEST, ignoreCancelled=true)
     public void onPlayerJoin(PlayerJoinEvent e)
     {
         RareItems.am.fetchPlayerRareItems(e.getPlayer().getName());
     }
 
     @EventHandler(priority=EventPriority.NORMAL, ignoreCancelled=true)
     public void onPlayerQuit(PlayerQuitEvent e)
     {
         RareItems.am.removeSiteId(e.getPlayer().getName());
         
         RareItems.rig.revokeAllItemProperties(e.getPlayer().getName());
     }
 }
