 package com.raphfrk.bukkit.serverport;
 import java.util.HashMap;
 import java.util.HashSet;
 
 import org.bukkit.Material;
 import org.bukkit.block.Block;
 import org.bukkit.entity.Player;
 import org.bukkit.event.block.Action;
 import org.bukkit.event.player.PlayerChatEvent;
 import org.bukkit.event.player.PlayerInteractEvent;
 import org.bukkit.event.player.PlayerKickEvent;
 import org.bukkit.event.player.PlayerJoinEvent;
 import org.bukkit.event.player.PlayerListener;
 import org.bukkit.event.player.PlayerMoveEvent;
 import org.bukkit.event.player.PlayerRespawnEvent;
 import org.bukkit.inventory.ItemStack;
 import org.bukkit.plugin.java.JavaPlugin;
 
 
 public class ServerPortPlayerListener extends PlayerListener {
 
 	JavaPlugin serverPort = null;
 
 	ServerPortListenerCommon serverPortListenerCommon;
 
 	ServerPortPlayerListener( ServerPortBukkit serverPort ) {
 		this.serverPort = serverPort;
 		this.serverPortListenerCommon = serverPort.serverPortListenerCommon;
 	}
 
 	public boolean onBlockPlace(Player player, Block blockPlaced, Block blockClicked, ItemStack itemInHand) {
 
 		MyItem newItem = new MyItem();
 		newItem.setBukkitItem(itemInHand, 0);
 
 		return serverPortListenerCommon.onBlockPlace(new MyPlayer(player), new MyBlock(blockPlaced, 0), new MyBlock(blockClicked, 0), newItem );
 
 	}
 	
 	@Override
 	public void onPlayerKick(PlayerKickEvent event) {
 		if(TeleportCommand.playerKicked(event.getPlayer().getName())) {
 			event.setLeaveMessage(null);
 		}
 	}
 
	@Override
 	public void onPlayerJoin(PlayerJoinEvent event) {
 
 		org.bukkit.entity.Player player = event.getPlayer();
 
 		MyPlayer myPlayer = new MyPlayer(player);
 		
 		if(!serverPortListenerCommon.onLogin(myPlayer)) {
 			event.setJoinMessage(null);
 		}
 	}
 
 	public void onPlayerChat(PlayerChatEvent event) {
 
 		MyPlayer player = new MyPlayer( event.getPlayer() );
 
 		serverPortListenerCommon.onChat(player, event.getMessage());
 
 	}
 
 /*	public void onPlayerCommand(PlayerChatEvent event) {
 
 		String[] split = event.getMessage().split(" ",-1);
 
 		if( split[0].equals("/clear")) {
 			event.getPlayer().getInventory().clear(Integer.parseInt(split[1]));
 			event.setCancelled(true);
 			return;
 		}
 
 		if( split.length > 1 && split[0].equals("/kickme")) {
 			event.getPlayer().kickPlayer(split[1]);
 			event.setCancelled(true);
 		}
 
 		MyPlayer player = new MyPlayer(event.getPlayer());
 
 		boolean ret = serverPortListenerCommon.onCommand(event.getPlayer(), split);
 
 		if( ret ) {
 			event.setCancelled(true);
 		}
 
 
 	}*/
 
 	public void onPlayerRespawn(PlayerRespawnEvent e) {
 
 		Player player = e.getPlayer();
 		if( serverPortListenerCommon.communicationManager.limboStore.bindEnable ) {
 
 			{
 				//if(!deadPlayers.isEmpty()) {
 
 					MyLocation loc = TeleportCommand.teleportToBind(serverPortListenerCommon.communicationManager, new MyPlayer(player));
 
 					if( loc != null) {
 						//player.sendMessage("You have died, restoring your position to bind - new version");
 
 						e.setRespawnLocation(loc.getBukkitLocation());
 						return;
 
 					} 
 
 				//}
 			}
 			
 
 			/*MiscUtils.safeLogging(player.getName() + " has died, attempting to teleport to bind");
 
 			final String name = player.getName();
 
 			MyServer.bukkitServer.getScheduler().scheduleSyncDelayedTask(serverPort, new Runnable() {
 
 				public void run() {
 					deadPlayers.add(name); 
 				}
 
 			},20);
 			*/
 		}
 
 	}
 
 	@Override
     public void onPlayerInteract(PlayerInteractEvent event) {
 		
 		Action action = event.getAction();
 		
 		if(action != Action.RIGHT_CLICK_BLOCK) {
 			return;
 		}
 		
 		Block target = event.getClickedBlock();
 
 		//event.getPlayer().sendMessage("Biome: " + target.getBiome());
 		
 		if(!event.hasItem()) {
 			return;
 		}
 		ItemStack item = event.getItem();
 
 		if( item.getType().equals(Material.FLINT_AND_STEEL)) {
 			if( target != null && target.getType() != null && target.getType().equals(Material.OBSIDIAN)) {
 				event.setCancelled(true);
 
 				//Block blockFace = target.getFace(event.getBlockFace());
 
 				MyBlock myBlock = new MyBlock(event.getClickedBlock().getFace(event.getBlockFace()),0);
 
 				MyPlayer player = new MyPlayer(event.getPlayer());
 
 				if(serverPortListenerCommon.portalManager.testSignBlock(myBlock)) {
 					serverPortListenerCommon.portalManager.buttonPress(myBlock, player);
 				} else {
 					serverPortListenerCommon.portalManager.fireStarted(player , myBlock );
 				}
 			}
 		}
 
 
 	}
 
 	//HashSet<String> deadPlayers = new HashSet<String>();
 	HashMap<String, Long> spamShield = new HashMap<String, Long>();
 	
 	//public HashMap<Integer,IntLocation> oldPositions = new HashMap<Integer,IntLocation>();
 
 	public void onPlayerMove(PlayerMoveEvent event) {
 
 		/*if(!deadPlayers.isEmpty()) {
 			Player player = event.getPlayer();
 			
 			if(player.getHealth()>0 && deadPlayers.contains(player.getName())) {
 
 				long currentTime = System.currentTimeMillis();
 				if( spamShield.containsKey(player.getName()) && spamShield.get(player.getName()) > currentTime - 5000 ) {
 					return;
 				}
 
 				spamShield.put(player.getName(), currentTime);
 
 				MyLocation loc = TeleportCommand.teleportToBind(serverPortListenerCommon.communicationManager, new MyPlayer(player));
 
 				if( loc != null) {
 					//player.sendMessage("You have died, restoring your position to bind");
 
 					player.teleportTo(loc.getBukkitLocation());
 					event.setTo(loc.getBukkitLocation());
 					event.setFrom(loc.getBukkitLocation());
 					deadPlayers.remove(player.getName());
 					return;
 
 				} else {
 					
 					deadPlayers.remove(player.getName());
 					
 				}
 
 			}
 		}
 		*/
 		MyPlayer player = new MyPlayer(event.getPlayer());
 
 		org.bukkit.Location from = event.getFrom().clone();
 		org.bukkit.Location to = event.getTo().clone();
 		
 		IntLocation oldLocation = new IntLocation(from.getBlockX(), from.getBlockY(), from.getBlockZ(), from.getWorld().getName());
 		
 		IntLocation newLocation = new IntLocation(to.getBlockX(), to.getBlockY(), to.getBlockZ(), to.getWorld().getName());
 		
 		//oldPositions.put(entityId, newLocation);
 		
 		if(oldLocation==null || !oldLocation.getWorld().getName().equals(newLocation.getWorld().getName())) {
 			return;
 		}
 		
 		if(oldLocation.equals(newLocation)) return;
 
 		from.setX(oldLocation.getX());
 		from.setY(oldLocation.getY());
 		from.setZ(oldLocation.getZ());
 
 		to.setX(newLocation.getX());
 		to.setY(newLocation.getY());
 		to.setZ(newLocation.getZ());
 		
 
 		MyLocation myFrom = new MyLocation(from);
 		MyLocation myTo= new MyLocation(to);
 
 		serverPortListenerCommon.onPlayerMove(player, myFrom, myTo);   	
 		if( player.getTeleportTo() != null ) {
 			event.setTo(player.getTeleportTo());
 		}
 
 	}
 	
 	
 
 
 
 }
