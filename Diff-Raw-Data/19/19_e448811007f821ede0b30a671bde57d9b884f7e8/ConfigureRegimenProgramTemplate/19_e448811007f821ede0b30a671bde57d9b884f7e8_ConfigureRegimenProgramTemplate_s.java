 /*
  * Copyright © 2013 VillageReach.  All Rights Reserved.  This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
  *
  * If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
  */
 
 package org.openlmis.functional;
 
 
 import com.thoughtworks.selenium.SeleneseTestNgHelper;
 import org.openlmis.UiUtils.CaptureScreenshotOnFailureListener;
 import org.openlmis.UiUtils.TestCaseHelper;
 import org.openlmis.pageobjects.*;
 import org.springframework.test.context.transaction.TransactionConfiguration;
 import org.springframework.transaction.annotation.Transactional;
 import org.testng.annotations.*;
 
 import java.io.IOException;
 import java.sql.SQLException;
 import java.util.ArrayList;
 import java.util.List;
 
 import static com.thoughtworks.selenium.SeleneseTestBase.assertEquals;
 import static com.thoughtworks.selenium.SeleneseTestBase.assertTrue;
 
 @TransactionConfiguration(defaultRollback = true)
 @Transactional
 
 @Listeners(CaptureScreenshotOnFailureListener.class)
 
 public class ConfigureRegimenProgramTemplate extends TestCaseHelper {
 
   private static String adultsRegimen = "Adults";
   private static String paediatricsRegimen = "Paediatrics";
   private static String duplicateErrorMessageSave = "Cannot add duplicate regimen code for same program";
   private static String requiredErrorMessageSave = "Please fill required values";
   private static String errorMessageONSaveBeforeDone = "Mark all regimens as 'Done' before saving the form";
   private static String oneShouldBeSelectedErrorMessage = "At least one column should be checked";
   private static String baseRegimenDivXpath = "//div[@id='sortable']/div";
   private static String CODE1 = "Code1";
   private static String CODE2 = "Code2";
   private static String NAME1 = "Name1";
   private static String NAME2 = "Name2";
 
   @BeforeMethod(groups = {"functional2", "smoke"})
   public void setUp() throws Exception {
     super.setup();
   }
 
 
   @Test(groups = {"smoke"}, dataProvider = "Data-Provider")
   public void testVerifyNewRegimenCreated(String program, String[] credentials) throws Exception {
     dbWrapper.setRegimenTemplateConfiguredForAllPrograms(false);
     String expectedProgramsString = dbWrapper.getAllActivePrograms();
     LoginPage loginPage = new LoginPage(testWebDriver, baseUrlGlobal);
     HomePage homePage = loginPage.loginAs(credentials[0], credentials[1]);
     RegimenTemplateConfigPage regimenTemplateConfigPage = homePage.navigateToRegimenConfigTemplate();
     List<String> programsList = getProgramsListedOnRegimeScreen();
     verifyProgramsListedOnManageRegimenTemplateScreen(programsList, expectedProgramsString);
     regimenTemplateConfigPage.configureProgram(program);
     regimenTemplateConfigPage.AddNewRegimen(adultsRegimen, CODE1, NAME1, true);
     regimenTemplateConfigPage.SaveRegime();
     verifySuccessMessage(regimenTemplateConfigPage);
     verifyProgramConfigured(program);
   }
 
   @Test(groups = {"smoke"}, dataProvider = "Data-Provider")
   public void testVerifyNewRegimenReportingFieldConfiguration(String program, String[] credentials) throws Exception {
     String newRemarksHeading = "Testing column";
     String newCodeHeading = "Testing code";
 
     dbWrapper.setRegimenTemplateConfiguredForAllPrograms(false);
     LoginPage loginPage = new LoginPage(testWebDriver, baseUrlGlobal);
     HomePage homePage = loginPage.loginAs(credentials[0], credentials[1]);
     RegimenTemplateConfigPage regimenTemplateConfigPage = homePage.navigateToRegimenConfigTemplate();
     regimenTemplateConfigPage.configureProgram(program);
     regimenTemplateConfigPage.AddNewRegimen(adultsRegimen, CODE1, NAME1, true);
     regimenTemplateConfigPage.clickReportingFieldTab();
     verifyDefaultRegimenReportingFieldsValues(regimenTemplateConfigPage);
     regimenTemplateConfigPage.NoOfPatientsOnTreatmentCheckBox(false);
     regimenTemplateConfigPage.setValueRemarksTextField(newRemarksHeading);
    regimenTemplateConfigPage.setValueCodeTextField(newCodeHeading);
     regimenTemplateConfigPage.SaveRegime();
     verifySuccessMessage(regimenTemplateConfigPage);
     verifyProgramConfigured(program);
 
     regimenTemplateConfigPage.clickEditProgram(program);
     verifyProgramDetailsSaved(adultsRegimen, CODE1, NAME1, newRemarksHeading, newCodeHeading);
 
     regimenTemplateConfigPage.NoOfPatientsOnTreatmentCheckBox(true);
     regimenTemplateConfigPage.SaveRegime();
     verifySuccessMessage(regimenTemplateConfigPage);
   }
 
   @Test(groups = {"functional"}, dataProvider = "Data-Provider")
   public void testVerifyAtLeastOneColumnChecked(String program, String[] credentials) throws Exception {
 
     dbWrapper.setRegimenTemplateConfiguredForAllPrograms(false);
     LoginPage loginPage = new LoginPage(testWebDriver, baseUrlGlobal);
     HomePage homePage = loginPage.loginAs(credentials[0], credentials[1]);
     RegimenTemplateConfigPage regimenTemplateConfigPage = homePage.navigateToRegimenConfigTemplate();
     regimenTemplateConfigPage.configureProgram(program);
     regimenTemplateConfigPage.clickReportingFieldTab();
     verifyDefaultRegimenReportingFieldsValues(regimenTemplateConfigPage);
     regimenTemplateConfigPage.NoOfPatientsOnTreatmentCheckBox(false);
     regimenTemplateConfigPage.NoOfPatientsStoppedTreatmentCheckBox(false);
     regimenTemplateConfigPage.NoOfPatientsToInitiateTreatmentCheckBox(false);
     regimenTemplateConfigPage.RemarksCheckBox(false);
 
     regimenTemplateConfigPage.SaveRegime();
     regimenTemplateConfigPage.verifySaveErrorMessageDiv(oneShouldBeSelectedErrorMessage);
 
   }
 
   @Test(groups = {"functional"}, dataProvider = "Data-Provider-Function-Positive")
   public void testVerifyAlteredRegimensColumnsOnRnRScreen(String program,String adminUser, String userSIC, String categoryCode, String password, String regimenCode, String regimenName, String regimenCode2, String regimenName2) throws Exception {
     String newRemarksHeading = "Testing column";
 
     dbWrapper.setRegimenTemplateConfiguredForAllPrograms(false);
     LoginPage loginPage = new LoginPage(testWebDriver, baseUrlGlobal);
     HomePage homePage = loginPage.loginAs(adminUser, adminUser);
     RegimenTemplateConfigPage regimenTemplateConfigPage = homePage.navigateToRegimenConfigTemplate();
     regimenTemplateConfigPage.configureProgram(program);
     regimenTemplateConfigPage.AddNewRegimen(adultsRegimen, CODE1, NAME1, false);
     regimenTemplateConfigPage.clickReportingFieldTab();
     regimenTemplateConfigPage.setValueRemarksTextField(newRemarksHeading);
     regimenTemplateConfigPage.SaveRegime();
     verifySuccessMessage(regimenTemplateConfigPage);
     homePage.logout(baseUrlGlobal);
     setUpDataForInitiateRnR(program, userSIC);
 
     LoginPage loginPage1 = new LoginPage(testWebDriver, baseUrlGlobal);
     HomePage homePage1 = loginPage1.loginAs(userSIC, password);
     homePage1.navigateAndInitiateRnr(program);
     InitiateRnRPage initiateRnRPage = homePage1.clickProceed();
 
     testWebDriver.sleep(2000);
     initiateRnRPage.clickRegimenTab();
     testWebDriver.sleep(500);
     String tableXpathTillTr = "//table[@id='regimenTable']/thead/tr";
     int columns = initiateRnRPage.getSizeOfElements(tableXpathTillTr + "/th");
     initiateRnRPage.verifyColumnsHeadingPresent(tableXpathTillTr, "Code", columns);
     initiateRnRPage.verifyColumnsHeadingPresent(tableXpathTillTr, "Name", columns);
     initiateRnRPage.verifyColumnsHeadingPresent(tableXpathTillTr, "Number of patients on treatment", columns);
     initiateRnRPage.verifyColumnsHeadingPresent(tableXpathTillTr, "Number of patients to be initiated treatment", columns);
     initiateRnRPage.verifyColumnsHeadingPresent(tableXpathTillTr, "Number of patients stopped treatment", columns);
     initiateRnRPage.verifyColumnsHeadingPresent(tableXpathTillTr, newRemarksHeading, columns);
 
   }
 
 
   @Test(groups = {"functional2"}, dataProvider = "Data-Provider")
   public void testVerifyMultipleCategoriesAddition(String program, String[] credentials) throws Exception {
     dbWrapper.setRegimenTemplateConfiguredForAllPrograms(false);
     LoginPage loginPage = new LoginPage(testWebDriver, baseUrlGlobal);
     HomePage homePage = loginPage.loginAs(credentials[0], credentials[1]);
     RegimenTemplateConfigPage regimenTemplateConfigPage = homePage.navigateToRegimenConfigTemplate();
     regimenTemplateConfigPage.configureProgram(program);
     regimenTemplateConfigPage.AddNewRegimen(adultsRegimen, CODE1, NAME1, true);
     regimenTemplateConfigPage.AddNewRegimen(paediatricsRegimen, CODE2, NAME1, true);
     regimenTemplateConfigPage.SaveRegime();
     verifySuccessMessage(regimenTemplateConfigPage);
     verifyProgramConfigured(program);
   }
 
   @Test(groups = {"functional2"}, dataProvider = "Data-Provider")
   public void testVerifyDuplicateCategoriesInterCategory(String program, String[] credentials) throws Exception {
     dbWrapper.setRegimenTemplateConfiguredForAllPrograms(false);
     LoginPage loginPage = new LoginPage(testWebDriver, baseUrlGlobal);
     HomePage homePage = loginPage.loginAs(credentials[0], credentials[1]);
     RegimenTemplateConfigPage regimenTemplateConfigPage = homePage.navigateToRegimenConfigTemplate();
     regimenTemplateConfigPage.configureProgram(program);
     regimenTemplateConfigPage.AddNewRegimen(adultsRegimen, CODE1, NAME1, true);
     regimenTemplateConfigPage.AddNewRegimen(paediatricsRegimen, CODE1, NAME2, true);
     verifyErrorMessage(regimenTemplateConfigPage, duplicateErrorMessageSave);
   }
 
 
   @Test(groups = {"functional2"}, dataProvider = "Data-Provider")
   public void testVerifyDuplicateCategoriesAdditionForSameCategory(String program, String[] credentials) throws Exception {
     dbWrapper.setRegimenTemplateConfiguredForAllPrograms(false);
     LoginPage loginPage = new LoginPage(testWebDriver, baseUrlGlobal);
     HomePage homePage = loginPage.loginAs(credentials[0], credentials[1]);
     RegimenTemplateConfigPage regimenTemplateConfigPage = homePage.navigateToRegimenConfigTemplate();
     regimenTemplateConfigPage.configureProgram(program);
     regimenTemplateConfigPage.AddNewRegimen(adultsRegimen, CODE1, NAME1, true);
     regimenTemplateConfigPage.AddNewRegimen(adultsRegimen, CODE1, NAME2, true);
     verifyErrorMessage(regimenTemplateConfigPage, duplicateErrorMessageSave);
   }
 
   @Test(groups = {"functional2"}, dataProvider = "Data-Provider-Multiple-Programs")
   public void testVerifyDuplicateCategoriesInterPrograms(String program1, String program2, String[] credentials) throws Exception {
     dbWrapper.setRegimenTemplateConfiguredForAllPrograms(false);
     LoginPage loginPage = new LoginPage(testWebDriver, baseUrlGlobal);
     HomePage homePage = loginPage.loginAs(credentials[0], credentials[1]);
     RegimenTemplateConfigPage regimenTemplateConfigPage = homePage.navigateToRegimenConfigTemplate();
     regimenTemplateConfigPage.configureProgram(program1);
     regimenTemplateConfigPage.AddNewRegimen(adultsRegimen, CODE1, NAME1, true);
     regimenTemplateConfigPage.SaveRegime();
     verifySuccessMessage(regimenTemplateConfigPage);
     verifyProgramConfigured(program1);
 
     regimenTemplateConfigPage.configureProgram(program2);
     regimenTemplateConfigPage.AddNewRegimen(adultsRegimen, CODE1, NAME1, true);
     regimenTemplateConfigPage.SaveRegime();
     verifySuccessMessage(regimenTemplateConfigPage);
     verifyProgramConfigured(program2);
   }
 
   @Test(groups = {"functional2"}, dataProvider = "Data-Provider")
   public void testVerifyEditCategory(String program, String[] credentials) throws Exception {
     dbWrapper.setRegimenTemplateConfiguredForAllPrograms(false);
     LoginPage loginPage = new LoginPage(testWebDriver, baseUrlGlobal);
     HomePage homePage = loginPage.loginAs(credentials[0], credentials[1]);
     RegimenTemplateConfigPage regimenTemplateConfigPage = homePage.navigateToRegimenConfigTemplate();
     regimenTemplateConfigPage.configureProgram(program);
     regimenTemplateConfigPage.AddNewRegimen(adultsRegimen, CODE1, NAME1, false);
     regimenTemplateConfigPage.SaveRegime();
     regimenTemplateConfigPage.clickEditProgram(program);
     verifyNonEditableRegimenAdded(CODE1, NAME1, true, 1);
     regimenTemplateConfigPage.clickEditButton();
     verifyEditableRegimenAdded(CODE1, NAME1, true, 1);
     enterCategoriesValuesForEditing(CODE2, NAME2, 1);
     regimenTemplateConfigPage.clickDoneButton();
     verifyNonEditableRegimenAdded(CODE2, NAME2, true, 1);
     regimenTemplateConfigPage.SaveRegime();
     verifySuccessMessage(regimenTemplateConfigPage);
     regimenTemplateConfigPage.clickEditProgram(program);
     verifyNonEditableRegimenAdded(CODE2, NAME2, true, 1);
     regimenTemplateConfigPage.SaveRegime();
     verifySuccessMessage(regimenTemplateConfigPage);
     verifyProgramConfigured(program);
   }
 
   @Test(groups = {"functional2"}, dataProvider = "Data-Provider")
   public void testVerifyDuplicateCategoryOnDone(String program, String[] credentials) throws Exception {
     dbWrapper.setRegimenTemplateConfiguredForAllPrograms(false);
     LoginPage loginPage = new LoginPage(testWebDriver, baseUrlGlobal);
     HomePage homePage = loginPage.loginAs(credentials[0], credentials[1]);
     RegimenTemplateConfigPage regimenTemplateConfigPage = homePage.navigateToRegimenConfigTemplate();
     regimenTemplateConfigPage.configureProgram(program);
     regimenTemplateConfigPage.AddNewRegimen(adultsRegimen, CODE1, NAME1, false);
     regimenTemplateConfigPage.AddNewRegimen(adultsRegimen, CODE2, NAME1, true);
     verifyNonEditableRegimenAdded(CODE1, NAME1, true, 1);
     verifyNonEditableRegimenAdded(CODE2, NAME1, false, 2);
     regimenTemplateConfigPage.clickEditButton();
     enterCategoriesValuesForEditing(CODE2, NAME1, 1);
     regimenTemplateConfigPage.clickDoneButton();
     verifyErrorMessage(regimenTemplateConfigPage, duplicateErrorMessageSave);
   }
 
   @Test(groups = {"functional2"}, dataProvider = "Data-Provider")
   public void testVerifyCategoryErrorOnDone(String program, String[] credentials) throws Exception {
     dbWrapper.setRegimenTemplateConfiguredForAllPrograms(false);
     LoginPage loginPage = new LoginPage(testWebDriver, baseUrlGlobal);
     HomePage homePage = loginPage.loginAs(credentials[0], credentials[1]);
     RegimenTemplateConfigPage regimenTemplateConfigPage = homePage.navigateToRegimenConfigTemplate();
     regimenTemplateConfigPage.configureProgram(program);
     regimenTemplateConfigPage.AddNewRegimen(adultsRegimen, CODE1, NAME1, true);
     regimenTemplateConfigPage.AddNewRegimen(adultsRegimen, CODE2, NAME1, true);
     regimenTemplateConfigPage.clickEditButton();
     regimenTemplateConfigPage.clickSaveButton();
     verifyErrorMessage(regimenTemplateConfigPage, errorMessageONSaveBeforeDone);
     enterCategoriesValuesForEditing("", NAME1, 1);
     regimenTemplateConfigPage.clickDoneButton();
     verifyDoneErrorMessage(regimenTemplateConfigPage, requiredErrorMessageSave);
   }
 
   @Test(groups = {"functional2"}, dataProvider = "Data-Provider")
   public void testVerifyCancelButtonFunctionality(String program, String[] credentials) throws Exception {
     dbWrapper.setRegimenTemplateConfiguredForAllPrograms(false);
     LoginPage loginPage = new LoginPage(testWebDriver, baseUrlGlobal);
     HomePage homePage = loginPage.loginAs(credentials[0], credentials[1]);
     RegimenTemplateConfigPage regimenTemplateConfigPage = homePage.navigateToRegimenConfigTemplate();
     regimenTemplateConfigPage.configureProgram(program);
     regimenTemplateConfigPage.CancelRegime(program);
     assertTrue("Clicking Cancel button should be redirected to Regimen Template screen", testWebDriver.getElementById(program).isDisplayed());
   }
 
   private void verifyDefaultRegimenReportingFieldsValues(RegimenTemplateConfigPage regimenTemplateConfigPage) {
     assertTrue("noOfPatientsOnTreatmentCheckBox should be checked", regimenTemplateConfigPage.IsSelectedNoOfPatientsOnTreatmentCheckBox());
     assertTrue("noOfPatientsToInitiateTreatmentCheckBox should be checked", regimenTemplateConfigPage.IsNoOfPatientsToInitiateTreatmentCheckBoxSelected());
     assertTrue("noOfPatientsStoppedTreatmentCheckBox should be checked", regimenTemplateConfigPage.IsNoOfPatientsStoppedTreatmentCheckBoxSelected());
     assertTrue("remarksCheckBox should be checked", regimenTemplateConfigPage.IsRemarksCheckBoxSelected());
    assertTrue("Code checkbox should always  be checked", regimenTemplateConfigPage.getCodeOKIcon().isDisplayed());
    assertTrue("Name checkbox should always  be checked", regimenTemplateConfigPage.getNameOKIcon().isDisplayed());
 
     assertEquals("Number of patients on treatment", regimenTemplateConfigPage.getValueNoOfPatientsOnTreatmentTextField());
     assertEquals("Number of patients to be initiated treatment", regimenTemplateConfigPage.getValueNoOfPatientsToInitiateTreatmentTextField());
     assertEquals("Number of patients stopped treatment", regimenTemplateConfigPage.getValueNoOfPatientsStoppedTreatmentTextField());
     assertEquals("Remarks", regimenTemplateConfigPage.getValueRemarksTextField());
    assertEquals("Code", regimenTemplateConfigPage.getValueCodeTextField());
    assertEquals("Name", regimenTemplateConfigPage.getValueNameTextField());
 
     assertEquals("Numeric", regimenTemplateConfigPage.getTextNoOfPatientsOnTreatmentDataType());
     assertEquals("Numeric", regimenTemplateConfigPage.getTextNoOfPatientsStoppedTreatmentDataType());
     assertEquals("Numeric", regimenTemplateConfigPage.getTextNoOfPatientsToInitiateTreatmentDataType());
     assertEquals("Text", regimenTemplateConfigPage.getTextRemarksDataType());
    assertEquals("Text", regimenTemplateConfigPage.getTextCodeDataType());
    assertEquals("Text", regimenTemplateConfigPage.getTextNameDataType());
 
   }
 
   private void verifyErrorMessage(RegimenTemplateConfigPage regimenTemplateConfigPage, String expectedErrorMessage) {
     regimenTemplateConfigPage.IsDisplayedSaveErrorMsgDiv();
     assertEquals(expectedErrorMessage, regimenTemplateConfigPage.getSaveErrorMsgDiv());
   }
 
   private void verifyDoneErrorMessage(RegimenTemplateConfigPage regimenTemplateConfigPage, String expectedErrorMessage) {
 
     assertTrue("Done regimen Error div should show up", regimenTemplateConfigPage.IsDisplayedDoneFailMessage());
   }
 
   private void verifyNonEditableRegimenAdded(String code, String name, boolean activeChecboxSelected, int indexOfCodeAdded) {
     RegimenTemplateConfigPage regimenTemplateConfigPage = new RegimenTemplateConfigPage(testWebDriver);
     assertEquals(code, regimenTemplateConfigPage.getNonEditableAddedCode(indexOfCodeAdded));
     assertEquals(name, regimenTemplateConfigPage.getNonEditableAddedName(indexOfCodeAdded));
     assertEquals(activeChecboxSelected, regimenTemplateConfigPage.getNonEditableAddedActiveCheckBox(indexOfCodeAdded));
 
   }
 
   private void verifyEditableRegimenAdded(String code, String name, boolean activeChecboxSelected, int indexOfCodeAdded) {
     RegimenTemplateConfigPage regimenTemplateConfigPage = new RegimenTemplateConfigPage(testWebDriver);
     assertEquals(code, regimenTemplateConfigPage.getEditableAddedCode(indexOfCodeAdded));
     assertEquals(name, regimenTemplateConfigPage.getEditableAddedName(indexOfCodeAdded));
     assertEquals(activeChecboxSelected, regimenTemplateConfigPage.getEditableAddedActiveCheckBox(indexOfCodeAdded));
   }
 
   private void enterCategoriesValuesForEditing(String code, String name, int indexOfCodeAdded) {
     testWebDriver.waitForElementToAppear(testWebDriver.getElementByXpath(baseRegimenDivXpath + "[" + indexOfCodeAdded + "]/div[2]/input"));
     sendKeys(baseRegimenDivXpath + "[" + indexOfCodeAdded + "]/div[2]/input", code);
     sendKeys(baseRegimenDivXpath + "[" + indexOfCodeAdded + "]/div[3]/input", name);
   }
 
 
   private void verifyProgramsListedOnManageRegimenTemplateScreen(List<String> actualProgramsString, String expectedProgramsString) {
     for (String program : actualProgramsString)
       SeleneseTestNgHelper.assertTrue("Program " + program + " not present in expected string : " + expectedProgramsString, expectedProgramsString.contains(program));
 
   }
 
 
   private List<String> getProgramsListedOnRegimeScreen() {
     List<String> programsList = new ArrayList<String>();
     String regimenTableTillTR = "//table[@id='configureProgramRegimensTable']/tbody/tr";
     int size = testWebDriver.getElementsSizeByXpath(regimenTableTillTR);
     for (int counter = 1; counter < size + 1; counter++) {
       testWebDriver.waitForElementToAppear(testWebDriver.getElementByXpath(regimenTableTillTR + "[" + counter + "]/td[1]"));
       programsList.add(testWebDriver.getElementByXpath(regimenTableTillTR + "[" + counter + "]/td[1]").getText().trim());
     }
     return programsList;
   }
 
   private void verifySuccessMessage(RegimenTemplateConfigPage regimenTemplateConfigPage) {
 
     assertTrue("saveSuccessMsgDiv should show up", regimenTemplateConfigPage.IsDisplayedSaveSuccessMsgDiv());
     String saveSuccessfullyMessage = "Regimens saved successfully";
     assertEquals(saveSuccessfullyMessage, regimenTemplateConfigPage.getSaveSuccessMsgDiv());
 
   }
 
   private void verifyProgramConfigured(String program) {
     testWebDriver.waitForElementToAppear(testWebDriver.getElementById(program));
     assertTrue("Program " + program + "should be configured", testWebDriver.getElementById(program).getText().trim().equals("Edit"));
 
   }
 
   private void verifyProgramDetailsSaved(String category, String code, String name, String reportingField, String codeLabelAltered) {
     RegimenTemplateConfigPage regimenTemplateConfigPage = new RegimenTemplateConfigPage(testWebDriver);
     verifyNonEditableRegimenAdded(code, name, false, 1);
 
     regimenTemplateConfigPage.clickReportingFieldTab();
     assertEquals(reportingField, regimenTemplateConfigPage.getValueRemarksTextField());
    assertEquals(codeLabelAltered, regimenTemplateConfigPage.getValueCodeTextField());
 
   }
 
 
   private void setUpDataForInitiateRnR(String program, String userSIC) throws SQLException, IOException {
     dbWrapper.setupMultipleProducts(program, "Lvl3 Hospital", 2, false);
     dbWrapper.insertFacilities("F10", "F11");
     dbWrapper.configureTemplate(program);
     List<String> rightsList = new ArrayList<String>();
     rightsList.add("CREATE_REQUISITION");
     rightsList.add("VIEW_REQUISITION");
     setupTestUserRoleRightsData("200", userSIC, "openLmis", rightsList);
     dbWrapper.insertSupervisoryNode("F10", "N1", "Node 1", "null");
     dbWrapper.insertRoleAssignment("200", "store in-charge");
     dbWrapper.insertSchedule("Q1stM", "QuarterMonthly", "QuarterMonth");
     dbWrapper.insertSchedule("M", "Monthly", "Month");
     dbWrapper.insertProcessingPeriod("Period1", "first period", "2012-12-01", "2013-01-15", 1, "Q1stM");
     dbWrapper.insertProcessingPeriod("Period2", "second period", "2013-01-16", "2013-01-30", 1, "M");
     setupRequisitionGroupData("RG1", "RG2", "N1", "N2", "F10", "F11");
     dbWrapper.insertSupplyLines("N1", program, "F10");
   }
 
   @AfterMethod(groups = {"smoke", "functional2"})
   public void tearDown() throws Exception {
     HomePage homePage = new HomePage(testWebDriver);
     homePage.logout(baseUrlGlobal);
     dbWrapper.deleteData();
     dbWrapper.closeConnection();
   }
 
   @DataProvider(name = "Data-Provider")
   public Object[][] parameterVerifyRnRScreen() {
     return new Object[][]{
       {"ESSENTIAL MEDICINES", new String[]{"Admin123", "Admin123"}}
     };
 
   }
 
   @DataProvider(name = "Data-Provider-Multiple-Programs")
   public Object[][] parameterMultiplePrograms() {
     return new Object[][]{
       {"ESSENTIAL MEDICINES", "TB", new String[]{"Admin123", "Admin123"}}
     };
 
   }
 
   @DataProvider(name = "Data-Provider-Function-Positive")
   public Object[][] parameterIntTestProviderPositive() {
     return new Object[][]{
       {"HIV","Admin123", "storeincharge", "ADULTS", "Admin123", "RegimenCode1", "RegimenName1", "RegimenCode2", "RegimenName2"}
     };
 
 
   }
 }
 
