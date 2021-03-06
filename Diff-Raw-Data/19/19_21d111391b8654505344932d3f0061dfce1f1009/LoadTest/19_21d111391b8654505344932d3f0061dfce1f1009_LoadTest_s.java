 /*
  * Copyright Myrrix Ltd
  *
  * Licensed under the Apache License, Version 2.0 (the "License");
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
 
 package net.myrrix.client;
 
 import java.util.concurrent.TimeUnit;
 
 import com.google.common.base.Stopwatch;
 import org.junit.BeforeClass;
 import org.junit.Test;
 
 import net.myrrix.common.LoadRunner;
 
 /**
  * @author Sean Owen
  * @since 1.0
  */
 public final class LoadTest extends AbstractClientTest {
   
   @BeforeClass
   public static void setUpMaxIterations() {
     // No real need for accuracy for a simple load test
     if (System.getProperty("model.iterations.max") == null) {
      System.setProperty("model.iterations.max", "2");
     }
   }
 
   @Override
   protected String getTestDataPath() {
     return "testdata/libimseti";
   }
 
   @Override
   protected boolean useSecurity() {
     return true;
   }
 
   @Test
   public void testLoad() throws Exception {
     ClientRecommender client = getClient();
    LoadRunner runner = new LoadRunner(client, getTestTempDir(), 10000);
     Stopwatch stopwatch = new Stopwatch().start();
     runner.runLoad();
     assertTrue(stopwatch.elapsed(TimeUnit.MILLISECONDS) < 40 * runner.getSteps());
   }
 
 }
