 /*
  * Copyright 2010 akquinet
  * Licensed under the Apache License, Version 2.0 (the "License");
  * you may not use this file except in compliance with the License.
  * You may obtain a copy of the License at
  *
  *   http://www.apache.org/licenses/LICENSE-2.0
  *
  * Unless required by applicable law or agreed to in writing, software
  * distributed under the License is distributed on an "AS IS" BASIS,
  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  * See the License for the specific language governing permissions and
  * limitations under the License.
  */
 package de.akquinet.android.marvin;
 
 import java.util.ArrayList;
 import java.util.Comparator;
 import java.util.HashMap;
 import java.util.List;
 import java.util.Map;
 import java.util.SortedSet;
 import java.util.TreeSet;
 import java.util.concurrent.TimeUnit;
 import java.util.concurrent.TimeoutException;
 
 import junit.framework.Assert;
 
 import org.hamcrest.Matcher;
 import org.hamcrest.MatcherAssert;
 
 import android.app.Activity;
 import android.app.Instrumentation;
 import android.app.Service;
 import android.content.Context;
 import android.content.Intent;
 import android.content.ServiceConnection;
 import android.os.IBinder;
 import android.os.SystemClock;
 import android.test.InstrumentationTestCase;
 import android.util.Log;
 import android.view.KeyEvent;
 import android.view.MotionEvent;
 import android.view.View;
 import de.akquinet.android.marvin.actions.ActionFactory;
 import de.akquinet.android.marvin.actions.ActivityAction;
 import de.akquinet.android.marvin.matchers.Condition;
 import de.akquinet.android.marvin.matchers.util.WaitForConditionUtil;
 import de.akquinet.android.marvin.monitor.ExtendedActivityMonitor;
 import de.akquinet.android.marvin.monitor.StartedActivity;
 import de.akquinet.android.marvin.util.TemporaryServiceConnection;
 
 
 /**
  * <p>
  * Base test case class for Marvin tests. It provides methods to control
  * activites, bind to services and define various assertions.
  * 
  * <p>
  * Every activity that is started directly or indirectly within a test method is
  * finished automatically on {@link #tearDown()} to ensure a clean state for
  * following tests. If you want to leave certain activities running, you can
  * make use of method {@link #leaveRunningAfterTearDown(Class...)}.
  * 
  * <p>
  * When overwriting {@link #setUp()} or {@link #tearDown()}, you must call the
  * super implementations.
  * 
  * @author Philipp Kumar
  */
 public class AndroidTestCase extends InstrumentationTestCase
 {
     /** Activity monitor keeping track of started activites */
     protected ExtendedActivityMonitor activityMonitor;
 
     /**
      * Information on what to do with instances of certain activity types on
      * {@link #tearDown()}
      */
     private Map<Class<? extends Activity>, TearDownAction> tearDownActions =
             new HashMap<Class<? extends Activity>, TearDownAction>();
 
     /**
      * {@link ServiceConnection} instances created using the bindService(..)
      * methods.
      */
     private Map<IBinder, ServiceConnection> serviceConnections =
             new HashMap<IBinder, ServiceConnection>();
 
     /*
      * 
      */
 
     /**
      * Launches a new {@link Activity} of the given type, wait for it to start
      * and return the corresponding activity instance. The activity is later
      * finished on {@link #tearDown()} per default.
      * <p>
      * The function returns as soon as the activity goes idle following the call
      * to its {@link Activity#onCreate}. Generally this means it has gone
      * through the full initialization including {@link Activity#onResume} and
      * drawn and displayed its initial window.
      * 
      * @param <T>
      *            the activity type
      * @param activityClass
      *            the activity type
      * @return the created activity instance
      */
     @SuppressWarnings("unchecked")
     public final <T extends Activity> T startActivity(Class<T> activityClass) {
         Instrumentation instr = getInstrumentation();
 
         Intent intent = new Intent(Intent.ACTION_MAIN);
         intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
         intent.addFlags(Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
         intent.setClassName(instr.getTargetContext(), activityClass.getName());
 
         return (T) startActivity(intent);
     }
 
     /**
      * Launches a new {@link Activity} using the given {@link Intent}. The
      * started activity is closed on {@link #tearDown()} per default.
      * <p>
      * The function returns as soon as the activity goes idle following the call
      * to its {@link Activity#onCreate}. Generally this means it has gone
      * through the full initialization including {@link Activity#onResume} and
      * drawn and displayed its initial window.
      * 
      * @param intent
      *            the {@link Intent} to use for activity start.
      * @return the created Activity instance
      */
     public final Activity startActivity(Intent intent) {
         Instrumentation instr = getInstrumentation();
         return instr.startActivitySync(intent);
     }
 
     /**
      * Blocks until an {@link Activity} of the given type is started. The
      * instance of the started activity is then returned. If such an activity is
      * not started within the given amount of time, this method returns null.
      * 
      * @param activityClass
      *            the type of activity to wait for
      * @param timeout
      *            amount of time to wait for activity start
      * @param timeUnit
      *            the time unit of the timeout parameter
      * @return the activity waited for, or null if timeout was reached before
      *         any suitable activity was started
      */
     public final <T extends Activity> T waitForActivity(
             Class<T> activityClass, long timeout, TimeUnit timeUnit) {
         return activityMonitor.waitForActivity(
                 activityClass, timeout, timeUnit);
     }
 
     /**
      * <p>
      * Sends an up and down key event sync to the currently focused window.
      * 
      * <p>
      * Example: <code>keyDownUp(KeyEvent.KEYCODE_DPAD_DOWN)</code>
      * 
      * <p>
      * You can only do this if the currently focused window belongs to the
      * application under test. In particular, this method will fail while the
      * key lock is active.
      * 
      * @param key
      *            The integer keycode for the event.
      * @see KeyEvent
      */
     public void keyDownUp(int key) {
         getInstrumentation().sendKeyDownUpSync(key);
 
     }
 
     /**
      * <p>
      * Sends a key event to the currently focused window.
      * 
      * <p>
      * Example: <code>keyDownUp(KeyEvent.KEYCODE_DPAD_DOWN)</code>
      * 
      * <p>
      * You can only do this if the currently focused window belongs to the
      * application under test. In particular, this method will fail while the
      * key lock is active.
      * 
      * @param event
      *            The key event.
      * @see KeyEvent
      */
     public void key(KeyEvent event) {
         getInstrumentation().sendKeySync(event);
 
     }
 
     /**
      * <p>
      * Sends the key events corresponding to the given text to the currently
      * focused window.
      * 
      * <p>
      * You can only do this if the currently focused window belongs to the
      * application under test. In particular, this method will fail while the
      * key lock is active.
      * 
      * @param text
      *            The text to be sent.
      */
     public void sendString(String text) {
         getInstrumentation().sendStringSync(text);
 
     }
 
     /**
      * <p>
      * Clicks the view at the given coordinates.
      * 
      * <p>
      * You can only do this if the currently focused window belongs to the
      * application under test. In particular, this method will fail while the
      * key lock is active.
      * 
      * @param x
      *            the x coordinate of the view to click
      * @param y
      *            the y coordinate of the view to click
      */
     public void click(float x, float y) {
         MotionEvent downEvent = MotionEvent.obtain(SystemClock.uptimeMillis(),
                 SystemClock.uptimeMillis(), MotionEvent.ACTION_DOWN, x, y, 0);
         MotionEvent upEvent = MotionEvent.obtain(SystemClock.uptimeMillis(),
                 SystemClock.uptimeMillis(), MotionEvent.ACTION_UP, x, y, 0);
         try {
             getInstrumentation().sendPointerSync(downEvent);
             getInstrumentation().sendPointerSync(upEvent);
         }
         catch (SecurityException e) {
             Assert.fail("Click on (" + x + "," + y + ") failed.");
         }
     }
 
     /**
      * Synchronously waits until the application becomes idle.
      */
     public void waitForIdle() {
         getInstrumentation().waitForIdleSync();
         try {
             Thread.sleep(1000);
         }
         catch (InterruptedException e) {
             // ignore
         }
         getInstrumentation().waitForIdleSync();
     }
 
     /**
      * Returns the most recently started {@link Activity}. In most cases, this
      * will be the activity currently visible on screen.
      */
     public final Activity getMostRecentlyStartedActivity() {
         return this.activityMonitor.getMostRecentlyStartedActivity();
     }
 
     /**
      * <p>
      * Define certain Activity types as to be left running even after
      * {@link #tearDown()}.
      * 
      * <p>
      * Per default, an activity started during {@link #setUp()} as well as
      * during the test method run itself is finished on {@link #tearDown()},
      * even those that are not explicitely started from the test. To prevent
      * this for certain activity types, call this method. Activities of the
      * given types will be left running until instrumentation finishes (in
      * general, this is after the whole test suite is finished).
      * 
      * @param activityClasses
      *            the activity types not to be finished on {@link #tearDown()}
      */
     public final void leaveRunningAfterTearDown(
             Class<? extends Activity>... activityClasses) {
         for (Class<? extends Activity> activityClass : activityClasses) {
             tearDownActions.put(activityClass, TearDownAction.LEAVE_RUNNING);
         }
     }
 
     public final void finishOnTearDown(
             Class<? extends Activity>... activityClasses) {
         for (Class<? extends Activity> activityClass : activityClasses) {
             tearDownActions.put(activityClass, TearDownAction.FINISH);
         }
     }
 
     /**
      * Returns a list of all activities that were started since the beginning of
      * this test, in the order of their start time.
      */
     public final List<Activity> getStartedActivities() {
         SortedSet<StartedActivity> startedActivities = new TreeSet<StartedActivity>(
                 new Comparator<StartedActivity>() {
                     public int compare(StartedActivity a1, StartedActivity a2) {
                         return (int) (a1.getStartTime() - a2.getStartTime());
                     };
                 });
 
         startedActivities.addAll(activityMonitor.getStartedActivities());
 
         ArrayList<Activity> results = new ArrayList<Activity>();
         for (StartedActivity startedActivity : startedActivities) {
             results.add(startedActivity.getActivity());
         }
 
         return results;
     }
 
     /**
      * Synchronously binds to the service of the given class and returns the
      * service object. The service is auto-created (see
      * Service.BIND_AUTO_CREATE) and automatically unbound during
      * {@link #tearDown()}.
      * 
      * @param serviceClass
      *            the class of the service to bind to
      * @param timeout
      *            Time to wait for the service before throwing an
      *            {@link TimeoutException}.
      * @param timeUnit
      *            the unit of the timeout parameter
      * @throws TimeoutException
      *             if the timeout is reached before we could connect to the
      *             service
      */
     public final IBinder bindService(Class<?> serviceClass, int timeout,
             TimeUnit timeUnit) throws TimeoutException {
         Intent intent = new Intent(
                 getInstrumentation().getTargetContext(), serviceClass);
         return bindService(intent, serviceClass, Service.BIND_AUTO_CREATE,
                 timeout, timeUnit);
     }
 
     /**
      * Synchronously binds to the service using the given intent and flags, and
      * return the service object. The intent and flags parameters are passed to
      * {@link Context#bindService(Intent, android.content.ServiceConnection, int)}
      * . The service is automatically unbound during {@link #tearDown()}.
      * 
      * @param serviceIntent
      *            the intent to bind to the service
      * @param flags
      *            the flags used to bind to the service
      * @param timeout
      *            Time to wait for the service before throwing an
      *            {@link TimeoutException}.
      * @param timeUnit
      *            the unit of the timeout parameter
      * @throws TimeoutException
      *             if the timeout is reached before we could connect to the
      *             service
      */
     public final IBinder bindService(Intent serviceIntent, int flags,
             int timeout, TimeUnit timeUnit) throws TimeoutException {
         return bindService(serviceIntent, null, flags, timeout, timeUnit);
     }
 
     public <T extends Activity> ActivityAction activity(T activity) {
         return ActionFactory.createActivityAction(getInstrumentation(), activityMonitor, activity);
     }
 
     public <T> void assertThat(T actual, Matcher<? super T> matcher) {
         MatcherAssert.assertThat(actual, matcher);
     }
 
     public <T> void assertThat(String reason, T actual, Matcher<? super T> matcher) {
         MatcherAssert.assertThat(reason, actual, matcher);
     }
 
     /**
      * Convenience method for
      * <i>getInstrumentation().getContext().getString(int)</i>
      */
     public final String getString(int resId) {
         return getInstrumentation().getContext().getString(resId);
     }
 
     /**
      * Convenience method for {@link Thread#sleep(long)}, returns immediately on
      * interrupt.
      */
     public final void sleep(long ms) {
         try {
             Thread.sleep(ms);
         }
         catch (InterruptedException e) {
             // abort sleeping
         }
     }
 
     /**
      * <p>
      * Waits for a view with the given id to become visible.
      * 
      * <p>
      * Will return immediately if this view is already existent and visible. If
      * it is not, the method blocks until the view appears or the given timeout
      * is reached. In the latter case, an {@link TimeoutException} is thrown.
      * 
      * @param activity
      *            the activity instance
      * @param viewId
      *            the id of the view to wait for
      * @param timeout
      *            the timeout value
      * @param timeUnit
      *            the time unit of the timeout value
      * @return the View object of the existent and visible view
      * @throws TimeoutException
      *             if the view does not appear withing the given time frame
      */
     public final View waitForView(final Activity activity, final int viewId,
             long timeout, TimeUnit timeUnit) throws TimeoutException {
         waitForCondition(new Condition("View exists and is visible") {
             @Override
             public boolean matches() {
                 View view = activity.findViewById(viewId);
                 return view != null && view.getVisibility() == View.VISIBLE;
             }
         }, timeout, timeUnit);
 
         return activity.findViewById(viewId);
     }
 
     /**
      * <p>
      * Waits for the given {@link Condition} to match.
      * 
      * <p>
      * Will return immediately if the condition already matches. If it does not,
      * the method blocks until the condition matches or the given timeout is
      * reached. In the latter case, an {@link TimeoutException} is thrown.
      * 
      * @param condition
      *            the {@link Condition} that shall match
      * @param timeout
      *            the timeout value
      * @param timeUnit
      *            the time unit of the timeout value
      * @throws TimeoutException
      *             if the condition does not match within the given time frame
      */
     public final void waitForCondition(Condition condition, long timeout, TimeUnit timeUnit)
             throws TimeoutException {
         WaitForConditionUtil.waitForCondition(condition, timeout, timeUnit);
     }
 
     /**
      * <p>
      * Waits for the given Hamcrest {@link Matcher} to match on a given item.
      * 
      * <p>
      * Will return immediately if the matcher already matches on this item. If
      * it does not, the method blocks until the matcher matches or the given
      * timeout is reached. In the latter case, an {@link TimeoutException} is
      * thrown.
      * 
      * @param item
      *            the item to be passed to the matcher
      * @param matcher
      *            the hamcrest {@link Matcher} that shall match
      * @param timeout
      *            the timeout value
      * @param timeUnit
      *            the time unit of the timeout value
      * @throws TimeoutException
      *             if the matcher does not match on the given item within the
      *             given time frame
      */
     public final <T> void waitForCondition(Object item, Matcher<T> matcher,
             long timeout, TimeUnit timeUnit) throws TimeoutException {
         WaitForConditionUtil.waitForCondition(item, matcher, timeout, timeUnit);
     }
 
     @Override
     protected void setUp() throws Exception {
         super.setUp();
 
         this.activityMonitor =
                 new ExtendedActivityMonitor(getInstrumentation());
         this.activityMonitor.start();
         getInstrumentation().waitForIdleSync();
         sleep(3000);
     }
 
     @Override
     protected void tearDown() throws Exception {
         this.activityMonitor.stop();
         getInstrumentation().waitForIdleSync();
 
         for (StartedActivity activity : activityMonitor.getStartedActivities()) {
             if (!shallLeaveRunning(activity.getActivity())
                     && !activity.getActivity().isFinishing()) {
                 try {
                     Log.i(getClass().getName(), "Finishing activity: "
                             + activity.getActivity().getClass().getName());
                     activity.getActivity().finish();
                     getInstrumentation().waitForIdleSync();
                     Thread.sleep(1000);
                     getInstrumentation().waitForIdleSync();
                 }
                 catch (Exception e) {
                     Log.e(getClass().getName(),
                             "Problem on activity finish:", e);
                 }
             }
         }
 
         // Unbind services that were bound using bindService(..) methods
         for (ServiceConnection connection : serviceConnections.values()) {
             getInstrumentation().getTargetContext().unbindService(connection);
         }
         serviceConnections.clear();
 
        activityMonitor.clear();
         getInstrumentation().waitForIdleSync();
 
         super.tearDown();
     }
 
     private IBinder bindService(Intent serviceIntent, Class<?> serviceClass,
             int flags, int timeout, TimeUnit timeUnit) throws TimeoutException {
         TemporaryServiceConnection serviceConnection =
                 new TemporaryServiceConnection(timeout, timeUnit);
         getInstrumentation().getTargetContext().bindService(serviceIntent,
                 serviceConnection, flags);
 
         IBinder serviceBinder = serviceConnection.getBinderSync();
         if (serviceBinder == null) {
             throw new TimeoutException("Timeout hit ("
                     + timeout + " " + timeUnit.toString().toLowerCase()
                     + ") while trying to connect to service"
                     + serviceClass != null ? " " + serviceClass.getName() : ""
                     + ".");
         }
         serviceConnections.put(serviceBinder, serviceConnection);
         return serviceBinder;
     }
 
     private boolean shallLeaveRunning(Activity activity) {
         TearDownAction tearDownAction =
                 this.tearDownActions.get(activity.getClass());
         if (tearDownAction != null) {
             return tearDownAction.equals(TearDownAction.LEAVE_RUNNING);
         }
         return false;
     }
 
     private enum TearDownAction
     {
         LEAVE_RUNNING, FINISH;
     }
 }
