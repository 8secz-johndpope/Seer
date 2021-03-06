 /*
  * Copyright (C) 2007 The Android Open Source Project
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
 
 package com.android.calendar.alerts;
 
 import com.android.calendar.R;
 import com.android.calendar.Utils;
 
 import android.app.Notification;
 import android.app.PendingIntent;
 import android.app.Service;
 import android.content.BroadcastReceiver;
 import android.content.ContentResolver;
 import android.content.ContentUris;
 import android.content.Context;
 import android.content.Intent;
 import android.content.res.Resources;
 import android.database.Cursor;
 import android.net.Uri;
 import android.os.PowerManager;
 import android.provider.CalendarContract.Attendees;
 import android.provider.CalendarContract.Calendars;
 import android.provider.CalendarContract.Events;
 import android.text.SpannableStringBuilder;
 import android.text.TextUtils;
 import android.text.style.TextAppearanceSpan;
 import android.util.Log;
 
 import java.util.ArrayList;
 import java.util.List;
 
 /**
  * Receives android.intent.action.EVENT_REMINDER intents and handles
  * event reminders.  The intent URI specifies an alert id in the
  * CalendarAlerts database table.  This class also receives the
  * BOOT_COMPLETED intent so that it can add a status bar notification
  * if there are Calendar event alarms that have not been dismissed.
  * It also receives the TIME_CHANGED action so that it can fire off
  * snoozed alarms that have become ready.  The real work is done in
  * the AlertService class.
  *
  * To trigger this code after pushing the apk to device:
  * adb shell am broadcast -a "android.intent.action.EVENT_REMINDER"
  *    -n "com.android.calendar/.alerts.AlertReceiver"
  */
 public class AlertReceiver extends BroadcastReceiver {
     private static final String TAG = "AlertReceiver";
 
     private static final String DELETE_ACTION = "delete";
 
     static final Object mStartingServiceSync = new Object();
     static PowerManager.WakeLock mStartingService;
 
     public static final String ACTION_DISMISS_OLD_REMINDERS = "removeOldReminders";
     private static final int NOTIFICATION_DIGEST_MAX_LENGTH = 3;
 
     @Override
     public void onReceive(Context context, Intent intent) {
         if (AlertService.DEBUG) {
             Log.d(TAG, "onReceive: a=" + intent.getAction() + " " + intent.toString());
         }
         if (DELETE_ACTION.equals(intent.getAction())) {
 
             /* The user has clicked the "Clear All Notifications"
              * buttons so dismiss all Calendar alerts.
              */
             // TODO Grab a wake lock here?
             Intent serviceIntent = new Intent(context, DismissAlarmsService.class);
             context.startService(serviceIntent);
         } else {
             Intent i = new Intent();
             i.setClass(context, AlertService.class);
             i.putExtras(intent);
             i.putExtra("action", intent.getAction());
             Uri uri = intent.getData();
 
             // This intent might be a BOOT_COMPLETED so it might not have a Uri.
             if (uri != null) {
                 i.putExtra("uri", uri.toString());
             }
             beginStartingService(context, i);
         }
     }
 
     /**
      * Start the service to process the current event notifications, acquiring
      * the wake lock before returning to ensure that the service will run.
      */
     public static void beginStartingService(Context context, Intent intent) {
         synchronized (mStartingServiceSync) {
             if (mStartingService == null) {
                 PowerManager pm =
                     (PowerManager)context.getSystemService(Context.POWER_SERVICE);
                 mStartingService = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
                         "StartingAlertService");
                 mStartingService.setReferenceCounted(false);
             }
             mStartingService.acquire();
             context.startService(intent);
         }
     }
 
     /**
      * Called back by the service when it has finished processing notifications,
      * releasing the wake lock if the service is now stopping.
      */
     public static void finishStartingService(Service service, int startId) {
         synchronized (mStartingServiceSync) {
             if (mStartingService != null) {
                 if (service.stopSelfResult(startId)) {
                     mStartingService.release();
                 }
             }
         }
     }
 
     private static PendingIntent createClickEventIntent(Context context, long eventId,
             long startMillis, long endMillis, int notificationId) {
         return createDismissAlarmsIntent(context, eventId, startMillis, endMillis, notificationId,
                 "com.android.calendar.CLICK", true);
     }
 
     private static PendingIntent createDeleteEventIntent(Context context, long eventId,
             long startMillis, long endMillis, int notificationId) {
         return createDismissAlarmsIntent(context, eventId, startMillis, endMillis, notificationId,
                 "com.android.calendar.DELETE", false);
     }
 
     private static PendingIntent createDismissAlarmsIntent(Context context, long eventId,
             long startMillis, long endMillis, int notificationId, String action,
             boolean showEvent) {
         Intent intent = new Intent();
         intent.setClass(context, DismissAlarmsService.class);
         intent.putExtra(AlertUtils.EVENT_ID_KEY, eventId);
         intent.putExtra(AlertUtils.EVENT_START_KEY, startMillis);
         intent.putExtra(AlertUtils.EVENT_END_KEY, endMillis);
         intent.putExtra(AlertUtils.SHOW_EVENT_KEY, showEvent);
         intent.putExtra(AlertUtils.NOTIFICATION_ID_KEY, notificationId);
 
         // Must set a field that affects Intent.filterEquals so that the resulting
         // PendingIntent will be a unique instance (the 'extras' don't achieve this).
         // This must be unique for the click event across all reminders (so using
         // event ID + startTime should be unique).  This also must be unique from
         // the delete event (which also uses DismissAlarmsService).
         Uri.Builder builder = Events.CONTENT_URI.buildUpon();
         ContentUris.appendId(builder, eventId);
         ContentUris.appendId(builder, startMillis);
         intent.setData(builder.build());
         intent.setAction(action);
         return PendingIntent.getService(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
     }
 
     private static PendingIntent createSnoozeIntent(Context context, long eventId,
             long startMillis, long endMillis, int notificationId) {
         Intent intent = new Intent();
         intent.setClass(context, SnoozeAlarmsService.class);
         intent.putExtra(AlertUtils.EVENT_ID_KEY, eventId);
         intent.putExtra(AlertUtils.EVENT_START_KEY, startMillis);
         intent.putExtra(AlertUtils.EVENT_END_KEY, endMillis);
         intent.putExtra(AlertUtils.NOTIFICATION_ID_KEY, notificationId);
 
         Uri.Builder builder = Events.CONTENT_URI.buildUpon();
         ContentUris.appendId(builder, eventId);
         ContentUris.appendId(builder, startMillis);
         intent.setData(builder.build());
         return PendingIntent.getService(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
     }
 
     public static Notification makeBasicNotification(Context context, String title,
             String summaryText, long startMillis, long endMillis, long eventId,
             int notificationId, boolean doPopup) {
         return makeBasicNotificationBuilder(context, title, summaryText, startMillis, endMillis,
                 eventId, notificationId, doPopup, false, false).build();
     }
 
     private static Notification.Builder makeBasicNotificationBuilder(Context context, String title,
             String summaryText, long startMillis, long endMillis, long eventId,
             int notificationId, boolean doPopup, boolean highPriority, boolean addActionButtons) {
         Resources resources = context.getResources();
         if (title == null || title.length() == 0) {
             title = resources.getString(R.string.no_title_label);
         }
 
         // Create an intent triggered by clicking on the status icon, that dismisses the
         // notification and shows the event.
         PendingIntent clickIntent = createClickEventIntent(context, eventId, startMillis,
                 endMillis, notificationId);
 
         // Create a delete intent triggered by dismissing the notification.
         PendingIntent deleteIntent = createDeleteEventIntent(context, eventId, startMillis,
             endMillis, notificationId);
 
         // Create the base notification.
         Notification.Builder notificationBuilder = new Notification.Builder(context);
         notificationBuilder.setContentTitle(title);
         notificationBuilder.setContentText(summaryText);
         notificationBuilder.setSmallIcon(R.drawable.stat_notify_calendar);
         notificationBuilder.setContentIntent(clickIntent);
         notificationBuilder.setDeleteIntent(deleteIntent);
         if (addActionButtons) {
             // Create a snooze button.  TODO: change snooze to 10 minutes.
             PendingIntent snoozeIntent = createSnoozeIntent(context, eventId, startMillis,
                     endMillis, notificationId);
             notificationBuilder.addAction(R.drawable.snooze,
                     resources.getString(R.string.snooze_5min_label), snoozeIntent);
 
             // Create an email button.
             PendingIntent emailIntent = createEmailIntent(context, eventId, title);
             if (emailIntent != null) {
                 notificationBuilder.addAction(R.drawable.ic_menu_email_holo_dark,
                         resources.getString(R.string.email_guests_label), emailIntent);
             }
         }
         if (doPopup) {
             notificationBuilder.setFullScreenIntent(clickIntent, true);
         }
 
        // Turn off timestamp.
        notificationBuilder.setWhen(0);

         // Setting to a higher priority will encourage notification manager to expand the
         // notification.
         if (highPriority) {
             notificationBuilder.setPriority(Notification.PRIORITY_HIGH);
         } else {
             notificationBuilder.setPriority(Notification.PRIORITY_DEFAULT);
         }
         return notificationBuilder;
     }
 
     /**
      * Creates an expanding notification.  The initial expanded state is decided by
      * the notification manager based on the priority.
      */
     public static Notification makeExpandingNotification(Context context, String title,
             String summaryText, String description, long startMillis, long endMillis, long eventId,
             int notificationId, boolean doPopup, boolean highPriority) {
         Notification.Builder basicBuilder = makeBasicNotificationBuilder(context, title,
                 summaryText, startMillis, endMillis, eventId, notificationId,
                 doPopup, highPriority, true);
 
         // Create an expanded notification
         Notification.BigTextStyle expandedBuilder = new Notification.BigTextStyle(
                 basicBuilder);
         if (description != null) {
             description = description.trim();
         }
         String text;
         if (TextUtils.isEmpty(description)) {
             text = summaryText;
         } else {
             text = context.getResources().getString(
                     R.string.event_notification_big_text, summaryText, description);
         }
         expandedBuilder.bigText(text);
         return expandedBuilder.build();
     }
 
     /**
      * Creates an expanding digest notification for expired events.
      */
     public static Notification makeDigestNotification(Context context,
             List<AlertService.NotificationInfo> notificationInfos, String digestTitle,
             boolean expandable) {
         if (notificationInfos == null || notificationInfos.size() < 1) {
             return null;
         }
 
         Resources res = context.getResources();
         int numEvents = notificationInfos.size();
 
         // Create an intent triggered by clicking on the status icon that shows the alerts list.
         Intent clickIntent = new Intent();
         clickIntent.setClass(context, AlertActivity.class);
         clickIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
         PendingIntent pendingClickIntent = PendingIntent.getActivity(context, 0, clickIntent,
                     PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_UPDATE_CURRENT);
 
         // Create an intent triggered by dismissing the digest notification that clears all
         // expired events.
         Intent deleteIntent = new Intent();
         deleteIntent.setClass(context, DismissAlarmsService.class);
         deleteIntent.setAction(DELETE_ACTION);
         deleteIntent.putExtra(AlertUtils.DELETE_EXPIRED_ONLY_KEY, true);
         PendingIntent pendingDeleteIntent = PendingIntent.getService(context, 0, deleteIntent,
                 PendingIntent.FLAG_UPDATE_CURRENT);
 
         if (digestTitle == null || digestTitle.length() == 0) {
             digestTitle = res.getString(R.string.no_title_label);
         }
 
         Notification.Builder notificationBuilder = new Notification.Builder(context);
         notificationBuilder.setContentTitle(digestTitle);
         notificationBuilder.setSmallIcon(R.drawable.stat_notify_calendar);
         notificationBuilder.setContentIntent(pendingClickIntent);
         notificationBuilder.setDeleteIntent(pendingDeleteIntent);
         String nEventsStr = res.getQuantityString(R.plurals.Nevents, numEvents, numEvents);
         notificationBuilder.setContentText(nEventsStr);
 
         // Set to min priority to encourage the notification manager to collapse it.
         notificationBuilder.setPriority(Notification.PRIORITY_MIN);
 
         if (expandable) {
             // Multiple reminders.  Combine into an expanded digest notification.
             Notification.InboxStyle expandedBuilder = new Notification.InboxStyle(
                     notificationBuilder);
             int i = 0;
             for (AlertService.NotificationInfo info : notificationInfos) {
                 if (i < NOTIFICATION_DIGEST_MAX_LENGTH) {
                     String name = info.eventName;
                     if (TextUtils.isEmpty(name)) {
                         name = context.getResources().getString(R.string.no_title_label);
                     }
                     String timeLocation = AlertUtils.formatTimeLocation(context, info.startMillis,
                             info.allDay, info.location);
 
                     TextAppearanceSpan primaryTextSpan = new TextAppearanceSpan(context,
                             R.style.NotificationPrimaryText);
                     TextAppearanceSpan secondaryTextSpan = new TextAppearanceSpan(context,
                             R.style.NotificationSecondaryText);
 
                     // Event title in bold.
                     SpannableStringBuilder stringBuilder = new SpannableStringBuilder();
                     stringBuilder.append(name);
                     stringBuilder.setSpan(primaryTextSpan, 0, stringBuilder.length(), 0);
                     stringBuilder.append("  ");
 
                     // Followed by time and location.
                     int secondaryIndex = stringBuilder.length();
                     stringBuilder.append(timeLocation);
                     stringBuilder.setSpan(secondaryTextSpan, secondaryIndex, stringBuilder.length(),
                             0);
                     expandedBuilder.addLine(stringBuilder);
                     i++;
                 } else {
                     break;
                 }
             }
 
             // If there are too many to display, add "+X missed events" for the last line.
             int remaining = numEvents - i;
             if (remaining > 0) {
                 String nMoreEventsStr = res.getQuantityString(R.plurals.N_missed_events, remaining,
                         remaining);
                 // TODO: Add highlighting and icon to this last entry once framework allows it.
                 expandedBuilder.setSummaryText(nMoreEventsStr);
             }
 
             // Remove the title in the expanded form (redundant with the listed items).
             expandedBuilder.setBigContentTitle("");
 
             return expandedBuilder.build();
         } else {
             return notificationBuilder.build();
         }
     }
 
     private static final String[] ATTENDEES_PROJECTION = new String[] {
         Attendees.ATTENDEE_EMAIL,           // 0
         Attendees.ATTENDEE_STATUS,          // 1
     };
     private static final int ATTENDEES_INDEX_EMAIL = 0;
     private static final int ATTENDEES_INDEX_STATUS = 1;
     private static final String ATTENDEES_WHERE = Attendees.EVENT_ID + "=?";
     private static final String ATTENDEES_SORT_ORDER = Attendees.ATTENDEE_NAME + " ASC, "
             + Attendees.ATTENDEE_EMAIL + " ASC";
 
     private static final String[] EVENT_PROJECTION = new String[] {
         Calendars.OWNER_ACCOUNT, // 0
         Calendars.ACCOUNT_NAME   // 1
     };
     private static final int EVENT_INDEX_OWNER_ACCOUNT = 0;
     private static final int EVENT_INDEX_ACCOUNT_NAME = 1;
 
     /**
      * Creates an Intent for emailing the attendees of the event.  Returns null if there
      * are no emailable attendees.
      */
     private static PendingIntent createEmailIntent(Context context, long eventId,
             String eventTitle) {
         ContentResolver resolver = context.getContentResolver();
 
         // TODO: Refactor to move query part into Utils.createEmailAttendeeIntent, to
         // be shared with EventInfoFragment.
 
         // Query for the owner account(s).
         String ownerAccount = null;
         String syncAccount = null;
         Cursor eventCursor = resolver.query(
                 ContentUris.withAppendedId(Events.CONTENT_URI, eventId), EVENT_PROJECTION,
                 null, null, null);
         if (eventCursor.moveToFirst()) {
             ownerAccount = eventCursor.getString(EVENT_INDEX_OWNER_ACCOUNT);
             syncAccount = eventCursor.getString(EVENT_INDEX_ACCOUNT_NAME);
         }
 
         // Query for the attendees.
         List<String> toEmails = new ArrayList<String>();
         List<String> ccEmails = new ArrayList<String>();
         Cursor attendeesCursor = resolver.query(Attendees.CONTENT_URI, ATTENDEES_PROJECTION,
                 ATTENDEES_WHERE, new String[] { Long.toString(eventId) }, ATTENDEES_SORT_ORDER);
         if (attendeesCursor.moveToFirst()) {
             do {
                 int status = attendeesCursor.getInt(ATTENDEES_INDEX_STATUS);
                 String email = attendeesCursor.getString(ATTENDEES_INDEX_EMAIL);
                 switch(status) {
                     case Attendees.ATTENDEE_STATUS_DECLINED:
                         addIfEmailable(ccEmails, email, syncAccount);
                         break;
                     default:
                         addIfEmailable(toEmails, email, syncAccount);
                 }
             } while (attendeesCursor.moveToNext());
         }
 
         Intent intent = null;
         if (ownerAccount != null && (toEmails.size() > 0 || ccEmails.size() > 0)) {
             intent = Utils.createEmailAttendeesIntent(context.getResources(), eventTitle,
                     toEmails, ccEmails, ownerAccount);
         }
 
         if (intent == null) {
             return null;
         }
         else {
             intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
             return PendingIntent.getActivity(context, Long.valueOf(eventId).hashCode(), intent,
                     PendingIntent.FLAG_CANCEL_CURRENT);
         }
     }
 
     private static void addIfEmailable(List<String> emailList, String email, String syncAccount) {
         if (Utils.isValidEmail(email) && !email.equals(syncAccount)) {
             emailList.add(email);
         }
     }
 }
