 package car.io.application;
 
 import java.text.NumberFormat;
 import java.util.ArrayList;
 import java.util.Locale;
 import java.util.concurrent.Executors;
 import java.util.concurrent.ScheduledExecutorService;
 import java.util.concurrent.TimeUnit;
 
 import android.app.Application;
 import android.bluetooth.BluetoothAdapter;
 import android.content.Context;
 import android.content.Intent;
 import android.content.SharedPreferences;
 import android.content.SharedPreferences.Editor;
 import android.location.Location;
 import android.location.LocationListener;
 import android.location.LocationManager;
 import android.net.ParseException;
 import android.os.Bundle;
 import android.os.Handler;
 import android.preference.PreferenceManager;
 import android.util.Log;
 import android.widget.Toast;
 import car.io.adapter.DbAdapter;
 import car.io.adapter.DbAdapterLocal;
 import car.io.adapter.DbAdapterRemote;
 import car.io.adapter.Measurement;
 import car.io.adapter.Track;
 import car.io.commands.CommonCommand;
 import car.io.commands.MAF;
 import car.io.commands.Speed;
 import car.io.exception.FuelConsumptionException;
 import car.io.exception.LocationInvalidException;
 import car.io.exception.MeasurementsException;
 import car.io.exception.TracksException;
 import car.io.obd.BackgroundService;
 import car.io.obd.Listener;
 import car.io.obd.ServiceConnector;
 /**
  * This is the main application that is the central linking component for all adapters, services and so on.
  * This application is implemented like a singleton, it exists only once while the app is running.
  * @author gerald, jakob
  *
  */
 public class ECApplication extends Application implements LocationListener {
 
 	public static final String BASE_URL = "https://giv-car.uni-muenster.de/stable/rest";
 
 	public static final String PREF_KEY_CAR_MODEL = "carmodel";
 	public static final String PREF_KEY_CAR_MANUFACTURER = "manufacturer";
 	public static final String PREF_KEY_CAR_CONSTRUCTION_YEAR = "constructionyear";
 	public static final String PREF_KEY_FUEL_TYPE = "fueltype";
 	public static final String PREF_KEY_SENSOR_ID = "sensorid";
 
 	private SharedPreferences preferences = null;
 
 	private DbAdapter dbAdapterLocal;
 	private DbAdapter dbAdapterRemote;
 	private final ScheduledExecutorService scheduleTaskExecutor = Executors
 			.newScheduledThreadPool(1);
 	// get the default Bluetooth adapter
 	private final BluetoothAdapter bluetoothAdapter = BluetoothAdapter
 			.getDefaultAdapter();
 
 	private ServiceConnector serviceConnector = null;
 	private Intent backgroundService = null;
 	private Handler handler = new Handler();
 	private Listener listener = null;
 	private LocationManager locationManager;
 
 	private float locationLatitude;
 	private float locationLongitude;
 	private int speedMeasurement = 0;
 	private double co2Measurement = 0.0;
 	private double mafMeasurement;
 	private Measurement measurement = null;
 	private long lastInsertTime = 0;
 
 	private Track track;
 	private String trackName = "Some Name";
 	private String trackDescription = "Some Description";
 
 	private boolean requirementsFulfilled = true;
 
 	private static User user;
 	
 	/**
 	 * Returns the service connector of the server
 	 * @return the serviceConnector
 	 */
 	public ServiceConnector getServiceConnector() {
 		return serviceConnector;
 	}
 	
 	/**
 	 * Returns whether requirements were fulfilled (bluetooth activated)
 	 * @return requirementsFulfilled?
 	 */
 	public boolean requirementsFulfilled() {
 		return requirementsFulfilled;
 	}
 
 	/**
 	 * This method updates the attributes of the current sensor (=car) 
 	 * @param sensorid the id that is stored on the server
 	 * @param carManufacturer the car manufacturer
 	 * @param carModel the car model
 	 * @param fuelType the fuel type of the car
 	 * @param year construction year of the car
 	 */
 	public void updateCurrentSensor(String sensorid, String carManufacturer,
 			String carModel, String fuelType, int year) {
 		Editor e = preferences.edit();
 		e.putString(PREF_KEY_SENSOR_ID, sensorid);
 		e.putString(PREF_KEY_CAR_MANUFACTURER, carManufacturer);
 		e.putString(PREF_KEY_CAR_MODEL, carModel);
 		e.putString(PREF_KEY_FUEL_TYPE, fuelType);
 		e.putString(PREF_KEY_CAR_CONSTRUCTION_YEAR, year + "");
 		e.commit();
 	}
 
 	@Override
 	public void onCreate() {
 		super.onCreate();
 
 		// TODO: Create something like a first-start method that determines the
 		// BT-Adapter, VIN etc. ... something like a setup method
 
 		initDbAdapter();
 		checkRequirementsForBluetooth();
 		initLocationManager();
 		// AutoConnect checkbox and service
 		// TODO settings -> automatic connection to bt adapter
 		// startServiceConnector();
 		// Make a new listener to interpret the measurement values that are
 		// returned
 		Log.e("obd2", "init listener");
 		startListener();
 		// If everything is available, start the service connector and listener
 		startBackgroundService();
 
 		// createNewTrackIfNecessary();
 
 		try {
 			measurement = new Measurement(locationLatitude, locationLongitude);
 		} catch (LocationInvalidException e) {
 			e.printStackTrace();
 		}
 
 		preferences = PreferenceManager.getDefaultSharedPreferences(this);
 		user = getUserFromSharedPreferences();
 
 	}
 
 	/**
 	 * This method determines whether it is necessary to create a new track or
 	 * of the current/last used track should be reused
 	 */
 	private void createNewTrackIfNecessary() {
 
 		// TODO decode vin or read from shared preferences...
 
 		// setting undefined, will hopefully prevent correct uploading.
 		// but this shouldn't be possible to record tracks without these values
 		String fuelType = preferences
 				.getString(PREF_KEY_FUEL_TYPE, "undefined");
 		String carManufacturer = preferences.getString(
 				PREF_KEY_CAR_MANUFACTURER, "undefined");
 		String carModel = preferences
 				.getString(PREF_KEY_CAR_MODEL, "undefined");
 		String sensorId = preferences
 				.getString(PREF_KEY_SENSOR_ID, "undefined");
 
 		// if track is null, create a new one or take the last one from the
 		// database
 
 		if (track == null) {
 
 			Log.e("obd2", "The track was null");
 
 			Track lastUsedTrack;
 
 			try {
 				lastUsedTrack = dbAdapterLocal.getLastUsedTrack();
 
 				try {
 
 					// New track if last measurement is more than 60 minutes
 					// ago
 
 					if ((System.currentTimeMillis() - lastUsedTrack
 							.getLastMeasurement().getMeasurementTime()) > 3600000) {
 						// TODO: make parameters dynamic
 						Log.e("obd2",
 								"I create a new track because the last measurement is more than 60 mins ago");
 						track = new Track("123456", fuelType, carManufacturer,
 								carModel, sensorId, dbAdapterLocal);
 						track.setName(trackName);
 						track.setDescription(trackDescription);
 						track.commitTrackToDatabase();
 						return;
 					}
 
 					// new track if last position is significantly different
 					// from the current position (more than 3 km)
 					if (getDistance(lastUsedTrack.getLastMeasurement(),
 							locationLatitude, locationLongitude) > 3.0) {
 						Log.e("obd2",
 								"The last measurement's position is more than 3 km away. I will create a new track");
 						track = new Track("123456", fuelType, carManufacturer,
 								carModel, sensorId, dbAdapterLocal); // TODO
 						track.setName(trackName);
 						track.setDescription(trackDescription);
 						track.commitTrackToDatabase();
 						return;
 
 					}
 
 					// TODO: New track if user clicks on create new track button
 
 					// TODO: new track if VIN changed
 
 					else {
 						Log.e("obd2",
 								"I will append to the last track because that still makes sense");
 						track = lastUsedTrack;
 						return;
 					}
 
 				} catch (MeasurementsException e) {
					Log.e("obd", "The last track contains no measurements. I will delete it and create a new one.");
					dbAdapterLocal.deleteTrack(lastUsedTrack.getId());
					track = new Track("123456", fuelType, carManufacturer,
							carModel, sensorId, dbAdapterLocal); // TODO:
					track.setName(trackName);
					track.setDescription(trackDescription);
					track.commitTrackToDatabase();
 				}
 
 			} catch (TracksException e) {
 				track = new Track("123456", fuelType, carManufacturer,
 						carModel, sensorId, dbAdapterLocal); // TODO:
 				track.setName(trackName);
 				track.setDescription(trackDescription);
 				track.commitTrackToDatabase();
 				e.printStackTrace();
 				Log.e("obd2",
 						"There was no track in the database so I created a new one");
 			}
 
 			return;
 
 		}
 
 		// if track is not null, determine whether it is useful to create a new
 		// track and store the current one //TODO: is it necessary to store
 		// this? normally, this is already in the database
 
 		if (track != null) {
 
 			Log.e("obd2", "the track was not null");
 
 			Track currentTrack = track;
 
 			try {
 
 				// New track if last measurement is more than 60 minutes
 				// ago
 				if ((System.currentTimeMillis() - currentTrack
 						.getLastMeasurement().getMeasurementTime()) > 3600000) {
 					// TODO: make parameters dynamic
 					track = new Track("123456", fuelType, carManufacturer,
 							carModel, sensorId, dbAdapterLocal);
 					track.setName(trackName);
 					track.setDescription(trackDescription);
 					track.commitTrackToDatabase();
 					Log.e("obd2",
 							"I create a new track because the last measurement is more than 60 mins ago");
 					return;
 				}
 				// TODO: New track if user clicks on create new track button
 
 				// new track if last position is significantly different from
 				// the
 				// current position (more than 3 km)
 
 				if (getDistance(currentTrack.getLastMeasurement(),
 						locationLatitude, locationLongitude) > 3.0) {
 					track = new Track("123456", fuelType, carManufacturer,
 							carModel, sensorId, dbAdapterLocal); // TODO
 					track.setName(trackName);
 					track.setDescription(trackDescription);
 					track.commitTrackToDatabase();
 					Log.e("obd2",
 							"The last measurement's position is more than 3 km away. I will create a new track");
 					return;
 
 				}
 
 				// TODO: new track if VIN changed
 
 				else {
 					Log.e("obd2",
 							"I will append to the last track because that still makes sense");
 					return;
 				}
 
 			} catch (MeasurementsException e) {
				Log.e("obd", "The last track contains no measurements. I will delete it and create a new one.");
				dbAdapterLocal.deleteTrack(currentTrack.getId());
				track = new Track("123456", fuelType, carManufacturer,
						carModel, sensorId, dbAdapterLocal); // TODO:
				track.setName(trackName);
				track.setDescription(trackDescription);
				track.commitTrackToDatabase();
 			}
 
 		}
 
 	}
 
 	/**
 	 * Returns the distance between a measurement and a coordinate in kilometers
 	 * 
 	 * @param m1
 	 *            Measurement
 	 * @param lat2
 	 *            Latitude of coordinate
 	 * @param lng2
 	 *            Longitude of coordinate
 	 * @return
 	 */
 	public double getDistance(Measurement m1, double lat2, double lng2) {
 
 		double lat1 = m1.getLatitude();
 		double lng1 = m1.getLongitude();
 
 		double earthRadius = 6369;
 		double dLat = Math.toRadians(lat2 - lat1);
 		double dLng = Math.toRadians(lng2 - lng1);
 		double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
 				+ Math.cos(Math.toRadians(lat1))
 				* Math.cos(Math.toRadians(lat2)) * Math.sin(dLng / 2)
 				* Math.sin(dLng / 2);
 		double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
 		double dist = earthRadius * c;
 
 		return dist;
 
 	}
 
 	/**
 	 * This method opens both dbadapters or also gets them and opens them afterwards.
 	 */
 	private void initDbAdapter() {
 		if (dbAdapterLocal == null) {
 			dbAdapterLocal = new DbAdapterLocal(this.getApplicationContext());
 			dbAdapterLocal.open();
 		} else {
 			if (!dbAdapterLocal.isOpen())
 				dbAdapterLocal.open();
 		}
 		if (dbAdapterRemote == null) {
 			dbAdapterRemote = new DbAdapterRemote(this.getApplicationContext());
 			dbAdapterRemote.open();
 		} else {
 			if (!dbAdapterRemote.isOpen())
 				dbAdapterRemote.open();
 		}
 	}
 
 	/**
 	 * This checks whether the bluetooth adadpter exists and whether it is enabled.
 	 * You can get the result by calling the requirementsFulfilled() funtion.
 	 */
 	private void checkRequirementsForBluetooth() {
 		if (bluetoothAdapter == null) {
 
 			requirementsFulfilled = false;
 
 		} else {
 
 			if (!bluetoothAdapter.isEnabled()) {
 				requirementsFulfilled = false;
 			}
 		}
 	}
 
 	/**
 	 * Checks if a track with specific index is already present in the
 	 * dbAdapterRemote
 	 * 
 	 * @param index
 	 * @return true if track already stored, false if track is new
 	 */
 	public boolean trackAlreadyInDB(String index) {
 
 		boolean matchFound = false;
 
 		ArrayList<Track> allStoredTracks = dbAdapterRemote.getAllTracks();
 		for (Track trackCompare : allStoredTracks) {
 			Log.i("obd2", "comparing: " + index + "");
 			Log.i("obd2", "to: " + trackCompare.getId() + "");
 
 			if (trackCompare.getId().equals(index)) {
 				Log.i("obd2", "match found");
 				matchFound = true;
 				return matchFound;
 			}
 		}
 
 		return matchFound;
 	}
 
 	/**
 	 * Get a user object from the shared preferences
 	 * @return the user that is stored on the device
 	 */
 	private User getUserFromSharedPreferences() {
 		if (preferences.contains("username") && preferences.contains("token")) {
 			return new User(preferences.getString("username", "anonymous"),
 					preferences.getString("token", "anon"));
 		}
 		return null;
 	}
 
 	/**
 	 * Set the user (to the application and also store it in the preferences)
 	 * @param user The user you want to set
 	 */
 	public void setUser(User user) {
 		ECApplication.user = user;
 		Editor e = preferences.edit();
 		e.putString("username", user.getUsername());
 		e.putString("token", user.getToken());
 		e.apply();
 	}
 
 	/**
 	 * Get the user
 	 * @return user
 	 */
 	public User getUser() {
 		return user;
 	}
 
 	/**
 	 * Determines whether the user is logged in. A user is logged in when
 	 * the application has a user as a variable.
 	 * @return
 	 */
 	public boolean isLoggedIn() {
 		return user != null;
 	}
 
 	/**
 	 * Logs out the user.
 	 */
 	public void logOut() {
 		if (preferences.contains("username"))
 			preferences.edit().remove("username");
 		if (preferences.contains("token"))
 			preferences.edit().remove("token");
 		preferences.edit().apply();
 		user = null;
 	}
 
 	/**
 	 * Returns the local db adadpter. This has to be called by other
 	 * functions in order to work with the data (change tracks and measurements).
 	 * @return the local db adapter
 	 */
 	public DbAdapter getDbAdapterLocal() {
 		initDbAdapter();
 		return dbAdapterLocal;
 	}
 
 	/**
 	 * Get the remote db adapter (to work with the measurements from the server).
 	 * @return the remote dbadapter
 	 */
 	public DbAdapter getDbAdapterRemote() {
 		initDbAdapter();
 		return dbAdapterRemote;
 	}
 
 	/**
 	 * Starts the location manager again after an resume.
 	 */
 	public void startLocationManager() {
 		locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0,
 				0, this);
 	}
 
 	/**
 	 * Stops the location manager (removeUpdates) for pause.
 	 */
 	public void stopLocating() {
 		locationManager.removeUpdates(this);
 	}
 
 	/**
 	 * This method starts the service that connects to the adapter to the app.
 	 */
 	public void startBackgroundService() {
 		if (requirementsFulfilled) {
 			Log.e("obd2", "requirements met");
 			backgroundService = new Intent(this, BackgroundService.class);
 			serviceConnector = new ServiceConnector();
 			serviceConnector.setServiceListener(listener);
 
 			bindService(backgroundService, serviceConnector,
 					Context.BIND_AUTO_CREATE);
 		} else {
 			Log.e("obd2", "requirements not met");
 		}
 	}
 
 	/**
 	 * This method starts the service connector every five minutes if the user 
 	 * wants an autoconnection
 	 */
 	public void startServiceConnector() {
 		scheduleTaskExecutor.scheduleAtFixedRate(new Runnable() {
 			public void run() {
 				if (requirementsFulfilled) {
 					if (!serviceConnector.isRunning()) {
 						startConnection();
 					} else {
 						Log.e("obd2", "serviceConnector not running");
 					}
 				} else {
 					Log.e("obd2", "requirementsFulfilled was false!");
 				}
 
 			}
 		}, 0, 5, TimeUnit.MINUTES);
 	}
 
 	/**
 	 * This method starts the listener that interprets the answers from the BT adapter.
 	 */
 	public void startListener() {
 		listener = new Listener() {
 
 			public void receiveUpdate(CommonCommand job) {
 				Log.e("obd2", "update received");
 				// Get the name and the result of the Command
 
 				String commandName = job.getCommandName();
 				String commandResult = job.getResult();
 				Log.i("btlogger", commandName + " " + commandResult);
 				if (commandResult.equals("NODATA"))
 					return;
 
 				/*
 				 * Check which measurent is returned and save the value in the
 				 * previously created measurement
 				 */
 
 				// Speed
 
 				if (commandName.equals("Vehicle Speed")) {
 
 					try {
 						speedMeasurement = Integer.valueOf(commandResult);
 					} catch (NumberFormatException e) {
 						Log.e("obd2", "speed parse exception");
 						e.printStackTrace();
 					}
 				}
 
 				// MAF
 
 				if (commandName.equals("Mass Air Flow")) {
 					String maf = commandResult;
 
 					try {
 						NumberFormat format = NumberFormat
 								.getInstance(Locale.GERMAN);
 						Number number;
 						number = format.parse(maf);
 						mafMeasurement = number.doubleValue();
 
 						// Dashboard Co2 current value preparation
 
 						double consumption = 0.0;
 
 						if (mafMeasurement != -1.0) {
 							if (preferences.getString(PREF_KEY_FUEL_TYPE,
 									"gasoline").equals("gasoline")) {
 								consumption = (mafMeasurement / 14.7) / 747;
 							} else if (preferences.getString(
 									PREF_KEY_FUEL_TYPE, "gasoline").equals(
 									"diesel")) {
 								consumption = (mafMeasurement / 14.5) / 832;
 							}
 						}
 
 						if (preferences.getString(PREF_KEY_FUEL_TYPE,
 								"gasoline").equals("gasoline")) {
 							co2Measurement = consumption * 2.35;
 						} else if (preferences.getString(PREF_KEY_FUEL_TYPE,
 								"gasoline").equals("diesel")) {
 							co2Measurement = consumption * 2.65;
 						}
 
 					} catch (ParseException e) {
 						Log.e("obd", "parse exception maf");
 						e.printStackTrace();
 					} catch (java.text.ParseException e) {
 						Log.e("obd", "parse exception maf");
 						e.printStackTrace();
 					}
 				}
 
 				// Update and insert the measurement
 
 				updateMeasurement();
 			}
 
 		};
 	}
 
 	/**
 	 * Stop the service connector and therefore the scheduled tasks.
 	 */
 	public void stopServiceConnector() {
 		scheduleTaskExecutor.shutdown();
 	}
 
 	/**
 	 * Connects to the Bluetooth Adapter and starts the execution of the
 	 * commands. also opens the db and starts the gps.
 	 */
 	public void startConnection() {
 		openDb();
 		startLocationManager();
 		createNewTrackIfNecessary();
 		if (!serviceConnector.isRunning()) {
 			Log.e("obd2", "service start");
 			startService(backgroundService);
 			bindService(backgroundService, serviceConnector,
 					Context.BIND_AUTO_CREATE);
 		}
 		handler.post(waitingListRunnable);
 	}
 
 	/**
 	 * Ends the connection with the Bluetooth Adapter. also stops gps and closes the db.
 	 */
 	public void stopConnection() {
 
 		if (serviceConnector.isRunning()) {
 			stopService(backgroundService);
 			unbindService(serviceConnector);
 		}
 		handler.removeCallbacks(waitingListRunnable);
 
 		stopLocating();
 		closeDb();
 	}
 
 	/**
 	 * Handles the waiting-list
 	 */
 	private Runnable waitingListRunnable = new Runnable() {
 		public void run() {
 
 			if (serviceConnector.isRunning())
 				addCommandstoWaitinglist();
 
 			handler.postDelayed(waitingListRunnable, 2000);
 		}
 	};
 
 	/**
 	 * Helper method that adds the desired commands to the waiting list where
 	 * all commands are executed
 	 */
 	private void addCommandstoWaitinglist() {
 		final CommonCommand speed = new Speed();
 		final CommonCommand maf = new MAF();
 		serviceConnector.addJobToWaitingList(speed);
 		serviceConnector.addJobToWaitingList(maf);
 	}
 
 	/**
 	 * Helper Command that updates the current measurement with the last
 	 * measurement data and inserts it into the database if the measurements is
 	 * young enough
 	 */
 	public void updateMeasurement() {
 
 		// Create track new measurement if necessary
 
 		if (measurement == null) {
 			try {
 				measurement = new Measurement(locationLatitude,
 						locationLongitude);
 			} catch (LocationInvalidException e) {
 				e.printStackTrace();
 			}
 		}
 
 		// Insert the values if the measurement (with the coordinates) is young
 		// enough (5000ms) or create track new one if it is too old
 
 		if (measurement != null) {
 
 			if (Math.abs(measurement.getMeasurementTime()
 					- System.currentTimeMillis()) < 5000) {
 
 				measurement.setSpeed(speedMeasurement);
 				measurement.setMaf(mafMeasurement);
 				Log.e("obd2", "new measurement");
 				Log.e("obd2",
 						measurement.getLatitude() + " "
 								+ measurement.getLongitude());
 				Log.e("obd2", measurement.toString());
 
 				insertMeasurement(measurement);
 
 			} else {
 				try {
 					measurement = new Measurement(locationLatitude,
 							locationLongitude);
 				} catch (LocationInvalidException e) {
 					e.printStackTrace();
 				}
 			}
 		}
 
 	}
 
 	/**
 	 * Helper method to insert track measurement into the database (ensures that
 	 * track measurement is only stored every 5 seconds and not faster...)
 	 * 
 	 * @param measurement2
 	 *            The measurement you want to insert
 	 */
 	private void insertMeasurement(Measurement measurement2) {
 
 		// TODO: This has to be added with the following conditions:
 		/*
 		 * 1)New measurement if more than 50 meters away 2)New measurement if
 		 * last measurement more than 1 minute ago 3)New measurement if MAF
 		 * value changed significantly (whatever this means... we will have to
 		 * investigate on that. also it is not clear whether we should use this
 		 * condition because we are vulnerable to noise from the sensor.
 		 * therefore, we should include a minimum time between measurements (1
 		 * sec) as well.)
 		 */
 
 		if (Math.abs(lastInsertTime - measurement2.getMeasurementTime()) > 5000) {
 
 			lastInsertTime = measurement2.getMeasurementTime();
 
 			track.addMeasurement(measurement2);
 
 			Log.i("obd2", measurement2.toString());
 
 			Toast.makeText(getApplicationContext(), measurement2.toString(),
 					Toast.LENGTH_SHORT).show();
 
 		}
 
 	}
 
 	/**
 	 * Init the location Manager
 	 */
 	private void initLocationManager() {
 
 		locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
 
 		// locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,
 		// 0,
 		// 0, this);
 
 	}
 
 	/**
 	 * Stops gps, kills service, kills service connector, kills listener and handler
 	 */
 	public void destroyStuff() {
 		stopLocating();
 		locationManager = null;
 		backgroundService = null;
 		serviceConnector = null;
 		listener = null;
 		handler = null;
 	}
 
 	/**
 	 * updates the location variables when the device moved
 	 */
 	@Override
 	public void onLocationChanged(Location location) {
 		locationLatitude = (float) location.getLatitude();
 		locationLongitude = (float) location.getLongitude();
 
 	}
 
 	@Override
 	public void onProviderDisabled(String arg0) {
 
 	}
 
 	@Override
 	public void onProviderEnabled(String arg0) {
 
 	}
 
 	@Override
 	public void onStatusChanged(String arg0, int arg1, Bundle arg2) {
 
 	}
 
 	/**
 	 * Opens the databases.
 	 */
 	public void openDb() {
 		initDbAdapter();
 	}
 
 	/**
 	 * Closes both databases.
 	 */
 	public void closeDb() {
 		if (dbAdapterLocal != null) {
 			dbAdapterLocal.close();
 			// dbAdapterLocal = null;
 		}
 		if (dbAdapterRemote != null) {
 			dbAdapterRemote.close();
 			// dbAdapterRemote = null;
 		}
 
 	}
 
 	/**
 	 * @return the speedMeasurement
 	 */
 	public int getSpeedMeasurement() {
 		return speedMeasurement;
 	}
 
 	/**
 	 * @return the track
 	 */
 	public Track getTrack() {
 		return track;
 	}
 
 	/**
 	 * @param track
 	 *            the track to set
 	 */
 	public void setTrack(Track track) {
 		this.track = track;
 	}
 
 	/**
 	 * 
 	 * @return the current co2 value
 	 */
 	public double getCo2Measurement() {
 		return co2Measurement;
 	}
 
 }
