 /*
  * Copyright 2009 Red Hat, Inc.
  * Red Hat licenses this file to you under the Apache License, version
  * 2.0 (the "License"); you may not use this file except in compliance
  * with the License.  You may obtain a copy of the License at
  *    http://www.apache.org/licenses/LICENSE-2.0
  * Unless required by applicable law or agreed to in writing, software
  * distributed under the License is distributed on an "AS IS" BASIS,
  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
  * implied.  See the License for the specific language governing
  * permissions and limitations under the License.
  */
 package org.hornetq.tests.integration.client;
 
 import javax.jms.Connection;
 import javax.jms.ExceptionListener;
 import javax.jms.JMSException;
 import javax.jms.Session;
 
 import org.hornetq.core.client.impl.ClientSessionInternal;
 import org.hornetq.core.config.Configuration;
 import org.hornetq.core.config.TransportConfiguration;
 import org.hornetq.core.config.impl.ConfigurationImpl;
 import org.hornetq.core.exception.HornetQException;
 import org.hornetq.core.logging.Logger;
 import org.hornetq.core.remoting.RemotingConnection;
 import org.hornetq.core.server.HornetQ;
 import org.hornetq.core.server.HornetQServer;
 import org.hornetq.jms.client.HornetQConnectionFactory;
 import org.hornetq.jms.client.HornetQSession;
 import org.hornetq.jms.server.impl.JMSServerManagerImpl;
 import org.hornetq.tests.integration.jms.server.management.NullInitialContext;
 import org.hornetq.tests.util.UnitTestCase;
 
 /**
  * 
  * A FailureDeadlockTest
  *
  * @author <a href="mailto:tim.fox@jboss.com">Tim Fox</a>
  *
  *
  */
 public class FailureDeadlockTest extends UnitTestCase
 {
    private static final Logger log = Logger.getLogger(FailureDeadlockTest.class);
 
    private HornetQServer server;
 
    private JMSServerManagerImpl jmsServer;
 
    private HornetQConnectionFactory cf1;
 
    private HornetQConnectionFactory cf2;
 
    @Override
    protected void setUp() throws Exception
    {
       super.setUp();
 
       Configuration conf = new ConfigurationImpl();
       conf.setSecurityEnabled(false);
       conf.getAcceptorConfigurations()
           .add(new TransportConfiguration("org.hornetq.core.remoting.impl.invm.InVMAcceptorFactory"));
       server = HornetQ.newHornetQServer(conf, false);
       jmsServer = new JMSServerManagerImpl(server);
       jmsServer.setContext(new NullInitialContext());
       jmsServer.start();
       cf1 = new HornetQConnectionFactory(new TransportConfiguration("org.hornetq.core.remoting.impl.invm.InVMConnectorFactory"));
 
       cf2 = new HornetQConnectionFactory(new TransportConfiguration("org.hornetq.core.remoting.impl.invm.InVMConnectorFactory"));
    }
 
    @Override
    protected void tearDown() throws Exception
    {
       if (jmsServer != null && jmsServer.isStarted())
       {
          jmsServer.stop();
       }
 
       if (server != null && server.isStarted())
       {
          try
          {
             server.stop();
          }
          catch (Exception e)
          {
             e.printStackTrace();
          }
 
       }
 
       server = null;
       
       jmsServer = null;
       
       cf1 = null;
       
       cf2 = null;
       
 
       super.tearDown();
    }
     
 
    // https://jira.jboss.org/jira/browse/JBMESSAGING-1702
    //Test that two failures concurrently executing and calling the same exception listener
    //don't deadlock
    public void testDeadlock() throws Exception
    {
       for (int i = 0; i < 100; i++)
       {
          final Connection conn1 = cf1.createConnection();
 
          Session sess1 = conn1.createSession(false, Session.AUTO_ACKNOWLEDGE);
          RemotingConnection rc1 = ((ClientSessionInternal)((HornetQSession)sess1).getCoreSession()).getConnection();
 
          final Connection conn2 = cf2.createConnection();
 
          Session sess2 = conn2.createSession(false, Session.AUTO_ACKNOWLEDGE);
          RemotingConnection rc2 = ((ClientSessionInternal)((HornetQSession)sess2).getCoreSession()).getConnection();
 
          ExceptionListener listener1 = new ExceptionListener()
          {
             public void onException(JMSException exception)
             {
                try
                {
                   conn2.close();
                }
                catch (Exception e)
                {
                   log.error("Failed to close connection2", e);
                }
             }
          };
 
          conn1.setExceptionListener(listener1);
 
          conn2.setExceptionListener(listener1);
 
          Failer f1 = new Failer(rc1);
 
          Failer f2 = new Failer(rc2);
 
          f1.start();
 
          f2.start();
 
          f1.join();
 
          f2.join();  
          
          conn1.close();
          
          conn2.close();
       }      
    }
    
    private class Failer extends Thread
    {
       RemotingConnection conn;
 
       Failer(RemotingConnection conn)
       {
          this.conn = conn;
       }
 
       public void run()
       {
          conn.fail(new HornetQException(HornetQException.NOT_CONNECTED, "blah"));
       }
    }
 
       
    //https://jira.jboss.org/jira/browse/JBMESSAGING-1703
    //Make sure that failing a connection removes it from the connection manager and can't be returned in a subsequent call
    public void testUsingDeadConnection() throws Exception
    {
       for (int i = 0; i < 100; i++)
       {
          final Connection conn1 = cf1.createConnection();
    
          Session sess1 = conn1.createSession(false, Session.AUTO_ACKNOWLEDGE);
          RemotingConnection rc1 = ((ClientSessionInternal)((HornetQSession)sess1).getCoreSession()).getConnection();      
    
          rc1.fail(new HornetQException(HornetQException.NOT_CONNECTED, "blah"));
    
         Session sess2 = conn1.createSession(false, Session.AUTO_ACKNOWLEDGE);
         
          conn1.close();
       }
    }
 
 }
