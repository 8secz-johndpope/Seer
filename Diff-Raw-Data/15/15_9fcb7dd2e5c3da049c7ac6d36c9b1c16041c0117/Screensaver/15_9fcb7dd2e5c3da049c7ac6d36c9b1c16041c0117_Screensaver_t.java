 /*
  * Copyright (C) 2011 The Android Open Source Project
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
 
 import android.animation.Animator;
 import android.animation.AnimatorSet;
 import android.animation.ObjectAnimator;
 import android.animation.TimeInterpolator;
 import android.content.SharedPreferences;
 import android.content.res.Configuration;
 import android.graphics.Color;
 import android.graphics.Paint;
 import android.graphics.PorterDuff;
 import android.graphics.PorterDuffColorFilter;
 import android.os.Handler;
 import android.preference.PreferenceManager;
 import android.service.dreams.DreamService;
 import android.util.Log;
 import android.view.View;
 import android.view.animation.AccelerateInterpolator;
 import android.view.animation.DecelerateInterpolator;
 
 public class Screensaver extends DreamService {
     static final boolean DEBUG = false;
     static final String TAG = "DeskClock/Screensaver";
 
     static final long MOVE_DELAY = 60000; // DeskClock.SCREEN_SAVER_MOVE_DELAY;
     static final long SLIDE_TIME = 10000;
     static final long FADE_TIME = 3000;
 
     static final boolean SLIDE = false;
 
     private View mContentView, mSaverView;
     private View mAnalogClock, mDigitalClock;
 
     private static TimeInterpolator mSlowStartWithBrakes =
         new TimeInterpolator() {
             @Override
             public float getInterpolation(float x) {
                 return (float)(Math.cos((Math.pow(x,3) + 1) * Math.PI) / 2.0f) + 0.5f;
             }
         };
 
     private final Handler mHandler = new Handler();
 
 
     private final Runnable mMoveSaverRunnable = new Runnable() {
         @Override
         public void run() {
             long delay = MOVE_DELAY;
 
             if (DEBUG) Log.d(TAG,
                     String.format("mContentView=(%d x %d) container=(%d x %d)",
                         mContentView.getWidth(), mContentView.getHeight(),
                         mSaverView.getWidth(), mSaverView.getHeight()
                         ));
             final float xrange = mContentView.getWidth() - mSaverView.getWidth();
             final float yrange = mContentView.getHeight() - mSaverView.getHeight();
 
             if (xrange == 0 && yrange == 0) {
                 delay = 500; // back in a split second
             } else {
                 final int nextx = (int) (Math.random() * xrange);
                 final int nexty = (int) (Math.random() * yrange);
 
                 if (mSaverView.getAlpha() == 0f) {
                     // jump right there
                     mSaverView.setX(nextx);
                     mSaverView.setY(nexty);
                     ObjectAnimator.ofFloat(mSaverView, "alpha", 0f, 1f)
                         .setDuration(FADE_TIME)
                         .start();
                 } else {
                     AnimatorSet s = new AnimatorSet();
                     Animator xMove   = ObjectAnimator.ofFloat(mSaverView,
                                          "x", mSaverView.getX(), nextx);
                     Animator yMove   = ObjectAnimator.ofFloat(mSaverView,
                                          "y", mSaverView.getY(), nexty);
 
                     Animator xShrink = ObjectAnimator.ofFloat(mSaverView, "scaleX", 1f, 0.85f);
                     Animator xGrow   = ObjectAnimator.ofFloat(mSaverView, "scaleX", 0.85f, 1f);
 
                     Animator yShrink = ObjectAnimator.ofFloat(mSaverView, "scaleY", 1f, 0.85f);
                     Animator yGrow   = ObjectAnimator.ofFloat(mSaverView, "scaleY", 0.85f, 1f);
                     AnimatorSet shrink = new AnimatorSet(); shrink.play(xShrink).with(yShrink);
                     AnimatorSet grow = new AnimatorSet(); grow.play(xGrow).with(yGrow);
 
                     Animator fadeout = ObjectAnimator.ofFloat(mSaverView, "alpha", 1f, 0f);
                     Animator fadein = ObjectAnimator.ofFloat(mSaverView, "alpha", 0f, 1f);
 
 
                     if (SLIDE) {
                         s.play(xMove).with(yMove);
                         s.setDuration(SLIDE_TIME);
 
                         s.play(shrink.setDuration(SLIDE_TIME/2));
                         s.play(grow.setDuration(SLIDE_TIME/2)).after(shrink);
                         s.setInterpolator(mSlowStartWithBrakes);
                     } else {
                         AccelerateInterpolator accel = new AccelerateInterpolator();
                         DecelerateInterpolator decel = new DecelerateInterpolator();
 
                         shrink.setDuration(FADE_TIME).setInterpolator(accel);
                         fadeout.setDuration(FADE_TIME).setInterpolator(accel);
                         grow.setDuration(FADE_TIME).setInterpolator(decel);
                         fadein.setDuration(FADE_TIME).setInterpolator(decel);
                         s.play(shrink);
                         s.play(fadeout);
                         s.play(xMove.setDuration(0)).after(FADE_TIME);
                         s.play(yMove.setDuration(0)).after(FADE_TIME);
                         s.play(fadein).after(FADE_TIME);
                         s.play(grow).after(FADE_TIME);
                     }
                     s.start();
                 }
 
                 long now = System.currentTimeMillis();
                 long adjust = (now % 60000);
                 delay = delay
                         + (MOVE_DELAY - adjust) // minute aligned
                         - (SLIDE ? 0 : FADE_TIME) // start moving before the fade
                         ;
                 if (DEBUG) Log.d(TAG,
                         "will move again in " + delay + " now=" + now + " adjusted by " + adjust);
             }
 
             mHandler.removeCallbacks(this);
             mHandler.postDelayed(this, delay);
         }
     };
 
     public Screensaver() {
         if (DEBUG) Log.d(TAG, "Screensaver allocated");
     }
 
     @Override
     public void onCreate() {
         if (DEBUG) Log.d(TAG, "Screensaver created");
         super.onCreate();
     }
 
     @Override
     public void onConfigurationChanged(Configuration newConfig) {
         if (DEBUG) Log.d(TAG, "Screensaver configuration changed");
         super.onConfigurationChanged(newConfig);
         mHandler.removeCallbacks(mMoveSaverRunnable);
         layoutClockSaver();
         mHandler.post(mMoveSaverRunnable);
     }
 
     @Override
     public void onAttachedToWindow() {
         if (DEBUG) Log.d(TAG, "Screensaver attached to window");
         super.onAttachedToWindow();
 
        // We want the screen saver to exit upon user interaction.
        setInteractive(false);

         setFullscreen(true);
 
         layoutClockSaver();
 
         mHandler.post(mMoveSaverRunnable);
     }
 
     @Override
     public void onDetachedFromWindow() {
         if (DEBUG) Log.d(TAG, "Screensaver detached from window");
         super.onDetachedFromWindow();
 
         mHandler.removeCallbacks(mMoveSaverRunnable);
     }
 
     private void setClockStyle() {
         SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
         String style = sharedPref.getString(ScreensaverSettingsActivity.KEY_CLOCK_STYLE, "analog");
         if (style.equals("analog")) {
             mDigitalClock.setVisibility(View.GONE);
             mAnalogClock.setVisibility(View.VISIBLE);
             mSaverView = mAnalogClock;
         } else {
             mDigitalClock.setVisibility(View.VISIBLE);
             mAnalogClock.setVisibility(View.GONE);
             mSaverView = mDigitalClock;
         }
         boolean night = sharedPref.getBoolean(ScreensaverSettingsActivity.KEY_NIGHT_MODE, false);
 
         if (night) {
             Paint paint = new Paint();
             paint.setColor(Color.WHITE);
             paint.setColorFilter(new PorterDuffColorFilter(0x60FFFFFF, PorterDuff.Mode.MULTIPLY));
             mSaverView.setLayerType(View.LAYER_TYPE_HARDWARE, paint);
         }
     }
 
     private void layoutClockSaver() {
         setContentView(R.layout.desk_clock_saver);
         mDigitalClock = findViewById(R.id.digital_clock);
         mAnalogClock =findViewById(R.id.analog_clock);
         setClockStyle();
         mContentView = (View) mSaverView.getParent();
         mSaverView.setAlpha(0);
     }
 }
