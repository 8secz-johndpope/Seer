 /*
  * Copyright: (c) 2004-2009 Mayo Foundation for Medical Education and 
  * Research (MFMER). All rights reserved. MAYO, MAYO CLINIC, and the
  * triple-shield Mayo logo are trademarks and service marks of MFMER.
  *
  * Except as contained in the copyright notice above, or as used to identify 
  * MFMER as the author of this software, the trade names, trademarks, service
  * marks, or product names of the copyright holder shall not be used in
  * advertising, promotion or otherwise in connection with this software without
  * prior written authorization of the copyright holder.
  * 
  * Licensed under the Eclipse Public License, Version 1.0 (the "License");
  * you may not use this file except in compliance with the License.
  * You may obtain a copy of the License at 
  * 
  * 		http://www.eclipse.org/legal/epl-v10.html
  * 
  */
 package org.lexevs.dao.database.ibatis.entity;
 
 import java.sql.SQLException;
 import java.util.HashMap;
 import java.util.List;
 import java.util.Map;
 
 import org.LexGrid.LexBIG.DataModel.Core.ResolvedConceptReference;
 import org.LexGrid.commonTypes.Property;
 import org.LexGrid.concepts.Entity;
 import org.LexGrid.concepts.PropertyLink;
 import org.LexGrid.relations.AssociationEntity;
 import org.LexGrid.util.sql.lgTables.SQLTableConstants;
 import org.LexGrid.versions.EntryState;
 import org.LexGrid.versions.types.ChangeType;
 import org.lexevs.dao.database.access.entity.EntityDao;
 import org.lexevs.dao.database.access.property.PropertyDao.PropertyType;
 import org.lexevs.dao.database.ibatis.AbstractIbatisDao;
 import org.lexevs.dao.database.ibatis.association.IbatisAssociationDao;
 import org.lexevs.dao.database.ibatis.batch.IbatisBatchInserter;
 import org.lexevs.dao.database.ibatis.batch.IbatisInserter;
 import org.lexevs.dao.database.ibatis.batch.SqlMapExecutorBatchInserter;
 import org.lexevs.dao.database.ibatis.entity.parameter.InsertOrUpdateEntityBean;
 import org.lexevs.dao.database.ibatis.parameter.PrefixedParameter;
 import org.lexevs.dao.database.ibatis.parameter.PrefixedParameterTriple;
 import org.lexevs.dao.database.ibatis.parameter.PrefixedParameterTuple;
 import org.lexevs.dao.database.ibatis.property.IbatisPropertyDao;
 import org.lexevs.dao.database.ibatis.versions.IbatisVersionsDao;
 import org.lexevs.dao.database.schemaversion.LexGridSchemaVersion;
 import org.lexevs.dao.database.utility.DaoUtility;
 import org.springframework.orm.ibatis.SqlMapClientCallback;
 import org.springframework.util.Assert;
 
 import com.ibatis.sqlmap.client.SqlMapExecutor;
 
 /**
  * The Class IbatisEntityDao.
  * 
  * @author <a href="mailto:kevin.peterson@mayo.edu">Kevin Peterson</a>
  */
 public class IbatisEntityDao extends AbstractIbatisDao implements EntityDao {
 	
 	/** The supported datebase version. */
 	private LexGridSchemaVersion supportedDatebaseVersion = LexGridSchemaVersion.parseStringToVersion("2.0");
 	
 	/** The ENTIT y_ namespace. */
 	public static String ENTITY_NAMESPACE = "Entity.";
 	
 	/** The INSER t_ entit y_ sql. */
 	public static String INSERT_ENTITY_SQL = ENTITY_NAMESPACE + "insertEntity";
 	
 	/** The INSER t_ entit y_ typ e_ sql. */
 	public static String INSERT_ENTITY_TYPE_SQL = ENTITY_NAMESPACE + "insertEntityType";
 	
 	/** The GE t_ entit y_ b y_ cod e_ an d_ namespac e_ sql. */
 	public static String GET_ENTITY_BY_CODE_AND_NAMESPACE_SQL = ENTITY_NAMESPACE + "getEntityByCodeAndNamespace";
 	
 	public static String GET_RESOLVED_CODED_NODE_REFERENCE_BY_CODE_AND_NAMESPACE_SQL = ENTITY_NAMESPACE + "getResolvedCodedNodeReferenceByCodeAndNamespace";
 	
 	public static String GET_ENTITY_BY_ID_AND_REVISION_ID_SQL = ENTITY_NAMESPACE + "getEntityByIdAndRevisionId";
 	
 	/** The GE t_ entit y_ coun t_ sql. */
 	public static String GET_ENTITY_COUNT_SQL = ENTITY_NAMESPACE + "getEntityCount";
 	
 	/** The GE t_ entitie s_ o f_ codin g_ schem e_ sql. */
 	public static String GET_ENTITIES_OF_CODING_SCHEME_SQL = ENTITY_NAMESPACE + "getAllEntitiesOfCodingScheme";
 	
 	public static String GET_ENTITY_ID_BY_CODE_AND_NAMESPACE = ENTITY_NAMESPACE + "getEntityIdByCodeAndNamespace";
 	
 	/** The ENTIT y_ cod e_ param. */
 	public static String ENTITY_CODE_PARAM = SQLTableConstants.TBLCOL_ENTITYCODE;
 	
 	/** The ENTIT y_ cod e_ namespac e_ param. */
 	public static String ENTITY_CODE_NAMESPACE_PARAM = SQLTableConstants.TBLCOL_ENTITYCODENAMESPACE;
 	
 	public static String GET_ENTITY_BY_ID_SQL = ENTITY_NAMESPACE + "getEntityById";
 	
 	public static String UPDATE_ENTITY_BY_UID_SQL = ENTITY_NAMESPACE + "updateEntityByUId";
 	
 	public static String GET_PROPERTY_LINKS_BY_ENTITY_ID_SQL = ENTITY_NAMESPACE + "getPropertyLinksByEntityId";
 	
 	public static String GET_ENTITY_ATTRIBUTES_BY_UID_SQL = ENTITY_NAMESPACE + "getEntityAttributesByEntityUId";
 	
 	/** update codingScheme versionableAttrib. */
 	private static String UPDATE_ENTITY_VER_ATTRIB_BY_ID_SQL = ENTITY_NAMESPACE + "updateEntityVerAttribByUId";
 	
 	/** The ENTITY. */
 	public static String ENTITY = "entity";
 	
 	/** The ENTIT y_ i d_ param. */
 	public static String ENTITY_ID_PARAM = "entityId";
 	
 	/** The ibatis versions dao. */
 	private IbatisVersionsDao ibatisVersionsDao;
 
 	/** The ibatis property dao. */
 	private IbatisPropertyDao ibatisPropertyDao;
 	
 	private IbatisAssociationDao ibatisAssociationDao;
 
 	/* (non-Javadoc)
 	 * @see org.lexevs.dao.database.access.entity.EntityDao#getEntityByCodeAndNamespace(java.lang.String, java.lang.String, java.lang.String)
 	 */
 	public Entity getEntityByCodeAndNamespace(String codingSchemeId, String entityCode, String entityCodeNamespace){
 		String prefix = this.getPrefixResolver().resolvePrefixForCodingScheme(codingSchemeId);
 		
 		String entityId = this.getEntityUId(codingSchemeId, entityCode, entityCodeNamespace);
 
 		return doGetEntity(prefix, codingSchemeId, entityId);
 	}
 	
 	@Override
 	public ResolvedConceptReference getResolvedCodedNodeReferenceByCodeAndNamespace(
 			String codingSchemeId, String entityCode, String entityCodeNamespace) {
 		String prefix = this.getPrefixResolver().resolvePrefixForCodingScheme(codingSchemeId);
 		
 		PrefixedParameterTriple triple = 
 			new PrefixedParameterTriple(prefix, codingSchemeId, entityCode, entityCodeNamespace);
 		
 		
 		return
 			(ResolvedConceptReference) 
 				this.getSqlMapClientTemplate().queryForObject(GET_RESOLVED_CODED_NODE_REFERENCE_BY_CODE_AND_NAMESPACE_SQL, triple);
 
 	}
 
 	public Entity getEntityByUId(String codingSchemeId, String entityId){
 		String prefix = this.getPrefixResolver().resolvePrefixForCodingScheme(codingSchemeId);
 		
 		return doGetEntity(prefix, codingSchemeId, entityId);
 	}
 	
 	@Override
 	public Entity getHistoryEntityByRevision(String codingSchemeId, String entityId, String revisionId) {
 		String prefix = this.getPrefixResolver().resolveHistoryPrefix();
 		String actualTableSetPrefix = this.getPrefixResolver().resolvePrefixForCodingScheme(codingSchemeId);
 		
 		PrefixedParameterTuple tuple = 
 			new PrefixedParameterTuple(prefix, entityId, revisionId);
 		
 		tuple.setActualTableSetPrefix(actualTableSetPrefix);
 		
 		return (Entity) this.getSqlMapClientTemplate().queryForObject(GET_ENTITY_BY_ID_AND_REVISION_ID_SQL, 
 				tuple);
 	}
 	
 	protected Entity doGetEntity(String prefix, String codingSchemeId, String entityId) {
 		Entity entity = (Entity) this.getSqlMapClientTemplate().queryForObject(GET_ENTITY_BY_ID_SQL, 
 				new PrefixedParameterTuple(prefix, entityId, codingSchemeId));
 		
 		if(entity == null) {return null;}
 			
 		entity.addAnyProperties(
 				ibatisPropertyDao.getAllPropertiesOfParent(codingSchemeId, entityId, PropertyType.ENTITY));
 		
 		entity.setPropertyLink(
 				doGetPropertyLinks(prefix, codingSchemeId, entityId));
 		
 		return entity;
 	}
 	
 	@SuppressWarnings("unchecked")
 	protected List<PropertyLink> doGetPropertyLinks(String prefix, String codingSchemeId, String entityId){
 		return this.getSqlMapClientTemplate().
 			queryForList(GET_PROPERTY_LINKS_BY_ENTITY_ID_SQL, 
 				new PrefixedParameterTuple(prefix, codingSchemeId, entityId));
 	}
 
 	/* (non-Javadoc)
 	 * @see org.lexevs.dao.database.access.entity.EntityDao#getEntityCount(java.lang.String)
 	 */
 	@Override
 	public int getEntityCount(String codingSchemeId) {
 		String prefix = this.getPrefixResolver().resolvePrefixForCodingScheme(codingSchemeId);
 		
 		return (Integer) 
 			this.getSqlMapClientTemplate().queryForObject(GET_ENTITY_COUNT_SQL, new PrefixedParameter(prefix, codingSchemeId));
 	}
 
 	/* (non-Javadoc)
 	 * @see org.lexevs.dao.database.access.entity.EntityDao#updateEntity(java.lang.String, org.LexGrid.concepts.Entity)
 	 */
 	public void updateEntity(String codingSchemeId,
 			Entity entity) {
 		String entityId = this.getEntityUId(codingSchemeId, entity.getEntityCode(), entity.getEntityCodeNamespace());
 		String prefix = this.getPrefixResolver().resolvePrefixForCodingScheme(codingSchemeId);
 		
 		this.doUpdateEntity(prefix, codingSchemeId, entityId, entity);	
 	}
 	
 	@Override
 	public void updateEntityVersionableAttrib(String codingSchemeUId, String entityUId, Entity entity) {
 		String prefix = this.getPrefixResolver().resolvePrefixForCodingScheme(codingSchemeUId);
 		
 		InsertOrUpdateEntityBean bean = new InsertOrUpdateEntityBean();
 		bean.setPrefix(prefix);
 		bean.setEntity(entity);
 		bean.setCodingSchemeUId(codingSchemeUId);
 		bean.setUId(entityUId);
 		
 		this.getSqlMapClientTemplate().update(UPDATE_ENTITY_VER_ATTRIB_BY_ID_SQL, bean);
 	}
 	
 	protected void doUpdateEntity(String prefix, String codingSchemeId, String entityId, Entity entity) {
 		Assert.hasText(entity.getEntityCode(), "An Entity Code is required to be populated to Update an Entity.");
 		Assert.hasText(entity.getEntityCodeNamespace(), "An Entity Code Namespace is required to be populated to Update an Entity.");
 	
 		InsertOrUpdateEntityBean bean = new InsertOrUpdateEntityBean();
 		bean.setPrefix(prefix);
 		bean.setEntity(entity);
 		bean.setCodingSchemeUId(codingSchemeId);
 		bean.setUId(entityId);
 		
 		this.getSqlMapClientTemplate().update(UPDATE_ENTITY_BY_UID_SQL, bean);
 	}
 	
 	public void updateEntity(String codingSchemeId,
 			AssociationEntity entity) {
 		String entityId = this.getEntityUId(codingSchemeId, entity.getEntityCode(), entity.getEntityCodeNamespace());
 		String prefix = this.getPrefixResolver().resolvePrefixForCodingScheme(codingSchemeId);
 		
 		this.doUpdateEntity(prefix, codingSchemeId, entityId, entity);	
 		
 		this.ibatisAssociationDao.updateAssociationEntity(codingSchemeId, entityId, entity);
 	}
 	
 	@Override
 	public String insertEntity(String codingSchemeId, Entity entity,
 			boolean cascade) {
 		
 		String prefix = this.getPrefixResolver().resolvePrefixForCodingScheme(codingSchemeId);
 		return this.doInsertEntity(
 				prefix, 
 				codingSchemeId, 
 				entity, 
 				this.getNonBatchTemplateInserter(), 
 				cascade);
 	}
 	
 	/**
 	 * Insert entity.
 	 * 
 	 * @param codingSchemeId the coding scheme id
 	 * @param entity the entity
 	 * @param inserter the inserter
 	 * 
 	 * @return the string
 	 */
 	protected String doInsertEntity(
 			String prefix, 
 			String codingSchemeId, 
 			Entity entity, 
 			IbatisInserter inserter,
 			boolean cascade) {
 		Map<String,String> propertyIdToGuidMap = new HashMap<String,String>();
 		
 		String entityId = this.createUniqueId();
 		String entryStateId = this.createUniqueId();
 		
 		this.ibatisVersionsDao.insertEntryState(entryStateId, 
 				entityId, "Entity", null, entity.getEntryState());
 		
 		inserter.insert(INSERT_ENTITY_SQL, 
 				buildInsertEntityParamaterBean(
 						prefix,
 						prefix,
 						codingSchemeId, entityId, entryStateId, entity));
 
 		for(String entityType : entity.getEntityType()){
 			inserter.insert(INSERT_ENTITY_TYPE_SQL, 
 					new PrefixedParameterTuple(prefix, entityId, entityType));
 		}
 			
 		if(cascade) {
 			for(Property prop : entity.getAllProperties()) {
 				String propertyId = this.createUniqueId();
 				ibatisPropertyDao.doInsertProperty(
 						prefix, 
 						entityId, 
 						propertyId, 
 						PropertyType.ENTITY, 
 						prop, 
 						inserter);
 				propertyIdToGuidMap.put(prop.getPropertyId(), propertyId);
 			}
 		}
 		
 		for(PropertyLink link : entity.getPropertyLink()) {
 			String propertyLinkId = this.createUniqueId();
 			String sourcePropertyId = propertyIdToGuidMap.get(link.getSourceProperty());
 			String targetPropertyId = propertyIdToGuidMap.get(link.getTargetProperty());
 			
 			this.ibatisPropertyDao.doInsertPropertyLink(prefix, entityId, 
 					propertyLinkId, link.getPropertyLink(), 
 					sourcePropertyId, targetPropertyId, inserter);
 		}
 
 		return entityId;
 	}
 	
 	protected String doInsertHistoryEntity( 
 			String codingSchemeUId, 
 			String entityUId,
 			Entity entity, 
 			IbatisInserter inserter,
 			boolean cascade) {
 
 		String historyPrefix = this.getPrefixResolver().resolveHistoryPrefix();
 		String prefix = this.getPrefixResolver().resolvePrefixForCodingScheme(codingSchemeUId);
 
 		InsertOrUpdateEntityBean entityData = (InsertOrUpdateEntityBean) this.getSqlMapClientTemplate()
 				.queryForObject(GET_ENTITY_ATTRIBUTES_BY_UID_SQL,
 						new PrefixedParameter(prefix, entityUId));
 		
 		inserter.insert(INSERT_ENTITY_SQL, 
 				buildInsertEntityParamaterBean(
 						historyPrefix,
 						prefix,
 						codingSchemeUId, entityUId, entityData.getEntryStateUId(), entityData.getEntity()));
 		
 		if (!entryStateExists(prefix, entityData.getEntryStateUId())) {
 
 			EntryState entryState = new EntryState();
 
 			entryState.setChangeType(ChangeType.NEW);
 			entryState.setRelativeOrder(0L);
 
 			ibatisVersionsDao
 					.insertEntryState(entityData.getEntryStateUId(),
 							entityData.getUId(), "Entity", null,
 							entryState);
 		}
 		
 		return entityData.getEntryStateUId();
 	}
 
 	/* (non-Javadoc)
 	 * @see org.lexevs.dao.database.access.entity.EntityDao#insertHistoryEntity(java.lang.String, org.LexGrid.concepts.Entity)
 	 */
 	public String insertHistoryEntity(String codingSchemeId, String entityId, Entity entity) {
 		return this.doInsertHistoryEntity(
 				codingSchemeId, 
 				entityId, 
 				entity, 
 				this.getNonBatchTemplateInserter(),
 				true);
 	}
 	
 	/* (non-Javadoc)
 	 * @see org.lexevs.dao.database.access.entity.EntityDao#getAllEntitiesOfCodingScheme(java.lang.String, int, int)
 	 */
 	@SuppressWarnings("unchecked")
 	public List<? extends Entity> getAllEntitiesOfCodingScheme(String codingSchemeId, int start, int pageSize) {
 		String prefix = this.getPrefixResolver().resolvePrefixForCodingScheme(codingSchemeId);
 		
 		if(pageSize < 0) {
 			pageSize = Integer.MAX_VALUE;
 		}
 		
 		List<Entity> entities = 
 			this.getSqlMapClientTemplate().queryForList(GET_ENTITIES_OF_CODING_SCHEME_SQL, 
 					new PrefixedParameter(prefix, codingSchemeId),
 					start, pageSize);
 		
 		for(Entity entity : entities) {
 			entity.addAnyProperties(
 					this.ibatisPropertyDao.getAllPropertiesOfParent(
 							codingSchemeId, 
 							this.getEntityUId(codingSchemeId, entity.getEntityCode(), entity.getEntityCodeNamespace()), 
 							PropertyType.ENTITY));
 		}
 
 		return entities;
 	}
 	
 	/* (non-Javadoc)
 	 * @see org.lexevs.dao.database.access.entity.EntityDao#insertBatchEntities(java.lang.String, java.util.List)
 	 */
 	public void insertBatchEntities(
 			final String codingSchemeId, 
 			final List<? extends Entity> entities,
 			final boolean cascade) {
 		final String prefix = this.getPrefixResolver().resolvePrefixForCodingScheme(codingSchemeId);
 
 		this.getSqlMapClientTemplate().execute(new SqlMapClientCallback(){
 
 			public Object doInSqlMapClient(SqlMapExecutor executor)
 					throws SQLException {
 				IbatisBatchInserter batchInserter = new SqlMapExecutorBatchInserter(executor);
 				
 				batchInserter.startBatch();
 				
 				for(Entity entity : entities){
 					doInsertEntity(
 							prefix, 
 							codingSchemeId, 
 							entity, 
 							batchInserter,
 							cascade);
 				}
 				
 				batchInserter.executeBatch();
 
 				return null;
 			}
 		});
 	}
 	
 
 	/* (non-Javadoc)
 	 * @see org.lexevs.dao.database.access.entity.EntityDao#getEntityId(java.lang.String, java.lang.String, java.lang.String)
 	 */
 	public String getEntityUId(String codingSchemeId, String entityCode,
 			String entityCodeNamespace) {
 		String prefix = this.getPrefixResolver().resolvePrefixForCodingScheme(codingSchemeId);
 		
 		return (String) this.getSqlMapClientTemplate().queryForObject(
 				GET_ENTITY_ID_BY_CODE_AND_NAMESPACE, 
 					new PrefixedParameterTriple(prefix, codingSchemeId, entityCode, entityCodeNamespace));
 	}
 	
 	/**
 	 * Builds the insert entity paramater bean.
 	 * 
 	 * @param prefix the prefix
 	 * @param codingSchemeId the coding scheme id
 	 * @param entityId the entity id
 	 * @param entryStateId the entry state id
 	 * @param entity the entity
 	 * 
 	 * @return the insert entity bean
 	 */
 	protected InsertOrUpdateEntityBean buildInsertEntityParamaterBean(
 			String prefix, 
 			String entityTypeTablePrefix,
 			String codingSchemeId, String entityId, String entryStateId, Entity entity){
 		InsertOrUpdateEntityBean bean = new InsertOrUpdateEntityBean();
 		bean.setPrefix(prefix);
 		bean.setEntityTypeTablePrefix(entityTypeTablePrefix);
 		bean.setCodingSchemeUId(codingSchemeId);
 		bean.setUId(entityId);
 		bean.setEntryStateUId(entryStateId);
 		bean.setEntity(entity);
 		
 		return bean;
 	}
 	
 	/* (non-Javadoc)
 	 * @see org.lexevs.dao.database.access.AbstractBaseDao#doGetSupportedLgSchemaVersions()
 	 */
 	@Override
 	public List<LexGridSchemaVersion> doGetSupportedLgSchemaVersions() {
 		return DaoUtility.createList(LexGridSchemaVersion.class, supportedDatebaseVersion);
 	}
 
 	/**
 	 * Sets the ibatis versions dao.
 	 * 
 	 * @param ibatisVersionsDao the new ibatis versions dao
 	 */
 	public void setIbatisVersionsDao(IbatisVersionsDao ibatisVersionsDao) {
 		this.ibatisVersionsDao = ibatisVersionsDao;
 	}
 
 	/**
 	 * Gets the ibatis versions dao.
 	 * 
 	 * @return the ibatis versions dao
 	 */
 	public IbatisVersionsDao getIbatisVersionsDao() {
 		return ibatisVersionsDao;
 	}
 	
 	/**
 	 * Gets the ibatis property dao.
 	 * 
 	 * @return the ibatis property dao
 	 */
 	public IbatisPropertyDao getIbatisPropertyDao() {
 		return ibatisPropertyDao;
 	}
 
 	/**
 	 * Sets the ibatis property dao.
 	 * 
 	 * @param ibatisPropertyDao the new ibatis property dao
 	 */
 	public void setIbatisPropertyDao(IbatisPropertyDao ibatisPropertyDao) {
 		this.ibatisPropertyDao = ibatisPropertyDao;
 	}
 
 	public void setIbatisAssociationDao(IbatisAssociationDao ibatisAssociationDao) {
 		this.ibatisAssociationDao = ibatisAssociationDao;
 	}
 
 	public IbatisAssociationDao getIbatisAssociationDao() {
 		return ibatisAssociationDao;
 	}
 
 }
