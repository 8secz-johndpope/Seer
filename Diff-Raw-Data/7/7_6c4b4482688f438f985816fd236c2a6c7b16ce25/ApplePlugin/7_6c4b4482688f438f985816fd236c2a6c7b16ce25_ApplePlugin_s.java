 /**
  * $Revision: 22540 $
  * $Date: 2005-10-10 08:44:25 -0700 (Mon, 10 Oct 2005) $
  *
  * Copyright (C) 1999-2005 Jive Software. All rights reserved.
  *
  * This software is the proprietary information of Jive Software.
  * Use is subject to license terms.
  */
 package com.jivesoftware.spark.plugin.apple;
 
 import com.apple.eawt.Application;
 import com.apple.eawt.ApplicationAdapter;
 import com.apple.eawt.ApplicationEvent;
 
 import javax.swing.*;
 import java.awt.*;
 
 import org.jivesoftware.spark.plugin.Plugin;
 import org.jivesoftware.spark.SparkManager;
 import org.jivesoftware.spark.ui.ChatRoomListener;
 import org.jivesoftware.MainWindow;
 import org.jivesoftware.Spark;
 
 
 /**
  * Plugins for handling Mac OS X specific functionality
  *
  * @author Andrew Wright
  */
 public class ApplePlugin implements Plugin {
 
     private ChatRoomListener roomListener;
 
     public void initialize() {
         if (Spark.isMac()) {
             roomListener = new DockRoomListener();
             SparkManager.getChatManager().addChatRoomListener(roomListener);
 
             // Remove the About Menu Item from the help menu
             MainWindow mainWindow = SparkManager.getMainWindow();
 
             JMenu helpMenu = mainWindow.getMenuByName("Help");
             Component[] menuComponents = helpMenu.getMenuComponents();
             Component prev = null;
             for (int i = 0; i < menuComponents.length; i++) {
                 Component current = menuComponents[i];
                 if (current instanceof JMenuItem) {
                     JMenuItem item = (JMenuItem) current;
                     if ("About".equals(item.getText())) {
                         helpMenu.remove(item);
 
                         // We want to remove the seperator
                         if (prev != null && (prev instanceof JSeparator)) {
                             helpMenu.remove(prev);
                         }
                     }
                 }
                 prev = current;
             }
 
             JMenu connectMenu = mainWindow.getMenuByName("Spark");
             connectMenu.setText("Connect");
             menuComponents = connectMenu.getMenuComponents();
             JSeparator lastSeperator = null;
             for (int i = 0; i < menuComponents.length; i++) {
                 Component current = menuComponents[i];
                 if (current instanceof JMenuItem) {
                     JMenuItem item = (JMenuItem) current;
 
                     if ("Preferences".equals(item.getText())) {
                        connectMenu.remove(item);
                     } else if ("Log Out".equals(item.getText())) {
                         connectMenu.remove(item);
                     }
 
 
                 } else if (current instanceof JSeparator) {
                     lastSeperator = (JSeparator) current;
                 }
             }
             if (lastSeperator != null) {
                 connectMenu.remove(lastSeperator);
             }
 
             // register an application listener to show the about box
             Application application = Application.getApplication();
 
            application.setEnabledPreferencesMenu(true);
            application.addPreferencesMenuItem();
             application.addApplicationListener(new ApplicationAdapter() {
 
                 public void handlePreferences(ApplicationEvent applicationEvent) {
                     SparkManager.getPreferenceManager().showPreferences();
                 }
 
                 public void handleReOpenApplication(ApplicationEvent event) {
                     MainWindow mainWindow = SparkManager.getMainWindow();
                     if (!mainWindow.isVisible()) {
                         mainWindow.setState(Frame.NORMAL);
                         mainWindow.setVisible(true);
                     }
                 }
 
 
                 public void handleQuit(ApplicationEvent applicationEvent) {
                     System.exit(0);
                 }
 
             });
             new AppleStatusMenu().display();
         }
 
     }
 
     public void shutdown() {
         if (Spark.isMac()) {
             SparkManager.getChatManager().removeChatRoomListener(roomListener);
             roomListener = null;
         }
     }
 
     public boolean canShutDown() {
         return false;
     }
 
     public void uninstall() {
         // No need, since this is internal
     }
 
 
 
 
 }
 
 
 
