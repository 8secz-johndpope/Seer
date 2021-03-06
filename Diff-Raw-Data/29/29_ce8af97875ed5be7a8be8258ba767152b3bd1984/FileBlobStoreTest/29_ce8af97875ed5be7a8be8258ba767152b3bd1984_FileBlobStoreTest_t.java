 /*
  * Licensed to the Apache Software Foundation (ASF) under one or more
  * contributor license agreements.  See the NOTICE file distributed with
  * this work for additional information regarding copyright ownership.
  * The ASF licenses this file to You under the Apache License, Version 2.0
  * (the "License"); you may not use this file except in compliance with
  * the License.  You may obtain a copy of the License at
  *
  *     http://www.apache.org/licenses/LICENSE-2.0
  *
  * Unless required by applicable law or agreed to in writing, software
  * distributed under the License is distributed on an "AS IS" BASIS,
  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  * See the License for the specific language governing permissions and
  * limitations under the License.
  */
 package org.apache.jackrabbit.mk.blobs;
 
import java.io.File;

import org.apache.commons.io.FileUtils;

 /**
  * Tests the FileBlobStore implementation.
  */
 public class FileBlobStoreTest extends AbstractBlobStoreTest {
 
    @Override
    public void setUp() {
         FileBlobStore store = new FileBlobStore("target/temp");
         store.setBlockSize(128);
         store.setBlockSizeMin(48);
         this.store = store;
     }
 
    @Override
    public void tearDown() throws Exception {
        store = null;
        FileUtils.deleteDirectory(new File("target/temp"));
    }

 }
