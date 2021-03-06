 
 package net.sf.freecol.server.control;
 
 import java.io.IOException;
 import java.util.Iterator;
 import java.util.logging.Logger;
 
 import javax.xml.stream.XMLStreamException;
 import javax.xml.stream.XMLStreamReader;
 import javax.xml.stream.XMLStreamWriter;
 
 import net.sf.freecol.FreeCol;
 import net.sf.freecol.common.model.Game;
 import net.sf.freecol.common.networking.Connection;
 import net.sf.freecol.common.networking.Message;
 import net.sf.freecol.common.networking.MessageHandler;
 import net.sf.freecol.common.networking.StreamedMessageHandler;
 import net.sf.freecol.server.FreeColServer;
 import net.sf.freecol.server.model.ServerPlayer;
 import net.sf.freecol.server.networking.Server;
 
 import org.w3c.dom.Element;
 
 
 
 
 /**
 * Handles a new client connection. {@link PreGameInputHandler} is set
 * as the message handler when the client has successfully logged on.
 */
 public final class UserConnectionHandler implements MessageHandler, StreamedMessageHandler {
     private static Logger logger = Logger.getLogger(UserConnectionHandler.class.getName());
 
     public static final String  COPYRIGHT = "Copyright (C) 2003-2005 The FreeCol Team";
     public static final String  LICENSE = "http://www.gnu.org/licenses/gpl.html";
     public static final String  REVISION = "$Revision$";
 
     private FreeColServer freeColServer;
 
 
 
     /**
     * The constructor to use.
     * @param freeColServer The main control object.
     */
     public UserConnectionHandler(FreeColServer freeColServer) {
         this.freeColServer = freeColServer;
     }
 
 
 
 
 
     /**
     * Handles a network message.
     *
     * @param connection The <code>Connection</code> the message came from.
     * @param element The message to be processed.
     * @return The reply.
     */
     public synchronized Element handle(Connection connection, Element element) {
         Element reply = null;
 
         String type = element.getTagName();
 
         if (element != null) {
             if (type.equals("getVacantPlayers")) {
                 reply = getVacantPlayers(connection, element);
             } else if (type.equals("disconnect")) {
                 reply = disconnect(connection, element);                                
             } else {
                 logger.warning("Unkown request: " + type);
             }
         }
 
         return reply;
     }
 
     
     /**
      * Handles the main element of an XML message.
      *
      * @param connection The connection the message came from.
      * @param in The stream containing the message.
      * @param out The output stream for the reply.
      */
     public void handle(Connection connection, XMLStreamReader in, XMLStreamWriter out) {
         if (in.getLocalName().equals("login")) {
             login(connection, in, out);
         } else {
             logger.warning("Unkown (streamed) request: " + in.getLocalName());
         }
     }
     
     /**
      * Checks if the message handler support the given message.
      * @param tagName The tag name of the message to check.
      * @return The result.
      */
     public boolean accepts(String tagName) {
         return tagName.equals("login");
     }
 
     /**
      * Handles a "getVacantPlayers"-request.
      * 
      * @param connection The connection the message came from. 
      * @param element The element containing the request.
      * @return The reply: An XML element containing a list of the
      *       vacant players.
      */
     private Element getVacantPlayers(Connection connection, Element element) {
         Game game = freeColServer.getGame();
 
         if (freeColServer.getGameState() == FreeColServer.STARTING_GAME) {
             return null;
         }
 
         Element reply = Message.createNewRootElement("vacantPlayers");
         Iterator playerIterator = game.getPlayerIterator();
         while (playerIterator.hasNext()) {
             ServerPlayer player = (ServerPlayer) playerIterator.next();
             if (!player.isDead() && player.isEuropean() && !player.isREF()
                     && (!player.isConnected() || player.isAI())) {
                 Element playerElement = reply.getOwnerDocument().createElement("player");
                 playerElement.setAttribute("username", player.getUsername());
                 reply.appendChild(playerElement);
             }
         }
 
         return reply;
     }
 
 
     /**
      * Handles a "login"-request.
      * 
      * @param connection The connection the message is comming from.
      * @param in The stream with the incoming data.
      * @param out The target stream for the reply.
      */
     private void login(Connection connection, XMLStreamReader in, XMLStreamWriter out) {        
         // TODO: Do not allow more than one (human) player to connect to a singleplayer game.
         Game game = freeColServer.getGame();
         Server server = freeColServer.getServer();
         
        final String username = in.getAttributeValue(null, "username");
         if (username == null) {
             throw new IllegalArgumentException("The attribute 'username' is missing.");
         }
         
         final String freeColVersion = in.getAttributeValue(null, "freeColVersion");
         if (freeColVersion == null) {
             throw new IllegalArgumentException("The attribute 'freeColVersion' is missing.");
         }
         
         if (!freeColVersion.equals(FreeCol.getVersion())) {
             Message.createError(out, "server.wrongFreeColVersion", "The game versions do not match.");
             return;
         }
         
         if (freeColServer.getGameState() != FreeColServer.STARTING_GAME) {
             if (game.getPlayerByName(username) == null) {
                 Message.createError(out, "server.alreadyStarted", "The game has already been started!");
                 return;
             }
 
             ServerPlayer player = (ServerPlayer) game.getPlayerByName(username);
             if (player.isConnected() && !player.isAI()) {
                 Message.createError(out, "server.usernameInUse", "The specified username is already in use.");
                 return;
             }
             player.setConnection(connection);
             player.setConnected(true);
 
             if (player.isAI()) {
                 player.setAI(false);
                 Element setAIElement = Message.createNewRootElement("setAI");
                 setAIElement.setAttribute("player", player.getID());
                 setAIElement.setAttribute("ai", Boolean.toString(false));
                 server.sendToAll(setAIElement);
             }
 
             // In case this player is the first to reconnect:
             boolean isCurrentPlayer = (game.getCurrentPlayer() == null);
             if (isCurrentPlayer) {
                 game.setCurrentPlayer(player);
             }
 
             connection.setMessageHandler(freeColServer.getInGameInputHandler());
 
             freeColServer.updateMetaServer();
 
             // Make the reply:
             try {
                 out.writeStartElement("loginConfirmed");
                 out.writeAttribute("admin", Boolean.toString(player.isAdmin()));
                 out.writeAttribute("startGame", "true");
                 out.writeAttribute("isCurrentPlayer", Boolean.toString(isCurrentPlayer));
                 freeColServer.getGame().toXML(out, player);
                 out.writeEndElement();
             } catch (XMLStreamException e) {
                 logger.warning("Could not write XML to stream (2).");
             }
 
             // Successful login:
             server.addConnection(connection);
             return;
         }
 
         // Wait until the game has been created:
         int timeOut = 20000;
         while (freeColServer.getGame() == null) {
             try {
                 Thread.sleep(1000);
             } catch (InterruptedException e) {}
 
             timeOut -= 1000;
 
             if (timeOut <= 0) {
                 Message.createError(out, "server.timeOut", "Timeout when connecting to the server.");
                 return;
             }
         }
 
         if (!freeColServer.getGame().canAddNewPlayer()) {
             Message.createError(out, "server.maximumPlayers", "Sorry, the maximum number of players reached.");
             return;
         }
 
         if (freeColServer.getGame().playerNameInUse(username)) {
             Message.createError(out, "server.usernameInUse", "The specified username is already in use.");
             return;
         }
 
 
         // Create and add the new player:
         boolean admin = (freeColServer.getGame().getPlayers().size() == 0);
         ServerPlayer newPlayer = new ServerPlayer(freeColServer.getGame(), username, admin, connection.getSocket(), connection);
         freeColServer.getGame().addPlayer(newPlayer);
 
         // Send message to all players except to the new player:
         Element addNewPlayer = Message.createNewRootElement("addPlayer");
         addNewPlayer.appendChild(newPlayer.toXMLElement(null, addNewPlayer.getOwnerDocument()));
         freeColServer.getServer().sendToAll(addNewPlayer, connection);
 
         connection.setMessageHandler(freeColServer.getPreGameInputHandler());
 
         freeColServer.updateMetaServer();
         // Make the reply:
         try {
             out.writeStartElement("loginConfirmed");
             out.writeAttribute("admin", (admin ? "true" : "false"));
             freeColServer.getGame().toXML(out, newPlayer);
             out.writeEndElement();
         }  catch (XMLStreamException e) {
             logger.warning("Could not write XML to stream (2).");
         }
 
         // Successful login:
         server.addConnection(connection);
 
         return;
 
         /*
         Game game = freeColServer.getGame();
         Server server = freeColServer.getServer();
 
         if (!element.hasAttribute("username")) {
             throw new IllegalArgumentException("The attribute 'username' is missing.");
         }
 
         if (!element.hasAttribute("freeColVersion")) {
             throw new IllegalArgumentException("The attribute 'freeColVersion' is missing.");
         }
 
 
         if (!element.getAttribute("freeColVersion").equals(FreeCol.getVersion())) {
             return Message.createError("server.wrongFreeColVersion", "The game versions do not match.");
         }
 
         String username = element.getAttribute("username");
 
         if (freeColServer.getGameState() != FreeColServer.STARTING_GAME) {
             if (game.getPlayerByName(username) == null) {
                 return Message.createError("server.alreadyStarted", "The game has already been started!");
             }
 
             ServerPlayer player = (ServerPlayer) game.getPlayerByName(username);
             if (player.isConnected() && !player.isAI()) {
                 return Message.createError("server.usernameInUse", "The specified username is already in use.");
             }
             player.setConnection(connection);
             player.setConnected(true);
 
             if (player.isAI()) {
                 player.setAI(false);
                 Element setAIElement = Message.createNewRootElement("setAI");
                 setAIElement.setAttribute("player", player.getID());
                 setAIElement.setAttribute("ai", Boolean.toString(false));
                 server.sendToAll(setAIElement);
             }
 
             // In case this player is the first to reconnect:
             boolean isCurrentPlayer = (game.getCurrentPlayer() == null);
             if (isCurrentPlayer) {
                 game.setCurrentPlayer(player);
             }
 
             connection.setMessageHandler(freeColServer.getInGameInputHandler());
 
             freeColServer.updateMetaServer();
 
             // Make the reply:
             Element reply = Message.createNewRootElement("loginConfirmed");
             reply.setAttribute("admin", Boolean.toString(player.isAdmin()));
             reply.setAttribute("startGame", "true");
             reply.setAttribute("isCurrentPlayer", Boolean.toString(isCurrentPlayer));
             reply.appendChild(freeColServer.getGame().toXMLElement(player, reply.getOwnerDocument()));
 
             // Successful login:
             server.addConnection(connection);
 
             return reply;
         }
 
         // Wait until the game has been created:
         int timeOut = 20000;
         while (freeColServer.getGame() == null) {
             try {
                 Thread.sleep(1000);
             } catch (InterruptedException e) {}
 
             timeOut -= 1000;
 
             if (timeOut <= 0) {
                 return Message.createError("server.timeOut", "Timeout when connecting to the server.");
             }
         }
 
         if (!freeColServer.getGame().canAddNewPlayer()) {
             return Message.createError("server.maximumPlayers", "Sorry, the maximum number of players reached.");
         }
 
         if (freeColServer.getGame().playerNameInUse(username)) {
             return Message.createError("server.usernameInUse", "The specified username is already in use.");
         }
 
 
         // Create and add the new player:
         boolean admin = (freeColServer.getGame().getPlayers().size() == 0);
         ServerPlayer newPlayer = new ServerPlayer(freeColServer.getGame(), username, admin, connection.getSocket(), connection);
         freeColServer.getGame().addPlayer(newPlayer);
 
         // Send message to all players except to the new player:
         Element addNewPlayer = Message.createNewRootElement("addPlayer");
         addNewPlayer.appendChild(newPlayer.toXMLElement(null, addNewPlayer.getOwnerDocument()));
         freeColServer.getServer().sendToAll(addNewPlayer, connection);
 
         connection.setMessageHandler(freeColServer.getPreGameInputHandler());
 
         freeColServer.updateMetaServer();
 
         // Make the reply:
         Element reply = Message.createNewRootElement("loginConfirmed");
         reply.setAttribute("admin", (admin ? "true" : "false"));
         reply.appendChild(freeColServer.getGame().toXMLElement(newPlayer, reply.getOwnerDocument()));
 
         // Successful login:
         server.addConnection(connection);
 
         return reply;
         */
     }
     
     /**
      * Handles a "disconnect"-message.
      *
      * @param connection The <code>Connection</code> the message was received on.
      * @param disconnectElement The element (root element in a DOM-parsed XML tree) that
      *                holds all the information.
      * @return The reply.
      */
     private Element disconnect(Connection connection, Element disconnectElement) {
         try {
             connection.reallyClose();
         } catch (IOException e) {
             logger.warning("Could not close the connection.");
         }
         
         return null;
     }    
 }
