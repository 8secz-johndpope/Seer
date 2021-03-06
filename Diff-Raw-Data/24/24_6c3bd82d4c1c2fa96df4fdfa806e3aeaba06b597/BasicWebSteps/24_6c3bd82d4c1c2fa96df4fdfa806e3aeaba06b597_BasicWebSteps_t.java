 package org.sukrupa.cucumber.steps;
 
 import cuke4duke.annotation.After;
 import cuke4duke.annotation.I18n.EN.Then;
 import cuke4duke.annotation.I18n.EN.When;
 import net.sf.sahi.client.Browser;
 import org.sukrupa.cucumber.SahiFacade;
 import java.util.Properties;
 
 import static org.hamcrest.Matchers.containsString;
 import static org.hamcrest.Matchers.not;
 import static org.junit.Assert.*;
 import static org.sukrupa.cucumber.SahiFacade.browser;
 
 public class BasicWebSteps {
 
     protected static final String TOP_LEVEL_DIV = "page";
 
     @When("^I enter \"([^\"]*)\" as the \"([^\"]*)\"$")
     public void enterAsThe(String objectInput, String objectID){
         browser().byId(objectID).setValue(objectInput);
     }
 
     @When("^I select \"([^\"]*)\"$")
     public void click(String objectID){
         browser().byId(objectID).click();
     }
 
     @When("^I  \"([^\"]*)\"$")
     public void clearForm(String objectID){
         browser().byId(objectID).click();
     }
 
     @When("^I select \"([^\"]*)\" from \"([^\"]*)\"$")
     public void choseFrom(String choice , String ObjectID){
         browser().byId(ObjectID).choose(choice);
     }
 
     @Then("^\"([^\"]*)\" should contain \"([^\"]*)\"$")
     public void shouldContain(String ObjectID , String objectValueToMatch){
         String objectValue = browser().byId(ObjectID).getValue();
         assertThat(objectValue, containsString(objectValueToMatch));
     }
 
     @Then("^\"([^\"]*)\" should not contain \"([^\"]*)\"$")
     public void shouldNotContain(String ObjectID , String objectValueToMatch){
         String objectValue = browser().byId(ObjectID).getValue();
         assertThat(objectValue, not(containsString(objectValueToMatch)));
     }
 
     @When("^I click \"([^\"]*)\" button$")
     public void clickButton(String buttonText) {
         browser().button(buttonText).click();
     }
 
     @When("^I click \"([^\"]*)\" submit button$")
     public void clickSubmitButton(String buttonText) {
         browser().submit(buttonText).click();
     }
 
     @When("^I submit the \"([^\"]*)\" form$")
     public void submitTheForm(String buttonText) {
         browser().submit(buttonText).click();
     }
 
     @Then("^\"([^\"]*)\" should be displayed$")
     public void shouldBeDisplayed(String text) {
         assertTrue(browser().containsText(browser().div(TOP_LEVEL_DIV), text));
     }
 
     @Then("^\"([^\"]*)\" should not be displayed$")
     public void shouldNotBeDisplayed(String text) {
         assertFalse(browser().containsText(browser().div(TOP_LEVEL_DIV), text));
     }
 
     @When("^I click \"([^\"]*)\" link$")
     public void clickLink(String text) {
         browser().link(text).click();
     }
 
     @When("^I fill in the \"([^\"]*)\" with \"([^\"]*)\"$")
     public void fillInTheTextfieldWith(String field, String fieldContent){
         browser().textbox(field).setValue(fieldContent);
     }
 
     @Then("^student \"([^\"]*)\" is displayed$")
     public void studentIsDisplayed(String text) {
         Browser browser = browser();
         assertTrue(browser.containsText(browser.div(TOP_LEVEL_DIV), text));
     }
 
     @Then("^student \"([^\"]*)\" is not displayed$")
     public void studentIsNotDisplayed(String text) {
         assertFalse(browser().containsText(browser().div("page"), text));
     }
 
     @Then("^\"([^\"]*)\" should be displayed in \"([^\"]*)\"$")
     public void shouldBeDisplayedInField(String text, String field){
         assertTrue(browser().select(field).getText().contains(text));
     }
 
     @Then("^the \"([^\"]*)\" page is displayed")
     public  void  thePageIsDisplayed(String pageName){
         assertTrue(browser().containsText(browser().div(TOP_LEVEL_DIV), pageName));
     }
 
     @When("^I \"([^\"]*)\" in the sidebar")
     public void clickLinkInSidebar(String text){
         browser().link(text).click();
     }
 
     @When("^I enter \"([^\"]*)\" as \"([^\"]*)\"")
     public void enterIntoTheTextBox(String text,String textBoxName){
         browser().textbox(textBoxName).setValue(text);
     }
 
     @When("^I select \"([^\"]*)\" as \"([^\"]*)\"")
     public void selectFromDropDown (String value, String dropDownName){
            browser().select(dropDownName).choose(value);
     }
 
     @Then("^the error message \"([^\"]*)\" is displayed")
     public void displayErrorMessage(String errorMessage){
        assertTrue(browser().containsText(browser().div(TOP_LEVEL_DIV),errorMessage));
     }
     
 
     @When("^I \"([^\"]*)\" the form")
     public  void submitForm(String submitButtonName){
          browser().submit(submitButtonName).click();
     }
 
     @After
     public void closeBrowser() {
         SahiFacade.closeBrowser();
     }
 }
