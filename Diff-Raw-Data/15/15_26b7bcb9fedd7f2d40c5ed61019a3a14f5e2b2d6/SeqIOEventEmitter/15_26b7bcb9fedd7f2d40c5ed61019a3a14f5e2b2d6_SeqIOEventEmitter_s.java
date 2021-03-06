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
 
 package org.biojava.bio.seq.io;
 
 import java.util.ArrayList;
 import java.util.Collection;
 import java.util.Collections;
 import java.util.Comparator;
 import java.util.Iterator;
 import java.util.List;
 
 import org.biojava.bio.Annotation;
 import org.biojava.bio.BioError;
 import org.biojava.bio.seq.Feature;
 import org.biojava.bio.seq.FeatureHolder;
 import org.biojava.bio.seq.Sequence;
 import org.biojava.bio.symbol.IllegalAlphabetException;
 import org.biojava.bio.symbol.Symbol;
 
 /**
  * <code>SeqIOEventEmitter</code> is a utility class which scans a
  * <code>Sequence</code> object and sends events describing its
  * constituent data to a <code>SeqIOListener</code>. The listener
  * should be able to reconstruct the <code>Sequence</code> from these
  * events.
  *
  * @author Keith James
  * @since 1.2
 */
 class SeqIOEventEmitter
 {
     private static Symbol [] symProto = new Symbol [0];
 
     private Comparator seqPropComparator;
     private Comparator featureComparator;
 
     SeqIOEventEmitter(Comparator seqPropComparator,
                       Comparator featureComparator)
     {
         this.seqPropComparator = seqPropComparator;
         this.featureComparator = featureComparator;
     };
 
     /**
      * <code>getSeqIOEvents</code> scans a <code>Sequence</code>
      * object and sends events describing its data to the
      * <code>SeqIOListener</code>.
      *
      * @param seq a <code>Sequence</code>.
      * @param listener a <code>SeqIOListener</code>.
      */
     void getSeqIOEvents(Sequence seq, SeqIOListener listener)
     {
         try
         {
             // Inform listener of sequence start
             listener.startSequence();
 
             // Pass name to listener
             listener.setName(seq.getName());
 
             // Pass URN to listener
             listener.setURI(seq.getURN());
 
             // Pass sequence properties to listener
             Annotation a = seq.getAnnotation();
             List sKeys = new ArrayList(a.keys());
             Collections.sort(sKeys, seqPropComparator);
 
             for (Iterator ki = sKeys.iterator(); ki.hasNext();)
             {
                 Object key = ki.next();
                 listener.addSequenceProperty(key, a.getProperty(key));
             }
 
             // Recurse through sub feature tree, flattening it for
             // EMBL
             List subs = getSubFeatures(seq);
             Collections.sort(subs, featureComparator);
 
             // Put the source features first for EMBL
             for (Iterator fi = subs.iterator(); fi.hasNext();)
             {		
                 // The template is required to call startFeature
                 Feature.Template t = ((Feature) fi.next()).makeTemplate();
 
                 // Inform listener of feature start
                 listener.startFeature(t);
 
                 // Pass feature properties (i.e. qualifiers to
                 // listener)
                List fKeys = new ArrayList(t.annotation.keys());
                 Collections.sort(fKeys);
 
                 for (Iterator ki = fKeys.iterator(); ki.hasNext();)
                 {
                     Object key = ki.next();
                     listener.addFeatureProperty(key, t.annotation.getProperty(key));
                 }
 
                 // Inform listener of feature end
                 listener.endFeature();
             }
 
             // Add symbols
             listener.addSymbols(seq.getAlphabet(),
                                 (Symbol []) seq.toList().toArray(symProto),
                                 0,
                                 seq.length());
 	    
             // Inform listener of sequence end
             listener.endSequence();
         }
         catch (IllegalAlphabetException iae)
         {
             // This should never happen as the alphabet is being used
             // by this Sequence instance
             throw new BioError(iae, "An internal error occurred processing symbols");
         }
         catch (ParseException pe)
         {
             throw new BioError(pe, "An internal error occurred creating SeqIO events");
         }
     }
 
     /**
      * <code>getSubFeatures</code> is a recursive method which returns
      * a list of all <code>Feature</code>s within a
      * <code>FeatureHolder</code>.
      *
      * @param fh a <code>FeatureHolder</code>.
      *
      * @return a <code>List</code>.
      */
     private static List getSubFeatures(FeatureHolder fh)
     {
         List subfeat = new ArrayList();
 
         for (Iterator fi = fh.features(); fi.hasNext();)
         {
             FeatureHolder sfh = (FeatureHolder) fi.next();
 
             subfeat.addAll((Collection) getSubFeatures(sfh));
             subfeat.add(sfh);
         }
         return subfeat;
     }
 }
