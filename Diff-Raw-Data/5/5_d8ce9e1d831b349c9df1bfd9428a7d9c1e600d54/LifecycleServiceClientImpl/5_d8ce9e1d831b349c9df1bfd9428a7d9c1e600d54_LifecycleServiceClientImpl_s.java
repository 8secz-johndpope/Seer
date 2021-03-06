 /*
  * Copyright (c) 2008-2012, Hazel Bilisim Ltd. All Rights Reserved.
  *
  * Licensed under the Apache License, Version 2.0 (the "License");
  * you may not use this file except in compliance with the License.
  * You may obtain a copy of the License at
  *
  * http://www.apache.org/licenses/LICENSE-2.0
  *
  * Unless required by applicable law or agreed to in writing, software
  * distributed under the License is distributed on an "AS IS" BASIS,
  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  * See the License for the specific language governing permissions and
  * limitations under the License.
  */
 
 package com.hazelcast.client;
 
 import com.hazelcast.core.LifecycleEvent;
 import com.hazelcast.core.LifecycleEvent.LifecycleState;
 import com.hazelcast.core.LifecycleListener;
 import com.hazelcast.core.LifecycleService;
 import com.hazelcast.logging.ILogger;
 import com.hazelcast.logging.Logger;
 
 import java.util.concurrent.Callable;
 import java.util.concurrent.CopyOnWriteArrayList;
 import java.util.concurrent.atomic.AtomicBoolean;
 import java.util.logging.Level;
 
 import static com.hazelcast.core.LifecycleEvent.LifecycleState.*;
 
 public class LifecycleServiceClientImpl implements LifecycleService {
     final static ILogger logger = Logger.getLogger(LifecycleServiceClientImpl.class.getName());
     final AtomicBoolean paused = new AtomicBoolean(false);
     final AtomicBoolean running = new AtomicBoolean(true);
     final CopyOnWriteArrayList<LifecycleListener> lsLifecycleListeners = new CopyOnWriteArrayList<LifecycleListener>();
     final Object lifecycleLock = new Object();
     final HazelcastClient hazelcastClient;
 
     public LifecycleServiceClientImpl(HazelcastClient hazelcastClient) {
         this.hazelcastClient = hazelcastClient;
     }
 
     public void addLifecycleListener(LifecycleListener lifecycleListener) {
         lsLifecycleListeners.add(lifecycleListener);
     }
 
     public void removeLifecycleListener(LifecycleListener lifecycleListener) {
         lsLifecycleListeners.remove(lifecycleListener);
     }
 
     public void fireLifecycleEvent(LifecycleState lifecycleState) {
         fireLifecycleEvent(new LifecycleEvent(lifecycleState));
     }
 
     public void fireLifecycleEvent(final LifecycleEvent event) {
         logger.log(Level.INFO, "HazelcastClient is " + event.getState());
         for (LifecycleListener lifecycleListener : lsLifecycleListeners) {
             lifecycleListener.stateChanged(event);
         }
     }
 
     public boolean resume() {
         Callable<Boolean> callable = new Callable<Boolean>() {
             public Boolean call() {
                 synchronized (lifecycleLock) {
                     if (paused.get()) {
                         fireLifecycleEvent(RESUMING);
                     } else {
                         return false;
                     }
                     paused.set(false);
                     fireLifecycleEvent(RESUMED);
                     return true;
                 }
             }
         };
         return hazelcastClient.callAsyncAndWait(callable);
     }
 
     public boolean pause() {
         Callable<Boolean> callable = new Callable<Boolean>() {
             public Boolean call() {
                 synchronized (lifecycleLock) {
                    if (paused.get()) {
                         fireLifecycleEvent(PAUSING);
                     } else {
                         return false;
                     }
                    paused.set(false);
                     fireLifecycleEvent(PAUSED);
                     return true;
                 }
             }
         };
         return hazelcastClient.callAsyncAndWait(callable);
     }
 
     public void shutdown() {
         Callable<Boolean> callable = new Callable<Boolean>() {
             public Boolean call() {
                 synchronized (lifecycleLock) {
                     long begin = System.currentTimeMillis();
                     fireLifecycleEvent(SHUTTING_DOWN);
                     hazelcastClient.doShutdown();
                     running.set(false);
                     long time = System.currentTimeMillis() - begin;
                     logger.log(Level.FINE, "HazelcastClient shutdown completed in " + time + " ms.");
                     fireLifecycleEvent(SHUTDOWN);
                     return true;
                 }
             }
         };
         hazelcastClient.callAsyncAndWait(callable);
     }
 
     public void kill() {
         shutdown();
     }
 
     public void restart() {
         throw new UnsupportedOperationException();
     }
 
     public boolean isRunning() {
         return running.get();
     }
 }
