 /*
  * Copyright (C) 2012 - 2012 NHN Corporation
  * All rights reserved.
  *
  * This file is part of The nGrinder software distribution. Refer to
  * the file LICENSE which is part of The nGrinder distribution for
  * licensing details. The nGrinder distribution is available on the
  * Internet at http://nhnopensource.org/ngrinder
  *
  * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
  * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
  * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
  * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
  * COPYRIGHT HOLDERS OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
  * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
  * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
  * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
  * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT,
  * STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
  * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
  * OF THE POSSIBILITY OF SUCH DAMAGE.
  */
 package org.ngrinder.chart.service;
 
 import static org.ngrinder.common.util.Preconditions.checkNotNull;
 
 import java.util.List;
 
import javax.annotation.PostConstruct;

 import org.ngrinder.chart.repository.JavaMonitorRepository;
 import org.ngrinder.chart.repository.SystemMonitorRepository;
import org.ngrinder.infra.AgentConfig;
import org.ngrinder.monitor.MonitorConstants;
 import org.ngrinder.monitor.controller.model.JavaDataModel;
 import org.ngrinder.monitor.controller.model.SystemDataModel;
 import org.springframework.beans.factory.annotation.Autowired;
 import org.springframework.stereotype.Service;
 
 
 
 /**
  * Monitor Service Class.
  * 
  * @author Mavlarn
  * @since 3.0
  */
 @Service
 public class MonitorService {
 
 	@Autowired
 	private JavaMonitorRepository javaMonitorRepository;
 
 	@Autowired
 	private SystemMonitorRepository sysMonitorRepository;
	
	@PostConstruct
	public void initMonitorEnv() {
		AgentConfig agentConfig = new AgentConfig();
		agentConfig.init();
		MonitorConstants.init(agentConfig);
	}
 
 
 	public JavaDataModel saveJavaMonitorInfo(JavaDataModel data) {
 		checkNotNull(data);
 		return javaMonitorRepository.save(data);
 	}
 
 	public SystemDataModel saveSystemMonitorInfo(SystemDataModel data) {
 		checkNotNull(data);
 		return sysMonitorRepository.save(data);
 	}
 	
 	public List<JavaDataModel> getJavaMonitorData(String ip, long startTime, long endTime) {
 		return javaMonitorRepository.findAllByIpAndCollectTimeBetween(ip, startTime, endTime);
 	}
 	
 	public List<SystemDataModel> getSystemMonitorData(String ip, long startTime, long endTime) {
 		return sysMonitorRepository.findAllByIpAndCollectTimeBetween(ip, startTime, endTime);
 	}
 
 }
