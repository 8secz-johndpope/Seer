 /*******************************************************************************
  * Copyright (c) 2010 Oobium, Inc.
  * All rights reserved. This program and the accompanying materials
  * are made available under the terms of the Eclipse Public License v1.0
  * which accompanies this distribution, and is available at
  * http://www.eclipse.org/legal/epl-v10.html
  * 
  * Contributors:
  *     Jeremy Dowdall <jeremy@oobium.com> - initial API and implementation
  ******************************************************************************/
 package org.oobium.persist.db.internal;
 
 import static org.oobium.utils.coercion.TypeCoercer.coerce;
 import static org.oobium.persist.db.internal.DbCache.*;
 import static org.oobium.persist.db.internal.QueryUtils.*;
 import static org.oobium.utils.SqlUtils.*;
 import static org.oobium.utils.StringUtils.*;
 
 import java.sql.Connection;
 import java.sql.PreparedStatement;
 import java.sql.ResultSet;
 import java.sql.SQLException;
 import java.sql.Statement;
 import java.sql.Types;
 import java.util.ArrayList;
 import java.util.Arrays;
 import java.util.Collection;
 import java.util.HashMap;
 import java.util.Iterator;
 import java.util.LinkedHashMap;
 import java.util.List;
 import java.util.Map;
 import java.util.Map.Entry;
 
 import org.oobium.logging.Logger;
 import org.oobium.persist.Model;
 import org.oobium.persist.ModelAdapter;
 import org.oobium.persist.Relation;
 import org.oobium.persist.RequiredSet;
 import org.oobium.persist.db.DbPersistService;
 import org.oobium.utils.StringUtils;
 
 public class DbPersistor {
 
 	private static final Cell createdAt = new Cell(CREATED_AT, Types.TIMESTAMP, "CURRENT_TIMESTAMP");
 	private static final Cell updatedAt = new Cell(UPDATED_AT, Types.TIMESTAMP, "CURRENT_TIMESTAMP");
 	private static final Cell createdOn = new Cell(CREATED_ON, Types.DATE, "CURRENT_DATE");
 	private static final Cell updatedOn = new Cell(UPDATED_ON, Types.DATE, "CURRENT_DATE");
 
 	private static final Logger logger = Logger.getLogger(DbPersistService.class);
 
 	private static String join(String starter, Object[] segments, String closer, String separator) {
 		Object[] oa = new Object[segments.length];
 		for(int i = 0; i < segments.length; i++) {
 			if(segments[i] instanceof Model) {
 				oa[i] = ((Model) segments[i]).asSimpleString();
 			} else {
 				oa[i] = segments[i];
 			}
 		}
 		return StringUtils.join(starter, oa, closer, separator);
 	}
 	
 
 	public DbPersistor() {
 	}
 
 	public int count(Connection connection, Class<? extends Model> clazz, String sql, Object... values) throws SQLException {
 		if(logger.isLoggingDebug()) {
 			logger.debug("start count: " + clazz.getCanonicalName() + join(", " + sql + " <- [", values, "]", ", "));
 		}
 		
 		StringBuilder sb = new StringBuilder();
 		sb.append("SELECT COUNT(*) FROM ").append(tableName(clazz));
 		if(!blank(sql)) {
 			String where = getWhere(sql);
 			if(!blank(where)) {
 				sb.append(" WHERE ").append(where);
 			}
 		}
 
 		String query = sb.toString();
 		logger.trace(query);
 		
 		Statement s = null;
 		ResultSet rs = null;
 		try {
 			if(values.length == 0) {
 				s = connection.createStatement();
 				rs = s.executeQuery(query);
 			} else {
 				PreparedStatement ps = connection.prepareStatement(query);
 				setStatementValues(ps, values);
 				rs = ps.executeQuery();
 			}
 			if(rs.next()) {
 				return rs.getInt(1);
 			}
 		} finally {
 			logger.debug("end count");
 			if(s != null) {
 				s.close();
 			}
 		}
 
 		return -1;
 	}
 
 	public void create(Connection connection, Model[] models) throws SQLException, NoSuchFieldException {
 		for(Model model : models) {
 			doCreate(connection, model);
 		}
 	}
 
 	private void handleDependentDelete(Connection connection, ModelAdapter adapter, Model model, String field) throws SQLException {
 		throw new UnsupportedOperationException("not yet implemented");
 	}
 	
 	private void handleDependentNullify(Connection connection, ModelAdapter adapter, Model model, String field) throws SQLException {
 		throw new UnsupportedOperationException("not yet implemented");
 	}
 	
 	private void handleDependents(Connection connection, Model model) throws SQLException {
 		ModelAdapter adapter = ModelAdapter.getAdapter(model);
 		for(String field : adapter.getRelations()) {
 			Relation relation = adapter.getRelation(field);
 			switch(relation.dependent()) {
 			case Relation.DELETE:
 				handleDependentDelete(connection, adapter, model, field);
 				break;
 			case Relation.NULLIFY:
 				handleDependentNullify(connection, adapter, model, field);
 				break;
 			}
 		}
 	}
 	
 	public void destroy(Connection connection, Model[] models) throws SQLException {
 		for(Model model : models) {
 			handleDependents(connection, model);
 			doDestroy(connection, model);
 		}
 	}
 
 	private int doCreate(Connection connection, Model model) throws SQLException, NoSuchFieldException {
 		if(logger.isLoggingDebug()) {
 			logger.debug("start doCreate " + model.asSimpleString());
 		}
 
 		if(!model.isNew()) {
 			throw new SQLException("model has already been created");
 		}
 
 		List<Model> models = getModelsToCreate(model);
 		models.add(0, model);
 
 		Map<Model, List<String>> deferredMap = new HashMap<Model, List<String>>();
 		for(Model deferred : models) {
 			deferredMap.putAll(getDeferredToCreate(deferred));
 		}
 
 		for(int i = models.size() - 1; i >= 0; i--) {
 			doCreateModel(connection, models.get(i));
 		}
 
 		for(Entry<Model, List<String>> entry : deferredMap.entrySet()) {
 			doCreateDeferred(connection, entry.getKey(), entry.getValue());
 		}
 
 		logger.debug("end doCreate");
 		return model.getId();
 	}
 
 	private int doCreate(Connection connection, String table, List<Cell> cells) throws SQLException {
 		StringBuilder sb = new StringBuilder();
 		sb.append("INSERT INTO ").append(table).append('(');
 		for(Iterator<Cell> iter = cells.iterator(); iter.hasNext();) {
 			sb.append(safeSqlWord(iter.next().column));
 			if(iter.hasNext()) {
 				sb.append(",");
 			}
 		}
 		sb.append(") VALUES(");
 		for(Iterator<Cell> iter = cells.iterator(); iter.hasNext();) {
 			Cell cell = iter.next();
 			if(cell.isQuery) {
 				sb.append(cell.query());
 				iter.remove();
 			} else if(isDateTimeField(cell) && cell.value instanceof String) {
 				iter.remove();
 				sb.append(cell.value);
 			} else {
 				sb.append("?");
 			}
 			if(iter.hasNext()) {
 				sb.append(",");
 			}
 		}
 		sb.append(')');
 
 		String sql = sb.toString();
 		logger.trace(sql);
 
 		PreparedStatement ps = null;
 		ResultSet rs = null;
 		try {
 			ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
 			for(int i = 0; i < cells.size(); i++) {
 				Cell cell = cells.get(i);
 				if(logger.isLoggingTrace()) {
 					logger.trace("  " + safeSqlWord(cell.column) + " -> " + cell.value);
 				}
 				setObject(ps, i + 1, cell.value, cell.type);
 			}
 			ps.executeUpdate();
 
 			rs = ps.getGeneratedKeys();
 			int id = (rs.next()) ? rs.getInt(1) : -1;
 			if(logger.isLoggingTrace()) {
 				logger.trace("  " + ID + " <- " + id);
 			}
 			return id;
 		} finally {
 			if(ps != null) {
 				ps.close();
 			}
 			if(rs != null) {
 				rs.close();
 			}
 		}
 	}
 	
 	private void doCreateDeferred(Connection connection, Model model, List<String> deferredMany) throws NoSuchFieldException, SQLException {
 		ModelAdapter adapter = ModelAdapter.getAdapter(model.getClass());
 		
 		Class<? extends Model> clazz = model.getClass();
 		for(String field : deferredMany) {
 			if(model.isSet(field)) {
 				Collection<?> collection = coerce(model.get(field), Collection.class);
 				if(!collection.isEmpty()) {
 					if(adapter.isManyToOne(field)) {
 						for(Object object : collection) {
 							// TODO object may not be a model...!
 							Model dModel = (Model) object;
 							if(dModel.isNew()) {
 								doCreate(connection, dModel);
 							}
 						}
 					} else {
 						Class<?> dClazz = adapter.getHasManyMemberClass(field);
 						String dField = adapter.getOpposite(field);
 						String column = columnName(clazz, field);
 						String dColumn = columnName(dClazz, dField);
 						String table = tableName(column, dColumn);
 						List<Integer> dIds = new ArrayList<Integer>();
 						for(Object object : collection) {
 							// TODO object may not be a model...!
 							Model dModel = (Model) object;
 							int dId = dModel.isNew() ? doCreate(connection, dModel) : dModel.getId();
 							dIds.add(dId);
 						}
 						doUpdateManyToMany(connection, table, column, model.getId(), dColumn, dIds);
 					}
 				}
 			}
 		}
 	}
 	
 	private void doCreateModel(Connection connection, Model model) throws SQLException, NoSuchFieldException {
 		if(model.isNew()) {
 			ModelAdapter adapter = ModelAdapter.getAdapter(model.getClass());
 
 			boolean needsCreatedAt, needsCreatedOn, needsUpdatedAt, needsUpdatedOn;
 			needsCreatedAt = needsUpdatedAt = adapter.isTimeStamped();
 			needsCreatedOn = needsUpdatedOn = adapter.isDateStamped();
 			
 			List<Cell> cells = new ArrayList<Cell>();
 			for(String field : adapter.getAttributeFields()) {
 				if(!adapter.isVirtual(field) && model.isSet(field)) {
 					String name = columnName(field);
 					if(needsCreatedAt && name.equals(createdAt.column)) needsCreatedAt = false;
 					if(needsCreatedOn && name.equals(createdOn.column)) needsCreatedOn = false;
 					if(needsUpdatedAt && name.equals(updatedAt.column)) needsUpdatedAt = false;
 					if(needsUpdatedOn && name.equals(updatedOn.column)) needsUpdatedOn = false;
 					int type = getSqlType(adapter.getClass(field));
 					Object val = model.get(field);
 					cells.add(new Cell(name, type, val));
 				}
 			}
 			
 			for(String field : adapter.getHasOneFields()) {
 				Model value = (Model) (model.isSet(field) ? model.get(field) : null);
 				cells.add(new Cell(columnName(field), Types.INTEGER, (value != null) ? value.getId() : null));
 			}
 			
 			if(needsCreatedAt) cells.add(createdAt);
 			if(needsCreatedOn) cells.add(createdOn);
 			if(needsUpdatedAt) cells.add(updatedAt);
 			if(needsUpdatedOn) cells.add(updatedOn);
 			
 			int id = doCreate(connection, tableName(model), cells);
 			model.setId(id);
 
 			if(model.isNew()) {
 				throw new SQLException("could not create record for " + model);
 			}
 
 			setCache(model);
 		}
 	}
 	
 	private void doDestroy(Connection connection, Model model) throws SQLException {
 		if(logger.isLoggingDebug()) {
 			logger.debug("start doDestroy " + model.asSimpleString());
 		}
 
 //		Class<? extends Model> clazz = model.getClass();
 //		ModelAdapter adapter = ModelAdapter.getAdapter(clazz);
 //		for(String field : adapter.getHasManys()) {
 //			if(!adapter.isManyToOne(field) && !adapter.isThrough(field)) {
 //				Class<?> dClazz = adapter.getHasManyClass(field);
 //				String dField = adapter.getOpposite(field);
 //				String column = columnName(clazz, field);
 //				String dColumn = columnName(dClazz, dField);
 //				String table = tableName(column, dColumn);
 //	
 //				String sql = "DELETE FROM " + table + " WHERE " + dColumn + "=" + model.getId();
 //				logger.trace(sql);
 //				
 //				System.out.println(sql);
 //	
 //				Statement s = null;
 //				try {
 //					s = connection.createStatement();
 //					s.executeUpdate(sql);
 //				} finally {
 //					s.close();
 //				}
 //			}
 //		}
 		
 		StringBuilder sb = new StringBuilder();
 		sb.append("DELETE FROM ").append(tableName(model)).append(" WHERE id=").append(model.getId());
 
 		String sql = sb.toString();
 		logger.trace(sql);
 
 		Statement s = null;
 		try {
 			s = connection.createStatement();
 			s.executeUpdate(sql);
 		} finally {
 			s.close();
 		}
 
 		logger.debug("end doDestroy");
 	}
 
 	private Statement doExecuteQuery(Connection connection, String sql, Object...values) throws SQLException {
 		int limit = -1;
 		int ix = sql.toLowerCase().indexOf(" limit ");
 		if(ix != -1) {
 			try {
 				limit = Integer.parseInt(sql.substring(ix + 7).trim());
 				sql = sql.substring(0, ix);
 			} catch(NumberFormatException nfe) {
 				throw new SQLException(nfe.getMessage(), nfe);
 			}
 		}
 
 		if(values.length == 0) {
 			Statement s = connection.createStatement();
 			if(limit > 0) {
 				s.setMaxRows(limit);
 			}
 			s.executeQuery(sql);
 			return s;
 		} else {
 			PreparedStatement ps = connection.prepareStatement(sql);
 			if(limit > 0) {
 				ps.setMaxRows(limit);
 			}
 			setStatementValues(ps, values);
 			ps.executeQuery();
 			return ps;
 		}
 	}
 
 	/**
 	 * @throws SQLException if record does not exist in the database (its relations may have already been saved though)
 	 */
 	private void doUpdate(Connection connection, Model model) throws SQLException, NoSuchFieldException {
 		if(logger.isLoggingDebug()) {
 			logger.debug("start doUpdate: " + model.asSimpleString());
 		}
 
 		int id = model.getId();
 		if(id < 1) {
 			throw new SQLException(ID + " has not yet been set for " + model.asSimpleString());
 		}
 
 		if(!model.isEmpty()) {
 			ModelAdapter adapter = ModelAdapter.getAdapter(model.getClass());
 			
 			List<RequiredSet<?>> removedSets = new ArrayList<RequiredSet<?>>();
 	
 			boolean needsUpdatedAt, needsUpdatedOn;
 			needsUpdatedAt = adapter.isTimeStamped();
 			needsUpdatedOn = adapter.isDateStamped();
 			
 			Class<? extends Model> clazz = model.getClass();
 			Map<String, Object> fields = model.getAll();
 			List<Cell> cells = new ArrayList<Cell>();
 			
 			for(String field : fields.keySet()) {
 				if(!adapter.isVirtual(field)) {
 					if(adapter.hasOne(field)) {
 						Model fModel = (Model) fields.get(field);
 						Integer fId = (fModel != null) ? fModel.getId() : null;
 						if(fId != null && fId < 1) {
 							fId = doCreate(connection, fModel);
 						}
 						cells.add(new Cell(columnName(field), Types.INTEGER, fId));
 					} else if(adapter.hasMany(field)) {
 						Collection<?> collection = coerce(fields.get(field), Collection.class);
 						if(adapter.isManyToOne(field)) {
 							String table = tableName(adapter.getHasManyMemberClass(field));
 							String column = columnName(adapter.getOpposite(field));
 							if(adapter.isOppositeRequired(field)) {
 								List<Integer> dIds = new ArrayList<Integer>();
 								for(Object object : collection) {
 									Model m = (Model) object;
 									int dId = m.getId();
 									if(dId < 1) {
 										dId = doCreate(connection, m);
 									} else {
 										dIds.add(dId);
 									}
 								}
 								List<Integer[]> ids = new ArrayList<Integer[]>();
 								for(Integer i : dIds) {
 									ids.add(new Integer[] { i, id });
 								}
 								if(collection instanceof RequiredSet) { // removed items will be missed...
 									RequiredSet<?> set = (RequiredSet<?>) collection;
 									removedSets.add(set);
 									for(Object object : set.getRemoved()) {
 										Model m = (Model) object;
 										Integer mId = m.getId();
 										Model o = (Model) (m.isSet(field) ? m.get(field) : null);
 										Integer oId = (o != null) ? o.getId() : null;
 										ids.add(new Integer[] { mId, oId });
 									}
 								}
 								doUpdateManyToOne(connection, table, column, ids);
 							} else {
 								doUpdateManyToOne(connection, table, column, id, collection);
 							}
 						} else {
 							Class<?> dClazz = adapter.getHasManyMemberClass(field);
 							String dField = adapter.getOpposite(field);
 							String column = columnName(clazz, field);
 							String dColumn = columnName(dClazz, dField);
 							String table = tableName(column, dColumn);
 							List<Integer> dIds = new ArrayList<Integer>();
 							for(Object object : collection) {
 								Model dModel = (Model) object;
 								int dId = dModel.isNew() ? doCreate(connection, dModel) : dModel.getId();
 								dIds.add(dId);
 							}
 							doUpdateManyToMany(connection, table, column, id, dColumn, dIds);
 						}
 					} else if(adapter.hasAttribute(field)) {
 						String name = columnName(field);
						if(!name.equals(createdAt.column) && !name.equals(createdOn.column)) {
							if(needsUpdatedAt && name.equals(updatedAt.column)) needsUpdatedAt = false;
							if(needsUpdatedOn && name.equals(updatedOn.column)) needsUpdatedOn = false;
							int type = getSqlType(adapter.getClass(field));
							Object val = fields.get(field);
							cells.add(new Cell(name, type, val));
						}
 					}
 				}
 			}
 			
 			if(needsUpdatedAt) cells.add(updatedAt);
 			if(needsUpdatedOn) cells.add(updatedOn);
 
 			if(!removedSets.isEmpty()) {
 				for(RequiredSet<?> set : removedSets) {
 					set.clearRemoved();
 				}
 			}
 			
 			if(!cells.isEmpty()) {
 				int result = doUpdate(connection, tableName(clazz), model.getId(), cells);
 				if(result < 1) {
 					throw new SQLException("could not update " + model.asSimpleString() + " (does not exist in database)");
 				}
 			}
 			
 		}			
 		logger.debug("end doUpdate");
 	}
 
 	private int doUpdate(Connection connection, String table, int id, List<Cell> cells) throws SQLException {
 		StringBuilder sb = new StringBuilder();
 		sb.append("UPDATE ").append(table).append(" SET ");
 		for(Iterator<Cell> iter = cells.iterator(); iter.hasNext();) {
 			Cell cell = iter.next();
			if(isUpdatedDateTimeField(cell) && cell.value instanceof String) {
 				iter.remove();
 				sb.append(cell.column).append('=').append(cell.value);
 			} else {
 				sb.append(safeSqlWord(cell.column)).append("=?");
 			}
 			if(iter.hasNext()) {
 				sb.append(",");
 			}
 		}
 		sb.append(" WHERE id=").append(id);
 
 		String sql = sb.toString();
 		logger.trace(sql);
 
 		PreparedStatement ps = null;
 		try {
 			ps = connection.prepareStatement(sql);
 			for(int i = 0; i < cells.size(); i++) {
 				Cell cell = cells.get(i);
 				setObject(ps, i + 1, cell.value, cell.type);
 				if(logger.isLoggingTrace()) {
 					logger.trace("  " + safeSqlWord(cell.column) + " -> " + cell.value);
 				}
 			}
 			return ps.executeUpdate();
 		} finally {
 			if(ps != null) {
 				ps.close();
 			}
 		}
 	}
 
 	/**
 	 * Update a Many To Many collection
 	 * 
 	 * @param connection
 	 * @param table
 	 * @param column1
 	 * @param id1
 	 * @param column2
 	 * @param id2s
 	 * @throws SQLException
 	 */
 	private void doUpdateManyToMany(Connection connection, String table, String column1, int id1, String column2, List<Integer> id2s) throws SQLException {
 		Statement s = null;
 		try {
 			s = connection.createStatement();
 
 			String sql = "DELETE FROM " + table + " WHERE " + column2 + "=" + id1;
 			logger.trace(sql);
 			s.executeUpdate(sql);
 
 			if(!id2s.isEmpty()) {
 				StringBuilder sb = new StringBuilder();
 				sb.append("INSERT INTO ").append(table).append('(').append(column1).append(',').append(column2).append(") VALUES");
 				for(Iterator<Integer> iter = id2s.iterator(); iter.hasNext();) {
 					sb.append('(').append(iter.next()).append(',').append(id1).append(')');
 					if(iter.hasNext()) {
 						sb.append(',');
 					}
 				}
 
 				sql = sb.toString();
 				logger.trace(sql);
 
 				s.executeUpdate(sql);
 			}
 		} finally {
 			s.close();
 		}
 	}
 
 	/**
 	 * Update a Many to One (opposite is NOT required) collection
 	 */
 	private void doUpdateManyToOne(Connection connection, String table, String column, int id, Collection<?> collection) throws SQLException {
 		Statement s = null;
 		try {
 			s = connection.createStatement();
 
 			String sql = "UPDATE " + table + " SET " + column + "=null WHERE " + column + "=" + id;
 			logger.trace(sql);
 			s.executeUpdate(sql);
 
 			if(!collection.isEmpty()) {
 				StringBuilder sb = new StringBuilder();
 				sb.append("UPDATE ").append(table).append(" SET ").append(column).append('=').append(id).append(" WHERE id IN (");
 				for(Iterator<?> iter = collection.iterator(); iter.hasNext();) {
 					Model model = (Model) iter.next();
 					sb.append(model.getId());
 					if(iter.hasNext()) {
 						sb.append(',');
 					}
 				}
 				sb.append(')');
 
 				sql = sb.toString();
 				logger.trace(sql);
 				s.executeUpdate(sql);
 			}
 		} finally {
 			s.close();
 		}
 	}
 
 	/**
 	 * Update a Many to One (opposite is required) collection
 	 */
 	private void doUpdateManyToOne(Connection connection, String table, String column, List<Integer[]> ids) throws SQLException {
 		if(ids.isEmpty()) {
 			return;
 		}
 
 		PreparedStatement ps = null;
 		try {
 			String sql = "UPDATE " + table + " SET " + column + "=? WHERE id=?";
 			logger.trace(sql);
 			ps = connection.prepareStatement(sql);
 			for(Integer[] ia : ids) {
 				ps.clearParameters();
 				if(ia[1] == null) {
 					ps.setNull(1, Types.INTEGER);
 				} else {
 					ps.setInt(1, ia[1]);
 				}
 				ps.setInt(2, ia[0]);
 				if(logger.isLoggingTrace()) {
 					logger.trace("  " + ia[1] + " <- " + ia[0]);
 				}
 				ps.addBatch();
 			}
 			ps.executeBatch();
 		} finally {
 			if(ps != null) {
 				ps.close();
 			}
 		}
 	}
 
 	public void drop(Connection connection, String schema) throws SQLException {
 		String sql = "select t.tablename, c.constraintname" + " from sys.sysconstraints c, sys.systables t"
 				+ " where c.type = 'F' and t.tableid = c.tableid";
 
 		List<Map<String, Object>> constraints = asFieldMaps(connection, sql);
 		for(Map<String, Object> map : constraints) {
 			sql = "alter table " + map.get("tablename") + " drop constraint " + map.get("constraintname");
 			logger.trace(sql);
 			try {
 				connection.createStatement().executeUpdate(sql);
 			} catch(Exception e) {
 				logger.error(e);
 			}
 		}
 
 		ResultSet rs = connection.getMetaData().getTables(null, schema, "%", new String[] { "TABLE" });
 		while(rs.next()) {
 			sql = "drop table " + schema + "." + rs.getString(3);
 			logger.trace(sql);
 			try {
 				connection.createStatement().executeUpdate(sql);
 			} catch(Exception e) {
 				logger.error(e);
 			}
 		}
 	}
 
 	public List<Map<String, Object>> executeQuery(Connection connection, String sql, Object... values) throws SQLException {
 		if(logger.isLoggingDebug()) {
 			logger.debug("start executeQuery: " + sql + join(" [", values, "]", ", "));
 		}
 
 		Statement s = null;
 		try {
 			s = doExecuteQuery(connection, sql, values);
 			return asFieldMaps(s.getResultSet());
 		} finally {
 			logger.debug("end executeQuery");
 			if(s != null) {
 				s.close();
 			}
 		}
 	}
 
 	public List<List<Object>> executeQueryLists(Connection connection, String sql, Object... values) throws SQLException {
 		if(logger.isLoggingDebug()) {
 			logger.debug("start executeQueryLists: " + sql + join(" [", values, "]", ", "));
 		}
 		
 		Statement s = null;
 		try {
 			s = doExecuteQuery(connection, sql, values);
 			return asLists(s.getResultSet(), true);
 		} finally {
 			logger.debug("end executeQueryLists");
 			if(s != null) {
 				s.close();
 			}
 		}
 	}
 
 	public Object executeQueryValue(Connection connection, String sql, Object... values) throws SQLException {
 		if(logger.isLoggingDebug()) {
 			logger.debug("start executeQueryValue: " + sql + join(" [", values, "]", ", "));
 		}
 
 		Statement s = null;
 		try {
 			ResultSet rs;
 			if(values.length == 0) {
 				s = connection.createStatement();
 				s.setMaxRows(1);
 				rs = s.executeQuery(sql);
 			} else {
 				PreparedStatement ps = (PreparedStatement) (s = connection.prepareStatement(sql));
 				ps.setMaxRows(1);
 				setStatementValues(ps, values);
 				rs = ps.executeQuery();
 			}
 			if(rs.next()) {
 				return rs.getObject(1);
 			} else {
 				return null;
 			}
 		} finally {
 			logger.debug("end executeQueryValue");
 			if(s != null) {
 				s.close();
 			}
 		}
 	}
 
 	public int executeUpdate(Connection connection, String sql, Object... values) throws SQLException {
 		PreparedStatement ps = null;
 		ResultSet rs = null;
 		try {
 			if(isInsert(sql)) {
 				if(logger.isLoggingDebug()) {
 					logger.debug("start executeUpdate(insert): " + sql + join(" [", values, "]", ", "));
 				}
 				ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
 				setStatementValues(ps, values);
 				ps.executeUpdate();
 				rs = ps.getGeneratedKeys();
 				return (rs.next()) ? rs.getInt(1) : -1;
 			} else {
 				if(logger.isLoggingDebug()) {
 					logger.debug("start executeUpdate: " + sql + join(" [", values, "]", ", "));
 				}
 				ps = connection.prepareStatement(sql);
 				setStatementValues(ps, values);
 				return ps.executeUpdate();
 			}
 		} finally {
 			logger.debug("end executeUpdate");
 			if(ps != null)
 				ps.close();
 			if(rs != null)
 				rs.close();
 		}
 	}
 	
 	public <T extends Model> T find(Connection connection, Class<T> clazz, int id) throws SQLException {
 		if(logger.isLoggingDebug()) {
 			logger.debug("start find " + clazz.getCanonicalName() + " id: " + id);
 		}
 
 		if(id < 1) {
 			return null;
 		}
 
 		T object = getCache(clazz, id);
 		if(object != null) {
 			return object;
 		}
 
 		T result = find(connection, clazz, "where id=" + id);
 
 		logger.debug("end find");
 		return result;
 	}
 
 	public <T extends Model> T find(Connection connection, Class<T> clazz, String sql, Object... values) throws SQLException {
 		if(logger.isLoggingDebug()) {
 			logger.debug("start find: " + clazz.getCanonicalName() + ", " + sql + join(" <- [", values, "]", ", "));
 		}
 
 		QueryProcessor<T> processor = QueryProcessor.create(clazz, limit(sql, 1), values);
 		List<T> list = processor.process(connection);
 
 		T result = list.isEmpty() ? null : list.get(0);
 
 		logger.debug("end find");
 		return result;
 	}
 
 	public <T extends Model> List<T> findAll(Connection connection, Class<T> clazz, String sql, Object...values) throws SQLException {
 		if(logger.isLoggingDebug()) {
 			logger.debug("start findAll: " + clazz.getCanonicalName() + ", " + sql + join(" <- [", values, "]", ", "));
 		}
 
 		QueryProcessor<T> processor = QueryProcessor.create(clazz, sql, values);
 		List<T> list = processor.process(connection);
 
 		logger.debug("end findAll");
 		return list;
 	}
 
 	private Map<Model, List<String>> getDeferredToCreate(Model model) throws NoSuchFieldException {
 		ModelAdapter adapter = ModelAdapter.getAdapter(model.getClass());
 
 		Map<Model, List<String>> deferred = new HashMap<Model, List<String>>();
 
 		for(String field : adapter.getHasManyFields()) {
 			if(model.isSet(field)) {
 				if(!deferred.containsKey(model)) {
 					deferred.put(model, new ArrayList<String>());
 				}
 				deferred.get(model).add(field);
 			}
 		}
 
 		return deferred;
 	}
 
 	private List<Model> getModelsToCreate(Model model) throws NoSuchFieldException {
 		ModelAdapter adapter = ModelAdapter.getAdapter(model.getClass());
 
 		List<Model> models = new ArrayList<Model>();
 
 		for(String field : adapter.getHasOneFields()) {
 			Model one = (Model) (model.isSet(field) ? model.get(field) : null);
 			if(one != null && one.isNew()) {
 				models.add(one);
 				models.addAll(getModelsToCreate(one));
 			}
 		}
 
 		return models;
 	}
 
 	private boolean isCreatedDateTimeField(Cell cell) {
 		return CREATED_AT.equals(cell.column) || CREATED_ON.equals(cell.column);
 	}
 	
 	private boolean isDateTimeField(Cell cell) {
 		return isCreatedDateTimeField(cell) || isUpdatedDateTimeField(cell);
 	}
 
 	private boolean isUpdatedDateTimeField(Cell cell) {
 		return UPDATED_AT.equals(cell.column) ||  UPDATED_ON.equals(cell.column);
 	}
 
 	public void retrieve(Connection connection, Model[] models) throws SQLException {
 		if(logger.isLoggingDebug()) {
 			logger.debug(join("start retrieve: [", models, "]", ", "));
 		}
 		if(models.length == 0) {
 			return;
 		} else if(models.length == 1) {
 			Model cache = getCache(models[0].getClass(), models[0].getId());
 			if(cache != null) {
 				if(logger.isLoggingDebug()) {
 					logger.debug("retrieving data from cache: " + cache.asSimpleString());
 				}
 				setFields(models[0], cache.getAll());
 			} else {
 				QueryProcessor<?> processor = QueryProcessor.create(models[0].getClass(), "where id=?", models[0].getId());
 				List<?> list = processor.process(connection);
 				// TODO throw exception if list is empty?
 				if(!list.isEmpty()) {
 					setFields(models[0], ((Model) list.get(0)).getAll());
 				}
 			}
 		} else {
 			LinkedHashMap<Class<? extends Model>, List<Integer>> map = null;
 			List<Model> list = new ArrayList<Model>(Arrays.asList(models));
 			for(Iterator<Model> iter = list.iterator(); iter.hasNext(); ) {
 				Model model = iter.next();
 				Class<? extends Model> clazz = model.getClass();
 				int id = model.getId();
 				Model cache = getCache(clazz, id);
 				if(cache != null) {
 					if(logger.isLoggingDebug()) {
 						logger.debug("retrieving data from cache: " + cache.asSimpleString());
 					}
 					setFields(model, cache.getAll());
 					iter.remove();
 				} else {
 					if(map == null) {
 						map = new LinkedHashMap<Class<? extends Model>, List<Integer>>();
 					}
 					List<Integer> ids = map.get(clazz);
 					if(ids == null) {
 						map.put(clazz, ids = new ArrayList<Integer>());
 					}
 					ids.add(id);
 				}
 			}
 			if(map != null) {
 				for(Entry<Class<? extends Model>, List<Integer>> entry : map.entrySet()) {
 					Class<? extends Model> clazz = entry.getKey();
 					List<Integer> ids = entry.getValue();
 					
 					if(logger.isLoggingDebug()) {
 						logger.debug("retrieving data from database: " + clazz.getCanonicalName() + StringUtils.join(", id IN (", ids, ")", ", "));
 					}
 					QueryProcessor<?> processor = QueryProcessor.create(clazz, StringUtils.join("id IN (", ids, ")", ","));
 					processor.process(connection); // loads data to cache
 					
 					for(Iterator<Model> iter = list.iterator(); iter.hasNext(); ) {
 						Model model = iter.next();
 						Model cache = getCache(clazz, model.getId());
 						if(cache != null) {
 							setFields(model, cache.getAll());
 							iter.remove();
 						}
 					}
 				}
 			}
 		}
 		logger.debug("end retrieve");
 	}
 
 	private void setStatementValues(PreparedStatement ps, Object[] values) throws SQLException {
 		for(int i = 0; i < values.length; i++) {
 			setObject(ps, i + 1, values[i]);
 		}
 	}
 
 	public void update(Connection connection, Model[] models) throws SQLException, NoSuchFieldException {
 		for(Model model : models) {
 			doUpdate(connection, model);
 		}
 	}
 
 }
