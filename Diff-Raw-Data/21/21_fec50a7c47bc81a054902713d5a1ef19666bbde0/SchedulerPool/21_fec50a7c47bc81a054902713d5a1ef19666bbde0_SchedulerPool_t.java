 /*
  * To change this template, choose Tools | Templates
  * and open the template in the editor.
  */
 package com.nationsmc.chunkrefresh.scheduler;
 
 import com.nationsmc.chunkrefresh.ChunkRefresh;
 import java.io.File;
 import java.io.IOException;
 import java.util.List;
import org.bukkit.Bukkit;
 import org.bukkit.Chunk;
 
 /**
  *
  * @author tarlach
  */
 public class SchedulerPool implements Runnable {
 
     /**
      *
      */
     public static final String defaultFlatFileDir = "plugins/ChunkRefresh/chunkScheduler/";
     List<ScheduleData> registered;
     File flatdir;
 
     public final void loadSchedules(String dir) throws IOException {
 
         this.flatdir = new File(dir);
         if (this.flatdir.isDirectory()) {
             for (File child : this.flatdir.listFiles()) {
                 if (child.canRead() && this.isValidFileName(child)) {
                     registered.add(new ScheduleData(child));
                     registered.get(registered.indexOf(child)).load();
                 } else {
                     throw new IOException();
                 }
 
             }
         }
     }
 
     public SchedulerPool(String fileDir) {
         try {
             this.loadSchedules(fileDir);
         } catch (IOException ex) {
             Bukkit.getLogger().info("[SEVERE] IOException on SchedulePool Constructor!");
         }
 
     }
 
     public void add(Chunk chunk, long time) {
         ScheduleData dat = new ScheduleData(chunk, time);
         if (!registered.contains(dat)) {
             registered.add(dat);
         }
     }
 
     public static long parseTimeInMilli(String time) {
         return 0;
 
     }
 
     public void run() {
         synchronized (this) {
             for (ScheduleData dat : this.registered) {
                 if (timeBound(dat.time)) {
                     // TODO:Pass chunk to update here. 
                 }
             }
         }
     }
 
     public boolean timeBound(long time) {
         long serverTime = System.currentTimeMillis();
         if (serverTime - 100000 < time && serverTime + 10000 > time) {
             return true;
         } else {
             return false;
         }
     }
 
     public boolean isValidFileName(File file) {
         String temp = file.getName();
         if (temp.matches("(\\d+).(\\d+).sch")) {
             return true;
         } else {
             return false;
         }
     }
 }
