 // Copyright  Corporation for National Research Initiatives
 package org.python.core;
 
 public class PyMethod extends PyObject {
     public PyObject im_self;
     public PyObject im_func;
     public PyObject im_class;
     public String __name__;
     public PyObject __doc__;
 
     public static PyClass __class__;
 
     public PyMethod(PyObject self, PyObject f, PyObject wherefound) {
         super(__class__);
         im_func = f;
         im_self = self;
         im_class = wherefound;
     }
         
     public PyMethod(PyObject self, PyFunction f, PyObject wherefound) {
         this(self, (PyObject)f, wherefound);
         __name__ = f.__name__;
         __doc__ = f.__doc__;
     }
         
     public PyMethod(PyObject self, PyReflectedFunction f, PyObject wherefound)
     {
         this(self, (PyObject)f, wherefound);
         __name__ = f.__name__;
         __doc__ = f.__doc__;
     }
 
     /*private final boolean isBound() {
       return im_self != null && !(im_self instanceof PyClass);
       }*/
         
     public PyObject __call__(PyObject[] args, String[] keywords) {
         if (im_self != null)
             // bound method
             return im_func.__call__(im_self, args, keywords);
         // unbound method.
         boolean badcall = false;
        if (args.length < 1)
            badcall = true;
        else if (im_class == null)
            // An example of this is running any function defined in the os
            // module.  If import os, you'll find it's a jclass object
            // instead of a module object.  That's wrong, but not easily
            // fixed right now.  Running os.makedirs('somedir') creates an
            // unbound method with im_class == null.  For backwards
            // compatibility, let this pass the call test
             ;
         else
             // first argument must be an instance who's class is im_class
             // or a subclass of im_class
             badcall = ! __builtin__.issubclass(args[0].__class__,
                                                (PyClass)im_class);
         if (badcall) {
             throw Py.TypeError(
              "unbound method must be called with class instance 1st argument");
         }
         else
             return im_func.__call__(args, keywords);
     }
 
     public int __cmp__(PyObject other) {
         if (other instanceof PyMethod) {
             PyMethod mother = (PyMethod)other;
             if (im_self != mother.im_self)
                 return Py.id(im_self) < Py.id(mother.im_self) ? -1 : 1;
             if (im_func != mother.im_func)
                 return Py.id(im_func) < Py.id(mother.im_func) ? -1 : 1;
             return 0;
         }
         return -2;
     }
 
     public String toString() {
         String classname = "?";
         if (im_class != null && im_class instanceof PyClass)
             classname = ((PyClass)im_class).__name__;
         if (im_self == null)
             // this is an unbound method
             return "<unbound method " + classname + "." + __name__ + ">";
         else
             return "<method " + classname + "." +
                 __name__ + " of " + im_self.__class__.__name__ +
                 " instance at " + Py.id(im_self) + ">";
     }
 }
