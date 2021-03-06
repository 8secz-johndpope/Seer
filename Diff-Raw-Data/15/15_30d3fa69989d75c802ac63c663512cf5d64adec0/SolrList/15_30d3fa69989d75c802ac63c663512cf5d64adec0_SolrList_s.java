 package eu.europeana.uim.enrichment.utils;
 
 import java.io.IOException;
 import java.util.ArrayList;
 import java.util.List;
 
 import org.apache.solr.client.solrj.SolrServer;
 import org.apache.solr.client.solrj.SolrServerException;
 import org.apache.solr.common.SolrInputDocument;
 
 /**
  * Singleton implementation of the Solr InputDocument List
  * @author Yorgos.Mamakis@ kb.nl
  *
  */
 public class SolrList {
 
 	public static SolrList solrList;
 	public static List<SolrInputDocument> queue;
 	private final static int MAX_QUEUE_SIZE=1000;
 	
 	private SolrList(){
 		queue = new ArrayList<SolrInputDocument>();
 	}
 	
 	/**
 	 * Get the current queue of documents
 	 * @return
 	 */
 	public List<SolrInputDocument> getQueue(){
 		return queue;
 	}
 	
 	/**
 	 * Add a document to a queue by saving first if the size exceeds the MAX_QUEUE_SIZE
 	 * @param server
 	 * @param doc
 	 * @throws SolrServerException
 	 * @throws IOException
 	 */
 	public void addToQueue(SolrServer server, SolrInputDocument doc) throws SolrServerException, IOException{
 		synchronized(queue){
 		if(queue.size()>=MAX_QUEUE_SIZE){
 			
			server.add(queue,10000);
 			queue = new ArrayList<SolrInputDocument>();
 		}
 		}
 		queue.add(doc);
 	}
 	
 	/**
 	 * Get a single instance of the SolrList
 	 * @return
 	 */
 	public static SolrList getInstance(){
 		if(solrList==null){
 			solrList = new SolrList();
 		}
 		return solrList;
 	}
 	
 	
 }
