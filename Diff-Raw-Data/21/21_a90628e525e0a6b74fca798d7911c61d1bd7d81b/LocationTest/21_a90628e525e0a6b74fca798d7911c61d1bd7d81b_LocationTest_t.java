 /*
  * Copyright (c) 2002, 2004 Gargoyle Software Inc. All rights reserved.
  *
  * Redistribution and use in source and binary forms, with or without
  * modification, are permitted provided that the following conditions are met:
  *
  * 1. Redistributions of source code must retain the above copyright notice,
  *    this list of conditions and the following disclaimer.
  * 2. Redistributions in binary form must reproduce the above copyright notice,
  *    this list of conditions and the following disclaimer in the documentation
  *    and/or other materials provided with the distribution.
  * 3. The end-user documentation included with the redistribution, if any, must
  *    include the following acknowledgment:
  *
  *       "This product includes software developed by Gargoyle Software Inc.
  *        (http://www.GargoyleSoftware.com/)."
  *
  *    Alternately, this acknowledgment may appear in the software itself, if
  *    and wherever such third-party acknowledgments normally appear.
  * 4. The name "Gargoyle Software" must not be used to endorse or promote
  *    products derived from this software without prior written permission.
  *    For written permission, please contact info@GargoyleSoftware.com.
  * 5. Products derived from this software may not be called "HtmlUnit", nor may
  *    "HtmlUnit" appear in their name, without prior written permission of
  *    Gargoyle Software Inc.
  *
  * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED WARRANTIES,
  * INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
  * FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL GARGOYLE
  * SOFTWARE INC. OR ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
  * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
  * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA,
  * OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
  * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
  * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE,
  * EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
  */
 package com.gargoylesoftware.htmlunit.javascript.host;
 
 import java.net.URL;
 import java.util.ArrayList;
 import java.util.Arrays;
 import java.util.Collections;
 import java.util.List;
 
 import com.gargoylesoftware.htmlunit.CollectingAlertHandler;
 import com.gargoylesoftware.htmlunit.MockWebConnection;
 import com.gargoylesoftware.htmlunit.WebClient;
 import com.gargoylesoftware.htmlunit.WebTestCase;
 import com.gargoylesoftware.htmlunit.html.HtmlPage;
 
 /**
  * Tests for Location.
  *
  * @version  $Revision$
  * @author  <a href="mailto:mbowler@GargoyleSoftware.com">Mike Bowler</a>
 * @author Michael Ottati
 * @author Marc Guillemot
  */
 public class LocationTest extends WebTestCase {
 
     /**
      * Create an instance
      * @param name The name of the test
      */
     public LocationTest( final String name ) {
         super(name);
     }
     
     /**
      * Regression test for bug 742902
      * @throws Exception if the test fails
      */
     public void testDocumentLocation() throws Exception {
         final WebClient webClient = new WebClient();
         final MockWebConnection webConnection = new MockWebConnection( webClient );
 
         final String firstContent
              = "<html><head><title>First</title><script>"
              + "function doTest() {\n"
              + "    alert(top.document.location);\n"
              + "}\n"
              + "</script></head><body onload='doTest()'>"
              + "</body></html>";
 
         webConnection.setResponse(
             URL_FIRST, firstContent, 200, "OK", "text/html", Collections.EMPTY_LIST );
         webClient.setWebConnection( webConnection );
 
         final List collectedAlerts = new ArrayList();
         webClient.setAlertHandler( new CollectingAlertHandler(collectedAlerts) );
 
         final HtmlPage firstPage = ( HtmlPage )webClient.getPage( URL_FIRST );
         assertEquals( "First", firstPage.getTitleText() );
 
         final List expectedAlerts = Collections.singletonList("http://first");
         assertEquals( expectedAlerts, collectedAlerts );
     }
     
     /**
      * @throws Exception if the test fails
      */
     public void testDocumentLocationHref() throws Exception {
         final WebClient webClient = new WebClient();
         final MockWebConnection webConnection = new MockWebConnection( webClient );
 
         final String firstContent
              = "<html><head><title>First</title><script>"
              + "function doTest() {\n"
              + "    alert(top.document.location.href);\n"
              + "}\n"
              + "</script></head><body onload='doTest()'>"
              + "</body></html>";
 
         webConnection.setResponse(
             URL_FIRST, firstContent, 200, "OK", "text/html", Collections.EMPTY_LIST );
         webClient.setWebConnection( webConnection );
 
         final List collectedAlerts = new ArrayList();
         webClient.setAlertHandler( new CollectingAlertHandler(collectedAlerts) );
 
         final HtmlPage firstPage = ( HtmlPage )webClient.getPage( URL_FIRST );
         assertEquals( "First", firstPage.getTitleText() );
 
         final List expectedAlerts = Collections.singletonList("http://first");
         assertEquals( expectedAlerts, collectedAlerts );
     }
     
     /**
      * @throws Exception if the test fails
      */
     public void testLocation_variousAttributes() throws Exception {
         final WebClient client = new WebClient();
         final MockWebConnection webConnection = new MockWebConnection( client );
 
         final String firstContent
              = "<html><head><title>First</title><script>\n"
              + "function doTest() {\n"
              + "    var location = document.location;"
              + "    alert(location.hash);\n"
              + "    alert(location.host);\n"
              + "    alert(location.hostname);\n"
              + "    alert(location.href);\n"
              + "    alert(location.pathname);\n"
              + "    alert(location.port);\n"
              + "    alert(location.protocol);\n"
              + "    alert(location.search);\n"
              + "}\n</script></head>"
              + "<body onload='doTest()'></body></html>";
 
         webConnection.setDefaultResponse( firstContent );
         client.setWebConnection( webConnection );
 
         final List collectedAlerts = new ArrayList();
         client.setAlertHandler( new CollectingAlertHandler(collectedAlerts) );
 
         List expectedAlerts;
 
         // Try page with only a server name
         client.getPage( new URL("http://first") );
         expectedAlerts = Arrays.asList( new String[]{
             "",               // hash
             "first",          // host
             "first",          // hostname
             "http://first",   // href
             "",               // pathname
             "",               // port
            "http:",           // protocol
             ""                // search
         } );
         assertEquals( "simple url", expectedAlerts, collectedAlerts );
 
         collectedAlerts.clear();
         
         // Try page with all the appropriate parts
         client.getPage( new URL("http://www.first:77/foo?bar#wahoo") );
         expectedAlerts = Arrays.asList( new String[]{
             "wahoo",                             // hash 
             "www.first:77",                      // host
             "www.first",                         // hostname
             "http://www.first:77/foo?bar#wahoo", // href
             "/foo",                              // pathname
             "77",                                // port
            "http:",                              // protocol 
             "?bar"                               // search
         } );
         assertEquals( "complete url", expectedAlerts, collectedAlerts );
     }
 }
