 package org.test.zlibrary.model;
 
 import junit.framework.TestCase;
 
 import org.zlibrary.text.model.entry.ZLTextEntry;
 import org.zlibrary.text.model.impl.ZLModelFactory;
 
 public class TestTextEntry extends TestCase {
     private ZLModelFactory factory = new ZLModelFactory(); 
 
     public void test() {
         String str = "marina";
         ZLTextEntry entry = factory.createTextEntry(str);
        assertEquals(entry.getData(), str);
         assertEquals(entry.getDataLength(), str.length());
     }
 }
