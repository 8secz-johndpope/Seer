 /*
  * Copyright (C) 2009 The Android Open Source Project
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
 
 package com.android.settings.bluetooth;
 
 import com.android.settings.R;
 import com.android.settings.bluetooth.LocalBluetoothProfileManager.Profile;
 
 import android.app.AlertDialog;
 import android.app.Notification;
 import android.app.PendingIntent;
 import android.app.Service;
 import android.bluetooth.BluetoothAdapter;
 import android.bluetooth.BluetoothDevice;
 import android.content.BroadcastReceiver;
 import android.content.Context;
 import android.content.DialogInterface;
 import android.content.Intent;
 import android.content.IntentFilter;
 import android.os.Handler;
 import android.os.HandlerThread;
 import android.os.IBinder;
 import android.os.Looper;
 import android.os.Message;
 import android.util.Log;
 import android.view.LayoutInflater;
 import android.view.View;
 import android.view.WindowManager;
 import android.widget.CheckBox;
 import android.widget.CompoundButton;
 
 public class DockService extends Service implements AlertDialog.OnMultiChoiceClickListener,
         DialogInterface.OnClickListener, DialogInterface.OnDismissListener,
         CompoundButton.OnCheckedChangeListener {
 
     private static final String TAG = "DockService";
 
     // TODO clean up logs. Disable DEBUG flag for this file and receiver's too
     private static final boolean DEBUG = false;
 
     // Time allowed for the device to be undocked and redocked without severing
     // the bluetooth connection
     private static final long UNDOCKED_GRACE_PERIOD = 1000;
 
     // Msg for user wanting the UI to setup the dock
     private static final int MSG_TYPE_SHOW_UI = 111;
 
     // Msg for device docked event
     private static final int MSG_TYPE_DOCKED = 222;
 
     // Msg for device undocked event
     private static final int MSG_TYPE_UNDOCKED_TEMPORARY = 333;
 
     // Msg for undocked command to be process after UNDOCKED_GRACE_PERIOD millis
     // since MSG_TYPE_UNDOCKED_TEMPORARY
     private static final int MSG_TYPE_UNDOCKED_PERMANENT = 444;
 
     // Created in OnCreate()
     private volatile Looper mServiceLooper;
     private volatile ServiceHandler mServiceHandler;
     private DockService mContext;
     private LocalBluetoothManager mBtManager;
 
     // Normally set after getting a docked event and unset when the connection
     // is severed. One exception is that mDevice could be null if the service
     // was started after the docked event.
     private BluetoothDevice mDevice;
 
     // Created and used for the duration of the dialog
     private AlertDialog mDialog;
     private Profile[] mProfiles;
     private boolean[] mCheckedItems;
     private int mStartIdAssociatedWithDialog;
 
     // Set while BT is being enabled.
     private BluetoothDevice mPendingDevice;
     private int mPendingStartId;
 
     private boolean mRegistered;
     private Object mBtSynchroObject = new Object();
 
     @Override
     public void onCreate() {
         if (DEBUG) Log.d(TAG, "onCreate");
 
         mBtManager = LocalBluetoothManager.getInstance(this);
         mContext = this;
 
         HandlerThread thread = new HandlerThread("DockService");
         thread.start();
 
         mServiceLooper = thread.getLooper();
         mServiceHandler = new ServiceHandler(mServiceLooper);
     }
 
     @Override
     public void onDestroy() {
         if (DEBUG) Log.d(TAG, "onDestroy");
         if (mDialog != null) {
             mDialog.dismiss();
             mDialog = null;
         }
         if (mRegistered) {
             unregisterReceiver(mReceiver);
             mRegistered = false;
         }
         mServiceLooper.quit();
     }
 
     @Override
     public IBinder onBind(Intent intent) {
         // not supported
         return null;
     }
 
     @Override
     public int onStartCommand(Intent intent, int flags, int startId) {
         if (DEBUG) Log.d(TAG, "onStartCommand startId:" + startId + " flags: " + flags);
 
         if (intent == null) {
             // Nothing to process, stop.
             if (DEBUG) Log.d(TAG, "START_NOT_STICKY - intent is null.");
 
             // NOTE: We MUST not call stopSelf() directly, since we need to
             // make sure the wake lock acquired by the Receiver is released.
             DockEventReceiver.finishStartingService(this, startId);
             return START_NOT_STICKY;
         }
 
         Message msg = parseIntent(intent);
         if (msg == null) {
             // Bad intent
             if (DEBUG) Log.d(TAG, "START_NOT_STICKY - Bad intent.");
             DockEventReceiver.finishStartingService(this, startId);
             return START_NOT_STICKY;
         }
 
         msg.arg2 = startId;
         processMessage(msg);
 
         return START_NOT_STICKY;
     }
 
     private final class ServiceHandler extends Handler {
         public ServiceHandler(Looper looper) {
             super(looper);
         }
 
         @Override
         public void handleMessage(Message msg) {
             processMessage(msg);
         }
     }
 
     // This method gets messages from both onStartCommand and mServiceHandler/mServiceLooper
     void processMessage(Message msg) {
         int msgType = msg.what;
         int state = msg.arg1;
         int startId = msg.arg2;
         BluetoothDevice device = (BluetoothDevice) msg.obj;
 
         if(DEBUG) Log.d(TAG, "processMessage: " + msgType + " state: " + state + " device = "
                 + (msg.obj == null ? "null" : device.toString()));
 
         switch (msgType) {
             case MSG_TYPE_SHOW_UI:
                 if (mDialog != null) {
                     // Shouldn't normally happen
                     mDialog.dismiss();
                     mDialog = null;
                 }
                 mDevice = device;
                 createDialog(mContext, mDevice, state, startId);
                 break;
 
             case MSG_TYPE_DOCKED:
                 if (DEBUG) {
                     // TODO figure out why hasMsg always returns false if device
                     // is supplied
                     Log.d(TAG, "1 Has undock perm msg = "
                             + mServiceHandler.hasMessages(MSG_TYPE_UNDOCKED_PERMANENT, mDevice));
                     Log.d(TAG, "2 Has undock perm msg = "
                             + mServiceHandler.hasMessages(MSG_TYPE_UNDOCKED_PERMANENT, device));
                 }
 
                 mServiceHandler.removeMessages(MSG_TYPE_UNDOCKED_PERMANENT);
 
                 if (!device.equals(mDevice)) {
                     if (mDevice != null) {
                         // Not expected. Cleanup/undock existing
                         handleUndocked(mContext, mBtManager, mDevice);
                     }
 
                     mDevice = device;
                     if (mBtManager.getDockAutoConnectSetting(device.getAddress())) {
                         // Setting == auto connect
                         initBtSettings(mContext, device, state, false);
                         applyBtSettings(mDevice, startId);
                     } else {
                         createDialog(mContext, mDevice, state, startId);
                     }
                 }
                 break;
 
             case MSG_TYPE_UNDOCKED_PERMANENT:
                 // Grace period passed. Disconnect.
                 handleUndocked(mContext, mBtManager, device);
                 break;
 
             case MSG_TYPE_UNDOCKED_TEMPORARY:
                 // Undocked event received. Queue a delayed msg to sever connection
                 Message newMsg = mServiceHandler.obtainMessage(MSG_TYPE_UNDOCKED_PERMANENT, state,
                         startId, device);
                 mServiceHandler.sendMessageDelayed(newMsg, UNDOCKED_GRACE_PERIOD);
                 break;
         }
 
         if (mDialog == null && mPendingDevice == null && msgType != MSG_TYPE_UNDOCKED_TEMPORARY) {
             // NOTE: We MUST not call stopSelf() directly, since we need to
             // make sure the wake lock acquired by the Receiver is released.
             DockEventReceiver.finishStartingService(DockService.this, startId);
         }
     }
 
     private Message parseIntent(Intent intent) {
         BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
         int state = intent.getIntExtra(Intent.EXTRA_DOCK_STATE, -1234);
 
         if (DEBUG) {
             Log.d(TAG, "Action: " + intent.getAction() + " State:" + state
                     + " Device: " + (device == null ? "null" : device.getName()));
         }
 
         if (device == null) {
             Log.w(TAG, "device is null");
             return null;
         }
 
         int msgType;
         switch (state) {
             case Intent.EXTRA_DOCK_STATE_UNDOCKED:
                 msgType = MSG_TYPE_UNDOCKED_TEMPORARY;
                 break;
             case Intent.EXTRA_DOCK_STATE_DESK:
             case Intent.EXTRA_DOCK_STATE_CAR:
                 if (DockEventReceiver.ACTION_DOCK_SHOW_UI.equals(intent.getAction())) {
                     msgType = MSG_TYPE_SHOW_UI;
                 } else {
                     msgType = MSG_TYPE_DOCKED;
                 }
                 break;
             default:
                 return null;
         }
 
         return mServiceHandler.obtainMessage(msgType, state, 0, device);
     }
 
     private boolean createDialog(DockService service, BluetoothDevice device, int state,
             int startId) {
         switch (state) {
             case Intent.EXTRA_DOCK_STATE_CAR:
             case Intent.EXTRA_DOCK_STATE_DESK:
                 break;
             default:
                 return false;
         }
 
         startForeground(0, new Notification());
 
         // Device in a new dock.
         boolean firstTime = !mBtManager.hasDockAutoConnectSetting(device.getAddress());
 
         CharSequence[] items = initBtSettings(service, device, state, firstTime);
 
         final AlertDialog.Builder ab = new AlertDialog.Builder(service);
         ab.setTitle(service.getString(R.string.bluetooth_dock_settings_title));
 
         // Profiles
         ab.setMultiChoiceItems(items, mCheckedItems, service);
 
         // Remember this settings
         LayoutInflater inflater = (LayoutInflater) service
                 .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
         float pixelScaleFactor = service.getResources().getDisplayMetrics().density;
         View view = inflater.inflate(R.layout.remember_dock_setting, null);
         CheckBox rememberCheckbox = (CheckBox) view.findViewById(R.id.remember);
 
         // check "Remember setting" by default if no value was saved
         boolean checked = firstTime || mBtManager.getDockAutoConnectSetting(device.getAddress());
         rememberCheckbox.setChecked(checked);
         rememberCheckbox.setOnCheckedChangeListener(this);
         int viewSpacingLeft = (int) (14 * pixelScaleFactor);
         int viewSpacingRight = (int) (14 * pixelScaleFactor);
         ab.setView(view, viewSpacingLeft, 0 /* top */, viewSpacingRight, 0 /* bottom */);
         if (DEBUG) {
             Log.d(TAG, "Auto connect = "
                     + mBtManager.getDockAutoConnectSetting(device.getAddress()));
         }
 
         // Ok Button
         ab.setPositiveButton(service.getString(android.R.string.ok), service);
 
         mStartIdAssociatedWithDialog = startId;
         mDialog = ab.create();
         mDialog.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_DIALOG);
         mDialog.setOnDismissListener(service);
         mDialog.show();
         return true;
     }
 
     // Called when the individual bt profiles are clicked.
     public void onClick(DialogInterface dialog, int which, boolean isChecked) {
         if (DEBUG) Log.d(TAG, "Item " + which + " changed to " + isChecked);
         mCheckedItems[which] = isChecked;
     }
 
     // Called when the "Remember" Checkbox is clicked
     public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
         if (DEBUG) Log.d(TAG, "onCheckedChanged: Remember Settings = " + isChecked);
        mBtManager.saveDockAutoConnectSetting(mDevice.getAddress(), isChecked);
     }
 
     // Called when the dialog is dismissed
     public void onDismiss(DialogInterface dialog) {
         // NOTE: We MUST not call stopSelf() directly, since we need to
         // make sure the wake lock acquired by the Receiver is released.
         if (mPendingDevice == null) {
             DockEventReceiver.finishStartingService(mContext, mStartIdAssociatedWithDialog);
         }
         mContext.stopForeground(true);
     }
 
     // Called when clicked on the OK button
     public void onClick(DialogInterface dialog, int which) {
        if (which == DialogInterface.BUTTON_POSITIVE) {
             if (!mBtManager.hasDockAutoConnectSetting(mDevice.getAddress())) {
                 mBtManager.saveDockAutoConnectSetting(mDevice.getAddress(), true);
             }
 
             applyBtSettings(mDevice, mStartIdAssociatedWithDialog);
         }
     }
 
     private CharSequence[] initBtSettings(DockService service, BluetoothDevice device, int state,
             boolean firstTime) {
         // TODO Avoid hardcoding dock and profiles. Read from system properties
         int numOfProfiles = 0;
         switch (state) {
             case Intent.EXTRA_DOCK_STATE_DESK:
                 numOfProfiles = 1;
                 break;
             case Intent.EXTRA_DOCK_STATE_CAR:
                 numOfProfiles = 2;
                 break;
             default:
                 return null;
         }
 
         mProfiles = new Profile[numOfProfiles];
         mCheckedItems = new boolean[numOfProfiles];
         CharSequence[] items = new CharSequence[numOfProfiles];
 
         int i = 0;
         switch (state) {
             case Intent.EXTRA_DOCK_STATE_CAR:
                 items[i] = service.getString(R.string.bluetooth_dock_settings_headset);
                 mProfiles[i] = Profile.HEADSET;
                 if (firstTime) {
                     mCheckedItems[i] = true;
                 } else {
                     mCheckedItems[i] = LocalBluetoothProfileManager.getProfileManager(mBtManager,
                             Profile.HEADSET).isPreferred(device);
                 }
                 ++i;
                 // fall through
             case Intent.EXTRA_DOCK_STATE_DESK:
                 items[i] = service.getString(R.string.bluetooth_dock_settings_a2dp);
                 mProfiles[i] = Profile.A2DP;
                 if (firstTime) {
                     mCheckedItems[i] = true;
                 } else {
                     mCheckedItems[i] = LocalBluetoothProfileManager.getProfileManager(mBtManager,
                             Profile.A2DP).isPreferred(device);
                 }
                 break;
         }
         return items;
     }
 
     private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
         @Override
         public void onReceive(Context context, Intent intent) {
             int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
             if (state == BluetoothAdapter.STATE_ON && mPendingDevice != null) {
                 synchronized (mBtSynchroObject) {
                     if (mPendingDevice.equals(mDevice)) {
                         if(DEBUG) Log.d(TAG, "applying settings");
                         applyBtSettings(mPendingDevice, mPendingStartId);
                     } else if(DEBUG) {
                         Log.d(TAG, "mPendingDevice  (" + mPendingDevice + ") != mDevice ("
                                 + mDevice + ")");
                     }
 
                     mPendingDevice = null;
                     DockEventReceiver.finishStartingService(mContext, mPendingStartId);
                 }
             }
         }
     };
 
     private void applyBtSettings(final BluetoothDevice device, int startId) {
         if (device == null || mProfiles == null || mCheckedItems == null)
             return;
 
         // Turn on BT if something is enabled
         synchronized (mBtSynchroObject) {
             for (boolean enable : mCheckedItems) {
                 if (enable) {
                     int btState = mBtManager.getBluetoothState();
                     switch (btState) {
                         case BluetoothAdapter.STATE_OFF:
                         case BluetoothAdapter.STATE_TURNING_OFF:
                         case BluetoothAdapter.STATE_TURNING_ON:
                             if (mPendingDevice != null && mPendingDevice.equals(mDevice)) {
                                 return;
                             }
                             if (!mRegistered) {
                                 registerReceiver(mReceiver, new IntentFilter(
                                         BluetoothAdapter.ACTION_STATE_CHANGED));
                             }
                             mPendingDevice = device;
                             mRegistered = true;
                             mPendingStartId = startId;
                             if (btState != BluetoothAdapter.STATE_TURNING_ON) {
                                 // BT is off. Enable it
                                 mBtManager.getBluetoothAdapter().enable();
                             }
                             return;
                     }
                 }
             }
         }
 
         mPendingDevice = null;
 
         for (int i = 0; i < mProfiles.length; i++) {
             LocalBluetoothProfileManager profileManager = LocalBluetoothProfileManager
                     .getProfileManager(mBtManager, mProfiles[i]);
             boolean isConnected = profileManager.isConnected(device);
             CachedBluetoothDevice cachedDevice = getCachedBluetoothDevice(mContext, mBtManager,
                     device);
 
             if (DEBUG) Log.d(TAG, mProfiles[i].toString() + " = " + mCheckedItems[i]);
 
             if (mCheckedItems[i] && !isConnected) {
                 // Checked but not connected
                 if (DEBUG) Log.d(TAG, "applyBtSettings - Connecting");
                 cachedDevice.connect(mProfiles[i]);
             } else if (!mCheckedItems[i] && isConnected) {
                 // Unchecked but connected
                 if (DEBUG) Log.d(TAG, "applyBtSettings - Disconnecting");
                 cachedDevice.disconnect(mProfiles[i]);
             }
             profileManager.setPreferred(device, mCheckedItems[i]);
             if (DEBUG) {
                 if (mCheckedItems[i] != profileManager.isPreferred(device)) {
                     Log.e(TAG, "Can't save prefered value");
                 }
             }
         }
     }
 
     void handleUndocked(Context context, LocalBluetoothManager localManager,
             BluetoothDevice device) {
         if (mDialog != null) {
             mDialog.dismiss();
             mDialog = null;
         }
         mDevice = null;
         mPendingDevice = null;
         CachedBluetoothDevice cachedBluetoothDevice = getCachedBluetoothDevice(context,
                 localManager, device);
         cachedBluetoothDevice.disconnect();
     }
 
     private static CachedBluetoothDevice getCachedBluetoothDevice(Context context,
             LocalBluetoothManager localManager, BluetoothDevice device) {
         CachedBluetoothDeviceManager cachedDeviceManager = localManager.getCachedDeviceManager();
         CachedBluetoothDevice cachedBluetoothDevice = cachedDeviceManager.findDevice(device);
         if (cachedBluetoothDevice == null) {
             cachedBluetoothDevice = new CachedBluetoothDevice(context, device);
         }
         return cachedBluetoothDevice;
     }
 
     // TODO Delete this method if not needed.
     private Notification getNotification(Service service) {
         CharSequence title = service.getString(R.string.dock_settings_title);
 
         Notification n = new Notification(R.drawable.ic_bt_headphones_a2dp, title, System
                 .currentTimeMillis());
 
         CharSequence contentText = service.getString(R.string.dock_settings_summary);
         Intent notificationIntent = new Intent(service, DockEventReceiver.class);
         notificationIntent.setAction(DockEventReceiver.ACTION_DOCK_SHOW_UI);
         PendingIntent pendingIntent = PendingIntent.getActivity(service, 0, notificationIntent, 0);
 
         n.setLatestEventInfo(service, title, contentText, pendingIntent);
         return n;
     }
 }
