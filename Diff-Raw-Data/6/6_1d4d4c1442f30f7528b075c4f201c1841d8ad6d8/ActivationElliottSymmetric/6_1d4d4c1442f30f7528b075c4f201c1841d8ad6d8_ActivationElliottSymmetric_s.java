 package org.encog.engine.network.activation;
 
 /**
  * Computationally efficient alternative to ActivationTANH.
  * Its output is in the range [-1, 1], and it is derivable.
  * 
  * It will approach the -1 and 1 more slowly than Tanh so it 
  * might be more suitable to classification tasks than predictions tasks.
  * 
  * Elliott, D.L. "A better activation function for artificial neural networks", 1993
  * http://citeseerx.ist.psu.edu/viewdoc/download?doi=10.1.1.46.7204&rep=rep1&type=pdf
  */
 public class ActivationElliottSymmetric implements ActivationFunction {
 
     /**
      * The parameters.
      */
     private final double[] params;
 
     /**
      * Construct a basic HTAN activation function, with a slope of 1.
      */
     public ActivationElliottSymmetric() {
         this.params = new double[1];
         this.params[0] = 1.0;
     }
 
     /**
      * {@inheritDoc}
      */
     @Override
     public final void activationFunction(final double[] x, final int start,
             final int size) {
         for (int i = start; i < start + size; i++) {
         	double s = params[0];
         	x[i] = (x[i]*s) / (1 + Math.abs(x[i]*s));        		     			
         }
     }
 
     /**
      * @return The object cloned;
      */
     @Override
     public final ActivationFunction clone() {
        return new ActivationElliott();
     }
 
     /**
      * {@inheritDoc}
      */
     @Override
     public final double derivativeFunction(final double b, final double a) {
     	double s = params[0];
    	return 	s/((1.0+Math.abs(b*s))*(1+Math.abs(b*s)));
     }
 
     /**
      * {@inheritDoc}
      */
     @Override
     public final String[] getParamNames() {
         final String[] result = { "Slope" };
         return result;
     }
 
     /**
      * {@inheritDoc}
      */
     @Override
     public final double[] getParams() {
         return this.params;
     }
 
     /**
      * @return Return true, Elliott activation has a derivative.
      */
     @Override
     public final boolean hasDerivative() {
         return true;
     }
 
     /**
      * {@inheritDoc}
      */
     @Override
     public final void setParam(final int index, final double value) {
         this.params[index] = value;
     }
 
 }
