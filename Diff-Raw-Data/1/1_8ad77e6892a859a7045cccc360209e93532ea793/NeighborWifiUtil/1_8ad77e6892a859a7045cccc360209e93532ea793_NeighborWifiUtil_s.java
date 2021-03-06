 package com.android.utils;
 
 import java.util.List;
 
 import android.content.BroadcastReceiver;
 import android.content.Context;
 import android.content.Intent;
 import android.content.IntentFilter;
 import android.net.wifi.ScanResult;
 import android.net.wifi.WifiManager;
 
 import com.android.Values;
 import com.android.listeners.ResponseListener;
 
 public class NeighborWifiUtil {
 	
 
 	private static Context context;
 	public static ResponseListener responseListener;
     public static WifiManager mainWifi;
     public static WifiReceiver receiverWifi;
     public static List<ScanResult> wifiList;
 	public static NeighborResult neighborResult;
 	public boolean isRunning = true;
 	
 	public void getNeighborWifi(Context ct, NeighborResult nr) {
 		context = ct;
 		neighborResult = nr;
 		isRunning = true;
 		mainWifi = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
 		receiverWifi = new WifiReceiver();
 		context.registerReceiver(receiverWifi, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
 		mainWifi.startScan();
 		
 		
 	}
 	
 
     class WifiReceiver extends BroadcastReceiver {
         public void onReceive(Context c, Intent intent) {
         	wifiList = mainWifi.getScanResults();
         	neighborResult.gotNeighbor(wifiList);
         	isRunning=false;
         	try{
         		context.unregisterReceiver(receiverWifi);
         	}
         	catch(Exception e){
         		
         	}
         }
     }
     
     public static abstract class NeighborResult {
         public abstract void gotNeighbor(List<ScanResult> wifiList);
           
     }
 }
