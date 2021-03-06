 /*  Copyright 2007 Niclas Hedhman.
  *
  * Licensed under the Apache License, Version 2.0 (the "License");
  * you may not use this file except in compliance with the License.
  * You may obtain a copy of the License at
  *  
  * http://www.apache.org/licenses/LICENSE-2.0
  * 
  * Unless required by applicable law or agreed to in writing, software
  * distributed under the License is distributed on an "AS IS" BASIS,
  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
  * implied.
  * 
  * See the License for the specific language governing permissions and
  * limitations under the License.
  */
 package org.ops4j.pax.logging.internal;
 
 import org.osgi.framework.Bundle;
 import org.osgi.framework.BundleEvent;
 import org.osgi.framework.BundleListener;
 import org.osgi.framework.FrameworkEvent;
 import org.osgi.framework.FrameworkListener;
 import org.osgi.framework.ServiceEvent;
 import org.osgi.framework.ServiceListener;
 import org.osgi.framework.ServiceReference;
 import org.osgi.service.log.LogService;
 
 /**
  * One instance of this class will be instantiated to listen in on events generated by
  * the OSGi framework and log those.
  */
 public class FrameworkHandler
     implements BundleListener, FrameworkListener, ServiceListener
 {
	private static final String FRAMEWORK_EVENTS_LOG_LEVEL_PROP_NAME = "org.ops4j.pax.logging.service.frameworkEventsLogLevel";
 
     private PaxLoggingServiceImpl m_service;
    private int loggingLevel;
 
     public FrameworkHandler( PaxLoggingServiceImpl service )
     {
         m_service = service;

		loggingLevel = LogService.LOG_DEBUG;
		String frameworkEventsLogLevelProperty = System.getProperty(FRAMEWORK_EVENTS_LOG_LEVEL_PROP_NAME);
		if ( frameworkEventsLogLevelProperty != null )
		{
			try
			{
				loggingLevel = Integer.parseInt(frameworkEventsLogLevelProperty);
			}
			catch ( NumberFormatException e )
			{
				throw new IllegalArgumentException( "Failed to parse system property "
				    + FRAMEWORK_EVENTS_LOG_LEVEL_PROP_NAME + " (value " + frameworkEventsLogLevelProperty
					+ ").  Must be a valid level from the OSGi LogService." );
			}
		}
     }
 
     public void bundleChanged( BundleEvent bundleEvent )
     {
         Bundle bundle = bundleEvent.getBundle();
         String message;
         int type = bundleEvent.getType();
         switch( type )
         {
             case BundleEvent.INSTALLED:
                 message = "BundleEvent INSTALLED";
                 break;
             case BundleEvent.STARTED:
                 message = "BundleEvent STARTED";
                 break;
             case BundleEvent.STOPPED:
                 message = "BundleEvent STOPPED";
                 break;
             case BundleEvent.UPDATED:
                 message = "BundleEvent UPDATED";
                 break;
             case BundleEvent.UNINSTALLED:
                 message = "BundleEvent UNINSTALLED";
                 break;
             case BundleEvent.RESOLVED:
                 message = "BundleEvent RESOLVED";
                 break;
             case BundleEvent.UNRESOLVED:
                 message = "BundleEvent UNRESOLVED";
                 break;
             case BundleEvent.STARTING:
                 message = "BundleEvent STARTING";
                 break;
             case BundleEvent.STOPPING:
                 message = "BundleEvent STOPPING";
                 break;
             default:
                 message = "BundleEvent [unknown:" + type + "]";
                 break;
         }
        m_service.log( bundle, loggingLevel, message, null );
     }
 
     public void frameworkEvent( FrameworkEvent frameworkEvent )
     {
         int type = frameworkEvent.getType();
         String message;
         switch( type )
         {
             case FrameworkEvent.ERROR:
                 message = "FrameworkEvent ERROR";
                 break;
             case FrameworkEvent.INFO:
                 message = "FrameworkEvent INFO";
                 break;
             case FrameworkEvent.PACKAGES_REFRESHED:
                 message = "FrameworkEvent PACKAGES REFRESHED";
                 break;
             case FrameworkEvent.STARTED:
                 message = "FrameworkEvent STARTED";
                 break;
             case FrameworkEvent.STARTLEVEL_CHANGED:
                 message = "FrameworkEvent STARTLEVEL CHANGED";
                 break;
             case FrameworkEvent.WARNING:
                 message = "FrameworkEvent WARNING";
                 break;
             default:
                 message = "FrameworkEvent [unknown:" + type + "]";
                 break;
         }
         Bundle bundle = frameworkEvent.getBundle();
         Throwable exception = frameworkEvent.getThrowable();
        m_service.log( bundle, loggingLevel, message, exception );
     }
 
     public void serviceChanged( ServiceEvent serviceEvent )
     {
         ServiceReference serviceRef = serviceEvent.getServiceReference();
         String message;
         int type = serviceEvent.getType();
         switch( type )
         {
             case ServiceEvent.MODIFIED:
                 message = "ServiceEvent MODIFIED";
                 break;
             case ServiceEvent.REGISTERED:
                 message = "ServiceEvent REGISTERED";
                 break;
             case ServiceEvent.UNREGISTERING:
                 message = "ServiceEvent UNREGISTERING";
                 break;
             default:
                 message = "ServiceEvent [unknown:" + type + "]";
                 break;
         }
        m_service.log( serviceRef, loggingLevel, message );
     }
 }
