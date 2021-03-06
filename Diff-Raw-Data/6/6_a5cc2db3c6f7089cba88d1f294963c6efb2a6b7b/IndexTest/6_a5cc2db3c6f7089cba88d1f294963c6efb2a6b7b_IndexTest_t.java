 package org.solrmarc.testUtils;
 
 import static org.junit.Assert.*;
 import static org.solrmarc.testUtils.IndexTest.logger;
 
 import java.io.*;
 import java.net.MalformedURLException;
 import java.net.URL;
 import java.util.*;
 
 import javax.xml.parsers.ParserConfigurationException;
 
 import org.apache.log4j.*;
 import org.apache.solr.client.solrj.*;
 import org.apache.solr.client.solrj.impl.CommonsHttpSolrServer;
 import org.apache.solr.client.solrj.response.QueryResponse;
 import org.apache.solr.common.*;
 import org.apache.solr.common.params.CommonParams;
 import org.solrmarc.marc.MarcImporter;
 import org.solrmarc.solr.*;
 import org.solrmarc.tools.Utils;
 import org.xml.sax.SAXException;
 
 public abstract class IndexTest
 {
 
 	protected static MarcImporter importer;
 	protected static SolrProxy solrProxy;
 	protected static SolrServer solrJSolrServer;
 	protected static SolrJettyProcess solrJettyProcess = null;
 
 	protected static String docIDfname = "id";
 
 	static Logger logger = Logger.getLogger(IndexTest.class.getName());
 
 	protected String testDataParentPath;
 	protected String testConfigFname;
 	protected String testSolrUrl;
 
 	protected boolean useBinaryRequestHandler = Boolean.valueOf(System.getProperty("core.test.use_streaming_proxy"));
 	protected boolean useStreamingProxy = Boolean.valueOf(System.getProperty("core.test.use_binary_request_handler"));
 	protected static String testSolrLogLevel = System.getProperty("test.solr.log.level");
 	protected static String testSolrMarcLogLevel = System.getProperty("test.solrmarc.log.level");
 
 	/**
 	 * initializes the properties used to create an index over http
 	 */
 	protected void initVarsForHttpTestIndexing()
 	{
 		testDataParentPath = getRequiredSystemProperty("test.data.path");
 		testConfigFname = getRequiredSystemProperty("test.config.file");
 		testSolrUrl = getRequiredSystemProperty("test.solr.url");
 	}
 
 	/**
 	 * Creates a pristine Solr index from the indicated test file of marc
 	 * records. Uses a bunch of class instance variables.  Sends commit
 	 * 
 	 * @param marcTestDataFname - file of marc records to be indexed. should end in ".mrc", "marc" or ".xml"
 	 */
 	protected void createFreshTestIxOverHTTP(String marcTestDataFname)
 			throws ParserConfigurationException, IOException, SAXException
 	{
 		createFreshTestIxOverHTTP(testConfigFname, testSolrUrl,	useBinaryRequestHandler, useStreamingProxy, 
 									testDataParentPath,	marcTestDataFname);
 	}
 
 	/**
 	 * Create a pristine Solr index from the marc file, and send a commit.
 	 * 
 	 * @param confPropFilename - name of config.properties file
 	 * @param testSolrUrl - url for test solr instances, as a string
 	 * @param useBinaryRequestHandler - true to use the binary request handler
 	 * @param useStreamingProxy - true to use streaming proxy (multiple records added at a time)
 	 * @param testDataParentPath - directory containing the test data file
 	 * @param marcTestDataFname - file of marc records to be indexed. should end in ".mrc", "marc" or ".xml"
 	 */
 	public void createFreshTestIxOverHTTP(String configPropFilename, String testSolrUrl, 
 										boolean useBinaryRequestHandler, boolean useStreamingProxy, 
 										String testDataParentPath, String marcTestDataFname) 
 			throws ParserConfigurationException, IOException, SAXException
 	{
 		createFreshTestIxOverHTTPNoCommit(testConfigFname, testSolrUrl, useBinaryRequestHandler, useStreamingProxy, 
 											testDataParentPath,	marcTestDataFname);
 		solrProxy.commit(false); // don't optimize
 	}
 
 	/**
 	 * Creates a pristine Solr index from the indicated test file of marc
 	 * records, but doesn't commit. Uses a bunch of class instance variables.
 	 * 
 	 * @param marcTestDataFname - file of marc records to be indexed. should end in ".mrc", "marc" or ".xml"
 	 */
 	protected void createFreshTestIxOverHTTPNoCommit(String marcTestDataFname)
 			throws ParserConfigurationException, IOException, SAXException
 	{
 		createFreshTestIxOverHTTPNoCommit(testConfigFname, testSolrUrl, useBinaryRequestHandler, useStreamingProxy, 
 											testDataParentPath,	marcTestDataFname);
 	}
 
 	/**
 	 * Create a pristine Solr index from the marc file, but don't send commit.
 	 * 
 	 * @param confPropFilename - name of config.properties file
 	 * @param testSolrUrl - url for test solr instances, as a string
 	 * @param useBinaryRequestHandler - true to use the binary request handler
 	 * @param useStreamingProxy - true to use streaming proxy (multiple records added at a time)
 	 * @param testDataParentPath - directory containing the test data file
 	 * @param marcTestDataFname - file of marc records to be indexed. should end in ".mrc", "marc" or ".xml"
 	 */
 	public void createFreshTestIxOverHTTPNoCommit(String configPropFilename, String testSolrUrl, 
 												boolean useBinaryRequestHandler, boolean useStreamingProxy, 
 												String testDataParentPath, String marcTestDataFname) 
 			throws ParserConfigurationException, IOException, SAXException
 	{
 		solrProxy = SolrCoreLoader.loadRemoteSolrServer(testSolrUrl + "/update", useBinaryRequestHandler, useStreamingProxy);
 		logger.debug("just set solrProxy to remote server at "	+ testSolrUrl + " - " + solrProxy.toString());
 		solrJSolrServer = ((SolrServerProxy) solrProxy).getSolrServer();
 
 		solrProxy.deleteAllDocs();
 		solrProxy.commit(false); // don't optimize
 		logger.debug("just deleted all docs known to the solrProxy");
 
 		importer = new MarcImporter(solrProxy);
 		importer.init(new String[] { configPropFilename,
 				testDataParentPath + File.separator + marcTestDataFname });
 		int numImported = importer.importRecords();
 	}
 
 	/**
 	 * Updates the Solr index from the indicated test file of marc records, and
 	 * initializes necessary variables. Uses a bunch of class instance variables
 	 * 
 	 * @param marcTestDataFname - file of marc records to be indexed. should end in ".mrc", "marc" or ".xml"
 	 */
 	protected void updateTestIxOverHTTP(String marcTestDataFname)
 			throws ParserConfigurationException, IOException, SAXException
 	{
 		updateTestIxOverHTTP(testConfigFname, testSolrUrl, useBinaryRequestHandler, useStreamingProxy, 
 							testDataParentPath,	marcTestDataFname);
 	}
 
 	/**
 	 * Updates the Solr index from the marc file.
 	 * 
 	 * @param confPropFilename - name of config.properties file
 	 * @param testSolrUrl - url for test solr instances, as a string
 	 * @param useBinaryRequestHandler - true to use the binary request handler
 	 * @param useStreamingProxy - true to use streaming proxy (multiple records added at a time)
 	 * @param testDataParentPath - directory containing the test data file
 	 * @param marcTestDataFname - file of marc records to be indexed. should end in ".mrc", "marc" or ".xml"
 	 */
 	public void updateTestIxOverHTTP(String configPropFilename,	String testSolrUrl, 
 									boolean useBinaryRequestHandler, boolean useStreamingProxy, 
 									String testDataParentPath, String marcTestDataFname) 
 			throws ParserConfigurationException, IOException, SAXException
 	{
 		logger.debug("solrProxy for " + testSolrUrl + " starting as - " + (solrProxy == null ? "null" : solrProxy.toString()));
 		if (solrProxy == null)
 		{
 			solrProxy = SolrCoreLoader.loadRemoteSolrServer(testSolrUrl	+ "/update", useBinaryRequestHandler, useStreamingProxy);
 			logger.debug("just set solrProxy to remote solr server at "	+ testSolrUrl + " - " + solrProxy.toString());
 			solrJSolrServer = ((SolrServerProxy) solrProxy).getSolrServer();
 			logger.debug("just set solrJSolrServer to "	+ solrJSolrServer.toString());
 		}
 
 		if (importer == null)
 			importer = new MarcImporter(solrProxy);
 
 		importer.init(new String[] { configPropFilename, testDataParentPath + File.separator + marcTestDataFname });
 		int numImported = importer.importRecords();
 		solrProxy.commit(false); // don't optimize
 	}
 
 	/**
 	 * Given the paths to a marc file to be indexed, the solr directory, and the
 	 * path for the solr index, create the index from the marc file.
 	 * 
 	 * @param confPropFilename
 	 *            - name of config.properties file
 	 * @param solrPath
 	 *            - the directory holding the solr instance (think conf files)
 	 * @param solrDataDir
 	 *            - the data directory to hold the index
 	 * @param testDataParentPath
 	 *            - directory containing the test data file
 	 * @param testDataFname
 	 *            - file of marc records to be indexed. should end in ".mrc"
 	 *            "marc" or ".xml"
 	 * @deprecated
 	 */
 	public void createIxInitVarsOld(String configPropFilename, String solrPath,
 			String solrDataDir, String testDataParentPath, String testDataFname)
 			throws ParserConfigurationException, IOException, SAXException
 	{
 		setSolrSysProperties(solrPath, solrDataDir);
 
 		// delete old index files
 		logger.debug("System.getProperty(\"os.name\") : "
 				+ System.getProperty("os.name"));
 		if (!System.getProperty("os.name").toLowerCase().contains("win"))
 		{
 			logger.info("Calling Delete Dir Contents");
 			Utils.deleteDirContents(System.getProperty("solr.data.dir"));
 		}
 		else
 		{
 			logger.info("Calling Delete All Docs");
 			importer.getSolrProxy().deleteAllDocs();
 		}
 		setupMarcImporter(configPropFilename, testDataParentPath
 				+ File.separator + testDataFname);
 		int numImported = importer.importRecords();
 		importer.finish();
 
 		solrProxy = importer.getSolrProxy();
 		solrProxy.commit(false);
 		solrJSolrServer = ((SolrServerProxy) solrProxy).getSolrServer();
 	}
 
 	/**
 	 * Set the appropriate system properties for Solr processing
 	 * 
 	 * @param solrPath
 	 *            - the directory holding the solr instance (think solr/conf
 	 *            files)
 	 * @param solrDataDir
 	 *            - the data directory to hold the index
 	 * @deprecated
 	 */
 	private void setSolrSysProperties(String solrPath, String solrDataDir)
 	{
 		if (solrPath != null)
 		{
 			System.setProperty("solr.path", solrPath);
 			if (solrDataDir == null)
 				solrDataDir = solrPath + File.separator + "data";
 
 			System.setProperty("solr.data.dir", solrDataDir);
 		}
 		if (!Boolean.parseBoolean(System.getProperty("test.solr.verbose")))
 		{
 			java.util.logging.Logger.getLogger("org.apache.solr").setLevel(
 					java.util.logging.Level.SEVERE);
 			Utils.setLog4jLogLevel(org.apache.log4j.Level.WARN);
 		}
 		// from DistSMCreateIxInitVars ... which only creates the smoketest
 		// if (!Boolean.parseBoolean(System.getProperty("test.solr.verbose")))
 		// {
 		// addnlProps.put("solr.log.level", "OFF");
 		// addnlProps.put("solrmarc.log.level", "OFF");
 		// }
 	}
 
 	/**
 	 * Given the paths to a marc file to be indexed, the solr directory, and the
 	 * path for the solr index, instantiate the MarcImporter object
 	 * 
 	 * @param confPropFilename
 	 *            - name of config.properties file (must include ".properties"
 	 *            on the end)
 	 * @param argFileName
 	 *            - the name of a file to be processed by the MarcImporter;
 	 *            should end in "marc" or ".mrc" or ".xml" or ".del", or be null
 	 *            (or the string "NONE") if there is no such file. (All this per
 	 *            MarcHandler constructor)
 	 * @deprecated
 	 */
 	private void setupMarcImporter(String configPropFilename, String argFileName)
 			throws ParserConfigurationException, IOException, SAXException
 	{
 		if (argFileName == null)
 			argFileName = "NONE";
 
 		importer = new MarcImporter();
 		if (configPropFilename != null)
 			importer.init(new String[] { configPropFilename, argFileName });
 		else
 			importer.init(new String[] { argFileName });
 	}
 
 	/**
 	 * Given the paths to a marc file to be indexed, the solr directory, and the
 	 * path for the solr index, create the index from the marc file.
 	 * 
 	 * @param confPropFilename
 	 *            - name of config.properties file
 	 * @param solrPath
 	 *            - the directory holding the solr instance (think conf files)
 	 * @param solrDataDir
 	 *            - the data directory to hold the index
 	 * @param testDataParentPath
 	 *            - directory containing the test data file
 	 * @param testDataFname
 	 *            - file of marc records to be indexed. should end in ".mrc"
 	 *            "marc" or ".xml"
 	 */
 	public void updateIx(String configPropFilename, String solrPath,
 			String solrDataDir, String testDataParentPath, String testDataFname)
 			throws ParserConfigurationException, IOException, SAXException
 	{
 		setSolrSysProperties(solrPath, solrDataDir);
 		setupMarcImporter(configPropFilename, testDataParentPath
 				+ File.separator + testDataFname);
 		int numImported = importer.importRecords();
 		// FIXME: Naomi doesn't think this will work for remote server debugging
 		importer.finish();
 
 		solrProxy = (SolrProxy) importer.getSolrProxy();
 		solrProxy.commit(false);
 		solrJSolrServer = ((SolrServerProxy) solrProxy).getSolrServer();
 	}
 
 	/**
 	 * Given the paths to a marc file to be indexed, the solr directory, and the
 	 * path for the solr index, delete the records from the index.
 	 * 
 	 * @param confPropFilename
 	 *            - name of config.properties file
 	 * @param solrPath
 	 *            - the directory holding the solr instance (think conf files)
 	 * @param solrDataDir
 	 *            - the data directory to hold the index
 	 * @param deletedIdsFilename
 	 *            - file containing record ids to be deleted (including parent
 	 *            path)
 	 * @deprecated
 	 */
 	public void deleteRecordsFromIx(String configPropFilename, String solrPath,
 			String solrDataDir, String deletedIdsFilename)
 			throws ParserConfigurationException, IOException, SAXException
 	{
 		setSolrSysProperties(solrPath, solrDataDir);
 		if (deletedIdsFilename != null)
 			System.setProperty("marc.ids_to_delete", deletedIdsFilename);
 		setupMarcImporter(configPropFilename, deletedIdsFilename);
 
 		int numDeleted = importer.deleteRecords();
 		// FIXME: Naomi doesn't think this will work for remote server debugging
 		importer.finish();
 
 		solrProxy = importer.getSolrProxy();
 		solrProxy.commit(false);
 		solrJSolrServer = ((SolrServerProxy) solrProxy).getSolrServer();
 	}
 
 	/**
 	 * Given the paths to a marc file to be indexed, the solr directory, and the
 	 * path for the solr index, create the index from the marc file.
 	 * 
 	 * @param confPropFilename
 	 *            - name of config.properties file
 	 * @param solrPath
 	 *            - the directory holding the solr instance (think conf files)
 	 * @param solrDataDir
 	 *            - the data directory to hold the index
 	 * @param testDataParentPath
 	 *            - directory containing the test data file
 	 * @param testDataFname
 	 *            - file of marc records to be indexed. should end in ".mrc"
 	 *            "marc" or ".xml"
 	 * @deprecated
 	 */
 	public void createIxInitVarsDistSM2_3_1(String configPropFilename,
 			String solrPath, String solrDataDir, String testDataParentPath,
 			String testDataFname)
 	{
 		// System.err.println("test.solr.verbose = " +
 		// System.getProperty("test.solr.verbose"));
 		// if (!Boolean.parseBoolean(System.getProperty("test.solr.verbose")))
 		// {
 		// java.util.logging.Logger.getLogger("org.apache.solr").setLevel(java.util.logging.Level.SEVERE);
 		// Utils.setLog4jLogLevel(org.apache.log4j.Level.WARN);
 		// }
 		// addnlProps = new LinkedHashMap<String, String>();
 		// backupProps = new LinkedHashMap<String, String>();
 		// allOrigProps = new LinkedHashMap<String, String>();
 		// CommandLineUtils.checkpointProps(allOrigProps);
 		//
 		// if (solrPath != null)
 		// {
 		// addnlProps.put("solr.path", solrPath);
 		// // if (solrDataDir == null)
 		// // {
 		// // solrDataDir = solrPath + File.separator + "data";
 		// // }
 		// // addnlProps.put("solr.data.dir", solrDataDir);
 		// }
 		logger.debug("System.getProperty(\"os.name\") : "
 				+ System.getProperty("os.name"));
 		// if (!System.getProperty("os.name").toLowerCase().contains("win"))
 		// {
 		// // comment out these two lines since if the solr data dir is set the
 		// same as the solr home, the conf directory would be deleted as well.
 		// // for that matter, if the solr data dir is accidently pointed at a
 		// valued directory, that directory, and all of its children, would be
 		// wiped out.
 		// // logger.info("Calling Delete Dir Contents");
 		// // deleteDirContents(solrDataDir);
 		// }
 		// index a small set of records (actually one record)
 		ByteArrayOutputStream out1 = new ByteArrayOutputStream();
 		ByteArrayOutputStream err1 = new ByteArrayOutputStream();
 		Map<String, String> addnlProps = new LinkedHashMap<String, String>();
 		addnlProps.put("solr.path", solrPath);
 		if (solrDataDir != null)
 		{
 			addnlProps.put("solr.data.dir", solrDataDir);
 		}
 		if (!Boolean.parseBoolean(System.getProperty("test.solr.verbose")))
 		{
 			addnlProps.put("solr.log.level", "OFF");
 			addnlProps.put("solrmarc.log.level", "OFF");
 		}
 
 		CommandLineUtils.runCommandLineUtil("org.solrmarc.marc.MarcImporter",
 				"main", null, out1, err1, new String[] { configPropFilename,
 						testDataParentPath + File.separator + testDataFname },
 				addnlProps);
 		solrProxy = SolrCoreLoader.loadEmbeddedCore(solrPath, solrDataDir,
 				null, false, logger);
 		solrJSolrServer = ((SolrServerProxy) solrProxy).getSolrServer();
 
 		// CommandLineUtils.addProps(addnlProps, backupProps);
 		// importer = new MarcImporter();
 		// if (configPropFilename != null)
 		// {
 		// importer.init(new String[]{configPropFilename, testDataParentPath +
 		// File.separator + testDataFname});
 		// }
 		// else
 		// {
 		// importer.init(new String[]{testDataParentPath + File.separator +
 		// testDataFname});
 		// }
 		// if (System.getProperty("os.name").toLowerCase().contains("win"))
 		// {
 		// logger.info("Calling Delete All Docs");
 		// importer.getSolrProxy().deleteAllDocs();
 		// }
 		//
 		// int numImported = importer.importRecords();
 		// importer.finish();
 		//
 		// solrProxy = (SolrCoreProxy)importer.getSolrProxy();
 		// solrCoreProxy.commit(false);
 		// searcherProxy = new SolrSearcherProxy(solrCoreProxy);
 	}
 
 	/**
 	 * close and set solrProxy to null
 	 */
 // @After
 	public static void closeSolrProxy()
 	{
 		// avoid "already closed" exception
 		logger.debug("IndexTest closing solrProxy and setting to null");
 		if (solrProxy != null)
 		{
 			logger.info("Closing solrProxy");
 			solrProxy.close();
 			solrProxy = null;
 		}
 	}
 
 	/**
 	 * assert there is a single doc in the index with the value indicated
 	 * 
 	 * @param docId - the identifier of the SOLR/Lucene document
 	 * @param fldname - the field to be searched
 	 * @param fldVal - field value to be found
 	 */
 	public final void assertSingleResult(String docId, String fldName,
 			String fldVal) throws ParserConfigurationException, SAXException,
 			IOException
 	{
 		SolrDocumentList sdl = getDocList(fldName, fldVal);
 		if (sdl.size() == 1)
 		{
 			SolrDocument doc = sdl.get(0);
 			Object field = doc.getFieldValue(docIDfname);
 			if (field.toString().equals(docId))
 				return;
 			fail("There is a single doc with " + fldName + " of " + fldVal + " but it is not doc \"" + docId + "\"");
 		}
 		if (sdl.size() == 0)
 			fail("There is no doc with " + fldName + " of " + fldVal);
 		if (sdl.size() > 1)
 			fail("There is more than 1 doc with " + fldName + " of " + fldVal);
 	}
 
 	public final void assertZeroResults(String fldName, String fldVal)
 	{
 		assertResultSize(fldName, fldVal, 0);
 	}
 
 	/**
 	 * Get the Lucene document with the given id from the solr index at the solrDataDir
 	 * 
 	 * @param doc_id - the unique id of the lucene document in the index
 	 * @return SolrDocument matching the given id
 	 */
 	public final SolrDocument getDocument(String doc_id)
 	{
 		SolrDocumentList sdl = getDocList(docIDfname, doc_id);
 		for (SolrDocument doc : sdl)
 		{
 			return (doc);
 		}
 		return (null);
 	}
 
 	/**
 	 * Get the List of Solr Documents with the given value for the given field
 	 * 
 	 * @param doc_id - the unique id of the lucene document in the index
 	 * @return org.apache.solr.common.SolrDocumentList
 	 */
 	public final SolrDocumentList getDocList(String field, String value)
 	{
 		SolrQuery query = new SolrQuery(field + ":" + value);
 		query.setQueryType("standard");
 		query.setFacet(false);
 		try
 		{
 			QueryResponse response = solrJSolrServer.query(query);
 			return (response.getResults());
 		} catch (SolrServerException e)
 		{
 			e.getCause().printStackTrace();
 			// e.printStackTrace();
 		}
 		return (new SolrDocumentList());
 	}
 
 	/**
 	 * asserts that the document is present in the index
 	 */
 	public final void assertDocPresent(String doc_id)
 	{
 		SolrDocumentList sdl = getDocList(docIDfname, doc_id);
 		assertTrue("Found no document with id \"" + doc_id + "\"", sdl.size() == 1);
 	}
 
     /**
 	 * Request record by id from Solr as JSON, and return the raw value of the 
 	 *  field (note that XML response does not preserve the raw value of the field.) 
 	 *  If the record doesn't exist id or the record doesn't contain that field return null
	 *  
	 *  NOTE:  does NOT work for retrieving binary values
	 *  
 	 *  @param desiredFld - the field from which we want the value 
 	 */
 	public String getFirstFieldValViaJSON(String id, String desiredFld)
 	{
 		SolrDocument doc = null;
 		
 		SolrQuery query = new SolrQuery(docIDfname + ":" + id);
 		query.setQueryType("standard");
 		query.setFacet(false);
 		query.setParam(CommonParams.WT, "json");
 		try
 		{
 			QueryResponse response = ((CommonsHttpSolrServer) solrJSolrServer).query(query);
 			SolrDocumentList docList = response.getResults();
 			for (SolrDocument d : docList)
 				doc = d;
 		} 
 		catch (SolrServerException e)
 		{
 			e.getCause().printStackTrace();
 			// e.printStackTrace();
 		}
 
 		if (doc == null)
 			return null;
 		Object fieldValObj = doc.getFieldValue(desiredFld);
 		if (fieldValObj.getClass() == java.lang.String.class)
 			return (String) fieldValObj;
 		
 		return null;
 	}
 
     /**
 	 * getFldValPreserveBinary - Request record by id from Solr, and return the raw
 	 *  value of the field. If the record doesn't exist id or the record
 	 * doesn't contain that field return null
 	 *  @param desiredFld - the field from which we want the value 
 	 */
 	public String getFldValPreserveBinary(String id, String desiredFld)
 	{
 		String fieldValue = null;
 		String selectStr = "select/?q=id%3A" + id + "&fl=" + desiredFld + "&rows=1&wt=json&qt=standard&facet=false";
 		try
 		{
 			InputStream is = new URL(testSolrUrl + "/" + selectStr).openStream();
 			String solrResultStr = Utils.readStreamIntoString(is);
 			String fieldLabel = "\"" + desiredFld + "\":";
 			int valStartIx = solrResultStr.indexOf(fieldLabel);
 			int valEndIx = solrResultStr.indexOf("\"}]");
 			if (valStartIx != -1 && valEndIx != -1)
 				fieldValue = solrResultStr.substring(valStartIx + fieldLabel.length(), valEndIx);
 		} 
 		catch (MalformedURLException e)
 		{
 			e.printStackTrace();
 		} 
 		catch (IOException e)
 		{
 			e.printStackTrace();
 		}
 		return (fieldValue);
 	}
         
 
 	
 	
 	/**
 	 * asserts that the document is NOT present in the index
 	 */
 	public final void assertDocNotPresent(String doc_id)
 	{
 		SolrDocumentList sdl = getDocList(docIDfname, doc_id);
 		assertTrue("Found no document with id \"" + doc_id + "\"", sdl.size() == 0);
 	}
 
 	public final void assertDocHasFieldValue(String doc_id, String fldName,	String fldVal)
 	{
 		// TODO: repeatable field vs. not ...
 		// TODO: check for single occurrence of field value, even for repeatable
 		// field
 		SolrDocumentList sdl = getDocList(docIDfname, doc_id);
 		if (sdl.size() > 0)
 		{
 			SolrDocument doc = sdl.get(0);
 			Collection<Object> fields = doc.getFieldValues(fldName);
 			for (Object field : fields)
 			{
 				if (field.toString().equals(fldVal))
 					// found field with desired value
 					return;
 			}
 			fail("Field " + fldName + " did not contain value \"" + fldVal + "\" in doc " + doc_id);
 		}
 		fail("Document " + doc_id + " was not found");
 	}
 
 	
 	public final void assertDocHasNoFieldValue(String doc_id, String fldName, String fldVal)
 	{
 		// TODO: repeatable field vs. not ...
 		// TODO: check for single occurrence of field value, even for repeatable
 		// field
 		SolrDocumentList sdl = getDocList(docIDfname, doc_id);
 		if (sdl.size() > 0)
 		{
 			SolrDocument doc = sdl.get(0);
 			Collection<Object> fields = doc.getFieldValues(fldName);
 			for (Object field : fields)
 			{
 				if (field.toString().equals(fldVal))
 					fail("Field " + fldName + " contained value \"" + fldVal + "\" in doc " + doc_id);
 			}
 			return;
 		}
 		fail("Document " + doc_id + " was not found");
 	}
 
 	@SuppressWarnings("unchecked")
 	public final void assertDocHasNoField(String doc_id, String fldName)
 	{
 		SolrDocumentList sdl = getDocList(docIDfname, doc_id);
 		if (sdl.size() > 0)
 		{
 			SolrDocument doc = sdl.get(0);
 			Collection<Object> fields = doc.getFieldValues(fldName);
 			if (fields == null || fields.size() == 0)
 				// Document has no field by that name. yay.
 				return;
 			fail("Field " + fldName + " found in doc \"" + doc_id + "\"");
 		}
 		fail("Document " + doc_id + " was not found");
 	}
 
 	/**
 	 * Do a search for the implied term query and assert the search results have
 	 * docIds that are an exact match of the set of docIds passed in
 	 * 
 	 * @param fldName - name of the field to be searched
 	 * @param fldVal - value of the field to be searched
 	 * @param docIds - Set of doc ids expected to be in the results
 	 */
 	public final void assertSearchResults(String fldName, String fldVal, Set<String> docIds)
 	{
 		SolrDocumentList sdl = getDocList(fldName, fldVal);
 
 		assertTrue("Expected " + docIds.size() + " documents for " + fldName + " search \"" + fldVal + "\" but got " + sdl.size(),
 					docIds.size() == sdl.size());
 
 		String msg = fldName + " search \"" + fldVal + "\": ";
 		for (SolrDocument doc : sdl)
 		{
 			assertDocInSet(doc, docIds, msg);
 		}
 	}
 
 	public final void assertDocInSet(SolrDocument doc, Set<String> docIds, String msgPrefix)
 	{
 		String id = doc.getFieldValue(docIDfname).toString();
 		if (docIds.contains(id))
 			return;
 		fail(msgPrefix + "doc \"" + id + "\" missing from list");
 	}
 
 	public final void assertFieldValues(String fldName, String fldVal, Set<String> docIds)
 	{
 		for (String docId : docIds)
 			assertDocHasFieldValue(docId, fldName, fldVal);
 	}
 
 	/**
 	 * ensure that the value(s) for the two fields in the document are the same
 	 * 
 	 * @param docId - the id of the document
 	 * @param fldName1 - the first field to match
 	 * @param fldName2 - the second field to match
 	 */
 	public final void assertFieldValuesEqual(String docId, String fldName1, String fldName2) 
 			throws ParserConfigurationException, SAXException, IOException
 	{
 // 		int solrDocNum = getSingleDocNum(docIDfname, docId);
 // 		DocumentProxy doc = getSearcherProxy().getDocumentProxyBySolrDocNum(solrDocNum);
 		SolrDocument doc = getDocument(docId);
 // 		String[] fld1Vals = doc.getValues(fldName1);
 // 		int numValsFld1 = fld1Vals.length;
 		Collection<Object> fldObjColl = doc.getFieldValues(fldName1);
 		int numValsFld1 = fldObjColl.size();
 		String[] fld1Vals = fldObjColl.toArray(new String[numValsFld1]);
 // 		String[] fld2Vals = doc.getValues(fldName2);
 // 		int numValsFld2 = fld2Vals.length;
 		fldObjColl = doc.getFieldValues(fldName1);
 		int numValsFld2 = fldObjColl.size();
 		String[] fld2Vals = fldObjColl.toArray(new String[numValsFld2]);
 		String errmsg = "fields " + fldName1 + ", " + fldName2	+ " have different numbers of values";
 		assertEquals(errmsg, numValsFld1, numValsFld2);
 
 		List<String> fld1ValList = Arrays.asList(fld1Vals);
 		List<String> fld2ValList = Arrays.asList(fld2Vals);
 		for (String val : fld1ValList)
 		{
 			errmsg = "In doc " + docId + ", field " + fldName1 + " has value not in " + fldName2 + ": ";
 			if (!fld2ValList.contains(val))
 				fail(errmsg + val);
 		}
 	}
 
 	/**
 	 * Assert the number of documents matching the implied term search equals
 	 * the expected number
 	 * 
 	 * @param fldName - the field to be searched
 	 * @param fldVal - field value to be found
 	 * @param numExp - the number of documents expected
 	 */
 	public final void assertResultSize(String fldName, String fldVal, int numExp)
 	{
 		int numActual = getNumMatchingDocs(fldName, fldVal);
 		assertTrue("Expected " + numExp + " documents for " + fldName + " search \"" + fldVal + "\" but got " + numActual, 
 					numActual == numExp);
 	}
 
 	/**
 	 * Given an index field name and value, return a list of Lucene Documents
 	 * that match the term query sent to the index
 	 * 
 	 * @param fld - the name of the field to be searched in the lucene index
 	 * @param value - the string to be searched in the given field
 	 * @return org.apache.solr.common.SolrDocumentList
 	 */
 	public final SolrDocumentList getAllMatchingDocs(String fld, String value)
 	{
 		return getDocList(fld, value);
 	}
 
 	/**
 	 * return the number of docs that match the implied term query
 	 * 
 	 * @param fld - the name of the field to be searched in the lucene index
 	 * @param value - the string to be searched in the given field
 	 */
 	public int getNumMatchingDocs(String fld, String value)
 	{
 		return (getDocList(fld, value).size());
 	}
 
 	/**
 	 * Given an index field name and value, return a list of Documents that
 	 * match the term query sent to the index, sorted in ascending order per the
 	 * sort fld
 	 * 
 	 * @param fld - the name of the field to be searched in the lucene index
 	 * @param valuea - the string to be searched in the given field
 	 * @param sortfld - name of the field by which results should be sorted (ascending)
 	 * @return org.apache.solr.common.SolrDocumentList
 	 */
 	public final SolrDocumentList getAscSortDocs(String fld, String value, String sortfld)
 	{
 		return getSortedDocs(fld, value, sortfld, SolrQuery.ORDER.asc);
 	}
 
 	/**
 	 * Given an index field name and value, return a list of Documents that
 	 * match the term query sent to the index, sorted in descending order per
 	 * the sort fld
 	 * 
 	 * @param fld - the name of the field to be searched in the lucene index
 	 * @param value - the string to be searched in the given field
 	 * @param sortfld - name of the field by which results should be sorted (descending)
 	 * @return org.apache.solr.common.SolrDocumentList
 	 */
 	public final SolrDocumentList getDescSortDocs(String fld, String value,	String sortfld)
 	{
 		return getSortedDocs(fld, value, sortfld, SolrQuery.ORDER.desc);
 	}
 
 	/**
 	 * Given an index field name and value, return a list of Documents that
 	 * match the term query sent to the index, sorted in descending order per
 	 * the sort fld
 	 * 
 	 * @param fld - the name of the field to be searched in the lucene index
 	 * @param value - the string to be searched in the given field
 	 * @param sortfld - name of the field by which results should be sorted (descending)
 	 * @param sortOrder = SolrQuery.ORDER.asc  or  SolrQuery.ORDER.desc
 	 * @return org.apache.solr.common.SolrDocumentList
 	 */
 	public final SolrDocumentList getSortedDocs(String fld, String value, String sortfld, SolrQuery.ORDER sortOrder)
 	{
 		SolrQuery query = new SolrQuery(fld + ":" + value);
 		query.setQueryType("standard");
 		query.setFacet(false);
 		query.setSortField(sortfld, sortOrder);
 		try
 		{
 			QueryResponse response = solrJSolrServer.query(query);
 			return (response.getResults());
 		} 
 		catch (SolrServerException e)
 		{
 			e.printStackTrace();
 			return (new SolrDocumentList());
 		}
 //		return (new SolrDocumentList());
 	}
 	
 	public static void setTestLoggingLevels()
 	{
 		setTestLoggingLevels(testSolrLogLevel, testSolrMarcLogLevel);
 	}
 
 // FIXME: move this to Utils, and also look for logging levels in config.properties
 	/**
 	 * default settings: solr: WARNING; solrmarc: WARN Solr uses
 	 * java.util.logging; level settings for solr logging: OFF, SEVERE, WARNING, INFO, FINE, FINER, FINEST, ALL 
 	 * SolrMarc uses log4j logging; level settings for solrmarc logging: OFF, FATAL, WARN, INFO, DEBUG, ALL
 	 */
 	public static void setTestLoggingLevels(String solrLogLevel, String solrmarcLogLevel)
 	{
 		java.util.logging.Level solrLevel = java.util.logging.Level.WARNING;
 
 		if (solrLogLevel != null)
 		{
 			if (solrLogLevel.equals("OFF"))	solrLevel = java.util.logging.Level.OFF;
 			if (solrLogLevel.equals("SEVERE")) solrLevel = java.util.logging.Level.SEVERE;
 			if (solrLogLevel.equals("WARNING"))	solrLevel = java.util.logging.Level.WARNING;
 			if (solrLogLevel.equals("INFO")) solrLevel = java.util.logging.Level.INFO;
 			if (solrLogLevel.equals("FINE")) solrLevel = java.util.logging.Level.FINE;
 			if (solrLogLevel.equals("FINER")) solrLevel = java.util.logging.Level.FINER;
 			if (solrLogLevel.equals("FINEST")) solrLevel = java.util.logging.Level.FINEST;
 			if (solrLogLevel.equals("ALL")) solrLevel = java.util.logging.Level.ALL;
 		}
 		java.util.logging.Logger.getLogger("org.apache.solr").setLevel(solrLevel);
 
 		org.apache.log4j.Level solrmarcLevel = org.apache.log4j.Level.WARN;
 		if (solrmarcLogLevel != null)
 		{
 			if (solrmarcLogLevel.equals("OFF")) solrmarcLevel = Level.OFF;
 			if (solrmarcLogLevel.equals("FATAL")) solrmarcLevel = Level.FATAL;
 			if (solrmarcLogLevel.equals("WARN")) solrmarcLevel = Level.WARN;
 			if (solrmarcLogLevel.equals("INFO")) solrmarcLevel = Level.INFO;
 			if (solrmarcLogLevel.equals("DEBUG")) solrmarcLevel = Level.DEBUG;
 			if (solrmarcLogLevel.equals("ALL"))	solrmarcLevel = Level.ALL;
 		}
 		Utils.setLog4jLogLevel(solrmarcLevel);
 	}
 
 	/**
 	 * @param propertyName
 	 *            the name of the System property
 	 * @return the value of the property; fail if there is no value
 	 */
 	private static String getRequiredSystemProperty(String propertyName)
 	{
 		String propVal = System.getProperty(propertyName);
 		if (propVal == null)
 			fail("property " + propertyName	+ " must be defined for the tests to run");
 		return propVal;
 	}
 
 	/**
 	 * Start a Jetty driven solr server running in a separate JVM at port
 	 * jetty.test.port
 	 */
 	public static void startTestJetty()
 	{
 		String testSolrHomeDir = getRequiredSystemProperty("test.solr.path");
 		String jettyDir = getRequiredSystemProperty("test.jetty.dir");
 		String jettyTestPortStr = getJettyPort();
 
 		solrJettyProcess = new SolrJettyProcess(testSolrHomeDir, jettyDir, jettyTestPortStr);
 		boolean serverIsUp = false;
 		try
 		{
 			serverIsUp = solrJettyProcess.startProcessWaitUntilSolrIsReady();
 		} 
 		catch (IOException e)
 		{
 			e.printStackTrace();
 			fail("Server did not become available");
 		}
 		assertTrue("Server did not become available", serverIsUp);
 
 		// If you need to see the output of the solr server after the server is up and running, call
 		// solrJettyProcess.outputReset() here to empty the buffer so the later output is visible in the Eclipse variable viewer
 		// solrJettyProcess.outputReset();
 		logger.info("Server is up and running at " + jettyDir + ", port " + solrJettyProcess.getJettyPort());
 	}
 
 	/**
 	 * Look for a value for system property test.jetty.port; if none, use 0.
 	 * 
 	 * @return the port to use for the jetty server.
 	 */
 	private static String getJettyPort()
 	{
 		String jettyTestPortStr;
 		jettyTestPortStr = System.getProperty("test.jetty.port");
 		// Specify port 0 to select any available port
 		if (jettyTestPortStr == null)
 			jettyTestPortStr = "0";
 		return jettyTestPortStr;
 	}
 
 	/**
 	 * stop the Jetty server if it is running
 	 */
 	public static void stopTestJetty()
 	{
 		if (solrJettyProcess != null && solrJettyProcess.isServerRunning())
 			solrJettyProcess.stopServer();
 	}
 
 }
