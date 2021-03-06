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
 
 import java.awt.event.ActionEvent;
 import java.awt.event.ActionListener;
 import java.util.ArrayList;
 import java.util.Collection;
 import java.util.HashMap;
 import java.util.List;
 import java.util.Map;
 import java.util.concurrent.CancellationException;
 import java.util.concurrent.locks.Lock;
 import java.util.concurrent.locks.ReentrantReadWriteLock;
 import java.util.logging.Level;
 import java.util.logging.Logger;
 import javax.swing.SwingUtilities;
 import javax.swing.SwingWorker;
 import javax.swing.Timer;
 import org.apache.commons.lang.StringEscapeUtils;
 import org.apache.solr.client.solrj.SolrServerException;
 import org.netbeans.api.progress.ProgressHandle;
 import org.netbeans.api.progress.ProgressHandleFactory;
 import org.openide.util.Cancellable;
 import org.sleuthkit.autopsy.casemodule.Case;
 import org.sleuthkit.autopsy.ingest.IngestManager;
 import org.sleuthkit.autopsy.ingest.IngestManagerProxy;
 import org.sleuthkit.autopsy.ingest.IngestMessage;
 import org.sleuthkit.autopsy.ingest.IngestMessage.MessageType;
 import org.sleuthkit.autopsy.ingest.IngestServiceAbstractFile;
 import org.sleuthkit.autopsy.ingest.ServiceDataEvent;
 import org.sleuthkit.autopsy.keywordsearch.Ingester.IngesterException;
 import org.sleuthkit.datamodel.BlackboardArtifact;
 import org.sleuthkit.datamodel.BlackboardArtifact.ARTIFACT_TYPE;
 import org.sleuthkit.datamodel.BlackboardAttribute;
 import org.sleuthkit.datamodel.AbstractFile;
 import org.sleuthkit.datamodel.FsContent;
 import org.sleuthkit.datamodel.SleuthkitCase;
 import org.sleuthkit.datamodel.TskData;
 
 /**
  * An ingest service on a file level
  * Performs indexing of allocated and Solr supported files,
  * string extraction and indexing of unallocated and not Solr supported files
  * Index commit is done periodically (determined by user set ingest update interval)
  * Runs a periodic keyword / regular expression search on currently configured lists for ingest
  * and writes results to blackboard
  * Reports interesting events to Inbox and to viewers
  * 
  * Registered as a service in layer.xml
  */
 public final class KeywordSearchIngestService implements IngestServiceAbstractFile {
 
     private static final Logger logger = Logger.getLogger(KeywordSearchIngestService.class.getName());
     public static final String MODULE_NAME = "Keyword Search";
     public static final String MODULE_DESCRIPTION = "Performs file indexing and periodic search using keywords and regular expressions in lists.";
     private static KeywordSearchIngestService instance = null;
     private IngestManagerProxy managerProxy;
     private static final long MAX_INDEX_SIZE = 100 * (1 << 10) * (1 << 10);
     private Ingester ingester = null;
     private volatile boolean commitIndex = false; //whether to commit index next time
     private volatile boolean runSearcher = false; //whether to run searcher next time
     private List<Keyword> keywords; //keywords to search
     private List<String> keywordLists; // lists currently being searched
     private Map<String, KeywordSearchList> keywordToList; //keyword to list name mapping
     private Timer commitTimer;
     private Timer searchTimer;
     //private static final int COMMIT_INTERVAL_MS = 10 * 60 * 1000;
     private Indexer indexer;
     private Searcher currentSearcher;
     private Searcher finalSearcher;
     private volatile boolean searcherDone = true;
     private Map<Keyword, List<ContentHit>> currentResults;
     private static final ReentrantReadWriteLock rwLock = new ReentrantReadWriteLock(true); //use fairness policy
     private static final Lock searcherLock = rwLock.writeLock(); 
     private volatile int messageID = 0;
     private boolean processedFiles;
    private volatile boolean finalSearcherDone = false;
     private final String hashDBServiceName = "Hash Lookup";
     private SleuthkitCase caseHandle = null;
     private boolean skipKnown = true;
     boolean initialized = false;
 
     private enum IngestStatus {
 
         INGESTED, EXTRACTED_INGESTED, SKIPPED,};
     private Map<Long, IngestStatus> ingestStatus;
 
     /**
      * Returns singleton instance of the service, creates one if needed
      * @return instance of the service
      */
     public static synchronized KeywordSearchIngestService getDefault() {
         if (instance == null) {
             instance = new KeywordSearchIngestService();
         }
         return instance;
     }
 
     /**
      * Starts processing of every file provided by IngestManager.  
      * Checks if it is time to commit and run search
      * @param abstractFile file/unallocated file/directory to process
      * @return ProcessResult.OK in most cases and ERROR only if error in the pipeline, otherwise does not advice to stop the pipeline
      */
     @Override
     public ProcessResult process(AbstractFile abstractFile) {
 
         if (initialized == false) //error initializing indexing/Solr
         {
             return ProcessResult.OK;
         }
 
         //check if we should skip this file according to HashDb service
         //if so do not index it, also postpone indexing and keyword search threads to later
         IngestServiceAbstractFile.ProcessResult hashDBResult = managerProxy.getAbstractFileServiceResult(hashDBServiceName);
         //logger.log(Level.INFO, "hashdb result: " + hashDBResult + "file: " + AbstractFile.getName());
         if (hashDBResult == IngestServiceAbstractFile.ProcessResult.COND_STOP && skipKnown) {
             return ProcessResult.OK;
         } else if (hashDBResult == IngestServiceAbstractFile.ProcessResult.ERROR) {
             //notify depending service that keyword search (would) encountered error for this file
             return ProcessResult.ERROR;
         }
 
         if (processedFiles == false) {
             processedFiles = true;
         }
 
         checkRunCommitSearch();
 
         indexer.indexFile(abstractFile);
         return ProcessResult.OK;
 
     }
 
     /**
      * After all files are ingested, execute final index commit and final search
      * Cleanup resources, threads, timers
      */
     @Override
     public void complete() {
         if (initialized == false) {
             return;
         }
 
         //logger.log(Level.INFO, "complete()");
         commitTimer.stop();
         
         //handle case if previous search running
         //cancel it, will re-run after final commit
         //note: cancellation of Searcher worker is graceful (between keywords)        
         if (currentSearcher != null) {
             currentSearcher.cancel(false);
         }    
         
         //cancel searcher timer, ensure unwanted searcher does not start 
         //before we start the final one
         if (searchTimer.isRunning())
             searchTimer.stop();
         runSearcher = false;
 
         logger.log(Level.INFO, "Running final index commit and search");
         //final commit
         commit();
 
         postIndexSummary();
 
         updateKeywords();
         //run one last search as there are probably some new files committed
         if (keywords != null && !keywords.isEmpty() && processedFiles == true) {
             finalSearcher = new Searcher(keywords, true); //final searcher run
             finalSearcher.execute();
         } else {
             finalSearcherDone = true;
             managerProxy.postMessage(IngestMessage.createMessage(++messageID, MessageType.INFO, this, "Completed"));
         }
 
         //postSummary();
     }
 
     /**
      * Handle stop event (ingest interrupted)
      * Cleanup resources, threads, timers
      */
     @Override
     public void stop() {
         logger.log(Level.INFO, "stop()");
 
         //stop timer
         commitTimer.stop();
         //stop currentSearcher
         if (currentSearcher != null) {
             currentSearcher.cancel(true);
         }
         
         //cancel searcher timer, ensure unwanted searcher does not start 
         if (searchTimer.isRunning())
             searchTimer.stop();
         runSearcher = false;
 
         //commit uncommited files, don't search again
         commit();
 
         //postSummary();
     }
 
     @Override
     public String getName() {
         return MODULE_NAME;
     }
 
     @Override
     public String getDescription() {
         return MODULE_DESCRIPTION;
     }
 
     /**
      * Initializes the service for new ingest run
      * Sets up threads, timers, retrieves settings, keyword lists to run on
      * @param managerProxy 
      */
     @Override
     public void init(IngestManagerProxy managerProxy) {
         logger.log(Level.INFO, "init()");
         initialized = false;
 
         caseHandle = Case.getCurrentCase().getSleuthkitCase();
 
         this.managerProxy = managerProxy;
 
         Server solrServer = KeywordSearch.getServer();
 
         ingester = solrServer.getIngester();
 
         ingestStatus = new HashMap<Long, IngestStatus>();
 
         keywords = new ArrayList<Keyword>();
         keywordLists = new ArrayList<String>();
         keywordToList = new HashMap<String, KeywordSearchList>();
 
         initKeywords();
 
         if (keywords.isEmpty() || keywordLists.isEmpty()) {
             managerProxy.postMessage(IngestMessage.createWarningMessage(++messageID, instance, "No keywords in keyword list.", "Only indexing will be done and and keyword search will be skipped (it can be executed later again as ingest or using toolbar search feature)."));
         }
 
         processedFiles = false;
         finalSearcherDone = false;
         searcherDone = true; //make sure to start the initial currentSearcher
         //keeps track of all results per run not to repeat reporting the same hits
         currentResults = new HashMap<Keyword, List<ContentHit>>();
 
         indexer = new Indexer();
 
         final int updateIntervalMs = managerProxy.getUpdateFrequency() * 60 * 1000;
         logger.log(Level.INFO, "Using commit interval (ms): " + updateIntervalMs);
         logger.log(Level.INFO, "Using searcher interval (ms): " + updateIntervalMs);
 
         commitTimer = new Timer(updateIntervalMs, new CommitTimerAction());
         searchTimer = new Timer(updateIntervalMs, new SearchTimerAction());
 
         initialized = true;
 
         commitTimer.start();
         searchTimer.start();
 
         managerProxy.postMessage(IngestMessage.createMessage(++messageID, MessageType.INFO, this, "Started"));
     }
 
     @Override
     public ServiceType getType() {
         return ServiceType.AbstractFile;
     }
 
     @Override
     public boolean hasSimpleConfiguration() {
         return true;
     }
 
     @Override
     public boolean hasAdvancedConfiguration() {
         return true;
     }
 
     @Override
     public javax.swing.JPanel getSimpleConfiguration() {
         return new KeywordSearchIngestSimplePanel();
     }
 
     @Override
     public javax.swing.JPanel getAdvancedConfiguration() {
         return KeywordSearchConfigurationPanel.getDefault();
     }
 
     @Override
     public void saveAdvancedConfiguration() {
     }
 
     @Override
     public void saveSimpleConfiguration() {
     }
 
     /**
      * The services maintains background threads, return true if background threads are running
      * or there are pending tasks to be run in the future, such as the final search post-ingest completion
      * @return 
      */
     @Override
     public boolean hasBackgroundJobsRunning() {
         if ( (currentSearcher != null && searcherDone == false) 
             || (finalSearcherDone == false)) {
             return true;
         } else {
             return false;
         }
 
     }
 
     /**
      * Commits index and notifies listeners of index update
      */
     private void commit() {
         if (initialized) {
             logger.log(Level.INFO, "Commiting index");
             ingester.commit();
             logger.log(Level.INFO, "Index comitted");
             //signal a potential change in number of indexed files
             indexChangeNotify();
         }
     }
 
     /**
      * Posts inbox message with summary of indexed files
      */
     private void postIndexSummary() {
         int indexed = 0;
         int indexed_extr = 0;
         int skipped = 0;
         for (IngestStatus s : ingestStatus.values()) {
             switch (s) {
                 case INGESTED:
                     ++indexed;
                     break;
                 case EXTRACTED_INGESTED:
                     ++indexed_extr;
                     break;
                 case SKIPPED:
                     ++skipped;
                     break;
                 default:
                     ;
             }
         }
 
         StringBuilder msg = new StringBuilder();
         msg.append("Indexed files: ").append(indexed).append("<br />Indexed strings: ").append(indexed_extr);
         msg.append("<br />Skipped files: ").append(skipped).append("<br />");
         String indexStats = msg.toString();
         logger.log(Level.INFO, "Keyword Indexing Completed: " + indexStats);
         managerProxy.postMessage(IngestMessage.createMessage(++messageID, MessageType.INFO, this, "Keyword Indexing Completed", indexStats));
 
     }
 
     /**
      * Helper method to notify listeners on index update
      */
     private void indexChangeNotify() {
         //signal a potential change in number of indexed files
         try {
             final int numIndexedFiles = KeywordSearch.getServer().queryNumIndexedFiles();
             KeywordSearch.changeSupport.firePropertyChange(KeywordSearch.NUM_FILES_CHANGE_EVT, null, new Integer(numIndexedFiles));
         } catch (NoOpenCoreException ex) {
             logger.log(Level.WARNING, "Error executing Solr query to check number of indexed files: ", ex);
         } catch (SolrServerException se) {
             logger.log(Level.WARNING, "Error executing Solr query to check number of indexed files: ", se);
         }
     }
 
     /**
      * Initialize the keyword search lists from the XML loader
      */
     private void initKeywords() {
         KeywordSearchListsXML loader = KeywordSearchListsXML.getCurrent();
 
         keywords.clear();
         keywordLists.clear();
         keywordToList.clear();
 
         for (KeywordSearchList list : loader.getListsL()) {
             String listName = list.getName();
             if (list.getUseForIngest()) {
                 keywordLists.add(listName);
             }
             for (Keyword keyword : list.getKeywords()) {
                 keywords.add(keyword);
                 keywordToList.put(keyword.getQuery(), list);
             }
 
         }
     }
 
     /**
      * Retrieve the updated keyword search lists from the XML loader
      */
     private void updateKeywords() {
         KeywordSearchListsXML loader = KeywordSearchListsXML.getCurrent();
 
         searcherLock.lock();
         try {
         keywords.clear();
         keywordToList.clear();
 
         for (String name : keywordLists) {
             KeywordSearchList list = loader.getList(name);
             for (Keyword k : list.getKeywords()) {
                 keywords.add(k);
                 keywordToList.put(k.getQuery(), list);
             }
         }
         }
         finally {
             searcherLock.unlock();
         }
     }
 
     List<String> getKeywordLists() {
         return keywordLists == null ? new ArrayList<String>() : keywordLists;
     }
     
     /**
      * Check if time to commit, if so, run commit.
      * Then run search if search timer is also set.
      */
     void checkRunCommitSearch() {
         if (commitIndex) {
             logger.log(Level.INFO, "Commiting index");
             commit();
             commitIndex = false;
 
             //after commit, check if time to run searcher
             //NOTE commit/searcher timings don't need to align
             //in worst case, we will run search next time after commit timer goes off, or at the end of ingest
             if (searcherDone && runSearcher) {
                 updateKeywords();
                 //start search if previous not running
                 if (keywords != null && !keywords.isEmpty()) {
                     currentSearcher = new Searcher(keywords);
                     currentSearcher.execute();//searcher will stop timer and restart timer when done
                 }
             }
         }
     }
 
     /**
      * CommitTimerAction to run by commitTimer
      * Sets a flag to indicate we are ready for commit
      */
     private class CommitTimerAction implements ActionListener {
 
         private final Logger logger = Logger.getLogger(CommitTimerAction.class.getName());
 
         @Override
         public void actionPerformed(ActionEvent e) {
             commitIndex = true;
             logger.log(Level.INFO, "CommitTimer awake");
         }
     }
 
    /**
      * SearchTimerAction to run by searchTimer
      * Sets a flag to indicate we are ready to search
      */
     private class SearchTimerAction implements ActionListener {
 
         private final Logger logger = Logger.getLogger(SearchTimerAction.class.getName());
 
         @Override
         public void actionPerformed(ActionEvent e) {
             runSearcher = true;
             logger.log(Level.INFO, "SearchTimer awake");
         }
     }
 
     /**
      * File indexer, processes and indexes known/allocated files,
      * unknown/unallocated files and directories accordingly 
      */
     private class Indexer {
 
         private final Logger logger = Logger.getLogger(Indexer.class.getName());
 
         private boolean extractAndIngest(AbstractFile aFile) {
             boolean indexed = false;
             final FileExtract fe = new FileExtract(KeywordSearchIngestService.this, aFile);
             try {
                 indexed = fe.index(ingester);
             } catch (IngesterException ex) {
                 logger.log(Level.WARNING, "Error extracting strings and indexing file: " + aFile.getName(), ex);
                 indexed = false;
             }
             return indexed;
         }
 
         private void indexFile(AbstractFile aFile) {
             //logger.log(Level.INFO, "Processing AbstractFile: " + abstractFile.getName());
             boolean ingestibleFile = Ingester.isIngestible(aFile);
 
             final long size = aFile.getSize();
             //limit size of entire file, do not limit strings
             if (size == 0 || (ingestibleFile && size > MAX_INDEX_SIZE)) {
                 ingestStatus.put(aFile.getId(), IngestStatus.SKIPPED);
                 return;
             }
 
             if (ingestibleFile == true) {
                 //we know it's an allocated file or dir (FsContent)
                 FsContent fileDir = (FsContent) aFile;
                 try {
                     //logger.log(Level.INFO, "indexing: " + fsContent.getName());
                     ingester.ingest(fileDir);
                     ingestStatus.put(fileDir.getId(), IngestStatus.INGESTED);
                 } catch (IngesterException e) {
                     ingestStatus.put(fileDir.getId(), IngestStatus.SKIPPED);
                     //try to extract strings if not a dir
                     if (fileDir.isFile() == true) {
                         processNonIngestible(fileDir);
                     }
 
                 } catch (Exception e) {
                     ingestStatus.put(fileDir.getId(), IngestStatus.SKIPPED);
                     //try to extract strings if not a dir
                     if (fileDir.isFile() == true) {
                         processNonIngestible(fileDir);
                     }
                 }
             } else {
                 //unallocated or unsupported type by Solr
                 processNonIngestible(aFile);
 
             }
         }
 
         private boolean processNonIngestible(AbstractFile aFile) {
             if (!extractAndIngest(aFile)) {
                 logger.log(Level.WARNING, "Failed to extract strings and ingest, file '" + aFile.getName() + "' (id: " + aFile.getId() + ").");
                 ingestStatus.put(aFile.getId(), IngestStatus.SKIPPED);
                 return false;
             } else {
                 ingestStatus.put(aFile.getId(), IngestStatus.EXTRACTED_INGESTED);
                 return true;
             }
         }
     }
 
     /**
      * Searcher responsible for searching the current index and writing results to blackboard
      * and the inbox.  Also, posts results to listeners as Ingest data events.
      * Searches entire index, and keeps track of only new results to report and save.
      * Runs as a background thread.
      */
     private class Searcher extends SwingWorker<Object, Void> {
 
         private List<Keyword> keywords;
         private ProgressHandle progress;
         private final Logger logger = Logger.getLogger(Searcher.class.getName());
         private boolean finalRun = false;
 
         Searcher(List<Keyword> keywords) {
             this.keywords = keywords;
         }
 
         Searcher(List<Keyword> keywords, boolean finalRun) {
             this(keywords);
             this.finalRun = finalRun;
         }
         
 
         @Override
         protected Object doInBackground() throws Exception {
             logger.log(Level.INFO, "Pending start of new searcher");
 
             final String displayName = "Keyword Search" + (finalRun ? " - Finalizing" : "");
             progress = ProgressHandleFactory.createHandle(displayName + (" (Pending)"), new Cancellable() {
 
                 @Override
                 public boolean cancel() {
                     logger.log(Level.INFO, "Cancelling the searcher by user.");
                     if (progress != null) {
                         progress.setDisplayName(displayName + " (Cancelling...)");
                     }
                     return Searcher.this.cancel(true);
                 }
             });
 
             progress.start();
             progress.switchToIndeterminate();
 
             //block to ensure previous searcher is completely done with doInBackground()
             //even after previous searcher cancellation, we need to check this
             searcherLock.lock();
             try {
                 logger.log(Level.INFO, "Started a new searcher");
                 progress.setDisplayName(displayName);
                 //make sure other searchers are not spawned 
                 searcherDone = false;
                 runSearcher = false;
                 if (searchTimer.isRunning())
                     searchTimer.stop();
 
                 int numSearched = 0;
                 progress.switchToDeterminate(keywords.size());
 
                 for (Keyword keywordQuery : keywords) {
                     if (this.isCancelled()) {
                         logger.log(Level.INFO, "Cancel detected, bailing before new keyword processed: " + keywordQuery.getQuery());
                         return null;
                     }
                     final String queryStr = keywordQuery.getQuery();
                     final KeywordSearchList list = keywordToList.get(queryStr);
                     final String listName = list.getName();
 
                     //DEBUG
                     //logger.log(Level.INFO, "Searching: " + queryStr);
 
                     progress.progress(queryStr, numSearched);
 
                     KeywordSearchQuery del = null;
 
                     boolean isRegex = !keywordQuery.isLiteral();
                     if (!isRegex) {
                         del = new LuceneQuery(keywordQuery);
                         del.escape();
                     } else {
                         del = new TermComponentQuery(keywordQuery);
                     }
 
                     Map<String, List<ContentHit>> queryResult = null;
 
                     try {
                         queryResult = del.performQuery();
                     } catch (NoOpenCoreException ex) {
                         logger.log(Level.WARNING, "Error performing query: " + keywordQuery.getQuery(), ex);
                         //no reason to continue with next query if recovery failed
                         //or wait for recovery to kick in and run again later
                         //likely case has closed and threads are being interrupted
                         return null;
                     } catch (CancellationException e) {
                         logger.log(Level.INFO, "Cancel detected, bailing during keyword query: " + keywordQuery.getQuery());
                         return null;
                     } catch (Exception e) {
                         logger.log(Level.WARNING, "Error performing query: " + keywordQuery.getQuery(), e);
                         continue;
                     }
 
                     //calculate new results but substracting results already obtained in this run
                     Map<Keyword, List<ContentHit>> newResults = new HashMap<Keyword, List<ContentHit>>();
 
                     for (String termResult : queryResult.keySet()) {
                         List<ContentHit> queryTermResults = queryResult.get(termResult);
                         Keyword termResultK = new Keyword(termResult, !isRegex);
                         List<ContentHit> curTermResults = currentResults.get(termResultK);
                         if (curTermResults == null) {
                             currentResults.put(termResultK, queryTermResults);
                             newResults.put(termResultK, queryTermResults);
                         } else {
                             //some AbstractFile hits already exist for this keyword
                             for (ContentHit res : queryTermResults) {
                                 if (!previouslyHit(curTermResults, res)) {
                                     //add to new results
                                     List<ContentHit> newResultsFs = newResults.get(termResultK);
                                     if (newResultsFs == null) {
                                         newResultsFs = new ArrayList<ContentHit>();
                                         newResults.put(termResultK, newResultsFs);
                                     }
                                     newResultsFs.add(res);
                                     curTermResults.add(res);
                                 }
                             }
                         }
                     }
 
                     if (!newResults.isEmpty()) {
 
                         //write results to BB
 
                         //new artifacts created, to report to listeners
                         Collection<BlackboardArtifact> newArtifacts = new ArrayList<BlackboardArtifact>();
 
                         for (final Keyword hitTerm : newResults.keySet()) {
                             List<ContentHit> contentHitsAll = newResults.get(hitTerm);
                             Map<AbstractFile, Integer> contentHitsFlattened = ContentHit.flattenResults(contentHitsAll);
                             for (final AbstractFile hitFile : contentHitsFlattened.keySet()) {
                                 String snippet = null;
                                 final String snippetQuery = KeywordSearchUtil.escapeLuceneQuery(hitTerm.getQuery(), true, false);
                                 int chunkId = contentHitsFlattened.get(hitFile);
                                 try {
                                     snippet = LuceneQuery.querySnippet(snippetQuery, hitFile.getId(), chunkId, isRegex, true);
                                 } catch (NoOpenCoreException e) {
                                     logger.log(Level.WARNING, "Error querying snippet: " + snippetQuery, e);
                                     //no reason to continue
                                     return null;
                                 } catch (Exception e) {
                                     logger.log(Level.WARNING, "Error querying snippet: " + snippetQuery, e);
                                     continue;
                                 }
 
                                 KeywordWriteResult written = del.writeToBlackBoard(hitTerm.getQuery(), hitFile, snippet, listName);
 
                                 if (written == null) {
                                     logger.log(Level.WARNING, "BB artifact for keyword hit not written, file: " + hitFile + ", hit: " + hitTerm.toString());
                                     continue;
                                 }
 
                                 newArtifacts.add(written.getArtifact());
 
                                 //generate a data message for each artifact
                                 StringBuilder subjectSb = new StringBuilder();
                                 StringBuilder detailsSb = new StringBuilder();
                                 //final int hitFiles = newResults.size();
 
                                 if (!keywordQuery.isLiteral()) {
                                     subjectSb.append("RegExp hit: ");
                                 } else {
                                     subjectSb.append("Keyword hit: ");
                                 }
                                 //subjectSb.append("<");
                                 String uniqueKey = null;
                                 BlackboardAttribute attr = written.getAttribute(BlackboardAttribute.ATTRIBUTE_TYPE.TSK_KEYWORD.getTypeID());
                                 if (attr != null) {
                                     final String keyword = attr.getValueString();
                                     subjectSb.append(keyword);
                                     uniqueKey = keyword.toLowerCase();
                                 }
 
                                 //subjectSb.append(">");
                                 //String uniqueKey = queryStr;
 
                                 //details
                                 detailsSb.append("<table border='0' cellpadding='4' width='280'>");
                                 //hit
                                 detailsSb.append("<tr>");
                                 detailsSb.append("<th>Keyword hit</th>");
                                 detailsSb.append("<td>").append(StringEscapeUtils.escapeHtml(attr.getValueString())).append("</td>");
                                 detailsSb.append("</tr>");
 
                                 //preview
                                 attr = written.getAttribute(BlackboardAttribute.ATTRIBUTE_TYPE.TSK_KEYWORD_PREVIEW.getTypeID());
                                 if (attr != null) {
                                     detailsSb.append("<tr>");
                                     detailsSb.append("<th>Preview</th>");
                                     detailsSb.append("<td>").append(StringEscapeUtils.escapeHtml(attr.getValueString())).append("</td>");
                                     detailsSb.append("</tr>");
 
                                 }
 
                                 //file
                                 detailsSb.append("<tr>");
                                 detailsSb.append("<th>File</th>");
                                 if (hitFile.getType().equals(TskData.TSK_DB_FILES_TYPE_ENUM.FS)) {
                                     detailsSb.append("<td>").append(((FsContent) hitFile).getParentPath()).append(hitFile.getName()).append("</td>");
                                 } else {
                                     detailsSb.append("<td>").append(hitFile.getName()).append("</td>");
                                 }
                                 detailsSb.append("</tr>");
 
 
                                 //list
                                 attr = written.getAttribute(BlackboardAttribute.ATTRIBUTE_TYPE.TSK_SET_NAME.getTypeID());
                                 detailsSb.append("<tr>");
                                 detailsSb.append("<th>List</th>");
                                 detailsSb.append("<td>").append(attr.getValueString()).append("</td>");
                                 detailsSb.append("</tr>");
 
                                 //regex
                                 if (!keywordQuery.isLiteral()) {
                                     attr = written.getAttribute(BlackboardAttribute.ATTRIBUTE_TYPE.TSK_KEYWORD_REGEXP.getTypeID());
                                     if (attr != null) {
                                         detailsSb.append("<tr>");
                                         detailsSb.append("<th>RegEx</th>");
                                         detailsSb.append("<td>").append(attr.getValueString()).append("</td>");
                                         detailsSb.append("</tr>");
 
                                     }
                                 }
                                 detailsSb.append("</table>");
 
                                 //check if should send messages on hits on this list
                                 if (list.getIngestMessages()) //post ingest inbox msg
                                 {
                                     managerProxy.postMessage(IngestMessage.createDataMessage(++messageID, instance, subjectSb.toString(), detailsSb.toString(), uniqueKey, written.getArtifact()));
                                 }
 
 
                             } //for each term hit
                         }//for each file hit
 
                         //update artifact browser
                         if (!newArtifacts.isEmpty()) {
                             IngestManager.fireServiceDataEvent(new ServiceDataEvent(MODULE_NAME, ARTIFACT_TYPE.TSK_KEYWORD_HIT, newArtifacts));
                         }
                     }
                     progress.progress(queryStr, ++numSearched);
                 }
 
             } //end try block
             catch (Exception ex) {
                 logger.log(Level.WARNING, "searcher exception occurred", ex);
             }
             finally {
                 finalizeSearcher();
                 searcherLock.unlock();
             }
 
             return null;
         }
         
         
 
         //perform all essential cleanup that needs to be done right AFTER doInBackground() returns
         //without relying on done() method that is not guaranteed to run after background thread completes
         //NEED to call this method always right before doInBackground() returns
         
         /**
          * Performs the cleanup that needs to be done right AFTER doInBackground() returns
          * without relying on done() method that is not guaranteed to run after background thread completes
          * REQUIRED to call this method always right before doInBackground() returns
          */
         private void finalizeSearcher() {
             logger.log(Level.INFO, "Searcher finalizing");
             SwingUtilities.invokeLater(new Runnable() {
 
                 @Override
                 public void run() {
                     progress.finish();
                 }
             });
             searcherDone = true;  //next currentSearcher can start
 
             if (finalRun) {
                 //this is the final searcher
                 logger.log(Level.INFO, "The final searcher in this ingest done.");
                 finalSearcherDone = true;
                 keywords.clear();
                 keywordLists.clear();
                 keywordToList.clear();
                 //reset current resuls earlier to potentially garbage collect sooner
                 currentResults = new HashMap<Keyword, List<ContentHit>>();
 
                 managerProxy.postMessage(IngestMessage.createMessage(++messageID, MessageType.INFO, KeywordSearchIngestService.instance, "Completed"));
             }
             else {
                 //start counting time for a new searcher to start
                 //unless final searcher is pending
                 if (finalSearcher != null)
                     searchTimer.start(); 
             }
         }
     }
 
     /**
      * Checks if the content has already been hit previously
      * @param previousHits the previous hits to check against
      * @param new hit, that potentially had already been hit
      * @return true if already hit
      */
     private static boolean previouslyHit(List<ContentHit> previousHits, ContentHit hit) {
         boolean ret = false;
         long hitId = hit.getId();
         for (ContentHit c : previousHits) {
             if (c.getId() == hitId) {
                 ret = true;
                 break;
             }
         }
         return ret;
     }
 
     /**
      * Set the skip known files setting on the service
      * @param skip true if skip, otherwise, will process known files as well, as reported by HashDB service
      */
     void setSkipKnown(boolean skip) {
         this.skipKnown = skip;
     }
     
     boolean getSkipKnown() {
         return skipKnown;
     }
 }
