 /*
  * JBoss, Home of Professional Open Source
  * Copyright ${year}, Red Hat, Inc., and individual contributors
  * by the @authors tag. See the copyright.txt in the distribution for a
  * full listing of individual contributors.
  *
  * This is free software; you can redistribute it and/or modify it
  * under the terms of the GNU Lesser General Public License as
  * published by the Free Software Foundation; either version 2.1 of
  * the License, or (at your option) any later version.
  *
  * This software is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
  * Lesser General Public License for more details.
  *
  * You should have received a copy of the GNU Lesser General Public
  * License along with this software; if not, write to the Free
  * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
  * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
  */ 
 package org.jboss.seam.drools;
 
 import javax.enterprise.context.SessionScoped;
 import javax.enterprise.inject.Default;
 import javax.enterprise.inject.Produces;
 import javax.enterprise.inject.spi.InjectionPoint;
 
 import org.slf4j.Logger;
 import org.slf4j.LoggerFactory;
 
 import org.drools.core.util.debug.SessionInspector;
 import org.drools.core.util.debug.SessionReporter;
 import org.drools.core.util.debug.StatefulKnowledgeSessionInfo;
 import org.drools.runtime.StatefulKnowledgeSession;
 import org.jboss.seam.drools.qualifiers.Scanned;
 import org.jboss.seam.drools.qualifiers.SessionReport;
 
 /**
  * 
  * @author Tihomir Surdilovic
  */
 @SessionScoped
public class SessionReportProducer
 {
    private static final Logger log = LoggerFactory.getLogger(SessionReportProducer.class);
    
    @Produces
    @Default
    @SessionReport
    public SessionReportWrapper produceSessionReport(StatefulKnowledgeSession ksession, InjectionPoint ip) {
       return generate(ksession, ip.getAnnotated().getAnnotation(SessionReport.class).name(), ip.getAnnotated().getAnnotation(SessionReport.class).template());
    }
    
    @Produces
    @Scanned
    @SessionReport
    public SessionReportWrapper produceScannedSessionReport(@Scanned StatefulKnowledgeSession ksession, InjectionPoint ip) {
       return generate(ksession, ip.getAnnotated().getAnnotation(SessionReport.class).name(), ip.getAnnotated().getAnnotation(SessionReport.class).template());
    }
    
    private SessionReportWrapper generate(StatefulKnowledgeSession ksession, String name, String template) {
       if(name == null)
       {
          name = "simple";
       }
       SessionInspector inspector = new SessionInspector( ksession );
       StatefulKnowledgeSessionInfo info = inspector.getSessionInfo();
       if(template != null) 
       {
          SessionReporter.addNamedTemplate( name, getClass().getResourceAsStream( template ) );
       }
       
       SessionReportWrapper sessionReportWrapper = new SessionReportWrapper();
       sessionReportWrapper.setReport(SessionReporter.generateReport( name, info, null ));
       return sessionReportWrapper;
    }
    
 }
