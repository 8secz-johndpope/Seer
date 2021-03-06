 /*
  * Copyright (C) 2013 The Android Open Source Project
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
 package com.android.dialer;
 
 import android.app.Activity;
 import android.view.LayoutInflater;
 import android.view.ViewGroup;
 import android.widget.AbsListView;
 import android.widget.AbsListView.OnScrollListener;
 
 import com.android.contacts.common.list.ContactListItemView;
 import com.android.contacts.common.list.ContactEntryListAdapter;
 import com.android.contacts.common.list.PhoneNumberPickerFragment;
 import com.android.dialer.list.OnListFragmentScrolledListener;
 
 public class SearchFragment extends PhoneNumberPickerFragment {
 
     private OnListFragmentScrolledListener mActivityScrollListener;
 
     public SearchFragment() {
         setDirectorySearchEnabled(true);
     }
 
     @Override
     public void onAttach(Activity activity) {
         super.onAttach(activity);
 
         setQuickContactEnabled(true);
         setDarkTheme(false);
         setPhotoPosition(ContactListItemView.getDefaultPhotoPosition(true /* opposite */));
         setUseCallableUri(true);
 
         try {
             mActivityScrollListener = (OnListFragmentScrolledListener) activity;
         } catch (ClassCastException e) {
             throw new ClassCastException(activity.toString()
                     + " must implement OnListFragmentScrolledListener");
         }
     }
 
     @Override
     public void onStart() {
         super.onStart();
         if (isSearchMode()) {
             getAdapter().setHasHeader(0, false);
         }
         getListView().setOnScrollListener(new OnScrollListener() {
             @Override
             public void onScrollStateChanged(AbsListView view, int scrollState) {
                 mActivityScrollListener.onListFragmentScrollStateChange(scrollState);
             }
 
             @Override
             public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount,
                     int totalItemCount) {
             }
         });
     }
 
     @Override
     protected void setSearchMode(boolean flag) {
         super.setSearchMode(flag);
         // This hides the "All contacts with phone numbers" header in the search fragment
         final ContactEntryListAdapter adapter = getAdapter();
         if (adapter != null) {
             adapter.setHasHeader(0, false);
         }
     }
 }
