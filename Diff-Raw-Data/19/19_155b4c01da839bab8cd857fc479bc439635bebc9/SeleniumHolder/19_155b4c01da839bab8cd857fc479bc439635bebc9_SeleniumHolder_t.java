 /*
  * This software is licensed under the terms of the GNU GENERAL PUBLIC LICENSE
  * Version 2, which can be found at http://www.gnu.org/copyleft/gpl.html
  */
 package org.cubictest.exporters.selenium.runner.holders;
 
 import org.cubictest.common.settings.CubicTestProjectSettings;
 import org.cubictest.export.exceptions.ExporterException;
 import org.cubictest.export.holders.RunnerResultHolder;
 import org.cubictest.model.UrlStartPoint;
 import org.eclipse.swt.widgets.Display;
 
 import com.thoughtworks.selenium.DefaultSelenium;
 import com.thoughtworks.selenium.Selenium;
 
 /**
  * Holder that has reference to the Selenium test system (for running Selenium commands).
  * Also holds the results of the test and the current contexts (@see ContextHolder).
  * Handles user cancel of the test run.
  *  
  * @author Christian Schwarz
  */
 public class SeleniumHolder extends RunnerResultHolder {
 
 	private Selenium selenium;
 	private boolean seleniumStarted;
 	private UrlStartPoint handledUrlStartPoint;
 	
 	
 	public SeleniumHolder(Selenium selenium, Display display, CubicTestProjectSettings settings) {
 		super(display, settings);
 		//use Selenium from client e.g. the CubicRecorder
 		this.selenium = selenium;
 	}
 	
 	public SeleniumHolder(int port, String browser, String initialUrl, Display display, CubicTestProjectSettings settings) {
 		super(display, settings);
 		if (port < 80) {
 			throw new ExporterException("Invalid port");
 		}
 		selenium = new DefaultSelenium("localhost", port, browser, initialUrl);
 	}
 	
 	public Selenium getSelenium() {
 		return selenium;
 	}
 
 
 	
 	public boolean isSeleniumStarted() {
 		return seleniumStarted;
 	}
 
 	public void setSeleniumStarted(boolean seleniumStarted) {
 		this.seleniumStarted = seleniumStarted;
 	}
 
 
 	public void setHandledUrlStartPoint(UrlStartPoint initialUrlStartPoint) {
 		this.handledUrlStartPoint = initialUrlStartPoint;
 	}
 
 	public UrlStartPoint getHandledUrlStartPoint() {
 		return handledUrlStartPoint;
 	}
 	
 }
