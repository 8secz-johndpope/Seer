 /*
  * Copyright (C) 2012 Brian Muramatsu
  *
  * Licensed under the Apache License, Version 2.0 (the "License");
  * you may not use this file except in compliance with the License.
  * You may obtain a copy of the License at
  *
  *      http://www.apache.org/licenses/LICENSE-2.0
  *
  * Unless required by applicable law or agreed to in writing, software
  * distributed under the License is distributed on an "AS IS" BASIS,
  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  * See the License for the specific language governing permissions and
  * limitations under the License.
  */
 
 package com.btmura.android.reddit.app;
 
 import android.app.ActionBar;
 import android.app.ActionBar.OnNavigationListener;
 import android.app.FragmentTransaction;
 import android.app.LoaderManager.LoaderCallbacks;
 import android.content.Intent;
 import android.content.Loader;
 import android.content.SharedPreferences;
 import android.os.Bundle;
 import android.util.Log;
 import android.view.Menu;
 import android.view.MenuItem;
 
 import com.btmura.android.reddit.BuildConfig;
 import com.btmura.android.reddit.R;
 import com.btmura.android.reddit.accounts.AccountPreferences;
import com.btmura.android.reddit.accounts.AccountUtils;
 import com.btmura.android.reddit.app.ThingListFragment.OnThingSelectedListener;
 import com.btmura.android.reddit.content.AccountLoader;
 import com.btmura.android.reddit.content.AccountLoader.AccountResult;
 import com.btmura.android.reddit.database.Subreddits;
 import com.btmura.android.reddit.util.Flag;
 import com.btmura.android.reddit.util.Objects;
 import com.btmura.android.reddit.widget.FilterAdapter;
 
 public class ThingListActivity extends GlobalMenuActivity implements
         LoaderCallbacks<AccountResult>,
         OnNavigationListener,
         OnSubredditEventListener,
         OnThingSelectedListener,
         AccountNameHolder,
         SubredditNameHolder {
 
     public static final String TAG = "ThingListActivity";
 
     public static final String EXTRA_SUBREDDIT = "subreddit";
     public static final String EXTRA_FLAGS = "flags";
 
     public static final int FLAG_INSERT_HOME = 0x1;
 
     private static final String STATE_NAVIGATION_INDEX = "navigationIndex";
     private static final String STATE_SUBREDDIT = "subreddit";
 
     private ActionBar bar;
     private FilterAdapter adapter;
     private String accountName;
     private String subreddit;
     private SharedPreferences prefs;
    private MenuItem postItem;
     private MenuItem aboutItem;
 
     @Override
     protected void onCreate(Bundle savedInstanceState) {
         super.onCreate(savedInstanceState);
         setContentView(R.layout.thing_list);
         setInitialFragments(savedInstanceState);
         setActionBar(savedInstanceState);
         getLoaderManager().initLoader(0, null, this);
     }
 
     private void setInitialFragments(Bundle savedInstanceState) {
         if (savedInstanceState == null) {
             FragmentTransaction ft = getFragmentManager().beginTransaction();
             ft.add(GlobalMenuFragment.newInstance(), GlobalMenuFragment.TAG);
             ft.commit();
         }
     }
 
     private void setActionBar(Bundle savedInstanceState) {
         bar = getActionBar();
         bar.setDisplayHomeAsUpEnabled(true);
         bar.setDisplayShowTitleEnabled(false);
         bar.setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);
 
         if (savedInstanceState != null) {
             subreddit = savedInstanceState.getString(STATE_SUBREDDIT);
         } else {
             subreddit = getIntent().getStringExtra(EXTRA_SUBREDDIT);
         }
 
         adapter = new FilterAdapter(this);
         adapter.setTitle(Subreddits.getTitle(this, subreddit));
         bar.setListNavigationCallbacks(adapter, this);
         if (savedInstanceState != null) {
             bar.setSelectedNavigationItem(savedInstanceState.getInt(STATE_NAVIGATION_INDEX));
         }
     }
 
     public Loader<AccountResult> onCreateLoader(int id, Bundle args) {
         return new AccountLoader(this, true);
     }
 
     public void onLoadFinished(Loader<AccountResult> loader, AccountResult result) {
         prefs = result.prefs;
         accountName = result.getLastAccount();
         adapter.addSubredditFilters(this);
         bar.setSelectedNavigationItem(result.getLastSubredditFilter());
     }
 
     public void onLoaderReset(Loader<AccountResult> loader) {
         accountName = null;
     }
 
     public boolean onNavigationItemSelected(int itemPosition, long itemId) {
         if (BuildConfig.DEBUG) {
             Log.d(TAG, "onNavigationItemSelected i:" + itemPosition);
         }
 
         int filter = adapter.getFilter(itemPosition);
         AccountPreferences.setLastSubredditFilter(prefs, filter);
 
         ThingListFragment frag = getThingListFragment();
         if (frag == null || !Objects.equals(frag.getAccountName(), accountName)
                 || frag.getFilter() != filter) {
             frag = ThingListFragment.newSubredditInstance(accountName, subreddit, filter, 0);
             FragmentTransaction ft = getFragmentManager().beginTransaction();
             ft.replace(R.id.thing_list_container, frag, ThingListFragment.TAG);
             ft.commit();
         }
         return true;
     }
 
     public void onSubredditDiscovery(String subreddit) {
         this.subreddit = subreddit;
         adapter.setTitle(subreddit);
         refreshMenuItems();
     }
 
     public void onThingSelected(Bundle thingBundle) {
         Intent i = new Intent(this, ThingActivity.class);
         i.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
         i.putExtra(ThingActivity.EXTRA_THING_BUNDLE, thingBundle);
         startActivity(i);
     }
 
     public int onMeasureThingBody() {
         return 0;
     }
 
     public String getAccountName() {
         return accountName;
     }
 
     public String getSubredditName() {
         return subreddit;
     }
 
     @Override
     public boolean onCreateOptionsMenu(Menu menu) {
         super.onCreateOptionsMenu(menu);
         getMenuInflater().inflate(R.menu.thing_list_menu, menu);
        postItem = menu.findItem(R.id.menu_new_post);
         aboutItem = menu.findItem(R.id.menu_about_subreddit);
         return true;
     }
 
     @Override
     public boolean onPrepareOptionsMenu(Menu menu) {
         super.onPrepareOptionsMenu(menu);
         refreshMenuItems();
         return true;
     }
 
     private void refreshMenuItems() {
        if (postItem != null) {
            postItem.setVisible(AccountUtils.isAccount(accountName));
        }

         if (aboutItem != null) {
             aboutItem.setVisible(Subreddits.hasSidebar(subreddit));
         }
     }
 
     @Override
     public boolean onOptionsItemSelected(MenuItem item) {
         switch (item.getItemId()) {
             case android.R.id.home:
                 handleHome();
                 return true;
 
             default:
                 return super.onOptionsItemSelected(item);
         }
     }
 
     private void handleHome() {
         if (insertBackStack()) {
             Intent intent = new Intent(this, BrowserActivity.class);
             startActivity(intent);
         }
         finish();
     }
 
     private boolean insertBackStack() {
         int flags = getIntent().getIntExtra(EXTRA_FLAGS, 0);
         return Flag.isEnabled(flags, FLAG_INSERT_HOME);
     }
 
     @Override
     protected void onSaveInstanceState(Bundle outState) {
         super.onSaveInstanceState(outState);
         outState.putInt(STATE_NAVIGATION_INDEX, bar.getSelectedNavigationIndex());
         outState.putString(STATE_SUBREDDIT, subreddit);
     }
 
     private ThingListFragment getThingListFragment() {
         return (ThingListFragment) getFragmentManager().findFragmentByTag(ThingListFragment.TAG);
     }
 }
