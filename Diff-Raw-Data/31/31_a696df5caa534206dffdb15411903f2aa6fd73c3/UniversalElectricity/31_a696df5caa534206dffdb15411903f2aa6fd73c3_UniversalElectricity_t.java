 package universalelectricity;
 
 import java.io.File;
 import java.util.ArrayList;
 import java.util.List;
 
 import net.minecraft.src.EntityPlayer;
 import net.minecraft.src.MapColor;
 import net.minecraft.src.Material;
 import net.minecraftforge.common.Configuration;
 import net.minecraftforge.common.ForgeVersion;
 import net.minecraftforge.common.MinecraftForge;
 import net.minecraftforge.event.ForgeSubscribe;
 import net.minecraftforge.event.entity.living.LivingDeathEvent;
 import net.minecraftforge.event.world.WorldEvent.Load;
 import net.minecraftforge.event.world.WorldEvent.Save;
 import universalelectricity.electricity.ElectricityManager;
 import universalelectricity.network.ConnectionHandler;
 import universalelectricity.ore.OreGenerator;
 import universalelectricity.recipe.RecipeManager;
 import cpw.mods.fml.common.Loader;
 import cpw.mods.fml.common.Mod;
 import cpw.mods.fml.common.Mod.PostInit;
 import cpw.mods.fml.common.Mod.PreInit;
 import cpw.mods.fml.common.event.FMLPostInitializationEvent;
 import cpw.mods.fml.common.event.FMLPreInitializationEvent;
 import cpw.mods.fml.common.network.NetworkMod;
 import cpw.mods.fml.common.registry.GameRegistry;
 
 @Mod(modid = "UniversalElectricity", name = "Universal Electricity", version = UniversalElectricity.VERSION, dependencies = "after:*")
 @NetworkMod(clientSideRequired = true, serverSideRequired = false, connectionHandler = ConnectionHandler.class)
 
 public class UniversalElectricity
 {
 	protected static final String VERSION = "0.8.3";
 
 	public static final Configuration CONFIGURATION = new Configuration(new File(Loader.instance().getConfigDir(), "UniversalElectricity/UniversalElectricity.cfg"));
 	public static final List<Object> MODS = new ArrayList<Object>();
         
     public static UniversalElectricity instance;
     
     //One IC2 EU is 0.012 Watt Hours. EU to Watt Hour
  	public static final float IC2_RATIO = 0.004f;
   	
   	//One MJ is 13 Watt Hours. MJ to Watt Hour
  	public static final float BC3_RATIO = 0.03f;
   	
   	public static final float Wh_IC2_RATIO = 1/IC2_RATIO;
   	
   	public static final float Wh_BC_RATIO = 1/BC3_RATIO;
     
     /**
 	 * Use this material for all your machine blocks. It can be breakable by hand.
 	 */
 	public static final Material machine = new Material(MapColor.ironColor);
         
     public static void registerMod(Object networkmod, String modName, String version)
     {
         String[] ueVersionNumbers = VERSION.split("\\.");
         String[] modVersionNumbers = version.split("\\.");
 
         if (Integer.parseInt(modVersionNumbers[0]) != Integer.parseInt(ueVersionNumbers[0]) || Integer.parseInt(modVersionNumbers[1]) != Integer.parseInt(ueVersionNumbers[1]))
         {
             throw new RuntimeException("Universal Electricity version mismatch, expecting " + version);
         }
         else if (Integer.parseInt(modVersionNumbers[2]) > Integer.parseInt(ueVersionNumbers[2]))
         {
             throw new RuntimeException("Universal Electricity too old, expecting at least" + version);
         }
 
         MODS.add(networkmod);
         System.out.println("Loaded Universal Electricity Mod: " + modName);
     }
 
     @PreInit
    	public void preInit(FMLPreInitializationEvent event)
     {
    		instance = this;
    		
    		GameRegistry.registerWorldGenerator(new OreGenerator());
    		MinecraftForge.EVENT_BUS.register(this.instance);
    		
 		ElectricityManager.instance = new ElectricityManager();
     }
     
     @PostInit
    	public void modsLoaded(FMLPostInitializationEvent evt) 
 	{
     	RecipeManager.addRecipes();
 	}
     
     @ForgeSubscribe
     public void onEntityDeath(LivingDeathEvent event)
     {
     	if(event.entity instanceof EntityPlayer)
     	{
         	ElectricityManager.instance.timedConductorRefresh();
     	}
     }
 
     @ForgeSubscribe
 	public void onWorldLoad(Load event)
 	{
     	ElectricityManager.instance.timedConductorRefresh();
 	}
     
     @ForgeSubscribe
 	public void onWorldSave(Save event)
 	{
     	ElectricityManager.instance.timedConductorRefresh();
 	}
     
     public static void forgeLock(int major, int minor, int revision, boolean strict)
     {
     	if(ForgeVersion.getMajorVersion() != major)
 		{
 			throw new RuntimeException("Universal Electricity: Wrong Minecraft Forge version! Get: "+major+"."+minor+"."+revision);
 		}
 		
     	if(ForgeVersion.getMinorVersion() < minor)
 		{
 			throw new RuntimeException("Universal Electricity: Minecraft Forge minor version is too old! Get: "+major+"."+minor+"."+revision);
 		}
     	
     	if(ForgeVersion.getRevisionVersion() < revision)
 		{
     		if(strict)
     		{
     			throw new RuntimeException("Universal Electricity: Minecraft Forge revision version is too old! Get: "+major+"."+minor+"."+revision);
     		}
     		else
     		{
     			System.out.println("Universal Electricity Warning: Minecraft Forge is not the specified version. Odd things might happen.");
     		}
 		}
     }
     
     public static void forgeLock(int major, int minor, int revision)
     {
     	forgeLock(major, minor, revision, false);
     }
     
     /**
      * Gets the ID of a block from the configuration file
      * @param name - The name of the block or item
      * @param defaultID - The default ID of the block or item. Any errors will restore this block/item ID
      * @return The block or item ID
      */
     public static int getBlockConfigID(Configuration configuration, String name, int defaultID)
     {
         configuration.load();
         int id = defaultID;
 
         id = Integer.parseInt(configuration.getOrCreateIntProperty(name, Configuration.CATEGORY_BLOCK, defaultID).value);
 
         if (id <= 136)
         {
             return defaultID;
         }
           
         configuration.save();
         return id;
     }
     
     public static int getItemConfigID(Configuration configuration, String name, int defaultID)
     {
         configuration.load();
         int id = defaultID;
 
         id = Integer.parseInt(configuration.getOrCreateIntProperty(name, Configuration.CATEGORY_ITEM, defaultID).value);
 
         if (id < 256)
         {
             return defaultID;
         }
 
         configuration.save();
         return id;
     }
 }
