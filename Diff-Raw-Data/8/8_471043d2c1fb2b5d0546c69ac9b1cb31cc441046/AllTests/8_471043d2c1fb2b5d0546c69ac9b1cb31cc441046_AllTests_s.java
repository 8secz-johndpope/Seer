 /**
  * Distribution License:
  * JSword is free software; you can redistribute it and/or modify it under
  * the terms of the GNU Lesser General Public License, version 2.1 as published by
  * the Free Software Foundation. This program is distributed in the hope
  * that it will be useful, but WITHOUT ANY WARRANTY; without even the
  * implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
  * See the GNU Lesser General Public License for more details.
  *
  * The License is available on the internet at:
  *       http://www.gnu.org/copyleft/lgpl.html
  * or by writing to:
  *      Free Software Foundation, Inc.
  *      59 Temple Place - Suite 330
  *      Boston, MA 02111-1307, USA
  *
  * Copyright: 2005
  *     The copyright to this program is held by it's authors.
  *
  */
 package org.crosswire.jsword.book.sword;
 
 import junit.framework.Test;
 import junit.framework.TestSuite;
 
 /**
  * JUnit Test.
  * 
  * @see gnu.lgpl.License for license details.<br>
  *      The copyright to this program is held by it's authors.
  * @author Joe Walker [joe at eireneh dot com]
  */
 public class AllTests {
     public static Test suite() {
         TestSuite suite = new TestSuite("Test for org.crosswire.jsword.book.sword");
         // $JUnit-BEGIN$
         suite.addTest(new TestSuite(ConfigEntryTableTest.class));
         suite.addTest(new TestSuite(GenBookTest.class));
         suite.addTest(new TestSuite(RawFileBackendTest.class));
         suite.addTest(new TestSuite(SwordBookDriverTest.class));
         suite.addTest(new TestSuite(SwordBookMetaDataTest.class));
         suite.addTest(new TestSuite(SwordBookTest.class));
         // $JUnit-END$
         return suite;
     }
 }
