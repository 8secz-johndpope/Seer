 /*
  * This is free software; you can redistribute it and/or modify it
  * under the terms of the GNU Lesser General Public License as
  * published by the Free Software Foundation; either version 2.1 of
  * the License, or (at your option) any later version.
  *
  * This software is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
  * Lesser General Public License for more details.
  *
  * You should have received a copy of the GNU Lesser General Public
  * License along with this software; if not, write to the Free
  * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
  * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
  */
 package org.mobicents.servlet.sip.message;
 
 import gov.nist.javax.sip.header.ims.PathHeader;
 
 import java.text.ParseException;
 import java.util.ArrayList;
 import java.util.HashSet;
 import java.util.List;
 import java.util.Map;
 import java.util.concurrent.ConcurrentHashMap;
 
 import javax.servlet.sip.B2buaHelper;
 import javax.servlet.sip.SipServletMessage;
 import javax.servlet.sip.SipServletRequest;
 import javax.servlet.sip.SipServletResponse;
 import javax.servlet.sip.SipSession;
 import javax.servlet.sip.TooManyHopsException;
 import javax.servlet.sip.UAMode;
 import javax.servlet.sip.SipSession.State;
 import javax.servlet.sip.ar.SipApplicationRoutingDirective;
 import javax.sip.ClientTransaction;
 import javax.sip.ListeningPoint;
 import javax.sip.ServerTransaction;
 import javax.sip.Transaction;
 import javax.sip.header.CSeqHeader;
 import javax.sip.header.CallIdHeader;
 import javax.sip.header.ContactHeader;
 import javax.sip.header.ContentDispositionHeader;
 import javax.sip.header.ContentLengthHeader;
 import javax.sip.header.ContentTypeHeader;
 import javax.sip.header.FromHeader;
 import javax.sip.header.Header;
 import javax.sip.header.MaxForwardsHeader;
 import javax.sip.header.RecordRouteHeader;
 import javax.sip.header.RouteHeader;
 import javax.sip.header.ToHeader;
 import javax.sip.header.ViaHeader;
 import javax.sip.message.Request;
 
 import org.apache.commons.logging.Log;
 import org.apache.commons.logging.LogFactory;
 import org.mobicents.servlet.sip.JainSipUtils;
 import org.mobicents.servlet.sip.SipFactories;
 import org.mobicents.servlet.sip.core.ApplicationRoutingHeaderComposer;
 import org.mobicents.servlet.sip.core.ExtendedListeningPoint;
 import org.mobicents.servlet.sip.core.RoutingState;
 import org.mobicents.servlet.sip.core.SipApplicationDispatcher;
 import org.mobicents.servlet.sip.core.session.MobicentsSipApplicationSession;
 import org.mobicents.servlet.sip.core.session.MobicentsSipSession;
 import org.mobicents.servlet.sip.core.session.SessionManagerUtil;
 import org.mobicents.servlet.sip.core.session.SipManager;
 import org.mobicents.servlet.sip.core.session.SipSessionKey;
 
 /**
  * Implementation of the B2BUA helper class.
  * 
  * 
  * @author mranga
  * @author Jean Deruelle
  */
 
 public class B2buaHelperImpl implements B2buaHelper {
 	private static Log logger = LogFactory.getLog(B2buaHelperImpl.class);
 	
 	protected static final HashSet<String> singletonHeadersNames = new HashSet<String>();
 	static {
 		singletonHeadersNames.add(FromHeader.NAME);
 		singletonHeadersNames.add(ToHeader.NAME);
 		singletonHeadersNames.add(CSeqHeader.NAME);
 		singletonHeadersNames.add(CallIdHeader.NAME);
 		singletonHeadersNames.add(MaxForwardsHeader.NAME);
 		singletonHeadersNames.add(ContentLengthHeader.NAME);		
 		singletonHeadersNames.add(ContentDispositionHeader.NAME);
 		singletonHeadersNames.add(ContentTypeHeader.NAME);
 		//TODO are there any other singleton headers ?
 	}	
 	
 	protected static final HashSet<String> b2buaSystemHeaders = new HashSet<String>();
 	static {
 
 		b2buaSystemHeaders.add(CallIdHeader.NAME);
 		b2buaSystemHeaders.add(CSeqHeader.NAME);
 		b2buaSystemHeaders.add(ViaHeader.NAME);
 		b2buaSystemHeaders.add(RouteHeader.NAME);
 		b2buaSystemHeaders.add(RecordRouteHeader.NAME);
 		b2buaSystemHeaders.add(PathHeader.NAME);
 	}
 	
 	//FIXME @jean.deruelle session map is never cleaned up => could lead to memory leak
 	//shall we have a thread scanning for invalid sessions and removing them accordingly ?
 	private Map<MobicentsSipSession, MobicentsSipSession> sessionMap = new ConcurrentHashMap<MobicentsSipSession, MobicentsSipSession>();
 	//Map to handle responses to original request and cancel on original request
 	private Map<String, SipServletRequest> originalRequestMap = new ConcurrentHashMap<String, SipServletRequest>();
 
 	private SipFactoryImpl sipFactoryImpl;
 
 	private SipServletRequestImpl sipServletRequest;		
 	
 	/**
 	 * 
 	 * @param sipFactoryImpl
 	 */
 	public B2buaHelperImpl(SipServletRequestImpl sipServletRequest) {
 		this.sipServletRequest = sipServletRequest;
 		this.sipFactoryImpl = sipServletRequest.sipFactoryImpl; 
 	}
 
 	/*
 	 * (non-Javadoc)
 	 * @see javax.servlet.sip.B2buaHelper#createRequest(javax.servlet.sip.SipServletRequest, boolean, java.util.Map)
 	 */
 	public SipServletRequest createRequest(SipServletRequest origRequest,
 			boolean linked, Map<String, List<String>> headerMap) throws TooManyHopsException {
 		
 		if(origRequest == null) {
 			throw new NullPointerException("original request cannot be null");
 		}
 		
 		if(origRequest.getMaxForwards() == 0) {
 			throw new TooManyHopsException();
 		}
 		
 		try {
 			SipServletRequestImpl origRequestImpl = (SipServletRequestImpl) origRequest;
 			Request newRequest = (Request) origRequestImpl.message.clone();
 			//content should be copied too, so commented out
 //		 	newRequest.removeContent();				
 			//removing the via header from original request
 			newRequest.removeHeader(ViaHeader.NAME);	
 			
 			((FromHeader) newRequest.getHeader(FromHeader.NAME))
 					.removeParameter("tag");
 			((ToHeader) newRequest.getHeader(ToHeader.NAME))
 					.removeParameter("tag");
 			// Remove the route header ( will point to us ).
 			newRequest.removeHeader(RouteHeader.NAME);
 			String tag = Integer.toString((int) (Math.random()*1000));
 			((FromHeader) newRequest.getHeader(FromHeader.NAME)).setParameter("tag", tag);
 			
 			// Remove the record route headers. This is a new call leg.
 			newRequest.removeHeader(RecordRouteHeader.NAME);
 			
 			//For non-REGISTER requests, the Contact header field is not copied 
 			//but is populated by the container as usualB2buaHelperImpl
 			if(!Request.REGISTER.equalsIgnoreCase(origRequest.getMethod())) {
 				newRequest.removeHeader(ContactHeader.NAME);
 			}
 
 			List<String> contactHeaderSet = retrieveContactHeaders(headerMap,
 					newRequest);			
 			MobicentsSipSession originalSession = origRequestImpl.getSipSession();
 			MobicentsSipApplicationSession appSession = originalSession
 					.getSipApplicationSession();	
 			
 			FromHeader newFromHeader = (FromHeader) newRequest.getHeader(FromHeader.NAME);
 			FromHeader oldFromHeader = (FromHeader) origRequestImpl.getMessage().getHeader(FromHeader.NAME);
 			
 			SipApplicationDispatcher dispatcher = (SipApplicationDispatcher) appSession.getSipContext().getSipApplicationDispatcher();
 			ApplicationRoutingHeaderComposer stack = new ApplicationRoutingHeaderComposer(
 					dispatcher.getMdToApplicationName(), oldFromHeader.getTag());
 			stack.addNode(new ApplicationRoutingHeaderComposer.ApplicationRouterNode(
 					originalSession.getKey().getApplicationName()));
 			newFromHeader.setTag(stack.toString());
 			
 			SipSessionKey key = SessionManagerUtil.getSipSessionKey(originalSession.getKey().getApplicationName(), newRequest, false);
 			MobicentsSipSession session = appSession.getSipContext().getSipManager().getSipSession(key, true, sipFactoryImpl, appSession);			
 			session.setHandler(originalSession.getHandler());
 		
 			SipServletRequestImpl newSipServletRequest = new SipServletRequestImpl(
 					newRequest,
 					sipFactoryImpl,					
 					session, 
 					null, 
 					null, 
 					JainSipUtils.dialogCreatingMethods.contains(newRequest.getMethod()));			
 			//JSR 289 Section 15.1.6
 			newSipServletRequest.setRoutingDirective(SipApplicationRoutingDirective.CONTINUE, origRequest);			
 			//If Contact header is present in the headerMap 
 			//then relevant portions of Contact header is to be used in the request created, 
 			//in accordance with section 4.1.3 of the specification.
 			for (String contactHeaderValue : contactHeaderSet) {
 				newSipServletRequest.addHeaderInternal(ContactHeader.NAME, contactHeaderValue, true);
 			}
 			
 			originalRequestMap.put(originalSession.getId(), origRequest);
 			originalRequestMap.put(session.getId(), newSipServletRequest);
 			
 			if (linked) {
 				sessionMap.put(originalSession, session);
 				sessionMap.put(session, originalSession);				
 			}
 			newSipServletRequest.setB2buaHelper(this);
 			return newSipServletRequest;
 		} catch (Exception ex) {
 			logger.error("Unexpected exception ", ex);
 			throw new IllegalArgumentException(
 					"Illegal arg ecnountered while creatigng b2bua", ex);
 		}
 	}
 
 	/*
 	 * (non-Javadoc)
 	 * @see javax.servlet.sip.B2buaHelper#createRequest(javax.servlet.sip.SipSession, javax.servlet.sip.SipServletRequest, java.util.Map)
 	 */
 	public SipServletRequest createRequest(SipSession session,
 			SipServletRequest origRequest, Map<String, List<String>> headerMap) {
 		
 		if(origRequest == null) {
 			throw new NullPointerException("original request cannot be null");
 		}
 		
 		try {
 			SipServletRequestImpl origRequestImpl = (SipServletRequestImpl) origRequest;
 			MobicentsSipSession originalSession = origRequestImpl.getSipSession();
 			MobicentsSipSession sessionImpl = (MobicentsSipSession) session;
 
 			SipServletRequestImpl newSubsequentServletRequest = (SipServletRequestImpl) session.createRequest(origRequest.getMethod());
 			
 			
 			//For non-REGISTER requests, the Contact header field is not copied 
 			//but is populated by the container as usual
 			if(!Request.REGISTER.equalsIgnoreCase(origRequest.getMethod())) {
 				newSubsequentServletRequest.getMessage().removeHeader(ContactHeader.NAME);
 			}
 			//If Contact header is present in the headerMap 
 			//then relevant portions of Contact header is to be used in the request created, 
 			//in accordance with section 4.1.3 of the specification.
 			//They will be added later after the sip servlet request has been created
 			List<String> contactHeaderSet = retrieveContactHeaders(headerMap,
 					(Request) newSubsequentServletRequest.getMessage());
 						
 			//If Contact header is present in the headerMap 
 			//then relevant portions of Contact header is to be used in the request created, 
 			//in accordance with section 4.1.3 of the specification.
 			for (String contactHeaderValue : contactHeaderSet) {
 				newSubsequentServletRequest.addHeaderInternal(ContactHeader.NAME, contactHeaderValue, true);
 			}
 			
 			if(logger.isDebugEnabled()) {
 				logger.debug("newSubsequentServletRequest = " + newSubsequentServletRequest);
 			}			
 			
 			originalRequestMap.put(originalSession.getId(), origRequest);
 			originalRequestMap.put(session.getId(), newSubsequentServletRequest);
 			
 			sessionMap.put(originalSession, sessionImpl);
 			sessionMap.put(sessionImpl, originalSession);
 
 			newSubsequentServletRequest.setB2buaHelper(this);
 			return newSubsequentServletRequest;
 		} catch (Exception ex) {
 			logger.error("Unexpected exception ", ex);
 			throw new IllegalArgumentException(
 					"Illegal arg ecnountered while creatigng b2bua", ex);
 		}
 	}
 
 	/**
 	 * @param headerMap
 	 * @param newRequest
 	 * @return
 	 * @throws ParseException
 	 */
 	private static List<String> retrieveContactHeaders(
 			Map<String, List<String>> headerMap, Request newRequest)
 			throws ParseException {
 		List<String> contactHeaderList = new ArrayList<String>();
 		if(headerMap != null) {
 			for (String headerName : headerMap.keySet()) {
 				if(!headerName.equalsIgnoreCase(ContactHeader.NAME)) {
 					if(b2buaSystemHeaders.contains(headerName)) {
 						throw new IllegalArgumentException(headerName + " in the provided map is a system header");
 					}
 					for (String value : headerMap.get(headerName)) {							
 						Header header = SipFactories.headerFactory.createHeader(
 								headerName, value);					
 						if(! singletonHeadersNames.contains(header.getName())) {
 							newRequest.addHeader(header);
 						} else {
 							newRequest.setHeader(header);
 						}
 					}
 				} else {
 					contactHeaderList = headerMap.get(headerName);
 				}
 			}
 		}
 		return contactHeaderList;
 	}
 
 	/*
 	 * (non-Javadoc)
 	 * @see javax.servlet.sip.B2buaHelper#createResponseToOriginalRequest(javax.servlet.sip.SipSession, int, java.lang.String)
 	 */
 	public SipServletResponse createResponseToOriginalRequest(
 			SipSession session, int status, String reasonPhrase) {
 
 		if (session == null) {
 			throw new NullPointerException("Null arg");
 		}
 		if(!session.isValid()) {
 			throw new IllegalArgumentException("session is invalid !");
 		}
 		
 		MobicentsSipSession sipSession = (MobicentsSipSession) session;
 
 		
 		Transaction trans = sipSession.getSessionCreatingTransaction();				
 		TransactionApplicationData appData = (TransactionApplicationData) trans.getApplicationData();
 		
 		SipServletRequestImpl sipServletRequestImpl = (SipServletRequestImpl) appData.getSipServletMessage();
 		if(RoutingState.FINAL_RESPONSE_SENT.equals(sipServletRequestImpl.getRoutingState())) {
 			throw new IllegalStateException("subsequent response is inconsistent with an already sent response. a Final response has already been sent ! ");
 		}
 		return sipServletRequestImpl.createResponse(status, reasonPhrase);
 	}
 	
 	/*
 	 * (non-Javadoc)
 	 * @see javax.servlet.sip.B2buaHelper#getLinkedSession(javax.servlet.sip.SipSession)
 	 */
 	public SipSession getLinkedSession(SipSession session) {
 		if ( session == null) { 
 			throw new NullPointerException("the argument is null");
 		}
 		if(!session.isValid()) {
 			throw new IllegalArgumentException("the session is invalid");
 		}
 		MobicentsSipSession linkedSession = this.sessionMap.get((MobicentsSipSession) session );
 		if(logger.isDebugEnabled()) {
 			logger.debug("Linked Session found : " + linkedSession + " for this session " + session);
 		}
 		return linkedSession;
 		
 	}
 
 	/*
 	 * (non-Javadoc)
 	 * @see javax.servlet.sip.B2buaHelper#getLinkedSipServletRequest(javax.servlet.sip.SipServletRequest)
 	 */
 	public SipServletRequest getLinkedSipServletRequest(SipServletRequest req) {
 		if ( req == null) { 
 			throw new NullPointerException("the argument is null");
 		}
 		MobicentsSipSession linkedSipSession = sessionMap.get(req.getSession());
 		SipServletRequest linkedSipServletRequest = originalRequestMap.get(linkedSipSession.getId());
 		return linkedSipServletRequest;
 	}
 	
 	/*
 	 * (non-Javadoc)
 	 * @see javax.servlet.sip.B2buaHelper#getPendingMessages(javax.servlet.sip.SipSession, javax.servlet.sip.UAMode)
 	 */
 	public List<SipServletMessage> getPendingMessages(SipSession session,
 			UAMode mode) {
 		if(!session.isValid()) {
 			throw new IllegalArgumentException("the session is invalid!");
 		}
 		MobicentsSipSession sipSessionImpl = (MobicentsSipSession) session;
 		List<SipServletMessage> retval = new ArrayList<SipServletMessage> ();
		if ( mode.equals(UAMode.UAC)) {
 			for ( Transaction transaction: sipSessionImpl.getOngoingTransactions()) {
 				if ( transaction instanceof ClientTransaction) {
 					TransactionApplicationData tad = (TransactionApplicationData) transaction.getApplicationData();
 					SipServletMessage sipServletMessage = tad.getSipServletMessage();
					if (!sipServletMessage.isCommitted()) {
 						retval.add(sipServletMessage);
 					}
 					for(SipServletResponseImpl sipServletResponseImpl : tad.getSipServletResponses()) {
 						if (!sipServletResponseImpl.isCommitted()) {
 							retval.add(sipServletResponseImpl);
 						}
 					}
 				}
 			}
 			
 		} else {
 			for ( Transaction transaction: sipSessionImpl.getOngoingTransactions()) {
 				if ( transaction instanceof ServerTransaction) {
 					TransactionApplicationData tad = (TransactionApplicationData) transaction.getApplicationData();
 					SipServletMessage sipServletMessage = tad.getSipServletMessage();
					if (!sipServletMessage.isCommitted()) {
 						retval.add(sipServletMessage);
 					}
 				}
 			}
 		}
 		return retval;
 	}
 	
 	/*
 	 * (non-Javadoc)
 	 * @see javax.servlet.sip.B2buaHelper#linkSipSessions(javax.servlet.sip.SipSession, javax.servlet.sip.SipSession)
 	 */
 	public void linkSipSessions(SipSession session1, SipSession session2) {
 		if ( session1 == null) {
 			throw new NullPointerException("First argument is null");
 		}
 		if ( session2 == null) {
 			throw new NullPointerException("Second argument is null");
 		}
 		
 		if(!session1.isValid() || !session2.isValid() || 
 				State.TERMINATED.equals(((MobicentsSipSession)session1).getState()) ||
 				State.TERMINATED.equals(((MobicentsSipSession)session2).getState()) ||
 				!session1.getApplicationSession().equals(session2.getApplicationSession()) ||
 				sessionMap.get(session1) != null ||
 				sessionMap.get(session2) != null) {
 			throw new IllegalArgumentException("either of the specified sessions has been terminated " +
 					"or the sessions do not belong to the same application session or " +
 					"one or both the sessions are already linked with some other session(s)");
 		}
 		this.sessionMap.put((MobicentsSipSession)session1, (MobicentsSipSession)session2);
 		this.sessionMap.put((MobicentsSipSession) session2, (MobicentsSipSession) session1);
 
 	}
 	
 	/*
 	 * (non-Javadoc)
 	 * @see javax.servlet.sip.B2buaHelper#unlinkSipSessions(javax.servlet.sip.SipSession)
 	 */
 	public void unlinkSipSessions(SipSession session) {		
 		if ( session == null) { 
 			throw new NullPointerException("the argument is null");
 		}
 		if(!session.isValid() || 
 				State.TERMINATED.equals(((MobicentsSipSession)session).getState()) ||
 				sessionMap.get(session) == null) {
 			throw new IllegalArgumentException("the session is not currently linked to another session or it has been terminated");
 		}
 		MobicentsSipSession key = (MobicentsSipSession) session;
 		MobicentsSipSession value  = this.sessionMap.get(key);
 		if (value != null) {
 			this.sessionMap.remove(value);
 		}
 		this.sessionMap.remove(key);
 	}
 	
 	/**
 	 * {@inheritDoc}
 	 */
 	public SipServletRequest createRequest(SipServletRequest origRequest) {
 		try {
 			SipServletRequestImpl origRequestImpl = (SipServletRequestImpl) origRequest;
 			Request newRequest = (Request) origRequestImpl.message.clone();
 			//removing the via header from original request
 			newRequest.removeHeader(ViaHeader.NAME);	
 			
 			//assign a new from tag
 			((FromHeader) newRequest.getHeader(FromHeader.NAME))
 					.removeParameter("tag");
 			//remove the to tag
 			((ToHeader) newRequest.getHeader(ToHeader.NAME))
 					.removeParameter("tag");
 			// Remove the route header ( will point to us ).
 			newRequest.removeHeader(RouteHeader.NAME);
 			
 			// Remove the record route headers. This is a new call leg.
 			newRequest.removeHeader(RecordRouteHeader.NAME);
 			
 			//For non-REGISTER requests, the Contact header field is not copied 
 			//but is populated by the container as usual
 			if(!Request.REGISTER.equalsIgnoreCase(origRequest.getMethod())) {
 				newRequest.removeHeader(ContactHeader.NAME);
 			}
 			//Creating new call id
 			ExtendedListeningPoint extendedListeningPoint = sipFactoryImpl.getSipNetworkInterfaceManager().findMatchingListeningPoint(ListeningPoint.UDP, false);
 			CallIdHeader callIdHeader = SipFactories.headerFactory.createCallIdHeader(extendedListeningPoint.getSipProvider().getNewCallId().getCallId());
 			newRequest.setHeader(callIdHeader);
 			
 			MobicentsSipSession originalSession = origRequestImpl.getSipSession();
 			MobicentsSipApplicationSession originalAppSession = originalSession
 					.getSipApplicationSession();				
 			
 			FromHeader newFromHeader = (FromHeader) newRequest.getHeader(FromHeader.NAME);
 			FromHeader oldFromHeader = (FromHeader) origRequestImpl.getMessage().getHeader(FromHeader.NAME);
 			
 			SipApplicationDispatcher dispatcher = (SipApplicationDispatcher) originalAppSession.getSipContext().getSipApplicationDispatcher();
 			ApplicationRoutingHeaderComposer stack = new ApplicationRoutingHeaderComposer(
 					dispatcher.getMdToApplicationName(), oldFromHeader.getTag());
 			stack.addNode(new ApplicationRoutingHeaderComposer.ApplicationRouterNode(
 					originalSession.getKey().getApplicationName()));
 			newFromHeader.setTag(stack.toString());
 			
 			SipSessionKey key = SessionManagerUtil.getSipSessionKey(originalSession.getKey().getApplicationName(), newRequest, false);
 			MobicentsSipSession session = ((SipManager)originalAppSession.getSipContext().getManager()).getSipSession(key, true, sipFactoryImpl, originalAppSession);			
 			session.setHandler(originalSession.getHandler());
 			
 			SipServletRequestImpl newSipServletRequest = new SipServletRequestImpl(
 					newRequest,
 					sipFactoryImpl,					
 					session, 
 					null, 
 					null, 
 					JainSipUtils.dialogCreatingMethods.contains(newRequest.getMethod()));			
 			//JSR 289 Section 15.1.6
 			newSipServletRequest.setRoutingDirective(SipApplicationRoutingDirective.CONTINUE, origRequest);			
 			
 			sessionMap.put(originalSession, session);
 			sessionMap.put(session, originalSession);				
 
 			originalRequestMap.put(originalSession.getId(), origRequest);
 			originalRequestMap.put(session.getId(), newSipServletRequest);						
 			
 			newSipServletRequest.setB2buaHelper(this);
 			return newSipServletRequest;
 		} catch (Exception ex) {
 			logger.error("Unexpected exception ", ex);
 			throw new IllegalArgumentException(
 					"Illegal arg ecnountered while creatigng b2bua", ex);
 		}
 	}
 
 	/**
 	 * {@inheritDoc}
 	 */
 	public SipServletRequest createCancel(SipSession session) {
 		SipServletRequest sipServletRequest = originalRequestMap.get(session.getId());
 		
 		SipServletRequestImpl sipServletRequestImpl = (SipServletRequestImpl)sipServletRequest.createCancel();
 		sipServletRequestImpl.setB2buaHelper(this);
 		return sipServletRequestImpl;
 	}
 
 }
