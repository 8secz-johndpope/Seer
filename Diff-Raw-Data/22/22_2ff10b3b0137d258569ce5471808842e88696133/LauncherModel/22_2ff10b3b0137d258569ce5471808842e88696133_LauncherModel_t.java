 /*
  * Copyright (C) 2008 The Android Open Source Project
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
 
 package com.android.launcher2;
 
 import android.appwidget.AppWidgetManager;
 import android.appwidget.AppWidgetProviderInfo;
 import android.content.BroadcastReceiver;
 import android.content.ComponentName;
 import android.content.ContentProviderClient;
 import android.content.ContentResolver;
 import android.content.ContentValues;
 import android.content.Intent;
 import android.content.Intent.ShortcutIconResource;
 import android.content.Context;
 import android.content.pm.ActivityInfo;
 import android.content.pm.PackageManager;
 import android.content.pm.ProviderInfo;
 import android.content.pm.ResolveInfo;
 import android.content.res.Resources;
 import android.database.Cursor;
 import android.graphics.Bitmap;
 import android.graphics.BitmapFactory;
 import android.net.Uri;
 import android.os.Parcelable;
 import android.os.RemoteException;
 import android.util.Log;
 import android.os.Process;
 import android.os.SystemClock;
 
 import java.lang.ref.WeakReference;
 import java.net.URISyntaxException;
 import java.text.Collator;
 import java.util.ArrayList;
 import java.util.Arrays;
 import java.util.Comparator;
 import java.util.Collections;
 import java.util.HashMap;
 import java.util.List;
 
 import com.android.launcher.R;
 
 /**
  * Maintains in-memory state of the Launcher. It is expected that there should be only one
  * LauncherModel object held in a static. Also provide APIs for updating the database state
  * for the Launcher.
  */
 public class LauncherModel extends BroadcastReceiver {
     static final boolean DEBUG_LOADERS = false;
     static final String TAG = "Launcher.Model";
 
    private int mAllAppsLoadDelay; // milliseconds between batches
 
     private final LauncherApplication mApp;
     private final Object mLock = new Object();
     private DeferredHandler mHandler = new DeferredHandler();
     private Loader mLoader = new Loader();
 
     private boolean mBeforeFirstLoad = true;
     private WeakReference<Callbacks> mCallbacks;
 
     private AllAppsList mAllAppsList;
     private IconCache mIconCache;
 
     private Bitmap mDefaultIcon;
 
     public interface Callbacks {
         public int getCurrentWorkspaceScreen();
         public void startBinding();
         public void bindItems(ArrayList<ItemInfo> shortcuts, int start, int end);
         public void bindFolders(HashMap<Long,FolderInfo> folders);
         public void finishBindingItems();
         public void bindAppWidget(LauncherAppWidgetInfo info);
         public void bindAllApplications(ArrayList<ApplicationInfo> apps);
         public void bindAppsAdded(ArrayList<ApplicationInfo> apps);
         public void bindAppsUpdated(ArrayList<ApplicationInfo> apps);
         public void bindAppsRemoved(ArrayList<ApplicationInfo> apps);
         public int  getAppBatchSize();
     }
 
     LauncherModel(LauncherApplication app, IconCache iconCache) {
         mApp = app;
         mAllAppsList = new AllAppsList(iconCache);
         mIconCache = iconCache;
 
         mDefaultIcon = Utilities.createIconBitmap(
                 app.getPackageManager().getDefaultActivityIcon(), app);

        mAllAppsLoadDelay = app.getResources().getInteger(R.integer.config_allAppsBatchLoadDelay);
     }
 
     public Bitmap getFallbackIcon() {
         return Bitmap.createBitmap(mDefaultIcon);
     }
 
     /**
      * Adds an item to the DB if it was not created previously, or move it to a new
      * <container, screen, cellX, cellY>
      */
     static void addOrMoveItemInDatabase(Context context, ItemInfo item, long container,
             int screen, int cellX, int cellY) {
         if (item.container == ItemInfo.NO_ID) {
             // From all apps
             addItemToDatabase(context, item, container, screen, cellX, cellY, false);
         } else {
             // From somewhere else
             moveItemInDatabase(context, item, container, screen, cellX, cellY);
         }
     }
 
     /**
      * Move an item in the DB to a new <container, screen, cellX, cellY>
      */
     static void moveItemInDatabase(Context context, ItemInfo item, long container, int screen,
             int cellX, int cellY) {
         item.container = container;
         item.screen = screen;
         item.cellX = cellX;
         item.cellY = cellY;
 
         final ContentValues values = new ContentValues();
         final ContentResolver cr = context.getContentResolver();
 
         values.put(LauncherSettings.Favorites.CONTAINER, item.container);
         values.put(LauncherSettings.Favorites.CELLX, item.cellX);
         values.put(LauncherSettings.Favorites.CELLY, item.cellY);
         values.put(LauncherSettings.Favorites.SCREEN, item.screen);
 
         cr.update(LauncherSettings.Favorites.getContentUri(item.id, false), values, null, null);
     }
 
     /**
      * Returns true if the shortcuts already exists in the database.
      * we identify a shortcut by its title and intent.
      */
     static boolean shortcutExists(Context context, String title, Intent intent) {
         final ContentResolver cr = context.getContentResolver();
         Cursor c = cr.query(LauncherSettings.Favorites.CONTENT_URI,
             new String[] { "title", "intent" }, "title=? and intent=?",
             new String[] { title, intent.toUri(0) }, null);
         boolean result = false;
         try {
             result = c.moveToFirst();
         } finally {
             c.close();
         }
         return result;
     }
 
     /**
      * Find a folder in the db, creating the FolderInfo if necessary, and adding it to folderList.
      */
     FolderInfo getFolderById(Context context, HashMap<Long,FolderInfo> folderList, long id) {
         final ContentResolver cr = context.getContentResolver();
         Cursor c = cr.query(LauncherSettings.Favorites.CONTENT_URI, null,
                 "_id=? and (itemType=? or itemType=?)",
                 new String[] { String.valueOf(id),
                         String.valueOf(LauncherSettings.Favorites.ITEM_TYPE_USER_FOLDER),
                         String.valueOf(LauncherSettings.Favorites.ITEM_TYPE_LIVE_FOLDER) }, null);
 
         try {
             if (c.moveToFirst()) {
                 final int itemTypeIndex = c.getColumnIndexOrThrow(LauncherSettings.Favorites.ITEM_TYPE);
                 final int titleIndex = c.getColumnIndexOrThrow(LauncherSettings.Favorites.TITLE);
                 final int containerIndex = c.getColumnIndexOrThrow(LauncherSettings.Favorites.CONTAINER);
                 final int screenIndex = c.getColumnIndexOrThrow(LauncherSettings.Favorites.SCREEN);
                 final int cellXIndex = c.getColumnIndexOrThrow(LauncherSettings.Favorites.CELLX);
                 final int cellYIndex = c.getColumnIndexOrThrow(LauncherSettings.Favorites.CELLY);
 
                 FolderInfo folderInfo = null;
                 switch (c.getInt(itemTypeIndex)) {
                     case LauncherSettings.Favorites.ITEM_TYPE_USER_FOLDER:
                         folderInfo = findOrMakeUserFolder(folderList, id);
                         break;
                     case LauncherSettings.Favorites.ITEM_TYPE_LIVE_FOLDER:
                         folderInfo = findOrMakeLiveFolder(folderList, id);
                         break;
                 }
 
                 folderInfo.title = c.getString(titleIndex);
                 folderInfo.id = id;
                 folderInfo.container = c.getInt(containerIndex);
                 folderInfo.screen = c.getInt(screenIndex);
                 folderInfo.cellX = c.getInt(cellXIndex);
                 folderInfo.cellY = c.getInt(cellYIndex);
 
                 return folderInfo;
             }
         } finally {
             c.close();
         }
 
         return null;
     }
 
     /**
      * Add an item to the database in a specified container. Sets the container, screen, cellX and
      * cellY fields of the item. Also assigns an ID to the item.
      */
     static void addItemToDatabase(Context context, ItemInfo item, long container,
             int screen, int cellX, int cellY, boolean notify) {
         item.container = container;
         item.screen = screen;
         item.cellX = cellX;
         item.cellY = cellY;
 
         final ContentValues values = new ContentValues();
         final ContentResolver cr = context.getContentResolver();
 
         item.onAddToDatabase(values);
 
         Uri result = cr.insert(notify ? LauncherSettings.Favorites.CONTENT_URI :
                 LauncherSettings.Favorites.CONTENT_URI_NO_NOTIFICATION, values);
 
         if (result != null) {
             item.id = Integer.parseInt(result.getPathSegments().get(1));
         }
     }
 
     /**
      * Update an item to the database in a specified container.
      */
     static void updateItemInDatabase(Context context, ItemInfo item) {
         final ContentValues values = new ContentValues();
         final ContentResolver cr = context.getContentResolver();
 
         item.onAddToDatabase(values);
 
         cr.update(LauncherSettings.Favorites.getContentUri(item.id, false), values, null, null);
     }
 
     /**
      * Removes the specified item from the database
      * @param context
      * @param item
      */
     static void deleteItemFromDatabase(Context context, ItemInfo item) {
         final ContentResolver cr = context.getContentResolver();
 
         cr.delete(LauncherSettings.Favorites.getContentUri(item.id, false), null, null);
     }
 
     /**
      * Remove the contents of the specified folder from the database
      */
     static void deleteUserFolderContentsFromDatabase(Context context, UserFolderInfo info) {
         final ContentResolver cr = context.getContentResolver();
 
         cr.delete(LauncherSettings.Favorites.getContentUri(info.id, false), null, null);
         cr.delete(LauncherSettings.Favorites.CONTENT_URI,
                 LauncherSettings.Favorites.CONTAINER + "=" + info.id, null);
     }
 
     /**
      * Set this as the current Launcher activity object for the loader.
      */
     public void initialize(Callbacks callbacks) {
         synchronized (mLock) {
             mCallbacks = new WeakReference<Callbacks>(callbacks);
         }
     }
 
     public void startLoader(Context context, boolean isLaunching) {
         mLoader.startLoader(context, isLaunching);
     }
 
     public void stopLoader() {
         mLoader.stopLoader();
     }
 
     /**
      * We pick up most of the changes to all apps.
      */
     public void setAllAppsDirty() {
         mLoader.setAllAppsDirty();
     }
 
     public void setWorkspaceDirty() {
         mLoader.setWorkspaceDirty();
     }
 
     /**
      * Call from the handler for ACTION_PACKAGE_ADDED, ACTION_PACKAGE_REMOVED and
      * ACTION_PACKAGE_CHANGED.
      */
     public void onReceive(Context context, Intent intent) {
         // Use the app as the context.
         context = mApp;
 
         ArrayList<ApplicationInfo> added = null;
         ArrayList<ApplicationInfo> removed = null;
         ArrayList<ApplicationInfo> modified = null;
 
         synchronized (mLock) {
             if (mBeforeFirstLoad) {
                 // If we haven't even loaded yet, don't bother, since we'll just pick
                 // up the changes.
                 return;
             }
 
             final String action = intent.getAction();
 
             if (Intent.ACTION_PACKAGE_CHANGED.equals(action)
                     || Intent.ACTION_PACKAGE_REMOVED.equals(action)
                     || Intent.ACTION_PACKAGE_ADDED.equals(action)) {
                 final String packageName = intent.getData().getSchemeSpecificPart();
                 final boolean replacing = intent.getBooleanExtra(Intent.EXTRA_REPLACING, false);
 
                 if (packageName == null || packageName.length() == 0) {
                     // they sent us a bad intent
                     return;
                 }
 
                 if (Intent.ACTION_PACKAGE_CHANGED.equals(action)) {
                     mAllAppsList.updatePackage(context, packageName);
                 } else if (Intent.ACTION_PACKAGE_REMOVED.equals(action)) {
                     if (!replacing) {
                         mAllAppsList.removePackage(packageName);
                     }
                     // else, we are replacing the package, so a PACKAGE_ADDED will be sent
                     // later, we will update the package at this time
                 } else if (Intent.ACTION_PACKAGE_ADDED.equals(action)) {
                     if (!replacing) {
                         mAllAppsList.addPackage(context, packageName);
                     } else {
                         mAllAppsList.updatePackage(context, packageName);
                     }
                 }
 
                 if (mAllAppsList.added.size() > 0) {
                     added = mAllAppsList.added;
                     mAllAppsList.added = new ArrayList<ApplicationInfo>();
                 }
                 if (mAllAppsList.removed.size() > 0) {
                     removed = mAllAppsList.removed;
                     mAllAppsList.removed = new ArrayList<ApplicationInfo>();
                     for (ApplicationInfo info: removed) {
                         mIconCache.remove(info.intent.getComponent());
                     }
                 }
                 if (mAllAppsList.modified.size() > 0) {
                     modified = mAllAppsList.modified;
                     mAllAppsList.modified = new ArrayList<ApplicationInfo>();
                 }
 
                 final Callbacks callbacks = mCallbacks != null ? mCallbacks.get() : null;
                 if (callbacks == null) {
                     Log.w(TAG, "Nobody to tell about the new app.  Launcher is probably loading.");
                     return;
                 }
 
                 if (added != null) {
                     final ArrayList<ApplicationInfo> addedFinal = added;
                     mHandler.post(new Runnable() {
                         public void run() {
                             callbacks.bindAppsAdded(addedFinal);
                         }
                     });
                 }
                 if (modified != null) {
                     final ArrayList<ApplicationInfo> modifiedFinal = modified;
                     mHandler.post(new Runnable() {
                         public void run() {
                             callbacks.bindAppsUpdated(modifiedFinal);
                         }
                     });
                 }
                 if (removed != null) {
                     final ArrayList<ApplicationInfo> removedFinal = removed;
                     mHandler.post(new Runnable() {
                         public void run() {
                             callbacks.bindAppsRemoved(removedFinal);
                         }
                     });
                 }
             } else {
                 if (Intent.ACTION_EXTERNAL_APPLICATIONS_AVAILABLE.equals(action)) {
                      String packages[] = intent.getStringArrayExtra(
                              Intent.EXTRA_CHANGED_PACKAGE_LIST);
                      if (packages == null || packages.length == 0) {
                          return;
                      }
                      setAllAppsDirty();
                      setWorkspaceDirty();
                      startLoader(context, false);
                 } else if (Intent.ACTION_EXTERNAL_APPLICATIONS_UNAVAILABLE.equals(action)) {
                      String packages[] = intent.getStringArrayExtra(
                              Intent.EXTRA_CHANGED_PACKAGE_LIST);
                      if (packages == null || packages.length == 0) {
                          return;
                      }
                      setAllAppsDirty();
                      setWorkspaceDirty();
                      startLoader(context, false);
                 }
             }
         }
     }
 
     public class Loader {
         private static final int ITEMS_CHUNK = 6;
 
         private LoaderThread mLoaderThread;
 
         private int mLastWorkspaceSeq = 0;
         private int mWorkspaceSeq = 1;
 
         private int mLastAllAppsSeq = 0;
         private int mAllAppsSeq = 1;
 
         final ArrayList<ItemInfo> mItems = new ArrayList<ItemInfo>();
         final ArrayList<LauncherAppWidgetInfo> mAppWidgets = new ArrayList<LauncherAppWidgetInfo>();
         final HashMap<Long, FolderInfo> mFolders = new HashMap<Long, FolderInfo>();
                 
         /**
          * Call this from the ui thread so the handler is initialized on the correct thread.
          */
         public Loader() {
         }
 
         public void startLoader(Context context, boolean isLaunching) {
             synchronized (mLock) {
                 if (DEBUG_LOADERS) {
                     Log.d(TAG, "startLoader isLaunching=" + isLaunching);
                 }
                 // Don't bother to start the thread if we know it's not going to do anything
                 if (mCallbacks != null && mCallbacks.get() != null) {
                     LoaderThread oldThread = mLoaderThread;
                     if (oldThread != null) {
                         if (oldThread.isLaunching()) {
                             // don't downgrade isLaunching if we're already running
                             isLaunching = true;
                         }
                         oldThread.stopLocked();
                     }
                     mLoaderThread = new LoaderThread(context, oldThread, isLaunching);
                     mLoaderThread.start();
                 }
             }
         }
 
         public void stopLoader() {
             synchronized (mLock) {
                 if (mLoaderThread != null) {
                     mLoaderThread.stopLocked();
                 }
             }
         }
 
         public void setWorkspaceDirty() {
             synchronized (mLock) {
                 mWorkspaceSeq++;
             }
         }
 
         public void setAllAppsDirty() {
             synchronized (mLock) {
                 mAllAppsSeq++;
             }
         }
 
         /**
          * Runnable for the thread that loads the contents of the launcher:
          *   - workspace icons
          *   - widgets
          *   - all apps icons
          */
         private class LoaderThread extends Thread {
             private Context mContext;
             private Thread mWaitThread;
             private boolean mIsLaunching;
             private boolean mStopped;
             private boolean mWorkspaceDoneBinding;
 
             LoaderThread(Context context, Thread waitThread, boolean isLaunching) {
                 mContext = context;
                 mWaitThread = waitThread;
                 mIsLaunching = isLaunching;
             }
 
             boolean isLaunching() {
                 return mIsLaunching;
             }
 
             /**
              * If another LoaderThread was supplied, we need to wait for that to finish before
              * we start our processing.  This keeps the ordering of the setting and clearing
              * of the dirty flags correct by making sure we don't start processing stuff until
              * they've had a chance to re-set them.  We do this waiting the worker thread, not
              * the ui thread to avoid ANRs.
              */
             private void waitForOtherThread() {
                 if (mWaitThread != null) {
                     boolean done = false;
                     while (!done) {
                         try {
                             mWaitThread.join();
                             done = true;
                         } catch (InterruptedException ex) {
                             // Ignore
                         }
                     }
                     mWaitThread = null;
                 }
             }
 
             public void run() {
                 waitForOtherThread();
 
                 // Elevate priority when Home launches for the first time to avoid
                 // starving at boot time. Staring at a blank home is not cool.
                 synchronized (mLock) {
                     android.os.Process.setThreadPriority(mIsLaunching
                             ? Process.THREAD_PRIORITY_DEFAULT : Process.THREAD_PRIORITY_BACKGROUND);
                 }
 
                 // Load the workspace only if it's dirty.
                 int workspaceSeq;
                 boolean workspaceDirty;
                 synchronized (mLock) {
                     workspaceSeq = mWorkspaceSeq;
                     workspaceDirty = mWorkspaceSeq != mLastWorkspaceSeq;
                 }
                 if (workspaceDirty) {
                     loadWorkspace();
                 }
                 synchronized (mLock) {
                     // If we're not stopped, and nobody has incremented mWorkspaceSeq.
                     if (mStopped) {
                         return;
                     }
                     if (workspaceSeq == mWorkspaceSeq) {
                         mLastWorkspaceSeq = mWorkspaceSeq;
                     }
                 }
 
                 // Bind the workspace
                 bindWorkspace();
                 
                 // Wait until the either we're stopped or the other threads are done.
                 // This way we don't start loading all apps until the workspace has settled
                 // down.
                 synchronized (LoaderThread.this) {
                     mHandler.postIdle(new Runnable() {
                             public void run() {
                                 synchronized (LoaderThread.this) {
                                     mWorkspaceDoneBinding = true;
                                     if (DEBUG_LOADERS) {
                                         Log.d(TAG, "done with workspace");
                                         }
                                     LoaderThread.this.notify();
                                 }
                             }
                         });
                     if (DEBUG_LOADERS) {
                         Log.d(TAG, "waiting to be done with workspace");
                     }
                     while (!mStopped && !mWorkspaceDoneBinding) {
                         try {
                             this.wait();
                         } catch (InterruptedException ex) {
                             // Ignore
                         }
                     }
                     if (DEBUG_LOADERS) {
                         Log.d(TAG, "done waiting to be done with workspace");
                     }
                 }
 
                 // Whew! Hard work done.
                 synchronized (mLock) {
                     if (mIsLaunching) {
                        android.os.Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
                     }
                 }
 
                 // Load all apps if they're dirty
                 int allAppsSeq;
                 boolean allAppsDirty;
                 synchronized (mLock) {
                     allAppsSeq = mAllAppsSeq;
                     allAppsDirty = mAllAppsSeq != mLastAllAppsSeq;
                     if (DEBUG_LOADERS) {
                         Log.d(TAG, "mAllAppsSeq=" + mAllAppsSeq
                                 + " mLastAllAppsSeq=" + mLastAllAppsSeq + " allAppsDirty");
                     }
                 }
                 if (allAppsDirty) {
                     loadAndBindAllApps();
                 }
                 synchronized (mLock) {
                     // If we're not stopped, and nobody has incremented mAllAppsSeq.
                     if (mStopped) {
                         return;
                     }
                     if (allAppsSeq == mAllAppsSeq) {
                         mLastAllAppsSeq = mAllAppsSeq;
                     }
                 }
 
                 // Clear out this reference, otherwise we end up holding it until all of the
                 // callback runnables are done.
                 mContext = null;
 
                 synchronized (mLock) {
                     // Setting the reference is atomic, but we can't do it inside the other critical
                     // sections.
                     mLoaderThread = null;
                 }
             }
 
             public void stopLocked() {
                 synchronized (LoaderThread.this) {
                     mStopped = true;
                     this.notify();
                 }
             }
 
             /**
              * Gets the callbacks object.  If we've been stopped, or if the launcher object
              * has somehow been garbage collected, return null instead.  Pass in the Callbacks
              * object that was around when the deferred message was scheduled, and if there's
              * a new Callbacks object around then also return null.  This will save us from
              * calling onto it with data that will be ignored.
              */
             Callbacks tryGetCallbacks(Callbacks oldCallbacks) {
                 synchronized (mLock) {
                     if (mStopped) {
                         return null;
                     }
 
                     if (mCallbacks == null) {
                         return null;
                     }
 
                     final Callbacks callbacks = mCallbacks.get();
                     if (callbacks != oldCallbacks) {
                         return null;
                     }
                     if (callbacks == null) {
                         Log.w(TAG, "no mCallbacks");
                         return null;
                     }
 
                     return callbacks;
                 }
             }
 
             private void loadWorkspace() {
                 long t = SystemClock.uptimeMillis();
 
                 final Context context = mContext;
                 final ContentResolver contentResolver = context.getContentResolver();
                 final PackageManager manager = context.getPackageManager();
                 final AppWidgetManager widgets = AppWidgetManager.getInstance(context);
                 final boolean isSafeMode = manager.isSafeMode();
 
                 mItems.clear();
                 mAppWidgets.clear();
                 mFolders.clear();
 
                 final ArrayList<Long> itemsToRemove = new ArrayList<Long>();
 
                 final Cursor c = contentResolver.query(
                         LauncherSettings.Favorites.CONTENT_URI, null, null, null, null);
 
                 try {
                     final int idIndex = c.getColumnIndexOrThrow(LauncherSettings.Favorites._ID);
                     final int intentIndex = c.getColumnIndexOrThrow
                             (LauncherSettings.Favorites.INTENT);
                     final int titleIndex = c.getColumnIndexOrThrow
                             (LauncherSettings.Favorites.TITLE);
                     final int iconTypeIndex = c.getColumnIndexOrThrow(
                             LauncherSettings.Favorites.ICON_TYPE);
                     final int iconIndex = c.getColumnIndexOrThrow(LauncherSettings.Favorites.ICON);
                     final int iconPackageIndex = c.getColumnIndexOrThrow(
                             LauncherSettings.Favorites.ICON_PACKAGE);
                     final int iconResourceIndex = c.getColumnIndexOrThrow(
                             LauncherSettings.Favorites.ICON_RESOURCE);
                     final int containerIndex = c.getColumnIndexOrThrow(
                             LauncherSettings.Favorites.CONTAINER);
                     final int itemTypeIndex = c.getColumnIndexOrThrow(
                             LauncherSettings.Favorites.ITEM_TYPE);
                     final int appWidgetIdIndex = c.getColumnIndexOrThrow(
                             LauncherSettings.Favorites.APPWIDGET_ID);
                     final int screenIndex = c.getColumnIndexOrThrow(
                             LauncherSettings.Favorites.SCREEN);
                     final int cellXIndex = c.getColumnIndexOrThrow
                             (LauncherSettings.Favorites.CELLX);
                     final int cellYIndex = c.getColumnIndexOrThrow
                             (LauncherSettings.Favorites.CELLY);
                     final int spanXIndex = c.getColumnIndexOrThrow
                             (LauncherSettings.Favorites.SPANX);
                     final int spanYIndex = c.getColumnIndexOrThrow(
                             LauncherSettings.Favorites.SPANY);
                     final int uriIndex = c.getColumnIndexOrThrow(LauncherSettings.Favorites.URI);
                     final int displayModeIndex = c.getColumnIndexOrThrow(
                             LauncherSettings.Favorites.DISPLAY_MODE);
 
                     ShortcutInfo info;
                     String intentDescription;
                     LauncherAppWidgetInfo appWidgetInfo;
                     int container;
                     long id;
                     Intent intent;
 
                     while (!mStopped && c.moveToNext()) {
                         try {
                             int itemType = c.getInt(itemTypeIndex);
 
                             switch (itemType) {
                             case LauncherSettings.Favorites.ITEM_TYPE_APPLICATION:
                             case LauncherSettings.Favorites.ITEM_TYPE_SHORTCUT:
                                 intentDescription = c.getString(intentIndex);
                                 try {
                                     intent = Intent.parseUri(intentDescription, 0);
                                 } catch (URISyntaxException e) {
                                     continue;
                                 }
 
                                 if (itemType == LauncherSettings.Favorites.ITEM_TYPE_APPLICATION) {
                                     info = getShortcutInfo(manager, intent, context, c, iconIndex,
                                             titleIndex);
                                 } else {
                                     info = getShortcutInfo(c, context, iconTypeIndex,
                                             iconPackageIndex, iconResourceIndex, iconIndex,
                                             titleIndex);
                                 }
 
                                 if (info != null) {
                                     updateSavedIcon(context, info, c, iconIndex);
 
                                     info.intent = intent;
                                     info.id = c.getLong(idIndex);
                                     container = c.getInt(containerIndex);
                                     info.container = container;
                                     info.screen = c.getInt(screenIndex);
                                     info.cellX = c.getInt(cellXIndex);
                                     info.cellY = c.getInt(cellYIndex);
 
                                     switch (container) {
                                     case LauncherSettings.Favorites.CONTAINER_DESKTOP:
                                         mItems.add(info);
                                         break;
                                     default:
                                         // Item is in a user folder
                                         UserFolderInfo folderInfo =
                                                 findOrMakeUserFolder(mFolders, container);
                                         folderInfo.add(info);
                                         break;
                                     }
                                 } else {
                                     // Failed to load the shortcut, probably because the
                                     // activity manager couldn't resolve it (maybe the app
                                     // was uninstalled), or the db row was somehow screwed up.
                                     // Delete it.
                                     id = c.getLong(idIndex);
                                     Log.e(TAG, "Error loading shortcut " + id + ", removing it");
                                     contentResolver.delete(LauncherSettings.Favorites.getContentUri(
                                                 id, false), null, null);
                                 }
                                 break;
 
                             case LauncherSettings.Favorites.ITEM_TYPE_USER_FOLDER:
                                 id = c.getLong(idIndex);
                                 UserFolderInfo folderInfo = findOrMakeUserFolder(mFolders, id);
 
                                 folderInfo.title = c.getString(titleIndex);
 
                                 folderInfo.id = id;
                                 container = c.getInt(containerIndex);
                                 folderInfo.container = container;
                                 folderInfo.screen = c.getInt(screenIndex);
                                 folderInfo.cellX = c.getInt(cellXIndex);
                                 folderInfo.cellY = c.getInt(cellYIndex);
 
                                 switch (container) {
                                     case LauncherSettings.Favorites.CONTAINER_DESKTOP:
                                         mItems.add(folderInfo);
                                         break;
                                 }
 
                                 mFolders.put(folderInfo.id, folderInfo);
                                 break;
 
                             case LauncherSettings.Favorites.ITEM_TYPE_LIVE_FOLDER:
                                 id = c.getLong(idIndex);
                                 Uri uri = Uri.parse(c.getString(uriIndex));
 
                                 // Make sure the live folder exists
                                 final ProviderInfo providerInfo =
                                         context.getPackageManager().resolveContentProvider(
                                                 uri.getAuthority(), 0);
 
                                 if (providerInfo == null && !isSafeMode) {
                                     itemsToRemove.add(id);
                                 } else {
                                     LiveFolderInfo liveFolderInfo = findOrMakeLiveFolder(mFolders, id);
     
                                     intentDescription = c.getString(intentIndex);
                                     intent = null;
                                     if (intentDescription != null) {
                                         try {
                                             intent = Intent.parseUri(intentDescription, 0);
                                         } catch (URISyntaxException e) {
                                             // Ignore, a live folder might not have a base intent
                                         }
                                     }
     
                                     liveFolderInfo.title = c.getString(titleIndex);
                                     liveFolderInfo.id = id;
                                     liveFolderInfo.uri = uri;
                                     container = c.getInt(containerIndex);
                                     liveFolderInfo.container = container;
                                     liveFolderInfo.screen = c.getInt(screenIndex);
                                     liveFolderInfo.cellX = c.getInt(cellXIndex);
                                     liveFolderInfo.cellY = c.getInt(cellYIndex);
                                     liveFolderInfo.baseIntent = intent;
                                     liveFolderInfo.displayMode = c.getInt(displayModeIndex);
     
                                     loadLiveFolderIcon(context, c, iconTypeIndex, iconPackageIndex,
                                             iconResourceIndex, liveFolderInfo);
     
                                     switch (container) {
                                         case LauncherSettings.Favorites.CONTAINER_DESKTOP:
                                             mItems.add(liveFolderInfo);
                                             break;
                                     }
                                     mFolders.put(liveFolderInfo.id, liveFolderInfo);
                                 }
                                 break;
 
                             case LauncherSettings.Favorites.ITEM_TYPE_APPWIDGET:
                                 // Read all Launcher-specific widget details
                                 int appWidgetId = c.getInt(appWidgetIdIndex);
                                 id = c.getLong(idIndex);
 
                                 final AppWidgetProviderInfo provider =
                                         widgets.getAppWidgetInfo(appWidgetId);
                                 
                                 if (!isSafeMode && (provider == null || provider.provider == null ||
                                         provider.provider.getPackageName() == null)) {
                                     Log.e(TAG, "Deleting widget that isn't installed anymore: id="
                                             + id + " appWidgetId=" + appWidgetId);
                                     itemsToRemove.add(id);
                                 } else {
                                     appWidgetInfo = new LauncherAppWidgetInfo(appWidgetId);
                                     appWidgetInfo.id = id;
                                     appWidgetInfo.screen = c.getInt(screenIndex);
                                     appWidgetInfo.cellX = c.getInt(cellXIndex);
                                     appWidgetInfo.cellY = c.getInt(cellYIndex);
                                     appWidgetInfo.spanX = c.getInt(spanXIndex);
                                     appWidgetInfo.spanY = c.getInt(spanYIndex);
     
                                     container = c.getInt(containerIndex);
                                     if (container != LauncherSettings.Favorites.CONTAINER_DESKTOP) {
                                         Log.e(TAG, "Widget found where container "
                                                 + "!= CONTAINER_DESKTOP -- ignoring!");
                                         continue;
                                     }
                                     appWidgetInfo.container = c.getInt(containerIndex);
     
                                     mAppWidgets.add(appWidgetInfo);
                                 }
                                 break;
                             }
                         } catch (Exception e) {
                             Log.w(TAG, "Desktop items loading interrupted:", e);
                         }
                     }
                 } finally {
                     c.close();
                 }
 
                 if (itemsToRemove.size() > 0) {
                     ContentProviderClient client = contentResolver.acquireContentProviderClient(
                                     LauncherSettings.Favorites.CONTENT_URI);
                     // Remove dead items
                     for (long id : itemsToRemove) {
                         if (DEBUG_LOADERS) {
                             Log.d(TAG, "Removed id = " + id);
                         }
                         // Don't notify content observers
                         try {
                             client.delete(LauncherSettings.Favorites.getContentUri(id, false),
                                     null, null);
                         } catch (RemoteException e) {
                             Log.w(TAG, "Could not remove id = " + id);
                         }
                     }
                 }
 
                 if (DEBUG_LOADERS) {
                     Log.d(TAG, "loaded workspace in " + (SystemClock.uptimeMillis()-t) + "ms");
                 }
             }
 
             /**
              * Read everything out of our database.
              */
             private void bindWorkspace() {
                 final long t = SystemClock.uptimeMillis();
 
                 // Don't use these two variables in any of the callback runnables.
                 // Otherwise we hold a reference to them.
                 final Callbacks oldCallbacks = mCallbacks.get();
                 if (oldCallbacks == null) {
                     // This launcher has exited and nobody bothered to tell us.  Just bail.
                     Log.w(TAG, "LoaderThread running with no launcher");
                     return;
                 }
 
                 int N;
                 // Tell the workspace that we're about to start firing items at it
                 mHandler.post(new Runnable() {
                     public void run() {
                         Callbacks callbacks = tryGetCallbacks(oldCallbacks);
                         if (callbacks != null) {
                             callbacks.startBinding();
                         }
                     }
                 });
                 // Add the items to the workspace.
                 N = mItems.size();
                 for (int i=0; i<N; i+=ITEMS_CHUNK) {
                     final int start = i;
                     final int chunkSize = (i+ITEMS_CHUNK <= N) ? ITEMS_CHUNK : (N-i);
                     mHandler.post(new Runnable() {
                         public void run() {
                             Callbacks callbacks = tryGetCallbacks(oldCallbacks);
                             if (callbacks != null) {
                                 callbacks.bindItems(mItems, start, start+chunkSize);
                             }
                         }
                     });
                 }
                 mHandler.post(new Runnable() {
                     public void run() {
                         Callbacks callbacks = tryGetCallbacks(oldCallbacks);
                         if (callbacks != null) {
                             callbacks.bindFolders(mFolders);
                         }
                     }
                 });
                 // Wait until the queue goes empty.
                 mHandler.postIdle(new Runnable() {
                     public void run() {
                         if (DEBUG_LOADERS) {
                             Log.d(TAG, "Going to start binding widgets soon.");
                         }
                     }
                 });
                 // Bind the widgets, one at a time.
                 // WARNING: this is calling into the workspace from the background thread,
                 // but since getCurrentScreen() just returns the int, we should be okay.  This
                 // is just a hint for the order, and if it's wrong, we'll be okay.
                 // TODO: instead, we should have that push the current screen into here.
                 final int currentScreen = oldCallbacks.getCurrentWorkspaceScreen();
                 N = mAppWidgets.size();
                 // once for the current screen
                 for (int i=0; i<N; i++) {
                     final LauncherAppWidgetInfo widget = mAppWidgets.get(i);
                     if (widget.screen == currentScreen) {
                         mHandler.post(new Runnable() {
                             public void run() {
                                 Callbacks callbacks = tryGetCallbacks(oldCallbacks);
                                 if (callbacks != null) {
                                     callbacks.bindAppWidget(widget);
                                 }
                             }
                         });
                     }
                 }
                 // once for the other screens
                 for (int i=0; i<N; i++) {
                     final LauncherAppWidgetInfo widget = mAppWidgets.get(i);
                     if (widget.screen != currentScreen) {
                         mHandler.post(new Runnable() {
                             public void run() {
                                 Callbacks callbacks = tryGetCallbacks(oldCallbacks);
                                 if (callbacks != null) {
                                     callbacks.bindAppWidget(widget);
                                 }
                             }
                         });
                     }
                 }
                 // Tell the workspace that we're done.
                 mHandler.post(new Runnable() {
                     public void run() {
                         Callbacks callbacks = tryGetCallbacks(oldCallbacks);
                         if (callbacks != null) {
                             callbacks.finishBindingItems();
                         }
                     }
                 });
                 // If we're profiling, this is the last thing in the queue.
                 mHandler.post(new Runnable() {
                     public void run() {
                         if (DEBUG_LOADERS) {
                             Log.d(TAG, "bound workspace in "
                                 + (SystemClock.uptimeMillis()-t) + "ms");
                         }
                         if (Launcher.PROFILE_ROTATE) {
                             android.os.Debug.stopMethodTracing();
                         }
                     }
                 });
             }
 
             private void loadAndBindAllApps() {
                 final long t = DEBUG_LOADERS ? SystemClock.uptimeMillis() : 0;
 
                 final Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
                 mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);
 
                 final Callbacks callbacks = mCallbacks.get();
                 if (callbacks == null) {
                     return;
                 }
 
                 final PackageManager packageManager = mContext.getPackageManager();
                 final List<ResolveInfo> apps = packageManager.queryIntentActivities(mainIntent, 0);
 
                 int N;
                 int batchSize = callbacks.getAppBatchSize();
 
                 synchronized (mLock) {
                     mBeforeFirstLoad = false;
                     mAllAppsList.clear();
                     if (apps == null) return;
                     N = apps.size();
                     if (batchSize <= 0)
                         batchSize = N;
                 }
 
                 int i=0;
                 while (i < N && !mStopped) {
                     synchronized (mLock) {
                         final long t2 = DEBUG_LOADERS ? SystemClock.uptimeMillis() : 0;
 
                         for (int j=0; i<N && j<batchSize; j++) {
                             // This builds the icon bitmaps.
                             mAllAppsList.add(new ApplicationInfo(apps.get(i), mIconCache));
                             i++;
                         }
                         if (DEBUG_LOADERS) {
                             Log.d(TAG, "batch of " + batchSize + " icons processed in "
                                     + (SystemClock.uptimeMillis()-t2) + "ms");
                         }
                     }
 
                     mHandler.post(bindAllAppsTask);
 
                    if (mAllAppsLoadDelay > 0 && i < N) {
                         try {
                            Thread.sleep(mAllAppsLoadDelay);
                         } catch (InterruptedException exc) { }
                     }
                 }
 
                 if (DEBUG_LOADERS) {
                     Log.d(TAG, "cached all " + N + " apps in "
                            + (SystemClock.uptimeMillis()-t) + "ms"
                            + (mAllAppsLoadDelay > 0 ? " (including delay)" : ""));
                 }
             }
 
             final Runnable bindAllAppsTask = new Runnable() {
                 public void run() {
                     final long t = SystemClock.uptimeMillis();
                     int count = 0;
                     Callbacks callbacks = null;
                     ArrayList<ApplicationInfo> results = null;
                     synchronized (mLock) {
                         mHandler.cancelRunnable(this);
 
                         results = (ArrayList<ApplicationInfo>) mAllAppsList.data.clone();
                         // We're adding this now, so clear out this so we don't re-send them.
                         mAllAppsList.added = new ArrayList<ApplicationInfo>();
                         count = results.size();
 
                         callbacks = tryGetCallbacks(mCallbacks.get());
                     }
 
                     if (callbacks != null && count > 0) {
                         callbacks.bindAllApplications(results);
                     }
 
                     if (DEBUG_LOADERS) {
                         Log.d(TAG, "bound " + count + " apps in "
                             + (SystemClock.uptimeMillis() - t) + "ms");
                     }
                 }
             };
 
             public void dumpState() {
                 Log.d(TAG, "mLoader.mLoaderThread.mContext=" + mContext);
                 Log.d(TAG, "mLoader.mLoaderThread.mWaitThread=" + mWaitThread);
                 Log.d(TAG, "mLoader.mLoaderThread.mIsLaunching=" + mIsLaunching);
                 Log.d(TAG, "mLoader.mLoaderThread.mStopped=" + mStopped);
                 Log.d(TAG, "mLoader.mLoaderThread.mWorkspaceDoneBinding=" + mWorkspaceDoneBinding);
             }
         }
 
         public void dumpState() {
             Log.d(TAG, "mLoader.mLastWorkspaceSeq=" + mLoader.mLastWorkspaceSeq);
             Log.d(TAG, "mLoader.mWorkspaceSeq=" + mLoader.mWorkspaceSeq);
             Log.d(TAG, "mLoader.mLastAllAppsSeq=" + mLoader.mLastAllAppsSeq);
             Log.d(TAG, "mLoader.mAllAppsSeq=" + mLoader.mAllAppsSeq);
             Log.d(TAG, "mLoader.mItems size=" + mLoader.mItems.size());
             if (mLoaderThread != null) {
                 mLoaderThread.dumpState();
             } else {
                 Log.d(TAG, "mLoader.mLoaderThread=null");
             }
         }
     }
 
     /**
      * This is called from the code that adds shortcuts from the intent receiver.  This
      * doesn't have a Cursor, but
      */
     public ShortcutInfo getShortcutInfo(PackageManager manager, Intent intent, Context context) {
         return getShortcutInfo(manager, intent, context, null, -1, -1);
     }
 
     /**
      * Make an ShortcutInfo object for a shortcut that is an application.
      *
      * If c is not null, then it will be used to fill in missing data like the title and icon.
      */
     public ShortcutInfo getShortcutInfo(PackageManager manager, Intent intent, Context context,
             Cursor c, int iconIndex, int titleIndex) {
         Bitmap icon = null;
         final ShortcutInfo info = new ShortcutInfo();
 
         ComponentName componentName = intent.getComponent();
         if (componentName == null) {
             return null;
         }
 
         // TODO: See if the PackageManager knows about this case.  If it doesn't
         // then return null & delete this.
 
         // the resource -- This may implicitly give us back the fallback icon,
         // but don't worry about that.  All we're doing with usingFallbackIcon is
         // to avoid saving lots of copies of that in the database, and most apps
         // have icons anyway.
         final ResolveInfo resolveInfo = manager.resolveActivity(intent, 0);
         if (resolveInfo != null) {
             icon = mIconCache.getIcon(componentName, resolveInfo);
         }
         // the db
         if (icon == null) {
             if (c != null) {
                 icon = getIconFromCursor(c, iconIndex);
             }
         }
         // the fallback icon
         if (icon == null) {
             icon = getFallbackIcon();
             info.usingFallbackIcon = true;
         }
         info.setIcon(icon);
 
         // from the resource
         if (resolveInfo != null) {
             info.title = resolveInfo.activityInfo.loadLabel(manager);
         }
         // from the db
         if (info.title == null) {
             if (c != null) {
                 info.title =  c.getString(titleIndex);
             }
         }
         // fall back to the class name of the activity
         if (info.title == null) {
             info.title = componentName.getClassName();
         }
         info.itemType = LauncherSettings.Favorites.ITEM_TYPE_APPLICATION;
         return info;
     }
 
     /**
      * Make an ShortcutInfo object for a shortcut that isn't an application.
      */
     private ShortcutInfo getShortcutInfo(Cursor c, Context context,
             int iconTypeIndex, int iconPackageIndex, int iconResourceIndex, int iconIndex,
             int titleIndex) {
 
         Bitmap icon = null;
         final ShortcutInfo info = new ShortcutInfo();
         info.itemType = LauncherSettings.Favorites.ITEM_TYPE_SHORTCUT;
 
         // TODO: If there's an explicit component and we can't install that, delete it.
 
         info.title = c.getString(titleIndex);
 
         int iconType = c.getInt(iconTypeIndex);
         switch (iconType) {
         case LauncherSettings.Favorites.ICON_TYPE_RESOURCE:
             String packageName = c.getString(iconPackageIndex);
             String resourceName = c.getString(iconResourceIndex);
             PackageManager packageManager = context.getPackageManager();
             info.customIcon = false;
             // the resource
             try {
                 Resources resources = packageManager.getResourcesForApplication(packageName);
                 if (resources != null) {
                     final int id = resources.getIdentifier(resourceName, null, null);
                     icon = Utilities.createIconBitmap(resources.getDrawable(id), context);
                 }
             } catch (Exception e) {
                 // drop this.  we have other places to look for icons
             }
             // the db
             if (icon == null) {
                 icon = getIconFromCursor(c, iconIndex);
             }
             // the fallback icon
             if (icon == null) {
                 icon = getFallbackIcon();
                 info.usingFallbackIcon = true;
             }
             break;
         case LauncherSettings.Favorites.ICON_TYPE_BITMAP:
             icon = getIconFromCursor(c, iconIndex);
             if (icon == null) {
                 icon = getFallbackIcon();
                 info.customIcon = false;
                 info.usingFallbackIcon = true;
             } else {
                 info.customIcon = true;
             }
             break;
         default:
             icon = getFallbackIcon();
             info.usingFallbackIcon = true;
             info.customIcon = false;
             break;
         }
         info.setIcon(icon);
         return info;
     }
 
     Bitmap getIconFromCursor(Cursor c, int iconIndex) {
         if (false) {
             Log.d(TAG, "getIconFromCursor app="
                     + c.getString(c.getColumnIndexOrThrow(LauncherSettings.Favorites.TITLE)));
         }
         byte[] data = c.getBlob(iconIndex);
         try {
             return BitmapFactory.decodeByteArray(data, 0, data.length);
         } catch (Exception e) {
             return null;
         }
     }
 
     ShortcutInfo addShortcut(Context context, Intent data,
             CellLayout.CellInfo cellInfo, boolean notify) {
 
         final ShortcutInfo info = infoFromShortcutIntent(context, data);
         addItemToDatabase(context, info, LauncherSettings.Favorites.CONTAINER_DESKTOP,
                 cellInfo.screen, cellInfo.cellX, cellInfo.cellY, notify);
 
         return info;
     }
 
     private ShortcutInfo infoFromShortcutIntent(Context context, Intent data) {
         Intent intent = data.getParcelableExtra(Intent.EXTRA_SHORTCUT_INTENT);
         String name = data.getStringExtra(Intent.EXTRA_SHORTCUT_NAME);
         Parcelable bitmap = data.getParcelableExtra(Intent.EXTRA_SHORTCUT_ICON);
 
         Bitmap icon = null;
         boolean filtered = false;
         boolean customIcon = false;
         ShortcutIconResource iconResource = null;
 
         if (bitmap != null && bitmap instanceof Bitmap) {
             icon = Utilities.createIconBitmap(new FastBitmapDrawable((Bitmap)bitmap), context);
             filtered = true;
             customIcon = true;
         } else {
             Parcelable extra = data.getParcelableExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE);
             if (extra != null && extra instanceof ShortcutIconResource) {
                 try {
                     iconResource = (ShortcutIconResource) extra;
                     final PackageManager packageManager = context.getPackageManager();
                     Resources resources = packageManager.getResourcesForApplication(
                             iconResource.packageName);
                     final int id = resources.getIdentifier(iconResource.resourceName, null, null);
                     icon = Utilities.createIconBitmap(resources.getDrawable(id), context);
                 } catch (Exception e) {
                     Log.w(TAG, "Could not load shortcut icon: " + extra);
                 }
             }
         }
 
         final ShortcutInfo info = new ShortcutInfo();
 
         if (icon == null) {
             icon = getFallbackIcon();
             info.usingFallbackIcon = true;
         }
         info.setIcon(icon);
 
         info.title = name;
         info.intent = intent;
         info.customIcon = customIcon;
         info.iconResource = iconResource;
 
         return info;
     }
 
     private static void loadLiveFolderIcon(Context context, Cursor c, int iconTypeIndex,
             int iconPackageIndex, int iconResourceIndex, LiveFolderInfo liveFolderInfo) {
 
         int iconType = c.getInt(iconTypeIndex);
         switch (iconType) {
         case LauncherSettings.Favorites.ICON_TYPE_RESOURCE:
             String packageName = c.getString(iconPackageIndex);
             String resourceName = c.getString(iconResourceIndex);
             PackageManager packageManager = context.getPackageManager();
             try {
                 Resources resources = packageManager.getResourcesForApplication(packageName);
                 final int id = resources.getIdentifier(resourceName, null, null);
                 liveFolderInfo.icon = Utilities.createIconBitmap(resources.getDrawable(id),
                         context);
             } catch (Exception e) {
                 liveFolderInfo.icon = Utilities.createIconBitmap(
                         context.getResources().getDrawable(R.drawable.ic_launcher_folder),
                         context);
             }
             liveFolderInfo.iconResource = new Intent.ShortcutIconResource();
             liveFolderInfo.iconResource.packageName = packageName;
             liveFolderInfo.iconResource.resourceName = resourceName;
             break;
         default:
             liveFolderInfo.icon = Utilities.createIconBitmap(
                     context.getResources().getDrawable(R.drawable.ic_launcher_folder),
                     context);
         }
     }
 
     void updateSavedIcon(Context context, ShortcutInfo info, Cursor c, int iconIndex) {
         // If this icon doesn't have a custom icon, check to see
         // what's stored in the DB, and if it doesn't match what
         // we're going to show, store what we are going to show back
         // into the DB.  We do this so when we're loading, if the
         // package manager can't find an icon (for example because
         // the app is on SD) then we can use that instead.
         if (info.onExternalStorage && !info.customIcon && !info.usingFallbackIcon) {
             boolean needSave;
             byte[] data = c.getBlob(iconIndex);
             try {
                 if (data != null) {
                     Bitmap saved = BitmapFactory.decodeByteArray(data, 0, data.length);
                     Bitmap loaded = info.getIcon(mIconCache);
                     needSave = !saved.sameAs(loaded);
                 } else {
                     needSave = true;
                 }
             } catch (Exception e) {
                 needSave = true;
             }
             if (needSave) {
                 Log.d(TAG, "going to save icon bitmap for info=" + info);
                 // This is slower than is ideal, but this only happens either
                 // after the froyo OTA or when the app is updated with a new
                 // icon.
                 updateItemInDatabase(context, info);
             }
         }
     }
 
     /**
      * Return an existing UserFolderInfo object if we have encountered this ID previously,
      * or make a new one.
      */
     private static UserFolderInfo findOrMakeUserFolder(HashMap<Long, FolderInfo> folders, long id) {
         // See if a placeholder was created for us already
         FolderInfo folderInfo = folders.get(id);
         if (folderInfo == null || !(folderInfo instanceof UserFolderInfo)) {
             // No placeholder -- create a new instance
             folderInfo = new UserFolderInfo();
             folders.put(id, folderInfo);
         }
         return (UserFolderInfo) folderInfo;
     }
 
     /**
      * Return an existing UserFolderInfo object if we have encountered this ID previously, or make a
      * new one.
      */
     private static LiveFolderInfo findOrMakeLiveFolder(HashMap<Long, FolderInfo> folders, long id) {
         // See if a placeholder was created for us already
         FolderInfo folderInfo = folders.get(id);
         if (folderInfo == null || !(folderInfo instanceof LiveFolderInfo)) {
             // No placeholder -- create a new instance
             folderInfo = new LiveFolderInfo();
             folders.put(id, folderInfo);
         }
         return (LiveFolderInfo) folderInfo;
     }
 
     private static String getLabel(PackageManager manager, ActivityInfo activityInfo) {
         String label = activityInfo.loadLabel(manager).toString();
         if (label == null) {
             label = manager.getApplicationLabel(activityInfo.applicationInfo).toString();
             if (label == null) {
                 label = activityInfo.name;
             }
         }
         return label;
     }
 
     private static final Collator sCollator = Collator.getInstance();
     public static final Comparator<ApplicationInfo> APP_NAME_COMPARATOR
             = new Comparator<ApplicationInfo>() {
         public final int compare(ApplicationInfo a, ApplicationInfo b) {
             return sCollator.compare(a.title.toString(), b.title.toString());
         }
     };
 
     public void dumpState() {
         Log.d(TAG, "mBeforeFirstLoad=" + mBeforeFirstLoad);
         Log.d(TAG, "mCallbacks=" + mCallbacks);
         ApplicationInfo.dumpApplicationInfoList(TAG, "mAllAppsList.data", mAllAppsList.data);
         ApplicationInfo.dumpApplicationInfoList(TAG, "mAllAppsList.added", mAllAppsList.added);
         ApplicationInfo.dumpApplicationInfoList(TAG, "mAllAppsList.removed", mAllAppsList.removed);
         ApplicationInfo.dumpApplicationInfoList(TAG, "mAllAppsList.modified", mAllAppsList.modified);
         mLoader.dumpState();
     }
 }
