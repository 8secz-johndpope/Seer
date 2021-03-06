 import java.io.File;
 import java.io.IOException;
 import java.lang.Object;
 import java.lang.reflect.Array;
 import java.util.Date;
 import java.util.concurrent.atomic.AtomicBoolean;
 import java.util.jar.Attributes;
 
 import org.jruby.embed.ScriptingContainer;
 import org.python.util.PythonInterpreter;
 import sandbox.runtime.Account;
 
 /**
  * Basic test suite that allocates heap memory in a bunch of different
  * ways and checks if the allocations are what we expect. Currently
  * doesn't quite do the math properly.
  */
 public class HelloWorld {
     public static String main(){
 
         // start actual accounting
         runningTotal = sandbox.runtime.Account.get().memory.current;
 
         checkIncrement(0);
         a = new int[10];
         checkIncrement(10 * 4);
 
         a = new int[10][10][10];
         checkIncrement(1000 * 4);
 
         java.lang.reflect.Array.newInstance(Object.class, 10);
         checkIncrement(10 * 8); // sizeof(T_REF) is 8 for now.
 
         int[] dims = {1, 2, 3, 4, 5};
         checkIncrement(5 * 4);
 
         java.lang.reflect.Array.newInstance(Object.class, dims);
         checkIncrement(120 * 8); // sizeof(T_REF) is 8 for now.
 
 	File f;
         f=new File("test.txt");
         try{
                 f.createNewFile();
 		System.out.println("Good, Created resource/test.txt");
         }catch(IOException e){
 		System.err.println("IOException: ");
                 System.err.println(e);
         }catch(SecurityException e){
 		System.err.println("Hmm should have been able to write to test.txt, but could not");
 	}
	
         java.util.Date d = new Date();
 
         // this should throw an exception
         try{
             java.util.jar.Attributes e = new Attributes();
         }catch(Error e){}
 
         if (output == null) return "Success! Nothing broke";
         else return output;
     }
 
     static Object a;
     static {
         // Initialize all classes by forcing classloading
         a = new int[0];
         a = new Object();
         a = "" + "a";
         Array.newInstance(Object.class, 0);
     }
 
     static long runningTotal = 0;
     static String output = null;
     /**
      * Helper method which asserts whether the expected number of bytes
      * of memory have been recorded as being allocated since the last time
      * this method was called.
      */
     private static void checkIncrement(int delta){
         long newTotal = Account.get().memory.current;
         long actualDelta = newTotal - runningTotal;
         if (actualDelta != delta) output = "memory incremented by " + actualDelta +" expected " + delta;
         runningTotal = newTotal;
     }
 }
