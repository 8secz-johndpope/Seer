 package com.fullwall.Citizens.Economy;
 
 import org.bukkit.entity.Player;
 
 import com.fullwall.Citizens.Utils.PropertyPool;
 
 public class EconomyHandler {
 	private static boolean useEconomy = true;
 	private static boolean iConomyEnabled = false;
 	private static boolean useiConomy = false;
 
 	public enum Operation {
 		BASIC_NPC_CREATE
 	}
 
 	public static void setiConomyEnable(boolean value) {
 		iConomyEnabled = value;
 	}
 
 	public static void setUpVariables() {
 		useEconomy = PropertyPool.checkEconomyEnabled();
 		useiConomy = PropertyPool.checkiConomyEnabled();
 	}
 
 	public static boolean canBuy(Operation op, Player player) {
 		if (useEconomy) {
 			if (useiConomy && iConomyEnabled)
 				return IconomyInterface.hasEnough(player, op);
 			else
 				return ItemInterface.hasEnough(player, op);
 		} else
 			return true;
 	}
 
 	public static boolean useEconomy() {
 		return useEconomy;
 	}
 
 	public static int pay(Operation op, Player player) {
		if (useEconomy) {
			if (iConomyEnabled && useiConomy)
				return IconomyInterface.pay(player, op);
			else
				return ItemInterface.pay(player, op);
		} else
			return 0;
 	}
 
 	public static String getPaymentType(Operation op) {
		if (useEconomy) {
			if (iConomyEnabled && useiConomy)
				return IconomyInterface.getCurrency();
			else
				return ItemInterface.getCurrency(op);
		} else
			return "None";
 	}
 
 	public static String getRemainder(Operation op, Player player) {
		if (useEconomy) {
			if (iConomyEnabled && useiConomy)
				return IconomyInterface.getRemainder(op, player);
			else
				return ItemInterface.getRemainder(op, player);
		} else
			return "0";
 	}
 }
