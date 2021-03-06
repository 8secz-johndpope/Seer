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
 
 package org.hornetq.tests.util;
 
 import java.util.ArrayList;
 import java.util.List;
 
 import javax.jms.ConnectionFactory;
 import javax.jms.Queue;
 import javax.jms.Topic;
 import javax.naming.NamingException;
 
 import org.hornetq.api.core.Pair;
 import org.hornetq.api.core.TransportConfiguration;
 import org.hornetq.api.core.client.HornetQClient;
 import org.hornetq.core.config.Configuration;
 import org.hornetq.core.server.HornetQServer;
 import org.hornetq.core.server.HornetQServers;
 import org.hornetq.integration.transports.netty.NettyAcceptorFactory;
 import org.hornetq.integration.transports.netty.NettyConnectorFactory;
 import org.hornetq.jms.server.impl.JMSServerManagerImpl;
 import org.hornetq.tests.unit.util.InVMContext;
 
 /**
  * A JMSBaseTest
  *
  * @author <mailto:clebert.suconic@jboss.org">Clebert Suconic</a>
  *
  *
  */
 public class JMSTestBase extends ServiceTestBase
 {
 
    protected HornetQServer server;
 
    protected JMSServerManagerImpl jmsServer;
 
    protected ConnectionFactory cf;
 
    protected InVMContext context;
 
    // Static --------------------------------------------------------
 
    // Attributes ----------------------------------------------------
 
    // Constructors --------------------------------------------------
 
    // TestCase overrides -------------------------------------------
 
    // Public --------------------------------------------------------
 
    // Package protected ---------------------------------------------
 
    // Protected -----------------------------------------------------
 
    protected boolean useSecurity()
    {
       return false;
    }
 
    protected boolean useJMX()
    {
       return true;
    }
 
    protected boolean usePersistence()
    {
       return false;
    }
 
    /**
     * @throws Exception
     * @throws NamingException
     */
    protected Queue createQueue(final String name) throws Exception, NamingException
    {
       jmsServer.createQueue(name, "/jms/" + name, null, true);
 
       return (Queue)context.lookup("/jms/" + name);
    }
    
    protected Topic createTopic(final String name) throws Exception, NamingException
    {
       jmsServer.createTopic(name, "/jms/" + name);
 
       return (Topic)context.lookup("/jms/" + name);
    }
 
    @Override
    protected void setUp() throws Exception
    {
       super.setUp();
 
       Configuration conf = createDefaultConfig(false);
       conf.setSecurityEnabled(false);
       conf.setJMXManagementEnabled(true);
 
       conf.getAcceptorConfigurations().add(new TransportConfiguration(NettyAcceptorFactory.class.getName()));
 
       server = HornetQServers.newHornetQServer(conf, usePersistence());
 
       jmsServer = new JMSServerManagerImpl(server);
       context = new InVMContext();
       jmsServer.setContext(context);
       jmsServer.start();
       jmsServer.activated();
 
       registerConnectionFactory();
    }
 
    protected void restartServer() throws Exception
    {
       jmsServer.start();
       jmsServer.activated();
       context = new InVMContext();
       jmsServer.setContext(context);
       registerConnectionFactory();
    }
 
    protected void killServer() throws Exception
    {
       jmsServer.stop();
    }
 
    @Override
    protected void tearDown() throws Exception
    {
 
       jmsServer.stop();
 
       server.stop();
 
       context.close();
 
       server = null;
 
       jmsServer = null;
 
       context = null;
 
       super.tearDown();
    }
 
    // Private -------------------------------------------------------
 
    // Inner classes -------------------------------------------------
 
    private void registerConnectionFactory() throws Exception
    {
       List<Pair<TransportConfiguration, TransportConfiguration>> connectorConfigs = new ArrayList<Pair<TransportConfiguration, TransportConfiguration>>();
       connectorConfigs.add(new Pair<TransportConfiguration, TransportConfiguration>(new TransportConfiguration(NettyConnectorFactory.class.getName()),
                                                                                     null));
 
       List<String> jndiBindings = new ArrayList<String>();
       jndiBindings.add("/cf");
 
       createCF(connectorConfigs, jndiBindings);
 
       cf = (ConnectionFactory)context.lookup("/cf");
 
    }
 
    /**
     * @param connectorConfigs
     * @param jndiBindings
     * @throws Exception
     */
    protected void createCF(final List<Pair<TransportConfiguration, TransportConfiguration>> connectorConfigs,
                            final List<String> jndiBindings) throws Exception
    {
       int retryInterval = 1000;
       double retryIntervalMultiplier = 1.0;
       int reconnectAttempts = -1;
       boolean failoverOnServerShutdown = true;
       int callTimeout = 30000;
 
       jmsServer.createConnectionFactory("ManualReconnectionToSingleServerTest",
                                         connectorConfigs,
                                         null,
                                         HornetQClient.DEFAULT_CLIENT_FAILURE_CHECK_PERIOD,
                                         HornetQClient.DEFAULT_CONNECTION_TTL,
                                         callTimeout,
                                         HornetQClient.DEFAULT_CACHE_LARGE_MESSAGE_CLIENT,
                                         HornetQClient.DEFAULT_MIN_LARGE_MESSAGE_SIZE,
                                         HornetQClient.DEFAULT_CONSUMER_WINDOW_SIZE,
                                         HornetQClient.DEFAULT_CONSUMER_MAX_RATE,
                                         HornetQClient.DEFAULT_CONFIRMATION_WINDOW_SIZE,
                                         HornetQClient.DEFAULT_PRODUCER_WINDOW_SIZE,
                                         HornetQClient.DEFAULT_PRODUCER_MAX_RATE,
                                         HornetQClient.DEFAULT_BLOCK_ON_ACKNOWLEDGE,
                                         HornetQClient.DEFAULT_BLOCK_ON_DURABLE_SEND,
                                         HornetQClient.DEFAULT_BLOCK_ON_NON_DURABLE_SEND,
                                         HornetQClient.DEFAULT_AUTO_GROUP,
                                         HornetQClient.DEFAULT_PRE_ACKNOWLEDGE,
                                         HornetQClient.DEFAULT_CONNECTION_LOAD_BALANCING_POLICY_CLASS_NAME,
                                         HornetQClient.DEFAULT_ACK_BATCH_SIZE,
                                         HornetQClient.DEFAULT_ACK_BATCH_SIZE,
                                         HornetQClient.DEFAULT_USE_GLOBAL_POOLS,
                                         HornetQClient.DEFAULT_SCHEDULED_THREAD_POOL_MAX_SIZE,
                                         HornetQClient.DEFAULT_THREAD_POOL_MAX_SIZE,
                                         retryInterval,
                                         retryIntervalMultiplier,
                                         HornetQClient.DEFAULT_MAX_RETRY_INTERVAL,
                                         reconnectAttempts,
                                         failoverOnServerShutdown,
                                         null,
                                         jndiBindings);
    }
 
 }
