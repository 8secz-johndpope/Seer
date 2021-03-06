 /*
  * Copyright 2006 The Apache Software Foundation.
  *
  *  Licensed under the Apache License, Version 2.0 (the "License");
  *  you may not use this file except in compliance with the License.
  *  You may obtain a copy of the License at
  *
  *      http://www.apache.org/licenses/LICENSE-2.0
  *
  *  Unless required by applicable law or agreed to in writing, software
  *  distributed under the License is distributed on an "AS IS" BASIS,
  *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  *  See the License for the specific language governing permissions and
  *  limitations under the License.
  *
  */
 package javax.xml.crypto.test.dsig;
 
 import java.io.File;
 import java.security.Security;
 import org.w3c.dom.Document;
 import org.w3c.dom.Element;
 import org.w3c.dom.NodeList;
 
 import junit.framework.*;
 
 import javax.xml.crypto.test.KeySelectors;
 import javax.xml.crypto.dsig.dom.DOMValidateContext;
 
 /**
  * This is a testcase that validates various signatures
  *
  * @author Sean Mullan
  */
 public class ValidateSignatureTest extends TestCase {
 
     private SignatureValidator validator;
     private File dir;
 
     static {
         Security.insertProviderAt
             (new org.jcp.xml.dsig.internal.dom.XMLDSigRI(), 1);
     }
 
     public ValidateSignatureTest(String name) {
         super(name);
 	String fs = System.getProperty("file.separator");
 	dir = new File(System.getProperty("basedir") + fs + "data" + fs 
 	    + "javax" + fs + "xml" + fs + "crypto", "dsig");
 	validator = new SignatureValidator(dir);
     }
 
     /** 
      * Validates a signature that references an element with an ID attribute. 
      * The element's ID needs to be registered so that it can be found.
      */
     public void test_signature_with_ID() throws Exception {
         String file = "envelopingSignature.xml";
 
 	DOMValidateContext vc = validator.getValidateContext
 	    (file, new KeySelectors.KeyValueKeySelector());
 	Document doc = vc.getNode().getOwnerDocument();
 	NodeList nl = doc.getElementsByTagName("Assertion");
 	vc.setIdAttributeNS((Element) nl.item(0), null, "AssertionID");
 	boolean coreValidity = validator.validate(vc);
 	assertTrue("Signature failed core validation", coreValidity);
     }
     
     public static void main(String[] args) throws Exception {
         ValidateSignatureTest vst = new ValidateSignatureTest("");
         vst.test_signature_with_ID();
     }
 }
