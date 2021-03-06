 /*
  * Copyright (C) 2010 The Android Open Source Project
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
 
 package com.android.calendar.month;
 
 import com.android.calendar.Event;
 import com.android.calendar.R;
 import com.android.calendar.Utils;
 import android.content.Context;
 import android.content.res.Configuration;
 import android.content.res.Resources;
 import android.graphics.Canvas;
 import android.graphics.Color;
 import android.graphics.Paint;
 import android.graphics.Paint.Align;
 import android.graphics.Paint.Style;
 import android.graphics.Typeface;
 import android.graphics.drawable.Drawable;
 import android.text.TextPaint;
 import android.text.TextUtils;
 import android.text.format.DateUtils;
 import android.text.format.Time;
 import android.util.Log;
 
 import java.util.ArrayList;
 import java.util.Arrays;
 import java.util.Formatter;
 import java.util.HashMap;
 import java.util.Iterator;
 import java.util.List;
 import java.util.Locale;
 
 public class MonthWeekEventsView extends SimpleWeekView {
 
     private static final String TAG = "MonthView";
 
     public static final String VIEW_PARAMS_ORIENTATION = "orientation";
 
     private static int TEXT_SIZE_MONTH_NUMBER = 32;
     private static int TEXT_SIZE_EVENT = 14;
     private static int TEXT_SIZE_MORE_EVENTS = 12;
     private static int TEXT_SIZE_MONTH_NAME = 14;
     private static int TEXT_SIZE_WEEK_NUM = 12;
 
     private static int DNA_MARGIN = 4;
     private static int DNA_ALL_DAY_HEIGHT = 4;
     private static int DNA_MIN_SEGMENT_HEIGHT = 4;
     private static int DNA_WIDTH = 8;
     private static int DNA_ALL_DAY_WIDTH = 32;
     private static int DNA_SIDE_PADDING = 6;
     private static int CONFLICT_COLOR = Color.BLACK;
 
     private static int DEFAULT_EDGE_SPACING = 0;
     private static int SIDE_PADDING_MONTH_NUMBER = 4;
     private static int TOP_PADDING_MONTH_NUMBER = 6;
     private static int TOP_PADDING_WEEK_NUMBER = 4;
     private static int SIDE_PADDING_WEEK_NUMBER = 20;
     private static int DAY_SEPARATOR_OUTER_WIDTH = 0;
     private static int DAY_SEPARATOR_INNER_WIDTH = 1;
     private static int DAY_SEPARATOR_VERTICAL_LENGTH = 53;
     private static int DAY_SEPARATOR_VERTICAL_LENGHT_PORTRAIT = 64;
     private static int MIN_WEEK_WIDTH = 50;
 
     private static int EVENT_X_OFFSET_LANDSCAPE = 44;
     private static int EVENT_Y_OFFSET_LANDSCAPE = 11;
     private static int EVENT_Y_OFFSET_PORTRAIT = 16;
     private static int EVENT_SQUARE_WIDTH = 10;
     private static int EVENT_SQUARE_BORDER = 1;
     private static int EVENT_LINE_PADDING = 4;
     private static int EVENT_RIGHT_PADDING = 4;
     private static int EVENT_BOTTOM_PADDING = 15;
 
 
     private static int SPACING_WEEK_NUMBER = 24;
     private static boolean mScaled = false;
     private static boolean mShowDetailsInMonth;
 
     protected Time mToday = new Time();
     protected boolean mHasToday = false;
     protected int mTodayIndex = -1;
     protected int mOrientation = Configuration.ORIENTATION_LANDSCAPE;
     protected List<ArrayList<Event>> mEvents = null;
     HashMap<Integer, Utils.DNAStrand> mDna = null;
     // This is for drawing the outlines around event chips and supports up to 10
     // events being drawn on each day. The code will expand this if necessary.
     protected FloatRef mEventOutlines = new FloatRef(10 * 4 * 4 * 7);
 
 
 
     protected static StringBuilder mStringBuilder = new StringBuilder(50);
     // TODO recreate formatter when locale changes
     protected static Formatter mFormatter = new Formatter(mStringBuilder, Locale.getDefault());
 
     protected Paint mMonthNamePaint;
     protected TextPaint mEventPaint;
     protected TextPaint mEventExtrasPaint;
     protected Paint mWeekNumPaint;
     protected Paint mDNAAllDayPaint;
     protected Paint mDNATimePaint;
 
 
     protected Drawable mTodayDrawable;
 
     protected int mMonthNumHeight;
     protected int mEventHeight;
     protected int mExtrasHeight;
     protected int mWeekNumHeight;
 
     protected int mMonthBGColor;
     protected int mMonthBGOtherColor;
     protected int mMonthBGTodayColor;
     protected int mMonthNumColor;
     protected int mMonthNumOtherColor;
     protected int mMonthNumTodayColor;
     protected int mMonthNameColor;
     protected int mMonthNameOtherColor;
     protected int mMonthEventColor;
     protected int mMonthEventExtraColor;
     protected int mMonthEventOtherColor;
     protected int mMonthEventExtraOtherColor;
     protected int mMonthWeekNumColor;
     protected int mMonthBusyBitsBgColor;
     protected int mMonthBusyBitsBusyTimeColor;
     protected int mMonthBusyBitsConflictTimeColor;
 
     protected int mEventChipOutlineColor = 0xFFFFFFFF;
     protected int mDaySeparatorOuterColor = 0x33FFFFFF;
     protected int mDaySeparatorInnerColor = 0xFFFFFFFF;
 
     private int[] mDayXs;
 
     /**
      * This provides a reference to a float array which allows for easy size
      * checking and reallocation. Used for drawing lines.
      */
     private class FloatRef {
         float[] array;
 
         public FloatRef(int size) {
             array = new float[size];
         }
 
         public void ensureSize(int newSize) {
             if (newSize >= array.length) {
                 // Add enough space for 7 more boxes to be drawn
                 array = Arrays.copyOf(array, newSize + 16 * 7);
             }
         }
     }
 
 
     /**
      * @param context
      */
     public MonthWeekEventsView(Context context) {
         super(context);
     }
 
     // Sets the list of events for this week. Takes a sorted list of arrays
     // divided up by day for generating the large month version and the full
     // arraylist sorted by start time to generate the dna version.
     public void setEvents(List<ArrayList<Event>> sortedEvents, ArrayList<Event> unsortedEvents) {
         setEvents(sortedEvents);
         // The MIN_WEEK_WIDTH is a hack to prevent the view from trying to
         // generate dna bits before its width has been fixed.
        if (unsortedEvents == null || mWidth <= MIN_WEEK_WIDTH || mContext == null) {
             mDna = null;
             return;
         }
         // Create the drawing coordinates for dna
         if (!mShowDetailsInMonth) {
             int numDays = mEvents.size();
             int wkNumOffset = 1;
             int effectiveWidth = mWidth - mPadding * 2 - SPACING_WEEK_NUMBER;
             DNA_ALL_DAY_WIDTH = effectiveWidth / numDays - 2 * DNA_SIDE_PADDING;
             mDNAAllDayPaint.setStrokeWidth(DNA_ALL_DAY_WIDTH);
             mDayXs = new int[numDays];
             for (int day = 0; day < numDays; day++) {
                 mDayXs[day] = computeDayLeftPosition(day) + DNA_WIDTH / 2 + DNA_SIDE_PADDING;
 
             }
 
             int top = DAY_SEPARATOR_INNER_WIDTH + DNA_MARGIN + DNA_ALL_DAY_HEIGHT + 1;
             int bottom = mHeight - DNA_MARGIN;
             mDna = Utils.createDNAStrands(mFirstJulianDay, unsortedEvents, top, bottom,
                    DNA_MIN_SEGMENT_HEIGHT, mDayXs, mContext);
         }
     }
 
     public void setEvents(List<ArrayList<Event>> sortedEvents) {
         mEvents = sortedEvents;
         if (sortedEvents == null) {
             return;
         }
         if (sortedEvents.size() != mNumDays) {
             if (Log.isLoggable(TAG, Log.ERROR)) {
                 Log.wtf(TAG, "Events size must be same as days displayed: size="
                         + sortedEvents.size() + " days=" + mNumDays);
             }
             mEvents = null;
             return;
         }
     }
 
     protected void loadColors(Context context) {
         Resources res = context.getResources();
         mMonthWeekNumColor = res.getColor(R.color.month_week_num_color);
         mMonthNumColor = res.getColor(R.color.month_day_number);
         mMonthNumOtherColor = res.getColor(R.color.month_day_number_other);
         mMonthNumTodayColor = res.getColor(R.color.month_today_number);
         mMonthNameColor = mMonthNumColor;
         mMonthNameOtherColor = mMonthNumOtherColor;
         mMonthEventColor = res.getColor(R.color.month_event_color);
         mMonthEventExtraColor = res.getColor(R.color.month_event_extra_color);
         mMonthEventOtherColor = res.getColor(R.color.month_event_other_color);
         mMonthEventExtraOtherColor = res.getColor(R.color.month_event_extra_other_color);
         mMonthBGTodayColor = res.getColor(R.color.month_today_bgcolor);
         mMonthBGOtherColor = res.getColor(R.color.month_other_bgcolor);
         mMonthBGColor = res.getColor(R.color.month_bgcolor);
 
         mTodayDrawable = res.getDrawable(R.drawable.today_blue_week_holo_light);
     }
 
     /**
      * Sets up the text and style properties for painting. Override this if you
      * want to use a different paint.
      */
     @Override
     protected void initView() {
         super.initView();
 
         if (!mScaled) {
             Resources resources = getContext().getResources();
            mShowDetailsInMonth = Utils.getConfigBool(mContext, R.bool.show_details_in_month);
             TEXT_SIZE_MONTH_NUMBER = resources.getInteger(R.integer.text_size_month_number);
             SIDE_PADDING_MONTH_NUMBER = resources.getInteger(R.integer.month_day_number_margin);
             CONFLICT_COLOR = resources.getColor(R.color.month_dna_conflict_time_color);
             if (mScale != 1) {
                 TOP_PADDING_MONTH_NUMBER *= mScale;
                 TOP_PADDING_WEEK_NUMBER *= mScale;
                 SIDE_PADDING_MONTH_NUMBER *= mScale;
                 SIDE_PADDING_WEEK_NUMBER *= mScale;
                 SPACING_WEEK_NUMBER *= mScale;
                 TEXT_SIZE_MONTH_NUMBER *= mScale;
                 TEXT_SIZE_EVENT *= mScale;
                 TEXT_SIZE_MORE_EVENTS *= mScale;
                 TEXT_SIZE_MONTH_NAME *= mScale;
                 TEXT_SIZE_WEEK_NUM *= mScale;
                 DAY_SEPARATOR_OUTER_WIDTH *= mScale;
                 DAY_SEPARATOR_INNER_WIDTH *= mScale;
                 DAY_SEPARATOR_VERTICAL_LENGTH *= mScale;
                 DAY_SEPARATOR_VERTICAL_LENGHT_PORTRAIT *= mScale;
                 EVENT_X_OFFSET_LANDSCAPE *= mScale;
                 EVENT_Y_OFFSET_LANDSCAPE *= mScale;
                 EVENT_Y_OFFSET_PORTRAIT *= mScale;
                 EVENT_SQUARE_WIDTH *= mScale;
                 EVENT_LINE_PADDING *= mScale;
                 EVENT_BOTTOM_PADDING *= mScale;
                 EVENT_RIGHT_PADDING *= mScale;
                 DNA_MARGIN *= mScale;
                 DNA_WIDTH *= mScale;
                 DNA_ALL_DAY_HEIGHT *= mScale;
                 DNA_MIN_SEGMENT_HEIGHT *= mScale;
                 DNA_SIDE_PADDING *= mScale;
                 DEFAULT_EDGE_SPACING *= mScale;
                 DNA_ALL_DAY_WIDTH *= mScale;
             }
             if (!mShowDetailsInMonth) {
                 TOP_PADDING_MONTH_NUMBER += DNA_ALL_DAY_HEIGHT + DNA_MARGIN;
             }
             mScaled = true;
         }
         mPadding = DEFAULT_EDGE_SPACING;
         loadColors(getContext());
         // TODO modify paint properties depending on isMini
 
         mMonthNumPaint = new Paint();
         mMonthNumPaint.setFakeBoldText(false);
         mMonthNumPaint.setAntiAlias(true);
         mMonthNumPaint.setTextSize(TEXT_SIZE_MONTH_NUMBER);
         mMonthNumPaint.setColor(mMonthNumColor);
         mMonthNumPaint.setStyle(Style.FILL);
         mMonthNumPaint.setTextAlign(mShowDetailsInMonth ? Align.LEFT : Align.RIGHT);
         mMonthNumPaint.setTypeface(Typeface.DEFAULT);
 
         mMonthNumHeight = (int) (-mMonthNumPaint.ascent());
 
         mEventPaint = new TextPaint();
         mEventPaint.setFakeBoldText(false);
         mEventPaint.setAntiAlias(true);
         mEventPaint.setTextSize(TEXT_SIZE_EVENT);
         mEventPaint.setColor(mMonthEventColor);
 
         mEventHeight = (int) (-mEventPaint.ascent());
 
         mEventExtrasPaint = new TextPaint();
         mEventExtrasPaint.setFakeBoldText(false);
         mEventExtrasPaint.setAntiAlias(true);
         mEventExtrasPaint.setStrokeWidth(EVENT_SQUARE_BORDER);
         mEventExtrasPaint.setTextSize(TEXT_SIZE_EVENT);
         mEventExtrasPaint.setColor(mMonthEventExtraColor);
         mEventExtrasPaint.setStyle(Style.FILL);
         mEventExtrasPaint.setTextAlign(Align.LEFT);
 
         mWeekNumPaint = new Paint();
         mWeekNumPaint.setFakeBoldText(false);
         mWeekNumPaint.setAntiAlias(true);
         mWeekNumPaint.setTextSize(TEXT_SIZE_WEEK_NUM);
         mWeekNumPaint.setColor(mWeekNumColor);
         mWeekNumPaint.setStyle(Style.FILL);
         mWeekNumPaint.setTextAlign(Align.RIGHT);
 
         mWeekNumHeight = (int) (-mWeekNumPaint.ascent());
 
         mDNAAllDayPaint = new Paint();
         mDNATimePaint = new Paint();
         mDNATimePaint.setColor(mMonthBusyBitsBusyTimeColor);
         mDNATimePaint.setStyle(Style.FILL_AND_STROKE);
         mDNATimePaint.setStrokeWidth(DNA_WIDTH);
         mDNATimePaint.setAntiAlias(false);
         mDNAAllDayPaint.setColor(mMonthBusyBitsConflictTimeColor);
         mDNAAllDayPaint.setStyle(Style.FILL_AND_STROKE);
         mDNAAllDayPaint.setStrokeWidth(DNA_ALL_DAY_WIDTH);
         mDNAAllDayPaint.setAntiAlias(false);
     }
 
     @Override
     public void setWeekParams(HashMap<String, Integer> params, String tz) {
         super.setWeekParams(params, tz);
 
         if (params.containsKey(VIEW_PARAMS_ORIENTATION)) {
             mOrientation = params.get(VIEW_PARAMS_ORIENTATION);
         }
 
         mToday.timezone = tz;
         mToday.setToNow();
         mToday.normalize(true);
         int julianToday = Time.getJulianDay(mToday.toMillis(false), mToday.gmtoff);
         if (julianToday >= mFirstJulianDay && julianToday < mFirstJulianDay + mNumDays) {
             mHasToday = true;
             mTodayIndex = julianToday - mFirstJulianDay;
         } else {
             mHasToday = false;
             mTodayIndex = -1;
         }
         mNumCells = mNumDays + 1;
     }
 
     @Override
     protected void onDraw(Canvas canvas) {
         drawBackground(canvas);
         drawWeekNums(canvas);
         drawDaySeparators(canvas);
         if (mShowDetailsInMonth) {
             drawEvents(canvas);
         } else {
             drawDNA(canvas);
         }
     }
 
     // TODO move into SimpleWeekView
     // Computes the x position for the left side of the given day
     private int computeDayLeftPosition(int day) {
         int effectiveWidth = mWidth;
         int x = 0;
         int xOffset = 0;
         if (mShowWeekNum) {
             xOffset = SPACING_WEEK_NUMBER + mPadding;
             effectiveWidth -= xOffset;
         }
         x = day * effectiveWidth / mNumDays + xOffset;
         return x;
     }
 
     @Override
     protected void drawDaySeparators(Canvas canvas) {
         // mDaySeparatorOuterColor
         float lines[] = new float[8 * 4];
         int count = 6 * 4;
         int wkNumOffset = 0;
         int i = 0;
         if (mShowWeekNum) {
             // This adds the first line separating the week number
             int xOffset = SPACING_WEEK_NUMBER + mPadding;
             count += 4;
             lines[i++] = xOffset;
             lines[i++] = 0;
             lines[i++] = xOffset;
             lines[i++] = mHeight;
             wkNumOffset++;
         }
         count += 4;
         lines[i++] = 0;
         lines[i++] = 0;
         lines[i++] = mWidth;
         lines[i++] = 0;
         int y0 = 0;
         int y1 = mHeight;
 
         while (i < count) {
             int x = computeDayLeftPosition(i / 4 - wkNumOffset);
             lines[i++] = x;
             lines[i++] = y0;
             lines[i++] = x;
             lines[i++] = y1;
         }
         p.setColor(mDaySeparatorInnerColor);
         p.setStrokeWidth(DAY_SEPARATOR_INNER_WIDTH);
         canvas.drawLines(lines, 0, count, p);
     }
 
     @Override
     protected void drawBackground(Canvas canvas) {
         int i = 0;
         int offset = 0;
         r.top = DAY_SEPARATOR_INNER_WIDTH;
         r.bottom = mHeight;
         if (mShowWeekNum) {
             i++;
             offset++;
         }
         if (mFocusDay[i]) {
             while (++i < mFocusDay.length && mFocusDay[i])
                 ;
             r.right = computeDayLeftPosition(i - offset);
             r.left = 0;
             p.setColor(mMonthBGColor);
             canvas.drawRect(r, p);
             // compute left edge for i, set up r, draw
         } else if (mFocusDay[(i = mFocusDay.length - 1)]) {
             while (--i >= offset && mFocusDay[i])
                 ;
             i++;
             // compute left edge for i, set up r, draw
             r.right = mWidth;
             r.left = computeDayLeftPosition(i - offset);
             p.setColor(mMonthBGColor);
             canvas.drawRect(r, p);
         }
         if (mHasToday) {
             p.setColor(mMonthBGTodayColor);
             r.left = computeDayLeftPosition(mTodayIndex);
             r.right = computeDayLeftPosition(mTodayIndex + 1);
             canvas.drawRect(r, p);
         }
     }
 
     @Override
     protected void drawWeekNums(Canvas canvas) {
         int y;
 
         int i = 0;
         int direction = 1;
         int offset = 0;
         int todayIndex = mTodayIndex;
         int x = 0;
         int numCount = mNumDays;
         if (mShowWeekNum) {
             x = SIDE_PADDING_WEEK_NUMBER + mPadding;
             y = mWeekNumHeight + TOP_PADDING_WEEK_NUMBER;
             canvas.drawText(mDayNumbers[0], x, y, mWeekNumPaint);
             numCount++;
             i++;
             todayIndex++;
             offset++;
 
         }
         if (!mShowDetailsInMonth) {
             direction = -1;
             offset--;
         }
 
         y = (mMonthNumHeight + TOP_PADDING_MONTH_NUMBER);
 
         boolean isFocusMonth = mFocusDay[i];
         boolean isBold = false;
         mMonthNumPaint.setColor(isFocusMonth ? mMonthNumColor : mMonthNumOtherColor);
         for (; i < numCount; i++) {
             if (mHasToday && todayIndex == i) {
                 mMonthNumPaint.setColor(mMonthNumTodayColor);
                 mMonthNumPaint.setFakeBoldText(isBold = true);
                 if (i + 1 < numCount) {
                     // Make sure the color will be set back on the next
                     // iteration
                     isFocusMonth = !mFocusDay[i + 1];
                 }
             } else if (mFocusDay[i] != isFocusMonth) {
                 isFocusMonth = mFocusDay[i];
                 mMonthNumPaint.setColor(isFocusMonth ? mMonthNumColor : mMonthNumOtherColor);
             }
             x = computeDayLeftPosition(i - offset) + direction * (SIDE_PADDING_MONTH_NUMBER);
             canvas.drawText(mDayNumbers[i], x, y, mMonthNumPaint);
             if (isBold) {
                 mMonthNumPaint.setFakeBoldText(isBold = false);
             }
         }
     }
 
     protected void drawEvents(Canvas canvas) {
         if (mEvents == null) {
             return;
         }
 
         int day = -1;
         int outlineCount = 0;
         for (ArrayList<Event> eventDay : mEvents) {
             day++;
             if (eventDay == null || eventDay.size() == 0) {
                 continue;
             }
             int ySquare;
             int xSquare = computeDayLeftPosition(day);
             if (mOrientation == Configuration.ORIENTATION_PORTRAIT) {
                 ySquare = EVENT_Y_OFFSET_PORTRAIT + mMonthNumHeight + TOP_PADDING_MONTH_NUMBER;
                 xSquare += SIDE_PADDING_MONTH_NUMBER + 1;
             } else {
                 ySquare = EVENT_Y_OFFSET_LANDSCAPE;
                 xSquare += EVENT_X_OFFSET_LANDSCAPE;
             }
             int rightEdge = computeDayLeftPosition(day + 1) - EVENT_RIGHT_PADDING;
             int eventCount = 0;
             Iterator<Event> iter = eventDay.iterator();
             while (iter.hasNext()) {
                 Event event = iter.next();
                 int newY = drawEvent(canvas, event, xSquare, ySquare, rightEdge, iter.hasNext());
                 if (newY == ySquare) {
                     break;
                 }
                 outlineCount = addChipOutline(mEventOutlines, outlineCount, xSquare, ySquare);
                 eventCount++;
                 ySquare = newY;
             }
 
             int remaining = eventDay.size() - eventCount;
             if (remaining > 0) {
                 drawMoreEvents(canvas, remaining, xSquare);
             }
         }
         if (outlineCount > 0) {
             p.setColor(mEventChipOutlineColor);
             p.setStrokeWidth(EVENT_SQUARE_BORDER);
             canvas.drawLines(mEventOutlines.array, 0, outlineCount, p);
         }
     }
 
     protected int addChipOutline(FloatRef lines, int count, int x, int y) {
         lines.ensureSize(count + 16);
         // top of box
         lines.array[count++] = x;
         lines.array[count++] = y;
         lines.array[count++] = x + EVENT_SQUARE_WIDTH;
         lines.array[count++] = y;
         // right side of box
         lines.array[count++] = x + EVENT_SQUARE_WIDTH;
         lines.array[count++] = y;
         lines.array[count++] = x + EVENT_SQUARE_WIDTH;
         lines.array[count++] = y + EVENT_SQUARE_WIDTH;
         // left side of box
         lines.array[count++] = x;
         lines.array[count++] = y;
         lines.array[count++] = x;
         lines.array[count++] = y + EVENT_SQUARE_WIDTH + 1;
         // bottom of box
         lines.array[count++] = x;
         lines.array[count++] = y + EVENT_SQUARE_WIDTH;
         lines.array[count++] = x + EVENT_SQUARE_WIDTH + 1;
         lines.array[count++] = y + EVENT_SQUARE_WIDTH;
 
         return count;
     }
 
     /**
      * Attempts to draw the given event. Returns the y for the next event or the
      * original y if the event will not fit. An event is considered to not fit
      * if the event and its extras won't fit or if there are more events and the
      * more events line would not fit after drawing this event.
      *
      * @param event the event to draw
      * @param x the top left corner for this event's color chip
      * @param y the top left corner for this event's color chip
      * @return the y for the next event or the original y if it won't fit
      */
     protected int drawEvent(
             Canvas canvas, Event event, int x, int y, int rightEdge, boolean moreEvents) {
         int requiredSpace = EVENT_LINE_PADDING + mEventHeight;
         int multiplier = 1;
         if (moreEvents) {
             multiplier++;
         }
         if (!event.allDay) {
             multiplier++;
         }
         requiredSpace *= multiplier;
         if (requiredSpace + y >= mHeight - EVENT_BOTTOM_PADDING) {
             // Not enough space, return
             return y;
         }
         r.left = x;
         r.right = x + EVENT_SQUARE_WIDTH;
         r.top = y;
         r.bottom = y + EVENT_SQUARE_WIDTH;
         p.setColor(event.color);
         canvas.drawRect(r, p);
 
         int textX = x + EVENT_SQUARE_WIDTH + EVENT_LINE_PADDING;
         int textY = y + mEventHeight - EVENT_LINE_PADDING / 2;
         float avail = rightEdge - textX;
         CharSequence text = TextUtils.ellipsize(
                 event.title, mEventPaint, avail, TextUtils.TruncateAt.END);
         canvas.drawText(text.toString(), textX, textY, mEventPaint);
         if (!event.allDay) {
             textY += mEventHeight + EVENT_LINE_PADDING;
             mStringBuilder.setLength(0);
             text = DateUtils.formatDateRange(getContext(), mFormatter, event.startMillis,
                     event.endMillis, DateUtils.FORMAT_SHOW_TIME | DateUtils.FORMAT_ABBREV_ALL,
                     Utils.getTimeZone(getContext(), null)).toString();
             text = TextUtils.ellipsize(text, mEventExtrasPaint, avail, TextUtils.TruncateAt.END);
             canvas.drawText(text.toString(), textX, textY, mEventExtrasPaint);
         }
 
         return textY + EVENT_LINE_PADDING;
     }
 
     protected void drawMoreEvents(Canvas canvas, int remainingEvents, int x) {
         FloatRef lines = new FloatRef(4 * 4);
         int y = mHeight - EVENT_BOTTOM_PADDING + EVENT_LINE_PADDING / 2 - mEventHeight;
         addChipOutline(lines, 0, x, y);
         canvas.drawLines(lines.array, mEventExtrasPaint);
         String text = getContext().getResources().getQuantityString(
                 R.plurals.month_more_events, remainingEvents);
         y = mHeight - EVENT_BOTTOM_PADDING;
         x += EVENT_SQUARE_WIDTH + EVENT_LINE_PADDING;
         mEventExtrasPaint.setFakeBoldText(true);
         canvas.drawText(String.format(text, remainingEvents), x, y, mEventExtrasPaint);
         mEventExtrasPaint.setFakeBoldText(false);
     }
 
     /**
      * Draws a line showing busy times in each day of week The method draws
      * non-conflicting times in the event color and times with conflicting
      * events in the dna conflict color defined in colors.
      *
      * @param canvas
      */
     protected void drawDNA(Canvas canvas) {
         // Draw event and conflict times
         if (mDna != null) {
             for (Utils.DNAStrand strand : mDna.values()) {
                 if (strand.color == CONFLICT_COLOR || strand.points == null
                         || strand.points.length == 0) {
                     continue;
                 }
                 mDNATimePaint.setColor(strand.color);
                 canvas.drawLines(strand.points, mDNATimePaint);
             }
             // Draw black last to make sure it's on top
             Utils.DNAStrand strand = mDna.get(CONFLICT_COLOR);
             if (strand != null && strand.points != null && strand.points.length != 0) {
                 mDNATimePaint.setColor(strand.color);
                 canvas.drawLines(strand.points, mDNATimePaint);
             }
             if (mDayXs == null) {
                 return;
             }
             int numDays = mDayXs.length;
             int xOffset = (DNA_ALL_DAY_WIDTH - DNA_WIDTH) / 2;
             float[] allDayPoints = new float[numDays * 4];
             if (strand != null && strand.allDays != null && strand.allDays.length == numDays) {
                 int count = 0;
                 for (int i = 0; i < numDays; i++) {
                     // this adds at most 7 draws. We could sort it by color and
                     // build an array instead but this is easier.
                     if (strand.allDays[i] != 0) {
                         mDNAAllDayPaint.setColor(strand.allDays[i]);
                         canvas.drawLine(mDayXs[i] + xOffset, DNA_MARGIN, mDayXs[i] + xOffset,
                                 DNA_MARGIN + DNA_ALL_DAY_HEIGHT, mDNAAllDayPaint);
                     }
                 }
             }
         }
     }
 
     @Override
     protected void updateSelectionPositions() {
         if (mHasSelectedDay) {
             int selectedPosition = mSelectedDay - mWeekStart;
             if (selectedPosition < 0) {
                 selectedPosition += 7;
             }
             int effectiveWidth = mWidth - mPadding * 2;
             effectiveWidth -= SPACING_WEEK_NUMBER;
             mSelectedLeft = selectedPosition * effectiveWidth / mNumDays + mPadding;
             mSelectedRight = (selectedPosition + 1) * effectiveWidth / mNumDays + mPadding;
             mSelectedLeft += SPACING_WEEK_NUMBER;
             mSelectedRight += SPACING_WEEK_NUMBER;
         }
     }
 
     @Override
     public Time getDayFromLocation(float x) {
         int dayStart = SPACING_WEEK_NUMBER + mPadding;
         if (x < dayStart || x > mWidth - mPadding) {
             return null;
         }
         // Selection is (x - start) / (pixels/day) == (x -s) * day / pixels
         int dayPosition = (int) ((x - dayStart) * mNumDays / (mWidth - dayStart - mPadding));
         int day = mFirstJulianDay + dayPosition;
 
         Time time = new Time(mTimeZone);
         if (mWeek == 0) {
             // This week is weird...
             if (day < Time.EPOCH_JULIAN_DAY) {
                 day++;
             } else if (day == Time.EPOCH_JULIAN_DAY) {
                 time.set(1, 0, 1970);
                 time.normalize(true);
                 return time;
             }
         }
 
         time.setJulianDay(day);
         return time;
     }
 
 }
