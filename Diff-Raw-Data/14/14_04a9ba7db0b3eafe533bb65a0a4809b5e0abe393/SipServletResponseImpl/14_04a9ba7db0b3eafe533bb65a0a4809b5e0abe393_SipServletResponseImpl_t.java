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
 
 import java.io.IOException;
 import java.io.PrintWriter;
 import java.text.ParseException;
 import java.util.ArrayList;
 import java.util.Iterator;
 import java.util.List;
 import java.util.ListIterator;
 import java.util.Locale;
 
 import javax.servlet.ServletOutputStream;
 import javax.servlet.sip.Proxy;
 import javax.servlet.sip.ProxyBranch;
 import javax.servlet.sip.Rel100Exception;
 import javax.servlet.sip.SipServletRequest;
 import javax.servlet.sip.SipServletResponse;
 import javax.sip.ClientTransaction;
 import javax.sip.Dialog;
 import javax.sip.InvalidArgumentException;
 import javax.sip.ServerTransaction;
 import javax.sip.SipException;
 import javax.sip.SipProvider;
 import javax.sip.Transaction;
 import javax.sip.TransactionState;
 import javax.sip.address.SipURI;
 import javax.sip.header.CSeqHeader;
 import javax.sip.header.ContactHeader;
 import javax.sip.header.ContentEncodingHeader;
 import javax.sip.header.ProxyAuthenticateHeader;
 import javax.sip.header.RecordRouteHeader;
 import javax.sip.header.RouteHeader;
 import javax.sip.header.ViaHeader;
 import javax.sip.header.WWWAuthenticateHeader;
 import javax.sip.message.Request;
 import javax.sip.message.Response;
 
 import org.apache.commons.logging.Log;
 import org.apache.commons.logging.LogFactory;
 import org.mobicents.servlet.sip.JainSipUtils;
 import org.mobicents.servlet.sip.SipFactories;
 import org.mobicents.servlet.sip.address.RFC2396UrlDecoder;
 import org.mobicents.servlet.sip.core.RoutingState;
 import org.mobicents.servlet.sip.core.dispatchers.MessageDispatcher;
 import org.mobicents.servlet.sip.core.session.MobicentsSipSession;
 
 /**
  * Implementation of the sip servlet response interface
  *
  */
 public class SipServletResponseImpl extends SipServletMessageImpl implements
 		SipServletResponse {
 	private static Log logger =  LogFactory.getLog(SipServletResponseImpl.class);
 	
 	Response response;
 	SipServletRequestImpl originalRequest;
 	ProxyBranch proxyBranch;
 
 	private boolean isProxiedResponse;
 	private boolean isResponseForwardedUpstream;
 	private boolean isAckGenerated;
 	
 	/**
 	 * Constructor
 	 * @param response
 	 * @param sipFactoryImpl
 	 * @param transaction
 	 * @param session
 	 * @param dialog
 	 * @param originalRequest
 	 */
 	public SipServletResponseImpl (
 			Response response, 
 			SipFactoryImpl sipFactoryImpl, 
 			Transaction transaction, 
 			MobicentsSipSession session, 
 			Dialog dialog) {
 		
 		super(response, sipFactoryImpl, transaction, session, dialog);
 		this.response = response;	
 		setProxiedResponse(false);
 		isResponseForwardedUpstream = false;
 		isAckGenerated = false;
 	}
 	
 	/**
 	 * @return the response
 	 */
 	public Response getResponse() {
 		return response;
 	}
 	
 	@Override
 	public boolean isSystemHeader(String headerName) {
 		String hName = getFullHeaderName(headerName);
 
 		/*
 		 * Contact is a system header field in messages other than REGISTER
 		 * requests and responses, 3xx and 485 responses, and 200/OPTIONS
 		 * responses.
 		 */
 
 		// This doesnt contain contact!!!!
 		boolean isSystemHeader = systemHeaders.contains(hName);
 
 		if (isSystemHeader) {
 			return isSystemHeader;
 		}
 
 		boolean isContactSystem = false;
 		Response sipResponse = (Response) this.message;
 
 		String method = ((CSeqHeader) sipResponse.getHeader(CSeqHeader.NAME))
 				.getMethod();
 		//Killer condition, see comment above for meaning
 		if (method.equals(Request.REGISTER)
 				|| (Response.MULTIPLE_CHOICES <= sipResponse.getStatusCode() && sipResponse.getStatusCode() < Response.BAD_REQUEST)
 				|| sipResponse.getStatusCode() == Response.AMBIGUOUS
 				|| (sipResponse.getStatusCode() == Response.OK && method.equals(Request.OPTIONS))) {
 			isContactSystem = false;
 		} else {
 			isContactSystem = true;
 		}
 
 		if (isContactSystem && hName.equals(ContactHeader.NAME)) {
 			isSystemHeader = true;
 		} else {
 			isSystemHeader = false;
 		}
 
 		return isSystemHeader;
 	}
 	
 	/*
 	 * (non-Javadoc)
 	 * @see javax.servlet.sip.SipServletResponse#createAck()
 	 */
 	@SuppressWarnings("unchecked")
 	public SipServletRequest createAck() {
 		if(!Request.INVITE.equals(getTransaction().getRequest().getMethod()) || (response.getStatusCode() >= 100 && response.getStatusCode() < 200) || isAckGenerated) {
 			throw new IllegalStateException("the transaction state is such that it doesn't allow an ACK to be sent now, e.g. the original request was not an INVITE, or this response is provisional only, or an ACK has already been generated");
 		}
 		Dialog dialog = super.session.getSessionCreatingDialog();
 		CSeqHeader cSeqHeader = (CSeqHeader)response.getHeader(CSeqHeader.NAME);
 		SipServletRequestImpl sipServletAckRequest = null; 
 		try {
 			if(logger.isDebugEnabled()) {
 				logger.debug("dialog to create the ack Request " + dialog);
 			}
 			Request ackRequest = dialog.createAck(cSeqHeader.getSeqNumber());
 			if(logger.isInfoEnabled()) {
 				logger.info("ackRequest just created " + ackRequest);
 			}
 			//Application Routing to avoid going through the same app that created the ack
 			ListIterator<RouteHeader> routeHeaders = ackRequest.getHeaders(RouteHeader.NAME);
 			ackRequest.removeHeader(RouteHeader.NAME);
 			while (routeHeaders.hasNext()) {
 				RouteHeader routeHeader = routeHeaders.next();
 				String routeAppName = ((SipURI)routeHeader .getAddress().getURI()).
 					getParameter(MessageDispatcher.RR_PARAM_APPLICATION_NAME);
 				if(routeAppName == null || !routeAppName.equals(getSipSession().getKey().getApplicationName())) {
 					ackRequest.addHeader(routeHeader);
 				}
 			}
 			sipServletAckRequest = new SipServletRequestImpl(
 					ackRequest,
 					this.sipFactoryImpl, 
 					this.getSipSession(), 
 					this.getTransaction(), 
 					dialog, 
 					false); 
 			isAckGenerated = true;
 		} catch (InvalidArgumentException e) {
 			logger.error("Impossible to create the ACK",e);
 		} catch (SipException e) {
 			logger.error("Impossible to create the ACK",e);
 		}		
 		return sipServletAckRequest;
 	}
 
 	public SipServletRequest createPrack() {
 		// TODO Auto-generated method stub
 		return null;
 	}
 
 	/*
 	 * (non-Javadoc)
 	 * @see javax.servlet.sip.SipServletResponse#getOutputStream()
 	 */
 	public ServletOutputStream getOutputStream() throws IOException {
 		// Always return null
 		return null;
 	}
 
 	/*
 	 * (non-Javadoc)
 	 * @see javax.servlet.sip.SipServletResponse#getProxy()
 	 */
 	public Proxy getProxy() {
 		if(proxyBranch != null) {
 			return proxyBranch.getProxy();
 		} else {
 			return null;
 		}
 	}
 
 	/*
 	 * (non-Javadoc)
 	 * @see javax.servlet.sip.SipServletResponse#getReasonPhrase()
 	 */
 	public String getReasonPhrase() {
 		return response.getReasonPhrase();
 	}
 
 	/*
 	 * (non-Javadoc)
 	 * @see javax.servlet.sip.SipServletResponse#getRequest()
 	 */
 	public SipServletRequest getRequest() {		
 		return originalRequest;
 	}
 
 	/*
 	 * (non-Javadoc)
 	 * @see javax.servlet.sip.SipServletResponse#getStatus()
 	 */
 	public int getStatus() {
 		return response.getStatusCode();
 	}
 
 	/*
 	 * (non-Javadoc)
 	 * @see javax.servlet.sip.SipServletResponse#getWriter()
 	 */
 	public PrintWriter getWriter() throws IOException {
 		// Always returns null.
 		return null;
 	}
 
 	/*
 	 * (non-Javadoc)
 	 * @see javax.servlet.sip.SipServletResponse#sendReliably()
 	 */
 	public void sendReliably() throws Rel100Exception {
 		//FIXME add support for it
 		throw new Rel100Exception(Rel100Exception.NOT_SUPPORTED);
 	}
 	
 	/*
 	 * (non-Javadoc)
 	 * @see javax.servlet.sip.SipServletResponse#setStatus(int)
 	 */
 	public void setStatus(int statusCode) {
 		try {
 			response.setStatusCode(statusCode);
 		} catch (ParseException e) {
 			throw new IllegalArgumentException(e);
 		}
 
 	}
 
 	/*
 	 * (non-Javadoc)
 	 * @see javax.servlet.sip.SipServletResponse#setStatus(int, java.lang.String)
 	 */
 	public void setStatus(int statusCode, String reasonPhrase) {
 		try {
 			response.setStatusCode(statusCode);
 			response.setReasonPhrase(reasonPhrase);
 		} catch (ParseException e) {
 			throw new IllegalArgumentException(e);
 		}
 
 	}
 
 	/*
 	 * (non-Javadoc)
 	 * @see javax.servlet.ServletResponse#flushBuffer()
 	 */
 	public void flushBuffer() throws IOException {
 		// Do nothing
 	}
 
 	/*
 	 * (non-Javadoc)
 	 * @see javax.servlet.ServletResponse#getBufferSize()
 	 */
 	public int getBufferSize() {		
 		return 0;
 	}
 
 	public Locale getLocale() {
 		// TODO Auto-generated method stub
 		return null;
 	}
 
 	/*
 	 * (non-Javadoc)
 	 * @see javax.servlet.ServletResponse#reset()
 	 */
 	public void reset() {
 		// Do nothing
 	}
 
 	/*
 	 * (non-Javadoc)
 	 * @see javax.servlet.ServletResponse#resetBuffer()
 	 */
 	public void resetBuffer() {
 		// Do nothing
 	}
 
 	/*
 	 * (non-Javadoc)
 	 * @see javax.servlet.ServletResponse#setBufferSize(int)
 	 */
 	public void setBufferSize(int arg0) {
 		// Do nothing
 	}
 
 	public void setLocale(Locale arg0) {
 		// TODO Auto-generated method stub
 
 	}
 	
 	@Override
 	public void send()  {
 		if(isMessageSent) {
 			throw new IllegalStateException("message already sent");
 		}
 		try {			
 			//if this is a final response
 			if(response.getStatusCode() >= Response.TRYING && 
 					response.getStatusCode() <= Response.SESSION_NOT_ACCEPTABLE && session.getProxyBranch() == null) {
 				//Issue 112 fix by folsson: use the viaheader transport
 				String transport = null;
 				ViaHeader viaHeader = ((ViaHeader) originalRequest.getMessage().getHeader(ViaHeader.NAME));
 				if(viaHeader != null) {
 					transport = viaHeader.getTransport();
 				}
 				if(transport == null || transport.length() <1) {
 					transport = JainSipUtils.findTransport((Request)originalRequest.getMessage());
 				}			     
 				javax.sip.address.SipURI sipURI = JainSipUtils.createRecordRouteURI(
 						sipFactoryImpl.getSipNetworkInterfaceManager(), 
 						transport
 						);
 				sipURI.setParameter(MessageDispatcher.RR_PARAM_APPLICATION_NAME, session.getKey().getApplicationName());
 				sipURI.setParameter(MessageDispatcher.FINAL_RESPONSE, "true");
 				if(session.getSipApplicationSession().getKey().isAppGeneratedKey()) {
 					sipURI.setParameter(MessageDispatcher.GENERATED_APP_KEY, RFC2396UrlDecoder.encode(session.getSipApplicationSession().getKey().getId()));
 				}
 				sipURI.setLrParam();				
 				javax.sip.address.Address recordRouteAddress = 
 					SipFactories.addressFactory.createAddress(sipURI);
 				RecordRouteHeader recordRouteHeader = 
 					SipFactories.headerFactory.createRecordRouteHeader(recordRouteAddress);
 				response.addFirst(recordRouteHeader);
 			}
 			ServerTransaction st = (ServerTransaction) getTransaction();
 			// Update Session state
 			session.updateStateOnResponse(this, false);						
 						
 			if(logger.isInfoEnabled()) {
 				logger.info("sending response "+ this.message);
 			}
 			//if a response is sent for an initial request, it means that the application
 			//acted as an endpoint so a dialog must be created but only for dialog creating method
 			CSeqHeader cSeqHeader = (CSeqHeader)response.getHeader(CSeqHeader.NAME);
 			if(!Request.CANCEL.equals(originalRequest.getMethod())					
 					&& (RoutingState.INITIAL.equals(originalRequest.getRoutingState()) 
 							|| RoutingState.RELAYED.equals(originalRequest.getRoutingState())) 
 					&& getTransaction().getDialog() == null 
 					&& JainSipUtils.dialogCreatingMethods.contains(cSeqHeader.getMethod())) {					
 				String transport = JainSipUtils.findTransport(st.getRequest());
 				SipProvider sipProvider = sipFactoryImpl.getSipNetworkInterfaceManager().findMatchingListeningPoint(
 						transport, false).getSipProvider();
 				
 				Dialog dialog = null;
 				// This ensures that the dialog is not created in terms of JSIP when responses arrive from
 				// proxy branches, that eventually return error responses. When they return error response,
 				// some other branch must create the dialog, but this other ranch will have another tag.
 				// If this is not here, you will get tag mismatch from JSIP.
				if(this.getStatus() != Response.TRYING && this.proxyBranch == null) {
 					dialog = sipProvider.getNewDialog(this
 						.getTransaction());				
				} else 
				if(this.getStatus() < 200 && this.proxyBranch != null) {
					dialog = sipProvider.getNewDialog(this
							.getTransaction());
 				}
 				getSipSession().setSessionCreatingDialog(dialog);
 				if(logger.isDebugEnabled()) {
 					logger.debug("created following dialog since the application is acting as an endpoint " + dialog);
 				}
 			}
 			//keeping track of application data and transaction in the dialog
 			if(getTransaction().getDialog() != null) {
 				if(getTransaction().getDialog().getApplicationData() == null) {
 					getTransaction().getDialog().setApplicationData(
 							originalRequest.getTransactionApplicationData());	
 				}				
 				((TransactionApplicationData)getTransaction().getDialog().getApplicationData()).
 					setTransaction(getTransaction());
 			}
 			//specify that a final response has been sent for the request
 			//so that the application dispatcher knows it has to stop
 			//processing the request
 			if(response.getStatusCode() >= Response.OK && 
 					response.getStatusCode() <= Response.SESSION_NOT_ACCEPTABLE) {				
 				originalRequest.setRoutingState(RoutingState.FINAL_RESPONSE_SENT);				
 			}
 			
 			st.sendResponse( (Response)this.message );
 			isMessageSent = true;
 			if(isProxiedResponse) {
 				isResponseForwardedUpstream = true;
 			}
 			//updating the last accessed times 
 			getSipSession().access();
 			getSipSession().getSipApplicationSession().access();
 		} catch (Exception e) {			
 			logger.error("an exception occured when sending the response", e);
 			throw new IllegalStateException(e);
 		}
 	}
 
 	/*
 	 * (non-Javadoc)
 	 * @see javax.servlet.sip.SipServletResponse#getChallengeRealms()
 	 */
 	public Iterator<String> getChallengeRealms() {
 		List<String> realms = new ArrayList<String>();
 		if(response.getStatusCode() == SipServletResponse.SC_UNAUTHORIZED) {
 			WWWAuthenticateHeader authenticateHeader = (WWWAuthenticateHeader) 
 				response.getHeader(WWWAuthenticateHeader.NAME);
 			realms.add(authenticateHeader.getRealm());
 		} else if (response.getStatusCode() == SipServletResponse.SC_PROXY_AUTHENTICATION_REQUIRED) {
 			ProxyAuthenticateHeader authenticateHeader = (ProxyAuthenticateHeader) 
 				response.getHeader(ProxyAuthenticateHeader.NAME);
 			realms.add(authenticateHeader.getRealm());
 		}
 		return realms.iterator();
 	}
 
 
 	/**
 	 * {@inheritDoc}
 	 */
 	public ProxyBranch getProxyBranch() {
 		return proxyBranch;
 	}
 
 
 	/**
 	 * @param proxyBranch the proxyBranch to set
 	 */
 	public void setProxyBranch(ProxyBranch proxyBranch) {
 		this.proxyBranch = proxyBranch;
 	}
 
 
 	/**
 	 * {@inheritDoc}
 	 */
 	public boolean isBranchResponse() {		
 		return this.proxyBranch != null;
 	}
 
 	/*
 	 * (non-Javadoc)
 	 * @see javax.servlet.sip.SipServletMessage#isCommitted()
 	 */
 	public boolean isCommitted() {
 		//the message is an incoming non-reliable provisional response received by a servlet acting as a UAC
 		if(getTransaction() instanceof ClientTransaction && getStatus() >= 101 && getStatus() <= 199 && getHeader("RSeq") == null) {
 			if(this.proxyBranch == null) { // Make sure this is not a proxy. Proxies are allowed to modify headers.
 				return true;
 			} else {
 				return false;
 			}
 		}
 		//the message is an incoming reliable provisional response for which PRACK has already been generated. (Note that this scenario applies to containers that support the 100rel extension.)
 		if(getTransaction() instanceof ClientTransaction && getStatus() >= 101 && getStatus() <= 199 && getHeader("RSeq") != null && TransactionState.TERMINATED.equals(getTransaction().getState())) {
 			if(this.proxyBranch == null) { // Make sure this is not a proxy. Proxies are allowed to modify headers.
 				return true;
 			} else {
 				return false;
 			}
 		}
 		//the message is an incoming final response received by a servlet acting as a UAC for a Non INVITE transaction
 		if(getTransaction() instanceof ClientTransaction && getStatus() >= 200 && getStatus() <= 999 && !Request.INVITE.equals(getTransaction().getRequest().getMethod())) {
 			if(this.proxyBranch == null) { // Make sure this is not a proxy. Proxies are allowed to modify headers.
 				return true;
 			} else {
 				return false;
 			}
 		}
 		//the message is a response which has been forwarded upstream
 		if(isResponseForwardedUpstream) {
 			return true;
 		}
 		//message is an incoming final response to an INVITE transaction and an ACK has been generated
 		if(getTransaction() instanceof ClientTransaction && getStatus() >= 200 && getStatus() <= 999 && TransactionState.TERMINATED.equals(getTransaction().getState()) && isAckGenerated) {
 			return true;
 		}
 		return false;
 	}
 	
 	@Override
 	protected void checkMessageState() {
 		if(isMessageSent || getTransaction() instanceof ClientTransaction) {
 			throw new IllegalStateException("Message already sent or incoming message");
 		}
 	}
 
 	public void setOriginalRequest(SipServletRequestImpl originalRequest) {
 		this.originalRequest = originalRequest;
 	}
 
 	/**
 	 * @param isProxiedResponse the isProxiedResponse to set
 	 */
 	public void setProxiedResponse(boolean isProxiedResponse) {
 		this.isProxiedResponse = isProxiedResponse;
 	}
 
 	/**
 	 * @return the isProxiedResponse
 	 */
 	public boolean isProxiedResponse() {
 		return isProxiedResponse;
 	}
 	
 	/*
 	 * (non-Javadoc)
 	 * @see javax.servlet.ServletResponse#setCharacterEncoding(java.lang.String)
 	 */
 	public void setCharacterEncoding(String enc) {		
 		try {			
 			this.message.setContentEncoding(SipFactories.headerFactory
 					.createContentEncodingHeader(enc));
 		} catch (Exception ex) {
 			throw new IllegalArgumentException("Encoding " + enc + " not valid", ex);
 		}
 
 	}
 }
