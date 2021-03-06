 package electricexpansion.mattredsox.tileentities;
 
 import java.util.EnumSet;
 
 import net.minecraft.src.EntityPlayer;
 import net.minecraft.src.IInventory;
 import net.minecraft.src.INetworkManager;
 import net.minecraft.src.ItemStack;
 import net.minecraft.src.NBTTagCompound;
 import net.minecraft.src.NBTTagList;
 import net.minecraft.src.Packet;
 import net.minecraft.src.Packet250CustomPayload;
 import net.minecraft.src.TileEntity;
 import net.minecraftforge.common.ForgeDirection;
 import universalelectricity.core.electricity.ElectricInfo;
 import universalelectricity.core.electricity.ElectricityConnections;
 import universalelectricity.core.electricity.ElectricityPack;
 import universalelectricity.core.implement.IConductor;
 import universalelectricity.core.vector.Vector3;
 import universalelectricity.prefab.modifier.IModifier;
 import universalelectricity.prefab.network.IPacketReceiver;
 import universalelectricity.prefab.network.PacketManager;
 import universalelectricity.prefab.tile.TileEntityElectricityReceiver;
 import buildcraft.api.power.IPowerProvider;
 
 import com.google.common.io.ByteArrayDataInput;
 
 import cpw.mods.fml.common.registry.LanguageRegistry;
 import electricexpansion.ElectricExpansion;
 import electricexpansion.mattredsox.blocks.BlockTransformer;
 import electricexpansion.mattredsox.items.ItemTransformerCoil;
 
 public class TileEntityTransformer extends TileEntityElectricityReceiver implements IPacketReceiver, IInventory
 {	
 	public ItemStack[] containingItems = new ItemStack[2];
 
 	private int playersUsing = 0;
 
 	public static final double VOLTAGE_DECREASE = 0;
 	
 	public TileEntityTransformer()
 	{
 		super();
 	}
 
 	@Override
 	public void initiate()
 	{
 		ElectricityConnections.registerConnector(this, EnumSet.of(ForgeDirection.getOrientation(this.getBlockMetadata() - BlockTransformer.meta + 4), ForgeDirection.getOrientation(this.getBlockMetadata() - BlockTransformer.meta + 4).getOpposite()));
 	}
 
 	@Override
 	public void updateEntity()
 	{
 		super.updateEntity();
 
 		if (!this.isDisabled())
 		{
 			if (!this.worldObj.isRemote)
 			{

 				ElectricityPack receivePack = new ElectricityPack(0, 0);
 				
				ForgeDirection outputDirection = ForgeDirection.getOrientation(this.getBlockMetadata() - BlockTransformer.meta + 2);
				TileEntity outputTile = Vector3.getTileEntityFromSide(this.worldObj, Vector3.get(this), outputDirection);
				
 				ForgeDirection inputDirection = ForgeDirection.getOrientation(this.getBlockMetadata() - BlockTransformer.meta + 2).getOpposite();
 				TileEntity inputTile = Vector3.getTileEntityFromSide(this.worldObj, Vector3.get(this), inputDirection);
 
				if (inputTile != null && outputTile != null)
 				{
					if (inputTile instanceof IConductor && outputTile instanceof IConductor)
					{
						((IConductor) inputTile).getNetwork().startRequesting(this, ((IConductor)outputTile).getNetwork().getRequest());
						receivePack = ((IConductor) inputTile).getNetwork().consumeElectricity(this);
 					}
 				}
 			
 				/**
 				 * Output Electricity
 				 */
 /*
 				if (this.receivePack.getWatts() > 0)
 				{
 
 					if (tileEntity != null)
 					{
 						TileEntity connector = Vector3.getConnectorFromSide(this.worldObj, Vector3.get(this), outputDirection);
 
 						// Output UE electricity
 						if (connector instanceof IConductor)
 						{
 							double joulesNeeded = ((IConductor) connector).getNetwork().getRequest().getWatts();
 							double transferAmps = Math.max(Math.min(Math.min(ElectricInfo.getAmps(joulesNeeded, receivePack.voltage), ElectricInfo.getAmps(this.joules, voltageAdd)), 80), 0);
 
 							if (transferAmps > 0)
 							{
 								((IConductor) connector).getNetwork().startProducing(this, receivePack.amperes, receivePack.voltage + VOLTAGE_DECREASE);
 								//this.setJoules(this.joules - ElectricInfo.getWatts(transferAmps, voltageAdd));
 							}
 							else
 							{
 								((IConductor) connector).getNetwork().stopProducing(this);
 							}
 
 						}
 
 					}*/
 			//	}
 			}
 		}
 	}
 
 	@Override
 	public void openChest()
 	{
 		this.playersUsing++;
 	}
 
 	@Override
 	public void closeChest()
 	{
 		this.playersUsing--;
 		this.worldObj.markBlockForUpdate(xCoord, yCoord, zCoord);
 	}
 
 	/**
 	 * Reads a tile entity from NBT.
 	 */
 	@Override
 	public void readFromNBT(NBTTagCompound par1NBTTagCompound)
 	{
 		super.readFromNBT(par1NBTTagCompound);
 
 		NBTTagList var2 = par1NBTTagCompound.getTagList("Items");
 		this.containingItems = new ItemStack[this.getSizeInventory()];
 
 		for (int var3 = 0; var3 < var2.tagCount(); ++var3)
 		{
 			NBTTagCompound var4 = (NBTTagCompound) var2.tagAt(var3);
 			byte var5 = var4.getByte("Slot");
 
 			if (var5 >= 0 && var5 < this.containingItems.length)
 			{
 				this.containingItems[var5] = ItemStack.loadItemStackFromNBT(var4);
 			}
 		}
 	}
 
 	/**
 	 * Writes a tile entity to NBT.
 	 */
 	@Override
 	public void writeToNBT(NBTTagCompound par1NBTTagCompound)
 	{
 		super.writeToNBT(par1NBTTagCompound);
 		NBTTagList var2 = new NBTTagList();
 
 		for (int var3 = 0; var3 < this.containingItems.length; ++var3)
 		{
 			if (this.containingItems[var3] != null)
 			{
 				NBTTagCompound var4 = new NBTTagCompound();
 				var4.setByte("Slot", (byte) var3);
 				this.containingItems[var3].writeToNBT(var4);
 				var2.appendTag(var4);
 			}
 		}
 
 		par1NBTTagCompound.setTag("Items", var2);
 	}
 
 	@Override
 	public int getSizeInventory()
 	{
 		return this.containingItems.length;
 	}
 
 	@Override
 	public ItemStack getStackInSlot(int par1)
 	{
 		return this.containingItems[par1];
 	}
 
 	@Override
 	public ItemStack decrStackSize(int par1, int par2)
 	{
 		if (this.containingItems[par1] != null)
 		{
 			ItemStack var3;
 
 			if (this.containingItems[par1].stackSize <= par2)
 			{
 				var3 = this.containingItems[par1];
 				this.containingItems[par1] = null;
 				return var3;
 			}
 			else
 			{
 				var3 = this.containingItems[par1].splitStack(par2);
 
 				if (this.containingItems[par1].stackSize == 0)
 				{
 					this.containingItems[par1] = null;
 				}
 
 				return var3;
 			}
 		}
 		else
 		{
 			return null;
 		}
 	}
 
 	@Override
 	public ItemStack getStackInSlotOnClosing(int par1)
 	{
 		if (this.containingItems[par1] != null)
 		{
 			ItemStack var2 = this.containingItems[par1];
 			this.containingItems[par1] = null;
 			return var2;
 		}
 		else
 		{
 			return null;
 		}
 	}
 
 	@Override
 	public void setInventorySlotContents(int par1, ItemStack par2ItemStack)
 	{
 		this.containingItems[par1] = par2ItemStack;
 
 		if (par2ItemStack != null && par2ItemStack.stackSize > this.getInventoryStackLimit())
 		{
 			par2ItemStack.stackSize = this.getInventoryStackLimit();
 		}
 	}
 
 	@Override
 	public String getInvName()
 	{
 		return "Transformer";
 	}
 
 	@Override
 	public int getInventoryStackLimit()
 	{
 		return 4;
 	}
 
 	@Override
 	public boolean isUseableByPlayer(EntityPlayer par1EntityPlayer)
 	{
 		return this.worldObj.getBlockTileEntity(this.xCoord, this.yCoord, this.zCoord) != this ? false : par1EntityPlayer.getDistanceSq(this.xCoord + 0.5D, this.yCoord + 0.5D, this.zCoord + 0.5D) <= 64.0D;
 	}
 
 	@Override
 	public Packet getDescriptionPacket()
 	{
 		return PacketManager.getPacket(ElectricExpansion.CHANNEL, this, this.receivePack.voltage);
 
 	}
 
 	@Override
 	public void handlePacketData(INetworkManager network, int type, Packet250CustomPayload packet, EntityPlayer player, ByteArrayDataInput dataStream)
 	{
 		try
 		{
 			
 			this.receivePack.voltage = dataStream.readDouble();
 			
 
 		}
 		catch (Exception e)
 		{
 			e.printStackTrace();
 		}
 	}
 }
