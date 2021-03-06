 package org.opennms.android.ui;
 
 import android.content.Intent;
 import android.content.SharedPreferences;
 import android.content.res.Configuration;
 import android.os.Bundle;
 import android.preference.PreferenceManager;
 import android.support.v4.app.ActionBarDrawerToggle;
 import android.support.v4.app.Fragment;
 import android.support.v4.app.FragmentTransaction;
 import android.support.v4.view.GravityCompat;
 import android.support.v4.widget.DrawerLayout;
 import android.view.View;
 import android.widget.FrameLayout;
 import com.actionbarsherlock.app.ActionBar;
 import com.actionbarsherlock.app.SherlockFragmentActivity;
 import com.actionbarsherlock.view.Menu;
 import com.actionbarsherlock.view.MenuItem;
 import org.opennms.android.R;
 import org.opennms.android.ui.dialogs.AboutDialog;
 import org.opennms.android.ui.dialogs.WelcomeDialog;
 
 public abstract class BaseActivity extends SherlockFragmentActivity {
     private static final String STATE_TITLE = "title";
     private static final String STATE_IS_NAV_OPEN = "is_nav_open";
     private DrawerLayout navigationLayout;
     private FrameLayout navDrawer;
     private ActionBarDrawerToggle navigationToggle;
     private CharSequence title;
     private ActionBar actionBar;
 
     @Override
     protected void onCreate(Bundle savedInstanceState) {
         super.onCreate(savedInstanceState);
         setContentView(R.layout.main);
 
         final CharSequence drawerTitle = title = getTitle();
         navigationLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
         navDrawer = (FrameLayout) findViewById(R.id.navigation_drawer);
 
         navigationLayout.setDrawerShadow(R.drawable.drawer_shadow, GravityCompat.START);
 
         FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
         Fragment navFragment = new MenuFragment();
         ft.replace(R.id.navigation_drawer, navFragment);
         ft.commit();
 
         actionBar = getSupportActionBar();
 
         // Enable ActionBar home button to behave as action to toggle navigation drawer
         actionBar.setDisplayHomeAsUpEnabled(true);
         actionBar.setHomeButtonEnabled(true);
 
         navigationToggle = new ActionBarDrawerToggle(
                 this,
                 navigationLayout,
                 R.drawable.ic_drawer,
                 R.string.drawer_open,
                 R.string.drawer_close
         ) {
             public void onDrawerClosed(View view) {
                 actionBar.setTitle(title);
             }
 
             public void onDrawerOpened(View drawerView) {
                 actionBar.setTitle(drawerTitle);
             }
         };
         navigationLayout.setDrawerListener(navigationToggle);
 
         SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
         if (sharedPref.getBoolean("is_first_launch", true)) {
             sharedPref.edit().putBoolean("is_first_launch", false).commit();
             showWelcomeDialog();
         }
 
         if (savedInstanceState != null) {
             title = savedInstanceState.getCharSequence(STATE_TITLE);
             if (savedInstanceState.getBoolean(STATE_IS_NAV_OPEN)) actionBar.setTitle(drawerTitle);
             else actionBar.setTitle(title);
         }
     }
 
     @Override
     public void onSaveInstanceState(Bundle savedInstanceState) {
         savedInstanceState.putCharSequence(STATE_TITLE, title);
         savedInstanceState.putBoolean(STATE_IS_NAV_OPEN, navigationLayout.isDrawerOpen(navDrawer));
         super.onSaveInstanceState(savedInstanceState);
     }
 
     @Override
     public boolean onCreateOptionsMenu(Menu menu) {
         getSupportMenuInflater().inflate(R.menu.main, menu);
         return super.onCreateOptionsMenu(menu);
     }
 
     @Override
     public boolean onOptionsItemSelected(MenuItem item) {
         switch (item.getItemId()) {
             case android.R.id.home:
                 if (navigationLayout.isDrawerOpen(navDrawer)) closeDrawer();
                 else openDrawer();
                 return true;
             case R.id.menu_settings:
                 Intent settingsIntent = new Intent(getApplicationContext(), SettingsActivity.class);
                 startActivity(settingsIntent);
                 return true;
             case R.id.menu_about:
                 showAboutDialog();
                 return true;
             default:
                 return super.onOptionsItemSelected(item);
         }
     }
 
     public void closeDrawer() {
         navigationLayout.closeDrawer(navDrawer);
     }
 
     public void openDrawer() {
         navigationLayout.openDrawer(navDrawer);
     }
 
     @Override
     public void setTitle(CharSequence title) {
         this.title = title;
         actionBar.setTitle(this.title);
     }
 
     @Override
     protected void onPostCreate(Bundle savedInstanceState) {
         super.onPostCreate(savedInstanceState);
         navigationToggle.syncState();
     }
 
     @Override
     public void onConfigurationChanged(Configuration newConfig) {
         super.onConfigurationChanged(newConfig);
         navigationToggle.onConfigurationChanged(newConfig);
     }
 
     public void showAboutDialog() {
         AboutDialog dialog = new AboutDialog();
         dialog.show(getSupportFragmentManager(), AboutDialog.TAG);
     }
 
     public void showWelcomeDialog() {
         WelcomeDialog dialog = new WelcomeDialog();
         dialog.show(getSupportFragmentManager(), WelcomeDialog.TAG);
     }
 
 }
