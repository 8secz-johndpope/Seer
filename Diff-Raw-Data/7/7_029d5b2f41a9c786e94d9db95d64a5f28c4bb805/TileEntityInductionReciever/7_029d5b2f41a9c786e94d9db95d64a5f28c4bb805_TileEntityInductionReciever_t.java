 package electricexpansion.common.tile;
 
 import net.minecraft.entity.player.EntityPlayer;
 import net.minecraft.inventory.IInventory;
 import net.minecraft.item.ItemStack;
 import net.minecraft.nbt.NBTTagCompound;
 import net.minecraft.network.INetworkManager;
 import net.minecraft.network.packet.Packet;
 import net.minecraft.network.packet.Packet250CustomPayload;
 import net.minecraft.tileentity.TileEntity;
 import net.minecraftforge.common.ForgeDirection;
 import universalelectricity.core.electricity.ElectricInfo;
 import universalelectricity.core.implement.IConductor;
 import universalelectricity.core.implement.IJouleStorage;
 import universalelectricity.core.vector.Vector3;
 import universalelectricity.prefab.implement.IRedstoneProvider;
 import universalelectricity.prefab.network.IPacketReceiver;
 import universalelectricity.prefab.network.PacketManager;
 import universalelectricity.prefab.tile.TileEntityDisableable;
 
 import com.google.common.io.ByteArrayDataInput;
 
 import dan200.computer.api.IComputerAccess;
 import dan200.computer.api.IPeripheral;
 import electricexpansion.api.WirelessPowerMachine;
 import electricexpansion.common.ElectricExpansion;
 import electricexpansion.common.wpt.InductionNetworks;
 
 public class TileEntityInductionReciever extends TileEntityDisableable implements IPacketReceiver, IJouleStorage, IPeripheral, IRedstoneProvider, IInventory, WirelessPowerMachine
 {
 	private double joules = 0;
 	private int playersUsing = 0;
 	private short frequency;
 	private static final double maxJoules = 500000; // To eventually go in config #Eventually
 	private byte orientation;
 	private boolean isOpen = false;
 	private double outputVoltage = 120;
 
 	@Override
 	public short getFrequency()
 	{
 		return frequency;
 	}
 
 	@Override
 	public void setFrequency(short newFrequency)
 	{
		if (newFrequency != (Short)null)
		{
			InductionNetworks.setRecieverFreq(this.frequency, newFrequency, this);
			this.frequency = newFrequency;
		}
 	}
 
 	public void setFrequency(int frequency)
 	{
 		this.setFrequency((short) frequency);
 	}
 
 	private int setFrequency(int frequency, boolean b)
 	{
 		return this.setFrequency((short) frequency, b);
 	}
 
 	private int setFrequency(short frequency, boolean b)
 	{
 		this.setFrequency(frequency);
 		return this.frequency;
 	}
 
 	public boolean canWirelessRecieve(double input)
 	{
 		return (this.joules + input <= this.maxJoules);
 	}
 
 	public void wirelessRecieve(double input)
 	{
 		this.addJoules(input);
 	}
 
 	@Override
 	public boolean canUpdate()
 	{
 		return true;
 	}
 
 	public TileEntityInductionReciever()
 	{
 		super();
 	}
 
 	@Override
 	public void updateEntity()
 	{
 		super.updateEntity();
 		if (this.joules < 0)
 			this.joules = 0;
 		if (this.joules > this.maxJoules)
 			this.joules = this.maxJoules;
 		if (!this.worldObj.isRemote)
 			this.sendPacket();
 		if (this.orientation != this.blockMetadata)
 			this.orientation = (byte) ForgeDirection.getOrientation(this.blockMetadata).ordinal();
 
 		/**
 		 * Output Electricity
 		 */
 
 		if (this.getJoules() > 0)
 		{
 			ForgeDirection outputDirection = ForgeDirection.getOrientation(this.getBlockMetadata() - blockMetadata);
 
 			TileEntity connector = Vector3.getConnectorFromSide(this.worldObj, new Vector3(this), ForgeDirection.getOrientation(this.blockMetadata));
 			// Output UE electricity
 			if (connector instanceof IConductor)
 			{
 				double joulesNeeded = ((IConductor) connector).getNetwork().getRequest().getWatts();
 				double transferAmps = Math.max(Math.min(Math.min(ElectricInfo.getAmps(joulesNeeded, this.outputVoltage), ElectricInfo.getAmps(this.joules, this.outputVoltage)), 80), 0);
 				if (!this.worldObj.isRemote && transferAmps > 0)
 				{
 					((IConductor) connector).getNetwork().startProducing(this, transferAmps, this.outputVoltage);
 					this.setJoules(this.joules - ElectricInfo.getJoules(transferAmps, this.outputVoltage));
 					System.out.println("PROD");
 				}
 				else
 				{
 					((IConductor) connector).getNetwork().stopProducing(this);
 				}
 			}
 		}
 	}
 
 	private void sendPacket()
 	{
 		PacketManager.sendPacketToClients(this.getDescriptionPacket(), this.worldObj, new Vector3(this), 8);
 	}
 
 	@Override
 	public Packet getDescriptionPacket()
 	{
 		return PacketManager.getPacket("ElecEx", this, this.joules, this.disabledTicks);
 	}
 
 	@Override
 	public boolean isUseableByPlayer(EntityPlayer par1EntityPlayer)
 	{
 		return this.worldObj.getBlockTileEntity(this.xCoord, this.yCoord, this.zCoord) != this ? false : par1EntityPlayer.getDistanceSq(this.xCoord + 0.5D, this.yCoord + 0.5D, this.zCoord + 0.5D) <= 64.0D;
 	}
 
 	@Override
 	public void openChest()
 	{
 		if (!this.worldObj.isRemote)
 			PacketManager.sendPacketToClients(getDescriptionPacket(), this.worldObj, new Vector3(this), 15);
 		this.playersUsing++;
 	}
 
 	@Override
 	public void closeChest()
 	{
 		this.playersUsing--;
 	}
 
 	@Override
 	public int getSizeInventory()
 	{
 		return 0;
 	}
 
 	@Override
 	public ItemStack getStackInSlot(int var1)
 	{
 		return null;
 	}
 
 	@Override
 	public ItemStack decrStackSize(int var1, int var2)
 	{
 		return null;
 	}
 
 	@Override
 	public ItemStack getStackInSlotOnClosing(int var1)
 	{
 		return null;
 	}
 
 	@Override
 	public void setInventorySlotContents(int var1, ItemStack var2)
 	{
 	}
 
 	@Override
 	public String getInvName()
 	{
 		return "Induction Power Sender";
 	}
 
 	@Override
 	public int getInventoryStackLimit()
 	{
 		return 0;
 	}
 
 	public boolean isFull()
 	{
 		return this.joules == this.maxJoules;
 	}
 
 	@Override
 	public void readFromNBT(NBTTagCompound par1NBTTagCompound)
 	{
 		super.readFromNBT(par1NBTTagCompound);
 		this.joules = par1NBTTagCompound.getDouble("joules");
 		this.frequency = par1NBTTagCompound.getShort("frequency");
 	}
 
 	@Override
 	public void writeToNBT(NBTTagCompound par1NBTTagCompound)
 	{
 		super.writeToNBT(par1NBTTagCompound);
 		par1NBTTagCompound.setDouble("joules", this.joules);
 		par1NBTTagCompound.setShort("frequency", this.frequency);
 	}
 
 	@Override
 	public void handlePacketData(INetworkManager network, int packetType, Packet250CustomPayload packet, EntityPlayer player, ByteArrayDataInput dataStream)
 	{
 		try
 		{
 			this.joules = dataStream.readDouble();
 			this.disabledTicks = dataStream.readInt();
 		}
 		catch (Exception e)
 		{
 			e.printStackTrace();
 		}
 	}
 
 	@Override
 	public boolean isPoweringTo(ForgeDirection side)
 	{
 		boolean returnValue = false;
 		if (this.joules == (double) this.maxJoules)
 			returnValue = true;
 		return returnValue;
 	}
 
 	@Override
 	public boolean isIndirectlyPoweringTo(ForgeDirection side)
 	{
 		boolean returnValue = false;
 		if (this.joules == (double) this.maxJoules)
 			returnValue = true;
 		return returnValue;
 	}
 
 	@Override
 	public double getJoules(Object... data)
 	{
 		return joules;
 	}
 
 	@Override
 	public void setJoules(double wattHours, Object... data)
 	{
 		this.joules = Math.max(Math.min(joules, this.getMaxJoules()), 0);
 	}
 
 	public void addJoules(double extraJoules)
 	{
 		this.joules = this.joules + extraJoules;
 	}
 
 	@Override
 	public double getMaxJoules(Object... data)
 	{
 		return maxJoules;
 	}
 
 	@Override
 	public String getType()
 	{
 		return "Wireless Power Transmitter";
 	}
 
 	@Override
 	public String[] getMethodNames()
 	{
 		return new String[] { "", "getWattage", "isFull", "getJoules", "getFrequency", "setFrequency" };
 	}
 
 	@Override
 	public Object[] callMethod(IComputerAccess computer, int method, Object[] arguments) throws IllegalArgumentException
 	{
 		final int getWattage = 1;
 		final int isFull = 2;
 		final int getJoules = 3;
 		final int getFrequency = 4;
 		final int setFrequency = 5;
 		int arg0 = 0;
 		try
 		{
 			if ((Integer) arguments[0] != null)
 				arg0 = ((Integer) arguments[0]).intValue();
 		}
 		catch (Exception e)
 		{
 			ElectricExpansion.EELogger.fine("Failed to get new frequency, from ComputerCraft functions.");
 		}
 
 		if (!this.isDisabled())
 		{
 			switch (method)
 			{
 				case getWattage:
 					return new Object[] { ElectricInfo.getWatts(joules) };
 				case isFull:
 					return new Object[] { isFull() };
 				case getJoules:
 					return new Object[] { getJoules() };
 				case getFrequency:
 					return new Object[] { getFrequency() };
 				case setFrequency:
 					return new Object[] { setFrequency(arg0, true) };
 				default:
 					throw new IllegalArgumentException("Function unimplemented");
 			}
 		}
 		else
 			return new Object[] { "Please wait for the EMP to run out." };
 	}
 
 	@Override
 	public boolean canAttachToSide(int side)
 	{
 		return side != this.blockMetadata;
 	}
 
 	@Override
 	public void attach(IComputerAccess computer, String computerSide)
 	{
 	}
 
 	@Override
 	public void detach(IComputerAccess computer)
 	{
 	}
 
 }
