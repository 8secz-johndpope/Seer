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
 
 package com.android.camera.ui;
 
 import com.android.camera.CameraPreference.OnPreferenceChangedListener;
 import com.android.camera.IconListPreference;
 import com.android.camera.ListPreference;
 import com.android.camera.PreferenceGroup;
 import com.android.camera.CameraSettings;
 import com.android.camera.R;
 
 import android.content.Context;
 import android.util.AttributeSet;
 import android.view.View;
 import android.widget.ImageView;
 import android.widget.RelativeLayout;
 
 import java.util.ArrayList;
 
 /**
  * A view that contains camera setting indicators. The indicators are spreaded
  * differently based on the screen resolution.
  */
 public abstract class IndicatorControl extends RelativeLayout implements
         IndicatorButton.Listener, OtherSettingsPopup.Listener {
     private static final String TAG = "IndicatorControl";
 
     private Context mContext;
     private OnPreferenceChangedListener mListener;
     protected OnIndicatorEventListener mOnIndicatorEventListener;
     protected CameraPicker mCameraPicker;
 
     private PreferenceGroup mPreferenceGroup;
     private int mDegree = 0;
 
     ArrayList<AbstractIndicatorButton> mIndicators =
             new ArrayList<AbstractIndicatorButton>();
 
     public void setListener(OnPreferenceChangedListener listener) {
         mListener = listener;
         if (mCameraPicker != null) mCameraPicker.setListener(listener);
     }
 
     public IndicatorControl(Context context, AttributeSet attrs) {
         super(context, attrs);
         mContext = context;
     }
 
     public void setDegree(int degree) {
         mDegree = degree;
         int count = getChildCount();
         for (int i = 0 ; i < count ; ++i) {
             View view = getChildAt(i);
             if (view instanceof RotateImageView) {
                 ((RotateImageView) view).setDegree(degree);
             }
         }
     }
 
     public void setOnIndicatorEventListener(OnIndicatorEventListener listener) {
         mOnIndicatorEventListener = listener;
     }
 
     // For the initialization of first-level indicator control.
     public void initialize(Context context, PreferenceGroup group,
             String flashSetting, String[] keys, String[] otherSettingKeys) {
         initialize(context, group, keys, otherSettingKeys);
     }
 
     public void initialize(Context context, PreferenceGroup group,
             String[] keys, String[] otherSettingKeys) {
         // Reset the variables and states.
         dismissSettingPopup();
         removeIndicators();
 
         // Initialize all variables and icons.
         mPreferenceGroup = group;
         // Add other settings indicator.
         if (otherSettingKeys != null) {
            addOtherSettingIndicator(context, R.drawable.ic_settings_holo_light, otherSettingKeys);
         }
 
         for (int i = 0; i < keys.length; i++) {
             IconListPreference pref = (IconListPreference) group.findPreference(keys[i]);
             if (pref != null) {
                 addIndicator(context, pref);
             }
         }
         requestLayout();
     }
 
     public void initializeCameraPicker(Context context, PreferenceGroup group) {
         ListPreference pref = group.findPreference(
                 CameraSettings.KEY_CAMERA_ID);
         if (pref == null) return;
         mCameraPicker = new CameraPicker(context);
         mCameraPicker.initialize(pref);
         mCameraPicker.setCameraPickerIcon();
         addView(mCameraPicker);
     }
 
     private void removeIndicators() {
         for (View v: mIndicators) {
             removeView(v);
         }
         mIndicators.clear();
     }
 
     @Override
     public boolean shouldDelayChildPressedState() {
         // Return false so the pressed feedback of the back/front camera switch
         // can be showed right away.
         return false;
     }
 
     public void addIndicator(Context context, IconListPreference pref) {
         IndicatorButton b = new IndicatorButton(context, pref);
         b.setSettingChangedListener(this);
         addView(b);
         mIndicators.add(b);
     }
 
     public void addOtherSettingIndicator(Context context, int resId, String[] keys) {
         OtherSettingIndicatorButton b = new OtherSettingIndicatorButton(context, resId,
                 mPreferenceGroup, keys);
         b.setSettingChangedListener(this);
         addView(b);
         mIndicators.add(b);
     }
 
     @Override
     public void onRestorePreferencesClicked() {
         if (mListener != null) {
             mListener.onRestorePreferencesClicked();
         }
     }
 
     @Override
     public void onSettingChanged() {
         if (mListener != null) {
             mListener.onSharedPreferenceChanged();
         }
     }
 
     public boolean dismissSettingPopup() {
         for (AbstractIndicatorButton v: mIndicators) {
             if (v.dismissPopup()) {
                 invalidate();
                 return true;
             }
         }
         return false;
     }
 
     public View getActiveSettingPopup() {
         for (AbstractIndicatorButton v: mIndicators) {
             View result = v.getPopupWindow();
             if (result != null) return result;
         }
         return null;
     }
 
     // Scene mode may override other camera settings (ex: flash mode).
     public void overrideSettings(final String ... keyvalues) {
         if (keyvalues.length % 2 != 0) {
             throw new IllegalArgumentException();
         }
 
         for (AbstractIndicatorButton b: mIndicators) {
             b.overrideSettings(keyvalues);
         }
     }
 
     public void reloadPreferences() {
         mPreferenceGroup.reloadValue();
         for (AbstractIndicatorButton b: mIndicators) {
             b.reloadPreferences();
         }
     }
 
     @Override
     public void setEnabled(boolean enabled) {
         super.setEnabled(enabled);
         final int count = getChildCount();
         for (int i = 0; i < count; i++) {
             View v = getChildAt(i);
             // Zoom buttons and shutter button are controlled by the activity.
             if (v instanceof AbstractIndicatorButton) {
                 v.setEnabled(enabled);
             }
         }
         if (mCameraPicker != null) mCameraPicker.setEnabled(enabled);
     }
 }
