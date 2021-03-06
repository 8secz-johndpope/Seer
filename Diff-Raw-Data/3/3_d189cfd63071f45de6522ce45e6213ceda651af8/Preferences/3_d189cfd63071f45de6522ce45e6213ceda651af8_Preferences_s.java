 /*
  *  This file is part of SWADroid.
  *
  *  Copyright (C) 2010 Juan Miguel Boyero Corral <juanmi1982@gmail.com>
  *
  *  SWADroid is free software: you can redistribute it and/or modify
  *  it under the terms of the GNU General Public License as published by
  *  the Free Software Foundation, either version 3 of the License, or
  *  (at your option) any later version.
  *
  *  SWADroid is distributed in the hope that it will be useful,
  *  but WITHOUT ANY WARRANTY; without even the implied warranty of
  *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  *  GNU General Public License for more details.
  *
  *  You should have received a copy of the GNU General Public License
  *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
  */
 
 package es.ugr.swad.swadroid;
 
 import android.content.Context;
 import android.content.SharedPreferences;
 import android.os.Bundle;
 import android.preference.Preference;
 import android.preference.Preference.OnPreferenceClickListener;
 import android.preference.PreferenceActivity;
 import android.preference.PreferenceManager;
 import android.widget.Toast;
 
 /**
  * Preferences window of application.
  * @author Juan Miguel Boyero Corral <juanmi1982@gmail.com>
  */
 public class Preferences extends PreferenceActivity {
 	/**
 	 * Application preferences
 	 */
 	private SharedPreferences prefs;
     /**
      * User identifier.
      */
     private String userID;
     /**
      * User password.
      */
     private String userPassword;
     /**
      * Old user identifier
      */
     private String oldUserID;
 
 	/**
      * Old user password 
      */
     private String oldUserPassword;
 
     /**
      * Gets user identifier.
      * @return User identifier.
      */
     public String getUserID() {
         return userID;
     }
 
     /**
      * Gets User password.
      * @return User password.
      */
     public String getUserPassword() {
         return userPassword;
     }
 
     /**
      * Gets old user identifier
 	 * @return Old user identifier
 	 */
 	public String getOldUserID() {
 		return oldUserID;
 	}
 	/**
 	 * Gets old user password
 	 * @return Old user password
 	 */
 	public String getOldUserPassword() {
 		return oldUserPassword;
 	}
 
 	/**
 	 * Get if this is the first run
 	 *
 	 * @return returns true, if this is the first run
 	 */
 	 public boolean getFirstRun() {
 	    return prefs.getBoolean("firstRun", true);
 	 }
 	 
 	 /**
 	 * Store the first run
 	 */
 	 public void setRunned() {
 	    SharedPreferences.Editor edit = prefs.edit();
 	    edit.putBoolean("firstRun", false);
 	    edit.commit();
 	 }
 	
     /**
      * Initializes preferences of activity.
      * @param ctx Context of activity.
      */
     public void getPreferences(Context ctx) {
         // Get the xml/preferences.xml preferences
         prefs = PreferenceManager.getDefaultSharedPreferences(ctx);
         userID = prefs.getString("userIDPref", "");
         userPassword = prefs.getString("userPasswordPref", "");
     }
 
 	/* (non-Javadoc)
 	 * @see android.app.Activity#onCreate()
 	 */
     @Override
     protected void onCreate(Bundle savedInstanceState) {
         super.onCreate(savedInstanceState);
         addPreferencesFromResource(R.xml.preferences);
         //Get the custom preference
         Preference savePref = findPreference("savePref");
         Preference userIDPref = findPreference("userIDPref");
         Preference userPasswordPref = findPreference("userPasswordPref");
         userIDPref.setOnPreferenceClickListener(new OnPreferenceClickListener() {
 
             /**
              * Called when a preference is selected.
              * @param preference Preference selected.
              */
             public boolean onPreferenceClick(Preference preference) {
                 prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
                 userID = prefs.getString("userIDPref", "");
                 //Save userID before change it
                 oldUserID = userID;
                 return true;
             }
         });
         userPasswordPref.setOnPreferenceClickListener(new OnPreferenceClickListener() {
 
             /**
              * Called when a preference is selected.
              * @param preference Preference selected.
              */
             public boolean onPreferenceClick(Preference preference) {
                 prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
                 userPassword = prefs.getString("userPasswordPref", "");
                 //Save userPassword before change it
                 oldUserPassword = userPassword;
                 return true;
             }
         });
         savePref.setOnPreferenceClickListener(new OnPreferenceClickListener() {
 
             /**
              * Called when a preference is selected.
              * @param preference Preference selected.
              */
             public boolean onPreferenceClick(Preference preference) {
                 Toast.makeText(getBaseContext(),
                         R.string.saveMsg_preferences,
                         Toast.LENGTH_LONG).show();
                 SharedPreferences saveSharedPreference = getSharedPreferences(
                 		Global.getPrefsName(), Context.MODE_PRIVATE);
                 SharedPreferences.Editor editor = saveSharedPreference.edit();
                 
                 //If user ID or password have changed, logout automatically to force a new login
                if(!userID.equals(oldUserID) || !userPassword.equals(oldUserPassword)) {
                 	Global.setLogged(false);
                 }
 
                 editor.putString("userIDPref", userID);
                 editor.putString("userPasswordPref", userPassword);
                 editor.commit();
                 return true;
             }
         });
     }
 }
