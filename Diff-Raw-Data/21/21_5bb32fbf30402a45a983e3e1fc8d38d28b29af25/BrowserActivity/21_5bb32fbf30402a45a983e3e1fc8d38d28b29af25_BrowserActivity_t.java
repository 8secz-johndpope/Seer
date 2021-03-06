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
 
import java.util.List;

 import android.accounts.Account;
 import android.app.ActionBar;
 import android.app.ActionBar.OnNavigationListener;
 import android.content.ContentResolver;
 import android.content.Loader;
 import android.content.SharedPreferences;
 import android.content.UriMatcher;
 import android.net.Uri;
 import android.os.AsyncTask;
 import android.os.Bundle;
 import android.text.TextUtils;
 import android.util.Log;
 
 import com.btmura.android.reddit.BuildConfig;
 import com.btmura.android.reddit.R;
 import com.btmura.android.reddit.accounts.AccountPreferences;
 import com.btmura.android.reddit.accounts.AccountUtils;
 import com.btmura.android.reddit.content.AccountLoader.AccountResult;
 import com.btmura.android.reddit.provider.AccountProvider;
 import com.btmura.android.reddit.widget.AccountSpinnerAdapter;
 
 public class BrowserActivity extends AbstractBrowserActivity implements OnNavigationListener,
         AccountNameHolder {
 
     /** String extra used to request a specific subreddit to view. */
     public static final String EXTRA_SUBREDDIT = "subreddit";
 
     // URI constants and matcher for handling intents with data.
     private static final String AUTHORITY = "www.reddit.com";
     private static final UriMatcher MATCHER = new UriMatcher(0);
     private static final int MATCH_SUBREDDIT = 1;
    private static final int MATCH_COMMENTS = 2;
     static {
         MATCHER.addURI(AUTHORITY, "r/*", MATCH_SUBREDDIT);
        MATCHER.addURI(AUTHORITY, "r/*/comments/*", MATCH_COMMENTS);
        MATCHER.addURI(AUTHORITY, "r/*/comments/*/*", MATCH_COMMENTS);
     }
 
    /** Requested subreddit from intent data or extra. */
     private String requestedSubreddit;

    /** Requested thing from intent data. */
    private String requestedThingId;

     private AccountSpinnerAdapter adapter;
     private SharedPreferences prefs;
 
     @Override
     protected void setContentView() {
         setContentView(R.layout.browser);
     }
 
     @Override
     protected boolean skipSetup() {
         // Process the intent's data if available.
         Uri data = getIntent().getData();
         if (data != null) {
             switch (MATCHER.match(data)) {
                 case MATCH_SUBREDDIT:
                     requestedSubreddit = data.getLastPathSegment();
                     break;

                case MATCH_COMMENTS:
                    List<String> segments = data.getPathSegments();
                    requestedSubreddit = segments.get(1);
                    requestedThingId = segments.get(2);
                    break;
             }
         }
 
         // Process the intent's extras if available. The data takes precedence.
         if (TextUtils.isEmpty(requestedSubreddit)) {
             requestedSubreddit = getIntent().getStringExtra(EXTRA_SUBREDDIT);
         }
 
         // Single pane browser activity only shows subreddits, so start the more
         // specific activity and bail out.
         if (isSinglePane && !TextUtils.isEmpty(requestedSubreddit)) {
             selectSubredditSinglePane(requestedSubreddit, ThingListActivity.FLAG_INSERT_HOME);
             finish();
             return true;
         }
 
         return false;
     }
 
     @Override
     protected void setupViews() {
     }
 
     @Override
     protected void setupActionBar(Bundle savedInstanceState) {
         adapter = new AccountSpinnerAdapter(this, !isSinglePane);
         bar.setDisplayShowTitleEnabled(false);
         bar.setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);
         bar.setListNavigationCallbacks(adapter, this);
     }
 
     @Override
     public void onLoadFinished(Loader<AccountResult> loader, AccountResult result) {
         prefs = result.prefs;
         adapter.setAccountNames(result.accountNames);
 
         String accountName = result.getLastAccount();
         adapter.setAccountName(accountName);
         adapter.setFilter(result.getLastSubredditFilter());
 
         int index = adapter.findAccountName(accountName);
         bar.setSelectedNavigationItem(index);
     }
 
     @Override
     public void onLoaderReset(Loader<AccountResult> loader) {
         adapter.setAccountNames(null);
     }
 
     @Override
     public String getAccountName() {
         return adapter.getAccountName();
     }
 
     @Override
     protected int getFilter() {
         return adapter.getFilter();
     }
 
     @Override
     protected boolean hasSubredditList() {
         return true;
     }
 
     @Override
     protected void refreshActionBar(String subreddit, Bundle thingBundle) {
         bar.setDisplayHomeAsUpEnabled(thingBundle != null);
         adapter.setSubreddit(subreddit);
     }
 
     public boolean onNavigationItemSelected(int itemPosition, long itemId) {
         adapter.updateState(itemPosition);
 
         final String accountName = adapter.getAccountName();
         AccountPreferences.setLastAccount(prefs, accountName);
 
         int filter = adapter.getFilter();
         AccountPreferences.setLastSubredditFilter(prefs, filter);
 
         if (BuildConfig.DEBUG) {
             Log.d(TAG, "onNavigationItemSelected i:" + itemPosition
                     + " an:" + accountName + " f:" + filter);
         }
 
         // Quickly sync to check whether the user has new messages.
         if (AccountUtils.isAccount(accountName)) {
             // requestSync can trigger a strict mode warning by writing to disk.
             AsyncTask.SERIAL_EXECUTOR.execute(new Runnable() {
                 public void run() {
                     Account account = AccountUtils.getAccount(getApplicationContext(), accountName);
                     ContentResolver.requestSync(account, AccountProvider.AUTHORITY, Bundle.EMPTY);
                 }
             });
         }
 
         SubredditListFragment slf = getSubredditListFragment();
         ThingListFragment tlf = getThingListFragment();
         if (slf == null || !slf.getAccountName().equals(accountName)) {
             String subreddit;
             if (!isSinglePane && !TextUtils.isEmpty(requestedSubreddit)) {
                 subreddit = requestedSubreddit;
             } else {
                 subreddit = AccountPreferences.getLastSubreddit(prefs, accountName);
             }
             setSubredditListNavigation(subreddit, null);
         } else if (tlf != null && tlf.getFilter() != filter) {
             replaceThingListFragmentMultiPane();
         }
 
         // Invalidate menu so that mail icon disappears when switching back to
         // app storage account.
         invalidateOptionsMenu();
 
         return true;
     }
 
     @Override
     public void onSubredditSelected(String subreddit) {
         super.onSubredditSelected(subreddit);
         AccountPreferences.setLastSubreddit(prefs, getAccountName(), subreddit);
     }
 }
