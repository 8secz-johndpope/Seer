 /* This file is part of VoltDB.
  * Copyright (C) 2008-2012 VoltDB Inc.
  *
  * VoltDB is free software: you can redistribute it and/or modify
  * it under the terms of the GNU General Public License as published by
  * the Free Software Foundation, either version 3 of the License, or
  * (at your option) any later version.
  *
  * VoltDB is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  * GNU General Public License for more details.
  *
  * You should have received a copy of the GNU General Public License
  * along with VoltDB.  If not, see <http://www.gnu.org/licenses/>.
  */
 
 package org.voltdb.iv2;
 
 import java.util.ArrayDeque;
 import java.util.Deque;
 import java.util.Iterator;
 import java.util.TreeMap;
 
 import org.voltcore.logging.VoltLogger;
 import org.voltdb.ReplicationRole;
 import org.voltdb.VoltDB;
 import org.voltdb.dtxn.TransactionState;
 import org.voltdb.exceptions.SerializableException;
 import org.voltdb.messaging.FragmentResponseMessage;
 import org.voltdb.messaging.FragmentTaskMessage;
 
 public class TransactionTaskQueue
 {
     protected static final VoltLogger hostLog = new VoltLogger("HOST");
 
     /*
      * A generic multi-part sentinel txnId used by DR. It blocks the queue until
      * the next multi-part fragment arrives. Since replica has to allow read
      * transactions, DR cannot use the original txnIds for sentinels.
      */
     public static final long GENERIC_MP_SENTINEL = Long.MIN_VALUE;
 
     final private SiteTaskerQueue m_taskQueue;
 
     /*
      * Task for a multi-part transaction that can't be executed because this is a replay
      * transaction and the sentinel has not been received.
      */
     private TransactionTask m_multiPartPendingSentinelReceipt = null;
 
     /*
      * Multi-part transactions create a backlog of tasks behind them. A queue is
      * created for each multi-part task to maintain the backlog until the next
      * multi-part task.
      *
      * DR uses m_drMPSentinelBacklog to queue additional sentinels than the one
      * currently in progress because DR uses GENERIC_MP_SENTINEL value for all
      * sentinels. When a DR multipart completes, another sentinel will be polled
      * from m_drMPSentinelBacklog and put in this map. It is impossible to have
      * an empty m_multipartBacklog while m_drMPSentinelBacklog has more
      * sentinels queued.
      */
     TreeMap<Long, Deque<TransactionTask>> m_multipartBacklog = new TreeMap<Long, Deque<TransactionTask>>();
 
     /**
      * Since DR uses GENERIC_MP_SENTINEL for all sentinels, they cannot
      * be differentiated. We need to keep a backlog of sentinels.
      */
     private Deque<Deque<TransactionTask>> m_drMPSentinelBacklog = new ArrayDeque<Deque<TransactionTask>>();
 
     TransactionTaskQueue(SiteTaskerQueue queue)
     {
         m_taskQueue = queue;
     }
 
     synchronized void offerMPSentinel(long txnId) {
         offerMPSentinelWithBacklog(txnId, null);
     }
 
     /**
      * Offer a sentinel with a existing backlog. The backlog could have already
      * queued single partition transactions in it.
      *
      * @param txnId
      * @param backlog An empty one will be created if this is null.
      */
     private void offerMPSentinelWithBacklog(long txnId, Deque<TransactionTask> backlog) {
         /*
          * If a backlog is provided, use it. Otherwise, create an empty one. The
          * provided backlog can only contain SP.
          */
         Deque<TransactionTask> deque = backlog;
         if (deque == null) {
             deque = new ArrayDeque<TransactionTask>();
         }
 
         if (m_multiPartPendingSentinelReceipt != null) {
             boolean mismatch = false;
             if (txnId == GENERIC_MP_SENTINEL) {
                 // no-op
             } else if (txnId != m_multiPartPendingSentinelReceipt.getTxnId()) {
                 mismatch = true;
             }
 
             /*
              * The fragment has arrived already, then this MUST be the correct
              * sentinel for this fragment.
              */
             if (mismatch) {
                 VoltDB.crashLocalVoltDB("Mismatch between replay sentinel txnid " +
                         txnId + " and next mutli-part fragment id " +
                         m_multiPartPendingSentinelReceipt.getTxnId(), false, null);
             }
 
             /*
              * Queue this in the back, you know nothing precedes it
              * since the sentinel is part of the single part stream
              */
             TransactionTask ts = m_multiPartPendingSentinelReceipt;
             m_multiPartPendingSentinelReceipt = null;
             // In case there are SP on the deque already. Add this MP to the front
             deque.addFirst(ts);
             m_multipartBacklog.put(txnId, deque);
             taskQueueOffer(ts);
         } else {
            if (!m_multipartBacklog.isEmpty() && m_multipartBacklog.firstKey() == txnId) {
                // Put the DR sentinel into the backlog
                assert(txnId == GENERIC_MP_SENTINEL);
                m_drMPSentinelBacklog.offer(new ArrayDeque<TransactionTask>());
            } else {
                /*
                 * The sentinel has arrived, but not the fragment. Stash it away
                 * and wait for the fragment. The presence of this handle
                 * pairing indicates that execution of the single part stream
                 * must block until the multi-part is satisfied
                 */
                m_multipartBacklog.put(txnId, deque);
            }
         }
     }
 
     /**
      * If necessary, stick this task in the backlog.
      * Many network threads may be racing to reach here, synchronize to
      * serialize queue order
      * @param task
      * @return true if this task was stored, false if not
      */
     synchronized boolean offer(TransactionTask task)
     {
         Iv2Trace.logTransactionTaskQueueOffer(task);
         boolean retval = false;
 
         final TransactionState ts = task.getTransactionState();
         boolean isDRReplicated = false;
         if (VoltDB.instance().getReplicationRole() == ReplicationRole.REPLICA && !ts.isReadOnly() &&
                 !(ts instanceof BorrowTransactionState)) {
             isDRReplicated = true;
         }
 
         // Single partitions never queue if empty
         // Multipartitions always queue
         // Fragments queue if they're not part of the queue head TXN ID
         // offer to SiteTaskerQueue if:
         // the queue was empty
         // the queue wasn't empty but the txn IDs matched
         if ((ts.isForReplay() || isDRReplicated) && (task instanceof FragmentTask)) {
             boolean hasSentinel = false;
             if (!m_multipartBacklog.isEmpty()) {
                 long txnId = m_multipartBacklog.firstKey();
                 if (ts.txnId == txnId) {
                     hasSentinel = true;
                 } else if (txnId == GENERIC_MP_SENTINEL && isDRReplicated) {
                     /*
                      * This branch is for DR. If the front of the deque is
                      * another fragment with a different txnId, then this MP
                      * doesn't have its sentinel yet. It needs to be queued
                      * until its sentinel arrives.
                      */
                     TransactionTask first = m_multipartBacklog.firstEntry().getValue().peekFirst();
                     if (first == null ||
                         (first.m_txn.isSinglePartition() || first.m_txn.txnId == ts.txnId)) {
                         hasSentinel = true;
                     }
                 }
             }
 
             /*
              * If this is a multi-partition transaction for replay then it can't
              * be inserted into the order for this partition until it's position is known
              * via the sentinel value. That value may not be known at the is point.
              */
             if (hasSentinel) {
                 /*
                  * This branch is for fragments that follow the first fragment during replay
                  * or first fragments during replay that follow the sentinel (hence the key exists)
                  * It is executed immediately either way, but it may need to be inserted into the backlog
                  * if it is the first fragment
                  */
                 Deque<TransactionTask> backlog = m_multipartBacklog.firstEntry().getValue();
 
                 TransactionTask first = backlog.peekFirst();
                 if (first != null) {
                     if (first.m_txn.txnId != task.getTxnId()) {
                         if (!first.m_txn.isSinglePartition()) {
                             hostLog.fatal("Head of backlog is " + first.m_txn.txnId +
                                           " but task is " + task.getClass().getCanonicalName() +
                                           " " + task.getTxnId());
                             VoltDB.crashLocalVoltDB(
                                     "If the first backlog task is multi-part, " +
                                     "but has a different transaction id it is a bug", true, null);
                         }
                         backlog.addFirst(task);
                     }
                 } else {
                     // The txnids match, don't need to put the second fragment at head of queue
                     //The first task is expected to be in the head of the queue
                     backlog.offer(task);
                 }
                 taskQueueOffer(task);
             }
             else {
                 /*
                  * We got an FragmentTask that didn't match the head of the the
                  * queue, but if we've received multiple sentinels, it may
                  * match one of the other deques.  Check to see.  This can
                  * happen if the CompleteTransactionTask has not yet been
                  * finished by the Site thread but we get the first fragment
                  * for the next MP transaction.
                  */
                 Deque<TransactionTask> backlog = m_multipartBacklog.get(task.getTxnId());
                 if (backlog != null) {
                     backlog.addFirst(task);
                 }
                 else {
                     /*
                      * This is the situation where the first fragment arrived before the sentinel.
                      * Its position in the order is not known.
                      * m_multiPartPendingSentinelReceipt should be null because the MP coordinator should only
                      * run one transaction at a time.
                      * It is not time to block single parts from executing because the order is not know,
                      * the only thing to do is stash it away for when the order is known from the sentinel
                      */
                     if (m_multiPartPendingSentinelReceipt != null) {
                         hostLog.fatal("\tBacklog length: " + m_multipartBacklog.size());
                         if (!m_multipartBacklog.isEmpty()) {
                             hostLog.fatal("\tBacklog first item: " + m_multipartBacklog.firstEntry().getValue().peekFirst());
                         }
                         hostLog.fatal("\tHave this one SentinelReceipt: " + m_multiPartPendingSentinelReceipt);
                         hostLog.fatal("\tAnd got this one, too: " + task);
                         VoltDB.crashLocalVoltDB(
                                 "There should be only one multipart pending sentinel receipt at a time", true, null);
                     }
                     m_multiPartPendingSentinelReceipt = task;
                 }
                 retval = true;
             }
         } else if (!m_multipartBacklog.isEmpty()) {
             /*
              * For DR multipart, only BorrowTransactionTask and
              * CompleteTransactionTask will enter this branch.
              */
             boolean isDRMPTask = false;
             if (((ts instanceof BorrowTransactionState) || !ts.isSinglePartition()) &&
                     !ts.isReadOnly() &&
                     m_multipartBacklog.firstKey() == GENERIC_MP_SENTINEL) {
                 isDRMPTask = true;
             }
 
             /*
              * This branch happens during regular execution when a multi-part is in progress.
              * The first task for the multi-part is the head of the queue, and all the single parts
              * are being queued behind it. The txnid check catches tasks that are part of the multi-part
              * and immediately queues them for execution.
              */
             if (!isDRMPTask && task.getTxnId() != m_multipartBacklog.firstKey())
             {
 
                 if (!ts.isSinglePartition()) {
                     /*
                      * In this case it is a multi-part fragment for the next transaction
                      * make sure it goes into the queue for that transaction
                      */
                     Deque<TransactionTask> d = m_multipartBacklog.get(task.getTxnId());
                     if (d == null) {
                         d = new ArrayDeque<TransactionTask>();
                         m_multipartBacklog.put(task.getTxnId(), d);
                     }
                     d.offerLast(task);
                 } else {
                     /*
                      * Note the use of last entry here. Each multi-part sentinel
                      * generates a backlog queue specific to the that
                      * multi-part. New single part transactions from the log go
                      * into the queue following the last received multi-part
                      * sentinel.
                      *
                      * It's possible to receive several sentinels with single
                      * part tasks mixed in before receiving the first fragment
                      * task from the MP coordinator for any of them so the
                      * backlog has to correctly preserve the order.
                      *
                      * During regular execution and not replay there should be
                      * at most one element in the multipart backlog, except for
                      * the kooky rollback corner case.
                      *
                      * For DR, if the m_drMPSentinelBacklog is not empty, it
                      * means that there are sentinels queued. The DR SP tasks
                      * should go in there instead of m_multipartBacklog.
                      */
                     if (m_drMPSentinelBacklog.isEmpty()) {
                         m_multipartBacklog.lastEntry().getValue().addLast(task);
                     } else {
                         m_drMPSentinelBacklog.getLast().addLast(task);
                     }
                     retval = true;
                 }
             }
             else {
                 taskQueueOffer(task);
             }
         }
         else {
             /*
              * Base case nothing queued nothing in progress
              * If the task is a multipart then put an entry in the backlog which
              * will act as a barrier for single parts, queuing them for execution after the
              * multipart
              */
             if (!task.getTransactionState().isSinglePartition()) {
                 Deque<TransactionTask> d = new ArrayDeque<TransactionTask>();
                 d.offer(task);
                 m_multipartBacklog.put(task.getTxnId(), d);
                 retval = true;
             }
             taskQueueOffer(task);
         }
         return retval;
     }
 
     // repair is used by MPI repair to inject a repair task into the
     // SiteTaskerQueue.  Before it does this, it unblocks the MP transaction
     // that may be running in the Site thread and causes it to rollback by
     // faking an unsuccessful FragmentResponseMessage.
     synchronized void repair(SiteTasker task)
     {
         m_taskQueue.offer(task);
         if (!m_multipartBacklog.isEmpty()) {
             // get head
             MpTransactionState txn =
                     (MpTransactionState)m_multipartBacklog.firstEntry().getValue().getFirst().getTransactionState();
             // inject poison pill
             FragmentTaskMessage dummy = new FragmentTaskMessage(0L, 0L, 0L, 0L, false, false, false);
             FragmentResponseMessage poison =
                 new FragmentResponseMessage(dummy, 0L); // Don't care about source HSID here
             // Provide a serializable exception so that the procedure runner sees
             // this as an Expected (allowed) exception and doesn't take the crash-
             // cluster-path.
             SerializableException forcedTermination = new SerializableException(
                     "Transaction rolled back by fault recovery or shutdown.");
             poison.setStatus(FragmentResponseMessage.UNEXPECTED_ERROR, forcedTermination);
             txn.offerReceivedFragmentResponse(poison);
         }
     }
 
     // Add a local method to offer to the SiteTaskerQueue so we have
     // a single point we can log through.
     private void taskQueueOffer(TransactionTask task)
     {
         Iv2Trace.logSiteTaskerQueueOffer(task);
         m_taskQueue.offer(task);
     }
 
     /**
      * Try to offer as many runnable Tasks to the SiteTaskerQueue as possible.
      * Currently just blocks on the next uncompleted multipartition transaction
      * @return
      */
     synchronized int flush()
     {
         int offered = 0;
         // check to see if head is done
         // then offer until the next MP or FragTask
         if (!m_multipartBacklog.isEmpty()) {
             Deque<TransactionTask> backlog = m_multipartBacklog.firstEntry().getValue();
             // We could have received just the sentinel for the first MP, which would
             // give us a non-empty multipartBacklog but an empty deque in the first key.
             // Do the null check on peek and fall through if we don't actually have the fragment.
             if (backlog.peek() != null && backlog.peek().getTransactionState().isDone()) {
                 // remove the completed MP txn
                 backlog.removeFirst();
                 m_multipartBacklog.remove(m_multipartBacklog.firstKey());
 
                 /*
                  * Drain all the single parts in that backlog queue
                  */
                 for (TransactionTask task : backlog) {
                     taskQueueOffer(task);
                     ++offered;
                 }
 
                 /*
                  * Now check to see if there was another multi-part queued after the one we just finished.
                  *
                  * This is a kooky corner case where a multi-part transaction can actually have multiple outstanding
                  * tasks. At first glance you would think that because the relationship is request response there
                  * can be only one outstanding task for a given multi-part transaction.
                  *
                  * That isn't true because a rollback can cause there to be a fragment task as well as a rollback
                  * task. The rollback is generated asynchronously by another partition.
                  * If we don't capture all the tasks right now then flush won't be called again because it is waiting
                  * for the complete transaction task that is languishing in the queue to do the flush post multi-part.
                  * It can't be called eagerly because that would destructively flush single parts as well.
                  *
                  * Iterate the queue to extract all tasks for the multi-part. The only time it isn't necessary
                  * to do this is when the first transaction in the queue is single part. This happens
                  * during replay when a sentinel creates the queue, but the multi-part task hasn't arrived yet.
                  */
                 if (!m_multipartBacklog.isEmpty() &&
                         !m_multipartBacklog.firstEntry().getValue().isEmpty() &&
                         !m_multipartBacklog.firstEntry().getValue().getFirst().getTransactionState().isSinglePartition()) {
                     Deque<TransactionTask> nextBacklog = m_multipartBacklog.firstEntry().getValue();
                     long txnId = m_multipartBacklog.firstKey();
                     Iterator<TransactionTask> iter = nextBacklog.iterator();
 
                     TransactionTask task = null;
                     boolean firstTask = true;
                     while (iter.hasNext()) {
                         task = iter.next();
                         if (task.getTxnId() == txnId) {
                             /*
                              * The old code always left the first fragment task
                              * in the head of the queue. The new code can probably do without it
                              * since the map contains the txnid, but I will
                              * leave it in to minimize change.
                              */
                             if (firstTask) {
                                 firstTask = false;
                             } else {
                                 iter.remove();
                             }
                             taskQueueOffer(task);
                             ++offered;
                         }
                     }
                 }
 
                 /*
                  * If there are any DR sentinels queued, release one.
                  */
                 if (!m_drMPSentinelBacklog.isEmpty()) {
                     offerMPSentinelWithBacklog(GENERIC_MP_SENTINEL, m_drMPSentinelBacklog.poll());
                 }
             }
         }
         return offered;
     }
 
     /**
      * How many Tasks are un-runnable?
      * @return
      */
     synchronized int size()
     {
         int size = 0;
         for (Deque<TransactionTask> d : m_multipartBacklog.values()) {
             size += d.size();
         }
         return size;
     }
 
     @Override
     public String toString()
     {
         StringBuilder sb = new StringBuilder();
         sb.append("TransactionTaskQueue:").append("\n");
         sb.append("\tSIZE: ").append(size());
         sb.append("\tHEAD: ").append(
                 m_multipartBacklog.firstEntry() != null ? m_multipartBacklog.firstEntry().getValue().peekFirst() : null);
         return sb.toString();
     }
 }
