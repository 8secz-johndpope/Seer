 /*
  * JBoss, Home of Professional Open Source
  *
  * Distributable under LGPL license.
  * See terms of license at gnu.org.
  */
 package org.jboss.messaging.core.remoting.impl.mina.integration.test;
 
 import static java.util.concurrent.TimeUnit.SECONDS;
 import static org.easymock.EasyMock.createMock;
 import static org.easymock.EasyMock.expect;
 import static org.easymock.EasyMock.isA;
 import static org.easymock.EasyMock.replay;
 import static org.easymock.EasyMock.verify;
 import static org.jboss.messaging.core.remoting.TransportType.TCP;
 import static org.jboss.messaging.core.remoting.impl.mina.integration.test.TestSupport.KEEP_ALIVE_INTERVAL;
 import static org.jboss.messaging.core.remoting.impl.mina.integration.test.TestSupport.KEEP_ALIVE_TIMEOUT;
 import static org.jboss.messaging.core.remoting.impl.mina.integration.test.TestSupport.PORT;
 import static org.jboss.messaging.test.unit.RandomUtil.randomLong;
import static org.jboss.messaging.test.unit.RandomUtil.randomString;
 
 import java.util.concurrent.CountDownLatch;
 
 import junit.framework.TestCase;
 
 import org.jboss.messaging.core.client.FailureListener;
 import org.jboss.messaging.core.config.impl.ConfigurationImpl;
 import org.jboss.messaging.core.exception.MessagingException;
 import org.jboss.messaging.core.remoting.KeepAliveFactory;
 import org.jboss.messaging.core.remoting.NIOSession;
 import org.jboss.messaging.core.remoting.RemotingException;
 import org.jboss.messaging.core.remoting.impl.ConfigurationHelper;
 import org.jboss.messaging.core.remoting.impl.PacketDispatcherImpl;
 import org.jboss.messaging.core.remoting.impl.mina.ClientKeepAliveFactory;
 import org.jboss.messaging.core.remoting.impl.mina.MinaConnector;
 import org.jboss.messaging.core.remoting.impl.mina.MinaService;
 import org.jboss.messaging.core.remoting.impl.wireformat.Ping;
 import org.jboss.messaging.core.remoting.impl.wireformat.Pong;
