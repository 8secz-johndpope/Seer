 /*
  * Copyright (C) 2011 Google Inc.
  * Licensed to The Android Open Source Project.
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
 package com.android.dialer.list;
 
 import android.content.Context;
 import android.content.res.Resources;
 import android.database.Cursor;
 import android.database.DataSetObserver;
 import android.view.MotionEvent;
 import android.view.View;
 import android.view.ViewConfiguration;
 import android.view.ViewGroup;
 import android.widget.BaseAdapter;
 import android.widget.FrameLayout;
 
 import com.android.dialer.R;
 import com.android.dialer.calllog.CallLogAdapter;
 import com.android.dialer.calllog.CallLogNotificationsHelper;
 import com.android.dialer.calllog.CallLogQueryHandler;
 import com.android.dialer.list.SwipeHelper.OnItemGestureListener;
 import com.android.dialer.list.SwipeHelper.SwipeHelperCallback;
 
 /**
  * An adapter that combines items from {@link com.android.contacts.common.list.ContactTileAdapter}
  * and {@link com.android.dialer.calllog.CallLogAdapter} into a single list.
  */
 public class PhoneFavoriteMergedAdapter extends BaseAdapter {
 
     private class CustomDataSetObserver extends DataSetObserver {
         @Override
         public void onChanged() {
             notifyDataSetChanged();
         }
     }
 
     private static final String TAG = PhoneFavoriteMergedAdapter.class.getSimpleName();
 
     private static final int ALL_CONTACTS_BUTTON_ITEM_ID = -1;
     private final PhoneFavoritesTileAdapter mContactTileAdapter;
     private final CallLogAdapter mCallLogAdapter;
     private final View mLoadingView;
     private final View mShowAllContactsButton;
 
     private final int mCallLogPadding;
 
     private final Context mContext;
 
     private final DataSetObserver mObserver;
 
     private final CallLogQueryHandler mCallLogQueryHandler;
 
     private final OnItemGestureListener mCallLogOnItemSwipeListener =
             new OnItemGestureListener() {
         @Override
         public void onSwipe(View view) {
             mCallLogQueryHandler.markNewCallsAsOld();
             mCallLogQueryHandler.markNewVoicemailsAsOld();
             CallLogNotificationsHelper.removeMissedCallNotifications();
             CallLogNotificationsHelper.updateVoicemailNotifications(mContext);
             mCallLogQueryHandler.fetchNewCalls(CallLogQueryHandler.CALL_TYPE_ALL);
         }
 
         @Override
         public void onTouch() {}
 
         @Override
         public boolean isSwipeEnabled() {
             return true;
         }
     };
 
     private final CallLogQueryHandler.Listener mCallLogQueryHandlerListener =
             new CallLogQueryHandler.Listener() {
         @Override
         public void onVoicemailStatusFetched(Cursor statusCursor) {}
 
         @Override
         public void onCallsFetched(Cursor combinedCursor) {
             mCallLogAdapter.invalidateCache();
             mCallLogAdapter.changeCursor(combinedCursor);
             mCallLogAdapter.notifyDataSetChanged();
         }
     };
 
     public PhoneFavoriteMergedAdapter(Context context,
             PhoneFavoritesTileAdapter contactTileAdapter,
             CallLogAdapter callLogAdapter,
             View loadingView,
             View showAllContactsButton) {
         final Resources resources = context.getResources();
         mContext = context;
         mCallLogPadding = resources.getDimensionPixelSize(R.dimen.recent_call_log_item_padding);
         mContactTileAdapter = contactTileAdapter;
         mCallLogAdapter = callLogAdapter;
         mObserver = new CustomDataSetObserver();
         mCallLogAdapter.registerDataSetObserver(mObserver);
         mContactTileAdapter.registerDataSetObserver(mObserver);
         mLoadingView = loadingView;
         mShowAllContactsButton = showAllContactsButton;
         mCallLogQueryHandler = new CallLogQueryHandler(mContext.getContentResolver(),
                 mCallLogQueryHandlerListener);
     }
 
     @Override
     public boolean isEmpty() {
         // This adapter will always contain at least the all contacts button
         return false;
     }
 
     @Override
     public int getCount() {
         return mContactTileAdapter.getCount() + mCallLogAdapter.getCount() + 1;
     }
 
     @Override
     public Object getItem(int position) {
         final int callLogAdapterCount = mCallLogAdapter.getCount();
 
         if (callLogAdapterCount > 0) {
             if (position < callLogAdapterCount) {
                 return mCallLogAdapter.getItem(position);
             }
             // Set position to the position of the actual favorite contact in the favorites adapter
             position = getAdjustedFavoritePosition(position, callLogAdapterCount);
         }
         return mContactTileAdapter.getItem(position);
     }
 
     /**
      * In order to ensure that items have stable ids (for animation purposes), we need to
      * guarantee that every single item has a unique ID, even across data set changes.
      *
      * These are the ranges of IDs reserved for each item type.
      *
      * -(N + 1) to -2: CallLogAdapterItems, where N is equal to the number of call log items
      * -1: All contacts button
      * 0 to (N -1): Rows of tiled contacts, where N is equal to the max rows of tiled contacts
      * N to infinity: Rows of regular contacts. Their item id is calculated by N + contact_id,
      * where contact_id is guaranteed to never be negative.
      */
     @Override
     public long getItemId(int position) {
         final int callLogAdapterCount = mCallLogAdapter.getCount();
         if (position < callLogAdapterCount) {
             // Call log items are not animated, so reusing their position for IDs is fine.
             return ALL_CONTACTS_BUTTON_ITEM_ID - 1 - position;
         } else if (position < (callLogAdapterCount + mContactTileAdapter.getCount())) {
             return mContactTileAdapter.getItemId(position - callLogAdapterCount);
         } else {
             // All contacts button
             return ALL_CONTACTS_BUTTON_ITEM_ID;
         }
     }
 
     @Override
     public boolean hasStableIds() {
         return true;
     }
 
     @Override
     public int getViewTypeCount() {
         return (mContactTileAdapter.getViewTypeCount() + mCallLogAdapter.getViewTypeCount() + 1);
     }
 
     @Override
     public int getItemViewType(int position) {
         final int callLogAdapterCount = mCallLogAdapter.getCount();
 
         if (position < callLogAdapterCount) {
             // View type of the call log adapter is the last view type of the contact tile adapter
             // + 1
             return mContactTileAdapter.getViewTypeCount();
         } else if (position < getCount() - 1) {
             return mContactTileAdapter.getItemViewType(
                     getAdjustedFavoritePosition(position, callLogAdapterCount));
         } else {
             // View type of the show all contact button is the last view type of the contact tile
             // adapter + 2
             return mContactTileAdapter.getViewTypeCount() + 1;
         }
     }
 
     @Override
     public View getView(int position, View convertView, ViewGroup parent) {
         final int callLogAdapterCount = mCallLogAdapter.getCount();
 
         if (position == getCount() - 1) {
             return mShowAllContactsButton;
         }
 
         if (callLogAdapterCount > 0) {
             if (position == 0) {
                 final SwipeableCallLogRow wrapper;
                 if (convertView == null) {
                     wrapper = new SwipeableCallLogRow(mContext);
                     wrapper.setOnItemSwipeListener(mCallLogOnItemSwipeListener);
                 } else {
                     wrapper = (SwipeableCallLogRow) convertView;
                 }
 
                 // Special case wrapper view for the most recent call log item. This allows
                 // us to create a card-like effect for the more recent call log item in
                 // the PhoneFavoriteMergedAdapter, but keep the original look of the item in
                 // the CallLogAdapter.
                 final View view = mCallLogAdapter.getView(position, convertView == null ?
                         null : wrapper.getChildAt(0), parent);
                 wrapper.removeAllViews();
                 wrapper.addView(view);
                 return wrapper;
             }
             // Set position to the position of the actual favorite contact in the
             // favorites adapter
             position = getAdjustedFavoritePosition(position, callLogAdapterCount);
         }
 
         // Favorites section
         final View view = mContactTileAdapter.getView(position, convertView, parent);
         if (position >= mContactTileAdapter.getMaxTiledRows()) {
             final FrameLayout frameLayout = (FrameLayout) view;
             final View child = frameLayout.getChildAt(0);
             FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                     FrameLayout.LayoutParams.WRAP_CONTENT,
                     FrameLayout.LayoutParams.WRAP_CONTENT);
             child.setLayoutParams(params);
         }
         return view;
     }
 
     @Override
     public boolean areAllItemsEnabled() {
         // If "all" section is being loaded we'll show mLoadingView, which is not enabled.
         // Otherwise check the all the other components in the ListView and return appropriate
         // result.
         return mCallLogAdapter.areAllItemsEnabled() && mContactTileAdapter.areAllItemsEnabled();
     }
 
     @Override
     public boolean isEnabled(int position) {
         final int callLogAdapterCount = mCallLogAdapter.getCount();
         if (position < callLogAdapterCount) {
             return mCallLogAdapter.isEnabled(position);
         } else { // For favorites section
             return mContactTileAdapter.isEnabled(
                     getAdjustedFavoritePosition(position, callLogAdapterCount));
         }
     }
 
     private int getAdjustedFavoritePosition(int position, int callLogAdapterCount) {
         return position - callLogAdapterCount;
     }
 
     /**
      * The swipeable call log row.
      * See also {@link PhoneFavoritesTileAdapter.ContactTileRow}.
      */
     private class SwipeableCallLogRow extends FrameLayout implements SwipeHelperCallback {
         private SwipeHelper mSwipeHelper;
         private OnItemGestureListener mOnItemSwipeListener;
 
         public SwipeableCallLogRow(Context context) {
             super(context);
             final float densityScale = getResources().getDisplayMetrics().density;
             final float pagingTouchSlop = ViewConfiguration.get(context)
                     .getScaledPagingTouchSlop();
             mSwipeHelper = new SwipeHelper(context, SwipeHelper.X, this,
                     densityScale, pagingTouchSlop);
         }
 
         @Override
         public void addView(View view) {
             view.setBackgroundResource(R.drawable.dialer_recent_card_bg);
 
             final FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                     FrameLayout.LayoutParams.WRAP_CONTENT);
             params.setMarginsRelative(mCallLogPadding, mCallLogPadding, mCallLogPadding,
                     mCallLogPadding);
             view.setLayoutParams(params);
 
             super.addView(view);
         }
 
         @Override
         public View getChildAtPosition(MotionEvent ev) {
             return getChildCount() > 0 ? getChildAt(0) : null;
         }
 
         @Override
         public View getChildContentView(View v) {
             return v.findViewById(R.id.call_log_list_item);
         }
 
         @Override
         public void onScroll() {}
 
         @Override
         public boolean canChildBeDismissed(View v) {
             return true;
         }
 
         @Override
         public void onBeginDrag(View v) {
             // We do this so the underlying ScrollView knows that it won't get
             // the chance to intercept events anymore
             requestDisallowInterceptTouchEvent(true);
         }
 
         @Override
         public void onChildDismissed(View v) {
             if (v != null && mOnItemSwipeListener != null) {
                 mOnItemSwipeListener.onSwipe(v);
             }
             removeAllViews();
         }
 
         @Override
         public void onDragCancelled(View v) {}
 
         @Override
         public boolean onInterceptTouchEvent(MotionEvent ev) {
             if (mSwipeHelper != null) {
                 return mSwipeHelper.onInterceptTouchEvent(ev) || super.onInterceptTouchEvent(ev);
             } else {
                 return super.onInterceptTouchEvent(ev);
             }
         }
 
         @Override
         public boolean onTouchEvent(MotionEvent ev) {
             if (mSwipeHelper != null) {
                 return mSwipeHelper.onTouchEvent(ev) || super.onTouchEvent(ev);
             } else {
                 return super.onTouchEvent(ev);
             }
         }
 
         public void setOnItemSwipeListener(OnItemGestureListener listener) {
             mOnItemSwipeListener = listener;
         }
     }
 }
