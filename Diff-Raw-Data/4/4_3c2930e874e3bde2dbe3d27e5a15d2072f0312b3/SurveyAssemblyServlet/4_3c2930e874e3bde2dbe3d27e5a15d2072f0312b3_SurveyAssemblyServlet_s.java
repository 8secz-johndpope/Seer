 /*
  *  Copyright (C) 2010-2012 Stichting Akvo (Akvo Foundation)
  *
  *  This file is part of Akvo FLOW.
  *
  *  Akvo FLOW is free software: you can redistribute it and modify it under the terms of
  *  the GNU Affero General Public License (AGPL) as published by the Free Software Foundation,
  *  either version 3 of the License or any later version.
  *
  *  Akvo FLOW is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
  *  without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
  *  See the GNU Affero General Public License included below for more details.
  *
  *  The full license text can also be seen at <http://www.gnu.org/licenses/agpl.html>.
  */
 
 package org.waterforpeople.mapping.app.web;
 
 
 import java.io.ByteArrayOutputStream;
 import java.util.ArrayList;
 import java.util.List;
 import java.util.Map;
 import java.util.Properties;
 import java.util.Random;
 import java.util.TreeMap;
 
 import javax.servlet.http.HttpServletRequest;
 import javax.servlet.http.HttpServletResponse;
 import javax.xml.bind.JAXBException;
 
 import org.apache.commons.lang.StringEscapeUtils;
 import org.apache.log4j.Logger;
 import org.waterforpeople.mapping.app.web.dto.SurveyAssemblyRequest;
 import org.waterforpeople.mapping.dao.SurveyContainerDao;
 
 import com.gallatinsystems.common.domain.UploadStatusContainer;
 import com.gallatinsystems.common.util.PropertyUtil;
 import com.gallatinsystems.common.util.UploadUtil;
 import com.gallatinsystems.common.util.ZipUtil;
 import com.gallatinsystems.framework.rest.AbstractRestApiServlet;
 import com.gallatinsystems.framework.rest.RestRequest;
 import com.gallatinsystems.framework.rest.RestResponse;
 import com.gallatinsystems.messaging.dao.MessageDao;
 import com.gallatinsystems.messaging.domain.Message;
 import com.gallatinsystems.survey.dao.QuestionDao;
 import com.gallatinsystems.survey.dao.QuestionGroupDao;
 import com.gallatinsystems.survey.dao.SurveyDAO;
 import com.gallatinsystems.survey.dao.SurveyGroupDAO;
 import com.gallatinsystems.survey.dao.SurveyXMLFragmentDao;
 import com.gallatinsystems.survey.domain.Question;
 import com.gallatinsystems.survey.domain.QuestionGroup;
 import com.gallatinsystems.survey.domain.QuestionHelpMedia;
 import com.gallatinsystems.survey.domain.QuestionOption;
 import com.gallatinsystems.survey.domain.ScoringRule;
 import com.gallatinsystems.survey.domain.Survey;
 import com.gallatinsystems.survey.domain.SurveyContainer;
 import com.gallatinsystems.survey.domain.SurveyGroup;
 import com.gallatinsystems.survey.domain.SurveyXMLFragment;
 import com.gallatinsystems.survey.domain.SurveyXMLFragment.FRAGMENT_TYPE;
 import com.gallatinsystems.survey.domain.Translation;
 import com.gallatinsystems.survey.domain.xml.AltText;
 import com.gallatinsystems.survey.domain.xml.Dependency;
 import com.gallatinsystems.survey.domain.xml.Help;
 import com.gallatinsystems.survey.domain.xml.ObjectFactory;
 import com.gallatinsystems.survey.domain.xml.Option;
 import com.gallatinsystems.survey.domain.xml.Options;
 import com.gallatinsystems.survey.domain.xml.Score;
 import com.gallatinsystems.survey.domain.xml.Scoring;
 import com.gallatinsystems.survey.domain.xml.ValidationRule;
 import com.gallatinsystems.survey.xml.SurveyXMLAdapter;
 import com.google.appengine.api.backends.BackendServiceFactory;
 import com.google.appengine.api.datastore.Text;
 import com.google.appengine.api.taskqueue.Queue;
 import com.google.appengine.api.taskqueue.QueueFactory;
 import com.google.appengine.api.taskqueue.TaskOptions;
 
 public class SurveyAssemblyServlet extends AbstractRestApiServlet {
 	private static final Logger log = Logger
 			.getLogger(SurveyAssemblyServlet.class.getName());
 
 	private static final int BACKEND_QUESTION_THRESHOLD = 80;
 	private static final String BACKEND_PUBLISH_PROP = "backendpublish";
 	private static final long serialVersionUID = -6044156962558183224L;
 	private static final String OPTION_RENDER_MODE_PROP = "optionRenderMode";
 	public static final String FREE_QUESTION_TYPE = "free";
 	public static final String OPTION_QUESTION_TYPE = "option";
 	public static final String GEO_QUESTION_TYPE = "geo";
 	public static final String VIDEO_QUESTION_TYPE = "video";
 	public static final String PHOTO_QUESTION_TYPE = "photo";
 	public static final String SCAN_QUESTION_TYPE = "scan";
 	public static final String STRENGTH_QUESTION_TYPE = "strength";
 	public static final String DATE_QUESTION_TYPE = "date";
 
 	private static final String SURVEY_UPLOAD_URL = "surveyuploadurl";
 	private static final String SURVEY_UPLOAD_DIR = "surveyuploaddir";
 	private static final String SURVEY_UPLOAD_SIG = "surveyuploadsig";
 	private static final String SURVEY_UPLOAD_POLICY = "surveyuploadpolicy";
 	private static final String S3_ID = "aws_identifier";
 
 	private Random randomNumber = new Random();
 
 	@Override
 	protected RestRequest convertRequest() throws Exception {
 		HttpServletRequest req = getRequest();
 		RestRequest restRequest = new SurveyAssemblyRequest();
 		restRequest.populateFromHttpRequest(req);
 		return restRequest;
 	}
 
 	@Override
 	protected RestResponse handleRequest(RestRequest req) throws Exception {
 		RestResponse response = new RestResponse();
 		SurveyAssemblyRequest importReq = (SurveyAssemblyRequest) req;
 		if (SurveyAssemblyRequest.ASSEMBLE_SURVEY.equalsIgnoreCase(importReq
 				.getAction())) {
 
 			QuestionDao questionDao = new QuestionDao();
 			boolean useBackend = false;
 			// make sure we're not already running on a backend and that we are
 			// allowed to use one
 			if (!importReq.getIsForwarded()
 					&& "true".equalsIgnoreCase(PropertyUtil
 							.getProperty(BACKEND_PUBLISH_PROP))) {
 				// if we're allowed to use a backend, then check to see if we
 				// need to (based on survey size)
 				List<Question> questionList = questionDao
 						.listQuestionsBySurvey(importReq.getSurveyId());
 				if (questionList != null
 						&& questionList.size() > BACKEND_QUESTION_THRESHOLD) {
 					useBackend = true;
 				}
 			}
 			if (useBackend) {
 				com.google.appengine.api.taskqueue.TaskOptions options = com.google.appengine.api.taskqueue.TaskOptions.Builder
 						.withUrl("/app_worker/surveyassembly")
 						.param(SurveyAssemblyRequest.ACTION_PARAM,
 								SurveyAssemblyRequest.ASSEMBLE_SURVEY)
 						.param(SurveyAssemblyRequest.IS_FWD_PARAM, "true")
 						.param(SurveyAssemblyRequest.SURVEY_ID_PARAM,
 								importReq.getSurveyId().toString());
 				// change the host so the queue invokes the backend
 				options = options
 						.header("Host",
 								BackendServiceFactory.getBackendService()
 										.getBackendAddress("dataprocessor"));
 				com.google.appengine.api.taskqueue.Queue queue = com.google.appengine.api.taskqueue.QueueFactory
 						.getQueue("surveyAssembly");
 				queue.add(options);
 			} else {
 				// assembleSurvey(importReq.getSurveyId());
 				assembleSurveyOnePass(importReq.getSurveyId());
 			}
 		} else if (SurveyAssemblyRequest.DISPATCH_ASSEMBLE_QUESTION_GROUP
 				.equalsIgnoreCase(importReq.getAction())) {
 			this.dispatchAssembleQuestionGroup(importReq.getSurveyId(),
 					importReq.getQuestionGroupId(),
 					importReq.getTransactionId());
 		} else if (SurveyAssemblyRequest.ASSEMBLE_QUESTION_GROUP
 				.equalsIgnoreCase(importReq.getAction())) {
 			assembleQuestionGroups(importReq.getSurveyId(),
 					importReq.getTransactionId());
 		} else if (SurveyAssemblyRequest.DISTRIBUTE_SURVEY
 				.equalsIgnoreCase(importReq.getAction())) {
 			uploadSurvey(importReq.getSurveyId(), importReq.getTransactionId());
 		} else if (SurveyAssemblyRequest.CLEANUP.equalsIgnoreCase(importReq
 				.getAction())) {
 			cleanupFragments(importReq.getSurveyId(),
 					importReq.getTransactionId());
 		}
 
 		return response;
 	}
 
 	/**
 	 * uploads full survey XML to S3
 	 *
 	 * @param surveyId
 	 */
 	private void uploadSurvey(Long surveyId, Long transactionId) {
 		SurveyContainerDao scDao = new SurveyContainerDao();
 		SurveyContainer sc = scDao.findBySurveyId(surveyId);
 		Properties props = System.getProperties();
 		String document = sc.getSurveyDocument().getValue();
 		boolean uploadedFile = UploadUtil.sendStringAsFile(sc.getSurveyId()
 				+ ".xml", document, props.getProperty(SURVEY_UPLOAD_DIR),
 				props.getProperty(SURVEY_UPLOAD_URL), props.getProperty(S3_ID),
 				props.getProperty(SURVEY_UPLOAD_POLICY),
 				props.getProperty(SURVEY_UPLOAD_SIG), "text/xml");
 
 		ByteArrayOutputStream os = ZipUtil.generateZip(document,
 				sc.getSurveyId() + ".xml");
 
 		boolean uploadedZip = UploadUtil.upload(os, sc.getSurveyId() + ".zip",
 				props.getProperty(SURVEY_UPLOAD_DIR),
 				props.getProperty(SURVEY_UPLOAD_URL), props.getProperty(S3_ID),
 				props.getProperty(SURVEY_UPLOAD_POLICY),
 				props.getProperty(SURVEY_UPLOAD_SIG), "application/zip", null);
 
 		sendQueueMessage(SurveyAssemblyRequest.CLEANUP, surveyId, null,
 				transactionId);
 
 		Message message = new Message();
 		message.setActionAbout("surveyAssembly");
 		message.setObjectId(surveyId);
 		// String messageText = CONSTANTS.surveyPublishOkMessage() + " "
 		// + url;
 
 		if (uploadedFile && uploadedZip) {
 			// increment the version so devices know to pick up the changes
 			SurveyDAO surveyDao = new SurveyDAO();
 			surveyDao.incrementVersion(surveyId);
 
 			String messageText = "Published.  Please check: "
 					+ props.getProperty(SURVEY_UPLOAD_URL)
 					+ props.getProperty(SURVEY_UPLOAD_DIR) + "/" + surveyId
 					+ ".xml";
 			message.setShortMessage(messageText);
 
 			Survey s = surveyDao.getById(surveyId);
 			if (s != null) {
 				message.setObjectTitle(s.getPath() + "/" + s.getName());
 			}
 
 			message.setTransactionUUID(transactionId.toString());
 			MessageDao messageDao = new MessageDao();
 			messageDao.save(message);
 		} else {
 			// String messageText =
 			// CONSTANTS.surveyPublishErrorMessage();
 			String messageText = "Failed to publish: " + surveyId + "\n";
 			message.setTransactionUUID(transactionId.toString());
 			message.setShortMessage(messageText);
 			MessageDao messageDao = new MessageDao();
 			messageDao.save(message);
 		}
 	}
 	
 	/**
 	 * deletes fragments for the survey
 	 *
 	 * @param surveyId
 	 */
 	private void cleanupFragments(Long surveyId, Long transactionId) {
 		SurveyXMLFragmentDao sxmlfDao = new SurveyXMLFragmentDao();
 		sxmlfDao.deleteFragmentsForSurvey(surveyId, transactionId);
 	}
 
 	@Override
 	protected void writeOkResponse(RestResponse resp) throws Exception {
 		HttpServletResponse httpResp = getResponse();
 		httpResp.setStatus(HttpServletResponse.SC_OK);
 		// httpResp.setContentType("text/plain");
 		httpResp.getWriter().print("OK");
 		httpResp.flushBuffer();
 	}
 
 	private void assembleSurveyOnePass(Long surveyId) {
 		/**************
 		 * 1, Select survey based on surveyId 2. Retrieve all question groups
 		 * fire off queue tasks
 		 */
 		log.warn("Starting assembly of " + surveyId);
 		// Swap with proper UUID
 		SurveyDAO surveyDao = new SurveyDAO();
 		Survey s = surveyDao.getById(surveyId);
 		SurveyGroupDAO surveyGroupDao = new SurveyGroupDAO();
 		SurveyGroup sg = surveyGroupDao.getByKey(s.getSurveyGroupId());
 		Long transactionId = randomNumber.nextLong();
 		String surveyHeader = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><survey";
 		String lang = "en";
 		if (s != null && s.getDefaultLanguageCode() != null) {
 			lang = s.getDefaultLanguageCode();
 		}
		surveyHeader += " defaultLanguageCode='" + lang + "' version='" + s.getVersion() + "'>";
 		String surveyFooter = "</survey>";
 		QuestionGroupDao qgDao = new QuestionGroupDao();
 		TreeMap<Integer, QuestionGroup> qgList = qgDao
 				.listQuestionGroupsBySurvey(surveyId);
 		if (qgList != null) {
 			StringBuilder surveyXML = new StringBuilder();
 			surveyXML.append(surveyHeader);
 			for (QuestionGroup item : qgList.values()) {
 				log.warn("Assembling group " + item.getKey().getId()
 						+ " for survey " + surveyId);
 				surveyXML.append(buildQuestionGroupXML(item));
 			}
 
 			surveyXML.append(surveyFooter);
 			log.warn("Uploading " + surveyId);
 			UploadStatusContainer uc = uploadSurveyXML(surveyId,
 					surveyXML.toString());
 			Message message = new Message();
 			message.setActionAbout("surveyAssembly");
 			message.setObjectId(surveyId);
 			message.setObjectTitle(sg.getCode() + " / " + s.getName());
 			// String messageText = CONSTANTS.surveyPublishOkMessage() + " "
 			// + url;
 			if (uc.getUploadedFile() && uc.getUploadedZip()) {
 				// increment the version so devices know to pick up the changes
 				log.warn("Finishing assembly of " + surveyId);
 				surveyDao.incrementVersion(surveyId);
 				s.setStatus(Survey.Status.PUBLISHED);
 				surveyDao.save(s);
 				String messageText = "Published.  Please check: " + uc.getUrl();
 				message.setShortMessage(messageText);
 				if (qgList != null && qgList.size() > 0) {
 					for (QuestionGroup g : qgList.values()) {
 						if (g.getPath() != null) {
 							message.setObjectTitle(g.getPath());
 							break;
 						}
 					}
 				}
 				message.setTransactionUUID(transactionId.toString());
 				MessageDao messageDao = new MessageDao();
 				messageDao.save(message);
 			} else {
 				// String messageText =
 				// CONSTANTS.surveyPublishErrorMessage();
 				String messageText = "Failed to publish: " + surveyId + "\n"
 						+ uc.getMessage();
 				message.setTransactionUUID(transactionId.toString());
 				message.setShortMessage(messageText);
 				MessageDao messageDao = new MessageDao();
 				messageDao.save(message);
 			}
 			log.warn("Completed onepass assembly method for " + surveyId);
 		}
 	}
 
 	public UploadStatusContainer uploadSurveyXML(Long surveyId, String surveyXML) {
 		Properties props = System.getProperties();
 		String document = surveyXML;
 		Boolean uploadedFile = UploadUtil.sendStringAsFile(surveyId + ".xml",
 				document, props.getProperty(SURVEY_UPLOAD_DIR),
 				props.getProperty(SURVEY_UPLOAD_URL), props.getProperty(S3_ID),
 				props.getProperty(SURVEY_UPLOAD_POLICY),
 				props.getProperty(SURVEY_UPLOAD_SIG), "text/xml");
 
 		ByteArrayOutputStream os = ZipUtil.generateZip(document, surveyId
 				+ ".xml");
 		UploadStatusContainer uc = new UploadStatusContainer();
 		Boolean uploadedZip = UploadUtil.upload(os, surveyId + ".zip",
 				props.getProperty(SURVEY_UPLOAD_DIR),
 				props.getProperty(SURVEY_UPLOAD_URL), props.getProperty(S3_ID),
 				props.getProperty(SURVEY_UPLOAD_POLICY),
 				props.getProperty(SURVEY_UPLOAD_SIG), "application/zip", uc);
 		uc.setUploadedFile(uploadedFile);
 		uc.setUploadedZip(uploadedZip);
 		uc.setUrl(props.getProperty(SURVEY_UPLOAD_URL)
 				+ props.getProperty(SURVEY_UPLOAD_DIR) + "/" + surveyId
 				+ ".xml");
 		return uc;
 	}
 
 	public String buildQuestionGroupXML(QuestionGroup item) {
 		QuestionDao questionDao = new QuestionDao();
 		QuestionGroupDao questionGroupDao = new QuestionGroupDao();
 		QuestionGroup group = questionGroupDao.getByKey(item.getKey().getId());
 		TreeMap<Integer, Question> questionList = questionDao
 				.listQuestionsByQuestionGroup(item.getKey().getId(), true);
 
 		StringBuilder sb = new StringBuilder("<questionGroup><heading>")
 				.append(StringEscapeUtils.escapeXml(group.getCode())).append(
 						"</heading>");
 
 		if (questionList != null) {
 			for (Question q : questionList.values()) {
 				sb.append(marshallQuestion(q));
 			}
 		}
 		return sb.toString() + "</questionGroup>";
 	}
 
 	@SuppressWarnings("unused")
 	private void assembleSurvey(Long surveyId) {
 
 		/**************
 		 * 1, Select survey based on surveyId 2. Retrieve all question groups
 		 * fire off queue tasks
 		 */
 		QuestionGroupDao qgDao = new QuestionGroupDao();
 		TreeMap<Integer, QuestionGroup> qgList = qgDao
 				.listQuestionGroupsBySurvey(surveyId);
 		if (qgList != null) {
 			ArrayList<Long> questionGroupIdList = new ArrayList<Long>();
 			StringBuilder builder = new StringBuilder();
 			int count = 1;
 			for (QuestionGroup item : qgList.values()) {
 				questionGroupIdList.add(item.getKey().getId());
 				builder.append(item.getKey().getId());
 				if (count < qgList.size()) {
 					builder.append(",");
 				}
 				count++;
 			}
 			count = 0;
 			Long transactionId = randomNumber.nextLong();
 			sendQueueMessage(
 					SurveyAssemblyRequest.DISPATCH_ASSEMBLE_QUESTION_GROUP,
 					surveyId, builder.toString(), transactionId);
 		}
 	}
 
 	/**
 	 * sends a message to the task queue for survey assembly
 	 *
 	 * @param action
 	 * @param surveyId
 	 * @param questionGroups
 	 */
 	private void sendQueueMessage(String action, Long surveyId,
 			String questionGroups, Long transactionId) {
 		Queue surveyAssemblyQueue = QueueFactory.getQueue("surveyAssembly");
 		TaskOptions task = TaskOptions.Builder.withUrl("/app_worker/surveyassembly").param("action",
 				action).param("surveyId", surveyId.toString());
 		if (questionGroups != null) {
 			task.param("questionGroupId", questionGroups);
 		}
 		if (transactionId != null) {
 			task.param("transactionId", transactionId.toString());
 		}
 		surveyAssemblyQueue.add(task);
 	}
 
 	private void dispatchAssembleQuestionGroup(Long surveyId,
 			String questionGroupIds, Long transactionId) {
 		boolean isLast = true;
 		String currentId = questionGroupIds;
 		String remainingIds = null;
 		if (questionGroupIds.contains(",")) {
 			isLast = false;
 			currentId = questionGroupIds.substring(0,
 					questionGroupIds.indexOf(","));
 			remainingIds = questionGroupIds.substring(questionGroupIds
 					.indexOf(",") + 1);
 		}
 
 		QuestionDao questionDao = new QuestionDao();
 		QuestionGroupDao questionGroupDao = new QuestionGroupDao();
 		QuestionGroup group = questionGroupDao.getByKey(Long
 				.parseLong(currentId));
 		TreeMap<Integer, Question> questionList = questionDao
 				.listQuestionsByQuestionGroup(Long.parseLong(currentId), true);
 
 		StringBuilder sb = new StringBuilder("<questionGroup><heading>")
 				.append(group.getCode()).append("</heading>");
 
 		if (questionList != null) {
 			for (Question q : questionList.values()) {
 				sb.append(marshallQuestion(q));
 			}
 		}
 		SurveyXMLFragment sxf = new SurveyXMLFragment();
 		sxf.setSurveyId(surveyId);
 		sxf.setQuestionGroupId(Long.parseLong(currentId));
 		sxf.setFragmentOrder(group.getOrder());
 		sxf.setFragment(new Text(sb.append("</questionGroup>").toString()));
 		sxf.setTransactionId(transactionId);
 
 		sxf.setFragmentType(FRAGMENT_TYPE.QUESTION_GROUP);
 		SurveyXMLFragmentDao sxmlfDao = new SurveyXMLFragmentDao();
 		sxmlfDao.save(sxf);
 		if (isLast) {
 			// Assemble the fragments
 			sendQueueMessage(SurveyAssemblyRequest.ASSEMBLE_QUESTION_GROUP,
 					surveyId, null, transactionId);
 
 		} else {
 			sendQueueMessage(
 					SurveyAssemblyRequest.DISPATCH_ASSEMBLE_QUESTION_GROUP,
 					surveyId, remainingIds, transactionId);
 		}
 	}
 
 	private String marshallQuestion(Question q) {
 
 		SurveyXMLAdapter sax = new SurveyXMLAdapter();
 		ObjectFactory objFactory = new ObjectFactory();
 		com.gallatinsystems.survey.domain.xml.Question qXML = objFactory
 				.createQuestion();
 		qXML.setId(new String("" + q.getKey().getId() + ""));
 		// ToDo fix
 		qXML.setMandatory("false");
 		if (q.getText() != null) {
 			com.gallatinsystems.survey.domain.xml.Text t = new com.gallatinsystems.survey.domain.xml.Text();
 			t.setContent(q.getText());
 			qXML.setText(t);
 		}
 		List<Help> helpList = new ArrayList<Help>();
 		// this is here for backward compatibility
 		if (q.getTip() != null) {
 			Help tip = new Help();
 			com.gallatinsystems.survey.domain.xml.Text t = new com.gallatinsystems.survey.domain.xml.Text();
 			t.setContent(q.getTip());
 			tip.setText(t);
 			tip.setType("tip");
 			if (q.getTip() != null && q.getTip().trim().length() > 0
 					&& !"null".equalsIgnoreCase(q.getTip().trim())) {
 				helpList.add(tip);
 			}
 		}
 		if (q.getQuestionHelpMediaMap() != null) {
 			for (QuestionHelpMedia helpItem : q.getQuestionHelpMediaMap()
 					.values()) {
 				Help tip = new Help();
 				com.gallatinsystems.survey.domain.xml.Text t = new com.gallatinsystems.survey.domain.xml.Text();
 				t.setContent(helpItem.getText());
 				if (helpItem.getType() == QuestionHelpMedia.Type.TEXT) {
 					tip.setType("tip");
 				} else {
 					tip.setType(helpItem.getType().toString().toLowerCase());
 					tip.setValue(helpItem.getResourceUrl());
 				}
 				if (helpItem.getTranslationMap() != null) {
 					List<AltText> translationList = new ArrayList<AltText>();
 					for (Translation trans : helpItem.getTranslationMap()
 							.values()) {
 						AltText aText = new AltText();
 						aText.setContent(trans.getText());
 						aText.setLanguage(trans.getLanguageCode());
 						aText.setType("translation");
 						translationList.add(aText);
 					}
 					if (translationList.size() > 0) {
 						tip.setAltText(translationList);
 					}
 				}
 				helpList.add(tip);
 			}
 		}
 		if (helpList.size() > 0) {
 			qXML.setHelp(helpList);
 		}
 
 		boolean hasValidation = false;
 		if (q.getIsName() != null && q.getIsName()) {
 			ValidationRule validationRule = objFactory.createValidationRule();
 			validationRule.setValidationType("name");
 			qXML.setValidationRule(validationRule);
 			hasValidation = true;
 		} else if (q.getType() == Question.Type.NUMBER
 				&& (q.getAllowDecimal() != null || q.getAllowSign() != null
 						|| q.getMinVal() != null || q.getMaxVal() != null)) {
 			ValidationRule validationRule = objFactory.createValidationRule();
 			validationRule.setValidationType("numeric");
 			validationRule.setAllowDecimal(q.getAllowDecimal() != null ? q
 					.getAllowDecimal().toString().toLowerCase() : "false");
 			validationRule.setSigned(q.getAllowSign() != null ? q
 					.getAllowSign().toString().toLowerCase() : "false");
 			if (q.getMinVal() != null) {
 				validationRule.setMinVal(q.getMinVal().toString());
 			}
 			if (q.getMaxVal() != null) {
 				validationRule.setMaxVal(q.getMaxVal().toString());
 			}
 			qXML.setValidationRule(validationRule);
 			hasValidation = true;
 		}
 
 		qXML.setAltText(formAltText(q.getTranslationMap()));
 
 		if (q.getType().equals(Question.Type.FREE_TEXT)) {
 			qXML.setType(FREE_QUESTION_TYPE);
 		} else if (q.getType().equals(Question.Type.GEO)) {
 			qXML.setType(GEO_QUESTION_TYPE);
 		} else if (q.getType().equals(Question.Type.NUMBER)) {
 			qXML.setType(FREE_QUESTION_TYPE);
 			if (!hasValidation) {
 				ValidationRule vrule = new ValidationRule();
 				vrule.setValidationType("numeric");
 				vrule.setSigned("false");
 				qXML.setValidationRule(vrule);
 			}
 		} else if (q.getType().equals(Question.Type.OPTION)) {
 			qXML.setType(OPTION_QUESTION_TYPE);
 		} else if (q.getType().equals(Question.Type.PHOTO)) {
 			qXML.setType(PHOTO_QUESTION_TYPE);
 		} else if (q.getType().equals(Question.Type.VIDEO)) {
 			qXML.setType(VIDEO_QUESTION_TYPE);
 		} else if (q.getType().equals(Question.Type.SCAN)) {
 			qXML.setType(SCAN_QUESTION_TYPE);
 		} else if (q.getType().equals(Question.Type.NAME)) {
 			qXML.setType(FREE_QUESTION_TYPE);
 			ValidationRule vrule = new ValidationRule();
 			vrule.setValidationType("name");
 			qXML.setValidationRule(vrule);
 		} else if (q.getType().equals(Question.Type.STRENGTH)) {
 			qXML.setType(STRENGTH_QUESTION_TYPE);
 		} else if (q.getType().equals(Question.Type.DATE)) {
 			qXML.setType(DATE_QUESTION_TYPE);
 		}
 
 		if (q.getOrder() != null) {
 			qXML.setOrder(q.getOrder().toString());
 		}
 		if (q.getMandatoryFlag() != null) {
 			qXML.setMandatory(q.getMandatoryFlag().toString());
 		}
 		Dependency dependency = objFactory.createDependency();
 		if (q.getDependentQuestionId() != null) {
 			dependency.setQuestion(q.getDependentQuestionId().toString());
 			dependency.setAnswerValue(q.getDependentQuestionAnswer());
 			qXML.setDependency(dependency);
 		}
 
 		if (q.getQuestionOptionMap() != null
 				&& q.getQuestionOptionMap().size() > 0) {
 			Options options = objFactory.createOptions();
 			if (q.getAllowOtherFlag() != null) {
 				options.setAllowOther(q.getAllowOtherFlag().toString());
 			}
 			if (q.getAllowMultipleFlag() != null) {
 				options.setAllowMultiple(q.getAllowMultipleFlag().toString());
 			}
 			if (options.getAllowMultiple() == null
 					|| "false".equals(options.getAllowMultiple())) {
 				options.setRenderType(PropertyUtil
 						.getProperty(OPTION_RENDER_MODE_PROP));
 			}
 
 			ArrayList<Option> optionList = new ArrayList<Option>();
 			for (QuestionOption qo : q.getQuestionOptionMap().values()) {
 				Option option = objFactory.createOption();
 				com.gallatinsystems.survey.domain.xml.Text t = new com.gallatinsystems.survey.domain.xml.Text();
 				t.setContent(qo.getText());
 				option.addContent(t);
 				option.setValue(qo.getCode() != null ? qo.getCode() : qo
 						.getText());
 				List<AltText> altTextList = formAltText(qo.getTranslationMap());
 				if (altTextList != null) {
 					for (AltText alt : altTextList) {
 						option.addContent(alt);
 					}
 				}
 				optionList.add(option);
 			}
 			options.setOption(optionList);
 
 			qXML.setOptions(options);
 		}
 
 		if (q.getScoringRules() != null) {
 			Scoring scoring = new Scoring();
 
 			for (ScoringRule rule : q.getScoringRules()) {
 				Score score = new Score();
 				if (scoring.getType() == null) {
 					scoring.setType(rule.getType().toLowerCase());
 				}
 				score.setRangeHigh(rule.getRangeMax());
 				score.setRangeLow(rule.getRangeMin());
 				score.setValue(rule.getValue());
 				scoring.addScore(score);
 			}
 			if (scoring.getScore() != null && scoring.getScore().size() > 0) {
 				qXML.setScoring(scoring);
 			}
 		}
 
 		String questionDocument = null;
 		try {
 			questionDocument = sax.marshal(qXML);
 		} catch (JAXBException e) {
 			log.warn("Could not marshal question: " + qXML, e);
 		}
 
 		questionDocument = questionDocument
 				.replace(
 						"<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>",
 						"");
 		return questionDocument;
 	}
 
 	private List<AltText> formAltText(Map<String, Translation> translationMap) {
 		List<AltText> altTextList = new ArrayList<AltText>();
 		if (translationMap != null) {
 			for (Translation lang : translationMap.values()) {
 				AltText alt = new AltText();
 				alt.setContent(lang.getText());
 				alt.setType("translation");
 				alt.setLanguage(lang.getLanguageCode());
 				altTextList.add(alt);
 			}
 		}
 
 		return altTextList;
 	}
 
 	private void assembleQuestionGroups(Long surveyId, Long transactionId) {
 		SurveyXMLFragmentDao sxmlfDao = new SurveyXMLFragmentDao();
 		List<SurveyXMLFragment> sxmlfList = sxmlfDao.listSurveyFragments(
 				surveyId, SurveyXMLFragment.FRAGMENT_TYPE.QUESTION_GROUP,
 				transactionId);
 		StringBuilder sbQG = new StringBuilder();
 		for (SurveyXMLFragment item : sxmlfList) {
 			sbQG.append(item.getFragment().getValue());
 		}
 		StringBuilder completeSurvey = new StringBuilder();
 		String surveyHeader = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><survey>";
 		String surveyFooter = "</survey>";
 		completeSurvey.append(surveyHeader);
 		completeSurvey.append(sbQG.toString());
 		sbQG = null;
 		completeSurvey.append(surveyFooter);
 
 		SurveyContainerDao scDao = new SurveyContainerDao();
 		SurveyContainer sc = scDao.findBySurveyId(surveyId);
 		if (sc == null) {
 			sc = new SurveyContainer();
 		}
 		sc.setSurveyDocument(new Text(completeSurvey.toString()));
 		sc.setSurveyId(surveyId);
 
 		scDao.save(sc);
 
 		sendQueueMessage(SurveyAssemblyRequest.DISTRIBUTE_SURVEY, surveyId,
 				null, transactionId);
 	}
 }
