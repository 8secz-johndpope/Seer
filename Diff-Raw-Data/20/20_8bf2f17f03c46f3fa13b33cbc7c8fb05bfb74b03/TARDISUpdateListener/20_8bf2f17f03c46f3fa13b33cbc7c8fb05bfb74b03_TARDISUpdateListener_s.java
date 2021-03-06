 /*
  * Copyright (C) 2013 eccentric_nz
  *
  * This program is free software: you can redistribute it and/or modify
  * it under the terms of the GNU General Public License as published by
  * the Free Software Foundation, either version 3 of the License, or
  * (at your option) any later version.
  *
  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
  * GNU General Public License for more details.
  *
  * You should have received a copy of the GNU General Public License
  * along with this program. If not, see <http://www.gnu.org/licenses/>.
  */
 package me.eccentric_nz.TARDIS.listeners;
 
 import java.util.ArrayList;
 import java.util.HashMap;
 import java.util.List;
 import me.eccentric_nz.TARDIS.TARDIS;
 import me.eccentric_nz.TARDIS.TARDISConstants;
 import me.eccentric_nz.TARDIS.database.QueryFactory;
 import me.eccentric_nz.TARDIS.database.ResultSetDoors;
 import me.eccentric_nz.TARDIS.database.ResultSetTardis;
 import me.eccentric_nz.TARDIS.database.ResultSetTravellers;
 import me.eccentric_nz.TARDIS.thirdparty.Version;
 import org.bukkit.Bukkit;
 import org.bukkit.ChatColor;
 import org.bukkit.Location;
 import org.bukkit.Material;
 import org.bukkit.World;
 import org.bukkit.block.Block;
 import org.bukkit.block.BlockFace;
 import org.bukkit.block.Sign;
 import org.bukkit.entity.Player;
 import org.bukkit.event.EventHandler;
 import org.bukkit.event.EventPriority;
 import org.bukkit.event.Listener;
 import org.bukkit.event.player.PlayerInteractEvent;
 
 /**
  * The TARDIS interior goes through occasional metamorphoses, sometimes by
  * choice, sometimes for other reasons, such as the Doctor's own regeneration.
  * Some of these changes were physical in nature (involving secondary control
  * rooms, etc.), but it was also possible to re-arrange the interior design of
  * the TARDIS with ease, using the Architectural Configuration system.
  *
  * @author eccentric_nz
  */
 public class TARDISUpdateListener implements Listener {
 
     private TARDIS plugin;
     List<Material> validBlocks = new ArrayList<Material>();
     HashMap<String, Integer> controls = new HashMap<String, Integer>();
     Version bukkitversion;
     Version prewoodbuttonversion = new Version("1.4.2");
 
     public TARDISUpdateListener(TARDIS plugin) {
         this.plugin = plugin;
         String[] v = Bukkit.getServer().getBukkitVersion().split("-");
         bukkitversion = (!v[0].equalsIgnoreCase("unknown")) ? new Version(v[0]) : new Version("1.4.7");
         if (bukkitversion.compareTo(prewoodbuttonversion) >= 0) {
             validBlocks.add(Material.WOOD_BUTTON);
         }
         validBlocks.add(Material.STONE_BUTTON);
         controls.put("handbrake", 0);
         controls.put("button", 1);
        controls.put("x-repeater", 2);
        controls.put("y-repeater", 3);
         controls.put("z-repeater", 4);
        controls.put("world-repeater", 5);
         controls.put("artron", 6);
     }
 
     /**
      * Listens for player interaction with the TARDIS console and other specific
      * items. If the block is clicked and players name is contained in the
      * appropriate HashMap, then the blocks position is recorded in the
      * database.
      *
      * @param event a player clicking on a block
      */
     @EventHandler(priority = EventPriority.MONITOR)
     public void onUpdateInteract(PlayerInteractEvent event) {
         final Player player = event.getPlayer();
         final String playerNameStr = player.getName();
         String blockName;
         boolean secondary = false;
         if (plugin.trackPlayers.containsKey(playerNameStr)) {
             blockName = plugin.trackPlayers.get(playerNameStr);
         } else if (plugin.trackSecondary.containsKey(playerNameStr)) {
             blockName = plugin.trackSecondary.get(playerNameStr);
             secondary = true;
         } else {
             return;
         }
         Block block = event.getClickedBlock();
         if (block != null) {
             Material blockType = block.getType();
             Location block_loc = block.getLocation();
             World bw = block_loc.getWorld();
             int bx = block_loc.getBlockX();
             int by = block_loc.getBlockY();
             int bz = block_loc.getBlockZ();
             byte blockData = block.getData();
             if (blockData >= 8 && blockType == Material.IRON_DOOR_BLOCK) {
                 by = (by - 1);
                 blockData = block.getRelative(BlockFace.DOWN).getData();
             }
             HashMap<String, Object> where = new HashMap<String, Object>();
             where.put("owner", playerNameStr);
             ResultSetTardis rs = new ResultSetTardis(plugin, where, "", false);
             if (!rs.resultSet()) {
                 player.sendMessage(plugin.pluginName + TARDISConstants.NO_TARDIS);
                 return;
             }
             int id = rs.getTardis_id();
             String home = rs.getHome();
             QueryFactory qf = new QueryFactory(plugin);
             String table = "tardis";
             HashMap<String, Object> tid = new HashMap<String, Object>();
             HashMap<String, Object> set = new HashMap<String, Object>();
             tid.put("tardis_id", id);
             String blockLocStr = bw.getName() + ":" + bx + ":" + by + ":" + bz;
             if (controls.containsKey(blockName)) {
                 if (!blockName.contains("repeater")) {
                     blockLocStr = plugin.utils.makeLocationStr(bw, bx, by, bz);
                 }
                 table = "controls";
                 tid.put("type", controls.get(blockName));
                 tid.put("secondary", 0);
             }
             if (secondary) {
                 plugin.trackSecondary.remove(playerNameStr);
             } else {
                 plugin.trackPlayers.remove(playerNameStr);
             }
             if (blockName.equalsIgnoreCase("door") && blockType == Material.IRON_DOOR_BLOCK) {
                 // get door data this should let us determine the direction
                 String d = getDirection(blockData);
                 table = "doors";
                 set.put("door_location", blockLocStr);
                 set.put("door_direction", d);
                 tid.put("door_type", 1);
             }
             if (blockName.equalsIgnoreCase("backdoor") && blockType == Material.IRON_DOOR_BLOCK) {
                 // get door data - this should let us determine the direction
                 String d = plugin.utils.getPlayersDirection(player, true);
                 table = "doors";
                 set.put("door_location", blockLocStr);
                 set.put("door_direction", d);
                 HashMap<String, Object> wheret = new HashMap<String, Object>();
                 wheret.put("tardis_id", id);
                 wheret.put("player", playerNameStr);
                 ResultSetTravellers rst = new ResultSetTravellers(plugin, wheret, false);
                 int type;
                 if (rst.resultSet()) {
                     type = 3;
                 } else {
                     type = 2;
                     // check the world
                     String wor = (plugin.getConfig().getBoolean("default_world")) ? plugin.getConfig().getString("default_world_name") : "TARDIS_";
                     if (bw.getName().contains(wor)) {
                         player.sendMessage(plugin.pluginName + "You didn't enter the TARDIS by the regular door, aborting...");
                         return;
                     }
                 }
                 tid.put("door_type", type);
                 // check if we have a backdoor yet
                 HashMap<String, Object> whered = new HashMap<String, Object>();
                 whered.put("tardis_id", id);
                 whered.put("door_type", type);
                 ResultSetDoors rsd = new ResultSetDoors(plugin, whered, false);
                 if (!rsd.resultSet()) {
                     // insert record
                     HashMap<String, Object> setd = new HashMap<String, Object>();
                     setd.put("tardis_id", id);
                     setd.put("door_type", type);
                     setd.put("door_location", blockLocStr);
                     setd.put("door_direction", d);
                     qf.doInsert("doors", setd);
                 }
             }
             if (blockName.equalsIgnoreCase("button") && (validBlocks.contains(blockType) || blockType == Material.LEVER)) {
                 if (secondary) {
                     qf.insertControl(id, 1, blockLocStr, 1);
                 } else {
                     set.put("location", blockLocStr);
                 }
             }
             if (blockName.equalsIgnoreCase("scanner") && (validBlocks.contains(blockType) || blockType == Material.LEVER)) {
                 set.put("scanner", blockLocStr);
             }
             if (blockName.equalsIgnoreCase("handbrake") && blockType == Material.LEVER) {
                 if (secondary) {
                     qf.insertControl(id, 0, blockLocStr, 1);
                 } else {
                     set.put("location", blockLocStr);
                 }
             }
             if (blockName.equalsIgnoreCase("condenser") && blockType == Material.CHEST) {
                 set.put("condenser", blockLocStr);
             }
             if (blockName.equalsIgnoreCase("world-repeater") && (blockType == Material.DIODE_BLOCK_OFF || blockType == Material.DIODE_BLOCK_ON)) {
                 if (secondary) {
                     qf.insertControl(id, 5, blockLocStr, 1);
                 } else {
                     set.put("location", blockLocStr);
                 }
             }
             if (blockName.equalsIgnoreCase("x-repeater") && (blockType == Material.DIODE_BLOCK_OFF || blockType == Material.DIODE_BLOCK_ON)) {
                 if (secondary) {
                     qf.insertControl(id, 2, blockLocStr, 1);
                 } else {
                     set.put("location", blockLocStr);
                 }
             }
             if (blockName.equalsIgnoreCase("z-repeater") && (blockType == Material.DIODE_BLOCK_OFF || blockType == Material.DIODE_BLOCK_ON)) {
                 if (secondary) {
                     qf.insertControl(id, 3, blockLocStr, 1);
                 } else {
                     set.put("location", blockLocStr);
                 }
             }
             if (blockName.equalsIgnoreCase("y-repeater") && (blockType == Material.DIODE_BLOCK_OFF || blockType == Material.DIODE_BLOCK_ON)) {
                 if (secondary) {
                     qf.insertControl(id, 4, blockLocStr, 1);
                 } else {
                     set.put("location", blockLocStr);
                 }
             }
             if (blockName.equalsIgnoreCase("artron") && validBlocks.contains(blockType)) {
                 if (secondary) {
                     qf.insertControl(id, 6, blockLocStr, 1);
                 } else {
                     set.put("location", blockLocStr);
                 }
             }
             if (blockName.equalsIgnoreCase("chameleon") && (blockType == Material.WALL_SIGN || blockType == Material.SIGN_POST)) {
                 set.put("chameleon", blockLocStr);
                 set.put("chamele_on", 0);
                 // add text to sign
                 Sign s = (Sign) block.getState();
                 s.setLine(0, "Chameleon");
                 s.setLine(1, "Circuit");
                 s.setLine(3, ChatColor.RED + "OFF");
                 s.update();
             }
             if (blockName.equalsIgnoreCase("save-sign") && (blockType == Material.WALL_SIGN || blockType == Material.SIGN_POST)) {
                 set.put("save_sign", blockLocStr);
                 // add text to sign
                 String[] coords = home.split(":");
                 Sign s = (Sign) block.getState();
                 s.setLine(0, "Saves");
                 s.setLine(1, "Home");
                 s.setLine(2, coords[0]);
                 s.setLine(3, coords[1] + "," + coords[2] + "," + coords[3]);
                 s.update();
             }
             if (set.size() > 0 || secondary) {
                 if (!secondary) {
                     qf.doUpdate(table, set, tid);
                 }
                 player.sendMessage(plugin.pluginName + "The position of the TARDIS " + blockName + " was updated successfully.");
             } else {
                 player.sendMessage(plugin.pluginName + "You didn't click the correct type of block for the " + blockName + "! Try the command again.");
             }
         }
     }
 
     private String getDirection(Byte blockData) {
         switch (blockData) {
             case 1:
                 return "SOUTH";
             case 2:
                 return "WEST";
             case 3:
                 return "NORTH";
             default:
                 return "EAST";
         }
     }
 }
