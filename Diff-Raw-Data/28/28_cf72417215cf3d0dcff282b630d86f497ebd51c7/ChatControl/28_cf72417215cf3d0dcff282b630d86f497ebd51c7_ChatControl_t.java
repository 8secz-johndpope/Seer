 package de.fu_berlin.inf.dpp.ui.widgets.chatControl;
 
 import java.util.ArrayList;
 import java.util.Collection;
 import java.util.Date;
 import java.util.HashMap;
 import java.util.HashSet;
 import java.util.List;
 import java.util.Map;
 import java.util.Set;
 
 import org.apache.log4j.Logger;
 import org.eclipse.swt.SWT;
 import org.eclipse.swt.custom.CTabItem;
 import org.eclipse.swt.custom.SashForm;
 import org.eclipse.swt.events.KeyAdapter;
 import org.eclipse.swt.events.KeyEvent;
 import org.eclipse.swt.graphics.Color;
 import org.eclipse.swt.graphics.Font;
 import org.eclipse.swt.graphics.FontData;
 import org.eclipse.swt.layout.FillLayout;
 import org.eclipse.swt.widgets.Composite;
 import org.eclipse.swt.widgets.Control;
 import org.eclipse.swt.widgets.Display;
 import org.eclipse.swt.widgets.Event;
 import org.eclipse.swt.widgets.Listener;
 import org.jivesoftware.smack.XMPPException;
 import org.jivesoftware.smackx.ChatState;
 import org.picocontainer.annotations.Inject;
 
 import de.fu_berlin.inf.dpp.SarosPluginContext;
 import de.fu_berlin.inf.dpp.User;
 import de.fu_berlin.inf.dpp.communication.chat.ChatElement;
 import de.fu_berlin.inf.dpp.communication.chat.ChatElement.ChatElementType;
 import de.fu_berlin.inf.dpp.communication.chat.IChat;
 import de.fu_berlin.inf.dpp.communication.chat.IChatListener;
 import de.fu_berlin.inf.dpp.editor.annotations.SarosAnnotation;
 import de.fu_berlin.inf.dpp.net.JID;
 import de.fu_berlin.inf.dpp.net.SarosNet;
 import de.fu_berlin.inf.dpp.net.util.RosterUtils;
 import de.fu_berlin.inf.dpp.project.AbstractSarosSessionListener;
 import de.fu_berlin.inf.dpp.project.AbstractSharedProjectListener;
 import de.fu_berlin.inf.dpp.project.ISarosSession;
 import de.fu_berlin.inf.dpp.project.ISarosSessionListener;
 import de.fu_berlin.inf.dpp.project.ISarosSessionManager;
 import de.fu_berlin.inf.dpp.project.ISharedProjectListener;
 import de.fu_berlin.inf.dpp.ui.sounds.SoundManager;
 import de.fu_berlin.inf.dpp.ui.sounds.SoundPlayer;
 import de.fu_berlin.inf.dpp.ui.widgets.chatControl.events.CharacterEnteredEvent;
 import de.fu_berlin.inf.dpp.ui.widgets.chatControl.events.ChatClearedEvent;
 import de.fu_berlin.inf.dpp.ui.widgets.chatControl.events.IChatControlListener;
 import de.fu_berlin.inf.dpp.ui.widgets.chatControl.events.IChatDisplayListener;
 import de.fu_berlin.inf.dpp.ui.widgets.chatControl.events.MessageEnteredEvent;
 import de.fu_berlin.inf.dpp.ui.widgets.chatControl.parts.ChatDisplay;
 import de.fu_berlin.inf.dpp.ui.widgets.chatControl.parts.ChatInput;
 import de.fu_berlin.inf.dpp.ui.widgets.session.ChatRoomsComposite;
 import de.fu_berlin.inf.dpp.util.Utils;
 import de.fu_berlin.inf.nebula.explanation.ExplanationComposite;
 import de.fu_berlin.inf.nebula.utils.ColorUtils;
 
 /**
  * This composite displays a chat conversation and the possibility to enter
  * text.
  * <p>
  * This composite does <strong>NOT</strong> handle setting the layout and adding
  * sub {@link Control}s correctly.
  * 
  * <dl>
  * <dt><b>Styles:</b></dt>
  * <dd>BORDER and those supported by {@link ExplanationComposite}</dd>
  * <dt><b>Events:</b></dt>
  * <dd>{@link MessageEnteredEvent}</dd>
  * </dl>
  * 
  * @author bkahlert
  * 
  */
 public class ChatControl extends Composite {
     protected List<IChatControlListener> chatControlListeners = new ArrayList<IChatControlListener>();
 
     private static final Logger log = Logger.getLogger(ChatControl.class);
 
     private static final Color LOCAL_USER_DEFAULT_COLOR = Display.getDefault()
         .getSystemColor(SWT.COLOR_CYAN);
 
     private static final Color REMOTE_USER_DEFAULT_COLOR = Display.getDefault()
         .getSystemColor(SWT.COLOR_GRAY);
 
     private final Map<JID, Color> colorCache = new HashMap<JID, Color>();
 
     private ISarosSession session;
 
     @Inject
     protected SarosNet sarosNet;
 
     @Inject
     protected ISarosSessionManager sessionManager;
 
     /**
      * This {@link IChatDisplayListener} is used to forward events fired in the
      * {@link ChatDisplay} so the user only has to add listeners on the
      * {@link ChatControl} and not on all its child components.
      */
     protected IChatDisplayListener chatDisplayListener = new IChatDisplayListener() {
         public void chatCleared(ChatClearedEvent event) {
             clearColorCache();
 
             ChatControl.this.chat.clearHistory();
             ChatControl.this.notifyChatCleared(event);
         }
     };
 
     /**
      * This {@link KeyAdapter} is used to forward events fired in the
      * {@link ChatInput} so the user only has to add listeners on the
      * {@link ChatControl} and not on all its child components.
      */
     protected KeyAdapter chatInputListener = new KeyAdapter() {
         @Override
         public void keyPressed(KeyEvent e) {
             switch (e.keyCode) {
             case SWT.CR:
             case SWT.KEYPAD_CR:
                 String message = getInputText().trim();
 
                 if (message.length() > 0) {
                     ChatControl.this.notifyMessageEntered(message);
 
                     sendMessage(message);
                 }
 
                 e.doit = false;
             }
         }
 
         @Override
         public void keyReleased(KeyEvent e) {
             switch (e.keyCode) {
             case SWT.CR:
             case SWT.KEYPAD_CR:
                 /*
                  * We do not want the ENTER to be inserted
                  */
                 e.doit = false;
                 return;
             default:
                 determineCurrentState();
                 break;
             }
 
             ChatControl.this.notifyCharacterEntered(e.character);
         }
     };
 
     private ISarosSessionListener sessionListener = new AbstractSarosSessionListener() {
 
         @Override
         public void sessionStarted(ISarosSession newSarosSession) {
             synchronized (ChatControl.this) {
                 if (session != null)
                     session.removeListener(projectListener);
 
                 session = newSarosSession;
                 session.addListener(projectListener);
             }
 
             // The chat contains the pre-session colors. Refresh it, to clear
             // the cache and use the in-session colors.
             updateColorsInSWTAsync();
         }
 
         @Override
         public void sessionEnding(ISarosSession oldSarosSession) {
             synchronized (ChatControl.this) {
                 session.removeListener(projectListener);
                 session = null;
             }
 
             // The chat contains the in-session colors. Refresh it, to clear the
             // color cache and use the pre-session colors again.
             updateColorsInSWTAsync();
         }
     };
 
     private ISharedProjectListener projectListener = new AbstractSharedProjectListener() {
 
         @Override
         public void userJoined(User user) {
             updateColorsInSWTAsync();
         }
     };
 
     private IChatListener chatListener = new IChatListener() {
 
         @Override
         public void messageReceived(final JID sender, final String message) {
             Utils.runSafeSWTAsync(log, new Runnable() {
 
                 @Override
                 public void run() {
                     if (ChatControl.this.isDisposed()) {
                         chat.removeChatListener(chatListener);
                         chatRooms.openChat(chat, false);
                         return;
                     }
 
                     addChatLine(new ChatElement(message, sender, new Date()));
 
                     if (!isOwnJID(sender)) {
                         SoundPlayer.playSound(SoundManager.MESSAGE_RECEIVED);
                         incrementUnseenMessages();
                     } else {
                         SoundPlayer.playSound(SoundManager.MESSAGE_SENT);
                     }
                 }
             });
 
         }
 
         @Override
         public void stateChanged(final JID jid, final ChatState state) {
             Utils.runSafeSWTAsync(log, new Runnable() {
                 @Override
                 public void run() {
                    if (ChatControl.this.isDisposed())
                        return;

                    if (isOwnJID(jid))
                         return;
 
                     CTabItem tab = chatRooms.getChatTab(chat);
 
                     if (state == ChatState.composing) {
                         tab.setImage(ChatRoomsComposite.composingImage);
                     } else {
                         tab.setImage(ChatRoomsComposite.chatViewImage);
                     }
                 }
             });
         }
 
         @Override
         public void connected(final JID jid) {
             Utils.runSafeSWTAsync(null, new Runnable() {
 
                 @Override
                 public void run() {
                    if (ChatControl.this.isDisposed())
                        return;

                     addChatLine(new ChatElement(jid, new Date(),
                         ChatElementType.JOIN));
 
                    if (isOwnJID(jid))
                         chatInput.setEnabled(true);
                 }
             });
         }
 
         @Override
         public void disconnected(final JID jid) {
             Utils.runSafeSWTAsync(null, new Runnable() {
 
                 @Override
                 public void run() {
                    if (ChatControl.this.isDisposed())
                        return;

                     addChatLine(new ChatElement(jid, new Date(),
                         ChatElementType.LEAVE));
 
                    if (isOwnJID(jid))
                         chatInput.setEnabled(false);
                 }
             });
         }
 
     };
 
     /**
      * Chat layer
      */
     protected SashForm sashForm;
     protected ChatRoomsComposite chatRooms;
     protected ChatDisplay chatDisplay;
     protected ChatInput chatInput;
     protected IChat chat;
     protected int missedMessages;
 
     public ChatControl(ChatRoomsComposite chatRooms, IChat chat,
         Composite parent, int style, Color displayBackgroundColor,
         final int minVisibleInputLines) {
         super(parent, style & ~SWT.BORDER);
 
         SarosPluginContext.initComponent(this);
 
         int chatDisplayStyle = (style & SWT.BORDER) | SWT.V_SCROLL
             | SWT.H_SCROLL;
         int chatInputStyle = (style & SWT.BORDER) | SWT.MULTI | SWT.V_SCROLL
             | SWT.WRAP;
 
         this.setLayout(new FillLayout());
 
         this.sashForm = new SashForm(this, SWT.VERTICAL);
 
         // ChatDisplay
         this.chatDisplay = new ChatDisplay(sashForm, chatDisplayStyle,
             displayBackgroundColor);
         this.chatDisplay.setAlwaysShowScrollBars(true);
         this.chatDisplay.addChatDisplayListener(this.chatDisplayListener);
 
         // ChatInput
         this.chatInput = new ChatInput(sashForm, chatInputStyle);
         this.chatInput.addKeyListener(this.chatInputListener);
         this.chatInput.setEnabled(true);
 
         /*
          * Updates SashForm weights to emulate a fixed ChatInput height
          */
         this.addListener(SWT.Resize, new Listener() {
             public void handleEvent(Event event) {
                 int fullHeight = ChatControl.this.getSize().y;
                 int chatInputHeight = ChatControl.this.chatInput.getSize().y;
                 int lineHeight = (int) Math.round(chatInput.getFont()
                     .getFontData()[0].getHeight() * 1.4);
                 int minChatInputHeight = minVisibleInputLines * lineHeight;
                 if (chatInputHeight < minChatInputHeight) {
                     chatInputHeight = minChatInputHeight;
                 }
 
                 int newChatDisplayHeight = fullHeight - chatInputHeight;
 
                 if (newChatDisplayHeight <= 0 || chatInputHeight <= 0)
                     return;
 
                 sashForm.setWeights(new int[] { newChatDisplayHeight,
                     chatInputHeight });
             }
         });
 
         this.chatRooms = chatRooms;
 
         this.chat = chat;
         this.chat.addChatListener(chatListener);
 
         synchronized (this) {
             this.session = sessionManager.getSarosSession();
             this.sessionManager.addSarosSessionListener(sessionListener);
 
             if (this.session != null)
                 this.session.addListener(projectListener);
         }
 
         for (ChatElement chatElement : this.chat.getHistory()) {
             addChatLine(chatElement);
         }
 
         this.missedMessages = 0;
 
         Listener showListener = new Listener() {
             public void handleEvent(Event event) {
                 resetUnseenMessages();
             }
         };
 
         this.addListener(SWT.Show, showListener);
     }
 
     public boolean isOwnJID(JID jid) {
         return jid.equals(chat.getJID());
     }
 
     /**
      * Updates the colors for the current chat contents.
      */
     public void updateColors() {
 
         clearColorCache();
 
         for (JID jid : getChatJIDsFromHistory())
             chatDisplay.updateColor(jid, getColorForJID(jid));
     }
 
     /**
      * Updates the display names for the current chat contents.
      */
     public void updateDisplayNames() {
         for (JID jid : getChatJIDsFromHistory())
             chatDisplay.updateDisplayName(jid, getNickname(jid));
 
         // TODO: this currently scrolls to the bottom
         chatDisplay.refresh();
     }
 
     @Override
     public void dispose() {
         super.dispose();
         sessionManager.removeSarosSessionListener(sessionListener);
         synchronized (this) {
             if (session != null)
                 session.removeListener(projectListener);
         }
         clearColorCache();
     }
 
     /**
      * Clears the color cache and disposes the stored colors.
      */
     private void clearColorCache() {
         for (Map.Entry<JID, Color> entry : colorCache.entrySet()) {
             entry.getValue().dispose();
         }
         colorCache.clear();
     }
 
     public void addChatLine(ChatElement element) {
         /*
          * FIXME: MUC JIDs are returned with perspective
          * saros419397963@conference
          * .saros-con.imp.fu-berlin.de/jenkins_bob_stf@saros
          * -con.imp.fu-berlin.de/Saros
          * 
          * which will become jenkins_bob_stf@saros-con.imp.fu-berlin.de/Saros
          * after getBareJID() and this is not the BARE JID!
          */
         JID jid = element.getSender().getBareJID();
         Color color = getColorForJID(jid);
         chatDisplay.addChatLine(jid, getNickname(jid), color,
             element.toString(), element.getDate());
     }
 
     /**
      * Retrieves the color for a the JID. If the JID is used by a user in the
      * currently running Saros session its session color will be returned.
      * Otherwise a default color is returned.
      * 
      */
     protected Color getColorForJID(JID jid) {
         Color color = colorCache.get(jid);
         if (color == null) {
             synchronized (ChatControl.this) {
 
                 User user = null;
                 if (session != null) {
                     JID resourceQualifiedJID = session
                         .getResourceQualifiedJID(jid);
 
                     if (resourceQualifiedJID != null)
                         user = session.getUser(resourceQualifiedJID);
                 }
 
                 if (user != null) {
                     // add default lightness to cached color
                     Color userColor = SarosAnnotation.getUserColor(user);
                     color = ColorUtils.addLightness(userColor,
                         SarosAnnotation.getLightnessScale());
                     userColor.dispose();
 
                     colorCache.put(jid, color);
                 } else if (isOwnJID(jid)) {
                     color = LOCAL_USER_DEFAULT_COLOR;
                 } else {
                     color = REMOTE_USER_DEFAULT_COLOR;
                 }
             }
         }
         return color;
     }
 
     private String getNickname(JID jid) {
         String name = RosterUtils.getNickname(sarosNet, jid);
 
         if (name == null)
             name = jid.getBase().toString();
 
         return name;
     }
 
     /**
      * Sets the chat input's text
      * 
      * @param string
      *            the new text
      */
     public void setInputText(String string) {
         chatInput.setText(string);
     }
 
     /**
      * Return entered text in the chat input
      * 
      * @return the entered text
      */
     public String getInputText() {
         return this.chatInput.getText();
     }
 
     /**
      * Sends message if there is any input to send.
      */
     private void sendMessage(String message) {
 
         if (message.length() != 0) {
             try {
                 chat.sendMessage(message);
                 chat.setCurrentState(ChatState.inactive);
                 setInputText("");
             } catch (Exception exception) {
                 addChatLine(new ChatElement("error while sending message: "
                     + exception.getMessage(), chat.getJID(), new Date()));
             }
         }
     }
 
     private void determineCurrentState() {
         try {
             chat.setCurrentState(getInputText().isEmpty() ? ChatState.inactive
                 : ChatState.composing);
         } catch (XMPPException ex) {
             log.error(ex.getMessage(), ex);
         }
     }
 
     /**
      * Adds a {@link IChatControlListener}
      * 
      * @param chatControlListener
      */
     public void addChatControlListener(IChatControlListener chatControlListener) {
         this.chatControlListeners.add(chatControlListener);
     }
 
     /**
      * Removes a {@link IChatControlListener}
      * 
      * @param chatControlListener
      */
     public void removeChatControlListener(
         IChatControlListener chatControlListener) {
         this.chatControlListeners.remove(chatControlListener);
     }
 
     /**
      * Notify all {@link IChatControlListener}s about entered character
      * 
      * @param character
      *            the entered character
      */
     public void notifyCharacterEntered(Character character) {
         for (IChatControlListener chatControlListener : this.chatControlListeners) {
             chatControlListener.characterEntered(new CharacterEnteredEvent(
                 this, character));
         }
     }
 
     /**
      * Notify all {@link IChatControlListener}s about entered text
      * 
      * @param message
      *            the entered text
      */
     public void notifyMessageEntered(String message) {
         for (IChatControlListener chatControlListener : this.chatControlListeners) {
             chatControlListener.messageEntered(new MessageEnteredEvent(this,
                 message));
         }
     }
 
     /**
      * Notify all {@link IChatDisplayListener}s about a cleared chat
      */
     public void notifyChatCleared(ChatClearedEvent event) {
         for (IChatControlListener chatControlListener : this.chatControlListeners) {
             chatControlListener.chatCleared(event);
         }
     }
 
     protected void toggleChatBoldFontStyle() {
         FontData[] fds = chatRooms.getChatTab(chat).getFont().getFontData();
         if (fds.length > 0) {
             chatRooms.getChatTab(chat).setFont(
                 new Font(getDisplay(), fds[0].getName(), fds[0].getHeight(),
                     fds[0].getStyle() ^ SWT.BOLD));
         }
     }
 
     protected void incrementUnseenMessages() {
         if (!chatRooms.isVisible()
             || chatRooms.getSelectedChatControl() != this) {
 
             if (missedMessages == 0) {
                 toggleChatBoldFontStyle();
             }
             missedMessages++;
             chatRooms.getChatTab(chat).setText(
                 "(" + missedMessages + ") " + chat.getTitle());
         }
     }
 
     protected void resetUnseenMessages() {
         if (missedMessages > 0) {
             toggleChatBoldFontStyle();
             missedMessages = 0;
             chatRooms.getChatTab(chat).setText(chat.getTitle());
         }
     }
 
     /**
      * @see ChatDisplay#clear()
      */
     public void clear() {
         chatDisplay.clear();
     }
 
     /**
      * @see ChatDisplay#silentClear()
      */
     public void silentClear() {
         chatDisplay.silentClear();
     }
 
     @Override
     public boolean setFocus() {
         return chatInput.setFocus();
     }
 
     /**
      * Makes sure refreshing the chat is done in the SWT thread. Performed
      * asynchronously to prevent dead locks.
      */
     private void updateColorsInSWTAsync() {
         Utils.runSafeSWTAsync(log, new Runnable() {
             @Override
             public void run() {
                if (isDisposed())
                    return;
                 updateColors();
             }
         });
     }
 
     private Collection<JID> getChatJIDsFromHistory() {
         /*
          * FIXME: MUC JIDs are returned with perspective
          * saros419397963@conference
          * .saros-con.imp.fu-berlin.de/jenkins_bob_stf@saros
          * -con.imp.fu-berlin.de/Saros
          * 
          * which will become jenkins_bob_stf@saros-con.imp.fu-berlin.de/Saros
          * after getBareJID() and this is not the BARE JID!
          */
 
         Set<JID> jids = new HashSet<JID>();
 
         for (ChatElement element : chat.getHistory())
             jids.add(element.getSender().getBareJID());
 
         return jids;
     }
 }
