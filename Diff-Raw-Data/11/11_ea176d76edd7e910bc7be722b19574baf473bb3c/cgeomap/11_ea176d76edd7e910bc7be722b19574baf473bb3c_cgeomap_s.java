 package carnero.cgeo;
 
 import android.app.Activity;
 import android.app.ProgressDialog;
 import java.util.ArrayList;
 import android.os.Bundle;
 import android.view.Menu;
 import android.view.SubMenu;
 import android.view.MenuItem;
 import android.content.Context;
 import android.content.DialogInterface;
 import android.content.Intent;
 import android.content.SharedPreferences;
 import android.graphics.drawable.Drawable;
 import android.os.Handler;
 import android.os.Message;
 import android.os.PowerManager;
 import android.text.Html;
 import android.widget.TextView;
 import com.google.android.maps.GeoPoint;
 import com.google.android.maps.MapView;
 import com.google.android.maps.MapActivity;
 import com.google.android.maps.MapController;
 import android.util.Log;
 import android.view.View;
 import android.view.Window;
 import android.widget.LinearLayout;
 import java.lang.reflect.Field;
 import java.util.HashMap;
 import java.util.Locale;
 
 public class cgeomap extends MapActivity {
 	private Activity activity = null;
     private MapView mapView = null;
 	private MapController mapController = null;
 	private cgSettings settings = null;
 	private cgBase base = null;
 	private cgWarning warning = null;
     private cgeoapplication app = null;
 	private SharedPreferences.Editor prefsEdit = null;
 	private cgGeo geo = null;
 	private cgDirection dir = null;
 	private cgUpdateLoc geoUpdate = new update();
 	private cgUpdateDir dirUpdate = new updateDir();
     private PowerManager pm = null;
 	protected PowerManager.WakeLock  wakeLock = null;
 	private boolean followLocation = false;
 	private boolean initLocation = true;
 	private cgMapOverlay overlay = null;
 	private cgUsersOverlay overlayUsers = null;
 	private cgMapMyOverlay overlayMyLoc = null;
 	private Drawable pin = null;
 	private boolean fromDetail = false;
     private Double oneLatitude = null;
     private Double oneLongitude = null;
     private Long searchId = null;
 	private String geocode = null;
 	private Integer centerLatitude = null;
 	private Integer centerLongitude = null;
 	private Integer spanLatitude = null;
 	private Integer spanLongitude = null;
 	private Integer centerLatitudeUsers = null;
 	private Integer centerLongitudeUsers = null;
 	private Integer spanLatitudeUsers = null;
 	private Integer spanLongitudeUsers = null;
 	private ArrayList<cgCache> caches = new ArrayList<cgCache>();
     private ArrayList<cgCoord> coordinates = new ArrayList<cgCoord>();
 	private ArrayList<cgUser> users = new ArrayList<cgUser>();
 	private loadCaches loadingThread = null;
 	private loadUsers usersThread = null;
 	private long closeShowed = 0l;
 	private LinearLayout close = null;
 	private TextView closeGC = null;
 	private TextView closeDst = null;
 	private ProgressDialog waitDialog = null;
     private int detailTotal = 0;
     private int detailProgress = 0;
     private Long detailProgressTime = 0l;
 	private boolean firstRun = true;
     private geocachesLoadDetails threadD = null;
 	private int closeCounter = 0;
 	private boolean progressBar = false;
 	protected boolean searching = false;
 	protected boolean searchingUsers = false;
 	protected boolean searchingForClose = false;
 	protected boolean live = false;
 	final private static HashMap<String, Integer> gcIconsClear = new HashMap<String, Integer>();
 	final private static HashMap<String, Integer> gcIcons = new HashMap<String, Integer>();
 	final private static HashMap<String, Integer> wpIcons = new HashMap<String, Integer>();
 
 	final private Handler startLoading = new Handler() {
 		@Override
 		public void handleMessage(Message msg) {
 			changeTitle(true);
 		}
 	};
 
 	final private Handler loadCacheFromDbHandler = new Handler() {
 		@Override
 		public void handleMessage(Message msg) {
 			try {
 				if (app != null && searchId != null && app.getError(searchId) != null && app.getError(searchId).length() > 0) {
 					warning.showToast("Sorry, c:geo failed to load cache or caches.");
 
 					changeTitle(false);
 					return;
 				}
 
 				addOverlays(true);
 			} catch (Exception e) {
 				Log.e(cgSettings.tag, "cgeomap.loadCacheFromDbHandler: " + e.toString());
 
 				changeTitle(false);
 			}
 		}
 	};
 
 	final private Handler loadCachesHandler = new Handler() {
 		@Override
 		public void handleMessage(Message msg) {
 			try {
 				if (app != null && app.getError(searchId) != null && app.getError(searchId).length() > 0) {
 					warning.showToast("Sorry, c:geo failed to download caches because of " + app.getError(searchId) + ".");
 
 					searching = false;
 					changeTitle(false);
 					return;
 				}
 
 				addOverlays(true);
 			} catch (Exception e) {
 				Log.e(cgSettings.tag, "cgeomap.loadCachesHandler: " + e.toString());
 				
 				searching = false;
 				changeTitle(false);
 				return;
 			}
 		}
 	};
 	
 	final private Handler loadUsersHandler = new Handler() {
 		@Override
 		public void handleMessage(Message msg) {
 			try {
 				addOverlays(false);
 			} catch (Exception e) {
 				Log.e(cgSettings.tag, "cgeomap.loadUsersHandler: " + e.toString());
 
 				searchingUsers = false;
 				return;
 			}
 		}
 	};
 
 	final private Handler loadDetailsHandler = new Handler() {
 		@Override
 		public void handleMessage(Message msg) {
             if (msg.what == 0) {
                 if (waitDialog != null) {
                     Float diffTime = new Float((System.currentTimeMillis() - detailProgressTime) / 1000); // seconds left
                     Float oneCache = diffTime / detailProgress; // left time per cache
                     Float etaTime = (detailTotal - detailProgress) * oneCache; // seconds remaining
 
                     waitDialog.setProgress(detailProgress);
                     if (etaTime < (1 / 60)) {
                         waitDialog.setMessage("downloading caches...\neta: less than minute");
                     } else {
                         waitDialog.setMessage("downloading caches...\neta: " + String.format(Locale.getDefault(), "%.0f", (etaTime / 60)) + " mins");
                     }
                 }
             } else {
                 if (waitDialog != null) {
                     waitDialog.dismiss();
                     waitDialog.setOnCancelListener(null);
                 }
 
 				if (geo == null) geo = app.startGeo(activity, geoUpdate, base, settings, warning, 0, 0);
 				if (settings.useCompass == 1 && dir == null) dir = app.startDir(activity, dirUpdate, warning);
             }
 		}
 	};
 
 	final private Handler setCloseHandler = new Handler() {
 		@Override
 		public void handleMessage(Message msg) {
 			try {
 				if (close == null) close = (LinearLayout)findViewById(R.id.close);
 				if (closeGC == null) closeGC = (TextView)findViewById(R.id.close_gc);
 				if (closeDst == null) closeDst = (TextView)findViewById(R.id.close_dst);
 
 				final int index = msg.what;
 				if (geo == null || caches == null || caches.isEmpty() == true || index == -1 || caches.size() <= index) {
 					if ((System.currentTimeMillis() - 5000) < closeShowed) {
 						close.setVisibility(View.GONE);
 						searchingForClose = false;
 						return;
 					}
 				}
 
 				cgCache cache = null;
 				try { // probably trying to get cache that doesn't exist in list
 					cache = caches.get(index);
 				} catch (Exception e) {
 					if ((System.currentTimeMillis() - 5000) < closeShowed) close.setVisibility(View.GONE);
 					searchingForClose = false;
 					return;
 				}
 
 				if (cache == null) {
 					if ((System.currentTimeMillis() - 5000) < closeShowed) close.setVisibility(View.GONE);
 					searchingForClose = false;
 
 					return;
 				}
 
 				final Double distance = base.getDistance(geo.latitudeNow, geo.longitudeNow, cache.latitude, cache.longitude);
 
 				close.setClickable(false);
 				close.setOnClickListener(null);
 
 				if (cache != null && geo != null && followLocation == true && geo.speedNow != null && geo.speedNow > 6) { // more than 6 m/s
 					if (closeCounter < 5) {
 						closeCounter ++;
 					} else {
 						closeShowed = System.currentTimeMillis();
 						close.setVisibility(View.VISIBLE);
 
 						if (geo != null) {
 							closeDst.setText(base.getHumanDistance(distance));
 						} else {
 							closeDst.setText("---");
 						}
 						if (cache.name != null && cache.name.length() > 0) {
 							closeGC.setText(cache.name);
 						} else {
 							closeGC.setText(cache.geocode);
 						}
 						if (cache.type != null && gcIcons.containsKey(cache.type) == true) { // cache icon
 							closeGC.setCompoundDrawablesWithIntrinsicBounds((Drawable)activity.getResources().getDrawable(gcIconsClear.get(cache.type)), null, null, null);
 						} else { // unknown cache type, "mystery" icon
 							closeGC.setCompoundDrawablesWithIntrinsicBounds((Drawable)activity.getResources().getDrawable(gcIconsClear.get("mystery")), null, null, null);
 						}
 						close.setClickable(true);
 						close.setOnClickListener(new closeClickListener(cache));
 
 						close.bringToFront();
 
 						closeCounter = 5;
 					}
 				} else {
 					if (closeCounter > 0) {
 						closeCounter --;
 					} else {
 						if (closeShowed < (System.currentTimeMillis() - (30 * 1000))) close.setVisibility(View.GONE);
 
 						closeCounter = 0;
 					}
 				}
 			} catch (Exception e) {
 				Log.e(cgSettings.tag, "cgeomap.setCloseHandler.handleMessage: " + e.toString());
 			}
 
 			searchingForClose = false;
 		}
 	};
 
 	@Override
 	public void onCreate(Bundle savedInstanceState) {
 		super.onCreate(savedInstanceState);
 
 		// class init
 		activity = this;
         app = (cgeoapplication)activity.getApplication();
 		app.setAction(null);
         settings = new cgSettings(activity, getSharedPreferences(cgSettings.preferences, 0));
         base = new cgBase(app, settings, getSharedPreferences(cgSettings.preferences, 0));
         warning = new cgWarning(activity);
 		prefsEdit = getSharedPreferences(cgSettings.preferences, 0).edit();
 
 		// set layout
 		progressBar = requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
 		setTitle("map");
 		if (settings.skin == 1) setContentView(R.layout.map_light);
 		else setContentView(R.layout.map_dark);
 
 		// keep backlight on
 		pm = (PowerManager)getSystemService(Context.POWER_SERVICE);
 		wakeLock = pm.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK, "c:geo");
 		wakeLock.acquire();
 
         if (geo == null) geo = app.startGeo(activity, geoUpdate, base, settings, warning, 0, 0);
 		if (settings.useCompass == 1 && dir == null) dir = app.startDir(activity, dirUpdate, warning);
 
 		mapView = (MapView)findViewById(R.id.map);
 		mapController = mapView.getController();
 		mapView.getOverlays().clear();
 
 		if (overlayMyLoc == null) {
 			overlayMyLoc = new cgMapMyOverlay(settings);
 			mapView.getOverlays().add(overlayMyLoc);
 		}
 
 		// get parameters
 		Bundle extras = getIntent().getExtras();
 		if (extras != null) {
 			fromDetail = extras.getBoolean("detail");
             searchId = extras.getLong("searchid");
             geocode = extras.getString("geocode");
 			oneLatitude = extras.getDouble("latitude");
 			oneLongitude = extras.getDouble("longitude");
 		}
 
 		if (settings.maptype == settings.mapSatellite) {
 			mapView.setSatellite(true);
 		} else {
 			mapView.setSatellite(false);
 		}
 		mapView.setBuiltInZoomControls(true);
 		mapView.displayZoomControls(true);
 
 		mapController.setZoom(settings.mapzoom);
 
 		if ((searchId == null || searchId <= 0) && (oneLatitude == null || oneLongitude == null)) {
 			setTitle("live map");
 			searchId = null;
 			live = true;
 			initLocation = false;
 			followLocation = true;
 			
 			loadingThread = new loadCaches(loadCachesHandler, mapView);
 			if (settings.maplive == 1) loadingThread.enable();
 			else loadingThread.disable();
 			loadingThread.start();
 
 			myLocationInMiddle();
 		} else
 		if (searchId != null && searchId > 0) {
 			setTitle("map");
 			live = false;
 			initLocation = true;
 			followLocation = false;
 			
 			(new loadCacheFromDb(loadCacheFromDbHandler)).start();
 		} else
 		if (geocode != null && geocode.length() > 0) {
 			setTitle("map");
 			live = false;
 			initLocation = true;
 			followLocation = false;
 
 			(new loadCacheFromDb(loadCacheFromDbHandler)).start();
 		} else
 		if (oneLatitude != null && oneLongitude != null) {
 			setTitle("map");
 			searchId = null;
 			live = false;
 			initLocation = true;
 			followLocation = false;
 
 			addOverlays(true);
 		}
 
 		usersThread = new loadUsers(loadUsersHandler, mapView);
 		if (settings.publicLoc == 1) usersThread.enable();
 		else usersThread.disable();
 		usersThread.start();
 	}
 
 	@Override
 	protected boolean isRouteDisplayed() {
 		return false;
 	}
 
 	@Override
 	public void onResume() {
 		super.onResume();
 
 		app.setAction(null);
 		if (geo == null) geo = app.startGeo(activity, geoUpdate, base, settings, warning, 0, 0);
 		if (settings.useCompass == 1 && dir == null) dir = app.startDir(activity, dirUpdate, warning);
 		
 		// keep backlight on
         if (pm == null) {
             pm = (PowerManager)getSystemService(Context.POWER_SERVICE);
         }
 
         if (wakeLock == null || wakeLock.isHeld() == false) {
             wakeLock = pm.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK, "c:geo");
             wakeLock.acquire();
         }
 
         // restart loading threads
 		if (loadingThread != null) loadingThread.kill();
 		if (usersThread != null) usersThread.kill();
         
         if (live == true) {
 			loadingThread = new loadCaches(loadCachesHandler, mapView);
 			if (settings.maplive == 1) loadingThread.enable();
 			else loadingThread.disable();
 			loadingThread.start();
         }
         
 		usersThread = new loadUsers(loadUsersHandler, mapView);
 		if (settings.publicLoc == 1) usersThread.enable();
 		else usersThread.disable();
 		usersThread.start();
 	}
 
 	@Override
 	public void onStop() {
 		if (dir != null) dir = app.removeDir(dir);
 		if (geo != null) geo = app.removeGeo(geo);
 
 		savePrefs();
 
 		if (wakeLock != null && wakeLock.isHeld() == true) wakeLock.release();
         if (mapView != null) mapView.destroyDrawingCache();
 
 		if (loadingThread != null) loadingThread.kill();
 		if (usersThread != null) usersThread.kill();
 
 		super.onStop();
 	}
 
 	@Override
 	public void onPause() {
 		if (loadingThread != null) loadingThread.kill();
 		if (usersThread != null) usersThread.kill();
 
 		if (dir != null) dir = app.removeDir(dir);
 		if (geo != null) geo = app.removeGeo(geo);
 
 		savePrefs();
 
 		if (wakeLock != null && wakeLock.isHeld() == true) wakeLock.release();
         if (mapView != null) mapView.destroyDrawingCache();
         
 		super.onPause();
 	}
 
 	@Override
 	public void onDestroy() {
 		if (loadingThread != null) loadingThread.kill();
 		if (usersThread != null) usersThread.kill();
 
 		if (dir != null) dir = app.removeDir(dir);
 		if (geo != null) geo = app.removeGeo(geo);
 
 		savePrefs();
 
 		if (wakeLock != null && wakeLock.isHeld() == true) wakeLock.release();
 
         if (mapView != null) {
             mapView.destroyDrawingCache();
     		mapView = null;
         }
 
 		try {
 			// clean up tiles from memory
 			Class<?> tileClass = Class.forName("com.google.googlenav.map.Tile");
 			Field fTileCache = tileClass.getDeclaredField("tileObjectCache");
 			fTileCache.setAccessible(true);
 			Object[] tileObjectCache = (Object[]) fTileCache.get(null);
 			for (int i = 0; i < tileObjectCache.length; i++) {
 				tileObjectCache[i] = null;
 			}
 		} catch (Exception e) {
 			Log.e(cgSettings.tag, "cgeomap.onDestroy: " + e.toString());
 		}
 
 		super.onDestroy();
 	}
 
 	@Override
 	public boolean onCreateOptionsMenu(Menu menu) {
 		menu.add(0, 1, 0, "my location").setIcon(android.R.drawable.ic_menu_mylocation);
 		menu.add(0, 2, 0, "hide trail").setIcon(android.R.drawable.ic_menu_recent_history);
 		menu.add(0, 3, 0, "disable live").setIcon(android.R.drawable.ic_menu_close_clear_cancel);
 		menu.add(0, 4, 0, "store for offline").setIcon(android.R.drawable.ic_menu_set_as).setVisible(false);
 		menu.add(0, 0, 0, "map view").setIcon(android.R.drawable.ic_menu_mapmode);
 
 		SubMenu subMenu = menu.addSubMenu(0, 5, 0, "select point").setIcon(android.R.drawable.ic_menu_myplaces);
 		if (coordinates.size() > 0) {
 			int cnt = 6;
 			for (cgCoord coordinate : coordinates) {
 				subMenu.add(0, cnt, 0, Html.fromHtml(coordinate.name) + " (" + coordinate.type + ")");
 				cnt ++;
 			}
 		}
 
 		return true;
 	}
 
 	@Override
 	public boolean onPrepareOptionsMenu(Menu menu) {
 		super.onPrepareOptionsMenu(menu);
 
 		MenuItem item;
 		try {
 			item = menu.findItem(0); // view
 			if (mapView != null && mapView.isSatellite() == false) item.setTitle("satellite view");
 			else item.setTitle("map view");
 
 			item = menu.findItem(1); // follow my location
 			if (followLocation == true) item.setTitle("don't follow");
 			else item.setTitle("my location");
 
 			item = menu.findItem(2); // show trail
 			if (settings.maptrail == 1) item.setTitle("hide trail");
 			else item.setTitle("show trail");
 
 			item = menu.findItem(3); // live map
 			if (live == false) {
 				item.setVisible(false);
 				item.setTitle("enable live");
 			} else {
 				if (loadingThread.enabled == true) item.setTitle("disable live");
 				else item.setTitle("enable live");
 			}
 
 			item = menu.findItem(4); // store loaded
 			if (live == true && caches != null && caches.size() > 0 && searching == false) item.setVisible(true);
 			else item.setVisible(false);
 
 			item = menu.findItem(5);
 			item.setVisible(false);
 
 			SubMenu subMenu = item.getSubMenu();
 			subMenu.clear();
 			if (coordinates.size() > 0) {
 				int cnt = 6;
 				for (cgCoord coordinate : coordinates) {
 					subMenu.add(0, cnt, 0, Html.fromHtml(coordinate.name) + " (" + coordinate.type + ")");
 					cnt ++;
 				}
 				item.setVisible(true);
 			}
 		} catch (Exception e) {
 			Log.e(cgSettings.tag, "cgeomap.onPrepareOptionsMenu: " + e.toString());
 		}
 
 		return true;
 	}
 
 	@Override
 	public boolean onOptionsItemSelected(MenuItem item) {
 		int id = item.getItemId();
 		
 		if (id == 0) {
 			if (mapView != null && mapView.isSatellite() == false) {
 				mapView.setSatellite(true);
 
 				prefsEdit.putInt("maptype", settings.mapSatellite);
 				prefsEdit.commit();
 			} else {
 				mapView.setSatellite(false);
 
 				prefsEdit.putInt("maptype", settings.mapClassic);
 				prefsEdit.commit();
 			}
 
 			return true;
 		} else if (id == 1) {
 			if (followLocation == true) {
 				followLocation = false;
 			} else {
 				followLocation = true;
 				myLocationInMiddle();
 			}
 
 			return true;
 		} else if (id == 2) {
 			if (settings.maptrail == 1) {
 				prefsEdit.putInt("maptrail", 0);
 				prefsEdit.commit();
 
 				settings.maptrail = 0;
 			} else {
 				prefsEdit.putInt("maptrail", 1);
 				prefsEdit.commit();
 				
 				settings.maptrail = 1;
 			}
 		} else if (id == 3) {
 			if (loadingThread.state() == true) {
 				dismissClose();
 				loadingThread.disable();
 
 				prefsEdit.putInt("maplive", 0);
 				prefsEdit.commit();
 			} else {
 				loadingThread.enable();
 
 				prefsEdit.putInt("maplive", 1);
 				prefsEdit.commit();
 			}
 		} else if (id == 4) {
 			ArrayList<String> geocodes = new ArrayList<String>();
 			
 			try {
 				if (coordinates != null && coordinates.size() > 0) {
 					for (cgCoord coord : coordinates) {
 						if (coord.geocode != null && coord.geocode.length() > 0) {
 							if (app.isOffline(coord.geocode, null) == false) {
 								geocodes.add(coord.geocode);
 							}
 						}
 					}
 				}
 			} catch (Exception e) {
 				Log.e(cgSettings.tag, "cgeomap.onOptionsItemSelected.#4: " + e.toString());
 			}
 
 			detailTotal = geocodes.size();
 
 			if (detailTotal == 0) {
 				warning.showToast("There is nothing to be saved.");
 				
 				return true;
 			}
 
 			waitDialog = new ProgressDialog(this);
 			waitDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
 				public void onCancel(DialogInterface arg0) {
 					try {
 						if (threadD != null) threadD.kill();
 
 						if (geo == null) geo = app.startGeo(activity, geoUpdate, base, settings, warning, 0, 0);
 						if (settings.useCompass == 1 && dir == null) dir = app.startDir(activity, dirUpdate, warning);
 					} catch (Exception e) {
 						Log.e(cgSettings.tag, "cgeocaches.onPrepareOptionsMenu.onCancel: " + e.toString());
 					}
 				}
 			});
 			waitDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
 			Float etaTime = new Float((detailTotal * 7) / 60);
 			if (etaTime < 1) {
 				waitDialog.setMessage("downloading caches...\neta: less than minute");
 			} else {
 				waitDialog.setMessage("downloading caches...\neta: " + String.format(Locale.getDefault(), "%.0f", new Float((detailTotal * 7) / 60)) + " mins");
 			}
 			waitDialog.setCancelable(true);
 			waitDialog.setMax(detailTotal);
 			waitDialog.show();
 
 			detailProgressTime = System.currentTimeMillis();
 
 			threadD = new geocachesLoadDetails(loadDetailsHandler, geocodes);
 			threadD.start();
 
 			return true;
 		} else if (id > 5 && coordinates.get(id - 6) != null) {
 			try {
 				cgCoord coordinate = coordinates.get(id - 6);
 
 				followLocation = false;
 				centerMap(coordinate.latitude, coordinate.longitude);
 			} catch (Exception e) {
 				Log.e(cgSettings.tag, "cgeomap.onOptionsItemSelected: " + e.toString());
 			}
 
 			return true;
 		}
 
 		return false;
 	}
 
 	private void savePrefs() {
 		if (mapView == null) return;
 		if (prefsEdit == null) prefsEdit = getSharedPreferences(cgSettings.preferences, 0).edit();
 
 		if (mapView.isSatellite()) prefsEdit.putInt("maptype", settings.mapSatellite);
 		else prefsEdit.putInt("maptype", settings.mapClassic);
 		if (loadingThread != null) {
 			if (loadingThread.enabled == true) prefsEdit.putInt("maplive", 1);
 			else prefsEdit.putInt("maplive", 0);
 		}
 		prefsEdit.putInt("mapzoom", mapView.getZoomLevel());
 		prefsEdit.commit();
 	}
 
 	private void addOverlays(boolean canChangeTitle) {
 		// users
 		if (mapView == null) return;
 
 		// init images
 		if (gcIconsClear.isEmpty()) {
 			gcIconsClear.put("ape", R.drawable.type_ape);
 			gcIconsClear.put("cito", R.drawable.type_cito);
 			gcIconsClear.put("earth", R.drawable.type_earth);
 			gcIconsClear.put("event", R.drawable.type_event);
 			gcIconsClear.put("letterbox", R.drawable.type_letterbox);
 			gcIconsClear.put("locationless", R.drawable.type_locationless);
 			gcIconsClear.put("mega", R.drawable.type_mega);
 			gcIconsClear.put("multi", R.drawable.type_multi);
 			gcIconsClear.put("traditional", R.drawable.type_traditional);
 			gcIconsClear.put("virtual", R.drawable.type_virtual);
 			gcIconsClear.put("webcam", R.drawable.type_webcam);
 			gcIconsClear.put("wherigo", R.drawable.type_wherigo);
 			gcIconsClear.put("mystery", R.drawable.type_mystery);
 		}
 
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
 
 		if (settings.publicLoc == 1 && users != null && users.isEmpty() == false) {
 			if (overlayUsers == null) {
 				overlayUsers = new cgUsersOverlay(app, (Context)this, base, getResources().getDrawable(R.drawable.user_location));
 			} else {
 				overlayUsers.disableTap();
 				overlayUsers.clearItems();
 			}
 			for (cgUser user : users) {
 				if (user.latitude == null && user.longitude == null) continue;
 
 				final cgOverlayUser item = new cgOverlayUser(activity, user);
 
 				pin = getResources().getDrawable(R.drawable.user_location);
 				pin.setBounds(0, 0, pin.getIntrinsicWidth(), pin.getIntrinsicHeight());
 				item.setMarker(pin);
 
 				overlayUsers.addItem(item);
 			}
 
 			if (mapView.getOverlays().contains(overlayUsers) == false) {
 				mapView.getOverlays().add(overlayUsers);
 			}
 
 			mapView.invalidate();
 			overlayUsers.enableTap();
 		}
 
 		searchingUsers = false;
 		
 		// geocaches
 		if (overlay == null) {
 			overlay = new cgMapOverlay(app, (Context)this, base, getResources().getDrawable(R.drawable.marker), fromDetail);
 		} else {
 			overlay.disableTap();
 			overlay.clearItems();
 		}
 
 		Integer maxLat = Integer.MIN_VALUE;
 		Integer minLat = Integer.MAX_VALUE;
 		Integer maxLon = Integer.MIN_VALUE;
 		Integer minLon = Integer.MAX_VALUE;
 
 		GeoPoint geopoint = null;
         int cachesWithCoords = 0;
         
 		coordinates.clear();
 		if (caches != null && caches.size() > 0) {
 			for (cgCache cache : caches) {
 				if (cache.latitude == null && cache.longitude == null) continue;
                 else cachesWithCoords ++;
 
 				String type = null;
 
 				if (cache.found == true) {
 					type = cache.type + "-found";
 				} else if (cache.disabled == true) {
 					type = cache.type + "-disabled";
 				} else {
 					type = cache.type;
 				}
 
 				if (type != null && gcIcons.containsKey(type) == true) {
 					pin = getResources().getDrawable(gcIcons.get(type));
 				} else {
 					pin = getResources().getDrawable(gcIcons.get("mystery"));
 				}
 
 				final cgCoord coord = new cgCoord(cache);
 
 				coordinates.add(coord);
 				final cgOverlayItem item = new cgOverlayItem(coord);
 
 				pin.setBounds(0, 0, pin.getIntrinsicWidth(), pin.getIntrinsicHeight());
 				item.setMarker(pin);
 
 				overlay.addItem(item);
 
 				final int latitudeE6 = (int)(cache.latitude * 1e6);
 				final int longitudeE6 = (int)(cache.longitude * 1e6);
 
 				if (latitudeE6 > maxLat) maxLat = latitudeE6;
 				if (latitudeE6 < minLat) minLat = latitudeE6;
 				if (longitudeE6 > maxLon) maxLon = longitudeE6;
 				if (longitudeE6 < minLon) minLon = longitudeE6;
 			}
 
             if (cachesWithCoords == 0) {
                 warning.showToast("There is no cache with coordinates.");
                 myLocationInMiddleForce();
             }
 
 			if (live == false) {
 				// there is only one cache
 				if (caches != null && caches.size() == 1 && cachesWithCoords > 0) {
 					cgCache oneCache = caches.get(0);
 
 					maxLat = (int)(oneCache.latitude * 1e6);
 					minLat = (int)(oneCache.latitude * 1e6);
 					maxLon = (int)(oneCache.longitude * 1e6);
 					minLon = (int)(oneCache.longitude * 1e6);
 
 					// waypoints
 					if (oneCache != null && oneCache.waypoints != null && oneCache.waypoints.size() > 0) {
 						for (cgWaypoint waypoint : oneCache.waypoints) {
 							if (waypoint.latitude == null && waypoint.longitude == null) continue;
 
 							if (waypoint.type != null && wpIcons.containsKey(waypoint.type) == true) {
 								pin = getResources().getDrawable(wpIcons.get(waypoint.type));
 							} else {
 								pin = getResources().getDrawable(wpIcons.get("waypoint"));
 							}
 
 							cgCoord coord = new cgCoord(waypoint);
 
 							coordinates.add(coord);
 							cgOverlayItem item = new cgOverlayItem(coord);
 
 							pin.setBounds(0, 0, pin.getIntrinsicWidth(), pin.getIntrinsicHeight());
 							item.setMarker(pin);
 
 							overlay.addItem(item);
 
 							int latitudeE6 = (int)(waypoint.latitude * 1e6);
 							int longitudeE6 = (int)(waypoint.longitude * 1e6);
 
 							if (latitudeE6 > maxLat) maxLat = latitudeE6;
 							if (latitudeE6 < minLat) minLat = latitudeE6;
 							if (longitudeE6 > maxLon) maxLon = longitudeE6;
 							if (longitudeE6 < minLon) minLon = longitudeE6;
 
 							coord = null;
 						}
 					}
 
 					int centerLat = 0;
 					int centerLon = 0;
 					if (coordinates.size() > 1) {
 						if ((Math.abs(maxLat) - Math.abs(minLat)) != 0) centerLat = minLat + ((maxLat - minLat) / 2);
 						if ((Math.abs(maxLon) - Math.abs(minLon)) != 0) centerLon = minLon + ((maxLon - minLon) / 2);
 					} else {
 						centerLat = (int)(oneCache.latitude * 1e6);
 						centerLon = (int)(oneCache.longitude * 1e6);
 					}
 
 					if (initLocation == true) {
 						mapController.animateTo(new GeoPoint(centerLat, centerLon));
 						if (Math.abs(maxLat - minLat) != 0 && Math.abs(maxLon - minLon) != 0) mapController.zoomToSpan(Math.abs(maxLat - minLat), Math.abs(maxLon - minLon));
 						initLocation = false;
 					}
 				} else {
 					int centerLat = 0;
 					int centerLon = 0;
 					if ((Math.abs(maxLat) - Math.abs(minLat)) != 0) centerLat = minLat + ((maxLat - minLat) / 2);
 					if ((Math.abs(maxLon) - Math.abs(minLon)) != 0) centerLon = minLon + ((maxLon - minLon) / 2);
 
 					if (initLocation == true && cachesWithCoords > 0) {
 						mapController.animateTo(new GeoPoint(centerLat, centerLon));
 						if (Math.abs(maxLat - minLat) != 0 && Math.abs(maxLon - minLon) != 0) mapController.zoomToSpan(Math.abs(maxLat - minLat), Math.abs(maxLon - minLon));
 						initLocation = false;
 					}
 				}
 			}
 		} else if (oneLatitude != null && oneLongitude != null) {
 			pin = getResources().getDrawable(wpIcons.get("waypoint"));
 
 			cgCoord coord = new cgCoord();
 			coord.type = "waypoint";
 			coord.latitude = oneLatitude;
 			coord.longitude = oneLongitude;
 			coord.name = "some plase";
 
 			coordinates.add(coord);
 			cgOverlayItem item = new cgOverlayItem(coord);
 
 			pin.setBounds(0, 0, pin.getIntrinsicWidth(), pin.getIntrinsicHeight());
 			item.setMarker(pin);
 
 			overlay.addItem(item);
 
 			geopoint = new GeoPoint((int)(oneLatitude * 1e6), (int)(oneLongitude * 1e6));
 
 			if (initLocation == true) {
 				mapController.animateTo(geopoint);
 				initLocation = false;
 			}
 		}
 
 		if (mapView.getOverlays().contains(overlay) == false) {
 			mapView.getOverlays().add(overlay);
 		}
 
 		mapView.invalidate();
 		overlay.enableTap();
 
 		searching = false;
 		if (canChangeTitle == true) changeTitle(false);
 	}
 
 	private void myLocation() {
 		if (mapView == null) return;
 		if (geo == null || geo.latitudeNow == null || geo.longitudeNow == null) return;
 	}
 
 	private void myLocationInMiddle() {
 		if (followLocation == false && initLocation == false) return;
 		if (geo == null) return;
 
 		centerMap(geo.latitudeNow, geo.longitudeNow);
 
 		if (initLocation == true) initLocation = false;
 	}
 
 	private void myLocationInMiddleForce() {
 		if (geo == null) return;
 
 		centerMap(geo.latitudeNow, geo.longitudeNow);
 	}
 
 	private void centerMap(Double latitude, Double longitude) {
 		if (latitude == null || longitude == null) return;
 		if (mapView == null) return;
 
 		mapController.animateTo(new GeoPoint((int)(latitude * 1e6), (int)(longitude * 1e6)));
 	}
 
 	private class update extends cgUpdateLoc {
 		@Override
 		public void updateLoc(cgGeo geo) {
 			if (geo == null) return;
 
 			try {
 				if (overlayMyLoc == null && mapView != null) {
 					overlayMyLoc = new cgMapMyOverlay(settings);
 					mapView.getOverlays().add(overlayMyLoc);
 				}
 
 				if (overlayMyLoc != null && geo.location != null) overlayMyLoc.setCoordinates(geo.location);
 
 				(new findClose()).start();
 
 				if (geo.latitudeNow != null && geo.longitudeNow != null) {
 					myLocation();
 					if (followLocation == true || initLocation == true) myLocationInMiddle();
 				}
 
 				if (settings.useCompass == 0) {
 					if (geo.bearingNow != null) overlayMyLoc.setHeading(geo.bearingNow);
 					else overlayMyLoc.setHeading(0.0f);
 				}
 			} catch (Exception e) {
 				Log.w(cgSettings.tag, "Failed to update location.");
 			}
 		}
 	}
 
 	private class updateDir extends cgUpdateDir {
 		@Override
 		public void updateDir(cgDirection dir) {
 			if (dir == null) return;
 
 			if (overlayMyLoc != null && mapView != null) {
 				overlayMyLoc.setHeading(dir.directionNow);
 				mapView.invalidate();
 			}
 		}
 	}
 
 	private class loadCacheFromDb extends Thread {
 		private Handler handler = null;
 
 		private loadCacheFromDb(Handler handlerIn) {
 			handler = handlerIn;
 		}
 
 		@Override
 		public void run() {
 			startLoading.sendEmptyMessage(0);
 
 			if (searchId != null) {
				caches = app.getCaches(searchId, false, false, false, false, false);
 			}
 			
 			if (geocode != null && geocode.length() > 0) {
 				caches = new ArrayList<cgCache>();
				caches.add(app.getCacheByGeocode(geocode, false, false, false, false, false));
 			}
 			handler.sendMessage(new Message());
 		}
 	}
 
 	// thread used just like timer
 	private class loadCaches extends Thread {
 		private boolean requestedKill = false;
 		private boolean enabled = true;
 		private Handler handler = null;
 		private String viewstate = null;
 		private MapView mapView = null;
 
 		private loadCaches(Handler handlerIn, MapView mapViewIn) {
 			handler = handlerIn;
 			mapView = mapViewIn;
 		}
 
 		protected void kill() {
 			requestedKill = true;
 		}
 
 		protected void enable() {
 			enabled = true;
 		}
 
 		protected void disable() {
 			enabled = false;
 		}
 
 		public boolean state() {
 			return enabled;
 		}
 
 		protected void setViewstate(String viewstateIn) {
 			viewstate = viewstateIn;
 		}
 
 		@Override
 		public void run() {
 			while (requestedKill == false) {
 				try {
 					if (firstRun == true) sleep(2000);
 					else sleep(1000);
 
 					firstRun = false;
 
 					if (enabled == true && mapView != null && searching == false) {
 						loadCachesReal realThread = new loadCachesReal(handler, mapView, viewstate);
 						realThread.start();
 					}
 				} catch (Exception e) {
 					Log.e(cgSettings.tag, "cgeomap.loadCaches: " + e.toString());
 				}
 			}
 		}
 	}
 
 	// thread that is downloading caches
 	private class loadCachesReal extends Thread {
 		private Handler handler = null;
 		private String viewstate = null;
 		private MapView mapView = null;
 		private Double latitudeT = null;
 		private Double latitudeB = null;
 		private Double longitudeL = null;
 		private Double longitudeR = null;
 
 		private loadCachesReal(Handler handlerIn, MapView mapViewIn, String viewstateIn) {
 			handler = handlerIn;
 			viewstate = viewstateIn;
 			mapView = mapViewIn;
 		}
 
 		@Override
 		public void run() {
 			GeoPoint center = mapView.getMapCenter();
 			int latitudeCenter = center.getLatitudeE6();
 			int longitudeCenter = center.getLongitudeE6();
 			int latitudeSpan = mapView.getLatitudeSpan();
 			int longitudeSpan = mapView.getLongitudeSpan();
 
 			if (
 					(centerLatitude == null || centerLongitude == null || spanLatitude == null || spanLongitude == null) || // first run
 					((
 						(Math.abs(latitudeSpan - spanLatitude) > 50) || // changed zoom
 						(Math.abs(longitudeSpan - spanLongitude) > 50) || // changed zoom
 						(Math.abs(latitudeCenter - centerLatitude) > (latitudeSpan / 6)) || // map moved
 						(Math.abs(longitudeCenter - centerLongitude) > (longitudeSpan / 6)) // map moved
 					) && (
 						base.isInViewPort(centerLatitude, centerLongitude, latitudeCenter, longitudeCenter, spanLatitude, spanLongitude, latitudeSpan, longitudeSpan) == false ||
 						caches.isEmpty() == true
 					))
 				) {
 
 				latitudeT = (latitudeCenter + (latitudeSpan / 2)) / 1e6;
 				latitudeB = (latitudeCenter - (latitudeSpan / 2)) / 1e6;
 				longitudeL = (longitudeCenter + (longitudeSpan / 2)) / 1e6;
 				longitudeR = (longitudeCenter - (longitudeSpan / 2)) / 1e6;
 
 				centerLatitude = latitudeCenter;
 				centerLongitude = longitudeCenter;
 				spanLatitude = latitudeSpan;
 				spanLongitude = longitudeSpan;
 
 				if (searching == false) {
 					searching = true;
 					startLoading.sendEmptyMessage(0);
 
 					HashMap<String, String> params = new HashMap<String, String>();
 					params.put("viewstate", viewstate);
 					params.put("latitude-t", String.format((Locale)null, "%.6f", latitudeT));
 					params.put("latitude-b", String.format((Locale)null, "%.6f", latitudeB));
 					params.put("longitude-l", String.format((Locale)null, "%.6f", longitudeL));
 					params.put("longitude-r", String.format((Locale)null, "%.6f", longitudeR));
 
 					Log.i(cgSettings.tag, "Starting download caches for: " + String.format((Locale)null, "%.6f", latitudeT) + "," + String.format((Locale)null, "%.6f", longitudeL) + " | " + String.format((Locale)null, "%.6f", latitudeB) + "," + String.format((Locale)null, "%.6f", longitudeR));
 
 					searchId = base.searchByViewport(params, 0);
 
 					if (searchId != null && searchId > 0) {
 						if (loadingThread != null && app.getViewstate(searchId) != null) {
 							loadingThread.setViewstate(app.getViewstate(searchId));
 						}
 
 						caches.clear();
 						if (app.getCount(searchId) > 0) caches.addAll(app.getCaches(searchId, false, false, false, false, false));
 					}
 
 					handler.sendEmptyMessage(0);
 				}
 			}
 		}
 	}
 
 	private class loadUsers extends Thread {
 		private boolean requestedKill = false;
 		private boolean enabled = true;
 		private Handler handler = null;
 		private MapView mapView = null;
 		private Double latitudeT = null;
 		private Double latitudeB = null;
 		private Double longitudeL = null;
 		private Double longitudeR = null;
 
 		protected void kill() {
 			requestedKill = true;
 		}
 
 		protected void enable() {
 			enabled = true;
 		}
 
 		protected void disable() {
 			enabled = false;
 		}
 
 		public boolean state() {
 			return enabled;
 		}
 
 		private loadUsers(Handler handlerIn, MapView mapViewIn) {
 			setPriority(Thread.MIN_PRIORITY);
 			
 			handler = handlerIn;
 			mapView = mapViewIn;
 		}
 
 		@Override
 		public void run() {
 			while (requestedKill == false) {
 				try {
 					sleep(500);
 				} catch (Exception e) {
 					// nothing
 				}
 
 				if (enabled == true && mapView != null) {
 					GeoPoint center = mapView.getMapCenter();
 					int latitudeCenter = center.getLatitudeE6();
 					int longitudeCenter = center.getLongitudeE6();
 					int latitudeSpan = mapView.getLatitudeSpan();
 					int longitudeSpan = mapView.getLongitudeSpan();
 
 					if (
 							(centerLatitudeUsers == null || centerLongitudeUsers == null || spanLatitudeUsers == null || spanLongitudeUsers == null) || // first run
 							((
 								(Math.abs(latitudeSpan - spanLatitudeUsers) > 50) || // changed zoom
 								(Math.abs(longitudeSpan - spanLongitudeUsers) > 50) || // changed zoom
 								(Math.abs(latitudeCenter - centerLatitudeUsers) > (latitudeSpan / 6)) || // map moved
 								(Math.abs(longitudeCenter - centerLongitudeUsers) > (longitudeSpan / 6)) // map moved
 							) && (
 								base.isInViewPort(centerLatitudeUsers, centerLongitudeUsers, latitudeCenter, longitudeCenter, spanLatitudeUsers, spanLongitudeUsers, latitudeSpan, longitudeSpan) == false ||
 								users == null || users.isEmpty() == true
 							))
 						) {
 
 						latitudeT = (latitudeCenter + (latitudeSpan / 2)) / 1e6;
 						latitudeB = (latitudeCenter - (latitudeSpan / 2)) / 1e6;
 						longitudeL = (longitudeCenter + (longitudeSpan / 2)) / 1e6;
 						longitudeR = (longitudeCenter - (longitudeSpan / 2)) / 1e6;
 
 						centerLatitudeUsers = latitudeCenter;
 						centerLongitudeUsers = longitudeCenter;
 						spanLatitudeUsers = latitudeSpan;
 						spanLongitudeUsers = longitudeSpan;
 
 						if (searchingUsers == false) {
 							Log.i(cgSettings.tag, "Starting download other users for: " + String.format((Locale)null, "%.6f", latitudeT) + "," + String.format((Locale)null, "%.6f", longitudeL) + " | " + String.format((Locale)null, "%.6f", latitudeB) + "," + String.format((Locale)null, "%.6f", longitudeR));
 
 							searchingUsers = true;
 							users = base.usersInViewport(settings.getUsername(), latitudeB, latitudeT, longitudeR, longitudeL);
 						}
 
 						handler.sendEmptyMessage(0);
 					}
 				}
 			}
 		}
 	}
 
 	private class findClose extends Thread {
 		public findClose() {
 			setPriority(Thread.MIN_PRIORITY);
 		}
 
 		@Override
 		public void run() {
 			if (searchingForClose == true) return;
 
 			searchingForClose = true;
 			try {
 				double distance = 0d;
 				double closestDistance = Double.POSITIVE_INFINITY;
 				int closestCache = -1;
 
 				if (geo != null && caches != null && caches.isEmpty() == false) {
 					for (cgCache oneCache : caches) {
 						distance = base.getDistance(geo.latitudeNow, geo.longitudeNow, oneCache.latitude, oneCache.longitude);
 						if (live == true && geo != null && distance < closestDistance) {
 							closestDistance = distance;
 							closestCache = caches.indexOf(oneCache);
 						}
 					}
 				}
 
 				setCloseHandler.sendEmptyMessage(closestCache);
 			} catch (Exception e) {
 				Log.e(cgSettings.tag, "cgeocaches.findClose.run: " + e.toString());
 			}
 		}
 	}
 
 	private class closeClickListener implements View.OnClickListener {
 		private cgCache cache = null;
 
 		public closeClickListener(cgCache cacheIn) {
 			cache = cacheIn;
 		}
 
 		public void onClick(View arg0) {
 			if (cache == null) return;
 
 			Intent cacheIntent = new Intent(activity, cgeodetail.class);
 			cacheIntent.putExtra("geocode", cache.geocode.toUpperCase());
 			activity.startActivity(cacheIntent);
 		}
 	}
 
 	public void dismissClose() {
 		if (close == null) close = (LinearLayout)findViewById(R.id.close);
 		close.setVisibility(View.GONE);
 	}
 
 	private class geocachesLoadDetails extends Thread {
 		private Handler handler = null;
 		private ArrayList<String> geocodes = null;
         private volatile Boolean needToStop = false;
 
 		public geocachesLoadDetails(Handler handlerIn, ArrayList<String> geocodesIn) {
 			handler = handlerIn;
 			geocodes = geocodesIn;
 		}
 
         public void kill() {
             this.needToStop = true;
         }
 
 		@Override
 		public void run() {
 			if (geocodes == null || geocodes.isEmpty()) return;
             if (dir != null) dir = app.removeDir(dir);
             if (geo != null) geo = app.removeGeo(geo);
 
 			Message msg = null;
 
             for (String geocode : geocodes) {
                 try {
                     if (needToStop == true) {
                         Log.i(cgSettings.tag, "Stopped storing process.");
                         break;
                     }
 
 					try {
 						sleep(3000 + ((Double)(Math.random() * 3000)).intValue());
 					} catch (Exception e) {
 						Log.e(cgSettings.tag, "cgeomap.geocachesLoadDetails.sleep: " + e.toString());
 					}
 
                     if (needToStop == true) {
                         Log.i(cgSettings.tag, "Stopped storing process.");
                         break;
                     }
 
                     detailProgress ++;
 					base.storeCache(app, activity, null, geocode, handler);
 
                     msg = new Message();
                     msg.what = 0;
                     handler.sendMessage(msg);
                 } catch (Exception e) {
 					Log.e(cgSettings.tag, "cgeocaches.geocachesLoadDetails: " + e.toString());
                 }
 				
                 yield();
             }
 
             msg = new Message();
             msg.what = 1;
 			handler.sendMessage(msg);
 		}
 	}
 
 	protected void changeTitle(boolean loading) {
 		String title = null;
 		if (live == true) title = "live map";
 		else title = "map";
 
 		if (loading == true) {
 			if (progressBar == true) setProgressBarIndeterminateVisibility(true);
 			setTitle(title);
 		} else if (caches != null) {
 			if (progressBar == true) setProgressBarIndeterminateVisibility(false);
 			setTitle(title + " (" + caches.size() + ")");
 		} else {
 			if (progressBar == true) setProgressBarIndeterminateVisibility(false);
 			setTitle(title + " (no caches)");
 		}
 	}
 }
