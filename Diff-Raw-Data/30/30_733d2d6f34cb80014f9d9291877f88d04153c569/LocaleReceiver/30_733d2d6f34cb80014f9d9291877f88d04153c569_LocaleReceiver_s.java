 /*
  * Copyright (C) 2009 Google Inc.
  *
  * Licensed under the Apache License, Version 2.0 (the "License"); you may not
  * use this file except in compliance with the License. You may obtain a copy of
  * the License at
  *
  * http://www.apache.org/licenses/LICENSE-2.0
  *
  * Unless required by applicable law or agreed to in writing, software
  * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
  * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
  * License for the specific language governing permissions and limitations under
  * the License.
  */
 
 package com.google.ase.locale;
 
 import android.content.BroadcastReceiver;
 import android.content.Context;
 import android.content.Intent;
 import android.util.Log;
 
 import com.google.ase.Constants;
import com.google.ase.activity.AseService;
 
 public class LocaleReceiver extends BroadcastReceiver {
 
   @Override
   public void onReceive(Context context, Intent intent) {
     String scriptName = intent.getStringExtra(Constants.EXTRA_SCRIPT_NAME);
     Log.v("LocaleReceiver", "Locale initiated launch of " + scriptName);
<<<<<<< local
    Intent launch = IntentBuilders.buildLaunchIntent(scriptName);
    launch.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    context.startService(launch);
=======
    Intent i = new Intent(context, AseService.class);
    intent.setAction(Constants.ACTION_LAUNCH_TERMINAL);
    intent.putExtra(Constants.EXTRA_SCRIPT_NAME, scriptName);
     context.startService(i);
>>>>>>> other
   }
 }
