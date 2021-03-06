 // Copyright 2011 Google Inc. All Rights Reserved.
 
 package com.google.wireless.speed.speedometer;
 
 import android.content.Context;
 import android.content.Intent;
 
 import java.util.concurrent.Callable;
 
 /**
  * A basic power manager implementation that decides whether a measurement can be scheduled
  * based on the current battery level: no measurements will be scheduled if the current battery
  * is lower than a threshold.
  * 
  * @author wenjiezeng@google.com (Steve Zeng)
  *
  */
 public class BatteryCapPowerManager {
   /** The minimum threshold below which no measurements will be scheduled */
   private int minBatteryThreshold;
     
   public BatteryCapPowerManager(int batteryThresh, Context context) {
     this.minBatteryThreshold = batteryThresh;
   }
   
   /** 
    * Sets the minimum battery percentage below which measurements cannot be run.
    * 
    * @param batteryThresh the battery percentage threshold between 0 and 100
    */
   public synchronized void setBatteryThresh(int batteryThresh) throws IllegalArgumentException {
     if (batteryThresh < 0 || batteryThresh > 100) {
       throw new IllegalArgumentException("batteryCap must fall between 0 and 100, inclusive");
     }
     this.minBatteryThreshold = batteryThresh;
   }
   
   public synchronized int getBatteryThresh() {
     return this.minBatteryThreshold;
   }
   
   /** 
    * Returns whether a measurement can be run.
    */
   public synchronized boolean canScheduleExperiment() {
     return (PhoneUtils.getPhoneUtils().isCharging() || 
         PhoneUtils.getPhoneUtils().getCurrentBatteryLevel() > minBatteryThreshold);
   }
   
   /**
    * A task wrapper that is power aware, the real logic is carried out by realTask
    * 
    * @author wenjiezeng@google.com (Steve Zeng)
    *
    */
   public static class PowerAwareTask implements Callable<MeasurementResult> {
     
     private MeasurementTask realTask;
     private BatteryCapPowerManager pManager;
     private MeasurementScheduler scheduler;
     
     public PowerAwareTask(MeasurementTask task, BatteryCapPowerManager manager, 
                           MeasurementScheduler scheduler) {
       realTask = task;
       pManager = manager;
       this.scheduler = scheduler;
     }
     
     private void broadcastMeasurementStart() {
       Intent intent = new Intent();
       intent.setAction(UpdateIntent.SYSTEM_STATUS_UPDATE_ACTION);
       intent.putExtra(UpdateIntent.STATUS_MSG_PAYLOAD, "Automated measurement " + 
           realTask.getDescriptor() + " has started.");
       
       scheduler.sendBroadcast(intent);
     }
     
     private void broadcastMeasurementEnd() {
       Intent intent = new Intent();
       intent.setAction(UpdateIntent.MEASUREMENT_PROGRESS_UPDATE_ACTION);
       intent.putExtra(UpdateIntent.TASK_PRIORITY_PAYLOAD, realTask.getDescription().priority);
       // A progress value MEASUREMENT_END_PROGRESS indicates the end of an measurement
       intent.putExtra(UpdateIntent.PROGRESS_PAYLOAD, Config.MEASUREMENT_END_PROGRESS);
       
       scheduler.sendBroadcast(intent);
       
       intent.setAction(UpdateIntent.SYSTEM_STATUS_UPDATE_ACTION);
       intent.putExtra(UpdateIntent.STATUS_MSG_PAYLOAD, "Automated measurement " + 
           realTask.getDescriptor() + " has finished. Speedometer is idle.");
       
       scheduler.sendBroadcast(intent);
     }
     
     private void broadcastPowerThreasholdReached() {
       Intent intent = new Intent();
      intent.setAction(UpdateIntent.SYSTEM_STATUS_UPDATE_ACTION);
       // A progress value MEASUREMENT_END_PROGRESS indicates the end of an measurement
       intent.putExtra(UpdateIntent.STATUS_MSG_PAYLOAD, 
           scheduler.getString(R.string.powerThreasholdReachedMsg));
       
       scheduler.sendBroadcast(intent);
     }
     
     @Override
     public MeasurementResult call() throws MeasurementError {
       boolean shouldBroadcastEnd = false;
       try {
         PhoneUtils.getPhoneUtils().acquireWakeLock();
         if (scheduler.isPauseRequested()) {
           throw new MeasurementError("Scheduler is paused.");
         }
         if (!pManager.canScheduleExperiment()) {
           broadcastPowerThreasholdReached();
           throw new MeasurementError("Not enough power");
         }
         scheduler.setCurrentTask(realTask);
         broadcastMeasurementStart();
         shouldBroadcastEnd = true;
         return realTask.call();
       } finally {
         PhoneUtils.getPhoneUtils().releaseWakeLock();
         scheduler.setCurrentTask(null);
         if (shouldBroadcastEnd) {
           broadcastMeasurementEnd();
         }
       }
     }
   }
 }
