 package mDiyo.inficraft.flora.trees.blocks;
 
 import java.util.ArrayList;
import java.util.Random;
 
 import mDiyo.inficraft.flora.trees.FloraTrees;
 import net.minecraft.src.Block;
 import net.minecraft.src.BlockLog;
 import net.minecraft.src.IBlockAccess;
 import net.minecraft.src.ItemStack;
 import net.minecraft.src.World;
 import net.minecraftforge.common.ForgeDirection;
 
 public class TreeBlock extends BlockLog
 {
     public TreeBlock(int i)
     {
         super(i);
         blockIndexInTexture = 0;
         this.setHardness(1.5F);
         this.setResistance(5F);
         this.setStepSound(Block.soundWoodFootstep);        
         this.setRequiresSelfNotify();
         setBurnProperties(this.blockID, 5, 20);
     }
     
     public int getBlockTextureFromSideAndMetadata(int side, int metadata)
     {
     	int tex = blockIndexInTexture + (metadata % 4);
     	int orientation = metadata / 4;
     	
     	switch (orientation) //Ends of logs
     	{
     	case 0:
     		if (side == 0 || side == 1)
     			return tex + 16;
     		break;
     	case 1:
     		if (side == 4 || side == 5)
     			return tex + 16;
     		break;
     	case 2:
     		if (side == 2 || side == 3)
     			return tex + 16;
     		break;
     	}
     	
     	return tex;
     }
    
    public int idDropped(int par1, Random par2Random, int par3)
    {
        return this.blockID;
    }
 
     @Override
     public int damageDropped(int meta)
     {
         return meta % 4;
     }
 
     public String getTextureFile()
     {
         return FloraTrees.texture;
     }
 
     public int getFlammability(IBlockAccess world, int x, int y, int z, int metadata, ForgeDirection face)
     {
         return metadata % 4 != 2 ? blockFlammability[blockID] : 0;
     }
 
     public int getFireSpreadSpeed(World world, int x, int y, int z, int metadata, ForgeDirection face)
     {
         return metadata % 4 != 2 ? blockFireSpreadSpeed[blockID] : 0;
     }
 
     public void addCreativeItems(ArrayList arraylist)
     {
         arraylist.add(new ItemStack(FloraTrees.tree, 1, 0));
         arraylist.add(new ItemStack(FloraTrees.tree, 1, 1));
         arraylist.add(new ItemStack(FloraTrees.tree, 1, 2));
     }
 }
