 package org.threadly.test.concurrent;
 
 import java.util.concurrent.locks.LockSupport;
 
 /**
  * {@link TestCondition} in unit test, designed to check a condition
  * for something that is happening in a different thread.  Allowing a 
  * test to efficiently block till the testable action has finished.
  * 
  * @author jent - Mike Jensen
  */
 public abstract class TestCondition {
   private static final int DEFAULT_POLL_INTERVAL = 20;
   private static final int DEFAULT_TIMEOUT = 1000 * 10;
   private static final int SPIN_THRESHOLD = 10;
   
   /**
    * Getter for the conditions current state.
    * 
    * @return condition state, true if ready
    */
   public abstract boolean get();
 
   /**
    * Blocks till condition is true, useful for asynchronism operations, 
    * waiting for them to complete in other threads during unit tests.
    * 
    * This uses a default timeout of 10 seconds, and a poll interval of 20ms
    */
   public void blockTillTrue() {
     blockTillTrue(DEFAULT_TIMEOUT, DEFAULT_POLL_INTERVAL);
   }
 
   /**
    * Blocks till condition is true, useful for asynchronism operations, 
    * waiting for them to complete in other threads during unit tests.
    * 
    * This uses the default poll interval of 20ms
    * 
    * @param timeout time to wait for value to become true
    */
   public void blockTillTrue(int timeout) {
     blockTillTrue(timeout, DEFAULT_POLL_INTERVAL);
   }
   
   /**
    * Blocks till condition is true, useful for asynchronism operations, 
    * waiting for them to complete in other threads during unit tests.
    * 
    * @param timeout time to wait for value to become true
    * @param pollInterval time to sleep between checks
    */
   public void blockTillTrue(int timeout, int pollInterval) {
     long startTime = System.currentTimeMillis();
     boolean lastResult;
     while (! (lastResult = get()) && 
            System.currentTimeMillis() - startTime < timeout) {
       if (pollInterval > SPIN_THRESHOLD) {
        LockSupport.parkNanos(1000000 * pollInterval);
       }
     }
     
     if (! lastResult) {
       throw new TimeoutException("Still false after " + 
                                    (System.currentTimeMillis() - startTime) + "ms");
     }
   }
   
   /**
    * Thrown if condition is still false after a given timeout.
    * 
    * @author jent - Mike Jensen
    */
   public static class TimeoutException extends RuntimeException {
     private static final long serialVersionUID = 7445447193772617274L;
     
     /**
      * Constructor for new TimeoutException.
      */
     public TimeoutException() {
       super();
     }
     
     /**
      * Constructor for new TimeoutException.
      * 
      * @param msg Exception message
      */
     public TimeoutException(String msg) {
       super(msg);
     }
   }
 }
