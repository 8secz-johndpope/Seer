 /*
  * Copyright 2012 University of South Florida
  * 
  * Licensed under the Apache License, Version 2.0 (the "License");
  * you may not use this file except in compliance with the License.
  * You may obtain a copy of the License at
  * 
  *        http://www.apache.org/licenses/LICENSE-2.0
  * 
  * Unless required by applicable law or agreed to in writing, software
  * distributed under the License is distributed on an "AS IS" BASIS,
  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  * See the License for the specific language governing permissions and 
  * limitations under the License.
  */
 
 package edu.usf.cutr.siri.android.client;
 
 /**
  * Spring imports
  */
 //import org.springframework.android.showcase.rest.State;
 //import org.springframework.android.showcase.rest.StatesListAdapter;
 
 import java.io.BufferedInputStream;
 import java.io.IOException;
 import java.net.HttpURLConnection;
 import java.net.URL;
 
 import uk.org.siri.siri.Siri;
 
 import android.app.ProgressDialog;
 import android.content.SharedPreferences;
 import android.os.AsyncTask;
 import android.os.Build;
 import android.os.Bundle;
 import android.preference.PreferenceManager;
 import android.util.Log;
 import android.view.LayoutInflater;
 import android.view.View;
 import android.view.ViewGroup;
 import android.widget.Button;
 import android.widget.EditText;
 import android.widget.Toast;
 
 import com.actionbarsherlock.app.SherlockFragment;
 
 import edu.usf.cutr.siri.android.util.SiriJacksonConfig;
 import edu.usf.cutr.siri.android.util.SiriUtils;
 
 /**
  * The UI for the input fields for the SIRI Vehicle Monitoring Request,
  * as well as the HTTP request for Vehicle Monitoring Request JSON.
  * 
  * @author Sean Barbeau
  * 
  */
 public class VehicleMonRequestFragment extends SherlockFragment {
 
 	private ProgressDialog progressDialog;
 
 	private boolean destroyed = false;
 
 	/**
 	 * EditText fields to hold values typed in by user
 	 */
 	EditText key;
 	EditText operatorRef;
 	EditText vehicleRef;
 	EditText lineRef;
 	EditText directionRef;
 	EditText vehicleMonitoringDetailLevel;
 	EditText maximumNumberOfCallsOnwards;
 
 	public VehicleMonRequestFragment() {
 	}
 
 	@Override
 	public void onCreate(Bundle savedInstanceState) {
 		super.onCreate(savedInstanceState);
 		// RuntimeInlineAnnotationReader.cachePackageAnnotation(
 		// MonitoredVehicleJourneyStructure.class.getPackage(), new
 		// XmlSchemaMine("uk.org.siri.siri.MonitoredVehicleJourneyStructure"));
 	}
 
 	@Override
 	public View onCreateView(LayoutInflater inflater, ViewGroup container,
 			Bundle savedInstanceState) {
 		View v = inflater.inflate(R.layout.siri_vehicle_mon_request, container,
 				false);
 
 		// Try to get the developer key from a resource file, if it exists
 		String strKey = SiriUtils.getKeyFromResource(this);
 
 		key = (EditText) v.findViewById(R.id.key);
 		key.setText(strKey);
 		operatorRef = (EditText) v.findViewById(R.id.operatorRef);
 		vehicleRef = (EditText) v.findViewById(R.id.vehicleRef);
 		lineRef = (EditText) v.findViewById(R.id.lineRef);
 		directionRef = (EditText) v.findViewById(R.id.directionRef);
 		vehicleMonitoringDetailLevel = (EditText) v
 				.findViewById(R.id.vehicleMonDetailLevel);
 		maximumNumberOfCallsOnwards = (EditText) v
 				.findViewById(R.id.maxNumOfCallsOnwards);
 
 		final Button button = (Button) v.findViewById(R.id.submit);
 
 		button.setOnClickListener(new View.OnClickListener() {
 			public void onClick(View v) {
 
 				// Start Async task to make REST request
 				new DownloadVehicleInfoTask().execute();
 
 				// TODO Get response back and show in another tab, then switch
 				// to that tab
 			}
 		});
 
 		return v;
 	}
 
 	@Override
 	public void onDestroy() {
 		super.onDestroy();
 		destroyed = true;
 	}
 
 	// ***************************************
 	// Private methods
 	// ***************************************
 	private void refreshStates(Siri states) {
 		if (states == null) {
 			return;
 		}
 
 		// StatesListAdapter adapter = new StatesListAdapter(this, states);
 		// setListAdapter(adapter);
 	}
 
 	// ***************************************
 	// Private classes
 	// ***************************************
 	private class DownloadVehicleInfoTask extends AsyncTask<Void, Void, Siri> {
 		@Override
 		protected void onPreExecute() {
 			// before the network request begins, show a progress indicator
 			showLoadingProgressDialog();
 		}
 
 		@Override
 		protected Siri doInBackground(Void... params) {
 
 			// The URL for making the GET request
 			String urlString = "http://bustime.mta.info/api/siri/vehicle-monitoring.json?OperatorRef=MTA%20NYCT&DirectionRef=0&LineRef=MTA%20NYCT_S40";
 
 			// String url = "http://bustime.mta.info/api/siri" +
 			// "/vehicle-monitoring.json?OperatorRef=MTA NYCT";
 			// final String url = "http://bustime.mta.info/api/siri" +
 			// "/vehicle-monitoring.json?" +
 			// "key={key}&OperatorRef={operatorRef}&VehicleRef={vehicleRef}&LineRef={lineRef}&DirectionRef={directionRef}"
 			// +
 			// "&VehicleMonitoringDetailLevel={vehicleMonitoringDetailLevel}&MaximumNumberOfCallsOnwards={maximumNumberOfCallsOnwards}";
 
 			// Sample vehicle request:
 			// http://bustime.mta.info/api/siri/vehicle-monitoring.json?OperatorRef=MTA%20NYCT&DirectionRef=0&LineRef=MTA%20NYCT_S40
 			// Sample stop monitoring request:
 			// http://bustime.mta.info/api/siri/stop-monitoring.json?OperatorRef=MTA%20NYCT&MonitoringRef=308214
 			urlString.replace(" ", "%20"); // Handle spaces
 
 			Siri s = null;
 
 			URL url = null;
 			HttpURLConnection urlConnection = null;
 					
 			//Used to time response and parsing
 			final long startTime;
 			final long endTime;
 			
 			/**
 			 * Get user preferences for HTTP connection type and jackson parser object type
 			 */
 			
 			SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getActivity());
			int httpConnectionType = Integer.parseInt(sharedPref.getString(Preferences.KEY_HTTP_CONNECTION_TYPE, "0"));
			int jacksonObjectType = Integer.parseInt(sharedPref.getString(Preferences.KEY_JACKSON_OBJECT_TYPE, "0"));
 						
 			try {
 				url = new URL(urlString);
 				
 				disableConnectionReuseIfNecessary();  //For bugs in HttpURLConnection pre-Froyo
 				
 				startTime= System.nanoTime();
 				
 				if(httpConnectionType == Preferences.HTTP_CONNECTION_TYPE_JACKSON){				
 					//Use integrated Jackson HTTP connection by passing URL directly into Jackson object
 					
 					if(jacksonObjectType == Preferences.JACKSON_OBJECT_TYPE_READER){
 						/*
 						 * Use an ObjectReader (instead of ObjectMapper), read from URL directly
 						 * 
 						 * According to Jackson Best Practices 
 						 * (http://wiki.fasterxml.com/JacksonBestPracticesPerformance),
 						 *  this should be most efficient of the 4 combinations.
 						 */
 						Log.v(SiriRestClientActivity.TAG, "Using ObjectReader Jackson parser, Jackson HTTP Connection");
 						s = SiriJacksonConfig.getObjectReaderInstance().readValue(url);
 					}else{
 						//Use ObjectMapper, read from URL directly
 						Log.v(SiriRestClientActivity.TAG, "Using ObjectMapper Jackson parser, Jackson HTTP Connection");
 						s = SiriJacksonConfig.getObjectMapperInstance().readValue(url, Siri.class);
 					}					
 				}else{
 					//Use Android HttpURLConnection					
 					urlConnection = (HttpURLConnection) url.openConnection();	
 										
 					if(jacksonObjectType == Preferences.JACKSON_OBJECT_TYPE_READER){
 						//Use ObjectReader with Android HttpURLConnection
 						Log.v(SiriRestClientActivity.TAG, "Using ObjectReader Jackson parser, Android HttpURLConnection");
 						s = SiriJacksonConfig.getObjectReaderInstance().readValue(urlConnection.getInputStream());
 					}else{
 						//Use ObjectMapper with Android HttpURLConnection
 						Log.v(SiriRestClientActivity.TAG, "Using ObjectMapper Jackson parser, Android HttpURLConnection");
 						s = SiriJacksonConfig.getObjectMapperInstance().readValue(urlConnection.getInputStream(), Siri.class);
 					}
 				}
 							
 				endTime = System.nanoTime();
 				
 				getActivity().runOnUiThread(new Runnable(){
 					public void run(){
 						Toast.makeText(getActivity(), "Elapsed Time = " + (endTime - startTime)/1000000 + "ms", Toast.LENGTH_SHORT).show();
 					}
 				});
 			} catch (IOException e) {
 				Log.e(SiriRestClientActivity.TAG, "Error fetching JSON: " + e);
 			}finally{
 				if(urlConnection != null){
 					urlConnection.disconnect();
 				}
 			}			
 
 			if (s != null) {
 				SiriUtils.printContents(s);
 			}
 
 			return s;
 		}
 
 		@Override
 		protected void onPostExecute(Siri result) {
 			// hide the progress indicator when the network request is complete
 			dismissProgressDialog();
 
 			// return the list of vehicle info
 			refreshStates(result);
 		}
 
 		// ***************************************
 		// Public methods
 		// ***************************************
 		public void showLoadingProgressDialog() {
 			this.showProgressDialog("Requesting. Please wait...");
 		}
 
 		public void showProgressDialog(CharSequence message) {
 			if (progressDialog == null) {
 				progressDialog = new ProgressDialog(getActivity());
 				progressDialog.setIndeterminate(true);
 			}
 
 			progressDialog.setMessage(message);
 			progressDialog.show();
 		}
 
 		public void dismissProgressDialog() {
 			if (progressDialog != null && !destroyed) {
 				progressDialog.dismiss();
 			}
 		}
 	}
 
 	/**
 	 * Disable HTTP connection reuse which was buggy pre-froyo
 	 */
 	private void disableConnectionReuseIfNecessary() {
 		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.FROYO) {
 			System.setProperty("http.keepAlive", "false");
 		}
 	}
 }
