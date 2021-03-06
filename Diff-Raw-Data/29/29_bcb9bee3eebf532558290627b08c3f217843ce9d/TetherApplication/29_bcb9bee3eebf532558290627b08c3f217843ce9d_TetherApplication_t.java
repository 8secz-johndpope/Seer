 /**
  *  This program is free software; you can redistribute it and/or modify it under 
  *  the terms of the GNU General Public License as published by the Free Software 
  *  Foundation; either version 3 of the License, or (at your option) any later 
  *  version.
  *  You should have received a copy of the GNU General Public License along with 
  *  this program; if not, see <http://www.gnu.org/licenses/>. 
  *  Use this application at your own risk.
  *
  *  Copyright (c) 2009 by Harald Mueller and Seth Lemons.
  */
 
 package android.tether;
 
 import java.io.File;
 import java.io.FileOutputStream;
 import java.io.IOException;
 import java.io.InputStream;
 import java.io.OutputStream;
 import java.lang.reflect.InvocationTargetException;
 import java.lang.reflect.Method;
 import java.util.ArrayList;
 import java.util.Date;
 import java.util.Enumeration;
 import java.util.Hashtable;
 import java.util.Properties;
 
 import android.app.Application;
 import android.app.Notification;
 import android.app.NotificationManager;
 import android.app.PendingIntent;
 import android.bluetooth.BluetoothAdapter;
 import android.content.Context;
 import android.content.Intent;
 import android.content.SharedPreferences;
 import android.content.pm.PackageInfo;
 import android.net.Uri;
 import android.net.wifi.WifiManager;
 import android.os.Handler;
 import android.os.Looper;
 import android.os.Message;
 import android.os.PowerManager;
 import android.preference.PreferenceManager;
 import android.tether.data.ClientData;
 import android.tether.system.CoreTask;
 import android.tether.system.NativeTask;
 import android.tether.system.WebserviceTask;
 import android.util.Log;
 import android.widget.Toast;
 
 public class TetherApplication extends Application {
 
 	public static final String MSG_TAG = "TETHER -> TetherApplication";
 	
 	// StartUp-Check perfomed
 	public boolean startupCheckPerformed = false;
 	
 	// Client-Connect-Thread
 	private Thread clientConnectThread = null;
 	private static final int CLIENT_CONNECT_ACDISABLED = 0;
 	private static final int CLIENT_CONNECT_AUTHORIZED = 1;
 	private static final int CLIENT_CONNECT_NOTAUTHORIZED = 2;
 	
 	// Data counters
 	private Thread trafficCounterThread = null;
 
 	// WifiManager
 	private WifiManager wifiManager;
 	public String tetherNetworkDevice = "";
 	
 	// PowerManagement
 	private PowerManager powerManager = null;
 	private PowerManager.WakeLock wakeLock = null;
 
 	// ConnectivityManager
 	//private ConnectivityManager connectivityManager;	
 	
 	// DNS-Server-Update Thread
 	private Thread dnsUpdateThread = null;	
 	
 	// Preferences
 	public SharedPreferences settings = null;
 	public SharedPreferences.Editor preferenceEditor = null;
 	
     // Notification
 	public NotificationManager notificationManager;
 	private Notification notification;
 	private int clientNotificationCount = 0;
 	
 	// Intents
 	private PendingIntent mainIntent;
 	private PendingIntent accessControlIntent;
     
 	// Original States
 	private static boolean origWifiState = false;
 	private static boolean origBluetoothState = false;
 	public static boolean origTickleState = false;
 	public static boolean origBackState = false;	
 	
 	// Client
 	ArrayList<ClientData> clientDataAddList = new ArrayList<ClientData>();
 	ArrayList<String> clientMacRemoveList = new ArrayList<String>();
 	
 	// Whitelist
 	public CoreTask.Whitelist whitelist = null;
 	// Supplicant
 	public CoreTask.WpaSupplicant wpasupplicant = null;
 	// TiWlan.conf
 	public CoreTask.TiWlanConf tiwlan = null;
 	// tether.cfg
 	public CoreTask.TetherConfig tethercfg = null;
 	
 	// CoreTask
 	public CoreTask coretask = null;
 	
 	// WebserviceTask
 	public WebserviceTask webserviceTask = null;
 	
 	// Update Url
 	private static final String APPLICATION_PROPERTIES_URL = "http://android-wifi-tether.googlecode.com/svn/download/update/google/nexusone/application.properties";
 	private static final String APPLICATION_DOWNLOAD_URL = "http://android-wifi-tether.googlecode.com/files/";
 	
 	@Override
 	public void onCreate() {
 		Log.d(MSG_TAG, "Calling onCreate()");
 		
 		//create CoreTask
 		this.coretask = new CoreTask();
 		this.coretask.setPath(this.getApplicationContext().getFilesDir().getParent());
 		Log.d(MSG_TAG, "Current directory is "+this.getApplicationContext().getFilesDir().getParent());
 
 		//create WebserviceTask
 		this.webserviceTask = new WebserviceTask();
 		
         // Check Homedir, or create it
         this.checkDirs(); 
         
         // Preferences
 		this.settings = PreferenceManager.getDefaultSharedPreferences(this);
 		
         // preferenceEditor
         this.preferenceEditor = settings.edit();
 		
         // init wifiManager
         wifiManager = (WifiManager) this.getSystemService(Context.WIFI_SERVICE); 
         
         // Whitelist
         this.whitelist = this.coretask.new Whitelist();
         // Supplicant config
         this.wpasupplicant = this.coretask.new WpaSupplicant();
         // tiwlan.conf
         this.tiwlan = this.coretask.new TiWlanConf();
         // tether.cfg
         this.tethercfg = this.coretask.new TetherConfig();
         this.tethercfg.read();
 		
         // Powermanagement
         powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
         wakeLock = powerManager.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK, "TETHER_WAKE_LOCK");
 
         // Connectivitymanager
         //connectivityManager = (ConnectivityManager) this.getSystemService(CONNECTIVITY_SERVICE);        
 		
         // init notificationManager
         this.notificationManager = (NotificationManager) this.getSystemService(Context.NOTIFICATION_SERVICE);
     	this.notification = new Notification(R.drawable.start_notification, "Wireless Tether", System.currentTimeMillis());
     	this.mainIntent = PendingIntent.getActivity(this, 0, new Intent(this, MainActivity.class), 0);
     	this.accessControlIntent = PendingIntent.getActivity(this, 1, new Intent(this, AccessControlActivity.class), 0);
 	}
 
 	@Override
 	public void onTerminate() {
 		Log.d(MSG_TAG, "Calling onTerminate()");
     	// Stopping Tether
 		this.stopTether();
 		// Remove all notifications
 		this.notificationManager.cancelAll();
 	}
 	
 	// ClientDataList Add
 	public synchronized void addClientData(ClientData clientData) {
 		this.clientDataAddList.add(clientData);
 	}
 
 	public synchronized void removeClientMac(String mac) {
 		this.clientMacRemoveList.add(mac);
 	}
 	
 	public synchronized ArrayList<ClientData> getClientDataAddList() {
 		ArrayList<ClientData> tmp = this.clientDataAddList;
 		this.clientDataAddList = new ArrayList<ClientData>();
 		return tmp;
 	}
 	
 	public synchronized ArrayList<String> getClientMacRemoveList() {
 		ArrayList<String> tmp = this.clientMacRemoveList;
 		this.clientMacRemoveList = new ArrayList<String>();
 		return tmp;
 	}	
 	
 	public synchronized void resetClientMacLists() {
 		this.clientDataAddList = new ArrayList<ClientData>();
 		this.clientMacRemoveList = new ArrayList<String>();
 	}
 
 	/*
 	 * Bluetooth API is not exposed publicly, so we need to use reflection
 	 * to query and set the configuration.
 	 */
 	@SuppressWarnings("unchecked")
 	public Object callBluetoothMethod(String methodName) {
     	Object manager = getSystemService("bluetooth");
     	Class c = manager.getClass();
     	Object returnValue = null;
     	if (c == null) {
     		Log.d(MSG_TAG, "Cant get BT manager");
     	} else {
         	try {
 	        	Method enable = c.getMethod(methodName);
 	        	enable.setAccessible(true);
 	        	returnValue = enable.invoke(manager);
 	        } catch (NoSuchMethodException e){
 	        		Log.d(MSG_TAG, "No such method: " + e);
 		    } catch (InvocationTargetException e) {
 		    		Log.d(MSG_TAG, "Invocation target exception: " + e.getTargetException().getMessage());
 		    } catch (IllegalAccessException e) {
 		    		Log.d(MSG_TAG, "Illegal access: " + e);
 		    }
 	    }
     	return returnValue;
 	}
 	
	public boolean setBluetoothState(boolean enabled) {
 		boolean connected = false;

		if (enabled == false) {
			BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
			adapter.disable();
			return false;
		}
 		int checkcounter = 0;
 		
 		Looper.prepare();
 		BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
 		origBluetoothState = adapter.isEnabled();
 		
 		if (origBluetoothState == false) {
 			adapter.enable();
 			while (connected == false && checkcounter <= 60) {
 				// Wait up to 60s for bluetooth to come up.
 				// does not behave unless started after BT is enabled.
 				connected = adapter.isEnabled();
 				if (connected == false) {
 					checkcounter++;
 					try {
 						Thread.sleep(1000);
 					} catch (InterruptedException e) {
 						// Nothing
 					}
 				} else {
 					break;
 				}
 			}
 			if (connected == false) {
 				Log.d(MSG_TAG, "Enable bluetooth failed");
 			}
 		} else {
 			connected = true;
 		}
 		return connected;
 	}

 	
 	// Start/Stop Tethering
     public int startTether() {
     	/*
     	 * ReturnCodes:
     	 *    0 = All OK, Service started
     	 *    2 = Fatal error 
     	 */
         boolean bluetoothPref = this.settings.getBoolean("bluetoothon", false);
         boolean bluetoothWifi = this.settings.getBoolean("bluetoothkeepwifi", false);
         
         this.tethercfg.read();
         this.tethercfg.put("tether.mode", bluetoothPref ? "bt" : "wifi");
         this.tethercfg.write();
         if (bluetoothPref) {
    		if (setBluetoothState(true) == false){
     			return 2;
     		}
 			if (bluetoothWifi == false) {
 	        	this.disableWifi();
 			}
         } 
         else {
         	this.disableWifi();
         }
 
         // Update resolv.conf-file
         String dns[] = this.coretask.updateResolvConf();     
         
     	// Starting service
     	if (this.coretask.runRootCommand(this.coretask.DATA_FILE_PATH+"/bin/tether start 1")) {
         	
         	this.clientConnectEnable(true);
     		this.trafficCounterEnable(true);
     		this.dnsUpdateEnable(dns, true);
         	
 			// Acquire Wakelock
 			this.acquireWakeLock();
 			
     		return 0;
     	}
     	return 2;
     }
     
     public boolean stopTether() {
     	this.releaseWakeLock();
     	this.clientConnectEnable(false);
 
         boolean bluetoothPref = this.settings.getBoolean("bluetoothon", false);
         boolean bluetoothWifi = this.settings.getBoolean("bluetoothkeepwifi", false);
         
     	boolean stopped = this.coretask.runRootCommand(this.coretask.DATA_FILE_PATH+"/bin/tether stop 1");
 		this.notificationManager.cancelAll();
 		
 		// Put WiFi and Bluetooth back, if applicable.
 		if (bluetoothPref && origBluetoothState == false) {
			setBluetoothState(false);
 		}
 		if (bluetoothPref == false || bluetoothWifi == false) {
 			this.enableWifi();
 		}
 		this.trafficCounterEnable(false);
 		this.dnsUpdateEnable(false);
 		return stopped;
     }
 	
     public boolean restartTether() {
     	return this.restartTether(0, 0);
     }
     
     public boolean restartTether(int tetherModeToStop, int tetherModeToStart) {
     	/* TetherModes:
     	 * 		0 =  wifi
     	 * 		1 =  bluetooth
     	 */
     	String command;
     	boolean stopped = false;
     	command = this.coretask.DATA_FILE_PATH+"/bin/tether stop";
     	if (tetherModeToStop == 1) {
     		command += "bt";
     	}
 		stopped = this.coretask.runRootCommand(command);    	
 		this.clientConnectEnable(false);
     	if (stopped != true) {
     		Log.d(MSG_TAG, "Couldn't stop tethering.");
     		return false;
     	}
     	command = this.coretask.DATA_FILE_PATH+"/bin/tether start";
     	if (tetherModeToStart == 1) {
     		command += "bt";
     	}
     	if (this.coretask.runRootCommand(command)) {
     		this.clientConnectEnable(true);
     	}
     	else {
     		Log.d(MSG_TAG, "Couldn't stop tethering.");
     		return false;
     	}
 		// Put WiFi and Bluetooth back, if applicable.
 		if (tetherModeToStop == 1 && tetherModeToStart == 0 && origBluetoothState == false)
 			callBluetoothMethod("disable");
 		if (this.settings.getBoolean("bluetoothkeepwifi", false)) {
 			this.enableWifi();
 		}
     	return true;
     }
     
     // gets user preference on whether wakelock should be disabled during tethering
     public boolean isWakeLockDisabled(){
 		return this.settings.getBoolean("wakelockpref", false);
 	} 
 	
     // gets user preference on whether sync should be disabled during tethering
     public boolean isSyncDisabled(){
 		return this.settings.getBoolean("syncpref", false);
 	}
     
     // gets user preference on whether sync should be disabled during tethering
     public boolean isUpdatecDisabled(){
 		return this.settings.getBoolean("updatepref", false);
 	}
     
     // get preferences on whether donate-dialog should be displayed
     public boolean showDonationDialog() {
     	return this.settings.getBoolean("donatepref", true);
     }
 
     // Wifi
     public void disableWifi() {
     	if (this.wifiManager.isWifiEnabled()) {
     		origWifiState = true;
     		this.wifiManager.setWifiEnabled(false);
     		Log.d(MSG_TAG, "Wifi disabled!");
         	// Waiting for interface-shutdown
     		try {
     			Thread.sleep(5000);
     		} catch (InterruptedException e) {
     			// nothing
     		}
     	}
     }
     
     public void enableWifi() {
     	if (origWifiState) {
         	// Waiting for interface-restart
     		this.wifiManager.setWifiEnabled(true);
     		try {
     			Thread.sleep(5000);
     		} catch (InterruptedException e) {
     			// nothing
     		}
 
     		Log.d(MSG_TAG, "Wifi started!");
     	}
     }
     
     // WakeLock
 	public void releaseWakeLock() {
 		try {
 			if(this.wakeLock != null && this.wakeLock.isHeld()) {
 				Log.d(MSG_TAG, "Trying to release WakeLock NOW!");
 				this.wakeLock.release();
 			}
 		} catch (Exception ex) {
 			Log.d(MSG_TAG, "Ups ... an exception happend while trying to release WakeLock - Here is what I know: "+ex.getMessage());
 		}
 	}
     
 	public void acquireWakeLock() {
 		try {
 			if (this.isWakeLockDisabled() == false) {
 				Log.d(MSG_TAG, "Trying to acquire WakeLock NOW!");
 				this.wakeLock.acquire();
 			}
 		} catch (Exception ex) {
 			Log.d(MSG_TAG, "Ups ... an exception happend while trying to acquire WakeLock - Here is what I know: "+ex.getMessage());
 		}
 	}
     
     public int getNotificationType() {
 		return Integer.parseInt(this.settings.getString("notificationpref", "2"));
     }
     
     // Notification
     public void showStartNotification() {
 		notification.flags = Notification.FLAG_ONGOING_EVENT;
     	notification.setLatestEventInfo(this, "Wireless Tether", "Tethering is currently running ...", this.mainIntent);
     	this.notificationManager.notify(-1, this.notification);
     }
     
     Handler clientConnectHandler = new Handler() {
  	   public void handleMessage(Message msg) {
  		    ClientData clientData = (ClientData)msg.obj;
  		   TetherApplication.this.showClientConnectNotification(clientData, msg.what);
  	   }
     };
     
     public void showClientConnectNotification(ClientData clientData, int authType) {
     	int notificationIcon = R.drawable.secmedium;
     	String notificationString = "";
     	switch (authType) {
 	    	case CLIENT_CONNECT_ACDISABLED :
 	    		notificationIcon = R.drawable.secmedium;
 	    		notificationString = "AC disabled";
 	    		break;
 	    	case CLIENT_CONNECT_AUTHORIZED :
 	    		notificationIcon = R.drawable.sechigh;
 	    		notificationString = "Authorized";
 	    		break;
 	    	case CLIENT_CONNECT_NOTAUTHORIZED :
 	    		notificationIcon = R.drawable.seclow;
 	    		notificationString = "Unauthorized";
     	}
 		Log.d(MSG_TAG, "New (" + notificationString + ") client connected ==> "+clientData.getClientName()+" - "+clientData.getMacAddress());
  	   	Notification clientConnectNotification = new Notification(notificationIcon, "Wireless Tether", System.currentTimeMillis());
  	   	clientConnectNotification.tickerText = clientData.getClientName()+" ("+clientData.getMacAddress()+")";
  	   	if (!this.settings.getString("notifyring", "").equals(""))
  	   		clientConnectNotification.sound = Uri.parse(this.settings.getString("notifyring", ""));
 
  	   	if(this.settings.getBoolean("notifyvibrate", true))
  	   		clientConnectNotification.vibrate = new long[] {100, 200, 100, 200};
 
  	   	clientConnectNotification.setLatestEventInfo(this, "Wireless Tether - " + notificationString, clientData.getClientName()+" ("+clientData.getMacAddress()+") connected ...", this.accessControlIntent);
  	   	clientConnectNotification.flags = Notification.FLAG_AUTO_CANCEL;
  	   	this.notificationManager.notify(this.clientNotificationCount, clientConnectNotification);
  	   	this.clientNotificationCount++;
     }    
     
     public void recoverConfig() {
 
     	// Updating tiwlan.conf
     	Hashtable<String,String> values = new Hashtable<String,String>();
     	// SSID
     	values.put("dot11DesiredSSID", this.settings.getString("ssidpref", "G1Tether"));
     	// Channel
     	values.put("dot11DesiredChannel", this.settings.getString("channelpref", "6"));
     	// Powermode
     	values.put("dot11PowerMode", this.settings.getString("powermodepref", "1"));
     	// writing tiwlan-config
     	this.tiwlan.write(values);
     	
     	// updating lan-settings
     	String lanconfig = this.settings.getString("lannetworkpref", "192.168.2.0/24");
     	this.coretask.writeLanConf(lanconfig);
     	
     	this.displayToastMessage("Configuration recovered.");
     }
     
     public boolean binariesExists() {
     	File file = new File(this.coretask.DATA_FILE_PATH+"/bin/tether");
     	return file.exists();
     }
     
     public void installWpaSupplicantConfig() {
     	this.copyBinary(this.coretask.DATA_FILE_PATH+"/conf/wpa_supplicant.conf", R.raw.wpa_supplicant_conf);
     }
     
     Handler displayMessageHandler = new Handler(){
         public void handleMessage(Message msg) {
        		if (msg.obj != null) {
        			TetherApplication.this.displayToastMessage((String)msg.obj);
        		}
         	super.handleMessage(msg);
         }
     };
     
     public void renewLibrary() {
     	File libNativeTaskFile = new File(TetherApplication.this.coretask.DATA_FILE_PATH+"/library/.libNativeTask.so");
     	if (libNativeTaskFile.exists()){
     		libNativeTaskFile.renameTo(new File(TetherApplication.this.coretask.DATA_FILE_PATH+"/library/libNativeTask.so"));
     	}
     }
     
     public void installFiles() {
     	new Thread(new Runnable(){
 			public void run(){
 				String message = null;
 				// tether
 		    	if (message == null) {
 			    	message = TetherApplication.this.copyBinary(TetherApplication.this.coretask.DATA_FILE_PATH+"/bin/tether", R.raw.tether);
 		    	}
 		    	// dnsmasq
 		    	if (message == null) {
 			    	message = TetherApplication.this.copyBinary(TetherApplication.this.coretask.DATA_FILE_PATH+"/bin/dnsmasq", R.raw.dnsmasq);
 		    	}
 		    	// iptables
 		    	if (message == null) {
 			    	message = TetherApplication.this.copyBinary(TetherApplication.this.coretask.DATA_FILE_PATH+"/bin/iptables", R.raw.iptables);
 		    	}		    	
 		    	// iwconfig
 		    	if (message == null) {
 			    	message = TetherApplication.this.copyBinary(TetherApplication.this.coretask.DATA_FILE_PATH+"/bin/iwconfig", R.raw.iwconfig);
 		    	}	
 		    	//pand
 		    	if (message == null) {
 			    	message = TetherApplication.this.copyBinary(TetherApplication.this.coretask.DATA_FILE_PATH+"/bin/pand", R.raw.pand);
 		    	}
 		    	// blue-up.sh
 				if (message == null) {
 					message = TetherApplication.this.copyBinary(TetherApplication.this.coretask.DATA_FILE_PATH+"/bin/blue-up.sh", R.raw.blue_up_sh);
 				}
 				// blue-down.sh
 				if (message == null) {
 					message = TetherApplication.this.copyBinary(TetherApplication.this.coretask.DATA_FILE_PATH+"/bin/blue-down.sh", R.raw.blue_down_sh);
 				}
 				try {
 		    		TetherApplication.this.coretask.chmodBin();
 				} catch (Exception e) {
 					message = "Unable to change permission on binary files!";
 				}
 		    	// dnsmasq.conf
 				if (message == null) {
 					message = TetherApplication.this.copyBinary(TetherApplication.this.coretask.DATA_FILE_PATH+"/conf/dnsmasq.conf", R.raw.dnsmasq_conf);
 					TetherApplication.this.coretask.updateDnsmasqFilepath();
 				}
 		    	// tiwlan.ini
 				if (message == null) {
 					TetherApplication.this.copyBinary(TetherApplication.this.coretask.DATA_FILE_PATH+"/conf/tiwlan.ini", R.raw.tiwlan_ini);
 				}
 		    	// version
 				if (message == null) {
 					TetherApplication.this.copyBinary(TetherApplication.this.coretask.DATA_FILE_PATH+"/conf/version", R.raw.version);
 				}		
 				// edify script
 				if (message == null) {
 					TetherApplication.this.copyBinary(TetherApplication.this.coretask.DATA_FILE_PATH+"/conf/tether.edify", R.raw.tether_edify);
 				}
 				// tether.cfg
 				if (message == null) {
 					TetherApplication.this.copyBinary(TetherApplication.this.coretask.DATA_FILE_PATH+"/conf/tether.cfg", R.raw.tether_cfg);
 				}
 				// wpa_supplicant drops privileges, we need to make files readable.
 				TetherApplication.this.coretask.chmod(TetherApplication.this.coretask.DATA_FILE_PATH+"/conf/", "0755");
 
 				TetherApplication.this.coretask.chmod(TetherApplication.this.coretask.DATA_FILE_PATH+"/conf/*", "0644");
 				if (message == null) {
 			    	message = "Binaries and config-files installed!";
 				}
 				
 				// Sending message
 				Message msg = new Message();
 				msg.obj = message;
 				TetherApplication.this.displayMessageHandler.sendMessage(msg);
 			}
 		}).start();
     }
     
     public void checkForUpdate() {
     	if (this.isUpdatecDisabled()) {
     		Log.d(MSG_TAG, "Update-checks are disabled!");	
     		return;
     	}
     	new Thread(new Runnable(){
 			public void run(){
 				Looper.prepare();
 				// Getting Properties
 				Properties updateProperties = TetherApplication.this.webserviceTask.queryForProperty(APPLICATION_PROPERTIES_URL);
 				if (updateProperties != null && updateProperties.containsKey("versionCode") && updateProperties.containsKey("fileName")) {
 					int availableVersion = Integer.parseInt(updateProperties.getProperty("versionCode"));
 					int installedVersion = TetherApplication.this.getVersionNumber();
 					String fileName = updateProperties.getProperty("fileName");
 					if (availableVersion != installedVersion) {
 						Log.d(MSG_TAG, "Installed version '"+installedVersion+"' and available version '"+availableVersion+"' do not match!");
 						MainActivity.currentInstance.openUpdateDialog(APPLICATION_DOWNLOAD_URL+fileName, fileName);
 					}
 				}
 				Looper.loop();
 			}
     	}).start();
     }
    
     public void downloadUpdate(final String downloadFileUrl, final String fileName) {
     	new Thread(new Runnable(){
 			public void run(){
 				Message msg = Message.obtain();
             	msg.what = MainActivity.MESSAGE_DOWNLOAD_STARTING;
             	msg.obj = "Downloading update...";
             	MainActivity.currentInstance.viewUpdateHandler.sendMessage(msg);
 				TetherApplication.this.webserviceTask.downloadUpdateFile(downloadFileUrl, fileName);
 				Intent intent = new Intent(Intent.ACTION_VIEW); 
 			    intent.setDataAndType(android.net.Uri.fromFile(new File(WebserviceTask.DOWNLOAD_FILEPATH+"/"+fileName)),"application/vnd.android.package-archive"); 
 			    MainActivity.currentInstance.startActivity(intent);
 			}
     	}).start();
     }
     
     private String copyBinary(String filename, int resource) {
     	File outFile = new File(filename);
     	Log.d(MSG_TAG, "Copying file '"+filename+"' ...");
     	InputStream is = this.getResources().openRawResource(resource);
     	byte buf[]=new byte[1024];
         int len;
         try {
         	OutputStream out = new FileOutputStream(outFile);
         	while((len = is.read(buf))>0) {
 				out.write(buf,0,len);
 			}
         	out.close();
         	is.close();
 		} catch (IOException e) {
 			return "Couldn't install file - "+filename+"!";
 		}
 		return null;
     }
     
 
     private void checkDirs() {
     	File dir = new File(this.coretask.DATA_FILE_PATH);
     	if (dir.exists() == false) {
     			this.displayToastMessage("Application data-dir does not exist!");
     	}
     	else {
     		String[] dirs = { "/bin", "/var", "/conf", "/library" };
     		for (String dirname : dirs) {
     			dir = new File(this.coretask.DATA_FILE_PATH + dirname);
     	    	if (dir.exists() == false) {
     	    		if (!dir.mkdir()) {
     	    			this.displayToastMessage("Couldn't create " + dirname + " directory!");
     	    		}
     	    	}
     	    	else {
     	    		Log.d(MSG_TAG, "Directory '"+dir.getAbsolutePath()+"' already exists!");
     	    	}
     		}
     	}
     }
     
     public void restartSecuredWifi() {
     	try {
 			if (this.coretask.isNatEnabled() && this.coretask.isProcessRunning("bin/dnsmasq")) {
 		    	Log.d(MSG_TAG, "Restarting iptables for access-control-changes!");
 				if (!this.coretask.runRootCommand(this.coretask.DATA_FILE_PATH+"/bin/tether restartsecwifi")) {
 					this.displayToastMessage("Unable to restart secured wifi!");
 					return;
 				}
 			}
 		} catch (Exception e) {
 			// nothing
 		}
     }
     
     // Display Toast-Message
 	public void displayToastMessage(String message) {
 		Toast.makeText(this, message, Toast.LENGTH_LONG).show();
 	}
     
     public int getVersionNumber() {
     	int version = -1;
         try {
             PackageInfo pi = getPackageManager().getPackageInfo(getPackageName(), 0);
             version = pi.versionCode;
         } catch (Exception e) {
             Log.e(MSG_TAG, "Package name not found", e);
         }
         return version;
     }
     
     public String getVersionName() {
     	String version = "?";
         try {
             PackageInfo pi = getPackageManager().getPackageInfo(getPackageName(), 0);
             version = pi.versionName;
         } catch (Exception e) {
             Log.e(MSG_TAG, "Package name not found", e);
         }
         return version;
     }
     
    	public void clientConnectEnable(boolean enable) {
    		if (enable == true) {
 			if (this.clientConnectThread == null || this.clientConnectThread.isAlive() == false) {
 				this.clientConnectThread = new Thread(new ClientConnect());
 				this.clientConnectThread.start();
 			}
    		} else {
 	    	if (this.clientConnectThread != null)
 	    		this.clientConnectThread.interrupt();
    		}
    	}    
     
     class ClientConnect implements Runnable {
 
         private ArrayList<String> knownWhitelists = new ArrayList<String>();
         private ArrayList<String> knownLeases = new ArrayList<String>();
         private Hashtable<String, ClientData> currentLeases = new Hashtable<String, ClientData>();
         private long timestampLeasefile = -1;
         private long timestampWhitelistfile = -1;
 
         // @Override
         public void run() {
             while (!Thread.currentThread().isInterrupted()) {
             	Log.d(MSG_TAG, "Checking for new clients ... ");
             	// Notification-Type
             	int notificationType = TetherApplication.this.getNotificationType();
             	// Access-Control activated
             	boolean accessControlActive = TetherApplication.this.whitelist.exists();
 		        // Checking if Access-Control is activated
 		        if (accessControlActive) {
                     // Checking whitelistfile
                     long currentTimestampWhitelistFile = TetherApplication.this.coretask.getModifiedDate(TetherApplication.this.coretask.DATA_FILE_PATH + "/conf/whitelist_mac.conf");
                     if (this.timestampWhitelistfile != currentTimestampWhitelistFile) {
                         knownWhitelists = TetherApplication.this.whitelist.get();
                         this.timestampWhitelistfile = currentTimestampWhitelistFile;
                     }
 		        }
 
                 // Checking leasefile
                 long currentTimestampLeaseFile = TetherApplication.this.coretask.getModifiedDate(TetherApplication.this.coretask.DATA_FILE_PATH + "/var/dnsmasq.leases");
                 if (this.timestampLeasefile != currentTimestampLeaseFile) {
                     try {
                     	// Getting current dns-leases
                         this.currentLeases = TetherApplication.this.coretask.getLeases();
                         
                         // Cleaning-up knownLeases after a disconnect (dhcp-release)
                         for (String lease : this.knownLeases) {
                             if (this.currentLeases.containsKey(lease) == false) {
                             	Log.d(MSG_TAG, "Removing '"+lease+"' from known-leases!");
                                 this.knownLeases.remove(lease);
                             	
                                 notifyActivity();
                             	TetherApplication.this.removeClientMac(lease);
                             }
                         }
                         
                         Enumeration<String> leases = this.currentLeases.keys();
                         while (leases.hasMoreElements()) {
                             String mac = leases.nextElement();
                             Log.d(MSG_TAG, "Mac-Address: '"+mac+"' - Known Whitelist: "+knownWhitelists.contains(mac)+" - Known Lease: "+knownLeases.contains(mac));
                             if (knownLeases.contains(mac) == false) {
 	                            if (knownWhitelists.contains(mac) == false) {
 	                            	// AddClientData to TetherApplication-Class for AccessControlActivity
 	                            	TetherApplication.this.addClientData(this.currentLeases.get(mac));
 	                            	
 	                            	if (accessControlActive) {
 	                            		if (notificationType == 1 || notificationType == 2) {
 	                            			this.sendClientMessage(this.currentLeases.get(mac),
 	                            					CLIENT_CONNECT_NOTAUTHORIZED);
 	                            		}
 	                            	}
 	                            	else {
 	                            		if (notificationType == 2) {
 	                            			this.sendClientMessage(this.currentLeases.get(mac),
 	                            					CLIENT_CONNECT_ACDISABLED);
 	                            		}
 	                            	}
 	                                this.knownLeases.add(mac);
 	                            } else if (knownWhitelists.contains(mac) == true) {
 	                            	// AddClientData to TetherApplication-Class for AccessControlActivity
 	                            	ClientData clientData = this.currentLeases.get(mac);
 	                            	clientData.setAccessAllowed(true);
 	                            	TetherApplication.this.addClientData(clientData);
 	                            	
 	                                if (notificationType == 2) {
 	                                    this.sendClientMessage(this.currentLeases.get(mac),
 	                                    		CLIENT_CONNECT_AUTHORIZED);
 	                                    this.knownLeases.add(mac);
 	                                }
 	                            }
 	                            notifyActivity();
                             }
                         }
                         this.timestampLeasefile = currentTimestampLeaseFile;
                     } catch (Exception e) {
                         Log.d(MSG_TAG, "Unexpected error detected - Here is what I know: " + e.getMessage());
                         e.printStackTrace();
                     }
                 }
                 try {
                     Thread.sleep(3000);
                 } catch (InterruptedException e) {
                     Thread.currentThread().interrupt();
                 }
             }
         }
 
         private void notifyActivity(){
         	if (AccessControlActivity.currentInstance != null){
         		AccessControlActivity.currentInstance.clientConnectHandler.sendMessage(new Message());
         	}
         }
         
         private void sendClientMessage(ClientData clientData, int connectType) {
             Message m = new Message();
             m.obj = clientData;
             m.what = connectType;
             TetherApplication.this.clientConnectHandler.sendMessage(m);
         }
 
     }
  
     public void dnsUpdateEnable(boolean enable) {
     	this.dnsUpdateEnable(null, enable);
     }
     
    	public void dnsUpdateEnable(String[] dns, boolean enable) {
    		if (enable == true) {
 			if (this.dnsUpdateThread == null || this.dnsUpdateThread.isAlive() == false) {
 				this.dnsUpdateThread = new Thread(new DnsUpdate(dns));
 				this.dnsUpdateThread.start();
 			}
    		} else {
 	    	if (this.dnsUpdateThread != null)
 	    		this.dnsUpdateThread.interrupt();
    		}
    	}
        
     class DnsUpdate implements Runnable {
 
     	String[] dns;
     	
     	public DnsUpdate(String[] dns) {
     		this.dns = dns;
     	}
     	
 		@Override
 		public void run() {
             while (!Thread.currentThread().isInterrupted()) {
             	String[] currentDns = TetherApplication.this.coretask.getCurrentDns();
             	if (this.dns == null || this.dns[0].equals(currentDns[0]) == false || this.dns[1].equals(currentDns[1]) == false) {
             		this.dns = TetherApplication.this.coretask.updateResolvConf();
             	}
             }
             // Taking a nap
    			try {
 				Thread.sleep(2000);
 			} catch (InterruptedException e) {
 				Thread.currentThread().interrupt();
 			}
 		}
     }    
     
    	public void trafficCounterEnable(boolean enable) {
    		if (enable == true) {
 			if (this.trafficCounterThread == null || this.trafficCounterThread.isAlive() == false) {
 				this.trafficCounterThread = new Thread(new TrafficCounter());
 				this.trafficCounterThread.start();
 			}
    		} else {
 	    	if (this.trafficCounterThread != null)
 	    		this.trafficCounterThread.interrupt();
    		}
    	}
    	
    	class TrafficCounter implements Runnable {
    		private static final int INTERVAL = 2;  // Sample rate in seconds.
    		long previousDownload;
    		long previousUpload;
    		long lastTimeChecked;
    		public void run() {
    			this.previousDownload = this.previousUpload = 0;
    			this.lastTimeChecked = new Date().getTime();
 
    			while (!Thread.currentThread().isInterrupted()) {
 		        // Check data count
 		        long [] trafficCount = TetherApplication.this.coretask.getDataTraffic(
 		        		TetherApplication.this.tetherNetworkDevice);
 		        long currentTime = new Date().getTime();
 		        float elapsedTime = (float) ((currentTime - this.lastTimeChecked) / 1000);
 		        this.lastTimeChecked = currentTime;
 		        DataCount datacount = new DataCount();
 		        datacount.totalUpload = trafficCount[0];
 		        datacount.totalDownload = trafficCount[1];
 		        datacount.uploadRate = (long) ((datacount.totalUpload - this.previousUpload)*8/elapsedTime);
 		        datacount.downloadRate = (long) ((datacount.totalDownload - this.previousDownload)*8/elapsedTime);
 				Message message = Message.obtain();
 				message.what = MainActivity.MESSAGE_TRAFFIC_COUNT;
 				message.obj = datacount;
 				MainActivity.currentInstance.viewUpdateHandler.sendMessage(message); 
 				this.previousUpload = datacount.totalUpload;
 				this.previousDownload = datacount.totalDownload;
                 try {
                     Thread.sleep(INTERVAL * 1000);
                 } catch (InterruptedException e) {
                     Thread.currentThread().interrupt();
                 }
    			}
 			Message message = Message.obtain();
 			message.what = MainActivity.MESSAGE_TRAFFIC_END;
 			MainActivity.currentInstance.viewUpdateHandler.sendMessage(message); 
    		}
    	}
    	
    	public class DataCount {
    		// Total data uploaded
    		public long totalUpload;
    		// Total data downloaded
    		public long totalDownload;
    		// Current upload rate
    		public long uploadRate;
    		// Current download rate
    		public long downloadRate;
    	}
 }
