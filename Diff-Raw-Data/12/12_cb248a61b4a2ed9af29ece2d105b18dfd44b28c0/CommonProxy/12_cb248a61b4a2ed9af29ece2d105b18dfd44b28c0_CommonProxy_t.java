 package com.dmillerw.remoteIO.core.proxy;
 
 import ic2.api.item.Items;
 
 import java.util.ArrayList;
 import java.util.List;
 
 import net.minecraft.block.Block;
 import net.minecraft.item.Item;
 import net.minecraft.item.ItemStack;
 import net.minecraftforge.oredict.OreDictionary;
 import net.minecraftforge.oredict.ShapedOreRecipe;
 import universalelectricity.prefab.block.BlockConductor;
 
 import com.dmillerw.remoteIO.RemoteIO;
 import com.dmillerw.remoteIO.block.BlockHandler;
 import com.dmillerw.remoteIO.block.tile.TileHeater;
 import com.dmillerw.remoteIO.block.tile.TileIO;
 import com.dmillerw.remoteIO.block.tile.TileRemoteInventory;
 import com.dmillerw.remoteIO.block.tile.TileReservoir;
 import com.dmillerw.remoteIO.block.tile.TileSideProxy;
 import com.dmillerw.remoteIO.core.helper.IOLogger;
 import com.dmillerw.remoteIO.core.helper.StackHelper;
 import com.dmillerw.remoteIO.item.ItemHandler;
 import com.dmillerw.remoteIO.item.ItemUpgrade.Upgrade;
 
 import cpw.mods.fml.common.Loader;
 import cpw.mods.fml.common.event.FMLInitializationEvent;
 import cpw.mods.fml.common.event.FMLPostInitializationEvent;
 import cpw.mods.fml.common.event.FMLPreInitializationEvent;
 import cpw.mods.fml.common.registry.GameRegistry;
 
 public class CommonProxy implements ISidedProxy {
 
 	@Override
 	public void preInit(FMLPreInitializationEvent event) {
 		if (BlockHandler.blockIOID != 0) {
 			GameRegistry.registerTileEntity(TileIO.class, "blockIO");
 		}
 		
 		if (BlockHandler.blockMachineID != 0) {
 			GameRegistry.registerTileEntity(TileHeater.class, "blockMachine_heater");
 			GameRegistry.registerTileEntity(TileReservoir.class, "blockMachine_reservoir");
 		}
 		
 		if (BlockHandler.blockProxyID != 0) {
 			GameRegistry.registerTileEntity(TileSideProxy.class, "blockProxy");
 		}
 		
 		if (BlockHandler.blockWirelessID != 0) {
 			GameRegistry.registerTileEntity(TileRemoteInventory.class, "blockRemote");
 		}
 	}
 
 	@Override
 	public void init(FMLInitializationEvent event) {
 		if (BlockHandler.blockIOID != 0) {
 			GameRegistry.addRecipe(new ItemStack(BlockHandler.blockIO, 2, 0), new Object[] {"SIS", "ESE", "SIS", 'S', Block.stone, 'I', Block.blockIron, 'E', Item.enderPearl});
 		}
 		
 		if (BlockHandler.blockMachineID != 0) {
 			GameRegistry.addRecipe(new ItemStack(BlockHandler.blockMachine, 1, 0), new Object[] {"SIS", "IFI", "SBS", 'S', Block.cobblestone, 'I', Block.fenceIron, 'F', Block.furnaceIdle, 'B', Item.bucketLava});
 			GameRegistry.addRecipe(new ItemStack(BlockHandler.blockMachine, 1, 1), new Object[] {"SFS", "FFF", "SBS", 'S', Block.cobblestone, 'F', Block.glass, 'B', Item.bucketWater});
 		}
 		
 		if (BlockHandler.blockProxyID != 0) {
 			GameRegistry.addRecipe(new ItemStack(BlockHandler.blockProxy, 4, 0), new Object[] {" E ", "1I2", 'E', Item.enderPearl, '1', Upgrade.ISIDED_AWARE.toItemStack(), '2', Upgrade.FLUID.toItemStack(), 'I', Block.hopperBlock});
 			GameRegistry.addRecipe(new ItemStack(BlockHandler.blockProxy, 4, 0), new Object[] {" E ", "1I2", 'E', Item.enderPearl, '2', Upgrade.ISIDED_AWARE.toItemStack(), '1', Upgrade.FLUID.toItemStack(), 'I', Block.hopperBlock});
 		}
 		
 		// Wrench
 		GameRegistry.addRecipe(new ItemStack(ItemHandler.itemTool), new Object[] {"EB ", "BI ", "  R", 'E', Item.enderPearl, 'B', Item.dyePowder, 'I', Item.ingotIron, 'R', Item.redstone});
 	
 		// IO Goggles
 		GameRegistry.addRecipe(new ItemStack(ItemHandler.itemGoggles), new Object[] {"L L", "I I", "GEG", 'L', Item.leather, 'I', Item.ingotIron, 'G', Block.thinGlass, 'E', Item.enderPearl});
 		
 		// Blank Upgrade
 		GameRegistry.addRecipe(new ShapedOreRecipe(StackHelper.resize(Upgrade.BLANK.toItemStack(), 16), "GCG", "IRI", "IRI", 'G', Item.goldNugget, 'I', Item.ingotIron, 'C', "dyeGreen", 'R', Item.redstone));
 	
 		for (Upgrade upgrade : Upgrade.values()) {
 			if (upgrade.recipeComponents != null && upgrade.recipeComponents.length == 1) {
 				GameRegistry.addRecipe(upgrade.toItemStack(), "C", "U", "C", 'C', upgrade.recipeComponents[0], 'U', Upgrade.BLANK.toItemStack());
 			} else if (upgrade.recipeComponents != null && upgrade.recipeComponents.length == 2) {
 				GameRegistry.addRecipe(upgrade.toItemStack(), "C", "U", "D", 'C', upgrade.recipeComponents[0], 'D', upgrade.recipeComponents[1], 'U', Upgrade.BLANK.toItemStack());
 				GameRegistry.addRecipe(upgrade.toItemStack(), "D", "U", "C", 'C', upgrade.recipeComponents[0], 'D', upgrade.recipeComponents[1], 'U', Upgrade.BLANK.toItemStack());
 			}
 		}
 		
 		// Wireless Transceiver
 		GameRegistry.addRecipe(new ItemStack(ItemHandler.itemTransmitter), new Object[] {" E ", "III", "IRI", 'E', Item.enderPearl, 'I', Item.ingotIron, 'R', Item.redstone});
 		
 		// Remote Inventory
 		GameRegistry.addRecipe(new ItemStack(BlockHandler.blockWireless), new Object[] {" U ", "III", "ITI", 'U', Upgrade.ITEM.toItemStack(), 'I', Item.ingotIron, 'T', new ItemStack(ItemHandler.itemTransmitter)});
 		
 		// Iron Rod component
 		GameRegistry.addRecipe(new ItemStack(ItemHandler.itemComponent, 1, 6), new Object[] {"I  ", " I ", "  I", 'I', Item.ingotIron});
 		OreDictionary.registerOre("rodIron", new ItemStack(ItemHandler.itemComponent, 1, 2));
 		
 		// Camo Component
 		GameRegistry.addRecipe(new ShapedOreRecipe(new ItemStack(ItemHandler.itemComponent, 1, 0), new Object[] {"LSL", "SIS", "LSL", 'L', "logWood", 'S', "stone", 'I', Item.ingotIron}));
 		
 		// Padlock Component
 		GameRegistry.addRecipe(new ShapedOreRecipe(new ItemStack(ItemHandler.itemComponent, 1, 1), new Object[] {"   ", " R ", " I ", 'R', "rodIron", 'I', Item.ingotIron}));
 	}
 
 	@Override
 	public void postInit(FMLPostInitializationEvent event) {
 		// If EnderStorage detected, replace Dimensional Upgrade recipe
 		if (Loader.isModLoaded("EnderStorage")) {
 			ItemStack obsidian = new ItemStack(Block.obsidian);
 			ItemStack enderChest = null;
 			
 			try {
 				Class clazz = Class.forName("codechicken.enderstorage.EnderStorage");
 				enderChest = new ItemStack((Block)clazz.getDeclaredField("blockEnderChest").get(clazz), 1, OreDictionary.WILDCARD_VALUE);
 			} catch(Exception ex) {
 				// IGNORING
 			}
 			
 			if (enderChest != null) {
 				GameRegistry.addRecipe(Upgrade.CROSS_DIMENSIONAL.toItemStack(), "C", "U", "D", 'C', enderChest, 'D', obsidian, 'U', Upgrade.BLANK.toItemStack());
 				GameRegistry.addRecipe(Upgrade.CROSS_DIMENSIONAL.toItemStack(), "D", "U", "C", 'C', enderChest, 'D', obsidian, 'U', Upgrade.BLANK.toItemStack());
 			} else {
 				IOLogger.warn("Tried to get Ender Storage EnderChest, but failed!");
 			}
 		}
 		
 		// If Buildcraft detected, add BC Power Upgrade recipe
 		if (Loader.isModLoaded("BuildCraft|Core")) {
 			String[] pipeTypes = new String[] {"Wood", "Cobblestone", "Stone", "Quartz", "Iron", "Gold", "Diamond"};
 			ItemStack[] pipes = new ItemStack[pipeTypes.length];
 			boolean failed = false;
 			
 			try {
 				Class clazz = Class.forName("buildcraft.BuildCraftTransport");
 				
 				for (int i=0; i<pipeTypes.length; i++) {
 					pipes[i] = new ItemStack((Item)clazz.getDeclaredField("pipePower" + pipeTypes[i]).get(clazz));
 				}
 			} catch(Exception ex) {
 				IOLogger.warn("Tried to get Buildcraft power pipes, but failed! Buildcraft support will not be available!");
 				IOLogger.warn("Reason: " + ex.getMessage());
 				failed = true;
 			}
 			
 			if (!failed) {
 				for (ItemStack pipe : pipes) {
 					GameRegistry.addRecipe(Upgrade.POWER_MJ.toItemStack(), "C", "U", "C", 'C', pipe, 'U', Upgrade.BLANK.toItemStack());
 				}
 			}
 		}
 		
 		// If IC2 detected, add EU Power Upgrade recipe
 		if (Loader.isModLoaded("IC2")) {
 			String[] cableTypes = new String[] {"copper", "insulatedCopper", "gold", "insulatedGold", "iron", "insulatedIron", "insulatedTin", "glassFiber", "tin"};
 			ItemStack[] cables = new ItemStack[cableTypes.length];
 			boolean failed = false;
 			
 			try {
 				for (int i=0; i<cableTypes.length; i++) {
 					cables[i] = Items.getItem(cableTypes[i] + "CableItem");
 				}
 			} catch(Exception ex) {
 				IOLogger.warn("Tried to get IC2 power cables, but failed! IC2 support will not be available!");
 				IOLogger.warn("Reason: " + ex.getMessage());
 				failed = true;
 			}
 			
 			if (!failed) {
 				for (ItemStack cable : cables) {
 					GameRegistry.addRecipe(Upgrade.POWER_EU.toItemStack(), "C", "U", "C", 'C', cable, 'U', Upgrade.BLANK.toItemStack());
 				}
 			}
 		}
 		
 		// If ThermalExpansion detected, add RF Power Upgrade recipe
 		if (Loader.isModLoaded("ThermalExpansion")) {
			final String conduitPrefix = "conduitEnergy";
			String[] conduitStrings = new String[] {"Basic", "Hardened", "Reinforced"};
			ItemStack[] conduits = new ItemStack[conduitStrings.length];
 			boolean failed = false;
 			
 			try {
				Class clazz = Class.forName("thermalexpansion.block.conduit.BlockConduit");
				for (int i=0; i<conduitStrings.length; i++) {
					conduits[i] = (ItemStack) clazz.getDeclaredField(conduitPrefix + conduitStrings[i]).get(clazz);
 				}
 			} catch(Exception ex) {
 				IOLogger.warn("Tried to get Thermal Expansion power conduits, but failed! Thermal Expansion support will not be available!");
 				IOLogger.warn("Reason: " + ex.getMessage());
 				failed = true;
 			}
 			
 			if (!failed) {
 				for (ItemStack conduit : conduits) {
 					GameRegistry.addRecipe(Upgrade.POWER_RF.toItemStack(), "C", "U", "C", 'C', conduit, 'U', Upgrade.BLANK.toItemStack());
 				}
 			}
 		}
 		
 		// Universal Electricty management
 		List<Block> conductorBlocks = new ArrayList<Block>();
 
 		for (Block block : Block.blocksList) {
 			if (block instanceof BlockConductor) conductorBlocks.add(block);
 		}
 		
 		if (conductorBlocks.size() > 0) {
 			for (Block block : conductorBlocks) {
 				GameRegistry.addRecipe(Upgrade.POWER_UE.toItemStack(), "C", "U", "C", 'C', block, 'U', Upgrade.BLANK.toItemStack());
 			}
 		}
 	}
 
 }
