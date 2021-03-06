 /*
  * Copyright 2011 Splunk, Inc.
  *
  * Licensed under the Apache License, Version 2.0 (the "License"): you may
  * not use this file except in compliance with the License. You may obtain
  * a copy of the License at
  *
  *     http://www.apache.org/licenses/LICENSE-2.0
  *
  * Unless required by applicable law or agreed to in writing, software
  * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
  * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
  * License for the specific language governing permissions and limitations
  * under the License.
  */
 
 package com.splunk;
 
 public class ScriptInput extends Input {
     ScriptInput(Service service, String path) {
         super(service, path);
     }
 
     public String getGroup() {
         return getString("group", null);
     }
 
     public String getHost() {
         return getString("host", null);
     }
 
     public String getIndex() {
         return getString("index");
     }
 
    public int getInteval() {
         return getInteger("interval");
     }
 
     public InputKind getKind() {
         return InputKind.Script;
     }
 
     public int getRcvBuf() {
         return getInteger("_rcvbuf");
     }
 }
