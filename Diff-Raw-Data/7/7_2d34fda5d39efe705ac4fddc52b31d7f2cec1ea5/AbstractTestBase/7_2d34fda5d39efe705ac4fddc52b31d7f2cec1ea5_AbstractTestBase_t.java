 package org.apache.ivory.resource;
 
 import java.io.BufferedReader;
 import java.io.File;
 import java.io.FileInputStream;
 import java.io.FileOutputStream;
 import java.io.FileReader;
 import java.io.IOException;
 import java.io.InputStream;
 import java.io.InputStreamReader;
 import java.io.OutputStream;
 import java.io.StringReader;
 import java.net.MalformedURLException;
 import java.net.URL;
 import java.util.Enumeration;
 import java.util.HashMap;
 import java.util.List;
 import java.util.Map;
 import java.util.Set;
 import java.util.regex.Matcher;
 import java.util.regex.Pattern;
 
 import javax.servlet.RequestDispatcher;
 import javax.servlet.Servlet;
 import javax.servlet.ServletContext;
 import javax.servlet.ServletContextEvent;
 import javax.servlet.ServletException;
 import javax.servlet.ServletInputStream;
 import javax.ws.rs.core.MediaType;
 import javax.ws.rs.core.UriBuilder;
 import javax.xml.bind.JAXBContext;
 import javax.xml.bind.JAXBException;
 import javax.xml.bind.Marshaller;
 import javax.xml.bind.Unmarshaller;
 
 import org.apache.hadoop.fs.FileSystem;
 import org.apache.hadoop.fs.Path;
 import org.apache.ivory.IvoryException;
 import org.apache.ivory.cluster.util.EmbeddedCluster;
 import org.apache.ivory.entity.store.ConfigurationStore;
 import org.apache.ivory.entity.v0.EntityType;
 import org.apache.ivory.entity.v0.cluster.Cluster;
 import org.apache.ivory.entity.v0.feed.Feed;
 import org.apache.ivory.listener.ContextStartupListener;
 import org.apache.ivory.util.EmbeddedServer;
 import org.apache.ivory.util.StartupProperties;
 import org.apache.ivory.workflow.engine.OozieClientFactory;
 import org.apache.oozie.client.BundleJob;
 import org.apache.oozie.client.CoordinatorJob;
 import org.apache.oozie.client.Job.Status;
 import org.apache.oozie.client.OozieClient;
 import org.apache.oozie.client.OozieClientException;
 import org.testng.Assert;
 import org.testng.annotations.AfterClass;
 import org.testng.annotations.AfterMethod;
 import org.testng.annotations.BeforeClass;
 
 import com.sun.jersey.api.client.Client;
 import com.sun.jersey.api.client.ClientResponse;
 import com.sun.jersey.api.client.WebResource;
 import com.sun.jersey.api.client.config.ClientConfig;
 import com.sun.jersey.api.client.config.DefaultClientConfig;
 
 public class AbstractTestBase {
     protected static final String FEED_TEMPLATE1 = "/feed-template1.xml";
     protected static final String FEED_TEMPLATE2 = "/feed-template2.xml";
     protected static final String CLUSTER_FILE_TEMPLATE = "target/cluster-template.xml";
 
     protected static final String SAMPLE_PROCESS_XML = "/process-version-0.xml";
     protected static final String PROCESS_TEMPLATE = "/process-template.xml";
 
     protected static final String BASE_URL = "http://localhost:15000/";
 
     protected EmbeddedServer server;
 
     protected Unmarshaller unmarshaller;
     protected Marshaller marshaller;
 
     protected EmbeddedCluster cluster;
     protected WebResource service = null;
     protected String clusterName;
     protected String processName;
 
     private static final Pattern varPattern = Pattern.compile("##[A-Za-z0-9_]*##");
 
     protected void scheduleProcess() throws Exception {
         ClientResponse response;
         Map<String, String> overlay = new HashMap<String, String>();
 
         clusterName = "local" + System.currentTimeMillis();
         overlay.put("name", clusterName);
         response = submitToIvory(CLUSTER_FILE_TEMPLATE, overlay, EntityType.CLUSTER);
         assertSuccessful(response);
 
         String feed1 = "f1" + System.currentTimeMillis();
         overlay.put("name", feed1);
         overlay.put("cluster", clusterName);
         response = submitToIvory(FEED_TEMPLATE1, overlay, EntityType.FEED);
         assertSuccessful(response);
 
         String feed2 = "f2" + System.currentTimeMillis();
         overlay.put("name", feed2);
         response = submitToIvory(FEED_TEMPLATE2, overlay, EntityType.FEED);
         assertSuccessful(response);
 
         processName = "p1" + System.currentTimeMillis();
         overlay.put("name", processName);
         overlay.put("f1", feed1);
         overlay.put("f2", feed2);
         response = submitToIvory(PROCESS_TEMPLATE, overlay, EntityType.PROCESS);
         assertSuccessful(response);        
         ClientResponse clientRepsonse = this.service.path("api/entities/schedule/process/" + processName)
                 .header("Remote-User", "guest").accept(MediaType.TEXT_XML).type(MediaType.TEXT_XML).post(ClientResponse.class);
         assertSuccessful(clientRepsonse);    
     }
     
     protected void waitForProcessStart() throws Exception {
         OozieClient ozClient = OozieClientFactory.get((Cluster) ConfigurationStore.get().get(EntityType.CLUSTER, clusterName));
         String bundleId = getBundleId(ozClient);
 
         for (int i = 0; i < 10; i++) {
             Thread.sleep(1000);
             BundleJob bundle = ozClient.getBundleJobInfo(bundleId);
             if (bundle.getStatus() == Status.RUNNING) {
                boolean done = false;
                 for (CoordinatorJob coord : bundle.getCoordinators())
                    if (coord.getStatus() == Status.RUNNING)
                        done = true;
                 if (done == true)
                     return;
             }
         }
         throw new Exception("Bundle " + bundleId + " is not RUNNING in oozie");
     }
     
     public AbstractTestBase() {
         try {
             JAXBContext jaxbContext = JAXBContext.newInstance(APIResult.class, Feed.class, Process.class, Cluster.class, ProcessInstancesResult.class);
             unmarshaller = jaxbContext.createUnmarshaller();
             marshaller = jaxbContext.createMarshaller();
 
         } catch (Exception e) {
             throw new RuntimeException(e);
         }
     }
 
     @BeforeClass
     public void configure() throws Exception {
         StartupProperties.get().setProperty("application.services", 
                 StartupProperties.get().getProperty("application.services").replace("org.apache.ivory.aspect.instances.ProcessSubscriberService", ""));
         if (new File("webapp/src/main/webapp").exists()) {
             this.server = new EmbeddedServer(15000, "webapp/src/main/webapp");
         } else if (new File("src/main/webapp").exists()) {
             this.server = new EmbeddedServer(15000, "src/main/webapp");
         } else {
             throw new RuntimeException("Cannot run jersey tests");
         }
         ClientConfig config = new DefaultClientConfig();
         Client client = Client.create(config);
         this.service = client.resource(UriBuilder.fromUri(BASE_URL).build());
         this.server.start();
         
         this.cluster = EmbeddedCluster.newCluster("##name##", false);
         Cluster clusterEntity = this.cluster.getCluster();
         FileOutputStream out = new FileOutputStream(CLUSTER_FILE_TEMPLATE);
         marshaller.marshal(clusterEntity, out);
         out.close();
 
         cleanupStore();
 
         //setup dependent workflow and lipath in hdfs
         FileSystem fs = FileSystem.get(this.cluster.getConf());
         fs.mkdirs(new Path("/examples/apps/aggregator"));
         fs.mkdirs(new Path("/examples/apps/aggregator/lib"));
     }
 
     /**
      * Converts a InputStream into ServletInputStream
      * 
      * @param fileName
      * @return ServletInputStream
      * @throws java.io.IOException
      */
     protected ServletInputStream getServletInputStream(String fileName) throws IOException {
         return getServletInputStream(new FileInputStream(fileName));
     }
 
     protected ServletInputStream getServletInputStream(final InputStream stream) throws IOException {
         return new ServletInputStream() {
 
             @Override
             public int read() throws IOException {
                 return stream.read();
             }
         };
     }
 
     public void tearDown() throws Exception {
         this.cluster.shutdown();
         server.stop();
     }
 
     public void cleanupStore() throws Exception {
         for (EntityType type : EntityType.values()) {
             for (String name : ConfigurationStore.get().getEntities(type)) {
                 ConfigurationStore.get().remove(type, name);
             }
         }
     }
 
     protected ClientResponse submitAndSchedule(String template, Map<String, String> overlay, EntityType entityType) throws Exception {
         String tmpFile = overlayParametersOverTemplate(template, overlay);        
         ServletInputStream rawlogStream = getServletInputStream(tmpFile);
 
         return this.service.path("api/entities/submitAndSchedule/" + entityType.name().toLowerCase()).header("Remote-User", "testuser")
                 .accept(MediaType.TEXT_XML).type(MediaType.TEXT_XML).post(ClientResponse.class, rawlogStream);
     }
     
     protected ClientResponse submitToIvory(String template, Map<String, String> overlay, EntityType entityType) throws IOException {
         String tmpFile = overlayParametersOverTemplate(template, overlay);
         return submitFileToIvory(entityType, tmpFile);
     }
 
     private ClientResponse submitFileToIvory(EntityType entityType, String tmpFile) throws IOException {
 
         ServletInputStream rawlogStream = getServletInputStream(tmpFile);
 
         return this.service.path("api/entities/submit/" + entityType.name().toLowerCase()).header("Remote-User", "testuser")
                 .accept(MediaType.TEXT_XML).type(MediaType.TEXT_XML).post(ClientResponse.class, rawlogStream);
     }
 
     protected void assertSuccessful(ClientResponse clientRepsonse) {
         String response = clientRepsonse.getEntity(String.class);
         try {
             APIResult result = (APIResult)unmarshaller.
                     unmarshal(new StringReader(response));
             Assert.assertEquals(result.getStatus(), APIResult.Status.SUCCEEDED);
         } catch (JAXBException e) {
             Assert.fail("Reponse " + response + " is not valid");
         }
     }
 
     protected String overlayParametersOverTemplate(String template, Map<String, String> overlay) throws IOException {
         File tmpFile = getTempFile();
         OutputStream out = new FileOutputStream(tmpFile);
 
         InputStreamReader in;
         if (getClass().getResourceAsStream(template) == null) {
             in = new FileReader(template);
         } else {
             in = new InputStreamReader(getClass().getResourceAsStream(template));
         }
         BufferedReader reader = new BufferedReader(in);
         String line;
         while ((line = reader.readLine()) != null) {
             Matcher matcher = varPattern.matcher(line);
             while (matcher.find()) {
                 String variable = line.substring(matcher.start(), matcher.end());
                 line = line.replace(variable, overlay.get(variable.substring(2, variable.length() - 2)));
                 matcher = varPattern.matcher(line);
             }
             out.write(line.getBytes());
             out.write("\n".getBytes());
         }
         reader.close();
         out.close();
         return tmpFile.getAbsolutePath();
     }
     
     protected File getTempFile() throws IOException {
         File target = new File("webapp/target");
         if (!target.exists()) {
             target = new File("target");
         }
 
         return File.createTempFile("test", ".xml", target);
     }
     
     protected String getBundleId(OozieClient ozClient) throws Exception {
         List<BundleJob> bundles = ozClient.getBundleJobsInfo("name=IVORY_PROCESS_" + processName, 0, 10);
         if(bundles != null)
             return bundles.get(0).getId();
         return null;
     }
 
     @AfterClass
     public void cleanup() throws Exception {
         tearDown();
         cleanupStore();
     }
 
     @AfterMethod
     public boolean killOozieJobs() throws IvoryException, OozieClientException {
         if(clusterName == null)
             return true;
 
         Cluster cluster = (Cluster) ConfigurationStore.get().get(EntityType.CLUSTER, clusterName);
         if(cluster == null)
             return true;
 
         OozieClient ozClient = OozieClientFactory.get(cluster);
         List<BundleJob> bundles = ozClient.getBundleJobsInfo("name=IVORY_PROCESS_" + processName, 0, 10);
         if(bundles != null) {
             for(BundleJob bundle:bundles)
                 ozClient.kill(bundle.getId());
         }
         return false;
     }
 }
