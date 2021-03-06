 package com.wikia.webdriver.PageObjectsFactory.PageObject;
 
 import java.util.Date;
 import java.util.List;
 import java.util.concurrent.TimeUnit;
 
 import org.openqa.selenium.Alert;
 import org.openqa.selenium.By;
 import org.openqa.selenium.JavascriptExecutor;
 import org.openqa.selenium.NoAlertPresentException;
 import org.openqa.selenium.Point;
 import org.openqa.selenium.TimeoutException;
 import org.openqa.selenium.WebDriver;
 import org.openqa.selenium.WebElement;
 import org.openqa.selenium.interactions.Actions;
 import org.openqa.selenium.support.FindBy;
 import org.openqa.selenium.support.PageFactory;
 import org.openqa.selenium.support.ui.ExpectedConditions;
 import org.openqa.selenium.support.ui.WebDriverWait;
 
 import com.wikia.webdriver.Common.Core.Assertion;
 import com.wikia.webdriver.Common.Core.CommonExpectedConditions;
 import com.wikia.webdriver.Common.Core.CommonFunctions;
 import com.wikia.webdriver.Common.Core.Global;
 import com.wikia.webdriver.Common.Logging.PageObjectLogging;
 import com.wikia.webdriver.PageObjectsFactory.PageObject.SignUp.UserProfilePageObject;
 import com.wikia.webdriver.PageObjectsFactory.PageObject.WikiPage.WikiArticlePageObject;
 
 /**
  * 
  * @author Karol
  *
  */
 
 public class BasePageObject{
 
 	public final WebDriver driver;
 	
 	public String wikiFactoryLiveDomain = "http://community.wikia.com/wiki/Special:WikiFactory";
 		
 	protected int timeOut = 30;
 
 	protected String Domain;
 	
 	protected String articlename;
 	
 	public WebDriverWait wait;
 
 	@FindBy(css="div[class*='wikia-bar'] a.tools-customize[data-name='customize']")
 	protected WebElement customizeToolbar_CustomizeButton;
 	@FindBy(css="div.msg")
 	protected WebElement customizeToolbar_PageWatchlistStatusMessage;
 	@FindBy(css="div.search-box input.search")
 	protected WebElement customizeToolbar_FindAToolField;
 	@FindBy(css="div.MyToolsRenameItem input.input-box")
 	protected WebElement customizeToolbar_RenameItemDialogInput;
 	@FindBy(css="div.MyToolsRenameItem input.save-button")
 	protected WebElement customizeToolbar_SaveItemDialogInput;
 	@FindBy(css="input.save-button")
 	protected WebElement customizeToolbar_SaveButton;
 	@FindBy(css="span.reset-defaults a")
 	protected WebElement customizeToolbar_ResetDefaultsButton;
 	@FindBy(css="li.mytools.menu")
 	protected WebElement customizeToolbar_MyToolsMenuButton;
 	@FindBy(css="ul[id='my-tools-menu']")
 	protected WebElement customizeToolbar_MyToolsMenu;
 	@FindBy(css="#WallNotifications div.notification div.msg-title")
 	protected WebElement notifications_LatestNotificationOnWiki;
 	@FindBy(css="#WallNotifications li")
 	protected WebElement notifications_ShowNotificationsLogo;
 	@FindBy(css="li.notifications-for-wiki")
 	protected WebElement notifications_NotificationsForWiki;
 	@FindBy(css="#wall-notifications-markasread-sub")
 	protected WebElement notifications_MarkAllAsReadButton;
 	@FindBy(css="#wall-notifications-markasread-all-wikis")
 	protected WebElement notifications_MarkAllWikisAsReadButton;
 	@FindBy(css="#wall-notifications-markasread-this-wiki")
 	protected WebElement notifications_MarkOnlyThisWikiAsReadButton;
 	@FindBy(css="input.control-button")
 	private WebElement publishButtonGeneral;
 	@FindBy(css="a#ca-edit")
 	protected WebElement editButton;
 	@FindBy(css=".mw-htmlform-submit") 
 	protected WebElement followSubmit;
 	@FindBy(css="#ca-unwatch")
 	protected WebElement followedButton;
 	@FindBy(css="#ca-watch")
 	protected WebElement unfollowedButton;
 	@FindBy(css = "a[data-canonical='random']")
 	private WebElement randomPageButton;
 	@FindBy(css = ".sprite.search")
 	private WebElement searchButton;
 	
 	@FindBy(css="form.WikiaSearch")
 	WebElement wikiaSearch_searchForm;
 	@FindBy(css="section.modalContent div.UserLoginModal form")
 	WebElement modalLoginForm;
 	
 	@FindBy(css="a[data-id='shareButton']")
 	WebElement shareButton;
 	@FindBy(css="iframe.twitter-share-button")
 	WebElement twitterIframe;
 	@FindBy(css="a#b")
 	WebElement twitterButton;	
 	@FindBy(css="iframe.fb_ltr")
 	WebElement fBIframe;
 	@FindBy(css="div.pluginConnectButton .pluginConnectButtonDisconnected button")
 	WebElement fBLikeButton;	
 	@FindBy(css="a.email-link")
 	WebElement emailButton;
 	@FindBy(css="a[id='ok']")
 	WebElement emailModalSendButton;
 	@FindBy(css="a[id='cancel']")
 	WebElement emailModalCancelButton;
 	@FindBy(css="button.wikia-chiclet-button")
 	WebElement emailModalCloseButton;
 	@FindBy(css="input#lightbox-share-email-text")
 	WebElement emailModalEmailInputField;
 	@FindBy(css="section.modalWrapper")
 	WebElement logInModal;
 	
 	private By customizeToolbar_ToolsList = By.cssSelector("ul.tools li");
 	private By customizeToolbar_MyToolsList = By.cssSelector("ul[id='my-tools-menu'] a");
 	
 	
 	public BasePageObject(WebDriver driver)
 	{
 		this.driver = driver;
 		this.Domain = Global.DOMAIN;
 		wait = new WebDriverWait(driver, timeOut);
 		PageFactory.initElements(driver, this);
 		driver.manage().window().maximize();
 	}
 	
 	/**
 	 * Click  on Publish button
 	 *  
 	 * @author Michal Nowierski
 	 */
 	public WikiArticlePageObject clickOnPublishButton() {
 		mouseOver("#GlobalNavigation li:nth(1)");
 		mouseRelease("#GlobalNavigation li:nth(1)");
 		waitForElementByElement(publishButtonGeneral);
 		waitForElementClickableByElement(publishButtonGeneral);
 //		clickAndWait(publishButtonGeneral);
 		jQueryClick("input.control-button");
 		waitForElementByElement(editButton);
 		PageObjectLogging.log("ClickOnPublishButton", "Click on 'Publish' button", true, driver);
 	
 		return new WikiArticlePageObject(driver, Domain, articlename);
 	}
 	
 
 	/**
 	 * Click  on Publish button
 	 *  
 	 * @author Michal Nowierski
 	 */
 	public WikiArticlePageObject clickOnPublishButtonAndCheckJSalertNotThere(String alertMessage) {
 		waitForElementByElement(publishButtonGeneral);
 		waitForElementClickableByElement(publishButtonGeneral);
 		clickAndWait(publishButtonGeneral);
 		checkJSalertIsThere(alertMessage, false);
 		waitForElementByElement(editButton);
 		PageObjectLogging.log("clickOnPublishButtonAndCheckJSalertNotThere", "Click on 'Publish' button and check there is no JS alert", true, driver);
 		
 		return new WikiArticlePageObject(driver, Domain, articlename);
 	}
 	
 
 	/**
 	 * Checks page title
 	 *
 	 ** @param title Specifies the title that you want to compare with the actual current title
 	 */
 
 	public boolean verifyTitle(String title)
 	{
 		String currentTitle = driver.getTitle();
 		if (!currentTitle.equals(title))
 		{
 			return false;
 		}
 		return true;
 	}
 	
 	/**
 	 * Checks if the current URL contains the given String
 	 *
 	 *  @author Michal Nowierski
 	 ** @param GivenString 
 	 */
 	public void verifyURLcontains(String GivenString)
 	{
 		String currentURL = driver.getCurrentUrl();
 		if (currentURL.contains(GivenString))
 		{
 			PageObjectLogging.log("verifyURLcontains", "current url is the same as expected url", true, driver);
 		}
 		else
 		{
 			PageObjectLogging.log("verifyURLcontains", "current url isn't the same as expetced url", false, driver);
 		}
 	}
 	
 	/**
 	 * Checks if the current URL is the given URL
 	 *
 	 *  @author Michal Nowierski
 	 ** @param GivenURL 
 	 */
 	public boolean verifyURL(String GivenURL)
 	{
 		String currentURL = driver.getCurrentUrl();
 		if (currentURL.equals(GivenURL))
 		{
 			PageObjectLogging.log("verifyURL", "Given URL matches actual URL", true);
 			return true;
 		}
 		else {
 			PageObjectLogging.log("verifyURL", "Given URL: "+GivenURL+", does not match actual URL: "+currentURL, false);
 			return false;
 		}
 		
 	}
 	
 	public String getCurrentUrl()
 	{
 		System.out.println(driver.getCurrentUrl());
 		return driver.getCurrentUrl();
 	}
 	
 	
 	
 	/**
 	 * Clicks on an element
 	 */
 
 	public void click(WebElement pageElem)
 	{
 		pageElem.click();	
 	}
 	
 	public void getUrl(String url)
 	{
 		try
 		{
 			driver.get(url);
 		}
 		catch(TimeoutException e)
 		{
 			PageObjectLogging.log("getUrl", "page loaded for more then 30 seconds after click", true);
 		}
 	}
 	
 	public void refreshPage()
 	{
 		try{
 			driver.navigate().refresh();			
 			PageObjectLogging.log("refreshPage", "page refreshed", true);
 		}
 		catch(TimeoutException e)
 		{
 			PageObjectLogging.log("refreshPage", "page loaded for more then 30 seconds after click", true);
 		}
 	}
 
 	
 	public void clickAndWait(WebElement pageElem)
 	{
 		try{
 			CommonFunctions.scrollToElement(pageElem);
 			pageElem.click();
 			
 		}
 		catch(TimeoutException e)
 		{
 			PageObjectLogging.log("clickAndWait", "page loaded for more then 30 seconds after click", true);
 		}
 	}
 	
 	/**
 	 * Send keys to WebElement
 	 */
 
 	public void sendKeys(WebElement pageElem, String KeysToSend)
 	{
 		try
 		{
 			pageElem.sendKeys(KeysToSend);			
 		}
 		catch(Exception e)
 		{
 			PageObjectLogging.log("sendKeys", e.toString(), false);			
 		}
 	}
 	
 	/**
 	 * Clicks on an element using Actions click method
 	 * 
 	 * @author Michal Nowierski
 	 * ** @param pageElem The WebElement to be clicked on 
 	 */
 	public void clickActions(WebElement pageElem)
 	{
 		try
 		{
 			Actions builder = new Actions(driver);
 			Actions click = builder.click(pageElem);
 			click.perform();				
 		}
 		catch(Exception e)
 		{
 			PageObjectLogging.log("clickActions", e.toString(), false);			
 		}
 	}
 	
 	/**
 	 * Sends Keys to an element within iFrame, using jQuery
 	 * 
 	 * @author Michal Nowierski
 	 * ** @param cssSelecotr The WebElement to which keys will be sent
 	 * ** @param text the text to be sent
 	 * 
 	 */
 	
 	public void sendKeysInArticleIframe(String cssSelecotr, String text)
 	{
 		executeScript("$($($('iframe[title*=\"Rich\"]')[0].contentDocument.body).find('"+cssSelecotr+"')).text('"+text+"')");
 		try {
 			Thread.sleep(500);
 		} catch (InterruptedException e) {
 			e.printStackTrace();
 		}
 	}
 	
 	public void mouseOverInArticleIframe(String cssSelecotr)
 	{
 		executeScript("$($($('iframe[title*=\"Rich\"]')[0].contentDocument.body).find('"+cssSelecotr+"')).mouseenter()");
 		try {
 			Thread.sleep(500);
 		} catch (InterruptedException e) {
 			e.printStackTrace();
 		}
 	}
 
 	public void mouseReleaseInArticleIframe(String cssSelecotr)
 	{
 		executeScript("$($($('iframe[title*=\"Rich\"]')[0].contentDocument.body).find('"+cssSelecotr+"')).mouseleave()");
 		try {
 			Thread.sleep(500);
 		} catch (InterruptedException e) {
 			e.printStackTrace();
 		}
 	}
 	
 	public void mouseOver(String cssSelecotr)
 	{
 		executeScript("$('"+cssSelecotr+"').mouseenter()");
 		try {
 			Thread.sleep(500);
 		} catch (InterruptedException e) {
 			e.printStackTrace();
 		}
 	}
 	
 	public void mouseRelease(String cssSelecotr)
 	{
 		executeScript("$('"+cssSelecotr+"').mouseleave()");
 		try {
 			Thread.sleep(500);
 		} catch (InterruptedException e) {
 			e.printStackTrace();
 		}
 	}
 	
 	
 	public void jQueryClick(String cssSelector)
 	{
 		executeScript("$('"+cssSelector+"').click()");
 	}
 	
 	/**
 	 * Click on nth element with given css
 	 * 
 	 * @author Michal Nowierski
 	 * ** @param n - the indicator of element from those which match the css selector  
 	 */
 	public void jQueryNthElemClick(String cssSelector, int n)
 	{
 		executeScript("$('"+cssSelector+"')["+n+"].click()");
 	}
 	
 	public void jQueryFocus(String cssSelector)
 	{
 		executeScript("$('"+cssSelector+"').focus()");
 	}
 	
 	/**
 	 * Returns parent element of the given element
 	 * 
 	 * @author Michal Nowierski
 	 * ** @param childElement - the element whose parent we are looking for
 	 */
 	public WebElement getParentElement(WebElement childElement) {
 		return childElement.findElement(By.xpath(".."));
 	}
 
 	public void executeScript(String script)
 	{
 		JavascriptExecutor js = (JavascriptExecutor) driver;
 		js.executeScript(script);
 		try {
 			Thread.sleep(1000);
 		} catch (InterruptedException e) {
 			e.printStackTrace();
 		}
 	}
 	
 	public String executeScriptRet(String script)
 	{
 		JavascriptExecutor js = (JavascriptExecutor) driver;
 		return (String) js.executeScript("return "+script);
 	}
 	
 	protected void executeScript(String script, WebDriver driver)
 	{
 		JavascriptExecutor js = (JavascriptExecutor) driver;
 		js.executeScript(script);
 		try {
 			Thread.sleep(1000);
 		} catch (InterruptedException e) {
 			e.printStackTrace();
 		}
 	}
 	
 	public void removeCssClass(String cssSelector, String className){
 		executeScript("$('."+cssSelector+"').removeClass('"+className+"')");
 		PageObjectLogging.log("removeCssClass", className+" removed for selector: "+cssSelector, true, driver);
 	}
 	
 	public void addCssClass(String cssSelector, String className){
 		executeScript("$('."+cssSelector+"').addClass('"+className+"')");
 		PageObjectLogging.log("removeCssClass", className+" removed for selector: "+cssSelector, true, driver);
 	}
 	
 	/**
 	 * Returns List of WebElements by CssSelector
 	 * 
 	 * @author Michal Nowierski
 	 * ** @param Selector  
 	 */
 	public List<WebElement> getListOfElementsByCss(String Selector) 
 	{
 		try
 		{
 			return driver.findElements(By.cssSelector(Selector));						
 		}
 		catch(Exception e)
 		{
 			PageObjectLogging.log("getListOfElementsByCss", e.toString(), false);
 			return null;
 		}
 	}
 	/**
 	 * Checks if the element is visible on browser
 	 *
 	 ** @param by The By class defined for the element
 	 */
 	public WebElement waitForElementByBy(By by)
 	{
 			wait.until(ExpectedConditions.visibilityOfElementLocated(by));		
 			return driver.findElement(by);
 	}
 	
 	/**
 	 * Checks if the element is visible on browser
 	 *
 	 ** @param element The element to be checked
 	 * @throws Exception 
 	 */
 	public void waitForElementByElement(WebElement element)
 	{
 			wait.until(ExpectedConditions.visibilityOf(element));
 	}
 
 	/**
 	 * Checks if the element is present in DOM
 	 *
 	 * @param element The element to be checked
 	 */
 	public void waitForElementPresenceByBy(By locator)
 	{
 			wait.until(ExpectedConditions.presenceOfElementLocated(locator));
 
 	}
 	
 	public WebElement waitForElementByCss(String cssSelector)
 	{
 			wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector(cssSelector)));
 			return driver.findElement(By.cssSelector(cssSelector));
 	}
 	
 	public void waitForElementByClassName(String className)
 	{							
 			wait.until(ExpectedConditions.visibilityOfElementLocated(By.className(className)));
 	}
 	
 	public void waitForElementByClass(String id)
 	{							
 			wait.until(ExpectedConditions.visibilityOfElementLocated(By.id(id)));
 	}
 	
 	public WebElement waitForElementByXPath(String xPath)
 	{
 			wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath(xPath)));
 			return driver.findElement(By.xpath(xPath));
 	}
 	
 	public void waitForElementNotVisibleByCss(String css)
 	{
			wait.until(ExpectedConditions.invisibilityOfElementLocated(By.cssSelector(css)));								
 	}
 	
 	public void waitForElementNotVisibleByBy(By by)
 	{
 			Global.LOG_ENABLED = false;
 			wait.until(CommonExpectedConditions.invisibilityOfElementLocated(by));
 			Global.LOG_ENABLED = true;
 	}
 	
 	public void waitForElementNotVisibleByElement(WebElement element)
 	{
 		try
 		{
 			Global.LOG_ENABLED = false;			
 			wait.until(CommonExpectedConditions.invisibilityOfElementLocated(element));						
 			Global.LOG_ENABLED = true;
 		}
 		catch(Exception e)
 		{
 			PageObjectLogging.log("waitForElementNotVisibleByElement", e.toString(), false);			
 		}
 	}
 	
 	public void waitForElementClickableByClassName(String className)
 	{
 			wait.until(ExpectedConditions.elementToBeClickable(By.className(className)));								
 
 	}
 	
 	public void waitForElementClickableByCss(String css)
 	{
 
 			wait.until(ExpectedConditions.elementToBeClickable(By.cssSelector(css)));								
 	}
 	
 	public void waitForElementClickableByBy(By by)
 	{
 
 			wait.until(ExpectedConditions.elementToBeClickable(by));								
 	}
 	
 	public void waitForElementClickableByElement(WebElement element)
 	{
 			wait.until(CommonExpectedConditions.elementToBeClickable(element));								
 	}
 	public void waitForElementNotClickableByElement(WebElement element)
 	{
 			wait.until(CommonExpectedConditions.elementNotToBeClickable(element));								
 	}
 	
 	public void waitForElementById(String id)
 	{
 			wait.until(ExpectedConditions.visibilityOfElementLocated(By.id(id)));								
 	}
 
 	
 	
 	public void waitForValueToBePresentInElementsAttributeByCss(String selector, String attribute, String value)
 	{
 			wait.until(CommonExpectedConditions.valueToBePresentInElementsAttribute(By.cssSelector(selector), attribute, value));								
 	}
 	
 	public void waitForValueToBePresentInElementsAttributeByElement(WebElement element, String attribute, String value)
 	{
 			wait.until(CommonExpectedConditions.valueToBePresentInElementsAttribute(element, attribute, value));								
 	}
 
 	public void waitForValueToNotBePresentInElementsAttributeByCss(String selector, String attribute, String value)
 	{
 			wait.until(CommonExpectedConditions.valueToNotBePresentInElementsAttribute(By.cssSelector(selector), attribute, value));								
 	}
 	
 	public void waitForTextToBePresentInElementByElement(WebElement element, String text)
 	{
 			wait.until(CommonExpectedConditions.textToBePresentInElement(element, text));								
 		
 	}
 	
 	public void waitForTextToBePresentInElementByBy(By by, String text)
 	{
 			WebElement temp = driver.findElement(by);
 			wait.until(CommonExpectedConditions.textToBePresentInElement(temp, text));										
 	}
 
 	public void waitForClassRemovedFromElement(WebElement element, String className)
 	{
 			wait.until(CommonExpectedConditions.classRemovedFromElement(element, className));								
 	}
 
 	public void waitForStringInURL(String givenString)
 	{						
 			wait.until(CommonExpectedConditions.givenStringtoBePresentInURL(givenString));
 	}
 	
 	/**
 	 * Navigates back to the previous page
 	 * 
 	 * @author Michal Nowierski
 	 */
 	public void navigateBack()
 	{
 		try
 		{								
 			driver.navigate().back();
 			PageObjectLogging.log("navigateBack", "succesfully navigated back", true);			
 		}
 		catch(Exception e)
 		{
 			PageObjectLogging.log("navigateBack", e.toString(), false);			
 		}
 	}
 	
 	public void showToolbar()
 	{
 		executeScript("$('div#WikiaBarWrapper').attr('class', 'WikiaBarWrapper')");
 		executeScript("$('div#WikiaBarWrapper').attr('class', 'WikiaBarWrapper')");
 	}
 	
 	/**
 	 * Verifies that user toolbar buttons are visible
 	 */
 	public void verifyUserToolBar()
 	{
 		waitForElementByCss("div.toolbar ul.tools li.overflow");
 		waitForElementByCss("div.toolbar ul.tools li.mytools");
 		waitForElementByCss("div.toolbar ul.tools li a.tools-customize");
 		PageObjectLogging.log("verifyUserToolBar", "user toolbar verified", true, driver);
 	}
 	
 	/**
 	 * Clicks on "Customize" button. User must be logged in.
 	 * 
 	 * @author Michal Nowierski
 	 */
 	public void customizeToolbar_ClickCustomize() {
 		waitForElementByElement(customizeToolbar_CustomizeButton);
 		waitForElementClickableByElement(customizeToolbar_CustomizeButton);
 		click(customizeToolbar_CustomizeButton);
 		PageObjectLogging.log("customizeToolbar_ClickCustomize", "Clicks on 'Customize' button.", true, driver);
 		
 	}
 	
 	/**
 	 * Clicks on "ResetDefaults" button.
 	 * 	 
 	 * @author Michal Nowierski
 	 */
 	public void customizeToolbar_ClickOnResetDefaults() {
 		waitForElementByElement(customizeToolbar_ResetDefaultsButton);
 		waitForElementClickableByElement(customizeToolbar_ResetDefaultsButton);
 		clickAndWait(customizeToolbar_ResetDefaultsButton);
 		PageObjectLogging.log("customizeToolbar_ClickOnResetDefaults", "Click on 'ResetDefaults' button.", true, driver);
 		
 	}
 	
 	/**
 	 * Types GivenString to Find A Tool field
 	 * 
 	 * @param GivenString String to be typed into search field 
 	 * @author Michal Nowierski
 	 */
 	public void customizeToolbar_TypeIntoFindATool(String GivenString) {
 		waitForElementByElement(customizeToolbar_FindAToolField);
 		waitForElementClickableByElement(customizeToolbar_FindAToolField);
 		customizeToolbar_FindAToolField.clear();
 		customizeToolbar_FindAToolField.sendKeys(GivenString);
 		PageObjectLogging.log("customizeToolbar_TypeIntoFindATool", "Type "+GivenString+" into Find A Tool field", true, driver);
 		
 	}
 	
 	/**
 	 * Types GivenString to Find A Tool field
 	 * 
 	 * @param GivenString new name for the Tool
 	 * @author Michal Nowierski
 	 */
 	public void customizeToolbar_TypeIntoRenameItemDialog(String GivenString) {
 		waitForElementByElement(customizeToolbar_RenameItemDialogInput);
 		waitForElementClickableByElement(customizeToolbar_RenameItemDialogInput);
 		customizeToolbar_RenameItemDialogInput.clear();
 		customizeToolbar_RenameItemDialogInput.sendKeys(GivenString);
 		PageObjectLogging.log("customizeToolbar_TypeIntoRenameItemDialog", "Type "+GivenString+" into rename item input", true, driver);
 	}
 	
 	/**
 	 * Clicks on "save" button on Rename Item dialog.
 	 * 	 
 	 * @author Michal Nowierski
 	 */
 	public void customizeToolbar_saveInRenameItemDialog() {
 		waitForElementByElement(customizeToolbar_SaveItemDialogInput);
 		waitForElementClickableByElement(customizeToolbar_SaveItemDialogInput);
 		clickAndWait(customizeToolbar_SaveItemDialogInput);
 		PageObjectLogging.log("customizeToolbar_saveInRenameItemDialog", "Click on 'save' button on Rename Item dialog.", true, driver);
 		
 	}
 	
 	/**
 	 * Click on a Tool after searching for it
 	 * 
 	 * @param Tool toolname appearing on the list of found tools
 	 * @author Michal Nowierski
 	 */
 	public void customizeToolbar_ClickOnFoundTool(String Tool) {
 		waitForElementByCss("div.autocomplete div[title='"+Tool+"']");
 		waitForElementClickableByCss("div.autocomplete div[title='"+Tool+"']");
 		clickAndWait(driver.findElement(By.cssSelector("div.autocomplete div[title='"+Tool+"']")));
 		PageObjectLogging.log("customizeToolbar_ClickOnFoundTool", "Click on "+Tool, true, driver);
 		
 	}
 	
 	/**
 	 * Click on a toolbar tool.
 	 * 
 	 * @param data-name data-name of the toolbar tool. <br> You should check the data-name of the tool you want to click.
 	 * @author Michal Nowierski
 	 */
 	public void customizeToolbar_ClickOnTool(String Tool_dataname) {
 		waitForElementByCss("li.overflow a[data-name='"+Tool_dataname+"']");
 		WebElement element = driver.findElement(By.cssSelector("li.overflow a[data-name='"+Tool_dataname+"']"));
 		if (Global.BROWSER.equals("IE")) {
 			// clicking on parent element of the above 'a' element, because IE couldn't click on the above 'a' element
 			// Unfortunately Firefox can't click on this parent element, so the code must be browser-dependent
 			WebElement parent = element.findElement(By.xpath(".."));
 			waitForElementClickableByElement(parent);
 			clickAndWait(parent);
 		}
 		else {
 			waitForElementClickableByElement(element);
 			clickAndWait(element);
 		}
 		PageObjectLogging.log("customizeToolbar_ClickOnFoundTool", "Click on "+Tool_dataname, true, driver);
 	}
 	
 	/**
 	 * Click on a toolbar tool.
 	 * 
 	 * @param data-name data-name of the toolbar tool. <br> You should check the data-name of the tool you want to click.
 	 * @author Michal Nowierski
 	 */
 	public void customizeToolbar_VerifyPageWatchlistStatusMessage() {
 		waitForElementByElement(customizeToolbar_PageWatchlistStatusMessage);
 		PageObjectLogging.log("customizeToolbar_VerifyPageWatchlistStatusMessage", "Verify that the page watchlist status message appeared ", true, driver);
 		
 	}
 	
 	/**
 	 * Verify that page is followed
 	 * The method should be used only after clicking on "follow" button. Before that, "follow" button does not have 'title' attribute which is necessary in the method
 	 * 
 	 * @author Michal Nowierski
 	 */
 	public void customizeToolbar_VerifyPageFollowed() {
 		waitForElementByCss("a[data-name='follow']");
 		waitForValueToBePresentInElementsAttributeByCss("a[data-name='follow']", "title", "Unfollow");
 		PageObjectLogging.log("customizeToolbar_VerifyPageFollowed", "Verify that page is followed", true, driver);
 	
 	}
 	
 	/**
 	 * Verify that page is unfollowed
 	 * The method should be used only after clicking on "Unfollow" button. Before that, "follow" button does not have 'title' attribute which is necessary in the method
 	 * 
 	 * @author Michal Nowierski
 	 */
 	public void customizeToolbar_VerifyPageUnfollowed() {
 		waitForElementByElement(customizeToolbar_PageWatchlistStatusMessage);
 		waitForValueToBePresentInElementsAttributeByCss("a[data-name='follow']", "title", "Follow");
 		PageObjectLogging.log("customizeToolbar_VerifyPageUnfollowed", "Verify that page is unfollowed", true, driver);
 		
 	}
 	
 	/**
 	 * Look up if Tool appears on Toolbar List
 	 * 
 	 * @param Tool {Follow, Edit, History, (...)} 
 	 * @author Michal Nowierski
 	 */
 	public void customizeToolbar_VerifyToolOnToolbarList(String Tool) {
 		waitForElementByCss("ul.options-list li[data-caption='"+Tool+"']");
 		PageObjectLogging.log("customizeToolbar_VerifyToolOnToolbarList", "Check if "+Tool+" appears on list", true);
 	
 	}
 	
 	/**
 	 * Look up if Tool does not appear on Toolbar List
 	 * 
 	 * @param Tool {Follow, Edit, History, (...)} 
 	 * @author Michal Nowierski
 	 */
 	public void customizeToolbar_VerifyToolNotOnToolbarList(String Tool) {
 		waitForElementByCss("ul.options-list li");
 		waitForElementNotVisibleByCss("ul.options-list li[data-caption='"+Tool+"']");
 		PageObjectLogging.log("customizeToolbar_VerifyToolNotOnToolbarList", "Check if "+Tool+" does not appear on Toolbar list", true, driver);
 	}
 	
 	/**
 	 * Remove a wanted Tool by its data-caption
 	 * 
 	 * @param Tool ID of tool to be removed. {Follow, Edit, History, (...)} 
 	 * @author Michal Nowierski
 	 */
 	public void customizeToolbar_ClickOnToolRemoveButton(String Tool) {
 		jQueryClick("ul.options-list li[data-caption=\""+Tool+"\"] img.trash");
 		PageObjectLogging.log("customizeToolbar_ClickOnToolRemoveButton", "Remove Tool with id "+Tool+" from Toolbar List", true, driver);
 	}
 	
 	/**
 	 * Rename the wanted Tool
 	 * 
 	 * @param ToolID ID of tool to be removed. {PageAction:Follow, PageAction:Edit, PageAction:History, (...)} 
 	 * @author Michal Nowierski
 	 */
 	public void customizeToolbar_ClickOnToolRenameButton(String ToolID) {
 		By By1 = By.cssSelector("ul.options-list li[data-caption='"+ToolID+"']");
 		waitForElementByBy(By1);
 		jQueryClick("ul.options-list li[data-caption=\""+ToolID+"\"] img.edit-pencil");
 		PageObjectLogging.log("customizeToolbar_ClickOnToolRenameButton", "Rename the "+ToolID+" Tool", true, driver);
 	}
 	
 	/**
 	 * Drag the wanted Tool
 	 * 
 	 * @param ToolID ID of tool to be dragged. {PageAction:Follow, PageAction:Edit, PageAction:History, (...)}
 	 * @param DragDirection The direction of dragging. e.g -1 is 'drop the tool one item below'
 	 * @author Michal Nowierski
 	 */
 	public void customizeToolbar_DragElemAndDrop(String ToolID, int DragDirection) {
 		By By1 = By.cssSelector("ul.options-list li[data-caption='"+ToolID+"']");
 		By By2 = By.cssSelector("ul.options-list li[data-caption='"+ToolID+"'] img.drag");
 		Point Elem1_location = driver.findElement(By1).getLocation();
 		CommonFunctions.MoveCursorToElement(Elem1_location);
 		waitForElementByBy(By2);
 		waitForElementClickableByBy(By2);
 		Point Elem2_location = driver.findElement(By2).getLocation();
 		CommonFunctions.MoveCursorToElement(Elem2_location);
 		if (Global.BROWSER.equals("FF")) {
 			// Firefox is unable to drag and drop customize toolbar elements using actions class. Able to do it with robot class
 			CommonFunctions.DragFromCurrentCursorPositionAndDrop(0, 25*DragDirection+8);
 		}
 		else {		
 			// Chrome is unable to drag and drop customize toolbar elements using robot class. Able to do it with actions class
 			WebElement draggable = driver.findElement(By2); 
 			new Actions(driver).dragAndDropBy(draggable, 0, 25*DragDirection+8).perform();  
 			
 		}
 		PageObjectLogging.log("customizeToolbar_DragElemAndDrop", "Drag element "+ToolID+", by "+DragDirection, true, driver);
 	}
 	
 	/**
 	 * Check the order of two first tools on My tools list
 	 * 
 	 * @param tool1 The first tool to appear on My Tools list. {History, What links here, (...)} 
 	 * @param tool2 The second tool to appear on My Tools list. {History, What links here, (...)} 
 	 * @author Michal Nowierski
 	 */
 	public void customizeToolbar_VerifyMyToolsOrder(String tool1, String tool2) {
 		CommonFunctions.MoveCursorTo(0, 100);		
 		CommonFunctions.MoveCursorTo(0, 0);		
 		waitForElementByElement(customizeToolbar_MyToolsMenuButton);
 		Point location = customizeToolbar_MyToolsMenuButton.getLocation();
 		try {Thread.sleep(1000);} catch (InterruptedException e) {}
 		CommonFunctions.MoveCursorToElement(location);
 		waitForElementByElement(customizeToolbar_MyToolsMenu);
 		List<WebElement> MyToolsList = driver.findElements(customizeToolbar_MyToolsList);
 		String ActualTool1=MyToolsList.get(0).getText();
 		String ActualTool2=MyToolsList.get(1).getText();
 		if (!tool1.equals(ActualTool1)) {
 			PageObjectLogging.log("customizeToolbar_VerifyMyToolsOrder", ActualTool1+" where "+tool1+" should be. Drag & drop action (from previous step) must hadn't been succesful", false, driver);
 		}
 		if (!tool2.equals(ActualTool2)) {
 			PageObjectLogging.log("customizeToolbar_VerifyMyToolsOrder", ActualTool2+" where "+tool2+" should be. Drag & drop action (from previous step) must hadn't been succesful", false, driver);
 		}
 		PageObjectLogging.log("customizeToolbar_VerifyMyToolsOrder", "Verify that My Tools list has"+tool2+" appearing after "+tool1, true, driver);
 	}
 
 	/**
 	 * Click on save button on customize toolbar
 	 * 
 	 * @author Michal Nowierski
 	 */
 	public void customizeToolbar_ClickOnSaveButton() {
 		waitForElementByElement(customizeToolbar_SaveButton);
 		waitForElementClickableByElement(customizeToolbar_SaveButton);
 		clickAndWait(customizeToolbar_SaveButton);
 		PageObjectLogging.log("customizeToolbar_ClickOnSaveButton", "Click on 'save' button.", true, driver);
 		
 	}
 	
 	public void customizeToolbar_VerifyToolOnToolbar(String ToolName)
 	{
 		waitForElementByXPath("//ul[@class='tools']//a[contains(text(), '"+ToolName+"')]");
 		PageObjectLogging.log("customizeToolbar_VerifyToolOnToolbar","Verify that "+ToolName+" appears in Toolbar.", true);
 	}
 	
 	/**
 	 * <p> Verify that wanted Tool appears in Toolbar. <br> 
 	 * The method finds all of Tools appearing in Toolbar (by their name), and checks if there is at least one name which fits the given param (ToolName)
 	 * 
 	 * @param ToolName Tool to be verified (name that should appear on toolbar)
 	 * @author Michal Nowierski
 	 */
 	public void customizeToolbar_UnfollowIfPageIsFollowed() {
 		List<WebElement> List = driver.findElements(customizeToolbar_ToolsList);
 		for (int i = 0; i < List.size(); i++) {
 			if (List.get(i).getText().equals("Following")) {
 				customizeToolbar_ClickOnTool("follow");
 				customizeToolbar_VerifyPageWatchlistStatusMessage();
 				wait.until(ExpectedConditions.textToBePresentInElement(customizeToolbar_ToolsList, "Follow"));
 			
 			}
 		}
 		PageObjectLogging.log("customizeToolbar_UnfollowIfPageIsFollowed",
 				"If the page is Followed, unfollow it (preconditions assurance)", true, driver);
 
 	}
 	
 	public void customizeToolbar_VerifyToolNotOnToolbar(String ToolName)
 	{
 		By tool = By.xpath("//ul[@class='tools']//a[contains(text(), '"+ToolName+"')]");
 		waitForElementNotVisibleByBy(tool);
 		PageObjectLogging.log("customizeToolbar_VerifyToolNotOnToolbar","Verify that "+ToolName+" tool does not appear in Toolbar.", true);
 	}
 	
 	/**
 	 * verify that wikia search field is displayed
 	 * 
 	 * @author Michal Nowierski
 	 */	
 	public void verifyWikiaSearchFieldIsDisplayed() {
 		waitForElementByElement(wikiaSearch_searchForm);
 		PageObjectLogging.log("verifyWikiaSearchFieldIsDisplayed", "verify that wikia search field is displayed", true);
 	}
 	
 	public String getTimeStamp()
 	{
 		Date time = new Date();
 		long timeCurrent = time.getTime();
 		return String.valueOf(timeCurrent);
 	} 
 	
 	public void verifyModalLoginAppeared()
 	{
 		waitForElementByElement(modalLoginForm);
 		PageObjectLogging.log("verifyModalLogin", "verify modal login form is displayed", true, driver);
 	}
 	
 	public UserProfilePageObject navigateToProfilePage(String domain, String userName) {
 		driver.navigate().to(domain+"wiki/User:"+userName);
 		PageObjectLogging.log("UserProfilePageObject ", "navigate to username page: "+domain+"wiki/User:"+userName, true, driver);	        			
 		return new UserProfilePageObject(driver);
 	}
 	
 	/**
 	 * <p> Verify if js alert is or isn't there. You can expect alert with certain message, or not expect alert with certain message <br> 
 	 * 
 	 * @param alert message that we do or do not expect 
 	 * @param ifAlertExpected  if we expect JS alert - true. If we don't expect JS alert - false 
 	 * @author Michal Nowierski
 	 */
 	public void checkJSalertIsThere(String alertMessage, Boolean ifAlertExpected) {
 
 	    try 
 	    { 
 	    	Thread.sleep(1000);
 	    	Alert alert =  driver.switchTo().alert(); 
 	       	if (alert.getText().equals(alertMessage)) {
 	    		alert.accept();
 	    		PageObjectLogging.log("checkJSalertIsThere", "We expect an alret = "+ifAlertExpected+". JS alert found", ifAlertExpected, driver);	 
 			}
 	       else {
 	    	   alert.accept();
 	    	   PageObjectLogging.log("checkJSalertIsThere", "We expect an alret = "+ifAlertExpected+". JS alert found, and it has unexpected message: "+alert.getText()+" while it should be: "+alertMessage, false, driver);	        			
 		
 	       } 
 	    }  
 	    catch (NoAlertPresentException Ex) 
 	    { 
 	    	PageObjectLogging.log("checkJSalertIsThere", "We expect an alret = "+ifAlertExpected+". JS alert not found", !ifAlertExpected, driver);	    	
 	    } 
 	    catch (InterruptedException e) {
 			
 		}  	
 	}
 	
 
 	public void openWikiPage() {
 		String temp = Domain;
 		try {
 			temp = Domain + "?noexternals=1";
 			getUrl(temp);
 		} catch (TimeoutException e) {
 			PageObjectLogging.log("logOut",
 					"page loads for more than 30 seconds", true);
 		}
 		waitForElementByCss("a[class*=hub]");
 		executeScript("$('ul#pagehistory li:nth-child(1) .mw-history-undo')");
 	}
 
 	public void openRandomArticle() {
 		clickAndWait(randomPageButton);
 		waitForElementByElement(searchButton);
 		PageObjectLogging.log("openRandomArticle",
 				"random page button clicked", true, driver);
 	}
 	
 	public void openRandomArticleByUrl() {
 		navigateToRandomPage();
 		waitForElementByElement(searchButton);
 		PageObjectLogging.log("openRandomArticle",
 				"random page button clicked", true, driver);
 	}
 	
 	private void navigateToRandomPage() {
 		String temp = Domain;
 		try {
 			temp = Domain + "wiki/Special:Random";
 			getUrl(temp);
 		} catch (TimeoutException e) {
 			PageObjectLogging.log("logOut",
 					"page loads for more than 30 seconds", true);
 		}
 		
 	}
 	public void clickShareButton() {
 		
 		waitForElementByElement(shareButton);
 		shareButton.click();
 		PageObjectLogging.log("clickShareButton", "Share button was clicked", true, driver);
 	}
 	
 	public void verifyTwitterIframeVisibility() {
 		
 		waitForElementByElement(twitterIframe);
 		PageObjectLogging.log("VerifyTwitterIframePresence", "Verify that the Twitter Iframe Is Present", true, driver);
 	}
 	
 	
 	public void verifyFBIframeVisibility() {
 		
 		waitForElementByElement(fBIframe);
 		PageObjectLogging.log("VerifyFBIframeVisibility", "Verify that the FB Iframe Is Present", true, driver);
 	}
 
 	public void verifyEmailButtonVisibility() {
 		
 		waitForElementByElement(emailButton);
 		PageObjectLogging.log("verifyEmailButtonVisibility", "Verify that the Email Button Is Present", true, driver);
 	}
 	
 	public void navigteTweetButtonUrl() {
 		
 		waitForElementByElement(twitterIframe);
 		driver.switchTo().frame(twitterIframe);
 		String href = twitterButton.getAttribute("href");
 		driver.switchTo().defaultContent();
 		getUrl(href);
 		PageObjectLogging.log("clickTweetButton", "Twitter button was clicked", true, driver);
 		
 	}
 	
 	public void clickFBLikeButton() {
 		
 		waitForElementByElement(fBIframe);
 		driver.switchTo().frame(fBIframe);
 		fBLikeButton.click();
 		driver.switchTo().defaultContent();
 		PageObjectLogging.log("clickFBLikeButton", "FB Like button was clicked", true, driver);
 	}
 	
 	public void clickEmailButton() {
 		
 		waitForElementByElement(emailButton);
 		emailButton.click();
 		PageObjectLogging.log("clickEmailButton", "Email button was clicked", true, driver);
 	}
 	
 	public void verifyTwitterModalURL() {
 		Assertion.assertStringContains(getCurrentUrl(), "twitter.com");
 		PageObjectLogging.log("VerifyTwitterModalURL", "Verify that the Twitter Modal URL is correct", true, driver);
 	}
 	
 	public void verifyFBModalURL() {
 		
 		CommonFunctions.waitForWindow("", "");
 		Object[] windows = driver.getWindowHandles().toArray();
 		driver.switchTo().window(windows[1].toString());
 		Assertion.assertStringContains(getCurrentUrl(), "facebook.com");
 		driver.switchTo().window(windows[0].toString());
 		PageObjectLogging.log("VerifyFBModalURL", "Verify that the FB Modal URL is correct", true, driver);
 	}
 	
 	public void verifyEmailModalElements() {
 		
 		waitForElementByElement(emailModalSendButton);
 		waitForElementByElement(emailModalCancelButton);
 		waitForElementByElement(emailModalCloseButton);
 		waitForElementByElement(emailModalEmailInputField);
 		PageObjectLogging.log("VerifyEmailModalElements", "Verify that the Email Modal elements are present", true, driver);
 	}
 	
 	public void verifyLogInModalForAnonsVisibility() {	
 		waitForElementByElement(logInModal);
 		PageObjectLogging.log("VerifyLogInModalForAnonsVisibility", "Verify that the Log In modal is present", true, driver);
 	}	
 	
 	public void notifications_verifyLatestNotificationTitle(String title) {
 		notifications_showNotifications();
 		//the below method is native click which is the only way to load notification
 		notifications_clickOnNotificationsLogo();
 		waitForElementByElement(notifications_LatestNotificationOnWiki);
 		waitForTextToBePresentInElementByElement(notifications_LatestNotificationOnWiki, title);
 		PageObjectLogging.log("notifications_verifyNotificationTitle", "Verify that the latest notification has the following title: "+title, true, driver);
 	}
 
 	public void notifications_clickOnNotificationsLogo() {
 		waitForElementByElement(notifications_ShowNotificationsLogo);
 		waitForElementClickableByElement(notifications_ShowNotificationsLogo);
 		clickAndWait(notifications_ShowNotificationsLogo);
 		PageObjectLogging.log("notifications_clickOnNotificationsLogo", "click on notifications logo on the upper right corner", true, driver);				
 	}
 
 	public void notifications_showNotifications() {
 		waitForElementByElement(notifications_ShowNotificationsLogo);
 		executeScript("$('#WallNotifications ul.subnav').addClass('show')");
 		PageObjectLogging.log("norifications_showNotifications", "show notifications by adding 'show' class to element", true, driver);		
 	}
 	
 	public void notifications_showNotificationsForWikiOnMenu() {
 		waitForElementByElement(notifications_NotificationsForWiki);
 		waitForElementClickableByElement(notifications_NotificationsForWiki);
 		clickAndWait(notifications_NotificationsForWiki);
 //		executeScript("$($('li.notifications-for-wiki')[0]).addClass('show')");
 		PageObjectLogging.log("notifications_showNotificationsForWiki", "show the upper wiki notifications on menu", true, driver);		
 	}
 	
 	public void notifications_markLatestNotificationsAsRead() {
 		notifications_showNotifications();
 		notifications_clickMarkAllAsRead(false);	
 	}
 
 	public void notifications_clickMarkAllAsRead(boolean allWikis) {
 		waitForElementByElement(notifications_MarkAllAsReadButton);
 		waitForElementClickableByElement(notifications_MarkAllAsReadButton);
 		clickAndWait(notifications_MarkAllAsReadButton);
 		if (allWikis) {
 			waitForElementClickableByElement(notifications_MarkAllWikisAsReadButton);
 			clickAndWait(notifications_MarkAllWikisAsReadButton);
 		}
 		else {
 			waitForElementClickableByElement(notifications_MarkOnlyThisWikiAsReadButton);
 			clickAndWait(notifications_MarkOnlyThisWikiAsReadButton);			
 		}
 		PageObjectLogging.log("notifications_clickMarkAllAsRead", (allWikis ? "all wikis" : "only one wiki")+" marked as read", true, driver);				
 	}
 } 
