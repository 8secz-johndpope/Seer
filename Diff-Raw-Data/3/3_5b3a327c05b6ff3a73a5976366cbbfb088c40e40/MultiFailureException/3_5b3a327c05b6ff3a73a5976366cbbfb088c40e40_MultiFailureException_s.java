 //
 // React - a library for functional-reactive-like programming in Java
 // Copyright (c) 2011, Three Rings Design, Inc. - All rights reserved.
 // http://github.com/threerings/react/blob/master/LICENSE
 
 package react;
 
 import java.io.PrintStream;
 import java.util.ArrayList;
 import java.util.List;
 
 /**
  * An exception thrown to communicate multiple listener failures.
  */
 public class MultiFailureException extends RuntimeException
 {
     public Iterable<Throwable> failures () {
         return _failures;
     }
 
     public void addFailure (Throwable t) {
         _failures.add(t);
     }
 
     public void trigger () {
         if (_failures.size() != 1) throw this;
 
         Throwable t = _failures.get(0);
         if (t instanceof RuntimeException) {
             throw (RuntimeException)t;
         } else if (t instanceof Error) {
             throw (Error)t;
         } else {
             throw this;
         }
     }
 
     @Override
     public String getMessage () {
         StringBuilder buf = new StringBuilder();
         for (Throwable failure : _failures) {
             if (buf.length() > 0) buf.append(", ");
             buf.append(failure.getMessage());
         }
         buf.insert(0, _failures.size() + " failures: ");
         return buf.toString();
     }
 
     @Override
     public void printStackTrace (PrintStream s) {
         for (Throwable failure : _failures) {
             failure.printStackTrace(s);
         }
     }
 
     @Override
     public Throwable fillInStackTrace () {
         return this; // no stack trace here
     }
 
    protected final List<Throwable> _failures = new ArrayList<Throwable>();
 }
