 /*
  * Copyright (c) 2005-2010 Grameen Foundation USA
  *  All rights reserved.
  *
  *  Licensed under the Apache License, Version 2.0 (the "License");
  *  you may not use this file except in compliance with the License.
  *  You may obtain a copy of the License at
  *
  *      http://www.apache.org/licenses/LICENSE-2.0
  *
  *  Unless required by applicable law or agreed to in writing, software
  *  distributed under the License is distributed on an "AS IS" BASIS,
  *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
  *  implied. See the License for the specific language governing
  *  permissions and limitations under the License.
  *
  *  See also http://www.apache.org/licenses/LICENSE-2.0.html for an
  *  explanation of the license and how it is applied.
  */
 
 package org.mifos.platform.questionnaire.persistence;
 
 import org.apache.commons.lang.StringUtils;
 import org.hamcrest.Description;
 import org.hamcrest.TypeSafeMatcher;
 import org.junit.Assert;
 import org.junit.Test;
 import org.junit.runner.RunWith;
 import org.mifos.customers.surveys.business.Question;
 import org.mifos.customers.surveys.helpers.AnswerType;
 import org.mifos.framework.exceptions.ApplicationException;
 import org.mifos.platform.questionnaire.QuestionnaireConstants;
 import org.mifos.platform.questionnaire.contract.*;
 import org.mifos.platform.questionnaire.domain.EventSourceEntity;
 import org.mifos.platform.questionnaire.domain.QuestionGroup;
 import org.mifos.platform.questionnaire.domain.QuestionGroupState;
 import org.mifos.platform.questionnaire.domain.Section;
 import org.mifos.test.matchers.EventSourceMatcher;
 import org.springframework.beans.factory.annotation.Autowired;
 import org.springframework.dao.DataAccessException;
 import org.springframework.test.context.ContextConfiguration;
 import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
 import org.springframework.test.context.transaction.TransactionConfiguration;
 import org.springframework.transaction.annotation.Transactional;
 
 import java.util.Calendar;
 import java.util.List;
 import java.util.Set;
 
 import static java.util.Arrays.asList;
 import static org.hamcrest.CoreMatchers.is;
 import static org.hamcrest.CoreMatchers.not;
 import static org.hamcrest.CoreMatchers.nullValue;
 import static org.hamcrest.Matchers.hasItem;
 import static org.hamcrest.Matchers.hasItems;
 import static org.junit.Assert.*;
 import static org.mifos.platform.questionnaire.contract.QuestionType.*;
 
 @RunWith(SpringJUnit4ClassRunner.class)
 @ContextConfiguration(locations = {"/org/mifos/config/resources/QuestionnaireContext.xml", "/org/mifos/config/resources/persistenceContext.xml", "/test-dataSourceContext.xml"})
 @TransactionConfiguration(transactionManager = "platformTransactionManager", defaultRollback = true)
 public class QuestionnaireServiceIntegrationTest {
 
     @Autowired
     private QuestionnaireService questionnaireService;
 
     @Autowired
     private QuestionDao questionDao;
 
     @Autowired
     private QuestionGroupDao questionGroupDao;
     public static final String TITLE = "Title";
 
     @Test
     @Transactional(rollbackFor = DataAccessException.class)
     public void shouldDefineQuestion() throws ApplicationException {
         String questionTitle = TITLE + System.currentTimeMillis();
         QuestionDetail questionDetail = defineQuestion(questionTitle, DATE);
         assertNotNull(questionDetail);
         Integer questionId = questionDetail.getId();
         assertNotNull(questionId);
         Question questionEntity = questionDao.getDetails(questionId);
         assertNotNull(questionEntity);
         assertEquals(questionTitle, questionEntity.getShortName());
         assertEquals(questionTitle, questionEntity.getQuestionText());
         assertEquals(AnswerType.DATE, questionEntity.getAnswerTypeAsEnum());
     }
 
     @Test
     @Transactional(rollbackFor = DataAccessException.class)
     public void shouldDefineQuestionGroup() throws ApplicationException {
         String title = TITLE + System.currentTimeMillis();
         QuestionDetail questionDetail1 = defineQuestion(title + 1, NUMERIC);
         QuestionDetail questionDetail2 = defineQuestion(title + 2, FREETEXT);
         SectionDefinition section1 = getSectionWithQuestionId("S1", questionDetail1.getId());
         SectionDefinition section2 = getSectionWithQuestionId("S2", questionDetail2.getId());
         QuestionGroupDetail questionGroupDetail = defineQuestionGroup(title, "Create", "Client", asList(section1, section2));
         assertNotNull(questionGroupDetail);
         Integer questionGroupId = questionGroupDetail.getId();
         assertNotNull(questionGroupId);
         QuestionGroup questionGroup = questionGroupDao.getDetails(questionGroupId);
         assertNotNull(questionGroup);
         assertEquals(title, questionGroup.getTitle());
         assertEquals(QuestionGroupState.ACTIVE, questionGroup.getState());
         List<Section> sections = questionGroup.getSections();
         assertEquals(2, sections.size());
         assertEquals("S1", sections.get(0).getName());
         assertEquals(title + 1, sections.get(0).getQuestions().get(0).getQuestion().getShortName());
         assertEquals("S2", sections.get(1).getName());
         assertEquals(title + 2, sections.get(1).getQuestions().get(0).getQuestion().getShortName());
         verifyCreationDate(questionGroup);
         verifyEventSources(questionGroup);
     }
 
     @Test
     @Transactional(rollbackFor = DataAccessException.class)
     public void shouldGetAllQuestions() throws ApplicationException {
         int initialCountOfQuestions = questionnaireService.getAllQuestions().size();
         defineQuestion("Q1" + System.currentTimeMillis(), NUMERIC);
         defineQuestion("Q2" + System.currentTimeMillis(), FREETEXT);
         int finalCountOfQuestions = questionnaireService.getAllQuestions().size();
         assertThat(finalCountOfQuestions - initialCountOfQuestions, is(2));
     }
 
     @Test
     @Transactional(rollbackFor = DataAccessException.class)
     public void shouldGetAllQuestionGroups() throws ApplicationException {
         int initialCount = questionnaireService.getAllQuestionGroups().size();
         String questionGroupTitle1 = "QG1" + System.currentTimeMillis();
         String questionGroupTitle2 = "QG2" + System.currentTimeMillis();
        List<SectionDefinition> sectionsForQG1 = asList(getSection("S1"));
        defineQuestionGroup(questionGroupTitle1, "Create", "Client", sectionsForQG1);
        List<SectionDefinition> sectionsForQG2 = asList(getSection("S2"), getSection("S1"));
        defineQuestionGroup(questionGroupTitle2, "Create", "Client", sectionsForQG2);
         List<QuestionGroupDetail> questionGroups = questionnaireService.getAllQuestionGroups();
         int finalCount = questionGroups.size();
         assertThat(finalCount - initialCount, is(2));
        assertThat(questionGroups, hasItems(getQuestionGroupDetailMatcher(questionGroupTitle1, sectionsForQG1),
                getQuestionGroupDetailMatcher(questionGroupTitle2, sectionsForQG2)));
     }
 
     @Test
     @Transactional(rollbackFor = DataAccessException.class)
     public void shouldGetQuestionGroupById() throws ApplicationException {
         String title = "QG1" + System.currentTimeMillis();
         QuestionGroupDetail createdQuestionGroupDetail = defineQuestionGroup(title, "Create", "Client", asList(getSection("S1"), getSection("S2")));
         QuestionGroupDetail retrievedQuestionGroupDetail = questionnaireService.getQuestionGroup(createdQuestionGroupDetail.getId());
         assertNotSame(createdQuestionGroupDetail, retrievedQuestionGroupDetail);
         assertThat(retrievedQuestionGroupDetail.getTitle(), is(title));
         List<SectionDefinition> sectionDefinitions = retrievedQuestionGroupDetail.getSectionDefinitions();
         assertThat(sectionDefinitions, is(not(nullValue())));
         assertThat(sectionDefinitions.size(), is(2));
         List<SectionDefinition> sectionDefinitionList = retrievedQuestionGroupDetail.getSectionDefinitions();
         assertThat(sectionDefinitionList.get(0).getName(), is("S1"));
         assertThat(sectionDefinitionList.get(1).getName(), is("S2"));
         EventSource eventSource = retrievedQuestionGroupDetail.getEventSource();
         assertThat(eventSource, is(not(nullValue())));
         assertThat(eventSource.getEvent(), is("Create"));
         assertThat(eventSource.getSource(), is("Client"));
     }
 
     @Test
     @Transactional(rollbackFor = DataAccessException.class)
     public void testGetQuestionGroupByIdFailure() throws ApplicationException {
         String title = "QG1" + System.currentTimeMillis();
         QuestionGroupDetail createdQuestionGroupDetail = defineQuestionGroup(title, "Create", "Client", asList(getSection("S1")));
         Integer maxQuestionGroupId = createdQuestionGroupDetail.getId();
         try {
             questionnaireService.getQuestionGroup(maxQuestionGroupId + 1);
         } catch (ApplicationException e) {
             assertThat(e.getKey(), is(QuestionnaireConstants.QUESTION_GROUP_NOT_FOUND));
         }
     }
 
     @Test
     @Transactional(rollbackFor = DataAccessException.class)
     public void shouldGetQuestionById() throws ApplicationException {
         String title = "Q1" + System.currentTimeMillis();
         QuestionDetail createdQuestionDetail = defineQuestion(title, QuestionType.FREETEXT);
         QuestionDetail retrievedQuestionDetail = questionnaireService.getQuestion(createdQuestionDetail.getId());
         assertNotSame(createdQuestionDetail, retrievedQuestionDetail);
         assertThat(retrievedQuestionDetail.getText(), is(title));
         assertThat(retrievedQuestionDetail.getShortName(), is(title));
         assertThat(retrievedQuestionDetail.getType(), is(QuestionType.FREETEXT));
     }
 
     @Test
     @Transactional(rollbackFor = DataAccessException.class)
     public void testGetQuestionByIdFailure() throws ApplicationException {
         String title = "Q1" + System.currentTimeMillis();
         QuestionDetail createdQuestionDetail = defineQuestion(title, QuestionType.DATE);
         Integer maxQuestionId = createdQuestionDetail.getId();
         try {
             questionnaireService.getQuestion(maxQuestionId + 1);
         } catch (ApplicationException e) {
             assertThat(e.getKey(), is(QuestionnaireConstants.QUESTION_NOT_FOUND));
         }
     }
 
     @Test
     @Transactional(rollbackFor = DataAccessException.class)
     public void shouldThrowExceptionForDuplicateQuestion() throws ApplicationException {
         long offset = System.currentTimeMillis();
         String questionTitle = TITLE + offset;
         defineQuestion(questionTitle, DATE);
         try {
             defineQuestion(questionTitle, FREETEXT);
             Assert.fail("Exception should have been thrown for duplicate question title");
         } catch (ApplicationException e) {
             assertEquals(QuestionnaireConstants.DUPLICATE_QUESTION, e.getKey());
         }
     }
 
     @Test
     @Transactional
     public void testIsDuplicateQuestion() throws ApplicationException {
         String questionTitle = TITLE + System.currentTimeMillis();
         boolean result = questionnaireService.isDuplicateQuestion(new QuestionDefinition(questionTitle, FREETEXT));
         assertThat(result, is(false));
         defineQuestion(questionTitle, DATE);
         result = questionnaireService.isDuplicateQuestion(new QuestionDefinition(questionTitle, FREETEXT));
         assertThat(result, is(true));
     }
 
     @Test
     @Transactional(rollbackFor = DataAccessException.class)
     public void shouldRetrieveAllEventSources() {
         List<EventSource> eventSources = questionnaireService.getAllEventSources();
         assertNotNull(eventSources);
         assertThat(eventSources, new EventSourceMatcher("Create", "Client", "Create Client"));
         assertThat(eventSources, new EventSourceMatcher("View", "Client", "View Client"));
     }
 
     private QuestionDetail defineQuestion(String questionTitle, QuestionType questionType) throws ApplicationException {
         return questionnaireService.defineQuestion(new QuestionDefinition(questionTitle, questionType));
     }
 
     private QuestionGroupDetail defineQuestionGroup(String title, String event, String source, List<SectionDefinition> sectionDefinitions) throws ApplicationException {
         return questionnaireService.defineQuestionGroup(new QuestionGroupDefinition(title, new EventSource(event, source, null), sectionDefinitions));
     }
 
     private SectionDefinition getSection(String name) throws ApplicationException {
         SectionDefinition section = new SectionDefinition();
         section.setName(name);
        String questionTitle = "Question" + name + System.currentTimeMillis();
        section.addQuestion(new SectionQuestionDetail(defineQuestion(questionTitle, NUMERIC).getId(), true));
         return section;
     }
 
     private SectionDefinition getSectionWithQuestionId(String name, int questionId) throws ApplicationException {
         SectionDefinition section = new SectionDefinition();
         section.setName(name);
         section.addQuestion(new SectionQuestionDetail(questionId, true));
         return section;
     }
 
     private void verifyCreationDate(QuestionGroup questionGroup) {
         Calendar creationDate = Calendar.getInstance();
         creationDate.setTime(questionGroup.getDateOfCreation());
         Calendar currentDate = Calendar.getInstance();
         assertThat(creationDate.get(Calendar.DATE), is(currentDate.get(Calendar.DATE)));
         assertThat(creationDate.get(Calendar.MONTH), is(currentDate.get(Calendar.MONTH)));
         assertThat(creationDate.get(Calendar.YEAR), is(currentDate.get(Calendar.YEAR)));
     }
 
     private void verifyEventSources(QuestionGroup questionGroup) {
         Set<EventSourceEntity> eventSources = questionGroup.getEventSources();
         assertNotNull(eventSources);
         assertEquals(1, eventSources.size());
         EventSourceEntity eventSourceEntity = eventSources.toArray(new EventSourceEntity[eventSources.size()])[0];
         assertEquals("Create", eventSourceEntity.getEvent().getName());
         assertEquals("Client", eventSourceEntity.getSource().getEntityType());
         assertEquals("Create Client", eventSourceEntity.getDescription());
     }
 
     private QuestionGroupDetailMatcher getQuestionGroupDetailMatcher(String questionGroupTitle, List<SectionDefinition> sectionDefinitions) {
         return new QuestionGroupDetailMatcher(new QuestionGroupDetail(0, questionGroupTitle, sectionDefinitions));
     }
 }
 
 
 class QuestionGroupDetailMatcher extends TypeSafeMatcher<QuestionGroupDetail> {
     private QuestionGroupDetail questionGroupDetail;
 
     public QuestionGroupDetailMatcher(QuestionGroupDetail questionGroupDetail) {
         this.questionGroupDetail = questionGroupDetail;
     }
 
     @Override
     public boolean matchesSafely(QuestionGroupDetail questionGroupDetail) {
         if (StringUtils.equals(this.questionGroupDetail.getTitle(), questionGroupDetail.getTitle())) {
             for (SectionDefinition sectionDefinition : this.questionGroupDetail.getSectionDefinitions()) {
                 assertThat(questionGroupDetail.getSectionDefinitions(), hasItem(new QuestionGroupSectionMatcher(sectionDefinition)));
             }
         } else {
             return false;
         }
         return true;
     }
 
     @Override
     public void describeTo(Description description) {
         description.appendText("QuestionGroupDetail do not match.");
     }
 }
 
 class QuestionGroupSectionMatcher extends TypeSafeMatcher<SectionDefinition> {
     private SectionDefinition sectionDefinition;
 
     public QuestionGroupSectionMatcher(SectionDefinition sectionDefinition) {
         this.sectionDefinition = sectionDefinition;
     }
 
     @Override
     public boolean matchesSafely(SectionDefinition sectionDefinition) {
         return StringUtils.equals(this.sectionDefinition.getName(), sectionDefinition.getName());
     }
 
     @Override
     public void describeTo(Description description) {
         description.appendText("QuestionGroup sections do not match");
     }
 }
