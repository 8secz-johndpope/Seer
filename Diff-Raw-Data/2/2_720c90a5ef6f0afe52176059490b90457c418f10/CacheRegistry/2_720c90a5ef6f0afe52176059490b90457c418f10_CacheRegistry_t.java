 package org.lexevs.cache;
 
 import java.io.Serializable;
 import java.util.ArrayList;
 import java.util.Collections;
 import java.util.HashMap;
 import java.util.List;
 import java.util.Map;
 
 import net.sf.ehcache.Cache;
 import net.sf.ehcache.CacheManager;
 import net.sf.ehcache.Ehcache;
 import net.sf.ehcache.Element;
 import net.sf.ehcache.Statistics;
 
 import org.lexevs.logging.LoggerFactory;
 import org.springframework.beans.factory.DisposableBean;
 import org.springframework.beans.factory.InitializingBean;
 
 public class CacheRegistry implements InitializingBean, DisposableBean {
 	
 	private CacheManager cacheManager;
 	
 	/** The caches. */
 	private Map<String,CacheWrapper<String,Object>> caches = new HashMap<String,CacheWrapper<String,Object>>();
 	
 	public void afterPropertiesSet() throws Exception {
 		initializeCache();
 	}
 	
 	@Override
 	public void destroy() throws Exception {
 		LoggerFactory.getLogger().info(
 				getCacheStatisticsStringRepresentation());
 	}
 	
 	protected String getCacheStatisticsStringRepresentation() {
 		StringBuffer sb = new StringBuffer();
 		sb.append("\n===============================");
 		sb.append("\n         Cache Statistics      \n");
 		
 		float hits = 0;
 		float misses = 0;
 
 		for(String cacheName : this.cacheManager.getCacheNames()) {
 			Cache cache = this.cacheManager.getCache(cacheName);
 			
 			Statistics stats = cache.getStatistics();
 			hits += stats.getCacheHits();
 			misses += stats.getCacheMisses();
 			
 			sb.append("\n" + cache.getStatistics().toString());
 		}
 		
 		sb.append("\n\n");
 		sb.append("\nTOTAL STATS:");
 		sb.append("\n - Total Cache Requests: " + (hits + misses));
 		sb.append("\n - Hits: " + hits);
 		sb.append("\n - Misses: " + misses);
 		
		sb.append("\n - Cache Efficiency: " + (hits/(misses+hits)));
 		
 		sb.append("\n===============================\n");
 		
 		return sb.toString();
 	}
 	
 	protected void initializeCache() {
 		for(String cacheName : this.cacheManager.getCacheNames()) {
 			this.caches.put(cacheName, new EhCacheWrapper<String,Object>(cacheName, this.cacheManager));
 		}
 	}
 
 	public void clearAll() {
 		for(CacheWrapper<String,Object> cache : this.caches.values()) {
 			cache.clear();
 		}
 	}
 	
 	public Map<String, CacheWrapper<String, Object>> getCaches() {
 		return Collections.unmodifiableMap(this.caches);
 	}
 
 	public CacheWrapper<String, Object> getCache(String cacheName, boolean createIfNotPresent) {
 		if(! caches.containsKey(cacheName)) {
 			if(!createIfNotPresent){
 				throw new RuntimeException("\n\n\n" +
 						"=============================================\n" +
 						"                Cache Error\n" +
 						" Cache: " + cacheName + " not found.\n" +
 						"=============================================\n\n");
 			} else {
 				if(this.cacheManager.cacheExists(cacheName)) {
 					CacheWrapper<String,Object> cacheWrapper = new EhCacheWrapper<String,Object>(cacheName,this.cacheManager);
 					this.caches.put(cacheName,cacheWrapper);
 					return cacheWrapper;
 				} else {
 					LoggerFactory.getLogger().warn("Using default cache for Cache Name: " + cacheName);
 					this.cacheManager.addCache(cacheName);
 					
 					CacheWrapper<String,Object> cacheWrapper = 
 						new EhCacheWrapper<String,Object>(cacheName,this.cacheManager);
 					this.caches.put(cacheName,cacheWrapper);
 					
 					return cacheWrapper;
 				}
 			}
 		}
 		return this.caches.get(cacheName);
 		
 	}
 
 	public void setCacheManager(CacheManager cacheManager) {
 		this.cacheManager = cacheManager;
 	}
 
 	public CacheManager getCacheManager() {
 		return cacheManager;
 	}
 
 	public interface CacheWrapper<K, V> 
 	{
 		public void put(K key, V value);
 
 		public V get(K key);
 		
 		public void clear();
 		
 		public int size();
 		
 		public List<V> values();
 	}
 
 	public class EhCacheWrapper<K extends Serializable, V> implements CacheWrapper<K, V> {
 		private final String cacheName;
 		private final CacheManager cacheManager;
 
 		public EhCacheWrapper(final String cacheName, final CacheManager cacheManager){
 			this.cacheName = cacheName;
 			this.cacheManager = cacheManager;
 		}
 
 		public void put(final K key, final V value){
 			getCache().put(new Element(key, value));
 		}
 
 		@SuppressWarnings("unchecked")
 		public V get(final K key){
 			Element element = getCache().get(key);
 			if (element != null) {
 				if(element.isSerializable()) {
 					return (V) element.getValue();
 				} else {
 					return (V) element.getObjectValue();
 				}
 			}
 			return null;
 		}
 		
 		public int size() {
 			return getCache().getSize();
 		}
 		
 		public void clear(){
 			getCache().removeAll();
 		}
 		
 		@SuppressWarnings("unchecked")
 		public List<V> values(){
 			List<V> returnList = new ArrayList<V>();
 			
 			List<K> keys = getCache().getKeys();
 			
 			for(K key : keys) {
 				returnList.add(this.get(key));
 			}
 			
 			return returnList;
 		}
 
 		public Ehcache getCache(){
 			return cacheManager.getEhcache(cacheName);
 		}
 	}
 }
