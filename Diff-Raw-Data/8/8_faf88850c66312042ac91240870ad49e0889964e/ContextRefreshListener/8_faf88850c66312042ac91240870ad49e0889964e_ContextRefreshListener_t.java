 /**
  * The contents of this file are subject to the OpenMRS Public License
  * Version 1.0 (the "License"); you may not use this file except in
  * compliance with the License. You may obtain a copy of the License at
  * http://license.openmrs.org
  *
  * Software distributed under the License is distributed on an "AS IS"
  * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
  * License for the specific language governing rights and limitations
  * under the License.
  *
  * Copyright (C) OpenMRS, LLC.  All Rights Reserved.
  */
 
 package org.openmrs.module.jmx.util;
 
 import org.apache.commons.logging.Log;
 import org.apache.commons.logging.LogFactory;
import org.openmrs.api.context.Context;
 import org.openmrs.module.jmx.JMXContext;
 import org.springframework.context.ApplicationListener;
 import org.springframework.context.event.ContextRefreshedEvent;
 
 /**
  * Listener to notify us when application context is refreshed... which should
  * correspond to when modules are loaded or unloaded
  */
 public class ContextRefreshListener implements ApplicationListener<ContextRefreshedEvent> {
 
 	protected Log log = LogFactory.getLog(ContextRefreshListener.class);
 			
 	@Override
 	public void onApplicationEvent(ContextRefreshedEvent event) {
		log.debug("Application context refreshed (session: " + Context.isSessionOpen() + ")");
		
 		// Refresh the management beans
		if (Context.isSessionOpen())
			JMXContext.refresh();
 		
 		log.debug("Refreshed management beans due to application context refresh");
 	}
 }
