 /**
  * Licensed to the Apache Software Foundation (ASF) under one
  * or more contributor license agreements. See the NOTICE file
  * distributed with this work for additional information
  * regarding copyright ownership. The ASF licenses this file
  * to you under the Apache License, Version 2.0 (the
  * "License"); you may not use this file except in compliance
  * with the License. You may obtain a copy of the License at
  *
  * http://www.apache.org/licenses/LICENSE-2.0
  *
  * Unless required by applicable law or agreed to in writing,
  * software distributed under the License is distributed on an
  * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
  * KIND, either express or implied. See the License for the
  * specific language governing permissions and limitations
  * under the License.
  */
 package org.apache.cxf.dosgi.dsw;
 
 import java.util.ArrayList;
 import java.util.Dictionary;
 import java.util.Enumeration;
 import java.util.HashMap;
 import java.util.Hashtable;
 import java.util.List;
 import java.util.Map;
 
 import org.apache.cxf.Bus;
 import org.apache.cxf.BusFactory;
 import org.apache.cxf.dosgi.dsw.decorator.ServiceDecorator;
 import org.apache.cxf.dosgi.dsw.decorator.ServiceDecoratorImpl;
 import org.apache.cxf.dosgi.dsw.handlers.ConfigTypeHandlerFactory;
 import org.apache.cxf.dosgi.dsw.handlers.HttpServiceManager;
 import org.apache.cxf.dosgi.dsw.qos.DefaultIntentMapFactory;
 import org.apache.cxf.dosgi.dsw.qos.IntentManager;
 import org.apache.cxf.dosgi.dsw.qos.IntentManagerImpl;
 import org.apache.cxf.dosgi.dsw.qos.IntentMap;
 import org.apache.cxf.dosgi.dsw.qos.IntentTracker;
 import org.apache.cxf.dosgi.dsw.service.RemoteServiceAdminCore;
 import org.apache.cxf.dosgi.dsw.service.RemoteServiceadminFactory;
 import org.osgi.framework.BundleActivator;
 import org.osgi.framework.BundleContext;
 import org.osgi.framework.Constants;
 import org.osgi.framework.ServiceRegistration;
 import org.osgi.service.cm.ConfigurationException;
 import org.osgi.service.cm.ManagedService;
 import org.osgi.service.remoteserviceadmin.RemoteServiceAdmin;
 import org.slf4j.Logger;
 import org.slf4j.LoggerFactory;
 
 // registered as spring bean -> start / stop called accordingly
 public class Activator implements ManagedService, BundleActivator {
     private static final int DEFAULT_INTENT_TIMEOUT = 30000;
     private static final Logger LOG = LoggerFactory.getLogger(Activator.class);
     private static final String CONFIG_SERVICE_PID = "cxf-dsw";
     private ServiceRegistration rsaFactoryReg;
     private ServiceRegistration decoratorReg;
     private IntentTracker intentTracker;
     private BundleContext bc;
 
     public void start(BundleContext bundlecontext) throws Exception {
         LOG.debug("RemoteServiceAdmin Implementation is starting up");
         this.bc = bundlecontext;
         // Disable the fast infoset as it's not compatible (yet) with OSGi
         System.setProperty("org.apache.cxf.nofastinfoset", "true");
         init(new Hashtable<String, Object>());
         registerManagedService(bc);
     }
 
     private synchronized void init(Map<String, Object> config) {
         String httpBase = (String) config.get(org.apache.cxf.dosgi.dsw.Constants.HTTP_BASE);
        String cxfServletAlias = (String) config.get(org.apache.cxf.dosgi.dsw.Constants.CXF_SERVLET_ALIAS);
 
         IntentMap intentMap = new IntentMap(new DefaultIntentMapFactory().create());
         intentTracker = new IntentTracker(bc, intentMap);
         intentTracker.open();
         IntentManager intentManager = new IntentManagerImpl(intentMap, DEFAULT_INTENT_TIMEOUT);
        HttpServiceManager httpServiceManager = new HttpServiceManager(bc, httpBase, cxfServletAlias);
         ConfigTypeHandlerFactory configTypeHandlerFactory
             = new ConfigTypeHandlerFactory(bc, intentManager, httpServiceManager);
         RemoteServiceAdminCore rsaCore = new RemoteServiceAdminCore(bc, configTypeHandlerFactory);
         RemoteServiceadminFactory rsaf = new RemoteServiceadminFactory(rsaCore);
         Dictionary<String, Object> props = new Hashtable<String, Object>();
         String[] supportedIntents = intentMap.keySet().toArray(new String[] {});
         props.put("remote.intents.supported", supportedIntents);
         props.put("remote.configs.supported", obtainSupportedConfigTypes(configTypeHandlerFactory.getSupportedConfigurationTypes()));
         LOG.info("Registering RemoteServiceAdminFactory...");
         rsaFactoryReg = bc.registerService(RemoteServiceAdmin.class.getName(), rsaf, props);
         decoratorReg = bc.registerService(ServiceDecorator.class.getName(), new ServiceDecoratorImpl(bc), null);
     }
 
     // The CT sometimes uses the first element returned to register a service, but does not provide any additional configuration
     // Return the configuration type that works without additional configuration as the first in the list.
     private String[] obtainSupportedConfigTypes(List<String> types) {
         List<String> l = new ArrayList<String>(types);
         if (l.contains(org.apache.cxf.dosgi.dsw.Constants.WS_CONFIG_TYPE)) {
             // make sure its the first element...
             l.remove(org.apache.cxf.dosgi.dsw.Constants.WS_CONFIG_TYPE);
             l.add(0, org.apache.cxf.dosgi.dsw.Constants.WS_CONFIG_TYPE);
         }
         return l.toArray(new String[] {});
     }
 
     private void registerManagedService(BundleContext bundlecontext) {
         Dictionary<String, String> props = new Hashtable<String, String>();
         props.put(Constants.SERVICE_PID, CONFIG_SERVICE_PID);
         // No need to store the registration. Will be unregistered in stop by framework
         bundlecontext.registerService(ManagedService.class.getName(), this, props);
     }
 
     public void stop(BundleContext context) throws Exception {
         LOG.debug("RemoteServiceAdmin Implementation is shutting down now");
         uninit();
         shutdownCXFBus();
     }
 
     private synchronized void uninit() {
         intentTracker.close();
         // This also triggers the unimport and unexport of the remote services
         rsaFactoryReg.unregister();
         decoratorReg.unregister();
         intentTracker = null;
         rsaFactoryReg = null;
         decoratorReg = null;
     }
 
     /**
      * Causes also the shutdown of the embedded HTTP server
      */
     private void shutdownCXFBus() {
         Bus b = BusFactory.getDefaultBus();
         if (b != null) {
             LOG.debug("Shutting down the CXF Bus");
             b.shutdown(true);
         }
     }
 
     @SuppressWarnings({ "rawtypes", "unchecked" })
     public synchronized void updated(Dictionary config) throws ConfigurationException {
         LOG.debug("RemoteServiceAdmin Implementation configuration is updated with {}", config);
         if (config != null) {
             try {
                 uninit();
             } catch (Exception e) {
                 LOG.error(e.getMessage(), e);
             }
             init(getMapFromDictionary(config));
         }
     }
 
     private Map<String, Object> getMapFromDictionary(Dictionary<String, Object> config) {
         Map<String, Object> configMap = new HashMap<String, Object>();
         if (config == null) {
             return configMap;
         }
         Enumeration<String> keys = config.keys();
         while (keys.hasMoreElements()) {
             String key = keys.nextElement();
             configMap.put(key, config.get(key));
         }
         return configMap;
     }
 }
