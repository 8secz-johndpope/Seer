 package org.molgenis.xgap.test;
 
 import java.io.File;
 
 import org.molgenis.framework.db.Database;
 import org.molgenis.util.DetectOS;
 import org.molgenis.util.TarGz;
 import org.openqa.selenium.server.RemoteControlConfiguration;
 import org.openqa.selenium.server.SeleniumServer;
 import org.testng.Assert;
 import org.testng.annotations.AfterClass;
 import org.testng.annotations.BeforeClass;
 import org.testng.annotations.Test;
 
 import app.DatabaseFactory;
 import app.servlet.UsedMolgenisOptions;
 import boot.RunStandalone;
 
 import com.thoughtworks.selenium.DefaultSelenium;
 import com.thoughtworks.selenium.HttpCommandProcessor;
 import com.thoughtworks.selenium.Selenium;
 
 import core.Helper;
 
 import filehandling.storage.StorageHandler;
 
 /**
  * 
  * The complete xQTL Selenium web test.
  * Has a section for init/helpers and section for the real tests.
  * 
  */
 public class XqtlSeleniumTest
 {
 	
 	boolean gbicdev_dontrunthis = false;
 	
 	
 	/**
 	 *******************************************************************
 	 *************************  Init and helpers  **********************
 	 *******************************************************************
 	 */
 
 	Selenium selenium;
 	String pageLoadTimeout = "30000";
 	boolean tomcat = false;
 
 	UsedMolgenisOptions usedOptions;
 	
 	public static void deleteDatabase() throws Exception
 	{
 		File dbDir = new File("hsqldb");
 		if (dbDir.exists())
 		{
 			TarGz.recursiveDeleteContentIgnoreSvn(dbDir);
 		}
 		else
 		{
 			throw new Exception("HSQL database directory does not exist");
 		}
 
 		if (dbDir.list().length != 1)
 		{
 			throw new Exception("HSQL database directory does not contain 1 file (.svn) after deletion! it contains: "
 					+ dbDir.list().toString());
 		}
 	}
 
 	public static void deleteStorage(String appName) throws Exception
 	{
 		// get storage folder and delete it completely
 		// throws exceptions if anything goes wrong
 		Database db = DatabaseFactory.create();
 		int appNameLength = appName.length();
 		String storagePath = new StorageHandler(db).getFileStorage(true, db).getAbsolutePath();
 		File storageRoot = new File(storagePath.substring(0, storagePath.length() - appNameLength));
 		TarGz.recursiveDeleteContent(new File(storagePath));
 		TarGz.delete(storageRoot, false);
 	}
 
 	/**
 	 * Configure Selenium server and delete the database
 	 */
 	@BeforeClass
 	public void start() throws Exception
 	{
 		usedOptions = new UsedMolgenisOptions();
 		
 		int webserverPort = 8080;
 		if (!tomcat) webserverPort = Helper.getAvailablePort(11040, 10);
 
 		String seleniumUrl = "http://localhost:" + webserverPort + "/";
 		String seleniumHost = "localhost";
 		String seleniumBrowser = "firefox";
 		int seleniumPort = Helper.getAvailablePort(11050, 10);
 
 		RemoteControlConfiguration rcc = new RemoteControlConfiguration();
 		rcc.setSingleWindow(true);
 		rcc.setPort(seleniumPort);
 
 		try
 		{
 			SeleniumServer server = new SeleniumServer(false, rcc);
 			server.boot();
 		}
 		catch (Exception e)
 		{
 			throw new IllegalStateException("Cannot start selenium server: ", e);
 		}
 
 		HttpCommandProcessor proc = new HttpCommandProcessor(seleniumHost, seleniumPort, seleniumBrowser, seleniumUrl);
 		selenium = new DefaultSelenium(proc);
 		selenium.start();
 
 		deleteDatabase();
 
 		if (!tomcat) new RunStandalone(webserverPort);
 	}
 
 	/**
 	 * Stop Selenium server and remove files
 	 */
 	@AfterClass
 	public void stop() throws Exception
 	{
 		selenium.stop();
 		deleteStorage(usedOptions.appName);
 	}
 
 	/**
 	 * Start the app and verify home page
 	 */
 	@Test
 	public void startup() throws InterruptedException
 	{
 		selenium.open("/" + usedOptions.appName + "/molgenis.do");
 		selenium.waitForPageToLoad(pageLoadTimeout);
 		Assert.assertTrue(selenium.getTitle().toLowerCase().contains("xQTL workbench".toLowerCase()));
 		Assert.assertTrue(selenium.isTextPresent("Welcome"));
 		Assert.assertEquals(selenium.getText("link=R api"), "R api");
 	}
 
 	/**
 	 * Login as admin and redirect
 	 */
 	@Test(dependsOnMethods =
 	{ "startup" })
 	public void login() throws InterruptedException
 	{
 		clickAndWait("id=UserLogin_tab_button");
 		Assert.assertEquals(selenium.getText("link=Register"), "Register");
 		selenium.type("id=username", "admin");
 		selenium.type("id=password", "admin");
 		clickAndWait("id=Login");
 		// note: page now redirects to the Home screen ('auth_redirect' in
 		// properties)
 		Assert.assertTrue(selenium
 				.isTextPresent("You are logged in as admin, and the database does not contain any investigations or other users."));
 	}
 
 	/**
 	 * Press 'Load' example data
 	 */
 	@Test(dependsOnMethods =
 	{ "login" })
 	public void loadExampleData() throws InterruptedException
 	{
 		selenium.type("id=inputBox", storagePath());
 		clickAndWait("id=loadExamples");
 		Assert.assertTrue(selenium.isTextPresent("File path '" + storagePath()
 				+ "' was validated and the dataloader succeeded"));
 	}
 
 	/**
 	 * Function that sets the application to a 'default' state after setting up.
 	 * All downstream test functions require this to be done.
 	 */
 	@Test(dependsOnMethods =
 	{ "loadExampleData" })
 	public void returnHome() throws InterruptedException
 	{
 		clickAndWait("id=ClusterDemo_tab_button");
 	}
 
 	/**
 	 * Helper function. Click a target and wait.
 	 */
 	public void clickAndWait(String target)
 	{
 		selenium.click(target);
 		selenium.waitForPageToLoad(pageLoadTimeout);
 	}
 
 	/**
 	 * Helper function. Get DOM property using JavaScript.
 	 */
 	public String propertyScript(String element, String property)
 	{
 		return "var x = window.document.getElementById('" + element
 				+ "'); window.document.defaultView.getComputedStyle(x,null).getPropertyValue('" + property + "');";
 	}
 
 	/**
 	 * Helper function. Get the storage path to use in test.
 	 */
 	public String storagePath()
 	{
 		String storagePath = new File(".").getAbsolutePath() + File.separator + "tmp_selenium_test_data";
 		if (DetectOS.getOS().startsWith("windows"))
 		{
 			return storagePath.replace("\\", "/");
 		}
 		else
 		{
 			return storagePath;
 		}
 	}
 	
 	
 	
 	/**
 	 *******************************************************************
 	 **************************  Assorted tests  ***********************
 	 *******************************************************************
 	 */
 	
 	
 	
 	@Test(dependsOnMethods =
 		{ "returnHome" })
 		public void exploreExampleData() throws InterruptedException
 		{
 			// browse to Overview and check links
 			clickAndWait("id=Investigations_tab_button");
 			clickAndWait("id=Overview_tab_button");
 			Assert.assertEquals(selenium.getText("link=Marker (117)"), "Marker (117)");
 
 			// click link to a matrix and check values
 			clickAndWait("link=metaboliteexpression");
 			Assert.assertTrue(selenium.isTextPresent("942") && selenium.isTextPresent("4857")
 					&& selenium.isTextPresent("20716"));
 			
 			//restore state
 			clickAndWait("id=remove_filter_0");
 		}
 
 		@Test(dependsOnMethods =
 		{ "returnHome" })
 		public void compactView() throws InterruptedException
 		{
 			// browse to Data tab
 			clickAndWait("id=Investigations_tab_button");
 			clickAndWait("id=Datas_tab_button");
 
 			// assert if the hide investigation and data table rows are hidden
 			Assert.assertEquals(selenium.getEval(propertyScript("Investigations_collapse_tr_id", "display")), "none");
 			Assert.assertEquals(selenium.getEval(propertyScript("Datas_collapse_tr_id", "display")), "none");
 
 			// click both unhide buttons
 			selenium.click("id=Investigations_collapse_button_id");
 			selenium.click("id=Datas_collapse_button_id");
 
 			// assert if the hide investigation and data table rows are exposed
 			Assert.assertEquals(selenium.getEval(propertyScript("Investigations_collapse_tr_id", "display")), "table-row");
 			Assert.assertEquals(selenium.getEval(propertyScript("Datas_collapse_tr_id", "display")), "table-row");
 
 			// click both unhide buttons
 			selenium.click("id=Investigations_collapse_button_id");
 			selenium.click("id=Datas_collapse_button_id");
 
 			// assert if the hide investigation and data table rows are hidden again
 			Assert.assertEquals(selenium.getEval(propertyScript("Investigations_collapse_tr_id", "display")), "none");
 			Assert.assertEquals(selenium.getEval(propertyScript("Datas_collapse_tr_id", "display")), "none");
 
 		}
 
 		@Test(dependsOnMethods =
 		{ "returnHome" })
 		public void individualForms() throws Exception
 		{
 			// browse to individuals
 			clickAndWait("id=Investigations_tab_button");
 			clickAndWait("id=BasicAnnotations_tab_button");
 			clickAndWait("id=Individuals_tab_button");
 			clickAndWait("id=first_Individuals");
 
 			// check some values here
 			Assert.assertEquals(selenium.getText("//form[@id='Individuals_form']/div[3]/div/div/table/tbody/tr[7]/td[4]"),
 					"X7");
 			Assert.assertTrue(selenium.isTextPresent("xgap_rqtl_straintype_riself"));
 
 			// switch to edit view and check some values there too
 			clickAndWait("id=Individuals_editview");
 			Assert.assertEquals(selenium.getValue("id=Individual_name"), "X1");
 			Assert.assertEquals(selenium.getText("css=#Individual_ontologyReference_chzn > a.chzn-single > span"),
 					"xgap_rqtl_straintype_riself");
 			Assert.assertEquals(
 					selenium.getText("//img[@onclick=\"if ($('#Individuals_form').valid() && validateForm(document.forms.Individuals_form,new Array())) {setInput('Individuals_form','_self','','Individuals','update','iframe'); document.forms.Individuals_form.submit();}\"]"),
 					"");
 
 			// click add new, wait for popup, and select it
 			selenium.click("id=Individuals_edit_new");
 			selenium.waitForPopUp("molgenis_edit_new", "30000");
 			selenium.selectWindow("name=molgenis_edit_new");
 
 			// fill in the form and click add
 			selenium.type("id=Individual_name", "testindv");
 			clickAndWait("id=Add");
 
 			// select main window and check if add was successful
 			selenium.selectWindow("title=xQTL workbench");
 			Assert.assertTrue(selenium.isTextPresent("ADD SUCCESS: affected 1"));
 
 			// page back and forth and check values
 			clickAndWait("id=prev_Individuals");
 			Assert.assertEquals(selenium.getValue("id=Individual_name"), "X193");
 			clickAndWait("id=last_Individuals");
 			Assert.assertEquals(selenium.getValue("id=Individual_name"), "testindv");
 
 			// delete the test individual and check if it happened
 			selenium.click("id=delete_Individuals");
 			Assert.assertEquals(selenium.getConfirmation(),
 					"You are about to delete a record. If you click [yes] you won't be able to undo this operation.");
 			selenium.waitForPageToLoad(pageLoadTimeout);
 			Assert.assertTrue(selenium.isTextPresent("REMOVE SUCCESS: affected 1"));
 
 		}
 
 		@Test(dependsOnMethods =
 		{ "returnHome" })
 		public void enumInput() throws Exception
 		{
 			// browse to 'Data' view and expand compact view
 			clickAndWait("id=Investigations_tab_button");
 			clickAndWait("id=Datas_tab_button");
 			selenium.click("id=Datas_collapse_button_id");
 
 			// assert content of enum fields
 			Assert.assertEquals(
 					selenium.getTable("css=#Datas_form > div.screenbody > div.screenpadding > table > tbody > tr > td > table.8.1"),
 					"Individual\n\nChromosomeDerivedTraitEnvironmentalFactorGeneIndividualMarkerMassPeakMeasurementMetabolitePanelProbeProbeSetSampleSpot");
 			Assert.assertEquals(
 					selenium.getTable("css=#Datas_form > div.screenbody > div.screenpadding > table > tbody > tr > td > table.9.1"),
 					"Metabolite\n\nChromosomeDerivedTraitEnvironmentalFactorGeneIndividualMarkerMassPeakMeasurementMetabolitePanelProbeProbeSetSampleSpot");
 
 			// change Individual to Gene and save
 			selenium.click("css=#Data_FeatureType_chzn > a.chzn-single > span");
 			selenium.click("id=Data_FeatureType_chzn_o_3");
 			selenium.click("id=save_Datas");
 			selenium.waitForPageToLoad(pageLoadTimeout);
 			Assert.assertTrue(selenium.isTextPresent("UPDATE SUCCESS: affected 1"));
 
 			// expand compact view again and check value has changed
 			selenium.click("id=Datas_collapse_button_id");
 			Assert.assertEquals(
 					selenium.getTable("css=#Datas_form > div.screenbody > div.screenpadding > table > tbody > tr > td > table.8.1"),
 					"Gene\n\nChromosomeDerivedTraitEnvironmentalFactorGeneIndividualMarkerMassPeakMeasurementMetabolitePanelProbeProbeSetSampleSpot");
 
 			// change back to Individual and save
 			selenium.click("css=#Data_FeatureType_chzn > a.chzn-single > span");
 			selenium.click("id=Data_FeatureType_chzn_o_4");
 			clickAndWait("id=save_Datas");
 			Assert.assertTrue(selenium.isTextPresent("UPDATE SUCCESS: affected 1"));
 
 			// expand compact view again and check value is back to normal again
 			selenium.click("id=Datas_collapse_button_id");
 			Assert.assertEquals(
 					selenium.getTable("css=#Datas_form > div.screenbody > div.screenpadding > table > tbody > tr > td > table.8.1"),
 					"Individual\n\nChromosomeDerivedTraitEnvironmentalFactorGeneIndividualMarkerMassPeakMeasurementMetabolitePanelProbeProbeSetSampleSpot");
 
 		}
 
 		@Test(dependsOnMethods =
 		{ "returnHome" })
 		public void qtlImporter() throws Exception
 		{
 			// not much to test here, cannot upload actual files
 			// could only be tested on an API level, or manually
 
 			// go to Import data and check the screen contents of the QTL importer
 			clickAndWait("id=ImportDataMenu_tab_button");
 			clickAndWait("id=QTLWizard_tab_button");
 			
 			Assert.assertTrue(selenium.isTextPresent("Again, your individuals must be in the first line."));
 			
 			Assert.assertEquals(selenium.getText("name=invSelect"), "ClusterDemo");
 			Assert.assertEquals(selenium.getText("name=cross"),"xgap_rqtl_straintype_f2xgap_rqtl_straintype_bcxgap_rqtl_straintype_riselfxgap_rqtl_straintype_risibxgap_rqtl_straintype_4wayxgap_rqtl_straintype_dhxgap_rqtl_straintype_specialxgap_rqtl_straintype_naturalxgap_rqtl_straintype_parentalxgap_rqtl_straintype_f1xgap_rqtl_straintype_rccxgap_rqtl_straintype_cssxgap_rqtl_straintype_unknownxgap_rqtl_straintype_other");
 			Assert.assertEquals(selenium.getText("name=trait"),"MeasurementDerivedTraitEnvironmentalFactorGeneMarkerMassPeakMetaboliteProbe");
 
 			// try pressing a button and see if the error pops up
 			clickAndWait("id=upload_genotypes");
 			Assert.assertTrue(selenium.isTextPresent("No file selected"));
 		}
 
 		@Test(dependsOnMethods =
 		{ "returnHome" })
 		public void runMapping() throws Exception
 		{
 			// we're not going to test actual execution, just page around
 
 			// go to Run QTL mapping
 			clickAndWait("id=Cluster_tab_button");
 			Assert.assertTrue(selenium.isTextPresent("This is the main menu for starting a new analysis"));
 
 			// page from starting page to Step 2
 			clickAndWait("id=start_new_analysis");
 			Assert.assertEquals(selenium.getValue("name=outputDataName"), "MyOutput");
 			clickAndWait("id=toStep2");
 			Assert.assertTrue(selenium.isTextPresent("You have selected: Rqtl_analysis"));
 			Assert.assertEquals(selenium.getText("name=phenotypes"), "Fu_LCMS_data");
 
 			// go back to starting page
 			clickAndWait("id=toStep1");
 			clickAndWait("id=toStep0");
 
 			// go to job manager
 			clickAndWait("id=view_running_analysis");
 			Assert.assertTrue(selenium.isTextPresent("No running analysis to display."));
 
 			// go back to starting page
 			clickAndWait("id=back_to_start");
 			Assert.assertTrue(selenium.isTextPresent("No settings saved."));
 
 		}
 
 		@Test(dependsOnMethods =
 		{ "returnHome" })
 		public void configureAnalysis() throws Exception
 		{
 			// browse to Configure Analysis and check values
 			clickAndWait("id=AnalysisSettings_tab_button");
 			Assert.assertEquals(selenium.getTable("css=table.listtable.1.3"), "Rqtl_analysis");
 			Assert.assertEquals(selenium.getTable("css=table.listtable.1.4"),
 					"This is a basic QTL analysis performed in the R environment for statistical computing, powered by th...");
 
 			if(gbicdev_dontrunthis){
 			
 			// browse to R scripts and add a script
 			clickAndWait("id=RScripts_tab_button");
 			selenium.click("id=RScripts_edit_new");
 			selenium.waitForPopUp("molgenis_edit_new", "30000");
 			selenium.selectWindow("name=molgenis_edit_new");
 			selenium.type("id=RScript_name", "test");
 			selenium.type("id=RScript_Extension", "r");
 			selenium.click("css=span");
 			//from: http://blog.browsermob.com/2011/03/selenium-tips-wait-with-waitforcondition/
 			//"Now for the killer part, for sites that use jQuery, if all you need is to confirm there aren't any active asynchronous requests, then the following does the trick:"
 			selenium.waitForCondition("selenium.browserbot.getUserWindow().$.active == 0", "10000");
 			selenium.click("css=span");
 			clickAndWait("id=Add");
 
 			// add content and save
 			selenium.selectWindow("title=xQTL workbench");
 			Assert.assertTrue(selenium.isTextPresent("ADD SUCCESS: affected 1"));
 			Assert.assertTrue(selenium.isTextPresent("No file found. Please upload it here."));
 			selenium.type("name=inputTextArea", "content");
 			clickAndWait("id=uploadTextArea");
 			Assert.assertFalse(selenium.isTextPresent("No file found. Please upload it here."));
 
 			// delete script and content
 			selenium.click("id=delete_RScripts");
 			Assert.assertEquals(selenium.getConfirmation(),
 					"You are about to delete a record. If you click [yes] you won't be able to undo this operation.");
 			selenium.waitForPageToLoad(pageLoadTimeout);
 			Assert.assertTrue(selenium.isTextPresent("REMOVE SUCCESS: affected 1"));
 			
 			}
 			
 			// browse to Tag data and click the hide/show buttons
 			clickAndWait("id=MatrixWizard_tab_button");
 			Assert.assertTrue(selenium
 					.isTextPresent("This screen provides an overview of all data matrices stored in your database."));
 			Assert.assertTrue(selenium.isTextPresent("ClusterDemo / genotypes"));
 			Assert.assertTrue(selenium.isTextPresent("Rqtl_data -> genotypes"));
 			clickAndWait("id=hide_verified");
 			Assert.assertTrue(selenium.isTextPresent("Nothing to display"));
 			clickAndWait("id=show_verified");
 			Assert.assertTrue(selenium.isTextPresent("Binary"));
 		}
 
 		@Test(dependsOnMethods =
 		{ "returnHome" })
 		public void search() throws Exception
 		{
 			// browse to Search
 			clickAndWait("id=SearchMenu_tab_button");
 			clickAndWait("id=SimpleDbSearch_tab_button");
 			selenium.type("name=searchThis", "ee");
 			clickAndWait("id=simple_search");
 
 			Assert.assertTrue(selenium.isTextPresent("Found 2 result(s)"));
 			Assert.assertTrue(selenium.isTextPresent("Type: BinaryDataMatrix"));
 
 			selenium.type("name=searchThis", "e");
 			clickAndWait("id=simple_search");
 			Assert.assertTrue(selenium.isTextPresent("Found 339 result(s)"));
 			Assert.assertTrue(selenium.isTextPresent("bioinformatician"));
 
 		}
 
 		@Test(dependsOnMethods =
 		{ "returnHome" })
 		public void molgenisFileMenu() throws Exception
 		{
 			// browse to chromosomes and click 'Update selected' in File
 			clickAndWait("id=Investigations_tab_button");
 			clickAndWait("id=BasicAnnotations_tab_button");
 			clickAndWait("Chromosomes_tab_button");
 			selenium.click("id=Chromosomes_menu_Edit");
 			selenium.click("id=Chromosomes_edit_update_selected_submenuitem");
 
 			// select the popup and verify the message
 			selenium.waitForPopUp("molgenis_edit_update_selected", "5000");
 			selenium.selectWindow("name=molgenis_edit_update_selected");
 			Assert.assertTrue(selenium.isTextPresent("No records were selected for updating."));
 
 			// cancel and select main window
 			selenium.click("id=Cancel");
 			selenium.selectWindow("title=xQTL workbench");
 
 			// TODO: select records, update 2+, verify values changed, change them
 			// back, verify values changed back
 
 			// TODO: Add in batch/upload CSV(as best as possible w/o uploading)
 
 			// TODO: Upload CSV file (as best as possible w/o uploading)
 		}
 		
 		@Test(dependsOnMethods =
 			{ "returnHome" })
 			public void userRoleMenuVisibility() throws Exception
 			{
 				//find out if admin can see the correct tabs
 				Assert.assertTrue(selenium.isTextPresent("Home*Login*Browse / add data*Import special data*Run QTL mapping*Configure analysis*Search / report*Utilities*Admin panel"));
 				clickAndWait("id=Admin_tab_button");
 				Assert.assertTrue(selenium.isTextPresent("Users and permissions*Database status*File storage*Install R packages*Admin utilities"));
 				clickAndWait("id=OtherAdmin_tab_button");
 				Assert.assertTrue(selenium.isTextPresent("Job table"));
 				
 				String whatBiologistCanSee = "Browse / add data*Import special data*Run QTL mapping*Search / report*Utilities";
 				
 				//logout and see if the correct tabs are visible
 				clickAndWait("id=UserLogin_tab_button");
 				clickAndWait("id=Logout");
 				Assert.assertTrue(selenium.isTextPresent("Home*Login"));
 				Assert.assertFalse(selenium.isTextPresent(whatBiologistCanSee));
 				
 				//login as biologist and see if the correct tabs are visible
 				selenium.type("id=username", "bio-user");
 				selenium.type("id=password", "bio");
 				clickAndWait("id=Login");
 				Assert.assertTrue(selenium.isTextPresent("Home*Login*" + whatBiologistCanSee));
 				Assert.assertFalse(selenium.isTextPresent("Configure analysis"));
 				Assert.assertFalse(selenium.isTextPresent("Admin panel"));
 				
 				clickAndWait("id=Utilities_tab_button");
 				clickAndWait("id=Tools_tab_button");
 				Assert.assertTrue(selenium.isTextPresent("Format names*Rename duplicates"));
 				Assert.assertFalse(selenium.isTextPresent("KEGG converter"));
 				Assert.assertFalse(selenium.isTextPresent("ROnline"));
 
 				//login as bioinformatician and see if the correct tabs are visible
 				clickAndWait("id=UserLogin_tab_button");
 				clickAndWait("id=Logout");
 				selenium.type("id=username", "bioinfo-user");
 				selenium.type("id=password", "bioinfo");
 				clickAndWait("id=Login");
 				Assert.assertTrue(selenium.isTextPresent("Home*Login*" + whatBiologistCanSee));
 				Assert.assertTrue(selenium.isTextPresent("Configure analysis"));
 				Assert.assertFalse(selenium.isTextPresent("Admin panel"));
 				
 				clickAndWait("id=Utilities_tab_button");
 				clickAndWait("id=Tools_tab_button");
 				Assert.assertTrue(selenium.isTextPresent("Format names*Rename duplicates*KEGG converter*ROnline"));
 				
 				//log back in as admin
 				clickAndWait("id=UserLogin_tab_button");
 				clickAndWait("id=Logout");
 				selenium.type("id=username", "admin");
 				selenium.type("id=password", "admin");
 				clickAndWait("id=Login");
 
 			}
 			
 			@Test(dependsOnMethods =
 			{ "returnHome" })
 			public void namePolicy() throws Exception
 			{
 			
 				//find out of the strict policy is in effect for entities
 				
 				// browse to individuals
 				clickAndWait("id=Investigations_tab_button");
 				clickAndWait("id=BasicAnnotations_tab_button");
 				clickAndWait("Individuals_tab_button");
 				
 				// click add new, wait for popup, and select it
 				selenium.click("id=Individuals_edit_new");
 				selenium.waitForPopUp("molgenis_edit_new", "30000");
 				selenium.selectWindow("name=molgenis_edit_new");
 
 				// fill in the form and click add
 				selenium.type("id=Individual_name", "#");
 				clickAndWait("id=Add");
 
 				// select main window and check if add failed
 				selenium.selectWindow("title=xQTL workbench");
 				Assert.assertTrue(selenium.isTextPresent("ADD FAILED: Illegal character (#) in name '#'. Use only a-z, A-Z, 0-9, and underscore."));
 
 				// click add new, wait for popup, and select it
 				selenium.click("id=Individuals_edit_new");
 				selenium.waitForPopUp("molgenis_edit_new", "30000");
 				selenium.selectWindow("name=molgenis_edit_new");
 
 				// fill in the form and click add
 				selenium.type("id=Individual_name", "1");
 				clickAndWait("id=Add");
 				
 				// select main window and check if add failed
 				selenium.selectWindow("title=xQTL workbench");
 				Assert.assertTrue(selenium.isTextPresent("ADD FAILED: Name '1' is not allowed to start with a numeral (1)."));
 
 				//find out of the strict policy is in effect for data matrix (files)
 				clickAndWait("id=Datas_tab_button");
 				Assert.assertTrue(selenium.isTextPresent("metaboliteexpression"));
 				
 				// click add new, wait for popup, and select it
 				selenium.click("id=Datas_edit_new");
 				selenium.waitForPopUp("molgenis_edit_new", "30000");
 				selenium.selectWindow("name=molgenis_edit_new");
 				
 				// fill in the form and click add
 				selenium.type("id=Data_name", "#");
 				clickAndWait("id=Add");
 				
 				// select main window and check if add failed
 				selenium.selectWindow("title=xQTL workbench");
 				Assert.assertTrue(selenium.isTextPresent("ADD FAILED: Illegal character (#) in name '#'. Use only a-z, A-Z, 0-9, and underscore."));
 
 				// click add new, wait for popup, and select it
 				selenium.click("id=Datas_edit_new");
 				selenium.waitForPopUp("molgenis_edit_new", "30000");
 				selenium.selectWindow("name=molgenis_edit_new");
 				
 				// fill in the form and click add
 				// notice the name is allowed, but not OK for files!
 				selenium.type("id=Data_name", "metaboliteExpression");
 				clickAndWait("id=Add");
 				
 				// select main window and check if add succeeded
 				selenium.selectWindow("title=xQTL workbench");
 				Assert.assertTrue(selenium.isTextPresent("ADD SUCCESS: affected 1"));
 				
 				//add some content and try to save - this must fail
 				selenium.type("id=matrixInputTextArea", "qw er\nty 1 2");
 				clickAndWait("id=matrixUploadTextArea");
				Assert.assertTrue(selenium.isTextPresent("There is already a storage file named 'metaboliteexpression' which is used when escaping the name 'metaboliteExpression'. Please rename your Data matrix or contact your admin."));
 				
 				// delete the test data and check if it happened
 				selenium.click("id=delete_Datas");
 				Assert.assertEquals(selenium.getConfirmation(),
 						"You are about to delete a record. If you click [yes] you won't be able to undo this operation.");
 				selenium.waitForPageToLoad(pageLoadTimeout);
 				Assert.assertTrue(selenium.isTextPresent("REMOVE SUCCESS: affected 1"));
 				
 			}
 			
 			@Test(dependsOnMethods =
 			{ "returnHome" })
 			public void guiNestingXrefDefault() throws Exception
 			{
 				// find out if nested forms by XREF display this relation by default
 				// when adding new entities
 				
 				
 				// browse to individuals
 				clickAndWait("id=Investigations_tab_button");
 				clickAndWait("id=BasicAnnotations_tab_button");
 				clickAndWait("Individuals_tab_button");
 				
 				// click add new, wait for popup, and select it
 				selenium.click("id=Individuals_edit_new");
 				selenium.waitForPopUp("molgenis_edit_new", "30000");
 				selenium.selectWindow("name=molgenis_edit_new");
 				
 				//check if the proper xreffed investigation is selected
 				Assert.assertEquals(selenium.getText("css=span"), "ClusterDemo");
 				
 				//cancel and return
 				selenium.click("id=cancel");
 				selenium.selectWindow("title=xQTL workbench");
 				
 				//change and save
 				selenium.type("id=Investigation_name", "TestIfThisWorks");
 				clickAndWait("id=save_Investigations");
 				Assert.assertTrue(selenium.isTextPresent("UPDATE SUCCESS: affected 1"));
 
 				// click add new, wait for popup, and select it
 				selenium.click("id=Individuals_edit_new");
 				selenium.waitForPopUp("molgenis_edit_new", "30000");
 				selenium.selectWindow("name=molgenis_edit_new");
 				
 				//check if the proper xreffed investigation is selected
 				Assert.assertEquals(selenium.getText("css=span"), "TestIfThisWorks");
 				
 				//cancel and return
 				selenium.click("id=cancel");
 				selenium.selectWindow("title=xQTL workbench");
 				
 				//revert and save
 				selenium.type("id=Investigation_name", "ClusterDemo");
 				clickAndWait("id=save_Investigations");
 				Assert.assertTrue(selenium.isTextPresent("UPDATE SUCCESS: affected 1"));
 
 			}
 			
 			@Test(dependsOnMethods =
 			{ "returnHome" })
 			public void startAnalysis() throws Exception
 			{
 				// start a default analysis and check if jobs have
 				// been created, regardless of further outcome
 				
 				// start QTL mapping
 				clickAndWait("id=Cluster_tab_button");
 				clickAndWait("id=start_new_analysis");
 				clickAndWait("id=toStep2");
 				clickAndWait("id=startAnalysis");
 				
 				Assert.assertTrue(selenium.isTextPresent("Refresh page every"));
 				Assert.assertTrue(selenium.isTextPresent("Running analysis"));
 				Assert.assertTrue(selenium.isTextPresent("[view all local logs]"));
 				
 				//check created job/subjobs
 				clickAndWait("id=Admin_tab_button");
 				clickAndWait("id=OtherAdmin_tab_button");
 				clickAndWait("id=Jobs_tab_button");
 				
 				Assert.assertEquals(selenium.getText("css=span"), "Rqtl_analysis");
 				Assert.assertEquals(selenium.getText("css=#Job_ComputeResource_chzn > a.chzn-single > span"), "local");
 				Assert.assertTrue(selenium.isTextPresent("MyOutput*Subjobs*1 - 6 of 6"));
 
 			}
 			
 			@Test(dependsOnMethods =
 			{ "returnHome" })
 			public void pageMatrix() throws Exception
 			{
 				// page around in the matrix and see if the correct
 				// values are displayed in the viewer
 			}
 			
 			@Test(dependsOnMethods =
 			{ "returnHome" })
 			public void addMatrix() throws Exception
 			{
 				// add a new matrix record
 				// add data by typing in the text area and store
 				// - as binary
 				// - as database
 				// - as csv
 				// backend deletes inbetween
 			}
 
 }
