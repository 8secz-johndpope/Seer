 /*
  *  Copyright (c) 2012 Jan Kotek
  *
  *  Licensed under the Apache License, Version 2.0 (the "License");
  *  you may not use this file except in compliance with the License.
  *  You may obtain a copy of the License at
  *
  *    http://www.apache.org/licenses/LICENSE-2.0
  *
  *  Unless required by applicable law or agreed to in writing, software
  *  distributed under the License is distributed on an "AS IS" BASIS,
  *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  *  See the License for the specific language governing permissions and
  *  limitations under the License.
  */
 
 package org.mapdb;
 
 import java.io.File;
 import java.io.IOError;
 import java.io.IOException;
 import java.io.UnsupportedEncodingException;
 import java.util.NavigableSet;
 import java.util.Set;
 
 import org.jetbrains.annotations.NotNull;
 import org.mapdb.EngineWrapper.*;
 
 /**
  * A builder class for creating and opening a database.
  *
  * @author Jan Kotek
  */
 public class DBMaker {
 
 
     protected static final byte CACHE_DISABLE = 0;
     protected static final byte CACHE_FIXED_HASH_TABLE = 1;
     protected static final byte CACHE_HARD_REF = 2;
     protected static final byte CACHE_WEAK_REF = 3;
     protected static final byte CACHE_SOFT_REF = 4;
     protected static final byte CACHE_LRU = 5;
 
 
 
     protected byte _cache = CACHE_FIXED_HASH_TABLE;
     protected int _cacheSize = 1024*32;
 
     /** file to open, if null opens in memory store */
     protected File _file;
 
     protected boolean _journalEnabled = true;
 
     protected boolean _asyncWriteEnabled = true;
     protected int _asyncFlushDelay = 0;
     protected boolean _asyncThreadDaemon = false;
 
     protected boolean _deleteFilesAfterClose = false;
     protected boolean _readOnly = false;
     protected boolean _closeOnJvmShutdown = false;
 
     protected boolean _compressionEnabled = false;
 
     protected byte[] _xteaEncryptionKey = null;
 
     protected boolean _appendOnlyEnabled = false;
 
     protected boolean _checksumEnabled = false;
 
     protected boolean _ifInMemoryUseDirectBuffer = false;
 
     protected boolean _failOnWrongHeader = false;
 
     protected boolean _RAF = false;
     protected boolean _powerSavingMode = false; //TODO add power saving option
 
     /** use static factory methods, or make subclass */
     protected DBMaker(){}
 
     /** Creates new in-memory database. Changes are lost after JVM exits.
      * <p/>
      * This will use HEAP memory so Garbage Collector is affected.
      */
     @NotNull
     public static DBMaker newMemoryDB(){
         DBMaker m = new DBMaker();
         m._file = null;
         return  m;
     }
 
     /** Creates new in-memory database. Changes are lost after JVM exits.
      * <p/>
      * This will use DirectByteBuffer outside of HEAP, so Garbage Collector is not affected
      *
      */
     @NotNull
     public static DBMaker newDirectMemoryDB(){
         DBMaker m = new DBMaker();
         m._file = null;
         m._ifInMemoryUseDirectBuffer = true;
         return  m;
     }
 
 
 
     /**
      * Create new BTreeMap backed by temporary file storage.
      * This is quick way to create 'throw away' collection.
      *
      * </p>Storage is created in temp folder and deleted on JVM shutdown
      */
     @NotNull
     public static <K,V> BTreeMap<K,V> newTempTreeMap(){
         return newTempFileDB()
                 .deleteFilesAfterClose()
                 .closeOnJvmShutdown()
                 .journalDisable()
                 .make()
                 .getTreeMap("temp");
     }
 
     /**
      * Create new HTreeMap backed by temporary file storage.
      * This is quick way to create 'throw away' collection.
      *
      * </p>Storage is created in temp folder and deleted on JVM shutdown
      */
     @NotNull
     public static <K,V> HTreeMap<K,V> newTempHashMap(){
         return newTempFileDB()
                 .deleteFilesAfterClose()
                 .closeOnJvmShutdown()
                 .journalDisable()
                 .make()
                 .getHashMap("temp");
     }
 
     /**
      * Create new TreeSet backed by temporary file storage.
      * This is quick way to create 'throw away' collection.
      *
      * </p>Storage is created in temp folder and deleted on JVM shutdown
      */
     @NotNull
     public static <K> NavigableSet<K> newTempTreeSet(){
         return newTempFileDB()
                 .deleteFilesAfterClose()
                 .closeOnJvmShutdown()
                 .journalDisable()
                 .make()
                 .getTreeSet("temp");
     }
 
     /**
      * Create new HashSet backed by temporary file storage.
      * This is quick way to create 'throw away' collection.
      *
      * </p>Storage is created in temp folder and deleted on JVM shutdown
      */
     @NotNull
     public static <K> Set<K> newTempHashSet(){
         return newTempFileDB()
                 .deleteFilesAfterClose()
                 .closeOnJvmShutdown()
                 .journalDisable()
                 .make()
                 .getHashSet("temp");
     }
 
     /**
      * Creates new database in temporary folder.
      *
      * @return
      */
     @NotNull
     public static DBMaker newTempFileDB() {
         try {
             return newFileDB(File.createTempFile("JDBM-temp","db"));
         } catch (IOException e) {
             throw new IOError(e);
         }
     }
 
     /** Creates or open database stored in file. */
     @NotNull
     public static DBMaker newFileDB(File file){
         DBMaker m = new DBMaker();
         m._file = file;
         return  m;
     }
 
     /** Creates or open database stored in file.
      * <p/>
      * This methods opens DB with `RAF` mode.
      * It does not use NIO memory mapped buffers, so is slower but safer and more compatible.
      * Use this if you are experiencing <b>java.lang.OutOfMemoryError: Map failed</b> exceptions
      */
     @NotNull
     public static DBMaker newRandomAccessFileDB(File file){
         DBMaker m = new DBMaker();
         m._file = file;
         m._RAF = true;
         return  m;
     }
 
 
     /**
      * Transaction journal is enabled by default
      * You must call <b>DB.commit()</b> to save your changes.
      * It is possible to disable transaction journal for better write performance
      * In this case all integrity checks are sacrificed for faster speed.
      * <p/>
      * If transaction journal is disabled, all changes are written DIRECTLY into store.
      * You must call DB.close() method before exit,
      * otherwise your store <b>WILL BE CORRUPTED</b>
      *
      *
      * @return this builder
      */
     @NotNull
     public DBMaker journalDisable(){
         this._journalEnabled = false;
         return this;
     }
 
     /**
      * Instance cache is enabled by default.
      * This greatly decreases serialization overhead and improves performance.
      * Call this method to disable instance cache, so an object will always be deserialized.
      * <p/>
      * This may workaround some problems
      *
      * @return this builder
      */
     @NotNull
     public DBMaker cacheDisable(){
         this._cache = CACHE_DISABLE;
         return this;
     }
 
     /**
      * Enables unbounded hard reference cache.
      * This cache is good if you have lot of available memory.
      * <p/>
      * All fetched records are added to HashMap and stored with hard reference.
      * To prevent OutOfMemoryExceptions JDBM monitors free memory,
      * if it is bellow 25% cache is cleared.
      *
      * @return this builder
      */
     @NotNull
     public DBMaker cacheHardRefEnable(){
         this._cache = CACHE_HARD_REF;
         return this;
     }
 
 
     /**
      * Enables unbounded cache which uses <code>WeakReference</code>.
      * Items are removed from cache by Garbage Collector
      *
      * @return this builder
      */
     @NotNull
     public DBMaker cacheWeakRefEnable(){
         this._cache = CACHE_WEAK_REF;
         return this;
     }
 
     /**
      * Enables unbounded cache which uses <code>SoftReference</code>.
      * Items are removed from cache by Garbage Collector
      *
      * @return this builder
      */
     @NotNull
     public DBMaker cacheSoftRefEnable(){
         this._cache = CACHE_SOFT_REF;
         return this;
     }
 
     /**
      * Enables Least Recently Used cache. It is fixed size cache and it removes less used items to make space.
      *
      * @return this builder
      */
     @NotNull
     public DBMaker cacheLRUEnable(){
         this._cache = CACHE_LRU;
         return this;
     }
 
 
 
     /**
      * Set cache size. Interpretations depends on cache type.
      * For fixed size caches (such as FixedHashTable cache) it is maximal number of items in cache.
      * <p/>
      * For unbounded caches (such as HardRef cache) it is initial capacity of underlying table (HashMap).
      * <p/>
      * Default cache size is 32768.
      *
      * @param cacheSize new cache size
      * @return this builder
      */
     @NotNull
     public DBMaker cacheSize(int cacheSize){
         this._cacheSize = cacheSize;
         return this;
     }
 
 
     /**
      * By default all modifications are queued and written into disk on Background Writer Thread.
      * So all modifications are performed in asynchronous mode and do not block.
      * <p/>
      * It is possible to disable Background Writer Thread, but this greatly hurts concurrency.
      * Without async writes, all threads blocks until all previous writes are not finished (single big lock).
      *
      * <p/>
      * This may workaround some problems
      *
      * @return this builder
      */
     @NotNull
     public DBMaker asyncWriteDisable(){
         this._asyncWriteEnabled = false;
         return this;
     }
 
     /**
      * In async mode writes are done in Writer Thread.
      * If main thread dies Writer Thread still lives and prevents JVM from exiting.
      * This is good as it prevents data loss.
      * However in some modes shuting down JVM may be more important than preserving data.
      * So you may set Writer Thread to 'daemon mode' so JVM can exit
      *
      * @return this builder
      */
     @NotNull
     public DBMaker asyncThreadDaemonEnable(){
         this._asyncThreadDaemon = true;
         return this;
     }
 
     /**
      * //TODO put this nice comment somewhere
      * By default all objects are serialized in Background Writer Thread.
      * <p/>
      * This may improve performance. For example with single thread access, Async Serialization offloads
      * lot of work to second core. Or when multiple values are added into single tree node,
      * node has to be serialized only once. Without Async Serialization node is serialized each time
      * node is updated.
      * <p/>
      * On other side Async Serialization moves all serialization into single thread. This
      * hurts performance with many concurrent-independent updates.
      * <p/>
      * Async Serialization may also produce some unexpected results when your data classes are not
      * immutable. Consider example bellow. If Async Serialization is disabled, it always prints 'Peter'.
      * If it is enabled (by default) it creates race condition and randomly prints 'Peter' or 'Jack',
      * <pre>
      *     Person person = new Person();
      *     person.setName("Peter");
      *     map.put(id, person)
      *     person.setName("Jack");
      *     //long pause
      *     println(map.get(id).getName());
      * </pre>
      *
      * <p/>
      * This may also workaround some problems
      *
      * @return this builder
      */
 
 
     /**
      * Set flush iterval for write cache, by default is 0
      * <p/>
      * When BTreeMap is constructed from ordered set, tree node size is increasing linearly with each
      * item added. Each time new key is added to tree node, its size changes and
      * storage needs to find new place. So constructing BTreeMap from ordered set leads to large
      * store fragmentation.
      * <p/>
      *  Setting flush interval is workaround as BTreeMap node is always updated in memory (write cache)
      *  and only final version of node is stored on disk.
      *
      *
      * @param delay flush write cache every N miliseconds
      * @return this builder
      */
     @NotNull
     public DBMaker asyncFlushDelay(int delay){
         _asyncFlushDelay = delay;
         return this;
     }
 
 
     /**
      * Try to delete files after DB is closed.
      * File deletion may silently fail, especially on Windows where buffer needs to be unmapped file delete.
      *
      * @return this builder
      */
     @NotNull
     public DBMaker deleteFilesAfterClose(){
         this._deleteFilesAfterClose = true;
         return this;
     }
 
     /**
      * Adds JVM shutdown hook and closes DB just before JVM;
      *
      * @return this builder
      */
     @NotNull
     public DBMaker closeOnJvmShutdown(){
         this._closeOnJvmShutdown = true;
         return this;
     }
 
     /**
      * Enables record compression.
      * <p/>
      * Make sure you enable this every time you reopen store, otherwise record de-serialization fails unpredictably.
      *
      * @return this builder
      */
     @NotNull
     public DBMaker compressionEnable(){
         this._compressionEnabled = true;
         return this;
     }
 
 
     /**
      * Encrypt storage using XTEA algorithm.
      * <p/>
      * XTEA is sound encryption algorithm. However implementation in JDBM was not peer-reviewed.
      * JDBM only encrypts records data, so attacker may see number of records and their sizes.
      * <p/>
      * Make sure you enable this every time you reopen store, otherwise record de-serialization fails unpredictably.
      *
      * @param password for encryption
      * @return this builder
      */
     @NotNull
     public DBMaker encryptionEnable(String password){
         try {
             return encryptionEnable(password.getBytes(Utils.UTF8));
         } catch (UnsupportedEncodingException e) {
             throw new RuntimeException(e);
         }
     }
 
 
 
     /**
      * Encrypt storage using XTEA algorithm.
      * <p/>
      * XTEA is sound encryption algorithm. However implementation in JDBM was not peer-reviewed.
      * JDBM only encrypts records data, so attacker may see number of records and their sizes.
      * <p/>
      * Make sure you enable this every time you reopen store, otherwise record de-serialization fails unpredictably.
      *
      * @param password for encryption
      * @return this builder
      */
     @NotNull
     public DBMaker encryptionEnable(byte[] password){
         _xteaEncryptionKey = password;
         return this;
     }
 
 
     /**
      * Adds CRC32 checksum at end of each record to check data integrity.
      * It throws 'IOException("CRC32 does not match, data broken")' on de-serialization if data are corrupted
      * <p/>
      * Make sure you enable this every time you reopen store, otherwise record de-serialization fails.
      *
      * @return this builder
      */
     @NotNull
     public DBMaker checksumEnable(){
         this._checksumEnabled = true;
         return this;
     }
 
 
     /**
      * Open store in read-only mode. Any modification attempt will throw
      * <code>UnsupportedOperationException("Read-only")</code>
      *
      * @return this builder
      */
     @NotNull
     public DBMaker readOnly(){
         this._readOnly = true;
         return this;
     }
 
 
 
     /**
      * In 'appendOnly' mode existing free space is not reused,
      * but records are added to the end of the store.
      * <p/>
      * This slightly improves write performance as store does not have
      * to traverse list of free records to find and reuse existing position.
      * <p/>
      * It also decreases chance for store corruption, as existing data
      * are not overwritten with new record.
      * <p/>
      * When this mode is used for longer time, store becomes fragmented.
      * It is necessary to run defragmentation then.
      *
      * @return this builder
      */
     @NotNull
     public DBMaker appendOnlyEnable(){
         this._appendOnlyEnabled = true;
         return this;
     }
 
 
     /** constructs DB using current settings */
     @NotNull
     public DB make(){
         return new DB(makeEngine());
     }
 
     /** constructs Engine using current settings */
     @NotNull
     public Engine makeEngine(){
 
 
         if(_readOnly && _file==null)
             throw new UnsupportedOperationException("Can not open in-memory DB in read-only mode.");
 
         if(_readOnly && !_file.exists()){
             throw new UnsupportedOperationException("Can not open non-existing file in read-only mode.");
         }
 
         Volume.Factory folFac = _file == null?
                 Volume.memoryFactory(_ifInMemoryUseDirectBuffer):
                 Volume.fileFactory(_readOnly, _RAF, _file);
 
         Engine engine = _journalEnabled ?
                 new StorageJournaled(folFac,  _appendOnlyEnabled, _deleteFilesAfterClose, _failOnWrongHeader, _readOnly):
                 new StorageDirect(folFac, _appendOnlyEnabled, _deleteFilesAfterClose , _failOnWrongHeader, _readOnly);
 
         AsyncWriteEngine engineAsync = null;
         if(_asyncWriteEnabled && !_readOnly){
             engineAsync = new AsyncWriteEngine(engine,  _asyncThreadDaemon, _powerSavingMode);
             engine = engineAsync;
         }
 
         if(_checksumEnabled){
             engine = new ByteTransformEngine(engine, Serializer.CRC32_CHECKSUM);
         }
 
         if(_xteaEncryptionKey!=null){
             engine = new ByteTransformEngine(engine, new EncryptionXTEA(_xteaEncryptionKey));
         }
 
 
         if(_compressionEnabled){
             engine = new ByteTransformEngine(engine, CompressLZF.SERIALIZER);
         }
 
         engine = new SnapshotEngine(engine, _cache==CACHE_DISABLE? 0 : 1024);
 
         if(_cache == CACHE_DISABLE){
             //do not wrap engine in cache
         }if(_cache == CACHE_FIXED_HASH_TABLE){
             engine = new CacheHashTable(engine,_cacheSize);
         }else if (_cache == CACHE_HARD_REF){
             engine = new CacheHardRef(engine,_cacheSize);
         }else if (_cache == CACHE_WEAK_REF){
             engine = new CacheWeakSoftRef(engine,true);
         }else if (_cache == CACHE_SOFT_REF){
             engine = new CacheWeakSoftRef(engine,false);
         }else if (_cache == CACHE_LRU){
             engine = new CacheLRU(engine, _cacheSize);
         }
 
 
         if(_readOnly)
             engine = new ReadOnlyEngine(engine);
 
         if(engineAsync!=null)
             engineAsync.setParentEngineReference(engine);
 
         if(_closeOnJvmShutdown){
             final Engine engine2 = engine;
             Runtime.getRuntime().addShutdownHook(new Thread("JDBM shutdown") {
                 @Override
 				public void run() {
                    //TODO handle already closed engine
                    engine2.close();
                 }
             });
         }
 
         return engine;
     }
 
 
 }
