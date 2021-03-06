 /**
  * Copyright (c) 2012, SURFnet BV
  * All rights reserved.
  *
  * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the
  * following conditions are met:
  *
  *   * Redistributions of source code must retain the above copyright notice, this list of conditions and the following
  *     disclaimer.
  *   * Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following
  *     disclaimer in the documentation and/or other materials provided with the distribution.
  *   * Neither the name of the SURFnet BV nor the names of its contributors may be used to endorse or promote products
  *     derived from this software without specific prior written permission.
  *
  * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES,
  * INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
  * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
  * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
  * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
  * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
  * THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
  */
 package nl.surfnet.bod.sabng;
 
 import static org.hamcrest.MatcherAssert.assertThat;
 import static org.hamcrest.Matchers.contains;
 import static org.hamcrest.Matchers.hasSize;
 import static org.mockito.Mockito.verifyZeroInteractions;
 
 import java.util.List;
 
 import javax.annotation.Resource;
 
 import nl.surfnet.bod.AppConfiguration;
 import nl.surfnet.bod.config.IntegrationDbConfiguration;
 import nl.surfnet.bod.util.TestHelper;
 
 import org.apache.http.impl.client.DefaultHttpClient;
 import org.junit.BeforeClass;
 import org.junit.Test;
 import org.junit.runner.RunWith;
 import org.mockito.Mockito;
 import org.springframework.test.annotation.DirtiesContext;
 import org.springframework.test.annotation.DirtiesContext.ClassMode;
 import org.springframework.test.context.ContextConfiguration;
 import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
 import org.springframework.transaction.annotation.Transactional;
 
 @RunWith(SpringJUnit4ClassRunner.class)
 @ContextConfiguration(classes = { AppConfiguration.class, IntegrationDbConfiguration.class })
 @Transactional
 @DirtiesContext(classMode = ClassMode.AFTER_EACH_TEST_METHOD)
 public class SabNgEntitlementsHandlerTestIntegration {
 
   @BeforeClass
   public static void testEnvironment() {
     TestHelper.testProperties();
   }
 
  @Resource
   private SabNgEntitlementsHandler subject;
 
   @Test
   public void shouldRetrieveRoles() {
     String nameId = "urn:collab:person:test.surfguest.nl:prolokees";
     List<String> institutes = subject.checkInstitutes(nameId);
 
     assertThat(institutes, contains("SURFNET"));
   }
 
   @Test
   public void shouldNotPerformCallWhenDisabled() {
 
     DefaultHttpClient httpClient = Mockito.mock(DefaultHttpClient.class);
     subject.setHttpClient(httpClient);
 
     subject.setSabEnabled("false");
     List<String> institutes = subject.checkInstitutes("urn:collab:person:test.surfguest.nl:prolokees");
 
     assertThat(institutes, hasSize(0));
     verifyZeroInteractions(httpClient);
   }
 
   @Test
   public void shouldReturnEmptyListForNoneExistingNameIdInsteadOfThrowingUp() {
     String nameId = "urn:collab:person:test.surfguest.nl:unknown";
     List<String> institutes = subject.checkInstitutes(nameId);
 
     assertThat(institutes, hasSize(0));
   }
 
 }
