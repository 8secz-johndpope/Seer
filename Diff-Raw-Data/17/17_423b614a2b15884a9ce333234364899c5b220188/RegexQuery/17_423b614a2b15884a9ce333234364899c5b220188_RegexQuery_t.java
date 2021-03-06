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
 
 import java.util.ArrayList;
 import java.util.Collection;
 import java.util.Comparator;
 import java.util.Iterator;
 import java.util.LinkedHashMap;
 import java.util.List;
 import java.util.Map;
 import java.util.Set;
 import java.util.TreeSet;
 import java.util.concurrent.ExecutionException;
 import java.util.logging.Level;
 import java.util.logging.Logger;
 import java.util.regex.Matcher;
 import java.util.regex.Pattern;
 import java.util.regex.PatternSyntaxException;
 import javax.swing.SwingWorker;
 import org.apache.solr.client.solrj.SolrQuery;
 import org.apache.solr.client.solrj.SolrServerException;
 import org.apache.solr.client.solrj.response.TermsResponse;
 import org.apache.solr.client.solrj.response.TermsResponse.Term;
 import org.netbeans.api.progress.ProgressHandle;
 import org.netbeans.api.progress.ProgressHandleFactory;
 import org.openide.nodes.AbstractNode;
 import org.openide.nodes.ChildFactory;
 import org.openide.nodes.Children;
 import org.openide.nodes.Node;
 import org.openide.windows.TopComponent;
 import org.sleuthkit.autopsy.corecomponents.DataResultTopComponent;
 import org.sleuthkit.autopsy.datamodel.KeyValueNode;
 import org.sleuthkit.autopsy.datamodel.KeyValueThing;
 import org.sleuthkit.datamodel.Content;
 import org.sleuthkit.datamodel.FsContent;
 
 public class RegexQuery implements KeywordSearchQuery {
 
     private static final int TERMS_UNLIMITED = -1;
     //corresponds to field in Solr schema, analyzed with white-space tokenizer only
     private static final String TERMS_SEARCH_FIELD = "content_ws";
     private static final String TERMS_HANDLER = "/terms";
     private static final int TERMS_TIMEOUT = 90 * 1000; //in ms
     private String regexQuery;
     private static Logger logger = Logger.getLogger(RegexQuery.class.getName());
 
     public RegexQuery(String query) {
         this.regexQuery = query;
     }
 
     @Override
     public boolean validate() {
         boolean valid = true;
         try {
             Pattern.compile(regexQuery);
         } catch (PatternSyntaxException ex1) {
             valid = false;
         } catch (IllegalArgumentException ex2) {
             valid = false;
         }
         return valid;
     }
 
     @Override
     public void execute() {
 
         final SolrQuery q = new SolrQuery();
         q.setQueryType(TERMS_HANDLER);
         q.setTerms(true);
         q.setTermsLimit(TERMS_UNLIMITED);
         q.setTermsRegexFlag("case_insensitive");
         //q.setTermsLimit(200);
         //q.setTermsRegexFlag(regexFlag);
         //q.setTermsRaw(true);
         q.setTermsRegex(regexQuery);
         q.addTermsField(TERMS_SEARCH_FIELD);
         q.setTimeAllowed(TERMS_TIMEOUT);
 
         logger.log(Level.INFO, "Executing TermsComponent query: " + q.toString());
 
         final SwingWorker worker = new RegexQueryWorker(q);
         worker.execute();
     }
 
     /**
      * map Terms to generic Nodes with key/value pairs properties
      * @param terms 
      */
     private void publishNodes(List<Term> terms) {
 
         Collection<KeyValueThing> things = new ArrayList<KeyValueThing>();
 
         Iterator<Term> it = terms.iterator();
         int termID = 0;
         long totalMatches = 0;
         while (it.hasNext()) {
             Term term = it.next();
             Map<String, Object> kvs = new LinkedHashMap<String, Object>();
             long matches = term.getFrequency();
             kvs.put("#exact matches", matches);
             things.add(new KeyValueThing(term.getTerm(), kvs, ++termID));
             totalMatches += matches;
         }
 
         Node rootNode = null;
         if (things.size() > 0) {
             Children childThingNodes =
                     Children.create(new RegexResultChildFactory(things), true);
 
             rootNode = new AbstractNode(childThingNodes);
         } else {
             rootNode = Node.EMPTY;
         }
 
         String pathText = "RegEx query: " + regexQuery
         + "    Files with exact matches: " + Long.toString(totalMatches) + " (also listing approximate matches)";
 
         TopComponent searchResultWin = DataResultTopComponent.createInstance("Keyword search", pathText, rootNode, things.size());
         searchResultWin.requestActive(); // make it the active top component
 
     }
 
     /**
      * factory produces top level result nodes showing *exact* regex match result
      */
     class RegexResultChildFactory extends ChildFactory<KeyValueThing> {
 
         Collection<KeyValueThing> things;
 
         RegexResultChildFactory(Collection<KeyValueThing> things) {
             this.things = things;
         }
 
         @Override
         protected boolean createKeys(List<KeyValueThing> toPopulate) {
             return toPopulate.addAll(things);
         }
 
         @Override
         protected Node createNodeForKey(KeyValueThing thing) {
             //return new KeyValueNode(thing, Children.LEAF);
             return new KeyValueNode(thing, Children.create(new RegexResultDetailsChildFactory(thing), true));
         }
 
         /**
          * factory produces 2nd level child nodes showing files with *approximate* matches
          * since they rely on underlying Lucene query to get details
          * To implement exact regex match detail view, we need to extract files content
          * returned by Lucene and further narrow down by applying a Java regex
          */
         class RegexResultDetailsChildFactory extends ChildFactory<KeyValueThing> {
 
             private KeyValueThing thing;
 
             RegexResultDetailsChildFactory(KeyValueThing thing) {
                 this.thing = thing;
             }
 
             @Override
             protected boolean createKeys(List<KeyValueThing> toPopulate) {
                 //use Lucene query to get files with regular expression match result
                 final String keywordQuery = thing.getName();
                 LuceneQuery filesQuery = new LuceneQuery(keywordQuery);
                 List<FsContent> matches = filesQuery.doQuery();
 
                 //get unique match result files
                 Set<FsContent> uniqueMatches = new TreeSet<FsContent>(new Comparator<FsContent>() {
 
                     @Override
                     public int compare(FsContent fsc1, FsContent fsc2) {
                         return (int) (fsc1.getId() - fsc2.getId());
 
                     }
                 });
                 uniqueMatches.addAll(matches);
 
                 int resID = 0;
                 for (FsContent f : uniqueMatches) {
                     Map<String, Object> resMap = new LinkedHashMap<String, Object>();
                     //final String name = f.getName();
                     final long id = f.getId();
 
                     //build dir name
                     String dirName = KeywordSearchUtil.buildDirName(f);
 
                     resMap.put("dir", dirName);
                     resMap.put("id", Long.toString(id));
                     final String name = dirName + f.getName();
                     resMap.put("name", name);
 
                     toPopulate.add(new KeyValueThingContent(name, resMap, ++resID, f, keywordQuery));
                 }
                 //TODO fix showing of 2nd level child attributes in the GUI (DataResultViewerTable issue?)
 
                 return true;
             }
 
             @Override
             protected Node createNodeForKey(KeyValueThing thing) {
                 final KeyValueThingContent thingContent = (KeyValueThingContent) thing;
                 final Content content = thingContent.getContent();
                 final String query = thingContent.getQuery();
 
                 final String contentStr = getSolrContent(content);
 
                 //make sure the file contains a match (this gets rid of large number of false positives)
                 //TODO option in GUI to include approximate matches (faster)
                 boolean matchFound = false;
                 if (contentStr != null) {//if not null, some error getting from Solr, handle it by not filtering out
                     Pattern p = Pattern.compile(regexQuery, Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL);
                     Matcher m = p.matcher(contentStr);
                     matchFound = m.find();
                 }
 
                 if (matchFound) {
                     Node kvNode = new KeyValueNode(thingContent, Children.LEAF);
                     //wrap in KeywordSearchFilterNode for the markup content, might need to override FilterNode for more customization
                     HighlightedMatchesSource highlights = new HighlightedMatchesSource(content, query);
                     return new KeywordSearchFilterNode(highlights, kvNode, query);
                 } else {
                     return null;
                 }
             }
 
             private String getSolrContent(final Content content) {
                 final Server.Core solrCore = KeywordSearch.getServer().getCore();
                 final SolrQuery q = new SolrQuery();
                 q.setQuery("*:*");
                 q.addFilterQuery("id:" + content.getId());
                 q.setFields("content");
                 try {
                     return (String) solrCore.query(q).getResults().get(0).getFieldValue("content");
                 } catch (SolrServerException ex) {
                     logger.log(Level.WARNING, "Error getting content from Solr and validating regex match", ex);
                     return null;
                 }
             }
         }
 
         /*
          * custom KeyValueThing that also stores retrieved Content and query string used
          */
         class KeyValueThingContent extends KeyValueThing {
 
             private Content content;
             private String query;
 
             Content getContent() {
                 return content;
             }
 
             String getQuery() {
                 return query;
             }
 
             public KeyValueThingContent(String name, Map<String, Object> map, int id, Content content, String query) {
                 super(name, map, id);
                 this.content = content;
                 this.query = query;
             }
         }
     }
 
     class RegexQueryWorker extends SwingWorker<List<Term>, Void> {
 
         private SolrQuery q;
         private ProgressHandle progress;
 
         RegexQueryWorker(SolrQuery q) {
             this.q = q;
         }
 
         @Override
         protected List<Term> doInBackground() throws Exception {
             progress = ProgressHandleFactory.createHandle("RegEx query task");
             progress.start();
             progress.progress("Running RegEx query.");
 
             Server.Core solrCore = KeywordSearch.getServer().getCore();
 
 
             List<Term> terms = null;
             try {
                 TermsResponse tr = solrCore.queryTerms(q);
                 terms = tr.getTerms(TERMS_SEARCH_FIELD);
             } catch (SolrServerException ex) {
                 logger.log(Level.SEVERE, "Error executing the regex terms query: " + regexQuery, ex);
                 return null;  //no need to create result view, just display error dialog
             }
 
             progress.progress("RegEx query completed.");
 
             //debug query
            //StringBuilder sb = new StringBuilder();
            //for (Term t : terms) {
            //    sb.append(t.getTerm() + " : " + t.getFrequency() + "\n");
            //}
             //logger.log(Level.INFO, "TermsComponent query result: " + sb.toString());
             //end debug query
 
             return terms;
         }
 
         @Override
         protected void done() {
             if (!this.isCancelled()) {
                 try {
                     List<Term> terms = get();
                     publishNodes(terms);
                 } catch (InterruptedException e) {
                     logger.log(Level.INFO, "Exception while executing regex query,", e);
 
                 } catch (ExecutionException e) {
                     logger.log(Level.INFO, "Exception while executing regex query,", e);
                 } finally {
                     progress.finish();
                 }
             }
 
 
         }
     }
 }
