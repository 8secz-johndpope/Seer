 package de.fu_berlin.inf.dpp.synchronize;
 
 import java.util.Collection;
 import java.util.Collections;
 import java.util.HashMap;
 import java.util.HashSet;
 import java.util.LinkedList;
 import java.util.List;
 import java.util.Map;
 import java.util.Set;
 import java.util.concurrent.CancellationException;
 import java.util.concurrent.CopyOnWriteArrayList;
 import java.util.concurrent.CountDownLatch;
 import java.util.concurrent.TimeUnit;
 import java.util.concurrent.locks.Condition;
 import java.util.concurrent.locks.Lock;
 import java.util.concurrent.locks.ReentrantLock;
 
 import org.apache.log4j.Logger;
 import org.eclipse.core.runtime.NullProgressMonitor;
 import org.eclipse.core.runtime.SubMonitor;
 import org.picocontainer.Disposable;
 
 import de.fu_berlin.inf.dpp.User;
 import de.fu_berlin.inf.dpp.activities.AbstractActivityReceiver;
 import de.fu_berlin.inf.dpp.activities.IActivity;
 import de.fu_berlin.inf.dpp.activities.IActivityReceiver;
 import de.fu_berlin.inf.dpp.activities.StopActivity;
 import de.fu_berlin.inf.dpp.activities.StopActivity.State;
 import de.fu_berlin.inf.dpp.activities.StopActivity.Type;
 import de.fu_berlin.inf.dpp.annotations.Component;
 import de.fu_berlin.inf.dpp.observables.SharedProjectObservable;
 import de.fu_berlin.inf.dpp.project.IActivityListener;
 import de.fu_berlin.inf.dpp.project.IActivityProvider;
 import de.fu_berlin.inf.dpp.project.ISharedProject;
 import de.fu_berlin.inf.dpp.project.internal.SharedProject;
 import de.fu_berlin.inf.dpp.util.Util;
 import de.fu_berlin.inf.dpp.util.ValueChangeListener;
 
 @Component(module = "core")
 public class StopManager implements IActivityProvider, Disposable {
 
     private static Logger log = Logger.getLogger(StopManager.class.getName());
 
     // Waits MILLISTOWAIT ms until the next test for progress cancellation
     public final int MILLISTOWAIT = 100;
 
     private final List<IActivityListener> activityListeners = new LinkedList<IActivityListener>();
 
     protected List<Blockable> blockables = new LinkedList<Blockable>();
 
     protected ISharedProject sharedProject;
 
     SharedProjectObservable sharedProjectObservable;
 
     /**
      * Maps a User to a List of his StartHandles. Never touch this directly, use
      * the add and remove methods.
      */
     private Map<User, List<StartHandle>> startHandles = Collections
         .synchronizedMap(new HashMap<User, List<StartHandle>>());
 
     /**
      * For every initiated unlock (identified by its StopActivity id) there is
      * one acknowledgment expected.
      */
     private Map<String, StartHandle> startsToBeAcknowledged = Collections
         .synchronizedMap(new HashMap<String, StartHandle>());
 
     // blocking mechanism
     protected Lock reentrantLock = new ReentrantLock();
     protected final Condition acknowledged = reentrantLock.newCondition();
 
     /**
      * For every initiated StopActivity (type: LockRequest) there is one
      * acknowledgment expected.
      */
     protected Set<StopActivity> expectedAcknowledgments = Collections
         .synchronizedSet(new HashSet<StopActivity>());
 
     protected ValueChangeListener<SharedProject> sharedProjectObserver = new ValueChangeListener<SharedProject>() {
         public void setValue(SharedProject newSharedProject) {
 
             if (newSharedProject == sharedProject)
                 return;
 
             // session ended, start all local start handles
             if (newSharedProject == null && sharedProject != null) {
                 for (StartHandle startHandle : getStartHandles(sharedProject
                     .getLocalUser())) {
                     startHandle.start();
                 }
                 lockProject(false);
             }
 
             if (sharedProject != null) {
                 sharedProject.removeActivityProvider(StopManager.this);
                 reset();
             }
 
             sharedProject = newSharedProject;
 
             if (newSharedProject != null) {
                 newSharedProject.addActivityProvider(StopManager.this);
             }
         }
     };
 
     public StopManager(SharedProjectObservable observable) {
 
         this.sharedProjectObservable = observable;
         observable.add(sharedProjectObserver);
     }
 
     protected IActivityReceiver activityReceiver = new AbstractActivityReceiver() {
 
         @Override
         public void receive(final StopActivity stopActivity) {
 
             if (sharedProject == null)
                 throw new IllegalStateException(
                     "Cannot receive StopActivities without a shared project");
 
             User user = sharedProject.getUser(stopActivity.getRecipient());
             if (user == null || !user.isLocal())
                 throw new IllegalArgumentException(
                     "Received StopActivity which is not for the local user");
 
             if (stopActivity.getType() == Type.LOCKREQUEST) {
 
                 /*
                  * local user locks his project and adds a startHandle so he
                  * knows he is locked. Then he acknowledges
                  */
                 if (stopActivity.getState() == State.INITIATED) {
                     addStartHandle(generateStartHandle(stopActivity));
                     // locks project and acknowledges
                     Util.runSafeSWTSync(log, new Runnable() {
                         public void run() {
                             lockProject(true);
                             fireActivity(stopActivity
                                 .generateAcknowledgment(sharedProject
                                     .getLocalUser().getJID().toString()));
                         }
                     });
                     return;
                 }
                 if (stopActivity.getState() == State.ACKNOWLEDGED) {
                     if (!expectedAcknowledgments.contains(stopActivity)) {
                         log.warn("Received unexpected StopActivity: "
                             + stopActivity);
                         return;
                     }
 
                     // it has to be removed from the expected ack list
                     // because it already arrived
                     if (expectedAcknowledgments.remove(stopActivity)) {
                         reentrantLock.lock();
                         acknowledged.signalAll();
                         reentrantLock.unlock();
                         return;
                     } else {
                         log.warn("Received unexpected "
                             + "StopActivity acknowledgement: " + stopActivity);
                         return;
                     }
                 }
             }
 
             if (stopActivity.getType() == Type.UNLOCKREQUEST) {
                 if (stopActivity.getState() == State.INITIATED) {
 
                     executeUnlock(generateStartHandle(stopActivity));
                     // sends an acknowledgment
                     fireActivity(stopActivity
                         .generateAcknowledgment(sharedProject.getLocalUser()
                             .getJID().toString()));
                 }
 
                 if (stopActivity.getState() == State.ACKNOWLEDGED) {
                     StartHandle handle = startsToBeAcknowledged
                         .remove(stopActivity.getActivityID());
                     if (handle == null) {
                         log.error("StartHandle for " + stopActivity
                             + " could not be found.");
                         return;
                     }
                     handle.acknowledge();
                     return;
                 }
             }
 
             throw new IllegalArgumentException(
                 "StopActivity is of unknown type: " + stopActivity);
         }
     };
 
     /**
      * Blocking method that asks the given users to halt all user-input and
      * returns a list of handles to be used when the users can start again.
      * 
      * TODO This method is not tested for more than one user since it is not
      * used yet.
      * 
      * @param users
      *            the participants who has to stop
      * @param cause
      *            the cause for stopping as it is displayed in the progress
      *            monitor
      * 
      * @param monitor
      *            The caller is expected to call beginTask and done on the given
      *            SubMonitor
      * 
      * @noSWT This method mustn't be called from the SWT thread.
      * 
      * @blocking returning after the given users acknowledged the stop
      * 
      * @cancelable This method can be canceled by the user
      * 
      * @throws CancellationException
      */
     public List<StartHandle> stop(final Collection<User> users,
         final String cause, final SubMonitor monitor)
         throws CancellationException {
 
         final List<StartHandle> resultingHandles = Collections
             .synchronizedList(new LinkedList<StartHandle>());
         final CountDownLatch doneSignal = new CountDownLatch(users.size());
 
         for (final User user : users) {
             Util.runSafeAsync(log, new Runnable() {
                 public void run() {
                     try {
                         StartHandle startHandle = stop(user, cause, SubMonitor
                             .convert(new NullProgressMonitor()));
                         // FIXME Race Condition: startHandle was not added yet
                         // in case of cancellation
                         resultingHandles.add(startHandle);
                         log.debug("Added " + startHandle
                             + " to resulting handles.");
                         doneSignal.countDown();
                     } catch (CancellationException e) {
                         log.debug("User canceled the Stopping");
                         monitor.setCanceled(true);
                     } catch (InterruptedException e) {
                         log
                             .debug("Canceling because of an InterruptedException");
                         monitor.setCanceled(true);
                     }
                 }
             });
         }
         while (resultingHandles.size() != users.size() && !monitor.isCanceled()) {
             try {
                 // waiting for all startHandles
                 doneSignal.await(MILLISTOWAIT, TimeUnit.MILLISECONDS);
             } catch (InterruptedException e) {
                 log
                     .error("Stopping was interrupted. Not all users could successfully be stopped.");
             }
         }
         if (monitor.isCanceled()) {
             // Restart the already stopped users
             log
                 .debug("Monitor was canceled. Restarting already stopped users.");
             for (StartHandle startHandle : resultingHandles)
                 startHandle.start();
             throw new CancellationException();
         }
         return resultingHandles;
     }
 
     /**
      * Blocking method that asks the given user to halt all user-input and
      * returns a handle to be used when the user can start again.
      * 
      * @param user
      *            the participant who has to stop
      * @param cause
      *            the cause for stopping as it is displayed in the progress
      *            monitor
      * 
      * @param progress
      *            The caller is expected to call beginTask and done on the given
      *            SubMonitor
      * 
      * @noSWT This method mustn't be called from the SWT thread.
      * 
      * @blocking returning after the given user acknowledged the stop
      * 
      * @cancelable This method can be canceled by the user
      * 
      * @throws CancellationException
      * @throws InterruptedException
      */
     public StartHandle stop(User user, String cause, final SubMonitor progress)
         throws CancellationException, InterruptedException {
 
         if (sharedProject == null)
             throw new IllegalStateException(
                 "Stop cannot be called without a shared project");
 
         // Creating StopActivity for asking user to stop
         final StopActivity stopActivity = new StopActivity(sharedProject
             .getLocalUser().getJID().toString(), sharedProject.getLocalUser()
             .getJID(), user.getJID(), Type.LOCKREQUEST, State.INITIATED);
 
         StartHandle handle = generateStartHandle(stopActivity);
         addStartHandle(handle);
 
         // Short cut if affected user is local
         if (user.isLocal()) {
             Util.runSafeSWTSync(log, new Runnable() {
                 public void run() {
                     lockProject(true);
                 }
             });
             return handle;
         }
 
         StopActivity expectedAck = stopActivity.generateAcknowledgment(user
             .getJID().toString());
         expectedAcknowledgments.add(expectedAck);
 
         Util.runSafeSWTSync(log, new Runnable() {
             public void run() {
                 fireActivity(stopActivity);
             }
         });
 
         // Block until user acknowledged
         log.debug("Waiting for acknowledgment " + Util.prefix(user.getJID()));
         reentrantLock.lock();
         try {
             while (expectedAcknowledgments.contains(expectedAck)
                 && !progress.isCanceled()) {
                 if (acknowledged.await(MILLISTOWAIT, TimeUnit.MILLISECONDS)) {
                     continue; /*
                                * Used to make FindBugs happy that we don't do
                                * anything if we are woken up
                                */
                 }
             }
             if (expectedAcknowledgments.contains(expectedAck)) {
                 log.warn("No acknowlegment arrived, gave up waiting");
                 expectedAcknowledgments.remove(expectedAck);
                 handle.start();
                 throw new CancellationException();
             }
             log.debug("Acknowledgment arrived " + Util.prefix(user.getJID()));
         } catch (InterruptedException e) {
             handle.start();
             throw new InterruptedException();
         } finally {
             reentrantLock.unlock();
         }
         return handle;
     }
 
     /**
      * The goal of this method is to ensure that the local user cannot cause any
      * editing activities (FileActivities and TextEditActivities).
      * 
      * @param lock
      *            if true the project gets locked, else it gets unlocked
      */
     protected void lockProject(boolean lock) {
         for (Blockable blockable : blockables) {
             if (lock)
                 blockable.block();
             else
                 blockable.unblock();
         }
     }
 
     /**
      * Unlocks project without sending an acknowledgment if there don't exist
      * any more startHandles.
      * 
      * @return true if the affected user is unlocked afterwards
      */
     protected boolean executeUnlock(StartHandle startHandle) {
 
         if (!startHandle.getUser().isLocal())
             throw new IllegalArgumentException(
                 "ExecuteUnlock may only be called with a StartHandle for the local user");
 
         if (!removeStartHandle(startHandle)) {
             /*
              * Ok if the local user was the initiator of the stop. If not
              * something is wrong.
              */
             log.debug(startHandle
                 + " couldn't be removed because it doesn't exist any more.");
         }
 
         int remainingHandles = getStartHandles(sharedProject.getLocalUser())
             .size();
         if (remainingHandles > 0) {
             log.debug(remainingHandles + " startHandles remaining.");
             return false;
         }
         Util.runSafeSWTSync(log, new Runnable() {
             public void run() {
                 lockProject(false);
             }
         });
         return true;
     }
 
     /**
      * Sends an initiated unlock request.
      * 
      * @param handle
      *            the startHandle whose start() initiated the unlocking
      */
     public void initiateUnlock(StartHandle handle) {
         if (sharedProject == null)
             throw new IllegalStateException(
                 "Cannot initiate unlock without a shared project");
 
         // short cut for local user
         if (handle.getUser().isLocal()) {
             executeUnlock(handle);
             return;
         }
 
         startsToBeAcknowledged.put(handle.getHandleID(), handle);
 
         final StopActivity activity = new StopActivity(sharedProject
             .getLocalUser().getJID().toString(), sharedProject.getLocalUser()
             .getJID(), handle.getUser().getJID(), Type.UNLOCKREQUEST,
             State.INITIATED, handle.getHandleID());
 
         Util.runSafeSWTSync(log, new Runnable() {
             public void run() {
                 fireActivity(activity);
             }
         });
     }
 
     /**
      * {@inheritDoc}
      */
     public void addActivityListener(IActivityListener listener) {
         activityListeners.add(listener);
     }
 
     /**
      * {@inheritDoc}
      */
     public void exec(IActivity activity) {
         activity.dispatch(activityReceiver);
     }
 
     /**
      * {@inheritDoc}
      */
     public void removeActivityListener(IActivityListener listener) {
         activityListeners.remove(listener);
     }
 
     public void fireActivity(StopActivity stopActivity) {
 
         User recipient = sharedProject.getUser(stopActivity.getRecipient());
         if (recipient == null)
             throw new IllegalArgumentException("StopActivity contains"
                 + " recipient which already left: " + stopActivity);
 
         sharedProject.sendActivity(recipient, stopActivity);
     }
 
     /**
      * Adds a StartHandle to startHandles, which maps a user to a list of
      * StartHandles. These Lists are created lazily.
      */
     public void addStartHandle(StartHandle startHandle) {
         getStartHandles(startHandle.getUser()).add(startHandle);
     }
 
     /**
      * Removes a StartHandle from startHandles. If the list for user is empty
      * then the user is removed from startHandles.
      * 
      * @return false if the given startHandle didn't exist in Map, true
      *         otherwise
      */
     public boolean removeStartHandle(StartHandle startHandle) {
         return getStartHandles(startHandle.getUser()).remove(startHandle);
     }
 
     public synchronized List<StartHandle> getStartHandles(User user) {
 
         List<StartHandle> result = startHandles.get(user);
         if (result == null) {
             result = new CopyOnWriteArrayList<StartHandle>();
             startHandles.put(user, result);
         }
         return result;
     }
 
     public StartHandle generateStartHandle(StopActivity stopActivity) {
         User user = sharedProject.getUser(stopActivity.getUser());
         return new StartHandle(user, this, stopActivity.getActivityID());
     }
 
     public void addBlockable(Blockable stoppable) {
         blockables.add(stoppable);
     }
 
     public void removeBlockable(Blockable stoppable) {
         blockables.remove(stoppable);
     }
 
     public void dispose() {
         sharedProjectObservable.remove(sharedProjectObserver);
     }
 
     protected void reset() {
         lockProject(false);
         startHandles.clear();
         expectedAcknowledgments.clear();
     }
 }
