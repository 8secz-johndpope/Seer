 /*
  * Copyright (c) 2006-2008 Chris Smith, Shane Mc Cormack, Gregory Holmes
  *
  * Permission is hereby granted, free of charge, to any person obtaining a copy
  * of this software and associated documentation files (the "Software"), to deal
  * in the Software without restriction, including without limitation the rights
  * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
  * copies of the Software, and to permit persons to whom the Software is
  * furnished to do so, subject to the following conditions:
  *
  * The above copyright notice and this permission notice shall be included in
  * all copies or substantial portions of the Software.
  *
  * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
  * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
  * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
  * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
  * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
  * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
  * SOFTWARE.
  */
 
 package com.dmdirc.ui.swing;
 
 import com.dmdirc.logger.ErrorLevel;
 import com.dmdirc.logger.Logger;
 import com.dmdirc.ui.swing.actions.RedoAction;
 import com.dmdirc.ui.swing.actions.UndoAction;
 import com.dmdirc.ui.swing.components.DMDircUndoableEditListener;
 import com.dmdirc.ui.swing.components.NullBorder;
 import com.dmdirc.util.ReturnableThread;
 
 import java.awt.Dimension;
 import java.awt.GraphicsConfiguration;
 import java.awt.GraphicsDevice;
 import java.awt.MouseInfo;
 import java.awt.PointerInfo;
 import java.awt.Rectangle;
 import java.awt.Window;
 import java.awt.event.KeyEvent;
 import java.lang.reflect.InvocationTargetException;
 
 import javax.swing.BorderFactory;
 import javax.swing.KeyStroke;
 import javax.swing.SwingUtilities;
 import javax.swing.UIManager;
 import javax.swing.UIManager.LookAndFeelInfo;
 import javax.swing.UnsupportedLookAndFeelException;
 import javax.swing.text.JTextComponent;
 import javax.swing.undo.UndoManager;
 
 import net.miginfocom.layout.PlatformDefaults;
 import net.miginfocom.layout.UnitValue;
 
 /**
  * UI constants.
  */
 public final class UIUtilities {
 
     /** Size of a large border. */
     public static final int LARGE_BORDER = 10;
     /** Size of a small border. */
     public static final int SMALL_BORDER = 5;
     /** Standard button size. */
     public static final Dimension BUTTON_SIZE = new Dimension(100, 25);
 
     /** Not intended to be instatiated. */
     private UIUtilities() {
     }
 
     /**
      * Adds an undo manager and associated key bindings to the specified text
      * component.
      * 
      * @param component component Text component to add an undo manager to
      */
     public static void addUndoManager(final JTextComponent component) {
         final UndoManager undoManager = new UndoManager();
 
         // Listen for undo and redo events
         component.getDocument().addUndoableEditListener(
                 new DMDircUndoableEditListener(undoManager));
 
         // Create an undo action and add it to the text component
         component.getActionMap().put("Undo", new UndoAction(undoManager));
 
         // Bind the undo action to ctl-Z
         component.getInputMap().put(KeyStroke.getKeyStroke("control Z"), "Undo");
 
         // Create a redo action and add it to the text component
         component.getActionMap().put("Redo", new RedoAction(undoManager));
 
         // Bind the redo action to ctl-Y
         component.getInputMap().put(KeyStroke.getKeyStroke("control Y"), "Redo");
     }
 
     /**
      * Initialises any settings required by this UI (this is always called
      * before any aspect of the UI is instansiated).
      *
      * @throws UnsupportedOperationException If unable to switch to the system
      * look and feel
      */
     public static void initUISettings() {
 
         try {
 
             UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
         } catch (InstantiationException ex) {
             throw new UnsupportedOperationException("Unable to switch to the " +
                     "system look and feel", ex);
         } catch (ClassNotFoundException ex) {
             throw new UnsupportedOperationException("Unable to switch to the " +
                     "system look and feel", ex);
         } catch (UnsupportedLookAndFeelException ex) {
             throw new UnsupportedOperationException("Unable to switch to the " +
                     "system look and feel", ex);
         } catch (IllegalAccessException ex) {
             throw new UnsupportedOperationException("Unable to switch to the " +
                     "system look and feel", ex);
         }
 
         UIManager.put("swing.useSystemFontSettings", true);
         if (getTabbedPaneOpaque()) {
             // If this is set on windows then .setOpaque seems to be ignored
             // and still used as true
             UIManager.put("TabbedPane.contentOpaque", false);
         }
         UIManager.put("swing.boldMetal", false);
         UIManager.put("InternalFrame.useTaskBar", false);
         UIManager.put("SplitPaneDivider.border",
                 BorderFactory.createEmptyBorder());
         UIManager.put("Tree.scrollsOnExpand", true);
         UIManager.put("Tree.scrollsHorizontallyAndVertically", true);
         UIManager.put("SplitPane.border", new NullBorder());
         UIManager.put("SplitPane.dividerSize", (int) PlatformDefaults.getPanelInsets(0).getValue());
         UIManager.put("TreeUI", "javax.swing.plaf.metal.MetalTreeUI");
        PlatformDefaults.setDefaultRowAlignment(UnitValue.CENTER);
     }
 
     /**
      * Returns the class name of the look and feel from its display name.
      *
      * @param displayName Look and feel display name
      *
      * @return Look and feel class name or a zero length string
      */
     public static String getLookAndFeel(final String displayName) {
         if (displayName == null || displayName.isEmpty() ||
                 "Native".equals(displayName)) {
             return UIManager.getSystemLookAndFeelClassName();
         }
 
         final StringBuilder classNameBuilder = new StringBuilder();
 
         for (LookAndFeelInfo laf : UIManager.getInstalledLookAndFeels()) {
             if (laf.getName().equals(displayName)) {
                 classNameBuilder.append(laf.getClassName());
                 break;
             }
         }
 
         if (classNameBuilder.length() == 0) {
             classNameBuilder.append(UIManager.getSystemLookAndFeelClassName());
         }
 
         return classNameBuilder.toString();
     }
 
     /**
      * Invokes and waits for the specified runnable, executed on the EDT.
      * 
      * @param runnable Thread to be executed
      */
     public static void invokeAndWait(final Runnable runnable) {
         if (SwingUtilities.isEventDispatchThread()) {
             runnable.run();
         } else {
             try {
                 SwingUtilities.invokeAndWait(runnable);
             } catch (InterruptedException ex) {
                 Logger.userError(ErrorLevel.HIGH, "Unable to execute thread.");
             } catch (InvocationTargetException ex) {
                 Logger.appError(ErrorLevel.HIGH, "Unable to execute thread.", ex);
             }
         }
     }
 
     /**
      * Invokes and waits for the specified runnable, executed on the EDT.
      * 
      * @param <T> The return type of the returnable thread
      * @param returnable Thread to be executed
      * @return Result from the compelted thread
      */
     public static <T> T invokeAndWait(final ReturnableThread<T> returnable) {
         if (SwingUtilities.isEventDispatchThread()) {
             returnable.run();
         } else {
             try {
                 SwingUtilities.invokeAndWait(returnable);
             } catch (InterruptedException ex) {
                 Logger.userError(ErrorLevel.HIGH, "Unable to execute thread.");
             } catch (InvocationTargetException ex) {
                 Logger.appError(ErrorLevel.HIGH, "Unable to execute thread.", ex);
             }            
         }
         
         return returnable.getObject();
     }
 
     /**
      * Queues the runnable to be executed on the EDT.
      * 
      * @param runnable Runnable to be executed.
      */
     public static void invokeLater(final Runnable runnable) {
         if (SwingUtilities.isEventDispatchThread()) {
             runnable.run();
         } else {
             SwingUtilities.invokeLater(runnable);
         }
     }
 
     /**
      * Check if we are using one of the Windows Look and Feels
      * 
      * @return True iff the current LAF is "Windows" or "Windows Classic"
      */
     public static boolean isWindowsUI() {
         final String uiname = UIManager.getLookAndFeel().getClass().getName();
         final String windows =
                 "com.sun.java.swing.plaf.windows.WindowsLookAndFeel";
         final String classic =
                 "com.sun.java.swing.plaf.windows.WindowsClassicLookAndFeel";
 
         return windows.equals(uiname) || classic.equals(uiname);
     }
 
     /**
      * Get the value to pass to set Opaque on items being added to a JTabbedPane
      * 
      * @return True iff the current LAF is not Windows or OS X.
      * @since 0.6
      */
     public static boolean getTabbedPaneOpaque() {
         final String uiname = UIManager.getLookAndFeel().getClass().getName();
         final String windows =
                 "com.sun.java.swing.plaf.windows.WindowsLookAndFeel";
 
         return !(windows.equals(uiname) || Apple.isAppleUI());
     }
     
     /**
      * Get the DOWN_MASK for the command/ctrl key.
      * 
      * @return on OSX this returns META_DOWN_MASK, else CTRL_DOWN_MASK
      * @since 0.6
      */
     public static int getCtrlDownMask() {
         return Apple.isAppleUI() ? KeyEvent.META_DOWN_MASK : KeyEvent.CTRL_DOWN_MASK;
     }
     
     /**
      * Get the MASK for the command/ctrl key.
      * 
      * @return on OSX this returns META_MASK, else CTRL_MASK
      * @since 0.6
      */
     public static int getCtrlMask() {
         return Apple.isAppleUI() ? KeyEvent.META_MASK : KeyEvent.CTRL_MASK;
     }
     
     /**
      * Check if the command/ctrl key is pressed down.
      * 
      * @param e The KeyEvent to check
      * @return on OSX this returns e.isMetaDown(), else e.isControlDown()
      * @since 0.6
      */
     public static boolean isCtrlDown(final KeyEvent e) {
         return Apple.isAppleUI() ? e.isMetaDown() : e.isControlDown();
     }
     
     /**
      * Center the specified window on the active monitor.
      * 
      * @param window Window to center
      */
     public static void centerWindow(final Window window) {
         // Get the Location of the mouse pointer
         final PointerInfo myPointerInfo = MouseInfo.getPointerInfo();
         // Get the Device (screen) the mouse pointer is on
         final GraphicsDevice myDevice = myPointerInfo.getDevice();
         // Get the configuration for the device
         final GraphicsConfiguration myGraphicsConfig =
                 myDevice.getDefaultConfiguration();
         // Get the bounds of the device
         final Rectangle gcBounds = myGraphicsConfig.getBounds();
         // Calculate the center of the screen
         // gcBounds.x and gcBounds.y give the co ordinates where the screen
         // starts. gcBounds.width and gcBounds.height return the size in pixels
         // of the screen.
         final int xPos = gcBounds.x + ((gcBounds.width - window.getWidth()) / 2);
         final int yPos = gcBounds.y + ((gcBounds.height - window.getHeight()) / 2);
         // Set the location of the window
         window.setLocation(xPos, yPos);
     }
 }
