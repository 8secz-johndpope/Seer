 /*
  * Copyright 2006 ThoughtWorks, Inc.
  *
  *  Licensed under the Apache License, Version 2.0 (the "License");
  *  you may not use this file except in compliance with the License.
  *  You may obtain a copy of the License at
  *
  *      http://www.apache.org/licenses/LICENSE-2.0
  *
  *  Unless required by applicable law or agreed to in writing, software
  *  distributed under the License is distributed on an "AS IS" BASIS,
  *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  *  See the License for the specific language governing permissions and
  *  limitations under the License.
  *
  */
 
 package org.openqa.selenium.server;
 
 import java.util.concurrent.TimeUnit;
 import java.util.concurrent.locks.Condition;
 import java.util.concurrent.locks.Lock;
 
 
 /**
  * <p>Provides a synchronizing queue that holds a single entry
  * (eg a single Selenium Command).</p>
  * @author Paul Hammant
  * @version $Revision: 411 $
  */
 public class SingleEntryAsyncQueue {
     private static int idGenerator = 1;
     private Object thing = null;
     private boolean done = false;
     private static int defaultTimeout = SeleniumServer.DEFAULT_TIMEOUT;
     private int timeout;
     private int id = idGenerator++;
     private boolean hasBlockedGetter = false;
     private String loggingPreamble;
     private String label;
     
     private int countOfCallsToGet = 0;
     private int clearCallsToGetPrecedingThisThreshold = 0;
     
     private int waitingThreadCount = 0;
     private final Lock dataLock;
     private final Condition condition;
    
     public SingleEntryAsyncQueue(String label, Lock dataLock, Condition condition) {
         this.label = label;
         this.dataLock = dataLock;
         this.condition = condition;
         timeout = defaultTimeout;
         loggingPreamble = this.label + id + ": ";
     }
     
     /**
      * clear contents and tell any waiting threads to go away.
      */
     public void clear() {
         clearCallsToGetPrecedingThisThreshold = countOfCallsToGet;
     }
     
     public int getTimeout() {
         return timeout;
     }
     public void setTimeout(int timeout) {
         this.timeout = timeout;
     }
     
     /**
      * <p>Retrieves the item from the queue.</p>
      * <p>If there's nothing in the queue right now, wait a period of time 
      * for something to show up.</p> 
      * @return the item in the queue
      * @throws SeleniumCommandTimedOutException if the timeout is exceeded. 
      */
     public Object get() {
         int thisCall = countOfCallsToGet++;
         if (done) {
             return null;
         }
         int retries = 0;
         hasBlockedGetter = true;
         Object t = null;
         waitingThreadCount++;
         try {
             while (thing==null) {
                 if (thing==null & retries >= timeout) {
                     throw new SeleniumCommandTimedOutException();
                 }
                 try {
                     condition.await(1000, TimeUnit.MILLISECONDS);
                 } catch (InterruptedException e) {
                     continue;
                 }
                 if (done || (thisCall < clearCallsToGetPrecedingThisThreshold)) {
                     return null;
                 }
                 retries++;
             }
             t = thing;
         }
         finally {
             hasBlockedGetter = false;
             waitingThreadCount--;
         }
         if (waitingThreadCount==0) {
             thing = null;
         }
         return t;
     }
         
     /**
      * <p>Retrieves the item from the queue.</p>
      * <p>If there's nothing in the queue right now, wait a period of time 
      * for something to show up.</p> 
      * @return the item in the queue
      * @throws SeleniumCommandTimedOutException if the timeout is exceeded. 
      */
     public Object peek() {
         if (done) {
             throw new RuntimeException("peek(" + this + ") on a retired queue");
         }
         if (isEmpty()) {
             throw new RuntimeException("peek() called on an empty queue");
         }
         return thing;
     }
         
     public boolean isEmpty() {
         return thing==null;
     }
 
     public String toString() {
         StringBuffer sb = new StringBuffer(loggingPreamble);
         sb.append((thing==null ? "null" : thing.toString()));
         return sb.toString();
     }
 
     /**
      * <p>Puts something in the queue.</p>
      * If there's already something available in the queue, wait
      * for that item to get picked up and removed from the queue. 
      * @param newObj - the thing to put in the queue
      */    
     public void put(Object newObj) {
         if (done) {
             throw new RuntimeException("put(" + newObj + ") on a retired queue");
         }
         if (newObj==null) {
             thing = null;
         }
         else {
             dataLock.lock();
             try {
                 thing = newObj;
                 condition.signalAll();
             }
             finally {
                 dataLock.unlock();
             }
         }
     }
 
     public static void setDefaultTimeout(int defaultTimeout) {
         SingleEntryAsyncQueue.defaultTimeout = defaultTimeout;        
     }
 
     public boolean hasBlockedGetter() {
         return hasBlockedGetter ;
     }
 
 }
