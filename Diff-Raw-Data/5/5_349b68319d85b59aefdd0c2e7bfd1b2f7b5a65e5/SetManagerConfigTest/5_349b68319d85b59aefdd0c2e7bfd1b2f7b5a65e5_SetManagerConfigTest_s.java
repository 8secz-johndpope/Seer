 // Copyright (C) 2006 Google Inc.
 //
 // Licensed under the Apache License, Version 2.0 (the "License");
 // you may not use this file except in compliance with the License.
 // You may obtain a copy of the License at
 //
 //      http://www.apache.org/licenses/LICENSE-2.0
 //
 // Unless required by applicable law or agreed to in writing, software
 // distributed under the License is distributed on an "AS IS" BASIS,
 // WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 // See the License for the specific language governing permissions and
 // limitations under the License.
 
 package com.google.enterprise.connector.instantiator;
 
 import com.google.enterprise.connector.manager.Context;
 
 import junit.framework.Assert;
 import junit.framework.TestCase;
 
 import org.json.JSONException;
 
 import java.io.FileInputStream;
 import java.io.IOException;
 import java.io.InputStream;
 import java.util.Properties;
 
 /**
  * 
  */
 public class SetManagerConfigTest extends TestCase {
 
   private static final String TEST_DIR =
       "testdata/contextTests/setManagerConfig/";
   private static final String APPLICATION_CONTEXT = "applicationContext.xml";
   private static final String APPLICATION_PROPERTIES =
       "applicationContext.properties";
 
  public final void testsetConnectorManagerConfig() throws JSONException,
       InstantiatorException, IOException {
     String propFileName = TEST_DIR + APPLICATION_PROPERTIES;
     Context context = Context.getInstance();
     context.setJunitContextLocation(TEST_DIR + APPLICATION_CONTEXT);
     context.setJunitContext();
     context.setFeeding(false);
     Assert.assertTrue(true);
 
     String host = null;
     int port = -1;
 
     {
       Properties props = loadProperties(propFileName);
       host = (String) props.get(Context.GSA_FEED_HOST_PROPERTY_KEY);
       port =
           Integer.valueOf(
               (String) props.get(Context.GSA_FEED_PORT_PROPERTY_KEY))
               .intValue();
     }
     System.out.println("Host = " + host);
     System.out.println("Port = " + port);
 
     context.setConnectorManagerConfig(true, "shme", 14);
     verifyPropsValues("shme", 14, propFileName);
     
     context.setConnectorManagerConfig(true, host, port);
     verifyPropsValues(host, port, propFileName);
   }
 
   private void verifyPropsValues(String expectedHost, int expectedPort,
       String propFileName) throws IOException {
     Properties props = loadProperties(propFileName);
     String actualHost = (String) props.get(Context.GSA_FEED_HOST_PROPERTY_KEY);
     int actualPort =
         Integer.valueOf((String) props.get(Context.GSA_FEED_PORT_PROPERTY_KEY))
             .intValue();
     Assert.assertEquals(expectedHost, actualHost);
     Assert.assertEquals(expectedPort, actualPort);
   }
 
   private Properties loadProperties(String propFileName) throws IOException {
     Properties props = new Properties();
     InputStream inStream = new FileInputStream(propFileName);
     props.load(inStream);
     return props;
   }
 }
