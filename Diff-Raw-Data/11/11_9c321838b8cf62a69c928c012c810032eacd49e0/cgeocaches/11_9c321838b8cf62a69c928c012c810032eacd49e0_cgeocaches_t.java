 package carnero.cgeo;
 
 import java.util.ArrayList;
 import java.util.HashMap;
 import android.os.Handler;
 import android.os.Message;
 import android.os.Bundle;
 import android.util.Log;
 import android.app.Activity;
 import android.app.AlertDialog;
 import android.app.ListActivity;
 import android.app.ProgressDialog;
 import android.content.DialogInterface;
 import android.view.Menu;
 import android.view.MenuItem;
 import android.content.Intent;
 import android.content.res.Configuration;
 import android.content.res.Resources;
 import android.net.Uri;
 import android.view.ContextMenu;
 import android.view.KeyEvent;
 import android.view.LayoutInflater;
 import android.view.View;
 import android.widget.AdapterView.AdapterContextMenuInfo;
 import android.widget.ListView;
 import android.widget.TextView;
 import com.google.android.apps.analytics.GoogleAnalyticsTracker;
 import java.util.Locale;
 
 public class cgeocaches extends ListActivity {
 	private GoogleAnalyticsTracker tracker = null;
 	private String action = null;
 	private String type = null;
 	private Double latitude = null;
 	private Double longitude = null;
 	private String cachetype = null;
 	private String keyword = null;
 	private String address = null;
 	private String username = null;
 	private Long searchId = null;
 	private ArrayList<cgCache> cacheList = new ArrayList<cgCache>();
 	private cgeoapplication app = null;
 	private Resources res = null;
 	private static Activity activity = null;
 	private cgCacheListAdapter adapter = null;
 	private LayoutInflater inflater = null;
 	private View listFooter = null;
 	private TextView listFooterText = null;
 	private cgSettings settings = null;
 	private cgBase base = null;
 	private cgWarning warning = null;
 	private ProgressDialog waitDialog = null;
 	private Float northHeading = 0.0f;
 	private cgGeo geo = null;
 	private cgDirection dir = null;
 	private cgUpdateLoc geoUpdate = new update();
 	private cgUpdateDir dirUpdate = new updateDir();
 	private String title = "";
 	private int detailTotal = 0;
 	private int detailProgress = 0;
 	private Long detailProgressTime = 0l;
 	private geocachesLoadDetails threadD = null;
 	private geocachesDropDetails threadR = null;
 	private boolean offline = false;
 
 	private Handler loadCachesHandler = new Handler() {
 		@Override
 		public void handleMessage(Message msg) {
 			try {
 				if (searchId != null && searchId > 0 && app.getCount(searchId) > 0) {
 					cacheList.clear();
 					cacheList.addAll(app.getCaches(searchId));
 				}
 
 				setAdapter();
 
 				if (cacheList == null) {
 					warning.showToast("Sorry, c:geo failed to load cache list.");
 					setMoreCaches(false);
 				} else {
 					final Integer count = app.getTotal(searchId);
 					final int size = cacheList.size();
 
 					if (count != null && count > 0) {
 						base.setTitle(activity, title + " (" + size + "/" + count + ")");
 						if (cacheList.size() < app.getTotal(searchId) && cacheList.size() < 1000) setMoreCaches(true);
 						else setMoreCaches(false);
 					} else {
 						base.setTitle(activity, title);
 						setMoreCaches(false);
 					}
 				}
 
 				if (cacheList != null && app.getError(searchId) != null && app.getError(searchId).equalsIgnoreCase(base.errorRetrieve.get(-7)) == true) {
 					AlertDialog.Builder dialog = new AlertDialog.Builder(activity);
 					dialog.setTitle("license");
 					dialog.setMessage("You have not agreed with Geocaching.com license agreement, so c:geo can't load caches coordinates.");
 					dialog.setCancelable(true);
 					dialog.setNegativeButton("Dismiss", new DialogInterface.OnClickListener() {
 					   public void onClick(DialogInterface dialog, int id) {
 						    settings.deleteCookies();
 							dialog.cancel();
 					   }
 					});
 					dialog.setPositiveButton("Show License", new DialogInterface.OnClickListener() {
 					   public void onClick(DialogInterface dialog, int id) {
 						    settings.deleteCookies();
 							activity.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("http://www.geocaching.com/software/agreement.aspx?ID=0")));
 					   }
 					});
 
 				   AlertDialog alert = dialog.create();
 				   alert.show();
 				} else if (app != null && app.getError(searchId) != null && app.getError(searchId).length() > 0) {
 					warning.showToast("Sorry, c:geo failed to download caches because of " + app.getError(searchId) + ".");
 
 					base.showProgress(activity, false);
 					if (waitDialog != null) {
 						waitDialog.dismiss();
 						waitDialog.setOnCancelListener(null);
 					}
 					
 					finish();
 					return;
 				}
 
 				if (geo != null && geo.latitudeNow != null && geo.longitudeNow != null) {
 					adapter.setActualCoordinates(geo.latitudeNow, geo.longitudeNow);
 					adapter.setActualHeading(northHeading);
 				}
 			} catch (Exception e) {
 				warning.showToast("Sorry, c:geo can\'t find any geocache.");
 				Log.e(cgSettings.tag, "cgeocaches.loadCachesHandler: " + e.toString());
 
 				base.showProgress(activity, false);
 				if (waitDialog != null) {
 					waitDialog.dismiss();
 					waitDialog.setOnCancelListener(null);
 				}
 				finish();
 				return;
 			}
 
 			try {
 				base.showProgress(activity, false);
 				if (waitDialog != null) {
 					waitDialog.dismiss();
 					waitDialog.setOnCancelListener(null);
 				}
 			} catch (Exception e2) {
 				Log.e(cgSettings.tag, "cgeocaches.loadCachesHandler.2: " + e2.toString());
 			}
 		}
 	};
 
 	private Handler loadNextPageHandler = new Handler() {
 		@Override
 		public void handleMessage(Message msg) {
 			try {
 				if (searchId != null && searchId > 0 && app.getCount(searchId) > 0) {
 					cacheList.clear();
 					cacheList.addAll(app.getCaches(searchId));
 				}
 				
 				setAdapter();
 
 				if (cacheList == null) {
 					warning.showToast("Sorry, c:geo failed to load cache list.");
 					setMoreCaches(false);
 				} else {
 					final Integer count = app.getTotal(searchId);
 					final int size = cacheList.size();
 					if (count != null && count > 0) {
 						base.setTitle(activity, title + " (" + size + "/" + count + ")");
 						if (cacheList.size() < app.getTotal(searchId) && cacheList.size() < 1000) {
 							setMoreCaches(true);
 						} else {
 							setMoreCaches(false);
 						}
 					} else {
 						base.setTitle(activity, title);
 						setMoreCaches(false);
 					}
 				}
 
 				if (app.getError(searchId) != null && app.getError(searchId).length() > 0) {
 					warning.showToast("Sorry, c:geo failed to download caches because of " + app.getError(searchId) + ".");
 
 					listFooter.setOnClickListener(new moreCachesListener());
 					base.showProgress(activity, false);
 					if (waitDialog != null) {
 						waitDialog.dismiss();
 						waitDialog.setOnCancelListener(null);
 					}
 					
 					finish();
 					return;
 				}
 
 				if (geo != null && geo.latitudeNow != null && geo.longitudeNow != null) {
 					adapter.setActualCoordinates(geo.latitudeNow, geo.longitudeNow);
 					adapter.setActualHeading(northHeading);
 				}
 			} catch (Exception e) {
 				warning.showToast("c:geo can\'t find next geocaches.");
 				Log.e(cgSettings.tag, "cgeocaches.loadNextPageHandler: " + e.toString());
 			}
 
 			listFooter.setOnClickListener(new moreCachesListener());
 			base.showProgress(activity, false);
 			if (waitDialog != null) {
 				waitDialog.dismiss();
 				waitDialog.setOnCancelListener(null);
 			}
 		}
 	};
 
 	private Handler loadDetailsHandler = new Handler() {
 		@Override
 		public void handleMessage(Message msg) {
 			setAdapter();
 
 			if (msg.what > -1) {
 				if (waitDialog != null) {
 					cacheList.get(msg.what).statusChecked = false;
 
 					if (adapter != null) {
 						adapter.notifyDataSetChanged();
 					}
 					
 					Float diffTime = new Float((System.currentTimeMillis() - detailProgressTime) / 1000); // seconds left
 					Float oneCache = diffTime / detailProgress; // left time per cache
 					int etaTime = (int)((detailTotal - detailProgress) * oneCache / 60); // seconds remaining
 
 					waitDialog.setProgress(detailProgress);
 					if (etaTime < 1) {
 						waitDialog.setMessage("downloading caches...\neta: less than minute");
 					} else if (etaTime == 1) {
 						waitDialog.setMessage("downloading caches...\neta: " + etaTime + " min");
 					} else {
 						waitDialog.setMessage("downloading caches...\neta: " + etaTime + " mins");
 					}
 				}
 			} else {
 				if (cacheList != null && searchId != null) {
 					cacheList.clear();
 					cacheList.addAll(app.getCaches(searchId));
 				}
 
 				if (geo != null && geo.latitudeNow != null && geo.longitudeNow != null) {
 					adapter.setActualCoordinates(geo.latitudeNow, geo.longitudeNow);
 					adapter.setActualHeading(northHeading);
 				}
 
 				base.showProgress(activity, false);
 				if (waitDialog != null) {
 					waitDialog.dismiss();
 					waitDialog.setOnCancelListener(null);
 				}
 
 				if (geo == null) geo = app.startGeo(activity, geoUpdate, base, settings, warning, 0, 0);
 				if (settings.livelist == 1 && settings.useCompass == 1 && dir == null) dir = app.startDir(activity, dirUpdate, warning);
 			}
 		}
 	};
 
 	private Handler dropDetailsHandler = new Handler() {
 		@Override
 		public void handleMessage(Message msg) {
 			setAdapter();
 
 			if (msg.what > -1) {
 				cacheList.get(msg.what).statusChecked = false;
 
 				if (adapter != null) {
 					adapter.notifyDataSetChanged();
 				}
 			} else {
 				if (waitDialog != null) {
 					waitDialog.dismiss();
 					waitDialog.setOnCancelListener(null);
 				}
 			}
 		}
 	};
 
     @Override
 	public void onCreate(Bundle savedInstanceState) {
 		super.onCreate(savedInstanceState);
 
 		// init
 		activity = this;
 		res = this.getResources();
 		app = (cgeoapplication)this.getApplication();
 		app.setAction(action);
 		settings = new cgSettings(this, getSharedPreferences(cgSettings.preferences, 0));
 		base = new cgBase(app, settings, getSharedPreferences(cgSettings.preferences, 0));
 		warning = new cgWarning(this);
 
 		// set layout
 		if (settings.skin == 1) {
 			setTheme(R.style.light);
 		} else {
 			setTheme(R.style.dark);
 		}
 		setContentView(R.layout.caches);
 		base.setTitle(activity, "caches");
 
 		// google analytics
 		tracker = GoogleAnalyticsTracker.getInstance();
 		tracker.start(cgSettings.analytics, this);
 		tracker.dispatch();
 		base.sendAnal(activity, tracker, "/cache/list");
 
 		// get parameters
 		Bundle extras = getIntent().getExtras();
 		if (extras !=null) {
 			type = extras.getString("type");
 			latitude = extras.getDouble("latitude");
 			longitude = extras.getDouble("longitude");
 			cachetype = extras.getString("cachetype");
 			keyword = extras.getString("keyword");
 			address = extras.getString("address");
 			username = extras.getString("username");
 		}
 
 		init();
 
 		String typeText;
 		if (cachetype != null && base.cacheTypesInv.containsKey(cachetype) == true) typeText = "\ntype: " + base.cacheTypesInv.get(cachetype);
 		else typeText = "\ntype: all cache types";
 
 		Thread thread;
 		if (type.equals("nearest")) {
 			action = "pending";
 			title = "nearby";
 			base.setTitle(activity, title);
 			base.showProgress(activity, true);
 			waitDialog = ProgressDialog.show(this, "searching for caches", "caches nearby" + typeText, true);
 			waitDialog.setCancelable(true);
 			thread = new geocachesLoadByCoords(loadCachesHandler,  latitude, longitude, cachetype);
 			thread.start();
 		} else if (type.equals("offline")) {
 			title = "stored";
 			offline = true;
 			base.setTitle(activity, title);
 			base.showProgress(activity, true);
 			waitDialog = ProgressDialog.show(this, "loading caches", "caches stored in device", true);
 			waitDialog.setCancelable(true);
 			thread = new geocachesLoadByOffline(loadCachesHandler, latitude, longitude);
 			thread.start();
 		} else if (type.equals("coordinate")) {
 			action = "planning";
 			title = base.formatCoordinate(latitude, "lat", true) + " | " + base.formatCoordinate(longitude, "lon", true);
 			base.setTitle(activity, title);
 			base.showProgress(activity, true);
 			waitDialog = ProgressDialog.show(this, "searching for caches", "caches near\n" + base.formatCoordinate(latitude, "lat", true) + " | " + base.formatCoordinate(longitude, "lon", true) + typeText, true);
 			waitDialog.setCancelable(true);
 			thread = new geocachesLoadByCoords(loadCachesHandler,  latitude, longitude, cachetype);
 			thread.start();
 		} else if (type.equals("keyword")) {
 			title = keyword;
 			base.setTitle(activity, title);
 			base.showProgress(activity, true);
 			waitDialog = ProgressDialog.show(this, "searching for caches", "caches by keyword " + keyword + typeText, true);
 			waitDialog.setCancelable(true);
 			thread = new geocachesLoadByKeyword(loadCachesHandler,  keyword, cachetype);
 			thread.start();
 		} else if (type.equals("address")) {
 			action = "planning";
 			if (address != null && address.length() > 0) {
 				title = address;
 				base.setTitle(activity, title);
 				base.showProgress(activity, true);
 				waitDialog = ProgressDialog.show(this, "searching for caches", "caches near\n" + address + typeText, true);
 			} else {
 				title = base.formatCoordinate(latitude, "lat", true) + " | " + base.formatCoordinate(longitude, "lon", true);
 				base.setTitle(activity, title);
 				base.showProgress(activity, true);
 				waitDialog = ProgressDialog.show(this, "searching for caches", "caches near\n" + base.formatCoordinate(latitude, "lat", true) + " | " + base.formatCoordinate(longitude, "lon", true) + typeText, true);
 			}
 			waitDialog.setCancelable(true);
 			thread = new geocachesLoadByCoords(loadCachesHandler,  latitude, longitude, cachetype);
 			thread.start();
 		} else if (type.equals("username")) {
 			title = username;
 			base.setTitle(activity, title);
 			base.showProgress(activity, true);
 			waitDialog = ProgressDialog.show(this, "searching for caches", "caches found by " + username + typeText, true);
 			waitDialog.setCancelable(true);
 			thread = new geocachesLoadByUserName(loadCachesHandler,  username, cachetype);
 			thread.start();
 		} else if (type.equals("owner")) {
 			title = username;
 			base.setTitle(activity, title);
 			base.showProgress(activity, true);
 			waitDialog = ProgressDialog.show(this, "searching for caches", "caches hidden by " + username + typeText, true);
 			waitDialog.setCancelable(true);
 			thread = new geocachesLoadByOwner(loadCachesHandler,  username, cachetype);
 			thread.start();
 		} else {
 			title = "caches";
 			base.setTitle(activity, title);
 			Log.e(cgSettings.tag, "cgeocaches.onCreate: No action or unknown action specified");
 		}
 	}
 
 	@Override
 	public void onConfigurationChanged(Configuration newConfig) {
 		super.onConfigurationChanged(newConfig);
 
 		init();
 	}
 
 	@Override
 	public void onResume() {
 		super.onResume();
 
 		init();
 
 		if (adapter != null && geo != null && geo.latitudeNow != null && geo.longitudeNow != null) {
 			adapter.setActualCoordinates(geo.latitudeNow, geo.longitudeNow);
 			adapter.setActualHeading(northHeading);
 		}
 
 		if (adapter != null) {
 			adapter.setSelectMode(false, true);
 			if (geo != null && geo.latitudeNow != null && geo.longitudeNow != null) {
 				adapter.forceSort(geo.latitudeNow, geo.longitudeNow);
 			}
 		}
 	}
 
 	@Override
 	public void onDestroy() {
 		if (adapter != null) adapter = null;
 
 		if (dir != null) dir = app.removeDir();
 		if (geo != null) geo = app.removeGeo();
 		if (tracker != null) tracker.stop();
 
 		super.onDestroy();
 	}
 
 	@Override
 	public void onStop() {
 		if (dir != null) dir = app.removeDir();
 		if (geo != null) geo = app.removeGeo();
 
 		super.onStop();
 	}
 
 	@Override
 	public void onPause() {
 		if (dir != null) dir = app.removeDir();
 		if (geo != null) geo = app.removeGeo();
 
 		super.onPause();
 	}
 	
 	@Override
 	public boolean onCreateOptionsMenu(Menu menu) {
 		menu.add(0, 0, 0, "select mode").setIcon(android.R.drawable.ic_menu_agenda);
 		if (type.equals("offline") == true) {
 			menu.add(0, 4, 0, "drop all").setIcon(android.R.drawable.ic_menu_delete); // delete saved caches
 			menu.add(0, 1, 0, "refresh listed").setIcon(android.R.drawable.ic_menu_set_as); // download details for all caches
 			menu.add(0, 5, 0, "import gpx").setIcon(android.R.drawable.ic_menu_upload); // import gpx file
 		} else {
 			menu.add(0, 1, 0, "store for offline").setIcon(android.R.drawable.ic_menu_set_as); // download details for all caches
 		}
 		menu.add(0, 2, 0, "show on map").setIcon(android.R.drawable.ic_menu_mapmode); // show all caches on map
 		return true;
 	}
 
 	@Override
 	public boolean onPrepareOptionsMenu(Menu menu) {
 		super.onPrepareOptionsMenu(menu);
 
 		try {
 			if (adapter != null && adapter.getSelectMode() == true) {
 				menu.findItem(0).setTitle("exit select mode");
 			} else {
 				menu.findItem(0).setTitle("select mode");
 			}
 
 			if (type.equals("offline") == true) {
 				if (adapter != null && adapter.getChecked() > 0) {
 					menu.findItem(4).setTitle("drop selected (" + adapter.getChecked() + ")");
 				} else {
 					menu.findItem(4).setTitle("drop all");
 				}
 				
 				if (adapter != null && adapter.getChecked() > 0) {
 					menu.findItem(1).setTitle("refresh selected (" + adapter.getChecked() + ")");
 				} else {
 					menu.findItem(1).setTitle("refresh listed");
 				}
 			} else {
 				if (adapter == null) {
 					Log.i(cgSettings.tag, "No adapter");
 				} else {
 					Log.i(cgSettings.tag, "Checked: " + adapter.getChecked());
 				}
 				if (adapter != null && adapter.getChecked() > 0) {
 					menu.findItem(1).setTitle("store selected (" + adapter.getChecked() + ")");
 				} else {
 					menu.findItem(1).setTitle("store for offline");
 				}
 			}
 
 			if (type.equals("offline") == false && (cacheList != null && app != null && cacheList.size() >= app.getTotal(searchId)) ) { // there are no more caches
 				menu.findItem(0).setEnabled(false);
 			}
 		} catch (Exception e) {
 			Log.e(cgSettings.tag, "cgeocaches.onPrepareOptionsMenu: " + e.toString());
 		}
 
 		return true;
 	}
 
 	@Override
 	public boolean onOptionsItemSelected(MenuItem item) {
 		switch (item.getItemId()) {
 			case 0:
 				if (adapter != null) {
 					adapter.switchSelectMode();
 				}
 				return true;
 			case 1:
 				refreshStored();
 				return true;
 			case 2:
 				showOnMap();
 				return false;
 			case 4:
 				dropStored();
 				return false;
 			case 5:
 				importGpx();
 				return false;
 		}
 
 		return false;
 	}
 
 	@Override
 	public void onCreateContextMenu(ContextMenu menu, View view, ContextMenu.ContextMenuInfo info) {
 		super.onCreateContextMenu(menu, view, info);
 
 		if (adapter == null) return;
 
 		AdapterContextMenuInfo adapterInfo = null;
 		try {
 			adapterInfo = (AdapterContextMenuInfo)info;
 		} catch (Exception e) {
 			Log.w(cgSettings.tag, "cgeocaches.onCreateContextMenu: " + e.toString());
 		}
 
 		final cgCache cache = adapter.getItem(adapterInfo.position);
 
 		if (cache.name != null && cache.name.length() > 0) {
 			menu.setHeaderTitle(cache.name);
 		} else {
 			menu.setHeaderTitle(cache.geocode);
 		}
 		if (cache.latitude != null && cache.longitude != null) {
 			menu.add(0, 1, 0, res.getString(R.string.cache_menu_compass));
 			menu.add(0, 2, 0, res.getString(R.string.cache_menu_radar));
 			menu.add(0, 3, 0, res.getString(R.string.cache_menu_map));
 			menu.add(0, 4, 0, res.getString(R.string.cache_menu_map_ext));
 			menu.add(0, 5, 0, res.getString(R.string.cache_menu_tbt));
 			menu.add(0, 6, 0, res.getString(R.string.cache_menu_visit));
 			menu.add(0, 7, 0, res.getString(R.string.cache_menu_details));
 		}
 	}
 
 	@Override
 	public boolean onContextItemSelected(MenuItem item) {
 		final int id = item.getItemId();
 		ContextMenu.ContextMenuInfo info = item.getMenuInfo();
 
 		if (info == null) {
 			return false;
 		}
 		
 		AdapterContextMenuInfo adapterInfo = null;
 		try {
 			adapterInfo = (AdapterContextMenuInfo)info;
 		} catch (Exception e) {
 			Log.w(cgSettings.tag, "cgeocaches.onContextItemSelected: " + e.toString());
 		}
 
 		final int touchedPos = adapterInfo.position;
 		final cgCache cache = adapter.getItem(touchedPos);
 
 		if (id == 1) { // compass
 			Intent navigateIntent = new Intent(activity, cgeonavigate.class);
 			navigateIntent.putExtra("latitude", cache.latitude);
 			navigateIntent.putExtra("longitude", cache.longitude);
 			navigateIntent.putExtra("geocode", cache.geocode.toUpperCase());
 			navigateIntent.putExtra("name", cache.name);
 			
 			activity.startActivity(navigateIntent);
 
 			return true;
 		} else if (id == 2) { // radar
 			try {
 				if (base.isIntentAvailable(activity, "com.google.android.radar.SHOW_RADAR") == true) {
 					Intent radarIntent = new Intent("com.google.android.radar.SHOW_RADAR");
 					radarIntent.putExtra("latitude", new Float(cache.latitude));
 					radarIntent.putExtra("longitude", new Float(cache.longitude));
 					activity.startActivity(radarIntent);
 				} else {
 					AlertDialog.Builder dialog = new AlertDialog.Builder(activity);
 					dialog.setTitle(res.getString(R.string.err_radar_title));
 					dialog.setMessage(res.getString(R.string.err_radar_message));
 					dialog.setCancelable(true);
 					dialog.setPositiveButton("yes", new DialogInterface.OnClickListener() {
 					   public void onClick(DialogInterface dialog, int id) {
 							try {
 								activity.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://search?q=pname:com.eclipsim.gpsstatus2")));
 								dialog.cancel();
 							} catch (Exception e) {
 								warning.showToast(res.getString(R.string.err_radar_market));
 								Log.e(cgSettings.tag, "cgeocaches.onContextItemSelected.radar.onClick: " + e.toString());
 							}
 					   }
 					});
 					dialog.setNegativeButton("no", new DialogInterface.OnClickListener() {
 					   public void onClick(DialogInterface dialog, int id) {
 							dialog.cancel();
 					   }
 					});
 
 				   AlertDialog alert = dialog.create();
 				   alert.show();
 				}
 			} catch (Exception e) {
 				warning.showToast(res.getString(R.string.err_radar_generic));
 				Log.e(cgSettings.tag, "cgeocaches.onContextItemSelected.radar: " + e.toString());
 			}
 
 			return true;
 		} else if (id == 3) { // show on map
 			Intent mapIntent = new Intent(activity, cgeomap.class);
 			mapIntent.putExtra("detail", false);
 			mapIntent.putExtra("geocode", cache.geocode);
 
 			activity.startActivity(mapIntent);
 
 			return true;
 		} else if (id == 4) { // show on external map
 			base.runExternalMap(activity, res, warning, tracker, cache.latitude, cache.longitude, cache.geocode, cache.name, true, cache.type, cache.found, cache.disabled);
 
 			return true;
 		} else if (id == 5) { // turn-by-turn
 			if (geo != null) {
 				base.runNavigation(activity, res, settings, warning, tracker, cache.latitude, cache.longitude, geo.latitudeNow, geo.longitudeNow);
 			} else {
 				base.runNavigation(activity, res, settings, warning, tracker, cache.latitude, cache.longitude);
 			}
 
 			return true;
 		} else if (id == 6) { // log visit
 			if (cache.cacheid == null || cache.cacheid.length() == 0) {
 				warning.showToast(res.getString(R.string.err_cannot_log_visit));
 				return true;
 			}
 
 			Intent logVisitIntent = new Intent(activity, cgeovisit.class);
 			logVisitIntent.putExtra("id", cache.cacheid);
 			logVisitIntent.putExtra("geocode", cache.geocode.toUpperCase());
 			logVisitIntent.putExtra("type", cache.type.toLowerCase());
 
 			activity.startActivity(logVisitIntent);
 
 			return true;
 		} else if (id == 7) { // cache details
 			Intent cachesIntent = new Intent(activity, cgeodetail.class);
 			cachesIntent.putExtra("geocode", cache.geocode.toUpperCase());
 			cachesIntent.putExtra("name", cache.name);
 			activity.startActivity(cachesIntent);
 
 			return true;
 		}
 
 		return false;
 	}
 
 	@Override
 	public boolean onKeyDown(int keyCode, KeyEvent event) {
 		if (keyCode == KeyEvent.KEYCODE_BACK) {
 			if (adapter != null) {
 				if (adapter.resetChecks() == true) {
 					return true;
 				} else if (adapter.getSelectMode() == true) {
 					adapter.setSelectMode(false, true);
 
 					return true;
 				}
 			}
 		}
 		return super.onKeyDown(keyCode, event);
 	}
 
 	private void setAdapter() {
 		if (listFooter == null) {
 			if (inflater == null) inflater = activity.getLayoutInflater();
 			listFooter = inflater.inflate(R.layout.caches_footer, null);
 			
 			listFooter.setClickable(true);
 			listFooter.setOnClickListener(new moreCachesListener());
 		}
 		if (listFooterText == null) {
 			listFooterText = (TextView)listFooter.findViewById(R.id.more_caches);
 		}
 
 		if (adapter == null) {
 			final ListView list = getListView();
 
 			list.setLongClickable(true);
 			registerForContextMenu(list);
 
 			list.addFooterView(listFooter);
 
 			adapter = new cgCacheListAdapter(activity, settings, cacheList, base);
 			setListAdapter(adapter);
 		}
 
 		if (adapter != null && geo != null) adapter.setActualCoordinates(geo.latitudeNow, geo.longitudeNow);
 		if (adapter != null && dir != null) adapter.setActualHeading(dir.directionNow);
 	}
 
 	private void setMoreCaches(boolean more) {
 		if (listFooter == null) return;
 		if (listFooterText == null) return;
 
 		if (more == false) {
 			if (cacheList == null || cacheList.isEmpty()) {
 				listFooterText.setText(res.getString(R.string.caches_no_cache));
 			} else {
 				listFooterText.setText(res.getString(R.string.caches_more_caches_no));
 			}
 			listFooter.setClickable(false);
 			listFooter.setOnClickListener(null);
 		} else {
 			listFooterText.setText(res.getString(R.string.caches_more_caches));
 			listFooter.setClickable(true);
 			listFooter.setOnClickListener(new moreCachesListener());
 		}
 	}
 
 	private void init() {
 		// sensor & geolocation manager
 		if (geo == null) {
 			geo = app.startGeo(activity, geoUpdate, base, settings, warning, 0, 0);
 		}
 		if (settings.livelist == 1 && settings.useCompass == 1 && dir == null) {
 			dir = app.startDir(activity, dirUpdate, warning);
 		}
 
 		if (cacheList != null) {
 			base.setTitle(activity, title + " (" + cacheList.size() + "/" + app.getTotal(searchId) + ")");
 		}
 
 		if (cacheList != null && cacheList.isEmpty() == false) {
 			final Integer count = app.getTotal(searchId);
 			final int size = cacheList.size();
 			if (count != null && count > 0) {
 				base.setTitle(activity, title + " (" + size + "/" + count + ")");
 				if (cacheList.size() < app.getTotal(searchId) && cacheList.size() < 1000) {
 					setMoreCaches(true);
 				} else {
 					setMoreCaches(false);
 				}
 			} else {
 				base.setTitle(activity, title);
 				setMoreCaches(false);
 			}
 		} else {
 			base.setTitle(activity, title);
 		}
 
 		setAdapter();
 		
 		if (geo != null) geoUpdate.updateLoc(geo);
 		if (dir != null) dirUpdate.updateDir(dir);
 	}
 
 	private void showOnMap() {
 		cgeomap mapActivity = new cgeomap();
 
 		Intent mapIntent = new Intent(activity, mapActivity.getClass());
 		mapIntent.putExtra("detail", false);
 		mapIntent.putExtra("searchid", searchId);
 
 		activity.startActivity(mapIntent);
 	}
 	
 	private void importGpx() {
 		activity.startActivity(new Intent(activity, cgeogpxes.class));
 
 		finish();
 	}
 
 	public void refreshStored() {
 		if (adapter != null && adapter.getChecked() > 0) {
 			// there are some checked caches
 			detailTotal = adapter.getChecked();
 		} else {
 			// no checked caches, download everything (when already stored - refresh them)
 			detailTotal = cacheList.size();
 		}
 
 		base.showProgress(activity, false);
 		waitDialog = new ProgressDialog(this);
 		waitDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
 		public void onCancel(DialogInterface arg0) {
 			try {
 				if (threadD != null) threadD.kill();
 
 				if (geo == null) geo = app.startGeo(activity, geoUpdate, base, settings, warning, 0, 0);
 				if (settings.livelist == 1 && settings.useCompass == 1 && dir == null) dir = app.startDir(activity, dirUpdate, warning);
 			} catch (Exception e) {
 				Log.e(cgSettings.tag, "cgeocaches.onOptionsItemSelected.onCancel: " + e.toString());
 			}
 			}
 		});
 
 		waitDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
 		int etaTime = (int)((detailTotal * 25) / 60);
 		if (etaTime < 1) {
 				waitDialog.setMessage("downloading caches...\neta: less than minute");
 		} else if (etaTime == 1) {
 				waitDialog.setMessage("downloading caches...\neta: " + etaTime + " min");
 		} else {
 				waitDialog.setMessage("downloading caches...\neta: " + etaTime + " mins");
 		}
 		waitDialog.setCancelable(true);
 		waitDialog.setMax(detailTotal);
 		waitDialog.show();
 
 		detailProgressTime = System.currentTimeMillis();
 
 		threadD = new geocachesLoadDetails(loadDetailsHandler);
 		threadD.start();
 	}
 
     public void dropStored() {
 		AlertDialog.Builder dialog = new AlertDialog.Builder(activity);
 		dialog.setCancelable(true);
 		dialog.setTitle("Drop stored");
 
 		if (adapter != null && adapter.getChecked() > 0) {
 			dialog.setMessage("Do you want to remove selected caches from device?");
 			dialog.setPositiveButton("yes", new DialogInterface.OnClickListener() {
 				public void onClick(DialogInterface dialog, int id) {
 					dropSelected();
 					dialog.cancel();
 				}
 			});
 		} else {
 			dialog.setMessage("Do you want to remove all caches from device?");
 			dialog.setPositiveButton("yes, all", new DialogInterface.OnClickListener() {
 				public void onClick(DialogInterface dialog, int id) {
 					dropSelected();
 					dialog.cancel();
 				}
 			});
 		}
 		dialog.setNegativeButton("no", new DialogInterface.OnClickListener() {
 			public void onClick(DialogInterface dialog, int id) {
 				dialog.cancel();
 			}
 		});
 
 		 AlertDialog alert = dialog.create();
 		 alert.show();
     }
 
 	public void dropSelected() {
 		if (adapter == null || adapter.getChecked() <= 0) {
 			// no checked caches, drop everything
 			app.dropStored();
 			activity.finish();
 			
 			return;
 		}
 
 		detailTotal = adapter.getChecked();
 
 		waitDialog = new ProgressDialog(this);
 		waitDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
 			public void onCancel(DialogInterface arg0) {
 				try {
 					if (threadR != null) threadR.kill();
 				} catch (Exception e) {
 					Log.e(cgSettings.tag, "cgeocaches.onOptionsItemSelected.onCancel: " + e.toString());
 				}
 				}
 		});
 
 		waitDialog.setCancelable(true);
 		waitDialog.show();
 
 		threadR = new geocachesDropDetails(dropDetailsHandler);
 		threadR.start();
 	}
 
 	private class update extends cgUpdateLoc {
 		@Override
 		public void updateLoc(cgGeo geo) {
 			if (geo == null) return;
 			if (adapter == null) return;
 
 			try {
 				if (cacheList != null && geo.latitudeNow != null && geo.longitudeNow != null) {
 					adapter.setActualCoordinates(geo.latitudeNow, geo.longitudeNow);
 				}
 
 				if (settings.useCompass == 0 || (geo.speedNow != null && geo.speedNow > 5)) { // use GPS when speed is higher than 18 km/h
 					if (settings.useCompass == 0) {
 						if (geo.bearingNow != null) adapter.setActualHeading(geo.bearingNow);
 						else adapter.setActualHeading(0.0f);
 					}
 					if (northHeading != null) adapter.setActualHeading(northHeading);
 				}
 			} catch (Exception e) {
 				Log.w(cgSettings.tag, "Failed to update location.");
 			}
 		}
 	}
 	
 	private class updateDir extends cgUpdateDir {
 		@Override
 		public void updateDir(cgDirection dir) {
 			if (settings.livelist == 0) return;
 			if (dir == null || dir.directionNow == null) return;
 
 			northHeading = dir.directionNow;
 			if (northHeading != null && adapter != null && (geo == null || geo.speedNow == null || geo.speedNow <= 5)) { // use compass when speed is lower than 18 km/h) {
 				adapter.setActualHeading(northHeading);
 			}
 		}
 	}
 
 	private class geocachesLoadNextPage extends Thread {
 		private Handler handler = null;
 
 		public geocachesLoadNextPage(Handler handlerIn) {
 			handler = handlerIn;
 		}
 
 		@Override
 		public void run() {
 			searchId = base.searchByNextPage(searchId, 0);
 
 			handler.sendMessage(new Message());
 		}
 	}
 
 	private class geocachesLoadByOffline extends Thread {
 		private Handler handler = null;
 		private Double latitude = null;
 		private Double longitude = null;
 
 		public geocachesLoadByOffline(Handler handlerIn, Double latitudeIn, Double longitudeIn) {
 			handler = handlerIn;
 			latitude = latitudeIn;
 			longitude = longitudeIn;
 		}
 
 		@Override
 		public void run() {
 			HashMap<String, Object> params = new HashMap<String, Object>();
 			if (latitude != null && longitude != null) {
 				params.put("latitude", latitude);
 				params.put("longitude", longitude);
 				params.put("cachetype", settings.cacheType);
 			}
 
 			searchId = base.searchByOffline(params);
 
 			handler.sendMessage(new Message());
 		}
 	}
 
 	private class geocachesLoadByCoords extends Thread {
 		private Handler handler = null;
 		private Double latitude = null;
 		private Double longitude = null;
 		private String cachetype = null;
 
 		public geocachesLoadByCoords(Handler handlerIn, Double latitudeIn, Double longitudeIn, String cachetypeIn) {
 			setPriority(Thread.MIN_PRIORITY);
 
 			handler = handlerIn;
 			latitude = latitudeIn;
 			longitude = longitudeIn;
 			cachetype = cachetypeIn;
 
 			if (latitude == null || longitude == null) {
 				warning.showToast("No coordinates given.");
 				
 				finish();
 				return;
 			}
 		}
 
 		@Override
 		public void run() {
 			HashMap<String, String> params = new HashMap<String, String>();
 			params.put("latitude", String.format((Locale)null, "%.6f", latitude));
 			params.put("longitude", String.format((Locale)null, "%.6f", longitude));
 			params.put("cachetype", cachetype);
 
 			searchId = base.searchByCoords(params, 0);
 
 			handler.sendMessage(new Message());
 		}
 	}
 	
 	private class geocachesLoadByKeyword extends Thread {
 		private Handler handler = null;
 		private String keyword = null;
 		private String cachetype = null;
 
 		public geocachesLoadByKeyword(Handler handlerIn, String keywordIn, String cachetypeIn) {
 			setPriority(Thread.MIN_PRIORITY);
 
 			handler = handlerIn;
 			keyword = keywordIn;
 			cachetype = cachetypeIn;
 
 			if (keyword == null) {
 				warning.showToast("No keyword given.");
 
 				finish();
 				return;
 			}
 		}
 
 		@Override
 		public void run() {
 			HashMap<String, String> params = new HashMap<String, String>();
 			params.put("keyword", keyword);
 			params.put("cachetype", cachetype);
 
 			searchId = base.searchByKeyword(params, 0);
 
 			handler.sendMessage(new Message());
 		}
 	}
 
 	private class geocachesLoadByUserName extends Thread {
 		private Handler handler = null;
 		private String username = null;
 		private String cachetype = null;
 
 		public geocachesLoadByUserName(Handler handlerIn, String usernameIn, String cachetypeIn) {
 			setPriority(Thread.MIN_PRIORITY);
 
 			handler = handlerIn;
 			username = usernameIn;
 			cachetype = cachetypeIn;
 
 			if (username == null || username.length() == 0) {
 				warning.showToast("No username given.");
 
 				finish();
 				return;
 			}
 		}
 
 		@Override
 		public void run() {
 			HashMap<String, String> params = new HashMap<String, String>();
 			params.put("username", username);
 			params.put("cachetype", cachetype);
 
 			searchId = base.searchByUsername(params, 0);
 
 			handler.sendMessage(new Message());
 		}
 	}
 
 	private class geocachesLoadByOwner extends Thread {
 		private Handler handler = null;
 		private String username = null;
 		private String cachetype = null;
 
 		public geocachesLoadByOwner(Handler handlerIn, String usernameIn, String cachetypeIn) {
 			setPriority(Thread.MIN_PRIORITY);
 
 			handler = handlerIn;
 			username = usernameIn;
 			cachetype = cachetypeIn;
 
 			if (username == null || username.length() == 0) {
 				warning.showToast("No username given.");
 
 				finish();
 				return;
 			}
 		}
 
 		@Override
 		public void run() {
 			HashMap<String, String> params = new HashMap<String, String>();
 			params.put("username", username);
 			params.put("cachetype", cachetype);
 
 			searchId = base.searchByOwner(params, 0);
 
 			handler.sendMessage(new Message());
 		}
 	}
 
 	private class geocachesLoadDetails extends Thread {
 		private Handler handler = null;
 		private volatile boolean needToStop = false;
 		private int checked = 0;
 
 		public geocachesLoadDetails(Handler handlerIn) {
 			setPriority(Thread.MIN_PRIORITY);
 
 			handler = handlerIn;
 
 			if (adapter != null) {
 				checked = adapter.getChecked();
 			}
 		}
 
 		public void kill() {
 			needToStop = true;
 		}
 
 		@Override
 		public void run() {
 			if (dir != null) dir = app.removeDir();
 			if (geo != null) geo = app.removeGeo();
 
			final ArrayList<cgCache> cacheListTemp = (ArrayList<cgCache>)cacheList.clone();
			for (cgCache cache : cacheListTemp) {
 				if (checked > 0 && cache.statusChecked == false) {
 					handler.sendEmptyMessage(0);
 
 					yield();
 					continue;
 				}
 
 				try {
 					if (needToStop == true) {
 						Log.i(cgSettings.tag, "Stopped storing process.");
 						break;
 					}
 
 					try {
 						sleep(3000 + ((Double)(Math.random() * 3000)).intValue());
 					} catch (Exception e) {
 						Log.e(cgSettings.tag, "cgeocaches.geocachesLoadDetails.sleep: " + e.toString());
 					}
 
 					if (needToStop == true) {
 						Log.i(cgSettings.tag, "Stopped storing process.");
 						break;
 					}
 
 					detailProgress ++;
 					base.storeCache(app, activity, cache, null, handler);
 
 					handler.sendEmptyMessage(cacheList.indexOf(cache));
 
 					yield();
 				} catch (Exception e) {
 					Log.e(cgSettings.tag, "cgeocaches.geocachesLoadDetails: " + e.toString());
 				}
 			}
			cacheListTemp.clear();
 
 			handler.sendEmptyMessage(-1);
 		}
 	}
 
 	private class geocachesDropDetails extends Thread {
 		private Handler handler = null;
 		private volatile boolean needToStop = false;
 		private int checked = 0;
 
 		public geocachesDropDetails(Handler handlerIn) {
 			setPriority(Thread.MIN_PRIORITY);
 
 			handler = handlerIn;
 
 			if (adapter != null) {
 				checked = adapter.getChecked();
 			}
 		}
 
 		public void kill() {
 			needToStop = true;
 		}
 
 		@Override
 		public void run() {
 			if (dir != null) dir = app.removeDir();
 			if (geo != null) geo = app.removeGeo();
 
			final ArrayList<cgCache> cacheListTemp = (ArrayList<cgCache>)cacheList.clone();
			for (cgCache cache : cacheListTemp) {
 				if (checked > 0 && cache.statusChecked == false) {
 					handler.sendEmptyMessage(0);
 
 					yield();
 					continue;
 				}
 
 				try {
 					if (needToStop == true) {
 						Log.i(cgSettings.tag, "Stopped dropping process.");
 						break;
 					}
 
 					detailProgress ++;
 					app.markDropped(cache.geocode);
 
 					handler.sendEmptyMessage(cacheList.indexOf(cache));
 
 					yield();
 				} catch (Exception e) {
 					Log.e(cgSettings.tag, "cgeocaches.geocachesDropDetails: " + e.toString());
 				}
 			}
			cacheListTemp.clear();
 
 			handler.sendEmptyMessage(-1);
 		}
 	}
 
 	private class moreCachesListener implements View.OnClickListener {
 		@Override
 		public void onClick(View arg0) {
 			base.showProgress(activity, true);
 			listFooter.setOnClickListener(null);
 
 			geocachesLoadNextPage thread;
 			thread = new geocachesLoadNextPage(loadNextPageHandler);
 			thread.start();
 		}
 	}
 
 	public void goHome(View view) {
 		base.goHome(activity);
 	}
 }
