 package org.soluvas.commons.inject;
 
 import java.lang.annotation.ElementType;
 import java.lang.annotation.Retention;
 import java.lang.annotation.RetentionPolicy;
 import java.lang.annotation.Target;
 
 /**
  * CDI Qualifier for adding any custom OSGi Blueprint filter.
  * @author ceefour
  */
 @Retention(RetentionPolicy.RUNTIME)
 @Target(ElementType.FIELD)
 public @interface Filter {
 	
 	String value();
 
 }
