 package org.cirrus.mobi.smarttransport;
 
 import java.util.ArrayList;
 import java.util.List;
 
 import org.cirrus.mobi.smarttransport.PublicNetworkProvider.ResultCallbacks;
 
 import android.content.Context;
 import android.graphics.Bitmap;
 import android.graphics.Canvas;
 import android.location.Location;
 import android.location.LocationListener;
 import android.location.LocationManager;
 import android.os.Bundle;
 import android.os.Handler;
 import android.text.TextUtils;
 import android.util.DisplayMetrics;
 import android.util.Log;
 import android.view.LayoutInflater;
 import android.view.View;
 import android.view.ViewGroup;
 import android.view.ViewGroup.LayoutParams;
 import android.widget.LinearLayout;
 import android.widget.RelativeLayout;
 import android.widget.TableLayout;
 import android.widget.TextView;
 
 import com.sonyericsson.extras.liveware.aef.control.Control;
 import com.sonyericsson.extras.liveware.extension.util.control.ControlExtension;
 
 import de.schildbach.pte.BahnProvider;
 import de.schildbach.pte.dto.Departure;
 import de.schildbach.pte.dto.NearbyStationsResult;
 import de.schildbach.pte.dto.QueryDeparturesResult;
 import de.schildbach.pte.dto.StationDepartures;
 /**
  *	 This file is part of SmartTransport
  *
  *   SmartTransport is free software: you can redistribute it and/or modify
  *   it under the terms of the GNU General Public License as published by
  *   the Free Software Foundation, either version 3 of the License, or
  *   (at your option) any later version.
  *
  *   SmartTransport is distributed in the hope that it will be useful,
  *   but WITHOUT ANY WARRANTY; without even the implied warranty of
  *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  *   GNU General Public License for more details.
  *
  *   You should have received a copy of the GNU General Public License
  *   along with SmartTransport.  If not, see <http://www.gnu.org/licenses/>.
  */
 public class SmartWatchControlExtension extends ControlExtension implements ResultCallbacks {
 
 	private static final int MAX_DEPATURE_ROWS = 3;
 	private Handler mHandler;
 	private Context mContext;
 	private int width;
 	private int height;
 	private Bitmap mBackground;
 	private static final Bitmap.Config BITMAP_CONFIG = Bitmap.Config.RGB_565;
 
 	private static final int STATE_INITIAL = 1;
 	private static final int STATE_SEARCHING = 2;
 	private static final int STATE_DISPLAY_DATA = 3;
 	protected static final String TAG = "SMT/SWCE";
 	
 	private int state = STATE_INITIAL;
 	private BahnProvider networkProvider;
 	private LocationManager locationManager;
 	private PublicNetworkProvider publicNetworkProvider;
 	private NearbyStationsResult mNearbyStationsResult;
 	private int mStationIndex;
 	private List<QueryDeparturesResult> mQueryDeparturesResults;
 	private LayoutInflater mInflater;
 	private int mScrollIndex;
 	
 
 	public SmartWatchControlExtension(Context context, String hostAppPackageName, Handler handler) {
 		super(context, hostAppPackageName);
 		if (handler == null) {
 			throw new IllegalArgumentException("handler == null");
 		}
 		mHandler = handler;
 		mContext = context;
 		width = getSupportedControlWidth(context);
 		height = getSupportedControlHeight(context);
 		mQueryDeparturesResults = new ArrayList<QueryDeparturesResult>(0);
 	}
 
 
 	/**
 	 * Get supported control width.
 	 *
 	 * @param context The context.
 	 * @return the width.
 	 */
 	public static int getSupportedControlWidth(Context context) {
 		return context.getResources().getDimensionPixelSize(R.dimen.smart_watch_control_width);
 	}
 
 	/**
 	 * Get supported control height.
 	 *
 	 * @param context The context.
 	 * @return the height.
 	 */
 	public static int getSupportedControlHeight(Context context) {
 		return context.getResources().getDimensionPixelSize(R.dimen.smart_watch_control_height);
 	}
 	
 	@Override
 	public void onStart() {	
 		super.onStart();
 		//intial call, kick search
 		state = STATE_SEARCHING;
 		
 		mStationIndex = 0;
 		
 		//TODO: get this from config
 		networkProvider = new BahnProvider();
 		// Acquire a reference to the system Location Manager
 		locationManager = (LocationManager) mContext.getSystemService(Context.LOCATION_SERVICE);
 		publicNetworkProvider = new PublicNetworkProvider(this, networkProvider);
 
 		locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, locationListener);
 		
 		mInflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
 
 	}
 	
 	@Override
 	public void onPause() {
 		super.onPause();
 		locationManager.removeUpdates(locationListener);
 	}
 
 	/**
 	 * this method is expected to display a bitmap
 	 */
 	@Override
 	public void onResume() {    
 		super.onResume();
 		redraw();
 	}
 
 	private void redraw() {
 		switch (state) {
 		case STATE_SEARCHING:
 			showSearchImage();
 			break;
 		case STATE_DISPLAY_DATA:
 			showData();
 		}
 		
 	}
 
 
 	@Override
 	public void onSwipe(int direction) {	
 		super.onSwipe(direction);
 		switch (state) {
 		case STATE_DISPLAY_DATA:
 			handleSwipe(direction);
 			break;
 
 		default:
 			break;
 		}
 	}
 	
 	private void handleSwipe(int direction) {
 		switch (direction) {
 		case Control.Intents.SWIPE_DIRECTION_LEFT:
 			if(mNearbyStationsResult != null)
 			{
 				mStationIndex--;
 				if(mStationIndex < 0)
 					mStationIndex = mNearbyStationsResult.stations.size()-1;
 				redraw();
 			}
 			break;
 		case Control.Intents.SWIPE_DIRECTION_RIGHT:
 			if(mNearbyStationsResult != null)
 			{
 				mStationIndex++;
 				if(mStationIndex > mNearbyStationsResult.stations.size()-1)
 					mStationIndex = 0;
 				redraw();
 			}			
 			break;
 			
 		case Control.Intents.SWIPE_DIRECTION_DOWN:
 				mScrollIndex--;
 				if(mScrollIndex < 0 )
 					mScrollIndex = 0;
 			break;
 	
 		case Control.Intents.SWIPE_DIRECTION_UP:
 				mScrollIndex++;

 			break;
 			
 		default:
 			break;
 		}
 		
 	}
 
 
 	private void showSearchImage() {
 		// Create background bitmap for animation.
 		mBackground = Bitmap.createBitmap(width, height, BITMAP_CONFIG); // Set default density to avoid scaling. background.setDensity(DisplayMetrics.DENSITY_DEFAULT);
 		mBackground.setDensity(DisplayMetrics.DENSITY_DEFAULT);
 		RelativeLayout locatingLayout = (RelativeLayout)RelativeLayout.inflate(mContext, R.layout.locating, null);
 		// Draw on canvas
 		Canvas canvas = new Canvas(mBackground);
 		locatingLayout.draw(canvas);
 		// Send bitmap to accessory
 		showBitmap(mBackground);
 		
 	}
 
 
 	public void showData()
 	{	
 		// Create background bitmap for animation.
 		mBackground = Bitmap.createBitmap(width, height, BITMAP_CONFIG); // Set default density to avoid scaling. background.setDensity(DisplayMetrics.DENSITY_DEFAULT);
 		//LinearLayout root = new LinearLayout(mContext); root.setLayoutParams(new LayoutParams(width, height));
 		//mBackground.setDensity(DisplayMetrics.DENSITY_DEFAULT);
 		LinearLayout stationsLayout = (LinearLayout)LinearLayout.inflate(mContext, R.layout.smartwatch_stations, null);
 		stationsLayout.setLayoutParams(new LayoutParams(width, height));
 		if(BuildConfig.DEBUG)
 			Log.d(TAG, "Using: w:"+width+" h: "+height);
 		
 		//fill Data
 		//station name
 		int departureRows = -1;
 		if(mNearbyStationsResult != null)
 		{
 			if(BuildConfig.DEBUG)
 				Log.d(TAG, "mStation index: "+mStationIndex+ " stations size: "+mNearbyStationsResult.stations.size());
 
 			de.schildbach.pte.dto.Location station = mNearbyStationsResult.stations.get(mStationIndex);
 			TextView stationName = (TextView) stationsLayout.findViewById(R.id.Station);
 			stationName.setText(shortStationName(station));
 			
 			stationsLayout.measure(width, height); 
 			stationsLayout.layout(0, 0, stationsLayout.getMeasuredWidth(),
 					stationsLayout.getMeasuredHeight());
 
 			int lines = stationName.getLineCount();			
			departureRows = MAX_DEPATURE_ROWS - lines;
 			if(BuildConfig.DEBUG)
 				Log.d(TAG, "calculated rows: "+ departureRows+ " line count header: "+ lines);
 		}
 		//depatures
 		if(mQueryDeparturesResults.size() > 0)
 		{
 			if(BuildConfig.DEBUG)
				Log.d(TAG, "mStation index: "+mStationIndex+ " departure size: "+mQueryDeparturesResults.size());
 
 			//how many rows can we insert
 			TableLayout tl = (TableLayout) stationsLayout.findViewById(R.id.departuesTable);
 			List<StationDepartures> dep = mQueryDeparturesResults.get(mStationIndex).stationDepartures;
 			for (StationDepartures stationDepartures : dep) {
 				List<Departure> depatures = stationDepartures.departures;
 				for(int i = 0; i < depatures.size(); i++)
 				{
					Departure depature = depatures.get(i);
 					View table = mInflater.inflate(R.layout.table_row_departure, tl, true);
 					
 					View row = ((ViewGroup)table).getChildAt(i*2);
 					View textView = ((ViewGroup)table).getChildAt((i*2)+1);
 										
 					TextView depLine = (TextView) row.findViewById(R.id.depLine);
 					depLine.setText(depature.line.label);
 					
 					TextView depTime = (TextView) row.findViewById(R.id.depTime);
 					depTime.setText(depature.plannedTime+"");//TODO: delays
 					
 					TextView depDest = (TextView) textView.findViewById(R.id.depTarget);
 					depDest.setText(depature.destination.name);
					if(i >= departureRows)
 						break;				
 				}
 			}
 		
 		}
 		stationsLayout.measure(width, height); 
 		stationsLayout.layout(0, 0, stationsLayout.getMeasuredWidth(),
 				stationsLayout.getMeasuredHeight());
 
 		// Draw on canvas
 		Canvas canvas = new Canvas(mBackground);
 		stationsLayout.draw(canvas);
 		// Send bitmap to accessory
 		showBitmap(mBackground);
 	}
 
 
 	private CharSequence shortStationName(de.schildbach.pte.dto.Location station) {
 		String stationName = "unknown";
 		if(!TextUtils.isEmpty(station.name))
 		{
 			stationName = station.name;
 			int index = station.name.indexOf(",");
 			if(index > 0)
 				stationName = station.name.substring(0, index);
 		}
 		return stationName;
 	}
 
 
 	@Override
 	public void nearbyStationsReceived(NearbyStationsResult result) {
 		this.mQueryDeparturesResults.clear();
 		this.mNearbyStationsResult = result;
 		state = STATE_DISPLAY_DATA;
 		if(BuildConfig.DEBUG)
 			Log.d(TAG, "Found: "+result.stations.size()+" stations");
 		redraw();
 		// for eacht station, request depatures
 		for (de.schildbach.pte.dto.Location station: result.stations ) {
 			publicNetworkProvider.getDepatures(station);	
 		}
 		
 	}
 
 
 	@Override
 	public void depaturesReceived(QueryDeparturesResult result) {
 		if(this.mQueryDeparturesResults == null)
 			this.mQueryDeparturesResults = new ArrayList<QueryDeparturesResult>(0);
 		this.mQueryDeparturesResults.add(result);
 		redraw();
 		
 	}
 
 	// Define a listener that responds to location updates
 	LocationListener locationListener = new LocationListener() {
 
 		private Location clocation;
 
 		public void onLocationChanged(Location location) {
 			// Called when a new location is found by the network location provider.
 			clocation = location;
 			Log.v(TAG, "got location to: "+location);
 			publicNetworkProvider.getNearbyStations(location);
 			locationManager.removeUpdates(locationListener);
 		};
 
 		public void onStatusChanged(String provider, int status, Bundle extras) {}
 
 		public void onProviderEnabled(String provider) {}
 
 		public void onProviderDisabled(String provider) {}
 	};
 
 }
