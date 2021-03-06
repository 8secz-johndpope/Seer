 package carnero.netmap.fragment;
 
 import android.content.Context;
 import android.location.Location;
 import android.os.Bundle;
 import android.telephony.CellInfo;
 import android.telephony.CellLocation;
 import android.telephony.PhoneStateListener;
 import android.telephony.TelephonyManager;
 import android.telephony.cdma.CdmaCellLocation;
 import android.telephony.gsm.GsmCellLocation;
 import android.util.Log;
 import android.view.LayoutInflater;
 import android.view.View;
 import android.view.ViewGroup;
 import carnero.netmap.App;
 import carnero.netmap.R;
 import carnero.netmap.common.Constants;
 import carnero.netmap.common.Geo;
 import carnero.netmap.common.SimpleGeoReceiver;
 import carnero.netmap.common.Util;
 import carnero.netmap.listener.OnBtsCacheChangedListener;
 import carnero.netmap.listener.OnSectorCacheChangedListener;
 import carnero.netmap.model.*;
 import com.google.android.gms.maps.CameraUpdateFactory;
 import com.google.android.gms.maps.GoogleMap;
 import com.google.android.gms.maps.MapFragment;
 import com.google.android.gms.maps.UiSettings;
 import com.google.android.gms.maps.model.*;
 
 import java.util.ArrayList;
 import java.util.HashMap;
 import java.util.List;
 
 public class NetMapFragment extends MapFragment implements SimpleGeoReceiver, OnBtsCacheChangedListener, OnSectorCacheChangedListener {
 
 	private Geo mGeo;
 	private GoogleMap mMap;
 	private boolean mCentered = false;
 	private TelephonyManager mTelephony;
 	private LatLng mLastLocation;
 	private Bts mLastBts;
 	private Marker mMyMarker;
 	private Polyline mConnectionCurrent;
 	private int[] mFillColors = new int[5];
 	private HashMap<String, Marker> mBtsMarkers = new HashMap<String, Marker>();
 	private HashMap<XY, Polygon> mCoveragePolygons = new HashMap<XY, Polygon>();
 	private float mZoomDefault = 16f;
 	//
 	final private StatusListener mListener = new StatusListener();
 
 	@Override
 	public void onActivityCreated(Bundle state) {
 		super.onActivityCreated(state);
 
 		mTelephony = (TelephonyManager) getActivity().getSystemService(Context.TELEPHONY_SERVICE);
 
 		mFillColors[0] = getResources().getColor(R.color.connection_l1);
 		mFillColors[1] = getResources().getColor(R.color.connection_l2);
 		mFillColors[2] = getResources().getColor(R.color.connection_l3);
 		mFillColors[3] = getResources().getColor(R.color.connection_l4);
 		mFillColors[4] = getResources().getColor(R.color.connection_l5);
 	}
 
 	@Override
 	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle state) {
 		final View mapView = super.onCreateView(inflater, container, state);
 		setMapTransparent((ViewGroup) mapView);
 
 		return mapView;
 	}
 
 	@Override
 	public void onResume() {
 		super.onResume();
 
 		initializeMap();
 
 		SectorCache.addListener(this);
 		BtsCache.addListener(this);
 
 		mGeo = App.getGeolocation();
 		mGeo.addReceiver(this);
 
 		mTelephony.listen(mListener, PhoneStateListener.LISTEN_CELL_LOCATION | PhoneStateListener.LISTEN_CELL_INFO | PhoneStateListener.LISTEN_DATA_ACTIVITY);
 	}
 
 	@Override
 	public void onPause() {
 		mTelephony.listen(mListener, PhoneStateListener.LISTEN_NONE);
 
 		mGeo.removeReceiver(this);
 
 		BtsCache.removeListener(this);
 		SectorCache.removeListener(this);
 
 		mMap.clear();
 		mMap = null;
 
 		super.onPause();
 	}
 
 	public void onBtsCacheChanged(Bts bts) {
 		addBts(bts);
 	}
 
 	public void onSectorCacheChanged(Sector sector) {
 		addSector(sector);
 	}
 
 	public void initializeMap() {
 		if (mMap == null) {
 			mMap = getMap();
 
 			if (mMap == null) {
 				return; // map is not yet available
 			}
 
 			UiSettings settings = mMap.getUiSettings();
 			settings.setCompassEnabled(true);
 			settings.setZoomControlsEnabled(false);
 			settings.setMyLocationButtonEnabled(false);
 
 			mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
 			mMap.setMyLocationEnabled(false);
 
 			if (mMap.getMaxZoomLevel() < mZoomDefault) {
 				mZoomDefault = mMap.getMaxZoomLevel();
 			}
 		}
 
 		final List<Sector> sectors = SectorCache.getAll();
		Log.d(Constants.TAG, "Loaded sector count: " + sectors.size());
 		for (Sector sector : sectors) {
 			addSector(sector);
 		}
 
 		final List<Bts> btses = BtsCache.getAll();
		Log.d(Constants.TAG, "Loaded BTS count: " + btses.size());
 		for (Bts bts : btses) {
 			addBts(bts);
 		}
 	}
 
 	public void onLocationChanged(Location location) {
 		mLastLocation = new LatLng(location.getLatitude(), location.getLongitude());
 
 		setMyMarker();
 		setConnection();
 	}
 
 	public void setMyMarker() {
 		if (mLastLocation == null) {
 			return;
 		}
 
 		// my current position
 		if (mMyMarker == null) {
 			final MarkerOptions markerOpts = new MarkerOptions();
 			markerOpts.position(mLastLocation);
 			markerOpts.icon(BitmapDescriptorFactory.fromResource(R.drawable.pin_my));
 			markerOpts.anchor(0.5f, 0.5f);
 
 			mMyMarker = mMap.addMarker(markerOpts);
 		} else {
 			mMyMarker.setPosition(mLastLocation);
 		}
 
 		if (!mCentered) {
 			mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(mLastLocation, mZoomDefault));
 
 			mCentered = true;
 		}
 	}
 
 	public void setConnection() {
 		if (mLastLocation == null || mLastBts == null || mLastBts.location == null) {
 			return;
 		}
 
 		// connection
 		if (mConnectionCurrent == null) {
 			final PolylineOptions polylineOpts = new PolylineOptions();
 			polylineOpts.width(getResources().getDimension(R.dimen.connection_width));
 			polylineOpts.color(getResources().getColor(R.color.connection_current));
 			polylineOpts.add(mLastBts.location);
 			polylineOpts.add(mLastLocation);
 
 			mConnectionCurrent = mMap.addPolyline(polylineOpts);
 		} else {
 			final List<LatLng> points = new ArrayList<LatLng>();
 			points.add(mLastBts.location);
 			points.add(mLastLocation);
 
 			mConnectionCurrent.setPoints(points);
 		}
 	}
 
 	private void setMapTransparent(ViewGroup group) {
 		int cnt = group.getChildCount();
 
 		for (int i = 0; i < cnt; i++) {
 			View child = group.getChildAt(i);
 
 			if (child instanceof ViewGroup) {
 				setMapTransparent((ViewGroup) child);
 			} else {
 				child.setBackgroundColor(0x00000000);
 			}
 		}
 	}
 
 	private void addBts(Bts bts) {
 		if (bts.location == null) {
 			return;
 		}
 
 		final String id = Bts.getId(bts);
 		final int level = Util.getNetworkLevel(bts.type);
 
 		int pinResource;
 		switch (level) {
 			case Constants.NET_LEVEL_2:
 				pinResource = R.drawable.pin_level_2;
 				break;
 			case Constants.NET_LEVEL_3:
 				pinResource = R.drawable.pin_level_3;
 				break;
 			case Constants.NET_LEVEL_4:
 				pinResource = R.drawable.pin_level_4;
 				break;
 			case Constants.NET_LEVEL_5:
 				pinResource = R.drawable.pin_level_5;
 				break;
 			default:
 				pinResource = R.drawable.pin_level_1;
 		}
 
 		if (mBtsMarkers.containsKey(id)) {
 			mBtsMarkers.get(id).remove();
 		}
 
 		final MarkerOptions markerOpts = new MarkerOptions();
 		markerOpts.position(bts.location);
 		markerOpts.icon(BitmapDescriptorFactory.fromResource(pinResource));
 		markerOpts.anchor(0.5f, 1.0f);
 
 		mBtsMarkers.put(id, mMap.addMarker(markerOpts));
 
 		setConnection();
 	}
 
 	private void addSector(Sector sector) {
 		final int level = Util.getNetworkLevel(sector.type);
 		final int fill = mFillColors[level - 1];
 
 		if (mCoveragePolygons.containsKey(sector.index)) {
 			mCoveragePolygons.get(sector.index).remove();
 		}
 
 		final PolygonOptions polygonOpts = new PolygonOptions();
 		polygonOpts.strokeWidth(getResources().getDimension(R.dimen.sector_margin));
 		polygonOpts.strokeColor(getResources().getColor(R.color.sector_border));
 		polygonOpts.fillColor(fill);
 		polygonOpts.addAll(sector.getCorners());
 
 		mCoveragePolygons.put(sector.index, mMap.addPolygon(polygonOpts));
 	}
 
 	public void getCurrentCellInfo() {
 		getCurrentCellInfo(null);
 	}
 
 	public void getCurrentCellInfo(CellLocation cell) {
 		final String operator = mTelephony.getNetworkOperator();
 		final int type = mTelephony.getNetworkType();
 
 		if (cell == null) {
 			cell = mTelephony.getCellLocation();
 		}
 
 		if (cell instanceof GsmCellLocation) {
 			final GsmCellLocation cellGsm = (GsmCellLocation) cell;
 
 			mLastBts = BtsCache.update(operator, cellGsm.getLac(), cellGsm.getCid(), type);
 
 			if (mLastBts != null && !mBtsMarkers.containsKey(Bts.getId(mLastBts))) {
 				addBts(mLastBts);
 			}
 		} else if (cell instanceof CdmaCellLocation) {
 			Log.w(Constants.TAG, "CDMA location not implemented");
 		}
 
 		setConnection();
 	}
 
 	// classes
 
 	public class StatusListener extends PhoneStateListener {
 
 		@Override
 		public void onCellLocationChanged(CellLocation cell) {
 			getCurrentCellInfo(cell);
 		}
 
 		@Override
 		public void onDataActivity(int direction) {
 			getCurrentCellInfo();
 		}
 
 		@Override
 		public void onCellInfoChanged(List<CellInfo> info) {
 			getCurrentCellInfo();
 		}
 	}
 }
