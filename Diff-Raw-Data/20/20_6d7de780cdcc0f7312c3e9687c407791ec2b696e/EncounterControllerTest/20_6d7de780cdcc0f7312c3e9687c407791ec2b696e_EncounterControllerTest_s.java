 package org.openmrs.module.webservices.rest.web.controller;
 
 import java.text.DateFormat;
 import java.text.SimpleDateFormat;
 import java.util.Date;
 import java.util.List;
 
 import javax.servlet.http.HttpServletResponse;
 
 import org.apache.commons.beanutils.PropertyUtils;
 import org.codehaus.jackson.map.ObjectMapper;
 import org.hibernate.exception.ConstraintViolationException;
 import org.junit.Assert;
 import org.junit.Test;
 import org.openmrs.Encounter;
 import org.openmrs.Patient;
 import org.openmrs.api.context.Context;
 import org.openmrs.module.webservices.rest.SimpleObject;
 import org.openmrs.module.webservices.rest.test.Util;
 import org.openmrs.module.webservices.rest.web.RestConstants;
 import org.openmrs.web.test.BaseModuleWebContextSensitiveTest;
 import org.springframework.mock.web.MockHttpServletRequest;
 import org.springframework.mock.web.MockHttpServletResponse;
 import org.springframework.web.context.request.WebRequest;
 
 public class EncounterControllerTest extends BaseModuleWebContextSensitiveTest {
 	
 	/**
 	 * @see EncounterController#create(SimpleObject,WebRequest,HttpServletResponse)
 	 * @verifies create a new encounter
 	 */
 	@Test
 	public void createEncounter_shouldCreateANewEncounter() throws Exception {
 		int before = Context.getEncounterService().getAllEncounters(null).size();
 		String json = "{\"location\":\"9356400c-a5a2-4532-8f2b-2361b3446eb8\", \"encounterType\": \"61ae96f4-6afe-4351-b6f8-cd4fc383cce1\", \"encounterDatetime\": \"2011-01-15\", \"patient\": \"da7f524f-27ce-4bb2-86d6-6d1d05312bd5\", \"provider\":\"ba1b19c2-3ed6-4f63-b8c0-f762dc8d7562\"}";
 		SimpleObject post = new ObjectMapper().readValue(json, SimpleObject.class);
 		Object newPatient = new EncounterController().create(post, emptyRequest(), new MockHttpServletResponse());
 		Assert.assertNotNull(newPatient);
 		Assert.assertEquals(before + 1, Context.getEncounterService().getAllEncounters(null).size());
 	}
 	
 	/**
 	 * @see EncounterController#find(String,WebRequest,HttpServletResponse)
 	 * @verifies return no results if there are no matching encounters
 	 */
 	@Test
 	public void findEncounters_shouldReturnNoResultsIfThereAreNoMatchingEncounters() throws Exception {
 		List<Object> results = (List<Object>) new EncounterController().search("noencounter", emptyRequest(),
 		    new MockHttpServletResponse()).get("results");
 		Assert.assertEquals(0, results.size());
 	}
 	
 	/**
 	 * @see EncounterController#getEncounter(String,WebRequest)
 	 * @verifies get a default representation of a encounter
 	 */
 	@Test
 	public void getEncounter_shouldGetADefaultRepresentationOfAEncounter() throws Exception {
 		Object result = new EncounterController().retrieve("6519d653-393b-4118-9c83-a3715b82d4ac", emptyRequest());
 		Assert.assertNotNull(result);
 		Assert.assertEquals("6519d653-393b-4118-9c83-a3715b82d4ac", PropertyUtils.getProperty(result, "uuid"));
 		Assert.assertNotNull(PropertyUtils.getProperty(result, "encounterType"));
 		Assert.assertNotNull(PropertyUtils.getProperty(result, "patient"));
 		Assert.assertNull(PropertyUtils.getProperty(result, "auditinfo"));
 	}
 	
 	/**
 	 * @see EncounterController#getEncounter(String,WebRequest)
 	 * @verifies get a full representation of a encounter
 	 */
 	@Test
 	public void getEncounter_shouldGetAFullRepresentationOfAEncounter() throws Exception {
 		MockHttpServletRequest req = new MockHttpServletRequest();
 		req.addParameter(RestConstants.REQUEST_PROPERTY_FOR_REPRESENTATION, RestConstants.REPRESENTATION_FULL);
 		
 		Object result = new EncounterController().retrieve("6519d653-393b-4118-9c83-a3715b82d4ac", req);
 		Assert.assertNotNull(result);
 		Assert.assertEquals("6519d653-393b-4118-9c83-a3715b82d4ac", PropertyUtils.getProperty(result, "uuid"));
 		Assert.assertNotNull(PropertyUtils.getProperty(result, "encounterType"));
 		Assert.assertNotNull(PropertyUtils.getProperty(result, "patient"));
 		Assert.assertNotNull(PropertyUtils.getProperty(result, "auditInfo"));
 	}
 	
 	/**
 	 * @see EncounterController#getEncounter(String,WebRequest)
 	 * @verifies get a full representation of a encounter including obs groups
 	 */
 	@Test
 	public void getEncounter_shouldGetAFullRepresentationOfAEncounterIncludingObsGroups() throws Exception {
		executeDataSet("org/openmrs/module/webservices/rest/web/controller/include/EncounterWithObsGroup.xml");
 		MockHttpServletRequest req = new MockHttpServletRequest();
 		req.addParameter(RestConstants.REQUEST_PROPERTY_FOR_REPRESENTATION, RestConstants.REPRESENTATION_FULL);
 		
 		SimpleDateFormat ymd = new SimpleDateFormat("yyyy-MM-dd");
 		
 		SimpleObject result = (SimpleObject) new EncounterController().retrieve("62967e68-96bb-11e0-8d6b-9b9415a91465", req);
 		Util.log("full", result);
 		Assert.assertNotNull(result);
 		Assert.assertEquals("62967e68-96bb-11e0-8d6b-9b9415a91465", result.get("uuid"));
 		Assert.assertNotNull(result.get("obs"));
 		Assert.assertEquals("0f97e14e-cdc2-49ac-9255-b5126f8a5147", Util.getByPath(result, "obs[0]/concept/uuid"));
 		Assert.assertEquals("96408258-000b-424e-af1a-403919332938", Util.getByPath(result,
 		    "obs[0]/groupMembers[0]/concept/uuid"));
 		Assert.assertEquals("Some text", Util.getByPath(result, "obs[0]/groupMembers[0]/value"));
 		Assert.assertEquals("11716f9c-1434-4f8d-b9fc-9aa14c4d6126", Util.getByPath(result,
 		    "obs[0]/groupMembers[1]/concept/uuid"));
 		// failing because of date format: Assert.assertEquals(ymd.parse("2011-06-12"), Util.getByPath(result, "obs[0]/groupMembers[1]/value"));
 		
 		// make sure there's a group in the group
 		Assert.assertEquals("0f97e14e-cdc2-49ac-9255-b5126f8a5147", Util.getByPath(result,
 		    "obs[0]/groupMembers[2]/concept/uuid"));
 		Assert.assertEquals("96408258-000b-424e-af1a-403919332938", Util.getByPath(result,
 		    "obs[0]/groupMembers[2]/groupMembers[0]/concept/uuid"));
 		Assert.assertEquals("Some text", Util.getByPath(result, "obs[0]/groupMembers[2]/groupMembers[0]/value"));
 		Assert.assertEquals("11716f9c-1434-4f8d-b9fc-9aa14c4d6126", Util.getByPath(result,
 		    "obs[0]/groupMembers[2]/groupMembers[1]/concept/uuid"));
 		// failing because of date format: Assert.assertEquals(ymd.parse("2011-06-12"), Util.getByPath(result, "obs[0]/groupMembers[2]/groupMembers[1]/value"));
 	}
 	
 	/**
 	 * @see EncounterController#purge(String,WebRequest,HttpServletResponse)
 	 * @verifies fail to purge a encounter with dependent data
 	 */
 	@Test(expected = ConstraintViolationException.class)
 	public void purgeEncounter_shouldNotPurgeAEncounterWithDependentData() throws Exception {
 		int size = Context.getEncounterService().getEncountersByPatient(new Patient(7)).size();
 		new EncounterController().purge("6519d653-393b-4118-9c83-a3715b82d4ac", emptyRequest(),
 		    new MockHttpServletResponse());
 		Assert.assertEquals(size - 1, Context.getEncounterService().getEncountersByPatient(new Patient(7)).size());
 	}
 	
 	/**
 	 * @see EncounterController#update(String,SimpleObject,WebRequest,HttpServletResponse)
 	 * @verifies change a property on a encounter
 	 */
 	@Test
 	public void updateEncounter_shouldChangeAPropertyOnAEncounter() throws Exception {
 		Date now = new Date();
 		DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
 		SimpleObject post = new ObjectMapper().readValue("{\"encounterDatetime\":\"" + df.format(now) + "\"}",
 		    SimpleObject.class);
 		Object editedPatient = new EncounterController().update("6519d653-393b-4118-9c83-a3715b82d4ac", post,
 		    emptyRequest(), new MockHttpServletResponse());
 		Assert.assertEquals(df.format(now), df.format(Context.getEncounterService().getEncounter(3).getEncounterDatetime()));
 	}
 	
 	/**
 	 * @see EncounterController#delete(String,String,WebRequest,HttpServletResponse)
 	 * @verifies void a encounter
 	 */
 	@Test
 	public void voidEncounter_shouldVoidAEncounter() throws Exception {
 		Encounter enc = Context.getEncounterService().getEncounter(3);
 		Assert.assertFalse(enc.isVoided());
 		new EncounterController().delete("6519d653-393b-4118-9c83-a3715b82d4ac", "unit test", emptyRequest(),
 		    new MockHttpServletResponse());
 		enc = Context.getEncounterService().getEncounter(3);
 		Assert.assertTrue(enc.isVoided());
 		Assert.assertEquals("unit test", enc.getVoidReason());
 	}
 	
 	private MockHttpServletRequest emptyRequest() {
 		return new MockHttpServletRequest();
 	}
 }
