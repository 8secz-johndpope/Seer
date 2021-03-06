 package com.thoughtworks.selenium.grid.hub.remotecontrol.commands;
 
 import com.thoughtworks.selenium.grid.hub.Environment;
 import com.thoughtworks.selenium.grid.hub.remotecontrol.DynamicRemoteControlPool;
 import com.thoughtworks.selenium.grid.hub.remotecontrol.RemoteControlProxy;
 import com.thoughtworks.selenium.grid.hub.remotecontrol.Response;
 import com.thoughtworks.selenium.grid.hub.remotecontrol.RemoteControlPool;
 import static junit.framework.Assert.assertEquals;
 import org.jbehave.core.mock.Mock;
 import org.jbehave.classmock.UsingClassMock;
 import org.junit.Test;
 
 import java.io.IOException;
 
 
 public class NewBrowserSessionCommandTest extends UsingClassMock {
 
     @Test
     public void sessionIdIsAlwaysNull() {
         assertEquals(null, new NewBrowserSessionCommand(null, "a query").sessionId());
     }
 
     @Test
     public void parseSessionIdReturnsTheSessionIdWhenResponseIsSuccessful() {
         assertEquals("22207", new NewBrowserSessionCommand(null, "").parseSessionId("OK,22207"));
     }
 
     @Test
     public void parseSessionIdReturnsNullWhenResponseIsNotSuccessful() {
         assertEquals(null, new NewBrowserSessionCommand(null, "").parseSessionId(""));
     }
 
     @Test
     public void queryStringSubstitutesBrowserWithTheEnvironmentBrowser() {
         final Environment environment;
 
         environment = new Environment("Firefox / Windows", "*chrome", null);
         assertEquals("a query string", new NewBrowserSessionCommand(environment, "a query string").queryString());
     }
 
     @Test
     public void executeReserveAndThenAssociateARemoteControlWithTheSession() throws IOException {
         final NewBrowserSessionCommand command;
         final Mock remoteControl;
         final Mock pool;
         final Response expectedResponse;
         final Environment environment;
 
         expectedResponse = new Response(0, "OK,1234");
         pool = mock(DynamicRemoteControlPool.class);
         remoteControl = mock(RemoteControlProxy.class);
         environment = new Environment("an environment", "*browser", null);
         command = new NewBrowserSessionCommand(environment, "a query");
         remoteControl.expects("forward").with(command.queryString()).will(returnValue(expectedResponse));
         pool.expects("reserve").with(environment).will(returnValue(remoteControl));
         pool.expects("associateWithSession"); //TODO .with(remoteControl, "1234");
 
         assertEquals(expectedResponse, command.execute((RemoteControlPool) pool));
         verifyMocks();
     }
 
     @Test
     public void executeReturnsAnErrorResponseWhenSessionCannotBeCreated() throws IOException {
         final NewBrowserSessionCommand command;
         final Mock remoteControl;
         final Environment environment;
         final Mock pool;
         final Response response;
 
         pool = mock(DynamicRemoteControlPool.class);
         remoteControl = mock(RemoteControlProxy.class);
         environment = new Environment("an environment", "*browser", null);
         command = new NewBrowserSessionCommand(environment, "a url");
         pool.expects("reserve").with(environment).will(returnValue(remoteControl));
         remoteControl.expects("forward").with(command.queryString()).will(returnValue(new Response(500, "")));
 
         response = command.execute((RemoteControlPool) pool);
         assertEquals(200, response.statusCode());
         assertEquals("ERROR: Could not retrieve a new session", response.body());
         verifyMocks();
     }
 
     @SuppressWarnings({"ThrowableInstanceNeverThrown"})
     @Test
     public void executeReleasesReservedRemoteControlAndReturnsAnErrorResponseWhenThereIsANetworkError() throws IOException {
         final NewBrowserSessionCommand command;
         final Mock remoteControl;
         final Environment environment;
         final Mock pool;
         final Response response;
 
         pool = mock(DynamicRemoteControlPool.class);
         remoteControl = mock(RemoteControlProxy.class);
         environment = new Environment("an environment", "*browser", null);
         command = new NewBrowserSessionCommand(environment, "a query");
         remoteControl.expects("forward").with(command.queryString()).will(throwException(new IOException("an error message")));
         pool.expects("reserve").with(environment).will(returnValue(remoteControl));
         pool.expects("release").with(remoteControl);
 
         response = command.execute((RemoteControlPool) pool);
         assertEquals(200, response.statusCode());
         assertEquals("ERROR: an error message", response.body());
         verifyMocks();
     }
 
     @Test
     public void executeReturnsAnErrorResponseWhenNoRemoteControlCanBeReserved() throws IOException {
         final NewBrowserSessionCommand command;
         final Environment environment;
         final Response response;
         final Mock pool;
 
         pool = mock(DynamicRemoteControlPool.class);
         environment = new Environment("an environment", "*browser", null);
         command = new NewBrowserSessionCommand(environment, "a query");
         pool.expects("reserve").with(environment).will(returnValue(null));
 
         response = command.execute((RemoteControlPool) pool);
         assertEquals(200, response.statusCode());
         assertEquals("ERROR: No available remote control for environment 'an environment'", response.body());
         verifyMocks();
     }
 
 }
