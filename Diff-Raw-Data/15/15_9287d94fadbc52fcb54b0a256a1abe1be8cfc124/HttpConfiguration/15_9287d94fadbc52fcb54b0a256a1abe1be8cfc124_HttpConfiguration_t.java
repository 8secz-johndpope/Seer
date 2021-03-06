 /*
  * Copyright 2005-2006 The Apache Software Foundation.
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
 package org.apache.servicemix.http;
 
 import org.apache.servicemix.common.PersistentConfiguration;
 
 public class HttpConfiguration extends PersistentConfiguration implements HttpConfigurationMBean {
 
    public static final String DEFAULT_JETTY_CONNECTOR_CLASS_NAME = org.mortbay.jetty.bio.SocketConnector.class.getName();
    
     private boolean streamingEnabled = false;
    private String jettyConnectorClassName = DEFAULT_JETTY_CONNECTOR_CLASS_NAME;
 
     /**
      * The maximum number of threads for the Jetty thread pool. It's set 
      * to 255 by default to match the default value in Jetty. 
      */
     private int jettyThreadPoolSize = 255;
 
     public boolean isStreamingEnabled() {
         return streamingEnabled;
     }
 
     public void setStreamingEnabled(boolean streamingEnabled) {
         this.streamingEnabled = streamingEnabled;
         save();
     }
 
     public String getJettyConnectorClassName() {
         return jettyConnectorClassName;
     }
 
     public void setJettyConnectorClassName(String jettyConnectorClassName) {
         this.jettyConnectorClassName = jettyConnectorClassName;
         save();
     }
 
     public int getJettyThreadPoolSize() {
         return jettyThreadPoolSize;
     }
 
     public void setJettyThreadPoolSize(int jettyThreadPoolSize) {
         this.jettyThreadPoolSize = jettyThreadPoolSize;
         save();
     }
     
     public void save() {
         properties.setProperty("jettyThreadPoolSize", Integer.toString(jettyThreadPoolSize));
         properties.setProperty("jettyConnectorClassName", jettyConnectorClassName);
         properties.setProperty("streamingEnabled", Boolean.toString(streamingEnabled));
         super.save();
     }
     
     public boolean load() {
         if (super.load()) {
             if (properties.getProperty("jettyThreadPoolSize") != null) {
                 jettyThreadPoolSize = Integer.parseInt(properties.getProperty("jettyThreadPoolSize"));
             }
             if (properties.getProperty("jettyConnectorClassName") != null) {
                 jettyConnectorClassName = properties.getProperty("jettyConnectorClassName");
             }
             if (properties.getProperty("streamingEnabled") != null) {
                streamingEnabled = Boolean.valueOf(properties.getProperty("streamingEnabled")).booleanValue();
             }
             return true;
         } else {
             return false;
         }
     }
     
 }
