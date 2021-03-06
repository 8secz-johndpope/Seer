 /** 
  * Copyright (c) Krapht, 2011
  * 
  * "LogisticsPipes" is distributed under the terms of the Minecraft Mod Public 
  * License 1.0, or MMPL. Please check the contents of the license located in
  * http://www.mod-buildcraft.com/MMPL-1.0.txt
  */
 
 package logisticspipes.logic;
 
 import java.util.HashMap;
 import java.util.Map.Entry;
 
 import logisticspipes.LogisticsPipes;
 import logisticspipes.interfaces.IChassiePowerProvider;
 import logisticspipes.interfaces.IInventoryUtil;
 import logisticspipes.interfaces.routing.IRequestItems;
 import logisticspipes.interfaces.routing.IRequireReliableTransport;
 import logisticspipes.network.GuiIDs;
 import logisticspipes.network.NetworkConstants;
 import logisticspipes.network.packets.PacketPipeInteger;
 import logisticspipes.pipefxhandlers.Particles;
 import logisticspipes.pipes.PipeItemsSupplierLogistics;
 import logisticspipes.pipes.basic.CoreRoutedPipe;
 import logisticspipes.proxy.MainProxy;
 import logisticspipes.proxy.SimpleServiceLocator;
 import logisticspipes.request.RequestManager;
 import logisticspipes.utils.AdjacentTile;
 import logisticspipes.utils.ItemIdentifier;
 import logisticspipes.utils.ItemIdentifierStack;
 import logisticspipes.utils.SimpleInventory;
 import logisticspipes.utils.WorldUtil;
 import net.minecraft.entity.player.EntityPlayer;
 import net.minecraft.inventory.IInventory;
 import net.minecraft.nbt.NBTTagCompound;
 import buildcraft.energy.EngineWood;
 import buildcraft.energy.TileEngine;
 import buildcraft.transport.TileGenericPipe;
 import cpw.mods.fml.common.network.Player;
 
 public class LogicSupplier extends BaseRoutingLogic implements IRequireReliableTransport{
 	
 	private SimpleInventory dummyInventory = new SimpleInventory(9, "Items to keep stocked", 127);
 	
 	private final HashMap<ItemIdentifier, Integer> _requestedItems = new HashMap<ItemIdentifier, Integer>();
 	
 	private boolean _requestPartials = false;
 	private int _lastSucess_count = 1024;
 
 	public boolean pause = false;
 	
 	public IChassiePowerProvider _power;
 	
 	public LogicSupplier() {
 		throttleTime = 100;
 	}
 	
 	@Override
 	public void destroy() {}
 
 	@Override
 	public void onWrenchClicked(EntityPlayer entityplayer) {
 		//pause = true; //Pause until GUI is closed //TODO Find a way to handle this
 		if(MainProxy.isServer(entityplayer.worldObj)) {
 			//GuiProxy.openGuiSupplierPipe(entityplayer.inventory, dummyInventory, this);
 			entityplayer.openGui(LogisticsPipes.instance, GuiIDs.GUI_SupplierPipe_ID, worldObj, xCoord, yCoord, zCoord);
 			MainProxy.sendPacketToPlayer(new PacketPipeInteger(NetworkConstants.SUPPLIER_PIPE_MODE_RESPONSE, xCoord, yCoord, zCoord, isRequestingPartials() ? 1 : 0).getPacket(), (Player)entityplayer);
 		}
 	}
 	
 	/*** GUI ***/
 	public SimpleInventory getDummyInventory() {
 		return dummyInventory;
 	}
 
 	@Override
 	public void throttledUpdateEntity() {
 		
 		if (!((CoreRoutedPipe)this.container.pipe).isEnabled()){
 			return;
 		}
 		
 		if(MainProxy.isClient(this.worldObj)) return;
 		if (pause) return;
 		super.throttledUpdateEntity();
 
 		for(int amount : _requestedItems.values()) {
 			if(amount > 0) {
 				MainProxy.sendSpawnParticlePacket(Particles.VioletParticle, xCoord, yCoord, zCoord, this.worldObj, 2);
 			}
 		}
 
 		WorldUtil worldUtil = new WorldUtil(worldObj, xCoord, yCoord, zCoord);
 		for (AdjacentTile tile :  worldUtil.getAdjacentTileEntities(true)){
 			if (tile.tile instanceof TileGenericPipe) continue;
 			if (!(tile.tile instanceof IInventory)) continue;
 			
 			//Do not attempt to supply redstone engines
 			if (tile.tile instanceof TileEngine && ((TileEngine)tile.tile).engine instanceof EngineWood) continue;
 			
 			IInventory inv = (IInventory) tile.tile;
 			if (inv.getSizeInventory() < 1) continue;
 			IInventoryUtil invUtil = SimpleServiceLocator.inventoryUtilFactory.getInventoryUtil(inv);
 			
 			//How many do I want?
 			HashMap<ItemIdentifier, Integer> needed = new HashMap<ItemIdentifier, Integer>(dummyInventory.getItemsAndCount());
 			
 			//How many do I have?
 			HashMap<ItemIdentifier, Integer> have = invUtil.getItemsAndCount();
 			
 			//Reduce what I have
 			for (Entry<ItemIdentifier, Integer> item : needed.entrySet()){
				Integer haveCount = have.get(item);
 				if (haveCount != null){
 					item.setValue(item.getValue() - haveCount);
 				}
 			}
 			
 			//Reduce what have been requested already
 			for (Entry<ItemIdentifier, Integer> item : needed.entrySet()){
				Integer requestedCount =  _requestedItems.get(item);
 				if (requestedCount!=null){
 					item.setValue(item.getValue() - requestedCount);
 				}
 			}
 			
 			((PipeItemsSupplierLogistics)this.container.pipe).setRequestFailed(false);
 			
 			//List<SearchNode> valid = getRouter().getIRoutersByCost();
 			
 			/*
 			//TODO Double Chests, Simplyfication
 			// Filter out providers attached to this inventory so that we don't get stuck in an
 			// endless supply/provide loop on this inventory.
 
 			WorldUtil invWU = new WorldUtil(tile.tile.worldObj, tile.tile.xCoord, tile.tile.yCoord, tile.tile.zCoord);
 			ArrayList<IProvideItems> invProviders = new ArrayList<IProvideItems>();
 
 			for (AdjacentTile atile : invWU.getAdjacentTileEntities()) {
 				if ((atile.tile instanceof TileGenericPipe)) {
 					Pipe p = ((TileGenericPipe) atile.tile).pipe;
 					if ((p instanceof IProvideItems)) {
 						invProviders.add((IProvideItems) p);
 					}
 				}
 			}
 
 			for (IRouter r : valid) {
 				CoreRoutedPipe cp = r.getPipe();
 				if (((cp instanceof IProvideItems)) && (invProviders.contains((IProvideItems) cp))) {
 					valid.remove(r);
 				}
 			}
 			*/
 
 			//Make request
 			for (Entry<ItemIdentifier, Integer> need : needed.entrySet()){
 				Integer amountRequested = need.getValue();
 				if (amountRequested==null || amountRequested < 1) continue;
 				int neededCount;
 				if(_requestPartials)
 					neededCount = Math.min(amountRequested,this._lastSucess_count);
 				else
 					neededCount=amountRequested;
 				if(!_power.useEnergy(10)) {
 					break;
 				}
 				
 				boolean success = false;
 					
 				do{ 
 					success = RequestManager.request(need.getKey().makeStack(neededCount),  (IRequestItems) container.pipe, null);
 					if (success || neededCount == 1){
 						break;
 					}
 					neededCount = neededCount / 2;
 				} while (_requestPartials && !success);
 				
 				if (success){
 					if(neededCount == amountRequested)
 						_lastSucess_count=1024;
 					else {
 						if(neededCount == _lastSucess_count)
 							_lastSucess_count *= 2;
 						else
 							_lastSucess_count= neededCount;
 					}
					Integer currentRequest = _requestedItems.get(need);
 					if (currentRequest == null){
 						_requestedItems.put(need.getKey(), neededCount);
 					}else
 					{
 						_requestedItems.put(need.getKey(), currentRequest + neededCount);
 					}
 				} else{
 					_lastSucess_count=1;
 					((PipeItemsSupplierLogistics)this.container.pipe).setRequestFailed(true);
 				}
 				
 			}
 		}
 	}
 
 	@Override
 	public void readFromNBT(NBTTagCompound nbttagcompound) {
 		super.readFromNBT(nbttagcompound);	
 		dummyInventory.readFromNBT(nbttagcompound, "");
 		_requestPartials = nbttagcompound.getBoolean("requestpartials");
     }
 
 	@Override
     public void writeToNBT(NBTTagCompound nbttagcompound) {
     	super.writeToNBT(nbttagcompound);
     	dummyInventory.writeToNBT(nbttagcompound, "");
     	nbttagcompound.setBoolean("requestpartials", _requestPartials);
     }
 	
 	
 	@Override
 	public void itemLost(ItemIdentifierStack item) {
 		Integer count = _requestedItems.get(item.getItem());
 		if (count != null){
 			_requestedItems.put(item.getItem(), Math.max(0, count - item.stackSize));
 		}
 	}
 
 	@Override
 	public void itemArrived(ItemIdentifierStack item) {
 		Integer count = _requestedItems.get(item.getItem());
 		if (count != null){
 			_requestedItems.put(item.getItem(), Math.max(0, count - item.stackSize));
 		}
 	}
 	
 	public boolean isRequestingPartials(){
 		return _requestPartials;
 	}
 	
 	public void setRequestingPartials(boolean value){
 		_requestPartials = value;
 	}
 }
