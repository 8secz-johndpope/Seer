 package org.broadinstitute.sting.gatk.walkers.genotyper;
 
 import org.broadinstitute.sting.WalkerTest;
 import org.broadinstitute.sting.gatk.GenomeAnalysisEngine;
 import org.testng.annotations.Test;
 
 import java.io.File;
 import java.util.Arrays;
 import java.util.HashMap;
 import java.util.List;
 import java.util.Map;
 
 // ********************************************************************************** //
 // Note that this class also serves as an integration test for the VariantAnnotator!  //
 // ********************************************************************************** //
 
 public class UnifiedGenotyperIntegrationTest extends WalkerTest {
 
     private final static String baseCommand = "-T UnifiedGenotyper -R " + b36KGReference + " -NO_HEADER";
 
     // --------------------------------------------------------------------------------------------------------------
     //
     // testing normal calling
     //
     // --------------------------------------------------------------------------------------------------------------
     @Test
     public void testMultiSamplePilot1() {
         WalkerTest.WalkerTestSpec spec = new WalkerTest.WalkerTestSpec(
                 baseCommand + " -I " + validationDataLocation + "low_coverage_CEU.chr1.10k-11k.bam -o %s -L 1:10,022,000-10,025,000", 1,
                 Arrays.asList("a2218244df08dce96d04f8736a6de796"));
         executeTest("test MultiSample Pilot1", spec);
     }
 
     @Test
     public void testMultiSamplePilot2AndRecallingWithAlleles() {
         String md5 = "d50bc17991d0dfd03ab2810a1cfeec85";
 
         WalkerTest.WalkerTestSpec spec1 = new WalkerTest.WalkerTestSpec(
                 baseCommand + " -I " + validationDataLocation + "pilot2_daughters.chr20.10k-11k.bam -o %s -L 20:10,000,000-10,050,000", 1,
                 Arrays.asList(md5));
         List<File> result = executeTest("test MultiSample Pilot2", spec1).getFirst();
 
         GenomeAnalysisEngine.resetRandomGenerator();
 
         WalkerTest.WalkerTestSpec spec2 = new WalkerTest.WalkerTestSpec(
                 baseCommand + " --genotyping_mode GENOTYPE_GIVEN_ALLELES -B:alleles,vcf " + result.get(0).getAbsolutePath() + " -I " + validationDataLocation + "pilot2_daughters.chr20.10k-11k.bam -o %s -L 20:10,000,000-10,050,000", 1,
                 Arrays.asList(md5));
         executeTest("test MultiSample Pilot2 with alleles passed in", spec2);       
     }
 
     @Test
     public void testWithAllelesPassedIn() {
         WalkerTest.WalkerTestSpec spec1 = new WalkerTest.WalkerTestSpec(
                 baseCommand + " --genotyping_mode GENOTYPE_GIVEN_ALLELES -B:alleles,vcf " + validationDataLocation + "allelesForUG.vcf -I " + validationDataLocation + "pilot2_daughters.chr20.10k-11k.bam -o %s -L 20:10,000,000-10,025,000", 1,
                 Arrays.asList("bcb02e2a969edfbd290b9ec22f1a5884"));
         executeTest("test MultiSample Pilot2 with alleles passed in", spec1);
 
         WalkerTest.WalkerTestSpec spec2 = new WalkerTest.WalkerTestSpec(
                 baseCommand + " --output_mode EMIT_ALL_SITES --genotyping_mode GENOTYPE_GIVEN_ALLELES -B:alleles,vcf " + validationDataLocation + "allelesForUG.vcf -I " + validationDataLocation + "pilot2_daughters.chr20.10k-11k.bam -o %s -L 20:10,000,000-10,025,000", 1,
                 Arrays.asList("f1ff29ac1c79c76fcb6c290b39bc3a18"));
         executeTest("test MultiSample Pilot2 with alleles passed in", spec2);
     }
 
     @Test
     public void testSingleSamplePilot2() {
         WalkerTest.WalkerTestSpec spec = new WalkerTest.WalkerTestSpec(
                 baseCommand + " -I " + validationDataLocation + "NA12878.1kg.p2.chr1_10mb_11_mb.SLX.bam -o %s -glm SNP -L 1:10,000,000-10,100,000", 1,
                 Arrays.asList("a7f59c32f63e8ca4c3ffe468c51fbaa2"));
         executeTest("test SingleSample Pilot2", spec);
     }
 
     // --------------------------------------------------------------------------------------------------------------
     //
     // testing compressed output
     //
     // --------------------------------------------------------------------------------------------------------------
 
     private final static String COMPRESSED_OUTPUT_MD5 = "211e4ed67cd7bd3501555d829548da48";
 
     @Test
     public void testCompressedOutput() {
         WalkerTest.WalkerTestSpec spec = new WalkerTest.WalkerTestSpec(
                 baseCommand + " -I " + validationDataLocation + "NA12878.1kg.p2.chr1_10mb_11_mb.SLX.bam -o %s -L 1:10,000,000-10,100,000", 1,
                 Arrays.asList("gz"), Arrays.asList(COMPRESSED_OUTPUT_MD5));
         executeTest("test compressed output", spec);
     }
 
     // todo -- fixme
 //    @Test
 //    public void testCompressedOutputParallel() {
 //        WalkerTest.WalkerTestSpec spec = new WalkerTest.WalkerTestSpec(
 //                baseCommand + " -I " + validationDataLocation + "NA12878.1kg.p2.chr1_10mb_11_mb.SLX.bam -o %s -L 1:10,000,000-10,100,000 -nt 4", 1,
 //                Arrays.asList("gz"), Arrays.asList(COMPRESSED_OUTPUT_MD5));
 //        executeTest("testCompressedOutput-nt4", spec);
 //    }
 
     // --------------------------------------------------------------------------------------------------------------
     //
     // testing parallelization
     //
     // --------------------------------------------------------------------------------------------------------------
 
     @Test
     public void testParallelization() {
 
         // Note that we need to turn off any randomization for this to work, so no downsampling and no annotations
 
         String md5 = "f83a33a1ecc350cae0c002e4a43a7861";
 
         WalkerTest.WalkerTestSpec spec1 = new WalkerTest.WalkerTestSpec(
                 baseCommand + " -dt NONE -G none -I " + validationDataLocation + "NA12878.1kg.p2.chr1_10mb_11_mb.SLX.bam -o %s -L 1:10,000,000-10,075,000", 1,
                 Arrays.asList(md5));
         executeTest("test parallelization (single thread)", spec1);
 
         GenomeAnalysisEngine.resetRandomGenerator();
 
         WalkerTest.WalkerTestSpec spec2 = new WalkerTest.WalkerTestSpec(
                 baseCommand + " -dt NONE -G none -I " + validationDataLocation + "NA12878.1kg.p2.chr1_10mb_11_mb.SLX.bam -o %s -L 1:10,000,000-10,075,000 -nt 2", 1,
                 Arrays.asList(md5));
         executeTest("test parallelization (2 threads)", spec2);
 
         GenomeAnalysisEngine.resetRandomGenerator();
 
         WalkerTest.WalkerTestSpec spec3 = new WalkerTest.WalkerTestSpec(
                 baseCommand + " -dt NONE -G none -I " + validationDataLocation + "NA12878.1kg.p2.chr1_10mb_11_mb.SLX.bam -o %s -L 1:10,000,000-10,075,000 -nt 4", 1,
                 Arrays.asList(md5));
         executeTest("test parallelization (4 threads)", spec3);
     }
 
     // --------------------------------------------------------------------------------------------------------------
     //
     // testing parameters
     //
     // --------------------------------------------------------------------------------------------------------------
 
     @Test
     public void testCallingParameters() {
         HashMap<String, String> e = new HashMap<String, String>();
         e.put( "--min_base_quality_score 26", "e74b0f3c3977f383645e82ae76034932" );
         e.put( "--min_mapping_quality_score 26", "5f08d9e052bdb04a2a5ee78db349dde9" );
         e.put( "--p_nonref_model GRID_SEARCH", "2f1350f76a571d28cd06e59ea6dffe4b" );
 
         for ( Map.Entry<String, String> entry : e.entrySet() ) {
             WalkerTest.WalkerTestSpec spec = new WalkerTest.WalkerTestSpec(
                     baseCommand + " -I " + validationDataLocation + "NA12878.1kg.p2.chr1_10mb_11_mb.SLX.bam -o %s -L 1:10,000,000-10,010,000 " + entry.getKey(), 1,
                     Arrays.asList(entry.getValue()));
             executeTest(String.format("test calling parameter[%s]", entry.getKey()), spec);
         }
     }
 
     @Test
     public void testOutputParameter() {
         HashMap<String, String> e = new HashMap<String, String>();
         e.put( "-sites_only", "63b76c4d26edf8cbb5bd91dafc81fee1" );
         e.put( "--output_mode EMIT_ALL_CONFIDENT_SITES", "5bf0268945d953377ea3a811b20ff1bc" );
        e.put( "--output_mode EMIT_ALL_SITES", "a1730ea5ae5e1aa57d85d9d2372facc8" );
 
         for ( Map.Entry<String, String> entry : e.entrySet() ) {
             WalkerTest.WalkerTestSpec spec = new WalkerTest.WalkerTestSpec(
                     baseCommand + " -I " + validationDataLocation + "NA12878.1kg.p2.chr1_10mb_11_mb.SLX.bam -o %s -L 1:10,000,000-10,010,000 " + entry.getKey(), 1,
                     Arrays.asList(entry.getValue()));
             executeTest(String.format("testParameter[%s]", entry.getKey()), spec);
         }
     }
 
     @Test
     public void testConfidence() {
         WalkerTest.WalkerTestSpec spec1 = new WalkerTest.WalkerTestSpec(
                 baseCommand + " -I " + validationDataLocation + "NA12878.1kg.p2.chr1_10mb_11_mb.SLX.bam -o %s -L 1:10,000,000-10,010,000 -stand_call_conf 10 ", 1,
                 Arrays.asList("5f08d9e052bdb04a2a5ee78db349dde9"));
         executeTest("test confidence 1", spec1);
 
         WalkerTest.WalkerTestSpec spec2 = new WalkerTest.WalkerTestSpec(
                 baseCommand + " -I " + validationDataLocation + "NA12878.1kg.p2.chr1_10mb_11_mb.SLX.bam -o %s -L 1:10,000,000-10,010,000 -stand_emit_conf 10 ", 1,
                 Arrays.asList("0d0bbfd08d1ce35ec1c007ba0f8dfe37"));
         executeTest("test confidence 2", spec2);
     }
 
     // --------------------------------------------------------------------------------------------------------------
     //
     // testing heterozygosity
     //
     // --------------------------------------------------------------------------------------------------------------
     @Test
     public void testHeterozyosity() {
         HashMap<Double, String> e = new HashMap<Double, String>();
         e.put( 0.01, "4fb6e66895bbaa9a3b9af91c985a2782" );
         e.put( 1.0 / 1850, "1397d29d924e74f43de466927a9f7915" );
 
         for ( Map.Entry<Double, String> entry : e.entrySet() ) {
             WalkerTest.WalkerTestSpec spec = new WalkerTest.WalkerTestSpec(
                     baseCommand + " -I " + validationDataLocation + "NA12878.1kg.p2.chr1_10mb_11_mb.SLX.bam -o %s -L 1:10,000,000-10,100,000 --heterozygosity " + entry.getKey(), 1,
                     Arrays.asList(entry.getValue()));
             executeTest(String.format("test heterozyosity[%s]", entry.getKey()), spec);
         }
     }
 
 
     // --------------------------------------------------------------------------------------------------------------
     //
     // testing calls with SLX, 454, and SOLID data
     //
     // --------------------------------------------------------------------------------------------------------------
     @Test
     public void testMultiTechnologies() {
         WalkerTest.WalkerTestSpec spec = new WalkerTest.WalkerTestSpec(
                 baseCommand +
                         " -I " + validationDataLocation + "NA12878.1kg.p2.chr1_10mb_11_mb.allTechs.bam" +
                         " -o %s" +
                         " -L 1:10,000,000-10,100,000",
                 1,
                 Arrays.asList("b44ddfbeb9fc413e44194799b3e3bbf8"));
 
         executeTest(String.format("test multiple technologies"), spec);
     }
 
     // --------------------------------------------------------------------------------------------------------------
     //
     // testing calls with BAQ
     //
     // --------------------------------------------------------------------------------------------------------------
     @Test
     public void testCallingWithBAQ() {
         WalkerTest.WalkerTestSpec spec = new WalkerTest.WalkerTestSpec(
                 baseCommand +
                         " -I " + validationDataLocation + "NA12878.1kg.p2.chr1_10mb_11_mb.allTechs.bam" +
                         " -o %s" +
                         " -L 1:10,000,000-10,100,000" +
                         " -baq CALCULATE_AS_NECESSARY",
                 1,
                 Arrays.asList("164e25d61ef562ae863871d4ec7cb387"));
 
         executeTest(String.format("test calling with BAQ"), spec);
     }
 
     // --------------------------------------------------------------------------------------------------------------
     //
     // testing indel caller
     //
     // --------------------------------------------------------------------------------------------------------------
     // Basic indel testing with SLX data
     @Test
     public void testSimpleIndels() {
         WalkerTest.WalkerTestSpec spec = new WalkerTest.WalkerTestSpec(
                 baseCommand +
                         " -I " + validationDataLocation + "NA12878.1kg.p2.chr1_10mb_11_mb.SLX.bam" +
                         " -o %s" +
                         " -glm INDEL" +
                         " -L 1:10,000,000-10,500,000",
                 1,
                 Arrays.asList("ffeea8550a707871a68f6707159f4ea9"));
 
         executeTest(String.format("test indel caller in SLX"), spec);
     }
 
     // Basic indel testing with SLX data
     @Test
     public void testIndelsWithLowMinAlleleCnt() {
         WalkerTest.WalkerTestSpec spec = new WalkerTest.WalkerTestSpec(
                 baseCommand +
                         " -I " + validationDataLocation + "NA12878.1kg.p2.chr1_10mb_11_mb.SLX.bam" +
                         " -o %s" +
                         " -glm INDEL -minIndelCnt 1" +
                         " -L 1:10,000,000-10,100,000",
                 1,
                 Arrays.asList("b6546f6e8f092b3c39cffec22aa47bcb"));
 
         executeTest(String.format("test indel caller in SLX witn low min allele count"), spec);
     }
 
     @Test
     public void testMultiTechnologyIndels() {
          WalkerTest.WalkerTestSpec spec = new WalkerTest.WalkerTestSpec(
                  baseCommand +
                          " -I " + validationDataLocation + "NA12878.1kg.p2.chr1_10mb_11_mb.allTechs.bam" +
                          " -o %s" +
                          " -glm INDEL" +
                          " -L 1:10,000,000-10,500,000",
                  1,
                  Arrays.asList("897057070aa2e3651d91625a58c5ec4b"));
 
          executeTest(String.format("test indel calling, multiple technologies"), spec);
      }
 
     // todo - feature not yet fully working with indels
     //@Test
     public void testWithIndelAllelesPassedIn() {
         WalkerTest.WalkerTestSpec spec1 = new WalkerTest.WalkerTestSpec(
                 baseCommand + " --genotyping_mode GENOTYPE_GIVEN_ALLELES -B:alleles,vcf " + validationDataLocation + "indelAllelesForUG.vcf -I " + validationDataLocation +
                         "pilot2_daughters.chr20.10k-11k.bam -o %s -L 20:10,000,000-10,100,000 -glm INDEL", 1,
                 Arrays.asList("e95c545b8ae06f0721f260125cfbe1f0"));
         executeTest("test MultiSample Pilot2 indels with alleles passed in", spec1);
 
         WalkerTest.WalkerTestSpec spec2 = new WalkerTest.WalkerTestSpec(
                 baseCommand + " --output_mode EMIT_ALL_SITES --genotyping_mode GENOTYPE_GIVEN_ALLELES -B:alleles,vcf "
                         + validationDataLocation + "indelAllelesForUG.vcf -I " + validationDataLocation +
                         "pilot2_daughters.chr20.10k-11k.bam -o %s -L 20:10,000,000-10,100,000 -glm INDEL", 1,
                 Arrays.asList("6c96d76b9bc3aade0c768d7c657ae210"));
         executeTest("test MultiSample Pilot2 indels with alleles passed in", spec2);
     }
 
 
 }
