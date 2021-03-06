 package org.broadinstitute.sting.utils;
 
 import net.sf.functionalj.reflect.StdReflect;
 import net.sf.functionalj.reflect.JdkStdReflect;
 import net.sf.functionalj.FunctionN;
 import net.sf.functionalj.Function1;
 import net.sf.functionalj.Functions;
 import net.sf.functionalj.util.Operators;
 import net.sf.samtools.SAMSequenceDictionary;
 import net.sf.samtools.SAMSequenceRecord;
 
 import java.util.*;
 import java.util.regex.Pattern;
 import java.util.regex.Matcher;
 
 import edu.mit.broad.picard.reference.ReferenceSequenceFile;
 import org.apache.log4j.Logger;
 
 /**
  * Created by IntelliJ IDEA.
  * User: mdepristo
  * Date: Mar 2, 2009
  * Time: 8:50:11 AM
  *
  * Genome location representation.  It is *** 1 *** based
  *
  *
  */
 public class GenomeLoc implements Comparable<GenomeLoc> {
     private static Logger logger = Logger.getLogger(GenomeLoc.class);
 
     private String contig;
     private long start;
     private long stop;
 
     // --------------------------------------------------------------------------------------------------------------
     //
     // Ugly global variable defining the optional ordering of contig elements
     //
     // --------------------------------------------------------------------------------------------------------------
     //public static Map<String, Integer> refContigOrdering = null;
     private static SAMSequenceDictionary contigInfo = null;
     private static HashMap<String, String> interns = null;
 
     public static boolean hasKnownContigOrdering() {
         return contigInfo != null;
     }
 
     public static SAMSequenceRecord getContigInfo( final String contig ) {
         return contigInfo.getSequence(contig);
     }
     
     public static int getContigIndex( final String contig ) {
         return contigInfo.getSequenceIndex(contig);
     }
 
     /*
     public static void setContigOrdering(Map<String, Integer> rco) {
         refContigOrdering = rco;
         interns = new HashMap<String, String>();
         for ( String contig : rco.keySet() )
             interns.put( contig, contig );
     }
     */
 
     /*
     public static boolean setupRefContigOrdering(final ReferenceSequenceFile refFile) {
         final SAMSequenceDictionary seqDict = refFile.getSequenceDictionary();
 
         if (seqDict == null) // we couldn't load the reference dictionary
             return false;
         
         List<SAMSequenceRecord> refContigs = seqDict.getSequences();
         HashMap<String, Integer> refContigOrdering = new HashMap<String, Integer>();
 
         if (refContigs != null) {
             int i = 0;
             logger.info(String.format("Prepared reference sequence contig dictionary%n  order ->"));
             for (SAMSequenceRecord contig : refContigs) {
                 logger.info(String.format(" %s (%d bp)", contig.getSequenceName(), contig.getSequenceLength()));
                 refContigOrdering.put(contig.getSequenceName(), i);
                 i++;
             }
         }
 
         setContigOrdering(refContigOrdering);
         return refContigs != null;
     }
     */
 
     public static boolean setupRefContigOrdering(final ReferenceSequenceFile refFile) {
         return setupRefContigOrdering(refFile.getSequenceDictionary());
     }
 
     public static boolean setupRefContigOrdering(final SAMSequenceDictionary seqDict) {
         if (seqDict == null) // we couldn't load the reference dictionary
             return false;
         else {
             contigInfo = seqDict;
             logger.info(String.format("Prepared reference sequence contig dictionary%n  order ->"));
             for (SAMSequenceRecord contig : seqDict.getSequences() ) {
                 logger.info(String.format(" %s (%d bp)", contig.getSequenceName(), contig.getSequenceLength()));
             }
         }
         
         return true;
     }
 
     // --------------------------------------------------------------------------------------------------------------
     //
     // constructors
     //
     // --------------------------------------------------------------------------------------------------------------
     public GenomeLoc( String contig, final long start, final long stop ) {
         if ( interns != null )
             contig = interns.get(contig);
 
         this.contig = contig;
         this.start = start;
         this.stop = stop;
     }
 
     public GenomeLoc( final String contig, final long pos ) {
         this( contig, pos, pos );
     }
 
     public GenomeLoc( final GenomeLoc toCopy ) {
         this( new String(toCopy.getContig()), toCopy.getStart(), toCopy.getStop() );
     }
 
     // --------------------------------------------------------------------------------------------------------------
     //
     // Parsing string representations
     //
     // --------------------------------------------------------------------------------------------------------------
     private static long parsePosition( final String pos ) {
         String x = pos.replaceAll(",", "");
         return Long.parseLong(x);
     }
 
     public static GenomeLoc parseGenomeLoc( final String str ) {
         // chr2, chr2:1000000 or chr2:1,000,000-2,000,000
         //System.out.printf("Parsing location '%s'%n", str);
 
         final Pattern regex1 = Pattern.compile("([\\w&&[^:]]+)$");             // matches case 1
         final Pattern regex2 = Pattern.compile("([\\w&&[^:]]+):([\\d,]+)$");      // matches case 2
         final Pattern regex3 = Pattern.compile("([\\w&&[^:]]+):([\\d,]+)-([\\d,]+)$");// matches case 3
 
         String contig = null;
         long start = 1;
         long stop = Integer.MAX_VALUE;
         boolean bad = false;
 
         Matcher match1 = regex1.matcher(str);
         Matcher match2 = regex2.matcher(str);
         Matcher match3 = regex3.matcher(str);
 
         try {
             if ( match1.matches() ) {
                 contig = match1.group(1);
             }
             else if ( match2.matches() ) {
                 contig = match2.group(1);
                 start = parsePosition(match2.group(2));
             }
             else if ( match3.matches() ) {
                 contig = match3.group(1);
                 start = parsePosition(match3.group(2));
                 stop = parsePosition(match3.group(3));
 
                 if ( start > stop )
                     bad = true;
             }
             else {
                 bad = true;
             }
         } catch ( Exception e ) {
             bad = true;
         }
 
         if ( bad ) {
             throw new RuntimeException("Invalid Genome Location string: " + str);
         }
 
         if ( stop == Integer.MAX_VALUE && hasKnownContigOrdering() ) {
             // lookup the actually stop position!
             stop = getContigInfo(contig).getSequenceLength();
         }
 
         GenomeLoc loc = new GenomeLoc(contig, start, stop);
         //System.out.printf("  => Parsed location '%s' into %s%n", str, loc);
 
         return loc;
     }
 
     /**
      * Useful utility function that parses a location string into a coordinate-order sorted
      * array of GenomeLoc objects
      *
      * @param str
      * @return Array of GenomeLoc objects corresponding to the locations in the string, sorted by coordinate order
      */
     public static ArrayList<GenomeLoc> parseGenomeLocs(final String str) {
         // Of the form: loc1;loc2;...
         // Where each locN can be:
         // chr2, chr2:1000000 or chr2:1,000,000-2,000,000
         StdReflect reflect = new JdkStdReflect();
         FunctionN<GenomeLoc> parseOne = reflect.staticFunction(GenomeLoc.class, "parseGenomeLoc", String.class);
         Function1<GenomeLoc, String> f1 = parseOne.f1();
         try {
             Collection<GenomeLoc> result = Functions.map(f1, Arrays.asList(str.split(";")));
             ArrayList<GenomeLoc> locs = new ArrayList(result);
             Collections.sort(locs);
             //logger.info(String.format("Going to process %d locations", locs.length));
             locs = mergeOverlappingLocations(locs);
             logger.info("  Locations are:\n" + Utils.join("\n", Functions.map(Operators.toString, locs)));
             return locs;
         } catch (Exception e) {
             e.printStackTrace();
             Utils.scareUser(String.format("Invalid locations string: %s, format is loc1;loc2; where each locN can be 'chr2', 'chr2:1000000' or 'chr2:1,000,000-2,000,000'", str));
             return null;
         }
     }
 
     public static ArrayList<GenomeLoc> mergeOverlappingLocations(final ArrayList<GenomeLoc> raw) {
         logger.info("  Raw locations are:\n" + Utils.join("\n", Functions.map(Operators.toString, raw)));        
         if ( raw.size() <= 1 )
             return raw;
         else {
             ArrayList<GenomeLoc> merged = new ArrayList<GenomeLoc>();
             Iterator<GenomeLoc> it = raw.iterator();
             GenomeLoc prev = it.next();
             while ( it.hasNext() ) {
                 GenomeLoc curr = it.next();
                 if ( prev.contiguousP(curr) ) {
                     prev = prev.merge(curr);
                 } else {
                     merged.add(prev);
                     prev = curr;
                 }
             }
             merged.add(prev);
             return merged;
         }
     }
 
     /**
      * Returns true iff we have a specified series of locations to process AND we are past the last
      * location in the list.  It means that, in a serial processing of the genome, that we are done.
      *
      * @param curr Current genome Location
      * @return true if we are past the last location to process
      */
     public static boolean pastFinalLocation(GenomeLoc curr, ArrayList<GenomeLoc> locs) {
        if ( locs.size() == 0 )
             return false;
         else {
             GenomeLoc last = locs.get(locs.size() - 1);
             return last.compareTo(curr) == -1 && ! last.overlapsP(curr);
         }
     }
 
     /**
      * A key function that returns true if the proposed GenomeLoc curr is within the list of
      * locations we are processing in this TraversalEngine
      *
      * @param curr
      * @return true if we should process GenomeLoc curr, otherwise false
      */
     public static boolean inLocations(GenomeLoc curr, ArrayList<GenomeLoc> locs) {
        if ( locs.size() == 0 ) {
             return true;
         } else {
             for ( GenomeLoc loc : locs ) {
                 //System.out.printf("  Overlap %s vs. %s => %b%n", loc, curr, loc.overlapsP(curr));
                 if (loc.overlapsP(curr))
                     return true;
             }
             return false;
         }
     }
 
     //
     // Accessors and setters
     //
     public final String getContig() { return this.contig; }
     public final long getStart()    { return this.start; }
     public final long getStop()     { return this.stop; }
     public final String toString()  {
         if ( throughEndOfContigP() && atBeginningOfContigP() )
             return getContig();
         else if ( throughEndOfContigP() || getStart() == getStop() )
             return String.format("%s:%d", getContig(), getStart());
         else
             return String.format("%s:%d-%d", getContig(), getStart(), getStop());
     }
 
 
     public final boolean isUnmapped() { return this.contig == null; }
     public final boolean throughEndOfContigP() { return this.stop == Integer.MAX_VALUE; }
     public final boolean atBeginningOfContigP() { return this.start == 1; }
 
     public void setContig(String contig) {
         this.contig = contig;
     }
 
     public void setStart(long start) {
         this.start = start;
     }
     public void setStop(long stop) {
         this.stop = stop;
     }
 
     public final boolean isSingleBP() { return stop == start; }
 
     public final boolean disjointP(GenomeLoc that) {
         if ( compareContigs(this.contig, that.contig) != 0 ) return true;   // different chromosomes
         if ( this.start > that.stop ) return true;                          // this guy is past that
         if ( that.start > this.stop ) return true;                          // that guy is past our start
         return false;
     }
 
     public final boolean discontinuousP(GenomeLoc that) {
         if ( compareContigs(this.contig, that.contig) != 0 ) return true;   // different chromosomes
         if ( (this.start - 1) > that.stop ) return true;                          // this guy is past that
         if ( (that.start - 1) > this.stop ) return true;                          // that guy is past our start
         return false;
     }
 
     public final boolean overlapsP(GenomeLoc that) {
         return ! disjointP( that );
     }
 
     public final boolean contiguousP(GenomeLoc that) {
         return ! discontinuousP( that );
     }
 
     public GenomeLoc merge( GenomeLoc that ) {
         assert this.contiguousP(that);
 
         return new GenomeLoc(getContig(),
                              Math.min(getStart(), that.getStart()),
                              Math.max( getStop(), that.getStop()) );
     }
 
     public final boolean containsP(GenomeLoc that) {
         if ( ! onSameContig(that) ) return false;
         return getStart() <= that.getStart() && getStop() >= that.getStop();
     }
 
     public final boolean onSameContig(GenomeLoc that) {
         return this.contig.equals(that.contig);
     }
 
     public final int minus( final GenomeLoc that ) {
         if ( this.getContig().equals(that.getContig()) )
             return (int) (this.getStart() - that.getStart());
         else
             return Integer.MAX_VALUE;
     }
 
     public final int distance( final GenomeLoc that ) {
         return Math.abs(minus(that));
     }    
 
     public final boolean isBetween( final GenomeLoc left, final GenomeLoc right ) {
         return this.compareTo(left) > -1 && this.compareTo(right) < 1;
     }
 
     public final boolean isPast( GenomeLoc that ) {
         return this.compareContigs(that) == 1 || this.getStart() > that.getStop();
     }
 
     public final void incPos() {
         incPos(1);
     }
     public final void incPos(long by) {
         this.start += by;
         this.stop += by;
     }
 
     public final GenomeLoc nextLoc() {
         GenomeLoc n = new GenomeLoc(this);
         n.incPos();
         return n;
     }
     //
     // Comparison operations
     //
     public static int compareContigs( final String thisContig, final String thatContig ) 
     {
         if ( thisContig == thatContig )
         {
             // Optimization.  If the pointers are equal, then the contigs are equal.
             return 0;
         }
 
         //assert getContigIndex(thisContig) != -1;// : this;
         //assert getContigIndex(thatContig) != -1;// : that;
 
         if ( hasKnownContigOrdering() ) 
         {
             int thisIndex = getContigIndex(thisContig);
             int thatIndex = getContigIndex(thatContig);
 
             if ( thisIndex == -1 )
             {
                 if ( thatIndex == -1 )
                 {
                     // Use regular sorted order
                     return thisContig.compareTo(thatContig);
                 }
                 else 
                 {
                     // this is always bigger if that is in the key set
                     return 1;
                 }
             }
             else if ( thatIndex == -1 )
             {
                 return -1;
             }
             else 
             {
                 if ( thisIndex < thatIndex ) return -1;
                 if ( thisIndex > thatIndex ) return 1;
                 return 0;
             }
         }
         else 
         {
             return thisContig.compareTo(thatContig);
         }
     }
 
     public int compareContigs( GenomeLoc that ) {
         return compareContigs( this.contig, that.contig );
     }
 
 
     public int compareTo( GenomeLoc that ) {
         if ( this == that ) return 0;
 
         final int cmpContig = compareContigs( this.getContig(), that.getContig() );
         if ( cmpContig != 0 ) return cmpContig;
         if ( this.getStart() < that.getStart() ) return -1;
         if ( this.getStart() > that.getStart() ) return 1;
         if ( this.getStop() < that.getStop() ) return -1;
         if ( this.getStop() > that.getStop() ) return 1;
         return 0;
     }
 }
