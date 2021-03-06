 /*
  * Copyright (C) 2012 The Android Open Source Project
  *
  * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
  * in compliance with the License. You may obtain a copy of the License at
  *
  * http://www.apache.org/licenses/LICENSE-2.0
  *
  * Unless required by applicable law or agreed to in writing, software distributed under the License
  * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
  * or implied. See the License for the specific language governing permissions and limitations under
  * the License.
  */
 
 package com.kentph.ttcnextbus;
 
 import android.app.Activity;
 import android.content.BroadcastReceiver;
 import android.content.ContentValues;
 import android.content.Context;
 import android.content.Intent;
 import android.content.IntentFilter;
 import android.content.SharedPreferences;
 import android.database.sqlite.SQLiteDatabase;
 import android.net.ConnectivityManager;
 import android.net.NetworkInfo;
 import android.os.AsyncTask;
 import android.os.Bundle;
 import android.preference.PreferenceManager;
 import android.support.v4.app.NavUtils;
 import android.view.Menu;
 import android.view.MenuInflater;
 import android.view.MenuItem;
 import android.webkit.WebView;
 import android.widget.Toast;
 
 //import com.example.android.networkusage.StackOverflowXmlParser.Entry;
 import com.kentph.ttcnextbus.NextBusRouteListXmlParser.Route;
 import com.kentph.ttcnextbus.NextBusRouteConfigXmlParser.RouteConfig;
 
 import org.xmlpull.v1.XmlPullParserException;
 
 import java.io.IOException;
 import java.io.InputStream;
 import java.net.HttpURLConnection;
 import java.net.URL;
 import java.util.List;
 
 import static com.kentph.ttcnextbus.NextBusRouteConfigXmlParser.Path;
 import static com.kentph.ttcnextbus.NextBusRouteConfigXmlParser.PathPoint;
 import static com.kentph.ttcnextbus.RouteDbHelper.RouteDbContract.*;
 
 
 /**
  * Main Activity for the sample application.
  *
  * This activity does the following:
  *
  * o Presents a WebView screen to users. This WebView has a list of HTML links to the latest
  *   questions tagged 'android' on stackoverflow.com.
  *
  * o Parses the StackOverflow XML feed using XMLPullParser.
  *
  * o Uses AsyncTask to download and process the XML feed.
  *
  * o Monitors preferences and the device's network connection to determine whether
  *   to refresh the WebView content.
  */
 public class NetworkActivity extends Activity {
     public static final String WIFI = "Wi-Fi";
     public static final String ANY = "Any";
 //    private static String URL =
 //            "http://webservices.nextbus.com/service/publicXMLFeed?command=routeConfig&a=ttc&r=";
 //            "http://stackoverflow.com/feeds/tag?tagnames=android&sort=newest";
 
     // Whether there is a Wi-Fi connection.
     private static boolean wifiConnected = false;
     // Whether there is a mobile connection.
     private static boolean mobileConnected = false;
     // Whether the display should be refreshed.
     public static boolean refreshDisplay = true;
 
     // The user's current network preference setting.
     public static String sPref = null;
 
     // The BroadcastReceiver that tracks network connectivity changes.
     private NetworkReceiver receiver = new NetworkReceiver();
 
     @Override
     public void onCreate(Bundle savedInstanceState) {
         super.onCreate(savedInstanceState);
 
         // Register BroadcastReceiver to track connection changes.
         IntentFilter filter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
         receiver = new NetworkReceiver();
         this.registerReceiver(receiver, filter);
 
 
     }
 
     // Refreshes the display if the network connection and the
     // pref settings allow it.
     @Override
     public void onStart() {
         super.onStart();
 
         // Gets the user's network preference settings
         SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
 
         // Retrieves a string value for the preferences. The second parameter
         // is the default value to use if a preference value is not found.
         sPref = sharedPrefs.getString("listPref", "Wi-Fi");
 
         updateConnectedFlags();
 
         // Only loads the page if refreshDisplay is true. Otherwise, keeps previous
         // display. For example, if the user has set "Wi-Fi only" in prefs and the
         // device loses its Wi-Fi connection midway through the user using the app,
         // you don't want to refresh the display--this would force the display of
         // an error page instead of stackoverflow.com content.
         if (refreshDisplay) {
             loadPage();
         }
     }
 
     @Override
     public void onDestroy() {
         super.onDestroy();
         if (receiver != null) {
             this.unregisterReceiver(receiver);
         }
     }
 
     // Checks the network connection and sets the wifiConnected and mobileConnected
     // variables accordingly.
     private void updateConnectedFlags() {
         ConnectivityManager connMgr =
                 (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
 
         NetworkInfo activeInfo = connMgr.getActiveNetworkInfo();
         if (activeInfo != null && activeInfo.isConnected()) {
             wifiConnected = activeInfo.getType() == ConnectivityManager.TYPE_WIFI;
             mobileConnected = activeInfo.getType() == ConnectivityManager.TYPE_MOBILE;
         } else {
             wifiConnected = false;
             mobileConnected = false;
         }
     }
 
     // Uses AsyncTask subclass to download the XML feed from stackoverflow.com.
     // This avoids UI lock up. To prevent network operations from
     // causing a delay that results in a poor user experience, always perform
     // network operations on a separate thread from the UI.
     private void loadPage() {
         if (((sPref.equals(ANY)) && (wifiConnected || mobileConnected))
                 || ((sPref.equals(WIFI)) && (wifiConnected))) {
             // AsyncTask subclass
             new DownloadXmlTask().execute();
         } else {
             showErrorPage();
         }
     }
 
     // Displays an error if the app is unable to load content.
     private void showErrorPage() {
         setContentView(R.layout.activity_network);
 
         // The specified network connection is not available. Displays error message.
         WebView myWebView = (WebView) findViewById(R.id.webview);
         myWebView.loadData(getResources().getString(R.string.connection_error),
                 "text/html", null);
 //        TextView textView = (TextView) findViewById(R.id.text);
 //        textView.setText(getResources().getString(R.string.connection_error));
     }
 
     // Populates the activity's options menu.
     @Override
     public boolean onCreateOptionsMenu(Menu menu) {
         MenuInflater inflater = getMenuInflater();
         inflater.inflate(R.menu.mainmenu, menu);
         return true;
     }
 
     // Handles the user's menu selection.
     @Override
     public boolean onOptionsItemSelected(MenuItem item) {
         switch (item.getItemId()) {
         case R.id.settings:
                 Intent settingsActivity = new Intent(getBaseContext(), SettingsActivity.class);
                 startActivity(settingsActivity);
                 return true;
         case R.id.refresh:
                 loadPage();
                 return true;
         case android.R.id.home:
                 // This ID represents the Home or Up button. In the case of this
                 // activity, the Up button is shown. Use NavUtils to allow users
                 // to navigate up one level in the application structure. For
                 // more details, see the Navigation pattern on Android Design:
                 //
                 // http://developer.android.com/design/patterns/navigation.html#up-vs-back
                 //
                 NavUtils.navigateUpFromSameTask(this);
                 return true;
         default:
                 return super.onOptionsItemSelected(item);
         }
     }
 
     // Implementation of AsyncTask used to download XML feed from stackoverflow.com.
     private class DownloadXmlTask extends AsyncTask<Void, Void, String> {
 
         @Override
         protected String doInBackground(Void... params) {
             try {
                 return loadXmlFromNetwork();
             } catch (IOException e) {
                 return getResources().getString(R.string.connection_error);
             } catch (XmlPullParserException e) {
                 return getResources().getString(R.string.xml_error);
             }
         }
 
         @Override
         protected void onPostExecute(String result) {
             setContentView(R.layout.activity_network);
             // Displays the HTML string in the UI via a WebView
             WebView myWebView = (WebView) findViewById(R.id.webview);
             myWebView.loadData(result, "text/html", null);
         }
     }
 
     // Uploads XML from stackoverflow.com, parses it, and combines it with
     // HTML markup. Returns HTML string.
     private String loadXmlFromNetwork() throws XmlPullParserException, IOException {
         InputStream stream = null;
         NextBusRouteListXmlParser nextBusRouteListXmlParser = new NextBusRouteListXmlParser();
         NextBusRouteConfigXmlParser nextBusRouteConfigXmlParser = new NextBusRouteConfigXmlParser();
         List<Route> routes = null;
         RouteConfig routeConfig = null;
 
 //        String routeNumber = null; // Split routeTitle into Number and Name
 //        String routeName = null;
 //        String stopTitle = null;  // Otherwise match output attrs exactly
 //        // attrs from direction tag
 //        String direction = null;
 //        String terminal = null;
 //
 //        List<Prediction> listOfPredictions = new ArrayList<Prediction>();
 
 //        Calendar rightNow = Calendar.getInstance();
 //        DateFormat formatter = new SimpleDateFormat("MMM dd h:mmaa");
 
         // Checks whether the user set the preference to include summary text
 //        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
 //        boolean pref = sharedPrefs.getBoolean("summaryPref", false);
 
         StringBuilder htmlString = new StringBuilder();
 
         RouteDbHelper mDbHelper = new RouteDbHelper(getBaseContext());
         // Gets the data repository in write mode
         SQLiteDatabase db = mDbHelper.getWritableDatabase();
 
         // create only 1 transaction for speed
         db.beginTransaction();
 
 //        htmlString.append("<h3>" + getResources().getString(R.string.page_title) + "</h3>");
 //        htmlString.append("<em>" + getResources().getString(R.string.updated) + " " +
 //                formatter.format(rightNow.getTime()) + "</em>");
 
         // get route list
         try {
             stream = downloadUrl("http://webservices.nextbus.com/service/publicXMLFeed?command=routeList&a=ttc");
             routes = nextBusRouteListXmlParser.parse(stream);
 
             for (Route route : routes) {
                 stream = downloadUrl("http://webservices.nextbus.com/service/publicXMLFeed?command=routeConfig&a=ttc&r=".concat(route.routeNumber));
                 routeConfig = nextBusRouteConfigXmlParser.parse(stream);
 
                 // import into path table
                 int pathCount = 0;
                 for (Path path : routeConfig.paths)
                     for (PathPoint pathPoint : path.path) {
                         // Create a new map of values, where column names are the keys
                         ContentValues pValues = new ContentValues();
                         pValues.put(PathsTable.COLUMN_NAME_ROUTE_NUMBER, routeConfig.routeNumber);
                         pValues.put(PathsTable.COLUMN_NAME_PATH_ID, pathCount);
                         pValues.put(PathsTable.COLUMN_NAME_LAT, pathPoint.lat);
                         pValues.put(PathsTable.COLUMN_NAME_LON, pathPoint.lon);
 
                         // Insert the new row, returning the primary key value of the new row
 //                        long newRowId;
                         db.insert(PathsTable.TABLE_NAME, null, pValues);
                     }
 
                 // import into stop table
                 for (NextBusRouteConfigXmlParser.Stop stop : routeConfig.stops) {
                     ContentValues sValues = new ContentValues();
                     sValues.put(StopsTable.COLUMN_NAME_ROUTE_NUMBER, routeConfig.routeNumber);
                     sValues.put(StopsTable.COLUMN_NAME_ROUTE_NAME, routeConfig.routeName);
                     sValues.put(StopsTable.COLUMN_NAME_STOP_ID, stop.stopId);
                     sValues.put(StopsTable.COLUMN_NAME_STOP_TAG, stop.tag);
                     sValues.put(StopsTable.COLUMN_NAME_STOP_TITLE, stop.title);
                     sValues.put(StopsTable.COLUMN_NAME_DIRECTION, stop.direction);
                     sValues.put(StopsTable.COLUMN_NAME_BRANCH, stop.branch);
                     sValues.put(StopsTable.COLUMN_NAME_LAT, stop.lat);
                     sValues.put(StopsTable.COLUMN_NAME_LON, stop.lon);
                     // we want integer division to get grid IDs
                    sValues.put(StopsTable.COLUMN_NAME_GRID_LAT, (int) stop.lat/0.004);
                    sValues.put(StopsTable.COLUMN_NAME_GRID_LON, (int) stop.lon/0.004);
 
                     // Insert the new row, returning the primary key value of the new row
 //                        long newRowId;
                     db.insert(StopsTable.TABLE_NAME, null, sValues);
                 }
             }
 
             // if all stops/paths imported...
             db.setTransactionSuccessful();
             htmlString.append("<p>Successful</p>");
 
         // Makes sure that the InputStream is closed after the app is
         // finished using it.
         } finally {
             // end transaction whether or not successful
             db.endTransaction();
             if (stream != null) {
                 stream.close();
             }
             htmlString.append("<p>Unsuccessful</p>");
         }
 
         htmlString.append("<p>Done</p>");
         // StackOverflowXmlParser returns a List (called "entries") of Entry objects.
         // Each Entry object represents a single post in the XML feed.
         // This section processes the entries list to combine each entry with HTML markup.
         // Each entry is displayed in the UI as a link that optionally includes
         // a text summary.
 //        for (Route route : routes) {
 //            htmlString.append("<p>");
 //            htmlString.append(route.routeNumber);
 //            htmlString.append(" - ");
 //            htmlString.append(route.routeName);
 //            htmlString.append("</p>");
 //        }
         return htmlString.toString();
     }
 
     // Given a string representation of a URL, sets up a connection and gets
     // an input stream.
     private InputStream downloadUrl(String urlString) throws IOException {
         URL url = new URL(urlString);
         HttpURLConnection conn = (HttpURLConnection) url.openConnection();
         conn.setReadTimeout(10000 /* milliseconds */);
         conn.setConnectTimeout(15000 /* milliseconds */);
         conn.setRequestMethod("GET");
         conn.setDoInput(true);
         // Starts the query
         conn.connect();
         InputStream stream = conn.getInputStream();
         return stream;
     }
 
     /**
      *
      * This BroadcastReceiver intercepts the android.net.ConnectivityManager.CONNECTIVITY_ACTION,
      * which indicates a connection change. It checks whether the type is TYPE_WIFI.
      * If it is, it checks whether Wi-Fi is connected and sets the wifiConnected flag in the
      * main activity accordingly.
      *
      */
     public class NetworkReceiver extends BroadcastReceiver {
 
         @Override
         public void onReceive(Context context, Intent intent) {
             ConnectivityManager connMgr =
                     (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
             NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
 
             // Checks the user prefs and the network connection. Based on the result, decides
             // whether
             // to refresh the display or keep the current display.
             // If the userpref is Wi-Fi only, checks to see if the device has a Wi-Fi connection.
             if (WIFI.equals(sPref) && networkInfo != null
                     && networkInfo.getType() == ConnectivityManager.TYPE_WIFI) {
                 // If device has its Wi-Fi connection, sets refreshDisplay
                 // to true. This causes the display to be refreshed when the user
                 // returns to the app.
                 refreshDisplay = true;
                 Toast.makeText(context, R.string.wifi_connected, Toast.LENGTH_SHORT).show();
 
                 // If the setting is ANY network and there is a network connection
                 // (which by process of elimination would be mobile), sets refreshDisplay to true.
             } else if (ANY.equals(sPref) && networkInfo != null) {
                 refreshDisplay = true;
 
                 // Otherwise, the app can't download content--either because there is no network
                 // connection (mobile or Wi-Fi), or because the pref setting is WIFI, and there
                 // is no Wi-Fi connection.
                 // Sets refreshDisplay to false.
             } else {
                 refreshDisplay = false;
                 Toast.makeText(context, R.string.lost_connection, Toast.LENGTH_SHORT).show();
             }
         }
     }
 }
