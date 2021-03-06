 package anotherDEVer.RedCarpet;
 
 import org.bukkit.plugin.java.JavaPlugin;
 import org.bukkit.entity.Player;
import org.bukkit.DyeColor;
 import org.bukkit.Location;
 import org.bukkit.event.*;
 import org.bukkit.event.player.*;
 import org.bukkit.block.*;
 import org.bukkit.World;
 
 public final class RedCarpet extends JavaPlugin implements Listener
 {
 	public void onEnable()
 	{
 		getServer().getPluginManager().registerEvents(this, this);
 	}
 	
	@SuppressWarnings("deprecation")
 	@EventHandler
 	public void PlayerMovement(PlayerMoveEvent move)
 	{
 		Player mover = move.getPlayer();
 		
 		Location playerLoc = mover.getLocation();
 		
 		World currentWorld = mover.getWorld();
 		
		double dubX = playerLoc.getX();
		double dubY = playerLoc.getY();
		double dubZ = playerLoc.getZ();
		
		int x = (int) Math.floor(dubX);
		int y = (int) Math.floor(dubY);
		int z = (int) Math.floor(dubZ);
 		
 		Block changeBlock = currentWorld.getBlockAt(x, y - 1, z);
 		
		changeBlock.setTypeIdAndData(35, DyeColor.RED.getData(), true);
 	}
 }
