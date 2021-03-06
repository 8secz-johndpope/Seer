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
 package org.mobicents.servlet.sip.testsuite;
 
 import java.io.IOException;
 import java.util.ArrayList;
import java.util.List;
 
 import javax.servlet.Servlet;
 import javax.servlet.ServletConfig;
 import javax.servlet.ServletException;
 import javax.servlet.sip.Proxy;
 import javax.servlet.sip.SipErrorEvent;
 import javax.servlet.sip.SipErrorListener;
 import javax.servlet.sip.SipFactory;
 import javax.servlet.sip.SipServlet;
 import javax.servlet.sip.SipServletRequest;
 import javax.servlet.sip.SipServletResponse;
 import javax.servlet.sip.SipURI;
 import javax.servlet.sip.URI;
 
 import org.apache.commons.logging.Log;
 import org.apache.commons.logging.LogFactory;
 
 public class ProxySipServlet extends SipServlet implements SipErrorListener,
 		Servlet {
 
 	private static Log logger = LogFactory.getLog(ProxySipServlet.class);
 	
 	
 	/** Creates a new instance of SimpleProxyServlet */
 	public ProxySipServlet() {
 	}
 
 	@Override
 	public void init(ServletConfig servletConfig) throws ServletException {
 		logger.info("the proxy sip servlet has been started");
 		super.init(servletConfig);
 	}
 
 	/**
 	 * {@inheritDoc}
 	 */
 	@Override
 	protected void doInvite(SipServletRequest request) throws ServletException,
 			IOException {
 
 		logger.info("Got request:\n"
 				+ request.getMethod());		
 		//This is a proxying sample.
 		SipFactory sipFactory = (SipFactory) getServletContext().getAttribute(SIP_FACTORY);
 		
 		URI uri1 = sipFactory.createAddress("sip:receiver@127.0.0.1:5057").getURI();
 		URI uri2 = sipFactory.createAddress("sip:cutme@127.0.0.1:5056").getURI();
 		ArrayList uris = new ArrayList();
 		uris.add(uri1);
 		uris.add(uri2);
 		Proxy proxy = request.getProxy();
		List<SipURI> outboundInterfaces = (List<SipURI>)getServletContext().getAttribute(OUTBOUND_INTERFACES);
		
		if(outboundInterfaces == null) throw new NullPointerException("Outbound interfaces should not be null");
		
		for(SipURI uri:outboundInterfaces) {
			if(uri.toString().indexOf("127.0.0.1")>0) {
				// pick the lo interface, since its universal on all machines
				proxy.setOutboundInterface(uri);
				break;
			}
		}
		//proxy.setOutboundInterface((SipURI)sipFactory.createAddress("sip:proxy@127.0.0.1:5070").getURI());
 		proxy.setRecordRoute(true);
 		proxy.getRecordRouteURI().setParameter("testparamname", "TESTVALUE");
 		proxy.setParallel(true);
 		proxy.proxyTo(uris);
 	}
 
 	/**
 	 * {@inheritDoc}
 	 */
 	protected void doBye(SipServletRequest request) throws ServletException,
 			IOException {
 
 		logger.info("Got BYE request:\n" + request);
 		SipServletResponse sipServletResponse = request.createResponse(200);
 		sipServletResponse.send();
 	}
 
 	/**
 	 * {@inheritDoc}
 	 */
 	protected void doResponse(SipServletResponse response)
 			throws ServletException, IOException {
 
 		logger.info("Got response: " + response);
 		super.doResponse(response);
 	}
 
 	// SipErrorListener methods
 
 	/**
 	 * {@inheritDoc}
 	 */
 	public void noAckReceived(SipErrorEvent ee) {
 		logger.error("noAckReceived.");
 	}
 
 	/**
 	 * {@inheritDoc}
 	 */
 	public void noPrackReceived(SipErrorEvent ee) {
 		logger.error("noPrackReceived.");
 	}
 
 }
