 package com.hmsonline.virgil;
 
 import org.junit.AfterClass;
 import org.junit.BeforeClass;
 
import com.hmsonline.cassandra.triggers.CommitLog;
import com.hmsonline.cassandra.triggers.ConfigurationStore;
import com.hmsonline.cassandra.triggers.TriggerStore;

 public abstract class VirgilServerTest {
     static Thread serverThread = null;
 
     @BeforeClass
     public static void setup() throws Exception {
         serverThread = new Thread(new EmbeddableServer(new String[] { "server", "src/test/resources/virgil.yaml",
                 "-embedded" }));
         serverThread.start();
         Thread.sleep(10000);
     }
 
     @AfterClass
     public static void tearDown() throws Exception {
         serverThread.interrupt();
     }
 }
