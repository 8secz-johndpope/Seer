 ////////////////////////////////////////////////////////////////////////////////
 // Test case file for checkstyle.
 // Created: 2001
 ////////////////////////////////////////////////////////////////////////////////
 package com.puppycrawl.tools.checkstyle;
 
 /**
  * Test case for the "design for inheritance" check.
  * @author Lars Khne
  **/
 public class InputDesignForExtension
 {
     // some methods that are OK
 
     public interface InterfaceOK
     {
         void implicitlyAbstract();
     }
 
     final class ClassOK
     {
         protected void finalThroughClassDef()
         {
             System.out.println("no way to override");
         }
     }
 
     protected void nonFinalButEmpty()
     {
     }
 
     public void nonFinalButEmpty2()
     {
         // comments don't count as content...
     }
 
     private void aPrivateMethod()
     {
         System.out.println("no way to override");
     }
 
     protected abstract void nonFinalButAbstract();
 
     // this one is bad: neither abtract, final, or empty
 
     protected void doh()
     {
         System.out.println("nonempty and overriding possible");
     }
 
 }
