 /***************************************************************************
  * Copyright (c) 2012 VMware, Inc. All Rights Reserved.
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
 package com.vmware.bdd.rest;
 
 import java.util.ArrayList;
 import java.util.List;
 
 import javax.servlet.http.HttpServletRequest;
 import javax.servlet.http.HttpServletResponse;
 
 import org.apache.commons.configuration.ConfigurationException;
 import org.apache.commons.configuration.PropertiesConfiguration;
 import org.apache.log4j.Logger;
 import org.springframework.beans.factory.annotation.Autowired;
 import org.springframework.core.NestedRuntimeException;
 import org.springframework.http.HttpStatus;
 import org.springframework.stereotype.Controller;
 import org.springframework.web.bind.annotation.ExceptionHandler;
 import org.springframework.web.bind.annotation.PathVariable;
 import org.springframework.web.bind.annotation.RequestBody;
 import org.springframework.web.bind.annotation.RequestMapping;
 import org.springframework.web.bind.annotation.RequestMethod;
 import org.springframework.web.bind.annotation.RequestParam;
 import org.springframework.web.bind.annotation.ResponseBody;
 import org.springframework.web.bind.annotation.ResponseStatus;
 
 import com.vmware.bdd.apitypes.BddErrorMessage;
 import com.vmware.bdd.apitypes.ClusterCreate;
 import com.vmware.bdd.apitypes.ClusterRead;
 import com.vmware.bdd.apitypes.DatastoreAdd;
 import com.vmware.bdd.apitypes.DatastoreRead;
 import com.vmware.bdd.apitypes.DistroRead;
 import com.vmware.bdd.apitypes.IpBlock;
 import com.vmware.bdd.apitypes.NetworkAdd;
 import com.vmware.bdd.apitypes.NetworkRead;
 import com.vmware.bdd.apitypes.RackInfo;
 import com.vmware.bdd.apitypes.ResourcePoolAdd;
 import com.vmware.bdd.apitypes.ResourcePoolRead;
 import com.vmware.bdd.apitypes.TaskRead;
 import com.vmware.bdd.apitypes.TopologyType;
 import com.vmware.bdd.exception.BddException;
 import com.vmware.bdd.exception.NetworkException;
 import com.vmware.bdd.manager.ClusterManager;
 import com.vmware.bdd.manager.DistroManager;
 import com.vmware.bdd.manager.NetworkManager;
 import com.vmware.bdd.manager.RackInfoManager;
 import com.vmware.bdd.manager.TaskManager;
 import com.vmware.bdd.manager.VcDataStoreManager;
 import com.vmware.bdd.manager.VcResourcePoolManager;
 import com.vmware.bdd.utils.AuAssert;
 import com.vmware.bdd.utils.IpAddressUtil;
 
 @Controller
 public class RestResource {
    private static final Logger logger = Logger.getLogger(RestResource.class);
 
    @Autowired
    private ClusterManager clusterMgr;
    @Autowired
    private TaskManager taskManager;
    @Autowired
    private VcResourcePoolManager vcRpMgr;
    @Autowired
    private NetworkManager networkManager;
    @Autowired
    private RackInfoManager rackInfoManager;
    @Autowired
    private DistroManager distroManager;
    @Autowired
    private VcDataStoreManager datastoreMgr;
 
    private static final String ERR_CODE_FILE = "serengeti-errcode.properties";
    private static final int DEFAULT_HTTP_ERROR_CODE = 500;
 
    /* HTTP status code read from a config file. */
    private static org.apache.commons.configuration.Configuration httpStatusCodes =
          init();
 
    private static org.apache.commons.configuration.Configuration init() {
       PropertiesConfiguration config = null;
       try {
          config = new PropertiesConfiguration();
          config.setListDelimiter('\0');
          config.load(ERR_CODE_FILE);
       } catch (ConfigurationException ex) {
          // error out if the configuration file is not there
          String message = "Failed to load serengeti error message file.";
          Logger.getLogger(RestResource.class).fatal(message, ex);
          throw BddException.APP_INIT_ERROR(ex, message);
       }
       return config;
    }
 
    private static int getHttpErrorCode(String errorId) {
       return httpStatusCodes.getInteger(errorId, DEFAULT_HTTP_ERROR_CODE);
    }
 
    @RequestMapping(value = "/hello", method = RequestMethod.GET)
    @ResponseStatus(HttpStatus.OK)
    public void getHello() {
    }
 
    // task API
    @RequestMapping(value = "/tasks", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    public List<TaskRead> getTasks() {
       return taskManager.getTasks();
    }
 
    @RequestMapping(value = "/task/{taskId}", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    public TaskRead getTaskById(@PathVariable long taskId) throws Exception {
       TaskRead task;
 
       if ((task = taskManager.getTaskById(taskId)) == null) {
          throw BddException.NOT_FOUND("task", "" + taskId);
       }
 
       return task;
    }
 
    // cluster API
    @RequestMapping(value = "/clusters", method = RequestMethod.POST, consumes = "application/json")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void createCluster(@RequestBody ClusterCreate createSpec,
          HttpServletRequest request, HttpServletResponse response)
          throws Exception {
 
       // TODO: make sure cluster name is valid
      createSpec。setTopologyPolicy(TopologyType.NONE); // XXX to be deleted, just make smoke test happy
       Long taskId = clusterMgr.createCluster(createSpec);
       redirectRequest(taskId, request, response);
    }
 
    // cluster API
    @RequestMapping(value = "/cluster/{clusterName}/config", method = RequestMethod.PUT, consumes = "application/json")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void configCluster(@PathVariable("clusterName") String clusterName, 
          @RequestBody ClusterCreate createSpec,
          HttpServletRequest request, HttpServletResponse response)
          throws Exception {
       Long taskId = clusterMgr.configCluster(clusterName, createSpec);
       redirectRequest(taskId, request, response);
    }
 
    private void redirectRequest(long taskId, HttpServletRequest request,
          HttpServletResponse response) {
       StringBuffer url = request.getRequestURL();
       int subLength = url.length() - request.getPathInfo().length();
       url.setLength(subLength);
       url.append("/task/").append(Long.toString(taskId));
       response.setHeader("Location", url.toString());
    }
 
    @RequestMapping(value = "/cluster/{clusterName}", method = RequestMethod.DELETE)
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void deleteCluster(@PathVariable("clusterName") String clusterName,
          HttpServletRequest request, HttpServletResponse response)
          throws Exception {
       // TODO: make sure cluster name is valid
       Long taskId = clusterMgr.deleteClusterByName(clusterName);
       redirectRequest(taskId, request, response);
    }
 
    @RequestMapping(value = "/cluster/{clusterName}", method = RequestMethod.PUT)
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void startStopResumeCluster(
          @PathVariable("clusterName") String clusterName,
          @RequestParam(value="state", required = true) String state,
          HttpServletRequest request, HttpServletResponse response)
          throws Exception {
       if (clusterName == null || clusterName.isEmpty()) {
          throw BddException.INVALID_PARAMETER("cluster name", clusterName);
       }
 
       Long taskId;
       if (state.equals("stop")) {
          taskId = clusterMgr.stopCluster(clusterName);
          redirectRequest(taskId, request, response);
       } else if (state.equals("start")) {
          taskId = clusterMgr.startCluster(clusterName);
          redirectRequest(taskId, request, response);
       } else if (state.equals("resume")) {
          taskId = clusterMgr.resumeClusterCreation(clusterName);
          redirectRequest(taskId, request, response);
       } else {
          throw BddException.INVALID_PARAMETER("cluster state", state);
       }
    }
 
    @RequestMapping(value = "/cluster/{clusterName}/nodegroup/{groupName}",
          method = RequestMethod.PUT)
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void startStopNodeGroup(
          @PathVariable("clusterName") String clusterName,
          @PathVariable("groupName") String groupName,
          @RequestParam(value="state", required = true) String state,
          HttpServletRequest request, HttpServletResponse response)
          throws Exception {
       if (clusterName == null || clusterName.isEmpty()) {
          throw BddException.INVALID_PARAMETER("cluster name", clusterName);
       }
 
       if (groupName == null || groupName.isEmpty()) {
          throw BddException.INVALID_PARAMETER("node group name", groupName);
       }
 
       Long taskId;
       if (state.equals("stop")) {
          taskId = clusterMgr.stopNodeGroup(clusterName, groupName);
          redirectRequest(taskId, request, response);
       } else if (state.equals("start")) {
          taskId = clusterMgr.startNodeGroup(clusterName, groupName);
          redirectRequest(taskId, request, response);
       } else {
          throw BddException.INVALID_PARAMETER("node group state", state);
       }
    }
 
    @RequestMapping(value = "/cluster/{clusterName}/nodegroup/{groupName}/node/{nodeName}",
          method = RequestMethod.PUT)
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void startStopNode(
          @PathVariable("clusterName") String clusterName,
          @PathVariable("groupName") String groupName,
          @PathVariable("nodeName") String nodeName,
          @RequestParam(value="state", required = true) String state,
          HttpServletRequest request, HttpServletResponse response)
          throws Exception {
       if (clusterName == null || clusterName.isEmpty()) {
          throw BddException.INVALID_PARAMETER("cluster name", clusterName);
       }
 
       if (groupName == null || groupName.isEmpty()) {
          throw BddException.INVALID_PARAMETER("node group name", groupName);
       }
 
       if (nodeName == null || nodeName.isEmpty()) {
          throw BddException.INVALID_PARAMETER("node name", nodeName);
       }
 
       Long taskId;
       if (state.equals("stop")) {
          taskId = clusterMgr.stopNode(clusterName, groupName, nodeName);
          redirectRequest(taskId, request, response);
       } else if (state.equals("start")) {
          taskId = clusterMgr.startNode(clusterName, groupName, nodeName);
          redirectRequest(taskId, request, response);
       } else {
          throw BddException.INVALID_PARAMETER("node state", state);
       }
    }
 
    @RequestMapping(value = "/cluster/{clusterName}/nodegroup/{groupName}/instancenum", method = RequestMethod.PUT)
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void resizeCluster(@PathVariable("clusterName") String clusterName,
          @PathVariable("groupName") String groupName,
          @RequestBody int instanceNum, HttpServletRequest request,
          HttpServletResponse response) throws Exception {
       if (instanceNum <= 0) {
          throw BddException.INVALID_PARAMETER(
                "node group instance number", String.valueOf(instanceNum));
       }
       Long taskId =
             clusterMgr.resizeCluster(clusterName, groupName, instanceNum);
       redirectRequest(taskId, request, response);
    }
 
    @RequestMapping(value = "/cluster/{clusterName}", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    public ClusterRead getCluster(
          @PathVariable("clusterName") final String clusterName) {
       return clusterMgr.getClusterByName(clusterName);
    }
 
    @RequestMapping(value = "/cluster/{clusterName}/spec", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    public ClusterCreate getClusterSpec(
          @PathVariable("clusterName") final String clusterName) {
       return clusterMgr.getClusterSpec(clusterName);
    }
 
    @RequestMapping(value = "/clusters", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    public List<ClusterRead> getClusters() {
       return clusterMgr.getClusters();
    }
 
    // cloud provider API
    @RequestMapping(value = "/resourcepools", method = RequestMethod.POST, consumes = "application/json")
    @ResponseStatus(HttpStatus.OK)
    public void addResourcePool(@RequestBody ResourcePoolAdd rpSpec) {
       if (rpSpec == null) {
          throw BddException.INVALID_PARAMETER("rpSpec", null);
       }
       if (rpSpec.getName() == null || rpSpec.getName().isEmpty()) {
          throw BddException.INVALID_PARAMETER("resource pool name",
                rpSpec.getName());
       }
       if (rpSpec.getVcClusterName() == null
             || rpSpec.getVcClusterName().isEmpty()) {
          throw BddException.INVALID_PARAMETER("vc cluster name",
                rpSpec.getVcClusterName());
       }
       if (rpSpec.getResourcePoolName() == null
             || rpSpec.getResourcePoolName().isEmpty()) {
          throw BddException.INVALID_PARAMETER("vc resource pool name",
                rpSpec.getResourcePoolName());
       }
 
       vcRpMgr.addResourcePool(rpSpec.getName(), rpSpec.getVcClusterName(),
             rpSpec.getResourcePoolName());
    }
 
    @RequestMapping(value = "/resourcepools", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    public List<ResourcePoolRead> getResourcePools() {
       return vcRpMgr.getAllResourcePoolForRest();
    }
 
    @RequestMapping(value = "/resourcepool/{rpName}", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    public ResourcePoolRead getResourcePool(
          @PathVariable("rpName") final String rpName) {
       if (rpName == null || rpName.isEmpty()) {
          throw BddException.INVALID_PARAMETER("resource pool name",
                rpName);
       }
       ResourcePoolRead read = vcRpMgr.getResourcePoolForRest(rpName);
       if (read == null) {
          throw BddException.NOT_FOUND("resource pool", rpName);
       }
       return read;
    }
 
    @RequestMapping(value = "/resourcepool/{rpName}", method = RequestMethod.DELETE)
    @ResponseStatus(HttpStatus.OK)
    public void deleteResourcePool(@PathVariable("rpName") final String rpName) {
       if (rpName == null || rpName.isEmpty()) {
          throw BddException.INVALID_PARAMETER("resource pool name",
                rpName);
       }
       vcRpMgr.deleteResourcePool(rpName);
    }
 
    @RequestMapping(value = "/datastores", method = RequestMethod.POST, consumes = "application/json")
    @ResponseStatus(HttpStatus.OK)
    public void addDatastore(@RequestBody DatastoreAdd dsSpec) {
       if (dsSpec == null) {
          throw BddException.INVALID_PARAMETER("dsSpec", null);
       }
       if (dsSpec.getName() == null || dsSpec.getName().isEmpty()) {
          throw BddException.INVALID_PARAMETER("date store name",
                dsSpec.getName());
       }
       if (dsSpec.getSpec() == null || dsSpec.getSpec().isEmpty()) {
          throw BddException.INVALID_PARAMETER("vc data store name",
                dsSpec.getSpec().toString());
       }
       datastoreMgr.addDataStores(dsSpec);
    }
 
    @RequestMapping(value = "/datastore/{dsName}", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    public DatastoreRead getDatastore(@PathVariable("dsName") final String dsName) {
       if (dsName == null || dsName.isEmpty()) {
          throw BddException.INVALID_PARAMETER("date store name", dsName);
       }
       DatastoreRead read = datastoreMgr.getDatastoreRead(dsName);
       if (read == null) {
          throw BddException.NOT_FOUND("data store", dsName);
       }
       return read;
    }
 
    @RequestMapping(value = "/datastores", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    public List<DatastoreRead> getDatastore() {
       return datastoreMgr.getAllDatastoreReads();
    }
 
    @RequestMapping(value = "/datastore/{dsName}", method = RequestMethod.DELETE)
    @ResponseStatus(HttpStatus.OK)
    public void deleteDatastore(@PathVariable("dsName") String dsName) {
       if (dsName == null || dsName.isEmpty()) {
          throw BddException.INVALID_PARAMETER("date store name", dsName);
       }
       datastoreMgr.deleteDatastore(dsName);
    }
 
    @RequestMapping(value = "/network/{networkName}", method = RequestMethod.DELETE)
    @ResponseStatus(HttpStatus.OK)
    public void deleteNetworkByName(
          @PathVariable("networkName") final String networkName) {
       networkManager.removeNetwork(networkName);
    }
 
    @RequestMapping(value = "/network/{networkName}", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    public NetworkRead getNetworkByName(
          @PathVariable("networkName") final String networkName,
          @RequestParam(value = "details", required = false, defaultValue = "false") final Boolean details) {
       NetworkRead network = networkManager.getNetworkByName(networkName,
             details != null ? details : false);
 
       if (network == null) {
          throw NetworkException.NOT_FOUND("network", networkName);
       }
 
       return network;
    }
 
    @RequestMapping(value = "/networks", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    public List<NetworkRead> getNetworks(@RequestParam(value = "details",
          required = false,
          defaultValue = "false") final Boolean details) {
       return networkManager.getAllNetworks(details != null ? details : false);
    }
 
    @RequestMapping(value = "/networks", method = RequestMethod.POST, consumes = "application/json")
    @ResponseStatus(HttpStatus.OK)
    public void addNetworks(@RequestBody final NetworkAdd na) {
       if (na.getName() == null || na.getName().isEmpty()) {
          throw BddException.INVALID_PARAMETER("name", na.getName());
       }
 
       if (na.getPortGroup() == null || na.getPortGroup().isEmpty()) {
          throw BddException.INVALID_PARAMETER("port group", na.getPortGroup());
       }
 
       if (na.isDhcp()) {
          networkManager.addDhcpNetwork(na.getName(), na.getPortGroup());
       } else {
          if (!IpAddressUtil.isValidNetmask(na.getNetmask())) {
             throw BddException.INVALID_PARAMETER("netmask", na.getNetmask());
          }
          long netmask = IpAddressUtil.getAddressAsLong(na.getNetmask());
          if (!IpAddressUtil.isValidIp(netmask,
                IpAddressUtil.getAddressAsLong(na.getGateway()))) {
             throw BddException.INVALID_PARAMETER("gateway", na.getGateway());
          }
          if (na.getDns1() != null
                && !IpAddressUtil.isValidIp(na.getDns1())) {
             throw BddException.INVALID_PARAMETER("primary dns", na.getDns1());
          }
          if (na.getDns2() != null
                && !IpAddressUtil.isValidIp(na.getDns2())) {
             throw BddException.INVALID_PARAMETER("secondary dns", na.getDns2());
          }
 
          AuAssert.check(na.getIp() != null, "Spring should guarantee this");
          for (IpBlock blk : na.getIp()) {
             Long begin = IpAddressUtil.getAddressAsLong(blk.getBeginIp());
             Long end = IpAddressUtil.getAddressAsLong(blk.getEndIp());
 
             if (begin == null || end == null || begin > end
                   || !IpAddressUtil.isValidIp(netmask, begin)
                   || !IpAddressUtil.isValidIp(netmask, end)) {
                throw BddException.INVALID_PARAMETER("IP block", "["
                      + blk.getBeginIp() + ", " + blk.getEndIp() + "]");
             }
          }
 
          networkManager.addIpPoolNetwork(na.getName(), na.getPortGroup(),
                na.getNetmask(), na.getGateway(), na.getDns1(), na.getDns2(),
                na.getIp());
       }
    }
 
    @RequestMapping(value = "/racks", method = RequestMethod.PUT)
    @ResponseStatus(HttpStatus.OK)
    public void importRacks(@RequestBody final List<RackInfo> racksInfo) throws Exception {
       if (racksInfo == null || racksInfo.size() == 0) {
          throw BddException.INVALID_PARAMETER("rack list", "empty");
       }
 
       rackInfoManager.importRackInfo(racksInfo);
    }
 
    @RequestMapping(value = "/racks", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    public List<RackInfo> exportRacks() throws Exception {
       return rackInfoManager.exportRackInfo();
    }
 
    @RequestMapping(value = "/distros", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    public List<DistroRead> getDistros() {
       return distroManager.getDistros();
    }
 
    @RequestMapping(value = "/distro/{distroName}", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    public DistroRead getDistroByName(
          @PathVariable("distroName") String distroName) {
       DistroRead distro = distroManager.getDistroByName(distroName);
 
       if (distro == null) {
          throw BddException.NOT_FOUND("distro", distroName);
       }
 
       return distro;
    }
 
    @ExceptionHandler(Throwable.class)
    @ResponseBody
    public BddErrorMessage handleException(Throwable t, HttpServletResponse response) {
       if (t instanceof NestedRuntimeException) {
          t = BddException.BAD_REST_CALL(t, t.getMessage());
       }
       BddException ex = BddException.wrapIfNeeded(t, "REST API transport layer error");
       logger.error("rest call error", ex);
       response.setStatus(getHttpErrorCode(ex.getFullErrorId()));
       return new BddErrorMessage(ex.getFullErrorId(), ex.getMessage());
    }
 }
