 /*
  * Copyright (c) 2003 Sun Microsystems, Inc. All Rights Reserved.
  * 
  * Redistribution and use in source and binary forms, with or without
  * modification, are permitted provided that the following conditions are
  * met:
  * 
  * - Redistribution of source code must retain the above copyright
  *   notice, this list of conditions and the following disclaimer.
  * 
  * - Redistribution in binary form must reproduce the above copyright
  *   notice, this list of conditions and the following disclaimer in the
  *   documentation and/or other materials provided with the distribution.
  * 
  * Neither the name of Sun Microsystems, Inc. or the names of
  * contributors may be used to endorse or promote products derived from
  * this software without specific prior written permission.
  * 
  * This software is provided "AS IS," without a warranty of any kind. ALL
  * EXPRESS OR IMPLIED CONDITIONS, REPRESENTATIONS AND WARRANTIES,
  * INCLUDING ANY IMPLIED WARRANTY OF MERCHANTABILITY, FITNESS FOR A
  * PARTICULAR PURPOSE OR NON-INFRINGEMENT, ARE HEREBY EXCLUDED. SUN
  * MICROSYSTEMS, INC. ("SUN") AND ITS LICENSORS SHALL NOT BE LIABLE FOR
  * ANY DAMAGES SUFFERED BY LICENSEE AS A RESULT OF USING, MODIFYING OR
  * DISTRIBUTING THIS SOFTWARE OR ITS DERIVATIVES. IN NO EVENT WILL SUN OR
  * ITS LICENSORS BE LIABLE FOR ANY LOST REVENUE, PROFIT OR DATA, OR FOR
  * DIRECT, INDIRECT, SPECIAL, CONSEQUENTIAL, INCIDENTAL OR PUNITIVE
  * DAMAGES, HOWEVER CAUSED AND REGARDLESS OF THE THEORY OF LIABILITY,
  * ARISING OUT OF THE USE OF OR INABILITY TO USE THIS SOFTWARE, EVEN IF
  * SUN HAS BEEN ADVISED OF THE POSSIBILITY OF SUCH DAMAGES.
  * 
  * You acknowledge that this software is not designed or intended for use
  * in the design, construction, operation or maintenance of any nuclear
  * facility.
  */
 
 package com.jogamp.nativewindow.impl.jawt;
 
 import com.jogamp.nativewindow.impl.*;
 import java.awt.EventQueue;
 import java.awt.Frame;
 
 import javax.media.nativewindow.*;
 
 
 import java.awt.GraphicsEnvironment;
 import java.awt.Toolkit;
 import java.lang.reflect.*;
 import java.security.*;
 import java.util.ArrayList;
 import java.util.Map;
 
 public class JAWTUtil {
   protected static final boolean DEBUG = Debug.debug("JAWT");
 
   // See whether we're running in headless mode
   private static final boolean headlessMode;
 
   // Java2D magic ..
   private static final Method isQueueFlusherThread;
   private static final boolean j2dExist;
 
   private static Class   sunToolkitClass;
   private static Method  sunToolkitAWTLockMethod;
   private static Method  sunToolkitAWTUnlockMethod;
   private static boolean hasSunToolkitAWTLock;
 
   private static final JAWTToolkitLock jawtToolkitLock;
 
   static {
     JAWTJNILibLoader.loadAWTImpl();
     JAWTJNILibLoader.loadNativeWindow("awt");
 
     headlessMode = GraphicsEnvironment.isHeadless();
 
     boolean ok = false;
     Class jC = null;
     Method m = null;
     if (!headlessMode) {
         try {
             jC = Class.forName("com.jogamp.opengl.impl.awt.Java2D");
             m = jC.getMethod("isQueueFlusherThread", null);
             ok = true;
         } catch (Exception e) {
         }
     }
     isQueueFlusherThread = m;
     j2dExist = ok;
 
     AccessController.doPrivileged(new PrivilegedAction() {
         public Object run() {
             try {
                 sunToolkitClass = Class.forName("sun.awt.SunToolkit");
                 sunToolkitAWTLockMethod = sunToolkitClass.getDeclaredMethod("awtLock", new Class[]{});
                 sunToolkitAWTLockMethod.setAccessible(true);
                 sunToolkitAWTUnlockMethod = sunToolkitClass.getDeclaredMethod("awtUnlock", new Class[]{});
                 sunToolkitAWTUnlockMethod.setAccessible(true);
             } catch (Exception e) {
                 // Either not a Sun JDK or the interfaces have changed since 1.4.2 / 1.5
             }
             return null;
         }
     });
     boolean _hasSunToolkitAWTLock = false;
     if (null != sunToolkitAWTLockMethod && null != sunToolkitAWTUnlockMethod) {
         try {
             sunToolkitAWTLockMethod.invoke(null, null);
             sunToolkitAWTUnlockMethod.invoke(null, null);
             _hasSunToolkitAWTLock = true;
         } catch (Exception e) {
         }
     }
     hasSunToolkitAWTLock = _hasSunToolkitAWTLock;
     // hasSunToolkitAWTLock = false;
 
     jawtToolkitLock = new JAWTToolkitLock();
 
    // trigger native AWT toolkit / properties initialization
    Map desktophints = null;
     try {
        if(EventQueue.isDispatchThread()) {
            desktophints = (Map)(Toolkit.getDefaultToolkit().getDesktopProperty("awt.font.desktophints"));
        } else {
            final ArrayList desktophintsBucket = new ArrayList();
            EventQueue.invokeAndWait(new Runnable() {
                public void run() {
                    Map _desktophints = (Map)(Toolkit.getDefaultToolkit().getDesktopProperty("awt.font.desktophints"));
                    if(null!=_desktophints) {
                        desktophintsBucket.add(_desktophints);
                    }
                 }
            });
            desktophints = ( desktophintsBucket.size() > 0 ) ? (Map)desktophintsBucket.get(0) : null ;
        }
     } catch (InterruptedException ex) {
         ex.printStackTrace();
     } catch (InvocationTargetException ex) {
         ex.printStackTrace();
     }
 
     if (DEBUG) {
         System.err.println("JAWTUtil: Has sun.awt.SunToolkit.awtLock/awtUnlock " + hasSunToolkitAWTLock);
         System.err.println("JAWTUtil: Has Java2D " + j2dExist);
         System.err.println("JAWTUtil: Is headless " + headlessMode);
        int hints = ( null != desktophints ) ? desktophints.size() : 0 ;
         System.err.println("JAWTUtil: AWT Desktop hints " + hints);
     }
   }
 
   public static void initSingleton() {
       // just exist to ensure static init has been run
   }
 
 
   public static final boolean hasJava2D() {
     return j2dExist;
   }
 
   public static final boolean isJava2DQueueFlusherThread() {
     boolean b = false;
     if(j2dExist) {
         try {
             b = ((Boolean)isQueueFlusherThread.invoke(null, null)).booleanValue();
         } catch (Exception e) {}
     }
     return b;
   }
 
   public static boolean isHeadlessMode() {
     return headlessMode;
   }
 
   /**
    * Locks the AWT's global ReentrantLock.<br>
    *
    * JAWT's native Lock() function calls SunToolkit.awtLock(),
    * which just uses AWT's global ReentrantLock.<br>
    */
   public static void awtLock() {
     if(hasSunToolkitAWTLock) {
         try {
             sunToolkitAWTLockMethod.invoke(null, null);
         } catch (Exception e) {
           throw new NativeWindowException("SunToolkit.awtLock failed", e);
         }
     } else {
         JAWT.getJAWT().Lock();
     }
   }
 
   /**
    * Unlocks the AWT's global ReentrantLock.<br>
    *
    * JAWT's native Unlock() function calls SunToolkit.awtUnlock(),
    * which just uses AWT's global ReentrantLock.<br>
    */
   public static void awtUnlock() {
     if(hasSunToolkitAWTLock) {
         try {
             sunToolkitAWTUnlockMethod.invoke(null, null);
         } catch (Exception e) {
           throw new NativeWindowException("SunToolkit.awtUnlock failed", e);
         }
     } else {
         JAWT.getJAWT().Unlock();
     }
   }
 
   public static void lockToolkit() throws NativeWindowException {
     if(!headlessMode && !isJava2DQueueFlusherThread()) {
         awtLock();
     }
   }
 
   public static void unlockToolkit() {
     if(!headlessMode && !isJava2DQueueFlusherThread()) {
         awtUnlock();
     }
   }
 
   public static JAWTToolkitLock getJAWTToolkitLock() {
     return jawtToolkitLock;
   }
 }
 
