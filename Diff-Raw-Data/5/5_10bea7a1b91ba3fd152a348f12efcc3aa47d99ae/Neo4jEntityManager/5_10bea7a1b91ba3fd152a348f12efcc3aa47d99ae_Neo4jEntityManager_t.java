 package org.springframework.datastore.graph.neo4j.jpa;
 
 import org.neo4j.graphdb.*;
 import org.neo4j.graphdb.Transaction;
 import org.neo4j.index.IndexService;
 import org.neo4j.kernel.EmbeddedGraphDatabase;
 import org.springframework.beans.factory.annotation.Configurable;
 import org.springframework.datastore.graph.api.NodeBacked;
 import org.springframework.datastore.graph.neo4j.finder.FinderFactory;
 import org.springframework.persistence.support.EntityInstantiator;
 import org.springframework.transaction.annotation.Transactional;
 
 import javax.annotation.Resource;
 import javax.persistence.*;
 import javax.persistence.criteria.CriteriaBuilder;
 import javax.persistence.criteria.CriteriaQuery;
 import javax.persistence.metamodel.Metamodel;
 import javax.persistence.spi.PersistenceUnitInfo;
 import javax.transaction.*;
 import java.util.Map;
 
 /**
  * @author Michael Hunger
  * @since 20.08.2010
  *        TODO Relationships
  */
 //@Service
 @Transactional
 @Configurable
 public class Neo4jEntityManager implements EntityManager {
     private GraphDatabaseService graphDatabaseService;
     private EntityInstantiator<NodeBacked, Node> nodeInstantiator;
     private PersistenceUnitInfo info;
 
     private Map params;
     private IndexService indexService;
     private volatile boolean closed;
     private final FinderFactory finderFactory;
 
     @Resource
    private EntityManagerFactory entityManagerFactory;
 
     public Neo4jEntityManager(final GraphDatabaseService graphDatabaseService, final EntityInstantiator<NodeBacked, Node> nodeInstantiator, final PersistenceUnitInfo info, final Map params, final IndexService indexService) {
         this.graphDatabaseService = graphDatabaseService;
         this.nodeInstantiator = nodeInstantiator;
         this.info = info;
         this.params = params;
         this.indexService = indexService;
         finderFactory = new FinderFactory(graphDatabaseService, nodeInstantiator, indexService);
     }
 
     public Neo4jEntityManager() {
         finderFactory = new FinderFactory(graphDatabaseService, nodeInstantiator, indexService);
     }
 
     private Node nodeFor(final Object entity) {
         checkClosed();
         if (!(entity instanceof NodeBacked)) throw new IllegalArgumentException("Not a nodebacked entity " + entity);
         final Node node = ((NodeBacked) entity).getUnderlyingNode();
         if (node == null) throw new IllegalArgumentException("Node of entity " + entity + " is null");
         return node;
     }
 
     @Override
     public void persist(final Object entity) {
         checkClosed();
         final Transaction tx = graphDatabaseService.beginTx();
         try {
             //todo nodebacked justwriteback, no check
             tx.success();
         } finally {
             tx.finish();
         }
     }
 
     @Override
     public <T> T merge(final T entity) {
         // todo nodebacked merge
         checkClosed();
         return entity;
     }
 
     @Override
     public void remove(final Object entity) {
         final Node node = nodeFor(entity);
         for (final Relationship r : node.getRelationships()) {
         	r.delete();
         }
 		node.delete();
     }
 
     @SuppressWarnings({"unchecked"})
     @Override
     public <T> T find(final Class<T> entityClass, final Object primaryKey) {
         checkClosed();
         if (!NodeBacked.class.isAssignableFrom(entityClass))
             throw new IllegalArgumentException("Not a nodebacked entity type " + entityClass);
         if (!(primaryKey instanceof Number))
             throw new IllegalArgumentException("Primary Key for entity type " + entityClass + " is not a number " + primaryKey);
         final Node node = graphDatabaseService.getNodeById(((Number) primaryKey).longValue());
         return (T) nodeInstantiator.createEntityFromState(node, (Class<? extends NodeBacked>) entityClass);
     }
 
     @Override
     public <T> T find(final Class<T> entityClass, final Object primaryKey, final Map<String, Object> params) {
         return find(entityClass, primaryKey);
     }
 
     @Override
     public <T> T find(final Class<T> entityClass, final Object primaryKey, final LockModeType lockModeType) {
         return find(entityClass, primaryKey);
     }
 
     @Override
     public <T> T find(final Class<T> entityClass, final Object primaryKey, final LockModeType lockModeType, final Map<String, Object> params) {
         return find(entityClass, primaryKey);
     }
 
     @Override
     public <T> T getReference(final Class<T> entityClass, final Object primaryKey) {
         return find(entityClass, primaryKey);
     }
 
     @Override
     public void flush() {
         checkClosed();
     }
 
     @Override
     public void setFlushMode(final FlushModeType flushMode) {
         checkClosed();
     }
 
     @Override
     public FlushModeType getFlushMode() {
         checkClosed();
         return FlushModeType.AUTO;
     }
 
     @Override
     public void lock(final Object entity, final LockModeType lockModeType) {
         nodeFor(entity);
     }
 
     @Override
     public void lock(final Object entity, final LockModeType lockModeType, final Map<String, Object> params) {
         lock(entity,lockModeType);
     }
 
     @Override
     public void refresh(final Object entity) {
         // todo nodebacked.refresh, throw away dirty
         nodeFor(entity);
     }
 
     @Override
     public void refresh(final Object entity, final Map<String, Object> params) {
         refresh(entity);
     }
 
     @Override
     public void refresh(final Object entity, final LockModeType lockModeType) {
         refresh(entity);
     }
 
     @Override
     public void refresh(final Object entity, final LockModeType lockModeType, final Map<String, Object> params) {
         refresh(entity);
     }
 
     /**
      * Clear the persistence context, causing all managed entities to become detached.
      */
     @Override
     public void clear() {
         checkClosed();
     }
 
     @Override
     public void detach(final Object entity) {
     }
 
     @Override
     public boolean contains(final Object entity) {
         checkClosed();
         try {
             return nodeFor(entity) != null;
         } catch (IllegalArgumentException e) {
             return false;
         }
     }
 
     @Override
     public LockModeType getLockMode(final Object entity) {
         return LockModeType.NONE; // todo use neo4j locks?
     }
 
     @Override
     public void setProperty(final String name, final Object value) {
         params.put(name,value);
     }
 
     @Override
     public Map<String, Object> getProperties() {
         return params;
     }
 
     /*
     TODO Gremlin
      */
 
     @Override
     public Query createQuery(final String qlString) {
         return createQuery(qlString,null);
     }
 
     @Override
     public <T> TypedQuery<T> createQuery(final CriteriaQuery<T> query) {
         return null;
     }
 
     @SuppressWarnings({"unchecked"})
     @Override
     public <T> TypedQuery<T> createQuery(final String qlString, final Class<T> entityClass) {
         checkClosed();
         return (TypedQuery<T>)createNeo4jQuery(qlString,(Class<? extends NodeBacked>)entityClass);
     }
 
     public <T extends NodeBacked> TypedQuery<T> createNeo4jQuery(final String qlString, final Class<T> entityClass) {
         return new Neo4JQuery<T>(qlString, finderFactory,info,entityClass);
     }
 
     /*
     TODO Gremlin
      */
 
     @Override
     public Query createNamedQuery(final String name) {
         checkClosed();
         throw new UnsupportedOperationException();
     }
 
     @Override
     public <T> TypedQuery<T> createNamedQuery(final String s, final Class<T> entityClass) {
         return null;
     }
 
     /*
     TODO Gremlin
      */
 
     @Override
     public Query createNativeQuery(final String sqlString) {
         checkClosed();
         throw new UnsupportedOperationException();
     }
 
     /*
     TODO Gremlin
      */
 
     @Override
     public Query createNativeQuery(final String sqlString, final Class resulentityClass) {
         checkClosed();
         throw new UnsupportedOperationException();
     }
 
     /*
     TODO Gremlin
      */
 
     @Override
     public Query createNativeQuery(final String sqlString, final String resultSetMapping) {
         checkClosed();
         throw new UnsupportedOperationException();
     }
 
     /*
     TODO Johan
      */
 
     @Override
     public void joinTransaction() {
         checkClosed();
         throw new UnsupportedOperationException();
     }
 
     @Override
     public <T> T unwrap(final Class<T> entityClass) {
         return null;
     }
 
     @Override
     public Object getDelegate() {
         checkClosed();
         return graphDatabaseService;
     }
 
     @Override
     public void close() {
         checkClosed();
         this.closed = true;
     }
 
     @Override
     public boolean isOpen() {
         return !closed;
     }
 
     private void checkClosed() {
         if (closed) throw new IllegalStateException("EntityManager is closed");
     }
 
     @Override
     public EntityTransaction getTransaction() {
         final TransactionManager transactionManager = ((EmbeddedGraphDatabase) graphDatabaseService).getConfig().getTxModule().getTxManager();
         return new Neo4jEntityTransaction(transactionManager);
     }
 
     @Override
     public EntityManagerFactory getEntityManagerFactory() {
        return entityManagerFactory;
     }
 
     @Override
     public CriteriaBuilder getCriteriaBuilder() {
         return null;
     }
 
     @Override
     public Metamodel getMetamodel() {
         return null;
     }
 
 
 }
