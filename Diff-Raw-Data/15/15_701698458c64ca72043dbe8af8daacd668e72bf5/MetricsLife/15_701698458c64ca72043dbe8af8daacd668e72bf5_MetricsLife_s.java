 /**
  * Copyright (c) 2009-2013, rultor.com
  * All rights reserved.
  *
  * Redistribution and use in source and binary forms, with or without
  * modification, are permitted provided that the following conditions
  * are met: 1) Redistributions of source code must retain the above
  * copyright notice, this list of conditions and the following
  * disclaimer. 2) Redistributions in binary form must reproduce the above
  * copyright notice, this list of conditions and the following
  * disclaimer in the documentation and/or other materials provided
  * with the distribution. 3) Neither the name of the rultor.com nor
  * the names of its contributors may be used to endorse or promote
  * products derived from this software without specific prior written
  * permission.
  *
  * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
  * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT
  * NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
  * FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL
  * THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT,
  * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
  * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
  * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
  * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT,
  * STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
  * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
  * OF THE POSSIBILITY OF SUCH DAMAGE.
  */
 package com.rultor.life;
 
 import com.codahale.metrics.MetricRegistry;
 import com.codahale.metrics.Slf4jReporter;
 import com.codahale.metrics.health.HealthCheck;
 import com.codahale.metrics.health.HealthCheckRegistry;
 import com.codahale.metrics.servlets.HealthCheckServlet;
 import com.codahale.metrics.servlets.MetricsServlet;
 import com.jcabi.aspects.Loggable;
 import com.rultor.spi.Metricable;
 import java.util.Enumeration;
 import java.util.concurrent.TimeUnit;
 import javax.servlet.ServletContext;
 import javax.servlet.ServletContextEvent;
 import javax.servlet.ServletContextListener;
 import org.slf4j.LoggerFactory;
 
 /**
  * Metrics Framework Lifespan.
  *
  * @author Yegor Bugayenko (yegor@tpc2.com)
  * @version $Id$
  * @checkstyle ClassDataAbstractionCoupling (500 lines)
  */
 @Loggable(Loggable.INFO)
 public final class MetricsLife implements ServletContextListener {
 
     /**
      * Metrics registry.
      */
     private final transient MetricRegistry metrics =
         new MetricRegistry();
 
     /**
      * Health Registry.
      */
     private final transient HealthCheckRegistry health =
         new HealthCheckRegistry();
 
     /**
      * {@inheritDoc}
      */
     @Override
     public void contextInitialized(final ServletContextEvent event) {
         final ServletContext context = event.getServletContext();
         context.setAttribute(
             HealthCheckServlet.HEALTH_CHECK_REGISTRY,
             this.health
         );
         context.setAttribute(
             MetricsServlet.METRICS_REGISTRY,
             this.metrics
         );
         this.health.register(
             "runtime",
             new HealthCheck() {
                 @Override
                 protected HealthCheck.Result check() throws Exception {
                     return HealthCheck.Result.healthy(
                         new StringBuilder()
                             .append("Java version: ")
                             .append(System.getProperty("java.version"))
                             .toString()
                     );
                 }
             }
         );
         final Enumeration<?> attrs = context.getAttributeNames();
         while (attrs.hasMoreElements()) {
             final Object object = context.getAttribute(
                 attrs.nextElement().toString()
             );
             if (object instanceof Metricable) {
                 Metricable.class.cast(object).register(this.metrics);
             }
         }
         Slf4jReporter.forRegistry(this.metrics)
             .outputTo(LoggerFactory.getLogger(this.getClass().getName()))
             .convertRatesTo(TimeUnit.SECONDS)
             .convertDurationsTo(TimeUnit.MILLISECONDS)
             .build()
             .start(1, TimeUnit.MINUTES);
     }
 
     /**
      * {@inheritDoc}
      */
     @Override
     public void contextDestroyed(final ServletContextEvent event) {
         // nothing to do
     }
 
 }
