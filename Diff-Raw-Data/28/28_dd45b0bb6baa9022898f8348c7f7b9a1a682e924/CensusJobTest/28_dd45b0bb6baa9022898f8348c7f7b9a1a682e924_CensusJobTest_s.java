 /*
  *                    BioJava development code
  *
  * This code may be freely distributed and modified under the
  * terms of the GNU Lesser General Public Licence.  This should
  * be distributed with the code.  If you do not have a copy,
  * see:
  *
  *      http://www.gnu.org/copyleft/lesser.html
  *
  * Copyright for this code is held jointly by the individual
  * authors.  These should be listed in @author doc comments.
  *
  * For more information on the BioJava project and its aims,
  * or to join the biojava-l mailing list, visit the home page
  * at:
  *
  *      http://www.biojava.org/
  *
  * Created on 2013-03-01
  *
  */
 package org.biojava3.structure.align.symm.census2;
 
 
 import static org.junit.Assert.assertEquals;
 import static org.mockito.Matchers.any;
 import static org.mockito.Mockito.mock;
 import static org.mockito.Mockito.when;
 
 import org.biojava.bio.structure.Atom;
 import org.biojava.bio.structure.align.StructureAlignment;
 import org.biojava.bio.structure.align.model.AFPChain;
 import org.biojava.bio.structure.scop.BerkeleyScopInstallation;
 import org.biojava.bio.structure.scop.ScopDatabase;
 import org.biojava.bio.structure.scop.ScopDomain;
 import org.biojava.bio.structure.scop.ScopFactory;
 import org.biojava3.structure.align.symm.protodomain.Protodomain;
 import org.biojava3.structure.align.symm.census2.Census.AlgorithmGiver;
 import org.biojava3.structure.align.symm.CeSymm;
 import org.biojava3.structure.align.symm.protodomain.ResourceList;
 import org.biojava3.structure.align.symm.protodomain.ResourceList.NameProvider;
 import org.junit.Before;
 import org.junit.Test;
 
 /**
  * A unit test for {@link CensusJob}.
  * @author dmyerstu
  */
 public class CensusJobTest {
 
 	private String[] domains = new String[] { "d2c35e1" };
 
 	private final double zScore = 6.0;
 	
 	@Before
 	public void setUp() throws Exception {
 		ResourceList.set(NameProvider.defaultNameProvider(), ResourceList.DEFAULT_PDB_DIR);
		if (!scop.getClass().getName().equals(BerkeleyScopInstallation.class.getName())) { // for efficiency
			ScopFactory.setScopDatabase(new BerkeleyScopInstallation()); // ScopDatabase is too hard to mock well
		}
 		final CeSymm ceSymm = mock(CeSymm.class);
 		AFPChain afpChain = new AFPChain();
 		afpChain.setProbability(zScore);
 		afpChain.setBlockNum(2);
 		afpChain.setOptAln(new int[1][][]);
 		when(ceSymm.align(any(Atom[].class), any(Atom[].class))).thenReturn(afpChain);
 		Significance sig = new Significance() {
 			@Override
 			public boolean isPossiblySignificant(AFPChain afpChain) {
 				return false;
 			}
 			@Override
 			public boolean isSignificant(Protodomain protodomain, int order, double angle, AFPChain afpChain) {
 				return false;
 			}
 			@Override
 			public boolean isSignificant(Result result) {
 				return false;
 			}
 		};
 		AlgorithmGiver giver = new AlgorithmGiver() {
 			@Override
 			public StructureAlignment getAlgorithm() {
 				return ceSymm;
 			}
 		};
 		job = new CensusJob(ResourceList.get().getCache(), giver, sig);
 	}
 	
 	private CensusJob job;
	private final ScopDatabase scop = ScopFactory.getSCOP();
 	
 	@Test
 	public void test() {
 		for (int i = 0; i < domains.length; i++) {
 			final ScopDomain domain = scop.getDomainByScopID(domains[i]);
 			job.setCount(i);
 			job.setDomain(domain);
 			job.setSuperfamily(scop.getScopDescriptionBySunid(domain.getSuperfamilyId()));
 			Result result = job.call();
 			assertEquals(domains[i], result.getScopId());
 			assertEquals(zScore, result.getAlignment().getzScore().doubleValue(), 0);
 		}
 	}
 
 }
