 /*
  * Copyright (C) 2010-2014 The MPDroid Project
  *
  * Licensed under the Apache License, Version 2.0 (the "License");
  * you may not use this file except in compliance with the License.
  * You may obtain a copy of the License at
  *
  *     http://www.apache.org/licenses/LICENSE-2.0
  *
  * Unless required by applicable law or agreed to in writing, software
  * distributed under the License is distributed on an "AS IS" BASIS,
  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  * See the License for the specific language governing permissions and
  * limitations under the License.
  */
 
 package com.namelessdev.mpdroid;
 
 import com.namelessdev.mpdroid.MPDroidActivities.MPDroidFragmentActivity;
 import com.namelessdev.mpdroid.fragments.BrowseFragment;
 import com.namelessdev.mpdroid.fragments.LibraryFragment;
 import com.namelessdev.mpdroid.fragments.NowPlayingFragment;
 import com.namelessdev.mpdroid.fragments.OutputsFragment;
 import com.namelessdev.mpdroid.fragments.PlaylistFragment;
 import com.namelessdev.mpdroid.library.ILibraryFragmentActivity;
 import com.namelessdev.mpdroid.library.ILibraryTabActivity;
 import com.namelessdev.mpdroid.tools.LibraryTabsUtil;
 import com.namelessdev.mpdroid.tools.Tools;
 import com.sothree.slidinguppanel.SlidingUpPanelLayout;
 
 import org.a0z.mpd.MPD;
 import org.a0z.mpd.MPDStatus;
 import org.a0z.mpd.exception.MPDServerException;
 
 import android.app.ActionBar;
 import android.app.ActionBar.OnNavigationListener;
 import android.content.Context;
 import android.content.Intent;
 import android.content.SharedPreferences;
 import android.content.res.Configuration;
 import android.os.Bundle;
 import android.os.Handler;
 import android.os.StrictMode;
 import android.preference.PreferenceManager;
 import android.support.v4.app.ActionBarDrawerToggle;
 import android.support.v4.app.Fragment;
 import android.support.v4.app.FragmentManager;
 import android.support.v4.app.FragmentManager.OnBackStackChangedListener;
 import android.support.v4.app.FragmentTransaction;
 import android.support.v4.view.PagerAdapter;
 import android.support.v4.view.ViewPager;
 import android.support.v4.widget.DrawerLayout;
 import android.support.v4.widget.PopupMenuCompat;
 import android.view.KeyEvent;
 import android.view.LayoutInflater;
 import android.view.Menu;
 import android.view.MenuItem;
 import android.view.View;
 import android.view.ViewGroup;
 import android.widget.AdapterView;
 import android.widget.ArrayAdapter;
 import android.widget.ImageButton;
 import android.widget.ListView;
 import android.widget.PopupMenu;
 import android.widget.TextView;
 
 import java.util.ArrayList;
 import java.util.List;
 
 public class MainMenuActivity extends MPDroidFragmentActivity implements OnNavigationListener,
         ILibraryFragmentActivity,
         ILibraryTabActivity, OnBackStackChangedListener, PopupMenu.OnMenuItemClickListener {
 
     public static enum DisplayMode {
         MODE_LIBRARY,
         MODE_OUTPUTS
     }
 
     public static class DrawerItem {
 
         public static enum Action {
             ACTION_LIBRARY,
             ACTION_OUTPUTS,
             ACTION_SETTINGS
         }
 
         public Action action;
 
         public String label;
 
         public DrawerItem(String label, Action action) {
             this.label = label;
             this.action = action;
         }
 
         @Override
         public String toString() {
             return label;
         }
     }
 
     private class DrawerItemClickListener implements ListView.OnItemClickListener {
 
         @Override
         public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
             mDrawerLayout.closeDrawer(mDrawerList);
 
             switch (((DrawerItem) parent.getItemAtPosition(position)).action) {
                 default:
                 case ACTION_LIBRARY:
                     // If we are already on the library, pop the whole stack.
                     // Acts like an "up" button
                     if (currentDisplayMode == DisplayMode.MODE_LIBRARY) {
                         final int fmStackCount = fragmentManager.getBackStackEntryCount();
                         if (fmStackCount > 0) {
                             fragmentManager.popBackStack(null,
                                     FragmentManager.POP_BACK_STACK_INCLUSIVE);
                         }
                     }
                     switchMode(DisplayMode.MODE_LIBRARY);
                     break;
                 case ACTION_OUTPUTS:
                     switchMode(DisplayMode.MODE_OUTPUTS);
                     break;
                 case ACTION_SETTINGS:
                     mDrawerList.setItemChecked(oldDrawerPosition, true);
                     final Intent i = new Intent(MainMenuActivity.this, SettingsActivity.class);
                     startActivityForResult(i, SETTINGS);
                     break;
             }
             oldDrawerPosition = position;
         }
     }
 
     class MainMenuPagerAdapter extends PagerAdapter {
 
         @Override
         public void destroyItem(ViewGroup container, int position, Object object) {
             return;
         }
 
         @Override
         public int getCount() {
             return 2;
         }
 
         public Object instantiateItem(View collection, int position) {
 
             int resId = 0;
             switch (position) {
                 case 0:
                     resId = R.id.nowplaying_fragment;
                     break;
                 case 1:
                     resId = R.id.playlist_fragment;
                     break;
             }
             return findViewById(resId);
         }
 
         @Override
         public boolean isViewFromObject(View arg0, Object arg1) {
             return arg0 == ((View) arg1);
         }
     }
 
     public static final int PLAYLIST = 1;
 
     public static final int ARTISTS = 2;
 
     public static final int SETTINGS = 5;
 
     public static final int STREAM = 6;
 
     public static final int LIBRARY = 7;
 
     public static final int CONNECT = 8;
 
     private static final String FRAGMENT_TAG_LIBRARY = "library";
 
     private static final String FRAGMENT_TAG_OUTPUTS = "outputs";
 
     private static final String EXTRA_DISPLAY_MODE = "displaymode";
 
     private static final String EXTRA_SLIDING_PANEL_EXPANDED = "slidingpanelexpanded";
 
     private int backPressExitCount;
 
     private Handler exitCounterReset;
 
     private boolean isDualPaneMode;
 
     private MPDApplication app;
 
     private View nowPlayingDualPane;
 
     private ViewPager nowPlayingPager;
 
     private View libraryRootFrame;
 
     private View outputsRootFrame;
 
     private OutputsFragment outputsFragment;
 
     private TextView titleView;
 
     private List<DrawerItem> mDrawerItems;
 
     private DrawerLayout mDrawerLayout;
 
     private ListView mDrawerList;
 
     private ActionBarDrawerToggle mDrawerToggle;
 
     private SlidingUpPanelLayout mSlidingLayout;
 
     private ImageButton mHeaderPlayQueue;
 
     private ImageButton mHeaderOverflowMenu;
 
     private PopupMenu mHeaderOverflowPopupMenu;
 
     private TextView mHeaderTitle;
 
     private int oldDrawerPosition;
 
     private LibraryFragment libraryFragment;
 
     private PlaylistFragment playlistFragment;
 
     private FragmentManager fragmentManager;
 
     private ArrayList<String> mTabList;
 
     private DisplayMode currentDisplayMode;
 
     @Override
     public ArrayList<String> getTabList() {
         return mTabList;
     }
 
     /**
      * Called when Back button is pressed, displays message to user indicating
      * the if back button is pressed again the application will exit. We keep a
      * count of how many time back button is pressed within 5 seconds. If the
      * count is greater than 1 then call system.exit(0) Starts a post delay
      * handler to reset the back press count to zero after 5 seconds
      *
      * @return None
      */
     @Override
     public void onBackPressed() {
         if (mSlidingLayout.isExpanded()) {
             mSlidingLayout.collapsePane();
             return;
         }
 
         if (currentDisplayMode == DisplayMode.MODE_LIBRARY) {
             final int fmStackCount = fragmentManager.getBackStackEntryCount();
             if (fmStackCount > 0) {
                 super.onBackPressed();
                 return;
             }
         }
 
         final SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
         final boolean exitConfirmationRequired = settings.getBoolean("enableExitConfirmation",
                 false);
         if (exitConfirmationRequired && backPressExitCount < 1) {
             Tools.notifyUser(String.format(getResources().getString(R.string.backpressToQuit)),
                     this);
             backPressExitCount += 1;
             exitCounterReset.postDelayed(new Runnable() {
                 @Override
                 public void run() {
                     backPressExitCount = 0;
                 }
             }, 5000);
         } else {
             /*
              * Nasty force quit, should shutdown everything nicely but there
              * just too many async tasks maybe I'll correctly implement
              * app.terminateApplication();
              */
             System.exit(0);
         }
         return;
     }
 
     /**
      * Library methods
      */
 
     @Override
     public void onBackStackChanged() {
         refreshActionBarTitle();
     }
 
     @Override
     public void onConfigurationChanged(Configuration newConfig) {
         super.onConfigurationChanged(newConfig);
         mDrawerToggle.onConfigurationChanged(newConfig);
     }
 
     @Override
     public void onCreate(Bundle savedInstanceState) {
         super.onCreate(savedInstanceState);
         app = (MPDApplication) getApplication();
 
         setContentView(app.isTabletUiEnabled() ? R.layout.main_activity_nagvigation_tablet
                 : R.layout.main_activity_nagvigation);
 
         LayoutInflater inflater = (LayoutInflater) this
                 .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
         titleView = (TextView) inflater.inflate(R.layout.actionbar_title, null);
         titleView.setFocusable(true);
         titleView.setFocusableInTouchMode(true);
         titleView.setSelected(true);
         titleView.requestFocus();
 
         nowPlayingDualPane = findViewById(R.id.nowplaying_dual_pane);
         nowPlayingPager = (ViewPager) findViewById(R.id.pager);
         libraryRootFrame = findViewById(R.id.library_root_frame);
         outputsRootFrame = findViewById(R.id.outputs_root_frame);
 
         isDualPaneMode = (nowPlayingDualPane != null);
         switchMode(DisplayMode.MODE_LIBRARY);
 
         exitCounterReset = new Handler();
 
         StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
         StrictMode.setThreadPolicy(policy);
 
         // Set up the action bar.
         final ActionBar actionBar = getActionBar();
         actionBar.setDisplayHomeAsUpEnabled(true);
         actionBar.setHomeButtonEnabled(true);
         actionBar.setCustomView(titleView);
         actionBar.setDisplayShowTitleEnabled(false);
         actionBar.setDisplayShowCustomEnabled(true);
 
         mDrawerItems = new ArrayList<DrawerItem>();
         mDrawerItems.add(new DrawerItem(getString(R.string.libraryTabActivity),
                 DrawerItem.Action.ACTION_LIBRARY));
 
         mDrawerItems.add(new DrawerItem(getString(R.string.outputs),
                 DrawerItem.Action.ACTION_OUTPUTS));
 
         mDrawerItems.add(new DrawerItem(getString(R.string.settings),
                 DrawerItem.Action.ACTION_SETTINGS));
 
         mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
         mDrawerList = (ListView) findViewById(R.id.left_drawer);
 
         mDrawerToggle = new ActionBarDrawerToggle(
                 this, /* host Activity */
                 mDrawerLayout, /* DrawerLayout object */
                 app.isLightThemeSelected() ? R.drawable.ic_drawer_light : R.drawable.ic_drawer, /* nav drawer icon to replace 'Up' caret */
                 R.string.drawer_open, /* "open drawer" description */
                 R.string.drawer_close /* "close drawer" description */
         ) {
 
             /**
              * Called when a drawer has settled in a completely closed
              * state.
              */
             public void onDrawerClosed(View view) {
                 refreshActionBarTitle();
             }
 
             /**
              * Called when a drawer has settled in a completely open
              * state.
              */
             public void onDrawerOpened(View drawerView) {
                 actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
                 actionBar.setDisplayShowCustomEnabled(true);
                 titleView.setText(R.string.app_name);
             }
         };
 
         // Set the drawer toggle as the DrawerListener
         mDrawerLayout.setDrawerListener(mDrawerToggle);
 
         // Set the adapter for the list view
         mDrawerList.setAdapter(new ArrayAdapter<DrawerItem>(this,
                 R.layout.drawer_list_item, mDrawerItems));
         oldDrawerPosition = 0;
         mDrawerList.setItemChecked(oldDrawerPosition, true);
         // Set the list's click listener
         mDrawerList.setOnItemClickListener(new DrawerItemClickListener());
 
         /*
          * Setup the library tab
          */
         fragmentManager = getSupportFragmentManager();
         fragmentManager.addOnBackStackChangedListener(this);
 
         // Get the list of the currently visible tabs
         mTabList = LibraryTabsUtil.getCurrentLibraryTabs(this.getApplicationContext());
 
         ArrayAdapter<CharSequence> actionBarAdapter = new ArrayAdapter<CharSequence>(
                 actionBar.getThemedContext(),
                 android.R.layout.simple_spinner_item);
         for (int i = 0; i < mTabList.size(); i++) {
             actionBarAdapter.add(getText(LibraryTabsUtil.getTabTitleResId(mTabList.get(i))));
         }
 
         actionBarAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
         actionBar.setListNavigationCallbacks(actionBarAdapter, this);
 
         libraryFragment = (LibraryFragment) fragmentManager.findFragmentByTag(FRAGMENT_TAG_LIBRARY);
         if (libraryFragment == null) {
             libraryFragment = new LibraryFragment();
             final FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
             ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
             ft.replace(R.id.library_root_frame, libraryFragment, FRAGMENT_TAG_LIBRARY);
             ft.commit();
         }
 
         outputsFragment = (OutputsFragment) fragmentManager.findFragmentByTag(FRAGMENT_TAG_OUTPUTS);
         if (outputsFragment == null) {
             outputsFragment = new OutputsFragment();
             final FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
             ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
             ft.replace(R.id.outputs_root_frame, outputsFragment, FRAGMENT_TAG_OUTPUTS);
             ft.commit();
         }
 
         // Setup the pager
         if (nowPlayingPager != null) {
             nowPlayingPager.setAdapter(new MainMenuPagerAdapter());
             nowPlayingPager.setOnPageChangeListener(
                     new ViewPager.SimpleOnPageChangeListener() {
                         @Override
                         public void onPageSelected(int position) {
                             refreshQueueIndicator(position == 0 ? false : true);
                         }
                     });
         }
 
         if (savedInstanceState != null) {
             switchMode((DisplayMode) savedInstanceState.getSerializable(EXTRA_DISPLAY_MODE));
         }
 
         final View nowPlayingSmallFragment = findViewById(R.id.now_playing_small_fragment);
         playlistFragment = (PlaylistFragment) fragmentManager.findFragmentById(R.id.playlist_fragment);
 
         mHeaderPlayQueue = (ImageButton) findViewById(R.id.header_show_queue);
         mHeaderOverflowMenu = (ImageButton) findViewById(R.id.header_overflow_menu);
         mHeaderTitle = (TextView) findViewById(R.id.header_title);
         if (mHeaderPlayQueue != null) {
             mHeaderPlayQueue.setOnClickListener(new View.OnClickListener() {
                 @Override
                 public void onClick(View v) {
                     if (nowPlayingPager != null) {
                         if (nowPlayingPager.getCurrentItem() == 0) {
                             showQueue();
                         } else {
                             nowPlayingPager.setCurrentItem(0, true);
                             refreshQueueIndicator(false);
                         }
                     }
                 }
             });
         }
         if (mHeaderOverflowMenu != null) {
             mHeaderOverflowPopupMenu = new PopupMenu(this, mHeaderOverflowMenu);
             mHeaderOverflowPopupMenu.getMenuInflater().inflate(R.menu.mpd_mainmenu,
                     mHeaderOverflowPopupMenu.getMenu());
             mHeaderOverflowPopupMenu.getMenuInflater().inflate(R.menu.mpd_playlistmenu,
                     mHeaderOverflowPopupMenu.getMenu());
             mHeaderOverflowPopupMenu.getMenu().removeItem(R.id.PLM_EditPL);
             mHeaderOverflowPopupMenu.setOnMenuItemClickListener(this);
 
             mHeaderOverflowMenu.setOnTouchListener(PopupMenuCompat.getDragToOpenListener(mHeaderOverflowPopupMenu));
 
             mHeaderOverflowMenu.setOnClickListener(new View.OnClickListener() {
                 @Override
                 public void onClick(View v) {
                     prepareNowPlayingMenu(mHeaderOverflowPopupMenu.getMenu());
                     mHeaderOverflowPopupMenu.show();
                 }
             });
         }
         // Sliding panel
         mSlidingLayout = (SlidingUpPanelLayout) findViewById(R.id.sliding_layout);
         mSlidingLayout.setDragView(findViewById(R.id.header_dragview));
         mSlidingLayout.setShadowDrawable(getResources().getDrawable(R.drawable.above_shadow));
         mSlidingLayout.setPanelHeight((int)getResources().getDimension(R.dimen.nowplaying_small_fragment_height));
         final SlidingUpPanelLayout.PanelSlideListener panelSlideListener =
                 new SlidingUpPanelLayout.PanelSlideListener() {
             @Override
             public void onPanelSlide(View panel, float slideOffset) {
                 if (slideOffset < 0.3) {
                     if (getActionBar().isShowing()) {
                         getActionBar().hide();
                     }
                 } else {
                     if (!getActionBar().isShowing()) {
                         getActionBar().show();
                     }
                 }
                 nowPlayingSmallFragment.setVisibility(slideOffset <= 0 ? View.GONE : View.VISIBLE);
                 nowPlayingSmallFragment.setAlpha(slideOffset);
             }
 
             @Override
             public void onPanelExpanded(View panel) {
                 nowPlayingSmallFragment.setVisibility(View.GONE);
                 nowPlayingSmallFragment.setAlpha(1);
                 mDrawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
             }
 
             @Override
             public void onPanelCollapsed(View panel) {
                 nowPlayingSmallFragment.setVisibility(View.VISIBLE);
                 nowPlayingSmallFragment.setAlpha(1);
                 mDrawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED);
             }
 
             @Override
             public void onPanelAnchored(View panel) {
             }
         };
         mSlidingLayout.setPanelSlideListener(panelSlideListener);
         // Ensure that the view state is consistent (otherwise we end up with a view mess)
         // The sliding layout should take care of it itself but does not
         if (savedInstanceState != null) {
             if ((Boolean) savedInstanceState.getSerializable(EXTRA_SLIDING_PANEL_EXPANDED)) {
                 mSlidingLayout.expandPane();
                 panelSlideListener.onPanelSlide(mSlidingLayout, 0);
                 panelSlideListener.onPanelExpanded(mSlidingLayout);
             } else {
                 mSlidingLayout.collapsePane();
                 panelSlideListener.onPanelSlide(mSlidingLayout, 1);
                 panelSlideListener.onPanelCollapsed(mSlidingLayout);
             }
         }
         refreshQueueIndicator(false);
     }
 
     @Override
     public boolean onCreateOptionsMenu(Menu menu) {
         super.onCreateOptionsMenu(menu);
         getMenuInflater().inflate(R.menu.mpd_searchmenu, menu);
         return true;
     }
 
     @Override
     public boolean onKeyDown(int keyCode, KeyEvent event) {
         if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN || keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
             // For onKeyLongPress to work
             event.startTracking();
             return !app.getApplicationState().streamingMode;
         }
         return super.onKeyDown(keyCode, event);
     }
 
     @Override
     public boolean onKeyLongPress(int keyCode, KeyEvent event) {
         final MPDApplication app = (MPDApplication) getApplicationContext();
         switch (event.getKeyCode()) {
             case KeyEvent.KEYCODE_VOLUME_UP:
                 new Thread(new Runnable() {
                     @Override
                     public void run() {
                         try {
                             app.oMPDAsyncHelper.oMPD.next();
                         } catch (MPDServerException e) {
                             e.printStackTrace();
                         }
                     }
                 }).start();
                 return true;
             case KeyEvent.KEYCODE_VOLUME_DOWN:
                 new Thread(new Runnable() {
                     @Override
                     public void run() {
                         try {
                             app.oMPDAsyncHelper.oMPD.previous();
                         } catch (MPDServerException e) {
                             e.printStackTrace();
                         }
                     }
                 }).start();
                 return true;
         }
         return super.onKeyLongPress(keyCode, event);
     }
 
     @Override
     public boolean onKeyUp(int keyCode, final KeyEvent event) {
         switch (event.getKeyCode()) {
             case KeyEvent.KEYCODE_VOLUME_UP:
             case KeyEvent.KEYCODE_VOLUME_DOWN:
                 if (event.isTracking() && !event.isCanceled()
                         && !app.getApplicationState().streamingMode) {
                     new Thread(new Runnable() {
                         @Override
                         public void run() {
                             try {
                                 app.oMPDAsyncHelper.oMPD
                                         .adjustVolume(
                                                 event.getKeyCode() == KeyEvent.KEYCODE_VOLUME_UP
                                                         ? NowPlayingFragment.VOLUME_STEP
                                                         : -NowPlayingFragment.VOLUME_STEP);
                             } catch (MPDServerException e) {
                                 e.printStackTrace();
                             }
                         }
                     }).start();
                 }
                 return true;
         }
         return super.onKeyUp(keyCode, event);
     }
 
     @Override
     public boolean onNavigationItemSelected(int itemPosition, long itemId) {
         libraryFragment.setCurrentItem(itemPosition, true);
         return true;
     }
 
     @Override
     public boolean onMenuItemClick(MenuItem item) {
         return onOptionsItemSelected(item);
     }
 
     @Override
     public boolean onOptionsItemSelected(MenuItem item) {
         if (mDrawerToggle.onOptionsItemSelected(item)) {
             return true;
         }
 
         if (playlistFragment != null && playlistFragment.onOptionsItemSelected(item)) {
             return true;
         }
 
         Intent i = null;
         final MPDApplication app = (MPDApplication) this.getApplication();
         final MPD mpd = app.oMPDAsyncHelper.oMPD;
 
         // Handle item selection
         switch (item.getItemId()) {
             case R.id.menu_search:
                 this.onSearchRequested();
                 return true;
             case CONNECT:
                 ((MPDApplication) this.getApplication()).connect();
                 return true;
             case R.id.GMM_Stream:
                 if (app.getApplicationState().streamingMode) {
                     i = new Intent(this, StreamingService.class);
                     i.setAction("com.namelessdev.mpdroid.DIE");
                     this.startService(i);
                     ((MPDApplication) this.getApplication()).getApplicationState().streamingMode
                             = false;
                     // Toast.makeText(this, "MPD Streaming Stopped",
                     // Toast.LENGTH_SHORT).show();
                 } else {
                     if (app.oMPDAsyncHelper.oMPD.isConnected()) {
                         i = new Intent(this, StreamingService.class);
                         i.setAction("com.namelessdev.mpdroid.START_STREAMING");
                         this.startService(i);
                         ((MPDApplication) this.getApplication()).getApplicationState().streamingMode
                                 = true;
                         // Toast.makeText(this, "MPD Streaming Started",
                         // Toast.LENGTH_SHORT).show();
                     }
                 }
                 return true;
             case R.id.GMM_bonjour:
                 startActivity(new Intent(this, ServerListActivity.class));
                 return true;
             case R.id.GMM_Consume:
                 try {
                     mpd.setConsume(!mpd.getStatus().isConsume());
                 } catch (MPDServerException e) {
                 }
                 return true;
             case R.id.GMM_Single:
                 try {
                     mpd.setSingle(!mpd.getStatus().isSingle());
                 } catch (MPDServerException e) {
                 }
                 return true;
             case R.id.GMM_ShowNotification:
                 i = new Intent(this, NotificationService.class);
                 i.setAction(NotificationService.ACTION_SHOW_NOTIFICATION);
                 this.startService(i);
                 return true;
 
             default:
                 return super.onOptionsItemSelected(item);
         }
 
     }
 
     @Override
     protected void onPostCreate(Bundle savedInstanceState) {
         super.onPostCreate(savedInstanceState);
         // Sync the toggle state after onRestoreInstanceState has occurred.
         mDrawerToggle.syncState();
     }
 
     @Override
     public boolean onPrepareOptionsMenu(Menu menu) {
         // Reminder : never disable buttons that are shown as actionbar actions
         // here
         super.onPrepareOptionsMenu(menu);
         return true;
     }
 
     public boolean prepareNowPlayingMenu(Menu menu) {
         // Reminder : never disable buttons that are shown as actionbar actions
         // here
         MPDApplication app = (MPDApplication) this.getApplication();
         MPD mpd = app.oMPDAsyncHelper.oMPD;
         if (!mpd.isConnected()) {
             if (menu.findItem(CONNECT) == null) {
                 menu.add(0, CONNECT, 0, R.string.connect);
             }
         } else {
             if (menu.findItem(CONNECT) != null) {
                 menu.removeItem(CONNECT);
             }
         }
         setMenuChecked(menu.findItem(R.id.GMM_Stream), app.getApplicationState().streamingMode);
         final MPDStatus mpdStatus = app.getApplicationState().currentMpdStatus;
         if (mpdStatus != null) {
             setMenuChecked(menu.findItem(R.id.GMM_Single), mpdStatus.isSingle());
             setMenuChecked(menu.findItem(R.id.GMM_Consume), mpdStatus.isConsume());
         }
         return true;
     }
 
     @Override
     protected void onResume() {
         super.onResume();
         backPressExitCount = 0;
     }
 
     @Override
     public void onSaveInstanceState(Bundle outState) {
         outState.putSerializable(EXTRA_DISPLAY_MODE, currentDisplayMode);
         outState.putSerializable(EXTRA_SLIDING_PANEL_EXPANDED, mSlidingLayout.isExpanded());
         super.onSaveInstanceState(outState);
     }
 
     @Override
     public void onStart() {
         super.onStart();
         MPDApplication app = (MPDApplication) getApplicationContext();
         app.setActivity(this);
     }
 
     @Override
     public void onStop() {
         super.onStop();
 
         MPDApplication app = (MPDApplication) getApplicationContext();
         app.unsetActivity(this);
     }
 
     @Override
     public void pageChanged(int position) {
         final ActionBar actionBar = getActionBar();
         if (currentDisplayMode == DisplayMode.MODE_LIBRARY
                 && actionBar.getNavigationMode() == ActionBar.NAVIGATION_MODE_LIST) {
             actionBar.setSelectedNavigationItem(position);
         }
     }
 
     @Override
     public void pushLibraryFragment(Fragment fragment, String label) {
         String title = "";
         if (fragment instanceof BrowseFragment) {
             title = ((BrowseFragment) fragment).getTitle();
         } else {
             title = fragment.toString();
         }
         final FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
         ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
         ft.replace(R.id.library_root_frame, fragment);
         ft.addToBackStack(label);
         ft.setBreadCrumbTitle(title);
         ft.commit();
     }
 
     /**
      * Navigation Drawer helpers
      */
 
     private void refreshActionBarTitle() {
         final ActionBar actionBar = getActionBar();
         actionBar.setDisplayShowCustomEnabled(true);
         switch (currentDisplayMode) {
             case MODE_OUTPUTS:
                 actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
                 if (currentDisplayMode == DisplayMode.MODE_OUTPUTS) {
                     titleView.setText(R.string.outputs);
                 }
                 break;
             case MODE_LIBRARY:
                 int fmStackCount = 0;
                 if (fragmentManager != null) {
                     fmStackCount = fragmentManager.getBackStackEntryCount();
                 }
                 if (fmStackCount > 0) {
                     actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
                     titleView.setText(fragmentManager.getBackStackEntryAt(fmStackCount - 1)
                             .getBreadCrumbTitle());
                 } else {
                     actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);
                     actionBar.setDisplayShowCustomEnabled(false);
                 }
                 break;
         }
     }
 
     private void setMenuChecked(MenuItem item, boolean checked) {
         item.setChecked(checked);
     }
 
     /** Swaps fragments in the main content view */
     public void switchMode(DisplayMode newMode) {
         currentDisplayMode = newMode;
         switch (currentDisplayMode) {
             case MODE_LIBRARY:
                 libraryRootFrame.setVisibility(View.VISIBLE);
                 outputsRootFrame.setVisibility(View.GONE);
                 break;
             case MODE_OUTPUTS:
                 libraryRootFrame.setVisibility(View.GONE);
                 outputsRootFrame.setVisibility(View.VISIBLE);
                 outputsFragment.refreshOutputs();
                 break;
         }
         refreshActionBarTitle();
     }
 
     public void refreshQueueIndicator(boolean queueShown) {
         if (mHeaderPlayQueue != null) {
             mHeaderPlayQueue.setAlpha((float)(queueShown ? 1 : 0.5));
         }
        if (mHeaderTitle != null) {
             mHeaderTitle.setText(queueShown ? R.string.playQueue : R.string.nowPlaying);
         }
     }
 
     public void showQueue() {
         if (mSlidingLayout != null) {
             mSlidingLayout.expandPane();
         }
         if (nowPlayingPager != null) {
             nowPlayingPager.setCurrentItem(1, true);
         }
         refreshQueueIndicator(true);
     }
 }
