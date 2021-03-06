 //
// $Id: MuteDirector.java,v 1.12 2004/08/19 20:59:06 ray Exp $
 
 package com.threerings.crowd.chat.client;
 
 import java.util.HashSet;
 
 import com.samskivert.util.ObserverList;
 
 import com.threerings.util.MessageBundle;
 import com.threerings.util.Name;
 
 import com.threerings.crowd.util.CrowdContext;
 
 import com.threerings.presents.client.BasicDirector;
 import com.threerings.presents.client.Client;
 
 /**
  * Manages the mutelist.
  *
  * TODO: This class right now is pretty much just a placeholder.
  */
 public class MuteDirector extends BasicDirector
     implements ChatFilter
 {
     /**
      * An interface that can be registered with the MuteDirector to
      * receive notifications to the mutelist.
      */
     public static interface MuteObserver
     {
         /**
          * The specified player was added or removed from the mutelist.
          */
         public void muteChanged (Name playername, boolean nowMuted);
     }
 
     /**
      * Should be instantiated after the ChatDirector.
      */
     public MuteDirector (CrowdContext ctx)
     {
         super(ctx);
     }
 
     /**
      * Set up the mute director with the specified list of initial mutees.
      */
     public MuteDirector (CrowdContext ctx, Name[] list)
     {
         this(ctx);
 
         for (int ii=0, nn=list.length; ii < nn; ii++) {
             _mutelist.add(list[ii]);
         }
     }
 
     /**
      * Set the required ChatDirector.
      */
     public void setChatDirector (ChatDirector chatdir)
     {
         if (_chatdir == null) {
             _chatdir = chatdir;
             _chatdir.addChatFilter(this);
         }
     }
 
     /**
      * Add the specified mutelist observer.
      */
     public void addMuteObserver (MuteObserver obs)
     {
         _observers.add(obs);
     }
 
     /**
      * Remove the specified mutelist observer.
      */
     public void removeMuteObserver (MuteObserver obs)
     {
         _observers.remove(obs);
     }
 
     /**
      * Check to see if the specified user is muted.
      */
     public boolean isMuted (Name username)
     {
         return _mutelist.contains(username);
     }
 
     /**
      * Mute or unmute the specified user.
      */
     public void setMuted (Name username, boolean mute)
     {
         if (mute ? _mutelist.add(username) : _mutelist.remove(username)) {
             _chatdir.displayFeedback(null, MessageBundle.tcompose(
                 mute ? "m.muted" : "m.unmuted", username));
             notifyObservers(username, mute);
         }
     }
 
     /**
      * @return a list of the currently muted players.
      *
      * This list may be out of date immediately upon returning from this
      * method.
      */
     public Name[] getMuted ()
     {
         return (Name[]) _mutelist.toArray(new Name[_mutelist.size()]);
     }
 
     // documentation inherited from interface ChatFilter
     public String filter (String msg, Name otherUser, boolean outgoing)
     {
         // we are only concerned with filtering things going to or coming
         // from muted users
         if ((otherUser != null) && isMuted(otherUser)) {
             // if it was outgoing, explain the dropped message, otherwise
             // silently drop
             if (outgoing) {
                 _chatdir.displayFeedback(null, "m.no_tell_mute");
             }
             return null;
         }
 
         return msg;
     }
 
     /**
      * Notify our observers of a change in the mutelist.
      */
     protected void notifyObservers (final Name username, final boolean muted)
     {
         _observers.apply(new ObserverList.ObserverOp() {
             public boolean apply (Object observer) {
                 ((MuteObserver)observer).muteChanged(username, muted);
                 return true;
             }
         });
     }
 
     /** The chat director that we're working hard for. */
     protected ChatDirector _chatdir;
 
     /** The mutelist. */
     protected HashSet _mutelist = new HashSet();
 
     /** List of mutelist observers. */
     protected ObserverList _observers =
         new ObserverList(ObserverList.FAST_UNSAFE_NOTIFY);
 }
