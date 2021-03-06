 /*
  * Copyright (c) 2006-2010 Chris Smith, Shane Mc Cormack, Gregory Holmes
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
 
 package com.dmdirc;
 
 import com.dmdirc.actions.ActionManager;
 import com.dmdirc.actions.CoreActionType;
 import com.dmdirc.config.ConfigManager;
 import com.dmdirc.interfaces.ConfigChangeListener;
 import com.dmdirc.interfaces.FrameInfoListener;
 import com.dmdirc.interfaces.NotificationListener;
 import com.dmdirc.interfaces.SelectionListener;
 import com.dmdirc.ui.WindowManager;
 import com.dmdirc.ui.interfaces.Window;
 import com.dmdirc.ui.messages.Formatter;
 import com.dmdirc.ui.messages.IRCDocument;
 import com.dmdirc.ui.messages.Styliser;
 import com.dmdirc.util.ListenerList;
 import com.dmdirc.util.StringTranscoder;
 
 import java.awt.Color;
 import java.nio.charset.Charset;
 import java.util.Date;
 import java.util.LinkedList;
 import java.util.List;
 
 /**
  * The frame container implements basic methods that should be present in
  * all objects that handle a frame.
  *
  * @author chris
  */
 public abstract class FrameContainer {
 
     /** Logger to use. */
     private static final java.util.logging.Logger LOGGER = java.util.logging
             .Logger.getLogger(FrameContainer.class.getName());
 
     /** The colour of our frame's notifications. */
     protected Color notification = Color.BLACK;
 
     /** A list of listeners for this containers's events. */
     protected final ListenerList listeners = new ListenerList();
 
     /** The document used to store this container's content. */
     protected final IRCDocument document;
 
     /** The name of the icon being used for this container's frame. */
     private String icon;
 
     /** The name of this container. */
     private String name;
 
     /** The transcoder to use for this container. */
     private StringTranscoder transcoder;
     
     /** The config manager for this container. */
     private final ConfigManager config;
     
     /** The IconChanger for this container. */
     private final IconChanger changer = new IconChanger();
 
     /** The styliser used by this container. */
     private final Styliser styliser;
 
     /**
      * Instantiate new frame container.
      * 
      * @param icon The icon to use for this container
      * @param name The name of this container
      * @param config The config manager for this container
      * @since 0.6.3m2
      */
     public FrameContainer(final String icon, final String name, final ConfigManager config) {
         this.config = config;
         this.name = name;
         this.styliser = new Styliser(this);
         this.document = new IRCDocument(this);
 
         try {
             transcoder = new StringTranscoder(Charset.forName(
                     config.getOption("channel", "encoding")));
         } catch (IllegalArgumentException ex) {
             transcoder = new StringTranscoder(Charset.forName("UTF-8"));
         }
         
         setIcon(icon);
     }
 
     /**
      * Returns the internal frame associated with this object.
      *
      * @return The internal frame associated with this object
      */
     public abstract Window getFrame();
 
     /**
      * Retrieves the {@link IRCDocument} used to store this frame's content.
      *
      * @return This frame's document
      * @since 0.6.4
      */
     public IRCDocument getDocument() {
         return document;
     }
 
     /** {@inheritDoc} */
     @Override
     public String toString() {
         return name;
     }
 
     /**
      * Retrieves the name of this container.
      *
      * @return This container's name
      * @since 0.6.3m2
      */
     public String getName() {
         return name;
     }
 
     /**
      * Changes the name of this container, and notifies any
      * {@link FrameInfoListener}s of the change.
      *
      * @param name The new name for this frame.
      */
     protected void setName(final String name) {
         this.name = name;
 
         synchronized (listeners) {
             for (FrameInfoListener listener : listeners.get(FrameInfoListener.class)) {
                 listener.nameChanged(getFrame(), name);
             }
         }
     }
 
     /**
      * Closes this container (and it's associated frame).
      */
     public void close() {
         if (getFrame() == null) {
             throw new IllegalStateException("No frame associated with this container!");
         } else {
             getFrame().close();
         }
     }
 
     /**
      * Returns the server instance associated with this container.
      *
      * @return the associated server connection
      */
     public abstract Server getServer();
 
     /**
      * Sets the icon to be used by this frame container.
      * 
      * @param icon The new icon to be used
      */
     public final void setIcon(final String icon) {
         this.icon = icon;
         
         iconUpdated();
         
         config.removeListener(changer);
         config.addChangeListener("icon", icon, changer);
     }
     
     /**
      * Called when this container's icon is updated.
      */
     private void iconUpdated() {
         synchronized (listeners) {
             for (FrameInfoListener listener : listeners.get(FrameInfoListener.class)) {
                 listener.iconChanged(getFrame(), icon);
             }
         }
     }
 
     /**
      * Retrieves the name of the icon used by this container's window.
      *
      * @return This container's icon
      */
     public final String getIcon() {
         return icon;
     }
 
     /**
      * Returns the config manager for this container.
      *
      * @return the associated config manager
      */
     public ConfigManager getConfigManager() {
         return config;
     }
 
     /**
      * Retrieves the styliser which should be used by this container.
      *
      * @return this container's styliser
      */
     public Styliser getStyliser() {
         return styliser;
     }
 
     /**
      * Requests that this object's frame be activated.
      */
     @Precondition("getFrame() does not return null")
     public void activateFrame() {
         final Window window = getFrame();
 
         if (window == null) {
             throw new IllegalStateException("Cannot activate frame '"
                     + getName() + "' while window is null");
         } else {
             window.activateFrame();
         }
     }
 
     /**
      * Clears any outstanding notifications this frame has set.
      */
     protected void clearNotification() {
         LOGGER.finer(toString() + ": clearNotification(): frame = "
                 + (getFrame() == null ? null : getFrame().getClass().getName()));
 
         // TODO: This should default ot something colour independent
         notification = Color.BLACK;
 
         synchronized (listeners) {
             for (NotificationListener listener : listeners.get(NotificationListener.class)) {
                 listener.notificationCleared(getFrame());
             }
         }
     }
 
     /**
      * Sends a notification to the frame manager if this fame isn't active.
      *
      * @param colour The colour to use for the notification
      */
     public void sendNotification(final Color colour) {
         final Window activeWindow = WindowManager.getActiveWindow();
 
         if (activeWindow != null && !activeWindow.equals(getFrame())
                 && !colour.equals(notification)) {
             notification = colour;
 
             synchronized (listeners) {
                 for (NotificationListener listener : listeners.get(NotificationListener.class)) {
                     listener.notificationSet(getFrame(), colour);
                 }
             }
         }
     }
 
     /**
      * Retrieves the current notification colour of this channel.
      *
      * @return This channel's notification colour
      */
     public Color getNotification() {
         return notification;
     }
 
     /**
      * Determines if the specified frame is owned by this object.
      *
      * @param target Window to check ownership of
      * @return True iff frame is owned by this container, false otherwise
      */
     public boolean ownsFrame(final Window target) {
         final Window window = getFrame();
         return window != null && window.equals(target);
     }
 
     /**
      * Invoked when our window has been opened.
      */
     public void windowOpened() {
         if (config == null || getFrame() == null) {
             return;
         }
     }
 
     /**
      * Invoked when our window is closing.
      * <p>
      * Frame containers must perform the following actions in this order:
      * <ol>
      *  <li>Make the window non-visible (so it appears 'closed' to the user)</li>
      *  <li>Remove any callbacks or listeners (events should not be processed
      *      once a window has been requested to close)</li>
      *  <li>Trigger any actions necessary (terminating any TCP connections,
      *      disconnecting parsers, closing children, etc)</li>
      *  <li>Trigger action for the window closing (raise a DMDirc action for
      *      the closure of the window, if required)</li>
      *  <li>Inform any parents that the window is closing (this includes
      *      unregistering the window with any specific managers, or from the
      *      parent windows if they track children)</li>
      *  <li>Remove the window from the window manager (by calling
      *      {@link WindowManager#removeWindow(com.dmdirc.ui.interfaces.Window)}</li>
      * </ol>
      * <p>
      * While resources may be relinquished in step three, references MUST NOT
      * be removed yet. That is, if a window holds a resource, the resource may
      * be closed, but the relevant object MUST still be available for
      * interrogation at the end of this method.
      * <p>
      * This behaviour is required so that parties receiving windowDeleted events
      * from the WindowManager may inspect the closing window and perform actions
      * on its frame, parser, etc. The resources should be completely freed in
      * the {@link #windowClosed()} method.
      */
     public abstract void windowClosing();
 
     /**
      * Invoked when our window has been closed.
      * <p>
      * At this point, all interested parties have been told that the window
      * has been closed, and therefore any references to frames or other
      * resources may be completely freed.
      */
     public abstract void windowClosed();
 
     /**
      * Invoked when our window is activated.
      */
     public void windowActivated() {
         LOGGER.finer(toString() + ": windowActivated(): frame = "
                 + (getFrame() == null ? null : getFrame().getClass().getName()));
 
         if (getFrame() == null) {
             return;
         }
 
         synchronized (listeners) {
             for (SelectionListener listener : listeners.get(SelectionListener.class)) {
                 listener.selectionChanged(getFrame());
             }
         }
 
         clearNotification();
 
         if (getServer() != null) {
             getServer().setActiveFrame(this);
         }
     }
 
     /**
      * Invoked when our window is deactivated.
      */
     public void windowDeactivated() {
         LOGGER.finer(toString() + ": windowDeactivated(): frame = "
                 + (getFrame() == null ? null : getFrame().getClass().getName()));
     }
 
     /**
      * Adds a line to this container's window. If the window is null for some
      * reason, the line is silently discarded.
      *
      * @param type The message type to use
      * @param args The message's arguments
      */
     public void addLine(final String type, final Object ... args) {
         if (type != null && !type.isEmpty()) {
             addLine(Formatter.formatMessage(getConfigManager(), type, args), true);
         }
     }
 
     /**
      * Adds a line to this container's window. If the window is null for some
      * reason, the line is silently discarded.
      *
      * @param type The message type to use
      * @param args The message's arguments
      */
     public void addLine(final StringBuffer type, final Object ... args) {
         if (type != null) {
             addLine(type.toString(), args);
         }
     }
 
     /**
      * Adds the specified raw line to the window, without using a formatter.
      *
      * @param line The line to be added
      * @param timestamp Whether or not to display the timestamp for this line
      */
     public void addLine(final String line, final boolean timestamp) {
         final String encodedLine = transcoder.decode(line);
         final List<String[]> lines = new LinkedList<String[]>();
         for (final String myLine : encodedLine.split("\n")) {
             if (timestamp) {
                 lines.add(new String[]{
                     Formatter.formatMessage(getConfigManager(), "timestamp", new Date()),
                     myLine,
                 });
             } else {
                 lines.add(new String[]{
                     myLine,
                 });
             }
 
             ActionManager.processEvent(CoreActionType.CLIENT_LINE_ADDED,
                     null, this, myLine);
         }

        document.addText(lines);
     }
 
     /**
      * Adds a notification listener for this frame container.
      *
      * @param listener The listener to be added
      */
     public void addNotificationListener(final NotificationListener listener) {
         synchronized (listeners) {
             listeners.add(NotificationListener.class, listener);
         }
     }
 
     /**
      * Removes a notification listener from this frame container.
      *
      * @param listener The listener to be removed
      */
     public void removeNotificationListener(final NotificationListener listener) {
         synchronized (listeners) {
             listeners.remove(NotificationListener.class, listener);
         }
     }
 
     /**
      * Adds a selection listener for this frame container.
      *
      * @param listener The listener to be added
      */
     public void addSelectionListener(final SelectionListener listener) {
         synchronized (listeners) {
             listeners.add(SelectionListener.class, listener);
         }
     }
 
     /**
      * Removes a selection listener from this frame container.
      *
      * @param listener The listener to be removed
      */
     public void removeSelectionListener(final SelectionListener listener) {
         synchronized (listeners) {
             listeners.remove(SelectionListener.class, listener);
         }
     }
 
     /**
      * Adds a frame info listener for this frame container.
      *
      * @param listener The listener to be added
      */
     public void addFrameInfoListener(final FrameInfoListener listener) {
         synchronized (listeners) {
             listeners.add(FrameInfoListener.class, listener);
         }
     }
 
     /**
      * Removes a frame info listener from this frame container.
      *
      * @param listener The listener to be removed
      */
     public void removeFrameInfoListener(final FrameInfoListener listener) {
         synchronized (listeners) {
             listeners.remove(FrameInfoListener.class, listener);
         }
     }
     
     /**
      * Updates the icon of this frame if its config setting is changed.
      */
     private class IconChanger implements ConfigChangeListener {
 
         /** {@inheritDoc} */
         @Override
         public void configChanged(final String domain, final String key) {
             iconUpdated();
         }
         
     }
 }
