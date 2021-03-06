 /*
  * Copyright 2010-2013 Ning, Inc.
  *
  * Ning licenses this file to you under the Apache License, version 2.0
  * (the "License"); you may not use this file except in compliance with the
  * License.  You may obtain a copy of the License at:
  *
  *    http://www.apache.org/licenses/LICENSE-2.0
  *
  * Unless required by applicable law or agreed to in writing, software
  * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
  * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
  * License for the specific language governing permissions and limitations
  * under the License.
  */
 
 package org.killbill.billing.server.listeners;
 
 import java.lang.management.ManagementFactory;
 
 import javax.management.MBeanServer;
 import javax.servlet.ServletContext;
 import javax.servlet.ServletContextEvent;
 
 import org.killbill.billing.beatrix.lifecycle.DefaultLifecycle;
 import org.killbill.billing.jaxrs.resources.JaxRsResourceBase;
 import org.killbill.billing.jaxrs.util.KillbillEventHandler;
 import org.killbill.billing.server.config.KillbillServerConfig;
 import org.killbill.billing.server.healthchecks.KillbillHealthcheck;
 import org.killbill.billing.server.modules.KillbillServerModule;
 import org.killbill.billing.server.security.TenantFilter;
 import org.killbill.billing.util.jackson.ObjectMapper;
 import org.killbill.billing.util.svcsapi.bus.BusService;
 import org.killbill.bus.api.PersistentBus;
 import org.killbill.commons.skeleton.listeners.GuiceServletContextListener;
 import org.killbill.commons.skeleton.modules.BaseServerModuleBuilder;
 import org.killbill.commons.skeleton.modules.ConfigModule;
 import org.killbill.commons.skeleton.modules.JMXModule;
 import org.killbill.commons.skeleton.modules.JaxrsJacksonModule;
 import org.killbill.commons.skeleton.modules.StatsModule;
 import org.killbill.notificationq.api.NotificationQueueService;
 import org.skife.config.ConfigurationObjectFactory;
 import org.slf4j.Logger;
 import org.slf4j.LoggerFactory;
 
 import com.codahale.metrics.MetricRegistry;
 import com.codahale.metrics.health.HealthCheckRegistry;
 import com.codahale.metrics.servlets.HealthCheckServlet;
 import com.codahale.metrics.servlets.MetricsServlet;
 import com.google.common.annotations.VisibleForTesting;
 import com.google.common.collect.ImmutableList;
 import com.google.inject.Injector;
 import com.google.inject.Module;
 import net.sf.ehcache.CacheManager;
 import net.sf.ehcache.management.ManagementService;
 
 public class KillbillGuiceListener extends GuiceServletContextListener {
 
     public static final Logger logger = LoggerFactory.getLogger(KillbillGuiceListener.class);
 
     private KillbillServerConfig config;
     private Injector injector;
     private DefaultLifecycle killbillLifecycle;
     private BusService killbillBusService;
     private KillbillEventHandler killbilleventHandler;
 
     protected Module getModule(final ServletContext servletContext) {
         return new KillbillServerModule(servletContext, config.isTestModeEnabled());
     }
 
     private void registerMBeansForCache(final CacheManager cacheManager) {
         if (cacheManager != null) {
             final MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
             ManagementService.registerMBeans(cacheManager, mBeanServer, false, true, true, true);
         }
     }
 
     @Override
     public void contextInitialized(final ServletContextEvent event) {
         config = new ConfigurationObjectFactory(System.getProperties()).build(KillbillServerConfig.class);
 
         // Don't filter all requests through Jersey, only the JAX-RS APIs (otherwise,
         // things like static resources, favicon, etc. are 404'ed)
         final BaseServerModuleBuilder builder = new BaseServerModuleBuilder().setJaxrsUriPattern("(" + JaxRsResourceBase.PREFIX + "|" + JaxRsResourceBase.PLUGINS_PATH + ")" + "/.*")
                                                                             .addJaxrsResource("com.ning.billing.jaxrs.mappers")
                                                                             .addJaxrsResource("com.ning.billing.jaxrs.resources");
 
         if (config.isMultiTenancyEnabled()) {
             builder.addFilter("/*", TenantFilter.class);
         }
 
         guiceModules = ImmutableList.<Module>of(builder.build(),
                                                 new ConfigModule(KillbillServerConfig.class),
                                                 new JaxrsJacksonModule(new ObjectMapper()),
                                                 new JMXModule(KillbillHealthcheck.class, NotificationQueueService.class, PersistentBus.class),
                                                 new StatsModule(KillbillHealthcheck.class),
                                                 getModule(event.getServletContext()));
 
         super.contextInitialized(event);
 
         logger.info("KillbillLifecycleListener : contextInitialized");
 
         injector = injector(event);
         event.getServletContext().setAttribute(Injector.class.getName(), injector);
 
         // Metrics initialization
         event.getServletContext().setAttribute(HealthCheckServlet.HEALTH_CHECK_REGISTRY, injector.getInstance(HealthCheckRegistry.class));
         event.getServletContext().setAttribute(MetricsServlet.METRICS_REGISTRY, injector.getInstance(MetricRegistry.class));
 
         killbillLifecycle = injector.getInstance(DefaultLifecycle.class);
         killbillBusService = injector.getInstance(BusService.class);
         killbilleventHandler = injector.getInstance(KillbillEventHandler.class);
 
         registerMBeansForCache(injector.getInstance(CacheManager.class));
 
         //
         // Fire all Startup levels up to service start
         //
         killbillLifecycle.fireStartupSequencePriorEventRegistration();
         //
         // Perform Bus registration
         //
         try {
             killbillBusService.getBus().register(killbilleventHandler);
         } catch (PersistentBus.EventBusException e) {
             logger.error("Failed to register for event notifications, this is bad exiting!", e);
             System.exit(1);
         }
         // Let's start!
         killbillLifecycle.fireStartupSequencePostEventRegistration();
     }
 
     @Override
     public void contextDestroyed(final ServletContextEvent sce) {
         super.contextDestroyed(sce);
 
         logger.info("IrsKillbillListener : contextDestroyed");
         // Stop services
         // Guice error, no need to fill the screen with useless stack traces
         if (killbillLifecycle == null) {
             return;
         }
 
         killbillLifecycle.fireShutdownSequencePriorEventUnRegistration();
 
         try {
             killbillBusService.getBus().unregister(killbilleventHandler);
         } catch (PersistentBus.EventBusException e) {
             logger.warn("Failed to unregister for event notifications", e);
         }
 
         // Complete shutdown sequence
         killbillLifecycle.fireShutdownSequencePostEventUnRegistration();
     }
 
     @VisibleForTesting
     public Injector getInstantiatedInjector() {
         return injector;
     }
 }
