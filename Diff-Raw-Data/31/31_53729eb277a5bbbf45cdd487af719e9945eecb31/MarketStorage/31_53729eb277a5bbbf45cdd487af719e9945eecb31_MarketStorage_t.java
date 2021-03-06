 package com.survivorserver.GlobalMarket;
 
 import java.util.ArrayList;
 import java.util.Collection;
 import java.util.Collections;
 import java.util.HashMap;
 import java.util.Iterator;
 import java.util.LinkedHashMap;
 import java.util.List;
 import java.util.Map;
 import java.util.Map.Entry;
 import java.util.TreeSet;
 
 import org.bukkit.Material;
 import org.bukkit.configuration.InvalidConfigurationException;
 import org.bukkit.configuration.file.YamlConfiguration;
 import org.bukkit.enchantments.Enchantment;
 import org.bukkit.inventory.ItemStack;
 import org.bukkit.inventory.meta.BookMeta;
 import org.yaml.snakeyaml.Yaml;
 import org.yaml.snakeyaml.constructor.CustomClassLoaderConstructor;
 
 import com.google.common.base.Predicate;
 import com.google.common.collect.Collections2;
 import com.google.common.collect.Lists;
 import com.survivorserver.GlobalMarket.Chat.TellRawClickEvent;
 import com.survivorserver.GlobalMarket.Chat.TellRawHoverEvent;
 import com.survivorserver.GlobalMarket.Chat.TellRawMessage;
 import com.survivorserver.GlobalMarket.Lib.SearchResult;
 import com.survivorserver.GlobalMarket.Lib.SortMethod;
 import com.survivorserver.GlobalMarket.SQL.Database;
 import com.survivorserver.GlobalMarket.SQL.AsyncDatabase;
 import com.survivorserver.GlobalMarket.SQL.MarketResult;
 import com.survivorserver.GlobalMarket.SQL.QueuedStatement;
 import com.survivorserver.GlobalMarket.SQL.StorageMethod;
 
 public class MarketStorage {
 
 	private Market market;
 	private AsyncDatabase asyncDb;
 	private Map<Integer, ItemStack> items;
 	private Map<Integer, Listing> listings;
 	private Map<String, TreeSet<Listing>> worldListings;
 	private Map<Integer, Mail> mail;
 	private Map<String, List<Mail>> worldMail;
 	private Map<Integer, QueueItem> queue;
 	private TreeSet<Listing> condensedListings;
 	private int itemIndex;
 	private int listingIndex;
 	private int mailIndex;
 	private int queueIndex;
 	
 	public MarketStorage(Market market, AsyncDatabase asyncDb) {
 		this.market = market;
 		this.asyncDb = asyncDb;
 		items = new HashMap<Integer, ItemStack>();
 		listings = new LinkedHashMap<Integer, Listing>();
 		worldListings = new HashMap<String, TreeSet<Listing>>();
 		mail = new LinkedHashMap<Integer, Mail>();
 		worldMail = new HashMap<String, List<Mail>>();
 		queue = new LinkedHashMap<Integer, QueueItem>();
 		condensedListings = new TreeSet<Listing>();
 	}
 	
 	public void loadSchema(Database db) {
 		boolean sqlite = market.getConfigHandler().getStorageMethod() == StorageMethod.SQLITE;
 		try {
 			// Create items table
 			db.createStatement("CREATE TABLE IF NOT EXISTS items ("
 					+ (sqlite ? "id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT, " : "id int NOT NULL PRIMARY KEY AUTO_INCREMENT, ")
 					+ (sqlite ? "item MEDIUMTEXT UNIQUE" : "item MEDIUMTEXT CHARACTER SET utf8 COLLATE utf8_general_ci")
 					+ ")").execute();
 			// Create listings table
 			db.createStatement("CREATE TABLE IF NOT EXISTS listings ("
 					+ (sqlite ? "id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT, " : "id int NOT NULL PRIMARY KEY AUTO_INCREMENT, ") 
 					+ "seller TINYTEXT, "
 					+ "item int, "
 					+ "amount int, "
 					+ "price DOUBLE, "
 					+ "world TINYTEXT, "
 					+ "time BIGINT)").execute();
 			// Create mail table
 			db.createStatement("CREATE TABLE IF NOT EXISTS mail ("
 					+ (sqlite ? "id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT, " : "id int NOT NULL PRIMARY KEY AUTO_INCREMENT, ")
 					+ "owner TINYTEXT, "
 					+ "item int, "
 					+ "amount int, "
 					+ "sender TINYTEXT, "
 					+ "world TINYTEXT, "
 					+ "pickup DOUBLE)").execute();
 			// Create queue table
 			db.createStatement("CREATE TABLE IF NOT EXISTS queue ("
 					+ (sqlite ? "id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT, " : "id int NOT NULL PRIMARY KEY AUTO_INCREMENT, ")
 					+ "data MEDIUMTEXT)").execute();
 			// Create users metadata table
 			db.createStatement("CREATE TABLE IF NOT EXISTS users ("
 					+ "name varchar(16) NOT NULL UNIQUE, "
 					+ "earned DOUBLE, "
 					+ "spent DOUBLE)").execute();
 			// Create history table
 			db.createStatement("CREATE TABLE IF NOT EXISTS history ("
 					+ (sqlite ? "id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT, " : "id int NOT NULL PRIMARY KEY AUTO_INCREMENT, ")
 					+ "player TINYTEXT, "
 					+ "action TINYTEXT, "
 					+ "who TINYTEXT, "
 					+ "item int, "
 					+ "amount int, "
 					+ "price DOUBLE, "
 					+ "time BIGINT)").execute();
 		} catch(Exception e) {
 			market.log.severe("Error while preparing database:");
 			e.printStackTrace();
 		}
 	}
 	
	public void load(Database db) {
		String dbName = market.getConfig().getString("storage.mysql_database");
 		List<Integer> corruptItems = new ArrayList<Integer>();
 		boolean sqlite = market.getConfigHandler().getStorageMethod() == StorageMethod.SQLITE;
 		// Items we should cache in memory
 		List<Integer> itemIds = new ArrayList<Integer>();
 		try {
 			/*
 			 * Synchronize the listing index with the database
 			 */
 			listings.clear();
 			listingIndex = 1;
			MarketResult res = sqlite ? db.createStatement("SELECT seq FROM sqlite_sequence WHERE name = ? ").setString("listings").query() :
										db.createStatement("SHOW TABLE STATUS FROM " + dbName + " LIKE ?").setString("listings").query();
 			if (res.next()) {
				listingIndex = sqlite ? res.getInt(1) + 1 : res.getInt("Auto_increment");
 			}
 			market.log.info("Listing index: " + listingIndex);
 			res = db.createStatement("SELECT * FROM listings ORDER BY id ASC").query();
 			while(res.next()) {
 				Listing listing = res.constructListing(this);
 				int id = listing.getItemId();
 				if (!itemIds.contains(id)) {
 					itemIds.add(id);
 				}
 				listings.put(listing.getId(), listing);
 			}
 			buildCondensed();
 			/*
 			 * Synchronize the mail index with the database
 			 */
 			mail.clear();
 			res = db.createStatement("SELECT * FROM mail ORDER BY id ASC").query();
 			while(res.next()) {
 				Mail m = res.constructMail(this);
 				int id = m.getItemId();
 				if (!itemIds.contains(id)) {
 					itemIds.add(id);
 				}
 				mail.put(m.getId(), m);
 				addWorldItem(m);
 			}
 			mailIndex = 1;
			res = sqlite ? db.createStatement("SELECT seq FROM sqlite_sequence WHERE name = ? ").setString("mail").query() :
						   db.createStatement("SHOW TABLE STATUS FROM " + dbName + " LIKE ?").setString("mail").query();
 			if (res.next()) {
				mailIndex = sqlite ? res.getInt(1) + 1 : res.getInt("Auto_increment");
 			}
 			market.log.info("Mail index: " + mailIndex);
 			/*
 			 * Queue
 			 */
 			queue.clear();
 			res = db.createStatement("SELECT * FROM queue ORDER BY id ASC").query();
 			Yaml yaml = new Yaml(new CustomClassLoaderConstructor(Market.class.getClassLoader()));
 			while(res.next()) {
 				String q = res.getString("data");
 				try {
 					QueueItem item = yaml.loadAs(q, QueueItem.class);
 					queue.put(item.getId(), item);
 					int itemId;
 					if (item.getMail() != null) {
 						itemId = item.getMail().getItemId();
 					} else {
 						itemId = item.getListing().getItemId();
 					}
 					if (!itemIds.contains(itemId)) {
 						itemIds.add(itemId);
 					}
 				} catch(NullPointerException e) {
 					market.log.warning("Queue item is corrupt:");
 					market.log.warning(q);
 				}
 			}
 			queueIndex = 1;
			res = sqlite ? db.createStatement("SELECT seq FROM sqlite_sequence WHERE name = ? ").setString("queue").query() :
						   db.createStatement("SHOW TABLE STATUS FROM " + dbName + " LIKE ?").setString("queue").query();
 			if (res.next()) {
				queueIndex = sqlite ? res.getInt(1) + 1 : res.getInt("Auto_increment");
 			}
 			market.log.info("Queue index: " + queueIndex);
 			/*
 			 * Synchronize needed items
 			 */
 			items.clear();
 			if (itemIds.size() > 0) {
 				/*StringBuilder query = new StringBuilder();
 				query.append("SELECT * FROM items WHERE id IN (");
 				for (int i = 0; i < itemIds.size(); i++) {
 					query.append(itemIds.get(i));
 					if (i + 1 == itemIds.size()) {
 						query.append(")");
 					} else {
 						query.append(", ");
 					}
 				}
 				res = db.createStatement(query.toString()).query();*/
 				
 				// Probably need a better fix for mismatched item IDs than this
 				res = db.createStatement("SELECT * FROM items").query();
 				Map<Integer, String> sanitizedItems = new HashMap<Integer, String>();
 				while(res.next()) {
 					try {
 						YamlConfiguration conf = new YamlConfiguration();
 						conf.loadFromString(res.getString(2));
 						int itemId = res.getInt(1);
 						items.put(itemId, conf.getItemStack("item").clone());
 						itemIds.remove(new Integer(itemId));
 					} catch(Exception e) {
 						if (e instanceof InvalidConfigurationException) {
 							int itemId = res.getInt(1);
 							market.log.warning("Item ID " + itemId + " has invalid characters");
 							String san = res.getString(2).replaceAll("[\\p{Cc}&&[^\r\n\t]]", "");
 							sanitizedItems.put(itemId, san);
 							items.put(res.getInt(1), itemStackFromString(san));
 						} else {
 							int itemId = res.getInt(1);
 							market.log.warning(String.format("Item with ID %s is corrupt or missing from the game. This item will be removed from storage and all mail/listings using it will be deleted. You can safely ignore this exception!", itemId));
 							items.put(itemId, new ItemStack(Material.STONE));
 							corruptItems.add(itemId);
 						}
 					}
 				}
 				for (int itemId : itemIds) {
 					market.log.warning(String.format("Item ID %s was requested but not found, perhaps a database desync?", itemId));
 					corruptItems.add(itemId);
 				}
 				for (Entry<Integer, String> entry : sanitizedItems.entrySet()) {
 					db.createStatement("UPDATE items SET item=? WHERE id=?").setString(entry.getValue()).setInt(entry.getKey()).execute();
 				}
 			}
 			itemIndex = 1;
			res = sqlite ? db.createStatement("SELECT seq FROM sqlite_sequence WHERE name = ? ").setString("items").query() :
						   db.createStatement("SHOW TABLE STATUS FROM " + dbName + " LIKE ?").setString("items").query();
 			if (res.next()) {
				itemIndex = sqlite ? res.getInt(1) + 1 : res.getInt("Auto_increment");
 			}
 			market.log.info("Item index: " + itemIndex);
 			if (!corruptItems.isEmpty()) {
 				for (int itemId : corruptItems) {
 					for (Listing listing : getAllListings()) {
 						if (listing.getItemId() == itemId) {
 							removeListing(listing.getId());
 						}
 					}
 					Iterator<Entry<Integer, Mail>> it = mail.entrySet().iterator();
 					while(it.hasNext()) {
 						Entry<Integer, Mail> entry = it.next();
 						if (entry.getValue().getItemId() == itemId) {
 							removeMail(entry.getValue().getId());
 						}
 					}
 					for (QueueItem item : getQueue()) {
 						if (item.getMail() != null) {
 							if (item.getMail().getItemId() == itemId) {
 								removeItemFromQueue(item.getId());
 								removeMail(item.getMail().getId());
 							}
 						} else {
 							if (item.getListing().getItemId() == itemId) {
 								removeItemFromQueue(item.getId());
 								removeListing(item.getListing().getId());
 							}
 						}
 					}
 					items.remove(itemId);
 					db.createStatement("DELETE FROM history WHERE item = ?").setInt(itemId).execute();
 					db.createStatement("DELETE FROM items WHERE id = ?").setInt(itemId).execute();
 					market.log.info(String.format("Removed corrupted item %s", itemId));
 				}
 			}
 		} catch(Exception e) {
 			market.log.severe("Error while loading:");
 			e.printStackTrace();
 		}
 	}
 	
 	private void addWorldItem(Listing listing) {
 		String world = listing.getWorld();
 		if (!worldListings.containsKey(world)) {
 			worldListings.put(world, new TreeSet<Listing>());
 		}
 		TreeSet<Listing> listings = worldListings.get(world);
 		for (Listing l : listings) {
 			if (l.isStackable(listing)) {
 				l.addStacked(listing);
 				return;
 			}
 		}
 		listings.add(listing);
 	}
 	
 	private void addWorldItem(Mail mailItem) {
 		String world = mailItem.getWorld();
 		if (!worldMail.containsKey(world)) {
 			worldMail.put(world, new ArrayList<Mail>());
 		}
 		worldMail.get(world).add(mailItem);
 	}
 	
 	private List<Listing> getListingsForWorld(String world) {
 		List<Listing> toReturn = new ArrayList<Listing>();
 		if (worldListings.containsKey(world)) {
 			toReturn.addAll(new ArrayList<Listing>(worldListings.get(world)));
 		}
 		for (String w : market.getLinkedWorlds(world)) {
 			if (worldListings.containsKey(w)) {
 				toReturn.addAll(new ArrayList<Listing>(worldListings.get(w)));
 			}
 		}
 		return toReturn;
 	}
 	
 	private List<Mail> getMailForWorld(String world) {
 		List<Mail> toReturn = new ArrayList<Mail>();
 		if (worldMail.containsKey(world)) {
 			toReturn.addAll(worldMail.get(world));
 		}
 		for (String w : market.getLinkedWorlds(world)) {
 			if (worldMail.containsKey(w)) {
 				toReturn.addAll(worldMail.get(w));
 			}
 		}
 		return toReturn;
 	}
 
 	public AsyncDatabase getAsyncDb() {
 		return asyncDb;
 	}
 	
 	public Listing queueListing(String seller, ItemStack itemStack, double price, String world) {
 		int itemId = storeItem(itemStack);
 		long time = System.currentTimeMillis();
 		Listing listing = new Listing(listingIndex++, seller, itemId, itemStack.getAmount(), price, world, time);
 		QueueItem item = new QueueItem(queueIndex++, time, listing);
 		queue.put(item.getId(), item);
 		asyncDb.addStatement(new QueuedStatement("INSERT INTO queue (data) VALUES (?)")
 		.setValue((String) new Yaml().dump(item)));
 		return listing;
 	}
 	
 	public void queueListing(String seller, List<ItemStack> items, double pricePerItem, String world) {
 		int itemId = storeItem(items.get(0));
 		for (ItemStack itemStack : items) {
 			double price = pricePerItem * itemStack.getAmount();
 			long time = System.currentTimeMillis();
 			Listing listing = new Listing(listingIndex++, seller, itemId, itemStack.getAmount(), price, world, time);
 			QueueItem item = new QueueItem(queueIndex++, time, listing);
 			queue.put(item.getId(), item);
 			asyncDb.addStatement(new QueuedStatement("INSERT INTO queue (data) VALUES (?)")
 			.setValue((String) new Yaml().dump(item)));
 		}
 	}
 	
 	public Mail queueMail(String owner, String from, ItemStack itemStack, String world) {
 		int itemId = storeItem(itemStack);
 		Mail mail = new Mail(owner, mailIndex++, itemId, itemStack.getAmount(), 0, from, world);
 		QueueItem item = new QueueItem(queueIndex++, System.currentTimeMillis(), mail);
 		queue.put(item.getId(), item);
 		asyncDb.addStatement(new QueuedStatement("INSERT INTO queue (data) VALUES (?)")
 		.setValue((String) new Yaml().dump(item)));
 		return mail;
 	}
 	
 	public Mail queueMail(String owner, String from, int itemId, int amount, String world) {
 		Mail mail = new Mail(owner, mailIndex++, itemId, amount, 0, from, world);
 		QueueItem item = new QueueItem(queueIndex++, System.currentTimeMillis(), mail);
 		queue.put(item.getId(), item);
 		asyncDb.addStatement(new QueuedStatement("INSERT INTO queue (data) VALUES (?)")
 		.setValue((String) new Yaml().dump(item)));
 		return mail;
 	}
 	
 	public List<QueueItem> getQueue() {
 		return new ArrayList<QueueItem>(queue.values());
 	}
 	
 	public void removeItemFromQueue(int id) {
 		QueueItem item = queue.get(new Integer(id));
 		if (item.getMail() != null) {
 			storeMail(item.getMail());
 		} else {
 			storeListing(item.getListing());
 		}
 		asyncDb.addStatement(new QueuedStatement("DELETE FROM queue WHERE id=?").setValue(id));
 		queue.remove(id);
 	}
 	
 	public static String itemStackToString(ItemStack item) {
 		YamlConfiguration conf = new YamlConfiguration();
 		ItemStack toSave = item.clone();
 		toSave.setAmount(1);
 		conf.set("item", toSave);
 		return conf.saveToString();
 	}
 	
 	public static ItemStack itemStackFromString(String item) throws InvalidConfigurationException {
 		YamlConfiguration conf = new YamlConfiguration();
 		conf.loadFromString(item);
 		return conf.getItemStack("item").clone();
 	}
 	
 	public static ItemStack itemStackFromString(String item, int amount) {
 		YamlConfiguration conf = new YamlConfiguration();
 		try {
 			conf.loadFromString(item);
 			ItemStack itemStack = conf.getItemStack("item");
 			itemStack.setAmount(amount);
 			return itemStack;
 		} catch (InvalidConfigurationException e) {
 			e.printStackTrace();
 		}
 		return null;
 	}
 	
 	public int storeItem(ItemStack item) {
 		ItemStack storable = item.clone();
 		storable.setAmount(1);
 		for (Entry<Integer, ItemStack> ent : items.entrySet()) {
 			if (ent.getValue().equals(storable)) {
 				return ent.getKey();
 			}
 		}
 		if (asyncDb.getDb().isSqlite()) {
 			asyncDb.addStatement(new QueuedStatement("INSERT OR IGNORE INTO items (item) VALUES (?)")
 			.setValue(storable));
 		} else {
 			String store = itemStackToString(storable);
 			asyncDb.addStatement(new QueuedStatement("INSERT INTO items (item) SELECT * FROM (SELECT ?) AS tmp WHERE NOT EXISTS (SELECT item FROM items WHERE item = ?) LIMIT 1;")
 			.setValue(store)
 			.setValue(store));
 		}
 		
 		items.put(itemIndex, storable);
 		return itemIndex++;
 	}
 	
 	public ItemStack getItem(int id, int amount) {
 		if (!items.containsKey(new Integer(id))) {
 			market.log.severe("Couldn't find an item with ID " + id);
 		}
 		ItemStack item = items.get(new Integer(id)).clone();
 		item.setAmount(amount);
 		return item;
 	}
 	
 	public Listing createListing(String seller, ItemStack item, double price, String world) {
 		int itemId = storeItem(item);
 		Long time = System.currentTimeMillis();
 		int id = listingIndex++;
 		asyncDb.addStatement(new QueuedStatement("INSERT INTO listings (id, seller, item, amount, price, world, time) VALUES (?, ?, ?, ?, ?, ?, ?)")
 		.setValue(id)
 		.setValue(seller)
 		.setValue(itemId)
 		.setValue(item.getAmount())
 		.setValue(price)
 		.setValue(world)
 		.setValue(time));
 		Listing listing = new Listing(id, seller, itemId, item.getAmount(), price, world, time);
 		listings.put(listing.getId(), listing);
 		addWorldItem(listing);
 		addToCondensed(listing);
 		if (market.getInterfaceHandler() != null) {
 			// This will be null if the importer is running
 			market.getInterfaceHandler().updateAllViewers();
 		}
 		return listing;
 	}
 	
 	public void createListing(String seller, List<ItemStack> items, double pricePerItem, String world) {
 		int itemId = storeItem(items.get(0));
 		for (ItemStack item : items) {
 			double price = pricePerItem * item.getAmount();
 			Long time = System.currentTimeMillis();
 			int id = listingIndex++;
 			asyncDb.addStatement(new QueuedStatement("INSERT INTO listings (id, seller, item, amount, price, world, time) VALUES (?, ?, ?, ?, ?, ?, ?)")
 			.setValue(id)
 			.setValue(seller)
 			.setValue(itemId)
 			.setValue(item.getAmount())
 			.setValue(price)
 			.setValue(world)
 			.setValue(time));
 			Listing listing = new Listing(id, seller, itemId, item.getAmount(), price, world, time);
 			listings.put(listing.getId(), listing);
 			addWorldItem(listing);
 			addToCondensed(listing);
 		}
 		if (market.getInterfaceHandler() != null) {
 			// This will be null if the importer is running
 			market.getInterfaceHandler().updateAllViewers();
 		}
 	}
 	
 	public void storeListing(Listing listing) {
 		asyncDb.addStatement(new QueuedStatement("INSERT INTO listings (id, seller, item, amount, price, world, time) VALUES (?, ?, ?, ?, ?, ?, ?)")
 		.setValue(listing.getId())
 		.setValue(listing.getSeller())
 		.setValue(listing.getItemId())
 		.setValue(listing.getAmount())
 		.setValue(listing.getPrice())
 		.setValue(listing.getWorld())
 		.setValue(listing.getTime()));
 		listings.put(listing.getId(), listing);
 		addWorldItem(listing);
 		addToCondensed(listing);
 		if (market.getInterfaceHandler() != null) {
 			// This will be null if the importer is running
 			market.notifyPlayer(listing.getSeller(), market.getLocale().get("your_listing_has_been_added", market.getItemName(getItem(listing.getItemId(), listing.getAmount()))));
 			market.getInterfaceHandler().updateAllViewers();
 		}
 	}
 	
 	public void storeMail(Mail m) {
 		asyncDb.addStatement(new QueuedStatement("INSERT INTO mail (id, owner, item, amount, sender, world, pickup) VALUES (?, ?, ?, ?, ?, ?, ?)")
 		.setValue(m.getId())
 		.setValue(m.getOwner())
 		.setValue(m.getItemId())
 		.setValue(m.getAmount())
 		.setValue(m.getSender())
 		.setValue(m.getWorld())
 		.setValue(m.getPickup()));
 		mail.put(m.getId(), m);
 		addWorldItem(m);
 		market.notifyPlayer(m.getOwner(), market.getLocale().get("you_have_new_mail"));
 		if (market.getInterfaceHandler() != null) {
 			// This will be null if the importer is running
 			market.getInterfaceHandler().refreshViewer(m.getOwner(), "Mail");
 		}
 	}
 	
 	public synchronized Listing getListing(int id) {
 		if (listings.containsKey(id)) {
 			return listings.get(id);
 		}
 		return null;
 	}
 	
 	public List<Listing> getListings(String viewer, SortMethod sort, int page, int pageSize, String world) {
 		List<Listing> toReturn = new ArrayList<Listing>();
 		int index = (pageSize * page) - pageSize;
 		List<Listing> list = market.enableMultiworld() ? getListingsForWorld(world) : new ArrayList<Listing>(condensedListings);
 		switch(sort) {
 			default:
 				Collections.sort(list, Listing.Comparators.RECENT);
 				break;
 			case DEFAULT:
 				Collections.sort(list, Listing.Comparators.RECENT);
 				break;
 			case PRICE_HIGHEST:
 				Collections.sort(list, Listing.Comparators.PRICE_HIGHEST);
 				break;
 			case PRICE_LOWEST:
 				Collections.sort(list, Listing.Comparators.PRICE_LOWEST);
 				break;
 			case AMOUNT_HIGHEST:
 				Collections.sort(list, Listing.Comparators.AMOUNT_HIGHEST);
 				break;
 		}
 		while (list.size() > index && toReturn.size() < pageSize) {
 			Listing l = list.get(index);
 			toReturn.add(l);
 			index++;
 		}
 		return toReturn;
 	}
 	
 	private void buildCondensed() {
 		for (Listing listing : Lists.reverse(new ArrayList<Listing>(listings.values()))) {
 			for (Listing l : condensedListings) {
 				if (l.isStackable(listing)) {
 					l.addStacked(listing);
 					break;
 				}
 			}
 			condensedListings.add(listing);
 			addWorldItem(listing);
 		}
 	}
 	
 	private void addToCondensed(Listing listing) {
 		for (Listing l : condensedListings) {
 			if (l.isStackable(listing)) {
 				l.addStacked(listing);
 				return;
 			}
 		}
 		condensedListings.add(listing);
 		
 		if (market.getChat() != null) {
 			// Don't run this if we're importing...
 			
 			if (market.announceOnCreate()) {
 				ItemStack created = getItem(listing.getItemId(), 1);
 				market.getChat().announce(new TellRawMessage().setText(market.getLocale().get("listing_created.prefix1"))
 						.setColor(market.getLocale().get("listing_created.prefix1_color"))
 						.setExtra(
 						new TellRawMessage[] {
 							new TellRawMessage().setText(market.getLocale().get("listing_created.prefix2")).setBold(true)
 							.setColor(market.getLocale().get("listing_created.prefix2_color")),
 							
 							new TellRawMessage().setText(market.getLocale().get("listing_created.prefix3")).setBold(false)
 							.setColor(market.getLocale().get("listing_created.prefix3_color")),
 							
 							new TellRawMessage().setText(market.getLocale().get("listing_created.main"))
 							.setColor(market.getLocale().get("listing_created.main_color"))
 							.setExtra(
 								new TellRawMessage[] {
 									new TellRawMessage()
 									.setText(market.getLocale().get("listing_created.item", market.getItemName(created)))
 									.setColor(market.getLocale().get("listing_created.item_color"))
 									.setHover(new TellRawHoverEvent()
 											.setAction(TellRawHoverEvent.ACTION_SHOW_ITEM)
 											.setValue(market.getChat().jsonStack(created)))
 									.setClick(new TellRawClickEvent()
 											.setAction(TellRawClickEvent.ACTION_RUN_COMMAND)
 											.setValue("/market listings " + listing.getId())),
 			
 									new TellRawMessage()
 									.setText(market.getLocale().get("listing_created.suffix"))
 									.setColor(market.getLocale().get("listing_created.suffix_color"))
 							}
 						)
 					})
 				, "globalmarket.seeannounce");
 			}
 		}
 	}
 	
 	private void removeFromCondensed(Listing listing) {
 		Iterator<Listing> it = condensedListings.iterator();
 		Listing sibling = null;
 		while(it.hasNext()) {
 			Listing l = it.next();
 			if (l.getId() == listing.getId()) {
 				it.remove();
 				if (l.getStacked().size() > 0) {
 					Listing n = l.getStacked().get(0);
 					l.getStacked().remove(n);
 					n.setStacked(new ArrayList<Listing>(l.getStacked()));
 					sibling = n;
 				}
 				break;
 			}
 		}
 		if (sibling != null) {
 			condensedListings.add(sibling);
 		}
 		if (worldListings.containsKey(listing.getWorld())) {
 			TreeSet<Listing> world = worldListings.get(listing.getWorld());
 			it = world.iterator();
 			while(it.hasNext()) {
 				Listing l = it.next();
 				if (l.getId() == listing.getId()) {
 					it.remove();
 					break;
 				}
 			}
 			if (sibling != null) {
 				world.add(sibling);
 			}
 		}
 	}
 	
 	public List<Listing> getOwnedListings(int page, int pageSize, String world, String name) {
 		List<Listing> list = new ArrayList<Listing>();
 		for (Listing listing : market.enableMultiworld() ? getListingsForWorld(world) : listings.values()) {
 			if (listing.getSeller().equalsIgnoreCase(name)) {
 				list.add(listing);
 			}
 		}
 		int index = (pageSize * page) - pageSize;
 		List<Listing> toReturn = new ArrayList<Listing>();
 		while (list.size() > index && toReturn.size() < pageSize) {
 			toReturn.add(list.get(index));
 			index++;
 		}
 		return toReturn;
 	}
 	
 	public List<Listing> getAllListings() {
 		return new ArrayList<Listing>(listings.values());
 	}
 	
 	@SuppressWarnings("deprecation")
 	public SearchResult getListings(String viewer, SortMethod sort, int page, int pageSize, String search, String world) {
 		List<Listing> found = new ArrayList<Listing>();
 		List<Listing> list = market.enableMultiworld() ? getListingsForWorld(world) : new ArrayList<Listing>(condensedListings);
 		for (Listing listing : list) {
 			ItemStack item = getItem(listing.getItemId(), listing.getAmount());
 			String itemName = market.getItemName(item);
 			if (itemName.toLowerCase().contains(search.toLowerCase())
 					|| isItemId(search, item.getTypeId())
 					|| isInDisplayName(search.toLowerCase(), item)
 					|| isInEnchants(search.toLowerCase(), item)
 					|| isInLore(search.toLowerCase(), item)
 					|| search.equalsIgnoreCase(Integer.toString(listing.getId()))
 					|| listing.seller.toLowerCase().contains(search.toLowerCase())) {
 				found.add(listing);
 			}
 		}
 		switch(sort) {
 			default:
 				break;
 			case PRICE_HIGHEST:
 				Collections.sort(found, Listing.Comparators.PRICE_HIGHEST);
 				break;
 			case PRICE_LOWEST:
 				Collections.sort(found, Listing.Comparators.PRICE_LOWEST);
 				break;
 			case AMOUNT_HIGHEST:
 				Collections.sort(found, Listing.Comparators.AMOUNT_HIGHEST);
 				break;
 		}
 		int index = (pageSize * page) - pageSize;
 		List<Listing> toReturn = new ArrayList<Listing>();
 		while (found.size() > index && toReturn.size() < pageSize) {
 			toReturn.add(found.get(index));
 			index++;
 		}
 		return new SearchResult(found.size(), toReturn);
 	}
 	
 	public void removeListing(int id) {
 		Listing listing = listings.get(id);
 		removeFromCondensed(listing);
 		listings.remove(id);
 		asyncDb.addStatement(new QueuedStatement("DELETE FROM listings WHERE id=?")
 		.setValue(id));
 	}
 	
 	public int getNumListings(String world) {
 		return market.enableMultiworld() ? getListingsForWorld(world).size() : condensedListings.size();
 	}
 	
 	public int getNumListingsFor(String name, String world) {
 		int amount = 0;
 		for (Listing listing : market.enableMultiworld() ? getListingsForWorld(world) : listings.values()) {
 			if (listing.getSeller().equalsIgnoreCase(name)) {
 				amount++;
 			}
 		}
 		return amount;
 	}
 	
 	public Map<Integer, Listing> getCachedListingIndex() {
 		return listings;
 	}
 	
 	public Mail createMail(String owner, String from, int itemId, int amount, String world) {
 		int id = mailIndex++;
 		asyncDb.addStatement(new QueuedStatement("INSERT INTO mail (id, owner, item, amount, sender, world, pickup) VALUES (?, ?, ?, ?, ?, ?, ?)")
 		.setValue(id)
 		.setValue(owner)
 		.setValue(itemId)
 		.setValue(amount)
 		.setValue(from)
 		.setValue(world)
 		.setValue(0));
 		Mail m = new Mail(owner, id, itemId, amount, 0, from, world);
 		mail.put(m.getId(), m);
 		addWorldItem(m);
 		if (market.getInterfaceHandler() != null) {
 			// This will be null if the importer is running
 			market.getInterfaceHandler().refreshViewer(m.getOwner(), "Mail");
 		}
 		return m;
 	}
 	
 	public Mail createMail(String owner, String from, ItemStack item, double pickup, String world) {
 		int itemId = storeItem(item);
 		int id = mailIndex++;
 		asyncDb.addStatement(new QueuedStatement("INSERT INTO mail (id, owner, item, amount, sender, world, pickup) VALUES (?, ?, ?, ?, ?, ?, ?)")
 		.setValue(id)
 		.setValue(owner)
 		.setValue(itemId)
 		.setValue(item.getAmount())
 		.setValue(from)
 		.setValue(world)
 		.setValue(pickup));
 		Mail m = new Mail(owner, id, itemId, item.getAmount(), pickup, from, world);
 		mail.put(m.getId(), m);
 		addWorldItem(m);
 		if (market.getInterfaceHandler() != null) {
 			// This will be null if the importer is running
 			market.getInterfaceHandler().refreshViewer(m.getOwner(), "Mail");
 		}
 		return m;
 	}
 	
 	public void storePayment(ItemStack item, String player, String buyer, double fullAmount, double amount, double cut, String world) {
 		ItemStack book = new ItemStack(Material.WRITTEN_BOOK);
 		BookMeta meta = (BookMeta) book.getItemMeta();
 		if (meta == null) {
 			meta = (BookMeta) market.getServer().getItemFactory().getItemMeta(book.getType());
 		}
 		meta.setTitle(market.getLocale().get("transaction_log.item_name"));
 		String itemName = market.getItemName(item);
 		String logStr = market.getLocale().get("transaction_log.title") + "\n\n" +
 						market.getLocale().get("transaction_log.item_sold", itemName) + "\n\n" +
 						market.getLocale().get("transaction_log.buyer", buyer) + "\n\n" +
 						market.getLocale().get("transaction_log.sale_price", fullAmount) + "\n\n" +
 						market.getLocale().get("transaction_log.market_cut", cut) +  "\n\n" +
 						market.getLocale().get("transaction_log.amount_recieved", amount);
 		meta.setPages(logStr);
 		book.setItemMeta(meta);
 		createMail(player, buyer, book, amount, world);
 	}
 	
 	public Mail getMail(int id) {
 		if (mail.containsKey(id)) {
 			return mail.get(id);
 		}
 		return null;
 	}
 	
 	public List<Mail> getMail(final String owner, int page, int pageSize, final String world) {
 		Collection<Mail> ownedMail = Collections2.filter(market.enableMultiworld() ? getMailForWorld(world) : mail.values(), new Predicate<Mail>() {
 			public boolean apply(Mail mail) {
 				return mail.getOwner().equals(owner);
 			}
 		});
 		List<Mail> toReturn = new ArrayList<Mail>();
 		int index = (pageSize * page) - pageSize;
 		List<Mail> list = Lists.reverse(new ArrayList<Mail>(ownedMail));
 		while (ownedMail.size() > index && toReturn.size() < pageSize) {
 			toReturn.add(list.get(index));
 			index++;
 		}
 		return toReturn;
 	}
 	
 	public void nullifyMailPayment(int id) {
 		asyncDb.addStatement(new QueuedStatement("UPDATE mail SET pickup=? WHERE id=?")
 		.setValue(0)
 		.setValue(id));
 		if (mail.containsKey(id)) {
 			Mail m = mail.get(id);
 			m.setPickup(0);
 			if (market.getInterfaceHandler() != null) {
 				// This will be null if the importer is running
 				market.getInterfaceHandler().refreshViewer(m.getOwner(), "Mail");
 			}
 		}
 	}
 	
 	public void removeMail(int id) {
 		Mail m = mail.get(id);
 		mail.remove(id);
 		worldMail.get(m.getWorld()).remove(m);
 		asyncDb.addStatement(new QueuedStatement("DELETE FROM mail WHERE id=?")
 		.setValue(id));
 		if (market.getInterfaceHandler() != null) {
 			// This will be null if the importer is running
 			market.getInterfaceHandler().refreshViewer(m.getOwner(), "Mail");
 		}
 	}
 	
 	public int getNumMail(final String player, final String world) {
 		Collection<Mail> ownedMail = Collections2.filter(market.enableMultiworld() ? getMailForWorld(world) : mail.values(), new Predicate<Mail>() {
 				public boolean apply(Mail mail) {
 					return mail.getOwner().equals(player);
 				}
 			});
 		return ownedMail.size();
 	}
 	
 	/*
 	 * Basic search method
 	 */
 	public boolean isItemId(String search, int typeId) {
 		if (search.equalsIgnoreCase(Integer.toString(typeId))) {
 			return true;
 		}
 		return false;
 	}
 	
 	/*
 	 * Basic search method
 	 */
 	public boolean isInDisplayName(String search, ItemStack item) {
 		if (item.hasItemMeta() && item.getItemMeta().hasDisplayName()) {
 			return item.getItemMeta().getDisplayName().toLowerCase().contains(search);
 		}
 		return false;
 	}
 	
 	/*
 	 * Basic search method
 	 */
 	public boolean isInEnchants(String search, ItemStack item) {
 		if (item.hasItemMeta() && item.getItemMeta().hasEnchants()) {
 			for (Entry<Enchantment, Integer> entry : item.getItemMeta().getEnchants().entrySet()) {
 				if (entry.getKey().getName().toLowerCase().contains(search)) {
 					return true;
 				}
 			}
 		}
 		return false;
 	}
 	
 	/*
 	 * Basic search method
 	 */
 	public boolean isInLore(String search, ItemStack item) {
 		if (item.hasItemMeta() && item.getItemMeta().hasLore()) {
 			for (String l : item.getItemMeta().getLore()) {
 				if (l.toLowerCase().contains(search)) {
 					return true;
 				}
 			}
 		}
 		return false;
 	}
 	
 	
 }
