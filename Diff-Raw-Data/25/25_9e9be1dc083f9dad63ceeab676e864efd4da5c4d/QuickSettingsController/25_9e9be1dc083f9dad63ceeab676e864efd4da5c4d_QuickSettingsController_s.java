 /*
  * Copyright (C) 2012 The Android Open Source Project
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
 
 package com.android.systemui.statusbar.quicksettings;
 
 import java.util.ArrayList;
 import java.util.HashMap;
 
 import android.bluetooth.BluetoothAdapter;
 import android.content.BroadcastReceiver;
 import android.content.ContentResolver;
 import android.content.Context;
 import android.content.Intent;
 import android.content.IntentFilter;
 import android.content.pm.PackageManager;
 import android.database.ContentObserver;
 import android.net.Uri;
 import android.os.Handler;
 import android.provider.Settings;
 import android.util.Log;
 import android.view.LayoutInflater;
 import android.view.ViewGroup;
 
 import com.android.systemui.statusbar.quicksettings.quicktile.AirplaneModeTile;
 import com.android.systemui.statusbar.quicksettings.quicktile.AlarmTile;
 import com.android.systemui.statusbar.quicksettings.quicktile.AutoRotateTile;
 import com.android.systemui.statusbar.quicksettings.quicktile.BatteryTile;
 import com.android.systemui.statusbar.quicksettings.quicktile.BluetoothTile;
 import com.android.systemui.statusbar.quicksettings.quicktile.BrightnessTile;
 import com.android.systemui.statusbar.quicksettings.quicktile.TorchTile;
 import com.android.systemui.statusbar.quicksettings.quicktile.GPSTile;
 import com.android.systemui.statusbar.quicksettings.quicktile.MobileNetworkTile;
 import com.android.systemui.statusbar.quicksettings.quicktile.MobileNetworkTypeTile;
 import com.android.systemui.statusbar.quicksettings.quicktile.QuickSettingsTile;
 import com.android.systemui.statusbar.quicksettings.quicktile.RingerModeTile;
 import com.android.systemui.statusbar.quicksettings.quicktile.SyncTile;
 import com.android.systemui.statusbar.quicksettings.quicktile.WiFiDisplayTile;
 import com.android.systemui.statusbar.quicksettings.quicktile.WiFiTile;
 import com.android.systemui.statusbar.quicksettings.quicktile.WifiAPTile;
 
 public class QuickSettingsController {
     private static String TAG = "QuickSettingsController";
 
     // Stores the broadcast receivers and content observers
     // quick tiles register for.
     public HashMap<String, ArrayList<QuickSettingsTile>> mReceiverMap
         = new HashMap<String, ArrayList<QuickSettingsTile>>();
     public HashMap<Uri, ArrayList<QuickSettingsTile>> mObserverMap
         = new HashMap<Uri, ArrayList<QuickSettingsTile>>();
 
     /**
      * START OF DATA MATCHING BLOCK
      *
      * THE FOLLOWING DATA MUST BE KEPT UP-TO-DATE WITH THE DATA IN
      * com.android.settings.cyanogenmod.QuickSettingsUtil IN THE
      * Settings PACKAGE.
      */
     public static final String TILE_BATTERY = "toggleBattery";
     public static final String TILE_WIFI = "toggleWifi";
     public static final String TILE_GPS = "toggleGPS";
     public static final String TILE_BLUETOOTH = "toggleBluetooth";
     public static final String TILE_BRIGHTNESS = "toggleBrightness";
     public static final String TILE_RINGER = "toggleSound";
     public static final String TILE_SYNC = "toggleSync";
     public static final String TILE_WIFIAP = "toggleWifiAp";
     public static final String TILE_MOBILEDATA = "toggleMobileData";
     public static final String TILE_NETWORKMODE = "toggleNetworkMode";
     public static final String TILE_AUTOROTATE = "toggleAutoRotate";
     public static final String TILE_AIRPLANE = "toggleAirplane";
     public static final String TILE_TORCH = "toggleFlashlight";  // Keep old string for compatibility
     public static final String TILE_WIMAX = "toggleWimax";
 
     private static final String TILE_DELIMITER = "|";
     private static final String TILES_DEFAULT = TILE_WIFI
             + TILE_DELIMITER + TILE_MOBILEDATA
             + TILE_DELIMITER + TILE_BATTERY
             + TILE_DELIMITER + TILE_GPS
            + TILE_DELIMITER + TILE_RINGER
             + TILE_DELIMITER + TILE_SYNC
             + TILE_DELIMITER + TILE_NETWORKMODE
             + TILE_DELIMITER + TILE_AUTOROTATE
            + TILE_DELIMITER + TILE_WIFIAP
             + TILE_DELIMITER + TILE_AIRPLANE
             + TILE_DELIMITER + TILE_BLUETOOTH;
     /**
      * END OF DATA MATCHING BLOCK
      */
 
     private final Context mContext;
     private final ViewGroup mContainerView;
     private final Handler mHandler;
     private BroadcastReceiver mReceiver;
     private ContentObserver mObserver;
     private final ArrayList<Integer> mQuickSettings;
 
     // Constants for use in switch statement
     public static final int WIFI_TILE = 0;
     public static final int MOBILE_NETWORK_TILE = 1;
     public static final int AIRPLANE_MODE_TILE = 2;
     public static final int BLUETOOTH_TILE = 3;
     public static final int RINGER_TILE = 4;
     public static final int GPS_TILE = 5;
     public static final int AUTO_ROTATION_TILE = 6;
     public static final int BRIGHTNESS_TILE = 7;
     public static final int MOBILE_NETWORK_TYPE_TILE = 8;
     public static final int BATTERY_TILE = 9;
     public static final int ALARM_TILE = 10;
     public static final int WIFI_DISPLAY_TILE = 11;
     public static final int TORCH_TILE = 12;
     public static final int WIFIAP_TILE = 13;
     public static final int SYNC_TILE = 14;
 
     public QuickSettingsController(Context context, QuickSettingsContainerView container) {
         mContext = context;
         mContainerView = container;
         mHandler = new Handler();
         mQuickSettings = new ArrayList<Integer>();
     }
 
     void loadTiles() {
         // Read the stored list of tiles
         ContentResolver resolver = mContext.getContentResolver();
         String tiles = null; //Settings.System.getString(resolver, Settings.System.QUICK_SETTINGS_TILES);
         if (tiles == null) {
             Log.i(TAG, "Default tiles being loaded");
             tiles = TILES_DEFAULT;
         }
 
         Log.i(TAG, "Tiles list: " + tiles);
 
         // Clear the list
         mQuickSettings.clear();
 
         // Split out the tile names and add to the list
         for (String tile : tiles.split("\\|")) {
             if (tile.equals(TILE_BATTERY)) {
                 mQuickSettings.add(BATTERY_TILE);
             } else if (tile.equals(TILE_WIFI)) {
                 mQuickSettings.add(WIFI_TILE);
             } else if (tile.equals(TILE_GPS)) {
                 mQuickSettings.add(GPS_TILE);
             } else if (tile.equals(TILE_BLUETOOTH)) {
                 if(deviceSupportsBluetooth()) {
                     mQuickSettings.add(BLUETOOTH_TILE);
                 }
             } else if (tile.equals(TILE_BRIGHTNESS)) {
                 mQuickSettings.add(BRIGHTNESS_TILE);
             } else if (tile.equals(TILE_RINGER)) {
                 mQuickSettings.add(RINGER_TILE);
             } else if (tile.equals(TILE_SYNC)) {
                 mQuickSettings.add(SYNC_TILE);
             } else if (tile.equals(TILE_WIFIAP)) {
                 if(deviceSupportsTelephony()) {
                     mQuickSettings.add(WIFIAP_TILE);
                 }
             } else if (tile.equals(TILE_MOBILEDATA)) {
                 if(deviceSupportsTelephony()) {
                     mQuickSettings.add(MOBILE_NETWORK_TILE);
                 }
             } else if (tile.equals(TILE_NETWORKMODE)) {
                 if(deviceSupportsTelephony()) {
                     mQuickSettings.add(MOBILE_NETWORK_TYPE_TILE);
                 }
             } else if (tile.equals(TILE_AUTOROTATE)) {
                 mQuickSettings.add(AUTO_ROTATION_TILE);
             } else if (tile.equals(TILE_AIRPLANE)) {
                 mQuickSettings.add(AIRPLANE_MODE_TILE);
             } else if (tile.equals(TILE_TORCH)) {
                 mQuickSettings.add(TORCH_TILE);
             } else if (tile.equals(TILE_WIMAX)) {
                 // Not available yet
             }
         }
 
         // Load the dynamic tiles
         // These toggles must be the last ones added to the view, as they will show
         // only when they are needed
         /*if (Settings.System.getInt(resolver, Settings.System.QS_DYNAMIC_ALARM, 1) == 1) {
             mQuickSettings.add(ALARM_TILE);
         }
         if (Settings.System.getInt(resolver, Settings.System.QS_DYNAMIC_WIFI, 1) == 1) {
             mQuickSettings.add(WIFI_DISPLAY_TILE);
         }*/
     }
 
     public void setupQuickSettings() {
         LayoutInflater inflater = LayoutInflater.from(mContext);
         // Clear out old receiver
         if (mReceiver != null) {
             mContext.unregisterReceiver(mReceiver);
         }
         mReceiver = new QSBroadcastReceiver();
         mReceiverMap.clear();
         ContentResolver resolver = mContext.getContentResolver();
         // Clear out old observer
         if (mObserver != null) {
             resolver.unregisterContentObserver(mObserver);
         }
         mObserver = new QuickSettingsObserver(mHandler);
         mObserverMap.clear();
         addQuickSettings(inflater);
         setupBroadcastReceiver();
         setupContentObserver();
     }
 
     void setupContentObserver() {
         ContentResolver resolver = mContext.getContentResolver();
         for (Uri uri : mObserverMap.keySet()) {
             resolver.registerContentObserver(uri, false, mObserver);
         }
     }
 
     private class QuickSettingsObserver extends ContentObserver {
         public QuickSettingsObserver(Handler handler) {
             super(handler);
         }
 
         public void onChange(boolean selfChange, Uri uri) {
             ContentResolver resolver = mContext.getContentResolver();
             for (QuickSettingsTile tile : mObserverMap.get(uri)) {
                 tile.onChangeUri(resolver, uri);
             }
         }
     }
 
     void setupBroadcastReceiver() {
         IntentFilter filter = new IntentFilter();
         for (String action : mReceiverMap.keySet()) {
             filter.addAction(action);
         }
         mContext.registerReceiver(mReceiver, filter);
     }
 
     private void registerInMap(Object item, QuickSettingsTile tile, HashMap map) {
         if (map.keySet().contains(item)) {
             ArrayList list = (ArrayList) map.get(item);
             if (!list.contains(tile)) {
                 list.add(tile);
             }
         } else {
             ArrayList<QuickSettingsTile> list = new ArrayList<QuickSettingsTile>();
             list.add(tile);
             map.put(item, list);
         }
     }
 
     public void registerAction(Object action, QuickSettingsTile tile) {
         registerInMap(action, tile, mReceiverMap);
     }
 
     public void registerObservedContent(Uri uri, QuickSettingsTile tile) {
         registerInMap(uri, tile, mObserverMap);
     }
 
     private class QSBroadcastReceiver extends BroadcastReceiver {
         public void onReceive(Context context, Intent intent) {
             String action = intent.getAction();
             if (action != null) {
                 for (QuickSettingsTile t : mReceiverMap.get(action)) {
                     t.onReceive(context, intent);
                 }
             }
         }
     };
 
     boolean deviceSupportsTelephony() {
         PackageManager pm = mContext.getPackageManager();
         return pm.hasSystemFeature(PackageManager.FEATURE_TELEPHONY);
     }
 
     boolean deviceSupportsBluetooth() {
         return (BluetoothAdapter.getDefaultAdapter() != null);
     }
 
     void addQuickSettings(LayoutInflater inflater){
         // Load the user configured tiles
         loadTiles();
 
         // Now add the actual tiles from the loaded list
         for (Integer entry: mQuickSettings) {
             QuickSettingsTile qs = null;
             switch (entry) {
             case WIFI_TILE:
                 qs = new WiFiTile(mContext, inflater,
                         (QuickSettingsContainerView) mContainerView, this);
                 break;
             case MOBILE_NETWORK_TILE:
                 qs = new MobileNetworkTile(mContext, inflater,
                         (QuickSettingsContainerView) mContainerView, this);
                 break;
             case AIRPLANE_MODE_TILE:
                 qs = new AirplaneModeTile(mContext, inflater,
                         (QuickSettingsContainerView) mContainerView, this);
                 break;
             case BLUETOOTH_TILE:
                 qs = new BluetoothTile(mContext, inflater,
                         (QuickSettingsContainerView) mContainerView, this);
                 break;
             case RINGER_TILE:
                 qs = new RingerModeTile(mContext, inflater,
                         (QuickSettingsContainerView) mContainerView, this);
                 break;
             case GPS_TILE:
                 qs = new GPSTile(mContext, inflater,
                         (QuickSettingsContainerView) mContainerView, this);
                 break;
             case AUTO_ROTATION_TILE:
                 qs = new AutoRotateTile(mContext, inflater,
                         (QuickSettingsContainerView) mContainerView, this, mHandler);
                 break;
             case BRIGHTNESS_TILE:
                 qs = new BrightnessTile(mContext, inflater,
                         (QuickSettingsContainerView) mContainerView, this, mHandler);
                 break;
             case MOBILE_NETWORK_TYPE_TILE:
                 qs = new MobileNetworkTypeTile(mContext, inflater,
                         (QuickSettingsContainerView) mContainerView, this);
                 break;
             case ALARM_TILE:
                 qs = new AlarmTile(mContext, inflater,
                         (QuickSettingsContainerView) mContainerView, this, mHandler);
                 break;
             case WIFI_DISPLAY_TILE:
                 qs = new WiFiDisplayTile(mContext, inflater,
                         (QuickSettingsContainerView) mContainerView, this);
                 break;
             case BATTERY_TILE:
                 qs = new BatteryTile(mContext, inflater,
                         (QuickSettingsContainerView) mContainerView, this);
                 break;
             case TORCH_TILE:
                 qs = new TorchTile(mContext, inflater,
                         (QuickSettingsContainerView) mContainerView, this, mHandler);
                 break;
             case WIFIAP_TILE:
                 qs = new WifiAPTile(mContext, inflater,
                         (QuickSettingsContainerView) mContainerView, this);
                 break;
             case SYNC_TILE:
                 qs = new SyncTile(mContext, inflater,
                         (QuickSettingsContainerView) mContainerView, this);
                 break;
             }
             if (qs != null) {
                 qs.setupQuickSettingsTile();
             }
         }
     }
 
     public void updateResources() {}
 }
