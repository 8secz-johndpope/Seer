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
 package com.vmware.bdd.utils;
 
 import java.util.ArrayList;
 import java.util.Arrays;
 import java.util.HashSet;
 import java.util.List;
 import java.util.Map;
 import java.util.Set;
 import java.util.UUID;
 import java.util.concurrent.Callable;
 
 import com.vmware.bdd.apitypes.NetworkAdd;
 import com.vmware.vim.binding.impl.vim.vApp.VmConfigSpecImpl;
 import com.vmware.vim.binding.impl.vim.vm.ToolsConfigInfoImpl;
 import com.vmware.vim.binding.vim.VirtualMachine;
 import com.vmware.vim.binding.vim.net.IpConfigInfo.IpAddress;
 import com.vmware.vim.binding.vim.option.OptionValue;
 import com.vmware.vim.binding.vim.vApp.VmConfigSpec;
 import com.vmware.vim.binding.vim.vm.ToolsConfigInfo;
 import org.apache.log4j.Logger;
 
 import com.vmware.aurora.composition.DiskSchema;
 import com.vmware.aurora.composition.DiskSchema.Disk;
 import com.vmware.aurora.composition.NetworkSchema;
 import com.vmware.aurora.composition.NetworkSchema.Network;
 import com.vmware.aurora.composition.ResourceSchema;
 import com.vmware.aurora.composition.VmSchema;
 import com.vmware.aurora.composition.concurrent.ExecutionResult;
 import com.vmware.aurora.composition.concurrent.Scheduler;
 import com.vmware.aurora.vc.DeviceId;
 import com.vmware.aurora.vc.DiskSpec.AllocationType;
 import com.vmware.aurora.vc.MoUtil;
 import com.vmware.aurora.vc.VcCache;
 import com.vmware.aurora.vc.VcCluster;
 import com.vmware.aurora.vc.VcDatastore;
 import com.vmware.aurora.vc.VcResourcePool;
 import com.vmware.aurora.vc.VcSnapshot;
 import com.vmware.aurora.vc.VcVirtualMachine;
 import com.vmware.aurora.vc.VmConfigUtil;
 import com.vmware.aurora.vc.vcservice.VcContext;
 import com.vmware.aurora.vc.vcservice.VcSession;
 import com.vmware.bdd.apitypes.ClusterCreate;
 import com.vmware.bdd.apitypes.NodeGroupCreate;
 import com.vmware.bdd.apitypes.NodeStatus;
 import com.vmware.bdd.apitypes.Priority;
 import com.vmware.bdd.entity.DiskEntity;
 import com.vmware.bdd.entity.NodeEntity;
 import com.vmware.bdd.entity.VcResourcePoolEntity;
 import com.vmware.bdd.exception.BddException;
 import com.vmware.bdd.exception.ClusteringServiceException;
 import com.vmware.bdd.placement.entity.BaseNode;
 import com.vmware.bdd.service.sp.NoProgressUpdateCallback;
 import com.vmware.bdd.service.utils.VcResourceUtils;
 import com.vmware.bdd.spectypes.DiskSpec;
 import com.vmware.vim.binding.impl.vim.SharesInfoImpl;
 import com.vmware.vim.binding.impl.vim.StorageResourceManager_Impl.IOAllocationInfoImpl;
 import com.vmware.vim.binding.impl.vim.vm.device.VirtualDeviceSpecImpl;
 import com.vmware.vim.binding.vim.Datastore;
 import com.vmware.vim.binding.vim.SharesInfo;
 import com.vmware.vim.binding.vim.SharesInfo.Level;
 import com.vmware.vim.binding.vim.StorageResourceManager.IOAllocationInfo;
 import com.vmware.vim.binding.vim.VirtualMachine.FaultToleranceState;
 import com.vmware.vim.binding.vim.cluster.DasVmSettings.RestartPriority;
 import com.vmware.vim.binding.vim.vm.FaultToleranceConfigInfo;
 import com.vmware.vim.binding.vim.vm.GuestInfo;
 import com.vmware.vim.binding.vim.vm.GuestInfo.NicInfo;
 import com.vmware.vim.binding.vim.vm.device.VirtualDevice;
 import com.vmware.vim.binding.vim.vm.device.VirtualDeviceSpec;
 import com.vmware.vim.binding.vim.vm.device.VirtualDisk;
 import com.vmware.vim.binding.vim.vm.device.VirtualDiskOption.DiskMode;
 import com.vmware.vim.binding.vim.vm.FlagInfo;
 import com.vmware.vim.binding.impl.vim.vm.FlagInfoImpl;
 import com.vmware.vim.binding.vim.vm.ConfigSpec;
 import com.vmware.vim.binding.impl.vim.vm.ConfigSpecImpl;
 
 public class VcVmUtil {
    private static final Logger logger = Logger.getLogger(VcVmUtil.class);
 
    public static final String NIC_LABEL_PREFIX = "Network adapter ";
 
    /**
     * some callers are not very convenient to provide portGroups,
     * we provide this function to check if all NICs are ready,
     */
    public static boolean checkIpAddresses(final VcVirtualMachine vcVm) {
       try {
          Boolean ready = VcContext.inVcSessionDo(new VcSession<Boolean>() {
             @Override
             protected boolean isTaskSession() {
                return true;
             }
 
             @Override
             public Boolean body() throws Exception {
                GuestInfo guestInfo = vcVm.queryGuest();
                NicInfo[] nicInfos = guestInfo.getNet();
 
                if (nicInfos == null || nicInfos.length == 0) {
                   return false;
                }
 
                for (NicInfo nicInfo : nicInfos) {
                   if (nicInfo.getNetwork() == null || nicInfo.getIpConfig() == null
                         ||nicInfo.getIpConfig().getIpAddress() == null
                         || nicInfo.getIpConfig().getIpAddress().length == 0) {
                      return  false;
                   }
 
                   boolean foundIpV4Address = false;
                   for (IpAddress info : nicInfo.getIpConfig().getIpAddress()) {
                      if (info.getIpAddress() != null && IpAddressUtil.isIpV4(info.getIpAddress())) {
                         foundIpV4Address = true;
                         break;
                      }
                   }
                   if (!foundIpV4Address) {
                      return false;
                   }
                }
                return true;
             }
          });
          return ready;
       } catch (Exception e) {
          throw BddException.wrapIfNeeded(e, e.getLocalizedMessage());
       }
    }
 
    public static Set<String> getAllIpAddresses(final VcVirtualMachine vcVm,
          final Set<String> portGroups, boolean inSession) {
       Set<String> allIpAddresses = new HashSet<String>();
       for (String portGroup : portGroups) {
          allIpAddresses.add(getIpAddressOfPortGroup(vcVm, portGroup));
       }
       return allIpAddresses;
    }
 
    private static String getIpAddressOfPortGroupWithoutSession(final VcVirtualMachine vcVm,
          final String portGroup) throws Exception {
       GuestInfo guestInfo = vcVm.queryGuest();
       NicInfo[] nicInfos = guestInfo.getNet();
 
       /*
        * We do not know when VC can retrieve vm's guestinfo from vmtools, so it's better
        * to do enough validation to avoid Null Point Exception
        */
       if (nicInfos == null || nicInfos.length == 0) {
          return Constants.NULL_IP;
       }
 
       for (NicInfo nicInfo : nicInfos) {
          if (nicInfo.getNetwork() == null || !nicInfo.getNetwork().equals(portGroup)) {
             continue;
          }
 
          if (nicInfo.getIpConfig() == null || nicInfo.getIpConfig().getIpAddress() == null
                || nicInfo.getIpConfig().getIpAddress().length == 0) {
             continue;
          }
 
          for (IpAddress info : nicInfo.getIpConfig().getIpAddress()) {
             if (info.getIpAddress() != null && IpAddressUtil.isIpV4(info.getIpAddress())) {
                return info.getIpAddress();
             }
          }
       }
 
       return Constants.NULL_IP;
    }
 
    public static String getIpAddressOfPortGroup(final VcVirtualMachine vcVm,
          final String portGroup) {
       try {
          String ip = VcContext.inVcSessionDo(new VcSession<String>() {
             @Override
             protected boolean isTaskSession() {
                return true;
             }
             @Override
             public String body() throws Exception {
                return VcVmUtil.getIpAddressOfPortGroupWithoutSession(vcVm, portGroup);
             }
          });
          return ip;
       } catch (Exception e) {
          throw BddException.wrapIfNeeded(e, e.getLocalizedMessage());
       }
    }
 
    public static String getGuestHostName(final VcVirtualMachine vcVm,
          boolean inSession) {
       try {
          if (inSession) {
             return vcVm.queryGuest().getHostName();
          }
          String hostName = VcContext.inVcSessionDo(new VcSession<String>() {
             @Override
             protected boolean isTaskSession() {
                return true;
             }
 
             @Override
             public String body() throws Exception {
                GuestInfo guest = vcVm.queryGuest();
                return guest.getHostName();
             }
          });
          return hostName;
       } catch (Exception e) {
          throw BddException.wrapIfNeeded(e, e.getLocalizedMessage());
       }
    }
 
    public static boolean setBaseNodeForVm(BaseNode vNode, VcVirtualMachine vm) {
       boolean success = true;
       String vmName = vm.getName();
       vm = VcCache.getIgnoreMissing(vm.getId()); //reload vm in case vm is changed from vc
       if (vm == null) {
          logger.info("vm " + vmName
                + "is created, and then removed afterwards.");
       }
       if (vm != null) {
          for (String portGroup : vNode.fetchAllPortGroups()) {
             String ip = VcVmUtil.getIpAddressOfPortGroup(vm, portGroup);
             vNode.updateIpAddressOfPortGroup(portGroup, ip);
          }
       }
       if (vNode.ipsReady()) {
          vNode.setSuccess(true);
          vNode.setGuestHostName(VcVmUtil.getGuestHostName(vm, false));
          vNode.setTargetHost(vm.getHost().getName());
          vNode.setVmMobId(vm.getId());
          if (vm.isPoweredOn()) {
             vNode.setNodeStatus(NodeStatus.VM_READY);
             vNode.setNodeAction(null);
          } else {
             vNode.setNodeStatus(NodeStatus.POWERED_OFF);
             vNode.setNodeAction(Constants.NODE_ACTION_CLONING_FAILED);
          }
       } else {
          vNode.setSuccess(false);
          // in static ip case, vNode contains the allocated address,
          // here reset the value in case the ip is unavailable from vc
          vNode.resetIps();
          if (vm != null) {
             vNode.setVmMobId(vm.getId());
             if (vm.isPoweredOn()) {
                vNode.setNodeStatus(NodeStatus.POWERED_ON);
                vNode.setNodeAction(Constants.NODE_ACTION_GET_IP_FAILED);
             } else {
                vNode.setNodeStatus(NodeStatus.POWERED_OFF);
                vNode.setNodeAction(Constants.NODE_ACTION_CLONING_FAILED);
             }
          }
          success = false;
          logger.error("Failed to get ip address of VM " + vNode.getVmName());
       }
       if (success) {
          String haFlag = vNode.getNodeGroup().getHaFlag();
          if (haFlag != null
                && Constants.HA_FLAG_FT.equals(haFlag.toLowerCase())) {
             // ha is enabled, need to check if secondary VM is ready either
             logger.error("Failed to power on FT secondary VM for node "
                   + vm.getName() + ", " + "FT state " + vm.getFTState()
                   + " is unexpected.");
             return verifyFTState(vm);
          }
       }
       return success;
    }
 
    public static boolean verifyFTState(final VcVirtualMachine vm) {
       try {
          VcContext.inVcSessionDo(new VcSession<Void>() {
             @Override
             public Void body() throws Exception {
                vm.updateRuntime();
                return null;
             }
          });
       } catch (Exception e) {
          logger.error("Failed to update VM " + vm.getName()
                + " runtime information,"
                + " this may cause the FT state wrong.");
       }
       if (vm.getFTState() == null
             || vm.getFTState() != FaultToleranceState.running) {
          return false;
       }
       return true;
    }
 
    public static VirtualDisk findVirtualDisk(String vmMobId, String externalAddr) {
       VcVirtualMachine vm = VcCache.getIgnoreMissing(vmMobId);
 
       DeviceId diskId = new DeviceId(externalAddr);
       VirtualDevice device = vm.getVirtualDevice(diskId);
       if (device == null)
          return null;
 
       AuAssert.check(device instanceof VirtualDisk);
       return (VirtualDisk) device;
    }
 
    public static void populateDiskInfo(final DiskEntity diskEntity,
          final String vmMobId) {
       VcContext.inVcSessionDo(new VcSession<Void>() {
          @Override
          protected boolean isTaskSession() {
             return true;
          }
 
          @Override
          protected Void body() throws Exception {
             VirtualDisk vDisk =
                   findVirtualDisk(vmMobId, diskEntity.getExternalAddress());
             if (vDisk == null)
                return null;
 
             VirtualDisk.FlatVer2BackingInfo backing =
                   (VirtualDisk.FlatVer2BackingInfo) vDisk.getBacking();
             Datastore ds = MoUtil.getManagedObject(backing.getDatastore());
             diskEntity.setSizeInMB((int) (vDisk.getCapacityInKB() / 1024));
             diskEntity.setDatastoreName(ds.getName());
             diskEntity.setVmdkPath(backing.getFileName());
             diskEntity.setDatastoreMoId(MoUtil.morefToString(ds._getRef()));
             diskEntity.setDiskMode(backing.getDiskMode());
             diskEntity.setHardwareUUID(backing.getUuid());
             return null;
          }
       });
    }
 
    public static boolean isDatastoreAccessible(String dsMobId) {
       final VcDatastore ds = VcCache.getIgnoreMissing(dsMobId);
       try {
          VcContext.inVcSessionDo(new VcSession<Void>() {
             @Override
             protected boolean isTaskSession() {
                return true;
             }
 
             @Override
             protected Void body() throws Exception {
                ds.update();
                return null;
             }
          });
       } catch (Exception e) {
          logger.info("failed to update datastore " + ds.getName()
                + ", ignore this error.");
       }
       if (ds != null && ds.isAccessible())
          return true;
       return false;
    }
 
    public static void updateVm(String vmId) {
       final VcVirtualMachine vm = VcCache.getIgnoreMissing(vmId);
       try {
          VcContext.inVcSessionDo(new VcSession<Void>() {
             @Override
             protected boolean isTaskSession() {
                return true;
             }
 
             @Override
             protected Void body() throws Exception {
                vm.update();
                return null;
             }
          });
       } catch (Exception e) {
          logger.info("failed to update vm " + vm.getName()
                + ", ignore this error.");
       }
    }
 
    public static boolean configIOShares(final String vmId,
          final Priority ioShares) {
       final VcVirtualMachine vcVm = VcCache.getIgnoreMissing(vmId);
 
       if (vcVm == null) {
          logger.info("vm " + vmId + " is not found.");
          return false;
       }
       VcContext.inVcSessionDo(new VcSession<Void>() {
          @Override
          protected Void body() throws Exception {
             List<VirtualDeviceSpec> deviceSpecs =
                   new ArrayList<VirtualDeviceSpec>();
             for (DeviceId slot : vcVm.getVirtualDiskIds()) {
                SharesInfo shares = new SharesInfoImpl();
                shares.setLevel(Level.valueOf(ioShares.toString().toLowerCase()));
                IOAllocationInfo allocationInfo = new IOAllocationInfoImpl();
                allocationInfo.setShares(shares);
                VirtualDisk vmdk = (VirtualDisk) vcVm.getVirtualDevice(slot);
                vmdk.setStorageIOAllocation(allocationInfo);
                VirtualDeviceSpec spec = new VirtualDeviceSpecImpl();
                spec.setOperation(VirtualDeviceSpec.Operation.edit);
                spec.setDevice(vmdk);
                deviceSpecs.add(spec);
             }
             logger.info("reconfiguring disks in vm " + vmId
                   + " io share level to " + ioShares);
             vcVm.reconfigure(VmConfigUtil.createConfigSpec(deviceSpecs));
             logger.info("reconfigured disks in vm " + vmId
                   + " io share level to " + ioShares);
             return null;
          }
 
          protected boolean isTaskSession() {
             return true;
          }
       });
       return true;
    }
 
    public static boolean runSPOnSingleVM(NodeEntity node, Callable<Void> call) {
       boolean operationResult = true;
       if (node.getMoId() == null) {
          logger.info("VC vm does not exist for node: " + node.getVmName());
          return false;
       }
       VcVirtualMachine vcVm = VcCache.getIgnoreMissing(node.getMoId());
       if (vcVm == null) {
          // cannot find VM
          logger.info("VC vm does not exist for node: " + node.getVmName());
          return false;
       }
       @SuppressWarnings("unchecked")
       Callable<Void>[] storeProceduresArray = new Callable[1];
       storeProceduresArray[0] = call;
       NoProgressUpdateCallback callback = new NoProgressUpdateCallback();
       try {
          ExecutionResult[] result =
                Scheduler
                      .executeStoredProcedures(
                            com.vmware.aurora.composition.concurrent.Priority.BACKGROUND,
                            storeProceduresArray, callback);
          if (result == null) {
             logger.error("No result from composition layer");
             return false;
          } else {
             if (result[0].finished && result[0].throwable == null) {
                operationResult = true;
                logger.info("successfully run operation on vm for node: "
                      + node.getVmName());
             } else {
                operationResult = false;
                logger.error(
                      "failed in run operation on vm for node: "
                            + node.getVmName(), result[0].throwable);
             }
          }
       } catch (Exception e) {
          operationResult = false;
          logger.error("error in run operation on vm.", e);
       }
       return operationResult;
    }
 
    // get the parent vc resource pool of the node
    public static VcResourcePool getTargetRp(String clusterName,
          String groupName, NodeEntity node) {
       String clusterRpName = ConfigInfo.getSerengetiUUID() + "-" + clusterName;
 
       VcResourcePoolEntity rpEntity = node.getVcRp();
       String vcRPName = "";
 
       try {
          VcCluster cluster =
                VcResourceUtils.findVcCluster(rpEntity.getVcCluster());
          if (!cluster.getConfig().getDRSEnabled()) {
             logger.debug("DRS disabled for cluster " + rpEntity.getVcCluster()
                   + ", put VM under cluster directly.");
             return cluster.getRootRP();
          }
          if (CommonUtil.isBlank(rpEntity.getVcResourcePool())) {
             vcRPName = clusterRpName + "/" + groupName;
          } else {
             vcRPName =
                   rpEntity.getVcResourcePool() + "/" + clusterRpName + "/"
                         + groupName;
          }
          VcResourcePool rp =
                VcResourceUtils.findRPInVCCluster(rpEntity.getVcCluster(),
                      vcRPName);
          if (rp == null) {
             throw ClusteringServiceException.TARGET_VC_RP_NOT_FOUND(
                   rpEntity.getVcCluster(), vcRPName);
          }
          return rp;
       } catch (Exception e) {
          logger.error("Failed to get VC resource pool " + vcRPName
                + " in vc cluster " + rpEntity.getVcCluster(), e);
 
          throw ClusteringServiceException.TARGET_VC_RP_NOT_FOUND(
                rpEntity.getVcCluster(), vcRPName);
       }
    }
 
    public static VmSchema getVmSchema(ClusterCreate spec, String nodeGroup,
          List<DiskSpec> diskSet, String templateVmId, String templateVmSnapId) {
       NodeGroupCreate groupSpec = spec.getNodeGroup(nodeGroup);
 
       VmSchema schema = new VmSchema();
 
       // prepare resource schema
       ResourceSchema resourceSchema = new ResourceSchema();
       resourceSchema.name = "Resource Schema";
       resourceSchema.cpuReservationMHz = 0;
       resourceSchema.memReservationSize = 0;
       resourceSchema.numCPUs =
             (groupSpec.getCpuNum() == null) ? 0 : groupSpec.getCpuNum();
       resourceSchema.memSize =
             (groupSpec.getMemCapacityMB() == null) ? 0 : groupSpec
                   .getMemCapacityMB();
       resourceSchema.priority =
             com.vmware.aurora.interfaces.model.IDatabaseConfig.Priority.Normal;
       schema.resourceSchema = resourceSchema;
 
       // prepare disk schema
       DiskSchema diskSchema = new DiskSchema();
       ArrayList<Disk> disks = new ArrayList<Disk>(diskSet.size());
       for (DiskSpec disk : diskSet) {
          Disk tmDisk = new Disk();
          tmDisk.name = disk.getName();
          tmDisk.type = disk.getDiskType().getType();
          tmDisk.initialSizeMB = disk.getSize() * 1024;
          if (disk.getAllocType() != null && !disk.getAllocType().isEmpty())
             tmDisk.allocationType =
                   AllocationType.valueOf(disk.getAllocType().toUpperCase());
          else
             tmDisk.allocationType = null;
          tmDisk.datastore = disk.getTargetDs();
          tmDisk.externalAddress = disk.getExternalAddress();
          tmDisk.vmdkPath = disk.getVmdkPath();
          tmDisk.mode = DiskMode.valueOf(disk.getDiskMode());
          disks.add(tmDisk);
       }
       diskSchema.setParent(templateVmId);
       diskSchema.setParentSnap(templateVmSnapId);
       diskSchema.setDisks(disks);
       schema.diskSchema = diskSchema;
 
       // prepare network schema
 
       ArrayList<Network> networks = new ArrayList<Network>();
       List<NetworkAdd> networkAdds = spec.getNetworkings();
       int labelIndex = 1;
       for (NetworkAdd networkAdd : networkAdds) {
          Network network = new Network();
          network.vcNetwork = networkAdd.getPortGroup();
          network.nicLabel = NIC_LABEL_PREFIX + labelIndex;
          labelIndex++;
          networks.add(network);
       }
 
       NetworkSchema networkSchema = new NetworkSchema();
       networkSchema.name = "Network Schema";
       networkSchema.networks = networks;
       schema.networkSchema = networkSchema;
 
       return schema;
    }
 
    public static long makeVmMemoryDivisibleBy4(long memory) {
       return CommonUtil.makeVmMemoryDivisibleBy4(memory);
    }
 
    public static void addBootupUUID(Map<String, String> bootupConfigs) {
       AuAssert.check(bootupConfigs != null);
       bootupConfigs.put(Constants.GUEST_VARIABLE_BOOTUP_UUID, UUID.randomUUID()
             .toString());
    }
 
    public static void destroyVm(final String vmId) throws Exception {
       VcVirtualMachine vm = VcCache.getIgnoreMissing(vmId);
 
       FaultToleranceConfigInfo info = vm.getConfig().getFtInfo();
       if (info != null && info.getRole() == 1) {
          logger.info("VM " + vm.getName()
                + " is FT primary VM, disable FT before delete it.");
          vm.turnOffFT();
       }
       // try guest shut down first, wait for 3 minutes, power it off after time out
       if (vm.isPoweredOn()
             && !vm.shutdownGuest(Constants.VM_FAST_SHUTDOWN_WAITING_SEC * 1000)) {
          vm.powerOff();
       }
 
       /*
        * TRICK: destroy vm with unaccessible disks will throw exceptions, ignore 
        * it and destroy it again.
        */
       try {
          vm.destroy(false);
       } catch (Exception e) {
          logger.warn("failed to delete vm " + vm.getName() + " as "
                + e.getMessage());
          logger.info("try to unregister it again");
          vm.unregister();
       }
       logger.info("VM " + vm.getName() + " deleted");
    }
 
    public static void destroyVm(final String vmId, Boolean inSession)
          throws Exception {
       if (inSession) {
          destroyVm(vmId);
       } else {
          VcContext.inVcSessionDo(new VcSession<Void>() {
             @Override
             protected Void body() throws Exception {
                destroyVm(vmId);
                return null;
             }
 
             @Override
             protected boolean isTaskSession() {
                return true;
             }
          });
       }
    }
 
    public static VcVirtualMachine findVmInRp(final VcResourcePool rp,
          final String vmName) {
       return VcContext.inVcSessionDo(new VcSession<VcVirtualMachine>() {
          @Override
          protected VcVirtualMachine body() throws Exception {
             VcVirtualMachine targetVm = null;
             for (VcVirtualMachine vm : rp.getChildVMs()) {
                if (vm.getName().equals(vmName)) {
                   targetVm = vm;
                   break;
                }
             }
 
             return targetVm;
          }
 
          @Override
          protected boolean isTaskSession() {
             return true;
          }
       });
    }
 
    public static void rename(final String vmId, final String newName) {
       VcContext.inVcSessionDo(new VcSession<Void>() {
          @Override
          protected Void body() throws Exception {
             VcVirtualMachine vm = VcCache.getIgnoreMissing(vmId);
             vm.rename(newName);
 
             return null;
          }
 
          @Override
          protected boolean isTaskSession() {
             return true;
          }
       });
    }
 
    public static void enableFt(final VcVirtualMachine vm) throws Exception {
       try {
          VcContext.inVcSessionDo(new VcSession<Void>() {
             @Override
             protected Void body() throws Exception {
                // cdrom is not supported in FT mode
                vm.detachAllCdroms();
                // do not change disk mode to persistent here,
                // instead set it to persistent before vm cloning
                vm.turnOnFT(null);
                return null;
             }
 
             protected boolean isTaskSession() {
                return true;
             }
          });
       } catch (Exception e) {
          throw ClusteringServiceException.ENABLE_FT_FAILED(e, vm.getName());
       }
    }
 
    public static void disableHa(final VcVirtualMachine vm) throws Exception {
       VcCluster cluster = vm.getResourcePool().getVcCluster();
       boolean clusterHa = cluster.getConfig().getHAEnabled();
       if (!clusterHa) {
          // cluster is not ha enabled, don't need to disable ha again
          return;
       }
       try {
          VcContext.inVcSessionDo(new VcSession<Void>() {
             @Override
             protected Void body() throws Exception {
                vm.modifyHASettings(RestartPriority.disabled, null, null);
                return null;
             }
 
             protected boolean isTaskSession() {
                return true;
             }
          });
       } catch (Exception e) {
          throw ClusteringServiceException.DISABLE_HA_FAILED(e, vm.getName());
       }
    }
 
    public static void enableDiskUUID(final VcVirtualMachine vm) throws Exception {
       try {
          VcContext.inVcSessionDo(new VcSession<Void>() {
             @Override
             protected Void body() throws Exception {
                FlagInfo flagInfo = new FlagInfoImpl();
                flagInfo.setDiskUuidEnabled(true);
                ConfigSpec configSpec = new ConfigSpecImpl();
                configSpec.setFlags(flagInfo);
                vm.reconfigure(configSpec);
                return null;
             }
 
             protected boolean isTaskSession() {
                return true;
             }
          });
       } catch (Exception e) {
          throw ClusteringServiceException.ENABLE_DISK_UUID_FAILED(e, vm.getName());
       }
    }
 
    /**
     * check and set vApp Options -> vmware tools flag
     * @param vm
     * @return true if vm reconfigured, in this case need remove snapshots
     */
    public static boolean enableOvfEnvTransport(final VcVirtualMachine vm) {
       return VcContext.inVcSessionDo(new VcSession<Boolean>() {
          @Override
          protected Boolean body() throws Exception {
             String vmToolEnableItem = "com.vmware.guestInfo";
 
             if (vm.getConfig() == null || vm.getConfig().getVAppConfig() == null
                   || vm.getConfig().getVAppConfig().getOvfEnvironmentTransport() == null
                   || !Arrays.asList(vm.getConfig().getVAppConfig().getOvfEnvironmentTransport()).contains(vmToolEnableItem)) {
                String[] ovfEnvTransport = new String[] {vmToolEnableItem};
                VmConfigSpec vmConfigSpec = new VmConfigSpecImpl();
                vmConfigSpec.setOvfEnvironmentTransport(ovfEnvTransport);
                ConfigSpec configSpec = new ConfigSpecImpl();
                configSpec.setVAppConfig(vmConfigSpec);
                vm.reconfigure(configSpec);
                logger.info("configured ovf env transport");
                return true;
             }
             return false;
          }
 
          protected boolean isTaskSession() {
             return true;
          }
       });
    }
 
    /**
     * check if number of vCPUs is a multiple of the number of cores per socket configured in the VM
     * @param vmId
     * @param cpuNum
     * @return
     */
    public static boolean validateCPU(final String vmId, final int cpuNum) {
      if (cpuNum < 0) {
          return false;
       }
 
       VcVirtualMachine vm = VcCache.getIgnoreMissing(vmId);
       if (vm == null) {
          logger.error("cannot found vm of id: " + vmId);
          return false;
       }
 
       String coresPerSocketKey = "cpuid.coresPerSocket";
       if (vm.getConfig() != null && vm.getConfig().getExtraConfig() != null) {
          for (OptionValue optionValue : vm.getConfig().getExtraConfig()) {
             if (coresPerSocketKey.equals(optionValue.getKey())) {
                int coresPerSocket = Integer.parseInt((String) optionValue.getValue());
                if (cpuNum % coresPerSocket == 0) {
                   return true;
                }
                return false;
             }
          }
       }
       return true;
    }
 
    public static boolean enableSyncTimeWithHost(final VcVirtualMachine vm) {
       return VcContext.inVcSessionDo(new VcSession<Boolean>() {
          @Override
          protected Boolean body() throws Exception {
             if (vm.getConfig() == null || vm.getConfig().getTools() == null
                   || vm.getConfig().getTools().getSyncTimeWithHost() == null
                   || !vm.getConfig().getTools().getSyncTimeWithHost()) {
                ToolsConfigInfo toolsConfigInfo = new ToolsConfigInfoImpl();
                toolsConfigInfo.setSyncTimeWithHost(true);
                ConfigSpec configSpec = new ConfigSpecImpl();
                configSpec.setTools(toolsConfigInfo);
                vm.reconfigure(configSpec);
                logger.info("enabled sync time with host");
                return true;
             }
             return false;
          }
 
          protected boolean isTaskSession() {
             return true;
          }
       });
    }
 
    public static void checkAndCreateSnapshot(final VmSchema vmSchema) {
       VcContext.inVcSessionDo(new VcSession<Void>() {
          @Override
          protected boolean isTaskSession() {
             return true;
          }
 
          @Override
          protected Void body() throws Exception {
             final VcVirtualMachine template =
                   VcCache.get(vmSchema.diskSchema.getParent());
             VcSnapshot snap =
                   template.getSnapshotByName(vmSchema.diskSchema
                         .getParentSnap());
             if (snap == null) {
                // this is a blocking call
                snap =
                      template.createSnapshot(
                            vmSchema.diskSchema.getParentSnap(),
                            "Serengeti template Root Snapshot");
             }
             return null;
          }
       });
    }
 }
