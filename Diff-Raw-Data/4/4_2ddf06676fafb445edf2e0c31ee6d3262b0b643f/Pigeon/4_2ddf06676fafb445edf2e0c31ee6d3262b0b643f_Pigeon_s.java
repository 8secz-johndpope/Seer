 package com.icecondor.nest;
 
 import java.io.IOException;
 import java.net.URISyntaxException;
 import java.net.URL;
 import java.net.UnknownHostException;
 import java.util.ArrayList;
 import java.util.Map;
 import java.util.Timer;
 import java.util.TimerTask;
 
 import net.oauth.OAuth;
 import net.oauth.OAuthAccessor;
 import net.oauth.OAuthException;
 import net.oauth.OAuthMessage;
 import net.oauth.client.OAuthClient;
 import net.oauth.client.httpclient4.HttpClient4;
 import net.oauth.client.httpclient4.HttpClientPool;
 
 import org.apache.http.client.ClientProtocolException;
 import org.apache.http.client.HttpClient;
 import org.apache.http.client.params.AllClientPNames;
 import org.apache.http.impl.client.DefaultHttpClient;
 import org.json.JSONException;
 import org.json.JSONObject;
 
 import android.app.Notification;
 import android.app.NotificationManager;
 import android.app.PendingIntent;
 import android.app.Service;
 import android.content.Intent;
 import android.content.SharedPreferences;
 import android.database.Cursor;
 import android.location.Location;
 import android.location.LocationListener;
 import android.location.LocationManager;
 import android.location.LocationProvider;
 import android.media.MediaPlayer;
 import android.net.wifi.WifiManager;
 import android.os.Bundle;
 import android.os.IBinder;
 import android.os.RemoteException;
 import android.preference.PreferenceManager;
 import android.util.Log;
 
 import com.icecondor.nest.db.GeoRss;
 import com.icecondor.nest.db.LocationStorageProviders;
 
 //look at android.permission.RECEIVE_BOOT_COMPLETED
 
 public class Pigeon extends Service implements Constants, LocationListener,
                                                SharedPreferences.OnSharedPreferenceChangeListener {
 	private Timer heartbeat_timer;
 	private Timer rss_timer;
 	private Timer push_queue_timer;
 	//private Timer wifi_scan_timer = new Timer();
 	static final String appTag = "Pigeon";
 	boolean on_switch = false;
 	private Location last_recorded_fix, last_pushed_fix, last_local_fix;
 	long last_pushed_time;
 	int last_fix_http_status;
 	Notification ongoing_notification;
 	NotificationManager notificationManager;
 	LocationManager locationManager;
 	WifiManager wifiManager;
 	PendingIntent contentIntent;
 	SharedPreferences settings;
 	GeoRss rssdb;
 	MediaPlayer mp;
 	private TimerTask heartbeatTask;
 	DefaultHttpClient httpClient;
 	OAuthClient oclient;
 	
 	public void onCreate() {
 		Log.i(appTag, "*** service created.");
 		super.onCreate();
 		
 		/* Database */
 		rssdb = new GeoRss(this);
 		rssdb.open();
 		rssdb.log("Pigon created");
 
 		/* GPS */
 		locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
 		Log.i(appTag, "GPS provider enabled: "+locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER));
 		last_local_fix = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
		Log.i(appTag, "Last known GPS fix: "+last_local_fix+" "+Util.DateTimeIso8601(last_local_fix.getTime()));
 		Log.i(appTag, "NETWORK provider enabled: "+locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER));
 		Location last_network_fix = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
 		Log.i(appTag, "Last known NETWORK fix: "+last_network_fix+" "+Util.DateTimeIso8601(last_network_fix.getTime()));
 		if (last_local_fix == null) { // fall back onto the network location
 			last_local_fix = last_network_fix;
 		}
 		
 		/* WIFI */
 		wifiManager = (WifiManager) getSystemService(WIFI_SERVICE);
 		
 		/* Notifications */
 		notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
 		contentIntent = PendingIntent.getActivity(this, 0, new Intent(this,
 				Start.class), 0);
 		CharSequence text = getText(R.string.status_started);
 		ongoing_notification = new Notification(R.drawable.condorhead_statusbar, text, System
 				.currentTimeMillis());
 		ongoing_notification.flags = ongoing_notification.flags ^ Notification.FLAG_ONGOING_EVENT;
 		ongoing_notification.setLatestEventInfo(this, "IceCondor", "", contentIntent);
 
 
 		/* Preferences */
 		settings = PreferenceManager.getDefaultSharedPreferences(this);
 		settings.registerOnSharedPreferenceChangeListener(this);
 		on_switch = settings.getBoolean(SETTING_PIGEON_TRANSMITTING, false);
 		if (on_switch) {
 			//startForeground(1, ongoing_notification);
 			startLocationUpdates();
 		}
 
 		/* Sound */
 		mp = MediaPlayer.create(this, R.raw.beep);	
 		
 		startHeartbeatTimer();
 		startRssTimer();
 		startPushQueueTimer();
 		
 		/* Apache HTTP Monstrosity*/
 		httpClient =  new DefaultHttpClient();
 		httpClient.getParams().setIntParameter(AllClientPNames.CONNECTION_TIMEOUT, 15 *1000);
 		httpClient.getParams().setIntParameter(AllClientPNames.SO_TIMEOUT, 30 *1000);
 		oclient = new OAuthClient(new HttpClient4());
 	}
 
 	public void onStart(Intent start, int key) {
 		super.onStart(start,key);
 		rssdb.log("Pigon started");
 	}
 	
 	public void onDestroy() {
 		stopRssTimer();
 		stopLocationUpdates();
 		stopPushQueueTimer();
 		rssdb.log("Pigon destroyed");
 		rssdb.close();
 		notificationManager.cancel(1);
 	}
 	
 	private void notificationStatusUpdate(String msg) {
 		ongoing_notification.setLatestEventInfo(this, "IceCondor",
 				msg, contentIntent);
 		ongoing_notification.when = System.currentTimeMillis();
 		notificationManager.notify(1, ongoing_notification);
 	}
 	
 	private void notification(String msg) {
 		Notification notification = new Notification(R.drawable.condorhead_statusbar, msg,
 				System.currentTimeMillis());
 		// a contentView error is thrown if this line is not here
 		notification.setLatestEventInfo(this, "IceCondor Notice", msg, contentIntent);
 		notificationManager.notify(2, notification);
 	}
 	
 	private void notificationFlash(String msg) {
 		notification(msg);
 		notificationManager.cancel(2);
 	}
 
 	private void startLocationUpdates() {
 		long record_frequency = Long.decode(settings.getString(SETTING_TRANSMISSION_FREQUENCY, "300000"));
 		rssdb.log("requesting Location Updates every "+ Util.millisecondsToWords(record_frequency));
 		locationManager.requestLocationUpdates(
 				LocationManager.GPS_PROVIDER, 
 				record_frequency, 
 				0.0F, this);
 		// Network provider takes no extra power but the accuracy is
 		// too low to be useful.
 		//locationManager.requestLocationUpdates(
 		//		LocationManager.NETWORK_PROVIDER, 60000L, 0.0F, pigeon);
 //		Log.i(appTag, "kicking off wifi scan timer");
 //		wifi_scan_timer.scheduleAtFixedRate(
 //				new TimerTask() {
 //					public void run() {
 //						Log.i(appTag, "wifi: start scan (enabled:"+wifiManager.isWifiEnabled()+")");
 //						wifiManager.startScan();
 //					}
 //				}, 0, 60000);		
 	}
 	
 	private void stopLocationUpdates() {
 		rssdb.log("pigeon: stopping GPS updates");		
 		locationManager.removeUpdates(this);
 	}
 
 	@Override
 	public IBinder onBind(Intent intent) {
 		rssdb.log("Pigeon onBind for "+intent.getAction());
 		return pigeonBinder;
 	}
 	
 	@Override
 	public void onRebind(Intent intent) {
 		rssdb.log("pigeon: onReBind for "+intent.getAction());
 	}
 	
 	@Override
 	public void onLowMemory() {
 		rssdb.log("onLowMemory");
 	}
 	
 	@Override
 	public boolean onUnbind(Intent intent) {
 		Log.i(appTag, "onUnbind for "+intent.getAction());
 		rssdb.log("pigeon: onUnbind for "+intent.getAction());
 		return false;
 	}
 	
 	public void pushQueue() {
 		Timer push_queue_timer_single = new Timer("Push Queue Single Timer");
 		push_queue_timer_single.schedule(new PushQueueTask(), 0);
 	}
 
 	class PushQueueTask extends TimerTask {
 		public void run() {
 			Cursor oldest;
 			rssdb.log("** Starting queue push of size "+rssdb.countPositionQueueRemaining());
 			if ((oldest = rssdb.oldestUnpushedLocationQueue()).getCount() > 0) {
 				int id = oldest.getInt(oldest.getColumnIndex("_id"));
 				Location fix =  locationFromJson( oldest.getString(
 				                    oldest.getColumnIndex(GeoRss.POSITION_QUEUE_JSON)));
 				int status = pushLocation(fix);
 				if (status == 200) {
 					rssdb.log("queue push #"+id+" OK");
 					rssdb.mark_as_pushed(id);
 				} else {
 					rssdb.log("queue push #"+id+" FAIL");
 				}
 			} 
 			oldest.close();
 			rssdb.log("** Finished queue push. size = "+rssdb.countPositionQueueRemaining());
 		}
 	}
 	
 	public int pushLocation(Location fix) {
 		Log.i(appTag, "sending id: "+settings.getString(SETTING_OPENID,"")+ " fix: " 
 				+fix.getLatitude()+" long: "+fix.getLongitude()+
 				" alt: "+fix.getAltitude() + " time: " + Util.DateTimeIso8601(fix.getTime()) +
 				" acc: "+fix.getAccuracy());
 		rssdb.log("pushing fix "+" time: " + Util.DateTimeIso8601(fix.getTime()) +
 				"("+fix.getTime()+") acc: "+fix.getAccuracy());
 		if (settings.getBoolean(SETTING_BEEP_ON_FIX, false)) {
 			play_fix_beep();
 		}
 		//ArrayList <NameValuePair> params = new ArrayList <NameValuePair>();
 		ArrayList<Map.Entry<String, String>> params = new ArrayList<Map.Entry<String, String>>();
 		addPostParameters(params, fix);
 		last_pushed_fix = fix;
 		last_pushed_time = System.currentTimeMillis();
 
 		OAuthAccessor accessor = LocationStorageProviders.defaultAccessor(this);
 		String[] token_and_secret = LocationStorageProviders.getDefaultAccessToken(this);
 		params.add(new OAuth.Parameter("oauth_token", token_and_secret[0]));
 		accessor.tokenSecret = token_and_secret[1];
 		try {
 			OAuthMessage omessage;
 			Log.d(appTag, "invoke("+accessor+", POST, "+ICECONDOR_WRITE_URL+", "+params);
 			omessage = oclient.invoke(accessor, "POST",  ICECONDOR_WRITE_URL, params);
 			omessage.getHeader("Result");
 			last_fix_http_status = 200;
 			return last_fix_http_status;
 		} catch (OAuthException e) {
 			rssdb.log("push OAuthException "+e);
 		} catch (URISyntaxException e) {
 			rssdb.log("push URISyntaxException "+e);
 		} catch (UnknownHostException e) {
 			rssdb.log("push UnknownHostException "+e);
 		} catch (IOException e) {
 			// includes host not found
 			rssdb.log("push IOException "+e);
 		}
 		last_fix_http_status = 500;
 		return last_fix_http_status; // something went wrong
 	}
 	
 	private void addPostParameters(ArrayList<Map.Entry<String, String>> dict, Location fix) {
 		dict.add(new Util.Parameter("location[latitude]", Double.toString(fix.getLatitude())));
 		dict.add(new Util.Parameter("location[longitude]", Double.toString(fix.getLongitude())));
 		dict.add(new Util.Parameter("location[altitude]", Double.toString(fix.getAltitude())));
 		dict.add(new Util.Parameter("client[version]", ""+ICECONDOR_VERSION));
 		if(fix.hasAccuracy()) {
 			dict.add(new Util.Parameter("location[accuracy]", Double.toString(fix.getAccuracy())));
 		}
 		if(fix.hasBearing()) {
 			dict.add(new Util.Parameter("location[heading]", Double.toString(fix.getBearing())));
 		}
 		if(fix.hasSpeed()) {
 			dict.add(new Util.Parameter("location[velocity]", Double.toString(fix.getSpeed())));
 		}
 		
 		dict.add(new Util.Parameter("location[timestamp]", Util.DateTimeIso8601(fix.getTime())));
 	}
 				
 	public void onLocationChanged(Location location) {
 		last_local_fix = location;
 		long time_since_last_update = last_local_fix.getTime() - (last_recorded_fix == null?0:last_recorded_fix.getTime()); 
 		long record_frequency = Long.decode(settings.getString(SETTING_TRANSMISSION_FREQUENCY, "180000"));
 		rssdb.log("pigeon onLocationChanged: lat:"+location.getLatitude()+
 				  " long:"+location.getLongitude() + " acc:"+
 			       location.getAccuracy()+" "+ 
 			       (time_since_last_update/1000)+" seconds since last update");
 
 		if (on_switch) {
 			if((last_local_fix.getAccuracy() < (last_recorded_fix == null?
 					                            500000:last_recorded_fix.getAccuracy())) ||
 					time_since_last_update > record_frequency ) {
 				last_recorded_fix = last_local_fix;
 				last_fix_http_status = 200;
 				long id = rssdb.addPosition(locationToJson(last_local_fix));
 				rssdb.log("Pigeon location queued. location #"+id);
 				pushQueue();
 			}
 		}
 	}
 
 	private String locationToJson(Location lastLocalFix) {
 		try {
 		JSONObject position = new JSONObject();
 			position.put("latitude", lastLocalFix.getLatitude());
 			position.put("longitude", lastLocalFix.getLongitude());
 			position.put("time", lastLocalFix.getTime());
 			position.put("altitude", lastLocalFix.getAltitude());
 			position.put("accuracy", lastLocalFix.getAccuracy());
 			position.put("bearing", lastLocalFix.getBearing());
 			position.put("speed", lastLocalFix.getSpeed());
 			JSONObject jloc = new JSONObject();
 			jloc.put("location", position);
 			return jloc.toString();
 		} catch (JSONException e) {
 			return "{\"ERROR\":\""+e.toString()+"\"}";
 		}
 	}
 
 	private Location locationFromJson(String json) {
 		try {
 			JSONObject j = new JSONObject(json);
 			JSONObject p = j.getJSONObject("location");
 			Location l = new Location("");
 			l.setLatitude(p.getDouble("latitude"));
 			l.setLongitude(p.getDouble("longitude"));
 			l.setTime(p.getLong("time"));
 			l.setAltitude(p.getDouble("altitude"));
 			l.setAccuracy(new Float(p.getDouble("accuracy")));
 			l.setBearing(new Float(p.getDouble("bearing")));
 			l.setSpeed(new Float(p.getDouble("speed")));
 			return l;
 		} catch (JSONException e) {
 			e.printStackTrace();
 			return null;
 		}
 	}
 	
 	private void play_fix_beep() {
 		mp.start();
 	}
 	public void onProviderDisabled(String provider) {
 		Log.i(appTag, "provider "+provider+" disabled");
 		rssdb.log("provider "+provider+" disabled");
 	}
 
 	public void onProviderEnabled(String provider) {
 		Log.i(appTag, "provider "+provider+" enabled");		
 		rssdb.log("provider "+provider+" enabled");
 	}
 
 	public void onStatusChanged(String provider, int status, Bundle extras) {
 		String status_msg = "";
 		if (status ==  LocationProvider.TEMPORARILY_UNAVAILABLE) {status_msg = "TEMPORARILY_UNAVAILABLE";}
 		if (status ==  LocationProvider.OUT_OF_SERVICE) {status_msg = "OUT_OF_SERVICE";}
 		if (status ==  LocationProvider.AVAILABLE) {status_msg = "AVAILABLE";}
 		Log.i(appTag, "provider "+provider+" status changed to "+status_msg);
 		rssdb.log("GPS "+status_msg);
 	}
 
 	@Override
 	public void onSharedPreferenceChanged(SharedPreferences prefs, String pref_name) {
 		Log.i(appTag, "shared preference changed: "+pref_name);		
 		if (pref_name.equals(SETTING_TRANSMISSION_FREQUENCY)) {
 			if (on_switch) {
 				stopLocationUpdates();
 				startLocationUpdates();
 				notificationFlash("Position reporting frequency now "+Util.millisecondsToWords(
 						Long.parseLong(prefs.getString(pref_name, "N/A"))));
 			}
 		}
 		if (pref_name.equals(SETTING_RSS_READ_FREQUENCY)) {
 			stopRssTimer();
 			startRssTimer();
 			notificationFlash("RSS Read frequency now "+Util.millisecondsToWords(
 						Long.parseLong(prefs.getString(pref_name, "N/A"))));
 		}
 	}
 	
 	private void startHeartbeatTimer() {
 		heartbeat_timer = new Timer("Heartbeat Timer");
 		heartbeatTask = new HeartBeatTask();
 		heartbeat_timer.scheduleAtFixedRate(heartbeatTask, 0, 20000);
 	}
 	
 	private void startRssTimer() {
 		rss_timer = new Timer("RSS Reader Timer");
 		long rss_read_frequency = Long.decode(settings.getString(SETTING_RSS_READ_FREQUENCY, "60000"));
 		Log.i(appTag, "starting rss timer at frequency "+rss_read_frequency);
 		rss_timer.scheduleAtFixedRate(
 				new TimerTask() {
 					public void run() {
 						Log.i(appTag, "rss_timer fired");
 						updateRSS();
 					}
 				}, 0, rss_read_frequency);
 	}
 	
 	private void stopRssTimer() {
 		rss_timer.cancel();
 	}
 	
 	private void startPushQueueTimer() {
 		push_queue_timer = new Timer("PushQueue Timer");
 		push_queue_timer.scheduleAtFixedRate(new PushQueueTask(), 0, 30000);
 	}
 
 	private void stopPushQueueTimer() {
 		push_queue_timer.cancel();
 	}
 	
 	protected void updateRSS() {
 		new Timer().schedule(
 				new TimerTask() {
 					public void run() {
 						Log.i(appTag, "rss_timer fired");
 						Cursor geoRssUrls = rssdb.findFeeds();
 						while (geoRssUrls.moveToNext()) {
 							try {
 								rssdb.readGeoRss(geoRssUrls);
 							} catch (ClientProtocolException e) {
 								Log.i(appTag, "http protocol exception "+e);
 							} catch (IOException e) {
 								Log.i(appTag, "io error "+e);
 							}
 						}
 						geoRssUrls.close();						
 					}
 				}, 0);
 	}
 	
 	class HeartBeatTask extends TimerTask {
 		public void run() {
 			String fix_part = "";
 			if (on_switch) {
 				if (last_pushed_fix != null) {
 					String ago = Util.timeAgoInWords(last_pushed_time);
 					String fago = Util.timeAgoInWords(last_pushed_fix.getTime());
 					if (last_fix_http_status != 200) {
 						ago = "err.";
 					}
 					fix_part = "push "+ ago+"/"+fago+".";
 			    }
 				if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
 					fix_part = "Warning: GPS set to disabled";
 				}
 			} else {
 				fix_part = "Location reporting is off.";
 			}
 			String queue_part = ""+rssdb.countPositionQueueRemaining()+" queued.";
 		    String beat_part = "";
 		    if (last_local_fix != null) {
 		    	String ago = Util.timeAgoInWords(last_local_fix.getTime());
 		    	beat_part = "fix "+ago+".";
 		    }
 		    String msg = fix_part+" "+beat_part+" "+queue_part;
 			notificationStatusUpdate(msg); 
 		}
 	};
 
     private final PigeonService.Stub pigeonBinder = new PigeonService.Stub() {
 		public boolean isTransmitting() throws RemoteException {
 			Log.i(appTag, "isTransmitting => "+on_switch);
 			return on_switch;
 		}
 		public void startTransmitting() throws RemoteException {
 			if (on_switch) {
 				Log.i(appTag, "startTransmitting: already transmitting");
 			} else {
 				Log.i(appTag, "startTransmitting");
 				on_switch = true;
 				settings.edit().putBoolean(SETTING_PIGEON_TRANSMITTING, on_switch).commit();
 				startLocationUpdates();
 				notificationStatusUpdate("Waiting for fix.");				
 				notificationFlash("Location reporting ON.");
 			}
 		}
 		public void stopTransmitting() throws RemoteException {
 			Log.i(appTag, "stopTransmitting");
 			on_switch = false;
 			settings.edit().putBoolean(SETTING_PIGEON_TRANSMITTING,on_switch).commit();
 			stopLocationUpdates();
 			notificationStatusUpdate("Location reporting is off.");				
 			notificationFlash("Location reporting OFF.");
 		}
 		public Location getLastFix() throws RemoteException {
 			if(on_switch) {
 				return last_local_fix;
 			} else {
 				return locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
 			}
 		}
 		@Override
 		public Location getLastPushedFix() throws RemoteException {
 			return last_pushed_fix;		
 		}
 		@Override
 		public void refreshRSS() throws RemoteException {
 			updateRSS();
 		}
 		@Override 
 		public void pushFix() throws RemoteException {
 			pushQueue();
 		}
     };
 
 }
