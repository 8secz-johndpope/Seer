 package biz.bokhorst.xprivacy;
 
 import java.lang.reflect.Constructor;
 import java.lang.reflect.Method;
 import java.lang.reflect.Modifier;
 import java.util.HashSet;
 import java.util.LinkedHashMap;
 import java.util.Map;
 import java.util.Set;
 
 import android.os.Build;
 import android.util.Log;
 
 import de.robv.android.xposed.IXposedHookLoadPackage;
 import de.robv.android.xposed.XposedBridge;
 import de.robv.android.xposed.XposedHelpers.ClassNotFoundError;
 import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;
 import de.robv.android.xposed.XC_MethodHook;
 import static de.robv.android.xposed.XposedHelpers.findClass;
 
 public class XPrivacy implements IXposedHookLoadPackage {
 
 	// This should correspond with perm_<name> in strings.xml
 	private static final String cPermissionBrowser = "browser";
 	private static final String cPermissionCalendar = "calendar";
 	private static final String cPermissionCalllog = "calllog";
 	private static final String cPermissionContacts = "contacts";
 	private static final String cPermissionID = "identification";
 	private static final String cPermissionLocation = "location";
 	private static final String cPermissionMessages = "messages";
 	private static final String cPermissionVoicemail = "voicemail";
 
 	public static Map<String, String[]> cPermissions = new LinkedHashMap<String, String[]>();
 
 	static {
 		cPermissions.put(cPermissionBrowser, new String[] { "READ_HISTORY_BOOKMARKS", "GLOBAL_SEARCH" });
 		cPermissions.put(cPermissionCalendar, new String[] { "READ_CALENDAR" });
 		cPermissions.put(cPermissionCalllog, new String[] { "READ_CALL_LOG" });
 		cPermissions.put(cPermissionContacts, new String[] { "READ_CONTACTS" });
 		cPermissions.put(cPermissionID, new String[] { "READ_PHONE_STATE" });
 		cPermissions.put(cPermissionLocation, new String[] { "ACCESS_FINE_LOCATION", "ACCESS_COARSE_LOCATION" });
 		cPermissions.put(cPermissionMessages, new String[] { "READ_SMS" });
 		cPermissions.put(cPermissionVoicemail, new String[] { "READ_WRITE_ALL_VOICEMAIL" });
 	}
 
 	public void handleLoadPackage(final LoadPackageParam lpparam) throws Throwable {
 		// Log load
 		XUtil.log(null, Log.INFO, String.format("load package=%s", lpparam.packageName));
 
 		// Skip hooking self
 		if (lpparam.packageName.equals(XPrivacy.class.getPackage()))
 			return;
 
 		// Check version
 		if (Build.VERSION.SDK_INT != 16)
 			XUtil.log(null, Log.WARN, String.format("Build version %d", Build.VERSION.SDK_INT));
 
 		// TODO: check if CyanogenMod
 
 		// Location manager
 		hook(new XLocationManager(cPermissionLocation), lpparam, "android.location.LocationManager",
 				"addGpsStatusListener", true);
 		hook(new XLocationManager(cPermissionLocation), lpparam, "android.location.LocationManager", "addNmeaListener",
 				true);
 		hook(new XLocationManager(cPermissionLocation), lpparam, "android.location.LocationManager",
 				"addProximityAlert", true);
 		hook(new XLocationManager(cPermissionLocation), lpparam, "android.location.LocationManager",
 				"getLastKnownLocation", true);
 		hook(new XLocationManager(cPermissionLocation), lpparam, "android.location.LocationManager",
 				"requestLocationUpdates", true);
 		hook(new XLocationManager(cPermissionLocation), lpparam, "android.location.LocationManager",
 				"requestSingleUpdate", true);
 		hook(new XLocationManager(cPermissionLocation), lpparam, "android.location.LocationManager",
 				"_requestLocationUpdates", false);
 
 		// Settings secure
 		hook(new XSettingsSecure(cPermissionID), lpparam, "android.provider.Settings.Secure", "getString", true);
 
 		// Telephony
 		hook(new XTelephonyManager(cPermissionID), lpparam, "android.telephony.TelephonyManager", "getDeviceId", true);
 		hook(new XTelephonyManager(cPermissionID), lpparam, "android.telephony.TelephonyManager", "getLine1Number",
 				true);
 		hook(new XTelephonyManager(cPermissionID), lpparam, "android.telephony.TelephonyManager", "getMsisdn", true);
 		hook(new XTelephonyManager(cPermissionID), lpparam, "android.telephony.TelephonyManager", "getSimSerialNumber",
 				true);
 		hook(new XTelephonyManager(cPermissionID), lpparam, "android.telephony.TelephonyManager", "getSubscriberId",
 				true);
 
		// Load calendar provider
 		if (lpparam.packageName.equals("com.android.browser.provider")) {
 			hook(new XContentProvider(cPermissionBrowser), lpparam, "com.android.browser.provider.BrowserProvider",
 					"query", true);
 			hook(new XContentProvider(cPermissionBrowser), lpparam, "com.android.browser.provider.BrowserProvider2",
 					"query", true);
 		}
 
 		// Load calendar provider
 		else if (lpparam.packageName.equals("com.android.providers.calendar"))
 			hook(new XContentProvider(cPermissionCalendar), lpparam,
 					"com.android.providers.calendar.CalendarProvider2", "query", true);
 
 		// Load contacts provider
 		else if (lpparam.packageName.equals("com.android.providers.contacts")) {
 			hook(new XContentProvider(cPermissionCalllog), lpparam, "com.android.providers.contacts.CallLogProvider",
 					"query", true);
 			hook(new XContentProvider(cPermissionContacts), lpparam,
 					"com.android.providers.contacts.ContactsProvider2", "query", true);
 			hook(new XContentProvider(cPermissionVoicemail), lpparam,
 					"com.android.providers.contacts.VoicemailContentProvider", "query", true);
 		}
 
 		// Load telephony provider
 		else if (lpparam.packageName.equals("com.android.providers.telephony")) {
 			hook(new XContentProvider(cPermissionMessages), lpparam, "com.android.providers.telephony.SmsProvider",
 					"query", true);
 			hook(new XContentProvider(cPermissionMessages), lpparam, "com.android.providers.telephony.MmsProvider",
 					"query", true);
 			hook(new XContentProvider(cPermissionMessages), lpparam, "com.android.providers.telephony.MmsSmsProvider",
 					"query", true);
 			// com.android.providers.telephony.TelephonyProvider
 		}
 
 		// Load settings
 		else if (lpparam.packageName.equals("com.android.settings"))
 			hook(new XInstalledAppDetails(null), lpparam, "com.android.settings.applications.InstalledAppDetails",
 					"refreshUi", false);
 	}
 
 	private void hook(final XHook hook, final LoadPackageParam lpparam, String className, String methodName,
 			boolean visible) {
 		try {
 			// Create hook
 			XC_MethodHook methodHook = new XC_MethodHook() {
 				@Override
 				protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
 					try {
 						hook.before(param);
 					} catch (Throwable ex) {
 						XUtil.bug(null, ex);
 						throw ex;
 					}
 				}
 
 				@Override
 				protected void afterHookedMethod(MethodHookParam param) throws Throwable {
 					try {
 						hook.after(param);
 					} catch (Throwable ex) {
 						XUtil.bug(null, ex);
 						throw ex;
 					}
 				}
 			};
 
 			// Add hook
 			Set<XC_MethodHook.Unhook> hookSet = new HashSet<XC_MethodHook.Unhook>();
 			Class<?> hookClass = findClass(className, lpparam.classLoader);
 			if (methodName == null) {
 				for (Constructor<?> constructor : hookClass.getDeclaredConstructors())
 					if (Modifier.isPublic(constructor.getModifiers()) ? visible : !visible)
 						hookSet.add(XposedBridge.hookMethod(constructor, methodHook));
 			} else {
 				for (Method method : hookClass.getDeclaredMethods())
 					if (method.getName().equals(methodName)
 							&& (Modifier.isPublic(method.getModifiers()) ? visible : !visible))
 						hookSet.add(XposedBridge.hookMethod(method, methodHook));
 			}
 
 			// Log
 			for (XC_MethodHook.Unhook unhook : hookSet) {
 				XUtil.log(hook, Log.INFO, String.format("hooked %s in %s (%d)", unhook.getHookedMethod().getName(),
 						lpparam.packageName, hookSet.size()));
 				break;
 			}
 		} catch (ClassNotFoundError ignored) {
 			XUtil.log(hook, Log.ERROR, "class not found");
 		} catch (NoSuchMethodError ignored) {
 			XUtil.log(hook, Log.ERROR, "method not found");
 		} catch (Throwable ex) {
 			XUtil.bug(null, ex);
 		}
 	}
 }
