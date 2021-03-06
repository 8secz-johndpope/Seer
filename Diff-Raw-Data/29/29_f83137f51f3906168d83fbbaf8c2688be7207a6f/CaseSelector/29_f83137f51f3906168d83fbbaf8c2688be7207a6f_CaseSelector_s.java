 /**
  * 
  */
 package tw.edu.ntu.mgov;
 
 import java.util.Date;
 
 import com.google.android.maps.GeoPoint;
 import com.google.android.maps.MapActivity;
 import com.google.android.maps.MapView;
 
 import de.android1.overlaymanager.ManagedOverlay;
 import de.android1.overlaymanager.ManagedOverlayGestureDetector;
 import de.android1.overlaymanager.ManagedOverlayItem;
 import de.android1.overlaymanager.OverlayManager;
 import de.android1.overlaymanager.ZoomEvent;
 import de.android1.overlaymanager.MarkerRenderer;
 
 import tw.edu.ntu.mgov.caseviewer.CaseViewer;
 import tw.edu.ntu.mgov.gae.GAECase;
 import tw.edu.ntu.mgov.gae.GAEQuery;
 import tw.edu.ntu.mgov.option.Option;
 import tw.edu.ntu.mgov.typeselector.QidToDescription;
 import android.app.Service;
 import android.content.Context;
 import android.content.Intent;
 import android.graphics.drawable.Drawable;
 import android.location.Location;
 import android.location.LocationListener;
 import android.location.LocationManager;
 import android.os.Bundle;
 import android.os.Vibrator;
 import android.util.Log;
 import android.view.Gravity;
 import android.view.LayoutInflater;
 import android.view.Menu;
 import android.view.MenuItem;
 import android.view.MotionEvent;
 import android.view.View;
 import android.view.ViewGroup;
 import android.widget.AdapterView;
 import android.widget.BaseAdapter;
 import android.widget.ImageView;
 import android.widget.ListView;
 import android.widget.RelativeLayout;
 import android.widget.TextView;
 import android.widget.Toast;
 import android.widget.ZoomControls;
 import android.widget.RelativeLayout.LayoutParams;
 
 /**
  * @author sodas
  * 2010/10/4
  * @company NTU CSIE Mobile HCI Lab
  * 
  * This class is set for data source.
  * Unlike iPhone, Android do not have to put all views with same data source in view controller,
  * so there's no Hybrid activity.
  * 
  * We use another open-source tool:
  * mapview-overlay-manager, Extension for Overlayer in the Android Maps-API
  * http://code.google.com/p/mapview-overlay-manager/
  * 
  */
 public abstract class CaseSelector extends MapActivity {
 	protected Context selfContext = this;
 	// Constant Identifier for Menu
 	protected static final int MENU_Option = Menu.FIRST;
 	protected static final int MENU_ListMode = Menu.FIRST+1;
 	protected static final int MENU_MapMode = Menu.FIRST+2;
 	// Selector Mode
 	protected static enum CaseSelectorMode {
 		CaseSelectorListMode,
 		CaseSelectorMapMode
 	}
 	public CaseSelectorMode currentMode;
 	// Default Mode is List Mode
 	protected CaseSelectorMode defaultMode = CaseSelectorMode.CaseSelectorListMode;
 	protected Boolean noCaseImageWillShow = true;
 	// Views
 	protected RelativeLayout infoBar;
 	protected ListView listMode;
 	protected MapView mapMode;
 	protected ZoomControls mapModeZoomControl;
 	public OverlayManager overlayManager;
 	protected GeoPoint currentLocationPoint;
 	protected ManagedOverlay managedOverlay;
 	// Query Google App Engine
 	protected GAEQuery qGAE;
 	protected GAECase caseSource[];
 	
 	
 	@Override
 	protected void onCreate(Bundle savedInstanceState) {
 		super.onCreate(savedInstanceState);
 		setContentView(R.layout.caseselector);
 		// Call Info bar from Layout XML
 		infoBar = (RelativeLayout)findViewById(R.id.infoBar);
 		// Call List View From Layout XML
 		listMode = (ListView)findViewById(R.id.listMode);
 		listMode.setAdapter(new caseListAdapter(this));
 		listMode.setOnItemClickListener(new AdapterView.OnItemClickListener() {
 
 			@Override
 			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
 				if (caseSource!=null) {
 					Intent caseViewerIntent = new Intent().setClass(selfContext, CaseViewer.class);
 					caseViewerIntent.putExtra("caseID", caseSource[position].getform("key"));
 					startActivity(caseViewerIntent);
 				}
 			}
 		});
 		// New Map by Java code for separate maps.
 		mapMode = new MapView(this, getResources().getString(R.string.google_mapview_api_key));
 		LayoutParams param1 = new RelativeLayout.LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT);
 		param1.addRule(RelativeLayout.ALIGN_PARENT_LEFT, RelativeLayout.ALIGN_PARENT_TOP);
 		RelativeLayout mapModeFrame = (RelativeLayout)findViewById(R.id.mapModeFrame);
 		mapModeFrame.addView(mapMode, 0, param1);
 		// Set Map
 		mapMode.setEnabled(true);
 		mapMode.setClickable(true);
 		mapMode.preLoad();
 		mapMode.setBuiltInZoomControls(false); // Use custom Map Control instead
 		mapMode.getController().setZoom(17);
 		// Call Zoom Controll for Map Mode, since we want to show it automaticlly
 		mapModeZoomControl = (ZoomControls)findViewById(R.id.mapModeZoomControl);
 		mapModeZoomControl.setOnZoomInClickListener(new View.OnClickListener() {
             @Override
             public void onClick(View v) { mapMode.getController().zoomIn(); }
 		});
 		mapModeZoomControl.setOnZoomOutClickListener(new View.OnClickListener() {
             @Override
             public void onClick(View v) { mapMode.getController().zoomOut(); }
 		});
 		// Get Current User Location
 		LocationManager locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
 		LocationListener locationListener = new LocationListener() {
 			@Override
 			public void onLocationChanged(Location location) {
 				currentLocationPoint = new GeoPoint((int)(location.getLatitude()*Math.pow(10, 6)), (int)(location.getLongitude()*Math.pow(10, 6)));
 				mapMode.getController().animateTo(currentLocationPoint);
 			}
 			@Override
 			public void onProviderDisabled(String provider) {}
 			@Override
 			public void onProviderEnabled(String provider) {}
 			@Override
 			public void onStatusChanged(String provider, int status, Bundle extras) {}
 		};
 		locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, locationListener);
 		Location lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
 		if (lastKnownLocation!=null) {
 			currentLocationPoint = new GeoPoint((int)(lastKnownLocation.getLatitude()*Math.pow(10, 6)), (int)(lastKnownLocation.getLongitude()*Math.pow(10, 6)));
 			mapMode.getController().animateTo(currentLocationPoint);
 		}
 		// Change to Default Mode
 		if (defaultMode==CaseSelectorMode.CaseSelectorListMode) {
 			currentMode = CaseSelectorMode.CaseSelectorListMode;
 			findViewById(R.id.mapModeFrame).setVisibility(View.GONE);
 			findViewById(R.id.listModeFrame).setVisibility(View.VISIBLE);
 		} else {
 			currentMode = CaseSelectorMode.CaseSelectorMapMode;
 			findViewById(R.id.listModeFrame).setVisibility(View.GONE);
 			findViewById(R.id.mapModeFrame).setVisibility(View.VISIBLE);
 		}
 		locationManager.removeUpdates(locationListener);
 		locationListener = null;
 		
 		if (currentMode==CaseSelectorMode.CaseSelectorMapMode)
 			createOverlayWithListener();
 	}
 	
 	protected void changCaseSelectorMode(CaseSelectorMode targetMode) {
 		if (targetMode == CaseSelectorMode.CaseSelectorListMode) {
 			findViewById(R.id.mapModeFrame).setVisibility(View.GONE);
 			findViewById(R.id.listModeFrame).setVisibility(View.VISIBLE);
 		} else {
 			findViewById(R.id.mapModeFrame).setVisibility(View.VISIBLE);
 			findViewById(R.id.listModeFrame).setVisibility(View.GONE);
 		}
 		currentMode = targetMode;
 	}
 	
 	/**
 	 * @category Menu
 	 * 
 	 */
 	@Override
 	public boolean onCreateOptionsMenu(Menu menu) {
 		menu.add(0, MENU_Option, 0, getResources().getString(R.string.menu_option)).setIcon(android.R.drawable.ic_menu_preferences);
		menu.add(0, MENU_ListMode, 0, getResources().getString(R.string.menu_ListMode)).setIcon(android.R.drawable.ic_menu_info_details);
 		menu.add(0, MENU_MapMode, 0, getResources().getString(R.string.menu_mapMode)).setIcon(android.R.drawable.ic_menu_mapmode);
 		return super.onCreateOptionsMenu(menu);
 	}
 	
 	@Override
 	public boolean onOptionsItemSelected(MenuItem item) {
 		switch(item.getItemId()) {
 			case MENU_ListMode:
 				// Change Mode
 				this.changCaseSelectorMode(CaseSelectorMode.CaseSelectorListMode);
 				break;
 			case MENU_MapMode:
 				// Change Mode
 				this.changCaseSelectorMode(CaseSelectorMode.CaseSelectorMapMode);
 				break;
 			case MENU_Option:
 				// Go to Option Activity
 				Intent intent = new Intent();
 				intent.setClass(this, Option.class);
 				startActivity(intent);
 				break;
 		}
 		return super.onOptionsItemSelected(item);
 	}
 	
 	// Set action for new action defined by subclasses.
 	public abstract void menuActionToTake(MenuItem item);
 	
 	/**
 	 * @category method 
 	 *
 	 */
 	protected int convertStatusStringToStatusCode(String status) {
 		if (status.equals("完工")||status.equals("結案")||status.equals("轉府外單位")) return 1;
 		else if (status.equals("無法辦理")||status.equals("退回區公所")||status.equals("查驗未通過")) return 2;
 		else return 0;
 	}
 	
 	/**
 	 * @category Custom List 
 	 *
 	 */
 	class ListCellContainer {
 		TextView caseID;
 		TextView caseType;
 		TextView caseAddress;
 		TextView caseDate;
 		ImageView caseStatus;
 	}
 	
 	protected class caseListAdapter extends BaseAdapter {
 		LayoutInflater myInflater;
 		// Constructor
 		public caseListAdapter(Context c) {
 			myInflater = LayoutInflater.from(c);
 		}
 		
 		@Override
 		public int getCount() {
 			// TODO Return Data Count
 			//return stringData.length;
 			ImageView noCaseImage;
 			noCaseImage = (ImageView) findViewById(R.id.CaseSelector_NoCaseImage);
 			if (caseSource == null) {
 				if (noCaseImageWillShow == true) noCaseImage.setVisibility(View.VISIBLE);
 				else noCaseImage.setVisibility(View.INVISIBLE);
 				return 0;
 			} else {
 				return caseSource.length;
 			}
 		}
 
 		@Override
 		public Object getItem(int position) {
 			// Get the data item associated with the specified position in the data set.
 			Log.d("List", "getItem:"+Integer.toString(position));
 			return position;
 		}
 
 		@Override
 		public long getItemId(int position) {
 			// Get the row id associated with the specified position in the list.
 			Log.d("List", "getItemId:"+Integer.toString(position));
 			return position;
 		}
 
 		@Override
 		public View getView(int position, View convertView, ViewGroup parent) {
 			Log.d("List", "getView:"+Integer.toString(position));
 			// Get a View that displays the data at the specified position in the data set.
 			ListCellContainer cellContent = new ListCellContainer();
 			// Reuse Cell
 			if (convertView==null) {
 				convertView = myInflater.inflate(R.layout.listcell, parent, false);
 				// Mapping To XML
 				cellContent.caseID = (TextView)convertView.findViewById(R.id.listCell_CaseID);
 				cellContent.caseType = (TextView)convertView.findViewById(R.id.listCell_CaseType);
 				cellContent.caseAddress = (TextView)convertView.findViewById(R.id.listCell_CaseAddress);
 				cellContent.caseDate = (TextView)convertView.findViewById(R.id.listCell_CaseDate);
 				cellContent.caseStatus = (ImageView)convertView.findViewById(R.id.listCell_StatusImage);
 				convertView.setTag(cellContent);
 			} else {
 				cellContent = (ListCellContainer)convertView.getTag();
 			}
 			// Set Cell Content
 			cellContent.caseStatus.setImageResource(R.drawable.ok);
 			if (caseSource!=null) {
 				if (convertStatusStringToStatusCode(caseSource[position].getform("status"))==1)
 					cellContent.caseStatus.setImageResource(R.drawable.ok);
 				else if (convertStatusStringToStatusCode(caseSource[position].getform("status"))==2)
 					cellContent.caseStatus.setImageResource(R.drawable.fail);
 				else
 					cellContent.caseStatus.setImageResource(R.drawable.unknown);
 				cellContent.caseID.setText(caseSource[position].getform("key"));
 				cellContent.caseType.setText(QidToDescription.getDetailByQID(selfContext, Integer.parseInt(caseSource[position].getform("typeid"))));
 
 				String d = caseSource[position].getform("date");
 				String tmp[]=d.split("年|月|日");
 				Date tmpdate = new Date(Integer.parseInt(tmp[0])+11,Integer.parseInt(tmp[1])-1,Integer.parseInt(tmp[2]));
 				Date nowdate = new Date();
 				nowdate = new Date(nowdate.getYear(),nowdate.getMonth(),nowdate.getDate());
 				long timeInterval= nowdate.getTime()-tmpdate.getTime();
 				if (timeInterval < 86400*1000) cellContent.caseDate.setText("今天");
 				else if (timeInterval < 2*86400*1000) cellContent.caseDate.setText("昨天");
 				else if (timeInterval < 3*86400*1000) cellContent.caseDate.setText("兩天前");
 				else cellContent.caseDate.setText(tmp[0]+"/"+tmp[1]+"/"+tmp[2]);
 				cellContent.caseAddress.setText(caseSource[position].getform("address"));
 			}
 			return convertView;
 		}
 	}
 	
 	/**
 	 * @category MapActivity and Overlay
 	 * 
 	 */
 	protected abstract void mapChangeRegionOrZoom();
 	@Override
 	protected boolean isRouteDisplayed() {
 		// We do not use route service, so return false.
 		return false;
 	}
 	
 	public void createOverlayWithListener() {
 		//This time we use our own marker
		if (overlayManager==null) {
			Log.d("mapMode", "H");
 			overlayManager = new OverlayManager(getApplication(), mapMode);
		}
 		managedOverlay = overlayManager.createOverlay("listenerOverlay");
 		managedOverlay.setOnOverlayGestureListener(new ManagedOverlayGestureDetector.OnOverlayGestureListener() {
 			@Override
 			public boolean onDoubleTap(MotionEvent me, ManagedOverlay overlay, GeoPoint point, ManagedOverlayItem item) {
 				try {
 					mapMode.getController().animateTo(point);
 					if (mapMode.getZoomLevel()+1 < mapMode.getMaxZoomLevel())
 						mapMode.getController().setZoom(mapMode.getZoomLevel()+1);
 				} catch (Exception e) {
 					e.printStackTrace();
 				}
 				return true;
 			}
 			@Override
 			public boolean onZoom(ZoomEvent zoom, ManagedOverlay overlay) {
 				mapChangeRegionOrZoom();
 				return false;
 			}
 			@Override
 			public void onLongPress(MotionEvent e, ManagedOverlay overlay) {
 				if (e.getAction()==MotionEvent.ACTION_UP) {
 					Vibrator myVibrator = (Vibrator) getApplication().getSystemService(Service.VIBRATOR_SERVICE);
 					myVibrator.vibrate(100);
 				}
 			}
 			@Override
 			public void onLongPressFinished(MotionEvent e, ManagedOverlay overlay, GeoPoint point, ManagedOverlayItem item) {
 				if (item!=null) {
 					String overlayInfo[] = item.getSnippet().split(",");
 					// Show Case data
 					Intent caseViewerIntent = new Intent().setClass(selfContext, CaseViewer.class);
 					Bundle bundle = new Bundle();
 					bundle.putString("caseID", overlayInfo[0]);
 					caseViewerIntent.putExtras(bundle);
 					startActivity(caseViewerIntent);
 					caseViewerIntent = null;
 				}
 			}
 			@Override
 			public boolean onScrolled(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY, ManagedOverlay overlay) {
 				mapChangeRegionOrZoom();
 				return false;
 			}
 			@Override
 			public boolean onSingleTap(MotionEvent e, ManagedOverlay overlay, GeoPoint point, ManagedOverlayItem item) {
 				if (item!=null) {
 					// This is Not a Map Event
 					// Custom Toast
 					LayoutInflater inflater = getLayoutInflater();
 					View layout = inflater.inflate(R.layout.case_selector_toast, (ViewGroup)findViewById(R.id.caseSelector_toast));
 					TextView title = (TextView)layout.findViewById(R.id.caseSelector_toast_Title);
 					ImageView status = (ImageView)layout.findViewById(R.id.caseSelector_toast_Status);
 					title.setText(item.getTitle());
 					status.setImageResource(R.drawable.ok);
 					// Add toast
 					Toast toast = new Toast(getApplicationContext());
 					toast.setGravity(Gravity.BOTTOM|Gravity.CENTER, 0, 60);
 					toast.setDuration(Toast.LENGTH_SHORT);
 					toast.setView(layout);
 					toast.show();
 					toast = null;
 				}
 				return false; 
 			}
 		});
 		overlayManager.populate();
 		
 		managedOverlay.setCustomMarkerRenderer(new MarkerRenderer() {
 		    @Override
 			public Drawable render(ManagedOverlayItem item, Drawable defaultMarker, int bitState) {
 		    	// Current Location
 		    	if (item.getTitle()==getResources().getString(R.string.mapOverlay_currentLocation)) {
 		    		Drawable currentLocationMarker = getResources().getDrawable(R.drawable.mapoverlay_current_location);
 		    		int currentLocationMarkerHalfWidth = currentLocationMarker.getIntrinsicWidth()/2;
 		    		int currentLocationMarkerHalfHeight = currentLocationMarker.getIntrinsicHeight()/2;
 		    		currentLocationMarker.setBounds(-currentLocationMarkerHalfWidth, -currentLocationMarkerHalfHeight, currentLocationMarkerHalfWidth, currentLocationMarkerHalfHeight);
 		    		return currentLocationMarker;
 		    	}
 		    	// Case
 		    	Drawable marker;
 		    	String overlayInfo[] = item.getSnippet().split(",");
 		    	if (Integer.parseInt(overlayInfo[1])==1)
 		    		marker = getResources().getDrawable(R.drawable.mapoverlay_greenpin);
 		    	else if (Integer.parseInt(overlayInfo[1])==2)
 		    		marker = getResources().getDrawable(R.drawable.mapoverlay_redpin);	
 		    	else 
 		    		marker = getResources().getDrawable(R.drawable.mapoverlay_orangepin);
 		    	
 		    	marker.setBounds((int)(-marker.getIntrinsicWidth()*0.25), -marker.getIntrinsicHeight(), marker.getIntrinsicWidth()-(int)(marker.getIntrinsicWidth()*0.25), 0);
 		    	return marker;
 			}
 		});
 	}
 }
