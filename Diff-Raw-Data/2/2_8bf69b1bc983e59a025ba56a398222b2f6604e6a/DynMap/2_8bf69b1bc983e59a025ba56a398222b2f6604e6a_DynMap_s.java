 /**
  * 
  */
 package com.trendrr.oss;
 
 import java.net.URLEncoder;
 import java.util.ArrayList;
 import java.util.Collection;
 import java.util.Collections;
 import java.util.Date;
 import java.util.HashMap;
 import java.util.HashSet;
 import java.util.Iterator;
 import java.util.List;
 import java.util.Map;
 import java.util.Set;
 import java.util.concurrent.ConcurrentHashMap;
 import java.util.logging.Level;
 import java.util.logging.Logger;
 
 import com.trendrr.json.simple.JSONFormatter;
 import com.trendrr.json.simple.JSONObject;
 import com.trendrr.json.simple.JSONValue;
 
 /**
  * 
  * A dynamic map.
  * 
  * 
  * caching:
  * 
  * set cacheEnabled to use an internal cache of TypeCasted results.
  * This is usefull for frequently read maps, with expensive conversions (i.e. string -> map, or list conversions)
  * Raises the memory footprint somewhat and adds some time to puts and removes 
  * 
  * 
  * 
  * 
  * 
  * 
  * @author Dustin Norlander
  * @created Dec 29, 2010
  * 
  */
 public class DynMap extends HashMap<String,Object>{
 	
 	private static final long serialVersionUID = 6342683643643465570L;
 
 	Logger log = Logger.getLogger(DynMap.class.getCanonicalName());
 	
 	ConcurrentHashMap<String, Object> cache = new ConcurrentHashMap<String, Object>();
 	boolean cacheEnabled = false;
 	
 	/**
 	 * Creates a new dynMap based on the passed in object.  This is just a wrapper
 	 * around DynMapFactory.instance()
 	 * 
 	 * @param object
 	 * @return
 	 */
 	public static DynMap instance(Object object) {
 		return DynMapFactory.instance(object);
 	}
 	
 	/*
 	 * Register Date and DynMap with the json formatter so we get properly encoded strings.
 	 */
 	static {
 		JSONValue.registerFormatter(Date.class, new JSONFormatter() {
 			@Override
 			public String toJSONString(Object value) {
				return IsoDateUtil.getIsoDate((Date)value);
 			}
 		});
 		
 		JSONValue.registerFormatter(DynMap.class, new JSONFormatter() {
 			@Override
 			public String toJSONString(Object value) {
 				return ((DynMap)value).toJSONString();
 			}
 		});
 	}
 	
 	
 	/**
 	 * puts the value if the key is absent (or null).
 	 * @param key
 	 * @param val
 	 */
 	public void putIfAbsent(String key, Object val) {
 		if (this.get(key) == null) {
 			this.put(key, val);
 		}
 	}
 	
 	@Override
 	public Object put(String key, Object val) {
 		this.ejectFromCache(key);
 		return super.put(key, val);
 	}
 	
 	public void removeAll(String...keys) {
 		for (String k : keys) {
 			this.remove(k);
 		}
 	}
 	
 	@Override
 	public Object remove(Object k) {
 		this.ejectFromCache((String)k);
 		return super.remove(k);
 	}
 	
 	private void ejectFromCache(String key) {
 		//TODO: this is a dreadful implementation.
 		Set<String> keys = new HashSet<String>();
 		for (String k : cache.keySet()) {
 			if (k.startsWith(key + ".")) {
 				keys.add(k);
 			}
 		}
 		for (String k : keys) {
 			cache.remove(k);
 		}
 		cache.remove(key);
 	}
 	
 	boolean isCacheEnabled() {
 		return cacheEnabled;
 	}
 
 	void setCacheEnabled(boolean cacheEnabled) {
 		this.cacheEnabled = cacheEnabled;
 	}
 
 	/**
 	 * Gets the requested object from the map.
 	 * 
 	 * this differs from the standard map.get in that you can 
 	 * use the dot operator to get a nested value:
 	 * 
 	 * map.get("key1.key2.key3");
 	 * 
 	 * @param key
 	 * @return
 	 */
 	@Override
 	public Object get(Object k) {
 		String key = (String)k;
 		Object val = super.get(key);
 		if (val != null) {
 			return val;
 		}
 		
 		if (key.contains(".")) {
 			//try to reach into the object..
 			String[] items = key.split("\\.");
 			DynMap cur = this.get(DynMap.class, items[0]);
 			for (int i= 1; i < items.length-1; i++) {				
 				cur = cur.get(DynMap.class, items[i]);
 				
 				if (cur == null)
 					return null;
 			}
 			return cur.get(items[items.length-1]);
 		}
 		return null;
 	}
 	
 	public <T> T get(Class<T> cls, String key) {
 		//cache the result.. 
 		if (this.cacheEnabled) {
 			String cacheKey = key + "." + cls.getCanonicalName(); 
 			if (this.cache.containsKey(cacheKey)) {
 				//null is an acceptable cache result.
 				return (T)this.cache.get(cacheKey);
 			} else {
 				T val = TypeCast.cast(cls,this.get(key));
 				this.cache.put(cacheKey, val);
 				return val;
 			}
 		} 
 		return TypeCast.cast(cls, this.get(key));
 	}
 
 	public <T> T get(Class<T> cls, String key, T defaultValue) {
 		T val = this.get(cls, key);
 		if (val == null )
 			return defaultValue;
 		return val;
 	}
 	
 	public String getString(String key) {
 		return this.get(String.class, key);
 	}
 	
 	public String getString(String key, String defaultValue) {
 		return this.get(String.class, key, defaultValue);
 	}
 	
 	public Integer getInteger(String key) {
 		return this.get(Integer.class, key);
 	}
 	
 	public Integer getInteger(String key, Integer defaultValue) {
 		return this.get(Integer.class, key, defaultValue);
 	}
 	
 	public Double getDouble(String key) {
 		return this.get(Double.class, key);
 	}
 	
 	public Double getDouble(String key, Double defaultValue) {
 		return this.get(Double.class, key, defaultValue);
 	}
 	
 	public Long getLong(String key) {
 		return this.get(Long.class, key);
 	}
 	
 	public Long getLong(String key, Long defaultValue) {
 		return this.get(Long.class, key, defaultValue);
 	}
 	
 	public DynMap getMap(String key) {
 		return this.get(DynMap.class, key);
 	}
 	
 	/**
 	 * Returns a typed list.  See TypeCast.getTypedList
 	 * 
 	 * returns the typed list, or null, never empty.
 	 * @param <T>
 	 * @param cls
 	 * @param key
 	 * @param delimiters
 	 * @return
 	 */
 	public <T> List<T> getList(Class<T> cls, String key, String... delimiters) {
 		//cache the result.. 
 		if (this.cacheEnabled) {
 			String cacheKey = key + ".LIST." + cls.getCanonicalName() + "."; 
 			if (this.cache.containsKey(cacheKey)) {
 				//null is an acceptable cache result.
 				return (List<T>)this.cache.get(cacheKey);
 			} else {
 				List<T> val = TypeCast.toTypedList(cls, this.get(key), delimiters);
 				this.cache.put(cacheKey, val);
 				return val;
 			}
 		} 
 		return TypeCast.toTypedList(cls, this.get(key), delimiters);
 	}
 	
 	/**
 	 * same principle as jquery extend.
 	 * 
 	 * each successive map will override any properties in the one before it. 
 	 * 
 	 * Last map in the params is considered the most important one.
 	 * 
 	 * 
 	 * @param map1
 	 * @param maps
 	 * @return this, allows for chaining
 	 */
 	public DynMap extend(Object map1, Object ...maps) {
 		if (map1 == null)
 			return this;
 		
 		DynMap mp1 = DynMapFactory.instance(map1);
 		this.putAll(mp1);
 		for (Object m : maps) {
 			this.putAll(DynMapFactory.instance(m));
 		}
 		return this;
 	}
 	/**
 	 * returns true if the passed in object map is equivelent 
 	 * to this map.  will check members of lists, String, maps, numbers.
 	 * 
 	 * 
 	 * does not check order
 	 * 
 	 * @param map
 	 * @return
 	 */
 	public boolean equivalent(Object map) {
 		DynMap other = DynMap.instance(map);
 		if (!ListHelper.equivalent(other.keySet(), this.keySet())) {
 			return false;
 		}
 		for (String key : this.keySet()) {
 			Object mine = this.get(key);
 			Object yours = other.get(key);
 //			log.info("mine: " + mine + " VS yours: " + yours);
 			if (mine == null && yours == null)
 				continue;
 			if (mine == null || yours == null) {
 //				log.info("key : " + key + " is null ");
 				return false;
 			}
 			
 			 if (ListHelper.isCollection(mine)) {
 				if (!ListHelper.isCollection(yours)) {
 //					log.info("key : " + key + " is not a collection ");
 					return false;
 				}
 				if (!ListHelper.equivalent(mine, yours)) {
 //					log.info("key : " + key + " collection not equiv ");
 					return false;
 				}
 			} else if (isMap(mine)) {
 				if (!DynMap.instance(mine).equivalent(yours)) {
 //					log.info("key : " + key + " map not equiv ");
 					return false;
 				}
 			} else {
 				//default to string compare.
 				if (!this.getString(key).equals(other.getString(key))) {
 //					log.info("key : " + key + " " + this.getString(key) + " VS " + other.getString(key));
 					return false;
 				}
 			}
 		}
 		return true;
 	}
 	
 	public static boolean isMap(Object obj) {
 		if (obj instanceof Map) 
 			return true;
 		if (Reflection.hasMethod(obj, "toMap"))
 			return true;
 		return false;
 	}
 	
 	public String toJSONString() {
 		return JSONObject.toJSONString(this);
 	}
 	
 	/**
 	 * will return the map as a url encoded string in the form:
 	 * key1=val1&key2=val2& ...
 	 * 
 	 * This can be used as getstring or form-encoded post. 
 	 * Lists are handled as multiple key /value pairs.
 	 * 
 	 * will skip keys that contain null values.
 	 * keys are sorted alphabetically so ordering is consistent
 	 * 
 	 * 
 	 * @return The url encoded string, or empty string.
 	 */
 	public String toURLString() {
 		StringBuilder str = new StringBuilder();
 		
 		boolean amp = false;
 		List<String> keys = new ArrayList<String>();
 		keys.addAll(this.keySet());
 		Collections.sort(keys);
 		
 		for (String key : keys) {
 			try {
 				String k = URLEncoder.encode(key, "utf-8");
 				List<String> vals = this.getList(String.class, key);
 				for (String v : vals) {
 					v = URLEncoder.encode(v, "utf-8");
 					if (v != null) {
 						if (amp)
 							str.append("&");
 						
 						str.append(k);
 						str.append("=");
 						str.append(v);
 						amp = true;
 					}
 				}
 			} catch (Exception x) {
 				log.log(Level.INFO, "Caught", x);
 				continue;
 			}
 		}
 		return str.toString();
 	}
 	
 	private String toXMLStringCollection(java.util.Collection c) {
 		if (c == null)
 			return "";
 
 		String collection = "";
 		for (Object o : c) {
 			collection += "<item>";
 			if (o instanceof DynMap)
 				collection += ((DynMap) o).toXMLString();
 			else if (o instanceof java.util.Collection) {
 				for (Object b : (java.util.Collection) o) {
 					collection += "<item>";
 					if (b instanceof java.util.Collection)
 						collection += this
 								.toXMLStringCollection((java.util.Collection) b);
 					else
 						collection += b.toString();
 					collection += "</item>";
 				}
 			} else if (o instanceof java.util.Map) {
 				DynMap dm = new DynMap();
 				dm.putAll((java.util.Map) o);
 				collection += dm.toXMLString();
 			} else
 				collection += o.toString();
 			collection += "</item>";
 		}
 		return collection;
 	}
 	
 	
 	/**
 	 * Constructs an xml string from this dynmap.  
 	 * @return
 	 */
 	public String toXMLString() {
 		if (this.isEmpty())
 			return null;
 
 		StringBuilder buf = new StringBuilder();
 		Iterator iter = this.entrySet().iterator();
 		while (iter.hasNext()) {
 			Map.Entry entry = (Map.Entry) iter.next();
 			String element = String.valueOf(entry.getKey());
 			buf.append("<" + element + ">");
 			if (entry.getValue() instanceof DynMap) {
 				buf.append(((DynMap) entry.getValue())
 						.toXMLString());
 			} else if ((entry.getValue()) instanceof java.util.Collection) {
 				buf.append(this
 						.toXMLStringCollection((java.util.Collection) entry
 								.getValue()));
 			} else if ((entry.getValue()) instanceof java.util.Map) {
 				DynMap dm = DynMapFactory.instance(entry.getValue());
 				buf.append(dm.toXMLString());
 			} else if ((entry.getValue()) instanceof Date) {
 				buf.append(IsoDateUtil.getIsoDateNoMillis(((Date)entry.getValue())));
 			} else {
 				buf.append(entry.getValue());
 			}
 			buf.append("</" + element + ">");
 		}
 
 		return buf.toString();
 	}
 }
