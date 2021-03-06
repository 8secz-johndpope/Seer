 package net.minecraft.src;
 
 import java.util.*;
 import java.io.*;
 import java.lang.reflect.*;
 
 public class GPEBTWTweak extends FCAddOn
 {
   public static GPEBTWTweak instance;
   public static GPEBTWTweakProxy proxy;
   public static String tweakVersion = "0.6";
 
   public static Block gpeBlockStone;
   public static Block compatAxleBlock;
 
   public static Item gpeItemLooseRock;
   public static Item gpeItemSilk;
 
   public static int gpeLooseRockID = 17000;
   public static int gpeSilkID = 17001;
   public static int gpeEntityRockID = 25;
   public static int hcSpawnRadius = 2000;
   public static int gpeEntityRockVehicleSpawnType = 120;
   public static int gpeStrataRegenKey = 0;
   public static String gpeStrataRegenWorldName = null;
 
   public static Map<Long, Integer> chunkRegenInfo;
 
   public GPEBTWTweak()
   {
     instance = this;
   }
 
   public void Initialize()
   {
     FCAddOnHandler.LogMessage("Grom PE's BTWTweak v" + tweakVersion + " is now going to tweak the hell out of this.");
     try
     {
       Class.forName("net.minecraft.client.Minecraft");
       proxy = new GPEBTWTweakProxyClient();
     }
     catch(ClassNotFoundException e)
     {
       proxy = new GPEBTWTweakProxy();
     }
     File config = new File(proxy.getConfigDir(), "BTWTweak.cfg");
 
     chunkRegenInfo = new HashMap<Long, Integer>();
 
     try
     {
       BufferedReader br = new BufferedReader(new FileReader(config));
       String line, key, value;
       while ((line = br.readLine()) != null)
       {
         String[] tmp = line.split("=");
         if (tmp.length < 2) continue;
         key = tmp[0].trim();
         value = tmp[1].trim();
 
         if (key.equals("gpeLooseRockID")) gpeLooseRockID = Integer.parseInt(value);
         if (key.equals("gpeSilkID")) gpeSilkID = Integer.parseInt(value);
         if (key.equals("gpeEntityRockID")) gpeEntityRockID = Integer.parseInt(value);
         if (key.equals("gpeEntityRockVehicleSpawnType")) gpeEntityRockVehicleSpawnType = Integer.parseInt(value);
         if (key.equals("hcSpawnRadius")) hcSpawnRadius = Integer.parseInt(value);
         if (key.equals("gpeStrataRegenKey")) gpeStrataRegenKey = Integer.parseInt(value);
         if (key.equals("gpeStrataRegenWorldName")) gpeStrataRegenWorldName = value;
 
       }
       br.close();
     }
     catch (FileNotFoundException e)
     {
       String defaultConfig = ""
         + "// **** BTWTweak Settings ****\r\n"
         + "\r\n"
         + "// Hardcore Spawn radius, in blocks. Changing it from default 2000 may destabilize your game balance.\r\n"
         + "\r\n"
         + "hcSpawnRadius=2000\r\n"
         + "\r\n"
         + "// **** Item IDs ****\r\n"
         + "\r\n"
         + "gpeLooseRockID=17000\r\n"
         + "gpeSilkID=17001\r\n"
         + "\r\n"
         + "// **** Entity IDs ****\r\n"
         + "\r\n"
         + "gpeEntityRockID=35\r\n"
         + "\r\n"
         + "// **** Other IDs ****\r\n"
         + "\r\n"
         + "gpeEntityRockVehicleSpawnType=120\r\n"
         + "\r\n"
         + "// **** World strata regeneration ****\r\n"
         + "\r\n"
         + "// To use, set key to non-zero and name of the world you want to process\r\n"
         + "gpeStrataRegenKey=0\r\n"
         + "gpeStrataRegenWorldName=???\r\n"
         + "";
       try
       {
         FileOutputStream fo = new FileOutputStream(config);
         fo.write(defaultConfig.getBytes());
         fo.close();
       }
       catch (IOException e2)
       {
         FCAddOnHandler.LogMessage("Error while writing default BTWTweak.cfg!");
         e2.printStackTrace();
       }
     }
     catch (IOException e)
     {
       FCAddOnHandler.LogMessage("Error while reading BTWTweak.cfg!");
       e.printStackTrace();
     }
     
     Block.blocksList[1] = null;  gpeBlockStone = new GPEBlockStone(1);
     Block.blocksList[4] = null;  new GPEBlockCobblestone(4);
     Block.blocksList[13] = null; new GPEBlockGravel(13);
     Block.blocksList[65] = null; new GPEBlockLadder(65);
     Block.blocksList[80] = null; new GPEBlockSnowBlock(80);
     Block.blocksList[91] = null;
     ItemAxe.SetAllAxesToBeEffectiveVsBlock((new FCBlockPumpkin(91, true)).setHardness(1.0F).setStepSound(Block.soundWoodFootstep).setLightValue(1.0F).setUnlocalizedName("litpumpkin"));
     new GPEItemPotash(FCBetterThanWolves.fcPotash.itemID - 256);
     gpeItemLooseRock = new GPEItemLooseRock(gpeLooseRockID - 256);
     gpeItemSilk = new Item(gpeSilkID - 256).setUnlocalizedName("gpeItemSilk").setCreativeTab(CreativeTabs.tabMaterials).SetBuoyancy(1.0F).SetBellowsBlowDistance(2);
 
     int id = FCBetterThanWolves.fcAestheticOpaque.blockID;
     Block.blocksList[id] = null;
     new GPEBlockAestheticOpaque(id);
 
     id = FCBetterThanWolves.fcBlockDirtSlab.blockID;
     Block.blocksList[id] = null;
     new GPEBlockDirtSlab(id);
 
     EntityList.addMapping(GPEEntityRock.class, "gpeEntityRock", gpeEntityRockID);
     proxy.addEntityRenderers();
     
     FCRecipes.AddVanillaRecipe(new ItemStack(FCBetterThanWolves.fcBlockDirtSlab, 4, 6), new Object[] {"##", '#', new ItemStack(Block.gravel)});
     FCRecipes.AddVanillaRecipe(new ItemStack(FCBetterThanWolves.fcBlockDirtSlab, 1, 6), new Object[] {"##", '#', new ItemStack(FCBetterThanWolves.fcItemPileGravel)});
     FCRecipes.AddVanillaRecipe(new ItemStack(FCBetterThanWolves.fcBlockDirtSlab, 4, 7), new Object[] {"##", '#', new ItemStack(Block.sand)});
     FCRecipes.AddVanillaRecipe(new ItemStack(FCBetterThanWolves.fcBlockDirtSlab, 1, 7), new Object[] {"##", '#', new ItemStack(FCBetterThanWolves.fcItemPileSand)});
     FCRecipes.AddShapelessVanillaRecipe(new ItemStack(Block.gravel), new Object[] {new ItemStack(FCBetterThanWolves.fcBlockDirtSlab, 1, 6), new ItemStack(FCBetterThanWolves.fcBlockDirtSlab, 1, 6)});
     FCRecipes.AddShapelessVanillaRecipe(new ItemStack(Block.sand), new Object[] {new ItemStack(FCBetterThanWolves.fcBlockDirtSlab, 1, 7), new ItemStack(FCBetterThanWolves.fcBlockDirtSlab, 1, 7)});
     FCRecipes.AddShapelessVanillaRecipe(new ItemStack(FCBetterThanWolves.fcItemPileDirt, 2), new Object[] {new ItemStack(FCBetterThanWolves.fcBlockDirtSlab)});
     FCRecipes.AddShapelessVanillaRecipe(new ItemStack(FCBetterThanWolves.fcItemPileSand, 2), new Object[] {new ItemStack(FCBetterThanWolves.fcBlockDirtSlab, 1, 7)});
     FCRecipes.AddShapelessVanillaRecipe(new ItemStack(FCBetterThanWolves.fcItemPileGravel, 2), new Object[] {new ItemStack(FCBetterThanWolves.fcBlockDirtSlab, 1, 6)});
     FCRecipes.AddStokedCauldronRecipe(new ItemStack(FCBetterThanWolves.fcGlue, 1), new ItemStack[] {new ItemStack(Item.bone, 8)});
     FCRecipes.AddStokedCauldronRecipe(new ItemStack(FCBetterThanWolves.fcGlue, 1), new ItemStack[] {new ItemStack(Item.dyePowder, 24, 15)});
 
     if (isBTWVersionOrNewer("4.891124"))
     {
       int i;
       for (i = 0; i < 16; i++)
       {
         FCRecipes.AddStokedCauldronRecipe(
           new ItemStack[]
           {
             new ItemStack(FCBetterThanWolves.fcItemWool, 4, 15 - i),
             new ItemStack(FCBetterThanWolves.fcAestheticOpaque, 1, 0)
           },
           new ItemStack[] {new ItemStack(Block.cloth, 1, i)});
       }
     }
 
     FCRecipes.RemoveVanillaRecipe(new ItemStack(Item.axeStone), new Object[] {"X ", "X#", " #", '#', Item.stick, 'X', Block.cobblestone});
     FCRecipes.AddVanillaRecipe(new ItemStack(Item.axeStone), new Object[] {"X ", "X#", " #", '#', Item.stick, 'X', gpeItemLooseRock});
     FCRecipes.RemoveVanillaRecipe(new ItemStack(Item.pickaxeStone), new Object[] {"XXX", " # ", " # ", '#', Item.stick, 'X', Block.cobblestone});
     FCRecipes.AddVanillaRecipe(new ItemStack(Item.pickaxeStone), new Object[] {"XXX", " # ", " # ", '#', Item.stick, 'X', gpeItemLooseRock});
     FCRecipes.RemoveVanillaRecipe(new ItemStack(Item.shovelStone), new Object[] {"X", "#", "#", '#', Item.stick, 'X', Block.cobblestone});
     FCRecipes.AddVanillaRecipe(new ItemStack(Item.shovelStone), new Object[] {"X", "#", "#", '#', Item.stick, 'X', gpeItemLooseRock});
     FCRecipes.RemoveVanillaRecipe(new ItemStack(Block.lever, 1), new Object[] {"X", "#", "r", '#', Block.cobblestone, 'X', Item.stick, 'r', Item.redstone});
     FCRecipes.AddVanillaRecipe(new ItemStack(Block.lever, 1), new Object[] {"X", "#", "r", '#', gpeItemLooseRock, 'X', Item.stick, 'r', Item.redstone});
     FCRecipes.AddVanillaRecipe(new ItemStack(Block.cobblestone), new Object[] {"XX", "XX", 'X', gpeItemLooseRock});
     FCRecipes.AddVanillaRecipe(new ItemStack(Block.stoneSingleSlab, 1, 3), new Object[] {"XX", 'X', gpeItemLooseRock});
     FCRecipes.AddVanillaRecipe(new ItemStack(Block.cobblestone), new Object[] {"X", "X", 'X', new ItemStack(Block.stoneSingleSlab, 1, 3)});
     FCRecipes.AddVanillaRecipe(new ItemStack(Block.stoneBrick), new Object[] {"X", "X", 'X', new ItemStack(Block.stoneSingleSlab, 1, 5)});
 
     FCRecipes.AddVanillaRecipe(new ItemStack(gpeItemSilk, 1), new Object[] {"###", "###", "###", '#', Item.silk});
     FCRecipes.AddShapelessVanillaRecipe(new ItemStack(Item.silk, 9), new Object[] {new ItemStack(gpeItemSilk)});
 
     FCRecipes.AddShapelessVanillaRecipe(new ItemStack(Item.book, 1), new Object[] {Item.paper, Item.paper, Item.paper, FCBetterThanWolves.fcItemTannedLeatherCut});
 
     Item pistonCore = isBTWVersionOrNewer("4.89666") ? Item.ingotIron : FCBetterThanWolves.fcSteel;
     FCRecipes.AddStokedCrucibleRecipe(new ItemStack[] {new ItemStack(Item.goldNugget, 3), new ItemStack(pistonCore, 1)}, new ItemStack[] {new ItemStack(Block.pistonBase, 1)});
     FCRecipes.AddStokedCrucibleRecipe(new ItemStack[] {new ItemStack(Item.goldNugget, 3), new ItemStack(pistonCore, 1)}, new ItemStack[] {new ItemStack(Block.pistonStickyBase, 1)});
 
     FCRecipes.RemoveShapelessVanillaRecipe(new ItemStack(FCBetterThanWolves.fcItemIngotDiamond), new Object[] {new ItemStack(Item.ingotIron), new ItemStack(Item.diamond), new ItemStack(FCBetterThanWolves.fcItemCreeperOysters)});
     FCRecipes.AddCauldronRecipe(new ItemStack(FCBetterThanWolves.fcItemIngotDiamond), new ItemStack[] {new ItemStack(Item.ingotIron), new ItemStack(Item.diamond), new ItemStack(FCBetterThanWolves.fcItemCreeperOysters)});
     
     FCRecipes.RemoveVanillaRecipe(new ItemStack(Item.bed, 1), new Object[] {"###", "XXX", '#', Block.cloth, 'X', Block.planks});
     FCRecipes.AddVanillaRecipe(new ItemStack(Item.bed, 1), new Object[] {"sss", "ppp", "www", 's', gpeItemSilk, 'p', FCBetterThanWolves.fcPadding, 'w', Block.woodSingleSlab});
     
     BlockDispenser.dispenseBehaviorRegistry.putObject(gpeItemLooseRock, new GPEBehaviorRock());
 
     // GitHub [#1]: fcBlockAxle got changed to fcAxleBlock
     try
     {
       try
       {
         compatAxleBlock = (Block)FCBetterThanWolves.class.getField("fcAxleBlock").get(null);
       }
       catch (NoSuchFieldException e)
       {
         compatAxleBlock = (Block)FCBetterThanWolves.class.getField("fcBlockAxle").get(null);
       }
     }
     catch (Exception e)
     {
       FCAddOnHandler.LogMessage("Error while retrieving Axle Block, assuming ID=247");
       compatAxleBlock = Block.blocksList[247];
     }
     
     FCAddOnHandler.LogMessage("Grom PE's BTWTweak is done tweaking. Enjoy!");
   }
 
   public void PostInitialize()
   {
     FCAddOnHandler.LogMessage("BTWTweak now looks for BTW Research Add-On to integrate with...");
     try
     {
       Class rb = Class.forName("SixModResearchBenchAddOn");
       Method addDesc = rb.getMethod("addResearchDescription", new Class[] {String.class, String.class});
       // addDesc.invoke(rb, "<KEY>", "<DESCRIPTION>");
       // Where <KEY> is of the form "ID#Metadata", (i.e. "5#3" for jungle planks) 
       // and <DESCRIPTION> is the description you want the item to have. 
       // Try to keep the description under 300 characters or so.
       addDesc.invoke(rb, "4", "Rough broken down stone. If arranged in a hollow square, can be crafted into a simple furnace to smelt and cook things.");
       addDesc.invoke(rb, "13", "Fine chunks of stone and sand. Digging through it may yield a rock, or even a flint, but passing it through a fine filter is more efficient.");
       addDesc.invoke(rb, "44#3", "A half high block of cobblestone, useful for getting up slopes without jumping. Two of them can be joined back into a single block. Many solid blocks seem to be able to be made into slabs like this.");
       addDesc.invoke(rb, "44#5", "A half high block of stone bricks, useful for getting up slopes without jumping. Two of them can be joined back into a single block. Many solid blocks seem to be able to be made into slabs like this.");
       addDesc.invoke(rb, "80", "This block of compacted snow seems to melt into water when placed directly nearby a heat source.");
       addDesc.invoke(rb, "91", "The torch encased in this carved pumpkin gives off a comforting light, and is well-protected from water. The pumpkin is still fragile and if dropped from a height, breaks into seeds and leaves the torch standing in most cases.");
       addDesc.invoke(rb, "397#5", "Spider head now silently stares at you with its all 8 eyes.");
       addDesc.invoke(rb, "397#6", "Looks like this black person won't be teleporting anymore.");
       addDesc.invoke(rb, "397#7", "Zombie pigman head smells rotten.");
       addDesc.invoke(rb, "397#8", "Blaze head is one cool-looking fiery trophy.");
       addDesc.invoke(rb, Integer.toString(gpeLooseRockID), "A rough loose rock. It could be crafted with shafts to make basic tools, one for a shovel, two for an axe or three for a pick. Heavy, but usable as a short ranged thrown weapon. Can be assembled to slabs and blocks.");
       addDesc.invoke(rb, Integer.toString(gpeSilkID), "Fine vowen spider silk. You wonder what could it be used for...");
       addDesc.invoke(rb, Integer.toString(FCBetterThanWolves.fcBlockDirtSlab.blockID) + "#6", "Gravel makes for a good road material, even in a slab form.");
       addDesc.invoke(rb, Integer.toString(FCBetterThanWolves.fcBlockDirtSlab.blockID) + "#7", "Running and jumping is a huge drain on energy and cuts into a food supply fast. Slabs could offer a huge help in this, allowing one to walk up slopes without any jumping at all. Needs a solid surface to sit on though.");
       addDesc.invoke(rb, Integer.toString(FCBetterThanWolves.fcPotash.itemID), "Grainy ash substance from rendered down wood. Can fertilize tilled soil. Also has a bleaching quality, so should be able to bleach coloured wool white.");
       addDesc.invoke(rb, Integer.toString(FCBetterThanWolves.fcAestheticOpaque.blockID) + "#4", "Nice, soft and comfy. A handy way to store padding. Or to make a calming padded room. Softens the blow when landed onto.");
       FCAddOnHandler.LogMessage("All good!");
     }
     catch (ClassNotFoundException e)
     {
       FCAddOnHandler.LogMessage("BTW Research Add-On not found.");
     }
     catch (Exception e)
     {
       FCAddOnHandler.LogMessage("Error while integrating with BTW Research Add-On!");
       e.printStackTrace();
     }    
   }
 
   public static boolean onBlockSawed(World world, int x, int y, int z)
   {
     int id = world.getBlockId(x, y, z);
     int meta = world.getBlockMetadata(x, y, z);
     if (id == Block.bookShelf.blockID)
     {
       EjectSawProducts(world, x, y, z, FCBetterThanWolves.fcBlockWoodSidingItemStubID, 0, 4);
       EjectSawProducts(world, x, y, z, Item.book.itemID, 0, 3);
     }
     else if (id == Block.chest.blockID)
     {
       EjectSawProducts(world, x, y, z, FCBetterThanWolves.fcBlockWoodSidingItemStubID, 0, 6);
     }
     else if (id == Block.doorWood.blockID)
     {
       if ((meta & 8) != 0) return false;
       EjectSawProducts(world, x, y, z, FCBetterThanWolves.fcBlockWoodSidingItemStubID, 0, 4);
     }
     else if (id == Block.fenceGate.blockID)
     {
       EjectSawProducts(world, x, y, z, FCBetterThanWolves.fcBlockWoodMouldingItemStubID, 0, 3);
     }
     else if (id == Block.jukebox.blockID)
     {
       EjectSawProducts(world, x, y, z, FCBetterThanWolves.fcBlockWoodSidingItemStubID, 0, 6);
       EjectSawProducts(world, x, y, z, Item.diamond.itemID, 0, 1);
     }
     else if (id == Block.ladder.blockID)
     {
       EjectSawProducts(world, x, y, z, Item.stick.itemID, 0, 2);
     }
     else if (id == Block.music.blockID)
     {
       EjectSawProducts(world, x, y, z, FCBetterThanWolves.fcBlockWoodSidingItemStubID, 0, 6);
       EjectSawProducts(world, x, y, z, FCBetterThanWolves.fcItemRedstoneLatch.itemID, 0, 1);
     }
     else if (id == Block.trapdoor.blockID)
     {
       EjectSawProducts(world, x, y, z, FCBetterThanWolves.fcBlockWoodSidingItemStubID, 0, 2);
     }
     else if (id == compatAxleBlock.blockID)
     {
       EjectSawProducts(world, x, y, z, FCBetterThanWolves.fcBlockWoodCornerItemStubID, 0, 2);
       EjectSawProducts(world, x, y, z, FCBetterThanWolves.fcRopeItem.itemID, 0, 1);
     }
     else if (id == FCBetterThanWolves.fcBellows.blockID)
     {
       EjectSawProducts(world, x, y, z, FCBetterThanWolves.fcBlockWoodSidingItemStubID, 0, 2);
       EjectSawProducts(world, x, y, z, FCBetterThanWolves.fcItemTannedLeatherCut.itemID, 0, 3);
       EjectSawProducts(world, x, y, z, FCBetterThanWolves.fcGear.itemID, 0, 1);
       EjectSawProducts(world, x, y, z, FCBetterThanWolves.fcBelt.itemID, 0, 1);
     }
     else if (id == FCBetterThanWolves.fcGearBox.blockID)
     {
       EjectSawProducts(world, x, y, z, FCBetterThanWolves.fcBlockWoodSidingItemStubID, 0, 3);
       EjectSawProducts(world, x, y, z, FCBetterThanWolves.fcGear.itemID, 0, 3);
       EjectSawProducts(world, x, y, z, FCBetterThanWolves.fcItemRedstoneLatch.itemID, 0, 1);
     }
     else if (id == FCBetterThanWolves.fcHopper.blockID)
     {
       EjectSawProducts(world, x, y, z, FCBetterThanWolves.fcBlockWoodMouldingItemStubID, 0, 3);
       EjectSawProducts(world, x, y, z, FCBetterThanWolves.fcGear.itemID, 0, 1);
       EjectSawProducts(world, x, y, z, Block.pressurePlatePlanks.blockID, 0, 1);
     }
     else if (id == FCBetterThanWolves.fcPlatform.blockID)
     {
       EjectSawProducts(world, x, y, z, FCBetterThanWolves.fcBlockWoodMouldingItemStubID, 0, 3);
       EjectSawProducts(world, x, y, z, FCBetterThanWolves.fcWicker.itemID, 0, 2);
     }
     else if (id == FCBetterThanWolves.fcPulley.blockID)
     {
       EjectSawProducts(world, x, y, z, FCBetterThanWolves.fcBlockWoodSidingItemStubID, 0, 3);
       EjectSawProducts(world, x, y, z, Item.ingotIron.itemID, 0, 2);
       EjectSawProducts(world, x, y, z, FCBetterThanWolves.fcGear.itemID, 0, 1);
       EjectSawProducts(world, x, y, z, FCBetterThanWolves.fcItemRedstoneLatch.itemID, 0, 1);
     }
     else if (id == FCBetterThanWolves.fcSaw.blockID)
     {
       EjectSawProducts(world, x, y, z, FCBetterThanWolves.fcGear.itemID, 0, 2);
       EjectSawProducts(world, x, y, z, Item.ingotIron.itemID, 0, 3);
       EjectSawProducts(world, x, y, z, FCBetterThanWolves.fcBlockWoodSidingItemStubID, 0, 1);
       EjectSawProducts(world, x, y, z, FCBetterThanWolves.fcBelt.itemID, 0, 1);
     }
     else if (id == FCBetterThanWolves.fcBlockScrewPump.blockID)
     {
       EjectSawProducts(world, x, y, z, FCBetterThanWolves.fcBlockWoodSidingItemStubID, 0, 3);
       EjectSawProducts(world, x, y, z, FCBetterThanWolves.fcGrate.itemID, 0, 1);
       EjectSawProducts(world, x, y, z, FCBetterThanWolves.fcItemScrew.itemID, 0, 1);
     }
     else if (id == Block.cloth.blockID)
     {
       EjectSawProducts(world, x, y, z, FCBetterThanWolves.fcWoolSlab.blockID, meta, 2);
     }
     else
     {
       return false;
     }
     return true;
   }
 
   private static void EjectSawProducts(World world, int x, int y, int z, int id, int meta, int count)
   {
     for (int i = 0; i < count; ++i)
     {
       FCUtilsItem.EjectSingleItemWithRandomOffset(world, x, y, z, id, meta);
     }
   }
 
   public void OnLanguageLoaded(StringTranslate st)
   {
     Properties t = st.GetTranslateTable();
     t.put(Item.stick.getUnlocalizedName() + ".name", "Rod");
     t.put(FCBetterThanWolves.fcBlockDirtSlab.getUnlocalizedName() + ".gravel.name", "Gravel Slab");
     t.put(FCBetterThanWolves.fcBlockDirtSlab.getUnlocalizedName() + ".sand.name", "Sand Slab");
     t.put(FCBetterThanWolves.fcItemRottenArrow.getUnlocalizedName() + ".name", "Rotten Arrow");
     t.put("item.skull.spider.name", "Spider head");
     t.put("item.skull.enderman.name", "Enderman head");
     t.put("item.skull.pigzombie.name", "Zombie Pigman head");
     t.put("item.skull.fire.name", "Blaze head");
     t.put(gpeItemLooseRock.getUnlocalizedName() + ".name", "Rock");
     t.put(gpeItemSilk.getUnlocalizedName() + ".name", "Silk");
   }
 
   public static void saveWorldData(World world)
   {
     if (!readyForStrataRegen(world)) return;
     File strataRegenFile = getStrataRegenFile(world);
 
     try
     {
       ObjectOutputStream s = new ObjectOutputStream(new FileOutputStream(strataRegenFile));
       s.writeObject(chunkRegenInfo);
       s.close();
     }
     catch (Exception e)
     {
       e.printStackTrace();
     }
   }
 
   public static void loadWorldData(World world)
   {
     if (!readyForStrataRegen(world)) return;
     FCAddOnHandler.LogMessage(String.format("BTWTweak is now going to stratify the world '%s'.", gpeStrataRegenWorldName));
     File strataRegenFile = getStrataRegenFile(world);
     if (!strataRegenFile.exists()) return;
 
     try
     {
       ObjectInputStream s = new ObjectInputStream(new FileInputStream(strataRegenFile));
       chunkRegenInfo = (HashMap<Long, Integer>)s.readObject();
       s.close();
     }
     catch (Exception e)
     {
       e.printStackTrace();
     }
   }
 
   public static void onSaveChunk(World world, Chunk chunk)
   {
     if (!readyForStrataRegen(world)) return;
     long l = (((long)chunk.xPosition) << 32) | (chunk.zPosition & 0xffffffffL);
     chunkRegenInfo.put(l, gpeStrataRegenKey);
   }
 
   public static void onLoadChunk(World world, Chunk chunk)
   {
     if (!readyForStrataRegen(world)) return;
     long l = (((long)chunk.xPosition) << 32) | (chunk.zPosition & 0xffffffffL);
     Integer key = chunkRegenInfo.get(l);
     if (key == null || key.intValue() != gpeStrataRegenKey)
     {
       System.out.println(String.format("Stratifying chunk at %d, %d", chunk.xPosition, chunk.zPosition));
       stratifyChunk(world, chunk);
     }
   }
 
  // Simply ignoring letters for now
   public static boolean isBTWVersionOrNewer(String ver)
   {
    String current = FCBetterThanWolves.fcVersionString.replaceAll("[^\\d\\.]+", "");
    return Double.parseDouble(current) >= Double.parseDouble(ver);
   }
 
   private static File getStrataRegenFile(World world)
   {
     File f;
     f = new File(proxy.getConfigDir(), "saves");
     f = new File(f, world.getSaveHandler().getWorldDirectoryName());
     f = new File(f, "strataRegen.dat");
     return f;
   }
 
   private static boolean readyForStrataRegen(World world)
   {
     if (gpeStrataRegenKey == 0) return false;
     if (world.provider.dimensionId != 0) return false;
     if (world.worldInfo == null) return false;
     return world.worldInfo.getWorldName().equals(gpeStrataRegenWorldName);
   }
 
   private static void stratifyChunk(World world, Chunk chunk)
   {
     for (int x = 0; x < 16; x++)
     {
       for (int z = 0; z < 16; z++)
       {
         int y = 1;
         int yy;
         for (yy = 24 + world.rand.nextInt(2); y <= yy; y++) stratifyBlockInChunk(chunk, x, y, z, 2);
         for (yy = 48 + world.rand.nextInt(2); y <= yy; y++) stratifyBlockInChunk(chunk, x, y, z, 1);
       }
     }
   }
 
   private static void stratifyBlockInChunk(Chunk chunk, int x, int y, int z, int strata)
   {
     int id = chunk.getBlockID(x, y, z);
     if (id == Block.stone.blockID)
     {
       chunk.setBlockMetadata(x, y, z, strata);
     }
     else if (id != 0)
     {
       Block b = Block.blocksList[id];
       if (b.HasStrata()) chunk.setBlockMetadata(x, y, z, b.GetMetadataConversionForStrataLevel(strata, 0));
     }
   }
 
 }
