 /**
  * 
  */
 package org.mobicents.servlet.sip.proxy;
 
 import java.io.IOException;
 import java.util.ArrayList;
 import java.util.HashMap;
 import java.util.List;
 import java.util.Map;
 
 import javax.servlet.sip.Proxy;
 import javax.servlet.sip.ProxyBranch;
 import javax.servlet.sip.SipServletRequest;
 import javax.servlet.sip.SipServletResponse;
 import javax.servlet.sip.SipURI;
 import javax.servlet.sip.URI;
 
 import org.mobicents.servlet.sip.JainSipUtils;
 import org.mobicents.servlet.sip.address.SipURIImpl;
 import org.mobicents.servlet.sip.message.SipFactoryImpl;
 import org.mobicents.servlet.sip.message.SipServletRequestImpl;
 import org.mobicents.servlet.sip.message.SipServletResponseImpl;
 
 /**
  * @author root
  *
  */
 public class ProxyImpl implements Proxy {
 	
 	
 	private SipServletRequestImpl originalRequest;
 	private SipServletResponseImpl bestResponse;
 	private ProxyBranchImpl bestBranch;
 	private boolean recurse;
 	private int proxyTimeout;
 	private int seqSearchTimeout;
 	private boolean supervised; 
 	private boolean recordRoutingEnabled;
 	private boolean parallel;
 	private boolean addToPath;
 	private SipURI pathURI;
 	private SipURI recordRouteURI;
 	private SipURI outboundInterface;
 	private SipFactoryImpl sipFactoryImpl;
 	private int proxyId;
 	
 	private ProxyUtils proxyUtils;
 	
 	private Map<URI, ProxyBranch> proxyBranches;
 	
 	public ProxyImpl(SipServletRequestImpl request, SipFactoryImpl sipFactoryImpl)
 	{
 		this.originalRequest = request;
 		this.sipFactoryImpl = sipFactoryImpl;
 		proxyBranches = new HashMap<URI, ProxyBranch> ();
 		proxyUtils = new ProxyUtils(sipFactoryImpl, this);
 	}
 	
 	/* (non-Javadoc)
 	 * @see javax.servlet.sip.Proxy#cancel()
 	 */
 	public void cancel() {
 		cancelAllExcept(null);
 	}
 	
 	public void cancelAllExcept(ProxyBranch except) {
 		for(ProxyBranch pb : proxyBranches.values())
 		{
 			if(pb != except && pb.isStarted()) pb.cancel();
 		}
 	}
 
 	/* (non-Javadoc)
 	 * @see javax.servlet.sip.Proxy#createProxyBranches(java.util.List)
 	 */
 	public List<ProxyBranch> createProxyBranches(List<? extends URI> targets) {
 		ArrayList<ProxyBranch> list = new ArrayList<ProxyBranch>();
 		for(URI target: targets)
 		{
 			ProxyBranchImpl branch = new ProxyBranchImpl((SipURI)target, this, sipFactoryImpl, this.recordRouteURI);
 			list.add(branch);
 		}
 		return list;
 	}
 
 	/* (non-Javadoc)
 	 * @see javax.servlet.sip.Proxy#getAddToPath()
 	 */
 	public boolean getAddToPath() {
 		return addToPath;
 	}
 
 	/* (non-Javadoc)
 	 * @see javax.servlet.sip.Proxy#getOriginalRequest()
 	 */
 	public SipServletRequest getOriginalRequest() {
 		return originalRequest;
 	}
 
 	/* (non-Javadoc)
 	 * @see javax.servlet.sip.Proxy#getParallel()
 	 */
 	public boolean getParallel() {
 		return this.parallel;
 	}
 
 	/* (non-Javadoc)
 	 * @see javax.servlet.sip.Proxy#getPathURI()
 	 */
 	public SipURI getPathURI() {
 		return this.pathURI;
 	}
 
 	/* (non-Javadoc)
 	 * @see javax.servlet.sip.Proxy#getProxyBranch(javax.servlet.sip.URI)
 	 */
 	public ProxyBranch getProxyBranch(URI uri) {
 		return this.proxyBranches.get(uri);
 	}
 
 	/* (non-Javadoc)
 	 * @see javax.servlet.sip.Proxy#getProxyBranches()
 	 */
 	public List<ProxyBranch> getProxyBranches() {
 		return new ArrayList<ProxyBranch>(this.proxyBranches.values());
 	}
 
 	/* (non-Javadoc)
 	 * @see javax.servlet.sip.Proxy#getProxyTimeout()
 	 */
 	public int getProxyTimeout() {
 		return this.proxyTimeout;
 	}
 
 	/* (non-Javadoc)
 	 * @see javax.servlet.sip.Proxy#getRecordRoute()
 	 */
 	public boolean getRecordRoute() {
 		return this.recordRoutingEnabled;
 	}
 
 	/* (non-Javadoc)
 	 * @see javax.servlet.sip.Proxy#getRecordRouteURI()
 	 */
 	public SipURI getRecordRouteURI() {
 		return this.recordRouteURI;
 	}
 
 	/* (non-Javadoc)
 	 * @see javax.servlet.sip.Proxy#getRecurse()
 	 */
 	public boolean getRecurse() {
 		return this.recurse;
 	}
 
 	/* (non-Javadoc)
 	 * @see javax.servlet.sip.Proxy#getSequentialSearchTimeout()
 	 */
 	public int getSequentialSearchTimeout() {
 		return this.seqSearchTimeout;
 	}
 
 	/* (non-Javadoc)
 	 * @see javax.servlet.sip.Proxy#getStateful()
 	 */
 	public boolean getStateful() {
 		return true;
 	}
 
 	/* (non-Javadoc)
 	 * @see javax.servlet.sip.Proxy#getSupervised()
 	 */
 	public boolean getSupervised() {
 		return this.supervised;
 	}
 
 	/* (non-Javadoc)
 	 * @see javax.servlet.sip.Proxy#proxyTo(java.util.List)
 	 */
 	public void proxyTo(List<? extends URI> uris) {
 		for (URI uri : uris)
 		{
 			ProxyBranchImpl pbi = new ProxyBranchImpl((SipURI) uri, this, sipFactoryImpl, this.recordRouteURI);
 			this.proxyBranches.put(uri, pbi);
 		}
 		startProxy();
 	}
 
 	/* (non-Javadoc)
 	 * @see javax.servlet.sip.Proxy#proxyTo(javax.servlet.sip.URI)
 	 */
 	public void proxyTo(URI uri) {
 		
 		ProxyBranchImpl pbi = new ProxyBranchImpl((SipURI) uri, this, sipFactoryImpl, this.recordRouteURI);
 		this.proxyBranches.put(uri, pbi);
 		pbi.start();
 
 	}
 
 	/* (non-Javadoc)
 	 * @see javax.servlet.sip.Proxy#setAddToPath(boolean)
 	 */
 	public void setAddToPath(boolean p) {
 		addToPath = p;
 
 	}
 
 	/* (non-Javadoc)
 	 * @see javax.servlet.sip.Proxy#setOutboundInterface(javax.servlet.sip.SipURI)
 	 */
 	public void setOutboundInterface(SipURI uri) {
 		outboundInterface = uri;
 
 	}
 
 	/* (non-Javadoc)
 	 * @see javax.servlet.sip.Proxy#setParallel(boolean)
 	 */
 	public void setParallel(boolean parallel) {
 		this.parallel = parallel;
 
 	}
 
 	/* (non-Javadoc)
 	 * @see javax.servlet.sip.Proxy#setProxyTimeout(int)
 	 */
 	public void setProxyTimeout(int seconds) {
 		proxyTimeout = seconds;
 
 	}
 
 	/* (non-Javadoc)
 	 * @see javax.servlet.sip.Proxy#setRecordRoute(boolean)
 	 */
 	public void setRecordRoute(boolean rr) {
 		
 		this.recordRouteURI = new SipURIImpl ( JainSipUtils.createRecordRouteURI( sipFactoryImpl.getSipProviders(), null));
 		this.recordRoutingEnabled = rr;
 
 	}
 
 	/* (non-Javadoc)
 	 * @see javax.servlet.sip.Proxy#setRecurse(boolean)
 	 */
 	public void setRecurse(boolean recurse) {
 		this.recurse = recurse;
 
 	}
 
 	/* (non-Javadoc)
 	 * @see javax.servlet.sip.Proxy#setSequentialSearchTimeout(int)
 	 */
 	public void setSequentialSearchTimeout(int seconds) {
 		seqSearchTimeout = seconds;
 
 	}
 
 	/* (non-Javadoc)
 	 * @see javax.servlet.sip.Proxy#setStateful(boolean)
 	 */
 	public void setStateful(boolean stateful) {
 		//NOTHING
 
 	}
 
 	/* (non-Javadoc)
 	 * @see javax.servlet.sip.Proxy#setSupervised(boolean)
 	 */
 	public void setSupervised(boolean supervised) {
 		this.supervised = supervised;
 
 	}
 
 	/* (non-Javadoc)
 	 * @see javax.servlet.sip.Proxy#startProxy()
 	 */
 	public void startProxy() {
 		if(this.parallel)
 		{
 			for (ProxyBranch pb : this.proxyBranches.values())
 			{
 				((ProxyBranchImpl)pb).start();
 			}
 		}
 		else
 		{
 			startNextUntriedBranch();
 		}
 
 	}
 
 	public SipURI getOutboundInterface() {
 		return outboundInterface;
 	}
 	
 	public void onFinalResponse(ProxyBranchImpl branch)
 	{
 		//Get the final response
 		SipServletResponseImpl response = (SipServletResponseImpl) branch.getResponse();
 		
 		// Cancel all others if 2xx or 6xx 10.2.4
 		if( (response.getStatus() >= 200 && response.getStatus() < 300) 
 				|| (response.getStatus() >= 600 && response.getStatus() < 700) ) 
 			cancelAllExcept(branch);
 		
 		// Recurse if allowed
 		if(response.getStatus() >= 300 && response.getStatus() < 400
 				&& getRecurse())
 		{
 			// Recurse
 		}
 		
 		// Sort best do far		
 		if(bestResponse == null || bestResponse.getStatus() > response.getStatus())
 		{
 			bestResponse = response;
 			bestBranch = branch;
 		}
 		
 		// Check if we are waiting for more response
 		if(allResponsesHaveArrived())
 		{
 			sendBestFinalResponse(bestResponse);
 		}
 
 	}
 	
 	public void onBranchTimeOut(ProxyBranchImpl branch)
 	{
		if(this.parallel)
 			startNextUntriedBranch();
 	}
 	
 	// In sequential proxying get some untried branch and start it, then wait for response and repeat
 	public void startNextUntriedBranch()
 	{
		if(!this.parallel) 
 			throw new IllegalStateException("This method is only for sequantial proxying");
 		
 		for(ProxyBranch pb: this.proxyBranches.values())
 		{
 			ProxyBranchImpl pbi = (ProxyBranchImpl) pb;
 			SipServletResponse response = pb.getResponse();
 			if(!pbi.isStarted())
 			{
 				pbi.start();
 				return;
 			}
 		}
 	}
 	
 	public boolean allResponsesHaveArrived()
 	{
 		for(ProxyBranch pb: this.proxyBranches.values())
 		{
 			ProxyBranchImpl pbi = (ProxyBranchImpl) pb;
 			SipServletResponse response = pb.getResponse();
 			if(pbi.isStarted() && (response == null || response.getStatus() < 200
 					|| pbi.isTimedOut()))
 				return false;
 		}
 		return true;
 	}
 	
 	public void sendBestFinalResponse(SipServletResponseImpl response)
 	{
 		SipServletResponse proxiedResponse = 
 			proxyUtils.createProxiedResponse(response);
 		
 		if(proxiedResponse == null) 
 			return; // this response was addressed to this proxy
 
 		try {
 			proxiedResponse.send();
 		} catch (IOException e) {
 			// TODO Auto-generated catch block
 			e.printStackTrace();
 		}
 	}
 	
 	ProxyUtils getProxyUtils() {
 		return proxyUtils;
 	}
 }
