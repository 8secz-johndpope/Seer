 /**
  * Licensed to the Apache Software Foundation (ASF) under one
  * or more contributor license agreements.  See the NOTICE file
  * distributed with this work for additional information
  * regarding copyright ownership.  The ASF licenses this file
  * to you under the Apache License, Version 2.0 (the
  * "License"); you may not use this file except in compliance
  * with the License.  You may obtain a copy of the License at
  *
  *     http://www.apache.org/licenses/LICENSE-2.0
  *
  * Unless required by applicable law or agreed to in writing, software
  * distributed under the License is distributed on an "AS IS" BASIS,
  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  * See the License for the specific language governing permissions and
  * limitations under the License.
  */
 
 package org.apache.jena.tdbloader3;
 
 import static org.junit.Assert.assertEquals;
 import static org.junit.Assert.assertTrue;
 
 import java.io.IOException;
 
 import org.apache.hadoop.util.ToolRunner;
 import org.junit.AfterClass;
 import org.junit.BeforeClass;
 import org.junit.Test;
 
 import cmd.tdbloader3;
 
 import com.hp.hpl.jena.tdb.base.file.Location;
 import com.hp.hpl.jena.tdb.store.DatasetGraphTDB;
 import com.hp.hpl.jena.tdb.sys.SetupTDB;
 
 public class TestTDBLoader3MiniMRCluster extends AbstractMiniMRClusterTest {
 
     @BeforeClass public static void before() throws IOException {
     	startCluster() ;
     }
     
    @AfterClass public static void after() {
     	stopCluster() ;
     }
 	
     @Test public void test() throws Exception {
         String input = "src/test/resources/input" ;
         String output = "target/output" ;
         String[] args = new String[] {
                 "-conf", config, 
                 "-D", "overrideOutput=true", 
                 "-D", "copyToLocal=true", 
                 "-D", "verify=true", 
                 "-D", "runLocal=false",
                 "-D", "numReducers=3", 
                 "-D", "numSamples=40", 
                 input, 
                 output
         };
         assertEquals ( 0, ToolRunner.run(new tdbloader3(), args) );
         DatasetGraphTDB dsgMem = tdbloader3.load(input);
         DatasetGraphTDB dsgDisk = SetupTDB.buildDataset(new Location(output)) ;
         assertTrue ( tdbloader3.dump(dsgMem, dsgDisk), tdbloader3.isomorphic ( dsgMem, dsgDisk ) );       
     }
 
 }
