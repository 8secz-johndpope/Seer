 /*
  * DPP - Serious Distributed Pair Programming
  * (c) Freie Universitaet Berlin - Fachbereich Mathematik und Informatik - 2006
  * (c) Riad Djemili - 2006
  *
  * This program is free software; you can redistribute it and/or modify
  * it under the terms of the GNU General Public License as published by
  * the Free Software Foundation; either version 1, or (at your option)
  * any later version.
  *
  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  * GNU General Public License for more details.
  *
  * You should have received a copy of the GNU General Public License
  * along with this program; if not, write to the Free Software
  * Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
  */
 package de.fu_berlin.inf.dpp.net.internal;
 
 import java.util.ArrayList;
 import java.util.Collections;
 import java.util.HashMap;
 import java.util.LinkedHashMap;
 import java.util.LinkedList;
 import java.util.List;
 import java.util.Map;
 import java.util.PriorityQueue;
 import java.util.Timer;
 import java.util.TimerTask;
 import java.util.Map.Entry;
 import java.util.concurrent.BlockingQueue;
 import java.util.concurrent.ConcurrentHashMap;
 import java.util.concurrent.LinkedBlockingQueue;
 
 import org.apache.log4j.Logger;
 
 import de.fu_berlin.inf.dpp.User;
 import de.fu_berlin.inf.dpp.activities.SPathDataObject;
 import de.fu_berlin.inf.dpp.activities.serializable.IActivityDataObject;
 import de.fu_berlin.inf.dpp.activities.serializable.TextEditActivityDataObject;
 import de.fu_berlin.inf.dpp.activities.serializable.TextSelectionActivityDataObject;
 import de.fu_berlin.inf.dpp.activities.serializable.ViewportActivityDataObject;
 import de.fu_berlin.inf.dpp.net.ITransmitter;
 import de.fu_berlin.inf.dpp.net.JID;
 import de.fu_berlin.inf.dpp.net.TimedActivityDataObject;
 import de.fu_berlin.inf.dpp.net.business.DispatchThreadContext;
 import de.fu_berlin.inf.dpp.project.ISharedProject;
 import de.fu_berlin.inf.dpp.util.AutoHashMap;
 import de.fu_berlin.inf.dpp.util.Util;
 
 /**
  * The ActivitySequencer is responsible for making sure that activityDataObjects
  * are sent and received in the right order.
  * 
  * TODO Remove the dependency of this class on the ConcurrentDocumentManager,
  * push all responsibility up a layer into the SharedProject
  * 
  * @author rdjemili
  * @author coezbek
  * @author marrin
  */
 public class ActivitySequencer {
 
     private static Logger log = Logger.getLogger(ActivitySequencer.class
         .getName());
 
     /**
      * Number of milliseconds between each flushing and sending of outgoing
      * activityDataObjects, and testing for too old queued incoming
      * activityDataObjects.
      */
     protected static final int MILLIS_UPDATE = 1000;
 
     public static class DataObjectQueueItem {
 
         public final List<User> recipients;
         public final IActivityDataObject activityDataObject;
 
         public DataObjectQueueItem(List<User> recipients,
             IActivityDataObject activityDataObject) {
             this.recipients = recipients;
             this.activityDataObject = activityDataObject;
         }
 
         public DataObjectQueueItem(User host,
             IActivityDataObject transformedActivity) {
             this(Collections.singletonList(host), transformedActivity);
         }
     }
 
     /** Buffer for outgoing activityDataObjects. */
     protected final BlockingQueue<DataObjectQueueItem> outgoingQueue = new LinkedBlockingQueue<DataObjectQueueItem>();
 
     /**
      * A priority queue for timed activityDataObjects.
      * 
      * TODO "Timestamps" are treated more like consecutive sequence numbers, so
      * may be all names and documentation should be changed to reflect this.
      */
     protected class ActivityQueue {
 
         /**
          * How long to wait until ignore missing activityDataObjects in
          * milliseconds.
          */
         protected static final long ACTIVITY_TIMEOUT = 30 * 1000;
 
         /**
          * Sequence numbers for outgoing and incoming activityDataObjects start
          * with this value.
          */
         protected static final int FIRST_SEQUENCE_NUMBER = 0;
 
         /** This {@link ActivityQueue} is for this user. */
         protected final JID jid;
 
         /** Sequence number this user sends next. */
         protected int nextSequenceNumber = FIRST_SEQUENCE_NUMBER;
 
         /** Sequence number expected from the next activityDataObject. */
         protected int expectedSequenceNumber = FIRST_SEQUENCE_NUMBER;
 
         /**
          * Oldest local timestamp for the queued activityDataObjects or 0 if
          * there are no activityDataObjects queued.
          * 
          * TODO Is this documentation correct?
          */
         protected long oldestLocalTimestamp = Long.MAX_VALUE;
 
         /** Queue of activityDataObjects received. */
         protected final PriorityQueue<TimedActivityDataObject> queuedActivities = new PriorityQueue<TimedActivityDataObject>();
 
         /**
          * History of activityDataObjects sent.
          * 
          * TODO Not really used at the moment. File creation activityDataObjects
          * don't store the content at the time they were sent, so they can't be
          * re-send.
          */
         protected final List<TimedActivityDataObject> history = new LinkedList<TimedActivityDataObject>();
 
         public ActivityQueue(JID jid) {
             this.jid = jid;
         }
 
         /**
          * Create a {@link TimedActivityDataObject} and add it to the history of
          * created activityDataObjects.
          */
         public TimedActivityDataObject createTimedActivity(
             IActivityDataObject activityDataObject) {
 
             TimedActivityDataObject result = new TimedActivityDataObject(
                 activityDataObject, localJID, nextSequenceNumber++);
             history.add(result);
             return result;
         }
 
         /**
          * Add a received activityDataObject to the priority queue.
          */
         public void add(TimedActivityDataObject activity) {
 
             // Ignore activityDataObjects with sequence numbers we have already
             // seen or
             // don't expect anymore.
             if (activity.getSequenceNumber() < expectedSequenceNumber) {
                 log.warn("Ignored activityDataObject. Expected Nr. "
                     + expectedSequenceNumber + ", got: " + activity);
                 return;
             }
 
             long now = System.currentTimeMillis();
             activity.setLocalTimestamp(now);
             if (oldestLocalTimestamp == Long.MAX_VALUE) {
                 oldestLocalTimestamp = now;
             }
 
             // Log debug message if there are queued activityDataObjects.
             int size = queuedActivities.size();
             if (size > 0) {
                 log.debug("For " + jid + " there are " + size
                     + " activityDataObjects queued. First queued: "
                     + queuedActivities.peek() + ", expected nr: "
                     + expectedSequenceNumber);
             }
 
             queuedActivities.add(activity);
         }
 
         /**
          * Set {@link ActivityQueue#oldestLocalTimestamp} to the oldest local
          * timestamp of the queued activityDataObjects or 0 if the queue is
          * empty.
          */
         protected void updateOldestLocalTimestamp() {
             oldestLocalTimestamp = Long.MAX_VALUE;
             for (TimedActivityDataObject timedActivity : queuedActivities) {
                 long localTimestamp = timedActivity.getLocalTimestamp();
                 if (localTimestamp < oldestLocalTimestamp) {
                     oldestLocalTimestamp = localTimestamp;
                 }
             }
         }
 
         /**
          * @return The next activityDataObject if there is one and it carries
          *         the expected sequence number, otherwise <code>null</code>.
          */
         public TimedActivityDataObject removeNext() {
 
             if (!queuedActivities.isEmpty()
                 && queuedActivities.peek().getSequenceNumber() == expectedSequenceNumber) {
 
                 expectedSequenceNumber++;
                 TimedActivityDataObject result = queuedActivities.remove();
                 updateOldestLocalTimestamp();
                 return result;
             }
             return null;
         }
 
         /**
          * Check for activityDataObjects that are missing for more than
          * {@link ActivityQueue#ACTIVITY_TIMEOUT} milliseconds or twice as long
          * if there is a file transfer for the JID of this queue, and skip an
          * expected sequence number.
          */
         protected void checkForMissingActivities() {
 
             if (queuedActivities.isEmpty())
                 return;
 
             int firstQueuedSequenceNumber = queuedActivities.peek()
                 .getSequenceNumber();
 
             // Discard all activityDataObjects which we are no longer waiting
             // for
             while (firstQueuedSequenceNumber < expectedSequenceNumber) {
 
                 TimedActivityDataObject activity = queuedActivities.remove();
 
                 log.error("Expected activityDataObject #"
                     + expectedSequenceNumber
                     + " but an older activityDataObject is still in the queue"
                     + " and will be dropped (#" + firstQueuedSequenceNumber
                     + "): " + activity);
 
                 if (queuedActivities.isEmpty())
                     return;
 
                 firstQueuedSequenceNumber = queuedActivities.peek()
                     .getSequenceNumber();
             }
 
             if (firstQueuedSequenceNumber == expectedSequenceNumber)
                 return; // Next Activity is ready to be executed
 
             /*
              * Last case: firstQueuedSequenceNumber > expectedSequenceNumber
              * 
              * -> Check for time-out
              */
             long age = System.currentTimeMillis() - oldestLocalTimestamp;
             if (age > ACTIVITY_TIMEOUT) {
                 if (age < ACTIVITY_TIMEOUT * 2) {
                     // Early exit if there is a file transfer running.
                     if (transferManager.isReceiving(jid)) {
                         // TODO SS need to be more flexible
                         return;
                     }
                 }
 
                 int skipCount = firstQueuedSequenceNumber
                     - expectedSequenceNumber;
                 log.warn("Gave up waiting for activityDataObject # "
                     + expectedSequenceNumber
                     + ((skipCount == 1) ? "" : " to "
                         + (firstQueuedSequenceNumber - 1)) + " from " + jid);
                 expectedSequenceNumber = firstQueuedSequenceNumber;
                 updateOldestLocalTimestamp();
             }
         }
 
         /**
          * Returns all activityDataObjects which can be executed. If there are
          * none, an empty List is returned.
          * 
          * This method also checks for missing activityDataObjects and discards
          * out-dated or unwanted activityDataObjects.
          */
         public List<TimedActivityDataObject> removeActivities() {
 
             checkForMissingActivities();
 
             ArrayList<TimedActivityDataObject> result = new ArrayList<TimedActivityDataObject>();
 
             TimedActivityDataObject activity;
             while ((activity = removeNext()) != null) {
                 result.add(activity);
             }
 
             return result;
         }
     }
 
     /**
      * This class manages a {@link ActivityQueue} for each other user of a
      * session.
      */
     protected class ActivityQueuesManager {
         protected final Map<JID, ActivityQueue> jid2queue = new ConcurrentHashMap<JID, ActivityQueue>();
 
         /**
          * Get the {@link ActivityQueue} for the given {@link JID}.
          * 
          * If there is no queue for the {@link JID}, a new one is created.
          * 
          * @param jid
          *            {@link JID} to get the queue for.
          * @return the {@link ActivityQueue} for the given {@link JID}.
          */
         protected synchronized ActivityQueue getActivityQueue(JID jid) {
             ActivityQueue queue = jid2queue.get(jid);
             if (queue == null) {
                 queue = new ActivityQueue(jid);
                 jid2queue.put(jid, queue);
             }
             return queue;
         }
 
         /**
          * @see ActivitySequencer#createTimedActivities(JID, List)
          */
         public synchronized List<TimedActivityDataObject> createTimedActivities(
             JID recipient, List<IActivityDataObject> activityDataObjects) {
 
             ArrayList<TimedActivityDataObject> result = new ArrayList<TimedActivityDataObject>(
                 activityDataObjects.size());
             ActivityQueue queue = getActivityQueue(recipient);
             for (IActivityDataObject activityDataObject : activityDataObjects) {
                 result.add(queue.createTimedActivity(activityDataObject));
             }
             return result;
         }
 
         /**
          * Adds a received {@link TimedActivityDataObject}. There must be a
          * source set on the activityDataObject.
          * 
          * @param timedActivity
         *            to add to the qeues.
          * 
          * @throws IllegalArgumentException
          *             if the source of the activityDataObject is
          *             <code>null</code>.
          */
         public void add(TimedActivityDataObject timedActivity) {
             getActivityQueue(timedActivity.getSender()).add(timedActivity);
         }
 
         /**
          * Remove the queue for a given user.
          * 
          * @param jid
          *            of the user to remove.
          */
         public void removeQueue(JID jid) {
             jid2queue.remove(jid);
         }
 
         /**
          * @return all activityDataObjects that can be executed. If there are
          *         none, an empty List is returned.
          * 
          *         This method also checks for missing activityDataObjects and
          *         discards out-dated or unwanted activityDataObjects.
          */
         public List<TimedActivityDataObject> removeActivities() {
             ArrayList<TimedActivityDataObject> result = new ArrayList<TimedActivityDataObject>();
             for (ActivityQueue queue : jid2queue.values()) {
                 result.addAll(queue.removeActivities());
             }
             return result;
         }
 
         /**
          * @see ActivitySequencer#getActivityHistory(JID, int, boolean)
          */
         public List<TimedActivityDataObject> getHistory(JID user,
             int fromSequenceNumber, boolean andUp) {
 
             LinkedList<TimedActivityDataObject> result = new LinkedList<TimedActivityDataObject>();
             for (TimedActivityDataObject activity : getActivityQueue(user).history) {
                 if (activity.getSequenceNumber() >= fromSequenceNumber) {
                     result.add(activity);
                     if (!andUp) {
                         break;
                     }
                 }
             }
             return result;
         }
 
         /**
          * @see ActivitySequencer#getExpectedSequenceNumbers()
          */
         public Map<JID, Integer> getExpectedSequenceNumbers() {
             HashMap<JID, Integer> result = new HashMap<JID, Integer>();
             for (ActivityQueue queue : jid2queue.values()) {
                 if (queue.queuedActivities.size() > 0) {
                     result.put(queue.jid, queue.expectedSequenceNumber);
                 }
             }
             return result;
         }
     }
 
     protected final ActivityQueuesManager incomingQueues = new ActivityQueuesManager();
 
     /**
      * Whether this AS currently sends or receives events
      */
     protected boolean started = false;
 
     protected Timer flushTimer;
 
     protected final ISharedProject sharedProject;
 
     protected final ITransmitter transmitter;
 
     protected final JID localJID;
 
     protected final DataTransferManager transferManager;
 
     protected final DispatchThreadContext dispatchThread;
 
     public ActivitySequencer(ISharedProject sharedProject,
         ITransmitter transmitter, DataTransferManager transferManager,
         DispatchThreadContext threadContext) {
 
         this.dispatchThread = threadContext;
         this.sharedProject = sharedProject;
         this.transmitter = transmitter;
         this.transferManager = transferManager;
 
         this.localJID = sharedProject.getLocalUser().getJID();
     }
 
     /**
      * Start periodical flushing and sending of outgoing activityDataObjects and
      * checking for received activityDataObjects that are queued for too long.
      * 
      * @throws IllegalStateException
      *             if this method is called on an already started
      *             {@link ActivitySequencer}
      * 
      * @see #stop()
      */
     public void start() {
 
         if (started) {
             throw new IllegalStateException();
         }
 
         this.flushTimer = new Timer(true);
 
         started = true;
 
         this.flushTimer.schedule(new TimerTask() {
             @Override
             public void run() {
                 Util.runSafeSync(log, new Runnable() {
                     public void run() {
                         flushTask();
                     }
                 });
             }
 
             private void flushTask() {
                 // Just to assert that after stop() no task is executed anymore
                 if (!started)
                     return;
 
                 List<DataObjectQueueItem> activities = new ArrayList<DataObjectQueueItem>(
                     outgoingQueue.size());
                 outgoingQueue.drainTo(activities);
 
                 Map<User, List<IActivityDataObject>> toSend = AutoHashMap
                     .getListHashMap();
 
                 for (DataObjectQueueItem item : activities) {
                     for (User recipient : item.recipients) {
                         toSend.get(recipient).add(item.activityDataObject);
                     }
                 }
 
                 for (Entry<User, List<IActivityDataObject>> e : toSend
                     .entrySet()) {
                     sendActivities(e.getKey(), optimize(e.getValue()));
                 }
 
                 /*
                  * Periodically execQueues() because waiting activityDataObjects
                  * might have timed-out
                  */
                 dispatchThread.executeAsDispatch(new Runnable() {
                     public void run() {
                         execQueue();
                     }
                 });
             }
 
             /**
              * Sends given activityDataObjects to given recipient.
              * 
              * @private because this method must not be called from somewhere
              *          else than this TimerTask.
              * 
              * @throws IllegalArgumentException
              *             if the recipient is the local user or the
              *             activityDataObjects contain <code>null</code>.
              */
             private void sendActivities(User recipient,
                 List<IActivityDataObject> activityDataObjects) {
 
                 if (recipient.isLocal()) {
                     throw new IllegalArgumentException(
                         "Sending a message to the local user is not supported");
                 }
 
                 if (activityDataObjects.contains(null)) {
                     throw new IllegalArgumentException(
                         "Cannot send a null activityDataObject");
                 }
 
                 JID recipientJID = recipient.getJID();
                 List<TimedActivityDataObject> timedActivities = createTimedActivities(
                     recipientJID, activityDataObjects);
 
                 log.trace("Sending Activities to " + recipientJID + ": "
                     + timedActivities);
 
                 transmitter.sendTimedActivities(recipientJID, timedActivities);
             }
         }, 0, MILLIS_UPDATE);
     }
 
     /**
      * Stop periodical flushing and sending of outgoing activityDataObjects and
      * checking for received activityDataObjects that are queued for too long.
      * 
      * @see #start()
      */
     public void stop() {
         if (!started) {
             throw new IllegalStateException();
         }
 
         this.flushTimer.cancel();
         this.flushTimer = null;
 
         started = false;
     }
 
     /**
      * The central entry point for receiving Activities from the Network
      * component (either via message or data transfer, thus the following is
      * synchronized on the queue).
      * 
      * The activityDataObjects are sorted (in the queue) and executed in order.
      * 
      * If an activityDataObject is missing, this method just returns and queues
      * the given activityDataObject
      */
     public void exec(TimedActivityDataObject nextActivity) {
 
         assert nextActivity != null;
 
         incomingQueues.add(nextActivity);
 
         if (!started) {
             log.debug("Received activityDataObject but "
                 + "ActivitySequencer has not yet been started: "
                 + nextActivity);
             return;
         }
 
         execQueue();
     }
 
     /**
      * executes all activityDataObjects that are currently in the queue
      */
     protected void execQueue() {
         List<IActivityDataObject> activityDataObjects = new ArrayList<IActivityDataObject>();
         for (TimedActivityDataObject timedActivity : incomingQueues
             .removeActivities()) {
             activityDataObjects.add(timedActivity.getActivity());
         }
         sharedProject.exec(activityDataObjects);
     }
 
     /**
      * Sends the given activityDataObject to the given recipients.
      */
     public void sendActivity(List<User> recipients,
         final IActivityDataObject activityDataObject) {
 
         /**
          * Short cut all messages directed at local user
          */
         ArrayList<User> toSendViaNetwork = new ArrayList<User>();
         for (User user : recipients) {
             if (user.isLocal()) {
                 dispatchThread.executeAsDispatch(new Runnable() {
                     public void run() {
                         sharedProject.exec(Collections
                             .singletonList(activityDataObject));
                     }
                 });
             } else {
                 toSendViaNetwork.add(user);
             }
         }
 
         this.outgoingQueue.add(new DataObjectQueueItem(toSendViaNetwork,
             activityDataObject));
     }
 
     /**
      * Create {@link TimedActivityDataObject}s for the given recipient and
      * activityDataObjects and add them to the history of activityDataObjects
      * for the recipient.
      * 
      * This operation is thread safe, i.e. it is guaranteed that all
      * activityDataObjects get increasing, consecutive sequencer numbers, even
      * if this method is called from different threads concurrently.
      */
     protected List<TimedActivityDataObject> createTimedActivities(
         JID recipient, List<IActivityDataObject> activityDataObjects) {
         return incomingQueues.createTimedActivities(recipient,
             activityDataObjects);
     }
 
     /**
      * Get the activityDataObject history for given user and given timestamp.
      * 
      * If andUp is <code>true</code> all activityDataObjects that are equal or
      * greater than the timestamp are returned, otherwise just the
      * activityDataObject that matches the timestamp exactly.
      * 
      * If no activityDataObject matches the criteria an empty list is returned.
      */
     public List<TimedActivityDataObject> getActivityHistory(JID user,
         int fromSequenceNumber, boolean andUp) {
 
         return incomingQueues.getHistory(user, fromSequenceNumber, andUp);
     }
 
     /**
      * Get a {@link Map} that maps the {@link JID} of users with queued
      * activityDataObjects to the first missing sequence number.
      */
     public Map<JID, Integer> getExpectedSequenceNumbers() {
         return incomingQueues.getExpectedSequenceNumbers();
     }
 
     /**
      * This method tries to reduce the number of activityDataObjects transmitted
      * by removing activityDataObjects that would overwrite each other and
      * joining activityDataObjects that can be send as a single
      * activityDataObject.
      */
     private static List<IActivityDataObject> optimize(
         List<IActivityDataObject> toOptimize) {
 
         List<IActivityDataObject> result = new ArrayList<IActivityDataObject>(
             toOptimize.size());
 
         TextSelectionActivityDataObject selection = null;
         LinkedHashMap<SPathDataObject, ViewportActivityDataObject> viewport = new LinkedHashMap<SPathDataObject, ViewportActivityDataObject>();
 
         for (IActivityDataObject activityDataObject : toOptimize) {
 
             if (activityDataObject instanceof TextEditActivityDataObject) {
                 TextEditActivityDataObject textEdit = (TextEditActivityDataObject) activityDataObject;
                 textEdit = joinTextEdits(result, textEdit);
                 result.add(textEdit);
             } else if (activityDataObject instanceof TextSelectionActivityDataObject) {
                 selection = (TextSelectionActivityDataObject) activityDataObject;
             } else if (activityDataObject instanceof ViewportActivityDataObject) {
                 ViewportActivityDataObject viewActivity = (ViewportActivityDataObject) activityDataObject;
                 viewport.remove(viewActivity.getEditor());
                 viewport.put(viewActivity.getEditor(), viewActivity);
             } else {
                 result.add(activityDataObject);
             }
         }
 
         // only send one selection activityDataObject
         if (selection != null)
             result.add(selection);
 
         // Add only one viewport per editor
         for (Entry<SPathDataObject, ViewportActivityDataObject> entry : viewport
             .entrySet()) {
             result.add(entry.getValue());
         }
 
         assert !result.contains(null);
 
         return result;
     }
 
     private static TextEditActivityDataObject joinTextEdits(
         List<IActivityDataObject> result, TextEditActivityDataObject textEdit) {
         if (result.size() == 0) {
             return textEdit;
         }
 
         IActivityDataObject lastActivity = result.get(result.size() - 1);
         if (lastActivity instanceof TextEditActivityDataObject) {
             TextEditActivityDataObject lastTextEdit = (TextEditActivityDataObject) lastActivity;
 
             if (((lastTextEdit.getSource() == null) || lastTextEdit.getSource()
                 .equals(textEdit.getSource()))
                 && (textEdit.getOffset() == lastTextEdit.getOffset()
                     + lastTextEdit.getText().length())) {
                 result.remove(lastTextEdit);
                 textEdit = new TextEditActivityDataObject(lastTextEdit
                     .getSource(), lastTextEdit.getOffset(), lastTextEdit
                     .getText()
                     + textEdit.getText(), lastTextEdit.getReplacedText()
                     + textEdit.getReplacedText(), lastTextEdit.getEditor());
             }
         }
 
         return textEdit;
     }
 
     /**
      * Removes queued activityDataObjects from given user.
      * 
      * TODO Maybe remove outgoing activityDataObjects from
      * {@link #outgoingQueue} too!?
      * 
      * @param jid
      *            of the user that left.
      */
     public void userLeft(JID jid) {
         incomingQueues.removeQueue(jid);
     }
 }
