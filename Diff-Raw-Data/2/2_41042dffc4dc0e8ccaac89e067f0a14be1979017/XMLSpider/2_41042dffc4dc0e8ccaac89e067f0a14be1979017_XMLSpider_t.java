 /* This code is part of Freenet. It is distributed under the GNU General
  * Public License, version 2 (or at your option any later version). See
  * http://www.gnu.org/ for further details of the GPL. */
 package plugins.XMLSpider;
 
 import java.io.BufferedOutputStream;
 import java.io.File;
 import java.io.FileNotFoundException;
 import java.io.FileOutputStream;
 import java.io.IOException;
 import java.io.UnsupportedEncodingException;
 import java.net.MalformedURLException;
 import java.net.URI;
 import java.net.URISyntaxException;
 import java.security.MessageDigest;
 import java.security.NoSuchAlgorithmException;
 import java.util.ArrayList;
 import java.util.Collections;
 import java.util.Date;
 import java.util.HashMap;
 import java.util.HashSet;
 import java.util.Iterator;
 import java.util.List;
 import java.util.Map;
 import java.util.Set;
 import java.util.Vector;
 import java.util.concurrent.atomic.AtomicLong;
 
 import javax.xml.parsers.DocumentBuilder;
 import javax.xml.parsers.DocumentBuilderFactory;
 import javax.xml.transform.OutputKeys;
 import javax.xml.transform.Transformer;
 import javax.xml.transform.TransformerFactory;
 import javax.xml.transform.dom.DOMSource;
 import javax.xml.transform.stream.StreamResult;
 
 import org.w3c.dom.DOMImplementation;
 import org.w3c.dom.Document;
 import org.w3c.dom.Element;
 import org.w3c.dom.Text;
 
 import com.db4o.Db4o;
 import com.db4o.ObjectContainer;
 import com.db4o.ObjectSet;
 import com.db4o.config.Configuration;
 import com.db4o.config.QueryEvaluationMode;
 import com.db4o.diagnostic.DiagnosticToConsole;
 import com.db4o.query.Query;
 
 import freenet.client.ClientMetadata;
 import freenet.client.FetchContext;
 import freenet.client.FetchException;
 import freenet.client.FetchResult;
 import freenet.client.InsertException;
 import freenet.client.async.BaseClientPutter;
 import freenet.client.async.ClientCallback;
 import freenet.client.async.ClientGetter;
 import freenet.client.async.USKCallback;
 import freenet.clients.http.PageMaker;
 import freenet.clients.http.filter.ContentFilter;
 import freenet.clients.http.filter.FoundURICallback;
 import freenet.clients.http.filter.UnsafeContentTypeException;
 import freenet.keys.FreenetURI;
 import freenet.keys.USK;
 import freenet.node.NodeClientCore;
 import freenet.node.PrioRunnable;
 import freenet.node.RequestStarter;
 import freenet.pluginmanager.FredPlugin;
 import freenet.pluginmanager.FredPluginHTTP;
 import freenet.pluginmanager.FredPluginThreadless;
 import freenet.pluginmanager.FredPluginVersioned;
 import freenet.pluginmanager.PluginHTTPException;
 import freenet.pluginmanager.PluginRespirator;
 import freenet.support.HTMLNode;
 import freenet.support.Logger;
 import freenet.support.api.Bucket;
 import freenet.support.api.HTTPRequest;
 import freenet.support.io.NativeThread;
 import freenet.support.io.NullBucketFactory;
 
 /**
  * XMLSpider. Produces xml index for searching words. 
  * In case the size of the index grows up a specific threshold the index is split into several subindices.
  * The indexing key is the md5 hash of the word.
  * 
  *  @author swati goyal
  *  
  */
 public class XMLSpider implements FredPlugin, FredPluginHTTP, FredPluginThreadless, FredPluginVersioned, USKCallback {
 	static enum Status {
 		/** For simplicity, running is also mark as QUEUED */
 		QUEUED, SUCCEEDED, FAILED
 	};
 
 	static class Page {
 		/** Page Id */
 		long id;
 		/** URI of the page */
 		String uri;
 		/** Title */
 		String pageTitle;
 		/** Status */
 		Status status = Status.QUEUED;
 		/** Last Change Time */
 		long lastChange = System.currentTimeMillis();
 		/** Comment, for debugging */
 		String comment;
 
 		public Page() {}	// for db4o callConstructors(true)
 
 		@Override
 		public int hashCode() {
 			return (int) (id ^ (id >>> 32));
 		}
 
 		@Override
 		public boolean equals(Object obj) {
 			if (this == obj)
 				return true;
 			if (obj == null)
 				return false;
 			if (getClass() != obj.getClass())
 				return false;
 
 			return id == ((Page) obj).id;
 		}
 
 		@Override
 		public String toString() {
 			return "[PAGE: id=" + id + ", title=" + pageTitle + ", uri=" + uri + ", status=" + status + ", comment="
 			+ comment
 			+ "]";
 		}
 	}
 
 	static class Term {
 		/** MD5 of the term */
 		String md5;
 		/** Term */
 		String word;
 
 		public Term(String word) {
 			this.word = word;
 			md5 = MD5(word);
 		}
 
 		public Term() {
 		}
 	}
 
 	static class TermPosition {
 		/** Term */
 		String word;
 		/** Page id */
 		long pageId;
 		/** Position List */
 		int[] positions;
 
 		public TermPosition() {
 		}
 	}
 
 	/** Document ID of fetching documents */
 	protected Map<Page, ClientGetter> runningFetch = Collections.synchronizedMap(new HashMap<Page, ClientGetter>());
 
 	long tProducedIndex;
 	protected AtomicLong maxPageId;
 
 	private Vector<String> indices;
 	private int match;
 	private long time_taken;
 
 	/**
 	 * directory where the generated indices are stored. 
 	 * Needs to be created before it can be used
 	 */
 	public static final String DEFAULT_INDEX_DIR = "myindex7/";
 	/**
 	 * Lists the allowed mime types of the fetched page. 
 	 */
 	public Set<String> allowedMIMETypes;
 	private static final int MAX_ENTRIES = 800;
 	private static final long MAX_SUBINDEX_UNCOMPRESSED_SIZE = 4*1024*1024;
 	private static int version = 33;
 	private static final String pluginName = "XML spider " + version;
 
 	public String getVersion() {
 		return version + " r" + Version.getSvnRevision();
 	}
 
 	/**
 	 * Gives the allowed fraction of total time spent on generating indices with
 	 * maximum value = 1; minimum value = 0. 
 	 */
 	public static final double MAX_TIME_SPENT_INDEXING = 0.5;
 
 	private static final String indexTitle= "XMLSpider index";
 	private static final String indexOwner = "Freenet";
 	private static final String indexOwnerEmail = null;
 
 	// Can have many; this limit only exists to save memory.
 	private static final int maxParallelRequests = 100;
 	private int maxShownURIs = 15;
 
 	private NodeClientCore core;
 	private FetchContext ctx;
 	// Equal to Frost, ARK fetches etc. One step down from Fproxy.
 	// Any lower makes it very difficult to debug. Maybe reduce for production - after solving the ARK bugs.
 	private final short PRIORITY_CLASS = RequestStarter.IMMEDIATE_SPLITFILE_PRIORITY_CLASS;
 	private boolean stopped = true;
 
 	private PageMaker pageMaker;
 
 	private final static String[] BADLIST_EXTENSTION = new String[] { 
 		".ico", ".bmp", ".png", ".jpg", ".gif",		// image
 		".zip", ".jar", ".gz" , ".bz2", ".rar",		// archive
 		".7z" , ".rar", ".arj", ".rpm",	".deb",
 		".xpi", ".ace", ".cab", ".lza", ".lzh",
 		".ace",
 		".exe", ".iso",								// binary
 		".mpg", ".ogg", ".mp3", ".avi",				// media
 		".css", ".sig"								// other
 	};
 
 	/**
 	 * Adds the found uri to the list of to-be-retrieved uris. <p>Every usk uri added as ssk.
 	 * @param uri the new uri that needs to be fetched for further indexing
 	 */
 	public void queueURI(FreenetURI uri, String comment, boolean force) {
 		String sURI = uri.toString();
 		for (String ext : BADLIST_EXTENSTION)
 			if (sURI.endsWith(ext))
 				return;	// be smart
 
 		if (uri.isUSK()) {
 			if(uri.getSuggestedEdition() < 0)
 				uri = uri.setSuggestedEdition((-1)* uri.getSuggestedEdition());
 			try{
 				uri = ((USK.create(uri)).getSSK()).getURI();
 				(ctx.uskManager).subscribe(USK.create(uri),this, false, this);	
 			}
 			catch(Exception e){}
 		}
 
 		synchronized (this) {
 			Page page = getPageByURI(uri);
 			if (page == null) {
 				page = new Page();
 				page.uri = uri.toString();
 				page.id = maxPageId.incrementAndGet();
 				page.comment = comment;
 
 				db.store(page);
 			} else if (force) {
 				synchronized (page) {
 					page.status = Status.QUEUED;
 					page.lastChange = System.currentTimeMillis();
 
 					db.store(page);
 				}
 			}
 			db.commit();
 		}
 	}
 
 	private void startSomeRequests() {
 		FreenetURI[] initialURIs = core.getBookmarkURIs();
 		for (int i = 0; i < initialURIs.length; i++)
 			queueURI(initialURIs[i], "bookmark", false);
 
 		ArrayList<ClientGetter> toStart = null;
 		synchronized (this) {
 			if (stopped)
 				return;
 			synchronized (runningFetch) {
 
 				int running = runningFetch.size();
 
 				Query query = db.query();
 				query.constrain(Page.class);
 				query.descend("status").constrain(Status.QUEUED);
 				query.descend("lastChange").orderAscending();
 				ObjectSet<Page> queuedSet = query.execute();
 
 				if ((running >= maxParallelRequests) || (queuedSet.size() - running <= 0))
 					return;
 
 				toStart = new ArrayList<ClientGetter>(maxParallelRequests - running);
 
 				while (running + toStart.size() < maxParallelRequests && queuedSet.hasNext()) {
 					Page page = queuedSet.next();
 
 					if (runningFetch.containsKey(page))
 						continue;
 
 					try {
 						ClientGetter getter = makeGetter(page);
 
 						Logger.minor(this, "Starting " + getter + " " + page);
 						toStart.add(getter);
 						runningFetch.put(page, getter);
 					} catch (MalformedURLException e) {
 						Logger.error(this, "IMPOSSIBLE-Malformed URI: " + page, e);
 
 						page.status = Status.FAILED;
 						page.lastChange = System.currentTimeMillis();
 						db.store(page);
 					}
 				}
 			}
 		}
 
 		for (ClientGetter g : toStart) {
 			try {
 				g.start();
 				Logger.minor(this, g + " started");
 			} catch (FetchException e) {
 				Logger.minor(this, "Fetch Exception: " + g, e);
 				onFailure(e, g, ((MyClientCallback) g.getClientCallback()).page);
 			}
 		}
 	}
 
 	private class MyClientCallback implements ClientCallback {
 		final Page page;
 
 		public MyClientCallback(Page page) {
 			this.page = page;
 		}
 
 		public void onFailure(FetchException e, ClientGetter state) {
 			XMLSpider.this.onFailure(e, state, page);
 		}
 
 		public void onFailure(InsertException e, BaseClientPutter state) {
 			// Ignore
 		}
 
 		public void onFetchable(BaseClientPutter state) {
 			// Ignore
 		}
 
 		public void onGeneratedURI(FreenetURI uri, BaseClientPutter state) {
 			// Ignore
 		}
 
 		public void onMajorProgress() {
 			// Ignore
 		}
 
 		public void onSuccess(FetchResult result, ClientGetter state) {
 			XMLSpider.this.onSuccess(result, state, page);
 		}
 
 		public void onSuccess(BaseClientPutter state) {
 			// Ignore
 		}
 
 		public String toString() {
 			return super.toString() + ":" + page;
 		}		
 	}
 
 	private ClientGetter makeGetter(Page page) throws MalformedURLException {
 		ClientGetter getter = new ClientGetter(new MyClientCallback(page),
 				core.requestStarters.chkFetchScheduler,
 		        core.requestStarters.sskFetchScheduler, new FreenetURI(page.uri), ctx, PRIORITY_CLASS, this, null, null);
 		return getter;
 	}
 
 	/**
 	 * Processes the successfully fetched uri for further outlinks.
 	 * 
 	 * @param result
 	 * @param state
 	 * @param page
 	 */
 	public void onSuccess(FetchResult result, ClientGetter state, Page page) {
 		synchronized (this) {
 			while (writingIndex && !stopped) {
 				try {
 					wait();
 				} catch (InterruptedException e) {
 					return;
 				}
 			}
 
 			if (stopped)
 				return;    				
 		}
 
 		FreenetURI uri = state.getURI();
 		page.status = Status.SUCCEEDED; // Content filter may throw, but we mark it as success anyway
 
 		try {
 			synchronized (page) {
 				// Page may be refetched if added manually
 				// Delete existing TermPosition
 				Query query = db.query();
 				query.constrain(TermPosition.class);
 				query.descend("pageId").constrain(page.id);
 				ObjectSet<TermPosition> set = query.execute();
 				for (TermPosition tp : set)
 					db.delete(tp);
 
 				ClientMetadata cm = result.getMetadata();
 				Bucket data = result.asBucket();
 				String mimeType = cm.getMIMEType();
 
 				/*
 				 * instead of passing the current object, the pagecallback object for every page is
 				 * passed to the content filter this has many benefits to efficiency, and allows us
 				 * to identify trivially which page is being indexed. (we CANNOT rely on the base
 				 * href provided).
 				 */
 				PageCallBack pageCallBack = new PageCallBack(page);
 				Logger.minor(this, "Successful: " + uri + " : " + page.id);
 
 				try {
 					ContentFilter.filter(data, new NullBucketFactory(), mimeType, uri.toURI("http://127.0.0.1:8888/"),
 					        pageCallBack);
 					Logger.minor(this, "Filtered " + uri + " : " + page.id);
 				} catch (UnsafeContentTypeException e) {
 					return; // Ignore
 				} catch (IOException e) {
 					Logger.error(this, "Bucket error?: " + e, e);
 				} catch (URISyntaxException e) {
 					Logger.error(this, "Internal error: " + e, e);
 				} finally {
 					data.free();
 				}
 			}
 		} finally {
 			synchronized (this) {
 				runningFetch.remove(page);
 				page.lastChange = System.currentTimeMillis();
 				db.store(page);
 				db.commit();
 			}
 			startSomeRequests();
 		}
 	}
 
 	public void onFailure(FetchException fe, ClientGetter state, Page page) {
 		Logger.minor(this, "Failed: " + page + " : " + state, fe);
 
 		synchronized (this) {
 			if (stopped)
 				return;
 
 			synchronized (page) {
 				if (fe.newURI != null) {
 					// redirect, mark as succeeded
 					queueURI(fe.newURI, "redirect from " + state.getURI(), false);
 
 					page.status = Status.SUCCEEDED;
 					page.lastChange = System.currentTimeMillis();
 					db.store(page);
 				} else if (fe.isFatal()) {
 					// too many tries or fatal, mark as failed
 					page.status = Status.FAILED;
 					page.lastChange = System.currentTimeMillis();
 					db.store(page);
 				} else {
 					// requeue at back
 					page.status = Status.QUEUED;
 					page.lastChange = System.currentTimeMillis();
 
 					db.store(page);
 				}
 			}
 			db.commit();
 			runningFetch.remove(page);
 		}
 
 		startSomeRequests();
 	} 
 
 	/**
 	 * generates the main index file that can be used by librarian for searching in the list of
 	 * subindices
 	 *  
 	 * @param void
 	 * @author swati 
 	 * @throws IOException
 	 * @throws NoSuchAlgorithmException
 	 */
 	private void makeMainIndex() throws IOException, NoSuchAlgorithmException {
 		// Produce the main index file.
 		Logger.minor(this, "Producing top index...");
 
 		//the main index file 
 		File outputFile = new File(DEFAULT_INDEX_DIR+"index.xml");
 		// Use a stream so we can explicitly close - minimise number of filehandles used.
 		BufferedOutputStream fos = new BufferedOutputStream(new FileOutputStream(outputFile));
 		StreamResult resultStream;
 		resultStream = new StreamResult(fos);
 
 		try {
 			/* Initialize xml builder */
 			Document xmlDoc = null;
 			DocumentBuilderFactory xmlFactory = null;
 			DocumentBuilder xmlBuilder = null;
 			DOMImplementation impl = null;
 			Element rootElement = null;
 
 			xmlFactory = DocumentBuilderFactory.newInstance();
 
 
 			try {
 				xmlBuilder = xmlFactory.newDocumentBuilder();
 			} catch (javax.xml.parsers.ParserConfigurationException e) {
 
 				Logger.error(this, "Spider: Error while initializing XML generator: " + e.toString(), e);
 				return;
 			}
 
 			impl = xmlBuilder.getDOMImplementation();
 			/* Starting to generate index */
 			xmlDoc = impl.createDocument(null, "main_index", null);
 			rootElement = xmlDoc.getDocumentElement();
 
 			/* Adding header to the index */
 			Element headerElement = xmlDoc.createElement("header");
 
 			/* -> title */
 			Element subHeaderElement = xmlDoc.createElement("title");
 			Text subHeaderText = xmlDoc.createTextNode(indexTitle);
 
 			subHeaderElement.appendChild(subHeaderText);
 			headerElement.appendChild(subHeaderElement);
 
 			/* -> owner */
 			subHeaderElement = xmlDoc.createElement("owner");
 			subHeaderText = xmlDoc.createTextNode(indexOwner);
 
 			subHeaderElement.appendChild(subHeaderText);
 			headerElement.appendChild(subHeaderElement);
 
 			/* -> owner email */
 			if (indexOwnerEmail != null) {
 				subHeaderElement = xmlDoc.createElement("email");
 				subHeaderText = xmlDoc.createTextNode(indexOwnerEmail);
 
 				subHeaderElement.appendChild(subHeaderText);
 				headerElement.appendChild(subHeaderElement);
 			}
 			/*
 			 * the max number of digits in md5 to be used for matching with the search query is
 			 * stored in the xml
 			 */
 			Element prefixElement = xmlDoc.createElement("prefix");
 			/* Adding word index */
 			Element keywordsElement = xmlDoc.createElement("keywords");
 			for (int i = 0; i < indices.size(); i++) {
 
 				Element subIndexElement = xmlDoc.createElement("subIndex");
 				subIndexElement.setAttribute("key", indices.elementAt(i));
 				//the subindex element key will contain the bits used for matching in that subindex
 				keywordsElement.appendChild(subIndexElement);
 			}
 
 			prefixElement.setAttribute("value", match + "");
 			rootElement.appendChild(prefixElement);
 			rootElement.appendChild(headerElement);
 			rootElement.appendChild(keywordsElement);
 
 			/* Serialization */
 			DOMSource domSource = new DOMSource(xmlDoc);
 			TransformerFactory transformFactory = TransformerFactory.newInstance();
 			Transformer serializer;
 
 			try {
 				serializer = transformFactory.newTransformer();
 			} catch (javax.xml.transform.TransformerConfigurationException e) {
 				Logger.error(this, "Spider: Error while serializing XML (transformFactory.newTransformer()): "
 				        + e.toString(), e);
 				return;
 			}
 
 			serializer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
 			serializer.setOutputProperty(OutputKeys.INDENT, "yes");
 
 			/* final step */
 			try {
 				serializer.transform(domSource, resultStream);
 			} catch (javax.xml.transform.TransformerException e) {
 				Logger.error(this, "Spider: Error while serializing XML (transform()): " + e.toString(), e);
 				return;
 			}
 		} finally {
 			fos.close();
 		}
 
 		//The main xml file is generated 
 		//As each word is generated enter it into the respective subindex
 		//The parsing will start and nodes will be added as needed 
 
 	}
 	/**
 	 * Generates the subindices. 
 	 * Each index has less than {@code MAX_ENTRIES} words.
 	 * The original treemap is split into several sublists indexed by the common substring
 	 * of the hash code of the words
 	 * @throws Exception
 	 */
 	private void makeSubIndices() throws Exception {
 		Logger.normal(this, "Generating index...");
 
 		Query query = db.query();
 		query.constrain(Term.class);
 		query.descend("md5").orderAscending();
 		ObjectSet<Term> termSet = query.execute();
 
 		indices = new Vector<String>();
 		int prefix = (int) ((Math.log(termSet.size()) - Math.log(MAX_ENTRIES)) / Math.log(16)) - 1;
 		if (prefix <= 0) prefix = 1;
 		match = 1;
 		Vector<String> list = new Vector<String>();
 
 		String str = termSet.get(0).md5;
 		String currentPrefix = str.substring(0, prefix);
 		list.add(str);
 
 		int i = 0;
 		for (Term term : termSet)
 		{
 			String key = term.md5;
 			//create a list of the words to be added in the same subindex
 			if (key.startsWith(currentPrefix)) 
 			{i++;
 			list.add(key);
 			}
 			else {
 				//generate the appropriate subindex with the current list
 				generateSubIndex(prefix,list);
 				str = key;
 				currentPrefix = str.substring(0, prefix);
 				list = new Vector<String>();
 				list.add(key);
 			}
 		}
 
 		generateSubIndex(prefix,list);
 	}
 
 
 	private void generateSubIndex(int p, List<String> list) throws Exception {
 		boolean logMINOR = Logger.shouldLog(Logger.MINOR, this);
 		/*
 		 * if the list is less than max allowed entries in a file then directly generate the xml 
 		 * otherwise split the list into further sublists
 		 * and iterate till the number of entries per subindex is less than the allowed value
 		 */
 		if(logMINOR)
 			Logger.minor(this, "Generating subindex for "+list.size()+" entries with prefix length "+p);
 
 		try {
 			if (list.size() == 0)
 				return;
 			if (list.size() < MAX_ENTRIES)
 			{	
 				generateXML(list,p);
 				return;
 			}
 		} catch (TooBigIndexException e) {
 			// Handle below
 		}
 		if(logMINOR)
 			Logger.minor(this, "Too big subindex for "+list.size()+" entries with prefix length "+p);
 		//prefix needs to be incremented
 		if (match <= p)
 			match = p + 1;
 		int prefix = p + 1;
 		int i = 0;
 		String str = list.get(i);
 		int index = 0;
 		while (i < list.size()) {
 			String key = list.get(i);
 			if ((key.substring(0, prefix)).equals(str.substring(0, prefix))) 
 			{
 				i++;
 			} else {
 				generateSubIndex(prefix, list.subList(index, i));
 				index = i;
 				str = key;
 			}
 		}
 		generateSubIndex(prefix, list.subList(index, i));
 	}	
 
 	private class TooBigIndexException extends Exception {
 		private static final long serialVersionUID = -6172560811504794914L;
 	}
 
 	/**
 	 * generates the xml index with the given list of words with prefix number of matching bits in md5
 	 * @param list  list of the words to be added in the index
 	 * @param prefix number of matching bits of md5
 	 * @throws Exception
 	 */
 	protected void generateXML(List<String> list, int prefix) throws TooBigIndexException, Exception
 	{
 		String p = list.get(0).substring(0, prefix);
 		indices.add(p);
 		File outputFile = new File(DEFAULT_INDEX_DIR+"index_"+p+".xml");
 		BufferedOutputStream fos = new BufferedOutputStream(new FileOutputStream(outputFile));
 		StreamResult resultStream;
 		resultStream = new StreamResult(fos);
 
 		try {
 			/* Initialize xml builder */
 			Document xmlDoc = null;
 			DocumentBuilderFactory xmlFactory = null;
 			DocumentBuilder xmlBuilder = null;
 			DOMImplementation impl = null;
 			Element rootElement = null;
 			xmlFactory = DocumentBuilderFactory.newInstance();
 
 			try {
 				xmlBuilder = xmlFactory.newDocumentBuilder();
 			} catch (javax.xml.parsers.ParserConfigurationException e) {
 				Logger.error(this, "Spider: Error while initializing XML generator: " + e.toString(), e);
 				return;
 			}
 
 			impl = xmlBuilder.getDOMImplementation();
 			/* Starting to generate index */
 			xmlDoc = impl.createDocument(null, "sub_index", null);
 			rootElement = xmlDoc.getDocumentElement();
 
 			/* Adding header to the index */
 			Element headerElement = xmlDoc.createElement("header");
 			/* -> title */
 			Element subHeaderElement = xmlDoc.createElement("title");
 			Text subHeaderText = xmlDoc.createTextNode(indexTitle);
 			subHeaderElement.appendChild(subHeaderText);
 			headerElement.appendChild(subHeaderElement);
 
 			Element filesElement = xmlDoc.createElement("files"); /* filesElement != fileElement */
 			Element EntriesElement = xmlDoc.createElement("entries");
 			EntriesElement.setNodeValue(list.size() + "");
 			EntriesElement.setAttribute("value", list.size() + "");
 
 			/* Adding word index */
 			Element keywordsElement = xmlDoc.createElement("keywords");
 			Vector<Long> fileid = new Vector<Long>();
 			for (int i = 0; i < list.size(); i++) {
 				Element wordElement = xmlDoc.createElement("word");
 				Term term = getTermByMd5(list.get(i));
 				wordElement.setAttribute("v", term.word);
 
 				Query query = db.query();
 				query.constrain(TermPosition.class);
 
 				query.descend("word").constrain(term.word);
 				ObjectSet<TermPosition> set = query.execute();
 
 				for (TermPosition termPos : set) {
 					synchronized (termPos) {
 						Page page = getPageById(termPos.pageId);
 
 						synchronized (page) {
 
 							/*
 							 * adding file information uriElement - lists the id of the file
 							 * containing a particular word fileElement - lists the id,key,title of
 							 * the files mentioned in the entire subindex
 							 */
 							Element uriElement = xmlDoc.createElement("file");
 							Element fileElement = xmlDoc.createElement("file");
 							uriElement.setAttribute("id", Long.toString(page.id));
 							fileElement.setAttribute("id", Long.toString(page.id));
 							fileElement.setAttribute("key", page.uri);
 							fileElement.setAttribute("title", page.pageTitle != null ? page.pageTitle : page.uri);
 
 							/* Position by position */
 							int[] positions = termPos.positions;
 
 							StringBuilder positionList = new StringBuilder();
 
 							for (int k = 0; k < positions.length; k++) {
 								if (k != 0)
 									positionList.append(',');
 								positionList.append(positions[k]);
 							}
 							uriElement.appendChild(xmlDoc.createTextNode(positionList.toString()));
 							wordElement.appendChild(uriElement);
 							if (!fileid.contains(page.id)) {
 								fileid.add(page.id);
 								filesElement.appendChild(fileElement);
 							}
 						}
 					}
 				}
 				keywordsElement.appendChild(wordElement);
 			}
 			rootElement.appendChild(EntriesElement);
 			rootElement.appendChild(headerElement);
 			rootElement.appendChild(filesElement);
 			rootElement.appendChild(keywordsElement);
 
 			/* Serialization */
 			DOMSource domSource = new DOMSource(xmlDoc);
 			TransformerFactory transformFactory = TransformerFactory.newInstance();
 			Transformer serializer;
 
 			try {
 				serializer = transformFactory.newTransformer();
 			} catch (javax.xml.transform.TransformerConfigurationException e) {
 				Logger.error(this, "Spider: Error while serializing XML (transformFactory.newTransformer()): "
 				        + e.toString(), e);
 				return;
 			}
 			serializer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
 			serializer.setOutputProperty(OutputKeys.INDENT, "yes");
 			/* final step */
 			try {
 				serializer.transform(domSource, resultStream);
 			} catch (javax.xml.transform.TransformerException e) {
 				Logger.error(this, "Spider: Error while serializing XML (transform()): " + e.toString(), e);
 				return;
 			}
 		} finally {
 			fos.close();
 		}
 		if(outputFile.length() > MAX_SUBINDEX_UNCOMPRESSED_SIZE && list.size() > 1) {
 			outputFile.delete();
 			throw new TooBigIndexException();
 		}
 
 		if(Logger.shouldLog(Logger.MINOR, this))
 			Logger.minor(this, "Spider: indexes regenerated.");
 	}
 
 	private static String convertToHex(byte[] data) {
 		StringBuilder buf = new StringBuilder();
 		for (int i = 0; i < data.length; i++) {
 			int halfbyte = (data[i] >>> 4) & 0x0F;
 			int two_halfs = 0;
 			do {
 				if ((0 <= halfbyte) && (halfbyte <= 9))
 					buf.append((char) ('0' + halfbyte));
 				else
 					buf.append((char) ('a' + (halfbyte - 10)));
 				halfbyte = data[i] & 0x0F;
 			} while(two_halfs++ < 1);
 		}
 		return buf.toString();
 	}
 
 	/*
 	 * calculate the md5 for a given string
 	 */
 	private static String MD5(String text) {
 		try {
 			MessageDigest md = MessageDigest.getInstance("MD5");
 			byte[] md5hash = new byte[32];
 			byte[] b = text.getBytes("UTF-8");
 			md.update(b, 0, b.length);
 			md5hash = md.digest();
 			return convertToHex(md5hash);
 		} catch (UnsupportedEncodingException e) {
 			throw new RuntimeException("UTF-8 not supported", e);
 		} catch (NoSuchAlgorithmException e) {
 			throw new RuntimeException("MD5 not supported", e);
 		}
 	}
 
 	public void generateSubIndex(String filename){
 		//		generates the new subIndex
 		File outputFile = new File(filename);
 		BufferedOutputStream fos;
 		try {
 			fos = new BufferedOutputStream(new FileOutputStream(outputFile));
 		} catch (FileNotFoundException e1) {
 			Logger.error(this, "Cannot open "+filename+" writing index : "+e1, e1);
 			return;
 		}
 		try {
 			StreamResult resultStream;
 			resultStream = new StreamResult(fos);
 
 			/* Initialize xml builder */
 			Document xmlDoc = null;
 			DocumentBuilderFactory xmlFactory = null;
 			DocumentBuilder xmlBuilder = null;
 			DOMImplementation impl = null;
 			Element rootElement = null;
 
 			xmlFactory = DocumentBuilderFactory.newInstance();
 
 
 			try {
 				xmlBuilder = xmlFactory.newDocumentBuilder();
 			} catch (javax.xml.parsers.ParserConfigurationException e) {
 				/* Will (should ?) never happen */
 				Logger.error(this, "Spider: Error while initializing XML generator: " + e.toString(), e);
 				return;
 			}
 
 
 			impl = xmlBuilder.getDOMImplementation();
 
 			/* Starting to generate index */
 
 			xmlDoc = impl.createDocument(null, "sub_index", null);
 			rootElement = xmlDoc.getDocumentElement();
 
 			/* Adding header to the index */
 			Element headerElement = xmlDoc.createElement("header");
 
 			/* -> title */
 			Element subHeaderElement = xmlDoc.createElement("title");
 			Text subHeaderText = xmlDoc.createTextNode(indexTitle);
 
 			subHeaderElement.appendChild(subHeaderText);
 			headerElement.appendChild(subHeaderElement);
 
 			/* -> owner */
 			subHeaderElement = xmlDoc.createElement("owner");
 			subHeaderText = xmlDoc.createTextNode(indexOwner);
 
 			subHeaderElement.appendChild(subHeaderText);
 			headerElement.appendChild(subHeaderElement);
 
 
 			/* -> owner email */
 			if (indexOwnerEmail != null) {
 				subHeaderElement = xmlDoc.createElement("email");
 				subHeaderText = xmlDoc.createTextNode(indexOwnerEmail);
 
 				subHeaderElement.appendChild(subHeaderText);
 				headerElement.appendChild(subHeaderElement);
 			}
 
 
 			Element filesElement = xmlDoc.createElement("files"); /* filesElement != fileElement */
 
 			Element EntriesElement = xmlDoc.createElement("entries");
 			EntriesElement.setNodeValue("0");
 			EntriesElement.setAttribute("value", "0");
 			//all index files are ready
 			/* Adding word index */
 			Element keywordsElement = xmlDoc.createElement("keywords");
 
 			rootElement.appendChild(EntriesElement);
 			rootElement.appendChild(headerElement);
 			rootElement.appendChild(filesElement);
 			rootElement.appendChild(keywordsElement);
 
 			/* Serialization */
 			DOMSource domSource = new DOMSource(xmlDoc);
 			TransformerFactory transformFactory = TransformerFactory.newInstance();
 			Transformer serializer;
 
 			try {
 				serializer = transformFactory.newTransformer();
 			} catch (javax.xml.transform.TransformerConfigurationException e) {
 				Logger.error(this, "Spider: Error while serializing XML (transformFactory.newTransformer()): "
 				        + e.toString(), e);
 				return;
 			}
 
 
 			serializer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
 			serializer.setOutputProperty(OutputKeys.INDENT, "yes");
 
 			/* final step */
 			try {
 				serializer.transform(domSource, resultStream);
 			} catch (javax.xml.transform.TransformerException e) {
 				Logger.error(this, "Spider: Error while serializing XML (transform()): " + e.toString(), e);
 				return;
 			}
 		} finally {
 			try {
 				fos.close();
 			} catch (IOException e) {
 				// Ignore
 			}
 		}
 
 		if(Logger.shouldLog(Logger.MINOR, this))
 			Logger.minor(this, "Spider: indexes regenerated.");
 	}
 
 	public void terminate(){
 		synchronized (this) {
 			stopped = true;
 			for (Map.Entry<Page, ClientGetter> me : runningFetch.entrySet()) {
 				me.getValue().cancel();
 			}
 			runningFetch.clear();
 		}
 	}
 
 	public void runPlugin(PluginRespirator pr){
 		this.core = pr.getNode().clientCore;
 
 		this.pageMaker = pr.getPageMaker();
 		pageMaker.addNavigationLink("/plugins/plugins.XMLSpider.XMLSpider", "Home", "Home page", false, null);
 		pageMaker.addNavigationLink("/plugins/", "Plugins page", "Back to Plugins page", false, null);
 
 		/* Initialize Fetch Context */
 		this.ctx = pr.getHLSimpleClient().getFetchContext();
 		ctx.maxSplitfileBlockRetries = 2;
 		ctx.maxNonSplitfileRetries = 2;
 		ctx.maxTempLength = 2 * 1024 * 1024;
 		ctx.maxOutputLength = 2 * 1024 * 1024;
 		allowedMIMETypes = new HashSet<String>();
 		allowedMIMETypes.add("text/html");
 		allowedMIMETypes.add("text/plain");
 		allowedMIMETypes.add("application/xhtml+xml");
 		ctx.allowedMIMETypes = new HashSet<String>(allowedMIMETypes);
 
 		tProducedIndex = 0;
 		stopped = false;
 
 		if (!new File(DEFAULT_INDEX_DIR).mkdirs()) {
 			Logger.error(this, "Could not create default index directory ");
 		}
 
 		// Initial DB4O
 		db = initDB4O();
 
 		// Find max Page ID
 		{
 			Query query = db.query();
 			query.constrain(Page.class);
 			query.descend("id").orderDescending();
 			ObjectSet<Page> set = query.execute();
 			if (set.hasNext())
 				maxPageId = new AtomicLong(set.next().id);
 			else
 				maxPageId = new AtomicLong(0);
 		}
 
 		pr.getNode().executor.execute(new Runnable() {
 			public void run() {
 				try{
 					Thread.sleep(30 * 1000); // Let the node start up
 				} catch (InterruptedException e){}
 				startSomeRequests();
 			}
 		}, "Spider Plugin Starter");
 	}
 
 	private long getPageCount(Status status) {
 		Query query = db.query();
 		query.constrain(Page.class);
 		query.descend("status").constrain(status);
 		ObjectSet<Page> set = query.execute();
 
 		return set.size();
 	}
 
 	private void listPage(Status status, HTMLNode parent) {
 		Query query = db.query();
 		query.constrain(Page.class);
 		query.descend("status").constrain(status);
 		query.descend("lastChange").orderDescending();
 		ObjectSet<Page> set = query.execute();
 
 		if (set.isEmpty()) {
 			HTMLNode list = parent.addChild("#", "NO URI");
 		} else {
 			HTMLNode list = parent.addChild("ol", "style", "overflow: auto; white-space: nowrap;");
 
 			for (int i = 0; i < maxShownURIs && set.hasNext(); i++) {
 				Page page = set.next();
 				HTMLNode litem = list.addChild("li", "title", page.comment);
 				litem.addChild("a", "href", "/freenet:" + page.uri, page.uri);
 			}
 		}
 	}
 
 	/**
 	 * Interface to the Spider data
 	 */
 	public String handleHTTPGet(HTTPRequest request) throws PluginHTTPException {
 		HTMLNode pageNode = pageMaker.getPageNode(pluginName, null);
 		HTMLNode contentNode = pageMaker.getContentNode(pageNode);
 
 		return generateHTML(request, pageNode, contentNode);
 	}
 
 	public String handleHTTPPost(HTTPRequest request) throws PluginHTTPException {
 		HTMLNode pageNode = pageMaker.getPageNode(pluginName, null);
 		HTMLNode contentNode = pageMaker.getContentNode(pageNode);
 
 		if (request.isPartSet("createIndex")) {
 			synchronized (this) {
 				if (!writingIndex) {
 					scheduleMakeIndex();
 
 					HTMLNode infobox = pageMaker.getInfobox("infobox infobox-success", "Scheduled Creating Index");
 					infobox.addChild("#", "Index will start create soon.");
 					contentNode.addChild(infobox);
 				}
 			}
 		}
 
 		String addURI = request.getPartAsString("addURI", 512);
 		if (addURI != null && addURI.length() != 0) {
 			try {
 				FreenetURI uri = new FreenetURI(addURI);
 				queueURI(uri, "manually", true);
 
 				HTMLNode infobox = pageMaker.getInfobox("infobox infobox-success", "URI Added");
 				infobox.addChild("#", "Added " + uri);
 				contentNode.addChild(infobox);
 			} catch (Exception e) {
 				HTMLNode infobox = pageMaker.getInfobox("infobox infobox-error", "Error adding URI");
 				infobox.addChild("#", e.getMessage());
 				contentNode.addChild(infobox);
 				Logger.normal(this, "Manual added URI cause exception", e);
 			}
 
 			startSomeRequests();
 		}
 
 		return generateHTML(request, pageNode, contentNode);
 	}
 
 	private String generateHTML(HTTPRequest request, HTMLNode pageNode, HTMLNode contentNode) {
 		HTMLNode overviewTable = contentNode.addChild("table", "class", "column");
 		HTMLNode overviewTableRow = overviewTable.addChild("tr");
 
 		// Column 1
 		HTMLNode nextTableCell = overviewTableRow.addChild("td", "class", "first");
 		HTMLNode statusBox = pageMaker.getInfobox("Spider Status");
 		HTMLNode statusContent = pageMaker.getContentNode(statusBox);
 		statusContent.addChild("#", "Running Request: " + runningFetch.size() + "/" + maxParallelRequests);
 		statusContent.addChild("br");
 		statusContent.addChild("#", "Queued: " + getPageCount(Status.QUEUED));
 		statusContent.addChild("br");
 		statusContent.addChild("#", "Succeeded: " + getPageCount(Status.SUCCEEDED));
 		statusContent.addChild("br");
 		statusContent.addChild("#", "Failed: " + getPageCount(Status.FAILED));
 		statusContent.addChild("br");
 		statusContent.addChild("br");
 		statusContent.addChild("#", "Index Writer: ");
 		synchronized (this) {
 			if (writingIndex)
 				statusContent.addChild("span", "style", "color: red; font-weight: bold;", "RUNNING");
 			else
 				statusContent.addChild("span", "style", "color: green; font-weight: bold;", "IDLE");
 		}
 		statusContent.addChild("br");
 		statusContent.addChild("#", "Last Written: "
 				+ (tProducedIndex == 0 ? "NEVER" : new Date(tProducedIndex).toString()));
 		nextTableCell.addChild(statusBox);
 
 		// Column 2
 		nextTableCell = overviewTableRow.addChild("td", "class", "second");
 		HTMLNode mainBox = pageMaker.getInfobox("Main");
 		HTMLNode mainContent = pageMaker.getContentNode(mainBox);
 		HTMLNode addForm = mainContent.addChild("form", //
 				new String[] { "action", "method" }, //
 		        new String[] { "plugins.XMLSpider.XMLSpider", "post" });
 		addForm.addChild("label", "for", "addURI", "Add URI:");
 		addForm.addChild("input", new String[] { "name", "style" }, new String[] { "addURI", "width: 20em;" });
 		addForm.addChild("input", //
 				new String[] { "name", "type", "value" },//
 		        new String[] { "formPassword", "hidden", core.formPassword });
 		addForm.addChild("input", "type", "submit");
 		nextTableCell.addChild(mainBox);
 
 		HTMLNode indexBox = pageMaker.getInfobox("Create Index");
 		HTMLNode indexContent = pageMaker.getContentNode(indexBox);
 		HTMLNode indexForm = indexContent.addChild("form", //
 				new String[] { "action", "method" }, //
 		        new String[] { "plugins.XMLSpider.XMLSpider", "post" });
 		indexForm.addChild("input", //
 				new String[] { "name", "type", "value" },//
 		        new String[] { "formPassword", "hidden", core.formPassword });
 		indexForm.addChild("input", //
 				new String[] { "name", "type", "value" },//
 		        new String[] { "createIndex", "hidden", "createIndex" });
 		indexForm.addChild("input", //
 				new String[] { "type", "value" }, //
 		        new String[] { "submit", "Create Index Now" });
 		nextTableCell.addChild(indexBox);
 
 		HTMLNode runningBox = pageMaker.getInfobox("Running URI");
 		runningBox.addAttribute("style", "right: 0;");
 		HTMLNode runningContent = pageMaker.getContentNode(runningBox);
 		synchronized (runningFetch) {
 			if (runningFetch.isEmpty()) {
 				HTMLNode list = runningContent.addChild("#", "NO URI");
 			} else {
 				HTMLNode list = runningContent.addChild("ol", "style", "overflow: auto; white-space: nowrap;");
 
 				Iterator<Page> pi = runningFetch.keySet().iterator();
 				for (int i = 0; i < maxShownURIs && pi.hasNext(); i++) {
 					Page page = pi.next();
 					HTMLNode litem = list.addChild("li", "title", page.comment);
 					litem.addChild("a", "href", "/freenet:" + page.uri, page.uri);
 				}
 			}
 		}
 		contentNode.addChild(runningBox);
 
 		HTMLNode queuedBox = pageMaker.getInfobox("Queued URI");
 		queuedBox.addAttribute("style", "right: 0; overflow: auto;");
 		HTMLNode queuedContent = pageMaker.getContentNode(queuedBox);
 		listPage(Status.QUEUED, queuedContent);
 		contentNode.addChild(queuedBox);
 
 		HTMLNode succeededBox = pageMaker.getInfobox("Succeeded URI");
 		succeededBox.addAttribute("style", "right: 0;");
 		HTMLNode succeededContent = pageMaker.getContentNode(succeededBox);
 		listPage(Status.SUCCEEDED, succeededContent);
 		contentNode.addChild(succeededBox);
 
 		HTMLNode failedBox = pageMaker.getInfobox("Failed URI");
 		failedBox.addAttribute("style", "right: 0;");
 		HTMLNode failedContent = pageMaker.getContentNode(failedBox);
 		listPage(Status.FAILED, failedContent);
 		contentNode.addChild(failedBox);
 
 		return pageNode.generate();
 	}
 
 	/**
 	 * creates the callback object for each page.
 	 *<p>Used to create inlinks and outlinks for each page separately.
 	 * @author swati
 	 *
 	 */
 	public class PageCallBack implements FoundURICallback{
 		final Page page;
 
 		PageCallBack(Page page) {
 			this.page = page;
 		}
 
 		public void foundURI(FreenetURI uri){
 			// Ignore
 		}
 
 		public void foundURI(FreenetURI uri, boolean inline){
 			Logger.debug(this, "foundURI " + uri + " on " + page);
 			queueURI(uri, "Added from " + page.uri, false);
 		}
 
 		Integer lastPosition = null;
 
 		public void onText(String s, String type, URI baseURI){
 			Logger.debug(this, "onText on " + page.id + " (" + baseURI + ")");
 
 			if ("title".equalsIgnoreCase(type) && (s != null) && (s.length() != 0) && (s.indexOf('\n') < 0)) {
 				/*
 				 * title of the page 
 				 */
 				page.pageTitle = s;
 				type = "title";
 			}
 			else type = null;
 			/*
 			 * determine the position of the word in the retrieved page
 			 * FIXME - replace with a real tokenizor
 			 */
 			String[] words = s.split("[^\\p{L}\\{N}]");
 
 			if(lastPosition == null)
 				lastPosition = 1; 
 			for (int i = 0; i < words.length; i++) {
 				String word = words[i];
 				if ((word == null) || (word.length() == 0))
 					continue;
 				word = word.toLowerCase();
 				word = word.intern();
 				try{
 					if(type == null)
 						addWord(word, lastPosition.intValue() + i);
 					else
 						addWord(word, -1 * (i + 1));
 				}
 				catch (Exception e){}
 			}
 
 			if(type == null) {
 				lastPosition = lastPosition + words.length;
 			}
 		}
 
 		private void addWord(String word, int position) throws Exception {
 			if (word.length() < 3)
 				return;
 			Term term = getTermByWord(word, true);
 			TermPosition termPos = getTermPosition(term, page, true);
 
 			synchronized (termPos) {
 				int[] newPositions = new int[termPos.positions.length + 1];
 				System.arraycopy(termPos.positions, 0, newPositions, 0, termPos.positions.length);
 				newPositions[termPos.positions.length] = position;
 
 				termPos.positions = newPositions;
 				db.store(termPos);						
 			}
 
 			mustWriteIndex = true;
 		}
 	}
 
 	private boolean mustWriteIndex = false;
 	private boolean writingIndex;
 
 	public void makeIndex() throws Exception {
 		synchronized(this) {
 			if (writingIndex || stopped)
 				return;
 
 			db.commit();
 			writingIndex = true;
 		}
 
 		try {
 			synchronized(this) {
 				if(!mustWriteIndex) {
 					Logger.minor(this, "Not making index, no data added since last time");
 					return;
 				}
 				mustWriteIndex = false;
 			}
 			time_taken = System.currentTimeMillis();
 
 			makeSubIndices();
 			makeMainIndex();
 
 			time_taken = System.currentTimeMillis() - time_taken;
 
 			Logger.minor(this, "Spider: indexes regenerated - tProducedIndex="
 					+ (System.currentTimeMillis() - tProducedIndex) + "ms ago time taken=" + time_taken + "ms");
 
 			tProducedIndex = System.currentTimeMillis();
 		} finally {
 			synchronized (this) {
 				writingIndex = false;
 				notifyAll();
 			}
 		}
 	}
 
 	private void scheduleMakeIndex() {
 		core.getTicker().queueTimedJob(new PrioRunnable() {
 			public void run() {
 				try {
 					makeIndex();
 				} catch (Exception e) {
 					Logger.error(this, "Could not generate index: "+e, e);
 				}
 			}
 
 			public int getPriority() {
 				return NativeThread.LOW_PRIORITY;
 			}
 
 		}, 1);
 	}
 
 	public void onFoundEdition(long l, USK key){
 		FreenetURI uri = key.getURI();
 		/*-
 		 * FIXME this code don't make sense 
 		 *  (1) runningFetchesByURI contain SSK, not USK
 		 *  (2) onFoundEdition always have the edition set
 		 *  
 		if(runningFetchesByURI.containsKey(uri)) runningFetchesByURI.remove(uri);
 		uri = key.getURI().setSuggestedEdition(l);
 		 */
 		queueURI(uri, "USK found edition", true);
 		startSomeRequests();
 	}
 
 	public short getPollingPriorityNormal() {
 		return (short) Math.min(RequestStarter.MINIMUM_PRIORITY_CLASS, PRIORITY_CLASS + 1);
 	}
 
 	public short getPollingPriorityProgress() {
 		return PRIORITY_CLASS;
 	}
 
 	/**
 	 * Initializes DB4O.
 	 * 
 	 * @return db4o's connector
 	 */
 	protected ObjectContainer db;
 
 	private ObjectContainer initDB4O() {
 		Configuration cfg = Db4o.newConfiguration();
 
 		//- Page
 		cfg.objectClass(Page.class).objectField("id").indexed(true);
 		cfg.objectClass(Page.class).objectField("uri").indexed(true);
 		cfg.objectClass(Page.class).objectField("status").indexed(true);
 		cfg.objectClass(Page.class).objectField("lastChange").indexed(true);		
 
 		cfg.objectClass(Page.class).callConstructor(true);
 
 		//- Term
 		cfg.objectClass(Term.class).objectField("md5").indexed(true);
 		cfg.objectClass(Term.class).objectField("word").indexed(true);
 
 		cfg.objectClass(Term.class).callConstructor(true);
 
 		//- TermPosition
 		cfg.objectClass(TermPosition.class).objectField("pageId").indexed(true);
 		cfg.objectClass(TermPosition.class).objectField("word").indexed(true);
 
 		cfg.objectClass(TermPosition.class).callConstructor(true);
 
 		//- Other
 		cfg.activationDepth(1);
 		cfg.updateDepth(1);
		cfg.queries().evaluationMode(QueryEvaluationMode.SNAPSHOT);
 		cfg.diagnostic().addListener(new DiagnosticToConsole());
 
 		ObjectContainer oc = Db4o.openFile(cfg, "XMLSpider-" + version + ".db4o");
 
 		return oc;
 	}
 
 	protected Page getPageByURI(FreenetURI uri) {
 		Query query = db.query();
 		query.constrain(Page.class);
 		query.descend("uri").constrain(uri.toString());
 		ObjectSet<Page> set = query.execute();
 
 		if (set.hasNext())
 			return set.next();
 		else
 			return null;
 	}
 
 	protected Page getPageById(long id) {
 		Query query = db.query();
 		query.constrain(Page.class);
 		query.descend("id").constrain(id);
 		ObjectSet<Page> set = query.execute();
 
 		if (set.hasNext())
 			return set.next();
 		else
 			return null;
 	}
 
 	protected Term getTermByMd5(String md5) {
 		Query query = db.query();
 		query.constrain(Term.class);
 		query.descend("md5").constrain(md5);
 		ObjectSet<Term> set = query.execute();
 
 		if (set.hasNext())
 			return set.next();
 		else
 			return null;
 	}
 
 	protected Term getTermByWord(String word, boolean create) {
 		synchronized (this) {
 			Query query = db.query();
 			query.constrain(Term.class);
 			query.descend("word").constrain(word);
 			ObjectSet<Term> set = query.execute();
 
 			if (set.hasNext())
 				return set.next();
 			else if (create) {
 				Term term = new Term(word);
 				db.store(term);
 				return term;
 			} else
 				return null;
 		}
 	}
 
 	protected TermPosition getTermPosition(Term term, Page page, boolean create) {
 		synchronized (term) {
 			synchronized (page) {
 				Query query = db.query();
 				query.constrain(TermPosition.class);
 
 				query.descend("word").constrain(term.word);
 				query.descend("pageId").constrain(page.id);
 				ObjectSet<TermPosition> set = query.execute();
 
 				if (set.hasNext()) {
 					return set.next();
 				} else if (create) {
 					TermPosition termPos = new TermPosition();
 					termPos.word = term.word;
 					termPos.pageId = page.id;
 					termPos.positions = new int[0];
 
 					db.store(termPos);
 					return termPos;
 				} else {
 					return null;
 				}
 			}
 		}
 	}
 }
