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
 package org.ngrinder.agent.service;
 
 import java.util.List;
 
 import junit.framework.Assert;
 import net.grinder.message.console.AgentControllerState;
 
 import org.junit.Test;
 import org.ngrinder.AbstractNGrinderTransactionalTest;
 import org.ngrinder.agent.model.AgentInfo;
 import org.springframework.beans.factory.annotation.Autowired;
 
 /**
  * Agent service test
  * 
  * @author Tobi
  * @since 3.0
  */
 public class AgentServiceTest extends AbstractNGrinderTransactionalTest {
 
 	@Autowired
 	private AgentManagerService agentService;
 	
 	@Test
 	public void testSaveGetDeleteAgent() {
 		AgentInfo agent = this.saveAgent("save");
 		AgentInfo agent2 = agentService.getAgent(agent.getId());
 		Assert.assertNotNull(agent2);
 
 		List<AgentInfo> agentListDB = agentService.getAgentListOnDB();
 		agentListDB = agentService.getAgentListOnDB();
 		Assert.assertNotNull(agentListDB);
 
 		agentService.approve("1.1.1.1", true);
 		
 		agentService.deleteAgent(agent.getId());
 		agent2 = agentService.getAgent(agent.getId());
 		Assert.assertNull(agent2);
 	}
 
 	
 
 	private AgentInfo saveAgent(String key) {
 		AgentInfo agent = new AgentInfo();
 		agent.setIp("1.1.1.1");
 		agent.setHostName("testAppName" + key);
 		agent.setPort(8080);
 		agent.setRegion("testRegion" + key);
 		agent.setStatus(AgentControllerState.BUSY);
 		agentService.saveAgent(agent);
 		return agent;
 	}
 }
