 /* 
  * Licensed under the Apache License, Version 2.0 (the "License");
  *  you may not use this file except in compliance with the License.
  *  You may obtain a copy of the License at
  *
  *      http://www.apache.org/licenses/LICENSE-2.0
  *
  * Unless required by applicable law or agreed to in writing, software
  * distributed under the License is distributed on an "AS IS" BASIS,
  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  * See the License for the specific language governing permissions and
  * limitations under the License. 
  */
 package org.ngrinder.agent.controller;
 
 import com.google.common.base.Predicate;
 import com.google.common.collect.Collections2;
 import org.apache.commons.lang.StringUtils;
 import org.ngrinder.agent.service.AgentManagerService;
 import org.ngrinder.agent.service.AgentPackageService;
 import org.ngrinder.common.controller.BaseController;
 import org.ngrinder.common.controller.RestAPI;
 import org.ngrinder.model.AgentInfo;
 import org.ngrinder.region.model.RegionInfo;
 import org.ngrinder.region.service.RegionService;
 import org.springframework.beans.factory.annotation.Autowired;
 import org.springframework.http.HttpEntity;
 import org.springframework.security.access.prepost.PreAuthorize;
 import org.springframework.stereotype.Controller;
 import org.springframework.ui.ModelMap;
 import org.springframework.web.bind.annotation.PathVariable;
 import org.springframework.web.bind.annotation.RequestMapping;
 import org.springframework.web.bind.annotation.RequestMethod;
 import org.springframework.web.bind.annotation.RequestParam;
 
 import java.io.File;
 import java.util.List;
 import java.util.Map;
 
 import static org.ngrinder.common.util.CollectionUtils.newArrayList;
 import static org.ngrinder.common.util.CollectionUtils.newHashMap;
 
 /**
  * Agent management controller.
  *
  * @author JunHo Yoon
  * @since 3.1
  */
 @Controller
 @RequestMapping("/agent")
 @PreAuthorize("hasAnyRole('A', 'S')")
 public class AgentManagerController extends BaseController {
 
 	@SuppressWarnings("SpringJavaAutowiringInspection")
 	@Autowired
 	private AgentManagerService agentManagerService;
 
 	@Autowired
 	private RegionService regionService;
 
 	@Autowired
 	private AgentPackageService agentPackageService;
 
 	/**
 	 * Get the agents.
 	 *
 	 * @param region the region to search. If null, it returns all the attached
 	 *               agents.
 	 * @param model  model
 	 * @return agent/list
 	 */
 	@RequestMapping({"", "/", "/list"})
 	public String getAll(@RequestParam(value = "region", required = false) final String region, ModelMap model) {
 		List<AgentInfo> agents = agentManagerService.getAllVisible();
 		model.addAttribute("agents", Collections2.filter(agents, new Predicate<AgentInfo>() {
 			@Override
 			public boolean apply(AgentInfo agentInfo) {
 				if (StringUtils.equals(region, "all") || StringUtils.isEmpty(region)) {
 					return true;
 				}
 				final String eachAgentRegion = agentInfo.getRegion();
 				return eachAgentRegion.startsWith(region + "_owned") || region.equals(eachAgentRegion);
 			}
 		}));
 		model.addAttribute("region", region);
 		model.addAttribute("regions", regionService.getAllVisibleRegionNames());
 		File agentPackage = null;
 		if (isClustered()) {
 			if (StringUtils.isNotBlank(region)) {
 				final RegionInfo regionInfo = regionService.getOne(region);
 				agentPackage = agentPackageService.createAgentPackage(region, regionInfo.getIp(), regionInfo.getControllerPort(), null);
 			}
 		} else {
 			agentPackage = agentPackageService.createAgentPackage("", "", getConfig().getControllerPort(), null);
 		}
 		if (agentPackage != null) {
 			model.addAttribute("downloadLink", "/agent/download/" + agentPackage.getName());
 		}
 		return "agent/list";
 	}
 
 
 	/**
 	 * Get the agent detail info for the given agent id.
 	 *
 	 * @param id    agent id
 	 * @param model model
 	 * @return agent/agentDetail
 	 */
 	@RequestMapping("/{id}")
 	public String getOne(@PathVariable Long id, ModelMap model) {
 		model.addAttribute("agent", agentManagerService.getOne(id));
 		return "agent/detail";
 	}
 
 	/**
 	 * Clean up the agents in the inactive region
 	 */
 
 	@PreAuthorize("hasAnyRole('A')")
 	@RequestMapping(value = "/api", params = "action=cleanup", method = RequestMethod.POST)
	public HttpEntity<String> cleanUpAgentsInInactiveRegion() {
 		agentManagerService.cleanup();
		return successJsonHttpEntity();
 	}
 
 	/**
 	 * Get the current performance of the given agent.
 	 *
 	 * @param id   agent id
 	 * @param ip   agent ip
 	 * @param name agent name
 	 * @return json message
 	 */
 
 	@PreAuthorize("hasAnyRole('A')")
 	@RequestMapping("/api/{id}/state")
 	public HttpEntity<String> getState(@PathVariable Long id, @RequestParam String ip, @RequestParam String name) {
 		agentManagerService.requestShareAgentSystemDataModel(id);
 		return toJsonHttpEntity(agentManagerService.getSystemDataModel(ip, name));
 	}
 
 	/**
 	 * Get the current all agents state.
 	 *
 	 * @return json message
 	 */
 	@RestAPI
 	@PreAuthorize("hasAnyRole('A')")
 	@RequestMapping(value = {"/api/states/", "/api/states"}, method = RequestMethod.GET)
 	public HttpEntity<String> getStates() {
 		List<AgentInfo> agents = agentManagerService.getAllVisible();
 		return toJsonHttpEntity(getAgentStatus(agents));
 	}
 
 	/**
 	 * Get all agents from database.
 	 *
 	 * @return json message
 	 */
 	@RestAPI
 	@PreAuthorize("hasAnyRole('A')")
 	@RequestMapping(value = {"/api/", "/api"}, method = RequestMethod.GET)
 	public HttpEntity<String> getAll() {
 		return toJsonHttpEntity(agentManagerService.getAllVisible());
 	}
 
 	/**
 	 * Get the agent for the given agent id.
 	 *
 	 * @return json message
 	 */
 	@RestAPI
 	@PreAuthorize("hasAnyRole('A')")
 	@RequestMapping(value = "/api/{id}", method = RequestMethod.GET)
 	public HttpEntity<String> getOne(@PathVariable("id") Long id) {
 		return toJsonHttpEntity(agentManagerService.getOne(id));
 	}
 
 	/**
 	 * Approve an agent.
 	 *
 	 * @param id agent id
 	 * @return json message
 	 */
 	@RestAPI
 	@PreAuthorize("hasAnyRole('A')")
 	@RequestMapping(value = "/api/{id}", params = "action=approve", method = RequestMethod.PUT)
 	public HttpEntity<String> approve(@PathVariable("id") Long id) {
 		agentManagerService.approve(id, true);
 		return successJsonHttpEntity();
 	}
 
 	/**
 	 * Disapprove an agent.
 	 *
 	 * @param id agent id
 	 * @return json message
 	 */
 	@RestAPI
 	@PreAuthorize("hasAnyRole('A')")
 	@RequestMapping(value = "/api/{id}", params = "action=disapprove", method = RequestMethod.PUT)
 	public HttpEntity<String> disapprove(@PathVariable("id") Long id) {
 		agentManagerService.approve(id, false);
 		return successJsonHttpEntity();
 	}
 
 	/**
 	 * Stop the given agent.
 	 *
 	 * @param id agent id
 	 * @return json message
 	 */
 	@RestAPI
 	@PreAuthorize("hasAnyRole('A')")
 	@RequestMapping(value = "/api/{id}", params = "action=stop", method = RequestMethod.PUT)
 	public HttpEntity<String> stop(@PathVariable("id") Long id) {
 		agentManagerService.stopAgent(id);
 		return successJsonHttpEntity();
 	}
 
 	/**
 	 * Stop the given agent.
 	 *
 	 * @param ids comma separated agent id list
 	 * @return json message
 	 */
 	@RestAPI
 	@PreAuthorize("hasAnyRole('A')")
 	@RequestMapping(value = "/api", params = "action=stop", method = RequestMethod.PUT)
 	public HttpEntity<String> stop(@RequestParam("ids") String ids) {
 		String[] split = StringUtils.split(ids, ",");
 		for (String each : split) {
 			stop(Long.parseLong(each));
 		}
 		return successJsonHttpEntity();
 	}
 
 	/**
 	 * Update the given agent.
 	 *
 	 * @param id agent id
 	 * @return json message
 	 */
 	@RestAPI
 	@PreAuthorize("hasAnyRole('A')")
 	@RequestMapping(value = "/api/{id}", params = "action=update", method = RequestMethod.PUT)
 	public HttpEntity<String> update(@PathVariable("id") Long id) {
 		agentManagerService.update(id);
 		return successJsonHttpEntity();
 	}
 
 	/**
 	 * Send update message to agent side
 	 *
 	 * @param ids comma separated agent id list
 	 * @return json message
 	 */
 	@RestAPI
 	@PreAuthorize("hasAnyRole('A')")
 	@RequestMapping(value = "/api", params = "action=update", method = RequestMethod.PUT)
 	public HttpEntity<String> update(@RequestParam("ids") String ids) {
 		String[] split = StringUtils.split(ids, ",");
 		for (String each : split) {
 			update(Long.parseLong(each));
 		}
 		return successJsonHttpEntity();
 	}
 
 	private List<Map<String, Object>> getAgentStatus(List<AgentInfo> agents) {
 		List<Map<String, Object>> statuses = newArrayList(agents.size());
 		for (AgentInfo each : agents) {
 			Map<String, Object> result = newHashMap();
 			result.put("id", each.getId());
 			result.put("port", each.getPort());
 			result.put("icon", each.getState().getCategory().getIconName());
 			result.put("state", each.getState());
 			statuses.add(result);
 		}
 		return statuses;
 	}
 }
