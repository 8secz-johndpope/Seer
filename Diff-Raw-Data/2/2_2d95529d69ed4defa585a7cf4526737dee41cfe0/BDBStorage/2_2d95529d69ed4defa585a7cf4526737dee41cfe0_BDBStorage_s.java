 /*
  * Copyright 2006 Amazon Technologies, Inc. or its affiliates.
  * Amazon, Amazon.com and Carbonado are trademarks or registered trademarks
  * of Amazon Technologies, Inc. or its affiliates.  All rights reserved.
  *
  * Licensed under the Apache License, Version 2.0 (the "License");
  * you may not use this file except in compliance with the License.
  * You may obtain a copy of the License at
  *
  *     http://www.apache.org/licenses/LICENSE-2.0
  *
  * Unless required by applicable law or agreed to in writing, software
  * distributed under the License is distributed on an "AS IS" BASIS,
  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  * See the License for the specific language governing permissions and
  * limitations under the License.
  */
 
 package com.amazon.carbonado.repo.sleepycat;
 
 import java.util.Collection;
 import java.util.Collections;
 import java.util.Map;
 
 import org.apache.commons.logging.Log;
 import org.apache.commons.logging.LogFactory;
 
 import com.amazon.carbonado.Cursor;
 import com.amazon.carbonado.FetchException;
 import com.amazon.carbonado.MalformedFilterException;
 import com.amazon.carbonado.PersistException;
 import com.amazon.carbonado.Query;
 import com.amazon.carbonado.Repository;
 import com.amazon.carbonado.RepositoryException;
 import com.amazon.carbonado.Storable;
 import com.amazon.carbonado.Storage;
 import com.amazon.carbonado.SupportException;
 import com.amazon.carbonado.Trigger;
 
 import com.amazon.carbonado.capability.IndexInfo;
 
 import com.amazon.carbonado.cursor.ArraySortBuffer;
 import com.amazon.carbonado.cursor.EmptyCursor;
 import com.amazon.carbonado.cursor.MergeSortBuffer;
 import com.amazon.carbonado.cursor.SingletonCursor;
 import com.amazon.carbonado.cursor.SortBuffer;
 
 import com.amazon.carbonado.filter.Filter;
 
 import com.amazon.carbonado.info.Direction;
 import com.amazon.carbonado.info.StorableIndex;
 import com.amazon.carbonado.info.StorableInfo;
 import com.amazon.carbonado.info.StorableIntrospector;
 import com.amazon.carbonado.info.StorableProperty;
 
 import com.amazon.carbonado.layout.Layout;
 import com.amazon.carbonado.layout.LayoutFactory;
 import com.amazon.carbonado.layout.Unevolvable;
 
 import com.amazon.carbonado.lob.Blob;
 import com.amazon.carbonado.lob.Clob;
 
 import com.amazon.carbonado.qe.BoundaryType;
 import com.amazon.carbonado.qe.QueryEngine;
 import com.amazon.carbonado.qe.QueryExecutorFactory;
 import com.amazon.carbonado.qe.StorageAccess;
 
 import com.amazon.carbonado.raw.StorableCodec;
 import com.amazon.carbonado.raw.StorableCodecFactory;
 import com.amazon.carbonado.raw.RawSupport;
 import com.amazon.carbonado.raw.RawUtil;
 
 import com.amazon.carbonado.spi.IndexInfoImpl;
 import com.amazon.carbonado.spi.LobEngine;
 import com.amazon.carbonado.spi.SequenceValueProducer;
 import com.amazon.carbonado.spi.StorableIndexSet;
 import com.amazon.carbonado.spi.TriggerManager;
 
 /**
  *
  * @author Brian S O'Neill
  */
 abstract class BDBStorage<Txn, S extends Storable> implements Storage<S>, StorageAccess<S> {
     /** Constant indicating success */
     protected static final byte[] SUCCESS = new byte[0];
 
     /** Constant indicating an entry was not found */
     protected static final byte[] NOT_FOUND = new byte[0];
 
     /** Constant indicating an entry already exists */
     protected static final Object KEY_EXIST = new Object();
 
     private static final int DEFAULT_LOB_BLOCK_SIZE = 1000;
 
     final BDBRepository<Txn> mRepository;
     /** Reference to the type of storable */
     private final Class<S> mType;
 
     /** Does most of the work in generating storables, used for preparing and querying  */
     private StorableCodec<S> mStorableCodec;
 
     /**
      * Reference to an instance of Proxy, defined in this class, which binds
      * the storables to our own implementation. Handed off to mStorableFactory.
      */
     private final RawSupport<S> mRawSupport;
 
     /** Primary key index is required, and is the only one supported. */
     private StorableIndex<S> mPrimaryKeyIndex;
 
     /** Reference to primary database. */
     private Object mPrimaryDatabase;
 
     /** Reference to query engine, defined later in this class */
     private QueryEngine<S> mQueryEngine;
 
     private Storage<S> mRootStorage;
 
     final TriggerManager<S> mTriggerManager;
 
     /**
      * Constructs a storage instance, but subclass must call open before it can
      * be used.
      *
      * @param repository repository this storage came from
      * @throws SupportException if storable type is not supported
      */
     protected BDBStorage(BDBRepository<Txn> repository, Class<S> type)
         throws SupportException
     {
         mRepository = repository;
         mType = type;
         mRawSupport = new Support<Txn, S>(repository, this);
         mTriggerManager = new TriggerManager<S>(type, repository.mTriggerFactories);
         try {
             // Ask if any lobs via static method first, to prevent stack
             // overflow that occurs when creating BDBStorage instances for
             // metatypes. These metatypes cannot support Lobs.
             if (LobEngine.hasLobs(type)) {
                 Trigger<S> lobTrigger = repository.getLobEngine()
                     .getSupportTrigger(type, DEFAULT_LOB_BLOCK_SIZE);
                 addTrigger(lobTrigger);
             }
         } catch (SupportException e) {
             throw e;
         } catch (RepositoryException e) {
             throw new SupportException(e);
         }
     }
 
     public Class<S> getStorableType() {
         return mType;
     }
 
     public S prepare() {
         return mStorableCodec.instantiate(mRawSupport);
     }
 
     public Query<S> query() throws FetchException {
         return mQueryEngine.query();
     }
 
     public Query<S> query(String filter) throws FetchException {
         return mQueryEngine.query(filter);
     }
 
     public Query<S> query(Filter<S> filter) throws FetchException {
         return mQueryEngine.query(filter);
     }
 
     public boolean addTrigger(Trigger<? super S> trigger) {
         return mTriggerManager.addTrigger(trigger);
     }
 
     public boolean removeTrigger(Trigger<? super S> trigger) {
         return mTriggerManager.removeTrigger(trigger);
     }
 
     public IndexInfo[] getIndexInfo() {
         StorableIndex<S> pkIndex = mPrimaryKeyIndex;
 
         if (pkIndex == null) {
             return new IndexInfo[0];
         }
 
         int i = pkIndex.getPropertyCount();
         String[] propertyNames = new String[i];
         Direction[] directions = new Direction[i];
         while (--i >= 0) {
             propertyNames[i] = pkIndex.getProperty(i).getName();
             directions[i] = pkIndex.getPropertyDirection(i);
         }
 
         return new IndexInfo[] {
             new IndexInfoImpl(getStorableType().getName(), true, true, propertyNames, directions)
         };
     }
 
     public QueryExecutorFactory<S> getQueryExecutorFactory() {
         return mQueryEngine;
     }
 
     public Collection<StorableIndex<S>> getAllIndexes() {
         return Collections.singletonList(mPrimaryKeyIndex);
     }
 
     public Storage<S> storageDelegate(StorableIndex<S> index) {
         // We're the grunt and don't delegate.
         return null;
     }
 
     public SortBuffer<S> createSortBuffer() {
         // FIXME: This is messy. If Storables had built-in serialization
         // support, then MergeSortBuffer would not need a root storage.
         if (mRootStorage == null) {
             try {
                 mRootStorage = mRepository.getRootRepository().storageFor(getStorableType());
             } catch (RepositoryException e) {
                 LogFactory.getLog(BDBStorage.class).warn(null, e);
                 return new ArraySortBuffer<S>();
             }
         }
 
         // FIXME: sort buffer should be on repository access. Also, create abstract
         // repository access that creates the correct merge sort buffer. And more:
         // create capability for managing merge sort buffers.
         return new MergeSortBuffer<S>(mRootStorage);
     }
 
     public long countAll() throws FetchException {
        // Return -1 to indicate default algorithmn should be used.
         return -1;
     }
 
     public Cursor<S> fetchAll() throws FetchException {
         return fetchSubset(null, null,
                            BoundaryType.OPEN, null,
                            BoundaryType.OPEN, null,
                            false, false);
     }
 
     public Cursor<S> fetchOne(StorableIndex<S> index,
                               Object[] identityValues)
         throws FetchException
     {
         byte[] key = mStorableCodec.encodePrimaryKey(identityValues);
         byte[] value = mRawSupport.tryLoad(key);
         if (value == null) {
             return EmptyCursor.the();
         }
         return new SingletonCursor<S>(instantiate(key, value));
     }
 
     public Cursor<S> fetchSubset(StorableIndex<S> index,
                                  Object[] identityValues,
                                  BoundaryType rangeStartBoundary,
                                  Object rangeStartValue,
                                  BoundaryType rangeEndBoundary,
                                  Object rangeEndValue,
                                  boolean reverseRange,
                                  boolean reverseOrder)
         throws FetchException
     {
         BDBTransactionManager<Txn> txnMgr = openTransactionManager();
 
         if (reverseRange) {
             {
                 BoundaryType temp = rangeStartBoundary;
                 rangeStartBoundary = rangeEndBoundary;
                 rangeEndBoundary = temp;
             }
 
             {
                 Object temp = rangeStartValue;
                 rangeStartValue = rangeEndValue;
                 rangeEndValue = temp;
             }
         }
 
         // Lock out shutdown task.
         txnMgr.getLock().lock();
         try {
             StorableCodec<S> codec = mStorableCodec;
 
             final byte[] identityKey;
             if (identityValues == null || identityValues.length == 0) {
                 identityKey = codec.encodePrimaryKeyPrefix();
             } else {
                 identityKey = codec.encodePrimaryKey(identityValues, 0, identityValues.length);
             }
 
             final byte[] startBound;
             if (rangeStartBoundary == BoundaryType.OPEN) {
                 startBound = identityKey;
             } else {
                 startBound = createBound(identityValues, identityKey, rangeStartValue, codec);
                 if (!reverseOrder && rangeStartBoundary == BoundaryType.EXCLUSIVE) {
                     // If key is composite and partial, need to skip trailing
                     // unspecified keys by adding one and making inclusive.
                     if (!RawUtil.increment(startBound)) {
                         return EmptyCursor.the();
                     }
                     rangeStartBoundary = BoundaryType.INCLUSIVE;
                 }
             }
 
             final byte[] endBound;
             if (rangeEndBoundary == BoundaryType.OPEN) {
                 endBound = identityKey;
             } else {
                 endBound = createBound(identityValues, identityKey, rangeEndValue, codec);
                 if (reverseOrder && rangeEndBoundary == BoundaryType.EXCLUSIVE) {
                     // If key is composite and partial, need to skip trailing
                     // unspecified keys by subtracting one and making
                     // inclusive.
                     if (!RawUtil.decrement(endBound)) {
                         return EmptyCursor.the();
                     }
                     rangeEndBoundary = BoundaryType.INCLUSIVE;
                 }
             }
 
             final boolean inclusiveStart = rangeStartBoundary != BoundaryType.EXCLUSIVE;
             final boolean inclusiveEnd = rangeEndBoundary != BoundaryType.EXCLUSIVE;
 
             try {
                 BDBCursor<Txn, S> cursor = openCursor
                     (txnMgr,
                      startBound, inclusiveStart,
                      endBound, inclusiveEnd,
                      mStorableCodec.getPrimaryKeyPrefixLength(),
                      reverseOrder,
                      getPrimaryDatabase());
 
                 cursor.open();
                 return cursor;
             } catch (Exception e) {
                 throw toFetchException(e);
             }
         } finally {
             txnMgr.getLock().unlock();
         }
     }
 
     private byte[] createBound(Object[] exactValues, byte[] exactKey, Object rangeValue,
                                StorableCodec<S> codec) {
         Object[] values = {rangeValue};
         if (exactValues == null || exactValues.length == 0) {
             return codec.encodePrimaryKey(values, 0, 1);
         }
 
         byte[] rangeKey = codec.encodePrimaryKey
             (values, exactValues.length, exactValues.length + 1);
         byte[] bound = new byte[exactKey.length + rangeKey.length];
         System.arraycopy(exactKey, 0, bound, 0, exactKey.length);
         System.arraycopy(rangeKey, 0, bound, exactKey.length, rangeKey.length);
         return bound;
     }
 
     protected BDBRepository getRepository() {
         return mRepository;
     }
 
     /**
      * @param readOnly when true, this method will not attempt to reconcile
      * differences between the current index set and the desired index set.
      */
     protected void open(boolean readOnly) throws RepositoryException {
         final Layout layout = getLayout();
 
         StorableInfo<S> info = StorableIntrospector.examine(getStorableType());
 
         StorableCodecFactory codecFactory = mRepository.getStorableCodecFactory();
 
         // Open primary database.
         Object primaryDatabase;
 
         String databaseName = codecFactory.getStorageName(getStorableType());
         if (databaseName == null) {
             databaseName = getStorableType().getName();
         }
 
         // Primary info may be null for StoredDatabaseInfo itself.
         StoredDatabaseInfo primaryInfo;
         boolean isPrimaryEmpty;
 
         try {
             BDBTransactionManager<Txn> txnMgr = mRepository.openTransactionManager();
             // Lock out shutdown task.
             txnMgr.getLock().lock();
             try {
                 primaryDatabase = env_openPrimaryDatabase(null, databaseName);
                 primaryInfo = registerPrimaryDatabase(readOnly, layout);
                 isPrimaryEmpty = db_isEmpty(null, primaryDatabase, txnMgr.isForUpdate());
             } finally {
                 txnMgr.getLock().unlock();
             }
         } catch (Exception e) {
             throw toRepositoryException(e);
         }
 
         StorableIndex<S> pkIndex;
 
         if (!isPrimaryEmpty && primaryInfo != null
             && primaryInfo.getIndexNameDescriptor() != null) {
 
             // Entries already exist, so primary key format is locked in.
             pkIndex = StorableIndex.parseNameDescriptor
                 (primaryInfo.getIndexNameDescriptor(), info);
             // TODO: Verify index types match and throw error if not.
         } else {
             // In order to select the best index for the primary key, allow all
             // indexes to be considered.
             StorableIndexSet<S> indexSet = new StorableIndexSet<S>();
             indexSet.addIndexes(info);
             indexSet.addAlternateKeys(info);
             indexSet.addPrimaryKey(info);
 
             indexSet.reduce(Direction.ASCENDING);
 
             pkIndex = indexSet.findPrimaryKeyIndex(info);
             if (primaryInfo != null) {
                 if (!pkIndex.getNameDescriptor().equals(primaryInfo.getIndexNameDescriptor()) ||
                     !pkIndex.getTypeDescriptor().equals(primaryInfo.getIndexTypeDescriptor())) {
 
                     primaryInfo.setIndexNameDescriptor(pkIndex.getNameDescriptor());
                     primaryInfo.setIndexTypeDescriptor(pkIndex.getTypeDescriptor());
 
                     if (!readOnly) {
                         primaryInfo.update();
                     }
                 }
             }
         }
 
         // Indicate that primary key is clustered, which can affect query analysis.
         pkIndex = pkIndex.clustered(true);
 
         try {
             mStorableCodec = codecFactory
                 .createCodec(getStorableType(), pkIndex, mRepository.isMaster(), layout);
         } catch (SupportException e) {
             // We've opened the database prematurely, since type isn't
             // supported by encoding strategy. Close it down and unregister.
             try {
                 db_close(primaryDatabase);
             } catch (Exception e2) {
                 // Don't care.
             }
             try {
                 unregisterDatabase(readOnly, getStorableType().getName());
             } catch (Exception e2) {
                 // Don't care.
             }
             throw e;
         }
 
         mPrimaryKeyIndex = mStorableCodec.getPrimaryKeyIndex();
         mPrimaryDatabase = primaryDatabase;
 
         mQueryEngine = new QueryEngine<S>(getStorableType(), mRepository);
     }
 
     protected S instantiate(byte[] key, byte[] value) throws FetchException {
         return mStorableCodec.instantiate(mRawSupport, key, value);
     }
 
     protected CompactionCapability.Result<S> compact() throws RepositoryException {
         byte[] start = mStorableCodec.encodePrimaryKeyPrefix();
         if (start != null && start.length == 0) {
             start = null;
         }
 
         byte[] end;
         if (start == null) {
             end = null;
         } else {
             end = start.clone();
             if (!RawUtil.increment(end)) {
                 end = null;
             }
         }
 
         try {
             Txn txn = mRepository.openTransactionManager().getTxn();
             return db_compact(txn, mPrimaryDatabase, start, end);
         } catch (Exception e) {
             throw mRepository.toRepositoryException(e);
         }
     }
 
     /**
      * @return true if record with given key exists
      */
     protected abstract boolean db_exists(Txn txn, byte[] key, boolean rmw) throws Exception;
 
     /**
      * @return NOT_FOUND, any byte[], or null (if empty result)
      */
     protected abstract byte[] db_get(Txn txn, byte[] key, boolean rmw) throws Exception;
 
     /**
      * @return SUCCESS, KEY_EXIST, or NOT_FOUND otherwise
      */
     protected abstract Object db_putNoOverwrite(Txn txn, byte[] key, byte[] value)
         throws Exception;
 
     /**
      * @return true if successful
      */
     protected abstract boolean db_put(Txn txn, byte[] key, byte[] value)
         throws Exception;
 
     /**
      * @return true if successful
      */
     protected abstract boolean db_delete(Txn txn, byte[] key)
         throws Exception;
 
     protected abstract void db_truncate(Txn txn) throws Exception;
 
     /**
      * @return true if database has no entries.
      */
     protected abstract boolean db_isEmpty(Txn txn, Object database, boolean rmw) throws Exception;
 
     protected CompactionCapability.Result<S> db_compact
         (Txn txn, Object database, byte[] start, byte[] end)
         throws Exception
     {
         throw new UnsupportedOperationException();
     }
 
     protected abstract void db_close(Object database) throws Exception;
 
     /**
      * Implementation should call runDatabasePrepareForOpeningHook on database
      * before opening.
      */
     protected abstract Object env_openPrimaryDatabase(Txn txn, String name) throws Exception;
 
     protected void runDatabasePrepareForOpeningHook(Object database) throws RepositoryException {
         mRepository.runDatabasePrepareForOpeningHook(database);
     }
 
     protected abstract void env_removeDatabase(Txn txn, String databaseName) throws Exception;
 
     /**
      * @param txn optional transaction to commit when cursor is closed
      * @param txnMgr
      * @param startBound specify the starting key for the cursor, or null if first
      * @param inclusiveStart true if start bound is inclusive
      * @param endBound specify the ending key for the cursor, or null if last
      * @param inclusiveEnd true if end bound is inclusive
      * @param maxPrefix maximum expected common initial bytes in start and end bound
      * @param reverse when true, iteration is reversed
      * @param database database to use
      */
     protected abstract BDBCursor<Txn, S> openCursor
         (BDBTransactionManager<Txn> txnMgr,
          byte[] startBound, boolean inclusiveStart,
          byte[] endBound, boolean inclusiveEnd,
          int maxPrefix,
          boolean reverse,
          Object database)
         throws Exception;
 
     FetchException toFetchException(Throwable e) {
         return mRepository.toFetchException(e);
     }
 
     PersistException toPersistException(Throwable e) {
         return mRepository.toPersistException(e);
     }
 
     RepositoryException toRepositoryException(Throwable e) {
         return mRepository.toRepositoryException(e);
     }
 
     BDBTransactionManager<Txn> openTransactionManager() {
         return mRepository.openTransactionManager();
     }
 
     /**
      * Caller must hold transaction lock. May throw FetchException if storage
      * is closed.
      */
     Object getPrimaryDatabase() throws FetchException {
         Object database = mPrimaryDatabase;
         if (database == null) {
             checkClosed();
             throw new IllegalStateException("BDBStorage not opened");
         }
         return database;
     }
 
     Blob getBlob(long locator) throws FetchException {
         try {
             return mRepository.getLobEngine().getBlobValue(locator);
         } catch (RepositoryException e) {
             throw e.toFetchException();
         }
     }
 
     long getLocator(Blob blob) throws PersistException {
         try {
             return mRepository.getLobEngine().getLocator(blob);
         } catch (ClassCastException e) {
             throw new PersistException(e);
         } catch (RepositoryException e) {
             throw e.toPersistException();
         }
     }
 
     Clob getClob(long locator) throws FetchException {
         try {
             return mRepository.getLobEngine().getClobValue(locator);
         } catch (RepositoryException e) {
             throw e.toFetchException();
         }
     }
 
     long getLocator(Clob clob) throws PersistException {
         try {
             return mRepository.getLobEngine().getLocator(clob);
         } catch (ClassCastException e) {
             throw new PersistException(e);
         } catch (RepositoryException e) {
             throw e.toPersistException();
         }
     }
 
     /**
      * If open, returns normally. If shutting down, blocks forever. Otherwise,
      * if closed, throws FetchException. Method blocks forever on shutdown to
      * prevent threads from starting work that will likely fail along the way.
      */
     void checkClosed() throws FetchException {
         BDBTransactionManager<Txn> txnMgr = openTransactionManager();
 
         // Lock out shutdown task.
         txnMgr.getLock().lock();
         try {
             if (mPrimaryDatabase == null) {
                 // If shuting down, this will force us to block forever.
                 try {
                     txnMgr.getTxn();
                 } catch (Exception e) {
                     // Don't care.
                 }
                 // Okay, not shutting down, so throw exception.
                 throw new FetchException("Repository closed");
             }
         } finally {
             txnMgr.getLock().unlock();
         }
     }
 
     void close() throws Exception {
         BDBTransactionManager<Txn> txnMgr = mRepository.openTransactionManager();
         txnMgr.getLock().lock();
         try {
             if (mPrimaryDatabase != null) {
                 db_close(mPrimaryDatabase);
                 mPrimaryDatabase = null;
             }
         } finally {
             txnMgr.getLock().unlock();
         }
     }
 
     private Layout getLayout() throws RepositoryException {
         if (Unevolvable.class.isAssignableFrom(getStorableType())) {
             // Don't record generation for storables marked as unevolvable.
             return null;
         }
 
         LayoutFactory factory;
         try {
             factory = mRepository.getLayoutFactory();
         } catch (SupportException e) {
             // Metadata repository does not support layout storables, so it
             // cannot support generations.
             return null;
         }
 
         return factory.layoutFor(getStorableType());
     }
 
     /**
      * Note: returned StoredDatabaseInfo does not have name and type
      * descriptors saved yet.
      *
      * @return null if type cannot be registered
      */
     private StoredDatabaseInfo registerPrimaryDatabase(boolean readOnly, Layout layout)
         throws Exception
     {
         if (getStorableType() == StoredDatabaseInfo.class) {
             // Can't register itself in itself.
             return null;
         }
         StoredDatabaseInfo info;
         try {
             info = prepareStoredDatabaseInfo();
         } catch (SupportException e) {
             return null;
         }
         info.setDatabaseName(getStorableType().getName());
         if (!info.tryLoad()) {
             if (layout == null) {
                 info.setEvolutionStrategy(StoredDatabaseInfo.EVOLUTION_NONE);
             } else {
                 info.setEvolutionStrategy(StoredDatabaseInfo.EVOLUTION_STANDARD);
             }
             info.setCreationTimestamp(System.currentTimeMillis());
             info.setVersionNumber(0);
             if (!readOnly) {
                 info.insert();
             }
         }
         return info;
     }
 
     private void unregisterDatabase(boolean readOnly, String name) throws RepositoryException {
         if (getStorableType() == StoredDatabaseInfo.class) {
             // Can't unregister when register wasn't allowed.
             return;
         }
         if (!readOnly) {
             StoredDatabaseInfo info;
             try {
                 info = prepareStoredDatabaseInfo();
             } catch (SupportException e) {
                 return;
             }
             info.setDatabaseName(name);
             info.delete();
         }
     }
 
     /**
      * @throws SupportException if StoredDatabaseInfo is not supported by codec factory
      */
     private StoredDatabaseInfo prepareStoredDatabaseInfo() throws RepositoryException {
         return mRepository.getRootRepository().storageFor(StoredDatabaseInfo.class).prepare();
     }
 
     // Note: BDBStorage could just implement the RawSupport interface, but
     // then these hidden methods would be public. A simple cast of Storage to
     // RawSupport would expose them.
     private static class Support<Txn, S extends Storable> implements RawSupport<S> {
         private final BDBRepository<Txn> mRepository;
         private final BDBStorage<Txn, S> mStorage;
         private Map<String, ? extends StorableProperty<S>> mProperties;
 
         Support(BDBRepository<Txn> repo, BDBStorage<Txn, S> storage) {
             mRepository = repo;
             mStorage = storage;
         }
 
         public Repository getRootRepository() {
             return mRepository.getRootRepository();
         }
 
         public boolean isPropertySupported(String name) {
             if (name == null) {
                 return false;
             }
             if (mProperties == null) {
                 mProperties = StorableIntrospector
                     .examine(mStorage.getStorableType()).getAllProperties();
             }
             return mProperties.containsKey(name);
         }
 
         public byte[] tryLoad(byte[] key) throws FetchException {
             BDBTransactionManager<Txn> txnMgr = mStorage.openTransactionManager();
             byte[] result;
             // Lock out shutdown task.
             txnMgr.getLock().lock();
             try {
                 try {
                     result = mStorage.db_get(txnMgr.getTxn(), key, txnMgr.isForUpdate());
                 } catch (Throwable e) {
                     throw mStorage.toFetchException(e);
                 }
             } finally {
                 txnMgr.getLock().unlock();
             }
             if (result == NOT_FOUND) {
                 return null;
             }
             if (result == null) {
                 result = SUCCESS;
             }
             return result;
         }
 
         public boolean tryInsert(S storable, byte[] key, byte[] value) throws PersistException {
             BDBTransactionManager<Txn> txnMgr = mStorage.openTransactionManager();
             Object result;
             // Lock out shutdown task.
             txnMgr.getLock().lock();
             try {
                 try {
                     result = mStorage.db_putNoOverwrite(txnMgr.getTxn(), key, value);
                 } catch (Throwable e) {
                     throw mStorage.toPersistException(e);
                 }
             } finally {
                 txnMgr.getLock().unlock();
             }
             if (result == KEY_EXIST) {
                 return false;
             }
             if (result != SUCCESS) {
                 throw new PersistException("Failed");
             }
             return true;
         }
 
         public void store(S storable, byte[] key, byte[] value) throws PersistException {
             BDBTransactionManager<Txn> txnMgr = mStorage.openTransactionManager();
             // Lock out shutdown task.
             txnMgr.getLock().lock();
             try {
                 try {
                     if (!mStorage.db_put(txnMgr.getTxn(), key, value)) {
                         throw new PersistException("Failed");
                     }
                 } catch (Throwable e) {
                     throw mStorage.toPersistException(e);
                 }
             } finally {
                 txnMgr.getLock().unlock();
             }
         }
 
         public boolean tryDelete(byte[] key) throws PersistException {
             BDBTransactionManager<Txn> txnMgr = mStorage.openTransactionManager();
             // Lock out shutdown task.
             txnMgr.getLock().lock();
             try {
                 try {
                     return mStorage.db_delete(txnMgr.getTxn(), key);
                 } catch (Throwable e) {
                     throw mStorage.toPersistException(e);
                 }
             } finally {
                 txnMgr.getLock().unlock();
             }
         }
 
         public Blob getBlob(long locator) throws FetchException {
             return mStorage.getBlob(locator);
         }
 
         public long getLocator(Blob blob) throws PersistException {
             return mStorage.getLocator(blob);
         }
 
         public Clob getClob(long locator) throws FetchException {
             return mStorage.getClob(locator);
         }
 
         public long getLocator(Clob clob) throws PersistException {
             return mStorage.getLocator(clob);
         }
 
         public SequenceValueProducer getSequenceValueProducer(String name)
             throws PersistException
         {
             return mStorage.mRepository.getSequenceValueProducer(name);
         }
 
         public Trigger<? super S> getInsertTrigger() {
             return mStorage.mTriggerManager.getInsertTrigger();
         }
 
         public Trigger<? super S> getUpdateTrigger() {
             return mStorage.mTriggerManager.getUpdateTrigger();
         }
 
         public Trigger<? super S> getDeleteTrigger() {
             return mStorage.mTriggerManager.getDeleteTrigger();
         }
     }
 }
