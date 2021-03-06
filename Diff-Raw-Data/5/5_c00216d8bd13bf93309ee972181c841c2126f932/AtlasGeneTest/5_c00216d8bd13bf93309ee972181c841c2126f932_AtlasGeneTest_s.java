 /*
  * Copyright 2008-2010 Microarray Informatics Team, EMBL-European Bioinformatics Institute
  *
  * Licensed under the Apache License, Version 2.0 (the "License");
  * you may not use this file except in compliance with the License.
  * You may obtain a copy of the License at
  *
  * http://www.apache.org/licenses/LICENSE-2.0
  *
  * Unless required by applicable law or agreed to in writing, software
  * distributed under the License is distributed on an "AS IS" BASIS,
  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  * See the License for the specific language governing permissions and
  * limitations under the License.
  *
  *
  * For further details of the Gene Expression Atlas project, including source code,
  * downloads and documentation, please see:
  *
  * http://ostolop.github.com/gxa/
  */
 
 package ae3.model;
 
 import uk.ac.ebi.gxa.index.AbstractOnceIndexTest;
 import uk.ac.ebi.gxa.index.GeneExpressionAnalyticsTable;
 import ae3.dao.AtlasDao;
 import uk.ac.ebi.gxa.utils.Pair;
 import uk.ac.ebi.microarray.atlas.model.ExpressionAnalysis;
 import org.junit.Before;
 import org.junit.Test;
 import static org.junit.Assert.assertTrue;
 import static org.junit.Assert.assertNotNull;
 import static org.junit.Assert.assertFalse;
 import org.apache.solr.common.SolrDocument;
 import org.apache.solr.client.solrj.embedded.EmbeddedSolrServer;
 
 import java.util.*;
 
 import static junit.framework.Assert.assertEquals;
 
 /**
  * @author pashky
  */
 public class AtlasGeneTest  extends AbstractOnceIndexTest {
     private AtlasGene gene;
 
     @Before
     public void initGene() {
         AtlasDao dao = new AtlasDao();
         dao.setSolrServerAtlas(new EmbeddedSolrServer(getContainer(), "atlas"));
         dao.setSolrServerExpt(new EmbeddedSolrServer(getContainer(), "expt"));
         gene = dao.getGeneByIdentifier("ENSG00000066279").getGene();
     }
 
     @Test
     public void test_getGeneSpecies() {
         assertNotNull(gene.getGeneSpecies());
         assertEquals("Homo sapiens", gene.getGeneSpecies());
     }
 
     @Test
     public void test_getGeneIds() {
         assertNotNull(gene.getGeneId());
         assertTrue(gene.getGeneId().matches("^[0-9]+$"));
     }
 
     @Test
     public void test_getGeneName() {
         assertNotNull(gene.getGeneName());
         assertEquals("ASPM", gene.getGeneName());
     }
 
     @Test
     public void test_getGeneIdentifier() {
         assertNotNull(gene.getGeneIdentifier());
         assertEquals("ENSG00000066279", gene.getGeneIdentifier());
     }
 
     @Test
     public void test_getGeneEnsembl() {
         assertNotNull(gene.getGeneEnsembl());
         assertEquals("ENSG00000066279", gene.getGeneEnsembl());
     }
 
     @Test
     public void test_getGoTerm() {
         assertNotNull(gene.getGoTerm());
         assertTrue(gene.getGoTerm().matches(".*\\S+.*"));
     }
 
     @Test
     public void test_getInterProTerm() {
         assertNotNull(gene.getInterProTerm());
         assertTrue(gene.getInterProTerm().matches(".*\\S+.*"));
     }
 
     @Test
     public void test_getKeyword() {
         assertNotNull(gene.getKeyword());
         assertTrue(gene.getKeyword().matches(".*\\S+.*"));
     }
 
     @Test
     public void test_getDisesase() {
         assertNotNull(gene.getDisease());
         assertTrue(gene.getDisease().matches(".*\\S+.*"));
     }
 
     @Test
     public void test_shortValues() {
         assertTrue(gene.getShortGOTerms().length() <= gene.getGoTerm().length());
         assertTrue(gene.getShortInterProTerms().length() <= gene.getInterProTerm().length());
         assertTrue(gene.getShortDiseases().length() <= gene.getDisease().length());
     }
 
     @Test
     public void test_getGeneSolrDocument() {
         SolrDocument solrdoc = gene.getGeneSolrDocument();
         assertNotNull(solrdoc);
         assertTrue(solrdoc.getFieldNames().contains("id"));
     }
 
     @Test
     public void test_getUniprotIds(){
         assertNotNull(gene.getUniprotId());
         assertFalse("Uniprot ID is an empty string", gene.getUniprotId().equals(""));
         assertTrue(gene.getUniprotId().matches("^[A-Z0-9, ]+$"));
     }
 
     @Test
     public void getSynonyms(){
         assertNotNull(gene.getSynonym());
         assertFalse("Synonym is an empty string", gene.getSynonym().equals(""));
         assertTrue(gene.getSynonym().contains("ASPM"));
     }
 
     @Test
     public void test_highlighting() {
         Map<String, List<String>> highlights = new HashMap<String, List<String>>();
         highlights.put("property_SYNONYM", Arrays.asList("<em>ASPM</em>", "MCPH5", "RP11-32D17.1-002", "hCG_2039667"));
         gene.setGeneHighlights(highlights);
         assertTrue(gene.getHilitSynonym().matches(".*<em>.*"));
         assertNotNull(gene.getGeneHighlightStringForHtml());
         assertTrue(gene.getGeneHighlightStringForHtml().matches(".*<em>.*"));
     }
 
     @Test
 	public void test_getAllFactorValues() {
         Collection<String> efvs = gene.getAllFactorValues("cellline");
         assertNotNull(efvs);
         assertTrue(efvs.size() > 0);
         assertTrue(efvs.contains("BT474"));
 	}
 
     /*
     @Test
 	public void test_orthologs() {
         gene.getOrthologs();
         gene.getOrthologsIds();
         public void addOrthoGene(AtlasGene ortho){
         public ArrayList<AtlasGene> getOrthoGenes(){
 
 	}
 
     @Test
     public void test_getCounts() {
 
         }
     }
 */
     @Test
     public void test_getExpermientsTable() {
         GeneExpressionAnalyticsTable et = gene.getExpressionAnalyticsTable();
         assertTrue(et.getAll().iterator().hasNext());
     }
 
     @Test
     public void test_getNumberOfExperiments() {
         assertTrue(gene.getNumberOfExperiments() > 0);
     }
 
     @Test
     public void test_getAllEfs() {
         Collection<String> efs = gene.getAllEfs();
         assertNotNull(efs);
         assertTrue(efs.size() > 0);
         assertTrue(efs.contains("cellline"));
         assertTrue(efs.contains("organismpart"));
     }
 
     @Test
     public void test_getHeatMapRows() {
         Collection<ListResultRow> rows = gene.getHeatMapRows();
         assertNotNull(rows);
         assertTrue(rows.size() > 0);
     }
 
     @Test
     public void test_getTopFVs() {
         Collection<ExpressionAnalysis> efvs = gene.getTopFVs(204778371);
         assertNotNull(efvs);
 
         double pv = 0;
         for(ExpressionAnalysis t : efvs) {
             assertTrue(pv <= t.getPValAdjusted());
             pv = t.getPValAdjusted();
         }
     }
 
     @Test
     public void test_getHighestRankEF() {
         Pair<String,Double> hef = gene.getHighestRankEF(204778371);
         assertNotNull(hef);
         assertTrue(hef.getSecond() >= 0);
         assertTrue(hef.getFirst().matches(".*[A-Za-z]+.*"));
     }
 }
