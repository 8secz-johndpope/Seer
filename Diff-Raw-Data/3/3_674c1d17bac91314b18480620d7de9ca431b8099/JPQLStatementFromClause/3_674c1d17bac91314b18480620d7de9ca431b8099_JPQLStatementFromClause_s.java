 /* SpagoBI, the Open Source Business Intelligence suite
 
  * Copyright (C) 2012 Engineering Ingegneria Informatica S.p.A. - SpagoBI Competency Center
  * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0, without the "Incompatible With Secondary Licenses" notice. 
  * If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/. */
 package it.eng.qbe.statement.jpa;
 
 import it.eng.qbe.model.structure.IModelEntity;
 import it.eng.qbe.query.Query;
 
 import java.util.ArrayList;
 import java.util.HashMap;
 import java.util.Iterator;
 import java.util.List;
 import java.util.Map;
 
 import org.apache.log4j.Logger;
 
 /**
  * @author Andrea Gioia (andrea.gioia@eng.it)
  * 
  */
 public class JPQLStatementFromClause extends AbstractJPQLStatementClause {
 
 	public static final String FROM = "FROM";
 
 	public static transient Logger logger = Logger
 			.getLogger(JPQLStatementFromClause.class);
 
 	public static String build(JPQLStatement parentStatement, Query query,
 			Map<String, Map<String, String>> entityAliasesMaps) {
 		JPQLStatementFromClause clause = new JPQLStatementFromClause(
 				parentStatement);
 		return clause.buildClause(query, entityAliasesMaps);
 	}
 
 	protected JPQLStatementFromClause(JPQLStatement statement) {
 		parentStatement = statement;
 	}
 
 	public String buildClause(Query query, Map entityAliasesMaps) {
 		StringBuffer buffer;
 
 		logger.debug("IN");
 		buffer = new StringBuffer();
 		try {
 			Map entityAliases = (Map) entityAliasesMaps.get(query.getId());
 
 			if (entityAliases == null || entityAliases.keySet().size() == 0) {
 				return "";
 			}
 
 			buffer.append(" " + FROM + " ");
 
 			List<IModelEntity> cubes = new ArrayList<IModelEntity>();
 			List<IModelEntity> normalEntities = new ArrayList<IModelEntity>();
 			Map<String, String> clauses = new HashMap<String, String>();
 
 			Iterator it = entityAliases.keySet().iterator();
 			while (it.hasNext()) {
 				String entityUniqueName = (String) it.next();
 				logger.debug("entity [" + entityUniqueName + "]");
 
 				String entityAlias = (String) entityAliases
 						.get(entityUniqueName);
 				logger.debug("entity alias [" + entityAlias + "]");
 
 				IModelEntity modelEntity = parentStatement.getDataSource()
 						.getModelStructure().getEntity(entityUniqueName);
				if (modelEntity.getProperty("type") == "cube") {
 					cubes.add(modelEntity);
 				} else {
 					normalEntities.add(modelEntity);
 				}
 
 				String fromClauseElement = modelEntity.getName() + " "
 						+ entityAlias;
 				logger.debug("from clause element [" + fromClauseElement + "]");
 
 				clauses.put(entityUniqueName, " " + fromClauseElement);
 
 			}
 
 			Iterator<IModelEntity> cubesIt = cubes.iterator();
 			while (cubesIt.hasNext()) {
 				IModelEntity cube = cubesIt.next();
 				buffer.append(clauses.get(cube.getUniqueName()));
 				if (cubesIt.hasNext()) {
 					buffer.append(",");
 				}
 			}
 
 			if (normalEntities.size() > 0 && cubes.size() > 0)
 				buffer.append(",");
 
 			Iterator<IModelEntity> normalEntitiesIt = normalEntities.iterator();
 			while (normalEntitiesIt.hasNext()) {
 				IModelEntity normalEntity = normalEntitiesIt.next();
 				buffer.append(clauses.get(normalEntity.getUniqueName()));
 				if (normalEntitiesIt.hasNext()) {
 					buffer.append(",");
 				}
 			}
 
 			for (IModelEntity cube : cubes) {
 
 			}
 		} finally {
 			logger.debug("OUT");
 		}
 
 		return buffer.toString().trim();
 	}
 
 }
