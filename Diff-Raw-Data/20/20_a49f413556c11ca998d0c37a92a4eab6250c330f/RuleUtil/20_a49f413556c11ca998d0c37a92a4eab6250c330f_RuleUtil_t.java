 /**
  * Copyright 2010 OpenEngSB Division, Vienna University of Technology
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
 
 package org.openengsb.core.workflow;
 
 import java.io.IOException;
 import java.io.InputStream;
 
 import org.apache.commons.io.IOUtils;
 import org.openengsb.core.common.workflow.RuleBaseException;
 import org.openengsb.core.common.workflow.RuleManager;
 import org.openengsb.core.common.workflow.model.RuleBaseElementId;
 import org.openengsb.core.common.workflow.model.RuleBaseElementType;
 
 public final class RuleUtil {
 
     private RuleUtil() {
 
     }
 
     public static void addHello1Rule(RuleManager manager) throws Exception {
         RuleBaseElementId id = new RuleBaseElementId(RuleBaseElementType.Rule, "hello1");
         String rule = readRule();
         manager.add(id, rule);
     }
 
     private static String readRule() throws IOException {
         InputStream helloWorldRule = null;
         try {
             helloWorldRule = RuleUtil.class.getClassLoader().getResourceAsStream("rulebase/org/openengsb/hello1.rule");
             return IOUtils.toString(helloWorldRule);
         } finally {
             IOUtils.closeQuietly(helloWorldRule);
         }
     }
 
     public static void addTestFlows(RuleManager manager) throws Exception {
         addFlow(manager, "flowtest");
         addFlow(manager, "ci");
         addFlow(manager, "floweventtest");
        
        manager.addImport("org.openengsb.core.common.workflow.model.ProcessBag");
        manager.addImport("org.openengsb.core.common.Event");
        addFlow(manager, "propertybagtest");
     }
 
     private static void addFlow(RuleManager manager, String flow) throws IOException, RuleBaseException {
         RuleBaseElementId testFlowId = new RuleBaseElementId(RuleBaseElementType.Process, flow);
         String code = readFlow(flow);
         manager.add(testFlowId, code);
     }
 
     private static String readFlow(String string) throws IOException {
         InputStream flowStream =
             RuleUtil.class.getClassLoader().getResourceAsStream("rulebase/org/openengsb/" + string + ".rf");
         return IOUtils.toString(flowStream);
     }
 
 }
