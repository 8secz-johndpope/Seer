 /*
  * Copyright (c) 2012 the original author or authors.
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
 
 package org.openinfinity.cloud.domain;
 
 import java.io.InputStream;
 import java.io.Serializable;
 import java.sql.Timestamp;
 
 import lombok.Data;
 import lombok.NoArgsConstructor;
 
 import org.openinfinity.core.annotation.NotScript;
 
 /**
  * Deployment domain object
  *  
  * @author Ilkka Leinonen
  * @author Ossi Hämäläinen
  * @author Juha-Matti Sironen
  * @author Vedran Bartonicek
  * 
  * @version 1.0.0 Initial version
  * @since 1.0.0
  */
 
 @Data
 @NoArgsConstructor
 public class Deployment implements Serializable, Comparable<Deployment> {
 	
 	private static final long serialVersionUID = -5063856729744786978L;
 	
 	@NotScript
 	private int id;
 	@NotScript
 	private int state;
 	@NotScript
 	private int availabilityZone;
 	@NotScript
 	private long organizationId;
 	@NotScript
 	private int instanceId;
 	@NotScript
 	private int clusterId;
 	@NotScript
 	private String name;
 	@NotScript
 	private String location;
 	@NotScript
 	private InputStream inputStream;
 	@NotScript
 	private Timestamp deploymentTimestamp;
 	@NotScript
 	private String type;
 	
 	private Long localTimeStamp;
 
 	// FIXME: What is this?? -ILe
 	public Deployment (Deployment d){
 		id = d.getId();
 		state = d.getState();
 		organizationId = d.getOrganizationId();
 		instanceId = d.getInstanceId();
 		clusterId = d.getClusterId();
 		name = d.getName();
 		location = d.getLocation();
 		inputStream = d.getInputStream();	
 		deploymentTimestamp = d.getDeploymentTimestamp();
 	}
 
 	@Override
 	public int compareTo(Deployment o) {
 		return Long.valueOf(this.localTimeStamp).compareTo(Long.valueOf(o.localTimeStamp));
 	}
 }
