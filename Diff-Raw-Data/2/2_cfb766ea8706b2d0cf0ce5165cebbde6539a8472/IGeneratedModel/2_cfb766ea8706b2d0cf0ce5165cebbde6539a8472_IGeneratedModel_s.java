 package water.genmodel;
 
 /**
  * A generic interface to access generated models.
  */
 public interface IGeneratedModel {
    /** The names of the columns used in the model (not including empty and response names). */
     public String[] getNames();
 
     /** The name of the response column. */
     public String getResponseName();
 
     /** Returns an index of the response column. */
     public int getResponseIdx();
 
     /** Get number of classes in in given column.
      * Return number greater than zero if the column is categorical
      * or -1 if the column is numeric. */
     public int getNumClasses(int i);
 
     /** Return a number of classes in response column. */
     public int getNumResponseClasses();
 
     /** Predict the given row and return prediction
      *
      * @param data row holding the data. Ordering should follow ordering of columns returned by getNames()
      * @param preds allocated array to hold a prediction
      * @return returned preds parameter
      */
     public float[] predict(double[] data, float[] preds);
 
     /** Gets domain of given column.
      * @param name column name
      * @return return domain for given column or null if column is numeric.
      */
     public String[] getDomainValues(String name);
     /**
      * Returns domain values for i-th column.
      * @param i index of column
      * @return domain for given enum column or null if columns contains numeric value
      */
     public String[] getDomainValues(int i);
 
     /** Returns domain values for all columns */
     public String[][] getDomainValues();
 
     /** Returns index of column with give name or -1 if column is not found. */
     public int getColIdx(String name);
 }
