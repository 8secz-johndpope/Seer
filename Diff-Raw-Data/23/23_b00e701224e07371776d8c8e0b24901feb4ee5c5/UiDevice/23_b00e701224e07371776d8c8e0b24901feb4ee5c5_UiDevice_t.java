 /*
  * Copyright (C) 2012 The Android Open Source Project
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
 
 package com.android.uiautomator.core;
 
 import android.content.Context;
 import android.graphics.Point;
 import android.os.Build;
 import android.os.Environment;
 import android.os.RemoteException;
 import android.os.ServiceManager;
 import android.os.SystemClock;
 import android.util.DisplayMetrics;
 import android.util.Log;
 import android.view.Display;
 import android.view.IWindowManager;
 import android.view.KeyEvent;
 import android.view.Surface;
 import android.view.WindowManagerImpl;
 import android.view.accessibility.AccessibilityEvent;
 import android.view.accessibility.AccessibilityNodeInfo;
 
 import com.android.internal.statusbar.IStatusBarService;
 import com.android.internal.util.Predicate;
 
 import java.io.File;
 import java.util.ArrayList;
 import java.util.HashMap;
 import java.util.List;
 import java.util.concurrent.TimeoutException;
 
 /**
  * UiDevice provides access to device wide states. Also provides methods to simulate
  * pressing hardware buttons such as DPad or the soft buttons such as Home and Menu.
  */
 public class UiDevice {
     private static final String LOG_TAG = UiDevice.class.getSimpleName();
 
     private static final long DEFAULT_TIMEOUT_MILLIS = 10 * 1000;
 
     // store for registered UiWatchers
     private final HashMap<String, UiWatcher> mWatchers = new HashMap<String, UiWatcher>();
     private final List<String> mWatchersTriggers = new ArrayList<String>();
 
     // remember if we're executing in the context of a UiWatcher
     private boolean mInWatcherContext = false;
 
     // provides access the {@link QueryController} and {@link InteractionController}
     private final UiAutomatorBridge mUiAutomationBridge;
 
     // reference to self
     private static UiDevice mDevice;
 
     private UiDevice() {
         mUiAutomationBridge = new UiAutomatorBridge();
         mDevice = this;
     }
 
     boolean isInWatcherContext() {
         return mInWatcherContext;
     }
 
     /**
      * Provides access the {@link QueryController} and {@link InteractionController}
      * @return {@link UiAutomatorBridge}
      */
     UiAutomatorBridge getAutomatorBridge() {
         return mUiAutomationBridge;
     }
     /**
      * Allow both the direct creation of a UiDevice and retrieving a existing
      * instance of UiDevice. This helps tests and their libraries to have access
      * to UiDevice with necessitating having to always pass copies of UiDevice
      * instances around.
      * @return UiDevice instance
      */
     public static UiDevice getInstance() {
         if (mDevice == null) {
             mDevice = new UiDevice();
         }
         return mDevice;
     }
 
     /**
      * Returns the display size in dp (device-independent pixel)
      *
      * The returned display size is adjusted per screen rotation
      *
      * @return
      * @hide
      */
     public Point getDisplaySizeDp() {
         Display display = WindowManagerImpl.getDefault().getDefaultDisplay();
         Point p = new Point();
         display.getSize(p);
         DisplayMetrics metrics = new DisplayMetrics();
         display.getMetrics(metrics);
         float dpx = p.x / metrics.density;
         float dpy = p.y / metrics.density;
         p.x = Math.round(dpx);
         p.y = Math.round(dpy);
         return p;
     }
 
     /**
      * Returns the product name of the device
      *
      * This provides info on what type of device that the test is running on. However, for the
      * purpose of adapting to different styles of UI, test should favor
      * {@link UiDevice#getDisplaySizeDp()} over this method, and only use product name as a fallback
      * mechanism
     * @hide
      */
     public String getProductName() {
         return Build.PRODUCT;
     }
 
     /**
      * This method returns the text from the last UI traversal event received.
      * This is helpful in WebView when the test performs directional arrow presses to focus
      * on different elements inside the WebView. The accessibility fires events
      * with every text highlighted. One can read the contents of a WebView control this way
      * however slow slow and unreliable it is. When the view control used can return a
      * reference to is Document Object Model, it is recommended then to use the view's
      * DOM instead.
      * @return text of the last traversal event else an empty string
      */
     public String getLastTraversedText() {
         return mUiAutomationBridge.getQueryController().getLastTraversedText();
     }
 
     /**
      * Helper to clear the text saved from the last accessibility UI traversal event.
      * See {@link #getLastTraversedText()}.
      */
     public void clearLastTraversedText() {
         mUiAutomationBridge.getQueryController().clearLastTraversedText();
     }
 
     /**
      * Helper method to do a short press on MENU button
      * @return true if successful else false
      */
     public boolean pressMenu() {
         return pressKeyCode(KeyEvent.KEYCODE_MENU);
     }
 
     /**
      * Helper method to do a short press on BACK button
      * @return true if successful else false
      */
     public boolean pressBack() {
         return pressKeyCode(KeyEvent.KEYCODE_BACK);
     }
 
     /**
      * Helper method to do a short press on HOME button
      * @return true if successful else false
      */
     public boolean pressHome() {
         return pressKeyCode(KeyEvent.KEYCODE_HOME);
     }
 
     /**
      * Helper method to do a short press on SEARCH button
      * @return true if successful else false
      */
     public boolean pressSearch() {
         return pressKeyCode(KeyEvent.KEYCODE_SEARCH);
     }
 
     /**
      * Helper method to do a short press on DOWN button
      * @return true if successful else false
      */
     public boolean pressDPadCenter() {
         return pressKeyCode(KeyEvent.KEYCODE_DPAD_CENTER);
     }
 
     /**
      * Helper method to do a short press on DOWN button
      * @return true if successful else false
      */
     public boolean pressDPadDown() {
         return pressKeyCode(KeyEvent.KEYCODE_DPAD_DOWN);
     }
 
     /**
      * Helper method to do a short press on UP button
      * @return true if successful else false
      */
     public boolean pressDPadUp() {
         return pressKeyCode(KeyEvent.KEYCODE_DPAD_UP);
     }
 
     /**
      * Helper method to do a short press on LEFT button
      * @return true if successful else false
      */
     public boolean pressDPadLeft() {
         return pressKeyCode(KeyEvent.KEYCODE_DPAD_LEFT);
     }
 
     /**
      * Helper method to do a short press on RIGTH button
      * @return true if successful else false
      */
     public boolean pressDPadRight() {
         return pressKeyCode(KeyEvent.KEYCODE_DPAD_RIGHT);
     }
 
     /**
      * Helper method to do a short press on DELETE
      * @return true if successful else false
      */
     public boolean pressDelete() {
         return pressKeyCode(KeyEvent.KEYCODE_DEL);
     }
 
     /**
      * Helper method to do a short press on ENTER
      * @return true if successful else false
      */
     public boolean pressEnter() {
         return pressKeyCode(KeyEvent.KEYCODE_ENTER);
     }
 
     /**
      * Helper method to do a short press using a key code. See {@link KeyEvent}
      * @return true if successful else false
      */
     public boolean pressKeyCode(int keyCode) {
         waitForIdle();
         return mUiAutomationBridge.getInteractionController().sendKey(keyCode, 0);
     }
 
     /**
      * Helper method to do a short press using a key code. See {@link KeyEvent}
      * @param keyCode See {@link KeyEvent}
      * @param metaState See {@link KeyEvent}
      * @return true if successful else false
      */
     public boolean pressKeyCode(int keyCode, int metaState) {
         waitForIdle();
         return mUiAutomationBridge.getInteractionController().sendKey(keyCode, metaState);
     }
 
     /**
      * Gets the width of the display, in pixels. The width and height details
      * are reported based on the current orientation of the display.
      * @return width in pixels or zero on failure
      */
     public int getDisplayWidth() {
         IWindowManager wm = IWindowManager.Stub.asInterface(
                 ServiceManager.getService(Context.WINDOW_SERVICE));
         Point p = new Point();
         try {
             wm.getDisplaySize(p);
         } catch (RemoteException e) {
             return 0;
         }
         return p.x;
     }
 
     /**
      * Press recent apps soft key
      * @return true if successful
      * @throws RemoteException
      */
     public boolean pressRecentApps() throws RemoteException {
         waitForIdle();
         final IStatusBarService statusBar = IStatusBarService.Stub.asInterface(
                 ServiceManager.getService(Context.STATUS_BAR_SERVICE));
 
         if (statusBar != null) {
             statusBar.toggleRecentApps();
             return true;
         }
         return false;
     }
 
     /**
      * Gets the height of the display, in pixels. The size is adjusted based
      * on the current orientation of the display.
      * @return height in pixels or zero on failure
      */
     public int getDisplayHeight() {
         IWindowManager wm = IWindowManager.Stub.asInterface(
                 ServiceManager.getService(Context.WINDOW_SERVICE));
         Point p = new Point();
         try {
             wm.getDisplaySize(p);
         } catch (RemoteException e) {
             return 0;
         }
         return p.y;
     }
 
     /**
      * Perform a click at arbitrary coordinates specified by the user
      *
      * @param x coordinate
      * @param y coordinate
      * @return true if the click succeeded else false
      */
     public boolean click(int x, int y) {
         if (x >= getDisplayWidth() || y >= getDisplayHeight()) {
             return (false);
         }
         return getAutomatorBridge().getInteractionController().click(x, y);
     }
 
     /**
      * Performs a swipe from one coordinate to another using the number of steps
      * to determine smoothness and speed. Each step execution is throttled to 5ms
      * per step. So for a 100 steps, the swipe will take about 1/2 second to complete.
      *
      * @param startX
      * @param startY
      * @param endX
      * @param endY
      * @param steps is the number of move steps sent to the system
      * @return false if the operation fails or the coordinates are invalid
      */
     public boolean swipe(int startX, int startY, int endX, int endY, int steps) {
         return mUiAutomationBridge.getInteractionController()
                 .scrollSwipe(startX, startY, endX, endY, steps);
     }
 
     /**
      * Performs a swipe between points in the Point array. Each step execution is throttled
      * to 5ms per step. So for a 100 steps, the swipe will take about 1/2 second to complete
      *
      * @param segments is Point array containing at least one Point object
      * @param segmentSteps steps to inject between two Points
      * @return true on success
      */
     public boolean swipe(Point[] segments, int segmentSteps) {
         return mUiAutomationBridge.getInteractionController().swipe(segments, segmentSteps);
     }
 
     public void waitForIdle() {
         waitForIdle(DEFAULT_TIMEOUT_MILLIS);
     }
 
     public void waitForIdle(long time) {
         mUiAutomationBridge.waitForIdle(time);
     }
 
     /**
      * Last activity to report accessibility events
      * @return String name of activity
      */
     public String getCurrentActivityName() {
         return mUiAutomationBridge.getQueryController().getCurrentActivityName();
     }
 
     /**
      * Last package to report accessibility events
      * @return String name of package
      */
     public String getCurrentPackageName() {
         return mUiAutomationBridge.getQueryController().getCurrentPackageName();
     }
 
     /**
      * Registers a condition watcher to be called by the automation library only when a
      * {@link UiObject} method call is in progress and is in retry waiting to match
      * its UI element. Only during these conditions the watchers are invoked to check if
      * there is something else unexpected on the screen that may be causing the match failure
      * and retries. Under normal conditions when UiObject methods are immediately matching
      * their UI element, watchers may never get to run. See {@link UiDevice#runWatchers()}
      *
      * @param name of watcher
      * @param watcher {@link UiWatcher}
      */
     public void registerWatcher(String name, UiWatcher watcher) {
         if (mInWatcherContext) {
             throw new IllegalStateException("Cannot register new watcher from within another");
         }
         mWatchers.put(name, watcher);
     }
 
     /**
      * Removes a previously registered {@link #registerWatcher(String, UiWatcher)}.
      *
      * @param name of watcher used when <code>registerWatcher</code> was called.
      * @throws UiAutomationException
      */
     public void removeWatcher(String name) {
         if (mInWatcherContext) {
             throw new IllegalStateException("Cannot remove a watcher from within another");
         }
         mWatchers.remove(name);
     }
 
     /**
      * See {@link #registerWatcher(String, UiWatcher)}. This forces all registered watchers
      * to run.
      */
     public void runWatchers() {
         if (mInWatcherContext) {
             return;
         }
 
         for (String watcherName : mWatchers.keySet()) {
             UiWatcher watcher = mWatchers.get(watcherName);
             if (watcher != null) {
                 try {
                     mInWatcherContext = true;
                     if (watcher.checkForCondition()) {
                         setWatcherTriggered(watcherName);
                     }
                 } catch (Exception e) {
                     Log.e(LOG_TAG, "Exceuting watcher: " + watcherName, e);
                 } finally {
                     mInWatcherContext = false;
                 }
             }
         }
     }
 
     /**
      * See {@link #registerWatcher(String, UiWatcher)}. If a watcher is run and
      * returns true from its implementation of {@link UiWatcher#checkForCondition()} then
      * it is considered triggered.
      */
     public void resetWatcherTriggers() {
         mWatchersTriggers.clear();
     }
 
     /**
      * See {@link #registerWatcher(String, UiWatcher)}. If a watcher is run and
      * returns true from its implementation of {@link UiWatcher#checkForCondition()} then
      * it is considered triggered. This method can be used to check if a specific UiWatcher
      * has been triggered during the test. This is helpful if a watcher is detecting errors
      * from ANR or crash dialogs and the test needs to know if a UiWatcher has been triggered.
      */
     public boolean hasWatcherTriggered(String watcherName) {
         return mWatchersTriggers.contains(watcherName);
     }
 
     /**
      * See {@link #registerWatcher(String, UiWatcher)} and {@link #hasWatcherTriggered(String)}
      */
     public boolean hasAnyWatcherTriggered() {
         return mWatchersTriggers.size() > 0;
     }
 
     private void setWatcherTriggered(String watcherName) {
         if (!hasWatcherTriggered(watcherName)) {
             mWatchersTriggers.add(watcherName);
         }
     }
 
     /**
      * Check if the device is in its natural orientation. This is determined by checking if the
      * orientation is at 0 or 180 degrees.
      * @return true if it is in natural orientation
     * @hide
      */
     public boolean isNaturalOrientation() {
         Display display = WindowManagerImpl.getDefault().getDefaultDisplay();
         return display.getRotation() == Surface.ROTATION_0 ||
                 display.getRotation() == Surface.ROTATION_180;
     }
 
     /**
      * Disables the sensors and freezes the device rotation at its
      * current rotation state.
      * @throws RemoteException
      */
     public void freezeRotation() throws RemoteException {
         getAutomatorBridge().getInteractionController().freezeRotation();
     }
 
     /**
      * Re-enables the sensors and un-freezes the device rotation allowing its contents
      * to rotate with the device physical rotation. Note that by un-freezing the rotation,
      * the screen contents may suddenly rotate depending on the current physical position
      * of the test device. During a test execution, it is best to keep the device frozen
      * in a specific orientation until the test case execution is completed.
      * @throws RemoteException
      */
     public void unfreezeRotation() throws RemoteException {
         getAutomatorBridge().getInteractionController().unfreezeRotation();
     }
 
     /**
      * Orients the device to the left and also freezes rotation in that
      * orientation by disabling the sensors. If you want to un-freeze the rotation
      * and re-enable the sensors see {@link #unfreezeRotation()}. Note that doing
      * so may cause the screen contents to get re-oriented depending on the current
      * physical position of the test device.
      * @throws RemoteException
     * @hide
      */
     public void setOrientationLeft() throws RemoteException {
         getAutomatorBridge().getInteractionController().setRotationLeft();
     }
 
     /**
      * Orients the device to the right and also freezes rotation in that
      * orientation by disabling the sensors. If you want to un-freeze the rotation
      * and re-enable the sensors see {@link #unfreezeRotation()}. Note that doing
      * so may cause the screen contents to get re-oriented depending on the current
      * physical position of the test device.
      * @throws RemoteException
     * @hide
      */
     public void setOrientationRight() throws RemoteException {
         getAutomatorBridge().getInteractionController().setRotationRight();
     }
 
     /**
      * Rotates right and also freezes rotation in that orientation by
      * disabling the sensors. If you want to un-freeze the rotation
      * and re-enable the sensors see {@link #unfreezeRotation()}. Note
      * that doing so may cause the screen contents to rotate
      * depending on the current physical position of the test device.
      * @throws RemoteException
     * @hide
      */
     public void setOrientationNatural() throws RemoteException {
         getAutomatorBridge().getInteractionController().setRotationNatural();
     }
 
     /**
      * This method simply presses the power button if the screen is OFF else
      * it does nothing if the screen is already ON. If the screen was OFF and
      * it just got turned ON, this method will insert a 500ms delay to allow
      * the device time to wake up and accept input.
      * @throws RemoteException
      */
     public void wakeUp() throws RemoteException {
         if(getAutomatorBridge().getInteractionController().wakeDevice()) {
             // sync delay to allow the window manager to start accepting input
             // after the device is awakened.
             SystemClock.sleep(500);
         }
     }
 
     /**
      * Checks the power manager if the screen is ON
      * @return true if the screen is ON else false
      * @throws RemoteException
      */
     public boolean isScreenOn() throws RemoteException {
         return getAutomatorBridge().getInteractionController().isScreenOn();
     }
 
     /**
      * This method simply presses the power button if the screen is ON else
      * it does nothing if the screen is already OFF.
      * @throws RemoteException
      */
     public void sleep() throws RemoteException {
         getAutomatorBridge().getInteractionController().sleepDevice();
     }
 
     /**
      * Helper method used for debugging to dump the current window's layout hierarchy.
      * The file root location is /data/local/tmp
      *
      * @param fileName
      */
     public void dumpWindowHierarchy(String fileName) {
         AccessibilityNodeInfo root =
                 getAutomatorBridge().getQueryController().getAccessibilityRootNode();
         if(root != null) {
             AccessibilityNodeInfoDumper.dumpWindowToFile(
                     root, new File(new File(Environment.getDataDirectory(),
                             "local/tmp"), fileName));
         }
     }
 
     /**
      * Waits for a window content update event to occur
      *
      * if a package name for window is specified, but current window is not with the same package
      * name, the function will return immediately
      *
      * @param packageName the specified window package name; maybe <code>null</code>, and a window
      *                    update from any frontend window will end the wait
      * @param timeout the timeout for the wait
      *
      * @return true if a window update occured, false if timeout has reached or current window is
      * not the specified package name
      */
     public boolean waitForWindowUpdate(final String packageName, long timeout) {
         if (packageName != null) {
             if (!packageName.equals(getCurrentPackageName())) {
                 return false;
             }
         }
         Runnable emptyRunnable = new Runnable() {
             @Override
             public void run() {
             }
         };
         Predicate<AccessibilityEvent> checkWindowUpdate = new Predicate<AccessibilityEvent>() {
             @Override
             public boolean apply(AccessibilityEvent t) {
                 if (t.getEventType() == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED) {
                     return packageName == null || packageName.equals(t.getPackageName());
                 }
                 return false;
             }
         };
         try {
             getAutomatorBridge().executeCommandAndWaitForAccessibilityEvent(
                     emptyRunnable, checkWindowUpdate, timeout);
         } catch (TimeoutException e) {
             return false;
         } catch (Exception e) {
             Log.e(LOG_TAG, "waitForWindowUpdate: general exception from bridge", e);
             return false;
         }
         return true;
     }
 }
