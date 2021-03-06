 package ds.mods.CCLights2;
 
 import java.util.logging.Level;
 import java.util.logging.Logger;
 
 import net.minecraft.block.Block;
 import net.minecraft.creativetab.CreativeTabs;
 import net.minecraft.item.Item;
 import net.minecraft.item.ItemStack;
 import net.minecraftforge.common.Configuration;
 import net.minecraftforge.common.MinecraftForge;
 import cpw.mods.fml.common.FMLLog;
 import cpw.mods.fml.common.Loader;
 import cpw.mods.fml.common.Mod;
 import cpw.mods.fml.common.Mod.EventHandler;
 import cpw.mods.fml.common.Mod.Instance;
 import cpw.mods.fml.common.SidedProxy;
 import cpw.mods.fml.common.event.FMLInitializationEvent;
 import cpw.mods.fml.common.event.FMLPreInitializationEvent;
 import cpw.mods.fml.common.network.NetworkMod;
 import ds.mods.CCLights2.gpu.imageLoader.GeneralImageLoader;
 import ds.mods.CCLights2.gpu.imageLoader.ImageLoader;
 import ds.mods.CCLights2.network.PacketHandler;
 
@Mod(modid = "CCLights2", name = "CCLights2", version = "0.2",dependencies="required-after:ComputerCraft;required-after:CCTurtle")
 @NetworkMod(clientSideRequired = true, serverSideRequired = true, channels = { "CCLights2" }, packetHandler = PacketHandler.class)
 public class CCLights2 {
 	@Instance("CCLights2")
 	public static CCLights2 instance;
 	@SidedProxy(serverSide = "ds.mods.CCLights2.CommonProxy", clientSide = "ds.mods.CCLights2.client.ClientProxy")
 	// start variables
 	public static CommonProxy proxy;
 	public static Block gpu,monitor,monitorBig,light,advancedlight,ttrans;
 	public static Item ram,tablet;
 	protected static Configuration config;
 	public static Logger logger;
 
 	public static CreativeTabs ccltab = new CreativeTabs("CClights2") {
 		@Override
 		public ItemStack getIconItemStack() {
 			return new ItemStack(Config.Tablet, 1, 0);
 		}
 	};
 
 	// end variables
 
 	@EventHandler
 	public void preInit(FMLPreInitializationEvent event) {
 		Configuration config = new Configuration(event.getSuggestedConfigurationFile());
 		Config.loadConfig(config);
 		ImageLoader.register(new GeneralImageLoader());
 		logger = event.getModLog();
 		logger.setParent(FMLLog.getLogger());
 		Compat.init();
 		if(Config.Vanilla){
 		Compat.Vanilla();
 		}
 		if (Loader.isModLoaded("IC2") && Config.IC2) {
             Compat.IC2();
         }
 	}
 
 	@EventHandler
 	public void load(FMLInitializationEvent event) {
 		proxy.registerRenderInfo();
 		MinecraftForge.EVENT_BUS.register(new Events());
 	}
 
 	public static void debug(String debugmsg) {
 		if (Config.DEBUGS) {
 			Level level = Level.INFO;
 			logger.log(level, debugmsg);
 		}
 	}
 }
