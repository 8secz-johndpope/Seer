 /*
  * Autopsy Forensic Browser
  *
  * Copyright 2011 Basis Technology Corp.
  * Contact: carrier <at> sleuthkit <dot> org
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
 package org.sleuthkit.autopsy.keywordsearch;
 
 import java.sql.ResultSet;
 import java.sql.SQLException;
 import java.util.ArrayList;
 import java.util.Collection;
 import java.util.List;
 import java.util.logging.Level;
 import java.util.logging.Logger;
 import org.apache.solr.client.solrj.SolrQuery;
 import org.apache.solr.client.solrj.SolrRequest.METHOD;
 import org.apache.solr.client.solrj.SolrServerException;
 import org.apache.solr.client.solrj.request.QueryRequest;
 import org.apache.solr.client.solrj.response.QueryResponse;
 import org.apache.solr.client.solrj.response.TermsResponse.Term;
 import org.apache.solr.common.SolrDocument;
 import org.apache.solr.common.SolrDocumentList;
 import org.openide.nodes.Node;
 import org.openide.windows.TopComponent;
 import org.sleuthkit.autopsy.casemodule.Case;
 import org.sleuthkit.autopsy.corecomponents.DataResultTopComponent;
 import org.sleuthkit.autopsy.corecomponents.TableFilterNode;
 import org.sleuthkit.datamodel.FsContent;
 import org.sleuthkit.datamodel.SleuthkitCase;
 
 public class LuceneQuery implements KeywordSearchQuery {
 
     private static final Logger logger = Logger.getLogger(LuceneQuery.class.getName());
     private String query; //original unescaped query
     private String queryEscaped;
     private boolean isEscaped;
 
     public LuceneQuery(String query) {
         this.query = query;
         this.queryEscaped = query;
         isEscaped = false;
     }
 
     @Override
     public void escape() {
        queryEscaped = KeywordSearchUtil.escapeLuceneQuery(query, true, false);
         isEscaped = true;
     }
 
     @Override
     public String getEscapedQueryString() {
         return this.queryEscaped;
     }
 
     @Override
     public String getQueryString() {
         return this.query;
     }
     
     @Override
     public Collection<Term>getTerms() {
         return null;
     }
 
     /**
      * Just perform the query and return result without updating the GUI
      * This utility is used in this class, can be potentially reused by other classes
      * @param query
      * @return matches List
      */
     @Override
     public List<FsContent> performQuery() throws RuntimeException {
         List<FsContent> matches = new ArrayList<FsContent>();
 
         boolean allMatchesFetched = false;
         final int ROWS_PER_FETCH = 10000;
 
         Server.Core solrCore = KeywordSearch.getServer().getCore();
 
         SolrQuery q = new SolrQuery();
 
         q.setQuery(queryEscaped);
         q.setRows(ROWS_PER_FETCH);
         q.setFields("id");
 
         for (int start = 0; !allMatchesFetched; start = start + ROWS_PER_FETCH) {
 
             q.setStart(start);
 
             try {
                 QueryResponse response = solrCore.query(q, METHOD.POST);
                 SolrDocumentList resultList = response.getResults();
                 long results = resultList.getNumFound();
 
                 allMatchesFetched = start + ROWS_PER_FETCH >= results;
 
                 for (SolrDocument resultDoc : resultList) {
                     long id = Long.parseLong((String) resultDoc.getFieldValue("id"));
 
                     SleuthkitCase sc = Case.getCurrentCase().getSleuthkitCase();
 
                     // TODO: has to be a better way to get files. Also, need to 
                     // check that we actually get 1 hit for each id
                     ResultSet rs = sc.runQuery("select * from tsk_files where obj_id=" + id);
                     matches.addAll(sc.resultSetToFsContents(rs));
                     rs.close();
                 }
 
             } catch (SolrServerException ex) {
                 logger.log(Level.WARNING, "Error executing Lucene Solr Query: " + query.substring(0,Math.min(query.length()-1, 200)), ex);
                 throw new RuntimeException(ex);
                 // TODO: handle bad query strings, among other issues
             } catch (SQLException ex) {
                 logger.log(Level.WARNING, "Error interpreting results from Lucene Solr Query: " + query, ex);
             }
 
         }
         return matches;
     }
 
     @Override
     public void execute() {
         escape();
         List<FsContent> matches = performQuery();
 
         String pathText = "Keyword query: " + query;
         
         Node rootNode = new KeywordSearchNode(matches, query);
         Node filteredRootNode = new TableFilterNode(rootNode, true);
         
         TopComponent searchResultWin = DataResultTopComponent.createInstance("Keyword search", pathText, filteredRootNode, matches.size());
         searchResultWin.requestActive(); // make it the active top component
     }
 
     @Override
     public boolean validate() {
         return query != null && ! query.equals("");
     }
 }
