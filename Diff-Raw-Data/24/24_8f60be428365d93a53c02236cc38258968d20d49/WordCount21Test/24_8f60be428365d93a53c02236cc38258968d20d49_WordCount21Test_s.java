 package com.hadoopilluminated.ch01;
 
 import com.google.common.io.Files;
 import java.io.File;
 import java.nio.charset.Charset;
 import org.junit.BeforeClass;
 import org.junit.Test;
 
 /**
  *
  * @author mark
  */
 public class WordCount21Test {
 
    static private String outputPath = "test-output/ch1";
 
     @BeforeClass
     public static void setupBase() throws Exception {
         System.out.println("hadoop21.setupBase");
         // Delete output, so that Hadoop can run
         // This is useful for JUnit testing to run repeatedly
         if (new File(outputPath).exists()) {
             Files.deleteRecursively(new File(outputPath));
         }
     }
 
     @Test
     public void testMain() throws Exception {
         System.out.println("hadoop21.testMain");
        String testData = "test-data/ch1/moby-dick.txt";
         String[] args = new String[2];
         args[0] = testData;
         args[1] = outputPath;
         WordCount21.main(args);
         assert (new File(outputPath + "/_SUCCESS").exists());
         assert (Files.readLines(new File(outputPath + "/part-r-00000"),
                 Charset.defaultCharset()).
                 size() == 19545);
     }
 }
