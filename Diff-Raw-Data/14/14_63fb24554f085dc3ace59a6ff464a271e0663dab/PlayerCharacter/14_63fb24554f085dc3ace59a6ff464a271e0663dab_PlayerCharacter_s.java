 package Character;
 
 import java.util.ArrayList;
 import java.util.Iterator;
 import java.util.LinkedList;
 
 import Quest.Quest;
 import Statistics.Statistics;
 import Handler.TimeHandler;
 import Item.ArmorItem;
 import Item.Item;
 import Item.LifeItem;
 import Item.MoneyItem;
 import Item.WeaponItem;
 
 @SuppressWarnings("serial")
 public class PlayerCharacter extends AttributeCharacter
 {
     private int money;
     private int maxInventorySize;
     private LinkedList<Quest> quests;
     private ArrayList<Item> inventory;
     private WeaponItem equippedWeapon;
     private ArmorItem equippedArmor;
     private int maxHealth;
     private int armor;
     private int damage;
     private Statistics statistics;
 
     public PlayerCharacter(int id, int x, int y, int width, int height, String name, int health,
     		boolean isAttackable, int speed, int money, int maxInventorySize, int maxHealth){
     	
     	//Sends 0 as area atm, no use for playercharacter.
     	super(id, x, y, width, height, name, health, isAttackable, speed, 0);
     	this.money = money;
     	this.maxInventorySize = maxInventorySize;    
     	this.maxHealth = maxHealth;
 
     	quests = new LinkedList<Quest>();       
     	inventory = new ArrayList<Item>();      
     	
     	statistics = new Statistics();
     }
     
     
     /**
      * Updates the statistics for the player.
      * Depending on the string it will update different statistics.
      * @param type "Monster" to update monsters, "Quest" to update quests.
      */
     public void updateStatistics(String type){
     	if(type.equals("Monster"))
     	{
     		statistics.incMonstersKilled();
     		System.out.println("Added monster stats");
     	}
     	else if(type.equals("Quest"))
     	{
     		statistics.incQuestsCompleted();
     		System.out.println("Added quest stats");
     	}
     }
     
     
     /**
      * Returns the statistics for the player.
      * @return Statistics
      */
     public Statistics getStatistics(){
 		return statistics;
     }
     
     
     /**
      * Add the supplied quest to the questlog of the player.
      * @param quest
      */
     public void addQuest(Quest quest){
     	quests.addLast(quest);
     }
     
     /**
      * Updates all the quests in the players questlog.
      * @param name Name of the quest part that needs to be updates.
      * @param p The player whose questlog to be updated.
      */
     public void updateQuests(String name, PlayerCharacter p)
     {
     	for(Quest quest : quests)
     	{
     		quest.update(name, p);
     	}
     } 
     
     /**
      * Returns how much money the player has.
      * @return
      */
     public int getMoney(){
         return money;
     }
      
     /**
      * Returns the damage of the player.
      * @return The weapons attack, or 5 if none equipped.
      */
     public int getDamage(){
     	damage = 5;   // If no weapon is equipped
     	if(equippedWeapon!=null){
     		damage = equippedWeapon.getAttackDamage();
     	}
     	return(damage);
     }
     
     /**
      * Returns the armor of the player.
      * @return The armors defence rating, or 0 if none equipped.
      */
     public int getArmorRating(){
     	armor = 0;
     	if(equippedArmor!=null){
     		armor = equippedArmor.getDefenceRating();
     	}
     	return(armor);
     }
     
     /**
      * Sets the player new amount of money
      * @param money
      */
     public void setMoney(int money){
         this.money = money;
     	setChanged();
         notifyObservers("information");  
     } 
     
     /**
      * Returns the inventory of this player
      * @return inventory
      */
     public ArrayList<Item> getInventory(){
     	return inventory;
     }
     
     /**
      * Returns the maximum size of the players inventory.
      * @return Max InventorySize.
      */
     public int getMaxInventorySize(){
         return maxInventorySize;
     }
     
     
     /**
      * Returns the current inventory size, including the equipped items
      * @return InventorySize
      */
     public int getCurrentInventorySize(){
     	int size = inventory.size();
     	if(equippedWeapon != null){
     		size++;
     	}
     	if(equippedArmor != null){
     		size++;
     	}
     	return size;
     }
     
     
     /**
      * Adds the supplied item to the players inventory.
      * @param item
      */
     public void addToInventory(Item item){
     	//kollar om det r moneyItem, i s fall lggs penagr till men moneyitem lggs inte till inventory.
     	if(item instanceof MoneyItem){
     		setMoney((((MoneyItem) item).getMoney() + getMoney()));
     	}
     	else{
 	    	inventory.add(item);
 	    	updateQuests(item.getName(), this);
 	    	setChanged();
 	        notifyObservers(inventory); 
     	}
     }
     
     /**
      * Removes an item from the inventory
      * @param item
      */
     public void removeFromInventory(String name){
     	Iterator<Item> it = inventory.iterator();
      	while(it.hasNext()){
      		Item i = it.next();
      		if(i.getName() == name){
      			it.remove();
      			break;
      		}
      	}
          	
      	setChanged();
         notifyObservers(inventory);
     }
     
   
     
     /**
      * Moves the player and updates its attacking status.
      */
     public void update(){
         move();
         
         // Is character still attacking
         if(isAttacking()){
         	if(TimeHandler.timePassed(getTimeStamp(), 450)){
         		setAttacking(false);
         	}
         }  
     }
     
     /**
      * Uses the players primary attack and sets isAttacking to true for
      * a short while. Plays sound effect.
      */
     public void primaryAttack(){
     	setTimeStamp(TimeHandler.getTime());   	
     	setAttacking(true);   	
     	
     	setChanged();
     	notifyObservers("audio/sounds/sword_swing.mp3");
     }
     
     /**
      * Updates the equipped items
      * @param item
      */
     public void equipItem(Item item){
     	
     	// weaponitem
     	if(item instanceof WeaponItem){
     		
     		// nothing equipped?
     		if(equippedWeapon == null){
     		} else {
     			inventory.add(equippedWeapon);
     		}
     		equippedWeapon = (WeaponItem) item;
     	}
     	
     	// armoritem
     	if(item instanceof ArmorItem){
     		
     		// equippedArmor equipped?
     		if(equippedArmor == null){
     		} else {
     			inventory.add(equippedArmor);
     		}
     		equippedArmor = (ArmorItem) item;
     	}
     	
     	// Remove newly equipped item from inventory
     	Iterator<Item> it = inventory.iterator();
    	while(it.hasNext()){
     		Item i = it.next();
     		if(i.equals(item)){
     			it.remove();
     		}
     	}
         	
     	setChanged();
         notifyObservers(inventory);	
     }
     
     /**
      * Removes the currently equipped weapon
      */
     public void unEquipWeapon(){
     	inventory.add(equippedWeapon);
     	equippedWeapon = null;
     	setChanged();
         notifyObservers(inventory);	
     }
 
     /**
      * Removes the currently equipped armor
      */
     public void unEquipArmor(){
     	inventory.add(equippedArmor);
     	equippedArmor = null;
     	setChanged();
         notifyObservers(inventory);	
     }
     
     /**
      * Returns the equipped weapon, null if you have nothing equipped
      * @return equippedWeapon
      */
     public WeaponItem getWeapon(){
     	return equippedWeapon;
     }
     
     /**
      * Based on the players equipped weapon this method returns how much damage the player does
      * @return damage
      */
     public int getPlayerDamage(){
     	if(equippedWeapon != null)
     		return equippedWeapon.getAttackDamage();
     	else
     		return 5;
     }
     
     /**
      * Returns the equipped armor, null if you have nothing equipped
      * @return equippedArmor
      */
     public ArmorItem getArmor(){
     	return equippedArmor;
     }
     
     /**
      * Based on the players equipped armor this method returns how much armor this player has
      * @return armor
      */
     public int getPlayerARmor(){
   		if(equippedArmor != null){
   			return equippedArmor.getDefenceRating(); 			
   		}else{
   			return 5;
   		}
   	}   
 	
 	/**
 	 * Returns the maximum life the player can have
 	 * @return maxHealth
 	 */
 	public int getMaxHealth(){
 		return maxHealth;
 	}
 	
 	/**
 	 * Sets the players maximum health
 	 * @param maxHealth
 	 */
 	public void setMaxHealth(int maxHealth){
 		this.maxHealth = maxHealth;
 	}
 	
 	/**
 	 * Picks up an item from the ground
 	 * @param item
 	 */
     public void pickUpItem(Item item){
         // Do something funny :D
        }
 	
 	/**
 	 * Consumes an item
 	 * @param item
 	 */
 	public void useItem(Item item)
 	{
 		// LifeItem
 		if(item instanceof LifeItem){
 			if(((LifeItem) item).getLifeValue() + getHealth() >= getMaxHealth()){
 				setHealth(getMaxHealth());
 			}
 			else{
 				setHealth(((LifeItem) item).getLifeValue() + getHealth());
 			}
 		}
 
 		// Remove newly used item from inventory
 		Iterator<Item> it = inventory.iterator();
 		while(it.hasNext()){
 			Item i = it.next();
 			if(i.equals(item)){
 				it.remove();
 			}
 		}
 
 		setChanged();
 		notifyObservers(inventory);	
 	}
 	
 	@Override
 	public void interact(PlayerCharacter player) {
 		// TODO Auto-generated method stub	
 	} 
 }
