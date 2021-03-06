 package org.biojava.bio.symbol;
 
 import junit.framework.*;
 import java.util.*;
import ca.mcgill.mcb.dog.overrepresentation.*;
 import org.biojava.bio.seq.*;
 
 /** List of tests for for UkkonenSuffixTree.
  * @author Francois Pepin
  * @version $Revision$
  */
 public class UkkonenSuffixTreeTest extends TestCase {
 
   UkkonenSuffixTree test;
   String mississippi= "mississippi";
   protected void setUp(){
     test = new UkkonenSuffixTree();
   }
 
 
   public static void main(String args[]){
     junit.textui.TestRunner.run(new TestSuite(UkkonenSuffixTreeTest.class));
   }
 
   public void testNumberNodes(){
     test.addSequence(mississippi, "name", false);
     //System.out.println(test.getRoot().children.size());
 
     assertEquals(19,test.getAllNodes(test.getRoot(), null, false).size());
     assertEquals(12,test.getAllNodes(test.getRoot(), null, true).size());
   }
 
   public void testSymbolListAddition(){
     SymbolList here=null;
     try{
       here = DNATools.createDNA("taccaccagga");
       test = new UkkonenSuffixTree((FiniteAlphabet)here.getAlphabet());
       test.addSymbolList(here, "name", false);
     }catch(IllegalSymbolException e){e.printStackTrace();}
     assertEquals(19,test.getAllNodes(test.getRoot(), null, false).size());
     assertEquals(12,test.getAllNodes(test.getRoot(), null, true).size());
   }
   
   public void testSymbolListConversion(){
     SymbolList here=null;
     SymbolList here2=null;
     try{
       here = DNATools.createDNA("taccaccagga");
       test = new UkkonenSuffixTree((FiniteAlphabet)here.getAlphabet());
       here2= test.stringToSymbolList(test.symbolListToString(here));
     }catch(IllegalSymbolException e){e.printStackTrace();}
     for (int i=1;i<=here.length();i++)
       assertEquals(here.symbolAt(i),here2.symbolAt(i));
   }
 
   /** adding twice the same string should be the same.
    */
   public void testDuplicateAddition(){
     test.addSequence(mississippi, "name", false);
     test.addSequence(mississippi, "name", false);
 
     assertEquals(19,test.getAllNodes(test.getRoot(), null, false).size());
     assertEquals(12,test.getAllNodes(test.getRoot(), null, true).size());
     }
 
 }
