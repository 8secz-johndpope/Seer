 /** 
  * Copyright (C) 2011-2012 Flow86
  * 
  * AdditionalBuildcraftObjects is open-source.
  *
  * It is distributed under the terms of my Open Source License. 
  * It grants rights to read, modify, compile or run the code. 
  * It does *NOT* grant the right to redistribute this software or its 
  * modifications in any form, binary or source, except if expressively
  * granted by the copyright holder.
  */
 
 package abo;
 
 import java.io.File;
 import java.util.LinkedList;
 import java.util.logging.Logger;
 
 import net.minecraft.src.Block;
 import net.minecraft.src.Item;
 import net.minecraft.src.ItemStack;
 import net.minecraftforge.common.Configuration;
 import net.minecraftforge.common.Property;
 import abo.pipes.PipeItemsBounce;
 import abo.pipes.PipeItemsCrossover;
 import abo.pipes.PipeItemsExtraction;
 import abo.pipes.PipeItemsInsertion;
 import abo.pipes.PipeItemsRoundRobin;
 import abo.pipes.PipeLiquidsGoldenIron;
 import abo.proxy.ABOProxy;
 import abo.triggers.ABOTriggerProvider;
 import abo.triggers.TriggerEngineControl;
 import buildcraft.BuildCraftCore;
 import buildcraft.BuildCraftTransport;
 import buildcraft.api.gates.ActionManager;
 import buildcraft.api.gates.Trigger;
 import buildcraft.core.utils.Localization;
 import buildcraft.transport.BlockGenericPipe;
 import buildcraft.transport.ItemPipe;
 import buildcraft.transport.Pipe;
 import buildcraft.transport.blueprints.BptItemPipeIron;
 import buildcraft.transport.blueprints.BptItemPipeWooden;
 import cpw.mods.fml.common.FMLLog;
 import cpw.mods.fml.common.Mod;
 import cpw.mods.fml.common.Mod.Init;
 import cpw.mods.fml.common.Mod.Instance;
 import cpw.mods.fml.common.Mod.PreInit;
 import cpw.mods.fml.common.event.FMLInitializationEvent;
 import cpw.mods.fml.common.event.FMLPreInitializationEvent;
 import cpw.mods.fml.common.registry.GameRegistry;
 import cpw.mods.fml.common.registry.LanguageRegistry;
 
 /**
  * @author Flow86
  * 
  */
 @Mod(modid = "Additional-Buildcraft-Objects", name = "Additional-Buildcraft-Objects", version = "@ABO_VERSION@", dependencies = "required-after:BuildCraft|Transport")
 public class ABO {
 	public static final String VERSION = "@ABO_VERSION@";
 
 	public static ABOConfiguration aboConfiguration;
 	public static Logger aboLog = Logger.getLogger("Additional-Buildcraft-Objects");
 
 	public static String texturePipes = "/gfx/abo/pipes.png";;
 	public static String textureTriggers = "/gfx/abo/triggers.png";;
 
 	// public static int pipeLiquidsValveID = 10200;
 	// public static Item pipeLiquidsValve = null;
 
 	public static int pipeLiquidsGoldenIronID = 10201;
 	public static Item pipeLiquidsGoldenIron = null;
 
 	// public static int pipeLiquidsBalanceID = 10203;
 	// public static Item pipeLiquidsBalance = null;
 
 	// public static int pipeLiquidsDiamondID = 10204;
 	// public static Item pipeLiquidsDiamond = null;
 
 	public static int pipeItemsRoundRobinID = 10300;
 	public static Item pipeItemsRoundRobin = null;
 
 	// public static int pipeItemsCompactorID = 10301;
 	// public static Item pipeItemsCompactor = null;
 
 	public static int pipeItemsInsertionID = 10302;
 	public static Item pipeItemsInsertion = null;
 
 	public static int pipeItemsExtractionID = 10303;
 	public static Item pipeItemsExtraction = null;
 
 	public static int pipeItemsBounceID = 10304;
 	public static Item pipeItemsBounce = null;
 
 	public static int pipeItemsCrossoverID = 10305;
 	public static Item pipeItemsCrossover = null;
 
 	// public static int pipePowerSwitchID = 10400;
 	// public static Item pipePowerSwitch = null;
 
 	// public static int triggerEngineControlID = 128;
 	// public static Trigger triggerEngineControl = null;
 	//
 	// public static int actionSwitchOnPipeID = 128;
 	// public static Action actionSwitchOnPipe = null;
 
 	public static int triggerEngineControlID = 128;
 	public static Trigger triggerEngineControl = null;
 
 	@Instance("Additional-Buildcraft-Objects")
 	public static ABO instance;
 
 	@PreInit
 	public void preInitialize(FMLPreInitializationEvent evt) {
 
 		aboLog.setParent(FMLLog.getLogger());
 		aboLog.info("Starting Additional-Buildcraft-Objects #@BUILD_NUMBER@ " + VERSION + " for Buildcraft @BUILDCRAFT_VERSION@ and Forge @FORGE_VERSION@");
 		aboLog.info("Copyright (c) Flow86, 2012");
 
 		aboConfiguration = new ABOConfiguration(new File(evt.getModConfigurationDirectory(), "abo/main.conf"));
 		try {
 			aboConfiguration.load();
 
 			// pipeLiquidsValve = createPipe(pipeLiquidsValveID,
 			// PipeLiquidsValve.class, "Valve Pipe", 1,
 			// BuildCraftTransport.pipeLiquidsWood, Block.lever,
 			// BuildCraftTransport.pipeLiquidsWood);
 
 			pipeLiquidsGoldenIron = createPipe(pipeLiquidsGoldenIronID, PipeLiquidsGoldenIron.class, "Golden Iron Waterproof Pipe", 1,
 					BuildCraftTransport.pipeLiquidsGold, BuildCraftTransport.pipeLiquidsIron, null);
 
 			// pipeLiquidsBalance = createPipe(pipeLiquidsBalanceID,
 			// PipeLiquidsBalance.class, "Balance Pipe", 1,
 			// BuildCraftTransport.pipeLiquidsWood,
 			// new ItemStack(BuildCraftEnergy.engineBlock, 1, 0),
 			// BuildCraftTransport.pipeLiquidsWood);
 
 			// pipeLiquidsDiamond = createPipe(pipeLiquidsDiamondID,
 			// PipeLiquidsDiamond.class, "Diamond Liquids Pipe", 1,
 			// BuildCraftTransport.pipeItemsDiamond,
 			// BuildCraftTransport.pipeWaterproof, null);
 
 			pipeItemsRoundRobin = createPipe(pipeItemsRoundRobinID, PipeItemsRoundRobin.class, "RoundRobin Transport Pipe", 1,
 					BuildCraftTransport.pipeItemsStone, Block.gravel, null);
 
 			// pipeItemsCompactor = createPipe(pipeItemsCompactorID,
 			// PipeItemsCompactor.class, "Compactor Pipe", 1,
 			// BuildCraftTransport.pipeItemsStone,
 			// Block.pistonBase, null);
 
 			pipeItemsInsertion = createPipe(pipeItemsInsertionID, PipeItemsInsertion.class, "Insertion Pipe", 1, BuildCraftTransport.pipeItemsStone,
 					Item.redstone, null);
 
 			pipeItemsExtraction = createPipe(pipeItemsExtractionID, PipeItemsExtraction.class, "Extraction Transport Pipe", 1,
 					BuildCraftTransport.pipeItemsStone, Block.planks, null);
 
 			pipeItemsBounce = createPipe(pipeItemsBounceID, PipeItemsBounce.class, "Bounce Transport Pipe", 1, BuildCraftTransport.pipeItemsStone,
 					Block.cobblestone, null);
 
 			pipeItemsCrossover = createPipe(pipeItemsCrossoverID, PipeItemsCrossover.class, "Crossover Transport Pipe", 1, BuildCraftTransport.pipeItemsStone,
 					BuildCraftTransport.pipeItemsIron, null);
 
 			// pipePowerSwitch = createPipe(pipePowerSwitchID,
 			// PipePowerSwitch.class, "Power Switch Pipe", 1,
 			// BuildCraftTransport.pipePowerGold, Block.lever, null);
 
 			triggerEngineControl = new TriggerEngineControl(triggerEngineControlID);
 			// actionSwitchOnPipe = new
 			// ActionSwitchOnPipe(actionSwitchOnPipeID);
 
 			ActionManager.registerTriggerProvider(new ABOTriggerProvider());
 
 			BuildCraftCore.itemBptProps[pipeItemsExtraction.shiftedIndex] = new BptItemPipeWooden();
 			// BuildCraftCore.itemBptProps[pipeLiquidsValve.shiftedIndex] = new
 			// BptItemPipeIron();
 			BuildCraftCore.itemBptProps[pipeLiquidsGoldenIron.shiftedIndex] = new BptItemPipeIron();
 			// BuildCraftCore.itemBptProps[pipeLiquidsDiamond.shiftedIndex] =
 			// new BptItemPipeDiamond();
 		} finally {
 			aboConfiguration.save();
 		}
 	}
 
 	@Init
 	public void load(FMLInitializationEvent evt) {
 
 		Localization.addLocalization("/lang/abo/", "en_US");
 
 		ABOProxy.proxy.registerTileEntities();
 		ABOProxy.proxy.registerRenderers();
		loadRecipes();
 	}
 
 	private static class PipeRecipe {
 		boolean isShapeless = false;
 		ItemStack result;
 		Object[] input;
 	}
 
 	private static LinkedList<PipeRecipe> pipeRecipes = new LinkedList<PipeRecipe>();
 
 	private static Item createPipe(int defaultID, Class<? extends Pipe> clas, String descr, int count, Object ingredient1, Object ingredient2,
 			Object ingredient3) {
 		String name = Character.toLowerCase(clas.getSimpleName().charAt(0)) + clas.getSimpleName().substring(1);
 
 		Property prop = aboConfiguration.getOrCreateIntProperty(name + ".id", Configuration.CATEGORY_ITEM, defaultID);
 
 		int id = prop.getInt(defaultID);
 		ItemPipe res = BlockGenericPipe.registerPipe(id, clas);
 		res.setItemName(clas.getSimpleName());
 		LanguageRegistry.addName(res, descr);
 
 		// Add appropriate recipe to temporary list
 		PipeRecipe recipe = new PipeRecipe();
 
 		if (ingredient1 != null && ingredient2 != null && ingredient3 != null) {
 			recipe.result = new ItemStack(res, count);
 			recipe.input = new Object[] { "ABC", Character.valueOf('A'), ingredient1, Character.valueOf('B'), ingredient2, Character.valueOf('C'), ingredient3 };
 
 			pipeRecipes.add(recipe);
 		} else if (ingredient1 != null && ingredient2 != null) {
 			recipe.isShapeless = true;
 			recipe.result = new ItemStack(res, count);
 			recipe.input = new Object[] { ingredient1, ingredient2 };
 
 			pipeRecipes.add(recipe);
 		}
 
 		return res;
 	}
 
 	public void loadRecipes() {
 		// Add pipe recipes
 		for (PipeRecipe pipe : pipeRecipes) {
 			if (pipe.isShapeless) {
 				GameRegistry.addShapelessRecipe(pipe.result, pipe.input);
 			} else {
 				GameRegistry.addRecipe(pipe.result, pipe.input);
 			}
 		}
 	}
 }
