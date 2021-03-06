 /**
  * 
  */
 package org.mobicents.servlet.sip.core;
 
 import java.io.IOException;
 import java.io.Serializable;
 import java.text.ParseException;
 import java.util.HashMap;
 import java.util.HashSet;
 import java.util.Map;
 import java.util.Set;
 
 import javax.servlet.Servlet;
 import javax.servlet.ServletException;
 import javax.servlet.sip.SipApplicationRouter;
 import javax.servlet.sip.SipApplicationRouterInfo;
 import javax.servlet.sip.SipApplicationRoutingDirective;
 import javax.servlet.sip.SipRouteModifier;
 import javax.servlet.sip.SipSession.State;
 import javax.sip.Dialog;
 import javax.sip.DialogState;
 import javax.sip.DialogTerminatedEvent;
 import javax.sip.IOExceptionEvent;
 import javax.sip.RequestEvent;
 import javax.sip.ResponseEvent;
 import javax.sip.ServerTransaction;
 import javax.sip.SipProvider;
 import javax.sip.TimeoutEvent;
 import javax.sip.Transaction;
 import javax.sip.TransactionAlreadyExistsException;
 import javax.sip.TransactionState;
 import javax.sip.TransactionTerminatedEvent;
 import javax.sip.TransactionUnavailableException;
 import javax.sip.address.Address;
 import javax.sip.header.RouteHeader;
 import javax.sip.message.Request;
 import javax.sip.message.Response;
 
 import org.apache.catalina.Container;
 import org.apache.catalina.LifecycleException;
 import org.apache.catalina.Wrapper;
 import org.apache.commons.logging.Log;
 import org.apache.commons.logging.LogFactory;
 import org.mobicents.servlet.sip.SipFactories;
 import org.mobicents.servlet.sip.core.session.SessionManager;
 import org.mobicents.servlet.sip.core.session.SipApplicationSessionImpl;
 import org.mobicents.servlet.sip.core.session.SipSessionImpl;
 import org.mobicents.servlet.sip.message.SipServletRequestImpl;
 import org.mobicents.servlet.sip.message.SipServletResponseImpl;
 import org.mobicents.servlet.sip.message.TransactionApplicationData;
 import org.mobicents.servlet.sip.proxy.ProxyBranchImpl;
 import org.mobicents.servlet.sip.startup.SipContext;
 
 /**
  * Implementation of the SipApplicationDispatcher interface.
  * Central point getting the sip messages from the different stacks for a Tomcat Service(Engine), 
  * translating jain sip SIP messages to sip servlets SIPmessages, calling the application router 
  * to know which app is interested in the messages and
  * dispatching them those sip applications interested in the messages. 
  */
 public class SipApplicationDispatcherImpl implements SipApplicationDispatcher {
 	//the logger
 	private static transient Log logger = LogFactory
 			.getLog(SipApplicationDispatcherImpl.class);
 	
 	private static Set<String> nonInitialSipRequestMethods = new HashSet<String>();
 	
 	static {
 		nonInitialSipRequestMethods.add("CANCEL");
 		nonInitialSipRequestMethods.add("BYE");
 		nonInitialSipRequestMethods.add("PRACK");
 		nonInitialSipRequestMethods.add("ACK");
 	};
 	
 	//the sip application router responsible for the routing logic of sip messages to
 	//sip servlet applications
 	private SipApplicationRouter sipApplicationRouter = null;
 	//map of applications deployed
 	private Map<String, SipContext> applicationDeployed = null;
 	//application chains
 //	private Map<String, SipContext> applicationChains = null;
 	
 	private SessionManager sessionManager = new SessionManager();
 	
 	/**
 	 * 
 	 */
 	public SipApplicationDispatcherImpl() {
 		applicationDeployed = new HashMap<String, SipContext>();
 	}
 	
 	/**
 	 * {@inheritDoc} 
 	 */
 	public void init(String sipApplicationRouterClassName) throws LifecycleException {
 		//load the sip application router from the class name specified in the server.xml file
 		//and initializes it
 		try {
 			sipApplicationRouter = (SipApplicationRouter)
 				Class.forName(sipApplicationRouterClassName).newInstance();
 		} catch (InstantiationException e) {
 			throw new LifecycleException("Impossible to load the Sip Application Router",e);
 		} catch (IllegalAccessException e) {
 			throw new LifecycleException("Impossible to load the Sip Application Router",e);
 		} catch (ClassNotFoundException e) {
 			throw new LifecycleException("Impossible to load the Sip Application Router",e);
 		} catch (ClassCastException e) {
 			throw new LifecycleException("Sip Application Router defined does not implement " + SipApplicationRouter.class.getName(),e);
 		}		
 		sipApplicationRouter.init();		
 	}
 	/**
 	 * {@inheritDoc}
 	 */
 	public void start() {
 		// TODO nothing to do here for now
 		
 	}
 	/**
 	 * {@inheritDoc}
 	 */
 	public void stop() {
 		sipApplicationRouter.destroy();		
 	}
 	
 	/**
 	 * {@inheritDoc}
 	 */
 	public void addSipApplication(String sipApplicationName, SipContext sipApplication) {
 		synchronized (applicationDeployed) {
 			applicationDeployed.put(sipApplicationName, sipApplication);	
 		}				
 		logger.info("the following sip servlet application has been added : " + sipApplicationName);
 	}
 	/**
 	 * {@inheritDoc}
 	 */
 	public SipContext removeSipApplication(String sipApplicationName) {
 		SipContext sipContext;
 		synchronized (applicationDeployed) {
 			sipContext = applicationDeployed.remove(sipApplicationName);
 		}
 		logger.info("the following sip servlet application has been removed : " + sipApplicationName);
 		return sipContext;
 	}
 	/**
 	 * {@inheritDoc}
 	 */	
 	public void processDialogTerminated(DialogTerminatedEvent arg0) {
 		// TODO Auto-generated method stub
 		
 	}
 	/**
 	 * {@inheritDoc}
 	 */
 	public void processIOException(IOExceptionEvent arg0) {
 		// TODO Auto-generated method stub
 		
 	}
 	/**
 	 * {@inheritDoc}
 	 */
 	public void processRequest(RequestEvent requestEvent) {
 		SipProvider sp = (SipProvider)requestEvent.getSource();
 		ServerTransaction transaction =  requestEvent.getServerTransaction();
 		Request request = requestEvent.getRequest();
 		logger.info("Got a request event "  + request.getMethod());
 		
 		if ( transaction == null ) {
 			try {
 				transaction = sp.getNewServerTransaction(request);
 			} catch ( TransactionUnavailableException tae) {
 				//TODO Create a 500 Internal server error and return it here.
 				return;
 			} catch ( TransactionAlreadyExistsException taex ) {
 				// Already processed this request so just return.
 				return;				
 			} 
 		}
 		SipSessionImpl session = sessionManager.getRequestSession(requestEvent,transaction);
 		
 		SipServletRequestImpl sipServletRequest = null;		
 		try {			
 			sipServletRequest = new SipServletRequestImpl(
 					request,
 					(SipProvider) requestEvent.getSource(),
 					session,
 					transaction,
 					session.getSessionCreatingDialog(),true);
 		}
 		catch(Exception e) {
 			throw new RuntimeException("Failure getting the transaction for current request", e);
 		}
 		
 		//check if the request is initial
 		boolean isInitialRequest = isInitialRequest(sipServletRequest, requestEvent.getDialog());		
 		sipServletRequest.setInitial(isInitialRequest);
 		
 		if(isInitialRequest) {
 			//15.4.1 Routing an Initial request Algorithm
 			
 			// Create new app session for initial requests. TODO: later you should check for SipApplicationKey annotated method in the servlet.
 			SipApplicationSessionImpl appSession = new SipApplicationSessionImpl();
 			session.setApplicationSession(appSession);
 			appSession.addSipSession(session);
 			
 			//get the request routing directive
 			SipApplicationRoutingDirective sipApplicationRoutingDirective = sipServletRequest.getRoutingDirective();
 			//get the state info associated with the request
 			Serializable stateInfo = null;
 			if(SipApplicationRoutingDirective.CONTINUE.equals(sipApplicationRoutingDirective) || 
 					SipApplicationRoutingDirective.REVERSE.equals(sipApplicationRoutingDirective)) {
 				//TODO get the original request : like directive where should it be retrieved ?
 
 			} 
 			
 			logger.info("Dispatching the request event");
 			// 15.4.1 Procedure : point 1
 			SipApplicationRouterInfo applicationRouterInfo = 
 				sipApplicationRouter.getNextApplication(sipServletRequest, null, sipApplicationRoutingDirective, stateInfo);
 			// 15.4.1 Procedure : point 2
 			SipRouteModifier sipRouteModifier = applicationRouterInfo.getRouteModifier();
 			if(sipRouteModifier != null) {
 				switch(sipRouteModifier) {
 					case ROUTE :
 						String route = applicationRouterInfo.getRoute();
 						//TODO : check if the route is external
 						boolean isExternal = false;
 						if(isExternal) {
 							// TODO push the route as the top Route header field value and send the request externally
 	//						sipServletRequest.addHeader(RouteHeader.NAME, route);
 						} else {
 							// the container MUST make the route available to the applications 
 							// via the SipServletRequest.getPoppedRoute() method.						
 							try {
 								Address routeAdress = SipFactories.addressFactory.createAddress(route);
 								RouteHeader routeHeader = SipFactories.headerFactory.createRouteHeader(routeAdress);
 								sipServletRequest.setPoppedRoute(routeHeader);
 							} catch (ParseException e) {
 								logger.error("Impossible to parse the route into a compliant sip address",e);
 								//TODO send a 500 server internal error
 							}						
 						}
 						break;
 					case CLEAR_ROUTE :
 						//nothing to do here
 						//disregard the result.getRoute() and proceed. Specifically the SipServletRequest.getPoppedRoute() 
 						//returns the same value as before the invocation of application router.
 						break;
 					case NO_ROUTE :
 						// clear the value of popped route header such that SipServletRequest.getPoppedRoute() 
 						//now returns null until another route header belonging to the container is popped.
 						sipServletRequest.setPoppedRoute(null);
 						break;				
 				}
 			}
 			// 15.4.1 Procedure : point 3
 			if(applicationRouterInfo.getNextApplicationName() == null) {
 				//TODO check if the request point to another domain
 				boolean isAnotherDomain = false;
 				if(isAnotherDomain) {
 					// TODO If the Request-URI points to a different domain, or if there are one or more Route headers, 
 					// send the request externally according to standard SIP mechanism.
 				} else {
 					// TODO If the Request-URI does not point to another domain, and there is no Route header, 
 					// the container should not send the request as it will cause a loop. 
 					// Instead, the container must reject the request with 404 Not Found final response with no Retry-After header.
 				}
 			} else {
 				// TODO set the request's stateInfo to result.getStateInfo(), region to result.getRegion(), and URI to result.getSubscriberURI().
//				URI subscriberUri = SipFactories.addressFactory.createURI(applicationRouterInfo.getSubscriberURI()));
//				if(subscriberUri instanceof javax.sip.address.SipURI) {
//					sipServletRequest.setRequestURI(new SipURIImpl((javax.sip.address.SipURI)subscriberUri));
//				} else (subscriberUri instanceof javax.sip.address.TelURL) {
////					sipServletRequest.setRequestURI(new TelURIImpl((javax.sip.address.SipURI)subscriberUri));
//				}
				
 				// follow the procedures of Chapter 16 to select a servlet from the application.			
 				SipContext sipContext = applicationDeployed.get(applicationRouterInfo.getNextApplicationName());
 				session.setSipContext(sipContext);
 				String sipSessionHandlerName = ((SipSessionImpl)sipServletRequest.getSession()).getHandler();						
 				if(sipSessionHandlerName == null) {
 					String mainServlet = sipContext.getMainServlet();
 					sipSessionHandlerName = mainServlet;					
 					try {
 						sipServletRequest.getSession().setHandler(sipSessionHandlerName);
 					} catch (ServletException e) {
 						// this should never happen
 						logger.error(e);
 						//TODO send a 500 server internal error
 					} catch (IllegalStateException ise) {
 						logger.error(ise);
 						//TODO send a 500 server internal error
 					}
 				}
 				Container container = sipContext.findChild(sipSessionHandlerName);
 				Wrapper sipServletImpl = (Wrapper) container;
 				try {
 					Servlet servlet = sipServletImpl.allocate();	        
 					servlet.service(sipServletRequest, null);
 				} catch (ServletException e) {				
 					logger.error(e);
 					//TODO sends an error message
 				} catch (IOException e) {				
 					logger.error(e);
 					//TODO sends an error message
 				}
 			}
 			logger.info("Request event dispatched");
 		} else {
 			logger.info("Routing of Subsequent Request -- TODO");
 			Wrapper servletWrapper = (Wrapper) session.getSipContext().findChild(session.getHandler());
 			try
 			{
 				Servlet servlet = servletWrapper.allocate();
 				servlet.service(sipServletRequest, null);
 			} catch (ServletException e) {				
 				logger.error(e);
 				//TODO sends an error message
 			} catch (IOException e) {				
 				logger.error(e);
 				//TODO sends an error message
 			}
 		}
 	}
 	
 	
 	
 	/**
 	 * Method checking whether or not the sip servlet request in parameter is initial
 	 * according to algorithm defined in JSR289 Appendix B
 	 * @param sipServletRequest the sip servlet request to check
 	 * @param dialog the dialog associated with this request
 	 * @return true if the request is initial false otherwise
 	 */
 	private boolean isInitialRequest(SipServletRequestImpl sipServletRequest, Dialog dialog) {
 		// 2. Ongoing Transaction Detection - Employ methods of Section 17.2.3 in RFC 3261 
 		//to see if the request matches an existing transaction. 
 		//If it does, stop. The request is not an initial request.
 		Transaction sipTransaction = sipServletRequest.getTransaction();
 		if(sipTransaction != null && !TransactionState.PROCEEDING.equals(sipTransaction.getState())) {
 			return false;
 		}
 		// 3. Examine Request Method. If it is CANCEL, BYE, PRACK or ACK, stop. 
 		//The request is not an initial request for which application selection occurs.
 		if(nonInitialSipRequestMethods.contains(sipServletRequest.getMethod())) {
 			return false;
 		}
 		// 4. Existing Dialog Detection - If the request has a tag in the To header field, 
 		// the container computes the dialog identifier (as specified in section 12 of RFC 3261) 
 		// corresponding to the request and compares it with existing dialogs. 
 		// If it matches an existing dialog, stop. The request is not an initial request. 
 		// The request is a subsequent request and must be routed to the application path 
 		// associated with the existing dialog. 
 		// If the request has a tag in the To header field, 
 		// but the dialog identifier does not match any existing dialogs, 
 		// the container must reject the request with a 481 (Call/Transaction Does Not Exist). 
 		// Note: When this occurs, RFC 3261 says either the UAS has crashed or the request was misrouted. 
 		// In the latter case, the misrouted request is best handled by rejecting the request. 
 		// For the Sip Servlet environment, a UAS crash may mean either an application crashed 
 		// or the container itself crashed. In either case, it is impossible to route the request 
 		// as a subsequent request and it is inappropriate to route it as an initial request. 
 		// Therefore, the only viable approach is to reject the request.
 		if(dialog != null && !DialogState.EARLY.equals(dialog.getState())) {
 			return false;
 		}
 		// TODO 5. Detection of Merged Requests - If the From tag, Call-ID, and CSeq exactly 
 		// match those associated with an ongoing transaction, 
 		// the request has arrived more than once across different paths, most likely due to forking. 
 		// Such requests represent merged requests and MUST NOT be treated as initial requests. 
 		// Refer to section 11.3 for more information on container treatment of merged requests.
 		
 		// TODO 6. Detection of Requests Sent to Encoded URIs - 
 		// Requests may be sent to a container instance addressed to a URI obtained by calling 
 		// the encodeURI() method of a SipApplicationSession managed by this container instance. 
 		// When a container receives such a request, stop. This request is not an initial request. 
 		// Refer to section 15.9.2 Simple call with no modifications of requests 
 		// for more information on how a request sent to an encoded URI is handled by the container.
 		
 		return true;		
 	}	
 	
 	/**
 	 * {@inheritDoc}
 	 */
 	public void processResponse(ResponseEvent arg0) {
 		logger.info("Response " + arg0.getResponse().toString());
 		Response response = arg0.getResponse();
 		
 		// See if this transaction has been here before
 		Object appData = arg0.getClientTransaction().getApplicationData();
 		if(appData instanceof org.mobicents.servlet.sip.message.TransactionApplicationData)
 		{
 			org.mobicents.servlet.sip.message.TransactionApplicationData tad =
 				(org.mobicents.servlet.sip.message.TransactionApplicationData) appData;
 			
 			SipSessionImpl session = tad.getSipSession();
 			
 			// Transate the repsponse to SipServletResponse
 			SipServletResponseImpl sipServletResponse = new
 				SipServletResponseImpl(response, (SipProvider)arg0.getSource(),
 						arg0.getClientTransaction(), session, arg0.getDialog());
 
 			// Update Session state
 			if( sipServletResponse.getStatus()>=200 && sipServletResponse.getStatus()<300 )
 				session.setState(State.CONFIRMED);
 			if( sipServletResponse.getStatus()>=100 && sipServletResponse.getStatus()<200 )
 				session.setState(State.EARLY);
 			
 			// See if this is a response to a proxied request
 			ProxyBranchImpl proxyBranch = tad.getProxyBranch();
 			if(proxyBranch != null)
 			{
 				// Handle it at the branch
 				proxyBranch.onResponse(sipServletResponse); 
 				
 				// Notfiy the servlet
 				if(proxyBranch.getProxy().getSupervised())
 				{
 					callServlet(sipServletResponse, session);
 				}
 			}
 			else
 			{
 				callServlet(sipServletResponse, session);
 			}
 		}
 	}
 	
 	public static void callServlet(SipServletRequestImpl request, SipSessionImpl session)
 	{
 		Container container = session.getSipContext().findChild(session.getHandler());
 		Wrapper sipServletImpl = (Wrapper) container;
 		try {
 			Servlet servlet = sipServletImpl.allocate();	        
 			servlet.service(request, null);
 		} catch (ServletException e) {				
 			logger.error(e);
 			//TODO sends an error message
 		} catch (IOException e) {				
 			logger.error(e);
 			//TODO sends an error message
 		}
 	}
 	
 	public static void callServlet(SipServletResponseImpl response, SipSessionImpl session)
 	{
 		Container container = session.getSipContext().findChild(session.getHandler());
 		Wrapper sipServletImpl = (Wrapper) container;
 		try {
 			Servlet servlet = sipServletImpl.allocate();	        
 			servlet.service(null, response);
 		} catch (ServletException e) {				
 			logger.error(e);
 			//TODO sends an error message
 		} catch (IOException e) {				
 			logger.error(e);
 			//TODO sends an error message
 		}
 	}
 	
 	/**
 	 * {@inheritDoc}
 	 */
 	public void processTimeout(TimeoutEvent arg0) {
 		// TODO: FIX ME
 		Transaction tx = null;
 		if(arg0.isServerTransaction())
 		{
 			tx = arg0.getServerTransaction();
 		}
 		else
 		{
 			tx = arg0.getClientTransaction();
 		}
 		TransactionApplicationData tad = (TransactionApplicationData)tx.getApplicationData();
 		tad.getSipSession().onTransactionTimeout(tx);
 
 	}
 	/**
 	 * {@inheritDoc}
 	 */
 	public void processTransactionTerminated(TransactionTerminatedEvent arg0) {
 		// TODO: FIX ME
 		Transaction tx = null;
 		if(arg0.isServerTransaction())
 		{
 			tx = arg0.getServerTransaction();
 		}
 		else
 		{
 			tx = arg0.getClientTransaction();
 		}
 		TransactionApplicationData tad = (TransactionApplicationData)tx.getApplicationData();
 		tad.getSipSession().onTransactionTimeout(tx);
 		
 	}
 
 	/**
 	 * @return the sipApplicationRouter
 	 */
 	public SipApplicationRouter getSipApplicationRouter() {
 		return sipApplicationRouter;
 	}
 
 	/**
 	 * @param sipApplicationRouter the sipApplicationRouter to set
 	 */
 	public void setSipApplicationRouter(SipApplicationRouter sipApplicationRouter) {
 		this.sipApplicationRouter = sipApplicationRouter;
 	}
 }
