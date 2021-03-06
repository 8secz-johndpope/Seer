 /*
  * IdentityMatcher.java
  * This file is part of Freemail
  * Copyright (C) 2011 Martin Nyhus
  *
  * This program is free software; you can redistribute it and/or modify
  * it under the terms of the GNU General Public License as published by
  * the Free Software Foundation; either version 2 of the License, or
  * (at your option) any later version.
  *
  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  * GNU General Public License for more details.
  *
  * You should have received a copy of the GNU General Public License along
  * with this program; if not, write to the Free Software Foundation, Inc.,
  * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
  */
 
 package freemail.wot;
 
 import java.util.HashMap;
 import java.util.LinkedList;
 import java.util.List;
 import java.util.Map;
 import java.util.Set;
 
 import freenet.pluginmanager.PluginNotFoundException;
 
 public class IdentityMatcher {
 	private final WoTConnection wotConnection;
 
 	public IdentityMatcher(WoTConnection wotConnection) {
 		this.wotConnection = wotConnection;
 	}
 
 	public Map<String, List<Identity>> matchIdentities(Set<String> recipients, String wotOwnIdentity) throws PluginNotFoundException {
 		Set<Identity> wotIdentities = wotConnection.getAllTrustedIdentities(wotOwnIdentity);
 		wotIdentities.addAll(wotConnection.getAllUntrustedIdentities(wotOwnIdentity));
 		wotIdentities.addAll(wotConnection.getAllOwnIdentities());
 
 		Map<String, List<Identity>> allMatches = new HashMap<String, List<Identity>>(recipients.size());
 
 		for(String recipient : recipients) {
			allMatches.put(recipient, new LinkedList<Identity>());
		}
 
		for(Identity wotIdentity : wotIdentities) {
			for(String recipient : recipients) {
 				if(matchFullBase64Address(recipient, wotIdentity)) {
					allMatches.get(recipient).add(wotIdentity);
 				}
 			}
 		}
 
 		return allMatches;
 	}
 
 	private boolean matchFullBase64Address(String recipient, Identity identity) {
 		//Matches <anything>@<identity id>.freemail
 		if(!recipient.matches(".*@[A-Za-z0-9~\\-]{43,44}\\.freemail")) {
 			return false;
 		}
 
 		String recipientID = recipient.substring(recipient.lastIndexOf("@") + 1, recipient.length() - ".freemail".length());
 		return identity.getIdentityID().equals(recipientID);
 	}
 }
