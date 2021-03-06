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
 
 package com.android.camera.ui;
 
 import com.android.camera.ComboPreferences;
 import com.android.camera.PreferenceGroup;
 import com.android.camera.R;
 
 import android.content.Context;
 import android.graphics.Canvas;
 import android.graphics.Paint;
 import android.graphics.RectF;
 import android.util.AttributeSet;
 import android.util.Log;
 import android.widget.Button;
 import android.view.MotionEvent;
 import android.view.ViewGroup;
 import android.view.View;
 
 import java.lang.Math;
 
 /**
  * A view that contains shutter button and camera setting indicators. The
  * indicators are spreaded around the shutter button. The first child is always
  * the shutter button.
  */
 public class IndicatorWheel extends ViewGroup {
     private static final String TAG = "IndicatorWheel";
     private Listener mListener;
     // The center of the shutter button.
     private int mCenterX, mCenterY;
     // The width of the wheel stroke.
     private int mStrokeWidth = 60;
     private final int STROKE_COLOR = 0x12FFFFFF;
     // The width of the edges on both sides of the wheel, which has less alpha.
     private final int EDGE_STROKE_WIDTH = 6, EDGE_STROKE_COLOR = 0x07FFFFFF;
     // Leave some space between the settings wheel and the decoration edges.
     private final int LINE_SPACE = 2;
     private final int OUTER_EDGE_STROKE_WIDTH = 3, OUTER_EDGE_STROKE_COLOR = 0x07FFFFFF;
     private final int HIGHLIGHT_COLOR = 0xFF6899FF;
     private final int HIGHLIGHT_WIDTH = 4;
     private final int HIGHLIGHT_DEGREE = 30;
     private View mShutterButton;
     private double mShutterButtonRadius;
     private double mWheelRadius;
     private double mSectorInitialRadians[];
     private Paint mBackgroundPaint;
     private RectF mBackgroundRect;
     private int mSelectedIndex = -1;
 
     static public interface Listener {
         public void onIndicatorClicked(int index);
     }
 
     public void setListener(Listener listener) {
         mListener = listener;
     }
 
     public IndicatorWheel(Context context, AttributeSet attrs) {
         super(context, attrs);
         setWillNotDraw(false);
 
         mBackgroundPaint = new Paint();
         mBackgroundPaint.setStyle(Paint.Style.STROKE);
         mBackgroundPaint.setAntiAlias(true);
 
         mBackgroundRect = new RectF();
     }
 
     public void unselectIndicator() {
         mSelectedIndex = -1;
         invalidate();
     }
 
     @Override
     public boolean onTouchEvent(MotionEvent event) {
         int count = getChildCount();
         if (mListener == null || count <= 1) return false;
 
         // Check if any setting is pressed.
         int action = event.getAction();
         if (action == MotionEvent.ACTION_DOWN
                 || action == MotionEvent.ACTION_MOVE) {
             double dx = event.getX() - mCenterX;
             double dy = mCenterY - event.getY();
             double radius = Math.sqrt(dx * dx + dy * dy);
             // Ignore the event if it's too near to the shutter button.
             if (radius < mShutterButtonRadius) return false;
             // Ignore the event if it's too far from the shutter button.
             if ((radius - mWheelRadius) > mStrokeWidth) return false;
 
             double delta = Math.atan2(dy, dx);
             if (delta < 0) delta += Math.PI * 2;
             // Check which sector is pressed.
             if (delta > mSectorInitialRadians[0]) {
                 for (int i = 1; i < count; i++) {
                     if (delta < mSectorInitialRadians[i]) {
                         // Do nothing if scene mode overrides the setting.
                         View child = getChildAt(i);
                         if (child instanceof IndicatorButton) {
                             if (((IndicatorButton) child).isOverridden()) {
                                 return true;
                             }
                         }
                         mSelectedIndex = i - 1;
                         invalidate();
                         mListener.onIndicatorClicked(i - 1);
                         return true;
                     }
                 }
             }
         }
         return false;
     }
 
     @Override
     protected void onFinishInflate() {
         super.onFinishInflate();
         // The first view is shutter button.
         mShutterButton = getChildAt(0);
     }
 
     public void removeIndicators() {
         // Remove everything but the shutter button.
         int count = getChildCount();
         if (count > 1) {
             removeViews(1, count - 1);
         }
     }
 
     @Override
     protected void onMeasure(int widthSpec, int heightSpec) {
         // Measure all children.
         int childCount = getChildCount();
         int freeSpec = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED);
         for (int i = 0; i < childCount; i++) {
             getChildAt(i).measure(freeSpec, freeSpec);
         }
 
         if (childCount > 1) {
             mStrokeWidth = (int) (getChildAt(1).getMeasuredWidth() * 2.0);
         }
 
         // Measure myself.
         int desiredWidth = (int)(mShutterButton.getMeasuredWidth() * 3);
         int desiredHeight = (int)(mShutterButton.getMeasuredHeight() * 4.5) + 2;
         int widthMode = MeasureSpec.getMode(widthSpec);
         int heightMode = MeasureSpec.getMode(heightSpec);
         int measuredWidth, measuredHeight;
         if (widthMode == MeasureSpec.UNSPECIFIED) {
             measuredWidth = desiredWidth;
         } else if (widthMode == MeasureSpec.AT_MOST) {
             measuredWidth = Math.min(desiredWidth, MeasureSpec.getSize(widthSpec));
         } else {  // MeasureSpec.EXACTLY
             measuredWidth = MeasureSpec.getSize(widthSpec);
         }
         if (heightMode == MeasureSpec.UNSPECIFIED) {
             measuredHeight = desiredHeight;
         } else if (heightMode == MeasureSpec.AT_MOST) {
             measuredHeight = Math.min(desiredHeight, MeasureSpec.getSize(heightSpec));
         } else {  // MeasureSpec.EXACTLY
             measuredHeight = MeasureSpec.getSize(heightSpec);
         }
         setMeasuredDimension(measuredWidth, measuredHeight);
     }
 
     @Override
     protected void onLayout(
             boolean changed, int left, int top, int right, int bottom) {
         int count = getChildCount();
         if (count == 0) return;
 
         // Layout the shutter button.
         int shutterButtonWidth = mShutterButton.getMeasuredWidth();
         mShutterButtonRadius = shutterButtonWidth / 2.0;
         int shutterButtonHeight = mShutterButton.getMeasuredHeight();
         mCenterX = (int) (right - left - mShutterButtonRadius * 1.2);
         mCenterY = (bottom - top) / 2;
         mShutterButton.layout(mCenterX - shutterButtonWidth / 2,
                 mCenterY - shutterButtonHeight / 2,
                 mCenterX + shutterButtonWidth / 2,
                 mCenterY + shutterButtonHeight / 2);
 
         // Layout the settings. The icons are spreaded on the left side of the
         // shutter button. So the angle starts from 90 to 270 degrees.
         if (count == 1) return;
         mWheelRadius = mShutterButtonRadius + mStrokeWidth * 1.5;
         double intervalDegrees = (count == 2) ? 90.0 : 180.0 / (count - 2);
         double initialDegrees = 90.0;
         int index = 0;
         for (int i = 0; i < count; i++) {
             View view = getChildAt(i);
             if (view == mShutterButton) continue;
             double degree = initialDegrees + intervalDegrees * index;
             double radian = Math.toRadians(degree);
             int x = mCenterX + (int)(mWheelRadius * Math.cos(radian));
             int y = mCenterY - (int)(mWheelRadius * Math.sin(radian));
             int width = view.getMeasuredWidth();
             int height = view.getMeasuredHeight();
             view.layout(x - width / 2, y - height / 2, x + width / 2,
                     y + height / 2);
             index++;
         }
 
         // Store the radian intervals for each icon.
         mSectorInitialRadians = new double[count];
         mSectorInitialRadians[0] = Math.toRadians(
                 initialDegrees - intervalDegrees / 2.0);
         for (int i = 1; i < count; i++) {
             mSectorInitialRadians[i] = mSectorInitialRadians[i - 1]
                     + Math.toRadians(intervalDegrees);
         }
     }
 
     @Override
     protected void onDraw(Canvas canvas) {
         // Draw the dark background.
         mBackgroundPaint.setStrokeWidth(mStrokeWidth);
         mBackgroundPaint.setColor(STROKE_COLOR);
         mBackgroundRect.set((float)(mCenterX - mWheelRadius),
                 (float)(mCenterY - mWheelRadius),
                 (float)(mCenterX + mWheelRadius),
                 (float)(mCenterY + mWheelRadius));
         canvas.drawArc(mBackgroundRect, 0, 360, false, mBackgroundPaint);
 
         // Draw a lighter background on the both sides of the arc.
         mBackgroundPaint.setStrokeWidth(EDGE_STROKE_WIDTH);
         mBackgroundPaint.setColor(EDGE_STROKE_COLOR);
         float delta = (mStrokeWidth + EDGE_STROKE_WIDTH) / 2.0f;
         mBackgroundRect.inset(-delta - LINE_SPACE, -delta - LINE_SPACE);
         canvas.drawArc(mBackgroundRect, 0, 360, false, mBackgroundPaint);
         mBackgroundRect.inset((delta + LINE_SPACE) * 2, (delta + LINE_SPACE) * 2);
         canvas.drawArc(mBackgroundRect, 0, 360, false, mBackgroundPaint);
 
         // Draw highlight.
         mBackgroundRect.inset(-delta * 2, -delta * 2);
         if (mSelectedIndex >= 0) {
             int count = getChildCount();
             float initialDegrees = 90.0f;
             float intervalDegrees = (count <= 2) ? 0.0f : 180.0f / (count - 2);
             float degree = initialDegrees + intervalDegrees * mSelectedIndex;
             mBackgroundPaint.setStrokeWidth(HIGHLIGHT_WIDTH);
             mBackgroundPaint.setStrokeCap(Paint.Cap.ROUND);
             mBackgroundPaint.setColor(HIGHLIGHT_COLOR);
             canvas.drawArc(mBackgroundRect, -degree - HIGHLIGHT_DEGREE / 2,
                     HIGHLIGHT_DEGREE, false, mBackgroundPaint);
         }
 
         // Draw another thinner, lighter background on the outer circle.
         mBackgroundPaint.setStrokeWidth(OUTER_EDGE_STROKE_WIDTH);
         mBackgroundPaint.setColor(OUTER_EDGE_STROKE_COLOR);
         mBackgroundRect.inset(-delta * 2 - EDGE_STROKE_WIDTH, -delta * 2 - EDGE_STROKE_WIDTH);
         canvas.drawArc(mBackgroundRect, 0, 360, false, mBackgroundPaint);
 
         super.onDraw(canvas);
     }
 
     public void updateIndicator(int index) {
         IndicatorButton indicator = (IndicatorButton) getChildAt(index + 1);
         indicator.reloadPreference();
     }
 
     // Scene mode may override other camera settings (ex: flash mode).
     public void overrideSettings(String key, String value) {
         int count = getChildCount();
         for (int j = 1; j < count; j++) {
             View v = getChildAt(j);
             if (v instanceof IndicatorButton) {  // skip the button of "other settings"
                 IndicatorButton indicator = (IndicatorButton) v;
                 if (key.equals(indicator.getKey())) {
                     indicator.overrideSettings(value);
                 }
             }
         }
     }
 }
