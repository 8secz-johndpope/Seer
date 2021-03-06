 /*
  * Copyright 1999,2006 The Apache Software Foundation.
  * 
  * Licensed under the Apache License, Version 2.0 (the "License");
  * you may not use this file except in compliance with the License.
  * You may obtain a copy of the License at
  * 
  *      http://www.apache.org/licenses/LICENSE-2.0
  * 
  * Unless required by applicable law or agreed to in writing, software
  * distributed under the License is distributed on an "AS IS" BASIS,
  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  * See the License for the specific language governing permissions and
  * limitations under the License.
  */
 package org.apache.xmlrpc.test;
 
 import java.io.IOException;
 
 import org.apache.xmlrpc.XmlRpcException;
 import org.apache.xmlrpc.client.XmlRpcClient;
 import org.apache.xmlrpc.client.util.ClientFactory;
 import org.apache.xmlrpc.server.XmlRpcHandlerMapping;
 
 
 /** Test case for the {@link ClientFactory}.
  */
 public class DynamicProxyTest extends XmlRpcTestCase {
     /** An interface, which is being implemented by the
      * server.
      */
     public interface Adder {
         /** Returns the sum of the given integers.
          */
         public int add(int pNum1, int pNum2);
     }
 
    /** Implementation of {@link DynamicProxyTest.Adder}, which is used by
      * the server.
      */
     public static class AdderImpl implements Adder {
         public int add(int pNum1, int pNum2) {
             return pNum1 + pNum2;
         }
     }
 
     protected XmlRpcHandlerMapping getHandlerMapping() throws IOException, XmlRpcException {
         return getHandlerMapping("DynamicProxyTest.properties");
     }
 
     private ClientFactory getClientFactory(ClientProvider pProvider) throws Exception {
         XmlRpcClient client = pProvider.getClient();
         client.setConfig(getConfig(pProvider));
         return new ClientFactory(client);
     }
 
     /** Tests calling the {@link Adder#add(int,int)} method
      * by using an object, which has been created by the
      * {@link ClientFactory}.
      */
     public void testAdderCall() throws Exception {
         for (int i = 0;  i < providers.length;  i++) {
             testAdderCall(providers[i]);
         }
     }
 
     private void testAdderCall(ClientProvider pProvider) throws Exception {
         ClientFactory factory = getClientFactory(pProvider);
         Adder adder = (Adder) factory.newInstance(Adder.class);
         assertEquals(6, adder.add(2, 4));
     }
 }
