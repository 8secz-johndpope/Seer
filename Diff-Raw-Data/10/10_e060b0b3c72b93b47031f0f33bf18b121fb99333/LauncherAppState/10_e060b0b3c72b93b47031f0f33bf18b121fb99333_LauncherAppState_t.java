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
 
 package com.android.launcher3;
 
 import android.app.SearchManager;
 import android.content.*;
 import android.content.res.Configuration;
 import android.database.ContentObserver;
 import android.os.Handler;
 import android.util.Log;
 
 import java.lang.ref.WeakReference;
 
 public class LauncherAppState {
     private static final String SHARED_PREFERENCES_KEY = "com.android.launcher3.prefs";
 
     private LauncherModel mModel;
     private IconCache mIconCache;
     private WidgetPreviewLoader.CacheDb mWidgetPreviewCacheDb;
     private boolean mIsScreenLarge;
     private float mScreenDensity;
     private int mLongPressTimeout = 300;
 
     private static WeakReference<LauncherProvider> sLauncherProvider;
     private static Context sContext;
 
     private static Object mLock = new Object();
     private static LauncherAppState INSTANCE;
 
     public static LauncherAppState getInstance() {
         if (INSTANCE == null) {
             INSTANCE = new LauncherAppState();
         }
         return INSTANCE;
     }
 
     public Context getContext() {
         return sContext;
     }
 
     public static void setApplicationContext(Context context) {
         if (sContext != null) {
            Log.w(Launcher.TAG, "setApplicationContext called twice! old=" + sContext + " new=" + context);
         }
         sContext = context.getApplicationContext();
     }
 
     private LauncherAppState() {
         if (sContext == null) {
             throw new IllegalStateException("LauncherAppState inited before app context set");
         }
 
         Log.v(Launcher.TAG, "LauncherAppState inited");
 
         if (sContext.getResources().getBoolean(R.bool.debug_memory_enabled)) {
             MemoryTracker.startTrackingMe(sContext, "L");
         }
 
         // set sIsScreenXLarge and mScreenDensity *before* creating icon cache
         mIsScreenLarge = sContext.getResources().getBoolean(R.bool.is_large_screen);
         mScreenDensity = sContext.getResources().getDisplayMetrics().density;
 
         mWidgetPreviewCacheDb = new WidgetPreviewLoader.CacheDb(sContext);
         mIconCache = new IconCache(sContext);
         mModel = new LauncherModel(this, mIconCache);
 
         // Register intent receivers
         IntentFilter filter = new IntentFilter(Intent.ACTION_PACKAGE_ADDED);
         filter.addAction(Intent.ACTION_PACKAGE_REMOVED);
         filter.addAction(Intent.ACTION_PACKAGE_CHANGED);
         filter.addDataScheme("package");
         sContext.registerReceiver(mModel, filter);
         filter = new IntentFilter();
         filter.addAction(Intent.ACTION_EXTERNAL_APPLICATIONS_AVAILABLE);
         filter.addAction(Intent.ACTION_EXTERNAL_APPLICATIONS_UNAVAILABLE);
         filter.addAction(Intent.ACTION_LOCALE_CHANGED);
         filter.addAction(Intent.ACTION_CONFIGURATION_CHANGED);
         sContext.registerReceiver(mModel, filter);
         filter = new IntentFilter();
         filter.addAction(SearchManager.INTENT_GLOBAL_SEARCH_ACTIVITY_CHANGED);
         sContext.registerReceiver(mModel, filter);
         filter = new IntentFilter();
         filter.addAction(SearchManager.INTENT_ACTION_SEARCHABLES_CHANGED);
         sContext.registerReceiver(mModel, filter);
 
         // Register for changes to the favorites
         ContentResolver resolver = sContext.getContentResolver();
         resolver.registerContentObserver(LauncherSettings.Favorites.CONTENT_URI, true,
                 mFavoritesObserver);
     }
 
     /**
      * Call from Application.onTerminate(), which is not guaranteed to ever be called.
      */
     public void onTerminate() {
         sContext.unregisterReceiver(mModel);
 
         ContentResolver resolver = sContext.getContentResolver();
         resolver.unregisterContentObserver(mFavoritesObserver);
     }
 
     /**
      * Receives notifications whenever the user favorites have changed.
      */
     private final ContentObserver mFavoritesObserver = new ContentObserver(new Handler()) {
         @Override
         public void onChange(boolean selfChange) {
             // If the database has ever changed, then we really need to force a reload of the
             // workspace on the next load
             mModel.resetLoadedState(false, true);
             mModel.startLoaderFromBackground();
         }
     };
 
     LauncherModel setLauncher(Launcher launcher) {
         if (mModel == null) {
             throw new IllegalStateException("setLauncher() called before init()");
         }
         mModel.initialize(launcher);
         return mModel;
     }
 
     IconCache getIconCache() {
         return mIconCache;
     }
 
     LauncherModel getModel() {
         return mModel;
     }
 
     WidgetPreviewLoader.CacheDb getWidgetPreviewCacheDb() {
         return mWidgetPreviewCacheDb;
     }
 
     static void setLauncherProvider(LauncherProvider provider) {
         sLauncherProvider = new WeakReference<LauncherProvider>(provider);
     }
 
     static LauncherProvider getLauncherProvider() {
         return sLauncherProvider.get();
     }
 
     public static String getSharedPreferencesKey() {
         return SHARED_PREFERENCES_KEY;
     }
 
     public boolean isScreenLarge() {
         return mIsScreenLarge;
     }
 
     public static boolean isScreenLandscape(Context context) {
         return context.getResources().getConfiguration().orientation ==
             Configuration.ORIENTATION_LANDSCAPE;
     }
 
     public float getScreenDensity() {
         return mScreenDensity;
     }
 
     public int getLongPressTimeout() {
         return mLongPressTimeout;
     }
 }
