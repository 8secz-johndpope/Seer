 package net.thucydides.core.steps;
 
 import com.google.common.base.Preconditions;
 import com.google.common.collect.ImmutableList;
 import net.thucydides.core.IgnoredStepException;
 import net.thucydides.core.PendingStepException;
 import net.thucydides.core.annotations.TestAnnotations;
 import net.thucydides.core.guice.Injectors;
 import net.thucydides.core.model.Story;
 import net.thucydides.core.model.TestOutcome;
 import net.thucydides.core.model.TestResult;
 import net.thucydides.core.model.TestStep;
 import net.thucydides.core.pages.Pages;
 import net.thucydides.core.pages.SystemClock;
 import net.thucydides.core.screenshots.Photographer;
 import net.thucydides.core.screenshots.ScreenshotAndHtmlSource;
 import net.thucydides.core.screenshots.ScreenshotException;
 import net.thucydides.core.webdriver.Configuration;
 import net.thucydides.core.webdriver.WebdriverProxyFactory;
 import org.apache.commons.lang3.StringUtils;
 import org.openqa.selenium.WebDriver;
 import org.slf4j.Logger;
 import org.slf4j.LoggerFactory;
 
 import java.io.File;
 import java.util.ArrayList;
 import java.util.List;
 import java.util.Stack;
 
 import static net.thucydides.core.model.Stories.findStoryFrom;
 import static net.thucydides.core.model.TestResult.FAILURE;
 import static net.thucydides.core.model.TestResult.IGNORED;
 import static net.thucydides.core.model.TestResult.PENDING;
 import static net.thucydides.core.model.TestResult.SKIPPED;
 import static net.thucydides.core.model.TestResult.SUCCESS;
 import static net.thucydides.core.util.NameConverter.underscore;
 
 /**
  * Observes the test run and stores test run details for later reporting.
  * Observations are recorded in an TestOutcome object. This includes
  * recording the names and results of each test, and taking and storing
  * screenshots at strategic points during the tests.
  */
 public class BaseStepListener implements StepListener, StepPublisher {
 
     /**
      * Used to build the test outcome structure as the test step results come in.
      */
     private final List<TestOutcome> testOutcomes;
 
     /**
      * Keeps track of what steps have been started but not finished, in order to structure nested steps.
      */
     private final Stack<TestStep> currentStepStack;
 
     /**
      * Keeps track of the current step group, if any.
      */
     private final Stack<TestStep> currentGroupStack;
 
     /**
      * Clock used to pause test execution.
      */
     private final SystemClock clock;
 
     /**
      * The Java class (if any) containing the tests.
      */
     private Class<?> testSuite;
 
     private static final Logger LOGGER = LoggerFactory.getLogger(BaseStepListener.class);
 
     private WebDriver driver;
 
     private File outputDirectory;
 
     private WebdriverProxyFactory proxyFactory;
 
     private Story testedStory;
 
     private Configuration configuration;
     
     private boolean inFluentStepSequence;
     
     private String fluentStepComposedName;
 
     public BaseStepListener(final File outputDirectory) {
         this.proxyFactory = WebdriverProxyFactory.getFactory();
         this.testOutcomes = new ArrayList<TestOutcome>();
         this.currentStepStack = new Stack<TestStep>();
         this.currentGroupStack = new Stack<TestStep>();
         this.outputDirectory = outputDirectory;
         this.clock = Injectors.getInjector().getInstance(SystemClock.class);
         this.configuration = Injectors.getInjector().getInstance(Configuration.class);
         this.inFluentStepSequence = false;
     }
 
     /**
      * Create a step listener with a given web driver type.
      * @param driverClass a driver of this type will be used
      * @param outputDirectory reports and screenshots are generated here
      */
     public BaseStepListener(final Class<? extends WebDriver> driverClass, final File outputDirectory) {
         this(outputDirectory);
         this.driver = getProxyFactory().proxyFor(driverClass);
     }
 
     public BaseStepListener(final Class<? extends WebDriver> driverClass,
                             final File outputDirectory,
                             final Configuration configuration) {
         this(outputDirectory);
         this.driver = getProxyFactory().proxyFor(driverClass);
         this.configuration = configuration;
     }
 
     public BaseStepListener(final File outputDirectory,
                             final Configuration configuration) {
         this(outputDirectory);
         this.driver = getProxyFactory().proxyFor(null);
         this.configuration = configuration;
     }
 
     /**
      * Create a step listener using the driver from a given page factory.
      * If the pages factory is null, a new driver will be created based on the default system values.
      * @param outputDirectory reports and screenshots are generated here
      * @param pages a pages factory.
      */
     public BaseStepListener(final File outputDirectory, final Pages pages) {
          this(outputDirectory);
          if (pages != null) {
              setDriverUsingPagesDriverIfDefined(pages);
          } else {
              createNewDriver();
          }
     }
 
     private void createNewDriver() {
         setDriver(getProxyFactory().proxyDriver());
     }
 
     private void setDriverUsingPagesDriverIfDefined(final Pages pages) {
         if (pages.getDriver() != null) {
             setDriver(pages.getDriver());
         } else {
             createNewDriver();
             pages.setDriver(getDriver());
             pages.notifyWhenDriverOpens();
         }
     }
 
     protected WebdriverProxyFactory getProxyFactory() {
         return proxyFactory;
     }
 
     protected TestOutcome getCurrentTestOutcome() {
         Preconditions.checkState(!testOutcomes.isEmpty());
         return testOutcomes.get(testOutcomes.size() - 1);
     }
 
     protected SystemClock getClock() {
         return clock;
     }
 
     /**
      * A test suite (containing a series of tests) starts.
      * @param startedTestSuite the class implementing the test suite (e.g. a JUnit test case)
      */
     public void testSuiteStarted(final Class<?> startedTestSuite) {
         testSuite = startedTestSuite;
         testedStory = findStoryFrom(startedTestSuite);
     }
 
     public void testSuiteStarted(final Story story) {
         testSuite = null;
         testedStory = story;
     }
 
     @Override
     public void testSuiteFinished() {
     }
 
 
     /**
      * An individual test starts.
      * @param testMethod the name of the test method in the test suite class.
      */
     public void testStarted(final String testMethod) {
         LOGGER.info("test started: {}", testMethod);
         testOutcomes.add(TestOutcome.forTestInStory(testMethod, testSuite, testedStory));
         setAnnotatedResult(testMethod);
     }
 
     private void setAnnotatedResult(String testMethod) {
         if (TestAnnotations.forClass(testSuite).isIgnored(testMethod)) {
             getCurrentTestOutcome().setAnnotatedResult(IGNORED);
         }
         if (TestAnnotations.forClass(testSuite).isPending(testMethod)) {
             getCurrentTestOutcome().setAnnotatedResult(PENDING);
         }
     }
 
     /**
      * A test has finished.
      * @param result the result of the test that just finished.
      */
     public void testFinished(final TestOutcome result) {
         recordTestDuration();
         currentStepStack.clear();
     }
 
     private void recordTestDuration() {
         if (!testOutcomes.isEmpty()) {
             getCurrentTestOutcome().recordDuration();
         }
     }
 
     /**
      * A step within a test is called.
      * This step might be nested in another step, in which case the original step becomes a group of steps.
      * @param description the description of the test that is about to be run
      */
     public void stepStarted(final ExecutedStepDescription description) {
         LOGGER.debug("step started: " + description);
         recordStep(description);
         takeInitialScreenshot();
     }
 
     public void skippedStepStarted(final ExecutedStepDescription description) {
         LOGGER.debug("skipped step started: " + description);
         recordStep(description);
     }
 
     private void recordStep(ExecutedStepDescription description) {
         String stepName = AnnotatedStepDescription.from(description).getName();
 
         updateFluentStepStatus(description, stepName);
 
         if (justStartedAFluentSequenceFor(description) || notInAFluentSequence()) {
 
             TestStep step = new TestStep(stepName);
 
             startNewGroupIfNested();
             setDefaultResultFromAnnotations(step, description);
     
             currentStepStack.push(step);
             getCurrentTestOutcome().recordStep(step);
         }
         inFluentStepSequence = AnnotatedStepDescription.from(description).isFluent();
     }
 
     private void updateFluentStepStatus(ExecutedStepDescription description, String stepName) {
         if (currentlyInAFluentSequenceFor(description) || justFinishedAFluentSequenceFor(description)) {
             addToFluentStepName(stepName);
         }
     }
 
     private void addToFluentStepName(String stepName) {
         String updatedStepName = getCurrentStep().getDescription() + " " + StringUtils.uncapitalize(stepName);
         getCurrentStep().setDescription(updatedStepName);
     }
 
     private boolean notInAFluentSequence() {
         return !inFluentStepSequence;
     }
 
     private boolean justFinishedAFluentSequenceFor(ExecutedStepDescription description) {
         boolean thisStepIsFluent = AnnotatedStepDescription.from(description).isFluent();
         return (inFluentStepSequence && !thisStepIsFluent);
     }
 
     private boolean justStartedAFluentSequenceFor(ExecutedStepDescription description) {
         boolean thisStepIsFluent = AnnotatedStepDescription.from(description).isFluent();
         return (!inFluentStepSequence && thisStepIsFluent);
     }
 
     private boolean currentlyInAFluentSequenceFor(ExecutedStepDescription description) {
         boolean thisStepIsFluent = AnnotatedStepDescription.from(description).isFluent();
         return (inFluentStepSequence && thisStepIsFluent);
     }
 
     private void setDefaultResultFromAnnotations(final TestStep step, final ExecutedStepDescription description) {
         if (TestAnnotations.isPending(description.getTestMethod())) {
             step.setResult(TestResult.PENDING);
         }
         if (TestAnnotations.isIgnored(description.getTestMethod())) {
             step.setResult(TestResult.SKIPPED);
         }
     }
 
     private void startNewGroupIfNested() {
         if (thereAreUnfinishedSteps()) {
             if (getCurrentStep() != getCurrentGroup()) {
                 startNewGroup();
             }
         }
     }
 
     private void startNewGroup() {
         getCurrentTestOutcome().startGroup();
         currentGroupStack.push(getCurrentStep());
     }
 
     private TestStep getCurrentStep() {
         return currentStepStack.peek();
     }
 
     private TestStep getCurrentGroup() {
         if (currentGroupStack.isEmpty()) {
             return null;
         } else {
             return currentGroupStack.peek();
         }
     }
 
     private boolean thereAreUnfinishedSteps() {
         return !currentStepStack.isEmpty();
     }
 
     public void stepFinished() {
         LOGGER.debug("step finished");
 
         takeScreenshotFor(SUCCESS);
         currentStepDone();
         markCurrentStepAs(SUCCESS);
         pauseIfRequired();
     }
 
     private void finishGroup() {
         currentGroupStack.pop();
         getCurrentTestOutcome().endGroup();
     }
 
     private void pauseIfRequired() {
         int delay = configuration.getStepDelay();
         if (delay > 0) {
             getClock().pauseFor(delay);
         }
     }
 
     private void markCurrentStepAs(final TestResult result) {
         getCurrentTestOutcome().getCurrentStep().setResult(result);
     }
 
     public void stepFailed(StepFailure failure) {
         LOGGER.debug("step failed: " + failure);
         takeScreenshotFor(FAILURE);
         getCurrentTestOutcome().setTestFailureCause(failure.getException());
         markCurrentStepAs(FAILURE);
         recordFailureDetailsInFailingTestStep(failure);
         currentStepDone();
     }
 
     public void lastStepFailed(StepFailure failure) {
         LOGGER.debug("last step failed: " + failure);
         takeScreenshotFor(FAILURE);
         getCurrentTestOutcome().lastStepFailedWith(failure);
     }
 
 
     private void recordFailureDetailsInFailingTestStep(final StepFailure failure) {
         if (currentStepExists()) {
             getCurrentStep().failedWith(new StepFailureException(failure.getMessage(), failure.getException()));
         }
     }
 
     public void stepIgnored() {
         LOGGER.debug("step ignored");
         if (aStepHasFailed()) {
             markCurrentStepAs(SKIPPED);
             currentStepDone();
         } else {
             markCurrentStepAs(IGNORED);
             currentStepDone();
         }
     }
 
     @Override
     public void stepIgnored(String message) {
         getCurrentStep().testAborted(new IgnoredStepException(message));
         stepIgnored();
     }
 
     public void stepPending() {
         LOGGER.debug("step pending");
         markCurrentStepAs(PENDING);
         currentStepDone();
     }
 
     @Override
     public void stepPending(String message) {
         getCurrentStep().testAborted(new PendingStepException(message));
         stepPending();
     }
 
     private void currentStepDone() {
        if ((!inFluentStepSequence) && currentStepExists()) {
             TestStep finishedStep =  currentStepStack.pop();
             finishedStep.recordDuration();
 
             if (finishedStep == getCurrentGroup()) {
                 finishGroup();
             }
         }
     }
 
     private boolean currentStepExists() {
         return !currentStepStack.isEmpty();
     }
 
     private void takeScreenshotFor(TestResult result) {
         if (shouldTakeScreenshotFor(result)) {
             takeScreenshot();
         }
     }
 
     private void takeScreenshot() {
         if (currentStepExists()) {
             try {
                 String stepDescription = getCurrentTestOutcome().getCurrentStep().getDescription();
                 ScreenshotAndHtmlSource screenshotAndHtmlSource = grabScreenshotFor(stepDescription);
                 if (screenshotAndHtmlSource.wasTaken()) {
                     getCurrentStep().addScreenshot(screenshotAndHtmlSource);
                 }
             } catch (ScreenshotException e) {
                 LOGGER.warn("Failed to take screenshot", e);
             }
         }
     }
 
     private void takeInitialScreenshot() {
         if ((currentStepExists()) && !configuration.onlySaveFailingScreenshots()) {
             takeScreenshot();
         }
     }
 
     private ScreenshotAndHtmlSource grabScreenshotFor(final String testName) {
         String snapshotName = underscore(testName);
         File screenshot = getPhotographer().takeScreenshot(snapshotName);
         File sourcecode = getPhotographer().getMatchingSourceCodeFor(screenshot);
         return new ScreenshotAndHtmlSource(screenshot, sourcecode);
     }
 
     public Photographer getPhotographer() {
         return new Photographer(driver, outputDirectory);
 
     }
 
     private boolean shouldTakeScreenshotFor(final TestResult result) {
         if (configuration.onlySaveFailingScreenshots()) {
             return (result == FAILURE);
         } else {
             return true;
         }
     }
 
     public List<TestOutcome> getTestOutcomes() {
         return ImmutableList.copyOf(testOutcomes);
     }
 
 
     public void setDriver(final WebDriver driver) {
         this.driver = driver;
     }
 
     public WebDriver getDriver() {
         return driver;
     }
 
     public boolean aStepHasFailed() {
         return (!getTestOutcomes().isEmpty()) && (getCurrentTestOutcome().getTestFailureCause() != null);
     }
 
     public Throwable getTestFailureCause() {
         return getCurrentTestOutcome().getTestFailureCause();
     }
 
     public void testFailed(final Throwable cause) {
         getCurrentTestOutcome().setTestFailureCause(cause);
     }
 
     public void testIgnored() {
         getCurrentTestOutcome().setAnnotatedResult(IGNORED);
     }
 
     @Override
     public void notifyScreenChange() {
         takeScreenshot();
     }
 }
