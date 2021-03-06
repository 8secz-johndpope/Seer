 package de.metalcon.autocompleteServer.Create;
 
 import static org.junit.Assert.assertEquals;
 import static org.junit.Assert.fail;
 import static org.mockito.Mockito.mock;
 import static org.mockito.Mockito.when;
 
 import javax.servlet.ServletConfig;
 import javax.servlet.ServletContext;
 import javax.servlet.ServletException;
 import javax.servlet.http.HttpServlet;
 import javax.servlet.http.HttpServletRequest;
 
 import org.junit.Before;
 import org.junit.Test;
 
 import de.metalcon.autocompleteServer.Helper.ProtocolConstants;
 import de.metalcon.autocompleteServer.Helper.SuggestTree;
 import de.metalcon.utils.FormItemList;
 
 public class TestProcessCreateRequest {
 
 	final private ServletConfig servletConfig = mock(ServletConfig.class);
 	final private ServletContext servletContext = mock(ServletContext.class);
 
 	private HttpServletRequest request;
 
 	@Before
 	public void initializeTest() {
 
 		this.request = mock(HttpServletRequest.class);
 		HttpServlet servlet = mock(HttpServlet.class);
 		when(this.servletConfig.getServletContext()).thenReturn(
 				this.servletContext);
 		SuggestTree generalIndex = new SuggestTree(7);
 		when(
 				this.servletContext
 						.getAttribute(ProtocolConstants.INDEX_PARAMETER
 								+ "testIndex")).thenReturn(generalIndex);
 
 		// TODO: see fix-me at missingIndex-Test
 		when(
 				this.servletContext
 						.getAttribute(ProtocolConstants.INDEX_PARAMETER
 								+ "defaultIndex")).thenReturn(generalIndex);
 
 		try {
 			servlet.init(this.servletConfig);
 		} catch (ServletException e) {
 			fail("could not initialize servlet");
 			e.printStackTrace();
 		}
 	}
 
 	// TODO: add tests according to protocol specifications. Check each possible
 	// status.
 
 	@Test
 	public void testFormMissingSuggestString() {
 
 		ProcessCreateResponse testResponse = this.processTestRequest(
 				ProtocolTestConstants.VALID_SUGGESTION_KEY, null,
 				ProtocolTestConstants.VALID_SUGGESTION_WEIGHT,
 				ProtocolTestConstants.VALID_SUGGESTION_INDEX, null);
 
 		if (testResponse.getResponse().containsKey("Error:queryNameNotGiven")) {
 			assertEquals(CreateStatusCodes.QUERYNAME_NOT_GIVEN, testResponse
 					.getResponse().get("Error:queryNameNotGiven"));
 		} else {
 			fail("noTerm Status-Message missing!");
 		}
 
 	}
 
 	@Test
 	public void testFormMissingKey() {
 
 		ProcessCreateResponse testResponse = this.processTestRequest(null,
 				ProtocolTestConstants.VALID_SUGGESTION_STRING,
 				ProtocolTestConstants.VALID_SUGGESTION_WEIGHT,
 				ProtocolTestConstants.VALID_SUGGESTION_INDEX, null);
 
 		if (testResponse.getResponse().containsKey("Warning:KeyNotGiven")) {
 			assertEquals(CreateStatusCodes.SUGGESTION_KEY_NOT_GIVEN,
 					testResponse.getResponse().get("Warning:KeyNotGiven"));
 		} else {
 			fail("Error-Message missing!");
 		}
 
 	}
 
 	@Test
 	public void testFormMissingIndex() {
 
 		ProcessCreateResponse testResponse = this.processTestRequest(
 				ProtocolTestConstants.VALID_SUGGESTION_KEY,
 				ProtocolTestConstants.VALID_SUGGESTION_STRING,
 				ProtocolTestConstants.VALID_SUGGESTION_WEIGHT, null, null);
 
 		if (testResponse.getResponse().containsKey("Warning:DefaultIndex")) {
 			assertEquals(CreateStatusCodes.INDEXNAME_NOT_GIVEN, testResponse
 					.getResponse().get("Warning:DefaultIndex"));
 		} else {
 			fail("Error-Message missing!");
 		}
		assertEquals(ProtocolTestConstants.DEFAULT_INDEX, testResponse
				.getContainer().getComponents().getIndexName());
 
 	}
 
 	@Test
 	public void testFormSuggestStringTooLong() {
 
 		ProcessCreateResponse testResponse = this.processTestRequest(
 				ProtocolTestConstants.VALID_SUGGESTION_KEY,
 				ProtocolTestConstants.TOO_LONG_SUGGESTION_STRING,
 				ProtocolTestConstants.VALID_SUGGESTION_WEIGHT,
 				ProtocolTestConstants.VALID_SUGGESTION_INDEX, null);
 
 		if (testResponse.getResponse().containsKey("Error:queryNameTooLong")) {
 			assertEquals(CreateStatusCodes.SUGGESTION_STRING_TOO_LONG,
 					testResponse.getResponse().get("Error:queryNameTooLong"));
 		} else {
 			fail("Error-Message missing!");
 		}
 
 	}
 
 	@Test
 	public void testFormWeightNotGiven() {
 
 		ProcessCreateResponse testResponse = this.processTestRequest(
 				ProtocolTestConstants.VALID_SUGGESTION_KEY,
 				ProtocolTestConstants.VALID_SUGGESTION_STRING, null,
 				ProtocolTestConstants.VALID_SUGGESTION_INDEX, null);
 
 		if (testResponse.getResponse().containsKey("Error:WeightNotGiven")) {
 			assertEquals(CreateStatusCodes.WEIGHT_NOT_GIVEN, testResponse
 					.getResponse().get("Error:WeightNotGiven"));
 		} else {
 			fail("Error-Message missing!");
 		}
 
 	}
 
 	@Test
 	public void testFormWeightNotANumber() {
 
 		ProcessCreateResponse testResponse = this.processTestRequest(
 				ProtocolTestConstants.VALID_SUGGESTION_KEY,
 				ProtocolTestConstants.VALID_SUGGESTION_STRING,
 				ProtocolTestConstants.NOT_A_NUMBER_WEIGHT,
 				ProtocolTestConstants.VALID_SUGGESTION_INDEX, null);
 
 		if (testResponse.getResponse().containsKey("Error:WeightNotANumber")) {
 			assertEquals(CreateStatusCodes.WEIGHT_NOT_A_NUMBER, testResponse
 					.getResponse().get("Error:WeightNotANumber"));
 		} else {
 			fail("Error-Message missing!");
 		}
 
 	}
 
 	@Test
 	public void testFullFormWithImage() {
 
 		// TODO insert base64 encoded image
 		ProcessCreateResponse testResponse = this.processTestRequest(
 				ProtocolTestConstants.VALID_SUGGESTION_KEY,
 				ProtocolTestConstants.VALID_SUGGESTION_STRING,
 				ProtocolTestConstants.VALID_SUGGESTION_WEIGHT,
 				ProtocolTestConstants.VALID_SUGGESTION_INDEX, null);
 
 		assertEquals(ProtocolTestConstants.VALID_SUGGESTION_KEY, testResponse
 				.getContainer().getComponents().getKey());
 		assertEquals(ProtocolTestConstants.VALID_SUGGESTION_STRING,
 				testResponse.getContainer().getComponents().getSuggestString());
 		assertEquals(ProtocolTestConstants.VALID_SUGGESTION_WEIGHT,
 				testResponse.getContainer().getComponents().getWeight()
 						.toString());
 		assertEquals(ProtocolTestConstants.VALID_SUGGESTION_INDEX, testResponse
 				.getContainer().getComponents().getIndexName());
 		// assertEquals("{" + "\"term\"" + ":" + "\"test\"" + ","
 		// + "\"Warning:noImage\"" + ":" + "\"No image inserted\"" + "}",
 		// testResponse.getResponse().toString());
 		// assert image is B64 encoded and not null
 	}
 
 	@Test
 	public void testFullFormWithoutImage() {
 
 		ProcessCreateResponse testResponse = this.processTestRequest(
 				ProtocolTestConstants.VALID_SUGGESTION_KEY,
 				ProtocolTestConstants.VALID_SUGGESTION_STRING,
 				ProtocolTestConstants.VALID_SUGGESTION_WEIGHT,
 				ProtocolTestConstants.VALID_SUGGESTION_INDEX, null);
 
 		assertEquals(ProtocolTestConstants.VALID_SUGGESTION_KEY, testResponse
 				.getContainer().getComponents().getKey());
 		assertEquals(ProtocolTestConstants.VALID_SUGGESTION_STRING,
 				testResponse.getContainer().getComponents().getSuggestString());
 		assertEquals(ProtocolTestConstants.VALID_SUGGESTION_WEIGHT,
 				testResponse.getContainer().getComponents().getWeight()
 						.toString());
 		assertEquals(ProtocolTestConstants.VALID_SUGGESTION_INDEX, testResponse
 				.getContainer().getComponents().getIndexName());
 		assertEquals("{" + "\"term\"" + ":" + "\""
 				+ ProtocolTestConstants.VALID_SUGGESTION_STRING + "\"" + ","
 				+ "\"Warning:noImage\"" + ":" + "\"No image inserted\"" + "}",
 				testResponse.getResponse().toString());
 	}
 
 	private ProcessCreateResponse processTestRequest(String key, String term,
 			String weight, String index, String imageBase64) {
 
 		ProcessCreateResponse response = new ProcessCreateResponse(
 				this.servletConfig.getServletContext());
 		FormItemList testItems = new FormItemList();
 		if (key != null) {
 			testItems.addField(ProtocolConstants.SUGGESTION_KEY, key);
 		}
 		if (term != null) {
 			testItems.addField(ProtocolConstants.SUGGESTION_STRING, term);
 		}
 		if (weight != null) {
 			testItems.addField(ProtocolConstants.SUGGESTION_WEIGHT, weight);
 		}
 		if (index != null) {
 			testItems.addField(ProtocolConstants.INDEX_PARAMETER, index);
 		}
 		if (imageBase64 != null) {
 			testItems.addField(ProtocolConstants.IMAGE, imageBase64);
 		}
 
 		return ProcessCreateRequest.checkRequestParameter(testItems, response,
 				this.servletConfig.getServletContext());
 	}
 }
