 /**
  * Copyright (C) 2011 Akiban Technologies Inc.
  * This program is free software: you can redistribute it and/or modify
  * it under the terms of the GNU Affero General Public License, version 3,
  * as published by the Free Software Foundation.
  *
  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  * GNU Affero General Public License for more details.
  *
  * You should have received a copy of the GNU Affero General Public License
  * along with this program.  If not, see http://www.gnu.org/licenses.
  */
 package com.persistit;
 
 import java.util.ArrayList;
 import java.util.List;
 import java.util.concurrent.atomic.AtomicBoolean;
 
 import com.persistit.TimestampAllocator.Checkpoint;
 import com.persistit.exception.PersistitIOException;
 
 class CheckpointManager extends IOTaskRunnable {
 
     private final static long SHORT_DELAY = 500;
 
     private final static long FLUSH_CHECKPOINT_INTERVAL = 5000;
 
     private final List<Checkpoint> _outstandingCheckpoints = new ArrayList<Checkpoint>();
 
     private Checkpoint _lastCheckpoint = new Checkpoint(0, 0);
 
     private AtomicBoolean _closed = new AtomicBoolean();
 
     private AtomicBoolean _fastClose = new AtomicBoolean();
 
     CheckpointManager(final Persistit persistit) {
         super(persistit);
     }
 
     public void start() {
         _closed.set(false);
         start("CHECKPOINT_WRITER", FLUSH_CHECKPOINT_INTERVAL);
     }
 
     public void close(final boolean flush) {
         if (flush) {
             checkpoint();
         } else {
             _fastClose.set(true);
         }
         _closed.set(true);
     }
 
     Checkpoint checkpoint() {
         _persistit.getTimestampAllocator().forceCheckpoint();
         proposeCheckpoint();
         final Checkpoint checkpoint = _persistit.getCurrentCheckpoint();
         _persistit.flushBuffers(checkpoint.getTimestamp());
 
         while (true) {
             urgent();
             synchronized (this) {
                 if (!_outstandingCheckpoints.contains(checkpoint)) {
                     return checkpoint;
                 }
             }
             try {
                 Thread.sleep(SHORT_DELAY);
             } catch (InterruptedException ie) {
                 return null;
             }
         }
     }
 
     /**
      * Apply a checkpoint. If the checkpoint has already been applied, this
      * method does nothing. If it is a new checkpoint, this method adds it to
      * the outstanding checkpoint list.
      * 
      * @param newCheckpoint
      */
     void proposeCheckpoint() {
         synchronized (this) {
             final Checkpoint checkpoint = _persistit.getCurrentCheckpoint();
             if (checkpoint.getTimestamp() > _lastCheckpoint.getTimestamp()) {
                 _outstandingCheckpoints.add(checkpoint);
                 _lastCheckpoint = checkpoint;
                 _persistit.getLogBase().checkpointProposed.log(checkpoint);
             }
         }
     }
 
     /**
      * Attempt to find a new checkpoint that can be completed. Looks for the
      * earliest live transaction and the earliest dirty page; determines from
      * these whether a currently outstanding checkpoint is ready to complete.
      */
     void flushCheckpoint() {
         final Checkpoint validCheckpoint;
         synchronized (this) {
             validCheckpoint = findValidCheckpoint(_outstandingCheckpoints);
         }
         if (validCheckpoint != null) {
             try {
                 if (!_persistit.flushTransactionalCaches(validCheckpoint)) {
                     return;
                 }
                 _persistit.getJournalManager().writeCheckpointToJournal(validCheckpoint);
                synchronized (this) {
                    _outstandingCheckpoints.remove(validCheckpoint);
                }
             } catch (PersistitIOException e) {
                 _persistit.getLogBase().exception.log(e);
             }
         }
     }
 
     /**
      * Given a List of outstanding Checkpoints, find the latest one that is safe
      * to write and return it. If there is no safe Checkpoint, return
      * <code>null</code>
      * 
      * @param List
      *            of outstanding Checkpoint instances
      * @return The latest Checkpoint from the list that can be written, or
      *         <code>null</code> if there are none.
      */
     private Checkpoint findValidCheckpoint(final List<Checkpoint> outstandingCheckpoints) {
         if (!outstandingCheckpoints.isEmpty()) {
             long earliestDirtyTimestamp = Math.min(_persistit.earliestLiveTransaction(), _persistit
                     .earliestDirtyTimestamp());
             for (int index = outstandingCheckpoints.size(); --index >= 0;) {
                 final Checkpoint checkpoint = outstandingCheckpoints.get(index);
                 if (checkpoint.getTimestamp() <= earliestDirtyTimestamp) {
                     for (int k = index - 1; k >= 0; --k) {
                         outstandingCheckpoints.remove(k);
                     }
                     return checkpoint;
                 }
             }
         }
         return null;
     }
 
     @Override
     protected synchronized boolean shouldStop() {
         return _closed.get() && (_outstandingCheckpoints.isEmpty() || _fastClose.get());
     }
 
     @Override
     protected void runTask() throws Exception {
         if (_persistit.isInitialized()) {
             _persistit.getTimestampAllocator().updatedCheckpoint();
             proposeCheckpoint();
             flushCheckpoint();
         }
     }
 
 }
