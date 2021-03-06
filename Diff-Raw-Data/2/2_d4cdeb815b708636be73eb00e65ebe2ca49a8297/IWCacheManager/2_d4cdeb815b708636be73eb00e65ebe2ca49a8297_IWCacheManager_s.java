 package com.idega.idegaweb;
 
 /**
  * Title:        idegaclasses
  * Description:
  * Copyright:    Copyright (c) 2001
  * Company:      idega
  * @author <a href="tryggvi@idega.is">Tryggvi Larusson</a>
  * @version 1.0
  */
 import com.idega.util.text.TextSoap;
 import java.net.URLEncoder;
 import java.io.InputStream;
 import java.io.IOException;
 import java.util.*;
 import java.io.PrintWriter;
 import com.idega.util.caching.Cache;
 import com.idega.util.FileUtil;
 import com.idega.data.IDOLegacyEntity;
 import com.idega.data.CacheableEntity;
 import com.idega.util.StringHandler;
 
 
 public class IWCacheManager {
 
   private static final String IW_CACHEMANAGER_KEY = "iw_cachemanager";
   public static final String IW_ROOT_CACHE_DIRECTORY = "iw_cache";
 
   //private static IWCacheManager instance;
   private Map objectsMap;
   private Map timesMap;
   private Map intervalsMap;
   private Map entityMaps;
   private Map entityMapsKeys;
   private Map _keysMap;
 
   private IWCacheManager() {
   }
 
   public static IWCacheManager getInstance(IWMainApplication iwma){
     IWCacheManager iwcm = (IWCacheManager)iwma.getAttribute(IW_CACHEMANAGER_KEY);
     if(iwcm==null){
       iwcm = new IWCacheManager();
       iwma.setAttribute(IW_CACHEMANAGER_KEY,iwcm);
     }
     return iwcm;
   }
 
   /*public static IWCacheManager getInstance(){
     if(instance==null){
       instance = new IWCacheManager();
     }
     return instance;
   }*/
 
   public boolean isCacheValid(String key){
     if(isNull(key)){
     //System.out.println(" CacheKey is null ");
       return false;
     }
     else{
       long currentTime = System.currentTimeMillis();
       long invalidation = getTimeOfInvalidation(key);
        //System.out.println("Current     : "+currentTime);
        //System.out.println("Invalidation: "+invalidation);
 
       if( (currentTime > invalidation) && (invalidation!=0) ){//invalidation==0 cache forever or until app.server shuts down
         //System.out.println(" currentTime > invalidation ");
         return false;
       }
       else{
         //System.out.println(" currentTime < invalidation ");
         return true;
       }
     }
   }
 
   private boolean isNull(String key){
     return !getObjectsMap().containsKey(key);
   }
 
   public void invalidateCache(String key){
     removeCache(key);
   }
 
   private Map getKeysMap(){
     if(_keysMap==null){
       _keysMap=new HashMap();
     }
     return _keysMap;
   }
 
   public synchronized void registerDerivedKey(String key,String derivedKey){
     List derived = (List) getKeysMap().get(key);
     if(derived==null){
       derived=new Vector();
       getKeysMap().put(key,derived);
     }
     derived.add(derivedKey);
   }
 
   public synchronized void setObject(String key,String derivedKey, Object object,long cacheInterval){
     registerDerivedKey(key,derivedKey);
     setObject(derivedKey,object,cacheInterval);
   }
 
 
   public synchronized void setObject(String key, Object object,long cacheInterval){
     getObjectsMap().put(key,object);
     getTimesMap().put(key,getCurrentTime());
     getIntervalsMap().put(key,new Long(cacheInterval));
   }
 
   public Object getObject(String key){
     return getObjectsMap().get(key);
   }
 
   private Long getCurrentTime(){
     return new Long(System.currentTimeMillis());
   }
 
   private long getTimeOfInvalidation(String key){
     return getTimeOfCacheing(key)+getCacheingInterval(key);
   }
 
   private long getTimeOfCacheing(String key){
     Long time = (Long)getTimesMap().get(key);
     if(time!=null){
       return time.longValue();
     }
     else{
       return 0;
     }
   }
 
   private long getCacheingInterval(String key){
     Long time = (Long)getIntervalsMap().get(key);
     if(time!=null){
       return time.longValue();
     }
     else{
       return 0;
     }
   }
 
   private synchronized void removeCache(String key){
     removeFromGlobalCache(key);
 
     Map map = getKeysMap();
     List derived = (List)map.get(key);
     if(derived!=null){
       Iterator iter = derived.iterator();
       while (iter.hasNext()) {
         String item = (String)iter.next();
         removeFromGlobalCache(item);
       }
     }
   }
 
   private void removeFromGlobalCache(String key){
     getObjectsMap().remove(key);
     getTimesMap().remove(key);
     getIntervalsMap().remove(key);
   }
 
 
   private Map getTimesMap(){
     if(timesMap==null){
       timesMap = new HashMap();
     }
     return timesMap;
   }
 
   private Map getObjectsMap(){
     if(objectsMap==null){
       objectsMap = new java.util.HashMap();
     }
     return objectsMap;
   }
 
   private Map getIntervalsMap(){
     if(intervalsMap==null){
       intervalsMap = new HashMap();
     }
     return intervalsMap;
   }
 //added by Eirikur Hrafnsson, eiki@idega.is
   public Cache getCachedBlobObject( String entityClassString, int id, IWMainApplication iwma){
     //check if this blob has already been cached
     Cache cache = (Cache) getObject(entityClassString+id);
     if( cache == null ) {//if null cache it for next time
      cache = cacheBlob(entityClassString,id,iwma);
     }
     return cache;
   }
 
   private Cache cacheBlob(String entityClassString, int id , IWMainApplication iwma){
     InputStream input = null;
     IDOLegacyEntity entity;
     Cache cacheObject = null;
     try{
       entity = com.idega.data.GenericEntity.getEntityInstance(Class.forName(entityClassString),id);
       input = entity.getInputStreamColumnValue(entity.getLobColumnName());
       String realPath = iwma.getApplicationRealPath()+FileUtil.getFileSeparator()+IW_ROOT_CACHE_DIRECTORY;
      String virtualPath = "/"+IW_ROOT_CACHE_DIRECTORY;
       String fileName = entity.getID()+"_"+entity.getName();
       fileName = TextSoap.findAndCut(fileName," ");//remove spaces
 
       if(input != null ){
         FileUtil.streamToFile(input,realPath,fileName);
         cacheObject = new Cache();
         cacheObject.setEntity(entity);
         cacheObject.setRealPathToFile(realPath+FileUtil.getFileSeparator()+fileName);
         cacheObject.setVirtualPathToFile(virtualPath+"/"+URLEncoder.encode(fileName));//used to url encode here
         setObject(entityClassString+id,cacheObject,0);
       }
 
     }
     catch( Exception e ){
      e.printStackTrace(System.err);
      System.err.println("IWCacheManager : error getting stream from blob");
     }
     finally{
       try{
        if (input != null ) input.close();
       }
       catch(IOException e){
         e.printStackTrace(System.err);
         System.err.println("IWCacheManager : error closing stream");
       }
     }
 
     return cacheObject;
   }
 
   public static void deleteCachedBlobs(IWMainApplication iwma){
     String realPath = iwma.getApplicationRealPath();
     FileUtil.deleteAllFilesInDirectory( realPath+IW_ROOT_CACHE_DIRECTORY );
   }
 
 /** caches a whole table. Must be of type CacheableEntity**/
   public void cacheTable(CacheableEntity entity){
     cacheTable(entity,entity.getCacheKey());
   }
 
 /** caches a single entity of type IDOLegacyEntity **/
   public void cacheEntity(IDOLegacyEntity entity, String cacheKey){
     if( entityMaps == null ){
       entityMaps = new HashMap();
     }
     entityMaps.put(cacheKey, entity);
   }
 
   public IDOLegacyEntity getCachedEntity(String cacheKey){
     if( entityMaps != null ){
       return (IDOLegacyEntity) entityMaps.get(cacheKey);
     }
     else return null;
   }
 
   public void removeCachedEntity(String cacheKey){
     if( entityMaps != null ){
       entityMaps.remove(cacheKey);
     }
   }
 /** caches a whole table and specifies which column to use for a key. Must be of type CacheableEntity**/
   public void cacheTable(CacheableEntity entity, String columnNameForKey){
     cacheTable(entity,columnNameForKey,null);
   }
 
   public void removeTableFromCache(Class entityClass){
     if( entityMaps != null ){
       entityMaps.remove(entityClass);
       if( entityMapsKeys != null ) entityMapsKeys.remove(entityClass);
     }
   }
 
   public Map getCachedTableMap(Class entityClass){
     if( entityMaps!=null ){
        return (Map) entityMaps.get(entityClass);
     }
     else return null;
   }
 
   public void cacheTable(CacheableEntity entity, String columnNameForKey ,String columnNameForSecondKey){
 
     if( entityMaps == null ){
       entityMaps = new HashMap();
       entityMapsKeys = new HashMap();
     }
 
 
     if( entityMaps.get(getCorrectClassForEntity(entity)) == null ){
       Map entityMap = new HashMap();
       Vector keys = new Vector();
 
       IDOLegacyEntity[] e;
       try {
         e = entity.findAll();
         if( (e!= null) && (e.length>0) ){
           boolean hasTwoKeys = false;
           //store key names for update, insert and delete
           keys.addElement(columnNameForKey);
           if( columnNameForSecondKey != null ){
             keys.addElement(columnNameForSecondKey);
             hasTwoKeys = true;
           }
           entityMapsKeys.put(getCorrectClassForEntity(entity),keys);
 
           //traverse through the table and make the entitymap and put it in the master map
           for (int i = 0; i < e.length; i++) {
             if(hasTwoKeys){
               entityMap.put(StringHandler.concatAlphabetically(e[i].getStringColumnValue(columnNameForKey),e[i].getStringColumnValue(columnNameForSecondKey)),e[i]);
             }
             else{
                entityMap.put(e[i].getStringColumnValue(columnNameForKey),e[i]);
             }
           }
           entityMaps.put(getCorrectClassForEntity(entity),entityMap);
           //done!
         }
       }
       catch (Exception ex) {
         ex.printStackTrace(System.err);
       }
     }
 
   }
 
   public IDOLegacyEntity getFromCachedTable(Class entityClass, String value ){
     IDOLegacyEntity entity = null;
 
     if( entityMaps != null ){
       Map entityMap = getEntityMap(entityClass);
       if( entityMap != null ){
        entity = (IDOLegacyEntity) entityMap.get(value);
       }
     }else System.out.println("IWCacheManager entityMaps is null!");
 
     return entity;
   }
 
   public IDOLegacyEntity getFromCachedTable(Class entityClass, String value, String value2 ){
     return getFromCachedTable(entityClass, StringHandler.concatAlphabetically(value,value2));
   }
 
   public Map getEntityMap(Class entityClass){
     Map entityMap = null;
     if( entityMaps != null ){
       Class entityBeanClass = this.getCorrectClassForEntity(entityClass);
       entityMap = (Map) entityMaps.get(entityBeanClass);
     }//else System.out.println("IWCacheManager entityMaps is null!");
     return entityMap;
   }
 
   public Vector getEntityKeyVector(Class entityClass){
     Vector entityKeys = null;
     if( entityMapsKeys != null ){
       entityKeys = (Vector) entityMapsKeys.get(entityClass);
     }
     return entityKeys;
   }
 
 
   public void updateFromCachedTable(IDOLegacyEntity entity){
     Vector keys = getEntityKeyVector(getCorrectClassForEntity(entity));
     if( keys!=null ){
       int length = keys.size();
       if(length==2){
         getEntityMap(getCorrectClassForEntity(entity)).put(StringHandler.concatAlphabetically(entity.getStringColumnValue((String) keys.elementAt(0)),entity.getStringColumnValue((String) keys.elementAt(1))),entity);
       }
       else{
          getEntityMap(getCorrectClassForEntity(entity)).put(entity.getStringColumnValue((String) keys.elementAt(0)),entity);
       }
     }
   }
 
   public void deleteFromCachedTable(IDOLegacyEntity entity){
     Vector keys = getEntityKeyVector(getCorrectClassForEntity(entity));
     if( keys!=null ){
       int length = keys.size();
       if(length==2){
          getEntityMap(getCorrectClassForEntity(entity)).remove(StringHandler.concatAlphabetically(entity.getStringColumnValue((String) keys.elementAt(0)),entity.getStringColumnValue((String) keys.elementAt(1))));
       }
       else{
          getEntityMap(getCorrectClassForEntity(entity)).remove(entity.getStringColumnValue((String) keys.elementAt(0)));
       }
     }
   }
 
   public void insertIntoCachedTable(IDOLegacyEntity entity){
     updateFromCachedTable(entity);
   }
 
 
   private Class getCorrectClassForEntity(IDOLegacyEntity entityInstance){
     return getCorrectClassForEntity(entityInstance.getClass());
   }
 
   private Class getCorrectClassForEntity(Class entityBeanOrInterfaceClass){
       return com.idega.data.IDOLookup.getInterfaceClassFor(entityBeanOrInterfaceClass);
   }
 }
