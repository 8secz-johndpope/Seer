 /**
  * The MIT License (MIT)
  * Copyright (c) 2012 David Carver
  * Permission is hereby granted, free of charge, to any person obtaining
  * a copy of this software and associated documentation files (the
  * "Software"), to deal in the Software without restriction, including
  * without limitation the rights to use, copy, modify, merge, publish,
  * distribute, sublicense, and/or sell copies of the Software, and to
  * permit persons to whom the Software is furnished to do so, subject to
  * the following conditions:
  * 
  * The above copyright notice and this permission notice shall be included
  * in all copies or substantial portions of the Software.
  * 
  * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS
  * OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
  * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS
  * OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
  * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF
  * OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
  * SOFTWARE.
  */
 
 package us.nineworlds.serenity;
 
 import java.util.List;
 
 import us.nineworlds.serenity.core.ServerConfig;
 import us.nineworlds.serenity.core.model.Server;
 import us.nineworlds.serenity.core.model.impl.GDMServer;
 import us.nineworlds.serenity.core.services.GDMService;
 import us.nineworlds.serenity.ui.activity.SerenityActivity;
 import us.nineworlds.serenity.ui.preferences.SerenityPreferenceActivity;
 import android.net.Uri;
 import android.os.Bundle;
 import android.os.Handler;
 import android.os.IBinder;
 import android.os.Message;
 import android.preference.PreferenceManager;
 import android.app.Activity;
 import android.app.AlertDialog;
 import android.app.Notification;
 import android.app.NotificationManager;
 import android.app.PendingIntent;
 import android.content.BroadcastReceiver;
 import android.content.ComponentName;
 import android.content.Context;
 import android.content.DialogInterface;
 import android.content.Intent;
 import android.content.IntentFilter;
 import android.content.ServiceConnection;
 import android.content.SharedPreferences;
 import android.content.SharedPreferences.Editor;
 import android.support.v4.content.LocalBroadcastManager;
 import android.util.Log;
 import android.view.Menu;
 import android.view.MenuItem;
 import android.view.View;
 import android.widget.Gallery;
 import android.widget.Toast;
 
 import us.nineworlds.serenity.R;
 
 import com.castillo.dd.DSInterface;
 import com.castillo.dd.Download;
 import com.castillo.dd.DownloadService;
 import com.castillo.dd.PendingDownload;
 import com.google.analytics.tracking.android.EasyTracker;
 import com.nostra13.universalimageloader.core.ImageLoader;
 
 public class MainActivity extends SerenityActivity {
 
 	private Gallery mainGallery;
 	private View mainGalleryBackgroundView;
 	private SharedPreferences preferences;
 	public static int MAIN_MENU_PREFERENCE_RESULT_CODE = 100;
 	public static int BROWSER_RESULT_CODE = 200;
 	private boolean restarted_state = false;
 
 	public final int ABOUT = 1;
 	public final int CLEAR_CACHE = 2;
 	public final int TUTORIAL = 3;
 
 	private static int downloadIndex;
 	private static Activity mainContext;
 
 	private static NotificationManager notificationManager;
 
 	private static DSInterface dsInterface;
 
 	public static DSInterface getDsInterface() {
 		return dsInterface;
 	}
 
 	private ServiceConnection downloadService = new ServiceConnection() {
 		@Override
 		public void onServiceConnected(ComponentName className, IBinder service) {
 			dsInterface = DSInterface.Stub.asInterface(service);
 		}
 
 		@Override
 		public void onServiceDisconnected(ComponentName className) {
 			dsInterface = null;
 		}
 	};
 
 	private static boolean downloadsCancelled = false;
 
 	@Override
 	public void onCreate(Bundle savedInstanceState) {
 		super.onCreate(savedInstanceState);
 
 		mainContext = this;
 
 		setContentView(R.layout.activity_plex_app_main);
 		mainGalleryBackgroundView = findViewById(R.id.mainGalleryBackground);
 
 		mainGallery = (Gallery) findViewById(R.id.mainGalleryMenu);
 		preferences = PreferenceManager.getDefaultSharedPreferences(this);
 		if (preferences != null) {
 			ServerConfig config = (ServerConfig) ServerConfig.getInstance();
 			if (config != null) {
 				preferences
 						.registerOnSharedPreferenceChangeListener(((ServerConfig) ServerConfig
 								.getInstance()).getServerConfigChangeListener());
 			}
 		}
 
 		boolean googletv = SerenityApplication.isGoogleTV(this);
 		if (!googletv) {
 			SharedPreferences.Editor editor = preferences.edit();
 			editor.putBoolean("external_player", true);
 			editor.commit();
 		}
 
 		getApplicationContext().bindService(
 				new Intent(this, DownloadService.class), downloadService,
 				Context.BIND_AUTO_CREATE);
 
 		mHandler.sendMessage(mHandler
 				.obtainMessage(SerenityApplication.PROGRESS));
 
 		String svcName = Context.NOTIFICATION_SERVICE;
 		notificationManager = (NotificationManager) getSystemService(svcName);
 	}
 
 	@Override
 	public boolean onCreateOptionsMenu(Menu menu) {
 		// Inflate the menu; this adds items to the action bar if it is present.
 		getMenuInflater().inflate(R.menu.activity_plex_app_main, menu);
 		menu.add(0, ABOUT, 0, R.string.options_main_about);
 		menu.add(0, CLEAR_CACHE, 0, R.string.options_main_clear_image_cache);
 		menu.add(0, TUTORIAL, 0, R.string.tutorial);
 		return true;
 	}
 
 	@Override
 	public boolean onOptionsItemSelected(MenuItem item) {
 
 		switch (item.getItemId()) {
 		case CLEAR_CACHE:
 			createClearCacheDialog();
 			break;
 		case ABOUT:
 			AboutDialog about = new AboutDialog(this);
 			about.setTitle(R.string.about_title_serenity_for_google_tv);
 			about.show();
 			break;
 		case TUTORIAL:
 			Intent youTubei = new Intent(Intent.ACTION_VIEW,
 					Uri.parse("http://www.youtube.com/watch?v=_yKc8ymXerg"));
 			startActivity(youTubei);
 			break;
 		default:
 			Intent i = new Intent(MainActivity.this,
 					SerenityPreferenceActivity.class);
 			startActivityForResult(i, 0);
 			break;
 		}
 
 		return true;
 	}
 
 	protected void createClearCacheDialog() {
 		AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this,
 				android.R.style.Theme_Holo_Dialog);
 
 		alertDialogBuilder.setTitle(R.string.options_main_clear_image_cache);
 		alertDialogBuilder
 				.setMessage(R.string.option_clear_the_image_cache_)
 				.setCancelable(true)
 				.setPositiveButton(R.string.clear,
 						new DialogInterface.OnClickListener() {
 
 							@Override
 							public void onClick(DialogInterface dialog,
 									int which) {
 
 								ImageLoader imageLoader = SerenityApplication
 										.getImageLoader();
 								imageLoader.clearDiscCache();
 								imageLoader.clearMemoryCache();
 							}
 						})
 				.setNegativeButton(R.string.cancel,
 						new DialogInterface.OnClickListener() {
 
 							@Override
 							public void onClick(DialogInterface dialog,
 									int which) {
 
 							}
 						});
 
 		alertDialogBuilder.create();
 		alertDialogBuilder.show();
 
 	}
 
 	@Override
 	protected void onResume() {
 		super.onResume();
 		IntentFilter filters = new IntentFilter();
 		filters.addAction(GDMService.MSG_RECEIVED);
 		filters.addAction(GDMService.SOCKET_CLOSED);
 		LocalBroadcastManager.getInstance(this).registerReceiver(gdmReciver,
 				filters);
 
 		// Start the auto-configuration service
 		Intent GDMService = new Intent(this, GDMService.class);
 		startService(GDMService);
 	}
 
 	@Override
 	protected void onStart() {
 		super.onStart();
 		EasyTracker.getInstance().activityStart(this);
 		
 		autoConfigureHandler.postDelayed(new AutoConfigureHandlerRunnable(), 2500);
 		if (restarted_state == false) {
 			setupGallery();
 		}
 		restarted_state = false;
 	}
 
 	@Override
 	protected void onStop() {
 		super.onStop();
 		EasyTracker.getInstance().activityStop(this);
 	}
 
 	@Override
 	protected void onRestart() {
 		restarted_state = true;
 		super.onRestart();
 
 	}
 
 	@Override
 	protected void onDestroy() {
 		super.onDestroy();
 		mHandler.removeMessages(SerenityApplication.PROGRESS);
		getApplicationContext().unregisterReceiver(gdmReciver);
 
 		getApplicationContext().unbindService(downloadService);
 	}
 
 	/**
 	 * Refresh the screen after coming back from the preferences screen.
 	 */
 	@Override
 	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
 		if (resultCode == MAIN_MENU_PREFERENCE_RESULT_CODE) {
 			setupGallery();
 		}
 
 	}
 
 	private void setupGallery() {
 		mainGallery.setAdapter(new MainMenuTextViewAdapter(this,
 				mainGalleryBackgroundView));
 		mainGallery
 				.setOnItemSelectedListener(new GalleryOnItemSelectedListener(
 						mainGalleryBackgroundView));
 		mainGallery
 				.setOnItemClickListener(new GalleryOnItemClickListener(this));
 		mainGallery.setCallbackDuringFling(false);
 	}
 
 	private static class DownloadHandler extends Handler {
 		@Override
 		public void handleMessage(Message msg) {
 			if ((msg.what == SerenityApplication.PROGRESS)
 					&& (!downloadsCancelled)) {
 				List<PendingDownload> pendingDownloads = SerenityApplication
 						.getPendingDownloads();
 				for (int i = 0; i < pendingDownloads.size(); i++) {
 					if (i == downloadIndex) {
 						try {
 							int status = dsInterface.getDownloadStatus(i);
 							pendingDownloads.get(i).setStatus(status);
 							if (status == Download.START) {
 								dsInterface.downloadFile(i);
 								notification(pendingDownloads.get(i)
 										.getFilename() + " has started.",
 										"Downloading "
 												+ pendingDownloads.get(i)
 														.getFilename());
 								pendingDownloads.get(i).setLaunchTime(
 										dsInterface.getDownloadLaunchTime(i));
 
 							} else if (status == Download.COMPLETE) {
 								Toast.makeText(
 										mainContext,
 										pendingDownloads.get(i).getFilename()
 												+ " has completed.",
 										Toast.LENGTH_LONG).show();
 
 								downloadIndex++;
 								if (downloadIndex >= pendingDownloads.size()
 										|| pendingDownloads.size() == 0) {
 									notificationManager.cancel(1);
 								}
 							}
 							if (status != Download.COMPLETE) {
 								pendingDownloads.get(i).setProgress(
 										dsInterface.getDownloadProgress(i));
 								pendingDownloads.get(i).setEllapsedTime(
 										dsInterface.getDownloadEllapsedTime(i));
 								pendingDownloads
 										.get(i)
 										.setRemainingTime(
 												dsInterface
 														.getDownloadRemainingTime(i));
 								pendingDownloads.get(i).setSpeed(
 										dsInterface.getDownloadSpeed(i));
 							} else {
 								pendingDownloads.get(i).setProgress(100);
 							}
 						} catch (Exception e) {
 							Log.e(getClass().getName(),
 									Log.getStackTraceString(e));
 						}
 					}
 				}
 				sendMessageDelayed(obtainMessage(SerenityApplication.PROGRESS),
 						50);
 			}
 		}
 
 		protected void notification(String tickerText, String expandedText) {
 			int icon = R.drawable.serenity_bonsai_logo;
 			long when = System.currentTimeMillis();
 			Notification notification = new Notification(icon, tickerText, when);
 			String expandedTitle = "Serenity Download";
 			Intent intent = new Intent(mainContext, MainActivity.class);
 			PendingIntent launchIntent = PendingIntent.getActivity(mainContext, 0,
 					intent, 0);
 			notification.setLatestEventInfo(mainContext, expandedTitle,
 					expandedText, launchIntent);
 			int notificationRef = 1;
 			notificationManager.notify(notificationRef, notification);
 		}
 		
 	}
 	
 	protected Handler mHandler = new DownloadHandler();
 	protected Handler autoConfigureHandler = new Handler();
 	
 	private BroadcastReceiver gdmReciver = new GDMReceiver(autoConfigureHandler);
 
 	private class GDMReceiver extends BroadcastReceiver {
 		
 		Handler handler = null;
 		
 		/**
 		 * 
 		 */
 		public GDMReceiver(Handler h) {
 			handler = h;
 		}
 		
 		@Override
 		public void onReceive(Context context, Intent intent) {
 
 			if (intent.getAction().equals(GDMService.MSG_RECEIVED)) {
 				String message = intent.getStringExtra("data").trim();
 				String ipAddress = intent.getStringExtra("ipaddress").substring(1);
 				Server server = new GDMServer();
 				
 				int namePos = message.indexOf("Name: ");
 				namePos += 6;
 				int crPos = message.indexOf("\r", namePos);
 				String serverName = message.substring(namePos, crPos);
 				
 				server.setServerName(serverName);
 				server.setIPAddress(ipAddress);
 				if (!SerenityApplication.getPlexMediaServers().containsKey(serverName)) {
 					SerenityApplication.getPlexMediaServers().put(serverName,
 							server);
 					Log.d(getClass().getName(), "Adding " + serverName);
 				} else {
 					Log.d(getClass().getName(), serverName + " already added.");
 				}
 			} else if (intent.getAction().equals(GDMService.SOCKET_CLOSED)) {
 				Log.i("GDMService", "Finished Searching");
 			}
 		}
 	}
 	
 	private class AutoConfigureHandlerRunnable implements Runnable {
 		
 		/* (non-Javadoc)
 		 * @see java.lang.Runnable#run()
 		 */
 		@Override
 		public void run() {
			if (SerenityApplication.getPlexMediaServers().isEmpty()) {
				Toast.makeText(MainActivity.this, "No servers discovered. Use settings to configure manually.", Toast.LENGTH_LONG).show();
				return;
			}
 			Server server = SerenityApplication.getPlexMediaServers().values().iterator().next();
 			String ipAddress = preferences.getString("server", "");
 			if ("".equals(ipAddress)) {
 				Editor edit = preferences.edit();
 				edit.putString("server", server.getIPAddress());
 				edit.apply();
 				Toast.makeText(mainContext, getResources().getText(R.string.auto_configuring_server_using_) + server.getServerName(), Toast.LENGTH_LONG).show();
 				mainContext.recreate();
 			}
 		}
 	}
 
 }
