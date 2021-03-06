 /*
  * Copyright (C) 2012 The Android Open Source Project
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
 
 package com.android.deskclock;
 
 import android.app.AlarmManager;
 import android.app.PendingIntent;
 import android.content.BroadcastReceiver;
 import android.content.Context;
 import android.content.Intent;
 import android.content.IntentFilter;
 import android.content.SharedPreferences;
 import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
 import android.os.Bundle;
 import android.preference.PreferenceManager;
 import android.provider.Settings;
 import android.text.TextUtils;
 import android.text.format.DateFormat;
 import android.util.Log;
 import android.view.LayoutInflater;
 import android.view.View;
 import android.view.View.OnClickListener;
 import android.view.ViewGroup;
 import android.view.animation.AnimationUtils;
 import android.widget.BaseAdapter;
 import android.widget.ListView;
 import android.widget.TextView;
 
 import com.android.deskclock.DeskClock.OnTapListener;
 import com.android.deskclock.worldclock.Cities;
 import com.android.deskclock.worldclock.CityObj;
 
 import java.text.Collator;
 import java.util.Arrays;
 import java.util.Calendar;
 import java.util.Comparator;
 import java.util.Locale;
 import java.util.TimeZone;
 
 
 /**
  * Fragment that shows  the clock (analog or digital), the next alarm info and the world clock.
  */
 
 public class ClockFragment extends DeskClockFragment implements OnSharedPreferenceChangeListener {
 
     private static final String BUTTONS_HIDDEN_KEY = "buttons_hidden";
     private static final String ACTION_ON_QUARTER_HOUR = "com.android.deskclock.ON_QUARTER_HOUR";
     private final static String TAG = "ClockFragment";
 
     private View mButtons;
     private boolean mButtonsHidden = false;
     private View mDigitalClock, mAnalogClock, mClockFrame;
     private WorldClockAdapter mAdapter;
     private ListView mList;
     private String mClockStyle;
     private SharedPreferences mPrefs;
     private final Collator mCollator = Collator.getInstance();
    private String mDateFormat;
 
     private PendingIntent mQuarterlyIntent;
     private final BroadcastReceiver mIntentReceiver = new BroadcastReceiver() {
             @Override
         public void onReceive(Context context, Intent intent) {
             if (intent.getAction().equals(ACTION_ON_QUARTER_HOUR)
                     || intent.getAction().equals(Intent.ACTION_TIME_CHANGED)
                     || intent.getAction().equals(Intent.ACTION_TIMEZONE_CHANGED)) {
                 updateDate();
                 if (mAdapter != null) {
                     mAdapter.notifyDataSetChanged();
                 }
                 if (intent.getAction().equals(Intent.ACTION_TIME_CHANGED)
                         || intent.getAction().equals(Intent.ACTION_TIMEZONE_CHANGED)) {
                     refreshAlarm();
                 }
             }
         }
     };
 
     public ClockFragment() {
     }
 
     @Override
     public View onCreateView(LayoutInflater inflater, ViewGroup container,
                              Bundle icicle) {
         // Inflate the layout for this fragment
         View v = inflater.inflate(R.layout.clock_fragment, container, false);
         mButtons = v.findViewById(R.id.clock_buttons);
         if (icicle != null) {
             mButtonsHidden = icicle.getBoolean(BUTTONS_HIDDEN_KEY, false);
         }
         mList = (ListView)v.findViewById(R.id.cities);
         mList.setDivider(null);
         View headerView = inflater.inflate(R.layout.blank_header_view, mList, false);
         headerView.setOnClickListener(new OnClickListener() {
             @Override
             public void onClick(View v) {
                 ((DeskClock) getActivity()).clockOnViewClick(v);
             }
         });
         mList.addHeaderView(headerView);
         mClockFrame = inflater.inflate(R.layout.main_clock_frame, mList, false);
         mDigitalClock = mClockFrame.findViewById(R.id.main_digital_clock);
         mAnalogClock = mClockFrame.findViewById(R.id.main_analog_clock);
         mList.addHeaderView(mClockFrame, null, false);
         View footerView = inflater.inflate(R.layout.blank_footer_view, mList, false);
         footerView.setOnClickListener(new OnClickListener() {
             @Override
             public void onClick(View v) {
                 ((DeskClock) getActivity()).clockOnViewClick(v);
             }
         });
         mList.addFooterView(footerView);
         mAdapter = new WorldClockAdapter(getActivity());
         mList.setAdapter(mAdapter);
         mList.setOnTouchListener(new OnTapListener(getActivity()));
         mPrefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
         return v;
     }
 
     @Override
     public void onResume () {
         super.onResume();
         mPrefs.registerOnSharedPreferenceChangeListener(this);
        mDateFormat = getString(R.string.abbrev_wday_month_day_no_year);
 
         // Setup to find out when the quarter-hour changes (e.g. Kathmandu is GMT+5:45)
         Calendar nextQuarter = Calendar.getInstance();
         //  Set 1 second to ensure quarter-hour threshold passed.
         nextQuarter.set(Calendar.SECOND, 1);
         int minute = nextQuarter.get(Calendar.MINUTE);
         nextQuarter.add(Calendar.MINUTE, 15 - (minute % 15));
         long alarmOnQuarterHour = nextQuarter.getTimeInMillis();
         if (0 >= (alarmOnQuarterHour - System.currentTimeMillis())
                 || (alarmOnQuarterHour - System.currentTimeMillis()) > 901000) {
             Log.wtf(TAG, "quarterly alarm calculation error");
         }
         mQuarterlyIntent = PendingIntent.getBroadcast(getActivity(), 0, new Intent(ACTION_ON_QUARTER_HOUR), 0);
         ((AlarmManager) getActivity().getSystemService(Context.ALARM_SERVICE)).setRepeating(
                 AlarmManager.RTC, alarmOnQuarterHour, AlarmManager.INTERVAL_FIFTEEN_MINUTES, mQuarterlyIntent);
         // Besides monitoring when quarter-hour changes, monitor other actions that effect clock time
         IntentFilter filter = new IntentFilter(ACTION_ON_QUARTER_HOUR);
         filter.addAction(Intent.ACTION_TIME_CHANGED);
         filter.addAction(Intent.ACTION_TIMEZONE_CHANGED);
         getActivity().registerReceiver(mIntentReceiver, filter);
 
         mButtons.setAlpha(mButtonsHidden ? 0 : 1);
 
         // Resume can invoked after changing the cities list.
         if (mAdapter != null) {
             mAdapter.reloadData(getActivity());
         }
         // Resume can invoked after changing the clock style.
         mClockStyle = mPrefs.getString(SettingsActivity.KEY_CLOCK_STYLE, "digital");
         setClockStyle();
         updateDate();
         refreshAlarm();
     }
 
     @Override
     public void onPause() {
         super.onPause();
         mPrefs.unregisterOnSharedPreferenceChangeListener(this);
         ((AlarmManager) getActivity().getSystemService(Context.ALARM_SERVICE)).cancel(mQuarterlyIntent);
         getActivity().unregisterReceiver(mIntentReceiver);
     }
 
     @Override
     public void onSaveInstanceState (Bundle outState) {
         outState.putBoolean(BUTTONS_HIDDEN_KEY, mButtonsHidden);
         super.onSaveInstanceState(outState);
     }
 
     private void setClockStyle() {
         if (mClockStyle.equals("analog")) {
             mDigitalClock.setVisibility(View.GONE);
             mAnalogClock.setVisibility(View.VISIBLE);
         } else {
             mDigitalClock.setVisibility(View.VISIBLE);
             mAnalogClock.setVisibility(View.GONE);
         }
         mAdapter.notifyDataSetChanged();
     }
 
     private void refreshAlarm() {
         String nextAlarm = Settings.System.getString(getActivity().getContentResolver(),
                 Settings.System.NEXT_ALARM_FORMATTED);
         TextView nextAlarmView;
         View slash;
         if (mClockStyle.equals("analog")) {
             nextAlarmView = (TextView)mAnalogClock.findViewById(R.id.nextAlarm);
             slash = mAnalogClock.findViewById(R.id.slash);
         } else {
             nextAlarmView = (TextView)mDigitalClock.findViewById(R.id.nextAlarm_digital);
             slash = mDigitalClock.findViewById(R.id.slash_digital);
         }
         if (!TextUtils.isEmpty(nextAlarm) && nextAlarmView != null) {
             nextAlarmView.setText(getString(R.string.control_set_alarm_with_existing, nextAlarm));
             nextAlarmView.setVisibility(View.VISIBLE);
             slash.setVisibility(View.VISIBLE);
         } else  {
             nextAlarmView.setVisibility(View.GONE);
             slash.setVisibility(View.GONE);
         }
     }
 
     private void updateDate() {
         Calendar cal = Calendar.getInstance();
         cal.setTimeInMillis(System.currentTimeMillis());
 
        CharSequence newDate = DateFormat.format(mDateFormat, cal);
         TextView dateDisplay;
         if (mClockStyle.equals("analog")) {
             dateDisplay = (TextView)mAnalogClock.findViewById(R.id.date);
         } else {
             dateDisplay = (TextView)mDigitalClock.findViewById(R.id.date_digital);
         }
         if (dateDisplay != null) {
             dateDisplay.setVisibility(View.VISIBLE);
             dateDisplay.setText(newDate);
         }
     }
 
     public void showButtons(boolean show) {
         if (mButtons == null) {
             return;
         }
         if (show && mButtonsHidden) {
             mButtons.startAnimation(
                     AnimationUtils.loadAnimation(getActivity(), R.anim.unhide));
             mButtonsHidden = false;
         } else if (!show && !mButtonsHidden) {
             mButtons.startAnimation(
                     AnimationUtils.loadAnimation(getActivity(), R.anim.hide));
             mButtonsHidden = true;
         }
     }
 
     @Override
     public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
         if (key == SettingsActivity.KEY_CLOCK_STYLE) {
             mClockStyle = prefs.getString(SettingsActivity.KEY_CLOCK_STYLE, "digital");
         }
     }
 
     private class WorldClockAdapter extends BaseAdapter {
         Object [] mCitiesList;
         LayoutInflater mInflater;
         @SuppressWarnings("hiding")
         Context mContext;
 
         public WorldClockAdapter(Context context) {
             super();
             mContext = context;
             loadData(context);
             mInflater = LayoutInflater.from(context);
         }
 
         public void reloadData(Context context) {
             loadData(context);
             notifyDataSetChanged();
         }
 
         private void loadData(Context context) {
             mCitiesList = Cities.readCitiesFromSharedPrefs(
                     PreferenceManager.getDefaultSharedPreferences(context)).values().toArray();
             sortList();
             mCitiesList = addHomeCity();
         }
 
         /***
          * Adds the home city as the first item of the adapter if the feature is on and the device time
          * zone is different from the home time zone that was set by the user.
          * return the list of cities.
          */
         private Object[] addHomeCity() {
             SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(mContext);
             if (sharedPref.getBoolean(SettingsActivity.KEY_AUTO_HOME_CLOCK, false)) {
                 String homeTZ = sharedPref.getString(SettingsActivity.KEY_HOME_TZ, "");
                 if (!TimeZone.getDefault().getID().equals(homeTZ)) {
                     CityObj c = new CityObj(
                             mContext.getResources().getString(R.string.home_label), homeTZ, null);
                     Object[] temp = new Object[mCitiesList.length + 1];
                     temp[0] = c;
                     for (int i = 0; i < mCitiesList.length; i++) {
                         temp[i + 1] = mCitiesList[i];
                     }
                     return temp;
                 }
             }
             return mCitiesList;
         }
 
         private void sortList() {
             final Calendar now = Calendar.getInstance();
 
             // Sort by the Offset from GMT taking DST into account
             // and if the same sort by City Name
             Arrays.sort(mCitiesList, new Comparator<Object>() {
                 private int safeCityNameCompare(CityObj city1, CityObj city2) {
                     if (city1.mCityName == null && city2.mCityName == null) {
                         return 0;
                     } else if (city1.mCityName == null) {
                         return -1;
                     } else if (city2.mCityName == null) {
                         return 1;
                     } else {
                         return mCollator.compare(city1.mCityName, city2.mCityName);
                     }
                 }
 
                 @Override
                 public int compare(Object object1, Object object2) {
                     CityObj city1 = (CityObj) object1;
                     CityObj city2 = (CityObj) object2;
 
                     if (city1.mTimeZone == null && city2.mTimeZone == null) {
                         return safeCityNameCompare(city1, city2);
                     } else if (city1.mTimeZone == null) {
                         return -1;
                     } else if (city2.mTimeZone == null) {
                         return 1;
                     }
 
                     int gmOffset1 = TimeZone.getTimeZone(city1.mTimeZone).getOffset(
                             now.get(Calendar.ERA), now.get(Calendar.YEAR),
                             now.get(Calendar.MONTH), now.get(Calendar.DAY_OF_MONTH),
                             now.get(Calendar.DAY_OF_WEEK),
                             now.get(Calendar.MILLISECOND));
                     int gmOffset2 = TimeZone.getTimeZone(city2.mTimeZone).getOffset(
                             now.get(Calendar.ERA), now.get(Calendar.YEAR),
                             now.get(Calendar.MONTH), now.get(Calendar.DAY_OF_MONTH),
                             now.get(Calendar.DAY_OF_WEEK),
                             now.get(Calendar.MILLISECOND));
                     if (gmOffset1 == gmOffset2) {
                         return safeCityNameCompare(city1, city2);
                     } else {
                         return gmOffset1 - gmOffset2;
                     }
                 }
             });
         }
 
         @Override
         public int getCount() {
             // Each item in the list holds 1 or 2 clocks
             return (mCitiesList.length  + 1)/2;
         }
 
         @Override
         public Object getItem(int p) {
             return null;
         }
 
         @Override
         public long getItemId(int p) {
             return p;
         }
 
         @Override
         public boolean isEnabled(int p) {
             return false;
         }
 
         @Override
         public View getView(int position, View view, ViewGroup parent) {
             // Index in cities list
             int index = position * 2;
             if (index < 0 || index >= mCitiesList.length) {
                 return null;
             }
 
             if (view == null) {
                 view = mInflater.inflate(R.layout.world_clock_list_item, parent, false);
             }
             // The world clock list item can hold two world clocks
             View rightClock = view.findViewById(R.id.city_right);
             updateView(view.findViewById(R.id.city_left), (CityObj)mCitiesList[index]);
             if (index + 1 < mCitiesList.length) {
                 rightClock.setVisibility(View.VISIBLE);
                 updateView(rightClock, (CityObj)mCitiesList[index + 1]);
             } else {
                 // To make sure the spacing is right , make sure that the right clock style is selected
                 // even if the clock is invisible.
                 DigitalClock dclock = (DigitalClock)(rightClock.findViewById(R.id.digital_clock));
                 AnalogClock aclock = (AnalogClock)(rightClock.findViewById(R.id.analog_clock));
                 if (mClockStyle.equals("analog")) {
                     dclock.setVisibility(View.GONE);
                     aclock.setVisibility(View.INVISIBLE);
                 } else {
                     dclock.setVisibility(View.INVISIBLE);
                     aclock.setVisibility(View.GONE);
                 }
                 rightClock.setVisibility(View.INVISIBLE);
             }
 
             return view;
         }
 
         private void updateView(View clock, CityObj cityObj) {
             View nameDigital= clock.findViewById(R.id.city_name_digital);
             View nameAnalog = clock.findViewById(R.id.city_name_analog);
             TextView name;
             TextView dayOfWeek;
             DigitalClock dclock = (DigitalClock)(clock.findViewById(R.id.digital_clock));
             AnalogClock aclock = (AnalogClock)(clock.findViewById(R.id.analog_clock));
 
             if (mClockStyle.equals("analog")) {
                 dclock.setVisibility(View.GONE);
                 nameDigital.setVisibility(View.GONE);
                 aclock.setVisibility(View.VISIBLE);
                 nameAnalog.setVisibility(View.VISIBLE);
                 aclock.setTimeZone(cityObj.mTimeZone);
                 aclock.enableSeconds(false);
                 name = (TextView)(nameAnalog.findViewById(R.id.city_name));
                 dayOfWeek = (TextView)(nameAnalog.findViewById(R.id.city_day));
             } else {
                 dclock.setVisibility(View.VISIBLE);
                 nameDigital.setVisibility(View.VISIBLE);
                 dclock.setTimeZone(cityObj.mTimeZone);
                 aclock.setVisibility(View.GONE);
                 nameAnalog.setVisibility(View.GONE);
                 name = (TextView)(nameDigital.findViewById(R.id.city_name));
                 dayOfWeek = (TextView)(nameDigital.findViewById(R.id.city_day));
             }
             name.setText(cityObj.mCityName);
             final Calendar now = Calendar.getInstance();
             now.setTimeZone(TimeZone.getDefault());
             int myDayOfWeek = now.get(Calendar.DAY_OF_WEEK);
             now.setTimeZone(TimeZone.getTimeZone(cityObj.mTimeZone));
             int cityDayOfWeek = now.get(Calendar.DAY_OF_WEEK);
             if (myDayOfWeek != cityDayOfWeek) {
                 dayOfWeek.setText(getString(R.string.world_day_of_week_label, now.getDisplayName(
                         Calendar.DAY_OF_WEEK, Calendar.SHORT, Locale.getDefault())));
                 dayOfWeek.setVisibility(View.VISIBLE);
             } else {
                 dayOfWeek.setVisibility(View.GONE);
             }
         }
     }
 
 }
