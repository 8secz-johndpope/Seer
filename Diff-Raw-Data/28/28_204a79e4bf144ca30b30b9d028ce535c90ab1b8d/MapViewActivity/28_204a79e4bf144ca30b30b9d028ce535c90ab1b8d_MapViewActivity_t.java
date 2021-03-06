 package com.doug.nextbus.activities;
 
 import com.doug.nextbus.backend.Data;
 
 import java.util.ArrayList;
 import java.util.LinkedList;
 import java.util.List;
 
 import org.json.JSONArray;
 import org.json.JSONException;
 import org.json.JSONObject;
 
 import android.graphics.Color;
 import android.graphics.drawable.Drawable;
 import android.os.AsyncTask;
 import android.os.Bundle;
 import android.os.Handler;
 import android.util.Log;
 import android.view.MotionEvent;
 import android.view.View;
 import android.view.View.OnClickListener;
 import android.view.View.OnTouchListener;
 import android.widget.ProgressBar;
 
 import com.doug.nextbus.R;
 import com.doug.nextbus.backend.APIController;
 import com.doug.nextbus.backend.BusOverlayItem;
 import com.doug.nextbus.backend.MapItemizedOverlay;
 import com.doug.nextbus.backend.RouteOverlay;
 import com.google.android.maps.GeoPoint;
 import com.google.android.maps.MapActivity;
 import com.google.android.maps.MapController;
 import com.google.android.maps.MapView;
 import com.google.android.maps.Overlay;
 import com.google.android.maps.OverlayItem;
 
 public class MapViewActivity extends MapActivity {
 
 	List<Overlay> mapOverlays;
 	Drawable redArrow;
 	Drawable blueArrow;
 	Drawable greenArrow;
 	Drawable yellowArrow;
 	Drawable purpleArrow;
 	MapItemizedOverlay redOverlay;
 	MapItemizedOverlay blueOverlay;
 	MapItemizedOverlay greenOverlay;
 	MapItemizedOverlay yellowOverlay;
 	MapItemizedOverlay purpleOverlay;
 	MapController mapController;
 	static ProgressBar pg;
 	MapView mapView;
 	Handler refreshHandler;
 	Runnable updateMapTask;
 	View redButton;
 	View blueButton;
 	View greenButton;
 	View yellowButton;
 	String displayRoute;
 	List<Overlay> redPath;
 	List<Overlay> bluePath;
 	List<Overlay> greenPath;
 	List<Overlay> yellowPath;
	boolean routesAreSet; 
 	public void onCreate(Bundle savedInstanceState) {
 		super.onCreate(savedInstanceState);
 		setContentView(R.layout.map_view);
 
 		mapView = (MapView) findViewById(R.id.mapview);
 
 		mapOverlays = mapView.getOverlays();
 		mapController = mapView.getController();
 
 		// Set map zoom. 17 is too close, 15 is too far.
 		mapController.setZoom(16);
 
 		redButton = (View) findViewById(R.id.redButton);
 		blueButton = (View) findViewById(R.id.blueButton);
 		greenButton = (View) findViewById(R.id.greenButton);
 		yellowButton = (View) findViewById(R.id.yellowButton);
 
 		redButton.setBackgroundColor(getResources().getColor(R.color.red));
 		greenButton.setBackgroundColor(getResources().getColor(R.color.green));
 		blueButton.setBackgroundColor(getResources().getColor(R.color.blue));
 		yellowButton.setBackgroundColor(getResources().getColor(R.color.yellow));
 
 		redPath = new LinkedList<Overlay>();
 		bluePath = new LinkedList<Overlay>();
 		greenPath = new LinkedList<Overlay>();
 		yellowPath = new LinkedList<Overlay>();
 		routesAreSet = false;
 
 		resetButtonTransparencies();
 		displayRoute = "red";
 		redButton.getBackground().setAlpha(250);
 		redButton.setOnClickListener(new OnClickListener() {
 
 			public void onClick(View arg0) {
 				resetButtonTransparencies();
 				redButton.getBackground().setAlpha(250);
 				displayRoute = "red";
 				refreshMap();
 			}
 		});
 		blueButton.setOnClickListener(new OnClickListener() {
 
 			public void onClick(View arg0) {
 				resetButtonTransparencies();
 				blueButton.getBackground().setAlpha(250);
 				displayRoute = "blue";
 				refreshMap();
 			}
 		});
 		yellowButton.setOnClickListener(new OnClickListener() {
 
 			public void onClick(View arg0) {
 				resetButtonTransparencies();
 				yellowButton.getBackground().setAlpha(250);
 				displayRoute = "yellow";
 				refreshMap();
 			}
 		});
 		greenButton.setOnClickListener(new OnClickListener() {
 
 			public void onClick(View arg0) {
 				resetButtonTransparencies();
 				greenButton.getBackground().setAlpha(250);
 				displayRoute = "green";
 				refreshMap();
 			}
 		});
 
 		// Grab all the drawables for the bus dots.
 		redArrow = this.getResources().getDrawable(R.drawable.red_arrow);
 		blueArrow = this.getResources().getDrawable(R.drawable.blue_arrow);
 		greenArrow = this.getResources().getDrawable(R.drawable.green_arrow);
 		yellowArrow = this.getResources().getDrawable(R.drawable.yellow_arrow);
 		purpleArrow = this.getResources().getDrawable(R.drawable.purple_arrow);
 
 		purpleOverlay = new MapItemizedOverlay(purpleArrow);
 		redOverlay = new MapItemizedOverlay(redArrow);
 		blueOverlay = new MapItemizedOverlay(blueArrow);
 		greenOverlay = new MapItemizedOverlay(greenArrow);
 		yellowOverlay = new MapItemizedOverlay(yellowArrow);
 
 		// Center the map
 		GeoPoint centerPoint = new GeoPoint(33776499, -84398400);
 		mapController.animateTo(centerPoint);
 
 		refreshHandler = new Handler();
 		updateMapTask = new Runnable() {
 
 			public void run() { // Refreshes map every two seconds
 				Log.i("POSTED", "POSTED");
 				new refreshBusLocations().execute();
 				refreshHandler.postDelayed(this, 5000);
 			}
 
 		};
 		Log.i("FINISHED", "FINISHED");
 
 	}
 
 	public void setRoutes() {
 
 		Log.i("MapViewActivity", "Setting routes...");
 		try {
 			for (int x = 0; x < 4; x++) {
 				JSONArray routePathData = null;
 				switch (x) {
 				case 0:
 					routePathData = Data.getRoutePathData("red");
 					break;
 				case 1:
 					routePathData = Data.getRoutePathData("blue");
 					break;
 				case 2:
 					routePathData = Data.getRoutePathData("trolley");
 					break;
 				case 3:
 					routePathData = Data.getRoutePathData("green");
 					break;
 				}
 
 				GeoPoint oldPoint = null;
 				GeoPoint newPoint = null;
 
 				for (int i = 0; i < routePathData.length(); i++) {
 
 					JSONObject path = routePathData.getJSONObject(i);
 					JSONArray points = path.getJSONArray("point");
 					for (int j = 0; j < points.length(); j++) {
 
 						JSONObject coords = points.getJSONObject(j);
 						int latitude = (int) (Float.parseFloat(coords.getString("lat")) * 1e6);
 						int longitude = (int) (Float.parseFloat(coords.getString("lon")) * 1e6);
 						newPoint = new GeoPoint(latitude, longitude);
 						if (oldPoint != null) {
 							switch (x) {
 							case 0:
 								redPath.add(new RouteOverlay(oldPoint, newPoint, 2, Color.parseColor("#e63f3f")));
 								break;
 							case 1:
 								bluePath.add(new RouteOverlay(oldPoint, newPoint, 2, Color.parseColor("#0078ff")));
 								break;
 							case 2:
 								yellowPath.add(new RouteOverlay(oldPoint, newPoint, 2, Color.parseColor("#ffd200")));
 								break;
 							case 3:
 								greenPath.add(new RouteOverlay(oldPoint, newPoint, 2, Color.parseColor("#02d038")));
 								break;
 							}
 
 						}
 						oldPoint = newPoint;
 					}
 					oldPoint = null;
 				}
 			}
 		} catch (JSONException je) {
 
 		}
 		routesAreSet = true;
 		Log.i("MapViewActivity", "Routes set.");
 	}
 
 	public void resetButtonTransparencies() {
 		redButton.getBackground().setAlpha(100);
 		greenButton.getBackground().setAlpha(100);
 		blueButton.getBackground().setAlpha(100);
 		yellowButton.getBackground().setAlpha(100);
 	}
 
 	public void refreshMap() {
 		mapOverlays.clear();
 
 		if (displayRoute.equals("red")) {
 			mapOverlays.add(redOverlay);
 			mapOverlays.addAll(redPath);
 		} else if (displayRoute.equals("blue")) {
 			mapOverlays.add(blueOverlay);
 			mapOverlays.addAll(bluePath);
 		} else if (displayRoute.equals("yellow")) {
 			mapOverlays.add(yellowOverlay);
 			mapOverlays.addAll(yellowPath);
 		} else if (displayRoute.equals("green")) {
 			mapOverlays.add(greenOverlay);
 			mapOverlays.addAll(greenPath);
 		}
 
 		mapView.postInvalidate();
 	}
 
 	/**
 	 * This grabs the current bus locations from the m.gatech.edu URL and plots
 	 * the color-coded locations of the buses. As mentioned above, this gets
 	 * called every two seconds.
 	 */
 
 	@Override
 	protected boolean isRouteDisplayed() {
 		return false;
 	}
 
 	/**
 	 * Stop updating location when paused.
 	 */
 	@Override
 	public void onPause() {
 		super.onPause();
 		refreshHandler.removeCallbacks(updateMapTask);
 	}
 
 	/**
 	 * Resume updating location when resumed.
 	 */
 	@Override
 	public void onResume() {
 		super.onResume();
 		refreshHandler.post(updateMapTask);
 	}
 
 	private class refreshBusLocations extends AsyncTask<Void, Void, ArrayList<MapItemizedOverlay>> {
 
 		protected ArrayList<MapItemizedOverlay> doInBackground(Void... voids) {
 
 			if (!routesAreSet) {
 				setRoutes();
 			}
 
 			ArrayList<String[]> busLocations = APIController.getBusLocations();
 
 			// clear all current overlays
 			ArrayList<MapItemizedOverlay> overlays = new ArrayList<MapItemizedOverlay>();
 
 			MapItemizedOverlay backgroundPurpleOverlay = new MapItemizedOverlay(purpleArrow);
 			MapItemizedOverlay backgroundRedOverlay = new MapItemizedOverlay(redArrow);
 			MapItemizedOverlay backgroundGreenOverlay = new MapItemizedOverlay(greenArrow);
 			MapItemizedOverlay backgroundYellowOverlay = new MapItemizedOverlay(yellowArrow);
 			MapItemizedOverlay backgroundBlueOverlay = new MapItemizedOverlay(blueArrow);
 
 			overlays.add(backgroundPurpleOverlay);
 			overlays.add(backgroundRedOverlay);
 			overlays.add(backgroundBlueOverlay);
 			overlays.add(backgroundYellowOverlay);
 			overlays.add(backgroundGreenOverlay);
 
 			for (String[] entry : busLocations) {
 
 				try { // TODO - parseFloat fails sometimes?
 					int latitude = (int) (Float.parseFloat(entry[2]) * 1e6);
 					int longitude = (int) (Float.parseFloat(entry[3]) * 1e6);
 					int platitude = (int) (Float.parseFloat(entry[4]) * 1e6);
 					int plongitude = (int) (Float.parseFloat(entry[5]) * 1e6);
 					GeoPoint busLocation = new GeoPoint(latitude, longitude);
 					GeoPoint pBusLocation = new GeoPoint(platitude, plongitude);
 					BusOverlayItem busOverlayItem;
 
 					String route = entry[0];
 					// String[] currentRoutes = (String[])
 					// (RoutePickerActivity.getActiveRoutesList())[0];
 					//
 					// // Add to the correct overlay.
 					// if (currentRoutes.length == 1 &&
 					// currentRoutes[0].equals("night") && route.equals("red"))
 					// {
 					// busOverlayItem = new BusOverlayItem(busLocation, "", "",
 					// pBusLocation, mapView, purpleArrow);
 					// backgroundPurpleOverlay.addOverlay(busOverlayItem);
 					// } else
 					if (route.equals("red")) {
 						busOverlayItem = new BusOverlayItem(busLocation, "", "", pBusLocation, mapView, redArrow);
 						backgroundRedOverlay.addOverlay(busOverlayItem);
 					} else if (route.equals("blue")) {
 						busOverlayItem = new BusOverlayItem(busLocation, "", "", pBusLocation, mapView, blueArrow);
 						backgroundBlueOverlay.addOverlay(busOverlayItem);
 					} else if (route.equals("yellow")) {
 						busOverlayItem = new BusOverlayItem(busLocation, "", "", pBusLocation, mapView, yellowArrow);
 						backgroundYellowOverlay.addOverlay(busOverlayItem);
 					} else if (route.equals("green")) {
 						busOverlayItem = new BusOverlayItem(busLocation, "", "", pBusLocation, mapView, greenArrow);
 						backgroundGreenOverlay.addOverlay(busOverlayItem);
 					}
 
 				} catch (NumberFormatException nfe) {
 					Log.e("RefreshMap", "Couldn't parse coordinates");
 				}
 			}
 
 			overlays.add(backgroundPurpleOverlay);
 			overlays.add(backgroundRedOverlay);
 			overlays.add(backgroundBlueOverlay);
 			overlays.add(backgroundYellowOverlay);
 			overlays.add(backgroundGreenOverlay);
 
 			return overlays;
 		}
 
 		public void onPostExecute(ArrayList<MapItemizedOverlay> overlays) {
 
 			purpleOverlay = overlays.get(0);
 			redOverlay = overlays.get(1);
 			blueOverlay = overlays.get(2);
 			yellowOverlay = overlays.get(3);
 			greenOverlay = overlays.get(4);
 
 			refreshMap();
 			Log.v("RefreshMap", "Map refreshed");
 		}
 
 	}
 
}
