 /*
  * Modifyworld - PermissionsEx ruleset plugin for Bukkit
  * Copyright (C) 2011 t3hk0d3 http://www.tehkode.ru
  *
  * This program is free software; you can redistribute it and/or
  * modify it under the terms of the GNU General Public License
  * as published by the Free Software Foundation; either version 2
  * of the License, or (at your option) any later version.
  *
  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  * GNU General Public License for more details.
  *
  * You should have received a copy of the GNU General Public License
  * along with this program; if not, write to the Free Software
  * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
  */
 package ru.tehkode.modifyworld.handlers;
 
 import org.bukkit.entity.Entity;
 import org.bukkit.entity.Player;
 import org.bukkit.event.Event.Type;
 import org.bukkit.event.entity.*;
 import org.bukkit.plugin.Plugin;
 import org.bukkit.util.config.ConfigurationNode;
 import ru.tehkode.modifyworld.EventHandler;
 import ru.tehkode.modifyworld.ModifyworldListener;
 import ru.tehkode.permissions.PermissionGroup;
 import ru.tehkode.permissions.PermissionUser;
 
 /**
  *
  * @author t3hk0d3
  */
 public class EntityListener extends ModifyworldListener {
 
     public EntityListener(Plugin plugin, ConfigurationNode config) {
         super(plugin, config);
     }
 
     protected boolean canMessWithEntity(Player player, String basePermission, Entity entity) {
         if (entity instanceof Player) {
             PermissionUser entityUser = permissionsManager.getUser(((Player) entity).getName());
 
             if (entityUser == null) {
                 return false;
             }
 
             for (PermissionGroup group : entityUser.getGroups()) {
                 if (permissionsManager.has(player, basePermission + "group." + group.getName())) {
                     return true;
                 }
             }
 
             return permissionsManager.has(player, basePermission + "player." + entityUser.getName());
         }
 
         return permissionsManager.has(player, basePermission + getEntityName(entity));
     }
 
     @EventHandler(Type.ENTITY_DAMAGE)
     public void onEntityDamage(EntityDamageEvent event) {
         if (event instanceof EntityDamageByEntityEvent) {
             EntityDamageByEntityEvent edbe = (EntityDamageByEntityEvent) event;
 
             Player player;
             if (edbe.getDamager() instanceof Player) { // Prevent from damaging by player
                 player = (Player) edbe.getDamager();
                 if (!canMessWithEntity(player, "modifyworld.damage.deal.", event.getEntity())) {
                     cancelDamageEvent(player, event);
                     return;
                 }
             }
 
            player = (Player) edbe.getEntity();
            if (edbe.getEntity() instanceof Player && edbe.getDamager() != null) { // Prevent from taking damage by player
                if (!canMessWithEntity(player, "modifyworld.damage.take.", edbe.getDamager())) {
                    cancelDamageEvent(player, event);
                    return;
                 }
            }
            
            if (edbe instanceof EntityDamageByProjectileEvent && edbe.getDamager() == null){
                EntityDamageByProjectileEvent edpe = (EntityDamageByProjectileEvent)edbe;
                
                if (!canMessWithEntity(player, "modifyworld.damage.take.", edpe.getProjectile())) {
                    cancelDamageEvent(player, event);
                    return;
                 }
                
                
             }
         } else if (event.getEntity() instanceof Player) { // player are been damaged by enviroment
             Player player = (Player) event.getEntity();
 
             String cause = event.getCause().name().toLowerCase().replace("_", "");
 
             if (!permissionsManager.has(player, "modifyworld.damage.take." + cause)) {
                 cancelDamageEvent(player, event);
                 return;
             }
         }
     }
 
     protected void cancelDamageEvent(Player player, EntityDamageEvent event) {
         informPlayerAboutDenial(player);
         event.setCancelled(true);
         event.setDamage(0);
     }
 
     @EventHandler(Type.ENTITY_TAME)
     public void onEntityTame(EntityTameEvent event) {
         if (!(event.getOwner() instanceof Player)) {
             return;
         }
 
         Player player = (Player) event.getOwner();
 
         if (!permissionsManager.has(player, "modifyworld.tame." + getEntityName(event.getEntity()))) {
             event.setCancelled(true);
             informPlayerAboutDenial(player);
         }
     }
 
     @EventHandler(Type.ENTITY_TARGET)
     public void onEntityTarget(EntityTargetEvent event) {
         if (event.getTarget() instanceof Player) {
             Player player = (Player) event.getTarget();
             if (!permissionsManager.has(player, "modifyworld.mobtarget." + getEntityName(event.getEntity()))) {
                 event.setCancelled(true);
             }
         }
     }
 }
