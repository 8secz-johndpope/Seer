 package org.motechproject.mobile.omp.manager.intellivr;
 
 import java.io.BufferedReader;
 import java.io.File;
 import java.io.FileInputStream;
 import java.io.FileNotFoundException;
 import java.io.FileOutputStream;
 import java.io.FileReader;
 import java.io.IOException;
 import java.io.ObjectInputStream;
 import java.io.ObjectOutputStream;
 import java.util.ArrayList;
 import java.util.Collection;
 import java.util.Date;
 import java.util.GregorianCalendar;
 import java.util.HashMap;
 import java.util.List;
 import java.util.Map;
 import java.util.Set;
 import java.util.Timer;
 import java.util.TimerTask;
 import java.util.regex.Matcher;
 import java.util.regex.Pattern;
 
 import org.apache.commons.lang.ObjectUtils;
 import org.apache.commons.logging.Log;
 import org.apache.commons.logging.LogFactory;
 import org.motechproject.mobile.core.dao.GatewayRequestDAO;
 import org.motechproject.mobile.core.dao.MessageRequestDAO;
 import org.motechproject.mobile.core.manager.CoreManager;
 import org.motechproject.mobile.core.model.GatewayRequest;
 import org.motechproject.mobile.core.model.GatewayRequestImpl;
 import org.motechproject.mobile.core.model.GatewayResponse;
 import org.motechproject.mobile.core.model.Language;
 import org.motechproject.mobile.core.model.MStatus;
 import org.motechproject.mobile.core.model.MessageRequest;
 import org.motechproject.mobile.core.model.MessageType;
 import org.motechproject.mobile.core.model.NotificationType;
 import org.motechproject.mobile.core.service.MotechContext;
 import org.motechproject.mobile.omp.manager.GatewayManager;
 import org.motechproject.mobile.omp.manager.GatewayMessageHandler;
 import org.motechproject.mobile.omp.manager.utils.MessageStatusStore;
 import org.motechproject.ws.server.RegistrarService;
 import org.motechproject.ws.server.ValidationException;
 import org.springframework.core.io.Resource;
 import org.springframework.transaction.annotation.Transactional;
 
 @Transactional
 public class IntellIVRBean implements GatewayManager, GetIVRConfigRequestHandler, ReportHandler {
 
 	private GatewayMessageHandler messageHandler;
 	protected String reportURL;
 	private String apiID;
 	private String method;
 	private String defaultLanguage;
 	private String defaultTree;
 	private String defaultReminder;
 	private IntellIVRServer ivrServer;
 	private MessageStatusStore statusStore;
 	protected Map<Long, IVRNotificationMapping> ivrNotificationMap;
 	protected Map<String, Long> ivrReminderIds;
 	protected Map<String, IVRSession> ivrSessions;
 	private Timer timer;
 	private long bundlingDelay;
 	private int retryDelay;
 	private int maxAttempts;
 	private int maxDays;
 	private int availableDays;
 	private int callCompletedThreshold;
 	private int preReminderDelay;
 	private boolean accelerateRetries;
 	private String noPendingMessagesRecordingName;
 	private String welcomeMessageRecordingName;
 	private Resource mappingResource;
 	private Resource ivrSessionSerialResource;
 	private CoreManager coreManager;
 	private RegistrarService registrarService;
 
 	private Log log = LogFactory.getLog(IntellIVRBean.class);
 	private Log reportLog = LogFactory.getLog(IntellIVRBean.class.getName() + ".reportlog");
 	private Log callLog = LogFactory.getLog(IntellIVRBean.class.getName() + ".calllog");
 
 	@SuppressWarnings("unused")
 	private void init() {
 
 		ivrNotificationMap = new HashMap<Long, IVRNotificationMapping>();
 		ivrReminderIds = new HashMap<String, Long>();
 
 		try {
 
 			File f = mappingResource.getFile();
 
 			log.debug("Looking for Notification to IVR entity mappings in " + f.getName());
 
 			BufferedReader br = new BufferedReader(new FileReader(f));
 
 			Pattern p = Pattern.compile("([0-9]+)=([IiRr]{1}),(.+)");
 			Matcher m;
 
 			String line = "";
 
 			while ( (line = br.readLine()) != null ) {
 
 				m = p.matcher(line);
 
 				if ( m.matches() ) {
 
 					long mapID = Long.parseLong(m.group(1));
 					String ivrType = m.group(2).toUpperCase();
 					String ivrEntity = m.group(3);
 
 					log.debug("Found IVR entity mapping: " + mapID + " => " + ivrType + "," + ivrEntity);
 
 					IVRNotificationMapping i = new IVRNotificationMapping();
 					i.setId(mapID);
 					i.setType(ivrType);
 					i.setIvrEntityName(ivrEntity);
 					ivrNotificationMap.put(mapID, i);
 
 					ivrReminderIds.put(ivrEntity, mapID);
 
 				}
 
 			}
 
 		} catch (IOException e) {
 			log.error("IOException creating IVR to Notification Map - default tree and message will be used");
 		}
 
 		timer = new Timer();
 
 		ivrSessions = loadIvrSessions();
 
 		if ( ivrSessions == null )
 			ivrSessions = new HashMap<String, IVRSession>();
 
 
 		if ( bundlingDelay >= 0 ) {
 			synchronized (ivrSessions) {
 				for ( IVRSession session : ivrSessions.values() ) {
 					if ( session.getState() == IVRSession.OPEN
 							|| session.getState() == IVRSession.SEND_WAIT ) {
 						IVRServerTimerTask task = new IVRServerTimerTask(session);
 						timer.schedule(task, bundlingDelay);
 					}
 				}
 			}
 		}
 
 		if ( accelerateRetries ) {
 			log.warn("Using accelerated retries.  Configured retry intervals will be ignored.");
 			retryDelay = 1;
 		}
 
 	}
 
 	public void cleanUp() {
 
 		saveIvrSessions();
 
 	}
 
 	@SuppressWarnings("unchecked")
 	protected Map<String, IVRSession> loadIvrSessions() {
 
 		Map<String, IVRSession> loadedSessions = null;
 
 		if ( ivrSessionSerialResource != null ) {
 
 			ObjectInputStream objIn = null;
 
 			try {
 
 				objIn = new ObjectInputStream(new FileInputStream(ivrSessionSerialResource.getFile()));
 
 				loadedSessions = (Map<String, IVRSession>)objIn.readObject();
 
 				for ( IVRSession s : loadedSessions.values() ) {
 					log.info("Loaded existing session " + s.getSessionId());
 				}
 
 				return loadedSessions;
 
 			} catch (IOException e) {
 				log.error("Cached IVRSessions not loaded due to following error: " + e.getMessage());
 			} catch (ClassNotFoundException e) {
 				log.error("Cached IVRSessions not loaded due to following error: " + e.getMessage());
 			} finally {
 				if ( objIn != null )
 					try {
 						objIn.close();
 					} catch (IOException e) {
 					}
 			}
 
 		}
 
 		return loadedSessions;
 
 	}
 
 	protected void saveIvrSessions() {
 
 		if ( ivrSessionSerialResource != null ) {
 
 			synchronized (ivrSessions) {
 
 				for ( IVRSession s : ivrSessions.values())
 					log.info("Serializing IVRSession " + s.getSessionId());
 
 				ObjectOutputStream objOut = null;
 
 				try {
 
 					objOut = new ObjectOutputStream(new FileOutputStream(ivrSessionSerialResource.getFile()));
 
 					objOut.writeObject(ivrSessions);
 
 				} catch (FileNotFoundException e) {
 					log.error("Cached IVRSessions not serialized due to following error: " + e.getMessage());
 				} catch (IOException e) {
 					log.error("Cached IVRSessions not serialized due to following error: " + e.getMessage());
 				} finally {
 					if ( objOut != null )
 						try {
 							objOut.close();
 						} catch (IOException e) {
 						}
 				}
 
 			}
 
 		}
 
 	}
 
 	public String getMessageStatus(GatewayResponse response) {
 		log.debug("Returning " + statusStore.getStatus(response.getGatewayMessageId()) + " for " + response.getId());
 		return statusStore.getStatus(response.getGatewayMessageId());
 	}
 
 	public MStatus mapMessageStatus(GatewayResponse response) {
 		log.debug("Returning " + messageHandler.lookupStatus(response.getResponseText()) + " for " + response.getId());
 		return messageHandler.lookupStatus(response.getResponseText());
 	}
 
 	/**
 	 * Method for the core mobile server to request a message be delivered to a user of the IVR system.
 	 * 
 	 * Messages may take as long the {@link #getBundlingDelay()} to be sent after a call to this method.  
 	 * 
 	 * When a request is received for a user at a particular phone number, the bean will wait up to the bundling
 	 * delay for other messages for that user at that phone number before triggering a call from the IVR system.  This 
 	 * is to compensate for the lack of message bundling in the underlying system.
 	 * 
 	 */
 	@SuppressWarnings("unchecked")
 	public Set<GatewayResponse> sendMessage(GatewayRequest gatewayRequest) {
 
 		log.debug("Received GatewayRequest:" + gatewayRequest);
 
 		initializeGatewayRequest(gatewayRequest);
 
 		IVRServerTimerTask task = null;
 
 		String recipientID = gatewayRequest
 			.getMessageRequest()
 			.getRecipientId();
 
 		String phone = gatewayRequest
 			.getRecipientsNumber();
 
 		Language language = gatewayRequest
 			.getMessageRequest().getLanguage();
 
 		String status = StatusType.OK.value();
 		if ( recipientID == null || gatewayRequest.getMessageRequest().getMessageType() == MessageType.TEXT ) {
 			status = StatusType.ERROR.value();
 		} else {
 
 			if ( gatewayRequest.getMessageRequest().getDateFrom() == null )
 				gatewayRequest.getMessageRequest().setDateFrom(new Date());
 
 			if ( gatewayRequest.getMessageRequest().getDateTo() == null ) {
 
 				GregorianCalendar endTime = new GregorianCalendar();
 				endTime.setTime(gatewayRequest.getMessageRequest().getDateFrom());
 				endTime.add(GregorianCalendar.DAY_OF_MONTH, availableDays);
 
 				gatewayRequest.getMessageRequest().setDateTo(endTime.getTime());
 
 			}
 
 			String phoneType = gatewayRequest.getMessageRequest().getPhoneNumberType();
 
 			if ( phoneType.equalsIgnoreCase("PERSONAL") || phoneType.equalsIgnoreCase("HOUSEHOLD") ) {
 
 				synchronized (ivrSessions) {
 
 					IVRSession session = null;
 
 					for ( IVRSession possibleSession : ivrSessions.values() ) {
 						if ( !possibleSession.isUserInitiated()
 								&& possibleSession.getUserId().equalsIgnoreCase(recipientID)
 								&& ObjectUtils.equals(possibleSession.getPhone(), phone)
 								&& possibleSession.getLanguage().equalsIgnoreCase(language.getName())
 								&& possibleSession.getAttempts() == 0
 								&& possibleSession.getDays() == gatewayRequest.getMessageRequest().getDaysAttempted()
 								&& possibleSession.getState() == IVRSession.OPEN) {
 							session = possibleSession;
 						}
 					}
 
 					if ( session == null ) {
 						log.debug("Creating new IVR Session for " + recipientID + "@" + phone);
 						session = new IVRSession(recipientID,
 								phone,
 								language.getName(),
 								gatewayRequest.getMessageRequest().getDaysAttempted());
 						session.addGatewayRequest(gatewayRequest);
 
 						ivrSessions.put(session.getSessionId(), session);
 
 						task = new IVRServerTimerTask(session);
 
 					} else {
 						log.debug("Using existing IVR Session for " + recipientID + "@" + phone);
 						session.addGatewayRequest(gatewayRequest);
 					}
 
 				}
 
 			} else {
 				log.debug("GatewayRequest " + gatewayRequest.getId() + " has phone type " +
 						gatewayRequest.getMessageRequest().getPhoneNumberType() +
 						".  Call will not be made and message will remain pending.");
 			}
 
 
 		}
 
 		Set<GatewayResponse> responses = messageHandler
 			.parseMessageResponse(gatewayRequest, status);
 
 		for ( GatewayResponse response : responses )
 			statusStore.updateStatus(response.getGatewayMessageId(),
 					response.getResponseText());
 
 		if ( task != null && bundlingDelay >= 0 )
 			timer.schedule(task, bundlingDelay);
 
 		return responses;
 	}
 
 	private void initializeGatewayRequest(GatewayRequest request) {
 		request.getId();
 		request.getRecipientsNumber();
 		request.getDateFrom();
 		request.getDateTo();
 		request.getMessageRequest().getDateFrom();
 		request.getMessageRequest().getDateTo();
 		request.getMessageRequest().getLanguage().getName();
 		request.getMessageRequest().getRecipientId();
 		request.getMessageRequest().getNotificationType().getId();
 		request.getMessageRequest().getDaysAttempted();
 	}
 
 	/**
 	 * Send request to the IVR server to initiate a call 
 	 * @param sessionId
 	 */
 	public void sendPending(String sessionId) {
 
 		IVRSession session = null;
 
 		synchronized (ivrSessions) {
 			session = ivrSessions.get(sessionId);
 			if ( session != null ) {
 				session.setState(IVRSession.SEND_WAIT);
 				session.setAttempts(session.getAttempts() + 1);
 			}
 		}
 
 		if ( session != null ) {
 
 			RequestType request = createIVRRequest(session);
 
 			log.debug("Created IVR Request: " + request);
 
 			ResponseType response = ivrServer.requestCall(request);
 
 			log.debug("Received response from IVR Server: " + response);
 
 			String status = response.getStatus() == StatusType.OK ? StatusType.OK.value() : response.getErrorCode().value();
 
 			for (GatewayRequest gatewayRequest : session.getGatewayRequests())
 				statusStore.updateStatus(gatewayRequest.getMessageRequest().getId().toString(), status);
 
 			if ( response.getStatus() == StatusType.ERROR )
 				ivrSessions.remove(session.getSessionId());
 			else
 				session.setState(IVRSession.REPORT_WAIT);
 
 			StringBuilder requestIds = new StringBuilder();
 			StringBuilder notificationIDs = new StringBuilder();
 			boolean firstRequest = true;
 			
 			for ( GatewayRequest gwRequest : session.getGatewayRequests() ) {
 				if ( firstRequest )
 					firstRequest = false;
 				else {
 					requestIds.append("|");
 					notificationIDs.append("|");
 				}
				requestIds.append(gwRequest.getId());
 				notificationIDs.append(gwRequest.getMessageRequest().getNotificationType().getId().toString());
 			}
 			
 			StringBuilder reminders = new StringBuilder();
 			boolean firstReminder = true;
 			
 			for ( Object o : request.getVxml().getPrompt().getAudioOrBreak() ) {
 				if ( o instanceof AudioType ) {
 					if ( firstReminder )
 						firstReminder = false;
 					else
 						reminders.append("|");
 					reminders.append(((AudioType)o).getSrc());
 				}
 			}
 			
 			callLog.info("OUT," +
 					session.getPhone() + "," +
 					session.getUserId() + "," +
 					status + "," +
 					session.getSessionId() + "," +
 					requestIds.toString() + "," +
 					notificationIDs.toString() + "," +
 					request.getTree() + "," + 
 					reminders.toString());
 		}
 
 	}
 
 	/**
 	 * A non-thread safe way to trigger a call on the IVR system.
 	 * @deprecated
 	 * @param session
 	 */
 	public void sendPending(IVRSession session) {
 
 		session.setAttempts(session.getAttempts() + 1);
 
 		RequestType request = createIVRRequest(session);
 
 		log.debug("Created IVR Request: " + request);
 
 		ResponseType response = ivrServer.requestCall(request);
 
 		log.debug("Received response from IVR Server: " + response);
 
 		String status = response.getStatus() == StatusType.OK ? StatusType.OK.value() : response.getErrorCode().value();
 
 		for (GatewayRequest gatewayRequest : session.getGatewayRequests())
 			statusStore.updateStatus(gatewayRequest.getMessageRequest().getId().toString(), status);
 
 		if ( response.getStatus() == StatusType.ERROR )
 			ivrSessions.remove(session.getSessionId());
 		else
 			session.setState(IVRSession.REPORT_WAIT);
 
 		callLog.info("OUT," +
 					 session.getPhone() + "," +
 					 session.getUserId() + "," +
 					 status + "," +
 					 session.getSessionId());
 
 	}
 
 	public RequestType createIVRRequest(IVRSession session) {
 
 		Collection<GatewayRequest> gwRequests = session.getGatewayRequests();
 
 		log.debug("Creating IVR Request for " + gwRequests);
 
 		RequestType ivrRequest = new RequestType();
 
 		/*
 		 * These first three values are fixed
 		 */
 		ivrRequest.setApiId(apiID);
 		ivrRequest.setMethod(method);
 		ivrRequest.setReportUrl(reportURL);
 
 		/*
 		 * recipient's phone number
 		 */
 		ivrRequest.setCallee(session.getPhone());
 
 		/*
 		 * Set language
 		 */
 		String language = session.getLanguage();
 		ivrRequest.setLanguage(language != null ? language : defaultLanguage);
 
 		/*
 		 * Private id
 		 */
 		ivrRequest.setPrivate(session.getSessionId());
 
 		/*
 		 * Create the content
 		 */
 		GatewayRequest infoRequest = null;
 		List<String> reminderMessages = new ArrayList<String>();
 		for (GatewayRequest gatewayRequest : gwRequests) {
 
 			long notificationId = gatewayRequest.getMessageRequest().getNotificationType().getId();
 
 			if ( !ivrNotificationMap.containsKey(notificationId) ) {
 				log.debug("No IVR Notification mapping found for " + notificationId);
 			} else {
 
 				IVRNotificationMapping mapping = ivrNotificationMap.get(notificationId);
 
 				if ( mapping.getType().equalsIgnoreCase(IVRNotificationMapping.INFORMATIONAL)) {
 					if ( infoRequest == null )
 						infoRequest = gatewayRequest;
 					else {
 						GregorianCalendar currDateFrom = new GregorianCalendar();
 						currDateFrom.setTime(infoRequest.getMessageRequest().getDateFrom());
 						GregorianCalendar possibleDateFrom = new GregorianCalendar();
 						possibleDateFrom.setTime(gatewayRequest.getMessageRequest().getDateFrom());
 						if ( currDateFrom.before(possibleDateFrom) )
 							infoRequest = gatewayRequest;
 					}
 
 				} else {
 					reminderMessages.add(mapping.getIvrEntityName());
 				}
 
 			}
 
 		}
 
 		if ( infoRequest != null ) {
 			IVRNotificationMapping infoMapping = ivrNotificationMap
 			.get(infoRequest
 					.getMessageRequest()
 					.getNotificationType()
 					.getId());
 			ivrRequest.setTree(infoMapping.getIvrEntityName());
 		}
 
 		RequestType.Vxml vxml = new RequestType.Vxml();
 		vxml.setPrompt(new RequestType.Vxml.Prompt());
 
 		if ( preReminderDelay > 0 ) {
 			BreakType breakType = new BreakType();
 			breakType.setTime(Integer.toString(preReminderDelay) + "s");
 			vxml.getPrompt()
 				.getAudioOrBreak()
 				.add(breakType);
 		}
 
 		if ( !session.isUserInitiated() 
 				&& welcomeMessageRecordingName != null
 				&& welcomeMessageRecordingName.trim().length() > 0 ) {
 			AudioType welcome = new AudioType();
 			welcome.setSrc(welcomeMessageRecordingName);
 			vxml.getPrompt()
 				.getAudioOrBreak()
 				.add(welcome);
 		}
 
 		for (String fileName : reminderMessages) {
 			AudioType audio = new AudioType();
 			audio.setSrc(fileName);
 			vxml.getPrompt()
 				.getAudioOrBreak()
 				.add(audio);
 		}
 		ivrRequest.setVxml(vxml);
 
 		return ivrRequest;
 
 	}
 
 
 	@SuppressWarnings("unchecked")
 	public ResponseType handleRequest(GetIVRConfigRequest request) {
 
 
 		ResponseType r = new ResponseType();
 		String userId = request.getUserid();
 		MotechContext context = null;
 		
 		log.info("Received ivr config request for id " + userId);
 
 		try {
 
 			
 			
 			String[] enrollments = registrarService.getPatientEnrollments(Integer.parseInt(userId));
 
 			if ( enrollments == null || enrollments.length == 0 ) {
 				callLog.info("IN,," + request.getUserid() + ",UNENROLLED");
 				r.setErrorCode(ErrorCodeType.MOTECH_INVALID_USER_ID);
 				r.setErrorString("Unenrolled user");
 				r.setStatus(StatusType.ERROR);
 			} else {
 
 				
 				MessageRequestDAO<MessageRequest> mrDAO = coreManager.createMessageRequestDAO();
 
 				List<MessageRequest> pendingMessageRequests = mrDAO.getMsgRequestByRecipientAndSchedule(request.getUserid(), new Date());
 
 				IVRSession session = new IVRSession(userId);
 
 				if ( pendingMessageRequests.size() == 0 ) {
 					log.debug("No pending messages found for " + request.getUserid());
 					callLog.info("IN,," + request.getUserid() + ",NO_PENDING");
 					r.setStatus(StatusType.OK);
 					RequestType.Vxml vxml = new RequestType.Vxml();
 					vxml.setPrompt(new RequestType.Vxml.Prompt());
 					AudioType a = new AudioType();
 					a.setSrc(noPendingMessagesRecordingName.trim());
 					vxml.getPrompt().getAudioOrBreak().add(a);
 					r.setVxml(vxml);
 					r.setReportUrl(reportURL);
 					r.setPrivate(session.getSessionId());
 				} else {
 
 					log.debug("Found pending messages for " + request.getUserid() + ": " + pendingMessageRequests);
 
 					for (MessageRequest messageRequest : pendingMessageRequests ) {
 
 						GatewayRequest gwr = new GatewayRequestImpl();
 						gwr.setMessageRequest(messageRequest);
 
 						session.addGatewayRequest(gwr);
 
 						statusStore.updateStatus(messageRequest.getId().toString(), StatusType.OK.value());
 
 					}
 
 					/*
 					 * ResponseType fields are a subset of the RequestType fields
 					 * Can create a RequestType based on this criteria and use
 					 * only the fields that are needed to create the ResponseType
 					 */
 					RequestType requestType = createIVRRequest(session);
 
 					r.setPrivate(requestType.getPrivate());
 					r.setReportUrl(requestType.getReportUrl());
 					r.setStatus(StatusType.OK);
 					r.setTree(requestType.getTree());
 					r.setVxml(requestType.getVxml());
 
 					StringBuilder notificationIDs = new StringBuilder();
 					boolean firstRequest = true;
 					
 					for ( GatewayRequest gwRequest : session.getGatewayRequests() ) {
 						if ( firstRequest )
 							firstRequest = false;
 						else 
 							notificationIDs.append("|");
 						notificationIDs.append(gwRequest.getMessageRequest().getNotificationType().getId().toString());
 					}
 					
 					StringBuilder reminders = new StringBuilder();
 					boolean firstReminder = true;
 					
 					for ( Object o : r.getVxml().getPrompt().getAudioOrBreak() ) {
 						if ( o instanceof AudioType ) {
 							if ( firstReminder )
 								firstReminder = false;
 							else
 								reminders.append("|");
 							reminders.append(((AudioType)o).getSrc());
 						}
 					}
 					
 					callLog.info("IN,," +
 									request.getUserid() + "," +
 									StatusType.OK.value() + "," +
 									session.getSessionId()+ ",," +
 									notificationIDs.toString() + "," +
 									r.getTree() + "," + 
 									reminders.toString());
 
 				}
 
 				ivrSessions.put(session.getSessionId(), session);
 
 			}
 
 		} catch (NumberFormatException e) {
 			log.error("Invalid user id: id must be numeric");
 			callLog.info("IN,," + request.getUserid() + "," + ErrorCodeType.MOTECH_INVALID_USER_ID.name());
 			r.setErrorCode(ErrorCodeType.MOTECH_INVALID_USER_ID);
 			r.setErrorString("Invalid user id: id must be numeric");
 			r.setStatus(StatusType.ERROR);
 		} catch (ValidationException e) {
 			log.error("Invalid user id: no such id '" + userId + "' on server");
 			callLog.info("IN,," + request.getUserid() + "," + ErrorCodeType.MOTECH_INVALID_USER_ID.name());
 			r.setErrorCode(ErrorCodeType.MOTECH_INVALID_USER_ID);
 			r.setErrorString("Invalid user id: no such id '" + userId + "' on server");
 			r.setStatus(StatusType.ERROR);
 		} finally {
 			if ( context != null )
 				context.cleanUp();
 		}
 
 		return r;
 	}
 
 	@SuppressWarnings("unchecked")
 	public ResponseType handleReport(ReportType report) {
 		log.info("Received call report: " + report.toString());
 
 		List<String> messages = formatReportLogMessages(report);
 		for ( String message : messages )
 			reportLog.info(message);
 
 		String sessionId = report.getPrivate();
 
 		if ( sessionId == null )
 			log.error("Unable to identify call in report: " + report.toString());
 		else {
 			IVRSession session = ivrSessions.get(sessionId);
 			if ( session == null ) {
 				log.error("Unable to find IVRSession for " + sessionId);
 			} else {
 
 				String status = report.getStatus().value();
 
 				/*
 				 * Retry if necessary
 				 */
 				if ( report.getStatus() == ReportStatusType.COMPLETED && callExceedsThreshold(report) ) {
 					ivrSessions.remove(sessionId);
 				} else {
 
 					if ( session.isUserInitiated() ) {
 						status = null;
 						ivrSessions.remove(sessionId);
 					} else {
 
 						if ( report.getStatus() == ReportStatusType.COMPLETED )
 							status = "BELOWTHRESHOLD";
 
 						if ( session.getAttempts() < this.maxAttempts ) {
 							if ( retryDelay >=  0 ) {
 								log.info("Retrying IVRSession " + session.getSessionId() + " in " + retryDelay + " minutes. (" + session.getAttempts() + " of " + maxAttempts + ")");
 								IVRServerTimerTask task = new IVRServerTimerTask(ivrSessions.get(sessionId));
 								timer.schedule(task, 1000 * 60 * retryDelay);
 							}
 						} else {
 
 							//all attempts for this day have been exhausted - increment days attempted
 							session.setDays(session.getDays() + 1);
 
 						
 
 							if ( session.getDays() < this.maxDays ) {
 
 								GatewayRequestDAO<GatewayRequest> gwReqDAO = coreManager.createGatewayRequestDAO();
 
 								for ( GatewayRequest gatewayRequest : session.getGatewayRequests() ) {
 
 									GatewayRequest gatewayRequestDB = gwReqDAO.getById(gatewayRequest.getId());
 
 									Date dateFrom = gatewayRequestDB.getDateFrom();
 									Date dateTo = gatewayRequestDB.getDateTo();
 
 									GregorianCalendar newDateFrom = new GregorianCalendar();
 									newDateFrom.setTime(dateFrom);
 									if ( accelerateRetries )
 										newDateFrom.add(GregorianCalendar.MINUTE, 5);
 									else
 										newDateFrom.add(GregorianCalendar.DAY_OF_MONTH, 1);
 
 									GregorianCalendar newDateTo = new GregorianCalendar();
 									if ( dateTo == null ) {
 										newDateTo.setTime(dateFrom);
 										newDateTo.add(GregorianCalendar.DAY_OF_MONTH, maxDays);
 									} else
 										newDateTo.setTime(dateTo);
 
 									if ( accelerateRetries )
 										newDateTo.add(GregorianCalendar.MINUTE, 5);
 									else
 										newDateTo.add(GregorianCalendar.DAY_OF_MONTH, 1);
 
 
 									gatewayRequestDB.setDateFrom(newDateFrom.getTime());
 									gatewayRequestDB.setDateTo(newDateTo.getTime());
 									gatewayRequestDB.setMessageStatus(MStatus.SCHEDULED);
 
 									
 									gwReqDAO.save(gatewayRequestDB);
 								
 
 								}
 
 							} else {
 								status = "MAXATTEMPTS";
 							}
 							ivrSessions.remove(sessionId);
 
 							/*
 							 * update days attempted in the database
 							 */
 							MessageRequestDAO<MessageRequest> messageRequestDAO = coreManager.createMessageRequestDAO();
 
 							for ( GatewayRequest gatewayRequest : session.getGatewayRequests() ) {
 
 								MessageRequest msgReqDB = messageRequestDAO.getById(gatewayRequest.getMessageRequest().getId());
 
 								msgReqDB.setDaysAttempted(session.getDays());
 
 								
 								messageRequestDAO.save(msgReqDB);
 							
 
 							}
 
 						
 
 						}
 
 					}
 
 				}
 
 				/*
 				 * Update message status
 				 */
 				if ( status != null ) {
 					Collection<GatewayRequest> requests = session.getGatewayRequests();
 					for (GatewayRequest gatewayRequest : requests) {
 
 						log.debug("Updating Message Request "
 								+ gatewayRequest.getMessageRequest().getId().toString()
 								+ " to " + status);
 						statusStore.updateStatus(gatewayRequest
 								.getMessageRequest()
 								.getId()
 								.toString()
 								, status);
 
 					}
 
 				}
 
 			}
 
 		}
 
 		ResponseType r = new ResponseType();
 		r.setStatus(StatusType.OK);
 		return r;
 	}
 
 	private List<String> formatReportLogMessages(ReportType report) {
 
 		List<String> result = new ArrayList<String>();
 
 		StringBuilder common = new StringBuilder();
 		common.append(report.getCallee());
 		common.append("," + report.getDuration());
 		common.append("," + report.getINTELLIVREntryCount());
 		common.append("," + report.getPrivate());
 		common.append("," + report.getConnectTime());
 		common.append("," + report.getDisconnectTime());
 		common.append("," + report.getStatus().value());
 
 		result.add(common.toString());
 		
 		for ( IvrEntryType entry : report.getINTELLIVREntry() ) {
 
 			StringBuilder message = new StringBuilder();
 			message.append(common.toString());
 			message.append("," + entry.getFile());
 			message.append("," + entry.getKeypress());
 			message.append("," + entry.getMenu());
 			message.append("," + entry.getDuration());
 			message.append("," + entry.getEntrytime());
 
 			result.add(message.toString());
 
 		}
 
 		return result;
 	}
 
 	private boolean callExceedsThreshold(ReportType report) {
 
 		int effectiveCallTime = 0;
 		int reminderCount = 0;
 		boolean shouldHaveInformationalMessage = false;
 		IVRSession session = ivrSessions.get(report.getPrivate());
 
 		for ( GatewayRequest request : session.getGatewayRequests() ) {
 			long notificationId = request.getMessageRequest().getNotificationType().getId();
 			if ( ivrNotificationMap.containsKey(notificationId) )
 				if ( ivrNotificationMap.get(notificationId).getType().equalsIgnoreCase(IVRNotificationMapping.INFORMATIONAL) )
 					shouldHaveInformationalMessage = true;
 		}
 
 		IvrEntryType firstInfoEntry = null;
 
 		List<IvrEntryType> entries = report.getINTELLIVREntry();
 
 		for (IvrEntryType entry : entries)
 			if ( ivrReminderIds.containsKey(entry.getMenu()) || entry.getMenu().equalsIgnoreCase("break") )
 				reminderCount++;
 			else
 				if ( firstInfoEntry == null && (!session.isUserInitiated() || reminderCount > 0) )
 					firstInfoEntry = entry;
 
 		if ( firstInfoEntry == null )
 			if ( shouldHaveInformationalMessage )
 				effectiveCallTime = 0;
 			else
 				if ( reminderCount > 0 )
 					effectiveCallTime = callCompletedThreshold;
 				else
 					effectiveCallTime = report.getDuration();
 		else
 			effectiveCallTime = firstInfoEntry.getDuration();
 
 		return effectiveCallTime >= callCompletedThreshold;
 	}
 
 	public void setMessageHandler(GatewayMessageHandler messageHandler) {
 		this.messageHandler = messageHandler;
 	}
 
 	public GatewayMessageHandler getMessageHandler() {
 		return messageHandler;
 	}
 
 	/**
 	 * The URL to which the IVR system will be requested to post call reports
 	 * @return reportURL
 	 */
 	public String getReportURL() {
 		return reportURL;
 	}
 
 	/**
 	 * Set URL to which the IVR system will be requested to post call reports
 	 * @param reportURL
 	 */
 	public void setReportURL(String reportURL) {
 		this.reportURL = reportURL;
 	}
 
 	/**
 	 * The API key for the IntellIVR server
 	 * @return
 	 */
 	public String getApiID() {
 		return apiID;
 	}
 
 	/**
 	 * Set the API key for the IntellIVR server
 	 * @param apiID
 	 */
 	public void setApiID(String apiID) {
 		this.apiID = apiID;
 	}
 
 	/**
 	 * The implementation of the {@link IntellIVRServer} interface being used
 	 * @return
 	 */
 	public IntellIVRServer getIvrServer() {
 		return ivrServer;
 	}
 
 	/**
 	 * Set the implementation of the {@link IntellIVRServer} interface being used
 	 * @param ivrServer
 	 */
 	public void setIvrServer(IntellIVRServer ivrServer) {
 		this.ivrServer = ivrServer;
 	}
 
 	/**
 	 * The method being used for IntellIVR server
 	 * @return
 	 */
 	public String getMethod() {
 		return method;
 	}
 
 	/**
 	 * Set the method being used for IntellIVR server.  Generally 'ivroriginate'.
 	 * @param method
 	 */
 	public void setMethod(String method) {
 		this.method = method;
 	}
 
 	/**
 	 * The default language to use if not otherwise specified.
 	 * @return
 	 */
 	public String getDefaultLanguage() {
 		return defaultLanguage;
 	}
 
 	/**
 	 * Set the default language to use if not otherwise specified.
 	 * @param defaultLanguage
 	 */
 	public void setDefaultLanguage(String defaultLanguage) {
 		this.defaultLanguage = defaultLanguage;
 	}
 
 	/**
 	 * The default tree.  Not used.
 	 * @return
 	 */
 	public String getDefaultTree() {
 		return defaultTree;
 	}
 
 	/**
 	 * Set the default tree.  Not used.
 	 * @param defaultTree
 	 */
 	public void setDefaultTree(String defaultTree) {
 		this.defaultTree = defaultTree;
 	}
 
 	/**
 	 * The default reminder.  Not used.
 	 * @return
 	 */
 	public String getDefaultReminder() {
 		return defaultReminder;
 	}
 
 	/**
 	 * Set the default reminder.  Not used.
 	 * @param defaultReminder
 	 */
 	public void setDefaultReminder(String defaultReminder) {
 		this.defaultReminder = defaultReminder;
 	}
 
 	/**
 	 * The {@link MessageStatusStore} used to store request statuses.
 	 * @return
 	 */
 	public MessageStatusStore getStatusStore() {
 		return statusStore;
 	}
 
 	/**
 	 * Set the {@link MessageStatusStore} used to store request statuses.
 	 * @param statusStore
 	 */
 	public void setStatusStore(MessageStatusStore statusStore) {
 		this.statusStore = statusStore;
 	}
 
 	/**
 	 * Delay to bundle additional messages for a user before sending
 	 * See {@link #sendMessage(GatewayRequest, MotechContext)} for more details. 
 	 * @return
 	 */
 	public long getBundlingDelay() {
 		return bundlingDelay;
 	}
 
 	/**
 	 * Set delay in milliseconds to bundle additional messages for a user before sending
 	 * See {@link #sendMessage(GatewayRequest, MotechContext)} for more details. 
 	 * @param bundlingDelay
 	 */
 	public void setBundlingDelay(long bundlingDelay) {
 		this.bundlingDelay = bundlingDelay;
 	}
 
 	/**
 	 * Delay in minutes to wait before retrying after a failed message delivery.
 	 * @return
 	 */
 	public int getRetryDelay() {
 		return retryDelay;
 	}
 
 	/**
 	 * Set delay in minutes to wait before retrying after a failed message delivery.
 	 * @param retryDelay
 	 */
 	public void setRetryDelay(int retryDelay) {
 		this.retryDelay = retryDelay;
 	}
 
 	/**
 	 * Max attempts to try a deliver a message each day.
 	 * @return
 	 */
 	public int getMaxAttempts() {
 		return maxAttempts;
 	}
 
 	/**
 	 * Set max attempts to try a deliver a message each day.
 	 * @param maxAttempts
 	 */
 	public void setMaxAttempts(int maxAttempts) {
 		this.maxAttempts = maxAttempts;
 	}
 
 	/**
 	 * Max days to retry message delivery
 	 * @return
 	 */
 	public int getMaxDays() {
 		return maxDays;
 	}
 
 	/**
 	 * Set max days to retry message delivery
 	 * @param maxDays
 	 */
 	public void setMaxDays(int maxDays) {
 		this.maxDays = maxDays;
 	}
 
 	/**
 	 * Number of days a message should remain available to replayed
 	 * @return
 	 */
 	public int getAvailableDays() {
 		return availableDays;
 	}
 
 	/**
 	 * Set number of days a message should remain available to replayed
 	 * @param availableDays
 	 */
 	public void setAvailableDays(int availableDays) {
 		this.availableDays = availableDays;
 	}
 
 	/**
 	 * Seconds of the first primary informational message that the user
 	 * must have listened to to consider the message delivered.
 	 * @return
 	 */
 	public int getCallCompletedThreshold() {
 		return callCompletedThreshold;
 	}
 
 	/**
 	 * Set seconds of the first primary informational message that the user
 	 * must have listened to to consider the message delivered.
 	 * @param callCompletedThreshold
 	 */
 	public void setCallCompletedThreshold(int callCompletedThreshold) {
 		this.callCompletedThreshold = callCompletedThreshold;
 	}
 
 	/**
 	 * Seconds of silence that is pre-pended to beginning of each call. 
 	 * @return
 	 */
 	public int getPreReminderDelay() {
 		return preReminderDelay;
 	}
 
 	/**
 	 * Set seconds of silence that is pre-pended to beginning of each call.
 	 * @param preReminderDelay
 	 */
 	public void setPreReminderDelay(int preReminderDelay) {
 		this.preReminderDelay = preReminderDelay;
 	}
 
 	/**
 	 * If true, the next day retries are tried immediately.  For testing.
 	 * @return
 	 */
 	public boolean isAccelerateRetries() {
 		return accelerateRetries;
 	}
 
 	/**
 	 * Enables/disables accelerated retries
 	 * @param accelerateRetries
 	 */
 	public void setAccelerateRetries(boolean accelerateRetries) {
 		this.accelerateRetries = accelerateRetries;
 	}
 
 	/**
 	 * Name of recording to play in the event a user has no pending messages
 	 * @return
 	 */
 	public String getNoPendingMessagesRecordingName() {
 		return noPendingMessagesRecordingName;
 	}
 
 	/**
 	 * Set the name of recording to play in the event a user has no pending messages
 	 * @param noPendingMessagesRecordingName
 	 */
 	public void setNoPendingMessagesRecordingName(
 			String noPendingMessagesRecordingName) {
 		this.noPendingMessagesRecordingName = noPendingMessagesRecordingName;
 	}
 
 	/**
 	 * Name of a recording of a welcome message to be played before all other messages
 	 * @return
 	 */
 	public String getWelcomeMessageRecordingName() {
 		return welcomeMessageRecordingName;
 	}
 
 	/**
 	 * Set the name of a recording of a welcome message to be played before all other messages
 	 * @param welcomeMessageRecordingName
 	 */
 	public void setWelcomeMessageRecordingName(String welcomeMessageRecordingName) {
 		this.welcomeMessageRecordingName = welcomeMessageRecordingName;
 	}
 
 	/**
 	 * Name of file resource that contains the mapping between {@link NotificationType} ids
 	 * and file names on the IVR server.  Each line should match the following expression:
 	 * 
 	 * [0-9]+=[IiRr]{1},.+
 	 * 
 	 * @return
 	 */
 	public Resource getMappingResource() {
 		return mappingResource;
 	}
 
 	/**
 	 * Set the file resource for mapping.  See {@link #getMappingResource()}.
 	 * @param mappingsFile
 	 */
 	public void setMappingResource(Resource mappingsFile) {
 		this.mappingResource = mappingsFile;
 	}
 
 	/**
 	 * Resource to serialize pending {@link IVRSession} object to.
 	 * @return
 	 */
 	public Resource getIvrSessionSerialResource() {
 		return ivrSessionSerialResource;
 	}
 
 	/**
 	 * Set resource to serialize pending {@link IVRSession} object to.
 	 * @param ivrSessionSerialResource
 	 */
 	public void setIvrSessionSerialResource(Resource ivrSessionSerialResource) {
 		this.ivrSessionSerialResource = ivrSessionSerialResource;
 	}
 
 	/**
 	 * For access to core motech mobile services
 	 * @return
 	 */
 	public CoreManager getCoreManager() {
 		return coreManager;
 	}
 
 	/**
 	 * Set the {@link CoreManager}.
 	 * @param coreManager
 	 */
 	public void setCoreManager(CoreManager coreManager) {
 		this.coreManager = coreManager;
 	}
 
 	/**
 	 * Interface the to the Motech Server.  
 	 * @return
 	 */
 	public RegistrarService getRegistrarService() {
 		return registrarService;
 	}
 
 	/**
 	 * Set the {@link RegistrarService}.
 	 * @param registrarService
 	 */
 	public void setRegistrarService(RegistrarService registrarService) {
 		this.registrarService = registrarService;
 	}
 
 	/**
 	 * 
 	 * @author fcbrooks
 	 * TimerTask used to implement the bundling delay.
 	 */
 	protected class IVRServerTimerTask extends TimerTask {
 
 		private String sessionId;
 		private Log log = LogFactory.getLog(IVRServerTimerTask.class);
 
 		protected IVRServerTimerTask(String sessionId) {
 			this.sessionId = sessionId;
 		}
 
 		protected IVRServerTimerTask(IVRSession session) {
 			this.sessionId = session.getSessionId();
 		}
 
 		@Override
 		public void run() {
 
 			log.debug("IVR Server timer task expired for session " + sessionId);
 
 			sendPending(sessionId);
 
 		}
 
 	}
 
 }
