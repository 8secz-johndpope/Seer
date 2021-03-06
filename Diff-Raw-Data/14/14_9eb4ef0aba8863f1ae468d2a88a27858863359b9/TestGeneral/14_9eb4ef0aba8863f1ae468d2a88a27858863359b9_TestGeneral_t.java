 package org.collectionspace.chain.csp.persistence.file;
 
 import static org.junit.Assert.*;
 
 import java.io.ByteArrayInputStream;
 import java.io.File;
 import java.io.FileOutputStream;
 import java.io.IOException;
 import java.io.UnsupportedEncodingException;
 import java.util.HashSet;
 import java.util.Properties;
 import java.util.Set;
 
 import java.security.Security;
  
 import javax.mail.Message;
 import javax.mail.MessagingException;
 import javax.mail.PasswordAuthentication;
 import javax.mail.Session;
 import javax.mail.Transport;
 import javax.mail.internet.AddressException;
 import javax.mail.internet.InternetAddress;
 import javax.mail.internet.MimeMessage;
 
 import org.apache.commons.io.IOUtils;
 import org.collectionspace.chain.controller.ChainServlet;
 import org.collectionspace.chain.csp.persistence.file.FileStorage;
 import org.collectionspace.chain.csp.persistence.file.StubJSONStore;
 import org.collectionspace.chain.csp.persistence.services.connection.RequestMethod;
 import org.collectionspace.chain.csp.persistence.services.connection.ReturnedDocument;
 import org.collectionspace.chain.csp.persistence.services.connection.ServicesConnection;
 import org.collectionspace.chain.storage.UTF8SafeHttpTester;
 import org.collectionspace.chain.uispec.SchemaStore;
 import org.collectionspace.chain.uispec.StubSchemaStore;
 import org.collectionspace.chain.util.json.JSONUtils;
 import org.collectionspace.csp.api.core.CSPDependencyException;
 import org.collectionspace.csp.api.persistence.ExistException;
 import org.collectionspace.csp.api.persistence.UnderlyingStorageException;
 import org.collectionspace.csp.api.persistence.UnimplementedException;
 import org.dom4j.Document;
 import org.dom4j.DocumentException;
 import org.dom4j.io.SAXReader;
 import org.json.JSONArray;
 import org.json.JSONException;
 import org.json.JSONObject;
 import org.junit.Before;
 import org.junit.Test;
 import org.mortbay.jetty.testing.HttpTester;
 import org.mortbay.jetty.testing.ServletTester;
 import org.slf4j.Logger;
 import org.slf4j.LoggerFactory;
 
 public class TestGeneral {
 
 	private static final Logger log=LoggerFactory.getLogger(TestGeneral.class);
 	
 	private final static String testStr = "{\"items\":[{\"value\":\"This is an experimental widget being tested. It will not do what you expect.\"," +
 	"\"title\":\"\",\"type\":\"caption\"},{\"title\":\"Your file\",\"type\":\"resource\",\"param\":\"file\"}," +
 	"{\"title\":\"Author\",\"type\":\"text\",\"param\":\"author\"},{\"title\":\"Title\",\"type\":\"text\"," +
 	"\"param\":\"title\"},{\"title\":\"Type\",\"type\":\"dropdown\",\"values\":[{\"value\":\"1\",\"text\":" +
 	"\"thesis\"},{\"value\":\"2\",\"text\":\"paper\"},{\"value\":\"3\",\"text\":\"excel-controlled\"}]," +
 	"\"param\":\"type\"}]}";
 
 	private final static String testStr2 = "{\"accessionNumber\":\"123\",\"a\":\"b\\\"\"}";
 
 	private final static String testStr3 = "{\"a\":\"b\",\"id\":\"***misc***\",\"objects\":\"***objects***\",\"intake\":\"***intake***\"}";
 	
 	private final static String testStr4 = "{\"a\":\"b\",\"id\":\"MISC2009.1\",\"objects\":\"OBJ2009.1\",\"intake\":\"IN2009.1\"}";
 	private final static String testStr5 = "{\"a\":\"b\",\"id\":\"MISC2009.2\",\"objects\":\"OBJ2009.2\",\"intake\":\"IN2009.2\"}";
 
 	private final static String testStr6 = "{\"userId\": \"unittest2@collectionspace.org\",\"userName\": \"unittest2@collectionspace.org\",\"password\": \"testpassword\",\"email\": \"unittest2@collectionspace.org\",\"status\": \"inactive\"}";
 	private final static String testStr7 = "{\"userId\": \"unittest@collectionspace.org\",\"screenName\": \"unittestzzz\",\"password\": \"testpassword\",\"email\": \"unittest@collectionspace.org\",\"status\": \"active\"}";
 	private final static String testStr8 = "{\"email\": \"unittest@collectionspace.org\", \"debug\" : true }";
 
 	private FileStorage store;
 
 	private static String tmp=null;
 
 	private static synchronized String tmpdir() {
 		if(tmp==null) {
 			tmp=System.getProperty("java.io.tmpdir");
 		}
 		return tmp;
 	}
 
 	private void rm_r(File in) {
 		for(File f : in.listFiles()) {
 			if(f.isDirectory())
 				rm_r(f);
 			f.delete();
 		}
 	}
 
 	@Before public void setup() throws IOException, CSPDependencyException {
 		File tmp=new File(tmpdir());
 		File dir=new File(tmp,"ju-cspace");
 		if(dir.exists())
 			rm_r(dir);
 		if(!dir.exists())
 			dir.mkdir();
 		store=new FileStorage(dir.toString());
 	}
 
 
 	@Test public void writeJSONToFile() throws JSONException, ExistException, UnderlyingStorageException, UnimplementedException {
 		JSONObject jsonObject = new JSONObject(testStr);
 		store.autocreateJSON("/objects/", jsonObject);
 	}
 
 	@Test public void readJSONFromFile() throws JSONException, ExistException, UnderlyingStorageException, UnimplementedException {
 		JSONObject jsonObject = new JSONObject(testStr);
 		String path=store.autocreateJSON("/objects/", jsonObject);
 		JSONObject resultObj = store.retrieveJSON("/objects/"+path);
 		JSONObject testObj = new JSONObject(testStr);
 		assertTrue(JSONUtils.checkJSONEquiv(resultObj,testObj));
 	}
 
 	@Test public void testJSONNotExist() throws JSONException, UnderlyingStorageException, UnimplementedException {
 		try
 		{
 			store.retrieveJSON("nonesuch.json");
 			assertTrue(false);
 		}
 		catch (ExistException onfe) {}
 	}
 
 	@Test public void testJSONUpdate() throws ExistException, JSONException, UnderlyingStorageException, UnimplementedException {
 		JSONObject jsonObject = new JSONObject(testStr2);
 		String id1=store.autocreateJSON("/objects/", jsonObject);
 		jsonObject = new JSONObject(testStr);
 		store.updateJSON("/objects/"+id1, jsonObject);		
 		JSONObject resultObj = store.retrieveJSON("/objects/"+id1);
 		JSONObject testObj = new JSONObject(testStr);
 		assertTrue(JSONUtils.checkJSONEquiv(resultObj,testObj));
 	}
 
 	@Test public void testJSONNoUpdateNonExisting() throws ExistException, JSONException, UnderlyingStorageException, UnimplementedException {
 		JSONObject jsonObject = new JSONObject(testStr);
 		try {
 			store.updateJSON("/objects/json1.test", jsonObject);
 			assertTrue(false);
 		} catch(ExistException e) {}
 	}
 
 	private File tmpSchemaFile(String type,boolean sj) {
 		File sroot=new File(store.getStoreRoot()+"/uispecs");
 		if(!sroot.exists())
 			sroot.mkdir();
 		File schema=new File(store.getStoreRoot()+"/uispecs/"+type);
 		if(!schema.exists())
 			schema.mkdir();
 		return new File(schema,sj?"uispec.json":"test-json-handle.tmp");
 	}
 
 	private void createSchemaFile(String type,boolean sj,boolean alt) throws IOException {
 		File file=tmpSchemaFile(type,sj);
 		FileOutputStream out=new FileOutputStream(file);
 		IOUtils.write(alt?testStr2:testStr,out);
 		out.close();
 	}
 
 	private void deleteSchemaFile(String type,boolean sj) {
 		File file=tmpSchemaFile(type,sj);
 		file.delete();
 	}
 
 	@Test public void testSchemaStore() throws IOException, JSONException {
 		SchemaStore schema=new StubSchemaStore(store.getStoreRoot());
 		createSchemaFile("collection-object",false,true);
 		JSONObject j=schema.getSchema("collection-object/test-json-handle.tmp");
 		JSONUtils.checkJSONEquiv(testStr2,j.toString());
 		deleteSchemaFile("collection-object",false);
 	}
 
 	@Test public void testDefaultingSchemaStore() throws IOException, JSONException {
 		SchemaStore schema=new StubSchemaStore(store.getStoreRoot());
 		createSchemaFile("collection-object",true,true);
 		JSONObject j=schema.getSchema("collection-object");
 		JSONUtils.checkJSONEquiv(testStr2,j.toString());
 		deleteSchemaFile("collection-object",true);
 	}
 
 	@Test public void testTrailingSlashOkayOnSchema() throws Exception {
 		SchemaStore schema=new StubSchemaStore(store.getStoreRoot());
 		createSchemaFile("collection-object",true,true);
 		JSONObject j=schema.getSchema("collection-object/");
 		JSONUtils.checkJSONEquiv(testStr2,j.toString());
 		deleteSchemaFile("collection-object",true);	
 	}
 
 	private ServletTester setupJetty() throws Exception {
 		ServletTester tester=new ServletTester();
 		tester.setContextPath("/chain");
 		tester.addServlet(ChainServlet.class, "/*");
 		tester.addServlet("org.mortbay.jetty.servlet.DefaultServlet", "/");
 		tester.setAttribute("test-store",store.getStoreRoot());
 		tester.setAttribute("config-filename","default.xml");
 		tester.start();
 		return tester;
 	}
 
 	// XXX refactor
 	private HttpTester jettyDo(ServletTester tester,String method,String path,String data) throws IOException, Exception {
 		HttpTester request = new HttpTester();
 		HttpTester response = new HttpTester();
 		request.setMethod(method);
 		request.setHeader("Host","tester");
 		request.setURI(path);
 		request.setVersion("HTTP/1.0");		
 		if(data!=null)
 			request.setContent(data);
 		response.parse(tester.getResponses(request.generate()));
 				
 		return response;
 	}
 
 	@Test public void testJettyStartupWorks() throws Exception {
 		setupJetty();
 	}
 
 	private JSONObject makeRequest(JSONObject fields) throws JSONException {
 		JSONObject out=new JSONObject();
 		out.put("fields",fields);
 		return out;
 	}
 	
 	private String makeSimpleRequest(String in) throws JSONException {
 		return makeRequest(new JSONObject(in)).toString();
 	}
 	
 	@Test public void testAutoPost() throws Exception {
 		deleteSchemaFile("collection-object",false);
 		ServletTester jetty=setupJetty();
 		HttpTester out=jettyDo(jetty,"POST","/chain/objects/",makeSimpleRequest(testStr2));
 		assertNotNull(out.getHeader("Location"));
 		assertTrue(out.getHeader("Location").startsWith("/objects/"));
 		Integer.parseInt(out.getHeader("Location").substring("/objects/".length()));
 	}
 	
 	private String getFields(String in) throws JSONException {
 		return getFields(new JSONObject(in)).toString();
 	}
 
 	private JSONObject getFields(JSONObject in) throws JSONException {
 		in=in.getJSONObject("fields");
 		in.remove("csid");
 		return in;
 	}
 	
 	
 	@Test public void testUserProfiles() throws Exception {
 		log.info("1");
 		deleteSchemaFile("collection-object",false);
 		log.info("2");
 
 		ServletTester jetty=setupJetty();
 		log.info("3");
 		HttpTester out=jettyDo(jetty,"POST","/chain/users/",makeSimpleRequest(testStr6));
 		log.info("4");
 		assertEquals(out.getMethod(),null);
 		log.info("GET CREATE "+out.getContent());
 		String id=out.getHeader("Location");
 		log.info(out.getContent());
 		assertEquals(201,out.getStatus());
 		
 		out=jettyDo(jetty,"GET","/chain"+id,null);
 		log.info("GET READ "+id+":"+out.getContent());
 		assertTrue(JSONUtils.checkJSONEquivOrEmptyStringKey(new JSONObject(getFields(out.getContent())),new JSONObject(testStr6)));
 		out=jettyDo(jetty,"PUT","/chain"+id,makeSimpleRequest(testStr7));
 		log.info("PUT "+id+":"+out.getContent());
 		assertEquals(200,out.getStatus());		
 		out=jettyDo(jetty,"GET","/chain"+id,null);
 		log.info("GET READ "+id+":"+out.getContent());
 		assertTrue(JSONUtils.checkJSONEquivOrEmptyStringKey(new JSONObject(getFields(out.getContent())),new JSONObject(testStr7)));
 		out=jettyDo(jetty,"DELETE","/chain"+id,null);
 		assertEquals(200,out.getStatus());
 		log.info("DELETE "+id+":"+out.getContent());
 		out=jettyDo(jetty,"GET","/chain"+id,null);
 		assertTrue(out.getStatus()>=400); // XXX should probably be 404
 		
 		
 	}
 	
 	@Test public void testPostAndDelete() throws Exception {
 		deleteSchemaFile("collection-object",false);
 		ServletTester jetty=setupJetty();
 		HttpTester out=jettyDo(jetty,"POST","/chain/objects/",makeSimpleRequest(testStr2));	
 		assertEquals(out.getMethod(),null);
 		String id=out.getHeader("Location");
 		assertEquals(201,out.getStatus());
 		out=jettyDo(jetty,"GET","/chain"+id,null);
 		assertTrue(JSONUtils.checkJSONEquivOrEmptyStringKey(new JSONObject(getFields(out.getContent())),new JSONObject(testStr2)));
 		out=jettyDo(jetty,"PUT","/chain"+id,makeSimpleRequest(testStr));
 		assertEquals(200,out.getStatus());		
 		out=jettyDo(jetty,"GET","/chain"+id,null);
 		assertTrue(JSONUtils.checkJSONEquivOrEmptyStringKey(new JSONObject(getFields(out.getContent())),new JSONObject(testStr)));
 		out=jettyDo(jetty,"DELETE","/chain"+id,null);
 		assertEquals(200,out.getStatus());
 		out=jettyDo(jetty,"GET","/chain"+id,null);
 		assertTrue(out.getStatus()>=400); // XXX should probably be 404
 	}
 
 	@Test public void testMultipleStoreTypes() throws Exception {
 		deleteSchemaFile("collection-object",false);
 		ServletTester jetty=setupJetty();
 		HttpTester out=jettyDo(jetty,"POST","/chain/objects/",makeSimpleRequest(testStr2));	
 		assertEquals(out.getMethod(),null);
 		String id1=out.getHeader("Location");
 		log.info(out.getContent());
 		assertEquals(201,out.getStatus());
 		out=jettyDo(jetty,"POST","/chain/intake/",makeSimpleRequest(testStr));	
 		assertEquals(out.getMethod(),null);
 		String id2=out.getHeader("Location");		
 		log.info(out.getContent());
 		assertEquals(201,out.getStatus());		
 		out=jettyDo(jetty,"GET","/chain"+id1,null);
 		assertTrue(JSONUtils.checkJSONEquivOrEmptyStringKey(new JSONObject(getFields(out.getContent())),new JSONObject(testStr2)));
 		out=jettyDo(jetty,"GET","/chain"+id2,null);
 		assertTrue(JSONUtils.checkJSONEquivOrEmptyStringKey(new JSONObject(getFields(out.getContent())),new JSONObject(testStr)));
 		out=jettyDo(jetty,"DELETE","/chain"+id1,null);
 		assertEquals(200,out.getStatus());
 		out=jettyDo(jetty,"GET","/chain"+id1,null);
 		assertTrue(out.getStatus()>=400); // XXX should probably be 404
 		out=jettyDo(jetty,"GET","/chain"+id2,null);
 		assertTrue(JSONUtils.checkJSONEquivOrEmptyStringKey(new JSONObject(getFields(out.getContent())),new JSONObject(testStr)));
 	}
 
 	@Test public void testServeStatic() throws Exception {
 		HttpTester out=jettyDo(setupJetty(),"GET","/chain/chain.properties",null);
 		assertEquals(200,out.getStatus());
 		assertTrue(out.getContent().contains("cspace.chain.store.dir"));
 	}
 	
 	@Test public void testObjectList() throws Exception {
 		ServletTester jetty=setupJetty();
 		HttpTester out1=jettyDo(jetty,"POST","/chain/objects/",makeSimpleRequest(testStr2));	
 		HttpTester out2=jettyDo(jetty,"POST","/chain/objects/",makeSimpleRequest(testStr2));	
 		HttpTester out3=jettyDo(jetty,"POST","/chain/objects/",makeSimpleRequest(testStr2));
 		File storedir=new File(store.getStoreRoot(),"store");
 		if(!storedir.exists())
 			storedir.mkdir();
 		File junk=new File(storedir,"junk");
 		IOUtils.write("junk",new FileOutputStream(junk));	
 		HttpTester out=jettyDo(jetty,"GET","/chain/objects",null);
 		assertEquals(200,out.getStatus());
 		JSONObject result=new JSONObject(out.getContent());
 		JSONArray items=result.getJSONArray("items");
 		Set<String> files=new HashSet<String>();
 		for(int i=0;i<items.length();i++)
 			files.add("/objects/"+items.getJSONObject(i).getString("csid"));
 		log.info(out1.getHeader("Location"));
 		assertTrue(files.contains(out1.getHeader("Location")));
 		assertTrue(files.contains(out2.getHeader("Location")));
 		assertTrue(files.contains(out3.getHeader("Location")));
 		assertEquals(3,files.size());
 	}
 
 	@Test public void testPutReturnsContent() throws Exception {
 		deleteSchemaFile("collection-object",false);
 		ServletTester jetty=setupJetty();
 		HttpTester out=jettyDo(jetty,"POST","/chain/objects/",makeSimpleRequest(testStr2));
 		String id=out.getHeader("Location");
 		assertEquals(out.getMethod(),null);
 		log.info(out.getContent());
 		assertEquals(201,out.getStatus());
 		out=jettyDo(jetty,"GET","/chain"+id,null);
 		assertTrue(JSONUtils.checkJSONEquivOrEmptyStringKey(new JSONObject(getFields(out.getContent())),new JSONObject(testStr2)));
 		out=jettyDo(jetty,"PUT","/chain"+id,makeSimpleRequest(testStr));
 		assertEquals(200,out.getStatus());	
 		assertTrue(JSONUtils.checkJSONEquivOrEmptyStringKey(new JSONObject(testStr),new JSONObject(getFields(out.getContent()))));
 	}
 
 	@Test public void testTrailingSlashOkayOnList() throws Exception {
 		ServletTester jetty=setupJetty();
 		HttpTester out1=jettyDo(jetty,"POST","/chain/objects",makeSimpleRequest(testStr2));	
 		HttpTester out2=jettyDo(jetty,"POST","/chain/objects",makeSimpleRequest(testStr2));	
 		HttpTester out3=jettyDo(jetty,"POST","/chain/objects",makeSimpleRequest(testStr2));
 		HttpTester out=jettyDo(jetty,"GET","/chain/objects/",null);
 		assertEquals(200,out.getStatus());
 		JSONObject result=new JSONObject(out.getContent());
 		JSONArray items=result.getJSONArray("items");
 		Set<String> files=new HashSet<String>();
 		for(int i=0;i<items.length();i++)
 			files.add("/objects/"+items.getJSONObject(i).getString("csid"));
 		assertTrue(files.contains(out1.getHeader("Location")));
 		assertTrue(files.contains(out2.getHeader("Location")));
 		assertTrue(files.contains(out3.getHeader("Location")));
 		assertEquals(3,files.size());		
 	}
 
 	@Test public void testDirectories() throws ExistException, UnimplementedException, UnderlyingStorageException, JSONException {
 		JSONObject jsonObject = new JSONObject(testStr);
 		String id1=store.autocreateJSON("/a", jsonObject);
 		String id2=store.autocreateJSON("/b", jsonObject);
 		File d1=new File(store.getStoreRoot());
 		assertTrue(d1.exists());
 		File d2=new File(d1,"data");
 		assertTrue(d2.exists());
 		File a=new File(d2,"a");
 		assertTrue(a.exists());
 		File b=new File(d2,"b");
 		assertTrue(b.exists());
 		assertTrue(new File(a,id1+".json").exists());
 		assertTrue(new File(b,id2+".json").exists());
 	}
 	
 	@Test public void testEmail(){
 		Boolean doIreallyWantToSpam = false; // set to true when you have configured the email addresses
 		/* please personalises these emails before sending - I don't want your spam. */
 	    String from = "";
 	    String[] recipients = { ""};
 
 	    String SMTP_HOST_NAME = "localhost";
 	    String SMTP_PORT = "25";
 	    String message = "Hi, Test Message Contents";
 	    String subject = "A test from collectionspace test suite";
 	    String SSL_FACTORY = "javax.net.ssl.SSLSocketFactory";
         Security.addProvider(new com.sun.net.ssl.internal.ssl.Provider());
         boolean debug = true;
         
         Properties props = new Properties();
         props.put("mail.smtp.host", SMTP_HOST_NAME);
         //props.put("mail.smtp.auth", "true");
         props.put("mail.smtp.auth", "false");
         props.put("mail.debug", "true");
         props.put("mail.smtp.port", SMTP_PORT);
         props.put("mail.smtp.socketFactory.port", SMTP_PORT);
         props.put("mail.smtp.socketFactory.class", SSL_FACTORY);
         props.put("mail.smtp.socketFactory.fallback", "false");
  
         Session session = Session.getDefaultInstance(props);
  
         session.setDebug(debug);
  
         Message msg = new MimeMessage(session);
         InternetAddress addressFrom;
 		try {
 			addressFrom = new InternetAddress(from);
         msg.setFrom(addressFrom);
  
         InternetAddress[] addressTo = new InternetAddress[recipients.length];
         for (int i = 0; i < recipients.length; i++) {
             addressTo[i] = new InternetAddress(recipients[i]);
         }
         msg.setRecipients(Message.RecipientType.TO, addressTo);
  
         // Setting the Subject and Content Type
         msg.setSubject(subject);
         msg.setContent(message, "text/plain");
         if(doIreallyWantToSpam){
         	Transport.send(msg);
        	assertTrue(doIreallyWantToSpam);
         }
 		} catch (AddressException e) {
			log.info(e.getMessage());
			assertTrue(false);
 		} catch (MessagingException e) {
			log.info(e.getMessage());
			assertTrue(false);
 		}
		//assertTrue(doIreallyWantToSpam);
 	}
 }
