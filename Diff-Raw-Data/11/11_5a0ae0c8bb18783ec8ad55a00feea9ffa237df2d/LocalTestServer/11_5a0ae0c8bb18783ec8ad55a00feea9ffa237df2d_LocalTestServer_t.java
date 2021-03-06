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
 package org.jboss.test.messaging.tools.container;
 
 import java.io.File;
 import java.sql.Connection;
 import java.sql.PreparedStatement;
 import java.sql.SQLException;
 import java.util.ArrayList;
 import java.util.Collection;
 import java.util.HashMap;
 import java.util.HashSet;
 import java.util.List;
 import java.util.Properties;
 
 import javax.jms.InvalidDestinationException;
 import javax.management.NotificationListener;
 import javax.management.ObjectName;
 import javax.naming.Context;
 import javax.naming.InitialContext;
 import javax.naming.NameNotFoundException;
 import javax.sql.DataSource;
 import javax.transaction.TransactionManager;
 import javax.transaction.UserTransaction;
 
 import org.jboss.jms.client.JBossConnectionFactory;
 import org.jboss.jms.client.api.ClientConnectionFactory;
 import org.jboss.jms.client.impl.ClientConnectionFactoryImpl;
 import org.jboss.jms.destination.JBossQueue;
 import org.jboss.jms.destination.JBossTopic;
 import org.jboss.jms.server.security.Role;
 import org.jboss.kernel.spi.deployment.KernelDeployment;
 import org.jboss.logging.Logger;
 import org.jboss.messaging.core.Binding;
 import org.jboss.messaging.core.Condition;
 import org.jboss.messaging.core.DestinationType;
 import org.jboss.messaging.core.MessagingServer;
 import org.jboss.messaging.core.MessagingServerManagement;
 import org.jboss.messaging.core.impl.ConditionImpl;
 import org.jboss.messaging.core.remoting.RemotingConfiguration;
 import org.jboss.messaging.microcontainer.JBMBootstrapServer;
 import org.jboss.messaging.util.JNDIUtil;
 import org.jboss.messaging.util.Version;
 import org.jboss.test.messaging.tools.ConfigurationHelper;
 import org.jboss.test.messaging.tools.ServerManagement;
 import org.jboss.test.messaging.tools.jboss.MBeanConfigurationElement;
 import org.jboss.tm.TransactionManagerLocator;
 
 /**
  * @author <a href="mailto:ovidiu@feodorov.com">Ovidiu Feodorov</a>
  * @author <a href="mailto:tim.fox@jboss.com">Tim Fox</a>
  * @version <tt>1.1</tt>
  *          <p/>
  *          LocalTestServer.java,v 1.1 2006/02/21 08:25:32 timfox Exp
  */
 public class LocalTestServer implements Server, Runnable
 {
    // Constants ------------------------------------------------------------------------------------
 
    private static final Logger log = Logger.getLogger(LocalTestServer.class);
    private boolean started = false;
    private HashMap<String, List<String>> allBindings = new HashMap<String, List<String>>();
    // Static ---------------------------------------------------------------------------------------
 
    public static void setEnvironmentServerIndex(int serverIndex)
    {
       System.setProperty(Constants.SERVER_INDEX_PROPERTY_NAME, Integer.toString(serverIndex));
    }
 
    public static void clearEnvironmentServerIndex()
    {
       System.getProperty(Constants.SERVER_INDEX_PROPERTY_NAME, null);
    }
 
    // Attributes -----------------------------------------------------------------------------------
 
    private ServiceContainer sc;
 
    // service dependencies
    private ObjectName persistenceManagerObjectName;
    private ObjectName postOfficeObjectName;
    private ObjectName jmsUserManagerObjectName;
 
    // the server MBean itself
    private ObjectName serverPeerObjectName;
 
    private int serverIndex;
 
    JBMBootstrapServer bootstrap;
 
    // Constructors ---------------------------------------------------------------------------------
 
    public LocalTestServer()
    {
       super();
    }
 
    public LocalTestServer(int serverIndex)
    {
       this();
 
       this.serverIndex = serverIndex;
    }
 
    // Server implementation ------------------------------------------------------------------------
 
    public int getServerID()
    {
       return serverIndex;
    }
 
 
    public synchronized void start(String[] containerConfig,
                                   HashMap<String, Object> configuration,
                                   boolean clearDatabase) throws Exception
    {
       if (isStarted())
       {
          return;
       }
 
       if (clearDatabase)
       {
          //Delete the BDB environment
 
          File dir = new File(System.getProperty("user.home") + "/bdbje/env");
 
          boolean deleted = deleteDirectory(dir);
 
          log.info("Deleted dir: " +dir.getAbsolutePath() + " deleted: " + deleted);
       }
 
       ConfigurationHelper.addServerConfig(getServerID(), configuration);
 
       JBMPropertyKernelConfig propertyKernelConfig = new JBMPropertyKernelConfig(System.getProperties());
       propertyKernelConfig.setServerID(getServerID());
       bootstrap = new JBMBootstrapServer(containerConfig, propertyKernelConfig);
       System.setProperty(Constants.SERVER_INDEX_PROPERTY_NAME, ""+getServerID());
       bootstrap.run();
       started = true;
 
    }
 
    private static boolean deleteDirectory(File directory)
    {
       if (directory.isDirectory())
       {
          String[] files = directory.list();
 
          for (int j = 0; j < files.length; j++)
          {
             if (!deleteDirectory(new File(directory, files[j])))
             {
                return false;
             }
          }
       }
 
       return directory.delete();
    }
 
 
    protected void deleteAllData() throws Exception
    {
       log.info("DELETING ALL DATA FROM DATABASE!");
 
       InitialContext ctx = getInitialContext();
 
       // We need to execute each drop in its own transaction otherwise postgresql will not execute
       // further commands after one fails
 
       TransactionManager mgr = TransactionManagerLocator.locateTransactionManager();
       DataSource ds = (DataSource) ctx.lookup("java:/DefaultDS");
 
       javax.transaction.Transaction txOld = mgr.suspend();
 
       executeStatement(mgr, ds, "DELETE FROM JBM_POSTOFFICE");
 
       executeStatement(mgr, ds, "DELETE FROM JBM_MSG_REF");
 
       executeStatement(mgr, ds, "DELETE FROM JBM_MSG");
 
       executeStatement(mgr, ds, "DELETE FROM JBM_TX");
 
       executeStatement(mgr, ds, "DELETE FROM JBM_COUNTER");
 
       executeStatement(mgr, ds, "DELETE FROM JBM_USER");
 
       executeStatement(mgr, ds, "DELETE FROM JBM_ROLE");
 
       if (txOld != null)
       {
          mgr.resume(txOld);
       }
 
       log.debug("done with the deleting data");
    }
 
    private void executeStatement(TransactionManager mgr, DataSource ds, String statement) throws Exception
    {
       Connection conn = null;
       boolean exception = false;
 
       try
       {
          try
          {
             mgr.begin();
 
             conn = ds.getConnection();
 
             log.debug("executing " + statement);
 
             PreparedStatement ps = conn.prepareStatement(statement);
 
             ps.executeUpdate();
 
             log.debug(statement + " executed");
 
             ps.close();
          }
          catch (SQLException e)
          {
             // Ignore
             log.debug("Failed to execute statement", e);
             exception = true;
          }
       }
       finally
       {
          if (conn != null)
          {
             conn.close();
          }
 
          if (exception)
          {
             mgr.rollback();
          }
          else
          {
             mgr.commit();
          }
       }
 
 
    }
 
    public synchronized boolean stop() throws Exception
    {
       bootstrap.shutDown();
       started=false;
       unbindAll();
       return true;
    }
 
 
    public void ping() throws Exception
    {
       if (!isStarted())
       {
          throw new RuntimeException("ok");
       }
    }
 
    public synchronized void kill() throws Exception
    {
       stop();
    }
 
    public KernelDeployment deploy(String resourceName) throws Exception
    {
       try
       {
          return bootstrap.deploy(resourceName);
       }
       catch (Throwable e)
       {
          // RMI can only throw Exception or its subclasses... This is case we ever implement Server as remote again
          if (e instanceof Exception)
          {
             throw (Exception)e;
          }
          else
          {
             throw new Exception (e.toString(), e);
          }
       }
    }
    
    public KernelDeployment deployXML(String name, String xml) throws Exception
    {
       try
       {
          return bootstrap.deploy(name, xml);
       }
       catch (Throwable e)
       {
          // RMI can only throw Exception or its subclasses... This is case we ever implement Server as remote again
          if (e instanceof Exception)
          {
             throw (Exception)e;
          }
          else
          {
             throw new Exception (e.toString(), e);
          }
       }
    }
 
    public void undeploy(KernelDeployment deployment) throws Exception
    {
       try
       {
          bootstrap.undeploy(deployment);
       }
       catch (Throwable e)
       {
          // RMI can only throw Exception or its subclasses... This is case we ever implement Server as remote again
          if (e instanceof Exception)
          {
             throw (Exception)e;
          }
          else
          {
             throw new Exception (e.toString(), e);
          }
       }
    }
 
    public Object getAttribute(ObjectName on, String attribute) throws Exception
    {
       return null;// sc.getAttribute(on, attribute);
    }
 
    public void setAttribute(ObjectName on, String name, String valueAsString) throws Exception
    {
       //sc.setAttribute(on, name, valueAsString);
    }
 
    public Object invoke(ObjectName on, String operationName, Object[] params, String[] signature)
            throws Exception
    {
       return null;//sc.invoke(on, operationName, params, signature);
    }
 
    public void addNotificationListener(ObjectName on, NotificationListener listener)
            throws Exception
    {
       // sc.addNotificationListener(on, listener);
    }
 
    public void removeNotificationListener(ObjectName on, NotificationListener listener)
            throws Exception
    {
       //sc.removeNotificationListener(on, listener);
    }
 
 
    public void log(int level, String text)
    {
       if (ServerManagement.FATAL == level)
       {
          log.fatal(text);
       }
       else if (ServerManagement.ERROR == level)
       {
          log.error(text);
       }
       else if (ServerManagement.WARN == level)
       {
          log.warn(text);
       }
       else if (ServerManagement.INFO == level)
       {
          log.info(text);
       }
       else if (ServerManagement.DEBUG == level)
       {
          log.debug(text);
       }
       else if (ServerManagement.TRACE == level)
       {
          log.trace(text);
       }
       else
       {
          // log everything else as INFO
          log.info(text);
       }
    }
 
    public synchronized boolean isStarted() throws Exception
    {
       return started;
    }
 
    public synchronized void startServerPeer(int serverPeerID,
                                String defaultQueueJNDIContext,
                                String defaultTopicJNDIContext,
                                ServiceAttributeOverrides attrOverrides,
                                boolean clustered) throws Exception
    {
       System.setProperty(Constants.SERVER_INDEX_PROPERTY_NAME, "" + getServerID());
       getMessagingServer().start();
    }
 
    public synchronized void stopServerPeer() throws Exception
    {
       System.setProperty(Constants.SERVER_INDEX_PROPERTY_NAME, "" + getServerID());
       getMessagingServer().stop();
       //also unbind everything
       unbindAll();
    }
 
    private void unbindAll()
            throws Exception
    {
       Collection<List<String>> bindings = allBindings.values();
       for (List<String> binding : bindings)
       {
          for (String s : binding)
          {
             getInitialContext().unbind(s);
          }
       }
    }
 
 
    public boolean isServerPeerStarted() throws Exception
    {
       return this.getMessagingServerManagement().isStarted();
    }
 
    public ObjectName getServerPeerObjectName()
    {
       return serverPeerObjectName;
    }
 
 //   /**
 //    * Only for in-VM use!
 //    */
 //   public MessageStore getMessageStore() throws Exception
 //   {
 //      return getServerPeer().getMessageStore();
 //   }
 
 //   public DestinationManager getDestinationManager() throws Exception
 //   {
 //      return getServerPeer().getDestinationManager();
 //   }
 //
 //   public PersistenceManager getPersistenceManager()
 //   {
 //      return getServerPeer().getPersistenceManagerInstance();
 //   }
 
    /**
     * Only for in-VM use!
     */
    public MessagingServer getServerPeer()
    {
       return getMessagingServer();
    }
 
 //   public void deployTopic(String name, String jndiName, boolean clustered) throws Exception
 //   {
 //      deployDestination(false, name, jndiName, clustered);
 //   }
 //
 //   public void deployTopic(String name, String jndiName, int fullSize, int pageSize,
 //                           int downCacheSize, boolean clustered) throws Exception
 //   {
 //      deployDestination(false, name, jndiName, fullSize, pageSize, downCacheSize, clustered);
 //   }
 //
 //   public void deployTopicProgrammatically(String name, String jndiName) throws Exception
 //   {
 //      deployTopic(name, jndiName, false);
 //   }
 //
 //   public void deployQueue(String name, String jndiName, boolean clustered) throws Exception
 //   {
 //      deployDestination(true, name, jndiName, clustered);
 //   }
 //
 //   public void deployQueue(String name, String jndiName, int fullSize, int pageSize,
 //                           int downCacheSize, boolean clustered) throws Exception
 //   {
 //      deployDestination(true, name, jndiName, fullSize, pageSize, downCacheSize, clustered);
 //   }
 //
 //   public void deployQueueProgrammatically(String name, String jndiName) throws Exception
 //   {
 //      deployQueue(name, jndiName, false);
 //   }
 //
 //   public void deployDestination(boolean isQueue, String name, String jndiName, boolean clustered) throws Exception
 //   {
 //      if (isQueue)
 //         getMessagingServer().deployQueue(name, jndiName);
 //      else
 //         getMessagingServer().deployTopic(name, jndiName);
 //   }
 //
 //   public void deployDestination(boolean isQueue,
 //                                 String name,
 //                                 String jndiName,
 //                                 int fullSize,
 //                                 int pageSize,
 //                                 int downCacheSize,
 //                                 boolean clustered) throws Exception
 //   {
 //      if (isQueue)
 //         getMessagingServer().deployQueue(name, jndiName, fullSize, pageSize, downCacheSize);
 //      else
 //         getMessagingServer().deployTopic(name, jndiName, fullSize, pageSize, downCacheSize);
 //   }
 //
 //   public void undeployDestination(boolean isQueue, String name) throws Exception
 //   {
 //      if (isQueue)
 //         getMessagingServer().undeployQueue(name);
 //      else
 //         getMessagingServer().undeployTopic(name);
 //   }
 //
 //   public boolean undeployDestinationProgrammatically(boolean isQueue, String name) throws Exception
 //   {
 //      if (isQueue)
 //         return getMessagingServer().undeployQueue(name);
 //      else
 //         return getMessagingServer().undeployTopic(name);
 //   }
 
 
    public void destroyQueue(String name, String jndiName) throws Exception
    {
       this.getMessagingServerManagement().destroyQueue(name);
      if (jndiName != null)
      {
         getInitialContext().unbind(jndiName);
      }
    }
 
    public void destroyTopic(String name, String jndiName) throws Exception
    {
       this.getMessagingServerManagement().destroyTopic(name);
      if (jndiName != null)
      {
         getInitialContext().unbind(jndiName);
      }
    }
 
    public void createQueue(String name, String jndiName) throws Exception
    {
       this.getMessagingServerManagement().createQueue(name);
       JBossQueue jBossQueue = new JBossQueue(name);
       bindObject("/queue/" + (jndiName != null ? jndiName : name), jBossQueue);
       List<String> bindings = new ArrayList<String>();
       bindings.add("/queue/" + (jndiName != null ? jndiName : name));
       allBindings.put(name, bindings);
    }
 
    public void createTopic(String name, String jndiName) throws Exception
    {
       this.getMessagingServerManagement().createTopic(name);
       JBossTopic jBossTopic = new JBossTopic(name);
       bindObject("/topic/" + (jndiName != null ? jndiName : name), jBossTopic);
       List<String> bindings = new ArrayList<String>();
       bindings.add("/topic/" + (jndiName != null ? jndiName : name));
       allBindings.put(name, bindings);
    }
 
    private void bindObject(String jndiName, Object object)
            throws Exception
    {
       String parentContext;
       String jndiNameInContext;
       int sepIndex = jndiName.lastIndexOf('/');
       if (sepIndex == -1)
       {
          parentContext = "";
       }
       else
       {
          parentContext = jndiName.substring(0, sepIndex);
       }
       jndiNameInContext = jndiName.substring(sepIndex + 1);
       try
       {
          getInitialContext().lookup(jndiName);
          throw new InvalidDestinationException("Destination " + jndiName + " already exists");
       }
       catch (NameNotFoundException e)
       {
          // OK
       }
 
       Context c = JNDIUtil.createContext(getInitialContext(), parentContext);
 
       c.rebind(jndiNameInContext, object);
    }
 
    public void deployConnectionFactory(String clientId, String objectName,
                                        String[] jndiBindings) throws Exception
    {
       deployConnectionFactory(clientId, objectName, jndiBindings, -1, -1, -1, -1, false, false, false, -1);
    }
 
    public void deployConnectionFactory(String objectName,
                                        String[] jndiBindings,
                                        int prefetchSize) throws Exception
    {
       deployConnectionFactory(null, objectName, jndiBindings, prefetchSize, -1, -1, -1, false, false, false, -1);
    }
 
 
    public void deployConnectionFactory(String objectName,
                                        String[] jndiBindings) throws Exception
    {
       deployConnectionFactory(null, objectName, jndiBindings, -1, -1, -1, -1, false, false, false, -1);
    }
 
 
    public void deployConnectionFactory(String objectName, String[] jndiBindings, boolean strictTck) throws Exception
    {
       deployConnectionFactory(null, objectName, jndiBindings, -1, -1, -1, -1, false, false, strictTck, -1);
    }
 
    public void deployConnectionFactory(String objectName,
                                        String[] jndiBindings,
                                        int prefetchSize,
                                        int defaultTempQueueFullSize,
                                        int defaultTempQueuePageSize,
                                        int defaultTempQueueDownCacheSize) throws Exception
    {
       this.deployConnectionFactory(null, objectName, jndiBindings, prefetchSize, defaultTempQueueFullSize,
               defaultTempQueuePageSize, defaultTempQueueDownCacheSize, false, false, false, -1);
    }
 
    public void deployConnectionFactory(String objectName,
                                        String[] jndiBindings,
                                        boolean supportsFailover, boolean supportsLoadBalancing) throws Exception
    {
       this.deployConnectionFactory(null, objectName, jndiBindings, -1, -1,
               -1, -1, supportsFailover, supportsLoadBalancing, false, -1);
    }
 
    public void deployConnectionFactory(String clientId,
                                        String objectName,
                                        String[] jndiBindings,
                                        int prefetchSize,
                                        int defaultTempQueueFullSize,
                                        int defaultTempQueuePageSize,
                                        int defaultTempQueueDownCacheSize,
                                        boolean supportsFailover,
                                        boolean supportsLoadBalancing,
                                        boolean strictTck,
                                        int dupsOkBatchSize) throws Exception
    {
       log.trace("deploying connection factory with name: " + objectName);
       //ConnectionFactory connectionFactory = new ConnectionFactory(clientId);
       List<String> bindings = new ArrayList<String>();
       if (jndiBindings != null)
       {
          for (String jndiBinding : jndiBindings)
          {
             bindings.add(jndiBinding);
          }
       }
       //connectionFactory.setJNDIBindings(bindings);
       //if (prefetchSize > 0)
       //   connectionFactory.setPrefetchSize(prefetchSize);
       //if (defaultTempQueueFullSize > 0) if (dupsOkBatchSize > 0)
        //  connectionFactory.setDupsOKBatchSize(dupsOkBatchSize);
      // connectionFactory.setSupportsFailover(supportsFailover);
       //connectionFactory.setSupportsLoadBalancing(supportsLoadBalancing);
       //connectionFactory.setStrictTck(strictTck);
       RemotingConfiguration remotingConfiguration = getMessagingServer().getRemotingService().getRemotingConfiguration();
 
       log.info("Remoting configuration is " + remotingConfiguration);
       log.info(this + " started");
       // See http://www.jboss.com/index.html?module=bb&op=viewtopic&p=4076040#4076040
       final String id = objectName;
 
       Version version = getMessagingServer().getVersion();
 
       /*ServerConnectionFactoryEndpoint endpoint =
               new ServerConnectionFactoryEndpoint(connectionFactory.getName(), id, getMessagingServer(), connectionFactory.getClientID(),
                       connectionFactory.getPrefetchSize(),
                       connectionFactory.getDefaultTempQueueFullSize(),
                       connectionFactory.getDefaultTempQueuePageSize(),
                       connectionFactory.getDefaultTempQueueDownCacheSize(),
                       connectionFactory.getDupsOKBatchSize());*/
 
       //The server peer strict setting overrides the connection factory
       boolean useStrict = getMessagingServer().getConfiguration().isStrictTck() || strictTck;
 
       ClientConnectionFactory delegate =
               new ClientConnectionFactoryImpl( getMessagingServer().getConfiguration().getMessagingServerID(),
                       remotingConfiguration, version, useStrict, prefetchSize, dupsOkBatchSize, clientId);
 
       log.debug(this + " created local delegate " + delegate);
 
       // Registering with the dispatcher should always be the last thing otherwise a client could
       // use a partially initialised object
 
       //getMessagingServer().getMinaService().getDispatcher().register(endpoint.newHandler());
       JBossConnectionFactory jBossConnectionFactory = new JBossConnectionFactory(delegate);
       for (String binding : bindings)
       {
          bindObject(binding, jBossConnectionFactory);
       }
       allBindings.put(objectName, bindings);
    }
 
    public void undeployConnectionFactory(String objectName) throws Exception
    {
       getMessagingServer().getRemotingService().getDispatcher().unregister(objectName);
       List<String> bindings = allBindings.get(objectName);
       for (String binding : bindings)
       {
          getInitialContext().unbind(binding);
       }
       allBindings.remove(objectName);
    }
 
    public void configureSecurityForDestination(String destName, boolean isQueue, HashSet<Role> roles) throws Exception
    {
       String prefix = isQueue ? "queues." : "topics";
       getMessagingServer().getSecurityRepository().addMatch(prefix + destName, roles);
    }
 
    public void setDefaultSecurityConfig(String config) throws Exception
    {
       //todo
    }
 
    public String getDefaultSecurityConfig() throws Exception
    {
       //todo
       return "";
    }
 
    public Object executeCommand(Command command) throws Exception
    {
       return command.execute(this);
    }
 
    public UserTransaction getUserTransaction() throws Exception
    {
       //return sc.getUserTransaction();
       return null;
    }
 
 //   public Set getNodeIDView() throws Exception
 //   {
 //      return getServerPeer().getPostOffice().nodeIDView();
 //   }
 //
 //   public Map getFailoverMap() throws Exception
 //   {
 //      return getServerPeer().getPostOffice().getFailoverMap();
 //   }
 //
 //   public Map getRecoveryArea(String queueName) throws Exception
 //   {
 //      return getServerPeer().getPostOffice().getRecoveryArea(queueName);
 //   }
 //
 //   public int getRecoveryMapSize(String queueName) throws Exception
 //   {
 //      return getServerPeer().getPostOffice().getRecoveryMapSize(queueName);
 //   }
 
    public List pollNotificationListener(long listenerID) throws Exception
    {
       throw new IllegalStateException("Poll doesn't make sense on a local server. " +
               "Register listeners directly instead.");
    }
 
    public void flushManagedConnectionPool()
    {
       //sc.flushManagedConnectionPool();
    }
 
    // Public ---------------------------------------------------------------------------------------
 
    // Package protected ----------------------------------------------------------------------------
 
    // Protected ------------------------------------------------------------------------------------
 
    protected ServiceContainer getServiceContainer()
    {
       return sc;
    }
 
    protected void overrideServerPeerConfiguration(MBeanConfigurationElement config,
                                                   int serverPeerID, String defaultQueueJNDIContext, String defaultTopicJNDIContext)
            throws Exception
    {
       config.setAttribute("ServerPeerID", Integer.toString(serverPeerID));
       config.setAttribute("DefaultQueueJNDIContext",
               defaultQueueJNDIContext == null ? "/queue" : defaultQueueJNDIContext);
       config.setAttribute("DefaultTopicJNDIContext",
               defaultTopicJNDIContext == null ? "/topic" : defaultTopicJNDIContext);
    }
 
    // Private --------------------------------------------------------------------------------------
 
 
    public MessagingServer getMessagingServer()
    {
       return (MessagingServer) bootstrap.getKernel().getRegistry().getEntry("MessagingServer").getTarget();
    }
 
    public MessagingServerManagement getMessagingServerManagement()
    {
       return (MessagingServerManagement) bootstrap.getKernel().getRegistry().getEntry("MessagingServerManagement").getTarget();
    }
 
    public InitialContext getInitialContext() throws Exception
    {
       Properties props = new Properties();
       props.setProperty("java.naming.factory.initial", "org.jboss.test.messaging.tools.container.InVMInitialContextFactory");
       props.setProperty(Constants.SERVER_INDEX_PROPERTY_NAME, "" + getServerID());
       //props.setProperty("java.naming.factory.url.pkgs", "org.jboss.naming:org.jnp.interfaces");
       return new InitialContext(props);
    }
 
    public void run()
    {
       bootstrap.run();
 
       started = true;
 
       synchronized (this)
       {
          notify();
          try
          {
             wait();
          }
          catch (InterruptedException e)
          {
             //e.printStackTrace();
          }
       }
 
    }
 
 
    public Integer getMessageCountForQueue(String queueName) throws Exception
    {
       return getMessagingServerManagement().getMessageCountForQueue(queueName);
    }
 
    public void removeAllMessagesForQueue(String destName) throws Exception
    {
       getMessagingServerManagement().removeAllMessagesForQueue(destName);
    }
 
    public void removeAllMessagesForTopic(String destName) throws Exception
    {
       getMessagingServerManagement().removeAllMessagesForTopic(destName);
    }
 
 
    public List listAllSubscriptionsForTopic(String s) throws Exception
    {
       return getMessagingServerManagement().listAllSubscriptionsForTopic(s);
    }
 
 
    public HashSet<Role> getSecurityConfig() throws Exception
    {
       return getMessagingServer().getConfiguration().getSecurityConfig();
    }
 
    public void setSecurityConfig(HashSet<Role> defConfig) throws Exception
    {
       getMessagingServer().getConfiguration().setSecurityConfig(defConfig);
    }
 
 
    public void setSecurityConfigOnManager(boolean b, String s, HashSet<Role> conf) throws Exception
    {
       String prefix = b ? "queues." : "topics";
       getMessagingServer().getSecurityRepository().addMatch(prefix + s, conf);
    }
 
 
    public void setRedeliveryDelayOnDestination(String dest, boolean queue, long delay) throws Exception
    {
       //getMessagingServer().getDestinationManager().getDestination(dest, queue).setRedeliveryDelay(delay);
 
       Condition condition = new ConditionImpl(queue ? DestinationType.QUEUE : DestinationType.TOPIC, dest);
 
       List<Binding> bindings = this.getMessagingServer().getPostOffice().getBindingsForCondition(condition);
 
       bindings.get(0).getQueue().setRedeliveryDelay(delay);
    }
 
 
    public void setDefaultRedeliveryDelay(long delay) throws Exception
    {
       getMessagingServer().getConfiguration().setDefaultRedeliveryDelay(delay);
    }
 
 
    // Inner classes --------------------------------------------------------------------------------
 
 }
