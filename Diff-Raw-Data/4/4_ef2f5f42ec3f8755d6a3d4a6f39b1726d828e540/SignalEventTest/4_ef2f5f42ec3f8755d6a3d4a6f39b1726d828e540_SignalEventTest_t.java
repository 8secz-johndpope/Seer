 /* Licensed under the Apache License, Version 2.0 (the "License");
  * you may not use this file except in compliance with the License.
  * You may obtain a copy of the License at
  * 
  *      http://www.apache.org/licenses/LICENSE-2.0
  * 
  * Unless required by applicable law or agreed to in writing, software
  * distributed under the License is distributed on an "AS IS" BASIS,
  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  * See the License for the specific language governing permissions and
  * limitations under the License.
  */
 package org.activiti.cdi.test.impl.bpmn;
 
 import static org.junit.Assert.assertEquals;
 
 import java.util.HashMap;
 import java.util.List;
 
 import javax.inject.Inject;
 import javax.inject.Named;
 
 import org.activiti.cdi.BusinessProcess;
 import org.activiti.cdi.test.CdiActivitiTestCase;
 import org.activiti.engine.RuntimeService;
 import org.activiti.engine.delegate.DelegateExecution;
 import org.activiti.engine.delegate.JavaDelegate;
 import org.activiti.engine.impl.context.Context;
 import org.activiti.engine.impl.interceptor.CommandContext;
 import org.activiti.engine.impl.persistence.entity.SignalEventSubscriptionEntity;
 import org.activiti.engine.runtime.ProcessInstance;
 import org.activiti.engine.test.Deployment;
 import org.junit.Test;
 
 public class SignalEventTest extends CdiActivitiTestCase {
   
 //  private static Logger log = Logger.getLogger(SignalEventTest.class.getName());
   
   @Named
   public static class SignalReceivedDelegate implements JavaDelegate {    
     
     @Inject
     private BusinessProcess businessProcess;
     
     public void execute(DelegateExecution execution) {
       businessProcess.setVariable("processName", "catchSignal-visited (was " + businessProcess.getVariable("processName")  + ")");
 //      log.log(Level.INFO, "");
     }
   }
 
   @Named
   public static class SendSignalDelegate implements JavaDelegate {
 
     @Inject
     private RuntimeService runtimeService;  
 
     @Inject
     private BusinessProcess businessProcess;   
 
     public void execute(DelegateExecution execution) throws Exception {
       businessProcess.setVariable("processName", "throwSignal-visited (was " + businessProcess.getVariable("processName")  + ")");
 
       String signalProcessInstanceId = (String) execution.getVariable("signalProcessInstanceId");      
       String executionId = runtimeService.createExecutionQuery().processInstanceId(signalProcessInstanceId).signalEventSubscription("alert").singleResult().getId();      
       
 //      runtimeService.signalEventReceived("alert", executionId);
       
       CommandContext commandContext = Context.getCommandContext();
       List<SignalEventSubscriptionEntity> findSignalEventSubscriptionsByEventName = commandContext
               .getEventSubscriptionManager()
               .findSignalEventSubscriptionsByNameAndExecution("alert", executionId);
 
       for (SignalEventSubscriptionEntity signalEventSubscriptionEntity : findSignalEventSubscriptionsByEventName) {
           signalEventSubscriptionEntity.eventReceived(null, true);
       }       
     }
 
   }
 
   @Test
  @Deployment(resources = {"org/activiti/cdi/test/impl/bpmn/SignalEventTests.catchAlertSignalBoundaryWithReceiveTask.bpmn20.xml", 
                          "org/activiti/cdi/test/impl/bpmn/SignalEventTests.throwAlertSignalWithDelegate.bpmn20.xml"})
   public void testSignalCatchBoundaryWithVariables() throws InterruptedException {
     HashMap<String, Object> variables1 = new HashMap<String, Object>();
     variables1.put("processName", "catchSignal");
     ProcessInstance piCatchSignal = runtimeService.startProcessInstanceByKey("catchSignal", variables1);
         
     HashMap<String, Object> variables2 = new HashMap<String, Object>();
     variables2.put("processName", "throwSignal");
     variables2.put("signalProcessInstanceId", piCatchSignal.getProcessInstanceId());
     ProcessInstance piThrowSignal = runtimeService.startProcessInstanceByKey("throwSignal", variables2);
     
     waitForJobExecutorToProcessAllJobs(1000, 10);
     
     assertEquals(1, runtimeService.createExecutionQuery().processInstanceId(piCatchSignal.getProcessInstanceId()).activityId("receiveTask").count());
     assertEquals(1, runtimeService.createExecutionQuery().processInstanceId(piThrowSignal.getProcessInstanceId()).activityId("receiveTask").count());
     
     assertEquals("catchSignal-visited (was catchSignal)", runtimeService.getVariable(piCatchSignal.getId(), "processName"));
     assertEquals("throwSignal-visited (was throwSignal)", runtimeService.getVariable(piThrowSignal.getId(), "processName"));
 
     // clean up
     runtimeService.signal(piCatchSignal.getId());
     runtimeService.signal(piThrowSignal.getId());
   }
 
 }
