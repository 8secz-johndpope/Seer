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
  */
 
 package org.biojava.bio.molbio;
 
 import java.util.Iterator;
 
 import org.biojava.bio.BioError;
 import org.biojava.bio.BioException;
 import org.biojava.bio.seq.DNATools;
 import org.biojava.bio.symbol.IllegalAlphabetException;
 import org.biojava.bio.symbol.IllegalSymbolException;
 import org.biojava.bio.symbol.MotifTools;
 import org.biojava.bio.symbol.SymbolList;
 
 /**
  * <code>RestrictionEnzyme</code> represents a restriction enzyme
  * according to the REBASE standard. The cut positions are indicated
  * relative to the 5' end of the recognition site and occur to the
  * right of the given residue. Note that some enzmyes cut in more than
  * one position and that cut positions may occur outside the
  * recognition site.
  *
  * @author Keith James
  */
 public class RestrictionEnzyme
 {
     /**
      * <code>OVERHANG_5PRIME</code> the sticky end type created by
      * enzymes which leave a 5' overhang.
      */
     public static final int OVERHANG_5PRIME = 0;
 
     /**
      * <code>OVERHANG_3PRIME</code> the sticky end type created by
      * enzymes which leave a 3' overhang.
      */
     public static final int OVERHANG_3PRIME = 1;
 
     /**
      * <code>BLUNT</code> the end type created by enzymes which leave
      * a blunt end.
      */
     public static final int BLUNT = 2;
 
     protected String name;
     protected SymbolList site;
     protected int [] dsCutPositions;
     protected int [] usCutPositions;
 
     protected String forwardRegex;
     protected String reverseRegex;
 
     private String summary;
 
     /**
      * Creates a new <code>RestrictionEnzyme</code> which cuts with or
      * downstream of the recognition site. The cut position indices
      * are <strong>always</strong> in the same coordinate space as the
      * recognition site.
      *
      * @param name a <code>String</code> such as EcoRI.
      * @param site a <code>SymbolList</code> recognition site.
      * @param dsForward an <code>int</code> index in the forward
      * strand (the strand conventionally written
      * <strong>5'</strong>-3') of the recognition site at which the
      * cut occurs. The cut occurs between this base and the following
      * one.
      * @param dsReverse an <code>int</code> index in the reverse
      * strand (the strand conventionally written
      * <strong>3'</strong>-5') of the recognition site at which the
      * cut occurs. The cut occurs between this base and the following
      * one.
      *
      * @exception IllegalAlphabetException if an error occurs.
      */
     public RestrictionEnzyme(String name, SymbolList site,
                              int dsForward, int dsReverse)
         throws IllegalAlphabetException
     {
         this(name, site,
              null,
              new int [] { dsForward, dsReverse });
     }
 
     /**
      * Creates a new <code>RestrictionEnzyme</code> of the unusual
     * type which cuts both upstream and downstream of its recognition
      * site. The cut position indices are <strong>always</strong> in
      * the same coordinate space as the recognition site.
      *
      * @param name a <code>String</code> such as Bsp24I.
      * @param site a <code>SymbolList</code> recognition site.
      * @param usForward an <code>int</code> index in the forward
      * strand (the strand conventionally written
      * <strong>5'</strong>-3' upstream of the recognition site at
      * which the cut occurs. The cut occurs between this base and the
      * following one.
      * @param usReverse an <code>int</code> index in the reverse
      * strand (the strand conventionally written
      * <strong>3'</strong>-5) upstream of the recognition site at
      * which the cut occurs. The cut occurs between this base and the
      * following one.
      * @param dsForward an <code>int</code> index in the forward
      * strand (the strand conventionally written
      * <strong>5'</strong>-3') downstream of the recognition site at
      * which the cut occurs. The cut occurs between this base and the
      * following one.
      * @param dsReverse an <code>int</code> index in the reverse
      * strand (the strand conventionally written
      * <strong>3'</strong>-5') downstream of the recognition site at
      * which the cut occurs. The cut occurs between this base and the
      * following one.
      *
      * @exception IllegalAlphabetException if an error occurs.
      */
     public RestrictionEnzyme(String name, SymbolList site,
                              int usForward, int usReverse,
                              int dsForward, int dsReverse)
         throws IllegalAlphabetException
     {
         this(name, site,
              new int [] { usForward, usReverse },
              new int [] { dsForward, dsReverse });
     }
 
     /**
      * Creates a new <code>RestrictionEnzyme</code>.
      *
      * @param name a <code>String</code> name.
      * @param site a <code>SymbolList</code> site.
      * @param usCutPositions an <code>int []</code> array of optional
      * upstream indices.
      * @param dsCutPositions an <code>int []</code> array of
      * downstream indices.
      *
      * @exception IllegalAlphabetException if an error occurs.
      */
     private RestrictionEnzyme(String name, SymbolList site,
                               int [] usCutPositions,
                               int [] dsCutPositions)
         throws IllegalAlphabetException
     {
         if (site.getAlphabet() != DNATools.getDNA())
             throw new IllegalAlphabetException("RestrictionEnzyme site can only be a DNA SymbolList."
                                                + " A SymbolList using the "
                                                + site.getAlphabet().getName()
                                                + " was supplied" );
         this.name = name;
         this.site = site;
         this.usCutPositions = usCutPositions;
         this.dsCutPositions = dsCutPositions;
 
         forwardRegex = MotifTools.createRegex(site);
 
         try
         {
             reverseRegex =
                 MotifTools.createRegex(DNATools.reverseComplement(site));
         }
         catch (IllegalAlphabetException iae)
         {
             throw new BioError(iae, "RestrictionEnzyme site was not composed of a complementable Alphabet");
         }
 
         StringBuffer sb = new StringBuffer();
         sb.append(name);
 
         if (usCutPositions != null)
         {
             sb.append(" (");
             sb.append(usCutPositions[0]);
             sb.append("/");
             sb.append(usCutPositions[1]);
             sb.append(") ");
         }
 
         try
         {
             for (int i = 1; i <= site.length(); i++)
                 sb.append(DNATools.dnaToken(site.symbolAt(i)));
         }
         catch (IllegalSymbolException ise)
         {
             throw new BioError(ise, "RestrictionEnzyme site contained non-DNA Symbol");
         }
 
         sb.append(" (");
         sb.append(dsCutPositions[0]);
         sb.append("/");
         sb.append(dsCutPositions[1]);
         sb.append(")");
 
         summary = sb.substring(0);
     }
     
     /**
      * <code>getName</code> returns the enzyme name.
      *
      * @return a <code>String</code>.
      */
     public String getName()
     {
         return name;
     }
 
     /**
      * <code>getRecognitionSite</code> returns the forward strand of
      * the recognition site.
      *
      * @return a <code>SymbolList</code>.
      */
     public SymbolList getRecognitionSite()
     {
         return site;
     }
 
     /**
      * <code>getForwardRegex</code> returns a regular expression which
      * matches the forward strand of the recognition site.
      *
      * @return a <code>String</code>.
      */
     public String getForwardRegex()
     {
         return forwardRegex;
     }
 
     /**
      * <code>getReverseRegex</code> returns a regular expression which
      * matches the reverse strand of the recognition site.
      *
      * @return a <code>String</code>.
      */
     public String getReverseRegex()
     {
         return reverseRegex;
     }
 
     /**
      * <code>getDownstreamCut</code> returns the cut site within or
      * downstream of the recognition site.
      *
      * @return an <code>int []</code> array with the position in the
      * 5'- strand at index 0 and the 3'- strand at index 1.
      */
     public int [] getDownstreamCut()
     {
         return dsCutPositions;
     }
 
     /**
      * <code>getDownstreamCut</code> returns the cut site upstream of
      * the recognition site.
      *
      * @return an <code>int []</code> array with the position in the
      * 5'- strand at index 0 and the 3'- strand at index 1.
      *
      * @exception BioException if the enzyme does not cleave on both
      * sides of its recognition site.
      */
     public int [] getUpstreamCut() throws BioException
     {
         if (usCutPositions == null)
             throw new BioException(name + " does not cut upstream of the recognition site");
 
         return usCutPositions;
     }
 
     /**
      * <code>getDownstreamEndType</code> returns the double-stranded
      * end type produced by the primary (intra-site or downstream)
      * cut.
      *
      * @return an <code>int</code> equal to one of the constant fields
      * OVERHANG_5PRIME, OVERHANG_3PRIME or BLUNT.
      */
     public int getDownstreamEndType()
     {
         if (dsCutPositions[0] > dsCutPositions[1])
             return OVERHANG_5PRIME;
         else if (dsCutPositions[0] < dsCutPositions[1])
             return OVERHANG_3PRIME;
         else
             return BLUNT;
     }
 
     /**
      * <code>getUpstreamEndType</code> returns the double-stranded end
      * type produced by the secondary (upstream) cut.
      *
      * @return an <code>int</code> equal to one of the constant fields
      * OVERHANG_5PRIME, OVERHANG_3PRIME or BLUNT.
      *
      * @exception BioException if the enzyme does not cleave on both
      * sides of its recognition site.
      */
     public int getUpstreamEndType() throws BioException
     {
         if (usCutPositions == null)
             throw new BioException(name + " does not cut upstream of the recognition site");
 
         if (usCutPositions[0] > usCutPositions[1])
             return OVERHANG_5PRIME;
         else if (usCutPositions[0] < usCutPositions[1])
             return OVERHANG_3PRIME;
         else
             return BLUNT;
     }
 
     public String toString()
     {
         return summary;
     }
 }
