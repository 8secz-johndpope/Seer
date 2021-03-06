 /*
  * Copyright [2012] [Dina Zuko]
 
    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at
 
        http://www.apache.org/licenses/LICENSE-2.0
 
    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License. 
  */
 
 package com.chalmers.schmaps;
 
 import java.io.BufferedReader;
 import java.io.IOException;
 import java.io.InputStream;
 import java.io.InputStreamReader;
 import java.io.UnsupportedEncodingException;
 import java.net.HttpURLConnection;
 import java.net.MalformedURLException;
 import java.net.URL;
 import java.util.List;
 
 import org.json.JSONArray;
 import org.json.JSONException;
 import org.json.JSONObject;
 
 import com.google.android.maps.GeoPoint;
 import com.google.android.maps.MapActivity;
 import com.google.android.maps.MapController;
 import com.google.android.maps.MapView;
 import com.google.android.maps.Overlay;
 
 import com.google.android.maps.OverlayItem;
 
 import android.content.Context;
 import android.graphics.drawable.Drawable;
 import android.location.Criteria;
 import android.location.Location;
 import android.location.LocationListener;
 import android.location.LocationManager;
 import android.os.AsyncTask;
 import android.os.Bundle;
 import android.util.Log;
 import android.view.View;
 import android.view.inputmethod.InputMethodManager;
 import android.widget.Button;
 import android.widget.EditText;
 
 /*******************************************************
  * CheckInActivity shows a MapView zoomed into your position and shows people that are checked in
  * It connects an external database and collects info about people who have checked in i the last two hours
  * User can check in by entering your name and clicking check-in, if user does not enter the name it will still
  * check in but it will state "Unknown" as name
  * By clicking on the figures on the map you can see the name and when they have checked in
  * WARNING: This application will probably crash on an emulator since it can not get your position
  **************************************************************/
 public class CheckInActivity extends MapActivity implements View.OnClickListener{
 
 	private GeoPoint ourLocation;
 	private LocationManager location_manager;
 	private LocationListener location_listener;
 	private JSONObject returnedJsonObject;
 	private List<Overlay> overlayList;
 	private MapItemizedOverlay mapItemizedCheckIn;
 
 	private Criteria criteria;
 	private String bestProvider;
 	private Location location;
 	private int longitude;
 	private int latitude;
 	private String username;
 	private boolean checkin;
 	private MapController mapcon;
 	private MapView mapview;
 	private Button checkInButton;
 	private EditText enterName;
 	private Drawable checkInDot;
 	private OverlayItem overlayitem;
 	private JSONArray result;
 	private boolean running;
 
 	@Override
 	public void onCreate(Bundle savedInstanceState) {
 		super.onCreate(savedInstanceState);
 		//assigns variables used in this class
 		assignInstances(); 
 
 		location_manager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
 		//deafult criteria
 		criteria = new Criteria(); 
 		//best reception
 		bestProvider = location_manager.getBestProvider(criteria, false); 
 		//gets last known location from chosen provider
 		location = location_manager.getLastKnownLocation(bestProvider); 
 
 		//if there is an provider that provides an location ->continue
 		if(location != null){ 
 			//get the latitude
 			latitude = (int) (location.getLatitude()*1E6);
 			//get the longitude
 			longitude = (int) (location.getLongitude()*1E6); 
 			//greates an geopoint with our location
 			ourLocation = new GeoPoint(latitude, longitude); 
 
 			mapcon.animateTo(ourLocation);
 			//zoom level
 			mapcon.setZoom(17); 
 
 		}
 
 		location_listener = new LocationListener(){
 			/**
 			 * method is called when location is changed, when onResumed() is called
 			 */
 			public void onLocationChanged(Location loc) { 
 				//get the latitude
 				latitude = (int) (location.getLatitude()*1E6); 
 				//get the longitude
 				longitude = (int) (location.getLongitude()*1E6); 
 				//greates an geopoint with our location
 				ourLocation = new GeoPoint(latitude, longitude); 
 			}
 
 			public void onProviderDisabled(String provider) {
 
 			}
 
 			public void onProviderEnabled(String provider) {
 
 			}
 
 			public void onStatusChanged(String provider, int status,
 					Bundle extras) {
 
 			}	
 		};
 		connectExternalDatabase();
 	}
 
 
 	/**
 	 * assigns variables used in this class
 	 */
 	public void assignInstances(){
 
 		setContentView(R.layout.activity_checkin);
 		returnedJsonObject = null;
 		username = "";
 		checkin = false;
 		running = false;
 
 		mapview = (MapView) findViewById(R.id.mapview);
 		mapview.setBuiltInZoomControls(true);
 		mapcon = mapview.getController(); 
 
 		checkInButton = (Button) findViewById(R.id.checkinbutton);
 		enterName = (EditText) findViewById(R.id.entername);
 		checkInButton.setOnClickListener(this);
 
 	}
 
 	/*********************************************************************
 	 * Creates the object of a new thread and executes it
 	 * Waits the thread to return an jsonobject, sleeps main thread if json object not returned
 	 * Calls method parseJsonAndDraw() when jsonobject returned
 	 ********************************************************************/
 	public void connectExternalDatabase(){
 		returnedJsonObject = null;
 		GetCheckIn getCheckIn = new GetCheckIn();
 		//the method doInBackground() is executed
 		getCheckIn.execute(); 
 
 		//if json object not returned, sleep for 30 sec
 		while(returnedJsonObject == null){ 
 			try {
 				Thread.sleep(500);
 			} catch (InterruptedException e1) {
 				e1.printStackTrace();
 			}
 		}
 
 		running = true;
 
 		parseJsonAndDraw(returnedJsonObject);
 	}
 
 	/*******************************************************************
 	 * Method parses through in parameter jsonObject and creates an array of geopoints
 	 * each geopoint representing an checked-in person
 	 * @param jsonObject
 	 * @return arraylist of geopoints
 	 **********************************************************************/
 	public void parseJsonAndDraw(JSONObject jsonObject){
 		//greates an geopoint with our location
 		GeoPoint geopoint; 
 		int lat, lng;
 		String name,time;
 		StringBuffer timebuffer = new StringBuffer();
 
 		overlayList = mapview.getOverlays();
 		 //drawable
 		checkInDot = this.getResources().getDrawable(R.drawable.chalmersandroid);
 		//mapitemizedoverlay with drawable
 		mapItemizedCheckIn = new MapItemizedOverlay(checkInDot, this); 
 
 		result =null;
 
 		try {
 			result = jsonObject.getJSONArray("result");
 			JSONObject checkedInPerson;
 
 			//loop through the jsonarray and extract all checked-in points
 			//collect data, create geopoint and add to list of overlays that will be drawn on map
 			for(int count = 0;count<result.length();count++){
 				checkedInPerson = result.getJSONObject(count);
 				//extract time
 				time = checkedInPerson.getString("time");
 				//insert in stringbuffer
 				timebuffer.insert(0,time); 
 				//delete seconds
 				timebuffer.delete(16, timebuffer.length()); 
 				 //delete date
 				timebuffer.delete(0, 11);
 				//convert back to string
 				time = timebuffer.toString(); 
 				//clear buffer
 				timebuffer.delete(0,timebuffer.length()); 
 				name = checkedInPerson.getString("name");
 				lat = (int)checkedInPerson.getInt("lat");
 				lng = (int)checkedInPerson.getInt("lng");
 				geopoint = new GeoPoint(lat,lng);
 				overlayitem = new OverlayItem(geopoint, name, "Checked in at " + time);
 				mapItemizedCheckIn.addOverlay(overlayitem);
 				overlayList.add(mapItemizedCheckIn);
 			}
 		} catch (JSONException e) {
 			e.printStackTrace();
 		}		
 
 		mapview.postInvalidate();
 	}
 
 	/***************************************************************************
 	 * 	 When user enters her/his name and presses check-in the information is sent to an external server
 	 *   Returened is a jsonobject with all postions where people have checked in the last hour
 	 *   Start new activity that will show the information on a map
 	 *************************************************************************/
 	public void onClick(View v) {
 		InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
 		imm.hideSoftInputFromWindow(enterName.getWindowToken(), 0);
 		checkin = true;
 
 		username = enterName.getText().toString();
 		//removes white signs
 		username.trim();
 		//Removes illegal characters to prevent sql injection
 		username = username.replaceAll("[^[a-z][A-Z][0-9]]",""); 
 
 		//if the user have not entered a name the name is set to unknown
 		if(username.equals("")){
 			username = "Unknown";
 		}
 		connectExternalDatabase();
 		checkin =false;
 	}
 
 	/**
 	 * @return the username that user enters
	 * @throws UnsupportedEncodingException 
 	 */
	public String getInputName() throws UnsupportedEncodingException{
		String str = new String(username.getBytes("macintosh"), "UTF-8");
		return str;
 	}

 	/**
 	 * @return the size of jsonarray returned from string
 	 */
 	public int getSizeOfJsonArray(){
 		return result.length();
 	}
 
 	/**
 	 * 
 <<<<<<< HEAD
 	 * @return true if the doinbackground() in asynktask has executed
 =======
 	 * @return true if the goinbackground method in getcheckin has executed
 >>>>>>> 3b9c24e68944d2e99ddfd225913f53eeb55aa051
 	 */
 	public boolean getIsAsyncTaskRunning(){
 		return running;
 	}
 
 
 	/****************************************************************************
 	 * this innerclass creates a new thread from where we can make a request
 	 *  to google directions api - to get the directions
 	 * 	inspired by
 	 *  http://www.vogella.com/articles/AndroidPerformance/article.html
 	 ********************************************************************************/
 	private class GetCheckIn extends AsyncTask<Void, Void, JSONObject> {
 
 		/** when called makes a request to google directions api (json format) 
 		 *  gets the response back
 		 *  convertes the response to a jsonobject
 		 */
 		@Override
 		protected JSONObject doInBackground(Void... params) {
 
 			StringBuilder urlString = new StringBuilder();
 			StringBuilder response = new StringBuilder();
 			InputStream is = null;
 			URL url = null;
 			HttpURLConnection urlConnection = null;
 			String line = null;
 			String jsonResponse = "";
 
 			//Create a string with the right start and end position
 			urlString.append("http://schmaps.scarleo.se/schmaps.php?name=");
 			//from, your position, latitude
 			urlString.append(username); 
 			urlString.append("&lat=");
 			//latitude
 			urlString.append(Integer.toString((int) latitude)); 
 			urlString.append("&lng=");
 			//longitude
 			urlString.append(Integer.toString((int) longitude)); 
 			//authorization key
 			urlString.append("&key=bSJ9B9CFn449QRsXL9qMxW-lc"); 
 			if(checkin){
 				urlString.append("&insert=1");
 			}
 
 			//establish a connection with google directions api
 			try {
 				url = new URL(urlString.toString());
 				urlConnection = (HttpURLConnection) url.openConnection();
 				urlConnection.setRequestMethod("GET");
 				urlConnection.setDoOutput(true);
 				urlConnection.setDoInput(true);
 				is = urlConnection.getInputStream();
 				urlConnection.connect();
 			} catch (MalformedURLException e) {
 				e.printStackTrace();
 			} catch (IOException e) {
 				e.printStackTrace();
 			}
 
 
 			//read from the buffer line by line and save in response (a stringbuider)
 			try{
 				InputStreamReader inputStream = new InputStreamReader(is);
 				BufferedReader reader = new BufferedReader(inputStream);
 				while((line = reader.readLine()) != null){
 					response.append(line);
 				}
 				//Close the reader, stream & connection
 				reader.close();
 				inputStream.close();
 				urlConnection.disconnect();
 			}catch(Exception e) {
 				Log.e("Buffer Error", "Error converting result " + e.toString());
 			}
 
 			jsonResponse = response.toString();
 
 			//convert string to jsonobject and return the object
 			try{
 				returnedJsonObject = new JSONObject(jsonResponse);
 			}catch(JSONException e){
 
 			}
 
 			return returnedJsonObject;
 		}
 
 	}
 
 
 	@Override
 	protected boolean isRouteDisplayed() {
 		return false;
 	}
 
 	@Override
 	protected void onPause() {
 		super.onPause();
 		location_manager.removeUpdates(location_listener);
 	}
 
 	@Override
 	protected void onResume() {
 		super.onResume();
 		try {
 			// Register the listener with the Location Manager to receive
 			// location updates
 			location_manager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 1000, 10, location_listener);
 		}
 		catch (Exception e) {
 		}
 	}
 }
