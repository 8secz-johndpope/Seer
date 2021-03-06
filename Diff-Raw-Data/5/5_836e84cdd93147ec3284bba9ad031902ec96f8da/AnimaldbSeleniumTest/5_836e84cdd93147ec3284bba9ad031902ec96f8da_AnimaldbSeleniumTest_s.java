 package test;
 
 import java.util.Calendar;
 
 import org.openqa.selenium.server.RemoteControlConfiguration;
 import org.openqa.selenium.server.SeleniumServer;
 import org.testng.Assert;
 import org.testng.annotations.AfterClass;
 import org.testng.annotations.BeforeClass;
 import org.testng.annotations.Test;
 
 import plugins.emptydb.emptyDatabase;
 import app.DatabaseFactory;
 import app.servlet.MolgenisServlet;
 import boot.RunStandalone;
 
 import com.thoughtworks.selenium.DefaultSelenium;
 import com.thoughtworks.selenium.HttpCommandProcessor;
 import com.thoughtworks.selenium.Selenium;
 
 public class AnimaldbSeleniumTest
 {
 	Selenium selenium;
 	Integer sleepTime = 1000;
 	String pageLoadTimeout = "120000";
 	boolean tomcat = false;
 	//String storagePath = new File(".").getAbsolutePath() + File.separator + "tmp_selenium_test_data";
 
 	@BeforeClass
 	public void start() throws Exception
 	{
 		int webserverPort = 8080;
 		if(!this.tomcat) webserverPort = Helper.getAvailablePort(11000, 100);
 		
 		String seleniumUrl = "http://localhost:" + webserverPort + "/";
 		String seleniumHost = "localhost";
 		String seleniumBrowser = "firefox";
 		int seleniumPort = Helper.getAvailablePort(9080, 100);
 	
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
 		selenium.setTimeout(pageLoadTimeout);
 		
 		// To be sure, empty db and don't add MolgenisUsers etc.
 		if(!this.tomcat) new emptyDatabase(new MolgenisServlet().getDatabase(), false);
 		else new emptyDatabase(DatabaseFactory.create("apps/animaldb/org/molgenis/animaldb/animaldb.properties"), false);
 		if(!this.tomcat) new RunStandalone(webserverPort);
 	}
 
 	@Test
 	public void startup() throws InterruptedException
 	{
 		// Start page is now login screen
 		selenium.open("/molgenis_apps/molgenis.do");
 		selenium.waitForPageToLoad(pageLoadTimeout);
 		// But we need to go to the welcome screen first because there the DB gets pre-filled! (TODO: fix)
 		selenium.click("id=AnimalDBWelcomeScreen_tab_button");
 		selenium.waitForPageToLoad(pageLoadTimeout);
 		Assert.assertEquals(selenium.getTitle(), "AnimalDB");
 		Assert.assertTrue(selenium.isTextPresent("Welcome to AnimalDB!"));
 		Assert.assertTrue(selenium.isTextPresent("Your database was empty, so it was prefilled with entities needed to make AnimalDB run"));
 		
 		sleepHelper("startup");
 	}
 
 	@Test(dependsOnMethods={"startup"})
 	public void login() throws InterruptedException
 	{
 		// Go back to login screen first
 		selenium.click("id=UserLogin_tab_button");
 		selenium.waitForPageToLoad(pageLoadTimeout);
 		// Login
 		Assert.assertEquals(selenium.getText("link=Register"), "Register");
 		selenium.type("id=username", "admin");
 		selenium.type("id=password", "admin");
 		selenium.click("id=Login");
 		selenium.waitForPageToLoad(pageLoadTimeout);
 		
 		sleepHelper("login");
 	}
 	
 	@Test(dependsOnMethods={"login"})
 	public void addAnimals() throws Exception {
 		// Go to Add Animal plugin
 		selenium.click("id=animalmenu_tab_button");
 		selenium.waitForPageToLoad(pageLoadTimeout);
 		Assert.assertTrue(selenium.isTextPresent("Bring in animals"));
 		// Add 10 female Syrian hamsters
 		selenium.select("id=species", "label=Syrian hamster");
 		selenium.select("id=namebase", "value=");
 		selenium.type("id=numberofanimals", "10");
 		selenium.click("id=Add");
 		selenium.waitForPageToLoad(pageLoadTimeout);
 		Assert.assertTrue(selenium.isTextPresent("10 animal(s) added succesfully"));
 		// Add 10 male Syrian hamsters
 		selenium.select("id=species", "label=Syrian hamster");
 		selenium.select("id=sex", "label=Male");
 		selenium.select("id=namebase", "value=");
 		selenium.type("id=startnumber", "21");
 		selenium.type("id=numberofanimals", "10");
 		selenium.click("id=Add");
 		selenium.waitForPageToLoad(pageLoadTimeout);
 		Assert.assertTrue(selenium.isTextPresent("10 animal(s) added succesfully"));
 		
 		sleepHelper("addAnimals");
 	}
 	
 	@Test(dependsOnMethods={"addAnimals"})
 	public void breedingWorkflow() throws Exception {
 		// Go to Breeding line plugin
 		selenium.click("id=breedingmodule_tab_button");
 		selenium.waitForPageToLoad(pageLoadTimeout);
 		selenium.click("id=ManageLines_tab_button");
 		selenium.waitForPageToLoad(pageLoadTimeout);
 		Assert.assertTrue(selenium.isTextPresent("Manage breeding lines"));
 		// Add a breeding line
 		selenium.type("id=linename", "MyLine");
 		selenium.click("id=add");
 		selenium.waitForPageToLoad(pageLoadTimeout);
 		Assert.assertTrue(selenium.isTextPresent("Line successfully added"));
 		// Go to Parentgroup plugin
 		selenium.click("id=ManageParentgroups_tab_button");
 		selenium.waitForPageToLoad(pageLoadTimeout);
 		Assert.assertTrue(selenium.isTextPresent("Manage parent groups"));
 		// Add a parent group
 		selenium.type("id=groupname", "MyParentgroup");
 		selenium.click("id=addmother");
 		selenium.waitForPageToLoad(pageLoadTimeout);
 		selenium.click("id=addfather");
 		selenium.waitForPageToLoad(pageLoadTimeout);
 		selenium.click("id=addpg");
 		selenium.waitForPageToLoad(pageLoadTimeout);
 		Assert.assertTrue(selenium.isTextPresent("Parent group successfully added"));
 		// Go to Litter plugin
 		selenium.click("ManageLitters_tab_button");
 		selenium.waitForPageToLoad(pageLoadTimeout);
 		Assert.assertTrue(selenium.isTextPresent("Manage litters"));
 		Assert.assertTrue(selenium.isTextPresent("Make new litter"));
 		// Add new litter
 		selenium.click("link=Make new litter");
 		selenium.waitForPageToLoad(pageLoadTimeout);
 		selenium.type("id=littername", "MyLitter");
 		selenium.type("id=littersize", "5");
 		selenium.click("id=addlitter");
 		selenium.waitForPageToLoad(pageLoadTimeout);
 		Assert.assertTrue(selenium.isTextPresent("Litter successfully added"));
 		Assert.assertTrue(selenium.isTextPresent("MyLitter"));
 		// Wean litter
 		selenium.click("link=Wean");
 		selenium.waitForPageToLoad(pageLoadTimeout);
 		selenium.type("id=weansizefemale", "2");
 		selenium.type("id=weansizemale", "3");
 		selenium.click("id=wean");
 		selenium.waitForPageToLoad(pageLoadTimeout);
 		Assert.assertTrue(selenium.isTextPresent("All 5 animals successfully weaned"));
 		Assert.assertTrue(selenium.isTextPresent("MyLitter"));
 		// Check cage labels link
 		selenium.click("link=Make temporary cage labels");
 		selenium.waitForPageToLoad(pageLoadTimeout);
 		Assert.assertTrue(selenium.isTextPresent("Download temporary wean labels as pdf"));
 		selenium.click("link=Back to overview");
 		selenium.waitForPageToLoad(pageLoadTimeout);
 		// Genotype litter
 		// TODO: expand
 		selenium.click("link=Genotype");
 		selenium.waitForPageToLoad(pageLoadTimeout);
 		Assert.assertTrue(selenium.isTextPresent("Parentgroup: MyParentgroup"));
 		Assert.assertTrue(selenium.isTextPresent("Line: MyLine"));
 		selenium.click("id=save");
 		selenium.waitForPageToLoad(pageLoadTimeout);
 		Assert.assertTrue(selenium.isTextPresent("All 5 animals successfully genotyped"));
 		Assert.assertTrue(selenium.isTextPresent("MyLitter"));
 		// Check definitive cage labels link
 		selenium.click("link=Make definitive cage labels");
 		selenium.waitForPageToLoad(pageLoadTimeout);
 		Assert.assertTrue(selenium.isTextPresent("Download definitive cage labels as pdf"));
 		selenium.click("link=Back to overview");
 		selenium.waitForPageToLoad(pageLoadTimeout);
 		
 		sleepHelper("breedingWorkflow");
 	}
 	
 	@Test(dependsOnMethods={"breedingWorkflow"})
 	public void decWorkflow() throws Exception {
 		Calendar calendar = Calendar.getInstance();
 		String[] months = new String[] {"January", "February", "March", "April", "May", "June",
 										"July", "August", "September", "October", "November", "December"};
 		// Go to DEC project plugin
 		selenium.click("id=projectmenu_tab_button");
 		selenium.waitForPageToLoad(pageLoadTimeout);
 		selenium.click("id=AddProject_tab_button");
 		selenium.waitForPageToLoad(pageLoadTimeout);
 		Assert.assertTrue(selenium.isTextPresent("Manage DEC projects"));
 		// Make a DEC project
 		selenium.click("link=Add");
 		selenium.waitForPageToLoad(pageLoadTimeout);
 		selenium.type("id=name", "MyDEC");
 		selenium.type("id=decnumber", "12345");
 		selenium.type("id=decapppdf", "/home/test/app.pdf");
 		selenium.type("id=decapprovalpdf", "/home/test/app2.pdf");
 		int thisYear = calendar.get(Calendar.YEAR);
 		selenium.type("id=startdate", "January 1, " + thisYear);
 		selenium.type("id=enddate", "December 31, " + thisYear);
 		selenium.click("id=addproject");
 		selenium.waitForPageToLoad(pageLoadTimeout);
 		Assert.assertTrue(selenium.isTextPresent("DEC Project successfully added"));
 		Assert.assertTrue(selenium.isTextPresent("MyDEC"));
 		// Go to DEC subproject plugin
 		selenium.click("id=AddSubproject_tab_button");
 		selenium.waitForPageToLoad(pageLoadTimeout);
 		Assert.assertTrue(selenium.isTextPresent("Manage DEC subprojects"));
 		// Make a DEC subproject
 		selenium.click("link=Add");
 		selenium.waitForPageToLoad(pageLoadTimeout);
 		selenium.type("id=name", "MyProject");
 		selenium.type("id=decnumber", "A");
 		selenium.type("id=decapppdf", "/home/test/subapp.pdf");
 		int thisMonth = calendar.get(Calendar.MONTH);
 		selenium.type("id=startdate", months[thisMonth] + " 1, " + thisYear);
 		selenium.type("id=enddate", months[thisMonth] + " "+calendar.getActualMaximum(Calendar.DAY_OF_MONTH)+", " + thisYear);		
 		selenium.click("id=addsubproject");
 		selenium.waitForPageToLoad(pageLoadTimeout);
 		Assert.assertTrue(selenium.isTextPresent("DEC Subproject successfully added"));
 		Assert.assertTrue(selenium.isTextPresent("MyProject"));
 		// Go to Animals in DEC plugin
 		selenium.click("id=AnimalsInSubprojects_tab_button");
 		selenium.waitForPageToLoad(pageLoadTimeout);
 		Assert.assertTrue(selenium.isTextPresent("Manage animals in DEC subprojects"));
 		// Add animals to DEC (multiple select does not seem to work in Selenium so there is some duplication here
 		selenium.select("id=subproject", "label=MyProject");
 		selenium.waitForPageToLoad(pageLoadTimeout);
 		addAnimalToDec("21");
 		addAnimalToDec("22");
 		addAnimalToDec("23");
 		addAnimalToDec("24");
 		addAnimalToDec("25");
 		// Remove animals from DEC
 		selenium.click("id=rem0");
 		selenium.click("id=rem1");
 		selenium.click("id=startrem");
 		selenium.waitForPageToLoad(pageLoadTimeout);
 		selenium.click("id=dorem");
 		selenium.waitForPageToLoad(pageLoadTimeout);
 		Assert.assertTrue(selenium.isTextPresent("Animal(s) successfully removed"));
 		selenium.click("link=Back to overview");
 		selenium.waitForPageToLoad(pageLoadTimeout);
 		// Check portal
 		selenium.click("DecStatus_tab_button");
 		selenium.waitForPageToLoad(pageLoadTimeout);
 		Assert.assertTrue(selenium.isTextPresent("DEC status portal"));
 		Assert.assertEquals(selenium.getText("//div[@id='StatusTable']/table/tbody/tr[2]/th"), "12345");
 		Assert.assertEquals(selenium.getText("//div[@id='StatusTable']/table/tbody/tr[3]/td[3]"), "A");
 		Assert.assertEquals(selenium.getText("//div[@id='StatusTable']/table/tbody/tr[3]/td[6]"), "3");
 		Assert.assertEquals(selenium.getText("//div[@id='StatusTable']/table/tbody/tr[3]/td[7]"), "2");
 		
 		sleepHelper("decWorkflow");
 	}
 	
 	@Test(dependsOnMethods={"decWorkflow"})
 	public void yearlyReports() throws Exception {
 		// Go to Report plugin
 		selenium.click("id=YearlyReportModule_tab_button");
 		selenium.waitForPageToLoad(pageLoadTimeout);
 		// Report 4A (default)
 		selenium.click("id=generate");
 		selenium.waitForPageToLoad(pageLoadTimeout);
 		Assert.assertEquals(selenium.getTable("css=#reporttablediv > table.2.0"), "Hamsters");
 		Assert.assertEquals(selenium.getText("//div[@id='reporttablediv']/table/tbody/tr[3]/td[2]"), "0");
 		Assert.assertEquals(selenium.getText("//div[@id='reporttablediv']/table/tbody/tr[3]/td[3]"), "5");
 		Assert.assertEquals(selenium.getText("//div[@id='reporttablediv']/table/tbody/tr[3]/td[4]"), "0");
 		Assert.assertEquals(selenium.getText("//div[@id='reporttablediv']/table/tbody/tr[3]/td[5]/strong"), "20");
 		Assert.assertEquals(selenium.getText("//div[@id='reporttablediv']/table/tbody/tr[3]/td[6]/strong"), "0");
 		Assert.assertEquals(selenium.getText("//div[@id='reporttablediv']/table/tbody/tr[3]/td[7]"), "0");
 		Assert.assertEquals(selenium.getText("//div[@id='reporttablediv']/table/tbody/tr[3]/td[8]"), "0");
 		Assert.assertEquals(selenium.getText("//div[@id='reporttablediv']/table/tbody/tr[3]/td[9]"), "0");
 		Assert.assertEquals(selenium.getText("//div[@id='reporttablediv']/table/tbody/tr[3]/td[10]"), "0");
 		Assert.assertEquals(selenium.getText("//div[@id='reporttablediv']/table/tbody/tr[3]/td[11]"), "0");
 		Assert.assertEquals(selenium.getText("//div[@id='reporttablediv']/table/tbody/tr[3]/td[12]"), "2");
 		Assert.assertEquals(selenium.getText("//div[@id='reporttablediv']/table/tbody/tr[3]/td[13]"), "0");
 		Assert.assertEquals(selenium.getText("//div[@id='reporttablediv']/table/tbody/tr[3]/td[14]"), "0");
 		Assert.assertEquals(selenium.getText("//div[@id='reporttablediv']/table/tbody/tr[3]/td[15]"), "0");
 		Assert.assertEquals(selenium.getText("//div[@id='reporttablediv']/table/tbody/tr[3]/td[16]"), "0");
 		Assert.assertEquals(selenium.getText("//div[@id='reporttablediv']/table/tbody/tr[3]/td[17]"), "0");
 		Assert.assertEquals(selenium.getText("//div[@id='reporttablediv']/table/tbody/tr[3]/td[18]"), "23");
 		// Report 5
 		selenium.select("id=form", "value=5");
 		selenium.click("id=generate");
 		selenium.waitForPageToLoad(pageLoadTimeout);
 		Assert.assertEquals(selenium.getText("//div[@id='reporttablediv']/table/tbody/tr[3]/td[1]"), "12345A - MyDEC");
 		Assert.assertEquals(selenium.getText("//div[@id='reporttablediv']/table/tbody/tr[3]/td[6]"), "2");
 		Assert.assertEquals(selenium.getText("//div[@id='reporttablediv']/table/tbody/tr[3]/td[15]"), "A. Dood in het kader van de proef");
 		Assert.assertEquals(selenium.getText("//div[@id='reporttablediv']/table/tbody/tr[3]/td[16]"), "12345A");
 		Assert.assertEquals(selenium.getText("//div[@id='reporttablediv']/table/tbody/tr[3]/td[17]"), "Mesocricetus auratus");
 		
 		sleepHelper("yearlyReports");
 	}
 	
 	@Test(dependsOnMethods={"yearlyReports"})
 	public void applyProtocol() throws Exception {
 		// Go to Protocol plugin
 		selenium.click("id=valuemenu_tab_button");
 		selenium.waitForPageToLoad(pageLoadTimeout);
 		selenium.click("id=ApplyProtocol_tab_button");
 		selenium.waitForPageToLoad(pageLoadTimeout);
 		// Apply 'SetWeight' protocol on animal '1'
 		selenium.select("id=Protocols", "label=SetWeight");
 		selenium.select("id=Targets", "label=1");
 		selenium.check("name=NewOrEdit value=New");
 		selenium.click("id=TimeBox");
 		selenium.click("id=Select");
 		selenium.waitForPageToLoad(pageLoadTimeout);
 		Assert.assertEquals(selenium.getText("//div[@id='divValueTable']/table/tbody/tr/th[2]"), "Weight");
 		Assert.assertEquals(selenium.getText("//div[@id='divValueTable']/table/tbody/tr/th[3]"), "Weight start");
 		Assert.assertEquals(selenium.getText("//div[@id='divValueTable']/table/tbody/tr/th[4]"), "Weight end");
 		selenium.type("id=0_1_0", "200");
 		selenium.click("id=ApplyStartTime_1");
 		selenium.waitForPageToLoad(pageLoadTimeout);
 		selenium.click("id=Apply");
 		selenium.waitForPageToLoad(pageLoadTimeout);
 		Assert.assertTrue(selenium.isTextPresent("Protocol applied successfully"));
 		// Check in Timeline value viewer
 		selenium.click("id=EventViewer_tab_button");
 		selenium.waitForPageToLoad(pageLoadTimeout);
 		selenium.select("id=animal", "label=1");
		sleepHelper("applyProtocol - Timeline value viewer Ajax call"); // We have to wait here, but it's Ajax, so it's faster than a normal full page load
 		Assert.assertTrue(selenium.isTextPresent("Weight"));
 		Assert.assertTrue(selenium.isTextPresent("200"));
 		
 		sleepHelper("applyProtocol");
 	}
 	
 	@Test(dependsOnMethods={"applyProtocol"})
 	public void logout() throws InterruptedException
 	{
 		selenium.click("id=UserLogin_tab_button");
 		selenium.waitForPageToLoad(pageLoadTimeout);
 		selenium.click("id=Logout");
 		selenium.waitForPageToLoad(pageLoadTimeout);
 		
 		sleepHelper("logout");
 	}
 	
 	@AfterClass(alwaysRun=true)
 	public void stop() throws Exception
 	{
 		selenium.stop();
 		
 		//added to fix TestDatabase which runs after this one...
 		//see comment in TestDatabase!
 		if (!this.tomcat) {
 			new emptyDatabase(new MolgenisServlet().getDatabase(), false);
 		} else {
 			new emptyDatabase(DatabaseFactory.create("apps/animaldb/org/molgenis/animaldb/animaldb.properties"), false);
 		}
 
 		//Helper.deleteStorage();
 		//Helper.deleteDatabase();
 	}
 	
 	private void sleepHelper(String who) throws InterruptedException
 	{
 		System.out.println(who + " done, now sleeping for " + sleepTime + " msec");
 		Thread.sleep(sleepTime);
 	}
 	
 	/**
 	 * Helper method to add an animal to a DEC subproject.
 	 * 
 	 * @param name
 	 */
 	private void addAnimalToDec(String name) {
 		selenium.click("id=startadd");
 		selenium.waitForPageToLoad(pageLoadTimeout);
 		selenium.select("id=animal", "label=" + name);
 		selenium.click("id=doadd");
 		selenium.waitForPageToLoad(pageLoadTimeout);
 		Assert.assertTrue(selenium.isTextPresent("Animal(s) successfully added"));
 		selenium.click("link=Back to overview");
 		selenium.waitForPageToLoad(pageLoadTimeout);
 		Assert.assertTrue(selenium.isTextPresent("Manage animals in DEC subprojects"));
 	}
 
 }
