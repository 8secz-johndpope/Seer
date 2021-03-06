 /**
  * Copyright (C) FuseSource, Inc.
  * http://fusesource.com
  *
  * Licensed under the Apache License, Version 2.0 (the "License");
  * you may not use this file except in compliance with the License.
  * You may obtain a copy of the License at
  *
  *    http://www.apache.org/licenses/LICENSE-2.0
  *
  * Unless required by applicable law or agreed to in writing, software
  * distributed under the License is distributed on an "AS IS" BASIS,
  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  * See the License for the specific language governing permissions and
  * limitations under the License.
  */
 package org.fusesource.fabric.zookeeper.internal;
 
 import java.io.IOException;
 import java.net.InetAddress;
 import java.net.SocketException;
 import java.net.UnknownHostException;
 import java.util.Arrays;
 import java.util.List;
 import java.util.Set;
 import java.util.concurrent.CopyOnWriteArraySet;
 import java.util.concurrent.TimeUnit;
 import java.util.concurrent.locks.ReentrantLock;
 import javax.management.MBeanServer;
 import javax.management.MBeanServerNotification;
 import javax.management.Notification;
 import javax.management.NotificationListener;
 import javax.management.ObjectName;
 import org.apache.zookeeper.CreateMode;
 import org.apache.zookeeper.KeeperException;
 import org.apache.zookeeper.ZooDefs;
 import org.apache.zookeeper.data.Stat;
 import org.fusesource.fabric.utils.HostUtils;
 import org.fusesource.fabric.zookeeper.ZkDefs;
 import org.fusesource.fabric.zookeeper.ZkPath;
 import org.linkedin.zookeeper.client.IZKClient;
 import org.linkedin.zookeeper.client.LifecycleListener;
 import org.osgi.framework.BundleContext;
 import org.osgi.framework.ServiceException;
 import org.osgi.framework.ServiceReference;
 import org.osgi.service.cm.Configuration;
 import org.osgi.service.cm.ConfigurationAdmin;
 import org.slf4j.Logger;
 import org.slf4j.LoggerFactory;
 
 
 import static org.fusesource.fabric.zookeeper.ZkPath.CONFIG_CONTAINER;
 import static org.fusesource.fabric.zookeeper.ZkPath.CONFIG_VERSIONS_CONTAINER;
 import static org.fusesource.fabric.zookeeper.ZkPath.CONTAINER_ADDRESS;
 import static org.fusesource.fabric.zookeeper.ZkPath.CONTAINER_ALIVE;
 import static org.fusesource.fabric.zookeeper.ZkPath.CONTAINER_DOMAIN;
 import static org.fusesource.fabric.zookeeper.ZkPath.CONTAINER_DOMAINS;
 import static org.fusesource.fabric.zookeeper.ZkPath.CONTAINER_IP;
 import static org.fusesource.fabric.zookeeper.ZkPath.CONTAINER_JMX;
 import static org.fusesource.fabric.zookeeper.ZkPath.CONTAINER_LOCAL_HOSTNAME;
 import static org.fusesource.fabric.zookeeper.ZkPath.CONTAINER_LOCAL_IP;
 import static org.fusesource.fabric.zookeeper.ZkPath.CONTAINER_PUBLIC_IP;
 import static org.fusesource.fabric.zookeeper.ZkPath.CONTAINER_RESOLVER;
 import static org.fusesource.fabric.zookeeper.ZkPath.CONTAINER_SSH;
 
 public class KarafContainerRegistration implements LifecycleListener, NotificationListener {
 
     private transient Logger logger = LoggerFactory.getLogger(KarafContainerRegistration.class);
 
     private ConfigurationAdmin configurationAdmin;
     private IZKClient zooKeeper;
     private BundleContext bundleContext;
     private final Set<String> domains = new CopyOnWriteArraySet<String>();
     private String name = System.getProperty("karaf.name");
     private volatile MBeanServer mbeanServer;
 
 
     private ReentrantLock lock = new ReentrantLock();
 
     public IZKClient getZooKeeper() {
         return zooKeeper;
     }
 
     public void setZooKeeper(IZKClient zooKeeper) {
         this.zooKeeper = zooKeeper;
     }
 
     public void setConfigurationAdmin(ConfigurationAdmin configurationAdmin) {
         this.configurationAdmin = configurationAdmin;
     }
 
     public void setBundleContext(BundleContext bundleContext) {
         this.bundleContext = bundleContext;
     }
 
     public void onConnected() {
         logger.trace("onConnected");
         try {
             lock.tryLock(10,TimeUnit.SECONDS);
             String nodeAlive = CONTAINER_ALIVE.getPath(name);
             Stat stat = zooKeeper.exists(nodeAlive);
             if (stat != null) {
                 if (stat.getEphemeralOwner() != zooKeeper.getSessionId()) {
                     zooKeeper.delete(nodeAlive);
                     zooKeeper.createWithParents(nodeAlive, null, ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL);
                 }
             } else {
                 zooKeeper.createWithParents(nodeAlive, null, ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL);
             }
 
             String domainsNode = CONTAINER_DOMAINS.getPath(name);
             stat = zooKeeper.exists(domainsNode);
             if (stat != null) {
                 zooKeeper.deleteWithChildren(domainsNode);
             }
 
             String jmxUrl = getJmxUrl();
             if (jmxUrl != null) {
                 zooKeeper.createOrSetWithParents(CONTAINER_JMX.getPath(name), getJmxUrl(), ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
             }
             String sshUrl = getSshUrl();
             if (sshUrl != null) {
                 zooKeeper.createOrSetWithParents(CONTAINER_SSH.getPath(name), getSshUrl(), ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
             }
 
             if (zooKeeper.exists(CONTAINER_RESOLVER.getPath(name)) == null) {
                 zooKeeper.createOrSetWithParents(CONTAINER_RESOLVER.getPath(name), getGlobalResolutionPolicy(zooKeeper), ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
             }
             zooKeeper.createOrSetWithParents(CONTAINER_LOCAL_HOSTNAME.getPath(name), HostUtils.getLocalHostName(), ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
             zooKeeper.createOrSetWithParents(CONTAINER_LOCAL_IP.getPath(name), HostUtils.getLocalIp(), ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
             zooKeeper.createOrSetWithParents(CONTAINER_IP.getPath(name), getContainerPointer(zooKeeper,name), ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
 
             //Check if there are addresses specified as system properties and use them if there is not an existing value in the registry.
             //Mostly usable for adding values when creating containers without an existing ensemble.
             for (String resolver : ZkDefs.VALID_RESOLVERS) {
                 String address = System.getProperty(resolver);
                 if (address != null && !address.isEmpty()) {
                     if (zooKeeper.exists(CONTAINER_ADDRESS.getPath(name, resolver)) == null) {
                        zooKeeper.createOrSetWithParents(CONTAINER_ADDRESS.getPath(name, resolver), getContainerPointer(zooKeeper, name), ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
                     }
                 }
             }
 
             String version = System.getProperty("fabric.version", ZkDefs.DEFAULT_VERSION);
             String profiles = System.getProperty("fabric.profiles");
 
             if (profiles != null) {
                 String versionNode = CONFIG_CONTAINER.getPath(name);
                 String profileNode = CONFIG_VERSIONS_CONTAINER.getPath(version, name);
 
                 if (zooKeeper.exists(versionNode) == null) {
                     zooKeeper.createOrSetWithParents(versionNode, version, ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
                 }
                 if (zooKeeper.exists(profileNode) == null) {
                     zooKeeper.createOrSetWithParents(profileNode, profiles, ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
                 }
             }
             registerDomains();
         } catch (Exception e) {
             logger.warn("Error updating Fabric Container information. This exception will be ignored.", e);
         } finally {
             lock.unlock();
         }
     }
 
     private String getJmxUrl() throws IOException {
         Configuration config = configurationAdmin.getConfiguration("org.apache.karaf.management");
         if (config.getProperties() != null) {
             String jmx = (String) config.getProperties().get("serviceUrl");
             jmx = jmx.replace("service:jmx:rmi://localhost:", "service:jmx:rmi://${zk:" + name  + "/ip}:");
             jmx = jmx.replace("jndi/rmi://localhost","jndi/rmi://${zk:" + name  + "/ip}");
             return jmx;
         } else {
             return null;
         }
     }
 
     private String getSshUrl() throws IOException {
         Configuration config = configurationAdmin.getConfiguration("org.apache.karaf.shell");
         if (config != null && config.getProperties() != null) {
             String port = (String) config.getProperties().get("sshPort");
             return "${zk:" + name  + "/ip}:" + port;
         } else {
             return null;
         }
     }
 
 
     /**
      * Returns the global resolution policy.
      * @param zookeeper
      * @return
      * @throws InterruptedException
      * @throws KeeperException
      */
     private static String getGlobalResolutionPolicy(IZKClient zookeeper) throws InterruptedException, KeeperException {
         String policy = ZkDefs.LOCAL_HOSTNAME;
         List<String> validResoverList = Arrays.asList(ZkDefs.VALID_RESOLVERS);
         if (zookeeper.exists(ZkPath.POLICIES.getPath(ZkDefs.RESOLVER)) != null) {
             policy = zookeeper.getStringData(ZkPath.POLICIES.getPath(ZkDefs.RESOLVER));
         } else if (System.getProperty(ZkDefs.GLOBAL_RESOLVER_PROPERTY) != null && validResoverList.contains(System.getProperty(ZkDefs.GLOBAL_RESOLVER_PROPERTY))){
             policy =  System.getProperty(ZkDefs.GLOBAL_RESOLVER_PROPERTY);
             zookeeper.createOrSetWithParents(ZkPath.POLICIES.getPath("resolver"),policy,  ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
         }
         return policy;
     }
 
      /**
      * Returns the container specific resolution policy.
      * @param zookeeper
      * @return
      * @throws InterruptedException
      * @throws KeeperException
      */
     private static String getContainerResolutionPolicy(IZKClient zookeeper, String container) throws InterruptedException, KeeperException {
         String policy = ZkDefs.LOCAL_HOSTNAME;
         if (zookeeper.exists(ZkPath.POLICIES.getPath(ZkDefs.RESOLVER)) != null) {
             policy = zookeeper.getStringData(ZkPath.CONTAINER_RESOLVER.getPath(container));
         }
         return policy;
     }
 
     /**
      * Returns a pointer to the container IP based on the global IP policy.
      * @param zookeeper The zookeeper client to use to read global policy.
      * @param container The name of the container.
      * @return
      * @throws InterruptedException
      * @throws KeeperException
      */
     private static String getContainerPointer(IZKClient zookeeper, String container) throws InterruptedException, KeeperException {
         String pointer = "${zk:%s/%s}";
         String policy = getContainerResolutionPolicy(zookeeper, container);
         return String.format(pointer,container,policy);
     }
 
 
     private static String getExternalAddresses(String host, String port) throws UnknownHostException, SocketException {
         InetAddress ip = InetAddress.getByName(host);
         if (ip.isAnyLocalAddress()) {
             return HostUtils.getLocalHostName() + ":" + port;
         } else if (!ip.isLoopbackAddress()) {
             return ip.getHostName() + ":" + port;
         }
         return null;
     }
 
     public void destroy() {
         logger.trace("destroy");
         try {
             unregisterDomains();
         } catch (ServiceException e) {
             logger.trace("ZooKeeper is no longer available", e);
         } catch (Exception e) {
             logger.warn("An error occurred during disconnecting to zookeeper. This exception will be ignored.", e);
         }
     }
 
     public void onDisconnected() {
         logger.trace("onDisconnected");
         // noop
     }
 
     public void registerMBeanServer(ServiceReference ref) {
         try {
             lock.lock();
             String name = System.getProperty("karaf.name");
             mbeanServer = (MBeanServer) bundleContext.getService(ref);
             if (mbeanServer != null) {
                 mbeanServer.addNotificationListener(new ObjectName("JMImplementation:type=MBeanServerDelegate"), this, null, name);
                 registerDomains();
             }
         } catch (Exception e) {
             logger.warn("An error occurred during mbean server registration. This exception will be ignored.", e);
         }  finally {
             lock.unlock();
         }
     }
 
     public void unregisterMBeanServer(ServiceReference ref) {
         if (mbeanServer != null) {
             try {
                 lock.lock();
                 mbeanServer.removeNotificationListener(new ObjectName("JMImplementation:type=MBeanServerDelegate"), this);
                 unregisterDomains();
             } catch (Exception e) {
                 logger.warn("An error occurred during mbean server unregistration. This exception will be ignored.", e);
             } finally {
                 lock.unlock();
             }
         }
         mbeanServer = null;
         bundleContext.ungetService(ref);
     }
 
     protected void registerDomains() throws InterruptedException, KeeperException {
         if (isConnected() && mbeanServer != null) {
             String name = System.getProperty("karaf.name");
             domains.addAll(Arrays.asList(mbeanServer.getDomains()));
             for (String domain : mbeanServer.getDomains()) {
                 zooKeeper.createOrSetWithParents(CONTAINER_DOMAIN.getPath(name, domain), null, ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
             }
         }
     }
 
     protected void unregisterDomains() throws InterruptedException, KeeperException {
         if (isConnected()) {
             String name = System.getProperty("karaf.name");
             String domainsPath = CONTAINER_DOMAINS.getPath(name);
             if (zooKeeper.exists(domainsPath) != null) {
                 for (String child : zooKeeper.getChildren(domainsPath)) {
                     zooKeeper.delete(domainsPath + "/" + child);
                 }
             }
         }
     }
 
     @Override
     public void handleNotification(Notification notif, Object o) {
         logger.trace("handleNotification[{}]", notif);
 
         // we may get notifications when zookeeper client is not really connected
         // handle mbeans registration and de-registration events
         if (isConnected() && mbeanServer != null && notif instanceof MBeanServerNotification) {
             MBeanServerNotification notification = (MBeanServerNotification) notif;
             String domain = notification.getMBeanName().getDomain();
             String path = CONTAINER_DOMAIN.getPath((String) o, domain);
             try {
                 lock.lock();
                 if (MBeanServerNotification.REGISTRATION_NOTIFICATION.equals(notification.getType())) {
                     if (domains.add(domain) && zooKeeper.exists(path) == null) {
                         zooKeeper.createOrSetWithParents(path, "", ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
                     }
                 } else if (MBeanServerNotification.UNREGISTRATION_NOTIFICATION.equals(notification.getType())) {
                     domains.clear();
                     domains.addAll(Arrays.asList(mbeanServer.getDomains()));
                     if (!domains.contains(domain)) {
                         // domain is no present any more
                         zooKeeper.delete(path);
                     }
                 }
 //            } catch (KeeperException.SessionExpiredException e) {
 //                logger.debug("Session expiry detected. Handling notification once again", e);
 //                handleNotification(notif, o);
             } catch (Exception e) {
                 logger.warn("Exception while jmx domain synchronization from event: " + notif + ". This exception will be ignored.", e);
             } finally {
                 lock.unlock();
             }
         }
     }
 
     private boolean isConnected() {
         // we are only considered connected if we have a client and its connected
         return zooKeeper != null && zooKeeper.isConnected();
     }
 
 }
