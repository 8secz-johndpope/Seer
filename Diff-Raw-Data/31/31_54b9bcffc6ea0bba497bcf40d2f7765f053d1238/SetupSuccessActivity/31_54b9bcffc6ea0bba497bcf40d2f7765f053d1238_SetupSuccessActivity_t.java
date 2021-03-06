 /* ***** BEGIN LICENSE BLOCK *****
  * Version: MPL 1.1/GPL 2.0/LGPL 2.1
  *
  * The contents of this file are subject to the Mozilla Public License Version
  * 1.1 (the "License"); you may not use this file except in compliance with
  * the License. You may obtain a copy of the License at
  * http://www.mozilla.org/MPL/
  *
  * Software distributed under the License is distributed on an "AS IS" basis,
  * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
  * for the specific language governing rights and limitations under the
  * License.
  *
  * The Original Code is Android Sync Client.
  *
  * The Initial Developer of the Original Code is
  * the Mozilla Foundation.
  * Portions created by the Initial Developer are Copyright (C) 2011
  * the Initial Developer. All Rights Reserved.
  *
  * Contributor(s):
  *  Chenxia Liu <liuche@mozilla.com>
  *
  * Alternatively, the contents of this file may be used under the terms of
  * either the GNU General Public License Version 2 or later (the "GPL"), or
  * the GNU Lesser General Public License Version 2.1 or later (the "LGPL"),
  * in which case the provisions of the GPL or the LGPL are applicable instead
  * of those above. If you wish to allow use of your version of this file only
  * under the terms of either the GPL or the LGPL, and not to allow others to
  * use your version of this file under the terms of the MPL, indicate your
  * decision by deleting the provisions above and replace them with the notice
  * and other provisions required by the GPL or the LGPL. If you do not delete
  * the provisions above, a recipient may use your version of this file under
  * the terms of any one of the MPL, the GPL or the LGPL.
  *
  * ***** END LICENSE BLOCK ***** */
 
 package org.mozilla.gecko.sync.setup.activities;
 
 import org.mozilla.gecko.R;
import org.mozilla.gecko.sync.Logger;
 import org.mozilla.gecko.sync.setup.Constants;
 
 import android.app.Activity;
 import android.content.Context;
 import android.content.Intent;
 import android.os.Bundle;
 import android.provider.Settings;
 import android.view.View;
 import android.widget.TextView;
 
 public class SetupSuccessActivity extends Activity {
   private final static String LOG_TAG = "SetupSuccessActivity";
   private TextView setupSubtitle;
   private Context mContext;
 
   @Override
   public void onCreate(Bundle savedInstanceState) {
     setTheme(R.style.SyncTheme);
     super.onCreate(savedInstanceState);
     mContext = getApplicationContext();
     Bundle extras = this.getIntent().getExtras();
     setContentView(R.layout.sync_setup_success);
     setupSubtitle = ((TextView) findViewById(R.id.setup_success_subtitle));
     if (extras != null) {
       boolean isSetup = extras.getBoolean(Constants.INTENT_EXTRA_IS_SETUP);
       if (!isSetup) {
         setupSubtitle.setText(getString(R.string.sync_subtitle_manage));
       }
     }
   }
 
  @Override
  public void onDestroy() {
    Logger.debug(LOG_TAG, "onDestroy() called.");
    super.onDestroy();
  }

   /* Click Handlers */
   public void settingsClickHandler(View target) {
     Intent intent = new Intent(Settings.ACTION_SYNC_SETTINGS);
    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
     startActivity(intent);
   }
 
   public void pairClickHandler(View target) {
     Intent intent = new Intent(mContext, SetupSyncActivity.class);
     intent.setFlags(Constants.FLAG_ACTIVITY_REORDER_TO_FRONT_NO_ANIMATION);
     intent.putExtra(Constants.INTENT_EXTRA_IS_SETUP, false);
     startActivity(intent);
   }
 }
