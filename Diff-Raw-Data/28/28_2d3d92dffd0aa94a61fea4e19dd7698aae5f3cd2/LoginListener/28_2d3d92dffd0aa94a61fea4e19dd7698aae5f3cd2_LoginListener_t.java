 package com.minecraftdimensions.bungeesuitechat.listeners;
 
 
 
 import org.bukkit.Bukkit;
 import org.bukkit.event.EventHandler;
 import org.bukkit.event.EventPriority;
 import org.bukkit.event.Listener;
 import org.bukkit.event.player.PlayerJoinEvent;
 import org.bukkit.event.player.PlayerLoginEvent;
 
 import com.minecraftdimensions.bungeesuitechat.BungeeSuiteChat;
 import com.minecraftdimensions.bungeesuitechat.managers.ChannelManager;
 import com.minecraftdimensions.bungeesuitechat.managers.PermissionsManager;
 import com.minecraftdimensions.bungeesuitechat.managers.PlayerManager;
 import com.minecraftdimensions.bungeesuitechat.objects.ServerData;
 
 
 public class LoginListener implements Listener {
 
 	@EventHandler(priority = EventPriority.NORMAL)
 	public void setFormatChat(final PlayerLoginEvent e) {
 		if(e.getPlayer().hasPermission("bungeesuite.*")){
 			PermissionsManager.addAllPermissions(e.getPlayer());
 		}else if(e.getPlayer().hasPermission("bungeesuite.admin")){
 			PermissionsManager.addAdminPermissions(e.getPlayer());
 		}else if(e.getPlayer().hasPermission("bungeesuite.mod")){
 			PermissionsManager.addModPermissions(e.getPlayer());
 		}else if(e.getPlayer().hasPermission("bungeesuite.vip")){
 			PermissionsManager.addVIPPermissions(e.getPlayer());
 		}else if(e.getPlayer().hasPermission("bungeesuite.user")){
 			PermissionsManager.addUserPermissions(e.getPlayer());
 		}
 		if(!ChannelManager.recievedChannels){
			ChannelManager.requestChannels();
 		}
 		Bukkit.getScheduler().runTaskLaterAsynchronously(BungeeSuiteChat.instance, new Runnable(){
 
 			@Override
 			public void run() {
 				if(e.getPlayer().isOnline() && !PlayerManager.isPlayerOnline(e.getPlayer().getName())){
 					PlayerManager.reloadPlayer(e.getPlayer().getName());
 				}
 			}
 			
 		}, 10L);
 	}
 	
 	@EventHandler(priority = EventPriority.NORMAL)
 	public void playerJoin(PlayerJoinEvent e) {
 		if(ServerData.getConnectionMessage()){
 			e.setJoinMessage(null);
 		}
 	}
 
 	
 
 }
