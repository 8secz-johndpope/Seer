 /**
  * JBoss, Home of Professional Open Source
  *
  * Distributable under LGPL license.
  * See terms of license at gnu.org.
  */
 package org.jboss.test.messaging.jms.clustering;
 
 import org.jboss.test.messaging.jms.clustering.base.ClusteringTestBase;
 import org.jboss.test.messaging.tools.ServerManagement;
 import org.jboss.test.messaging.tools.aop.PoisonInterceptor;
 import org.jboss.jms.client.JBossConnection;
 import org.jboss.jms.client.FailoverEvent;
 import org.jboss.jms.client.remoting.JMSRemotingConnection;
 import org.jboss.jms.client.state.ConnectionState;
 import org.jboss.jms.client.delegate.DelegateSupport;
 import org.jboss.jms.client.delegate.ClientConnectionDelegate;
 
 import javax.jms.Connection;
 import javax.jms.Session;
 import javax.jms.MessageProducer;
 import javax.jms.MessageConsumer;
 import javax.jms.QueueBrowser;
 import javax.jms.TextMessage;
 import javax.jms.DeliveryMode;
 import javax.jms.Message;
 
 import java.util.Enumeration;
 import java.util.Set;
 import java.util.HashSet;
 
 /**
  * @author <a href="mailto:ovidiu@jboss.org">Ovidiu Feodorov</a>
  * @version <tt>$Revision$</tt>
  *
  * $Id$
  */
 public class FailoverTest extends ClusteringTestBase
 {
    // Constants ------------------------------------------------------------------------------------
 
    // Static ---------------------------------------------------------------------------------------
 
    // Attributes -----------------------------------------------------------------------------------
 
    // Constructors ---------------------------------------------------------------------------------
 
    public FailoverTest(String name)
    {
       super(name);
    }
 
    // Public ---------------------------------------------------------------------------------------
 
    public void testSimpleConnectionFailover() throws Exception
    {
       Connection conn = null;
 
       try
       {
          // skip connection to node 0
          conn = cf.createConnection();
          conn.close();
 
          // create a connection to node 1
          conn = cf.createConnection();
          conn.start();
 
          assertEquals(1, ((JBossConnection)conn).getServerID());
 
          // register a failover listener
          SimpleFailoverListener failoverListener = new SimpleFailoverListener();
          ((JBossConnection)conn).registerFailoverListener(failoverListener);
 
          log.debug("killing node 1 ....");
 
          ServerManagement.kill(1);
 
          log.info("########");
          log.info("######## KILLED NODE 1");
          log.info("########");
 
          // wait for the client-side failover to complete
 
          while(true)
          {
             FailoverEvent event = failoverListener.getEvent(120000);
             if (event != null && FailoverEvent.FAILOVER_COMPLETED == event.getType())
             {
                break;
             }
             if (event == null)
             {
                fail("Did not get expected FAILOVER_COMPLETED event");
             }
          }
 
          // failover complete
          log.info("failover completed");
 
          assertEquals(0, ((JBossConnection)conn).getServerID());
 
       }
       finally
       {
          if (conn != null)
          {
             conn.close();
          }
       }
    }
 
    public void testSessionFailover() throws Exception
    {
       Connection conn = null;
 
       try
       {
          // skip connection to node 0
          conn = cf.createConnection();
          conn.close();
 
          // create a connection to node 1
          conn = cf.createConnection();
          conn.start();
 
          assertEquals(1, ((JBossConnection)conn).getServerID());
 
          Session session = conn.createSession(false, Session.AUTO_ACKNOWLEDGE);
 
          // register a failover listener
          SimpleFailoverListener failoverListener = new SimpleFailoverListener();
          ((JBossConnection)conn).registerFailoverListener(failoverListener);
 
          log.debug("killing node 1 ....");
 
          ServerManagement.kill(1);
 
          log.info("########");
          log.info("######## KILLED NODE 1");
          log.info("########");
 
          // wait for the client-side failover to complete
 
          while(true)
          {
             FailoverEvent event = failoverListener.getEvent(120000);
             if (event != null && FailoverEvent.FAILOVER_COMPLETED == event.getType())
             {
                break;
             }
             if (event == null)
             {
                fail("Did not get expected FAILOVER_COMPLETED event");
             }
          }
 
          // failover complete
          log.info("failover completed");
 
          assertEquals(0, ((JBossConnection)conn).getServerID());
 
          // use the old session to send/receive a message
          session.createProducer(queue[0]).send(session.createTextMessage("blik"));
 
          TextMessage m = (TextMessage)session.createConsumer(queue[0]).receive(2000);
 
          assertEquals("blik", m.getText());
       }
       finally
       {
          if (conn != null)
          {
             conn.close();
          }
       }
    }
 
    public void testProducerFailover() throws Exception
    {
       Connection conn = null;
 
       try
       {
          // skip connection to node 0
          conn = cf.createConnection();
          conn.close();
 
          // create a connection to node 1
          conn = cf.createConnection();
 
          conn.start();
 
          assertEquals(1, ((JBossConnection)conn).getServerID());
 
          Session session = conn.createSession(false, Session.AUTO_ACKNOWLEDGE);
          MessageProducer prod = session.createProducer(queue[1]);
 
          // register a failover listener
          SimpleFailoverListener failoverListener = new SimpleFailoverListener();
          ((JBossConnection)conn).registerFailoverListener(failoverListener);
 
          log.debug("killing node 1 ....");
 
          ServerManagement.kill(1);
 
          log.info("########");
          log.info("######## KILLED NODE 1");
          log.info("########");
 
          // wait for the client-side failover to complete
 
          while(true)
          {
             FailoverEvent event = failoverListener.getEvent(120000);
             if (event != null && FailoverEvent.FAILOVER_COMPLETED == event.getType())
             {
                break;
             }
             if (event == null)
             {
                fail("Did not get expected FAILOVER_COMPLETED event");
             }
          }
 
          // failover complete
          log.info("failover completed");
 
          assertEquals(0, ((JBossConnection)conn).getServerID());
 
          // send a message, send it with the failed over producer and make sure I can receive it
          Message m = session.createTextMessage("clik");
          prod.send(m);
 
          MessageConsumer cons = session.createConsumer(queue[0]);
          TextMessage tm = (TextMessage)cons.receive(2000);
 
          assertNotNull(tm);
          assertEquals("clik", tm.getText());
       }
       finally
       {
          if (conn != null)
          {
             conn.close();
          }
       }
    }
 
    public void testConsumerFailoverWithConnectionStopped() throws Exception
    {
       Connection conn = null;
 
       try
       {
          // skip connection to node 0
          conn = cf.createConnection();
          conn.close();
 
          // create a connection to node 1
          conn = cf.createConnection();
 
          assertEquals(1, ((JBossConnection)conn).getServerID());
 
          Session session = conn.createSession(false, Session.AUTO_ACKNOWLEDGE);
          MessageConsumer cons = session.createConsumer(queue[1]);
          MessageProducer prod = session.createProducer(queue[1]);
 
          // send a message (connection is stopped, so it will stay on the server), and I expect
          // to receive it with the failed-over consumer after crash
 
          Message m = session.createTextMessage("plik");
          prod.send(m);
 
 
          // register a failover listener
          SimpleFailoverListener failoverListener = new SimpleFailoverListener();
          ((JBossConnection)conn).registerFailoverListener(failoverListener);
 
          log.debug("killing node 1 ....");
 
          ServerManagement.kill(1);
 
          log.info("########");
          log.info("######## KILLED NODE 1");
          log.info("########");
 
          // wait for the client-side failover to complete
 
          while(true)
          {
             FailoverEvent event = failoverListener.getEvent(120000);
             if (event != null && FailoverEvent.FAILOVER_COMPLETED == event.getType())
             {
                break;
             }
             if (event == null)
             {
                fail("Did not get expected FAILOVER_COMPLETED event");
             }
          }
 
          // failover complete
          log.info("failover completed");
 
          assertEquals(0, ((JBossConnection)conn).getServerID());
 
          // activate the failed-over consumer
          conn.start();
 
          TextMessage rm = (TextMessage)cons.receive(2000);
          assertNotNull(rm);
          assertEquals("plik", rm.getText());
       }
       finally
       {
          if (conn != null)
          {
             conn.close();
          }
       }
    }
 
    public void testConsumerFailoverWithConnectionStarted() throws Exception
    {
       Connection conn = null;
 
       try
       {
          // skip connection to node 0
          conn = cf.createConnection();
          conn.close();
 
          // create a connection to node 1
          conn = cf.createConnection();
 
          assertEquals(1, ((JBossConnection)conn).getServerID());
 
          Session session = conn.createSession(false, Session.AUTO_ACKNOWLEDGE);
          MessageConsumer cons = session.createConsumer(queue[1]);
          MessageProducer prod = session.createProducer(queue[1]);
 
          // start the connection, so the message makes it to the client-side MessageCallbackHandler
          // buffer
 
          conn.start();
 
          Message m = session.createTextMessage("nik");
          prod.send(m);
 
          // wait a bit so the message makes it to the client
          log.info("sleeping 2 secs ...");
          Thread.sleep(2000);
 
          // register a failover listener
          SimpleFailoverListener failoverListener = new SimpleFailoverListener();
          ((JBossConnection)conn).registerFailoverListener(failoverListener);
 
          log.debug("killing node 1 ....");
 
          ServerManagement.kill(1);
 
          log.info("########");
          log.info("######## KILLED NODE 1");
          log.info("########");
 
          // wait for the client-side failover to complete
 
          while(true)
          {
             FailoverEvent event = failoverListener.getEvent(120000);
             if (event != null && FailoverEvent.FAILOVER_COMPLETED == event.getType())
             {
                break;
             }
             if (event == null)
             {
                fail("Did not get expected FAILOVER_COMPLETED event");
             }
          }
 
          // failover complete
          log.info("failover completed");
 
          assertEquals(0, ((JBossConnection)conn).getServerID());
 
          TextMessage rm = (TextMessage)cons.receive(2000);
          assertNotNull(rm);
          assertEquals("nik", rm.getText());
       }
       finally
       {
          if (conn != null)
          {
             conn.close();
          }
       }
    }
 
    public void testBrowserFailoverSendMessagesPreFailure() throws Exception
    {
       Connection conn = null;
 
       try
       {
          // skip connection to node 0
          conn = cf.createConnection();
          conn.close();
 
          // create a connection to node 1
          conn = cf.createConnection();
 
          assertEquals(1, ((JBossConnection)conn).getServerID());
 
          Session session = conn.createSession(false, Session.AUTO_ACKNOWLEDGE);
 
          QueueBrowser browser = session.createBrowser(queue[1]);
 
          Enumeration en = browser.getEnumeration();
          assertFalse(en.hasMoreElements());
 
          // send one persistent and one non-persistent message
 
          MessageProducer prod = session.createProducer(queue[1]);
          prod.setDeliveryMode(DeliveryMode.PERSISTENT);
          prod.send(session.createTextMessage("click"));
          prod.setDeliveryMode(DeliveryMode.NON_PERSISTENT);
          prod.send(session.createTextMessage("clack"));
 
          // register a failover listener
          SimpleFailoverListener failoverListener = new SimpleFailoverListener();
          ((JBossConnection)conn).registerFailoverListener(failoverListener);
 
          log.debug("killing node 1 ....");
 
          ServerManagement.kill(1);
 
          log.info("########");
          log.info("######## KILLED NODE 1");
          log.info("########");
 
          // wait for the client-side failover to complete
 
          while(true)
          {
             FailoverEvent event = failoverListener.getEvent(120000);
             if (event != null && FailoverEvent.FAILOVER_COMPLETED == event.getType())
             {
                break;
             }
             if (event == null)
             {
                fail("Did not get expected FAILOVER_COMPLETED event");
             }
          }
 
          // failover complete
          log.info("failover completed");
 
          assertEquals(0, ((JBossConnection)conn).getServerID());
 
          en = browser.getEnumeration();
 
          // we expect to only be able to browse the persistent message
          assertTrue(en.hasMoreElements());
          TextMessage tm = (TextMessage)en.nextElement();
          assertEquals("click", tm.getText());
 
          assertFalse(en.hasMoreElements());
       }
       finally
       {
          if (conn != null)
          {
             conn.close();
          }
       }
    }
 
    /**
     * TODO - Must double check if this is desired browser behavior - currently, once
     *        getEnumeration() was called once, all subsequent getEnumeration() calls return
     *        the same depleted iterator.
     */
    public void testBrowserFailoverSendMessagesPostFailure() throws Exception
    {
       Connection conn = null;
 
       try
       {
          // skip connection to node 0
          conn = cf.createConnection();
          conn.close();
 
          // create a connection to node 1
          conn = cf.createConnection();
 
          assertEquals(1, ((JBossConnection)conn).getServerID());
 
          Session session = conn.createSession(false, Session.AUTO_ACKNOWLEDGE);
 
          QueueBrowser browser = session.createBrowser(queue[1]);
 
          Enumeration en = browser.getEnumeration();
          assertFalse(en.hasMoreElements());
 
          // register a failover listener
          SimpleFailoverListener failoverListener = new SimpleFailoverListener();
          ((JBossConnection)conn).registerFailoverListener(failoverListener);
 
          log.debug("killing node 1 ....");
 
          ServerManagement.kill(1);
 
          log.info("########");
          log.info("######## KILLED NODE 1");
          log.info("########");
 
          // wait for the client-side failover to complete
 
          while(true)
          {
             FailoverEvent event = failoverListener.getEvent(120000);
             if (event != null && FailoverEvent.FAILOVER_COMPLETED == event.getType())
             {
                break;
             }
             if (event == null)
             {
                fail("Did not get expected FAILOVER_COMPLETED event");
             }
          }
 
          // failover complete
          log.info("failover completed");
 
          assertEquals(0, ((JBossConnection)conn).getServerID());
 
          // send one persistent and one non-persistent message
 
          MessageProducer prod = session.createProducer(queue[1]);
          prod.setDeliveryMode(DeliveryMode.PERSISTENT);
          prod.send(session.createTextMessage("click"));
          prod.setDeliveryMode(DeliveryMode.NON_PERSISTENT);
          prod.send(session.createTextMessage("clack"));
 
          en = browser.getEnumeration();
 
          // we expect to be able to browse persistent and non-persistent messages
          Set texts = new HashSet();
 
          assertTrue(en.hasMoreElements());
          TextMessage tm = (TextMessage)en.nextElement();
          texts.add(tm.getText());
 
          assertTrue(en.hasMoreElements());
          tm = (TextMessage)en.nextElement();
          texts.add(tm.getText());
 
          assertFalse(en.hasMoreElements());
 
          assertTrue(texts.contains("click"));
          assertTrue(texts.contains("clack"));
       }
       finally
       {
          if (conn != null)
          {
             conn.close();
          }
       }
    }
 
    /**
     * Sending one persistent message.
     */
    public void testSessionWithOneTransactedPersistentMessageFailover() throws Exception
    {
       Connection conn = null;
 
       try
       {
          // skip connection to node 0
          conn = cf.createConnection();
          conn.close();
 
          // create a connection to node 1
          conn = cf.createConnection();
 
          conn.start();
 
          assertEquals(1, ((JBossConnection)conn).getServerID());
 
          Session session = conn.createSession(true, Session.SESSION_TRANSACTED);
 
          // send 2 transacted messages (one persistent and one non-persistent) but don't commit
          MessageProducer prod = session.createProducer(queue[1]);
 
          prod.setDeliveryMode(DeliveryMode.PERSISTENT);
          prod.send(session.createTextMessage("clik-persistent"));
 
          // close the producer
          prod.close();
 
          log.debug("producer closed");
 
          // create a consumer on the same local queue (creating a consumer AFTER failover will end
          // up getting messages from a local queue, not a failed over queue; at least until
          // redistribution is implemented.
 
          Session session2 = conn.createSession(false, Session.AUTO_ACKNOWLEDGE);
          MessageConsumer cons = session2.createConsumer(queue[1]);
 
          // register a failover listener
          SimpleFailoverListener failoverListener = new SimpleFailoverListener();
          ((JBossConnection)conn).registerFailoverListener(failoverListener);
 
          log.debug("killing node 1 ....");
 
          ServerManagement.kill(1);
 
          log.info("########");
          log.info("######## KILLED NODE 1");
          log.info("########");
 
          // wait for the client-side failover to complete
 
          while(true)
          {
             FailoverEvent event = failoverListener.getEvent(120000);
             if (event != null && FailoverEvent.FAILOVER_COMPLETED == event.getType())
             {
                break;
             }
             if (event == null)
             {
                fail("Did not get expected FAILOVER_COMPLETED event");
             }
          }
 
          // failover complete
          log.info("failover completed");
 
          assertEquals(0, ((JBossConnection)conn).getServerID());
 
          // commit the failed-over session
          session.commit();
 
          // make sure messages made it to the queue
 
          TextMessage tm = (TextMessage)cons.receive(2000);
          assertNotNull(tm);
          assertEquals("clik-persistent", tm.getText());
 
       }
       finally
       {
          if (conn != null)
          {
             conn.close();
          }
       }
    }
 
    /**
     * Sending one non-persistent message.
     */
    public void testSessionWithOneTransactedNonPersistentMessageFailover() throws Exception
    {
       Connection conn = null;
 
       try
       {
          // skip connection to node 0
          conn = cf.createConnection();
          conn.close();
 
          // create a connection to node 1
          conn = cf.createConnection();
 
          conn.start();
 
          assertEquals(1, ((JBossConnection)conn).getServerID());
 
          Session session = conn.createSession(true, Session.SESSION_TRANSACTED);
 
          // send 2 transacted messages (one persistent and one non-persistent) but don't commit
          MessageProducer prod = session.createProducer(queue[1]);
 
          prod.setDeliveryMode(DeliveryMode.NON_PERSISTENT);
          prod.send(session.createTextMessage("clik-non-persistent"));
 
          // close the producer
          prod.close();
 
          log.debug("producer closed");
 
          // create a consumer on the same local queue (creating a consumer AFTER failover will end
          // up getting messages from a local queue, not a failed over queue; at least until
          // redistribution is implemented.
 
          Session session2 = conn.createSession(false, Session.AUTO_ACKNOWLEDGE);
          MessageConsumer cons = session2.createConsumer(queue[1]);
 
          // register a failover listener
          SimpleFailoverListener failoverListener = new SimpleFailoverListener();
          ((JBossConnection)conn).registerFailoverListener(failoverListener);
 
          log.debug("killing node 1 ....");
 
          ServerManagement.kill(1);
 
          log.info("########");
          log.info("######## KILLED NODE 1");
          log.info("########");
 
          // wait for the client-side failover to complete
 
          while(true)
          {
             FailoverEvent event = failoverListener.getEvent(120000);
             if (event != null && FailoverEvent.FAILOVER_COMPLETED == event.getType())
             {
                break;
             }
             if (event == null)
             {
                fail("Did not get expected FAILOVER_COMPLETED event");
             }
          }
 
          // failover complete
          log.info("failover completed");
 
          assertEquals(0, ((JBossConnection)conn).getServerID());
 
          // commit the failed-over session
          session.commit();
 
          // make sure messages made it to the queue
 
          TextMessage tm = (TextMessage)cons.receive(2000);
          assertNotNull(tm);
          assertEquals("clik-non-persistent", tm.getText());
 
       }
       finally
       {
          if (conn != null)
          {
             conn.close();
          }
       }
    }
 
    /**
     * Sending 2 non-persistent messages.
     */
    public void testSessionWithTwoTransactedNonPersistentMessagesFailover() throws Exception
    {
       Connection conn = null;
 
       try
       {
          // skip connection to node 0
          conn = cf.createConnection();
          conn.close();
 
          // create a connection to node 1
          conn = cf.createConnection();
 
          conn.start();
 
          assertEquals(1, ((JBossConnection)conn).getServerID());
 
          Session session = conn.createSession(true, Session.SESSION_TRANSACTED);
 
          // send 2 transacted messages (one persistent and one non-persistent) but don't commit
          MessageProducer prod = session.createProducer(queue[1]);
 
          prod.setDeliveryMode(DeliveryMode.NON_PERSISTENT);
          prod.send(session.createTextMessage("clik-non-persistent"));
          prod.send(session.createTextMessage("clak-non-persistent"));
 
          // close the producer
          prod.close();
 
          log.debug("producer closed");
 
          // create a consumer on the same local queue (creating a consumer AFTER failover will end
          // up getting messages from a local queue, not a failed over queue; at least until
          // redistribution is implemented.
 
          Session session2 = conn.createSession(false, Session.AUTO_ACKNOWLEDGE);
          MessageConsumer cons = session2.createConsumer(queue[1]);
 
          // register a failover listener
          SimpleFailoverListener failoverListener = new SimpleFailoverListener();
          ((JBossConnection)conn).registerFailoverListener(failoverListener);
 
          log.debug("killing node 1 ....");
 
          ServerManagement.kill(1);
 
          log.info("########");
          log.info("######## KILLED NODE 1");
          log.info("########");
 
          // wait for the client-side failover to complete
 
          while(true)
          {
             FailoverEvent event = failoverListener.getEvent(120000);
             if (event != null && FailoverEvent.FAILOVER_COMPLETED == event.getType())
             {
                break;
             }
             if (event == null)
             {
                fail("Did not get expected FAILOVER_COMPLETED event");
             }
          }
 
          // failover complete
          log.info("failover completed");
 
          assertEquals(0, ((JBossConnection)conn).getServerID());
 
          // commit the failed-over session
          session.commit();
 
          // make sure messages made it to the queue
 
          TextMessage tm = (TextMessage)cons.receive(2000);
          assertNotNull(tm);
          assertEquals("clik-non-persistent", tm.getText());
 
          tm = (TextMessage)cons.receive(2000);
          assertNotNull(tm);
          assertEquals("clak-non-persistent", tm.getText());
       }
       finally
       {
          if (conn != null)
          {
             conn.close();
          }
       }
    }
 
    /**
     * Sending 2 persistent messages.
     */
    public void testSessionWithTwoTransactedPersistentMessagesFailover() throws Exception
    {
       Connection conn = null;
 
       try
       {
          // skip connection to node 0
          conn = cf.createConnection();
          conn.close();
 
          // create a connection to node 1
          conn = cf.createConnection();
 
          conn.start();
 
          assertEquals(1, ((JBossConnection)conn).getServerID());
 
          Session session = conn.createSession(true, Session.SESSION_TRANSACTED);
 
          // send 2 transacted messages (one persistent and one non-persistent) but don't commit
          MessageProducer prod = session.createProducer(queue[1]);
 
          prod.setDeliveryMode(DeliveryMode.PERSISTENT);
          prod.send(session.createTextMessage("clik-persistent"));
          prod.send(session.createTextMessage("clak-persistent"));
 
          // close the producer
          prod.close();
 
          log.debug("producer closed");
 
          // create a consumer on the same local queue (creating a consumer AFTER failover will end
          // up getting messages from a local queue, not a failed over queue; at least until
          // redistribution is implemented.
 
          Session session2 = conn.createSession(false, Session.AUTO_ACKNOWLEDGE);
          MessageConsumer cons = session2.createConsumer(queue[1]);
 
          // register a failover listener
          SimpleFailoverListener failoverListener = new SimpleFailoverListener();
          ((JBossConnection)conn).registerFailoverListener(failoverListener);
 
          log.debug("killing node 1 ....");
 
          ServerManagement.kill(1);
 
          log.info("########");
          log.info("######## KILLED NODE 1");
          log.info("########");
 
          // wait for the client-side failover to complete
 
          while(true)
          {
             FailoverEvent event = failoverListener.getEvent(120000);
             if (event != null && FailoverEvent.FAILOVER_COMPLETED == event.getType())
             {
                break;
             }
             if (event == null)
             {
                fail("Did not get expected FAILOVER_COMPLETED event");
             }
          }
 
          // failover complete
          log.info("failover completed");
 
          assertEquals(0, ((JBossConnection)conn).getServerID());
 
          // commit the failed-over session
          session.commit();
 
          // make sure messages made it to the queue
 
          TextMessage tm = (TextMessage)cons.receive(2000);
          assertNotNull(tm);
          assertEquals("clik-persistent", tm.getText());
 
          tm = (TextMessage)cons.receive(2000);
          assertNotNull(tm);
          assertEquals("clak-persistent", tm.getText());
 
       }
       finally
       {
          if (conn != null)
          {
             conn.close();
          }
       }
    }
 
    /**
     * Sending a mix of persistent and non-persistent messages.
     */
    public void testSessionWithTwoTransactedMixedMessagesFailover() throws Exception
    {
       Connection conn = null;
 
       try
       {
          // skip connection to node 0
          conn = cf.createConnection();
          conn.close();
 
          // create a connection to node 1
          conn = cf.createConnection();
 
          conn.start();
 
          assertEquals(1, ((JBossConnection)conn).getServerID());
 
          Session session = conn.createSession(true, Session.SESSION_TRANSACTED);
 
          // send 2 transacted messages (one persistent and one non-persistent) but don't commit
          MessageProducer prod = session.createProducer(queue[1]);
 
          prod.setDeliveryMode(DeliveryMode.NON_PERSISTENT);
          prod.send(session.createTextMessage("clik-non-persistent"));
 
          prod.setDeliveryMode(DeliveryMode.PERSISTENT);
          prod.send(session.createTextMessage("clak-persistent"));
 
          // close the producer
          prod.close();
 
          log.debug("producer closed");
 
          // create a consumer on the same local queue (creating a consumer AFTER failover will end
          // up getting messages from a local queue, not a failed over queue; at least until
          // redistribution is implemented.
 
          Session session2 = conn.createSession(false, Session.AUTO_ACKNOWLEDGE);
          MessageConsumer cons = session2.createConsumer(queue[1]);
 
          // register a failover listener
          SimpleFailoverListener failoverListener = new SimpleFailoverListener();
          ((JBossConnection)conn).registerFailoverListener(failoverListener);
 
          log.debug("killing node 1 ....");
 
          ServerManagement.kill(1);
 
          log.info("########");
          log.info("######## KILLED NODE 1");
          log.info("########");
 
          // wait for the client-side failover to complete
 
          while(true)
          {
             FailoverEvent event = failoverListener.getEvent(120000);
             if (event != null && FailoverEvent.FAILOVER_COMPLETED == event.getType())
             {
                break;
             }
             if (event == null)
             {
                fail("Did not get expected FAILOVER_COMPLETED event");
             }
          }
 
          // failover complete
          log.info("failover completed");
 
          assertEquals(0, ((JBossConnection)conn).getServerID());
 
          // commit the failed-over session
          session.commit();
 
          // make sure messages made it to the queue
 
          TextMessage tm = (TextMessage)cons.receive(2000);
          assertNotNull(tm);
          assertEquals("clik-non-persistent", tm.getText());
 
          tm = (TextMessage)cons.receive(2000);
          assertNotNull(tm);
          assertEquals("clak-persistent", tm.getText());
 
       }
       finally
       {
          if (conn != null)
          {
             conn.close();
          }
       }
    }
 
    public void testSessionWithAcknowledgmentsFailover() throws Exception
    {
       Connection conn = null;
 
       try
       {
          // skip connection to node 0
          conn = cf.createConnection();
          conn.close();
 
          // create a connection to node 1
          conn = cf.createConnection();
 
          conn.start();
 
          assertEquals(1, ((JBossConnection)conn).getServerID());
 
          Session session = conn.createSession(false, Session.CLIENT_ACKNOWLEDGE);
 
          // send 2 messages (one persistent and one non-persistent)
 
          MessageProducer prod = session.createProducer(queue[1]);
 
          prod.setDeliveryMode(DeliveryMode.PERSISTENT);
          prod.send(session.createTextMessage("clik-persistent"));
          prod.setDeliveryMode(DeliveryMode.NON_PERSISTENT);
          prod.send(session.createTextMessage("clak-non-persistent"));
 
          // close the producer
          prod.close();
 
          // create a consumer and receive messages, but don't acknowledge
 
          MessageConsumer cons = session.createConsumer(queue[1]);
          TextMessage clik = (TextMessage)cons.receive(2000);
          assertEquals("clik-persistent", clik.getText());
          TextMessage clak = (TextMessage)cons.receive(2000);
          assertEquals("clak-non-persistent", clak.getText());
 
          // register a failover listener
          SimpleFailoverListener failoverListener = new SimpleFailoverListener();
          ((JBossConnection)conn).registerFailoverListener(failoverListener);
 
          log.debug("killing node 1 ....");
 
          ServerManagement.kill(1);
 
          log.info("########");
          log.info("######## KILLED NODE 1");
          log.info("########");
 
          // wait for the client-side failover to complete
 
          while(true)
          {
             FailoverEvent event = failoverListener.getEvent(120000);
             if (event != null && FailoverEvent.FAILOVER_COMPLETED == event.getType())
             {
                break;
             }
             if (event == null)
             {
                fail("Did not get expected FAILOVER_COMPLETED event");
             }
          }
 
          // failover complete
          log.info("failover completed");
 
          assertEquals(0, ((JBossConnection)conn).getServerID());
 
          // acknowledge the messages
          clik.acknowledge();
          clak.acknowledge();
 
          // make sure no messages are left in the queue
          Message m = cons.receive(1000);
          assertNull(m);
       }
       finally
       {
          if (conn != null)
          {
             conn.close();
          }
       }
    }
 
    public void testTransactedSessionWithAcknowledgmentsCommitOnFailover() throws Exception
    {
       Connection conn = null;
 
       try
       {
          // skip connection to node 0
          conn = cf.createConnection();
          conn.close();
 
          // create a connection to node 1
          conn = cf.createConnection();
 
          conn.start();
 
          assertEquals(1, ((JBossConnection)conn).getServerID());
 
          Session session = conn.createSession(true, Session.SESSION_TRANSACTED);
 
          // send 2 messages (one persistent and one non-persistent)
 
          MessageProducer prod = session.createProducer(queue[1]);
 
          prod.setDeliveryMode(DeliveryMode.PERSISTENT);
          prod.send(session.createTextMessage("clik-persistent"));
          prod.setDeliveryMode(DeliveryMode.NON_PERSISTENT);
          prod.send(session.createTextMessage("clak-non-persistent"));
 
          session.commit();
 
          // close the producer
          prod.close();
 
          // create a consumer and receive messages, but don't acknowledge
 
          MessageConsumer cons = session.createConsumer(queue[1]);
          TextMessage clik = (TextMessage)cons.receive(2000);
          assertEquals("clik-persistent", clik.getText());
          TextMessage clak = (TextMessage)cons.receive(2000);
          assertEquals("clak-non-persistent", clak.getText());
 
          // register a failover listener
          SimpleFailoverListener failoverListener = new SimpleFailoverListener();
          ((JBossConnection)conn).registerFailoverListener(failoverListener);
 
          log.debug("killing node 1 ....");
 
          ServerManagement.kill(1);
 
          log.info("########");
          log.info("######## KILLED NODE 1");
          log.info("########");
 
          // wait for the client-side failover to complete
 
          while(true)
          {
             FailoverEvent event = failoverListener.getEvent(120000);
             if (event != null && FailoverEvent.FAILOVER_COMPLETED == event.getType())
             {
                break;
             }
             if (event == null)
             {
                fail("Did not get expected FAILOVER_COMPLETED event");
             }
          }
 
          // failover complete
          log.info("failover completed");
 
          assertEquals(0, ((JBossConnection)conn).getServerID());
 
          // acknowledge the messages
          session.commit();
 
          // make sure no messages are left in the queue
          Message m = cons.receive(1000);
          assertNull(m);
       }
       finally
       {
          if (conn != null)
          {
             conn.close();
          }
       }
    }
 
    public void testTransactedSessionWithAcknowledgmentsRollbackOnFailover() throws Exception
    {
       Connection conn = null;
 
       try
       {
          // skip connection to node 0
          conn = cf.createConnection();
          conn.close();
 
          // create a connection to node 1
          conn = cf.createConnection();
 
          conn.start();
 
          assertEquals(1, ((JBossConnection)conn).getServerID());
 
          Session session = conn.createSession(true, Session.SESSION_TRANSACTED);
 
          // send 2 messages (one persistent and one non-persistent)
 
          MessageProducer prod = session.createProducer(queue[1]);
 
          prod.setDeliveryMode(DeliveryMode.PERSISTENT);
          prod.send(session.createTextMessage("clik-persistent"));
          prod.setDeliveryMode(DeliveryMode.NON_PERSISTENT);
          prod.send(session.createTextMessage("clak-non-persistent"));
 
          session.commit();
 
          // close the producer
          prod.close();
 
          // create a consumer and receive messages, but don't acknowledge
 
          MessageConsumer cons = session.createConsumer(queue[1]);
          TextMessage clik = (TextMessage)cons.receive(2000);
          assertEquals("clik-persistent", clik.getText());
          TextMessage clak = (TextMessage)cons.receive(2000);
          assertEquals("clak-non-persistent", clak.getText());
 
          // register a failover listener
          SimpleFailoverListener failoverListener = new SimpleFailoverListener();
          ((JBossConnection)conn).registerFailoverListener(failoverListener);
 
          log.debug("killing node 1 ....");
 
          ServerManagement.kill(1);
 
          log.info("########");
          log.info("######## KILLED NODE 1");
          log.info("########");
 
          // wait for the client-side failover to complete
 
          while(true)
          {
             FailoverEvent event = failoverListener.getEvent(120000);
             if (event != null && FailoverEvent.FAILOVER_COMPLETED == event.getType())
             {
                break;
             }
             if (event == null)
             {
                fail("Did not get expected FAILOVER_COMPLETED event");
             }
          }
 
          // failover complete
          log.info("failover completed");
 
          assertEquals(0, ((JBossConnection)conn).getServerID());
 
          session.rollback();
 
          TextMessage m = (TextMessage)cons.receive(2000);
          assertNotNull(m);
          assertEquals("clik-persistent", m.getText());
 
          m = (TextMessage)cons.receive(2000);
          assertNull(m);
       }
       finally
       {
          if (conn != null)
          {
             conn.close();
          }
       }
    }
 
 
    public void testFailoverListener() throws Exception
    {
       Connection conn = null;
 
       try
       {
          conn = cf.createConnection();
          conn.close();
 
          conn = cf.createConnection();
          conn.start();
 
          // create a producer/consumer on node 1 and make sure we're connecting to node 1
 
          int nodeID = ((ConnectionState)((DelegateSupport)((JBossConnection)conn).
             getDelegate()).getState()).getServerID();
 
          assertEquals(1, nodeID);
 
          SimpleFailoverListener failoverListener = new SimpleFailoverListener();
          ((JBossConnection)conn).registerFailoverListener(failoverListener);
 
          // kill node 1
 
          log.debug("killing node 1");
 
          ServerManagement.kill(1);
 
          log.info("########");
          log.info("######## KILLED NODE 1");
          log.info("########");
 
          FailoverEvent event = failoverListener.getEvent(120000);
 
          assertNotNull(event);
          assertEquals(FailoverEvent.FAILURE_DETECTED, event.getType());
          log.info("got " + event);
 
          event = failoverListener.getEvent(120000);
 
          assertNotNull(event);
          assertEquals(FailoverEvent.FAILOVER_STARTED, event.getType());
          log.info("got " + event);
 
          event = failoverListener.getEvent(120000);
 
          assertNotNull(event);
          assertEquals(FailoverEvent.FAILOVER_COMPLETED, event.getType());
          log.info("got " + event);
       }
       finally
       {
          if (conn != null)
          {
             conn.close();
          }
       }
    }
 
    public void testFailoverMessageOnServer() throws Exception
    {
       Connection conn = null;
 
       try
       {
          conn = cf.createConnection();
          conn.close();
 
          conn = cf.createConnection();
          conn.start();
 
          assertEquals(1, ((JBossConnection)conn).getServerID());
 
          SimpleFailoverListener listener = new SimpleFailoverListener();
          ((JBossConnection)conn).registerFailoverListener(listener);
 
          Session session = conn.createSession(false, Session.AUTO_ACKNOWLEDGE);
          MessageProducer prod = session.createProducer(queue[1]);
          prod.setDeliveryMode(DeliveryMode.PERSISTENT);
          MessageConsumer cons = session.createConsumer(queue[0]);
 
          // send a message
 
          prod.send(session.createTextMessage("blip"));
 
          // kill node 1
 
          log.debug("killing node 1");
 
          ServerManagement.kill(1);
 
          log.info("########");
          log.info("######## KILLED NODE 1");
          log.info("########");
 
          // wait until the failure (not the completion of client-side failover) is detected
 
          while(true)
          {
             FailoverEvent event = listener.getEvent(120000);
             if (event != null && FailoverEvent.FAILOVER_STARTED == event.getType())
             {
                break;
             }
             if (event == null)
             {
                fail("Did not get expected FAILOVER_COMPLETED event");
             }
          }
 
          // start to receive the very next moment the failure is detected. This way, we also
          // test the client-side failover valve
 
          TextMessage tm = (TextMessage)cons.receive(60000);
          assertNotNull(tm);
          assertEquals("blip", tm.getText());
       }
       finally
       {
          if (conn != null)
          {
             conn.close();
          }
       }
    }
 
    public void testFailoverMessageOnServer2() throws Exception
    {
       Connection conn = null;
 
       try
       {
          conn = cf.createConnection();
          conn.close();
 
          conn = cf.createConnection();
          conn.start();
 
          assertEquals(1, ((JBossConnection)conn).getServerID());
 
          SimpleFailoverListener listener = new SimpleFailoverListener();
          ((JBossConnection)conn).registerFailoverListener(listener);
 
          Session session = conn.createSession(false, Session.AUTO_ACKNOWLEDGE);
          MessageProducer prod = session.createProducer(queue[1]);
          prod.setDeliveryMode(DeliveryMode.PERSISTENT);
 
          // send a message
 
          prod.send(session.createTextMessage("blip"));
 
          // kill node 1
 
          log.debug("killing node 1");
 
          ServerManagement.kill(1);
 
          log.info("########");
          log.info("######## KILLED NODE 1");
          log.info("########");
 
          // wait until the failure (not the completion of client-side failover) is detected
 
          assertEquals(FailoverEvent.FAILURE_DETECTED, listener.getEvent(60000).getType());
 
          // create a consumer the very next moment the failure is detected. This way, we also
          // test the client-side failover valve
 
          MessageConsumer cons = session.createConsumer(queue[0]);
 
          // we must receive the message
 
          TextMessage tm = (TextMessage)cons.receive(60000);
          assertEquals("blip", tm.getText());
       }
       finally
       {
          if (conn != null)
          {
             conn.close();
          }
       }
    }
 
    public void testSimpleFailover() throws Exception
    {
       simpleFailover(null, null);
    }
 
    public void testSimpleFailoverUserPassword() throws Exception
    {
       final String testTopicConf =
          "<security>" +
             "<role name=\"guest\" read=\"true\" write=\"true\"/>" +
             "<role name=\"publisher\" read=\"true\" write=\"true\" create=\"false\"/>" +
             "<role name=\"durpublisher\" read=\"true\" write=\"true\" create=\"true\"/>" +
          "</security>";
 
       ServerManagement.configureSecurityForDestination(0, "testDistributedQueue", testTopicConf);
       ServerManagement.configureSecurityForDestination(1, "testDistributedQueue", testTopicConf);
 
       simpleFailover("john", "needle");
    }
 
    public void testMethodSmackingIntoFailure() throws Exception
    {
       Connection conn = null;
 
       try
       {
          conn = cf.createConnection();
          conn.close();
 
          conn = cf.createConnection();
 
          assertEquals(1, ((JBossConnection)conn).getServerID());
 
          // we "cripple" the remoting connection by removing ConnectionListener. This way, failures
          // cannot be "cleanly" detected by the client-side pinger, and we'll fail on an invocation
          JMSRemotingConnection rc = ((ClientConnectionDelegate)((JBossConnection)conn).
             getDelegate()).getRemotingConnection();
          rc.removeConnectionListener();
 
          ServerManagement.killAndWait(1);
 
          log.info("########");
          log.info("######## KILLED NODE 1");
          log.info("########");
 
          conn.createSession(false, Session.AUTO_ACKNOWLEDGE);
       }
       finally
       {
          if (conn != null)
          {
             conn.close();
          }
       }
    }
 
    public void testFailureInTheMiddleOfAnInvocation() throws Exception
    {
       Connection conn = null;
 
       try
       {
          conn = cf.createConnection();
          conn.close();
 
          conn = cf.createConnection();
 
          assertEquals(1, ((JBossConnection)conn).getServerID());
 
          // we "cripple" the remoting connection by removing ConnectionListener. This way, failures
          // cannot be "cleanly" detected by the client-side pinger, and we'll fail on an invocation
          JMSRemotingConnection rc = ((ClientConnectionDelegate)((JBossConnection)conn).
             getDelegate()).getRemotingConnection();
          rc.removeConnectionListener();
 
          SimpleFailoverListener failoverListener = new SimpleFailoverListener();
          ((JBossConnection)conn).registerFailoverListener(failoverListener);
 
          // poison the server
          ServerManagement.poisonTheServer(1, PoisonInterceptor.TYPE_CREATE_SESSION);
 
          // this invocation will halt the server ...
          Session session = conn.createSession(false, Session.AUTO_ACKNOWLEDGE);
 
          // ... and hopefully it be failed over
 
          MessageConsumer cons = session.createConsumer(queue[0]);
          MessageProducer prod = session.createProducer(queue[0]);
 
          prod.send(session.createTextMessage("after-poison"));
 
          conn.start();
 
          TextMessage tm = (TextMessage)cons.receive(2000);
 
          assertNotNull(tm);
          assertEquals("after-poison", tm.getText());
 
       }
       finally
       {
          if (conn != null)
          {
             conn.close();
          }
       }
    }
 
    public void testSimpleFailoverWithRemotingListenerEnabled() throws Exception
    {
       Connection conn = null;
 
       try
       {
          conn = cf.createConnection();
          conn.close();
 
          conn = cf.createConnection();
          conn.start();
 
          // make sure we're connecting to node 1
 
          int nodeID = ((ConnectionState)((DelegateSupport)((JBossConnection)conn).
             getDelegate()).getState()).getServerID();
 
          assertEquals(1, nodeID);
 
          Session s1 = conn.createSession(false, Session.AUTO_ACKNOWLEDGE);
          MessageConsumer c1 = s1.createConsumer(queue[1]);
          MessageProducer p1 = s1.createProducer(queue[1]);
          p1.setDeliveryMode(DeliveryMode.PERSISTENT);
 
          // send a message
 
          p1.send(s1.createTextMessage("blip"));
 
          // kill node 1
 
          ServerManagement.killAndWait(1);
          log.info("########");
          log.info("######## KILLED NODE 1");
          log.info("########");
 
          try
          {
             ic[1].lookup("queue"); // looking up anything
             fail("The server still alive, kill didn't work yet");
          }
          catch (Exception e)
          {
          }
 
          // we must receive the message
 
          TextMessage tm = (TextMessage)c1.receive(1000);
          assertEquals("blip", tm.getText());
 
       }
       finally
       {
          if (conn != null)
          {
             conn.close();
          }
       }
    }
 
    // http://jira.jboss.org/jira/browse/JBMESSAGING-808
    public void testFailureRightAfterACK() throws Exception
    {
       failureOnInvocation(PoisonInterceptor.FAIL_AFTER_ACKNOWLEDGE_DELIVERY);
    }
 
    // http://jira.jboss.org/jira/browse/JBMESSAGING-808
    public void testFailureRightBeforeACK() throws Exception
    {
       failureOnInvocation(PoisonInterceptor.FAIL_BEFORE_ACKNOWLEDGE_DELIVERY);
    }
 
    public void testFailureRightBeforeSend() throws Exception
    {
       failureOnInvocation(PoisonInterceptor.FAIL_BEFORE_SEND);
    }
 
    public void testFailureRightAfterSend() throws Exception
    {
       failureOnInvocation(PoisonInterceptor.FAIL_AFTER_SEND);
    }
 
    public void testFailureRightAFterSendTransaction() throws Exception
    {
       Connection conn = null;
       Connection conn0 = null;
 
       try
       {
          conn0 = cf.createConnection();
 
          assertEquals(0, ((JBossConnection)conn0).getServerID());
 
          Session session0 = conn0.createSession(true, Session.SESSION_TRANSACTED);
 
          MessageConsumer consumer0 = session0.createConsumer(queue[0]);
 
          conn0.start();
 
          conn = cf.createConnection();
 
          assertEquals(1, ((JBossConnection)conn).getServerID());
 
          // we "cripple" the remoting connection by removing ConnectionListener. This way, failures
          // cannot be "cleanly" detected by the client-side pinger, and we'll fail on an invocation
          JMSRemotingConnection rc = ((ClientConnectionDelegate)((JBossConnection)conn).
             getDelegate()).getRemotingConnection();
          rc.removeConnectionListener();
 
          // poison the server
          ServerManagement.poisonTheServer(1, PoisonInterceptor.FAIL_AFTER_SENDTRANSACTION);
 
          Session session = conn.createSession(true, Session.SESSION_TRANSACTED);
 
          conn.start();
 
          MessageProducer producer = session.createProducer(queue[0]);
 
          producer.setDeliveryMode(DeliveryMode.PERSISTENT);
 
          MessageConsumer consumer = session.createConsumer(queue[0]);
 
          producer.send(session.createTextMessage("before-poison1"));
          producer.send(session.createTextMessage("before-poison2"));
          producer.send(session.createTextMessage("before-poison3"));
          session.commit();
 
          Thread.sleep(2000);
 
          for (int i = 1; i <= 3; i++)
          {
             TextMessage tm = (TextMessage) consumer.receive(5000);
 
             assertNotNull(tm);
 
             assertEquals("before-poison" + i, tm.getText());
          }
 
          assertNull(consumer.receive(1000));
          assertNull(consumer0.receive(5000));
 
       }
       finally
       {
          if (conn != null)
          {
             conn.close();
          }
          if (conn0 != null)
          {
             conn0.close();
          }
       }
    }
 
    // Package protected ----------------------------------------------------------------------------
 
    // Protected ------------------------------------------------------------------------------------
 
    protected void setUp() throws Exception
    {
       nodeCount = 2;
 
       super.setUp();
 
       log.debug("setup done");
    }
 
    protected void tearDown() throws Exception
    {
       super.tearDown();
    }
 
    // Private --------------------------------------------------------------------------------------
 
    private void simpleFailover(String userName, String password) throws Exception
    {
       Connection conn = null;
 
       try
       {
          if (userName!=null)
          {
             conn = cf.createConnection(userName, password);
          }
          else
          {
             conn = cf.createConnection();
          }
          conn.close();
 
          if (userName!=null)
          {
             conn = cf.createConnection(userName, password);
          }
          else
          {
             conn = cf.createConnection();
          }
          conn.start();
 
          // Disable Lease for this test.. as the ValveAspect should capture this
          getConnectionState(conn).getRemotingConnection().removeConnectionListener();
 
          // make sure we're connecting to node 1
 
          int nodeID = ((ConnectionState)((DelegateSupport)((JBossConnection)conn).
             getDelegate()).getState()).getServerID();
 
          assertEquals(1, nodeID);
 
          Session s1 = conn.createSession(false, Session.AUTO_ACKNOWLEDGE);
          MessageConsumer c1 = s1.createConsumer(queue[1]);
          MessageProducer p1 = s1.createProducer(queue[1]);
          p1.setDeliveryMode(DeliveryMode.PERSISTENT);
 
          // send a message
 
          p1.send(s1.createTextMessage("blip"));
 
          // kill node 1
 
          ServerManagement.killAndWait(1);
          log.info("########");
          log.info("######## KILLED NODE 1");
          log.info("########");
 
          try
          {
             ic[1].lookup("queue"); // looking up anything
             fail("The server still alive, kill didn't work yet");
          }
          catch (Exception e)
          {
          }
 
          // we must receive the message
 
          TextMessage tm = (TextMessage)c1.receive(1000);
          assertEquals("blip", tm.getText());
 
       }
       finally
       {
          if (conn != null)
          {
             conn.close();
          }
       }
    }
 
    // Used for both testFailureRightAfterACK and  testFailureRightBeforeACK
    private void failureOnInvocation(int typeOfFailure) throws Exception
    {
       Connection conn = null;
       Connection conn0 = null;
 
       try
       {
          conn0 = cf.createConnection();
 
          assertEquals(0, ((JBossConnection)conn0).getServerID());
 
         Session session0 = conn0.createSession(false, Session.AUTO_ACKNOWLEDGE);

         MessageConsumer consumer0 = session0.createConsumer(queue[0]);

         conn0.start();
 
          conn = cf.createConnection();
 
          assertEquals(1, ((JBossConnection)conn).getServerID());
 
          // we "cripple" the remoting connection by removing ConnectionListener. This way, failures
          // cannot be "cleanly" detected by the client-side pinger, and we'll fail on an invocation
          JMSRemotingConnection rc = ((ClientConnectionDelegate)((JBossConnection)conn).
             getDelegate()).getRemotingConnection();
          rc.removeConnectionListener();
 
          // poison the server
          ServerManagement.poisonTheServer(1, typeOfFailure);
 
          Session session = conn.createSession(false, Session.AUTO_ACKNOWLEDGE);
 
          conn.start();
 
          MessageProducer producer = session.createProducer(queue[0]);
 
          producer.setDeliveryMode(DeliveryMode.PERSISTENT);
 
          MessageConsumer consumer = session.createConsumer(queue[0]);
 
          producer.send(session.createTextMessage("before-poison"));
 
          TextMessage tm = (TextMessage)consumer.receive(5000);
 
          assertNotNull(tm);
 
          assertEquals("before-poison", tm.getText());
 
          tm = (TextMessage)consumer.receive(1000);
 
          assertNull(tm);
 
         assertNull(consumer0.receive(5000));

       }
       finally
       {
          if (conn != null)
          {
             conn.close();
          }
          if (conn0 != null)
          {
             conn0.close();
          }
       }
    }
 
    // Inner classes --------------------------------------------------------------------------------
    
 }
