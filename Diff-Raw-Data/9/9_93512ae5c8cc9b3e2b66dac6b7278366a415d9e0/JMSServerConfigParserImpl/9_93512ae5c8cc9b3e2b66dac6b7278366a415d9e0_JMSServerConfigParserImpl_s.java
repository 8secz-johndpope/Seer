 /*
  * Copyright 2010 Red Hat, Inc.
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
 
 package org.hornetq.jms.server.impl;
 
 import java.io.InputStream;
 import java.io.InputStreamReader;
 import java.io.Reader;
 import java.util.ArrayList;
 import java.util.List;
 
 import org.hornetq.api.core.HornetQException;
 import org.hornetq.api.core.Pair;
 import org.hornetq.api.core.client.HornetQClient;
 import org.hornetq.core.config.impl.Validators;
 import org.hornetq.core.logging.Logger;
 import org.hornetq.jms.server.JMSServerConfigParser;
 import org.hornetq.jms.server.config.ConnectionFactoryConfiguration;
 import org.hornetq.jms.server.config.JMSConfiguration;
 import org.hornetq.jms.server.config.JMSQueueConfiguration;
 import org.hornetq.jms.server.config.TopicConfiguration;
 import org.hornetq.jms.server.config.impl.ConnectionFactoryConfigurationImpl;
 import org.hornetq.jms.server.config.impl.JMSConfigurationImpl;
 import org.hornetq.jms.server.config.impl.JMSQueueConfigurationImpl;
 import org.hornetq.jms.server.config.impl.TopicConfigurationImpl;
 import org.hornetq.utils.XMLConfigurationUtil;
 import org.hornetq.utils.XMLUtil;
 import org.w3c.dom.Element;
 import org.w3c.dom.NamedNodeMap;
 import org.w3c.dom.Node;
 import org.w3c.dom.NodeList;
 
 /**
  * It parses JMS Configuration Files
  *
  * @author <mailto:clebert.suconic@jboss.org">Clebert Suconic</a>
  *
  *
  */
 public class JMSServerConfigParserImpl implements JMSServerConfigParser 
 {
    private static final Logger log = Logger.getLogger(JMSServerConfigParserImpl.class);
 
    // Constants -----------------------------------------------------
 
    protected static final String NAME_ATTR = "name";
 
    // Attributes ----------------------------------------------------
 
    // Static --------------------------------------------------------
 
    // Constructors --------------------------------------------------
 
    // Public --------------------------------------------------------
 
    /**
     * Parse the JMS Configuration XML as a JMSConfiguration object
     */
    public JMSConfiguration parseConfiguration(final InputStream stream) throws Exception
    {
       Reader reader = new InputStreamReader(stream);
       String xml = org.hornetq.utils.XMLUtil.readerToString(reader);
       xml = XMLUtil.replaceSystemProps(xml);
       return parseConfiguration(XMLUtil.stringToElement(xml));
    }
 
    /**
     * Parse the JMS Configuration XML as a JMSConfiguration object
     */
    public JMSConfiguration parseConfiguration(final Node rootnode) throws Exception
    {
 
       ArrayList<JMSQueueConfiguration> queues = new ArrayList<JMSQueueConfiguration>();
       ArrayList<TopicConfiguration> topics = new ArrayList<TopicConfiguration>();
       ArrayList<ConnectionFactoryConfiguration> cfs = new ArrayList<ConnectionFactoryConfiguration>();
 
       Element e = (Element)rootnode;
 
       org.hornetq.utils.XMLUtil.validate(rootnode, "schema/hornetq-jms.xsd");
 
       String elements[] = new String[] { JMSServerDeployer.QUEUE_NODE_NAME,
                                         JMSServerDeployer.TOPIC_NODE_NAME,
                                         JMSServerDeployer.CONNECTION_FACTORY_NODE_NAME };
       for (String element : elements)
       {
          NodeList children = e.getElementsByTagName(element);
          for (int i = 0; i < children.getLength(); i++)
          {
             Node node = children.item(i);
             Node keyNode = node.getAttributes().getNamedItem(JMSServerConfigParserImpl.NAME_ATTR);
             if (keyNode == null)
             {
                JMSServerConfigParserImpl.log.error("key attribute missing for configuration " + node);
                continue;
             }
 
             if (node.getNodeName().equals(JMSServerDeployer.CONNECTION_FACTORY_NODE_NAME))
             {
                cfs.add(parseConnectionFactoryConfiguration(node));
             }
             else if (node.getNodeName().equals(JMSServerDeployer.TOPIC_NODE_NAME))
             {
                topics.add(parseTopicConfiguration(node));
             }
             else if (node.getNodeName().equals(JMSServerDeployer.QUEUE_NODE_NAME))
             {
                queues.add(parseQueueConfiguration(node));
             }
          }
       }
 
       JMSConfiguration value = newConfig(queues, topics, cfs);
 
       return value;
    }
 
    /**
     * Parse the topic node as a TopicConfiguration object
     * @param node
     * @return
     * @throws Exception
     */
    public TopicConfiguration parseTopicConfiguration(final Node node) throws Exception
    {
       String topicName = node.getAttributes().getNamedItem(JMSServerConfigParserImpl.NAME_ATTR).getNodeValue();
       NodeList children = node.getChildNodes();
       ArrayList<String> jndiNames = new ArrayList<String>();
       for (int i = 0; i < children.getLength(); i++)
       {
          Node child = children.item(i);
 
          if (JMSServerDeployer.ENTRY_NODE_NAME.equals(children.item(i).getNodeName()))
          {
             String jndiElement = child.getAttributes().getNamedItem("name").getNodeValue();
             jndiNames.add(jndiElement);
          }
       }
 
       String[] strBindings = jndiNames.toArray(new String[jndiNames.size()]);
 
       return newTopic(topicName, strBindings);
 
    }
 
    /**
     * Parse the Queue Configuration node as a QueueConfiguration object
     * @param node
     * @return
     * @throws Exception
     */
    public JMSQueueConfiguration parseQueueConfiguration(final Node node) throws Exception
    {
       Element e = (Element)node;
       NamedNodeMap atts = node.getAttributes();
       String queueName = atts.getNamedItem(JMSServerConfigParserImpl.NAME_ATTR).getNodeValue();
       String selectorString = null;
       boolean durable = XMLConfigurationUtil.getBoolean(e, "durable", JMSServerDeployer.DEFAULT_QUEUE_DURABILITY);
       NodeList children = node.getChildNodes();
       ArrayList<String> jndiNames = new ArrayList<String>();
       for (int i = 0; i < children.getLength(); i++)
       {
          Node child = children.item(i);
 
          if (JMSServerDeployer.ENTRY_NODE_NAME.equals(children.item(i).getNodeName()))
          {
             String jndiName = child.getAttributes().getNamedItem("name").getNodeValue();
             jndiNames.add(jndiName);
          }
          else if (JMSServerDeployer.QUEUE_SELECTOR_NODE_NAME.equals(children.item(i).getNodeName()))
          {
             Node selectorNode = children.item(i);
             Node attNode = selectorNode.getAttributes().getNamedItem("string");
             selectorString = attNode.getNodeValue();
          }
       }
 
       String jndiArray[] = jndiNames.toArray(new String[jndiNames.size()]);
       return newQueue(queueName, selectorString, durable, jndiArray);
    }
 
    /**
     * Parse the Connection Configuration node as a ConnectionFactoryConfiguration object
     * @param node
     * @return
     * @throws Exception
     */
    public ConnectionFactoryConfiguration parseConnectionFactoryConfiguration(final Node node) throws Exception
    {
       if (!node.getNodeName().equals(JMSServerDeployer.CONNECTION_FACTORY_NODE_NAME))
       {
          // sanity check, this shouldn't ever happen
          throw new HornetQException(HornetQException.INTERNAL_ERROR, "Invalid node " + node.getNodeName() +
                                                                      " at parseConnectionFactory");
       }
       Element e = (Element)node;
 
       String name = node.getAttributes().getNamedItem(JMSServerConfigParserImpl.NAME_ATTR).getNodeValue();
 
       long clientFailureCheckPeriod = XMLConfigurationUtil.getLong(e,
                                                                    "client-failure-check-period",
                                                                    HornetQClient.DEFAULT_CLIENT_FAILURE_CHECK_PERIOD,
                                                                    Validators.MINUS_ONE_OR_GT_ZERO);
       long connectionTTL = XMLConfigurationUtil.getLong(e,
                                                         "connection-ttl",
                                                         HornetQClient.DEFAULT_CONNECTION_TTL,
                                                         Validators.MINUS_ONE_OR_GE_ZERO);
       long callTimeout = XMLConfigurationUtil.getLong(e,
                                                       "call-timeout",
                                                       HornetQClient.DEFAULT_CALL_TIMEOUT,
                                                       Validators.GE_ZERO);
       String clientID = XMLConfigurationUtil.getString(e, "client-id", null, Validators.NO_CHECK);
       int dupsOKBatchSize = XMLConfigurationUtil.getInteger(e,
                                                             "dups-ok-batch-size",
                                                             HornetQClient.DEFAULT_ACK_BATCH_SIZE,
                                                             Validators.GT_ZERO);
       int transactionBatchSize = XMLConfigurationUtil.getInteger(e,
                                                                  "transaction-batch-size",
                                                                  HornetQClient.DEFAULT_ACK_BATCH_SIZE,
                                                                  Validators.GT_ZERO);
       int consumerWindowSize = XMLConfigurationUtil.getInteger(e,
                                                                "consumer-window-size",
                                                                HornetQClient.DEFAULT_CONSUMER_WINDOW_SIZE,
                                                                Validators.MINUS_ONE_OR_GE_ZERO);
       int producerWindowSize = XMLConfigurationUtil.getInteger(e,
                                                                "producer-window-size",
                                                                HornetQClient.DEFAULT_PRODUCER_WINDOW_SIZE,
                                                                Validators.MINUS_ONE_OR_GT_ZERO);
       int consumerMaxRate = XMLConfigurationUtil.getInteger(e,
                                                             "consumer-max-rate",
                                                             HornetQClient.DEFAULT_CONSUMER_MAX_RATE,
                                                             Validators.MINUS_ONE_OR_GT_ZERO);
       int confirmationWindowSize = XMLConfigurationUtil.getInteger(e,
                                                                    "confirmation-window-size",
                                                                    HornetQClient.DEFAULT_CONFIRMATION_WINDOW_SIZE,
                                                                    Validators.MINUS_ONE_OR_GT_ZERO);
       int producerMaxRate = XMLConfigurationUtil.getInteger(e,
                                                             "producer-max-rate",
                                                             HornetQClient.DEFAULT_PRODUCER_MAX_RATE,
                                                             Validators.MINUS_ONE_OR_GT_ZERO);
       boolean cacheLargeMessagesClient = XMLConfigurationUtil.getBoolean(e,
                                                                          "cache-large-message-client",
                                                                          HornetQClient.DEFAULT_CACHE_LARGE_MESSAGE_CLIENT);
       int minLargeMessageSize = XMLConfigurationUtil.getInteger(e,
                                                                 "min-large-message-size",
                                                                 HornetQClient.DEFAULT_MIN_LARGE_MESSAGE_SIZE,
                                                                 Validators.GT_ZERO);
       boolean blockOnAcknowledge = XMLConfigurationUtil.getBoolean(e,
                                                                    "block-on-acknowledge",
                                                                    HornetQClient.DEFAULT_BLOCK_ON_ACKNOWLEDGE);
       boolean blockOnNonDurableSend = XMLConfigurationUtil.getBoolean(e,
                                                                       "block-on-non-durable-send",
                                                                       HornetQClient.DEFAULT_BLOCK_ON_NON_DURABLE_SEND);
       boolean blockOnDurableSend = XMLConfigurationUtil.getBoolean(e,
                                                                    "block-on-durable-send",
                                                                    HornetQClient.DEFAULT_BLOCK_ON_DURABLE_SEND);
       boolean autoGroup = XMLConfigurationUtil.getBoolean(e, "auto-group", HornetQClient.DEFAULT_AUTO_GROUP);
       boolean preAcknowledge = XMLConfigurationUtil.getBoolean(e,
                                                                "pre-acknowledge",
                                                                HornetQClient.DEFAULT_PRE_ACKNOWLEDGE);
       long retryInterval = XMLConfigurationUtil.getLong(e,
                                                         "retry-interval",
                                                         HornetQClient.DEFAULT_RETRY_INTERVAL,
                                                         Validators.GT_ZERO);
       double retryIntervalMultiplier = XMLConfigurationUtil.getDouble(e,
                                                                       "retry-interval-multiplier",
                                                                       HornetQClient.DEFAULT_RETRY_INTERVAL_MULTIPLIER,
                                                                       Validators.GT_ZERO);
       long maxRetryInterval = XMLConfigurationUtil.getLong(e,
                                                            "max-retry-interval",
                                                            HornetQClient.DEFAULT_MAX_RETRY_INTERVAL,
                                                            Validators.GT_ZERO);
       int reconnectAttempts = XMLConfigurationUtil.getInteger(e,
                                                               "reconnect-attempts",
                                                               HornetQClient.DEFAULT_RECONNECT_ATTEMPTS,
                                                               Validators.MINUS_ONE_OR_GE_ZERO);
       boolean failoverOnServerShutdown = XMLConfigurationUtil.getBoolean(e,
                                                                          "failover-on-server-shutdown",
                                                                          HornetQClient.DEFAULT_FAILOVER_ON_SERVER_SHUTDOWN);
       boolean useGlobalPools = XMLConfigurationUtil.getBoolean(e,
                                                                "use-global-pools",
                                                                HornetQClient.DEFAULT_USE_GLOBAL_POOLS);
       int scheduledThreadPoolMaxSize = XMLConfigurationUtil.getInteger(e,
                                                                        "scheduled-thread-pool-max-size",
                                                                        HornetQClient.DEFAULT_SCHEDULED_THREAD_POOL_MAX_SIZE,
                                                                        Validators.MINUS_ONE_OR_GT_ZERO);
       int threadPoolMaxSize = XMLConfigurationUtil.getInteger(e,
                                                               "thread-pool-max-size",
                                                               HornetQClient.DEFAULT_THREAD_POOL_MAX_SIZE,
                                                               Validators.MINUS_ONE_OR_GT_ZERO);
       String connectionLoadBalancingPolicyClassName = XMLConfigurationUtil.getString(e,
                                                                                      "connection-load-balancing-policy-class-name",
                                                                                      HornetQClient.DEFAULT_CONNECTION_LOAD_BALANCING_POLICY_CLASS_NAME,
                                                                                      Validators.NOT_NULL_OR_EMPTY);
       long discoveryInitialWaitTimeout = XMLConfigurationUtil.getLong(e,
                                                                       "discovery-initial-wait-timeout",
                                                                       HornetQClient.DEFAULT_DISCOVERY_INITIAL_WAIT_TIMEOUT,
                                                                       Validators.GT_ZERO);
       String groupid = XMLConfigurationUtil.getString(e, "group-id", null, Validators.NO_CHECK);
       List<String> jndiBindings = new ArrayList<String>();
       List<Pair<String, String>> connectorNames = new ArrayList<Pair<String,String>>();
       String discoveryGroupName = null;
 
       NodeList children = node.getChildNodes();
 
       for (int j = 0; j < children.getLength(); j++)
       {
          Node child = children.item(j);
 
          if (JMSServerDeployer.ENTRIES_NODE_NAME.equals(child.getNodeName()))
          {
             NodeList entries = child.getChildNodes();
             for (int i = 0; i < entries.getLength(); i++)
             {
                Node entry = entries.item(i);
                if (JMSServerDeployer.ENTRY_NODE_NAME.equals(entry.getNodeName()))
                {
                   String jndiName = entry.getAttributes().getNamedItem("name").getNodeValue();
 
                   jndiBindings.add(jndiName);
                }
             }
          }
          else if (JMSServerDeployer.CONNECTORS_NODE_NAME.equals(child.getNodeName()))
          {
             NodeList entries = child.getChildNodes();
             for (int i = 0; i < entries.getLength(); i++)
             {
                Node entry = entries.item(i);
                if (JMSServerDeployer.CONNECTOR_REF_ELEMENT.equals(entry.getNodeName()))
                {
                   String connectorName = entry.getAttributes().getNamedItem("connector-name").getNodeValue();
 
                   String backupConnectorName = null;
 
                   Node backupNode = entry.getAttributes().getNamedItem("backup-connector-name");
                   if (backupNode != null)
                   {
                      backupConnectorName = backupNode.getNodeValue();
                   }
                   
                   
                   connectorNames.add(new Pair<String, String>(connectorName, backupConnectorName));
                   
                   
                }
             }
          }
          else if (JMSServerDeployer.DISCOVERY_GROUP_ELEMENT.equals(child.getNodeName()))
          {
             discoveryGroupName = child.getAttributes().getNamedItem("discovery-group-name").getNodeValue();
 
          }
       }
 
       ConnectionFactoryConfiguration cfConfig;
 
       if (discoveryGroupName != null)
       {
         cfConfig = new ConnectionFactoryConfigurationImpl(name);
          cfConfig.setInitialWaitTimeout(discoveryInitialWaitTimeout);
          cfConfig.setDiscoveryGroupName(discoveryGroupName);
       }
       else
       {
         cfConfig = new ConnectionFactoryConfigurationImpl(name);
          cfConfig.setConnectorNames(connectorNames);
       }
       
       cfConfig.setInitialWaitTimeout(discoveryInitialWaitTimeout);
       cfConfig.setClientID(clientID);
       cfConfig.setClientFailureCheckPeriod(clientFailureCheckPeriod);
       cfConfig.setConnectionTTL(connectionTTL);
       cfConfig.setCallTimeout(callTimeout);
       cfConfig.setCacheLargeMessagesClient(cacheLargeMessagesClient);
       cfConfig.setMinLargeMessageSize(minLargeMessageSize);
       cfConfig.setConsumerWindowSize(consumerWindowSize);
       cfConfig.setConsumerMaxRate(consumerMaxRate);
       cfConfig.setConfirmationWindowSize(confirmationWindowSize);
       cfConfig.setProducerWindowSize(producerWindowSize);
       cfConfig.setProducerMaxRate(producerMaxRate);
       cfConfig.setBlockOnAcknowledge(blockOnAcknowledge);
       cfConfig.setBlockOnDurableSend(blockOnDurableSend);
       cfConfig.setBlockOnNonDurableSend(blockOnNonDurableSend);
       cfConfig.setAutoGroup(autoGroup);
       cfConfig.setPreAcknowledge(preAcknowledge);
       cfConfig.setLoadBalancingPolicyClassName(connectionLoadBalancingPolicyClassName);
       cfConfig.setTransactionBatchSize(transactionBatchSize);
       cfConfig.setDupsOKBatchSize(dupsOKBatchSize);
       cfConfig.setUseGlobalPools(useGlobalPools);
       cfConfig.setScheduledThreadPoolMaxSize(scheduledThreadPoolMaxSize);
       cfConfig.setThreadPoolMaxSize(threadPoolMaxSize);
       cfConfig.setRetryInterval(retryInterval);
       cfConfig.setRetryIntervalMultiplier(retryIntervalMultiplier);
       cfConfig.setMaxRetryInterval(maxRetryInterval);
       cfConfig.setReconnectAttempts(reconnectAttempts);
       cfConfig.setFailoverOnServerShutdown(failoverOnServerShutdown);
       cfConfig.setGroupID(groupid);
       return cfConfig;
    }
 
 
    /**
     * hook for integration layers 
     * @param topicName
     * @param strBindings
     * @return
     */
    protected TopicConfiguration newTopic(final String topicName, final String[] strBindings)
    {
       return new TopicConfigurationImpl(topicName, strBindings);
    }
 
    /**
     * hook for integration layers 
     * @param queueName
     * @param selectorString
     * @param durable
     * @param jndiArray
     * @return
     */
    protected JMSQueueConfiguration newQueue(final String queueName,
                                          final String selectorString,
                                          final boolean durable,
                                          final String[] jndiArray)
    {
       return new JMSQueueConfigurationImpl(queueName, selectorString, durable, jndiArray);
    }
 
    /**
     * hook for integration layers 
     * @param queues
     * @param topics
     * @param cfs
     * @return
     */
    protected JMSConfiguration newConfig(final ArrayList<JMSQueueConfiguration> queues,
                                         final ArrayList<TopicConfiguration> topics,
                                         final ArrayList<ConnectionFactoryConfiguration> cfs)
    {
       JMSConfiguration value = new JMSConfigurationImpl(cfs, queues, topics);
       return value;
    }
 
    // Package protected ---------------------------------------------
 
    // Protected -----------------------------------------------------
 
    // Private -------------------------------------------------------
 
    // Inner classes -------------------------------------------------
 
 }
