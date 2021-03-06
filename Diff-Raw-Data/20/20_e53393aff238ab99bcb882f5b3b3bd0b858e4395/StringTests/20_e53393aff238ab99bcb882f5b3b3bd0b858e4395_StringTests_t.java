 /*******************************************************************************
  * Copyright (c) 2012 Secure Software Engineering Group at EC SPRIDE.
  * All rights reserved. This program and the accompanying materials
  * are made available under the terms of the GNU Lesser Public License v2.1
  * which accompanies this distribution, and is available at
  * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
  * 
  * Contributors: Christian Fritz, Steven Arzt, Siegfried Rasthofer, Eric
  * Bodden, and others.
  ******************************************************************************/
 package soot.jimple.infoflow.test.junit;
 
 import static org.junit.Assert.assertNotNull;
 import static org.junit.Assert.assertTrue;
 
 import java.util.ArrayList;
 import java.util.Collections;
 import java.util.LinkedList;
 import java.util.List;
 
 import org.junit.Before;
 import org.junit.Test;
 
 import soot.jimple.infoflow.Infoflow;
 import soot.jimple.infoflow.config.IInfoflowConfig;
 import soot.options.Options;
 /**
  * covers taint propagation for Strings, String functions such as concat, toUpperCase() or substring, but also StringBuilder and concatenation via '+' operato
  * @author Christian
  *
  */
 public class StringTests extends JUnitTests {
 	
 	@Before
 	public void init() {
     	taintWrapper = false;
 	}
 	
 	@Test
     public void multipleSourcesTest(){
     	Infoflow infoflow = initInfoflow();
     	List<String> epoints = new ArrayList<String>();
     	epoints.add("<soot.jimple.infoflow.test.StringTestCode: void multipleSources()>");
 		infoflow.computeInfoflow(path, epoints,sources, sinks);
 		checkInfoflow(infoflow, 1);
     }
 	
 	@Test
     public void substringTest(){
     	Infoflow infoflow = initInfoflow();
     	List<String> epoints = new ArrayList<String>();
     	epoints.add("<soot.jimple.infoflow.test.StringTestCode: void methodSubstring()>");
 		infoflow.computeInfoflow(path, epoints,sources, sinks);
 		checkInfoflow(infoflow, 1);
     }
 	
 	@Test
     public void lowerCaseTest(){
     	Infoflow infoflow = initInfoflow();
     	List<String> epoints = new ArrayList<String>();
     	epoints.add("<soot.jimple.infoflow.test.StringTestCode: void methodStringLowerCase()>");
 		infoflow.computeInfoflow(path, epoints,sources, sinks);
 		checkInfoflow(infoflow, 1);
     }
 	
 	@Test
     public void upperCaseTest(){
     	Infoflow infoflow = initInfoflow();
     	List<String> epoints = new ArrayList<String>();
     	epoints.add("<soot.jimple.infoflow.test.StringTestCode: void methodStringUpperCase()>");
 		infoflow.computeInfoflow(path, epoints,sources, sinks);
 		checkInfoflow(infoflow, 1);
     }
 
     @Test
     public void concatTest1(){
     	Infoflow infoflow = initInfoflow();
     	List<String> epoints = new ArrayList<String>();
     	epoints.add("<soot.jimple.infoflow.test.StringTestCode: void methodStringConcat1()>");
 		infoflow.computeInfoflow(path, epoints,sources, sinks);
 		checkInfoflow(infoflow, 1);
     }
     
     @Test
     public void concatTest1b(){
     	Infoflow infoflow = initInfoflow();
     	List<String> epoints = new ArrayList<String>();
     	epoints.add("<soot.jimple.infoflow.test.StringTestCode: void methodStringConcat1b()>");
 		infoflow.computeInfoflow(path, epoints,sources, sinks);
 		checkInfoflow(infoflow, 1);
     }
     
     @Test
     public void concatTest1c(){
     	Infoflow infoflow = initInfoflow();
     	List<String> epoints = new ArrayList<String>();
     	epoints.add("<soot.jimple.infoflow.test.StringTestCode: void methodStringConcat1c(java.lang.String)>");
 		infoflow.computeInfoflow(path, epoints,sources, sinks);
 		checkInfoflow(infoflow, 1);
     }
     
     @Test
     public void concatTest2(){
     	Infoflow infoflow = initInfoflow();
     	List<String> epoints = new ArrayList<String>();
     	epoints.add("<soot.jimple.infoflow.test.StringTestCode: void methodStringConcat2()>");
 		infoflow.computeInfoflow(path, epoints,sources, sinks);
 		checkInfoflow(infoflow, 1);
     }
     
     @Test
     public void stringConcatTest(){
 	   Infoflow infoflow = initInfoflow();
     	List<String> epoints = new ArrayList<String>();
     	epoints.add("<soot.jimple.infoflow.test.StringTestCode: void stringConcatTest()>");
 		infoflow.computeInfoflow(path, epoints,sources, sinks);
 		checkInfoflow(infoflow, 1);
     }
    
    @Test
    public void stringConcatTestSmall1(){
 	   Infoflow infoflow = initInfoflow();
    		List<String> epoints = new ArrayList<String>();
    		epoints.add("<soot.jimple.infoflow.test.StringTestCode: void stringConcatTestSmall1()>");
 		infoflow.computeInfoflow(path, epoints,sources, sinks);
 		checkInfoflow(infoflow, 1);
    }
    
    @Test
    public void stringConcatTestSmall2(){
 	   Infoflow infoflow = initInfoflow();
    		List<String> epoints = new ArrayList<String>();
    		epoints.add("<soot.jimple.infoflow.test.StringTestCode: void stringConcatTestSmall2()>");
 		infoflow.computeInfoflow(path, epoints,sources, sinks);
 		checkInfoflow(infoflow, 1);
    }
     
     @Test
     public void concatTestNegative(){
     	Infoflow infoflow = initInfoflow();
     	List<String> epoints = new ArrayList<String>();
     	epoints.add("<soot.jimple.infoflow.test.StringTestCode: void methodStringConcatNegative()>");
 		infoflow.computeInfoflow(path, epoints,sources, sinks);
 		negativeCheckInfoflow(infoflow);
     }
     
     @Test
     public void concatPlusTest1(){
     	Infoflow infoflow = initInfoflow();
     	List<String> epoints = new ArrayList<String>();
     	epoints.add("<soot.jimple.infoflow.test.StringTestCode: void methodStringConcatPlus1()>");
 		infoflow.computeInfoflow(path, epoints,sources, sinks);
 		checkInfoflow(infoflow, 1);
     }
     
     @Test
     public void concatPlusTest2(){
     	Infoflow infoflow = initInfoflow();
     	List<String> epoints = new ArrayList<String>();
     	epoints.add("<soot.jimple.infoflow.test.StringTestCode: void methodStringConcatPlus2()>");
 		infoflow.computeInfoflow(path, epoints,sources, sinks);
 		checkInfoflow(infoflow, 1);	
     }
     
     @Test
     public void valueOfTest1(){
     	Infoflow infoflow = initInfoflow();
     	List<String> epoints = new ArrayList<String>();
     	epoints.add("<soot.jimple.infoflow.test.StringTestCode: void methodValueOf()>");
 		infoflow.computeInfoflow(path, epoints,sources, sinks);
 		checkInfoflow(infoflow, 1);
     }
     
     @Test
     public void toStringTest1(){
     	Infoflow infoflow = initInfoflow();
     	List<String> epoints = new ArrayList<String>();
     	epoints.add("<soot.jimple.infoflow.test.StringTestCode: void methodtoString()>");
 		infoflow.computeInfoflow(path, epoints,sources, sinks);
 		checkInfoflow(infoflow, 1);
     }
     
     @Test
     public void stringBufferTest1(){
     	Infoflow infoflow = initInfoflow();
     	List<String> epoints = new ArrayList<String>();
     	epoints.add("<soot.jimple.infoflow.test.StringTestCode: void methodStringBuffer1()>");
 		infoflow.computeInfoflow(path, epoints,sources, sinks);
 		checkInfoflow(infoflow, 1);
     }
     
     @Test
     public void stringBufferTest2(){
     	Infoflow infoflow = initInfoflow();
     	List<String> epoints = new ArrayList<String>();
     	epoints.add("<soot.jimple.infoflow.test.StringTestCode: void methodStringBuffer2()>");
 		infoflow.computeInfoflow(path, epoints,sources, sinks);
 		checkInfoflow(infoflow, 1);
    }
     
   
     @Test
     public void stringBuilderTest1(){
     	Infoflow infoflow = initInfoflow();
     	List<String> epoints = new ArrayList<String>();
     	epoints.add("<soot.jimple.infoflow.test.StringTestCode: void methodStringBuilder1()>");
 		infoflow.computeInfoflow(path, epoints,sources, sinks);
 		checkInfoflow(infoflow, 1);
     }
     
     @Test
     public void stringBuilderTest2(){
     	Infoflow infoflow = initInfoflow();
     	List<String> epoints = new ArrayList<String>();
     	epoints.add("<soot.jimple.infoflow.test.StringTestCode: void methodStringBuilder2()>");
 		infoflow.computeInfoflow(path, epoints,sources, sinks);
 		checkInfoflow(infoflow, 1);		
     }
     
     @Test
     public void stringBuilderTest2_NoJDK(){
     	taintWrapper = true;
     	Infoflow infoflow = initInfoflow();
     	infoflow.setSootConfig(new IInfoflowConfig() {
 			
 			@Override
 			public void setSootOptions(Options options) {
				List<String> excludeList = new ArrayList<String>();
				excludeList.add("java.");
				excludeList.add("javax.");
				options.set_exclude(excludeList);
 				options.set_prepend_classpath(false);
 			}
 			
 		});
     	List<String> epoints = new ArrayList<String>();
     	epoints.add("<soot.jimple.infoflow.test.StringTestCode: void methodStringBuilder2()>");
 		infoflow.computeInfoflow(path, epoints,sources, sinks);
 		checkInfoflow(infoflow, 1);		
     }
 
     @Test
     public void stringBuilderTest3(){
     	Infoflow infoflow = initInfoflow();
     	List<String> epoints = new ArrayList<String>();
     	epoints.add("<soot.jimple.infoflow.test.StringTestCode: void methodStringBuilder3()>");
 		infoflow.computeInfoflow(path, epoints,sources, sinks);
 		checkInfoflow(infoflow, 1);	
     }
     
     @Test
     public void stringBuilderTest4(){
     	Infoflow infoflow = initInfoflow();
     	List<String> epoints = new ArrayList<String>();
     	epoints.add("<soot.jimple.infoflow.test.StringTestCode: void methodStringBuilder4(java.lang.String)>");
 		infoflow.computeInfoflow(path, epoints,sources, sinks);
 		checkInfoflow(infoflow, 1);	
     }
     
     @Test
     public void stringBuilderTest5(){
     	Infoflow infoflow = initInfoflow();
     	List<String> epoints = new ArrayList<String>();
     	epoints.add("<soot.jimple.infoflow.test.StringTestCode: void methodStringBuilder5()>");
 		infoflow.computeInfoflow(path, epoints,sources, sinks);
 		checkInfoflow(infoflow, 1);	
     }
 
     @Test
     public void stringBuilderTest6(){
     	taintWrapper = true;	// Implicit flow, does not run without taint wrapper
     	Infoflow infoflow = initInfoflow();
     	List<String> epoints = new ArrayList<String>();
     	epoints.add("<soot.jimple.infoflow.test.StringTestCode: void methodStringBuilder6()>");
 		infoflow.computeInfoflow(path, epoints,sources, sinks);
 		checkInfoflow(infoflow, 1);	
     }
     
     @Test
     public void testcharArray(){
     	Infoflow infoflow = initInfoflow();
     	List<String> epoints = new ArrayList<String>();
     	epoints.add("<soot.jimple.infoflow.test.StringTestCode: void getChars()>");
 		infoflow.computeInfoflow(path, epoints,sources, sinks);
 		checkInfoflow(infoflow, 1);
     }
     
     @Test
     public void testStringConcat(){
     	Infoflow infoflow = initInfoflow();
     	List<String> epoints = new ArrayList<String>();
     	epoints.add("<soot.jimple.infoflow.test.StringTestCode: void methodStringConcat()>");
 		infoflow.computeInfoflow(path, epoints,sources, sinks);
 		checkInfoflow(infoflow, 1);
 		assertTrue(infoflow.getResults().isPathBetweenMethods(sink, sourcePwd));
     }
 
     @Test
     public void testStringConstructor(){
     	Infoflow infoflow = initInfoflow();
     	List<String> epoints = new ArrayList<String>();
     	epoints.add("<soot.jimple.infoflow.test.StringTestCode: void methodStringConstructor()>");
     	
     	List<String> sources = new LinkedList<String>();
     	sources.add("<java.lang.String: void <init>(java.lang.String)>");
     	
     	List<String> sinks = new LinkedList<String>();
     	sinks.add("<java.lang.Runtime: java.lang.Process exec(java.lang.String)>");
 
     	infoflow.computeInfoflow(path, epoints, sources, sinks);
     	assertNotNull(infoflow.getResults());
 		assertTrue(infoflow.getResults().isPathBetweenMethods(sinks.get(0), sources.get(0)));
     }
 
 }
