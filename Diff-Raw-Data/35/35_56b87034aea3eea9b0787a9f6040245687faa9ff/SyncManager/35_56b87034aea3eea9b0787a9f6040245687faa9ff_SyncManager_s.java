 /*
  * Copyright (C) 2008-2009 Marc Blank
  * Licensed to The Android Open Source Project.
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
 
 package com.android.exchange;
 
 import com.android.email.mail.MessagingException;
 import com.android.email.provider.EmailContent;
 import com.android.email.provider.EmailContent.Account;
 import com.android.email.provider.EmailContent.Attachment;
 import com.android.email.provider.EmailContent.HostAuth;
 import com.android.email.provider.EmailContent.HostAuthColumns;
 import com.android.email.provider.EmailContent.Mailbox;
 import com.android.email.provider.EmailContent.MailboxColumns;
 import com.android.email.provider.EmailContent.Message;
 import com.android.email.provider.EmailContent.MessageColumns;
 import com.android.email.provider.EmailContent.SyncColumns;
 import com.android.exchange.utility.FileLogger;
 
 import android.accounts.AccountManager;
 import android.app.AlarmManager;
 import android.app.PendingIntent;
 import android.app.Service;
 import android.content.BroadcastReceiver;
 import android.content.ContentResolver;
 import android.content.ContentValues;
 import android.content.Context;
 import android.content.Intent;
 import android.content.IntentFilter;
 import android.database.ContentObserver;
 import android.database.Cursor;
 import android.net.ConnectivityManager;
 import android.net.NetworkInfo;
 import android.net.Uri;
 import android.net.NetworkInfo.State;
 import android.os.Bundle;
 import android.os.Debug;
 import android.os.Handler;
 import android.os.IBinder;
 import android.os.PowerManager;
 import android.os.RemoteCallbackList;
 import android.os.RemoteException;
 import android.os.PowerManager.WakeLock;
 import android.util.Log;
 
 import java.io.BufferedReader;
 import java.io.BufferedWriter;
 import java.io.File;
 import java.io.FileNotFoundException;
 import java.io.FileReader;
 import java.io.FileWriter;
 import java.io.IOException;
 import java.util.ArrayList;
 import java.util.HashMap;
 import java.util.List;
 
 /**
  * The SyncManager handles all aspects of starting, maintaining, and stopping the various sync
  * adapters used by Exchange.  However, it is capable of handing any kind of email sync, and it
  * would be appropriate to use for IMAP push, when that functionality is added to the Email
  * application.
  *
  * The Email application communicates with EAS sync adapters via SyncManager's binder interface,
  * which exposes UI-related functionality to the application (see the definitions below)
  *
  * SyncManager uses ContentObservers to detect changes to accounts, mailboxes, and messages in
  * order to maintain proper 2-way syncing of data.  (More documentation to follow)
  *
  */
 public class SyncManager extends Service implements Runnable {
 
     private static final String TAG = "EAS SyncManager";
 
     // The SyncManager's mailbox "id"
     private static final int SYNC_MANAGER_ID = -1;
 
     private static final int SECONDS = 1000;
     private static final int MINUTES = 60*SECONDS;
     private static final int ONE_DAY_MINUTES = 1440;
 
     private static final int SYNC_MANAGER_HEARTBEAT_TIME = 15*MINUTES;
     private static final int CONNECTIVITY_WAIT_TIME = 10*MINUTES;
 
     // Sync hold constants for services with transient errors
     private static final int HOLD_DELAY_ESCALATION = 30*SECONDS;
     private static final int HOLD_DELAY_MAXIMUM = 3*MINUTES;
 
     // Reason codes when SyncManager.kick is called (mainly for debugging)
     // UI has changed data, requiring an upsync of changes
     public static final int SYNC_UPSYNC = 0;
     // A scheduled sync (when not using push)
     public static final int SYNC_SCHEDULED = 1;
     // Mailbox was marked push
     public static final int SYNC_PUSH = 2;
     // A ping (EAS push signal) was received
     public static final int SYNC_PING = 3;
     // startSync was requested of SyncManager
     public static final int SYNC_SERVICE_START_SYNC = 4;
     // A part request (attachment load, for now) was sent to SyncManager
     public static final int SYNC_SERVICE_PART_REQUEST = 5;
     // Misc.
     public static final int SYNC_KICK = 6;
 
     private static final String WHERE_PUSH_OR_PING_NOT_ACCOUNT_MAILBOX =
         MailboxColumns.ACCOUNT_KEY + "=? and " + MailboxColumns.TYPE + "!=" +
         Mailbox.TYPE_EAS_ACCOUNT_MAILBOX + " and " + MailboxColumns.SYNC_INTERVAL +
         " IN (" + Mailbox.CHECK_INTERVAL_PING + ',' + Mailbox.CHECK_INTERVAL_PUSH + ')';
     protected static final String WHERE_IN_ACCOUNT_AND_PUSHABLE =
         MailboxColumns.ACCOUNT_KEY + "=? and type in (" + Mailbox.TYPE_INBOX + ','
         /*+ Mailbox.TYPE_CALENDAR + ','*/ + Mailbox.TYPE_CONTACTS + ')';
     private static final String WHERE_MAILBOX_KEY = EmailContent.RECORD_ID + "=?";
     private static final String WHERE_PROTOCOL_EAS = HostAuthColumns.PROTOCOL + "=\"" +
         AbstractSyncService.EAS_PROTOCOL + "\"";
 
     // Offsets into the syncStatus data for EAS that indicate type, exit status, and change count
     // The format is S<type_char>:<exit_char>:<change_count>
     public static final int STATUS_TYPE_CHAR = 1;
     public static final int STATUS_EXIT_CHAR = 3;
     public static final int STATUS_CHANGE_COUNT_OFFSET = 5;
 
     // Ready for ping
     public static final int PING_STATUS_OK = 0;
     // Service already running (can't ping)
     public static final int PING_STATUS_RUNNING = 1;
     // Service waiting after I/O error (can't ping)
     public static final int PING_STATUS_WAITING = 2;
     // Service had a fatal error; can't run
     public static final int PING_STATUS_UNABLE = 3;
 
     // We synchronize on this for all actions affecting the service and error maps
     private static Object sSyncToken = new Object();
     // All threads can use this lock to wait for connectivity
     public static Object sConnectivityLock = new Object();
 
     // Keeps track of running services (by mailbox id)
     private HashMap<Long, AbstractSyncService> mServiceMap =
         new HashMap<Long, AbstractSyncService>();
     // Keeps track of services whose last sync ended with an error (by mailbox id)
     private HashMap<Long, SyncError> mSyncErrorMap = new HashMap<Long, SyncError>();
     // Keeps track of which services require a wake lock (by mailbox id)
     private HashMap<Long, Boolean> mWakeLocks = new HashMap<Long, Boolean>();
     // Keeps track of PendingIntents for mailbox alarms (by mailbox id)
     static private HashMap<Long, PendingIntent> sPendingIntents =
         new HashMap<Long, PendingIntent>();
     // The actual WakeLock obtained by SyncManager
     private WakeLock mWakeLock = null;
 
     // Observers that we use to look for changed mail-related data
     private AccountObserver mAccountObserver;
     private MailboxObserver mMailboxObserver;
     private SyncedMessageObserver mSyncedMessageObserver;
     private MessageObserver mMessageObserver;
     private Handler mHandler = new Handler();
 
     private ContentResolver mResolver;
 
     // The singleton SyncManager object, with its thread and stop flag
     protected static SyncManager INSTANCE;
     private static Thread sServiceThread = null;
     private boolean mStop = false;
 
     // The reason for SyncManager's next wakeup call
     private String mNextWaitReason;
 
     // Receiver of connectivity broadcasts
     private ConnectivityReceiver mConnectivityReceiver = null;
 
     // Cached unique device id
     private String mDeviceId = null;
 
     // The callback sent in from the UI using setCallback
     private IEmailServiceCallback mCallback;
     private RemoteCallbackList<IEmailServiceCallback> mCallbackList =
         new RemoteCallbackList<IEmailServiceCallback>();
 
     /**
      * Proxy that can be used by various sync adapters to tie into SyncManager's callback system.
      * Used this way:  SyncManager.callback().callbackMethod(args...);
      * The proxy wraps checking for existence of a SyncManager instance and an active callback.
      * Failures of these callbacks can be safely ignored.
      */
     static private final IEmailServiceCallback.Stub sCallbackProxy =
         new IEmailServiceCallback.Stub() {
 
         public void loadAttachmentStatus(long messageId, long attachmentId, int statusCode,
                 int progress) throws RemoteException {
             IEmailServiceCallback cb = INSTANCE == null ? null: INSTANCE.mCallback;
             if (cb != null) {
                 cb.loadAttachmentStatus(messageId, attachmentId, statusCode, progress);
             }
         }
 
         public void sendMessageStatus(long messageId, String subject, int statusCode, int progress)
                 throws RemoteException {
             IEmailServiceCallback cb = INSTANCE == null ? null: INSTANCE.mCallback;
             if (cb != null) {
                 cb.sendMessageStatus(messageId, subject, statusCode, progress);
             }
         }
 
         public void syncMailboxListStatus(long accountId, int statusCode, int progress)
                 throws RemoteException {
             IEmailServiceCallback cb = INSTANCE == null ? null: INSTANCE.mCallback;
             if (cb != null) {
                 cb.syncMailboxListStatus(accountId, statusCode, progress);
             }
         }
 
         public void syncMailboxStatus(long mailboxId, int statusCode, int progress)
                 throws RemoteException {
             IEmailServiceCallback cb = INSTANCE == null ? null: INSTANCE.mCallback;
             if (cb != null) {
                 cb.syncMailboxStatus(mailboxId, statusCode, progress);
             } else if (INSTANCE != null) {
                 INSTANCE.log("orphan syncMailboxStatus, id=" + mailboxId + " status=" + statusCode);
             }
         }
     };
 
     /**
      * Create our EmailService implementation here.
      */
     private final IEmailService.Stub mBinder = new IEmailService.Stub() {
 
         public int validate(String protocol, String host, String userName, String password,
                 int port, boolean ssl) throws RemoteException {
             try {
                 AbstractSyncService.validate(EasSyncService.class, host, userName, password, port,
                         ssl, SyncManager.this);
                 return MessagingException.NO_ERROR;
             } catch (MessagingException e) {
                 return e.getExceptionType();
             }
         }
 
         public void startSync(long mailboxId) throws RemoteException {
             if (INSTANCE == null) return;
             Mailbox m = Mailbox.restoreMailboxWithId(INSTANCE, mailboxId);
             if (m.mType == Mailbox.TYPE_OUTBOX) {
                 // We're using SERVER_ID to indicate an error condition (it has no other use for
                 // sent mail)  Upon request to sync the Outbox, we clear this so that all messages
                 // are candidates for sending.
                 ContentValues cv = new ContentValues();
                 cv.put(SyncColumns.SERVER_ID, 0);
                 INSTANCE.getContentResolver().update(Message.CONTENT_URI,
                     cv, WHERE_MAILBOX_KEY, new String[] {Long.toString(mailboxId)});
 
                 kick("start outbox");
             }
             startManualSync(mailboxId, SyncManager.SYNC_SERVICE_START_SYNC, null);
         }
 
         public void stopSync(long mailboxId) throws RemoteException {
             stopManualSync(mailboxId);
         }
 
         public void loadAttachment(long attachmentId, String destinationFile,
                 String contentUriString) throws RemoteException {
             Attachment att = Attachment.restoreAttachmentWithId(SyncManager.this, attachmentId);
             partRequest(new PartRequest(att, destinationFile, contentUriString));
         }
 
         public void updateFolderList(long accountId) throws RemoteException {
             reloadFolderList(SyncManager.this, accountId, false);
         }
 
         public void setLogging(int on) throws RemoteException {
             Eas.setUserDebug(on);
         }
 
         public void loadMore(long messageId) throws RemoteException {
             // TODO Auto-generated method stub
         }
 
         // The following three methods are not implemented in this version
         public boolean createFolder(long accountId, String name) throws RemoteException {
             return false;
         }
 
         public boolean deleteFolder(long accountId, String name) throws RemoteException {
             return false;
         }
 
         public boolean renameFolder(long accountId, String oldName, String newName)
                 throws RemoteException {
             return false;
         }
 
         public void setCallback(IEmailServiceCallback cb) throws RemoteException {
             if (mCallback != null) {
                 mCallbackList.unregister(mCallback);
             }
             mCallback = cb;
             mCallbackList.register(cb);
         }
     };
 
     class AccountList extends ArrayList<Account> {
         private static final long serialVersionUID = 1L;
 
         public boolean contains(long id) {
             for (Account account: this) {
                 if (account.mId == id) {
                     return true;
                 }
             }
             return false;
         }
     }
 
     class AccountObserver extends ContentObserver {
         // mAccounts keeps track of Accounts that we care about (EAS for now)
         AccountList mAccounts = new AccountList();
 
         public AccountObserver(Handler handler) {
             super(handler);
             Context context = getContext();
 
             // At startup, we want to see what EAS accounts exist and cache them
             Cursor c = getContentResolver().query(Account.CONTENT_URI, Account.CONTENT_PROJECTION,
                     null, null, null);
             try {
                 collectEasAccounts(c, mAccounts);
             } finally {
                 c.close();
             }
 
             // Create the account mailbox for any account that doesn't have one
             for (Account account: mAccounts) {
                 int cnt = Mailbox.count(context, Mailbox.CONTENT_URI, "accountKey=" + account.mId,
                         null);
                 if (cnt == 0) {
                     addAccountMailbox(account.mId);
                 }
             }
         }
 
         private boolean syncParametersChanged(Account account) {
             long accountId = account.mId;
             // Reload account from database to get its current state
             account = Account.restoreAccountWithId(getContext(), accountId);
             for (Account oldAccount: mAccounts) {
                 if (oldAccount.mId == accountId) {
                     return oldAccount.mSyncInterval != account.mSyncInterval ||
                             oldAccount.mSyncLookback != account.mSyncLookback;
                 }
             }
             // Really, we can't get here, but we don't want the compiler to complain
             return false;
         }
 
         @Override
         public void onChange(boolean selfChange) {
             maybeStartSyncManagerThread();
 
             // A change to the list requires us to scan for deletions (to stop running syncs)
             // At startup, we want to see what accounts exist and cache them
             AccountList currentAccounts = new AccountList();
             Cursor c = getContentResolver().query(Account.CONTENT_URI, Account.CONTENT_PROJECTION,
                     null, null, null);
             try {
                 collectEasAccounts(c, currentAccounts);
                 for (Account account : mAccounts) {
                     if (!currentAccounts.contains(account.mId)) {
                         // This is a deletion; shut down any account-related syncs
                         stopAccountSyncs(account.mId, true);
                         // Delete this from AccountManager...
                         android.accounts.Account acct =
                             new android.accounts.Account(Eas.ACCOUNT_MANAGER_TYPE,
                                     account.mEmailAddress);
                         AccountManager.get(SyncManager.this).removeAccount(acct, null, null);
                     } else {
                         // See whether any of our accounts has changed sync interval or window
                         if (syncParametersChanged(account)) {
                             // Here's one that has...
                             INSTANCE.log("Account " + account.mDisplayName +
                                     " changed; stopping running syncs...");
                             // If account is push, set contacts and inbox to push
                             Account updatedAccount =
                                 Account.restoreAccountWithId(getContext(), account.mId);
                             if (updatedAccount.mSyncInterval == Account.CHECK_INTERVAL_PUSH) {
                                 ContentValues cv = new ContentValues();
                                 cv.put(MailboxColumns.SYNC_INTERVAL, Mailbox.CHECK_INTERVAL_PUSH);
                                 getContext().getContentResolver().update(Mailbox.CONTENT_URI, cv,
                                         WHERE_IN_ACCOUNT_AND_PUSHABLE,
                                         new String[] {Long.toString(account.mId)});
                             }
                             // Stop all current syncs; the appropriate ones will restart
                             stopAccountSyncs(account.mId, true);
                         }
                     }
                 }
 
                 // Look for new accounts
                 for (Account account: currentAccounts) {
                     if (!mAccounts.contains(account.mId)) {
                         // This is an addition; create our magic hidden mailbox...
                         addAccountMailbox(account.mId);
                         mAccounts.add(account);
                     }
                 }
 
                 // Finally, make sure mAccounts is up to date
                 mAccounts = currentAccounts;
             } finally {
                 c.close();
             }
 
             // See if there's anything to do...
             kick("account changed");
         }
 
         private void collectEasAccounts(Cursor c, ArrayList<Account> accounts) {
             Context context = getContext();
             while (c.moveToNext()) {
                 long hostAuthId = c.getLong(Account.CONTENT_HOST_AUTH_KEY_RECV_COLUMN);
                 if (hostAuthId > 0) {
                     HostAuth ha = HostAuth.restoreHostAuthWithId(context, hostAuthId);
                     if (ha != null && ha.mProtocol.equals("eas")) {
                         accounts.add(new Account().restore(c));
                     }
                 }
             }
         }
 
         private void addAccountMailbox(long acctId) {
             Account acct = Account.restoreAccountWithId(getContext(), acctId);
             Mailbox main = new Mailbox();
             main.mDisplayName = Eas.ACCOUNT_MAILBOX;
             main.mServerId = Eas.ACCOUNT_MAILBOX + System.nanoTime();
             main.mAccountKey = acct.mId;
             main.mType = Mailbox.TYPE_EAS_ACCOUNT_MAILBOX;
             main.mSyncInterval = Mailbox.CHECK_INTERVAL_PUSH;
             main.mFlagVisible = false;
             main.save(getContext());
             INSTANCE.log("Initializing account: " + acct.mDisplayName);
         }
 
         private void stopAccountSyncs(long acctId, boolean includeAccountMailbox) {
             synchronized (sSyncToken) {
                 List<Long> deletedBoxes = new ArrayList<Long>();
                 for (Long mid : INSTANCE.mServiceMap.keySet()) {
                     Mailbox box = Mailbox.restoreMailboxWithId(INSTANCE, mid);
                     if (box != null) {
                         if (box.mAccountKey == acctId) {
                             if (!includeAccountMailbox &&
                                     box.mType == Mailbox.TYPE_EAS_ACCOUNT_MAILBOX) {
                                 AbstractSyncService svc = INSTANCE.mServiceMap.get(mid);
                                 if (svc != null) {
                                     svc.stop();
                                 }
                                 continue;
                             }
                             AbstractSyncService svc = INSTANCE.mServiceMap.get(mid);
                             if (svc != null) {
                                 svc.stop();
                                 svc.mThread.interrupt();
                             }
                             deletedBoxes.add(mid);
                         }
                     }
                 }
                 for (Long mid : deletedBoxes) {
                     releaseMailbox(mid);
                 }
             }
         }
     }
 
     class MailboxObserver extends ContentObserver {
         public MailboxObserver(Handler handler) {
             super(handler);
         }
 
         @Override
         public void onChange(boolean selfChange) {
             // See if there's anything to do...
             if (!selfChange) {
                 kick("mailbox changed");
             }
         }
     }
 
     class SyncedMessageObserver extends ContentObserver {
         long maxChangedId = 0;
         long maxDeletedId = 0;
         Intent syncAlarmIntent = new Intent(INSTANCE, EmailSyncAlarmReceiver.class);
         PendingIntent syncAlarmPendingIntent =
             PendingIntent.getBroadcast(INSTANCE, 0, syncAlarmIntent, 0);
         AlarmManager alarmManager = (AlarmManager)INSTANCE.getSystemService(Context.ALARM_SERVICE);
         final String[] MAILBOX_DATA_PROJECTION = {MessageColumns.MAILBOX_KEY, SyncColumns.DATA};
 
         public SyncedMessageObserver(Handler handler) {
             super(handler);
         }
 
         @Override
         public void onChange(boolean selfChange) {
             INSTANCE.log("SyncedMessage changed: (re)setting alarm for 10s");
             alarmManager.set(AlarmManager.RTC_WAKEUP,
                     System.currentTimeMillis() + 10*SECONDS, syncAlarmPendingIntent);
         }
     }
 
     class MessageObserver extends ContentObserver {
 
         public MessageObserver(Handler handler) {
             super(handler);
         }
 
         @Override
         public void onChange(boolean selfChange) {
             // A rather blunt instrument here.  But we don't have information about the URI that
             // triggered this, though it must have been an insert
             if (!selfChange) {
                 kick(null);
             }
         }
     }
 
     static public IEmailServiceCallback callback() {
         return sCallbackProxy;
     }
 
     static public AccountList getAccountList() {
         if (INSTANCE != null) {
             return INSTANCE.mAccountObserver.mAccounts;
         } else {
             return null;
         }
     }
 
     public class SyncStatus {
         static public final int NOT_RUNNING = 0;
         static public final int DIED = 1;
         static public final int SYNC = 2;
         static public final int IDLE = 3;
     }
 
     class SyncError {
         int reason;
         boolean fatal = false;
         long holdEndTime;
         long holdDelay = 0;
 
         SyncError(int _reason, boolean _fatal) {
             reason = _reason;
             fatal = _fatal;
             escalate();
         }
 
         /**
          * We increase the hold on I/O errors in 30 second increments to 5 minutes
          */
         void escalate() {
             if (holdDelay < HOLD_DELAY_MAXIMUM) {
                 holdDelay += HOLD_DELAY_ESCALATION;
             }
             holdEndTime = System.currentTimeMillis() + holdDelay;
         }
     }
 
     static public void smLog(String str) {
         if (INSTANCE != null) {
             INSTANCE.log(str);
         }
     }
 
     protected void log(String str) {
         if (Eas.USER_LOG) {
             Log.d(TAG, str);
             if (Eas.FILE_LOG) {
                 FileLogger.log(TAG, str);
             }
         }
     }
 
     /**
      * EAS requires a unique device id, so that sync is possible from a variety of different
      * devices (e.g. the syncKey is specific to a device)  If we're on an emulator or some other
      * device that doesn't provide one, we can create it as droid<n> where <n> is system time.
      * This would work on a real device as well, but it would be better to use the "real" id if
      * it's available
      */
     static public synchronized String getDeviceId() throws IOException {
         if (INSTANCE == null) {
             throw new IOException();
         }
         // If we've already got the id, return it
         if (INSTANCE.mDeviceId != null) {
             return INSTANCE.mDeviceId;
         }
 
         // Otherwise, we'll read the id file or create one if it's not found
         try {
             File f = INSTANCE.getFileStreamPath("deviceName");
             BufferedReader rdr = null;
             String id;
             if (f.exists() && f.canRead()) {
                 rdr = new BufferedReader(new FileReader(f), 128);
                 id = rdr.readLine();
                 rdr.close();
                 return id;
             } else if (f.createNewFile()) {
                 BufferedWriter w = new BufferedWriter(new FileWriter(f), 128);
                 id = "droid" + System.currentTimeMillis();
                 w.write(id);
                 w.close();
                 return id;
             }
         } catch (FileNotFoundException e) {
             // We'll just use the default below
             Log.e(TAG, "Can't get device name!");
         } catch (IOException e) {
             // We'll just use the default below
             Log.e(TAG, "Can't get device name!");
         }
         throw new IOException();
     }
 
     @Override
     public IBinder onBind(Intent arg0) {
         return mBinder;
     }
 
     @Override
     public void onCreate() {
         if (INSTANCE != null) {
             Log.d(TAG, "onCreate called on running SyncManager");
         } else {
             INSTANCE = this;
             mAccountObserver = new AccountObserver(mHandler);
             mMailboxObserver = new MailboxObserver(mHandler);
             mSyncedMessageObserver = new SyncedMessageObserver(mHandler);
             mMessageObserver = new MessageObserver(mHandler);
 
             try {
                 mDeviceId = getDeviceId();
             } catch (IOException e) {
                 // We can't run in this situation
                 throw new RuntimeException();
             }
         }
 
         mResolver = getContentResolver();
         mResolver.registerContentObserver(Account.CONTENT_URI, false, mAccountObserver);
 
         maybeStartSyncManagerThread();
     }
 
     @Override
     public void onDestroy() {
         log("!!! SyncManager onDestroy");
 
         // Cover the case in which we never really started (no EAS accounts)
         if (INSTANCE == null) {
             return;
         }
 
         stopServices();
         // Stop receivers and content observers
         if (mConnectivityReceiver != null) {
             unregisterReceiver(mConnectivityReceiver);
         }
         ContentResolver resolver = getContentResolver();
         resolver.unregisterContentObserver(mAccountObserver);
         resolver.unregisterContentObserver(mMailboxObserver);
         resolver.unregisterContentObserver(mSyncedMessageObserver);
         resolver.unregisterContentObserver(mMessageObserver);
 
         // Clear pending alarms
         clearAlarms();
 
         // Release our wake lock, if we have one
         synchronized (mWakeLocks) {
             if (mWakeLock != null) {
                 mWakeLock.release();
                 mWakeLock = null;
             }
         }
 
         sPendingIntents.clear();
 
         INSTANCE = null;
     }
 
     void maybeStartSyncManagerThread() {
         // Start our thread...
         // See if there are any EAS accounts; otherwise, just go away
         if (EmailContent.count(this, HostAuth.CONTENT_URI, WHERE_PROTOCOL_EAS, null) > 0) {
             if (sServiceThread == null || !sServiceThread.isAlive()) {
                 log(sServiceThread == null ? "Starting thread..." : "Restarting thread...");
                 sServiceThread = new Thread(this, "SyncManager");
                 sServiceThread.start();
             }
         }
     }
 
     static public void reloadFolderList(Context context, long accountId, boolean force) {
         if (INSTANCE == null) return;
         Cursor c = context.getContentResolver().query(Mailbox.CONTENT_URI,
                 Mailbox.CONTENT_PROJECTION, MailboxColumns.ACCOUNT_KEY + "=? AND " +
                 MailboxColumns.TYPE + "=?",
                 new String[] {Long.toString(accountId),
                     Long.toString(Mailbox.TYPE_EAS_ACCOUNT_MAILBOX)}, null);
         try {
             if (c.moveToFirst()) {
                 synchronized(sSyncToken) {
                     Mailbox m = new Mailbox().restore(c);
                     String syncKey = m.mSyncKey;
                     // No need to reload the list if we don't have one
                     if (!force && (syncKey == null || syncKey.equals("0"))) {
                         return;
                     }
 
                     // Change all ping/push boxes to push/hold
                     ContentValues cv = new ContentValues();
                     cv.put(Mailbox.SYNC_INTERVAL, Mailbox.CHECK_INTERVAL_PUSH_HOLD);
                     context.getContentResolver().update(Mailbox.CONTENT_URI, cv,
                             WHERE_PUSH_OR_PING_NOT_ACCOUNT_MAILBOX,
                             new String[] {Long.toString(accountId)});
                     INSTANCE.log("Set push/ping boxes to push/hold");
 
                     long id = m.mId;
                     AbstractSyncService svc = INSTANCE.mServiceMap.get(id);
                     // Tell the service we're done
                     if (svc != null) {
                         synchronized (svc.getSynchronizer()) {
                             svc.stop();
                         }
                         // Interrupt the thread so that it can stop
                         Thread thread = svc.mThread;
                         thread.setName(thread.getName() + " (Stopped)");
                         thread.interrupt();
                         // Abandon the service
                         INSTANCE.releaseMailbox(id);
                         // And have it start naturally
                         kick("reload folder list");
                     }
                 }
             }
         } finally {
             c.close();
         }
     }
 
     /**
      * Informs SyncManager that an account has a new folder list; as a result, any existing folder
      * might have become invalid.  Therefore, we act as if the account has been deleted, and then
      * we reinitialize it.
      *
      * @param acctId
      */
     static public void folderListReloaded(long acctId) {
         if (INSTANCE != null) {
             AccountObserver obs = INSTANCE.mAccountObserver;
             obs.stopAccountSyncs(acctId, false);
             kick("reload folder list");
         }
     }
 
 //    private void logLocks(String str) {
 //        StringBuilder sb = new StringBuilder(str);
 //        boolean first = true;
 //        for (long id: mWakeLocks.keySet()) {
 //            if (!first) {
 //                sb.append(", ");
 //            } else {
 //                first = false;
 //            }
 //            sb.append(id);
 //        }
 //        log(sb.toString());
 //    }
 
     private void acquireWakeLock(long id) {
         synchronized (mWakeLocks) {
             Boolean lock = mWakeLocks.get(id);
             if (lock == null) {
                 if (id > 0) {
                     //log("+WakeLock requested for " + alarmOwner(id));
                 }
                 if (mWakeLock == null) {
                     PowerManager pm = (PowerManager)getSystemService(Context.POWER_SERVICE);
                     mWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "MAIL_SERVICE");
                     mWakeLock.acquire();
                     log("+WAKE LOCK ACQUIRED");
                 }
                 mWakeLocks.put(id, true);
                 //logLocks("Post-acquire of WakeLock for " + alarmOwner(id) + ": ");
             }
         }
     }
 
     private void releaseWakeLock(long id) {
         synchronized (mWakeLocks) {
             Boolean lock = mWakeLocks.get(id);
             if (lock != null) {
                 if (id > 0) {
                     //log("+WakeLock not needed for " + alarmOwner(id));
                 }
                 mWakeLocks.remove(id);
                 if (mWakeLocks.isEmpty()) {
                     if (mWakeLock != null) {
                         mWakeLock.release();
                     }
                     mWakeLock = null;
                     log("+WAKE LOCK RELEASED");
                 } else {
                     //logLocks("Post-release of WakeLock for " + alarmOwner(id) + ": ");
                 }
             }
         }
     }
 
     static public String alarmOwner(long id) {
         if (id == SYNC_MANAGER_ID) {
             return "SyncManager";
         } else
             return "Mailbox " + Long.toString(id);
     }
 
     private void clearAlarm(long id) {
         synchronized (sPendingIntents) {
             PendingIntent pi = sPendingIntents.get(id);
             if (pi != null) {
                 AlarmManager alarmManager = (AlarmManager)getSystemService(Context.ALARM_SERVICE);
                 alarmManager.cancel(pi);
                 //log("+Alarm cleared for " + alarmOwner(id));
                 sPendingIntents.remove(id);
             }
         }
     }
 
     private void setAlarm(long id, long millis) {
         synchronized (sPendingIntents) {
             PendingIntent pi = sPendingIntents.get(id);
             if (pi == null) {
                 Intent i = new Intent(this, MailboxAlarmReceiver.class);
                 i.putExtra("mailbox", id);
                 i.setData(Uri.parse("Box" + id));
                 pi = PendingIntent.getBroadcast(this, 0, i, 0);
                 sPendingIntents.put(id, pi);
 
                 AlarmManager alarmManager = (AlarmManager)getSystemService(Context.ALARM_SERVICE);
                 alarmManager.set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + millis, pi);
                 //log("+Alarm set for " + alarmOwner(id) + ", " + millis/1000 + "s");
             }
         }
     }
 
     private void clearAlarms() {
         AlarmManager alarmManager = (AlarmManager)getSystemService(Context.ALARM_SERVICE);
         synchronized (sPendingIntents) {
             for (PendingIntent pi : sPendingIntents.values()) {
                 alarmManager.cancel(pi);
             }
             sPendingIntents.clear();
         }
     }
 
     static public void runAwake(long id) {
         if (INSTANCE == null) return;
         INSTANCE.acquireWakeLock(id);
         INSTANCE.clearAlarm(id);
     }
 
     static public void runAsleep(long id, long millis) {
         if (INSTANCE == null) return;
         INSTANCE.setAlarm(id, millis);
         INSTANCE.releaseWakeLock(id);
     }
 
     static public void ping(long id) {
         if (id < 0) {
             kick("ping SyncManager");
         } else {
             AbstractSyncService service = INSTANCE.mServiceMap.get(id);
             if (service != null) {
                 Mailbox m = Mailbox.restoreMailboxWithId(INSTANCE, id);
                 if (m != null) {
                     service.mAccount = Account.restoreAccountWithId(INSTANCE, m.mAccountKey);
                     service.mMailbox = m;
                     service.ping();
                 }
             }
         }
     }
 
     private void releaseConnectivityLock(String reason) {
         // Clear our sync error map when we get connected
         mSyncErrorMap.clear();
         synchronized (sConnectivityLock) {
             sConnectivityLock.notifyAll();
         }
         kick(reason);
     }
 
     public class ConnectivityReceiver extends BroadcastReceiver {
         @Override
         public void onReceive(Context context, Intent intent) {
             Bundle b = intent.getExtras();
             if (b != null) {
                 NetworkInfo a = (NetworkInfo)b.get(ConnectivityManager.EXTRA_NETWORK_INFO);
                 String info = "Connectivity alert for " + a.getTypeName();
                 State state = a.getState();
                 if (state == State.CONNECTED) {
                     info += " CONNECTED";
                     log(info);
                     releaseConnectivityLock("connected");
                 } else if (state == State.DISCONNECTED) {
                     info += " DISCONNECTED";
                     a = (NetworkInfo)b.get(ConnectivityManager.EXTRA_OTHER_NETWORK_INFO);
                     if (a != null && a.getState() == State.CONNECTED) {
                         info += " (OTHER CONNECTED)";
                         releaseConnectivityLock("disconnect/other");
                         ConnectivityManager cm =
                             (ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE);
                         if (cm != null) {
                             NetworkInfo i = cm.getActiveNetworkInfo();
                             if (i == null || i.getState() != State.CONNECTED) {
                                 log("CM says we're connected, but no active info?");
                             }
                         }
                     } else {
                         log(info);
                         kick("disconnected");
                     }
                 }
             }
         }
     }
 
     private void startService(AbstractSyncService service, Mailbox m) {
         synchronized (sSyncToken) {
             String mailboxName = m.mDisplayName;
             String accountName = service.mAccount.mDisplayName;
             Thread thread = new Thread(service, mailboxName + "(" + accountName + ")");
             log("Starting thread for " + mailboxName + " in account " + accountName);
             thread.start();
             mServiceMap.put(m.mId, service);
             runAwake(m.mId);
         }
     }
 
     private void startService(Mailbox m, int reason, PartRequest req) {
         synchronized (sSyncToken) {
             Account acct = Account.restoreAccountWithId(this, m.mAccountKey);
             if (acct != null) {
                 AbstractSyncService service;
                 service = new EasSyncService(this, m);
                 service.mSyncReason = reason;
                 if (req != null) {
                     service.addPartRequest(req);
                 }
                 startService(service, m);
             }
         }
     }
 
     private void stopServices() {
         synchronized (sSyncToken) {
             ArrayList<Long> toStop = new ArrayList<Long>();
 
             // Keep track of which services to stop
             for (Long mailboxId : mServiceMap.keySet()) {
                 toStop.add(mailboxId);
             }
 
             // Shut down all of those running services
             for (Long mailboxId : toStop) {
                 AbstractSyncService svc = mServiceMap.get(mailboxId);
                 if (svc != null) {
                     log("Stopping " + svc.mAccount.mDisplayName + '/' + svc.mMailbox.mDisplayName);
                     svc.stop();
                     if (svc.mThread != null) {
                         svc.mThread.interrupt();
                     }
                 }
                 releaseWakeLock(mailboxId);
             }
         }
     }
 
     private void waitForConnectivity() {
         int cnt = 0;
         while (!mStop) {
             ConnectivityManager cm =
                 (ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE);
             NetworkInfo info = cm.getActiveNetworkInfo();
             if (info != null) {
                 log("NetworkInfo: " + info.getTypeName() + ", " + info.getState().name());
                 return;
             } else {
 
                 // If we're waiting for the long haul, shut down running service threads
                 if (++cnt > 1) {
                     stopServices();
                 }
 
                 // Wait until a network is connected, but let the device sleep
                 // We'll set an alarm just in case we don't get notified (bugs happen)
                 synchronized (sConnectivityLock) {
                     runAsleep(SYNC_MANAGER_ID, CONNECTIVITY_WAIT_TIME+5*SECONDS);
                     try {
                         log("Connectivity lock...");
                         sConnectivityLock.wait(CONNECTIVITY_WAIT_TIME);
                         log("Connectivity lock released...");
                     } catch (InterruptedException e) {
                     }
                     runAwake(SYNC_MANAGER_ID);
                 }
             }
         }
     }
 
     public void run() {
         mStop = false;
 
         // If we're really debugging, turn on all logging
         if (Eas.DEBUG) {
             Eas.USER_LOG = true;
             Eas.PARSER_LOG = true;
             Eas.FILE_LOG = true;
         }
 
         // If we need to wait for the debugger, do so
         if (Eas.WAIT_DEBUG) {
             Debug.waitForDebugger();
         }
 
         // Set up our observers; we need them to know when to start/stop various syncs based
         // on the insert/delete/update of mailboxes and accounts
         // We also observe synced messages to trigger upsyncs at the appropriate time
         mResolver.registerContentObserver(Mailbox.CONTENT_URI, false, mMailboxObserver);
         mResolver.registerContentObserver(Message.SYNCED_CONTENT_URI, true, mSyncedMessageObserver);
        mResolver.registerContentObserver(Message.CONTENT_URI, false, mMessageObserver);
 
         mConnectivityReceiver = new ConnectivityReceiver();
         registerReceiver(mConnectivityReceiver,
                 new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
 
         try {
             while (!mStop) {
                 runAwake(SYNC_MANAGER_ID);
                 waitForConnectivity();
                 mNextWaitReason = "Heartbeat";
                 long nextWait = checkMailboxes();
                 try {
                     synchronized (this) {
                         if (nextWait < 0) {
                             log("Negative wait? Setting to 1s");
                             nextWait = 1*SECONDS;
                         }
                         if (nextWait > 30*SECONDS) {
                             runAsleep(SYNC_MANAGER_ID, nextWait - 1000);
                         }
                         if (nextWait != SYNC_MANAGER_HEARTBEAT_TIME) {
                             log("Next awake in " + nextWait / 1000 + "s: " + mNextWaitReason);
                         }
                         wait(nextWait);
                     }
                 } catch (InterruptedException e) {
                     // Needs to be caught, but causes no problem
                 }
             }
             stopServices();
             log("Shutdown requested");
         } finally {
             log("Goodbye");
         }
 
         startService(new Intent(this, SyncManager.class));
         throw new RuntimeException("EAS SyncManager crash; please restart me...");
     }
 
     private void releaseMailbox(long mailboxId) {
         mServiceMap.remove(mailboxId);
         releaseWakeLock(mailboxId);
     }
 
     private long checkMailboxes () {
         // First, see if any running mailboxes have been deleted
         ArrayList<Long> deletedMailboxes = new ArrayList<Long>();
         synchronized (sSyncToken) {
             for (long mailboxId: mServiceMap.keySet()) {
                 Mailbox m = Mailbox.restoreMailboxWithId(this, mailboxId);
                 if (m == null) {
                     deletedMailboxes.add(mailboxId);
                 }
             }
         }
         // If so, stop them or remove them from the map
         for (Long mailboxId: deletedMailboxes) {
             AbstractSyncService svc = mServiceMap.get(mailboxId);
             if (svc != null) {
                 boolean alive = svc.mThread.isAlive();
                 log("Deleted mailbox: " + svc.mMailboxName);
                 if (alive) {
                     stopManualSync(mailboxId);
                 } else {
                     log("Removing from serviceMap");
                     releaseMailbox(mailboxId);
                 }
             }
         }
 
         long nextWait = SYNC_MANAGER_HEARTBEAT_TIME;
         long now = System.currentTimeMillis();
         // Start up threads that need it...
         Cursor c = getContentResolver().query(Mailbox.CONTENT_URI,
                 Mailbox.CONTENT_PROJECTION, null, null, null);
         try {
             while (c.moveToNext()) {
                 // TODO Could be much faster - just get cursor of
                 // ones we're watching...
                 long aid = c.getLong(Mailbox.CONTENT_ACCOUNT_KEY_COLUMN);
                 // Only check mailboxes for EAS accounts
                 if (!mAccountObserver.mAccounts.contains(aid)) {
                     continue;
                 }
                 long mid = c.getLong(Mailbox.CONTENT_ID_COLUMN);
                 AbstractSyncService service = mServiceMap.get(mid);
                 if (service == null) {
                     // Check whether we're in a hold (temporary or permanent)
                     SyncError syncError = mSyncErrorMap.get(mid);
                     if (syncError != null) {
                         // Nothing we can do about fatal errors
                         if (syncError.fatal) continue;
                         if (now < syncError.holdEndTime) {
                             // If release time is earlier than next wait time,
                             // move next wait time up to the release time
                             if (syncError.holdEndTime < now + nextWait) {
                                 nextWait = syncError.holdEndTime - now;
                                 mNextWaitReason = "Release hold";
                             }
                             continue;
                         } else {
                             // The hold has ended; remove from the error map
                             mSyncErrorMap.remove(mid);
                         }
                     }
                     long freq = c.getInt(Mailbox.CONTENT_SYNC_INTERVAL_COLUMN);
                     if (freq == Mailbox.CHECK_INTERVAL_PUSH) {
                         Mailbox m = EmailContent.getContent(c, Mailbox.class);
                         startService(m, SYNC_PUSH, null);
                     } else if (c.getInt(Mailbox.CONTENT_TYPE_COLUMN) == Mailbox.TYPE_OUTBOX) {
                         int cnt = EmailContent.count(this, Message.CONTENT_URI,
                                 EasOutboxService.MAILBOX_KEY_AND_NOT_SEND_FAILED,
                                 new String[] {Long.toString(mid)});
                         if (cnt > 0) {
                             Mailbox m = EmailContent.getContent(c, Mailbox.class);
                             startService(new EasOutboxService(this, m), m);
                         }
                     } else if (freq > 0 && freq <= ONE_DAY_MINUTES) {
                         long lastSync = c.getLong(Mailbox.CONTENT_SYNC_TIME_COLUMN);
                         long toNextSync = freq*MINUTES - (now - lastSync);
                         if (toNextSync <= 0) {
                             Mailbox m = EmailContent.getContent(c, Mailbox.class);
                             startService(m, SYNC_SCHEDULED, null);
                         } else if (toNextSync < nextWait) {
                             nextWait = toNextSync;
                             mNextWaitReason = "Scheduled sync, "
                                 + c.getString(Mailbox.CONTENT_DISPLAY_NAME_COLUMN);
                         }
                     }
                 } else {
                     Thread thread = service.mThread;
                     if (thread != null && !thread.isAlive()) {
                         releaseMailbox(mid);
                         // Restart this if necessary
                         if (nextWait > 3*SECONDS) {
                             nextWait = 3*SECONDS;
                             mNextWaitReason = "Clean up dead thread(s)";
                         }
                     } else {
                         long requestTime = service.mRequestTime;
                         if (requestTime > 0) {
                             long timeToRequest = requestTime - now;
                             if (service instanceof AbstractSyncService && timeToRequest <= 0) {
                                 service.mRequestTime = 0;
                                 service.ping();
                             } else if (requestTime > 0 && timeToRequest < nextWait) {
                                 if (timeToRequest < 11*MINUTES) {
                                     nextWait = timeToRequest < 250 ? 250 : timeToRequest;
                                     mNextWaitReason = "Sync data change";
                                 } else {
                                     log("Illegal timeToRequest: " + timeToRequest);
                                 }
                             }
                         }
                     }
                 }
             }
         } finally {
             c.close();
         }
         return nextWait;
     }
 
     static public void serviceRequest(long mailboxId, int reason) {
         serviceRequest(mailboxId, 5*SECONDS, reason);
     }
 
     static public void serviceRequest(long mailboxId, long ms, int reason) {
         if (INSTANCE == null) return;
         try {
             AbstractSyncService service = INSTANCE.mServiceMap.get(mailboxId);
             if (service != null) {
                 service.mRequestTime = System.currentTimeMillis() + ms;
                 kick("service request");
             } else {
                 startManualSync(mailboxId, reason, null);
             }
         } catch (Exception e) {
             e.printStackTrace();
         }
     }
 
     static public void serviceRequestImmediate(long mailboxId) {
         if (INSTANCE == null) return;
         AbstractSyncService service = INSTANCE.mServiceMap.get(mailboxId);
         if (service != null) {
             service.mRequestTime = System.currentTimeMillis();
             Mailbox m = Mailbox.restoreMailboxWithId(INSTANCE, mailboxId);
             if (m != null) {
                 service.mAccount = Account.restoreAccountWithId(INSTANCE, m.mAccountKey);
                 service.mMailbox = m;
                 kick("service request immediate");
             }
         }
     }
 
     static public void partRequest(PartRequest req) {
         if (INSTANCE == null) return;
         Message msg = Message.restoreMessageWithId(INSTANCE, req.emailId);
         if (msg == null) {
             return;
         }
         long mailboxId = msg.mMailboxKey;
         AbstractSyncService service = INSTANCE.mServiceMap.get(mailboxId);
 
         if (service == null) {
             service = startManualSync(mailboxId, SYNC_SERVICE_PART_REQUEST, req);
             kick("part request");
         } else {
             service.addPartRequest(req);
         }
     }
 
     static public PartRequest hasPartRequest(long emailId, String part) {
         if (INSTANCE == null) return null;
         Message msg = Message.restoreMessageWithId(INSTANCE, emailId);
         if (msg == null) {
             return null;
         }
         long mailboxId = msg.mMailboxKey;
         AbstractSyncService service = INSTANCE.mServiceMap.get(mailboxId);
         if (service != null) {
             return service.hasPartRequest(emailId, part);
         }
         return null;
     }
 
     static public void cancelPartRequest(long emailId, String part) {
         Message msg = Message.restoreMessageWithId(INSTANCE, emailId);
         if (msg == null) {
             return;
         }
         long mailboxId = msg.mMailboxKey;
         AbstractSyncService service = INSTANCE.mServiceMap.get(mailboxId);
         if (service != null) {
             service.cancelPartRequest(emailId, part);
         }
     }
 
     /**
      * Determine whether a given Mailbox can be synced, i.e. is not already syncing and is not in
      * an error state
      *
      * @param mailboxId
      * @return whether or not the Mailbox is available for syncing (i.e. is a valid push target)
      */
     static public int pingStatus(long mailboxId) {
         // Already syncing...
         if (INSTANCE.mServiceMap.get(mailboxId) != null) {
             return PING_STATUS_RUNNING;
         }
         // No errors or a transient error, don't ping...
         SyncError error = INSTANCE.mSyncErrorMap.get(mailboxId);
         if (error != null) {
             if (error.fatal) {
                 return PING_STATUS_UNABLE;
             } else {
                 return PING_STATUS_WAITING;
             }
         }
         return PING_STATUS_OK;
     }
 
     static public AbstractSyncService startManualSync(long mailboxId, int reason, PartRequest req) {
         if (INSTANCE == null || INSTANCE.mServiceMap == null) return null;
         synchronized (sSyncToken) {
             if (INSTANCE.mServiceMap.get(mailboxId) == null) {
                 INSTANCE.mSyncErrorMap.remove(mailboxId);
                 Mailbox m = Mailbox.restoreMailboxWithId(INSTANCE, mailboxId);
                 INSTANCE.log("Starting sync for " + m.mDisplayName);
                 INSTANCE.startService(m, reason, req);
             }
         }
         return INSTANCE.mServiceMap.get(mailboxId);
     }
 
     // DO NOT CALL THIS IN A LOOP ON THE SERVICEMAP
     static private void stopManualSync(long mailboxId) {
         if (INSTANCE == null || INSTANCE.mServiceMap == null) return;
         synchronized (sSyncToken) {
             AbstractSyncService svc = INSTANCE.mServiceMap.get(mailboxId);
             if (svc != null) {
                 INSTANCE.log("Stopping sync for " + svc.mMailboxName);
                 svc.stop();
                 svc.mThread.interrupt();
                 INSTANCE.releaseWakeLock(mailboxId);
             }
         }
     }
 
     /**
      * Wake up SyncManager to check for mailboxes needing service
      */
     static public void kick(String reason) {
         if (INSTANCE != null) {
             synchronized (INSTANCE) {
                 INSTANCE.notify();
             }
         }
         if (sConnectivityLock != null) {
             synchronized (sConnectivityLock) {
                 sConnectivityLock.notify();
             }
         }
     }
 
     static public void kick(long mailboxId) {
         if (INSTANCE == null) return;
         Mailbox m = Mailbox.restoreMailboxWithId(INSTANCE, mailboxId);
         int syncType = m.mSyncInterval;
         if (syncType == Mailbox.CHECK_INTERVAL_PUSH) {
             SyncManager.serviceRequestImmediate(mailboxId);
         } else {
             SyncManager.startManualSync(mailboxId, SYNC_KICK, null);
         }
     }
 
     static public void accountUpdated(long acctId) {
         if (INSTANCE == null) return;
         synchronized (sSyncToken) {
             for (AbstractSyncService svc : INSTANCE.mServiceMap.values()) {
                 if (svc.mAccount.mId == acctId) {
                     svc.mAccount = Account.restoreAccountWithId(INSTANCE, acctId);
                 }
             }
         }
     }
 
     /**
      * Sent by services indicating that their thread is finished; action depends on the exitStatus
      * of the service.
      *
      * @param svc the service that is finished
      */
     static public void done(AbstractSyncService svc) {
         if (INSTANCE == null) return;
         synchronized(sSyncToken) {
             long mailboxId = svc.mMailboxId;
             HashMap<Long, SyncError> errorMap = INSTANCE.mSyncErrorMap;
             SyncError syncError = errorMap.get(mailboxId);
             INSTANCE.releaseMailbox(mailboxId);
             int exitStatus = svc.mExitStatus;
             switch (exitStatus) {
                 case AbstractSyncService.EXIT_DONE:
                     if (!svc.mPartRequests.isEmpty()) {
                         // TODO Handle this case
                     }
                     errorMap.remove(mailboxId);
                     break;
                 case AbstractSyncService.EXIT_IO_ERROR:
                     if (syncError != null) {
                         syncError.escalate();
                         INSTANCE.log("Mailbox " + mailboxId + " now held for "
                                 + syncError.holdDelay + "s");
                     } else {
                         errorMap.put(mailboxId, INSTANCE.new SyncError(exitStatus, false));
                         INSTANCE.log("Mailbox " + mailboxId + " added to syncErrorMap");
                     }
                     break;
                 case AbstractSyncService.EXIT_LOGIN_FAILURE:
                 case AbstractSyncService.EXIT_EXCEPTION:
                     errorMap.put(mailboxId, INSTANCE.new SyncError(exitStatus, true));
                     break;
             }
             kick("sync completed");
         }
     }
 
     /**
      * Given the status string from a Mailbox, return the type code for the last sync
      * @param status the syncStatus column of a Mailbox
      * @return
      */
     static public int getStatusType(String status) {
         if (status == null) {
             return -1;
         } else {
             return status.charAt(STATUS_TYPE_CHAR) - '0';
         }
     }
 
     /**
      * Given the status string from a Mailbox, return the change count for the last sync
      * The change count is the number of adds + deletes + changes in the last sync
      * @param status the syncStatus column of a Mailbox
      * @return
      */
     static public int getStatusChangeCount(String status) {
         try {
             String s = status.substring(STATUS_CHANGE_COUNT_OFFSET);
             return Integer.parseInt(s);
         } catch (RuntimeException e) {
             return -1;
         }
     }
 
     static public Context getContext() {
         return INSTANCE;
     }
 }
