 /*
  * Copyright (c) 2009-2010 Chris Smith
  *
  * Permission is hereby granted, free of charge, to any person obtaining a copy
  * of this software and associated documentation files (the "Software"), to deal
  * in the Software without restriction, including without limitation the rights
  * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
  * copies of the Software, and to permit persons to whom the Software is
  * furnished to do so, subject to the following conditions:
  *
  * The above copyright notice and this permission notice shall be included in
  * all copies or substantial portions of the Software.
  *
  * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
  * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
  * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
  * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
  * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
  * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
  * SOFTWARE.
  */
 
 package uk.co.md87.android.contextanalyser;
 
 import android.app.Service;
 import android.content.Intent;
 import android.location.Address;
 import android.location.Geocoder;
 import android.location.Location;
 import android.os.Handler;
 import android.os.IBinder;
 import android.util.Log;
 
 import java.io.IOException;
 import java.util.Collection;
 import java.util.HashMap;
 import java.util.Iterator;
 import java.util.LinkedList;
 import java.util.List;
 import java.util.Map;
 
 import uk.co.md87.android.common.ModelReader;
 import uk.co.md87.android.common.accel.AccelReaderFactory;
 import uk.co.md87.android.common.aggregator.AutoAggregator;
 import uk.co.md87.android.common.aggregator.AutoAggregatorFactory;
 import uk.co.md87.android.common.geo.LocationMonitor;
 import uk.co.md87.android.common.geo.LocationMonitorFactory;
 import uk.co.md87.android.contextanalyser.model.Journey;
 import uk.co.md87.android.contextanalyser.model.JourneyStep;
 import uk.co.md87.android.contextanalyser.model.Place;
 
 /**
  * Background service which monitors and aggregates various sources of
  * contextual information, including current activity and place. Changes in
  * activity or other context cause the service to emit a broadcast Intent.
  *
  * @author chris
  */
 public class ContextAnalyserService extends Service {
 
     public static final String ACTIVITY_CHANGED_INTENT
             = "uk.co.md87.android.contextanalyser.ACTIVITY_CHANGED";
     public static final String CONTEXT_CHANGED_INTENT
             = "uk.co.md87.android.contextanalyser.CONTEXT_CHANGED";
     public static final String PREDICTION_AVAILABLE_INTENT
             = "uk.co.md87.android.contextanalyser.PREDICTION_AVAILABLE";
 
     public static final int LOCATION_REPEATS = 2;
 
     public static final int CONTEXT_PLACE = 1;
 
     private static final int POLLING_DELAY = 10000;
 
     private final Runnable scheduleRunnable = new Runnable() {
 
         public void run() {
             poll();
         }
     };
 
     private final Runnable analyseRunnable = new Runnable() {
 
         public void run() {
             analyse();
         }
     };
 
     private final Map<String, Long> names = new HashMap<String, Long>();
     private final List<String> activityLog = new LinkedList<String>();
 
     private double lat = 0, lon = 0;
     private int locationCount = 0;
     private Place location, lastLocation;
     private Geocoder geocoder;
     private String lastActivity = "";
 
     private AutoAggregator aggregator;
     private LocationMonitor locationMonitor;
     private DataHelper dataHelper;
     private Handler handler = new Handler();
 
     @Override
     public void onCreate() {
         super.onCreate();
 
         locationMonitor = new LocationMonitorFactory().getMonitor(this);
 
         aggregator = new AutoAggregatorFactory().getAutoAggregator(this, handler,
                 new AccelReaderFactory().getReader(this),
                 ModelReader.getModel(this, R.raw.basic_model).entrySet(), analyseRunnable);
         dataHelper = new DataHelper(this);
         geocoder = new Geocoder(this);
 
         names.putAll(dataHelper.getUnnamedLocations());
 
         handler.postDelayed(scheduleRunnable, POLLING_DELAY);
     }
     
     public void poll() {
         handler.postDelayed(scheduleRunnable, POLLING_DELAY);
 
         Log.v(getClass().getSimpleName(), "Polling...");
 
         pollLocation();
         pollGeolocation();
 
         aggregator.start();
     }
 
     protected void pollLocation() {
         final double newLat = locationMonitor.getLat();
         final double newLon = locationMonitor.getLon();
         final float[] distance = new float[1];
         Location.distanceBetween(newLat, newLon, lat, lon, distance);
 
         if ((lat == 0 && lon == 0) || distance[0] > 500) {
             // New location
             lat = newLat;
             lon = newLon;
             locationCount = 1;
             updateLastLocation(false);
         } else {
             // Existing location
 
             if (++locationCount > LOCATION_REPEATS && location == null) {
                 // But we don't know it yet - add it!
                 final String name = lat + "," + lon;
 
                 final long id = dataHelper.addLocation(name, lat, lon);
                 names.put(name, id);
                 updateLastLocation(true);
             }
         }
     }
 
     protected void pollGeolocation() {
         final Iterator<Map.Entry<String, Long>> it = names.entrySet().iterator();
         while (it.hasNext()) {
             final Map.Entry<String, Long> tuple = it.next();
 
             Log.v(getClass().getSimpleName(), "Attempting to geocode " + tuple.getKey());
             
             final String[] parts = tuple.getKey().split(",", 2);
 
             try {
                 final List<Address> addresses = geocoder.getFromLocation(
                         Double.parseDouble(parts[0]), Double.parseDouble(parts[1]), 1);
 
                 if (addresses != null && !addresses.isEmpty()) {
                     // We found a nice address
 
                     dataHelper.updateLocation(tuple.getValue(),
                             addresses.get(0).getAddressLine(0));
 
                     it.remove();
                 }
             } catch (IOException ex) {
                 // Do nothing
             }
         }
     }
 
     public void updateLastLocation(final boolean added) {
         location = dataHelper.findLocation(lat, lon);
 
         if (location != null) {
             if ((lastLocation == null || !lastLocation.equals(location))) {
                 Log.i(getClass().getSimpleName(), "New location, broadcasting: " + location);
 
                 if (lastLocation != null) {
                     if (added) {
                         // The place was newly added, so for the final N
                         // elements, the user has been at the place.
                         
                        for (int i = 0; i < Math.min(LOCATION_REPEATS, activityLog.size()); i++) {
                             activityLog.remove(activityLog.size() - 1);
                         }
                     }
 
                     if (!activityLog.isEmpty()) {
                         Log.i(getClass().getSimpleName(), "Activity log to here: " + activityLog);
                         dataHelper.addJourney(lastLocation, location, activityLog);
                     }
                 }
 
                 final Intent intent = new Intent(CONTEXT_CHANGED_INTENT);
                 intent.putExtra("type", CONTEXT_PLACE);
                 intent.putExtra("old", lastLocation == null ? -1 : lastLocation.getId());
                 intent.putExtra("new", location.getId());
                 sendBroadcast(intent, Manifest.permission.RECEIVE_UPDATES);
             }
 
             activityLog.clear();
             lastLocation = location;
         }
     }
 
     public void analyse() {
         final String newActivity = aggregator.getClassification();
 
         Log.v(getClass().getSimpleName(), "Aggregator says: " + newActivity);
 
         if (location == null && lastLocation != null) {
             // We're going somewhere - record the activity
             activityLog.add(newActivity);
 
             checkPredictions();
         }
 
         if (!newActivity.equals(lastActivity)) {
             Log.i(getClass().getSimpleName(), "Broadcasting activity change");
 
             final Intent intent = new Intent(ACTIVITY_CHANGED_INTENT);
             intent.putExtra("old", lastActivity);
             intent.putExtra("new", newActivity);
             sendBroadcast(intent, Manifest.permission.RECEIVE_UPDATES);
 
             lastActivity = newActivity;
         }
     }
 
     protected void checkPredictions() {
         final Collection<Journey> journeys = dataHelper.findJourneys(lastLocation);
         final List<JourneyStep> mySteps = JourneyUtil.getSteps(activityLog);
         final Map<Long, Integer> predictions = new HashMap<Long, Integer>();
 
         int total = 0;
         int count = 0;
         int best = 0;
         long bestTarget = -1;
         final Iterator<Journey> it = journeys.iterator();
         while (it.hasNext()) {
             final Journey journey = it.next();
 
             if (journey.getSteps() < mySteps.size()) {
                 it.remove();
                 continue;
             }
 
             final List<JourneyStep> theirSteps = dataHelper.getSteps(journey);
 
             if (JourneyUtil.isCompatible(mySteps, theirSteps)) {
                 total += journey.getNumber();
                 count++;
 
                 int last = predictions.containsKey(journey.getEnd())
                         ? predictions.get(journey.getEnd()) : 0;
                 last += journey.getNumber();
 
                 if (last > best) {
                     best = last;
                     bestTarget = journey.getEnd();
                 }
 
                 predictions.put(journey.getEnd(), last);
             } else {
                 it.remove();
             }
         }
 
         Log.i(getClass().getSimpleName(), "Predictions: " + journeys);
 
         final Intent intent = new Intent(PREDICTION_AVAILABLE_INTENT);
         intent.putExtra("count", count);
         intent.putExtra("best_target", bestTarget);
         intent.putExtra("best_probability", (float) best / total);
         sendBroadcast(intent, Manifest.permission.RECEIVE_UPDATES);
     }
 
     @Override
     public void onDestroy() {
         super.onDestroy();
 
         handler.removeCallbacks(scheduleRunnable);
     }
 
     @Override
     public IBinder onBind(Intent arg0) {
         return null;
     }
 
 }
