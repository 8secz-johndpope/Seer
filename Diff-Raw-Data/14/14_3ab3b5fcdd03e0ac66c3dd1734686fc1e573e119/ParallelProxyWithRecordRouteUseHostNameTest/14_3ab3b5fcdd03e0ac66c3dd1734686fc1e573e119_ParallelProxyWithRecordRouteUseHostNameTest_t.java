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
 package org.mobicents.servlet.sip.testsuite.proxy;
 
 import java.util.EventObject;
 import java.util.Hashtable;
 
 import javax.sip.DialogTerminatedEvent;
 import javax.sip.IOExceptionEvent;
 import javax.sip.RequestEvent;
 import javax.sip.ResponseEvent;
 import javax.sip.SipListener;
 import javax.sip.SipProvider;
 import javax.sip.TimeoutEvent;
 import javax.sip.TransactionTerminatedEvent;
 
 import org.apache.log4j.Logger;
 import org.mobicents.servlet.sip.SipServletTestCase;
 
 public class ParallelProxyWithRecordRouteUseHostNameTest extends SipServletTestCase implements SipListener {
 
 	private static transient Logger logger = Logger.getLogger(ParallelProxyWithRecordRouteUseHostNameTest.class);
 
 	protected Shootist shootist;
 
 	protected Shootme shootme;
 	
 	protected Cutme cutme;
 
 	protected Hashtable providerTable = new Hashtable();
 
 	private static final int timeout = 20000;
 
 	private static final int receiversCount = 1;
 
 	public ParallelProxyWithRecordRouteUseHostNameTest(String name) {
 		super(name);
 
 		this.sipIpAddress="0.0.0.0";
 	}
 
 	@Override
 	public void setUp() throws Exception {
 		super.setUp();
 		this.shootist = new Shootist(false);
 		this.shootme = new Shootme(5057);
 		this.cutme = new Cutme();
 	}
 
 	public void testProxy() {
 		this.shootme.init("stackName", null);
 		this.cutme.init(null);
 		this.shootist.init();
 		for (int q = 0; q < 20; q++) {
			if (shootme.ended == false && cutme.canceled == false)
 				try {
					Thread.sleep(10000);
 				} catch (InterruptedException e) {
 					// TODO Auto-generated catch block
 					e.printStackTrace();
 				}
 		}
		if (shootme.ended == false)
 			fail("Conversation not complete!");
 		if (cutme.canceled == false)
 			fail("The party that was supposed to be cancelled didn't cancel.");
 	}
 
 	@Override
 	public void tearDown() throws Exception {
 		shootist.destroy();
 		shootme.destroy();
 		cutme.destroy();
 		super.tearDown();
 	}
 
 	@Override
 	public void deployApplication() {
 		assertTrue(tomcat
 				.deployContext(
 						projectHome
 								+ "/sip-servlets-test-suite/applications/proxy-sip-servlet/src/main/sipapp",
 						"sip-test-context", "sip-test"));
 	}
 
 	@Override
 	protected String getDarConfigurationFile() {
 		return "file:///"
 				+ projectHome
 				+ "/sip-servlets-test-suite/testsuite/src/test/resources/"
 				+ "org/mobicents/servlet/sip/testsuite/proxy/simple-sip-servlet-dar.properties";
 	}
 
 	public void init() {
 		// setupPhones();
 	}
 
 	private SipListener getSipListener(EventObject sipEvent) {
 		SipProvider source = (SipProvider) sipEvent.getSource();
 		SipListener listener = (SipListener) providerTable.get(source);
 		if (listener == null)
 			throw new RuntimeException("Unexpected null listener");
 		return listener;
 	}
 
 	public void processRequest(RequestEvent requestEvent) {
 		getSipListener(requestEvent).processRequest(requestEvent);
 
 	}
 
 	public void processResponse(ResponseEvent responseEvent) {
 		getSipListener(responseEvent).processResponse(responseEvent);
 
 	}
 
 	public void processTimeout(TimeoutEvent timeoutEvent) {
 		getSipListener(timeoutEvent).processTimeout(timeoutEvent);
 	}
 
 	public void processIOException(IOExceptionEvent exceptionEvent) {
 		fail("unexpected exception");
 
 	}
 
 	public void processTransactionTerminated(
 			TransactionTerminatedEvent transactionTerminatedEvent) {
 		getSipListener(transactionTerminatedEvent)
 				.processTransactionTerminated(transactionTerminatedEvent);
 
 	}
 
 	public void processDialogTerminated(
 			DialogTerminatedEvent dialogTerminatedEvent) {
 		getSipListener(dialogTerminatedEvent).processDialogTerminated(
 				dialogTerminatedEvent);
 
 	}
 }
