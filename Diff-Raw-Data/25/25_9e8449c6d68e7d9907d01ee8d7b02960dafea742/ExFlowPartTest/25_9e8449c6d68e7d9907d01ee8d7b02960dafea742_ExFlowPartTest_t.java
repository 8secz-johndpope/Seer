 /**
  * Copyright 2011 Asakusa Framework Team.
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
 package ${package}.flowpart;
 
 import ${package}.modelgen.table.model.Ex1;
 
import org.junit.Test;
 
 import com.asakusafw.testdriver.FlowPartTester;
 import com.asakusafw.vocabulary.flow.FlowDescription;
 import com.asakusafw.vocabulary.flow.In;
 import com.asakusafw.vocabulary.flow.Out;
 
 /**
  * サンプル：フロー部品のテストクラス
  */
 public class ExFlowPartTest {
 
     /**
      * サンプル：フロー部品の実行
      * 
      * @throws Throwable 
      */
     @Test
     public void testExample() throws Throwable {
 
         String testDataSheet = "EX1.xls";
 
         FlowPartTester driver = new FlowPartTester(this.getClass());
 
         In<Ex1> in = driver.input("ex1", Ex1.class).prepare(testDataSheet);
         Out<Ex1> out = driver.output("ex1", Ex1.class).verify(testDataSheet + "#:1", testDataSheet+ "#:2");
 
         FlowDescription flowDesc = new ExFlowPart(in, out);
         driver.runTest(flowDesc);
     }
 
 }
