 package com.ghelius.narodmon;
 
 import android.content.SharedPreferences;
 import android.os.Bundle;
 import android.preference.*;
 import android.util.Log;
 import com.actionbarsherlock.app.SherlockPreferenceActivity;
 
 public class PreferActivity extends SherlockPreferenceActivity implements SharedPreferences.OnSharedPreferenceChangeListener{
 
 //    final MyPreferenceFragment settingsFragment = new MyPreferenceFragment();
     private static final String TAG = "narodmon-pref";
 
     @Override
     protected void onCreate(final Bundle savedInstanceState)
     {
         setTheme(R.style.Theme_Sherlock);
         super.onCreate(savedInstanceState);
 //        getFragmentManager().beginTransaction().replace(android.R.id.content, settingsFragment).commit();
         addPreferencesFromResource(R.xml.preference_screen);
     }
 
 //    public static class MyPreferenceFragment extends PreferenceFragment
 //    {
 //        @Override
 //        public void onCreate(final Bundle savedInstanceState)
 //        {
 //            super.onCreate(savedInstanceState);
 //            addPreferencesFromResource(R.xml.preference_screen);
 //        }
 //
 //        public void updateSummary () {
 //            Preference loginPref =  findPreference(this.getText(R.string.pref_key_login));
 //            EditTextPreference el = (EditTextPreference) loginPref;
 //            el.setSummary(el.getText());
 //            Preference useGeoCode = findPreference(this.getText(R.string.pref_key_use_geocode));
 //            CheckBoxPreference cb = (CheckBoxPreference) useGeoCode;
 //            if (cb.isChecked())
 //                findPreference(this.getText(R.string.pref_key_geoloc)).setEnabled(true);
 //            else
 //                findPreference(this.getText(R.string.pref_key_geoloc)).setEnabled(false);
 //        }
 //    }
 
     public void updateSummary () {
         Preference loginPref =  findPreference(this.getText(R.string.pref_key_login));
         EditTextPreference el = (EditTextPreference) loginPref;
         el.setSummary(el.getText());
         Preference useGeoCode = findPreference(this.getText(R.string.pref_key_use_geocode));
         CheckBoxPreference cb = (CheckBoxPreference) useGeoCode;
         if (cb.isChecked())
             findPreference(this.getText(R.string.pref_key_geoloc)).setEnabled(true);
         else
             findPreference(this.getText(R.string.pref_key_geoloc)).setEnabled(false);
        Preference geoCode = findPreference(this.getText(R.string.pref_key_geoloc));
        EditTextPreference gc = (EditTextPreference) geoCode;
        gc.setSummary(((EditTextPreference) geoCode).getText());
     }
 
     @Override
     protected void onResume() {
         super.onResume();
         // Set up a listener whenever a key changes
         PreferenceManager.getDefaultSharedPreferences(this).registerOnSharedPreferenceChangeListener(this);
         // Setup the initial values
         updateSummary();
     }
 
     @Override
     protected void onPause() {
         super.onPause();
         // Unregister the listener whenever a key changes
         PreferenceManager.getDefaultSharedPreferences(this).unregisterOnSharedPreferenceChangeListener(this);
     }
 
     public void onSharedPreferenceChanged (SharedPreferences sharedPreferences, String key) {
         // Let's do something a preference value changes
         Log.d(TAG,"onSharedPreferenceChanged: " + key);
         updateSummary();
     }
 
 
 }
