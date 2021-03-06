 package nuroko.module;
 
 import mikera.vectorz.AVector;
 import mikera.vectorz.Vectorz;
 import nuroko.core.IInputState;
 import nuroko.core.IParameterised;
 import nuroko.core.IThinker;
 import static org.junit.Assert.*;
 
 public class GenericModuleTests {
 
 	private static void testFill(IParameterised p) {
 		// test that parameters and gradient can both be filled
 		p=p.clone();
 		
 		AVector param=p.getParameters();
 		AVector grad=p.getGradient();
 		
 		param.fill(1);
 		grad.fill(1);
 		assertTrue(param.epsilonEquals(grad));
 		
 		// extra logic should pick up any duplicated / repeated element references
 		for (int i=0; i<param.length(); i++) {
 			assertEquals(1.0, param.get(i),0.0);
 			param.set(i,2.0);
 		}
 		grad.add(grad);
 		assertTrue(param.epsilonEquals(grad));	
 	}
 	
 	private static void testCloneNotLinked(IParameterised p) {
 		// Test that a clone does not share any elements of the parameter or gardient vectors
 		if(p.getParameterLength()==0) return;
 		
 		p=p.clone();
 		
 		AVector param=p.getParameters();
 		AVector grad=p.getGradient();
 		param.fill(1.0);
 		grad.fill(1.0);	
 		
 		IParameterised p2=p.clone();
 		p2.getParameters().fill(2);
 		p2.getGradient().fill(2);
 		
 		assertEquals(1.0,Vectorz.maxValue(param),0.0);
 		assertEquals(1.0,Vectorz.maxValue(grad),0.0);
 	}
 	
 	private static void testCloneCopyParameters(IParameterised p) {
 		if (p instanceof IThinker) {
 			// Test that a clone does not share any elements of the parameter or gardient vectors
 			p=p.clone();
 			
 			AVector param=p.getParameters();
 			Vectorz.fillGaussian(param);
 			IParameterised p2=p.clone();
 			p2.getParameters().fill(2);
 			p2.getParameters().set(param);
 			
 			AVector input=Vectorz.newVector(((IThinker)p).getInputLength());
 			AVector output=Vectorz.newVector(((IThinker)p).getOutputLength());
 			AVector output2=Vectorz.newVector(((IThinker)p).getOutputLength());
 			
 			Vectorz.fillRandom(input);
 			((IThinker)p).think(input, output);
 			((IThinker)p2).think(input, output2);
 			assertTrue(output.epsilonEquals(output2));
 		}
 	}
 	
 	private static void testParameterized(IParameterised p) {
 		p=p.clone();
 		testFill(p);
 		testCloneNotLinked(p);
 		testCloneCopyParameters(p);
 	}
 	
 	private static void testOverwriteOutput(IThinker p) {
 		// test that output vector is completely overwritten by any IThinker
 		p=p.clone();
 		AVector input=Vectorz.newVector(p.getInputLength());
 		AVector output=Vectorz.newVector(p.getOutputLength());
 		
 		output.fill(Double.NaN);
 		p.think(input, output);
 		for (int i=0; i<output.length(); i++) {
 			assertTrue(output.get(i)!=Double.NaN);
 		}
 	}
 	
 	private static void testThinker(IThinker p) {
 		p=p.clone();
 		testOverwriteOutput(p);
 	}
 	
 
 	private static void testInput(IInputState o) {
 		AVector input=o.getInput();
 		assertEquals(o.getInputLength(),input.length());
 		assertEquals(input.length(),o.getInputGradient().length());
 	}
 	
 	public static void test(Object o) {
 		if (o instanceof IParameterised) {
 			testParameterized((IParameterised)o);
 		}
 		if (o instanceof IInputState) {
 			testInput((IInputState)o);
 		}
 		if (o instanceof IThinker) {
 			testThinker((IThinker)o);
 		}
 	}
 
 }
