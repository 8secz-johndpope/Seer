 /*
  * Copyright (C) 2012 Pixmob (http://github.com/pixmob)
  *
  * Licensed under the Apache License, Version 2.0 (the "License");
  * you may not use this file except in compliance with the License.
  * You may obtain a copy of the License at
  *
  *      http://www.apache.org/licenses/LICENSE-2.0
  *
  * Unless required by applicable law or agreed to in writing, software
  * distributed under the License is distributed on an "AS IS" BASIS,
  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  * See the License for the specific language governing permissions and
  * limitations under the License.
  */
 package org.pixmob.freemobile.netstat.ui;
 
 import static org.pixmob.freemobile.netstat.BuildConfig.DEBUG;
 import static org.pixmob.freemobile.netstat.Constants.TAG;
 
 import org.pixmob.freemobile.netstat.Event;
 import org.pixmob.freemobile.netstat.MobileOperator;
 import org.pixmob.freemobile.netstat.R;
 import org.pixmob.freemobile.netstat.content.NetstatContract.Events;
 import org.pixmob.freemobile.netstat.ui.StatisticsFragment.Statistics;
 
 import android.app.Activity;
 import android.content.ContentResolver;
 import android.content.Context;
 import android.database.ContentObserver;
 import android.database.Cursor;
 import android.os.Bundle;
 import android.os.Handler;
 import android.os.SystemClock;
 import android.support.v4.app.Fragment;
 import android.support.v4.app.LoaderManager.LoaderCallbacks;
 import android.support.v4.content.AsyncTaskLoader;
 import android.support.v4.content.Loader;
 import android.telephony.TelephonyManager;
 import android.util.Log;
 import android.view.LayoutInflater;
 import android.view.View;
 import android.view.ViewGroup;
 import android.widget.ProgressBar;
 import android.widget.TextView;
 
 /**
  * Fragment showing statistics using charts.
  * @author Pixmob
  */
 public class StatisticsFragment extends Fragment implements
         LoaderCallbacks<Statistics> {
     private static final String STAT_NO_VALUE = "-";
     private ContentObserver contentMonitor;
     private View statisticsGroup;
     private ProgressBar progressBar;
     private MobileNetworkChart mobileNetworkChart;
     private TextView onFreeMobileNetwork;
     private TextView onOrangeNetwork;
     private TextView statMobileNetwork;
     private TextView statMobileCode;
     private TextView statConnectedSince;
     private TextView statStartedSince;
     private TextView statScreenOn;
     private TextView statWifiOn;
     
     @Override
     public void onActivityCreated(Bundle savedInstanceState) {
         super.onActivityCreated(savedInstanceState);
         
         contentMonitor = new ContentObserver(new Handler()) {
             @Override
             public void onChange(boolean selfChange) {
                 super.onChange(selfChange);
                 
                 Log.i(TAG, "Content updated: refresh statistics");
                 refresh();
             }
         };
         
         final Activity a = getActivity();
         statisticsGroup = a.findViewById(R.id.statistics);
         statisticsGroup.setVisibility(View.INVISIBLE);
         progressBar = (ProgressBar) a.findViewById(R.id.states_progress);
         mobileNetworkChart = (MobileNetworkChart) a
                 .findViewById(R.id.mobile_network_chart);
         onOrangeNetwork = (TextView) a.findViewById(R.id.on_orange_network);
         onFreeMobileNetwork = (TextView) a
                 .findViewById(R.id.on_free_mobile_network);
         statMobileNetwork = (TextView) a.findViewById(R.id.stat_mobile_network);
         statMobileCode = (TextView) a.findViewById(R.id.stat_mobile_code);
         statConnectedSince = (TextView) a
                 .findViewById(R.id.stat_connected_since);
         statStartedSince = (TextView) a.findViewById(R.id.stat_started_since);
         statScreenOn = (TextView) a.findViewById(R.id.stat_screen);
         statWifiOn = (TextView) a.findViewById(R.id.stat_wifi);
     }
     
     @Override
     public void onResume() {
         super.onResume();
         
         final ContentResolver cr = getActivity().getContentResolver();
         cr.registerContentObserver(Events.CONTENT_URI, true, contentMonitor);
         
         refresh();
     }
     
     @Override
     public void onPause() {
         super.onPause();
         getActivity().getContentResolver().unregisterContentObserver(
             contentMonitor);
     }
     
     @Override
     public View onCreateView(LayoutInflater inflater, ViewGroup container,
             Bundle savedInstanceState) {
         return inflater.inflate(R.layout.statistics_fragment, container, false);
     }
     
     public void refresh() {
         getLoaderManager().restartLoader(0, null, this);
     }
     
     @Override
     public Loader<Statistics> onCreateLoader(int id, Bundle args) {
         progressBar.setVisibility(View.VISIBLE);
         statisticsGroup.setVisibility(View.INVISIBLE);
         
         return new StatisticsLoader(getActivity());
     }
     
     @Override
     public void onLoaderReset(Loader<Statistics> loader) {
     }
     
     @Override
     public void onLoadFinished(Loader<Statistics> loader, Statistics s) {
         Log.i(TAG, "Statistics loaded: " + s);
         
         onOrangeNetwork.setText(s.orangeUsePercent + "%");
         onFreeMobileNetwork.setText(s.freeMobileUsePercent + "%");
         mobileNetworkChart.setData(s.orangeUsePercent, s.freeMobileUsePercent);
         
         final Activity a = getActivity();
         statMobileNetwork.setText(s.mobileOperator.toName(a));
         statMobileCode.setText(s.mobileOperatorCode == null ? STAT_NO_VALUE
                 : s.mobileOperatorCode);
         statConnectedSince.setText(formatDuration(s.connectionTime));
         statStartedSince.setText(formatDuration(s.bootTime));
         statScreenOn.setText(formatDuration(s.screenOnTime));
         statWifiOn.setText(formatDuration(s.wifiOnTime));
         
         progressBar.setVisibility(View.INVISIBLE);
         statisticsGroup.setVisibility(View.VISIBLE);
     }
     
     private CharSequence formatDuration(long duration) {
         if (duration == 0) {
             return STAT_NO_VALUE;
         }
         final long ds = duration / 1000;
         
         final StringBuilder buf = new StringBuilder(32);
         if (ds < 60) {
            buf.append(ds);
         } else if (ds < 3600) {
             final long m = ds / 60;
             buf.append(m).append(getString(R.string.minutes));
             
             final long s = ds - m * 60;
             if (s != 0) {
                 buf.append(" ").append(s).append(getString(R.string.seconds));
             }
         } else if (ds < 86400) {
             final long h = ds / 3600;
             buf.append(h).append(getString(R.string.hours));
             
             final long m = (ds - h * 3600) / 60;
             if (m != 0) {
                 buf.append(" ").append(m).append(getString(R.string.minutes));
             }
             
             final long s = ds - h * 3600 - m * 60;
             if (s != 0) {
                 buf.append(" ").append(s).append(getString(R.string.seconds));
             }
         } else {
             final long d = ds / 86400;
             buf.append(d).append(getString(R.string.days));
             
             final long h = (ds - d * 86400) / 3600;
             if (h != 0) {
                 buf.append(" ").append(h).append(getString(R.string.hours));
             }
             
             final long m = (ds - d * 86400 - h * 3600) / 60;
             if (m != 0) {
                 buf.append(" ").append(m).append(getString(R.string.minutes));
             }
             
             final long s = ds - d * 86400 - h * 3600 - m * 60;
             if (s != 0) {
                 buf.append(" ").append(s).append(getString(R.string.seconds));
             }
         }
         
         return buf;
     }
     /**
      * {@link Loader} implementation for loading events from the database, and
      * computing statistics.
      * @author Pixmob
      */
     private static class StatisticsLoader extends AsyncTaskLoader<Statistics> {
         public StatisticsLoader(final Context context) {
             super(context);
         }
         
         @Override
         protected void onStartLoading() {
             super.onStartLoading();
             forceLoad();
         }
         
         @Override
         public Statistics loadInBackground() {
             final long start = System.currentTimeMillis();
             if (DEBUG) {
                 Log.d(TAG, "Loading statistics from ContentProvider");
             }
             
             final long now = System.currentTimeMillis();
             final long deviceBootTimestamp = now
                     - SystemClock.elapsedRealtime();
             final Statistics s = new Statistics();
             
             final TelephonyManager tm = (TelephonyManager) getContext()
                     .getSystemService(Context.TELEPHONY_SERVICE);
             s.mobileOperatorCode = tm.getNetworkOperator();
             s.mobileOperator = MobileOperator.fromString(s.mobileOperatorCode);
             if (s.mobileOperator == null) {
                 s.mobileOperatorCode = null;
             }
             
             long orangeNetworkTime = 0;
             long freeMobileNetworkTime = 0;
             
             Cursor c = null;
             try {
                 c = getContext().getContentResolver().query(
                     Events.CONTENT_URI,
                     new String[] { Events.TIMESTAMP, Events.SCREEN_ON,
                             Events.WIFI_CONNECTED, Events.MOBILE_CONNECTED,
                             Events.MOBILE_OPERATOR, Events.BATTERY_LEVEL },
                     Events.TIMESTAMP + ">?",
                     new String[] { String.valueOf(deviceBootTimestamp) },
                     Events.TIMESTAMP + " ASC");
                 final int rowCount = c.getCount();
                 s.events = new Event[rowCount];
                 for (int i = 0; c.moveToNext(); ++i) {
                     final Event e = new Event();
                     e.read(c);
                     s.events[i] = e;
                     
                     if (i > 0) {
                         final Event e0 = s.events[i - 1];
                         final long dt = e.timestamp - e0.timestamp;
                         
                         if (e.mobileConnected && e0.mobileConnected) {
                             final MobileOperator op = MobileOperator
                                     .fromString(e.mobileOperator);
                             final MobileOperator op0 = MobileOperator
                                     .fromString(e0.mobileOperator);
                             if (op != null && op.equals(op0)) {
                                 if (MobileOperator.ORANGE.equals(op)) {
                                     orangeNetworkTime += dt;
                                 } else if (MobileOperator.FREE_MOBILE
                                         .equals(op)) {
                                     freeMobileNetworkTime += dt;
                                 }
                             }
                         }
                         if (e.wifiConnected && e0.wifiConnected) {
                             s.wifiOnTime += dt;
                         }
                         if (e.screenOn && e0.screenOn) {
                             s.screenOnTime += dt;
                         }
                     }
                 }
                 
                 final double sTime = (double) (now - deviceBootTimestamp);
                 s.freeMobileUsePercent = (int) Math.round(freeMobileNetworkTime
                         / sTime * 100d);
                 s.orangeUsePercent = (int) Math.round(orangeNetworkTime / sTime
                         * 100d);
                 s.bootTime = now - deviceBootTimestamp;
                 
                 if (MobileOperator.FREE_MOBILE.equals(s.mobileOperator)) {
                     s.connectionTime = freeMobileNetworkTime;
                 } else if (MobileOperator.ORANGE.equals(s.mobileOperator)) {
                     s.connectionTime = orangeNetworkTime;
                 }
             } catch (Exception e) {
                 Log.e(TAG, "Failed to load statistics", e);
                 s.events = new Event[0];
             } finally {
                 try {
                     if (c != null) {
                         c.close();
                     }
                 } catch (Exception ignore) {
                 }
             }
             
             if (DEBUG) {
                 final long end = System.currentTimeMillis();
                 Log.d(TAG, "Statistics loaded in " + (end - start) + " ms");
             }
             
             return s;
         }
     }
     
     /**
      * Store statistics.
      * @author Pixmob
      */
     public static class Statistics {
         public Event[] events = new Event[0];
         public int orangeUsePercent;
         public int freeMobileUsePercent;
         public MobileOperator mobileOperator;
         public String mobileOperatorCode;
         public long connectionTime;
         public long bootTime;
         public long screenOnTime;
         public long wifiOnTime;
         
         @Override
         public String toString() {
             return "Statistics[events=" + events.length + "; orange="
                     + orangeUsePercent + "%; free=" + freeMobileUsePercent
                     + "%]";
         }
     }
 }
