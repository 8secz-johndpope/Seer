 /**
  * $Revision: $
  * $Date: $
  *
  * Copyright (C) 2006 Jive Software. All rights reserved.
  *
  * This software is published under the terms of the GNU Lesser Public License (LGPL),
  * a copy of which is included in this distribution.
  */
 
 package org.jivesoftware.spark;
 
 import org.jdesktop.jdic.systeminfo.SystemInfo;
 import org.jivesoftware.Spark;
 import org.jivesoftware.smack.ConnectionListener;
 import org.jivesoftware.smack.XMPPConnection;
 import org.jivesoftware.smack.XMPPException;
 import org.jivesoftware.smack.packet.Presence;
 import org.jivesoftware.smack.packet.StreamError;
 import org.jivesoftware.smack.provider.ProviderManager;
 import org.jivesoftware.smack.util.StringUtils;
 import org.jivesoftware.smackx.PrivateDataManager;
 import org.jivesoftware.smackx.ServiceDiscoveryManager;
 import org.jivesoftware.smackx.packet.DiscoverItems;
 import org.jivesoftware.spark.ui.ChatRoom;
 import org.jivesoftware.spark.ui.PresenceListener;
 import org.jivesoftware.spark.ui.status.StatusItem;
 import org.jivesoftware.spark.util.log.Log;
 import org.jivesoftware.sparkimpl.plugin.manager.Features;
 import org.jivesoftware.sparkimpl.settings.local.LocalPreferences;
 import org.jivesoftware.sparkimpl.settings.local.SettingsManager;
 
 import java.util.ArrayList;
 import java.util.Collection;
 import java.util.Iterator;
 import java.util.List;
 import java.util.Timer;
 import java.util.TimerTask;
 
 import javax.swing.SwingUtilities;
 
 /**
  * This manager is responsible for the handling of the XMPPConnection used within Spark. This is used
  * for the changing of the users presence, the handling of connection errors and the ability to add
  * presence listeners and retrieve the connection used in Spark.
  *
  * @author Derek DeMoro
  */
 public final class SessionManager implements ConnectionListener {
     private XMPPConnection connection;
     private PrivateDataManager personalDataManager;
 
     private String serverAddress;
     private String username;
     private String password;
 
     private String JID;
 
     private List presenceListeners = new ArrayList();
 
     private String userBareAddress;
     private boolean unavaliable = false;
     private DiscoverItems discoverItems;
 
     private int previousPriority = -1;
 
 
     public SessionManager() {
     }
 
     /**
      * Initializes session.
      *
      * @param connection the XMPPConnection used in this session.
      * @param username   the agents username.
      * @param password   the agents password.
      */
     public void initializeSession(XMPPConnection connection, String username, String password) {
         this.connection = connection;
         this.username = username;
         this.password = password;
         this.userBareAddress = StringUtils.parseBareAddress(connection.getUser());
 
         // create workgroup session
         personalDataManager = new PrivateDataManager(getConnection());
 

        if (Spark.isWindows()) {
            try {
                setIdleListener();
            }
            catch (Exception e) {
                Log.error(e);
             }
         }
 
         // Discover items
         discoverItems();
 
         ProviderManager.addExtensionProvider("event", "http://jabber.org/protocol/disco#info", new Features.Provider());
     }
 
     /**
      * Does the initial service discovery.
      */
     private void discoverItems() {
         ServiceDiscoveryManager disco = ServiceDiscoveryManager.getInstanceFor(SparkManager.getConnection());
         try {
             discoverItems = disco.discoverItems(SparkManager.getConnection().getServiceName());
         }
         catch (XMPPException e) {
             Log.error(e);
         }
     }
 
     /**
      * Returns the XMPPConnection used for this session.
      *
      * @return the XMPPConnection used for this session.
      */
     public XMPPConnection getConnection() {
         return connection;
     }
 
 
     /**
      * Returns the PrivateDataManager responsible for handling all private data for individual
      * agents.
      *
      * @return the PrivateDataManager responsible for handling all private data for individual
      *         agents.
      */
     public PrivateDataManager getPersonalDataManager() {
         return personalDataManager;
     }
 
 
     /**
      * Returns the host for this connection.
      *
      * @return the connection host.
      */
     public String getServerAddress() {
         return serverAddress;
     }
 
     /**
      * Set the server address
      *
      * @param address the address of the server.
      */
     public void setServerAddress(String address) {
         this.serverAddress = address;
     }
 
     /**
      * Notify agent the connection was closed due to an exception.
      *
      * @param ex the Exception that took place.
      */
     public void connectionClosedOnError(final Exception ex) {
         SwingUtilities.invokeLater(new Runnable() {
             public void run() {
                 Log.error("Connection closed on error.", ex);
 
                 String message = "Your connection was closed due to an error.";
 
                 if (ex instanceof XMPPException) {
                     XMPPException xmppEx = (XMPPException)ex;
                     StreamError error = xmppEx.getStreamError();
                     String reason = error.getCode();
                     if ("conflict".equals(reason)) {
                         message = "Your connection was closed due to the same user logging in from another location.";
                     }
                 }
 
                 Collection rooms = SparkManager.getChatManager().getChatContainer().getChatRooms();
                 Iterator iter = rooms.iterator();
                 while (iter.hasNext()) {
                     ChatRoom chatRoom = (ChatRoom)iter.next();
                     chatRoom.getChatInputEditor().setEnabled(false);
                     chatRoom.getSendButton().setEnabled(false);
                     chatRoom.getTranscriptWindow().insertNotificationMessage(message);
                 }
 
 
             }
         });
     }
 
     /**
      * Notify agent that the connection has been closed.
      */
     public void connectionClosed() {
         connectionClosedOnError(null);
     }
 
     /**
      * Return the username associated with this session.
      *
      * @return the username associated with this session.
      */
     public String getUsername() {
         return username;
     }
 
     /**
      * Return the password associated with this session.
      *
      * @return the password assoicated with this session.
      */
     public String getPassword() {
         return password;
     }
 
     /**
      * Update the current availability of the user
      *
      * @param presence the current presence of the user.
      */
     public void changePresence(Presence presence) {
         // Send Presence Packet
         SparkManager.getConnection().sendPacket(presence);
 
         // Update Tray Icon
         SparkManager.getNotificationsEngine().changePresence(presence);
 
         // Fire Presence Listeners
         final Iterator presenceListeners = new ArrayList(this.presenceListeners).iterator();
         while (presenceListeners.hasNext()) {
             ((PresenceListener)presenceListeners.next()).presenceChanged(presence);
         }
     }
 
     /**
      * Returns the jid of the Spark user.
      *
      * @return the jid of the Spark user.
      */
     public String getJID() {
         return JID;
     }
 
     /**
      * Sets the jid of the current Spark user.
      *
      * @param jid the jid of the current Spark user.
      */
     public void setJID(String jid) {
         this.JID = jid;
     }
 
     /**
      * Adds a <code>PresenceListener</code> to Spark. PresenceListener's are used
      * to allow notification of when the Spark users changes their presence.
      *
      * @param listener the listener.
      */
     public void addPresenceListener(PresenceListener listener) {
         presenceListeners.add(listener);
     }
 
     /**
      * Remove a <code>PresenceListener</code> from Spark.
      *
      * @param listener the listener.
      */
     public void removePresenceListener(PresenceListener listener) {
         presenceListeners.remove(listener);
     }
 
     /**
      * Returns the users bare address. A bare-address is the address without a resource (ex. derek@jivesoftware.com/spark would
      * be derek@jivesoftware.com)
      *
      * @return the users bare address.
      */
     public String getBareAddress() {
         return userBareAddress;
     }
 
     /**
      * Sets the Idle Timeout for this instance of Spark.
      */
    private void setIdleListener() throws Exception {
 
         final Timer timer = new Timer();
 
         timer.scheduleAtFixedRate(new TimerTask() {
             public void run() {
                LocalPreferences localPref = SettingsManager.getLocalPreferences();
                int delay = 0;
                if (localPref.isIdleOn()) {
                    delay = localPref.getIdleTime() * 60000;
                }

                 long idleTime = SystemInfo.getSessionIdleTime();
                 boolean isLocked = SystemInfo.isSessionLocked();
                if (idleTime > delay) {
                     try {
                         // Handle if spark is not connected to the server.
                         if (SparkManager.getConnection() == null || !SparkManager.getConnection().isConnected()) {
                             return;
                         }
 
                         // Change Status
                         Workspace workspace = SparkManager.getWorkspace();
                         Presence presence = workspace.getStatusBar().getPresence();
                         if (workspace != null && presence.getMode() == Presence.Mode.available) {
                             unavaliable = true;
                             StatusItem away = workspace.getStatusBar().getStatusItem("Away");
                             Presence p = away.getPresence();
                             if (isLocked) {
                                 p.setStatus("User has locked their workstation.");
                             }
                             else {
                                 p.setStatus("Away due to idle.");
                             }
 
                             previousPriority = presence.getPriority();
 
                             p.setPriority(0);
                             SparkManager.getSessionManager().changePresence(p);
                         }
                     }
                     catch (Exception e) {
                         Log.error("Error with IDLE status.", e);
                         timer.cancel();
                     }
                 }
                 else {
                     if (unavaliable) {
                         setAvailableIfActive();
                     }
                 }
             }
         }, 1000, 1000);
     }
 
     private void setAvailableIfActive() {
 
         // Handle if spark is not connected to the server.
         if (SparkManager.getConnection() == null || !SparkManager.getConnection().isConnected()) {
             return;
         }
 
         // Change Status
         Workspace workspace = SparkManager.getWorkspace();
         if (workspace != null) {
             Presence presence = workspace.getStatusBar().getStatusItem("Online").getPresence();
             if (previousPriority != -1) {
                 presence.setPriority(previousPriority);
             }
 
             SparkManager.getSessionManager().changePresence(presence);
             unavaliable = false;
         }
     }
 
     /**
      * Returns the Discovered Items.
      *
      * @return the discovered items found on startup.
      */
     public DiscoverItems getDiscoveredItems() {
         return discoverItems;
     }
 
     public void setConnection(XMPPConnection con) {
         this.connection = con;
     }
 
 }
