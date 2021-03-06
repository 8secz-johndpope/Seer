 package net.happygiraffe.jslint;
 
 /**
  * @author dom
  * @version $Id$
  */
 public enum Option {
     // BEGIN-OPTIONS
     /** If use of some browser features should be restricted */
     ADSAFE("If use of some browser features should be restricted"),
     /** If bitwise operators should not be allowed */
     BITWISE("If bitwise operators should not be allowed"),
     /** If the standard browser globals should be predefined */
     BROWSER("If the standard browser globals should be predefined"),
     /** If upper case html should be allowed */
     CAP("If upper case html should be allowed"),
     /** If debugger statements should be allowed */
     DEBUG("If debugger statements should be allowed"),
     /** If === should be required */
     EQEQEQ("If === should be required"),
     /** If eval should be allowed */
     EVIL("If eval should be allowed"),
     /** If html fragments should be allowed */
     FRAGMENT("If html fragments should be allowed"),
     /** If line breaks should not be checked */
     LAXBREAK("If line breaks should not be checked"),
     /** If names should be checked */
     NOMEN("If names should be checked"),
     /** If the scan should stop on first error */
     PASSFAIL("If the scan should stop on first error"),
     /** If increment/decrement should not be allowed */
     PLUSPLUS("If increment/decrement should not be allowed"),
     /** If the rhino environment globals should be predefined */
     RHINO("If the rhino environment globals should be predefined"),
    /** If undefined variables are errors */
    UNDEF("If undefined variables are errors"),
     /** If strict whitespace rules apply */
     WHITE("If strict whitespace rules apply"),
     /** If the yahoo widgets globals should be predefined */
     WIDGET("If the yahoo widgets globals should be predefined");
     // END-OPTIONS
 
     private String description;
 
     Option(String description) {
         this.description = description;
     }
 
     /**
      * Return a description of what this option affects.
      */
     public String getDescription() {
         return description;
     }
 
     /**
      * Return the lowercase name of this option.
      */
     public String getLowerName() {
         return name().toLowerCase();
     }
 
     /**
      * Calculate the maximum length of all of the {@link Option} names.
     * 
      * @return the length of the largest name.
      */
     public static int maximumNameLength() {
         int maxOptLen = 0;
         for (Option o : values()) {
             int len = o.name().length();
             if (len > maxOptLen) {
                 maxOptLen = len;
             }
         }
         return maxOptLen;
     }
 
     /**
      * Show this option and its description.
      */
     @Override
     public String toString() {
         return getLowerName() + "[" + getDescription() + "]";
     }
 }
