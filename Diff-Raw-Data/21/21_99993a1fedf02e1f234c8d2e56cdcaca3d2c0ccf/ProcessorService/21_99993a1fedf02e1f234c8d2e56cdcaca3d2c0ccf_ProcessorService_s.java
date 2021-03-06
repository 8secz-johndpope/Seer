 /**
  * Copyright 2013 Unicon (R) Licensed under the
  * Educational Community License, Version 2.0 (the "License"); you may
  * not use this file except in compliance with the License. You may
  * obtain a copy of the License at
  *
  * http://www.osedu.org/licenses/ECL-2.0
  *
  * Unless required by applicable law or agreed to in writing,
  * software distributed under the License is distributed on an "AS IS"
  * BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
  * or implied. See the License for the specific language governing
  * permissions and limitations under the License.
  */
 package org.apereo.oaa.services;
 
 import org.slf4j.Logger;
 import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
 
 /**
  * Handles the merging of the data inputs and processing in kettle with the selected PMML
  * 
  * @author Aaron Zeckoski (azeckoski @ unicon.net) (azeckoski @ vt.edu)
  */
@Service
 public class ProcessorService {
 
     private static final Logger logger = LoggerFactory.getLogger(ProcessorService.class);
 
     public void init() {
         // TODO load up config
         // TODO init kettle
     }
 
     public void process() {
         // TODO execute the processor
     }
 
 }
