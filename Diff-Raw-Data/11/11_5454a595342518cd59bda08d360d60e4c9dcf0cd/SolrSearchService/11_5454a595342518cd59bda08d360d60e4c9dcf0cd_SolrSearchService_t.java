 /**
  * Copyright 2008 The University of North Carolina at Chapel Hill
  *
  * Licensed under the Apache License, Version 2.0 (the "License");
  * you may not use this file except in compliance with the License.
  * You may obtain a copy of the License at
  *
  *         http://www.apache.org/licenses/LICENSE-2.0
  *
  * Unless required by applicable law or agreed to in writing, software
  * distributed under the License is distributed on an "AS IS" BASIS,
  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  * See the License for the specific language governing permissions and
  * limitations under the License.
  */
 package edu.unc.lib.dl.search.solr.service;
 
 import java.util.ArrayList;
 import java.util.Date;
 import java.util.Iterator;
 import java.util.List;
 import java.util.Map;
 import java.util.Map.Entry;
 import java.util.Set;
 
 import org.apache.solr.client.solrj.SolrQuery;
 import org.apache.solr.client.solrj.SolrServer;
 import org.apache.solr.client.solrj.SolrServerException;
 import org.apache.solr.client.solrj.beans.DocumentObjectBinder;
 import org.apache.solr.client.solrj.response.FacetField;
 import org.apache.solr.client.solrj.response.Group;
 import org.apache.solr.client.solrj.response.GroupCommand;
 import org.apache.solr.client.solrj.response.GroupResponse;
 import org.apache.solr.client.solrj.response.QueryResponse;
 import org.apache.solr.common.SolrDocument;
 import org.apache.solr.common.params.GroupParams;
 import org.slf4j.Logger;
 import org.slf4j.LoggerFactory;
 import org.springframework.beans.factory.annotation.Autowired;
 
 import edu.unc.lib.dl.search.solr.model.AbstractHierarchicalFacet;
 import edu.unc.lib.dl.search.solr.model.BriefObjectMetadata;
 import edu.unc.lib.dl.acl.exception.AccessRestrictionException;
 import edu.unc.lib.dl.acl.util.AccessGroupConstants;
 import edu.unc.lib.dl.acl.util.AccessGroupSet;
 import edu.unc.lib.dl.search.solr.model.BriefObjectMetadataBean;
 import edu.unc.lib.dl.search.solr.model.CutoffFacet;
 import edu.unc.lib.dl.search.solr.model.FacetFieldFactory;
 import edu.unc.lib.dl.search.solr.model.FacetFieldObject;
 import edu.unc.lib.dl.search.solr.model.GroupedMetadataBean;
 import edu.unc.lib.dl.search.solr.model.SearchRequest;
 import edu.unc.lib.dl.search.solr.model.SearchResultResponse;
 import edu.unc.lib.dl.search.solr.model.SearchState;
 import edu.unc.lib.dl.search.solr.model.SimpleIdRequest;
 import edu.unc.lib.dl.search.solr.util.DateFormatUtil;
 import edu.unc.lib.dl.search.solr.util.FacetFieldUtil;
 import edu.unc.lib.dl.search.solr.util.SearchFieldKeys;
 import edu.unc.lib.dl.search.solr.util.SearchSettings;
 import edu.unc.lib.dl.search.solr.util.SolrSettings;
 
 /**
  * Performs CDR specific Solr search tasks, using solrj for connecting to the solr instance.
  * 
  * @author bbpennel
  */
 public class SolrSearchService {
 	private static final Logger LOG = LoggerFactory.getLogger(SolrSearchService.class);
 	private SolrServer server;
 	@Autowired
 	protected SolrSettings solrSettings;
 	@Autowired
 	protected SearchSettings searchSettings;
 	@Autowired
 	protected FacetFieldFactory facetFieldFactory;
 	protected FacetFieldUtil facetFieldUtil;
 
 	public SolrSearchService() {
 	}
 
 	/**
 	 * Establish the SolrServer object according to the configuration specified in settings.
 	 */
 	protected void initializeSolrServer() {
 		server = solrSettings.getSolrServer();
 	}
 
 	/**
 	 * Retrieves the Solr tuple representing the object identified by id.
 	 * 
 	 * @param id
 	 *           identifier (uuid) of the object to retrieve.
 	 * @param userAccessGroups
 	 * @return
 	 */
 	public BriefObjectMetadataBean getObjectById(SimpleIdRequest idRequest) {
 		LOG.debug("In getObjectbyID");
 
 		QueryResponse queryResponse = null;
 		SolrQuery solrQuery = new SolrQuery();
 		StringBuilder query = new StringBuilder();
 		query.append(solrSettings.getFieldName(SearchFieldKeys.ID)).append(':')
 				.append(SolrSettings.sanitize(idRequest.getId()));
 		try {
 			// Add access restrictions to query
 			addAccessRestrictions(query, idRequest.getAccessGroups());
 			/*if (idRequest.getAccessTypeFilter() != null) {
 				addAccessRestrictions(query, idRequest.getAccessGroups(), idRequest.getAccessTypeFilter());
 			}*/
 		} catch (AccessRestrictionException e) {
 			// If the user doesn't have any access groups, they don't have access to anything, return null.
 			LOG.error("Error while attempting to add access restrictions to object " + idRequest.getId(), e);
 			return null;
 		}
 
 		// Restrict the result fields if set
 		if (idRequest.getResultFields() != null) {
 			for (String field : idRequest.getResultFields()) {
 				solrQuery.addField(solrSettings.getFieldName(field));
 			}
 		}
 
 		solrQuery.setQuery(query.toString());
 		solrQuery.setRows(1);
 
 		LOG.debug("getObjectById query: " + solrQuery.toString());
 		try {
 			queryResponse = server.query(solrQuery);
 		} catch (SolrServerException e) {
 			LOG.error("Error retrieving Solr object request: " + e);
 			return null;
 		}
 
 		List<BriefObjectMetadataBean> results = queryResponse.getBeans(BriefObjectMetadataBean.class);
 		if (results != null && results.size() > 0) {
 			return results.get(0);
 		}
 		return null;
 	}
 
 	/**
 	 * Retrieves search results as a SearchResultResponse. Will not return the solr query use for the request.
 	 * 
 	 * @param searchRequest
 	 * @return
 	 */
 	public SearchResultResponse getSearchResults(SearchRequest searchRequest) {
 		return getSearchResults(searchRequest, false);
 	}
 
 	/**
 	 * Generates a solr query from the search state specified in searchRequest and executes it, returning a result set of
 	 * BriefObjectMetadataBeans and, optionally, the SolrQuery generated for this request.
 	 * 
 	 * @param searchRequest
 	 * @param returnQuery
 	 * @return
 	 */
 	public SearchResultResponse getSearchResults(SearchRequest searchRequest, boolean returnQuery) {
 		LOG.debug("In getSearchResults: " + searchRequest.getSearchState());
 
 		boolean isRetrieveFacetsRequest = searchRequest.getSearchState().getFacetsToRetrieve() != null
 				&& searchRequest.getSearchState().getFacetsToRetrieve().size() > 0;
 
 		SolrQuery solrQuery = this.generateSearch(searchRequest, isRetrieveFacetsRequest);
 
 		LOG.debug("getSearchResults query: " + solrQuery);
 		try {
 			SearchResultResponse resultResponse = executeSearch(solrQuery, searchRequest.getSearchState(),
 					isRetrieveFacetsRequest, returnQuery);
 
 			// Add in the correct rollup representatives when they are missing.
 			if (searchRequest.getSearchState().isRollup()) {
 				for (BriefObjectMetadata item : resultResponse.getResultList()) {
 					if (!item.getId().equals(item.getRollup())) {
 						BriefObjectMetadataBean representative = this.getObjectById(new SimpleIdRequest(item.getRollup(),
 								searchRequest.getAccessGroups()));
 						GroupedMetadataBean grouped = (GroupedMetadataBean) item;
 						grouped.getItems().add(representative);
 						grouped.setRepresentative(representative);
 					}
 				}
 			}
 			
 			return resultResponse;
 		} catch (SolrServerException e) {
 			LOG.error("Error retrieving Solr search result request", e);
 		}
 		return null;
 	}
 
 	/**
 	 * Adds access restrictions to the provided query string buffer. If there are no access groups in the provided group
 	 * set, then an AccessRestrictionException is thrown as it is invalid for a user to have no permissions. If the user
 	 * is an admin, then do not restrict access
 	 * 
 	 * @param query
 	 *           string buffer containing the query to append access groups to.
 	 * @param accessGroups
 	 *           set of access groups to append to the query
 	 * @return The original query restricted to results available to the provided access groups
 	 * @throws AccessRestrictionException
 	 *            thrown if no groups are provided.
 	 */
 	protected StringBuilder addAccessRestrictions(StringBuilder query, AccessGroupSet accessGroups/*, String accessType*/)
 			throws AccessRestrictionException {
 		if (accessGroups == null || accessGroups.size() == 0) {
 			throw new AccessRestrictionException("No access groups were provided.");
 		}
 		if (!accessGroups.contains(AccessGroupConstants.ADMIN_GROUP)) {
 			String joinedGroups = accessGroups.joinAccessGroups(" OR ", null, true);
 			if (searchSettings.getAllowPatronAccess()) {
 				query.append(" AND ((").append("readGroup:(").append(joinedGroups).append(')')
 					.append(" AND status:Published) OR adminGroup:(").append(joinedGroups).append("))");
 			} else {
 				query.append(" AND adminGroup:(").append(joinedGroups).append(')');
 			}
 		}
 		return query;
 	}
 
 	/**
 	 * Attempts to retrieve the hierarchical facet from the facet field corresponding to fieldKey that matches the value
 	 * of searchValue. Retrieves and populates all tiers leading up to the tier number given in searchValue.
 	 * 
 	 * @param fieldKey
 	 *           Key of the facet field to search for the facet within.
 	 * @param searchValue
 	 *           Value to find a matching facet for, should be formatted <tier>,<value>
 	 * @param accessGroups
 	 * @return
 	 */
 	public FacetFieldObject getHierarchicalFacet(AbstractHierarchicalFacet facet, AccessGroupSet accessGroups) {
 		QueryResponse queryResponse = null;
 		SolrQuery solrQuery = new SolrQuery();
 		StringBuilder query = new StringBuilder();
 		query.append("[* TO *]");
 
 		try {
 			// Add access restrictions to query
 			addAccessRestrictions(query, accessGroups);
 		} catch (AccessRestrictionException e) {
 			// If the user doesn't have any access groups, they don't have access to anything, return null.
 			LOG.error(e.getMessage());
 			return null;
 		}
 		solrQuery.setQuery(query.toString());
 		solrQuery.setRows(0);
 
 		solrQuery.setFacet(true);
 		solrQuery.setFacetMinCount(1);
 
 		String solrFieldName = solrSettings.getFieldName(facet.getFieldName());
 
 		solrQuery.addFacetField(solrFieldName);
 		solrQuery.setFacetPrefix(solrFieldName, facet.getSearchValue());
 
 		LOG.debug("getHierarchicalFacet query: " + solrQuery.toString());
 		try {
 			queryResponse = server.query(solrQuery);
 		} catch (SolrServerException e) {
 			LOG.error("Error retrieving Solr object request: " + e);
 			return null;
 		}
 		FacetField facetField = queryResponse.getFacetField(solrFieldName);
 		if (facetField.getValueCount() == 0)
 			return null;
 		return facetFieldFactory.createFacetFieldObject(facet.getFieldName(), queryResponse.getFacetField(solrFieldName));
 	}
 
 	/**
 	 * Checks if an item is accessible given the specified access restrictions
 	 * 
 	 * @param idRequest
 	 * @param accessType
 	 * @return
 	 */
 	public boolean isAccessible(SimpleIdRequest idRequest, String accessType) {
 		QueryResponse queryResponse = null;
 		SolrQuery solrQuery = new SolrQuery();
 		StringBuilder query = new StringBuilder();
 		query.append(solrSettings.getFieldName(SearchFieldKeys.ID)).append(':')
 				.append(SolrSettings.sanitize(idRequest.getId()));
 
 		try {
 			// Add access restrictions to query
 			addAccessRestrictions(query, idRequest.getAccessGroups());
 			/*if (accessType != null) {
 				addAccessRestrictions(query, idRequest.getAccessGroups(), accessType);
 			}*/
 		} catch (AccessRestrictionException e) {
 			// If the user doesn't have any access groups, they don't have access to anything, return null.
 			LOG.error(e.getMessage());
 			return false;
 		}
 
 		solrQuery.setQuery(query.toString());
 		solrQuery.setRows(0);
 
 		solrQuery.addField(solrSettings.getFieldName(SearchFieldKeys.ID));
 
 		LOG.debug("getObjectById query: " + solrQuery.toString());
 		try {
 			queryResponse = server.query(solrQuery);
 		} catch (SolrServerException e) {
 			LOG.error("Error retrieving Solr object request: " + e);
 			return false;
 		}
 
 		return queryResponse.getResults().getNumFound() > 0;
 	}
 
 	/**
 	 * Gets the ancestor path facet for the provided pid, given the access groups provided.
 	 * 
 	 * @param pid
 	 * @param accessGroups
 	 * @return
 	 */
 	public CutoffFacet getAncestorPath(String pid, AccessGroupSet accessGroups) {
 		List<String> resultFields = new ArrayList<String>();
 		resultFields.add(SearchFieldKeys.ANCESTOR_PATH);
 
 		SimpleIdRequest idRequest = new SimpleIdRequest(pid, resultFields, accessGroups);
 
 		BriefObjectMetadataBean rootNode = null;
 		try {
 			rootNode = getObjectById(idRequest);
 		} catch (Exception e) {
 			LOG.error("Error while retrieving Solr entry for ", e);
 		}
 		if (rootNode == null)
 			return null;
 		return rootNode.getAncestorPathFacet();
 	}
 
 	public Date getTimestamp(String pid, AccessGroupSet accessGroups) {
 		QueryResponse queryResponse = null;
 		SolrQuery solrQuery = new SolrQuery();
 		StringBuilder query = new StringBuilder();
 		query.append(solrSettings.getFieldName(SearchFieldKeys.ID)).append(':').append(SolrSettings.sanitize(pid));
 		try {
 			// Add access restrictions to query
 			addAccessRestrictions(query, accessGroups);
 		} catch (AccessRestrictionException e) {
 			// If the user doesn't have any access groups, they don't have access to anything, return null.
 			LOG.error("Error while attempting to add access restrictions to object " + pid, e);
 			return null;
 		}
 
 		solrQuery.addField(solrSettings.getFieldName(SearchFieldKeys.TIMESTAMP));
 
 		solrQuery.setQuery(query.toString());
 		solrQuery.setRows(1);
 
 		LOG.debug("query: " + solrQuery.toString());
 		try {
 			queryResponse = server.query(solrQuery);
 		} catch (SolrServerException e) {
 			LOG.error("Error retrieving Solr object request: " + e);
 			return null;
 		}
 
 		if (queryResponse.getResults().getNumFound() == 0)
 			return null;
 		return (Date) queryResponse.getResults().get(0).getFieldValue("timestamp");
 	}
 
 	/**
 	 * Wrapper method for executing a solr query.
 	 * 
 	 * @param query
 	 * @return
 	 * @throws SolrServerException
 	 */
 	protected QueryResponse executeQuery(SolrQuery query) throws SolrServerException {
 		return server.query(query);
 	}
 
 	/**
 	 * Constructs a SolrQuery object from the search state specified within a SearchRequest object. The request may
 	 * optionally request to retrieve facet results in addition to search results.
 	 * 
 	 * @param searchRequest
 	 * @param isRetrieveFacetsRequest
 	 * @return
 	 */
 	protected SolrQuery generateSearch(SearchRequest searchRequest, boolean isRetrieveFacetsRequest) {
 		SearchState searchState = searchRequest.getSearchState();
 		SolrQuery solrQuery = new SolrQuery();
 		StringBuilder query = new StringBuilder();
 		// Generate search term query string
 		String searchType = null;
 		Map<String, String> searchFields = searchState.getSearchFields();
 		if (searchFields != null) {
 			boolean firstTerm = true;
 			Iterator<String> searchTypeIt = searchFields.keySet().iterator();
 			while (searchTypeIt.hasNext()) {
 				searchType = searchTypeIt.next();
 				List<String> searchFragments = searchState.getSearchTermFragments(searchType);
 				if (searchFragments != null) {
 					LOG.debug(searchType + ": " + searchFragments);
 					for (String searchFragment : searchFragments) {
 						searchFragment = SolrSettings.sanitize(searchFragment);
 						if (firstTerm)
 							firstTerm = false;
 						else
 							query.append(' ').append(searchState.getSearchTermOperator()).append(' ');
 						query.append(solrSettings.getFieldName(searchType)).append(':').append(searchFragment).append(' ');
 					}
 				}
 			}
 		}
 
 		// Add range Fields to the query
 		Map<String, SearchState.RangePair> rangeFields = searchState.getRangeFields();
 		if (rangeFields != null) {
 			Iterator<Map.Entry<String, SearchState.RangePair>> rangeTermIt = rangeFields.entrySet().iterator();
 			while (rangeTermIt.hasNext()) {
 				Map.Entry<String, SearchState.RangePair> rangeTerm = rangeTermIt.next();
 				if (rangeTerm != null
 						&& !(rangeTerm.getValue().getLeftHand() == null && rangeTerm.getValue().getRightHand() == null)) {
 					query.append(solrSettings.getFieldName(rangeTerm.getKey())).append(":[");
 
 					if (rangeTerm.getValue().getLeftHand() == null || rangeTerm.getValue().getLeftHand().length() == 0) {
 						query.append('*');
 					} else {
 						if (searchSettings.dateSearchableFields.contains(rangeTerm.getKey())) {
 							try {
 								query.append(DateFormatUtil.getFormattedDate(rangeTerm.getValue().getLeftHand(), true, false));
 							} catch (NumberFormatException e) {
 								query.append('*');
 							}
 						} else {
 							query.append(SolrSettings.sanitize(rangeTerm.getValue().getLeftHand()));
 						}
 					}
 					query.append(" TO ");
 					if (rangeTerm.getValue().getRightHand() == null || rangeTerm.getValue().getRightHand().length() == 0) {
 						query.append('*');
 					} else {
 						if (searchSettings.dateSearchableFields.contains(rangeTerm.getKey())) {
 							try {
 								query.append(DateFormatUtil.getFormattedDate(rangeTerm.getValue().getRightHand(), true, true));
 							} catch (NumberFormatException e) {
 								query.append('*');
 							}
 						} else {
 							query.append(SolrSettings.sanitize(rangeTerm.getValue().getRightHand()));
 						}
 					}
 					query.append("] ");
 				}
 			}
 		}
 
 		// Blank query, make it an everything query
 		if (query.length() == 0) {
 			query.append("*:* ");
 		}
 		
 		try {
 			// Add access restrictions to query
 			addAccessRestrictions(query, searchRequest.getAccessGroups());
 			/*if (searchState.getAccessTypeFilter() != null) {
 				addAccessRestrictions(query, searchRequest.getAccessGroups(), searchState.getAccessTypeFilter());
 			}*/
 		} catch (AccessRestrictionException e) {
 			// If the user doesn't have any access groups, they don't have access to anything, return null.
 			LOG.error(e.getMessage());
 			return null;
 		}
 
 		// Add query
 		solrQuery.setQuery(query.toString());
 
 		if (searchState.getResultFields() != null) {
 			for (String field : searchState.getResultFields()) {
 				solrQuery.addField(solrSettings.getFieldName(field));
 			}
 		}
 
 		if (searchState.isRollup()) {
 			solrQuery.set(GroupParams.GROUP, true);
 			solrQuery.set(GroupParams.GROUP_FIELD, solrSettings.getFieldName(SearchFieldKeys.ROLLUP_ID));
 			solrQuery.set(GroupParams.GROUP_TOTAL_COUNT, true);
 		}
 
 		// Add sort parameters
 		List<SearchSettings.SortField> sortFields = searchSettings.sortTypes.get(searchState.getSortType());
 		if (sortFields != null) {
 			for (int i = 0; i < sortFields.size(); i++) {
 				SearchSettings.SortField sortField = sortFields.get(i);
 				SolrQuery.ORDER sortOrder = SolrQuery.ORDER.valueOf(sortField.getSortOrder());
 				if (searchState.getSortOrder() != null && searchState.getSortOrder().equals(searchSettings.sortReverse)) {
 					sortOrder = sortOrder.reverse();
 				}
 				solrQuery.addSortField(solrSettings.getFieldName(sortField.getFieldName()), sortOrder);
 			}
 		}
 
 		// Set requested resource types
 		String resourceTypeFilter = this.getResourceTypeFilter(searchState.getResourceTypes());
 		if (resourceTypeFilter != null) {
 			solrQuery.addFilterQuery(resourceTypeFilter);
 		}
 
 		// Turn on faceting
 		solrQuery.setFacet(true);
 		solrQuery.setFacetMinCount(1);
 		solrQuery.setFacetLimit(searchState.getBaseFacetLimit());
 
 		// Override the base facet limit if overrides are given.
 		if (searchState.getFacetLimits() != null) {
 			for (Entry<String, Integer> facetLimit : searchState.getFacetLimits().entrySet()) {
 				solrQuery.add("f." + solrSettings.getFieldName(facetLimit.getKey()) + ".facet.limit", facetLimit.getValue()
 						.toString());
 			}
 		}
 
 		if (isRetrieveFacetsRequest) {
 			// Add facet fields
 			for (String facetName : searchState.getFacetsToRetrieve()) {
 				solrQuery.addFacetField(solrSettings.getFieldName(facetName));
 			}
 		}
 
 		// Add facet limits
 		Map<String, Object> facets = searchState.getFacets();
 		if (facets != null) {
 			Iterator<Entry<String, Object>> facetIt = facets.entrySet().iterator();
 			while (facetIt.hasNext()) {
 				Entry<String, Object> facetEntry = facetIt.next();
 
 				if (facetEntry.getValue() instanceof String) {
 					LOG.debug("Adding facet " + facetEntry.getKey() + " as a String");
 					// Add Normal facets
 					solrQuery.addFilterQuery(solrSettings.getFieldName(facetEntry.getKey()) + ":\""
 							+ SolrSettings.sanitize((String) facetEntry.getValue()) + "\"");
 				} else {
 					LOG.debug("Adding facet " + facetEntry.getKey() + " as a " + facetEntry.getValue().getClass().getName());
 					facetFieldUtil.addToSolrQuery(facetEntry.getValue(), solrQuery);
 				}
 			}
 		}
 
 		// Scope hierarchical facet results to the highest tier selected within the facet tree
 		if (isRetrieveFacetsRequest && searchRequest.isApplyFacetPrefixes()) {
 			Set<String> facetsQueried = searchState.getFacets().keySet();
 			// Apply closing cutoff to all cutoff facets that are being retrieved but not being queried for
 			for (String fieldKey : searchRequest.getSearchState().getFacetsToRetrieve()) {
 				if (!facetsQueried.contains(fieldKey)) {
 					facetFieldUtil.addDefaultFacetPivot(fieldKey, solrQuery);
 				}
 			}
 
 			// Add individual facet field sorts if they are present.
 			if (searchState.getFacetSorts() != null) {
 				for (Entry<String, String> facetSort : searchState.getFacetSorts().entrySet()) {
 					solrQuery
 							.add("f." + solrSettings.getFieldName(facetSort.getKey()) + ".facet.sort", facetSort.getValue());
 				}
 			}
 		}
 
 		// Set Navigation options
 		solrQuery.setStart(searchState.getStartRow());
 		if (searchState.getRowsPerPage() != null)
 			solrQuery.setRows(searchState.getRowsPerPage());
 
 		return solrQuery;
 	}
 
 	/**
 	 * Executes a SolrQuery based off of a search state and stores the results as BriefObjectMetadataBeans.
 	 * 
 	 * @param query
 	 *           the solr query to be executed
 	 * @param searchState
 	 *           the search state used to generate this SolrQuery
 	 * @param isRetrieveFacetsRequest
 	 *           indicates if facet results hould be returned
 	 * @param returnQuery
 	 *           indicates whether to return the solr query object as part of the response.
 	 * @return
 	 * @throws SolrServerException
 	 */
 	@SuppressWarnings("unchecked")
 	protected SearchResultResponse executeSearch(SolrQuery query, SearchState searchState,
 			boolean isRetrieveFacetsRequest, boolean returnQuery) throws SolrServerException {
 		QueryResponse queryResponse = server.query(query);
 
 		GroupResponse groupResponse = queryResponse.getGroupResponse();
 		SearchResultResponse response = new SearchResultResponse();
 		if (groupResponse != null) {
 			List<BriefObjectMetadata> groupResults = new ArrayList<BriefObjectMetadata>();
 			for (GroupCommand groupCmd : groupResponse.getValues()) {
 				//response.setResultCount(groupCmd.getMatches());
 				response.setResultCount(groupCmd.getNGroups());
 				for (Group group : groupCmd.getValues()) {
 					GroupedMetadataBean grouped = new GroupedMetadataBean(group.getGroupValue(), this.server.getBinder()
 							.getBeans(BriefObjectMetadataBean.class, group.getResult()), group.getResult().getNumFound());
 					groupResults.add(grouped);
 				}
 			}
 			response.setResultList(groupResults);
 			
 		} else {
 			List<?> results = queryResponse.getBeans(BriefObjectMetadataBean.class);
 			response.setResultList((List<BriefObjectMetadata>) results);
 			// Store the number of results
 			response.setResultCount(queryResponse.getResults().getNumFound());
 		}
 
 		if (isRetrieveFacetsRequest) {
 			// Store facet results
 			response.setFacetFields(facetFieldFactory.createFacetFieldList(queryResponse.getFacetFields()));
 			// Add empty entries for any empty facets, then sort the list
			if (response.getFacetFields() != null) {
				if (searchState.getFacetsToRetrieve() != null
						&& searchState.getFacetsToRetrieve().size() != response.getFacetFields().size()) {
					facetFieldFactory.addMissingFacetFieldObjects(response.getFacetFields(), searchState.getFacetsToRetrieve());
				}
				response.getFacetFields().sort(searchSettings.facetDisplayOrder);
 			}
 		} else {
 			response.setFacetFields(null);
 		}
 		
 		// Set search state that generated this result
 		response.setSearchState(searchState);
 
 		// Add the query to the result if it was requested
 		if (returnQuery) {
 			response.setGeneratedQuery(query);
 		}
 		return response;
 	}
 
 	/**
 	 * Generates a solr query style string to add a resource type filter for the given list of resources types.
 	 * 
 	 * @param resourceTypes
 	 * @return
 	 */
 	protected String getResourceTypeFilter(List<String> resourceTypes) {
 		if (resourceTypes == null || resourceTypes.size() == 0)
 			return null;
 
 		StringBuilder sb = new StringBuilder();
 		boolean firstType = true;
 		String resourceTypeLabel = solrSettings.getFieldName(SearchFieldKeys.RESOURCE_TYPE);
 		Iterator<String> resourceTypeIt = resourceTypes.iterator();
 		while (resourceTypeIt.hasNext()) {
 			if (firstType)
 				firstType = false;
 			else
 				sb.append(" OR ");
 			sb.append(resourceTypeLabel).append(':').append(resourceTypeIt.next()).append(' ');
 		}
 		return sb.toString();
 	}
 	
 	/**
 	 * Returns the value of a single field from the object identified by pid.
 	 * 
 	 * @param pid
 	 * @param field
 	 * @return The value of the specified field or null if it wasn't found.
 	 */
 	public Object getField(String pid, String field) throws SolrServerException {
 		QueryResponse queryResponse = null;
 		SolrQuery solrQuery = new SolrQuery();
 		StringBuilder query = new StringBuilder();
 		query.append("id:").append(SolrSettings.sanitize(pid));
 		solrQuery.setQuery(query.toString());
 		solrQuery.addField(field);
 
 		queryResponse = server.query(solrQuery);
 		if (queryResponse.getResults().getNumFound() > 0) {
 			return queryResponse.getResults().get(0).getFirstValue(field);
 		}
 		return null;
 	}
 
 	public SolrSettings getSolrSettings() {
 		return solrSettings;
 	}
 
 	public void setSolrSettings(SolrSettings solrSettings) {
 		this.solrSettings = solrSettings;
 	}
 
 	public SearchSettings getSearchSettings() {
 		return searchSettings;
 	}
 
 	public void setSearchSettings(SearchSettings searchSettings) {
 		this.searchSettings = searchSettings;
 	}
 
 	public void setFacetFieldFactory(FacetFieldFactory facetFieldFactory) {
 		this.facetFieldFactory = facetFieldFactory;
 	}
 
 	public void setFacetFieldUtil(FacetFieldUtil facetFieldUtil) {
 		this.facetFieldUtil = facetFieldUtil;
 	}
 }
