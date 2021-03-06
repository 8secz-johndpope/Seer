 /*
  * Jobs Plugin for Bukkit
  * Copyright (C) 2011  Zak Ford <zak.j.ford@gmail.com>
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
  * 
  */
 
 package com.zford.jobs.event;
 
 import org.bukkit.event.Cancellable;
 import org.bukkit.event.Event;
 import org.bukkit.event.HandlerList;
 
 import com.zford.jobs.config.container.Job;
 import com.zford.jobs.config.container.JobsPlayer;
 
 public class JobsLeaveEvent extends Event implements Cancellable {
     private static final HandlerList handlers = new HandlerList();
     private boolean cancelled = false;
     private JobsPlayer player;
     private Job job;
 
     /**
      * Constructor
      * @param player - the player leaving the job
      * @param job - job they are leaving
      */
     public JobsLeaveEvent(JobsPlayer player, Job job){
         this.player = player;
         this.job = job;
     }
     
     /**
      * Get the player involved in this event
      * @return the player involved in this event
      */
     public JobsPlayer getPlayer(){
         return player;
     }
     
     /**
      * Get the job the player is leaving
      * @return the job the player is leaving
      */
     public Job getOldJob(){
         return job;
     }
 
     @Override
     public boolean isCancelled() {
         return cancelled;
     }
 
     @Override
     public void setCancelled(boolean cancelled) {
         this.cancelled = cancelled;
     }
 
     @Override
     public HandlerList getHandlers() {
         return handlers;
     }
 
     public static HandlerList getHandlerList() {
         return handlers;
     }
 }
