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
 package org.apache.cxf.dosgi.dsw.handlers;
 
 import java.util.Map;
 import java.util.logging.Level;
 import java.util.logging.Logger;
 
 import org.apache.cxf.aegis.databinding.AegisDatabinding;
 import org.apache.cxf.dosgi.dsw.Constants;
 import org.apache.cxf.dosgi.dsw.OsgiUtils;
 import org.apache.cxf.dosgi.dsw.service.CxfDistributionProvider;
 import org.apache.cxf.endpoint.Server;
 import org.apache.cxf.frontend.ClientProxyFactoryBean;
 import org.apache.cxf.frontend.ServerFactoryBean;
 import org.osgi.framework.BundleContext;
 import org.osgi.framework.ServiceReference;
 import org.osgi.service.discovery.ServiceEndpointDescription;
 
 public class PojoConfigurationTypeHandler extends AbstractPojoConfigurationTypeHandler {
     private static final Logger LOG = Logger.getLogger(PojoConfigurationTypeHandler.class.getName());
 
     public PojoConfigurationTypeHandler(BundleContext dswBC,
                                         CxfDistributionProvider dp, 
                                         Map<String, Object> handlerProps) {
         super(dswBC, dp, handlerProps);
     }
 
     public Object createProxy(ServiceReference serviceReference,
                               BundleContext dswContext, 
                               BundleContext callingContext,
                               Class<?> iClass, 
                               ServiceEndpointDescription sd)
                               throws IntentUnsatifiedException {
 
         String address = getPojoAddress(sd, iClass);
         if (address == null) {
             LOG.warning("Remote address is unavailable");
             return null;
         }
 
         LOG.info("Creating a " + sd.getProvidedInterfaces().toArray()[0]
                 + " client, endpoint address is " + address);
 
         try {
             ClientProxyFactoryBean factory = createClientProxyFactoryBean();
             factory.setServiceClass(iClass);
             factory.setAddress(address);
             factory.getServiceFactory().setDataBinding(new AegisDatabinding());
 
             applyIntents(dswContext, 
                          callingContext, 
                          factory.getFeatures(),
                          factory.getClientFactoryBean(), 
                          sd);
 
             Object proxy = getProxy(factory.create(), iClass);
             getDistributionProvider().addRemoteService(serviceReference);
             return proxy;
         } catch (Exception e) {
             LOG.log(Level.WARNING, "proxy creation failed", e);
         }
         return null;
     }
     
     public Server createServer(ServiceReference serviceReference,
                                BundleContext dswContext,
                                BundleContext callingContext, 
                                ServiceEndpointDescription sd, 
                                Class<?> iClass,
                                Object serviceBean) throws IntentUnsatifiedException {        
         String address = getPojoAddress(sd, iClass);
         if (address == null) {
             LOG.warning("Remote address is unavailable");
             return null;
         }
         
         LOG.info("Creating a " + sd.getProvidedInterfaces().toArray()[0]
             + " endpoint from CXF PublishHook, address is " + address);
         
         ServerFactoryBean factory = createServerFactoryBean();
         factory.setServiceClass(iClass);
         factory.setAddress(address);
         factory.getServiceFactory().setDataBinding(new AegisDatabinding());
         factory.setServiceBean(serviceBean);
         
         try {
             String [] intents = applyIntents(
                 dswContext, callingContext, factory.getFeatures(), factory, sd);
         
             Server server = factory.create();
             getDistributionProvider().addExposedService(serviceReference, registerPublication(server, intents));
             addAddressProperty(sd.getProperties(), address);
             return server;
         } catch (IntentUnsatifiedException iue) {
             getDistributionProvider().intentsUnsatisfied(serviceReference);
             throw iue;
         }
     }       
     
     @Override
     Map<String, String> registerPublication(Server server, String[] intents) {
         Map<String, String> publicationProperties = super.registerPublication(server, intents);
         publicationProperties.put(Constants.POJO_ADDRESS_PROPERTY, 
             server.getDestination().getAddress().getAddress().getValue());
 
         return publicationProperties;
     }
 
     private String getPojoAddress(ServiceEndpointDescription sd, Class<?> iClass) {
         String address = OsgiUtils.getProperty(sd, Constants.POJO_ADDRESS_PROPERTY);
         if (address == null) {
             address = getDefaultAddress(iClass);
             if (address != null) {
                 LOG.info("Using a default address : " + address);
             }
         }
         return address;
     }
     
 }
