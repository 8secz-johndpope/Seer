 package com.github.rmsy.channels.impl;
 
 import com.github.rmsy.channels.Channel;
 import com.github.rmsy.channels.event.ChannelMessageEvent;
 import com.google.common.base.Preconditions;
 import com.google.common.collect.ImmutableSet;
 import org.bukkit.Bukkit;
 import org.bukkit.ChatColor;
 import org.bukkit.entity.Player;
 
 import javax.annotation.Nullable;
 import java.util.HashSet;
 import java.util.Set;
 
 /** Simple implementation of {@link Channel}. */
 public class SimpleChannel implements Channel {
 
     /** The members of the channel. */
     private final Set<Player> members;
     /** The permission node that will be broadcast from this channel to. */
     private final String permission;
     /** The format. */
     private String format;
     /** Whether or not to strip colors. */
     private boolean shouldStripColors;
 
     private SimpleChannel() {
         this.members = null;
         this.permission = null;
     }
 
     /**
      * Creates a new SimpleChannel.
      *
      * @param format            The format to be applied to messages.
      * @param shouldStripColors Whether or not to strip messages of colors.
      * @param permission        The permission node that will be broadcast from this channel to.
      */
     public SimpleChannel(final String format, boolean shouldStripColors, final String permission) {
         this.format = Preconditions.checkNotNull(format, "format");
         this.shouldStripColors = shouldStripColors;
         this.permission = Preconditions.checkNotNull(permission);
         this.members = new HashSet<>();
     }
 
     /**
      * Gets the channel's format.
      *
      * @return The channel's format.
      * @see #setFormat(String)
      */
     @Override
     public String getFormat() {
         return this.format;
     }
 
     /**
      * Sets the channel's format (the string that appears before the message).</br><b>Note</b>: <code>%s</code> will be
      * replaced with the sending user's display name. For example, if iamramsey had a display name of 'rmsy', and had a
      * message directed to a channel with a format of <code>[Z] <%s> </code>, his message would be prepended in chat
      * with "[Z] rmsy".
      *
      * @param format The format.
      */
     @Override
     public void setFormat(String format) {
         this.format = Preconditions.checkNotNull(format, "format");
     }
 
     /**
      * Gets the users who are sending to this channel by default.
      *
      * @return The users who are sending to this channel by default.
      */
     @Override
     public final ImmutableSet<Player> getMembers() {
         return ImmutableSet.copyOf(this.members);
     }
 
     /**
      * Gets whether or not messages sent are stripped of color.
      *
      * @return Whether or not messages sent are stripped of color.
      */
     @Override
     public boolean shouldStripColors() {
         return this.shouldStripColors;
     }
 
     /**
      * Sets whether or not messages sent are stripped of color.
      *
      * @param shouldStripColors Whether or not messages sent are stripped of color.
      */
     @Override
     public void shouldStripColors(boolean shouldStripColors) {
         this.shouldStripColors = shouldStripColors;
     }
 
     /**
      * Sends a new message to the channel.
      *
      * @param rawMessage The message to be sent.
      * @param sender     The message sender, or null for console.
      * @return Whether or not the message was sent.
      */
     @Override
     public boolean sendMessage(String rawMessage, @Nullable Player sender) {
         String message = Preconditions.checkNotNull(rawMessage, "message");
         String senderDisplayName;
         if (sender != null) {
             senderDisplayName = sender.getDisplayName();
         } else {
             senderDisplayName = ChatColor.GOLD + "*" + ChatColor.AQUA + "Console";
         }
         if (this.shouldStripColors) {
             message = ChatColor.stripColor(message);
         }
         if (this.format.contains("%s")) {
             message = String.format(this.format, senderDisplayName) + message;
         } else {
             message = this.format + message;
         }
        ChannelMessageEvent event = new ChannelMessageEvent(message, sender);
         Bukkit.getPluginManager().callEvent(event);
         if (!event.isCancelled()) {
             Bukkit.broadcast(message, this.permission);
             return true;
         } else {
             return false;
         }
     }
 
     /**
      * Gets the permission node that is required for listening on this channel. Users without this permission node will
      * not receive messages from this channel.
      *
      * @return The permission node that is required for listening on this channel.
      */
     @Override
     public String getListeningPermission() {
         return this.permission;
     }
 
     /**
      * Broadcasts a message to the channel.
      *
      * @param message The message to be broadcast.
      */
     @Override
     public void broadcast(final String message) {
         Bukkit.broadcast(Preconditions.checkNotNull(message, "message"), this.permission);
     }
 
     /**
      * Removes a user as a member.</br><b>Caution</b>: Only invoke this when the user has been (or will be) assigned as
      * a member to another channel.
      *
      * @param member The user.
      */
     protected final void removeMember(Player member) {
         this.members.remove(Preconditions.checkNotNull(member, "member"));
     }
 
     /**
      * Adds a user as a member.</br><b>Caution</b>: Only invoke this when the user has been (or will be) removed as a
      * member from their existing channel (if applicable).
      *
      * @param member The user.
      */
     protected final void addMember(Player member) {
         this.members.add(Preconditions.checkNotNull(member, "member"));
     }
 }
