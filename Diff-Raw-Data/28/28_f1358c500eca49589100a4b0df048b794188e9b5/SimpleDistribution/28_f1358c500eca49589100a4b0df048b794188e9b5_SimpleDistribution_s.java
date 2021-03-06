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
 
 
 
 
 
 package org.biojava.bio.dist;
 
 import java.io.IOException;
 import java.io.ObjectInputStream;
 import java.io.Serializable;
 
 import org.biojava.bio.BioError;
 import org.biojava.bio.symbol.Alphabet;
 import org.biojava.bio.symbol.AlphabetIndex;
 import org.biojava.bio.symbol.AlphabetManager;
 import org.biojava.bio.symbol.AtomicSymbol;
 import org.biojava.bio.symbol.FiniteAlphabet;
 import org.biojava.bio.symbol.IllegalAlphabetException;
 import org.biojava.bio.symbol.IllegalSymbolException;
 import org.biojava.utils.ChangeAdapter;
 import org.biojava.utils.ChangeEvent;
 import org.biojava.utils.ChangeSupport;
 import org.biojava.utils.ChangeVetoException;
 
 /**
  * A simple implementation of a distribution, which works with any finite alphabet.
  *
  * @author Matthew Pocock
  * @author Thomas Down
  * @author Mark Schreiber
  * @since 1.0
 * @serial WARNING serilized versions of this class may not be compatible with later versions of BioJava
  */
 public class SimpleDistribution
 extends AbstractDistribution implements Serializable{
 
   private transient AlphabetIndex indexer;
   private transient double[] weights = null;//because indexer is transient.
   private Distribution nullModel;
   private FiniteAlphabet alpha;
     //Change this value if you change this class substantially
   private static final long serialVersionUID = 899744356;
 
   private void readObject(ObjectInputStream stream)
     throws IOException, ClassNotFoundException{
     stream.defaultReadObject();
     indexer = AlphabetManager.getAlphabetIndex(alpha);
     indexer.addChangeListener(
       new ChangeAdapter(){
         public void preChange(ChangeEvent ce) throws ChangeVetoException{
           if(hasWeights()){
             throw new ChangeVetoException(
               ce,
               "Can't allow the index to change as we have probabilities."
             );
           }
         }
       },AlphabetIndex.INDEX
     );
     weights = new double[alpha.size()];
 
     for (int i = 0; i < weights.length; i++) {
       String s = indexer.symbolForIndex(i).getName();
       if(symbolIndices.get(s)!=null){
         weights[i] = ((Double)symbolIndices.get(s)).doubleValue();
       }else{
         weights[i] = 0.0;
       }
     }
 
     //mark for garbage collection
     symbolIndices = null;
   }
 
   public Alphabet getAlphabet() {
     return indexer.getAlphabet();
   }
 
   public Distribution getNullModel() {
     return this.nullModel;
   }
 
 
 
   protected void setNullModelImpl(Distribution nullModel)
 
   throws IllegalAlphabetException, ChangeVetoException {
     this.nullModel = nullModel;
   }
 
 
   /**
   * Indicate wether the weights array has been allocated yet.
    *
    * @return  true if the weights are allocated
    */
   protected boolean hasWeights() {
     return weights != null;
   }
 
 
   /**
    * Get the underlying array that stores the weights.
    *
    * <p>
    * Modifying this will modify the state of the distribution.
    * </p>
    *
    * @return  the weights array
    */
   protected double[] getWeights() {
     if(weights == null) {
       weights = new double[((FiniteAlphabet)getAlphabet()).size()];
       for(int i = 0; i < weights.length; i++) {
         weights[i] = Double.NaN;
 
       }
     }
      return weights;
   }
 
 
 
   public double getWeightImpl(AtomicSymbol s)
 
   throws IllegalSymbolException {
     if(!hasWeights()) {
       return Double.NaN;
     } else {
       int index = indexer.indexForSymbol(s);
       return weights[index];
     }
   }
 
 
   protected void setWeightImpl(AtomicSymbol s, double w)
   throws IllegalSymbolException, ChangeVetoException {
     double[] weights = getWeights();
     if(w < 0.0) {
       throw new IllegalArgumentException(
         "Can't set weight to negative score: " +
         s.getName() + " -> " + w
       );
     }
     weights[indexer.indexForSymbol(s)] = w;
   }
 
   public SimpleDistribution(FiniteAlphabet alphabet) {
     this.alpha = alphabet;
     this.indexer = AlphabetManager.getAlphabetIndex(alphabet);
     indexer.addChangeListener(
       new ChangeAdapter() {
         public void preChange(ChangeEvent ce) throws ChangeVetoException {
           if(hasWeights()) {
             throw new ChangeVetoException(
               ce,
               "Can't allow the index to change as we have probabilities."
             );
           }
         }
       },
       AlphabetIndex.INDEX
     );
 
     try {
       setNullModel(new UniformDistribution(alphabet));
     } catch (Exception e) {
       throw new BioError("This should never fail. Something is screwed!", e);
     }
   }
 
   public void registerWithTrainer(DistributionTrainerContext dtc) {
    dtc.registerTrainer(this, new Trainer());
   }
 
 
   /**
    * A simple implementation of a trainer for this class.
    *
    * @author Matthew Pocock
    * @since 1.0
    */
   protected class Trainer implements DistributionTrainer {
     private final Count counts;
 
     /**
      * Create a new trainer.
      */
     public Trainer() {
       counts = new IndexedCount(indexer);
     }
 
     public void addCount(DistributionTrainerContext dtc, AtomicSymbol sym, double times)
     throws IllegalSymbolException {
       try {
           counts.increaseCount(sym, times);
       } catch (ChangeVetoException cve) {
         throw new BioError(
           "Assertion Failure: Change to Count object vetoed", cve
         );
       }
     }
 
     public double getCount(DistributionTrainerContext dtc, AtomicSymbol sym)
     throws IllegalSymbolException {
       return counts.getCount(sym);
     }
 
 
 
     public void clearCounts(DistributionTrainerContext dtc) {
       try {
         int size = ((FiniteAlphabet) counts.getAlphabet()).size();
         for(int i = 0; i < size; i++) {
           counts.zeroCounts();
         }
       } catch (ChangeVetoException cve) {
         throw new BioError(
           "Assertion Failure: Change to Count object vetoed",cve
         );
       }
     }
 
 
 
     public void train(DistributionTrainerContext dtc, double weight)
     throws ChangeVetoException {
       if(!hasListeners())  {
         trainImpl(dtc, weight);
       } else {
         ChangeSupport changeSupport = getChangeSupport(Distribution.WEIGHTS);
         synchronized(changeSupport) {
           ChangeEvent ce = new ChangeEvent(
             SimpleDistribution.this,
             Distribution.WEIGHTS
           );
           changeSupport.firePreChangeEvent(ce);
           trainImpl(dtc, weight);
           changeSupport.firePostChangeEvent(ce);
         }
       }
     }
 
 
 
     protected void trainImpl(DistributionTrainerContext dtc, double weight) {
       //System.out.println("Training");
       try {
         Distribution nullModel = getNullModel();
         double[] weights = getWeights();
         double[] total = new double[weights.length];
         double sum = 0.0;
 
         for(int i = 0; i < total.length; i++) {
           AtomicSymbol s = (AtomicSymbol) indexer.symbolForIndex(i);
           sum +=
             total[i] =
               getCount(dtc, s) +
               nullModel.getWeight(s) * weight;
         }
         double sum_inv = 1.0 / sum;
         for(int i = 0; i < total.length; i++) {
           //System.out.println("\t" + weights[i] + "\t" + total[i] * sum_inv);
           weights[i] = total[i] * sum_inv;
         }
       } catch (IllegalSymbolException ise) {
         throw new BioError(
           "Assertion Failure: Should be impossible to mess up the symbols.",ise
         );
       }
     }
   }
 }
 
 
 
