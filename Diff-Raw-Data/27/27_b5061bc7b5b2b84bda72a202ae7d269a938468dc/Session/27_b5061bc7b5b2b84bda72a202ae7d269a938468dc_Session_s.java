 /*******************************************************************************
  * Copyright (c) quickfixengine.org  All rights reserved.
  *
  * This file is part of the QuickFIX FIX Engine
  *
  * This file may be distributed under the terms of the quickfixengine.org
  * license as defined by quickfixengine.org and appearing in the file
  * LICENSE included in the packaging of this file.
  *
  * This file is provided AS IS with NO WARRANTY OF ANY KIND, INCLUDING
  * THE WARRANTY OF DESIGN, MERCHANTABILITY AND FITNESS FOR A
  * PARTICULAR PURPOSE.
  *
  * See http://www.quickfixengine.org/LICENSE for licensing information.
  *
  * Contact ask@quickfixengine.org if any conditions of this licensing
  * are not clear to you.
  ******************************************************************************/
 
 package quickfix;
 
 import static quickfix.LogUtil.logThrowable;
 
 import java.io.IOException;
 import java.util.ArrayList;
 import java.util.Date;
 import java.util.HashMap;
 import java.util.List;
 import java.util.Map;
 
 import quickfix.Message.Header;
 import quickfix.field.BeginSeqNo;
 import quickfix.field.BeginString;
 import quickfix.field.BusinessRejectReason;
 import quickfix.field.EncryptMethod;
 import quickfix.field.EndSeqNo;
 import quickfix.field.GapFillFlag;
 import quickfix.field.HeartBtInt;
 import quickfix.field.MsgSeqNum;
 import quickfix.field.MsgType;
 import quickfix.field.NewSeqNo;
 import quickfix.field.OrigSendingTime;
 import quickfix.field.PossDupFlag;
 import quickfix.field.RefMsgType;
 import quickfix.field.RefSeqNum;
 import quickfix.field.RefTagID;
 import quickfix.field.ResetSeqNumFlag;
 import quickfix.field.SenderCompID;
 import quickfix.field.SenderLocationID;
 import quickfix.field.SenderSubID;
 import quickfix.field.SendingTime;
 import quickfix.field.SessionRejectReason;
 import quickfix.field.TargetCompID;
 import quickfix.field.TargetLocationID;
 import quickfix.field.TargetSubID;
 import quickfix.field.TestReqID;
 import quickfix.field.Text;
 
 /**
  * The Session is the primary FIX abstraction for message communication. It
  * performs sequencing and error recovery and represents an communication channel
  * to a counterparty. Sessions are independent of specific communication layer 
  * connections. A Session is defined as starting with message sequence number of 1
  * and ending when the session is reset. The Sesion could span many sequential
  * connections (it cannot operate on multiple connection simultaneously).
  */
 public class Session {
     /**
      * Session setting for heartbeat interval (in seconds).
      */
     public static final String SETTING_HEARTBTINT = "HeartBtInt";
 
     /**
      * Session setting for enabling message latency checks. Values are "Y" or
      * "N".
      */
     public static final String SETTING_CHECK_LATENCY = "CheckLatency";
 
     /**
      * If set to Y, messages must be received from the counterparty with the 
      * correct SenderCompID and TargetCompID. Some systems will send you 
      * different CompIDs by design, so you must set this to N.
      */
     public static final String SETTING_CHECK_COMP_ID = "CheckCompID";
 
     /**
      * Session setting for maximum message latency (in seconds).
      */
     public static final String SETTING_MAX_LATENCY = "MaxLatency";
 
     /**
      * Session setting for the test delay multiplier (0-1, as fraction of Heartbeat interval)
      */
     public static final String SETTING_TEST_REQUEST_DELAY_MULTIPLIER = "TestRequestDelayMultiplier";
     /**
      * Session scheduling setting to specify first day of trading week.
      */
     public static final String SETTING_START_DAY = "StartDay";
 
     /**
      * Session scheduling setting to specify last day of trading week.
      */
     public static final String SETTING_END_DAY = "EndDay";
 
     /**
      * Session scheduling setting to specify time zone for the session.
      */
     public static final String SETTING_TIMEZONE = "TimeZone";
 
     /**
      * Session scheduling setting to specify starting time of the trading day.
      */
     public static final String SETTING_START_TIME = "StartTime";
 
     /**
      * Session scheduling setting to specify end time of the trading day.
      */
     public static final String SETTING_END_TIME = "EndTime";
 
     /**
      * Session setting to indicate whether a data dictionary should be used. If
      * a data dictionary is not used then message validation is not possble.
      */
     public static final String SETTING_USE_DATA_DICTIONARY = "UseDataDictionary";
 
     /**
      * Session setting specifying the path to the data dictionary to use for
      * this session. This setting supports the possibility of a custom data
      * dictionary for each session. Normally, the default data dictionary for a
      * specific FIX version will be specified.
      */
     public static final String SETTING_DATA_DICTIONARY = "DataDictionary";
 
     /**
      * Session validation setting for enabling whether field ordering is
      * validated. Values are "Y" or "N". Default is "Y".
      */
     public static final String SETTING_VALIDATE_FIELDS_OUT_OF_ORDER = "ValidateFieldsOutOfOrder";
 
     /**
      * Session validation setting for enabling whether field values are
      * validated. Empty fields values are not allowed. Values are "Y" or "N".
      * Default is "Y".
      */
     public static final String SETTING_VALIDATE_FIELDS_HAVE_VALUES = "ValidateFieldsHaveValues";
 
     /**
      * Session setting for logon timeout (in seconds).
      */
     public static final String SETTING_LOGON_TIMEOUT = "LogonTimeout";
 
     /**
      * Session setting for logout timeout (in seconds).
      */
     public static final String SETTING_LOGOUT_TIMEOUT = "LogoutTimeout";
 
     /**
      * Session setting for doing an automatic sequence number reset on logout.
      * Valid values are "Y" or "N". Default is "N".
      */
     public static final String SETTING_RESET_ON_LOGOUT = "ResetOnLogout";
 
     /**
      * Session setting for doing an automatic sequence number reset on
      * disconnect. Valid values are "Y" or "N". Default is "N".
      */
     public static final String SETTING_RESET_ON_DISCONNECT = "ResetOnDisconnect";
 
     /**
      * Session setting to enable milliseconds in message timestamps. Valid
      * values are "Y" or "N". Default is "Y". Only valid for FIX version >= 4.2.
      */
     public static final String SETTING_MILLISECONDS_IN_TIMESTAMP = "MillisecondsInTimeStamp";
 
     /**
      * Controls validation of user-defined fields.
      */
     public static final String SETTING_VALIDATE_USER_DEFINED_FIELDS = "ValidateUserDefinedFields";
 
     /**
      * Session setting that causes the session to reset sequence numbers when initiating
      * a logon (>= FIX 4.2).
      */
     public static final String SETTING_RESET_ON_LOGON = "ResetOnLogon";
 
     /**
      * Session description. Used by external tools.
      */
     public static final String SETTING_DESCRIPTION = "Description";
 
     /**
      * Requests that state and message data be refreshed from the message store at
      * logon, if possible. This supports simple failover behavior for acceptors
      */
     public static final String SETTING_REFRESH_ON_LOGON = "RefreshOnLogon";
 
     /**
      * Configures the session to send redundant resend requests (off, by default).
      */
     public static final String SETTING_SEND_REDUNDANT_RESEND_REQUEST = "SendRedundantResendRequests";
 
     /**
      * Persist messages setting (true, by default). If set to false this will cause the Session to
      * not persist any messages and all resend requests will be answered with a gap fill.
      */
     public static final String SETTING_PERSIST_MESSAGES = "PersistMessages";
 
     /**
      * Use actual end of sequence gap for resend requests rather than using "infinity"
      * as the end sequence of the gap. Not recommended by the FIX specification, but
      * needed for some counterparties.
      */
     public static final String USE_CLOSED_RESEND_INTERVAL = "ClosedResendInterval";
 
     /**
      * Allow unknown fields in messages. This is intended for unknown fields with tags < 5000
      * (not user defined fields)
      */
     public static final String SETTING_ALLOW_UNKNOWN_MSG_FIELDS = "AllowUnknownMsgFields";
 
     // @GuardedBy(sessions)
     private static final Map<SessionID, Session> sessions = new HashMap<SessionID, Session>();
 
     private final Application application;
     private final SessionID sessionID;
     private final SessionSchedule sessionSchedule;
     private final MessageFactory messageFactory;
 
     // @GuardedBy(this)
     private final SessionState state;
 
     private boolean enabled;
 
     private final String responderSync = "SessionResponderSync";
     // @GuardedBy(responderSync)
     private Responder responder;
 
     //
     // The session time checks were causing performance problems
     // so we are caching the last session time check result and
     // only recalculating it if it's been at least 1 second since
     // the last check
     //
     // @GuardedBy(this)
     private long lastSessionTimeCheck = 0;
     private boolean lastSessionTimeResult = false;
 
     private final DataDictionary dataDictionary;
     private final boolean checkLatency;
     private final int maxLatency;
     private final boolean resetOnLogon;
     private final boolean resetOnLogout;
     private final boolean resetOnDisconnect;
     private final boolean millisecondsInTimeStamp;
     private final boolean refreshMessageStoreAtLogon;
     private final boolean redundantResentRequestsAllowed;
     private final boolean persistMessages;
     private final boolean checkCompID;
     private final boolean useClosedRangeForResend;
 
     private final ListenerSupport stateListeners = new ListenerSupport(SessionStateListener.class);
     private final SessionStateListener stateListener = (SessionStateListener) stateListeners.getMulticaster();
     
     public static final int DEFAULT_MAX_LATENCY = 120;
     public static final double DEFAULT_TEST_REQUEST_DELAY_MULTIPLIER = 0.5;
 
 
     Session(Application application, MessageStoreFactory messageStoreFactory, SessionID sessionID,
             DataDictionary dataDictionary, SessionSchedule sessionSchedule, LogFactory logFactory,
             MessageFactory messageFactory, int heartbeatInterval) {
         this(application, messageStoreFactory, sessionID, dataDictionary, sessionSchedule,
                 logFactory, messageFactory, heartbeatInterval, true, DEFAULT_MAX_LATENCY, true, false, false,
                 false, false, true, false, true, false, DEFAULT_TEST_REQUEST_DELAY_MULTIPLIER);
     }
 
     Session(Application application, MessageStoreFactory messageStoreFactory, SessionID sessionID,
             DataDictionary dataDictionary, SessionSchedule sessionSchedule, LogFactory logFactory,
             MessageFactory messageFactory, int heartbeatInterval, boolean checkLatency,
             int maxLatency, boolean millisecondsInTimeStamp, boolean resetOnLogon,
             boolean resetOnLogout, boolean resetOnDisconnect,
             boolean refreshMessageStoreAtLogon, boolean checkCompID,
             boolean redundantResentRequestsAllowed, boolean persistMessages,
             boolean useClosedRangeForResend, double testRequestDelayMultiplier) {
         this.application = application;
         this.sessionID = sessionID;
         this.sessionSchedule = sessionSchedule;
         this.checkLatency = checkLatency;
         this.maxLatency = maxLatency;
         this.resetOnLogon = resetOnLogon;
         this.resetOnLogout = resetOnLogout;
         this.resetOnDisconnect = resetOnDisconnect;
         this.millisecondsInTimeStamp = millisecondsInTimeStamp;
         this.refreshMessageStoreAtLogon = refreshMessageStoreAtLogon;
         this.dataDictionary = dataDictionary;
         this.messageFactory = messageFactory;
         this.checkCompID = checkCompID;
         this.redundantResentRequestsAllowed = redundantResentRequestsAllowed;
         this.persistMessages = persistMessages;
         this.useClosedRangeForResend = useClosedRangeForResend;
         this.state = new SessionState(this, logFactory != null
                 ? logFactory.create(sessionID)
                 : null, heartbeatInterval, heartbeatInterval != 0, messageStoreFactory
                 .create(sessionID), testRequestDelayMultiplier);
 
         registerSession(this);
 
         getLog().onEvent("Session " + sessionID + " schedule is " + sessionSchedule);
         try {
             if (!checkSessionTime()) {
                 getLog().onEvent("Session state is not current; resetting " + sessionID);
                 reset();
             }
         } catch (IOException e) {
             LogUtil.logThrowable(getLog(), "error during session construction", e);
         }
 
         setEnabled(true);
 
         getLog().onEvent("Created session: " + sessionID);
     }
 
     public MessageFactory getMessageFactory() {
         return messageFactory;
     }
 
     /**
      * Registers a responder with the session. This is used by the acceptor and
      * initiator implementations.
      *
      * @param responder a responder implementation
      */
     public void setResponder(Responder responder) {
         synchronized (responderSync) {
             this.responder = responder;
             if (responder != null) {
                 stateListener.onConnect();
             } else {
                 stateListener.onDisconnect();
             }
         }
     }
 
     public Responder getResponder() {
         synchronized (responderSync) {
             return responder;
         }
     }
 
     /**
      * This should not be used by end users.
      *
      * @return the Session's connection responder
      */
     public boolean hasResponder() {
         return getResponder() != null;
     }
 
     private synchronized boolean checkSessionTime() throws IOException {
         if (sessionSchedule == null) {
             return true;
         }
 
         //
         // Only check the session time once per second at most. It isn't
         // necessary to do for every message received.
         //
         Date date = SystemTime.getDate();
         if ((date.getTime() - lastSessionTimeCheck) >= 1000L) {
             Date getSessionCreationTime = state.getCreationTime();
             lastSessionTimeResult = sessionSchedule.isSameSession(SystemTime.getUtcCalendar(date),
                     SystemTime.getUtcCalendar(getSessionCreationTime));
             lastSessionTimeCheck = date.getTime();
             return lastSessionTimeResult;
         } else {
             return lastSessionTimeResult;
         }
     }
 
     /**
      * Send a message to the session specified in the message's target
      * identifiers.
      *
      * @param message a FIX message
      * @return true is send was successful, false otherwise
      * @throws SessionNotFound if session could not be located
      */
     public static boolean sendToTarget(Message message) throws SessionNotFound {
         return sendToTarget(message, "");
     }
 
     /**
      * Send a message to the session specified in the message's target
      * identifiers. The session qualifier is used to distinguish sessions with
      * the same target identifiers.
      *
      * @param message   a FIX message
      * @param qualifier a session qualifier
      * @return true is send was successful, false otherwise
      * @throws SessionNotFound if session could not be located
      */
     public static boolean sendToTarget(Message message, String qualifier) throws SessionNotFound {
         try {
             String senderCompID = message.getHeader().getString(SenderCompID.FIELD);
             String targetCompID = message.getHeader().getString(TargetCompID.FIELD);
             return sendToTarget(message, senderCompID, targetCompID, qualifier);
         } catch (FieldNotFound e) {
             throw new SessionNotFound("missing sender or target company ID");
         }
     }
 
     /**
      * Send a message to the session specified by the provided target company
      * ID. The sender company ID is provided as an argument rather than from the
      * message.
      *
      * @param message      a FIX message
      * @param senderCompID the sender's company ID
      * @param targetCompID the target's company ID
      * @return true is send was successful, false otherwise
      * @throws SessionNotFound if session could not be located
      */
     public static boolean sendToTarget(Message message, String senderCompID, String targetCompID)
             throws SessionNotFound {
         return sendToTarget(message, senderCompID, targetCompID, "");
     }
 
     /**
      * Send a message to the session specified by the provided target company
      * ID. The sender company ID is provided as an argument rather than from the
      * message. The session qualifier is used to distinguish sessions with the
      * same target identifiers.
      *
      * @param message      a FIX message
      * @param senderCompID the sender's company ID
      * @param targetCompID the target's company ID
      * @param qualifier    a session qualifier
      * @return true is send was successful, false otherwise
      * @throws SessionNotFound if session could not be located
      */
     public static boolean sendToTarget(Message message, String senderCompID, String targetCompID,
             String qualifier) throws SessionNotFound {
         try {
             return sendToTarget(message, new SessionID(message.getHeader().getString(
                     BeginString.FIELD), senderCompID, targetCompID, qualifier));
         } catch (SessionNotFound e) {
             throw e;
         } catch (Exception e) {
             throw new SessionException(e);
         }
     }
 
     /**
      * Send a message to the session specified by the provided session ID.
      *
      * @param message   a FIX message
      * @param sessionID the target SessionID
      * @return true is send was successful, false otherwise
      * @throws SessionNotFound if session could not be located
      */
     public static boolean sendToTarget(Message message, SessionID sessionID) throws SessionNotFound {
         message.setSessionID(sessionID);
         Session session = lookupSession(sessionID);
         if (session == null) {
             throw new SessionNotFound();
         }
         return session.send(message);
     }
 
     static void registerSession(Session session) {
         synchronized (sessions) {
             sessions.put(session.getSessionID(), session);
         }
     }
 
     static void unregisterSessions(List<SessionID> sessionIds) {
         synchronized (sessions) {
             for (SessionID sessionId : sessionIds) {
                 sessions.remove((SessionID) sessionId);
             }
         }
     }
 
     /**
      * Locates a session specified by the provided session ID.
      *
      * @param sessionID the session ID
      * @return the session, if found, or null otherwise
      */
     public static Session lookupSession(SessionID sessionID) {
         synchronized (sessions) {
             return sessions.get(sessionID);
         }
     }
 
     /**
      * This method can be used to manually logon to a FIX session.
      */
     public void logon() {
         state.clearLogoutReason();
         setEnabled(true);
     }
 
     private synchronized void setEnabled(boolean enabled) {
         this.enabled = enabled;
     }
 
     private void initializeHeader(Message.Header header) {
         state.setLastSentTime(SystemTime.currentTimeMillis());
         header.setString(BeginString.FIELD, sessionID.getBeginString());
         header.setString(SenderCompID.FIELD, sessionID.getSenderCompID());
         optionallySetID(header, SenderSubID.FIELD, sessionID.getSenderSubID());
         optionallySetID(header, SenderLocationID.FIELD, sessionID.getSenderLocationID());
         header.setString(TargetCompID.FIELD, sessionID.getTargetCompID());
         optionallySetID(header, TargetSubID.FIELD, sessionID.getTargetSubID());
         optionallySetID(header, TargetLocationID.FIELD, sessionID.getTargetLocationID());
         header.setInt(MsgSeqNum.FIELD, getExpectedSenderNum());
         insertSendingTime(header);
     }
 
     private void optionallySetID(Header header, int field, String value) {
         if (!value.equals(SessionID.NOT_SET)) {
             header.setString(field, value);
         }
     }
 
     private void insertSendingTime(Message.Header header) {
         boolean includeMillis = sessionID.getBeginString().compareTo(FixVersions.BEGINSTRING_FIX42) >= 0
                 && millisecondsInTimeStamp;
         header.setUtcTimeStamp(SendingTime.FIELD, SystemTime.getDate(), includeMillis);
     }
 
     /**
      * This method can be used to manually logout of a FIX session.
      */
     public void logout() {
         setEnabled(false);
     }
 
     /**
      * This method can be used to manually logout of a FIX session.
      * @param reason this will be included in the logout message
      */
     public void logout(String reason) {
         state.setLogoutReason(reason);
         logout();
     }
 
     /**
      * Used internally by initiator implementation.
      *
      * @return true if session is enabled, false otherwise.
      */
     public synchronized boolean isEnabled() {
         return enabled;
     }
 
     /**
      * Predicate indicating whether a logon message has been sent.
      * 
      * (QF Compatibility)
      *
      * @return true if logon message was sent, false otherwise.
      */
     public boolean sentLogon() {
         return state.isLogonSent();
     }
 
     /**
      * Predicate indicating whether a logon message has been received.
      * 
      * (QF Compatibility)
      *
      * @return true if logon message was received, false otherwise.
      */
     public boolean receivedLogon() {
         return state.isLogonReceived();
     }
 
     /**
      * Predicate indicating whether a logout message has been sent.
      * 
      * (QF Compatibility)
      *
      * @return true if logout message was sent, false otherwise.
      */
     public boolean sentLogout() {
         return state.isLogoutSent();
     }
 
     /**
      * Predicate indicating whether a logout message has been received. This can
      * be used to determine if a session ended with an unexpected disconnect.
      *  
      * @return true if logout message has been received, false otherwise.
      */
     public boolean receivedLogout() {
         return state.isLogoutReceived();
     }
 
     /**
      * Is the session logged on.
      * 
      * @return true if logged on, false otherwise.
      */
     public boolean isLoggedOn() {
         return sentLogon() && receivedLogon();
     }
 
     private boolean isResetNeeded() {
         return sessionID.getBeginString().compareTo(FixVersions.BEGINSTRING_FIX41) >= 0
                 && (resetOnLogon || resetOnLogout || resetOnDisconnect)
                 && getExpectedSenderNum() == 1 && getExpectedTargetNum() == 1;
     }
 
     /**
      * Logouts and disconnects session and then resets session state.
      *
      * @throws IOException IO error
      * @see #disconnect()
      * @see SessionState#reset()
      */
     public void reset() throws IOException {
         if (hasResponder()) {
             generateLogout();
             disconnect();
         }
         resetState();
     }
 
     /**
      * Set the next outgoing message sequence number. This method is not
      * synchronized.
      *
      * @param num next outgoing sequence number
      * @throws IOException IO error
      */
     public void setNextSenderMsgSeqNum(int num) throws IOException {
         state.getMessageStore().setNextSenderMsgSeqNum(num);
     }
 
     /**
      * Set the next expected target message sequence number. This method is not
      * synchronized.
      *
      * @param num next expected target sequence number
      * @throws IOException IO error
      */
     public void setNextTargetMsgSeqNum(int num) throws IOException {
         state.getMessageStore().setNextTargetMsgSeqNum(num);
     }
 
     /**
      * Retrieves the expected sender sequence number. This method is not
      * synchronized.
      *
      * @return next expected sender sequence number
      */
     public int getExpectedSenderNum() {
         try {
             return state.getMessageStore().getNextSenderMsgSeqNum();
         } catch (IOException e) {
             getLog().onEvent("getNextSenderMsgSeqNum failed: " + e.getMessage());
             return -1;
         }
     }
 
     /**
      * Retrieves the expected target sequence number. This method is not
      * synchronized.
      *
      * @return next expected target sequence number
      */
     public int getExpectedTargetNum() {
         try {
             return state.getMessageStore().getNextTargetMsgSeqNum();
         } catch (IOException e) {
             getLog().onEvent("getNextTargetMsgSeqNum failed: " + e.getMessage());
             return -1;
         }
     }
 
     public Log getLog() {
         return state.getLog();
     }
 
     /**
      * Get the message store. (QF Compatibility)
      * @return the message store
      */
     public MessageStore getStore() {
         return state.getMessageStore();
     }
 
     /**
      * (Internal use only)
      */
     public void next(Message message) throws FieldNotFound, RejectLogon, IncorrectDataFormat,
             IncorrectTagValue, UnsupportedMessageType, IOException, InvalidMessage {
 
         if (!checkSessionTime()) {
             reset();
             return;
         }
 
         String msgType = message.getHeader().getString(MsgType.FIELD);
 
         try {
 
             String beginString = message.getHeader().getString(BeginString.FIELD);
 
             if (!beginString.equals(sessionID.getBeginString())) {
                 throw new UnsupportedVersion();
             }
 
             if (dataDictionary != null) {
                 dataDictionary.validate(message);
             }
 
             if (msgType.equals(MsgType.LOGON)) {
                 nextLogon(message);
             } else if (msgType.equals(MsgType.HEARTBEAT)) {
                 nextHeartBeat(message);
             } else if (msgType.equals(MsgType.TEST_REQUEST)) {
                 nextTestRequest(message);
             } else if (msgType.equals(MsgType.SEQUENCE_RESET)) {
                 nextSequenceReset(message);
             } else if (msgType.equals(MsgType.LOGOUT)) {
                 nextLogout(message);
             } else if (msgType.equals(MsgType.RESEND_REQUEST)) {
                 nextResendRequest(message);
             } else if (msgType.equals(MsgType.REJECT)) {
                 nextReject(message);
             } else {
                 if (!verify(message)) {
                     return;
                 }
                 state.incrNextTargetMsgSeqNum();
             }
         } catch (FieldException e) {
             generateReject(message, e.getSessionRejectReason(), e.getField());
         } catch (FieldNotFound e) {
             if (sessionID.getBeginString().compareTo(FixVersions.BEGINSTRING_FIX42) >= 0
                     && message.isApp()) {
                 generateBusinessReject(message,
                         BusinessRejectReason.CONDITIONALLY_REQUIRED_FIELD_MISSING, e.field);
             } else {
                 generateReject(message, SessionRejectReason.REQUIRED_TAG_MISSING, e.field);
                 if (msgType.equals(MsgType.LOGON)) {
                     getLog().onEvent("Required field missing from logon");
                     disconnect();
                 }
             }
         } catch (IncorrectDataFormat e) {
             generateReject(message, SessionRejectReason.INCORRECT_DATA_FORMAT_FOR_VALUE, e.field);
         } catch (IncorrectTagValue e) {
             generateReject(message, SessionRejectReason.VALUE_IS_INCORRECT, e.field);
         } catch (InvalidMessage e) {
             getLog().onEvent("Skipping invalid message: " + e.getMessage());
         } catch (RejectLogon e) {
             String rejectMessage = e.getMessage() != null ? (": " + e.getMessage()) : "";
             getLog().onEvent("Logon rejected" + rejectMessage);
             if (e.isLogoutBeforeDisconnect()) {
                 generateLogout(e.getMessage());
             }
             state.incrNextTargetMsgSeqNum();
             disconnect();
         } catch (UnsupportedMessageType e) {
             if (sessionID.getBeginString().compareTo(FixVersions.BEGINSTRING_FIX42) >= 0) {
                 generateBusinessReject(message, BusinessRejectReason.UNSUPPORTED_MESSAGE_TYPE, 0);
             } else {
                 generateReject(message, "Unsupported message type");
             }
         } catch (UnsupportedVersion e) {
             if (msgType.equals(MsgType.LOGOUT)) {
                 nextLogout(message);
             } else {
                 generateLogout("Incorrect BeginString");
                 state.incrNextTargetMsgSeqNum();
                 // 1d_InvalidLogonWrongBeginString.def appears to require
                 // a disconnect although the C++ didn't appear to be doing it.
                 // ???
                 disconnect();
             }
         } catch (IOException e) {
             LogUtil.logThrowable(sessionID, "error processing message", e);
         }
 
         nextQueued();
         if (isLoggedOn()) {
             next();
         }
     }
 
     private boolean isStateRefreshNeeded(String msgType) {
         return refreshMessageStoreAtLogon && !state.isInitiator() && msgType.equals(MsgType.LOGON);
     }
 
     private void nextReject(Message reject) throws FieldNotFound, RejectLogon, IncorrectDataFormat,
             IncorrectTagValue, UnsupportedMessageType, IOException, InvalidMessage {
         if (!verify(reject, false, true)) {
             return;
         }
         state.incrNextTargetMsgSeqNum();
         nextQueued();
     }
 
     private void nextResendRequest(Message resendRequest) throws IOException, RejectLogon,
             FieldNotFound, IncorrectDataFormat, IncorrectTagValue, UnsupportedMessageType,
             InvalidMessage {
         if (!verify(resendRequest, false, false)) {
             return;
         }
 
         int beginSeqNo = resendRequest.getInt(BeginSeqNo.FIELD);
         int endSeqNo = resendRequest.getInt(EndSeqNo.FIELD);
 
         getLog().onEvent("Received ResendRequest FROM: " + beginSeqNo + " TO: " + 
                 formatEndSeqNum(endSeqNo));
 
         // Adjust the ending sequence number for older versions of FIX
         String beginString = sessionID.getBeginString();
         int expectedSenderNum = getExpectedSenderNum();
         if (beginString.compareTo(FixVersions.BEGINSTRING_FIX42) >= 0 && endSeqNo == 0
                 || beginString.compareTo(FixVersions.BEGINSTRING_FIX42) <= 0 && endSeqNo == 999999
                 || endSeqNo >= expectedSenderNum) {
             endSeqNo = expectedSenderNum - 1;
         }
 
         // Just do a gap fill when messages aren't persisted
         if (!persistMessages) {
             endSeqNo += 1;
             int next = state.getNextSenderMsgSeqNum();
             if (endSeqNo > next) {
                 endSeqNo = next;
             }
             generateSequenceReset(beginSeqNo, endSeqNo);
             return;
         }
 
         ArrayList<String> messages = new ArrayList<String>();
         state.get(beginSeqNo, endSeqNo, messages);
 
         int msgSeqNum = 0;
         int begin = 0;
         int current = beginSeqNo;
 
         for (String message : messages) {
             Message msg = parseMessage((String) message);
             msgSeqNum = msg.getHeader().getInt(MsgSeqNum.FIELD);
 
             if ((current != msgSeqNum) && begin == 0) {
                 begin = current;
             }
 
             String msgType = msg.getHeader().getString(MsgType.FIELD);
 
             if (isAdminMessage(msgType)) {
                 if (begin == 0) {
                     begin = msgSeqNum;
                 }
             } else {
                 initializeResendFields(msg);
                 if (resendApproved(msg)) {
                     if (begin != 0) {
                         generateSequenceReset(begin, msgSeqNum);
                     }
                     getLog().onEvent("Resending Message: " + msgSeqNum);
                     send(msg.toString());
                     begin = 0;
                 } else {
                     if (begin == 0) {
                         begin = msgSeqNum;
                 }
             }
             }
             current = msgSeqNum + 1;
         }
 
         if (begin != 0) {
             generateSequenceReset(begin, msgSeqNum + 1);
         }
 
         if (endSeqNo > msgSeqNum) {
             endSeqNo = endSeqNo + 1;
             int next = state.getNextSenderMsgSeqNum();
             if (endSeqNo > next)
                 endSeqNo = next;
             generateSequenceReset(beginSeqNo, endSeqNo);
         }
 
         msgSeqNum = resendRequest.getHeader().getInt(MsgSeqNum.FIELD);
         if (!isTargetTooHigh(msgSeqNum) && !isTargetTooLow(msgSeqNum)) {
             state.incrNextTargetMsgSeqNum();
         }
     }
 
     private String formatEndSeqNum(int seqNo) {
         return (seqNo == 0 ? "infinity" : Integer.toString(seqNo));
     }
 
     private boolean isAdminMessage(String msgType) {
         return msgType.length() == 1 && "0A12345".indexOf(msgType) != -1;
     }
 
     private Message parseMessage(String messageData) throws InvalidMessage {
         String msgType = MessageUtils.getMessageType(messageData);
         Message msg = messageFactory.create(sessionID.getBeginString(), msgType);
         msg.fromString(messageData, dataDictionary, false);
         return msg;
     }
 
     private boolean isTargetTooLow(int msgSeqNum) throws IOException {
         return msgSeqNum < state.getNextTargetMsgSeqNum();
     }
 
     private void generateSequenceReset(int beginSeqNo, int endSeqNo) throws FieldNotFound {
         Message sequenceReset = messageFactory.create(sessionID.getBeginString(),
                 MsgType.SEQUENCE_RESET);
         int newSeqNo = endSeqNo;
         Header header = sequenceReset.getHeader();
         header.setBoolean(PossDupFlag.FIELD, true);
         initializeHeader(header);
         header.setUtcTimeStamp(OrigSendingTime.FIELD, header.getUtcTimeStamp(SendingTime.FIELD));
         header.setInt(MsgSeqNum.FIELD, beginSeqNo);
         sequenceReset.setInt(NewSeqNo.FIELD, newSeqNo);
         sequenceReset.setBoolean(GapFillFlag.FIELD, true);
         sendRaw(sequenceReset, beginSeqNo);
         getLog().onEvent("Sent SequenceReset TO: " + newSeqNo);
     }
 
     private boolean resendApproved(Message message) throws FieldNotFound {
         try {
             application.toApp(message, sessionID);
         } catch (DoNotSend e) {
             return false;
         } catch (Throwable t) {
             // Any exception other than DoNotSend will not stop the message from being resent
             logApplicationException("toApp() during resend", t);
         }
 
         return true;
     }
 
     private void initializeResendFields(Message message) throws FieldNotFound {
         Message.Header header = message.getHeader();
         Date sendingTime = header.getUtcTimeStamp(SendingTime.FIELD);
         header.setUtcTimeStamp(OrigSendingTime.FIELD, sendingTime);
         header.setBoolean(PossDupFlag.FIELD, true);
         insertSendingTime(header);
     }
 
     private void logApplicationException(String location, Throwable t) {
         logThrowable(getLog(), "Application exception in " + location, t);
     }
 
     private void nextLogout(Message logout) throws IOException, RejectLogon, FieldNotFound,
             IncorrectDataFormat, IncorrectTagValue, UnsupportedMessageType {
         if (!verify(logout, false, false)) {
             return;
         }
 
         if (!state.isLogoutSent()) {
             getLog().onEvent("Received logout request");
             generateLogout();
             getLog().onEvent("Sent logout response");
         } else {
             getLog().onEvent("Received logout response");
         }
 
         state.setLogoutReceived(true);
 
         state.incrNextTargetMsgSeqNum();
         if (resetOnLogout) {
             resetState();
         }
 
         disconnect();
     }
 
     private void generateLogout() {
         generateLogout(null);
     }
 
     private void generateLogout(String text) {
         Message logout = messageFactory.create(sessionID.getBeginString(), MsgType.LOGOUT);
         initializeHeader(logout.getHeader());
         if (text != null && !"".equals(text)) {
             logout.setString(Text.FIELD, text);
         }
         sendRaw(logout, 0);
         state.setLogoutSent(true);
     }
 
     private void nextSequenceReset(Message sequenceReset) throws IOException, RejectLogon,
             FieldNotFound, IncorrectDataFormat, IncorrectTagValue, UnsupportedMessageType {
         boolean isGapFill = false;
         if (sequenceReset.isSetField(GapFillFlag.FIELD)) {
             isGapFill = sequenceReset.getBoolean(GapFillFlag.FIELD);
         }
 
         if (!verify(sequenceReset, isGapFill, isGapFill)) {
             return;
         }
 
         if (sequenceReset.isSetField(NewSeqNo.FIELD)) {
             int newSequence = sequenceReset.getInt(NewSeqNo.FIELD);
 
             getLog().onEvent(
                     "Received SequenceReset FROM: " + getExpectedTargetNum() + " TO: "
                             + newSequence);
 
             if (newSequence > getExpectedTargetNum()) {
                 state.setNextTargetMsgSeqNum(newSequence);
             } else if (newSequence < getExpectedTargetNum()) {
                 generateReject(sequenceReset, SessionRejectReason.VALUE_IS_INCORRECT, 0);
             }
         }
     }
 
     private void generateReject(Message message, String str) throws FieldNotFound, IOException {
         String beginString = sessionID.getBeginString();
         Message reject = messageFactory.create(beginString, MsgType.REJECT);
         Header header = message.getHeader();
         
         reject.reverseRoute(header);
         initializeHeader(reject.getHeader());
 
         String msgType = header.getString(MsgType.FIELD);
         String msgSeqNum = header.getString(MsgSeqNum.FIELD);
         if (beginString.compareTo(FixVersions.BEGINSTRING_FIX42) >= 0) {
             reject.setString(RefMsgType.FIELD, msgType);
         }
         reject.setString(RefSeqNum.FIELD, msgSeqNum);
 
         if (!msgType.equals(MsgType.LOGON) && !msgType.equals(MsgType.SEQUENCE_RESET)
                 && !isPossibleDuplicate(message)) {
             state.incrNextTargetMsgSeqNum();
         }
 
         reject.setString(Text.FIELD, str);
         sendRaw(reject, 0);
         getLog().onEvent("Message " + msgSeqNum + " Rejected: " + str);
 
     }
 
     private boolean isPossibleDuplicate(Message message) throws FieldNotFound {
         Header header = message.getHeader();
         return header.isSetField(PossDupFlag.FIELD) && header.getBoolean(PossDupFlag.FIELD);
     }
 
     private void generateReject(Message message, int err, int field) throws IOException,
             FieldNotFound {
         String reason = SessionRejectReasonText.getMessage(err);
         if (!state.isLogonReceived()) {
             String errorMessage = "Tried to send a reject while not logged on: " + reason
                     + " (field " + field + ")";
             throw new SessionException(errorMessage);
 
         }
 
         String beginString = sessionID.getBeginString();
         Message reject = messageFactory.create(beginString, MsgType.REJECT);
         Header header = message.getHeader();
         
         reject.reverseRoute(header);
         initializeHeader(reject.getHeader());
 
         String msgType = "";
         if (header.isSetField(MsgType.FIELD)) {
             msgType = header.getString(MsgType.FIELD);
         }
 
         int msgSeqNum = 0;
         if (header.isSetField(MsgSeqNum.FIELD)) {
             msgSeqNum = header.getInt(MsgSeqNum.FIELD);
             reject.setInt(RefSeqNum.FIELD, msgSeqNum);
         }
 
         if (beginString.compareTo(FixVersions.BEGINSTRING_FIX42) >= 0) {
             if (!msgType.equals("")) {
                 reject.setString(RefMsgType.FIELD, msgType);
             }
             if ((beginString.equals(FixVersions.BEGINSTRING_FIX42) && err <= SessionRejectReason.INVALID_MSGTYPE)
                     || beginString.compareTo(FixVersions.BEGINSTRING_FIX42) > 0) {
                 reject.setInt(SessionRejectReason.FIELD, err);
             }
         }
 
         // This is a set and increment of target msg sequence number, the sequence
         // number must be locked to guard against race conditions.
         
         state.lockTargetMsgSeqNum();
         try {
             if (!msgType.equals(MsgType.LOGON) && !msgType.equals(MsgType.SEQUENCE_RESET)
                     && (msgSeqNum == getExpectedTargetNum() || !isPossibleDuplicate(message))) {
                 state.incrNextTargetMsgSeqNum();
             }
         } finally {
             state.unlockTargetMsgSeqNum();
         }
         
         if (reason != null && (field > 0 || err == SessionRejectReason.INVALID_TAG_NUMBER)) {
             setRejectReason(reject, field, reason, true);
             getLog().onEvent("Message " + msgSeqNum + " Rejected: " + reason + ":" + field);
         } else if (reason != null) {
             setRejectReason(reject, reason);
             getLog().onEvent("Message " + msgSeqNum + " Rejected: " + reason);
         } else
             getLog().onEvent("Message " + msgSeqNum + " Rejected");
 
         sendRaw(reject, 0);
     }
 
     private void setRejectReason(Message reject, String reason) {
         reject.setString(Text.FIELD, reason);
     }
 
     private void setRejectReason(Message reject, int field, String reason,
             boolean includeFieldInReason) {
         boolean isRejectMessage;
         try {
             isRejectMessage = MsgType.REJECT.equals(reject.getHeader().getString(MsgType.FIELD));
         } catch (FieldNotFound e) {
             isRejectMessage = false;
         }
         if (isRejectMessage
                 && sessionID.getBeginString().compareTo(FixVersions.BEGINSTRING_FIX42) >= 0) {
             reject.setInt(RefTagID.FIELD, field);
             reject.setString(Text.FIELD, reason);
         } else {
             reject.setString(Text.FIELD, reason + (includeFieldInReason ? " (" + field + ")" : ""));
         }
     }
 
     private void generateBusinessReject(Message message, int err, int field) throws FieldNotFound,
             IOException {
         Message reject = messageFactory.create(sessionID.getBeginString(),
                 MsgType.BUSINESS_MESSAGE_REJECT);
         initializeHeader(reject.getHeader());
         String msgType = message.getHeader().getString(MsgType.FIELD);
         String msgSeqNum = message.getHeader().getString(MsgSeqNum.FIELD);
         reject.setString(RefMsgType.FIELD, msgType);
         reject.setString(RefSeqNum.FIELD, msgSeqNum);
         reject.setInt(BusinessRejectReason.FIELD, err);
         state.incrNextTargetMsgSeqNum();
 
         String reason = BusinessRejectReasonText.getMessage(err);
        setRejectReason(reject, field, reason, false);
         getLog().onEvent(
                 "Message " + msgSeqNum + (reason != null ? (" Rejected: " + reason) : "")
                         + (field != 0 ? (": tag=" + field) : ""));
 
         sendRaw(reject, 0);
     }
 
     private void nextTestRequest(Message testRequest) throws FieldNotFound, RejectLogon,
             IncorrectDataFormat, IncorrectTagValue, UnsupportedMessageType, IOException,
             InvalidMessage {
         if (!verify(testRequest)) {
             return;
         }
         generateHeartbeat(testRequest);
         state.incrNextTargetMsgSeqNum();
         nextQueued();
     }
 
     private void generateHeartbeat(Message testRequest) throws FieldNotFound {
         Message heartbeat = messageFactory.create(sessionID.getBeginString(), MsgType.HEARTBEAT);
         initializeHeader(heartbeat.getHeader());
         if (testRequest.isSetField(TestReqID.FIELD)) {
             heartbeat.setString(TestReqID.FIELD, testRequest.getString(TestReqID.FIELD));
         }
         sendRaw(heartbeat, 0);
     }
 
     private void nextHeartBeat(Message heartBeat) throws FieldNotFound, RejectLogon,
             IncorrectDataFormat, IncorrectTagValue, UnsupportedMessageType, IOException,
             InvalidMessage {
         if (!verify(heartBeat)) {
             return;
         }
         state.incrNextTargetMsgSeqNum();
         nextQueued();
     }
 
     private boolean verify(Message msg, boolean checkTooHigh, boolean checkTooLow)
             throws RejectLogon, FieldNotFound, IncorrectDataFormat, IncorrectTagValue,
             UnsupportedMessageType, IOException {
         String msgType;
         try {
             Message.Header header = msg.getHeader();
             String senderCompID = header.getString(SenderCompID.FIELD);
             String targetCompID = header.getString(TargetCompID.FIELD);
             Date sendingTime = header.getUtcTimeStamp(SendingTime.FIELD);
             msgType = header.getString(MsgType.FIELD);
             int msgSeqNum = 0;
             if (checkTooHigh || checkTooLow) {
                 msgSeqNum = header.getInt(MsgSeqNum.FIELD);
             }
 
             if (!validLogonState(msgType)) {
                 throw new SessionException("Logon state is not valid for message (MsgType="
                         + msgType + ")");
             }
 
             if (!isGoodTime(sendingTime)) {
                 doBadTime(msg);
                 return false;
             }
 
             if (!isCorrectCompID(senderCompID, targetCompID)) {
                 doBadCompID(msg);
                 return false;
             }
 
             if (checkTooHigh && isTargetTooHigh(msgSeqNum)) {
                 doTargetTooHigh(msg);
                 return false;
             } else if (checkTooLow && isTargetTooLow(msgSeqNum)) {
                 doTargetTooLow(msg);
                 return false;
             }
             
             // Handle poss dup where msgSeq is as expected
             // FIX 4.4 Vol 2, test case 2f&g
             if (isPossibleDuplicate(msg) && !validatePossDup(msg)) {
                 return false;
             }
 
             if ((checkTooHigh || checkTooLow) && state.isResendRequested()) {
                 synchronized (state.getLock()) {
                     int[] range = state.getResendRange();
                     if (msgSeqNum >= range[1]) {
                         getLog().onEvent(
                                 "ResendRequest for messages FROM " + range[0] + " TO " + range[1]
                                         + " has been satisfied.");
                         state.setResendRange(0, 0);
                     }
                 }
             }
         } catch (FieldNotFound e) {
             throw e;
         } catch (Exception e) {
             getLog().onEvent(e.getClass().getName() + " " + e.getMessage());
             disconnect();
             return false;
         }
 
         state.setLastReceivedTime(SystemTime.currentTimeMillis());
         state.clearTestRequestCounter();
 
         fromCallback(msgType, msg, sessionID);
         return true;
     }
 
     private boolean doTargetTooLow(Message msg) throws FieldNotFound, IOException {
         if (!isPossibleDuplicate(msg)) {
             int msgSeqNum = msg.getHeader().getInt(MsgSeqNum.FIELD);
             String text = "MsgSeqNum too low, expecting " + getExpectedTargetNum()
                     + " but received " + msgSeqNum;
             generateLogout(text);
             throw new SessionException(text);
         }
 
         return validatePossDup(msg);
     }
 
     private void doBadCompID(Message msg) throws IOException, FieldNotFound {
         generateReject(msg, SessionRejectReason.COMPID_PROBLEM, 0);
         generateLogout();
     }
 
     private void doBadTime(Message msg) throws IOException, FieldNotFound {
         generateReject(msg, SessionRejectReason.SENDINGTIME_ACCURACY_PROBLEM, 0);
         generateLogout();
     }
 
     private boolean isGoodTime(Date sendingTime) {
         if (!checkLatency) {
             return true;
         }
         return Math.abs(SystemTime.currentTimeMillis() - sendingTime.getTime()) / 1000 <= maxLatency;
     }
 
     private void fromCallback(String msgType, Message msg, SessionID sessionID2)
             throws RejectLogon, FieldNotFound, IncorrectDataFormat, IncorrectTagValue,
             UnsupportedMessageType {
         // Application exceptions will prevent the incoming sequence number from being incremented
         // and may result in resend requests and the next startup. This way, a buggy application
         // can be fixed and then reprocess previously sent messages.
         if (isAdminMessage(msgType)) {
             application.fromAdmin(msg, sessionID);
         } else {
             application.fromApp(msg, sessionID);
         }
     }
 
     private synchronized boolean validLogonState(String msgType) {
         if (msgType.equals(MsgType.LOGON) && state.isResetSent() || state.isResetReceived()) {
             return true;
         }
         if (msgType.equals(MsgType.LOGON) && !state.isLogonReceived()
                 || !msgType.equals(MsgType.LOGON) && state.isLogonReceived()) {
             return true;
         }
         if (msgType.equals(MsgType.LOGOUT) && state.isLogonSent()) {
             return true;
         }
         if (!msgType.equals(MsgType.LOGOUT) && state.isLogoutSent()) {
             return true;
         }
         if (msgType.equals(MsgType.SEQUENCE_RESET)) {
             return true;
         }
         if (msgType.equals(MsgType.REJECT)) {
             return true;
         }
         return false;
     }
 
     private boolean verify(Message message) throws RejectLogon, FieldNotFound, IncorrectDataFormat,
             IncorrectTagValue, UnsupportedMessageType, IOException {
         return verify(message, true, true);
     }
 
     /**
      * Called from the timer-related code in the acceptor/initiator
      * implementations. This is not typically called from application code.
      *
      * @throws IOException IO error
      */
     public void next() throws IOException {
         if (!isEnabled()) {
             if (isLoggedOn()) {
                 if (!state.isLogoutSent()) {
                     getLog().onEvent("Initiated logout request");
                     generateLogout(state.getLogoutReason());
                 }
             } else {
                 return;
             }
         }
 
         if (!checkSessionTime()) {
             reset();
             return;
         }
 
         // Return if we are not connected
         if (!hasResponder()) {
             return;
         }
         
         if (!state.isLogonReceived()) {
             if (state.isLogonSendNeeded()) {
                 if (generateLogon()) {
                     getLog().onEvent("Initiated logon request");
                 } else {
                     getLog().onEvent("Error during logon request initiation");
                 }
             } else if (state.isLogonAlreadySent() && state.isLogonTimedOut()) {
                 getLog().onEvent("Timed out waiting for logon response");
                 disconnect();
             }
             return;
         }
 
         if (state.getHeartBeatInterval() == 0) {
             return;
         }
 
         if (state.isLogoutTimedOut()) {
             getLog().onEvent("Timed out waiting for logout response");
             disconnect();
         }
 
         if (state.isWithinHeartBeat()) {
             return;
         }
 
         if (state.isTimedOut()) {
             getLog().onEvent("Timed out waiting for heartbeat");
             disconnect();
             stateListener.onHeartBeatTimeout();
         } else {
             if (state.isTestRequestNeeded()) {
                 generateTestRequest("TEST");
                 state.incrementTestRequestCounter();
                 getLog().onEvent("Sent test request TEST");
                 stateListener.onMissedHeartBeat();
             } else if (state.isHeartBeatNeeded()) {
                 generateHeartbeat();
             }
         }
     }
 
     private void generateHeartbeat() {
         Message heartbeat = messageFactory.create(sessionID.getBeginString(), MsgType.HEARTBEAT);
         initializeHeader(heartbeat.getHeader());
         sendRaw(heartbeat, 0);
     }
 
     private void generateTestRequest(String id) {
         Message testRequest = messageFactory.create(sessionID.getBeginString(),
                 MsgType.TEST_REQUEST);
         initializeHeader(testRequest.getHeader());
         testRequest.setString(TestReqID.FIELD, id);
         sendRaw(testRequest, 0);
     }
 
     private boolean generateLogon() throws IOException {
         Message logon = messageFactory.create(sessionID.getBeginString(), MsgType.LOGON);
         logon.setInt(EncryptMethod.FIELD, 0);
         logon.setInt(HeartBtInt.FIELD, state.getHeartBeatInterval());
         if (isStateRefreshNeeded(MsgType.LOGON)) {
             getLog().onEvent("Refreshing message/state store at logon");
             getStore().refresh();
             stateListener.onRefresh();
         }
         if (resetOnLogon) {
             resetState();
         }
         if (isResetNeeded()) {
             logon.setBoolean(ResetSeqNumFlag.FIELD, true);
         }
         state.setLastReceivedTime(SystemTime.currentTimeMillis());
         state.clearTestRequestCounter();
         state.setLogonSent(true);
         return sendRaw(logon, 0);
     }
 
     /**
      * Logs out from session and closes the network connection.
      *
      * @throws IOException IO error
      */
     public void disconnect() throws IOException {
         synchronized (responderSync) {
             if (!hasResponder()) {
                 return;
             }
             getLog().onEvent("Disconnecting");
             responder.disconnect();
             setResponder(null);
         }
         
         boolean logonReceived = state.isLogonReceived();
         boolean logonSent = state.isLogonSent();
         if (logonReceived || logonSent) {
             state.setLogonReceived(false);
             state.setLogonSent(false);
             
             try {
                 application.onLogout(sessionID);
             } catch (Throwable t) {
                 logApplicationException("onLogout()", t);
             }
             
             stateListener.onLogout();
         }
 
         state.setLogoutSent(false);
         state.setLogoutReceived(false);
         state.setResetReceived(false);
         state.setResetSent(false);
 
         state.clearQueue();
         state.clearLogoutReason();
         state.setResendRange(0, 0);
 
         if (resetOnDisconnect) {
             resetState();
         }
 
     }
 
     private void nextLogon(Message logon) throws FieldNotFound, RejectLogon, IncorrectDataFormat,
             IncorrectTagValue, UnsupportedMessageType, IOException, InvalidMessage {
         String senderCompID = logon.getHeader().getString(SenderCompID.FIELD);
         String targetCompID = logon.getHeader().getString(TargetCompID.FIELD);
 
         if (isStateRefreshNeeded(MsgType.LOGON)) {
             getLog().onEvent("Refreshing message/state store at logon");
             getStore().refresh();
             stateListener.onRefresh();
         }
 
         if (logon.isSetField(ResetSeqNumFlag.FIELD)) {
             state.setResetReceived(logon.getBoolean(ResetSeqNumFlag.FIELD));
         }
 
         if (state.isResetReceived()) {
             getLog().onEvent("Logon contains ResetSeqNumFlag=Y, resetting sequence numbers to 1");
             if (!state.isResetSent()) {
                 resetState();
             }
         }
 
         if (state.isLogonSendNeeded() && !state.isResetReceived()) {
             getLog().onEvent("Received logon response before sending request");
             disconnect();
             return;
         }
 
         if (!state.isInitiator() && resetOnLogon) {
             resetState();
         }
 
         if (!verify(logon, false, true)) {
             return;
         }
 
         state.setLogonReceived(true);
 
         if (isCorrectCompID(senderCompID, targetCompID)) {
             state.setLogonReceived(true);
         }
 
         if (!state.isInitiator() || (state.isResetSent() && !state.isResetReceived())) {
             getLog().onEvent("Received logon request");
             generateLogon(logon);
             getLog().onEvent("Responding to logon request");
         } else {
             getLog().onEvent("Received logon response");
         }
 
         state.setResetSent(false);
         state.setResetReceived(false);
 
         int sequence = logon.getHeader().getInt(MsgSeqNum.FIELD);
         if (isTargetTooHigh(sequence) && !resetOnLogon) {
             doTargetTooHigh(logon);
         } else {
             state.incrNextTargetMsgSeqNum();
             nextQueued();
         }
 
         if (isLoggedOn()) {
             try {
                 application.onLogon(sessionID);
             } catch (Throwable t) {
                 logApplicationException("onLogon()", t);
             }
             stateListener.onLogon();
         }
     }
 
     private void nextQueued() throws FieldNotFound, RejectLogon, IncorrectDataFormat,
             IncorrectTagValue, UnsupportedMessageType, IOException, InvalidMessage {
         while (nextQueued(getExpectedTargetNum())) {
             // continue
         }
     }
 
     private boolean nextQueued(int num) throws FieldNotFound, RejectLogon, IncorrectDataFormat,
             IncorrectTagValue, UnsupportedMessageType, IOException, InvalidMessage {
         Message msg = state.dequeue(num);
 
         if (msg != null) {
             getLog().onEvent(
                     "Processing queued message: " + num + ", pending: "
                             + state.getQueuedSeqNums());
             
             String msgType = msg.getHeader().getString(MsgType.FIELD);
             if (msgType.equals(MsgType.LOGON) || msgType.equals(MsgType.RESEND_REQUEST)) {
                 state.incrNextTargetMsgSeqNum();
             } else {
                 // TODO SESSION Is it necessary to convert the queued message to a string?
                 next(msg.toString());
             }
             return true;
         }
         return false;
     }
 
     private void next(String msg) throws InvalidMessage, FieldNotFound, RejectLogon,
             IncorrectDataFormat, IncorrectTagValue, UnsupportedMessageType, IOException {
         try {
             next(parseMessage(msg));
         } catch (InvalidMessage e) {
             String message = e.getMessage();
             getLog().onEvent(message);
             if (MsgType.LOGON.equals(MessageUtils.getMessageType(msg))) {
                 getLog().onEvent("Logon message is not valid");
                 disconnect();
             }
             throw e;
         }
     }
 
     private void doTargetTooHigh(Message msg) throws FieldNotFound {
         Message.Header header = msg.getHeader();
         String beginString = header.getString(BeginString.FIELD);
         int msgSeqNum = header.getInt(MsgSeqNum.FIELD);
 
         getLog().onEvent(
                 "MsgSeqNum too high, expecting " + getExpectedTargetNum() + " but received "
                         + msgSeqNum);
         state.enqueue(msgSeqNum, msg);
 
         if (state.isResendRequested()) {
             int[] range = state.getResendRange();
 
             if (!redundantResentRequestsAllowed && msgSeqNum >= range[0]) {
                 getLog().onEvent(
                         "Already sent ResendRequest FROM: " + range[0] + " TO: " + range[1]
                                 + ".  Not sending another.");
                 return;
             }
         }
 
         generateResendRequest(beginString, msgSeqNum);
     }
 
     private void generateResendRequest(String beginString, int msgSeqNum) {
         Message resendRequest = messageFactory.create(beginString, MsgType.RESEND_REQUEST);
         int beginSeqNo = getExpectedTargetNum();
         
         int endSeqNo = msgSeqNum - 1; 
         if (!useClosedRangeForResend) {
             if (beginString.compareTo("FIX.4.2") >= 0) {
                 endSeqNo = 0;
             } else if (beginString.compareTo("FIX.4.1") <= 0) {
                 endSeqNo = 999999;
             }
         }
         
         resendRequest.setInt(BeginSeqNo.FIELD, beginSeqNo);
         resendRequest.setInt(EndSeqNo.FIELD, endSeqNo);
         initializeHeader(resendRequest.getHeader());
         sendRaw(resendRequest, 0);
         getLog().onEvent("Sent ResendRequest FROM: " + beginSeqNo + " TO: " + endSeqNo);
         state.setResendRange(beginSeqNo, msgSeqNum - 1);
     }
 
     private boolean validatePossDup(Message msg) throws FieldNotFound, IOException {
         Message.Header header = msg.getHeader();
         String msgType = header.getString(MsgType.FIELD);
         Date sendingTime = header.getUtcTimeStamp(SendingTime.FIELD);
 
         if (!msgType.equals(MsgType.SEQUENCE_RESET)) {
             if (!header.isSetField(OrigSendingTime.FIELD)) {
                 generateReject(msg, SessionRejectReason.REQUIRED_TAG_MISSING, OrigSendingTime.FIELD);
                 return false;
             }
 
             Date origSendingTime = header.getUtcTimeStamp(OrigSendingTime.FIELD);
             if (origSendingTime.compareTo(sendingTime) > 0) {
                 generateReject(msg, SessionRejectReason.SENDINGTIME_ACCURACY_PROBLEM, 0);
                 generateLogout();
                 return false;
             }
         }
 
         return true;
     }
 
     private boolean isTargetTooHigh(int sequence) throws IOException {
         return sequence > state.getNextTargetMsgSeqNum();
     }
 
     private void generateLogon(Message otherLogon) throws FieldNotFound {
         Message logon = messageFactory.create(sessionID.getBeginString(), MsgType.LOGON);
         logon.setInt(EncryptMethod.FIELD, 0);
         if (state.isResetReceived()) {
             logon.setBoolean(ResetSeqNumFlag.FIELD, true);
         }
         logon.setInt(HeartBtInt.FIELD, otherLogon.getInt(HeartBtInt.FIELD));
         initializeHeader(logon.getHeader());
         sendRaw(logon, 0);
         state.setLogonSent(true);
     }
 
     private boolean sendRaw(Message message, int num) {
         // sequence number must be locked until application
         // callback returns since it may be effectively rolled
         // back if the callback fails.
         state.lockSenderMsgSeqNum();
         try {
             boolean result = false;
             Message.Header header = message.getHeader();
             String msgType = header.getString(MsgType.FIELD);
 
             
             initializeHeader(header);
 
             if (num > 0) {
                 header.setInt(MsgSeqNum.FIELD, num);
             }
 
             String messageString = null;
 
             if (message.isAdmin()) {
                 try {
                     application.toAdmin(message, sessionID);
                 } catch (Throwable t) {
                     logApplicationException("toAdmin()", t);
                 }
 
                 if (msgType.equals(MsgType.LOGON) && !state.isResetReceived()) {
                     boolean resetSeqNumFlag = false;
                     if (message.isSetField(ResetSeqNumFlag.FIELD)) {
                         resetSeqNumFlag = message.getBoolean(ResetSeqNumFlag.FIELD);
                     }
                     if (resetSeqNumFlag) {
                         resetState();
                         message.getHeader().setInt(MsgSeqNum.FIELD, getExpectedSenderNum());
                     }
                     state.setResetSent(resetSeqNumFlag);
                 }
 
                 messageString = message.toString();
                 if (msgType.equals(MsgType.LOGON) || msgType.equals(MsgType.LOGOUT)
                         || msgType.equals(MsgType.RESEND_REQUEST)
                         || msgType.equals(MsgType.SEQUENCE_RESET) || isLoggedOn()) {
                     result = send(messageString);
                 }
             } else {
                 try {
                     application.toApp(message, sessionID);
                 } catch (DoNotSend e) {
                     return false;
                 } catch (Throwable t) {
                     logApplicationException("toApp()", t);
                 }
                 messageString = message.toString();
                 if (isLoggedOn()) {
                     result = send(messageString);
                 }
             }
 
             if (num == 0) {
                 int msgSeqNum = header.getInt(MsgSeqNum.FIELD);
                 if (persistMessages) {
                     state.set(msgSeqNum, messageString);
                 }
                 state.incrNextSenderMsgSeqNum();
             }
             return result;
         } catch (IOException e) {
             logThrowable(getLog(), "Error Reading/Writing in MessageStore", e);
             return false;
         } catch (FieldNotFound e) {
             logThrowable(state.getLog(), "Error accessing message fields", e);
             return false;
         } finally {
             state.unlockSenderMsgSeqNum();
         }
     }
 
     private void resetState() {
         state.reset();
         stateListener.onReset();
     }
 
     /**
      * Send a message to a counterparty. Sequence numbers and information about the sender
      * and target identification will be added automatically (or overwritten if that
      * information already is present). 
      * 
      * The returned status flag is included for 
      * compatibility with the JNI API but it's usefulness is questionable.
      * In QuickFIX/J, the message is transmitted using asynchronous network I/O so the boolean
      * only indicates the message was successfully queued for transmission. An error could still
      * occur before the message data is actually sent.
      * 
      * @param message the message to send
      * @return a status flag indicating whether the write to the network layer was successful.
      * 
      */
     public boolean send(Message message) {
         message.getHeader().removeField(PossDupFlag.FIELD);
         message.getHeader().removeField(OrigSendingTime.FIELD);
         return sendRaw(message, 0);
     }
 
     private boolean send(String messageString) {
         getLog().onOutgoing(messageString);
         synchronized (responderSync) {
        if (!hasResponder()) {
            getLog().onEvent("No responder, not sending message");
            return false;
        }
        return getResponder().send(messageString);
         }
     }
 
     private boolean isCorrectCompID(String senderCompID, String targetCompID) {
         if (!checkCompID) {
             return true;
         }
         return sessionID.getSenderCompID().equals(targetCompID)
                 && sessionID.getTargetCompID().equals(senderCompID);
     }
 
     /**
      * Set the data dictionary. (QF Compatibility)
      * 
      * @deprecated
      * @param dataDictionary
      */
     public void setDataDictionary(DataDictionary dataDictionary) {
         throw new UnsupportedOperationException(
                 "Modification of session dictionary is not supported in QFJ");
     }
 
     public DataDictionary getDataDictionary() {
         return dataDictionary;
     }
 
     public SessionID getSessionID() {
         return sessionID;
     }
 
     /**
      * Predicate for determining if the session should be active at the current
      * time.
      *
      * @return true if session should be active, false otherwise.
      */
     public boolean isSessionTime() {
         return sessionSchedule.isSessionTime();
     }
 
     /**
      * Determine if a session exists with the given ID.
      * @param sessionID
      * @return true if session exists, false otherwise.
      */
     public static boolean doesSessionExist(SessionID sessionID) {
         synchronized (sessions) {
             return sessions.containsKey(sessionID);
         }
     }
 
     /**
      * Return the session count.
      * @return the number of sessions
      */
     public static int numSessions() {
         synchronized (sessions) {
             return sessions.size();
         }
     }
 
     /**
      * Sets the timeout for waiting for a logon response.
      * @param seconds the timeout in seconds
      */
     public void setLogonTimeout(int seconds) {
         state.setLogonTimeout(seconds);
     }
 
     /**
      * Sets the timeout for waiting for a logout response.
      * @param seconds the timeout in seconds
      */
     public void setLogoutTimeout(int seconds) {
         state.setLogoutTimeout(seconds);
     }
 
     /**
      * Internal use by acceptor code.
      * 
      * @param heartbeatInterval
      */
     public void setHeartBeatInterval(int heartbeatInterval) {
         state.setHeartBeatInterval(heartbeatInterval);
     }
 
     public boolean getCheckCompID() {
         return checkCompID;
     }
 
     public int getLogonTimeout() {
         return state.getLogonTimeout();
     }
 
     public int getLogoutTimeout() {
         return state.getLogoutTimeout();
     }
 
     public boolean getRedundantResentRequestsAllowed() {
         return redundantResentRequestsAllowed;
     }
 
     public boolean getRefreshOnLogon() {
         return refreshMessageStoreAtLogon;
     }
 
     public boolean getResetOnDisconnect() {
         return resetOnDisconnect;
     }
 
     public boolean getResetOnLogout() {
         return resetOnLogout;
     }
 
     public boolean isLogonAlreadySent() {
         return state.isLogonAlreadySent();
     }
 
     public boolean isLogonReceived() {
         return state.isLogonReceived();
     }
 
     public boolean isLogonSendNeeded() {
         return state.isLogonSendNeeded();
     }
 
     public boolean isLogonSent() {
         return state.isLogonSent();
     }
 
     public boolean isLogonTimedOut() {
         return state.isLogonTimedOut();
     }
 
     public boolean isLogoutReceived() {
         return state.isLogoutReceived();
     }
 
     public boolean isLogoutSent() {
         return state.isLogoutSent();
     }
 
     public boolean isLogoutTimedOut() {
         return state.isLogoutTimedOut();
     }
 
     public boolean isUsingDataDictionary() {
         return dataDictionary != null;
     }
 
     public Date getStartTime() throws IOException {
         return state.getCreationTime();
     }
 
     public double getTestRequestDelayMultiplier() {
         return state.getTestRequestDelayMultiplier();
     }
 
     public String toString() {
         String s = sessionID.toString();
         try {
             s += "[in:" + state.getNextTargetMsgSeqNum() + ",out:" + state.getNextSenderMsgSeqNum()
                     + "]";
         } catch (IOException e) {
             LogUtil.logThrowable(sessionID, e.getMessage(), e);
         }
         return s;
     }
     
     public void addStateListener(SessionStateListener listener) {
         stateListeners.addListener(listener);
     }
     
     public void removeStateListener(SessionStateListener listener) {
         stateListeners.removeListener(listener);
     }
 }
