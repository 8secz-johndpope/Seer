 /**
  * Copyright 2012-2013 Rafal Lewczuk <rafal.lewczuk@jitlogic.com>
  * <p/>
  * This is free software. You can redistribute it and/or modify it under the
  * terms of the GNU General Public License as published by the Free Software
  * Foundation, either version 3 of the License, or (at your option) any later
  * version.
  * <p/>
  * This software is distributed in the hope that it will be useful, but WITHOUT ANY
  * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
  * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
  * <p/>
  * You should have received a copy of the GNU General Public License
  * along with this software. If not, see <http://www.gnu.org/licenses/>.
  */
 package com.jitlogic.zorka.central.test;
 
 
 import com.jitlogic.zorka.central.Store;
 import com.jitlogic.zorka.central.test.support.CentralFixture;
 
 import com.jitlogic.zorka.common.test.support.TestTraceGenerator;
 import com.jitlogic.zorka.common.tracedata.FressianTraceWriter;
 import com.jitlogic.zorka.common.tracedata.TraceRecord;
 import com.jitlogic.zorka.common.zico.ZicoTraceOutput;
 
 import org.junit.Before;
 import org.junit.Test;
 import static org.junit.Assert.*;
 
 import static org.fest.reflect.core.Reflection.*;
 
 public class DataCollectionIntegTest extends CentralFixture {
 
     private TestTraceGenerator generator;
     private ZicoTraceOutput output;
 
 
     @Before
     public void setUpOutputAndCollector() throws Exception {
         zicoService.start();
 
         generator = new TestTraceGenerator();
         output = new ZicoTraceOutput(
                 new FressianTraceWriter(generator.getSymbols(), generator.getMetrics()),
                 "127.0.0.1", 8640, "test", "aaa");
     }
 
 
     private void submit(TraceRecord rec) {
         method("open").in(output).invoke();
         output.submit(rec);
         method("runCycle").in(output).invoke();
     }
 
 
     @Test(timeout = 1000)
     public void testCollectSingleTraceRecord() throws Exception {
         TraceRecord rec = generator.generate();
 
         submit(rec);
 
         assertEquals("One trace should be noticed.", 1, storeManager.get("test").getTraces().size());
     }
 
 
    @Test(timeout = 1000)
     public void testCollectTwoTraceRecords() throws Exception {
         submit(generator.generate());
         assertEquals("One trace should be noticed.", 1, storeManager.get("test").getTraces().size());
         submit(generator.generate());
        assertEquals("Two traces should be noticed.", 2, storeManager.get("test").getTraces().size());
     }
 
 
 
    @Test(timeout = 1000)
     public void testCollectBrokenTraceCausingNPE() throws Exception {
         TraceRecord rec = generator.generate();
         //rec.setMarker(null);
         rec.setFlags(0);
 
         submit(rec);
 
         assertEquals("Trace will not reach store.", 0, storeManager.get("test").getTraces().size());
 
         rec = generator.generate();
         submit(rec);
         assertEquals("TraceOutput should reconnect and send properly.", 1, storeManager.get("test").getTraces().size());
     }
 }
