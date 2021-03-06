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
 
 package com.android.camera;
 
 import android.content.Intent;
 import android.content.res.Configuration;
 import android.graphics.drawable.Drawable;
 import android.os.Bundle;
import android.provider.MediaStore;
 import android.util.Log;
 import android.view.KeyEvent;
 import android.view.MotionEvent;
 import android.view.View;
 import android.view.View.OnClickListener;
 import android.view.ViewGroup.LayoutParams;
 import android.widget.FrameLayout;
 import android.widget.ImageView;
 
 import com.android.camera.ui.CameraSwitcher;
 import com.android.gallery3d.util.LightCycleHelper;
 
 import java.util.HashSet;
 
 public class CameraActivity extends ActivityBase
         implements CameraSwitcher.CameraSwitchListener {
    private static final int VIDEO_MODULE_INDEX      = 0;
    private static final int PHOTO_MODULE_INDEX      = 1;
    private static final int PANORAMA_MODULE_INDEX   = 2;
    private static final int LIGHTCYCLE_MODULE_INDEX = 3;

     CameraModule mCurrentModule;
     private FrameLayout mFrame;
     private ShutterButton mShutter;
     private CameraSwitcher mSwitcher;
     private Drawable[] mDrawables;
     private int mSelectedModule;
     private View mEventGroup;
     private HashSet<View> mDispatched;
 
     private static final String TAG = "CAM_activity";
 
     private static final int[] DRAW_IDS = {
             R.drawable.ic_switch_video_holo_light,
             R.drawable.ic_switch_camera_holo_light,
             R.drawable.ic_switch_pan_holo_light,
             R.drawable.ic_switch_photo_pan_holo_light
     };
 
     @Override
     public void onCreate(Bundle state) {
         super.onCreate(state);
         mDispatched = new HashSet<View>();
         setContentView(R.layout.camera_main);
         mFrame =(FrameLayout) findViewById(R.id.main_content);
         mShutter = (ShutterButton) findViewById(R.id.shutter_button);
         mSwitcher = (CameraSwitcher) findViewById(R.id.camera_switcher);
         mEventGroup = findViewById(R.id.event_group);
         mDrawables = new Drawable[DRAW_IDS.length];
         for (int i = 0; i < DRAW_IDS.length; i++) {
             Drawable d = getResources().getDrawable(DRAW_IDS[i]);
             mDrawables[i] = d;
         }
         for (int i = 0; i < mDrawables.length; i++) {
            if (i == LIGHTCYCLE_MODULE_INDEX && !LightCycleHelper.hasLightCycleCapture(this)) {
                 continue; // not enabled, so don't add to UI
             }
             ImageView iv = new ImageView(this);
             iv.setImageDrawable(mDrawables[i]);
             mSwitcher.add(iv, new LayoutParams(LayoutParams.WRAP_CONTENT,
                     LayoutParams.WRAP_CONTENT));
             final int index = i;
             iv.setOnClickListener(new OnClickListener() {
                 @Override
                 public void onClick(View arg0) {
                     mSwitcher.setCurrentModule(index);
                     onCameraSelected(index);
                 }
             });
         }
 
         mSwitcher.setSwitchListener(this);
        if (MediaStore.INTENT_ACTION_VIDEO_CAMERA.equals(getIntent().getAction())) {
            mCurrentModule = new VideoModule();
            mSelectedModule = VIDEO_MODULE_INDEX;
        } else {
            mCurrentModule = new PhotoModule();
            mSelectedModule = PHOTO_MODULE_INDEX;
        }
         mCurrentModule.init(this, mFrame, true);
         mSwitcher.setCurrentModule(mSelectedModule);
     }
 
     @Override
     public void onScroll() {
     }
 
     @Override
     public void onCameraSelected(int i) {
         if (i != mSelectedModule) {
             mPaused = true;
             boolean wasPanorama = isPanoramaActivity();
             closeModule(mCurrentModule);
             mSelectedModule = i;
             switch (i) {
             case 0:
                 mCurrentModule = new VideoModule();
                 break;
             case 1:
                 mCurrentModule = new PhotoModule();
                 break;
             case 2:
                 mCurrentModule = new PanoramaModule();
                 break;
             case 3:
                 mCurrentModule = LightCycleHelper.createPanoramaModule();
                 break;
             }
             openModule(mCurrentModule, wasPanorama);
         }
     }
 
     private void openModule(CameraModule module, boolean wasPanorama) {
         module.init(this, mFrame, !(wasPanorama || isPanoramaActivity()));
         mPaused = false;
         module.onResumeBeforeSuper();
         module.onResumeAfterSuper();
     }
 
     private void closeModule(CameraModule module) {
         module.onPauseBeforeSuper();
         module.onPauseAfterSuper();
         mFrame.removeAllViews();
     }
 
     public ShutterButton getShutterButton() {
         return mShutter;
     }
 
     public void hideUI() {
         mSwitcher.setVisibility(View.GONE);
         mShutter.setVisibility(View.GONE);
     }
 
     public void showUI() {
         mSwitcher.setVisibility(View.VISIBLE);
         mShutter.setVisibility(View.VISIBLE);
     }
 
     public void hideSwitcher() {
         mSwitcher.setVisibility(View.GONE);
     }
 
     public void showSwitcher() {
         mSwitcher.setVisibility(View.VISIBLE);
     }
 
     @Override
     public void onConfigurationChanged(Configuration config) {
         super.onConfigurationChanged(config);
         mCurrentModule.onConfigurationChanged(config);
     }
 
     @Override
     public void onPause() {
         mPaused = true;
         mCurrentModule.onPauseBeforeSuper();
         super.onPause();
         mCurrentModule.onPauseAfterSuper();
     }
 
     @Override
     public void onResume() {
         mPaused = false;
         mCurrentModule.onResumeBeforeSuper();
         super.onResume();
         mCurrentModule.onResumeAfterSuper();
     }
 
     @Override
     protected void onFullScreenChanged(boolean full) {
         if (full) {
             showUI();
         } else {
             hideUI();
         }
         super.onFullScreenChanged(full);
         mCurrentModule.onFullScreenChanged(full);
     }
 
     @Override
     protected void onStop() {
         super.onStop();
         mCurrentModule.onStop();
     }
 
     @Override
     protected void onNewIntent(Intent intent) {
         super.onNewIntent(intent);
         getStateManager().clearActivityResult();
     }
 
     @Override
     protected void installIntentFilter() {
         super.installIntentFilter();
         mCurrentModule.installIntentFilter();
     }
 
     @Override
     protected void onActivityResult(
             int requestCode, int resultCode, Intent data) {
         super.onActivityResult(requestCode, resultCode, data);
         mCurrentModule.onActivityResult(requestCode, resultCode, data);
     }
 
     // Preview area is touched. Handle touch focus.
     @Override
     protected void onSingleTapUp(View view, int x, int y) {
         mCurrentModule.onSingleTapUp(view, x, y);
     }
 
     @Override
     public void onBackPressed() {
         if (!mCurrentModule.onBackPressed()) {
             super.onBackPressed();
         }
     }
 
     @Override
     public boolean onKeyDown(int keyCode, KeyEvent event) {
         return mCurrentModule.onKeyDown(keyCode,  event)
                 || super.onKeyDown(keyCode, event);
     }
 
     @Override
     public boolean onKeyUp(int keyCode, KeyEvent event) {
         return mCurrentModule.onKeyUp(keyCode,  event)
                 || super.onKeyUp(keyCode, event);
     }
 
     @Override
     public boolean dispatchTouchEvent(MotionEvent m) {
         // some custom logic to feed both switcher and shutter
         boolean res = mCurrentModule.dispatchTouchEvent(m);
         if (!res) {
             // try switcher and shutter first
             boolean front = tryDispatch(m, mShutter);
             front |= tryDispatch(m, mSwitcher);
             return front || mEventGroup.dispatchTouchEvent(m);
         }
         return res;
     }
 
     private boolean tryDispatch(MotionEvent m, View v) {
         if ((m.getActionMasked() == MotionEvent.ACTION_DOWN)
                 && isInside(m, v)) {
             mDispatched.add(v);
             return v.dispatchTouchEvent(transformEvent(m, v));
         } else {
             if (mDispatched.contains(v)) {
                 boolean res = v.dispatchTouchEvent(transformEvent(m, v));
                 if (MotionEvent.ACTION_UP == m.getActionMasked()
                         || MotionEvent.ACTION_CANCEL == m.getActionMasked()) {
                     mDispatched.remove(v);
                 }
                 return res;
             }
             return false;
         }
     }
 
     private boolean isInside(MotionEvent evt, View v) {
         return (evt.getX() >= v.getLeft() && evt.getX() < v.getRight()
                 && evt.getY() >= v.getTop() && evt.getY() < v.getBottom());
     }
 
     private MotionEvent transformEvent(MotionEvent m, View v) {
         MotionEvent r = MotionEvent.obtain(m);
         r.offsetLocation(- v.getLeft(), - v.getTop());
         return r;
     }
 
 
     // Preview texture has been copied. Now camera can be released and the
     // animation can be started.
     @Override
     public void onPreviewTextureCopied() {
         mCurrentModule.onPreviewTextureCopied();
     }
 
     @Override
     public void onUserInteraction() {
         super.onUserInteraction();
         mCurrentModule.onUserInteraction();
     }
 
     @Override
     protected boolean updateStorageHintOnResume() {
         return mCurrentModule.updateStorageHintOnResume();
     }
 
     @Override
     public void updateCameraAppView() {
         super.updateCameraAppView();
         mCurrentModule.updateCameraAppView();
     }
 
     @Override
     public boolean isPanoramaActivity() {
         return (mCurrentModule instanceof PanoramaModule);
     }
 
 }
