 /*
  * To change this template, choose Tools | Templates
  * and open the template in the editor.
  */
 package org.freeeed.main;
 
 import static org.junit.Assert.assertEquals;
 import static org.junit.Assert.fail;
 import org.junit.*;
 
 /**
  *
  * @author mark
  */
 public class MRFreeEedProcessTest {
     
     public MRFreeEedProcessTest() {
     }
 
     @BeforeClass
     public static void setUpClass() throws Exception {
     }
 
     @AfterClass
     public static void tearDownClass() throws Exception {
     }
     
     @Before
     public void setUp() {
     }
     
     @After
     public void tearDown() {
     }
 
 
     /**
      * Test of main method, of class MRFreeEedProcess.
      */
    @Test
     public void testMain() throws Exception {
         System.out.println("main");
         String[] args = new String[2];
         args[0] = "sample_freeeed_hadoop.project";
         args[1] = "/freeeed_output";
         MRFreeEedProcess.main(args);
     }
 }
