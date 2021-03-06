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
 package com.android.contacts.list;
 
 import com.android.contacts.R;
 import com.android.contacts.editor.ContactEditorFragment;
 
 import android.content.Intent;
 import android.database.Cursor;
 import android.provider.ContactsContract.Contacts;
 import android.text.TextUtils;
 import android.view.LayoutInflater;
 import android.view.View;
 import android.view.ViewGroup;
 import android.widget.Button;
 import android.widget.FrameLayout;
 import android.widget.ListView;
 import android.widget.ProgressBar;
 import android.widget.TextView;
 
 /**
  * Fragment containing a contact list used for browsing (as compared to
  * picking a contact with one of the PICK intents).
  */
 public class DefaultContactBrowseListFragment extends ContactBrowseListFragment {
 
     private TextView mCounterHeaderView;
     private View mSearchHeaderView;
     private TextView mAccountFilterHeaderView;
     private View mAccountFilterHeaderContainer;
     private FrameLayout mProfileHeaderContainer;
     private View mProfileHeader;
     private Button mProfileMessage;
     private FrameLayout mMessageContainer;
     private View mProfileTitle;
 
     private View mPaddingView;
 
     public DefaultContactBrowseListFragment() {
         setPhotoLoaderEnabled(true);
         setSectionHeaderDisplayEnabled(true);
         setVisibleScrollbarEnabled(true);
     }
 
     @Override
     protected void onItemClick(int position, long id) {
         viewContact(getAdapter().getContactUri(position));
     }
 
     @Override
     protected ContactListAdapter createListAdapter() {
         DefaultContactListAdapter adapter = new DefaultContactListAdapter(getContext());
         adapter.setSectionHeaderDisplayEnabled(isSectionHeaderDisplayEnabled());
         adapter.setDisplayPhotos(true);
         return adapter;
     }
 
     @Override
     protected View inflateView(LayoutInflater inflater, ViewGroup container) {
         return inflater.inflate(R.layout.contacts_list_content, null);
     }
 
     @Override
     protected void onCreateView(LayoutInflater inflater, ViewGroup container) {
         super.onCreateView(inflater, container);
 
         mAccountFilterHeaderView = (TextView) getView().findViewById(R.id.account_filter_header);
         mAccountFilterHeaderContainer =
                 getView().findViewById(R.id.account_filter_header_container);
         mCounterHeaderView = (TextView) getView().findViewById(R.id.contacts_count);
 
         // Create an empty user profile header and hide it for now (it will be visible if the
         // contacts list will have no user profile).
         addEmptyUserProfileHeader(inflater);
         showEmptyUserProfile(false);
 
         // Putting the header view inside a container will allow us to make
         // it invisible later. See checkHeaderViewVisibility()
         FrameLayout headerContainer = new FrameLayout(inflater.getContext());
         mSearchHeaderView = inflater.inflate(R.layout.search_header, null, false);
         headerContainer.addView(mSearchHeaderView);
         getListView().addHeaderView(headerContainer, null, false);
         checkHeaderViewVisibility();
     }
 
     @Override
     public void setSearchMode(boolean flag) {
         super.setSearchMode(flag);
         checkHeaderViewVisibility();
     }
 
     private void checkHeaderViewVisibility() {
         if (mCounterHeaderView != null) {
             mCounterHeaderView.setVisibility(isSearchMode() ? View.GONE : View.VISIBLE);
         }
         updateFilterHeaderView();
 
         // Hide the search header by default. See showCount().
         if (mSearchHeaderView != null) {
             mSearchHeaderView.setVisibility(View.GONE);
         }
     }
 
     @Override
     public void setFilter(ContactListFilter filter) {
         super.setFilter(filter);
         updateFilterHeaderView();
     }
 
     private void updateFilterHeaderView() {
         ContactListFilter filter = getFilter();
         if (mAccountFilterHeaderView == null) {
             return; // Before onCreateView -- just ignore it.
         }
         if (filter != null && filter.filterType != ContactListFilter.FILTER_TYPE_ALL_ACCOUNTS &&
                 !isSearchMode() && filter.filterType != ContactListFilter.FILTER_TYPE_CUSTOM) {
             mAccountFilterHeaderContainer.setVisibility(View.VISIBLE);
             mAccountFilterHeaderView.setText(getContext().getString(
                     R.string.listAllContactsInAccount, filter.accountName));
         } else {
             mAccountFilterHeaderContainer.setVisibility(View.GONE);
         }
     }
 
     @Override
     protected void showCount(int partitionIndex, Cursor data) {
         if (!isSearchMode() && data != null) {
             int count = data.getCount();
             if (count != 0) {
                count -= (mUserProfileExists ? 1: 0);
                 String format = getResources().getQuantityText(
                         R.plurals.listTotalAllContacts, count).toString();
                 // Do not count the user profile in the contacts count
                 if (mUserProfileExists) {
                    getAdapter().setContactsCount(String.format(format, count));
                 } else {
                     mCounterHeaderView.setText(String.format(format, count));
                 }
             } else {
                 ContactListFilter filter = getFilter();
                 int filterType = filter != null ? filter.filterType
                         : ContactListFilter.FILTER_TYPE_ALL_ACCOUNTS;
                 switch (filterType) {
                     case ContactListFilter.FILTER_TYPE_ACCOUNT:
                         mCounterHeaderView.setText(getString(
                                 R.string.listTotalAllContactsZeroGroup, filter.accountName));
                         break;
                     case ContactListFilter.FILTER_TYPE_GROUP:
                         mCounterHeaderView.setText(
                                 getString(R.string.listTotalAllContactsZeroGroup, filter.title));
                         break;
                     case ContactListFilter.FILTER_TYPE_WITH_PHONE_NUMBERS_ONLY:
                         mCounterHeaderView.setText(R.string.listTotalPhoneContactsZero);
                         break;
                     case ContactListFilter.FILTER_TYPE_STARRED:
                         mCounterHeaderView.setText(R.string.listTotalAllContactsZeroStarred);
                         break;
                     case ContactListFilter.FILTER_TYPE_CUSTOM:
                         mCounterHeaderView.setText(R.string.listTotalAllContactsZeroCustom);
                         break;
                     default:
                         mCounterHeaderView.setText(R.string.listTotalAllContactsZero);
                         break;
                 }
             }
         } else {
             ContactListAdapter adapter = getAdapter();
             if (adapter == null) {
                 return;
             }
 
             // In search mode we only display the header if there is nothing found
             if (TextUtils.isEmpty(getQueryString()) || !adapter.areAllPartitionsEmpty()) {
                 mSearchHeaderView.setVisibility(View.GONE);
             } else {
                 TextView textView = (TextView) mSearchHeaderView.findViewById(
                         R.id.totalContactsText);
                 ProgressBar progress = (ProgressBar) mSearchHeaderView.findViewById(
                         R.id.progress);
                 if (adapter.isLoading()) {
                     textView.setText(R.string.search_results_searching);
                     progress.setVisibility(View.VISIBLE);
                 } else {
                     textView.setText(R.string.listFoundAllContactsZero);
                     progress.setVisibility(View.GONE);
                 }
                 mSearchHeaderView.setVisibility(View.VISIBLE);
             }
             showEmptyUserProfile(false);
         }
     }
 
     @Override
     protected void setProfileHeader() {
         mUserProfileExists = getAdapter().hasProfile();
         showEmptyUserProfile(!mUserProfileExists && !isSearchMode());
     }
 
     private void showEmptyUserProfile(boolean show) {
         // Changing visibility of just the mProfileHeader doesn't do anything unless
         // you change visibility of its children, hence the call to mCounterHeaderView
         // and mProfileTitle
         mProfileHeaderContainer.setVisibility(show ? View.VISIBLE : View.GONE);
         mProfileHeader.setVisibility(show ? View.VISIBLE : View.GONE);
         mCounterHeaderView.setVisibility(show ? View.VISIBLE : View.GONE);
         mProfileTitle.setVisibility(show ? View.VISIBLE : View.GONE);
         mMessageContainer.setVisibility(show ? View.VISIBLE : View.GONE);
         mProfileMessage.setVisibility(show ? View.VISIBLE : View.GONE);
 
         mPaddingView.setVisibility(show ? View.GONE : View.VISIBLE);
     }
 
     /**
      * This method creates a pseudo user profile contact. When the returned query doesn't have
      * a profile, this methods creates 2 views that are inserted as headers to the listview:
      * 1. A header view with the "ME" title and the contacts count.
      * 2. A button that prompts the user to create a local profile
      */
     private void addEmptyUserProfileHeader(LayoutInflater inflater) {
 
         ListView list = getListView();
         // Put a header with the "ME" name and a view for the number of contacts
         // The view is embedded in a frame view since you cannot change the visibility of a
         // view in a ListView without having a parent view.
         mProfileHeaderContainer = new FrameLayout(inflater.getContext());
         mProfileHeader = inflater.inflate(R.layout.user_profile_header, null, false);
         mCounterHeaderView = (TextView) mProfileHeader.findViewById(R.id.contacts_count);
         mProfileTitle = mProfileHeader.findViewById(R.id.profile_title);
         mProfileHeaderContainer.addView(mProfileHeader);
         list.addHeaderView(mProfileHeaderContainer, null, false);
 
         // Add a selectable view with a message inviting the user to create a local profile
         mMessageContainer = new FrameLayout(inflater.getContext());
         mProfileMessage = (Button)inflater.inflate(R.layout.user_profile_button, null, false);
         mMessageContainer.addView(mProfileMessage);
         list.addHeaderView(mMessageContainer, null, true);
 
         mProfileMessage.setOnClickListener(new View.OnClickListener() {
             public void onClick(View v) {
                 Intent intent = new Intent(Intent.ACTION_INSERT, Contacts.CONTENT_URI);
                 intent.putExtra(ContactEditorFragment.INTENT_EXTRA_NEW_LOCAL_PROFILE, true);
                 startActivity(intent);
             }
         });
 
         View paddingViewContainer =
                 inflater.inflate(R.layout.contact_detail_list_padding, null, false);
         mPaddingView = paddingViewContainer.findViewById(R.id.contact_detail_list_padding);
         mPaddingView.setVisibility(View.GONE);
         getListView().addHeaderView(paddingViewContainer);
     }
 }
