 package StevenDimDoors.mod_pocketDim;
 
 import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
 import net.minecraft.entity.player.EntityPlayer;
 import net.minecraft.util.ChunkCoordinates;
 import net.minecraftforge.client.event.sound.SoundLoadEvent;
 import net.minecraftforge.event.EventPriority;
 import net.minecraftforge.event.ForgeSubscribe;
 import net.minecraftforge.event.entity.living.LivingDeathEvent;
 import net.minecraftforge.event.entity.living.LivingFallEvent;
 import net.minecraftforge.event.world.WorldEvent;
 import StevenDimDoors.mod_pocketDim.core.DDTeleporter;
 import StevenDimDoors.mod_pocketDim.core.PocketManager;
 import StevenDimDoors.mod_pocketDim.ticking.RiftRegenerator;
 import StevenDimDoors.mod_pocketDim.util.Point4D;
 import StevenDimDoors.mod_pocketDim.world.LimboProvider;
 import StevenDimDoors.mod_pocketDim.world.PocketProvider;
 import cpw.mods.fml.relauncher.Side;
 import cpw.mods.fml.relauncher.SideOnly;
 
 public class EventHookContainer
 {
 	private final DDProperties properties;
 	
 	public EventHookContainer(DDProperties properties)
 	{
 		this.properties = properties;
 	}
 	
 	@SideOnly(Side.CLIENT)
 	@ForgeSubscribe
 	public void onSoundLoad(SoundLoadEvent event) 
 	{
 		event.manager.soundPoolSounds.addSound("mods/DimDoors/sfx/monk.ogg", (mod_pocketDim.class.getResource("/mods/DimDoors/sfx/monk.ogg")));
 		event.manager.soundPoolSounds.addSound("mods/DimDoors/sfx/crack.ogg", (mod_pocketDim.class.getResource("/mods/DimDoors/sfx/crack.ogg")));
 		event.manager.soundPoolSounds.addSound("mods/DimDoors/sfx/tearing.ogg", (mod_pocketDim.class.getResource("/mods/DimDoors/sfx/tearing.ogg")));
 		event.manager.soundPoolSounds.addSound("mods/DimDoors/sfx/rift.ogg", (mod_pocketDim.class.getResource("/mods/DimDoors/sfx/rift.ogg")));
 		event.manager.soundPoolSounds.addSound("mods/DimDoors/sfx/riftStart.ogg", (mod_pocketDim.class.getResource("/mods/DimDoors/sfx/riftStart.ogg")));
 		event.manager.soundPoolSounds.addSound("mods/DimDoors/sfx/riftEnd.ogg", (mod_pocketDim.class.getResource("/mods/DimDoors/sfx/riftEnd.ogg")));
 		event.manager.soundPoolSounds.addSound("mods/DimDoors/sfx/riftClose.ogg", (mod_pocketDim.class.getResource("/mods/DimDoors/sfx/riftClose.ogg")));
 		event.manager.soundPoolSounds.addSound("mods/DimDoors/sfx/riftDoor.ogg", (mod_pocketDim.class.getResource("/mods/DimDoors/sfx/riftDoor.ogg")));
 	}
 
     @ForgeSubscribe
     public void onWorldLoad(WorldEvent.Load event)
     {
     	// We need to initialize PocketManager here because onServerAboutToStart fires before we can
     	// use DimensionManager and onServerStarting fires after the game tries to generate terrain.
     	// If a gateway tries to generate before PocketManager has initialized, we get a crash.
     	if (!PocketManager.isLoaded())
     	{
     		PocketManager.load();
     	}
     	
     	if (PocketManager.isLoaded())
     	{
     		RiftRegenerator.regenerateRiftsInAllWorlds();
     	}
     }
     
     @ForgeSubscribe
     public void onPlayerFall(LivingFallEvent event)
     {
     	event.setCanceled(event.entity.worldObj.provider.dimensionId == properties.LimboDimensionID);
     }
    
     
     @ForgeSubscribe(priority=EventPriority.HIGHEST)
     public boolean LivingDeathEvent(LivingDeathEvent event)
     {
     	Entity entity = event.entity;
    	if(entity instanceof EntityPlayer&&entity.worldObj.provider instanceof PocketProvider && this.properties.LimboEnabled)
     	{
    		if(!this.properties.LimboReturnsInventoryEnabled)
     		{
    			((EntityPlayer)entity).inventory.clearInventory(-1, -1);
     		}
    		ChunkCoordinates coords = LimboProvider.getLimboSkySpawn(entity.worldObj.rand);
    		DDTeleporter.teleportEntity(entity, new Point4D(coords.posX,coords.posY,coords.posZ,mod_pocketDim.properties.LimboDimensionID));
    		((EntityLiving) entity).setEntityHealth(20);
     		event.setCanceled(true);
     		return false;
     	}
     	return true;
     }
 
     @ForgeSubscribe
     public void onWorldsave(WorldEvent.Save event)
     {
     	if (event.world.provider.dimensionId == 0)
     	{
     		PocketManager.save();
     	}
     }
 }
