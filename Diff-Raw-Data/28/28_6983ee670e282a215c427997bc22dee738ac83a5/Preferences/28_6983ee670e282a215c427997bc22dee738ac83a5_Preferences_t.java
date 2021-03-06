 /**
  * *******************************************************************************************************************
  * <p/>
  * Copyright (C) 8/26/12 by Manuel Palacio
  * <p/>
  * **********************************************************************************************************************
  * <p/>
  * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
  * the License. You may obtain a copy of the License at
  * <p/>
  * http://www.apache.org/licenses/LICENSE-2.0
  * <p/>
  * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
  * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
  * specific language governing permissions and limitations under the License.
  * <p/>
  * **********************************************************************************************************************
  */
 package net.palacesoft.cngstation.client;
 
 import android.content.Context;
 import android.content.Intent;
 import android.os.Bundle;
 import android.preference.Preference;
 import android.preference.PreferenceActivity;
 import android.preference.PreferenceManager;
 import net.palacesoft.cngstation.R;
 
 
 public class Preferences extends PreferenceActivity {
 
     @Override
     protected void onCreate(Bundle savedInstanceState) {
         super.onCreate(savedInstanceState);
         addPreferencesFromResource(R.xml.settings);
     }
 
     public static int getDistance(Context context) {
 
        return Integer.parseInt(PreferenceManager.getDefaultSharedPreferences(context).getString("distancePref", "20"));
 
     }
 }
