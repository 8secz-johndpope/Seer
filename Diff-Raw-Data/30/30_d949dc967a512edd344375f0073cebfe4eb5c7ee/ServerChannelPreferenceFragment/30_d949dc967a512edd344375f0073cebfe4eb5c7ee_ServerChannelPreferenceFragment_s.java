 /*
     HoloIRC - an IRC client for Android
 
     Copyright 2013 Lalit Maganti
 
     This file is part of HoloIRC.
 
     HoloIRC is free software: you can redistribute it and/or modify
     it under the terms of the GNU General Public License as published by
     the Free Software Foundation, either version 3 of the License, or
     (at your option) any later version.
 
     HoloIRC is distributed in the hope that it will be useful,
     but WITHOUT ANY WARRANTY; without even the implied warranty of
     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
     GNU General Public License for more details.
 
     You should have received a copy of the GNU General Public License
     along with HoloIRC. If not, see <http://www.gnu.org/licenses/>.
  */
 
 package com.fusionx.lightirc.ui;
 
 import android.annotation.TargetApi;
 import android.os.Build;
 import android.os.Bundle;
 import android.preference.Preference;
 import android.preference.PreferenceFragment;
 import android.preference.PreferenceScreen;
 
 import com.fusionx.lightirc.R;
 import com.fusionx.lightirc.constants.PreferenceConstants;
 import com.fusionx.lightirc.ui.preferences.NumberPickerPreference;
 
 @TargetApi(Build.VERSION_CODES.HONEYCOMB)
 public class ServerChannelPreferenceFragment extends PreferenceFragment {
     @Override
     public void onCreate(final Bundle savedInstanceState) {
         super.onCreate(savedInstanceState);
         addPreferencesFromResource(R.xml.server_channel_settings_fragment);
 
         setupNumberPicker(getPreferenceScreen());
     }
 
     public static void setupNumberPicker(final PreferenceScreen screen) {
         final NumberPickerPreference numberPickerDialogPreference = (NumberPickerPreference)
                 screen.findPreference(PreferenceConstants.ReconnectTries);
         numberPickerDialogPreference.setSummary(String.valueOf(numberPickerDialogPreference
                 .getValue()));
         numberPickerDialogPreference.setOnPreferenceChangeListener(new Preference
                 .OnPreferenceChangeListener() {
             @Override
             public boolean onPreferenceChange(Preference preference, Object newValue) {
                numberPickerDialogPreference.setSummary(String.valueOf(numberPickerDialogPreference
                        .getValue()));
                return false;
             }
         });
     }
 }
