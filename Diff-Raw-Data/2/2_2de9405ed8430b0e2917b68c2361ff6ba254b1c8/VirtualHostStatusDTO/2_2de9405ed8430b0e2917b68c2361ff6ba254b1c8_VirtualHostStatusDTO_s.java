 /**
  * Licensed to the Apache Software Foundation (ASF) under one or more
  * contributor license agreements.  See the NOTICE file distributed with
  * this work for additional information regarding copyright ownership.
  * The ASF licenses this file to You under the Apache License, Version 2.0
  * (the "License"); you may not use this file except in compliance with
  * the License.  You may obtain a copy of the License at
  *
  *      http://www.apache.org/licenses/LICENSE-2.0
  *
  * Unless required by applicable law or agreed to in writing, software
  * distributed under the License is distributed on an "AS IS" BASIS,
  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  * See the License for the specific language governing permissions and
  * limitations under the License.
  */
 package org.apache.activemq.apollo.dto;
 
 import org.codehaus.jackson.annotate.JsonProperty;
 
 import javax.xml.bind.annotation.*;
 import java.util.ArrayList;
 import java.util.List;
 
 /**
  * <p>
  * </p>
  *
  * @author <a href="http://hiramchirino.com">Hiram Chirino</a>
  */
 @XmlRootElement(name="virtual_host_status")
 @XmlAccessorType(XmlAccessType.FIELD)
 public class VirtualHostStatusDTO extends ServiceStatusDTO {
 
     /**
      * Ids of all the topics that exist on the virtual host
      */
     @XmlElement(name="topic")
     public List<String> topics = new ArrayList<String>();
 
     /**
      * The number of topics that exist on the virtual host
      */
     @XmlAttribute(name="topic_count")
     public int topic_count;
 
     /**
      * Ids of all the queues that exist on the broker
      */
     @XmlElement(name="queue")
     public List<String> queues = new ArrayList<String>();
 
     /**
      * The number of queues that exist on the virtual host
      */
    @XmlAttribute(name="topic_count")
     public int queue_count;
 
     /**
      * Ids of all the durable subscriptions that exist on the broker
      */
     @XmlElement(name="dsub")
     public List<String> dsubs = new ArrayList<String>();
 
     /**
      * The number of durable subscriptions that exist on the virtual host
      */
     @XmlAttribute(name="dsub_count")
     public int dsub_count;
 
     /**
      * Is the virtual host using a store.
      */
     @XmlAttribute(name="store")
     public boolean store;
 
     /**
      * The host names route you to the the virtual host.
      */
     @XmlElement(name="host_names")
     public List<String> host_names = new ArrayList<String>();
 }
