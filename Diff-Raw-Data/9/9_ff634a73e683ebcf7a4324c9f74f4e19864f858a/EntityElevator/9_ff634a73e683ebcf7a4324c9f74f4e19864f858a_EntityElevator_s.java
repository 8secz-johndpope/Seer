 package slimevoid.dynamictransport.entities;
 
 import java.util.HashSet;
 import java.util.Iterator;
 import java.util.Set;
 
 import net.minecraft.entity.Entity;
 import net.minecraft.item.ItemStack;
 import net.minecraft.nbt.NBTTagCompound;
 import net.minecraft.network.packet.Packet3Chat;
 import net.minecraft.server.MinecraftServer;
 import net.minecraft.tileentity.TileEntity;
 import net.minecraft.util.AxisAlignedBB;
 import net.minecraft.util.ChatMessageComponent;
 import net.minecraft.util.ChunkCoordinates;
 import net.minecraft.util.MathHelper;
 import net.minecraft.world.World;
 import slimevoid.dynamictransport.core.lib.BlockLib;
 import slimevoid.dynamictransport.core.lib.ConfigurationLib;
 import slimevoid.dynamictransport.tileentity.TileEntityElevator;
 import slimevoid.dynamictransport.tileentity.TileEntityElevatorComputer;
 import slimevoidlib.util.helpers.BlockHelper;
 
 public class EntityElevator extends Entity {
 	// Constants
 	private static final float	elevatorAccel			= 0.01F;
 	private static final float	minElevatorMovingSpeed	= 0.016F;
 
 	// server only
 	private ChunkCoordinates	computerPos				= null;
 	private String				elevatorName			= "";
 	private String				destFloorName			= "";
 	private boolean				canBeHalted				= true;
 	private boolean				enableMobilePower		= false;
 
 	// only needed for emerhalt but also used in kill all conjoined
 	public Set<Integer>			conjoinedelevators		= new HashSet<Integer>();
 	public Set<Integer>			confirmedRiders			= new HashSet<Integer>();
 
 	// possible watcher
 	private byte				waitToAccelerate		= 0;
 	public int					startStops				= 0;
 	public int					notifierElevatorID		= 0;
 	public boolean				emerHalt				= false;
 	public boolean				isNotifierElevator		= false;
 
 	// most likely fine
 	private byte				stillCount				= 0;
 	private boolean				slowingDown				= false;
 
 	public EntityElevator(World world) {
 		super(world);
 		this.preventEntitySpawning = true;
 		this.isImmuneToFire = true;
 		this.entityCollisionReduction = 1.0F;
 		this.ignoreFrustumCheck = true;
 		setSize(0.98F,
 				0.98F);
 
 		motionX = 0.0D;
 		motionY = 0.0D;
 		motionZ = 0.0D;
 		waitToAccelerate = 100;
 	}
 
 	public EntityElevator(World world, double i, double j, double k) {
 		this(world);
 		prevPosX = i + 0.5F;
 		prevPosY = j + 0.5F;
 		prevPosZ = k + 0.5F;
 		setPosition(prevPosX,
 					prevPosY,
 					prevPosZ);
 
 		isNotifierElevator = false;
 
 		waitToAccelerate = 0;
 		this.updatePotentialRiders();
 	}
 
 	public int getDestinationY() {
 		return this.getDataWatcher().getWatchableObjectInt(2);
 	}
 
 	public float getMaximumSpeed() {
 		return this.getDataWatcher().getWatchableObjectFloat(3);
 	}
 
 	public ItemStack getCamoItem() {
 		return this.getDataWatcher().getWatchableObjectItemStack(4);
 	}
 
 	protected void setDestinationY(int destinationY) {
 		this.getDataWatcher().updateObject(	2,
 											destinationY);
 	}
 
 	protected void setMaximumSpeed(float speed) {
 		this.getDataWatcher().updateObject(	3,
 											speed);
 	}
 
 	protected void setCamoItem(ItemStack itemstack) {
 		this.getDataWatcher().updateObject(	4,
 											itemstack);
 	}
 
 	@Override
 	protected void entityInit() {
 		this.getDataWatcher().addObject(2,
 										new Integer(-1));
 		this.getDataWatcher().addObject(3,
 										0f);
 		this.getDataWatcher().addObjectByDataType(	4,
 													5);
 	}
 
 	@Override
 	public void setDead() {
 		int i = MathHelper.floor_double(posX);
 		int k = MathHelper.floor_double(posZ);
 		int curY = MathHelper.floor_double(posY);
 
 		boolean blockPlaced = !worldObj.isRemote
 								&& (worldObj.getBlockId(i,
 														curY,
 														k) == ConfigurationLib.blockTransportBaseID || worldObj.canPlaceEntityOnSide(	ConfigurationLib.blockTransportBaseID,
 																																		i,
 																																		curY,
 																																		k,
 																																		true,
 																																		1,
 																																		(Entity) null,
 																																		null)
 																										&& worldObj.setBlock(	i,
 																																curY,
 																																k,
 																																ConfigurationLib.blockTransportBaseID,
 																																BlockLib.BLOCK_ELEVATOR_ID,
 																																3));
 
 		if (!worldObj.isRemote) {
 			if (blockPlaced) {
 				TileEntityElevator tile = (TileEntityElevator) this.worldObj.getBlockTileEntity(i,
 																								curY,
 																								k);
 				if (tile != null) {
 					tile.setParentElevatorComputer(this.computerPos);
 					if (this.getCamoItem() != null) {
 						tile.setCamoItem(this.getCamoItem());
 					}
 				}
 			} else {
 				entityDropItem(	new ItemStack(ConfigurationLib.blockTransportBaseID, 1, BlockLib.BLOCK_ELEVATOR_ID),
 								0.0F);
 			}
 		}
 		this.updateRiders(true);
 
 		if (!worldObj.isRemote) {
 			if (isNotifierElevator) {
 
 				MinecraftServer.getServer().getConfigurationManager().sendToAllNear(this.posX,
 																					this.posY,
 																					this.posZ,
 																					4,
 																					this.worldObj.provider.dimensionId,
 																					new Packet3Chat(this.elevatorName != null
 																									&& !this.elevatorName.trim().equals("") ? ChatMessageComponent.createFromTranslationWithSubstitutions(	"slimevoid.DT.entityElevator.arriveWithName",
 																																																			this.elevatorName,
 																																																			destFloorName) : ChatMessageComponent.createFromTranslationWithSubstitutions(	"slimevoid.DT.entityElevator.arrive",
 																																																																							this.destFloorName)));
 
 			}
 
 		}
 		super.setDead();
 	}
 
 	@Override
 	public void onUpdate() {
 		int x = MathHelper.floor_double(posX);
 		int y = MathHelper.floor_double(posY);
 		int z = MathHelper.floor_double(posZ);
 
 		// on first update remove blocks
 		if (!worldObj.isRemote && this.ticksExisted == 1) {
 			removeElevatorBlock(x,
 								y,
 								z);
 		}
 
 		if (this.getDestinationY() == -1) return;
 
 		// Place transient block
 		if (!worldObj.isRemote && !this.isDead && this.enableMobilePower) {
 			this.setTransitBlocks(	x,
 									y,
 									z);
 		}
 		if (this.velocityChanged) {
 			this.velocityChanged = false;
 			setEmerHalt(!emerHalt);
 
 			startStops++;
 			if (startStops > 2) {
 				setDead();
 			}
 		}
 
 		float destY = this.getDestinationY() + 0.5F;
 		float elevatorSpeed = (float) Math.abs(this.motionY);
 		if (emerHalt) {
 			elevatorSpeed = 0;
 		} else if (waitToAccelerate < 15) {
 			if (waitToAccelerate < 10) {
 				elevatorSpeed = 0;
 			} else {
 				elevatorSpeed = minElevatorMovingSpeed;
 			}
 			waitToAccelerate++;
 
 		} else {
 			float maxElevatorSpeed = this.getMaximumSpeed();
 			float tempSpeed = elevatorSpeed + elevatorAccel;
 			if (tempSpeed > maxElevatorSpeed) {
 				tempSpeed = maxElevatorSpeed;
 			}
 			// Calculate elevator range to break
 
 			if (!slowingDown
 				&& MathHelper.abs((float) (destY - posY)) >= (tempSpeed
 																* tempSpeed - minElevatorMovingSpeed
 																				* minElevatorMovingSpeed)
 																/ (2 * elevatorAccel)) {
 				// if current destination is further away than this range and <
 				// max speed, continue to accelerate
 				elevatorSpeed = tempSpeed;
 			}
 			// else start to slow down
 			else {
 				elevatorSpeed -= elevatorAccel;
 				slowingDown = true;
 			}
 			if (elevatorSpeed > maxElevatorSpeed) {
 				elevatorSpeed = maxElevatorSpeed;
 			}
 			if (elevatorSpeed < minElevatorMovingSpeed) {
 				elevatorSpeed = minElevatorMovingSpeed;
 			}
 		}
 		// check whether at the destination or not
 		boolean atDestination = onGround
 								|| (MathHelper.abs((float) (destY - posY)) < elevatorSpeed);
 		if (destY < 1 || destY > this.worldObj.getHeight()) {
 			atDestination = true;
 		}
 
 		// if not there yet, update speed and location
 		if (!atDestination) {
 			motionY = (destY > posY) ? elevatorSpeed : -elevatorSpeed;
 		} else if (atDestination) {
 			killAllConjoined();
 			return;
 		}
 		this.moveEntity(this.motionX,
 						this.motionY,
 						this.motionX);
 
 		updateRiderPosition();
 
 		if (!emerHalt) {
 			if (MathHelper.abs((float) motionY) < minElevatorMovingSpeed) {
 				if (stillCount++ > 10) {
 					// should notify computer that this block is invalid
 					// that way the computer doesn't think it still has this
 					// block when it goes into maintenance
 					killAllConjoined();
 				}
 			} else {
 				stillCount = 0;
 			}
 		}
 
 	}
 
 	@Override
 	protected boolean canTriggerWalking() {
 		return false;
 	}
 
 	@Override
 	public AxisAlignedBB getBoundingBox() {
 		return AxisAlignedBB.getBoundingBox(posX - 0.5,
 											posY - 0.5,
 											posZ - 0.5,
 											posX + 0.5,
 											posY + 0.5,
 											posZ + 0.5);
 	}
 
 	@Override
 	public boolean canBeCollidedWith() {
 		return true;
 	}
 
 	@Override
 	public boolean canBePushed() {
 		return false;
 	}
 
 	@Override
 	protected void writeEntityToNBT(NBTTagCompound nbttagcompound) {
 		nbttagcompound.setInteger(	"destY",
 									this.getDestinationY());
 		if (this.destFloorName != null && !this.destFloorName.trim().isEmpty()) {
 			nbttagcompound.setString(	"destName",
 										this.destFloorName);
 		}
 		nbttagcompound.setBoolean(	"emerHalt",
 									emerHalt);
 		nbttagcompound.setBoolean(	"isCenter",
 									isNotifierElevator);
 		nbttagcompound.setInteger(	"ComputerX",
 									this.computerPos.posX);
 		nbttagcompound.setInteger(	"ComputerY",
 									this.computerPos.posY);
 		nbttagcompound.setInteger(	"ComputerZ",
 									this.computerPos.posZ);
 		nbttagcompound.setFloat("TopSpeed",
 								this.getMaximumSpeed());
 
 		if (this.getCamoItem() != null) {
 			NBTTagCompound itemNBTTagCompound = new NBTTagCompound();
 			this.getCamoItem().writeToNBT(itemNBTTagCompound);
 
 			nbttagcompound.setTag(	"CamoItem",
 									itemNBTTagCompound);
 		}
 
 	}
 
 	@Override
 	protected void readEntityFromNBT(NBTTagCompound nbttagcompound) {
 		this.setDestinationY(nbttagcompound.getInteger("destY"));
 		this.setMaximumSpeed(nbttagcompound.getFloat("TopSpeed"));
 		this.emerHalt = nbttagcompound.getBoolean("emerHalt");
 		this.destFloorName = nbttagcompound.getString("destName");
 		this.isNotifierElevator = nbttagcompound.getBoolean("isCenter");
 		this.computerPos = new ChunkCoordinates(nbttagcompound.getInteger("ComputerX"), nbttagcompound.getInteger("ComputerY"), nbttagcompound.getInteger("ComputerZ"));
 		if (ItemStack.loadItemStackFromNBT(nbttagcompound.getCompoundTag("CamoItem")) != null) {
 			this.setCamoItem(ItemStack.loadItemStackFromNBT(nbttagcompound.getCompoundTag("CamoItem")));
 		}
 	}
 
 	@Override
 	public AxisAlignedBB getCollisionBox(Entity entity) {
 		return entity.getBoundingBox();
 	}
 
 	protected void unmountRiders() {
 		if (!confirmedRiders.isEmpty()) {
 			for (Integer entityID : confirmedRiders) {
 				Entity rider = this.worldObj.getEntityByID(entityID);
 				if (rider != null) {
					rider.moveEntity(	0,
										(this.getDestinationY() + 1.0D + this.getMountedYOffset())
												- rider.boundingBox.minY,
										0);
 					rider.isAirBorne = false;
 					rider.onGround = true;
 					rider.fallDistance = 0;
 				}
 			}
 			confirmedRiders.clear();
 		}
 	}
 
 	protected void updatePotentialRiders() {
 		// this.confirmedRiders.clear();
 		Set<Entity> potentialRiders = new HashSet<Entity>();
 		AxisAlignedBB boundBox = this.getBoundingBox().offset(	0,
 																1,
 																0).expand(	0,
 																			1.0,
 																			0);
 		potentialRiders.addAll(worldObj.getEntitiesWithinAABBExcludingEntity(	this,
 																				boundBox));
 		for (Entity rider : potentialRiders) {
 			if (!(rider instanceof EntityElevator)
 				&& !this.confirmedRiders.contains(rider.entityId)) {
 				double yPos = (this.posY + this.getMountedYOffset())
 								- rider.boundingBox.minY;
 				rider.motionY = this.motionY < 0 ? this.motionY : Math.max(	yPos,
 																			rider.motionY);
 				rider.isAirBorne = true;
 				rider.onGround = true;
 				rider.fallDistance = 0;
 				this.confirmedRiders.add(rider.entityId);
 			}
 		}
 	}
 
 	protected boolean isRiding(Entity rider) {
 		return rider != null
 				&& rider.boundingBox.maxX >= this.getBoundingBox().minX
 				&& rider.boundingBox.minX <= this.getBoundingBox().maxX
 				&& rider.boundingBox.maxZ >= this.getBoundingBox().minZ
 				&& rider.boundingBox.minZ <= this.getBoundingBox().maxZ
 				&& rider.boundingBox.minY <= (this.posY
 												+ this.getMountedYOffset() + 2.0);
 	}
 
 	protected void updateConfirmedRiders() {
 		if (!confirmedRiders.isEmpty()) {
 			Set<Integer> removedRiders = new HashSet<Integer>();
 			for (Integer entityID : confirmedRiders) {
 				Entity rider = this.worldObj.getEntityByID(entityID);
 
 				if (isRiding(rider)) {
 					double yPos = (this.posY + this.getMountedYOffset())
 									- rider.boundingBox.minY;
 					double yDif = Math.abs(this.posY + this.getMountedYOffset()
 											- rider.boundingBox.minY);
 					if (yDif < 1.0) {
 						rider.motionY = this.motionY < 0 ? this.motionY : Math.max(	yPos,
 																					rider.motionY);
 					} else {
 						rider.moveEntity(	0,
 											yPos,
 											0);
 						rider.motionY = this.motionY;
 					}
 					rider.isAirBorne = true;
 					rider.onGround = true;
 					rider.fallDistance = 0;
 				} else {
 					removedRiders.add(entityID);
 				}
 			}
 
 			if (!removedRiders.isEmpty()) {
 				this.confirmedRiders.removeAll(removedRiders);
 			}
 		}
 	}
 
 	public void updateRiders(boolean atDestination) {
 		if (this.isDead) {
 			return;
 		}
 		updatePotentialRiders();
 		if (atDestination) {
 			this.unmountRiders();
 			return;
 		}
 		updateConfirmedRiders();
 	}
 
 	// this should be called by each elevator entity and not just the controller
 	@Override
 	public void updateRiderPosition() {
 		this.updateRiders(false);
 	}
 
 	@Override
 	public double getMountedYOffset() {
 		return 0.50D;
 	}
 
 	public void setProperties(int destination, String destinationName, float elevatorTopSpeed, ChunkCoordinates computer, boolean haltable, int notifierID, boolean mobilePower) {
 		this.setDestinationY(destination);
 		destFloorName = destinationName != null && destinationName.trim() != "" ? destinationName : String.valueOf(destination);
 
 		this.computerPos = computer;
 		this.canBeHalted = haltable;
 		this.enableMobilePower = mobilePower;
 
 		isNotifierElevator = (notifierID == this.entityId);
 
 		this.setMaximumSpeed(elevatorTopSpeed);
 
 		waitToAccelerate = 0;
 
 		if (!isNotifierElevator) {
 			this.notifierElevatorID = notifierID;
 			this.getControler().conjoinedelevators.add(this.entityId);
 		}
 	}
 
 	private void removeElevatorBlock(int x, int y, int z) {
 		if (worldObj.getBlockId(x,
 								y,
 								z) == ConfigurationLib.blockTransportBaseID
 			&& worldObj.getBlockMetadata(	x,
 											y,
 											z) == BlockLib.BLOCK_ELEVATOR_ID) {
 			TileEntityElevator tile = (TileEntityElevator) BlockHelper.getTileEntity(	this.worldObj,
 																						x,
 																						y,
 																						z,
 																						TileEntityElevator.class);
 			if (tile != null) {
 				if (tile.getCamoItem() != null) {
 					this.setCamoItem(tile.removeCamoItemWithoutDrop());
 				}
 
 			}
 
 			if (this.enableMobilePower) {
 				worldObj.setBlock(	x,
 									y,
 									z,
 									ConfigurationLib.blockTransportBaseID,
 									1,
 									BlockLib.BLOCK_TRANSIT_ID);
 			} else {
 				worldObj.setBlockToAir(	x,
 										y,
 										z);
 			}
 
 		}
 
 	}
 
 	private void setTransitBlocks(int x, int y, int z) {
 
 		if (this.motionY > 0) {
 			x = (int) Math.ceil(posX - 0.5);
 			y = (int) Math.ceil(posY - 0.5);
 			z = (int) Math.ceil(posZ - 0.5);
 		} else {
 			x = (int) Math.floor(posX - 0.5);
 			y = (int) Math.floor(posY - 0.5);
 			z = (int) Math.floor(posZ - 0.5);
 		}
 
 		if (worldObj.isAirBlock(x,
 								y,
 								z)) {
 			worldObj.setBlock(	x,
 								y,
 								z,
 								ConfigurationLib.blockTransportBaseID,
 								1,
 								BlockLib.BLOCK_TRANSIT_ID);
 		}
 
 	}
 
 	// only function that absolutely needs to keep track of elevators
 	public void setEmerHalt(boolean newhalt) {
 		if (!this.canBeHalted && newhalt) {
 			return;
 		}
 		emerHalt = newhalt;
 
 		if (emerHalt) {
 			motionY = 0;
 		}
 
 		if (this.getIsControlerElevator()) {
 
 			Iterator<Integer> iter = conjoinedelevators.iterator();
 			while (iter.hasNext()) {
 				EntityElevator curElevator = (EntityElevator) this.worldObj.getEntityByID(iter.next());
 				if (curElevator != this && curElevator.emerHalt != emerHalt) {
 					curElevator.setEmerHalt(emerHalt);
 				}
 			}
 		} else if (getControler() != null
 					&& getControler().emerHalt != emerHalt) {
 			getControler().setEmerHalt(emerHalt);
 		}
 	}
 
 	private void killAllConjoined() {
 		Iterator<Integer> iter = this.conjoinedelevators.iterator();
 		while (iter.hasNext()) {
 			EntityElevator curElevator = (EntityElevator) this.worldObj.getEntityByID(iter.next());
 			if (curElevator != null) curElevator.setDead();
 		}
 		this.setDead();
 		if (isNotifierElevator
 			&& MathHelper.floor_double(posY) == this.getDestinationY()) {
 			TileEntityElevatorComputer comTile = this.getParentElevatorComputer();
 			if (comTile != null) {
 
 				comTile.elevatorArrived(MathHelper.floor_double(this.posY),
 										isNotifierElevator);
 			}
 
 		}
 	}
 
 	// Used to get isControler on both client and server
 	private boolean getIsControlerElevator() {
 		return this.isNotifierElevator
 				|| (this.notifierElevatorID == this.entityId);
 	}
 
 	private EntityElevator getControler() {
 		return ((EntityElevator) this.worldObj.getEntityByID(this.notifierElevatorID));
 	}
 
 	// used to get access to the elevators computer
 	public TileEntityElevatorComputer getParentElevatorComputer() {
 		TileEntity tile = this.computerPos == null ? null : this.worldObj.getBlockTileEntity(	this.computerPos.posX,
 																								this.computerPos.posY,
 																								this.computerPos.posZ);
 		if (tile == null) {
 		} else if (!(tile instanceof TileEntityElevatorComputer)) {
 			tile = null;
 		}
 
 		return (TileEntityElevatorComputer) tile;
 	}
 
 }
