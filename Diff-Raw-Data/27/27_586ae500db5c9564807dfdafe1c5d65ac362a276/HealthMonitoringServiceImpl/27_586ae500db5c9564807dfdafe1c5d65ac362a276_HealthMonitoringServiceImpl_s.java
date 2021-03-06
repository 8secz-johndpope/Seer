 /*
  * Copyright (c) 2012 the original author or authors.
  *
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
  */
 package org.openinfinity.cloud.service.healthmonitoring;
 
 import java.util.ArrayList;
 import java.util.Collection;
 import java.util.Collections;
 import java.util.Comparator;
 import java.util.Date;
 import java.util.HashMap;
import java.util.Iterator;
 import java.util.List;
 import java.util.Map;
 import java.util.Set;
 import org.apache.commons.lang.ArrayUtils;
 import org.apache.commons.lang.StringUtils;
 import org.apache.http.client.HttpClient;
 import org.codehaus.jackson.map.ObjectMapper;
 import org.openinfinity.cloud.domain.AbstractResponse;
 import org.openinfinity.cloud.domain.Cluster;
 import org.openinfinity.cloud.domain.GroupListResponse;
 import org.openinfinity.cloud.domain.HealthStatusResponse;
 import org.openinfinity.cloud.domain.Machine;
 import org.openinfinity.cloud.domain.MetricBoundariesResponse;
 import org.openinfinity.cloud.domain.MetricNamesResponse;
 import org.openinfinity.cloud.domain.MetricTypesResponse;
 import org.openinfinity.cloud.domain.Node;
 import org.openinfinity.cloud.domain.Notification;
 import org.openinfinity.cloud.domain.NodeListResponse;
 import org.openinfinity.cloud.domain.NotificationResponse;
 import org.openinfinity.cloud.service.administrator.ClusterService;
 import org.openinfinity.cloud.service.administrator.MachineService;
 import org.openinfinity.cloud.service.healthmonitoring.HealthMonitoringServiceImpl;
 import org.openinfinity.cloud.service.healthmonitoring.Request;
 import org.openinfinity.cloud.service.healthmonitoring.RequestBuilder;
 import org.openinfinity.cloud.util.http.HttpHelper;
 import org.slf4j.Logger;
 import org.slf4j.LoggerFactory;
 import org.springframework.beans.factory.annotation.Autowired;
 import org.springframework.beans.factory.annotation.Value;
 import org.springframework.stereotype.Service;
 
 /**
  * 
  * @author Ivan Bilechyk
  * @author Ilkka Leinonen
  * @author Timo Saarinen
  * @author Nishant Gupta
  */
 @Service("healthMonitoringService")
 public class HealthMonitoringServiceImpl implements HealthMonitoringService {
 
 	@Autowired
 	private ClusterService clusterService;
 	
 	@Autowired
 	private MachineService machineService;
 
     @Autowired
     private HttpClient client;
 
     @Value("${connTimeout}")
 	private String connTimeout = "1000";
     
     private static final Logger LOGGER = LoggerFactory.getLogger(HealthMonitoringServiceImpl.class);
 	private static final int RRD_PORT = 8181;
 	private static final String CONTEXT_PATH = "monitoring";
 	private static final String PROTOCOL = "http://";
     
    private Map<Integer,String> clusterMasterMap = new HashMap<Integer,String>();
 	private List<Machine> badMachines = new ArrayList<Machine>();
 	private List <Cluster> badClusters = new ArrayList<Cluster>();
     private Map<String,String> groupMachineMap = new HashMap<String,String>();
     private Map<String,String> hostnameIpMap = new HashMap<String,String>();
     private long previousClusterCheckTime = 0L;
 // Nishant: Removed the autowired bean and create dynamically based on cluster content
 //    @Autowired
 //    private RequestBuilder requestBuilder;
 
 
     @Override
     public NodeListResponse getHostList() {	
     	List<Node> activeNodes = new ArrayList<Node>();
     	List<Node> inactiveNodes = new ArrayList<Node>();
     	client.getParams().setIntParameter("http.connection.timeout", new Integer(this.connTimeout)); 	
     	NodeListResponse finalNodeResponse = new NodeListResponse();
     	// Nishant : Get all clusters using ClusterService as a Collection and iterate through it
     	Collection<Cluster> clusters = clusterService.getClusters();
     	long timeElapsed = (System.currentTimeMillis() - previousClusterCheckTime)/1000;
     	for (Cluster cluster : clusters) {
     		if(!badClusters.contains(cluster) || timeElapsed >= 120) {
     			int clusterId = cluster.getId();
     			// Nishant : Get all machines in the cluster using MachineService as a Collection and iterate through it
     			Collection<Machine> machinesInCluster = machineService.getMachinesInCluster(clusterId);
     			for(Machine machine : machinesInCluster) {
     				if(!badMachines.contains(machine)) {
     					List<Node> allNodes = new ArrayList<Node>();
     					// Nishant : Create request builder object dynamically for each machine in the cluster.
     					String url = getRequestBuilder(machine.getDnsName()).buildHostListRequest(new Request());
     					String response = HttpHelper.executeHttpRequest(client, url);
     					LOGGER.info("--------------------Request for machine "+machine.getDnsName()+" = " +url);
     					LOGGER.info("--------------------getHostList response for machine "+machine.getDnsName()+" = " +response);
     					NodeListResponse nodeResponse = toObject(response, NodeListResponse.class);
     					// Nishant: Check the node response on port 8181. Add to the consolidated list if status is OK.
     					if(nodeResponse.getResponseStatus() == AbstractResponse.STATUS_OK) {
     						if (nodeResponse.getActiveNodes() != null) {
     							activeNodes.addAll(nodeResponse.getActiveNodes());
     							allNodes.addAll(nodeResponse.getActiveNodes());
     						}
     						if (nodeResponse.getInactiveNodes() != null) {
     							inactiveNodes.addAll(nodeResponse.getInactiveNodes());
     							allNodes.addAll(nodeResponse.getInactiveNodes());
     						}
     						clusterMasterMap.put(clusterId,machine.getDnsName());
     						badMachines.remove(machine);
     						badClusters.remove(cluster);
     						for(Node node : allNodes) { 
     							if (!groupMachineMap.containsKey(node.getGroupName()))
     								groupMachineMap.put(node.getGroupName(),machine.getDnsName());
     							//hostnameIpMap.put(node.getNodeName(), node.getIpAddress());
     							hostnameIpMap.put(node.getNodeName(), machine.getDnsName());
     						}                   
     						break;
     					} else {
     						LOGGER.info("!!!!Machine:"+machine.getDnsName()+" is not responding!!!!!!!!!!!");
     						badMachines.add(machine);
     						clusterMasterMap.remove(clusterId);
     					}
     				}
     			}
     			// All machines in cluster have gone bad.
     			if(badMachines.containsAll(machinesInCluster)) {
     				badMachines.removeAll(machinesInCluster);
     				badClusters.add(cluster);
     				LOGGER.info("!!!!Cluster:"+clusterId+"(Name:"+cluster.getName()+"LBDNS:"+cluster.getLbDns()+") is not responding!!!!!!!");
     			}
     			if(badClusters.contains(cluster)) {
     				previousClusterCheckTime = System.currentTimeMillis();
     				LOGGER.info("!!!!Checking for bad cluster:"+clusterId+"(Name:"+cluster.getName()+"LBDNS:"+cluster.getLbDns()+") at "+new Date());
     			}
     		}
     	}
     	// Nishant : Create the final node list response of all clusters and return after sorting
     	finalNodeResponse.setActiveNodes(activeNodes);
     	finalNodeResponse.setInactiveNodes(inactiveNodes);
     	if (finalNodeResponse.getActiveNodes() != null) {
     		Collections.sort(finalNodeResponse.getActiveNodes());
     	}
     	if (finalNodeResponse.getInactiveNodes() != null) {
     		Collections.sort(finalNodeResponse.getInactiveNodes());
     	}  
     	return finalNodeResponse;
     }
 
     @Override
     public MetricTypesResponse getMetricTypes(Request request) {
         MetricTypesResponse response = null;
         if (StringUtils.isNotBlank(request.getSourceName())) {
     		// Nishant : Create request builder object dynamically for the machine or group for which metric types is required.
 			// If the souce name is present in groupMachine map then the request is for a group and the HTTP request will be 
 			// sent to the corresponding machine.
 			//****************************************************
         	String sourceName = "";
         	if(groupMachineMap.get(request.getSourceName()) != null){
         		sourceName = groupMachineMap.get(request.getSourceName());
         	} else {
         		sourceName = hostnameIpMap.get(request.getSourceName());
         	}        		
         	
             String url = getRequestBuilder(sourceName).buildMetricTypesRequest(request);
             String responseStr = HttpHelper.executeHttpRequest(client, url);
             response = toObject(responseStr, MetricTypesResponse.class);
             if (response.getMetricTypes() != null) {
                 Collections.sort(response.getMetricTypes());
             }
     		LOGGER.info("--------------------Request for machine "+url);
         } else {
         	response = new MetricTypesResponse();
             response.setResponseStatus(AbstractResponse.STATUS_PARAM_FAIL);
         }
 
         LOGGER.info("--------------------Returning final MetricTypesResponse = " +response.getMetricTypes());
         return response;
     }
 
     @Override
     public MetricNamesResponse getMetricNames(Request request) {
         MetricNamesResponse response = null;
         if (StringUtils.isNotBlank(request.getSourceName()) && StringUtils.isNotBlank(request.getMetricType())) {
     		// Nishant : Create request builder object dynamically for the machine or group for which metric names is required.
 			// If the souce name is present in groupMachine map then the request is for a group and the HTTP request will be 
 			// sent to the corresponding machine.
 			//****************************************************       	
         	String sourceName = "";
         	if(groupMachineMap.get(request.getSourceName()) != null){
         		sourceName = groupMachineMap.get(request.getSourceName());
         	} else {
         		sourceName = hostnameIpMap.get(request.getSourceName());
         	}  
         	
             String url = getRequestBuilder(sourceName).buildMetricNamesRequest(request);
             String responseStr = HttpHelper.executeHttpRequest(client, url);
             response = toObject(responseStr, MetricNamesResponse.class);
             if (response.getMetricNames() != null) {
                 Collections.sort(response.getMetricNames());
             }
     		LOGGER.info("--------------------Request for machine "+url);
         } else {
             response = new MetricNamesResponse();
             response.setResponseStatus(AbstractResponse.STATUS_PARAM_FAIL);
         }
         LOGGER.info("--------------------Returning final MetricNamesResponse = " +response.getMetricNames());
         return response;
     }
 
     @Override
     public HealthStatusResponse getHealthStatus(String sourceName, String sourceType, String metricType,
             String[] metricNames, Date startTime, Date endTime) {
         HealthStatusResponse response = null;
         if (StringUtils.isNotBlank(sourceName) && StringUtils.isNotBlank(metricType)
                 && ArrayUtils.isNotEmpty(metricNames)) {
     		// Nishant : Create request builder object dynamically for the machine or group for which health status is required.
 			// If the souce name is present in groupMachine map then the request is for a group and the HTTP request will be 
 			// sent to the corresponding machine.
 			//****************************************************       
         	String hostName = "";
         	if(groupMachineMap.get(sourceName)  != null ){
         		hostName = groupMachineMap.get(sourceName);
         	} else {
         		hostName = hostnameIpMap.get(sourceName);
         	}  
         	
         	String url =
             		getRequestBuilder(hostName).buildHealthStatusRequest(new Request(sourceName, sourceType, metricType,
                             metricNames, startTime, endTime, 100L));
             String responseStr = HttpHelper.executeHttpRequest(client, url);
             response = toObject(responseStr, HealthStatusResponse.class);
     		LOGGER.info("--------------------Request for machine "+url);
         } else {
             response = new HealthStatusResponse();
             response.setResponseStatus(AbstractResponse.STATUS_PARAM_FAIL);
         }
         return response;
     }
 
     @Override
     public String getHealthStatus() {
         throw new UnsupportedOperationException("Method is not implemented yet");
     }
 
     private <T extends AbstractResponse> T toObject(String source, Class<T> expectedType) {
         if (source == null || StringUtils.isEmpty(source)) {
             T newInstance = null;
             try {
                 newInstance = expectedType.newInstance();
                 newInstance.setResponseStatus(AbstractResponse.STATUS_RRD_FAIL);
             } catch (InstantiationException e) {
                 LOGGER.error(e.getMessage(), e);
             } catch (IllegalAccessException e) {
                 LOGGER.error(e.getMessage(), e);
             }
             return newInstance;
         }
         ObjectMapper mapper = new ObjectMapper();
         T obj = null;
         try {
             //LOGGER.info("================================================");
             //LOGGER.info("Now: {} : {}", System.currentTimeMillis(), source);
             //LOGGER.info("================================================");
             obj = mapper.readValue(source, expectedType);
         } catch (Exception e) {
             LOGGER.error("Exception occured while converting from String to Object. ", e);
         }
 
         return obj;
     }
 
     @Override
     public MetricBoundariesResponse getMetricBoundaries(String sourceName,String metricType) {
         MetricBoundariesResponse response = null;
         if (StringUtils.isNotBlank(metricType)) {
         	String hostName = "";
 			// Nishant : Create request builder object dynamically for the machine or group for which metric boundaries is required.
 			// If the souce name is present in groupMachine map then the request is for a group and the HTTP request will be 
 			// sent to the corresponding machine.
 			//****************************************************       
         	if(groupMachineMap.get(sourceName)  != null ){
         		hostName = groupMachineMap.get(sourceName);
         	} else {
         		hostName = hostnameIpMap.get(sourceName);
         	}  
         	String url = "";
         	url = getRequestBuilder(hostName).buildMetricBoundariesRequest(new Request(sourceName, metricType));
             String responseStr = HttpHelper.executeHttpRequest(client, url);
             response = toObject(responseStr, MetricBoundariesResponse.class);
     		LOGGER.info("--------------------Request for machine "+url);
         } else {
             response = new MetricBoundariesResponse();
             response.setResponseStatus(AbstractResponse.STATUS_PARAM_FAIL);
         }
         LOGGER.info("--------------------Returning final getMetricBoundaries = " +response.getBoundaries());
         return response;
     }
 
     @Override
     public GroupListResponse getGroupList() {
     	GroupListResponse finalGroupListResponse = new GroupListResponse();
 		finalGroupListResponse.setGroups(new HashMap<String, Set<String>>());
		Collection<String> masterMachines = clusterMasterMap.values();
 		for(String sourceName : masterMachines) {
 	        String url = getRequestBuilder(sourceName).buildGroupListRequest(new Request());
 	        String response = HttpHelper.executeHttpRequest(client, url);
 			
 	        GroupListResponse groupListResponse = toObject(response, GroupListResponse.class);
 	        if(groupListResponse.getGroups() != null)
 	        	finalGroupListResponse.getGroups().putAll(groupListResponse.getGroups());
 			LOGGER.info("--------------------Request for machine "+url);
 		}
 		LOGGER.info("--------------------Returning final GroupListResponse = " +finalGroupListResponse.getGroups());
 		if(finalGroupListResponse.getGroups().isEmpty()) {
 			finalGroupListResponse.setResponseStatus(AbstractResponse.STATUS_PARAM_FAIL);
 			return finalGroupListResponse;
 		}
 		return finalGroupListResponse;
     }
 
     @Override
     public NotificationResponse getNotifications(Long startTime, Long endTime) {
     	NotificationResponse finalNotificationResponse = new NotificationResponse();
     	List<Notification> notifications = new ArrayList<Notification>(); 	
     	finalNotificationResponse.setNotifications(notifications);
		Collection<String> masterMachines = clusterMasterMap.values();
 		for(String sourceName : masterMachines) {
 	    	// Nishant : Create request builder object dynamically for the machine for which notifications are required.
 			//****************************************************
 	        String url = getRequestBuilder(sourceName).buildNotificationsRequest(startTime, endTime);
 	        String response = HttpHelper.executeHttpRequest(client, url);
 			LOGGER.info("--------------------Request for machine "+url);
 			LOGGER.info("--------------------Returning NotificationResponse = " +response);    
 	        NotificationResponse notificationResponse = toObject(response, NotificationResponse.class);
 	        if(notificationResponse.getNotifications() != null)
 	        	finalNotificationResponse.getNotifications().addAll(notificationResponse.getNotifications());
 		}
 		
 		if (finalNotificationResponse.getNotifications().isEmpty()) {
 			finalNotificationResponse.setResponseStatus(AbstractResponse.STATUS_PARAM_FAIL);
 		}
 		Comparator<? super Notification> notificationsComparator = new Comparator<Notification>() {
 			@Override
 			public int compare(Notification n1, Notification n2) {
 				// TODO Auto-generated method stub
 				return n1.getFileModificationTime().compareTo(n2.getFileModificationTime());
 			}			
 		};
 		
 		//List<Notification> nonEmptyNotifications = finalNotificationResponse.getNotifications();
 		//for (Iterator<?> it = nonEmptyNotifications.iterator(); it.hasNext();)
 	    //    if ((it.next() == null)) it.remove();
 		//finalNotificationResponse.setNotifications(nonEmptyNotifications);
 		Collections.sort(finalNotificationResponse.getNotifications(), notificationsComparator);
 		Collections.reverse(finalNotificationResponse.getNotifications());
 		return finalNotificationResponse;
     }
     
     private RequestBuilder getRequestBuilder(String sourceName) {  
 		RequestBuilder requestBuilder = new RequestBuilder();
 		requestBuilder.setHostName(sourceName);
 		requestBuilder.setPort(RRD_PORT);
 		requestBuilder.setContextPath(CONTEXT_PATH);
 		requestBuilder.setProtocol(PROTOCOL);
 		return requestBuilder;
     } 
     
     public Map<Integer,String> getClusterMasterMap(){
         return clusterMasterMap; 
     }
 
 }
