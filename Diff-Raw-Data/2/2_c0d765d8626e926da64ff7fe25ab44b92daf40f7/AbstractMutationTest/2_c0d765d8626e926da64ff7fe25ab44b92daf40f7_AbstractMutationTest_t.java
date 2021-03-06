 /*
  * See the NOTICE file distributed with this work for additional
  * information regarding copyright ownership.
  *
  * This is free software; you can redistribute it and/or modify it
  * under the terms of the GNU Lesser General Public License as
  * published by the Free Software Foundation; either version 2.1 of
  * the License, or (at your option) any later version.
  *
  * This software is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
  * Lesser General Public License for more details.
  *
  * You should have received a copy of the GNU Lesser General Public
  * License along with this software; if not, write to the Free
  * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
  * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
  */
 package org.xwiki.gwt.dom.mutation.client;
 
 import com.google.gwt.dom.client.Document;
 import com.google.gwt.dom.client.Element;
 import com.google.gwt.junit.DoNotRunWith;
 import com.google.gwt.junit.Platform;
 import com.google.gwt.junit.client.GWTTestCase;
 
 /**
  * Base class for all Mutation tests. It returns the name of the module in {@link #getModuleName()} so you don't have to
  * do it in each test.
  * <p>
  * Note: Unfortunately HtmlUnit doesn't support DOM mutations at this moment.
  * 
  * @version $Id: ee2843175d2f7f69c9eb7d9d1c6c4bec5e3a2af2 $
  */
 @DoNotRunWith(Platform.HtmlUnitBug)
public abstract class AbstractMutationTest extends GWTTestCase
 {
     /**
      * The document in which we run the tests.
      */
     private Document document;
 
     /**
      * The DOM element in which we run the tests.
      */
     private Element container;
 
     /**
      * {@inheritDoc}
      * 
      * @see GWTTestCase#gwtSetUp()
      */
     protected void gwtSetUp() throws Exception
     {
         super.gwtSetUp();
 
         document = Document.get();
         container = document.createDivElement();
         document.getBody().appendChild(container);
     }
 
     /**
      * {@inheritDoc}
      * 
      * @see GWTTestCase#gwtTearDown()
      */
     protected void gwtTearDown() throws Exception
     {
         super.gwtTearDown();
 
         container.getParentNode().removeChild(container);
     }
 
     /**
      * @return the document in which we run the tests
      */
     protected Document getDocument()
     {
         return document;
     }
 
     /**
      * @return the DOM element in which we run the tests
      */
     protected Element getContainer()
     {
         return container;
     }
 
     /**
      * {@inheritDoc}
      * 
      * @see GWTTestCase#getModuleName()
      */
     public String getModuleName()
     {
         return "org.xwiki.gwt.dom.mutation.Mutation";
     }
 }
