 /*
  * JBoss, Home of Professional Open Source
  * Copyright 2010, Red Hat, Inc. and/or its affiliates, and individual
  * contributors as indicated by the @authors tag. All rights reserved.
  * See the copyright.txt in the distribution for a full listing
  * of individual contributors.
  * 
  * This copyrighted material is made available to anyone wishing to use,
  * modify, copy, or redistribute it subject to the terms and conditions
  * of the GNU General Public License, v. 2.0.
  * 
  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of 
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU 
  * General Public License for more details.
  * 
  * You should have received a copy of the GNU General Public License,
  * v. 2.0 along with this distribution; if not, write to the Free 
  * Software Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
  * MA 02110-1301, USA.
  */
 package org.jdiameter.server.impl.app.cxdx;
 
 import org.jdiameter.api.Answer;
 import org.jdiameter.api.EventListener;
 import org.jdiameter.api.IllegalDiameterStateException;
 import org.jdiameter.api.InternalException;
 import org.jdiameter.api.NetworkReqListener;
 import org.jdiameter.api.OverloadException;
 import org.jdiameter.api.Request;
 import org.jdiameter.api.RouteException;
 import org.jdiameter.api.app.AppEvent;
 import org.jdiameter.api.app.StateChangeListener;
 import org.jdiameter.api.app.StateEvent;
 import org.jdiameter.api.cxdx.ServerCxDxSession;
 import org.jdiameter.api.cxdx.ServerCxDxSessionListener;
 import org.jdiameter.api.cxdx.events.JLocationInfoAnswer;
 import org.jdiameter.api.cxdx.events.JLocationInfoRequest;
 import org.jdiameter.api.cxdx.events.JMultimediaAuthAnswer;
 import org.jdiameter.api.cxdx.events.JMultimediaAuthRequest;
 import org.jdiameter.api.cxdx.events.JPushProfileAnswer;
 import org.jdiameter.api.cxdx.events.JPushProfileRequest;
 import org.jdiameter.api.cxdx.events.JRegistrationTerminationAnswer;
 import org.jdiameter.api.cxdx.events.JRegistrationTerminationRequest;
 import org.jdiameter.api.cxdx.events.JServerAssignmentAnswer;
 import org.jdiameter.api.cxdx.events.JServerAssignmentRequest;
 import org.jdiameter.api.cxdx.events.JUserAuthorizationAnswer;
 import org.jdiameter.api.cxdx.events.JUserAuthorizationRequest;
 import org.jdiameter.client.api.ISessionFactory;
 import org.jdiameter.common.api.app.cxdx.CxDxSessionState;
 import org.jdiameter.common.api.app.cxdx.ICxDxMessageFactory;
 import org.jdiameter.common.impl.app.AppAnswerEventImpl;
 import org.jdiameter.common.impl.app.AppRequestEventImpl;
 import org.jdiameter.common.impl.app.cxdx.CxDxSession;
 import org.jdiameter.server.impl.app.cxdx.Event.Type;
 import org.slf4j.Logger;
 import org.slf4j.LoggerFactory;
 
 /**
  * Cx/Dx Server session implementation
  * 
  * @author <a href="mailto:baranowb@gmail.com">Bartosz Baranowski </a>
  * @author <a href="mailto:brainslog@gmail.com"> Alexandre Mendonca </a>
  */
 public class CxDxServerSessionImpl extends CxDxSession implements ServerCxDxSession, EventListener<Request, Answer>, NetworkReqListener {
 
   private static final long serialVersionUID = 1L;
 
   private static final Logger logger = LoggerFactory.getLogger(CxDxServerSessionImpl.class);
 
   // Factories and Listeners --------------------------------------------------
   private transient ServerCxDxSessionListener listener;
 
   protected long appId = -1;
   protected IServerCxDxSessionData sessionData;
 
   public CxDxServerSessionImpl(IServerCxDxSessionData sessionData, ICxDxMessageFactory fct, ISessionFactory sf, ServerCxDxSessionListener lst) {
     super(sf,sessionData);
     if (lst == null) {
       throw new IllegalArgumentException("Listener can not be null");
     }
     if ((this.appId = fct.getApplicationId()) < 0) {
       throw new IllegalArgumentException("ApplicationId can not be less than zero");
     }
 
     this.listener = lst;
     super.messageFactory = fct;
     this.sessionData = sessionData;
   }
 
   /* (non-Javadoc)
    * @see org.jdiameter.api.cxdx.ServerCxDxSession#sendLocationInformationAnswer(org.jdiameter.api.cxdx.events.JLocationInfoAnswer)
    */
   public void sendLocationInformationAnswer(JLocationInfoAnswer answer) throws InternalException, IllegalDiameterStateException, RouteException, OverloadException {
     send(Event.Type.SEND_MESSAGE, null, answer);
   }
 
   /* (non-Javadoc)
    * @see org.jdiameter.api.cxdx.ServerCxDxSession#sendMultimediaAuthAnswer(org.jdiameter.api.cxdx.events.JMultimediaAuthAnswer)
    */
   public void sendMultimediaAuthAnswer(JMultimediaAuthAnswer answer) throws InternalException, IllegalDiameterStateException, RouteException, OverloadException {
     send(Event.Type.SEND_MESSAGE, null, answer);
   }
 
   /* (non-Javadoc)
    * @see org.jdiameter.api.cxdx.ServerCxDxSession#sendServerAssignmentAnswer(org.jdiameter.api.cxdx.events.JServerAssignmentAnswer)
    */
   public void sendServerAssignmentAnswer(JServerAssignmentAnswer answer) throws InternalException, IllegalDiameterStateException, RouteException, OverloadException {
     send(Event.Type.SEND_MESSAGE, null, answer);
   }
 
   /* (non-Javadoc)
    * @see org.jdiameter.api.cxdx.ServerCxDxSession#sendUserAuthorizationAnswer(org.jdiameter.api.cxdx.events.JUserAuthorizationAnswer)
    */
   public void sendUserAuthorizationAnswer(JUserAuthorizationAnswer answer) throws InternalException, IllegalDiameterStateException, RouteException, OverloadException {
     send(Event.Type.SEND_MESSAGE, null, answer);
   }
 
   /*
    * (non-Javadoc)
    * @see org.jdiameter.api.cxdx.ServerCxDxSession#sendPushProfileRequest(org.jdiameter.api.cxdx.events.JPushProfileRequest)
    */
   public void sendPushProfileRequest(JPushProfileRequest request) throws InternalException, IllegalDiameterStateException, RouteException, OverloadException {
     send(Event.Type.SEND_MESSAGE, request, null);
   }
 
   /*
    * (non-Javadoc)
    * @see org.jdiameter.api.cxdx.ServerCxDxSession#sendRegistrationTerminationRequest(org.jdiameter.api.cxdx.events.JRegistrationTerminationRequest)
    */
   public void sendRegistrationTerminationRequest(JRegistrationTerminationRequest request) throws InternalException, IllegalDiameterStateException, RouteException, OverloadException {
     send(Event.Type.SEND_MESSAGE, request, null);
   }
 
   /*
    * (non-Javadoc)
    * @see org.jdiameter.api.app.StateMachine#getState(java.lang.Class)
    */
   @SuppressWarnings("unchecked")
   public <E> E getState(Class<E> stateType) {
     return stateType == CxDxSessionState.class ? (E) this.sessionData.getCxDxSessionState() : null;
   }
 
   /*
    * (non-Javadoc)
    * @see org.jdiameter.api.app.StateMachine#handleEvent(org.jdiameter.api.app.StateEvent)
    */
   public boolean handleEvent(StateEvent event) throws InternalException, OverloadException {
     try {
       sendAndStateLock.lock();
       if (!super.session.isValid()) {
         // FIXME: throw new InternalException("Generic session is not valid.");
         return false;
       }
       final CxDxSessionState state = this.sessionData.getCxDxSessionState();
       CxDxSessionState newState = null;
       Event.Type eventType = (Type) event.getType();
       switch (state) {
 
       case IDLE:
         switch (eventType) {
 
         case RECEIVE_LIR:
           this.sessionData.setBuffer((Request)((AppEvent) event.getData()).getMessage());
           super.cancelMsgTimer();
           super.startMsgTimer();
           newState = CxDxSessionState.MESSAGE_SENT_RECEIVED;
           listener.doLocationInformationRequest(this,  (JLocationInfoRequest) event.getData());
           break;
 
         case RECEIVE_MAR:
           this.sessionData.setBuffer((Request)((AppEvent) event.getData()).getMessage());
           super.cancelMsgTimer();
           super.startMsgTimer();
           newState = CxDxSessionState.MESSAGE_SENT_RECEIVED;
           listener.doMultimediaAuthRequest(this,  (JMultimediaAuthRequest) event.getData());
           break;
 
         case RECEIVE_SAR:
           this.sessionData.setBuffer((Request)((AppEvent) event.getData()).getMessage());
           super.cancelMsgTimer();
           super.cancelMsgTimer();
           super.startMsgTimer();
           newState = CxDxSessionState.MESSAGE_SENT_RECEIVED;
           listener.doServerAssignmentRequest(this,  (JServerAssignmentRequest) event.getData());
           break;
 
         case RECEIVE_UAR:
           this.sessionData.setBuffer((Request)((AppEvent) event.getData()).getMessage());
           super.cancelMsgTimer();
           super.cancelMsgTimer();
           super.startMsgTimer();
           newState = CxDxSessionState.MESSAGE_SENT_RECEIVED;
           listener.doUserAuthorizationRequest(this, (JUserAuthorizationRequest) event.getData());
           break;
 
         case SEND_MESSAGE:
           newState = CxDxSessionState.MESSAGE_SENT_RECEIVED;
           super.session.send(((AppEvent) event.getData()).getMessage(),this);
           break;
 
         default:
           logger.error("Wrong action in Cx/Dx Server FSM. State: IDLE, Event Type: {}", eventType);
           break;
         }
         break;
 
       case MESSAGE_SENT_RECEIVED:
         switch (eventType) {
         case TIMEOUT_EXPIRES:
           newState = CxDxSessionState.TIMEDOUT;
           break;
 
         case SEND_MESSAGE:
           super.session.send(((AppEvent) event.getData()).getMessage(), this);
           newState = CxDxSessionState.TERMINATED;
           break;
 
         case RECEIVE_PPA:
           newState = CxDxSessionState.TERMINATED;
           super.cancelMsgTimer();
          listener.doPushProfileAnswer(this,  null, (JPushProfileAnswer)event.getData());
           break;
 
         case RECEIVE_RTA:
           newState = CxDxSessionState.TERMINATED;
           super.cancelMsgTimer();
          listener.doRegistrationTerminationAnswer(this,  null,(JRegistrationTerminationAnswer) event.getData());
           break;
 
         default:
           throw new InternalException("Should not receive more messages after initial. Command: " + event.getData());
         }
         break;
 
       case TERMINATED:
         throw new InternalException("Cant receive message in state TERMINATED. Command: " + event.getData());
 
       case TIMEDOUT:
         throw new InternalException("Cant receive message in state TIMEDOUT. Command: " + event.getData());
 
       default:
         logger.error("Cx/Dx Server FSM in wrong state: {}", state);
         break;
       }
 
       if (newState != null && newState != state) {
         setState(newState);
       }
     }
     catch (Exception e) {
       throw new InternalException(e);
     }
     finally {
       sendAndStateLock.unlock();
     }
 
     return true;
   }
 
   /*
    * (non-Javadoc)
    * 
    * @see
    * org.jdiameter.api.EventListener#receivedSuccessMessage(org.jdiameter.
    * api.Message, org.jdiameter.api.Message)
    */
   public void receivedSuccessMessage(Request request, Answer answer) {
     AnswerDelivery rd = new AnswerDelivery();
     rd.session = this;
     rd.request = request;
     rd.answer = answer;
     super.scheduler.execute(rd);
   }
 
   /*
    * (non-Javadoc)
    * @see org.jdiameter.api.EventListener#timeoutExpired(org.jdiameter.api.Message)
    */
   public void timeoutExpired(Request request) {
     try {
       handleEvent(new Event(Event.Type.TIMEOUT_EXPIRES, new AppRequestEventImpl(request), null));
     }
     catch (Exception e) {
       logger.debug("Failed to process timeout message", e);
     }
   }
 
   /*
    * (non-Javadoc)
    * @see org.jdiameter.api.NetworkReqListener#processRequest(org.jdiameter.api.Request)
    */
   public Answer processRequest(Request request) {
     RequestDelivery rd = new RequestDelivery();
     rd.session = this;
     rd.request = request;
     super.scheduler.execute(rd);
     return null;
   }
 
   protected void send(Event.Type type, AppEvent request, AppEvent answer) throws InternalException {
     try {
       if (type != null) {
         handleEvent(new Event(type, request, answer));
       }
     }
     catch (Exception e) {
       throw new InternalException(e);
     }
   }
 
   @SuppressWarnings("unchecked")
   protected void setState(CxDxSessionState newState) {
     CxDxSessionState oldState = this.sessionData.getCxDxSessionState();
     this.sessionData.setCxDxSessionState(newState);
 
     for (StateChangeListener i : stateListeners) {
       i.stateChanged(this,(Enum) oldState, (Enum) newState);
     }
     if (newState == CxDxSessionState.TERMINATED || newState == CxDxSessionState.TIMEDOUT) {
       super.cancelMsgTimer();
       this.release();
     }
   }
 
   @Override
   public void onTimer(String timerName) {
     if(timerName.equals(CxDxSession.TIMER_NAME_MSG_TIMEOUT)) {
       try{
         sendAndStateLock.lock();
         try {
           handleEvent(new Event(Event.Type.TIMEOUT_EXPIRES, new AppRequestEventImpl(this.sessionData.getBuffer()), null));
         }
         catch (Exception e) {
           logger.debug("Failure handling Timeout event.");
         }
         this.sessionData.setBuffer(null);
         this.sessionData.setTsTimerId(null);
       }
       finally {
         sendAndStateLock.unlock();
       }
     }
   }
 
   private class RequestDelivery implements Runnable {
     ServerCxDxSession session;
     Request request;
 
     public void run() {
 
       try {
         switch (request.getCommandCode()) {
        case JUserAuthorizationAnswer.code:
           handleEvent(new Event(Event.Type.RECEIVE_UAR, messageFactory.createUserAuthorizationRequest(request), null));
           break;
 
        case JServerAssignmentAnswer.code:
           handleEvent(new Event(Event.Type.RECEIVE_SAR, messageFactory.createServerAssignmentRequest(request), null));
           break;
 
        case JMultimediaAuthAnswer.code:
           handleEvent(new Event(Event.Type.RECEIVE_MAR, messageFactory.createMultimediaAuthRequest(request), null));
           break;
 
        case JLocationInfoAnswer.code:
           handleEvent(new Event(Event.Type.RECEIVE_LIR, messageFactory.createLocationInfoRequest(request), null));
           break;
 
         default:
           listener.doOtherEvent(session, new AppRequestEventImpl(request), null);
           break;
         }
       }
       catch (Exception e) {
         logger.debug("Failed to process request message", e);
       }
     }
   }
 
   private class AnswerDelivery implements Runnable {
     ServerCxDxSession session;
     Answer answer;
     Request request;
 
     public void run() {
       try{
         switch (answer.getCommandCode()) {
 
         case JPushProfileAnswer.code:
          handleEvent(new Event(Event.Type.RECEIVE_PPA, null, messageFactory.createPushProfileAnswer(answer)));
           break;
 
         case JRegistrationTerminationAnswer.code:
          handleEvent(new Event(Event.Type.RECEIVE_RTA, null, messageFactory.createRegistrationTerminationAnswer(answer)));
           break;
 
         default:
           listener.doOtherEvent(session, new AppRequestEventImpl(request), new AppAnswerEventImpl(answer));
           break;
         }
       }
       catch (Exception e) {
         logger.debug("Failed to process success message", e);
       }
     }
   }
 
 }
