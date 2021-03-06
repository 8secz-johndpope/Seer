 package com.nolanlawson.apptracker;
 
 import java.io.BufferedReader;
 import java.io.IOException;
 import java.io.InputStreamReader;
 import java.util.regex.Matcher;
 import java.util.regex.Pattern;
 
 import android.app.IntentService;
import android.content.BroadcastReceiver;
import android.content.Context;
 import android.content.Intent;
import android.content.IntentFilter;
 
 import com.nolanlawson.apptracker.db.AppHistoryDbHelper;
 import com.nolanlawson.apptracker.util.FlagUtil;
 import com.nolanlawson.apptracker.util.UtilLogger;
 
 /**
  * Reads logs. Named "AppTrackerService" in order to obfuscate, so the user
  * won't get freaked out if they see e.g. "LogReaderService" running on their
  * phone.
  * 
  * @author nolan
  * 
  */
 public class AppTrackerService extends IntentService {
 
 	private AppHistoryDbHelper dbHelper;
 
 	
 	private static UtilLogger log = new UtilLogger(AppTrackerService.class);
 
 	private static Pattern launcherPattern = Pattern
 			.compile("\\bcmp=([^/]++)/(\\.?\\S++)\\s");
 	
 	private static Pattern flagPattern = Pattern.compile("\\bflg=0x(\\d+)\\b");
 
 
 
 	public AppTrackerService() {
 		super("AppTrackerService");
 	}
 	
 	
 
 	@Override
 	public void onCreate() {
 		super.onCreate();
 
 		dbHelper = new AppHistoryDbHelper(getApplicationContext());
 		
		// update all widgets when the screen wakes up again - that's the case where
		// the user unlocks their screen and sees the home screen, so we need
		// instant updates
		registerReceiver(new BroadcastReceiver() {
			
			@Override
			public void onReceive(Context context, Intent intent) {
				log.d("Screen waking up; updating wigets");
				WidgetUpdater.updateWidget(context, dbHelper);
				
			}
		}, new IntentFilter(Intent.ACTION_SCREEN_ON));
 	}
 	
 	
 
 
 
 	@Override
 	public void onDestroy() {
 		super.onDestroy();
 		dbHelper.close();
 	}
 
 
 
 	protected void onHandleIntent(Intent intent) {
 
 		log.d("Starting up LogReader now");
 
 		Process mLogcatProc = null;
 		BufferedReader reader = null;
 		
 		try {
 			// logcat -d AndroidRuntime:E ActivityManager:V *:S
 			mLogcatProc = Runtime.getRuntime().exec(
 					new String[] { "logcat",
 							"AndroidRuntime:E ActivityManager:V *:S" });
 
 			reader = new BufferedReader(new InputStreamReader(mLogcatProc
 					.getInputStream()));
 
 			String line;
 
 			while ((line = reader.readLine()) != null) {
 
 				if (line.contains("Starting activity") 
 						&& line.contains("act=android.intent.action.MAIN")
 						&& !line.contains("(has extras)")) { // if it has extras, we can't call it (e.g. com.android.phone)
 					log.d("log is %s", line);
 					
 					if (!line.contains("android.intent.category.HOME")) { // ignore home apps
 	
 						Matcher flagMatcher = flagPattern.matcher(line);
 						
 						if (flagMatcher.find()) {
 							String flagsAsString = flagMatcher.group(1);
 							int flags = Integer.parseInt(flagsAsString, 16);
 							
 							log.d("flags are: 0x%s",flagsAsString);
 							
 							// intents have to be "new tasks" and they have to have been launched by the user 
 							// (not like e.g. the incoming call screen)
 							if (FlagUtil.hasFlag(flags, Intent.FLAG_ACTIVITY_NEW_TASK)
 									&& !FlagUtil.hasFlag(flags, Intent.FLAG_ACTIVITY_NO_USER_ACTION)) {
 								
 								Matcher launcherMatcher = launcherPattern.matcher(line);
 	
 								if (launcherMatcher.find()) {
 									String packageName = launcherMatcher.group(1);
 									String process = launcherMatcher.group(2);
 									
 									log.d("package name is: " + packageName);
 									log.d("process name is: " + process);
 									synchronized (AppHistoryDbHelper.class) {
 										dbHelper.incrementAndUpdate(packageName, process);
 									}
 								}				
 							}
 							
 						}
 					}
 					// update the widget no matter what the activity is
 					// especially if it's the home activity, this is important to do
 					// so that the widgets stay up-to-date (e.g. with time estimates)
 					WidgetUpdater.updateWidget(this, dbHelper);
 				}
 			}
 
 		}
 
 		catch (IOException e) {
 			log.e(e, "unexpected exception");
 		}
 
 		finally {
 			if (reader != null) {
 				try {
 					reader.close();
 				} catch (IOException e) {
 					log.e(e, "unexpected exception");
 				}
 			}
 
 			log.d("AppTrackerService died for some reason");
 
 		}
 	}
 
 }
