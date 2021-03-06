 package com.sd_editions.collatex.spike2;
 
 import java.util.ArrayList;
 import java.util.List;
 import java.util.Set;
 
 import junit.framework.TestCase;
 
 import com.google.common.collect.Lists;
 import com.google.common.collect.Sets;
 
 public class MatchPermutatorTest extends TestCase {
 
   private MatchPermutator permutator;
   private Set<Match> testSet;
   private Match match_1_2;
   private Match match_1_3;
   private Match match_2_1;
   private Match match_2_3;
   private Match match_3_4;
   private PMatch pmatch_1_2;
   private PMatch pmatch_1_3;
   private PMatch pmatch_2_1;
   private PMatch pmatch_2_3;
   private PMatch pmatch_3_4;
 
   @Override
   protected void setUp() throws Exception {
     super.setUp();
     testSet = Sets.newLinkedHashSet();
     match_1_2 = new Match(new Word("een", 1), new Word("een", 2));
     match_1_3 = new Match(new Word("een", 1), new Word("tween", 3));
     match_2_1 = new Match(new Word("twee", 2), new Word("twee", 1));
     match_2_3 = new Match(new Word("twee", 2), new Word("tween", 3));
     match_3_4 = new Match(new Word("drie", 3), new Word("drie", 4));
     pmatch_1_2 = new PMatch(match_1_2);
     pmatch_1_3 = new PMatch(match_1_3);
     pmatch_2_1 = new PMatch(match_2_1);
     pmatch_2_3 = new PMatch(match_2_3);
     pmatch_3_4 = new PMatch(match_3_4);
     testSet.add(match_1_2);
     testSet.add(match_1_3);
     testSet.add(match_2_1);
     testSet.add(match_2_3);
     testSet.add(match_3_4);
     permutator = new MatchPermutator(testSet);
   }
 
   public void testFindAlternatives1() {
     List<PMatch> pmatches = Lists.newArrayList(pmatch_1_2, pmatch_1_3, pmatch_2_1, pmatch_2_3, pmatch_3_4);
     ArrayList<PMatch> alternatives = Lists.newArrayList(permutator.findAlternatives(pmatches, pmatch_1_3));
     ArrayList<PMatch> expected = Lists.newArrayList(pmatch_1_2, pmatch_1_3, pmatch_2_3);
     assertEquals(expected.size(), alternatives.size());
     assertEquals(expected, alternatives);
   }
 
   public void testFindAlternatives2() {
     List<PMatch> pmatches = Lists.newArrayList(pmatch_1_2, pmatch_1_3, pmatch_2_1, pmatch_2_3, pmatch_3_4);
     ArrayList<PMatch> alternatives = Lists.newArrayList(permutator.findAlternatives(pmatches, pmatch_1_2));
     ArrayList<PMatch> expected = Lists.newArrayList(pmatch_1_2, pmatch_1_3);
     assertEquals(expected.size(), alternatives.size());
     assertEquals(expected, alternatives);
   }
 
   public void testFixCell1() {
     List<PMatch> pmatches = Lists.newArrayList(pmatch_1_2, pmatch_1_3, pmatch_2_1, pmatch_2_3, pmatch_3_4);
     List<PMatch> expected = Lists.newArrayList(pmatch_1_3, pmatch_2_1, pmatch_3_4);
    ArrayList<PMatch> fixCell = Lists.newArrayList(permutator.fixPMatch(pmatches, pmatch_1_3));
     assertEquals(expected.size(), fixCell.size());
    //    assertEquals(expected, fixCell);
   }
 
   public void testFixCell2() {
     List<PMatch> pmatches = Lists.newArrayList(pmatch_1_2, pmatch_1_3, pmatch_3_4);
    PMatch fpmatch_1_2 = pmatch_1_2.copy();
    fpmatch_1_2.fix();
    List<PMatch> expected = Lists.newArrayList(fpmatch_1_2, pmatch_3_4);
    ArrayList<PMatch> fixCell = Lists.newArrayList(permutator.fixPMatch(pmatches, pmatch_1_2));
     assertEquals(expected.size(), fixCell.size());
    //    assertEquals(expected, fixCell);
   }
 
 }
