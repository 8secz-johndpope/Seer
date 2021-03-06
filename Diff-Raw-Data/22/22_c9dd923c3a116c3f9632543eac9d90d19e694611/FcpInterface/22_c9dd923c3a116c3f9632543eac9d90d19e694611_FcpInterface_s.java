 /*
  * Sone - FcpInterface.java - Copyright © 2011 David Roden
  *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
  *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
  *
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
  */
 
 package net.pterodactylus.sone.freenet.fcp;
 
 import java.util.Collections;
 import java.util.HashMap;
 import java.util.Map;
 
 import net.pterodactylus.sone.core.Core;
 import net.pterodactylus.sone.freenet.fcp.Command.AccessType;
 import net.pterodactylus.sone.freenet.fcp.Command.Reply;
 import freenet.pluginmanager.PluginNotFoundException;
 import freenet.pluginmanager.PluginReplySender;
 import freenet.support.SimpleFieldSet;
 import freenet.support.api.Bucket;
 
 /**
  * Implementation of an FCP interface for other clients or plugins to
  * communicate with Sone.
  *
  * @author <a href="mailto:bombe@pterodactylus.net">David ‘Bombe’ Roden</a>
  */
 public class FcpInterface {
 
 	private final Core core;
 	private final Map<String, Command> commands = Collections.synchronizedMap(new HashMap<String, Command>());
 
 	public FcpInterface(Core core) {
 		this.core = core;
 		commands.put("Version", new VersionCommand());
 		commands.put("GetPostFeed", new GetPostFeedCommand(core));
 	}
 
 	public void handle(PluginReplySender pluginReplySender, SimpleFieldSet parameters, Bucket data, int accessType) {
 		Command command = commands.get(parameters.get("Message"));
 		if (command == null) {
 			/* TODO - return error? */
 			return;
 		}
 		String identifier = parameters.get("Identifier");
 		if ((identifier == null) || (identifier.length() == 0)) {
 			/* TODO - return error? */
 			return;
 		}
 		Reply reply = command.execute(parameters, data, AccessType.values()[accessType]);
 		try {
 			SimpleFieldSet replyParameters = reply.getReplyParameters();
 			replyParameters.putOverwrite("Identifier", identifier);
 			if (reply.hasData()) {
 				pluginReplySender.send(replyParameters, reply.getData());
 			} else if (reply.hasBucket()) {
 				pluginReplySender.send(replyParameters, reply.getBucket());
 			} else {
 				pluginReplySender.send(replyParameters);
 			}
 		} catch (PluginNotFoundException pnfe1) {
 			/* TODO - log */
 		}
 	}
 
 }
