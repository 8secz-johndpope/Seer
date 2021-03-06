 package com.github.peter200lx.toolbelt.tool;
 
 import java.util.HashMap;
 import java.util.List;
 import java.util.Map;
 import java.util.Map.Entry;
 
 import org.bukkit.ChatColor;
 import org.bukkit.Material;
 import org.bukkit.block.Block;
 import org.bukkit.command.CommandSender;
 import org.bukkit.configuration.ConfigurationSection;
 import org.bukkit.entity.Player;
 import org.bukkit.event.block.Action;
 import org.bukkit.event.player.PlayerInteractEvent;
 import org.bukkit.inventory.ItemStack;
 
 import com.github.peter200lx.toolbelt.GlobalConf;
 import com.github.peter200lx.toolbelt.PrintEnum;
 import com.github.peter200lx.toolbelt.SetMat;
 import com.github.peter200lx.toolbelt.AbstractTool;
 
 public class Duplicator extends AbstractTool {
 
 	public Duplicator(GlobalConf gc) {
 		super(gc);
 	}
 
 	public static final String NAME = "dupe";
 
 	private Map<Material, Material> dupeMap;
 
 	private final SetMat keepData = new SetMat(log, gc.modName, "keepData");
 
 	@Override
 	public String getToolName() {
 		return NAME;
 	}
 
 	@Override
 	@SuppressWarnings("deprecation")
 	// TODO Investigate replacement .updateInventory()
 	public void handleInteract(PlayerInteractEvent event) {
 		final Player subject = event.getPlayer();
 		if (!delayElapsed(subject.getName())) {
 			return;
 		}
 
 		if (event.getAction().equals(Action.RIGHT_CLICK_BLOCK)) {
 			final Block clicked = event.getClickedBlock();
 			final Material type = clicked.getType();
 
 			Material toUse = dupeMap.get(type);
 			if (toUse == null) {
 				toUse = type;
 			}
 			if (toUse == Material.AIR) {
 				uPrint(PrintEnum.WARN, subject, ChatColor.RED + "Duplicating "
 						+ ChatColor.GOLD + type.toString()
 						+ ChatColor.RED + " is disabled");
 				return;
 			}
 
			if (clicked.getType().equals(Material.DOUBLE_PLANT)
					&& ((clicked.getData() & 0x8) == 0x8)) {
				uPrint(PrintEnum.HINT, subject, ChatColor.RED + "Duplicating "
						+ "the top half of DOUBLE_PLANT is not supported");
				return;
			}

 			if ((clicked.getData() != 0) && (keepData.contains(toUse))
 					&& (type.equals(toUse)
 					|| type.equals(Material.WOOL)
 							&& toUse.equals(Material.INK_SACK)
 					|| type.equals(Material.STEP)
 							&& toUse.equals(Material.DOUBLE_STEP)
 					|| type.equals(Material.DOUBLE_STEP)
 							&& toUse.equals(Material.STEP)
 					|| type.equals(Material.LOG)
 							&& toUse.equals(Material.LEAVES)
 					|| type.equals(Material.LOG)
 							&& toUse.equals(Material.SAPLING)
 					|| type.equals(Material.LEAVES)
 							&& toUse.equals(Material.LOG)
 					|| type.equals(Material.LEAVES)
 							&& toUse.equals(Material.SAPLING))) {
 				subject.getInventory().addItem(
 						new ItemStack(toUse, 64, (short) 0, clicked.getData()));
 			} else {
 				subject.getInventory().addItem(new ItemStack(toUse, 64));
 			}
 			subject.updateInventory();
 			if (printData.contains(toUse)) {
 				uPrint(PrintEnum.INFO, subject, ChatColor.GREEN + "Enjoy your "
 						+ ChatColor.GOLD + toUse.toString() + ChatColor.WHITE
 						+ ":" + ChatColor.BLUE
 						+ data2Str(clicked.getState().getData()));
 			} else {
 				uPrint(PrintEnum.INFO, subject, ChatColor.GREEN + "Enjoy your "
 						+ ChatColor.GOLD + toUse.toString());
 			}
 		}
 	}
 
 	@Override
 	public boolean printUse(CommandSender sender) {
 		if (hasPerm(sender)) {
 			uPrint(PrintEnum.CMD, sender, useFormat(
 					"Right click to duplicate the item selected"));
 			return true;
 		}
 		return false;
 	}
 
 	@Override
 	public boolean loadConf(String tSet, ConfigurationSection conf) {
 
 		// Load the repeat delay
 		if (!loadRepeatDelay(tSet, conf, 0)) {
 			return false;
 		}
 
 		final ConfigurationSection sect = conf.getConfigurationSection(
 				tSet + "." + NAME + ".replace");
 
 		if (sect == null) {
 			log.warning("[" + gc.modName + "] " + tSet + "." + NAME
 					+ ".replace is returning null");
 			return false;
 		}
 
 		final Map<Material, Material> holdDupeMap = defDupeMap();
 		for (Entry<String, Object> entry : sect.getValues(false).entrySet()) {
 			try {
 				final int key = Integer.parseInt(entry.getKey());
 				if (entry.getValue() instanceof Number) {
 					final int val = ((Number) entry.getValue()).intValue();
 					if ((key > 0) && (val >= 0)) {
 						final Material keyType = Material.getMaterial(key);
 						final Material valType = Material.getMaterial(val);
 						if ((keyType != null) && (valType != null)) {
 							holdDupeMap.put(keyType, valType);
 							if (isDebug()) {
 								log.info("[" + gc.modName + "][loadConf] added"
 										+ " to dupeMap: " + keyType + " to "
 										+ valType);
 							}
 							continue;
 						}
 					}
 				}
 				log.warning("[" + gc.modName + "] " + tSet + "." + NAME
 						+ ".replace: '" + entry.getKey() + "': '"
 						+ entry.getValue() + "' is not a Material type");
 				return false;
 			} catch (NumberFormatException e) {
 				log.warning("[" + gc.modName + "] " + tSet + "." + NAME
 						+ ".replace: '" + entry.getKey()
 						+ "': is not an integer");
 				return false;
 			}
 		}
 		dupeMap = holdDupeMap;
 
 		final List<Integer> intL = conf.getIntegerList(tSet + "." + NAME
 				+ ".keepData");
 
 		if (!keepData.loadMatList(intL, false, tSet + "." + NAME)) {
 			return false;
 		}
 
 		if (isDebug()) {
 			keepData.logMatSet("loadConf", NAME);
 		}
 
 		return true;
 	}
 
 	private Map<Material, Material> defDupeMap() {
 		final Map<Material, Material> dm = new HashMap<Material, Material>();
 		// What about Material.GLOWING_REDSTONE_ORE ? It is safe to place
 		// TODO Investigate (Stationary)Water/Lava
 		// Material.STATIONARY_LAVA    Material.STATIONARY_WATER
 		// Material.LAVA               Material.WATER
 		dm.put(Material.BED_BLOCK, Material.BED);
 		dm.put(Material.PISTON_EXTENSION, Material.PISTON_BASE);
 		dm.put(Material.PISTON_MOVING_PIECE, Material.PISTON_BASE);
 		// Material.DOUBLE_STEP This is fine for someone to have
 		// Can anyone even click on Material.FIRE ? No
 		// Do we want to block Material.MOB_SPAWNER ?
 		dm.put(Material.REDSTONE_WIRE, Material.REDSTONE);
 		// Do we want to block Material.SOIL ?
 		dm.put(Material.SIGN_POST, Material.SIGN);
 		dm.put(Material.WOODEN_DOOR, Material.WOOD_DOOR);
 		dm.put(Material.WALL_SIGN, Material.SIGN);
 		dm.put(Material.IRON_DOOR_BLOCK, Material.IRON_DOOR);
 		dm.put(Material.REDSTONE_TORCH_OFF, Material.REDSTONE_TORCH_ON);
 		dm.put(Material.SUGAR_CANE_BLOCK, Material.SUGAR_CANE);
 		// Do we want to block Material.PORTAL ?
 		dm.put(Material.CAKE_BLOCK, Material.CAKE);
 		dm.put(Material.DIODE_BLOCK_OFF, Material.DIODE);
 		dm.put(Material.DIODE_BLOCK_ON, Material.DIODE);
 		dm.put(Material.LOCKED_CHEST, Material.CHEST);
 		// Do we want to block Material.NETHER_WARTS ?
 		dm.put(Material.BREWING_STAND, Material.BREWING_STAND_ITEM);
 		dm.put(Material.CAULDRON, Material.CAULDRON_ITEM);
 		// Can anyone even click Material.ENDER_PORTAL ?
 		// Do we want to block Material.ENDER_PORTAL_FRAME ?
 		dm.put(Material.REDSTONE_LAMP_ON, Material.REDSTONE_LAMP_OFF);
 		dm.put(Material.TRIPWIRE, Material.STRING);
 		dm.put(Material.FLOWER_POT, Material.FLOWER_POT_ITEM);
 		dm.put(Material.SKULL, Material.SKULL_ITEM);
 		dm.put(Material.REDSTONE_COMPARATOR_OFF, Material.REDSTONE_COMPARATOR);
 		dm.put(Material.REDSTONE_COMPARATOR_ON, Material.REDSTONE_COMPARATOR);
 		return dm;
 	}
 
 }
