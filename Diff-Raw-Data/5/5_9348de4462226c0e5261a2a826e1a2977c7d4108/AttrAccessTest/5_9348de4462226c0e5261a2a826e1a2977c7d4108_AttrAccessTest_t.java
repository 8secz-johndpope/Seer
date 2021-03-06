 /**
  * Razvan's public code. Copyright 2008 based on Apache license (share alike) see LICENSE.txt for
  * details. No warranty implied nor any liability assumed for this code.
  */
 package com.razie.pub.base.test;
 
import razie.base.AttrAccess;
import razie.base.AttrAccessImpl;
 import junit.framework.TestCase;
 
 
 /**
  * test the attr implementation
  * 
  * @author razvanc99
  */
 public class AttrAccessTest extends TestCase {
 
     public void testStringDefn() throws InterruptedException {
         AttrAccess aa = new AttrAccessImpl();
         aa.setAttr("attr1", "val1");
         check(aa, 1);
     }
 
     public void testSimpleDefn() throws InterruptedException {
         AttrAccess aa = new AttrAccessImpl();
         aa.setAttr("attr1:string", "val1");
         aa.setAttr("attr2:int", "2");
         check(aa, 2);
     }
 
     public void testEscapedDefn() throws InterruptedException {
         AttrAccess aa = new AttrAccessImpl();
         aa.setAttr("attr1\\:string", "val1");
         aa.setAttr("attr2\\:string:int", "2");
         assertTrue(aa.isPopulated("attr1:string"));
         assertTrue(aa.isPopulated("attr2:string"));
     }
 
     public void testComplexDefn() throws InterruptedException {
         AttrAccess aa = new AttrAccessImpl("attr1:string", "val1", "attr2:int", "2");
         check(aa, 2);
     }
 
     public void testOneLineDefn() throws InterruptedException {
         AttrAccess aa = new AttrAccessImpl("attr1:string=val1,attr2:int=2");
         check(aa, 2);
     }
 
     public void testOneLine2Defn() throws InterruptedException {
         AttrAccess aa = new AttrAccessImpl("attr1=val1,attr2:int=2");
         check(aa, 2);
     }
     
     private void check(AttrAccess aa, int howmuch) {
         assertTrue(aa.isPopulated("attr1"));
 
         if (howmuch >= 2)
             assertTrue(aa.isPopulated("attr2"));
 
         assertTrue(AttrAccess.AttrType.STRING.equals(aa.getAttrType("attr1")));
         if (howmuch >= 2)
             assertTrue(AttrAccess.AttrType.INT.equals(aa.getAttrType("attr2")));
 
         assertTrue("val1".equals(aa.getAttr("attr1")));
         if (howmuch >= 2)
             assertTrue("2".equals(aa.getAttr("attr2")));
     }
 
 }
