 package org.bombusim.lime;
 
 import java.util.ArrayList;
 
 import org.bombusim.lime.activity.Smilify;
 import org.bombusim.lime.data.AccountsFactory;
 import org.bombusim.lime.data.ChatFactory;
 import org.bombusim.lime.data.Roster;
 import org.bombusim.lime.data.VcardResolver;
 import org.bombusim.lime.logger.LoggerData;
 import org.bombusim.lime.service.XmppServiceBinding;
 import org.bombusim.xmpp.XmppAccount;
 
 import android.app.Application;
 import android.content.pm.PackageInfo;
 import android.content.pm.PackageManager.NameNotFoundException;
 import android.content.res.Configuration;
 
 public class Lime extends Application {
 	private static Lime instance;
 	
 	public static Lime getInstance() { return instance; }
 	
 	public int avatarSize;
 	
 	//TODO: free memory when activity destroyed (avatars)
 	private Roster roster;
 	
 	private LoggerData log;
 
 	private ChatFactory chatFactory;
 	
 	private Smilify smilify;
 	
 	public Preferences prefs;
 	
 	public ArrayList<XmppAccount> accounts;
 	
 	public Roster getRoster() { return roster; } 
 	
 	public LoggerData getLog() { return log; }
 	
 	//temporary
 	public VcardResolver vcardResolver;
 	
 	//temporary
 	public XmppServiceBinding sb;
 	
 	@Override
 	public final void onCreate() {
 		super.onCreate();
 		
 		instance = this;
 		
		prefs = new Preferences(getApplicationContext());
 		
 		accounts = AccountsFactory.loadAccounts(getApplicationContext());
 		
 		log = new LoggerData();
 		
 		vcardResolver = new VcardResolver(this);
 
 		roster=new Roster(accounts.get(0).userJid);
 		
 		avatarSize = getResources().getDimensionPixelSize(R.dimen.avatarSize);
 		
 	}
 
 	@Override
 	public final void onTerminate() {
 		// TODO Auto-generated method stub
 		super.onTerminate();
 	}
 
 	//TODO: clean memory if inactive
 	
 	@Override
 	public final void onLowMemory() {
 		smilify = null;
 		//TODO: cleanup log
 		
 		//drop chats (not critical since they are stored in db)
 		chatFactory = null;
 		
 		//drop cached avatars
 		roster.dropCachedAvatars();
 		super.onLowMemory();
 	}
 	
 	
 	@Override
 	public final void onConfigurationChanged(Configuration newConfig) {
 		// TODO Auto-generated method stub
 		super.onConfigurationChanged(newConfig);
 	}
 
 	private String version;
 	
 	public String getVersion() {
 		if (version==null) {
 			try {
 				PackageInfo pinfo = getPackageManager().getPackageInfo(getPackageName(), 0);
 
 				version = pinfo.versionName + " ("+ pinfo.versionCode + ")";
 			} catch (NameNotFoundException e) {
 				version = "unknown";
 			}
 			
 		}
 		return version;
 	}
 
 	public String getOsId() {
 		StringBuilder sb = new StringBuilder();
 		sb.append(android.os.Build.MANUFACTURER).append(' ');
 		sb.append(android.os.Build.MODEL).append(" / Android");
 		sb.append(" sdk=").append(android.os.Build.VERSION.SDK);
 		sb.append(' ').append(android.os.Build.VERSION.INCREMENTAL);
 		
 		return sb.toString();
 	}
 
 	public ChatFactory getChatFactory() {
 		if (chatFactory == null) chatFactory = new ChatFactory();
 		return chatFactory;
 	}
 
 	public NotificationMgr notificationMgr() {
 		// TODO Auto-generated method stub
 		return new NotificationMgr(this);
 	}
 
 	public Smilify getSmilify() {
 		if (smilify == null) {
 			smilify = new Smilify();
 		}
 		return smilify;
 	}
 }
