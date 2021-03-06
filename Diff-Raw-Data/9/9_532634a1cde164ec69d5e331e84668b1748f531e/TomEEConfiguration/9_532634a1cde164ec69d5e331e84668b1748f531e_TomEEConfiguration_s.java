 /**
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
 package org.apache.openejb.arquillian;
 
 import org.jboss.arquillian.spi.ConfigurationException;
 import org.jboss.arquillian.spi.client.container.ContainerConfiguration;
 
 public class TomEEConfiguration implements ContainerConfiguration {
 
 	private int httpPort = 8080;
 	private int stopPort = 8005;
 	private String stopCommand = "SHUTDOWN";
 	private String dir = System.getProperty("java.io.tmpdir") + "/arquillian-apache-tomee";
	private String openejbPath = "classpath:/openejb-tomcat-webapp-4.0.0-SNAPSHOT.war";
 	private int timeout = 30;
 	
 	public int getHttpPort() {
 		return httpPort;
 	}
 
 	public void setHttpPort(int httpPort) {
 		this.httpPort = httpPort;
 	}
 
 	public int getStopPort() {
 		return stopPort;
 	}
 
 	public void setStopPort(int stopPort) {
 		this.stopPort = stopPort;
 	}
 
 	public String getStopCommand() {
 		return stopCommand;
 	}
 
 	public void setStopCommand(String stopCommand) {
 		this.stopCommand = stopCommand;
 	}
 
 	public String getDir() {
 		return dir;
 	}
 
 	public void setDir(String dir) {
 		this.dir = dir;
 	}
 
 	public String getOpenejbPath() {
 		return openejbPath;
 	}
 
 	public void setOpenejbPath(String openejbPath) {
 		this.openejbPath = openejbPath;
 	}
 
 	public void validate() throws ConfigurationException {
 	}
 
 	public int getTimeOut() {
 		return timeout;
 	}
 
 	public int getTimeout() {
 		return timeout;
 	}
 
 	public void setTimeout(int timeout) {
 		this.timeout = timeout;
 	}
 }
