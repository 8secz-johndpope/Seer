 package com.meterware.httpunit.javascript;
 /********************************************************************************************************************
  * $Id$
  *
  * Copyright (c) 2002, Russell Gold
  *
  * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
  * documentation files (the "Software"), to deal in the Software without restriction, including without limitation
  * the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
  * to permit persons to whom the Software is furnished to do so, subject to the following conditions:
  *
  * The above copyright notice and this permission notice shall be included in all copies or substantial portions
  * of the Software.
  *
  * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
  * THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
  * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF
  * CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
  * DEALINGS IN THE SOFTWARE.
  *
  *******************************************************************************************************************/
 import junit.textui.TestRunner;
 import junit.framework.TestSuite;
 import com.meterware.httpunit.*;
 
 /**
  *
  * @author <a href="mailto:russgold@httpunit.org">Russell Gold</a>
  **/
 public class HTMLElementTest  extends HttpUnitTest {
 
     public static void main( String args[] ) {
         TestRunner.run( suite() );
     }
 
 
     public static TestSuite suite() {
         return new TestSuite( HTMLElementTest.class );
     }
 
 
     public HTMLElementTest( String name ) {
         super( name );
     }
 
 
     public void testIDProperty() throws Exception {
         defineResource( "start.html",
                        "<html><head><script language='JavaScript'>" +
                        "function showTitle( id ) {" +
                       "   alert( 'element with id ' + id + ' has title ' + document.getElementById( id ).title );" +
                        "}" +
                        "function showAll() {" +
                        "    showTitle( 'there' ); showTitle( 'perform' ); showTitle( 'doIt' );" +
                        "    showTitle( 'grouping' ); showTitle( 'aCell' ); showTitle( 'myDiv' );\n" +
                        "}</script>" +
                        "</head><body onLoad='showAll();'>" +
                        "<div id=myDiv title=first><a href='somewhere' id='there' title=second>here</a>" +
                        "<table id=grouping title=third><tr><td id='aCell' title=fourth>" +
                        "<form id='perform' title=fifth><input type='submit' id='doIt' title=sixth></form>" +
                        "</td></tr></table></div>" );
         WebConversation wc = new WebConversation();
        wc.getResponse( getHostPath() + "/start.html" );
 
         assertElementTitle( wc, "there", "second" );
         assertElementTitle( wc, "perform", "fifth" );
         assertElementTitle( wc, "doIt", "sixth" );
         assertElementTitle( wc, "grouping", "third" );
         assertElementTitle( wc, "aCell", "fourth" );
         assertElementTitle( wc, "myDiv", "first" );
     }
 
 
     private void assertElementTitle( WebConversation wc, final String id, final String title ) {
         assertEquals( "element '" + id + "' message", "element with id " + id + " has title " + title, wc.popNextAlert() );
     }
 
 
 }
