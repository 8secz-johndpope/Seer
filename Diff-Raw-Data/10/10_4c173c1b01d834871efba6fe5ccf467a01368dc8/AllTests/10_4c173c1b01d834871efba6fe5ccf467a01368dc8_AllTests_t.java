 package org.mobicents.servlet.sip.testsuite;
 
 import junit.framework.Test;
 import junit.framework.TestSuite;
 
import org.mobicents.servlet.sip.testsuite.annotations.AnnotationTest;
 import org.mobicents.servlet.sip.testsuite.b2bua.B2BUASipUnitTest;
 import org.mobicents.servlet.sip.testsuite.callcontroller.CallBlockingTest;
 import org.mobicents.servlet.sip.testsuite.callcontroller.CallControllerSipUnitTest;
 import org.mobicents.servlet.sip.testsuite.callcontroller.CallForwardingB2BUAJunitTest;
 import org.mobicents.servlet.sip.testsuite.callcontroller.CallForwardingJunitTest;
 import org.mobicents.servlet.sip.testsuite.callcontroller.CallForwardingSipUnitTest;
 import org.mobicents.servlet.sip.testsuite.click2call.Click2CallBasicTest;
 import org.mobicents.servlet.sip.testsuite.composition.SpeedDialLocationServiceJunitTest;
 import org.mobicents.servlet.sip.testsuite.listeners.ListenersSipServletTest;
 import org.mobicents.servlet.sip.testsuite.proxy.ProxyBranchTimeoutTest;
 import org.mobicents.servlet.sip.testsuite.proxy.ParallelProxyWithRecordRouteTest;
 import org.mobicents.servlet.sip.testsuite.session.SessionStateUACSipServletTest;
 import org.mobicents.servlet.sip.testsuite.session.SessionStateUASSipServletTest;
 import org.mobicents.servlet.sip.testsuite.simple.ShootistSipServletTest;
 import org.mobicents.servlet.sip.testsuite.simple.ShootmeSipServletTest;
 import org.mobicents.servlet.sip.testsuite.timers.TimersSipServletTest;
 
 public class AllTests {
 
 	public static Test suite() {
 		TestSuite suite = new TestSuite(
 				"Test for org.mobicents.servlet.sip.testsuite");
 		//$JUnit-BEGIN$
 		suite.addTestSuite(ShootmeSipServletTest.class);
 		suite.addTestSuite(ShootistSipServletTest.class);
 		suite.addTestSuite(B2BUASipUnitTest.class);
 		suite.addTestSuite(CallBlockingTest.class);
 		suite.addTestSuite(CallForwardingJunitTest.class);
 		suite.addTestSuite(CallForwardingSipUnitTest.class);
 		suite.addTestSuite(CallForwardingB2BUAJunitTest.class);
 		suite.addTestSuite(CallControllerSipUnitTest.class);
 		suite.addTestSuite(ParallelProxyWithRecordRouteTest.class);
 		suite.addTestSuite(ProxyBranchTimeoutTest.class);
 		suite.addTestSuite(SpeedDialLocationServiceJunitTest.class);
 		suite.addTestSuite(Click2CallBasicTest.class);
 		suite.addTestSuite(ListenersSipServletTest.class);
 		suite.addTestSuite(TimersSipServletTest.class);		
 		suite.addTestSuite(SessionStateUASSipServletTest.class);
 		suite.addTestSuite(SessionStateUACSipServletTest.class);
		suite.addTestSuite(AnnotationTest.class);
 		//$JUnit-END$
 		return suite;
 	}
 
 }
