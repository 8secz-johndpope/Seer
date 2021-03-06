 /*******************************************************************************
  * @author Reika Kalseki
  * 
  * Copyright 2013
  * 
  * All rights reserved.
  * Distribution of the software in any form is only allowed with
  * explicit, prior permission from the owner.
  ******************************************************************************/
 package Reika.DragonAPI.Libraries;
 
 import net.minecraft.block.Block;
 import net.minecraft.block.material.Material;
 import net.minecraft.entity.Entity;
 import net.minecraft.entity.item.EntityItem;
 import net.minecraft.entity.item.EntityXPOrb;
 import net.minecraft.inventory.IInventory;
 import net.minecraft.item.ItemStack;
 import net.minecraft.util.MathHelper;
 import net.minecraft.util.MovingObjectPosition;
 import net.minecraft.util.Vec3;
 import net.minecraft.world.World;
 import net.minecraft.world.biome.BiomeGenBase;
 import Reika.DragonAPI.DragonAPICore;
 import Reika.DragonAPI.Auxiliary.BlockProperties;
 
 public final class ReikaWorldHelper extends DragonAPICore {
 
 	public static boolean softBlocks(int id) {
 		BlockProperties.setSoft();
 		return (BlockProperties.softBlocksArray[id]);
 	}
 
 	public static boolean flammable(int id) {
 		BlockProperties.setFlammable();
 		return (BlockProperties.flammableArray[id]);
 	}
 
 	public static boolean nonSolidBlocks(int id) {
 		BlockProperties.setNonSolid();
 		return (BlockProperties.nonSolidArray[id]);
 	}
 
 	/** Converts the given block ID to a hex color. Renders ores (or disguises as stone) as requested.
 	 * Args: Block ID, Ore Rendering */
 	public static int blockColors(int id, boolean renderOres) {
 		BlockProperties.setBlockColors(renderOres);
 		if (BlockProperties.blockColorArray[id] == 0)
 			return 0xffD47EFF;
 		return BlockProperties.blockColorArray[id];
 	}
 
 	/** Converts the given coordinates to an RGB representation of those coordinates' biome's color, for the given material type.
 	 * Args: World, x, z, material (String) */
 	public static int[] biomeToRGB(World world, int x, int z, String material) {
 		BiomeGenBase biome = world.getBiomeGenForCoords(x, z);
 		int color = ReikaWorldHelper.biomeToHex(biome, material);
 		return ReikaGuiAPI.HexToRGB(color);
 	}
 
 	/** Converts the given coordinates to a hex representation of those coordinates' biome's color, for the given material type.
 	 * Args: World, x, z, material (String) */
 	public static int biomeToHexColor(World world, int x, int z, String material) {
 		BiomeGenBase biome = world.getBiomeGenForCoords(x, z);
 		int color = ReikaWorldHelper.biomeToHex(biome, material);
 		return color;
 	}
 
 	private static int biomeToHex(BiomeGenBase biome, String mat) {
 		int color = 0;
 		if (mat == "Leaves")
 			color = biome.getBiomeFoliageColor();
 		if (mat == "Grass")
 			color = biome.getBiomeGrassColor();
 		if (mat == "Water")
 			color = biome.getWaterColorMultiplier();
 		if (mat == "Sky")
 			color = biome.getSkyColorByTemp(biome.getIntTemperature());
 		return color;
 	}
 
 	/** Caps the metadata at a certain value (eg, for leaves, metas are from 0-11, but there are only 4 types, and each type has 3 metas).
 	 * Args: Initial metadata, cap (# of types) */
 	public static int capMetadata(int meta, int cap) {
 		while (meta >= cap)
 			meta -= cap;
 		return meta;
 	}
 
 	/** Finds the top edge of the top solid (nonair) block in the column. Args: World, this.x,y,z */
 	public static double findSolidSurface(World world, double x, double y, double z) { //Returns double y-coord of top surface of top block
 
 		int xp = (int)x;
 		int zp = (int)z;
 		boolean lowestsolid = false;
 		boolean solidup = false;
 		boolean soliddown = false;
 
 		while (!(!solidup && soliddown)) {
 			solidup = (world.getBlockMaterial(xp, (int)y, zp) != Material.air);
 			soliddown = (world.getBlockMaterial(xp, (int)y-1, zp) != Material.air);
 			if (solidup && soliddown) //Both blocks are solid -> below surface
 				y++;
 			if (solidup && !soliddown) //Upper only is solid -> should never happen
 				y += 2;					// Fix attempt
 			if (!solidup && soliddown) // solid lower only
 				;						// the case we want
 			if (!solidup && !soliddown) //Neither solid -> above surface
 				y--;
 		}
 		return y;
 	}
 
 	/** Finds the top edge of the top water block in the column. Args: World, this.x,y,z */
 	public static double findWaterSurface(World world, double x, double y, double z) { //Returns double y-coord of top surface of top block
 
 		int xp = (int)x;
 		int zp = (int)z;
 		boolean lowestwater = false;
 		boolean waterup = false;
 		boolean waterdown = false;
 
 		while (!(!waterup && waterdown)) {
 			waterup = (world.getBlockMaterial(xp, (int)y, zp) == Material.water);
 			waterdown = (world.getBlockMaterial(xp, (int)y-1, zp) == Material.water);
 			if (waterup && waterdown) //Both blocks are water -> below surface
 				y++;
 			if (waterup && !waterdown) //Upper only is water -> should never happen
 				return 255;		//Return top of chunk and exit function
 			if (!waterup && waterdown) // Water lower only
 				;						// the case we want
 			if (!waterup && !waterdown) //Neither water -> above surface
 				y--;
 		}
 		return y;
 	}
 
 	/** Search for a specific block in a range. Returns true if found. Cannot identify if
 	 * found more than one, or where the found one(s) is/are. May be CPU-intensive. Args: World, this.x,y,z, search range, target id */
 	public static boolean findNearBlock(World world, int x, int y, int z, int range, int id) {
 		x -= range/2;
 		y -= range/2;
 		z -= range/2;
 		for (int i = 0; i < range; i++) {
 			for (int j = 0; j < range; j++) {
 				for (int k = 0; k < range; k++) {
 					if (world.getBlockId(x+i, y+j, z+k) == id)
 						return true;
 				}
 			}
 		}
 		return false;
 	}
 
 	/** Search for a specific block in a range. Returns number found. Cannot identify where they
 	 * are. May be CPU-intensive. Args: World, this.x,y,z, search range, target id */
 	public static int findNearBlocks(World world, int x, int y, int z, int range, int id) {
 		int count = 0;
 		x -= range/2;
 		y -= range/2;
 		z -= range/2;
 		for (int i = 0; i < range; i++) {
 			for (int j = 0; j < range; j++) {
 				for (int k = 0; k < range; k++) {
 					if (world.getBlockId(x+i, y+j, z+k) == id)
 						count++;
 				}
 			}
 		}
 		return count;
 	}
 
 	/** Tests for if a block of a certain id is in the "sights" of a directional block (eg dispenser).
 	 * Returns the number of blocks away it is. If not found, returns 0 (an impossibility).
 	 * Args: World, this.x,y,z, search range, target id, direction "f" */
 	public static int isLookingAt(World world, int x, int y, int z, int range, int id, int f) {
 		int idfound = 0;
 
 		switch (f) {
 		case 0:		//facing north (-z);
 			for (int i = 0; i < range; i++) {
 				idfound = world.getBlockId(x, y, z-i);
 				if (idfound == id)
 					return i;
 			}
 			break;
 		case 1:		//facing east (-x);
 			for (int i = 0; i < range; i++) {
 				idfound = world.getBlockId(x-i, y, z);
 				if (idfound == id)
 					return i;
 			}
 			break;
 		case 2:		//facing south (+z);
 			for (int i = 0; i < range; i++) {
 				idfound = world.getBlockId(x, y, z+i);
 				if (idfound == id)
 					return i;
 			}
 			break;
 		case 3:		//facing west (+x);
 			for (int i = 0; i < range; i++) {
 				idfound = world.getBlockId(x+i, y, z);
 				if (idfound == id)
 					return i;
 			}
 			break;
 		}
 		return 0;
 	}
 
 	/** Returns the direction in which a block of the specified ID was found.
 	 * Returns -1 if not found. Args: World, x,y,z, id to search.
 	 * Convention: 0 up 1 down 2 x+ 3 x- 4 z+ 5 z- */
 	public static int checkForAdjBlock(World world, int x, int y, int z, int id) {
 		if (world.getBlockId(x,y+1,z) == id)
 			return 0;
 		if (world.getBlockId(x,y-1,z) == id)
 			return 1;
 		if (world.getBlockId(x+1,y,z) == id)
 			return 2;
 		if (world.getBlockId(x-1,y,z) == id)
 			return 3;
 		if (world.getBlockId(x,y,z+1) == id)
 			return 4;
 		if (world.getBlockId(x,y,z-1) == id)
 			return 5;
 		return -1;
 	}
 
 	/** Returns the direction in which a block of the specified material was found.
 	 * Returns -1 if not found. Args: World, x,y,z, material to search.
 	 * Convention: 0 up 1 down 2 x+ 3 x- 4 z+ 5 z- */
 	public static int checkForAdjMaterial(World world, int x, int y, int z, Material mat) {
 		if (world.getBlockMaterial(x,y+1,z) == mat)
 			return 0;
 		if (world.getBlockMaterial(x,y-1,z) == mat)
 			return 1;
 		if (world.getBlockMaterial(x+1,y,z) == mat)
 			return 2;
 		if (world.getBlockMaterial(x-1,y,z) == mat)
 			return 3;
 		if (world.getBlockMaterial(x,y,z+1) == mat)
 			return 4;
 		if (world.getBlockMaterial(x,y,z-1) == mat)
 			return 5;
 		return -1;
 	}
 
 	/** Returns the direction in which a source block of the specified liquid was found.
 	 * Returns -1 if not found. Args: World, x,y,z, material (water/lava) to search.
 	 * Convention: 0 up 1 down 2 x+ 3 x- 4 z+ 5 z- */
 	public static int checkForAdjSourceBlock(World world, int x, int y, int z, Material mat) {
 		if (world.getBlockMaterial(x, y+1, z) == mat && world.getBlockMetadata(x, y+1, z) == 0)
 			return 0;
 		if (world.getBlockMaterial(x, y-1, z) == mat && world.getBlockMetadata(x, y-1, z) == 0)
 			return 1;
 		if (world.getBlockMaterial(x+1, y, z) == mat && world.getBlockMetadata(x+1, y, z) == 0)
 			return 2;
 		if (world.getBlockMaterial(x-1, y, z) == mat && world.getBlockMetadata(x-1, y, z) == 0)
 			return 3;
 		if (world.getBlockMaterial(x, y, z+1) == mat && world.getBlockMetadata(x, y, z+1) == 0)
 			return 4;
 		if (world.getBlockMaterial(x, y, z-1) == mat && world.getBlockMetadata(x, y, z-1) == 0)
 			return 5;
 		return -1;
 	}
 
 	/** Edits a block adjacent to the passed arguments, on the specified side.
 	 * Args: World, x, y, z, side, id to change to */
 	public static void changeAdjBlock(World world, int x, int y, int z, int side, int id) {
 		switch(side) {
 		case 0:
 			legacySetBlockWithNotify(world, x, y+1, z, id);
 			break;
 		case 1:
 			legacySetBlockWithNotify(world, x, y-1, z, id);
 			break;
 		case 2:
 			legacySetBlockWithNotify(world, x+1, y, z, id);
 			break;
 		case 3:
 			legacySetBlockWithNotify(world, x-1, y, z, id);
 			break;
 		case 4:
 			legacySetBlockWithNotify(world, x, y, z+1, id);
 			break;
 		case 5:
 			legacySetBlockWithNotify(world, x, y, z-1, id);
 			break;
 		}
 	}
 
 	/** Returns true if the passed biome is a snow biome.  Args: Biome*/
 	public static boolean isSnowBiome(BiomeGenBase biome) {
 		if (biome == BiomeGenBase.frozenOcean)
 			return true;
 		if (biome == BiomeGenBase.frozenRiver)
 			return true;
 		if (biome == BiomeGenBase.iceMountains)
 			return true;
 		if (biome == BiomeGenBase.icePlains)
 			return true;
 		if (biome == BiomeGenBase.taiga)
 			return true;
 		if (biome == BiomeGenBase.taigaHills)
 			return true;
 
 		return false;
 	}
 
 	/** Returns true if the passed biome is a hot biome.  Args: Biome*/
 	public static boolean isHotBiome(BiomeGenBase biome) {
 		if (biome == BiomeGenBase.desert)
 			return true;
 		if (biome == BiomeGenBase.desertHills)
 			return true;
 		if (biome == BiomeGenBase.hell)
 			return true;
 		if (biome == BiomeGenBase.jungle)
 			return true;
 		if (biome == BiomeGenBase.jungleHills)
 			return true;
 
 		return false;
 	}
 
 	/** Applies temperature effects to the environment. Args: World, x, y, z, temperature */
 	public static void temperatureEnvironment(World world, int x, int y, int z, int temperature) {
 		if (temperature < 0) {
 			for (int i = 0; i < 6; i++) {
 				int side = (ReikaWorldHelper.checkForAdjMaterial(world, x, y, z, Material.water));
 				if (side != -1)
 					ReikaWorldHelper.changeAdjBlock(world, x, y, z, side, Block.ice.blockID);
 			}
 		}
 		if (temperature > 450)	{ // Wood autoignition
 			for (int i = 0; i < 4; i++) {
 				if (world.getBlockMaterial(x-i, y, z) == Material.wood)
 					ignite(world, x-i, y, z);
 				if (world.getBlockMaterial(x+i, y, z) == Material.wood)
 					ignite(world, x+i, y, z);
 				if (world.getBlockMaterial(x, y-i, z) == Material.wood)
 					ignite(world, x, y-i, z);
 				if (world.getBlockMaterial(x, y+i, z) == Material.wood)
 					ignite(world, x, y+i, z);
 				if (world.getBlockMaterial(x, y, z-i) == Material.wood)
 					ignite(world, x, y, z-i);
 				if (world.getBlockMaterial(x, y, z+i) == Material.wood)
 					ignite(world, x, y, z+i);
 			}
 		}
 		if (temperature > 600)	{ // Wool autoignition
 			for (int i = 0; i < 4; i++) {
 				if (world.getBlockMaterial(x-i, y, z) == Material.cloth)
 					ignite(world, x-i, y, z);
 				if (world.getBlockMaterial(x+i, y, z) == Material.cloth)
 					ignite(world, x+i, y, z);
 				if (world.getBlockMaterial(x, y-i, z) == Material.cloth)
 					ignite(world, x, y-i, z);
 				if (world.getBlockMaterial(x, y+i, z) == Material.cloth)
 					ignite(world, x, y+i, z);
 				if (world.getBlockMaterial(x, y, z-i) == Material.cloth)
 					ignite(world, x, y, z-i);
 				if (world.getBlockMaterial(x, y, z+i) == Material.cloth)
 					ignite(world, x, y, z+i);
 			}
 		}
 		if (temperature > 300)	{ // TNT autoignition
 			for (int i = 0; i < 4; i++) {
 				if (world.getBlockMaterial(x-i, y, z) == Material.tnt)
 					ignite(world, x-i, y, z);
 				if (world.getBlockMaterial(x+i, y, z) == Material.tnt)
 					ignite(world, x+i, y, z);
 				if (world.getBlockMaterial(x, y-i, z) == Material.tnt)
 					ignite(world, x, y-i, z);
 				if (world.getBlockMaterial(x, y+i, z) == Material.tnt)
 					ignite(world, x, y+i, z);
 				if (world.getBlockMaterial(x, y, z-i) == Material.tnt)
 					ignite(world, x, y, z-i);
 				if (world.getBlockMaterial(x, y, z+i) == Material.tnt)
 					ignite(world, x, y, z+i);
 			}
 		}
 		if (temperature > 230)	{ // Grass/leaves/plant autoignition
 			for (int i = 0; i < 4; i++) {
 				if (world.getBlockMaterial(x-i, y, z) == Material.leaves || world.getBlockMaterial(x-i, y, z) == Material.vine || world.getBlockMaterial(x-i, y, z) == Material.plants || world.getBlockMaterial(x-i, y, z) == Material.web)
 					ignite(world, x-i, y, z);
 				if (world.getBlockMaterial(x+i, y, z) == Material.leaves || world.getBlockMaterial(x+i, y, z) == Material.vine || world.getBlockMaterial(x+i, y, z) == Material.plants || world.getBlockMaterial(x+i, y, z) == Material.web)
 					ignite(world, x+i, y, z);
 				if (world.getBlockMaterial(x, y-i, z) == Material.leaves || world.getBlockMaterial(x, y-i, z) == Material.vine || world.getBlockMaterial(x, y-i, z) == Material.plants || world.getBlockMaterial(x, y-i, z) == Material.web)
 					ignite(world, x, y-i, z);
 				if (world.getBlockMaterial(x, y+i, z) == Material.leaves || world.getBlockMaterial(x, y+i, z) == Material.vine || world.getBlockMaterial(x, y+i, z) == Material.plants || world.getBlockMaterial(x, y+i, z) == Material.web)
 					ignite(world, x, y+i, z);
 				if (world.getBlockMaterial(x, y, z-i) == Material.leaves || world.getBlockMaterial(x, y, z-i) == Material.vine || world.getBlockMaterial(x, y, z-i) == Material.plants || world.getBlockMaterial(x, y, z-i) == Material.web)
 					ignite(world, x, y, z-i);
 				if (world.getBlockMaterial(x, y, z+i) == Material.leaves || world.getBlockMaterial(x, y, z+i) == Material.vine || world.getBlockMaterial(x, y, z+i) == Material.plants || world.getBlockMaterial(x, y, z+i) == Material.web)
 					ignite(world, x, y, z+i);
 			}
 		}
 
 		if (temperature > 0)	{ // Melting snow/ice
 			for (int i = 0; i < 3; i++) {
 				if (world.getBlockMaterial(x-i, y, z) == Material.ice)
 					legacySetBlockWithNotify(world, x-i, y, z, Block.waterMoving.blockID);
 				if (world.getBlockMaterial(x+i, y, z) == Material.ice)
 					legacySetBlockWithNotify(world, x+i, y, z, Block.waterMoving.blockID);
 				if (world.getBlockMaterial(x, y-i, z) == Material.ice)
 					legacySetBlockWithNotify(world, x, y-i, z, Block.waterMoving.blockID);
 				if (world.getBlockMaterial(x, y+i, z) == Material.ice)
 					legacySetBlockWithNotify(world, x, y+i, z, Block.waterMoving.blockID);
 				if (world.getBlockMaterial(x, y, z-i) == Material.ice)
 					legacySetBlockWithNotify(world, x, y, z-i, Block.waterMoving.blockID);
 				if (world.getBlockMaterial(x, y, z+i) == Material.ice)
 					legacySetBlockWithNotify(world, x, y, z+i, Block.waterMoving.blockID);
 			}
 		}
 		if (temperature > 0)	{ // Melting snow/ice
 			for (int i = 0; i < 3; i++) {
 				if (world.getBlockMaterial(x-i, y, z) == Material.snow)
 					legacySetBlockWithNotify(world, x-i, y, z, 0);
 				if (world.getBlockMaterial(x+i, y, z) == Material.snow)
 					legacySetBlockWithNotify(world, x+i, y, z, 0);
 				if (world.getBlockMaterial(x, y-i, z) == Material.snow)
 					legacySetBlockWithNotify(world, x, y-i, z, 0);
 				if (world.getBlockMaterial(x, y+i, z) == Material.snow)
 					legacySetBlockWithNotify(world, x, y+i, z, 0);
 				if (world.getBlockMaterial(x, y, z-i) == Material.snow)
 					legacySetBlockWithNotify(world, x, y, z-i, 0);
 				if (world.getBlockMaterial(x, y, z+i) == Material.snow)
 					legacySetBlockWithNotify(world, x, y, z+i, 0);
 
 				if (world.getBlockMaterial(x-i, y, z) == Material.craftedSnow)
 					legacySetBlockWithNotify(world, x-i, y, z, 0);
 				if (world.getBlockMaterial(x+i, y, z) == Material.craftedSnow)
 					legacySetBlockWithNotify(world, x+i, y, z, 0);
 				if (world.getBlockMaterial(x, y-i, z) == Material.craftedSnow)
 					legacySetBlockWithNotify(world, x, y-i, z, 0);
 				if (world.getBlockMaterial(x, y+i, z) == Material.craftedSnow)
 					legacySetBlockWithNotify(world, x, y+i, z, 0);
 				if (world.getBlockMaterial(x, y, z-i) == Material.craftedSnow)
 					legacySetBlockWithNotify(world, x, y, z-i, 0);
 				if (world.getBlockMaterial(x, y, z+i) == Material.craftedSnow)
 					legacySetBlockWithNotify(world, x, y, z+i, 0);
 			}
 		}
 		if (temperature > 900)	{ // Melting sand, ground
 			for (int i = 0; i < 3; i++) {
 				if (world.getBlockMaterial(x-i, y, z) == Material.sand)
 					legacySetBlockWithNotify(world, x-i, y, z, Block.glass.blockID);
 				if (world.getBlockMaterial(x+i, y, z) == Material.sand)
 					legacySetBlockWithNotify(world, x+i, y, z, Block.glass.blockID);
 				if (world.getBlockMaterial(x, y-i, z) == Material.sand)
 					legacySetBlockWithNotify(world, x, y-i, z, Block.glass.blockID);
 				if (world.getBlockMaterial(x, y+i, z) == Material.sand)
 					legacySetBlockWithNotify(world, x, y+i, z, Block.glass.blockID);
 				if (world.getBlockMaterial(x, y, z-i) == Material.sand)
 					legacySetBlockWithNotify(world, x, y, z-i, Block.glass.blockID);
 				if (world.getBlockMaterial(x, y, z+i) == Material.sand)
 					legacySetBlockWithNotify(world, x, y, z+i, Block.glass.blockID);
 
 				if (world.getBlockMaterial(x-i, y, z) == Material.ground)
 					legacySetBlockWithNotify(world, x-i, y, z, Block.glass.blockID);
 				if (world.getBlockMaterial(x+i, y, z) == Material.ground)
 					legacySetBlockWithNotify(world, x+i, y, z, Block.glass.blockID);
 				if (world.getBlockMaterial(x, y-i, z) == Material.ground)
 					legacySetBlockWithNotify(world, x, y-i, z, Block.glass.blockID);
 				if (world.getBlockMaterial(x, y+i, z) == Material.ground)
 					legacySetBlockWithNotify(world, x, y+i, z, Block.glass.blockID);
 				if (world.getBlockMaterial(x, y, z-i) == Material.ground)
 					legacySetBlockWithNotify(world, x, y, z-i, Block.glass.blockID);
 				if (world.getBlockMaterial(x, y, z+i) == Material.ground)
 					legacySetBlockWithNotify(world, x, y, z+i, Block.glass.blockID);
 			}
 		}
 		if (temperature > 1700)	{ // Melting rock
 			for (int i = 0; i < 3; i++) {
 				if (world.getBlockMaterial(x-i, y, z) == Material.rock)
 					legacySetBlockWithNotify(world, x-i, y, z, Block.lavaMoving.blockID);
 				if (world.getBlockMaterial(x+i, y, z) == Material.rock)
 					legacySetBlockWithNotify(world, x+i, y, z, Block.lavaMoving.blockID);
 				if (world.getBlockMaterial(x, y-i, z) == Material.rock)
 					legacySetBlockWithNotify(world, x, y-i, z, Block.lavaMoving.blockID);
 				if (world.getBlockMaterial(x, y+i, z) == Material.rock)
 					legacySetBlockWithNotify(world, x, y+i, z, Block.lavaMoving.blockID);
 				if (world.getBlockMaterial(x, y, z-i) == Material.rock)
 					legacySetBlockWithNotify(world, x, y, z-i, Block.lavaMoving.blockID);
 				if (world.getBlockMaterial(x, y, z+i) == Material.rock)
 					legacySetBlockWithNotify(world, x, y, z+i, Block.lavaMoving.blockID);
 			}
 		}
 	}
 
 	/** Surrounds the block with fire. Args: World, x, y, z */
 	public static void ignite(World world, int x, int y, int z) {
 		if (world.getBlockId		(x-1, y, z) == 0)
 			legacySetBlockWithNotify(world, x-1, y, z, Block.fire.blockID);
 		if (world.getBlockId		(x+1, y, z) == 0)
 			legacySetBlockWithNotify(world, x+1, y, z, Block.fire.blockID);
 		if (world.getBlockId		(x, y-1, z) == 0)
 			legacySetBlockWithNotify(world, x, y-1, z, Block.fire.blockID);
 		if (world.getBlockId		(x, y+1, z) == 0)
 			legacySetBlockWithNotify(world, x, y+1, z, Block.fire.blockID);
 		if (world.getBlockId		(x, y, z-1) == 0)
 			legacySetBlockWithNotify(world, x, y, z-1, Block.fire.blockID);
 		if (world.getBlockId		(x, y, z+1) == 0)
 			legacySetBlockWithNotify(world, x, y, z+1, Block.fire.blockID);
 	}
 
 	/** Returns the number of water blocks directly and continuously above the passed coordinates.
 	 * Returns -1 if invalid liquid specified. Args: World, x, y, z */
 	public static int getDepth(World world, int x, int y, int z, String liq) {
 		int i = 1;
 		if (liq == "water") {
 			while (world.getBlockId(x, y+i, z) == Block.waterMoving.blockID || world.getBlockId(x, y+i, z) == Block.waterStill.blockID) {
 				i++;
 			}
 			return (i-1);
 		}
 		if (liq == "lava") {
 			while (world.getBlockId(x, y+i, z) == Block.lavaMoving.blockID || world.getBlockId(x, y+i, z) == Block.lavaStill.blockID) {
 				i++;
 			}
 			return (i-1);
 		}
 		return -1;
 	}
 
 	/** Returns true if the block ID is one associated with caves, like air, cobwebs,
 	 * spawners, mushrooms, etc. Args: Block ID */
 	public static boolean caveBlock(int id) {
 		if (id == 0 || id == Block.waterMoving.blockID || id == Block.waterStill.blockID || id == Block.lavaMoving.blockID ||
 				id == Block.lavaStill.blockID || id == Block.web.blockID || id == Block.mobSpawner.blockID || id == Block.mushroomRed.blockID ||
 				id == Block.mushroomBrown.blockID)
 			return true;
 		return false;
 	}
 
 	/** Returns a broad-stroke biome temperature in degrees centigrade.
 	 * Args: biome */
 	public static int getBiomeTemp(BiomeGenBase biome) {
 		int Tamb = 25; //Most biomes = 25C
 		if (ReikaWorldHelper.isSnowBiome(biome))
 			Tamb = -20; //-20C
 		if (ReikaWorldHelper.isHotBiome(biome))
 			Tamb = 40;
 		if (biome == BiomeGenBase.hell)
 			Tamb = 300;	//boils water, so 300C (3 x 100)
 		return Tamb;
 	}
 
 	/** Returns a broad-stroke biome temperature in degrees centigrade.
 	 * Args: World, x, z */
 	public static int getBiomeTemp(World world, int x, int z) {
 		int Tamb = 25; //Most biomes = 25C
 		BiomeGenBase biome = world.getBiomeGenForCoords(x, z);
 		if (ReikaWorldHelper.isSnowBiome(biome))
 			Tamb = -20; //-20C
 		if (ReikaWorldHelper.isHotBiome(biome))
 			Tamb = 40;
 		if (biome == BiomeGenBase.hell)
 			Tamb = 300;	//boils water, so 300C (3 x 100)
 		return Tamb;
 	}
 
 	/** Performs machine overheat effects (primarily intended for RotaryCraft).
 	 * Args: World, x, y, z, item drop id, item drop metadata, min drops, max drops,
 	 * spark particles yes/no, number-of-sparks multiplier (default 20-40),
 	 * flaming explosion yes/no, smoking explosion yes/no, explosion force (0 for none) */
 	public static void overheat(World world, int x, int y, int z, int id, int meta, int mindrops, int maxdrops, boolean sparks, float sparkmultiplier, boolean flaming, boolean smoke, float force) {
 		if (force > 0 && !world.isRemote) {
 			if (flaming)
 				world.newExplosion(null, x, y, z, force, true, smoke);
 			else
 				world.createExplosion(null, x, y, z, force, smoke);
 		}
 		int numsparks = rand.nextInt(20)+20;
 		numsparks *= sparkmultiplier;
 		if (sparks)
 			for (int i = 0; i < numsparks; i++)
 				world.spawnParticle("lava", x+rand.nextFloat(), y+1, z+rand.nextFloat(), 0, 0, 0);
 		ItemStack scrap = new ItemStack(id, 1, meta);
 		int numdrops = rand.nextInt(maxdrops)+mindrops;
 		if (!world.isRemote || id <= 0) {
 			for (int i = 0; i < numdrops; i++) {
 				EntityItem ent = new EntityItem(world, x+rand.nextFloat(), y+0.5, z+rand.nextFloat(), scrap);
				ent.motionX = -0.2+0.4*rand.nextFloat();
				ent.motionY = 0.5*rand.nextFloat();
				ent.motionZ = -0.2+0.4*rand.nextFloat();
 				world.spawnEntityInWorld(ent);
 				ent.velocityChanged = true;
 			}
 		}
 	}
 
 	/** Takes a specified amount of XP and splits it randomly among a bunch of orbs.
 	 * Args: World, x, y, z, amount */
 	public static void splitAndSpawnXP(World world, float x, float y, float z, int xp) {
 		int max = xp/5+1;
 
 		while (xp > 0) {
 			int value = rand.nextInt(max)+1;
 			while (value > xp)
 				value = rand.nextInt(max)+1;
 			xp -= value;
 			EntityXPOrb orb = new EntityXPOrb(world, x, y, z, value);
			orb.motionX = -0.2+0.4*rand.nextFloat();
			orb.motionY = 0.3*rand.nextFloat();
			orb.motionZ = -0.2+0.4*rand.nextFloat();
 			if (world.isRemote)
 				return;
 			orb.velocityChanged = true;
 			world.spawnEntityInWorld(orb);
 		}
 	}
 
 	/** Returns true if the coordinate specified is a lava source block and would be recreated according to the lava-duplication rules
 	 * that existed for a short time in Beta 1.9. Args: World, x, y, z */
 	public static boolean is1p9InfiniteLava(World world, int x, int y, int z) {
 		if (world.getBlockMaterial(x, y, z) != Material.lava || world.getBlockMetadata(x, y, z) != 0)
 			return false;
 		if (world.getBlockMaterial(x+1, y, z) != Material.lava || world.getBlockMetadata(x+1, y, z) != 0)
 			return false;
 		if (world.getBlockMaterial(x, y, z+1) != Material.lava || world.getBlockMetadata(x, y, z+1) != 0)
 			return false;
 		if (world.getBlockMaterial(x-1, y, z) != Material.lava || world.getBlockMetadata(x-1, y, z) != 0)
 			return false;
 		if (world.getBlockMaterial(x, y, z-1) != Material.lava || world.getBlockMetadata(x, y, z-1) != 0)
 			return false;
 		return true;
 	}
 
 	/** Returns the y-coordinate of the top non-air block at the given xz coordinates, at or
 	 * below the specified y-coordinate. Returns -1 if none. Args: World, x, z, y */
 	public static int findTopBlockBelowY(World world, int x, int z, int y) {
 		int id = world.getBlockId(x, y, z);
 		while ((id == 0) && y >= 0) {
 			y--;
 			id = world.getBlockId(x, y, z);
 		}
 		return y;
 	}
 
 	/** Returns true if the coordinate is a liquid source block. Args: World, x, y, z */
 	public static boolean isLiquidSourceBlock(World world, int x, int y, int z) {
 		if (world.getBlockMetadata(x, y, z) != 0)
 			return false;
 		if (world.getBlockMaterial(x, y, z) != Material.lava && world.getBlockMaterial(x, y, z) != Material.water)
 			return false;
 		return true;
 	}
 
 	/** Breaks a contiguous area of blocks recursively (akin to a fill tool in image editors).
 	 * Args: World, start x, start y, start z, id, metadata (-1 for any) */
 	public static void recursiveBreak(World world, int x, int y, int z, int id, int meta) {
 		if (id == 0)
 			return;
 		if (world.getBlockId(x, y, z) != id)
 			return;
 		if (meta != world.getBlockMetadata(x, y, z) && meta != -1)
 			return;
 		int metad = world.getBlockMetadata(x, y, z);
 		Block.blocksList[id].dropBlockAsItem(world, x, y, z, id, metad);
 		legacySetBlockWithNotify(world, x, y, z, 0);
 		world.markBlockForUpdate(x, y, z);
 		recursiveBreak(world, x+1, y, z, id, meta);
 		recursiveBreak(world, x-1, y, z, id, meta);
 		recursiveBreak(world, x, y+1, z, id, meta);
 		recursiveBreak(world, x, y-1, z, id, meta);
 		recursiveBreak(world, x, y, z+1, id, meta);
 		recursiveBreak(world, x, y, z-1, id, meta);
 	}
 
 	/** Like the ordinary recursive break but with a spherical bounded volume. Args: World, x, y, z,
 	 * id to replace, metadata to replace (-1 for any), origin x,y,z, max radius */
 	public static void recursiveBreakWithinSphere(World world, int x, int y, int z, int id, int meta, int x0, int y0, int z0, double r) {
 		if (id == 0)
 			return;
 		if (world.getBlockId(x, y, z) != id)
 			return;
 		if (meta != world.getBlockMetadata(x, y, z) && meta != -1)
 			return;
 		if (ReikaMathLibrary.py3d(x-x0, y-y0, z-z0) > r)
 			return;
 		int metad = capMetadata(world.getBlockMetadata(x, y, z), 4);
 		Block.blocksList[id].dropBlockAsItem(world, x, y, z, metad, 0);
 		legacySetBlockWithNotify(world, x, y, z, 0);
 		world.markBlockForUpdate(x, y, z);
 		recursiveBreakWithinSphere(world, x+1, y, z, id, meta, x0, y0, z0, r);
 		recursiveBreakWithinSphere(world, x-1, y, z, id, meta, x0, y0, z0, r);
 		recursiveBreakWithinSphere(world, x, y+1, z, id, meta, x0, y0, z0, r);
 		recursiveBreakWithinSphere(world, x, y-1, z, id, meta, x0, y0, z0, r);
 		recursiveBreakWithinSphere(world, x, y, z+1, id, meta, x0, y0, z0, r);
 		recursiveBreakWithinSphere(world, x, y, z-1, id, meta, x0, y0, z0, r);
 	}
 
 	/** Like the ordinary recursive break but with a bounded volume. Args: World, x, y, z,
 	 * id to replace, metadata to replace (-1 for any), min x,y,z, max x,y,z */
 	public static void recursiveBreakWithBounds(World world, int x, int y, int z, int id, int meta, int x1, int y1, int z1, int x2, int y2, int z2) {
 		if (id == 0)
 			return;
 		if (x < x1 || y < y1 || z < z1 || x > x2 || y > y2 || z > z2)
 			return;
 		if (world.getBlockId(x, y, z) != id)
 			return;
 		if (meta != world.getBlockMetadata(x, y, z) && meta != -1)
 			return;
 		int metad = world.getBlockMetadata(x, y, z);
 		Block.blocksList[id].dropBlockAsItem(world, x, y, z, id, metad);
 		legacySetBlockWithNotify(world, x, y, z, 0);
 		world.markBlockForUpdate(x, y, z);
 		recursiveBreakWithBounds(world, x+1, y, z, id, meta, x1, y1, z1, x2, y2, z2);
 		recursiveBreakWithBounds(world, x-1, y, z, id, meta, x1, y1, z1, x2, y2, z2);
 		recursiveBreakWithBounds(world, x, y+1, z, id, meta, x1, y1, z1, x2, y2, z2);
 		recursiveBreakWithBounds(world, x, y-1, z, id, meta, x1, y1, z1, x2, y2, z2);
 		recursiveBreakWithBounds(world, x, y, z+1, id, meta, x1, y1, z1, x2, y2, z2);
 		recursiveBreakWithBounds(world, x, y, z-1, id, meta, x1, y1, z1, x2, y2, z2);
 	}
 
 	/** Recursively fills a contiguous area of one block type with another, akin to a fill tool.
 	 * Args: World, start x, start y, start z, id to replace, id to fill with,
 	 * metadata to replace (-1 for any), metadata to fill with */
 	public static void recursiveFill(World world, int x, int y, int z, int id, int idto, int meta, int metato) {
 		if (world.getBlockId(x, y, z) != id)
 			return;
 		if (meta != world.getBlockMetadata(x, y, z) && meta != -1)
 			return;
 		int metad = world.getBlockMetadata(x, y, z);
 		legacySetBlockAndMetadataWithNotify(world, x, y, z, idto, metato);
 		world.markBlockForUpdate(x, y, z);
 		recursiveFill(world, x+1, y, z, id, idto, meta, metato);
 		recursiveFill(world, x-1, y, z, id, idto, meta, metato);
 		recursiveFill(world, x, y+1, z, id, idto, meta, metato);
 		recursiveFill(world, x, y-1, z, id, idto, meta, metato);
 		recursiveFill(world, x, y, z+1, id, idto, meta, metato);
 		recursiveFill(world, x, y, z-1, id, idto, meta, metato);
 	}
 
 	/** Like the ordinary recursive fill but with a bounded volume. Args: World, x, y, z,
 	 * id to replace, id to fill with, metadata to replace (-1 for any),
 	 * metadata to fill with, min x,y,z, max x,y,z */
 	public static void recursiveFillWithBounds(World world, int x, int y, int z, int id, int idto, int meta, int metato, int x1, int y1, int z1, int x2, int y2, int z2) {
 		if (x < x1 || y < y1 || z < z1 || x > x2 || y > y2 || z > z2)
 			return;
 		if (world.getBlockId(x, y, z) != id)
 			return;
 		if (meta != world.getBlockMetadata(x, y, z) && meta != -1)
 			return;
 		int metad = world.getBlockMetadata(x, y, z);
 		legacySetBlockAndMetadataWithNotify(world, x, y, z, idto, metato);
 		world.markBlockForUpdate(x, y, z);
 		recursiveFillWithBounds(world, x+1, y, z, id, idto, meta, metato, x1, y1, z1, x2, y2, z2);
 		recursiveFillWithBounds(world, x-1, y, z, id, idto, meta, metato, x1, y1, z1, x2, y2, z2);
 		recursiveFillWithBounds(world, x, y+1, z, id, idto, meta, metato, x1, y1, z1, x2, y2, z2);
 		recursiveFillWithBounds(world, x, y-1, z, id, idto, meta, metato, x1, y1, z1, x2, y2, z2);
 		recursiveFillWithBounds(world, x, y, z+1, id, idto, meta, metato, x1, y1, z1, x2, y2, z2);
 		recursiveFillWithBounds(world, x, y, z-1, id, idto, meta, metato, x1, y1, z1, x2, y2, z2);
 	}
 
 	/** Like the ordinary recursive fill but with a spherical bounded volume. Args: World, x, y, z,
 	 * id to replace, id to fill with, metadata to replace (-1 for any),
 	 * metadata to fill with, origin x,y,z, max radius */
 	public static void recursiveFillWithinSphere(World world, int x, int y, int z, int id, int idto, int meta, int metato, int x0, int y0, int z0, double r) {
 		//ReikaGuiAPI.write(world.getBlockId(x, y, z)+" & "+id+" @ "+x0+", "+y0+", "+z0);
 		if (world.getBlockId(x, y, z) != id)
 			return;
 		if (meta != world.getBlockMetadata(x, y, z) && meta != -1)
 			return;
 		if (ReikaMathLibrary.py3d(x-x0, y-y0, z-z0) > r)
 			return;
 		int metad = world.getBlockMetadata(x, y, z);
 		legacySetBlockAndMetadataWithNotify(world, x, y, z, idto, metato);
 		world.markBlockForUpdate(x, y, z);
 		recursiveFillWithinSphere(world, x+1, y, z, id, idto, meta, metato, x0, y0, z0, r);
 		recursiveFillWithinSphere(world, x-1, y, z, id, idto, meta, metato, x0, y0, z0, r);
 		recursiveFillWithinSphere(world, x, y+1, z, id, idto, meta, metato, x0, y0, z0, r);
 		recursiveFillWithinSphere(world, x, y-1, z, id, idto, meta, metato, x0, y0, z0, r);
 		recursiveFillWithinSphere(world, x, y, z+1, id, idto, meta, metato, x0, y0, z0, r);
 		recursiveFillWithinSphere(world, x, y, z-1, id, idto, meta, metato, x0, y0, z0, r);
 	}
 
 	/** Returns true if there is a clear line of sight between two points. Args: World, Start x,y,z, End x,y,z
 	 * NOTE: If one point is a block, use canBlockSee instead, as this method will always return false. */
 	public static boolean lineOfSight(World world, double x1, double y1, double z1, double x2, double y2, double z2) {
 		if (world.isRemote)
 			return false;
 		Vec3 v1 = Vec3.fakePool.getVecFromPool(x1, y1, z1);
 		Vec3 v2 = Vec3.fakePool.getVecFromPool(x2, y2, z2);
 		return (world.rayTraceBlocks(v1, v2) == null);
 	}
 
 	/** Returns true if there is a clear line of sight between two entites. Args: World, Entity 1, Entity 2 */
 	public static boolean lineOfSight(World world, Entity e1, Entity e2) {
 		if (world.isRemote)
 			return false;
 		Vec3 v1 = Vec3.fakePool.getVecFromPool(e1.posX, e1.posY+e1.getEyeHeight(), e1.posZ);
 		Vec3 v2 = Vec3.fakePool.getVecFromPool(e2.posX, e2.posY+e2.getEyeHeight(), e2.posZ);
 		return (world.rayTraceBlocks(v1, v2) == null);
 	}
 
 	/** Returns true if a block can see an point. Args: World, block x,y,z, Point x,y,z, Max Range */
 	public static boolean canBlockSee(World world, int x, int y, int z, double x0, double y0, double z0, double range) {
 		int locid = world.getBlockId(x, y, z);
 		range += 2;
 		for (int k = 0; k < 10; k++) {
 			float a = 0; float b = 0; float c = 0;
 			switch(k) {
 			case 1:
 				a = 1;
 				break;
 			case 2:
 				b = 1;
 				break;
 			case 3:
 				a = 1;
 				b = 1;
 				break;
 			case 4:
 				c = 1;
 				break;
 			case 5:
 				a = 1;
 				c = 1;
 				break;
 			case 6:
 				b = 1;
 				c = 1;
 				break;
 			case 7:
 				a = 1;
 				b = 1;
 				c = 1;
 				break;
 			case 8:
 				a = 0.5F;
 				b = 0.5F;
 				c = 0.5F;
 				break;
 			case 9:
 				b = 0.5F;
 				break;
 			}
 			for (float i = 0; i <= range; i += 0.25) {
 				Vec3 vec2 = ReikaVectorHelper.getVec2Pt(x+a, y+b, z+c, x0, y0, z0).normalize();
 				vec2.xCoord *= i;
 				vec2.yCoord *= i;
 				vec2.zCoord *= i;
 				vec2.xCoord += x0;
 				vec2.yCoord += y0;
 				vec2.zCoord += z0;
 				//ReikaGuiAPI.write(String.format("%f -->  %.3f,  %.3f, %.3f", i, vec2.xCoord, vec2.yCoord, vec2.zCoord));
 				int id = world.getBlockId((int)vec2.xCoord, (int)vec2.yCoord, (int)vec2.zCoord);
 				if ((int)vec2.xCoord == x && (int)vec2.yCoord == y && (int)vec2.zCoord == z) {
 					//ReikaGuiAPI.writeCoords(world, (int)vec2.xCoord, (int)vec2.yCoord, (int)vec2.zCoord);
 					return true;
 				}
 				else if (id != 0 && id != locid && (isCollideable(world, (int)vec2.xCoord, (int)vec2.yCoord, (int)vec2.zCoord) && !softBlocks(id))) {
 					i = (float)(range + 1); //Hard loop break
 				}
 			}
 		}
 		return false;
 	}
 
 	/** Returns true if the entity can see a block, or if it could be moved to a position where it could see the block.
 	 * Args: World, Block x,y,z, Entity, Max Move Distance
 	 * DO NOT USE THIS - CPU INTENSIVE TO ALL HELL! */
 	public static boolean canSeeOrMoveToSeeBlock(World world, int x, int y, int z, Entity ent, double r) {
 		double d = 4;//+ReikaMathLibrary.py3d(x-ent.posX, y-ent.posY, z-ent.posZ);
 		if (canBlockSee(world, x, y, z, ent.posX, ent.posY, ent.posZ, d))
 			return true;
 		double xmin; double ymin; double zmin;
 		double xmax; double ymax; double zmax;
 		double[] pos = new double[3];
 		boolean[] signs = new boolean[3];
 		boolean[] signs2 = new boolean[3];
 		signs[0] = (ReikaMathLibrary.isSameSign(ent.posX, x));
 		signs[1] = (ReikaMathLibrary.isSameSign(ent.posY, y));
 		signs[2] = (ReikaMathLibrary.isSameSign(ent.posZ, z));
 		for (double i = ent.posX-r; i <= ent.posX+r; i += 0.5) {
 			for (double j = ent.posY-r; j <= ent.posY+r; j += 0.5) {
 				for (double k = ent.posZ-r; k <= ent.posZ+r; k += 0.5) {
 					if (canBlockSee(world, x, y, z, ent.posX+i, ent.posY+j, ent.posZ+k, d))
 						return true;
 				}
 			}
 		}
 		/*
     	for (double i = ent.posX; i > ent.posX-r; i -= 0.5) {
     		int id = world.getBlockId((int)i, (int)ent.posY, (int)ent.posZ);
     		if (isCollideable(world, (int)i, (int)ent.posY, (int)ent.posZ)) {
     			xmin = i+Block.blocksList[id].getBlockBoundsMaxX();
     		}
     	}
     	for (double i = ent.posX; i < ent.posX+r; i += 0.5) {
     		int id = world.getBlockId((int)i, (int)ent.posY, (int)ent.posZ);
     		if (isCollideable(world, (int)i, (int)ent.posY, (int)ent.posZ)) {
     			xmax = i+Block.blocksList[id].getBlockBoundsMinX();
     		}
     	}
     	for (double i = ent.posY; i > ent.posY-r; i -= 0.5) {
     		int id = world.getBlockId((int)ent.posX, (int)i, (int)ent.posZ);
     		if (isCollideable(world, (int)ent.posX, (int)i, (int)ent.posZ)) {
     			ymin = i+Block.blocksList[id].getBlockBoundsMaxX();
     		}
     	}
     	for (double i = ent.posY; i < ent.posY+r; i += 0.5) {
     		int id = world.getBlockId((int)ent.posX, (int)i, (int)ent.posZ);
     		if (isCollideable(world, (int)ent.posX, (int)i, (int)ent.posZ)) {
     			ymax = i+Block.blocksList[id].getBlockBoundsMinX();
     		}
     	}
     	for (double i = ent.posZ; i > ent.posZ-r; i -= 0.5) {
     		int id = world.getBlockId((int)ent.posX, (int)ent.posY, (int)i);
     		if (isCollideable(world, (int)ent.posX, (int)ent.posY, (int)i)) {
     			zmin = i+Block.blocksList[id].getBlockBoundsMaxX();
     		}
     	}
     	for (double i = ent.posZ; i < ent.posZ+r; i += 0.5) {
     		int id = world.getBlockId((int)ent.posX, (int)ent.posY, (int)i);
     		if (isCollideable(world, (int)ent.posX, (int)ent.posY, (int)i)) {
     			zmax = i+Block.blocksList[id].getBlockBoundsMinX();
     		}
     	}*/
 		signs2[0] = (ReikaMathLibrary.isSameSign(pos[0], x));
 		signs2[1] = (ReikaMathLibrary.isSameSign(pos[1], y));
 		signs2[2] = (ReikaMathLibrary.isSameSign(pos[2], z));
 		if (signs[0] != signs2[0] || signs[1] != signs2[1] || signs[2] != signs2[2]) //Cannot pull the item "Across" (so that it moves away)
 			return false;
 		return false;
 	}
 
 	public static boolean lenientSeeThrough(World world, double x, double y, double z, double x0, double y0, double z0) {
 		MovingObjectPosition pos;
 		Vec3 par1Vec3 = Vec3.fakePool.getVecFromPool(x, y, z);
 		Vec3 par2Vec3 = Vec3.fakePool.getVecFromPool(x0, y0, z0);
 		if (!Double.isNaN(par1Vec3.xCoord) && !Double.isNaN(par1Vec3.yCoord) && !Double.isNaN(par1Vec3.zCoord)) {
 			if (!Double.isNaN(par2Vec3.xCoord) && !Double.isNaN(par2Vec3.yCoord) && !Double.isNaN(par2Vec3.zCoord)) {
 				int var5 = MathHelper.floor_double(par2Vec3.xCoord);
 				int var6 = MathHelper.floor_double(par2Vec3.yCoord);
 				int var7 = MathHelper.floor_double(par2Vec3.zCoord);
 				int var8 = MathHelper.floor_double(par1Vec3.xCoord);
 				int var9 = MathHelper.floor_double(par1Vec3.yCoord);
 				int var10 = MathHelper.floor_double(par1Vec3.zCoord);
 				int var11 = world.getBlockId(var8, var9, var10);
 				int var12 = world.getBlockMetadata(var8, var9, var10);
 				Block var13 = Block.blocksList[var11];
 				//ReikaGuiAPI.write(var11);
 				if (var13 != null && (var11 > 0 && !ReikaWorldHelper.softBlocks(var11) && (var11 != Block.leaves.blockID) && (var11 != Block.web.blockID)) && var13.canCollideCheck(var12, false)) {
 					MovingObjectPosition var14 = var13.collisionRayTrace(world, var8, var9, var10, par1Vec3, par2Vec3);
 					if (var14 != null)
 						pos = var14;
 				}
 				var11 = 200;
 				while (var11-- >= 0) {
 					if (Double.isNaN(par1Vec3.xCoord) || Double.isNaN(par1Vec3.yCoord) || Double.isNaN(par1Vec3.zCoord))
 						pos = null;
 					if (var8 == var5 && var9 == var6 && var10 == var7)
 						pos = null;
 					boolean var39 = true;
 					boolean var40 = true;
 					boolean var41 = true;
 					double var15 = 999.0D;
 					double var17 = 999.0D;
 					double var19 = 999.0D;
 					if (var5 > var8)
 						var15 = var8 + 1.0D;
 					else if (var5 < var8)
 						var15 = var8 + 0.0D;
 					else
 						var39 = false;
 					if (var6 > var9)
 						var17 = var9 + 1.0D;
 					else if (var6 < var9)
 						var17 = var9 + 0.0D;
 					else
 						var40 = false;
 					if (var7 > var10)
 						var19 = var10 + 1.0D;
 					else if (var7 < var10)
 						var19 = var10 + 0.0D;
 					else
 						var41 = false;
 					double var21 = 999.0D;
 					double var23 = 999.0D;
 					double var25 = 999.0D;
 					double var27 = par2Vec3.xCoord - par1Vec3.xCoord;
 					double var29 = par2Vec3.yCoord - par1Vec3.yCoord;
 					double var31 = par2Vec3.zCoord - par1Vec3.zCoord;
 					if (var39)
 						var21 = (var15 - par1Vec3.xCoord) / var27;
 					if (var40)
 						var23 = (var17 - par1Vec3.yCoord) / var29;
 					if (var41)
 						var25 = (var19 - par1Vec3.zCoord) / var31;
 					boolean var33 = false;
 					byte var42;
 					if (var21 < var23 && var21 < var25) {
 						if (var5 > var8)
 							var42 = 4;
 						else
 							var42 = 5;
 						par1Vec3.xCoord = var15;
 						par1Vec3.yCoord += var29 * var21;
 						par1Vec3.zCoord += var31 * var21;
 					}
 					else if (var23 < var25) {
 						if (var6 > var9)
 							var42 = 0;
 						else
 							var42 = 1;
 						par1Vec3.xCoord += var27 * var23;
 						par1Vec3.yCoord = var17;
 						par1Vec3.zCoord += var31 * var23;
 					}
 					else {
 						if (var7 > var10)
 							var42 = 2;
 						else
 							var42 = 3;
 
 						par1Vec3.xCoord += var27 * var25;
 						par1Vec3.yCoord += var29 * var25;
 						par1Vec3.zCoord = var19;
 					}
 					Vec3 var34 = world.getWorldVec3Pool().getVecFromPool(par1Vec3.xCoord, par1Vec3.yCoord, par1Vec3.zCoord);
 					var8 = (int)(var34.xCoord = MathHelper.floor_double(par1Vec3.xCoord));
 					if (var42 == 5) {
 						--var8;
 						++var34.xCoord;
 					}
 					var9 = (int)(var34.yCoord = MathHelper.floor_double(par1Vec3.yCoord));
 					if (var42 == 1) {
 						--var9;
 						++var34.yCoord;
 					}
 					var10 = (int)(var34.zCoord = MathHelper.floor_double(par1Vec3.zCoord));
 					if (var42 == 3) {
 						--var10;
 						++var34.zCoord;
 					}
 					int var35 = world.getBlockId(var8, var9, var10);
 					int var36 = world.getBlockMetadata(var8, var9, var10);
 					Block var37 = Block.blocksList[var35];
 					if (var35 > 0 && var37.canCollideCheck(var36, false)) {
 						MovingObjectPosition var38 = var37.collisionRayTrace(world, var8, var9, var10, par1Vec3, par2Vec3);
 						if (var38 != null)
 							pos = var38;
 					}
 				}
 				pos = null;
 			}
 			else
 				pos = null;
 		}
 		else
 			pos = null;
 		return (pos == null);
 	}
 
 	/** Returns true if the block has a hitbox. Args: World, x, y, z */
 	public static boolean isCollideable(World world, int x, int y, int z) {
 		if (world.getBlockId(x, y, z) == 0)
 			return false;
 		Block b = Block.blocksList[world.getBlockId(x, y, z)];
 		return (b.getCollisionBoundingBoxFromPool(world, x, y, z) != null);
 	}
 
 	public static boolean legacySetBlockMetadataWithNotify(World world, int x, int y, int z, int meta) {
 		return world.setBlockMetadataWithNotify(x, y, z, meta, 3);
 	}
 
 	public static boolean legacySetBlockAndMetadataWithNotify(World world, int x, int y, int z, int id, int meta) {
 		return world.setBlock(x, y, z, id, meta, 3);
 	}
 
 	public static boolean legacySetBlockWithNotify(World world, int x, int y, int z, int id) {
 		return world.setBlock(x, y, z, id, 0, 3);
 	}
 
 	/** Returns true if the specified corner has at least one air block adjacent to it,
 	 * but is not surrounded by air on all sides or in the void. Args: World, x, y, z */
 	public static boolean cornerHasAirAdjacent(World world, int x, int y, int z) {
 		if (y <= 0)
 			return false;
 		int airs = 0;
 		if (world.getBlockId(x, y, z) == 0)
 			airs++;
 		if (world.getBlockId(x-1, y, z) == 0)
 			airs++;
 		if (world.getBlockId(x, y, z-1) == 0)
 			airs++;
 		if (world.getBlockId(x-1, y, z-1) == 0)
 			airs++;
 		if (world.getBlockId(x, y-1, z) == 0)
 			airs++;
 		if (world.getBlockId(x-1, y-1, z) == 0)
 			airs++;
 		if (world.getBlockId(x, y-1, z-1) == 0)
 			airs++;
 		if (world.getBlockId(x-1, y-1, z-1) == 0)
 			airs++;
 		return (airs > 0 && airs != 8);
 	}
 
 	/** Returns true if the specified corner has at least one nonopaque block adjacent to it,
 	 * but is not surrounded by air on all sides or in the void. Args: World, x, y, z */
 	public static boolean cornerHasTransAdjacent(World world, int x, int y, int z) {
 		if (y <= 0)
 			return false;
 		int id;
 		int airs = 0;
 		boolean nonopq = false;
 		id = world.getBlockId(x, y, z);
 		if (id == 0)
 			airs++;
 		else if (!Block.blocksList[id].isOpaqueCube())
 			nonopq = true;
 		id = world.getBlockId(x-1, y, z);
 		if (id == 0)
 			airs++;
 		else if (!Block.blocksList[id].isOpaqueCube())
 			nonopq = true;
 		id = world.getBlockId(x, y, z-1);
 		if (id == 0)
 			airs++;
 		else if (!Block.blocksList[id].isOpaqueCube())
 			nonopq = true;
 		id = world.getBlockId(x-1, y, z-1);
 		if (id == 0)
 			airs++;
 		else if (!Block.blocksList[id].isOpaqueCube())
 			nonopq = true;
 		id = world.getBlockId(x, y-1, z);
 		if (id == 0)
 			airs++;
 		else if (!Block.blocksList[id].isOpaqueCube())
 			nonopq = true;
 		id = world.getBlockId(x-1, y-1, z);
 		if (id == 0)
 			airs++;
 		else if (!Block.blocksList[id].isOpaqueCube())
 			nonopq = true;
 		id = world.getBlockId(x, y-1, z-1);
 		if (id == 0)
 			airs++;
 		else if (!Block.blocksList[id].isOpaqueCube())
 			nonopq = true;
 		id = world.getBlockId(x-1, y-1, z-1);
 		if (id == 0)
 			airs++;
 		else if (!Block.blocksList[id].isOpaqueCube())
 			nonopq = true;
 		return (airs != 8 && nonopq);
 	}
 
 	/** Spills the entire inventory of an ItemStack[] at the specified coordinates with a 1-block spread.
 	 * Args: World, x, y, z, inventory */
 	public static void spillAndEmptyInventory(World world, int x, int y, int z, ItemStack[] inventory) {
 		EntityItem ei;
 		ItemStack is;
 		for (int i = 0; i < inventory.length; i++) {
 			is = inventory[i];
 			inventory[i] = null;
 			if (is != null && !world.isRemote) {
 				ei = new EntityItem(world, x+rand.nextFloat(), y+rand.nextFloat(), z+rand.nextFloat(), is);
 				ReikaEntityHelper.addRandomDirVelocity(ei, 0.2);
 				world.spawnEntityInWorld(ei);
 			}
 		}
 	}
 
 	/** Spills the entire inventory of an ItemStack[] at the specified coordinates with a 1-block spread.
 	 * Args: World, x, y, z, IInventory */
 	public static void spillAndEmptyInventory(World world, int x, int y, int z, IInventory ii) {
 		int size = ii.getSizeInventory();
 		for (int i = 0; i < size; i++) {
 			ItemStack s = ii.getStackInSlot(i);
 			if (s != null) {
 				ii.setInventorySlotContents(i, null);
 				EntityItem ei = new EntityItem(world, x+rand.nextFloat(), y+rand.nextFloat(), z+rand.nextFloat(), s);
 				ReikaEntityHelper.addRandomDirVelocity(ei, 0.2);
 				ei.delayBeforeCanPickup = 10;
 				if (!world.isRemote)
 					world.spawnEntityInWorld(ei);
 			}
 		}
 	}
 
 	/** Spawns a line of particles between two points. Args: World, start x,y,z, end x,y,z, particle type, particle speed x,y,z, number of particles */
 	public static void spawnParticleLine(World world, double x1, double y1, double z1, double x2, double y2, double z2, String name, double vx, double vy, double vz, int spacing) {
 		double dx = x2-x1;
 		double dy = y2-y1;
 		double dz = z2-z1;
 		double sx = dx/spacing;
 		double sy = dy/spacing;
 		double sz = dy/spacing;
 		double[][] parts = new double[spacing+1][3];
 		for (int i = 0; i <= spacing; i++) {
 			parts[i][0] = i*sx+x1;
 			parts[i][1] = i*sy+y1;
 			parts[i][2] = i*sz+z1;
 		}
 		for (int i = 0; i < parts.length; i++) {
 			world.spawnParticle(name, parts[i][0], parts[i][1], parts[i][2], vx, vy, vz);
 		}
 	}
 
 	/** Checks if a liquid block is part of a column (has same liquid above and below and none of them are source blocks).
 	 * Args: World, x, y, z */
 	public static boolean isLiquidAColumn(World world, int x, int y, int z) {
 		Material mat = world.getBlockMaterial(x, y, z);
 		if (isLiquidSourceBlock(world, x, y, z))
 			return false;
 		if (world.getBlockMaterial(x, y+1, z) != mat)
 			return false;
 		if (isLiquidSourceBlock(world, x, y+1, z))
 			return false;
 		if (world.getBlockMaterial(x, y-1, z) != mat)
 			return false;
 		if (isLiquidSourceBlock(world, x, y-1, z))
 			return false;
 		return true;
 	}
 
 	public static void causeAdjacentUpdates(World world, int x, int y, int z) {
 		int id = world.getBlockId(x, y, z);
 		world.notifyBlocksOfNeighborChange(x, y, z, id);
 	}
 }
