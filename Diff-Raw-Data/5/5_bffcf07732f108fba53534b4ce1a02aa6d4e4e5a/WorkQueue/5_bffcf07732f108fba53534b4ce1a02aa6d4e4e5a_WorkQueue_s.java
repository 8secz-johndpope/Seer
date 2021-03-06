 // Copyright 2006 Google Inc.  All Rights Reserved.
 //
 // Licensed under the Apache License, Version 2.0 (the "License");
 // you may not use this file except in compliance with the License.
 // You may obtain a copy of the License at
 //
 //      http://www.apache.org/licenses/LICENSE-2.0
 //
 // Unless required by applicable law or agreed to in writing, software
 // distributed under the License is distributed on an "AS IS" BASIS,
 // WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 // See the License for the specific language governing permissions and
 // limitations under the License.
 
 package com.google.enterprise.connector.common;
 
 import java.util.HashMap;
 import java.util.HashSet;
 import java.util.Iterator;
 import java.util.LinkedList;
 import java.util.Map;
 import java.util.Set;
 import java.util.logging.Logger;
 
 /**
  * The WorkQueue provides a way for callers to issue non-blocking work
  * requests.  This class is multi-thread safe.
  */
 public class WorkQueue {
   private static final Logger logger = 
     Logger.getLogger(WorkQueue.class.getName());
   
   // possible value for nextAbsTimeout
   private static final long NO_TIMEOUT = 0;  
   
   // relative timeout that tells us when we should just kill a WorkQueueThread 
   // instead of just trying to interrupt it
   private long killThreadTimeout = 60 * 1000;
   
   /*
    * Variables that are protected by instance lock
    */
   private LinkedList workQueue;  // Queue used for added work.
   private Map absTimeoutMap;  // <WorkQueueItem, absTimeout>
   private long nextAbsTimeout;  // next earliest absolute timeout
   private boolean isInitialized;
 
   private int numThreads;
   private Set threads;
   
   private InterrupterThread interrupterThread;
   
   /**
    * Creates a WorkQueue with a default 60 second timeout before a thread is
    * killed after it is interrupted.
    * @param numThreads number of threads servicing work queue
    */
   public WorkQueue(int numThreads) {
     this(numThreads, 60 * 1000);
   }
   
   /**
    * Creates a WorkQueue with a given number of worker threads.
    * @param numThreads the number of threads to execute work on the WorkQueue.
    * This number should be at least 1.
    */
   public WorkQueue(int numThreads, long killThreadTimeout) {
     if (numThreads <= 0) {
       throw new IllegalArgumentException("numThreads must be > 0");
     }
     this.killThreadTimeout = killThreadTimeout;
     this.workQueue = new LinkedList();
     this.absTimeoutMap = new HashMap();
     this.nextAbsTimeout = NO_TIMEOUT;
     this.isInitialized = false;
     this.numThreads = numThreads;
     this.threads = new HashSet();
   }
   
   /**
    * Initialize work queue by starting worker threads.
    */
   public synchronized void init() {
     if (isInitialized) {
       return;
     }
     for (int i = 0; i < numThreads; i++) {
       WorkQueueThread thread = new WorkQueueThread(); 
      thread.setName("WorkQueueThread-" + numThreads);
       threads.add(thread);
       thread.start();
     }
     interrupterThread = new InterrupterThread();
     interrupterThread.start();
     isInitialized = true;
   }
   
   /**
    * Shutdown the work queue by interrupting any work in progress.  All work
    * that was added is discarded.
    * @param force if true, force shutdown, if false, wait till WorkQueue is 
    * cleared before shutting down
    */
   public synchronized void shutdown(boolean force) {
     if (isInitialized) {
       if (force) {
         Iterator iter = threads.iterator();
         while (iter.hasNext()) {
           Thread thread = (Thread) iter.next();
           thread.interrupt();
         }
       } else {
         while (true) {
           boolean noThreadIsWorking = true;
           Iterator iter = threads.iterator();
           while (iter.hasNext()) {
             WorkQueueThread thread = (WorkQueueThread) iter.next();
             if (thread.isWorking()) {
               noThreadIsWorking = false;
               break;
             }
           }
           if (!noThreadIsWorking) {
             try {
               Thread.sleep(1000);
             } catch (InterruptedException e) {
               // wait until we have no more work
             }
           } else {
             break;
           }
         }
       }
       interrupterThread.shutdown();
       workQueue.clear();
       isInitialized = false;
     }
   }
   
   private synchronized long getNextAbsoluteTimeout() {
     return nextAbsTimeout;
   }
 
   /**
    * Interrupts all WorkQueueItems that have timed out.
    */
  private void interruptAllTimedOutItems() {
     long now;
     // determine which work threads should be interrupted as well as 
     // set nextAbsTimeout
     now = System.currentTimeMillis();
     nextAbsTimeout = NO_TIMEOUT;
     Iterator iter;
     for (iter = absTimeoutMap.entrySet().iterator(); iter.hasNext(); ) {
       Map.Entry entry = (Map.Entry) iter.next();
       Long absTimeout = (Long) entry.getValue();
       if (absTimeout.longValue() <= now) {  // we have a timeout
         WorkQueueItem item = (WorkQueueItem) entry.getKey();
         item.getWorkQueueThread().interrupt();
         // if the wait is too long, then we want to replace the thread
         if (absTimeout.longValue() + killThreadTimeout <= now) {
           replaceHangingThread(item);
         }
       } else {  // we have a thread that will timeout later
         if ((NO_TIMEOUT == getNextAbsoluteTimeout())
             || (getNextAbsoluteTimeout() > absTimeout.longValue())) {
           nextAbsTimeout = absTimeout.longValue();
         }
       }
     }
   }
 
   /**
    * Given that a particular work item is hanging and isn't responding to
    * an interrupt, we want to be able to reclaim the thread that is hung
    * executing that work.  We do so by spawning a new thread.
    * @param item the item that is causing the hang
    */
   private void replaceHangingThread(WorkQueueItem item) {
     absTimeoutMap.remove(item);
     // replace hanging thread with new thread if timeout is too long
     threads.remove(item.getWorkQueueThread());
     WorkQueueThread thread = new WorkQueueThread();
     threads.add(thread);
     thread.start();
   }
 
   /**
    * Work to do right before executing the item.
    * @param item
    */
   private void preWork(WorkQueueItem item) {
     Long absTimeout = 
       new Long(item.getTimeout() + System.currentTimeMillis());
     synchronized(this) {
       absTimeoutMap.put(item, absTimeout);
     }
     synchronized(interrupterThread) {
       interrupterThread.notifyAll();
     }
   }
   
   /**
    * Work to do right after executing an item.
    * @param item
    */
   private void postWork(WorkQueueItem item) {
     synchronized(this) {
       absTimeoutMap.remove(item);
     }
   }
 
   /**
    * Add a piece of work that will be executed at a later time.
    * @param work one piece of work
    */
   public synchronized void addWork(WorkQueueItem work) {
     if (!isInitialized) {
       throw new IllegalStateException(
         "Must init() WorkQueue object before adding work.");
     }
     workQueue.addLast(work);
     notifyAll();
   }
   
   /**
    * Remove a piece of work.
    * @return the work item
    */
   private WorkQueueItem removeWork() {
     return (WorkQueueItem) workQueue.removeFirst();
   }
 
   /**
    * Determine the number of pieces of work that are in the queue.
    * @return the number of pieces of work in the queue.
    */
   public synchronized int getWorkCount() {
     if (!isInitialized) {
       throw new IllegalStateException(
         "Must init() WorkQueue object before getWorkCount().");
     }
     return workQueue.size();
   }
   
 
   /**
    * Interrupts WorkQueueItemThread if it takes too long.
    */
   private class InterrupterThread extends Thread {
      
     private boolean shutdown;  // true if this thread should shutdown and exit
         
     public InterrupterThread() {
       shutdown = false;
     }
         
     public void shutdown() {
       shutdown = true;
       synchronized (this) {
         notifyAll();
       }      
     }
     
     public void run() {
       while (!shutdown) {
         long now = System.currentTimeMillis();
         try {
           if (getNextAbsoluteTimeout() == NO_TIMEOUT) {
             synchronized (this) {
               wait(killThreadTimeout);
             }            
           } else {
             long timeout = getNextAbsoluteTimeout() - now;
             if (timeout > 0) {
               synchronized (this) {
                 wait(timeout);
               }              
             }
           }
         } catch (InterruptedException e) {
           // thread was signalled to determine whether there are new work items
           // to be interrupted
         }
 
         interruptAllTimedOutItems();
       }
     }
   }
   
   /**
    * A WorkQueueThread executes work in the WorkQueue and calls the associated
    * callback method when the work is complete.
    */
   private class WorkQueueThread extends Thread {    
     private boolean isWorking;
     
     public boolean isWorking() {
       return isWorking;
     }
 
     public void setWorking(boolean isWorking) {
       this.isWorking = isWorking;
     }
 
     public void run() {
       while (true) {
         WorkQueueItem item;
         synchronized (WorkQueue.this) {
           while (workQueue.isEmpty()) {
             try {
               WorkQueue.this.wait();
             } catch (InterruptedException ie) {
               // thread exits when shutdown of WorkQueue occurs
               return;
             }
           }
           
           item = removeWork();
         }
         
         item.setWorkQueueThread(this);
         try {
           WorkQueue.this.preWork(item);
           isWorking = true;
           item.doWork();
         } catch (RuntimeException e) {
           logger.warning("WorkQueueThread work had problems: " 
             + e.getMessage());
           continue;
         } finally {
           isWorking = false;
           WorkQueue.this.postWork(item);
         }
       }
     }
   }
 }
