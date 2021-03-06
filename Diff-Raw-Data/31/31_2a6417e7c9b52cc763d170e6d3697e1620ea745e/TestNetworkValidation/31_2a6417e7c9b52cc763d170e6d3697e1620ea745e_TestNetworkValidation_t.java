 package neural.parsec.validation;
 
import static org.junit.Assert.assertEquals;
 import static org.junit.Assert.assertTrue;
 
 import java.util.ArrayList;
 import java.util.List;
 
 import neural.parsec.ast.IntegerParameter;
 import neural.parsec.ast.Parameter;
 
 import org.junit.Before;
 import org.junit.Test;
 
 public class TestNetworkValidation {
 	
 	private ValidationFactory validationFactory;
 
 	@Before
 	public void setUpValidationFactory() {
 		validationFactory = new ValidationFactory("scripts/encog.validation");
 	}
 	
 	@Test
 	public void testHopfieldValidator() {
 		Validator validator = validationFactory.getValidator("network.hopfield");
 		List<Parameter> params = new ArrayList<Parameter>();
 		
 		validator.validate(params);
 		assertTrue(!validator.isValid());
		String[] msgs = validator.getIssues();
		assertEquals(1, msgs.length);
		assertEquals("Mandantory parameter 'size' is missing", msgs[0]);
 		
 		params.add(new IntegerParameter("size", 10));
 		validator.validate(params);
 		assertTrue(validator.isValid());
		
		params.add(new IntegerParameter("notneeded", 42));
		validator.validate(params);
		assertTrue(!validator.isValid());
		assertEquals(1, msgs.length);
		assertEquals("Mandantory parameter 'size' is missing", msgs[0]);
 	}
 
 }
