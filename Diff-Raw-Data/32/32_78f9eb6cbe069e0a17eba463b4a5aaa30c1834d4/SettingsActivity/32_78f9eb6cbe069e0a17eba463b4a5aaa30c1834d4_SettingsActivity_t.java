 /*
  * Copyright (C) 2013 Felix Bechstein
  * 
  * This file is part of Portal Timer.
  * 
  * This program is free software; you can redistribute it and/or modify it under
  * the terms of the GNU General Public License as published by the Free Software
  * Foundation; either version 3 of the License, or (at your option) any later
  * version.
  * 
  * This program is distributed in the hope that it will be useful, but WITHOUT
  * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
  * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
  * details.
  * 
  * You should have received a copy of the GNU General Public License along with
  * this program; If not, see <http://www.gnu.org/licenses/>.
  */
 package de.ub0r.android.portaltimer;
 
 import java.text.ParseException;
 
 import android.os.Bundle;
 import android.preference.Preference;
 import android.preference.Preference.OnPreferenceChangeListener;
 import android.preference.PreferenceActivity;
 import android.util.Log;
 import android.widget.Toast;
 
 public class SettingsActivity extends PreferenceActivity implements
 		OnPreferenceChangeListener {
	private static final String TAG = "portal-timer/sa";
 
 	@SuppressWarnings("deprecation")
 	@Override
 	protected void onCreate(final Bundle savedInstanceState) {
 		super.onCreate(savedInstanceState);
 		addPreferencesFromResource(R.xml.settings);
 		addPreferencesFromResource(R.xml.additional);
 
 		Preference p = findPreference("start_ingress");
 		p.setEnabled(getPackageManager().getLaunchIntentForPackage(
 				MainActivity.INGRESS_PACKAGE) != null);
 
 		for (String k : Timer.COOLDOWN_KEYS) {
 			p = findPreference(k);
 			if (p == null) continue;
 			p.setOnPreferenceChangeListener(this);
 		}
 	}
 
 	@Override
 	public boolean onPreferenceChange(Preference preference, Object newValue) {
 		String s = newValue.toString();
 		try {
 			Timer.parseCooldownString(s);
 			return true;
 		} catch (ParseException e) {
 			Toast.makeText(this, getString(R.string.parse_error, s),
 					Toast.LENGTH_LONG).show();
 			Log.e(TAG, "parse error", e);
 			return false;
 		}
 	}
 }
