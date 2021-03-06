 /**
  * Candybean is a next generation automation and testing framework suite.
  * It is a collection of components that foster test automation, execution
  * configuration, data abstraction, results illustration, tag-based execution,
  * top-down and bottom-up batches, mobile variants, test translation across
  * languages, plain-language testing, and web service testing.
  * Copyright (C) 2013 SugarCRM, Inc. <candybean@sugarcrm.com>
  *
  * This program is free software: you can redistribute it and/or modify
  * it under the terms of the GNU Affero General Public License as
  * published by the Free Software Foundation, either version 3 of the
  * License, or (at your option) any later version.
  *
  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  * GNU Affero General Public License for more details.
  *
  * You should have received a copy of the GNU Affero General Public License
  * along with this program.  If not, see <http://www.gnu.org/licenses/>.
  */
 package com.sugarcrm.candybean.automation;
 
 import java.awt.AWTException;
 import java.awt.Rectangle;
 import java.awt.Robot;
 import java.awt.Toolkit;
 import java.io.File;
 import java.io.IOException;
 import java.net.MalformedURLException;
 import java.net.URL;
 import java.util.ArrayList;
 import java.util.Iterator;
 import java.util.List;
 import java.util.Set;
 import java.util.Stack;
 import java.util.concurrent.TimeUnit;
 import java.awt.image.BufferedImage;
 
 import javax.imageio.ImageIO;
 import javax.swing.JOptionPane;
 
 import org.openqa.selenium.Alert;
 import org.openqa.selenium.By;
 import org.openqa.selenium.Capabilities;
 import org.openqa.selenium.Dimension;
 import org.openqa.selenium.interactions.HasTouchScreen;
 import org.openqa.selenium.interactions.TouchScreen;
 import org.openqa.selenium.NoAlertPresentException;
 import org.openqa.selenium.UnexpectedAlertBehaviour;
 import org.openqa.selenium.UnhandledAlertException;
 import org.openqa.selenium.WebDriver;
 import org.openqa.selenium.WebElement;
 import org.openqa.selenium.ie.InternetExplorerDriver;
 import org.openqa.selenium.chrome.ChromeDriver;
 import org.openqa.selenium.chrome.ChromeOptions;
 import org.openqa.selenium.firefox.FirefoxBinary;
 import org.openqa.selenium.firefox.FirefoxDriver;
 import org.openqa.selenium.firefox.FirefoxProfile;
 import org.openqa.selenium.firefox.internal.ProfilesIni;
 import org.openqa.selenium.remote.CapabilityType;
 import org.openqa.selenium.remote.DesiredCapabilities;
 import org.openqa.selenium.remote.RemoteTouchScreen;
 import org.openqa.selenium.remote.RemoteWebDriver;
 
 import com.sugarcrm.candybean.automation.control.VControl;
 import com.sugarcrm.candybean.automation.control.VHook;
 import com.sugarcrm.candybean.automation.control.VHook.Strategy;
 import com.sugarcrm.candybean.automation.control.VSelect;
 import com.sugarcrm.candybean.configuration.Configuration;
 import com.sugarcrm.candybean.utilities.Utils.Pair;
 import com.sugarcrm.candybean.utilities.exception.CandybeanException;
 
 /**
  * Drives the creation of multi-platform automation tests by providing a resourceful API
  * containing several helper methods to write automation tests. The {@link Candybean} configuration
  * will build a {@link VInterface} based on the platform specified in the configuration. An appropriate platform-specific
  * driver is instantiated for use to write tests.
  *
  */
 public class VInterface {
 
 	public enum Type { FIREFOX, IE, CHROME, SAFARI, ANDROID, IOS }
 
 	private final Candybean candybean;
 	private final Configuration config;
 
 	private WebDriver wd = null;
 	private Type iType = null;
 //	public final AndroidInterface vac; //vac as in candybean android control
 	private Stack<Pair<Integer, String>> windows = new Stack<Pair<Integer, String>>();
 
 	/**
 	 * Instantiate VInterface; Deprecated in favor of the type-less
 	 * constructor (where type is given during start or from config)
 	 *
 	 * @param candybean  {@link Candybean} object
 	 * @param config   {@link Configuration} for this test run
 	 * @param iType   {@link IInterface.Type} of interface to run
 	 */
 	@Deprecated
 	public VInterface(Candybean candybean, Configuration config, Type iType) {
 		this.candybean = candybean;
 		this.config = config;
 		this.iType = iType;
 	}
 	
 	/**
 	 * Instantiate VInterface
 	 *
 	 * @param candybean  {@link Candybean} object
 	 * @param config   {@link Configuration} for this test run
 	 */
 	public VInterface(Candybean candybean, Configuration config) {
 		this.candybean = candybean;
 		this.config = config;
 	}
 
 	/**
 	 * Pause the test for the specified duration.
 	 *
 	 * @param ms  duration of pause in milliseconds
 	 * @throws InterruptedException if the underlying {@link Thread#sleep} is interrupted
 	 */
 	public void pause(long ms) throws InterruptedException {
 		candybean.log.info("Pausing for " + ms + "ms via thread sleep.");
 		Thread.sleep(ms);
 	}
 
 	/**
 	 * Display a modal dialog box to the test user.
 	 *
 	 * @param message	 	String to display on the dialog box
 	 */
 	public void interact(String message) {
 		candybean.log.info("Interaction via popup dialog with message: " + message);
 		JOptionPane.showInputDialog(message);
 	}
 	
 	/**
 	 * Takes a full screenshot and saves it to the given file.
 	 * 
 	 * @param file			The file to which a screenshot is saved
 	 * @throws IOException 
 	 * @throws AWTException 
 	 */
 	public void screenshot(File file) throws IOException, AWTException {
 		this.candybean.log.info("Taking screenshot; saving to file: " + file.toString());
 		Rectangle screen = new Rectangle(Toolkit.getDefaultToolkit().getScreenSize());
 		BufferedImage screenshot = (new Robot()).createScreenCapture(screen);
 		ImageIO.write(screenshot, "png", file);
 	}
 
 	/**
 	 * Launch and initialize an interface with type defined
 	 * during instantiation.
 	 * 
 	 * @throws Exception		if type is undefined during instantiation
 	 */
 	public void start() throws Exception {
 		this.iType = this.parseInterfaceType(this.config.getValue("automation.interface", "chrome"));
 		this.start(this.iType);
 	}
 
 	/**
 	 * Launch and initialize an interface.
 	 * 
 	 * @param iType			The interface type to start automation 
 	 * @throws Exception
 	 */
 	public void start(Type iType) throws Exception {
 		candybean.log.info("Starting automation interface with type: " + iType);
 //		if (iType == Type.ANDROID) {
 //			this.vac = this.getAndroidControl();
 //			this.wd = null;
 //		}
 //		else {
 //			this.wd = this.getWebDriver(iType);
 //			this.vac = null;
 //			this.start();
 //		}
 		if (this.getWd() != null) {
 			throw new Exception("Automation interface already started with type: " + this.iType);
 		}
 		this.iType = iType;
 		this.setWd(this.getWebDriver(iType));
		this.windows.push(new Pair<Integer, String>(Integer.valueOf(0), this.getWd().getWindowHandle()));
 	}
 
 	/**
 	 * Close the interface and perform final cleanup.
 	 *
 	 */
 	public void stop() {
 		candybean.log.info("Stopping automation interface with type: " + this.iType);
 		this.windows.clear();
 		this.iType = null;
 		if (this.getWd() != null) {
 			this.getWd().quit();
 			this.setWd(null);
 		} else {
 			candybean.log.warning("Automation interface already stopped.");
 		}
 	}
 	
 	/**
 	 * Refreshes the interface.  If refresh is undefined, it does nothing.
 	 * 
 	 */
 	public void refresh() {
 		candybean.log.info("Refreshing the interface.");
 		this.getWd().navigate().refresh();
 	}
 
 	/**
 	 * Restarts the interface with the current interface type
 	 * 
 	 * @throws Exception
 	 */
 	public void restart() throws Exception {
 		candybean.log.info("Restarting automation interface with type: " + this.iType);
 		if (this.getWd() == null) {
 			throw new Exception("Automation interface not yet started; cannot restart.");
 		}
 		Type type = this.iType;
 		this.stop();
 		this.start(type);
 	}
 
 	/**
 	 * Load a URL in the browser window.
 	 *
 	 * @param url	the URL to be loaded by the browser
 	 */
 	public void go(String url) {
 		candybean.log.info("Going to URL and switching to window: " + url);
 		this.getWd().get(url);
 //		this.wd.switchTo().window(this.wd.getWindowHandle());
 	}
 	
 	/**
 	 * Returns the current URL of the current window
 	 * 
 	 * @return		Returns the current window's URL as a String
 	 */
 	public String getURL() {
 		String url = this.getWd().getCurrentUrl();
 		candybean.log.info("Getting URL " + url);
 		return url;
 	}
 
 	/**
 	 * Navigates the interface backward.  If backward is undefined, it does nothing.
 	 * 
 	 */
 	public void backward() {
 		candybean.log.info("Navigating the interface backward.");
 		this.getWd().navigate().back();
 	}
 
 	/**
 	 * Returns true if the interface visibly contains the 
 	 * given string in any non-visible=false element.
 	 * 
 	 * @param s					The target string searched 
 	 * for in the interface		
 	 * @param caseSensitive		Whether or not the search
 	 * is case sensitive		
 	 * @return		Returns true if the interface visibly 
 	 * contains the given string
 	 */
 	public boolean contains(String s, boolean caseSensitive) {
 		candybean.log.info("Searching if the interface contains the following string: " + s + " with case sensitivity: " + caseSensitive);
 		if (!caseSensitive) {
 			s = s.toLowerCase();
 		}
 		List<WebElement> wes = this.getWd().findElements(By.xpath("//*[not(@visible='false')]"));
 		for (WebElement we : wes) {
 			String text = we.getText();
 			if (!caseSensitive) {
 				text = text.toLowerCase();
 			}
 //			System.out.println("text: " + text);
 			if (text.contains(s)) {
 				return true;
 			}
 		}
 		return false;
 	}
 	
 	/**
 	 * Switches focus to default content.
 	 * 
 	 */
 	public void focusDefault() {
 		candybean.log.info("Focusing to default content.");
 		this.getWd().switchTo().defaultContent();
 	}
 	
 	/**
 	 * Switches focus to the IFrame identified by the given zero-based index
 	 * 
 	 * @param index		the serial, zero-based index of the iframe to focus
 	 */
 	public void focusFrame(int index) {
 		candybean.log.info("Focusing to frame by index: " + index);
 		this.getWd().switchTo().frame(index);
 	}
 	
 	/**
 	 * Switches focus to the IFrame identified by the given name or ID string
 	 * 
 	 * @param nameOrId	the name or ID identifying the targeted IFrame
 	 */
 	public void focusFrame(String nameOrId) {
 		candybean.log.info("Focusing to frame by name or ID: " + nameOrId);
 		this.getWd().switchTo().frame(nameOrId);
 	}
 	
 	/**
 	 * Switches focus to the IFrame identified by the given {@link VControl}
 	 * 
 	 * @param control		The VControl representing a focus-targeted IFrame
 	 */
 	public void focusFrame(VControl control) {
 		candybean.log.info("Focusing to frame by control: " + control.toString());
 		this.getWd().switchTo().frame(control.getWe());
 	}
 	
 	/**
 	 * Close the current browser window.
 	 *
 	 */
 	public void closeWindow() {
 		candybean.log.info("Closing window with handle: " + windows.peek());
 		this.getWd().close();
 		this.windows.pop();
 		candybean.log.info("Refocusing to previous window with handle: " + windows.peek());
 		this.getWd().switchTo().window(windows.peek().getY());
 	}
 
 	/**
 	 * Focus a browser window by its index if it is not the current index.
 	 *
 	 * <p>The order of browser windows is somewhat arbitrary and not
 	 * guaranteed, although window creation time ordering seems to be
 	 * the most common.</p>
 	 *
 	 * @param index  		the window index
 	 * @throws Exception	if the specified window index is out of range
 	 */
 	public void focusWindow(int index) throws Exception {
 		if (index == windows.peek().getX().intValue()) {
 			candybean.log.warning("No focus was made because the given index matched the current index: " + index);
 		} else if (index < 0) {
 			throw new Exception("Given focus window index is out of bounds: " + index + "; current size: " + windows.size());
 		} else {
 			Set<String> windowHandlesSet = this.getWd().getWindowHandles();
 			String[] windowHandles = windowHandlesSet.toArray(new String[] {""});
 			if (index >= windowHandles.length) {
 				throw new Exception("Given focus window index is out of bounds: " + index + "; current size: " + windows.size());
 			} else {
 				this.getWd().switchTo().window(windowHandles[index]);
				windows.push(new Pair<Integer, String>(Integer.valueOf(index), this.getWd().getWindowHandle()));
 				candybean.log.info("Focused by index: " + index + " to window: " + windows.peek());
 			}
 		}
 	}
 
 	/**
 	 * Focus a browser window by its window title or URL if it does not
 	 * match the current title or URL.
 	 *
 	 * <p>If more than one window has the same title or URL, the first
 	 * encountered is the one that is focused.</p>
 	 *
 	 * @param titleOrUrl  	the exact window title or URL to be matched
 	 * @throws Exception	if the specified window cannot be found
 	 */
 	public void focusWindow(String titleOrUrl) throws Exception {
 		String curTitle = this.getWd().getTitle();
 		String curUrl = this.getWd().getCurrentUrl();
 		if (titleOrUrl.equals(curTitle) || titleOrUrl.equals(curUrl)) {
 			candybean.log.warning("No focus was made because the given string matched the current title or URL: " + titleOrUrl);
 		} else {
 			Set<String> windowHandlesSet = this.getWd().getWindowHandles();
 			String[] windowHandles = windowHandlesSet.toArray(new String[] {""});
 			int i = 0;
 			boolean windowFound = false;
 			while (i < windowHandles.length && !windowFound) {
 				WebDriver window = this.getWd().switchTo().window(windowHandles[i]);
 				if (window.getTitle().equals(titleOrUrl) || window.getCurrentUrl().equals(titleOrUrl)) {
					windows.push(new Pair<Integer, String>(Integer.valueOf(i), this.getWd().getWindowHandle()));
 					candybean.log.info("Focused by title or URL: " + titleOrUrl + " to window: " + windows.peek());
 					windowFound = true;
 				}
 				i++;
 			}
 			if (!windowFound) {
 				this.getWd().switchTo().window(windows.peek().getY());
 				throw new Exception("The given focus window string matched no title or URL: " + titleOrUrl);
 			}
 		}	
 	}
 	
 	/**
 	 * Navigates the interface forward.  If forward is undefined, it does nothing.
 	 * 
 	 */
 	public void forward() {
 		candybean.log.info("Navigating the interface forward.");
 		this.getWd().navigate().forward();
 	}
 	
 	/**
 	 * Returns a string with the contents of the windows data structure.
 	 * 
 	 * @return	A string representation of all focused windows, with 
 	 * chronological index of focus and handle
 	 */
 	public String getWindowsString() {
 		String s = "Reverse stack:\n";
 		Iterator<Pair<Integer, String>> winIter = windows.iterator();
 		while (winIter.hasNext()) {
 			s += winIter.next() + "\n";
 		}
 		return s;
 	}
 	
 	/**
 	 * Maximize the browser window.
 	 *
 	 * @throws Exception	 <i>not thrown</i>
 	 */
 	public void maximize() {
 		candybean.log.info("Maximizing window");
 //		java.awt.Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
 		this.getWd().manage().window().maximize();//.setSize(new Dimension(screenSize.width, screenSize.height));
 	}
 
 	/**
 	 * Get a control from the current page.
 	 *
 	 * @param hook	 description of how to find the control
 	 * @throws CandybeanException 
 	 */
 	public VControl getControl(VHook hook) throws CandybeanException {
 		return new VControl(this.candybean, this, hook);
 	}
 
 	/**
 	 * Get a control from the current page by index.
 	 *
 	 * @param hook	 description of how to find the control
 	 * @throws Exception	 <i>not thrown</i>
 	 */
 	public VControl getControl(VHook hook, int index) throws CandybeanException {
 		return new VControl(this.candybean, this, hook, index);
 	}
 
 	/**
 	 * Get a control from the current page.
 	 *
 	 * @param strategy  method to use to search for the control
 	 * @param hook		  string to find using the specified strategy
 	 * @throws Exception	 <i>not thrown</i>
 	 */
 	public VControl getControl(Strategy strategy, String hook) throws CandybeanException {
 		return this.getControl(new VHook(strategy, hook));
 	}
 	
 	/**
 	 * Gets a list of controls from the current page
 	 * @param strategy The strategy used to search for the control
 	 * @param hook The associated hook for the strategy
 	 * @return The list of all controls that match the strategy and hook
 	 */
 	public List<VControl> getControls(Strategy strategy, String hook) {
 		return this.getControls(strategy, new VHook(strategy, hook));
 	}
 	
 	/**
 	 * Gets a list of controls from the current page based on a VHook
 	 * @param strategy The strategy used to search for the control
 	 * @param hook The associated hook for the strategy
 	 * @return The list of all controls that match the strategy and hook
 	 */
 	private List<VControl> getControls(Strategy strategy, VHook hook) {
 		List<VControl> controls = new ArrayList<VControl>();
 		List<WebElement> wes = this.getWd().findElements(VControl.makeBy(strategy, hook.getHookString()));
 		for (WebElement we : wes)
 			controls.add(new VControl(this.candybean, this, hook, we));
 		return controls;
 	}
 
 	/**
 	 * Get a control from the current page by index.
 	 *
 	 * @param strategy  method to use to search for the control
 	 * @param hook		  string to find using the specified strategy
 	 * @throws CandybeanException 
 	 */
 	public VControl getControl(Strategy strategy, String hook, int index) throws CandybeanException {
 		return this.getControl(new VHook(strategy, hook), index);
 	}
 
 	/**
 	 * Get a &lt;SELECT&gt; control from the current page.
 	 *
 	 * @param hook	 description of how to find the control
 	 * @throws CandybeanException 
 	 */
 	public VSelect getSelect(VHook hook) throws CandybeanException {
 		return new VSelect(this.candybean, this, hook);
 	}
 
 	/**
 	 * Get a &lt;SELECT&gt; control from the current page.
 	 *
 	 * @param strategy  method to use to search for the control
 	 * @param hook		  string to find using the specified strategy
 	 * @throws CandybeanException 
 	 */
 	public VSelect getSelect(Strategy strategy, String hook) throws CandybeanException {
 		return this.getSelect(new VHook(strategy, hook));
 	}
 	
 	
 	/**
 	 * Returns the predefined type of interface instantiated.
 	 * 
 	 * @return	The type of interface
 	 */
 	public Type getType() {
 		return this.iType;
 	}
 	
 	/**
 	 * Click &quot;OK&quot; on a modal dialog box (usually referred to
 	 * as a &quot;javascript dialog&quot;).
 	 *
 	 */
 	public void acceptDialog() {
 		try {
 			candybean.log.info("Accepting dialog.");
 			this.getWd().switchTo().alert().accept();
 //			this.wd.switchTo().defaultContent();
 		} catch(UnhandledAlertException uae) {
 			candybean.log.warning("Unhandled alert exception");
 		}
 	}
 	
 	/**
 	 * Dismisses a modal dialog box (usually referred to
 	 * as a &quot;javascript dialog&quot;).
 	 *
 	 */
 	public void dismissDialog() {
 		try {
 			candybean.log.info("Dismissing dialog.");
 			this.getWd().switchTo().alert().dismiss();
 //			this.wd.switchTo().defaultContent();
 		} catch(UnhandledAlertException uae) {
 			candybean.log.warning("Unhandled alert exception");
 		}
 	}
 
 	/**
 	 * Returns true if a modal dialog can be switched to 
 	 * and switched back from; otherwise, returns false.
 	 * 
 	 * @return 	Boolean true only if a modal dialog can 
 	 * be switched to, then switched back from.
 	 */
 	public boolean isDialogVisible() {
 		try { 
 			this.getWd().switchTo().alert(); 
 //			this.wd.switchTo().defaultContent();
 			candybean.log.info("Dialog present?: true.");
 			return true;
 		} catch(UnhandledAlertException uae) {
 			candybean.log.info("(Unhandled alert in FF?) Dialog present?: true.  May have ignored dialog...");
 			return true;
 		} catch(NoAlertPresentException nape) {
 			candybean.log.info("Dialog present?: false.");
 			return false;
 		}
 	}
 	
     /**
 	 * Encompasses a widget from the current page in order to perform
 	 * an action on it.  Providing this as a more aptly named alternative
 	 * to getControl as the 'thing' encompassed by this is not necessarily
 	 * a 'control' and merely referencing it does nothing; an action must be
 	 * performed off of it, hence not named 'getWidget'.
 	 *
 	 * @param hookStrategy	method to use to search for the widget
 	 * @param hookString	string to find using the specified strategy
      * @throws CandybeanException 
 	 */
 	public VControl widget(Strategy hookStrategy, String hookString) throws CandybeanException {
 		return this.getControl(new VHook(hookStrategy, hookString));
 	}
 	
 	/**
 	 * Encompasses a widget from the current page in order to perform
 	 * an action on it.  Providing this as a more aptly named alternative
 	 * to getControl as the 'thing' encompassed by this is not necessarily
 	 * a 'control' and merely referencing it does nothing; an action must be
 	 * performed off of it, hence not named 'getWidget'.
 	 *
 	 * @param hook			VHook method to use to search for the widget
 	 * @throws CandybeanException 
 	 */
 	public VControl widget(VHook hook) throws CandybeanException {
 		return this.getControl(hook);
 	}
 	
 	private VInterface.Type parseInterfaceType(String iTypeString) throws Exception {
 		VInterface.Type iType = null;
 		for (VInterface.Type iTypeIter : VInterface.Type.values()) {
 			if (iTypeIter.name().equalsIgnoreCase(iTypeString)) {
 				iType = iTypeIter;
 				break;
 			}
 		}
 		if (iType == Type.ANDROID) {
 			throw new Exception("Android interface type not yet implemented.");
 		}else if (iType == Type.IOS) {
 			throw new Exception("iOS interface type not yet implemented.");
 		}
 		return iType;
 	}
 
 	private WebDriver getWebDriver(Type iType) throws Exception {
         DesiredCapabilities capabilities = new DesiredCapabilities();
 //        capabilities.setCapability(CapabilityType.UNEXPECTED_ALERT_BEHAVIOUR, UnexpectedAlertBehaviour.ACCEPT);
         WebDriver wd = null;
         switch (iType) {
 		case FIREFOX:
 			String profileName = this.config.getValue("browser.firefox_profile", "default");
 			File ffBinaryPath = new File(this.config.getPathValue("browser.firefox_binary"));
 			FirefoxProfile ffProfile = (new ProfilesIni()).getProfile(profileName);
 //			ffProfile.setEnableNativeEvents(false);
 			FirefoxBinary ffBinary = new FirefoxBinary(ffBinaryPath);
 			// if (System.getProperty("headless") != null) {
 			// FirefoxBinary ffBinary = new FirefoxBinary();//new
 			// File("//home//conrad//Applications//firefox-10//firefox"));
 			// ffBinary.setEnvironmentProperty("DISPLAY", ":1");
 			// webDriver = new FirefoxDriver(ffBinary, ffProfile);
 			// }
 			candybean.log.info("Instantiating Firefox with profile name: "
 					+ profileName + " and binary path: " + ffBinaryPath);
 			wd = new FirefoxDriver(ffBinary, ffProfile);
 			break;
 		case CHROME:
 			ChromeOptions chromeOptions = new ChromeOptions();
 			String chromeDriverLogPath = this.config.getPathValue("browser.chrome_driver_log_path");
 			candybean.log.info("chromeDriverLogPath: " + chromeDriverLogPath);
 			chromeOptions.addArguments("--log-path=" + chromeDriverLogPath);
 			String chromeDriverPath = this.config.getPathValue("browser.chrome_driver_path");
 			candybean.log.info("chromeDriverPath: " + chromeDriverPath);
 			// chromeOptions.setBinary(new File(chromeDriverPath));
 			System.setProperty("webdriver.chrome.driver", chromeDriverPath);
 			candybean.log.info("Instantiating Chrome with:\n    log path:"
 					+ chromeDriverLogPath + "\n    driver path: "
 					+ chromeDriverPath);
 			wd = new ChromeDriver(chromeOptions);
 			break;
 		case IE:
 			String ieDriverPath = this.config.getPathValue("browser.ie_driver_path");
 			candybean.log.info("ieDriverPath: " + ieDriverPath);
 			System.setProperty("webdriver.ie.driver", ieDriverPath);
 			capabilities = DesiredCapabilities.internetExplorer();
 			wd = new InternetExplorerDriver(capabilities);
 			break;
 		case SAFARI:
 			throw new Exception("Selenium: safari browser not yet supported.");
         case ANDROID:
             capabilities = new DesiredCapabilities();
             capabilities.setCapability(CapabilityType.BROWSER_NAME, "Android");
             capabilities.setCapability(CapabilityType.PLATFORM, "Mac");
             capabilities.setCapability("app", "https://s3.amazonaws.com/voodoo2/ApiDemos-debug.apk");
             wd = new SwipeableWebDriver(new URL("http://127.0.0.1:4723/wd/hub"), capabilities);
             break;
         case IOS:
             capabilities = new DesiredCapabilities();
             capabilities.setCapability(CapabilityType.BROWSER_NAME, "iOS");
             capabilities.setCapability(CapabilityType.VERSION, "6.0");
             capabilities.setCapability(CapabilityType.PLATFORM, "Mac");
             capabilities.setCapability("app", "https://s3.amazonaws.com/voodoo2/TestApp.zip");
             wd = new SwipeableWebDriver(new URL("http://127.0.0.1:4723/wd/hub"), capabilities);
             break;
         default:
 			throw new Exception("Selenium: browser type not recognized.");
 		}
 		long implicitWait = Long.parseLong(config.getValue("perf.implicit_wait_seconds"));
 		if (System.getProperty("headless") == null) {
 			java.awt.Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
 			wd.manage().window().setSize(new Dimension(screenSize.width, screenSize.height));
 		}
 		wd.manage().timeouts().implicitlyWait(implicitWait, TimeUnit.SECONDS);
 		return wd;
 	}
 	
 	public WebDriver getWd() {
 		return wd;
 	}
 
 	public void setWd(WebDriver wd) {
 		this.wd = wd;
 	}
 
 	public class SwipeableWebDriver extends RemoteWebDriver implements HasTouchScreen {
         private RemoteTouchScreen touch;
 
         public SwipeableWebDriver(URL remoteAddress, Capabilities desiredCapabilities) {
             super(remoteAddress, desiredCapabilities);
             touch = new RemoteTouchScreen(getExecuteMethod());
         }
 
         public TouchScreen getTouch() {
             return touch;
         }
     }
 
 	// ANDROID ROBOTIUM FUNCTIONALITY
 	//	private AndroidInterface getAndroidControl() throws Exception {
 	//		AndroidInterface vac = new AndroidInterface(this.props);
 	//		return vac;
 	//	}
 	//	
 	//	public void startApp() throws Exception {
 	//		this.vac.startApp();
 	//	}
 	//	
 	//	public void finishApp() throws Exception {
 	//		this.vac.finishApp();
 	//	}
 	//	
 	//	public void setApkPath(String aut, String messenger, String testrunner) {
 	//		this.vac.setApkPath(aut, messenger, testrunner);
 	//	}
 	//	
 	//	public void ignoreInstallAUT() throws Exception {
 	//		this.vac.ignoreInstallAUT();
 	//	}
 	//	
 	//	public void ignoreInstallMessenger() throws Exception {
 	//		this.vac.ignoreInstallMessenger();
 	//	}
 	//	
 	//	public void ignoreInstallRunner() throws Exception {
 	//		this.vac.ignoreInstallRunner();
 	//	}
 	//	
 	//	public VAControl getAControl() throws Exception{
 	//		return new VAControl(this.candybean, this);
 	//	}
 
 
 	//	/**
 	//	 * @param selectElement
 	//	 * @param actionElement
 	//	 */
 	//	public static void allOptionsAction(Select selectElement, WebElement actionElement) {
 	//		List<WebElement> options = selectElement.getOptions();
 	//		for (WebElement option : options) {
 	//			selectElement.selectByVisibleText(option.getText());
 	//			actionElement.click();
 	//		}
 	//	}
 	//	
 	//	
 	//	/**
 	//	 * @param selectElement
 	//	 * @param actionOptionValues
 	//	 * @param actionElement
 	//	 * @throws Exception
 	//	 */
 	//	public static void optionAction(Select selectElement, Set<String> actionOptionValues, WebElement actionElement) throws Exception {
 	//		List<WebElement> allOptions = selectElement.getOptions();
 	//		HashSet<String> optionValues = new HashSet<String>();
 	//		for(WebElement option : allOptions) {
 	//			optionValues.add(option.getText());
 	////			System.out.println("Adding to options set:" + option.getText());
 	//		}
 	//		if(optionValues.containsAll(actionOptionValues)) {
 	//			for(String option : actionOptionValues) {
 	//				selectElement.selectByVisibleText(option);
 	//				actionElement.click();
 	//			}
 	//		} else throw new Exception("Specified select option unavailable...");
 	//	}
 	//	
 	//	
 	//
 	//	/**
 	//	 * @param element
 	//	 * @return
 	//	 */
 	//	public static String webElementToString(WebElement element) {
 	//		List<WebElement> childElements = element.findElements(By.xpath("*"));
 	//		String s = element.getTagName() + ":" + element.getText() + " ";
 	//		for(WebElement we : childElements) {
 	//			s += we.getTagName() + ":" + we.getText() + " ";
 	//		}
 	//		return s;
 	//	}
 	//	
 	//	
 	//	/**
 	//	 * @param nativeOptions
 	//	 * @param queryOptionNames
 	//	 * @return
 	//	 */
 	//	public static boolean optionValuesEqual(List<WebElement> nativeOptions, Set<String> queryOptionNames) {
 	//		Set<String> nativeOptionNames = new HashSet<String>();
 	//		for (WebElement option : nativeOptions) {
 	//			nativeOptionNames.add(option.getText());
 	//		}
 	//		if (nativeOptionNames.containsAll(queryOptionNames) && queryOptionNames.containsAll(nativeOptionNames)) return true;
 	//		else return false;
 	//	}
 }
