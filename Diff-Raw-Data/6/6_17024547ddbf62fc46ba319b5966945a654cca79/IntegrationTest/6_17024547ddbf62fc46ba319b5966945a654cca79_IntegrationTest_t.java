 import org.junit.*;
 import org.junit.Test;
 import play.test.TestBrowser;
 import play.libs.F.Callback;
 import static play.test.Helpers.HTMLUNIT;
 import static play.test.Helpers.inMemoryDatabase;
 import static play.test.Helpers.fakeApplication;
 import static play.test.Helpers.testServer;
 import static play.test.Helpers.running;
 import static org.fest.assertions.Assertions.assertThat;
 
 public class IntegrationTest {
 
     /**
      * add your integration test here
      * in this example we just check if the welcome page is being shown
      */
     @Test
     public void test() {
         running(testServer(3333, fakeApplication(inMemoryDatabase())), HTMLUNIT, new Callback<TestBrowser>() {
             public void invoke(TestBrowser browser) {
                 browser.goTo("http://localhost:3333");
                 assertThat(browser.pageSource()).contains("Your new application is ready.");
             }
         });
     }
 
 }
