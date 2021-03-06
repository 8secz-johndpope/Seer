 package org.biojava.bio.symbol;
 
 import junit.framework.TestCase;
 import junit.framework.TestSuite;
 
 import org.biojava.bio.seq.DNATools;
 
 /** List of tests for for UkkonenSuffixTree.
  * @author Francois Pepin
  * @version $Revision$
  */
 public class UkkonenSuffixTreeTest extends TestCase {
 
   public UkkonenSuffixTreeTest(String name){
     super(name);
   }
 
   UkkonenSuffixTree test;
   String mississippi= "mississippi";
   protected void setUp(){
     test = new UkkonenSuffixTree();
   }
 
 
   public static void main(String args[]){
     junit.textui.TestRunner.run(new TestSuite(UkkonenSuffixTreeTest.class));
   }
  
  public void testMultiple() {
	  // Bug #2260
	  try {
		  new UkkonenSuffixTree(mississippi+"$"+mississippi);
	  } catch (NullPointerException e) {
		  e.printStackTrace();
	  }
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
 
   /*public void testSymbolListConversion(){
     SymbolList here=null;
     SymbolList here2=null;
     try{
       here = DNATools.createDNA("taccaccagga");
       test = new UkkonenSuffixTree((FiniteAlphabet)here.getAlphabet());
       //here2= test.stringToSymbolList(test.symbolListToString(here));
     }catch(IllegalSymbolException e){e.printStackTrace();}
     for (int i=1;i<=here.length();i++)
       assertEquals(here.symbolAt(i),here2.symbolAt(i));
       }*/
 
   // adding twice the same string should be the same
   public void testDuplicateAddition(){
     test.addSequence(mississippi, "name", false);
     test.addSequence(mississippi, "name", false);
 
     assertEquals(19,test.getAllNodes(test.getRoot(), null, false).size());
     assertEquals(12,test.getAllNodes(test.getRoot(), null, true).size());
   }
 
   public void testGetterMethods()
   {
     SymbolList here=null;
     try{
       here = DNATools.createDNA("taccaccagga");
       test = new UkkonenSuffixTree((FiniteAlphabet)here.getAlphabet());
       test.addSymbolList(here, "name", false);
     }catch(IllegalSymbolException e){e.printStackTrace();}
     assertTrue(test.getRoot().getParent()==null);
     //bad test that mixes SymbolLists and CharSequence, but it still works.
     assertTrue(test.getRoot().hasChild(new Character('g')));
     assertTrue(test.getRoot().getChild(new Character(UkkonenSuffixTree.DEFAULT_TERM_CHAR)).isTerminal());
   }
 
 
   public void testlongAddition(){
     test.addSequence(dna, "name", false);
     int before = test.getAllNodes(test.getRoot(), null, false).size();
 
     test.addSequence(dna, "name", false);
     int after = test.getAllNodes(test.getRoot(), null, false).size();
 
     assertEquals(before, after);
 
     //test.printTree();
   }
    private String dna=
    "GGCTCAGTCCCGACGTGGAACTCAGCAGCGGAGGCTGGACGCTTGCATGGCGCTTGAGGCAAGTTCGGGGCTCATTTTGGAAGTTTTCTTTCTAGCACAGACATCCAACTCTGCTCCTATGCCAGTCAGATAATTAAGGAATTAAGTAATAATTGTGCTCTGCAAATTATGATAGTGATCTGTATTTACTACGTGCATATATTTTGGGCCAGTGAATTTTTTTCTAAGCTAATATAGTTATTTGGACTTTTGACATGACTTTGTGTTTAATTAAAACAAAAAAAGAAATTGCAGAAGTGTTGTAAGCTTGTAAAAAAATTCAAACAATGCAGACAAATGTGTCTCGCAGTCTTCCACTCAGTATCATTTTTGTTTGTACCTTATCAGAAATGTTTCTATGTACAAGTCTTTAAAATCATTTCGAACTTGCTTTGTCCACTGAGTATATTATGGACATCTTTTCATGGCAGGACATATAGATGTGTTAATGGCATTAAAAATAAAACAAAAAACTGATTCGGCCGGGTACGGTGGCTCACGCCTGTAATCCCAGCACTTTGGGAGATCGAGGAGGGAGGATCACCTGAGGTCAGGAGTTACAGACATGGAGAAACCCCGTCTCTACTAAAAATACAAAATTAGCCTGGCGTGGTGGCGCATGCCTGTAATCCCAGCTACTCGGGAGGCTGAGGCAGGAGAATCGCTTGAACCCGGGAGCGGAGGTTGCGGTGAGCCGAGATCGCACCGTTGCACTCCAGCCTGGGCGACAGAGCGAAACTGTCTCAAACAAACAAACAAAAAAACCTGATACATGGTATGGGAAGTACATTGTTTAAACAATGCATGGAGATTTAGGTTGTTTCCAGTTTTTACTGGCACAGATACGGCAATGAATATAATTTTATGTATACATTCATACAAATATATCGGTGGAAAATTCCTAGAAGTGGAATGGCTGGGTCAGTGGGCATTCATATTGAGAAATTGGAAGGATGTTGTCAAACTCTGCAAATCAGAGTATTTTAGTCTTAACCTCTCTTCTTCACACCCTTTTCCTTGGAAGAAAGCTAAATTTAGACTTTTAAACACAAAACTCCATTTTGAGACCCCTGAAAATCTGGGTTCAAAGTGTTTGAAAATTAAAGCAGAGGCTTTAATTTGTACTTATTTAGGTATAATTTGTACTTTAAAGTTGTTCCAGAAAACAAGGCAAATACTGAAAAGCATTTCATCTGAAGTTTCTTTCTGGTATTGAACAGAGTTGGATGGAGAGTGAATGCAGGTTACCGATGTGTTCGCAGGAACAAAACGGGAGGGAGAGTAACACAGGTGCACGGCTTCATATTGATTTATCAAAGAACTTGCTTTTGGACTGTTAAAAAATGTGTTGCACAGTTCTGACACTTTTACACAATGATGAGCTTACAAAGGGGAAAATGCTTAACATAAACATAGTTTCAAAGAAGGTAAGTCCTAGTTTTTCCACGAGCTGTTTTTTTTTTTGAGAGAGAGTAGTCCCAAAATTTCCCTATTATGTGGTTTTTCATCCAAAGTGTTGTAATCCGTGTGTTCTAACCCGCATATTACGATTACTCAAGAAATGCAAAATAGGCCGGGCGCGGTGGCTCACGCCTGTAATCCCAGCAATTTGGGAGGCCGAGGCAGGTGGATCACGAGGTCAGGAGATCAAGACCATCCTAGCTAACACGGTGCAAACCCATCTACTAAAAATAAAAAATATTAGCTGGGCGTGGCGGCAGGCGCCTGTAGTCCCAACTACTCGGGAGGCTGAGGCAGGAGAATGGCGTGAACCTGGGAGGCGGAGCTTGCAGTGAGCCAAGATCCGGCCACTGCACTCCAGCCTGGGCGACAGAGGGAGACTCCGTCTCAAAAATAAAAATAAAAATAAAAAAAGAAAAAAGAAATGCAAAATAATGTGAACACGTCATCGAGACTGCGGTCTACAATTGTGGTTGCCATGATGGGGTGGGTCGGAAGACACCGAGGCTGCTTGAATCATGTTCCTGGCAGGTTAATTTTGTTCCTTTTAGGGCATCTAAATGTTGGAGTTGGCTTTCCTGGCAGAGAGTCCAGTTGTGAGTGGAGTATTGGGTGGAGGCCCCACCCTCCCCAGGTTTTCTGCAGCTTGTTCCTGGCTCTTCTGGGGCTTCTCTGAGGACCCCGATTGGTCCTGGGGCTTCTTTAGTGGTGGGCCTGGGCCAGTTCCTTCCGTTGCTCACCTCTCCCCGAAGCTTAAGGATAACATTTCGGGAAGATCATGAGAGAACGTGAACCAAAAAAAAAAAAAAAAATTCAGGTAGGGTGGGAGCTAAGTTAGTTTAGTATTTGGGATTTTTGTTTTGTTTTGTTTGAGACAGAGTCTCAAACAAAGACCCCGCGCCTGGCCTGGGGGTCTTTTTTTGAGTCAGCAGTTGAGTTTGAAAATCCCCTTCCTTGGAATCCGCCCAGGCTGTTTTCTCTGGTGGATGCTTATTGCCCCTGCTAACTTCAGCTGCGGTCTCTCTGCTCGCCTTGCCCTCCCCATCCTCCTTAGACCTTTTAAGCTTCACTTTCCTTTCCTTGCTGCCCCGCCTCCCTGTCTCCCCAGTGTGATCTACCCTGAAAGGCTGCTGTATTTTGTTTCCCTTTGGGACCGAGCCTGCTCAAATGATCTGCTCCATGAGGTTTTACTGCTATATCCTACTGAAGTATCTGGGAGGTCTTGGCGGAGAAGAGGACAACATCTTGGAAAATGCAGCCTGGAACCTCTCTGCTCTCTTTCATCAGCTTTCCCTCAATGATGAGCTCAAAATCCTCTTTCTTGCTGATGGAATTGTCTGCTGTCATCATTTCAGTTGAGTCCTCCTAGGCTGGGCCTTGTAAGAAGAACAACTGTTTATAACTATTGAAAGTCAATATTTACTAAGAAAAAGAGGTTGTTGTTATCTGAGCCATCCAACATGAATTTAAGAATATATTCCAGGCTGGGCGCGGTGGCTCATGCCTGTAATCCCAGCACTTTGGGAGGCCGAGATGGGTGGATCACCTGAGGTCAGGAGTTCAAGACCAGCCTGGCCAACATGGTGAAACCCCGTCTCTACTAAAAATACAAAAATTAGCCATGCGTGGTGGCACATGCCTGTAGTTCCAACTATTTGGGACACTAAGGCAGGGGAAGTGCTTGAACTCGGGAGGCAGAGGTTGCAGTGAGCTGAGATTGTACCACTGCACTCCAGCCTGGGTAAAAAGAGTGAAATTCCATCTCAAAAAAAAAAAAGGAGACAGGGTCTTGCCCTGTTGCCCAGGCTGGAGTGCCGTGGCACGATCATAACTCACTGTGGCCTCAAATTCCTGGGCTCAACTGATTCTCCTGCCTCAGTCTCCCAAGTAGCTGGGACCACAAGTACATGCCACCATGCTAGGCTAATTTTATTTTTATTTTTAGAAAATAAAATATTTTTTATTTTATTTAGGCAACATGCTATGTTGCCTAGACTGGTCTTGAACTCCTGGCTTCAAGCTATCCTCTTGCCTCAGCCAACCAAAGTGCTGGGATTGCAGGTGTGAACTGCCATGCCTGGCTTTCTATTCTCTATTTCTAGGAGATAAGCTTTTTGAGCTTTCACATATGAGTGTGAACATGTGGTATTTATCTTTCTGTGCCTAGGTTATTCCACTTAACGTAATGTACTCCAGGCTCATCCATGTTGATGGGAATTACAGGATTTCATTCTCTTGTAATGGCTGAATAACATCCCATTTTGTGTATATAGTCCACATGTTCTTTATCCATTCCTCTTTTGATAGACACTTAGGTTGATTCCGTATCTCGGGTATTATGAGCAGTGCTGCAGTAAACATGAGAGTGCAAATATCTCTTCAACATACTGATTCCCTTTCCTTTGGATATATATTCAGCAGT";
 }
