 package lycanite.lycanitesmobs;
 
 import lycanite.lycanitesmobs.api.ILycaniteMod;
 import lycanite.lycanitesmobs.api.entity.EntityPortal;
 import lycanite.lycanitesmobs.api.info.MobInfo;
 import lycanite.lycanitesmobs.api.info.ObjectLists;
 import lycanite.lycanitesmobs.api.info.SpawnInfo;
 import lycanite.lycanitesmobs.api.item.ItemSoulgazer;
 import lycanite.lycanitesmobs.api.item.ItemStaffBlood;
 import lycanite.lycanitesmobs.api.item.ItemStaffSavage;
 import lycanite.lycanitesmobs.api.item.ItemStaffStable;
 import lycanite.lycanitesmobs.api.item.ItemStaffSturdy;
 import lycanite.lycanitesmobs.api.item.ItemStaffSummoning;
 import lycanite.lycanitesmobs.api.packet.PacketPipeline;
 import lycanite.lycanitesmobs.api.spawning.CustomSpawner;
 import lycanite.lycanitesmobs.api.spawning.SpawnType;
 import net.minecraft.creativetab.CreativeTabs;
 import net.minecraft.init.Blocks;
 import net.minecraft.init.Items;
 import net.minecraft.item.ItemStack;
 import net.minecraft.world.biome.BiomeGenBase;
 import net.minecraftforge.common.BiomeDictionary;
 import net.minecraftforge.common.MinecraftForge;
 import net.minecraftforge.oredict.ShapedOreRecipe;
 import cpw.mods.fml.common.Mod;
 import cpw.mods.fml.common.Mod.EventHandler;
 import cpw.mods.fml.common.Mod.Instance;
 import cpw.mods.fml.common.SidedProxy;
 import cpw.mods.fml.common.event.FMLInitializationEvent;
 import cpw.mods.fml.common.event.FMLPostInitializationEvent;
 import cpw.mods.fml.common.event.FMLPreInitializationEvent;
 import cpw.mods.fml.common.network.NetworkRegistry;
 import cpw.mods.fml.common.registry.EntityRegistry;
 import cpw.mods.fml.common.registry.GameRegistry;
 
 @Mod(modid = LycanitesMobs.modid, name = LycanitesMobs.name, version = LycanitesMobs.version)
 public class LycanitesMobs implements ILycaniteMod {
 	
 	public static final String modid = "lycanitesmobs";
 	public static final String name = "Lycanites Mobs";
	public static final String version = "1.5.2c - MC 1.7.2";
 	public static final String domain = modid.toLowerCase();
 	
 	public static final PacketPipeline packetPipeline = new PacketPipeline();
 	
 	public static int mobID = -1;
 	public static int projectileID = 99;
 	
 	public static Config config = new SubConfig();
 	
 	// Instance:
 	@Instance(modid)
 	public static LycanitesMobs instance;
 	
 	// Proxy:
 	@SidedProxy(clientSide="lycanite.lycanitesmobs.ClientProxy", serverSide="lycanite.lycanitesmobs.CommonProxy")
 	public static CommonProxy proxy;
 	
 	// Creative Tab:
 	public static final CreativeTabs creativeTab = new CreativeTab(CreativeTabs.getNextID(), modid);
 	
 	// Texture Path:
 	public static String texturePath = "mods/lycanitesmobs/";
 	
 	
 	// ==================================================
 	//                Pre-Initialization
 	// ==================================================
 	@EventHandler
 	public void preInit(FMLPreInitializationEvent event) {
 		// ========== Config ==========
 		config.init(modid);
 		
 		// ========== Mob Info ==========
 		MobInfo.loadGlobalSettings();
 		
 		// ========== Spawn Type ==========
 		SpawnType.loadSpawnTypes();
 		
 		// ========== Spawn Info ==========
 		SpawnInfo.loadGlobalSettings();
 		
 		// ========== Register Event Listeners ==========
 		MinecraftForge.EVENT_BUS.register(new EventListener());
 		MinecraftForge.EVENT_BUS.register(new CustomSpawner());
 		
 		// ========== Set Current Mod ==========
 		ObjectManager.setCurrentMod(this);
 		
 		// ========== Create Items ==========
 		ObjectManager.addItem("soulgazer", new ItemSoulgazer());
 		ObjectManager.addItem("summoningstaff", new ItemStaffSummoning());
 		ObjectManager.addItem("stablesummoningstaff", new ItemStaffStable());
 		ObjectManager.addItem("bloodsummoningstaff", new ItemStaffBlood());
 		ObjectManager.addItem("sturdysummoningstaff", new ItemStaffSturdy());
 		ObjectManager.addItem("savagesummoningstaff", new ItemStaffSavage());
 		
 		// ========== Register New 1.7.2 Vanilla Biomes Into Groups (Temporary until forge updates this) ==========
 		BiomeDictionary.registerBiomeType(BiomeGenBase.jungleEdge, BiomeDictionary.Type.JUNGLE);
 		BiomeDictionary.registerBiomeType(BiomeGenBase.deepOcean, BiomeDictionary.Type.WATER);
 		BiomeDictionary.registerBiomeType(BiomeGenBase.stoneBeach, BiomeDictionary.Type.BEACH);
 		BiomeDictionary.registerBiomeType(BiomeGenBase.coldBeach, BiomeDictionary.Type.BEACH);
 		BiomeDictionary.registerBiomeType(BiomeGenBase.birchForest, BiomeDictionary.Type.FOREST);
 		BiomeDictionary.registerBiomeType(BiomeGenBase.birchForestHills, BiomeDictionary.Type.FOREST);
 		BiomeDictionary.registerBiomeType(BiomeGenBase.roofedForest, BiomeDictionary.Type.FOREST, BiomeDictionary.Type.MUSHROOM);
 		BiomeDictionary.registerBiomeType(BiomeGenBase.coldTaiga, BiomeDictionary.Type.FOREST, BiomeDictionary.Type.FROZEN);
 		BiomeDictionary.registerBiomeType(BiomeGenBase.coldTaigaHills, BiomeDictionary.Type.FOREST, BiomeDictionary.Type.FROZEN);
 		BiomeDictionary.registerBiomeType(BiomeGenBase.extremeHillsPlus, BiomeDictionary.Type.MOUNTAIN);
 		BiomeDictionary.registerBiomeType(BiomeGenBase.savanna, BiomeDictionary.Type.PLAINS);
 		BiomeDictionary.registerBiomeType(BiomeGenBase.savannaPlateau, BiomeDictionary.Type.PLAINS);
 		BiomeDictionary.registerBiomeType(BiomeGenBase.mesa, BiomeDictionary.Type.DESERT);
 		BiomeDictionary.registerBiomeType(BiomeGenBase.mesaPlateau_F, BiomeDictionary.Type.DESERT);
 		BiomeDictionary.registerBiomeType(BiomeGenBase.mesaPlateau, BiomeDictionary.Type.DESERT);
 		BiomeDictionary.registerBiomeType(BiomeGenBase.jungleEdge, BiomeDictionary.Type.JUNGLE);
 		BiomeDictionary.registerBiomeType(BiomeGenBase.jungleEdge, BiomeDictionary.Type.JUNGLE);
 	}
 	
 	
 	// ==================================================
 	//                  Initialization
 	// ==================================================
 	@EventHandler
     public void load(FMLInitializationEvent event) {
 		// ========== Register and Initialize Handlers ==========
 		proxy.registerEvents();
 		packetPipeline.initialize();
 		NetworkRegistry.INSTANCE.registerGuiHandler(this, new GuiHandler());
 		
 		// ========== Register Entities ==========
 		int specialEntityID = 0;
 		EntityRegistry.registerModEntity(EntityPortal.class, "summoningportal", specialEntityID++, instance, 64, 1, true);
 	}
 	
 	
 	// ==================================================
 	//                Post-Initialization
 	// ==================================================
 	@EventHandler
     public void postInit(FMLPostInitializationEvent event) {
 		// ========== Register and Initialize Handlers/Objects ==========
 		packetPipeline.postInitialize();
 		proxy.registerAssets();
 		proxy.registerTileEntities();
 		proxy.registerRenders();
 		
 		// ========== Call Object Lists Setup ==========
 		ObjectLists.createLists();
 		
 		// ========== Add Custom Potion Effects ==========
 		if(this.config.getFeatureBool("CustomEffects")) {
 			PotionBase.reserveEffectIDSpace();
 			ObjectManager.addPotionEffect("Paralysis", config, true, 0xFFFF00, 1, 0);
 			ObjectManager.addPotionEffect("Leech", config, true, 0x00FF99, 7, 0);
 			ObjectManager.addPotionEffect("Penetration", config, true, 0x222222, 6, 1);
 			ObjectManager.addPotionEffect("Recklessness", config, true, 0xFF0044, 4, 0);
 			ObjectManager.addPotionEffect("Rage", config, true, 0xFF4400, 4, 0);
 			ObjectManager.addPotionEffect("Weight", config, true, 0x000022, 1, 0);
 			ObjectManager.addPotionEffect("Swiftswimming", config, true, 0x0000FF, 0, 2);
 			MinecraftForge.EVENT_BUS.register(new PotionEffects());
 		}
 		
 		// ========== Crafting ==========
 		GameRegistry.addRecipe(new ShapedOreRecipe(
 				new ItemStack(ObjectManager.getItem("soulgazer"), 1, 0),
 				new Object[] { "GBG", "BDB", "GBG",
 				Character.valueOf('G'), Items.gold_ingot,
 				Character.valueOf('D'), Items.diamond,
 				Character.valueOf('B'), Items.bone
 			}));
 		
 		GameRegistry.addRecipe(new ShapedOreRecipe(
 				new ItemStack(ObjectManager.getItem("summoningstaff"), 1, 0),
 				new Object[] { " E ", " B ", " G ",
 				Character.valueOf('E'), Items.ender_pearl,
 				Character.valueOf('B'), Items.bone,
 				Character.valueOf('G'), Items.gold_ingot
 			}));
 		
 		GameRegistry.addRecipe(new ShapedOreRecipe(
 				new ItemStack(ObjectManager.getItem("stablesummoningstaff"), 1, 0),
 				new Object[] { " D ", " S ", " G ",
 				Character.valueOf('S'), ObjectManager.getItem("summoningstaff"),
 				Character.valueOf('G'), Items.gold_ingot,
 				Character.valueOf('D'), Items.diamond
 			}));
 		
 		GameRegistry.addRecipe(new ShapedOreRecipe(
 				new ItemStack(ObjectManager.getItem("bloodsummoningstaff"), 1, 0),
 				new Object[] { "RRR", "BSB", "NDN",
 				Character.valueOf('S'), ObjectManager.getItem("summoningstaff"),
 				Character.valueOf('R'), Items.redstone,
 				Character.valueOf('B'), Items.bone,
 				Character.valueOf('N'), Items.nether_wart,
 				Character.valueOf('D'), Items.diamond
 			}));
 		
 		GameRegistry.addRecipe(new ShapedOreRecipe(
 				new ItemStack(ObjectManager.getItem("sturdysummoningstaff"), 1, 0),
 				new Object[] { "III", "ISI", " O ",
 				Character.valueOf('S'), ObjectManager.getItem("summoningstaff"),
 				Character.valueOf('I'), Items.iron_ingot,
 				Character.valueOf('O'), Blocks.obsidian
 			}));
 		
 		GameRegistry.addRecipe(new ShapedOreRecipe(
 				new ItemStack(ObjectManager.getItem("savagesummoningstaff"), 1, 0),
 				new Object[] { "LLL", "BSB", "GGG",
 				Character.valueOf('S'), ObjectManager.getItem("summoningstaff"),
 				Character.valueOf('B'), Items.bone,
 				Character.valueOf('G'), Items.ghast_tear,
 				Character.valueOf('L'), new ItemStack(Items.dye, 1, 4)
 			}));
     }
 	
 	
 	// ==================================================
 	//                     Debugging
 	// ==================================================
 	public static void printDebug(String key, String message) {
 		if("".equals(key) || config.getDebug(key)) {
 			System.out.println("[LycanitesMobs] [Debug] " + message);
 		}
 	}
 	
 	public static void printWarning(String key, String message) {
 		if("".equals(key) || config.getDebug(key)) {
 			System.err.println("[LycanitesMobs] [WARNING] " + message);
 		}
 	}
 	
 	
 	// ==================================================
 	//                    Mod Info
 	// ==================================================
 	@Override
 	public LycanitesMobs getInstance() { return instance; }
 	
 	@Override
 	public String getModID() { return modid; }
 	
 	@Override
 	public String getDomain() { return domain; }
 	
 	@Override
 	public Config getConfig() { return config; }
 	
 	@Override
 	public int getNextMobID() { return ++this.mobID; }
 	
 	@Override
 	public int getNextProjectileID() { return ++this.projectileID; }
 }
