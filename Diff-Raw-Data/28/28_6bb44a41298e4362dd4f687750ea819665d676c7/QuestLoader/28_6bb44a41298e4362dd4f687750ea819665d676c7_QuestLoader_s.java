 package com.adamki11s.quests;
 
 import java.io.File;
 import java.util.HashMap;
 
 import org.bukkit.Location;
 import org.bukkit.entity.ExperienceOrb;
 import org.bukkit.entity.Player;
 import org.bukkit.inventory.ItemStack;
 
 import com.adamki11s.commands.QuestXCMDExecutor;
 import com.adamki11s.exceptions.InvalidISAException;
 import com.adamki11s.exceptions.InvalidKillTrackerException;
 import com.adamki11s.exceptions.InvalidQuestException;
 import com.adamki11s.io.FileLocator;
 import com.adamki11s.npcs.tasks.EntityKillTracker;
 import com.adamki11s.npcs.tasks.Fireworks;
 import com.adamki11s.npcs.tasks.ISAParser;
 import com.adamki11s.npcs.tasks.NPCKillTracker;
 import com.adamki11s.questx.QuestX;
 import com.adamki11s.reputation.ReputationManager;
 import com.adamki11s.sync.io.configuration.SyncConfiguration;
 
 public class QuestLoader {
 
 	final SyncConfiguration config;
 
 	volatile QuestTask[] tasks;
 
 	String questName, startText, endText;
 
 	int nodes, rewardExp, rewardRep, rewardGold, fwRadius, fwSectors;
 	ItemStack[] rewardItems;
 
 	volatile HashMap<String, Integer> playerProgress = new HashMap<String, Integer>();
 	volatile HashMap<Integer, String> nodeCompleteText = new HashMap<Integer, String>();
 	volatile HashMap<String, QuestTask> currentTask = new HashMap<String, QuestTask>();
 
 	String[] addPerms, remPerms, playerCmds, serverCmds;
 	EntityKillTracker ekt;
 	NPCKillTracker nkt;
 	boolean apAdd, apRem, execPlayerCommand, execServerCommand, fireWorks;
 
 	public QuestLoader(File f) {
 		this.config = new SyncConfiguration(f);
 		try {
 			this.load();
 		} catch (InvalidQuestException e) {
 			e.printErrorReason();
 		}
 	}
 
 	void load() throws InvalidQuestException {
 		this.config.read();
 		this.questName = config.getString("NAME");
 		int i = 0;
 		while (config.doesKeyExist((i + 1) + "")) {
 			i++;
 		}
 		this.nodes = i;
 		if (config.doesKeyExist("REWARD_ITEMS")) {
 			if (!this.config.getString("REWARD_ITEMS").equalsIgnoreCase("0")) {
 				try {
 					this.rewardItems = ISAParser.parseISA(this.config.getString("REWARD_ITEMS"), this.questName, true);
 				} catch (InvalidISAException e) {
 					this.rewardItems = null;
 					e.printErrorReason();
 				}
 			} else {
 				this.rewardItems = null;
 			}
 		} else {
 			QuestX.logError("Missing property 'REWARD_ITEMS' in quest file for Quest " + this.questName);
 			this.rewardItems = null;
 		}
 
 		if (config.doesKeyExist("REWARD_EXP")) {
 			this.rewardExp = config.getInt("REWARD_EXP");
 		} else {
 			QuestX.logError("Missing property 'REWARD_EXP' in quest file for Quest " + this.questName);
 		}
 
 		if (config.doesKeyExist("REWARD_REP")) {
 			this.rewardRep = config.getInt("REWARD_REP");
 		} else {
 			QuestX.logError("Missing property 'REWARD_REP' in quest file for Quest " + this.questName);
 		}
 		if (config.doesKeyExist("START_TEXT")) {
 			this.startText = this.config.getString("START_TEXT");
 		} else {
 			QuestX.logError("Missing property 'START_TEXT' in quest file for Quest " + this.questName);
 		}
 
 		if (config.doesKeyExist("END_TEXT")) {
 			this.endText = this.config.getString("END_TEXT");
 		} else {
 			QuestX.logError("Missing property 'END_TEXT' in quest file for Quest " + this.questName);
 		}
 
 		if (config.doesKeyExist("REWARD_GOLD")) {
 			this.rewardGold = config.getInt("REWARD_GOLD");
 		} else {
 			QuestX.logError("Missing property 'REWARD_GOLD' in quest file for Quest " + this.questName);
 		}
 
 		if (config.doesKeyExist("REWARD_PERMISSIONS_ADD")) {
 			if (!config.getString("REWARD_PERMISSIONS_ADD").equalsIgnoreCase("0")) {
 				this.addPerms = config.getString("REWARD_PERMISSIONS_ADD").split(",");
 				this.apAdd = true;
 			} else {
 				this.apAdd = false;
 			}
 		} else {
 			QuestX.logError("Missing property 'REWARD_PERMISSIONS_ADD' in quest file for Quest " + this.questName);
 			this.apAdd = false;
 		}
 
 		if (config.doesKeyExist("REWARD_PERMISSIONS_REMOVE")) {
 			if (!config.getString("REWARD_PERMISSIONS_REMOVE").equalsIgnoreCase("0")) {
 				this.remPerms = config.getString("REWARD_PERMISSIONS_REMOVE").split(",");
 				this.apRem = true;
 			} else {
 				this.apRem = false;
 			}
 		} else {
 			QuestX.logError("Missing property 'REWARD_PERMISSIONS_REMOVE' in quest file for Quest " + this.questName);
 			this.apRem = false;
 		}
 
 		if (config.doesKeyExist("EXECUTE_PLAYER_CMD")) {
 			if (!config.getString("EXECUTE_PLAYER_CMD").equalsIgnoreCase("0")) {
 				this.playerCmds = config.getString("EXECUTE_PLAYER_CMD").split(",");
 				this.execPlayerCommand = true;
 			} else {
 				this.execPlayerCommand = false;
 			}
 		} else {
 			QuestX.logError("Missing property 'EXECUTE_PLAYER_CMD' in quest file for Quest " + this.questName);
 			this.execPlayerCommand = false;
 		}
 
 		if (config.doesKeyExist("FIREWORKS")) {
 			if (!config.getString("FIREWORKS").equalsIgnoreCase("0")) {
 				this.fireWorks = true;
 				String parts[] = config.getString("FIREWORKS").split(",");
 				int rad, sect;
 
 				try {
 					rad = Integer.parseInt(parts[0]);
 					sect = Integer.parseInt(parts[1]);
 				} catch (NumberFormatException nfe) {
 					this.fireWorks = false;
 					throw new InvalidQuestException(config.getString("FIREWORKS"), "Could not parse integer! Make sure it is a whole number and greater or equal to 0.", this.questName);
 				}
 
 				fwRadius = rad;
 				fwSectors = sect;
 			} else {
 				this.fireWorks = false;
 			}
 		} else {
 			QuestX.logError("Missing property 'FIREWORKS' in quest file for Quest " + this.questName);
 			this.fireWorks = false;
 		}
 
 		this.tasks = new QuestTask[i];
 		QuestX.logMSG("Loading Quest " + questName + " with " + this.nodes + " objectives.");
 		for (int c = 1; c <= this.nodes; c++) {
 			// load and parse string into a QuestTask object
 			String raw = this.config.getString(c + "");
 			String qtypeEnum = raw.substring(0, raw.indexOf(":"));
 			String dataString = raw.substring(raw.indexOf(":") + 1);
 			QuestX.logMSG("READING---------------");
 			QType qType = QType.parseType(qtypeEnum);
 			if (qType == null) {
 				throw new InvalidQuestException(raw, "QuestType was invalid! Got '" + qtypeEnum + "', expected (FETCH_ITEMS, KILL_ENTITIES, KILL_NPC OR TALK_NPC)", this.questName);
 			} else {
 				QuestX.logMSG("Quest Type = '" + qType.toString() + "'");
 			}
 			QuestX.logMSG("raw = " + raw);
 			QuestX.logMSG("qtypeEnum = " + qtypeEnum);
 			QuestX.logMSG("dataString = " + dataString);
 
 			try {
 				this.tasks[c - 1] = QuestTaskParser.getTaskObject(dataString, qType, this.questName);
 			} catch (InvalidKillTrackerException e) {
 				e.printCustomErrorReason(true, this.questName);
 			} catch (InvalidISAException e) {
 				e.printErrorReason();
 			}
 
 			// this.tasks[c - 1] = new
 			QuestX.logMSG("QUEST TASK LOAD LOOOP-----------");
 		}
 
 		QuestX.logMSG("QUEST LOAD COMPLETE");
 	}
 
 	public String getProgress(String player) {
 		return ("(" + this.playerProgress.get(player) + "/" + this.nodes + ")");
 	}
 
 	public boolean isExecutingPlayerCmds() {
 		return this.execPlayerCommand;
 	}
 
 	public boolean isExecutingServerCmds() {
 		return this.execServerCommand;
 	}
 
 	public String[] getServerCmds() {
 		return this.serverCmds;
 	}
 
 	public String[] getPlayerCmds() {
 		return this.playerCmds;
 	}
 
 	public boolean isAwardingAddPerms() {
 		return this.apAdd;
 	}
 
 	public boolean isAwardingRemPerms() {
 		return this.apRem;
 	}
 
 	public boolean isAwardingItems() {
 		return (this.rewardItems != null);
 	}
 
 	public String[] getAddPerms() {
 		return this.addPerms;
 	}
 
 	public String[] getRemPerms() {
 		return this.remPerms;
 	}
 
 	public boolean isAwardGold() {
 		return (this.rewardGold > 0);
 	}
 
 	public String getName() {
 		return this.questName;
 	}
 
 	public String getStartText() {
 		return this.startText;
 	}
 
 	public String getEndText() {
 		return this.endText;
 	}
 
 	public boolean isPlayerProgressLoaded(String p) {
 		return this.playerProgress.containsKey(p);
 	}
 
 	public synchronized void playerStartedQuest(String p) {
 		File cur = FileLocator.getCurrentQuestFile();
 		SyncConfiguration cfg = new SyncConfiguration(cur);
 		cfg.read();
 		cfg.MergeRWArrays();
 		cfg.add(p, this.getName());
 		cfg.write();
 
 		this.loadAndCheckPlayerProgress(p);
 		this.currentTask.put(p, this.tasks[0].getClonedInstance());// We only
 																	// want to
 																	// use the
 																	// initial
 																	// array for
 																	// refernce,
 																	// we don't
 																	// want to
 																	// change
 																	// anything
 	}
 
 	public int getRewardExp() {
 		return this.rewardExp;
 	}
 
 	public int getRewardRep() {
 		return this.rewardRep;
 	}
 
 	public int getRewardGold() {
 		return this.rewardGold;
 	}
 
 	public boolean isAwardExp() {
 		return (this.rewardExp > 0);
 	}
 
 	public boolean isAwardRep() {
 		return (this.rewardRep != 0);
 	}
 
 	public synchronized void awardPlayerOnQuestComplete(Player p) {
 		if (this.isAwardingItems()) {
 			ItemStack[] rewardItems = this.rewardItems;
 			for (ItemStack i : rewardItems) {
 				if (i != null) {
 					if ((p.getInventory().firstEmpty()) != -1) {
 						p.getInventory().addItem(i);
 					} else {
 						p.getWorld().dropItemNaturally(p.getLocation(), i);
 					}
 				}
 			}
 		}
 
 		if (this.isAwardExp()) {
 			for (int exp = 1; exp <= this.getRewardExp(); exp++) {
 				ExperienceOrb orb = p.getWorld().spawn(p.getLocation().add(0.5, 10, 0.5), ExperienceOrb.class);
 				orb.setExperience(1);
 			}
 		}
 
 		if (this.isAwardRep()) {
 
 			int awardRep = this.getRewardRep();
 			QuestX.logChat(p, "Trying to award rep = " + awardRep);
 			ReputationManager.updateReputation(p.getName(), awardRep);
 			// adjust rep
 		}
 
 		if (this.isAwardGold()) {
 			if (QuestX.economy.hasAccount(p.getName())) {
 				QuestX.economy.bankDeposit(p.getName(), this.getRewardGold());
 			} else {
 				// can't award, no account
 			}
 		}
 
 		if (this.isAwardingAddPerms()) {
 			for (String perm : this.getAddPerms()) {
 				QuestX.permission.playerAdd(p, perm);
 			}
 		}
 
 		if (this.isAwardingRemPerms()) {
 			for (String perm : this.getRemPerms()) {
 				if (QuestX.permission.has(p, perm)) {
 					QuestX.permission.playerRemove(p, perm);
 				}
 			}
 		}
 
 		if (this.isExecutingPlayerCmds()) {
 			QuestXCMDExecutor.executeAsPlayer(p.getName(), this.getPlayerCmds());
 		}
 
 		if (this.isExecutingServerCmds()) {
 			QuestXCMDExecutor.executeAsServer(this.getServerCmds());
 		}
 
 		if (this.fireWorks) {
 			Location pL = p.getLocation();
 			Fireworks display = new Fireworks(pL, fwRadius, fwSectors);
 			display.circularDisplay();
 		}
 
 		File cur = FileLocator.getCurrentQuestFile();
 		SyncConfiguration cfg = new SyncConfiguration(cur);
 		cfg.read();
 		cfg.MergeRWArrays();
 		cfg.getWriteableData().remove(p.getName());
 		cfg.write();
 	}
 
 	public void loadAndCheckPlayerProgress(String p) {
 		File f = FileLocator.getQuestProgressionPlayerFile(questName, p);
 		SyncConfiguration c = new SyncConfiguration(f);
 		if (f.exists()) {
 			c.read();
 			this.playerProgress.put(p, c.getInt("P"));
 			QuestX.logMSG("'" + p + "' progress loaded = " + c.getInt("P"));
 		} else {
 			c.createFileIfNeeded();
 			c.add("P", 1);
 			c.write();
 			this.playerProgress.put(p, 1);
 		}
 	}
 
 	public QuestTask getPlayerQuestTask(String p) {
 		QuestX.logMSG("Logging player progress = " + this.playerProgress.get(p));
 		return this.currentTask.get(p);
 	}
 
 	public int getCurrentQuestNode(String p) {
 		return this.playerProgress.get(p);
 	}
 
 	void setPlayerTask(String p) {
 		this.currentTask.put(p, this.tasks[this.playerProgress.get(p) - 1].getClonedInstance());
 	}
 
 	public synchronized void setTaskComplete(Player player) {
 		this.incrementTaskProgress(player);
 		if (player != null) {
 			QuestX.logChat(player, "Quest task completed!");
 		}
 	}
 
 	public boolean isQuestComplete(String player) {
 		return this.playerProgress.get(player) > this.nodes;
 	}
 
 	@SuppressWarnings("deprecation")
 	public void incrementTaskProgress(Player p) {
 		QuestTask qt = this.currentTask.get(p.getName());
 
 		if (qt.isItemStacks()) {
 			QuestX.logMSG("Items do need to be removed...");
 			qt.removeItems(p);
 			p.updateInventory();
 		}
 
 		int current = this.playerProgress.get(p.getName()) + 1;
 		this.playerProgress.put(p.getName(), current);
 		if (current <= +this.nodes) {
 			this.currentTask.put(p.getName(), this.tasks[current - 1].getClonedInstance());
 		} else {
 			this.awardPlayerOnQuestComplete(p);
 		}
 		SyncConfiguration c = new SyncConfiguration(FileLocator.getQuestProgressionPlayerFile(questName, p.getName()));
 		c.add("P", current);
 		c.write();
 
 	}
 
 }
