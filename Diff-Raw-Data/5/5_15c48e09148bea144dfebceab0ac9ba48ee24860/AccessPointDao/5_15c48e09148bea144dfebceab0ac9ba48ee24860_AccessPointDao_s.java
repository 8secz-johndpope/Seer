 package org.waterforpeople.mapping.dao;
 
 import static com.google.appengine.api.labs.taskqueue.TaskOptions.Builder.url;
 
 import java.util.ArrayList;
 import java.util.Date;
 import java.util.HashMap;
 import java.util.List;
 import java.util.Map;
 
 import javax.jdo.PersistenceManager;
 
 import org.waterforpeople.mapping.app.web.dto.TaskRequest;
 import org.waterforpeople.mapping.domain.AccessPoint;
 import org.waterforpeople.mapping.domain.AccessPoint.AccessPointType;
 
 import com.beoui.geocell.GeocellManager;
 import com.beoui.geocell.model.GeocellQuery;
 import com.beoui.geocell.model.Point;
 import com.gallatinsystems.framework.dao.BaseDAO;
 import com.gallatinsystems.framework.servlet.PersistenceFilter;
 import com.google.appengine.api.labs.taskqueue.Queue;
 import com.google.appengine.api.labs.taskqueue.QueueFactory;
 
 /**
  * dao for manipulating access points
  * 
  * @author Christopher Fagiani
  * 
  */
 public class AccessPointDao extends BaseDAO<AccessPoint> {
 	private static final int MAX_RESULTS = 40;
 
 	public AccessPointDao() {
 		super(AccessPoint.class);
 	}
 
 	/**
 	 * Lists all access points that are near the point identified by the lat/lon
 	 * parameters in order of increasing distance. if maxDistance is 0, all
 	 * points (up to MAX_RESULTS) are returned otherwise, only those points
 	 * maxDistance meters away or less are returned
 	 */
 	public List<AccessPoint> listNearbyAccessPoints(Double lat, Double lon,
 			String countryCode, double maxDistance, String cursor) {
 		PersistenceManager pm = PersistenceFilter.getManager();
 		if (lat != null && lon != null) {
 			Point loc = new Point(lat, lon);
 			List<Object> params = new ArrayList<Object>();
 			params.add(countryCode);
 			GeocellQuery gq = new GeocellQuery(
 					"countryCode == countryCodeParam",
 					"String countryCodeParam", params);
 			return GeocellManager.proximityFetch(loc, MAX_RESULTS, maxDistance,
 					AccessPoint.class, gq, pm);
 		} else {
 			return listAccessPointByLocation(countryCode, null, null, null,
 					cursor);
 		}
 	}
 
 	/**
 	 * lists all the access points for the country/community/type passed in
 	 * 
 	 * @param country
 	 * @param community
 	 * @param type
 	 * @return
 	 */
 	@SuppressWarnings("unchecked")
 	public List<AccessPoint> listAccessPointByLocation(String country,
 			String community, String type, Date updatedSinceDate,
 			String cursorString) {
 		PersistenceManager pm = PersistenceFilter.getManager();
 		javax.jdo.Query query = pm.newQuery(AccessPoint.class);
 		Map<String, Object> paramMap = null;
 
 		StringBuilder filterString = new StringBuilder();
 		StringBuilder paramString = new StringBuilder();
 		paramMap = new HashMap<String, Object>();
 
 		appendNonNullParam("countryCode", filterString, paramString, "String",
 				country, paramMap);
 		appendNonNullParam("communityCode", filterString, paramString,
 				"String", community, paramMap);
 		appendNonNullParam("pointType", filterString, paramString, "String",
 				type, paramMap);
 		appendNonNullParam("lastUpdateDateTime", filterString, paramString,
 				"Date", updatedSinceDate, paramMap, GTE_OP);
 		if (updatedSinceDate != null) {
 			query.declareImports("import java.util.Date");
 		}
 		query.setOrdering("lastUpdateDateTime desc");
 		query.setFilter(filterString.toString());
 		query.declareParameters(paramString.toString());
 
 		prepareCursor(cursorString, query);
 
 		List<AccessPoint> results = (List<AccessPoint>) query
 				.executeWithMap(paramMap);
 
 		return results;
 	}
 
 	/**
 	 * searches for access points that match all of the non-null params
 	 * 
 	 * @param country
 	 * @param community
 	 * @param constDateFrom
 	 * @param constDateTo
 	 * @param type
 	 * @param tech
 	 * @return
 	 */
 	@SuppressWarnings("unchecked")
 	public List<AccessPoint> searchAccessPoints(String country,
 			String community, Date collDateFrom, Date collDateTo, String type,
 			String tech, Date constructionDateFrom, Date constructionDateTo,
 			String orderByField, String orderByDir, String cursorString) {
 
 		Map<String, Object> paramMap = new HashMap<String, Object>();
 		javax.jdo.Query query = constructQuery(country, community,
 				collDateFrom, collDateTo, type, tech, constructionDateFrom,
 				constructionDateTo, orderByField, orderByDir, paramMap);
 		prepareCursor(cursorString, query);
 		List<AccessPoint> results = (List<AccessPoint>) query
 				.executeWithMap(paramMap);
 
 		return results;
 	}
 
 	public void deleteByQuery(String country, String community,
 			Date collDateFrom, Date collDateTo, String type, String tech,
 			Date constructionDateFrom, Date constructionDateTo) {
 		Map<String, Object> paramMap = new HashMap<String, Object>();
 		javax.jdo.Query query = constructQuery(country, community,
 				collDateFrom, collDateTo, type, tech, constructionDateFrom,
 				constructionDateTo, null, null, paramMap);
 		query.deletePersistentAll(paramMap);
 	}
 
 	private javax.jdo.Query constructQuery(String country, String community,
 			Date collDateFrom, Date collDateTo, String type, String tech,
 			Date constructionDateFrom, Date constructionDateTo,
 			String orderByField, String orderByDir, Map<String, Object> paramMap) {
 		PersistenceManager pm = PersistenceFilter.getManager();
 		javax.jdo.Query query = pm.newQuery(AccessPoint.class);
 		StringBuilder filterString = new StringBuilder();
 		StringBuilder paramString = new StringBuilder();
 
 		appendNonNullParam("countryCode", filterString, paramString, "String",
 				country, paramMap);
 		appendNonNullParam("communityCode", filterString, paramString,
 				"String", community, paramMap);
 		appendNonNullParam("pointType", filterString, paramString, "String",
 				type, paramMap);
 		appendNonNullParam("typeTechnologyString", filterString, paramString,
 				"String", tech, paramMap);
 		appendNonNullParam("collectionDate", filterString, paramString, "Date",
 				collDateFrom, paramMap, GTE_OP);
 		appendNonNullParam("collectionDate", filterString, paramString, "Date",
 				collDateTo, paramMap, LTE_OP);
 		appendNonNullParam("constructionDate", filterString, paramString,
 				"Date", constructionDateFrom, paramMap, GTE_OP);
 		appendNonNullParam("constructionDate", filterString, paramString,
 				"Date", constructionDateTo, paramMap, LTE_OP);
 
 		if (orderByField != null) {
 			String ordering = orderByDir;
 			if (ordering == null) {
 				ordering = "asc";
 			}
 			query.setOrdering(orderByField + " " + ordering);
 		}
 		query.setFilter(filterString.toString());
 		query.declareParameters(paramString.toString());
 
 		if (collDateFrom != null || collDateTo != null
 				|| constructionDateFrom != null || constructionDateTo != null) {
 			query.declareImports("import java.util.Date");
 		}
 		return query;
 	}
 
 	@SuppressWarnings("unchecked")
 	public AccessPoint findAccessPoint(AccessPoint.AccessPointType type,
 			Double lat, Double lon, Date collectionDate) {
 		PersistenceManager pm = PersistenceFilter.getManager();
 		javax.jdo.Query query = pm.newQuery(AccessPoint.class);
 		StringBuilder filterString = new StringBuilder();
 		StringBuilder paramString = new StringBuilder();
 		Map<String, Object> paramMap = null;
 		paramMap = new HashMap<String, Object>();
 
 		appendNonNullParam("pointType", filterString, paramString, "String",
 				type, paramMap);
 		appendNonNullParam("latitude", filterString, paramString, "Double",
 				lat, paramMap);
 		appendNonNullParam("longitude", filterString, paramString, "Double",
 				lon, paramMap);
 		appendNonNullParam("collectionDate", filterString, paramString, "Date",
 				collectionDate, paramMap, EQ_OP);
 		query.declareImports("import java.util.Date");
 
 		query.setFilter(filterString.toString());
 		query.declareParameters(paramString.toString());
 
 		List<AccessPoint> results = (List<AccessPoint>) query
 				.executeWithMap(paramMap);
 		if (results != null && results.size() >= 1)
 			return results.get(0);
 		else
 			return null;
 	}
 
 	/**
 	 * lists all access points by the technology type string
 	 * 
 	 * @param countryCode
 	 * @param technologyType
 	 * @param cursorString
 	 * @return
 	 */
 	@SuppressWarnings("unchecked")
 	public List<AccessPoint> listAccessPointsByTechnology(String countryCode,
 			String technologyType, String cursorString) {
 		PersistenceManager pm = PersistenceFilter.getManager();
 		javax.jdo.Query q = pm.newQuery(AccessPoint.class);
 		q.setFilter("countryCode == countryCodeParam && typeTechnologyString ==  typeTechnologyParam");
 		q.declareParameters("String countryCodeParam, String typeTechnologyParam");
 		prepareCursor(cursorString, q);
 		List<AccessPoint> result = (List<AccessPoint>) q.execute(countryCode,
 				technologyType);
 		return result;
 	}
 
 	/**
 	 * lists all access points in order of decreasing date (either collection or
 	 * construction date depending on the dateColumn passed in)
 	 * 
 	 * @param dateColumn
 	 * @param orderDirection
 	 * @param cursorString
 	 * @return
 	 */
 	@SuppressWarnings("unchecked")
 	public List<AccessPoint> listAccessPointsByDateOrdered(String dateColumn,
 			String orderDirection, String cursorString) {
 		PersistenceManager pm = PersistenceFilter.getManager();
 		javax.jdo.Query q = pm.newQuery(AccessPoint.class);
 		q.setOrdering(dateColumn + " " + orderDirection);
 		prepareCursor(cursorString, q);
 		List<AccessPoint> result = (List<AccessPoint>) q.execute();
 		return result;
 	}
 
 	/**
 	 * lists all access points that contain invalid data
 	 * 
 	 * @param cursorString
 	 * @return
 	 */
 	@SuppressWarnings("unchecked")
 	public List<AccessPoint> listAccessPointsWithErrors(String cursorString) {
 		PersistenceManager pm = PersistenceFilter.getManager();
 		javax.jdo.Query q = pm.newQuery(AccessPoint.class);
 		q.setOrdering("createdDateTime desc");
 		// q.setFilter("latitude == 0.0 || longitude == 0.0 || pointStatus == null");
 		q.setFilter("latitude == 0.0");
 		prepareCursor(cursorString, q);
 		List<AccessPoint> result = (List<AccessPoint>) q.execute();
 		return result;
 	}
 
 	@SuppressWarnings("unchecked")
 	public AccessPoint findAccessPoint(String communityCode,
 			AccessPointType pointType) {
 		PersistenceManager pm = PersistenceFilter.getManager();
 		javax.jdo.Query query = pm.newQuery(AccessPoint.class);
 		Map<String, Object> paramMap = null;
 
 		StringBuilder filterString = new StringBuilder();
 		StringBuilder paramString = new StringBuilder();
 		paramMap = new HashMap<String, Object>();
 
 		appendNonNullParam("communityCode", filterString, paramString,
 				"String", communityCode, paramMap);
 		appendNonNullParam("pointType", filterString, paramString, "String",
 				pointType.toString(), paramMap);
 		query.setOrdering("collectionDate desc");
 		query.setFilter(filterString.toString());
 		query.declareParameters(paramString.toString());
 		List<AccessPoint> results = (List<AccessPoint>) query
 				.executeWithMap(paramMap);
 
 		return results.get(0);
 	}
 
 	/**
 	 * finds a single access point by its sms code. If there is more than one,
 	 * it will return the one with the latest collectionDate
 	 * 
 	 * @param code
 	 * @return
 	 */
 	public AccessPoint findAccessPointBySMSCode(String code) {
 		List<AccessPoint> apList = listByProperty("smsCode", code, "String");
 		AccessPoint latest = null;
 		if (apList != null) {
 			for (AccessPoint point : apList) {
 				if (latest == null) {
 					latest = point;
 				} else {
 					if (latest.getCollectionDate() != null
 							&& point.getCollectionDate() != null) {
 						if (latest.getCollectionDate().before(
 								point.getCollectionDate())) {
 							latest = point;
 						}
 					} else {
 						latest = point;
 					}
 				}
 			}
 		}
 		return latest;
 	}
 
 	
 	public AccessPoint saveButDonotFireAsync(AccessPoint point){
 		return super.save(point);
 	}
 	public AccessPoint save(AccessPoint point) {
 		Queue queue = QueueFactory.getDefaultQueue();
		
 		queue.add(url("/app_worker/task").param("action", TaskRequest.UPDATE_AP_GEO_SUB).param(TaskRequest.ACCESS_POINT_ID_PARAM,new Long(point.getKey().getId()).toString()));
		return super.save(point);
 	}
 }
