 /*
  * AndFHEM - Open Source Android application to control a FHEM home automation
  * server.
  *
  * Copyright (c) 2011, Matthias Klass or third-party contributors as
  * indicated by the @author tags or express copyright attribution
  * statements applied by the authors.  All third-party contributions are
  * distributed under license by Red Hat Inc.
  *
  * This copyrighted material is made available to anyone wishing to use, modify,
  * copy, or redistribute it subject to the terms and conditions of the GNU GENERAL PUBLICLICENSE, as published by the Free Software Foundation.
  *
  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
  * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU GENERAL PUBLIC LICENSE
  * for more details.
  *
  * You should have received a copy of the GNU GENERAL PUBLIC LICENSE
  * along with this distribution; if not, write to:
  *   Free Software Foundation, Inc.
  *   51 Franklin Street, Fifth Floor
  *   Boston, MA  02110-1301  USA
  */
 
 package li.klass.fhem.activities.core;
 
 import android.content.BroadcastReceiver;
 import android.content.Context;
 import android.content.Intent;
 import android.content.IntentFilter;
 import android.net.Uri;
 import android.os.Bundle;
 import android.os.Handler;
 import android.preference.PreferenceManager;
 import android.support.v4.app.*;
 import android.support.v4.view.Menu;
 import android.support.v4.view.MenuItem;
 import android.util.Log;
 import android.widget.Toast;
 import li.klass.fhem.ApplicationUrls;
 import li.klass.fhem.R;
 import li.klass.fhem.activities.PreferencesActivity;
 import li.klass.fhem.activities.base.Updateable;
 import li.klass.fhem.constants.Actions;
 import li.klass.fhem.constants.BundleExtraKeys;
 import li.klass.fhem.fragments.AllDevicesFragment;
 import li.klass.fhem.fragments.FavoritesFragment;
 import li.klass.fhem.fragments.RoomListFragment;
 import li.klass.fhem.fragments.core.ActionBarShowTabs;
 import li.klass.fhem.fragments.core.TopLevelFragment;
 import li.klass.fhem.service.room.RoomListService;
 
 import java.lang.reflect.Constructor;
 import java.util.ArrayList;
 
 import static li.klass.fhem.constants.Actions.*;
 
 public abstract class FragmentBaseActivity extends FragmentActivity implements ActionBar.TabListener, Updateable {
     private static final int FAVORITES_TAB = 1;
     private static final int ROOM_LIST_TAB = 2;
     private static final int ALL_DEVICES_TAB = 3;
 
     private Intent currentFragmentIntent = null;
     private ArrayList<Intent> intentHistoryStack = new ArrayList<Intent>();
 
     private Receiver broadcastReceiver;
     private Menu optionsMenu;
 
     private Handler autoUpdateHandler;
     private final Runnable autoUpdateCallback = new Runnable() {
         boolean firstRun = true;
 
         @Override
         public void run() {
             String updateTime = PreferenceManager.getDefaultSharedPreferences(FragmentBaseActivity.this).getString("AUTO_UPDATE_TIME", "-1");
             Long millis = Long.valueOf(updateTime);
 
             if (!firstRun && millis != -1) {
                 Intent updateIntent = new Intent(Actions.DO_UPDATE);
                 updateIntent.putExtra(BundleExtraKeys.DO_REFRESH, true);
                 sendBroadcast(updateIntent);
 
                 Log.d(FragmentBaseActivity.class.getName(), "update");
             }
 
             if (millis == -1) {
                 millis = 30 * 1000L;
             }
             autoUpdateHandler.postDelayed(this, millis);
 
             firstRun = false;
         }
     };
 
     private class Receiver extends BroadcastReceiver {
 
         private final IntentFilter intentFilter;
 
         private Receiver() {
             intentFilter = new IntentFilter();
             intentFilter.addAction(Actions.SHOW_FRAGMENT);
             intentFilter.addAction(Actions.DISMISS_UPDATING_DIALOG);
             intentFilter.addAction(Actions.DO_UPDATE);
             intentFilter.addAction(SHOW_EXECUTING_DIALOG);
             intentFilter.addAction(DISMISS_EXECUTING_DIALOG);
             intentFilter.addAction(SHOW_TOAST);
             intentFilter.addAction(DO_UPDATE);
         }
 
         @Override
         public void onReceive(Context context, final Intent intent) {
             runOnUiThread(new Runnable() {
                 @Override
                 public void run() {
                     try {
                         String action = intent.getAction();
                         if (action.equals(Actions.SHOW_FRAGMENT)) {
                             String fragmentName = intent.getStringExtra(BundleExtraKeys.FRAGMENT_NAME);
                             Constructor<?> constructor = Class.forName(fragmentName).getConstructor(Bundle.class);
 
                             Fragment fragment = (Fragment) constructor.newInstance(intent.getExtras());
 
                             switchToFragment(intent, fragment, intent.getBooleanExtra(BundleExtraKeys.FRAGMENT_ADD_TO_STACK, true));
                         } else if (action.equals(Actions.DISMISS_UPDATING_DIALOG)) {
                             setShowRefreshProgressIcon(false);
                         } else if (action.equals(Actions.DO_UPDATE) && intent.getBooleanExtra(BundleExtraKeys.DO_REFRESH, false)) {
                             setShowRefreshProgressIcon(true);
                         } else if (action.equals(SHOW_EXECUTING_DIALOG)) {
                             Bundle bundle = new Bundle();
                             bundle.putInt(BundleExtraKeys.CONTENT, R.string.executing);
                             showDialog(bundle);
                         } else if (action.equals(DISMISS_EXECUTING_DIALOG)) {
                             removeDialog();
                         } else if (action.equals(SHOW_TOAST)) {
                             Toast.makeText(FragmentBaseActivity.this, intent.getIntExtra(BundleExtraKeys.TOAST_STRING_ID, 0), Toast.LENGTH_SHORT).show();
                         }
                     } catch (Exception e) {
                         Log.e(FragmentBaseActivity.class.getName(), "exception occurred while receiving broadcast", e);
                     }
                 }
             });
         }
 
         public IntentFilter getIntentFilter() {
             return intentFilter;
         }
     }
 
     @Override
     protected void onCreate(Bundle savedInstanceState) {
         super.onCreate(savedInstanceState);
 
         autoUpdateHandler = new Handler();
         autoUpdateHandler.postDelayed(autoUpdateCallback, 0);
 
         ActionBar actionBar = getSupportActionBar();
         actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
         actionBar.setDisplayShowTitleEnabled(false);
 
         ActionBar.Tab favoritesTab = actionBar.newTab()
                 .setText(R.string.tab_favorites)
                 .setTabListener(this)
                 .setTag(FAVORITES_TAB);
         actionBar.addTab(favoritesTab);
 
         ActionBar.Tab roomListTab = actionBar.newTab()
                 .setText(R.string.tab_roomList)
                 .setTabListener(this)
                 .setTag(ROOM_LIST_TAB);
         actionBar.addTab(roomListTab);
 
         ActionBar.Tab allDevicesTab = actionBar.newTab()
                 .setText(R.string.tab_alldevices)
                 .setTabListener(this)
                 .setTag(ALL_DEVICES_TAB);
         actionBar.addTab(allDevicesTab);
 
         actionBar.setDisplayHomeAsUpEnabled(true);
 
         broadcastReceiver = new Receiver();
         registerReceiver(broadcastReceiver, broadcastReceiver.getIntentFilter());
 
         boolean restoreResult = false;
         if (savedInstanceState != null) {
             restoreResult = restoreSavedInstance(savedInstanceState, actionBar);
         }
 
         if (savedInstanceState == null || ! restoreResult)  {
             Intent intent = new Intent(Actions.SHOW_FRAGMENT);
             intent.putExtra(BundleExtraKeys.FRAGMENT_NAME, FavoritesFragment.class.getName());
             sendBroadcast(intent);
         }
     }
 
     @SuppressWarnings("unchecked")
     private boolean restoreSavedInstance(Bundle savedInstanceState, ActionBar actionBar) {
         try {
             ArrayList<Intent> previousIntentStack = (ArrayList<Intent>) savedInstanceState.getSerializable(BundleExtraKeys.FRAGMENT_HISTORY_STACK);
 
             if (previousIntentStack != null) {
                 removeLastHistoryIntent(previousIntentStack);
                 intentHistoryStack = previousIntentStack;
             }
 
             if (savedInstanceState.containsKey(BundleExtraKeys.CURRENT_TAB)) {
                 actionBar.setSelectedNavigationItem(savedInstanceState.getInt(BundleExtraKeys.CURRENT_TAB));
             }
 
             Intent intent = savedInstanceState.getParcelable(BundleExtraKeys.CURRENT_FRAGMENT_INTENT);
             if (intent != null) {
                 sendBroadcast(intent);
                 return true;
             } else {
                 return false;
             }
         } catch(Exception e) {
             return false;
         }
     }
 
     @Override
     protected void onResume() {
         super.onResume();
         registerReceiver(broadcastReceiver, broadcastReceiver.getIntentFilter());
         removeDialog();
     }
 
     @Override
     protected void onStop() {
         super.onStop();
         RoomListService.INSTANCE.storeDeviceListMap();
         unregisterReceiver(broadcastReceiver);
 
         autoUpdateHandler.removeCallbacks(autoUpdateCallback);
         setShowRefreshProgressIcon(false);
     }
 
     @Override
     public void onTabSelected(ActionBar.Tab tab, FragmentTransaction ft) {
         Intent intent = new Intent(Actions.SHOW_FRAGMENT);
         switch(Integer.valueOf(tab.getTag().toString())){
             case FAVORITES_TAB:
                 intent.putExtra(BundleExtraKeys.FRAGMENT_NAME, FavoritesFragment.class.getName());
                 break;
             case ROOM_LIST_TAB:
                 intent.putExtra(BundleExtraKeys.FRAGMENT_NAME, RoomListFragment.class.getName());
                 break;
             case ALL_DEVICES_TAB:
                 intent.putExtra(BundleExtraKeys.FRAGMENT_NAME, AllDevicesFragment.class.getName());
                 break;
             default:
                 return;
         }
 
         sendBroadcast(intent);
     }
 
     @Override
     public void onTabUnselected(ActionBar.Tab tab, FragmentTransaction ft) {
     }
 
     @Override
     public void onTabReselected(ActionBar.Tab tab, FragmentTransaction ft) {
         onTabSelected(tab, ft);
     }
 
     @Override
     public boolean onCreateOptionsMenu(Menu menu) {
         getMenuInflater().inflate(R.menu.main_menu, menu);
         this.optionsMenu = menu;
         return super.onCreateOptionsMenu(menu);
     }
 
     @Override
     public boolean onOptionsItemSelected(MenuItem item) {
         switch (item.getItemId()) {
             case android.R.id.home:
                 onBackPressed();
                 break;
 
             case R.id.menu_refresh:
                 Intent refreshIntent = new Intent(Actions.DO_UPDATE);
                 refreshIntent.putExtra(BundleExtraKeys.DO_REFRESH, true);
                 sendBroadcast(refreshIntent);
 
                 return true;
 
             case R.id.menu_settings:
                 Intent settingsIntent = new Intent(this, PreferencesActivity.class);
                 startActivity(settingsIntent);
 
                 return true;
 
             case R.id.menu_help:
                 Uri helpUri = Uri.parse(ApplicationUrls.HELP_PAGE);
                 Intent helpIntent = new Intent(Intent.ACTION_VIEW, helpUri);
                 startActivity(helpIntent);
 
                 return true;
 
             case R.id.menu_donate:
                 Uri donateUri = Uri.parse(ApplicationUrls.DONATE_PAGE);
                 Intent donateIntent = new Intent(Intent.ACTION_VIEW, donateUri);
                 startActivity(donateIntent);
 
                 return true;
 
         }
 
         return super.onOptionsItemSelected(item);
     }
 
     @Override
     public void onBackPressed() {
         Intent previousFragmentIntent = removeLastHistoryIntent(intentHistoryStack);
 
         if (previousFragmentIntent != null) {
             previousFragmentIntent.putExtra(BundleExtraKeys.FRAGMENT_ADD_TO_STACK, false);
             sendBroadcast(previousFragmentIntent);
         } else {
             finish();
         }
     }
 
     @Override
     protected void onSaveInstanceState(Bundle outState) {
         removeDialog();
 
         outState.putParcelable(BundleExtraKeys.CURRENT_FRAGMENT_INTENT, currentFragmentIntent);
         outState.putSerializable(BundleExtraKeys.FRAGMENT_HISTORY_STACK, intentHistoryStack);
 
         if (getSupportActionBar().getSelectedTab() != null) {
             outState.putInt(BundleExtraKeys.CURRENT_TAB, getSupportActionBar().getSelectedTab().getPosition());
         }
 
         super.onSaveInstanceState(outState);
     }
 
     /**
      * Shows the given fragment in the main content section.
      * @param callingIntent received intent including the SHOW_FRAGMENT action.
      * @param fragment fragment to show
      * @param putToStack put the fragment to history. Usually true, except when back is pressed (history)
      */
     private void switchToFragment(Intent callingIntent, Fragment fragment, boolean putToStack) {
         if (fragment != null && intentHistoryStack.size() < 10) {
             if (currentFragmentIntent != null&& putToStack &&
                     ! currentFragmentIntent.getStringExtra(BundleExtraKeys.FRAGMENT_NAME)
                             .equals(callingIntent.getStringExtra(BundleExtraKeys.FRAGMENT_NAME))) {
                 intentHistoryStack.add(currentFragmentIntent);
             }
 
             currentFragmentIntent = callingIntent;
 
             int navigationMode = ActionBar.NAVIGATION_MODE_STANDARD;
             if (fragment instanceof ActionBarShowTabs) {
                 navigationMode = ActionBar.NAVIGATION_MODE_TABS;
             }
             if (fragment instanceof TopLevelFragment) {
                 intentHistoryStack.clear();
             }
 
             if (getSupportActionBar().getNavigationMode() != navigationMode) {
                 getSupportActionBar().setNavigationMode(navigationMode);
             }
 
             getSupportFragmentManager()
                     .beginTransaction()
                     .replace(android.R.id.content, fragment)
                     .commit();
         }
     }
 
     private void showDialog(Bundle bundle) {
         FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
         Fragment prev = getSupportFragmentManager().findFragmentByTag("dialog");
         if (prev != null) {
             fragmentTransaction.remove(prev);
         }
         fragmentTransaction.addToBackStack(null);
 
         DialogFragment newFragment = new ProgressFragment(bundle);
         newFragment.show(fragmentTransaction, "dialog");
     }
 
     private void removeDialog() {
        FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
        Fragment prev = getSupportFragmentManager().findFragmentByTag("dialog");
        if (prev != null) {
            fragmentTransaction.remove(prev);
         }
        fragmentTransaction.addToBackStack(null);
        fragmentTransaction.commit();
     }
 
     private void setShowRefreshProgressIcon(boolean show) {
         if (optionsMenu == null) return;
         optionsMenu.findItem(R.id.menu_refresh).setVisible(! show);
         optionsMenu.findItem(R.id.menu_refresh_progress).setVisible(show);
     }
 
     private Intent removeLastHistoryIntent(ArrayList<Intent> previousIntentStack) {
         if (! previousIntentStack.isEmpty()) {
             return previousIntentStack.remove(previousIntentStack.size() - 1);
         }
         return null;
     }
 }
