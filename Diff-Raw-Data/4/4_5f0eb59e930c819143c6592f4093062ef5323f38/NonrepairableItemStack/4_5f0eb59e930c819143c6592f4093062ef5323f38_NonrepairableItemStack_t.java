 package net.nunnerycode.bukkit.mythicdrops.api.items;
 
 import org.bukkit.Material;
 import org.bukkit.enchantments.Enchantment;
 import org.bukkit.inventory.meta.Repairable;
 
 import java.util.ArrayList;
 import java.util.HashMap;
 import java.util.List;
 import java.util.Map;
 
 /**
  * An ItemStack that cannot be repaired without using commands.
  */
 public class NonrepairableItemStack extends MythicItemStack {
 
 	public static final int DEFAULT_COST = 1000;
 
 	/**
 	 * Instantiates an ItemStack with a display name and a cost for repairing.
 	 *
 	 * @param type        material
 	 * @param amount      amount
 	 * @param durability  damage / durability
 	 * @param displayName name of item
 	 * @param cost        experience level cost to repair
 	 */
 	public NonrepairableItemStack(Material type, int amount, short durability, String displayName, int cost) {
 		this(type, amount, durability, displayName, new ArrayList<String>(), new HashMap<Enchantment, Integer>(),
 				cost);
 	}
 
 	/**
 	 * Instantiates an ItemStack with a display name, lore, enchantments, and a cost for repairing.
 	 *
 	 * @param type         material
 	 * @param amount       amount
 	 * @param durability   damage / durability
 	 * @param displayName  name of item
 	 * @param lore         lore for item
 	 * @param enchantments enchantments for item
 	 * @param cost         experience level cost to repair
 	 */
 	public NonrepairableItemStack(Material type, int amount, short durability, String displayName, List<String> lore,
 								  Map<Enchantment, Integer> enchantments, int cost) {
 		super(type, amount, durability, displayName, lore, enchantments);
		if (this.getItemMeta() instanceof Repairable) {
			((Repairable) this.getItemMeta()).setRepairCost(cost);
 		}
 	}
 
 	/**
 	 * Instantiates an ItemStack with lore and a cost to repair.
 	 *
 	 * @param type       material
 	 * @param amount     amount
 	 * @param durability damage / durability
 	 * @param lore       lore for item
 	 * @param cost       experience level cost to repair
 	 */
 	public NonrepairableItemStack(Material type, int amount, short durability, List<String> lore, int cost) {
 		this(type, amount, durability, null, lore, new HashMap<Enchantment, Integer>(), cost);
 	}
 
 	/**
 	 * Instantiates an ItemStack with enchantments and a cost to repair.
 	 *
 	 * @param type         material
 	 * @param amount       amount
 	 * @param durability   damage / durability
 	 * @param enchantments enchantments for item
 	 * @param cost         experience level cost to repair
 	 */
 	public NonrepairableItemStack(Material type, int amount, short durability, Map<Enchantment,
 			Integer> enchantments, int cost) {
 		this(type, amount, durability, null, new ArrayList<String>(), enchantments, cost);
 	}
 
 	/**
 	 * Instantiates an ItemStack with a display name, lore, and a cost to repair.
 	 *
 	 * @param type        material
 	 * @param amount      amount
 	 * @param durability  damage / durability
 	 * @param displayName name of item
 	 * @param lore        lore for item
 	 * @param cost        experience level cost to repair
 	 */
 	public NonrepairableItemStack(Material type, int amount, short durability, String displayName, List<String> lore,
 								  int cost) {
 		this(type, amount, durability, displayName, lore, new HashMap<Enchantment, Integer>(), cost);
 	}
 
 	/**
 	 * Instantiates an ItemStack with a display name, enchantments, and a cost to repair.
 	 *
 	 * @param type         material
 	 * @param amount       amount
 	 * @param durability   damage / durability
 	 * @param displayName  name of item
 	 * @param enchantments enchantments for item
 	 * @param cost         experience level cost to repair
 	 */
 	public NonrepairableItemStack(Material type, int amount, short durability, String displayName, Map<Enchantment,
 			Integer> enchantments, int cost) {
 		this(type, amount, durability, displayName, new ArrayList<String>(), enchantments, cost);
 	}
 
 	/**
 	 * Instantiates an ItemStack with lore, enchantments, and a cost to repair.
 	 *
 	 * @param type         material
 	 * @param amount       amount
 	 * @param durability   damage / durability
 	 * @param lore         lore for item
 	 * @param enchantments enchantments for item
 	 * @param cost         experience level cost to repair
 	 */
 	public NonrepairableItemStack(Material type, int amount, short durability, List<String> lore, Map<Enchantment,
 			Integer> enchantments, int cost) {
 		this(type, amount, durability, null, lore, enchantments, cost);
 	}
 
 	/**
 	 * Instantiates an ItemStack with a display name.
 	 *
 	 * @param type        material
 	 * @param amount      amount
 	 * @param durability  damage / durability
 	 * @param displayName name of item
 	 */
 	public NonrepairableItemStack(Material type, int amount, short durability, String displayName) {
 		this(type, amount, durability, displayName, new ArrayList<String>(), new HashMap<Enchantment, Integer>(),
 				DEFAULT_COST);
 	}
 
 	/**
 	 * Instantiates an ItemStack with a display name, lore, and enchantments.
 	 *
 	 * @param type         material
 	 * @param amount       amount
 	 * @param durability   damage / durability
 	 * @param displayName  name of item
 	 * @param lore         lore for item
 	 * @param enchantments enchantments for item
 	 */
 	public NonrepairableItemStack(Material type, int amount, short durability, String displayName, List<String> lore,
 								  Map<Enchantment, Integer> enchantments) {
 		this(type, amount, durability, displayName, lore, enchantments, DEFAULT_COST);
 	}
 
 	/**
 	 * Instantiates an ItemStack with lore.
 	 *
 	 * @param type       material
 	 * @param amount     amount
 	 * @param durability damage / durability
 	 * @param lore       lore for item
 	 */
 	public NonrepairableItemStack(Material type, int amount, short durability, List<String> lore) {
 		this(type, amount, durability, null, lore, new HashMap<Enchantment, Integer>(), DEFAULT_COST);
 	}
 
 	/**
 	 * Instantiates an ItemStack with enchantments.
 	 *
 	 * @param type         material
 	 * @param amount       amount
 	 * @param durability   damage / durability
 	 * @param enchantments enchantments for item
 	 */
 	public NonrepairableItemStack(Material type, int amount, short durability, Map<Enchantment,
 			Integer> enchantments) {
 		this(type, amount, durability, null, new ArrayList<String>(), enchantments, DEFAULT_COST);
 	}
 
 	/**
 	 * Instantiates an ItemStack with a display name and lore.
 	 *
 	 * @param type        material
 	 * @param amount      amount
 	 * @param durability  damage / durability
 	 * @param displayName name of item
 	 * @param lore        lore for item
 	 */
 	public NonrepairableItemStack(Material type, int amount, short durability, String displayName, List<String> lore) {
 		this(type, amount, durability, displayName, lore, new HashMap<Enchantment, Integer>(), DEFAULT_COST);
 	}
 
 	/**
 	 * Instantiates an ItemStack with a display name and enchantments.
 	 *
 	 * @param type         material
 	 * @param amount       amount
 	 * @param durability   damage / durability
 	 * @param displayName  name of item
 	 * @param enchantments enchantments for item
 	 */
 	public NonrepairableItemStack(Material type, int amount, short durability, String displayName, Map<Enchantment,
 			Integer> enchantments) {
 		this(type, amount, durability, displayName, new ArrayList<String>(), enchantments, DEFAULT_COST);
 	}
 
 	/**
 	 * Instantiates an ItemStack with lore and enchantments.
 	 *
 	 * @param type         material
 	 * @param amount       amount
 	 * @param durability   damage / durability
 	 * @param lore         lore for item
 	 * @param enchantments enchantments for item
 	 */
 	public NonrepairableItemStack(Material type, int amount, short durability, List<String> lore, Map<Enchantment,
 			Integer> enchantments) {
 		this(type, amount, durability, null, lore, enchantments, DEFAULT_COST);
 	}
 
 }
