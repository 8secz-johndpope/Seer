 package StevenDimDoors.mod_pocketDim;
 
 
 import java.util.ArrayList;
 import java.util.HashMap;
 
 import net.minecraft.block.Block;
 import net.minecraft.block.material.Material;
 import net.minecraft.creativetab.CreativeTabs;
 import net.minecraft.entity.EntityEggInfo;
 import net.minecraft.entity.EntityList;
 import net.minecraft.entity.item.EntityItem;
 import net.minecraft.item.Item;
 import net.minecraft.item.ItemStack;
 import net.minecraft.world.biome.BiomeGenBase;
 import net.minecraftforge.common.DimensionManager;
 import net.minecraftforge.common.MinecraftForge;
 import StevenDimDoors.mod_pocketDim.blocks.BlockDimWall;
 import StevenDimDoors.mod_pocketDim.blocks.BlockDimWallPerm;
 import StevenDimDoors.mod_pocketDim.blocks.BlockLimbo;
 import StevenDimDoors.mod_pocketDim.blocks.BlockRift;
 import StevenDimDoors.mod_pocketDim.blocks.ChaosDoor;
 import StevenDimDoors.mod_pocketDim.blocks.ExitDoor;
 import StevenDimDoors.mod_pocketDim.blocks.dimDoor;
 import StevenDimDoors.mod_pocketDim.blocks.dimHatch;
 import StevenDimDoors.mod_pocketDim.commands.CommandCreateDungeonRift;
 import StevenDimDoors.mod_pocketDim.commands.CommandCreatePocket;
 import StevenDimDoors.mod_pocketDim.commands.CommandDeleteAllLinks;
 import StevenDimDoors.mod_pocketDim.commands.CommandDeleteDimensionData;
 import StevenDimDoors.mod_pocketDim.commands.CommandDeleteRifts;
 import StevenDimDoors.mod_pocketDim.commands.CommandExportDungeon;
 import StevenDimDoors.mod_pocketDim.commands.CommandPrintDimensionData;
 import StevenDimDoors.mod_pocketDim.commands.CommandPruneDimensions;
 import StevenDimDoors.mod_pocketDim.commands.CommandResetDungeons;
 import StevenDimDoors.mod_pocketDim.helpers.BlockRotationHelper;
 import StevenDimDoors.mod_pocketDim.helpers.DungeonHelper;
 import StevenDimDoors.mod_pocketDim.helpers.dimHelper;
 import StevenDimDoors.mod_pocketDim.items.ItemBlockDimWall;
 import StevenDimDoors.mod_pocketDim.items.ItemChaosDoor;
 import StevenDimDoors.mod_pocketDim.items.ItemRiftBlade;
 import StevenDimDoors.mod_pocketDim.items.ItemStabilizedRiftSignature;
 import StevenDimDoors.mod_pocketDim.items.ItemStableFabric;
 import StevenDimDoors.mod_pocketDim.items.itemDimDoor;
 import StevenDimDoors.mod_pocketDim.items.itemExitDoor;
 import StevenDimDoors.mod_pocketDim.items.itemLinkSignature;
 import StevenDimDoors.mod_pocketDim.items.itemRiftRemover;
 import StevenDimDoors.mod_pocketDim.ticking.CommonTickHandler;
 import StevenDimDoors.mod_pocketDim.ticking.MobMonolith;
 import StevenDimDoors.mod_pocketDim.ticking.MonolithSpawner;
 import StevenDimDoors.mod_pocketDim.ticking.RiftRegenerator;
 import StevenDimDoors.mod_pocketDim.world.BiomeGenLimbo;
 import StevenDimDoors.mod_pocketDim.world.BiomeGenPocket;
 import StevenDimDoors.mod_pocketDim.world.LimboProvider;
 import StevenDimDoors.mod_pocketDim.world.PocketProvider;
 import StevenDimDoors.mod_pocketDimClient.ClientPacketHandler;
 import StevenDimDoors.mod_pocketDimClient.ClientTickHandler;
 import cpw.mods.fml.common.Mod;
 import cpw.mods.fml.common.Mod.Init;
 import cpw.mods.fml.common.Mod.Instance;
 import cpw.mods.fml.common.Mod.PostInit;
 import cpw.mods.fml.common.Mod.PreInit;
 import cpw.mods.fml.common.Mod.ServerStarting;
 import cpw.mods.fml.common.Mod.ServerStopping;
 import cpw.mods.fml.common.SidedProxy;
 import cpw.mods.fml.common.event.FMLInitializationEvent;
 import cpw.mods.fml.common.event.FMLPostInitializationEvent;
 import cpw.mods.fml.common.event.FMLPreInitializationEvent;
 import cpw.mods.fml.common.event.FMLServerStartingEvent;
 import cpw.mods.fml.common.event.FMLServerStoppingEvent;
 import cpw.mods.fml.common.network.NetworkMod;
 import cpw.mods.fml.common.network.NetworkMod.SidedPacketHandler;
 import cpw.mods.fml.common.registry.EntityRegistry;
 import cpw.mods.fml.common.registry.GameRegistry;
 import cpw.mods.fml.common.registry.LanguageRegistry;
 import cpw.mods.fml.common.registry.TickRegistry;
 import cpw.mods.fml.relauncher.Side;
 
 
 @Mod(modid = mod_pocketDim.modid, name = "Dimensional Doors", version = mod_pocketDim.version)
 
 
 @NetworkMod(clientSideRequired = true, serverSideRequired = false,
 clientPacketHandlerSpec =
 @SidedPacketHandler(channels = {"pocketDim" }, packetHandler = ClientPacketHandler.class),
 serverPacketHandlerSpec =
 @SidedPacketHandler(channels = {"pocketDim" }, packetHandler = ServerPacketHandler.class),
 channels={"DimDoorPackets"}, packetHandler = PacketHandler.class, connectionHandler=ConnectionHandler.class)
 
 public class mod_pocketDim
 {
 	public static final String version = "1.5.2R1.4.1RC1";
 	public static final String modid = "DimDoors";
 
 	//need to clean up 
 	@SidedProxy(clientSide = "StevenDimDoors.mod_pocketDimClient.ClientProxy", serverSide = "StevenDimDoors.mod_pocketDim.CommonProxy")
 	public static CommonProxy proxy;
 
 	@Instance("PocketDimensions")
 	public static mod_pocketDim instance = new mod_pocketDim();
 	
 	public static SchematicLoader loader;
 	public static pocketTeleporter teleporter;
 	public static BlockRotationHelper rotationHelper;
 
 	public static Block transientDoor;
 	public static Block ExitDoor;
 	public static Block chaosDoor;
 	public static Block blockRift;
 	public static Block blockLimbo;
 	public static Block dimDoor;    
 	public static Block blockDimWall;   
 	public static Block dimHatch;
 	public static Block blockDimWallPerm;
 
 	public static Item itemRiftBlade;
 	public static Item itemDimDoor;
 	public static Item itemExitDoor;
 	public static Item itemRiftRemover;
 	public static Item itemLinkSignature;
 	public static Item itemStableFabric;
 	public static Item itemChaosDoor;
 	public static Item itemStabilizedLinkSignature;
 	
 	public static BiomeGenBase limboBiome;
 	public static BiomeGenBase pocketBiome;
 
 	public static PlayerRespawnTracker tracker;
 
 	public static HashMap<String,ArrayList<EntityItem>> limboSpawnInventory = new HashMap<String,ArrayList<EntityItem>>();
 
 	public static ArrayList<Integer> blocksImmuneToRift = new ArrayList<Integer>();
 
 	public static boolean hasInitDims = false;
 	public static boolean isPlayerWearingGoogles = false;
 
 	public static DDProperties properties;
 	public static MonolithSpawner spawner; //Added this field temporarily. Will be refactored out later.
 	public static RiftGenerator riftGen;
 
 	public static long genTime;
 	public static int teleTimer = 0;
 	
 	 public static CreativeTabs dimDoorsCreativeTab = new CreativeTabs("dimDoorsCreativeTab") 
 	 {
 		 @Override
 		 public ItemStack getIconItemStack() 
 		 {
 			 return new ItemStack(mod_pocketDim.itemDimDoor, 1, 0);
          }
 		 
 		 @Override
 		 public String getTranslatedTabLabel()
 		 {
 			 return "Dimensional Doors";
 		 }
 	 };
 	 
 	 
 
 	@PreInit
 	public void PreInit(FMLPreInitializationEvent event)
 	{
 		//This should be the FIRST thing that gets done.
 		properties = DDProperties.initialize(event.getSuggestedConfigurationFile());
 
 		//Now do other stuff
 		MinecraftForge.EVENT_BUS.register(new EventHookContainer());
 		
 		//These fields MUST be initialized after properties are loaded to prevent
 		//instances from holding onto null references to the properties.
 		
 		loader = new SchematicLoader();
 		teleporter = new pocketTeleporter();
 		tracker = new PlayerRespawnTracker();
 		riftGen = new RiftGenerator();
 		rotationHelper = new BlockRotationHelper();
 	}
 
 	@Init
 	public void Init(FMLInitializationEvent event)
 	{
 		CommonTickHandler commonTickHandler = new CommonTickHandler();
 		TickRegistry.registerTickHandler(new ClientTickHandler(), Side.CLIENT);
 		TickRegistry.registerTickHandler(commonTickHandler, Side.SERVER);
 		
 		//MonolithSpawner should be initialized before any provider instances are created
 		//Register the other regular tick receivers as well
 		spawner = new MonolithSpawner(commonTickHandler, properties);
		new RiftRegenerator(commonTickHandler); //No need to store the reference
 		LimboDecay decay = new LimboDecay(commonTickHandler, properties);
 
 		transientDoor = (new TransientDoor(properties.TransientDoorID, Material.iron)).setHardness(1.0F) .setUnlocalizedName("transientDoor");
 
 		blockDimWall = (new BlockDimWall(properties.FabricBlockID, 0, Material.iron)).setLightValue(1.0F).setHardness(0.1F).setUnlocalizedName("blockDimWall");
 		blockDimWallPerm = (new BlockDimWallPerm(properties.PermaFabricBlockID, 0, Material.iron)).setLightValue(1.0F).setBlockUnbreakable().setResistance(6000000.0F).setUnlocalizedName("blockDimWallPerm");
 		ExitDoor = (new ExitDoor(properties.WarpDoorID, Material.wood)).setHardness(1.0F) .setUnlocalizedName("dimDoorWarp");
 		blockRift = (new BlockRift(properties.RiftBlockID, 0, Material.air).setHardness(1.0F) .setUnlocalizedName("rift"));
 		blockLimbo = (new BlockLimbo(properties.LimboBlockID, 15, Material.iron, properties.LimboDimensionID, decay).setHardness(.2F).setUnlocalizedName("BlockLimbo").setLightValue(.0F));
 		chaosDoor = (new ChaosDoor(properties.UnstableDoorID, Material.iron).setHardness(.2F).setUnlocalizedName("chaosDoor").setLightValue(.0F) );
 		dimDoor = (new dimDoor(properties.DimensionalDoorID, Material.iron)).setHardness(1.0F).setResistance(2000.0F) .setUnlocalizedName("dimDoor");
 		dimHatch = (new dimHatch(properties.TransTrapdoorID, 84, Material.iron)).setHardness(1.0F) .setUnlocalizedName("dimHatch");
 		//  dimRail = (new DimRail(dimRailID, 88, false)).setHardness(.5F) .setUnlocalizedName("dimRail");
 
 		itemDimDoor = (new itemDimDoor(properties.DimensionalDoorItemID, Material.iron)).setUnlocalizedName("itemDimDoor");
 		itemExitDoor = (new itemExitDoor(properties.WarpDoorItemID, Material.wood)).setUnlocalizedName("itemDimDoorWarp");
 		itemLinkSignature = (new itemLinkSignature(properties.RiftSignatureItemID)).setUnlocalizedName("itemLinkSignature");
 		itemRiftRemover = (new itemRiftRemover(properties.RiftRemoverItemID, Material.wood)).setUnlocalizedName("itemRiftRemover");
 		itemStableFabric = (new ItemStableFabric(properties.StableFabricItemID, 0)).setUnlocalizedName("itemStableFabric");
 		itemChaosDoor = (new ItemChaosDoor(properties.UnstableDoorItemID, Material.iron)).setUnlocalizedName("itemChaosDoor");
 		itemRiftBlade = (new ItemRiftBlade(properties.RiftBladeItemID)).setUnlocalizedName("ItemRiftBlade");
 		itemStabilizedLinkSignature = (new ItemStabilizedRiftSignature(properties.StabilizedRiftSignatureItemID)).setUnlocalizedName("itemStabilizedRiftSig");
 
 		mod_pocketDim.limboBiome= (new BiomeGenLimbo(properties.LimboBiomeID));
 		mod_pocketDim.pocketBiome= (new BiomeGenPocket(properties.PocketBiomeID));
 
 		GameRegistry.registerWorldGenerator(mod_pocketDim.riftGen);
 
 		GameRegistry.registerBlock(chaosDoor, "Unstable Door");
 		GameRegistry.registerBlock(ExitDoor, "Warp Door");
 		GameRegistry.registerBlock(blockRift, "Rift");
 		GameRegistry.registerBlock(blockLimbo, "Unraveled Fabric");
 		GameRegistry.registerBlock(dimDoor, "Dimensional Door");
 		GameRegistry.registerBlock(dimHatch,"Transdimensional Trapdoor");
 		GameRegistry.registerBlock(blockDimWallPerm, "Fabric of RealityPerm");
 		GameRegistry.registerBlock(transientDoor, "transientDoor");
 		
 		GameRegistry.registerBlock(blockDimWall, ItemBlockDimWall.class, "Fabric of Reality");
 
 		GameRegistry.registerPlayerTracker(tracker);
 
 		DimensionManager.registerProviderType(properties.PocketProviderID, PocketProvider.class, false);
 		DimensionManager.registerProviderType(properties.LimboProviderID, LimboProvider.class, false);
 		DimensionManager.registerDimension(properties.LimboDimensionID, properties.LimboProviderID);
 
 		LanguageRegistry.addName(transientDoor	, "transientDoor");
 		LanguageRegistry.addName(blockRift	, "Rift");
 		LanguageRegistry.addName(blockLimbo	, "Unraveled Fabric");
 		LanguageRegistry.addName(ExitDoor	, "Warp Door");
 		LanguageRegistry.addName(chaosDoor	, "Unstable Door");
 		LanguageRegistry.addName(blockDimWall	, "Fabric of Reality");
 		LanguageRegistry.addName(blockDimWallPerm	, "Eternal Fabric");
 		LanguageRegistry.addName(dimDoor, "Dimensional Door");
 		LanguageRegistry.addName(dimHatch, "Transdimensional Trapdoor");
 	
 		LanguageRegistry.addName(itemExitDoor, "Warp Door");
 		LanguageRegistry.addName(itemLinkSignature	, "Rift Signature");
 		LanguageRegistry.addName(itemStabilizedLinkSignature, "Stabilized Rift Signature");
 		LanguageRegistry.addName(itemRiftRemover	, "Rift Remover");
 		LanguageRegistry.addName(itemStableFabric	, "Stable Fabric");
 		LanguageRegistry.addName(itemChaosDoor	, "Unstable Door");
 		LanguageRegistry.addName(itemDimDoor, "Dimensional Door");
 		LanguageRegistry.addName(itemRiftBlade	, "Rift Blade");
 		
 		/**
 		 * Add names for multiblock inventory item
 		 */
 		LanguageRegistry.addName(new ItemStack(blockDimWall, 1, 0), "Fabric of Reality");
 		LanguageRegistry.addName(new ItemStack(blockDimWall, 1, 1), "Ancient Fabric");
 
 		
 		LanguageRegistry.instance().addStringLocalization("itemGroup.dimDoorsCustomTab", "en_US", "Dimensional Doors Items");
 		
 		//GameRegistry.registerTileEntity(TileEntityDimDoor.class, "TileEntityDimRail");
 
 		GameRegistry.registerTileEntity(TileEntityDimDoor.class, "TileEntityDimDoor");
 		GameRegistry.registerTileEntity(TileEntityRift.class, "TileEntityRift");
 
 		EntityRegistry.registerModEntity(MobMonolith.class, "Monolith", properties.MonolithEntityID, this, 70, 1, true);
 		EntityList.IDtoClassMapping.put(properties.MonolithEntityID, MobMonolith.class);
 		EntityList.entityEggs.put(properties.MonolithEntityID, new EntityEggInfo(properties.MonolithEntityID, 0, 0xffffff));
 		LanguageRegistry.instance().addStringLocalization("entity.DimDoors.Obelisk.name", "Monolith");
 
 		//GameRegistry.addBiome(this.limboBiome);
 		//GameRegistry.addBiome(this.pocketBiome);
 
 		if (properties.CraftingDimensionaDoorAllowed)
 		{
 			GameRegistry.addRecipe(new ItemStack(itemDimDoor, 1), new Object[]
 					{
 				"   ", "yxy", "   ", 'x', Item.enderPearl,  'y', Item.doorIron 
 					});
 
 			GameRegistry.addRecipe(new ItemStack(itemDimDoor, 1), new Object[]
 					{
 				"   ", "yxy", "   ", 'x', mod_pocketDim.itemStableFabric,  'y', Item.doorIron 
 					});
 		}
 
 		/**
        if(this.enableDimRail)
         {
     	 GameRegistry.addRecipe(new ItemStack(dimRail, 1), new Object[]
                  {
                      "   ", "yxy", "   ", 'x', this.itemDimDoor,  'y', Block.rail 
                  });
 
     	 GameRegistry.addRecipe(new ItemStack(dimRail, 1), new Object[]
                  {
                      "   ", "yxy", "   ", 'x', this.itemExitDoor,  'y', Block.rail 
                  });
         }
 		 **/
 
 		if(properties.CraftingUnstableDoorAllowed)
 		{
 			GameRegistry.addRecipe(new ItemStack(itemChaosDoor, 1), new Object[]
 					{
 				"   ", "yxy", "   ", 'x', Item.eyeOfEnder,  'y', mod_pocketDim.itemDimDoor 
 					});
 		}
 		if(properties.CraftingWarpDoorAllowed)
 		{
 			GameRegistry.addRecipe(new ItemStack(itemExitDoor, 1), new Object[]
 					{
 				"   ", "yxy", "   ", 'x', Item.enderPearl,  'y', Item.doorWood 
 					});
 
 			GameRegistry.addRecipe(new ItemStack(itemExitDoor, 1), new Object[]
 					{
 				"   ", "yxy", "   ", 'x', mod_pocketDim.itemStableFabric,  'y', Item.doorWood 
 					});
 		}
 		if(properties.CraftingTransTrapdoorAllowed)
 		{
 			GameRegistry.addRecipe(new ItemStack(dimHatch, 1), new Object[]
 					{
 				" y ", " x ", " y ", 'x', Item.enderPearl,  'y', Block.trapdoor
 					});
 
 			GameRegistry.addRecipe(new ItemStack(dimHatch, 1), new Object[]
 					{
 				" y ", " x ", " y ", 'x', mod_pocketDim.itemStableFabric,  'y', Block.trapdoor
 					});
 		}
 		if(properties.CraftingRiftSignatureAllowed)
 		{
 			GameRegistry.addRecipe(new ItemStack(itemLinkSignature, 1), new Object[]
 					{
 				" y ", "yxy", " y ", 'x', Item.enderPearl,  'y', Item.ingotIron
 					});
 
 			GameRegistry.addRecipe(new ItemStack(itemLinkSignature, 1), new Object[]
 					{
 				" y ", "yxy", " y ", 'x', mod_pocketDim.itemStableFabric,  'y', Item.ingotIron
 					});
 		}
 
 		if(properties.CraftingRiftRemoverAllowed)
 		{
 			GameRegistry.addRecipe(new ItemStack(itemRiftRemover, 1), new Object[]
 					{
 				" y ", "yxy", " y ", 'x', Item.enderPearl,  'y', Item.ingotGold
 					});
 			GameRegistry.addRecipe(new ItemStack(itemRiftRemover, 1), new Object[]
 					{
 				"yyy", "yxy", "yyy", 'x', mod_pocketDim.itemStableFabric,  'y', Item.ingotGold
 					});
 		}
 
 		if (properties.CraftingRiftBladeAllowed)
 		{
 
 			GameRegistry.addRecipe(new ItemStack(itemRiftBlade, 1), new Object[]
 					{
 				" x ", " x ", " y ", 'x', Item.enderPearl,  'y',mod_pocketDim.itemRiftRemover
 					});
 		}
 
 		if (properties.CraftingStableFabricAllowed)
 		{
 			GameRegistry.addRecipe(new ItemStack(itemStableFabric, 4), new Object[]
 					{
 				" y ", "yxy", " y ", 'x', Item.enderPearl,  'y', mod_pocketDim.blockDimWall
 					});
 		}
 		
 		if (properties.CraftingStabilizedRiftSignatureAllowed)
 		{
 			GameRegistry.addRecipe(new ItemStack(mod_pocketDim.itemStabilizedLinkSignature,1), new Object[]
 					{
 				" y ", "yxy", " y ", 'x', mod_pocketDim.itemLinkSignature,  'y', mod_pocketDim.itemStableFabric
 					});
 		}
 
 		mod_pocketDim.blocksImmuneToRift.add(properties.FabricBlockID);
 		mod_pocketDim.blocksImmuneToRift.add(properties.PermaFabricBlockID);
 		mod_pocketDim.blocksImmuneToRift.add(properties.DimensionalDoorID);
 		mod_pocketDim.blocksImmuneToRift.add(properties.WarpDoorID);
 		mod_pocketDim.blocksImmuneToRift.add(properties.TransTrapdoorID);
 		mod_pocketDim.blocksImmuneToRift.add(properties.UnstableDoorID);
 		mod_pocketDim.blocksImmuneToRift.add(properties.RiftBlockID);
 		mod_pocketDim.blocksImmuneToRift.add(properties.TransientDoorID);
 		mod_pocketDim.blocksImmuneToRift.add(Block.blockIron.blockID);
 		mod_pocketDim.blocksImmuneToRift.add(Block.blockDiamond.blockID);
 		mod_pocketDim.blocksImmuneToRift.add(Block.blockEmerald.blockID);
 		mod_pocketDim.blocksImmuneToRift.add(Block.blockGold.blockID);
 		mod_pocketDim.blocksImmuneToRift.add(Block.blockLapis.blockID);
 		mod_pocketDim.blocksImmuneToRift.add(Block.bedrock.blockID);
 
 		DungeonHelper.initialize();
 		
 		proxy.loadTextures();
 		proxy.registerRenderers();
 	}
 
 
 	@PostInit
 	public void PostInit(FMLPostInitializationEvent event)
 	{	
 		//Register loot chests
 		DDLoot.registerInfo();
 	}
 
 	@ServerStopping
 	public void serverStopping(FMLServerStoppingEvent event)
 	{
 		try
 		{
 			dimHelper.instance.save();
 			dimHelper.instance.unregsisterDims();
 			dimHelper.dimList.clear();
 			dimHelper.blocksToDecay.clear();
 			dimHelper.instance.interDimLinkList.clear();
 			mod_pocketDim.hasInitDims=false;
 		}
 		catch(Exception e)
 		{
 			e.printStackTrace();
 		}
 	}
 
 
 	@ServerStarting
 	public void serverStarting(FMLServerStartingEvent event)
 	{
 		CommandResetDungeons.instance().register(event);
 		CommandCreateDungeonRift.instance().register(event);
 		CommandDeleteAllLinks.instance().register(event);
 		CommandDeleteDimensionData.instance().register(event);
 		CommandDeleteRifts.instance().register(event);
 		CommandExportDungeon.instance().register(event);
 		CommandPrintDimensionData.instance().register(event);
 		CommandPruneDimensions.instance().register(event);
 		CommandCreatePocket.instance().register(event);
 		dimHelper.instance.load();
 		
 		if(!dimHelper.dimList.containsKey(properties.LimboDimensionID))
 		{
 			dimHelper.dimList.put(properties.LimboDimensionID, new DimData( properties.LimboDimensionID,  false,  0,  new LinkData()));
 		}
 	}
 	
 	
 }
