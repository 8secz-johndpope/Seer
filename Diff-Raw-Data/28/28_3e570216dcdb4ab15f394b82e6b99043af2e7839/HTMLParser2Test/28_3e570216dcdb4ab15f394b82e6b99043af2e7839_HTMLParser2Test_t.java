 /*
  * Copyright (c) 2002-2012 Gargoyle Software Inc.
  *
  * Licensed under the Apache License, Version 2.0 (the "License");
  * you may not use this file except in compliance with the License.
  * You may obtain a copy of the License at
  * http://www.apache.org/licenses/LICENSE-2.0
  *
  * Unless required by applicable law or agreed to in writing, software
  * distributed under the License is distributed on an "AS IS" BASIS,
  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  * See the License for the specific language governing permissions and
  * limitations under the License.
  */
 package com.gargoylesoftware.htmlunit.html;
 
 import org.junit.Test;
 import org.junit.runner.RunWith;
 
 import com.gargoylesoftware.htmlunit.BrowserRunner;
 import com.gargoylesoftware.htmlunit.BrowserRunner.Alerts;
 import com.gargoylesoftware.htmlunit.BrowserRunner.NotYetImplemented;
 import com.gargoylesoftware.htmlunit.WebDriverTestCase;
 
 /**
  * Test class for {@link HTMLParser}.
  *
  * @version $Revision$
  * @author Ahmed Ashour
  * @author Ronald Brill
  * @author Marc Guillemot
  */
 @RunWith(BrowserRunner.class)
 public class HTMLParser2Test extends WebDriverTestCase {
 
     /**
      * @throws Exception failure
      */
     @Test
     public void qualified_body() throws Exception {
         final String html = "<html><body>"
             + "<wicket:body>whatever</wicket:body>\n"
             + "</body></html>";
         loadPage2(html);
     }
 
     /**
      * Malformed HTML:
      * &lt;/td&gt;some text&lt;/tr&gt; =&gt; text comes before the table.
      *
      * @throws Exception on test failure
      */
     @Test
     @Alerts({ "before", "after", "TABLE" })
     public void testHtmlTableTextAroundTD() throws Exception {
         final String html = "<html><head><title>test_Table</title>\n"
             + "<script>\n"
             + "function test() {\n"
             + "  var tmp = document.getElementById('testDiv');\n"
             + "  tmp = tmp.firstChild;\n"
             + "  alert(tmp.data)\n"
             + "  tmp = tmp.nextSibling;\n"
             + "  alert(tmp.data)\n"
             + "  tmp = tmp.nextSibling;\n"
             + "  alert(tmp.tagName)\n"
             + "}\n"
             + "</script>\n"
             + "</head>\n"
             + "<body onload='test()'><div id='testDiv'>"
             + "<table><tr>before<td></td>after</tr></table>"
             + "</div></body></html>";
 
         loadPageWithAlerts2(html);
     }
 
     /**
      * @throws Exception on test failure
      */
     @Test
     @Alerts("TABLE")
     public void testHtmlTableWhitespaceAroundTD() throws Exception {
         final String html = "<html><head><title>test_Table</title>\n"
             + "<script>\n"
             + "function test() {\n"
             + "  var tmp = document.getElementById('testDiv');\n"
             + "  tmp = tmp.firstChild;\n"
             + "  alert(tmp.tagName)\n"
             + "}\n"
             + "</script>\n"
             + "</head>\n"
             + "<body onload='test()'><div id='testDiv'>"
             + "<table><tr> <td></td> </tr></table>"
             + "</div></body></html>";
 
         loadPageWithAlerts2(html);
     }
 
     /**
      * @throws Exception on test failure
      */
     @Test
     @Alerts(FF = "Hi!")
     @NotYetImplemented
     public void unclosedCommentsInScript() throws Exception {
         final String html = "<html><body>\n"
             + "<script><!--\n"
             + "alert('Hi!');\n"
             + "</script>\n"
             + "<h1>Ho!</h1>\n"
             + "<!-- some comment -->\n"
             + "<h1>Hu!</h1>\n"
             + "</body></html>";
 
         loadPageWithAlerts2(html);
     }

    /**
     * This tests for an bug in nekohtml.
     * @throws Exception on test failure
     */
    @Test
    @Alerts({ "[object HTMLParagraphElement]", "[object HTMLButtonElement]", "[object HTMLDivElement]" })
    @NotYetImplemented
    public void divInsideButtonInsideParagraph() throws Exception {
        final String html = "<html><head>\n"
            + "<script>\n"
            + "function test() {\n"
            + "  var tmp = document.getElementById('myP');\n"
            + "  alert(tmp)\n"
            + "  tmp = tmp.firstChild;\n"
            + "  alert(tmp)\n"
            + "  tmp = tmp.firstChild;\n"
            + "  alert(tmp)\n"
            + "}\n"
            + "</script>\n"
            + "</head>\n"
            + "<body onload='test()'>\n"
            + "<p id='myP'><button><div>Test<div></button></p>\n"
            + "</p>\n"
            + "</body></html>";

        loadPageWithAlerts2(html);
    }
 }
