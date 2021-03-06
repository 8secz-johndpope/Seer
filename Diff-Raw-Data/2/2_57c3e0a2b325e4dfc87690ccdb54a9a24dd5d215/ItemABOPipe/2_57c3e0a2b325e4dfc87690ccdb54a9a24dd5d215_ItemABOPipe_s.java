 /** 
  * Copyright (C) 2011 Flow86
  * 
  * AdditionalBuildcraftObjects is open-source.
  *
  * It is distributed under the terms of my Open Source License. 
  * It grants rights to read, modify, compile or run the code. 
  * It does *NOT* grant the right to redistribute this software or its 
  * modifications in any form, binary or source, except if expressively
  * granted by the copyright holder.
  */
 
 package net.minecraft.src.AdditionalBuildcraftObjects;
 
 import net.minecraft.src.Block;
 import net.minecraft.src.EntityPlayer;
 import net.minecraft.src.ItemStack;
 import net.minecraft.src.World;
 import net.minecraft.src.mod_AdditionalBuildcraftObjects;
 import net.minecraft.src.buildcraft.transport.ItemPipe;
 import net.minecraft.src.buildcraft.transport.Pipe;
 
 public class ItemABOPipe extends ItemPipe {
 
 	Pipe dummyPipe;
 
 	protected ItemABOPipe(int i) {
 		super(i);
 	}
 
 	@Override
 	public boolean onItemUse(ItemStack itemstack, EntityPlayer entityplayer, World world, int i, int j, int k, int l) {
 		int blockID = mod_AdditionalBuildcraftObjects.blockABOPipe.blockID;
 
 		if (world.getBlockId(i, j, k) == Block.snow.blockID) {
 			l = 0;
 		} else {
 			if (l == 0) {
 				j--;
 			}
 			if (l == 1) {
 				j++;
 			}
 			if (l == 2) {
 				k--;
 			}
 			if (l == 3) {
 				k++;
 			}
 			if (l == 4) {
 				i--;
 			}
 			if (l == 5) {
 				i++;
 			}
 		}
 		if (itemstack.stackSize == 0) {
 			return false;
 		}
 		if (j == 127 && Block.blocksList[blockID].blockMaterial.isSolid()) {
 			return false;
 		}
 		if (world.canBlockBePlacedAt(blockID, i, j, k, false, l)) {
			BlockABOPipe.createPipe(i, j, k, shiftedIndex);
 			if (world.setBlockAndMetadataWithNotify(i, j, k, blockID, 0)) {
 				Block.blocksList[blockID].onBlockPlaced(world, i, j, k, l);
 				Block.blocksList[blockID].onBlockPlacedBy(world, i, j, k, entityplayer);
 				// To move to a proxt
 				// world.playSoundEffect((float)i + 0.5F, (float)j + 0.5F,
 				// (float)k + 0.5F, block.stepSound.stepSoundDir2(),
 				// (block.stepSound.getVolume() + 1.0F) / 2.0F,
 				// block.stepSound.getPitch() * 0.8F);
 				itemstack.stackSize--;
 			}
 			return true;
 		} else {
 			return false;
 		}
 	}
 
 	@Override
 	public String getTextureFile() {
 		return mod_AdditionalBuildcraftObjects.customTexture;
 	}
 
 	@Override
 	public int getTextureIndex() {
 		if (dummyPipe == null) {
 			dummyPipe = BlockABOPipe.createPipe(shiftedIndex);
 		}
 
 		return dummyPipe.getBlockTexture();
 	}
 }
