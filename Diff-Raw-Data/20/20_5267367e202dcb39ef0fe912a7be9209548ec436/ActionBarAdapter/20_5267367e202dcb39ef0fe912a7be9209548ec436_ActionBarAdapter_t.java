 /*
  * Copyright (C) 2010 The Android Open Source Project
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
 
 package com.android.contacts.activities;
 
 import com.android.contacts.R;
 import com.android.contacts.activities.ActionBarAdapter.Listener.Action;
 import com.android.contacts.list.ContactsRequest;
 
 import android.app.ActionBar;
 import android.app.ActionBar.LayoutParams;
 import android.app.ActionBar.Tab;
 import android.app.FragmentTransaction;
 import android.content.Context;
 import android.content.SharedPreferences;
 import android.os.Bundle;
 import android.preference.PreferenceManager;
 import android.text.TextUtils;
 import android.view.LayoutInflater;
 import android.view.View;
 import android.widget.SearchView;
 import android.widget.SearchView.OnCloseListener;
 import android.widget.SearchView.OnQueryTextListener;
 
 /**
  * Adapter for the action bar at the top of the Contacts activity.
  */
 public class ActionBarAdapter implements OnQueryTextListener, OnCloseListener {
 
     public interface Listener {
         public enum Action {
             CHANGE_SEARCH_QUERY, START_SEARCH_MODE, STOP_SEARCH_MODE
         }
 
         void onAction(Action action);
 
         /**
          * Called when the user selects a tab.  The new tab can be obtained using
          * {@link #getCurrentTab}.
          */
         void onSelectedTabChanged();
     }
 
     private static final String EXTRA_KEY_SEARCH_MODE = "navBar.searchMode";
     private static final String EXTRA_KEY_QUERY = "navBar.query";
     private static final String EXTRA_KEY_SELECTED_TAB = "navBar.selectedTab";
 
     private static final String PERSISTENT_LAST_TAB = "actionBarAdapter.lastTab";
 
     private boolean mSearchMode;
     private String mQueryString;
 
     private SearchView mSearchView;
 
     private final Context mContext;
     private final SharedPreferences mPrefs;
 
     private Listener mListener;
 
     private final ActionBar mActionBar;
     private final MyTabListener mTabListener = new MyTabListener();
 
     private boolean mShowHomeIcon;
 
     public enum TabState {
         FAVORITES, ALL, GROUPS;
 
         public static TabState fromInt(int value) {
             switch (value) {
                 case 0:
                     return FAVORITES;
                 case 1:
                     return ALL;
                 case 2:
                     return GROUPS;
             }
             throw new IllegalArgumentException("Invalid value: " + value);
         }
     }
 
     private static final TabState DEFAULT_TAB = TabState.ALL;
     private TabState mCurrentTab = DEFAULT_TAB;
 
     public ActionBarAdapter(Context context, Listener listener, ActionBar actionBar) {
         mContext = context;
         mListener = listener;
         mActionBar = actionBar;
         mPrefs = PreferenceManager.getDefaultSharedPreferences(mContext);
 
         mShowHomeIcon = mContext.getResources().getBoolean(R.bool.show_home_icon);
 
         // Set up search view.
         View customSearchView = LayoutInflater.from(mContext).inflate(R.layout.custom_action_bar,
                 null);
         int searchViewWidth = mContext.getResources().getDimensionPixelSize(
                 R.dimen.search_view_width);
         if (searchViewWidth == 0) {
             searchViewWidth = LayoutParams.MATCH_PARENT;
         }
         LayoutParams layoutParams = new LayoutParams(searchViewWidth, LayoutParams.WRAP_CONTENT);
         mSearchView = (SearchView) customSearchView.findViewById(R.id.search_view);
         mSearchView.setQueryHint(mContext.getString(R.string.hint_findContacts));
         mSearchView.setOnQueryTextListener(this);
         mSearchView.setOnCloseListener(this);
         mSearchView.setQuery(mQueryString, false);
         mActionBar.setCustomView(customSearchView, layoutParams);
 
         // Set up tabs
         addTab(TabState.FAVORITES, mContext.getString(R.string.contactsFavoritesLabel));
         addTab(TabState.ALL, mContext.getString(R.string.contactsAllLabel));
         addTab(TabState.GROUPS, mContext.getString(R.string.contactsGroupsLabel));
     }
 
     public void initialize(Bundle savedState, ContactsRequest request) {
         if (savedState == null) {
             mSearchMode = request.isSearchMode();
             mQueryString = request.getQueryString();
             mCurrentTab = loadLastTabPreference();
         } else {
             mSearchMode = savedState.getBoolean(EXTRA_KEY_SEARCH_MODE);
             mQueryString = savedState.getString(EXTRA_KEY_QUERY);
 
             // Just set to the field here.  The listener will be notified by update().
             mCurrentTab = TabState.fromInt(savedState.getInt(EXTRA_KEY_SELECTED_TAB));
         }
         update();
     }
 
     public void setListener(Listener listener) {
         mListener = listener;
     }
 
     private void addTab(TabState tabState, String text) {
         final Tab tab = mActionBar.newTab();
         tab.setTag(tabState);
         tab.setText(text);
         tab.setTabListener(mTabListener);
         mActionBar.addTab(tab);
     }
 
     private class MyTabListener implements ActionBar.TabListener {
         /**
          * If true, it won't call {@link #setCurrentTab} in {@link #onTabSelected}.
          * This flag is used when we want to programmatically update the current tab without
          * {@link #onTabSelected} getting called.
          */
         public boolean mIgnoreTabSelected;
 
         @Override public void onTabReselected(Tab tab, FragmentTransaction ft) { }
         @Override public void onTabUnselected(Tab tab, FragmentTransaction ft) { }
 
         @Override public void onTabSelected(Tab tab, FragmentTransaction ft) {
             if (!mIgnoreTabSelected) {
                 setCurrentTab((TabState)tab.getTag());
             }
         }
     }
 
     /**
      * Change the current tab, and notify the listener.
      */
     public void setCurrentTab(TabState tab) {
         setCurrentTab(tab, true);
     }
 
     /**
      * Change the current tab
      */
     public void setCurrentTab(TabState tab, boolean notifyListener) {
         if (tab == null) throw new NullPointerException();
         if (tab == mCurrentTab) {
             return;
         }
         mCurrentTab = tab;
 
         int index = mCurrentTab.ordinal();
         if ((mActionBar.getNavigationMode() == ActionBar.NAVIGATION_MODE_TABS)
                 && (index != mActionBar.getSelectedNavigationIndex())) {
             mActionBar.setSelectedNavigationItem(index);
         }
 
         if (notifyListener && mListener != null) mListener.onSelectedTabChanged();
         saveLastTabPreference(mCurrentTab);
     }
 
     public TabState getCurrentTab() {
         return mCurrentTab;
     }
 
     public boolean isSearchMode() {
         return mSearchMode;
     }
 
     public void setSearchMode(boolean flag) {
         if (mSearchMode != flag) {
             mSearchMode = flag;
             update();
             if (mSearchView == null) {
                 return;
             }
             if (mSearchMode) {
                 setFocusOnSearchView();
             } else {
                 mSearchView.setQuery(null, false);
             }
         }
     }
 
     public String getQueryString() {
         return mQueryString;
     }
 
     public void setQueryString(String query) {
         mQueryString = query;
         if (mSearchView != null) {
             mSearchView.setQuery(query, false);
         }
     }
 
     /** @return true if the "UP" icon is showing. */
     public boolean isUpShowing() {
         return mSearchMode; // Only shown on the search mode.
     }
 
     private void updateDisplayOptions() {
         // All the flags we may change in this method.
         final int MASK = ActionBar.DISPLAY_SHOW_TITLE | ActionBar.DISPLAY_SHOW_HOME
                 | ActionBar.DISPLAY_HOME_AS_UP | ActionBar.DISPLAY_SHOW_CUSTOM;
 
         // The current flags set to the action bar.  (only the ones that we may change here)
         final int current = mActionBar.getDisplayOptions() & MASK;
 
         // Build the new flags...
         int newFlags = 0;
         newFlags |= ActionBar.DISPLAY_SHOW_TITLE;
         if (mShowHomeIcon) {
             newFlags |= ActionBar.DISPLAY_SHOW_HOME;
         }
         if (mSearchMode) {
             newFlags |= ActionBar.DISPLAY_SHOW_HOME;
             newFlags |= ActionBar.DISPLAY_HOME_AS_UP;
             newFlags |= ActionBar.DISPLAY_SHOW_CUSTOM;
         }
         mActionBar.setHomeButtonEnabled(mSearchMode);
 
         if (current != newFlags) {
             // Pass the mask here to preserve other flags that we're not interested here.
             mActionBar.setDisplayOptions(newFlags, MASK);
         }
     }
 
     private void update() {
         if (mSearchMode) {
            setFocusOnSearchView();
             if (mActionBar.getNavigationMode() != ActionBar.NAVIGATION_MODE_STANDARD) {
                 mActionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
             }
             if (mListener != null) {
                 mListener.onAction(Action.START_SEARCH_MODE);
             }
         } else {
             if (mActionBar.getNavigationMode() != ActionBar.NAVIGATION_MODE_TABS) {
                 // setNavigationMode will trigger onTabSelected() with the tab which was previously
                 // selected.
                 // The issue is that when we're first switching to the tab navigation mode after
                 // screen orientation changes, onTabSelected() will get called with the first tab
                 // (i.e. favorite), which would results in mCurrentTab getting set to FAVORITES and
                 // we'd lose restored tab.
                 // So let's just disable the callback here temporarily.  We'll notify the listener
                 // after this anyway.
                 mTabListener.mIgnoreTabSelected = true;
                 mActionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
                 mActionBar.setSelectedNavigationItem(mCurrentTab.ordinal());
                 mTabListener.mIgnoreTabSelected = false;
             }
             mActionBar.setTitle(null);
             if (mListener != null) {
                 mListener.onAction(Action.STOP_SEARCH_MODE);
                 mListener.onSelectedTabChanged();
             }
         }
         updateDisplayOptions();
     }
 
     @Override
     public boolean onQueryTextChange(String queryString) {
         // TODO: Clean up SearchView code because it keeps setting the SearchView query,
         // invoking onQueryChanged, setting up the fragment again, invalidating the options menu,
         // storing the SearchView again, and etc... unless we add in the early return statements.
         if (queryString.equals(mQueryString)) {
             return false;
         }
         mQueryString = queryString;
         if (!mSearchMode) {
             if (!TextUtils.isEmpty(queryString)) {
                 setSearchMode(true);
             }
         } else if (mListener != null) {
             mListener.onAction(Action.CHANGE_SEARCH_QUERY);
         }
 
         return true;
     }
 
     @Override
     public boolean onQueryTextSubmit(String query) {
         return true;
     }
 
     @Override
     public boolean onClose() {
         setSearchMode(false);
         return false;
     }
 
     public void onSaveInstanceState(Bundle outState) {
         outState.putBoolean(EXTRA_KEY_SEARCH_MODE, mSearchMode);
         outState.putString(EXTRA_KEY_QUERY, mQueryString);
         outState.putInt(EXTRA_KEY_SELECTED_TAB, mCurrentTab.ordinal());
     }
 
     private void setFocusOnSearchView() {
         mSearchView.requestFocus();
         mSearchView.setIconified(false); // Workaround for the "IME not popping up" issue.
     }
 
     private void saveLastTabPreference(TabState tab) {
         mPrefs.edit().putInt(PERSISTENT_LAST_TAB, tab.ordinal()).apply();
     }
 
     private TabState loadLastTabPreference() {
         try {
             return TabState.fromInt(mPrefs.getInt(PERSISTENT_LAST_TAB, DEFAULT_TAB.ordinal()));
         } catch (IllegalArgumentException e) {
             // Preference is corrupt?
             return DEFAULT_TAB;
         }
     }
 }
