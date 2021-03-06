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
 package com.android.contacts.vcard;
 
 import android.accounts.Account;
 import android.app.Notification;
 import android.app.NotificationManager;
 import android.app.PendingIntent;
 import android.app.Service;
 import android.content.ComponentName;
 import android.content.ContentResolver;
 import android.content.ContentUris;
 import android.content.Context;
 import android.content.Intent;
 import android.content.ServiceConnection;
 import android.net.Uri;
 import android.os.IBinder;
 import android.os.Message;
 import android.os.Messenger;
 import android.os.RemoteException;
 import android.provider.ContactsContract.RawContacts;
 import android.util.Log;
 
 import com.android.contacts.R;
 import com.android.vcard.VCardEntryCommitter;
 import com.android.vcard.VCardEntryConstructor;
 import com.android.vcard.VCardInterpreter;
 import com.android.vcard.VCardParser;
 import com.android.vcard.VCardParser_V21;
 import com.android.vcard.VCardParser_V30;
 import com.android.vcard.exception.VCardException;
 import com.android.vcard.exception.VCardNestedException;
 import com.android.vcard.exception.VCardNotSupportedException;
 import com.android.vcard.exception.VCardVersionException;
 
 import java.io.IOException;
 import java.io.InputStream;
 import java.util.ArrayList;
 import java.util.LinkedList;
 import java.util.List;
 import java.util.Queue;
 
 /**
  * Class for processing incoming import request from {@link ImportVCardActivity}.
  *
  * This class is designed so that a user ({@link Service}) does not need to (and should not)
  * recreate multiple instances, as this holds total count of vCard entries to be imported.
  */
 public class ImportProcessor {
     private static final String LOG_TAG = ImportProcessor.class.getSimpleName();
 
     private class ImportProcessorConnection implements ServiceConnection {
         private Messenger mMessenger;
         private boolean mBound;
 
         public synchronized void tryBind() {
             if (!mContext.bindService(new Intent(mContext, VCardService.class),
                     mConnection, Context.BIND_AUTO_CREATE)) {
                 throw new RuntimeException("Failed to bind to VCardService.");
             }
             mBound = true;
         }
 
         public synchronized void tryUnbind() {
             if (mBound) {
                 mContext.unbindService(mConnection);
             } else {
                 // TODO: Not graceful.
                 Log.w(LOG_TAG, "unbind() is tried while ServiceConnection is not bound yet");
             }
             mBound = false;
         }
 
        public void sendFinishNotification() {
             try {
                 mMessenger.send(Message.obtain(null,
                         VCardService.MSG_NOTIFY_IMPORT_FINISHED,
                         null));
             } catch (RemoteException e) {
                 Log.e(LOG_TAG, "RemoteException is thrown when trying to send request");
             }
         }
 
         @Override
         public void onServiceConnected(ComponentName name, IBinder service) {
             mMessenger = new Messenger(service);
         }
         @Override
         public void onServiceDisconnected(ComponentName name) {
             mMessenger = null;
         }
     }
 
     private final Context mContext;
 
     private final ImportProcessorConnection mConnection = new ImportProcessorConnection();
 
     private ContentResolver mResolver;
     private NotificationManager mNotificationManager;
 
     private final List<Uri> mFailedUris = new ArrayList<Uri>();
     private final List<Uri> mCreatedUris = new ArrayList<Uri>();
     private final ImportProgressNotifier mNotifier = new ImportProgressNotifier();
 
     private VCardParser mVCardParser;
 
     /**
      * Meaning a controller of this object requests the operation should be canceled
      * or not, which implies {@link #mReadyForRequest} should be set to false soon, but
      * it does not meaning cancel request is able to immediately stop this object,
      * so we have two variables.
      */
     private boolean mCanceled;
 
     /**
      * Meaning that this object is able to accept import requests.
      */
     private boolean mReadyForRequest;
     private final Queue<ImportRequest> mPendingRequests =
             new LinkedList<ImportRequest>();
 
     // For testability.
     /* package */ ThreadStarter mThreadStarter = new ThreadStarter() {
         public void start() {
             final Thread thread = new Thread(new Runnable() {
                 public void run() {
                     process();
                 }
             });
             thread.start();
         }
     };
     /* package */ interface CommitterGenerator {
         public VCardEntryCommitter generate(ContentResolver resolver);
     }
     /* package */ class DefaultCommitterGenerator implements CommitterGenerator {
         public VCardEntryCommitter generate(ContentResolver resolver) {
             return new VCardEntryCommitter(mResolver);
         }
     }
 
     private CommitterGenerator mCommitterGenerator =
         new DefaultCommitterGenerator();
 
     public ImportProcessor(final Context context) {
         mContext = context;
 
         mConnection.tryBind();
     }
 
     /**
      * Checks this object and initialize it if not.
      *
      * This method is needed since {@link VCardService} is not ready when this object is
      * created and we need to delay this initialization, while we want to initialize
      * this object soon in tests.
      */
     /* package */ void ensureInit() {
         if (mResolver == null) {
             // Service object may not ready at the construction time
             // (e.g. ContentResolver may be null).
             mResolver = mContext.getContentResolver();
             mNotificationManager =
                     (NotificationManager)mContext.getSystemService(Context.NOTIFICATION_SERVICE);
         }
     }
 
     public synchronized void pushRequest(final ImportRequest request) {
         ensureInit();
 
         final boolean needThreadStart;
         if (!mReadyForRequest) {
             mFailedUris.clear();
             mCreatedUris.clear();
 
             mNotifier.init(mContext, mNotificationManager);
             needThreadStart = true;
         } else {
             needThreadStart = false;
         }
         final int count = request.entryCount;
         if (count > 0) {
             mNotifier.addTotalCount(count);
         }
         mPendingRequests.add(request);
         if (needThreadStart) {
             mThreadStarter.start();
         }
 
         mReadyForRequest = true;
     }
 
     /**
      * Starts processing import requests. Never stops until all given requests are
      * processed or some error happens, assuming this method is called from a
      * {@link Thread} object.
      */
     private void process() {
         if (!mReadyForRequest) {
             mConnection.tryUnbind();
             throw new RuntimeException(
                     "process() is called before request being pushed "
                     + "or after this object's finishing its processing.");
         }
 
         try {
             while (!mCanceled) {
                 final ImportRequest parameter;
                 synchronized (this) {
                     if (mPendingRequests.size() == 0) {
                         mReadyForRequest = false;
                         break;
                     } else {
                         parameter = mPendingRequests.poll();
                     }
                 }  // synchronized (this)
                 handleOneRequest(parameter);
             }
 
             // Currenty we don't have an appropriate way to let users see all URIs imported.
             // Instead, we show one only when there's just one created uri.
             doFinishNotification(mCreatedUris.size() > 0 ? mCreatedUris.get(0) : null);
            mConnection.sendFinishNotification();
         } finally {
             // TODO: verify this works fine.
             mReadyForRequest = false;  // Just in case.
             mNotifier.resetTotalCount();
             mConnection.tryUnbind();
         }
     }
 
     /**
      * Would be run inside synchronized block.
      */
     /* package */ boolean handleOneRequest(final ImportRequest request) {
         if (mCanceled) {
             Log.i(LOG_TAG, "Canceled before actually handling parameter ("
                     + request.uri + ")");
             return false;
         }
         final int[] possibleVCardVersions;
         if (request.vcardVersion == ImportVCardActivity.VCARD_VERSION_AUTO_DETECT) {
             /**
              * Note: this code assumes that a given Uri is able to be opened more than once,
              * which may not be true in certain conditions.
              */
             possibleVCardVersions = new int[] {
                     ImportVCardActivity.VCARD_VERSION_V21,
                     ImportVCardActivity.VCARD_VERSION_V30
             };
         } else {
             possibleVCardVersions = new int[] {
                     request.vcardVersion
             };
         }
 
         final Uri uri = request.uri;
         final Account account = request.account;
         final int estimatedVCardType = request.estimatedVCardType;
         final String estimatedCharset = request.estimatedCharset;
 
         final VCardEntryConstructor constructor =
                 new VCardEntryConstructor(estimatedVCardType, account, estimatedCharset);
         final VCardEntryCommitter committer = mCommitterGenerator.generate(mResolver);
         constructor.addEntryHandler(committer);
         constructor.addEntryHandler(mNotifier);
 
         final boolean successful =
             readOneVCard(uri, estimatedVCardType, estimatedCharset,
                     constructor, possibleVCardVersions);
         if (successful) {
             List<Uri> uris = committer.getCreatedUris();
             if (uris != null) {
                 mCreatedUris.addAll(uris);
             } else {
                 // Not critical, but suspicious.
                 Log.w(LOG_TAG,
                         "Created Uris is null while the creation itself is successful.");
             }
         } else {
             mFailedUris.add(uri);
         }
 
         return successful;
     }
 
     /*
     private void doErrorNotification(int id) {
         final Notification notification = new Notification();
         notification.icon = android.R.drawable.stat_sys_download_done;
         notification.flags |= Notification.FLAG_AUTO_CANCEL;
         final String title = mService.getString(R.string.reading_vcard_failed_title);
         final PendingIntent intent =
                 PendingIntent.getActivity(mService, 0, new Intent(), 0);
         notification.setLatestEventInfo(mService, title, "", intent);
         mNotificationManager.notify(MESSAGE_ID, notification);
     }
     */
 
     private void doFinishNotification(final Uri createdUri) {
         final Notification notification = new Notification();
         final String title;
         notification.flags |= Notification.FLAG_AUTO_CANCEL;
 
         if (isCanceled()) {
             notification.icon = android.R.drawable.stat_notify_error;
             title = mContext.getString(R.string.importing_vcard_canceled_title);
         } else {
             notification.icon = android.R.drawable.stat_sys_download_done;
             title = mContext.getString(R.string.importing_vcard_finished_title);
         }
 
         final Intent intent;
         if (createdUri != null) {
             final long rawContactId = ContentUris.parseId(createdUri);
             final Uri contactUri = RawContacts.getContactLookupUri(
                     mResolver, ContentUris.withAppendedId(
                             RawContacts.CONTENT_URI, rawContactId));
             intent = new Intent(Intent.ACTION_VIEW, contactUri);
         } else {
             intent = null;
         }
 
         notification.setLatestEventInfo(mContext, title, "",
                 PendingIntent.getActivity(mContext, 0, intent, 0));
         mNotificationManager.notify(VCardService.IMPORT_NOTIFICATION_ID, notification);
     }
 
     // Make package private for testing use.
     /* package */ boolean readOneVCard(Uri uri, int vcardType, String charset,
             final VCardInterpreter interpreter,
             final int[] possibleVCardVersions) {
         boolean successful = false;
         final int length = possibleVCardVersions.length;
         for (int i = 0; i < length; i++) {
             InputStream is = null;
             final int vcardVersion = possibleVCardVersions[i];
             try {
                 if (i > 0 && (interpreter instanceof VCardEntryConstructor)) {
                     // Let the object clean up internal temporary objects,
                     ((VCardEntryConstructor) interpreter).clear();
                 }
 
                 is = mResolver.openInputStream(uri);
 
                 // We need synchronized block here,
                 // since we need to handle mCanceled and mVCardParser at once.
                 // In the worst case, a user may call cancel() just before creating
                 // mVCardParser.
                 synchronized (this) {
                     mVCardParser = (vcardVersion == ImportVCardActivity.VCARD_VERSION_V30 ?
                             new VCardParser_V30(vcardType) :
                                 new VCardParser_V21(vcardType));
                     if (mCanceled) {
                         mVCardParser.cancel();
                     }
                 }
                 mVCardParser.parse(is, interpreter);
 
                 successful = true;
                 break;
             } catch (IOException e) {
                 Log.e(LOG_TAG, "IOException was emitted: " + e.getMessage());
             } catch (VCardNestedException e) {
                 // This exception should not be thrown here. We should instead handle it
                 // in the preprocessing session in ImportVCardActivity, as we don't try
                 // to detect the type of given vCard here.
                 //
                 // TODO: Handle this case appropriately, which should mean we have to have
                 // code trying to auto-detect the type of given vCard twice (both in
                 // ImportVCardActivity and ImportVCardService).
                 Log.e(LOG_TAG, "Nested Exception is found.");
             } catch (VCardNotSupportedException e) {
                 Log.e(LOG_TAG, e.getMessage());
             } catch (VCardVersionException e) {
                 if (i == length - 1) {
                     Log.e(LOG_TAG, "Appropriate version for this vCard is not found.");
                 } else {
                     // We'll try the other (v30) version.
                 }
             } catch (VCardException e) {
                 Log.e(LOG_TAG, e.getMessage());
             } finally {
                 if (is != null) {
                     try {
                         is.close();
                     } catch (IOException e) {
                     }
                 }
             }
         }
 
         return successful;
     }
 
     public synchronized boolean isReadyForRequest() {
         return mReadyForRequest;
     }
 
     public boolean isCanceled() {
         return mCanceled;
     }
 
     public void cancel() {
         mCanceled = true;
         synchronized (this) {
             if (mVCardParser != null) {
                 mVCardParser.cancel();
             }
         }
     }
 
     public List<Uri> getCreatedUrisForTest() {
         return mCreatedUris;
     }
 
     public List<Uri> getFailedUrisForTest() {
         return mFailedUris;
     }
 
     public void injectCommitterGeneratorForTest(
             final CommitterGenerator generator) {
         mCommitterGenerator = generator;
     }
 
     public void initNotifierForTest() {
         mNotifier.init(mContext, mNotificationManager);
     }
 }
