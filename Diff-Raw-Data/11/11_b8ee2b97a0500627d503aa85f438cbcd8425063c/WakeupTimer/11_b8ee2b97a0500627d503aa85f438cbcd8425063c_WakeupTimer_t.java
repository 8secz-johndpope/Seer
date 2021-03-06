 /*
  * Copyright (C) 2010 The Android Open Source Project
  *
  * Licensed under the Apache License, Version 2.0 (the "License");
  * you may not use this file except in compliance with the License.
  * You may obtain a copy of the License at
  *
  *      http://www.apache.org/licenses/LICENSE-2.0
  *
  * Unless required by applicable law or agreed to in writing, software
  * distributed under the License is distributed on an "AS IS" BASIS,
  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  * See the License for the specific language governing permissions and
  * limitations under the License.
  */
 
 package com.android.sip;
 
 import android.app.AlarmManager;
 import android.app.PendingIntent;
 import android.content.BroadcastReceiver;
 import android.content.Context;
 import android.content.Intent;
 import android.content.IntentFilter;
 import android.os.SystemClock;
 import android.util.Log;
 
 import java.util.Collection;
 import java.util.Comparator;
 import java.util.Iterator;
 import java.util.TreeSet;
 
 /**
  * Timer that can schedule events to occur even when the device is in sleep.
  * Only used internally in this package.
  */
 class WakeupTimer extends BroadcastReceiver {
     private static final String TAG = "__SIP.WakeupTimer__";
     private static final String TRIGGER_TIME = "TriggerTime";
 
     private Context mContext;
     private AlarmManager mAlarmManager;
 
     // runnable --> time to execute in SystemClock
     private TreeSet<MyEvent> mEventQueue =
             new TreeSet<MyEvent>(new MyEventComparator());
 
     private PendingIntent mPendingIntent;
 
     public WakeupTimer(Context context) {
         mContext = context;
         mAlarmManager = (AlarmManager)
                 context.getSystemService(Context.ALARM_SERVICE);
 
         IntentFilter filter = new IntentFilter(getAction());
         context.registerReceiver(this, filter);
     }
 
     /**
      * Stops the timer. No event can be scheduled after this method is called.
      */
     public synchronized void stop() {
         mContext.unregisterReceiver(this);
         if (mPendingIntent != null) {
             mAlarmManager.cancel(mPendingIntent);
             mPendingIntent = null;
         }
         mEventQueue.clear();
         mEventQueue = null;
     }
 
     private synchronized boolean stopped() {
         if (mEventQueue == null) {
             Log.w(TAG, "Timer stopped");
             return true;
         } else {
             return false;
         }
     }
 
     private void cancelAlarm() {
         mAlarmManager.cancel(mPendingIntent);
         mPendingIntent = null;
     }
 
     private void recalculatePeriods(long now) {
         if (mEventQueue.isEmpty()) return;
 
         int minPeriod = mEventQueue.first().mMaxPeriod;
         for (MyEvent e : mEventQueue) {
             int remainingTime = (int) (e.mTriggerTime - now);
             remainingTime = remainingTime / minPeriod * minPeriod;
             e.mTriggerTime = now + remainingTime;
 
             e.mPeriod = e.mMaxPeriod / minPeriod * minPeriod;
         }
         TreeSet<MyEvent> newQueue = new TreeSet<MyEvent>(
                 mEventQueue.comparator());
         newQueue.addAll((Collection<MyEvent>) mEventQueue);
         mEventQueue.clear();
         mEventQueue = newQueue;
         Log.v(TAG, "queue re-calculated");
         printQueue();
     }
 
     // Determines the period and the trigger time of the new event and insert it
     // to the queue.
     private void insertEvent(MyEvent event) {
         long now = SystemClock.elapsedRealtime();
         if (mEventQueue.isEmpty()) {
             event.mTriggerTime = now + event.mPeriod;
             mEventQueue.add(event);
             return;
         }
         MyEvent firstEvent = mEventQueue.first();
         int minPeriod = firstEvent.mPeriod;
         if (minPeriod <= event.mMaxPeriod) {
             int period = event.mPeriod
                     = event.mMaxPeriod / minPeriod * minPeriod;
             period -= (int) (firstEvent.mTriggerTime - now);
             period = period / minPeriod * minPeriod;
             event.mTriggerTime = firstEvent.mTriggerTime + period;
             mEventQueue.add(event);
         } else {
             long triggerTime = now + event.mPeriod;
             if (firstEvent.mTriggerTime < triggerTime) {
                 triggerTime = firstEvent.mTriggerTime;
             }
             event.mTriggerTime = triggerTime;
             mEventQueue.add(event);
             recalculatePeriods(triggerTime);
         }
     }
 
     /**
      * Sets a periodic timer.
      *
      * @param period the timer period; in milli-second
      * @param callback is called back when the timer goes off; the same callback
      *      can be specified in multiple timer events
      */
     public synchronized void set(int period, Runnable callback) {
         if (stopped()) return;
 
         long now = SystemClock.elapsedRealtime();
         MyEvent event = new MyEvent(period, callback);
         insertEvent(event);
 
         if (mEventQueue.first() == event) {
             if (mEventQueue.size() > 1) cancelAlarm();
             scheduleNext();
         }
 
         long triggerTime = event.mTriggerTime;
         Log.v(TAG, " add event " + event + " scheduled at "
                 + showTime(triggerTime) + " at " + showTime(now)
                 + ", #events=" + mEventQueue.size());
         printQueue();
     }
 
     /**
      * Cancels all the timer events with the specified callback.
      *
      * @param callback the callback
      */
     public synchronized void cancel(Runnable callback) {
         if (stopped() || mEventQueue.isEmpty()) return;
         Log.d(TAG, "cancel callback:" + callback);
 
         MyEvent firstEvent = mEventQueue.first();
         for (Iterator<MyEvent> iter = mEventQueue.iterator();
                 iter.hasNext();) {
             MyEvent event = iter.next();
             if (event.mCallback == callback) {
                 iter.remove();
                 Log.d(TAG, "cancel event:" + event);
             }
         }
         if (mEventQueue.isEmpty() || (mEventQueue.first() != firstEvent)) {
             cancelAlarm();
             recalculatePeriods(firstEvent.mTriggerTime);
             scheduleNext();
         }
         printQueue();
     }
 
     private void scheduleNext() {
         if (stopped() || mEventQueue.isEmpty()) return;
 
         if (mPendingIntent != null) {
             throw new RuntimeException("pendingIntent is not null!");
         }
 
         MyEvent event = mEventQueue.first();
         Intent intent = new Intent(getAction());
         intent.putExtra(TRIGGER_TIME, event.mTriggerTime);
         PendingIntent pendingIntent = mPendingIntent =
                 PendingIntent.getBroadcast(mContext, 0, intent,
                         PendingIntent.FLAG_UPDATE_CURRENT);
         mAlarmManager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP,
                 event.mTriggerTime, pendingIntent);
     }
 
     @Override
     public synchronized void onReceive(Context context, Intent intent) {
         String action = intent.getAction();
         if (getAction().equals(action)
                 && intent.getExtras().containsKey(TRIGGER_TIME)) {
             mPendingIntent = null;
             long triggerTime = intent.getLongExtra(TRIGGER_TIME, -1L);
             execute(triggerTime);
         } else {
             Log.d(TAG, "unrecognized intent: " + intent);
         }
     }
 
     private void printQueue() {
         int count = 0;
         for (MyEvent event : mEventQueue) {
             Log.d(TAG, "     " + event + ": scheduled at "
                     + showTime(event.mTriggerTime));
             if (++count >= 5) break;
         }
         if (mEventQueue.size() > count) {
             Log.d(TAG, "     .....");
         }
     }
 
     private void execute(long triggerTime) {
         Log.d(TAG, "time's up, triggerTime = " + showTime(triggerTime) + ": "
                 + mEventQueue.size());
         if (stopped() || mEventQueue.isEmpty()) return;
 
        for (MyEvent event : mEventQueue) {
             if (event.mTriggerTime != triggerTime) break;
             Log.d(TAG, "execute " + event);
 
             event.mTriggerTime += event.mPeriod;
 
             // run the callback in a new thread to prevent deadlock
             new Thread(event.mCallback).start();
         }
         Log.d(TAG, "after timeout execution");
         printQueue();
         scheduleNext();
     }
 
     private String getAction() {
         return toString();
     }
 
     private static class MyEvent {
         int mPeriod;
         int mMaxPeriod;
         long mTriggerTime;
         Runnable mCallback;
 
         MyEvent(int period, Runnable callback) {
             mPeriod = mMaxPeriod = period;
             mCallback = callback;
         }
 
         @Override
         public String toString() {
             String s = super.toString();
             s = s.substring(s.indexOf("@"));
             return s + ":" + (mPeriod / 1000) + ":" + (mMaxPeriod / 1000);
         }
     }
 
     private static class MyEventComparator implements Comparator<MyEvent> {
         public int compare(MyEvent e1, MyEvent e2) {
             if (e1 == e2) return 0;
             int diff = e1.mMaxPeriod - e2.mMaxPeriod;
             if (diff == 0) diff = -1;
             return diff;
         }
 
         public boolean equals(Object that) {
             return (this == that);
         }
     }
 
     private static String showTime(long time) {
         int ms = (int) (time % 1000);
         int s = (int) (time / 1000);
         int m = s / 60;
         s %= 60;
         return String.format("%d.%d.%d", m, s, ms);
     }
 }
