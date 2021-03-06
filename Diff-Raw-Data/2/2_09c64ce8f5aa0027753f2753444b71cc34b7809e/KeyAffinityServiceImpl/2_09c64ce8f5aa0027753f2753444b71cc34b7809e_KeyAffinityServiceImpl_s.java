 /*
  * JBoss, Home of Professional Open Source
  * Copyright 2010 Red Hat Inc. and/or its affiliates and other
  * contributors as indicated by the @author tags. All rights reserved.
  * See the copyright.txt in the distribution for a full listing of
  * individual contributors.
  *
  * This is free software; you can redistribute it and/or modify it
  * under the terms of the GNU Lesser General Public License as
  * published by the Free Software Foundation; either version 2.1 of
  * the License, or (at your option) any later version.
  *
  * This software is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
  * Lesser General Public License for more details.
  *
  * You should have received a copy of the GNU Lesser General Public
  * License along with this software; if not, write to the Free
  * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
  * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
  */
 package org.infinispan.affinity;
 
 import net.jcip.annotations.GuardedBy;
 import net.jcip.annotations.ThreadSafe;
 import org.infinispan.Cache;
 import org.infinispan.distribution.ch.ConsistentHash;
 import org.infinispan.distribution.DistributionManager;
 import org.infinispan.manager.EmbeddedCacheManager;
 import org.infinispan.notifications.cachemanagerlistener.event.CacheStoppedEvent;
 import org.infinispan.notifications.cachemanagerlistener.event.ViewChangedEvent;
 import org.infinispan.remoting.transport.Address;
 import org.infinispan.util.concurrent.ConcurrentHashSet;
 import org.infinispan.util.concurrent.ReclosableLatch;
 import org.infinispan.util.logging.Log;
 import org.infinispan.util.logging.LogFactory;
 
 import java.util.Collection;
 import java.util.Collections;
 import java.util.List;
 import java.util.Map;
 import java.util.Set;
 import java.util.concurrent.ArrayBlockingQueue;
 import java.util.concurrent.BlockingQueue;
 import java.util.concurrent.ConcurrentHashMap;
 import java.util.concurrent.Executor;
 import java.util.concurrent.atomic.AtomicInteger;
 import java.util.concurrent.locks.ReadWriteLock;
 import java.util.concurrent.locks.ReentrantReadWriteLock;
 
 /**
  * Implementation of KeyAffinityService.
  *
  * @author Mircea.Markus@jboss.com
  * @since 4.1
  */
 @ThreadSafe
 public class KeyAffinityServiceImpl implements KeyAffinityService {
 
    private final float THRESHOLD = 0.5f;
    
    private static final Log log = LogFactory.getLog(KeyAffinityServiceImpl.class);
 
    private final Set<Address> filter;
 
    @GuardedBy("maxNumberInvariant")
    private final Map<Address, BlockingQueue> address2key = new ConcurrentHashMap<Address, BlockingQueue>();
    private final Executor executor;
    private final Cache cache;
    private final KeyGenerator keyGenerator;
    private final int bufferSize;
    private final AtomicInteger maxNumberOfKeys = new AtomicInteger(); //(nr. of addresses) * bufferSize;
    private final AtomicInteger exitingNumberOfKeys = new AtomicInteger();
 
    private volatile boolean started;
 
    /**
     * Guards and make sure the following invariant stands:  maxNumberOfKeys ==  address2key.keys().size() * bufferSize
     */
    private final ReadWriteLock maxNumberInvariant = new ReentrantReadWriteLock();
 
    /**
     * Used for coordinating between the KeyGeneratorWorker and consumers.
     */
    private final ReclosableLatch keyProducerStartLatch = new ReclosableLatch();
    private volatile KeyGeneratorWorker keyGenWorker;
    private volatile ListenerRegistration listenerRegistration;
 
 
    public KeyAffinityServiceImpl(Executor executor, Cache cache, KeyGenerator keyGenerator, int bufferSize, Collection<Address> filter, boolean start) {
       this.executor = executor;
       this.cache = cache;
       this.keyGenerator = keyGenerator;
       this.bufferSize = bufferSize;
       if (filter != null) {
          this.filter = new ConcurrentHashSet<Address>();
          for (Address address : filter) {
             this.filter.add(address);
          }
       } else {
          this.filter = null;
       }
       if (start)
          start();
    }
 
    @Override
    public Object getCollocatedKey(Object otherKey) {
       Address address = getAddressForKey(otherKey);
       return getKeyForAddress(address);
    }
 
    @Override
    public Object getKeyForAddress(Address address) {
       if (!started) {
          throw new IllegalStateException("You have to start the service first!");
       }
       if (address == null)
          throw new NullPointerException("Null address not supported!");
       BlockingQueue queue = address2key.get(address);
       try {
          maxNumberInvariant.readLock().lock();
          Object result;
          try {
             result = queue.take();
          } finally {
             maxNumberInvariant.readLock().unlock();
          }
          exitingNumberOfKeys.decrementAndGet();
          return result;
       } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
          return null;
       } finally {
          if (queue.size() < maxNumberOfKeys.get() * THRESHOLD + 1) {
             keyProducerStartLatch.open();
          }
       }
    }
 
    @Override
    public void start() {
       if (started) {
          log.debug("Service already started, ignoring call to start!");
          return;
       }
       List<Address> existingNodes = getExistingNodes();
       maxNumberInvariant.writeLock().lock();
       try {
          addQueuesForAddresses(existingNodes);
          resetNumberOfKeys();
       } finally {
          maxNumberInvariant.writeLock().unlock();
       }
       keyGenWorker = new KeyGeneratorWorker();
       executor.execute(keyGenWorker);
       listenerRegistration = new ListenerRegistration(this);
      ((EmbeddedCacheManager)cache.getCacheManager()).addListener(listenerRegistration);
       cache.addListener(listenerRegistration);
       keyProducerStartLatch.open();
       started = true;
    }
 
    @Override
    public void stop() {
       if (!started) {
          log.debug("Ignoring call to stop as service is not started.");
          return;
       }
       started = false;
       EmbeddedCacheManager cacheManager = (EmbeddedCacheManager) cache.getCacheManager();
       if (cacheManager.getListeners().contains(listenerRegistration)) {
          cacheManager.removeListener(listenerRegistration);
       } else {
          throw new IllegalStateException("Listener must have been registered!");
       }
       //most likely the listeners collection is shared between CacheManager and the Cache
       if (cache.getListeners().contains(listenerRegistration)) {
          cache.removeListener(listenerRegistration);
       }
       keyGenWorker.stop();
    }
 
    public void handleViewChange(ViewChangedEvent vce) {
       if (log.isTraceEnabled()) {
          log.tracef("ViewChange received: %s", vce);
       }
       maxNumberInvariant.writeLock().lock();
       try {
          address2key.clear(); //wee need to drop everything as key-mapping data is stale due to view change
          addQueuesForAddresses(vce.getNewMembers());
          resetNumberOfKeys();
          keyProducerStartLatch.open();
       } finally {
          maxNumberInvariant.writeLock().unlock();
       }
    }
 
    public boolean isKeyGeneratorThreadAlive() {
       return keyGenWorker.isAlive() ;
    }
 
    public void handleCacheStopped(CacheStoppedEvent cse) {
       if (log.isTraceEnabled()) {
          log.tracef("Cache stopped, stopping the service: %s", cse);
       }
       stop();
    }
 
 
    public class KeyGeneratorWorker implements Runnable {
 
       private volatile boolean isActive;
       private boolean isAlive;
       private volatile Thread runner;
 
       @Override
       public void run() {
          this.runner = Thread.currentThread();
          isAlive = true;
          while (true) {
             if (waitToBeWakenUp()) break;
             isActive = true;
             if (log.isTraceEnabled()) {
                log.trace("KeyGeneratorWorker marked as ACTIVE");
             }
             generateKeys();
             
             isActive = false;
             if (log.isTraceEnabled()) {
                log.trace("KeyGeneratorWorker marked as INACTIVE");
             }
          }
          isAlive = false;
       }
 
       private void generateKeys() {
          maxNumberInvariant.readLock().lock();
          try {
             while (maxNumberOfKeys.get() != exitingNumberOfKeys.get()) {
                Object key = keyGenerator.getKey();
                Address addressForKey = getAddressForKey(key);
                if (interestedInAddress(addressForKey)) {
                   tryAddKey(addressForKey, key);
                }
             }
             keyProducerStartLatch.close();
          } finally {
             maxNumberInvariant.readLock().unlock();
          }
       }
 
       private boolean waitToBeWakenUp() {
          try {
             keyProducerStartLatch.await();
          } catch (InterruptedException e) {
             if (log.isDebugEnabled()) {
                log.debugf("Shutting down KeyAffinity service for key set: %s", filter);
             }
             return true;
          }
          return false;
       }
 
       private void tryAddKey(Address address, Object key) {
          BlockingQueue queue = address2key.get(address);
          // on node stop the distribution manager might still return the dead server for a while after we have already removed its queue
          if (queue == null)
             return;
 
          boolean added = queue.offer(key);
          if (added) {
             exitingNumberOfKeys.incrementAndGet();
          }
          if (log.isTraceEnabled()) {
             if (added)
                log.tracef("Successfully added key(%s) to the address(%s), maxNumberOfKeys=%d, exitingNumberOfKeys=%d",
                           key, address, maxNumberOfKeys.get(), exitingNumberOfKeys.get());
             else
                log.tracef("Not added key(%s) to the address(%s)", key, address);
          }
       }
 
       public boolean isActive() {
          return isActive;
       }
 
       public boolean isAlive() {
          return isAlive;
       }
 
       public void stop() {
          runner.interrupt();
       }
    }
 
    /**
     * Important: this *MUST* be called with WL on {@link #address2key}.
     */
    private void resetNumberOfKeys() {
       maxNumberOfKeys.set(address2key.keySet().size() * bufferSize);
       exitingNumberOfKeys.set(0);
       if (log.isTraceEnabled()) {
          log.tracef("resetNumberOfKeys ends with: maxNumberOfKeys=%s, exitingNumberOfKeys=%s",
                     maxNumberOfKeys.get(), exitingNumberOfKeys.get());
       }
    }
 
    /**
     * Important: this *MUST* be called with WL on {@link #address2key}.
     */
    private void addQueuesForAddresses(Collection<Address> addresses) {
       for (Address address : addresses) {
          if (interestedInAddress(address)) {
             address2key.put(address, new ArrayBlockingQueue(bufferSize));
          } else {
             if (log.isTraceEnabled())
                log.tracef("Skipping address: %s", address);
          }
       }
    }
 
    private boolean interestedInAddress(Address address) {
       return filter == null || filter.contains(address);
    }
 
    private List<Address> getExistingNodes() {
       return cache.getAdvancedCache().getRpcManager().getTransport().getMembers();
    }
 
    private Address getAddressForKey(Object key) {
       DistributionManager distributionManager = getDistributionManager();
       ConsistentHash hash = distributionManager.getConsistentHash();
       List<Address> addressList = hash.locate(key, 1);
       if (addressList.size() == 0) {
          throw new IllegalStateException("Empty address list returned by consistent hash " + hash + " for key " + key);
       }
       return addressList.get(0);
    }
 
    private DistributionManager getDistributionManager() {
       DistributionManager distributionManager = cache.getAdvancedCache().getDistributionManager();
       if (distributionManager == null) {
          throw new IllegalStateException("Null distribution manager. Is this an distributed(v.s. replicated) cache?");
       }
       return distributionManager;
    }
 
    public Map<Address, BlockingQueue> getAddress2KeysMapping() {
       return Collections.unmodifiableMap(address2key);
    }
 
    public int getMaxNumberOfKeys() {
       return maxNumberOfKeys.intValue();
    }
 
    public int getExitingNumberOfKeys() {
       return exitingNumberOfKeys.intValue();
    }
 
    public boolean isKeyGeneratorThreadActive() {
       return keyGenWorker.isActive();
    }
 
    public boolean isStarted() {
       return started;
    }
 }
