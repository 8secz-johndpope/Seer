 /*
  *
  * Copyright (c) 2011. All Rights Reserved. Oracle Corporation.
  *
  * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
  *
  * This software is the confidential and proprietary information of Oracle
  * Corporation. You shall not disclose such confidential and proprietary
  * information and shall use it only in accordance with the terms of the license
  * agreement you entered into with Oracle Corporation.
  *
  * Oracle Corporation makes no representations or warranties about the
  * suitability of the software, either express or implied, including but not
  * limited to the implied warranties of merchantability, fitness for a
  * particular purpose, or non-infringement. Oracle Corporation shall not be
  * liable for any damages suffered by licensee as a result of using, modifying
  * or distributing this software or its derivatives.
  *
  * This notice may not be removed or altered.
  */
 package com.tangosol.coherence.jsr107;
 
 import com.tangosol.net.ConfigurableCacheFactory;
 import com.tangosol.net.NamedCache;
 import com.tangosol.util.InvocableMap;
 import com.tangosol.util.WrapperException;
 
 import javax.cache.CacheConfiguration;
 import javax.cache.CacheException;
 import javax.cache.CacheLoader;
 import javax.cache.CacheStatistics;
 import javax.cache.CacheWriter;
 import javax.cache.Status;
 import javax.cache.event.CacheEntryListener;
 import javax.cache.event.NotificationScope;
 import javax.cache.implementation.AbstractCache;
 import java.util.ArrayList;
 import java.util.Collection;
 import java.util.Collections;
 import java.util.Iterator;
 import java.util.Map;
 import java.util.Set;
 import java.util.concurrent.Callable;
 import java.util.concurrent.Future;
 import java.util.concurrent.FutureTask;
 
 /**
  * @author ycosmado
  * @since 1.0
  */
 public class CoherenceCache<K, V> extends AbstractCache<K, V> {
     private static final int CACHE_LOADER_THREADS = 2;
 
     private final NamedCache namedCache;
     private volatile Status status;
 
     private CoherenceCache(NamedCache namedCache,
                            String cacheName,
                            String cacheManagerName,
                            Set<Class<?>> immutableClasses,
                            ClassLoader classLoader,
                            CacheConfiguration configuration,
                            CacheLoader<K, V> cacheLoader,
                            CacheWriter<K, V> cacheWriter) {
         super(cacheName,
             cacheManagerName,
             immutableClasses,
             classLoader,
             configuration,
             cacheLoader,
             cacheWriter);
         this.namedCache = namedCache;
         status = Status.UNINITIALISED;
     }
 
     @Override
     public V get(K key) throws CacheException {
         checkStatusStarted();
         if (key == null) {
             throw new NullPointerException();
         }
         try {
             return invokeWithCacheLoader(key, new GetProcessor<V>());
         } catch (WrapperException e) {
             throw thunkException(e);
         }
     }
 
     @Override
     public Map<K, V> getAll(Collection<? extends K> keys) throws CacheException {
         checkStatusStarted();
         if (keys == null) {
             throw new NullPointerException();
         }
         if (keys.contains(null)) {
             throw new NullPointerException();
         }
         //return namedCache.getAll(keys);
         try {
             return invokeWithCacheLoader(keys, new GetProcessor<V>());
         } catch (WrapperException e) {
             throw thunkException(e);
         }
     }
 
     @Override
     public boolean containsKey(K key) throws CacheException {
         checkStatusStarted();
         if (key == null) {
             throw new NullPointerException();
         }
         return namedCache.containsKey(key);
     }
 
     @Override
     public Future<V> load(K key) throws CacheException {
         checkStatusStarted();
         if (key == null) {
             throw new NullPointerException();
         }
         if (getCacheLoader() == null) {
             return null;
         }
         FutureTask<V> task = new FutureTask<V>(new CoherenceCacheLoaderLoadCallable<K, V>(namedCache, getCacheLoader(), key));
         submit(task);
         return task;
     }
 
     @Override
     public Future<Map<K, V>> loadAll(Collection<? extends K> keys) throws CacheException {
         checkStatusStarted();
         if (keys == null) {
             throw new NullPointerException();
         }
         if (keys.contains(null)) {
             throw new NullPointerException();
         }
         if (getCacheLoader() == null) {
             return null;
         }
         FutureTask<Map<K, V>> task = new FutureTask<Map<K, V>>(new CoherenceCacheLoaderLoadAllCallable<K, V>(namedCache, getCacheLoader(), keys));
         submit(task);
         return task;
     }
 
     @Override
     public CacheStatistics getStatistics() {
         throw new UnsupportedOperationException();
     }
 
     @Override
     public void put(K key, V value) throws CacheException {
         checkStatusStarted();
         if (key == null) {
             throw new NullPointerException();
         }
         if (value == null) {
             throw new NullPointerException();
         }
         Map<K, V> map = Collections.singletonMap(key, value);
         namedCache.putAll(map);
     }
 
     @Override
     public V getAndPut(K key, V value) throws CacheException {
         checkStatusStarted();
         if (key == null) {
             throw new NullPointerException();
         }
         if (value == null) {
             throw new NullPointerException();
         }
         return (V) namedCache.put(key, value);
     }
 
     @Override
     public void putAll(Map<? extends K, ? extends V> map) throws CacheException {
         checkStatusStarted();
         if (map == null) {
             throw new NullPointerException();
         }
         if (map.containsKey(null)) {
             throw new NullPointerException();
         }
         if (map.containsValue(null)) {
             throw new NullPointerException();
         }
         namedCache.putAll(map);
         //throw new UnsupportedOperationException();
     }
 
     @Override
     public boolean putIfAbsent(K key, V value) throws CacheException {
         checkStatusStarted();
         if (key == null) {
             throw new NullPointerException();
         }
         if (value == null) {
             throw new NullPointerException();
         }
         return (Boolean) namedCache.invoke(key, new PutIfAbsentProcessor<V>(value));
     }
 
     @Override
     public boolean remove(K key) throws CacheException {
         //TODO: optimize
         return getAndRemove(key) != null;
     }
 
     @Override
     public boolean remove(K key, V oldValue) throws CacheException {
         checkStatusStarted();
         if (key == null) {
             throw new NullPointerException();
         }
         if (oldValue == null) {
             throw new NullPointerException();
         }
         return (Boolean) namedCache.invoke(key, new Remove2Processor<V>(oldValue));
     }
 
     @Override
     public V getAndRemove(K key) throws CacheException {
         checkStatusStarted();
         if (key == null) {
             throw new NullPointerException();
         }
         return (V) namedCache.remove(key);
     }
 
     @Override
     public boolean replace(K key, V oldValue, V newValue) throws CacheException {
         checkStatusStarted();
         if (key == null) {
             throw new NullPointerException();
         }
         if (oldValue == null) {
             throw new NullPointerException();
         }
         if (newValue == null) {
             throw new NullPointerException();
         }
         return (Boolean) namedCache.invoke(key, new Replace3Processor<V>(oldValue, newValue));
     }
 
     @Override
     public boolean replace(K key, V value) throws CacheException {
         checkStatusStarted();
         if (key == null) {
             throw new NullPointerException();
         }
         if (value == null) {
             throw new NullPointerException();
         }
         return (Boolean) namedCache.invoke(key, new Replace2Processor<V>(value));
     }
 
     @Override
     public V getAndReplace(K key, V value) throws CacheException {
         checkStatusStarted();
         if (key == null) {
             throw new NullPointerException();
         }
         if (value == null) {
             throw new NullPointerException();
         }
         return (V) namedCache.invoke(key, new GetAndReplaceProcessor<V>(value));
     }
 
     @Override
     public void removeAll(Collection<? extends K> keys) throws CacheException {
         checkStatusStarted();
         if (keys == null) {
             throw new NullPointerException();
         }
         if (keys.contains(null)) {
             throw new NullPointerException();
         }
         namedCache.invokeAll(keys, new RemoveAll1Processor());
     }
 
     @Override
     public void removeAll() throws CacheException {
         checkStatusStarted();
         namedCache.clear();
     }
 
     @Override
     public boolean registerCacheEntryListener(CacheEntryListener<? super K, ? super V> cacheEntryListener, NotificationScope scope, boolean synchronous) {
         throw new UnsupportedOperationException();
     }
 
     @Override
     public boolean unregisterCacheEntryListener(CacheEntryListener<?, ?> cacheEntryListener) {
         throw new UnsupportedOperationException();
     }
 
     @Override
     public String getName() {
         return namedCache.getCacheName();
     }
 
     @Override
     public <T> T unwrap(Class<T> cls) {
         if (cls.isAssignableFrom(this.getClass())) {
             return cls.cast(this);
         }
         throw new IllegalArgumentException();
     }
 
     @Override
     public void start() throws CacheException {
         status = Status.STARTED;
     }
 
     @Override
     public void stop() throws CacheException {
         super.stop();
         namedCache.clear();
         //TODO: this causes problem
         //namedCache.release();
         status = Status.STOPPED;
     }
 
     @Override
     public Status getStatus() {
         return status;
     }
 
     @Override
     public Iterator<Entry<K, V>> iterator() {
         checkStatusStarted();
         return new EntryIterator<K, V>(namedCache.entrySet().iterator());
     }
 
     public NamedCache getNamedCache() {
         return namedCache;
     }
 
     private void checkStatusStarted() {
         if (!Status.STARTED.equals(status)) {
             throw new IllegalStateException("The cache status is not STARTED");
         }
     }
 
     private RuntimeException thunkException(WrapperException e) {
         Throwable originalException = e.getOriginalException();
         if (originalException instanceof RuntimeException) {
             return (RuntimeException) originalException;
         } else {
             return new CacheException(originalException);
         }
     }
 
     private V invokeWithCacheLoader(K key, InvocableMap.EntryProcessor processor) {
         CacheLoader<K, V> cacheLoader = getCacheLoader();
         Object ret = cacheLoader == null ?
             namedCache.invoke(key, processor) :
             namedCache.invoke(key, new CacheLoaderProcessor<K, V>(processor, cacheLoader));
         return (V) ret;
     }
 
     private Map<K, V> invokeWithCacheLoader(Collection<? extends K> keys, GetProcessor<V> processor) {
         CacheLoader<K, V> cacheLoader = getCacheLoader();
         Object ret = cacheLoader == null ?
             namedCache.invokeAll(keys, processor) :
             namedCache.invokeAll(keys, new CacheLoaderProcessor<K, V>(processor, cacheLoader));
         return (Map<K, V>) ret;
     }
 
     public static class EntryIterator<K, V> implements Iterator<Entry<K, V>> {
         private final Iterator<Map.Entry<K, V>> mapIterator;
 
         public EntryIterator(Iterator<Map.Entry<K, V>> iterator) {
             this.mapIterator = iterator;
         }
 
         @Override
         public boolean hasNext() {
             return mapIterator.hasNext();
         }
 
         @Override
         public Entry<K, V> next() {
             final Map.Entry<K, V> mapEntry = mapIterator.next();
             return new Entry<K, V>() {
                 @Override
                 public K getKey() {
                     return mapEntry.getKey();
                 }
                 @Override
                 public V getValue() {
                     return mapEntry.getValue();
                 }
             };
         }
 
         @Override
         public void remove() {
             mapIterator.remove();
         }
     }
 
     static class Builder<K, V> extends AbstractCache.Builder<K, V> {
         private final ConfigurableCacheFactory ccf;
 
         public Builder(String cacheName, String cacheManagerName, Set<Class<?>> immutableClasses,
                        ClassLoader classLoader, ConfigurableCacheFactory ccf) {
             this(cacheName, cacheManagerName, immutableClasses, classLoader, new CoherenceCacheConfiguration.Builder(), ccf);
         }
 
         private Builder(String cacheName, String cacheManagerName,
                         Set<Class<?>> immutableClasses,
                         ClassLoader classLoader,
                         CoherenceCacheConfiguration.Builder configurationBuilder,
                         ConfigurableCacheFactory ccf) {
             super(cacheName, cacheManagerName, immutableClasses, classLoader, configurationBuilder);
             if (ccf == null) {
                 throw new NullPointerException("ConfigurableCacheFactory");
             }
             this.ccf = ccf;
         }
 
         @Override
         public CoherenceCache<K, V> build() {
             CacheConfiguration configuration = createCacheConfiguration();
             NamedCache namedCache = ccf.ensureCache(cacheName, classLoader);
             return new CoherenceCache<K, V>(namedCache, cacheName, cacheManagerName, immutableClasses, classLoader,
                     configuration, cacheLoader, cacheWriter);
         }
 
         /**
          * Set the cache loader.
          *
          * @param cacheLoader the CacheLoader
          * @return the builder
          */
         @Override
         public Builder<K, V> setCacheLoader(CacheLoader<K, V> cacheLoader) {
             if (cacheLoader == null) {
                 throw new NullPointerException("cacheLoader");
             }
             this.cacheLoader = cacheLoader;
             return this;
         }
 
         @Override
         public Builder<K, V> registerCacheEntryListener(CacheEntryListener<K, V> listener, NotificationScope scope, boolean synchronous) {
             throw new UnsupportedOperationException();
         }
     }
 
     private static class CoherenceCacheLoaderLoadCallable<K, V> implements Callable<V> {
         private final NamedCache cache;
         private final CacheLoader<K, V> cacheLoader;
         private final K key;
 
         public CoherenceCacheLoaderLoadCallable(NamedCache cache, CacheLoader<K, V> cacheLoader, K key) {
             this.cache = cache;
             this.cacheLoader = cacheLoader;
             this.key = key;
         }
 
         @Override
         public V call() throws Exception {
             Entry<K, V> entry = cacheLoader.load(key);
             if (entry.getValue() == null) {
                 throw new NullPointerException();
             }
             cache.put(entry.getKey(), entry.getValue());
             return entry.getValue();
         }
     }
 
     private static class CoherenceCacheLoaderLoadAllCallable<K, V> implements Callable<Map<K, V>> {
         private final NamedCache cache;
         private final CacheLoader<K, V> cacheLoader;
         private final Collection<? extends K> keys;
 
         CoherenceCacheLoaderLoadAllCallable(NamedCache cache, CacheLoader<K, V> cacheLoader, Collection<? extends K> keys) {
             this.cache = cache;
             this.cacheLoader = cacheLoader;
             this.keys = keys;
         }
 
         @Override
         public Map<K, V> call() throws Exception {
             ArrayList<K> keysNotInStore = new ArrayList<K>();
             for (K key : keys) {
                 if (!cache.containsKey(key)) {
                     keysNotInStore.add(key);
                 }
             }
             Map<K, V> value = cacheLoader.loadAll(keysNotInStore);
             if (value.containsValue(null)) {
                 throw new NullPointerException();
             }
             cache.putAll(value);
             return value;
         }
     }
 }
