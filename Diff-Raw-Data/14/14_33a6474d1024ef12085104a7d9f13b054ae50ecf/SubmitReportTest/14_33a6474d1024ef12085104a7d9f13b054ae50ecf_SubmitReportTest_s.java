 /*
  * This program is part of the OpenLMIS logistics management information system platform software.
  * Copyright © 2013 VillageReach
  *
  * This program is free software: you can redistribute it and/or modify it under the terms of the GNU Affero General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
  *  
  * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Affero General Public License for more details.
  * You should have received a copy of the GNU Affero General Public License along with this program.  If not, see http://www.gnu.org/licenses.  For additional information contact info@OpenLMIS.org. 
  */
 
 package org.openlmis.functional;
 
 import org.openlmis.UiUtils.HttpClient;
 import org.openlmis.UiUtils.ResponseEntity;
 import org.openlmis.restapi.domain.Report;
 import org.testng.annotations.AfterMethod;
 import org.testng.annotations.BeforeMethod;
 import org.testng.annotations.Test;
 
 import java.io.IOException;
 import java.sql.SQLException;
 
 import static com.thoughtworks.selenium.SeleneseTestBase.assertEquals;
 import static com.thoughtworks.selenium.SeleneseTestBase.assertTrue;
 import static org.openlmis.UiUtils.HttpClient.POST;
 

 public class SubmitReportTest extends JsonUtility {
 
   public static final String MINIMUM_JSON_TXT_FILE_NAME = "ReportMinimumJson.txt";
   public static final String FULL_JSON_TXT_FILE_NAME = "ReportFullJson.txt";
   public static final String PRODUCT_JSON_TXT_FILE_NAME = "ReportWithProductJson.txt";
 
   @BeforeMethod(groups = {"webservice", "webserviceSmoke"})
   public void setUp() throws Exception {
     super.setup();
     super.setupTestData(false);
     createVirtualFacilityThroughApi("V10", "F10");
     dbWrapper.insertProcessingPeriod("current", "current period", "2013-01-30", "2016-01-30", 1, "M");
     dbWrapper.insertRoleAssignmentForSupervisoryNodeForProgramId1("700", "store in-charge", "N1");
     dbWrapper.updateRestrictLogin("commTrack", true);
   }
 
   @AfterMethod(groups = {"webservice", "webserviceSmoke"})
   public void tearDown() throws IOException, SQLException {
     dbWrapper.deleteData();
     dbWrapper.closeConnection();
   }
 
   @Test(groups = {"webserviceSmoke"})
   public void testInitiateRnr() throws Exception {
     long id = submitRnRThroughApi("V10", "HIV", "P10", 1, 10, 1, 0, 0, 2);
     assertEquals("AUTHORIZED", dbWrapper.getAttributeFromTable("requisitions", "status", "id", String.valueOf(id)));
   }
 
   @Test(groups = {"webserviceSmoke"})
   public void shouldReturn401StatusWhenSubmittingReportWithInvalidAPIUser() throws Exception {
     HttpClient client = new HttpClient();
     client.createContext();
 
     Report reportFromJson = JsonUtility.readObjectFromFile(MINIMUM_JSON_TXT_FILE_NAME, Report.class);
     reportFromJson.setAgentCode("V10");
     reportFromJson.setProgramCode("HIV");
 
     ResponseEntity responseEntity =
       client.SendJSON(
         getJsonStringFor(reportFromJson),
         "http://localhost:9091/rest-api/requisitions.json",
         POST,
         "commTrack1000",
         "Admin123");
 
     assertEquals(401, responseEntity.getStatus());
   }
 
   @Test(groups = {"webservice"})
   public void testSubmitReportWhenUserWithoutRights() throws Exception {
     dbWrapper.deleteSupervisoryRoleFromRoleAssignment();
     HttpClient client = new HttpClient();
     client.createContext();
 
     Report reportFromJson = readObjectFromFile(FULL_JSON_TXT_FILE_NAME, Report.class);
     reportFromJson.setAgentCode("V10");
     reportFromJson.setProgramCode("HIV");
 
     ResponseEntity responseEntity = client.SendJSON(getJsonStringFor(reportFromJson),
       "http://localhost:9091/rest-api/requisitions.json",
       POST,
       "commTrack",
       "Admin123");
 
     assertEquals(400, responseEntity.getStatus());
     assertEquals(responseEntity.getResponse(), "{\"error\":\"User does not have permission\"}");
   }
 
   @Test(groups = {"webservice"})
   public void testSubmitReportInvalidFacility() throws Exception {
     HttpClient client = new HttpClient();
     client.createContext();
 
     Report reportFromJson = readObjectFromFile(FULL_JSON_TXT_FILE_NAME, Report.class);
     reportFromJson.setAgentCode("Invalid");
     reportFromJson.setProgramCode("HIV");
 
     ResponseEntity responseEntity = client.SendJSON(getJsonStringFor(reportFromJson),
       "http://localhost:9091/rest-api/requisitions.json",
       POST,
       "commTrack",
       "Admin123");
 
     assertEquals(400, responseEntity.getStatus());
     assertEquals(responseEntity.getResponse(), "{\"error\":\"Invalid Facility code\"}");
   }
 
   @Test(groups = {"webservice"})
   public void testSubmitReportWithoutHeaders() throws Exception {
     HttpClient client = new HttpClient();
     client.createContext();
     Report reportFromJson = readObjectFromFile(FULL_JSON_TXT_FILE_NAME, Report.class);
     reportFromJson.setAgentCode("V10");
     reportFromJson.setProgramCode("HIV");
 
     ResponseEntity responseEntity = client.SendJSONWithoutHeaders(getJsonStringFor(reportFromJson),
       "http://localhost:9091/rest-api/requisitions.json",
       POST,
       "",
       "");
     assertEquals(401, responseEntity.getStatus());
   }
 
   @Test(groups = {"webservice"})
   public void testSubmitReportInvalidProgram() throws Exception {
     HttpClient client = new HttpClient();
     client.createContext();
     Report reportFromJson = JsonUtility.readObjectFromFile(FULL_JSON_TXT_FILE_NAME, Report.class);
     reportFromJson.setAgentCode("V10");
     reportFromJson.setProgramCode("InvalidProgram");
 
     ResponseEntity responseEntity = client.SendJSON(getJsonStringFor(reportFromJson),
       "http://localhost:9091/rest-api/requisitions.json",
       POST,
       "commTrack",
       "Admin123");
 
     assertEquals(400, responseEntity.getStatus());
     assertEquals("{\"error\":\"Invalid program code\"}", responseEntity.getResponse());
   }
 
   @Test(groups = {"webservice"})
   public void testSubmitReportMandatoryFieldsMissing() throws Exception {
     HttpClient client = new HttpClient();
     client.createContext();
     Report reportFromJson = JsonUtility.readObjectFromFile(FULL_JSON_TXT_FILE_NAME, Report.class);
     reportFromJson.setAgentCode("V10");
 
     ResponseEntity responseEntity = client.SendJSON(getJsonStringFor(reportFromJson),
       "http://localhost:9091/rest-api/requisitions.json",
       POST,
       "commTrack",
       "Admin123");
 
     assertEquals(400, responseEntity.getStatus());
     assertEquals("{\"error\":\"Missing mandatory fields\"}", responseEntity.getResponse());
 
     reportFromJson = JsonUtility.readObjectFromFile(FULL_JSON_TXT_FILE_NAME, Report.class);
     reportFromJson.setProgramCode("HIV");
 
     responseEntity = client.SendJSON(getJsonStringFor(reportFromJson),
       "http://localhost:9091/rest-api/requisitions.json",
       POST,
       "commTrack",
       "Admin123");
 
     assertEquals(400, responseEntity.getStatus());
     assertEquals("{\"error\":\"Missing mandatory fields\"}", responseEntity.getResponse());
   }
 
   @Test(groups = {"webservice"})
   public void testSubmitReportWhenVirtualFacilityInactive() throws Exception {
     dbWrapper.updateActiveStatusOfFacility("V10", "false");
     HttpClient client = new HttpClient();
     client.createContext();
     Report reportFromJson = JsonUtility.readObjectFromFile(FULL_JSON_TXT_FILE_NAME, Report.class);
     reportFromJson.setAgentCode("V10");
     reportFromJson.setProgramCode("HIV");
 
     ResponseEntity responseEntity = client.SendJSON(getJsonStringFor(reportFromJson),
       "http://localhost:9091/rest-api/requisitions.json",
       POST,
       "commTrack",
       "Admin123");
 
     assertEquals(400, responseEntity.getStatus());
     assertEquals("{\"error\":\"Facility is inoperative\"}", responseEntity.getResponse());
   }
 
   @Test(groups = {"webservice"})
   public void testSubmitReportWhenVirtualFacilityDisabled() throws Exception {
     dbWrapper.updateFacilityFieldBYCode("enabled", "false", "V10");
     HttpClient client = new HttpClient();
     client.createContext();
     Report reportFromJson = JsonUtility.readObjectFromFile(FULL_JSON_TXT_FILE_NAME, Report.class);
     reportFromJson.setAgentCode("V10");
     reportFromJson.setProgramCode("HIV");
 
     ResponseEntity responseEntity = client.SendJSON(getJsonStringFor(reportFromJson),
       "http://localhost:9091/rest-api/requisitions.json",
       POST,
       "commTrack",
       "Admin123");
 
     assertEquals(400, responseEntity.getStatus());
     assertEquals("{\"error\":\"Facility is inoperative\"}", responseEntity.getResponse());
   }
 
   @Test(groups = {"webservice"})
   public void testSubmitReportWhenParentFacilityInactive() throws Exception {
     dbWrapper.updateActiveStatusOfFacility("V10", "false");
     HttpClient client = new HttpClient();
     client.createContext();
     Report reportFromJson = JsonUtility.readObjectFromFile(FULL_JSON_TXT_FILE_NAME, Report.class);
     reportFromJson.setAgentCode("V10");
     reportFromJson.setProgramCode("HIV");
 
     ResponseEntity responseEntity = client.SendJSON(getJsonStringFor(reportFromJson),
       "http://localhost:9091/rest-api/requisitions.json",
       POST,
       "commTrack",
       "Admin123");
 
     assertEquals(400, responseEntity.getStatus());
     assertEquals("{\"error\":\"Facility is inoperative\"}", responseEntity.getResponse());
   }
 
   @Test(groups = {"webservice"})
   public void testSubmitReportWhenProgramGloballyInactive() throws Exception {
     dbWrapper.updateActiveStatusOfProgram("HIV", false);
     HttpClient client = new HttpClient();
     client.createContext();
     Report reportFromJson = JsonUtility.readObjectFromFile(FULL_JSON_TXT_FILE_NAME, Report.class);
     reportFromJson.setAgentCode("V10");
     reportFromJson.setProgramCode("HIV");
 
     ResponseEntity responseEntity = client.SendJSON(getJsonStringFor(reportFromJson),
       "http://localhost:9091/rest-api/requisitions.json",
       POST,
       "commTrack",
       "Admin123");
 
     assertEquals(400, responseEntity.getStatus());
     assertEquals("{\"error\":\"User does not have permission\"}", responseEntity.getResponse());
     dbWrapper.updateActiveStatusOfProgram("HIV", true);
   }
 
   @Test(groups = {"webservice"})
   public void testSubmitReportWhenProgramInactiveAtVirtualFacility() throws Exception {
     dbWrapper.updateProgramsSupportedByField("active", "false", "V10");
     HttpClient client = new HttpClient();
     client.createContext();
     Report reportFromJson = JsonUtility.readObjectFromFile(FULL_JSON_TXT_FILE_NAME, Report.class);
     reportFromJson.setAgentCode("V10");
     reportFromJson.setProgramCode("HIV");
 
     ResponseEntity responseEntity = client.SendJSON(getJsonStringFor(reportFromJson),
       "http://localhost:9091/rest-api/requisitions.json",
       POST,
       "commTrack",
       "Admin123");
 
     assertEquals(400, responseEntity.getStatus());
     assertEquals("{\"error\":\"User does not have permission\"}", responseEntity.getResponse());
   }
 
   @Test(groups = {"webservice"})
   public void testSubmitReportWhenParentFacilityDisabled() throws Exception {
     dbWrapper.updateFacilityFieldBYCode("enabled", "false", "F10");
     HttpClient client = new HttpClient();
     client.createContext();
     Report reportFromJson = JsonUtility.readObjectFromFile(FULL_JSON_TXT_FILE_NAME, Report.class);
     reportFromJson.setAgentCode("V10");
     reportFromJson.setProgramCode("HIV");
 
     ResponseEntity responseEntity = client.SendJSON(getJsonStringFor(reportFromJson),
       "http://localhost:9091/rest-api/requisitions.json",
       POST,
       "commTrack",
       "Admin123");
 
     assertEquals(400, responseEntity.getStatus());
     assertEquals("{\"error\":\"Facility is inoperative\"}", responseEntity.getResponse());
     dbWrapper.updateFacilityFieldBYCode("enabled", "true", "F10");
   }
 
   @Test(groups = {"webservice"})
   public void testSubmitReportWhenTemplateNotConfigured() throws Exception {
     dbWrapper.deleteRnrTemplate();
     HttpClient client = new HttpClient();
     client.createContext();
     Report reportFromJson = JsonUtility.readObjectFromFile(FULL_JSON_TXT_FILE_NAME, Report.class);
     reportFromJson.setAgentCode("V10");
     reportFromJson.setProgramCode("HIV");
 
     ResponseEntity responseEntity = client.SendJSON(getJsonStringFor(reportFromJson),
       "http://localhost:9091/rest-api/requisitions.json",
       POST,
       "commTrack",
       "Admin123");
 
     assertEquals(400, responseEntity.getStatus());
     assertEquals("{\"error\":\"Please contact admin to define R&R template for this program\"}",
       responseEntity.getResponse());
   }
 
   @Test(groups = {"webservice"})
   public void shouldSubmitDuplicateReport() throws Exception {
     HttpClient client = new HttpClient();
     client.createContext();
 
     Report reportFromJson = readObjectFromFile(FULL_JSON_TXT_FILE_NAME, Report.class);
     reportFromJson.setAgentCode("V10");
     reportFromJson.setProgramCode("HIV");
     reportFromJson.getProducts().get(0).setProductCode("P10");
 
     String jsonStringFor = getJsonStringFor(reportFromJson);
     client.SendJSON(jsonStringFor,
       "http://localhost:9091/rest-api/requisitions.json",
       POST,
       "commTrack",
       "Admin123");
 
     ResponseEntity responseEntity = client.SendJSON(jsonStringFor,
       "http://localhost:9091/rest-api/requisitions.json",
       POST,
       "commTrack",
       "Admin123");
 
     assertEquals(201, responseEntity.getStatus());
     assertTrue(responseEntity.getResponse().contains("{\"requisitionId\":"));
   }
 
 
   @Test(groups = {"webservice"})
   public void testBlankProductSubmitReport() throws Exception {
     HttpClient client = new HttpClient();
     client.createContext();
     Report reportFromJson = JsonUtility.readObjectFromFile(PRODUCT_JSON_TXT_FILE_NAME, Report.class);
     reportFromJson.setAgentCode("V10");
     reportFromJson.setProgramCode("HIV");
 
 
     ResponseEntity responseEntity = client.SendJSON(getJsonStringFor(reportFromJson), "http://localhost:9091/rest-api/requisitions.json", POST,
       "commTrack",
       "Admin123");
 
     assertEquals(400, responseEntity.getStatus());
     assertEquals("{\"error\":\"Invalid product codes [null]\"}", responseEntity.getResponse());
   }
 
   @Test(groups = {"webservice"})
   public void testInitiateRnrWhenCurrentPeriodNotDefined() throws Exception {
     dbWrapper.deleteCurrentPeriod();
     HttpClient client = new HttpClient();
     client.createContext();
     Report reportFromJson = JsonUtility.readObjectFromFile(PRODUCT_JSON_TXT_FILE_NAME, Report.class);
     reportFromJson.setAgentCode("V10");
     reportFromJson.setProgramCode("HIV");
     reportFromJson.getProducts().get(0).setProductCode("P10");
 
     ResponseEntity responseEntity = client.SendJSON(getJsonStringFor(reportFromJson), "http://localhost:9091/rest-api/requisitions.json", POST,
       "commTrack",
       "Admin123");
 
     assertEquals(400, responseEntity.getStatus());
     assertEquals(responseEntity.getResponse(), "{\"error\":\"Program configuration missing\"}");
   }
 
   @Test(groups = {"webservice"})
   public void testInitiateRnrWhenProgramStartDateIsAfterCurrentDateAndInCurrentPeriod() throws Exception {
     dbWrapper.updateProgramsSupportedByField("startDate", "2015-01-01", "V10");
     HttpClient client = new HttpClient();
     client.createContext();
     Report reportFromJson = JsonUtility.readObjectFromFile(PRODUCT_JSON_TXT_FILE_NAME, Report.class);
     reportFromJson.setAgentCode("V10");
     reportFromJson.setProgramCode("HIV");
     reportFromJson.getProducts().get(0).setProductCode("P10");
     reportFromJson.getProducts().get(0).setBeginningBalance(1);
     reportFromJson.getProducts().get(0).setQuantityDispensed(1);
     reportFromJson.getProducts().get(0).setQuantityReceived(4);
     reportFromJson.getProducts().get(0).setStockInHand(4);
 
     ResponseEntity responseEntity = client.SendJSON(getJsonStringFor(reportFromJson), "http://localhost:9091/rest-api/requisitions.json", POST,
       "commTrack",
       "Admin123");
 
     assertEquals(201, responseEntity.getStatus());
     assertTrue(responseEntity.getResponse().contains("{\"requisitionId\":"));
   }
 
   @Test(groups = {"webservice"})
   public void testInitiateRnrWhenProgramStartDateIsAfterCurrentDateAndCurrentPeriodEndDate() throws Exception {
     dbWrapper.insertProcessingPeriod("future", "future period", "2016-01-30", "2017-01-30", 1, "M");
     dbWrapper.updateProgramsSupportedByField("startDate", "2017-01-01", "V10");
     HttpClient client = new HttpClient();
     client.createContext();
     Report reportFromJson = JsonUtility.readObjectFromFile(PRODUCT_JSON_TXT_FILE_NAME, Report.class);
     reportFromJson.setAgentCode("V10");
     reportFromJson.setProgramCode("HIV");
     reportFromJson.getProducts().get(0).setProductCode("P10");
 
     ResponseEntity responseEntity = client.SendJSON(getJsonStringFor(reportFromJson), "http://localhost:9091/rest-api/requisitions.json", POST,
       "commTrack",
       "Admin123");
 
     assertEquals(400, responseEntity.getStatus());
     assertEquals(responseEntity.getResponse(), "{\"error\":\"Program configuration missing\"}");
   }
 
   @Test(groups = {"webservice"})
   public void testInvalidProductSubmitReport() throws Exception {
     HttpClient client = new HttpClient();
     client.createContext();
 
     Report reportFromJson = JsonUtility.readObjectFromFile(FULL_JSON_TXT_FILE_NAME, Report.class);
     reportFromJson.setAgentCode("V10");
     reportFromJson.setProgramCode("HIV");
     reportFromJson.getProducts().get(0).setProductCode("P10000");
 
     ResponseEntity responseEntity =
       client.SendJSON(getJsonStringFor(reportFromJson),
         "http://localhost:9091/rest-api/requisitions.json",
         POST,
         "commTrack",
         "Admin123");
 
     assertEquals(400, responseEntity.getStatus());
     assertEquals("{\"error\":\"Invalid product codes [P10000]\"}", responseEntity.getResponse());
   }
 
   @Test(groups = {"webservice"})
   public void testGloballyInactiveProductSubmitReport() throws Exception {
     dbWrapper.updateActiveStatusOfProduct("P10", "False");
     HttpClient client = new HttpClient();
     client.createContext();
 
     Report reportFromJson = JsonUtility.readObjectFromFile(FULL_JSON_TXT_FILE_NAME, Report.class);
     reportFromJson.setAgentCode("V10");
     reportFromJson.setProgramCode("HIV");
     reportFromJson.getProducts().get(0).setProductCode("P10");
 
     ResponseEntity responseEntity =
       client.SendJSON(getJsonStringFor(reportFromJson),
         "http://localhost:9091/rest-api/requisitions.json",
         POST,
         "commTrack",
         "Admin123");
 
     assertEquals(400, responseEntity.getStatus());
     assertEquals("{\"error\":\"Invalid product codes [P10]\"}", responseEntity.getResponse());
     dbWrapper.updateActiveStatusOfProduct("P10", "True");
   }
 
   @Test(groups = {"webservice"})
   public void testProductNotActiveAtProgramSubmitReport() throws Exception {
     dbWrapper.updateActiveStatusOfProgramProduct("P10", "HIV", "false");
     HttpClient client = new HttpClient();
     client.createContext();
 
     Report reportFromJson = JsonUtility.readObjectFromFile(FULL_JSON_TXT_FILE_NAME, Report.class);
     reportFromJson.setAgentCode("V10");
     reportFromJson.setProgramCode("HIV");
     reportFromJson.getProducts().get(0).setProductCode("P10");
 
     ResponseEntity responseEntity =
       client.SendJSON(getJsonStringFor(reportFromJson),
         "http://localhost:9091/rest-api/requisitions.json",
         POST,
         "commTrack",
         "Admin123");
 
     assertEquals(400, responseEntity.getStatus());
     assertEquals("{\"error\":\"Invalid product codes [P10]\"}", responseEntity.getResponse());
     dbWrapper.updateActiveStatusOfProgramProduct("P10", "HIV", "true");
   }
 
   @Test(groups = {"webservice"})
   public void testProductNotAvailableAtFacilitySubmitReport() throws Exception {
     dbWrapper.deleteProductAvailableAtFacility("P10", "HIV", "V10");
     HttpClient client = new HttpClient();
     client.createContext();
 
     Report reportFromJson = JsonUtility.readObjectFromFile(FULL_JSON_TXT_FILE_NAME, Report.class);
     reportFromJson.setAgentCode("V10");
     reportFromJson.setProgramCode("HIV");
     reportFromJson.getProducts().get(0).setProductCode("P10");
 
     ResponseEntity responseEntity =
       client.SendJSON(getJsonStringFor(reportFromJson),
         "http://localhost:9091/rest-api/requisitions.json",
         POST,
         "commTrack",
         "Admin123");
 
     assertEquals(400, responseEntity.getStatus());
     assertEquals("{\"error\":\"Invalid product codes [P10]\"}", responseEntity.getResponse());
   }
 
   @Test(groups = {"webservice"})
   public void testSubmitReportWithUnrecognizedField() throws Exception {
     HttpClient client = new HttpClient();
     client.createContext();
 
     String wrongJson = "{\"agentCode\": \"V10\"," +
       "    \"programCode\": \"HIV\"," +
       "    \"blah\" : \"blah\"," +
       "    \"products\": [" +
       "        {" +
       "            \"productCode\": \"P10\"," +
       "        }" +
       "    ]" +
       "}";
 
     ResponseEntity responseEntity =
       client.SendJSON(wrongJson,
         "http://localhost:9091/rest-api/requisitions.json",
         POST,
         "commTrack",
         "Admin123");
 
     assertEquals(400, responseEntity.getStatus());
   }
 
   @Test(groups = {"webservice"})
   public void testPreviousPeriodSubmitReportForParentFacility() throws Exception {
     HttpClient client = new HttpClient();
     client.createContext();
 
     Report reportFromJson = JsonUtility.readObjectFromFile(FULL_JSON_TXT_FILE_NAME, Report.class);
     reportFromJson.setAgentCode("F10");
     reportFromJson.setProgramCode("HIV");
     reportFromJson.getProducts().get(0).setProductCode("P10");
 
     ResponseEntity responseEntity =
       client.SendJSON(getJsonStringFor(reportFromJson),
         "http://localhost:9091/rest-api/requisitions.json",
         POST,
         "commTrack",
         "Admin123");
 
     assertEquals(400, responseEntity.getStatus());
     assertEquals("{\"error\":\"Please finish all R&R of previous period(s)\"}", responseEntity.getResponse());
   }
 
   @Test(groups = {"webservice"})
   public void testCurrentPeriodSubmitReportForParentFacility() throws Exception {
     dbWrapper.updateProgramsSupportedByField("startDate", "NOW()", "F10");
     HttpClient client = new HttpClient();
     client.createContext();
 
     Report reportFromJson = JsonUtility.readObjectFromFile(FULL_JSON_TXT_FILE_NAME, Report.class);
     reportFromJson.setAgentCode("F10");
     reportFromJson.setProgramCode("HIV");
     reportFromJson.getProducts().get(0).setProductCode("P10");
 
     ResponseEntity responseEntity =
       client.SendJSON(getJsonStringFor(reportFromJson),
         "http://localhost:9091/rest-api/requisitions.json",
         POST,
         "commTrack",
         "Admin123");
 
     assertEquals(201, responseEntity.getStatus());
     assertTrue(responseEntity.getResponse().contains("{\"requisitionId\":"));
   }
 
   @Test(groups = {"webservice"})
   public void testMultipleSubmitReportForParentFacility() throws Exception {
     dbWrapper.updateProgramsSupportedByField("startDate", "NOW()", "F10");
     HttpClient client = new HttpClient();
     client.createContext();
 
     Report reportFromJson = JsonUtility.readObjectFromFile(FULL_JSON_TXT_FILE_NAME, Report.class);
     reportFromJson.setAgentCode("F10");
     reportFromJson.setProgramCode("HIV");
     reportFromJson.getProducts().get(0).setProductCode("P10");
 
     ResponseEntity responseEntity =
       client.SendJSON(getJsonStringFor(reportFromJson),
         "http://localhost:9091/rest-api/requisitions.json",
         POST,
         "commTrack",
         "Admin123");
 
     assertEquals(201, responseEntity.getStatus());
     assertTrue(responseEntity.getResponse().contains("{\"requisitionId\":"));
 
     responseEntity =
       client.SendJSON(getJsonStringFor(reportFromJson),
         "http://localhost:9091/rest-api/requisitions.json",
         POST,
         "commTrack",
         "Admin123");
     assertEquals(400, responseEntity.getStatus());
     assertEquals("{\"error\":\"Program configuration missing\"}", responseEntity.getResponse());
 
   }
 
   @Test(groups = {"webservice"})
   public void testCommTrackSubmitReportValidRnR() throws Exception {
     dbWrapper.deleteConfigureTemplate("HIV");
     dbWrapper.configureTemplateForCommTrack("HIV");
     dbWrapper.insertPastPeriodRequisitionAndLineItems("F10", "HIV", "Period1", "P10");
     HttpClient client = new HttpClient();
     client.createContext();
 
     Report reportFromJson = readObjectFromFile("CommTrackReportJson.txt", Report.class);
     reportFromJson.setAgentCode("V10");
     reportFromJson.setProgramCode("HIV");
     reportFromJson.getProducts().get(0).setProductCode("P10");
     reportFromJson.getProducts().get(0).setBeginningBalance(10);
     reportFromJson.getProducts().get(0).setQuantityDispensed(10);
     reportFromJson.getProducts().get(0).setQuantityReceived(10);
 
     ResponseEntity responseEntity =
       client.SendJSON(getJsonStringFor(reportFromJson),
         "http://localhost:9091/rest-api/requisitions.json",
         "POST",
         "commTrack",
         "Admin123");
 
     assertEquals(201, responseEntity.getStatus());
     assertTrue(responseEntity.getResponse().contains("{\"requisitionId\":"));
   }
 
   @Test(groups = {"webservice"})
   public void testInvalidSubmitReportRnRWithoutFillingRegimen() throws Exception {
     dbWrapper.insertRegimenTemplateColumnsForProgram("HIV");
     dbWrapper.insertRegimenTemplateConfiguredForProgram("HIV", "ADULTS", "Regimen", "Regimen1", true);
     dbWrapper.setRegimenTemplateConfiguredForAllPrograms(true);
     HttpClient client = new HttpClient();
     client.createContext();
 
     Report reportFromJson = readObjectFromFile(FULL_JSON_TXT_FILE_NAME, Report.class);
     reportFromJson.setAgentCode("V10");
     reportFromJson.setProgramCode("HIV");
     reportFromJson.getProducts().get(0).setProductCode("P10");
     reportFromJson.getProducts().get(0).setBeginningBalance(10);
     reportFromJson.getProducts().get(0).setQuantityDispensed(10);
     reportFromJson.getProducts().get(0).setQuantityReceived(10);
 
     String jsonStringFor = getJsonStringFor(reportFromJson);
     ResponseEntity responseEntity = client.SendJSON(jsonStringFor,
       "http://localhost:9091/rest-api/requisitions.json",
       POST,
       "commTrack",
       "Admin123");
 
     assertEquals(400, responseEntity.getStatus());
   }
 
   @Test(groups = {"webservice"})
   public void testSubmitReportRnRWithoutFillingInactiveRegimen() throws Exception {
     dbWrapper.insertRegimenTemplateColumnsForProgram("HIV");
     dbWrapper.insertRegimenTemplateConfiguredForProgram("HIV", "ADULTS", "Regimen", "Regimen1", false);
     dbWrapper.setRegimenTemplateConfiguredForAllPrograms(true);
     HttpClient client = new HttpClient();
     client.createContext();
 
     Report reportFromJson = readObjectFromFile(FULL_JSON_TXT_FILE_NAME, Report.class);
     reportFromJson.setAgentCode("V10");
     reportFromJson.setProgramCode("HIV");
     reportFromJson.getProducts().get(0).setProductCode("P10");
     reportFromJson.getProducts().get(0).setBeginningBalance(10);
     reportFromJson.getProducts().get(0).setQuantityDispensed(10);
     reportFromJson.getProducts().get(0).setQuantityReceived(10);
 
     String jsonStringFor = getJsonStringFor(reportFromJson);
     ResponseEntity responseEntity = client.SendJSON(jsonStringFor,
       "http://localhost:9091/rest-api/requisitions.json",
       POST,
       "commTrack",
       "Admin123");
 
     assertEquals(201, responseEntity.getStatus());
     assertTrue(responseEntity.getResponse().contains("{\"requisitionId\":"));
   }
 
   @Test(groups = {"webservice"})
   public void testSubmitReportRnRWithRegimen() throws Exception {
     dbWrapper.insertRegimenTemplateColumnsForProgram("HIV");
     dbWrapper.insertRegimenTemplateConfiguredForProgram("HIV", "ADULTS", "Regimen", "Regimen1", true);
     dbWrapper.setRegimenTemplateConfiguredForAllPrograms(true);
     HttpClient client = new HttpClient();
     client.createContext();
 
     String regimenJson = "{\"agentCode\": \"V10\"," +
       "    \"programCode\": \"HIV\"," +
       "    \"products\": [" +
       "        {" +
       "            \"productCode\": \"P10\"," +
       "             \"beginningBalance\": \"3\"," +
       "              \"quantityDispensed\": \"1\"," +
       "               \"quantityReceived\": \"0\"" +
       "        }" +
       "    ]," +
       "\"regimens\" : [  " +
       "{ " +
       "\"code\" : \"Regimen\"," +
       "\"name\" :\"Regimen1\"," +
       "\"patientsOnTreatment\":\"111\"," +
       "\"patientsToInitiateTreatment\" :\"12\"," +
       "\"patientsStoppedTreatment\" :\"12\"" +
       "}]}";
 
     ResponseEntity responseEntity = client.SendJSON(regimenJson,
       "http://localhost:9091/rest-api/requisitions.json",
       POST,
       "commTrack",
       "Admin123");
 
     assertEquals(201, responseEntity.getStatus());
     assertTrue(responseEntity.getResponse().contains("{\"requisitionId\":"));
   }
 
   @Test(groups = {"webservice"})
   public void testSubmitReportRnRWithRegimenHavingExtraFields() throws Exception {
     dbWrapper.insertRegimenTemplateColumnsForProgram("HIV");
     dbWrapper.insertRegimenTemplateConfiguredForProgram("HIV", "ADULTS", "Regimen", "Regimen1", true);
     dbWrapper.setRegimenTemplateConfiguredForAllPrograms(true);
     HttpClient client = new HttpClient();
     client.createContext();
 
     String regimenJson = "{\"agentCode\": \"V10\"," +
       "    \"programCode\": \"HIV\"," +
       "    \"products\": [" +
       "        {" +
       "            \"productCode\": \"P10\"," +
       "             \"beginningBalance\": \"3\"," +
       "              \"quantityDispensed\": \"1\"," +
       "               \"quantityReceived\": \"0\"" +
       "        }" +
       "    ]," +
       "\"regimens\" : [  " +
       "{ " +
       "\"code\" : \"Regimen\"," +
       "\"name\" :\"Regimen1\"," +
       "\"patientsOnTreatment\":\"111\"," +
       "\"patientsToInitiateTreatment\" :\"12\"," +
       "\"patients\" :\"12\"," +
       "\"patientsStoppedTreatment\" :\"12\"" +
       "}]}";
 
     ResponseEntity responseEntity = client.SendJSON(regimenJson,
       "http://localhost:9091/rest-api/requisitions.json",
       POST,
       "commTrack",
       "Admin123");
 
     assertEquals(400, responseEntity.getStatus());
     assertTrue(responseEntity.getResponse().contains("Unrecognized field"));
   }
 
   @Test(groups = {"webservice"})
   public void testSubmitReportRnRWithRegimenMissingMandatoryFields() throws Exception {
     dbWrapper.insertRegimenTemplateColumnsForProgram("HIV");
     dbWrapper.insertRegimenTemplateConfiguredForProgram("HIV", "ADULTS", "Regimen", "Regimen1", true);
     dbWrapper.setRegimenTemplateConfiguredForAllPrograms(true);
     HttpClient client = new HttpClient();
     client.createContext();
 
     String regimenJson = "{\"agentCode\": \"V10\"," +
       "    \"programCode\": \"HIV\"," +
       "    \"products\": [" +
       "        {" +
       "            \"productCode\": \"P10\"," +
       "             \"beginningBalance\": \"3\"," +
       "              \"quantityDispensed\": \"1\"," +
       "               \"quantityReceived\": \"0\"" +
       "        }" +
       "    ]," +
       "\"regimens\" : [  " +
       "{ " +
       "\"code\" : \"Regimen\"," +
       "\"name\" :\"Regimen1\"," +
       "\"patientsOnTreatment\":\"111\"," +
       "\"patientsStoppedTreatment\" :\"12\"" +
       "}]}";
 
     ResponseEntity responseEntity = client.SendJSON(regimenJson,
       "http://localhost:9091/rest-api/requisitions.json",
       POST,
       "commTrack",
       "Admin123");
 
     assertEquals(400, responseEntity.getStatus());
     assertTrue(responseEntity.getResponse().contains("{\"error\":\"R&R has errors, please correct them to proceed.\"}"));
   }
 
   @Test(groups = {"webservice"})
   public void testInvalidSubmitReportRnRWithExtraRegimenLineItem() throws Exception {
     dbWrapper.insertRegimenTemplateColumnsForProgram("HIV");
     dbWrapper.insertRegimenTemplateConfiguredForProgram("HIV", "ADULTS", "Regimen", "Regimen1", true);
     dbWrapper.setRegimenTemplateConfiguredForAllPrograms(true);
     HttpClient client = new HttpClient();
     client.createContext();
 
     String regimenJson = "{\"agentCode\": \"V10\"," +
       "    \"programCode\": \"HIV\"," +
       "    \"products\": [" +
       "        {" +
       "            \"productCode\": \"P10\"," +
       "             \"beginningBalance\": \"3\"," +
       "              \"quantityDispensed\": \"1\"," +
       "               \"quantityReceived\": \"0\"" +
       "        }" +
       "    ]," +
       "\"regimens\" : [  " +
       "{ " +
       "\"code\" : \"Regimen\"," +
       "\"name\" :\"Regimen1\"," +
       "\"patientsOnTreatment\":\"111\"," +
       "\"patientsToInitiateTreatment\" :\"12\"," +
       "\"patientsStoppedTreatment\" :\"12\"" +
       "}" +
       "]," +
       "\"regimens\" : [  " +
       "{ " +
       "\"code\" : \"Regimen2\"," +
       "\"name\" :\"Regimen12\"," +
       "\"patientsOnTreatment\":\"111\"," +
       "\"patientsToInitiateTreatment\" :\"12\"," +
       "\"patientsStoppedTreatment\" :\"12\"" +
       "}" +
       "]" +
       "}";
 
     ResponseEntity responseEntity = client.SendJSON(regimenJson,
       "http://localhost:9091/rest-api/requisitions.json",
       POST,
       "commTrack",
       "Admin123");
 
     assertEquals(400, responseEntity.getStatus());
     assertEquals("{\"error\":\"Invalid regimen found\"}", responseEntity.getResponse());
   }
 
   @Test(groups = {"webservice"})
   public void testInvalidSubmitReportRnRWithLessRegimenLineItems() throws Exception {
     dbWrapper.insertRegimenTemplateColumnsForProgram("HIV");
     dbWrapper.insertRegimenTemplateConfiguredForProgram("HIV", "ADULTS", "Regimen", "Regimen1", true);
     dbWrapper.insertRegimenTemplateConfiguredForProgram("HIV", "ADULTS", "Regimen2", "Regimen12", true);
     dbWrapper.setRegimenTemplateConfiguredForAllPrograms(true);
     HttpClient client = new HttpClient();
     client.createContext();
 
     String regimenJson = "{\"agentCode\": \"V10\"," +
       "    \"programCode\": \"HIV\"," +
       "    \"products\": [" +
       "        {" +
       "            \"productCode\": \"P10\"," +
       "             \"beginningBalance\": \"3\"," +
       "              \"quantityDispensed\": \"1\"," +
       "               \"quantityReceived\": \"0\"" +
       "        }" +
       "    ]," +
       "\"regimens\" : [  " +
       "{ " +
       "\"code\" : \"Regimen\"," +
       "\"name\" :\"Regimen1\"," +
       "\"patientsOnTreatment\":\"111\"," +
       "\"patientsToInitiateTreatment\" :\"12\"," +
       "\"patientsStoppedTreatment\" :\"12\"" +
       "}]}";
 
     ResponseEntity responseEntity = client.SendJSON(regimenJson,
       "http://localhost:9091/rest-api/requisitions.json",
       POST,
       "commTrack",
       "Admin123");
 
     assertEquals(400, responseEntity.getStatus());
     assertEquals("{\"error\":\"R&R has errors, please correct them to proceed.\"}", responseEntity.getResponse());
 
   }
 
   @Test(groups = {"webservice"})
   public void testMasterTemplateValidationMissingBothMandatoryUserInputFields() throws Exception {
     HttpClient client = new HttpClient();
     client.createContext();
     dbWrapper.updateConfigureTemplate("HIV", "source", "U", "true", "stockInHand");
     dbWrapper.updateConfigureTemplate("HIV", "source", "U", "true", "quantityDispensed");
     Report reportFromJson = JsonUtility.readObjectFromFile(MINIMUM_JSON_TXT_FILE_NAME, Report.class);
     reportFromJson.setAgentCode("V10");
     reportFromJson.setProgramCode("HIV");
     reportFromJson.getProducts().get(0).setProductCode("P10");
     reportFromJson.getProducts().get(0).setBeginningBalance(1);
     reportFromJson.getProducts().get(0).setQuantityReceived(4);
     reportFromJson.getProducts().get(0).setQuantityDispensed(null);
     reportFromJson.getProducts().get(0).setStockInHand(null);
     ResponseEntity responseEntity =
       client.SendJSON(getJsonStringFor(reportFromJson),
         "http://localhost:9091/rest-api/requisitions.json",
         POST,
         "commTrack",
         "Admin123");
 
     assertEquals(400, responseEntity.getStatus());
     assertEquals("{\"error\":\"R&R has errors, please correct them to proceed.\"}", responseEntity.getResponse());
   }
 
   @Test(groups = {"webservice"})
   public void testMasterTemplateValidationMissingOneMandatoryUserInputFields() throws Exception {
     HttpClient client = new HttpClient();
     client.createContext();
     dbWrapper.updateConfigureTemplate("HIV", "source", "U", "true", "stockInHand");
     dbWrapper.updateConfigureTemplate("HIV", "source", "U", "true", "quantityDispensed");
     Report reportFromJson = JsonUtility.readObjectFromFile(MINIMUM_JSON_TXT_FILE_NAME, Report.class);
     reportFromJson.setAgentCode("V10");
     reportFromJson.setProgramCode("HIV");
     reportFromJson.getProducts().get(0).setProductCode("P10");
     reportFromJson.getProducts().get(0).setBeginningBalance(1);
     reportFromJson.getProducts().get(0).setQuantityReceived(4);
     reportFromJson.getProducts().get(0).setStockInHand(null);
     reportFromJson.getProducts().get(0).setQuantityDispensed(1);
 
     ResponseEntity responseEntity =
       client.SendJSON(getJsonStringFor(reportFromJson),
         "http://localhost:9091/rest-api/requisitions.json",
         POST,
         "commTrack",
         "Admin123");
 
     assertEquals(400, responseEntity.getStatus());
     assertEquals("{\"error\":\"R&R has errors, please correct them to proceed.\"}", responseEntity.getResponse());
   }
 
   @Test(groups = {"webservice"})
   public void testMasterTemplateValidationViolateArithmeticValidation() throws Exception {
     HttpClient client = new HttpClient();
     client.createContext();
     dbWrapper.updateConfigureTemplate("HIV", "source", "U", "true", "stockInHand");
     dbWrapper.updateConfigureTemplateValidationFlag("HIV", "true");
     dbWrapper.updateConfigureTemplate("HIV", "source", "U", "true", "quantityDispensed");
     Report reportFromJson = JsonUtility.readObjectFromFile(MINIMUM_JSON_TXT_FILE_NAME, Report.class);
     reportFromJson.setAgentCode("V10");
     reportFromJson.setProgramCode("HIV");
     reportFromJson.getProducts().get(0).setProductCode("P10");
     reportFromJson.getProducts().get(0).setBeginningBalance(1);
     reportFromJson.getProducts().get(0).setQuantityReceived(4);
     reportFromJson.getProducts().get(0).setQuantityDispensed(1);
     reportFromJson.getProducts().get(0).setStockInHand(10);
 
     ResponseEntity responseEntity =
       client.SendJSON(getJsonStringFor(reportFromJson),
         "http://localhost:9091/rest-api/requisitions.json",
         POST,
         "commTrack",
         "Admin123");
     assertEquals(400, responseEntity.getStatus());
     assertEquals("{\"error\":\"R&R has errors, please correct them to proceed.\"}", responseEntity.getResponse());
   }
 
 
   @Test(groups = {"webservice"})
   public void testMasterTemplateValidationOverrideWithCalculatedValue() throws Exception {
     long id = submitRnRThroughApi("V10", "HIV", "P10", 1, 10, 1, 4, 0, 2);
     assertEquals("4", dbWrapper.getRequisitionLineItemFieldValue(id, "stockInHand", "P10"));
   }
 
 
   @Test(groups = {"webservice"})
   public void testMasterTemplateValidationIgnoreReportedValueWhenFieldNotVisible() throws Exception {
     dbWrapper.updateConfigureTemplate("HIV", "source", "C", "false", "stockInHand");
     dbWrapper.updateConfigureTemplate("HIV", "source", "C", "false", "beginningBalance");
     Long id = submitRnRThroughApi("V10", "HIV", "P10", 1, 10, 1, 4, 0, 2);
     assertEquals("0", dbWrapper.getRequisitionLineItemFieldValue(id, "beginningBalance", "P10"));
   }
 
 
   @Test(groups = {"webservice"})
   public void testShowErrorMessageForUnrecognizedFieldInAPI() throws Exception {
     HttpClient client = new HttpClient();
     client.createContext();
 
     String wrongJson = "{\"agentCode\": \"V10\"," +
       "    \"programCode\": \"HIV\"," +
       "    \"products\": [" +
       "        {" +
       "            \"productCode\": \"P10\"," +
       "            \"zzzzzbbbb\": \"10\"," +
 
       "        }" +
       "    ]" +
       "}";
 
     ResponseEntity responseEntity =
       client.SendJSON(wrongJson,
         "http://localhost:9091/rest-api/requisitions.json",
         POST,
         "commTrack",
         "Admin123");
 
     assertEquals(400, responseEntity.getStatus());
     assertTrue(responseEntity.getResponse().contains("{\"error\":\"Could not read JSON: Unrecognized field"));
   }
 
   @Test(groups = {"webservice"})
   public void testSubmitRnrWithReferenceData() throws Exception {
     HttpClient client = new HttpClient();
     client.createContext();
 
     String wrongJson = "{\"agentCode\": \"V10\"," +
       "    \"programCode\": \"HIV\"," +
       "    \"products\": [" +
       "        {" +
       "            \"productCode\": \"P10\"," +
       "            \"productName\": \"abc\"," +
 
       "        }" +
       "    ]" +
       "}";
 
     ResponseEntity responseEntity =
       client.SendJSON(wrongJson,
         "http://localhost:9091/rest-api/requisitions.json",
         POST,
         "commTrack",
         "Admin123");
 
     assertEquals(400, responseEntity.getStatus());
     assertTrue(responseEntity.getResponse().contains("{\"error\":\"Could not read JSON: Unrecognized field"));
   }
 }
 
