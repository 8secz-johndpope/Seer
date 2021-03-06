 // License: GPL. For details, see LICENSE file.
 package org.openstreetmap.josm.io;
 
 import static org.junit.Assert.assertEquals;
 import static org.junit.Assert.assertNotNull;
 import static org.junit.Assert.fail;
 import static org.junit.Assert.*;
 
 import java.io.File;
 import java.io.FileInputStream;
 import java.io.FileWriter;
 import java.io.IOException;
 import java.io.PrintWriter;
 import java.text.MessageFormat;
 import java.util.ArrayList;
 import java.util.Iterator;
 import java.util.Properties;
 import java.util.logging.Level;
 import java.util.logging.Logger;
 
 import org.junit.Before;
 import org.junit.BeforeClass;
 import org.junit.Test;
 import org.openstreetmap.josm.Main;
 import org.openstreetmap.josm.data.coor.LatLon;
 import org.openstreetmap.josm.data.osm.DataSet;
 import org.openstreetmap.josm.data.osm.Node;
 import org.openstreetmap.josm.data.osm.OsmPrimitive;
 import org.openstreetmap.josm.data.osm.Relation;
 import org.openstreetmap.josm.data.osm.RelationMember;
 import org.openstreetmap.josm.data.osm.Way;
 import org.openstreetmap.josm.data.projection.Mercator;
 import org.openstreetmap.josm.gui.PleaseWaitDialog;
 import org.xml.sax.SAXException;
 
 public class MultiFetchServerObjectReaderTest {
     private static Logger logger = Logger.getLogger(MultiFetchServerObjectReader.class.getName());
 
     /**
      * builds a large data set to be used later for testing MULTI FETCH on the server
      * 
      * @return a large data set
      */
     protected static DataSet buildTestDataSet() {
         DataSet ds = new DataSet();
         ds.version = "0.6";
 
         int numNodes = 1000;
         int numWays = 1000;
         int numRelations = 1000;
 
         ArrayList<Node> nodes = new ArrayList<Node>();
         ArrayList<Way> ways = new ArrayList<Way>();
 
         // create a set of nodes
         //
         for (int i=0; i< numNodes; i++) {
             Node n = new Node(0);
             n.setCoor(new LatLon(-36.6,47.6));
             n.put("name", "node-"+i);
             n.incomplete = false;
             ds.addPrimitive(n);
             nodes.add(n);
         }
 
         // create a set of ways, each with a random number of
         // nodes
         //
         for (int i=0; i< numWays; i++) {
             Way w = new Way(0);
             w.incomplete = false;
             int numNodesInWay = 2 + (int)Math.round(Math.random() * 5);
             int start = (int)Math.round(Math.random() * numNodes);
             for (int j = 0; j < numNodesInWay;j++) {
                 int idx = (start + j) % numNodes;
                 Node n = nodes.get(idx);
                 w.nodes.add(n);
             }
             w.put("name", "way-"+i);
             ds.addPrimitive(w);
             ways.add(w);
         }
 
         // create a set of relations each with a random number of nodes,
         // and ways
         //
         for (int i=0; i< numRelations; i++) {
             Relation r = new Relation(0);
             r.incomplete = false;
             r.put("name", "relation-" +i);
             int numNodesInRelation = (int)Math.round(Math.random() * 10);
             int start = (int)Math.round(Math.random() * numNodes);
             for (int j = 0; j < numNodesInRelation;j++) {
                 int idx = (start + j) % 500;
                 Node n = nodes.get(idx);
                 RelationMember member = new RelationMember();
                 member.member = n;
                 member.role = "role-" + j;
                 r.members.add(member);
             }
             int numWaysInRelation = (int)Math.round(Math.random() * 10);
             start = (int)Math.round(Math.random() * numWays);
             for (int j = 0; j < numWaysInRelation;j++) {
                 int idx = (start + j) % 500;
                 Way w = ways.get(idx);
                 RelationMember member = new RelationMember();
                 member.member = w;
                 member.role = "role-" + j;
                 r.members.add(member);
             }
             ds.addPrimitive(r);
         }
 
         return ds;
     }
 
     static public DataSet testDataSet;
     static public Properties testProperties;
 
     /**
      * creates the dataset on the server.
      * 
      * @param ds the data set
      * @throws OsmTransferException
      */
     static public void createDataSetOnServer(DataSet ds) throws OsmTransferException {
         logger.info("creating data set on the server ...");
         ArrayList<OsmPrimitive> primitives = new ArrayList<OsmPrimitive>();
         primitives.addAll(testDataSet.nodes);
         primitives.addAll(testDataSet.ways);
         primitives.addAll(testDataSet.relations);
 
         OsmServerWriter writer = new OsmServerWriter();
         writer.uploadOsm("0.6", primitives);
     }
 
     @BeforeClass
     public static void  init() throws OsmTransferException, InterruptedException{
         logger.info("initializing ...");
         testProperties = new Properties();
 
         // load properties
         //
         try {
             testProperties.load(MultiFetchServerObjectReaderTest.class.getResourceAsStream("/test-functional-env.properties"));
         } catch(Exception e){
             logger.log(Level.SEVERE, MessageFormat.format("failed to load property file ''{0}''", "test-functional-env.properties"));
             fail(MessageFormat.format("failed to load property file ''{0}''", "test-functional-env.properties"));
         }
 
         // check josm.home
         //
         String josmHome = testProperties.getProperty("josm.home");
         if (josmHome == null) {
             fail(MessageFormat.format("property ''{0}'' not set in test environment", "josm.home"));
         } else {
             File f = new File(josmHome);
             if (! f.exists() || ! f.canRead()) {
                 fail(MessageFormat.format("property ''{0}'' points to ''{1}'' which is either not existing or not readable", "josm.home", josmHome));
             }
         }
 
         // check temp output dir
         //
         String tempOutputDir = testProperties.getProperty("test.functional.tempdir");
         if (tempOutputDir == null) {
             fail(MessageFormat.format("property ''{0}'' not set in test environment", "test.functional.tempdir"));
         } else {
             File f = new File(tempOutputDir);
             if (! f.exists() || ! f.isDirectory() || ! f.canWrite()) {
                 fail(MessageFormat.format("property ''{0}'' points to ''{1}'' which is either not existing, not a directory, or not writeable", "test.functional.tempdir", tempOutputDir));
             }
         }
 
 
         // init preferences
         //
         System.setProperty("josm.home", josmHome);
         Main.pleaseWaitDlg = new PleaseWaitDialog(null);
         Main.pref.init(false);
         // don't use atomic upload, the test API server can't cope with large diff uploads
         //
         Main.pref.put("osm-server.atomic-upload", false);
         Main.proj = new Mercator();
 
         File dataSetCacheOutputFile = new File(tempOutputDir, MultiFetchServerObjectReaderTest.class.getName() + ".dataset");
 
         // make sure we don't upload to production
         //
         String url = OsmApi.getOsmApi().getBaseUrl().toLowerCase().trim();
         if (url.startsWith("http://www.openstreetmap.org")
                 || url.startsWith("http://api.openstreetmap.org")) {
             fail(MessageFormat.format("configured url ''{0}'' seems to be a productive url, aborting.", url));
         }
 
 
         String p = System.getProperties().getProperty("useCachedDataset");
         if (p != null && Boolean.parseBoolean(p.trim().toLowerCase())) {
             logger.info(MessageFormat.format("property ''{0}'' set, using cached dataset", "useCachedDataset"));
             return;
         }
 
         logger.info(MessageFormat.format("property ''{0}'' not set to true, creating test dataset on the server. property is ''{1}''", "useCachedDataset", p));
 
         // build and upload the test data set
         //
         logger.info("creating test data set ....");
         testDataSet = buildTestDataSet();
         logger.info("uploading test data set ...");
         createDataSetOnServer(testDataSet);
 
         PrintWriter pw = null;
         try {
             pw = new PrintWriter(
                     new FileWriter(dataSetCacheOutputFile)
             );
         } catch(IOException e) {
             fail(MessageFormat.format("failed to open file ''{0}'' for writing", dataSetCacheOutputFile.toString()));
         }
         logger.info(MessageFormat.format("caching test data set in ''{0}'' ...", dataSetCacheOutputFile.toString()));
         OsmWriter w = new OsmWriter(pw, false, testDataSet.version);
         w.header();
         w.writeDataSources(testDataSet);
         w.writeContent(testDataSet);
         w.footer();
         w.close();
         pw.close();
     }
 
     private DataSet ds;
 
 
     @Before
     public void setUp() throws IOException, SAXException {
         File f = new File(testProperties.getProperty("test.functional.tempdir"), MultiFetchServerObjectReaderTest.class.getName() + ".dataset");
         logger.info(MessageFormat.format("reading cached dataset ''{0}''", f.toString()));
         ds = new DataSet();
         FileInputStream fis = new FileInputStream(f);
        ds = OsmReader.parseDataSet(fis, Main.pleaseWaitDlg);
         fis.close();
     }
 
     @Test
     public void testMultiGet10Nodes() throws OsmTransferException {
         MultiFetchServerObjectReader reader = new MultiFetchServerObjectReader();
         ArrayList<Node> nodes = new ArrayList<Node>(ds.nodes);
         for (int i =0; i< 10; i++) {
             reader.append(nodes.get(i));
         }
         DataSet out = reader.parseOsm();
         assertEquals(10, out.nodes.size());
         Iterator<Node> it = out.nodes.iterator();
         while(it.hasNext()) {
             Node n1 = it.next();
             Node n2 = (Node)ds.getPrimitiveById(n1.id);
             assertNotNull(n2);
             assertEquals(n2.get("name"),n2.get("name"));
         }
         assertTrue(reader.getMissingPrimitives().isEmpty());
         assertTrue(reader.getSkippedWays().isEmpty());
     }
 
     @Test
     public void testMultiGet10Ways() throws OsmTransferException {
         MultiFetchServerObjectReader reader = new MultiFetchServerObjectReader();
         ArrayList<Way> ways= new ArrayList<Way>(ds.ways);
         for (int i =0; i< 10; i++) {
             reader.append(ways.get(i));
         }
         DataSet out = reader.parseOsm();
         assertEquals(10, out.ways.size());
         Iterator<Way> it = out.ways.iterator();
         while(it.hasNext()) {
             Way w1 = it.next();
             Way w2 = (Way)ds.getPrimitiveById(w1.id);
             assertNotNull(w2);
             assertEquals(w2.nodes.size(), w1.nodes.size());
             assertEquals(w2.get("name"),w1.get("name"));
         }
         assertTrue(reader.getMissingPrimitives().isEmpty());
         assertTrue(reader.getSkippedWays().isEmpty());
 
     }
 
     @Test
     public void testMultiGet10Relations() throws OsmTransferException {
         MultiFetchServerObjectReader reader = new MultiFetchServerObjectReader();
         ArrayList<Relation> relations= new ArrayList<Relation>(ds.relations);
         for (int i =0; i< 10; i++) {
             reader.append(relations.get(i));
         }
         DataSet out = reader.parseOsm();
         assertEquals(10, out.relations.size());
         Iterator<Relation> it = out.relations.iterator();
         while(it.hasNext()) {
             Relation r1 = it.next();
             Relation r2 = (Relation)ds.getPrimitiveById(r1.id);
             assertNotNull(r2);
             assertEquals(r2.members.size(), r1.members.size());
             assertEquals(r2.get("name"),r2.get("name"));
         }
         assertTrue(reader.getMissingPrimitives().isEmpty());
         assertTrue(reader.getSkippedWays().isEmpty());
 
     }
 
     @Test
     public void testMultiGet800Nodes() throws OsmTransferException {
         MultiFetchServerObjectReader reader = new MultiFetchServerObjectReader();
         ArrayList<Node> nodes = new ArrayList<Node>(ds.nodes);
         for (int i =0; i< 812; i++) {
             reader.append(nodes.get(i));
         }
         DataSet out = reader.parseOsm();
         assertEquals(812, out.nodes.size());
         Iterator<Node> it = out.nodes.iterator();
         while(it.hasNext()) {
             Node n1 = it.next();
             Node n2 = (Node)ds.getPrimitiveById(n1.id);
             assertNotNull(n2);
             assertEquals(n2.get("name"),n2.get("name"));
         }
         assertTrue(reader.getMissingPrimitives().isEmpty());
         assertTrue(reader.getSkippedWays().isEmpty());
 
 
     }
 
     @Test
     public void multiGetWithNonExistingNode() throws OsmTransferException {
         MultiFetchServerObjectReader reader = new MultiFetchServerObjectReader();
         ArrayList<Node> nodes = new ArrayList<Node>(ds.nodes);
         for (int i =0; i< 10; i++) {
             reader.append(nodes.get(i));
         }
         Node n = new Node(9999999);
         reader.append(n); // doesn't exist
         DataSet out = reader.parseOsm();
         assertEquals(10, out.nodes.size());
         Iterator<Node> it = out.nodes.iterator();
         while(it.hasNext()) {
             Node n1 = it.next();
             Node n2 = (Node)ds.getPrimitiveById(n1.id);
             assertNotNull(n2);
             assertEquals(n2.get("name"),n2.get("name"));
         }
         assertFalse(reader.getMissingPrimitives().isEmpty());
         assertEquals(1, reader.getMissingPrimitives().size());
         assertEquals(9999999, reader.getMissingPrimitives().iterator().next());
         assertTrue(reader.getSkippedWays().isEmpty());
     }
 }
