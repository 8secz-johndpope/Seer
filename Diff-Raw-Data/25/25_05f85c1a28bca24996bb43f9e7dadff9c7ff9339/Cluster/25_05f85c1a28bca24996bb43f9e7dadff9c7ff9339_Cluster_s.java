 /*
  * Copyright (c) 1998-2004 Caucho Technology -- all rights reserved
  *
  * This file is part of Resin(R) Open Source
  *
  * Each copy or derived work must preserve the copyright notice and this
  * notice unmodified.
  *
  * Resin Open Source is free software; you can redistribute it and/or modify
  * it under the terms of the GNU General Public License as published by
  * the Free Software Foundation; either version 2 of the License, or
  * (at your option) any later version.
  *
  * Resin Open Source is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, or any warranty
  * of NON-INFRINGEMENT.  See the GNU General Public License for more
  * details.
  *
  * You should have received a copy of the GNU General Public License
  * along with Resin Open Source; if not, write to the
  *
  *   Free Software Foundation, Inc.
  *   59 Temple Place, Suite 330
  *   Boston, MA 02111-1307  USA
  *
  * @author Scott Ferguson
  */
 
 package com.caucho.server.cluster;
 
 import java.util.ArrayList;
 
 import java.util.logging.Logger;
 import java.util.logging.Level;
 
 import javax.management.ObjectName;
 
 import com.caucho.util.*;
 import com.caucho.vfs.*;
 
 import com.caucho.log.Log;
 
 import com.caucho.config.ConfigException;
 
 import com.caucho.config.types.Period;
 
 import com.caucho.loader.Environment;
 import com.caucho.loader.EnvironmentLocal;
 import com.caucho.loader.DynamicClassLoader;
 import com.caucho.loader.EnvironmentClassLoader;
 import com.caucho.loader.EnvironmentListener;
 
 import com.caucho.jmx.Jmx;
 import com.caucho.jmx.IntrospectionAttributeDescriptor;
 import com.caucho.jmx.AdminAttributeCategory;
 import com.caucho.jmx.IntrospectionMBeanDescriptor;
 
 import com.caucho.server.resin.SrunPort;
 
 import com.caucho.server.cluster.mbean.ClusterMBean;
 
 /**
  * Defines a set of clustered servers.
  */
 public class Cluster implements EnvironmentListener, ClusterMBean {
   static protected final L10N L = new L10N(ClusterGroup.class);
   static protected final Logger log = Log.open(Cluster.class);
 
   static protected final EnvironmentLocal<String> _serverIdLocal
     = new EnvironmentLocal<String>("caucho.server-id");
   
   static protected final EnvironmentLocal<Cluster> _clusterLocal
     = new EnvironmentLocal<Cluster>("caucho.cluster");
 
   private String _id = "";
   
   private String _serverId = "";
 
   private ObjectName _objectName;
   
   private ClusterServer []_serverList = new ClusterServer[0];
 
   private ClusterGroup _defaultGroup;
   private ArrayList<ClusterGroup> _groupList = new ArrayList<ClusterGroup>();
 
   private StoreManager _clusterStore;
 
   private long _clientLiveTime = 30000L;
   private long _clientDeadTime = 15000L;
   private long _clientReadTimeout = 60000L;
   private long _clientWriteTimeout = 60000L;
 
   private String _ref;
 
   private volatile boolean _isClosed;
   
   public Cluster()
   {
     Environment.addEnvironmentListener(this);
   }
   
   /**
    * Returns the currently active local cluster.
    */
   public static Cluster getLocal()
   {
     Cluster cluster = _clusterLocal.get();
     
     return cluster;
   }
   
   /**
    * Returns the currently active local cluster.
    */
   public static Cluster getCluster(ClassLoader loader)
   {
     Cluster cluster = _clusterLocal.get(loader);
     
     return cluster;
   }
 
   /**
    * Sets the cluster id.
    */
   public void setId(String id)
   {
     _id = id;
   }
 
   /**
    * Gets the cluster id.
    */
   public String getId()
   {
     return _id;
   }
 
   /**
    * Sets the cluster ref.
    */
   public void setClusterRef(String ref)
   {
     _ref = ref;
   }
 
   /**
    * Finds the first server with the given server-id.
    */
   public ClusterServer findServer(String id)
   {
     for (int i = 0; i < _serverList.length; i++) {
       ClusterServer server = _serverList[i];
 
       if (server != null && server.getId().equals(id))
 	return server;
     }
 
     return null;
   }
 
   /**
    * Adds a new server to the cluster.
    */
   void addServer(ClusterServer server)
     throws ConfigException
   {
     ClusterServer oldServer = findServer(server.getId());
 
     if (oldServer != null)
       log.warning(L.l("duplicate <srun> with server-id='{0}'",
 		      server.getId()));
 
     if (_serverList.length <= server.getIndex()) {
       int newLength = server.getIndex() + 1;
       ClusterServer []newList = new ClusterServer[newLength];
 
       System.arraycopy(_serverList, 0, newList, 0, _serverList.length);
 
       _serverList = newList;
     }
 
     if (_serverList[server.getIndex()] != null)
       throw new ConfigException(L.l("Cluster server `{0}' conflicts with a previous server.", server.getIndex()));
     
     _serverList[server.getIndex()] = server;
   }
 
   /**
    * Adds a new group to the cluster.
    */
   public ClusterGroup createGroup()
   {
     ClusterGroup group = new ClusterGroup();
     group.setCluster(this);
 
     _groupList.add(group);
 
     return group;
   }
 
   /**
    * Adds a srun server.
    */
   public void addPort(ClusterPort port)
     throws Exception
   {
     createDefaultGroup().addPort(port);
   }
 
   /**
    * Adds a srun server.
    */
   public void addSrun(ClusterPort port)
     throws Exception
   {
     addPort(port);
   }
 
   /**
    * Creates the default group.
    */
   ClusterGroup createDefaultGroup()
   {
     if (_defaultGroup == null)
       _defaultGroup = createGroup();
 
     return _defaultGroup;
   }
 
   /**
    * Returns the cluster store.
    */
   public StoreManager getStore()
   {
     return _clusterStore;
   }
 
   /**
    * Sets the cluster store.
    */
   void setStore(StoreManager store)
   {
     _clusterStore = store;
   }
 
   /**
    * Sets the live time.
    */
   public void setClientLiveTime(Period period)
   {
     _clientLiveTime = period.getPeriod();
   }
 
   /**
    * Gets the live time.
    */
   public long getClientLiveTime()
   {
     return _clientLiveTime;
   }
 
   /**
    * Sets the dead time.
    */
   public void setClientDeadTime(Period period)
   {
     _clientDeadTime = period.getPeriod();
   }
 
   /**
    * Gets the dead time.
    */
   public long getClientDeadTime()
   {
     return _clientDeadTime;
   }
 
   /**
    * Sets the read timeout.
    */
   public void setClientReadTimeout(Period period)
   {
     _clientReadTimeout = period.getPeriod();
   }
 
   /**
    * Gets the read timeout.
    */
   public long getClientReadTimeout()
   {
     return _clientReadTimeout;
   }
 
   /**
    * Sets the write timeout.
    */
   public void setClientWriteTimeout(Period period)
   {
     _clientWriteTimeout = period.getPeriod();
   }
 
   /**
    * Gets the write timeout.
    */
   public long getClientWriteTimeout()
   {
     return _clientWriteTimeout;
   }
 
   public StoreManager createJdbcStore()
     throws ConfigException
   {
     StoreManager store = null;
     
     try {
       Class cl = Class.forName("com.caucho.server.cluster.JdbcStore");
 	
       store = (StoreManager) cl.newInstance();
 
       store.setCluster(this);
 
       setStore(store);
     } catch (Throwable e) {
       log.log(Level.FINER, e.toString(), e);
     }
 
     if (store == null)
       throw new ConfigException(L.l("'jdbc' persistent sessions are available in Resin Professional.  See http://www.caucho.com for information and licensing."));
 
     return store;
   }
 
   public StoreManager createClusterStore()
     throws ConfigException
   {
     StoreManager store = null;
     
     try {
       Class cl = Class.forName("com.caucho.server.cluster.ClusterStore");
 	
       store = (StoreManager) cl.newInstance();
 
       store.setCluster(this);
       
       setStore(store);
     } catch (Throwable e) {
       log.log(Level.FINER, e.toString(), e);
     }
 
     if (store == null)
       throw new ConfigException(L.l("'cluster' persistent sessions are available in Resin Professional.  See http://www.caucho.com for information and licensing."));
 
     return store;
   }
 
   /**
    * Initializes the cluster.
    */
   public void init()
     throws ConfigException
   {
     ClusterContainer container = ClusterContainer.create();
 
     for (int i = 0; i < _serverList.length; i++) {
       ClusterServer server = _serverList[i];
 
       if (server == null)
 	continue;
       
       ClusterPort port = server.getClusterPort();
       
       if (port.getReadTimeout() < getClientLiveTime()) {
 	throw new ConfigException(L.l("client-live-time '{0}s' must be less than the read-timeout '{1}s'.",
 				      getClientLiveTime() / 1000L,
 				      port.getReadTimeout() / 1000L));
       }
     }
 
     String serverId = _serverIdLocal.get();
 
     if (serverId == null)
       serverId = "";
 
     if (_ref != null) {
       Cluster cluster = container.findCluster(_ref);
 
       if (cluster == null)
 	throw new ConfigException(L.l("'{0}' is an unknown cluster-ref.",
 				      _ref));
 
       _clusterLocal.set(cluster);
     }
     else {
       container.addCluster(this);
 
       ClusterServer self = findServer(serverId);
 
       if (self != null)
 	_clusterLocal.set(this);
     }
 
     try {
       String name = _id;
       
       if (name == null || name.equals(""))
 	name = "default";
       
       _objectName = Jmx.getObjectName("type=Cluster,name=" + name);
 
       Jmx.register(this, _objectName);
     } catch (Throwable e) {
       log.log(Level.FINER, e.toString(), e);
     }
   }
 
   public void describe(IntrospectionMBeanDescriptor descriptor)
   {
     String title;
 
     String id = getId();
 
     if (id == null || id.length() == 0)
       title = L.l("Cluster");
     else
       title = L.l("Cluster {0}", id);
 
     descriptor.setTitle(title);
   }
 
   /**
    * Returns the server id.
    */
   public static String getServerId()
   {
     return _serverIdLocal.get();
   }
 
   /**
    * Returns the JMX object name.
    */
   public ObjectName getObjectName()
   {
     return _objectName;
   }
   
   public void describeObjectName(IntrospectionAttributeDescriptor descriptor)
   {
     descriptor.setIgnored(true);
   }
 
   /**
    * Returns the server corresponding to the current server-id.
    */
   public ClusterServer getSelfServer()
   {
     _serverId = _serverIdLocal.get();
 
     return getServer(_serverId);
   }
 
   /**
    * Returns the server list.
    */
   public ClusterServer []getServerList()
   {
     return _serverList;
   }
 
   /**
    * Returns the client name list.
    */
   public ObjectName []getClientObjectNames()
   {
     ObjectName []objectNames = new ObjectName[_serverList.length];
 
     for (int i = 0; i < _serverList.length; i++) {
       ClusterServer client = _serverList[i];
 
       objectNames[i] = client.getObjectName();
     }
 
     return objectNames;
   }
 
   public void describeClientObjectNames(IntrospectionAttributeDescriptor descriptor)
   {
     descriptor.setCategory(AdminAttributeCategory.CHILD);
   }
 
   /**
    * Returns the server in the cluster with the given server-id.
    */
   public ClusterServer getServer(String serverId)
   {
     for (int i = 0; i < _serverList.length; i++) {
       ClusterServer server = _serverList[i];
 
       if (server != null && server.getId().equals(serverId))
         return server;
     }
     
     return null;
   }
 
   /**
    * Returns the server with the matching index.
    */
   public ClusterServer getServer(int index)
   {
     for (int i = 0; i < _serverList.length; i++) {
       ClusterServer server = _serverList[i];
 
       if (server != null && server.getIndex() == index)
         return server;
     }
     
     return null;
   }
 
   /**
    * Returns the matching ports.
    */
   public ArrayList<ClusterPort> getServerPorts(String serverId)
   {
     ArrayList<ClusterPort> ports = new ArrayList<ClusterPort>();
     
     for (int i = 0; i < _serverList.length; i++) {
       ClusterServer server = _serverList[i];
 
       if (server != null) {
         ClusterPort port = server.getClusterPort();
       
         if (port.getServerId().equals(serverId))
           ports.add(port);
       }
     }
 
     return ports;
   }
   
   /**
    * Handles the case where a class loader has completed initialization
    */
   public void classLoaderInit(DynamicClassLoader loader)
   {
   }
   
   /**
    * Handles the case where a class loader is dropped.
    */
   public void classLoaderDestroy(DynamicClassLoader loader)
   {
   }
   
   /**
    * Handles the case where the environment is starting (after init).
    */
   public void environmentStart(EnvironmentClassLoader loader)
   {
     try {
       if (_clusterStore != null)
 	_clusterStore.start();
     } catch (Throwable e) {
       log.log(Level.WARNING, e.toString(), e);
     }
   }
   
   /**
    * Handles the case where the environment is stopping
    */
   public void environmentStop(EnvironmentClassLoader loader)
   {
     try {
       close();
     } catch (Throwable e) {
       log.log(Level.WARNING, e.toString(), e);
     }
   }
 
   /**
    * Closes the cluster.
    */
   public void close()
   {
     synchronized (this) {
       if (_isClosed)
         return;
 
       _isClosed = true;
     }
     
     for (int i = 0; i < _serverList.length; i++) {
       ClusterServer server = _serverList[i];
 
       try {
         if (server != null)
           server.close();
       } catch (Throwable e) {
         log.log(Level.WARNING, e.toString(), e);
       }
     }
   }
 
   public String toString()
   {
     return "Cluster[" + _id + "]";
   }
 }
