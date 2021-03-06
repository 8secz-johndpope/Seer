 /*
 
    Derby - Class org.apache.derby.impl.services.cache.ClockPolicy
 
    Licensed to the Apache Software Foundation (ASF) under one or more
    contributor license agreements.  See the NOTICE file distributed with
    this work for additional information regarding copyright ownership.
    The ASF licenses this file to you under the Apache License, Version 2.0
    (the "License"); you may not use this file except in compliance with
    the License.  You may obtain a copy of the License at
 
       http://www.apache.org/licenses/LICENSE-2.0
 
    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
 
  */
 
 package org.apache.derby.impl.services.cache;
 
 import java.util.ArrayList;
 import java.util.concurrent.atomic.AtomicBoolean;
 import java.util.concurrent.atomic.AtomicInteger;
 import org.apache.derby.iapi.error.StandardException;
 import org.apache.derby.iapi.services.cache.Cacheable;
 import org.apache.derby.iapi.services.sanity.SanityManager;
 
 /**
  * Implementation of a replacement policy which uses the clock algorithm. All
  * the cache entries are stored in a circular buffer, called the clock. There
  * is also a clock hand which points to one of the entries in the clock. Each
  * time an entry is accessed, it is marked as recently used. If a new entry is
  * inserted into the cache and the cache is full, the clock hand is moved until
  * it is over a not recently used entry, and that entry is evicted to make
  * space for the new entry. Each time the clock hand sweeps over a recently
  * used entry, it is marked as not recently used, and it will be a candidate
  * for removal the next time the clock hand sweeps over it, unless it has been
  * marked as recently used in the meantime.
  *
  * <p>
  *
  * To allow concurrent access from multiple threads, the methods in this class
  * need to synchronize on a number of different objects:
  *
  * <ul>
  *
  * <li><code>CacheEntry</code> objects must be locked before they can be
  * used</li>
  *
  * <li>accesses to the clock structure (circular buffer + clock hand) should be
  * synchronized on the <code>ArrayList</code> representing the circular
  * buffer</li>
  *
  * <li>accesses to individual <code>Holder</code> objects in the clock
  * structure should be protected by synchronizing on the holder</li>
  *
  * </ul>
  *
  * To avoid deadlocks, we need to ensure that all threads obtain
  * synchronization locks in the same order. <code>CacheEntry</code>'s class
  * javadoc dictates the order when locking <code>CacheEntry</code>
  * objects. Additionally, we require that no thread should obtain any other
  * synchronization locks while it is holding a synchronization lock on the
  * clock structure or on a <code>Holder</code> object. The threads are however
  * allowed to obtain synchronization locks on the clock structure or on a
  * holder while they are locking one or more <code>CacheEntry</code> objects.
  */
 final class ClockPolicy implements ReplacementPolicy {
 
     /** The cache manager for which this replacement policy is used. */
     private final ConcurrentCache cacheManager;
 
     /**
      * The maximum size of the cache. When this size is exceeded, entries must
      * be evicted before new ones are inserted.
      */
     private final int maxSize;
 
     /**
      * The circular clock buffer which holds all the entries in the
      * cache. Accesses to <code>clock</code> and <code>hand</code> must be
      * synchronized on <code>clock</code>.
      */
     private final ArrayList<Holder> clock;
 
     /** The current position of the clock hand. */
     private int hand;
 
     /**
      * The number of free entries. This is the number of objects that have been
      * removed from the cache and whose entries are free to be reused without
      * eviction.
      */
     private final AtomicInteger freeEntries = new AtomicInteger();
 
     /**
      * Tells whether there currently is a thread in the <code>doShrink()</code>
      * or <code>trimToSize()</code> methods. If this variable is
      * <code>true</code> a call to any one of those methods will be a no-op.
      */
     private final AtomicBoolean isShrinking = new AtomicBoolean();
 
     /**
      * Create a new <code>ClockPolicy</code> instance.
      *
      * @param cacheManager the cache manager that requests this policy
      * @param initialSize the initial capacity of the cache
      * @param maxSize the maximum size of the cache
      */
     ClockPolicy(ConcurrentCache cacheManager, int initialSize, int maxSize) {
         this.cacheManager = cacheManager;
         this.maxSize = maxSize;
         clock = new ArrayList<Holder>(initialSize);
     }
 
     /**
      * Insert an entry into the cache. If the maximum size is exceeded, evict a
      * <em>not recently used</em> object from the cache. If there are no
      * entries available for reuse, increase the size of the cache.
      *
      * @param entry the entry to insert (must be locked)
      * @return callback object used by the cache manager
      * @exception StandardException if an error occurs when inserting the entry
      */
     public Callback insertEntry(CacheEntry entry) throws StandardException {
 
         final int size;
         synchronized (clock) {
             size = clock.size();
             if (size < maxSize) {
                 if (freeEntries.get() == 0) {
                     // We have not reached the maximum size yet, and there's no
                     // free entry to reuse. Make room by growing.
                     return grow(entry);
                 }
             }
         }
 
         if (size > maxSize) {
             // Maximum size is exceeded. Shrink the clock in the background
             // cleaner, if we have one; otherwise, shrink it in the current
             // thread.
             BackgroundCleaner cleaner = cacheManager.getBackgroundCleaner();
             if (cleaner != null) {
                 cleaner.scheduleShrink();
             } else {
                 doShrink();
             }
         }
 
         // Rotate the clock hand (look at up to 20% of the cache) and try to
         // find free space for the entry. Only allow evictions if the cache
         // has reached its maximum size. Otherwise, we only look for invalid
         // entries and rather grow the cache than evict valid entries.
         Holder h = rotateClock(entry, (float) 0.2, size >= maxSize);
         if (h != null) {
             return h;
         }
 
         // didn't find a victim, so we need to grow
         synchronized (clock) {
             return grow(entry);
         }
     }
 
     /**
      * Holder class which represents an entry in the cache. It maintains a
      * <code>recentlyUsed</code> required by the clock algorithm. The class
      * also implements the <code>Callback</code> interface, so that
      * <code>ConcurrentCache</code> can notify the clock policy about events
      * relevant to the clock algorithm.
      */
     private class Holder implements Callback {
         /**
          * Flag indicating whether or not this entry has been accessed
          * recently. Should only be accessed/modified when the current thread
          * has locked the <code>CacheEntry</code> object stored in the
          * <code>entry</code> field.
          */
         boolean recentlyUsed;
 
         /**
          * Reference to the <code>CacheEntry</code> object held by this
          * object. The reference should only be accessed when the thread owns
          * the monitor on this holder. A thread is only allowed to change the
          * reference if it also has locked the entry that the reference points
          * to (if the reference is non-null). This ensures that no other thread
          * can disassociate a holder from its entry while the entry is locked,
          * even though the monitor on the holder has been released.
          */
         private CacheEntry entry;
 
         /**
          * Cacheable object from a removed object. If this object is non-null,
          * <code>entry</code> must be <code>null</code> (which means that the
          * holder is not associated with any object in the cache).
          */
         private Cacheable freedCacheable;
 
         /**
          * Flag which tells whether this holder has been evicted from the
          * clock. If it has been evicted, it can't be reused when a new entry
          * is inserted. Only the owner of this holder's monitor is allowed to
          * access this variable.
          */
         private boolean evicted;
 
         Holder(CacheEntry e) {
             entry = e;
             e.setCallback(this);
         }
 
         /**
          * Mark this entry as recently used. Caller must have locked
          * <code>entry</code>.
          */
         public void access() {
             recentlyUsed = true;
         }
 
         /**
          * Mark this object as free and reusable. Caller must have locked
          * <code>entry</code>.
          */
         public synchronized void free() {
             freedCacheable = entry.getCacheable();
             entry = null;
             recentlyUsed = false;
             // let others know that a free entry is available
             int free = freeEntries.incrementAndGet();
             if (SanityManager.DEBUG) {
                 SanityManager.ASSERT(
                     free > 0,
                     "freeEntries should be greater than 0, but is " + free);
             }
         }
 
         /**
          * Associate this holder with the specified entry if the holder is free
          * (that is, not associated with any other entry).
          *
          * @param e the entry to associate the holder with (it must be locked
          * by the current thread)
          * @return <code>true</code> if the holder has been associated with the
          * specified entry, <code>false</code> if someone else has taken it or
          * the holder has been evicted from the clock
          */
         synchronized boolean takeIfFree(CacheEntry e) {
             if (entry == null && !evicted) {
                 // the holder is free - take it!
                 int free = freeEntries.decrementAndGet();
                 if (SanityManager.DEBUG) {
                     SanityManager.ASSERT(
                         free >= 0, "freeEntries is negative: " + free);
                 }
                 e.setCacheable(freedCacheable);
                 e.setCallback(this);
                 entry = e;
                 freedCacheable = null;
                 return true;
             }
             // someone else has taken it
             return false;
         }
 
         /**
          * Returns the entry that is currently associated with this holder.
          *
          * @return the associated entry
          */
         synchronized CacheEntry getEntry() {
             return entry;
         }
 
         /**
          * Switch which entry the holder is associated with. Will be called
          * when we evict an entry to make room for a new one. When this method
          * is called, the current thread must have locked both the entry that
          * is evicted and the entry that is inserted.
          *
          * @param e the entry to associate this holder with
          */
         synchronized void switchEntry(CacheEntry e) {
             e.setCallback(this);
             e.setCacheable(entry.getCacheable());
             entry = e;
         }
 
         /**
          * Evict this holder from the clock if it is not associated with an
          * entry.
          *
          * @return <code>true</code> if the holder was successfully evicted,
          * <code>false</code> otherwise
          */
         synchronized boolean evictIfFree() {
             if (entry == null && !evicted) {
                 int free = freeEntries.decrementAndGet();
                 if (SanityManager.DEBUG) {
                     SanityManager.ASSERT(
                         free >= 0, "freeEntries is negative: " + free);
                 }
                 evicted = true;
                 return true;
             }
             return false;
         }
 
         /**
          * Mark this holder as evicted from the clock, effectively preventing
          * reuse of the holder. Calling thread must have locked the holder's
          * entry.
          */
         synchronized void setEvicted() {
             if (SanityManager.DEBUG) {
                 SanityManager.ASSERT(!evicted, "Already evicted");
             }
             evicted = true;
             entry = null;
         }
 
         /**
          * Check whether this holder has been evicted from the clock.
          *
          * @return <code>true</code> if it has been evicted, <code>false</code>
          * otherwise
          */
         synchronized boolean isEvicted() {
             return evicted;
         }
     }
 
     /**
      * Get the holder under the clock hand, and move the hand to the next
      * holder.
      *
      * @return the holder under the clock hand
      */
     private Holder moveHand() {
         synchronized (clock) {
             if (hand >= clock.size()) {
                 hand = 0;
             }
             return clock.get(hand++);
         }
     }
 
     /**
      * Increase the size of the clock by one and return a new holder. The
      * caller must be synchronized on <code>clock</code>.
      *
      * @param entry the entry to insert into the clock
      * @return a new holder which wraps the entry
      */
     private Holder grow(CacheEntry entry) {
         Holder h = new Holder(entry);
         clock.add(h);
         return h;
     }
 
     /**
      * Rotate the clock in order to find a free space for a new entry. If
      * <code>allowEvictions</code> is <code>true</code>, an not recently used
      * object might be evicted to make room for the new entry. Otherwise, only
      * unused entries are searched for. When evictions are allowed, entries are
      * marked as not recently used when the clock hand sweeps over them. The
      * search stops when a reusable entry is found, or when as many entries as
      * specified by <code>partOfClock</code> have been checked. If there are
      * free (unused) entries, the search will continue until a reusable entry
      * is found, regardless of how many entries that need to be checked.
      *
      * @param entry the entry to insert
      * @param partOfClock how large part of the clock to look at before we give
      * up
      * @param allowEvictions tells whether evictions are allowed (normally
      * <code>true</code> if the cache is full and <code>false</code> otherwise)
      * @return a holder that we can reuse, or <code>null</code> if we didn't
      * find one
      */
     private Holder rotateClock(CacheEntry entry, float partOfClock,
                                boolean allowEvictions)
             throws StandardException {
 
         // calculate how many items to check
         int itemsToCheck;
         if (allowEvictions) {
             final int size;
             synchronized (clock) {
                 size = clock.size();
             }
             if (size < 20) {
                 // if we have a very small cache, allow two rounds before
                 // giving up
                 itemsToCheck = size * 2;
             } else {
                 // otherwise, just check a fraction of the clock
                 itemsToCheck = (int) (size * partOfClock);
             }
         } else {
             // we don't allow evictions, so we shouldn't check any items unless
             // there are unused ones
             itemsToCheck = 0;
         }
 
         // Check up to itemsToCheck entries before giving up, but don't give up
         // if we know there are unused entries.
         while (itemsToCheck-- > 0 || freeEntries.get() > 0) {
 
             final Holder h = moveHand();
             final CacheEntry e = h.getEntry();
 
             if (e == null) {
                 if (h.takeIfFree(entry)) {
                     return h;
                 }
                 // Someone else grabbed this entry between the calls to
                 // getEntry() and takeIfFree(). Just move on to the next entry.
                 continue;
             }
 
             if (!allowEvictions) {
                 // Evictions are not allowed, so we can't reuse this entry.
                 continue;
             }
 
             // This variable will hold a dirty cacheable that should be cleaned
             // after the try/finally block.
             final Cacheable dirty;
 
             e.lock();
             try {
                 if (h.getEntry() != e) {
                     // Someone else evicted this entry before we obtained the
                     // lock. Move on to the next entry.
                     continue;
                 }
 
                 if (e.isKept()) {
                     // The entry is in use. Move on to the next entry.
                     continue;
                 }
 
                 if (SanityManager.DEBUG) {
                     // At this point the entry must be valid. If it's not, it's
                     // either removed (in which case we shouldn't get here), or
                     // it is setting it's identity (in which case it is kept,
                     // and we shouldn't get here).
                     SanityManager.ASSERT(e.isValid(),
                             "Holder contains invalid entry");
                     SanityManager.ASSERT(!h.isEvicted(),
                             "Trying to reuse an evicted holder");
                 }
 
                 if (h.recentlyUsed) {
                     // The object has been used recently. Clear the
                     // recentlyUsed flag and move on to the next entry.
                     h.recentlyUsed = false;
                     continue;
                 }
 
                 // The entry is not in use, and has not been used for at least
                 // one round on the clock. See if it needs to be cleaned.
                 Cacheable c = e.getCacheable();
                 if (!c.isDirty()) {
                     // Not in use and not dirty. Take over the holder.
                     h.switchEntry(entry);
                     cacheManager.evictEntry(c.getIdentity());
                     return h;
                 }
 
                 // Ask the background cleaner to clean the entry.
                 BackgroundCleaner cleaner = cacheManager.getBackgroundCleaner();
                 if (cleaner != null && cleaner.scheduleClean(e)) {
                     // Successfully scheduled the clean operation. Move on to
                     // the next entry.
                     continue;
                 }
 
                 // There is no background cleaner, or the background cleaner
                 // has no free capacity. Let's clean the object ourselves.
                 // First, mark the entry as kept to prevent eviction until
                 // we have cleaned it, but don't mark it as accessed (recently
                 // used).
                 e.keep(false);
                 dirty = c;
 
             } finally {
                 e.unlock();
             }
 
             // Clean the entry and unkeep it.
             cacheManager.cleanAndUnkeepEntry(e, dirty);
         }
 
         return null;
     }
 
     /**
      * Remove the holder at the given clock position.
      *
      * @param pos position of the holder
      * @param h the holder to remove
      */
     private void removeHolder(int pos, Holder h) {
         synchronized (clock) {
             Holder removed = clock.remove(pos);
             if (SanityManager.DEBUG) {
                 SanityManager.ASSERT(removed == h, "Wrong Holder removed");
             }
         }
     }
 
     /**
      * Try to shrink the clock if it's larger than its maximum size.
      */
     public void doShrink() {
         // If we're already performing a shrink, ignore this request. We'll get
         // a new call later by someone else if the current shrink operation is
         // not enough.
         if (isShrinking.compareAndSet(false, true)) {
             try {
                 if (shrinkMe()) {
                     // the clock shrunk, try to trim it too
                     trimMe();
                 }
             } finally {
                 isShrinking.set(false);
             }
         }
     }
 
     /**
      * Try to reduce the size of the clock as much as possible by removing
      * invalid entries. In most cases, this method will do nothing.
      *
      * @see #trimMe()
      */
     public void trimToSize() {
         // ignore this request if we're already performing trim or shrink
         if (isShrinking.compareAndSet(false, true)) {
             try {
                 trimMe();
             } finally {
                 isShrinking.set(false);
             }
         }
     }
 
     /**
      * Perform the shrinking of the clock. This method should only be called
      * by a single thread at a time, and should not be called concurrently
      * with <code>trimMe()</code>.
      *
     * @return <code>true</code> if the
      */
     private boolean shrinkMe() {
 
         if (SanityManager.DEBUG) {
             SanityManager.ASSERT(isShrinking.get(),
                     "Called shrinkMe() without ensuring exclusive access");
         }
 
         // Look at 10% of the cache to find candidates for shrinking
         int maxLooks = Math.max(1, maxSize / 10);
 
         // Since we don't scan the entire cache, start at the clock hand so
         // that we don't always scan the first 10% of the cache.
         int pos;
         synchronized (clock) {
             pos = hand;
         }
 
         boolean shrunk = false;
 
         while (maxLooks-- > 0) {
 
             final Holder h;
             final int size;
 
             // The index of the holder we're looking at. Since no one else than
             // us can remove elements from the clock while we're in this
             // method, and new elements will be added at the end of the list,
             // the index for a holder does not change until we remove it.
             final int index;
 
             synchronized (clock) {
                 size = clock.size();
                 if (pos >= size) {
                     pos = 0;
                 }
                 index = pos++;
                 h = clock.get(index);
             }
 
             // No need to shrink if the size isn't greater than maxSize.
             if (size <= maxSize) {
                 break;
             }
 
             final CacheEntry e = h.getEntry();
 
             if (e == null) {
                 // The holder does not hold an entry. Try to remove it.
                 if (h.evictIfFree()) {
                     removeHolder(index, h);
                     shrunk = true;
                     // move position back because of the removal so that we
                     // don't skip one clock element
                     pos = index;
                 }
                 // Either the holder was evicted, or someone else took it
                 // before we could evict it. In either case, we should move on
                 // to the next holder.
                 continue;
             }
 
             e.lock();
             try {
                 if (h.getEntry() != e) {
                     // Entry got evicted before we got the lock. Move on.
                     continue;
                 }
 
                 if (e.isKept()) {
                     // Don't evict entries currently in use.
                     continue;
                 }
 
                 if (SanityManager.DEBUG) {
                     // At this point the entry must be valid. If it's not, it's
                     // either removed (in which case we shouldn't get here), or
                     // it is setting it's identity (in which case it is kept,
                     // and we shouldn't get here).
                     SanityManager.ASSERT(e.isValid(),
                             "Holder contains invalid entry");
                     SanityManager.ASSERT(!h.isEvicted(),
                             "Trying to evict already evicted holder");
                 }
 
                 if (h.recentlyUsed) {
                     // Don't evict recently used entries.
                     continue;
                 }
 
                 final Cacheable c = e.getCacheable();
                 if (c.isDirty()) {
                     // Don't evict dirty entries.
                     continue;
                 }
 
                 // mark as evicted to prevent reuse
                 h.setEvicted();
 
                 // remove from cache manager
                 cacheManager.evictEntry(c.getIdentity());
 
                 // remove from clock
                 removeHolder(index, h);
 
                 // move position back because of the removal so that we don't
                 // skip one clock element
                 pos = index;
 
                 shrunk = true;
 
             } finally {
                 e.unlock();
             }
         }
 
         return shrunk;
     }
 
     /**
      * The number of times <code>trimMe()</code> has been called since the last
      * time <code>trimMe()</code> tried to do some real work. This variable is
      * used by <code>trimMe()</code> to decide whether it's about time to
      * actually do something.
      */
     private int trimRequests;
 
     /**
      * Perform the trimming of the clock. This method should only be called by
      * a single thread at a time, and should not be called concurrently with
      * <code>shrinkMe()</code>.
      *
      * This method will not do anything unless it has been called a substantial
      * number of times. Also, it won't do anything if less than 25% of the
      * clock entries are unused.
      */
     private void trimMe() {
 
         if (SanityManager.DEBUG) {
             SanityManager.ASSERT(isShrinking.get(),
                     "Called trimMe() without ensuring exclusive access");
         }
 
         // Only trim the clock occasionally, as it's an expensive operation.
         if (++trimRequests < maxSize / 8) {
             return;
         }
         trimRequests = 0;
 
         // Get the current size of the clock.
         final int size;
         synchronized (clock) {
             size = clock.size();
         }
 
         // no need to trim a small clock
         if (size < 32) {
             return;
         }
 
         final int unused = freeEntries.get();
 
         if (unused < size / 4) {
             // don't trim unless more than 25% of the entries are unused
             return;
         }
 
         // We still want 10% unused entries as a pool for new objects.
         final int minUnused = (size - unused) / 10;
 
         // Search for unused entries from the end since it's cheaper to remove
         // elements near the end of an ArrayList. Since no one else can shrink
         // the cache while we are in this method, we know that the size of the
         // clock still must be the same as or greater than the size variable,
         // so it's OK to search from position (size-1).
         for (int i = size - 1; i >= 0 && freeEntries.get() > minUnused; i--) {
             final Holder h;
             synchronized (clock) {
                 h = clock.get(i);
             }
             // Index will be stable since no one else is allowed to remove
             // elements from the list, and new elements will be appended at the
             // end of the list.
             if (h.evictIfFree()) {
                 removeHolder(i, h);
             }
         }
 
         // Finally, trim the underlying array.
         synchronized (clock) {
             clock.trimToSize();
         }
     }
 }
