 package assemblyline.common.machine.armbot;
 
 import net.minecraft.entity.player.EntityPlayer;
 import net.minecraft.item.ItemStack;
 import net.minecraft.tileentity.TileEntity;
 import net.minecraft.world.World;
 import universalelectricity.core.UniversalElectricity;
 import universalelectricity.core.vector.Vector3;
 import universalelectricity.prefab.BlockMachine;
 import universalelectricity.prefab.multiblock.IMultiBlock;
 import assemblyline.client.render.BlockRenderingHandler;
 import assemblyline.common.TabAssemblyLine;
 import cpw.mods.fml.relauncher.Side;
 import cpw.mods.fml.relauncher.SideOnly;
 
 public class BlockArmbot extends BlockMachine
 {
 	public BlockArmbot(int id)
 	{
 		super("armbot", id, UniversalElectricity.machine);
		this.setResistance(5.0f);
		this.setHardness(5.0f);
 		this.setCreativeTab(TabAssemblyLine.INSTANCE);
 	}
 
 	@Override
 	public void onBlockAdded(World world, int x, int y, int z)
 	{
 		TileEntity tileEntity = world.getBlockTileEntity(x, y, z);
 
 		if (tileEntity != null && tileEntity instanceof IMultiBlock)
 		{
 			((IMultiBlock) tileEntity).onCreate(new Vector3(x, y, z));
 		}
 	}
 
 	@Override
 	public boolean onMachineActivated(World world, int x, int y, int z, EntityPlayer player, int par6, float par7, float par8, float par9)
 	{
 		TileEntity tileEntity = world.getBlockTileEntity(x, y, z);
 
 		if (tileEntity != null && tileEntity instanceof IMultiBlock)
 		{
 			return ((IMultiBlock) tileEntity).onActivated(player);
 		}
 
 		return false;
 	}
 
 	@Override
 	public void breakBlock(World world, int x, int y, int z, int par5, int par6)
 	{
 		TileEntity tileEntity = world.getBlockTileEntity(x, y, z);
 
 		if (tileEntity != null && tileEntity instanceof IMultiBlock)
 		{
 			((IMultiBlock) tileEntity).onDestroy(tileEntity);
 		}
 
 		this.dropBlockAsItem_do(world, x, y, z, new ItemStack(this));
 
 		super.breakBlock(world, x, y, z, par5, par6);
 	}
 
 	@Override
 	public TileEntity createNewTileEntity(World var1)
 	{
 		return new TileEntityArmbot();
 	}
 
 	@SideOnly(Side.CLIENT)
 	@Override
 	public int getRenderType()
 	{
 		return BlockRenderingHandler.BLOCK_RENDER_ID;
 	}
 
 	@Override
 	public boolean isOpaqueCube()
 	{
 		return false;
 	}
 }
