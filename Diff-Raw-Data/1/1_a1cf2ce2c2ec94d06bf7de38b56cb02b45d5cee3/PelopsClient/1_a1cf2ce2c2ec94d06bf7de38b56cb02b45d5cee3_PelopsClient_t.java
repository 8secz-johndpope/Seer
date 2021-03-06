 /*******************************************************************************
  * * Copyright 2012 Impetus Infotech.
  *  *
  *  * Licensed under the Apache License, Version 2.0 (the "License");
  *  * you may not use this file except in compliance with the License.
  *  * You may obtain a copy of the License at
  *  *
  *  *      http://www.apache.org/licenses/LICENSE-2.0
  *  *
  *  * Unless required by applicable law or agreed to in writing, software
  *  * distributed under the License is distributed on an "AS IS" BASIS,
  *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  *  * See the License for the specific language governing permissions and
  *  * limitations under the License.
  ******************************************************************************/
 
 package com.impetus.client.cassandra.pelops;
 
 import java.util.ArrayList;
 import java.util.Arrays;
 import java.util.Collection;
 import java.util.Iterator;
 import java.util.List;
 import java.util.Map;
 
 import javax.persistence.PersistenceException;
 
 import org.apache.cassandra.thrift.Cassandra;
 import org.apache.cassandra.thrift.CfDef;
 import org.apache.cassandra.thrift.Column;
 import org.apache.cassandra.thrift.ColumnDef;
 import org.apache.cassandra.thrift.ColumnOrSuperColumn;
 import org.apache.cassandra.thrift.ColumnParent;
 import org.apache.cassandra.thrift.ConsistencyLevel;
 import org.apache.cassandra.thrift.IndexClause;
 import org.apache.cassandra.thrift.IndexOperator;
 import org.apache.cassandra.thrift.IndexType;
 import org.apache.cassandra.thrift.InvalidRequestException;
 import org.apache.cassandra.thrift.KeySlice;
 import org.apache.cassandra.thrift.KsDef;
 import org.apache.cassandra.thrift.NotFoundException;
 import org.apache.cassandra.thrift.SchemaDisagreementException;
 import org.apache.cassandra.thrift.SlicePredicate;
 import org.apache.cassandra.thrift.SuperColumn;
 import org.apache.commons.logging.Log;
 import org.apache.commons.logging.LogFactory;
 import org.apache.thrift.TException;
 import org.scale7.cassandra.pelops.Bytes;
 import org.scale7.cassandra.pelops.Mutator;
 import org.scale7.cassandra.pelops.Pelops;
 import org.scale7.cassandra.pelops.RowDeletor;
 import org.scale7.cassandra.pelops.Selector;
 
 import com.impetus.kundera.KunderaException;
 import com.impetus.kundera.client.Client;
 import com.impetus.kundera.client.EnhanceEntity;
 import com.impetus.kundera.db.DataRow;
 import com.impetus.kundera.index.IndexManager;
 import com.impetus.kundera.metadata.KunderaMetadataManager;
 import com.impetus.kundera.metadata.model.EntityMetadata;
 import com.impetus.kundera.persistence.EntityReader;
 import com.impetus.kundera.persistence.handler.impl.EntitySaveGraph;
 import com.impetus.kundera.property.PropertyAccessException;
 import com.impetus.kundera.property.PropertyAccessor;
 import com.impetus.kundera.property.PropertyAccessorFactory;
 import com.impetus.kundera.property.PropertyAccessorHelper;
 import com.impetus.kundera.proxy.EnhancedEntity;
 
 /**
  * Client implementation using Pelops. http://code.google.com/p/pelops/
  * 
  * @author animesh.kumar
  * @since 0.1
  */
 public class PelopsClient implements Client
 {
 
     /** log for this class. */
     private static Log log = LogFactory.getLog(PelopsClient.class);
 
     /** The closed. */
     private boolean closed = false;
 
     /** The data handler. */
     private PelopsDataHandler dataHandler;
 
     /** The index manager. */
     private IndexManager indexManager;
 
     /** The reader. */
     private EntityReader reader;
 
     /** The persistence unit. */
     private String persistenceUnit;
 
     /** The timestamp. */
     private long timestamp;
 
     /**
      * default constructor.
      * 
      * @param indexManager
      *            the index manager
      * @param reader
      *            the reader
      */
     public PelopsClient(IndexManager indexManager, EntityReader reader, String persistenceUnit)
     {
    	this.persistenceUnit = persistenceUnit;
         this.indexManager = indexManager;
         this.dataHandler = new PelopsDataHandler(this);
         this.reader = reader;
     }
 
     /*
      * (non-Javadoc)
      * 
      * @see com.impetus.kundera.client.Client#find(java.lang.Class,
      * java.lang.String)
      */
     @Override
     @Deprecated
     public final <E> E find(Class<E> entityClass, Object rowId, List<String> relationNames) 
     {
         EntityMetadata entityMetadata = KunderaMetadataManager.getEntityMetadata(entityClass);
         return (E) find(entityClass, entityMetadata, rowId != null ? rowId.toString() : null, relationNames);
     }
 
     /*
      * (non-Javadoc)
      * 
      * @see com.impetus.kundera.client.Client#find(java.lang.Class,
      * com.impetus.kundera.metadata.model.EntityMetadata, java.lang.String)
      */
     public final Object find(Class<?> clazz, EntityMetadata metadata, Object rowId, List<String> relationNames)
     {
 
         List<Object> result = null;
         try
         {
             result = (List<Object>) find(clazz, relationNames, relationNames != null, metadata,
                     rowId != null ? rowId.toString() : null);
         }
         catch (Exception e)
         {
             log.error("Error on retrieval" + e.getMessage());
             throw new PersistenceException(e.getMessage());
         }
 
         return result != null & !result.isEmpty() ? result.get(0) : null;
     }
 
     /*
      * (non-Javadoc)
      * 
      * @see com.impetus.kundera.client.Client#find(java.lang.Class,
      * java.lang.String[])
      */
     @Override
     public final <E> List<E> findAll(Class<E> entityClass, Object... rowIds) 
     {
         EntityMetadata entityMetadata = KunderaMetadataManager.getEntityMetadata(entityClass);
         List<E> results = new ArrayList<E>();
         for (Object rowKey : rowIds)
         {
             E r = (E) find(entityClass, entityMetadata, rowKey.toString(), null);
             if (r != null)
             {
                 results.add(r);
             }
         }
 
         return results.isEmpty() ? null : results;
     }
 
     /**
      * Method to return list of entities for given below attributes:.
      * 
      * @param entityClass
      *            entity class
      * @param relationNames
      *            relation names
      * @param isWrapReq
      *            true, in case it needs to populate enhance entity.
      * @param metadata
      *            entity metadata.
      * @param rowIds
      *            array of row key s
      * @return list of wrapped entities.
      * @throws Exception
      *             throws exception. don't know why TODO: why is it throwing
      *             exception. need to take care as part of exception handling
      *             exercise.
      */
     public final List find(Class entityClass, List<String> relationNames, boolean isWrapReq, EntityMetadata metadata,
             String... rowIds)
     {
         if (!isOpen())
         {
             throw new PersistenceException("PelopsClient is closed.");
         }
 
         Selector selector = Pelops.createSelector(PelopsUtils.generatePoolName(getPersistenceUnit()));
 
         PelopsDataHandler handler = new PelopsDataHandler(this);
 
         List entities = null;
         try
         {
             entities = handler.fromThriftRow(selector, entityClass, metadata, relationNames, isWrapReq, rowIds);
         }
         catch (Exception e)
         {
             throw new KunderaException(e);
         }
 
         return entities;
     }
 
     /*
      * (non-Javadoc)
      * 
      * @see com.impetus.kundera.client.Client#find(java.lang.Class,
      * java.util.Map)
      */
     @Override
     public <E> List<E> find(Class<E> entityClass, Map<String, String> superColumnMap) 
     {
         List<E> entities = null;
         try
         {
             EntityMetadata entityMetadata = KunderaMetadataManager.getEntityMetadata(getPersistenceUnit(), entityClass);
             entities = new ArrayList<E>();
             for (String superColumnName : superColumnMap.keySet())
             {
                 String entityId = superColumnMap.get(superColumnName);
                 List<SuperColumn> superColumnList = loadSuperColumns(entityMetadata.getSchema(),
                         entityMetadata.getTableName(), entityId,
                         new String[] { superColumnName.substring(0, superColumnName.indexOf("|")) });
                 E e = (E) dataHandler.fromThriftRow(entityMetadata.getEntityClazz(), entityMetadata,
                         new DataRow<SuperColumn>(entityId, entityMetadata.getTableName(), superColumnList));
                 entities.add(e);
             }
         }
         catch (Exception e)
         {
             throw new KunderaException(e);
         }
         return entities;
     }
 
     /**
      * Load super columns.
      * 
      * @param keyspace
      *            the keyspace
      * @param columnFamily
      *            the column family
      * @param rowId
      *            the row id
      * @param superColumnNames
      *            the super column names
      * @return the list
      * @throws Exception
      *             the exception
      */
     private final List<SuperColumn> loadSuperColumns(String keyspace, String columnFamily, String rowId,
             String... superColumnNames) 
     {
         if (!isOpen())
             throw new PersistenceException("PelopsClient is closed.");
         Selector selector = Pelops.createSelector(PelopsUtils.generatePoolName(getPersistenceUnit()));
         return selector.getSuperColumnsFromRow(columnFamily, rowId, Selector.newColumnsPredicate(superColumnNames),
                 ConsistencyLevel.ONE);
     }
 
     /*
      * (non-Javadoc)
      * 
      * @see com.impetus.kundera.client.Client#delete(java.lang.Object,
      * java.lang.Object, com.impetus.kundera.metadata.model.EntityMetadata)
      */
     @Override
     public void delete(Object entity, Object pKey, EntityMetadata metadata)
     {
         if (!isOpen())
         {
             throw new PersistenceException("PelopsClient is closed.");
         }
 
         RowDeletor rowDeletor = Pelops.createRowDeletor(PelopsUtils.generatePoolName(getPersistenceUnit()));
         rowDeletor.deleteRow(metadata.getTableName(), pKey.toString(), ConsistencyLevel.ONE);
         getIndexManager().remove(metadata, entity, pKey.toString());
     }
 
     /*
      * (non-Javadoc)
      * 
      * @see com.impetus.kundera.client.Client#getIndexManager()
      */
     @Override
     public final IndexManager getIndexManager()
     {
         return indexManager;
     }
 
     /*
      * (non-Javadoc)
      * 
      * @see com.impetus.kundera.client.Client#getPersistenceUnit()
      */
     @Override
     public String getPersistenceUnit()
     {
         return persistenceUnit;
     }
 
     /**
      * Checks if is open.
      * 
      * @return true, if is open
      */
     private final boolean isOpen()
     {
         return !closed;
     }
 
     /*
      * (non-Javadoc)
      * 
      * @see com.impetus.kundera.client.Client#close()
      */
     @Override
     public final void close()
     {
         this.indexManager.flush();
         this.dataHandler = null;
         closed = true;
 
     }
 
     /*
      * (non-Javadoc)
      * 
      * @see com.impetus.kundera.client.Client#persist(java.lang.Object)
      */
     @Override
     public String persist(EntitySaveGraph entityGraph, EntityMetadata metadata)
     {
         try
         {
             Object entity = entityGraph.getParentEntity();
             String id = entityGraph.getParentId();
             PelopsDataHandler.ThriftRow tf = populateTfRow(entity, id, metadata);
 
             if (entityGraph.getRevFKeyName() != null)
             {
                 addRelation(entityGraph, metadata, entityGraph.getRevFKeyName(), entityGraph.getRevFKeyValue(), tf);
             }
 
             onPersist(metadata, entity, tf);
 
             if (entityGraph.getRevParentClass() != null)
             {
                 getIndexManager().write(metadata, entity, entityGraph.getRevFKeyValue(),
                         entityGraph.getRevParentClass());
             }
             else
             {
                 getIndexManager().write(metadata, entity);
             }
 
         }
         catch (Exception e)
         {            
             throw new KunderaException(e);
         }
         return null;
     }
 
     /*
      * (non-Javadoc)
      * 
      * @see com.impetus.kundera.client.Client#persist(java.lang.Object,
      * com.impetus.kundera.persistence.handler.impl.EntitySaveGraph,
      * com.impetus.kundera.metadata.model.EntityMetadata)
      */
     @Override
     public void persist(Object childEntity, EntitySaveGraph entitySaveGraph, EntityMetadata metadata)
     {
         // you got child entity and
         String rlName = entitySaveGraph.getfKeyName();
         String rlValue = entitySaveGraph.getParentId();
         String id = entitySaveGraph.getChildId();
         try
         {
             PelopsDataHandler.ThriftRow tf = populateTfRow(childEntity, id, metadata);
             if (rlName != null)
             {
                 addRelation(entitySaveGraph, metadata, rlName, rlValue, tf);
             }
 
             onPersist(metadata, childEntity, tf);
             onIndex(childEntity, entitySaveGraph, metadata, rlValue);
         }
         catch (Exception e)
         {
             throw new KunderaException(e);
         }
 
     }
 
     @Override
     public void persistJoinTable(String joinTableName, String joinColumnName, String inverseJoinColumnName,
             EntityMetadata relMetadata, Object primaryKey, Object childEntity)
     {
 
         String poolName = PelopsUtils.generatePoolName(getPersistenceUnit());
 
         Mutator mutator = Pelops.createMutator(poolName);
 
         String parentId = (String) primaryKey;
         List<Column> columns = new ArrayList<Column>();
 
         if (Collection.class.isAssignableFrom(childEntity.getClass()))
         {
             Collection children = (Collection) childEntity;
 
             for (Object child : children)
             {
 
                 addColumnsToJoinTable(inverseJoinColumnName, relMetadata, columns, child);
             }
 
         }
         else
         {
             addColumnsToJoinTable(inverseJoinColumnName, relMetadata, columns, childEntity);
         }
 
         createIndexesOnColumns(joinTableName, poolName, columns);
 
         // Pelops.createColumnFamilyManager(cluster, keyspace)
         mutator.writeColumns(joinTableName, new Bytes(parentId.getBytes()),
                 Arrays.asList(columns.toArray(new Column[0])));
         mutator.execute(ConsistencyLevel.ONE);
     }
 
     /**
      * Creates secondary indexes on columns if not already created
      * 
      * @param tableName
      *            Column family name
      * @param poolName
      *            Pool Name
      * @param columns
      *            List of columns
      */
     private void createIndexesOnColumns(String tableName, String poolName, List<Column> columns)
     {
         String keysapce = Pelops.getDbConnPool(poolName).getKeyspace();
         try
         {
             Cassandra.Client api = Pelops.getDbConnPool(poolName).getConnection().getAPI();
             KsDef ksDef = api.describe_keyspace(keysapce);
             List<CfDef> cfDefs = ksDef.getCf_defs();
 
             // Column family definition on which secondary index creation is
             // required
             CfDef columnFamilyDefToUpdate = null;
             for (CfDef cfDef : cfDefs)
             {
                 if (cfDef.getName().equals(tableName))
                 {
                     columnFamilyDefToUpdate = cfDef;
                     break;
                 }
             }
 
             // Get list of indexes already created
             List<ColumnDef> columnMetadataList = columnFamilyDefToUpdate.getColumn_metadata();
             List<String> indexList = new ArrayList<String>();
             for (ColumnDef columnDef : columnMetadataList)
             {
                 indexList.add(columnDef.getIndex_name());
             }
 
             // Iterate over all columns for creating secondary index on them
             for (Column column : columns)
             {
 
                 ColumnDef columnDef = new ColumnDef();
 
                 columnDef.setName(column.getName());
                 columnDef.setValidation_class("UTF8Type");
                 columnDef.setIndex_type(IndexType.KEYS);
 
                 String indexName = PelopsUtils.getSecondaryIndexName(tableName, column);
 
                 // Add secondary index only if it's not already created
                 // (if already created, it would be there in column family
                 // definition)
                 if (!indexList.contains(indexName))
                 {
                     columnFamilyDefToUpdate.addToColumn_metadata(columnDef);
                 }
             }
 
             // Finally, update column family with modified column family
             // definition
             api.system_update_column_family(columnFamilyDefToUpdate);
 
         }
         catch (InvalidRequestException e)
         {
             log.warn("Could not create secondary index on column family " + tableName + ".Details:" + e.getMessage());
 
         }
         catch (SchemaDisagreementException e)
         {
             log.warn("Could not create secondary index on column family " + tableName + ".Details:" + e.getMessage());
 
         }
         catch (TException e)
         {
             log.warn("Could not create secondary index on column family " + tableName + ".Details:" + e.getMessage());
 
         }
         catch (NotFoundException e)
         {
             log.warn("Could not create secondary index on column family " + tableName + ".Details:" + e.getMessage());
 
         }
         catch (PropertyAccessException e)
         {
             log.warn("Could not create secondary index on column family " + tableName + ".Details:" + e.getMessage());
 
         }
     }
 
     @Override
     public <E> List<E> getForeignKeysFromJoinTable(String joinTableName, String joinColumnName,
             String inverseJoinColumnName, EntityMetadata relMetadata, EntitySaveGraph objectGraph)
     {
         String parentId = objectGraph.getParentId();
         Selector selector = Pelops.createSelector(PelopsUtils.generatePoolName(getPersistenceUnit()));
         List<Column> columns = selector.getColumnsFromRow(joinTableName, new Bytes(parentId.getBytes()),
                 Selector.newColumnsPredicateAll(true, 10), ConsistencyLevel.ONE);
 
         PelopsDataHandler handler = new PelopsDataHandler(this);
         List<E> foreignKeys = handler.getForeignKeysFromJoinTable(inverseJoinColumnName, columns);
         return foreignKeys;
     }
 
     @Override
     public List<Object> findParentEntityFromJoinTable(EntityMetadata parentMetadata, String joinTableName,
             String joinColumnName, String inverseJoinColumnName, Object childId)
     {
 
         Selector selector = Pelops.createSelector(PelopsUtils.generatePoolName(getPersistenceUnit()));
         SlicePredicate slicePredicate = Selector.newColumnsPredicateAll(false, 10000);
 
         String childIdStr = (String) childId;
 
         List<Object> entities = null;
         IndexClause ix = Selector.newIndexClause(
                 Bytes.EMPTY,
                 10000,
                 Selector.newIndexExpression(inverseJoinColumnName + "_" + childIdStr, IndexOperator.EQ,
                         Bytes.fromByteArray(childIdStr.getBytes())));
 
         Map<Bytes, List<Column>> qResults = selector.getIndexedColumns(joinTableName, ix, slicePredicate,
                 ConsistencyLevel.ONE);
 
         List<Object> rowKeys = new ArrayList<Object>();
         entities = new ArrayList<Object>(qResults.size());
 
         try
         {
             // iterate through complete map and
             Iterator<Bytes> rowIter = qResults.keySet().iterator();
             while (rowIter.hasNext())
             {
                 Bytes rowKey = rowIter.next();
 
                 PropertyAccessor<?> accessor = PropertyAccessorFactory.getPropertyAccessor(parentMetadata.getIdColumn()
                         .getField());
                 Object value = accessor.fromBytes(parentMetadata.getIdColumn()
                         .getField().getClass(), rowKey.toByteArray());
 
                 rowKeys.add(value);
             }
 
             entities.addAll(findAll(parentMetadata.getEntityClazz(), rowKeys.toArray(new Object[0])));
         }
         catch (Exception e)
         {
             throw new KunderaException(e);
         }
 
         return entities;
 
     }
 
     /*
      * (non-Javadoc)
      * 
      * @see
      * com.impetus.kundera.client.Client#deleteFromJoinTable(java.lang.String,
      * java.lang.String, java.lang.String,
      * com.impetus.kundera.metadata.model.EntityMetadata,
      * com.impetus.kundera.persistence.handler.impl.EntitySaveGraph)
      */
     @Override
     public void deleteFromJoinTable(String joinTableName, String joinColumnName, String inverseJoinColumnName,
             EntityMetadata relMetadata, EntitySaveGraph objectGraph)
     {
         if (!isOpen())
         {
             throw new PersistenceException("PelopsClient is closed.");
         }
 
         String pKey = objectGraph.getParentId();
 
         RowDeletor rowDeletor = Pelops.createRowDeletor(PelopsUtils.generatePoolName(getPersistenceUnit()));
         rowDeletor.deleteRow(joinTableName, pKey, ConsistencyLevel.ONE);
     }
 
     /**
      * Adds the columns to join table.
      * 
      * @param inverseJoinColumnName
      *            the inverse join column name
      * @param relMetadata
      *            the rel metadata
      * @param columns
      *            the columns
      * @param child
      *            the child
      */
     private void addColumnsToJoinTable(String inverseJoinColumnName, EntityMetadata relMetadata, List<Column> columns,
             Object child)
     {
         String childId = null;
         try
         {
             childId = PropertyAccessorHelper.getId(child, relMetadata);
             Column col = new Column();
             col.setName(PropertyAccessorFactory.STRING.toBytes(inverseJoinColumnName + "_" + childId));
             col.setValue(PropertyAccessorFactory.STRING.toBytes(childId));
             col.setTimestamp(System.currentTimeMillis());
 
             columns.add(col);
         }
         catch (PropertyAccessException e)
         {
             throw new KunderaException(e);
         }
 
     }
 
     /**
      * Find.
      * 
      * @param ixClause
      *            the ix clause
      * @param m
      *            the m
      * @param isRelation
      *            the is relation
      * @param relations
      *            the relations
      * @return the list
      */
     public List find(List<IndexClause> ixClause, EntityMetadata m, boolean isRelation, List<String> relations)
     {
         // ixClause can be 0,1 or more!
         Selector selector = Pelops.createSelector(PelopsUtils.generatePoolName(getPersistenceUnit()));
 
         SlicePredicate slicePredicate = Selector.newColumnsPredicateAll(false, Integer.MAX_VALUE);
         List<Object> entities = null;
         if (ixClause.isEmpty())
         {
             Map<Bytes, List<Column>> qResults = selector.getColumnsFromRows(m.getTableName(),
                     selector.newKeyRange("", "", 100), slicePredicate, ConsistencyLevel.ONE);
             entities = new ArrayList<Object>(qResults.size());
             populateData(m, qResults, entities, isRelation, relations);
         }
         else
         {
             entities = new ArrayList<Object>();
             for (IndexClause ix : ixClause)
             {
                 Map<Bytes, List<Column>> qResults = selector.getIndexedColumns(m.getTableName(), ix, slicePredicate,
                         ConsistencyLevel.ONE);
                 // iterate through complete map and
                 populateData(m, qResults, entities, isRelation, relations);
             }
         }
         return entities;
     }
 
     /**
      * Find by range.
      * 
      * @param minVal
      *            the min val
      * @param maxVal
      *            the max val
      * @param m
      *            the m
      * @param isWrapReq
      *            the is wrap req
      * @param relations
      *            the relations
      * @return the list
      * @throws Exception
      *             the exception
      */
     public List findByRange(byte[] minVal, byte[] maxVal, EntityMetadata m, boolean isWrapReq, List<String> relations)
             throws Exception
     {
         Selector selector = Pelops.createSelector(PelopsUtils.generatePoolName(getPersistenceUnit()));
 
         SlicePredicate slicePredicate = Selector.newColumnsPredicateAll(false, Integer.MAX_VALUE);
         List<Object> entities = null;
 
         List<KeySlice> keys = selector.getKeySlices(new ColumnParent(m.getTableName()),
                 selector.newKeyRange(Bytes.fromByteArray(minVal), Bytes.fromByteArray(maxVal), 10000), slicePredicate,
                 ConsistencyLevel.ONE);
 
         List<String> superColumnNames = m.getEmbeddedColumnFieldNames();
 
         List results = null;
         if (keys != null)
         {
             results = new ArrayList(keys.size());
             for (KeySlice key : keys)
             {
                 List<ColumnOrSuperColumn> columns = key.getColumns();
                 byte[] rowKey = key.getKey();
 
                 if (!superColumnNames.isEmpty())
                 {
                     List<SuperColumn> superColumns = new ArrayList<SuperColumn>(columns.size());
 
                     for (ColumnOrSuperColumn supCol : columns)
                     {
                         superColumns.add(supCol.getSuper_column());
                     }
 
                     Object r = dataHandler.fromSuperColumnThriftRow(m.getEntityClazz(), m, dataHandler.new ThriftRow(
                             new String(rowKey), m.getTableName(), null, superColumns), relations, isWrapReq);
                     results.add(r);
                     // List<SuperColumn> superCol = columns.
                 }
                 else
                 {
                     List<Column> cols = new ArrayList<Column>(columns.size());
                     for (ColumnOrSuperColumn supCol : columns)
                     {
                         cols.add(supCol.getColumn());
                     }
 
                     Object r = dataHandler.fromColumnThriftRow(m.getEntityClazz(), m, dataHandler.new ThriftRow(
                             new String(rowKey), m.getTableName(), cols, null), relations, isWrapReq);
                     results.add(r);
                 }
             }
         }
 
         return results;
     }
 
     /**
      * Populate data.
      * 
      * @param m
      *            the m
      * @param qResults
      *            the q results
      * @param entities
      *            the entities
      * @param isRelational
      *            the is relational
      * @param relationNames
      *            the relation names
      */
     private void populateData(EntityMetadata m, Map<Bytes, List<Column>> qResults, List<Object> entities,
             boolean isRelational, List<String> relationNames)
     {
         Iterator<Bytes> rowIter = qResults.keySet().iterator();
         while (rowIter.hasNext())
         {
             Bytes rowKey = rowIter.next();
             List<Column> columns = qResults.get(rowKey);
             try
             {
                 Object e = dataHandler.fromColumnThriftRow(m.getEntityClazz(), m,
                         dataHandler.new ThriftRow(Bytes.toUTF8(rowKey.toByteArray()), m.getTableName(), columns, null),
                         relationNames, isRelational);
                 entities.add(e);
             }
             catch (IllegalStateException e)
             {
                 throw new KunderaException(e);
             }
             catch (Exception e)
             {
                 throw new KunderaException(e);            }
         }
     }
 
     /*
      * (non-Javadoc)
      * 
      * @see com.impetus.kundera.client.Client#find(java.lang.String,
      * java.lang.String, com.impetus.kundera.metadata.model.EntityMetadata)
      */
     public List<Object> find(String colName, String colValue, EntityMetadata m)
     {
         Selector selector = Pelops.createSelector(PelopsUtils.generatePoolName(getPersistenceUnit()));
 
         SlicePredicate slicePredicate = Selector.newColumnsPredicateAll(false, 10000);
         List<Object> entities = null;
         IndexClause ix = Selector.newIndexClause(Bytes.EMPTY, 10000,
                 Selector.newIndexExpression(colName, IndexOperator.EQ, Bytes.fromByteArray(colValue.getBytes())));
         Map<Bytes, List<Column>> qResults = selector.getIndexedColumns(m.getTableName(), ix, slicePredicate,
                 ConsistencyLevel.ONE);
         entities = new ArrayList<Object>(qResults.size());
         // iterate through complete map and
         populateData(m, qResults, entities, false, null);
 
         return entities;
     }
 
     /**
      * Find.
      * 
      * @param m
      *            the m
      * @param relationNames
      *            the relation names
      * @param conditions
      *            the conditions
      * @return the list
      */
     public List<EnhanceEntity> find(EntityMetadata m, List<String> relationNames, List<IndexClause> conditions)
     {
 
         return (List<EnhanceEntity>) find(conditions, m, true, relationNames);
     }
 
     /**
      * On index.
      * 
      * @param childEntity
      *            the child entity
      * @param entitySaveGraph
      *            the entity save graph
      * @param metadata
      *            the metadata
      * @param rlValue
      *            the rl value
      */
     private void onIndex(Object childEntity, EntitySaveGraph entitySaveGraph, EntityMetadata metadata, String rlValue)
     {
         if (!entitySaveGraph.isSharedPrimaryKey())
         {
             getIndexManager().write(metadata, childEntity, rlValue, entitySaveGraph.getParentEntity().getClass());
         }
         else
         {
             getIndexManager().write(metadata, childEntity);
         }
     }
 
     /**
      * Adds the relation.
      * 
      * @param entitySaveGraph
      *            the entity save graph
      * @param m
      *            the m
      * @param rlName
      *            the rl name
      * @param rlValue
      *            the rl value
      * @param tf
      *            the tf
      * @throws PropertyAccessException
      *             the property access exception
      */
     private void addRelation(EntitySaveGraph entitySaveGraph, EntityMetadata m, String rlName, String rlValue,
             PelopsDataHandler.ThriftRow tf) throws PropertyAccessException
     {
         if (!entitySaveGraph.isSharedPrimaryKey())
         {
             if (m.getEmbeddedColumnsAsList().isEmpty())
             {
                 Column col = populateFkey(rlName, rlValue, timestamp);
                 tf.addColumn(col);
             }
             else
             {
                 SuperColumn superColumn = new SuperColumn();
                 superColumn.setName(rlName.getBytes());
                 Column column = populateFkey(rlName, rlValue, timestamp);
                 superColumn.addToColumns(column);
                 tf.addSuperColumn(superColumn);
             }
 
         }
     }
 
     /**
      * Populates foreign key as column.
      * 
      * @param rlName
      *            relation name
      * @param rlValue
      *            relation value
      * @param timestamp
      *            the timestamp
      * @return the column
      * @throws PropertyAccessException
      *             the property access exception
      */
     private Column populateFkey(String rlName, String rlValue, long timestamp) throws PropertyAccessException
     {
         Column col = new Column();
         col.setName(PropertyAccessorFactory.STRING.toBytes(rlName));
         col.setValue(rlValue.getBytes());
         col.setTimestamp(timestamp);
         return col;
     }
 
     /**
      * Populate tf row.
      * 
      * @param entity
      *            the entity
      * @param id
      *            the id
      * @param metadata
      *            the metadata
      * @return the pelops data handler n. thrift row
      * @throws Exception
      *             the exception
      */
     private PelopsDataHandler.ThriftRow populateTfRow(Object entity, String id, EntityMetadata metadata)
             throws Exception
     {
 
         String columnFamily = metadata.getTableName();
 
         if (!isOpen())
         {
             throw new PersistenceException("PelopsClient is closed.");
         }
 
         PelopsDataHandler handler = new PelopsDataHandler(this);
         PelopsDataHandler.ThriftRow tf = handler.toThriftRow(this, entity, id, metadata, columnFamily);
         timestamp = handler.getTimestamp();
         return tf;
     }
 
     /**
      * On persist.
      * 
      * @param metadata
      *            the metadata
      * @param entity
      *            the entity
      * @param tf
      *            the tf
      */
     private void onPersist(EntityMetadata metadata, Object entity, PelopsDataHandler.ThriftRow tf)
     {
         Mutator mutator = Pelops.createMutator(PelopsUtils.generatePoolName(getPersistenceUnit()));
 
         List<Column> thriftColumns = tf.getColumns();
         List<SuperColumn> thriftSuperColumns = tf.getSuperColumns();
         if (thriftColumns != null && !thriftColumns.isEmpty())
         {
             // Bytes.fromL
             mutator.writeColumns(metadata.getTableName(), new Bytes(tf.getId().getBytes()),
                     Arrays.asList(tf.getColumns().toArray(new Column[0])));
         }
 
         if (thriftSuperColumns != null && !thriftSuperColumns.isEmpty())
         {
             for (SuperColumn sc : thriftSuperColumns)
             {
                 mutator.writeSubColumns(metadata.getTableName(), tf.getId(), Bytes.toUTF8(sc.getName()),
                         sc.getColumns());
 
             }
 
         }
 
         mutator.execute(ConsistencyLevel.ONE);
         tf = null;
     }
 
     /*
      * (non-Javadoc)
      * 
      * @see com.impetus.kundera.client.Client#getReader()
      */
     @Override
     public EntityReader getReader()
     {
         return reader;
     }
 
 }
