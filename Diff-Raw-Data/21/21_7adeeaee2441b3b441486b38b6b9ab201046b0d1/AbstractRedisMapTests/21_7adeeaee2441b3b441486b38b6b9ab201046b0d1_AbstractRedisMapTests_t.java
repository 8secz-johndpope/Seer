 /*
  * Copyright 2010 the original author or authors.
  * 
  * Licensed under the Apache License, Version 2.0 (the "License");
  * you may not use this file except in compliance with the License.
  * You may obtain a copy of the License at
  * 
  *      http://www.apache.org/licenses/LICENSE-2.0
  * 
  * Unless required by applicable law or agreed to in writing, software
  * distributed under the License is distributed on an "AS IS" BASIS,
  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  * See the License for the specific language governing permissions and
  * limitations under the License.
  */
 package org.springframework.data.keyvalue.redis.util;
 
 import static org.hamcrest.CoreMatchers.*;
 import static org.junit.Assert.*;
 import static org.junit.matchers.JUnitMatchers.*;
 
 import java.util.Collection;
 import java.util.Iterator;
 import java.util.LinkedHashMap;
 import java.util.LinkedHashSet;
 import java.util.Map;
 import java.util.Set;
 import java.util.UUID;
 import java.util.Map.Entry;
 
 import org.junit.After;
 import org.junit.AfterClass;
 import org.junit.Before;
 import org.junit.Test;
 import org.junit.runner.RunWith;
 import org.junit.runners.Parameterized;
 import org.springframework.beans.factory.DisposableBean;
 import org.springframework.dao.InvalidDataAccessApiUsageException;
 import org.springframework.data.keyvalue.redis.connection.RedisConnection;
 import org.springframework.data.keyvalue.redis.connection.RedisConnectionFactory;
 import org.springframework.data.keyvalue.redis.core.RedisCallback;
 import org.springframework.data.keyvalue.redis.core.RedisOperations;
 import org.springframework.data.keyvalue.redis.core.RedisTemplate;
 
 /**
  * Integration test for Redis Map.
  * 
  * @author Costin Leau
  */
 @RunWith(Parameterized.class)
 public abstract class AbstractRedisMapTests<K, V> {
 
 	protected RedisMap<K, V> map;
 	protected ObjectFactory<K> keyFactory;
 	protected ObjectFactory<V> valueFactory;
 	protected RedisTemplate template;
 
 	private static Set<RedisConnectionFactory> connFactories = new LinkedHashSet<RedisConnectionFactory>();
 
 	abstract RedisMap<K, V> createMap();
 
 	@Before
 	public void setUp() throws Exception {
 		map = createMap();
 	}
 
 	public AbstractRedisMapTests(ObjectFactory<K> keyFactory, ObjectFactory<V> valueFactory, RedisTemplate template) {
 		this.keyFactory = keyFactory;
 		this.valueFactory = valueFactory;
 		this.template = template;
 		connFactories.add(template.getConnectionFactory());
 	}
 
 	@AfterClass
 	public static void cleanUp() {
 		if (connFactories != null) {
 			for (RedisConnectionFactory connectionFactory : connFactories) {
 				try {
 					((DisposableBean) connectionFactory).destroy();
 					System.out.println("Succesfully cleaned up factory " + connectionFactory);
 				} catch (Exception ex) {
 					System.err.println("Cannot clean factory " + connectionFactory + ex);
 				}
 			}
 		}
 	}
 
 	protected K getKey() {
 		return keyFactory.instance();
 	}
 
 	protected V getValue() {
 		return valueFactory.instance();
 	}
 
 	protected RedisStore<String> copyStore(RedisStore<String> store) {
 		return new DefaultRedisMap(store.getKey(), store.getOperations());
 	}
 
 	@After
 	public void tearDown() throws Exception {
 		// remove the collection entirely since clear() doesn't always work
 		map.getOperations().delete(map.getKey());
 		template.execute(new RedisCallback<Object>() {
 
 			@Override
 			public Object doInRedis(RedisConnection connection) {
 				connection.flushDb();
 				return null;
 			}
 		});
 	}
 
 	@Test
 	public void testClear() {
 		map.clear();
 		assertEquals(0, map.size());
 		map.put(getKey(), getValue());
 		assertEquals(1, map.size());
 		map.clear();
 		assertEquals(0, map.size());
 	}
 
 	@Test
 	public void testContainsKey() {
 		K k1 = getKey();
 		K k2 = getKey();
 
 		assertFalse(map.containsKey(k1));
 		assertFalse(map.containsKey(k2));
 		map.put(k1, getValue());
 		assertTrue(map.containsKey(k1));
 		map.put(k2, getValue());
 		assertTrue(map.containsKey(k2));
 	}
 
 	@Test(expected = UnsupportedOperationException.class)
 	public void testContainsValue() {
 		V v1 = getValue();
 		V v2 = getValue();
 
 		assertFalse(map.containsValue(v1));
 		assertFalse(map.containsValue(v2));
 		map.put(getKey(), v1);
 		assertTrue(map.containsValue(v1));
 		map.put(getKey(), v2);
 		assertTrue(map.containsValue(v2));
 	}
 
 	public Set<Entry<K, V>> entrySet() {
 		return map.entrySet();
 	}
 
 	@Test
 	public void testEquals() {
 		RedisStore<String> clone = copyStore(map);
 		assertEquals(clone, map);
 		assertEquals(clone, clone);
 		assertEquals(map, map);
 	}
 
 	@Test
 	public void testNotEquals() {
 		RedisOperations<String, ?> ops = map.getOperations();
 		RedisStore<String> newInstance = new DefaultRedisMap<K, V>(ops.<K, V> forHash(map.getKey() + ":new"));
 		assertFalse(map.equals(newInstance));
 		assertFalse(newInstance.equals(map));
 	}
 
 	@Test
 	public void testGet() {
 		K k1 = getKey();
 		V v1 = getValue();
 
 		assertNull(map.get(UUID.randomUUID()));
 		assertNull(map.get(k1));
 		map.put(k1, v1);
 		assertEquals(v1, map.get(k1));
 	}
 
 	@Test
 	public void testGetKey() {
 		assertNotNull(map.getKey());
 	}
 
 	@Test
 	public void testGetOperations() {
 		assertEquals(template, map.getOperations());
 	}
 
 	@Test
 	public void testHashCode() {
 		assertThat(map.hashCode(), not(equalTo(map.getKey().hashCode())));
 		assertEquals(map.hashCode(), copyStore(map).hashCode());
 	}
 
 	@Test(expected = InvalidDataAccessApiUsageException.class)
 	public void testIncrement() {
 		K k1 = getKey();
 		V v1 = getValue();
 
 		map.put(k1, v1);
 		Integer value = map.increment(k1, 1);
 		System.out.println("Value is " + value);
 	}
 
 	@Test
 	public void testIsEmpty() {
 		map.clear();
 		assertTrue(map.isEmpty());
 		map.put(getKey(), getValue());
 		assertFalse(map.isEmpty());
 		map.clear();
 		assertTrue(map.isEmpty());
 	}
 
 	@Test
 	public void testKeySet() {
 		map.clear();
 		assertTrue(map.keySet().isEmpty());
 		K k1 = getKey();
 		K k2 = getKey();
 		K k3 = getKey();
 
 		map.put(k1, getValue());
 		map.put(k2, getValue());
 		map.put(k3, getValue());
 
 		Iterator<K> iterator = map.keySet().iterator();
 		assertEquals(k1, iterator.next());
 		assertEquals(k2, iterator.next());
 		assertEquals(k3, iterator.next());
 		assertFalse(iterator.hasNext());
 	}
 
 	@Test
 	public void testPut() {
 		K k1 = getKey();
 		K k2 = getKey();
 		V v1 = getValue();
 		V v2 = getValue();
 
 		map.put(k1, v1);
 		map.put(k2, v2);
 
 		assertEquals(v1, map.get(k1));
 		assertEquals(v2, map.get(k2));
 	}
 
 	@Test
 	public void testPutAll() {
 		Map<K, V> m = new LinkedHashMap<K, V>();
 		K k1 = getKey();
 		K k2 = getKey();
 
 		V v1 = getValue();
 		V v2 = getValue();
 
 		m.put(k1, v1);
 		m.put(k2, v2);
 
 		assertNull(map.get(k1));
 		assertNull(map.get(k2));
 
 		map.putAll(m);
 
 		assertEquals(v1, map.get(k1));
 		assertEquals(v2, map.get(k2));
 	}
 
 	@Test
 	public void testPutIfAbsent() {
 		K k1 = getKey();
 		K k2 = getKey();
 
 		V v1 = getValue();
 		V v2 = getValue();
 
 		assertNull(map.get(k1));
 		assertTrue(map.putIfAbsent(k1, v1));
 		assertFalse(map.putIfAbsent(k1, v2));
 		assertEquals(v1, map.get(k1));
 
 		assertTrue(map.putIfAbsent(k2, v2));
 		assertFalse(map.putIfAbsent(k2, v1));
 
 		assertEquals(v2, map.get(k2));
 	}
 
 	@Test
 	public void testRemove() {
 		K k1 = getKey();
 		K k2 = getKey();
 
 		V v1 = getValue();
 		V v2 = getValue();
 
 		assertNull(map.remove(k1));
 		assertNull(map.remove(k2));
 
 		map.put(k1, v1);
 		map.put(k2, v2);
 
 		assertEquals(v1, map.remove(k1));
 		assertNull(map.remove(k1));
 		assertNull(map.get(k1));
 
 		assertEquals(v2, map.remove(k2));
 		assertNull(map.remove(k2));
 		assertNull(map.get(k2));
 	}
 
 	@Test
 	public void testSize() {
 		assertEquals(0, map.size());
 		map.put(getKey(), getValue());
 		assertEquals(1, map.size());
 		K k = getKey();
 		map.put(k, getValue());
 		assertEquals(2, map.size());
 		map.remove(k);
 		assertEquals(1, map.size());
 
 		map.clear();
 		assertEquals(0, map.size());
 	}
 
 	@Test
 	public void testValues() {
 		V v1 = getValue();
 		V v2 = getValue();
 		V v3 = getValue();
 
 		map.put(getKey(), v1);
 		map.put(getKey(), v2);
 
 		Collection<V> values = map.values();
 		assertEquals(2, values.size());
 		assertThat(values, hasItems(v1, v2));
 
 		map.put(getKey(), v3);
 		values = map.values();
 		assertEquals(3, values.size());
 		assertThat(values, hasItems(v1, v2, v3));
 	}

	@Test(expected = UnsupportedOperationException.class)
	public void testEntrySet() {
		map.entrySet();
	}
 }
