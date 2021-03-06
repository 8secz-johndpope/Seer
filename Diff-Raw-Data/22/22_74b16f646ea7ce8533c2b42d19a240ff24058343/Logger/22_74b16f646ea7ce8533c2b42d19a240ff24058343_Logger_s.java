 //
 // Triple Play - utilities for use in PlayN-based games
 // Copyright (c) 2011, Three Rings Design, Inc. - All rights reserved.
 // http://github.com/threerings/tripleplay/blob/master/LICENSE
 
 package tripleplay.util;
 
 /**
  * Provides logging services that are routed to the appropriate logging destination on the client
  * or server. A useful usage pattern is for a library or game to define a shared {@code Log} class
  * like so:
  *
  * <pre>{@code
  * import tripleplay.util.Logger;
  * public class Log {
  *   public static final Logger log = new Logger("libident");
  * }</pre>
  *
  * and then import {@code Log.log} statically into classes so that they can invoke {@code
  * log.info}, etc.
  */
 public class Logger
 {
     /**
      * Wires the logging front-end to the logging back-end. See {@link #setImpl}.
      */
     public interface Impl {
         void debug (String message, Throwable t);
         void info (String message, Throwable t);
         void warning (String message, Throwable t);
     }
 
     /**
      * Configures the logging back-end. This should be called before any code that makes use of the
      * logging services. The default back-end logs to {@code stderr}, which is useful when running
      * in unit tests.
      */
     public static void setImpl (Impl impl) {
         _impl = impl;
     }
 
     /**
      * Formats and returns the supplied key/value arguments as {@code [key=value, key=value, ...]}.
      */
     public static String format (Object... args) {
         return format(new StringBuilder("["), args).append("]").toString();
     }
 
     /**
      * Formats the supplied key/value arguments into the supplied string builder as {@code
      * key=value, key=value, ...}.
      * @return the supplied string builder.
      */
     public static StringBuilder format (StringBuilder into, Object... args) {
         for (int ii = 0, ll = args.length/2; ii < ll; ii++) {
             if (ii > 0) {
                 into.append(", ");
             }
             into.append(args[2*ii]).append("=").append(args[2*ii+1]);
         }
         return into;
     }
 
     /**
      * Creates a logger with the specified ident string.
      */
     public Logger (String ident) {
         _ident = ident;
     }
 
     /**
      * Logs a debug message.
      *
      * @param message the text of the message.
      * @param args a series of zero or more key/value pairs followed by an optional {@link
      * Throwable} cause.
      */
     public void debug (String message, Object... args) {
         log(DEBUG_TARGET, _ident, message, args);
     }
 
     /**
      * Logs an info message.
      *
      * @param message the text of the message.
      * @param args a series of zero or more key/value pairs followed by an optional {@link
      * Throwable} cause.
      */
     public void info (String message, Object... args) {
         log(INFO_TARGET, _ident, message, args);
     }
 
     /**
      * Logs a warning message.
      *
      * @param message the text of the message.
      * @param args a series of zero or more key/value pairs followed by an optional {@link
      * Throwable} cause.
      */
     public void warning (String message, Object... args) {
         log(WARNING_TARGET, _ident, message, args);
     }
 
     protected final String _ident;
 
     protected static void log (Target target, String ident, String message, Object... args) {
         StringBuilder sb = new StringBuilder().append(ident).append(": ").append(message);
         if (args.length > 1) {
             sb.append(" [");
             format(sb, args);
             sb.append("]");
         }
         Object error = (args.length % 2 == 1) ? args[args.length-1] : null;
         target.log(sb.toString(), (Throwable)error);
     }
 
     protected static interface Target {
         void log (String message, Throwable t);
     }
 
     protected static Target DEBUG_TARGET = new Target() {
         public void log (String message, Throwable t) {
             _impl.debug(message, t);
         }
     };
     protected static Target INFO_TARGET = new Target() {
         public void log (String message, Throwable t) {
             _impl.info(message, t);
         }
     };
     protected static Target WARNING_TARGET = new Target() {
         public void log (String message, Throwable t) {
             _impl.warning(message, t);
         }
     };
 
     protected static Impl _impl = new Impl() {
         public void debug (String message, Throwable t) {
             info(message, t);
         }
         public void info (String message, Throwable t) {
             System.out.println(message);
             if (t != null) {
                 t.printStackTrace(System.out);
             }
         }
         public void warning (String message, Throwable t) {
             info(message, t);
         }
     };
 }
