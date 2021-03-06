 package me.eccentric_nz.plugins.TARDIS;
 
 import java.sql.Connection;
 import java.sql.ResultSet;
 import java.sql.SQLException;
 import java.sql.Statement;
 import java.util.ArrayList;
 import java.util.List;
 import org.bukkit.block.Block;
 import org.bukkit.block.BlockFace;
 import org.bukkit.event.EventHandler;
 import org.bukkit.event.EventPriority;
 import org.bukkit.event.Listener;
 import org.bukkit.event.block.BlockBurnEvent;
 import org.bukkit.event.block.BlockIgniteEvent;
 
 public class TARDISBlockProtectListener implements Listener {
 
     private TARDIS plugin;
     TARDISDatabase service = TARDISDatabase.getInstance();
     List<BlockFace> faces = new ArrayList<BlockFace>();
 
     public TARDISBlockProtectListener(TARDIS plugin) {
         this.plugin = plugin;
         faces.add(BlockFace.UP);
         faces.add(BlockFace.DOWN);
         faces.add(BlockFace.NORTH);
         faces.add(BlockFace.SOUTH);
         faces.add(BlockFace.EAST);
         faces.add(BlockFace.WEST);
     }
 
     @EventHandler(priority = EventPriority.LOWEST)
     public void onBlockIgnite(BlockIgniteEvent event) {
         Block b = event.getBlock();
         try {
             Connection connection = service.getConnection();
             Statement statement = connection.createStatement();
             for (BlockFace bf : faces) {
                 Block chkBlock = b.getRelative(bf);
                 String l = chkBlock.getLocation().toString();
                 String queryBlock = "SELECT location FROM blocks WHERE location = '" + l + "'";
                 ResultSet rsBlockLoc = statement.executeQuery(queryBlock);
                 if (rsBlockLoc.next()) {
                     event.setCancelled(true);
                     break;
                 }
             }
         } catch (SQLException e) {
            System.err.println(Constants.MY_PLUGIN_NAME + " Could not get block locations from DB!");
         }
         if (plugin.config.getBoolean("protect_blocks") == true) {
             String[] set = {"EAST", "SOUTH", "WEST", "NORTH", "UP", "DOWN"};
             for (String f : set) {
                 int id = b.getRelative(BlockFace.valueOf(f)).getTypeId();
                 byte d = b.getRelative(BlockFace.valueOf(f)).getData();
                 if (id == 35 && (d == 1 || d == 7 || d == 8)) {
                     event.setCancelled(true);
                     break;
                 }
             }
         }
     }
 
     @EventHandler(priority = EventPriority.LOWEST)
     public void onBlockBurn(BlockBurnEvent event) {
         Block b = event.getBlock();
         String l = b.getLocation().toString();
         try {
             Connection connection = service.getConnection();
             Statement statement = connection.createStatement();
             String queryBlock = "SELECT location FROM blocks WHERE location = '" + l + "'";
             ResultSet rsBlockLoc = statement.executeQuery(queryBlock);
             if (rsBlockLoc.next()) {
                 event.setCancelled(true);
             }
         } catch (SQLException e) {
            System.err.println(Constants.MY_PLUGIN_NAME + " Could not get block locations from DB!");
         }
 
         if (plugin.config.getBoolean("protect_blocks") == true) {
             for (BlockFace bf : faces) {
                 int id = b.getRelative(bf).getTypeId();
                 byte d = b.getRelative(bf).getData();
                 if (id == 35 && (d == 1 || d == 7 || d == 8)) {
                     event.setCancelled(true);
                     break;
                 }
             }
         }
     }
 }
