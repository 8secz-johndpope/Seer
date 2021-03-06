 /*
  * Contact.java
  *
  * Created on 6.01.2005, 19:16
  * Copyright (c) 2005-2008, Eugene Stahov (evgs), http://bombus-im.org
  *
  * This program is free software; you can redistribute it and/or
  * modify it under the terms of the GNU General Public License
  * as published by the Free Software Foundation; either version 2
  * of the License, or (at your option) any later version.
  *
  * You can also redistribute and/or modify this program under the
  * terms of the Psi License, specified in the accompanied COPYING
  * file, as published by the Psi Project; either dated January 1st,
  * 2005, or (at your option) any later version.
  *
  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  * GNU General Public License for more details.
  *
  * You should have received a copy of the GNU General Public License
  * along with this library; if not, write to the Free Software
  * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
  *
  */
 package Client;
 
 //#ifndef WMUC
 import xmpp.Jid;
 import Conference.MucContact;
 //#endif
 import Fonts.FontCache;
 //#ifdef CLIENTS_ICONS
 //# import images.ClientsIcons;
 //#endif
 //#if HISTORY
 //# import History.HistoryAppend;
 //#endif
 import javax.microedition.lcdui.Graphics;
 import ui.ImageList;
 //#ifdef PEP
 //# import images.MoodIcons;
 //# import PEP.Moods;
 //#endif
 import images.RosterIcons;
 import Colors.ColorTheme;
 import Messages.MessageItem;
 import VCard.VCard;
 import ui.IconTextElement;
 import com.alsutton.jabber.datablocks.Presence;
 import java.util.Enumeration;
 import java.util.Vector;
 import locale.SR;
 import xmpp.JidUtils;
 
 public class Contact extends IconTextElement {
 
 //#if USE_ROTATOR
 //#     private int isnew = 0;
 //# 
 //#     public void setNewContact() {
 //#         this.isnew = 8;
 //#     }
 //#endif
 //#ifdef PEP    
 //#     public int pepMood = -1;
 //#     public String pepMoodName = null;
 //#     public String pepMoodText = null;
 //#ifdef PEP_TUNE
 //#     public boolean pepTune;
 //#     public String pepTuneText = null;
 //#endif
 //#ifdef PEP_ACTIVITY
 //#     public String activity = null;
 //#endif
 //#ifdef PEP_LOCATION
 //#     public String location = null;
 //#endif
 //#endif
     public final static short ORIGIN_ROSTER = 0;
     public final static short ORIGIN_ROSTERRES = 1;
     public final static short ORIGIN_CLONE = 2;
     public final static short ORIGIN_PRESENCE = 3;
     public final static short ORIGIN_GROUPCHAT = 4;
 //#ifndef WMUC
     public final static short ORIGIN_GC_MEMBER = 5;
     public final static short ORIGIN_GC_MYSELF = 6;
 //#endif
     public String nick;
     public Jid jid;
     public int status;
     public int priority;
     public Group group;
     public int transport;
     public boolean autoresponded = false;
     public boolean moveToLatest = false;
     public String presence;
     public String statusString;
     public boolean acceptComposing;
     public boolean showComposing = false;
     public short deliveryType;
     public short incomingState = INC_NONE;
     public final static short INC_NONE = 0;
     public final static short INC_APPEARING = 1;
     public final static short INC_VIEWING = 2;
     protected short key0;
     protected String key1;
     public byte origin;
     public String subscr;
     public int offline_type = Presence.PRESENCE_UNKNOWN;
     public boolean ask_subscribe;
     public final Vector msgs;
 //#ifdef STATUSES_WINDOW    
 //#     public final Vector statuses = new Vector();
 //#endif    
     public int activeMessage = -1;
     private int newMsgCnt = 0;
     private int newHighLitedMsgCnt = 0;
     public int unreadType;
     public int lastUnread;
     public int mark = -1;
     public String msgSuspended;
     public String lastSendedMessage;
     public VCard vcard;
 //#ifdef CLIENTS_ICONS
 //#     public int client = -1;
 //#     public String clientName = null;
 //#endif
 //#ifdef LOGROTATE
 //#     public boolean redraw = false;
 //#endif
     public String j2j;
     public String lang;
     public String version;
 //#ifdef FILE_TRANSFER
 //#     public boolean fileQuery;
 //#endif
 //#ifdef HISTORY
 //#ifdef LAST_MESSAGES
 //#     private boolean loaded;
 //#endif
 //#endif
     StaticData sd = StaticData.getInstance();
     //private Font secondFont; //Issue 88
     //private int secondFontHeight;
     private int fontHeight;
     int ilHeight;
     int maxImgHeight;
     private boolean smiles = false;
     ContactMessageList cml = null;
 
     protected Contact() {
         super(RosterIcons.getInstance());
         cf = Config.getInstance();
 //#ifdef SMILES
 //#         smiles = cf.smiles;
 //#endif
 
         msgs = new Vector();
 
         key1 = "";
 
         ilHeight = il.getHeight();
         maxImgHeight = ilHeight;
 
         //secondFont=FontCache.getFont(false, FontCache.baloon);
         //secondFontHeight=secondFont.getHeight();
         fontHeight = getFont().getHeight();
     }
 
     public Contact(final String Nick, final String sJid, final int Status, String subscr) {
         this();
         nick = Nick;        
         jid = new Jid(sJid);
         
         if (nick == null || nick.length() == 0) {
             nick = jid.getNode();
         }
         status = Status;
 
         this.subscr = subscr;
 
         setSortKey((Nick == null) ? sJid : Nick);
 
         //calculating transport
         transport = RosterIcons.getInstance().getTransportIndex(JidUtils.getTransport(jid));
     }
 
     public Contact clone(Jid newjid, final int status) {
         Contact clone = new Contact();
         clone.group = group;
         clone.jid = newjid;
         clone.nick = nick;
         clone.key1 = key1;
         clone.subscr = subscr;
         clone.offline_type = offline_type;
         clone.origin = ORIGIN_CLONE;
         clone.status = status;
         clone.transport = RosterIcons.getInstance().getTransportIndex(JidUtils.getTransport(newjid)); //<<<<
 //#ifdef PEP
 //#         clone.pepMood = pepMood;
 //#         clone.pepMoodName = pepMoodName;
 //#         clone.pepMoodText = pepMoodText;
 //#ifdef PEP_TUNE
 //#         clone.pepTune = pepTune;
 //#         clone.pepTuneText = pepTuneText;
 //#endif
 //#ifdef PEP_ACTIVITY
 //#         clone.activity = activity;
 //#endif
 //#ifdef PEP_LOCATION
 //#         clone.location = location;
 //#endif
 //# 
 //#endif
         return clone;
     }
 
     public int getColor() {
 //#if USE_ROTATOR        
 //#         if (isnew > 0) {
 //#             isnew--;
 //#             return (isnew % 2 == 0) ? 0xFF0000 : 0x0000FF;
 //#         }
 //#endif
         if (j2j != null) {
             return ColorTheme.getColor(ColorTheme.CONTACT_J2J);
         }
 
         return getMainColor();
     }
 
     public int getMainColor() {
         switch (status) {
             case Presence.PRESENCE_CHAT:
                 return ColorTheme.getColor(ColorTheme.CONTACT_CHAT);
             case Presence.PRESENCE_AWAY:
                 return ColorTheme.getColor(ColorTheme.CONTACT_AWAY);
             case Presence.PRESENCE_XA:
                 return ColorTheme.getColor(ColorTheme.CONTACT_XA);
             case Presence.PRESENCE_DND:
                 return ColorTheme.getColor(ColorTheme.CONTACT_DND);
         }
         return ColorTheme.getColor(ColorTheme.CONTACT_DEFAULT);
     }
 
     public boolean haveChatMessages() {
         for (Enumeration e = msgs.elements(); e.hasMoreElements();) {
             Msg msg = ((MessageItem) e.nextElement()).msg;
             if (msg.messageType == Msg.MESSAGE_TYPE_IN || msg.messageType == Msg.MESSAGE_TYPE_OUT || msg.messageType == Msg.MESSAGE_TYPE_AUTH) {
                 return true;
             }
         }
         return false;
     }
 
     public int getNewMsgsCount() {
         if (msgs.isEmpty()) {
             return 0;
         }
         /*  if (newMsgCnt > 0) {
         return newMsgCnt;
         }*/
         int nm = 0;
         if (getGroupType() != Groups.TYPE_IGNORE) {
             unreadType = Msg.MESSAGE_TYPE_IN;
 
             for (Enumeration e = msgs.elements(); e.hasMoreElements();) {
                 Msg m = ((MessageItem) e.nextElement()).msg;
                 if (m.unread) {
                     nm++;
                     if (m.messageType == Msg.MESSAGE_TYPE_AUTH) {
                         unreadType = m.messageType;
                     }
                 }
             }
         }
         return newMsgCnt = nm;
     }
     
     public final boolean hasNewMsgs() {
         return getNewMsgsCount() > 0;
     }
 
     public int getNewHighliteMsgsCount() {
         if (newHighLitedMsgCnt > 0) {
             return newHighLitedMsgCnt;
         }
         int nm = 0;
         if (getGroupType() != Groups.TYPE_IGNORE) {
             for (Enumeration e = msgs.elements(); e.hasMoreElements();) {
                 Msg m = ((MessageItem) e.nextElement()).msg;
                 if (m.unread && m.highlite) {
                     nm++;
                 }
             }
         }
         return newHighLitedMsgCnt = nm;
     }
 
     public boolean active() {
         if (msgSuspended != null) {
             return true;
         }
         return (activeMessage > -1);
     }
 
     public void resetNewMsgCnt() {
         newMsgCnt = 0;
         newHighLitedMsgCnt = 0;
     }
 
     public void setIncoming(int state) {
         if (!cf.IQNotify && state == INC_VIEWING) {
             return;
         }
 
         short i = 0;
         switch (state) {
             case INC_APPEARING:
                 i = RosterIcons.ICON_APPEARING_INDEX;
                 break;
             case INC_VIEWING:
                 i = RosterIcons.ICON_VIEWING_INDEX;
                 break;
         }
         incomingState = i;
     }
 
     public int compare(IconTextElement right) {
         Contact c = (Contact) right;
         int cmp;
         if ((cmp = key0 - c.key0) != 0) {
             return cmp;
         }
         if ((cmp = status - c.status) != 0) {
             return cmp;
         }
         if ((cmp = key1.compareTo(c.key1)) != 0) {
             return cmp;
         }
         if ((cmp = c.priority - priority) != 0) {
             return cmp;
         }
         return c.transport - transport;
     }       
 
     public void addMessage(Msg m) {
         boolean last_replace = false;
         if (origin == ORIGIN_GROUPCHAT) {
             if (!m.body.startsWith("/me ")) {
                 if (cf.showNickNames && !m.isPresence() && m.messageType != Msg.MESSAGE_TYPE_SUBJ && m.messageType != Msg.MESSAGE_TYPE_SYSTEM) {
                     StringBuffer who = new StringBuffer();
                     Msg.appendNick(who, m.from + ((cf.hideTimestamps) ? ":" : " ("+m.getTime() + ")" + ":"));
                     if (m.subject != null) {
                         who.append("\n").append(m.subject);
                     }
                     m.subject = who.toString();
                 }
             }
             if (m.body.startsWith("/me ")) {
                 StringBuffer b = new StringBuffer();
                 Msg.appendNick(b, m.from);
                 b.insert(0, '*');
                 b.append(m.body.substring(3));
                 m.body = b.toString();
                 b = null;
             }
             status = Presence.PRESENCE_ONLINE;
 //#ifdef LOGROTATE
 //#             redraw = deleteOldMessages();
 //#endif			
         }
 //#ifdef STATUSES_WINDOW        
 //#         if (m.isPresence()) {
 //#             statuses.addElement(new MessageItem(m, smiles));
 //#         }
 //#endif        
         if (origin != ORIGIN_GROUPCHAT) {
             if (msgs.size() > 0 && m.isPresence()) {
                 Object item = msgs.lastElement();
                 if (item != null) {
                     if (((MessageItem) item).msg.isPresence()) {
                         last_replace = true;
                     }
                 }
             } else {
                 if (!m.body.startsWith("/me ")) {
                     if (cf.showNickNames && !m.isPresence()) {
                         StringBuffer who = new StringBuffer();
                         Msg.appendNick(who, m.messageType == Msg.MESSAGE_TYPE_OUT ? sd.account.getNickName() + ((cf.hideTimestamps) ? ":" : " (" + m.getTime() + ")" + ":") : getName() + ((cf.hideTimestamps) ? ":" : " ("+m.getTime() + ")" + ":"));
                         if (m.subject != null) {
                             who.append("\n").append(m.subject);
                         }
                         m.subject = who.toString();
                     }
                 } else { // if (m.body.startsWith("/me "))
                     StringBuffer b = new StringBuffer();
                     Msg.appendNick(b, (m.messageType == Msg.MESSAGE_TYPE_OUT) ? sd.account.getNickName() : getName());
                     b.insert(0, '*');
                     b.append(m.body.substring(3));
                     m.body = b.toString();
                 }
             }
         } else {
             status = Presence.PRESENCE_ONLINE;
 //#ifdef LOGROTATE
 //#             redraw = deleteOldMessages();
 //#endif
         }
 //#if HISTORY
 //#         if (!m.history) {
 //#             if (!cf.msgPath.equals("") && !JidUtils.isTransport(jid) && group.type != Groups.TYPE_SEARCH_RESULT) {
 //#                 boolean allowLog = false;
 //#                 switch (m.messageType) {
 //#                     case Msg.MESSAGE_TYPE_PRESENCE:
 //#                         if (origin >= ORIGIN_GROUPCHAT) {
 //#                             if (cf.msgLogConfPresence) {
 //#                                 allowLog = true;
 //#                             }
 //#                         } else if (cf.msgLogPresence) {
 //#                             allowLog = true;
 //#                         }
 //#                         break;
 //#                     case Msg.MESSAGE_TYPE_HISTORY:
 //#                         break;
 //#                     default:
 //#                         if (origin >= ORIGIN_GROUPCHAT && cf.msgLogConf) {
 //#                             allowLog = true;
 //#                         }
 //#                         if (origin < ORIGIN_GROUPCHAT && cf.msgLog) {
 //#                             allowLog = true;
 //#                         }
 //#                 }
 //# 
 //#ifndef WMUC
 //#                 if (origin != ORIGIN_GROUPCHAT && this instanceof MucContact) {
 //#                     allowLog = false;
 //#                 }
 //#endif
 //# 
 //#                 if (allowLog) {
 //#                     HistoryAppend.getInstance().addMessage(m, jid.getBare());
 //#                 }
 //#             }
 //#         }
 //#endif
         if (last_replace) {
             msgs.setElementAt(new MessageItem(m, smiles), msgs.size() - 1);
             return;
         }
 
         if (m.messageType != Msg.MESSAGE_TYPE_HISTORY && m.messageType != Msg.MESSAGE_TYPE_PRESENCE) {
             activeMessage = msgs.size();
         }
 
         msgs.addElement(new MessageItem(m, smiles));
 
         if (m.unread || m.messageType == Msg.MESSAGE_TYPE_OUT) {
             lastUnread = msgs.size();
             if (m.messageType > unreadType) {
                 unreadType = m.messageType;
             }
             if (newMsgCnt >= 0) {
                 newMsgCnt++;
             }
             if (m.highlite) {
                 if (newHighLitedMsgCnt >= 0) {
                     newHighLitedMsgCnt++;
                 }
             }
         }
     }
 
     public int getFontIndex() {
         if (cf.showResources) {
             return (cf.useBoldFont && status < 5) ? 1 : 0;
         }
 
         return active() ? 1 : 0;
     }
 
     public final String getName() {
         return (nick == null) ? jid.getBare() : nick;
     }
 
     public final Jid getJid() {
         return jid;
     }
 
     public String getResource() {
         return jid.resource;
     }
 
     public String getNickJid() {
         if (nick == null) {
             return jid.getBare();
         }
         return nick + " <" + jid.getBare() + ">";
     }
 
     public final void purge() {
         msgs.removeAllElements();
 //#ifdef STATUSES_WINDOW
 //#         statuses.removeAllElements();
 //#endif        
         lastSendedMessage = null;
         activeMessage = -1; //drop activeMessage num
 
         resetNewMsgCnt();
 
         clearVCard();
     }
 
     public final void clearVCard() {
         try {
             if (vcard != null) {
                 vcard.clearVCard();
                 vcard = null;
             }
         } catch (Exception e) {
         }
     }
 
 //#ifdef LOGROTATE
 //#     public final boolean deleteOldMessages() {
 //#         int limit = cf.msglistLimit;
 //#         if (msgs.size() < limit) {
 //#             return false;
 //#         }
 //# 
 //#         int trash = msgs.size() - limit;
 //#         for (int i = 0; i < trash; i++) {
 //#             msgs.removeElementAt(0);
 //#         }
 //# 
 //#         return true;
 //#     }
 //#endif
 
     public final void setSortKey(String sortKey) {
         key1 = (sortKey == null) ? "" : sortKey.toLowerCase();
     }
 
     public String getTipString() {
         int nm = getNewMsgsCount();
         if (nm != 0) {
             return String.valueOf(nm);
         }
         StringBuffer mess = new StringBuffer();
 
 //#ifndef WMUC
         boolean isMucContact = (this instanceof MucContact);
         if (isMucContact) {
             MucContact mucContact = (MucContact) this;
 
             if (mucContact.origin != Contact.ORIGIN_GROUPCHAT) {
                 mess.append((mucContact.realJid == null) ? "" : "jid: " + mucContact.realJid + "\n");
 
                 if (mucContact.affiliationCode > MucContact.AFFILIATION_NONE) {
                     mess.append(MucContact.getAffiliationLocale(mucContact.affiliationCode));
                 }
 
                 if (!(mucContact.roleCode == MucContact.ROLE_PARTICIPANT && mucContact.affiliationCode == MucContact.AFFILIATION_MEMBER)) {
                     if (mucContact.affiliationCode > MucContact.AFFILIATION_NONE) {
                         mess.append(SR.MS_AND);
                     }
                     mess.append(MucContact.getRoleLocale(mucContact.roleCode));
                 }
             }
         } else {
 //#endif
             mess.append("jid: ").append(jid.toString()).append("\n").append(SR.MS_SUBSCRIPTION).append(": ").append(subscr);
 //#ifdef PEP
 //#             if (hasMood()) {
 //#                 mess.append("\n").append(SR.MS_USERMOOD).append(": ").append(getMoodString());
 //#             }
 //#ifdef PEP_ACTIVITY
 //#             if (hasActivity()) {
 //#                 mess.append("\n").append(SR.MS_USERACTIVITY).append(": ").append(activity);
 //#             }
 //#endif
 //#ifdef PEP_LOCATION
 //#             if (hasLocation()) {
 //#                 mess.append("\n").append(SR.MS_USERLOCATION).append(": ").append(location);
 //#             }
 //#endif
 //# 
 //#ifdef PEP_TUNE
 //#             if (pepTune) {
 //#                 mess.append("\n").append(SR.MS_USERTUNE);
 //#                 if (!pepTuneText.equals("")) {
 //#                     mess.append(": ").append(pepTuneText);
 //#                 }
 //#             }
 //#endif
 //#endif
 //#ifndef WMUC
         }
 //#endif
         if (origin != Contact.ORIGIN_GROUPCHAT) {
             mess.append((j2j != null) ? "\nJ2J: " + j2j : "");
 //#ifdef CLIENTS_ICONS
 //#             if (client > -1) {
 //#                 mess.append("\n").append(SR.MS_USE).append(": ").append(clientName);
 //#             }
 //#endif
             if (version != null) {
                 mess.append("\n").append(SR.MS_VERSION).append(": ").append(version);
             }
             if (lang != null) {
                 mess.append("\n").append(SR.MS_LANGUAGE).append(": ").append(lang);
             }
         }
 
         if (statusString != null) {
             if (origin != Contact.ORIGIN_GROUPCHAT) {
                 mess.append("\n").append(SR.MS_STATUS).append(": ");
             }
             mess.append(statusString);
 
             if (priority != 0) {
                 mess.append(" [").append(priority).append("]");
             }
         }
         return mess.toString();
 
     }
 
     public int getGroupType() {
         if (group == null) {
             return 0;
         }
         return group.type;
     }
 
     public void setStatus(int status) {
         setIncoming(0);
         this.status = status;
         if (status >= Presence.PRESENCE_OFFLINE) {
             acceptComposing = false;
         }
     }
 
     public void markDelivered(String id) {
         if (id == null) {
             return;
         }
         for (Enumeration e = msgs.elements(); e.hasMoreElements();) {
             Msg m = ((MessageItem) e.nextElement()).msg;
             if (m.id != null) {
                 if (m.id.equals(id)) {
                     m.delivered = true;
                 }
             }
         }
     }
 
 //#ifdef HISTORY
 //#ifdef LAST_MESSAGES
 //#     public boolean isHistoryLoaded() {
 //#         return loaded;
 //#     }
 //# 
 //#     public void setHistoryLoaded(boolean state) {
 //#         loaded = state;
 //#     }
 //#endif
 //#endif
 
     public int getVWidth() {
         String str = (!cf.rosterStatus) ? getFirstString() : (getFirstLength() > getSecondLength()) ? getFirstString() : getSecondString();
         int wft = getFont().stringWidth(str);
 
         return wft + il.getWidth() + 4;
     }
 
     public String toString() {
         return getFirstString();
     }
 
     public int getSecondLength() {
         if (getSecondString() == null) {
             return 0;
         }
         if (getSecondString().length() == 0) {
             return 0;
         }
         return FontCache.getFont(false, FontCache.baloon).stringWidth(getSecondString());
     }
 
     public int getFirstLength() {
         if (getFirstString() == null) {
             return 0;
         }
         if (getFirstString().length() == 0) {
             return 0;
         }
         return getFont().stringWidth(getFirstString());
     }
 
     public String getFirstString() {
         if (!cf.showResources) {
             return (nick == null) ? jid.getBare() : nick;
         }
         if (origin > ORIGIN_GROUPCHAT) {
             return nick;
         }
         if (origin == ORIGIN_GROUPCHAT) {
             return getJid().toString();
         }
 
        return (nick == null) ? getJid().toString() : jid.resource.length() > 0 ?
                nick + "/" + jid.resource
                : nick;
     }
 
     public String getSecondString() {
         if (cf.rosterStatus) {
             if (statusString != null) {
                 return statusString;
             }
 //#if PEP
 //#             return getMoodString();
 //#endif
         }
         return null;
     }
 
     public int getImageIndex() {
         if (showComposing == true) {
             return RosterIcons.ICON_COMPOSING_INDEX;
         }
         int st = (status == Presence.PRESENCE_OFFLINE) ? offline_type : status;
         if (st < 8) {
             st += transport;
         }
         return st;
     }
 
     public int getSecImageIndex() {
         if (getNewMsgsCount() > 0) {
             return (unreadType == Msg.MESSAGE_TYPE_AUTH) ? RosterIcons.ICON_AUTHRQ_INDEX : RosterIcons.ICON_MESSAGE_INDEX;
         }
 
         if (incomingState > 0) {
             return incomingState;
         }
 
         return -1;
     }
 
 //#ifdef PEP
 //#     public String getMoodString() {
 //#         StringBuffer mood = null;
 //#         if (hasMood()) {
 //#             mood = new StringBuffer(pepMoodName);
 //#             if (pepMoodText != null) {
 //#                 if (pepMoodText.length() > 0) {
 //#                     mood.append("(").append(pepMoodText).append(")");
 //#                 }
 //#             }
 //#         }
 //#         return (mood != null) ? mood.toString() : null;
 //#     }
 //#endif
 
     public int getVHeight() {
         int itemVHeight = Math.max(maxImgHeight, fontHeight);
         if (getSecondString() != null) {
             itemVHeight += FontCache.getFont(false, FontCache.baloon).getHeight() - 3;
         }
 
         return Math.max(itemVHeight, cf.minItemHeight);
     }
 
     public void drawItem(Graphics g, int ofs, boolean sel) {
         int w = g.getClipWidth();
         int h = getVHeight();
         int xo = g.getClipX();
         int yo = g.getClipY();
 
         int offset = xo + 4;
 
         int imgH = (h - ilHeight) >> 1;
 
         if (getImageIndex() > -1) {
             offset += ilHeight;
             il.drawImage(g, getImageIndex(), xo + 2, imgH);
         }
 //#ifdef CLIENTS_ICONS
 //#         if (hasClientIcon()) {
 //#             ImageList clients = ClientsIcons.getInstance();
 //#             int clientImgSize = clients.getWidth();
 //#             w -= clientImgSize;
 //#             clients.drawImage(g, client, w, (h - clientImgSize) / 2);
 //#             if (maxImgHeight < clientImgSize) {
 //#                 maxImgHeight = clientImgSize;
 //#             }
 //#         }
 //#endif
 //#ifdef PEP
 //#         if (hasMood()) {
 //#             ImageList moods = MoodIcons.getInstance();
 //#             int moodImgSize = moods.getWidth();
 //#             w -= moodImgSize;
 //#             moods.drawImage(g, pepMood, w, (h - moodImgSize) / 2);
 //#             if (maxImgHeight < moodImgSize) {
 //#                 maxImgHeight = moodImgSize;
 //#             }
 //#         }
 //#ifdef PEP_TUNE
 //#         if (pepTune) {
 //#             w -= ilHeight;
 //#             il.drawImage(g, RosterIcons.ICON_PROFILE_INDEX + 1, w, imgH);
 //#         }
 //#ifdef PEP_ACTIVITY
 //#         if (hasActivity()) {
 //#             w -= ilHeight;
 //#             il.drawImage(g, RosterIcons.ICON_PROFILE_INDEX, w, imgH);
 //#         }
 //#endif
 //#ifdef PEP_LOCATION
 //#         if (hasLocation()) {
 //#             w -= ilHeight;
 //#             il.drawImage(g, RosterIcons.ICON_PROGRESS_INDEX, w, imgH);
 //#         }
 //#endif
 //# 
 //#endif
 //#endif
 /*         
         if (vcard!=null) {
         w-=ilHeight;
         il.drawImage(g, RosterIcons.ICON_SEARCH_INDEX, w,imgH);
         }
          */
 //#ifdef FILE_TRANSFER
 //#         if (fileQuery) {
 //#             w -= ilHeight;
 //#             il.drawImage(g, RosterIcons.ICON_PROGRESS_INDEX, w, imgH);
 //#         }
 //#endif
         if (getSecImageIndex() > -1) {
             w -= ilHeight;
             il.drawImage(g, getSecImageIndex(), w, imgH);
         }
 
         int thisOfs = 0;
 
         g.setClip(offset, yo, w - offset, h);
 
         thisOfs = (getFirstLength() > w) ? -ofs + offset : offset;
         if ((thisOfs + getFirstLength()) < 0) {
             thisOfs = offset;
         }
         g.setFont(getFont());
 
         int thisYOfs = 0;
         if (getSecondString() == null) {
             thisYOfs = (h - getFont().getHeight()) >> 1;
         }
         FontCache.drawString(g, getFirstString(), thisOfs, thisYOfs, Graphics.TOP | Graphics.LEFT);
 
         if (getSecondString() != null) {
             int y = getFont().getHeight() - 3;
             thisOfs = (getSecondLength() > w) ? -ofs + offset : offset;
             g.setFont(FontCache.getFont(false, FontCache.baloon));
             g.setColor(ColorTheme.getColor(ColorTheme.SECOND_LINE));
             FontCache.drawString(g, getSecondString(), thisOfs, y, Graphics.TOP | Graphics.LEFT);
         }
         g.setClip(xo, yo, w, h);
     }
 
 //#ifdef CLIENTS_ICONS
 //#     boolean hasClientIcon() {
 //#         return (client > -1);
 //#     }
 //#endif
 
 //#ifdef PEP
 //#     boolean hasMood() {
 //#         return (pepMood > -1 && pepMood < Moods.getInstance().getCount());
 //#     }
 //#ifdef PEP_ACTIVITY
 //# 
 //#     boolean hasActivity() {
 //#         if (activity != null) {
 //#             if (activity.length() > 0) {
 //#                 return true;
 //#             }
 //#         }
 //#         return false;
 //#     }
 //#endif
 //#ifdef PEP_LOCATION
 //# 
 //#     boolean hasLocation() {
 //#         return (location != null);
 //#     }
 //#endif
 //#endif    
 
     public ContactMessageList getMsgList() {
         if (cml == null) {
             cml = new ContactMessageList(this);
         } else {
             if (newMsgCnt > 0 && cml.on_end) cml.moveToUnread();
             cml.show();
         }
 
         return cml;
     }
 
 }
