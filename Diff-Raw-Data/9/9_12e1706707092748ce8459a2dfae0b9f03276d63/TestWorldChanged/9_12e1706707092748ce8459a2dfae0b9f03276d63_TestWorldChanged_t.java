 package com.onarandombox.multiverseinventories;
 
 import com.onarandombox.multiverseinventories.api.DataStrings;
 import com.onarandombox.multiverseinventories.api.profile.ProfileType;
 import com.onarandombox.multiverseinventories.share.Sharables;
 import com.onarandombox.multiverseinventories.util.TestInstanceCreator;
 import com.onarandombox.multiverseinventories.util.data.FlatFileDataHelper;
 import junit.framework.Assert;
 import org.bukkit.Location;
 import org.bukkit.Material;
 import org.bukkit.Server;
 import org.bukkit.command.Command;
 import org.bukkit.command.CommandSender;
 import org.bukkit.configuration.file.FileConfiguration;
 import org.bukkit.configuration.file.YamlConfiguration;
 import org.bukkit.entity.Player;
 import org.bukkit.event.player.PlayerChangedWorldEvent;
 import org.bukkit.inventory.ItemStack;
 import org.bukkit.inventory.PlayerInventory;
 import org.bukkit.plugin.Plugin;
 import org.bukkit.plugin.PluginDescriptionFile;
 import org.junit.After;
 import org.junit.Before;
 import org.junit.Test;
 import org.junit.runner.RunWith;
 import org.powermock.core.classloader.annotations.PrepareForTest;
 import org.powermock.modules.junit4.PowerMockRunner;
 
 import java.io.File;
 import java.io.IOException;
 import java.lang.reflect.Field;
 import java.util.HashMap;
 import java.util.Map;
 
 import static org.junit.Assert.assertNotNull;
 import static org.junit.Assert.assertTrue;
 import static org.mockito.Mockito.mock;
 import static org.mockito.Mockito.when;
 
 @RunWith(PowerMockRunner.class)
 @PrepareForTest({MultiverseInventories.class, PluginDescriptionFile.class})
 public class TestWorldChanged {
     TestInstanceCreator creator;
     Server mockServer;
     CommandSender mockCommandSender;
     MultiverseInventories inventories;
     InventoriesListener listener;
 
     @Before
     public void setUp() throws Exception {
         creator = new TestInstanceCreator();
         assertTrue(creator.setUp());
         mockServer = creator.getServer();
         mockCommandSender = creator.getCommandSender();
         // Pull a core instance from the server.
         Plugin plugin = mockServer.getPluginManager().getPlugin("Multiverse-Inventories");
         // Make sure Core is not null
         assertNotNull(plugin);
         inventories = (MultiverseInventories) plugin;
         Field field = MultiverseInventories.class.getDeclaredField("inventoriesListener");
         field.setAccessible(true);
         listener = (InventoriesListener) field.get(inventories);
         // Make sure Core is enabled
         assertTrue(inventories.isEnabled());
 
 
     }
 
     @After
     public void tearDown() throws Exception {
         creator.tearDown();
     }
 
     public void changeWorld(Player player, String fromWorld, String toWorld) {
         Location location = new Location(mockServer.getWorld(toWorld), 0.0, 70.0, 0.0);
         player.teleport(location);
         Assert.assertEquals(location, player.getLocation());
         listener.playerChangedWorld(new PlayerChangedWorldEvent(player, mockServer.getWorld(fromWorld)));
     }
 
     public void addToInventory(PlayerInventory inventory, Map<Integer, ItemStack> items) {
         for (Map.Entry<Integer, ItemStack> invEntry : items.entrySet()) {
             inventory.setItem(invEntry.getKey(), invEntry.getValue());
         }
     }
 
     @Test
     public void testBasicWorldChange() {
 
         // Initialize a fake command
         Command mockCommand = mock(Command.class);
         when(mockCommand.getName()).thenReturn("mvinv");
 
         // Assert debug mode is off
         Assert.assertEquals(0, inventories.getMVIConfig().getGlobalDebug());
 
         // Send the debug command.
         String[] cmdArgs = new String[]{"debug", "3"};
         inventories.onCommand(mockCommandSender, mockCommand, "", cmdArgs);
 
         // remove world2 from default group
         cmdArgs = new String[]{"rmworld", "world2", "default"};
         inventories.onCommand(mockCommandSender, mockCommand, "", cmdArgs);
 
         // Verify removal
         Assert.assertTrue(!inventories.getGroupManager().getDefaultGroup().getWorlds().contains("world2"));
         cmdArgs = new String[]{"info", "default"};
         inventories.onCommand(mockCommandSender, mockCommand, "", cmdArgs);
 
         Assert.assertEquals(3, inventories.getMVIConfig().getGlobalDebug());
 
         Player player = inventories.getServer().getPlayer("dumptruckman");
 
         Map<Integer, ItemStack> fillerItems = new HashMap<Integer, ItemStack>();
         fillerItems.put(3, new ItemStack(Material.BOW, 1));
         fillerItems.put(13, new ItemStack(Material.DIRT, 64));
         fillerItems.put(36, new ItemStack(Material.IRON_HELMET, 1));
         addToInventory(player.getInventory(), fillerItems);
         String originalInventory = player.getInventory().toString();
 
         changeWorld(player, "world", "world_nether");
 
         String newInventory = player.getInventory().toString();
         Assert.assertEquals(originalInventory, newInventory);
 
         changeWorld(player, "world_nether", "world2");
 
         Assert.assertNotSame(originalInventory, newInventory);
     }
 
     @Test
     public void testGroupedSharesWorldChange() {
 
         // Initialize a fake command
         Command mockCommand = mock(Command.class);
         when(mockCommand.getName()).thenReturn("mvinv");
 
         // Assert debug mode is off
         Assert.assertEquals(0, inventories.getMVIConfig().getGlobalDebug());
 
         // Send the debug command.
         String[] cmdArgs = new String[]{"debug", "3"};
         inventories.onCommand(mockCommandSender, mockCommand, "", cmdArgs);
 
         // remove world2 from default group
         cmdArgs = new String[]{"rmworld", "world2", "default"};
         inventories.onCommand(mockCommandSender, mockCommand, "", cmdArgs);
 
         // remove all shares from default group
         cmdArgs = new String[]{"rmshares", "all", "default"};
         inventories.onCommand(mockCommandSender, mockCommand, "", cmdArgs);
         // add inventory shares (inv and armor) to default group
         cmdArgs = new String[]{"addshares", "inventory", "default"};
         inventories.onCommand(mockCommandSender, mockCommand, "", cmdArgs);

         Assert.assertFalse(inventories.getGroupManager().getDefaultGroup().getShares().isSharing(Sharables.all()));
         Assert.assertTrue(inventories.getGroupManager().getDefaultGroup().getShares().isSharing(Sharables.INVENTORY));
         Assert.assertTrue(inventories.getGroupManager().getDefaultGroup().getShares().isSharing(Sharables.ARMOR));
 
         // Reload to ensure things are saving to config.yml
         cmdArgs = new String[]{"reload"};
         inventories.onCommand(mockCommandSender, mockCommand, "", cmdArgs);
 
         Assert.assertFalse(inventories.getGroupManager().getDefaultGroup().getShares().isSharing(Sharables.all()));
         Assert.assertTrue(inventories.getGroupManager().getDefaultGroup().getShares().isSharing(Sharables.INVENTORY));
         Assert.assertTrue(inventories.getGroupManager().getDefaultGroup().getShares().isSharing(Sharables.ARMOR));
 
         // Verify removal
         Assert.assertTrue(!inventories.getGroupManager().getDefaultGroup().getWorlds().contains("world2"));
         cmdArgs = new String[]{"info", "default"};
         inventories.onCommand(mockCommandSender, mockCommand, "", cmdArgs);
 
         Assert.assertEquals(3, inventories.getMVIConfig().getGlobalDebug());
 
         Player player = inventories.getServer().getPlayer("dumptruckman");
 
         Map<Integer, ItemStack> fillerItems = new HashMap<Integer, ItemStack>();
         fillerItems.put(3, new ItemStack(Material.BOW, 1));
         fillerItems.put(13, new ItemStack(Material.DIRT, 64));
         fillerItems.put(36, new ItemStack(Material.IRON_HELMET, 1));
         addToInventory(player.getInventory(), fillerItems);
         String originalInventory = player.getInventory().toString();
 
         changeWorld(player, "world", "world_nether");
 
         String newInventory = player.getInventory().toString();
         Assert.assertEquals(originalInventory, newInventory);
 
         changeWorld(player, "world_nether", "world2");
         newInventory = player.getInventory().toString();
 
         Assert.assertNotSame(originalInventory, newInventory);
 
         FlatFileDataHelper dataHelper = new FlatFileDataHelper(inventories.getData());
         File playerFile = dataHelper.getPlayerFile(ProfileType.GROUP, "default", "dumptruckman");
         FileConfiguration playerConfig = YamlConfiguration.loadConfiguration(playerFile);
         playerConfig.set(DataStrings.PLAYER_INVENTORY_CONTENTS, "");
         playerConfig.set(DataStrings.PLAYER_ARMOR_CONTENTS, "");
         try {
             playerConfig.save(playerFile);
         } catch (IOException e) {
             e.printStackTrace();
         }
 
         cmdArgs = new String[]{"reload"};
         inventories.onCommand(mockCommandSender, mockCommand, "", cmdArgs);
 
         changeWorld(player, "world2", "world");

        newInventory = player.getInventory().toString();
         Assert.assertEquals(originalInventory, newInventory);
     }
 }
