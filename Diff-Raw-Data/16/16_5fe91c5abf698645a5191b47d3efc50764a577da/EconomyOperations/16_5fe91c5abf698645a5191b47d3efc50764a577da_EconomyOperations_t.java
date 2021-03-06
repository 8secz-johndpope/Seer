 package com.drtshock.playervaults.util;
 
 import net.milkbowl.vault.economy.EconomyResponse;
 
 import org.bukkit.configuration.file.FileConfiguration;
 import org.bukkit.entity.Player;
 
 import com.drtshock.playervaults.Main;
 
 public class EconomyOperations {
 
 	public Main plugin;
 	private static FileConfiguration config;
 
 	public EconomyOperations(Main instance) {
 		this.plugin = instance;
 		EconomyOperations.config = plugin.getConfig();
 	}
 
 	/**
 	 * Have a player pay to open a vault.
 	 * Returns true if successful. Otherwise false.
 	 * @param player
 	 * @return transaction success
 	 */
 	public static boolean payToOpen(Player player) {
		if(!config.getBoolean("economy.enabled") || player.hasPermission("playervaults.free") || !Main.useVault)
 			return true;
 
 		double cost = config.getDouble("economy.cost-to-open");
 		EconomyResponse resp = Main.econ.withdrawPlayer(player.getName(), cost);
 		if(resp.transactionSuccess()) {
 			player.sendMessage(Lang.TITLE.toString() + Lang.COST_TO_OPEN.toString());
 			return true;
 		}
 
 		return false;
 	}
 
 	/**
 	 * Have a player pay to make a vault.
 	 * Returns true if successful. Otherwise false.
 	 * @param player
 	 * @return transaction success
 	 */
 	public static boolean payToMake(Player player) {
		if(!config.getBoolean("economy.enabled") || player.hasPermission("playervaults.free") || !Main.useVault)
 			return true;
 
 		double cost = config.getDouble("economy.cost-to-create");
 		EconomyResponse resp = Main.econ.withdrawPlayer(player.getName(), cost);
 		if(resp.transactionSuccess()) {
 			player.sendMessage(Lang.TITLE.toString() + Lang.COST_TO_CREATE.toString());
 			return true;
 		}
 
 		return false;
 	}
 
 	/**
 	 * Have a player get his money back when vault is deleted.
 	 * Returns true if successful. Otherwise false.
 	 * @param player
 	 * @return transaction success.
 	 */
 	public static boolean refundOnDelete(Player player) {
		if(!config.getBoolean("economy.enabled") || player.hasPermission("playervaults.free") || !Main.useVault)
 			return true;
 
 		double cost = config.getDouble("economy.refund-on-delete");
 		EconomyResponse resp = Main.econ.depositPlayer(player.getName(), cost);
 		if(resp.transactionSuccess()) {
 			player.sendMessage(Lang.TITLE.toString() + Lang.REFUND_AMOUNT.toString());
 			return true;
 		}
 
 		return false;
 	}
 
 }
