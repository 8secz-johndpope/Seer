 package biomesoplenty.itemblocks;
 
 import net.minecraft.block.Block;
 import net.minecraft.item.ItemBlock;
 import net.minecraft.item.ItemStack;
 import biomesoplenty.blocks.BlockBOPColorizedLeaves;
 
 public class ItemBlockColorizedLeaves extends ItemBlock
 {
 	public ItemBlockColorizedLeaves(int par1)
 	{
 		super(par1);
 		setMaxDamage(0);
 		setHasSubtypes(true);
 	}
 
 	@Override
 	public int getMetadata(int meta)
 	{
		return meta;
 	}
 
 	@Override
 	public String getUnlocalizedName(ItemStack itemStack)
 	{
 		BlockBOPColorizedLeaves block = (BlockBOPColorizedLeaves)Block.blocksList[itemStack.itemID];		
 		return super.getUnlocalizedName() + "." + block.getLeafType(itemStack.getItemDamage());
 	}
 }
