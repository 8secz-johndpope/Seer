 /*
  * Copyright (C) 2012-2014  Cedric Cheneau
  *
  * This program is free software: you can redistribute it and/or modify
  * it under the terms of the GNU General Public License as published by
  * the Free Software Foundation, either version 3 of the License, or
  * (at your option) any later version.
  *
  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  * GNU General Public License for more details.
  *
  * You should have received a copy of the GNU General Public License
  * along with this program.  If not, see <http://www.gnu.org/licenses/>.
  */
 
 package net.holmes.core.service.systray;
 
 import javax.swing.*;
 import javax.swing.event.PopupMenuEvent;
 import javax.swing.event.PopupMenuListener;
 import java.awt.*;
 import java.awt.event.MouseAdapter;
 import java.awt.event.MouseEvent;
 
 /**
  * System tray icon.
  * Freely inspired from <a href="http://grepcode.com/file/repo1.maven.org/maven2/org.jvnet.hudson.plugins.hudsontrayapp/client-jdk16/0.7.3/org/jdesktop/swinghelper/tray/JXTrayIcon.java">org.jdesktop.swinghelper.tray.JXTrayIcon</a> class (under LGPL v2.1 license)
  */
 public class SystemTrayIcon extends TrayIcon {
     private static final JDialog DIALOG;
 
     static {
         DIALOG = new JDialog((Frame) null, "HolmesSysTray");
         DIALOG.setUndecorated(true);
         DIALOG.setAlwaysOnTop(true);
     }
 
     private static final PopupMenuListener POPUP_LISTENER = new PopupMenuListener() {
         @Override
         public void popupMenuWillBecomeVisible(final PopupMenuEvent event) {
             // Nothing
         }
 
         @Override
         public void popupMenuWillBecomeInvisible(final PopupMenuEvent event) {
             DIALOG.setVisible(false);
         }
 
         @Override
         public void popupMenuCanceled(final PopupMenuEvent event) {
             DIALOG.setVisible(false);
         }
     };
     private JPopupMenu popupMenu;
 
     /**
      * Instantiates a new system tray icon.
      *
      * @param image   icon image
      * @param tooltip icon tooltip
      */
     public SystemTrayIcon(final Image image, final String tooltip) {
         super(image, tooltip);
         addMouseListener(new MouseAdapter() {
             @Override
             public void mouseClicked(final MouseEvent event) {
                 showPopupMenu(event);
             }
         });
     }
 
     /**
      * Show popup menu.
      *
      * @param event mouse event
      */
     private void showPopupMenu(final MouseEvent event) {
         if (popupMenu != null) {
            GraphicsEnvironment env = GraphicsEnvironment.getLocalGraphicsEnvironment();
            int maxWindowHeight = env.getMaximumWindowBounds().height;

            // Get y location for popup menu
            int yLocation;
            if (event.getY() > maxWindowHeight) {
                // TaskBar is on bottom of the screen
                yLocation = maxWindowHeight - popupMenu.getPreferredSize().height;
            } else {
                // TaskBar is on top of the screen
                yLocation = env.getDefaultScreenDevice().getDisplayMode().getHeight() - env.getMaximumWindowBounds().height;
            }

            // Show popup menu
            DIALOG.setLocation(event.getX(), yLocation);
             DIALOG.setVisible(true);
             popupMenu.show(DIALOG.getContentPane(), 0, 0);
             DIALOG.toFront();
         }
     }
 
     /**
      * Set popup menu.
      *
      * @param popupMenu popup menu
      */
     public void setPopupMenu(final JPopupMenu popupMenu) {
         if (this.popupMenu != null) {
             this.popupMenu.removePopupMenuListener(POPUP_LISTENER);
         }
 
         if (popupMenu != null) {
             this.popupMenu = popupMenu;
             this.popupMenu.addPopupMenuListener(POPUP_LISTENER);
         }
     }
 }
