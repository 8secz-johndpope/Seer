 /*
  * Copyright (C) 2012 THM webMedia
  *
  * This file is part of ARSnova.
  *
  * ARSnova is free software: you can redistribute it and/or modify
  * it under the terms of the GNU General Public License as published by
  * the Free Software Foundation, either version 3 of the License, or
  * (at your option) any later version.
  *
  * ARSnova is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  * GNU General Public License for more details.
  *
  * You should have received a copy of the GNU General Public License
  * along with this program.  If not, see <http://www.gnu.org/licenses/>.
  */
 
 package de.thm.arsnova.dao;
 
 import java.io.IOException;
 import java.io.UnsupportedEncodingException;
 import java.net.URLEncoder;
 import java.text.SimpleDateFormat;
 import java.util.ArrayList;
 import java.util.Collection;
 import java.util.Collections;
 import java.util.Date;
 import java.util.HashMap;
 import java.util.HashSet;
 import java.util.List;
 import java.util.Map;
 import java.util.Set;
 
 import net.sf.ezmorph.Morpher;
 import net.sf.ezmorph.MorpherRegistry;
 import net.sf.ezmorph.bean.BeanMorpher;
 import net.sf.json.JSONArray;
 import net.sf.json.JSONException;
 import net.sf.json.JSONObject;
 import net.sf.json.util.JSONUtils;
 
 import org.slf4j.Logger;
 import org.slf4j.LoggerFactory;
 import org.springframework.beans.factory.annotation.Autowired;
 import org.springframework.beans.factory.annotation.Value;
 import org.springframework.stereotype.Component;
 import org.springframework.transaction.annotation.Isolation;
 import org.springframework.transaction.annotation.Transactional;
 
 import com.fourspaces.couchdb.Database;
 import com.fourspaces.couchdb.Document;
 import com.fourspaces.couchdb.View;
 import com.fourspaces.couchdb.ViewResults;
 
 import de.thm.arsnova.connector.model.Course;
 import de.thm.arsnova.entities.Answer;
 import de.thm.arsnova.entities.Feedback;
 import de.thm.arsnova.entities.FoodVote;
 import de.thm.arsnova.entities.InterposedQuestion;
 import de.thm.arsnova.entities.InterposedReadingCount;
 import de.thm.arsnova.entities.LoggedIn;
 import de.thm.arsnova.entities.PossibleAnswer;
 import de.thm.arsnova.entities.Question;
 import de.thm.arsnova.entities.Session;
 import de.thm.arsnova.entities.User;
 import de.thm.arsnova.entities.VisitedSession;
 import de.thm.arsnova.exceptions.ForbiddenException;
 import de.thm.arsnova.exceptions.NotFoundException;
 import de.thm.arsnova.exceptions.UnauthorizedException;
 import de.thm.arsnova.services.IFeedbackService;
 import de.thm.arsnova.services.ISessionService;
 import de.thm.arsnova.services.IUserService;
 
 @Component
 public class CouchDBDao implements IDatabaseDao {
 	@Autowired
 	private IUserService userService;
 
 	@Autowired
 	private IFeedbackService feedbackService;
 
 	@Autowired
 	private ISessionService sessionService;
 
 	private String databaseHost;
 	private int databasePort;
 	private String databaseName;
 	private Database database;
 
 	public static final Logger LOGGER = LoggerFactory.getLogger(CouchDBDao.class);
 
 	@Value("${couchdb.host}")
 	public final void setDatabaseHost(final String newDatabaseHost) {
 		this.databaseHost = newDatabaseHost;
 	}
 
 	@Value("${couchdb.port}")
 	public final void setDatabasePort(final String newDatabasePort) {
 		this.databasePort = Integer.parseInt(newDatabasePort);
 	}
 
 	@Value("${couchdb.name}")
 	public final void setDatabaseName(final String newDatabaseName) {
 		this.databaseName = newDatabaseName;
 	}
 
 	public final void setSessionService(final ISessionService service) {
 		this.sessionService = service;
 	}
 
 	public final void setUserService(final IUserService service) {
 		this.userService = service;
 	}
 
 	/**
 	 * This method cleans up old feedback votes at the scheduled interval.
 	 */
 	@Override
 	public final void cleanFeedbackVotes(final int cleanupFeedbackDelay) {
 		final long timelimitInMillis = 60000 * (long) cleanupFeedbackDelay;
 		final long maxAllowedTimeInMillis = System.currentTimeMillis() - timelimitInMillis;
 
 		Map<String, Set<String>> affectedUsers = new HashMap<String, Set<String>>();
 		Set<String> allAffectedSessions = new HashSet<String>();
 
 		List<Document> results = findFeedbackForDeletion(maxAllowedTimeInMillis);
 		for (Document d : results) {
 			try {
 				// Read the required document data
 				Document feedback = this.getDatabase().getDocument(d.getId());
 				String arsInternalSessionId = feedback.getString("sessionId");
 				String user = feedback.getString("user");
 
 				// Store user and session data for later. We need this to
 				// communicate the changes back to the users.
 				Set<String> affectedArsSessions = affectedUsers.get(user);
 				if (affectedArsSessions == null) {
 					affectedArsSessions = new HashSet<String>();
 				}
 				affectedArsSessions.add(getSessionKeyword(arsInternalSessionId));
 				affectedUsers.put(user, affectedArsSessions);
 				allAffectedSessions.addAll(affectedArsSessions);
 
 				this.database.deleteDocument(feedback);
 				LOGGER.debug("Cleaning up Feedback document " + d.getId());
 			} catch (IOException e) {
 				LOGGER.error("Could not delete Feedback document " + d.getId());
 			} catch (JSONException e) {
 				LOGGER.error(
 						"Could not delete Feedback document {}, error is: {} ",
 						new Object[] {d.getId(), e}
 				);
 			}
 		}
 		if (!results.isEmpty()) {
 			feedbackService.broadcastFeedbackChanges(affectedUsers, allAffectedSessions);
 		}
 	}
 
 	private List<Document> findFeedbackForDeletion(final long maxAllowedTimeInMillis) {
 		View cleanupFeedbackView = new View("understanding/cleanup");
 		cleanupFeedbackView.setStartKey("null");
 		cleanupFeedbackView.setEndKey(String.valueOf(maxAllowedTimeInMillis));
 		ViewResults feedbackForCleanup = this.getDatabase().view(cleanupFeedbackView);
 		return feedbackForCleanup.getResults();
 	}
 
 	@Override
 	public final Session getSession(final String keyword) {
 		Session result = this.getSessionFromKeyword(keyword);
 		if (result == null) {
 			throw new NotFoundException();
 		}
 		if (result.isActive() || result.getCreator().equals(userService.getCurrentUser().getUsername())) {
 			return result;
 		}
 
 		throw new ForbiddenException();
 	}
 
 	@Override
 	public final List<Session> getMySessions(final User user) {
 		try {
 			View view = new View("session/by_creator");
 			view.setStartKey("[" + URLEncoder.encode("\"" + user.getUsername() + "\"", "UTF-8") + "]");
 			view.setEndKey("[" + URLEncoder.encode("\"" + user.getUsername() + "\",{}", "UTF-8") + "]");
 
 			ViewResults sessions = this.getDatabase().view(view);
 
 			List<Session> result = new ArrayList<Session>();
 			for (Document d : sessions.getResults()) {
 				Session session = (Session) JSONObject.toBean(
 						d.getJSONObject().getJSONObject("value"),
 						Session.class
 				);
 				session.setCreator(d.getJSONObject().getJSONArray("key").getString(0));
 				session.setName(d.getJSONObject().getJSONArray("key").getString(1));
 				session.set_id(d.getId());
 				result.add(session);
 			}
 			return result;
 		} catch (UnsupportedEncodingException e) {
 			return null;
 		}
 	}
 
 	@Override
 	public final List<Question> getSkillQuestions(final String sessionKeyword) {
 		Session session = this.getSessionFromKeyword(sessionKeyword);
 		if (session == null) {
 			throw new NotFoundException();
 		}
 
 		User user = this.userService.getCurrentUser();
 		View view = null;
 
 		try {
 			if (session.getCreator().equals(user.getUsername())) {
 				view = new View("skill_question/by_session_sorted_by_subject_and_text");
 			} else {
 				if (user.getType().equals(User.THM)) {
 					view = new View("skill_question/by_session_for_thm_full");
 				} else {
 					view = new View("skill_question/by_session_for_all_full");
 				}
 			}
 			view.setStartKey("[" + URLEncoder.encode("\"" + session.get_id() + "\"", "UTF-8") + "]");
 			view.setEndKey("[" + URLEncoder.encode("\"" + session.get_id() + "\",{}", "UTF-8") + "]");
 
 			ViewResults questions = this.getDatabase().view(view);
 			if (questions == null || questions.isEmpty()) {
 				return null;
 			}
 			List<Question> result = new ArrayList<Question>();
 
 			MorpherRegistry morpherRegistry = JSONUtils.getMorpherRegistry();
 			Morpher dynaMorpher = new BeanMorpher(PossibleAnswer.class, morpherRegistry);
 			morpherRegistry.registerMorpher(dynaMorpher);
 			for (Document document : questions.getResults()) {
 				Question question = (Question) JSONObject.toBean(
 						document.getJSONObject().getJSONObject("value"),
 						Question.class
 				);
 				Collection<PossibleAnswer> answers = JSONArray.toCollection(
 						document.getJSONObject().getJSONObject("value")
 							.getJSONArray("possibleAnswers"),
 						PossibleAnswer.class
 				);
 				question.setPossibleAnswers(new ArrayList<PossibleAnswer>(answers));
 				question.setSessionKeyword(this.getSessionKeyword(question.getSessionId()));
 				if (!"freetext".equals(question.getQuestionType()) && 0 == question.getPiRound()) {
 					/* needed for legacy questions whose piRound property has not been set */
 					question.setPiRound(1);
 				}
 				result.add(question);
 			}
 
 			return result;
 		} catch (UnsupportedEncodingException e) {
 			return null;
 		} catch (IOException e) {
 			return null;
 		}
 	}
 
 	@Override
 	public final int getSkillQuestionCount(final Session session) {
 		try {
 			View view = new View("skill_question/count_by_session");
 			view.setKey(URLEncoder.encode("\"" + session.get_id() + "\"", "UTF-8"));
 			ViewResults results = this.getDatabase().view(view);
 
 			if (results.getJSONArray("rows").optJSONObject(0) == null) {
 				return 0;
 			}
 
 			return results.getJSONArray("rows").optJSONObject(0).optInt("value");
 
 		} catch (UnsupportedEncodingException e) {
 			return 0;
 		}
 	}
 
 	@Override
 	public final Session getSessionFromKeyword(final String keyword) {
 		try {
 			View view = new View("session/by_keyword");
 			view.setKey(URLEncoder.encode("\"" + keyword + "\"", "UTF-8"));
 			ViewResults results = this.getDatabase().view(view);
 
 			if (results.getJSONArray("rows").optJSONObject(0) == null) {
 				return null;
 			}
 			return (Session) JSONObject.toBean(
 					results.getJSONArray("rows").optJSONObject(0).optJSONObject("value"),
 					Session.class
 			);
 		} catch (UnsupportedEncodingException e) {
 			return null;
 		}
 	}
 
 	@Override
 	public final Session getSessionFromId(final String sessionId) {
 		try {
 			View view = new View("session/by_id");
 			view.setKey(URLEncoder.encode("\"" + sessionId + "\"", "UTF-8"));
 			ViewResults results = this.getDatabase().view(view);
 
 			if (results.getJSONArray("rows").optJSONObject(0) == null) {
 				return null;
 			}
 			return (Session) JSONObject.toBean(
 					results.getJSONArray("rows").optJSONObject(0).optJSONObject("value"),
 					Session.class
 			);
 		} catch (UnsupportedEncodingException e) {
 			return null;
 		}
 	}
 
 	@Override
 	public final Session saveSession(final Session session) {
 
 		Document sessionDocument = new Document();
 		sessionDocument.put("type", "session");
 		sessionDocument.put("name", session.getName());
 		sessionDocument.put("shortName", session.getShortName());
 		sessionDocument.put("keyword", sessionService.generateKeyword());
 		sessionDocument.put("creator", this.actualUserName());
 		sessionDocument.put("active", true);
 		sessionDocument.put("courseType", session.getCourseType());
 		sessionDocument.put("courseId", session.getCourseId());
 		try {
 			database.saveDocument(sessionDocument);
 		} catch (IOException e) {
 			return null;
 		}
 
 		return this.getSession(sessionDocument.getString("keyword"));
 	}
 
 	@Override
 	public final Feedback getFeedback(final String keyword) {
 		String sessionId = this.getSessionId(keyword);
 		if (sessionId == null) {
 			throw new NotFoundException();
 		}
 		View view = new View("understanding/by_session");
 		view.setGroup(true);
 		view.setStartKey(URLEncoder.encode("[\"" + sessionId + "\"]"));
 		view.setEndKey(URLEncoder.encode("[\"" + sessionId + "\",{}]"));
 		ViewResults results = this.getDatabase().view(view);
 
 		LOGGER.debug("Feedback: {}", results.getJSONArray("rows"));
 
 		return this.createFeedbackObject(results);
 	}
 
 	private Feedback createFeedbackObject(final ViewResults results) {
 		int[] values = {0, 0, 0, 0};
 		JSONArray rows = results.getJSONArray("rows");
 
 		try {
 			for (int i = Feedback.MIN_FEEDBACK_TYPE; i <= Feedback.MAX_FEEDBACK_TYPE; i++) {
 				String key = rows.optJSONObject(i).optJSONArray("key").getString(1);
 				JSONObject feedback = rows.optJSONObject(i);
 
 				if (key.equals("Bitte schneller")) {
 					values[Feedback.FEEDBACK_FASTER] = feedback.getInt("value");
 				}
 				if (key.equals("Kann folgen")) {
 					values[Feedback.FEEDBACK_OK] = feedback.getInt("value");
 				}
 				if (key.equals("Zu schnell")) {
 					values[Feedback.FEEDBACK_SLOWER] = feedback.getInt("value");
 				}
 				if (key.equals("Nicht mehr dabei")) {
 					values[Feedback.FEEDBACK_AWAY] = feedback.getInt("value");
 				}
 			}
 		} catch (Exception e) {
 			return new Feedback(
 					values[Feedback.FEEDBACK_FASTER],
 					values[Feedback.FEEDBACK_OK],
 					values[Feedback.FEEDBACK_SLOWER],
 					values[Feedback.FEEDBACK_AWAY]
 			);
 		}
 		return new Feedback(
 				values[Feedback.FEEDBACK_FASTER],
 				values[Feedback.FEEDBACK_OK],
 				values[Feedback.FEEDBACK_SLOWER],
 				values[Feedback.FEEDBACK_AWAY]
 		);
 	}
 
 	@Override
 	public final boolean saveFeedback(
 			final String keyword,
 			final int value,
 			final de.thm.arsnova.entities.User user
 	) {
 		String sessionId = this.getSessionId(keyword);
 		if (sessionId == null) {
 			return false;
 		}
 		if (!(value >= Feedback.MIN_FEEDBACK_TYPE && value <= Feedback.MAX_FEEDBACK_TYPE)) {
 			return false;
 		}
 
 		Document feedback = new Document();
 		List<Document> postedFeedback = findPreviousFeedback(sessionId, user);
 
 		// Feedback can only be posted once. If there already is some feedback,
 		// we need to update it.
 		if (!postedFeedback.isEmpty()) {
 			for (Document f : postedFeedback) {
 				// Use the first found feedback and update value and timestamp
 				try {
 					feedback = this.getDatabase().getDocument(f.getId());
 					feedback.put("value", feedbackValueToString(value));
 					feedback.put("timestamp", System.currentTimeMillis());
 				} catch (IOException e) {
 					return false;
 				}
 				break;
 			}
 		} else {
 			feedback.put("type", "understanding");
 			feedback.put("user", user.getUsername());
 			feedback.put("sessionId", sessionId);
 			feedback.put("timestamp", System.currentTimeMillis());
 			feedback.put("value", feedbackValueToString(value));
 		}
 
 		try {
 			this.getDatabase().saveDocument(feedback);
 		} catch (IOException e) {
 			return false;
 		}
 
 		return true;
 	}
 
 	private List<Document> findPreviousFeedback(final String sessionId, final de.thm.arsnova.entities.User user) {
 		View view = new View("understanding/by_user");
 		try {
 			view.setKey(
 					URLEncoder.encode(
 							"[\"" + sessionId + "\",\"" + user.getUsername() + "\"]",
 							"UTF-8"
 					)
 			);
 		} catch (UnsupportedEncodingException e) {
 			return Collections.<Document> emptyList();
 		}
 		ViewResults results = this.getDatabase().view(view);
 		return results.getResults();
 	}
 
 	private String feedbackValueToString(final int value) {
 		switch (value) {
 		case Feedback.FEEDBACK_FASTER:
 			return "Bitte schneller";
 		case Feedback.FEEDBACK_OK:
 			return "Kann folgen";
 		case Feedback.FEEDBACK_SLOWER:
 			return "Zu schnell";
 		case Feedback.FEEDBACK_AWAY:
 			return "Nicht mehr dabei";
 		default:
 			return null;
 		}
 	}
 
 	private int feedbackValueFromString(final String value) {
 		if (value.equals("Bitte schneller")) {
 			return Feedback.FEEDBACK_FASTER;
 		}
 		if (value.equals("Kann folgen")) {
 			return Feedback.FEEDBACK_OK;
 		}
 		if (value.equals("Zu schnell")) {
 			return Feedback.FEEDBACK_AWAY;
 		}
 		if (value.equals("Nicht mehr dabei")) {
 			return Feedback.FEEDBACK_AWAY;
 		}
 		return Integer.MIN_VALUE;
 	}
 
 	@Override
 	@Transactional(isolation = Isolation.READ_COMMITTED)
 	public final boolean sessionKeyAvailable(final String keyword) {
 		View view = new View("session/by_keyword");
 		ViewResults results = this.getDatabase().view(view);
 
 		return !results.containsKey(keyword);
 	}
 
 	private String getSessionId(final String keyword) {
 		View view = new View("session/by_keyword");
 		view.setKey(URLEncoder.encode("\"" + keyword + "\""));
 		ViewResults results = this.getDatabase().view(view);
 		if (results.getJSONArray("rows").optJSONObject(0) == null) {
 			return null;
 		}
 		return results.getJSONArray("rows").optJSONObject(0).optJSONObject("value").getString("_id");
 	}
 
 	private String getSessionKeyword(final String internalSessionId) throws IOException {
 		Document document = this.getDatabase().getDocument(internalSessionId);
 		if (document.has("keyword")) {
 			return (String) document.get("keyword");
 		}
 		LOGGER.error("No session found for internal id: {}", internalSessionId);
 		return null;
 	}
 
 	private String actualUserName() {
 		User user = userService.getCurrentUser();
 		if (user == null) {
 			return null;
 		}
 		return user.getUsername();
 	}
 
 	private Database getDatabase() {
 		if (database == null) {
 			try {
 				com.fourspaces.couchdb.Session session = new com.fourspaces.couchdb.Session(
 						databaseHost,
 						databasePort
 				);
 				database = session.getDatabase(databaseName);
 			} catch (Exception e) {
 				LOGGER.error(
 						"Cannot connect to CouchDB database '" + databaseName
 						+ "' on host '" + databaseHost
 						+ "' using port " + databasePort
 				);
 			}
 		}
 
 		return database;
 	}
 
 	@Override
 	public final Question saveQuestion(final Session session, final Question question) {
 		Document q = new Document();
 		q.put("type", "skill_question");
 		q.put("questionType", question.getQuestionType());
 		q.put("sessionId", session.get_id());
 		q.put("subject", question.getSubject());
 		q.put("text", question.getText());
 		q.put("active", question.isActive());
 		q.put("number", 0); // TODO This number has to get incremented
 							// automatically
 		q.put("releasedFor", question.getReleasedFor());
 		q.put("possibleAnswers", question.getPossibleAnswers());
 		q.put("noCorrect", question.isNoCorrect());
 		q.put("piRound", question.getPiRound());
 		q.put("showStatistic", question.isShowStatistic());
 		q.put("showAnswer", question.isShowAnswer());
 		try {
 			database.saveDocument(q);
 			question.set_id(q.getId());
 			question.set_rev(q.getRev());
 			return question;
 		} catch (IOException e) {
 			LOGGER.error("Could not save question {}", question);
 		}
 		return null;
 	}
 
 	@Override
 	public final Question updateQuestion(final Question question) {
 		try {
 			Document q = this.database.getDocument(question.get_id());
 			q.put("subject", question.getSubject());
 			q.put("text", question.getText());
 			q.put("active", question.isActive());
 			q.put("releasedFor", question.getReleasedFor());
 			q.put("possibleAnswers", question.getPossibleAnswers());
 			q.put("noCorrect", question.isNoCorrect());
 			q.put("piRound", question.getPiRound());
 			q.put("showStatistic", question.isShowStatistic());
 			q.put("showAnswer", question.isShowAnswer());
 			this.database.saveDocument(q);
 			question.set_rev(q.getRev());
 
 			return question;
 		} catch (IOException e) {
 			LOGGER.error("Could not update question {}", question);
 		}
 
 		return null;
 	}
 
 	@Override
 	public final InterposedQuestion saveQuestion(final Session session, final InterposedQuestion question) {
 		Document q = new Document();
 		q.put("type", "interposed_question");
 		q.put("sessionId", session.get_id());
 		q.put("subject", question.getSubject());
 		q.put("text", question.getText());
 		q.put("timestamp", System.currentTimeMillis());
 		q.put("read", false);
 		try {
 			database.saveDocument(q);
 			question.set_id(q.getId());
 			question.set_rev(q.getRev());
 
 			return question;
 		} catch (IOException e) {
 			LOGGER.error("Could not save interposed question {}", question);
 		}
 
 		return null;
 	}
 
 	@Override
 	public final Question getQuestion(final String id) {
 		try {
 			View view = new View("skill_question/by_id");
 			view.setKey(URLEncoder.encode("\"" + id + "\"", "UTF-8"));
 			ViewResults results = this.getDatabase().view(view);
 
 			if (results.getJSONArray("rows").optJSONObject(0) == null) {
 				return null;
 			}
 
 			Question q = (Question) JSONObject.toBean(
 					results.getJSONArray("rows").optJSONObject(0).optJSONObject("value"),
 					Question.class
 			);
 			JSONArray possibleAnswers = results.getJSONArray("rows").optJSONObject(0).optJSONObject("value")
 					.getJSONArray("possibleAnswers");
 			Collection<PossibleAnswer> answers = JSONArray.toCollection(
 					possibleAnswers,
 					PossibleAnswer.class
 			);
 			q.setPossibleAnswers(new ArrayList<PossibleAnswer>(answers));
 			q.setSessionKeyword(this.getSessionKeyword(q.getSessionId()));
 			return q;
 		} catch (IOException e) {
 			LOGGER.error("Could not get question with id {}", id);
 		}
 		return null;
 	}
 
 	@Override
 	public final LoggedIn registerAsOnlineUser(final User user, final Session session) {
 		try {
 			View view = new View("logged_in/all");
 			view.setKey(URLEncoder.encode("\"" + user.getUsername() + "\"", "UTF-8"));
 			ViewResults results = this.getDatabase().view(view);
 
 			LoggedIn loggedIn = new LoggedIn();
 			if (results.getJSONArray("rows").optJSONObject(0) != null) {
 				JSONObject json = results.getJSONArray("rows").optJSONObject(0).optJSONObject("value");
 				loggedIn = (LoggedIn) JSONObject.toBean(json, LoggedIn.class);
 				JSONArray vs = json.optJSONArray("visitedSessions");
 				if (vs != null) {
 					Collection<VisitedSession> visitedSessions = JSONArray.toCollection(vs, VisitedSession.class);
 					loggedIn.setVisitedSessions(new ArrayList<VisitedSession>(visitedSessions));
 				}
 			}
 
 			loggedIn.setUser(user.getUsername());
 			loggedIn.setSessionId(session.get_id());
 			loggedIn.addVisitedSession(session);
 			loggedIn.updateTimestamp();
 
 			JSONObject json = JSONObject.fromObject(loggedIn);
 			Document doc = new Document(json);
 			if (doc.getId().isEmpty()) {
 				// If this is a new user without a logged_in document, we have
 				// to remove the following
 				// pre-filled fields. Otherwise, CouchDB will take these empty
 				// fields as genuine
 				// identifiers, and will throw errors afterwards.
 				doc.remove("_id");
 				doc.remove("_rev");
 			}
 			this.getDatabase().saveDocument(doc);
 
 			LoggedIn l = (LoggedIn) JSONObject.toBean(doc.getJSONObject(), LoggedIn.class);
 			JSONArray vs = doc.getJSONObject().optJSONArray("visitedSessions");
 			if (vs != null) {
 				Collection<VisitedSession> visitedSessions = JSONArray.toCollection(vs, VisitedSession.class);
 				l.setVisitedSessions(new ArrayList<VisitedSession>(visitedSessions));
 			}
 			return l;
 		} catch (UnsupportedEncodingException e) {
 			return null;
 		} catch (IOException e) {
 			return null;
 		}
 	}
 
 	@Override
 	public final void updateSessionOwnerActivity(final Session session) {
 		try {
 			session.setLastOwnerActivity(System.currentTimeMillis());
 			JSONObject json = JSONObject.fromObject(session);
 			this.getDatabase().saveDocument(new Document(json));
 		} catch (IOException e) {
 			LOGGER.error("Failed to update lastOwnerActivity for Session {}", session);
 			return;
 		}
 	}
 
 	@Override
 	public final Integer getMyFeedback(final String keyword, final User user) {
 		try {
 			String sessionId = this.getSessionId(keyword);
 			if (sessionId == null) {
 				throw new NotFoundException();
 			}
 
 			View view = new View("understanding/by_user");
 			view.setKey(
 					URLEncoder.encode(
 							"[\"" + sessionId + "\", \"" + user.getUsername() + "\"]",
 							"UTF-8"
 					)
 			);
 			ViewResults results = this.getDatabase().view(view);
 			JSONArray rows = results.getJSONArray("rows");
 
 			if (rows.size() == 0) {
 				return null;
 			}
 
 			JSONObject json = rows.optJSONObject(0).optJSONObject("value");
 			return this.feedbackValueFromString(json.getString("value"));
 		} catch (UnsupportedEncodingException e) {
 			return null;
 		}
 	}
 
 	@Override
 	public final List<String> getQuestionIds(final Session session, final User user) {
 		View view;
 		if (user.getType().equals("thm")) {
 			view = new View("skill_question/by_session_only_id_for_thm");
 		} else {
 			view = new View("skill_question/by_session_only_id_for_all");
 		}
 
 		try {
 			view.setKey(URLEncoder.encode("\"" + session.get_id() + "\"", "UTF-8"));
 			ViewResults results = this.getDatabase().view(view);
 			if (results.getResults().size() == 0) {
 				return new ArrayList<String>();
 			}
 
 			List<String> ids = new ArrayList<String>();
 			for (Document d : results.getResults()) {
 				ids.add(d.getId());
 			}
 			return ids;
 
 		} catch (IOException e) {
 			LOGGER.error("Could not get list of question ids of session {}", session.getKeyword());
 		}
 		return new ArrayList<String>();
 	}
 
 	@Override
 	public final void deleteQuestion(final Question question) {
 		try {
 			this.deleteAnswers(question);
 			this.deleteDocument(question.get_id());
 		} catch (IOException e) {
 			LOGGER.error("IOException: Could not delete question {}", question.get_id());
 		}
 	}
 
 	private void deleteDocument(final String documentId) throws IOException {
 		Document d = this.getDatabase().getDocument(documentId);
 		this.getDatabase().deleteDocument(d);
 	}
 
 	@Override
 	public final void deleteAnswers(final Question question) {
 		try {
 			View view = new View("answer/cleanup");
 			view.setKey(URLEncoder.encode("\"" + question.get_id() + "\"", "UTF-8"));
 			ViewResults results = this.getDatabase().view(view);
 	
 			for (Document d : results.getResults()) {
 				this.deleteDocument(d.getId());
 			}
 		} catch (IOException e) {
 			LOGGER.error("IOException: Could not delete answers for question {}", question.get_id());
 		}
 	}
 
 	@Override
 	public final List<String> getUnAnsweredQuestions(final Session session, final User user) {
 		try {
 			View view = new View("answer/by_user");
 			view.setKey(
 					"[" + URLEncoder.encode(
 							"\"" + user.getUsername() + "\",\"" + session.get_id() + "\"",
 							"UTF-8"
 					)
 					+ "]"
 			);
 			ViewResults anseweredQuestions = this.getDatabase().view(view);
 
 			List<String> answered = new ArrayList<String>();
 			for (Document d : anseweredQuestions.getResults()) {
 				answered.add(d.getString("value"));
 			}
 
 			List<String> questions = this.getQuestionIds(session, user);
 			List<String> unanswered = new ArrayList<String>();
 			for (String questionId : questions) {
 				if (!answered.contains(questionId)) {
 					unanswered.add(questionId);
 				}
 			}
 			return unanswered;
 		} catch (UnsupportedEncodingException e) {
 			LOGGER.error("Error while retrieving unansweredquestions", e);
 		}
 
 		return null;
 	}
 
 	@Override
 	public final Answer getMyAnswer(final String questionId, int piRound) {
 		User user = userService.getCurrentUser();
 		if (user == null) {
 			throw new UnauthorizedException();
 		}
 
 		try {
 			View view = new View("answer/by_question_and_user_and_piround");
 			if (2 == piRound) {
 				view.setKey(
 						"[" + URLEncoder.encode(
 								"\"" + questionId + "\",\"" + user.getUsername() + "\",2",
 								"UTF-8"
 						)
 						+ "]"
 				);
 			} else {
 				/* needed for legacy questions whose piRound property has not been set */
 				view.setStartKey(
 						"[" + URLEncoder.encode(
 								"\"" + questionId + "\",\"" + user.getUsername() + "\"",
 								"UTF-8"
 						)
 						+ "]"
 				);
 				view.setEndKey(
 						"[" + URLEncoder.encode(
 								"\"" + questionId + "\",\"" + user.getUsername() + "\",1",
 								"UTF-8"
 						)
 						+ "]"
 				);
 			}
 			ViewResults results = this.getDatabase().view(view);
 			if (results.getResults().isEmpty()) {
 				return null;
 			}
 			return (Answer) JSONObject.toBean(
 					results.getJSONArray("rows").optJSONObject(0).optJSONObject("value"),
 					Answer.class
 			);
 		} catch (UnsupportedEncodingException e) {
 			LOGGER.error(
 					"Error while retrieving answer for user {} and question {}, {}",
 					new Object[] {user,	questionId, e }
 			);
 		}
 
 		return null;
 	}
 
 	@Override
 	public final List<Answer> getAnswers(final String questionId, int piRound) {
 		try {
 			View view = new View("skill_question/count_answers_by_question_and_piround");
 			if (2 == piRound) {
				view.setStartKey("[" + URLEncoder.encode(
 					"\"" + questionId + "\",2",
 					"UTF-8"
 				) + "]");
				view.setEndKey("[" + URLEncoder.encode(
					"\"" + questionId + "\",2,{}",
					"UTF-8"
				) + "]");
 			} else {
 				/* needed for legacy questions whose piRound property has not been set */
 				view.setStartKey("[" + URLEncoder.encode(
 					"\"" + questionId + "\"",
 					"UTF-8"
 				) + "]");
 				view.setEndKey("[" + URLEncoder.encode(
					"\"" + questionId + "\",1,{}",
 					"UTF-8"
 				) + "]");
 			}
 			view.setGroup(true);
 			ViewResults results = this.getDatabase().view(view);
 			List<Answer> answers = new ArrayList<Answer>();
 			for (Document d : results.getResults()) {
 				Answer a = new Answer();
 				a.setAnswerCount(d.getInt("value"));
 				a.setQuestionId(d.getJSONObject().getJSONArray("key").getString(0));
 				a.setPiRound(piRound);
 				a.setAnswerText(d.getJSONObject().getJSONArray("key").getString(2));
 				answers.add(a);
 			}
 			return answers;
 		} catch (UnsupportedEncodingException e) {
 			LOGGER.error("Error while retrieving answers", e);
 		}
 		return null;
 	}
 
 	@Override
 	public final int getAnswerCount(final String questionId) {
 		try {
 			View view = new View("skill_question/count_answers_by_question");
 			view.setKey(URLEncoder.encode("\"" + questionId + "\"", "UTF-8"));
 			view.setGroup(true);
 			ViewResults results = this.getDatabase().view(view);
 			if (results.getResults().size() == 0) {
 				return 0;
 			}
 			return results.getJSONArray("rows").optJSONObject(0).optInt("value");
 		} catch (UnsupportedEncodingException e) {
 			LOGGER.error("Error while retrieving answer count", e);
 		}
 		return 0;
 	}
 
 	@Override
 	public final int countActiveUsers(final long since) {
 		try {
 			View view = new View("statistic/count_active_users");
 			view.setStartKey(String.valueOf(since));
 			ViewResults results = this.getDatabase().view(view);
 			if (isEmptyResults(results)) {
 				return 0;
 			}
 			return results.getJSONArray("rows").optJSONObject(0).getInt("value");
 		} catch (Exception e) {
 			LOGGER.error("Error while retrieving active users count", e);
 		}
 		return 0;
 	}
 
 	@Override
 	public final int countActiveUsers(Session session, long since) {
 		if (session == null) throw new NotFoundException();
 		try {
 			View view = new View("logged_in/count");
 			view.setStartKey(
 					URLEncoder.encode("[\"" + session.get_id() + "\", " + String.valueOf(since) + "]", "UTF-8")
 			);
 			view.setEndKey(URLEncoder.encode("[\"" + session.get_id() + "\", {}]", "UTF-8"));
 			ViewResults results = this.getDatabase().view(view);
 			if (isEmptyResults(results)) {
 				return 0;
 			}
 			return results.getJSONArray("rows").optJSONObject(0).getInt("value");
 		} catch (UnsupportedEncodingException e) {
 			return 0;
 		}
 	}
 
 	private boolean isEmptyResults(ViewResults results) {
 		return results == null || results.getResults().isEmpty() || results.getJSONArray("rows").size() == 0;
 	}
 
 	@Override
 	public List<Answer> getFreetextAnswers(String questionId) {
 		try {
 			View view = new View("skill_question/freetext_answers");
 			view.setKey(URLEncoder.encode("\"" + questionId + "\"", "UTF-8"));
 			ViewResults results = this.getDatabase().view(view);
 			if (results.getResults().isEmpty()) {
 				throw new NotFoundException();
 			}
 			List<Answer> answers = new ArrayList<Answer>();
 			for (Document d : results.getResults()) {
 				Answer a = (Answer) JSONObject.toBean(d.getJSONObject().getJSONObject("value"), Answer.class);
 				a.setAnswerSubject(d.getJSONObject().getJSONObject("value").getString("subject"));
 				a.setAnswerText(d.getJSONObject().getJSONObject("value").getString("text"));
 				a.setQuestionId(questionId);
 				answers.add(a);
 			}
 			return answers;
 		} catch (UnsupportedEncodingException e) {
 			LOGGER.error("Error while retrieving freetext answers", e);
 		}
 		return null;
 	}
 
 	@Override
 	public List<Answer> getMyAnswers(String sessionKey) {
 		Session s = this.getSessionFromKeyword(sessionKey);
 		if (s == null) {
 			throw new NotFoundException();
 		}
 
 		User user = userService.getCurrentUser();
 		if (user == null) {
 			throw new UnauthorizedException();
 		}
 
 		try {
 			View view = new View("answer/by_user_and_session");
 			view.setKey(
 				"[" + URLEncoder.encode("\"" + user.getUsername() + "\",\"" + s.get_id() + "\"", "UTF-8") + "]"
 			);
 			ViewResults results = this.getDatabase().view(view);
 			List<Answer> answers = new ArrayList<Answer>();
 			if (results.getResults().isEmpty()) {
 				return answers;
 			}
 			for (Document d : results.getResults()) {
 				Answer a = (Answer) JSONObject.toBean(d.getJSONObject().getJSONObject("value"), Answer.class);
 				a.set_id(d.getId());
 				a.set_rev(d.getRev());
 				a.setUser(user.getUsername());
 				a.setSessionId(s.get_id());
 				answers.add(a);
 			}
 			return answers;
 		} catch (UnsupportedEncodingException e) {
 			LOGGER.error("Error while retrieving user answers", e);
 		}
 		return null;
 	}
 
 	@Override
 	public int getTotalAnswerCount(String sessionKey) {
 		Session s = this.getSessionFromKeyword(sessionKey);
 		if (s == null) {
 			throw new NotFoundException();
 		}
 
 		try {
 			View view = new View("skill_question/count_answers_by_session");
 			view.setKey(URLEncoder.encode("\"" + s.get_id() + "\"", "UTF-8"));
 			ViewResults results = this.getDatabase().view(view);
 			if (results.getResults().size() == 0) {
 				return 0;
 			}
 			return results.getJSONArray("rows").optJSONObject(0).optInt("value");
 		} catch (UnsupportedEncodingException e) {
 			LOGGER.error("Error while retrieving total answer count", e);
 		}
 		return 0;
 	}
 
 	@Override
 	public int getInterposedCount(String sessionKey) {
 		Session s = this.getSessionFromKeyword(sessionKey);
 		if (s == null) {
 			throw new NotFoundException();
 		}
 
 		try {
 			View view = new View("interposed_question/count_by_session");
 			view.setKey(URLEncoder.encode("\"" + s.get_id() + "\"", "UTF-8"));
 			view.setGroup(true);
 			ViewResults results = this.getDatabase().view(view);
 			if (results.size() == 0 || results.getResults().size() == 0) {
 				return 0;
 			}
 			return results.getJSONArray("rows").optJSONObject(0).optInt("value");
 		} catch (UnsupportedEncodingException e) {
 			LOGGER.error("Error while retrieving interposed question count", e);
 		}
 		return 0;
 	}
 
 	@Override
 	public InterposedReadingCount getInterposedReadingCount(Session session) {
 		try {
 			View view = new View("interposed_question/count_by_session_reading");
 			view.setStartKey(URLEncoder.encode("[\"" + session.get_id() + "\"]", "UTF-8"));
 			view.setEndKey(URLEncoder.encode("[\"" + session.get_id() + "\", {}]", "UTF-8"));
 			view.setGroup(true);
 			ViewResults results = this.getDatabase().view(view);
 			if (results.size() == 0 || results.getResults().size() == 0) {
 				return new InterposedReadingCount();
 			}
 			int read = results.getJSONArray("rows").optJSONObject(0).optInt("value");
 			int unread = 0;
 			if (results.getJSONArray("rows").optJSONObject(1) != null) {
 				unread = results.getJSONArray("rows").optJSONObject(1).optInt("value");
 			}
 			return new InterposedReadingCount(read, unread);
 		} catch (UnsupportedEncodingException e) {
 			LOGGER.error("Error while retrieving interposed question count", e);
 		}
 		return new InterposedReadingCount();
 	}
 
 	@Override
 	public List<InterposedQuestion> getInterposedQuestions(String sessionKey) {
 		Session s = this.getSessionFromKeyword(sessionKey);
 		if (s == null) {
 			throw new NotFoundException();
 		}
 
 		try {
 			View view = new View("interposed_question/by_session");
 			view.setKey(URLEncoder.encode("\"" + s.get_id() + "\"", "UTF-8"));
 			ViewResults questions = this.getDatabase().view(view);
 			if (questions == null || questions.isEmpty()) {
 				return null;
 			}
 			List<InterposedQuestion> result = new ArrayList<InterposedQuestion>();
 			LOGGER.debug("{}", questions.getResults());
 			for (Document document : questions.getResults()) {
 				InterposedQuestion question = (InterposedQuestion) JSONObject.toBean(
 						document.getJSONObject().getJSONObject("value"),
 						InterposedQuestion.class
 				);
 				question.setSessionId(sessionKey);
 				question.set_id(document.getId());
 				result.add(question);
 			}
 			return result;
 		} catch (UnsupportedEncodingException e) {
 			LOGGER.error("Error while retrieving interposed questions", e);
 		}
 		return null;
 	}
 	
 	public Question getInterposedQuestion(String sessionKey, String documentId) {
 		try {
 			Document document = this.getDatabase().getDocument(documentId);
 			if (document == null) {
 				LOGGER.error("Document is NULL");
 				return null;
 			}
 			Question question = (Question) JSONObject.toBean(document.getJSONObject(), Question.class);
 			question.setQuestionType("interposed_question");
 			return question;
 		} catch (IOException e) {
 			LOGGER.error("Error while retrieving interposed question", e);
 		}
 		return null;
 	}
 
 	@Override
 	public void vote(String menu) {
 		User u = this.userService.getCurrentUser();
 		if (u == null) {
 			throw new UnauthorizedException();
 		}
 
 		String date = new SimpleDateFormat("dd-mm-yyyyy").format(new Date());
 		try {
 			View view = new View("food_vote/get_user_vote");
 			view.setKey("[" + URLEncoder.encode("\"" + date + "\",\"" + u.getUsername() + "\"", "UTF-8") + "]");
 			ViewResults results = this.getDatabase().view(view);
 
 			if (results.getResults().isEmpty()) {
 				Document vote = new Document();
 				vote.put("type", "food_vote");
 				vote.put("name", menu);
 				vote.put("user", u.getUsername());
 				vote.put("day", date);
 				this.database.saveDocument(vote);
 			} else {
 				Document vote = results.getResults().get(0);
 				vote.put("name", menu);
 				this.database.saveDocument(vote);
 			}
 		} catch (UnsupportedEncodingException e) {
 			LOGGER.error("Error while retrieving user food vote", e);
 		} catch (IOException e) {
 			LOGGER.error("Error while saving user food vote", e);
 		}
 	}
 
 	@Override
 	public List<FoodVote> getFoodVote() {
 		List<FoodVote> foodVotes = new ArrayList<FoodVote>();
 		String date = new SimpleDateFormat("dd-mm-yyyyy").format(new Date());
 		try {
 			View view = new View("food_vote/count_by_day");
 			view.setStartKey("[" + URLEncoder.encode("\"" + date + "\"", "UTF-8") + "]");
 			view.setEndKey("[" + URLEncoder.encode("\"" + date + "\",{}", "UTF-8") + "]");
 			view.setGroup(true);
 			ViewResults results = this.getDatabase().view(view);
 			for (Document d : results.getResults()) {
 				FoodVote vote = new FoodVote();
 				vote.setCount(d.getJSONObject().optInt("value"));
 				vote.setDay(date);
 				vote.setName(d.getJSONObject().getJSONArray("key").getString(1));
 				foodVotes.add(vote);
 			}
 
 			return foodVotes;
 		} catch (UnsupportedEncodingException e) {
 			LOGGER.error("Error while retrieving food vote count", e);
 		}
 		return foodVotes;
 	}
 
 	@Override
 	public int getFoodVoteCount() {
 		String date = new SimpleDateFormat("dd-mm-yyyyy").format(new Date());
 		try {
 			View view = new View("food_vote/count_by_day");
 			view.setStartKey("[" + URLEncoder.encode("\"" + date + "\"", "UTF-8") + "]");
 			view.setEndKey("[" + URLEncoder.encode("\"" + date + "\",{}", "UTF-8") + "]");
 			view.setGroup(false);
 			ViewResults results = this.getDatabase().view(view);
 			if (results.size() == 0 || results.getResults().size() == 0) {
 				return 0;
 			}
 			return results.getJSONArray("rows").optJSONObject(0).optInt("value");
 		} catch (UnsupportedEncodingException e) {
 			LOGGER.error("Error while retrieving food vote count", e);
 		}
 		return 0;
 	}
 
 	@Override
 	public int countSessions() {
 		return sessionsCountValue("openSessions")
 				+ sessionsCountValue("closedSessions");
 	}
 
 	@Override
 	public int countClosedSessions() {
 		return sessionsCountValue("closedSessions");
 	}
 
 	@Override
 	public int countOpenSessions() {
 		return sessionsCountValue("openSessions");
 	}
 
 	@Override
 	public int countAnswers() {
 		return sessionsCountValue("answers");
 	}
 
 	@Override
 	public int countQuestions() {
 		return sessionsCountValue("questions");
 	}
 
 	private int sessionsCountValue(String key) {
 		try {
 			View view = new View("statistic/count_sessions");
 			view.setGroup(true);
 
 			ViewResults results = this.getDatabase().view(view);
 			if (isEmptyResults(results)) {
 				return 0;
 			}
 
 			int result = 0;
 			JSONArray rows = results.getJSONArray("rows");
 
 			for (int i = 0; i < rows.size(); i++) {
 				JSONObject row = rows.getJSONObject(i);
 				if (
 					row.getString("key").equals(key)
 				) {
 					result += row.getInt("value");
 				}
 			}
 			return result;
 		} catch (Exception e) {
 			LOGGER.error("Error while retrieving session count", e);
 		}
 		return 0;
 	}
 
 	@Override
 	public InterposedQuestion getInterposedQuestion(String questionId) {
 		try {
 			Document document = this.getDatabase().getDocument(questionId);
 			InterposedQuestion question = (InterposedQuestion) JSONObject.toBean(document.getJSONObject(),
 					InterposedQuestion.class);
 			question.setSessionId(getSessionKeyword(question.getSessionId()));
 			return question;
 		} catch (IOException e) {
 			LOGGER.error("Could not load interposed question {}", questionId);
 		}
 		return null;
 	}
 
 	@Override
 	public void markInterposedQuestionAsRead(InterposedQuestion question) {
 		try {
 			question.setRead(true);
 			Document document = this.getDatabase().getDocument(question.get_id());
 			document.put("read", question.isRead());
 			this.getDatabase().saveDocument(document);
 		} catch (IOException e) {
 			LOGGER.error("Coulg not mark interposed question as read {}", question.get_id());
 		}
 	}
 
 	@Override
 	public List<Session> getMyVisitedSessions(User user) {
 		try {
 			View view = new View("logged_in/visited_sessions_by_user");
 			view.setKey(URLEncoder.encode("\"" + user.getUsername() + "\"", "UTF-8"));
 			ViewResults sessions = this.getDatabase().view(view);
 			List<Session> allSessions = new ArrayList<Session>();
 			for (Document d : sessions.getResults()) {
 				// Not all users have visited sessions
 				if (d.getJSONObject().optJSONArray("value") != null) {
 					@SuppressWarnings("unchecked")
 					Collection<Session> visitedSessions =  JSONArray.toCollection(
 						d.getJSONObject().getJSONArray("value"),
 						Session.class
 					);
 					allSessions.addAll(visitedSessions);
 				}
 			}
 			// Do these sessions still exist?
 			List<Session> result = new ArrayList<Session>();
 			for (Session s : allSessions) {
 				Session session = this.getSessionFromKeyword(s.getKeyword());
 				if (session != null) {
 					result.add(session);
 				}
 			}
 			return result;
 		} catch (UnsupportedEncodingException e) {
 			return null;
 		}
 	}
 
 	@Override
 	public Answer saveAnswer(Answer answer, User user) {
 		try {
 			Document a = new Document();
 			a.put("type", "skill_question_answer");
 			a.put("sessionId", answer.getSessionId());
 			a.put("questionId", answer.getQuestionId());
 			a.put("answerSubject", answer.getAnswerSubject());
 			a.put("answerText", answer.getAnswerText());
 			a.put("timestamp", answer.getTimestamp());
 			a.put("user", user.getUsername());
 			a.put("piRound", answer.getPiRound());
 			this.database.saveDocument(a);
 			answer.set_id(a.getId());
 			answer.set_rev(a.getRev());
 			return answer;
 		} catch (IOException e) {
 			LOGGER.error("Could not save answer {}", answer);
 		}
 		return null;
 	}
 
 	@Override
 	public Answer updateAnswer(Answer answer) {
 		try {
 			Document a = this.database.getDocument(answer.get_id());
 			a.put("answerSubject", answer.getAnswerSubject());
 			a.put("answerText", answer.getAnswerText());
 			a.put("timestamp", answer.getTimestamp());
 			this.database.saveDocument(a);
 			answer.set_rev(a.getRev());
 			return answer;
 		} catch (IOException e) {
 			LOGGER.error("Could not save answer {}", answer);
 		}
 		return null;
 	}
 
 	@Override
 	public void deleteAnswer(String answerId) {
 		try {
 			this.database.deleteDocument(this.database.getDocument(answerId));
 		} catch (IOException e) {
 			LOGGER.error("Could not delete answer {} because of {}", answerId, e.getMessage());
 		}
 	}
 
 	@Override
 	public void deleteInterposedQuestion(InterposedQuestion question) {
 		try {
 			this.deleteDocument(question.get_id());
 		} catch (IOException e) {
 			LOGGER.error("Could not delete interposed question {} because of {}", question.get_id(), e.getMessage());
 		}
 	}
 	
 	@Override
 	public List<Session> getCourseSessions(List<Course> courses) {
 		ExtendedView view = new ExtendedView("logged_in/available_moodlesessions");
 		view.setCourseIdKeys(courses);
 		
 		ViewResults sessions = this.getDatabase().view(view);
 
 		List<Session> result = new ArrayList<Session>();
 		for (Document d : sessions.getResults()) {
 			Session session = (Session) JSONObject.toBean(
 					d.getJSONObject().getJSONObject("value"),
 					Session.class
 			);
 			result.add(session);
 		}
 		return result;
 	}
 
 	@Override
 	public final List<String> getActiveUsers(int timeDifference) {
 		try {
 			long inactiveBeforeTimestamp = new Date().getTime() - timeDifference * 1000;
 
 			View view = new View("logged_in/by_and_only_timestamp_and_username");
 			view.setStartKey("[" + URLEncoder.encode(String.valueOf(inactiveBeforeTimestamp), "UTF-8") + "]");
 			ViewResults results = this.getDatabase().view(view);
 			LOGGER.debug("getActiveUsers result count: {}", String.valueOf(results.size()));
 
 			List<String> result = new ArrayList<String>();
 			for (Document d : results.getResults()) {
 				result.add(d.getJSONObject().getJSONArray("key").getString(1));
 			}
 
 			return result;
 		} catch (UnsupportedEncodingException e) {
 			LOGGER.error("Error while retrieving active users", e);
 		}
 		return null;
 	}
 
 	private class ExtendedView extends View {
 
 		private String keys;
 		
 		public ExtendedView(String fullname) {
 			super(fullname);
 		}
 		
 		public void setKeys(String newKeys) {
 			this.keys = newKeys;
 		}
 		
 		public void setCourseIdKeys(List<Course> courses) {
 			if (courses.isEmpty()) {
 				this.keys = "[]";
 				return;
 			}
 			
 			StringBuilder sb = new StringBuilder();
 			sb.append("[");
 			for (int i = 0; i < courses.size() - 1; i++) {
 				sb.append("\"" + courses.get(i).getId() + "\",");
 			}
 			sb.append("\"" + courses.get(courses.size() - 1).getId() + "\"");
 			sb.append("]");
 			try {
 				this.setKeys(URLEncoder.encode(sb.toString(), "UTF-8"));
 			}
 			catch (UnsupportedEncodingException e) {
 				LOGGER.error("Error while encoding course ID keys", e);
 			}
 		}
 		
 		public String getQueryString() {
 			StringBuilder query = new StringBuilder();
 			if (super.getQueryString() != null) {
 				query.append(super.getQueryString());
 			}
 			if (this.keys != null) {
 				if (query.toString().isEmpty()) {
 					query.append("&");
 				}
 
 				query.append("keys=" + this.keys);
 			}
 			
 			if (query.toString().isEmpty()) return null;
 			return query.toString();
 		}
 	}
 
 	@Override
 	public Session lockSession(Session session, Boolean lock) {
 		try {
 			Document s = this.database.getDocument(session.get_id());
 			s.put("active", lock);
 			this.database.saveDocument(s);
 			session.set_rev(s.getRev());
 			return session;
 		} catch (IOException e) {
 			LOGGER.error("Could not lock session {}", session);
 		}
 		return null;
 	}
 }
