 package br.com.caelum.seleniumdsl;
 
 import br.com.caelum.seleniumdsl.js.Array;
 import br.com.caelum.seleniumdsl.js.DefaultArray;
 import br.com.caelum.seleniumdsl.table.DefaultTable;
 import br.com.caelum.seleniumdsl.table.Table;
 
 import com.thoughtworks.selenium.Selenium;
 
 class DefaultPage implements Page {
 
 	final Selenium selenium;
 
 	private final int timeout;
 
 	public DefaultPage(Selenium selenium, int timeout) {
 		this.selenium = selenium;
 		this.timeout = timeout;
 	}
 
 	public String title() {
 		return selenium.getTitle();
 	}
 
 	public Form form(String id) {
 		return new DefaultForm(selenium, timeout, id.equals("") ? "" : id + ".");
 	}
 
 	public ContentTag div(String id) {
 		return new DefaultContentTag(selenium, id);
 	}
 
 	public ContentTag span(String id) {
 		return new DefaultContentTag(selenium, id);
 	}
 
 	public Table table(String id) {
 		return new DefaultTable(selenium, id);
 	}
 
 	public Page navigate(String link) {
 		selenium.click(link);
 		selenium.waitForPageToLoad(Integer.toString(timeout));
 		return this;
 	}
 
 	public Page click(String element) {
 		selenium.click(element);
 		return this;
 	}
 
 	public boolean hasLink(String link) {
 		return selenium.isTextPresent(link);
 	}
 
 	public Array array(String name) {
 		return new DefaultArray(selenium, name);
 	}
 
 	public String invoke(String cmd) {
 		return selenium.getEval("this.browserbot.getCurrentWindow()." + cmd);
 	}
 
 	public void screenshot(String filename) {
 		selenium.captureScreenshot(filename);
 	}
 
 	public Page waitUntil(String condition, long timeout) {
 		selenium.waitForCondition("selenium.browserbot.getCurrentWindow()." + condition, "" + timeout);
 		return this;
 	}
 
 	public Page refresh() {
 		selenium.refresh();
		selenium.waitForPageToLoad(title());
 		return this;
 	}
 
 }
