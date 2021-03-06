 /*
   * JBoss, Home of Professional Open Source
   * Copyright 2005, JBoss Inc., and individual contributors as indicated
   * by the @authors tag. See the copyright.txt in the distribution for a
   * full listing of individual contributors.
   *
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
 package org.jboss.test.messaging.jms;
 
 import javax.jms.ConnectionFactory;
 import javax.jms.Queue;
 import javax.jms.Connection;
 import javax.jms.Session;
 import javax.jms.MessageProducer;
 import javax.jms.MessageConsumer;
 import javax.jms.TextMessage;
 import javax.naming.InitialContext;
 
 import org.jboss.jms.client.JBossConnectionFactory;
 import org.jboss.test.messaging.MessagingTestCase;
 import org.jboss.test.messaging.tools.ServerManagement;
 
 /**
  * @author <a href="mailto:ovidiu@jboss.org">Ovidiu Feodorov</a>
  * @version <tt>$Revision$</tt>
  *
  * $Id$
  */
 public class QueueTest extends MessagingTestCase
 {
    // Constants -----------------------------------------------------
    
    // Static --------------------------------------------------------
    
    // Attributes ----------------------------------------------------
 
    protected InitialContext ic;
    protected ConnectionFactory cf;
 
    // Constructors --------------------------------------------------
    
    public QueueTest(String name)
    {
       super(name);
    }
    
    // TestCase overrides -------------------------------------------
    
    public void setUp() throws Exception
    {
       super.setUp();                  
       
       ServerManagement.start("all");
       
       
       ic = new InitialContext(ServerManagement.getJNDIEnvironment());
       cf = (JBossConnectionFactory)ic.lookup("/ConnectionFactory");
       
       ServerManagement.undeployQueue("TestQueue");
       ServerManagement.deployQueue("TestQueue");
 
       log.debug("setup done");
    }
    
    public void tearDown() throws Exception
    {
       ServerManagement.undeployQueue("TestQueue");
       super.tearDown();
 
       log.debug("tear down done");
    }
    
    
    // Public --------------------------------------------------------
 
    /**
     * The simplest possible queue test.
     */
    public void testQueue() throws Exception
    {
       Queue queue = (Queue)ic.lookup("/queue/TestQueue");
 
       Connection conn = cf.createConnection();
 
       try
       {
          Session s = conn.createSession(false, Session.AUTO_ACKNOWLEDGE);
          MessageProducer p = s.createProducer(queue);
          MessageConsumer c = s.createConsumer(queue);
          conn.start();
 
          p.send(s.createTextMessage("payload"));
          TextMessage m = (TextMessage)c.receive();
 
          assertEquals("payload", m.getText());
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
     * The simplest possible queue test.
     */
    public void testRedeployQueue() throws Exception
    {
       Queue queue = (Queue)ic.lookup("/queue/TestQueue");
 
       Connection conn = cf.createConnection();
 
       try
       {
          Session s = conn.createSession(false, Session.AUTO_ACKNOWLEDGE);
          MessageProducer p = s.createProducer(queue);
          MessageConsumer c = s.createConsumer(queue);
          conn.start();
 
          for (int i = 0; i < 500; i++)
          {
             p.send(s.createTextMessage("payload " + i));
          }
 
          //ServerManagement.undeployQueue("TestQueue");
 
          log.info("Stopping server");
         ServerManagement.stopServerPeer();
 
          log.info("Starting server");
         ServerManagement.startServerPeer();
          ServerManagement.deployQueue("TestQueue");
 
          ic = new InitialContext(ServerManagement.getJNDIEnvironment());
          cf = (JBossConnectionFactory)ic.lookup("/ConnectionFactory");
 
          conn = cf.createConnection();
          s = conn.createSession(false, Session.AUTO_ACKNOWLEDGE);
          p = s.createProducer(queue);
          c = s.createConsumer(queue);
          conn.start();
 
          for (int i = 0; i < 500; i++)
          {
             TextMessage message = (TextMessage)c.receive(3000);
             assertNotNull(message);
             assertNotNull(message.getJMSDestination());
          }
 
       }
       finally
       {
          if (conn != null)
          {
             conn.close();
          }
       }
    }
 
 
    public void testQueueName() throws Exception
    {
       Queue queue = (Queue)ic.lookup("/queue/TestQueue");
       assertEquals("TestQueue", queue.getQueueName());
    }
 
    // Package protected ---------------------------------------------
    
    // Protected -----------------------------------------------------
    
    // Private -------------------------------------------------------
    
    // Inner classes -------------------------------------------------
    
 }
 
