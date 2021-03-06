 
 package com.mattprecious.prioritysms.activity;
 
 import android.annotation.TargetApi;
 import android.content.Context;
 import android.content.pm.PackageInfo;
 import android.content.pm.PackageManager;
 import android.util.Log;
 import android.view.inputmethod.InputMethodManager;
 import com.actionbarsherlock.app.ActionBar;
 import com.actionbarsherlock.app.SherlockFragmentActivity;
 import com.actionbarsherlock.app.SherlockPreferenceActivity;
 import com.actionbarsherlock.view.Menu;
 import com.actionbarsherlock.view.MenuInflater;
 import com.actionbarsherlock.view.MenuItem;
 import com.crashlytics.android.Crashlytics;
 import com.mattprecious.prioritysms.BuildConfig;
 import com.mattprecious.prioritysms.R;
 import com.mattprecious.prioritysms.devtools.TriggerAlarmPhoneDialogFragment;
 import com.mattprecious.prioritysms.devtools.TriggerAlarmSmsDialogFragment;
 import com.mattprecious.prioritysms.fragment.ChangeLogDialogFragment;
 import com.mattprecious.prioritysms.fragment.ProfileDetailFragment;
 import com.mattprecious.prioritysms.fragment.ProfileListFragment;
 import com.mattprecious.prioritysms.model.BaseProfile;
 
 import com.mattprecious.prioritysms.preferences.AboutPreferenceFragment;
 import com.mattprecious.prioritysms.preferences.SettingsActivity;
 import com.mattprecious.prioritysms.util.Helpers;
 import org.jraf.android.backport.switchwidget.Switch;
 
 import android.content.Intent;
 import android.content.SharedPreferences;
 import android.os.Bundle;
 import android.preference.PreferenceManager;
 import android.support.v4.app.Fragment;
 import android.view.Gravity;
 import android.view.View;
 import android.widget.CompoundButton;
 import android.widget.CompoundButton.OnCheckedChangeListener;
 
 import de.keyboardsurfer.android.widget.crouton.Crouton;
 import de.keyboardsurfer.android.widget.crouton.Style;
 
 public class ProfileListActivity extends SherlockFragmentActivity
         implements ProfileListFragment.Callbacks, ProfileDetailFragment.Callbacks {
     private static final String TAG = ProfileListActivity.class.getSimpleName();
 
     private static final String STATE_DETAIL_FRAGMENT = "detail_fragment";
 
     private final String KEY_CHANGE_LOG_VERSION = "change_log_version";
 
     private static final int REQUEST_ID_PROFILE_EDIT = 1;
 
     private boolean mTwoPane;
 
     private SharedPreferences mPreferences;
 
     private Switch mActionBarSwitch;
 
     private ProfileListFragment mListFragment;
 
     private Fragment mDetailFragment;
 
     @Override
     protected void onCreate(Bundle savedInstanceState) {
         super.onCreate(savedInstanceState);
 
         if (!BuildConfig.DEBUG) {
             Crashlytics.start(this);
         }
 
         setContentView(R.layout.activity_profile_list);
         mPreferences = PreferenceManager.getDefaultSharedPreferences(this);
 
         configureActionBar();
 
         mListFragment = (ProfileListFragment) getSupportFragmentManager().findFragmentById(
                 R.id.profile_list);
 
         if (savedInstanceState != null) {
             mDetailFragment = getSupportFragmentManager().getFragment(savedInstanceState,
                     STATE_DETAIL_FRAGMENT);
         }
 
         if (findViewById(R.id.profile_detail_container) != null) {
             mTwoPane = true;
 
             mListFragment.setActivateOnItemClick(true);
 
             if (mDetailFragment != null) {
                 setHasOptionsMenu(false);
             }
         }
 
         changeLog();
     }
 
     @Override
     protected void onDestroy() {
         super.onDestroy();
         Crouton.clearCroutonsForActivity(this);
     }
 
     @Override
     protected void onSaveInstanceState(Bundle outState) {
         super.onSaveInstanceState(outState);
 
         if (mDetailFragment != null) {
             getSupportFragmentManager().putFragment(outState, STATE_DETAIL_FRAGMENT,
                     mDetailFragment);
         }
     }
 
     @Override
     public void onBackPressed() {
         if (mDetailFragment == null) {
             super.onBackPressed();
         } else {
             onDiscard();
         }
     }
 
     @Override
     public void onItemSelected(BaseProfile profile) {
         if (mTwoPane) {
             boolean wasEditing = mDetailFragment != null;
 
             Crouton.cancelAllCroutons();
 
             mDetailFragment = ProfileDetailFragment.create(profile);
             getSupportFragmentManager().beginTransaction()
                     .replace(R.id.profile_detail_container, mDetailFragment)
                     .commit();
 
             if (wasEditing) {
                 showDiscardCrouton();
             } else {
                 setHasOptionsMenu(false);
             }
 
         } else {
             Intent detailIntent = new Intent(this, ProfileDetailActivity.class);
             detailIntent.putExtra(ProfileDetailFragment.EXTRA_PROFILE, profile);
             startActivityForResult(detailIntent, REQUEST_ID_PROFILE_EDIT);
         }
     }
 
     @Override
     public boolean onCreateOptionsMenu(Menu menu) {
         MenuInflater inflater = getSupportMenuInflater();
         inflater.inflate(R.menu.menu_profile_list_activity, menu);
 
         if (BuildConfig.DEBUG) {
             menu.findItem(R.id.menu_dev_tools).setVisible(true);
         }
 
         return true;
     }
 
     @Override
     public boolean onOptionsItemSelected(MenuItem item) {
         switch (item.getItemId()) {
             case android.R.id.home:
                 onDiscard();
                 return true;
             case R.id.menu_dev_sms:
                 new TriggerAlarmSmsDialogFragment().show(getSupportFragmentManager(), null);
                 return true;
             case R.id.menu_dev_phone:
                 new TriggerAlarmPhoneDialogFragment().show(getSupportFragmentManager(), null);
                 return true;
             case R.id.menu_preferences:
                 startActivity(new Intent(this, SettingsActivity.class));
                 return true;
             case R.id.menu_feedback:
                 Helpers.openSupportPage(this);
                 return true;
             case R.id.menu_about:
                 startActivity(buildAboutIntent());
                 return true;
             default:
                 return super.onOptionsItemSelected(item);
         }
     }
 
     @Override
     protected void onActivityResult(int requestCode, int resultCode, Intent data) {
         switch (requestCode) {
             case REQUEST_ID_PROFILE_EDIT:
                 switch (resultCode) {
                     case RESULT_OK:
                         showSaveCrouton();
                         break;
                     case RESULT_CANCELED:
                         showDiscardCrouton();
                         break;
                     case ProfileDetailActivity.RESULT_DELETED:
                         showDeleteCrouton();
                         break;
                     default:
                         break;
                 }
                 break;
             default:
                 super.onActivityResult(requestCode, resultCode, data);
                 break;
         }
     }
 
     private void configureActionBar() {
         mActionBarSwitch = new Switch(this, null, R.attr.switchStyleAb);
         mActionBarSwitch.setChecked(mPreferences.getBoolean(getString(R.string.pref_key_enabled),
                 false));
         mActionBarSwitch.setOnCheckedChangeListener(new OnCheckedChangeListener() {
 
             @Override
             public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                 mPreferences.edit().putBoolean(getString(R.string.pref_key_enabled), isChecked)
                         .commit();
             }
         });
 
         final int padding = getResources().getDimensionPixelSize(R.dimen.action_bar_switch_padding);
         mActionBarSwitch.setPadding(0, 0, padding, 0);
         getSupportActionBar().setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM,
                 ActionBar.DISPLAY_SHOW_CUSTOM);
         getSupportActionBar().setCustomView(mActionBarSwitch, new ActionBar.LayoutParams(
                 ActionBar.LayoutParams.WRAP_CONTENT, ActionBar.LayoutParams.WRAP_CONTENT,
                 Gravity.CENTER_VERTICAL | Gravity.RIGHT));
     }
 
     private void setHasOptionsMenu(boolean hasMenu) {
         mActionBarSwitch.setVisibility(hasMenu ? View.VISIBLE : View.GONE);
         mListFragment.setHasOptionsMenu(hasMenu);
     }
 
     private void refreshList() {
         mListFragment.refreshList();
     }
 
     private void removeDetailFragment() {
         if (mDetailFragment != null) {
             getSupportFragmentManager().beginTransaction().remove(mDetailFragment).commit();
             mDetailFragment = null;
             setHasOptionsMenu(true);
             getSupportActionBar().setDisplayHomeAsUpEnabled(false);
         }
 
         mListFragment.clearActivated();
 
         hideKeyboard();
     }
 
     private void showSaveCrouton() {
         showCrouton(R.string.crouton_profile_saved, Style.CONFIRM);
     }
 
     private void showDiscardCrouton() {
         showCrouton(R.string.crouton_profile_discarded, Style.INFO);
     }
 
     private void showDeleteCrouton() {
         showCrouton(R.string.crouton_profile_deleted, Style.INFO);
     }
 
     private void showCrouton(int messageResId, Style style) {
         Crouton.showText(this, messageResId, style, mTwoPane ? R.id.profile_detail_container
                 : android.R.id.content);
     }
 
     @Override
     public void onNewProfile(BaseProfile profile) {
         onItemSelected(profile);
     }
 
     @Override
     public void onNameUpdated(String name) {
     }
 
     @Override
     public void onDiscard() {
         removeDetailFragment();
         showDiscardCrouton();
     }
 
     @Override
     public void onDelete() {
         removeDetailFragment();
         refreshList();
         showDeleteCrouton();
     }
 
     @Override
     public void onSave() {
         removeDetailFragment();
         refreshList();
         showSaveCrouton();
     }
 
     private void hideKeyboard() {
         InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
         imm.hideSoftInputFromWindow(getWindow().getDecorView().getWindowToken(), 0);
     }
 
     private Intent buildAboutIntent() {
         Intent aboutIntent;
         if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB) {
             aboutIntent = buildAboutIntentLegacy();
         } else {
             aboutIntent = buildAboutIntentFragments();
         }
 
         return aboutIntent;
     }
 
     private Intent buildAboutIntentLegacy() {
         Intent aboutIntent = new Intent(this, SettingsActivity.class);
         aboutIntent.setAction(SettingsActivity.PREFS_ABOUT);
 
         return aboutIntent;
     }
 
     @TargetApi(Build.VERSION_CODES.HONEYCOMB)
     private Intent buildAboutIntentFragments() {
         Intent aboutIntent = new Intent(this, SettingsActivity.class);
         aboutIntent.putExtra(SherlockPreferenceActivity.EXTRA_SHOW_FRAGMENT,
                 AboutPreferenceFragment.class.getName());
 
         if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
             setAboutIntentTitleICS(aboutIntent);
         }
 
         return aboutIntent;
     }
 
     @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
     private void setAboutIntentTitleICS(Intent intent) {
         intent.putExtra(SherlockPreferenceActivity.EXTRA_SHOW_FRAGMENT_TITLE,
                 R.string.preference_header_about);
     }
 
     private void changeLog() {
         PackageManager packageManager = getPackageManager();
 
         try {
             PackageInfo packageInfo = packageManager.getPackageInfo(getPackageName(), 0);
 
             if (getPreferences(MODE_PRIVATE).getInt(KEY_CHANGE_LOG_VERSION, 0) < packageInfo.versionCode) {
                 if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                     showChangeLog();
                 } else {
                     showChangeLogLegacy();
                 }
 
                 getPreferences(MODE_PRIVATE).edit()
                         .putInt(KEY_CHANGE_LOG_VERSION, packageInfo.versionCode)
                         .commit();
             }
         } catch (PackageManager.NameNotFoundException e) {
             Log.e(TAG, "Failed to show change log", e);
         }
     }
 
     private void showChangeLogLegacy() {
         SettingsActivity.buildChangeLogDialog(this).show();
     }
 
     @TargetApi(Build.VERSION_CODES.HONEYCOMB)
     private void showChangeLog() {
         new ChangeLogDialogFragment().show(getFragmentManager(), null);
     }
 }
