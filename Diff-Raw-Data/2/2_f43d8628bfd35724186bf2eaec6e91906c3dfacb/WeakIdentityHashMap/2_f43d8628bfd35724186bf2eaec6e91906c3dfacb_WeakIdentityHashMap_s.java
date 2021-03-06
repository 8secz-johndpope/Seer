 /* WeakIdentityHashMap -- an identity hashtable that keeps only weak references
  to its keys, allowing the virtual machine to reclaim them
  Copyright (C) 1999, 2000, 2001, 2002, 2003, 2004, 2006 Free Software Foundation, Inc.
 
  This file is part of GNU Classpath.
 
  GNU Classpath is free software; you can redistribute it and/or modify
  it under the terms of the GNU General Public License as published by
  the Free Software Foundation; either version 2, or (at your option)
  any later version.
 
  GNU Classpath is distributed in the hope that it will be useful, but
  WITHOUT ANY WARRANTY; without even the implied warranty of
  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
  General Public License for more details.
 
  You should have received a copy of the GNU General Public License
  along with GNU Classpath; see the file COPYING.  If not, write to the
  Free Software Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA
  02110-1301 USA.
 
  Linking this library statically or dynamically with other modules is
  making a combined work based on this library.  Thus, the terms and
  conditions of the GNU General Public License cover the whole
  combination.
 
  As a special exception, the copyright holders of this library give you
  permission to link this library with independent modules to produce an
  executable, regardless of the license terms of these independent
  modules, and to copy and distribute the resulting executable under
  terms of your choice, provided that you also meet, for each linked
  independent module, the terms and conditions of the license of that
  module.  An independent module is a module which is not derived from
  or based on this library.  If you modify this library, you may extend
  this exception to your version of the library, but you are not
  obligated to do so.  If you do not wish to do so, delete this
  exception statement from your version. */
 
 package com.scratchdisk.util;
 
 import java.lang.ref.ReferenceQueue;
 import java.lang.ref.WeakReference;
 import java.util.AbstractMap;
 import java.util.AbstractSet;
 import java.util.Collection;
 import java.util.ConcurrentModificationException;
 import java.util.Iterator;
 import java.util.Map;
 import java.util.NoSuchElementException;
 import java.util.Set;
 
 /**
  * A weak hash map has only weak references to the key. This means that it
  * allows the key to be garbage collected if it is not used otherwise. If
  * this happens, the entry will eventually disappear from the map,
  * asynchronously.
  *
  * <p>Other strange behaviors to be aware of: The size of this map may
  * spontaneously shrink (even if you use a synchronized map and synchronize
  * it); it behaves as if another thread removes entries from this table
  * without synchronization.  The entry set returned by {@code entrySet}
  * has similar phenomenons: The size may spontaneously shrink, or an
  * entry, that was in the set before, suddenly disappears.
  *
  * <p>A weak hash map is not meant for caches; use a normal map, with
  * soft references as values instead, or try {@link LinkedHashMap}.
  *
  * <p>The weak hash map supports null values and null keys.  The null key
  * is never deleted from the map (except explicitly of course). The
  * performance of the methods are similar to that of a hash map.
  *
  * <p>The value objects are strongly referenced by this table.  So if a
  * value object maintains a strong reference to the key (either direct
  * or indirect) the key will never be removed from this map.  According
  * to Sun, this problem may be fixed in a future release.  It is not
  * possible to do it with the jdk 1.2 reference model, though.
  *
  * @author Jochen Hoenicke
  * @author Eric Blake (ebb9@email.byu.edu)
  * @author Jeroen Frijters
 * @author Jrg Lehni
  *
  * @see HashMap
  * @see WeakReference
  * @see WeakHashMap
  * @see IdentityHashMap
  * @see LinkedHashMap
  */
 public class WeakIdentityHashMap<K,V> extends AbstractMap<K,V> implements Map<K,V> {
 	/**
 	 * The default capacity for an instance of HashMap.
 	 * Sun's documentation mildly suggests that this (11) is the correct
 	 * value.
 	 */
 	private static final int DEFAULT_CAPACITY = 11;
 
 	/**
 	 * The default load factor of a HashMap.
 	 */
 	private static final float DEFAULT_LOAD_FACTOR = 0.75F;
 
 	/**
 	 * This is used instead of the key value <i>null</i>.  It is needed
 	 * to distinguish between an null key and a removed key.
 	 */
 	// Package visible for use by nested classes.
 	static final Object NULL_KEY = new Object();
 
 	/**
 	 * The reference queue where our buckets (which are WeakReferences) are
 	 * registered to.
 	 */
 	private final ReferenceQueue<Object> queue;
 
 	/**
 	 * The number of entries in this hash map.
 	 */
 	// Package visible for use by nested classes.
 	int size;
 
 	/**
 	 * The load factor of this WeakIdentityHashMap.  This is the maximum ratio of
 	 * size versus number of buckets.  If size grows the number of buckets
 	 * must grow, too.
 	 */
 	private float loadFactor;
 
 	/**
 	 * The rounded product of the capacity (i.e. number of buckets) and
 	 * the load factor. When the number of elements exceeds the
 	 * threshold, the HashMap calls {@code rehash()}.
 	 */
 	private int threshold;
 
 	/**
 	 * The number of structural modifications.  This is used by
 	 * iterators, to see if they should fail.  This doesn't count
 	 * the silent key removals, when a weak reference is cleared
 	 * by the garbage collection.  Instead the iterators must make
 	 * sure to have strong references to the entries they rely on.
 	 */
 	// Package visible for use by nested classes.
 	int modCount;
 
 	/**
 	 * The entry set.  There is only one instance per hashmap, namely
 	 * theEntrySet.  Note that the entry set may silently shrink, just
 	 * like the WeakIdentityHashMap.
 	 */
 	private final class WeakEntrySet extends AbstractSet<Map.Entry<K,V>> {
 		/**
 		 * Non-private constructor to reduce bytecode emitted.
 		 */
 		WeakEntrySet() {
 		}
 
 		/**
 		 * Returns the size of this set.
 		 *
 		 * @return the set size
 		 */
 		public int size() {
 			return size;
 		}
 
 		/**
 		 * Returns an iterator for all entries.
 		 *
 		 * @return an Entry iterator
 		 */
 		public Iterator<Map.Entry<K,V>> iterator() {
 			return new Iterator<Map.Entry<K,V>>() {
 				/**
 				 * The entry that was returned by the last
 				 * {@code next()} call.  This is also the entry whose
 				 * bucket should be removed by the {@code remove} call. <br>
 				 *
 				 * It is null, if the {@code next} method wasn't
 				 * called yet, or if the entry was already removed.  <br>
 				 *
 				 * Remembering this entry here will also prevent it from
 				 * being removed under us, since the entry strongly refers
 				 * to the key.
 				 */
 				WeakBucket<K,V>.WeakEntry lastEntry;
 
 				/**
 				 * The entry that will be returned by the next
 				 * {@code next()} call.  It is {@code null} if there
 				 * is no further entry. <br>
 				 *
 				 * Remembering this entry here will also prevent it from
 				 * being removed under us, since the entry strongly refers
 				 * to the key.
 				 */
 				WeakBucket<K,V>.WeakEntry nextEntry = findNext(null);
 
 				/**
 				 * The known number of modification to the list, if it differs
 				 * from the real number, we throw an exception.
 				 */
 				int knownMod = modCount;
 
 				/**
 				 * Check the known number of modification to the number of
 				 * modifications of the table.  If it differs from the real
 				 * number, we throw an exception.
 				 * @throws ConcurrentModificationException if the number
 				 *         of modifications doesn't match.
 				 */
 				private void checkMod() {
 					// This method will get inlined.
 					cleanQueue();
 					if (knownMod != modCount)
 						throw new ConcurrentModificationException(knownMod
 								+ " != " + modCount);
 				}
 
 				/**
 				 * Get a strong reference to the next entry after
 				 * lastBucket.
 				 * @param lastEntry the previous bucket, or null if we should
 				 * get the first entry.
 				 * @return the next entry.
 				 */
 				private WeakBucket<K,V>.WeakEntry findNext(
 						WeakBucket<K,V>.WeakEntry lastEntry) {
 					int slot;
 					WeakBucket<K,V> nextBucket;
 					if (lastEntry != null) {
 						nextBucket = lastEntry.getBucket().next;
 						slot = lastEntry.getBucket().slot;
 					} else {
 						nextBucket = buckets[0];
 						slot = 0;
 					}
 
 					while (true) {
 						while (nextBucket != null) {
 							WeakBucket<K,V>.WeakEntry entry = nextBucket.getEntry();
 							if (entry != null)
 								// This is the next entry.
 								return entry;
 
 							// Entry was cleared, try next.
 							nextBucket = nextBucket.next;
 						}
 
 						slot++;
 						if (slot == buckets.length)
 							// No more buckets, we are through.
 							return null;
 
 						nextBucket = buckets[slot];
 					}
 				}
 
 				/**
 				 * Checks if there are more entries.
 				 * @return true, iff there are more elements.
 				 * @throws ConcurrentModificationException if the hash map was
 				 *         modified.
 				 */
 				public boolean hasNext() {
 					checkMod();
 					return nextEntry != null;
 				}
 
 				/**
 				 * Returns the next entry.
 				 * @return the next entry.
 				 * @throws ConcurrentModificationException if the hash map was
 				 *         modified.
 				 * @throws NoSuchElementException if there is no entry.
 				 */
 				public Entry<K, V> next() {
 					checkMod();
 					if (nextEntry == null)
 						throw new NoSuchElementException();
 					lastEntry = nextEntry;
 					nextEntry = findNext(lastEntry);
 					return lastEntry;
 				}
 
 				/**
 				 * Removes the last returned entry from this set.  This will
 				 * also remove the bucket of the underlying weak hash map.
 				 * @throws ConcurrentModificationException if the hash map was
 				 *         modified.
 				 * @throws IllegalStateException if {@code next()} was
 				 *         never called or the element was already removed.
 				 */
 				public void remove() {
 					checkMod();
 					if (lastEntry == null)
 						throw new IllegalStateException();
 					modCount++;
 					internalRemove(lastEntry.getBucket());
 					lastEntry = null;
 					knownMod++;
 				}
 			};
 		}
 	}
 
 	/**
 	 * A bucket is a weak reference to the key, that contains a strong
 	 * reference to the value, a pointer to the next bucket and its slot
 	 * number. <br>
 	 *
 	 * It would be cleaner to have a WeakReference as field, instead of
 	 * extending it, but if a weak reference gets cleared, we only get
 	 * the weak reference (by queue.poll) and wouldn't know where to
 	 * look for this reference in the hashtable, to remove that entry.
 	 *
 	 * @author Jochen Hoenicke
 	 */
 	private static class WeakBucket<K, V> extends WeakReference<Object> {
 		/**
 		 * The value of this entry.  The key is stored in the weak
 		 * reference that we extend.
 		 */
 		V value;
 
 		/**
 		 * The next bucket describing another entry that uses the same
 		 * slot.
 		 */
 		WeakBucket<K,V> next;
 
 		/**
 		 * The slot of this entry. This should be
 		 * {@code Math.abs(key.hashCode() % buckets.length)}.
 		 *
 		 * But since the key may be silently removed we have to remember
 		 * the slot number.
 		 *
 		 * If this bucket was removed the slot is -1.  This marker will
 		 * prevent the bucket from being removed twice.
 		 */
 		int slot;
 
 		/**
 		 * Creates a new bucket for the given key/value pair and the specified
 		 * slot.
 		 * @param key the key
 		 * @param queue the queue the weak reference belongs to
 		 * @param value the value
 		 * @param slot the slot.  This must match the slot where this bucket
 		 *        will be enqueued.
 		 */
 		public WeakBucket(Object key, ReferenceQueue<Object> queue, V value,
 				int slot) {
 			super(key, queue);
 			this.value = value;
 			this.slot = slot;
 		}
 
 		/**
 		 * This class gives the {@code Entry} representation of the
 		 * current bucket.  It also keeps a strong reference to the
 		 * key; bad things may happen otherwise.
 		 */
 		class WeakEntry implements Map.Entry<K, V> {
 			/**
 			 * The strong reference to the key.
 			 */
 			Object key;
 
 			/**
 			 * Creates a new entry for the key.
 			 * @param key the key
 			 */
 			public WeakEntry(Object key) {
 				this.key = key;
 			}
 
 			/**
 			 * Returns the underlying bucket.
 			 * @return the owning bucket
 			 */
 			public WeakBucket<K,V> getBucket() {
 				return WeakBucket.this;
 			}
 
 			/**
 			 * Returns the key.
 			 * @return the key
 			 */
 			@SuppressWarnings("unchecked")
 			public K getKey() {
 				return key == NULL_KEY ? null : (K) key;
 			}
 
 			/**
 			 * Returns the value.
 			 * @return the value
 			 */
 			public V getValue() {
 				return value;
 			}
 
 			/**
 			 * This changes the value.  This change takes place in
 			 * the underlying hash map.
 			 * @param newVal the new value
 			 * @return the old value
 			 */
 			public V setValue(V newVal) {
 				V oldVal = value;
 				value = newVal;
 				return oldVal;
 			}
 
 			/**
 			 * The hashCode as specified in the Entry interface.
 			 * @return the hash code
 			 */
 			public int hashCode() {
 				return System.identityHashCode(key)
 						^ (value == null ? 0 : value.hashCode());
 			}
 
 			/**
 			 * The equals method as specified in the Entry interface.
 			 * @param o the object to compare to
 			 * @return true if o represents the same key/value pair
 			 */
 			public boolean equals(Object o) {
 				if (o instanceof Map.Entry) {
 					Map.Entry e = (Map.Entry) o;
 					return getKey() == e.getKey()
 							&& (value == null ? e.getValue() == null : value
 									.equals(e.getValue()));
 				}
 				return false;
 			}
 
 			public String toString() {
 				return getKey() + "=" + value;
 			}
 		}
 
 		/**
 		 * This returns the entry stored in this bucket, or null, if the
 		 * bucket got cleared in the mean time.
 		 * @return the Entry for this bucket, if it exists
 		 */
 		WeakEntry getEntry() {
 			final Object key = this.get();
 			if (key == null)
 				return null;
 			return new WeakEntry(key);
 		}
 	}
 
 	/**
 	 * The entry set returned by {@code entrySet()}.
 	 */
 	private final WeakEntrySet theEntrySet;
 
 	/**
 	 * The hash buckets.  These are linked lists. Package visible for use in
 	 * nested classes.
 	 */
 	WeakBucket<K,V>[] buckets;
 
 	/**
 	 * Creates a new weak hash map with default load factor and default
 	 * capacity.
 	 */
 	public WeakIdentityHashMap() {
 		this(DEFAULT_CAPACITY, DEFAULT_LOAD_FACTOR);
 	}
 
 	/**
 	 * Creates a new weak hash map with default load factor and the given
 	 * capacity.
 	 * @param initialCapacity the initial capacity
 	 * @throws IllegalArgumentException if initialCapacity is negative
 	 */
 	public WeakIdentityHashMap(int initialCapacity) {
 		this(initialCapacity, DEFAULT_LOAD_FACTOR);
 	}
 
 	/**
 	 * Creates a new weak hash map with the given initial capacity and
 	 * load factor.
 	 * @param initialCapacity the initial capacity.
 	 * @param loadFactor the load factor (see class description of HashMap).
 	 * @throws IllegalArgumentException if initialCapacity is negative, or
 	 *         loadFactor is non-positive
 	 */
 	@SuppressWarnings("unchecked")
 	public WeakIdentityHashMap(int initialCapacity, float loadFactor) {
 		// Check loadFactor for NaN as well.
 		if (initialCapacity < 0 || !(loadFactor > 0))
 			throw new IllegalArgumentException();
 		if (initialCapacity == 0)
 			initialCapacity = 1;
 		this.loadFactor = loadFactor;
 		threshold = (int) (initialCapacity * loadFactor);
 		theEntrySet = new WeakEntrySet();
 		queue = new ReferenceQueue<Object>();
 		buckets = new WeakBucket[initialCapacity];
 	}
 
 	/**
 	 * Construct a new WeakIdentityHashMap with the same mappings as the given map.
 	 * The WeakIdentityHashMap has a default load factor of 0.75.
 	 *
 	 * @param m the map to copy
 	 * @throws NullPointerException if m is null
 	 * @since 1.3
 	 */
 	public WeakIdentityHashMap(Map<? extends K, ? extends V> m) {
 		this(m.size(), DEFAULT_LOAD_FACTOR);
 		putAll(m);
 	}
 
 	/**
 	 * Simply hashes a non-null Object to its array index.
 	 * @param key the key to hash
 	 * @return its slot number
 	 */
 	private int hash(Object key) {
 		return Math.abs(System.identityHashCode(key) % buckets.length);
 	}
 
 	/**
 	 * Cleans the reference queue.  This will poll all references (which
 	 * are WeakBuckets) from the queue and remove them from this map.
 	 * This will not change modCount, even if it modifies the map.  The
 	 * iterators have to make sure that nothing bad happens.  <br>
 	 *
 	 * Currently the iterator maintains a strong reference to the key, so
 	 * that is no problem.
 	 */
 	// Package visible for use by nested classes.
 	@SuppressWarnings("unchecked")
 	void cleanQueue() {
 		Object bucket = queue.poll();
 		while (bucket != null) {
 			internalRemove((WeakBucket) bucket);
 			bucket = queue.poll();
 		}
 	}
 
 	/**
 	 * Rehashes this hashtable.  This will be called by the
 	 * {@code add()} method if the size grows beyond the threshold.
 	 * It will grow the bucket size at least by factor two and allocates
 	 * new buckets.
 	 */
 	@SuppressWarnings("unchecked")
 	private void rehash() {
 		WeakBucket<K,V>[] oldBuckets = buckets;
 		int newsize = buckets.length * 2 + 1; // XXX should be prime.
 		threshold = (int) (newsize * loadFactor);
 		buckets = new WeakBucket[newsize];
 
 		// Now we have to insert the buckets again.
 		for (int i = 0; i < oldBuckets.length; i++) {
 			WeakBucket<K,V> bucket = oldBuckets[i];
 			WeakBucket<K,V> nextBucket;
 			while (bucket != null) {
 				nextBucket = bucket.next;
 
 				Object key = bucket.get();
 				if (key == null) {
 					// This bucket should be removed; it is probably
 					// already on the reference queue.  We don't insert it
 					// at all, and mark it as cleared.
 					bucket.slot = -1;
 					size--;
 				} else {
 					// Add this bucket to its new slot.
 					int slot = hash(key);
 					bucket.slot = slot;
 					bucket.next = buckets[slot];
 					buckets[slot] = bucket;
 				}
 				bucket = nextBucket;
 			}
 		}
 	}
 
 	/**
 	 * Finds the entry corresponding to key.  Since it returns an Entry
 	 * it will also prevent the key from being removed under us.
 	 * @param key the key, may be null
 	 * @return The WeakBucket.WeakEntry or null, if the key wasn't found.
 	 */
 	private WeakBucket<K,V>.WeakEntry internalGet(Object key) {
 		if (key == null)
 			key = NULL_KEY;
 		int slot = hash(key);
 		WeakBucket<K,V> bucket = buckets[slot];
 		while (bucket != null) {
 			WeakBucket<K,V>.WeakEntry entry = bucket.getEntry();
 			if (entry != null && key == entry.key)
 				return entry;
 
 			bucket = bucket.next;
 		}
 		return null;
 	}
 
 	/**
 	 * Adds a new key/value pair to the hash map.
 	 * @param key the key. This mustn't exists in the map. It may be null.
 	 * @param value the value.
 	 */
 	private void internalAdd(Object key, V value) {
 		if (key == null)
 			key = NULL_KEY;
 		int slot = hash(key);
 		WeakBucket<K,V> bucket = new WeakBucket<K,V>(key, queue, value, slot);
 		bucket.next = buckets[slot];
 		buckets[slot] = bucket;
 		size++;
 	}
 
 	/**
 	 * Removes a bucket from this hash map, if it wasn't removed before
 	 * (e.g. one time through rehashing and one time through reference queue).
 	 * Package visible for use in nested classes.
 	 *
 	 * @param bucket the bucket to remove.
 	 */
 	void internalRemove(WeakBucket<K,V> bucket) {
 		int slot = bucket.slot;
 		if (slot == -1)
 			// This bucket was already removed.
 			return;
 
 		// Mark the bucket as removed.  This is necessary, since the
 		// bucket may be enqueued later by the garbage collection, and
 		// internalRemove will be called a second time.
 		bucket.slot = -1;
 
 		WeakBucket<K,V> prev = null;
 		WeakBucket<K,V> next = buckets[slot];
 		while (next != bucket) {
 			if (next == null)
 				throw new InternalError(
 						"WeakIdentityHashMap in inconsistent state");
 			prev = next;
 			next = prev.next;
 		}
 		if (prev == null)
 			buckets[slot] = bucket.next;
 		else
 			prev.next = bucket.next;
 
 		size--;
 	}
 
 	/**
 	 * Returns the size of this hash map.  Note that the size() may shrink
 	 * spontaneously, if the some of the keys were only weakly reachable.
 	 * @return the number of entries in this hash map.
 	 */
 	public int size() {
 		cleanQueue();
 		return size;
 	}
 
 	/**
 	 * Tells if the map is empty.  Note that the result may change
 	 * Spontaneously, if all of the keys were only weakly reachable.
 	 * @return true, if the map is empty.
 	 */
 	public boolean isEmpty() {
 		cleanQueue();
 		return size == 0;
 	}
 
 	/**
 	 * Tells if the map contains the given key.  Note that the result
 	 * may change spontaneously, if the key was only weakly
 	 * reachable.
 	 * @param key the key to look for
 	 * @return true, if the map contains an entry for the given key.
 	 */
 	public boolean containsKey(Object key) {
 		cleanQueue();
 		return internalGet(key) != null;
 	}
 
 	/**
 	 * Gets the value the key is mapped to.
 	 * @return the value the key was mapped to.  It returns null if
 	 *         the key wasn't in this map, or if the mapped value was
 	 *         explicitly set to null.
 	 */
 	public V get(Object key) {
 		cleanQueue();
 		WeakBucket<K,V>.WeakEntry entry = internalGet(key);
 		return entry == null ? null : entry.getValue();
 	}
 
 	/**
 	 * Adds a new key/value mapping to this map.
 	 * @param key the key, may be null
 	 * @param value the value, may be null
 	 * @return the value the key was mapped to previously.  It returns
 	 *         null if the key wasn't in this map, or if the mapped value
 	 *         was explicitly set to null.
 	 */
 	public V put(K key, V value) {
 		cleanQueue();
 		WeakBucket<K,V>.WeakEntry entry = internalGet(key);
 		if (entry != null)
 			return entry.setValue(value);
 
 		modCount++;
 		if (size >= threshold)
 			rehash();
 
 		internalAdd(key, value);
 		return null;
 	}
 
 	/**
 	 * Removes the key and the corresponding value from this map.
 	 * @param key the key. This may be null.
 	 * @return the value the key was mapped to previously.  It returns
 	 *         null if the key wasn't in this map, or if the mapped value was
 	 *         explicitly set to null.
 	 */
 	public V remove(Object key) {
 		cleanQueue();
 		WeakBucket<K,V>.WeakEntry entry = internalGet(key);
 		if (entry == null)
 			return null;
 
 		modCount++;
 		internalRemove(entry.getBucket());
 		return entry.getValue();
 	}
 
 	/**
 	 * Returns a set representation of the entries in this map.  This
 	 * set will not have strong references to the keys, so they can be
 	 * silently removed.  The returned set has therefore the same
 	 * strange behavior (shrinking size(), disappearing entries) as
 	 * this weak hash map.
 	 * @return a set representation of the entries.
 	 */
 	public Set<Map.Entry<K,V>> entrySet() {
 		cleanQueue();
 		return theEntrySet;
 	}
 
 	/**
 	 * Clears all entries from this map.
 	 */
 	public void clear() {
 		super.clear();
 	}
 
 	/**
 	 * Returns true if the map contains at least one key which points to
 	 * the specified object as a value.  Note that the result
 	 * may change spontaneously, if its key was only weakly reachable.
 	 * @param value the value to search for
 	 * @return true if it is found in the set.
 	 */
 	public boolean containsValue(Object value) {
 		cleanQueue();
 		return super.containsValue(value);
 	}
 
 	/**
 	 * Returns a set representation of the keys in this map.  This
 	 * set will not have strong references to the keys, so they can be
 	 * silently removed.  The returned set has therefore the same
 	 * strange behavior (shrinking size(), disappearing entries) as
 	 * this weak hash map.
 	 * @return a set representation of the keys.
 	 */
 	public Set<K> keySet() {
 		cleanQueue();
 		return super.keySet();
 	}
 
 	/**
 	 * Puts all of the mappings from the given map into this one. If the
 	 * key already exists in this map, its value is replaced.
 	 * @param m the map to copy in
 	 */
 	public void putAll(Map<? extends K, ? extends V> m) {
 		super.putAll(m);
 	}
 
 	/**
 	 * Returns a collection representation of the values in this map.  This
 	 * collection will not have strong references to the keys, so mappings
 	 * can be silently removed.  The returned collection has therefore the same
 	 * strange behavior (shrinking size(), disappearing entries) as
 	 * this weak hash map.
 	 * @return a collection representation of the values.
 	 */
 	public Collection<V> values() {
 		cleanQueue();
 		return super.values();
 	}
 } // class WeakIdentityHashMap
