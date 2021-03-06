 /*
  * Copyright (c) 2008-2013, Hazelcast, Inc. All Rights Reserved.
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
 
 package com.hazelcast.concurrent.lock;
 
 import com.hazelcast.config.Config;
 import com.hazelcast.core.Hazelcast;
 import com.hazelcast.core.HazelcastInstance;
 import com.hazelcast.core.HazelcastInstanceNotActiveException;
 import com.hazelcast.core.ILock;
 import com.hazelcast.instance.StaticNodeFactory;
 import com.hazelcast.spi.exception.DistributedObjectDestroyedException;
 import org.junit.Assert;
 import org.junit.Test;
 import org.junit.runner.RunWith;
 
 import java.util.Random;
 import java.util.concurrent.CountDownLatch;
 import java.util.concurrent.ExecutorService;
 import java.util.concurrent.Executors;
 import java.util.concurrent.TimeUnit;
 import java.util.concurrent.atomic.AtomicInteger;
 import java.util.concurrent.locks.Lock;
 
 import static org.junit.Assert.*;
 
 
 /**
  * User: sancar
  * Date: 2/18/13
  * Time: 5:12 PM
  */
 @RunWith(com.hazelcast.util.RandomBlockJUnit4ClassRunner.class)
 public class LockTest {
 
     @Test
     public void testSimpleUsage() throws InterruptedException {
         // with multiple threads on single node
         // lock, tryLock, isLocked, unlock
         final StaticNodeFactory nodeFactory = new StaticNodeFactory(1);
         final Config config = new Config();
        final HazelcastInstance instance = nodeFactory.newInstance(config);
         final AtomicInteger atomicInteger = new AtomicInteger(0);
         final ILock lock = instance.getLock("testSimpleUsage");
         Assert.assertEquals("testSimpleUsage", lock.getName());
 
         final Runnable tryLockRunnable = new Runnable() {
             public void run() {
                 if (lock.tryLock())
                     atomicInteger.incrementAndGet();
             }
         };
 
         final Runnable lockRunnable = new Runnable() {
             public void run() {
                 lock.lock();
             }
         };
 
         Assert.assertEquals(false, lock.isLocked());
         lock.lock();
         Assert.assertEquals(true, lock.isLocked());
         Assert.assertEquals(true, lock.tryLock());
         lock.unlock();
 
         Thread thread1 = new Thread(tryLockRunnable);
         thread1.start();
         thread1.join();
         Assert.assertEquals(0, atomicInteger.get());
 
         lock.unlock();
         Thread thread2 = new Thread(tryLockRunnable);
         thread2.start();
         thread2.join();
         Assert.assertEquals(1, atomicInteger.get());
         Assert.assertEquals(true, lock.isLocked());
         lock.forceUnlock();
 
         Thread thread3 = new Thread(lockRunnable);
         thread3.start();
         thread3.join();
         Assert.assertEquals(true, lock.isLocked());
         Assert.assertEquals(false, lock.tryLock(2, TimeUnit.SECONDS));
 
         Thread thread4 = new Thread(lockRunnable);
         thread4.start();
         Thread.sleep(1000);
         Assert.assertEquals(true, lock.isLocked());
         lock.forceUnlock();
         thread4.join();
     }
 
     @Test
     public void testSimpleUsageOnMultipleNodes() {
         // TODO with multiple threads on multiple nodes
     }
 
     @Test(expected = DistributedObjectDestroyedException.class)
     public void testDestroyLockWhenOtherWaitingOnLock() throws InterruptedException {
         final StaticNodeFactory nodeFactory = new StaticNodeFactory(1);
        final HazelcastInstance instance = nodeFactory.newInstance(new Config());
         final ILock lock = instance.getLock("testLockDestroyWhenWaitingLock");
         lock.lock();
         Thread t = new Thread(new Runnable() {
             public void run() {
                 lock.lock();
             }
         });
         t.start();
         lock.destroy();
         t.join();
     }
 
     @Test(expected = HazelcastInstanceNotActiveException.class)
     public void testShutDownNodeWhenOtherWaitingOnLock() throws InterruptedException {
         final StaticNodeFactory nodeFactory = new StaticNodeFactory(2);
        final HazelcastInstance instance = nodeFactory.newInstance(new Config());
        nodeFactory.newInstance(new Config());
         final ILock lock = instance.getLock("testLockDestroyWhenWaitingLock");
         lock.lock();
         Thread t = new Thread(new Runnable() {
             public void run() {
                 lock.lock();
             }
         });
         t.start();
         instance.getLifecycleService().shutdown();
         t.join();
     }
 
     @Test(expected = IllegalMonitorStateException.class)
     public void testIllegalUnlock() {
         final StaticNodeFactory nodeFactory = new StaticNodeFactory(1);
        final HazelcastInstance instance = nodeFactory.newInstance(new Config());
         final ILock lock = instance.getLock("testIllegalUnlock");
         lock.unlock();
     }
 
     @Test(timeout = 100000)
     public void testLockOwnerDies() throws Exception {
         final StaticNodeFactory nodeFactory = new StaticNodeFactory(2);
         final Config config = new Config();
         final AtomicInteger integer = new AtomicInteger(0);
        final HazelcastInstance lockOwner = nodeFactory.newInstance(config);
        final HazelcastInstance instance1 = nodeFactory.newInstance(config);
 
         final String name = "testLockOwnerDies";
         final ILock lock = lockOwner.getLock(name);
         lock.lock();
         Assert.assertEquals(true, lock.isLocked());
         Thread t = new Thread(new Runnable() {
             public void run() {
                 final ILock lock = instance1.getLock(name);
                 lock.lock();
                 integer.incrementAndGet();
 
             }
         });
         t.start();
         Assert.assertEquals(0, integer.get());
         lockOwner.getLifecycleService().shutdown();
         Thread.sleep(5000);
         Assert.assertEquals(1, integer.get());
     }
 
     @Test(timeout = 100000)
     public void testKeyOwnerDies() throws Exception {
         final StaticNodeFactory nodeFactory = new StaticNodeFactory(3);
         final Config config = new Config();
        final HazelcastInstance keyOwner = nodeFactory.newInstance(config);
        final HazelcastInstance instance1 = nodeFactory.newInstance(config);
        final HazelcastInstance instance2 = nodeFactory.newInstance(config);
         int k = 0;
         final AtomicInteger atomicInteger = new AtomicInteger(0);
         while (instance1.getPartitionService().getPartition(k++).equals(keyOwner.getCluster().getLocalMember())) ;
         final int key = k;
 
         final ILock lock1 = instance1.getLock(key);
         lock1.lock();
 
         Thread t = new Thread(new Runnable() {
             public void run() {
                 final ILock lock = instance2.getLock(key);
                 lock.lock();
                 atomicInteger.incrementAndGet();
             }
         });
         t.start();
 
         keyOwner.getLifecycleService().shutdown();
         Assert.assertEquals(true, lock1.isLocked());
         Assert.assertEquals(true, lock1.tryLock());
         lock1.unlock();
         lock1.unlock();
         Thread.sleep(1000);
 
         Assert.assertEquals(1, atomicInteger.get());
         lock1.forceUnlock();
 
     }
 
     @Test(timeout = 100000)
     public void testScheduledLockActionForDeadMember() throws Exception {
         final HazelcastInstance h1 = Hazelcast.newHazelcastInstance(new Config());
         final ILock lock1 = h1.getLock("default");
         final HazelcastInstance h2 = Hazelcast.newHazelcastInstance(new Config());
         final ILock lock2 = h2.getLock("default");
         assertTrue(lock1.tryLock());
         new Thread(new Runnable() {
             public void run() {
                 try {
                     lock2.lock();
                     fail("Shouldn't be able to lock!");
                 } catch (Throwable e) {
                 }
             }
         }).start();
         Thread.sleep(2000);
         h2.getLifecycleService().shutdown();
         Thread.sleep(2000);
         lock1.unlock();
         assertTrue(lock1.tryLock());
     }
 
     @Test
     public void testLockInterruption() throws InterruptedException {
         Config config = new Config();
         final HazelcastInstance hz = Hazelcast.newHazelcastInstance(config);
 
         final Lock lock = hz.getLock("test");
         Random rand = new Random();
         for (int i = 0; i < 30; i++) {
             Thread t = new Thread() {
                 public void run() {
                     try {
                         lock.lock();
                         sleep(1);
                     } catch (InterruptedException e) {
                     } finally {
                         lock.unlock();
                     }
                 }
             };
 
             t.start();
             Thread.sleep(rand.nextInt(3));
             t.interrupt();
             t.join();
 
             if (!lock.tryLock(3, TimeUnit.SECONDS)) {
                 fail("Could not acquire lock!");
             } else {
                 lock.unlock();
             }
             Thread.sleep(100);
         }
     }
 
     @Test
     public void testLockInterruption2() throws InterruptedException {
         Config config = new Config();
         final HazelcastInstance hz = Hazelcast.newHazelcastInstance(config);
 
         final Lock lock = hz.getLock("test");
         Thread t = new Thread(new Runnable() {
             public void run() {
                 try {
                     lock.tryLock(60, TimeUnit.SECONDS);
                 } catch (InterruptedException e) {
                     System.err.println(e);
                 } finally {
                     lock.unlock();
                 }
             }
         });
         lock.lock();
         t.start();
         Thread.sleep(250);
         t.interrupt();
         Thread.sleep(1000);
         lock.unlock();
         Thread.sleep(500);
         assertTrue("Could not acquire lock!", lock.tryLock());
     }
 
     /**
      * Test for issue #39
      */
     @Test
     public void testLockIsLocked() throws InterruptedException {
         Config config = new Config();
         final HazelcastInstance h1 = Hazelcast.newHazelcastInstance(config);
         final HazelcastInstance h2 = Hazelcast.newHazelcastInstance(config);
         final HazelcastInstance h3 = Hazelcast.newHazelcastInstance(config);
         final ILock lock = h1.getLock("testLockIsLocked");
         final ILock lock2 = h2.getLock("testLockIsLocked");
 
         assertFalse(lock.isLocked());
         assertFalse(lock2.isLocked());
         lock.lock();
         assertTrue(lock.isLocked());
         assertTrue(lock2.isLocked());
 
         final CountDownLatch latch = new CountDownLatch(1);
         Thread thread = new Thread(new Runnable() {
             public void run() {
                 ILock lock3 = h3.getLock("testLockIsLocked");
 
                 assertTrue(lock3.isLocked());
                 try {
                     while (lock3.isLocked()) {
                         Thread.sleep(100);
                     }
                 } catch (InterruptedException e) {
                     throw new RuntimeException(e);
                 }
                 latch.countDown();
             }
         });
         thread.start();
         Thread.sleep(100);
         lock.unlock();
         assertTrue(latch.await(3, TimeUnit.SECONDS));
     }
 
     @Test(timeout = 1000 * 100)
     /**
      * Test for issue 267
      */
     public void testHighConcurrentLockAndUnlock() {
         Config config = new Config();
         final HazelcastInstance hz = Hazelcast.newHazelcastInstance(config);
         final String key = "key";
         final int threadCount = 100;
         final int lockCountPerThread = 5000;
         final int locks = 50;
         final CountDownLatch latch = new CountDownLatch(threadCount);
         final AtomicInteger totalCount = new AtomicInteger();
 
         class InnerTest implements Runnable {
             public void run() {
                 boolean live = true;
                 Random rand = new Random();
                 try {
                     for (int j = 0; j < lockCountPerThread && live; j++) {
                         final Lock lock = hz.getLock(key + rand.nextInt(locks));
                         lock.lock();
                         try {
                             totalCount.incrementAndGet();
                             Thread.sleep(1);
                         } catch (InterruptedException e) {
                             e.printStackTrace();
                             break;
                         } finally {
                             try {
                                 lock.unlock();
                             } catch (Exception e) {
                                 e.printStackTrace();
                                 live = false;
                             }
                         }
                     }
                 } finally {
                     latch.countDown();
                 }
             }
         }
 
         ExecutorService executorService = Executors.newCachedThreadPool();
         for (int i = 0; i < threadCount; i++) {
             executorService.execute(new InnerTest());
         }
 
         try {
             assertTrue("Lock tasks stuck!", latch.await(60, TimeUnit.SECONDS));
             assertEquals((threadCount * lockCountPerThread), totalCount.get());
         } catch (InterruptedException e) {
             e.printStackTrace();
         } finally {
             try {
                 hz.getLifecycleService().kill();
             } catch (Throwable ignored) {
             }
             executorService.shutdownNow();
         }
     }
 
 }
