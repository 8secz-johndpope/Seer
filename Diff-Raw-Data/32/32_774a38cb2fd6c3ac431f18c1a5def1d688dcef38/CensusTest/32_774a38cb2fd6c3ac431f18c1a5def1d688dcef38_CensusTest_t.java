 package org.biojava3.structure.align.symm.census2;
 
 import static org.junit.Assert.assertEquals;
 
 import java.io.BufferedReader;
 import java.io.File;
import java.io.FileReader;
 import java.io.IOException;
 import java.util.ArrayList;
 import java.util.List;
 
 import org.biojava.bio.structure.StructureException;
 import org.biojava.bio.structure.scop.ScopDatabase;
 import org.biojava.bio.structure.scop.ScopDomain;
 import org.biojava.bio.structure.scop.ScopFactory;
 import org.biojava3.structure.align.symm.protodomain.ResourceList;
 import org.biojava3.structure.align.symm.protodomain.ResourceList.NameProvider;
 import org.junit.Before;
 import org.junit.Test;
 
 /**
  * A unit test for {@link Census}.
  * @author dmyerstu
  */
 public class CensusTest {
 
 	class TinyCensus extends Census {
 
 		public TinyCensus() {
 			super(1); // 1 core only to ensure the results return in the expected order
 		}
 
 		@Override
 		protected List<ScopDomain> getDomains() {
 			List<ScopDomain> domains = new ArrayList<ScopDomain>();
 			ScopDatabase scop = ScopFactory.getSCOP(ScopFactory.VERSION_1_75B);
 			for (String domain : CensusTest.domains) {
 				domains.add(scop.getDomainByScopID(domain));
 			}
 			return domains;
 		}
 
 	}
 
 	private static String[] domains = new String[] { "d2c35e1" };
 	
 	private static String expectedResult;
 
 	@Before
 	public void setUp() throws StructureException {
 		ResourceList.set(NameProvider.defaultNameProvider(), ResourceList.DEFAULT_PDB_DIR);
 		expectedResult = ResourceList.get().openFileAsString("census2/expected1.xml");
 		
 		ScopDatabase scop = ScopFactory.getSCOP(ScopFactory.VERSION_1_75B);		
 		ScopFactory.setScopDatabase(scop); 
 		
 	}
 
 	/**
 	 * Test on live data.
 	 * @throws IOException
 	 */
 	@Test
 	public void testBasic() throws IOException {
		File actualFile = File.createTempFile("actualresult1", "xml");
 		Census census = new TinyCensus();
 		census.setCache(ResourceList.get().getCache());
		census.setOutputWriter(actualFile);
 		census.run();
 		// unfortunately, the timestamp will be different
 		String[] expectedLines = expectedResult.split("\n");
		BufferedReader br = new BufferedReader(new FileReader(actualFile));
 		String line = "";
 		int i = 0;
 		while ((line = br.readLine()) != null) {
 			if (!line.contains("<timestamp>")) {
 				assertEquals(expectedLines[i], line);
 			}
 			i++;
 		}
 		br.close();
		actualFile.delete();
 	}
 
 	@Test
 	public void testWithPartialResult() {
		// TODO
 	}
 	
 	@Test
 	public void testHard() {
		// TODO
 	}
 
 }
