 /*
     HoloIRC - an IRC client for Android
 
     Copyright 2013 Lalit Maganti
 
     This file is part of HoloIRC.
 
     HoloIRC is free software: you can redistribute it and/or modify
     it under the terms of the GNU General Public License as published by
     the Free Software Foundation, either version 3 of the License, or
     (at your option) any later version.
 
     HoloIRC is distributed in the hope that it will be useful,
     but WITHOUT ANY WARRANTY; without even the implied warranty of
     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
     GNU General Public License for more details.
 
     You should have received a copy of the GNU General Public License
     along with HoloIRC. If not, see <http://www.gnu.org/licenses/>.
  */
 
 package com.fusionx.irc.connection;
 
 import android.content.Context;
 import android.os.Bundle;
 
 import com.fusionx.common.Utils;
 import com.fusionx.irc.AppUser;
 import com.fusionx.irc.Server;
 import com.fusionx.irc.ServerConfiguration;
 import com.fusionx.irc.UserChannelInterface;
 import com.fusionx.irc.enums.ServerChannelEventType;
 import com.fusionx.irc.parser.ServerConnectionParser;
 import com.fusionx.irc.parser.ServerLineParser;
 import com.fusionx.irc.writers.ServerWriter;
 import com.fusionx.lightirc.R;
 import com.fusionx.uiircinterface.MessageSender;
 
 import org.apache.commons.lang3.StringUtils;
 
 import java.io.BufferedReader;
 import java.io.IOException;
 import java.io.InputStreamReader;
 import java.io.OutputStreamWriter;
 import java.net.Socket;
 
 import javax.net.ssl.SSLSocketFactory;
 
 import lombok.AccessLevel;
 import lombok.Getter;
 import lombok.Setter;
 
 class ServerConnection {
     @Getter(AccessLevel.PACKAGE)
     private Server server;
     private final Context mContext;
 
     private final ServerConfiguration serverConfiguration;
     private Socket mSocket;
 
     private ServerLineParser parser;
 
     @Setter(AccessLevel.PACKAGE)
     private boolean disconnectSent = false;
 
     private int timesToTry;
     private int reconnectAttempts = 0;
 
     ServerConnection(final ServerConfiguration configuration, final Context context,
                      final ConnectionWrapper wrapper) {
         server = new Server(configuration.getTitle(), wrapper);
         serverConfiguration = configuration;
         mContext = context;
     }
 
     void connectToServer() {
         timesToTry = Utils.getNumberOfReconnectEvents(mContext);
         reconnectAttempts = 0;
 
         connect();
 
         while (true) {
             final MessageSender sender = MessageSender.getSender(server.getTitle());
             if (!disconnectSent) {
                 if(reconnectAttempts < timesToTry) {
                     sender.sendGenericServerEvent("Trying to " +
                             "reconnect to the server in 5 seconds.");
                     try {
                         Thread.sleep(5000);
                     } catch (InterruptedException e) {
                         sender.sendFinalDisconnection("Disconnected from the server", disconnectSent);
                         break;
                     }
                     connect();
                 } else {
                     break;
                 }
                 ++reconnectAttempts;
             } else {
                 break;
             }
         }
     }
 
     private void connect() {
         final MessageSender sender = MessageSender.getSender(server.getTitle());
         try {
             final SSLSocketFactory sslSocketFactory = (SSLSocketFactory)
                     SSLSocketFactory.getDefault();
 
             mSocket = serverConfiguration.isSsl() ?
                     sslSocketFactory.createSocket(serverConfiguration.getUrl(),
                             serverConfiguration.getPort()) :
                     new Socket(serverConfiguration.getUrl(), serverConfiguration.getPort());
 
             final OutputStreamWriter writer = new OutputStreamWriter(mSocket.getOutputStream());
             server.setWriter(new ServerWriter(writer));
 
             server.setStatus(mContext.getString(R.string.status_connecting));
 
             final UserChannelInterface userChannelInterface = new UserChannelInterface(writer,
                     mContext, server);
             server.setUserChannelInterface(userChannelInterface);
 
             if (StringUtils.isNotEmpty(serverConfiguration.getServerPassword())) {
                 server.getWriter().sendServerPassword(serverConfiguration.getServerPassword());
             }
 
             server.getWriter().changeNick(serverConfiguration.getNickStorage().getFirstChoiceNick());
             server.getWriter().sendUser(serverConfiguration.getServerUserName(), "8", "*",
                     StringUtils.isNotEmpty(serverConfiguration.getRealName()) ?
                             serverConfiguration.getRealName() : "HoloIRC");
 
             final BufferedReader reader = new BufferedReader(
                     new InputStreamReader(mSocket.getInputStream()));
             final String nick = ServerConnectionParser.parseConnect(server.getTitle(), reader,
                     mContext, serverConfiguration.isNickChangable(), server.getWriter(),
                     serverConfiguration.getNickStorage());
 
             final Bundle event = Utils.parcelDataForBroadcast(null,
                     ServerChannelEventType.Connected, String.format(mContext
                     .getString(R.string.parser_connected),
                     serverConfiguration.getUrl()));
 
             sender.sendServerChannelMessage(event);
 
             server.setStatus(mContext.getString(R.string.status_connected));
 
             if (nick != null) {
                 final AppUser user = new AppUser(nick, server.getUserChannelInterface());
                 server.setUser(user);
 
                 if (StringUtils.isNotEmpty(serverConfiguration.getNickservPassword())) {
                     server.getWriter().sendNickServPassword(serverConfiguration
                             .getNickservPassword());
                 }
 
                 for (String channelName : serverConfiguration.getAutoJoinChannels()) {
                     server.getWriter().joinChannel(channelName);
                 }
 
                 parser = new ServerLineParser(mContext, server);
                 parser.parseMain(reader);
 
                 if (timesToTry == reconnectAttempts + 1 || disconnectSent) {
                     sender.sendFinalDisconnection("Disconnected from the server", disconnectSent);
                 } else {
                     sender.sendRetryPendingServerDisconnection("Disconnected from the server");
                 }
             }
         } catch (final IOException ex) {
             if (timesToTry == reconnectAttempts + 1 || disconnectSent) {
                 sender.sendFinalDisconnection(ex.getMessage() + "\n" + "Disconnected" +
                         " from the server", disconnectSent);
             } else {
                 sender.sendRetryPendingServerDisconnection(ex.getMessage());
             }
         }
         server.setStatus(mContext.getString(R.string.status_disconnected));
         closeSocket();
     }
 
     public void disconnectFromServer() {
         disconnectSent = true;
         parser.setDisconnectSent(true);
         server.getWriter().quitServer(Utils.getQuitReason(mContext));
     }
 
     public void closeSocket() {
         try {
             if (mSocket != null) {
                 mSocket.close();
             }
         } catch (IOException e) {
             e.printStackTrace();
         }
     }
 }
