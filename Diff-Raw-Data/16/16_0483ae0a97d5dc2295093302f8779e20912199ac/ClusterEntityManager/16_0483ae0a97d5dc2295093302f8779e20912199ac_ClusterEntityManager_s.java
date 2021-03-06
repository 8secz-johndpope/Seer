 /***************************************************************************
  * Copyright (c) 2012-2013 VMware, Inc. All Rights Reserved.
  * Licensed under the Apache License, Version 2.0 (the "License");
  * you may not use this file except in compliance with the License.
  * You may obtain a copy of the License at
  *
  *     http://www.apache.org/licenses/LICENSE-2.0
  *
  * Unless required by applicable law or agreed to in writing, software
  * distributed under the License is distributed on an "AS IS" BASIS,
  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  * See the License for the specific language governing permissions and
  * limitations under the License.
  ***************************************************************************/
 package com.vmware.bdd.manager;
 
 import java.util.ArrayList;
 import java.util.Iterator;
 import java.util.List;
 import java.util.Map;
 import java.util.Set;
 
 import org.apache.log4j.Logger;
 import org.springframework.beans.factory.annotation.Autowired;
 import org.springframework.transaction.annotation.Transactional;
 
 import com.google.gson.Gson;
 import com.vmware.aurora.vc.VcCache;
 import com.vmware.aurora.vc.VcVirtualMachine;
 import com.vmware.bdd.apitypes.ClusterRead;
 import com.vmware.bdd.apitypes.ClusterRead.ClusterStatus;
 import com.vmware.bdd.apitypes.NodeGroupRead;
 import com.vmware.bdd.apitypes.NodeStatus;
 import com.vmware.bdd.apitypes.ResourcePoolRead;
 import com.vmware.bdd.dal.IClusterDAO;
 import com.vmware.bdd.dal.INodeDAO;
 import com.vmware.bdd.dal.INodeGroupDAO;
 import com.vmware.bdd.entity.ClusterEntity;
 import com.vmware.bdd.entity.DiskEntity;
 import com.vmware.bdd.entity.NodeEntity;
 import com.vmware.bdd.entity.NodeGroupEntity;
 import com.vmware.bdd.entity.VcResourcePoolEntity;
 import com.vmware.bdd.software.mgmt.thrift.GroupData;
 import com.vmware.bdd.software.mgmt.thrift.OperationStatusWithDetail;
 import com.vmware.bdd.software.mgmt.thrift.ServerData;
 import com.vmware.bdd.utils.AuAssert;
 import com.vmware.bdd.utils.Constants;
 import com.vmware.bdd.utils.VcVmUtil;
 
 @Transactional(readOnly = true)
 public class ClusterEntityManager {
    private static final Logger logger = Logger
          .getLogger(ClusterEntityManager.class);
 
    private IClusterDAO clusterDao;
 
    private INodeGroupDAO nodeGroupDao;
 
    private INodeDAO nodeDao;
 
    public IClusterDAO getClusterDao() {
       return clusterDao;
    }
 
    @Autowired
    public void setClusterDao(IClusterDAO clusterDao) {
       this.clusterDao = clusterDao;
    }
 
    public INodeGroupDAO getNodeGroupDao() {
       return nodeGroupDao;
    }
 
    @Autowired
    public void setNodeGroupDao(INodeGroupDAO nodeGroupDao) {
       this.nodeGroupDao = nodeGroupDao;
    }
 
    public INodeDAO getNodeDao() {
       return nodeDao;
    }
 
    @Autowired
    public void setNodeDao(INodeDAO nodeDao) {
       this.nodeDao = nodeDao;
    }
 
    public ClusterEntity findClusterById(Long id) {
       return clusterDao.findById(id);
    }
 
    public NodeGroupEntity findNodeGroupById(Long id) {
       return nodeGroupDao.findById(id);
    }
 
    public NodeEntity findNodeById(Long id) {
       return nodeDao.findById(id);
    }
 
    public ClusterEntity findByName(String clusterName) {
       return clusterDao.findByName(clusterName);
    }
 
    public NodeGroupEntity findByName(String clusterName, String groupName) {
       return nodeGroupDao.findByName(clusterDao.findByName(clusterName),
             groupName);
    }
 
    public NodeGroupEntity findByName(ClusterEntity cluster, String groupName) {
       return nodeGroupDao.findByName(cluster, groupName);
    }
 
    public NodeEntity findByName(String clusterName, String groupName,
          String nodeName) {
       return nodeDao.findByName(findByName(clusterName, groupName), nodeName);
    }
 
    public NodeEntity findByName(NodeGroupEntity nodeGroup, String nodeName) {
       return nodeDao.findByName(nodeGroup, nodeName);
    }
 
    public NodeEntity findNodeByName(String nodeName) {
       return nodeDao.findByName(nodeName);
    }
 
    public List<ClusterEntity> findAllClusters() {
       return clusterDao.findAll();
    }
 
    public List<NodeGroupEntity> findAllGroups(String clusterName) {
       List<ClusterEntity> clusters = new ArrayList<ClusterEntity>();
       ClusterEntity cluster = clusterDao.findByName(clusterName);
       clusters.add(cluster);
 
       return nodeGroupDao.findAllByClusters(clusters);
    }
 
    public List<NodeEntity> findAllNodes(String clusterName) {
       return clusterDao.getAllNodes(clusterName);
    }
 
    public List<NodeEntity> findAllNodes(String clusterName, String groupName) {
       NodeGroupEntity nodeGroup = findByName(clusterName, groupName);
       return new ArrayList<NodeEntity>(nodeGroup.getNodes());
    }
 
    @Transactional
    public void insert(ClusterEntity cluster) {
       AuAssert.check(cluster != null);
       clusterDao.insert(cluster);
    }
 
    @Transactional
    public void insert(NodeEntity node) {
       AuAssert.check(node != null);
       nodeDao.insert(node);
    }
 
    @Transactional
    public void delete(NodeEntity node) {
       AuAssert.check(node != null);
       // remove from parent's collection by cascading
       NodeGroupEntity parent = node.getNodeGroup();
       parent.getNodes().remove(node);
       nodeDao.delete(node);
    }
 
    @Transactional
    public void delete(ClusterEntity cluster) {
       AuAssert.check(cluster != null);
       clusterDao.delete(cluster);
    }
 
    @Transactional
    public void updateClusterStatus(String clusterName, ClusterStatus status) {
       clusterDao.updateStatus(clusterName, status);
    }
 
    @Transactional
    public void update(ClusterEntity clusterEntity) {
       clusterDao.update(clusterEntity);
    }
 
    @Transactional
    public void update(NodeGroupEntity group) {
       nodeGroupDao.update(group);
    }
 
    @Transactional
    public void update(NodeEntity node) {
       nodeDao.update(node);
    }
 
    @Transactional
    public void updateDisks(String nodeName, List<DiskEntity> diskSets) {
       NodeEntity node = findNodeByName(nodeName);
       for (DiskEntity disk : diskSets) {
          boolean found = false;
          for (DiskEntity old : node.getDisks()) {
             if (disk.getName().equals(old.getName())) {
                found = true;
                old.setDatastoreName(disk.getDatastoreName());
                old.setDatastoreMoId(disk.getDatastoreMoId());
                old.setVmdkPath(disk.getVmdkPath());
                old.setSizeInMB(disk.getSizeInMB());
             }
          }
          if (!found) {
             disk.setNodeEntity(node);
             node.getDisks().add(disk);
          }
       }
    }
 
    @Transactional
    public boolean handleOperationStatus(String clusterName,
          OperationStatusWithDetail status) {
       logger.info("handle operation status- finished: "
             + status.getOperationStatus().isFinished()
             + status.getOperationStatus());
       boolean finished = status.getOperationStatus().isFinished();
       final Map<String, GroupData> groups = status.getClusterData().getGroups();
 
       ClusterEntity cluster = findByName(clusterName);
       AuAssert.check(cluster.getId() != null);
       for (NodeGroupEntity group : cluster.getNodeGroups()) {
          for (String groupName : groups.keySet()) {
             if (groupName.equals(group.getName())) {
                for (ServerData serverData : groups.get(groupName)
                      .getInstances()) {
                   logger.debug("server data: " + serverData.getName()
                         + ", action:" + serverData.getAction() + ", status:"
                         + serverData.getStatus());
                   Iterator<NodeEntity> iter = group.getNodes().iterator();
                   while (iter.hasNext()) {
                      NodeEntity oldNode = iter.next();
                      if (oldNode.getVmName().equals(serverData.getName())) {
                         logger.debug("old node:" + oldNode.getVmName()
                               + ", status: " + oldNode.getStatus());
                         oldNode.setAction(serverData.getAction());
                         logger.debug("node status: "
                               + NodeStatus.fromString(serverData.getStatus()));
                         oldNode.setStatus(
                               NodeStatus.fromString(serverData.getStatus()),
                               false);
                         logger.debug("new node:" + oldNode.getVmName()
                               + ", status: " + oldNode.getStatus());
                         update(oldNode);
                         break;
                      }
                   }
                }
             }
          }
       }
       logger.debug("updated database");
       return finished;
    }
 
    private void setNotExist(NodeEntity node) {
       logger.debug("vm " + node.getVmName()
             + " does not exist. Update node status to NOT_EXIST.");
       node.setStatus(NodeStatus.NOT_EXIST);
       node.setIpAddress(null);
       node.setHostName(null);
       node.setMoId(null);
       if (node.getAction() != null
             && !(node.getAction().equals(Constants.NODE_ACTION_CLONING_VM))
             && !(node.getAction().equals(Constants.NODE_ACTION_CLONING_FAILED))) {
          node.setAction(null);
       }
       update(node);
    }
 
    @Transactional
    synchronized public void syncUp(String clusterName) {
       List<NodeEntity> nodes = findAllNodes(clusterName);
 
       for (NodeEntity node : nodes) {
          refreshNodeStatus(node, false);
       }
    }
 
    @Transactional
    synchronized public void syncUpNode(String clusterName, String nodeName) {
       NodeEntity node = findNodeByName(nodeName);
       if (node != null) {
          refreshNodeStatus(node, false);
       }
    }
 
    private void refreshNodeStatus(NodeEntity node, boolean inSession) {
       String mobId = node.getMoId();
       if (mobId == null) {
          setNotExist(node);
          return;
       }
       VcVirtualMachine vcVm = VcCache.getIgnoreMissing(mobId);
       if (vcVm == null) {
          // vm is deleted
          setNotExist(node);
          return;
       }
       // TODO: consider more status
       if (!vcVm.isPoweredOn()) {
          node.setStatus(NodeStatus.POWERED_OFF);
       } else {
          node.setStatus(NodeStatus.POWERED_ON);
       }
 
       if (node.isPowerStatusChanged()) {
          if (vcVm.isPoweredOn()) {
             //update ip address
             String ipAddress = VcVmUtil.getIpAddress(vcVm, inSession);
             if (ipAddress != null) {
                node.setStatus(NodeStatus.VM_READY);
                node.setIpAddress(ipAddress);
                if (node.getAction() != null
                      && node.getAction().equals(
                            Constants.NODE_ACTION_WAITING_IP)) {
                   node.setAction(null);
                }
             }
             String guestHostName = VcVmUtil.getGuestHostName(vcVm, inSession);
             if (guestHostName != null) {
                node.setGuestHostName(guestHostName);
             }
          }
          node.setHostName(vcVm.getHost().getName());
       }
       update(node);
    }
 
    @SuppressWarnings("rawtypes")
    public ClusterRead toClusterRead(String clusterName) {
       ClusterEntity cluster = findByName(clusterName);
       ClusterStatus clusterStatus = cluster.getStatus();
       ClusterRead clusterRead = new ClusterRead();
       clusterRead.setInstanceNum(cluster.getRealInstanceNum());
       clusterRead.setName(cluster.getName());
       clusterRead.setStatus(clusterStatus);
       clusterRead.setDistro(cluster.getDistro());
       clusterRead.setDistroVendor(cluster.getDistroVendor());
       clusterRead.setTopologyPolicy(cluster.getTopologyPolicy());
       clusterRead.setAutomationEnable(cluster.getAutomationEnable());
       clusterRead.setVhmMinNum(cluster.getVhmMinNum());
       clusterRead.setVhmTargetNum(cluster.getVhmTargetNum());
       clusterRead.setIoShares(cluster.getIoShares());
 
       List<NodeGroupRead> groupList = new ArrayList<NodeGroupRead>();
       for (NodeGroupEntity group : cluster.getNodeGroups()) {
          groupList.add(group.toNodeGroupRead());
       }
       clusterRead.setNodeGroups(groupList);
      if (cluster.getHadoopConfig() != null) {
          Map conf = (new Gson()).fromJson(cluster.getHadoopConfig(), Map.class);
          Map hadoopConf = (Map) conf.get("hadoop");
          if (hadoopConf != null) {
             Map coreSiteConf = (Map) hadoopConf.get("core-site.xml");
             if (coreSiteConf != null) {
                String hdfs = (String) coreSiteConf.get("fs.default.name");
                if (hdfs != null && !hdfs.isEmpty()) {
                   clusterRead.setExternalHDFS(hdfs);
                }
             }
          }
       }
 
       Set<VcResourcePoolEntity> rps = cluster.getUsedRps();
       List<ResourcePoolRead> rpReads = new ArrayList<ResourcePoolRead>(rps.size());
       for (VcResourcePoolEntity rp : rps) {
          ResourcePoolRead rpRead = rp.toRest();
          rpRead.setNodes(null);
          rpReads.add(rpRead);
       }
       clusterRead.setResourcePools(rpReads);
 
       if (clusterStatus == ClusterStatus.RUNNING || clusterStatus == ClusterStatus.STOPPED) {
          clusterRead.setDcSeperation(clusterRead.validateSetManualElasticity());
       }
 
       return clusterRead;
    }
 
    @Transactional
    synchronized public void refreshNodeByMobId(String vmId, boolean inSession) {
       NodeEntity node = nodeDao.findByMobId(vmId);
       if (node != null) {
          refreshNodeStatus(node, inSession);
       }
    }
 
    @Transactional
    synchronized public void refreshNodeByMobId(String vmId, String action,
          boolean inSession) {
       NodeEntity node = nodeDao.findByMobId(vmId);
       if (node != null) {
          refreshNodeStatus(node, inSession);
          node.setAction(action);
       }
    }
 
    public NodeEntity getNodeByMobId(String vmId) {
       return nodeDao.findByMobId(vmId);
    }
 
    public NodeEntity getNodeByVmName(String vmName) {
       return nodeDao.findByName(vmName);
    }
 
    @Transactional
    synchronized public void refreshNodeByVmName(String vmId, String vmName,
          boolean inSession) {
       NodeEntity node = nodeDao.findByName(vmName);
       if (node != null) {
          node.setMoId(vmId);
          refreshNodeStatus(node, inSession);
       }
    }
 
    @Transactional
    synchronized public void refreshNodeByVmName(String vmId, String vmName,
          String nodeAction, boolean inSession) {
       NodeEntity node = nodeDao.findByName(vmName);
       if (node != null) {
          node.setMoId(vmId);
          refreshNodeStatus(node, inSession);
          node.setAction(nodeAction);
       }
    }
 
    @Transactional
    public void updateClusterTaskId(String clusterName, Long taskId) {
       ClusterEntity cluster = clusterDao.findByName(clusterName);
       cluster.setLatestTaskId(taskId);
       clusterDao.update(cluster);
    }
 
    public List<Long> getLatestTaskIds() {
       List<ClusterEntity> clusters = clusterDao.findAll();
       List<Long> taskIds = new ArrayList<Long>(clusters.size());
 
       for (ClusterEntity cluster : clusters) {
          taskIds.add(cluster.getLatestTaskId());
       }
 
       return taskIds;
    }
 
    public List<DiskEntity> getDisks(String nodeName) {
       NodeEntity node = nodeDao.findByName(nodeName);
       return new ArrayList<DiskEntity>(node.getDisks());
    }
 }
