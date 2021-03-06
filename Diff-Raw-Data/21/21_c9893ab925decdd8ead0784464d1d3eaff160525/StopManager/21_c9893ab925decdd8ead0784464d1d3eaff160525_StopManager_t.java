 package de.fu_berlin.inf.dpp.synchronize;
 
 import java.util.Collections;
 import java.util.HashMap;
 import java.util.HashSet;
 import java.util.LinkedList;
 import java.util.List;
 import java.util.Set;
 import java.util.concurrent.TimeUnit;
 import java.util.concurrent.locks.Condition;
 import java.util.concurrent.locks.Lock;
 import java.util.concurrent.locks.ReentrantLock;
 
 import org.apache.log4j.Logger;
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
 
     // waits MILLISTOWAIT ms until giving up waiting for expected acknowledgment
     protected final int MILLISTOWAIT = 5000;
 
     private final List<IActivityListener> activityListeners = new LinkedList<IActivityListener>();
 
     protected List<Blockable> blockables = new LinkedList<Blockable>();
 
     protected ISharedProject sharedProject;
 
     SharedProjectObservable sharedProjectObservable;
 
     /**
      * Maps a User to a List of his StartHandles. Never touch this directly, use
      * the add and remove methods.
      */
     private HashMap<User, List<StartHandle>> startHandles = new HashMap<User, List<StartHandle>>();
 
     // blocking mechanism
     protected Lock reentrantLock = new ReentrantLock();
     protected final Condition acknowledged = reentrantLock.newCondition();
 
     /**
      * for every initiated StopActivity (type: LockRequest) there is one
      * acknowledgment expected
      */
     protected Set<StopActivity> expectedAcknowledgments = Collections
         .synchronizedSet(new HashSet<StopActivity>());
 
     protected ValueChangeListener<SharedProject> sharedProjectObserver = new ValueChangeListener<SharedProject>() {
         public void setValue(SharedProject sharedProject) {
 
             if (sharedProject == null) {
                 StopManager.this.sharedProject.getSequencer().removeProvider(
                     StopManager.this);
                 StopManager.this.sharedProject = null;
                 return;
             }
 
             StopManager.this.sharedProject = sharedProject;
             sharedProject.getSequencer().addProvider(StopManager.this);
         }
     };
 
     public StopManager(SharedProjectObservable observable) {
 
         this.sharedProjectObservable = observable;
         observable.add(sharedProjectObserver);
     }
 
     protected final IActivityReceiver activityReceiver = new AbstractActivityReceiver() {
         /**
          * @return true if the activity is expected and causes an effect, false
          *         otherwise
          */
         @Override
         public boolean receive(final StopActivity stopActivity) {
 
             if (sharedProject == null)
                 throw new IllegalStateException(
                     "cannot receive StopActivities without a shared project");
 
             if (stopActivity.getType() == Type.LOCKREQUEST) {
 
                 /*
                  * local user locks his project and adds a startHandle so he
                  * knows he is locked. Then he acknowledges
                  */
                 if (stopActivity.getState() == State.INITIATED
                     && sharedProject.getParticipant(stopActivity.getUser())
                         .isLocal()) {
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
                     return true;
                 }
                 if (stopActivity.getState() == State.ACKNOWLEDGED
                     && sharedProject
                         .getParticipant(stopActivity.getInitiator()).isLocal()) {
                     if (!expectedAcknowledgments.contains(stopActivity)) {
                         log.warn("received unexpected StopActivity: "
                             + stopActivity);
                         return false;
                     }
 
                     // it has to be removed from the expected ack list
                     // because it already arrived
                     if (expectedAcknowledgments.remove(stopActivity)) {
                         reentrantLock.lock();
                         acknowledged.signalAll();
                         reentrantLock.unlock();
                         return true;
                     } else {
                         log
                             .warn("received unexpected StopActivity acknowledgement: "
                                 + stopActivity);
                         return false;
                     }
                 }
             }
 
             if (stopActivity.getType() == Type.UNLOCKREQUEST) {
                 if (stopActivity.getState() == State.INITIATED
                     && sharedProject.getParticipant(stopActivity.getUser())
                         .isLocal()) {
                     /*
                      * unlocks project without acknowledgment if there don't
                      * exist any more startHandles
                      */
                     removeStartHandle(generateStartHandle(stopActivity));
                     if (!noStartHandlesFor(sharedProject.getLocalUser()))
                         return true;
                     Util.runSafeSWTSync(log, new Runnable() {
                         public void run() {
                             lockProject(false);
                         }
                     });
                     return true;
                 }
             }
             return false;
         }
     };
 
     /**
      * Blocking method that asks the given user to halt all user-input and
      * returns a handle to be used when the user can start again.
      * 
      * @noSWT This method mustn't be called from the SWT thread.
      * 
      * @param user
      *            the participant who has to stop
      */
     public StartHandle stop(User user) {
 
         if (sharedProject == null)
             throw new IllegalStateException(
                 "stop cannot be called without a shared project");
 
        // creating StopActivity for asking user to stop
         StopActivity stopActivity = new StopActivity(sharedProject
             .getLocalUser().getJID().toString(), sharedProject.getLocalUser()
             .getJID(), user.getJID(), Type.LOCKREQUEST, State.INITIATED);
 
        StartHandle handle = generateStartHandle(stopActivity);

        // short cut if affected user is local
        if (user.isLocal()) {
            addStartHandle(handle);
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
 
         fireActivity(stopActivity);
 
         // block until user acknowledged
         reentrantLock.lock();
         log.debug("Waiting for acknowledgment");
         try {
             long startTime = System.currentTimeMillis();
             while (expectedAcknowledgments.contains(expectedAck)
                 && System.currentTimeMillis() - startTime < MILLISTOWAIT) {
                 acknowledged.await(MILLISTOWAIT, TimeUnit.MILLISECONDS);
             }
             if (expectedAcknowledgments.contains(expectedAck)) {
                 log.warn("No acknowlegment arrived, gave up waiting");
                 expectedAcknowledgments.remove(expectedAck);
                 return null;
             }
             log.debug("Acknowledgment arrived");
 
         } catch (InterruptedException e) {
             Thread.currentThread().interrupt();
         } finally {
             reentrantLock.unlock();
         }
 
        addStartHandle(handle);
         return handle;
     }
 
     /**
      * The goal of this method is to ensure that the local user cannot cause any
      * editing activities (FileActivities and TextEditActivities).
      * 
      * @param lock
      *            if true the project gets locked, else it gets unlocked (if
      *            local user is driver)
      */
     protected void lockProject(boolean lock) {
         // don't unlock if local user is observer
         if (!lock && !sharedProject.isDriver())
             return;
         for (Blockable blockable : blockables) {
             if (lock)
                 blockable.block();
             else
                 blockable.unblock();
         }
     }
 
     /**
      * sends an initiated unlock request
      * 
      * @param handle
      *            the startHandle whose start() initiated the unlocking
      */
     public void initiateUnlock(StartHandle handle) {
         if (sharedProject == null)
             throw new IllegalStateException(
                 "cannot initiate unlock without a shared project");
 
         IActivity stopActivity = new StopActivity(sharedProject.getLocalUser()
             .getJID().toString(), sharedProject.getLocalUser().getJID(), handle
             .getUser().getJID(), Type.UNLOCKREQUEST, State.INITIATED, handle
             .getHandleID());
         fireActivity(stopActivity);
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
 
     public void fireActivity(IActivity stopActivity) {
         /*
          * TODO Now StopActivities are sent to everybody, it would be better if
          * it was sent only to the affected participant.
          */
         for (IActivityListener listener : activityListeners) {
             listener.activityCreated(stopActivity); // informs ActivitySequencer
         }
     }
 
     /**
      * Adds a StartHandle to startHandles, which maps a user to a list of
      * StartHandles. These Lists are created lazily.
      */
     public void addStartHandle(StartHandle startHandle) {
         User user = startHandle.getUser();
         List<StartHandle> handleList = startHandles.get(user);
         if (handleList == null) {
             handleList = new LinkedList<StartHandle>();
             startHandles.put(user, handleList);
         }
         handleList.add(startHandle);
     }
 
     /**
      * Removes a StartHandle from startHandles. If the list for user is empty
      * then the user is removed from startHandles.
      */
     public void removeStartHandle(StartHandle startHandle) {
         User user = startHandle.getUser();
         List<StartHandle> handleList = startHandles.get(user);
         if (handleList == null)
             return; // nothing to do
         handleList.remove(startHandle);
         if (handleList.isEmpty())
             startHandles.remove(user);
     }
 
     /**
      * @return true if there exist no StartHandles for the given user
      */
     public boolean noStartHandlesFor(User user) {
         return !startHandles.containsKey(user);
     }
 
     public StartHandle generateStartHandle(StopActivity stopActivity) {
         User user = sharedProject.getParticipant(stopActivity.getUser());
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
 
 }
