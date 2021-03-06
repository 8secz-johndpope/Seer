 package test.test;
 
 
 import java.util.concurrent.Executors;
 import java.util.concurrent.ScheduledExecutorService;
 import java.util.concurrent.TimeUnit;
 
 
 import org.openqa.selenium.By;
 import org.openqa.selenium.WebDriver;
 import org.openqa.selenium.firefox.FirefoxDriver;
 import org.openqa.selenium.support.ui.ExpectedConditions;
 import org.openqa.selenium.support.ui.WebDriverWait;
 import org.testng.Assert;
 import org.testng.annotations.Test;
 
 
 
 
 import PageObject.User;
 
 import com.thoughtworks.selenium.DefaultSelenium;
 import com.thoughtworks.selenium.Selenium;
 
 public class rrrTest {
 
 	@Test
 	public void main4() {
 	    Assert.assertEquals(true, true);
  	}
 	
 	@Test
 	public void main5() {
		Assert.assertEquals(false, true, "This is forced error");
  	}
 	
 	@Test
 	public void main6() {
 		Assert.assertEquals(true, true);
  	}
 	
 	@Test
 	public void main7() {
 		Assert.assertEquals(true, true);
  	}
 	
 	@Test
 	public void main8() {
	Assert.assertEquals(true, false,"This is forced error");
  	}
 	
 	@Test
 	public void main() {
 	    System.out.println("HELLLLLLLLLooooo");
 	    testWebDriver();
 	    System.out.println("==End of test==");
 	}
 	
 	public void testSelenium() {
 		Selenium mSelenium = new DefaultSelenium ("localhost",4444,"*firefox","http://www.nngroup.com/articles/checkboxes-vs-radio-buttons");
 		mSelenium.start();
 		mSelenium.setTimeout("90000");
 		mSelenium.open("http://www.nngroup.com/articles/checkboxes-vs-radio-buttons/");
 		mSelenium.click("permission");
 		System.out.println(mSelenium.isTextPresent("May we send you updates using e-mail?"));
 		mSelenium.stop();
 	}
 	
 	 public void testWebDriver() {
 		 WebDriver driver = new FirefoxDriver();
 		 driver.get("https://geolotto.co.uk/");
 			User user = User.createValidUser();
 			user.setPostCode("73321");
 			PageObject.HomePage homePage = new PageObject.HomePage(driver);
 		    homePage.clickRegister();
 		
 			ScheduledExecutorService worker = Executors.newSingleThreadScheduledExecutor();
 			 (new WebDriverWait(driver, 10))
 			 .until(ExpectedConditions.presenceOfElementLocated(By.xpath("//input[@name='firstNameText']")));
 			PageObject.RegistrationPage registrationPage = new PageObject.RegistrationPage(driver);
 			registrationPage.registerUserError(user);
 		    driver.close();
 		    driver.quit();
     }
 }
