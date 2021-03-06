 package com.pwr.zpi;
 
 import java.util.ArrayList;
 import java.util.List;
 
 import android.app.ProgressDialog;
 import android.content.ComponentName;
 import android.content.DialogInterface;
 import android.content.Intent;
 import android.content.ServiceConnection;
 import android.graphics.Color;
 import android.location.Location;
 import android.os.Bundle;
 import android.os.Handler;
 import android.os.IBinder;
 import android.os.RemoteException;
 import android.preference.PreferenceManager;
 import android.provider.MediaStore;
 import android.support.v4.app.FragmentActivity;
 import android.support.v4.widget.DrawerLayout;
 import android.support.v4.widget.DrawerLayout.DrawerListener;
 import android.util.Log;
 import android.util.TypedValue;
 import android.view.Gravity;
 import android.view.KeyEvent;
 import android.view.View;
 import android.view.View.OnClickListener;
 import android.widget.Button;
 import android.widget.ImageButton;
 import android.widget.LinearLayout;
 import android.widget.ListView;
 import android.widget.RelativeLayout;
 import android.widget.TextView;
 
 import com.google.android.gms.maps.CameraUpdateFactory;
 import com.google.android.gms.maps.GoogleMap;
 import com.google.android.gms.maps.SupportMapFragment;
 import com.google.android.gms.maps.model.CameraPosition;
 import com.google.android.gms.maps.model.CameraPosition.Builder;
 import com.google.android.gms.maps.model.LatLng;
 import com.google.android.gms.maps.model.Marker;
 import com.google.android.gms.maps.model.MarkerOptions;
 import com.google.android.gms.maps.model.Polyline;
 import com.google.android.gms.maps.model.PolylineOptions;
 import com.pwr.zpi.adapters.DrawerWorkoutsAdapter;
 import com.pwr.zpi.database.entity.Workout;
 import com.pwr.zpi.database.entity.WorkoutAction;
 import com.pwr.zpi.dialogs.MyDialog;
 import com.pwr.zpi.listeners.OnNextActionListener;
 import com.pwr.zpi.services.LocationService;
 import com.pwr.zpi.utils.BeepPlayer;
 import com.pwr.zpi.utils.TimeFormatter;
 
 public class ActivityActivity extends FragmentActivity implements OnClickListener {
 	
 	private static final float MIN_SPEED_FOR_AUTO_PAUSE = 0.3f;
 	
 	private GoogleMap mMap;
 	
 	private Button stopButton;
 	private Button pauseButton;
 	private Button resumeButton;
 	private ImageButton musicPlayer;
 	private ImageButton workoutDdrawerButton;
 	private TextView DataTextView1;
 	private TextView DataTextView2;
 	private TextView clickedContentTextView;
 	private TextView LabelTextView1;
 	private TextView LabelTextView2;
 	private TextView clickedLabelTextView;
 	private TextView unitTextView1;
 	private TextView unitTextView2;
 	private TextView clickedUnitTextView;
 	private TextView GPSAccuracy;
 	private TextView countDownTextView;
 	private LinearLayout startStopLayout;
 	private RelativeLayout dataRelativeLayout1;
 	private RelativeLayout dataRelativeLayout2;
 	private Location mLastLocation;
 	private boolean isPaused;
 	//private SingleRun singleRun;
 	//private LinkedList<LinkedList<Pair<Location, Long>>> traceWithTime;
 	//private Calendar calendar;
 	private PolylineOptions traceOnMap;
 	private Polyline traceOnMapObject;
 	private static final float traceThickness = 5;
 	private static final int traceColor = Color.RED;
 	
 	// measured values
 	double pace;
 	double avgPace;
 	double distance;
 	double lastDistance;
 	Long time = 0L;
 	long startTime;
 	long pauseTime;
 	long pauseStartTime;
 	
 	private int dataTextView1Content;
 	private int dataTextView2Content;
 	private int clickedField;
 	// measured values IDs
 	private static final int distanceID = 0;
 	private static final int paceID = 1;
 	private static final int avgPaceID = 2;
 	private static final int timeID = 3;
 	
 	// service data
 	boolean mIsBound;
 	boolean isServiceConnected;
 	boolean canStart;
 	private RunListenerApi api;
 	private Handler handlerForService;
 	
 	// time counting fields
 	private Handler handler;
 	//	private Runnable timeHandler;
 	private static final int COUNT_DOWN_TIME = 5;
 	private static final String TAG = ActivityActivity.class.getSimpleName();
 	BeepPlayer beepPlayer;
 	
 	// progress dialog lost gps
 	private ProgressDialog lostGPSDialog;
 	
 	// workout drawer fields
 	private DrawerWorkoutsAdapter drawerListAdapter;
 	private ListView listView;
 	private Workout workout;
 	private DrawerLayout drawerLayout;
 	
 	@Override
 	protected void onCreate(Bundle savedInstanceState) {
 		super.onCreate(savedInstanceState);
 		setContentView(R.layout.activity_view);
 		
 		initFields();
 		addListeners();
 		
 		initDisplayedData();
 		
 		prepareServiceAndStart();
 		
 	}
 	
 	private Workout getWorkoutData() {
 		Intent i = getIntent();
 		
 		Workout workout;
 		workout = i.getParcelableExtra(Workout.TAG);
 		return workout;
 	}
 	
 	private void initFields() {
 		stopButton = (Button) findViewById(R.id.stopButton);
 		pauseButton = (Button) findViewById(R.id.pauseButton);
 		resumeButton = (Button) findViewById(R.id.resumeButton);
 		musicPlayer = (ImageButton) findViewById(R.id.buttonMusicDuringActivity);
 		workoutDdrawerButton = (ImageButton) findViewById(R.id.imageButtonWorkoutDrawerButton);
 		dataRelativeLayout1 = (RelativeLayout) findViewById(R.id.dataRelativeLayout1);
 		dataRelativeLayout2 = (RelativeLayout) findViewById(R.id.dataRelativeLayout2);
 		GPSAccuracy = (TextView) findViewById(R.id.TextViewGPSAccuracy);
 		countDownTextView = (TextView) findViewById(R.id.textViewCountDown);
 		startStopLayout = (LinearLayout) findViewById(R.id.startStopLinearLayout);
 		SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
 		mMap = mapFragment.getMap();
 		//mMap.setMyLocationEnabled(true);
 		
 		//		traceWithTime = new LinkedList<LinkedList<Pair<Location, Long>>>();
 		//		pauseTime = 0;
 		traceOnMap = new PolylineOptions();
 		traceOnMap.width(traceThickness);
 		traceOnMap.color(traceColor);
 		traceOnMapObject = mMap.addPolyline(traceOnMap);
 		
 		DataTextView1 = (TextView) findViewById(R.id.dataTextView1);
 		DataTextView2 = (TextView) findViewById(R.id.dataTextView2);
 		
 		LabelTextView1 = (TextView) findViewById(R.id.dataTextView1Discription);
 		LabelTextView2 = (TextView) findViewById(R.id.dataTextView2Discription);
 		
 		unitTextView1 = (TextView) findViewById(R.id.dataTextView1Unit);
 		unitTextView2 = (TextView) findViewById(R.id.dataTextView2Unit);
 		
 		// to change displayed info, change dataTextViewContent and start
 		// initLabelsMethod
 		dataTextView1Content = distanceID;
 		dataTextView2Content = timeID;
 		
 		// make single run object
 		//	singleRun = new SingleRun();
 		//	calendar = Calendar.getInstance();
 		
 		//	singleRun.setStartDate(calendar.getTime());
 		isPaused = false;
 		canStart = false;
 		
 		beepPlayer = new BeepPlayer(this);
 		
 		Intent intent = getIntent();
 		listView = (ListView) findViewById(R.id.left_drawer);
 		drawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
		if (intent.hasExtra(Workout.TAG)) {
 			// drawer initialization
 			listView.addHeaderView(getLayoutInflater().inflate(R.layout.workout_drawer_list_header, null));
 			workout = getWorkoutData();
 			List<WorkoutAction> actions = new ArrayList<WorkoutAction>();
 			for (int i = 0; i < workout.getRepeatCount(); i++) {
 				actions.addAll(workout.getActions());
 			}
 			workout.setActions(actions);
 			drawerListAdapter = new DrawerWorkoutsAdapter(this, R.layout.workout_drawer_list_item,
 				workout.getActions(), workout);
 			listView.setAdapter(drawerListAdapter);
 			listView.setVisibility(View.VISIBLE);
 		}
 		else {
 			workoutDdrawerButton.setVisibility(View.GONE);
 			listView.setVisibility(View.GONE);
 		}
 		
 		moveSystemControls(mapFragment);
 	}
 	
 	private void addListeners() {
 		stopButton.setOnClickListener(this);
 		resumeButton.setOnClickListener(this);
 		pauseButton.setOnClickListener(this);
 		
 		dataRelativeLayout1.setOnClickListener(this);
 		dataRelativeLayout2.setOnClickListener(this);
 		
 		musicPlayer.setOnClickListener(this);
 		
 		if (workout != null) {
 			workoutDdrawerButton.setOnClickListener(this);
 			
 			drawerLayout.setDrawerListener(new DrawerListener() {
 				
 				@Override
 				public void onDrawerStateChanged(int arg0) {}
 				
 				@Override
 				public void onDrawerSlide(View arg0, float arg1) {}
 				
 				@Override
 				public void onDrawerOpened(View arg0) {
 					if (workout == null) {
 						drawerLayout.closeDrawer(Gravity.LEFT);
 					}
 					else {
 						listView.smoothScrollToPosition(workout.getCurrentAction() + 4, workout.getActions()
 							.size());
 					}
 				}
 				
 				@Override
 				public void onDrawerClosed(View arg0) {}
 			});
 			
 			workout.setOnNextActionListener(new OnNextActionListener());
 		}
 	}
 	
 	private void initDisplayedData() {
 		GPSAccuracy.setText(getMyString(R.string.gps_accuracy) + " ?");
 		
 		initLabels(DataTextView1, LabelTextView1, dataTextView1Content);
 		initLabels(DataTextView2, LabelTextView2, dataTextView2Content);
 	}
 	
 	private void prepareServiceAndStart() {
 		doBindService();
 		
 		handlerForService = new Handler();
 	}
 	
 	@Override
 	protected void onDestroy() {
 		doUnbindService();
 		
 		super.onDestroy();
 	}
 	
 	//TODO set pause and stop clickable
 	
 	private void startCountDown()
 	{
 		handler = new Handler();
 		if (PreferenceManager.getDefaultSharedPreferences(this).getBoolean(
 			getString(R.string.key_countdown_before_start), true)) {
 			handler.post(new CounterRunnable(COUNT_DOWN_TIME));
 		}
 		else {
 			canStart = true;
 		}
 	}
 	
 	private void startRecording()
 	{
 		pauseButton.setClickable(true);
 		countDownTextView.setVisibility(View.GONE);
 		if (isServiceConnected)
 		{
 			try {
 				api.setStarted(workout);
 			}
 			catch (RemoteException e) {
 				Log.e(TAG, "Failed to start activity", e);
 			}
 		}
 	}
 	
 	private class CounterRunnable implements Runnable {
 		
 		final int x;
 		
 		public CounterRunnable(int x) {
 			this.x = x;
 		}
 		
 		@Override
 		public void run() {
 			runOnUiThread(new Runnable() {
 				
 				@Override
 				public void run() {
 					if (x == 0) {
 						canStart = true;
 						startRecording();
 					}
 					else {
 						countDownTextView.setText(x + "");
 						beepPlayer.playBeep();
 						handler.postDelayed(new CounterRunnable(x - 1), 1000);
 					}
 				}
 			});
 		}
 	}
 	
 	// end of timer methods
 	//TODO przesun\B9\E6 \BFeby nie by\B3y pod innymi ikonami
 	private void moveSystemControls(SupportMapFragment mapFragment) {
 		
 		View zoomControls = mapFragment.getView().findViewById(0x1);
 		
 		if (zoomControls != null && zoomControls.getLayoutParams() instanceof RelativeLayout.LayoutParams) {
 			// ZoomControl is inside of RelativeLayout
 			RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) zoomControls.getLayoutParams();
 			
 			// Align it to - parent top|left
 			params.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
 			params.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
 			
 			// nie do ko�ca rozumiem t� metod�, trzeba zobaczy� czy u
 			// Ciebie
 			// jest to samo czy nie za bardzo
 			final int margin = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, getResources()
 				.getDimension(R.dimen.zoom_buttons_margin), getResources().getDisplayMetrics());
 			params.setMargins(0, 0, 0, margin);
 		}
 		View locationControls = mapFragment.getView().findViewById(0x2);
 		
 		if (locationControls != null && locationControls.getLayoutParams() instanceof RelativeLayout.LayoutParams) {
 			// ZoomControl is inside of RelativeLayout
 			RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) locationControls.getLayoutParams();
 			
 			// Align it to - parent top|left
 			params.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
 			
 			// Update margins, set to 10dp
 			final int margin1 = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, getResources()
 				.getDimension(R.dimen.location_button_margin_top), getResources().getDisplayMetrics());
 			final int margin2 = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, getResources()
 				.getDimension(R.dimen.location_button_margin_right), getResources().getDisplayMetrics());
 			params.setMargins(0, margin1, margin2, 0);
 		}
 	}
 	
 	private void initLabels(TextView textViewInitialValue, TextView textView, int meassuredValue) {
 		switch (meassuredValue) {
 			case distanceID:
 				textView.setText(R.string.distance);
 				textViewInitialValue.setText("0.000");
 				break;
 			case paceID:
 				textView.setText(R.string.pace);
 				textViewInitialValue.setText("0:00");
 				break;
 			case avgPaceID:
 				textView.setText(R.string.pace_avrage);
 				textViewInitialValue.setText("0:00");
 				break;
 			case timeID:
 				textView.setText(R.string.time);
 				textViewInitialValue.setText("00:00:00");
 				break;
 		}
 		
 	}
 	
 	private void updateLabels(int meassuredValue, TextView labelTextView, TextView unitTextView,
 		TextView contentTextView) {
 		switch (meassuredValue) {
 			case distanceID:
 				labelTextView.setText(R.string.distance);
 				unitTextView.setText(R.string.km);
 				break;
 			case paceID:
 				labelTextView.setText(R.string.pace);
 				unitTextView.setText(R.string.minutes_per_km);
 				break;
 			case avgPaceID:
 				labelTextView.setText(R.string.pace_avrage);
 				unitTextView.setText(R.string.minutes_per_km);
 				break;
 			case timeID:
 				labelTextView.setText(R.string.time);
 				unitTextView.setText(R.string.empty_string);
 				break;
 		}
 		
 		updateData(contentTextView, meassuredValue);
 	}
 	
 	@Override
 	public void onBackPressed() {
 		super.onBackPressed();
 		showAlertDialog();
 	}
 	
 	private void showAlertDialog() {
 		MyDialog dialog = new MyDialog();
 		DialogInterface.OnClickListener positiveButtonHandler = new DialogInterface.OnClickListener() {
 			@Override
 			public void onClick(DialogInterface dialog, int id) {
 				
 				try {
 					if (isServiceConnected) {
 						api.setStoped();
 					}
 				}
 				catch (RemoteException e) {
 					Log.e(TAG, "Failed to tell that activity is stoped", e);
 				}
 				finish();
 				overridePendingTransition(R.anim.in_up_anim, R.anim.out_up_anim);
 			}
 		};
 		dialog.showAlertDialog(this, R.string.dialog_message_on_stop, R.string.empty_string, android.R.string.yes,
 			android.R.string.no, positiveButtonHandler, null);
 	}
 	
 	private void showLostGpsSignalDialog() {
 		handlerForService.post(new Runnable() {
 			@Override
 			public void run() {
 				if (isServiceConnected)
 				{
 					
 					lostGPSDialog = ProgressDialog.show(ActivityActivity.this, getResources()
 						.getString(R.string.dialog_message_on_lost_gpsp), null); // TODO strings
 					lostGPSDialog.setCancelable(true);
 					
 				}
 			}
 		});
 	}
 	
 	@Override
 	public void onClick(View v) {
 		
 		switch (v.getId()) {
 			case R.id.stopButton:
 				showAlertDialog();
 				break;
 			case R.id.pauseButton:
 				pauseRun();
 				break;
 			case R.id.resumeButton:
 				resumeRun();
 				break;
 			case R.id.dataRelativeLayout1:
 				clickedContentTextView = DataTextView1;
 				clickedLabelTextView = LabelTextView1;
 				clickedUnitTextView = unitTextView1;
 				clickedField = 1;
 				showMeassuredValuesMenu();
 				break;
 			case R.id.dataRelativeLayout2:
 				clickedContentTextView = DataTextView2;
 				clickedLabelTextView = LabelTextView2;
 				clickedUnitTextView = unitTextView2;
 				clickedField = 2;
 				showMeassuredValuesMenu();
 				break;
 			case R.id.imageButtonWorkoutDrawerButton:
 				boolean isOpen = drawerLayout.isDrawerOpen(Gravity.LEFT);
 				if (!isOpen) {
 					drawerLayout.openDrawer(Gravity.LEFT);
 				}
 				else {
 					drawerLayout.closeDrawer(Gravity.LEFT);
 				}
 				break;
 			case R.id.buttonMusicDuringActivity:
 				startSystemMusicPlayer();
 				break;
 		}
 		
 	}
 	
 	private void pauseRun() {
 		handlerForService.post(new Runnable() {
 			@Override
 			public void run() {
 				if (!isPaused) {
 					isPaused = true;
 					startStopLayout.setVisibility(View.INVISIBLE);
 					resumeButton.setVisibility(View.VISIBLE);
 					try {
 						if (isServiceConnected) {
 							api.setPaused();
 						}
 					}
 					catch (RemoteException e) {
 						Log.e(TAG, "Failed to tell that activity is paused", e);
 					}
 				}
 			}
 		});
 	}
 	
 	private void resumeRun() {
 		handlerForService.post(new Runnable() {
 			@Override
 			public void run() {
 				if (isPaused) {
 					isPaused = false;
 					startStopLayout.setVisibility(View.VISIBLE);
 					resumeButton.setVisibility(View.GONE);
 					try {
 						if (isServiceConnected) {
 							api.setResumed();
 						}
 					}
 					catch (RemoteException e) {
 						Log.e(TAG, "Failed to tell that activity is resumed", e);
 					}
 				}
 			}
 		});
 	}
 	
 	private String getMyString(int stringId) {
 		return getResources().getString(stringId);
 	}
 	
 	private void showMeassuredValuesMenu() {
 		// chcia�em zrobi� tablice w stringach, ale potem zobaczy�em, �e
 		// ju� mam
 		// te wszystkie nazwy i teraz nie wiem czy tamto zmienia� w tablic�
 		// czy
 		// nie ma sensu
 		// kolejno�� w tablicy musi odpowiada� nr ID, tzn 0 - dystans itp.
 		
 		final CharSequence[] items = { getMyString(R.string.distance), getMyString(R.string.pace),
 			getMyString(R.string.pace_avrage), getMyString(R.string.time) };
 		MyDialog dialog = new MyDialog();
 		DialogInterface.OnClickListener itemsHandler = new DialogInterface.OnClickListener() {
 			@Override
 			public void onClick(DialogInterface dialog, int item) {
 				updateLabels(item, clickedLabelTextView, clickedUnitTextView, clickedContentTextView);
 				if (clickedField == 1) {
 					dataTextView1Content = item;
 				}
 				else {
 					dataTextView2Content = item;
 				}
 			}
 		};
 		dialog.showAlertDialog(this, R.string.dialog_choose_what_to_display, R.string.empty_string,
 			R.string.empty_string, R.string.empty_string, null, null, items, itemsHandler);
 	}
 	
 	// update display
 	protected void updateData(final TextView textBox, final int meassuredValue) {
 		
 		switch (meassuredValue) {
 			case distanceID:
 				textBox.setText(String.format("%.3f", distance / 1000));
 				break;
 			case paceID:
 				if (pace < 30) {
 					textBox.setText(TimeFormatter.formatTimeMMSSorHHMMSS(pace));
 				}
 				else {
 					textBox.setText(getResources().getString(R.string.dashes));
 				}
 				break;
 			case avgPaceID:
 				if (avgPace < 30) {
 					textBox.setText(TimeFormatter.formatTimeMMSSorHHMMSS(avgPace));
 				}
 				else {
 					textBox.setText(getResources().getString(R.string.dashes));
 				}
 				break;
 			case timeID:
 				textBox.setText(TimeFormatter.formatTimeHHMMSS(time));
 				break;
 		}
 		
 	}
 	
 	// count everything with 2 last location points
 	private void countData(final Location location, final Location lastLocation) {
 		
 		Log.i("ActivityActivity", "countData: " + location);
 		handlerForService.post(new Runnable() {
 			@Override
 			public void run() {
 				LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
 				
 				traceOnMap.add(latLng);
 				traceOnMapObject.setPoints(traceOnMap.getPoints());
 				
 				CameraPosition cameraPosition = buildCameraPosition(latLng, location, lastLocation);
 				mMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
 				
 				// mMap.animateCamera(CameraUpdateFactory.newLatLng(latLng));
 				
 				float speed = location.getSpeed();
 				GPSAccuracy.setText(String.format("%s %.2f m", getString(R.string.gps_accuracy), location.getAccuracy()));
 				
 				pace = (double) 1 / (speed * 60 / 1000);
 				
 				double lastDistance = ActivityActivity.this.lastDistance / 1000;
 				
 				int distancetoShow = (int) (distance / 1000);
 				// new km
 				if (distancetoShow - (int) lastDistance > 0) {
 					addMarker(location, distancetoShow);
 				}
 				
 				synchronized (time) {
 					avgPace = ((double) time / 60) / distance;
 				}
 			}
 		});
 	}
 	
 	private CameraPosition buildCameraPosition(LatLng latLng, Location location, Location lastLocation) {
 		Builder builder = new CameraPosition.Builder().target(latLng).zoom(17);	// Sets the zoom
 		if (PreferenceManager.getDefaultSharedPreferences(this).getBoolean(getString(R.string.key_map_3d), true)) {
 			builder
 				.bearing(lastLocation.bearingTo(location)) // Sets the orientation of the
 				// camera to east
 				.tilt(60); // Creates a CameraPosition from the builder
 		}
 		return builder.build();
 	}
 	
 	private void addMarker(Location location, int distance) {
 		Marker marker = mMap.addMarker(new MarkerOptions().position(
 			new LatLng(location.getLatitude(), location.getLongitude())).title(distance + "km"));
 		marker.showInfoWindow();
 	}
 	
 	// this runs on every update
 	private void updateGpsInfo(final Location newLocation) {
 		autoPauseIfEnabled(newLocation);
 		handlerForService.post(new Runnable() {
 			
 			@Override
 			public void run() {
 				if (!isPaused && newLocation.getAccuracy() < LocationService.REQUIRED_ACCURACY) {
 					// not first point after start or resume
 					
 					if (lostGPSDialog != null) {
 						lostGPSDialog.dismiss();
 						lostGPSDialog = null;
 					}
 					if (mLastLocation != null) {
 						countData(newLocation, mLastLocation);
 					}
 					
 					updateData(DataTextView1, dataTextView1Content);
 					updateData(DataTextView2, dataTextView2Content);
 				}
 				else if (newLocation.getAccuracy() >= LocationService.REQUIRED_ACCURACY) {
 					// TODO make progress dialog, waiting for gps
 					showLostGpsSignalDialog();
 				}
 				mLastLocation = newLocation;
 			}
 		});
 		
 	}
 	
 	private void autoPauseIfEnabled(Location newLocation) {
 		if (PreferenceManager.getDefaultSharedPreferences(this).getBoolean(getString(R.string.key_auto_pause), false)) {
 			if (newLocation.hasSpeed()) {
 				Log.e(TAG, "Speed:" + newLocation.getSpeed());
 				if (newLocation.getSpeed() < MIN_SPEED_FOR_AUTO_PAUSE) {
 					pauseRun();
 				}
 				else {
 					resumeRun();
 				}
 			}
 			else {
 				Log.e(TAG, "No speed.. pausing anyway");
 				pauseRun();
 			}
 		}
 	}
 	
 	@Override
 	protected void onPause() {
 		beepPlayer.stopPlayer();
 		super.onPause();
 	}
 	
 	@Override
 	public boolean onKeyDown(int keyCode, KeyEvent event) {
 		if ((keyCode == KeyEvent.KEYCODE_BACK)) {
 			showAlertDialog();
 		}
 		return super.onKeyDown(keyCode, event);
 	}
 	
 	// SERVICE METHODS
 	
 	private void doBindService() {
 		
 		Log.i("Service_info", "ActivityActivity Binding");
 		Intent intent = new Intent(LocationService.class.getName());
 		//intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
 		//intent.setFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
 		bindService(intent, serviceConnection, 0);
 		mIsBound = true;
 		
 	}
 	
 	private void doUnbindService() {
 		Log.i("Service_info", "Activity Unbinding");
 		if (mIsBound) {
 			try {
 				api.removeListener(runListener);
 				unbindService(serviceConnection);
 			}
 			catch (RemoteException e) {
 				
 				e.printStackTrace();
 			}
 			
 			mIsBound = false;
 			
 		}
 	}
 	
 	private final ServiceConnection serviceConnection = new ServiceConnection() {
 		
 		@Override
 		public void onServiceConnected(ComponentName name, IBinder service) {
 			Log.i(TAG, "Service connection established");
 			isServiceConnected = true;
 			// that's how we get the client side of the IPC connection
 			api = RunListenerApi.Stub.asInterface(service);
 			try {
 				api.addListener(runListener);
 				List<Location> locationList = api.getWholeRun();
 				if (locationList == null) {
 					startCountDown();
 				}
 				else {
 					setTracefromServer(locationList);
 				}
 				
 				if (canStart) {
 					startRecording();
 				}
 			}
 			catch (RemoteException e) {
 				Log.e(TAG, "Failed to add listener", e);
 			}
 		}
 		
 		@Override
 		public void onServiceDisconnected(ComponentName name) {
 			isServiceConnected = false;
 			Log.i(TAG, "Service connection closed");
 		}
 		
 	};
 	private final RunListener.Stub runListener = new RunListener.Stub() {
 		
 		@Override
 		public void handleLocationUpdate() throws RemoteException {
 			Location location = api.getLatestLocation();
 			lastDistance = distance;
 			distance = api.getDistance();
 			updateGpsInfo(location);
 		}
 		
 		@Override
 		public void handleConnectionResult() throws RemoteException {
 			// TODO Auto-generated method stub
 			
 		}
 		
 		@Override
 		public void handleTimeChange() throws RemoteException {
 			handleTimeUpdates();
 		}
 		
 		@Override
 		public void handleWorkoutChange(Workout workout) throws RemoteException {
 			handleWorkoutUpdate(workout);
 		}
 		
 	};
 	
 	private void setTracefromServer(final List<Location> locationList)
 	{
 		handlerForService.post(new Runnable() {
 			
 			@Override
 			public void run() {
 				if (locationList != null)
 				{
 					LatLng latLng = null;
 					for (Location location : locationList)
 					{
 						latLng = new LatLng(location.getLatitude(), location.getLongitude());
 						traceOnMap.add(latLng);
 					}
 					traceOnMapObject.setPoints(traceOnMap.getPoints());
 					int size = locationList.size();
 					if (size > 1)
 					{
 						CameraPosition cameraPosition = buildCameraPosition(latLng, locationList.get(size - 2),
 							locationList.get(size - 1));
 						mMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
 						
 					}
 				}
 			}
 		});
 		
 	}
 	
 	private void updateViewsAfterTimeChange()
 	{
 		handlerForService.post(new Runnable() {
 			
 			@Override
 			public void run() {
 				updateData(DataTextView1, dataTextView1Content);
 				updateData(DataTextView2, dataTextView2Content);
 			}
 		});
 	}
 	
 	private void handleTimeUpdates()
 	{
 		try {
 			time = api.getTime();
 			updateViewsAfterTimeChange();
 		}
 		catch (RemoteException e) {
 			// TODO Auto-generated catch block
 			e.printStackTrace();
 		}
 	}
 	
 	private void handleWorkoutUpdate(final Workout newWorkout) {
 		handlerForService.post(new Runnable() {
 			
 			@Override
 			public void run() {
 				ActivityActivity.this.workout.updateWorkoutData(newWorkout);
 				drawerListAdapter.notifyDataSetChanged();
 				listView.smoothScrollToPosition(workout.getCurrentAction() + 4, workout.getActions().size());
 			}
 		});
 	}
 	
 	private void startSystemMusicPlayer() {
 		Intent i;
 		i = new Intent(MediaStore.INTENT_ACTION_MUSIC_PLAYER);
 		startActivity(i);
 	}
 }
