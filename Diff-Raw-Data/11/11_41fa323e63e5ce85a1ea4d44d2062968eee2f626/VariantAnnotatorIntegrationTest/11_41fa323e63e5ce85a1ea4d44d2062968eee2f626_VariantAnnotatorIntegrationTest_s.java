 package org.broadinstitute.sting.gatk.walkers.annotator;
 
 import org.broadinstitute.sting.WalkerTest;
 import org.junit.Test;
 
 import java.util.Arrays;
 
 public class VariantAnnotatorIntegrationTest extends WalkerTest {
 
     public static String baseTestString() {
         return "-T VariantAnnotator -R " + b36KGReference + " -NO_HEADER -o %s";
     }
 
     @Test
     public void testHasAnnotsNotAsking1() {
         WalkerTestSpec spec = new WalkerTestSpec(
                 baseTestString() + " -B:variant,VCF " + validationDataLocation + "vcfexample2.vcf -I " + validationDataLocation + "low_coverage_CEU.chr1.10k-11k.bam -L 1:10,020,000-10,021,000", 1,
                 Arrays.asList("89b846266a0c565b9d2a7dbe793546aa"));
         executeTest("test file has annotations, not asking for annotations, #1", spec);
     }
 
     @Test
     public void testHasAnnotsNotAsking2() {
         WalkerTestSpec spec = new WalkerTestSpec(
                 baseTestString() + " -B:variant,VCF " + validationDataLocation + "vcfexample3.vcf -I " + validationDataLocation + "NA12878.1kg.p2.chr1_10mb_11_mb.SLX.bam -L 1:10,000,000-10,050,000", 1,
                 Arrays.asList("14e546fa7e819faeda3227c842fa92e7"));
         executeTest("test file has annotations, not asking for annotations, #2", spec);
     }
 
     @Test
     public void testHasAnnotsAsking1() {
         WalkerTestSpec spec = new WalkerTestSpec(
                 baseTestString() + " -G \"Standard\" -B:variant,VCF " + validationDataLocation + "vcfexample2.vcf -I " + validationDataLocation + "low_coverage_CEU.chr1.10k-11k.bam -L 1:10,020,000-10,021,000", 1,
                 Arrays.asList("b48199f9dc6f91c61418536151afa8fd"));
         executeTest("test file has annotations, asking for annotations, #1", spec);
     }
 
     @Test
     public void testHasAnnotsAsking2() {
         WalkerTestSpec spec = new WalkerTestSpec(
                 baseTestString() + " -G \"Standard\" -B:variant,VCF " + validationDataLocation + "vcfexample3.vcf -I " + validationDataLocation + "NA12878.1kg.p2.chr1_10mb_11_mb.SLX.bam -L 1:10,000,000-10,050,000", 1,
                 Arrays.asList("dcc7c522c4178b4fd9a0e5439bcdaebc"));
         executeTest("test file has annotations, asking for annotations, #2", spec);
     }
 
     @Test
     public void testNoAnnotsNotAsking1() {
         WalkerTestSpec spec = new WalkerTestSpec(
                 baseTestString() + " -B:variant,VCF " + validationDataLocation + "vcfexample2empty.vcf -I " + validationDataLocation + "low_coverage_CEU.chr1.10k-11k.bam -L 1:10,020,000-10,021,000", 1,
                 Arrays.asList("34d2831e7d843093a2cf47c1f4e5f0f0"));
         executeTest("test file doesn't have annotations, not asking for annotations, #1", spec);
     }
 
     @Test
     public void testNoAnnotsNotAsking2() {
         WalkerTestSpec spec = new WalkerTestSpec(
                 baseTestString() + " -B:variant,VCF " + validationDataLocation + "vcfexample3empty.vcf -I " + validationDataLocation + "NA12878.1kg.p2.chr1_10mb_11_mb.SLX.bam -L 1:10,000,000-10,050,000", 1,
                 Arrays.asList("20b946924fffdf6700cd029424fb72b5"));
         executeTest("test file doesn't have annotations, not asking for annotations, #2", spec);
     }
 
     @Test
     public void testNoAnnotsAsking1() {
         WalkerTestSpec spec = new WalkerTestSpec(
                 baseTestString() + " -G \"Standard\" -B:variant,VCF " + validationDataLocation + "vcfexample2empty.vcf -I " + validationDataLocation + "low_coverage_CEU.chr1.10k-11k.bam -L 1:10,020,000-10,021,000", 1,
                 Arrays.asList("133275d150a8100ba4dc756d17b23ef1"));
         executeTest("test file doesn't have annotations, asking for annotations, #1", spec);
     }
 
     @Test
     public void testNoAnnotsAsking2() {
         WalkerTestSpec spec = new WalkerTestSpec(
                 baseTestString() + " -G \"Standard\" -B:variant,VCF " + validationDataLocation + "vcfexample3empty.vcf -I " + validationDataLocation + "NA12878.1kg.p2.chr1_10mb_11_mb.SLX.bam -L 1:10,000,000-10,050,000", 1,
                 Arrays.asList("120fc0d4af1d370f1b306700258464b9"));
         executeTest("test file doesn't have annotations, asking for annotations, #2", spec);
     }
 
     @Test
     public void testOverwritingHeader() {
         WalkerTestSpec spec = new WalkerTestSpec(
                 baseTestString() + " -G \"Standard\" -B:variant,VCF " + validationDataLocation + "vcfexample4.vcf -I " + validationDataLocation + "NA12878.1kg.p2.chr1_10mb_11_mb.SLX.bam -L 1:10,001,292", 1,
                 Arrays.asList("10c31f8ab903843538a7604ed1e5405c"));
         executeTest("test overwriting header", spec);
     }
 
     @Test
     public void testNoReads() {
         WalkerTestSpec spec = new WalkerTestSpec(
                 baseTestString() + " -G \"Standard\" -B:variant,VCF " + validationDataLocation + "vcfexample3empty.vcf -BTI variant", 1,
                 Arrays.asList("a6ec667a656e7ba368de9dbae781eef3"));
         executeTest("not passing it any reads", spec);
     }
 
     @Test
     public void testDBTagWithDbsnp() {
         WalkerTestSpec spec = new WalkerTestSpec(
                 baseTestString() + " -D " + GATKDataLocation + "dbsnp_129_b36.rod -G \"Standard\" -B:variant,VCF " + validationDataLocation + "vcfexample3empty.vcf -BTI variant", 1,
                 Arrays.asList("8b4c309fe4900b5c8ae720c649715c7d"));
         executeTest("getting DB tag with dbSNP", spec);
     }
 
     @Test
     public void testDBTagWithHapMap() {
         WalkerTestSpec spec = new WalkerTestSpec(
                 baseTestString() + " -B:compH3,VCF " + validationDataLocation + "fakeHM3.vcf -G \"Standard\" -B:variant,VCF " + validationDataLocation + "vcfexample3empty.vcf -BTI variant", 1,
                 Arrays.asList("ff6b3468f21b262de671e823349cbb3c"));
         executeTest("getting DB tag with HM3", spec);
     }
 }
