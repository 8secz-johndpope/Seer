 package com.mridang.wifiinfo;
 
 import android.content.BroadcastReceiver;
 import android.content.Context;
 import android.content.Intent;
 import android.content.IntentFilter;
 import android.net.ConnectivityManager;
 import android.net.wifi.WifiInfo;
 import android.net.wifi.WifiManager;
 import android.provider.Settings;
 import android.util.Log;
 
 import com.bugsense.trace.BugSenseHandler;
 import com.google.android.apps.dashclock.api.DashClockExtension;
 import com.google.android.apps.dashclock.api.ExtensionData;
 
 /*
  * This class is the main class that provides the widget
  */
 public class WifiinfoWidget extends DashClockExtension {
 
 	/* This is the instance of the receiver that deals with cellular status */
 	private ToggleReceiver objReceiver;
 
 	/*
 	 * This class is the receiver for getting hotspot toggle events
 	 */
 	private class ToggleReceiver extends BroadcastReceiver {
 
 		/*
 		 * @see android.content.BroadcastReceiver#onReceive(android.content.Context, android.content.Intent)
 		 */
 		@Override
 		public void onReceive(Context context, Intent intent) {
 
 			onUpdateData(0);
 
 		}
 
 	}
 
 	/*
 	 * @see com.google.android.apps.dashclock.api.DashClockExtension#onInitialize(boolean)
 	 */
 	@Override
 	protected void onInitialize(boolean booReconnect) {
 
 		super.onInitialize(booReconnect);
 
 		if (objReceiver != null) {
 
 			try {
 
 				Log.d("WifiinfoWidget", "Unregistered any existing status receivers");
 				unregisterReceiver(objReceiver);
 
 			} catch (Exception e) {
 				e.printStackTrace();
 			}
 
 		}
 
 		objReceiver = new ToggleReceiver();
 		registerReceiver(objReceiver, new IntentFilter(Intent.ACTION_AIRPLANE_MODE_CHANGED));
 		registerReceiver(objReceiver, new IntentFilter(WifiManager.WIFI_STATE_CHANGED_ACTION));
 		registerReceiver(objReceiver, new IntentFilter(WifiManager.SUPPLICANT_STATE_CHANGED_ACTION));
 		Log.d("WifiinfoWidget", "Registered the status receiver");
 
 	}
 
 	/*
      * @see com.google.android.apps.dashclock.api.DashClockExtension#onCreate()
      */
     public void onCreate() {
 
         super.onCreate();
         Log.d("WifiinfoWidget", "Created");
         BugSenseHandler.initAndStartSession(this, "29314c07");
 
     }
 
     /*
      * @see
      * com.google.android.apps.dashclock.api.DashClockExtension#onUpdateData
      * (int)
      */
     @Override
     protected void onUpdateData(int arg0) {
 
         setUpdateWhenScreenOn(true);
 
         Log.d("WifiinfoWidget", "Fetching wireless network information");
         ExtensionData edtInformation = new ExtensionData();
         edtInformation.visible(false);
 
         try {
 
             Log.d("WifiinfoWidget", "Checking if the airplane mode is on");
             if (Settings.System.getInt(getContentResolver(), Settings.Global.AIRPLANE_MODE_ON, 0) == 0) {
 
                 Log.d("WifiinfoWidget", "Airplane-mode is off");
                 ConnectivityManager cmrConnectivity = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
 
                 Log.d("WifiinfoWidget", "Checking if connected to a wifi network");
                if (cmrConnectivity != null && cmrConnectivity.getNetworkInfo(ConnectivityManager.TYPE_WIFI).isConnected()) {
 
                     Log.d("WifiinfoWidget", "Connected to a wireless network");
                     WifiManager wifManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
                     WifiInfo wifInfo = wifManager.getConnectionInfo();
 
                     if (wifInfo != null && !wifInfo.getSSID().trim().isEmpty() ) {
 
                         edtInformation.visible(true);
                         edtInformation.status(wifInfo.getSSID().replaceAll("^\"|\"$", ""));
                         edtInformation.expandedBody(wifInfo.getLinkSpeed() + WifiInfo.LINK_SPEED_UNITS);
                         edtInformation.clickIntent(new Intent(Settings.ACTION_WIFI_SETTINGS));
 
                     }
 
                 } else {
                     Log.d("WifiinfoWidget", "Not connected to a wireless network");
                 }
 
             } else {
                 Log.d("WifiinfoWidget", "Airplane-mode is on");
             }
 
         } catch (Exception e) {
             Log.e("WifiinfoWidget", "Encountered an error", e);
             BugSenseHandler.sendException(e);
         }
 
         edtInformation.icon(R.drawable.ic_dashclock);
         publishUpdate(edtInformation);
         Log.d("WifiinfoWidget", "Done");
 
     }
 
     /*
      * @see com.google.android.apps.dashclock.api.DashClockExtension#onDestroy()
      */
     public void onDestroy() {
 
         super.onDestroy();
         Log.d("WifiinfoWidget", "Destroyed");
         BugSenseHandler.closeSession(this);
 
     }
 
 }
