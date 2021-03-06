 package carnero.cgeo;
 
 import android.app.Activity;
 import android.content.Context;
 import android.content.Intent;
 import java.io.InputStreamReader;
 import java.io.InputStream;
 import java.io.OutputStream;
 import java.io.OutputStreamWriter;
 import java.io.BufferedReader;
 import java.io.IOException;
 import java.net.URL;
 import java.net.URLEncoder;
 import java.net.URLConnection;
 import java.net.HttpURLConnection;
 import java.util.Map;
 import java.util.Calendar;
 import java.util.Locale;
 import java.util.HashMap;
 import java.util.ArrayList;
 import java.util.regex.Pattern;
 import java.util.regex.Matcher;
 import java.text.DateFormat;
 import java.text.SimpleDateFormat;
 import android.util.Log;
 import android.content.SharedPreferences;
 import android.content.pm.PackageManager;
 import android.content.pm.ResolveInfo;
 import android.content.res.Resources;
 import android.graphics.Bitmap;
 import android.graphics.BitmapFactory;
 import android.net.Uri;
 import android.os.Handler;
 import android.os.Message;
 import android.text.Html;
 import android.text.Spannable;
 import android.text.style.StrikethroughSpan;
 import android.view.Display;
 import android.view.View;
 import android.view.WindowManager;
 import android.widget.ProgressBar;
 import android.widget.TextView;
 import com.google.android.apps.analytics.GoogleAnalyticsTracker;
 import java.io.ByteArrayOutputStream;
 import java.io.DataOutputStream;
 import java.io.File;
 import java.math.BigInteger;
 import java.net.URLDecoder;
 import java.security.MessageDigest;
 import java.security.cert.CertificateException;
 import java.security.cert.X509Certificate;
 import java.util.Date;
 import java.util.List;
 import java.util.Set;
 import java.util.zip.GZIPInputStream;
 import java.util.zip.Inflater;
 import java.util.zip.InflaterInputStream;
 import javax.crypto.Mac;
 import javax.crypto.spec.SecretKeySpec;
 import javax.net.ssl.HostnameVerifier;
 import javax.net.ssl.HttpsURLConnection;
 import javax.net.ssl.SSLContext;
 import javax.net.ssl.SSLSession;
 import javax.net.ssl.TrustManager;
 import javax.net.ssl.X509TrustManager;
 import org.json.JSONArray;
 import org.json.JSONObject;
 
 public class cgBase {
 
 	public static HashMap<String, String> cacheTypes = new HashMap<String, String>();
 	public static HashMap<String, String> cacheTypesInv = new HashMap<String, String>();
 	public static HashMap<String, String> cacheIDs = new HashMap<String, String>();
 	public static HashMap<String, String> cacheIDsChoices = new HashMap<String, String>();
 	public static HashMap<String, String> waypointTypes = new HashMap<String, String>();
 	public static HashMap<String, Integer> logTypes = new HashMap<String, Integer>();
 	public static HashMap<String, Integer> logTypes0 = new HashMap<String, Integer>();
 	public static HashMap<Integer, String> logTypes1 = new HashMap<Integer, String>();
 	public static HashMap<Integer, String> logTypes2 = new HashMap<Integer, String>();
 	public static HashMap<Integer, String> logTypesTrackable = new HashMap<Integer, String>();
 	public static HashMap<Integer, String> logTypesTrackableAction = new HashMap<Integer, String>();
 	public static HashMap<Integer, String> errorRetrieve = new HashMap<Integer, String>();
 	public static SimpleDateFormat dateIn = new SimpleDateFormat("MM/dd/yyyy");
 	public static SimpleDateFormat dateEvIn = new SimpleDateFormat("dd MMMMM yyyy"); // 28 March 2009
 	public static SimpleDateFormat dateTbIn1 = new SimpleDateFormat("EEEEE, dd MMMMM yyyy"); // Saturday, 28 March 2009
 	public static SimpleDateFormat dateTbIn2 = new SimpleDateFormat("EEEEE, MMMMM dd, yyyy"); // Saturday, March 28, 2009
 	public static SimpleDateFormat dateSqlIn = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"); // 2010-07-25 14:44:01
 	public static SimpleDateFormat dateGPXIn = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'"); // 2010-04-20T07:00:00Z
 	public static DateFormat dateOut = DateFormat.getDateInstance(DateFormat.LONG, Locale.getDefault());
 	public static DateFormat timeOut = DateFormat.getTimeInstance(DateFormat.SHORT, Locale.getDefault());
 	public static DateFormat dateOutShort = DateFormat.getDateInstance(DateFormat.SHORT, Locale.getDefault());
 	public static String ratingKey = "hbM9fjmrxy7z42LFD58BkKgPGdHscvCqNnw3ptO6lJ";
 	public static String xorKey = "groundspeak";
 	private Resources res = null;
 	private HashMap<String, String> cookies = new HashMap<String, String>();
 	private Pattern patternLoggedIn = null;
 	private final Pattern patternViewstate = Pattern.compile("id=\"__VIEWSTATE\"[^(value)]+value=\"([^\"]+)\"[^>]+>", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE);
 	private final Pattern patternViewstate1 = Pattern.compile("id=\"__VIEWSTATE1\"[^(value)]+value=\"([^\"]+)\"[^>]+>", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE);
 	private final Pattern patternLines = Pattern.compile("[\r\n\t ]+");
 	public static final double kmInMiles = 1 / 1.609344;
 	public static final double deg2rad = Math.PI / 180;
 	public static final double rad2deg = 180 / Math.PI;
 	public static final float erad = 6371.0f;
 	private cgeoapplication app = null;
 	private cgSettings settings = null;
 	private SharedPreferences prefs = null;
 	private String idBrowser = "Mozilla/5.0 (X11; U; Linux i686; en-US) AppleWebKit/533.4 (KHTML, like Gecko) Chrome/5.0.375.86 Safari/533.4";
 	final private static HashMap<String, Integer> gcIcons = new HashMap<String, Integer>();
 	final private static HashMap<String, Integer> wpIcons = new HashMap<String, Integer>();
 
 	public cgBase(cgeoapplication appIn, cgSettings settingsIn, SharedPreferences prefsIn) {
 		res = appIn.getBaseContext().getResources();
 
 		// cache types
 		cacheTypes.put("traditional cache", "traditional");
 		cacheTypes.put("multi-cache", "multi");
 		cacheTypes.put("unknown cache", "mystery");
 		cacheTypes.put("letterbox hybrid", "letterbox");
 		cacheTypes.put("event cache", "event");
 		cacheTypes.put("mega-event cache", "mega");
 		cacheTypes.put("earthcache", "earth");
 		cacheTypes.put("cache in trash out event", "cito");
 		cacheTypes.put("webcam cache", "webcam");
 		cacheTypes.put("virtual cache", "virtual");
 		cacheTypes.put("wherigo cache", "wherigo");
 		cacheTypes.put("lost & found", "lostfound");
 		cacheTypes.put("project ape cache", "ape");
 		cacheTypes.put("groundspeak gq", "gchq");
 		cacheTypes.put("gps cache exhibit", "gps");
 
 		// cache types inverted
 		cacheTypesInv.put("traditional", res.getString(R.string.traditional));
 		cacheTypesInv.put("multi", res.getString(R.string.multi));
 		cacheTypesInv.put("mystery", res.getString(R.string.mystery));
 		cacheTypesInv.put("letterbox", res.getString(R.string.letterbox));
 		cacheTypesInv.put("event", res.getString(R.string.event));
 		cacheTypesInv.put("mega", res.getString(R.string.mega));
 		cacheTypesInv.put("earth", res.getString(R.string.earth));
 		cacheTypesInv.put("cito", res.getString(R.string.cito));
 		cacheTypesInv.put("webcam", res.getString(R.string.webcam));
 		cacheTypesInv.put("virtual", res.getString(R.string.virtual));
 		cacheTypesInv.put("wherigo", res.getString(R.string.wherigo));
 		cacheTypesInv.put("lostfound", res.getString(R.string.lostfound));
 		cacheTypesInv.put("gchq", res.getString(R.string.gchq));
 		cacheTypesInv.put("gps", res.getString(R.string.gps));
 
 		// cache ids
 		cacheIDs.put("all", "9a79e6ce-3344-409c-bbe9-496530baf758"); // hard-coded also in cgSettings
 		cacheIDs.put("traditional", "32bc9333-5e52-4957-b0f6-5a2c8fc7b257");
 		cacheIDs.put("multi", "a5f6d0ad-d2f2-4011-8c14-940a9ebf3c74");
 		cacheIDs.put("mystery", "40861821-1835-4e11-b666-8d41064d03fe");
 		cacheIDs.put("letterbox", "4bdd8fb2-d7bc-453f-a9c5-968563b15d24");
 		cacheIDs.put("event", "69eb8534-b718-4b35-ae3c-a856a55b0874");
 		cacheIDs.put("mega-event", "69eb8535-b718-4b35-ae3c-a856a55b0874");
 		cacheIDs.put("earth", "c66f5cf3-9523-4549-b8dd-759cd2f18db8");
 		cacheIDs.put("cito", "57150806-bc1a-42d6-9cf0-538d171a2d22");
 		cacheIDs.put("webcam", "31d2ae3c-c358-4b5f-8dcd-2185bf472d3d");
 		cacheIDs.put("virtual", "294d4360-ac86-4c83-84dd-8113ef678d7e");
 		cacheIDs.put("wherigo", "0544fa55-772d-4e5c-96a9-36a51ebcf5c9");
 		cacheIDs.put("lostfound", "3ea6533d-bb52-42fe-b2d2-79a3424d4728");
 		cacheIDs.put("ape", "2555690d-b2bc-4b55-b5ac-0cb704c0b768");
 		cacheIDs.put("gchq", "416f2494-dc17-4b6a-9bab-1a29dd292d8c");
 		cacheIDs.put("gps", "72e69af2-7986-4990-afd9-bc16cbbb4ce3");
 
 		// cache choices
 		cacheIDsChoices.put(res.getString(R.string.all), cacheIDs.get("all"));
 		cacheIDsChoices.put(res.getString(R.string.traditional), cacheIDs.get("traditional"));
 		cacheIDsChoices.put(res.getString(R.string.multi), cacheIDs.get("multi"));
 		cacheIDsChoices.put(res.getString(R.string.mystery), cacheIDs.get("mystery"));
 		cacheIDsChoices.put(res.getString(R.string.letterbox), cacheIDs.get("letterbox"));
 		cacheIDsChoices.put(res.getString(R.string.event), cacheIDs.get("event"));
 		cacheIDsChoices.put(res.getString(R.string.mega), cacheIDs.get("mega"));
 		cacheIDsChoices.put(res.getString(R.string.earth), cacheIDs.get("earth"));
 		cacheIDsChoices.put(res.getString(R.string.cito), cacheIDs.get("cito"));
 		cacheIDsChoices.put(res.getString(R.string.webcam), cacheIDs.get("webcam"));
 		cacheIDsChoices.put(res.getString(R.string.virtual), cacheIDs.get("virtual"));
 		cacheIDsChoices.put(res.getString(R.string.wherigo), cacheIDs.get("whereigo"));
 		cacheIDsChoices.put(res.getString(R.string.lostfound), cacheIDs.get("lostfound"));
 		cacheIDsChoices.put(res.getString(R.string.ape), cacheIDs.get("ape"));
 		cacheIDsChoices.put(res.getString(R.string.gchq), cacheIDs.get("gchq"));
 		cacheIDsChoices.put(res.getString(R.string.gps), cacheIDs.get("gps"));
 
 		// waypoint types
 		waypointTypes.put("flag", res.getString(R.string.wp_final));
 		waypointTypes.put("stage", res.getString(R.string.wp_stage));
 		waypointTypes.put("puzzle", res.getString(R.string.wp_puzzle));
 		waypointTypes.put("pkg", res.getString(R.string.wp_pkg));
 		waypointTypes.put("trailhead", res.getString(R.string.wp_trailhead));
 		waypointTypes.put("waypoint", res.getString(R.string.wp_waypoint));
 
 		// log types
 		logTypes.put("icon_smile", 2);
 		logTypes.put("icon_sad", 3);
 		logTypes.put("icon_note", 4);
 		logTypes.put("icon_greenlight", 5);
 		logTypes.put("icon_enabled", 6);
 		logTypes.put("traffic_cone", 7);
 		logTypes.put("icon_disabled", 8);
 		logTypes.put("icon_rsvp", 9);
 		logTypes.put("icon_attended", 10);
 		logTypes.put("icon_transfer", 19);
 		logTypes.put("icon_needsmaint", 45);
 		logTypes.put("icon_maint", 46);
 		logTypes.put("coord_update", 47);
 		logTypes.put("icon_discovered", 48);
 		logTypes.put("big_smile", 49);
 		logTypes.put("picked_up", -1);
 		logTypes.put("dropped_off", -2);
 
 		logTypes0.put("found it", 2);
 		logTypes0.put("didn't find it", 3);
 		logTypes0.put("write note", 4);
 		logTypes0.put("publish listing", 5);
 		logTypes0.put("enable listing", 6);
 		logTypes0.put("archive", 7);
 		logTypes0.put("temporarily disable listing", 8);
 		logTypes0.put("will attend", 9);
 		logTypes0.put("attended", 10);
 		logTypes0.put("retrieved it", 13);
 		logTypes0.put("grabbed it", 19);
 		logTypes0.put("needs maintenance", 45);
 		logTypes0.put("owner maintenance", 46);
 		logTypes0.put("update coordinates", 47);
 		logTypes0.put("discovered it", 48);
 		logTypes0.put("post reviewer note", 49);
 		logTypes0.put("placed it", -2);
 
 		logTypes1.put(2, res.getString(R.string.log_found));
 		logTypes1.put(3, res.getString(R.string.log_dnf));
 		logTypes1.put(4, res.getString(R.string.log_note));
 		logTypes1.put(5, res.getString(R.string.log_published));
 		logTypes1.put(6, res.getString(R.string.log_enabled));
 		logTypes1.put(7, res.getString(R.string.log_archived));
 		logTypes1.put(8, res.getString(R.string.log_disabled));
 		logTypes1.put(9, res.getString(R.string.log_attend));
 		logTypes1.put(10, res.getString(R.string.log_attended));
 		logTypes1.put(13, res.getString(R.string.log_retrieved));
 		logTypes1.put(19, res.getString(R.string.log_grabbed));
 		logTypes1.put(45, res.getString(R.string.log_needs));
 		logTypes1.put(46, res.getString(R.string.log_maintenance));
 		logTypes1.put(47, res.getString(R.string.log_update));
 		logTypes1.put(48, res.getString(R.string.log_discovered));
 		logTypes1.put(49, res.getString(R.string.log_review));
 
 		logTypes2.put(2, res.getString(R.string.log_new_found)); // traditional, multi, unknown, earth, wherigo, virtual, letterbox
 		logTypes2.put(3, res.getString(R.string.log_new_dnf)); // traditional, multi, unknown, earth, wherigo, virtual, letterbox, webcam
 		logTypes2.put(4, res.getString(R.string.log_new_note)); // traditional, multi, unknown, earth, wherigo, virtual, event, letterbox, webcam, trackable
 		logTypes2.put(7, res.getString(R.string.log_new_archive)); // traditional, multi, unknown, earth, event, wherigo, virtual, letterbox, webcam
 		logTypes2.put(9, res.getString(R.string.log_new_attend)); // event
 		logTypes2.put(10, res.getString(R.string.log_new_attended)); // event
 		logTypes2.put(11, res.getString(R.string.log_new_webcam)); // webcam
 		logTypes2.put(13, res.getString(R.string.log_new_retrieve)); //trackable
 		logTypes2.put(19, res.getString(R.string.log_new_grab)); //trackable
 		logTypes2.put(45, res.getString(R.string.log_new_maintenance)); // traditional, unknown, multi, wherigo, virtual, letterbox, webcam
 		logTypes2.put(46, res.getString(R.string.log_new_maintenance_owner)); // owner
 		logTypes2.put(48, res.getString(R.string.log_new_discovered)); //trackable
 		logTypes2.put(49, res.getString(R.string.log_new_review)); // X
 
 		// trackables for logs
 		logTypesTrackable.put(0, res.getString(R.string.log_tb_nothing)); // do nothing
 		logTypesTrackable.put(1, res.getString(R.string.log_tb_visit)); // visit cache
 		logTypesTrackable.put(2, res.getString(R.string.log_tb_drop)); // drop here
 		logTypesTrackableAction.put(0, ""); // do nothing
 		logTypesTrackableAction.put(1, "_Visited"); // visit cache
 		logTypesTrackableAction.put(2, "_DroppedOff"); // drop here
 
 		// retrieving errors (because of ____ )
 		errorRetrieve.put(1, res.getString(R.string.err_none));
 		errorRetrieve.put(0, res.getString(R.string.err_start));
 		errorRetrieve.put(-1, res.getString(R.string.err_parse));
 		errorRetrieve.put(-2, res.getString(R.string.err_server));
 		errorRetrieve.put(-3, res.getString(R.string.err_login));
 		errorRetrieve.put(-4, res.getString(R.string.err_unknown));
 		errorRetrieve.put(-5, res.getString(R.string.err_comm));
 		errorRetrieve.put(-6, res.getString(R.string.err_wrong));
 		errorRetrieve.put(-7, res.getString(R.string.err_license));
 
 		// init
 		app = appIn;
 		settings = settingsIn;
 		prefs = prefsIn;
 
 		patternLoggedIn = Pattern.compile("<p class=\"AlignRight\">[^<]+<a href=\"http://www\\.geocaching\\.com/my/\">([^<]+)</a>\\.", Pattern.CASE_INSENSITIVE);
 
 		if (settings.asBrowser == 1) {
 			final long rndBrowser = Math.round(Math.random() * 6);
 			if (rndBrowser == 0) {
 				idBrowser = "Mozilla/5.0 (Windows; U; Windows NT 6.1; en-US) AppleWebKit/533.1 (KHTML, like Gecko) Chrome/5.0.322.2 Safari/533.1";
 			} else if (rndBrowser == 1) {
 				idBrowser = "Mozilla/4.0 (compatible; MSIE 8.0; Windows NT 6.1; WOW64; Trident/4.0; SLCC2; .NET CLR 2.0.50727; .NET CLR 3.5.30729; .NET CLR 3.0.30729; Media Center PC 6.0; MDDC)";
 			} else if (rndBrowser == 2) {
 				idBrowser = "Mozilla/5.0 (Windows; U; Windows NT 5.1; en-US; rv:1.9.2.3) Gecko/20100401 Firefox/3.6.3";
 			} else if (rndBrowser == 3) {
 				idBrowser = "Mozilla/5.0 (Macintosh; U; Intel Mac OS X 10_6_2; en-us) AppleWebKit/531.21.8 (KHTML, like Gecko) Version/4.0.4 Safari/531.21.10";
 			} else if (rndBrowser == 4) {
 				idBrowser = "Mozilla/5.0 (iPod; U; CPU iPhone OS 2_2_1 like Mac OS X; en-us) AppleWebKit/525.18.1 (KHTML, like Gecko) Version/3.1.1 Mobile/5H11a Safari/525.20";
 			} else if (rndBrowser == 5) {
 				idBrowser = "Mozilla/5.0 (Linux; U; Android 1.1; en-gb; dream) AppleWebKit/525.10+ (KHTML, like Gecko) Version/3.0.4 Mobile Safari/523.12.2";
 			} else if (rndBrowser == 6) {
 				idBrowser = "Mozilla/5.0 (X11; U; Linux i686; en-US) AppleWebKit/533.4 (KHTML, like Gecko) Chrome/5.0.375.86 Safari/533.4";
 			} else {
 				idBrowser = "Mozilla/5.0 (Macintosh; U; Intel Mac OS X 10_6_2; en-US) AppleWebKit/532.9 (KHTML, like Gecko) Chrome/5.0.307.11 Safari/532.9";
 			}
 		}
 	}
 
 	public String findViewstate(String page, int index) {
 		String viewstate = null;
 
 		if (index == 0) {
 			final Matcher matcherViewstate = patternViewstate.matcher(page);
 			while (matcherViewstate.find()) {
 				if (matcherViewstate.groupCount() > 0) {
 					viewstate = matcherViewstate.group(1);
 				}
 			}
 		} else if (index == 1) {
 			final Matcher matcherViewstate = patternViewstate1.matcher(page);
 			while (matcherViewstate.find()) {
 				if (matcherViewstate.groupCount() > 0) {
 					viewstate = matcherViewstate.group(1);
 				}
 			}
 		}
 
 		return viewstate;
 	}
 
 	public class loginThread extends Thread {
 
 		@Override
 		public void run() {
 			login();
 		}
 	}
 
 	public int login() {
 		final String host = "www.geocaching.com";
 		final String path = "/login/default.aspx";
 		String loginPage = null;
 
 		String viewstate = null;
 		String viewstate1 = null;
 
 		final HashMap<String, String> loginStart = settings.getLogin();
 
 		if (loginStart == null) {
 			return -3; // no login information stored
 		}
 
 		loginPage = request(host, path, "GET", new HashMap<String, String>(), false, false, false);
 		if (loginPage != null && loginPage.length() > 0) {
 			if (checkLogin(loginPage) == true) {
 				Log.i(cgSettings.tag, "Already logged in Geocaching.com as " + loginStart.get("username"));
 
 				switchToEnglish(viewstate, viewstate1);
 
 				return 1; // logged in
 			}
 
 			viewstate = findViewstate(loginPage, 0);
 			viewstate1 = findViewstate(loginPage, 1);
 
 			if (viewstate == null || viewstate.length() == 0) {
 				Log.e(cgSettings.tag, "cgeoBase.login: Failed to find viewstate");
 				return -1; // no viewstate
 			}
 		} else {
 			Log.e(cgSettings.tag, "cgeoBase.login: Failed to retrieve login page (1st)");
 			return -2; // no loginpage
 		}
 
 		final HashMap<String, String> login = settings.getLogin();
 		final HashMap<String, String> params = new HashMap<String, String>();
 
 		if (login == null || login.get("username") == null || login.get("username").length() == 0 || login.get("password") == null || login.get("password").length() == 0) {
 			Log.e(cgSettings.tag, "cgeoBase.login: No login information stored");
 			return -3;
 		}
 
 		settings.deleteCookies();
 
 		params.put("__EVENTTARGET", "");
 		params.put("__EVENTARGUMENT", "");
 		params.put("__VIEWSTATE", viewstate);
 		if (viewstate1 != null) {
 			params.put("__VIEWSTATE1", viewstate1);
 			params.put("__VIEWSTATEFIELDCOUNT", "2");
 		}
 		params.put("ctl00$ContentBody$myUsername", login.get("username"));
 		params.put("ctl00$ContentBody$myPassword", login.get("password"));
 		params.put("ctl00$ContentBody$cookie", "on");
 		params.put("ctl00$ContentBody$Button1", "Login");
 		loginPage = request(host, path, "POST", params, false, false, false);
 
 		if (loginPage != null && loginPage.length() > 0) {
 			if (checkLogin(loginPage) == true) {
 				Log.i(cgSettings.tag, "Successfully logged in Geocaching.com as " + login.get("username"));
 
 				switchToEnglish(findViewstate(loginPage, 0), findViewstate(loginPage, 1));
 
 				return 1; // logged in
 			} else {
 				if (loginPage.indexOf("Your username/password combination does not match.") != -1) {
 					Log.i(cgSettings.tag, "Failed to log in Geocaching.com as " + login.get("username") + " because of wrong username/password");
 
 					return -6; // wrong login
 				} else {
 					Log.i(cgSettings.tag, "Failed to log in Geocaching.com as " + login.get("username") + " for some unknown reason");
 
 					return -4; // can't login
 				}
 			}
 		} else {
 			Log.e(cgSettings.tag, "cgeoBase.login: Failed to retrieve login page (2nd)");
 
 			return -5; // no login page
 		}
 	}
 
 	public Boolean checkLogin(String page) {
 		final Matcher matcherLoggedIn = patternLoggedIn.matcher(page);
 		while (matcherLoggedIn.find()) {
 			if (matcherLoggedIn.groupCount() > 0) {
 				return true;
 			}
 		}
 
 		return false;
 	}
 
 	public String switchToEnglish(String viewstate, String viewstate1) {
 		final String host = "www.geocaching.com";
 		final String path = "/login/default.aspx";
 		final HashMap<String, String> params = new HashMap<String, String>();
 
 		if (viewstate != null) {
 			params.put("__VIEWSTATE", viewstate);
 		}
 		if (viewstate1 != null) {
 			params.put("__VIEWSTATE1", viewstate1);
 			params.put("__VIEWSTATEFIELDCOUNT", "2");
 		}
 		params.put("__EVENTTARGET", "ctl00$uxLocaleList$uxLocaleList$ctl01$uxLocaleItem"); // switch to english
 		params.put("__EVENTARGUMENT", "");
 
 		return request(host, path, "POST", params, false, false, false);
 	}
 
 	public cgCacheWrap parseSearch(String url, String page) {
 		if (page == null || page.length() == 0) {
 			Log.e(cgSettings.tag, "cgeoBase.parseSearch: No page given");
 			return null;
 		}
 
 		final cgCacheWrap caches = new cgCacheWrap();
 		final ArrayList<String> cids = new ArrayList<String>();
 		final ArrayList<String> guids = new ArrayList<String>();
 
 		caches.url = url;
 
 		final Pattern patternCacheType = Pattern.compile("<td>[^<]*<a[^>]+>[^<]*<img src=\"[^\"]*/wpttypes/sm/[^\\.]+\\.gif\" alt=\"([^\"]+)\" title=\"[^\"]+\"[^>]*>[^<]*</a>", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE);
 		final Pattern patternGuidAndDisabled = Pattern.compile("<img src=\"[^\"]*/wpttypes/sm/[^>]*>[^<]*</a>[^<]*<a href=\"[^\"]*/seek/cache_details\\.aspx\\?guid=([a-z0-9\\-]+)\" class=\"lnk ?([^<\"]*)\">(<span>)?([^<]*)(</span>)?</a>[^<]+<br />([^<]+)(<img[^>]*>)?([^<]*)</td>", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE);
 		final Pattern patternTbs = Pattern.compile("<a id=\"ctl00_ContentBody_dlResults_ctl01_uxTravelBugList\" class=\"tblist\" data-tbcount=\"([0-9]+)\" data-id=\"[^\"]*\"[^>]*>(.*)</a>", Pattern.CASE_INSENSITIVE);
 		final Pattern patternTbsInside = Pattern.compile("(<img src=\"[^\"]+\" alt=\"([^\"]+)\" title=\"[^\"]*\" />[^<]*)", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE);
 		final Pattern patternDirection = Pattern.compile("<img id=\"ctl00_ContentBody_dlResults_ctl[0-9]+_uxDistanceAndHeading\" title=\"[^\"]*\" src=\"[^\"]*/seek/CacheDir\\.ashx\\?k=([^\"]+)\"[^>]*>", Pattern.CASE_INSENSITIVE);
 		final Pattern patternRating = Pattern.compile("<img id=\"ctl00_ContentBody_dlResults_ctl[0-9]+_uxDTCacheTypeImage\" src=\"[^\"]*/seek/CacheInfo\\.ashx\\?v=([^\"]+)\"[^>]*>", Pattern.CASE_INSENSITIVE);
 		final Pattern patternCacheDiffAndSize = Pattern.compile("\\(([0-9\\.\\,]+)/([0-9\\.\\,]+)\\)<br[^>]*>[^<]*<img src=\".*\\/icons\\/container\\/[a-z_]+.gif\" alt=\"Size: ([^\"]+)\"", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE);
 		final Pattern patternCode = Pattern.compile("\\((GC[a-z0-9]+)\\)", Pattern.CASE_INSENSITIVE);
 		final Pattern patternId = Pattern.compile("name=\"CID\"[^v]*value=\"([0-9]+)\"", Pattern.CASE_INSENSITIVE);
 		final Pattern patternTotalCnt = Pattern.compile("<td class=\"PageBuilderWidget\"><span>Total Records[^<]*<b>(\\d+)<\\/b>", Pattern.CASE_INSENSITIVE);
 
 		caches.viewstate = findViewstate(page, 0);
 		caches.viewstate1 = findViewstate(page, 1);
 
 		int startPos = -1;
 		int endPos = -1;
 
 		startPos = page.indexOf("<table id=\"ctl00_ContentBody_dlResults\"");
 		if (startPos == -1) {
 			Log.e(cgSettings.tag, "cgeoBase.parseSearch: ID \"ctl00_ContentBody_dlResults\" not found on page");
 			return null;
 		}
 
 		page = page.substring(startPos); // cut on <table
 
 		startPos = page.indexOf(">");
 		endPos = page.indexOf("ctl00_ContentBody_UnitTxt");
 		if (startPos == -1 || endPos == -1) {
 			Log.e(cgSettings.tag, "cgeoBase.parseSearch: ID \"ctl00_ContentBody_UnitTxt\" not found on page");
 			return null;
 		}
 
 		page = page.substring(startPos + 1, endPos - startPos + 1); // cut between <table> and </table>
 
 		final String[] rows = page.split("</tr><tr>");
 		final int rows_count = rows.length;
 
 		for (int z = 1; z < rows_count; z++) {
 			cgCache cache = new cgCache();
 			String row = rows[z];
 
 			// check for cache type presence
 			if (row.indexOf("images/wpttypes") == -1) {
 				continue;
 			}
 
 			try {
 				final Matcher matcherGuidAndDisabled = patternGuidAndDisabled.matcher(row);
 
 				while (matcherGuidAndDisabled.find()) {
 					if (matcherGuidAndDisabled.groupCount() > 0) {
 						guids.add(matcherGuidAndDisabled.group(1));
 
 						cache.guid = matcherGuidAndDisabled.group(1);
 						cache.name = Html.fromHtml(matcherGuidAndDisabled.group(4).trim()).toString();
 						cache.location = Html.fromHtml(matcherGuidAndDisabled.group(6).trim()).toString();
 
 						final String attr = matcherGuidAndDisabled.group(2);
 						if (attr != null) {
 							if (attr.contains("Strike") == true) {
 								cache.disabled = true;
 							} else {
 								cache.disabled = false;
 							}
 
 							if (attr.contains("OldWarning") == true) {
 								cache.archived = true;
 							} else {
 								cache.archived = false;
 							}
 						}
 					}
 				}
 			} catch (Exception e) {
 				// failed to parse GUID and/or Disabled
 				Log.w(cgSettings.tag, "cgeoBase.parseSearch: Failed to parse GUID and/or Disabled data");
 			}
 
 			if (settings.excludeDisabled == 1 && (cache.disabled == true || cache.archived == true)) {
 				// skip disabled and archived caches
 				cache = null;
 				continue;
 			}
 
 			String inventoryPre = null;
 
 			// GC* code
 			try {
 				final Matcher matcherCode = patternCode.matcher(row);
 				while (matcherCode.find()) {
 					if (matcherCode.groupCount() > 0) {
 						cache.geocode = matcherCode.group(1).toUpperCase();
 					}
 				}
 			} catch (Exception e) {
 				// failed to parse code
 				Log.w(cgSettings.tag, "cgeoBase.parseSearch: Failed to parse cache code");
 			}
 
 			// cache type
 			try {
 				final Matcher matcherCacheType = patternCacheType.matcher(row);
 				while (matcherCacheType.find()) {
 					if (matcherCacheType.groupCount() > 0) {
 						cache.type = cacheTypes.get(matcherCacheType.group(1).toLowerCase());
 					}
 				}
 			} catch (Exception e) {
 				// failed to parse type
 				Log.w(cgSettings.tag, "cgeoBase.parseSearch: Failed to parse cache type");
 			}
 
 			// cache direction
 			try {
 				final Matcher matcherDirection = patternDirection.matcher(row);
 				while (matcherDirection.find()) {
 					if (matcherDirection.groupCount() > 0) {
 						final String directionPre = xorEnDecrypt(URLDecoder.decode(matcherDirection.group(1)));
						final String[] directionParts = directionPre.split("\\|");
 
 						cache.distance = parseDistance(directionParts[0]);
 
 						try {
 							cache.direction = new Double(directionParts[1]);
						} catch (Exception e) {
 							cache.direction = new Double(0);
 						}
 					}
 				}
 			} catch (Exception e) {
 				// failed to parse direction
 				Log.w(cgSettings.tag, "cgeoBase.parseSearch: Failed to parse cache direction");
 			}
 
 			// cache rating
 			try {
 				final Matcher matcherRating = patternRating.matcher(row);
 				while (matcherRating.find()) {
 					if (matcherRating.groupCount() > 0) {
 						final HashMap<String, Object> rating = ratingDecrypt(matcherRating.group(1));
 
                         cache.size = (String)rating.get("size");
                         cache.terrain = (Float)rating.get("terrain");
                         cache.difficulty = (Float)rating.get("difficulty");
 					}
 				}
 			} catch (Exception e) {
 				// failed to parse rating
 				Log.w(cgSettings.tag, "cgeoBase.parseSearch: Failed to parse cache rating");
 			}
 
 			// cache inventory
 			try {
 				final Matcher matcherTbs = patternTbs.matcher(row);
 				while (matcherTbs.find()) {
 					if (matcherTbs.groupCount() > 0) {
 						cache.inventoryItems = Integer.parseInt(matcherTbs.group(1));
 						inventoryPre = matcherTbs.group(2);
 					}
 				}
 			} catch (Exception e) {
 				// failed to parse inventory
 				Log.w(cgSettings.tag, "cgeoBase.parseSearch: Failed to parse cache inventory");
 			}
 
 			if (inventoryPre != null && inventoryPre.trim().length() > 0) {
 				try {
 					final Matcher matcherTbsInside = patternTbsInside.matcher(inventoryPre);
 					while (matcherTbsInside.find()) {
 						if (matcherTbsInside.groupCount() == 2 && matcherTbsInside.group(2) != null) {
 							final String inventoryItem = matcherTbsInside.group(2).toLowerCase();
 							if (inventoryItem.equals("premium member only cache")) {
 								continue;
 							} else {
 								if (cache.inventoryItems <= 0) {
 									cache.inventoryItems = 1;
 								}
 							}
 						}
 					}
 				} catch (Exception e) {
 					// failed to parse cache inventory info
 					Log.w(cgSettings.tag, "cgeoBase.parseSearch: Failed to parse cache inventory info");
 				}
 			}
 
 			// cache size, difficulty and terrain
 			try {
 				final Matcher matcherCacheDiffSize = patternCacheDiffAndSize.matcher(row);
 				while (matcherCacheDiffSize.find()) {
 					if (matcherCacheDiffSize.groupCount() > 0) {
 						cache.difficulty = new Float(matcherCacheDiffSize.group(1));
 						cache.terrain = new Float(matcherCacheDiffSize.group(2));
 						cache.size = matcherCacheDiffSize.group(3).toLowerCase();
 					}
 				}
 			} catch (Exception e) {
 				// failed to parse size
 				Log.w(cgSettings.tag, "cgeoBase.parseSearch: Failed to parse cache size, difficulty or terrain");
 			}
 
 			// premium cache
 			if (row.indexOf("images/small_profile.gif") != -1) {
 				cache.members = true;
 			} else {
 				cache.members = false;
 			}
 
 			// found it
 			if (row.indexOf("images/silk/emoticon_smile.png") != -1) {
 				cache.found = true;
 			} else {
 				cache.found = false;
 			}
 
 			// id
 			try {
 				final Matcher matcherId = patternId.matcher(row);
 				while (matcherId.find()) {
 					if (matcherId.groupCount() > 0) {
 						cache.cacheid = matcherId.group(1);
 						cids.add(cache.cacheid);
 					}
 				}
 			} catch (Exception e) {
 				// failed to parse cache id
 				Log.w(cgSettings.tag, "cgeoBase.parseSearch: Failed to parse cache id");
 			}
 
 			if (cache.nameSp == null) {
 				cache.nameSp = (new Spannable.Factory()).newSpannable(cache.name);
 				if (cache.disabled == true || cache.archived == true) { // strike
 					cache.nameSp.setSpan(new StrikethroughSpan(), 0, cache.nameSp.toString().length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
 				}
 			}
 
 			caches.cacheList.add(cache);
 		}
 
 		// total caches found
 		try {
 			final Matcher matcherTotalCnt = patternTotalCnt.matcher(page);
 			while (matcherTotalCnt.find()) {
 				if (matcherTotalCnt.groupCount() > 0) {
 					if (matcherTotalCnt.group(1) != null) {
 						caches.totalCnt = new Integer(matcherTotalCnt.group(1));
 					}
 				}
 			}
 		} catch (Exception e) {
 			// failed to parse cache count
 			Log.w(cgSettings.tag, "cgeoBase.parseSearch: Failed to parse cache count");
 		}
 
 		if (cids.size() > 0) {
 			Log.i(cgSettings.tag, "Trying to get .loc for " + cids.size() + " caches");
 
 			try {
 				// get coordinates for parsed caches
 				final String host = "www.geocaching.com";
 				final String path = "/seek/nearest.aspx";
 				final StringBuilder params = new StringBuilder();
 				for (String cid : cids) {
 					params.append("CID=");
 					params.append(urlencode_rfc3986(cid));
 					params.append("&");
 				}
 				params.append("Download=Download+Waypoints");
 
 				final String coordinates = request(host, path, "POST", params.toString(), 0, true);
 
 				if (coordinates != null && coordinates.length() > 0) {
 					if (coordinates.indexOf("You have not agreed to the license agreement. The license agreement is required before you can start downloading GPX or LOC files from Geocaching.com") > -1) {
 						Log.i(cgSettings.tag, "User has not agreed to the license agreement. Can\'t download .loc file.");
 
 						caches.error = errorRetrieve.get(-7);
 
 						return caches;
 					}
 				}
 
 				if (coordinates != null && coordinates.length() > 0) {
 					final HashMap<String, cgCoord> cidCoords = new HashMap<String, cgCoord>();
 					final Pattern patternCidCode = Pattern.compile("name id=\"([^\"]+)\"");
 					final Pattern patternCidLat = Pattern.compile("lat=\"([^\"]+)\"");
 					final Pattern patternCidLon = Pattern.compile("lon=\"([^\"]+)\"");
 					final String[] points = coordinates.split("<waypoint>");
 
 					// parse coordinates
 					for (String point : points) {
 						final cgCoord pointCoord = new cgCoord();
 						final Matcher matcherCidCode = patternCidCode.matcher(point);
 						final Matcher matcherLatCode = patternCidLat.matcher(point);
 						final Matcher matcherLonCode = patternCidLon.matcher(point);
 						HashMap tmp = null;
 
 						if (matcherCidCode.find() == true) {
 							pointCoord.name = matcherCidCode.group(1).trim().toUpperCase();
 						}
 						if (matcherLatCode.find() == true) {
 							tmp = parseCoordinate(matcherLatCode.group(1), "lat");
 							pointCoord.latitude = (Double) tmp.get("coordinate");
 						}
 						if (matcherLonCode.find() == true) {
 							tmp = parseCoordinate(matcherLonCode.group(1), "lon");
 							pointCoord.longitude = (Double) tmp.get("coordinate");
 						}
 
 						cidCoords.put(pointCoord.name, pointCoord);
 					}
 
 					// save found cache coordinates
 					for (cgCache oneCache : caches.cacheList) {
 						if (cidCoords.containsKey(oneCache.geocode) == true) {
 							cgCoord thisCoords = cidCoords.get(oneCache.geocode);
 
 							oneCache.latitude = thisCoords.latitude;
 							oneCache.longitude = thisCoords.longitude;
 						}
 					}
 				}
 			} catch (Exception e) {
 				Log.e(cgSettings.tag, "cgBase.parseSearch.CIDs: " + e.toString());
 			}
 		}
 
 		if (guids.size() > 0) {
 			Log.i(cgSettings.tag, "Trying to get ratings for " + cids.size() + " caches");
 
 			try {
 				final HashMap<String, cgRating> ratings = getRating(guids, null);
 
 				if (ratings != null) {
 					// save found cache coordinates
 					for (cgCache oneCache : caches.cacheList) {
 						if (ratings.containsKey(oneCache.guid) == true) {
 							cgRating thisRating = ratings.get(oneCache.guid);
 
 							oneCache.rating = thisRating.rating;
 							oneCache.votes = thisRating.votes;
 							oneCache.vote = thisRating.vote;
 						}
 					}
 				}
 			} catch (Exception e) {
 				Log.e(cgSettings.tag, "cgBase.parseSearch.GCvote: " + e.toString());
 			}
 		}
 
 		return caches;
 	}
 
 	public cgCacheWrap parseMapJSON(String url, String data) {
 		if (data == null || data.length() == 0) {
 			Log.e(cgSettings.tag, "cgeoBase.parseMapJSON: No page given");
 			return null;
 		}
 
 		final cgCacheWrap caches = new cgCacheWrap();
 		caches.url = url;
 
 		try {
 			final JSONObject yoDawg = new JSONObject(data);
 			final String json = yoDawg.getString("d");
 
 			if (json == null || json.length() == 0) {
 				Log.e(cgSettings.tag, "cgeoBase.parseMapJSON: No JSON inside JSON");
 				return null;
 			}
 
 			final JSONObject dataJSON = new JSONObject(json);
 			final JSONObject extra = dataJSON.getJSONObject("cs");
 			if (extra != null && extra.length() > 0) {
 				int count = extra.getInt("count");
 
 				if (count > 0 && extra.has("cc")) {
 					final JSONArray cachesData = extra.getJSONArray("cc");
 					if (cachesData != null && cachesData.length() > 0) {
 						JSONObject oneCache = null;
 						for (int i = 0; i < count; i++) {
 							oneCache = cachesData.getJSONObject(i);
 							if (oneCache == null) {
 								break;
 							}
 
 							final cgCache cacheToAdd = new cgCache();
 							cacheToAdd.geocode = oneCache.getString("gc");
 							cacheToAdd.latitude = oneCache.getDouble("lat");
 							cacheToAdd.longitude = oneCache.getDouble("lon");
 							cacheToAdd.name = oneCache.getString("nn");
 							cacheToAdd.found = oneCache.getBoolean("f");
 							cacheToAdd.own = oneCache.getBoolean("o");
 							cacheToAdd.disabled = !oneCache.getBoolean("ia");
 							int ctid = oneCache.getInt("ctid");
 							if (ctid == 2) {
 								cacheToAdd.type = "traditional";
 							} else if (ctid == 3) {
 								cacheToAdd.type = "multi";
 							} else if (ctid == 4) {
 								cacheToAdd.type = "virtual";
 							} else if (ctid == 5) {
 								cacheToAdd.type = "letterbox";
 							} else if (ctid == 6) {
 								cacheToAdd.type = "event";
 							} else if (ctid == 8) {
 								cacheToAdd.type = "mystery";
 							} else if (ctid == 11) {
 								cacheToAdd.type = "webcam";
 							} else if (ctid == 13) {
 								cacheToAdd.type = "cito";
 							} else if (ctid == 137) {
 								cacheToAdd.type = "earth";
 							} else if (ctid == 453) {
 								cacheToAdd.type = "mega";
 							} else if (ctid == 1858) {
 								cacheToAdd.type = "wherigo";
 							} else if (ctid == 3653) {
 								cacheToAdd.type = "lost";
 							}
 
 							caches.cacheList.add(cacheToAdd);
 						}
 					}
 				} else {
 					Log.w(cgSettings.tag, "There are no caches in viewport");
 				}
 				caches.totalCnt = caches.cacheList.size();
 			}
 		} catch (Exception e) {
 			Log.e(cgSettings.tag, "cgBase.parseMapJSON: " + e.toString());
 		}
 
 		return caches;
 	}
 
 	public cgCacheWrap parseCache(String page, int reason) {
 		if (page == null || page.length() == 0) {
 			Log.e(cgSettings.tag, "cgeoBase.parseCache: No page given");
 			return null;
 		}
 
 		final Pattern patternGeocode = Pattern.compile("<span id=\"ctl00_uxWaypointName\" class=\"GCCode\">[^G]*(GC[a-z0-9]+)</span>", Pattern.CASE_INSENSITIVE);
 		final Pattern patternCacheId = Pattern.compile("\\/seek\\/log\\.aspx\\?ID=(\\d+)", Pattern.CASE_INSENSITIVE);
 		final Pattern patternCacheGuid = Pattern.compile("<link rel=\"alternate\" href=\".*rss_galleryimages\\.ashx\\?guid=([a-z0-9\\-]+)\"[^>]*>", Pattern.CASE_INSENSITIVE);
 		final Pattern patternType = Pattern.compile("<img src=\"[a-z0-9\\-\\_\\.\\?\\/\\:\\@]*\\/WptTypes\\/\\d+.gif\" alt=\"([^\"]+)\" width=\"32\" height=\"32\"[^>]*>", Pattern.CASE_INSENSITIVE);
 
 		final Pattern patternName = Pattern.compile("<h2 class=\"NoSpacing\"[^>]*>[^<]*<a[^>]+>[^<]*<img[^>]+>[^<]*<\\/a>[^<]*<span id=\"ctl00_ContentBody_CacheName\">([^<]+)<\\/span>[^<]*<\\/h2>", Pattern.CASE_INSENSITIVE);
 		final Pattern patternSize = Pattern.compile("<td[^>]*>[^<]*<strong>[^S]*Size[^:]*:[^<]*</strong>[^<]*<img src=\"[a-z0-9\\-\\_\\.\\?\\/\\:\\@]*\\/icons\\/container\\/[a-z_]+.gif\" alt=\"Size: ([^\"]+)\" />[^<]*<small>[^<]*<\\/small>[^<]*<\\/td>", Pattern.CASE_INSENSITIVE);
 		final Pattern patternDifficulty = Pattern.compile("<td[^>]*>[^<]*<strong>[^S]*Difficulty[^:]*:[^<]*</strong>[^<]*<img src=\"[^>]*/stars/stars([0-9_]+)\\.gif\"", Pattern.CASE_INSENSITIVE);
 		final Pattern patternTerrain = Pattern.compile("<td[^>]*>[^<]*<strong>[^S]*Terrain[^:]*:[^<]*</strong>[^<]*<img src=\"[^>]*/stars/stars([0-9_]+)\\.gif\"", Pattern.CASE_INSENSITIVE);
 		final Pattern patternOwner = Pattern.compile("<td[^>]*>[^<]*<strong>[^\\w]*An?([^\\w]*Event)?[^\\w]*cache[^<]*<\\/strong>[^\\w]*by[^<]*<a href=\"[^\"]+\">([^<]+)<\\/a>", Pattern.CASE_INSENSITIVE);
 		final Pattern patternHidden = Pattern.compile("<td[^>]*>[^<]*<strong>[^\\w]*Hidden[^:]*:[^<]*</strong>[^\\d]*((\\d+)\\/(\\d+)\\/(\\d+))[^<]*</td>", Pattern.CASE_INSENSITIVE);
 		final Pattern patternHiddenEvent = Pattern.compile("<td[^>]*>[^<]*<strong>[^\\w]*Event[^\\w]*date[^:]*:[^<]*</strong>[^\\w]*[a-zA-Z]+,[^\\d]*((\\d+)[^\\w]*(\\w+)[^\\d]*(\\d+))[^<]*<div", Pattern.CASE_INSENSITIVE);
 
 		final Pattern patternFound = Pattern.compile("<p>[^<]*<a id=\"ctl00_ContentBody_hlFoundItLog\"[^<]*<img src=\".*/images/stockholm/16x16/check\\.gif\"[^>]*>[^<]+</a>[^<]+</p>", Pattern.CASE_INSENSITIVE);
 		final Pattern patternLatLon = Pattern.compile("<span id=\"ctl00_ContentBody_LatLon\"[^>]*>(<b>)?([^<]*)(<\\/b>)?<\\/span>", Pattern.CASE_INSENSITIVE);
 		final Pattern patternLocation = Pattern.compile("<span id=\"ctl00_ContentBody_Location\"[^>]*>In ([^<]*)<\\/span>", Pattern.CASE_INSENSITIVE);
 		final Pattern patternHint = Pattern.compile("<p>([^<]*<strong>)?[^\\w]*Additional Hints([^<]*<\\/strong>)?[^\\(]*\\(<a[^>]+>Encrypt<\\/a>\\)[^<]*<\\/p>[^<]*<p>[^<]*<\\/p>[^<]*<div id=\"div_hint\"[^>]*>(.*)<\\/div>[^<]*<div id=\\'dk\\'[^>]+>", Pattern.CASE_INSENSITIVE);
 		final Pattern patternDescShort = Pattern.compile("<span id=\"ctl00_ContentBody_ShortDescription\"[^>]*>(.*)<\\/span>", Pattern.CASE_INSENSITIVE);
 		final Pattern patternDesc = Pattern.compile("<span id=\"ctl00_ContentBody_LongDescription\"[^>]*>(.*)<\\/span>.*<div class=\"CacheDetailNavigationWidget\">", Pattern.CASE_INSENSITIVE);
 		final Pattern patternLogs = Pattern.compile("<table class=\"LogsTable Table\">(.*)<\\/table>[^<]*<p>", Pattern.CASE_INSENSITIVE);
 		final Pattern patternAttributes = Pattern.compile("<div class=\"CacheDetailNavigationWidget Spacing\"[^>]*>(([^<]*<img src=\"[^\"]+\" alt=\"[^\"]+\"[^>]*>)+)", Pattern.CASE_INSENSITIVE);
 		final Pattern patternAttributesInside = Pattern.compile("[^<]*<img src=\"[^\"]+\" alt=\"([^\"]+)\"[^>]*>", Pattern.CASE_INSENSITIVE);
 		final Pattern patternSpoilers = Pattern.compile("<span id=\"ctl00_ContentBody_Images\">((<a href=\"[^\"]+\"[^>]*><img[^>]+>[^<]*<span>[^>]+<\\/span><\\/a><br \\/><br \\/>([^<]*<br \\/>)?)+)", Pattern.CASE_INSENSITIVE);
 		final Pattern patternSpoilersInside = Pattern.compile("[^<]*<a href=\"([^\"]+)\"[^>]*><img[^>]+>[^<]*<span>([^>]+)<\\/span><\\/a><br \\/><br \\/>(([^<]*)<br \\/>)?", Pattern.CASE_INSENSITIVE);
 		final Pattern patternInventory = Pattern.compile("<span id=\"ctl00_ContentBody_uxTravelBugList_uxInventoryLabel\">Inventory</span>[^<]*<\\/h3>[^<]*<div class=\"WidgetBody\">[^<]*<ul>(([^<]*<li>[^<]*<a href=\"[^\"]+\"[^>]*>[^<]*<img src=\"[^\"]+\"[^>]*>[^<]*<span>[^<]+<\\/span>[^<]*<\\/a>[^<]*<\\/li>)+)[^<]*<\\/ul>", Pattern.CASE_INSENSITIVE);
 		final Pattern patternInventoryInside = Pattern.compile("[^<]*<li>[^<]*<a href=\"[a-z0-9\\-\\_\\.\\?\\/\\:\\@]*\\/track\\/details\\.aspx\\?guid=([0-9a-z\\-]+)[^\"]*\"[^>]*>[^<]*<img src=\"[^\"]+\"[^>]*>[^<]*<span>([^<]+)<\\/span>[^<]*<\\/a>[^<]*<\\/li>", Pattern.CASE_INSENSITIVE);
 
 		final cgCacheWrap caches = new cgCacheWrap();
 		final cgCache cache = new cgCache();
 
 		if (page.indexOf("Cache is Unpublished") > -1) {
 			caches.error = "cache was unpublished";
 			return caches;
 		}
 
 		if (page.indexOf("Sorry, the owner of this listing has made it viewable to Premium Members only.") != -1) {
 			caches.error = "requested cache is for premium members only";
 			return caches;
 		}
 
 		if (page.indexOf("has chosen to make this cache listing visible to Premium Members only.") != -1) {
 			caches.error = "requested cache is for premium members only";
 			return caches;
 		}
 
 		if (page.indexOf("<li>This cache is temporarily unavailable.") != -1) {
 			cache.disabled = true;
 		} else {
 			cache.disabled = false;
 		}
 
 		if (page.indexOf("<li>This cache has been archived,") != -1) {
 			cache.archived = true;
 		} else {
 			cache.archived = false;
 		}
 
 		if (page.indexOf("<p class=\"Warning\">This is a Premium Member Only cache.</p>") != -1) {
 			cache.members = true;
 		} else {
 			cache.members = false;
 		}
 
 		cache.reason = reason;
 
 		// cache geocode
 		try {
 			final Matcher matcherGeocode = patternGeocode.matcher(page);
 			while (matcherGeocode.find()) {
 				if (matcherGeocode.groupCount() > 0) {
 					cache.geocode = (String) matcherGeocode.group(1);
 				}
 			}
 		} catch (Exception e) {
 			// failed to parse cache geocode
 			Log.w(cgSettings.tag, "cgeoBase.parseCache: Failed to parse cache geocode");
 		}
 
 		// cache id
 		try {
 			final Matcher matcherCacheId = patternCacheId.matcher(page);
 			while (matcherCacheId.find()) {
 				if (matcherCacheId.groupCount() > 0) {
 					cache.cacheid = (String) matcherCacheId.group(1);
 				}
 			}
 		} catch (Exception e) {
 			// failed to parse cache id
 			Log.w(cgSettings.tag, "cgeoBase.parseCache: Failed to parse cache id");
 		}
 
 		// cache guid
 		try {
 			final Matcher matcherCacheGuid = patternCacheGuid.matcher(page);
 			while (matcherCacheGuid.find()) {
 				if (matcherCacheGuid.groupCount() > 0) {
 					cache.guid = (String) matcherCacheGuid.group(1);
 				}
 			}
 		} catch (Exception e) {
 			// failed to parse cache guid
 			Log.w(cgSettings.tag, "cgeoBase.parseCache: Failed to parse cache guid");
 		}
 
 		// name
 		try {
 			final Matcher matcherName = patternName.matcher(page);
 			while (matcherName.find()) {
 				if (matcherName.groupCount() > 0) {
 					cache.name = Html.fromHtml(matcherName.group(1)).toString();
 				}
 			}
 		} catch (Exception e) {
 			// failed to parse cache name
 			Log.w(cgSettings.tag, "cgeoBase.parseCache: Failed to parse cache name");
 		}
 
 		int pos = -1;
 		String tableInside = page;
 
 		pos = tableInside.indexOf("<div id=\"yui-g");
 		if (pos == -1) {
 			Log.e(cgSettings.tag, "cgeoBase.parseCache: ID \"yui-g\" not found on page");
 			return null;
 		}
 
 		tableInside = tableInside.substring(pos);
 
 		pos = tableInside.indexOf("<table");
 		if (pos == -1) {
 			Log.e(cgSettings.tag, "cgeoBase.parseCache: content table not found on page");
 			return null;
 		}
 
 		tableInside = tableInside.substring(pos);
 
 		pos = tableInside.indexOf("<table");
 		if (pos == -1) {
 			Log.e(cgSettings.tag, "cgeoBase.parseCache: inner content table not found on page");
 			return null;
 		}
 
 		tableInside = tableInside.substring(pos);
 
 		pos = tableInside.indexOf("</table>");
 		if (pos == -1) {
 			Log.e(cgSettings.tag, "cgeoBase.parseCache: end of inner content table not found on page");
 			return null;
 		}
 
 		tableInside = tableInside.substring(0, pos);
 
 		if (tableInside != null && tableInside.length() > 0) {
 			// cache terrain
 			try {
 				final Matcher matcherTerrain = patternTerrain.matcher(tableInside);
 				while (matcherTerrain.find()) {
 					if (matcherTerrain.groupCount() > 0) {
 						cache.terrain = new Float(Pattern.compile("_").matcher(matcherTerrain.group(1)).replaceAll("."));
 					}
 				}
 			} catch (Exception e) {
 				// failed to parse terrain
 				Log.w(cgSettings.tag, "cgeoBase.parseCache: Failed to parse cache terrain");
 			}
 
 			// cache difficulty
 			try {
 				final Matcher matcherDifficulty = patternDifficulty.matcher(tableInside);
 				while (matcherDifficulty.find()) {
 					if (matcherDifficulty.groupCount() > 0) {
 						cache.difficulty = new Float(Pattern.compile("_").matcher(matcherDifficulty.group(1)).replaceAll("."));
 					}
 				}
 			} catch (Exception e) {
 				// failed to parse difficulty
 				Log.w(cgSettings.tag, "cgeoBase.parseCache: Failed to parse cache difficulty");
 			}
 
 			// owner
 			try {
 				final Matcher matcherOwner = patternOwner.matcher(tableInside);
 				while (matcherOwner.find()) {
 					if (matcherOwner.groupCount() > 0) {
 						cache.owner = matcherOwner.group(2);
 					}
 				}
 			} catch (Exception e) {
 				// failed to parse owner
 				Log.w(cgSettings.tag, "cgeoBase.parseCache: Failed to parse cache owner");
 			}
 
 			// hidden
 			try {
 				final Matcher matcherHidden = patternHidden.matcher(tableInside);
 				while (matcherHidden.find()) {
 					if (matcherHidden.groupCount() > 0) {
 						cache.hidden = dateIn.parse(matcherHidden.group(1));
 					}
 				}
 			} catch (Exception e) {
 				// failed to parse cache hidden date
 				Log.w(cgSettings.tag, "cgeoBase.parseCache: Failed to parse cache hidden date");
 			}
 
 			if (cache.hidden == null) {
 				// event date
 				try {
 					final Matcher matcherHiddenEvent = patternHiddenEvent.matcher(tableInside);
 					while (matcherHiddenEvent.find()) {
 						if (matcherHiddenEvent.groupCount() > 0) {
 							cache.hidden = dateEvIn.parse(matcherHiddenEvent.group(1));
 						}
 					}
 				} catch (Exception e) {
 					// failed to parse cache event date
 					Log.w(cgSettings.tag, "cgeoBase.parseCache: Failed to parse cache event date");
 				}
 			}
 
 			// cache size
 			try {
 				final Matcher matcherSize = patternSize.matcher(tableInside);
 				while (matcherSize.find()) {
 					if (matcherSize.groupCount() > 0) {
 						cache.size = matcherSize.group(1).toLowerCase();
 					}
 				}
 			} catch (Exception e) {
 				// failed to parse size
 				Log.w(cgSettings.tag, "cgeoBase.parseCache: Failed to parse cache size");
 			}
 		}
 
 		// cache found
 		try {
 			final Matcher matcherFound = patternFound.matcher(page);
 			while (matcherFound.find()) {
 				if (matcherFound.group() != null && matcherFound.group().length() > 0) {
 					cache.found = true;
 				}
 			}
 		} catch (Exception e) {
 			// failed to parse found
 			Log.w(cgSettings.tag, "cgeoBase.parseCache: Failed to parse found");
 		}
 
 		// cache type
 		try {
 			final Matcher matcherType = patternType.matcher(page);
 			while (matcherType.find()) {
 				if (matcherType.groupCount() > 0) {
 					cache.type = cacheTypes.get(matcherType.group(1).toLowerCase());
 				}
 			}
 		} catch (Exception e) {
 			// failed to parse type
 			Log.w(cgSettings.tag, "cgeoBase.parseCache: Failed to parse cache type");
 		}
 
 		// latitude and logitude
 		try {
 			final Matcher matcherLatLon = patternLatLon.matcher(page);
 			while (matcherLatLon.find()) {
 				if (matcherLatLon.groupCount() > 0) {
 					cache.latlon = matcherLatLon.group(2); // first is <b>
 
 					HashMap tmp = this.parseLatlon(cache.latlon);
 					if (tmp.size() > 0) {
 						cache.latitude = (Double) tmp.get("latitude");
 						cache.longitude = (Double) tmp.get("longitude");
 						cache.latitudeString = (String) tmp.get("latitudeString");
 						cache.longitudeString = (String) tmp.get("longitudeString");
 					}
 					tmp = null;
 				}
 			}
 		} catch (Exception e) {
 			// failed to parse latitude and/or longitude
 			Log.w(cgSettings.tag, "cgeoBase.parseCache: Failed to parse cache coordinates");
 		}
 
 		// cache location
 		try {
 			final Matcher matcherLocation = patternLocation.matcher(page);
 			while (matcherLocation.find()) {
 				if (matcherLocation.groupCount() > 0) {
 					cache.location = matcherLocation.group(1);
 				}
 			}
 		} catch (Exception e) {
 			// failed to parse location
 			Log.w(cgSettings.tag, "cgeoBase.parseCache: Failed to parse cache location");
 		}
 
 		// cache hint
 		try {
 			final Matcher matcherHint = patternHint.matcher(page);
 			while (matcherHint.find()) {
 				if (matcherHint.groupCount() > 2 && matcherHint.group(3) != null) {
 					cache.hint = Pattern.compile("<br[^>]*>").matcher(matcherHint.group(3)).replaceAll("\n");
 					if (cache.hint != null) {
 						cache.hint = cache.hint.trim();
 					}
 				}
 			}
 		} catch (Exception e) {
 			// failed to parse hint
 			Log.w(cgSettings.tag, "cgeoBase.parseCache: Failed to parse cache hint");
 		}
 
 		// cache short description
 		try {
 			final Matcher matcherDescShort = patternDescShort.matcher(page);
 			while (matcherDescShort.find()) {
 				if (matcherDescShort.groupCount() > 0) {
 					int end = matcherDescShort.group(1).indexOf("</span>");
 
 					cache.shortdesc = matcherDescShort.group(1).substring(0, end);
 				}
 			}
 		} catch (Exception e) {
 			// failed to parse short description
 			Log.w(cgSettings.tag, "cgeoBase.parseCache: Failed to parse cache short description");
 		}
 
 		// cache description
 		try {
 			final Matcher matcherDesc = patternDesc.matcher(page);
 			while (matcherDesc.find()) {
 				if (matcherDesc.groupCount() > 0) {
 					cache.description = matcherDesc.group(1);
 				}
 			}
 		} catch (Exception e) {
 			// failed to parse short description
 			Log.w(cgSettings.tag, "cgeoBase.parseCache: Failed to parse cache description");
 		}
 
 		// cache attributes
 		try {
 			final Matcher matcherAttributes = patternAttributes.matcher(page);
 			while (matcherAttributes.find()) {
 				if (matcherAttributes.groupCount() > 0) {
 					final String attributesPre = matcherAttributes.group(1);
 					final Matcher matcherAttributesInside = patternAttributesInside.matcher(attributesPre);
 
 					while (matcherAttributesInside.find()) {
 						if (matcherAttributesInside.groupCount() > 0 && matcherAttributesInside.group(1).equalsIgnoreCase("blank") != true) {
 							cache.attributes.add(matcherAttributesInside.group(1).toLowerCase());
 						}
 					}
 				}
 			}
 		} catch (Exception e) {
 			// failed to parse cache attributes
 			Log.w(cgSettings.tag, "cgeoBase.parseCache: Failed to parse cache attributes");
 		}
 
 		// cache spoilers
 		try {
 			final Matcher matcherSpoilers = patternSpoilers.matcher(page);
 			while (matcherSpoilers.find()) {
 				if (matcherSpoilers.groupCount() > 0) {
 					final String spoilersPre = matcherSpoilers.group(1);
 					final Matcher matcherSpoilersInside = patternSpoilersInside.matcher(spoilersPre);
 
 					while (matcherSpoilersInside.find()) {
 						if (matcherSpoilersInside.groupCount() > 0) {
 							final cgSpoiler spoiler = new cgSpoiler();
 							spoiler.url = matcherSpoilersInside.group(1);
 							if (matcherSpoilersInside.group(2) != null) {
 								spoiler.title = matcherSpoilersInside.group(2);
 							}
 							if (matcherSpoilersInside.group(4) != null) {
 								spoiler.description = matcherSpoilersInside.group(4);
 							}
 
 							cache.spoilers.add(spoiler);
 						}
 					}
 				}
 			}
 		} catch (Exception e) {
 			// failed to parse cache spoilers
 			Log.w(cgSettings.tag, "cgeoBase.parseCache: Failed to parse cache spoilers");
 		}
 
 		// cache inventory
 		try {
 			cache.inventoryItems = 0;
 
 			final Matcher matcherInventory = patternInventory.matcher(page);
 			while (matcherInventory.find()) {
 				if (matcherInventory.groupCount() > 0) {
 					final String inventoryPre = matcherInventory.group(1);
 					final Matcher matcherInventoryInside = patternInventoryInside.matcher(inventoryPre);
 
 					while (matcherInventoryInside.find()) {
 						if (matcherInventoryInside.groupCount() > 0) {
 							final cgTrackable inventoryItem = new cgTrackable();
 							inventoryItem.guid = matcherInventoryInside.group(1);
 							inventoryItem.name = matcherInventoryInside.group(2);
 							cache.inventory.add(inventoryItem);
 							cache.inventoryItems++;
 						}
 					}
 				}
 			}
 		} catch (Exception e) {
 			// failed to parse cache inventory
 			Log.w(cgSettings.tag, "cgeoBase.parseCache: Failed to parse cache inventory");
 		}
 
 		// cache logs
 		try {
 			final Matcher matcherLogs = patternLogs.matcher(page);
 			while (matcherLogs.find()) {
 				if (matcherLogs.groupCount() > 0) {
 					final Pattern patternLog = Pattern.compile("<td[^>]*>[^<]*<strong>[^<]*<img src=[\"|'].*\\/icons\\/([^\\.]+)\\.[a-z]{2,5}[\"|'][^>]*>&nbsp;([a-zA-Z]+) (\\d+)(, (\\d+))? by <a href=[^>]+>([^<]+)</a>[<^]*</strong>([^\\(]*\\((\\d+) found\\))?(<br[ ]*/>)+(.*)(<br[ ]*/>)+<small><a href=");
 					final String[] logs = matcherLogs.group(1).split("<tr>");
 					final int logsCnt = logs.length;
 
 					for (int k = 1; k < logsCnt; k++) {
 						final Matcher matcherLog = patternLog.matcher(logs[k]);
 						if (matcherLog.find()) {
 							final cgLog logDone = new cgLog();
 
 							String logTmp = matcherLog.group(10);
 
 							logTmp = Pattern.compile("<p>").matcher(logTmp).replaceAll("\n");
 							logTmp = Pattern.compile("<br[^>]*>").matcher(logTmp).replaceAll("\n");
 							logTmp = Pattern.compile("<\\/p>").matcher(logTmp).replaceAll("");
 							logTmp = Pattern.compile("\r+").matcher(logTmp).replaceAll("\n");
 
 							int day = -1;
 							try {
 								day = Integer.parseInt(matcherLog.group(3));
 							} catch (Exception e) {
 								Log.w(cgSettings.tag, "Failed to parse logs date (day): " + e.toString());
 							}
 
 							int month = -1;
 							// January  | February  | March  | April  | May | June | July | August  | September | October  | November  | December
 							if (matcherLog.group(2).equalsIgnoreCase("January")) {
 								month = 0;
 							} else if (matcherLog.group(2).equalsIgnoreCase("February")) {
 								month = 1;
 							} else if (matcherLog.group(2).equalsIgnoreCase("March")) {
 								month = 2;
 							} else if (matcherLog.group(2).equalsIgnoreCase("April")) {
 								month = 3;
 							} else if (matcherLog.group(2).equalsIgnoreCase("May")) {
 								month = 4;
 							} else if (matcherLog.group(2).equalsIgnoreCase("June")) {
 								month = 5;
 							} else if (matcherLog.group(2).equalsIgnoreCase("July")) {
 								month = 6;
 							} else if (matcherLog.group(2).equalsIgnoreCase("August")) {
 								month = 7;
 							} else if (matcherLog.group(2).equalsIgnoreCase("September")) {
 								month = 8;
 							} else if (matcherLog.group(2).equalsIgnoreCase("October")) {
 								month = 9;
 							} else if (matcherLog.group(2).equalsIgnoreCase("November")) {
 								month = 10;
 							} else if (matcherLog.group(2).equalsIgnoreCase("December")) {
 								month = 11;
 							} else {
 								Log.w(cgSettings.tag, "Failed to parse logs date (month).");
 							}
 
 
 							int year = -1;
 							final String yearPre = matcherLog.group(5);
 
 							if (yearPre == null) {
 								Calendar date = Calendar.getInstance();
 								year = date.get(Calendar.YEAR);
 							} else {
 								try {
 									year = Integer.parseInt(matcherLog.group(5));
 								} catch (Exception e) {
 									Log.w(cgSettings.tag, "Failed to parse logs date (year): " + e.toString());
 								}
 							}
 
 							long logDate;
 							if (year > 0 && month >= 0 && day > 0) {
 								Calendar date = Calendar.getInstance();
 								date.set(year, month, day, 12, 0, 0);
 								logDate = date.getTimeInMillis();
 								logDate = (long) (Math.ceil(logDate / 1000)) * 1000;
 							} else {
 								logDate = 0;
 							}
 
 							if (logTypes.containsKey(matcherLog.group(1).toLowerCase()) == true) {
 								logDone.type = logTypes.get(matcherLog.group(1).toLowerCase());
 							} else {
 								logDone.type = logTypes.get("icon_note");
 							}
 
 							logDone.author = matcherLog.group(6);
 							logDone.date = logDate;
 							if (matcherLog.group(8) != null) {
 								logDone.found = new Integer(matcherLog.group(8));
 							}
 							logDone.log = logTmp;
 
 							cache.logs.add(logDone);
 						}
 					}
 				}
 			}
 		} catch (Exception e) {
 			// failed to parse logs
 			Log.w(cgSettings.tag, "cgeoBase.parseCache: Failed to parse cache logs");
 		}
 
 		int wpBegin = 0;
 		int wpEnd = 0;
 
 		wpBegin = page.indexOf("<table class=\"Table\" id=\"ctl00_ContentBody_Waypoints\">");
 		if (wpBegin != -1) { // parse waypoints
 			final Pattern patternWpType = Pattern.compile("\\/wpttypes\\/sm\\/(.+)\\.jpg", Pattern.CASE_INSENSITIVE);
 			final Pattern patternWpPrefixOrLookupOrLatlon = Pattern.compile(">([^<]*<[^>]+>)?([^<]+)(<[^>]+>[^<]*)?<\\/td>", Pattern.CASE_INSENSITIVE);
 			final Pattern patternWpName = Pattern.compile(">[^<]*<a[^>]+>([^<]*)<\\/a>", Pattern.CASE_INSENSITIVE);
 			final Pattern patternWpNote = Pattern.compile("colspan=\"6\">(.*)<\\/td>", Pattern.CASE_INSENSITIVE);
 
 			String wpList = page.substring(wpBegin);
 
 			wpEnd = wpList.indexOf("</p>");
 			if (wpEnd > -1 && wpEnd <= wpList.length()) {
 				wpList = wpList.substring(0, wpEnd);
 			}
 
 			if (wpList.indexOf("No additional waypoints to display.") == -1) {
 				wpEnd = wpList.indexOf("</table>");
 				wpList = wpList.substring(0, wpEnd);
 
 				wpBegin = wpList.indexOf("<tbody>");
 				wpEnd = wpList.indexOf("</tbody>");
 				if (wpBegin >= 0 && wpEnd >= 0 && wpEnd <= wpList.length()) {
 					wpList = wpList.substring(wpBegin + 7, wpEnd);
 				}
 
 				final String[] wpItems = wpList.split("<tr");
 
 				String[] wp;
 				for (int j = 1; j < wpItems.length; j++) {
 					final cgWaypoint waypoint = new cgWaypoint();
 
 					wp = wpItems[j].split("<td");
 
 					// waypoint type
 					try {
 						final Matcher matcherWpType = patternWpType.matcher(wp[3]);
 						while (matcherWpType.find()) {
 							if (matcherWpType.groupCount() > 0) {
 								waypoint.type = matcherWpType.group(1);
 							}
 						}
 					} catch (Exception e) {
 						// failed to parse type
 						Log.w(cgSettings.tag, "cgeoBase.parseCache: Failed to parse waypoint type");
 					}
 
 					// waypoint prefix
 					try {
 						final Matcher matcherWpPrefix = patternWpPrefixOrLookupOrLatlon.matcher(wp[4]);
 						while (matcherWpPrefix.find()) {
 							if (matcherWpPrefix.groupCount() > 1) {
 								waypoint.prefix = matcherWpPrefix.group(2);
 							}
 						}
 					} catch (Exception e) {
 						// failed to parse prefix
 						Log.w(cgSettings.tag, "cgeoBase.parseCache: Failed to parse waypoint prefix");
 					}
 
 					// waypoint lookup
 					try {
 						final Matcher matcherWpLookup = patternWpPrefixOrLookupOrLatlon.matcher(wp[5]);
 						while (matcherWpLookup.find()) {
 							if (matcherWpLookup.groupCount() > 1) {
 								waypoint.lookup = matcherWpLookup.group(2);
 							}
 						}
 					} catch (Exception e) {
 						// failed to parse lookup
 						Log.w(cgSettings.tag, "cgeoBase.parseCache: Failed to parse waypoint lookup");
 					}
 
 					// waypoint name
 					try {
 						final Matcher matcherWpName = patternWpName.matcher(wp[6]);
 						while (matcherWpName.find()) {
 							if (matcherWpName.groupCount() > 0) {
 								waypoint.name = matcherWpName.group(1);
 							}
 						}
 					} catch (Exception e) {
 						// failed to parse name
 						Log.w(cgSettings.tag, "cgeoBase.parseCache: Failed to parse waypoint name");
 					}
 
 					// waypoint latitude and logitude
 					try {
 						final Matcher matcherWpLatLon = patternWpPrefixOrLookupOrLatlon.matcher(wp[7]);
 						while (matcherWpLatLon.find()) {
 							if (matcherWpLatLon.groupCount() > 1) {
 								waypoint.latlon = Html.fromHtml(matcherWpLatLon.group(2)).toString();
 
 								final HashMap tmp = this.parseLatlon(waypoint.latlon);
 								if (tmp.size() > 0) {
 									waypoint.latitude = (Double) tmp.get("latitude");
 									waypoint.longitude = (Double) tmp.get("longitude");
 									waypoint.latitudeString = (String) tmp.get("latitudeString");
 									waypoint.longitudeString = (String) tmp.get("longitudeString");
 								}
 							}
 						}
 					} catch (Exception e) {
 						// failed to parse latitude and/or longitude
 						Log.w(cgSettings.tag, "cgeoBase.parseCache: Failed to parse waypoint coordinates");
 					}
 
 					j++;
 					if (wpItems.length > j) {
 						wp = wpItems[j].split("<td");
 					}
 
 					// waypoint note
 					try {
 						final Matcher matcherWpNote = patternWpNote.matcher(wp[3]);
 						while (matcherWpNote.find()) {
 							if (matcherWpNote.groupCount() > 0) {
 								waypoint.note = matcherWpNote.group(1);
 								if (waypoint.note != null) {
 									waypoint.note = waypoint.note.trim();
 								}
 							}
 						}
 					} catch (Exception e) {
 						// failed to parse note
 						Log.w(cgSettings.tag, "cgeoBase.parseCache: Failed to parse waypoint note");
 					}
 
 					cache.waypoints.add(waypoint);
 				}
 			}
 		}
 
 		final cgRating rating = getRating(cache.guid, cache.geocode);
 		if (rating != null) {
 			cache.rating = rating.rating;
 			cache.votes = rating.votes;
 			cache.vote = rating.vote;
 		}
 
 		cache.updated = System.currentTimeMillis();
 		cache.detailedUpdate = System.currentTimeMillis();
 		cache.detailed = true;
 		caches.cacheList.add(cache);
 
 		return caches;
 	}
 
 	public cgRating getRating(String guid, String geocode) {
 		ArrayList<String> guids = null;
 		ArrayList<String> geocodes = null;
 
 		if (guid != null && guid.length() > 0) {
 			guids = new ArrayList<String>();
 			guids.add(guid);
 		} else if (geocode != null && geocode.length() > 0) {
 			geocodes = new ArrayList<String>();
 			geocodes.add(geocode);
 		} else {
 			return null;
 		}
 
 		final HashMap<String, cgRating> ratings = getRating(guids, geocodes);
 		final Set<String> ratingKeys = ratings.keySet();
 		for (String ratingKey : ratingKeys) {
 			return ratings.get(ratingKey);
 		}
 
 		return null;
 	}
 
 	public HashMap<String, cgRating> getRating(ArrayList<String> guids, ArrayList<String> geocodes) {
 		if (guids == null && geocodes == null) {
 			return null;
 		}
 
 		final HashMap<String, cgRating> ratings = new HashMap<String, cgRating>();
 
 		final HashMap<String, String> params = new HashMap<String, String>();
 		if (settings.isLogin() == true) {
 			final HashMap<String, String> login = settings.getGCvoteLogin();
 			if (login != null) {
 				params.put("userName", login.get("username"));
 				params.put("password", login.get("password"));
 			}
 		}
 		if (guids != null && guids.size() > 0) {
 			params.put("cacheIds", implode(",", guids.toArray()));
 		} else {
 			params.put("waypoints", implode(",", geocodes.toArray()));
 		}
 		params.put("version", "cgeo");
 		final String votes = request("gcvote.com", "/getVotes.php", "GET", params, false, false, false);
 
 		final Pattern patternLoggedIn = Pattern.compile("loggedIn='([^']+)'", Pattern.CASE_INSENSITIVE);
 		final Pattern patternGuid = Pattern.compile("cacheId='([^']+)'", Pattern.CASE_INSENSITIVE);
 		final Pattern patternRating = Pattern.compile("voteAvg='([0-9\\.]+)'", Pattern.CASE_INSENSITIVE);
 		final Pattern patternVotes = Pattern.compile("voteCnt='([0-9]+)'", Pattern.CASE_INSENSITIVE);
 		final Pattern patternVote = Pattern.compile("voteUser='([0-9]+)'", Pattern.CASE_INSENSITIVE);
 
 		String voteData = null;
 		final Pattern patternVoteElement = Pattern.compile("<vote ([^>]+)>", Pattern.CASE_INSENSITIVE);
 		final Matcher matcherVoteElement = patternVoteElement.matcher(votes);
 		while (matcherVoteElement.find()) {
 			if (matcherVoteElement.groupCount() > 0) {
 				voteData = matcherVoteElement.group(1);
 			}
 
 			if (voteData == null) {
 				continue;
 			}
 
 			String guid = null;
 			cgRating rating = new cgRating();
 			boolean loggedIn = false;
 
 			try {
 				final Matcher matcherGuid = patternGuid.matcher(voteData);
 				if (matcherGuid.find()) {
 					if (matcherGuid.groupCount() > 0) {
 						guid = (String) matcherGuid.group(1);
 					}
 				}
 			} catch (Exception e) {
 				Log.w(cgSettings.tag, "cgBase.getRating: Failed to parse guid");
 			}
 
 			try {
 				final Matcher matcherLoggedIn = patternLoggedIn.matcher(votes);
 				if (matcherLoggedIn.find()) {
 					if (matcherLoggedIn.groupCount() > 0) {
 						if (matcherLoggedIn.group(1).equalsIgnoreCase("true") == true) {
 							loggedIn = true;
 						}
 					}
 				}
 			} catch (Exception e) {
 				Log.w(cgSettings.tag, "cgBase.getRating: Failed to parse loggedIn");
 			}
 
 			try {
 				final Matcher matcherRating = patternRating.matcher(voteData);
 				if (matcherRating.find()) {
 					if (matcherRating.groupCount() > 0) {
 						rating.rating = Float.parseFloat(matcherRating.group(1));
 					}
 				}
 			} catch (Exception e) {
 				Log.w(cgSettings.tag, "cgBase.getRating: Failed to parse rating");
 			}
 
 			try {
 				final Matcher matcherVotes = patternVotes.matcher(voteData);
 				if (matcherVotes.find()) {
 					if (matcherVotes.groupCount() > 0) {
 						rating.votes = Integer.parseInt(matcherVotes.group(1));
 					}
 				}
 			} catch (Exception e) {
 				Log.w(cgSettings.tag, "cgBase.getRating: Failed to parse vote count");
 			}
 
 			if (loggedIn == true) {
 				try {
 					final Matcher matcherVote = patternVote.matcher(voteData);
 					if (matcherVote.find()) {
 						if (matcherVote.groupCount() > 0) {
 							rating.vote = Integer.parseInt(matcherVote.group(1));
 						}
 					}
 				} catch (Exception e) {
 					Log.w(cgSettings.tag, "cgBase.getRating: Failed to parse user's vote");
 				}
 			}
 
 			if (guid != null) {
 				ratings.put(guid, rating);
 			}
 		}
 
 
 		return ratings;
 	}
 
 	public boolean setRating(String guid, int vote) {
 		if (guid == null || guid.length() == 0) {
 			return false;
 		}
 		if (vote < 0 || vote > 5) {
 			return false;
 		}
 
 		final HashMap<String, String> login = settings.getGCvoteLogin();
 		if (login == null) {
 			return false;
 		}
 
 		final HashMap<String, String> params = new HashMap<String, String>();
 		params.put("userName", login.get("username"));
 		params.put("password", login.get("password"));
 		params.put("cacheId", guid);
 		params.put("voteUser", Integer.toString(vote));
 		params.put("version", "cgeo");
 
 		final String result = request("gcvote.com", "/setVote.php", "GET", params, false, false, false);
 
 		if (result.trim().equalsIgnoreCase("ok") == true) {
 			return true;
 		}
 
 		return false;
 	}
 
 	public Long parseGPX(cgeoapplication app, File file, Handler handler) {
 		cgSearch search = new cgSearch();
 		long searchId = 0l;
 
 		try {
 			cgGPXParser GPXparser = new cgGPXParser(app, this, search);
 
 			searchId = GPXparser.parse(file, 10, handler);
 			if (searchId == 0l) {
 				searchId = GPXparser.parse(file, 11, handler);
 			}
 		} catch (Exception e) {
 			Log.e(cgSettings.tag, "cgBase.parseGPX: " + e.toString());
 		}
 
 		Log.i(cgSettings.tag, "Caches found in .gpx file: " + app.getCount(searchId));
 
 		return search.getCurrentId();
 	}
 
 	public cgTrackable parseTrackable(String page) {
 		if (page == null || page.length() == 0) {
 			Log.e(cgSettings.tag, "cgeoBase.parseTrackable: No page given");
 			return null;
 		}
 
 		final Pattern patternTrackableId = Pattern.compile("<a id=\"ctl00_ContentBody_LogLink\" title=\"[^\"]*\" href=\".*log\\.aspx\\?wid=([a-z0-9\\-]+)\"[^>]*>[^<]*</a>", Pattern.CASE_INSENSITIVE);
 		final Pattern patternGeocode = Pattern.compile("<span id=\"ctl00_ContentBody_BugDetails_BugTBNum\" String=\"[^\"]*\">Use[^<]*<strong>(TB[0-9a-z]+)[^<]*</strong> to reference this item.[^<]*</span>", Pattern.CASE_INSENSITIVE);
 		final Pattern patternName = Pattern.compile("<h2>([^<]*<img[^>]*>)?[^<]*<span id=\"ctl00_ContentBody_lbHeading\">([^<]+)</span>[^<]*</h2>", Pattern.CASE_INSENSITIVE);
 		final Pattern patternOwner = Pattern.compile("<dt>[\\s]*Owner:[^<]*</dt>[^<]*<dd>[^<]*<a id=\"ctl00_ContentBody_BugDetails_BugOwner\" title=\"[^\"]*\" href=\".*\\/profile\\/\\?guid=([a-z0-9\\-]+)\">([^<]+)<\\/a>[^<]*</dd>", Pattern.CASE_INSENSITIVE);
 		final Pattern patternReleased = Pattern.compile("<dt>[\\s]*Released:[^<]*</dt>[^<]*<dd>[^<]*<span id=\"ctl00_ContentBody_BugDetails_BugReleaseDate\">([^<]+)<\\/span>[^<]*</dd>", Pattern.CASE_INSENSITIVE);
 		final Pattern patternOrigin = Pattern.compile("<dt>[\\s]*Origin:[^<]*</dt>[^<]*<dd>[^<]*<span id=\"ctl00_ContentBody_BugDetails_BugOrigin\">([^<]+)<\\/span>[^<]*</dd>", Pattern.CASE_INSENSITIVE);
 		final Pattern patternSpotted = Pattern.compile("<dt>[\\s]*Recently Spotted:[^<]*</dt>[^<]*<dd>[^<]*<a id=\"ctl00_ContentBody_BugDetails_BugLocation\" title=\"[^\"]*\" href=\"[a-z0-9\\-\\_\\.\\?\\/\\:\\@]*\\/seek\\/cache_details.aspx\\?guid=([a-z0-9\\-]+)\">In ([^<]+)</a>[^<]*</dd>", Pattern.CASE_INSENSITIVE);
 		final Pattern patternGoal = Pattern.compile("<h3>[\\s]*Current GOAL[^<]*</h3>[^<]*<p[^>]*>(.*)</p>[^<]*<h3>[^\\w]*About This Item[^<]*</h3>", Pattern.CASE_INSENSITIVE);
 		final Pattern patternDetailsImage = Pattern.compile("<h3>[^\\w]*About This Item[^<]*</h3>([^<]*<p>[^<]*<img id=\"ctl00_ContentBody_BugDetails_BugImage\" class=\"[^\"]+\" src=\"([^\"]+)\"[^>]*>[^<]*</p>)?[^<]*<p[^>]*>(.*)</p>[^<]*<div id=\"ctl00_ContentBody_BugDetails_uxAbuseReport\">", Pattern.CASE_INSENSITIVE);
 
 		final cgTrackable trackable = new cgTrackable();
 
 		// trackable geocode
 		try {
 			final Matcher matcherGeocode = patternGeocode.matcher(page);
 			while (matcherGeocode.find()) {
 				if (matcherGeocode.groupCount() > 0) {
 					trackable.geocode = matcherGeocode.group(1).toUpperCase();
 				}
 			}
 		} catch (Exception e) {
 			// failed to parse trackable geocode
 			Log.w(cgSettings.tag, "cgeoBase.parseTrackable: Failed to parse trackable geocode");
 		}
 
 		// trackable id
 		try {
 			final Matcher matcherTrackableId = patternTrackableId.matcher(page);
 			while (matcherTrackableId.find()) {
 				if (matcherTrackableId.groupCount() > 0) {
 					trackable.guid = matcherTrackableId.group(1);
 				}
 			}
 		} catch (Exception e) {
 			// failed to parse trackable id
 			Log.w(cgSettings.tag, "cgeoBase.parseTrackable: Failed to parse trackable id");
 		}
 
 		// trackable type and name
 		try {
 			final Matcher matcherName = patternName.matcher(page);
 			while (matcherName.find()) {
 				if (matcherName.groupCount() > 1) {
 					trackable.name = matcherName.group(2);
 				}
 			}
 		} catch (Exception e) {
 			// failed to parse trackable type and name
 			Log.w(cgSettings.tag, "cgeoBase.parseTrackable: Failed to parse trackable type and name");
 		}
 
 		// trackable owner name
 		try {
 			final Matcher matcherOwner = patternOwner.matcher(page);
 			while (matcherOwner.find()) {
 				if (matcherOwner.groupCount() > 0) {
 					trackable.owner = matcherOwner.group(2);
 				}
 			}
 		} catch (Exception e) {
 			// failed to parse trackable owner name
 			Log.w(cgSettings.tag, "cgeoBase.parseTrackable: Failed to parse trackable owner name");
 		}
 
 		// trackable origin
 		try {
 			final Matcher matcherOrigin = patternOrigin.matcher(page);
 			while (matcherOrigin.find()) {
 				if (matcherOrigin.groupCount() > 0) {
 					trackable.origin = matcherOrigin.group(1);
 				}
 			}
 		} catch (Exception e) {
 			// failed to parse trackable origin
 			Log.w(cgSettings.tag, "cgeoBase.parseTrackable: Failed to parse trackable origin");
 		}
 
 		// trackable spotted
 		try {
 			final Matcher matcherSpotted = patternSpotted.matcher(page);
 			while (matcherSpotted.find()) {
 				if (matcherSpotted.groupCount() > 0) {
 					trackable.spottedGuid = matcherSpotted.group(1);
 					trackable.spottedName = matcherSpotted.group(2);
 				}
 			}
 		} catch (Exception e) {
 			// failed to parse trackable origin
 			Log.w(cgSettings.tag, "cgeoBase.parseTrackable: Failed to parse trackable origin");
 		}
 
 		// released
 		try {
 			final Matcher matcherReleased = patternReleased.matcher(page);
 			while (matcherReleased.find()) {
 				if (matcherReleased.groupCount() > 0 && matcherReleased.group(1) != null) {
 					try {
 						if (trackable.released == null) {
 							trackable.released = dateTbIn1.parse(matcherReleased.group(1));
 						}
 					} catch (Exception e) {
 						//
 					}
 
 					try {
 						if (trackable.released == null) {
 							trackable.released = dateTbIn2.parse(matcherReleased.group(1));
 						}
 					} catch (Exception e) {
 						//
 					}
 				}
 			}
 		} catch (Exception e) {
 			// failed to parse trackable released date
 			Log.w(cgSettings.tag, "cgeoBase.parseTrackable: Failed to parse trackable released date");
 		}
 
 		// trackable goal
 		try {
 			final Matcher matcherGoal = patternGoal.matcher(page);
 			while (matcherGoal.find()) {
 				if (matcherGoal.groupCount() > 0) {
 					trackable.goal = matcherGoal.group(1);
 				}
 			}
 		} catch (Exception e) {
 			// failed to parse trackable goal
 			Log.w(cgSettings.tag, "cgeoBase.parseTrackable: Failed to parse trackable goal");
 		}
 
 		// trackable details & image
 		try {
 			final Matcher matcherDetailsImage = patternDetailsImage.matcher(page);
 			while (matcherDetailsImage.find()) {
 				if (matcherDetailsImage.groupCount() > 0) {
 					final String image = matcherDetailsImage.group(2);
 					final String details = matcherDetailsImage.group(3);
 					if (image != null) {
 						trackable.image = image;
 					}
 					if (image != null) {
 						trackable.details = details;
 					}
 				}
 			}
 		} catch (Exception e) {
 			// failed to parse trackable details & image
 			Log.w(cgSettings.tag, "cgeoBase.parseTrackable: Failed to parse trackable details & image");
 		}
 
 		app.saveTrackable(trackable);
 
 		return trackable;
 	}
 
 	public ArrayList<Integer> parseTypes(String page) {
 		if (page == null || page.length() == 0) {
 			return null;
 		}
 
 		final ArrayList<Integer> types = new ArrayList<Integer>();
 
 		final Pattern typeBoxPattern = Pattern.compile("<select name=\"ctl00\\$ContentBody\\$LogBookPanel1\\$ddLogType\" id=\"ctl00_ContentBody_LogBookPanel1_ddLogType\"[^>]*>"
 				+ "(([^<]*<option[^>]*>[^<]+</option>)+)[^<]*</select>", Pattern.CASE_INSENSITIVE);
 		final Matcher typeBoxMatcher = typeBoxPattern.matcher(page);
 		String typesText = null;
 		if (typeBoxMatcher.find()) {
 			if (typeBoxMatcher.groupCount() > 0) {
 				typesText = typeBoxMatcher.group(1);
 			}
 		}
 
 		if (typesText != null) {
 			final Pattern typePattern = Pattern.compile("<option( selected=\"selected\")? value=\"(\\d+)\">[^<]+</option>", Pattern.CASE_INSENSITIVE);
 			final Matcher typeMatcher = typePattern.matcher(typesText);
 			while (typeMatcher.find()) {
 				if (typeMatcher.groupCount() > 1) {
 					final int type = Integer.parseInt(typeMatcher.group(2));
 
 					if (type > 0) {
 						types.add(type);
 					}
 				}
 			}
 		}
 
 		return types;
 	}
 
 	public ArrayList<cgTrackableLog> parseTrackableLog(String page) {
 		if (page == null || page.length() == 0) {
 			return null;
 		}
 
 		final ArrayList<cgTrackableLog> trackables = new ArrayList<cgTrackableLog>();
 
 		int startPos = -1;
 		int endPos = -1;
 
 		startPos = page.indexOf("<table id=\"tblTravelBugs\"");
 		if (startPos == -1) {
 			Log.e(cgSettings.tag, "cgeoBase.parseTrackableLog: ID \"tblTravelBugs\" not found on page");
 			return null;
 		}
 
 		page = page.substring(startPos); // cut on <table
 
 		endPos = page.indexOf("</table>");
 		if (endPos == -1) {
 			Log.e(cgSettings.tag, "cgeoBase.parseTrackableLog: end of ID \"tblTravelBugs\" not found on page");
 			return null;
 		}
 
 		page = page.substring(0, endPos); // cut on </table>
 
 		startPos = page.indexOf("<tbody>");
 		if (startPos == -1) {
 			Log.e(cgSettings.tag, "cgeoBase.parseTrackableLog: tbody not found on page");
 			return null;
 		}
 
 		page = page.substring(startPos); // cut on <tbody>
 
 		endPos = page.indexOf("</tbody>");
 		if (endPos == -1) {
 			Log.e(cgSettings.tag, "cgeoBase.parseTrackableLog: end of tbody not found on page");
 			return null;
 		}
 
 		page = page.substring(0, endPos); // cut on </tbody>
 
 		final Pattern trackablePattern = Pattern.compile("<tr id=\"ctl00_ContentBody_LogBookPanel1_uxTrackables_repTravelBugs_ctl[0-9]+_row\"[^>]*>"
 				+ "[^<]*<td>[^<]*<a href=\"[^\"]+\">([A-Z0-9]+)</a>[^<]*</td>[^<]*<td>([^<]+)</td>[^<]*<td>"
 				+ "[^<]*<select name=\"ctl00\\$ContentBody\\$LogBookPanel1\\$uxTrackables\\$repTravelBugs\\$ctl([0-9]+)\\$ddlAction\"[^>]*>"
 				+ "([^<]*<option value=\"([0-9]+)(_[a-z]+)?\">[^<]+</option>)+"
 				+ "[^<]*</select>[^<]*</td>[^<]*</tr>", Pattern.CASE_INSENSITIVE);
 		final Matcher trackableMatcher = trackablePattern.matcher(page);
 		while (trackableMatcher.find()) {
 			if (trackableMatcher.groupCount() > 0) {
 				final cgTrackableLog trackable = new cgTrackableLog();
 
 				if (trackableMatcher.group(1) != null) {
 					trackable.trackCode = trackableMatcher.group(1);
 				} else {
 					continue;
 				}
 				if (trackableMatcher.group(2) != null) {
 					trackable.name = Html.fromHtml(trackableMatcher.group(2)).toString();
 				} else {
 					continue;
 				}
 				if (trackableMatcher.group(3) != null) {
 					trackable.ctl = new Integer(trackableMatcher.group(3));
 				} else {
 					continue;
 				}
 				if (trackableMatcher.group(5) != null) {
 					trackable.id = new Integer(trackableMatcher.group(5));
 				} else {
 					continue;
 				}
 
 				Log.i(cgSettings.tag, "Trackable in inventory (#" + trackable.ctl + "/" + trackable.id + "): " + trackable.trackCode + " - " + trackable.name);
 
 				trackables.add(trackable);
 			}
 		}
 
 		return trackables;
 	}
 
 	public static String stripParagraphs(String text) {
 		if (text == null) {
 			return "";
 		}
 
 		final Pattern patternP = Pattern.compile("(<p>|</p>|<br \\/>|<br>)", Pattern.CASE_INSENSITIVE);
 		final Pattern patternP2 = Pattern.compile("([ ]+)", Pattern.CASE_INSENSITIVE);
 		final Matcher matcherP = patternP.matcher(text);
 		final Matcher matcherP2 = patternP2.matcher(text);
 
 		matcherP.replaceAll(" ");
 		matcherP2.replaceAll(" ");
 
 		return text.trim();
 	}
 
 	public static String stripTags(String text) {
 		if (text == null) {
 			return "";
 		}
 
 		final Pattern patternP = Pattern.compile("(<[^>]+>)", Pattern.CASE_INSENSITIVE);
 		final Matcher matcherP = patternP.matcher(text);
 
 		matcherP.replaceAll(" ");
 
 		return text.trim();
 	}
 
 	public static String capitalizeSentence(String sentence) {
 		if (sentence == null) {
 			return "";
 		}
 
 		final String[] word = sentence.split(" ");
 
 		for (int i = 0; i < word.length; i++) {
 			word[i] = capitalizeWord(word[i]);
 		}
 
 		return implode(" ", word);
 	}
 
 	public static String capitalizeWord(String word) {
 		if (word.length() == 0) {
 			return word;
 		}
 
 		return (word.substring(0, 1).toUpperCase() + word.substring(1).toLowerCase());
 	}
 	
 	public static Double parseDistance(String dst) {
 		Double distance = null;
 		
		final Pattern pattern = Pattern.compile("([0-9\\.,]+)[ ]+(km|mi)", Pattern.CASE_INSENSITIVE);
 		final Matcher matcher = pattern.matcher(dst);
 		while (matcher.find()) {
			if (matcher.groupCount() > 1) {
				if (matcher.group(2).equalsIgnoreCase("km") == true) {
					distance = new Double(matcher.group(1));
 				} else {
					distance = new Double(matcher.group(1)) / kmInMiles;
 				}
 			}
 		}
 		
 		return distance;
 	}
 
 	public static double getDistance(Double lat1, Double lon1, Double lat2, Double lon2) {
 		if (lat1 == null || lon1 == null || lat2 == null || lon2 == null) {
 			return 0d;
 		}
 
 		lat1 *= deg2rad;
 		lon1 *= deg2rad;
 		lat2 *= deg2rad;
 		lon2 *= deg2rad;
 
 		final double d = Math.sin(lat1) * Math.sin(lat2) + Math.cos(lat1) * Math.cos(lat2) * Math.cos(lon1 - lon2);
 		final double distance = erad * Math.acos(d); // distance in km
 
 		if (Double.isNaN(distance) == false && distance > 0) {
 			return distance;
 		} else {
 			return 0d;
 		}
 	}
 
 	public static Double getHeading(Double lat1, Double lon1, Double lat2, Double lon2) {
 		Double result = new Double(0);
 
 		int ilat1 = (int) Math.round(0.5 + lat1 * 360000);
 		int ilon1 = (int) Math.round(0.5 + lon1 * 360000);
 		int ilat2 = (int) Math.round(0.5 + lat2 * 360000);
 		int ilon2 = (int) Math.round(0.5 + lon2 * 360000);
 
 		lat1 *= deg2rad;
 		lon1 *= deg2rad;
 		lat2 *= deg2rad;
 		lon2 *= deg2rad;
 
 		if (ilat1 == ilat2 && ilon1 == ilon2) {
 			return new Double(result);
 		} else if (ilat1 == ilat2) {
 			if (ilon1 > ilon2) {
 				result = new Double(270);
 			} else {
 				result = new Double(90);
 			}
 		} else if (ilon1 == ilon2) {
 			if (ilat1 > ilat2) {
 				result = new Double(180);
 			}
 		} else {
 			Double c = Math.acos(Math.sin(lat2) * Math.sin(lat1) + Math.cos(lat2) * Math.cos(lat1) * Math.cos(lon2 - lon1));
 			Double A = Math.asin(Math.cos(lat2) * Math.sin(lon2 - lon1) / Math.sin(c));
 			result = new Double(A * rad2deg);
 			if (ilat2 > ilat1 && ilon2 > ilon1) {
 				// result don't need change
 			} else if (ilat2 < ilat1 && ilon2 < ilon1) {
 				result = 180f - result;
 			} else if (ilat2 < ilat1 && ilon2 > ilon1) {
 				result = 180f - result;
 			} else if (ilat2 > ilat1 && ilon2 < ilon1) {
 				result += 360f;
 			}
 		}
 
 		return result;
 	}
 
 	public HashMap<String, Double> getRadialDistance(Double latitude, Double longitude, Double bearing, Double distance) {
 		final Double rlat1 = latitude * deg2rad;
 		final Double rlon1 = longitude * deg2rad;
 		final Double rbearing = bearing * deg2rad;
 		final Double rdistance = distance / erad;
 
 		final Double rlat = Math.asin(Math.sin(rlat1) * Math.cos(rdistance) + Math.cos(rlat1) * Math.sin(rdistance) * Math.cos(rbearing));
 		final Double rlon = rlon1 + Math.atan2(Math.sin(rbearing) * Math.sin(rdistance) * Math.cos(rlat1), Math.cos(rdistance) - Math.sin(rlat1) * Math.sin(rlat));
 
 		HashMap<String, Double> result = new HashMap<String, Double>();
 		result.put("latitude", rlat * rad2deg);
 		result.put("longitude", rlon * rad2deg);
 
 		return result;
 	}
 
 	public String getHumanDistance(Float distance) {
 		if (distance == null) {
 			return "?";
 		}
 
 		return getHumanDistance(new Double(distance));
 	}
 
 	public String getHumanDistance(Double distance) {
 		if (distance == null) {
 			return "?";
 		}
 
 		if (settings.units == settings.unitsImperial) {
 			distance *= kmInMiles;
 			if (distance > 100) {
 				return String.format(Locale.getDefault(), "%.0f", new Double(Math.round(distance))) + " mi";
 			} else if (distance > 0.5) {
 				return String.format(Locale.getDefault(), "%.1f", new Double(Math.round(distance * 10.0) / 10.0)) + " mi";
 			} else if (distance > 0.1) {
 				return String.format(Locale.getDefault(), "%.2f", new Double(Math.round(distance * 100.0) / 100.0)) + " mi";
 			} else if (distance > 0.05) {
 				return String.format(Locale.getDefault(), "%.0f", new Double(Math.round(distance * 5280.0))) + " ft";
 			} else if (distance > 0.01) {
 				return String.format(Locale.getDefault(), "%.1f", new Double(Math.round(distance * 5280 * 10.0) / 10.0)) + " ft";
 			} else {
 				return String.format(Locale.getDefault(), "%.2f", new Double(Math.round(distance * 5280 * 100.0) / 100.0)) + " ft";
 			}
 		} else {
 			if (distance > 100) {
 				return String.format(Locale.getDefault(), "%.0f", new Double(Math.round(distance))) + " km";
 			} else if (distance > 10) {
 				return String.format(Locale.getDefault(), "%.1f", new Double(Math.round(distance * 10.0) / 10.0)) + " km";
 			} else if (distance > 1) {
 				return String.format(Locale.getDefault(), "%.2f", new Double(Math.round(distance * 100.0) / 100.0)) + " km";
 			} else if (distance > 0.1) {
 				return String.format(Locale.getDefault(), "%.0f", new Double(Math.round(distance * 1000.0))) + " m";
 			} else if (distance > 0.01) {
 				return String.format(Locale.getDefault(), "%.1f", new Double(Math.round(distance * 1000.0 * 10.0) / 10.0)) + " m";
 			} else {
 				return String.format(Locale.getDefault(), "%.2f", new Double(Math.round(distance * 1000.0 * 100.0) / 100.0)) + " m";
 			}
 		}
 	}
 
 	public String getHumanSpeed(float speed) {
 		double kph = speed * 3.6;
 		String unit = "kmh";
 
 		if (this.settings.units == this.settings.unitsImperial) {
 			kph *= kmInMiles;
 			unit = "mph";
 		}
 
 		if (kph < 10) {
 			return String.format(Locale.getDefault(), "%.1f", new Double((Math.round(kph * 10) / 10))) + " " + unit;
 		} else {
 			return String.format(Locale.getDefault(), "%.0f", new Double(Math.round(kph))) + " " + unit;
 		}
 	}
 
 	public HashMap parseLatlon(String latlon) {
 		final HashMap result = new HashMap();
 		final Pattern patternLatlon = Pattern.compile("([NS])[^\\d]*(\\d+)[^°]*° (\\d+)\\.(\\d+) ([WE])[^\\d]*(\\d+)[^°]*° (\\d+)\\.(\\d+)", Pattern.CASE_INSENSITIVE);
 		final Matcher matcherLatlon = patternLatlon.matcher(latlon);
 
 		while (matcherLatlon.find()) {
 			if (matcherLatlon.groupCount() > 0) {
 				result.put("latitudeString", (String) (matcherLatlon.group(1) + " " + matcherLatlon.group(2) + "° " + matcherLatlon.group(3) + "." + matcherLatlon.group(4)));
 				result.put("longitudeString", (String) (matcherLatlon.group(5) + " " + matcherLatlon.group(6) + "° " + matcherLatlon.group(7) + "." + matcherLatlon.group(8)));
 				int latNegative = -1;
 				int lonNegative = -1;
 				if (matcherLatlon.group(1).equalsIgnoreCase("N")) {
 					latNegative = 1;
 				}
 				if (matcherLatlon.group(5).equalsIgnoreCase("E")) {
 					lonNegative = 1;
 				}
 				result.put("latitude", new Double(latNegative * (new Float(matcherLatlon.group(2)) + new Float(matcherLatlon.group(3) + "." + matcherLatlon.group(4)) / 60)));
 				result.put("longitude", new Double(lonNegative * (new Float(matcherLatlon.group(6)) + new Float(matcherLatlon.group(7) + "." + matcherLatlon.group(8)) / 60)));
 			} else {
 				Log.w(cgSettings.tag, "cgBase.parseLatlon: Failed to parse coordinates.");
 			}
 		}
 
 		return result;
 	}
 
 	public String formatCoordinate(Double coord, String latlon, boolean degrees) {
 		String formatted = "";
 
 		if (coord == null) {
 			return formatted;
 		}
 
 		String worldSide = "";
 		if (latlon.equalsIgnoreCase("lat") == true) {
 			if (coord >= 0) {
 				worldSide = "N";
 			} else {
 				worldSide = "S";
 			}
 		} else if (latlon.equalsIgnoreCase("lon") == true) {
 			if (coord >= 0) {
 				worldSide = "E";
 			} else {
 				worldSide = "W";
 			}
 		}
 
 		coord = Math.abs(coord);
 
 		if (latlon.equalsIgnoreCase("lat") == true) {
 			if (degrees == true) {
 				formatted = worldSide + " " + String.format(Locale.getDefault(), "%02.0f", Math.floor(coord)) + "° " + String.format(Locale.getDefault(), "%06.3f", ((coord - Math.floor(coord)) * 60));
 			} else {
 				formatted = worldSide + " " + String.format(Locale.getDefault(), "%02.0f", Math.floor(coord)) + " " + String.format(Locale.getDefault(), "%06.3f", ((coord - Math.floor(coord)) * 60));
 			}
 		} else {
 			if (degrees == true) {
 				formatted = worldSide + " " + String.format(Locale.getDefault(), "%03.0f", Math.floor(coord)) + "° " + String.format(Locale.getDefault(), "%06.3f", ((coord - Math.floor(coord)) * 60));
 			} else {
 				formatted = worldSide + " " + String.format(Locale.getDefault(), "%03.0f", Math.floor(coord)) + " " + String.format(Locale.getDefault(), "%06.3f", ((coord - Math.floor(coord)) * 60));
 			}
 		}
 
 		return formatted;
 	}
 
 	public HashMap parseCoordinate(String coord, String latlon) {
 		final HashMap coords = new HashMap();
 
 		final Pattern patternA = Pattern.compile("^([NSWE])[^\\d]*(\\d+)°? +(\\d+)([\\.|,](\\d+))?$", Pattern.CASE_INSENSITIVE);
 		final Pattern patternB = Pattern.compile("^([NSWE])[^\\d]*(\\d+)([\\.|,](\\d+))?$", Pattern.CASE_INSENSITIVE);
 		final Pattern patternC = Pattern.compile("^(-?\\d+)([\\.|,](\\d+))?$", Pattern.CASE_INSENSITIVE);
 		final Pattern patternD = Pattern.compile("^([NSWE])[^\\d]*(\\d+)°?$", Pattern.CASE_INSENSITIVE);
 		final Pattern patternE = Pattern.compile("^(-?\\d+)°?$", Pattern.CASE_INSENSITIVE);
 		final Pattern patternF = Pattern.compile("^([NSWE])[^\\d]*(\\d+)$", Pattern.CASE_INSENSITIVE);
 		final Pattern pattern0 = Pattern.compile("^(-?\\d+)([\\.|,](\\d+))?$", Pattern.CASE_INSENSITIVE);
 
 		coord = coord.trim().toUpperCase();
 
 		final Matcher matcherA = patternA.matcher(coord);
 		final Matcher matcherB = patternB.matcher(coord);
 		final Matcher matcherC = patternC.matcher(coord);
 		final Matcher matcherD = patternD.matcher(coord);
 		final Matcher matcherE = patternE.matcher(coord);
 		final Matcher matcherF = patternF.matcher(coord);
 		final Matcher matcher0 = pattern0.matcher(coord);
 
 		int latlonNegative;
 		if (matcherA.find() == true && matcherA.groupCount() > 0) {
 			if (matcherA.group(1).equalsIgnoreCase("N") || matcherA.group(1).equalsIgnoreCase("E")) {
 				latlonNegative = 1;
 			} else {
 				latlonNegative = -1;
 			}
 
 			if (matcherA.groupCount() < 5 || matcherA.group(5) == null) {
 				coords.put("coordinate", new Double(latlonNegative * (new Double(matcherA.group(2)) + new Double(matcherA.group(3) + ".0") / 60)));
 				coords.put("string", matcherA.group(1) + " " + matcherA.group(2) + "° " + matcherA.group(3) + ".000");
 			} else {
 				coords.put("coordinate", new Double(latlonNegative * (new Double(matcherA.group(2)) + new Double(matcherA.group(3) + "." + matcherA.group(5)) / 60)));
 				coords.put("string", matcherA.group(1) + " " + matcherA.group(2) + "° " + matcherA.group(3) + "." + matcherA.group(5));
 			}
 
 			return coords;
 		} else if (matcherB.find() == true && matcherB.groupCount() > 0) {
 			if (matcherB.group(1).equalsIgnoreCase("N") || matcherB.group(1).equalsIgnoreCase("E")) {
 				latlonNegative = 1;
 			} else {
 				latlonNegative = -1;
 			}
 
 			if (matcherB.groupCount() < 4 || matcherB.group(4) == null) {
 				coords.put("coordinate", new Double(latlonNegative * (new Double(matcherB.group(2) + ".0"))));
 			} else {
 				coords.put("coordinate", new Double(latlonNegative * (new Double(matcherB.group(2) + "." + matcherB.group(4)))));
 			}
 		} else if (matcherC.find() == true && matcherC.groupCount() > 0) {
 			if (matcherC.groupCount() < 3 || matcherC.group(3) == null) {
 				coords.put("coordinate", new Double(new Float(matcherC.group(1) + ".0")));
 			} else {
 				coords.put("coordinate", new Double(new Float(matcherC.group(1) + "." + matcherC.group(3))));
 			}
 		} else if (matcherD.find() == true && matcherD.groupCount() > 0) {
 			if (matcherD.group(1).equalsIgnoreCase("N") || matcherD.group(1).equalsIgnoreCase("E")) {
 				latlonNegative = 1;
 			} else {
 				latlonNegative = -1;
 			}
 
 			coords.put("coordinate", new Double(latlonNegative * (new Double(matcherB.group(2)))));
 		} else if (matcherE.find() == true && matcherE.groupCount() > 0) {
 			coords.put("coordinate", new Double(matcherE.group(1)));
 		} else if (matcherF.find() == true && matcherF.groupCount() > 0) {
 			if (matcherF.group(1).equalsIgnoreCase("N") || matcherF.group(1).equalsIgnoreCase("E")) {
 				latlonNegative = 1;
 			} else {
 				latlonNegative = -1;
 			}
 
 			coords.put("coordinate", new Double(latlonNegative * (new Double(matcherB.group(2)))));
 		} else {
 			return null;
 		}
 
 		if (matcher0.find() == true && matcher0.groupCount() > 0) {
 			String tmpDir = null;
 			Float tmpCoord;
 			if (matcher0.groupCount() < 3 || matcher0.group(3) == null) {
 				tmpCoord = new Float("0.0");
 			} else {
 				tmpCoord = new Float("0." + matcher0.group(3));
 			}
 
 			if (latlon.equalsIgnoreCase("lat")) {
 				if (matcher0.group(1).equals("+")) {
 					tmpDir = "N";
 				}
 				if (matcher0.group(1).equals("-")) {
 					tmpDir = "S";
 				}
 			} else if (latlon.equalsIgnoreCase("lon")) {
 				if (matcher0.group(1).equals("+")) {
 					tmpDir = "E";
 				}
 				if (matcher0.group(1).equals("-")) {
 					tmpDir = "W";
 				}
 			}
 
 			coords.put("string", tmpDir + " " + matcher0.group(1) + "° " + (Math.round(tmpCoord / (1 / 60) * 1000) * 1000));
 
 			return coords;
 		} else {
 			return new HashMap();
 		}
 	}
 
 	public Long searchByNextPage(Long searchId, int reason) {
 		final String viewstate = app.getViewstate(searchId);
 		final String viewstate1 = app.getViewstate1(searchId);
 		cgCacheWrap caches = new cgCacheWrap();
 		String url = app.getUrl(searchId);
 
 		if (url == null || url.length() == 0) {
 			Log.e(cgSettings.tag, "cgeoBase.searchByNextPage: No url found");
 			return searchId;
 		}
 
 		if (viewstate == null || viewstate.length() == 0) {
 			Log.e(cgSettings.tag, "cgeoBase.searchByNextPage: No viewstate given");
 			return searchId;
 		}
 
 		String host = "www.geocaching.com";
 		String path = "/";
 		final String method = "POST";
 
 		int dash = -1;
 		if (url.indexOf("http://") > -1) {
 			url = url.substring(7);
 		}
 
 		dash = url.indexOf("/");
 		if (dash > -1) {
 			host = url.substring(0, dash);
 			url = url.substring(dash);
 		} else {
 			host = url;
 			url = "";
 		}
 
 		dash = url.indexOf("?");
 		if (dash > -1) {
 			path = url.substring(0, dash);
 		} else {
 			path = url;
 		}
 
 		final HashMap<String, String> params = new HashMap<String, String>();
 		params.put("__VIEWSTATE", viewstate);
 		if (viewstate1 != null) {
 			params.put("__VIEWSTATE1", viewstate1);
 			params.put("__VIEWSTATEFIELDCOUNT", "2");
 		}
 		params.put("__EVENTTARGET", "ctl00$ContentBody$pgrBottom$ctl08");
 		params.put("__EVENTARGUMENT", "");
 
 		String page = request(host, path, method, params, false, false, true);
 		if (checkLogin(page) == false) {
 			int loginState = login();
 			if (loginState == 1) {
 				page = request(host, path, method, params, false, false, true);
 			} else if (loginState == -3) {
 				Log.i(cgSettings.tag, "Working as guest.");
 			} else {
 				app.setError(searchId, errorRetrieve.get(loginState));
 				Log.e(cgSettings.tag, "cgeoBase.searchByNextPage: Can not log in geocaching");
 				return searchId;
 			}
 		}
 
 		if (page == null || page.length() == 0) {
 			Log.e(cgSettings.tag, "cgeoBase.searchByNextPage: No data from server");
 			return searchId;
 		}
 
 		caches = parseSearch(url, page);
 		if (caches == null || caches.cacheList == null || caches.cacheList.isEmpty()) {
 			Log.e(cgSettings.tag, "cgeoBase.searchByNextPage: No cache parsed");
 			return searchId;
 		}
 
 		// save to application
 		app.setError(searchId, caches.error);
 		app.setViewstate(searchId, caches.viewstate);
 		app.setViewstate1(searchId, caches.viewstate1);
 
 		final ArrayList<cgCache> cacheList = new ArrayList<cgCache>();
 		for (cgCache cache : caches.cacheList) {
 			app.addGeocode(searchId, cache.geocode);
 			cacheList.add(cache);
 		}
 
 		app.addSearch(searchId, cacheList, true, reason);
 
 		return searchId;
 	}
 
 	public Long searchByGeocode(HashMap<String, String> parameters, int reason, boolean forceReload) {
 		final cgSearch search = new cgSearch();
 		String geocode = parameters.get("geocode");
 		String guid = parameters.get("guid");
 
 		if ((geocode == null || geocode.length() == 0) && ((guid == null || guid.length() == 0))) {
 			Log.e(cgSettings.tag, "cgeoBase.searchByGeocode: No geocode nor guid given");
 			return null;
 		}
 
 		if (forceReload == false && reason == 0 && (app.isOffline(geocode, guid) == true || app.isThere(geocode, guid, true, true) == true)) {
 			if ((geocode == null || geocode.length() == 0) && guid != null && guid.length() > 0) {
 				geocode = app.getGeocode(guid);
 			}
 
 			ArrayList<cgCache> cacheList = new ArrayList<cgCache>();
 			cacheList.add(app.getCacheByGeocode(geocode, true, true, true, true, true, true));
 			search.addGeocode(geocode);
 
 			app.addSearch(search, cacheList, false, reason);
 
 			cacheList.clear();
 			cacheList = null;
 
 			return search.getCurrentId();
 		}
 
 		final String host = "www.geocaching.com";
 		final String path = "/seek/cache_details.aspx";
 		final String method = "GET";
 		final HashMap<String, String> params = new HashMap<String, String>();
 		if (geocode != null && geocode.length() > 0) {
 			params.put("wp", geocode);
 		} else if (guid != null && guid.length() > 0) {
 			params.put("guid", guid);
 		}
 		params.put("decrypt", "y");
 		params.put("log", "y"); // download logs (more than 5
 		params.put("numlogs", "35"); // 35 logs
 
 		String page = requestLogged(host, path, method, params, false, false, false);
 
 		if (page == null || page.length() == 0) {
 			if (app.isThere(geocode, guid, true, false) == true) {
 				if ((geocode == null || geocode.length() == 0) && guid != null && guid.length() > 0) {
 					Log.i(cgSettings.tag, "Loading old cache from cache.");
 
 					geocode = app.getGeocode(guid);
 				}
 
 				final ArrayList<cgCache> cacheList = new ArrayList<cgCache>();
 				cacheList.add(app.getCacheByGeocode(geocode));
 				search.addGeocode(geocode);
 				search.error = null;
 				search.errorRetrieve = 0; // reset errors from previous failed request
 
 				app.addSearch(search, cacheList, false, reason);
 
 				cacheList.clear();
 
 				return search.getCurrentId();
 			}
 
 			Log.e(cgSettings.tag, "cgeoBase.searchByGeocode: No data from server");
 			return null;
 		}
 
 		final cgCacheWrap caches = parseCache(page, reason);
 		if (caches == null || caches.cacheList == null || caches.cacheList.isEmpty()) {
 			if (caches.error != null && caches.error.length() > 0) {
 				search.error = caches.error;
 			}
 			if (caches.url != null && caches.url.length() > 0) {
 				search.url = caches.url;
 			}
 
 			app.addSearch(search, null, true, reason);
 
 			Log.e(cgSettings.tag, "cgeoBase.searchByGeocode: No cache parsed");
 			return null;
 		}
 
 		if (app == null) {
 			Log.e(cgSettings.tag, "cgeoBase.searchByGeocode: No application found");
 			return null;
 		}
 
 		final ArrayList<cgCache> cacheList = new ArrayList<cgCache>();
 		if (caches != null) {
 			if (caches.error != null && caches.error.length() > 0) {
 				search.error = caches.error;
 			}
 			if (caches.url != null && caches.url.length() > 0) {
 				search.url = caches.url;
 			}
 			if (caches.viewstate != null && caches.viewstate.length() > 0) {
 				search.viewstate = caches.viewstate;
 			}
 			if (caches.viewstate1 != null && caches.viewstate1.length() > 0) {
 				search.viewstate1 = caches.viewstate1;
 			}
 			search.totalCnt = caches.totalCnt;
 
 			for (cgCache cache : caches.cacheList) {
 				search.addGeocode(cache.geocode);
 				cacheList.add(cache);
 			}
 		}
 
 		app.addSearch(search, cacheList, true, reason);
 
 		page = null;
 		cacheList.clear();
 
 		return search.getCurrentId();
 	}
 
 	public Long searchByOffline(HashMap<String, Object> parameters) {
 		if (app == null) {
 			Log.e(cgSettings.tag, "cgeoBase.searchByOffline: No application found");
 			return null;
 		}
 
 		Double latitude = null;
 		Double longitude = null;
 		String cachetype = null;
 
 		if (parameters.containsKey("latitude") == true && parameters.containsKey("longitude") == true) {
 			latitude = (Double) parameters.get("latitude");
 			longitude = (Double) parameters.get("longitude");
 		}
 
 		if (parameters.containsKey("cachetype") == true) {
 			cachetype = (String) parameters.get("cachetype");
 		}
 
 		final cgSearch search = app.getBatchOfStoredCaches(true, latitude, longitude, cachetype);
 		search.totalCnt = app.getAllStoredCachesCount(true, cachetype);
 
 		return search.getCurrentId();
 	}
 
 	public Long searchByCoords(HashMap<String, String> parameters, int reason) {
 		final cgSearch search = new cgSearch();
 		final String latitude = parameters.get("latitude");
 		final String longitude = parameters.get("longitude");
 		cgCacheWrap caches = new cgCacheWrap();
 		String cacheType = parameters.get("cachetype");
 
 		if (latitude == null || latitude.length() == 0) {
 			Log.e(cgSettings.tag, "cgeoBase.searchByCoords: No latitude given");
 			return null;
 		}
 
 		if (longitude == null || longitude.length() == 0) {
 			Log.e(cgSettings.tag, "cgeoBase.searchByCoords: No longitude given");
 			return null;
 		}
 
 		if (cacheType != null && cacheType.length() == 0) {
 			cacheType = null;
 		}
 
 		final String host = "www.geocaching.com";
 		final String path = "/seek/nearest.aspx";
 		final String method = "GET";
 		final HashMap<String, String> params = new HashMap<String, String>();
 		if (cacheType != null && cacheIDs.containsKey(cacheType) == true) {
 			params.put("tx", cacheIDs.get(cacheType));
 		} else {
 			params.put("tx", cacheIDs.get("all"));
 		}
 		params.put("lat", latitude);
 		params.put("lng", longitude);
 
 		final String url = "http://" + host + path + "?" + prepareParameters(params, false, true);
 		String page = requestLogged(host, path, method, params, false, false, true);
 
 		if (page == null || page.length() == 0) {
 			Log.e(cgSettings.tag, "cgeoBase.searchByCoords: No data from server");
 			return null;
 		}
 
 		caches = parseSearch(url, page);
 		if (caches == null || caches.cacheList == null || caches.cacheList.isEmpty()) {
 			Log.e(cgSettings.tag, "cgeoBase.searchByCoords: No cache parsed");
 		}
 
 		if (app == null) {
 			Log.e(cgSettings.tag, "cgeoBase.searchByCoords: No application found");
 			return null;
 		}
 
 		final ArrayList<cgCache> cacheList = new ArrayList<cgCache>();
 		if (caches != null) {
 			if (caches.error != null && caches.error.length() > 0) {
 				search.error = caches.error;
 			}
 			if (caches.url != null && caches.url.length() > 0) {
 				search.url = caches.url;
 			}
 			if (caches.viewstate != null && caches.viewstate.length() > 0) {
 				search.viewstate = caches.viewstate;
 			}
 			if (caches.viewstate1 != null && caches.viewstate1.length() > 0) {
 				search.viewstate1 = caches.viewstate1;
 			}
 			search.totalCnt = caches.totalCnt;
 
 			for (cgCache cache : caches.cacheList) {
 				if (settings.excludeDisabled == 0 || (settings.excludeDisabled == 1 && cache.disabled == false)) {
 					search.addGeocode(cache.geocode);
 					cacheList.add(cache);
 				}
 			}
 		}
 
 		app.addSearch(search, cacheList, true, reason);
 
 		return search.getCurrentId();
 	}
 
 	public Long searchByKeyword(HashMap<String, String> parameters, int reason) {
 		final cgSearch search = new cgSearch();
 		final String keyword = parameters.get("keyword");
 		cgCacheWrap caches = new cgCacheWrap();
 		String cacheType = parameters.get("cachetype");
 
 		if (keyword == null || keyword.length() == 0) {
 			Log.e(cgSettings.tag, "cgeoBase.searchByKeyword: No keyword given");
 			return null;
 		}
 
 		if (cacheType != null && cacheType.length() == 0) {
 			cacheType = null;
 		}
 
 		final String host = "www.geocaching.com";
 		final String path = "/seek/nearest.aspx";
 		final String method = "GET";
 		final HashMap<String, String> params = new HashMap<String, String>();
 		if (cacheType != null && cacheIDs.containsKey(cacheType) == true) {
 			params.put("tx", cacheIDs.get(cacheType));
 		} else {
 			params.put("tx", cacheIDs.get("all"));
 		}
 		params.put("key", keyword);
 
 		final String url = "http://" + host + path + "?" + prepareParameters(params, false, true);
 		String page = requestLogged(host, path, method, params, false, false, true);
 
 		if (page == null || page.length() == 0) {
 			Log.e(cgSettings.tag, "cgeoBase.searchByKeyword: No data from server");
 			return null;
 		}
 
 		caches = parseSearch(url, page);
 		if (caches == null || caches.cacheList == null || caches.cacheList.isEmpty()) {
 			Log.e(cgSettings.tag, "cgeoBase.searchByKeyword: No cache parsed");
 		}
 
 		if (app == null) {
 			Log.e(cgSettings.tag, "cgeoBase.searchByCoords: No application found");
 			return null;
 		}
 
 		final ArrayList<cgCache> cacheList = new ArrayList<cgCache>();
 		if (caches != null) {
 			if (caches.error != null && caches.error.length() > 0) {
 				search.error = caches.error;
 			}
 			if (caches.url != null && caches.url.length() > 0) {
 				search.url = caches.url;
 			}
 			if (caches.viewstate != null && caches.viewstate.length() > 0) {
 				search.viewstate = caches.viewstate;
 			}
 			if (caches.viewstate1 != null && caches.viewstate1.length() > 0) {
 				search.viewstate1 = caches.viewstate1;
 			}
 			search.totalCnt = caches.totalCnt;
 
 			for (cgCache cache : caches.cacheList) {
 				if (settings.excludeDisabled == 0 || (settings.excludeDisabled == 1 && cache.disabled == false)) {
 					search.addGeocode(cache.geocode);
 					cacheList.add(cache);
 				}
 			}
 		}
 
 		app.addSearch(search, cacheList, true, reason);
 
 		return search.getCurrentId();
 	}
 
 	public Long searchByUsername(HashMap<String, String> parameters, int reason) {
 		final cgSearch search = new cgSearch();
 		final String userName = parameters.get("username");
 		cgCacheWrap caches = new cgCacheWrap();
 		String cacheType = parameters.get("cachetype");
 
 		if (userName == null || userName.length() == 0) {
 			Log.e(cgSettings.tag, "cgeoBase.searchByUsername: No user name given");
 			return null;
 		}
 
 		if (cacheType != null && cacheType.length() == 0) {
 			cacheType = null;
 		}
 
 		final String host = "www.geocaching.com";
 		final String path = "/seek/nearest.aspx";
 		final String method = "GET";
 		final HashMap<String, String> params = new HashMap<String, String>();
 		if (cacheType != null && cacheIDs.containsKey(cacheType) == true) {
 			params.put("tx", cacheIDs.get(cacheType));
 		} else {
 			params.put("tx", cacheIDs.get("all"));
 		}
 		params.put("ul", userName);
 
 		boolean my = false;
 		if (userName.equalsIgnoreCase(settings.getLogin().get("username")) == true) {
 			my = true;
 			Log.i(cgSettings.tag, "cgBase.searchByUsername: Overriding users choice, downloading all caches.");
 		}
 
 		final String url = "http://" + host + path + "?" + prepareParameters(params, my, true);
 		String page = requestLogged(host, path, method, params, false, my, true);
 
 		if (page == null || page.length() == 0) {
 			Log.e(cgSettings.tag, "cgeoBase.searchByUsername: No data from server");
 			return null;
 		}
 
 		caches = parseSearch(url, page);
 		if (caches == null || caches.cacheList == null || caches.cacheList.isEmpty()) {
 			Log.e(cgSettings.tag, "cgeoBase.searchByUsername: No cache parsed");
 		}
 
 		if (app == null) {
 			Log.e(cgSettings.tag, "cgeoBase.searchByCoords: No application found");
 			return null;
 		}
 
 		final ArrayList<cgCache> cacheList = new ArrayList<cgCache>();
 		if (caches != null) {
 			if (caches.error != null && caches.error.length() > 0) {
 				search.error = caches.error;
 			}
 			if (caches.url != null && caches.url.length() > 0) {
 				search.url = caches.url;
 			}
 			if (caches.viewstate != null && caches.viewstate.length() > 0) {
 				search.viewstate = caches.viewstate;
 			}
 			if (caches.viewstate1 != null && caches.viewstate1.length() > 0) {
 				search.viewstate1 = caches.viewstate1;
 			}
 			search.totalCnt = caches.totalCnt;
 
 			for (cgCache cache : caches.cacheList) {
 				if (settings.excludeDisabled == 0 || (settings.excludeDisabled == 1 && cache.disabled == false)) {
 					search.addGeocode(cache.geocode);
 					cacheList.add(cache);
 				}
 			}
 		}
 
 		app.addSearch(search, cacheList, true, reason);
 
 		return search.getCurrentId();
 	}
 
 	public Long searchByOwner(HashMap<String, String> parameters, int reason) {
 		final cgSearch search = new cgSearch();
 		final String userName = parameters.get("username");
 		cgCacheWrap caches = new cgCacheWrap();
 		String cacheType = parameters.get("cachetype");
 
 		if (userName == null || userName.length() == 0) {
 			Log.e(cgSettings.tag, "cgeoBase.searchByOwner: No user name given");
 			return null;
 		}
 
 		if (cacheType != null && cacheType.length() == 0) {
 			cacheType = null;
 		}
 
 		final String host = "www.geocaching.com";
 		final String path = "/seek/nearest.aspx";
 		final String method = "GET";
 		final HashMap<String, String> params = new HashMap<String, String>();
 		if (cacheType != null && cacheIDs.containsKey(cacheType) == true) {
 			params.put("tx", cacheIDs.get(cacheType));
 		} else {
 			params.put("tx", cacheIDs.get("all"));
 		}
 		params.put("u", userName);
 
 		final String url = "http://" + host + path + "?" + prepareParameters(params, false, true);
 		String page = requestLogged(host, path, method, params, false, false, true);
 
 		if (page == null || page.length() == 0) {
 			Log.e(cgSettings.tag, "cgeoBase.searchByOwner: No data from server");
 			return null;
 		}
 
 		caches = parseSearch(url, page);
 		if (caches == null || caches.cacheList == null || caches.cacheList.isEmpty()) {
 			Log.e(cgSettings.tag, "cgeoBase.searchByOwner: No cache parsed");
 		}
 
 		if (app == null) {
 			Log.e(cgSettings.tag, "cgeoBase.searchByCoords: No application found");
 			return null;
 		}
 
 		final ArrayList<cgCache> cacheList = new ArrayList<cgCache>();
 		if (caches != null) {
 			if (caches.error != null && caches.error.length() > 0) {
 				search.error = caches.error;
 			}
 			if (caches.url != null && caches.url.length() > 0) {
 				search.url = caches.url;
 			}
 			if (caches.viewstate != null && caches.viewstate.length() > 0) {
 				search.viewstate = caches.viewstate;
 			}
 			if (caches.viewstate1 != null && caches.viewstate1.length() > 0) {
 				search.viewstate1 = caches.viewstate1;
 			}
 			search.totalCnt = caches.totalCnt;
 
 			for (cgCache cache : caches.cacheList) {
 				if (settings.excludeDisabled == 0 || (settings.excludeDisabled == 1 && cache.disabled == false)) {
 					search.addGeocode(cache.geocode);
 					cacheList.add(cache);
 				}
 			}
 		}
 
 		app.addSearch(search, cacheList, true, reason);
 
 		return search.getCurrentId();
 	}
 
 	public Long searchByViewport(HashMap<String, String> parameters, int reason) {
 		final cgSearch search = new cgSearch();
 		final String latitudeT = parameters.get("latitude-t");
 		final String latitudeB = parameters.get("latitude-b");
 		final String longitudeL = parameters.get("longitude-l");
 		final String longitudeR = parameters.get("longitude-r");
 		String usertoken = null;
 		if (parameters.get("usertoken") != null) {
 			usertoken = parameters.get("usertoken");
 		} else {
 			usertoken = "";
 		}
 		cgCacheWrap caches = new cgCacheWrap();
 
 		String page = null;
 
 		if (latitudeT == null || latitudeT.length() == 0 || latitudeB == null || latitudeB.length() == 0 || longitudeL == null || longitudeL.length() == 0 || longitudeR == null || longitudeR.length() == 0) {
 			Log.e(cgSettings.tag, "cgeoBase.searchByViewport: Not enough parameters to recognize viewport");
 			return null;
 		}
 
 		final String host = "www.geocaching.com";
 		final String path = "/map/default.aspx/MapAction";
 
 		String params = "{\"dto\":{\"data\":{\"c\":1,\"m\":\"\",\"d\":\"" + latitudeT + "|" + latitudeB + "|" + longitudeL + "|" + longitudeR + "\"},\"ut\":\"" + usertoken + "\"}}";
 
 		final String url = "http://" + host + path + "?" + params;
 		page = requestJSON(host, path, params);
 
 		if (page == null || page.length() == 0) {
 			Log.e(cgSettings.tag, "cgeoBase.searchByViewport: No data from server");
 			return null;
 		}
 
 		caches = parseMapJSON(url, page);
 		if (caches == null || caches.cacheList == null || caches.cacheList.isEmpty()) {
 			Log.e(cgSettings.tag, "cgeoBase.searchByViewport: No cache parsed");
 		}
 
 		if (app == null) {
 			Log.e(cgSettings.tag, "cgeoBase.searchByViewport: No application found");
 			return null;
 		}
 
 		final ArrayList<cgCache> cacheList = new ArrayList<cgCache>();
 		if (caches != null) {
 			if (caches.error != null && caches.error.length() > 0) {
 				search.error = caches.error;
 			}
 			if (caches.url != null && caches.url.length() > 0) {
 				search.url = caches.url;
 			}
 			if (caches.viewstate != null && caches.viewstate.length() > 0) {
 				search.viewstate = caches.viewstate;
 			}
 			if (caches.viewstate1 != null && caches.viewstate1.length() > 0) {
 				search.viewstate1 = caches.viewstate1;
 			}
 			search.totalCnt = caches.totalCnt;
 
 			if (caches.cacheList != null && caches.cacheList.size() > 0) {
 				for (cgCache cache : caches.cacheList) {
 					if ((settings.excludeDisabled == 0 || (settings.excludeDisabled == 1 && cache.disabled == false))
 							&& (settings.excludeMine == 0 || (settings.excludeMine == 1 && cache.own == false))
 							&& (settings.excludeMine == 0 || (settings.excludeMine == 1 && cache.found == false))
 							&& (settings.cacheType == null || (settings.cacheType.equals(cache.type) == true))) {
 						search.addGeocode(cache.geocode);
 						cacheList.add(cache);
 					}
 				}
 			}
 		}
 
 		app.addSearch(search, cacheList, true, reason);
 
 		return search.getCurrentId();
 	}
 
 	public ArrayList<cgUser> usersInViewport(String username, Double latMin, Double latMax, Double lonMin, Double lonMax) {
 		final ArrayList<cgUser> users = new ArrayList<cgUser>();
 
 		if (username == null) {
 			return users;
 		}
 		if (latMin == null || latMax == null || lonMin == null || lonMax == null) {
 			return users;
 		}
 
 		final String host = "api.go4cache.com";
 		final String path = "/get.php";
 		final String method = "POST";
 		final HashMap<String, String> params = new HashMap<String, String>();
 		params.put("u", username);
 		params.put("ltm", String.format((Locale) null, "%.6f", latMin));
 		params.put("ltx", String.format((Locale) null, "%.6f", latMax));
 		params.put("lnm", String.format((Locale) null, "%.6f", lonMin));
 		params.put("lnx", String.format((Locale) null, "%.6f", lonMax));
 
 		final String data = request(host, path, method, params, false, false, false);
 
 		if (data == null || data.length() == 0) {
 			Log.e(cgSettings.tag, "cgeoBase.usersInViewport: No data from server");
 			return null;
 		}
 
 		try {
 			final JSONObject dataJSON = new JSONObject(data);
 
 			final JSONArray usersData = dataJSON.getJSONArray("users");
 			if (usersData != null && usersData.length() > 0) {
 				int count = usersData.length();
 				JSONObject oneUser = null;
 				for (int i = 0; i < count; i++) {
 					final cgUser user = new cgUser();
 					oneUser = usersData.getJSONObject(i);
 					if (oneUser != null) {
 						final String located = oneUser.getString("located");
 						if (located != null) {
 							user.located = dateSqlIn.parse(located);
 						} else {
 							user.located = new Date();
 						}
 						user.username = oneUser.getString("user");
 						user.latitude = oneUser.getDouble("latitude");
 						user.longitude = oneUser.getDouble("longitude");
 						user.action = oneUser.getString("action");
 						user.client = oneUser.getString("client");
 
 						if (user.latitude != null && user.longitude != null) {
 							users.add(user);
 						}
 					}
 				}
 			}
 		} catch (Exception e) {
 			Log.e(cgSettings.tag, "cgBase.usersInViewport: " + e.toString());
 		}
 
 		return users;
 	}
 
 	public cgTrackable searchTrackable(HashMap<String, String> parameters) {
 		final String geocode = parameters.get("geocode");
 		final String guid = parameters.get("guid");
 		cgTrackable trackable = new cgTrackable();
 
 		if ((geocode == null || geocode.length() == 0) && ((guid == null || guid.length() == 0))) {
 			Log.e(cgSettings.tag, "cgeoBase.searchTrackable: No geocode nor guid given");
 			return null;
 		}
 
 		final String host = "www.geocaching.com";
 		final String path = "/track/details.aspx";
 		final String method = "GET";
 		final HashMap<String, String> params = new HashMap<String, String>();
 		if (geocode != null && geocode.length() > 0) {
 			params.put("tracker", geocode);
 		} else if (guid != null && guid.length() > 0) {
 			params.put("guid", guid);
 		}
 
 		String page = requestLogged(host, path, method, params, false, false, false);
 
 		if (page == null || page.length() == 0) {
 			Log.e(cgSettings.tag, "cgeoBase.searchTrackable: No data from server");
 			return trackable;
 		}
 
 		trackable = parseTrackable(page);
 		if (trackable == null) {
 			Log.e(cgSettings.tag, "cgeoBase.searchTrackable: No trackable parsed");
 			return trackable;
 		}
 
 		return trackable;
 	}
 
 	public int postLog(String cacheid, String viewstate, String viewstate1, int logType, int year, int month, int day, String log, ArrayList<cgTrackableLog> trackables) {
 		if (viewstate == null || viewstate.length() == 0) {
 			Log.e(cgSettings.tag, "cgeoBase.postLog: No viewstate given");
 			return 1000;
 		}
 
 		if (logTypes2.containsKey(logType) == false) {
 			Log.e(cgSettings.tag, "cgeoBase.postLog: Unknown logtype");
 			return 1000;
 		}
 
 		if (log == null || log.length() == 0) {
 			Log.e(cgSettings.tag, "cgeoBase.postLog: No log text given");
 			return 1001;
 		}
 
 		// fix log (non-Latin characters converted to HTML entities)
 		final int logLen = log.length();
 		final StringBuilder logUpdated = new StringBuilder();
 
 		for (int i = 0; i < logLen; i++) {
 			char c = log.charAt(i);
 
 			if (c > 300) {
 				logUpdated.append("&#");
 				logUpdated.append(Integer.toString((int) c));
 				logUpdated.append(";");
 			} else {
 				logUpdated.append(c);
 			}
 		}
 		log = logUpdated.toString();
 
 		if (trackables != null) {
 			Log.i(cgSettings.tag, "Trying to post log for cache #" + cacheid + " - action: " + logType + "; date: " + year + "." + month + "." + day + ", log: " + log + "; trackables: " + trackables.size());
 		} else {
 			Log.i(cgSettings.tag, "Trying to post log for cache #" + cacheid + " - action: " + logType + "; date: " + year + "." + month + "." + day + ", log: " + log + "; trackables: 0");
 		}
 
 		final Calendar currentDate = Calendar.getInstance();
 		final String host = "www.geocaching.com";
 		final String path = "/seek/log.aspx?ID=" + cacheid;
 		final String method = "POST";
 		final HashMap<String, String> params = new HashMap<String, String>();
 
 		params.put("__VIEWSTATE", viewstate);
 		if (viewstate1 != null) {
 			params.put("__VIEWSTATE1", viewstate1);
 			params.put("__VIEWSTATEFIELDCOUNT", "2");
 		}
 		params.put("__EVENTTARGET", "");
 		params.put("__EVENTARGUMENT", "");
 		params.put("__LASTFOCUS", "");
 		params.put("ctl00$ContentBody$LogBookPanel1$ddLogType", Integer.toString(logType));
 		if (currentDate.get(Calendar.YEAR) == year && (currentDate.get(Calendar.MONTH) + 1) == month && currentDate.get(Calendar.DATE) == day) {
 			params.put("ctl00$ContentBody$LogBookPanel1$DateTimeLogged", "");
 		} else {
 			params.put("ctl00$ContentBody$LogBookPanel1$DateTimeLogged", Integer.toString(month) + "/" + Integer.toString(day) + "/" + Integer.toString(year));
 		}
 		params.put("ctl00$ContentBody$LogBookPanel1$DateTimeLogged$Day", Integer.toString(day));
 		params.put("ctl00$ContentBody$LogBookPanel1$DateTimeLogged$Month", Integer.toString(month));
 		params.put("ctl00$ContentBody$LogBookPanel1$DateTimeLogged$Year", Integer.toString(year));
 		params.put("ctl00$ContentBody$LogBookPanel1$uxLogInfo", log);
 		params.put("ctl00$ContentBody$LogBookPanel1$LogButton", "Submit Log Entry");
 		params.put("ctl00$ContentBody$uxVistOtherListingGC", "");
 		if (trackables != null && trackables.isEmpty() == false) { //  we have some trackables to proceed
 			final StringBuilder hdnSelected = new StringBuilder();
 
 			for (cgTrackableLog tb : trackables) {
 				String ctl = null;
 				final String action = Integer.toString(tb.id) + logTypesTrackableAction.get(tb.action);
 
 				if (tb.ctl < 10) {
 					ctl = "0" + Integer.toString(tb.ctl);
 				} else {
 					ctl = Integer.toString(tb.ctl);
 				}
 
 				params.put("ctl00$ContentBody$LogBookPanel1$uxTrackables$repTravelBugs$ctl" + ctl + "$ddlAction", action);
 				if (tb.action > 0) {
 					hdnSelected.append(action);
 					hdnSelected.append(",");
 				}
 			}
 
 			params.put("ctl00$ContentBody$LogBookPanel1$uxTrackables$hdnSelectedActions", hdnSelected.toString()); // selected trackables
 			params.put("ctl00$ContentBody$LogBookPanel1$uxTrackables$hdnCurrentFilter", "");
 		}
 
 		String page = request(host, path, method, params, false, false, false);
 		if (checkLogin(page) == false) {
 			int loginState = login();
 			if (loginState == 1) {
 				page = request(host, path, method, params, false, false, false);
 			} else {
 				Log.e(cgSettings.tag, "cgeoBase.postLog: Can not log in geocaching (error: " + loginState + ")");
 				return loginState;
 			}
 		}
 
 		if (page == null || page.length() == 0) {
 			Log.e(cgSettings.tag, "cgeoBase.postLog: No data from server");
 			return 1002;
 		}
 
 		// maintenance, archived needs to be confirmed
 		final Pattern pattern = Pattern.compile("<span id=\"ctl00_ContentBody_LogBookPanel1_lbConfirm\"[^>]*>([^<]*<font[^>]*>)?([^<]+)(<\\/font>[^<]*)?<\\/span>", Pattern.CASE_INSENSITIVE);
 		final Matcher matcher = pattern.matcher(page);
 
 		try {
 			if (matcher.find() == true && matcher.groupCount() > 0) {
 				final String viewstateConfirm = findViewstate(page, 0);
 				final String viewstate1Confirm = findViewstate(page, 1);
 
 				if (viewstateConfirm == null || viewstateConfirm.length() == 0) {
 					Log.e(cgSettings.tag, "cgeoBase.postLog: No viewstate for confirm log");
 					return 1000;
 				}
 
 				params.clear();
 				params.put("__VIEWSTATE", viewstateConfirm);
 				if (viewstate1 != null) {
 					params.put("__VIEWSTATE1", viewstate1Confirm);
 					params.put("__VIEWSTATEFIELDCOUNT", "2");
 				}
 				params.put("__EVENTTARGET", "");
 				params.put("__EVENTARGUMENT", "");
 				params.put("__LASTFOCUS", "");
 				params.put("ctl00$ContentBody$LogBookPanel1$btnConfirm", "Yes");
 				params.put("ctl00$ContentBody$LogBookPanel1$uxLogInfo", log);
 				params.put("ctl00$ContentBody$uxVistOtherListingGC", "");
 				if (trackables != null && trackables.isEmpty() == false) { //  we have some trackables to proceed
 					final StringBuilder hdnSelected = new StringBuilder();
 
 					for (cgTrackableLog tb : trackables) {
 						String ctl = null;
 						final String action = Integer.toString(tb.id) + logTypesTrackableAction.get(tb.action);
 
 						if (tb.ctl < 10) {
 							ctl = "0" + Integer.toString(tb.ctl);
 						} else {
 							ctl = Integer.toString(tb.ctl);
 						}
 
 						params.put("ctl00$ContentBody$LogBookPanel1$uxTrackables$repTravelBugs$ctl" + ctl + "$ddlAction", action);
 						if (tb.action > 0) {
 							hdnSelected.append(action);
 							hdnSelected.append(",");
 						}
 					}
 
 					params.put("ctl00$ContentBody$LogBookPanel1$uxTrackables$hdnSelectedActions", hdnSelected.toString()); // selected trackables
 					params.put("ctl00$ContentBody$LogBookPanel1$uxTrackables$hdnCurrentFilter", "");
 				}
 
 				page = request(host, path, method, params, false, false, false);
 			}
 		} catch (Exception e) {
 			Log.e(cgSettings.tag, "cgeoBase.postLog.confim: " + e.toString());
 		}
 
 		try {
 			final Pattern patternOk = Pattern.compile("<h2[^>]*>[^<]*<span id=\"ctl00_ContentBody_lbHeading\"[^>]*>[^<]*</span>[^<]*</h2>[^<]*<div id=[\"|']ctl00_ContentBody_LogBookPanel1_ViewLogPanel[\"|']>", Pattern.CASE_INSENSITIVE);
 			final Matcher matcherOk = patternOk.matcher(page);
 			if (matcherOk.find() == true) {
 				Log.i(cgSettings.tag, "Log successfully posted to cache #" + cacheid);
 				return 1;
 			}
 		} catch (Exception e) {
 			Log.e(cgSettings.tag, "cgeoBase.postLog.check: " + e.toString());
 		}
 
 		Log.e(cgSettings.tag, "cgeoBase.postLog: Failed to post log because of unknown error");
 		return 1000;
 	}
 
 	public int postLogTrackable(String tbid, String trackingCode, String viewstate, String viewstate1, int logType, int year, int month, int day, String log) {
 		if (viewstate == null || viewstate.length() == 0) {
 			Log.e(cgSettings.tag, "cgeoBase.postLogTrackable: No viewstate given");
 			return 1000;
 		}
 
 		if (logTypes2.containsKey(logType) == false) {
 			Log.e(cgSettings.tag, "cgeoBase.postLogTrackable: Unknown logtype");
 			return 1000;
 		}
 
 		if (log == null || log.length() == 0) {
 			Log.e(cgSettings.tag, "cgeoBase.postLogTrackable: No log text given");
 			return 1001;
 		}
 
 		Log.i(cgSettings.tag, "Trying to post log for trackable #" + trackingCode + " - action: " + logType + "; date: " + year + "." + month + "." + day + ", log: " + log);
 
 		final Calendar currentDate = Calendar.getInstance();
 		final String host = "www.geocaching.com";
 		final String path = "/track/log.aspx?wid=" + tbid;
 		final String method = "POST";
 		final HashMap<String, String> params = new HashMap<String, String>();
 
 		params.put("__VIEWSTATE", viewstate);
 		if (viewstate1 != null) {
 			params.put("__VIEWSTATE1", viewstate1);
 			params.put("__VIEWSTATEFIELDCOUNT", "2");
 		}
 		params.put("__EVENTTARGET", "");
 		params.put("__EVENTARGUMENT", "");
 		params.put("__LASTFOCUS", "");
 		params.put("ctl00$ContentBody$LogBookPanel1$ddLogType", Integer.toString(logType));
 		params.put("ctl00$ContentBody$LogBookPanel1$tbCode", trackingCode);
 		if (currentDate.get(Calendar.YEAR) == year && (currentDate.get(Calendar.MONTH) + 1) == month && currentDate.get(Calendar.DATE) == day) {
 			params.put("ctl00$ContentBody$LogBookPanel1$DateTimeLogged", "");
 		} else {
 			params.put("ctl00$ContentBody$LogBookPanel1$DateTimeLogged", Integer.toString(month) + "/" + Integer.toString(day) + "/" + Integer.toString(year));
 		}
 		params.put("ctl00$ContentBody$LogBookPanel1$DateTimeLogged$Day", Integer.toString(day));
 		params.put("ctl00$ContentBody$LogBookPanel1$DateTimeLogged$Month", Integer.toString(month));
 		params.put("ctl00$ContentBody$LogBookPanel1$DateTimeLogged$Year", Integer.toString(year));
 		params.put("ctl00$ContentBody$LogBookPanel1$uxLogInfo", log);
 		params.put("ctl00$ContentBody$LogBookPanel1$LogButton", "Submit Log Entry");
 		params.put("ctl00$ContentBody$uxVistOtherListingGC", "");
 
 		String page = request(host, path, method, params, false, false, false);
 		if (checkLogin(page) == false) {
 			int loginState = login();
 			if (loginState == 1) {
 				page = request(host, path, method, params, false, false, false);
 			} else {
 				Log.e(cgSettings.tag, "cgeoBase.postLogTrackable: Can not log in geocaching (error: " + loginState + ")");
 				return loginState;
 			}
 		}
 
 		if (page == null || page.length() == 0) {
 			Log.e(cgSettings.tag, "cgeoBase.postLogTrackable: No data from server");
 			return 1002;
 		}
 
 		try {
 			final Pattern patternOk = Pattern.compile("<h2[^>]*>[^<]*<span id=\"ctl00_ContentBody_lbHeading\"[^>]*>[^<]*</span>[^<]*</h2>[^<]*<div id=[\"|']ctl00_ContentBody_LogBookPanel1_ViewLogPanel[\"|']>", Pattern.CASE_INSENSITIVE);
 			final Matcher matcherOk = patternOk.matcher(page);
 			if (matcherOk.find() == true) {
 				Log.i(cgSettings.tag, "Log successfully posted to trackable #" + trackingCode);
 				return 1;
 			}
 		} catch (Exception e) {
 			Log.e(cgSettings.tag, "cgeoBase.postLogTrackable.check: " + e.toString());
 		}
 
 		Log.e(cgSettings.tag, "cgeoBase.postLogTrackable: Failed to post log because of unknown error");
 		return 1000;
 	}
 	final public static HostnameVerifier doNotVerify = new HostnameVerifier() {
 
 		public boolean verify(String hostname, SSLSession session) {
 			return true;
 		}
 	};
 
 	public static void trustAllHosts() {
 		TrustManager[] trustAllCerts = new TrustManager[]{
 			new X509TrustManager() {
 
 				public java.security.cert.X509Certificate[] getAcceptedIssuers() {
 					return new java.security.cert.X509Certificate[]{};
 				}
 
 				public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
 				}
 
 				public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
 				}
 			}
 		};
 
 		try {
 			SSLContext sc = SSLContext.getInstance("TLS");
 			sc.init(null, trustAllCerts, new java.security.SecureRandom());
 			HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
 		} catch (Exception e) {
 			Log.e(cgSettings.tag, "cgBase.trustAllHosts: " + e.toString());
 		}
 	}
 
 	public void postTweetCache(cgeoapplication app, cgSettings settings, String geocode) {
 		final cgCache cache = app.getCacheByGeocode(geocode);
 		String name = cache.name;
 		if (name.length() > 84) {
 			name = name.substring(0, 81) + "...";
 		}
 		final String status = "I found " + name + " (http://coord.info/" + cache.geocode.toUpperCase() + ")! #cgeo #geocaching"; // 56 chars + cache name
 
 		postTweet(app, settings, status, null, null);
 	}
 
 	public void postTweetTrackable(cgeoapplication app, cgSettings settings, String geocode) {
 		final cgTrackable trackable = app.getTrackableByGeocode(geocode);
 		String name = trackable.name;
 		if (name.length() > 82) {
 			name = name.substring(0, 79) + "...";
 		}
 		final String status = "I touched " + name + " (http://coord.info/" + trackable.geocode.toUpperCase() + ")! #cgeo #geocaching"; // 58 chars + trackable name
 
 		postTweet(app, settings, status, null, null);
 	}
 
 	public void postTweet(cgeoapplication app, cgSettings settings, String status, Double latitude, Double longitude) {
 		if (app == null) {
 			return;
 		}
 		if (settings == null || settings.tokenPublic == null || settings.tokenPublic.length() == 0 || settings.tokenSecret == null || settings.tokenSecret.length() == 0) {
 			return;
 		}
 
 		try {
 			HashMap<String, String> parameters = new HashMap<String, String>();
 
 			parameters.put("status", status);
 			if (latitude != null && longitude != null) {
 				parameters.put("lat", String.format("%.6f", latitude));
 				parameters.put("long", String.format("%.6f", longitude));
 				parameters.put("display_coordinates", "true");
 			}
 
 			final String paramsDone = cgOAuth.signOAuth("api.twitter.com", "/1/statuses/update.json", "POST", false, parameters, settings.tokenPublic, settings.tokenSecret);
 
 			HttpURLConnection connection = null;
 			try {
 				final StringBuffer buffer = new StringBuffer();
 				final URL u = new URL("http://api.twitter.com/1/statuses/update.json");
 				final URLConnection uc = u.openConnection();
 
 				uc.setRequestProperty("Host", "api.twitter.com");
 
 				connection = (HttpURLConnection) uc;
 				connection.setReadTimeout(30000);
 				connection.setRequestMethod("POST");
 				connection.setFollowRedirects(true);
 				connection.setDoInput(true);
 				connection.setDoOutput(true);
 
 				final OutputStream out = connection.getOutputStream();
 				final OutputStreamWriter wr = new OutputStreamWriter(out);
 				wr.write(paramsDone);
 				wr.flush();
 				wr.close();
 
 				Log.i(cgSettings.tag, "Twitter.com: " + connection.getResponseCode() + " " + connection.getResponseMessage());
 
 				InputStream ins;
 				final String encoding = connection.getContentEncoding();
 
 				if (encoding != null && encoding.equalsIgnoreCase("gzip")) {
 					ins = new GZIPInputStream(connection.getInputStream());
 				} else if (encoding != null && encoding.equalsIgnoreCase("deflate")) {
 					ins = new InflaterInputStream(connection.getInputStream(), new Inflater(true));
 				} else {
 					ins = connection.getInputStream();
 				}
 
 				final InputStreamReader inr = new InputStreamReader(ins);
 				final BufferedReader br = new BufferedReader(inr);
 
 				String line;
 				while ((line = br.readLine()) != null) {
 					if (line.length() > 0) {
 						buffer.append(line);
 						buffer.append("\n");
 					}
 				}
 
 				br.close();
 				ins.close();
 				inr.close();
 				connection.disconnect();
 			} catch (IOException e) {
 				Log.e(cgSettings.tag, "cgBase.postTweet.IO: " + e.toString() + " ~ " + connection.getResponseCode() + ": " + connection.getResponseMessage());
 
 				final InputStream ins = connection.getErrorStream();
 				final StringBuffer buffer = new StringBuffer();
 				final InputStreamReader inr = new InputStreamReader(ins);
 				final BufferedReader br = new BufferedReader(inr);
 
 				String line;
 				while ((line = br.readLine()) != null) {
 					if (line.length() > 0) {
 						buffer.append(line);
 						buffer.append("\n");
 					}
 				}
 
 				br.close();
 				ins.close();
 				inr.close();
 			} catch (Exception e) {
 				Log.e(cgSettings.tag, "cgBase.postTweet.inner: " + e.toString());
 			}
 
 			connection.disconnect();
 		} catch (Exception e) {
 			Log.e(cgSettings.tag, "cgBase.postTweet: " + e.toString());
 		}
 	}
 
 	public static String implode(String delim, Object[] array) {
 		String out = "";
 
 		try {
 			for (int i = 0; i < array.length; i++) {
 				if (i != 0) {
 					out += delim;
 				}
 				out += array[i].toString();
 			}
 		} catch (Exception e) {
 			Log.e(cgSettings.tag, "cgeoBase.implode: " + e.toString());
 		}
 		return out;
 	}
 
 	public static String urlencode_rfc3986(String text) {
 		final String encoded = URLEncoder.encode(text).replace("+", "%20").replaceAll("%7E", "~");
 
 		return encoded;
 	}
 
 	public String prepareParameters(HashMap<String, String> params, boolean my, boolean addF) {
 		String paramsDone = null;
 
 		if (my != true && settings.excludeMine > 0) {
 			if (params == null) {
 				params = new HashMap<String, String>();
 			}
 			if (addF == true) {
 				params.put("f", "1");
 			}
 
 			Log.i(cgSettings.tag, "Skipping caches found or hidden by user.");
 		}
 
 		if (params != null) {
 			Object[] keys = params.keySet().toArray();
 			ArrayList<String> paramsEncoded = new ArrayList();
 			String key;
 			String value;
 
 			for (int i = 0; i < keys.length; i++) {
 				key = (String) keys[i];
 				value = (String) params.get(key);
 
 				if (key.charAt(0) == '^') {
 					key = "";
 				}
 				if (value == null) {
 					value = "";
 				}
 
 				paramsEncoded.add(key + "=" + urlencode_rfc3986(value));
 			}
 
 			paramsDone = implode("&", paramsEncoded.toArray());
 		} else {
 			paramsDone = "";
 		}
 
 		return paramsDone;
 	}
 
 	public String requestViewstate(String host, String path, String method, HashMap<String, String> params, boolean xContentType, boolean my) {
 		final String page = request(host, path, method, params, xContentType, my, false);
 
 		return findViewstate(page, 0);
 	}
 
 	public String requestViewstate1(String host, String path, String method, HashMap<String, String> params, boolean xContentType, boolean my) {
 		final String page = request(host, path, method, params, xContentType, my, false);
 
 		return findViewstate(page, 1);
 	}
 
 	public String requestLogged(String host, String path, String method, HashMap<String, String> params, boolean xContentType, boolean my, boolean addF) {
 		String page = request(host, path, method, params, xContentType, my, addF);
 
 		if (checkLogin(page) == false) {
 			int loginState = login();
 			if (loginState == 1) {
 				page = request(host, path, method, params, xContentType, my, addF);
 			} else {
 				Log.i(cgSettings.tag, "Working as guest.");
 			}
 		}
 
 		return page;
 	}
 
 	public String request(String host, String path, String method, HashMap<String, String> params, boolean xContentType, boolean my, boolean addF) {
 		// prepare parameters
 		final String paramsDone = prepareParameters(params, my, addF);
 
 		return request(host, path, method, paramsDone, 0, xContentType);
 	}
 
 	public String request(String host, String path, String method, HashMap<String, String> params, int requestId, boolean xContentType, boolean my, boolean addF) {
 		// prepare parameters
 		final String paramsDone = prepareParameters(params, my, addF);
 
 		return request(host, path, method, paramsDone, requestId, xContentType);
 	}
 
 	public String request(String host, String path, String method, String params, int requestId, Boolean xContentType) {
 		int httpCode = -1;
 		String httpLocation = null;
 
 		if (requestId == 0) {
 			requestId = (int) (Math.random() * 1000);
 		}
 
 		if (method == null || (method.equalsIgnoreCase("GET") == false && method.equalsIgnoreCase("POST") == false)) {
 			method = "POST";
 		} else {
 			method = method.toUpperCase();
 		}
 
 		// prepare cookies
 		String cookiesDone = null;
 		if (cookies != null) {
 			final Object[] keys = cookies.keySet().toArray();
 			final ArrayList<String> cookiesEncoded = new ArrayList();
 
 			for (int i = 0; i < keys.length; i++) {
 				String value = cookies.get(keys[i].toString());
 				cookiesEncoded.add(keys[i] + "=" + value);
 			}
 
 			if (cookiesEncoded.size() > 0) {
 				cookiesDone = implode("; ", cookiesEncoded.toArray());
 			}
 		}
 
 		if (cookiesDone == null) {
 			Map prefsValues = prefs.getAll();
 
 			if (prefsValues != null && prefsValues.size() > 0) {
 				final Object[] keys = prefsValues.keySet().toArray();
 				final ArrayList<String> cookiesEncoded = new ArrayList();
 				final int length = keys.length;
 
 				for (int i = 0; i < length; i++) {
 					if (keys[i].toString().length() > 7 && keys[i].toString().substring(0, 7).equals("cookie_") == true) {
 						cookiesEncoded.add(keys[i].toString().substring(7) + "=" + prefsValues.get(keys[i].toString()));
 					}
 				}
 
 				if (cookiesEncoded.size() > 0) {
 					cookiesDone = implode("; ", cookiesEncoded.toArray());
 				}
 			}
 		}
 
 		if (cookiesDone == null) {
 			cookiesDone = "";
 		}
 
 		URLConnection uc = null;
 		HttpURLConnection connection = null;
 		Integer timeout = 30000;
 		final StringBuffer buffer = new StringBuffer();
 
 		for (int i = 0; i < 3; i++) {
 			if (i > 0) {
 				Log.w(cgSettings.tag, "Failed to download data, retrying. Attempt #" + (i + 1));
 			}
 
 			buffer.delete(0, buffer.length());
 			timeout = 30000 + (i * 15000);
 
 			try {
 				if (method.equals("GET")) {
 					// GET
 					final URL u = new URL("http://" + host + path + "?" + params);
 					uc = u.openConnection();
 
 					uc.setRequestProperty("Host", host);
 					uc.setRequestProperty("Cookie", cookiesDone);
 					if (xContentType == true) {
 						uc.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
 					}
 
 					if (settings.asBrowser == 1) {
 						uc.setRequestProperty("Accept", "application/xml,application/xhtml+xml,text/html;q=0.9,text/plain;q=0.8,image/png,*/*;q=0.5");
 						// uc.setRequestProperty("Accept-Encoding", "gzip"); // not supported via cellular network
 						uc.setRequestProperty("Accept-Charset", "utf-8, iso-8859-1, utf-16, *;q=0.7");
 						uc.setRequestProperty("Accept-Language", "en-US");
 						uc.setRequestProperty("User-Agent", idBrowser);
 						uc.setRequestProperty("Connection", "keep-alive");
 						uc.setRequestProperty("Keep-Alive", "300");
 					}
 
 					connection = (HttpURLConnection) uc;
 					connection.setReadTimeout(timeout);
 					connection.setRequestMethod(method);
 					connection.setFollowRedirects(false);
 					connection.setDoInput(true);
 					connection.setDoOutput(false);
 				} else {
 					// POST
 					final URL u = new URL("http://" + host + path);
 					uc = u.openConnection();
 
 					uc.setRequestProperty("Host", host);
 					uc.setRequestProperty("Cookie", cookiesDone);
 					if (xContentType == true) {
 						uc.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
 					}
 
 					if (settings.asBrowser == 1) {
 						uc.setRequestProperty("Accept", "application/xml,application/xhtml+xml,text/html;q=0.9,text/plain;q=0.8,image/png,*/*;q=0.5");
 						// uc.setRequestProperty("Accept-Encoding", "gzip"); // not supported via cellular network
 						uc.setRequestProperty("Accept-Charset", "utf-8, iso-8859-1, utf-16, *;q=0.7");
 						uc.setRequestProperty("Accept-Language", "en-US");
 						uc.setRequestProperty("User-Agent", idBrowser);
 						uc.setRequestProperty("Connection", "keep-alive");
 						uc.setRequestProperty("Keep-Alive", "300");
 					}
 
 					connection = (HttpURLConnection) uc;
 					connection.setReadTimeout(timeout);
 					connection.setRequestMethod(method);
 					connection.setFollowRedirects(false);
 					connection.setDoInput(true);
 					connection.setDoOutput(true);
 
 					final OutputStream out = connection.getOutputStream();
 					final OutputStreamWriter wr = new OutputStreamWriter(out);
 					wr.write(params);
 					wr.flush();
 					wr.close();
 				}
 
 				String headerName = null;
 				final SharedPreferences.Editor prefsEditor = prefs.edit();
 				for (int j = 1; (headerName = uc.getHeaderFieldKey(j)) != null; j++) {
 					if (headerName != null && headerName.equalsIgnoreCase("Set-Cookie")) {
 						int index;
 						String cookie = uc.getHeaderField(j);
 
 						index = cookie.indexOf(";");
 						if (index > -1) {
 							cookie = cookie.substring(0, cookie.indexOf(";"));
 						}
 
 						index = cookie.indexOf("=");
 						if (index > - 1 && cookie.length() > (index + 1)) {
 							String name = cookie.substring(0, cookie.indexOf("="));
 							String value = cookie.substring(cookie.indexOf("=") + 1, cookie.length());
 
 							this.cookies.put(name, value);
 							prefsEditor.putString("cookie_" + name, value);
 						}
 					}
 				}
 				prefsEditor.commit();
 
 				final String encoding = connection.getContentEncoding();
 				InputStream ins;
 
 				if (encoding != null && encoding.equalsIgnoreCase("gzip")) {
 					ins = new GZIPInputStream(connection.getInputStream());
 				} else if (encoding != null && encoding.equalsIgnoreCase("deflate")) {
 					ins = new InflaterInputStream(connection.getInputStream(), new Inflater(true));
 				} else {
 					ins = connection.getInputStream();
 				}
 				final InputStreamReader inr = new InputStreamReader(ins);
 				final BufferedReader br = new BufferedReader(inr);
 
 				String line;
 				while ((line = br.readLine()) != null) {
 					if (line.length() > 0) {
 						buffer.append(line);
 						buffer.append("\n");
 					}
 				}
 
 				httpCode = connection.getResponseCode();
 				httpLocation = uc.getHeaderField("Location");
 
 				Log.i(cgSettings.tag + " | " + requestId, "[" + buffer.length() + "B] Downloading server response (" + method + " " + httpCode + ", " + connection.getResponseMessage() + ") " + "http://" + host + path + "?" + params);
 
 				connection.disconnect();
 				br.close();
 				ins.close();
 				inr.close();
 			} catch (IOException e) {
 				Log.e(cgSettings.tag, "cgeoBase.request.IOException: " + e.toString());
 			} catch (Exception e) {
 				Log.e(cgSettings.tag, "cgeoBase.request: " + e.toString());
 			}
 
 			if (buffer != null && buffer.length() > 0) {
 				break;
 			}
 		}
 
 		String page = null;
 		if (httpCode == 302 && httpLocation != null) {
 			final Uri newLocation = Uri.parse(httpLocation);
 			if (newLocation.isRelative() == true) {
 				page = request(host, path, "GET", new HashMap<String, String>(), requestId, false, false, false);
 			} else {
 				page = request(newLocation.getHost(), newLocation.getPath(), "GET", new HashMap<String, String>(), requestId, false, false, false);
 			}
 		} else if (buffer != null) {
 			final Matcher matcherLines = patternLines.matcher(buffer.toString());
 			page = matcherLines.replaceAll(" ");
 
 			final Pattern patternTitle = Pattern.compile("<title>([^<]+)</title>", Pattern.CASE_INSENSITIVE);
 			final Matcher matcherTitle = patternTitle.matcher(page);
 			if (matcherTitle.find() == true && matcherTitle.groupCount() > 0) {
 				Log.i(cgSettings.tag + " | " + requestId, "Downloaded page title: " + matcherTitle.group(1).trim());
 			} else {
 				Log.i(cgSettings.tag + " | " + requestId, "Downloaded file has no title.");
 			}
 		} else {
 			return "";
 		}
 
 		if (page != null) {
 			return page;
 		} else {
 			return "";
 		}
 	}
 
 	public String requestJSON(String host, String path, String params) {
 		int httpCode = -1;
 		String httpLocation = null;
 
 		// prepare cookies
 		String cookiesDone = null;
 		if (cookies != null) {
 			final Object[] keys = cookies.keySet().toArray();
 			final ArrayList<String> cookiesEncoded = new ArrayList();
 
 			for (int i = 0; i < keys.length; i++) {
 				String value = cookies.get(keys[i].toString());
 				cookiesEncoded.add(keys[i] + "=" + value);
 			}
 
 			if (cookiesEncoded.size() > 0) {
 				cookiesDone = implode("; ", cookiesEncoded.toArray());
 			}
 		}
 
 		if (cookiesDone == null) {
 			Map prefsValues = prefs.getAll();
 
 			if (prefsValues != null && prefsValues.size() > 0) {
 				final Object[] keys = prefsValues.keySet().toArray();
 				final ArrayList<String> cookiesEncoded = new ArrayList();
 				final int length = keys.length;
 
 				for (int i = 0; i < length; i++) {
 					if (keys[i].toString().length() > 7 && keys[i].toString().substring(0, 7).equals("cookie_") == true) {
 						cookiesEncoded.add(keys[i].toString().substring(7) + "=" + prefsValues.get(keys[i].toString()));
 					}
 				}
 
 				if (cookiesEncoded.size() > 0) {
 					cookiesDone = implode("; ", cookiesEncoded.toArray());
 				}
 			}
 		}
 
 		if (cookiesDone == null) {
 			cookiesDone = "";
 		}
 
 		URLConnection uc = null;
 		HttpURLConnection connection = null;
 		Integer timeout = 30000;
 		final StringBuffer buffer = new StringBuffer();
 
 		for (int i = 0; i < 3; i++) {
 			if (i > 0) {
 				Log.w(cgSettings.tag, "Failed to download data, retrying. Attempt #" + (i + 1));
 			}
 
 			buffer.delete(0, buffer.length());
 			timeout = 30000 + (i * 15000);
 
 			try {
 				// POST
 				final URL u = new URL("http://" + host + path);
 				uc = u.openConnection();
 
 				uc.setRequestProperty("Host", host);
 				uc.setRequestProperty("Cookie", cookiesDone);
 				uc.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
 				uc.setRequestProperty("X-Requested-With", "XMLHttpRequest");
 				uc.setRequestProperty("Accept", "application/json, text/javascript, */*; q=0.01");
 				uc.setRequestProperty("Referer", host + "/" + path);
 
 				if (settings.asBrowser == 1) {
 					uc.setRequestProperty("Accept-Charset", "utf-8, iso-8859-1, utf-16, *;q=0.7");
 					uc.setRequestProperty("Accept-Language", "en-US");
 					uc.setRequestProperty("User-Agent", idBrowser);
 					uc.setRequestProperty("Connection", "keep-alive");
 					uc.setRequestProperty("Keep-Alive", "300");
 				}
 
 				connection = (HttpURLConnection) uc;
 				connection.setReadTimeout(timeout);
 				connection.setRequestMethod("POST");
 				connection.setFollowRedirects(false);
 				connection.setDoInput(true);
 				connection.setDoOutput(true);
 
 				final OutputStream out = connection.getOutputStream();
 				final OutputStreamWriter wr = new OutputStreamWriter(out);
 				wr.write(params);
 				wr.flush();
 				wr.close();
 
 				String headerName = null;
 				final SharedPreferences.Editor prefsEditor = prefs.edit();
 				for (int j = 1; (headerName = uc.getHeaderFieldKey(j)) != null; j++) {
 					if (headerName != null && headerName.equalsIgnoreCase("Set-Cookie")) {
 						int index;
 						String cookie = uc.getHeaderField(j);
 
 						index = cookie.indexOf(";");
 						if (index > -1) {
 							cookie = cookie.substring(0, cookie.indexOf(";"));
 						}
 
 						index = cookie.indexOf("=");
 						if (index > - 1 && cookie.length() > (index + 1)) {
 							String name = cookie.substring(0, cookie.indexOf("="));
 							String value = cookie.substring(cookie.indexOf("=") + 1, cookie.length());
 
 							this.cookies.put(name, value);
 							prefsEditor.putString("cookie_" + name, value);
 						}
 					}
 				}
 				prefsEditor.commit();
 
 				final String encoding = connection.getContentEncoding();
 				InputStream ins;
 
 				if (encoding != null && encoding.equalsIgnoreCase("gzip")) {
 					ins = new GZIPInputStream(connection.getInputStream());
 				} else if (encoding != null && encoding.equalsIgnoreCase("deflate")) {
 					ins = new InflaterInputStream(connection.getInputStream(), new Inflater(true));
 				} else {
 					ins = connection.getInputStream();
 				}
 				final InputStreamReader inr = new InputStreamReader(ins);
 				final BufferedReader br = new BufferedReader(inr);
 
 				String line;
 				while ((line = br.readLine()) != null) {
 					if (line.length() > 0) {
 						buffer.append(line);
 						buffer.append("\n");
 					}
 				}
 
 				httpCode = connection.getResponseCode();
 				httpLocation = uc.getHeaderField("Location");
 
 				Log.i(cgSettings.tag + " | JSON", "[" + buffer.length() + "B] Downloading server response (POST " + httpCode + ", " + connection.getResponseMessage() + ") " + "http://" + host + path + "?" + params);
 
 				connection.disconnect();
 				br.close();
 				ins.close();
 				inr.close();
 			} catch (IOException e) {
 				Log.e(cgSettings.tag, "cgeoBase.requestJSON.IOException: " + e.toString());
 			} catch (Exception e) {
 				Log.e(cgSettings.tag, "cgeoBase.requestJSON: " + e.toString());
 			}
 
 			if (buffer != null && buffer.length() > 0) {
 				break;
 			}
 		}
 
 		String page = null;
 		if (httpCode == 302 && httpLocation != null) {
 			final Uri newLocation = Uri.parse(httpLocation);
 			if (newLocation.isRelative() == true) {
 				page = requestJSON(host, path, params);
 			} else {
 				page = requestJSON(newLocation.getHost(), newLocation.getPath(), params);
 			}
 		} else if (buffer != null) {
 			final Matcher matcherLines = patternLines.matcher(buffer.toString());
 			page = matcherLines.replaceAll(" ");
 
 			final Pattern patternTitle = Pattern.compile("<title>([^<]+)</title>", Pattern.CASE_INSENSITIVE);
 			final Matcher matcherTitle = patternTitle.matcher(page);
 			if (matcherTitle.find() == true && matcherTitle.groupCount() > 0) {
 				Log.i(cgSettings.tag + " | JSON", "Downloaded page title: " + matcherTitle.group(1).trim());
 			} else {
 				Log.i(cgSettings.tag + " | JSON", "Downloaded file has no title.");
 			}
 		} else {
 			return "";
 		}
 
 		if (page != null) {
 			return page;
 		} else {
 			return "";
 		}
 	}
 
 	public static String rot13(String text) {
 		final String xlr13 = "abcdefghijklmnopqrstuvwxyzabcdefghijklmABCDEFGHIJKLMNOPQRSTUVWXYZABCDEFGHIJKLM";
 		final StringBuilder progress = new StringBuilder();
 
 		//  plaintext flag (do not convert)
 		Boolean pt = false;
 
 		int len = text.length();
 		for (int z = 0; z < len; z++) {
 			Character c = text.charAt(z);
 			if (c == '[') {
 				pt = true;
 			} else if (c == ']') {
 				pt = false;
 			} else {
 				int idx = xlr13.indexOf(c);
 				if (idx >= 0 && !pt) {
 					c = xlr13.charAt(idx + 13);
 				}
 			}
 			progress.append(c);
 		}
 		return progress.toString();
 	}
 
 	public static String md5(String text) {
 		String hashed = "";
 
 		try {
 			MessageDigest digest = MessageDigest.getInstance("MD5");
 			digest.update(text.getBytes(), 0, text.length());
 			hashed = new BigInteger(1, digest.digest()).toString(16);
 		} catch (Exception e) {
 			Log.e(cgSettings.tag, "cgBase.md5: " + e.toString());
 		}
 
 		return hashed;
 	}
 
 	public static String sha1(String text) {
 		String hashed = "";
 
 		try {
 			MessageDigest digest = MessageDigest.getInstance("SHA-1");
 			digest.update(text.getBytes(), 0, text.length());
 			hashed = new BigInteger(1, digest.digest()).toString(16);
 		} catch (Exception e) {
 			Log.e(cgSettings.tag, "cgBase.sha1: " + e.toString());
 		}
 
 		return hashed;
 	}
 
 	public static byte[] hashHmac(String text, String salt) {
 		byte[] macBytes = {};
 
 		try {
 			SecretKeySpec secretKeySpec = new SecretKeySpec(salt.getBytes(), "HmacSHA1");
 			Mac mac = Mac.getInstance("HmacSHA1");
 			mac.init(secretKeySpec);
 			macBytes = mac.doFinal(text.getBytes());
 		} catch (Exception e) {
 			Log.e(cgSettings.tag, "cgBase.hashHmac: " + e.toString());
 		}
 
 		return macBytes;
 	}
 
 	public static boolean deleteDirectory(File path) {
 		if (path.exists()) {
 			File[] files = path.listFiles();
 
 			for (int i = 0; i < files.length; i++) {
 				if (files[i].isDirectory()) {
 					deleteDirectory(files[i]);
 				} else {
 					files[i].delete();
 				}
 			}
 		}
 
 		return (path.delete());
 	}
 
 	public static boolean isIntentAvailable(Context context, String action) {
 		final Intent intent = new Intent(action);
 
 		return isIntentAvailable(context, intent);
 	}
 
 	public static boolean isIntentAvailable(Context context, Intent intent) {
 		final PackageManager packageManager = context.getPackageManager();
 		final List<ResolveInfo> list = packageManager.queryIntentActivities(intent, packageManager.MATCH_DEFAULT_ONLY);
 
 		return list.size() > 0;
 	}
 
 	public void storeCache(cgeoapplication app, Activity activity, cgCache cache, String geocode, Handler handler) {
 		try {
 			// cache details
 			if (cache != null) {
 				final HashMap<String, String> params = new HashMap<String, String>();
 				params.put("geocode", cache.geocode);
 				final Long searchId = searchByGeocode(params, 1, false);
 				cache = app.getCache(searchId);
 			} else if (geocode != null) {
 				final HashMap<String, String> params = new HashMap<String, String>();
 				params.put("geocode", geocode);
 				final Long searchId = searchByGeocode(params, 1, false);
 				cache = app.getCache(searchId);
 			}
 
 			if (cache == null) {
 				return;
 			}
 
 			// store images from description
 			if (cache.description != null) {
 				Html.fromHtml(cache.description, new cgHtmlImg(activity, settings, cache.geocode, false, 0, true), null);
 			}
 
 			// store spoilers
 			if (cache.spoilers != null && cache.spoilers.isEmpty() == false) {
 				for (cgSpoiler oneSpoiler : cache.spoilers) {
 					final cgHtmlImg imgGetter = new cgHtmlImg(activity, settings, cache.geocode, false, 0, true);
 					imgGetter.getDrawable(oneSpoiler.url);
 				}
 			}
 
 			// store map previews
 			if (settings.storeOfflineMaps == 1 && cache.latitude != null && cache.longitude != null) {
 				final String latlonMap = String.format((Locale) null, "%.6f", cache.latitude) + "," + String.format((Locale) null, "%.6f", cache.longitude);
 				final Display display = ((WindowManager) activity.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
 				final int maxWidth = display.getWidth() - 25;
 				final int maxHeight = display.getHeight() - 25;
 				int edge = 0;
 				if (maxWidth > maxHeight) {
 					edge = maxWidth;
 				} else {
 					edge = maxHeight;
 				}
 
 				cgMapImg mapGetter = new cgMapImg(settings, cache.geocode);
 
 				String type = "mystery";
 				if (cache.found == true) {
 					type = cache.type + "_found";
 				} else if (cache.disabled == true) {
 					type = cache.type + "_disabled";
 				} else {
 					type = cache.type;
 				}
 				final String markerUrl = urlencode_rfc3986("http://cgeo.carnero.cc/_markers/marker_cache_" + type + ".png");
 
 				final StringBuilder waypoints = new StringBuilder();
 				if (cache.waypoints != null && cache.waypoints.size() > 0) {
 					for (cgWaypoint waypoint : cache.waypoints) {
 						if (waypoint.latitude == null && waypoint.longitude == null) {
 							continue;
 						}
 
 						waypoints.append("&markers=icon%3Ahttp://cgeo.carnero.cc/_markers/marker_waypoint_");
 						waypoints.append(waypoint.type);
 						waypoints.append(".png%7C");
 						waypoints.append(String.format((Locale) null, "%.6f", waypoint.latitude));
 						waypoints.append(",");
 						waypoints.append(String.format((Locale) null, "%.6f", waypoint.longitude));
 					}
 				}
 
 				mapGetter.setLevel(1);
 				mapGetter.getDrawable("http://maps.google.com/maps/api/staticmap?center=" + latlonMap + "&zoom=20&size=" + edge + "x" + edge + "&maptype=satellite&markers=icon%3A" + markerUrl + "%7C" + latlonMap + waypoints.toString() + "&sensor=false");
 
 				mapGetter.setLevel(2);
 				mapGetter.getDrawable("http://maps.google.com/maps/api/staticmap?center=" + latlonMap + "&zoom=18&size=" + edge + "x" + edge + "&maptype=satellite&markers=icon%3A" + markerUrl + "%7C" + latlonMap + waypoints.toString() + "&sensor=false");
 
 				mapGetter.setLevel(3);
 				mapGetter.getDrawable("http://maps.google.com/maps/api/staticmap?center=" + latlonMap + "&zoom=16&size=" + edge + "x" + edge + "&maptype=roadmap&markers=icon%3A" + markerUrl + "%7C" + latlonMap + waypoints.toString() + "&sensor=false");
 
 				mapGetter.setLevel(4);
 				mapGetter.getDrawable("http://maps.google.com/maps/api/staticmap?center=" + latlonMap + "&zoom=14&size=" + edge + "x" + edge + "&maptype=roadmap&markers=icon%3A" + markerUrl + "%7C" + latlonMap + waypoints.toString() + "&sensor=false");
 
 				mapGetter.setLevel(5);
 				mapGetter.getDrawable("http://maps.google.com/maps/api/staticmap?center=" + latlonMap + "&zoom=11&size=" + edge + "x" + edge + "&maptype=roadmap&markers=icon%3A" + markerUrl + "%7C" + latlonMap + waypoints.toString() + "&sensor=false");
 			}
 
 			app.markStored(cache.geocode);
 			app.removeCacheFromCache(cache.geocode);
 
 			if (handler != null) {
 				handler.sendMessage(new Message());
 			}
 		} catch (Exception e) {
 			Log.e(cgSettings.tag, "cgBase.storeCache: " + e.toString());
 		}
 	}
 
 	public void dropCache(cgeoapplication app, Activity activity, cgCache cache, Handler handler) {
 		try {
 			app.markDropped(cache.geocode);
 			app.removeCacheFromCache(cache.geocode);
 
 			handler.sendMessage(new Message());
 		} catch (Exception e) {
 			Log.e(cgSettings.tag, "cgBase.dropCache: " + e.toString());
 		}
 	}
 
 	public boolean isInViewPort(int centerLat1, int centerLon1, int centerLat2, int centerLon2, int spanLat1, int spanLon1, int spanLat2, int spanLon2) {
 		try {
 			// expects coordinates in E6 format
 			final int left1 = centerLat1 - (spanLat1 / 2);
 			final int right1 = centerLat1 + (spanLat1 / 2);
 			final int top1 = centerLon1 + (spanLon1 / 2);
 			final int bottom1 = centerLon1 - (spanLon1 / 2);
 
 			final int left2 = centerLat2 - (spanLat2 / 2);
 			final int right2 = centerLat2 + (spanLat2 / 2);
 			final int top2 = centerLon2 + (spanLon2 / 2);
 			final int bottom2 = centerLon2 - (spanLon2 / 2);
 
 			if (left2 <= left1) {
 				return false;
 			}
 			if (right2 >= right1) {
 				return false;
 			}
 			if (top2 >= top1) {
 				return false;
 			}
 			if (bottom2 <= bottom1) {
 				return false;
 			}
 
 			return true;
 		} catch (Exception e) {
 			Log.e(cgSettings.tag, "cgBase.isInViewPort: " + e.toString());
 			return false;
 		}
 	}
 
 	public boolean isCacheInViewPort(int centerLat, int centerLon, int spanLat, int spanLon, Double cacheLat, Double cacheLon) {
 		if (cacheLat == null || cacheLon == null) {
 			return false;
 		}
 
 		// viewport is defined by center, span and some (10%) reserve on every side
 		int minLat = centerLat - (spanLat / 2) - (spanLat / 10);
 		int maxLat = centerLat + (spanLat / 2) + (spanLat / 10);
 		int minLon = centerLon - (spanLon / 2) - (spanLon / 10);
 		int maxLon = centerLon + (spanLon / 2) + (spanLon / 10);
 		int cLat = (int) Math.round(cacheLat * 1e6);
 		int cLon = (int) Math.round(cacheLon * 1e6);
 		int mid = 0;
 
 		if (maxLat < minLat) {
 			mid = minLat;
 			minLat = maxLat;
 			maxLat = mid;
 		}
 
 		if (maxLon < minLon) {
 			mid = minLon;
 			minLon = maxLon;
 			maxLon = mid;
 		}
 
 		boolean latOk = false;
 		boolean lonOk = false;
 
 		if (cLat >= minLat && cLat <= maxLat) {
 			latOk = true;
 		}
 		if (cLon >= minLon && cLon <= maxLon) {
 			lonOk = true;
 		}
 
 		if (latOk == true && lonOk == true) {
 			return true;
 		} else {
 			return false;
 		}
 	}
 	private static char[] base64map1 = new char[64];
 
 	static {
 		int i = 0;
 		for (char c = 'A'; c <= 'Z'; c++) {
 			base64map1[i++] = c;
 		}
 		for (char c = 'a'; c <= 'z'; c++) {
 			base64map1[i++] = c;
 		}
 		for (char c = '0'; c <= '9'; c++) {
 			base64map1[i++] = c;
 		}
 		base64map1[i++] = '+';
 		base64map1[i++] = '/';
 	}
 	private static byte[] base64map2 = new byte[128];
 
 	static {
 		for (int i = 0; i < base64map2.length; i++) {
 			base64map2[i] = -1;
 		}
 		for (int i = 0; i < 64; i++) {
 			base64map2[base64map1[i]] = (byte) i;
 		}
 	}
 
 	public static String base64Encode(byte[] in) {
 		int iLen = in.length;
 		int oDataLen = (iLen * 4 + 2) / 3; // output length without padding
 		int oLen = ((iLen + 2) / 3) * 4; // output length including padding
 		char[] out = new char[oLen];
 		int ip = 0;
 		int op = 0;
 
 		while (ip < iLen) {
 			int i0 = in[ip++] & 0xff;
 			int i1 = ip < iLen ? in[ip++] & 0xff : 0;
 			int i2 = ip < iLen ? in[ip++] & 0xff : 0;
 			int o0 = i0 >>> 2;
 			int o1 = ((i0 & 3) << 4) | (i1 >>> 4);
 			int o2 = ((i1 & 0xf) << 2) | (i2 >>> 6);
 			int o3 = i2 & 0x3F;
 			out[op++] = base64map1[o0];
 			out[op++] = base64map1[o1];
 			out[op] = op < oDataLen ? base64map1[o2] : '=';
 			op++;
 			out[op] = op < oDataLen ? base64map1[o3] : '=';
 			op++;
 		}
 
 		return new String(out);
 	}
 
 	public static byte[] base64Decode(String text) {
 		char[] in = text.toCharArray();
 
 		int iLen = in.length;
 		if (iLen % 4 != 0) {
 			throw new IllegalArgumentException("Length of Base64 encoded input string is not a multiple of 4.");
 		}
 		while (iLen > 0 && in[iLen - 1] == '=') {
 			iLen--;
 		}
 		int oLen = (iLen * 3) / 4;
 		byte[] out = new byte[oLen];
 		int ip = 0;
 		int op = 0;
 		while (ip < iLen) {
 			int i0 = in[ip++];
 			int i1 = in[ip++];
 			int i2 = ip < iLen ? in[ip++] : 'A';
 			int i3 = ip < iLen ? in[ip++] : 'A';
 			if (i0 > 127 || i1 > 127 || i2 > 127 || i3 > 127) {
 				throw new IllegalArgumentException("Illegal character in Base64 encoded data.");
 			}
 			int b0 = base64map2[i0];
 			int b1 = base64map2[i1];
 			int b2 = base64map2[i2];
 			int b3 = base64map2[i3];
 			if (b0 < 0 || b1 < 0 || b2 < 0 || b3 < 0) {
 				throw new IllegalArgumentException("Illegal character in Base64 encoded data.");
 			}
 			int o0 = (b0 << 2) | (b1 >>> 4);
 			int o1 = ((b1 & 0xf) << 4) | (b2 >>> 2);
 			int o2 = ((b2 & 3) << 6) | b3;
 			out[op++] = (byte) o0;
 			if (op < oLen) {
 				out[op++] = (byte) o1;
 			}
 			if (op < oLen) {
 				out[op++] = (byte) o2;
 			}
 		}
 		return out;
 	}
 
 	public int getIcon(boolean cache, String type, boolean found, boolean disabled) {
 		if (gcIcons.isEmpty()) {
 			gcIcons.put("ape", R.drawable.marker_cache_ape);
 			gcIcons.put("cito", R.drawable.marker_cache_cito);
 			gcIcons.put("earth", R.drawable.marker_cache_earth);
 			gcIcons.put("event", R.drawable.marker_cache_event);
 			gcIcons.put("letterbox", R.drawable.marker_cache_letterbox);
 			gcIcons.put("locationless", R.drawable.marker_cache_locationless);
 			gcIcons.put("mega", R.drawable.marker_cache_mega);
 			gcIcons.put("multi", R.drawable.marker_cache_multi);
 			gcIcons.put("traditional", R.drawable.marker_cache_traditional);
 			gcIcons.put("virtual", R.drawable.marker_cache_virtual);
 			gcIcons.put("webcam", R.drawable.marker_cache_webcam);
 			gcIcons.put("wherigo", R.drawable.marker_cache_wherigo);
 			gcIcons.put("mystery", R.drawable.marker_cache_mystery);
 			gcIcons.put("ape-found", R.drawable.marker_cache_ape_found);
 			gcIcons.put("cito-found", R.drawable.marker_cache_cito_found);
 			gcIcons.put("earth-found", R.drawable.marker_cache_earth_found);
 			gcIcons.put("event-found", R.drawable.marker_cache_event_found);
 			gcIcons.put("letterbox-found", R.drawable.marker_cache_letterbox_found);
 			gcIcons.put("locationless-found", R.drawable.marker_cache_locationless_found);
 			gcIcons.put("mega-found", R.drawable.marker_cache_mega_found);
 			gcIcons.put("multi-found", R.drawable.marker_cache_multi_found);
 			gcIcons.put("traditional-found", R.drawable.marker_cache_traditional_found);
 			gcIcons.put("virtual-found", R.drawable.marker_cache_virtual_found);
 			gcIcons.put("webcam-found", R.drawable.marker_cache_webcam_found);
 			gcIcons.put("wherigo-found", R.drawable.marker_cache_wherigo_found);
 			gcIcons.put("mystery-found", R.drawable.marker_cache_mystery_found);
 			gcIcons.put("ape-disabled", R.drawable.marker_cache_ape_disabled);
 			gcIcons.put("cito-disabled", R.drawable.marker_cache_cito_disabled);
 			gcIcons.put("earth-disabled", R.drawable.marker_cache_earth_disabled);
 			gcIcons.put("event-disabled", R.drawable.marker_cache_event_disabled);
 			gcIcons.put("letterbox-disabled", R.drawable.marker_cache_letterbox_disabled);
 			gcIcons.put("locationless-disabled", R.drawable.marker_cache_locationless_disabled);
 			gcIcons.put("mega-disabled", R.drawable.marker_cache_mega_disabled);
 			gcIcons.put("multi-disabled", R.drawable.marker_cache_multi_disabled);
 			gcIcons.put("traditional-disabled", R.drawable.marker_cache_traditional_disabled);
 			gcIcons.put("virtual-disabled", R.drawable.marker_cache_virtual_disabled);
 			gcIcons.put("webcam-disabled", R.drawable.marker_cache_webcam_disabled);
 			gcIcons.put("wherigo-disabled", R.drawable.marker_cache_wherigo_disabled);
 			gcIcons.put("mystery-disabled", R.drawable.marker_cache_mystery_disabled);
 		}
 
 		if (wpIcons.isEmpty()) {
 			wpIcons.put("waypoint", R.drawable.marker_waypoint_waypoint);
 			wpIcons.put("flag", R.drawable.marker_waypoint_flag);
 			wpIcons.put("pkg", R.drawable.marker_waypoint_pkg);
 			wpIcons.put("puzzle", R.drawable.marker_waypoint_puzzle);
 			wpIcons.put("stage", R.drawable.marker_waypoint_stage);
 			wpIcons.put("trailhead", R.drawable.marker_waypoint_trailhead);
 		}
 
 		int icon = -1;
 		String iconTxt = null;
 
 		if (cache == true) {
 			if (type != null || type.length() > 0) {
 				if (found == true) {
 					iconTxt = type + "-found";
 				} else if (disabled == true) {
 					iconTxt = type + "-disabled";
 				} else {
 					iconTxt = type;
 				}
 			} else {
 				iconTxt = "traditional";
 			}
 
 			if (gcIcons.containsKey(iconTxt) == true) {
 				icon = gcIcons.get(iconTxt);
 			}
 		} else {
 			if (type != null || type.length() > 0) {
 				iconTxt = type;
 			} else {
 				iconTxt = "waypoint";
 			}
 
 			if (wpIcons.containsKey(iconTxt) == true) {
 				icon = wpIcons.get(iconTxt);
 			}
 		}
 
 		return icon;
 	}
 
 	public boolean runExternalMap(Activity activity, Resources res, cgWarning warning, GoogleAnalyticsTracker tracker, cgWaypoint waypoint) {
 		// waypoint
 		return runExternalMap(activity, res, warning, tracker, null, waypoint);
 	}
 
 	public boolean runExternalMap(Activity activity, Resources res, cgWarning warning, GoogleAnalyticsTracker tracker, cgCache cache) {
 		// cache
 		return runExternalMap(activity, res, warning, tracker, cache, null);
 	}
 
 	public boolean runExternalMap(Activity activity, Resources res, cgWarning warning, GoogleAnalyticsTracker tracker, cgCache cache, cgWaypoint waypoint) {
 		if (cache == null && waypoint == null) {
 			return false;
 		}
 
 		// locus
 		try {
 			final Intent intentTest = new Intent(Intent.ACTION_VIEW);
 			intentTest.setData(Uri.parse("menion.points:x"));
 
 			if (isIntentAvailable(activity, intentTest) == true) {
 				final ArrayList<cgWaypoint> waypoints = new ArrayList<cgWaypoint>();
 				// get only waypoints with coordinates
 				if (cache != null && cache.waypoints != null && cache.waypoints.isEmpty() == false) {
 					for (cgWaypoint wp : cache.waypoints) {
 						if (wp.latitude != null && wp.longitude != null) {
 							waypoints.add(wp);
 						}
 					}
 				}
 
 				final ByteArrayOutputStream baos = new ByteArrayOutputStream();
 				final DataOutputStream dos = new DataOutputStream(baos);
 
 				dos.writeInt(1); // not used
 				if (cache != null) {
 					if (waypoints == null || waypoints.isEmpty() == true) {
 						dos.writeInt(1); // cache only
 					} else {
 						dos.writeInt((1 + waypoints.size())); // cache and waypoints
 					}
 				} else {
 					dos.writeInt(1); // one waypoint
 				}
 
 				int icon = -1;
 				if (cache != null) {
 					icon = getIcon(true, cache.type, cache.found, cache.disabled);
 				} else {
 					icon = getIcon(false, waypoint.type, false, false);
 				}
 
 				if (icon > 0) {
 					// load icon
 					Bitmap bitmap = BitmapFactory.decodeResource(res, icon);
 					ByteArrayOutputStream baos2 = new ByteArrayOutputStream();
 					bitmap.compress(Bitmap.CompressFormat.PNG, 100, baos2);
 					byte[] image = baos2.toByteArray();
 
 					dos.writeInt(image.length);
 					dos.write(image);
 				} else {
 					// no icon
 					dos.writeInt(0); // no image
 				}
 
 				// name
 				if (cache != null && cache.geocode != null && cache.geocode.length() > 0) {
 					dos.writeUTF(cache.geocode.toUpperCase());
 				} else if (waypoint != null && waypoint.lookup != null && waypoint.lookup.length() > 0) {
 					dos.writeUTF(waypoint.lookup.toUpperCase());
 				} else {
 					dos.writeUTF("");
 				}
 
 				// description
 				if (cache != null && cache.name != null && cache.name.length() > 0) {
 					dos.writeUTF(cache.name);
 				} else if (waypoint != null && waypoint.name != null && waypoint.name.length() > 0) {
 					dos.writeUTF(waypoint.name);
 				} else {
 					dos.writeUTF("");
 				}
 
 				// additional data :: keyword, button title, package, activity, data name, data content
 				if (cache != null && cache.geocode != null && cache.geocode.length() > 0) {
 					dos.writeUTF("intent;c:geo;carnero.cgeo;carnero.cgeo.cgeodetail;geocode;" + cache.geocode);
 				} else if (waypoint != null && waypoint.id != null && waypoint.id > 0) {
 					dos.writeUTF("intent;c:geo;carnero.cgeo;carnero.cgeo.cgeowaypoint;id" + waypoint.id);
 				} else {
 					dos.writeUTF("");
 				}
 
 				if (cache != null && cache.latitude != null && cache.longitude != null) {
 					dos.writeDouble(cache.latitude); // latitude
 					dos.writeDouble(cache.longitude); // longitude
 				} else if (waypoint != null && waypoint.latitude != null && waypoint.longitude != null) {
 					dos.writeDouble(waypoint.latitude); // latitude
 					dos.writeDouble(waypoint.longitude); // longitude
 				}
 
 				// cache waypoints
 				if (waypoints != null && waypoints.isEmpty() == false) {
 					for (cgWaypoint wp : waypoints) {
 						if (wp == null || wp.latitude == null || wp.longitude == null) {
 							continue;
 						}
 
 						final int wpIcon = getIcon(false, wp.type, false, false);
 
 						if (wpIcon > 0) {
 							// load icon
 							Bitmap bitmap = BitmapFactory.decodeResource(res, wpIcon);
 							ByteArrayOutputStream baos2 = new ByteArrayOutputStream();
 							bitmap.compress(Bitmap.CompressFormat.PNG, 100, baos2);
 							byte[] image = baos2.toByteArray();
 
 							dos.writeInt(image.length);
 							dos.write(image);
 						} else {
 							// no icon
 							dos.writeInt(0); // no image
 						}
 
 						// name
 						if (wp.lookup != null && wp.lookup.length() > 0) {
 							dos.writeUTF(wp.lookup.toUpperCase());
 						} else {
 							dos.writeUTF("");
 						}
 
 						// description
 						if (wp.name != null && wp.name.length() > 0) {
 							dos.writeUTF(wp.name);
 						} else {
 							dos.writeUTF("");
 						}
 
 						// additional data :: keyword, button title, package, activity, data name, data content
 						if (wp.id != null && wp.id > 0) {
 							dos.writeUTF("intent;c:geo;carnero.cgeo;carnero.cgeo.cgeowaypoint;id" + wp.id);
 						} else {
 							dos.writeUTF("");
 						}
 
 						dos.writeDouble(wp.latitude); // latitude
 						dos.writeDouble(wp.longitude); // longitude
 					}
 				}
 
 				final Intent intent = new Intent();
 				intent.setAction(Intent.ACTION_VIEW);
 				intent.setData(Uri.parse("menion.points:data"));
 				intent.putExtra("data", baos.toByteArray());
 
 				activity.startActivity(intent);
 
 				sendAnal(activity, tracker, "/external/locus");
 
 				return true;
 			}
 		} catch (Exception e) {
 			// nothing
 		}
 
 		// rmaps
 		try {
 			final Intent intent = new Intent("com.robert.maps.action.SHOW_POINTS");
 
 			if (isIntentAvailable(activity, intent) == true) {
 				final ArrayList<String> locations = new ArrayList<String>();
 				if (cache != null && cache.latitude != null && cache.longitude != null) {
 					locations.add(String.format((Locale) null, "%.6f", cache.latitude) + "," + String.format((Locale) null, "%.6f", cache.longitude) + ";" + cache.geocode + ";" + cache.name);
 				} else if (waypoint != null && waypoint.latitude != null && waypoint.longitude != null) {
 					locations.add(String.format((Locale) null, "%.6f", waypoint.latitude) + "," + String.format((Locale) null, "%.6f", waypoint.longitude) + ";" + waypoint.lookup + ";" + waypoint.name);
 				}
 
 				intent.putStringArrayListExtra("locations", locations);
 
 				activity.startActivity(intent);
 
 				sendAnal(activity, tracker, "/external/rmaps");
 
 				return true;
 			}
 		} catch (Exception e) {
 			// nothing
 		}
 
 		// default map
 		try {
 			if (cache != null && cache.latitude != null && cache.longitude != null) {
 				activity.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("geo:" + cache.latitude + "," + cache.longitude)));
 			} else if (waypoint != null && waypoint.latitude != null && waypoint.longitude != null) {
 				activity.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("geo:" + waypoint.latitude + "," + waypoint.longitude)));
 			}
 
 			sendAnal(activity, tracker, "/external/native/maps");
 
 			return true;
 		} catch (Exception e) {
 			// nothing
 		}
 
 		Log.i(cgSettings.tag, "cgBase.runExternalMap: No maps application available.");
 
 		if (warning != null && res != null) {
 			warning.showToast(res.getString(R.string.err_application_no));
 		}
 
 		return false;
 	}
 
 	public boolean runNavigation(Activity activity, Resources res, cgSettings settings, cgWarning warning, GoogleAnalyticsTracker tracker, Double latitude, Double longitude) {
 		return runNavigation(activity, res, settings, warning, tracker, latitude, longitude, null, null);
 	}
 
 	public boolean runNavigation(Activity activity, Resources res, cgSettings settings, cgWarning warning, GoogleAnalyticsTracker tracker, Double latitude, Double longitude, Double latitudeNow, Double longitudeNow) {
 		if (activity == null) {
 			return false;
 		}
 		if (settings == null) {
 			return false;
 		}
 
 		// Google Navigation
 		if (settings.useGNavigation == 1) {
 			try {
 				activity.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("google.navigation:q=" + latitude + "," + longitude)));
 
 				sendAnal(activity, tracker, "/external/native/navigation");
 
 				return true;
 			} catch (Exception e) {
 				// nothing
 			}
 		}
 
 		// Google Maps Directions
 		try {
 			if (latitudeNow != null && longitudeNow != null) {
 				activity.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("http://maps.google.com/maps?f=d&saddr=" + latitudeNow + "," + longitudeNow + "&daddr=" + latitude + "," + longitude)));
 			} else {
 				activity.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("http://maps.google.com/maps?f=d&daddr=" + latitude + "," + longitude)));
 			}
 
 			sendAnal(activity, tracker, "/external/native/maps");
 
 			return true;
 		} catch (Exception e) {
 			// nothing
 		}
 
 		Log.i(cgSettings.tag, "cgBase.runNavigation: No navigation application available.");
 
 		if (warning != null && res != null) {
 			warning.showToast(res.getString(R.string.err_navigation_no));
 		}
 
 		return false;
 	}
 
 	public String getMapUserToken() {
 		final String page = request("www.geocaching.com", "/map/default.aspx", "GET", "", 0, false);
 		String usertoken = null;
 
 		if (page != null && page.length() > 0) {
 			final Pattern pattern = Pattern.compile("var userToken[^=]*=[^']*'([^']+)';", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE);
 
 			final Matcher matcher = pattern.matcher(page);
 			while (matcher.find()) {
 				if (matcher.groupCount() > 0) {
 					usertoken = matcher.group(1);
 				}
 			}
 		}
 
 		return usertoken;
 	}
 
 	public void sendAnal(Context context, String page) {
 		(new sendAnalThread(context, null, page)).start();
 	}
 
 	public void sendAnal(Context context, GoogleAnalyticsTracker tracker, String page) {
 		(new sendAnalThread(context, tracker, page)).start();
 	}
 
 	private class sendAnalThread extends Thread {
 
 		Context context = null;
 		GoogleAnalyticsTracker tracker = null;
 		String page = null;
 		boolean startedHere = false;
 
 		public sendAnalThread(Context contextIn, GoogleAnalyticsTracker trackerIn, String pageIn) {
 			context = contextIn;
 			tracker = trackerIn;
 			page = pageIn;
 		}
 
 		@Override
 		public void run() {
 			try {
 				if (page == null || page.length() == 0) {
 					page = "/";
 				}
 
 				if (tracker == null && context != null) {
 					startedHere = true;
 					tracker = GoogleAnalyticsTracker.getInstance();
 					tracker.start(cgSettings.analytics, context);
 				}
 
 				tracker.trackPageView(page);
 				tracker.dispatch();
 
 				Log.i(cgSettings.tag, "Logged use of " + page);
 
 				if (startedHere == true) {
 					tracker.stop();
 				}
 			} catch (Exception e) {
 				// nothing
 			}
 		}
 	}
 
 	public void showProgress(Activity activity, boolean status) {
 		if (activity == null) {
 			return;
 		}
 
 		final ProgressBar progress = (ProgressBar) activity.findViewById(R.id.actionbar_progress);
 		if (status == true) {
 			progress.setVisibility(View.VISIBLE);
 		} else {
 			progress.setVisibility(View.GONE);
 		}
 	}
 
 	public void goHome(Activity activity) {
 		final Intent intent = new Intent(activity, cgeo.class);
 		intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
 
 		activity.startActivity(intent);
 		activity.finish();
 	}
 
 	public void setTitle(Activity activity, String text) {
 		if (activity == null || text == null) {
 			return;
 		}
 
 		final TextView title = (TextView) activity.findViewById(R.id.actionbar_title);
 		if (title != null) {
 			title.setText(text);
 		}
 	}
 	
 	public static HashMap<String, Object> ratingDecrypt(String text) {
 		final HashMap<String, Object> rating = new HashMap<String, Object>();
         String size = null;
         Float difficulty = null;
         Float terrain = null;
 		
 		final StringBuilder sb = new StringBuilder(text);
         int sbLen = sb.length();
 
         if (sbLen > 6) {
             // cut leading zero
             while (sb.length() > 6) {
                 sb.delete(0, 1);
             }
         } else if (sbLen < 6) {
             // add missing leading zero
             while (sb.length() < 6) {
                 sb.insert(0, "h");
             }
         }
 
         sbLen = sb.length(); // have to be six
         long value = 0;
         for (int i = 0; i < sbLen; i ++) {
             value += ratingKey.indexOf(sb.charAt(i)) * Math.pow(42, (5 - i));
         }
 
         long mod = (value - 131586) % 16777216;
         long sizePre = (long)(mod / Math.pow(42, 3));
 
         long diff = -1;
         if (sizePre == 0) {
             diff = 0;
         } else if (sizePre == 1) {
             diff = 131072;
         } else if (sizePre == 3) {
             diff = 262144;
         } else if (sizePre == 5) {
             diff = 393217;
         } else if (sizePre == 7) {
             diff = 524288;
         } else if (sizePre == 8) {
             diff = 655360;
         } else if (sizePre == 12) {
             diff = 917504;
         }
 
         long terrainPre = (long)((mod - diff) / 252);
         long difficultyPre = (long)(((mod - diff) % 42) - (terrainPre * 4));
 
         // cache size
         if (sizePre == 0) {
             size = "not chosen";
         } else if (sizePre == 1) {
             size = "micro";
         } else if (sizePre == 3) {
             size = "regular";
         } else if (sizePre == 5) {
             size = "large";
         } else if (sizePre == 7) {
             size = "virtual";
         } else if (sizePre == 8) {
             size = "unknown";
         } else if (sizePre == 12) {
             size = "small";
         }
 
         // terrain rating
         if (terrainPre == 0) {
             terrain = new Float(1.0);
         } else if (terrainPre == 1) {
             terrain = new Float(1.5);
         } else if (terrainPre == 2) {
             terrain = new Float(2.0);
         } else if (terrainPre == 3) {
             terrain = new Float(2.5);
         } else if (terrainPre == 4) {
             terrain = new Float(3.0);
         } else if (terrainPre == 5) {
             terrain = new Float(3.5);
         } else if (terrainPre == 6) {
             terrain = new Float(4.0);
         } else if (terrainPre == 7) {
             terrain = new Float(4.5);
         } else {
             terrain = new Float(5.0);
         }
 
         // difficulty rating
         if (difficultyPre == 0) {
             difficulty = new Float(1.0);
         } else if (difficultyPre == 1) {
             difficulty = new Float(1.5);
         } else if (difficultyPre == 2) {
             difficulty = new Float(2.0);
         } else if (difficultyPre == 3) {
             difficulty = new Float(2.5);
         } else if (difficultyPre == 5) {
             difficulty = new Float(3.0);
         } else if (difficultyPre == 6) {
             difficulty = new Float(3.5);
         } else if (difficultyPre == 6) {
             difficulty = new Float(4.0);
         } else if (difficultyPre == 7) {
             difficulty = new Float(4.5);
         } else {
             difficulty = new Float(5.0);
         }
 
         rating.put("size", size);
         rating.put("terrain", terrain);
         rating.put("difficulty", difficulty);
 
 		return rating;
 	}
 
 	public static String xorEnDecrypt(String text) {
 		final int textLen = text.length();
 		final int keyLen = xorKey.length();
 
 		final StringBuilder outSb = new StringBuilder(textLen);
 
 		char c;
 		char k;
 		for (int i = 0; i < textLen; i++) {
 			k = xorKey.charAt(i % keyLen);
 			c = text.charAt(i);
 
 			c = (char) (c ^ k);
 
 			outSb.append(c);
 		}
 
 		return outSb.toString();
 	}
 }
