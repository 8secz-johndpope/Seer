 /*
  * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
  * 
  * Copyright 2007 Sun Microsystems, Inc. All rights reserved. 
  * 
  * The contents of this file are subject to the terms of the Common Development
  * and Distribution License("CDDL") (the "License").  You may not use this file
  * except in compliance with the License. 
  * 
  * You can obtain a copy of the License at:
  *     https://jersey.dev.java.net/license.txt
  * See the License for the specific language governing permissions and
  * limitations under the License.
  *
  * When distributing the Covered Code, include this CDDL Header Notice in each
  * file and include the License file at:
  *     https://jersey.dev.java.net/license.txt
  * If applicable, add the following below this CDDL Header, with the fields
  * enclosed by brackets [] replaced by your own identifying information:
  *     "Portions Copyrighted [year] [name of copyright owner]"
  */
 
 package com.sun.ws.rest.impl;
 
 import javax.ws.rs.core.Response;
 import junit.framework.TestCase;
 
 /**
  *
  * @author Paul.Sandoz@Sun.Com
  */
 public class ResponseBuilderTest extends TestCase {
     
     public ResponseBuilderTest(String testName) {
         super(testName);
     }
         
     public void testInvalidMediaType() {
         boolean caught = false;
         try {
             Response.ok(null, "abcd");
         } catch (IllegalArgumentException e) {
             caught = true;
         }
         assertTrue(caught);
         
         caught = false;
         try {
             Response.ok(null).type("abcd");
         } catch (IllegalArgumentException e) {
             caught = true;
         }
         assertTrue(caught);
     }
     
     
 }
