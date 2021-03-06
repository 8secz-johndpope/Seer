 package com.fullwall.Citizens.NPCTypes.Bandits;
 
import java.util.Random;
 import java.util.Map.Entry;
 
 import org.bukkit.Bukkit;
 import org.bukkit.ChatColor;
 import org.bukkit.entity.Player;
 import org.bukkit.inventory.ItemStack;
 
 import com.fullwall.Citizens.Citizens;
 import com.fullwall.Citizens.Constants;
 import com.fullwall.Citizens.NPCs.NPCManager;
 import com.fullwall.Citizens.Utils.ActionManager;
 import com.fullwall.Citizens.Utils.CachedAction;
 import com.fullwall.Citizens.Utils.LocationUtils;
 import com.fullwall.Citizens.Utils.MessageUtils;
 import com.fullwall.Citizens.Utils.StringUtils;
 import com.fullwall.resources.redecouverte.NPClib.HumanNPC;
 
 public class BanditTask implements Runnable {
 	@SuppressWarnings("unused")
	private Citizens plugin;
 
 	public BanditTask(Citizens plugin) {
 		this.plugin = plugin;
 	}
 
 	@Override
 	public void run() {
 		HumanNPC npc;
 		int UID;
 		Player[] online = Bukkit.getServer().getOnlinePlayers();
 		for (Entry<Integer, HumanNPC> entry : NPCManager.getList().entrySet()) {
 			{
 				npc = entry.getValue();
 				npc.updateMovement();
 				UID = entry.getKey();
 				for (Player p : online) {
 					String name = p.getName();
 					if (LocationUtils.checkLocation(npc.getLocation(),
 							p.getLocation(), Constants.banditStealRadius)) {
 						cacheActions(p, npc, UID, name);
 					} else {
 						resetActions(UID, name, npc);
 					}
 				}
 			}
 		}
 	}
 
 	private void resetActions(int entityID, String name, HumanNPC npc) {
 		ActionManager.resetAction(entityID, name, "takenItem", npc.isBandit());
 	}
 
 	private void cacheActions(Player p, HumanNPC npc, int entityID, String name) {
 		CachedAction cached = ActionManager.getAction(entityID, name);
 		if (!cached.has("takenItem") && npc.isBandit()) {
 			stealItem(p, npc);
 			cached.set("takenItem");
 		}
 		ActionManager.putAction(entityID, name, cached);
 	}
 
 	/**
 	 * Steal an item from a player's inventory and put it in a bandit's
 	 * inventory
 	 * 
 	 * @param player
 	 * @param npc
 	 */
 	private void stealItem(Player player, HumanNPC npc) {
 		Random random = new Random();
 		int randomSlot;
 		int count = 0;
 		ItemStack item = null;
 		if (npc.isBandit()) {
 			if (!NPCManager.validateOwnership(player, npc.getUID())) {
 				int limit = player.getInventory().getSize();
 				while (true) {
 					randomSlot = random.nextInt(limit);
 					item = player.getInventory().getItem(randomSlot);
 					if (item != null) {
 						if (npc.getBandit().getStealables()
 								.contains(item.getTypeId())) {
							player.getInventory().removeItem(item);
 							player.sendMessage(StringUtils.wrap(
 									npc.getStrippedName(), ChatColor.RED)
 									+ " has stolen "
 									+ StringUtils.wrap(
 											MessageUtils.getStackString(item),
 											ChatColor.RED)
 									+ " from your inventory!");
 							// may want to check if this returns a non-empty
 							// hashmap (bandit didn't have enough room).
 							npc.getInventory().addItem(item);
 						}
 						break;
 					} else {
 						if (count >= limit) {
 							break;
 						}
 						count += 1;
 					}
 				}
 			}
 		}
 	}
 }
