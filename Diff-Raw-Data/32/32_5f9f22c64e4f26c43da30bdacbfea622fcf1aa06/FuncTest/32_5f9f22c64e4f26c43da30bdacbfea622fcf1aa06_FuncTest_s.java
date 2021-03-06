 package info.piwai.funkyjfunctional.apitest;
 
 import static com.google.common.collect.Lists.transform;
 import static info.piwai.funkyjfunctional.Funky.withFunc;
 import static java.util.Arrays.asList;
 import static org.junit.Assert.assertEquals;
 import static org.junit.Assert.assertTrue;
 import static org.junit.Assert.fail;
 import info.piwai.funkyjfunctional.Func;
 
import java.lang.reflect.InvocationTargetException;
 import java.util.List;
 
 import org.junit.Test;
 
 public class FuncTest {
 	
 	@Test
 	public void testTransform() throws Exception {
 
 		List<Integer> values = asList(42, 69);
 
 		class Price extends Func<Integer, String> {{ t = f+"$"; }}
 		
 		List<String> prices = transform(values, withFunc(Price.class));
 
 		assertEquals("42$", prices.get(0));
 		assertEquals("69$", prices.get(1));
 	}
 	
 	@Test
 	public void testStaticTransform() {
 		staticTransform();
 	}
 	
 	private static void staticTransform() {
 		List<Integer> values = asList(42, 69);
 
 		class Price extends Func<Integer, String> {{ t = f+"$"; }}
 		
 		List<String> prices = transform(values, withFunc(Price.class));
 
 		assertEquals("42$", prices.get(0));
 		assertEquals("69$", prices.get(1));
 	}
 	
     @Test
     public void testThrows() {
         class Fails extends Func<Object, Integer> {{ t = 42 / 0; }}
         try {
             withFunc(Fails.class).apply(null);
             fail();
         } catch(RuntimeException e) {
            assertTrue(e.getCause() instanceof InvocationTargetException);
            assertTrue(e.getCause().getCause() instanceof ArithmeticException);
         }
     }
 
 }
