 /*
  * Copyright (c) 2010.
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
  *
  * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
  * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
  * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
  * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
  * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
  * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
  * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR
  * THE USE OR OTHER DEALINGS IN THE SOFTWARE.
  */
 
 package org.broadinstitute.sting.gatk.walkers.genotyper;
 
 import net.sf.samtools.SAMRecord;
 import net.sf.samtools.SAMUtils;
 import org.broadinstitute.sting.utils.*;
 import org.broadinstitute.sting.utils.exceptions.UserException;
 import org.broadinstitute.sting.utils.sam.GATKSAMRecord;
 import org.broadinstitute.sting.utils.pileup.ReadBackedPileup;
 import org.broadinstitute.sting.utils.pileup.PileupElement;
 import org.broadinstitute.sting.utils.genotype.DiploidGenotype;
 
 import static java.lang.Math.log10;
 import static java.lang.Math.pow;
 
 /**
  * Stable, error checking version of the Bayesian genotyper.  Useful for calculating the likelihoods, priors,
  * and posteriors given a pile of bases and quality scores
  *
  * Suppose we have bases b1, b2, ..., bN with qualities scores q1, q2, ..., qN.  This object
  * calculates:
  *
  * P(G | D) = P(G) * P(D | G)
  *
  * where
  *
  * P(D | G) = sum_i log10 P(bi | G)
  *
  * and
  *
  * P(bi | G) = 1 - P(error | q1) if bi is in G
  *           = P(error | q1) / 3 if bi is not in G
  *
  * for homozygous genotypes and for heterozygous genotypes:
  *
  * P(bi | G) = 1 - P(error | q1) / 2 + P(error | q1) / 6 if bi is in G
  *           = P(error | q1) / 3 if bi is not in G
  *
  * for each of the 10 unique diploid genotypes AA, AC, AG, .., TT
  *
  * Everything is stored as arrays indexed by DiploidGenotype.ordinal() values in log10 space.
  *
  * The priors contain the relative probabilities of each genotype, and must be provided at object creation.
  * From then on, you can call any of the add() routines to update the likelihoods and posteriors in the above
  * model.
  */
 public class DiploidSNPGenotypeLikelihoods implements Cloneable {
     protected final static int FIXED_PLOIDY = 2;
     protected final static int MAX_PLOIDY = FIXED_PLOIDY + 1;
     protected final static double ploidyAdjustment = log10(FIXED_PLOIDY);
     protected final static double log10_3 = log10(3.0);
 
     protected boolean VERBOSE = false;
 
     //
     // The fundamental data arrays associated with a Genotype Likelhoods object
     //
     protected double[] log10Likelihoods = null;
     protected double[] log10Posteriors = null;
 
     protected DiploidSNPGenotypePriors priors = null;
 
     /**
      * Create a new GenotypeLikelhoods object with flat priors for each diploid genotype
      *
      */
     public DiploidSNPGenotypeLikelihoods() {
         this.priors = new DiploidSNPGenotypePriors();
         setToZero();
     }
 
     /**
      * Create a new GenotypeLikelhoods object with given priors for each diploid genotype
      *
      * @param priors priors
      */
     public DiploidSNPGenotypeLikelihoods(DiploidSNPGenotypePriors priors) {
         this.priors = priors;
         setToZero();
     }
 
     /**
      * Cloning of the object
      * @return clone
      * @throws CloneNotSupportedException
      */
     protected Object clone() throws CloneNotSupportedException {
         DiploidSNPGenotypeLikelihoods c = (DiploidSNPGenotypeLikelihoods)super.clone();
         c.priors = priors;
         c.log10Likelihoods = log10Likelihoods.clone();
         c.log10Posteriors = log10Posteriors.clone();
         return c;
     }
 
     protected void setToZero() {
         log10Likelihoods = genotypeZeros.clone();                 // likelihoods are all zeros
         log10Posteriors = priors.getPriors().clone();     // posteriors are all the priors
     }
 
     /**
      * Returns an array of log10 likelihoods for each genotype, indexed by DiploidGenotype.ordinal values()
      * @return likelihoods array
      */
     public double[] getLikelihoods() {
         return log10Likelihoods;
     }
 
     /**
      * Returns the likelihood associated with DiploidGenotype g
      * @param g genotype
      * @return log10 likelihood as a double
      */
     public double getLikelihood(DiploidGenotype g) {
         return getLikelihoods()[g.ordinal()];
     }
 
     /**
      * Returns an array of posteriors for each genotype, indexed by DiploidGenotype.ordinal values().
      *
      * @return raw log10 (not-normalized posteriors) as a double array
      */
     public double[] getPosteriors() {
         return log10Posteriors;
     }
 
     /**
      * Returns the posterior associated with DiploidGenotype g
      * @param g genotpe
      * @return raw log10 (not-normalized posteror) as a double
      */
     public double getPosterior(DiploidGenotype g) {
         return getPosteriors()[g.ordinal()];
     }
 
 
     /**
      * Returns an array of posteriors for each genotype, indexed by DiploidGenotype.ordinal values().
      *
      * @return normalized posterors as a double array
      */
     public double[] getNormalizedPosteriors() {
         double[] normalized = new double[log10Posteriors.length];
         double sum = 0.0;
 
         // for precision purposes, we need to add (or really subtract, since everything is negative)
         // the largest posterior value from all entries so that numbers don't get too small
         double maxValue = log10Posteriors[0];
         for (int i = 1; i < log10Posteriors.length; i++) {
             if ( maxValue < log10Posteriors[i] )
                 maxValue = log10Posteriors[i];
         }
 
         // collect the posteriors
         for ( DiploidGenotype g : DiploidGenotype.values() ) {
             double posterior = Math.pow(10, getPosterior(g) - maxValue);
             normalized[g.ordinal()] = posterior;
             sum += posterior;
         }
 
         // normalize
         for (int i = 0; i < normalized.length; i++)
             normalized[i] /= sum;
 
         return normalized;
     }
 
 
 
     public DiploidSNPGenotypePriors getPriorObject() {
         return priors;
     }
 
     /**
      * Returns an array of priors for each genotype, indexed by DiploidGenotype.ordinal values().
      *
      * @return log10 prior as a double array
      */
     public double[] getPriors() {
         return priors.getPriors();
     }
 
     /**
      * Sets the priors
      * @param priors priors
      */
     public void setPriors(DiploidSNPGenotypePriors priors) {
         this.priors = priors;
         log10Posteriors = genotypeZeros.clone();
         for ( DiploidGenotype g : DiploidGenotype.values() ) {
             int i = g.ordinal();
             log10Posteriors[i] = priors.getPriors()[i] + log10Likelihoods[i];
         }
     }
 
     /**
      * Returns the prior associated with DiploidGenotype g
      * @param g genotype
      * @return log10 prior as a double
      */
     public double getPrior(DiploidGenotype g) {
         return getPriors()[g.ordinal()];
     }
 
     public int add(ReadBackedPileup pileup) {
         return add(pileup, false, false);
     }
 
     /**
      * Updates likelihoods and posteriors to reflect the additional observations contained within the
      * read-based pileup up by calling add(observedBase, qualityScore) for each base / qual in the
      * pileup
      *
      * @param pileup                    read pileup
      * @param ignoreBadBases            should we ignore bad bases?
      * @param capBaseQualsAtMappingQual should we cap a base's quality by its read's mapping quality?
      * @return the number of good bases found in the pileup
      */
     public int add(ReadBackedPileup pileup, boolean ignoreBadBases, boolean capBaseQualsAtMappingQual) {
         int n = 0;
 
         // todo: first validate that my changes were good by passing in fragments representing just a single read...
         //Set<PerFragmentPileupElement> fragments = new HashSet<PerFragmentPileupElement>();
         for ( PileupElement p : pileup ) {
             if ( usableBase(p, ignoreBadBases) ) {
 
                 /****
                 Set<PileupElement> fragment = new HashSet<PileupElement>();
                 fragment.add(p);
                 fragments.add(new PerFragmentPileupElement(fragment));
             }
         }
 
         // for each fragment, add to the likelihoods
         for ( PerFragmentPileupElement fragment : fragments ) {
             n += add(fragment, capBaseQualsAtMappingQual);
         }
         ****/
 
                 byte qual = capBaseQualsAtMappingQual ? (byte)Math.min((int)p.getQual(), p.getMappingQual()) : p.getQual();
                 n += add(p.getBase(), qual, p.getRead(), p.getOffset());
             }
         }
 
         return n;
     }
 
     // public int add(PerFragmentPileupElement fragment, boolean capBaseQualsAtMappingQual) {
 
     public int add(byte observedBase, byte qualityScore, SAMRecord read, int offset) {
         if ( qualityScore == 0 ) { // zero quals are wrong
             return 0;
         }
 
         // Just look up the cached result if it's available, or compute and store it
         DiploidSNPGenotypeLikelihoods gl;
         if ( ! inCache( observedBase, qualityScore, FIXED_PLOIDY, read) ) {
             gl = calculateCachedGenotypeLikelihoods(observedBase, qualityScore, FIXED_PLOIDY, read, offset);
         } else {
             gl = getCachedGenotypeLikelihoods(observedBase, qualityScore, FIXED_PLOIDY, read);
         }
 
         // for bad bases, there are no likelihoods
         if ( gl == null )
             return 0;
 
         double[] likelihoods = gl.getLikelihoods();
 
         for ( DiploidGenotype g : DiploidGenotype.values() ) {
             double likelihood = likelihoods[g.ordinal()];
             
             if ( VERBOSE ) {
                 boolean fwdStrand = ! read.getReadNegativeStrandFlag();
                 System.out.printf("  L(%c | G=%s, Q=%d, S=%s) = %f / %f%n",
                         observedBase, g, qualityScore, fwdStrand ? "+" : "-", pow(10,likelihood) * 100, likelihood);
             }
 
             log10Likelihoods[g.ordinal()] += likelihood;
             log10Posteriors[g.ordinal()] += likelihood;
         }
 
         return 1;
     }
 
     static DiploidSNPGenotypeLikelihoods[][][][] CACHE = new DiploidSNPGenotypeLikelihoods[BaseUtils.BASES.length][QualityUtils.MAX_QUAL_SCORE+1][MAX_PLOIDY][2];
 
     protected boolean inCache( byte observedBase, byte qualityScore, int ploidy, SAMRecord read) {
         return getCache(CACHE, observedBase, qualityScore, ploidy, read) != null;
     }
 
     protected DiploidSNPGenotypeLikelihoods getCachedGenotypeLikelihoods( byte observedBase, byte qualityScore, int ploidy, SAMRecord read) {
         DiploidSNPGenotypeLikelihoods gl = getCache(CACHE, observedBase, qualityScore, ploidy, read);
         if ( gl == null )
             throw new RuntimeException(String.format("BUG: trying to fetch an unset cached genotype likelihood at base=%c, qual=%d, ploidy=%d, read=%s",
                     observedBase, qualityScore, ploidy, read));
         return gl;
     }
 
     protected DiploidSNPGenotypeLikelihoods calculateCachedGenotypeLikelihoods(byte observedBase, byte qualityScore, int ploidy, SAMRecord read, int offset) {
         DiploidSNPGenotypeLikelihoods gl = calculateGenotypeLikelihoods(observedBase, qualityScore, read, offset);
         setCache(CACHE, observedBase, qualityScore, ploidy, read, gl);
         return gl;
     }
 
     protected void setCache( DiploidSNPGenotypeLikelihoods[][][][] cache,
                              byte observedBase, byte qualityScore, int ploidy,
                              SAMRecord read, DiploidSNPGenotypeLikelihoods val ) {
         int i = BaseUtils.simpleBaseToBaseIndex(observedBase);
         int j = qualityScore;
         if ( j > SAMUtils.MAX_PHRED_SCORE )
            throw new UserException.MalformedBam(read, String.format("the maximum allowed quality score is %d, but a quality of %d was observed in read %s.  Perhaps your BAM incorrectly encodes the quality scores in Sanger format; see http://en.wikipedia.org/wiki/FASTQ_format for more details", SAMUtils.MAX_PHRED_SCORE, j, read.getReadName()));
         int k = ploidy;
         int x = strandIndex(! read.getReadNegativeStrandFlag() );
 
         cache[i][j][k][x] = val;
     }
 
     protected DiploidSNPGenotypeLikelihoods getCache( DiploidSNPGenotypeLikelihoods[][][][] cache,
                                             byte observedBase, byte qualityScore, int ploidy, SAMRecord read) {
         int i = BaseUtils.simpleBaseToBaseIndex(observedBase);
         int j = qualityScore;
         if ( j > SAMUtils.MAX_PHRED_SCORE )
            throw new UserException.MalformedBam(read, String.format("the maximum allowed quality score is %d, but a quality of %d was observed in read %s.  Perhaps your BAM incorrectly encodes the quality scores in Sanger format; see http://en.wikipedia.org/wiki/FASTQ_format for more details", SAMUtils.MAX_PHRED_SCORE, j, read.getReadName()));
         int k = ploidy;
         int x = strandIndex(! read.getReadNegativeStrandFlag() );
         return cache[i][j][k][x];
     }
 
     protected DiploidSNPGenotypeLikelihoods calculateGenotypeLikelihoods(byte observedBase, byte qualityScore, SAMRecord read, int offset) {
         double[] log10FourBaseLikelihoods = computeLog10Likelihoods(observedBase, qualityScore, read);
 
         try {
 
             DiploidSNPGenotypeLikelihoods gl = (DiploidSNPGenotypeLikelihoods)this.clone();
             gl.setToZero();
 
             // we need to adjust for ploidy.  We take the raw p(obs | chrom) / ploidy, which is -log10(ploidy) in log space
             for ( DiploidGenotype g : DiploidGenotype.values() ) {
 
                 // todo assumes ploidy is 2 -- should be generalized.  Obviously the below code can be turned into a loop
                 double p_base = 0.0;
                 p_base += pow(10, log10FourBaseLikelihoods[BaseUtils.simpleBaseToBaseIndex(g.base1)] - ploidyAdjustment);
                 p_base += pow(10, log10FourBaseLikelihoods[BaseUtils.simpleBaseToBaseIndex(g.base2)] - ploidyAdjustment);
                 double likelihood = log10(p_base);
 
                 gl.log10Likelihoods[g.ordinal()] += likelihood;
                 gl.log10Posteriors[g.ordinal()] += likelihood;
             }
 
             if ( VERBOSE ) {
                 for ( DiploidGenotype g : DiploidGenotype.values() ) { System.out.printf("%s\t", g); }
                 System.out.println();
                 for ( DiploidGenotype g : DiploidGenotype.values() ) { System.out.printf("%.2f\t", gl.log10Likelihoods[g.ordinal()]); }
                 System.out.println();
             }
 
             return gl;
 
          } catch ( CloneNotSupportedException e ) {
              throw new RuntimeException(e);
          }
     }
 
     /**
      * Updates likelihoods and posteriors to reflect an additional observation of observedBase with
      * qualityScore.
      *
      * @param observedBase observed base
      * @param qualityScore base quality
      * @param read         SAM read
      * @return likelihoods for this observation or null if the base was not considered good enough to add to the likelihoods (Q0 or 'N', for example)
      */
     protected double[] computeLog10Likelihoods(byte observedBase, byte qualityScore, SAMRecord read) {
         double[] log10FourBaseLikelihoods = baseZeros.clone();
 
         for ( byte base : BaseUtils.BASES ) {
             double likelihood = log10PofObservingBaseGivenChromosome(observedBase, base, qualityScore);
 
             if ( VERBOSE ) {
                 boolean fwdStrand = ! read.getReadNegativeStrandFlag();
                 System.out.printf("  L(%c | b=%s, Q=%d, S=%s) = %f / %f%n",
                         observedBase, base, qualityScore, fwdStrand ? "+" : "-", pow(10,likelihood) * 100, likelihood);
             }
 
             log10FourBaseLikelihoods[BaseUtils.simpleBaseToBaseIndex(base)] = likelihood;
         }
 
         return log10FourBaseLikelihoods;
     }
 
     /**
      *
      * @param observedBase observed base
      * @param chromBase    target base
      * @param qual         base quality
      * @return log10 likelihood
      */
     protected double log10PofObservingBaseGivenChromosome(byte observedBase, byte chromBase, byte qual) {
 
         double logP;
 
         if ( observedBase == chromBase ) {
             // the base is consistent with the chromosome -- it's 1 - e
             //logP = oneMinusData[qual];
             double e = pow(10, (qual / -10.0));
             logP = log10(1.0 - e);
         } else {
             // the base is inconsistent with the chromosome -- it's e * P(chromBase | observedBase is an error)
             logP = qual / -10.0 + (-log10_3);
         }
 
         //System.out.printf("%c %c %d => %f%n", observedBase, chromBase, qual, logP);
         return logP;
     }
 
     // -----------------------------------------------------------------------------------------------------------------
     //
     //
     // helper routines
     //
     //
     // -----------------------------------------------------------------------------------------------------------------
 
     public static int strandIndex(boolean fwdStrand) {
         return fwdStrand ? 0 : 1;
     }
 
     /**
      * Returns true when the observedBase is considered usable.
      * @param p          pileup element
      * @param ignoreBadBases should we ignore bad bases?
      * @return true if the base is a usable base
      */
     protected static boolean usableBase(PileupElement p, boolean ignoreBadBases) {
         // ignore deletions and filtered bases
         if ( p.isDeletion() ||
                 (p.getRead() instanceof GATKSAMRecord &&
                  !((GATKSAMRecord)p.getRead()).isGoodBase(p.getOffset())) )
             return false;
 
         return ( !ignoreBadBases || !badBase(p.getBase()) );
     }
 
     /**
      * Returns true when the observedBase is considered bad and shouldn't be processed by this object.  A base
      * is considered bad if:
      *
      *   Criterion 1: observed base isn't a A,C,T,G or lower case equivalent
      *
      * @param observedBase observed base
      * @return true if the base is a bad base
      */
     protected static boolean badBase(byte observedBase) {
         return BaseUtils.simpleBaseToBaseIndex(observedBase) == -1;
     }
 
     /**
      * Return a string representation of this object in a moderately usable form
      *
      * @return string representation
      */
     public String toString() {
         double sum = 0;
         StringBuilder s = new StringBuilder();
         for (DiploidGenotype g : DiploidGenotype.values()) {
             s.append(String.format("%s %.10f ", g, log10Likelihoods[g.ordinal()]));
 			sum += Math.pow(10,log10Likelihoods[g.ordinal()]);
         }
 		s.append(String.format(" %f", sum));
         return s.toString();
     }
 
     // -----------------------------------------------------------------------------------------------------------------
     //
     //
     // Validation routines
     //
     //
     // -----------------------------------------------------------------------------------------------------------------
 
     public boolean validate() {
         return validate(true);
     }
 
     public boolean validate(boolean throwException) {
         try {
             priors.validate(throwException);
 
             for ( DiploidGenotype g : DiploidGenotype.values() ) {
                 String bad = null;
 
                 int i = g.ordinal();
                 if ( ! MathUtils.wellFormedDouble(log10Likelihoods[i]) || ! MathUtils.isNegativeOrZero(log10Likelihoods[i]) ) {
                     bad = String.format("Likelihood %f is badly formed", log10Likelihoods[i]);
                 } else if ( ! MathUtils.wellFormedDouble(log10Posteriors[i]) || ! MathUtils.isNegativeOrZero(log10Posteriors[i]) ) {
                     bad = String.format("Posterior %f is badly formed", log10Posteriors[i]);
                 }
 
                 if ( bad != null ) {
                     throw new IllegalStateException(String.format("At %s: %s", g.toString(), bad));
                 }
             }
         } catch ( IllegalStateException e ) {
             if ( throwException )
                 throw new RuntimeException(e);
             else
                 return false;
         }
 
         return true;
     }
 
     //
     // Constant static data
     //
     private final static double[] genotypeZeros = new double[DiploidGenotype.values().length];
     private final static double[] baseZeros = new double[BaseUtils.BASES.length];
 
     static {
         for ( DiploidGenotype g : DiploidGenotype.values() ) {
             genotypeZeros[g.ordinal()] = 0.0;
         }
         for ( byte base : BaseUtils.BASES ) {
             baseZeros[BaseUtils.simpleBaseToBaseIndex(base)] = 0.0;
         }
     }
 }
