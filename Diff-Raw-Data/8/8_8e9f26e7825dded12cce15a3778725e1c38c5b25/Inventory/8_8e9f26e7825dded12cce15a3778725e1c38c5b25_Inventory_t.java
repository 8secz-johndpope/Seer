 /**
  * This file is part of aion-unique <aion-unique.com>.
  *
  *  aion-unique is free software: you can redistribute it and/or modify
  *  it under the terms of the GNU General Public License as published by
  *  the Free Software Foundation, either version 3 of the License, or
  *  (at your option) any later version.
  *
  *  aion-unique is distributed in the hope that it will be useful,
  *  but WITHOUT ANY WARRANTY; without even the implied warranty of
  *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  *  GNU General Public License for more details.
  *
  *  You should have received a copy of the GNU General Public License
  *  along with aion-unique.  If not, see <http://www.gnu.org/licenses/>.
  */
 
 package com.aionemu.gameserver.model.gameobjects.player;
 
 import java.util.ArrayList;
 import java.util.Collections;
 import java.util.List;
 import java.util.SortedMap;
 import java.util.TreeMap;
 
 import org.apache.log4j.Logger;
 
 import com.aionemu.commons.database.dao.DAOManager;
 import com.aionemu.gameserver.dao.InventoryDAO;
 import com.aionemu.gameserver.model.ItemSlot;
 import com.aionemu.gameserver.model.gameobjects.Item;
 import com.aionemu.gameserver.model.gameobjects.PersistentState;
 import com.aionemu.gameserver.model.gameobjects.stats.listeners.ItemEquipmentListener;
 import com.aionemu.gameserver.model.items.ItemId;
 import com.aionemu.gameserver.model.items.ItemStorage;
 import com.aionemu.gameserver.model.templates.item.ArmorType;
 import com.aionemu.gameserver.model.templates.item.WeaponType;
 import com.aionemu.gameserver.network.aion.serverpackets.SM_DELETE_ITEM;
 import com.aionemu.gameserver.network.aion.serverpackets.SM_UPDATE_ITEM;
 import com.aionemu.gameserver.utils.PacketSendUtility;
 
 /**
  * @author Avol
  * modified by ATracer
  */
 public class Inventory
 {
 
 	private static final Logger log = Logger.getLogger(Inventory.class);
 
 	private Player owner;
 
 	private ItemStorage defaultItemBag;
 
 	private SortedMap<Integer, Item> equipment = Collections.synchronizedSortedMap(new TreeMap<Integer, Item>());
 
 	private Item kinahItem;
 
 	/**
 	 *  Will be enhanced during development.
 	 */
 	public Inventory()
 	{
 		defaultItemBag = new ItemStorage(108);
 	}
 
 	/**
 	 * @param owner
 	 */
 	public Inventory(Player owner)
 	{
 		this();
 		this.owner = owner;
 	}	
 
 	/**
 	 * @return the owner
 	 */
 	public Player getOwner()
 	{
 		return owner;
 	}
 
 	/**
 	 * @param owner the owner to set
 	 */
 	public void setOwner(Player owner)
 	{
 		this.owner = owner;
 	}
 
 	/**
 	 * @return the kinahItem
 	 */
 	public Item getKinahItem()
 	{
 		return kinahItem;
 	}
 	/**
 	 *  Increasing kinah amount is persisted immediately
 	 *  
 	 * @param amount
 	 */
 	public void increaseKinah(int amount)
 	{
 		kinahItem.increaseItemCount(amount);
 	}
 	/**
 	 *  Decreasing kinah amount is persisted immediately
 	 *  
 	 * @param amount
 	 */
 	public boolean decreaseKinah(int amount)
 	{
 		boolean operationResult = kinahItem.decreaseItemCount(amount);
 		if(operationResult)
 		{
 			DAOManager.getDAO(InventoryDAO.class).store(kinahItem, getOwner().getObjectId());
 		}
 		return operationResult;
 	}
 
 	/**
 	 * 
 	 *  This method should be called only for new items added to inventory (loading from DB)
 	 *  If item is equiped - will be put to equipment
 	 *  if item is unequiped - will be put to default bag for now
 	 *  Kinah is stored separately as it will be used frequently
 	 *  
 	 *  @param item
 	 */
 	public void onLoadHandler(Item item)
 	{
 		//TODO checks
 		if(item.isEquipped())
 		{
 			equipment.put(item.getEquipmentSlot(), item);
 			if (owner.getGameStats() != null)
 			{
 				ItemEquipmentListener.onItemEquipment(item, owner.getGameStats());
 			}
 			if(owner.getLifeStats() != null)//TODO why onLoadHandler called 2 times on load ?
 			{
 				owner.getLifeStats().synchronizeWithMaxStats();
 			}			
 		}
 		else if(item.getItemTemplate().getItemId() == ItemId.KINAH.value())
 		{
 			kinahItem = item;
 		}
 		else
 		{
 			defaultItemBag.putToDefinedOrNextAvaiableSlot(item);
 		}	
 	}
 
 	/**
 	 * 	Return is the result of add operation. It can be null if item was not stored, it can be existing 
 	 *  item reference if stack count was increased or new item in bag
 	 *  
 	 *  
 	 *  Every add operation is persisted immediately now
 	 *  
 	 * @param item
 	 */
 	public Item addToBag(Item item)
 	{
 		Item resultItem = defaultItemBag.addItemToStorage(item);
 		if(resultItem != null)
 		{
 			DAOManager.getDAO(InventoryDAO.class).store(resultItem, getOwner().getObjectId());
 		}
 		return resultItem;
 	}
 
 	/**
 	 *  Used to put item into storage cube at first avaialble slot (no check for existing item)
 	 * 
 	 *  Every put operation is persisted immediately now
 	 *  
 	 * @param item
 	 * @return
 	 */
 	public Item putToBag(Item item)
 	{
 
 		Item resultItem = defaultItemBag.putToNextAvailableSlot(item);
 		if(resultItem != null)
 		{
 			DAOManager.getDAO(InventoryDAO.class).store(resultItem, getOwner().getObjectId());
 		}
 		return resultItem;
 	}
 
 	/**
 	 *  Removes item completely from inventory.
 	 *  Every remove operation is persisted immediately now
 	 *  
 	 * @param item
 	 */
 	public void removeFromBag(Item item)
 	{
 		boolean operationResult = defaultItemBag.removeItemFromStorage(item);
 		if(operationResult)
 		{
 			item.setPersistentState(PersistentState.DELETED);
 			DAOManager.getDAO(InventoryDAO.class).store(item, getOwner().getObjectId());
 		}
 	}
 
 	/**
 	 *  Used to reduce item count in bag or completely remove by ITEMID
 	 *  This method operates in iterative manner overl all items with specified ITEMID.
 	 *  Return value can be the following:
 	 *  - true - item removal was successfull
 	 *  - false - not enough amount of items to reduce 
 	 *  or item is not present
 	 *  
 	 * @param itemId
 	 * @param count
 	 * @return
 	 */
 	public boolean removeFromBagByItemId(int itemId, int count)
 	{
 		if(count < 1)
 			return false;
 
 		List<Item> items = defaultItemBag.getItemsFromStorageByItemId(itemId);
 
 		for(Item item : items)
 		{
 			count = decreaseItemCount(item, count);
 
 			if(count == 0)
 				break;
 		}
 
 		return count >= 0;
 	}
 
 	/**
 	 *  Used to reduce item count in bag or completely remove by OBJECTID
 	 *  Return value can be the following:
 	 *  - true - item removal was successfull
 	 *  - false - not enough amount of items to reduce 
 	 *  or item is not present
 	 *  
 	 * @param itemObjId
 	 * @param count
 	 * @return
 	 */
 	public boolean removeFromBagByObjectId(int itemObjId, int count)
 	{
 		if(count < 1)
 			return false;
 
 		Item item = defaultItemBag.getItemFromStorageByItemObjId(itemObjId);
 		return decreaseItemCount(item, count) >= 0;
 	}
 
 	/**
 	 *   This method decreases inventory's item by count and sends
 	 *   appropriate packets to owner.
 	 *   Item will be saved in database after update or deleted if count=0
 	 * 
 	 * @param count should be > 0
 	 * @param item
 	 * @return
 	 */
 	private int decreaseItemCount(Item item, int count)
 	{
 		if(item.getItemCount() >= count)
 		{
 			item.decreaseItemCount(count);
 			count = 0;
 		}
 		else
 		{
 			item.decreaseItemCount(item.getItemCount());
 			count -= item.getItemCount();
 		}
 		if(item.getItemCount() == 0)
 		{
 			PacketSendUtility.sendPacket(getOwner(), new SM_DELETE_ITEM(item.getObjectId()));
 		}
 		PacketSendUtility.sendPacket(getOwner(), new SM_UPDATE_ITEM(item));
 		DAOManager.getDAO(InventoryDAO.class).store(item, getOwner().getObjectId());
 		return count;
 	}
 
 	/**
 	 *  Method primarily used when saving to DB
 	 *  //TODO getAllItems(compartment)
 	 * @return
 	 */
 	public List<Item> getAllItems()
 	{
 		List<Item> allItems = new ArrayList<Item>();
 		allItems.add(kinahItem);
 		allItems.addAll(defaultItemBag.getStorageItems());
 		allItems.addAll(equipment.values());
 		return allItems;
 	}
 
 	/**
 	 * 	Only equipped items
 	 * 
 	 * @return
 	 */
 	public List<Item> getEquippedItems()
 	{
 		List<Item> equippedItems = new ArrayList<Item>();
 
 		synchronized(equipment)
 		{	
 			for(Item item : equipment.values())
 			{
 				if(item != null)
 				{
 					equippedItems.add(item);
 				}
 			}
 		}
 
 		return equippedItems;
 	}
 
 	/**
 	 *	Items from all boxes + kinah item
 	 * 
 	 * @return
 	 */
 	public List<Item> getUnquippedItems()
 	{
 		List<Item> unequipedItems = new ArrayList<Item>();
 		unequipedItems.add(kinahItem);
 		unequipedItems.addAll(defaultItemBag.getStorageItems());
 		return unequipedItems;
 	}
 	/**
 	 *	Called when CM_EQUIP_ITEM packet arrives with action 0
 	 * 
 	 * @param itemUniqueId
 	 * @param slot
 	 * @return
 	 */
 	public boolean equipItem(int itemUniqueId, int slot)
 	{
 		synchronized(this)
 		{
 			Item item = defaultItemBag.getItemFromStorageByItemObjId(itemUniqueId);
 
 			if(item == null)
 			{
 				return false;
 			}
 			//don't allow to wear items of higher level
 			if(item.getItemTemplate().getLevel() > owner.getCommonData().getLevel())
 			{
 				return false;
 			}
 
 			int itemSlotToEquip = 0;
 
 			Item itemInMainHand = equipment.get(ItemSlot.MAIN_HAND.getSlotIdMask());
 			Item itemInSubHand = equipment.get(ItemSlot.SUB_HAND.getSlotIdMask());
 
 			switch(item.getItemTemplate().getEquipmentType())
 			{
 				case ARMOR:
 					if(!validateEquippedArmor(item, itemInMainHand))
 						return false;
 					break;				
 				case WEAPON:
 					if(!validateEquippedWeapon(item, itemInMainHand, itemInSubHand))
 						return false;
 					break;
 			}
 
 			//remove item first from inventory to have at least one slot free
 			defaultItemBag.removeItemFromStorage(item);
 			//check whether there is already item in specified slot
 			int itemSlotMask = item.getItemTemplate().getItemSlot();
 
 			List<ItemSlot> possibleSlots = ItemSlot.getSlotsFor(itemSlotMask);
 			for(ItemSlot possibleSlot : possibleSlots)
 			{
 				if(equipment.get(possibleSlot.getSlotIdMask()) == null)
 				{
 					itemSlotToEquip = possibleSlot.getSlotIdMask();
 					break;
 				}	
 			}
 			// equip first occupied slot if there is no free
 			if(itemSlotToEquip == 0)
 			{
 				itemSlotToEquip = possibleSlots.get(0).getSlotIdMask();
 			}
 
 			Item equippedItem = equipment.get(itemSlotToEquip);
 			if(equippedItem != null)
 			{
 				unEquip(equippedItem);
 			}
 
 			if(itemSlotToEquip == 0)
 				return false;
 
 			item.setEquipped(true);
 
 			equip(itemSlotToEquip, item);
 		}
 		return true;
 	}
 	
 	/**
 	 *  Used during equip process and analyzes equipped slots
 	 *  
 	 * @param item
 	 * @param itemInMainHand
 	 * @param itemInSubHand
 	 * @return
 	 */
 	private boolean validateEquippedWeapon(Item item, Item itemInMainHand, Item itemInSubHand)
 	{
 		// check present skill
 		int[] requiredSkills = item.getItemTemplate().getWeaponType().getRequiredSkills();
 		
 		if(!checkAvaialbeEquipSkills(requiredSkills))
 			return false;
 
 		switch(item.getItemTemplate().getWeaponType().getRequiredSlots())
 		{
 			case 2:
 				switch(item.getItemTemplate().getWeaponType())
 				{
 					//if bow and arrows are equipped + new item is bow - dont uneqiup arrows								
 					case BOW:
 						if(itemInSubHand != null &&
 							itemInSubHand.getItemTemplate().getArmorType() != ArmorType.ARROW)
 						{
 							//TODO more wise check here is needed
 							if(getNumberOfFreeSlots() < 1)
 								return false;
 							unEquip(itemInSubHand);
 						}
 						break;
 					//if new item is not bow - unequip arrows
 					default:
 						if(itemInSubHand != null)
 						{
 							//TODO more wise check here is needed
 							if(getNumberOfFreeSlots() < 1)
 								return false;
 							//remove 2H weapon
 							unEquip(itemInSubHand);
 						}	
 				}//no break						
 			case 1:
 				//check dual skill
 				if(itemInMainHand != null && !getOwner().getSkillList().isSkillPresent(19))
 				{
 					unEquip(itemInMainHand);
 				}
 				//unequipe arrows if bow+arrows were equipeed
 				Item possibleArrows = equipment.get(ItemSlot.SUB_HAND.getSlotIdMask());
 				if(possibleArrows != null && possibleArrows.getItemTemplate().getArmorType() == ArmorType.ARROW)
 				{
 					//TODO more wise check here is needed
 					if(getNumberOfFreeSlots() < 1)
 						return false;
 					unEquip(possibleArrows);
 				}
 			break;
 		}
 		return true;
 	}
 	
 	/**
 	 * 
 	 * @param requiredSkills
 	 * @return
 	 */
 	private boolean checkAvaialbeEquipSkills(int[] requiredSkills)
 	{
 		boolean isSkillPresent = false;		
 		for(int skill : requiredSkills)
 		{
 			if(getOwner().getSkillList().isSkillPresent(skill))
 			{
 				isSkillPresent = true;
 				break;
 			}
 		}	
 		return isSkillPresent;
 	}
 
 	/**
 	 *  Used during equip process and analyzes equipped slots
 	 *  
 	 * @param item
 	 * @param itemInMainHand
 	 * @return
 	 */
 	private boolean validateEquippedArmor(Item item, Item itemInMainHand)
 	{
		//allow wearing of jewelry etc stuff
		ArmorType armorType = item.getItemTemplate().getArmorType();
		if(armorType == null)
			return true;
		
 		// check present skill
		int[] requiredSkills = armorType.getRequiredSkills();
 		if(!checkAvaialbeEquipSkills(requiredSkills))
 			return false;
 
 		switch(item.getItemTemplate().getArmorType())
 		{
 			case ARROW:
 				if(itemInMainHand == null
 					|| itemInMainHand.getItemTemplate().getWeaponType() != WeaponType.BOW)
 					return false;
 				break;
 			case SHIELD:
 				if(itemInMainHand != null 
 					&& itemInMainHand.getItemTemplate().getWeaponType().getRequiredSlots() == 2)
 				{		
 					//TODO more wise check here is needed
 					if(isFull())
 						return false;
 					//remove 2H weapon
 					unEquip(itemInMainHand);
 				}	
 				break;
 		}
 		return true;
 	}
 
 	private void equip(int slot, Item item)
 	{
 		equipment.put(slot, item);
 		item.setEquipmentSlot(slot);
 		if (owner.getGameStats()!=null)
 		{
 			ItemEquipmentListener.onItemEquipment(item, owner.getGameStats());
 		}
 		owner.getLifeStats().updateCurrentStats();
 		PacketSendUtility.sendPacket(getOwner(), new SM_UPDATE_ITEM(item));
 	}
 
 	/**
 	 *	Called when CM_EQUIP_ITEM packet arrives with action 1
 	 * 
 	 * @param itemUniqueId
 	 * @param slot
 	 * @return
 	 */
 	public boolean unEquipItem(int itemUniqueId, int slot)
 	{
 		//if inventory is full unequip action is disabled
 		if(isFull())
 		{
 			return false;
 		}
 		synchronized(this)
 		{
 			Item itemToUnequip = null;
 
 			for(Item item : equipment.values())
 			{
 				if(item.getObjectId() == itemUniqueId)
 				{
 					itemToUnequip = item;
 				}
 			}
 
 			if(itemToUnequip == null || !itemToUnequip.isEquipped())
 			{
 				return false;
 			}
 			
 			//if unequip bow - unequip arrows also
 			if(itemToUnequip.getItemTemplate().getWeaponType() == WeaponType.BOW)
 			{
 				Item possibleArrows = equipment.get(ItemSlot.SUB_HAND.getSlotIdMask());
 				if(possibleArrows != null && possibleArrows.getItemTemplate().getArmorType() == ArmorType.ARROW)
 				{
 					//TODO more wise check here is needed
 					if(getNumberOfFreeSlots() < 1)
 						return false;
 					unEquip(possibleArrows);
 				}
 			}
 			unEquip(itemToUnequip);
 		}
 
 		return true;	
 	}
 
 	private void unEquip(Item itemToUnequip)
 	{
 		equipment.remove(itemToUnequip.getEquipmentSlot());
 		itemToUnequip.setEquipped(false);
 		if (owner.getGameStats()!=null)
 		{
 			ItemEquipmentListener.onItemUnequipment(itemToUnequip, owner.getGameStats());
 		}
 		owner.getLifeStats().updateCurrentStats();
 		defaultItemBag.addItemToStorage(itemToUnequip);
 		PacketSendUtility.sendPacket(getOwner(), new SM_UPDATE_ITEM(itemToUnequip));
 	}
 
 	/**
 	 * 
 	 * @return
 	 */
 	public boolean switchHands(int itemUniqueId, int slot)
 	{
 		Item mainHandItem = equipment.get(ItemSlot.MAIN_HAND.getSlotIdMask());
 		Item subHandItem = equipment.get(ItemSlot.SUB_HAND.getSlotIdMask());
 		Item mainOffHandItem = equipment.get(ItemSlot.MAIN_OFF_HAND.getSlotIdMask());
 		Item subOffHandItem = equipment.get(ItemSlot.SUB_OFF_HAND.getSlotIdMask());
 		//TODO switch items
 		return false;
 	}
 
 	/**
 	 *  Will look item in default item bag
 	 *  
 	 * @param value
 	 * @return
 	 */
 	public Item getItemByObjId(int value)
 	{
 		return defaultItemBag.getItemFromStorageByItemObjId(value);
 	}
 
 	/**
 	 *  Will look item in equipment item set
 	 *  
 	 * @param value
 	 * @return
 	 */
 	public Item getEquippedItemByObjId(int value)
 	{
 		for(Item item : equipment.values())
 		{
 			if(item.getObjectId() == value)
 				return item;
 		}
 		return null;
 	}
 
 	/**
 	 *  Will look for item in both equipment and cube
 	 *  
 	 * @param value
 	 * @return
 	 */
 	public Item findItemByObjId(int value)
 	{
 		Item item = getItemByObjId(value);
 		if(item == null)
 			item = getEquippedItemByObjId(value);
 
 		return item;
 	}
 
 	/**
 	 * 
 	 * @param value
 	 * @return
 	 */
 	public List<Item> getItemsByItemId(int value)
 	{
 		return defaultItemBag.getItemsFromStorageByItemId(value);
 	}
 
 	/**
 	 *  
 	 * @param itemId
 	 * @return number of items using search by itemid
 	 */
 	public int getItemCountByItemId(int itemId)
 	{
 		List<Item> items = getItemsByItemId(itemId);
 		int count = 0;
 		for(Item item : items)
 		{
 			count += item.getItemCount();
 		}
 		return count;
 	}
 
 	/**
 	 *  Checks whether default cube is full
 	 *  
 	 * @return
 	 */
 	public boolean isFull()
 	{
 		return defaultItemBag.getNextAvailableSlot() == -1;
 	}
 
 	public int getNumberOfFreeSlots()
 	{
 		return defaultItemBag.getNumberOfFreeSlots();
 	}
 	/**
 	 *  Sets the Inventory Limit from Cube Size
 	 *  
 	 * @param Limit
 	 */
 	public void setLimit(int limit){
 		this.defaultItemBag.setLimit(limit);
 	}
 	public int getLimit(){
 		return this.defaultItemBag.getLimit();
 	}
 }
