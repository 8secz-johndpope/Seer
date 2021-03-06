 package ru.sawim;
 
 import android.app.ActivityManager;
 import android.app.Application;
 import android.app.NotificationManager;
 import android.content.Context;
 import android.content.Intent;
 import android.content.SharedPreferences;
 import android.content.pm.PackageInfo;
 import android.content.pm.PackageManager;
 import android.os.Environment;
 import android.os.Message;
 import android.preference.PreferenceManager;
 import android.support.v4.app.Fragment;
 import android.support.v7.app.ActionBar;
 import android.support.v7.app.ActionBarActivity;
 import android.util.Log;
 import android.view.Surface;
 import android.view.WindowManager;
 import com.google.android.gcm.GCMRegistrar;
 import org.microemu.util.AndroidRecordStoreManager;
 import ru.sawim.service.GCMIntentService;
 import ru.sawim.service.SawimService;
 import ru.sawim.service.SawimServiceConnection;
 import ru.sawim.text.TextFormatter;
 import sawim.Options;
 import sawim.Updater;
 import sawim.chat.ChatHistory;
 import sawim.comm.Util;
 import sawim.modules.*;
 import sawim.roster.RosterHelper;
 import sawim.search.Search;
 
 import java.io.InputStream;
 import java.util.ArrayList;
 import java.util.List;
 
 /**
  * Created with IntelliJ IDEA.
  * User: Gerc
  * Date: 12.06.13
  * Time: 17:43
  * To change this template use File | Settings | File Templates.
  */
 public class SawimApplication extends Application {
 
     public static final String LOG_TAG = SawimApplication.class.getSimpleName();
 
     public static String NAME;
     public static String VERSION;
     public static final String PHONE = "Android/" + android.os.Build.MODEL
             + "/" + android.os.Build.VERSION.RELEASE;
     public static final String DEFAULT_SERVER = "jabber.ru";
     public static final String PATH_AVATARS = Environment.getExternalStorageDirectory().getAbsolutePath() + "/sawimne/avatars/";
 
     public static boolean returnFromAcc = false;
     private static ActionBarActivity currentActivity;
     private static ActionBar actionBar;
     private boolean paused = true;
     private static int fontSize;
     public static boolean showStatusLine;
     public static int sortType;
     public static boolean hideIconsClient;
     public static int autoAbsenceTime;
 
     public static ArrayList<Fragment> fragmentsStack = new ArrayList<Fragment>();
     public static SawimApplication instance;
     public AndroidRecordStoreManager recordStoreManager;
     private final SawimServiceConnection serviceConnection = new SawimServiceConnection();
     private final NetworkStateReceiver networkStateReceiver = new NetworkStateReceiver();
     public boolean isBindService = false;
 
     public static SawimApplication getInstance() {
         return instance;
     }
 
     public static Context getContext() {
         return instance.getApplicationContext();
     }
 
     @Override
     public void onCreate() {
         instance = this;
         NAME = getString(R.string.app_name);
         VERSION = getVersion();
         Thread.setDefaultUncaughtExceptionHandler(ExceptionHandler.inContext(getContext()));
         super.onCreate();
         recordStoreManager = new AndroidRecordStoreManager(getContext());
         startService();
         networkStateReceiver.updateNetworkState(this);
 
         startApp();
         TextFormatter.init();
     }
 
     public void initGCM() {
         try {
             GCMRegistrar.checkDevice(this);
             GCMRegistrar.checkManifest(this);
            final String regId = GCMRegistrar.getRegistrationId(this);
            SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
            String prefRegId = sp.getString("gcm_regid", null);
            if (regId.length() == 0 || !regId.equals(prefRegId)) {
                GCMRegistrar.register(this, GCMIntentService.CLIENT_ID);
            }
         } catch (Exception e) {
             Log.e(LOG_TAG, e.toString());
         }
     }
 
     private void startService() {
         if (!isRunService()) {
             startService(new Intent(this, SawimService.class));
         }
         if (!isBindService) {
             isBindService = true;
             registerReceiver(networkStateReceiver, networkStateReceiver.getFilter());
             bindService(new Intent(this, SawimService.class), serviceConnection, BIND_AUTO_CREATE);
         }
     }
 
     public void stopService() {
         if (isBindService) {
             isBindService = false;
             unbindService(serviceConnection);
             unregisterReceiver(networkStateReceiver);
         }
         if (isRunService()) {
             stopService(new Intent(this, SawimService.class));
         }
         ((NotificationManager) getSystemService(NOTIFICATION_SERVICE)).cancelAll();
     }
 
     public void updateConnectionState() {
         serviceConnection.send(Message.obtain(null, SawimService.UPDATE_CONNECTION_STATUS));
     }
 
     public void updateAppIcon() {
         serviceConnection.send(Message.obtain(null, SawimService.UPDATE_APP_ICON));
     }
 
     public void setStatus() {
         serviceConnection.send(Message.obtain(null, SawimService.SET_STATUS));
     }
 
     public void sendNotify(String title, String text) {
         String[] parameters = new String[]{title, text};
         serviceConnection.send(Message.obtain(null, SawimService.SEND_NOTIFY, parameters));
     }
 
     public boolean isNetworkAvailable() {
         return networkStateReceiver.isNetworkAvailable();
     }
 
     private String getVersion() {
         String version = "";
         try {
             PackageInfo pinfo = getPackageManager().getPackageInfo(getPackageName(), 0);
             version = pinfo.versionName;
         } catch (PackageManager.NameNotFoundException e) {
             version = "unknown";
         }
         return version;
     }
 
     private void startApp() {
         instance.paused = false;
         ru.sawim.config.HomeDirectory.init();
         Options.loadOptions();
         new ru.sawim.config.Options().load();
         Scheme.load();
         Scheme.setColorScheme(Options.getInt(Options.OPTION_COLOR_SCHEME));
         updateOptions();
         Updater.startUIUpdater();
         try {
             Notify.getSound().initSounds();
             gc();
             Emotions.instance.load();
             Answerer.getInstance().load();
             gc();
 
             Options.loadAccounts();
             RosterHelper.getInstance().initAccounts();
             RosterHelper.getInstance().loadAccounts();
             sawim.modules.tracking.Tracking.loadTrackingFromRMS();
         } catch (Exception e) {
             DebugLog.panic("init", e);
             DebugLog.instance.activate();
         }
         DebugLog.startTests();
     }
 
     public static void updateOptions() {
         SawimResources.initIcons();
         fontSize = Math.max(Options.getInt(Options.OPTION_FONT_SCHEME), 16);
         showStatusLine = Options.getBoolean(Options.OPTION_SHOW_STATUS_LINE);
         hideIconsClient = Options.getBoolean(Options.OPTION_HIDE_ICONS_CLIENTS);
         sortType = Options.getInt(Options.OPTION_CL_SORT_BY);
         autoAbsenceTime = Options.getInt(Options.OPTION_AA_TIME) * 60;
     }
 
     public static boolean isManyPane() {
         int rotation = ((WindowManager) SawimApplication.getContext()
                 .getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay().getRotation();
         return !(rotation == Surface.ROTATION_0 || rotation == Surface.ROTATION_180) && isTablet();
     }
 
     public static boolean isTablet() {
         return getInstance().getResources().getBoolean(R.bool.is_tablet);
     }
 
     public void quit(boolean isForceClose) {
         new Thread(new Runnable() {
             @Override
             public void run() {
                 RosterHelper.getInstance().safeSave();
                 ChatHistory.instance.saveUnreadMessages();
                 AutoAbsence.getInstance().online();
             }
         });
     }
 
     public static long getCurrentGmtTime() {
         return System.currentTimeMillis() / 1000
                 + Options.getInt(Options.OPTION_LOCAL_OFFSET) * 3600;
     }
 
     public static void openUrl(String url) {
         Search search = RosterHelper.getInstance().getCurrentProtocol().getSearchForm();
         search.show(Util.getUrlWithoutProtocol(url), true);
     }
 
     public static java.io.InputStream getResourceAsStream(String name) {
         InputStream in = sawim.modules.fs.FileSystem.openSawimFile(name);
         if (null == in) {
             try {
                 in = SawimApplication.getInstance().getAssets().open(name.substring(1));
             } catch (Exception ignored) {
             }
         }
         return in;
     }
 
     private boolean isRunService() {
         ActivityManager manager = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
         List<ActivityManager.RunningServiceInfo> runningServiceInfoList = manager.getRunningServices(Integer.MAX_VALUE);
         if (runningServiceInfoList != null) {
             for (ActivityManager.RunningServiceInfo service : runningServiceInfoList) {
                 if (SawimService.class.getCanonicalName().equals(service.service.getClassName())) {
                     return true;
                 }
             }
         }
         return false;
     }
 
     public static boolean isPaused() {
         return instance.paused;
     }
 
     public static void maximize() {
         AutoAbsence.getInstance().online();
         instance.paused = false;
     }
 
     public static void minimize() {
         sawim.modules.AutoAbsence.getInstance().userActivity();
         instance.paused = true;
     }
 
     public static void gc() {
         System.gc();
         try {
             Thread.sleep(50);
         } catch (Exception e) {
         }
     }
 
     public static int getFontSize() {
         return fontSize;
     }
 
     private static OnConfigurationChanged configurationChanged;
 
     public OnConfigurationChanged getConfigurationChanged() {
         return configurationChanged;
     }
 
     public void setConfigurationChanged(OnConfigurationChanged cC) {
         configurationChanged = null;
         configurationChanged = cC;
     }
 
     public static ActionBar getActionBar() {
         return actionBar;
     }
 
     public static void setActionBar(ActionBar actionBar) {
         SawimApplication.actionBar = null;
         SawimApplication.actionBar = actionBar;
     }
 
     public static ActionBarActivity getCurrentActivity() {
         return currentActivity;
     }
 
     public static void setCurrentActivity(ActionBarActivity a) {
         SawimApplication.currentActivity = null;
         SawimApplication.currentActivity = a;
     }
 
     public interface OnConfigurationChanged {
         public void onConfigurationChanged();
     }
}
