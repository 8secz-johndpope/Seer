 package org.infinispan.loaders.cloud;
 
 import org.infinispan.CacheDelegate;
 import org.infinispan.container.entries.InternalCacheEntry;
 import org.infinispan.container.entries.InternalEntryFactory;
 import org.infinispan.io.UnclosableObjectInputStream;
 import org.infinispan.io.UnclosableObjectOutputStream;
 import org.infinispan.loaders.BaseCacheStoreTest;
 import org.infinispan.loaders.CacheLoaderException;
 import org.infinispan.loaders.CacheStore;
 import org.infinispan.marshall.Marshaller;
 import org.jclouds.blobstore.BlobStoreContext;
 import org.jclouds.blobstore.integration.StubBlobStoreContextBuilder;
 import org.testng.annotations.AfterMethod;
 import org.testng.annotations.BeforeMethod;
 import org.testng.annotations.BeforeTest;
 import org.testng.annotations.Optional;
 import org.testng.annotations.Parameters;
 import org.testng.annotations.Test;
 
 import java.io.ByteArrayInputStream;
 import java.io.ByteArrayOutputStream;
 import java.io.IOException;
 import java.io.ObjectInput;
 import java.io.ObjectOutput;
import java.io.Serializable;
 import java.util.HashSet;
 import java.util.Set;
 
 import static org.testng.Assert.assertEquals;
 
 @Test(groups = "unit", sequential = true, testName = "loaders.cloud.CloudCacheStoreIntegrationTest")
 public class CloudCacheStoreIntegrationTest extends BaseCacheStoreTest {
 
    private String proxyHost;
    private String proxyPort = "-1";
    private int maxConnections = 20;
    private boolean isSecure = false;
    private String csBucket;
    private String cs2Bucket;
    private String accessKey;
    private String secretKey;
    private String service;
 
    private static final String sysUsername = System.getProperty("infinispan.jclouds.username");
    private static final String sysPassword = System.getProperty("infinispan.jclouds.password");
    private static final String sysService = System.getProperty("infinispan.jclouds.service");
 
    @BeforeTest
    @Parameters({"infinispan.jclouds.username", "infinispan.jclouds.password", "infinispan.jclouds.service"})
    protected void setUpClient(@Optional String JcloudsUsername,
                               @Optional String JcloudsPassword,
                               @Optional String JcloudsService) throws Exception {
 
       accessKey = (JcloudsUsername == null) ? sysUsername : JcloudsUsername;
       secretKey = (JcloudsPassword == null) ? sysPassword : JcloudsPassword;
       service = (JcloudsService == null) ? sysService : JcloudsService;
 
       if (accessKey == null || accessKey.trim().length() == 0 || secretKey == null || secretKey.trim().length() == 0) {
          accessKey = "dummy";
          secretKey = "dummy";
       }
       csBucket = (System.getProperty("user.name")
             + "." + this.getClass().getSimpleName()).toLowerCase();
       System.out.printf("accessKey: %1$s, bucket: %2$s%n", accessKey, csBucket);
 
       cs2Bucket = csBucket + "2";
    }
 
    protected CacheStore createCacheStore() throws Exception {
       return createAndStartCacheStore(csBucket);
    }
 
    protected CacheStore createAnotherCacheStore() throws Exception {
       return createAndStartCacheStore(cs2Bucket);
    }
 
    private CacheStore createAndStartCacheStore(String bucket) throws Exception {
       CloudCacheStore cs = new CloudCacheStore();
       CloudCacheStoreConfig cfg = new CloudCacheStoreConfig();
       cfg.setBucketPrefix(bucket);
       cfg.setCloudService(service);
       cfg.setIdentity(accessKey);
       cfg.setPassword(secretKey);
       cfg.setProxyHost(proxyHost);
       cfg.setProxyPort(proxyPort);
       cfg.setSecure(isSecure);
       cfg.setMaxConnections(maxConnections);
       cfg.setPurgeSynchronously(true); // for more accurate unit testing
       cs.init(cfg, new CacheDelegate("aName"), getMarshaller());
       cs.start();
       return cs;
    }
 
    /*  Changes below are needed to support testing of multiple cache stores */
 
    protected CacheStore cs2;
 
    @BeforeMethod
    @Override
    public void setUp() throws Exception {
       super.setUp();
       cs.clear();
       Set entries = cs.loadAll();
       assert entries.isEmpty();
       cs2 = createAnotherCacheStore();
       cs2.clear();
       entries = cs2.loadAll();
       assert entries.isEmpty();
    }
 
 
    @AfterMethod
    @Override
    public void tearDown() throws CacheLoaderException {
       if (cs != null) {
          cs.clear();
          cs.stop();
 
       }
       cs = null;
       if (cs2 != null) {
          cs2.clear();
 
          cs2.stop();
       }
       cs2 = null;
    }
 
 
    @SuppressWarnings("unchecked")
    @Override
    @Test (enabled = false, description = "Disabled until JClouds gains a proper streaming API")
    public void testStreamingAPI() throws IOException, ClassNotFoundException, CacheLoaderException {
       cs.store(InternalEntryFactory.create("k1", "v1", -1, -1));
       cs.store(InternalEntryFactory.create("k2", "v2", -1, -1));
       cs.store(InternalEntryFactory.create("k3", "v3", -1, -1));
 
       Marshaller marshaller = getMarshaller();
       ByteArrayOutputStream out = new ByteArrayOutputStream();
       ObjectOutput oo = marshaller.startObjectOutput(out, false);
       try {
          cs.toStream(new UnclosableObjectOutputStream(oo));
       } finally {
          marshaller.finishObjectOutput(oo);
          out.close();
       }
 
       ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());
       ObjectInput oi = marshaller.startObjectInput(in, false);
       try {
          cs2.fromStream(new UnclosableObjectInputStream(oi));
       } finally {
          marshaller.finishObjectInput(oi);
          in.close();
       }
 
       Set<InternalCacheEntry> set = cs2.loadAll();
       assertEquals(set.size(), 3);
       Set expected = new HashSet();
       expected.add("k1");
       expected.add("k2");
       expected.add("k3");
       for (InternalCacheEntry se : set) assert expected.remove(se.getKey());
       assert expected.isEmpty();
    }
 
   @Test (enabled = false, description = "Much too slow to run this on a live cloud setup")
    @Override public void testConcurrency() throws Exception {}
 
    public void testNegativeHashCodes() throws CacheLoaderException {
       ObjectWithNegativeHashcode objectWithNegativeHashcode = new ObjectWithNegativeHashcode();
       cs.store(InternalEntryFactory.create(objectWithNegativeHashcode, "hello", -1, -1));
       InternalCacheEntry ice = cs.load(objectWithNegativeHashcode);
       assert ice.getKey().equals(objectWithNegativeHashcode);
       assert ice.getValue().equals("hello");
    }
 
   private static class ObjectWithNegativeHashcode implements Serializable {
       String s = "hello";
 
       @Override
       public boolean equals(Object o) {
          if (this == o) return true;
          if (o == null || getClass() != o.getClass()) return false;
 
          ObjectWithNegativeHashcode blah = (ObjectWithNegativeHashcode) o;
          return !(s != null ? !s.equals(blah.s) : blah.s != null);
       }
 
       @Override
       public int hashCode() {
          return -700;
       }
    }
 
    @Override
    @Test (enabled = false, description = "Disabled until we can build the blobstore stub to retain state somewhere.")
    public void testStopStartDoesntNukeValues() throws InterruptedException, CacheLoaderException {
 
    }
 
    @SuppressWarnings("unchecked")
    @Override
    @Test (enabled = false, description = "Disabled until JClouds gains a proper streaming API")
    public void testStreamingAPIReusingStreams() throws IOException, ClassNotFoundException, CacheLoaderException {
       cs.store(InternalEntryFactory.create("k1", "v1", -1, -1));
       cs.store(InternalEntryFactory.create("k2", "v2", -1, -1));
       cs.store(InternalEntryFactory.create("k3", "v3", -1, -1));
 
       Marshaller marshaller = getMarshaller();
       ByteArrayOutputStream out = new ByteArrayOutputStream();
       byte[] dummyStartBytes = {1, 2, 3, 4, 5, 6, 7, 8};
       byte[] dummyEndBytes = {8, 7, 6, 5, 4, 3, 2, 1};
       ObjectOutput oo = marshaller.startObjectOutput(out, false);
       try {
          oo.write(dummyStartBytes);
          cs.toStream(new UnclosableObjectOutputStream(oo));
          oo.flush();
          oo.write(dummyEndBytes);
       } finally {
          marshaller.finishObjectOutput(oo);
          out.close();
       }
 
       // first pop the start bytes
       byte[] dummy = new byte[8];
       ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());
       ObjectInput oi = marshaller.startObjectInput(in, false);
       try {
          int bytesRead = oi.read(dummy, 0, 8);
          assert bytesRead == 8;
          for (int i = 1; i < 9; i++) assert dummy[i - 1] == i : "Start byte stream corrupted!";
          cs2.fromStream(new UnclosableObjectInputStream(oi));
          bytesRead = oi.read(dummy, 0, 8);
          assert bytesRead == 8;
          for (int i = 8; i > 0; i--) assert dummy[8 - i] == i : "Start byte stream corrupted!";
       } finally {
          marshaller.finishObjectInput(oi);
          in.close();
       }
 
       Set<InternalCacheEntry> set = cs2.loadAll();
       assertEquals(set.size(), 3);
       Set expected = new HashSet();
       expected.add("k1");
       expected.add("k2");
       expected.add("k3");
       for (InternalCacheEntry se : set) assert expected.remove(se.getKey());
       assert expected.isEmpty();
    }
 }
