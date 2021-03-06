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
 package com.vmware.bdd.entity;
 
 import java.util.ArrayList;
 import java.util.HashSet;
 import java.util.List;
 import java.util.Set;
 
 import javax.persistence.CascadeType;
 import javax.persistence.Column;
 import javax.persistence.Entity;
 import javax.persistence.EnumType;
 import javax.persistence.Enumerated;
 import javax.persistence.FetchType;
 import javax.persistence.JoinColumn;
 import javax.persistence.ManyToOne;
 import javax.persistence.OneToMany;
 import javax.persistence.SequenceGenerator;
 import javax.persistence.Table;
 
 import org.apache.log4j.Logger;
 
 import com.vmware.bdd.apitypes.NodeRead;
 import com.vmware.bdd.apitypes.NodeStatus;
import com.vmware.bdd.apitypes.StorageRead.DiskType;
 import com.vmware.bdd.utils.AuAssert;
 
 /**
  * Hadoop Node Entity class: describes hadoop node info
  * 
  */
 @Entity
 @SequenceGenerator(name = "IdSequence", sequenceName = "node_seq", allocationSize = 1)
 @Table(name = "node")
 public class NodeEntity extends EntityBase {
    private static final Logger logger = Logger.getLogger(NodeEntity.class);
    @Column(name = "vm_name", unique = true, nullable = false)
    private String vmName;
 
    @Column(name = "moid", unique = true)
    private String moId;
 
    @Column(name = "rack")
    private String rack;
 
    @Column(name = "host_name")
    private String hostName;
 
    // vm status, poweredOn/poweredOff
    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private NodeStatus status = NodeStatus.NOT_EXIST;
 
    // this field means VM status changed from powered on to powered off or vice visa.
    // if status changed is true, when user query status, will start a query to Ironfan
    // to get VM bootstrap status
    @Column(name = "power_status_changed")
    private boolean powerStatusChanged;
 
    @Column(name = "action")
    private String action;
 
    @Column(name = "ip_address")
    private String ipAddress;
 
    @Column(name = "guest_host_name")
    private String guestHostName;
 
    @ManyToOne
    @JoinColumn(name = "node_group_id")
    private NodeGroupEntity nodeGroup;
 
    @ManyToOne
    @JoinColumn(name = "vc_rp_id")
    private VcResourcePoolEntity vcRp;
 
    @OneToMany(mappedBy = "nodeEntity", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Set<DiskEntity> disks;
 
    public NodeEntity() {
       this.disks = new HashSet<DiskEntity>();
    }
 
    public NodeEntity(String vmName, String rack, String hostName,
          NodeStatus status, String ipAddress) {
       super();
       this.vmName = vmName;
       this.rack = rack;
       this.hostName = hostName;
       this.status = status;
       this.ipAddress = ipAddress;
       this.disks = new HashSet<DiskEntity>();
    }
 
    public List<String> getVolumns() {
       List<String> volumns = new ArrayList<String>();
       for (DiskEntity disk : disks) {
         if (!DiskType.SWAP_DISK.getType().equals(disk.getDiskType()))
            volumns.add(disk.getDeviceName());
       }
       return volumns;
    }
 
    public String getGuestHostName() {
       return guestHostName;
    }
 
    public void setGuestHostName(String guestHostName) {
       this.guestHostName = guestHostName;
    }
 
    public boolean isPowerStatusChanged() {
       return powerStatusChanged;
    }
 
    public void setPowerStatusChanged(boolean powerStatusChanged) {
       this.powerStatusChanged = powerStatusChanged;
    }
 
    public String getVmName() {
       return vmName;
    }
 
    public void setVmName(String vmName) {
       this.vmName = vmName;
    }
 
    public String getMoId() {
       return moId;
    }
 
    public void setMoId(String moId) {
       this.moId = moId;
    }
 
    public String getRack() {
       return rack;
    }
 
    public void setRack(String rack) {
       this.rack = rack;
    }
 
    public String getHostName() {
       return hostName;
    }
 
    public void setHostName(String hostName) {
       this.hostName = hostName;
    }
 
    public NodeStatus getStatus() {
       return status;
    }
 
    /*
     * This method will compare the setting status with existing status.
     * If they are different, powerChanged field will be set accordingly.
     * To turn off statusChanged, user need to call setStatusChanged explicitly.
     * 
     */
    public void setStatus(NodeStatus status) {
       if (status.ordinal() >= NodeStatus.POWERED_ON.ordinal()
             && this.status.ordinal() >= NodeStatus.POWERED_ON.ordinal()) {
          if (status.ordinal() > this.status.ordinal()) {
             this.status = status;
             logger.info("node " + getVmName() + " status changed to " + status);
          }
          return;
       } else if (status.ordinal() <= NodeStatus.POWERED_OFF.ordinal()
             && this.status.ordinal() <= NodeStatus.POWERED_OFF.ordinal()) {
          // the new status and old status are in same range, both powered on, or both powered off status
          // don't think that power status is changed
          if (this.status != status) {
             this.status = status;
             logger.info("node " + getVmName() + " status changed to " + status);
          }
          return;
       }
 
       if (this.status != status) {
          powerStatusChanged = true;
          logger.info("node " + getVmName() + " status changed to " + status);
          this.status = status;
       }
    }
 
    public void setStatus(NodeStatus status, boolean validation) {
       if (validation) {
          setStatus(status);
       } else {
          this.status = status;
       }
    }
 
    public String getAction() {
       return action;
    }
 
    public void setAction(String action) {
       if (this.action != action) {
          logger.info("node " + getVmName() + " action changed to " + action);
          this.action = action;
       }
    }
 
    public String getIpAddress() {
       return ipAddress;
    }
 
    public void setIpAddress(String ipAddress) {
       this.ipAddress = ipAddress;
    }
 
    public NodeGroupEntity getNodeGroup() {
       return nodeGroup;
    }
 
    public void setNodeGroup(NodeGroupEntity nodeGroup) {
       this.nodeGroup = nodeGroup;
    }
 
    public VcResourcePoolEntity getVcRp() {
       return vcRp;
    }
 
    public void setVcRp(VcResourcePoolEntity vcRp) {
       this.vcRp = vcRp;
    }
 
    public List<String> getDatastoreNameList() {
       Set<String> datastores = new HashSet<String>(disks.size());
       for (DiskEntity disk : disks) {
          datastores.add(disk.getDatastoreName());
       }
       return new ArrayList<String>(datastores);
    }
 
    public Set<DiskEntity> getDisks() {
       return disks;
    }
 
    public void setDisks(Set<DiskEntity> disks) {
       this.disks = disks;
    }
 
    public void copy(NodeEntity newNode) {
       this.ipAddress = newNode.getIpAddress();
       this.status = newNode.getStatus();
       this.action = newNode.getAction();
 
       if (newNode.getRack() != null) {
          this.rack = newNode.getRack();
       }
       if (newNode.getHostName() != null) {
          this.hostName = newNode.getHostName();
       }
       if (newNode.getMoId() != null) {
          this.moId = newNode.getMoId();
       }
       if (newNode.getVcRp() != null) {
          this.vcRp = newNode.getVcRp();
       }
 
       if (newNode.getDisks() != null && !newNode.getDisks().isEmpty()) {
          if (this.disks == null)
             this.disks = new HashSet<DiskEntity>(newNode.disks.size());
 
          for (DiskEntity disk : newNode.getDisks()) {
             DiskEntity clone = disk.copy(disk);
             clone.setNodeEntity(this);
             this.disks.add(clone);
          }
       }
    }
 
    public DiskEntity findDisk(String diskName) {
       AuAssert.check(diskName != null && !diskName.isEmpty()
             && this.disks != null);
       for (DiskEntity disk : this.disks) {
          if (disk.getName().equals(diskName))
             return disk;
       }
 
       return null;
    }
 
    // if includeVolumes is true, this method must be called inside a transaction
    public NodeRead toNodeRead(boolean includeVolumes) {
       NodeRead node = new NodeRead();
       node.setRack(this.rack);
       node.setHostName(this.hostName);
       node.setIp(this.ipAddress);
       node.setName(this.vmName);
       node.setMoId(this.moId);
       node.setStatus(this.status.toString());
       node.setAction(this.action);
       List<String> roleNames = nodeGroup.getRoleNameList();
       node.setRoles(roleNames);
       if (includeVolumes)
          node.setVolumes(this.getVolumns());
       return node;
    }
 
    @Override
    public int hashCode() {
       if (this.vmName != null) {
          return this.vmName.hashCode();
       }
       if (this.id != null) {
          return this.id.intValue();
       }
       return 0;
    }
 
    @Override
    public boolean equals(Object node) {
       if (!(node instanceof NodeEntity))
          return false;
       if (this.vmName != null && ((NodeEntity) node).getVmName() != null) {
          return ((NodeEntity) node).getVmName().equals(this.vmName);
       }
       return false;
    }
 }
