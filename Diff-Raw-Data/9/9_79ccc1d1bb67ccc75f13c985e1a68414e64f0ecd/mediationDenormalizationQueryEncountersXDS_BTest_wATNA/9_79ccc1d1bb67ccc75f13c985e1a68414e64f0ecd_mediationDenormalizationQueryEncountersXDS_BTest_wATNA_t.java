 package org.jembi.rhea.flows;
 
 import static org.junit.Assert.assertNotNull;
 
 import java.util.HashMap;
 import java.util.Map;
 
 import org.apache.commons.logging.Log;
 import org.apache.commons.logging.LogFactory;
 import org.apache.log4j.Level;
 import org.apache.log4j.Logger;
 import org.jembi.rhea.Constants;
 import org.jembi.rhea.RestfulHttpRequest;
 import org.junit.Test;
 import org.mule.DefaultMuleMessage;
 import org.mule.api.MuleMessage;
 import org.mule.api.transport.PropertyScope;
 import org.mule.module.client.MuleClient;
 import org.mule.tck.junit4.FunctionalTestCase;
 
 /**
  * Copy of mediationDenormalizationQueryEncountersXDS_BTest with ATNA enabled
  */
 public class mediationDenormalizationQueryEncountersXDS_BTest_wATNA extends
 		FunctionalTestCase {
 	
 	private final Log log = LogFactory.getLog(this.getClass());
 	
 	@Override
 	protected void doSetUp() throws Exception {
 		Logger.getRootLogger().setLevel(Level.INFO);
 		super.doSetUp();
 	}
 	
 	@Override
 	protected void doTearDown() throws Exception {
 		Logger.getRootLogger().setLevel(Level.WARN);
 		super.doTearDown();
 	}
 
 	@Override
 	protected String getConfigResources() {
 		return "src/main/app/queryencounters-denormalization-xds.b.xml, src/main/app/atnasend.xml, src/main/app/global-elements.xml";
 	}
 	
 	@Test
 	public void testSend() throws Exception {
 		log.info("Starting test");
 	    MuleClient client = new MuleClient(muleContext);
 	    
 	    RestfulHttpRequest payload = new RestfulHttpRequest();
 	    payload.setHttpMethod(RestfulHttpRequest.HTTP_GET);
 	    
 	    Map<String, Object> properties = new HashMap<String, Object>();
 	    
 	    // NIST
 	    //payload.setPath("ws/rest/v1/patient/NID-1b48e083395f498/encounters");//NIST2010-2
	    //properties.put(Constants.ASSIGNING_AUTHORITY_OID_PROPERTY_NAME, "1.19.6.24.109.42.1.3");
 	    
 	    // Mohawk
 	    payload.setPath("ws/rest/v1/patient/MOH_CAAT_MARC_HI-3770298161/encounters");
 	    properties.put(Constants.ASSIGNING_AUTHORITY_OID_PROPERTY_NAME, "1.3.6.1.4.1.33349.3.1.100.2012.1.3");
 	    
 	    MuleMessage result = client.send("vm://queryEncounters-De-normailization-XDS.b", payload, properties);
 	    
 	    assertNotNull(result.getPayload());
 	    
 	    log.info(result.getPayloadAsString());
 	    
 	    log.info("Test completed");
 	}
 
 }
