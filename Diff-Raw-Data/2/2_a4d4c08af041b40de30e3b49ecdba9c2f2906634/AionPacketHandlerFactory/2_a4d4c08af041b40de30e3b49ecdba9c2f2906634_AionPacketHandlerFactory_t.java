 /*
  * This file is part of aion-emu <aion-emu.com>.
  *
  *  aion-emu is free software: you can redistribute it and/or modify
  *  it under the terms of the GNU General Public License as published by
  *  the Free Software Foundation, either version 3 of the License, or
  *  (at your option) any later version.
  *
  *  aion-emu is distributed in the hope that it will be useful,
  *  but WITHOUT ANY WARRANTY; without even the implied warranty of
  *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  *  GNU General Public License for more details.
  *
  *  You should have received a copy of the GNU General Public License
  *  along with aion-emu.  If not, see <http://www.gnu.org/licenses/>.
  */
 package com.aionemu.gameserver.network.factories;
 
 import com.aionemu.gameserver.network.aion.AionClientPacket;
 import com.aionemu.gameserver.network.aion.AionPacketHandler;
 import com.aionemu.gameserver.network.aion.AionConnection.State;
 import com.aionemu.gameserver.network.aion.clientpackets.CM_ATTACK;
 import com.aionemu.gameserver.network.aion.clientpackets.CM_BLOCK_ADD;
 import com.aionemu.gameserver.network.aion.clientpackets.CM_BLOCK_DEL;
 import com.aionemu.gameserver.network.aion.clientpackets.CM_BLOCK_SET_REASON;
 import com.aionemu.gameserver.network.aion.clientpackets.CM_BUY_ITEM;
 import com.aionemu.gameserver.network.aion.clientpackets.CM_CASTSPELL;
 import com.aionemu.gameserver.network.aion.clientpackets.CM_CHARACTER_LIST;
 import com.aionemu.gameserver.network.aion.clientpackets.CM_CHAT_MESSAGE_PUBLIC;
 import com.aionemu.gameserver.network.aion.clientpackets.CM_CHAT_MESSAGE_WHISPER;
 import com.aionemu.gameserver.network.aion.clientpackets.CM_CHECK_NICKNAME;
 import com.aionemu.gameserver.network.aion.clientpackets.CM_CLIENT_COMMAND_LOC;
 import com.aionemu.gameserver.network.aion.clientpackets.CM_CLOSE_DIALOG;
 import com.aionemu.gameserver.network.aion.clientpackets.CM_CLOSE_LOOT;
 import com.aionemu.gameserver.network.aion.clientpackets.CM_CREATE_CHARACTER;
 import com.aionemu.gameserver.network.aion.clientpackets.CM_DELETE_CHARACTER;
 import com.aionemu.gameserver.network.aion.clientpackets.CM_DELETE_ITEM;
 import com.aionemu.gameserver.network.aion.clientpackets.CM_DIALOG_SELECT;
 import com.aionemu.gameserver.network.aion.clientpackets.CM_DUEL_REQUEST;
 import com.aionemu.gameserver.network.aion.clientpackets.CM_EMOTION;
 import com.aionemu.gameserver.network.aion.clientpackets.CM_ENTER_WORLD;
 import com.aionemu.gameserver.network.aion.clientpackets.CM_EQUIP_ITEM;
 import com.aionemu.gameserver.network.aion.clientpackets.CM_FRIEND_ADD;
 import com.aionemu.gameserver.network.aion.clientpackets.CM_FRIEND_DEL;
 import com.aionemu.gameserver.network.aion.clientpackets.CM_FRIEND_STATUS;
 import com.aionemu.gameserver.network.aion.clientpackets.CM_L2AUTH_LOGIN_CHECK;
 import com.aionemu.gameserver.network.aion.clientpackets.CM_LEVEL_READY;
 import com.aionemu.gameserver.network.aion.clientpackets.CM_LOGIN_OUT;
 import com.aionemu.gameserver.network.aion.clientpackets.CM_LOOT_ITEM;
 import com.aionemu.gameserver.network.aion.clientpackets.CM_MACRO_CREATE;
 import com.aionemu.gameserver.network.aion.clientpackets.CM_MACRO_DELETE;
 import com.aionemu.gameserver.network.aion.clientpackets.CM_MAC_ADDRESS;
 import com.aionemu.gameserver.network.aion.clientpackets.CM_MAC_ADDRESS2;
 import com.aionemu.gameserver.network.aion.clientpackets.CM_MAY_LOGIN_INTO_GAME;
 import com.aionemu.gameserver.network.aion.clientpackets.CM_MOVE;
 import com.aionemu.gameserver.network.aion.clientpackets.CM_PING;
 import com.aionemu.gameserver.network.aion.clientpackets.CM_PING_REQUEST;
 import com.aionemu.gameserver.network.aion.clientpackets.CM_PLAYER_SEARCH;
 import com.aionemu.gameserver.network.aion.clientpackets.CM_QUESTION_RESPONSE;
 import com.aionemu.gameserver.network.aion.clientpackets.CM_QUIT;
 import com.aionemu.gameserver.network.aion.clientpackets.CM_RECONNECT_AUTH;
 import com.aionemu.gameserver.network.aion.clientpackets.CM_RESTORE_CHARACTER;
 import com.aionemu.gameserver.network.aion.clientpackets.CM_REVIVE;
 import com.aionemu.gameserver.network.aion.clientpackets.CM_SEARCH_STATUS;
 import com.aionemu.gameserver.network.aion.clientpackets.CM_SET_NOTE;
 import com.aionemu.gameserver.network.aion.clientpackets.CM_SHOW_BLOCKLIST;
 import com.aionemu.gameserver.network.aion.clientpackets.CM_SHOW_DIALOG;
 import com.aionemu.gameserver.network.aion.clientpackets.CM_SHOW_FRIENDLIST;
 import com.aionemu.gameserver.network.aion.clientpackets.CM_START_LOOT;
 import com.aionemu.gameserver.network.aion.clientpackets.CM_TARGET_SELECT;
 import com.aionemu.gameserver.network.aion.clientpackets.CM_TELEPORT_SELECT;
 import com.aionemu.gameserver.network.aion.clientpackets.CM_TERRITORY;
 import com.aionemu.gameserver.network.aion.clientpackets.CM_TIME_CHECK;
 import com.aionemu.gameserver.network.aion.clientpackets.CM_EXCHANGE_CANCEL;
 import com.aionemu.gameserver.network.aion.clientpackets.CM_EXCHANGE_LOCK;
 import com.aionemu.gameserver.network.aion.clientpackets.CM_EXCHANGE_OK;
 import com.aionemu.gameserver.network.aion.clientpackets.CM_EXCHANGE_REQUEST;
 import com.aionemu.gameserver.network.aion.clientpackets.CM_USE_ITEM;
 import com.aionemu.gameserver.network.aion.clientpackets.CM_VERIFY_LOCATION;
 import com.aionemu.gameserver.network.aion.clientpackets.CM_VERSION_CHECK;
 import com.aionemu.gameserver.network.aion.clientpackets.CM_EXCHANGE_ADD_ITEM;
 import com.aionemu.gameserver.network.aion.clientpackets.CM_EXCHANGE_ADD_KINAH;
 import com.aionemu.gameserver.network.aion.clientpackets.CM_VIEW_PLAYER_DETAILS;
 import com.google.inject.Injector;
 
 /**
  * This factory is responsible for creating {@link AionPacketHandler} object. It also initializes created handler with a
  * set of packet prototypes.<br>
  * Object of this classes uses <tt>Injector</tt> for injecting dependencies into prototype objects.<br>
  * <br>
  * 
  * @author Luno
  * 
  */
 public class AionPacketHandlerFactory
 {
 	private Injector			injector;
 	private AionPacketHandler	handler	= new AionPacketHandler();
 	
 	/**
 	 * Creates new instance of <tt>AionPacketHandlerFactory</tt><br>
 	 * 
 	 * @param injector
 	 */
 	public AionPacketHandlerFactory(Injector injector)
 	{
 		this.injector = injector;
 
 		addPacket(new CM_RECONNECT_AUTH(0x21), State.AUTHED);
 		addPacket(new CM_L2AUTH_LOGIN_CHECK(0x7F), State.CONNECTED);
 		addPacket(new CM_VERSION_CHECK(0xEA), State.CONNECTED);
 		addPacket(new CM_TIME_CHECK(0xFC), State.CONNECTED, State.AUTHED, State.IN_GAME);
 		addPacket(new CM_ATTACK(0x8A), State.IN_GAME); 
 		addPacket(new CM_SET_NOTE(0xA4), State.IN_GAME); 
 		addPacket(new CM_TARGET_SELECT(0x89), State.IN_GAME);
 		addPacket(new CM_SHOW_FRIENDLIST(0x58), State.IN_GAME);
 		addPacket(new CM_SHOW_BLOCKLIST(0x12), State.IN_GAME);
 		addPacket(new CM_SEARCH_STATUS(0x4A), State.IN_GAME);
 		addPacket(new CM_RESTORE_CHARACTER(0x03), State.AUTHED);
 		addPacket(new CM_QUIT(0xED), State.AUTHED, State.IN_GAME);
 		addPacket(new CM_QUESTION_RESPONSE(0x9C), State.IN_GAME);
 		addPacket(new CM_PLAYER_SEARCH(0x09), State.IN_GAME);
 		addPacket(new CM_PING(0x96), State.AUTHED, State.IN_GAME);
 		addPacket(new CM_MOVE(0x9A), State.IN_GAME);
 		addPacket(new CM_MAY_LOGIN_INTO_GAME(0x24), State.AUTHED);
 		addPacket(new CM_MACRO_DELETE(0x1A), State.IN_GAME);// 1.5.x 
 		addPacket(new CM_MACRO_CREATE(0x19), State.IN_GAME);// 1.5.x
 		addPacket(new CM_MAC_ADDRESS2(0x18), State.IN_GAME);
 		addPacket(new CM_MAC_ADDRESS(0x27), State.CONNECTED, State.AUTHED, State.IN_GAME);
 		addPacket(new CM_LOGIN_OUT(0xEC), State.AUTHED, State.IN_GAME);
 		addPacket(new CM_LEVEL_READY(0xF3), State.IN_GAME);
 		addPacket(new CM_FRIEND_STATUS(0x14), State.IN_GAME);// 1.5.x
 		addPacket(new CM_FRIEND_DEL(0x5A), State.IN_GAME);// 1.5.x
		addPacket(new CM_FRIEND_ADD(0x59), State.IN_GAME);// 1.5.x
 		addPacket(new CM_ENTER_WORLD(0xF2), State.AUTHED);
 		addPacket(new CM_EMOTION(0x95), State.IN_GAME);
 		addPacket(new CM_DELETE_CHARACTER(0x02), State.AUTHED);// 1.5.x
 		addPacket(new CM_CREATE_CHARACTER(0x01), State.AUTHED);// 1.5.x
 		addPacket(new CM_CLIENT_COMMAND_LOC(0x78), State.IN_GAME);// 1.5.x
 		addPacket(new CM_CHECK_NICKNAME(0x1B), State.AUTHED);// 1.5.x
 		addPacket(new CM_CHAT_MESSAGE_WHISPER(0x86), State.IN_GAME);// 1.5.x
 		addPacket(new CM_CHAT_MESSAGE_PUBLIC(0x85), State.IN_GAME);
 		addPacket(new CM_CHARACTER_LIST(0x00), State.AUTHED);
 		addPacket(new CM_BLOCK_SET_REASON(0x1D),State.IN_GAME);
 		addPacket(new CM_BLOCK_DEL(0x11), State.IN_GAME);
 		addPacket(new CM_BLOCK_ADD(0x10), State.IN_GAME);
 
 		addPacket(new CM_EXCHANGE_REQUEST(0xA9), State.IN_GAME);
 		addPacket(new CM_EXCHANGE_LOCK(0xAD), State.IN_GAME);
 		addPacket(new CM_EXCHANGE_CANCEL(0xAF), State.IN_GAME);
 		addPacket(new CM_EXCHANGE_OK(0xAE), State.IN_GAME);
 		addPacket(new CM_EXCHANGE_ADD_ITEM(0xAA), State.IN_GAME);
 		addPacket(new CM_EXCHANGE_ADD_KINAH(0xAC), State.IN_GAME);
 
 		addPacket(new CM_TERRITORY(0x4A), State.IN_GAME);
 		addPacket(new CM_START_LOOT(0x04), State.IN_GAME);
 		addPacket(new CM_LOOT_ITEM(0x05), State.IN_GAME);
 		addPacket(new CM_CLOSE_LOOT(0x06), State.IN_GAME);
 		addPacket(new CM_CASTSPELL(0x8B), State.IN_GAME);
 		addPacket(new CM_SHOW_DIALOG(0x9E), State.IN_GAME);
 		addPacket(new CM_DIALOG_SELECT(0xA0), State.IN_GAME);
 		addPacket(new CM_CLOSE_DIALOG(0x9f), State.IN_GAME);//
 		addPacket(new CM_REVIVE(0xEF), State.IN_GAME);//0x9F
 		addPacket(new CM_BUY_ITEM(0x9d), State.IN_GAME);//0x9d
 		addPacket(new CM_USE_ITEM(0x8F), State.IN_GAME);//
 		addPacket(new CM_EQUIP_ITEM(0x90), State.IN_GAME);//
 		addPacket(new CM_DELETE_ITEM(0x5E), State.IN_GAME);//
 		addPacket(new CM_PING_REQUEST(0x51), State.IN_GAME); //1.5.x
 		addPacket(new CM_TELEPORT_SELECT(0x7E), State.IN_GAME);//
 		addPacket(new CM_VERIFY_LOCATION(0x9B), State.IN_GAME);//
 
 		addPacket(new CM_VIEW_PLAYER_DETAILS(0x4E), State.IN_GAME);
 		
 		addPacket(new CM_DUEL_REQUEST(0x5C), State.IN_GAME);
 
 		//addPacket(new CM_INVITE_TO_GROUP(0x4B ), State.IN_GAME);
 		//addPacket(new CM_REQUEST_DUEL(0x5C ), State.IN_GAME);
 
 		
 	}
 
 	public AionPacketHandler getPacketHandler()
 	{
 		return handler;
 	}
 
 	private void addPacket(AionClientPacket prototype, State... states)
 	{
 		injector.injectMembers(prototype);
 		handler.addPacketPrototype(prototype, states);
 	}
 
 }
