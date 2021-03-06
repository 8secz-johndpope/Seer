 package org.jeroen.ddd.specification;
 
 /**
  * Determine if a property value is less than the specified value.
  * 
  * @author Jeroen van Schagen
  * @since 5-1-2011
  *
  * @param <T> type of entity being checked
  */
 public class LessThanSpecification<T> extends CompareSpecification<T> {
     private static final int LESS_THAN_COMPARISON = -1;
 
     /**
      * Construct a new {@link LessThanSpecification}.
     * @param property determines what property should be verified
     * @param value candidates are only matched when their property value is beneat this value
      */
     public LessThanSpecification(String property, Object value) {
         super(property, value, LESS_THAN_COMPARISON);
     }
 
 }
