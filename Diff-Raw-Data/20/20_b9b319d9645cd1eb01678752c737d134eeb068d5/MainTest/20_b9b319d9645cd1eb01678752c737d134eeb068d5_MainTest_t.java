 /*
  *
  * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
  *
  * Copyright 1997-2008 Sun Microsystems, Inc. All rights reserved.
  *
  * The contents of this file are subject to the terms of either the GNU
  * General Public License Version 2 only ("GPL") or the Common Development
  * and Distribution License("CDDL") (collectively, the "License").  You
  * may not use this file except in compliance with the License. You can obtain
  * a copy of the License at https://jersey.dev.java.net/CDDL+GPL.html
  * or jersey/legal/LICENSE.txt.  See the License for the specific
  * language governing permissions and limitations under the License.
  *
  * When distributing the software, include this License Header Notice in each
  * file and include the License file at jersey/legal/LICENSE.txt.
  * Sun designates this particular file as subject to the "Classpath" exception
  * as provided by Sun in the GPL Version 2 section of the License file that
  * accompanied this code.  If applicable, add the following below the License
  * Header, with the fields enclosed by brackets [] replaced by your own
  * identifying information: "Portions Copyrighted [year]
  * [name of copyright owner]"
  *
  * Contributor(s):
  *
  * If you wish your version of this file to be governed by only the CDDL or
  * only the GPL Version 2, indicate your decision by adding "[Contributor]
  * elects to include this software in this distribution under the [CDDL or GPL
  * Version 2] license."  If you don't indicate a single choice of license, a
  * recipient has the option to distribute your version of this file under
  * either the CDDL, the GPL Version 2 or to extend the choice of license to
  * its licensees as provided above.  However, if you add GPL Version 2 code
  * and therefore, elected the GPL Version 2 license, then the option applies
  * only if the new code is made subject to such option by the copyright
  * holder.
  */
 
 package com.sun.jersey.samples.helloworld;
 
 import com.sun.grizzly.http.SelectorThread;
 import com.sun.jersey.api.MediaTypes;
 import com.sun.jersey.api.client.Client;
 import com.sun.jersey.api.client.WebResource;
 import junit.framework.TestCase;
 
 /**
  *
  * @author Naresh
  */
 public class MainTest extends TestCase {
 
     SelectorThread threadSelector;
     private Client c;
     private WebResource wr;
 
     public MainTest(String testName) {
         super(testName);
     }
 
     @Override
     protected void setUp() throws Exception {
         super.setUp();
         threadSelector = Main.startServer();
         c = Client.create();
         wr = c.resource(Main.baseUri);
     }
 
     @Override
     protected void tearDown() throws Exception {
         super.tearDown();
         if (threadSelector != null) {
             threadSelector.stopEndpoint();
         }
     }
 
     /**
      * Test to see that the message "Hello World" is sent in the response.
      */
     public void testHelloWorld() throws Exception {
         String responseMsg = wr.path("helloworld").get(String.class);
         assertEquals("Hello World", responseMsg);
     }
 
     /**
      * Test if a WADL document is available at the relative path
      * "application.wadl".
      */
     public void testApplicationWadl() {
         String serviceWadl = wr.path("application.wadl").
                 accept(MediaTypes.WADL).get(String.class);
                 
         assertTrue(serviceWadl.length() > 0);
     }
 }
