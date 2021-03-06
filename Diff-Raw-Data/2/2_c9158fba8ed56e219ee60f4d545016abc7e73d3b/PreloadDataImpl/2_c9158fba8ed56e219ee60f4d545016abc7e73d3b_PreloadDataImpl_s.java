 /******************************************************************************
  * PreloadDataImpl.java - created by aaronz@vt.edu
  * 
  * Copyright (c) 2007 Virginia Polytechnic Institute and State University
  * Licensed under the Educational Community License version 1.0
  * 
  * A copy of the Educational Community License has been included in this 
  * distribution and is available at: http://www.opensource.org/licenses/ecl1.php
  * 
  * Contributors:
  * Aaron Zeckoski (aaronz@vt.edu) - primary
  * Antranig Basman (antranig@caret.cam.ac.uk)
  *****************************************************************************/
 
 package org.sakaiproject.evaluation.dao.impl;
 
 import java.util.Date;
 import java.util.HashSet;
 import java.util.Set;
 
 import org.apache.commons.logging.Log;
 import org.apache.commons.logging.LogFactory;
 import org.sakaiproject.evaluation.dao.EvaluationDao;
 import org.sakaiproject.evaluation.logic.EvalSettings;
 import org.sakaiproject.evaluation.logic.utils.SettingsLogicUtils;
 import org.sakaiproject.evaluation.model.EvalConfig;
 import org.sakaiproject.evaluation.model.EvalEmailTemplate;
 import org.sakaiproject.evaluation.model.EvalItem;
 import org.sakaiproject.evaluation.model.EvalItemGroup;
 import org.sakaiproject.evaluation.model.EvalScale;
 import org.sakaiproject.evaluation.model.constant.EvalConstants;
 
 /**
  * This checks and preloads any data that is needed for the evaluation app
  * 
  * @author Aaron Zeckoski (aaronz@vt.edu)
  */
 public class PreloadDataImpl {
 
 	private static Log log = LogFactory.getLog(PreloadDataImpl.class);
 
 	private static final String ADMIN_OWNER = "admin";
 
 	private EvaluationDao evaluationDao;
 	public void setEvaluationDao(EvaluationDao evaluationDao) {
 		this.evaluationDao = evaluationDao;
 	}
 
 
 	// a few things we will need in the various other parts
 	private EvalScale agreeDisagree;
 
 
 	public void init() {
 		preloadEvalConfig();
 		preloadEmailTemplate();
 		preloadScales();
 		preloadExpertItems();
 	}
 
 	/**
 	 * Preload the default system configuration settings<br/> <b>Note:</b> If
 	 * you attempt to save a null value here in the preload it will cause this
 	 * to fail, just comment out or do not include the setting you want to
 	 * "save" as null to have the effect without causing a failure
 	 */
 	public void preloadEvalConfig() {
 		// check if there are any EvalConfig items present
 		if (evaluationDao.findAll(EvalConfig.class).isEmpty()) {
 			// Default Instructor system settings
 			saveConfig(EvalSettings.INSTRUCTOR_ALLOWED_CREATE_EVALUATIONS, true);
 			saveConfig(EvalSettings.INSTRUCTOR_ALLOWED_VIEW_RESULTS, true);
 			saveConfig(EvalSettings.INSTRUCTOR_ALLOWED_EMAIL_STUDENTS, true);
 			// leave this out to use the setting in the evaluation
 			//saveConfig(EvalSettings.INSTRUCTOR_MUST_USE_EVALS_FROM_ABOVE, EvalConstants.INSTRUCTOR_OPT_OUT);
 
 			saveConfig(EvalSettings.INSTRUCTOR_ADD_ITEMS_NUMBER, 5);
 
 			// Default Student settings
 			saveConfig(EvalSettings.STUDENT_ALLOWED_LEAVE_UNANSWERED, true);
 			saveConfig(EvalSettings.STUDENT_MODIFY_RESPONSES, false);
 			saveConfig(EvalSettings.STUDENT_VIEW_RESULTS, false);
 
 			// Default Admin settings
 			saveConfig(EvalSettings.ADMIN_ADD_ITEMS_NUMBER, 5);
 			saveConfig(EvalSettings.ADMIN_VIEW_BELOW_RESULTS, false);
 			saveConfig(EvalSettings.ADMIN_VIEW_INSTRUCTOR_ADDED_RESULTS, false);
 
 			// Default general settings
 			saveConfig(EvalSettings.FROM_EMAIL_ADDRESS, "helpdesk@institution.edu");
 			saveConfig(EvalSettings.RESPONSES_REQUIRED_TO_VIEW_RESULTS, 5);
 			saveConfig(EvalSettings.NOT_AVAILABLE_ALLOWED, true);
 			saveConfig(EvalSettings.ITEMS_ALLOWED_IN_QUESTION_BLOCK, 10);
 			saveConfig(EvalSettings.TEMPLATE_SHARING_AND_VISIBILITY, EvalConstants.SHARING_OWNER);
 			saveConfig(EvalSettings.USE_EXPERT_TEMPLATES, true);
 			saveConfig(EvalSettings.USE_EXPERT_ITEMS, true);
 			saveConfig(EvalSettings.REQUIRE_COMMENTS_BLOCK, true);
 			saveConfig(EvalSettings.EVAL_RECENTLY_CLOSED_DAYS, 10);
 
 			// default is configurable (unset)
 			//saveConfig(EvalSettings.ITEM_USE_COURSE_CATEGORY_ONLY, false);
 			saveConfig(EvalSettings.EVAL_USE_STOP_DATE, false);
 			saveConfig(EvalSettings.EVAL_USE_SAME_VIEW_DATES, true);
 			saveConfig(EvalSettings.EVAL_MIN_TIME_DIFF_BETWEEN_START_DUE, 4);
 
 			log.info("Preloaded " + evaluationDao.countAll(EvalConfig.class) + " evaluation system EvalConfig items");
 		}
 	}
 
 	private void saveConfig(String key, boolean value) {
 		saveConfig(key, value ? "true" : "false");
 	}
 
 	private void saveConfig(String key, int value) {
 		saveConfig(key, Integer.toString(value));
 	}
 
 	private void saveConfig(String key, String value) {
 		evaluationDao.save(new EvalConfig(new Date(), SettingsLogicUtils.getName(key), value));
 	}
 
 
 	/**
 	 * Preload the default email template
 	 */
 	public void preloadEmailTemplate() {
 
 		// check if there are any emailTemplates present
 		if (evaluationDao.findAll(EvalEmailTemplate.class).isEmpty()) {
 
 			evaluationDao.save(new EvalEmailTemplate(new Date(), ADMIN_OWNER,
 					EvalConstants.EMAIL_AVAILABLE_DEFAULT_TEXT, EvalConstants.EMAIL_TEMPLATE_DEFAULT_AVAILABLE));
 			evaluationDao.save(new EvalEmailTemplate(new Date(), ADMIN_OWNER,
 					EvalConstants.EMAIL_REMINDER_DEFAULT_TEXT, EvalConstants.EMAIL_TEMPLATE_DEFAULT_REMINDER));
 
 			log.info("Preloaded " + evaluationDao.countAll(EvalEmailTemplate.class) + " evaluation EmailTemplates");
 		}
 	}
 
 
 	/**
 	 * Preload the default expert built scales into the database
 	 */
 	public void preloadScales() {
 
 		// check if there are any scales present
 		if (evaluationDao.findAll(EvalScale.class).isEmpty()) {
 			// NOTE: If you change the number of scales here
 			// you will need to update the test in EvaluationDaoImplTest and EvalScalesLogicImplTest also
 
 			// initial VT scales
 			agreeDisagree = saveScale("Agree disagree scale", new String[] { "Strongly Disagree", "Disagree", "Uncertain", "Agree",
 					"Strongly agree" });
 			saveScale("Frequency scale", new String[] { "Hardly ever", "Occasionally", "Sometimes", "Frequently",
 					"Always" });
 			saveScale("Relative rating scale", new String[] { "Poor", "Fair", "Good", "Excellent" });
 			saveScale("Averages scale", new String[] { "Less than Average", "Average", "More than Average" });
 			// initial demographic scales
 			saveScale("Gender scale", new String[] { "Female", "Male" });
 			saveScale("Class requirements scale", new String[] { "Req. in Major", "Req. out of Major",
 					"Elective filling Req.", "Free Elec. in Major", "Free Elec. out of Major" });
 			saveScale("Student year scale", new String[] { "Fresh", "Soph", "Junior", "Senior", "Master", "Doctoral" });
 			saveScale("Student grade scale", new String[] { "F", "D", "C", "B", "A", "Pass" });
 			saveScale("Business major scale", new String[] { "MGT", "MSCI", "MKTG", "FIN", "ACIS", "ECON", "OTHER" });
 			saveScale("Business student yr scale", new String[] { "Freshman", "Sophomore", "Junior", "Senior",
 					"Graduate" });
 			// initial expert scales
 			saveScale("Effectiveness scale", new String[] { "Not effective", "Somewhat effective",
 					"Moderately effective", "Effective", "Very effective" });
 			saveScale("Adequacy scale",
 					new String[] { "Unsatisfactory", "Inadequate", "Adequate", "Good", "Excellent" });
 			saveScale("Relationships scale", new String[] { "Much less", "Less", "Some", "More", "Much more" });
 			saveScale("Low high scale", new String[] { "Very low", "High", "Moderately high", "High", "Very high" });
 			saveScale("Speed scale", new String[] { "Too slow", "Appropriate", "Too fast" });
 
 			log.info("Preloaded " + evaluationDao.countAll(EvalScale.class) + " evaluation scales");
 		}
 	}
 
 	/**
 	 * @param title
 	 * @param options
 	 * @return a persisted {@link EvalScale}
 	 */
 	private EvalScale saveScale(String title, String[] options) {
 		EvalScale scale = new EvalScale(new Date(), ADMIN_OWNER, title, EvalConstants.SHARING_PUBLIC, Boolean.TRUE,
 				"", EvalConstants.SCALE_IDEAL_HIGH, options, Boolean.TRUE);
 		evaluationDao.save(scale);
 		return scale;
 	}
 
 
 	/**
 	 * Preload the default expert built items into the database
 	 */
 	public void preloadExpertItems() {
 
 		// check if there are any items present
		if (evaluationDao.findAll(EvalScale.class).isEmpty()) {
 			// NOTE: If you change the number of items here
 			// you will need to update the test in EvalItemsLogicImplTest also
 
 			// create expert items
 			Set itemSet;
 			Set groupSet;
 	
 			// student development
 			groupSet = new HashSet();
 	
 			itemSet = new HashSet();
 			itemSet.add( saveScaledExpertItem("I learned a good deal of factual material in this course", null, null, agreeDisagree, EvalConstants.ITEM_CATEGORY_COURSE) );
 			itemSet.add( saveScaledExpertItem("I gained a good understanding of principals and concepts in this field", null, null, agreeDisagree, EvalConstants.ITEM_CATEGORY_COURSE) );
 			itemSet.add( saveScaledExpertItem("I developed the a working knowledge of this field", null, null, agreeDisagree, EvalConstants.ITEM_CATEGORY_COURSE) );
 			groupSet.add( saveObjectiveGroup("Knowledge", "", itemSet) );
 	
 			itemSet = new HashSet();
 			itemSet.add( saveScaledExpertItem("I participated actively in group discussions", null, null, agreeDisagree, EvalConstants.ITEM_CATEGORY_COURSE) );
 			itemSet.add( saveScaledExpertItem("I developed leadership skills within this group", null, null, agreeDisagree, EvalConstants.ITEM_CATEGORY_COURSE) );
 			itemSet.add( saveScaledExpertItem("I developed new friendships within this group", null, null, agreeDisagree, EvalConstants.ITEM_CATEGORY_COURSE) );
 			groupSet.add( saveObjectiveGroup("Participation", "", itemSet) );
 	
 			itemSet = new HashSet();
 			itemSet.add( saveScaledExpertItem("I gained a better understanding of myself through this course", null, null, agreeDisagree, EvalConstants.ITEM_CATEGORY_COURSE) );
 			itemSet.add( saveScaledExpertItem("I developed a greater sense of personal responsibility", null, null, agreeDisagree, EvalConstants.ITEM_CATEGORY_COURSE) );
 			itemSet.add( saveScaledExpertItem("I increased my awareness of my own interests and talents", null, null, agreeDisagree, EvalConstants.ITEM_CATEGORY_COURSE) );
 			groupSet.add( saveObjectiveGroup("Self-concept", "", itemSet) );
 	
 			itemSet = new HashSet();
 			itemSet.add( saveScaledExpertItem("Group activities contributed significantly to my learning", null, null, agreeDisagree, EvalConstants.ITEM_CATEGORY_COURSE) );
 			itemSet.add( saveScaledExpertItem("Collaborative group activities helped me learn the materials", null, null, agreeDisagree, EvalConstants.ITEM_CATEGORY_COURSE) );
 			itemSet.add( saveScaledExpertItem("Working with others in the group helpded me learn more effectively", null, null, agreeDisagree, EvalConstants.ITEM_CATEGORY_COURSE) );
 			groupSet.add( saveObjectiveGroup("Interaction", "", itemSet) );
 	
 			evaluationDao.save( new EvalItemGroup(new Date(), EvalConstants.ITEM_GROUP_TYPE_CATEGORY,
 					"Student Development", "Determine how student development is perceived", null, groupSet) );
 	
 	
 			// instructor effectiveness
 			groupSet = new HashSet();
 	
 			itemSet = new HashSet();
 			itemSet.add( saveScaledExpertItem("The instructor explained material clearly and understandably", null, null, agreeDisagree, EvalConstants.ITEM_CATEGORY_INSTRUCTOR) );
 			itemSet.add( saveScaledExpertItem("The instructor handled questions well", null, null, agreeDisagree, EvalConstants.ITEM_CATEGORY_INSTRUCTOR) );
 			itemSet.add( saveScaledExpertItem("The instructor appeared to have a thorough knowledge of the subject and field", null, null, agreeDisagree, EvalConstants.ITEM_CATEGORY_INSTRUCTOR) );
 			itemSet.add( saveScaledExpertItem("The instructor taught in a manner that served my needs", null, null, agreeDisagree, EvalConstants.ITEM_CATEGORY_INSTRUCTOR) );
 			groupSet.add( saveObjectiveGroup("Skill", "", itemSet) );
 	
 			itemSet = new HashSet();
 			itemSet.add( saveScaledExpertItem("The instructor was friendly", null, null, agreeDisagree, EvalConstants.ITEM_CATEGORY_INSTRUCTOR) );
 			itemSet.add( saveScaledExpertItem("The instructor was permissive and flexible", null, null, agreeDisagree, EvalConstants.ITEM_CATEGORY_INSTRUCTOR) );
 			itemSet.add( saveScaledExpertItem("The instructor treated students with respect", null, null, agreeDisagree, EvalConstants.ITEM_CATEGORY_INSTRUCTOR) );
 			groupSet.add( saveObjectiveGroup("Climate", "", itemSet) );
 	
 			itemSet = new HashSet();
 			itemSet.add( saveScaledExpertItem("The instructor suggested specific ways students could improve", null, null, agreeDisagree, EvalConstants.ITEM_CATEGORY_INSTRUCTOR) );
 			itemSet.add( saveScaledExpertItem("The instructor gave positive feedback when students did especially well", null, null, agreeDisagree, EvalConstants.ITEM_CATEGORY_INSTRUCTOR) );
 			itemSet.add( saveScaledExpertItem("The instructor kept students informed of their progress", null, null, agreeDisagree, EvalConstants.ITEM_CATEGORY_INSTRUCTOR) );
 			groupSet.add( saveObjectiveGroup("Feedback", "", itemSet) );
 	
 			evaluationDao.save( new EvalItemGroup(new Date(), EvalConstants.ITEM_GROUP_TYPE_CATEGORY,
 					"Instructor Effectiveness", "Determine the perceived effectiveness of the instructor", null, groupSet) );
 	
 	
 			itemSet = new HashSet();
 			itemSet.add( saveScaledExpertItem("Examinations covered the important aspects of the course", null, null, agreeDisagree, EvalConstants.ITEM_CATEGORY_COURSE) );
 			itemSet.add( saveScaledExpertItem("Exams were creative and required original thought", null, null, agreeDisagree, EvalConstants.ITEM_CATEGORY_COURSE) );
 			itemSet.add( saveScaledExpertItem("Exams were reasonable in length and difficulty", null, null, agreeDisagree, EvalConstants.ITEM_CATEGORY_COURSE) );
 			itemSet.add( saveScaledExpertItem("Examination items were clearly worded", null, null, agreeDisagree, EvalConstants.ITEM_CATEGORY_COURSE) );
 			itemSet.add( saveScaledExpertItem("Exam length was appropriate for the time alloted", null, null, agreeDisagree, EvalConstants.ITEM_CATEGORY_COURSE) );
 	
 			evaluationDao.save( new EvalItemGroup(new Date(), EvalConstants.ITEM_GROUP_TYPE_CATEGORY,
 					"Exams", "Measure the perception of examinations", itemSet, null) );
 	
 	
 			itemSet = new HashSet();
 			itemSet.add( saveScaledExpertItem("Assignments were interesting and stimulating", null, null, agreeDisagree, EvalConstants.ITEM_CATEGORY_COURSE) );
 			itemSet.add( saveScaledExpertItem("Assignments made students think", null, null, agreeDisagree, EvalConstants.ITEM_CATEGORY_COURSE) );
 			itemSet.add( saveScaledExpertItem("Assignments required a reasonable amount of time and effort", null, null, agreeDisagree, EvalConstants.ITEM_CATEGORY_COURSE) );
 			itemSet.add( saveScaledExpertItem("Assignments were relevant to what was presented", null, null, agreeDisagree, EvalConstants.ITEM_CATEGORY_COURSE) );
 			itemSet.add( saveScaledExpertItem("Assignments were graded fairly", null, null, agreeDisagree, EvalConstants.ITEM_CATEGORY_COURSE) );
 	
 			evaluationDao.save( new EvalItemGroup(new Date(), EvalConstants.ITEM_GROUP_TYPE_CATEGORY,
 					"Assignments", "Measure the perception of out of class assignments", itemSet, null) );
 
 			log.info("Preloaded " + evaluationDao.countAll(EvalItem.class) + " evaluation items");
 		}
 
 	}
 
 	private EvalItem saveScaledExpertItem(String text, String description, String expertDescription, EvalScale scale, String category) {
 		EvalItem item = new EvalItem(new Date(), ADMIN_OWNER, 
 				text, description, EvalConstants.SHARING_PUBLIC, EvalConstants.ITEM_TYPE_SCALED,
 				Boolean.TRUE, expertDescription, scale, null, Boolean.FALSE, null, 
 				EvalConstants.ITEM_SCALE_DISPLAY_FULL_COLORED, category, Boolean.FALSE);
 		evaluationDao.save(item);
 		return item;
 	}
 
 	private EvalItemGroup saveObjectiveGroup(String title, String description, Set items) {
 		EvalItemGroup group = new EvalItemGroup(new Date(), EvalConstants.ITEM_GROUP_TYPE_OBJECTIVE,
 				title, description, items, null);
 		evaluationDao.save( group );
 		return group;
 	}
 }
