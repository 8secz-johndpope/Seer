 /*  
  *  PlayerAnimationLib
  *  Copyright (C) 2013 CaptainBern
  *   
  *  This program is free software: you can redistribute it and/or modify
  *  it under the terms of the GNU General Public License as published by
  *  the Free Software Foundation, either version 3 of the License, or
  *  (at your option) any later version.
  *
  *  This program is distributed in the hope that it will be useful,
  *  but WITHOUT ANY WARRANTY; without even the implied warranty of
  *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  *  GNU General Public License for more details.
  *
  *  You should have received a copy of the GNU General Public License
  *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
  */
 
 package me.captainbern.animationlib.animations;
 
import java.util.ArrayList;
 import java.util.Arrays;
 import java.util.Collection;
 
 import me.captainbern.animationlib.event.PlayerAnimationEvent;
 import me.captainbern.animationlib.utils.Packet;
import me.captainbern.animationlib.utils.ReflectionUtil;
 
 import org.bukkit.Bukkit;
 import org.bukkit.Location;
 import org.bukkit.World;
 import org.bukkit.entity.Player;
 
 public enum PlayerAnimation {
 	
 	ARM_SWING {
 		@Override
 		protected void broadcastAnimation(Player player) {
 			try {
 
 				Packet packet = new Packet("Packet18ArmAnimation");
 				packet.setPublicValue("a", player.getEntityId());
 				packet.setPublicValue("b", 1);
 				sendPacketNearby(player.getLocation(), Arrays.asList(packet), this);
 
 			} catch (Exception e) {
 				Bukkit.getLogger().warning("[AnimationLib] Something went wrong while crafting the Packet18ArmAnimation packet!" + "(" + this + ")");
 				e.printStackTrace();
 			}
 		}
 	},
 	CRIT {
 		@Override
 		protected void broadcastAnimation(Player player) {
 			try {
 
 				Packet packet = new Packet("Packet18ArmAnimation");
 				packet.setPublicValue("a", player.getEntityId());
 				packet.setPublicValue("b", 6);
 				sendPacketNearby(player.getLocation(), Arrays.asList(packet), this);
 
 			} catch (Exception e) {
 				Bukkit.getLogger().warning("[AnimationLib] Something went wrong while crafting the Packet18ArmAnimation packet!" + "(" + this + ")");
 				e.printStackTrace();
 			}
 		}
 	},
 	HURT {
 		@Override
 		protected void broadcastAnimation(Player player) {
 			try {
 
 				Packet packet = new Packet("Packet18ArmAnimation");
 				packet.setPublicValue("a", player.getEntityId());
 				packet.setPublicValue("b", 2);
 				sendPacketNearby(player.getLocation(), Arrays.asList(packet), this);
 
 			} catch (Exception e) {
 				Bukkit.getLogger().warning("[AnimationLib] Something went wrong while crafting the Packet18ArmAnimation packet!" + "(" + this + ")");
 				e.printStackTrace();
 			}
 		}
 	},
 	DEAD {
 		@Override
 		protected void broadcastAnimation(Player player) {
 			try {
 
 				Packet packet = new Packet("Packet70Bed");
 				packet.setPublicValue("c", player.getEntityId());
 				packet.setPublicValue("b", 3);
 				sendPacketNearby(player.getLocation(), Arrays.asList(packet), this);
 
 			} catch (Exception e) {
 				Bukkit.getLogger().warning("[AnimationLib] Something went wrong while crafting the Packet70Bed packet!" + "(" + this + ")");
 				e.printStackTrace();
 			}
 		}
 	},
 	MAGIC_CRIT {
 		@Override
 		protected void broadcastAnimation(Player player) {
 			try {
 
 				Packet packet = new Packet("Packet18ArmAnimation");
 				packet.setPublicValue("a", player.getEntityId());
 				packet.setPublicValue("b", 7);
 				sendPacketNearby(player.getLocation(), Arrays.asList(packet), this);
 
 			} catch (Exception e) {
 				Bukkit.getLogger().warning("[AnimationLib] Something went wrong while crafting the Packet18ArmAnimation packet!" + "(" + this + ")");
 				e.printStackTrace();
 			}
 		}
 	},
 	SLEEP {
 		@Override
 		protected void broadcastAnimation(Player player) {
 			try {
 
 				Packet packet = new Packet("Packet17EntityLocationAction");
 				packet.setPublicValue("a", player.getEntityId());
 				packet.setPublicValue("e", 0);
 				packet.setPublicValue("b", (int) player.getLocation().getX());
 				packet.setPublicValue("c", (int) player.getLocation().getY());
 				packet.setPublicValue("d", (int) player.getLocation().getZ());
 				sendPacketNearby(player.getLocation(), Arrays.asList(packet), this);
 
 			} catch (Exception e) {
 				Bukkit.getLogger().warning("[AnimationLib] Something went wrong while crafting the Packet17EntityLocation packet!" + "(" + this + ")");
 				e.printStackTrace();
 			}
 
 		}
 	},
 	STOP_SLEEPING {
 		@Override
 		protected void broadcastAnimation(Player player) {
 			try {
 
 				Packet packet = new Packet("Packet18ArmAnimation");
 				packet.setPublicValue("a", player.getEntityId());
 				packet.setPublicValue("b", 3);
 				sendPacketNearby(player.getLocation(), Arrays.asList(packet), this);
 
 			} catch (Exception e) {
 				Bukkit.getLogger().warning("[AnimationLib] Something went wrong while crafting the Packet18ArmAnimation packet!" + "(" + this + ")");
 				e.printStackTrace();
 			}
 		}
 	},
 	START_RAIN {
 		@Override
 		protected void broadcastAnimation(Player player) {
 			try {
 
 				Packet packet = new Packet("Packet70Bed");
 				packet.setPublicValue("c", player.getEntityId());
 				packet.setPublicValue("b", 1);
 				sendPacketNearby(player.getLocation(), Arrays.asList(packet), this);
 
 			} catch (Exception e) {
 				Bukkit.getLogger().warning("[AnimationLib] Something went wrong while crafting the Packet70Bed packet!" + "(" + this + ")");
 				e.printStackTrace();
 			}
 		}
 	},
 	STOP_RAIN {
 		@Override
 		protected void broadcastAnimation(Player player) {
 			try {
 
 				Packet packet = new Packet("Packet70Bed");
 				packet.setPublicValue("c", player.getEntityId());
 				packet.setPublicValue("b", 2);
 				sendPacketNearby(player.getLocation(), Arrays.asList(packet), this);
 
 			} catch (Exception e) {
 				Bukkit.getLogger().warning("[AnimationLib] Something went wrong while crafting the Packet70Bed packet!" + "(" + this + ")");
 				e.printStackTrace();
 			}
 		}
 	},
 	ENTER_CREDITS {
 		@Override
 		protected void broadcastAnimation(Player player) {
 			try {
 
 				Packet packet = new Packet("Packet70Bed");
 				packet.setPublicValue("c", player.getEntityId());
 				packet.setPublicValue("b", 4);
 				sendPacketNearby(player.getLocation(), Arrays.asList(packet), this);
 
 			} catch (Exception e) {
 				Bukkit.getLogger().warning("[AnimationLib] Something went wrong while crafting the Packet70Bed packet!" + "(" + this + ")");
 				e.printStackTrace();
 			}
 		}
 	},
 	CHANGE_GAMEMODE {
 		@Override
 		protected void broadcastAnimation(Player player) {
 			try {
 
 				Packet packet = new Packet("Packet70Bed");
 				packet.setPublicValue("c", player.getEntityId());
 				packet.setPublicValue("b", 3);
 				sendPacketNearby(player.getLocation(), Arrays.asList(packet), this);
 
 			} catch (Exception e) {
 				Bukkit.getLogger().warning("[AnimationLib] Something went wrong while crafting the Packet70Bed packet!" + "(" + this + ")");
 				e.printStackTrace();
 			}
 		}
 	},
	SIT {
 		@Override
 		protected void broadcastAnimation(Player player) {
 			try {
 
 				Object entityPlayer = ReflectionUtil.BukkitPlayerToEntityPlayer(player);
 
 				Packet packet = new Packet("Packet40EntityMetadata");
 				packet.setPublicValue("a", player.getEntityId());
 				Object datawatcher = entityPlayer.getClass().getMethod("getDataWatcher").invoke(entityPlayer);
 				packet.setPrivateValue("b", datawatcher.getClass().getMethod("c").invoke(datawatcher));
 				
 				
 				Collection<Packet> packets = Arrays.asList(packet);
 
 				sendPacketNearby(player.getLocation(), packets, this);
 
 			} catch (Exception e) {
 				e.printStackTrace();
 			}
 		}
 	},
 	/*STOP_SITTING {
 		@Override
 		protected void broadcastAnimation(Player player) {
 			player.mount(null);
 		}
 	},*/
	SNEAK {
 		@SuppressWarnings("unchecked")
 		@Override
 		protected void broadcastAnimation(Player player) {
 			player.setSneaking(true);
 			try {
 				
 				Object entityPlayer = ReflectionUtil.BukkitPlayerToEntityPlayer(player);
 				player.setSneaking(true);
 				Packet packet = new Packet("Packet40EntityMetadata");
 				packet.setPublicValue("a", player.getEntityId());
 				//DataWatcherUtil dw = new DataWatcherUtil((byte) 4);
 				Object datawatcher = entityPlayer.getClass().getMethod("getDataWatcher").invoke(entityPlayer);
 				@SuppressWarnings("rawtypes")
 				ArrayList list = (ArrayList) datawatcher.getClass().getMethod("c").invoke(datawatcher);
 				list.set(0, (byte) 4);
 				packet.setPrivateValue("b", list);
 				sendPacketNearby(player.getLocation(), Arrays.asList(packet), this);
 
 			} catch (Exception e) {
 				Bukkit.getLogger().warning("[PlayerAnimationLib] Something went wrong while crafting the Packet40EntityMetadata packet! (SNEAK)");
 				e.printStackTrace();
 			}
 		}
 	},
 	/*STOP_SNEAKING {
 		@Override
 		protected void broadcastAnimation(Player player) {
 			player.getBukkitEntity().setSneaking(false);
 			sendPacketNearby(new Packet40EntityMetadata(player.id, player.getDataWatcher(), true), player, radius);
 		}
 	}*/;
 	/*
 	 * You can look again
 	 */
 
 	/* STOP ENUMS */
 
 	public void play(Player player){
 		broadcastAnimation(player);
 	}
 
 	private static void sendPacketNearby(Location loc, Collection<Packet> packets, PlayerAnimation pa) {
 		World world = loc.getWorld();
 		for(Player player : Bukkit.getOnlinePlayers()){
 			if(player == null || player.getWorld() != world){
 				continue;
 			}
 			PlayerAnimationEvent event = new PlayerAnimationEvent(player, pa);
 			Bukkit.getPluginManager().callEvent(event);
 			if(!event.isCancelled()){
 				for(Packet packet : packets){
 					packet.send(player);
 				}
 			}
 		}
 	}
 
 	protected void broadcastAnimation(Player player){
 		throw new UnsupportedOperationException("[AnimationLib] Unimplemented animation");
 	}
 
 }
