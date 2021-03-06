 /*
  * Copyright (C) 2013 The Android Open Source Project
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
 
 package com.android.datetimepicker.date;
 
 import android.app.Activity;
 import android.app.DialogFragment;
 import android.os.Bundle;
 import android.support.v4.view.PagerAdapter;
 import android.support.v4.view.ViewPager;
 import android.support.v4.view.ViewPager.OnPageChangeListener;
 import android.util.Log;
 import android.view.LayoutInflater;
 import android.view.View;
 import android.view.View.OnClickListener;
 import android.view.ViewGroup;
 import android.view.Window;
 import android.widget.Button;
 import android.widget.TextView;
 
 import com.android.datetimepicker.R;
 import com.android.datetimepicker.Utils;
 import com.android.datetimepicker.date.SimpleMonthAdapter.CalendarDay;
 
 import java.text.SimpleDateFormat;
 import java.util.Calendar;
 import java.util.Locale;
 
 /**
  * Dialog allowing users to select a date.
  */
 public class DatePickerDialog extends DialogFragment implements
         OnPageChangeListener, OnClickListener, DatePickerController {
 
     private static final String TAG = "DatePickerDialog";
 
     private static final int TOTAL_VIEWS = 3;
 
     private static final int MONTH_VIEW = 0;
     private static final int DAY_VIEW = 1;
     private static final int YEAR_VIEW = 2;
 
     private static String KEY_SELECTED_YEAR = "year";
     private static String KEY_SELECTED_MONTH = "month";
     private static String KEY_SELECTED_DAY = "day";
     private static String KEY_LIST_POSITION = "position";
     private static String KEY_WEEK_START = "week_start";
     private static String KEY_YEAR_START = "year_start";
     private static String KEY_YEAR_END = "year_end";
 
     private static final int DEFAULT_START_YEAR = 1900;
     private static final int DEFAULT_END_YEAR = 2100;
 
     private static SimpleDateFormat YEAR_FORMAT = new SimpleDateFormat("yyyy", Locale.getDefault());
     private static SimpleDateFormat DAY_FORMAT = new SimpleDateFormat("dd", Locale.getDefault());
 
     private final Calendar mCalendar = Calendar.getInstance();
     private OnDateSetListener mCallBack;
 
     private ViewPager mPager;
 
     private TextView mDayOfWeekView;
     private TextView mMonthView;
     private TextView mDayOfMonthView;
     private TextView mYearView;
     private MonthPickerView mMonthPickerView;
     private DayPickerView mDayPickerView;
     private YearPickerView mYearPickerView;
     private Button mDoneButton;
 
     private int mWeekStart = mCalendar.getFirstDayOfWeek();
     private int mMinYear = DEFAULT_START_YEAR;
     private int mMaxYear = DEFAULT_END_YEAR;
 
     private final View[] mViews = new View[TOTAL_VIEWS];
 
     /**
      * The callback used to indicate the user is done filling in the date.
      */
     public interface OnDateSetListener {
 
         /**
          * @param view The view associated with this listener.
          * @param year The year that was set.
          * @param monthOfYear The month that was set (0-11) for compatibility
          *            with {@link java.util.Calendar}.
          * @param dayOfMonth The day of the month that was set.
          */
         void onDateSet(DatePickerDialog dialog, int year, int monthOfYear, int dayOfMonth);
     }
 
     public DatePickerDialog() {
         // Empty constructor required for dialog fragment.
     }
 
     /**
      * @param callBack How the parent is notified that the date is set.
      * @param year The initial year of the dialog.
      * @param monthOfYear The initial month of the dialog.
      * @param dayOfMonth The initial day of the dialog.
      */
     public static DatePickerDialog newInstance(OnDateSetListener callBack, int year,
             int monthOfYear,
             int dayOfMonth) {
         DatePickerDialog ret = new DatePickerDialog();
         ret.initialize(callBack, year, monthOfYear, dayOfMonth);
         return ret;
     }
 
     public void initialize(OnDateSetListener callBack, int year, int monthOfYear, int dayOfMonth) {
         mCallBack = callBack;
         mCalendar.set(Calendar.YEAR, year);
         mCalendar.set(Calendar.MONTH, monthOfYear);
         mCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
     }
 
     @Override
     public void onCreate(Bundle savedInstanceState) {
         super.onCreate(savedInstanceState);
         if (savedInstanceState != null) {
             mCalendar.set(Calendar.YEAR, savedInstanceState.getInt(KEY_SELECTED_YEAR));
             mCalendar.set(Calendar.MONTH, savedInstanceState.getInt(KEY_SELECTED_MONTH));
             mCalendar.set(Calendar.DAY_OF_MONTH, savedInstanceState.getInt(KEY_SELECTED_DAY));
         }
     }
 
     @Override
     public void onSaveInstanceState(Bundle outState) {
         super.onSaveInstanceState(outState);
         outState.putInt(KEY_SELECTED_YEAR, mCalendar.get(Calendar.YEAR));
         outState.putInt(KEY_SELECTED_MONTH, mCalendar.get(Calendar.MONTH));
         outState.putInt(KEY_SELECTED_DAY, mCalendar.get(Calendar.DAY_OF_MONTH));
         outState.putInt(KEY_LIST_POSITION, mDayPickerView.getFirstVisiblePosition());
         outState.putInt(KEY_WEEK_START, mWeekStart);
         outState.putInt(KEY_YEAR_START, mMinYear);
         outState.putInt(KEY_YEAR_END, mMaxYear);
     }
 
     @Override
     public View onCreateView(LayoutInflater inflater, ViewGroup container,
             Bundle savedInstanceState) {
         Log.d(TAG, "onCreateView: ");
         getDialog().getWindow().requestFeature(Window.FEATURE_NO_TITLE);
 
         View view = inflater.inflate(R.layout.date_picker_dialog, null);
 
         mDayOfWeekView = (TextView) view.findViewById(R.id.date_picker_header);
         mMonthView = (TextView) view.findViewById(R.id.date_picker_month);
         mMonthView.setOnClickListener(this);
         mDayOfMonthView = (TextView) view.findViewById(R.id.date_picker_day);
         mDayOfMonthView.setOnClickListener(this);
         mYearView = (TextView) view.findViewById(R.id.date_picker_year);
         mYearView.setOnClickListener(this);
         final Activity activity = getActivity();
 
         mMonthPickerView = new MonthPickerView(activity, this);
         mDayPickerView = new DayPickerView(activity, this);
         if (savedInstanceState != null) {
             Log.d(TAG,
                     "Setting first visible position: "
                             + savedInstanceState.getInt(KEY_LIST_POSITION));
             mDayPickerView.setSelectionFromTop(savedInstanceState.getInt(KEY_LIST_POSITION),
                     DayPickerView.LIST_TOP_OFFSET);
             mWeekStart = savedInstanceState.getInt(KEY_WEEK_START);
             mMinYear = savedInstanceState.getInt(KEY_YEAR_START);
             mMaxYear = savedInstanceState.getInt(KEY_YEAR_END);
         }
         mYearPickerView = new YearPickerView(activity, this);
 
         mViews[MONTH_VIEW] = mMonthPickerView;
         mViews[DAY_VIEW] = mDayPickerView;
         mViews[YEAR_VIEW] = mYearPickerView;
 
         mPager = (ViewPager) view.findViewById(R.id.pager);
         mPager.setAdapter(new DatePickerPagerAdapter());
         mPager.setOnPageChangeListener(this);
         mPager.setCurrentItem(DAY_VIEW);
 
         mDoneButton = (Button) view.findViewById(R.id.done);
         mDoneButton.setOnClickListener(new OnClickListener() {
 
             @Override
             public void onClick(View v) {
                 if (mCallBack != null) {
                     mCallBack.onDateSet(DatePickerDialog.this, mCalendar.get(Calendar.YEAR),
                             mCalendar.get(Calendar.MONTH), mCalendar.get(Calendar.DAY_OF_MONTH));
                 }
                 dismiss();
             }
         });
 
         updateDisplay();
 
         return view;
     }
 
     private void updateDisplay() {
         mDayOfWeekView.setText(mCalendar.getDisplayName(Calendar.DAY_OF_WEEK, Calendar.LONG,
                 Locale.getDefault()).toUpperCase(Locale.getDefault()));
         mMonthView.setText(mCalendar.getDisplayName(Calendar.MONTH, Calendar.SHORT,
                 Locale.getDefault()).toUpperCase(Locale.getDefault()));
         mDayOfMonthView.setText(DAY_FORMAT.format(mCalendar.getTime()));
         mYearView.setText(YEAR_FORMAT.format(mCalendar.getTime()));
     }
 
     public void setFirstDayOfWeek(int startOfWeek) {
         if (startOfWeek < Calendar.SUNDAY || startOfWeek > Calendar.SATURDAY) {
             throw new IllegalArgumentException("Value must be between Calendar.SUNDAY and " +
                     "Calendar.SATURDAY");
         }
         mWeekStart = startOfWeek;
         if (mDayPickerView != null) {
             mDayPickerView.onChange();
         }
     }
 
     public void setYearRange(int startYear, int endYear) {
         if (endYear <= startYear) {
             throw new IllegalArgumentException("Year end must be larger than year start");
         }
         mMinYear = startYear;
         mMaxYear = endYear;
         if (mDayPickerView != null) {
             mDayPickerView.onChange();
             mYearPickerView.onChange();
         }
     }
 
     public void setOnDateSetListener(OnDateSetListener listener) {
         mCallBack = listener;
     }
 
     private class DatePickerPagerAdapter extends PagerAdapter {
 
         @Override
         public Object instantiateItem(ViewGroup container, int position) {
             container.addView(mViews[position]);
             return mViews[position];
         }
 
         @Override
         public void destroyItem(ViewGroup container, int position, Object view) {
             container.removeView((View) view);
         }
 
         @Override
         public int getCount() {
             return mViews.length;
         }
 
         @Override
         public boolean isViewFromObject(View view, Object object) {
             return view == object;
         }
     }
 
     // If the newly selected month / year does not contain the currently selected day number,
     // change the selected day number to the last day of the selected month or year.
     //      e.g. Switching from Mar to Apr when Mar 31 is selected -> Apr 30
     //      e.g. Switching from 2012 to 2013 when Feb 29, 2012 is selected -> Feb 28, 2013
     private void adjustDayInMonthIfNeeded(int month, int year) {
         int day = mCalendar.get(Calendar.DAY_OF_MONTH);
         int daysInMonth = Utils.getDaysInMonth(month, year);
         if (day > daysInMonth) {
             mCalendar.set(Calendar.DAY_OF_MONTH, daysInMonth);
         }
     }
 
     @Override
     public void onPageScrollStateChanged(int arg0) {
         // TODO Auto-generated method stub
 
     }
 
     @Override
     public void onPageScrolled(int arg0, float arg1, int arg2) {
         // TODO Auto-generated method stub
     }
 
     @Override
     public void onPageSelected(int index) {
         switch (index) {
             case MONTH_VIEW:
                 mMonthView.setSelected(true);
                 mDayOfMonthView.setSelected(false);
                 mYearView.setSelected(false);
                 break;
             case DAY_VIEW:
                 mMonthView.setSelected(false);
                 mDayOfMonthView.setSelected(true);
                 mYearView.setSelected(false);
                 break;
             case YEAR_VIEW:
                 mMonthView.setSelected(false);
                 mDayOfMonthView.setSelected(false);
                 mYearView.setSelected(true);
                 break;
         }
     }
 
     @Override
     public void onClick(View v) {
         if (v.getId() == R.id.date_picker_year) {
             mPager.setCurrentItem(YEAR_VIEW);
         } else if (v.getId() == R.id.date_picker_month) {
             mPager.setCurrentItem(MONTH_VIEW);
         } else if (v.getId() == R.id.date_picker_day) {
             mPager.setCurrentItem(DAY_VIEW);
         }
     }
 
     @Override
     public void onYearPickerSelectionChanged(int year) {
         adjustDayInMonthIfNeeded(mCalendar.get(Calendar.MONTH), year);
         mCalendar.set(Calendar.YEAR, year);
         mDayPickerView.setCalendarDate(getSelectedDay());
         updateDisplay();
     }
 
     @Override
     public void onMonthPickerSelectionChanged(int month) {
         adjustDayInMonthIfNeeded(month, mCalendar.get(Calendar.YEAR));
         mCalendar.set(Calendar.MONTH, month);
         mDayPickerView.setCalendarDate(getSelectedDay());
         mPager.setCurrentItem(DAY_VIEW);
         updateDisplay();
     }
 
     @Override
     public void onDayPickerSelectionChanged(int year, int month, int day) {
         mCalendar.set(Calendar.YEAR, year);
         mCalendar.set(Calendar.MONTH, month);
         mCalendar.set(Calendar.DAY_OF_MONTH, day);
         mYearPickerView.setValue(mCalendar.get(Calendar.YEAR));
         updateDisplay();
     }
 
 
     @Override
     public CalendarDay getSelectedDay() {
         return new CalendarDay(mCalendar);
     }
 
     @Override
     public int getMinYear() {
         return mMinYear;
     }
 
     @Override
     public int getMaxYear() {
         return mMaxYear;
     }
 
     @Override
     public int getFirstDayOfWeek() {
         return mWeekStart;
     }
 }