import org.jboss.messaging.test.unit.RandomUtil;
 
 /**
  * @author <a href="mailto:jmesnil@redhat.com">Jeff Mesnil</a>
  * 
  * @version <tt>$Revision$</tt>
  * 
  */
 public class ClientKeepAliveTest extends TestCase
 {
    // Constants -----------------------------------------------------
 
    // Attributes ----------------------------------------------------
 
    private MinaService service;
 
    // Static --------------------------------------------------------
 
    // Constructors --------------------------------------------------
 
    // Public --------------------------------------------------------
 
    @Override
    protected void setUp() throws Exception
    {
       ConfigurationImpl config = ConfigurationHelper.newConfiguration(TCP, "localhost", PORT);
       config.setKeepAliveInterval(KEEP_ALIVE_INTERVAL);
       config.setKeepAliveTimeout(KEEP_ALIVE_TIMEOUT);
       service = new MinaService(config);
       service.start();
    }
 
    @Override
    protected void tearDown() throws Exception
    {
       service.stop();
       service = null;
    }
 
    public void testKeepAliveWithClientOK() throws Exception
    {  
       KeepAliveFactory factory = createMock(KeepAliveFactory.class);
 
       // client never send ping
      expect(factory.ping(isA(Long.class))).andStubReturn(null);
      expect(factory.isPing(isA(Long.class), isA(Ping.class))).andStubReturn(true);
      expect(factory.isPing(isA(Long.class), isA(Object.class))).andStubReturn(false);
       // client is responding
      expect(factory.pong(isA(Long.class), isA(Ping.class))).andReturn(new Pong(randomLong(), false)).atLeastOnce();
 
       replay(factory);
 
       final CountDownLatch latch = new CountDownLatch(1);
 
       FailureListener listener = new FailureListener() {
          public void onFailure(MessagingException me)
          {
             assertTrue(me instanceof RemotingException);
            RemotingException re = (RemotingException) me;
             latch.countDown();
          }
       };
       service.addFailureListener(listener);
 
       MinaConnector connector = new MinaConnector(service.getConfiguration(), new PacketDispatcherImpl(null), factory);
       connector.connect();
 
       boolean firedKeepAliveNotification = latch.await(KEEP_ALIVE_INTERVAL
             + KEEP_ALIVE_TIMEOUT + 2, SECONDS);
       assertFalse(firedKeepAliveNotification);
 
       service.removeFailureListener(listener);
       connector.disconnect();
 
       verify(factory);
    }
    
    public void testKeepAliveWithClientNotResponding() throws Exception
    {
       KeepAliveFactory factory = new ClientKeepAliveFactoryNotResponding();
 
       final long[] clientSessionIDNotResponding = new long[1];
       final CountDownLatch latch = new CountDownLatch(1);
 
       FailureListener listener = new FailureListener() {
          public void onFailure(MessagingException me)
          {
             assertTrue(me instanceof RemotingException);
             RemotingException re = (RemotingException) me;
             clientSessionIDNotResponding[0] = re.getSessionID();
             latch.countDown();
          }
       };
       service.addFailureListener(listener);
       
       MinaConnector connector = new MinaConnector(service.getConfiguration(), new PacketDispatcherImpl(null), factory);
 
       NIOSession session = connector.connect();
       long clientSessionID = session.getID();
 
       boolean firedKeepAliveNotification = latch.await(KEEP_ALIVE_INTERVAL
             + KEEP_ALIVE_TIMEOUT + 2, SECONDS);
       assertTrue("notification has not been received", firedKeepAliveNotification);
       assertNotNull(clientSessionIDNotResponding[0]);
       assertEquals(clientSessionID, clientSessionIDNotResponding[0]);
 
       service.removeFailureListener(listener);
       connector.disconnect();
    }
 
    public void testKeepAliveWithClientTooLongToRespond() throws Exception
    {
       KeepAliveFactory factory = new KeepAliveFactory()
       {
          public Ping ping(long sessionID)
          {
             return null;
          }
          
          public boolean isPing(long sessionID, Object message)
          {
             return (message instanceof Ping);
          }
 
          public synchronized Pong pong(long sessionID, Ping ping)
          {
             // like a TCP timeout, there is no response in the next 2 hours
             try
             {
                wait(2 * 3600);
             } catch (InterruptedException e)
             {
                e.printStackTrace();
             }
             return new Pong(randomLong(), false);
          }         
       };
 
       try
       {
          MinaConnector connector = new MinaConnector(service.getConfiguration(),
                new PacketDispatcherImpl(null), factory);
 
          NIOSession session = connector.connect();
          long clientSessionID = session.getID();
 
          final long[] clientSessionIDNotResponding = new long[1];
          final CountDownLatch latch = new CountDownLatch(1);
 
          FailureListener listener = new FailureListener() {
             public void onFailure(MessagingException me)
             {
                assertTrue(me instanceof RemotingException);
                RemotingException re = (RemotingException) me;
                clientSessionIDNotResponding[0] = re.getSessionID();
                latch.countDown();
             }
          };
          service.addFailureListener(listener);
 
          boolean firedKeepAliveNotification = latch.await(KEEP_ALIVE_INTERVAL
                + KEEP_ALIVE_TIMEOUT + 2, SECONDS);
          assertTrue("notification has not been received", firedKeepAliveNotification);
          assertNotNull(clientSessionIDNotResponding[0]);
          assertEquals(clientSessionID, clientSessionIDNotResponding[0]);
 
          service.removeFailureListener(listener);
          connector.disconnect();
 
       } finally
       {
          // test is done: wake up the factory
          synchronized (factory)
          {
             factory.notify();
          }
       }
    }
 
    public void testKeepAliveWithClientRespondingAndClientNotResponding()
          throws Exception
    {
       KeepAliveFactory notRespondingfactory = new ClientKeepAliveFactoryNotResponding();
       KeepAliveFactory respondingfactory = new ClientKeepAliveFactory();
 
       final long[] sessionIDNotResponding = new long[1];
       final CountDownLatch latch = new CountDownLatch(1);
 
       FailureListener listener = new FailureListener() {
          public void onFailure(MessagingException me)
          {
             assertTrue(me instanceof RemotingException);
             RemotingException re = (RemotingException) me;
             sessionIDNotResponding[0] = re.getSessionID();
             latch.countDown();
          }
       };
       service.addFailureListener(listener);
       
       MinaConnector connectorNotResponding = new MinaConnector(service
             .getConfiguration(), new PacketDispatcherImpl(null), notRespondingfactory);
       MinaConnector connectorResponding = new MinaConnector(service
             .getConfiguration(), new PacketDispatcherImpl(null), respondingfactory);
 
       NIOSession sessionNotResponding = connectorNotResponding.connect();
       long clientSessionIDNotResponding = sessionNotResponding.getID();
 
       
       NIOSession sessionResponding = connectorResponding.connect();
       long clientSessionIDResponding = sessionResponding.getID();
 
       boolean firedKeepAliveNotification = latch.await(KEEP_ALIVE_INTERVAL
             + KEEP_ALIVE_TIMEOUT + 2, SECONDS);
       assertTrue("notification has not been received", firedKeepAliveNotification);
 
       assertNotNull(sessionIDNotResponding[0]);
       assertEquals(clientSessionIDNotResponding, sessionIDNotResponding[0]);
       assertNotSame(clientSessionIDResponding, sessionIDNotResponding[0]);
 
       service.removeFailureListener(listener);
       connectorNotResponding.disconnect();
       connectorResponding.disconnect();
    }
    
    // Package protected ---------------------------------------------
 
    // Protected -----------------------------------------------------
 
    // Private -------------------------------------------------------
 
    // Inner classes -------------------------------------------------
    
    private class ClientKeepAliveFactoryNotResponding extends ClientKeepAliveFactory
    {
       @Override
       public Ping ping(long clientSessionID)
       {
          return null;
       }
 
       @Override
       public Pong pong(long sessionID, Ping ping)
       {
          return null;
       }
    }
 }
