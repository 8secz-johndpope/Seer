 /*
  * $Header$
  * $Revision$
  * $Date$
  *
  * ====================================================================
  *
  * Copyright (C) 2005 Elliotte Rusty Harold
  * All rights reserved.
  *
  * Redistribution and use in source and binary forms, with or without
  * modification, are permitted provided that the following conditions
  * are met:
  * 
  * 1. Redistributions of source code must retain the above copyright
  *    notice, this list of conditions, and the following disclaimer.
  *
  * 2. Redistributions in binary form must reproduce the above copyright
  *    notice, this list of conditions, and the disclaimer that follows 
  *    these conditions in the documentation and/or other materials 
  *    provided with the distribution.
  *
  * 3. The name "Jaxen" must not be used to endorse or promote products
  *    derived from this software without prior written permission.  For
  *    written permission, please contact license@jaxen.org.
  * 
  * 4. Products derived from this software may not be called "Jaxen", nor
  *    may "Jaxen" appear in their name, without prior written permission
  *    from the Jaxen Project Management (pm@jaxen.org).
  * 
  * In addition, we request (but do not require) that you include in the 
  * end-user documentation provided with the redistribution and/or in the 
  * software itself an acknowledgement equivalent to the following:
  *     "This product includes software developed by the
  *      Jaxen Project (http://www.jaxen.org/)."
  * Alternatively, the acknowledgment may be graphical using the logos 
  * available at http://www.jaxen.org/
  *
  * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
  * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
  * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
  * DISCLAIMED.  IN NO EVENT SHALL THE Jaxen AUTHORS OR THE PROJECT
  * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
  * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
  * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
  * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
  * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
  * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
  * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
  * SUCH DAMAGE.
  *
  * ====================================================================
  * This software consists of voluntary contributions made by many 
  * individuals on behalf of the Jaxen Project and was originally 
  * created by bob mcwhirter <bob@werken.com> and 
  * James Strachan <jstrachan@apache.org>.  For more information on the 
  * Jaxen Project, please see <http://www.jaxen.org/>.
  * 
  * $Id$
  */
 
 package org.jaxen.function;
 
 import java.io.IOException;
 import java.util.List;
 
 import javax.xml.parsers.DocumentBuilder;
 import javax.xml.parsers.DocumentBuilderFactory;
 import javax.xml.parsers.ParserConfigurationException;
 
 import junit.framework.TestCase;
 
 import org.jaxen.FunctionCallException;
 import org.jaxen.JaxenException;
 import org.jaxen.XPath;
 import org.jaxen.dom.DOMXPath;
 import org.w3c.dom.Attr;
 import org.w3c.dom.Document;
 import org.w3c.dom.ProcessingInstruction;
 import org.w3c.dom.Text;
 import org.xml.sax.SAXException;
 
 /**
  * @author Elliotte Rusty Harold
  *
  */
 public class NamespaceURITest extends TestCase {
 
     private Document doc;
     
     public void setUp() throws ParserConfigurationException, SAXException, IOException
     {
         DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
         factory.setNamespaceAware(true);
         DocumentBuilder builder = factory.newDocumentBuilder();
         doc = builder.parse( "xml/basic.xml" );
     }
 
 
     public NamespaceURITest(String name) {
         super(name);
     }
 
    public void testNamespaceURIOfNumber()
     {
         try
         {
             XPath xpath = new DOMXPath( "namespace-uri(3)" );
             xpath.selectNodes( doc );
             fail("namespace-uri of non-node-set");
         }
         catch (FunctionCallException e) 
         {
            assertEquals("The argument to the namespace-uri function must be a node-set", e.getMessage());
         }
        catch (Exception e)
        {
            e.printStackTrace();
            fail( e.getMessage() );
        }
     }    
 
     public void testNamespaceURINoArguments() throws JaxenException
     {
         XPath xpath = new DOMXPath( "namespace-uri()" );
         List results = xpath.selectNodes( doc );
         assertEquals("", results.get(0));
     }    
     
     public void testNamespaceURIOfEmptyNodeSetIsEmptyString() throws JaxenException
     {
         XPath xpath = new DOMXPath( "namespace-uri(/aaa)" );
         String result = (String) xpath.evaluate(doc);
         assertEquals("", result);
     }    
 
     public void testNamespaceURIOfProcessingInstructionIsEmptyString() throws JaxenException
     {
         XPath xpath = new DOMXPath( "namespace-uri(/processing-instruction())" );
         ProcessingInstruction pi = doc.createProcessingInstruction("target", "value");
         doc.appendChild(pi);
         String result = (String) xpath.evaluate(doc);
         assertEquals("", result);
     }    
 
     public void testNamespaceURIOfAttribute() throws JaxenException
     {
         XPath xpath = new DOMXPath( "namespace-uri(/*/@*)" );
         Attr a = doc.createAttribute("name");
         doc.getDocumentElement().setAttributeNode(a);
         Object result = (String) xpath.evaluate(doc);
         assertEquals("", result);
     }    
 
     public void testNamespaceURIOfTextIsEmptyString() throws JaxenException
     {
         XPath xpath = new DOMXPath( "namespace-uri(/*/text())" );
         Text c = doc.createTextNode("test");
         doc.getDocumentElement().appendChild(c);
         String result = (String) xpath.evaluate(doc);
         assertEquals("", result);
     }    
 
     public void testNamespaceURIRequiresAtMostOneArgument() throws JaxenException
     {
         XPath xpath = new DOMXPath( "namespace-uri(/*, /*)" );
         try {
             xpath.evaluate(doc);
             fail("Allowed namespace-uri function with no arguments");
         }
         catch (FunctionCallException ex) {
             assertNotNull(ex.getMessage());
         }
     }    
 
    public void testNamespaceURIOfNamespaceIsPrefix() throws JaxenException
     {
         XPath xpath = new DOMXPath( "namespace-uri(/*/namespace::node())" );
         String result = (String) xpath.evaluate(doc);
        assertEquals("http://www.w3.org/XML/1998/namespace", result);
     }    
 }
