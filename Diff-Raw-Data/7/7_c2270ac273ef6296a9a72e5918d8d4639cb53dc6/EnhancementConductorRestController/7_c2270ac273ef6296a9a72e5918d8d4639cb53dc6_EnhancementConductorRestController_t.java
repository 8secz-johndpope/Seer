 /**
  * Copyright 2008 The University of North Carolina at Chapel Hill
  *
  * Licensed under the Apache License, Version 2.0 (the "License");
  * you may not use this file except in compliance with the License.
  * You may obtain a copy of the License at
  *
  *   http://www.apache.org/licenses/LICENSE-2.0
  *
  * Unless required by applicable law or agreed to in writing, software
  * distributed under the License is distributed on an "AS IS" BASIS,
  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  * See the License for the specific language governing permissions and
  * limitations under the License.
  */
 package edu.unc.lib.dl.cdr.services.rest;
 
 import java.io.IOException;
 import java.io.PrintWriter;
 import java.util.ArrayList;
 import java.util.Date;
 import java.util.HashMap;
 import java.util.Iterator;
 import java.util.List;
 import java.util.Map;
 import java.util.Map.Entry;
 import java.util.Set;
 
 import javax.annotation.PostConstruct;
 import javax.annotation.Resource;
 import javax.servlet.http.HttpServletResponse;
 
 import org.jdom.output.XMLOutputter;
 import org.slf4j.Logger;
 import org.slf4j.LoggerFactory;
 import org.springframework.stereotype.Controller;
 import org.springframework.web.bind.annotation.PathVariable;
 import org.springframework.web.bind.annotation.RequestMapping;
 import org.springframework.web.bind.annotation.RequestMethod;
 import org.springframework.web.bind.annotation.RequestParam;
 import org.springframework.web.bind.annotation.ResponseBody;
 
 import edu.unc.lib.dl.cdr.services.ObjectEnhancementService;
 import edu.unc.lib.dl.cdr.services.model.AbstractXMLEventMessage;
 import edu.unc.lib.dl.cdr.services.model.CDREventMessage;
 import edu.unc.lib.dl.cdr.services.model.EnhancementMessage;
 import edu.unc.lib.dl.cdr.services.model.FailedEnhancementObject;
 import edu.unc.lib.dl.cdr.services.model.FailedEnhancementObject.MessageFailure;
 import edu.unc.lib.dl.cdr.services.model.FailedObjectHashMap;
 import edu.unc.lib.dl.cdr.services.model.FedoraEventMessage;
 import edu.unc.lib.dl.cdr.services.processing.EnhancementConductor;
 import edu.unc.lib.dl.cdr.services.processing.EnhancementConductor.PerformServicesRunnable;
 import edu.unc.lib.dl.message.ActionMessage;
 
 /**
  * 
  * @author bbpennel
  *
  */
 @Controller
 @RequestMapping(value={"/enhancement*", "/enhancement"})
 public class EnhancementConductorRestController extends AbstractServiceConductorRestController {
 	private static final Logger LOG = LoggerFactory.getLogger(EnhancementConductorRestController.class);
 	public static final String BASE_PATH = "/rest/enhancement/";
 	public static final String QUEUED_PATH = "queued";
 	public static final String BLOCKED_PATH = "blocked";
 	public static final String ACTIVE_PATH = "active";
 	public static final String FAILED_PATH = "failed";
 	
 	@Resource
 	private EnhancementConductor enhancementConductor;
 	
 	private Map<String,String> serviceNameLookup;
 	
 	@PostConstruct
 	public void init(){
 		serviceNameLookup = new HashMap<String,String>();
 		for (ObjectEnhancementService service: enhancementConductor.getServices()){
 			serviceNameLookup.put(service.getClass().getName(), service.getName());
 		}
 	}
 	
 	@RequestMapping(value = { "", "/" }, method = RequestMethod.GET)
 	public @ResponseBody Map<String, ? extends Object> getInfo() {
 		Map<String, Object> result = new HashMap<String, Object>();
 		addServiceConductorInfo(result, this.enhancementConductor);
 		result.put("pendingJobs", this.enhancementConductor.getQueueSize());
 		result.put("queuedJobs", this.enhancementConductor.getPidQueue().size());
 		result.put("blockedJobs", this.enhancementConductor.getCollisionList().size());
 		result.put("failedJobs", this.enhancementConductor.getFailedPids().size());
 		result.put("activeJobs", this.enhancementConductor.getExecutor().getRunningNow().size());
 		
 		Map<String, Object> uris = new HashMap<String, Object>();
 		result.put("uris", uris);
 		
 		uris.put("queuedJobs", BASE_PATH + QUEUED_PATH);
 		uris.put("blockedJobs", BASE_PATH + BLOCKED_PATH);
 		uris.put("activeJobs", BASE_PATH + ACTIVE_PATH);
 		uris.put("failedJobs", BASE_PATH + FAILED_PATH);
 		
 		return result;
 	}
 	
 	@RequestMapping(value = QUEUED_PATH, method = RequestMethod.GET)
 	public @ResponseBody Map<String, ? extends Object> getQueuedInfo( 
 			@RequestParam(value = "begin", required = false) Integer begin,
 			@RequestParam(value = "end", required = false) Integer end) {
 		
 		Map<String, Object> result = new HashMap<String, Object>();
 		
 		//Duplicate pid queue so that we can iterate over it.
 		List<ActionMessage> queued = new ArrayList<ActionMessage>(this.enhancementConductor.getPidQueue());
 		addMessageListInfo(result, queued, begin, end, QUEUED_PATH);
 		
 		return result;
 	}
 	
 	@RequestMapping(value = BLOCKED_PATH, method = RequestMethod.GET)
 	public @ResponseBody Map<String, ? extends Object> getBlockedInfo( 
 			@RequestParam(value = "begin", required = false) Integer begin,
 			@RequestParam(value = "end", required = false) Integer end) {
 		
 		Map<String, Object> result = new HashMap<String, Object>();
 		List<ActionMessage> messageList = new ArrayList<ActionMessage>(this.enhancementConductor.getCollisionList());
 		addMessageListInfo(result, messageList, begin, end, BLOCKED_PATH);
 		return result;
 	}
 	
 	@RequestMapping(value = ACTIVE_PATH, method = RequestMethod.GET)
 	public @ResponseBody Map<String, ? extends Object> getActiveInfo( 
 			@RequestParam(value = "begin", required = false) Integer begin,
 			@RequestParam(value = "end", required = false) Integer end) {
 		
 		Set<PerformServicesRunnable> currentlyRunning = this.enhancementConductor.getExecutor().getRunningNow();
 		List<ActionMessage> messages = new ArrayList<ActionMessage>();
 		for (PerformServicesRunnable task: currentlyRunning){
 			messages.add(task.getMessage());
 		}
 		Map<String, Object> result = new HashMap<String, Object>();
 		addMessageListInfo(result, messages, begin, end, ACTIVE_PATH);
 		return result;
 	}
 	
 	/**
 	 * Returns a view of the failed job list.
 	 * Failed list does not retain order, so paging is not possible.
 	 * @return
 	 */
 	@RequestMapping(value = FAILED_PATH, method = RequestMethod.GET)
 	public @ResponseBody Map<String, ? extends Object> getFailedInfo() {
 		
 		Map<String, Object> result = new HashMap<String, Object>();
 		
 		List<Map<String, Object>> jobs = new ArrayList<Map<String, Object>>();
 		result.put("jobs", jobs);
 		
 		FailedObjectHashMap failedList = this.enhancementConductor.getFailedPids();
 		result.put("count", failedList.size());
 		
 		Iterator<Entry<String,FailedEnhancementObject>> iterator = failedList.entrySet().iterator();
 		while (iterator.hasNext()){
 			Entry<String,FailedEnhancementObject> entry = iterator.next();
 			Map<String, Object> failedEntry = new HashMap<String, Object>();
 			failedEntry.put("id", entry.getKey());
 			List<String> failedServices = new ArrayList<String>();
 			failedEntry.put("failedServices", failedServices);
 			for (String failedService: entry.getValue().getFailedServices()){
 				failedServices.add(this.serviceNameLookup.get(failedService));
 			}
 			failedEntry.put("timestamp", entry.getValue().getTimestamp());
 			if (entry.getValue().getMessages() != null){
 				Map<String, Object> uris = new HashMap<String, Object>();
 				failedEntry.put("uris", uris);
				uris.put("jobInfo", BASE_PATH + FAILED_PATH + "/job/");
				
				List<String> messageIDList = new ArrayList<String>();
				failedEntry.put("messageIDs", messageIDList);
 				for (ActionMessage message: entry.getValue().getMessages()){
					messageIDList.add(message.getMessageID());
 				}
 			}
 			jobs.add(failedEntry);
 		}
 		
 		return result;
 	}
 	
 	@RequestMapping(value = { FAILED_PATH + "/job/{id}" }, method = RequestMethod.GET)
 	public @ResponseBody Map<String, ? extends Object> getFailedMessageInfo(@PathVariable("id") String id){
 		if (id == null || id.length() == 0)
 			return null;
 		MessageFailure messageFailure;
 		try {
 			messageFailure = this.enhancementConductor.getFailedPids().getMessageFailure(id);
 			Map<String, Object> jobInfo = getJobFullInfo(messageFailure.getMessage(), FAILED_PATH);
 			jobInfo.put("stackTrace", messageFailure.getFailureLog());			
 			
 			return jobInfo;
 		} catch (IOException e) {
 			LOG.error("Failed to load stack trace file for " + id, e);
 			return null;
 		}
 	}
 	
 	/**
 	 * Returns the full details for a job selected by id, which is the hash code of the message object
 	 * until another identifier is added.
 	 * @param id
 	 * @return
 	 */
 	@RequestMapping(value = { QUEUED_PATH + "/job/{id}" }, method = RequestMethod.GET)
 	public @ResponseBody Map<String, ? extends Object> getQueuedJobInfo(@PathVariable("id") String id){
 		return getJobFullInfo(lookupJobInfo(id, new ArrayList<ActionMessage>(this.enhancementConductor.getPidQueue())), QUEUED_PATH);
 	}
 	
 	@RequestMapping(value = { BLOCKED_PATH + "/job/{id}" }, method = RequestMethod.GET)
 	public @ResponseBody Map<String, ? extends Object> getBlockedJobInfo(@PathVariable("id") String id){
 		return getJobFullInfo(lookupJobInfo(id, new ArrayList<ActionMessage>(this.enhancementConductor.getCollisionList())), BLOCKED_PATH);
 	}
 	
 	@RequestMapping(value = { ACTIVE_PATH + "/job/{id}" }, method = RequestMethod.GET)
 	public @ResponseBody Map<String, ? extends Object> getActiveJobInfo(@PathVariable("id") String id){
 		Set<PerformServicesRunnable> currentlyRunning = this.enhancementConductor.getExecutor().getRunningNow();
 		List<ActionMessage> messages = new ArrayList<ActionMessage>();
 		for (PerformServicesRunnable task: currentlyRunning){
 			messages.add(task.getMessage());
 		}
 		return getJobFullInfo(lookupJobInfo(id, messages), ACTIVE_PATH);
 	}
 	
 	/**
 	 * Transforms a PIDMessage object into a map of properties for reporting purposes.
 	 * @return
 	 */
 	@Override
 	protected Map<String,Object> getJobBriefInfo(ActionMessage actionMessage, String queuePath){
 		if (actionMessage == null)
 			return null;
 		EnhancementMessage message = (EnhancementMessage)actionMessage;
 		Map<String, Object> job = new HashMap<String, Object>();
 		
 		job.put("id", message.getMessageID());
 		job.put("targetPID", message.getTargetID());
 		addJobPropertyIfNotEmpty("depositID", message.getDepositID(), job);
 		addJobPropertyIfNotEmpty("action", message.getQualifiedAction(), job);
 		addJobPropertyIfNotEmpty("serviceName", message.getServiceName(), job);
 
 		if (message instanceof FedoraEventMessage){
 			FedoraEventMessage fMessage = (FedoraEventMessage)message;
 			addJobPropertyIfNotEmpty("dataStream", fMessage.getDatastream(), job);
 			addJobPropertyIfNotEmpty("relation", fMessage.getRelationPredicate(), job);
 			addJobPropertyIfNotEmpty("generatedTimestamp", fMessage.getEventTimestamp(), job);
 		} else if (message instanceof CDREventMessage){
 			CDREventMessage cdrMessage = (CDREventMessage)message;
 			addJobPropertyIfNotEmpty("mode", cdrMessage.getMode(), job);
 			addJobPropertyIfNotEmpty("targetParent", cdrMessage.getParent(), job);
 		}
 		
 		Date timeCreated = new Date(message.getTimeCreated());
 		job.put("queuedTimestamp", formatISO8601.format(timeCreated));
 		
 		if (message.getFilteredServices() != null){
 			List<String> filteredServices = new ArrayList<String>();
 			for (String service: message.getFilteredServices()){
 				filteredServices.add(service);
 			}
 			job.put("filteredServices", filteredServices);
 		}
 		
 		Map<String, Object> uris = new HashMap<String, Object>();
 		job.put("uris", uris);
 		uris.put("jobInfo", BASE_PATH + queuePath + "/job/" + message.getMessageID());
 
 		return job;
 	}
 	
 	/**
 	 * Transforms a PIDMessage object into a map of properties for reporting purposes.
 	 * @return
 	 */
 	private Map<String, Object> getJobFullInfo(ActionMessage actionMessage, String queuedPath){
 		if (actionMessage == null)
 			return null;
 		EnhancementMessage message = (EnhancementMessage)actionMessage;
 		Map<String, Object> job = new HashMap<String, Object>();
 		
 		job.put("id", message.getMessageID());
 		job.put("targetPID", message.getTargetID());
 		addJobPropertyIfNotEmpty("depositID", message.getDepositID(), job);
 		addJobPropertyIfNotEmpty("action", message.getQualifiedAction(), job);
 		addJobPropertyIfNotEmpty("serviceName", message.getServiceName(), job);
 
 		if (message instanceof FedoraEventMessage){
 			FedoraEventMessage fMessage = (FedoraEventMessage)message;
 			addJobPropertyIfNotEmpty("dataStream", fMessage.getDatastream(), job);
 			addJobPropertyIfNotEmpty("relationPredicate", fMessage.getRelationPredicate(), job);
 			addJobPropertyIfNotEmpty("relationObject", fMessage.getRelationObject(), job);
 			addJobPropertyIfNotEmpty("generatedTimestamp", fMessage.getEventTimestamp(), job);
 		} else if (message instanceof CDREventMessage){
 			CDREventMessage cdrMessage = (CDREventMessage)message;
 			addJobPropertyIfNotEmpty("mode", cdrMessage.getMode(), job);
 			addJobPropertyIfNotEmpty("targetParent", cdrMessage.getParent(), job);
 			addJobPropertyIfNotEmpty("oldParents", cdrMessage.getOldParents(), job);
 			addJobPropertyIfNotEmpty("reordered", cdrMessage.getReordered(), job);
 			addJobPropertyIfNotEmpty("subjects", cdrMessage.getSubjects(), job);
 			addJobPropertyIfNotEmpty("generatedTimestamp", cdrMessage.getEventTimestamp(), job);
 		}
 		Date timeCreated = new Date(message.getTimeCreated());
 		job.put("queuedTimestamp", formatISO8601.format(timeCreated));
 		
 		if (message.getFilteredServices() != null){
 			List<String> filteredServices = new ArrayList<String>();
 			for (String service: message.getFilteredServices()){
 				filteredServices.add(service);
 			}
 			job.put("filteredServices", filteredServices);
 		}
 		
 		Map<String, Object> uris = new HashMap<String, Object>();
 		job.put("uris", uris);
 		if (message instanceof AbstractXMLEventMessage && ((AbstractXMLEventMessage)message).getMessageBody() != null){
 			uris.put("xml", BASE_PATH + queuedPath + "/job/" + message.getMessageID() + "/xml");
 		}
 		uris.put("targetInfo", ItemInfoRestController.BASE_PATH + message.getTargetID());
 		
 		return job;
 	}
 	
 	@RequestMapping(value = { QUEUED_PATH + "/job/{id}/xml" }, method = RequestMethod.GET)
 	public void getQueuedJobXML(HttpServletResponse response, @PathVariable("id") String id) throws IOException{
 		response.setContentType("application/xml");
 		PrintWriter pr = response.getWriter();
 		pr.write(getJobXML(id, new ArrayList<ActionMessage>(this.enhancementConductor.getPidQueue())));
 	}
 	
 	@RequestMapping(value = { BLOCKED_PATH + "/job/{id}/xml" }, method = RequestMethod.GET)
 	public void getBlockedJobXML(HttpServletResponse response, @PathVariable("id") String id) throws IOException{
 		response.setContentType("application/xml");
 		PrintWriter pr = response.getWriter();
 		pr.write(getJobXML(id, new ArrayList<ActionMessage>(this.enhancementConductor.getCollisionList())));
 	}
 	
 	@RequestMapping(value = { ACTIVE_PATH + "/job/{id}/xml" }, method = RequestMethod.GET)
 	public void getActiveJobXML(HttpServletResponse response, @PathVariable("id") String id) throws IOException{
 		response.setContentType("application/xml");
 		PrintWriter pr = response.getWriter();
 		Set<PerformServicesRunnable> currentlyRunning = this.enhancementConductor.getExecutor().getRunningNow();
 		List<ActionMessage> messages = new ArrayList<ActionMessage>();
 		for (PerformServicesRunnable task: currentlyRunning){
 			messages.add(task.getMessage());
 		}
 		pr.write(getJobXML(id, messages));
 	}
 	
 	private String getJobXML(String id, List<ActionMessage> messages){
 		ActionMessage aMessage = lookupJobInfo(id, messages);
 		if (!(aMessage instanceof AbstractXMLEventMessage))
 			return null;
 		AbstractXMLEventMessage message = (AbstractXMLEventMessage)lookupJobInfo(id, messages);
 		if (message != null && message.getMessageBody() != null){
 			XMLOutputter outputter = new XMLOutputter();
 			try {
 				return outputter.outputString(message.getMessageBody());
 			} catch (Exception e) {
 				LOG.error("Error while generating xml output for " + id, e);
 			}
 		}
 		return null;
 	}
 	
 	private void addJobPropertyIfNotEmpty(String propertyName, Object propertyValue, Map<String, Object> job){
 		if (propertyValue != null || !"".equals(propertyValue)){
 			job.put(propertyName, propertyValue);
 		}
 	}
 
 	public void setEnhancementConductor(EnhancementConductor enhancementConductor) {
 		this.enhancementConductor = enhancementConductor;
 	}
 }
