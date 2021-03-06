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
 package org.ngrinder.region.service;
 
 import static org.ngrinder.common.util.ExceptionUtils.processException;
 
 import java.io.File;
 import java.util.HashSet;
 import java.util.Map;
 
 import javax.annotation.PostConstruct;
 import javax.annotation.PreDestroy;
 
 import net.grinder.common.processidentity.AgentIdentity;
 import net.grinder.util.thread.InterruptibleRunnable;
 import net.sf.ehcache.Ehcache;
 
 import org.apache.commons.io.FileUtils;
 import org.apache.commons.lang.StringUtils;
 import org.ngrinder.infra.config.Config;
 import org.ngrinder.infra.schedule.ScheduledTask;
 import org.ngrinder.perftest.service.AgentManager;
 import org.ngrinder.region.model.RegionInfo;
 import org.springframework.beans.factory.annotation.Autowired;
 import org.springframework.cache.Cache;
 import org.springframework.cache.Cache.ValueWrapper;
 import org.springframework.cache.CacheManager;
 import org.springframework.stereotype.Service;
 
 import com.google.common.collect.Maps;
 import com.google.common.collect.Sets;
 
 /**
  * Region service class. This class responsible to keep the status of available regions.
  * 
  * @author Mavlarn
  * @author JunHo Yoon
  * @since 3.1
  */
 @Service
 public class RegionService {
 
 	@Autowired
 	private Config config;
 
 	@Autowired
 	private ScheduledTask scheduledTask;
 
 	@Autowired
 	private CacheManager cacheManager;
 	private Cache cache;
 
 	/**
 	 * Set current region into cache, using the IP as key and region name as value.
 	 * 
 	 */
 	@PostConstruct
 	public void initRegion() {
 		if (config.isCluster()) {
 			cache = cacheManager.getCache("regions");
 			verifyDuplicateRegion();
 			scheduledTask.addScheduledTaskEvery3Sec(new InterruptibleRunnable() {
 				@Override
 				public void interruptibleRun() {
 					checkRegionUpdate();
 				}
 
 			});
 		}
 	}
 
 	/**
 	 * Verify duplicate region when starting with cluster mode.
 	 * 
 	 * @since 3.2
 	 */
 	private void verifyDuplicateRegion() {
 		Map<String, RegionInfo> regions = getRegions();
 		String localRegion = getCurrentRegion();
 		RegionInfo regionInfo = regions.get(localRegion);
 		if (regionInfo != null && !StringUtils.equals(regionInfo.getIp(), config.getCurrentIP())) {
 			throw processException("The region name, " + localRegion
 							+ ", is already used by other controller " + regionInfo.getIp()
 							+ ". Please set the different region name in this controller.");
 		}
 	}
 
 	@Autowired
 	private AgentManager agentManager;
 
 	/**
 	 * check Region and Update its value.
 	 */
 	public void checkRegionUpdate() {
 		if (!config.isInvisibleRegion()) {
 			HashSet<AgentIdentity> newHashSet = Sets.newHashSet(agentManager.getAllAttachedAgents());
 			cache.put(getCurrentRegion(), new RegionInfo(config.getCurrentIP(), newHashSet));
 		}
 	}
 
 	/**
 	 * Destroy method. this method is responsible to delete our current region from dist cache.
 	 */
 	@PreDestroy
 	public void destroy() {
 		if (config.isCluster()) {
 			File file = new File(config.getHome().getControllerShareDirectory(), config.getRegion());
 			FileUtils.deleteQuietly(file);
 		}
 	}
 
 	/**
 	 * Get current region. This method returns where this service is running.
 	 * 
 	 * @return current region.
 	 */
 	public String getCurrentRegion() {
 		return config.getRegion();
 	}
 
 	/**
 	 * Get region list of all clustered controller.
 	 * 
 	 * @return region list
 	 */
 	public Map<String, RegionInfo> getRegions() {
 		Map<String, RegionInfo> regions = Maps.newHashMap();
 		if (config.isCluster()) {
 			for (Object eachKey : ((Ehcache) (cache.getNativeCache())).getKeys()) {
 				ValueWrapper valueWrapper = cache.get(eachKey);
 				if (valueWrapper != null && valueWrapper.get() != null) {
 					regions.put((String) eachKey, (RegionInfo) valueWrapper.get());
 				}
 			}
 		}
 		return regions;
 	}
 
 	Config getConfig() {
 		return config;
 	}
 
 	void setConfig(Config config) {
 		this.config = config;
 	}
 }
