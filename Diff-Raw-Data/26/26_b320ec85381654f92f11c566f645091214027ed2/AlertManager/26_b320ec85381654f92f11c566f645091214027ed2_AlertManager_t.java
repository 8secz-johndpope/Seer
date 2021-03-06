 package com.ftechz.DebatingTimer;
 
 import android.app.Notification;
 import android.app.NotificationManager;
 import android.app.PendingIntent;
 import android.content.Context;
 import android.content.Intent;
 import android.os.PowerManager;
 
 /**
 * AlertManager class
 * Manages all alerts for the application
 * Only a single instance of this should exist
 */
 public class AlertManager
 {
     public static final int NOTIFICATION_ID = 1;
 
     private final DebatingTimerService mDebatingTimerService;
     private final NotificationManager mNotificationManager;
     private Notification mNotification;
     private final PendingIntent mPendingIntent;
     private boolean mShowingNotification = false;
     private AlarmChain mStage;
     private final PowerManager.WakeLock mWakeLock;
     private BellRepeater mBellRepeater = null;
 
     public AlertManager(DebatingTimerService debatingTimerService)
     {
         mDebatingTimerService = debatingTimerService;
         mNotificationManager = (NotificationManager) debatingTimerService.getSystemService(
                 Context.NOTIFICATION_SERVICE);
         Intent notificationIntent = new Intent(debatingTimerService, DebatingActivity.class);
         mPendingIntent = PendingIntent.getActivity(debatingTimerService, 0, notificationIntent, 0);
 
         PowerManager pm = (PowerManager) debatingTimerService.getSystemService(Context.POWER_SERVICE);
         mWakeLock = pm.newWakeLock(
                 PowerManager.SCREEN_DIM_WAKE_LOCK | PowerManager.ON_AFTER_RELEASE,
                 "DebatingWakeLock");
     }
 
     public void makeActive(AlarmChain stage)
     {
         mStage = stage;
 
         if(!mShowingNotification)
         {
             mNotification = new Notification(R.drawable.icon,
                     mDebatingTimerService.getText(R.string.notificationTicker),
                     System.currentTimeMillis());
 
             updateNotification();
             mDebatingTimerService.startForeground(NOTIFICATION_ID, mNotification);
 
             mShowingNotification = true;
         }
 
         mWakeLock.acquire();
     }
 
     public void updateNotification()
     {
         if(mStage != null)
         {
             mNotification.setLatestEventInfo(mDebatingTimerService,
                     mDebatingTimerService.getText(R.string.notification_title),
                     mStage.getNotificationText(), mPendingIntent);
         }
     }
 
     public void makeInactive()
     {
         if(mShowingNotification)
         {
             mWakeLock.release();
             mDebatingTimerService.stopForeground(true);
             mBellRepeater.stop();
             mShowingNotification = false;
         }
     }
 
     public void triggerAlert(AlarmChain.Event alert)
     {
         updateNotification();
         if(mShowingNotification)
         {
 
             mNotificationManager.notify(NOTIFICATION_ID, mNotification);
 
             if (mBellRepeater != null) {
                mBellRepeater.stop();
             }
 
             mBellRepeater = new BellRepeater(mDebatingTimerService.getApplicationContext(), alert.getBellInfo());
             mBellRepeater.play();
 
         }
     }
 }
