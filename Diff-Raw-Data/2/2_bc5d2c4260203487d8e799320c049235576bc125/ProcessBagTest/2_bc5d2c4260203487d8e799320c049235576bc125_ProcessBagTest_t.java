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
 
 package org.openengsb.core.common.workflow.model;
 
 import static org.hamcrest.CoreMatchers.is;
 import static org.junit.Assert.assertEquals;
 import static org.junit.Assert.assertFalse;
 import static org.junit.Assert.assertThat;
 import static org.junit.Assert.assertTrue;
 
 import java.util.Set;
 import java.util.concurrent.Callable;
 import java.util.concurrent.Executors;
 import java.util.concurrent.Future;
 import java.util.concurrent.TimeUnit;
 
 import org.junit.Before;
 import org.junit.Test;
 import org.openengsb.core.common.workflow.ProcessBagException;
 
 public class ProcessBagTest {
     private ProcessBag pb;
 
     @Before
     public void init() throws Exception {
         pb = new ProcessBag();
     }
 
     @Test(expected = ProcessBagException.class)
     public void addProperty_shouldReturnException() throws ProcessBagException {
         pb.addProperty("test", "42");
         pb.addProperty("test", "42");
     }
 
     @Test
     public void getPropertyClass_shouldReturnStringClass() throws Exception {
         pb.addProperty("test", "42");
         assertTrue(pb.getPropertyClass("test") == String.class);
     }
 
     @Test
     public void getPropertyKeyList_shouldReturnTwo() throws Exception {
         pb.addProperty("test", "42");
         pb.addProperty("number", 42);
 
         Set<String> list = pb.propertyKeySet();
         assertThat(list.size(), is(2));
     }
 
     @Test
     public void removeProperty_shouldContainStringOnly() throws Exception {
         pb.addProperty("number", 42);
         pb.addProperty("string", "42");
         pb.removeProperty("number");
 
         Set<String> list = pb.propertyKeySet();
         assertTrue(list.contains("string"));
         assertFalse(list.contains("number"));
     }
 
     @Test
     public void getPropertyCount_shouldReturnTwo() throws Exception {
         pb.addProperty("test", "42");
         pb.addProperty("number", 42);
 
         assertTrue(pb.propertyCount() == 2);
     }
 
     @Test
     public void addOrReplaceProperty_shouldOverwriteValue() throws Exception {
         pb.addProperty("test", "42");
         pb.addOrReplaceProperty("test", "43");
         assertEquals(pb.getProperty("test"), "43");
     }
 
     @Test
     public void removeAllProperties_shouldDeleteEverything() throws Exception {
         pb.addProperty("test", "42");
         pb.addProperty("number", 42);
 
         pb.removeAllProperties();
         assertTrue(pb.propertyCount() == 0);
     }
 
     @Test
     public void setProcessIdLater_shouldBlockGetter() throws Exception {
         Callable<String> task = new Callable<String>() {
             @Override
             public String call() throws Exception {
                 return pb.getProcessId();
             }
         };
         Future<String> processIdFuture = Executors.newSingleThreadExecutor().submit(task);
 
         Thread.sleep(100);
         // this task should block now
         assertThat(processIdFuture.isDone(), is(false));
 
         // just call it to make sure it isn't blocking
        pb.propertyCount();
 
         pb.setProcessId("foo");
         processIdFuture.get(1, TimeUnit.SECONDS);
     }
 }
