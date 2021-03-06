 /*
  * Copyright (c) 2011, The Broad Institute
  *
  * Permission is hereby granted, free of charge, to any person
  * obtaining a copy of this software and associated documentation
  * files (the "Software"), to deal in the Software without
  * restriction, including without limitation the rights to use,
  * copy, modify, merge, publish, distribute, sublicense, and/or sell
  * copies of the Software, and to permit persons to whom the
  * Software is furnished to do so, subject to the following
  * conditions:
  *
  * The above copyright notice and this permission notice shall be
  * included in all copies or substantial portions of the Software.
  * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
  * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
  * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
  * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
  * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
  * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
  * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
  * OTHER DEALINGS IN THE SOFTWARE.
  */
 
 package org.broadinstitute.sting.gatk.walkers.annotator;
 
 import org.apache.log4j.Logger;
 import org.broadinstitute.sting.commandline.RodBinding;
 import org.broadinstitute.sting.gatk.GenomeAnalysisEngine;
 import org.broadinstitute.sting.gatk.contexts.AlignmentContext;
 import org.broadinstitute.sting.gatk.contexts.ReferenceContext;
 import org.broadinstitute.sting.gatk.refdata.RefMetaDataTracker;
 import org.broadinstitute.sting.gatk.walkers.annotator.interfaces.AnnotatorCompatibleWalker;
 import org.broadinstitute.sting.gatk.walkers.annotator.interfaces.ExperimentalAnnotation;
 import org.broadinstitute.sting.gatk.walkers.annotator.interfaces.InfoFieldAnnotation;
 import org.broadinstitute.sting.utils.Utils;
 import org.broadinstitute.sting.utils.codecs.vcf.*;
 import org.broadinstitute.sting.utils.exceptions.UserException;
 import org.broadinstitute.sting.utils.variantcontext.VariantContext;
 
 import java.util.*;
 
 /**
  * A set of genomic annotations based on the output of the SnpEff variant effect predictor tool
  * (http://snpeff.sourceforge.net/).
  *
  * For each variant, chooses one of the effects of highest biological impact from the SnpEff
 * output file (which must be provided on the command line via --snpEffFile <filename>.vcf),
  * and adds annotations on that effect.
  *
  * @author David Roazen
  */
 public class SnpEff extends InfoFieldAnnotation implements ExperimentalAnnotation {
 
     private static Logger logger = Logger.getLogger(SnpEff.class);
 
     // We refuse to parse SnpEff output files generated by unsupported versions, or
     // lacking a SnpEff version number in the VCF header:
     public static final String[] SUPPORTED_SNPEFF_VERSIONS = { "2.0.2" };
     public static final String SNPEFF_VCF_HEADER_VERSION_LINE_KEY = "SnpEffVersion";
 
     // SnpEff aggregates all effects (and effect metadata) together into a single INFO
     // field annotation with the key EFF:
     public static final String SNPEFF_INFO_FIELD_KEY = "EFF";
     public static final String SNPEFF_EFFECT_METADATA_DELIMITER = "[()]";
     public static final String SNPEFF_EFFECT_METADATA_SUBFIELD_DELIMITER = "\\|";
 
     // Key names for the INFO field annotations we will add to each record, along
     // with parsing-related information:
     public enum InfoFieldKey {
         EFF                   (-1),
         EFF_IMPACT            (0),
         EFF_CODON_CHANGE      (1),
         EFF_AMINO_ACID_CHANGE (2),
         EFF_GENE_NAME         (3),
         EFF_GENE_BIOTYPE      (4),
         EFF_TRANSCRIPT_ID     (6),
         EFF_EXON_ID           (7);
 
         // Index within the effect metadata subfields from the SnpEff EFF annotation
         // where each key's associated value can be found during parsing.
         private final int fieldIndex;
 
         InfoFieldKey ( int fieldIndex ) {
             this.fieldIndex = fieldIndex;
         }
 
         public int getFieldIndex() {
             return fieldIndex;
         }
     }
 
     // Possible SnpEff biological effects. All effect names found in the SnpEff input file
     // are validated against this list.
     public enum EffectType {
         NONE,
         CHROMOSOME,
         INTERGENIC,
         UPSTREAM,
         UTR_5_PRIME,
         UTR_5_DELETED,
         START_GAINED,
         SPLICE_SITE_ACCEPTOR,
         SPLICE_SITE_DONOR,
         START_LOST,
         SYNONYMOUS_START,
         NON_SYNONYMOUS_START,
         CDS,
         GENE,
         TRANSCRIPT,
         EXON,
         EXON_DELETED,
         NON_SYNONYMOUS_CODING,
         SYNONYMOUS_CODING,
         FRAME_SHIFT,
         CODON_CHANGE,
         CODON_INSERTION,
         CODON_CHANGE_PLUS_CODON_INSERTION,
         CODON_DELETION,
         CODON_CHANGE_PLUS_CODON_DELETION,
         STOP_GAINED,
         SYNONYMOUS_STOP,
         NON_SYNONYMOUS_STOP,
         STOP_LOST,
         INTRON,
         UTR_3_PRIME,
         UTR_3_DELETED,
         DOWNSTREAM,
         INTRON_CONSERVED,
         INTERGENIC_CONSERVED,
         REGULATION,
         CUSTOM,
         WITHIN_NON_CODING_GENE
     }
 
     // SnpEff labels each effect as either LOW, MODERATE, or HIGH impact.
     public enum EffectImpact {
         LOW       (1),
         MODERATE  (2),
         HIGH      (3);
 
         private final int severityRating;
 
         EffectImpact ( int severityRating ) {
             this.severityRating = severityRating;
         }
 
         public boolean isHigherImpactThan ( EffectImpact other ) {
             return this.severityRating > other.severityRating;
         }
     }
 
     // SnpEff labels most effects as either CODING or NON_CODING, but sometimes omits this information.
     public enum EffectCoding {
         CODING,
         NON_CODING,
         UNKNOWN
     }
 
 
     public void initialize ( AnnotatorCompatibleWalker walker, GenomeAnalysisEngine toolkit ) {
         validateRodBinding(walker.getSnpEffRodBinding());
         checkSnpEffVersion(walker, toolkit);
     }
 
     public Map<String, Object> annotate ( RefMetaDataTracker tracker, AnnotatorCompatibleWalker walker, ReferenceContext ref, Map<String, AlignmentContext> stratifiedContexts, VariantContext vc ) {
         RodBinding<VariantContext> snpEffRodBinding = walker.getSnpEffRodBinding();
 
         // Get only SnpEff records that start at this locus, not merely span it:
         List<VariantContext> snpEffRecords = tracker.getValues(snpEffRodBinding, ref.getLocus());
 
         // Within this set, look for a SnpEff record whose ref/alt alleles match the record to annotate.
         // If there is more than one such record, we only need to pick the first one, since the biological
         // effects will be the same across all such records:
         VariantContext matchingRecord = getMatchingSnpEffRecord(snpEffRecords, vc);
         if ( matchingRecord == null ) {
             return null;
         }
 
         // Parse the SnpEff INFO field annotation from the matching record into individual effect objects:
         List<SnpEffEffect> effects = parseSnpEffRecord(matchingRecord);
         if ( effects.size() == 0 ) {
             return null;
         }
 
         // Add only annotations for one of the most biologically-significant effects from this set:
         SnpEffEffect mostSignificantEffect = getMostSignificantEffect(effects);
         return mostSignificantEffect.getAnnotations();
     }
 
     private void validateRodBinding ( RodBinding<VariantContext> snpEffRodBinding ) {
         if ( snpEffRodBinding == null || ! snpEffRodBinding.isBound() ) {
             throw new UserException("The SnpEff annotator requires that a SnpEff VCF output file be provided " +
                                     "as a rodbinding on the command line via the --snpEffFile option, but " +
                                     "no SnpEff rodbinding was found.");
         }
     }
 
     private void checkSnpEffVersion ( AnnotatorCompatibleWalker walker, GenomeAnalysisEngine toolkit ) {
         RodBinding<VariantContext> snpEffRodBinding = walker.getSnpEffRodBinding();
 
         VCFHeader snpEffVCFHeader = VCFUtils.getVCFHeadersFromRods(toolkit, Arrays.asList(snpEffRodBinding.getName())).get(snpEffRodBinding.getName());
         VCFHeaderLine snpEffVersionLine = snpEffVCFHeader.getOtherHeaderLine(SNPEFF_VCF_HEADER_VERSION_LINE_KEY);
 
         if ( snpEffVersionLine == null || snpEffVersionLine.getValue() == null || snpEffVersionLine.getValue().trim().length() == 0 ) {
             throw new UserException("Could not find a " + SNPEFF_VCF_HEADER_VERSION_LINE_KEY + " entry in the VCF header for the SnpEff " +
                                     "input file, and so could not verify that the file was generated by a supported version of SnpEff (" +
                                     Arrays.toString(SUPPORTED_SNPEFF_VERSIONS) + ")");
         }
 
         String snpEffVersionString = snpEffVersionLine.getValue().replaceAll("\"", "").split(" ")[0];
 
         if ( ! isSupportedSnpEffVersion(snpEffVersionString) ) {
             throw new UserException("The version of SnpEff used to generate the SnpEff input file (" + snpEffVersionString + ") " +
                                     "is not currently supported by the GATK. Supported versions are: " + Arrays.toString(SUPPORTED_SNPEFF_VERSIONS));
         }
     }
 
     private boolean isSupportedSnpEffVersion ( String versionString ) {
         for ( String supportedVersion : SUPPORTED_SNPEFF_VERSIONS ) {
             if ( supportedVersion.equals(versionString) ) {
                 return true;
             }
         }
 
         return false;
     }
 
     private VariantContext getMatchingSnpEffRecord ( List<VariantContext> snpEffRecords, VariantContext vc ) {
         for ( VariantContext snpEffRecord : snpEffRecords ) {
             if ( snpEffRecord.hasSameAlternateAllelesAs(vc) && snpEffRecord.getReference().equals(vc.getReference()) ) {
                 return snpEffRecord;
             }
         }
 
         return null;
     }
 
     private List<SnpEffEffect> parseSnpEffRecord ( VariantContext snpEffRecord ) {
         List<SnpEffEffect> parsedEffects = new ArrayList<SnpEffEffect>();
 
         Object effectFieldValue = snpEffRecord.getAttribute(SNPEFF_INFO_FIELD_KEY);
         List<String> individualEffects;
 
         // The VCF codec stores multi-valued fields as a List<String>, and single-valued fields as a String.
         // We can have either in the case of SnpEff, since there may be one or more than one effect in this record.
         if ( effectFieldValue instanceof List ) {
             individualEffects = (List<String>)effectFieldValue;
         }
         else {
             individualEffects = Arrays.asList((String)effectFieldValue);
         }
 
         for ( String effectString : individualEffects ) {
             String[] effectNameAndMetadata = effectString.split(SNPEFF_EFFECT_METADATA_DELIMITER);
 
             if ( effectNameAndMetadata.length != 2 ) {
                 logger.warn(String.format("Malformed SnpEff effect field at %s:%d, skipping: %s",
                                           snpEffRecord.getChr(), snpEffRecord.getStart(), effectString));
                 continue;
             }
 
             String effectName = effectNameAndMetadata[0];
             String[] effectMetadata = effectNameAndMetadata[1].split(SNPEFF_EFFECT_METADATA_SUBFIELD_DELIMITER, -1);
 
             SnpEffEffect parsedEffect = new SnpEffEffect(effectName, effectMetadata);
 
             if ( parsedEffect.isWellFormed() ) {
                 parsedEffects.add(parsedEffect);
             }
             else {
                 logger.warn(String.format("Skipping malformed SnpEff effect field at %s:%d. Error was: \"%s\". Field was: \"%s\"",
                                           snpEffRecord.getChr(), snpEffRecord.getStart(), parsedEffect.getParseError(), effectString));
             }
         }
 
         return parsedEffects;
     }
 
     private SnpEffEffect getMostSignificantEffect ( List<SnpEffEffect> effects ) {
         SnpEffEffect mostSignificantEffect = null;
 
         for ( SnpEffEffect effect : effects ) {
             if ( mostSignificantEffect == null ||
                  effect.isHigherImpactThan(mostSignificantEffect) ) {
 
                 mostSignificantEffect = effect;
             }
         }
 
         return mostSignificantEffect;
     }
 
     public List<String> getKeyNames() {
         return Arrays.asList( InfoFieldKey.EFF.toString(),
                               InfoFieldKey.EFF_IMPACT.toString(),
                               InfoFieldKey.EFF_CODON_CHANGE.toString(),
                               InfoFieldKey.EFF_AMINO_ACID_CHANGE.toString(),
                               InfoFieldKey.EFF_GENE_NAME.toString(),
                               InfoFieldKey.EFF_GENE_BIOTYPE.toString(),
                               InfoFieldKey.EFF_TRANSCRIPT_ID.toString(),
                               InfoFieldKey.EFF_EXON_ID.toString()
                             );
     }
 
     public List<VCFInfoHeaderLine> getDescriptions() {
         return Arrays.asList(
             new VCFInfoHeaderLine(InfoFieldKey.EFF.toString(),                   1, VCFHeaderLineType.String,  "The highest-impact effect resulting from the current variant (or one of the highest-impact effects, if there is a tie)"),
             new VCFInfoHeaderLine(InfoFieldKey.EFF_IMPACT.toString(),            1, VCFHeaderLineType.String,  "Impact of the highest-impact effect resulting from the current variant " + Arrays.toString(EffectImpact.values())),
             new VCFInfoHeaderLine(InfoFieldKey.EFF_CODON_CHANGE.toString(),      1, VCFHeaderLineType.String,  "Old/New codon for the highest-impact effect resulting from the current variant"),
             new VCFInfoHeaderLine(InfoFieldKey.EFF_AMINO_ACID_CHANGE.toString(), 1, VCFHeaderLineType.String,  "Old/New amino acid for the highest-impact effect resulting from the current variant"),
             new VCFInfoHeaderLine(InfoFieldKey.EFF_GENE_NAME.toString(),         1, VCFHeaderLineType.String,  "Gene name for the highest-impact effect resulting from the current variant"),
             new VCFInfoHeaderLine(InfoFieldKey.EFF_GENE_BIOTYPE.toString(),      1, VCFHeaderLineType.String,  "Gene biotype for the highest-impact effect resulting from the current variant"),
             new VCFInfoHeaderLine(InfoFieldKey.EFF_TRANSCRIPT_ID.toString(),     1, VCFHeaderLineType.String,  "Transcript ID for the highest-impact effect resulting from the current variant"),
             new VCFInfoHeaderLine(InfoFieldKey.EFF_EXON_ID.toString(),           1, VCFHeaderLineType.String,  "Exon ID for the highest-impact effect resulting from the current variant")
         );
     }
 
     /**
      * Helper class to parse, validate, and store a single SnpEff effect and its metadata.
      */
     protected static class SnpEffEffect {
         private EffectType effect;
         private EffectImpact impact;
         private String codonChange;
         private String aminoAcidChange;
         private String geneName;
         private String geneBiotype;
         private EffectCoding coding;
         private String transcriptID;
         private String exonID;
 
         private String parseError = null;
         private boolean isWellFormed = true;
 
         private static final int EXPECTED_NUMBER_OF_METADATA_FIELDS = 8;
         private static final int NUMBER_OF_METADATA_FIELDS_UPON_WARNING = 9;
         private static final int NUMBER_OF_METADATA_FIELDS_UPON_ERROR = 10;
 
         // Note that contrary to the description for the EFF field layout that SnpEff adds to the VCF header,
         // errors come after warnings, not vice versa:
         private static final int SNPEFF_WARNING_FIELD_INDEX = NUMBER_OF_METADATA_FIELDS_UPON_WARNING - 1;
         private static final int SNPEFF_ERROR_FIELD_INDEX = NUMBER_OF_METADATA_FIELDS_UPON_ERROR - 1;
 
         private static final int SNPEFF_CODING_FIELD_INDEX = 5;
 
         public SnpEffEffect ( String effectName, String[] effectMetadata ) {
             parseEffectName(effectName);
             parseEffectMetadata(effectMetadata);
         }
 
         private void parseEffectName ( String effectName ) {
             try {
                 effect = EffectType.valueOf(effectName);
             }
             catch ( IllegalArgumentException e ) {
                 parseError(String.format("%s is not a recognized effect type", effectName));
             }
         }
 
         private void parseEffectMetadata ( String[] effectMetadata ) {
             if ( effectMetadata.length != EXPECTED_NUMBER_OF_METADATA_FIELDS ) {
                 if ( effectMetadata.length == NUMBER_OF_METADATA_FIELDS_UPON_WARNING ) {
                     parseError(String.format("SnpEff issued the following warning: %s", effectMetadata[SNPEFF_WARNING_FIELD_INDEX]));
                 }
                 else if ( effectMetadata.length == NUMBER_OF_METADATA_FIELDS_UPON_ERROR ) {
                     parseError(String.format("SnpEff issued the following error: %s", effectMetadata[SNPEFF_ERROR_FIELD_INDEX]));
                 }
                 else {
                     parseError(String.format("Wrong number of effect metadata fields. Expected %d but found %d",
                                              EXPECTED_NUMBER_OF_METADATA_FIELDS, effectMetadata.length));
                 }
 
                 return;
             }
 
             try {
                 impact = EffectImpact.valueOf(effectMetadata[InfoFieldKey.EFF_IMPACT.getFieldIndex()]);
             }
             catch ( IllegalArgumentException e ) {
                 parseError(String.format("Unrecognized value for effect impact: %s", effectMetadata[InfoFieldKey.EFF_IMPACT.getFieldIndex()]));
             }
 
             codonChange = effectMetadata[InfoFieldKey.EFF_CODON_CHANGE.getFieldIndex()];
             aminoAcidChange = effectMetadata[InfoFieldKey.EFF_AMINO_ACID_CHANGE.getFieldIndex()];
             geneName = effectMetadata[InfoFieldKey.EFF_GENE_NAME.getFieldIndex()];
             geneBiotype = effectMetadata[InfoFieldKey.EFF_GENE_BIOTYPE.getFieldIndex()];
 
             if ( effectMetadata[SNPEFF_CODING_FIELD_INDEX].trim().length() > 0 ) {
                 try {
                     coding = EffectCoding.valueOf(effectMetadata[SNPEFF_CODING_FIELD_INDEX]);
                 }
                 catch ( IllegalArgumentException e ) {
                     parseError(String.format("Unrecognized value for effect coding: %s", effectMetadata[SNPEFF_CODING_FIELD_INDEX]));
                 }
             }
             else {
                 coding = EffectCoding.UNKNOWN;
             }
 
             transcriptID = effectMetadata[InfoFieldKey.EFF_TRANSCRIPT_ID.getFieldIndex()];
             exonID = effectMetadata[InfoFieldKey.EFF_EXON_ID.getFieldIndex()];
         }
 
         private void parseError ( String message ) {
             isWellFormed = false;
 
             // Cache only the first error encountered:
             if ( parseError == null ) {
                 parseError = message;
             }
         }
 
         public boolean isWellFormed() {
             return isWellFormed;
         }
 
         public String getParseError() {
             return parseError == null ? "" : parseError;
         }
 
         public boolean isCoding() {
             return coding == EffectCoding.CODING;
         }
 
         public boolean isHigherImpactThan ( SnpEffEffect other ) {
             // If one effect is within a coding gene and the other is not, the effect that is
             // within the coding gene has higher impact:
 
             if ( isCoding() && ! other.isCoding() ) {
                 return true;
             }
             else if ( ! isCoding() && other.isCoding() ) {
                 return false;
             }
 
             // Otherwise, both effects are either in or not in a coding gene, so we compare the impacts
             // of the effects themselves:
 
             return impact.isHigherImpactThan(other.impact);
         }
 
         public Map<String, Object> getAnnotations() {
             Map<String, Object> annotations = new LinkedHashMap<String, Object>(Utils.optimumHashSize(InfoFieldKey.values().length));
 
             addAnnotation(annotations, InfoFieldKey.EFF.toString(), effect.toString());
             addAnnotation(annotations, InfoFieldKey.EFF_IMPACT.toString(), impact.toString());
             addAnnotation(annotations, InfoFieldKey.EFF_CODON_CHANGE.toString(), codonChange);
             addAnnotation(annotations, InfoFieldKey.EFF_AMINO_ACID_CHANGE.toString(), aminoAcidChange);
             addAnnotation(annotations, InfoFieldKey.EFF_GENE_NAME.toString(), geneName);
             addAnnotation(annotations, InfoFieldKey.EFF_GENE_BIOTYPE.toString(), geneBiotype);
             addAnnotation(annotations, InfoFieldKey.EFF_TRANSCRIPT_ID.toString(), transcriptID);
             addAnnotation(annotations, InfoFieldKey.EFF_EXON_ID.toString(), exonID);
 
             return annotations;
         }
 
         private void addAnnotation ( Map<String, Object> annotations, String keyName, String keyValue ) {
             // Only add annotations for keys associated with non-empty values:
             if ( keyValue != null && keyValue.trim().length() > 0 ) {
                 annotations.put(keyName, keyValue);
             }
         }
     }
 }
