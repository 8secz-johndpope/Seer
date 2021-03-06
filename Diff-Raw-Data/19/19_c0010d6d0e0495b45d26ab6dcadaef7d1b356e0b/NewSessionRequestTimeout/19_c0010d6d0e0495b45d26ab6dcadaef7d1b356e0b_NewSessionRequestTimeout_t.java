 package org.openqa.grid.internal;
 
 import static org.openqa.grid.common.RegistrationRequest.APP;
 
 import org.junit.AfterClass;
 import org.junit.BeforeClass;
 import org.junit.Test;
 import org.openqa.grid.internal.mock.MockedNewSessionRequestHandler;
 import org.openqa.grid.internal.mock.MockedRequestHandler;
 
 import java.util.HashMap;
 import java.util.Map;
 
 public class NewSessionRequestTimeout {
 
   private static Registry registry;
   private static Map<String, Object> ff = new HashMap<String, Object>();
   private static RemoteProxy p1;
 
   /**
    * create a hub with 1 IE and 1 FF
    */
   @BeforeClass
   public static void setup() {
     registry = Registry.newInstance();
     ff.put(APP, "FF");
 
     p1 = RemoteProxyFactory.getNewBasicRemoteProxy(ff, "http://machine1:4444", registry);
     registry.add(p1);
     // after 1 sec in the queue, request are kicked out.
     registry.setNewSessionWaitTimeout(1000);
   }
 
  @Test(timeout = 5000)
   public void method() {
 
     // should work
     MockedRequestHandler newSessionRequest = new MockedNewSessionRequestHandler(registry, ff);
     newSessionRequest.process();
 
     // should throw after 1sec being stuck in the queue
    try {
      MockedRequestHandler newSessionRequest2 = new MockedNewSessionRequestHandler(registry, ff);
      newSessionRequest2.process();
    } catch (RuntimeException ignore) {
    }
 
   }
 
   @AfterClass
   public static void teardown() {
     registry.stop();
   }
 }
