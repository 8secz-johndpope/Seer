 package org.github.craftfortress2;
 import org.bukkit.Material;
 import org.bukkit.entity.Player;
 import org.bukkit.potion.*;
 import org.bukkit.inventory.*;
public class Scout extends CFClasses {
 	public static void init(Player player){
 		PlayerInventory inv = player.getInventory();
		saveInv(player);
 		inv.clear();
 		player.setFoodLevel(17);
 		player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 99999999, 1));
 		inv.setHelmet(new ItemStack(Material.IRON_HELMET, 1));
 		inv.setBoots(new ItemStack(Material.IRON_BOOTS, 1));
		inv.setItem(inv.getHeldItemSlot(), new ItemStack(Material.STICK, 1));
 	}
 }
