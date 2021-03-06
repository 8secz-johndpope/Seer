 /*
  * Copyright 2011 DELVING BV
  *
  * Licensed under the EUPL, Version 1.1 or as soon they
  * will be approved by the European Commission - subsequent
  * versions of the EUPL (the "Licence");
  * you may not use this work except in compliance with the
  * Licence.
  * You may obtain a copy of the Licence at:
  *
  * http://ec.europa.eu/idabc/eupl
  *
  * Unless required by applicable law or agreed to in
  * writing, software distributed under the Licence is
  * distributed on an "AS IS" BASIS,
  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
  * express or implied.
  * See the Licence for the specific language governing
  * permissions and limitations under the Licence.
  */
 
 package eu.europeana.core;
 
 import eu.delving.core.binding.FacetMap;
 import eu.delving.core.binding.SolrBindingService;
 import eu.delving.core.util.PortalTheme;
 import eu.delving.core.util.ThemeInterceptor;
 import eu.delving.metadata.MetadataModel;
 import eu.europeana.core.querymodel.query.*;
 import org.apache.log4j.Logger;
 import org.apache.solr.client.solrj.SolrQuery;
 import org.apache.solr.client.solrj.SolrServerException;
 import org.apache.solr.client.solrj.impl.CommonsHttpSolrServer;
 import org.apache.solr.client.solrj.response.FacetField;
 import org.apache.solr.client.solrj.response.QueryResponse;
 import org.apache.solr.client.solrj.response.SpellCheckResponse;
 import org.apache.solr.common.SolrDocumentList;
 import org.apache.solr.common.SolrException;
 import org.springframework.beans.factory.annotation.Autowired;
 import org.springframework.beans.factory.annotation.Value;
 
 import java.io.UnsupportedEncodingException;
 import java.text.MessageFormat;
 import java.util.HashMap;
 import java.util.List;
 import java.util.Map;
 
 import static eu.delving.core.binding.SolrBindingService.getDocIds;
 
 /**
  * todo: javadoc
  *
  * @author Gerald de Jong <geralddejong@gmail.com>
  * @author Sjoerd Siebinga <sjoerd.siebinga@gmail.com>
  */
 
 @SuppressWarnings({"ValueOfIncrementOrDecrementUsed"})
 public class BeanQueryModelFactory implements QueryModelFactory {
     private Logger log = Logger.getLogger(getClass());
     private CommonsHttpSolrServer solrServer;
 
     @Value("#{launchProperties['portal.name']}")
     private String portalName;
 
     @Autowired
     private DocIdWindowPagerFactory docIdWindowPagerFactory;
 
     @Autowired
     private MetadataModel metadataModel;
 
     public MetadataModel getMetadataModel() {
         return metadataModel;
     }
 
     public void setSolrServer(CommonsHttpSolrServer solrServer) {
         this.solrServer = solrServer;
     }
 
     @Autowired
     private QueryAnalyzer queryAnalyzer;
 
     public QueryAnalyzer getQueryAnalyzer() {
         return queryAnalyzer;
     }
 
     /**
      * create solr query from http query parameters
      */
     @Override
     public SolrQuery createFromQueryParams(Map<String, String[]> params) throws EuropeanaQueryException {
         return SolrQueryUtil.createFromQueryParams(params, queryAnalyzer, ThemeInterceptor.getTheme().getRecordDefinition());
     }
 
     @Override
     public SolrQuery createFromUri(String europeanaUri) throws EuropeanaQueryException {
         if (europeanaUri == null) {
             throw new EuropeanaQueryException(QueryProblem.MALFORMED_URL.toString()); // Expected uri query parameter
         }
         SolrQuery solrQuery = new SolrQuery();
         solrQuery.setQuery("europeana_uri:\"" + europeanaUri + "\"");
         solrQuery.setQueryType(QueryType.MORE_LIKE_THIS_QUERY.toString());
         return solrQuery;
     }
 
     @Override
     public BriefBeanView getBriefResultView(SolrQuery solrQuery, String requestQueryString, Map<String, String[]> params) throws EuropeanaQueryException, UnsupportedEncodingException {
         QueryResponse queryResponse = getSolrResponse(solrQuery, true, params);
         return new BriefBeanViewImpl(solrQuery, queryResponse, requestQueryString);
     }
 
     @Override
     public BriefBeanView getBriefResultView(SolrQuery solrQuery, String requestQueryString) throws EuropeanaQueryException, UnsupportedEncodingException {
         return getBriefResultView(solrQuery, requestQueryString, null);
     }
 
     @Override
     public FullBeanView getFullResultView(Map<String, String[]> params) throws EuropeanaQueryException, SolrServerException {
         String europeanaUri = "";
         if (params.get("uri") != null) {
             europeanaUri = params.get("uri")[0];
         }
         else if (params.get("id") != null) {
             europeanaUri = params.get("id")[0];
         }
         else {
             throw new EuropeanaQueryException(QueryProblem.MALFORMED_URL.toString()); // Expected uri query parameter
         }
         SolrQuery solrQuery = new SolrQuery();
         solrQuery.setQuery("europeana_uri:\"" + europeanaUri + "\"");
         solrQuery.setQueryType(QueryType.MORE_LIKE_THIS_QUERY.toString());
         return new FullBeanViewImpl(solrQuery, getSolrResponse(solrQuery, false, params), params);
     }
 
     // todo remove maybe use FullBeanView.getFullDoc instead
 
     @Override
     public FullDoc getFullDoc(SolrQuery solrQuery) throws EuropeanaQueryException {
         QueryResponse response = getSolrResponseWithHiddenQueryFilters(solrQuery, null);
         return getFullDocFromSolrResponse(response);
     }
 
     @Override
     public List<?> getDocIdList(Map<String, String[]> params) throws EuropeanaQueryException, SolrServerException {
         SolrQuery solrQuery = createFromQueryParams(params);
         Integer start = solrQuery.getStart();
         if (start > 1) {
             solrQuery.setStart(start - 2);
         }
         solrQuery.setRows(3);
         solrQuery.setFields("europeana_uri");
         // Fetch results from server
         final PortalTheme theme = ThemeInterceptor.getTheme();
         if (theme != null) {
                 solrServer.setBaseURL(theme.getSolrSelectUrl());
         }
         QueryResponse queryResponse = solrServer.query(solrQuery);
         // fetch beans
         return getDocIdsFromQueryResponse(queryResponse);
     }
 
     /**
      * Get records from Sorl for a particular collection for the siteMap.
      *
      * @param europeanaCollectionName the europeana collectionName as stored in the EuropeanaCollection Domain object
      * @param rowsReturned            number of rows to be returned from Solr
      * @param pageNumber              which page of the sitemap per collection will be returned.
      * @return list of IdBeans
      * @throws EuropeanaQueryException
      * @throws SolrServerException
      */
     @Override
     public SiteMapBeanView getSiteMapBeanView(String europeanaCollectionName, int rowsReturned, int pageNumber) throws EuropeanaQueryException, SolrServerException {
         SolrQuery solrQuery = new SolrQuery("PROVIDER:\"" + europeanaCollectionName + "\"");
         solrQuery.setRows(rowsReturned);
         solrQuery.setFields("europeana_uri", "timestamp");
         solrQuery.setStart(pageNumber * rowsReturned);
         final PortalTheme theme = ThemeInterceptor.getTheme();
         if (theme != null) {
             solrServer.setBaseURL(theme.getSolrSelectUrl());
         }
         QueryResponse queryResponse = solrServer.query(solrQuery);
         return new SiteMapBeanViewImpl(europeanaCollectionName, queryResponse, rowsReturned);
     }
 
     public class SiteMapBeanViewImpl implements SiteMapBeanView {
         private String europeanaCollectionName;
         private List<? extends DocId> docIds;
         private int numFound;
         private int maxPageForCollection;
 
         public SiteMapBeanViewImpl(String europeanaCollectionName, QueryResponse response, int rowsToBeReturned) {
             this.europeanaCollectionName = europeanaCollectionName;
             this.numFound = (int) response.getResults().getNumFound();
             this.docIds = getDocIdsFromQueryResponse(response);
             this.maxPageForCollection = numFound / rowsToBeReturned + 1;
         }
 
         @Override
         public List<? extends DocId> getIdBeans() {
             return docIds;
         }
 
         @Override
         public int getNumFound() {
             return numFound;
         }
 
         @Override
         public String getCollectionName() {
             return europeanaCollectionName;
         }
 
         @Override
         public int getMaxPageForCollection() {
             return maxPageForCollection;
         }
     }
 
 
     public class BriefBeanViewImpl implements BriefBeanView {
         private ResultPagination pagination;
         private List<? extends BriefDoc> briefDocs;
         private List<FacetQueryLinks> queryLinks;
         private Map<String, String> facetLogs;
         private BriefDoc matchDoc;
         private SpellCheckResponse spellcheck;
 
         @SuppressWarnings("unchecked")
         private BriefBeanViewImpl(SolrQuery solrQuery, QueryResponse solrResponse, String requestQueryString) throws UnsupportedEncodingException, EuropeanaQueryException {
             pagination = createPagination(solrResponse, solrQuery, requestQueryString);
             // todo: convert this list into briefdoc instances
 //            SolrDocumentList list = solrResponse.getResults();
             briefDocs = addIndexToBriefDocList(solrQuery, getBriefDocListFromQueryResponse(solrResponse), solrResponse);
             queryLinks = FacetQueryLinks.createDecoratedFacets(solrQuery, solrResponse.getFacetFields());
             facetLogs = createFacetLogs(solrResponse);
             matchDoc = createMatchDoc(solrResponse);
             spellcheck = solrResponse.getSpellCheckResponse();
         }
 
         private BriefDoc createMatchDoc(QueryResponse solrResponse) {
             BriefDoc briefDoc = null;
             SolrDocumentList matchDoc = (SolrDocumentList) solrResponse.getResponse().get("match");
             if (matchDoc != null) {
                 List<? extends BriefDoc> briefBeanList = getMatchDocFromDocumentList(matchDoc);
                 if (briefBeanList.size() > 0) {
                     briefDoc = briefBeanList.get(0);
                     String europeanaId = createFullDocUrl(briefDoc.getId());
                     briefDoc.setFullDocUrl(europeanaId);
                 }
             }
             return briefDoc;
         }
 
         private Map<String, String> createFacetLogs(QueryResponse solrResponse) {
             Map<String, String> facetLogs = new HashMap<String, String>();
             List<FacetField> facetFieldList = solrResponse.getFacetFields();
             for (FacetField facetField : facetFieldList) {
                 if (facetField.getName().equalsIgnoreCase("LANGUAGE") || facetField.getName().equalsIgnoreCase("COUNTRY")) {
                     StringBuilder out = new StringBuilder();
                     List<FacetField.Count> list = facetField.getValues();
                     if (list == null) {
                         break;
                     }
                     int counter = 0;
                     for (FacetField.Count count : list) {
                         counter++;
                         out.append(count.toString()).append(",");
                         if (counter > 5) {
                             break;
                         }
                     }
                     facetLogs.put(facetField.getName(), out.toString().substring(0, out.toString().length() - 1));
                 }
             }
             return facetLogs;
         }
 
         @Override
         public List<? extends BriefDoc> getBriefDocs() {
             return briefDocs;
         }
 
         @Override
         public List<FacetQueryLinks> getFacetQueryLinks() {
             return queryLinks;
         }
 
         @Override
         public ResultPagination getPagination() {
             return pagination;
         }
 
         @Override
         public Map<String, String> getFacetLogs() {
             return facetLogs;
         }
 
         @Override
         public BriefDoc getMatchDoc() {
             return matchDoc;
         }
 
         @Override
         public SpellCheckResponse getSpellCheck() {
             return spellcheck;
         }
 
         @Override
         public FacetMap getFacetMap() {
             return SolrBindingService.createFacetMap(queryLinks);
         }
     }
 
     private List<? extends BriefDoc> addIndexToBriefDocList(SolrQuery solrQuery, List<? extends BriefDoc> briefDocList, QueryResponse solrResponse) {
         Boolean debug = solrQuery.getBool("debugQuery");
         Map<String, String> explainMap = solrResponse.getExplainMap();
         Integer start = solrQuery.getStart();
         int index = start == null ? 1 : start + 1;
         for (BriefDoc briefDoc : briefDocList) {
             briefDoc.setIndex(index++);
             briefDoc.setFullDocUrl(createFullDocUrl(briefDoc.getId()));
             if (debug != null && debug) {
                 briefDoc.setDebugQuery(explainMap.get(briefDoc.getId()));
             }
         }
         return briefDocList;
     }
 
     private String createFullDocUrl(String europeanaId) {
         return MessageFormat.format("/{0}/object/{1}.html", portalName, europeanaId);
     }
 
     private class FullBeanViewImpl implements FullBeanView {
         private QueryResponse solrResponse;
         private Map<String, String[]> params;
         private FullDoc fullDoc;
         private DocIdWindowPager docIdWindowPager;
         private List<? extends BriefDoc> relatedItems;
 
         private FullBeanViewImpl(SolrQuery solrQuery, QueryResponse solrResponse, Map<String, String[]> params) throws EuropeanaQueryException, SolrServerException {
             this.solrResponse = solrResponse;
             this.params = params;
             fullDoc = createFullDoc();
             relatedItems = addIndexToBriefDocList(solrQuery, getBriefDocListFromQueryResponse(solrResponse), solrResponse);
             docIdWindowPager = createDocIdPager(params);
         }
 
         private DocIdWindowPager createDocIdPager(Map<String, String[]> params) throws SolrServerException, EuropeanaQueryException {
             DocIdWindowPager idWindowPager = null;
             if (params.containsKey("query")) {
                 final PortalTheme theme = ThemeInterceptor.getTheme();
                 if (theme != null) {
                     solrServer.setBaseURL(theme.getSolrSelectUrl());
                 }
                 idWindowPager = docIdWindowPagerFactory.getPager(params, createFromQueryParams(params), ThemeInterceptor.getTheme().getRecordDefinition());
             }
             return idWindowPager;
         }
 
         @Override
         public DocIdWindowPager getDocIdWindowPager() throws Exception {
             return docIdWindowPager;
         }
 
         @Override
         public List<? extends BriefDoc> getRelatedItems() {
             return relatedItems;
         }
 
         @Override
         public FullDoc getFullDoc() throws EuropeanaQueryException {
             return fullDoc;
         }
 
         private FullDoc createFullDoc() throws EuropeanaQueryException {
             SolrDocumentList matchDoc = (SolrDocumentList) solrResponse.getResponse().get("match");
             List<? extends FullDoc> fullBeanItem = getFullDocFromSolrResponse(matchDoc);
 
             // if the record is not found give useful error message
             if (fullBeanItem.size() == 0) {
                 throw new EuropeanaQueryException(QueryProblem.RECORD_NOT_FOUND.toString());
             }
             return fullBeanItem.get(0);
         }
     }
 
     @Override
     public List<? extends DocId> getDocIdsFromQueryResponse(QueryResponse queryResponse) {
         return getDocIds(queryResponse);
     }
 
     @Override
     public List<? extends FullDoc> getFullDocFromSolrResponse(SolrDocumentList matchDoc) {
         return SolrBindingService.getFullDocs(matchDoc);
     }
 
     public FullDoc getFullDocFromOaiPmh(QueryResponse response) throws EuropeanaQueryException {
         return SolrBindingService.getFullDocFromOaiPmh(response);
     }
 
     @Override
     public FullDoc getFullDocFromSolrResponse(QueryResponse response) throws EuropeanaQueryException {
         return SolrBindingService.getFullDoc(response);
     }
 
     @Override
     public List<? extends BriefDoc> getBriefDocListFromQueryResponse(QueryResponse solrResponse) {
         return SolrBindingService.getBriefDocs(solrResponse);
     }
 
     @Override
     public List<? extends BriefDoc> getMatchDocFromDocumentList(SolrDocumentList matchDoc) {
         return SolrBindingService.getBriefDocs(matchDoc);
     }
 
     @Override
     public QueryResponse getSolrResponse(SolrQuery solrQuery) throws EuropeanaQueryException {
         return getSolrResponseFromServer(solrQuery, true);
     }
 
     @Override
     public QueryResponse getSolrResponseWithHiddenQueryFilters(SolrQuery solrQuery, Map<String, String[]> params) throws EuropeanaQueryException {
         SolrQuery dCopy = addHiddenQueryFilters(solrQuery, params);
         return getSolrResponseFromServer(dCopy, true);
     }
 
     private QueryResponse getSolrResponseFromServer(SolrQuery solrQuery, boolean decrementStart) throws EuropeanaQueryException {
         if (solrQuery.getStart() != null && solrQuery.getStart() < 0) {
             solrQuery.setStart(0);
             log.warn("Solr Start cannot be negative");
         }
         // solr query is 0 based
         if (decrementStart && solrQuery.getStart() != null && solrQuery.getStart() > 0) {
             solrQuery.setStart(solrQuery.getStart() - 1);
         }
         QueryResponse queryResponse;
         // todo: add view limitation to query
         try {
             final PortalTheme theme = ThemeInterceptor.getTheme();
             if (theme != null) {
                 solrServer.setBaseURL(theme.getSolrSelectUrl());
             }
             queryResponse = solrServer.query(solrQuery);
         }
         catch (SolrException e) {
             log.error("unable to execute SolrQuery", e);
             throw new EuropeanaQueryException(QueryProblem.MALFORMED_QUERY.toString(), e);
         }
         catch (SolrServerException e) {
             //todo determine which errors the SolrServer can throw
             log.error("Unable to fetch result", e);
             if (e.getMessage().equalsIgnoreCase("Error executing query")) {
                 throw new EuropeanaQueryException(QueryProblem.MALFORMED_QUERY.toString(), e);
             }
             else {
                 throw new EuropeanaQueryException(QueryProblem.SOLR_UNREACHABLE.toString(), e);
             }
         }
         return queryResponse;
     }
 
     @Override
     public QueryResponse getSolrResponse(SolrQuery solrQuery, boolean isBriefDoc, Map<String, String[]> params) throws EuropeanaQueryException { // add bean to ???
         // since we make a defensive copy before the start is decremented we must do it here
         if (solrQuery.getStart() != null && solrQuery.getStart() > 0) {
             solrQuery.setStart(solrQuery.getStart() - 1);
         }
         // set facets
         if (isBriefDoc) {
             // only show spelling-suggestion on the first result page
             if ((solrQuery.getStart() == null || solrQuery.getStart() == 0) && solrQuery.getFilterQueries() == null) {
                 // give spelling suggestions
                 solrQuery.setParam("spellcheck", true);
                 solrQuery.setParam("spellcheck.collate", true);
                 solrQuery.setParam("spellcheck.extendedResults", true);
                 solrQuery.setParam("spellcheck.onlyMorePopular", true);
 //                solrQuery.setParam("spellcheck.count", "4");
             }
             solrQuery.setFacet(true);
             solrQuery.setFacetMinCount(1);
             solrQuery.setFacetLimit(100);
             if (solrQuery.getRows() ==  null) {
                 solrQuery.setRows(12);
             }
             solrQuery.addFacetField(ThemeInterceptor.getTheme().getRecordDefinition().getFacetFieldStrings());
             // todo now hard-coded but these values must be retrieved from the RecordDefinition later
             if (solrQuery.getFields() == null) {
                 solrQuery.setFields("europeana_uri,dc_title,europeana_object,dc_creator,europeana_year,europeana_provider," +
                        "europeana_dataProvider,europeana_language,europeana_type,dc_description");
 //            solrQuery.setFields("*,score");
 //            solrQuery.setFields(metadataModel.getRecordDefinition().getFieldStrings());
             }
             if (solrQuery.getQueryType().equalsIgnoreCase(QueryType.SIMPLE_QUERY.toString())) {
                 solrQuery.setQueryType(queryAnalyzer.findSolrQueryType(solrQuery.getQuery(), ThemeInterceptor.getTheme().getRecordDefinition()).toString());
             }
         }
         SolrQuery dCopy = addHiddenQueryFilters(solrQuery, params);
         return getSolrResponseFromServer(dCopy, false);
     }
 
     SolrQuery addHiddenQueryFilters(SolrQuery solrQuery, Map<String, String[]> params) {
         SolrQuery dCopy = solrQuery.getCopy();
         if (params != null && params.containsKey("hqf")) {
             final String[] hqf_fields = SolrQueryUtil.getFilterQueriesAsPhrases(params.get("hqf"));
             for (String hqf_field : hqf_fields) {
                 dCopy.addFilterQuery(hqf_field);
             }
         }
         final PortalTheme theme = ThemeInterceptor.getTheme();
         if (theme != null && !theme.getHiddenQueryFilters().isEmpty()) {
              final String[] themeHqf = SolrQueryUtil.getFilterQueriesAsPhrases(theme.getHiddenQueryFilters().split(","));
             for (String qf : themeHqf) {
                 dCopy.addFilterQuery(qf);
             }
         }
         dCopy.setFilterQueries(SolrQueryUtil.getFilterQueriesAsOrQueries(dCopy, ThemeInterceptor.getTheme().getRecordDefinition().getFacetMap()));
         return dCopy;
     }
 
      @Override
      public QueryResponse getPagingQueryResponse(SolrQuery solrQuery, Map<String, String[]> params, int solrStartRow) throws EuropeanaQueryException, SolrServerException {
          SolrQuery dCopy = addHiddenQueryFilters(solrQuery, params);
          dCopy.setFields("europeana_uri");
          dCopy.setStart(solrStartRow);
          dCopy.setRows(3);
 //        this.breadcrumbs = Breadcrumb.createList(originalBriefSolrQuery); //todo decide for or queries
          return getSolrResponseFromServer(dCopy, true); // todo should this not be false
     }
 
     private static ResultPagination createPagination(QueryResponse response, SolrQuery query, String requestQueryString) throws EuropeanaQueryException {
         int numFound = (int) response.getResults().getNumFound();
         Boolean debug = query.getBool("debugQuery");
         String parsedQuery = "Information not available";
         if (debug != null && debug) {
             parsedQuery = String.valueOf(response.getDebugMap().get("parsedquery_toString"));
         }
         return new ResultPaginationImpl(query, numFound, requestQueryString, parsedQuery);
     }
 }
