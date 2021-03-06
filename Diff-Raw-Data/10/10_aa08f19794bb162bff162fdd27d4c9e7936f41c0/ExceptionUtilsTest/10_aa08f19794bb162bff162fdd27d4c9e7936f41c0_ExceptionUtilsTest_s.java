 package org.threadly.util;
 
 import static org.junit.Assert.*;
 
 import java.util.StringTokenizer;
 
 import org.junit.Test;
 
 @SuppressWarnings("javadoc")
 public class ExceptionUtilsTest {
   @Test
   public void makeRuntimeWithRuntimeTest() {
     RuntimeException testException = new RuntimeException();
 
     RuntimeException resultException = ExceptionUtils.makeRuntime(testException);
     assertNotNull(resultException);
 
     // we expect the exact same reference to come out
     assertTrue(testException == resultException);
   }
 
   @Test
   public void makeRuntimeWithNonRuntimeTest() {
     Exception testException = new Exception();
 
     RuntimeException resultException = ExceptionUtils.makeRuntime(testException);
     assertNotNull(resultException);
 
     // verify stack trace does not contain Util.makeRuntime inside it for when it created a new exception
     StackTraceElement[] stack = resultException.getStackTrace();
     for (StackTraceElement ste : stack) {
       assertFalse(ste.getClass().getName().equals(ExceptionUtils.class.getName()));
     }
 
     // verify the cause was our original exception
     assertTrue(testException == resultException.getCause());
   }
 
   @Test
   public void makeRuntimeWithNullTest() {
     RuntimeException resultException = ExceptionUtils.makeRuntime(null);
     assertNotNull(resultException);
 
     // verify stack trace does not contain Util.makeRuntime inside it for when it created a new exception
     StackTraceElement[] stack = resultException.getStackTrace();
     for (StackTraceElement ste : stack) {
       assertFalse(ste.getClass().getName().equals(ExceptionUtils.class.getName()));
     }
 
     // verify that no cause is provided
     assertNull(resultException.getCause());
   }
   
   @Test
   public void stackToStringTest() {
     String message = "TestMessage";
     Exception testException = new Exception(message);
     String result = ExceptionUtils.stackToString(testException);
     assertNotNull(result);
     
     StringTokenizer st = new StringTokenizer(result, "\n");
     assertEquals(st.countTokens(), testException.getStackTrace().length + 1);
     assertTrue(result.contains(message));
   }
   
   @Test
   public void stackToStringNullTest() {
     String result = ExceptionUtils.stackToString(null);
     
     assertNotNull(result);
     
     assertEquals(result.length(), 0);
   }
 }
