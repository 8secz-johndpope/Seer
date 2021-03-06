 //
// $Id: RunAnywhere.java,v 1.2 2003/05/03 00:06:23 mdb Exp $
 
 package com.samskivert.util;
 
 import java.awt.event.InputEvent;
 
 /**
  * <cite>Write once, run anywhere.</cite> Well, at least that's what it
  * said in the brochures. For those less than fresh days, you might need
  * to use this class to work around bugs on particular operating systems.
  */
 public class RunAnywhere
 {
     /**
      * Returns true if we're running in a JVM that identifies its
      * operating system as Windows.
      */
     public static final boolean isWindows ()
     {
         return _isWindows;
     }
 
     /**
      * Returns true if we're running in a JVM that identifies its
      * operating system as MacOS.
      */
     public static final boolean isMacOS ()
     {
         return _isMacOS;
     }
 
     /**
      * Returns the timestamp associated with the supplied event except on
      * the Macintosh where it returns {@link System#currentTimeMillis}
      * because {@link InputEvent#getWhen} returns completely incorrect
      * values. Hopefully this method will become unnecessary soon.
      */
     public static long getWhen (InputEvent event)
     {
         return isMacOS() ? System.currentTimeMillis() : event.getWhen();
     }
 
     /** Flag indicating that we're on Windows; initialized when this class
      * is first loaded. */
     protected static boolean _isWindows;
 
     /** Flag indicating that we're on MacOS; initialized when this class
      * is first loaded. */
     protected static boolean _isMacOS;
 
     static {
         try {
             String osname = System.getProperty("os.name");
             osname = (osname == null) ? "" : osname;
             _isWindows = (osname.indexOf("Windows") != -1);
             _isMacOS = (osname.indexOf("Mac OS") != -1 ||
                         osname.indexOf("MacOS") != -1);
         } catch (Exception e) {
             // dang, can't grab system properties; we'll just pretend
             // we're not on any of these OSes
         }
     }
 }
