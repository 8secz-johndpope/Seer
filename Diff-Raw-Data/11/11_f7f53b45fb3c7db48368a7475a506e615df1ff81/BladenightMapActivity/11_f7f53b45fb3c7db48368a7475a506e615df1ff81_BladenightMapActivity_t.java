 package de.greencity.bladenightapp.android.map;
 
 
 import java.io.File;
 import java.lang.ref.WeakReference;
 
 import org.apache.commons.io.FileUtils;
 import org.apache.commons.lang3.exception.ExceptionUtils;
 import org.mapsforge.android.maps.MapActivity;
 import org.mapsforge.android.maps.mapgenerator.TileCache;
 import org.mapsforge.core.model.BoundingBox;
 import org.mapsforge.core.model.GeoPoint;
 import org.mapsforge.core.model.MapPosition;
 import org.mapsforge.map.reader.header.FileOpenResult;
 
 import android.app.ProgressDialog;
 import android.content.Intent;
 import android.os.Bundle;
 import android.os.Environment;
 import android.os.Handler;
 import android.os.Message;
 import android.util.Log;
 import android.view.View;
 import android.view.ViewTreeObserver;
 import android.view.ViewTreeObserver.OnGlobalLayoutListener;
 import android.view.Window;
 import android.widget.LinearLayout;
 import android.widget.Toast;
 
 import com.markupartist.android.widget.ActionBar;
 
 import de.greencity.bladenightapp.android.actionbar.ActionBarConfigurator;
 import de.greencity.bladenightapp.android.actionbar.ActionBarConfigurator.ActionItemType;
 import de.greencity.bladenightapp.android.map.userovl.UserPositionOverlay;
 import de.greencity.bladenightapp.android.network.NetworkClient;
 import de.greencity.bladenightapp.android.tracker.GpsListener;
 import de.greencity.bladenightapp.android.utils.AsyncDownloadTaskHttpClient;
 import de.greencity.bladenightapp.android.utils.BroadcastReceiversRegister;
 import de.greencity.bladenightapp.android.utils.JsonCacheAccess;
 import de.greencity.bladenightapp.dev.android.R;
 import de.greencity.bladenightapp.network.messages.RealTimeUpdateData;
 import de.greencity.bladenightapp.network.messages.RouteMessage;
 
 public class BladenightMapActivity extends MapActivity {
 	@Override
 	public void onCreate(Bundle savedInstanceState) {
 		super.onCreate(savedInstanceState);
 
 		Log.i(TAG, "onCreate");
 
 		networkClient = new NetworkClient(this);
 
 		requestWindowFeature(Window.FEATURE_NO_TITLE);
 		setContentView(R.layout.activity_action);
 		createMapView();
 		createOverlays();
 
 		downloadProgressDialog = new ProgressDialog(this);
 		processionProgressBar = (ProcessionProgressBar) findViewById(R.id.progress_procession);
 
 	}
 
 	@Override
 	public void onStart() {
 		super.onStart();
 
 		Log.i(TAG, "onStart");
 
 		verifyMapFile();
 
 		configureBasedOnIntent();
 
 	}
 
 	@Override
 	protected void onNewIntent(Intent intent) {
 		super.onNewIntent(intent);
 
 		Log.i(TAG, "onNewIntent");
 
 		setIntent(intent);
 		configureBasedOnIntent();
 	}
 
 	private void configureBasedOnIntent() {
 
 		Log.i(TAG, "configureBasedOnIntent");
 
 		getActivityParametersFromIntent(getIntent());
 
 		configureActionBar();
 	}
 
 
 	@Override
 	public void onPause() {
 		super.onPause();
 		cancelAllAutomaticTasks();
 		isRunning = false;
 	}
 
 	public void cancelAllAutomaticTasks() {
 		if ( periodicTask != null )
 			periodicHandler.removeCallbacks(periodicTask);
 		if ( gpsListenerForPositionOverlay != null)
 			gpsListenerForPositionOverlay.cancelLocationUpdates();
 		if ( gpsListenerForNetworkClient != null )
 			gpsListenerForNetworkClient.cancelLocationUpdates();
 	}
 
 	@Override
 	public void onResume() {
 		super.onResume();
 
 		// Friend colors and stuff like that could have been changed in the meantime, so re-create the overlays
 		userPositionOverlay.onResume();
 		processionProgressBar.onResume();
 
 		if ( gpsListenerForPositionOverlay != null )
 			gpsListenerForPositionOverlay.cancelLocationUpdates();
 		gpsListenerForPositionOverlay = new GpsListener(this, userPositionOverlay);
 		gpsListenerForPositionOverlay.requestLocationUpdates(updatePeriod);
 
 		if ( gpsListenerForNetworkClient != null )
 			gpsListenerForNetworkClient.cancelLocationUpdates();
 		gpsListenerForNetworkClient = new GpsListener(this, networkClient);
 		gpsListenerForNetworkClient.requestLocationUpdates(updatePeriod);
 
 		if ( isShowingActiveEvent ) {
 			periodicTask = new Runnable() {
 				@Override
 				public void run() {
 					if ( ! isRunning )
 						return;
 					// Log.i(TAG, "periodic task");
 					if ( ! isRouteInfoAvailable )
 						requestRouteFromNetworkService();
 					getRealTimeDataFromServer();
 					periodicHandler.postDelayed(this, updatePeriod);
 				}
 			};
 			periodicHandler.postDelayed(periodicTask, updatePeriod);
 		}
 		else {
 			processionProgressBar.setVisibility(View.GONE);
 		}
 
 		// The auto-zooming of the fetched route requires to have the layout 
 		if (mapView.getWidth() == 0 || mapView.getHeight() == 0 ) {
 			Log.i(TAG, "scheduling triggerInitialRouteDataFetch");
 			ViewTreeObserver vto = mapView.getViewTreeObserver(); 
 			vto.addOnGlobalLayoutListener(new OnGlobalLayoutListener() { 
 				@Override 
 				public void onGlobalLayout() { 
 					triggerInitialRouteDataFetch();
 					mapView.getViewTreeObserver().removeGlobalOnLayoutListener(this); 
 				} 
 			}); 
 		}
 		else {
 			triggerInitialRouteDataFetch();
 		}
 
 		isRunning = true;
 	}
 
 	private void triggerInitialRouteDataFetch() {
 		Log.i(TAG, "triggerInitialRouteDataFetch");
 		updateRouteFromCache();
 		requestRouteFromNetworkService();
 	}
 
 	@Override
 	public void onStop() {
 		super.onStop();
 		cancelAllAutomaticTasks();
 		isRunning = false;
 	}
 
 
 	private void getActivityParametersFromIntent(Intent intent) {
 
 		Log.i(TAG, "getActivityParametersFromIntent intent="+intent);
 
 		isShowingActiveEvent = true;
 		if ( intent != null) {
 			Bundle bundle = intent.getExtras();
 			Log.i(TAG, "getActivityParametersFromIntent bundle="+bundle);
 			if ( bundle != null ) {
 				String routeNameFromBundle = bundle.getString(PARAM_ROUTENAME);
 				isShowingActiveEvent = bundle.getBoolean(PARAM_ACTIVE);
 				Log.i(TAG, "getActivityParametersFromIntent routeNameFromBundle="+routeNameFromBundle);
 				if ( routeNameFromBundle != null) {
 					if ( ! routeNameFromBundle.equals(routeName)) {
 						// Activity is (re)started with a new route, request automatic zooming
 						shallFitViewWhenPossible = true;
 						isRouteInfoAvailable = false;
 					}
 					routeName = routeNameFromBundle;
 				}
 			}
 			else {
 				Log.w(TAG, "bundle="+bundle);
 			}
 		}
 		else {
 			Log.w(TAG, "intent="+intent);
 		}
 		Log.i(TAG, "getActivityParametersFromIntent DONE routeName="+routeName);
 		Log.i(TAG, "isShowingActiveEvent="+isShowingActiveEvent);
 	}
 
 	static class GetRealTimeDataFromServerHandler extends Handler {
 		private WeakReference<BladenightMapActivity> reference;
 		GetRealTimeDataFromServerHandler(BladenightMapActivity activity) {
 			this.reference = new WeakReference<BladenightMapActivity>(activity);
 		}
 		@Override
 		public void handleMessage(Message msg) {
 			final BladenightMapActivity bladenightMapActivity = reference.get();
 			if ( bladenightMapActivity == null || bladenightMapActivity.isFinishing() || ! bladenightMapActivity.isRunning )
 				return;
 			RealTimeUpdateData realTimeUpdateData = (RealTimeUpdateData)msg.obj;
 			String liveRouteName = realTimeUpdateData.getRouteName(); 
 			if ( ! liveRouteName.equals(bladenightMapActivity.routeName) ) {
 				// the route has changed, typically Lang -> Kurz
 				Log.i(TAG, "GetRealTimeDataFromServerHandler: route has changed: " + bladenightMapActivity.routeName + " -> " + liveRouteName);
 				String text = bladenightMapActivity.getResources().getString(R.string.msg_route_has_changed);
 				Toast.makeText(bladenightMapActivity, text + " " + liveRouteName, Toast.LENGTH_LONG).show();
 				bladenightMapActivity.routeName = liveRouteName;
 				bladenightMapActivity.requestRouteFromNetworkService();
 			}
 			bladenightMapActivity.routeOverlay.update(realTimeUpdateData);
 			bladenightMapActivity.processionProgressBar.update(realTimeUpdateData);
 			bladenightMapActivity.userPositionOverlay.update(realTimeUpdateData);
 		}
 	}
 
 	protected void getRealTimeDataFromServer() {
 		networkClient.getRealTimeData(new GetRealTimeDataFromServerHandler(this), null);
 	}
 
 	protected void requestRouteFromNetworkService() {
 		if ( routeName.length() > 0 )
 			getSpecificRouteFromServer(routeName);
 		else
 			Log.e(TAG, "requestRouteFromNetworkService: I don't know what route to request. routeName=" + routeName);
 	}
 
 	static class GetRouteFromServerHandler extends Handler {
 		private WeakReference<BladenightMapActivity> reference;
 		GetRouteFromServerHandler(BladenightMapActivity activity) {
 			this.reference = new WeakReference<BladenightMapActivity>(activity);
 		}
 		@Override
 		public void handleMessage(Message msg) {
 			final BladenightMapActivity bladenightMapActivity = reference.get();
 			if ( bladenightMapActivity == null || bladenightMapActivity.isFinishing() || ! bladenightMapActivity.isRunning )
 				return;
 			RouteMessage routeMessage = (RouteMessage) msg.obj;
 			bladenightMapActivity.updateRouteFromRouteMessage(routeMessage);
 			JsonCacheAccess.saveRouteToCache(bladenightMapActivity, routeMessage);
 		}
 	}
 
 	private void getSpecificRouteFromServer(String routeName) {
 		Log.i(TAG,"getSpecificRouteFromServer routeName="+routeName);
 		networkClient.getRoute(routeName, new GetRouteFromServerHandler(this), null);
 	}
 
 	private void updateRouteFromRouteMessage(RouteMessage routeMessage) {
 		if ( ! routeMessage.getRouteName().equals(routeName) ) {
 			Log.e(TAG, "Inconsistency: Got \"" + routeMessage.getRouteName() + "\" but expected: \"" + routeName + "\"");
 			Log.i(TAG, "Trace: " + ExceptionUtils.getStackTrace( new Throwable()));
 		}
 		isRouteInfoAvailable = true;
 		routeName = routeMessage.getRouteName();
 		routeOverlay.update(routeMessage);
 		if ( shallFitViewWhenPossible ) {
 			shallFitViewWhenPossible = false;
 			fitViewToRoute();
 		}
 	}
 
 	private void updateRouteFromCache() {
 		RouteMessage message = JsonCacheAccess.getRouteFromCache(this, routeName);
 		if ( message != null ) {
 			updateRouteFromRouteMessage(message);
 		}
 	}
 
 	@Override
 	public void onDestroy() {
 		super.onDestroy();
 		broadcastReceiversRegister.unregisterReceivers();
 	}
 
 	private void configureActionBar() {
 		final ActionBar actionBar = (ActionBar) findViewById(R.id.actionbar);
 		ActionBarConfigurator configurator = new ActionBarConfigurator(actionBar)
 		.show(ActionItemType.FRIENDS)
 		.setTitle(R.string.title_map);
 		if ( isShowingActiveEvent ) {
 			configurator.show(ActionItemType.TRACKER_CONTROL);
 		}
 		configurator.configure();
 	}
 
 
 	public void createMapView() {
 
 		mapView = new BladenightMapView(this);
 		mapView.setClickable(true);
 		mapView.setBuiltInZoomControls(true);
 		mapView.setRenderTheme(CustomRenderTheme.CUSTOM_RENDER);
 
 		setMapFile();
 
 		LinearLayout parent = (LinearLayout) findViewById(R.id.map_parent);
 		parent.removeAllViews();
 
 		parent.addView(mapView);
 
 		TileCache fileSystemTileCache = mapView.getFileSystemTileCache();
 		fileSystemTileCache.setPersistent(true);
 		fileSystemTileCache.setCapacity(100);
 
 		centerViewOnCoordinates(new GeoPoint(48.132491, 11.543474), (byte)13);
 	}
 
 	public void createOverlays() {
 		if ( routeOverlay != null )
 			mapView.getOverlays().remove(routeOverlay);
 		routeOverlay = new RouteOverlay(mapView);
 		if ( userPositionOverlay != null )
 			mapView.getOverlays().remove(userPositionOverlay);
 		userPositionOverlay = new UserPositionOverlay(this, mapView);
 	}
 
 	// Will be called via the onClick attribute
 	// of the buttons in main.xml
 	public void onClick(View view) {	  
 		switch (view.getId()) {
 		//	    case R.id.next: goUp();
 		//	      break;
 
 		}
 	}
 
 	private void verifyMapFile() {
 		if ( ! new File(mapLocalPath).exists() ) {
 			startMapFileDownload();
 		}
 	}
 
 	private void startMapFileDownload() {
 		downloadProgressDialog.setMessage(getResources().getString(R.string.msg_download_maps));
 		downloadProgressDialog.setIndeterminate(false);
 		downloadProgressDialog.setMax(100);
 		downloadProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
 
 		downloadProgressDialog.show();
 
 		AsyncDownloadTaskHttpClient.StatusHandler handler = new AsyncDownloadTaskHttpClient.StatusHandler() {
 
 			@Override
 			public void onProgress(long current, long total) {
 				int percent = (int)(current*100.0/total);
 				downloadProgressDialog.setProgress(percent);
 			}
 
 			@Override
 			public void onDownloadSuccess() {
 				Log.i(TAG, "Download successful");
 				downloadProgressDialog.dismiss();
 				clearTileCache();
 				setMapFile();
 			}
 
 			@Override
 			public void onDownloadFailure() {
 				Log.i(TAG, "Download failed");
 				downloadProgressDialog.dismiss();
 				clearTileCache();
 				setMapFile();
 			}
 		};
 		networkClient.downloadFile(mapLocalPath, mapRemotePath, handler);
 	}
 
 	private void setMapFile() {
 		if ( mapView.setMapFile(new File(mapLocalPath)) == FileOpenResult.SUCCESS ) {
 			mapView.redraw();
 			// mapView.getMapViewPosition().setZoomLevel((byte) 15);
 			fitViewToRoute();
 		}
 		else {
 			Log.e(TAG, "Failed to set map file: " + mapLocalPath);
 		}
 	}
 
 	protected void fitViewToBoundingBox(BoundingBox boundingBox) {
 		if ( boundingBox != null && boundingBox.getLatitudeSpan() > 0 && boundingBox.getLongitudeSpan() > 0 )
 			mapView.fitViewToBoundingBox(boundingBox);
 	}
 
 	protected void fitViewToRoute() {
		if ( routeOverlay != null ) {
			shallFitViewWhenPossible = false;
			fitViewToBoundingBox(routeOverlay.getRouteBoundingBox());
		}
 	}
 
 	protected void fitViewToProcession() {
		if ( routeOverlay != null ) {
			fitViewToBoundingBox(routeOverlay.getProcessionBoundingBox());
		}
 	}
 
 
 	protected void centerViewOnCoordinates(GeoPoint center, byte zoomLevel) {
 		mapView.getMapViewPosition().setMapPosition(new MapPosition(center, zoomLevel));
 	}
 
 	private void clearTileCache() {
 		try {
 			Log.i(TAG, "Clearing Mapsforge cache...");
 			String externalStorageDirectory = Environment.getExternalStorageDirectory().getAbsolutePath();
 			String CACHE_DIRECTORY = "/Android/data/org.mapsforge.android.maps/cache/";
 			String cacheDirectoryPath = externalStorageDirectory + CACHE_DIRECTORY;
 			Log.i(TAG, "cacheDirectoryPath="+cacheDirectoryPath);
 			FileUtils.deleteDirectory(new File(cacheDirectoryPath));
 		} catch (Exception e) {
 			Log.w(TAG, "Failed to clear the MapsForge cache",e);
 		}
 	}
 
 	final static String TAG = "BladenightMapActivity";
 	private BroadcastReceiversRegister broadcastReceiversRegister = new BroadcastReceiversRegister(this); 
 	private final String mapLocalPath = Environment.getExternalStorageDirectory().getPath()+"/Bladenight/munich.map";
 	private final String mapRemotePath = "maps/munich.map";
 	private ProgressDialog downloadProgressDialog;
 	private String routeName = "";
 	private boolean isShowingActiveEvent = false;
 	private RouteOverlay routeOverlay;
 	private BladenightMapView mapView;
 	private ProcessionProgressBar processionProgressBar;
 	private NetworkClient networkClient;
 	private final int updatePeriod = 3000;
 	private final Handler periodicHandler = new Handler();
 	private Runnable periodicTask;
 	private UserPositionOverlay userPositionOverlay;
 	private GpsListener gpsListenerForPositionOverlay;
 	private GpsListener gpsListenerForNetworkClient;
 	private boolean isRouteInfoAvailable = false;
 	static public final String PARAM_ROUTENAME = "routeName";
 	public static final String PARAM_ACTIVE = "active";
 	private boolean isRunning = true;
 	private boolean shallFitViewWhenPossible = true;
 
 } 
