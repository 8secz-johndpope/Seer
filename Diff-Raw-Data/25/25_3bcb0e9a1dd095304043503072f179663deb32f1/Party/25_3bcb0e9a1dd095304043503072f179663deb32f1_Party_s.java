 package model;
 
 import java.util.ArrayList;
 import java.util.List;
 
 import model.datasource.HUDDataSource;
 import model.item.Animal;
 import model.item.Vehicle;
 import model.worldMap.LocationNode;
 import model.worldMap.TrailEdge;
 
 import core.Logger;
 import core.Logger.Level;
 
 /**
  * Party class that contains an array of persons that are members.
  */
 public class Party implements HUDDataSource {
 	
 	private List<Person> members = new ArrayList<Person>();
 	
 	private int money;
 	
 	private Pace currentPace;
 	
 	private Rations currentRations;
 	
 	private Vehicle vehicle;
 	
 	private TrailEdge trail;
 	
 	private LocationNode location;
 
 	private List<Animal> animals =  new ArrayList<Animal>();
 	
 	/**
 	 * 
 	 * The possible values of the party's pace
 	 */
 	public enum Pace{
 		STEADY ("Steady", 30),
 		STRENUOUS ("Strenuous", 55),
 		GRUELING ("Grueling", 80);
 		
 		private final String name;
 
 		private final int speed;
 		
 		/**
 		 * Constructs a new Pace.
 		 * @param name The name of the pace
 		 * @param speed The speed the pace makes you travel
 		 */
 		private Pace(String name, int speed) {
 			this.name = name;
 			this.speed = speed;
 		}
 		
 		@Override
 		public String toString() {
 			return this.name;
 		}
 		
 		/**
 		 * Returns the current pace/speed.
 		 * @return the current pace/speed
 		 */
 		public int getSpeed() {
 			return speed;
 		}
 	}
 
 	/**
 	 * The possible settings of the party's consumption rate
 	 */	
 	public enum Rations {
 		FILLING ("Filling", 100),
 		MEAGER ("Meager", 75),
 		BAREBONES ("Barebones", 50);
 		
 		private final String name;
 
 		private final int rationAmount;
 		
 		/**
 		 * Construct a new Ration
 		 * @param name The name of the rations
 		 * @param rationAmount The goal stable health for the party members
 		 */
 		private Rations(String name, int rationAmount) {
 			this.name = name;
 			this.rationAmount = rationAmount;
 		}
 		
 		@Override
 		public String toString() {
 			return this.name;
 		}
 		
 		/**
 		 * Returns the breakpoint.
 		 * @return The breakpoint value
 		 */
 		public int getRationAmount() {
 			return rationAmount;
 		}
 	}
 
 	/**
 	 * If party is created before members, this constructor is used.
 	 */
 	public Party() {
 		this.members = null;
 	}
 	
 	/**
 	 * If people are present before party is created, this constructor is used
 	 * @param currentPace The current pace of the party
 	 * @param currentRations The current rations of the party
 	 * @param party Array of people to be initialized into party
 	 */
 	public Party(Pace currentPace, Rations currentRations, List<Person> party) {
 		for (Person person : party) {
 			if (person != null) {
 				members.add(person);
 			}
 		}
 		
 		this.money = 0;
 		this.currentPace = currentPace;
 		this.currentRations = currentRations;
 		this.location = null;
 		
 		final StringBuffer partyCreationLog = new StringBuffer(members.size() + 
 				" members were created successfully: ");
 		for (Person person : party) {
 			this.money += person.getProfession().getMoney();
 			Logger.log(person.getName() + " as a " + 
 					person.getProfession() + " brings $" + 
 					person.getProfession().getMoney() + 
 					" to the party.", Logger.Level.INFO);
 
 			partyCreationLog.append(person.getName() + " ");
 		}
 		Logger.log(partyCreationLog.toString(), Logger.Level.INFO);
 		Logger.log("Party starting money is: $" + money, Logger.Level.INFO);
 		Logger.log("Current pace is: " + this.currentPace + 
 				" and current rations is: " + this.currentRations, Level.INFO);
 	}
 	
 	/**
 	 * Buys the item (pays for it) and then gives it to the designated buyer.
 	 * @param items The items to buy
 	 * @param buyer The thing that wants the items in its inventory
 	 */
 	public void buyItemForInventory(List<Item> items, Inventoried buyer) {
 		int cost = 0;
 		final Item.ITEM_TYPE itemType = items.get(0).getType();
 		final int numberOf = items.size();
 		
 		for (Item item : items) {
 			cost += item.getCost();
 		}
 		
 		if (money > cost && buyer.canGetItem(itemType, numberOf)) {
 			buyer.addItemsToInventory(items);
 			money -= cost;
 			return;
 		} else {
 			return;
 		}
 	}
 	
 	/**
 	 * Returns a list of inventoried things that can but the designated items.
 	 * @param itemType The item type that is being tested
 	 * @param numberOf The number of items to try with
 	 * @return The list of people
 	 */
 	public List<Inventoried> canGetItem(Item.ITEM_TYPE itemType, int numberOf) {
 		final List<Inventoried> ableList = new ArrayList<Inventoried>();
 		
 		if (itemType.getCost() * numberOf > money) {
 			return ableList;
 		}
 		
 		for (Person person : members) {
 			if (person.canGetItem(itemType, numberOf)) {
 				ableList.add(person);
 			}
 		}
 		
 		if (vehicle != null && vehicle.canGetItem(itemType, numberOf)) {
 			ableList.add(vehicle);
 		}
 		
 		return ableList;
 	}
 
 	/**
 	 * Returns a list of Persons present in the party
 	 * @return The members of the party
 	 */
 	public List<Person> getPartyMembers() {
 		return this.members;
 	}
 
 	/**
 	 * Returns a list of the Skills present in the party
 	 * @return A list of skills in the party
 	 */
 	public List<Person.Skill> getSkills() {
 		final List<Person.Skill> skillList = new ArrayList<Person.Skill>();
 		
 		for (Person person : members) {
 			for (Person.Skill skill: person.getSkills()) {
 				if (!skillList.contains(skill) && skill != Person.Skill.NONE) {
 					skillList.add(skill);
 				}
 			}
 		}
 		
 		return skillList;
 	}
 
 	@Override
 	public int getMoney() {
 		return this.money;
 	}
 	
 	/**
 	 * Sets the party's money
 	 * @param money
 	 */
 	public void setMoney(int money) {
 		Logger.log("Party money changed to: $" + money, Logger.Level.INFO);
 		
 		this.money = money;
 	}
 	
 	/**
 	 * Returns the vehicle of the party
 	 * @return The vehicle of the party
 	 */
 	public Vehicle getVehicle() {
 		return vehicle;
 	}
 	
 	/**
 	 * Sets a vehicle as the party vehicle.
 	 * @param vehicle The party vehicle
 	 */
 	public void setVehicle(Vehicle vehicle) {
 		this.vehicle = vehicle;
 	}
 	
 	/**
 	 * Get the party's pace.
 	 * @return the party's pace setting
 	 */
 	public Pace getPace() {
 		return currentPace;
 	}
 	
 	/**
 	 * Sets a party's new pace.
 	 * @param currentPace The party's new pace
 	 */
 	public void setPace(Pace currentPace) {
 		Logger.log("Party pace changed to: " + currentPace, Logger.Level.INFO);
 		this.currentPace = currentPace;
 	}
 	
 	/**
 	 * Returns the party's current ration consumption rate.
 	 * @return The party's current ration consumption rate
 	 */
 	public Rations getRations() {
 		return currentRations;
 	}
 	
 	/**
 	 * Sets the party's ration to a new ration consumption rate
 	 * @param rations The desired new ration consumption rate
 	 */
 	public void setRations(Rations rations) {
 		Logger.log("Party rations changed to: " + rations, Logger.Level.INFO);
 		
 		this.currentRations = rations;
 	}
 	
 	@Override
 	public String toString() {
 		final StringBuffer str = new StringBuffer("Members: ");
 		for (Person person : members) {
 			str.append(person.toString() + "; ");
 		}
 		str.append("Money remaining:" + money);
 		return str.toString();
 	}
 	
 	/**
 	 * Returns the current location.
 	 * @return Party's current location
 	 */
 	public LocationNode getLocation() {
 		return location;
 	}
 	
 	public void setLocation(LocationNode location) {
 		this.location = location;
 	}
 	
 	public TrailEdge getTrail() {
 		return trail;
 	}
 	
 	public void setTrail(TrailEdge trailEdge) {
 		this.trail = trailEdge;
 	}
 	
 	/**
 	 * Steps the player forward through the game.
 	 * @return the distance traveled with the walk
 	 */
 	public List<Notification> walk() {
 		List<Notification> messages = new ArrayList<Notification>();
 		trail.advance((int)(getPace().getSpeed() * getMoveModifier()));
 		
 		List<Animal> slaughterHouse = new ArrayList<Animal>();
 		for (Animal animal : animals) {
 			animal.decreaseStatus(getPace().getSpeed() - 75);
 			if(animal.getStatus().getCurrent() == 0) {
				vehicle.addItemsToInventory(animal.killForFood());
 				slaughterHouse.add(animal);
 			}
 		}
 		
 		List<Person> deathList = new ArrayList<Person>();
 		int healAmount = 0;
 		for (Person person : members) {
 			person.increaseSkillPoints((int) (getPace().getSpeed() / 10));
 			if(!personHasFood(person) && !vehicleHasFood()) {
 				person.decreaseHealth(getPace().getSpeed());
 			} else {
 				healAmount = getRations().getRationAmount() - eatFood(person, getRations().getRationAmount());
 				if(healAmount > 0) {
 					person.increaseHealth(healAmount);
 				}
 				else {
 					person.decreaseHealth(-healAmount);
 				}
 			}
 			if(person.getHealth().getCurrent() == 0) {
				vehicle.addItemsToInventory(person.killForFood());
 				deathList.add(person);
 			}
 		}
 		if(checkHungerStatus() != null) {
 			messages.add(new Notification(checkHungerStatus(), true));
 		}
 		for (Person person : deathList) {
 			members.remove(person);
 		}
 		for (Animal animal : slaughterHouse) {
 			animals.remove(animal);
 		}
 		//messages.add(new Notification("Current Distance Travelled: " + String.format("%,d", location), false));
 		return messages;
 	}
 
 	private double getMoveModifier() {
 		double moveModifier = 1;
 		for(Animal animal: getAnimals()) {
 			moveModifier += (double) animal.getMoveFactor() / 10;
 		}
 		return moveModifier;
 	}
 
 	private List<Animal> getAnimals() {
 		return animals;
 	}
 
 	/**
 	 * Heals the person by the designated amount.
 	 * @param person The person to feed
 	 * @param amount the amount to feed them
 	 */
 	private int eatFood(Person person, int amount) {
 		// Figure out how much restoration is needed.
 		int restoreNeeded = getRations().getRationAmount();
 		if(restoreNeeded == 0) {
 			return 0;
 		}
 				
 		if(!personHasFood(person) && !vehicleHasFood()) {
 			return amount;
 		}
 		
 		Inventoried donator = null;
 		if (personHasFood(person)) {
 			donator = person;
 		}
 		else if (!personHasFood(person) && vehicleHasFood()){
 			donator = vehicle;
 		}
 		
 		//If we need restoration, and have food
 		Item.ITEM_TYPE firstFood = null;
 		final List<Item.ITEM_TYPE> typeList = 
 			donator.getInventory().getPopulatedSlots();
 		
 		for (Item.ITEM_TYPE itemType : typeList) {
 			if (itemType.isFood() && firstFood == null) {
 				firstFood = itemType;
 			}
 		}
 		
 		final List<Item> foodList = 
 			donator.removeItemFromInventory(firstFood, 1);
 		
 		final Item food = foodList.get(0);
 		int foodFactor = food.getType().getFoodFactor();
 		
 		//Do some handling for party member skills, such as cooking
 		if(food.getType().isPlant() && getSkills().contains(Person.Skill.BOTANY)) {
 			foodFactor += 1;
 		}
 		if(getSkills().contains(Person.Skill.COOKING)) {
 			foodFactor += 1;
 		}
 
 		final int foodToEat = (restoreNeeded / foodFactor) + 1; //+1 to ensure that we overshoot
 		
 		if (food.getStatus().getCurrent() > foodToEat) {
 			//If there is enough condition in the food to feed the person completely, heal them and eat
 			
 			//person.increaseHealth(foodToEat * foodFactor);
 			food.decreaseStatus(foodToEat);
 			donator.addItemToInventory(food); //puts the item back in inventory
 			//Food status down, person health up
 			return 0;
 		} else {
 			//we don't have enough status in the food to completely heal the person, so eat it all.
 			int restoreAmount = food.getStatus().getCurrent() * foodFactor;
 			//person.increaseHealth(restoreAmount);
 			return eatFood(person, restoreNeeded - restoreAmount); //Recursively call the function to ensure we eat as much as possible.
 		}
 	}
 	
 	private boolean vehicleHasFood() {
		for (Item.ITEM_TYPE itemType : vehicle.getInventory().getPopulatedSlots()) {
			if (itemType.isFood()) {
				return true;
 			}
 		}
 		return false;
 	}
 
 	private boolean personHasFood(Person person) {
 		for (Item.ITEM_TYPE itemType : person.getInventory().getPopulatedSlots()) {
 			if (itemType.isFood()) {
 				return true;
 			}
 		}
 		return false;
 	}
 
 //	
 //	public void feedAnimal(Animal animal) {
 //		// Figure out how much restoration is needed.
 //		int restoreNeeded = 0;
 //		
 //		if (animal.getStatus().getCurrent() < getRations().getRationAmount()) {
 //			restoreNeeded = getRations().getRationAmount() - 
 //			animal.getStatus().getCurrent();
 //		
 //		}
 //		
 //		//See if there is feed available
 //		boolean vehicleHasFood = false;
 //		
 //		for (Item.ITEM_TYPE itemType : vehicle.getInventory().getPopulatedSlots()) {
 //			if (itemType.isPlant()) {
 //					vehicleHasFood = true;
 //			}
 //		}
 //		
 //		Inventoried donator = vehicleHasFood ? vehicle : null;
 //		
 //		if (restoreNeeded > 0 && donator != null) {
 //			//If we need restoration, and have food
 //			Item.ITEM_TYPE firstFood = null;
 //			final List<Item.ITEM_TYPE> typeList = 
 //				donator.getInventory().getPopulatedSlots();
 //			
 //			for (Item.ITEM_TYPE itemType : typeList) {
 //				if (itemType.isFood() && firstFood == null) {
 //					firstFood = itemType;
 //				}
 //			}
 //			
 //			final List<Item> foodList = 
 //				donator.removeItemFromInventory(firstFood, 1);
 //			
 //			final Item food = foodList.get(0);
 //			int foodFactor = food.getType().getFoodFactor() * 3;
 //			
 //			//Do some handling for party member skills, such as cooking
 //			if(food.getType().isPlant() && getSkills().contains(Person.Skill.BOTANY)) {
 //				foodFactor += 3;
 //			}
 //			if(getSkills().contains(Person.Skill.COOKING)) {
 //				foodFactor += 3;
 //			}
 //
 //			final int foodToEat = (restoreNeeded / foodFactor) + 1; //+1 to ensure that we overshoot
 //			
 //			if (food.getStatus().getCurrent() > foodToEat) {
 //				//If there is enough condition in the food to feed the person completely, heal them and eat
 //				
 //				animal.increaseStatus(foodToEat * foodFactor);
 //				food.decreaseStatus(foodToEat);
 //				donator.addItemToInventory(food); //puts the item back in inventory
 //				//Food status down, person health up
 //			} else {
 //				//we don't have enough status in the food to completely heal the person, so eat it all.
 //				animal.increaseStatus(food.getStatus().getCurrent() * foodFactor);
 //				feedAnimal(animal); //Recursively call the function to ensure we eat as much as possible.
 //			}
 //		}
 //	}
 	
 	/**
 	 * Determines if a party member is near dying or dead, and alerts the player.
 	 * @param person The person who's status we're checking.
 	 * @return A string with a message about the health status of the person
 	 */
 	public String checkHungerStatus() {
 		final StringBuilder str = new StringBuilder();
 		int currentHealth;
 		boolean hasMessage = false;
 		for(Person person : members) {
 			currentHealth = person.getHealth().getCurrent();
 			if(currentHealth == 0) {
 				hasMessage = true;
 				str.append(person.getName() + " has died of starvation!");
 			}
 			else if(person.getHealth().getCurrent() < getPace().getSpeed() - getRations().getRationAmount()) {
 				hasMessage = true;
 				str.append(person.getName() + " is in danger of starvation.");
 			}
 		}
 		if (hasMessage) {
 			return str.toString();
 		}
 		return null;
 	}
 
 	public void addAnimals(List<Animal> animalList) {
 		animals.addAll(animalList);
 	}
 }
