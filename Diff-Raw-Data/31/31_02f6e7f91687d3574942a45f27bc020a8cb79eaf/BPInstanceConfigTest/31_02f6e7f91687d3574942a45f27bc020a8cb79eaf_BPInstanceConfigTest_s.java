 /*
  * The MIT License
  *
  * Copyright (C) 2010-2011 by Anthony Robinson
  *
  * Permission is hereby granted, free of charge, to any person obtaining a copy
  * of this software and associated documentation files (the "Software"), to deal
  * in the Software without restriction, including without limitation the rights
  * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
  * copies of the Software, and to permit persons to whom the Software is
  * furnished to do so, subject to the following conditions:
  *
  * The above copyright notice and this permission notice shall be included in
  * all copies or substantial portions of the Software.
  *
  * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
  * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
  * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
  * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
  * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
  * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
  * THE SOFTWARE.
  */
 
 package jenkins.plugins.publish_over;
 
 import hudson.model.Result;
 import jenkins.plugins.publish_over.helper.BPBuildInfoFactory;
 import jenkins.plugins.publish_over.helper.BPHostConfigurationFactory;
 import org.easymock.classextension.EasyMock;
 import org.easymock.classextension.IMocksControl;
 import org.junit.AfterClass;
 import org.junit.Before;
 import org.junit.BeforeClass;
 import org.junit.Test;
 import org.mockito.Mockito;
 
 import java.util.LinkedList;
 import java.util.List;
 import java.util.Map;
 import java.util.logging.Level;
 import java.util.logging.Logger;
 
 import static org.junit.Assert.assertEquals;
 import static org.junit.Assert.assertFalse;
 
 public class BPInstanceConfigTest {
 
     private static Level originalLogLevel;
    private static Logger logger;
 
     @BeforeClass public static void before() {
        logger = Logger.getLogger(BPInstanceConfig.class.getCanonicalName());
        originalLogLevel = logger.getLevel();
        logger.setLevel(Level.OFF);
     }
 
     @AfterClass public static void after() {
        logger.setLevel(originalLogLevel);
     }
 
     private BPBuildInfo buildInfo = new BPBuildInfoFactory().createEmpty();
     private IMocksControl mockControl = EasyMock.createStrictControl();
     private BPHostConfigurationAccess mockHostConfigurationAccess = Mockito.mock(BPHostConfigurationAccess.class);
     private BPHostConfiguration hostConfiguration = new BPHostConfigurationFactory().create("TEST-CONFIG");
     private List<BapPublisher> publishers = new LinkedList<BapPublisher>();
 
     @Before public void setUp() throws Exception {
         Mockito.when(mockHostConfigurationAccess.getConfiguration(Mockito.anyString())).thenReturn(hostConfiguration);
         buildInfo.setBaseDirectory(buildInfo.getCurrentBuildEnv().getBaseDirectory());
         buildInfo.setEnvVars(buildInfo.getCurrentBuildEnv().getEnvVars());
         buildInfo.setBuildTime(buildInfo.getCurrentBuildEnv().getBuildTime());
     }
 
     @Test public void testPerformReturnsSuccessIfNoExceptionsThrown() throws Exception {
         BPInstanceConfig instanceConfig = new BPInstanceConfig(publishers, false, false, false, null);
         instanceConfig.setHostConfigurationAccess(mockHostConfigurationAccess);
         BapPublisher mockPublisher = createAndAddMockPublisher(hostConfiguration.getName());
         mockPublisher.perform(hostConfiguration, buildInfo);
 
         assertResult(Result.SUCCESS, instanceConfig);
     }
 
     @Test public void testPerformReturnsUnstableAndDoesNotInvokeOtherPublishers() throws Exception {
         BapPublisher mockPub1 = createAndAddMockPublisher(hostConfiguration.getName());
         mockPub1.perform(hostConfiguration, buildInfo);
         EasyMock.expectLastCall().andThrow(new RuntimeException("Bad stuff here!"));
         createAndAddMockPublisher(null);
 
         BPInstanceConfig instanceConfig = new BPInstanceConfig(publishers, false, false, false, null);
         instanceConfig.setHostConfigurationAccess(mockHostConfigurationAccess);
 
         assertResult(Result.UNSTABLE, instanceConfig);
     }
 
     @Test public void testPerformReturnsUnstableAndInvokesOtherPublishersWhenContinueOnErrorSet() throws Exception {
         BapPublisher mockPub1 = createAndAddMockPublisher(hostConfiguration.getName());
         mockPub1.perform(hostConfiguration, buildInfo);
         EasyMock.expectLastCall().andThrow(new RuntimeException("Bad stuff here!"));
         BapPublisher mockPub2 = createAndAddMockPublisher(hostConfiguration.getName());
         mockPub2.perform(hostConfiguration, buildInfo);
 
         BPInstanceConfig instanceConfig = new BPInstanceConfig(publishers, true, false, false, null);
         instanceConfig.setHostConfigurationAccess(mockHostConfigurationAccess);
 
         assertResult(Result.UNSTABLE, instanceConfig);
     }
 
     @Test public void testPerformReturnsUnstableWhenNoHostConfigFound() throws Exception {
         BapPublisher mockPublisher = createAndAddMockPublisher(null);
         mockPublisher.setEffectiveEnvironmentInBuildInfo((BPBuildInfo) EasyMock.anyObject());
         EasyMock.expect(mockPublisher.getConfigName()).andReturn(hostConfiguration.getName());
 
         Mockito.reset(mockHostConfigurationAccess);
         Mockito.when(mockHostConfigurationAccess.getConfiguration(hostConfiguration.getName())).thenReturn(null);
 
         BPInstanceConfig instanceConfig = new BPInstanceConfig(publishers, false, false, false, null);
         instanceConfig.setHostConfigurationAccess(mockHostConfigurationAccess);
 
         assertResult(Result.UNSTABLE, instanceConfig);
         Mockito.verify(mockHostConfigurationAccess).getConfiguration(hostConfiguration.getName());
     }
 
     @Test public void testPerformReturnsFailureIfFailOnError() throws Exception {
         BapPublisher mockPub1 = createAndAddMockPublisher(hostConfiguration.getName());
         mockPub1.perform(hostConfiguration, buildInfo);
         EasyMock.expectLastCall().andThrow(new RuntimeException("Bad stuff here!"));
         createAndAddMockPublisher(null);
 
         BPInstanceConfig instanceConfig = new BPInstanceConfig(publishers, false, true, false, null);
         instanceConfig.setHostConfigurationAccess(mockHostConfigurationAccess);
 
         assertResult(Result.FAILURE, instanceConfig);
     }
 
     private BapPublisher createAndAddMockPublisher(final String hostConfigurationName) {
         BapPublisher mockPublisher = mockControl.createMock(BapPublisher.class);
         if (hostConfigurationName != null) {
             mockPublisher.setEffectiveEnvironmentInBuildInfo((BPBuildInfo) EasyMock.anyObject());
             EasyMock.expect(mockPublisher.getConfigName()).andReturn(hostConfigurationName);
         }
         publishers.add(mockPublisher);
         return mockPublisher;
     }
 
     private void assertResult(final Result expectedResult, final BPInstanceConfig instanceConfig) {
         mockControl.replay();
         assertEquals(expectedResult, instanceConfig.perform(buildInfo));
         mockControl.verify();
     }
 
     @Test public void testGiveMasterANodeName() throws Exception {
         assertFixNodeName("", "MASTER", "MASTER");
     }
 
     @Test public void testGiveMasterANodeNameWasNull() throws Exception {
         assertFixNodeName(null, "MASTER", "MASTER");
     }
 
     @Test public void testDoNotAffectNodeNameIfMasterNodeNameNotSet() throws Exception {
         assertFixNodeName("", null, "");
     }
 
     @Test public void testDoNotAffectNodeNameIfMasterNodeNameNotSetNull() throws Exception {
         assertFixNodeName(null, "", null);
     }
 
     @Test public void testDoNotAffectNodeNameIfHasValue() throws Exception {
         assertFixNodeName("bob", "master", "bob");
     }
 
     private void assertFixNodeName(final String nodeName, final String masterNodeName, final String expectedNodeName) throws Exception {
         assertFixNodeName(buildInfo.getCurrentBuildEnv().getEnvVars(), nodeName, masterNodeName, expectedNodeName);
     }
 
     @Test public void testGiveMasterANodeNameForPromotion() throws Exception {
         assertFixPromotionNodeName("", "MASTER", "MASTER");
     }
 
     @Test public void testGiveMasterANodeNameWasNullForPromotion() throws Exception {
         assertFixPromotionNodeName(null, "MASTER", "MASTER");
     }
 
     @Test public void testDoNotAffectNodeNameIfMasterNodeNameNotSetForPromotion() throws Exception {
         assertFixPromotionNodeName("", null, "");
     }
 
     @Test public void testDoNotAffectNodeNameIfMasterNodeNameNotSetNullForPromotion() throws Exception {
         assertFixPromotionNodeName(null, "", null);
     }
 
     @Test public void testDoNotAffectNodeNameIfHasValueForPromotion() throws Exception {
         assertFixPromotionNodeName("bob", "master", "bob");
     }
 
     private void assertFixPromotionNodeName(final String nodeName, final String masterNodeName,
                                             final String expectedNodeName) throws Exception {
         BPBuildEnv target = new BPBuildInfoFactory().createEmptyBuildEnv();
         buildInfo.setTargetBuildEnv(target);
         assertFixNodeName(target.getEnvVars(), nodeName, masterNodeName, expectedNodeName);
     }
 
     private void assertFixNodeName(final Map<String, String> envVars, final String nodeName, final String masterNodeName,
                                    final String expectedNodeName) throws Exception {
         envVars.put(BPBuildInfo.ENV_NODE_NAME, nodeName);
         BPInstanceConfig instanceConfig = new BPInstanceConfig(publishers, false, false, false, masterNodeName);
         instanceConfig.setHostConfigurationAccess(mockHostConfigurationAccess);
         BapPublisher mockPublisher = createAndAddMockPublisher(hostConfiguration.getName());
         mockPublisher.perform(hostConfiguration, buildInfo);
         assertResult(Result.SUCCESS, instanceConfig);
 
         assertEquals(expectedNodeName, envVars.get(BPBuildInfo.ENV_NODE_NAME));
     }
 
     @Test public void testDoNotCreateNodeName() throws Exception {
         BPInstanceConfig instanceConfig = new BPInstanceConfig(publishers, false, false, false, "master");
         instanceConfig.setHostConfigurationAccess(mockHostConfigurationAccess);
         BapPublisher mockPublisher = createAndAddMockPublisher(hostConfiguration.getName());
         mockPublisher.perform(hostConfiguration, buildInfo);
         assertResult(Result.SUCCESS, instanceConfig);
 
         assertFalse(buildInfo.getCurrentBuildEnv().getEnvVars().containsKey(BPBuildInfo.ENV_NODE_NAME));
     }
 
 }
