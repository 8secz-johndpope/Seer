 /*
  * Copyright (C) 2010 The Android Open Source Project
  *
  * Licensed under the Apache License, Version 2.0 (the "License"); you may not
  * use this file except in compliance with the License. You may obtain a copy of
  * the License at
  *
  * http://www.apache.org/licenses/LICENSE-2.0
  *
  * Unless required by applicable law or agreed to in writing, software
  * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
  * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
  * License for the specific language governing permissions and limitations under
  * the License.
  */
 
 package com.android.browser;
 
 import android.content.Context;
 import android.util.AttributeSet;
 import android.view.View;
 import android.webkit.WebView;
 
 import java.util.Map;
 
 /**
  * Manage WebView scroll events
  */
 public class BrowserWebView extends WebView {
 
     private boolean mBackgroundRemoved = false;
     private TitleBar mTitleBar;
 
     /**
      * @param context
      * @param attrs
      * @param defStyle
      * @param javascriptInterfaces
      */
     public BrowserWebView(Context context, AttributeSet attrs, int defStyle,
             Map<String, Object> javascriptInterfaces, boolean privateBrowsing) {
         super(context, attrs, defStyle, javascriptInterfaces, privateBrowsing);
     }
 
     /**
      * @param context
      * @param attrs
      * @param defStyle
      */
     public BrowserWebView(
             Context context, AttributeSet attrs, int defStyle, boolean privateBrowsing) {
         super(context, attrs, defStyle, privateBrowsing);
     }
 
     /**
      * @param context
      * @param attrs
      */
     public BrowserWebView(Context context, AttributeSet attrs) {
         super(context, attrs);
     }
 
     /**
      * @param context
      */
     public BrowserWebView(Context context) {
         super(context);
     }
 
     @Override
     protected int getTitleHeight() {
         return (mTitleBar != null) ? mTitleBar.getEmbeddedHeight() : 0;
     }
 
     void hideEmbeddedTitleBar() {
         scrollBy(0, getVisibleTitleHeight());
     }
 
     @Override
     public void setEmbeddedTitleBar(final View title) {
         super.setEmbeddedTitleBar(title);
         mTitleBar = (TitleBar) title;
     }
 
     public boolean hasTitleBar() {
         return (mTitleBar != null);
     }
 
     @Override
     protected void onDraw(android.graphics.Canvas c) {
         super.onDraw(c);
         if (!mBackgroundRemoved && getRootView().getBackground() != null) {
             mBackgroundRemoved = true;
             post(new Runnable() {
                 public void run() {
                     getRootView().setBackgroundDrawable(null);
                 }
             });
         }
     }
 
    @Override
    protected void updateCachedTextfield(String updatedText) {
        super.updateCachedTextfield(updatedText);
        CrashRecoveryHandler handler = CrashRecoveryHandler.getInstance();
        if (handler != null) {
            handler.backupState();
        }
    }

 }
