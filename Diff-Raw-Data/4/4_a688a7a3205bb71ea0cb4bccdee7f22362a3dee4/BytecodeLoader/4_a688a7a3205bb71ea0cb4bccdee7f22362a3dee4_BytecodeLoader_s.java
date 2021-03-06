 // Copyright  Corporation for National Research Initiatives
 
 package org.python.core;
 import java.util.Hashtable;
 
 public class BytecodeLoader extends ClassLoader
 {
     protected Class loadClass(String s, boolean b)
         throws ClassNotFoundException
     {
         ClassLoader classLoader = Py.getSystemState().getClassLoader();
         if (classLoader != null)
             return classLoader.loadClass(s);
         return findSystemClass(s);
     }
 
     public Class loadClass(String name, byte[] data) {
         Class c = defineClass(name, data, 0, data.length);
         resolveClass(c);
         // This method has caused much trouble.  Using it breaks jdk1.2rc1
         // Not using it can make SUN's jdk1.1.6 JIT slightly unhappy.
         // Don't use by default, but allow python.options.compileClass to
         // override
         if (!Options.skipCompile) {
             //System.err.println("compile: "+name);
             Compiler.compileClass(c);
         }
         return c;
     }
 
     public PyCode loadBytes(String name, byte[] data)
         throws InstantiationException, IllegalAccessException
     {
         Class c = loadClass(name, data);
         return ((PyRunnable)c.newInstance()).getMain();
     }
 
    public static Class makeClass(String name, byte[] data) {
        return new BytecodeLoader().loadClass(name, data);
     }
 
     public static PyCode makeCode(String name, byte[] data)
     {
         try {
             return new BytecodeLoader().loadBytes(name, data);
         }
         catch (Exception e) {
             throw Py.JavaError(e);
         }
     }
 }
