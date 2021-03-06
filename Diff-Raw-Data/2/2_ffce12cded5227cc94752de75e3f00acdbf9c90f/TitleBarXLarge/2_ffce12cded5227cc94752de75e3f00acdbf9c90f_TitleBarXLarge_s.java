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
 
 package com.android.browser;
 
 import com.android.browser.UrlInputView.UrlInputListener;
 
 import android.app.Activity;
 import android.app.SearchManager;
 import android.content.Context;
 import android.content.Intent;
 import android.content.res.Resources;
 import android.graphics.Bitmap;
 import android.graphics.drawable.Drawable;
 import android.text.TextUtils;
 import android.util.AttributeSet;
 import android.view.LayoutInflater;
 import android.view.View;
 import android.view.View.OnClickListener;
 import android.widget.CheckBox;
 import android.widget.ImageView;
 import android.widget.TextView;
 
 /**
  * tabbed title bar for xlarge screen browser
  */
 public class TitleBarXLarge extends TitleBarBase
     implements UrlInputListener, OnClickListener {
 
     private static final int PROGRESS_MAX = 100;
 
     private UiController mUiController;
 
     private Drawable mStopDrawable;
     private Drawable mReloadDrawable;
 
     private View mContainer;
     private View mBackButton;
     private View mForwardButton;
     private CheckBox mStar;
     private View mSearchButton;
     private View mFocusContainer;
     private View mUnfocusContainer;
     private View mGoButton;
     private ImageView mStopButton;
     private View mAllButton;
     private View mClearButton;
     private PageProgressView mProgressView;
     private UrlInputView mUrlFocused;
     private TextView mUrlUnfocused;
     private boolean mInLoad;
 
     public TitleBarXLarge(Activity activity, UiController controller) {
         super(activity);
         mUiController = controller;
         Resources resources = activity.getResources();
         mStopDrawable = resources.getDrawable(R.drawable.ic_stop_normal);
         mReloadDrawable = resources.getDrawable(R.drawable.ic_refresh_normal);
         rebuildLayout(activity, true);
     }
 
     private void rebuildLayout(Context context, boolean rebuildData) {
         LayoutInflater factory = LayoutInflater.from(context);
         factory.inflate(R.layout.url_bar, this);
 
         mContainer = findViewById(R.id.taburlbar);
         mUrlFocused = (UrlInputView) findViewById(R.id.url_focused);
         mUrlUnfocused = (TextView) findViewById(R.id.url_unfocused);
         mAllButton = findViewById(R.id.all_btn);
         // TODO: Change enabled states based on whether you can go
         // back/forward.  Probably should be done inside onPageStarted.
         mBackButton = findViewById(R.id.back);
         mForwardButton = findViewById(R.id.forward);
         mStar = (CheckBox) findViewById(R.id.star);
         mStopButton = (ImageView) findViewById(R.id.stop);
         mSearchButton = findViewById(R.id.search);
         mLockIcon = (ImageView) findViewById(R.id.lock);
         mGoButton = findViewById(R.id.go);
         mClearButton = findViewById(R.id.clear);
         mProgressView = (PageProgressView) findViewById(R.id.progress);
         mFocusContainer = findViewById(R.id.urlbar_focused);
         mUnfocusContainer = findViewById(R.id.urlbar_unfocused);
 
         mBackButton.setOnClickListener(this);
         mForwardButton.setOnClickListener(this);
         mStar.setOnClickListener(this);
         mAllButton.setOnClickListener(this);
         mStopButton.setOnClickListener(this);
         mSearchButton.setOnClickListener(this);
         mGoButton.setOnClickListener(this);
         mClearButton.setOnClickListener(this);
         mUrlFocused.setUrlInputListener(this);
         mUrlFocused.setContainer(mFocusContainer);
         mUrlFocused.setController(mUiController);
         mUnfocusContainer.setOnClickListener(this);
     }
 
     public void setCurrentUrlIsBookmark(boolean isBookmark) {
         mStar.setChecked(isBookmark);
     }
 
     @Override
     public void onClick(View v) {
         if (mUnfocusContainer == v) {
             setUrlMode(true);
         } else if (mBackButton == v) {
             mUiController.getCurrentTopWebView().goBack();
         } else if (mForwardButton == v) {
             mUiController.getCurrentTopWebView().goForward();
         } else if (mStar == v) {
             mUiController.bookmarkCurrentPage(
                     AddBookmarkPage.DEFAULT_FOLDER_ID);
         } else if (mAllButton == v) {
             mUiController.bookmarksOrHistoryPicker(false);
         } else if (mSearchButton == v) {
             search();
         } else if (mStopButton == v) {
             stopOrRefresh();
         } else if (mGoButton == v) {
             if (!TextUtils.isEmpty(mUrlFocused.getText())) {
                 onAction(mUrlFocused.getText().toString(), null);
             }
         } else if (mClearButton == v) {
             mUrlFocused.setText("");
         }
     }
 
     int getHeightWithoutProgress() {
         return mContainer.getHeight();
     }
 
     @Override
     void setFavicon(Bitmap icon) { }
 
     // UrlInputListener implementation
 
     @Override
     public void onAction(String text, String extra) {
         mUiController.getCurrentTopWebView().requestFocus();
         ((BaseUi) mUiController.getUi()).hideFakeTitleBar();
         Intent i = new Intent();
         i.setAction(Intent.ACTION_SEARCH);
         i.putExtra(SearchManager.QUERY, text);
         if (extra != null) {
             i.putExtra(SearchManager.EXTRA_DATA_KEY, extra);
         }
         mUiController.handleNewIntent(i);
         setUrlMode(false);
         setDisplayTitle(text);
     }
 
     @Override
     public void onDismiss() {
         mUiController.getCurrentTopWebView().requestFocus();
         ((BaseUi) mUiController.getUi()).hideFakeTitleBar();
         setUrlMode(false);
         setDisplayTitle(mUiController.getCurrentWebView().getUrl());
     }
 
     @Override
     public void onEdit(String text) {
         setDisplayTitle(text, true);
         if (text != null) {
             mUrlFocused.setSelection(text.length());
         }
     }
 
     private void setUrlMode(boolean focused) {
         swapUrlContainer(focused);
         if (focused) {
             mUrlFocused.selectAll();
             mUrlFocused.requestFocus();
             mUrlFocused.setDropDownWidth(mUnfocusContainer.getWidth());
             mUrlFocused.setDropDownHorizontalOffset(-mUrlFocused.getLeft());
             mSearchButton.setVisibility(View.GONE);
             mGoButton.setVisibility(View.VISIBLE);
         } else {
             mSearchButton.setVisibility(View.VISIBLE);
             mGoButton.setVisibility(View.GONE);
         }
     }
 
     private void swapUrlContainer(boolean focus) {
         mUnfocusContainer.setVisibility(focus ? View.GONE : View.VISIBLE);
         mFocusContainer.setVisibility(focus ? View.VISIBLE : View.GONE);
     }
 
     private void search() {
         setDisplayTitle("");
        mUrlUnfocused.requestFocus();
     }
 
     private void stopOrRefresh() {
         if (mInLoad) {
             mUiController.stopLoading();
         } else {
             mUiController.getCurrentTopWebView().reload();
         }
     }
 
     /**
      * Update the progress, from 0 to 100.
      */
     @Override
     void setProgress(int newProgress) {
         if (newProgress >= PROGRESS_MAX) {
             mProgressView.setProgress(PageProgressView.MAX_PROGRESS);
             mProgressView.setVisibility(View.GONE);
             mInLoad = false;
             mStopButton.setImageDrawable(mReloadDrawable);
         } else {
             if (!mInLoad) {
                 mProgressView.setVisibility(View.VISIBLE);
                 mInLoad = true;
                 mStopButton.setImageDrawable(mStopDrawable);
             }
             mProgressView.setProgress(newProgress * PageProgressView.MAX_PROGRESS
                     / PROGRESS_MAX);
         }
     }
 
     @Override
     /* package */ void setDisplayTitle(String title) {
         mUrlFocused.setText(title, false);
         mUrlUnfocused.setText(title);
     }
 
     void setDisplayTitle(String title, boolean filter) {
         mUrlFocused.setText(title, filter);
         mUrlUnfocused.setText(title);
     }
 
     /**
      * Custom CheckBox which does not toggle when pressed.  Used by mStar.
      */
     public static class CustomCheck extends CheckBox {
         public CustomCheck(Context context) {
             super(context);
         }
 
         public CustomCheck(Context context, AttributeSet attrs) {
             super(context, attrs);
         }
 
         public CustomCheck(Context context, AttributeSet attrs, int defStyle) {
             super(context, attrs, defStyle);
         }
 
         @Override
         public void toggle() {
             // Do nothing
         }
     }
 }
