 /*
  * Licensed to the Apache Software Foundation (ASF) under one
  * or more contributor license agreements.  See the NOTICE file
  * distributed with this work for additional information
  * regarding copyright ownership.  The ASF licenses this file
  * to you under the Apache License, Version 2.0 (the
  * "License"); you may not use this file except in compliance
  * with the License.  You may obtain a copy of the License at
  *
  *     http://www.apache.org/licenses/LICENSE-2.0
  *
  * Unless required by applicable law or agreed to in writing, software
  * distributed under the License is distributed on an "AS IS" BASIS,
  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  * See the License for the specific language governing permissions and
  * limitations under the License.
  */
 package org.apache.ambari.controller;
 
 import java.io.IOException;
 import java.util.ArrayList;
 import java.util.Date;
 import java.util.List;
 import java.util.StringTokenizer;
 import java.util.concurrent.ConcurrentHashMap;
 
 import javax.ws.rs.WebApplicationException;
 import javax.ws.rs.core.Response;
 
 import org.apache.ambari.common.rest.entities.Stack;
 import org.apache.ambari.common.rest.entities.ClusterDefinition;
 import org.apache.ambari.common.rest.entities.ClusterInformation;
 import org.apache.ambari.common.rest.entities.ClusterState;
 import org.apache.ambari.common.rest.entities.Node;
 import org.apache.ambari.common.rest.entities.RoleToNodes;
 import org.apache.ambari.datastore.DataStoreFactory;
 import org.apache.ambari.datastore.PersistentDataStore;
 import org.apache.ambari.resource.statemachine.ClusterFSM;
 import org.apache.ambari.resource.statemachine.StateMachineInvoker;
 import org.apache.commons.logging.Log;
 import org.apache.commons.logging.LogFactory;
 
 public class Clusters {
     // TODO: replace system.out.print by LOG
     private static Log LOG = LogFactory.getLog(Clusters.class);
     
     /*
      * Operational clusters include both active and inactive clusters
      */
     protected ConcurrentHashMap<String, Cluster> operational_clusters = new ConcurrentHashMap<String, Cluster>();
     protected PersistentDataStore dataStore = DataStoreFactory.getDataStore(DataStoreFactory.ZOOKEEPER_TYPE);
     
     private static Clusters ClustersTypeRef=null;
         
     private Clusters() {
         
         /*
          * Cluster definition 
          */
         ClusterDefinition cluster123 = new ClusterDefinition();
         
         cluster123.setName("blue.dev.Cluster123");
         cluster123.setStackName("cluster123");
         cluster123.setStackRevision("0");
         cluster123.setDescription("cluster123 - development cluster");
         cluster123.setGoalState(ClusterState.CLUSTER_STATE_ATTIC);
         List<String> activeServices = new ArrayList<String>();
         activeServices.add("hdfs");
         activeServices.add("mapred");
         cluster123.setActiveServices(activeServices);
         
         String nodes = "jt-nodex,nn-nodex,hostname-1x,hostname-2x,hostname-3x,"+
                        "hostname-4x,node-2x,node-3x,node-4x";  
         cluster123.setNodes(nodes);
         
         List<RoleToNodes> rnm = new ArrayList<RoleToNodes>();
         
         RoleToNodes rnme = new RoleToNodes();
         rnme.setRoleName("jobtracker-role");
         rnme.setNodes("jt-nodex");
         rnm.add(rnme);
         
         rnme = new RoleToNodes();
         rnme.setRoleName("namenode-role");
         rnme.setNodes("nn-nodex");
         rnm.add(rnme);
         
         rnme = new RoleToNodes();
         rnme.setRoleName("slaves-role");
         rnme.setNodes("hostname-1x,hostname-2x,hostname-3x,"+
                        "hostname-4x,node-2x,node-3x,node-4x");
         rnm.add(rnme);
         
         cluster123.setRoleToNodesMap(rnm);
         
         /*
          * Cluster definition 
          */
         ClusterDefinition cluster124 = new ClusterDefinition();
         cluster124.setName("blue.research.Cluster124");
         cluster124.setStackName("cluster124");
         cluster124.setStackRevision("0");
         cluster124.setDescription("cluster124 - research cluster");
         cluster124.setGoalState(ClusterState.CLUSTER_STATE_INACTIVE);
         activeServices = new ArrayList<String>();
         activeServices.add("hdfs");
         activeServices.add("mapred");
         cluster124.setActiveServices(activeServices);
         
         nodes = "jt-node,nn-node,hostname-1,hostname-2,hostname-3,hostname-4,"+
                 "node-2,node-3,node-4";  
         cluster124.setNodes(nodes);
         
         rnm = new ArrayList<RoleToNodes>();
         
         rnme = new RoleToNodes();
         rnme.setRoleName("jobtracker-role");
         rnme.setNodes("jt-node");
         rnm.add(rnme);
         
         rnme = new RoleToNodes();
         rnme.setRoleName("namenode-role");
         rnme.setNodes("nn-node");
         rnm.add(rnme);
         
         rnme = new RoleToNodes();
         rnme.setRoleName("slaves-role");
         rnme.setNodes("hostname-1,hostname-2,hostname-3,hostname-4,"+
                       "node-2,node-3,node-4");
         rnm.add(rnme);
         
         cluster124.setRoleToNodesMap(rnm);
         
         try {
             if (!clusterExists(cluster123.getName())) {
                 addCluster(cluster123, false);
             }
             if (!clusterExists(cluster124.getName())) {
                 addCluster(cluster124, false);
             }
         } catch (Exception e) {
             e.printStackTrace();
         }
     }
     
     public static synchronized Clusters getInstance() {
         if(ClustersTypeRef == null) {
                 ClustersTypeRef = new Clusters();
         }
         return ClustersTypeRef;
     }
 
     public Object clone() throws CloneNotSupportedException {
         throw new CloneNotSupportedException();
     }
 
     /*
      * Wrapper method over datastore API
      */
     public boolean clusterExists(String clusterName) throws IOException {
         int x = 0;
         if (!this.operational_clusters.containsKey(clusterName) &&
             dataStore.clusterExists(clusterName) == false) {
             return false;
         }
         return true;
     }
     
     /* 
      * Get the cluster by name
      * Wrapper over datastore API
      */
     public synchronized Cluster getClusterByName(String clusterName) throws Exception {
         if (clusterExists(clusterName)) {
             if (!this.operational_clusters.containsKey(clusterName)) {
                 Cluster cls = new Cluster(clusterName);
                 cls.init();
                 this.operational_clusters.put(clusterName, cls);
             }
             return this.operational_clusters.get(clusterName);
         } else {
             return null;
         }
     }
     
     /*
      * Purge the cluster entry from memory and the data store
      */
     public synchronized void purgeClusterEntry (String clusterName) throws IOException {
         dataStore.deleteCluster(clusterName);
         this.operational_clusters.remove(clusterName);
     }
     
     /*
      * Add Cluster Entry
      */
     public synchronized Cluster addClusterEntry (ClusterDefinition cdef, ClusterState cs) throws Exception {
         Cluster cls = new Cluster (cdef, cs);
         this.operational_clusters.put(cdef.getName(), cls);
         return cls;
     }
     
     /*
      * Rename the cluster
      */
     public synchronized void renameCluster(String clusterName, String new_name) throws Exception {
         if (!clusterExists(clusterName)) {
             String msg = "Cluster ["+clusterName+"] does not exist";
             throw new WebApplicationException((new ExceptionResponse(msg, Response.Status.NOT_FOUND)).get());
         }
         
         if (new_name == null || new_name.equals("")) {
             String msg = "New name of the cluster should be specified as query parameter, (?new_name=xxxx)";
             throw new WebApplicationException((new ExceptionResponse(msg, Response.Status.BAD_REQUEST)).get());
         }
         
         /*
          * Check if cluster state is ATTAIC, If yes update the name
          * don't make new revision of cluster definition as it is in ATTIC state
          */
         if (!getClusterByName(clusterName).getClusterState().getState().equals(ClusterState.CLUSTER_STATE_ATTIC)) {
             String msg = "Cluster state is not ATTIC. Cluster is only allowed to be renamed in ATTIC state";
             throw new WebApplicationException((new ExceptionResponse(msg, Response.Status.NOT_ACCEPTABLE)).get());
         }
         
         Cluster x = this.getClusterByName(clusterName);
         ClusterDefinition cdef = x.getClusterDefinition(-1);
         cdef.setName(new_name);
         ClusterState cs = x.getClusterState();
         this.addClusterEntry(cdef, cs);
         this.purgeClusterEntry(clusterName);
     }
     
     /* 
      * Create/Update cluster definition 
      * TODO: As nodes or role to node association changes, validate key services nodes are not removed
     */
     public synchronized ClusterDefinition updateCluster(String clusterName, ClusterDefinition c, boolean dry_run) throws Exception {       
         /*
          * Add new cluster if cluster does not exist
          */
         if (!clusterExists(clusterName)) {
             return addCluster(c, dry_run);
         }
         
         /*
          * Time being we will keep entire updated copy as new revision
          */
         Cluster cls = getClusterByName(clusterName);
         ClusterDefinition newcd = new ClusterDefinition ();
         newcd.setName(clusterName);
         if (c.getStackName() != null) {
             newcd.setStackName(c.getStackName());
         } else {
             newcd.setStackName(cls.getClusterDefinition(-1).getStackName());
         }
         if (c.getStackRevision() != null) {
             newcd.setStackRevision(c.getStackRevision());
         } else {
             newcd.setStackRevision(cls.getClusterDefinition(-1).getStackRevision());
         }
         if (c.getDescription() != null) {
             newcd.setDescription(c.getDescription());
         } else {
             newcd.setDescription(cls.getClusterDefinition(-1).getDescription());
         }
         if (c.getGoalState() != null) {
             newcd.setGoalState(c.getGoalState());
         } else {
             newcd.setGoalState(cls.getClusterDefinition(-1).getGoalState());
         }
         if (c.getActiveServices() != null) {
             newcd.setActiveServices(c.getActiveServices());
         } else {
             newcd.setActiveServices(cls.getClusterDefinition(-1).getActiveServices());
         }
         
         /*
          * TODO: What if controller is crashed after updateClusterNodesReservation 
          * before updating and adding new revision of cluster definition?
          */
         boolean updateNodesReservation = false;
         boolean updateNodeToRolesAssociation = false;
         if (c.getNodes() != null) {
             newcd.setNodes(c.getNodes());
             updateNodesReservation = true;
             
         } else {
             newcd.setNodes(cls.getClusterDefinition(-1).getNodes());
         }
         if (c.getRoleToNodes() != null) {
             newcd.setRoleToNodesMap(c.getRoleToNodes());
             updateNodeToRolesAssociation = true;
             
         }  
         
         /*
          * if Cluster goal state is ATTIC then no need to take any action other than
          * updating the cluster definition.
          */
         if (newcd.getGoalState().equals(ClusterState.CLUSTER_STATE_ATTIC)) {
             ClusterState cs = cls.getClusterState();
             cs.setLastUpdateTime(Util.getXMLGregorianCalendar(new Date()));
             cls.updateClusterDefinition(newcd);
             cls.updateClusterState(cs);
             return cls.getClusterDefinition(-1);
         }
         
         /*
          * Validate the updated cluster definition
          */
         validateClusterDefinition(newcd);
         
         /*
          * If dry_run then return the newcd at this point
          */
         if (dry_run) {
             System.out.println ("Dry run for update cluster..");
             return newcd;
         }
         
         /*
          *  Udate the new cluster definition
          */
         ClusterState cs = cls.getClusterState();
         cs.setLastUpdateTime(Util.getXMLGregorianCalendar(new Date()));
         cls.updateClusterDefinition(newcd);
         cls.updateClusterState(cs);
         
         /*
          * Update the nodes reservation and node to roles association 
          */
         if (updateNodesReservation) {
             updateClusterNodesReservation (cls.getName(), c);   
         }
         if (updateNodeToRolesAssociation) {
             updateNodeToRolesAssociation(newcd.getNodes(), c.getRoleToNodes());
         }
         
         /*
          * Invoke state machine event
          */
         if(c.getGoalState().equals(ClusterState.CLUSTER_STATE_ACTIVE)) {
          StateMachineInvoker.startCluster(cls.getName());
         } else if(c.getGoalState().
             equals(ClusterState.CLUSTER_STATE_INACTIVE)) {
          StateMachineInvoker.stopCluster(cls.getName());
         } else if(c.getGoalState().
             equals(ClusterState.CLUSTER_STATE_ATTIC)) {
          StateMachineInvoker.deleteCluster(cls.getName());
         }
      
         return cls.getClusterDefinition(-1);
     }
     
     /*
      * Add default values for new cluster definition 
      */
     private void setNewClusterDefaults(ClusterDefinition cdef) throws Exception {
         /* 
          * Populate the input cluster definition w/ default values
          */
         if (cdef.getDescription() == null) { cdef.setDescription("Ambari cluster : "+cdef.getName());
         }
         if (cdef.getGoalState() == null) { cdef.setGoalState(ClusterDefinition.GOAL_STATE_INACTIVE);
         }
         
         /*
          * If its new cluster, do not specify the revision, set it to null. A revision number is obtained
          * after persisting the definition
          */
         cdef.setRevision(null);
         
         // TODO: Add the list of active services by querying pluging component.
         if (cdef.getActiveServices() == null) {
             List<String> services = new ArrayList<String>();
             services.add("ALL");
             cdef.setActiveServices(services);
         }    
     }
     
     /* 
      * Add new Cluster to cluster list 
      * Validate the cluster definition
      * Lock the cluster list
      *   -- Check if cluster with given name already exist?
      *   -- Set the cluster state and timestamps 
      *   -- Reserve the nodes. i.e. add the cluster and role referenes to Node
      *   -- Throw exception, if some nodes are already preallocated to other cluster.
      *   -- Persist the cluster definition as revision 0 and list of node names against cluster & service:role 
      *   -- Background daemon should trigger the agent installation on the new nodes (UNREGISTERED), if not done already. 
      *      (daemon can keep track of which nodes agent is already installed or check it by ssh to nodes, if nodes added
      *       are in UNREGISTERED state).  
      */   
     private ClusterDefinition addCluster(ClusterDefinition cdef, boolean dry_run) throws Exception {
         
         /*
          * TODO: Validate the cluster definition and set the default
          * 
          */
         validateClusterDefinition(cdef);
         
         /*
          * Add the defaults for optional values, if not set
          */
         setNewClusterDefaults(cdef);
         
         /*
          * Create new cluster object
          */
         Date requestTime = new Date();
         
         ClusterState clsState = new ClusterState();
         clsState.setCreationTime(Util.getXMLGregorianCalendar(requestTime));
         clsState.setLastUpdateTime(Util.getXMLGregorianCalendar(requestTime));
         clsState.setDeployTime(Util.getXMLGregorianCalendar((Date)null));          
         if (cdef.getGoalState().equals(ClusterDefinition.GOAL_STATE_ATTIC)) {
             clsState.setState(ClusterState.CLUSTER_STATE_ATTIC);
         } else {
             clsState.setState(ClusterDefinition.GOAL_STATE_INACTIVE);
         }
         
         /*
          * TODO: Derive the role to nodes map based on nodes attributes
          * then populate the node to roles association.
          */
         if (cdef.getRoleToNodes() == null) {
             List<RoleToNodes> role2NodesList = generateRoleToNodesListBasedOnNodeAttributes (cdef);
             cdef.setRoleToNodesMap(role2NodesList);
         }
         
         /*
          * If dry run then update roles to nodes map, if not specified explicitly
          * and return
          */
         if (dry_run) {
             return cdef;
         }
         
         /*
          * Persist the new cluster and add entry to cache
          * 
          */
         Cluster cls = this.addClusterEntry(cdef, clsState);
         
         /*
          * Update cluster nodes reservation. 
          */
         if (cdef.getNodes() != null 
             && !cdef.getGoalState().equals(ClusterDefinition.GOAL_STATE_ATTIC)) {
             updateClusterNodesReservation (cls.getName(), cdef);
         }
         
         /*
          * Update the Node to Roles association
          */
         if (!cdef.getGoalState().equals(ClusterDefinition.GOAL_STATE_ATTIC)) {
             updateNodeToRolesAssociation(cdef.getNodes(), cdef.getRoleToNodes());
         }
         
         /*
          * Activate the cluster if the goal state is ACTIVE
          * TODO: What to do if activate fails ??? 
         */
         if(cdef.getGoalState().equals(ClusterDefinition.GOAL_STATE_ACTIVE)) {          
             org.apache.ambari.resource.statemachine.ClusterFSM cs = 
                 StateMachineInvoker.createCluster(cls,cls.getLatestRevisionNumber(),
                     cls.getClusterState());
             cs.activate();
         }
         return cdef;
     } 
     
     /*
      * Create RoleToNodes list based on node attributes
      * TODO: For now just pick some nodes randomly
      */
     public List<RoleToNodes> generateRoleToNodesListBasedOnNodeAttributes (ClusterDefinition cdef) {
         List<RoleToNodes> role2NodesList = new ArrayList<RoleToNodes>();
         return role2NodesList;
     }
     
     /*
      * Validate the cluster definition
      * TODO: Validate each role has enough nodes associated with it. 
      */
     private void validateClusterDefinition (ClusterDefinition cdef) throws Exception {
         /*
          * Check if name is not empty or null
          */
         if (cdef.getName() == null ||  cdef.getName().equals("")) {
             String msg = "Cluster Name must be specified and must be non-empty string";
             throw new WebApplicationException((new ExceptionResponse(msg, Response.Status.BAD_REQUEST)).get());
         }
         
         if (cdef.getNodes() == null || cdef.getNodes().equals("")) {
             String msg = "Cluster node range must be specified and must be non-empty string";
             throw new WebApplicationException((new ExceptionResponse(msg, Response.Status.BAD_REQUEST)).get());
         }
         
         if (cdef.getStackName() == null || cdef.getStackName().equals("")) {
             String msg = "Cluster stack must be specified and must be non-empty string";
             throw new WebApplicationException((new ExceptionResponse(msg, Response.Status.BAD_REQUEST)).get());
         }
         
         if (cdef.getStackRevision() == null || cdef.getStackRevision().equals("")) {
             String msg = "Cluster stack revision must be specified";
             throw new WebApplicationException((new ExceptionResponse(msg, Response.Status.BAD_REQUEST)).get());
         }
         
         /*
          * Check if the cluster stack and its parents exist
          * getStack would throw exception if it does not find the stack
          */
         Stack bp = Stacks.getInstance()
                        .getStack(cdef.getStackName(), Integer.parseInt(cdef.getStackRevision()));
         while (bp.getParentName() != null) {
             if (bp.getParentRevision() == null) {
                 bp = Stacks.getInstance()
                     .getStack(bp.getParentName(), -1);
             } else {
                 bp = Stacks.getInstance()
                 .getStack(bp.getParentName(), Integer.parseInt(bp.getParentRevision()));
             }
         }
         
         
         /*
          * Check if all the nodes explicitly specified in the RoleToNodesMap belong the cluster node range specified 
          */
         List<String> cluster_node_range = new ArrayList<String>();
         cluster_node_range.addAll(getHostnamesFromRangeExpressions(cdef.getNodes()));
         if (cdef.getRoleToNodes() != null) {
             List<String> nodes_specified_using_role_association = new ArrayList<String>();
             for (RoleToNodes e : cdef.getRoleToNodes()) {
                 List<String> hosts = getHostnamesFromRangeExpressions(e.getNodes());
                 nodes_specified_using_role_association.addAll(hosts);
                 // TODO: Remove any duplicate nodes from nodes_specified_using_role_association
             }
             
             nodes_specified_using_role_association.removeAll(cluster_node_range);
             if (!nodes_specified_using_role_association.isEmpty()) {
                 String msg = "Some nodes explicityly associated with roles using RoleToNodesMap do not belong in the " +
                              "golbal node range specified for the cluster : ["+nodes_specified_using_role_association+"]";
                 throw new WebApplicationException((new ExceptionResponse(msg, Response.Status.BAD_REQUEST)).get());
             }
         }
     }
     
     /*
      * Update the nodes associated with cluster
      */
     private synchronized void updateClusterNodesReservation (String clusterName, ClusterDefinition clsDef) throws Exception {
                 
         ConcurrentHashMap<String, Node> all_nodes = Nodes.getInstance().getNodes();
         List<String> cluster_node_range = new ArrayList<String>();
         cluster_node_range.addAll(getHostnamesFromRangeExpressions(clsDef.getNodes()));
        
         /*
          * Reserve the nodes as specified in the node range expressions
          * -- throw exception, if any nodes are pre-associated with other cluster
          */    
         List<String> nodes_currently_allocated_to_cluster = new ArrayList<String>();
         for (Node n : Nodes.getInstance().getNodes().values()) {
             if ( n.getNodeState().getClusterName() != null &&
                  n.getNodeState().getClusterName().equals(clusterName)) {
                 nodes_currently_allocated_to_cluster.add(n.getName());
             }
         }
         
         List<String> nodes_to_allocate = new ArrayList<String>(cluster_node_range);
         nodes_to_allocate.removeAll(nodes_currently_allocated_to_cluster);
         List<String> nodes_to_deallocate = new ArrayList<String>(nodes_currently_allocated_to_cluster);
         nodes_to_deallocate.removeAll(cluster_node_range);
         
         /*
          * Check for any nodes that are allocated to other cluster
          */
         List<String> preallocatedhosts = new ArrayList<String>();
         for (String n : nodes_to_allocate) {
             if (all_nodes.containsKey(n) && 
                     (all_nodes.get(n).getNodeState().getClusterName() != null || 
                      all_nodes.get(n).getNodeState().getAllocatedToCluster()
                     )
                 ) {
                 preallocatedhosts.add(n);
             }
         }
         
         /* 
          * Throw exception, if some of the hosts are already allocated to other cluster
          */
         if (!preallocatedhosts.isEmpty()) {
             /*
              * TODO: Return invalid request code and return list of preallocated nodes as a part of
              *       response element
              */
             String msg = "Some of the nodes specified for the cluster roles are allocated to other cluster: ["+preallocatedhosts+"]";
             throw new WebApplicationException((new ExceptionResponse(msg, Response.Status.CONFLICT)).get());
         }
         
         /*
          * Allocate nodes to given cluster
          */    
         for (String node_name : nodes_to_allocate) {
             if (all_nodes.containsKey(node_name)) { 
                 // Set the cluster name in the node 
                 synchronized (all_nodes.get(node_name)) {
                     all_nodes.get(node_name).reserveNodeForCluster(clusterName, true);
                 }    
             } else {
                 Date epoch = new Date(0);
                 Nodes.getInstance().checkAndUpdateNode(node_name, epoch);
                 Node node = Nodes.getInstance().getNode(node_name);
                 /*
                  * TODO: Set agentInstalled = true, unless controller uses SSH to setup the agent
                  */
                 node.reserveNodeForCluster(clusterName, true);
             }
         }
         
         /*
          * deallocate nodes from a given cluster
          * TODO: Node agent would asynchronously clean up the node and notify it through heartbeat which 
          * would reset the clusterID associated with node
          */
         for (String node_name : nodes_to_deallocate) {
             if (all_nodes.containsKey(node_name)) {
                 synchronized (all_nodes.get(node_name)) {
                     all_nodes.get(node_name).releaseNodeFromCluster();
                 }
             }
         }
     }
 
     /*
      * This function disassociate all the nodes from the cluster. The clsuterID associated w/
      * cluster will be reset by heart beat when node reports all clean.
      */
     public synchronized void releaseClusterNodes (String clusterName) throws Exception {
         for (Node clusterNode : Nodes.getInstance().getClusterNodes (clusterName, "", "")) {
             clusterNode.releaseNodeFromCluster();     
         }
     }
     
     /**
      * Update Node to Roles association.  
      * If role is not explicitly associated w/ any node, then assign it w/ default role
      * 
      * @param clusterNodes
      * @param roleToNodesList
      * @throws Exception
      */
     private synchronized void updateNodeToRolesAssociation (String clusterNodes, List<RoleToNodes> roleToNodesList) throws Exception {
         /*
          * Associate roles list with node
          */
         if (roleToNodesList == null) {
             return;
         }
         
         /*
          * Add list of roles to Node
          * If node is not explicitly associated with any role then assign it w/ default role
          */
         for (RoleToNodes e : roleToNodesList) {
             List<String> hosts = getHostnamesFromRangeExpressions(e.getNodes());
             for (String host : hosts) {
               if (Nodes.getInstance().getNodes().get(host).getNodeState().getNodeRoleNames() == null) {
                 Nodes.getInstance().getNodes().get(host).getNodeState().setNodeRoleNames((new ArrayList<String>()));
               } 
               Nodes.getInstance().getNodes().get(host).getNodeState().getNodeRoleNames().add(e.getRoleName());
             }
         }
         
         
         /*
          * Get the list of specified global node list for the cluster and any nodes NOT explicitly specified in the
          * role to nodes map, assign them with default role 
          */
         List<String> specified_node_range = new ArrayList<String>();
         specified_node_range.addAll(getHostnamesFromRangeExpressions(clusterNodes));
         for (String host : specified_node_range) {
             if (Nodes.getInstance().getNodes().get(host).getNodeState().getNodeRoleNames() == null) {
                 Nodes.getInstance().getNodes().get(host).getNodeState().setNodeRoleNames((new ArrayList<String>()));
                 String cid = Nodes.getInstance().getNodes().get(host).getNodeState().getClusterName();
                 Nodes.getInstance().getNodes().get(host).getNodeState().getNodeRoleNames().add(getDefaultRoleName(cid));
             } 
         }
     }
 
     /*
      * Get Cluster stack
      */
     public Stack getClusterStack(String clusterName, boolean expanded) throws Exception {
         if (!this.clusterExists(clusterName)) {
             String msg = "Cluster ["+clusterName+"] does not exist";
             throw new WebApplicationException((new ExceptionResponse(msg, Response.Status.NOT_FOUND)).get());
         }
         
         Cluster cls = this.getClusterByName(clusterName);
         String stackName = cls.getClusterDefinition(-1).getStackName();
         int stackRevision = Integer.parseInt(cls.getClusterDefinition(-1).getStackRevision());
         
         Stack bp;
         if (!expanded) {
             bp = Stacks.getInstance().getStack(stackName, stackRevision);
         } else {
             // TODO: Get the derived/expanded stack
             bp = Stacks.getInstance().getStack(stackName, stackRevision);
         }
         return bp;
     }
     
     
     /*
      * Delete Cluster 
      * Delete operation will mark the cluster to_be_deleted and then set the goal state to ATTIC
      * Once cluster gets to ATTIC state, background daemon should purge the cluster entry.
      */
     public synchronized void deleteCluster(String clusterName) throws Exception { 
 
         if (!this.clusterExists(clusterName)) {
             System.out.println("Cluster ["+clusterName+"] does not exist!");
             return;
         }
         
         /*
          * Update the cluster definition with goal state to be ATTIC
          */
         Cluster cls = this.getClusterByName(clusterName);   
         ClusterDefinition cdf = new ClusterDefinition();
         cdf.setName(clusterName);
         cdf.setGoalState(ClusterState.CLUSTER_STATE_ATTIC);
         cls.updateClusterDefinition(cdf);
         
         /* 
          * Update cluster state, mark it "to be deleted"
          */
         ClusterState cs = cls.getClusterState();
         cs.setMarkForDeletionWhenInAttic(true); 
         cls.updateClusterState(cs);
     }      
     
     /*
      * Get the latest cluster definition
      */
     public ClusterDefinition getLatestClusterDefinition(String clusterName) throws Exception {
         return this.getClusterByName(clusterName).getClusterDefinition(-1);
     }
     
     /*
      * Get Cluster Definition given name and revision
      */
     public ClusterDefinition getClusterDefinition(String clusterName, int revision) throws Exception {
         return this.getClusterByName(clusterName).getClusterDefinition(revision);
     }
     
     /* 
      * Get the cluster Information by name
      */
     public ClusterInformation getClusterInformation (String clusterName) throws Exception  {
         if (!this.clusterExists(clusterName)) {
             String msg = "Cluster ["+clusterName+"] does not exist";
             throw new WebApplicationException((new ExceptionResponse(msg, Response.Status.NOT_FOUND)).get());
         }
         ClusterInformation clsInfo = new ClusterInformation();
         clsInfo.setDefinition(this.getLatestClusterDefinition(clusterName));
         clsInfo.setState(this.getClusterByName(clusterName).getClusterState());
         return clsInfo;
     }
     
     
     /* 
      * Get the cluster state
     */
     public ClusterState getClusterState(String clusterName) throws Exception {
         if (!this.clusterExists(clusterName)) {
             String msg = "Cluster ["+clusterName+"] does not exist";
             throw new WebApplicationException((new ExceptionResponse(msg, Response.Status.NOT_FOUND)).get());
         }
         return this.getClusterByName(clusterName).getClusterState();
     }
     
     
     /*
      * Get Cluster Information list i.e. cluster definition and cluster state
      */
     public List<ClusterInformation> getClusterInformationList(String state) throws Exception {
       List<ClusterInformation> list = new ArrayList<ClusterInformation>();
       List<String> clusterNames = dataStore.retrieveClusterList();
       for (String clsName : clusterNames) {
         Cluster cls = this.getClusterByName(clsName);
         if (state.equals("ALL")) {
           ClusterInformation clsInfo = new ClusterInformation();
           clsInfo.setDefinition(cls.getClusterDefinition(-1));
           clsInfo.setState(cls.getClusterState());
           list.add(clsInfo);
         } else {
           if (cls.getClusterState().getState().equals(state)) {
               ClusterInformation clsInfo = new ClusterInformation();
               clsInfo.setDefinition(cls.getClusterDefinition(-1));
               clsInfo.setState(cls.getClusterState());
               list.add(clsInfo);
           }
         }
       }
       return list;
     }
     
     /*
      * Get the list of clusters
      * TODO: Get the synchronized snapshot of each cluster definition? 
      */
     public List<Cluster> getClustersList(String state) throws Exception {
         List<Cluster> list = new ArrayList<Cluster>();
         List<String> clusterNames = dataStore.retrieveClusterList();
         for (String clsName : clusterNames) {
           Cluster cls = this.getClusterByName(clsName);
           if (state.equals("ALL")) {
             list.add(cls);
           } else {
             if (cls.getClusterState().getState().equals(state)) {
                 list.add(cls);
             }
           }
         }
         return list;
     }
     
     /* 
      * UTIL methods on entities
      */
     
     /*
      * Get the list of role names associated with node
      */
     public List<String> getAssociatedRoleNames(String hostname) {
       return Nodes.getInstance().getNodes().get(hostname).getNodeState().getNodeRoleNames();
     }
     
     /*
      *  Return the default role name to be associated with specified cluster node that 
      *  has no specific role to nodes association specified in the cluster definition
      *  Throw exception if node is not associated to with any cluster
      */
     public String getDefaultRoleName(String clusterName) throws Exception {
         Cluster c = Clusters.getInstance().getClusterByName(clusterName);
         // TODO: find the default role from the clsuter stack 
         return "slaves-role";
     }
     
   /*
    * TODO: Implement proper range expression
    * TODO: Remove any duplicate nodes from the derived list
    */
   public List<String> getHostnamesFromRangeExpressions (String nodeRangeExpression) throws Exception {
       List<String> list = new ArrayList<String>();
       StringTokenizer st = new StringTokenizer(nodeRangeExpression, ",");
       while (st.hasMoreTokens()) {
         list.add(st.nextToken());
       }
       return list;
   }
   
   /*
    * Restart recovery for clusters
    */
   public void recoverClustersStateAfterRestart () throws Exception {
       for (Cluster cls : this.getClustersList("ALL")) {
           ClusterDefinition cdef = cls.getClusterDefinition(-1);
           this.validateClusterDefinition (cdef);
           /*
            * Update cluster nodes reservation. 
            */
           if (cdef.getNodes() != null 
               && !cdef.getGoalState().equals(ClusterDefinition.GOAL_STATE_ATTIC)) {
               this.updateClusterNodesReservation (cls.getName(), cdef);
           }
           
           /*
            * Update the Node to Roles association
            *
            */
           if (!cdef.getGoalState().equals(ClusterDefinition.GOAL_STATE_ATTIC)) {
               this.updateNodeToRolesAssociation(cdef.getNodes(), cdef.getRoleToNodes());
           }
       }
   }
 }
