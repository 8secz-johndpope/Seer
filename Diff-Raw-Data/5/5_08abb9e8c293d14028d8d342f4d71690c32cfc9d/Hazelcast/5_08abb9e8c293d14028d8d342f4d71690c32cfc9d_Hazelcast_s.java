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
 
 package com.hazelcast.core;
 
 import com.hazelcast.config.Config;
 import com.hazelcast.instance.HazelcastInstanceFactory;
 import com.hazelcast.instance.OutOfMemoryErrorDispatcher;
 
 import java.util.Set;
 
 /**
  * Factory for all of the Hazelcast data and execution components such as
  * maps, queues, multimaps, topics and executor service.
  * <p/>
  * If not started already, Hazelcast member (HazelcastInstance) will start
  * automatically if any of the functions is called on Hazelcast.
  */
 public final class Hazelcast {
 
     private Hazelcast() {
     }
 
     /**
      * Shuts down all running Hazelcast Instances on this JVM, including the
      * default one if it is running. It doesn't shutdown all members of the
      * cluster but just the ones running on this JVM.
      *
      * @see #newHazelcastInstance(Config)
      */
     public static void shutdownAll() {
         HazelcastInstanceFactory.shutdownAll();
     }
 
     /**
      * Creates a new HazelcastInstance (a new node in a cluster).
      * This method allows you to create and run multiple instances
      * of Hazelcast cluster members on the same JVM.
      * <p/>
      * To shutdown all running HazelcastInstances (all members on this JVM)
      * call {@link #shutdownAll()}.
      *
      * @param config Configuration for the new HazelcastInstance (member)
      * @return new HazelcastInstance
      * @see #shutdownAll()
      * @see #getHazelcastInstanceByName(String)
      */
     public static HazelcastInstance newHazelcastInstance(Config config) {
         return HazelcastInstanceFactory.newHazelcastInstance(config, null);
     }
 
     /**
      * Creates a new HazelcastInstance (a new node in a cluster).
      * This method allows you to create and run multiple instances
      * of Hazelcast cluster members on the same JVM.
      * <p/>
      * To shutdown all running HazelcastInstances (all members on this JVM)
      * call {@link #shutdownAll()}.
      *
      * Hazelcast will look into two places for the configuration file:
      * <ol>
      *     <li>
      *         System property: Hazelcast will first check if "hazelcast.config" system property is set to a file path.
      *         Example: -Dhazelcast.config=C:/myhazelcast.xml.
      *     </li>
      *     <li>
      *         Classpath: If config file is not set as a system property, Hazelcast will check classpath for hazelcast.xml file.
      *     </li>
      * </ol>
      *
      * @return new HazelcastInstance
      * @see #shutdownAll()
      * @see #getHazelcastInstanceByName(String)
      */
     public static HazelcastInstance newHazelcastInstance() {
         return HazelcastInstanceFactory.newHazelcastInstance(null, null);
     }
 
     /**
      * Creates a new HazelcastInstance Lite Member (a new node in a cluster).
      * This method allows you to create and run multiple instances
      * of Hazelcast cluster members on the same JVM.
      * <p/>
      * To shutdown all running HazelcastInstances (all members on this JVM)
      * call {@link #shutdownAll()}.
      *
      * Hazelcast will look into two places for the configuration file:
       * <ol>
       *     <li>
       *         System property: Hazelcast will first check if "hazelcast.config" system property is set to a file path.
      *         Example: -Dhazelcast.config=C:/myhazelcast.xml.
       *     </li>
       *     <li>
       *         Classpath: If config file is not set as a system property, Hazelcast will check classpath for hazelcast.xml file.
       *     </li>
       * </ol>
      *
      * @return new HazelcastInstance
      * @see #shutdownAll()
      * @see #getHazelcastInstanceByName(String)
      */
     public static HazelcastInstance newLiteMemberHazelcastInstance() {
         return HazelcastInstanceFactory.newHazelcastInstance(null, Boolean.TRUE);
     }
 
     /**
      * Returns an existing HazelcastInstance with instanceName.
      * <p/>
      * To shutdown all running HazelcastInstances (all members on this JVM)
      * call {@link #shutdownAll()}.
      *
      * @param instanceName Name of the HazelcastInstance (member)
      * @return HazelcastInstance
      * @see #newHazelcastInstance(Config)
      * @see #shutdownAll()
      */
     public static HazelcastInstance getHazelcastInstanceByName(String instanceName) {
         return HazelcastInstanceFactory.getHazelcastInstance(instanceName);
     }
 
     /**
      * Returns all active/running HazelcastInstances on this JVM.
      * <p/>
      * To shutdown all running HazelcastInstances (all members on this JVM)
      * call {@link #shutdownAll()}.
      *
      * @return all HazelcastInstances
      * @see #newHazelcastInstance(Config)
      * @see #getHazelcastInstanceByName(String)
      * @see #shutdownAll()
      */
     public static Set<HazelcastInstance> getAllHazelcastInstances() {
         return HazelcastInstanceFactory.getAllHazelcastInstances();
     }
 
     /**
      * Sets <tt>OutOfMemoryHandler</tt> to be used when an <tt>OutOfMemoryError</tt>
      * is caught by Hazelcast threads.
      *
      * <p>
      * <b>Warning: </b> <tt>OutOfMemoryHandler</tt> may not be called although JVM throws
      * <tt>OutOfMemoryError</tt>.
      * Because error may be thrown from an external (user thread) thread
      * and Hazelcast may not be informed about <tt>OutOfMemoryError</tt>.
      * </p>
      *
      * @param outOfMemoryHandler
      *
      * @see OutOfMemoryError
      * @see OutOfMemoryHandler
      */
     public static void setOutOfMemoryHandler(OutOfMemoryHandler outOfMemoryHandler) {
         OutOfMemoryErrorDispatcher.setHandler(outOfMemoryHandler);
     }
 }
