 package uk.org.taverna.scufl2.translator.t2flow;
 
 import static org.junit.Assert.assertEquals;
 import static org.junit.Assert.assertFalse;
 import static org.junit.Assert.assertNotNull;
 
 import java.net.URL;
 
 import org.junit.Test;
 
 import uk.org.taverna.scufl2.api.common.Scufl2Tools;
 import uk.org.taverna.scufl2.api.container.WorkflowBundle;
 import uk.org.taverna.scufl2.api.core.Processor;
 import uk.org.taverna.scufl2.api.profiles.Profile;
 
 import com.fasterxml.jackson.databind.node.ObjectNode;
 
 public class TestSpreadsheetActivityParser {
 
 	private static final String SPREADSHEET_WF_WITH_DEFAULTS = "/spreadsheet_activity_defaults_892.t2flow";
 	private static Scufl2Tools scufl2Tools = new Scufl2Tools();
 
 	@Test
 	public void parseSpreadsheetWorkflow() throws Exception {
 		URL wfResource = getClass().getResource(SPREADSHEET_WF_WITH_DEFAULTS);
 		assertNotNull("Could not find workflow " + SPREADSHEET_WF_WITH_DEFAULTS, wfResource);
 		T2FlowParser parser = new T2FlowParser();
 		parser.setValidating(true);
 		parser.setStrict(true);
 		WorkflowBundle wfBundle = parser.parseT2Flow(wfResource.openStream());
 		Profile profile = wfBundle.getMainProfile();
 		Processor proc = wfBundle.getMainWorkflow().getProcessors().getByName("SpreadsheetImport");
 		ObjectNode config = scufl2Tools
 				.configurationForActivityBoundToProcessor(proc, profile).getJsonAsObjectNode();
 		assertNotNull(config);
		System.out.println(config);
 		assertEquals("",config.get("emptyCellValue").asText());
 		assertFalse(config.has("outputFormat"));
 		assertFalse(config.has("csvDelimiter"));
 	}
 	
 }
