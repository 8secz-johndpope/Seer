 /*
  * Copyright 2005-2009 Niclas Hedhman.
  *
  * Licensed  under the  Apache License,  Version 2.0  (the "License");
  * you may not use  this file  except in  compliance with the License.
  * You may obtain a copy of the License at
  *
  *   http://www.apache.org/licenses/LICENSE-2.0
  *
  * Unless required by applicable law or agreed to in writing, software
  * distributed  under the  License is distributed on an "AS IS" BASIS,
  * WITHOUT  WARRANTIES OR CONDITIONS  OF ANY KIND, either  express  or
  * implied.
  *
  * See the License for the specific language governing permissions and
  * limitations under the License.
  */
 package org.ops4j.pax.logging.service.internal;
 
 import java.util.Dictionary;
 import java.util.Enumeration;
 import java.util.Iterator;
 import java.util.Map;
 import java.util.Properties;
 import java.util.logging.Level;
 import org.apache.log4j.LogManager;
 import org.apache.log4j.Logger;
 import org.apache.log4j.PaxLoggingConfigurator;
 import org.knopflerfish.service.log.LogService;
 import org.ops4j.pax.logging.EventAdminPoster;
 import org.ops4j.pax.logging.PaxContext;
 import org.ops4j.pax.logging.PaxLogger;
 import org.ops4j.pax.logging.PaxLoggingService;
 import org.osgi.framework.Bundle;
 import org.osgi.framework.BundleContext;
 import org.osgi.framework.ServiceFactory;
 import org.osgi.framework.ServiceReference;
 import org.osgi.framework.ServiceRegistration;
 import org.osgi.service.cm.ConfigurationException;
 import org.osgi.service.cm.ManagedService;
 import org.osgi.service.log.LogEntry;
 
 public class PaxLoggingServiceImpl
     implements PaxLoggingService, LogService, ManagedService, ServiceFactory
 {
 
     private LogReaderServiceImpl m_logReader;
     private EventAdminPoster m_eventAdmin;
     private BundleContext m_bundleContext;
     private AppenderTracker m_appenderTracker;
     private PaxContext m_context;
 
     private int m_logLevel = LOG_DEBUG;
     private static final String DEFAULT_SERVICE_LOG_LEVEL = "org.ops4j.pax.logging.DefaultServiceLog.level";
 
     public PaxLoggingServiceImpl( BundleContext context, LogReaderServiceImpl logReader, EventAdminPoster eventAdmin,
                                   AppenderTracker appenderTracker
     )
     {
         m_bundleContext = context;
         m_appenderTracker = appenderTracker;
         m_logReader = logReader;
         m_eventAdmin = eventAdmin;
         m_context = new PaxContext();
         configureDefaults();
     }
 
     public PaxLogger getLogger( Bundle bundle, String category, String fqcn )
     {
 
         Logger log4jLogger;
         if( category == null )
         {
             // Anonymous Logger in JDK Util Logging will have a category of null.
             log4jLogger = Logger.getRootLogger();
         }
         else
         {
             log4jLogger = Logger.getLogger( category );
         }
         return new PaxLoggerImpl( bundle, log4jLogger, fqcn, this );
     }
 
     public int getLogLevel()
     {
         return m_logLevel;
     }
 
     public void log( int level, String message )
     {
         log( level, message, null );
     }
 
     public void log( int level, String message, Throwable exception )
     {
         log( (ServiceReference) null, level, message, exception );
     }
 
     public void log( ServiceReference sr, int level, String message )
     {
         log( sr, level, message, null );
     }
 
     public void log( ServiceReference sr, int level, String message, Throwable exception )
     {
         log( null, sr, level, message, exception );
     }
 
     /**
      * This method is used by the FrameworkHandler to log framework events.
      *
      * @param bundle    The bundle that caused the event.
      * @param level     The level to be logged as.
      * @param message   The message.
      * @param exception The exception, if any otherwise null.
      */
     void log( Bundle bundle, int level, String message, Throwable exception )
     {
         log( bundle, null, level, message, exception );
     }
 
     private void log( Bundle bundle, ServiceReference sr, int level, String message, Throwable exception )
     {
         // failsafe in case bundle is null
         if( null == bundle && null != sr )
         {
             bundle = sr.getBundle();
         }
 
         String category = "[undefined]";
         if( bundle != null )
         {
             category = bundle.getSymbolicName();
             if( null == category )
             {
                 category = "[bundle@" + bundle.getBundleId() + ']';
             }
         }
 
         PaxLogger logger = getLogger( bundle, category, "" );
         switch( level )
         {
         case LOG_ERROR:
             logger.error( message, exception );
             break;
         case LOG_WARNING:
             logger.warn( message, exception );
             break;
         case LOG_INFO:
             logger.inform( message, exception );
             break;
         case LOG_DEBUG:
             logger.debug( message, exception );
             break;
         default:
             logger.warn( "Undefined Level: " + level + " : " + message, exception );
         }
         handleEvents( bundle, sr, level, message, exception );
     }
 
     void handleEvents( Bundle bundle, ServiceReference sr, int level, String message, Throwable exception )
     {
         LogEntry entry = new LogEntryImpl( bundle, sr, level, message, exception );
         m_logReader.fireEvent( entry );
 
         // This should only be null for TestCases.
         if( m_eventAdmin != null )
         {
             m_eventAdmin.postEvent( bundle, level, entry, message, exception, sr, getPaxContext().getContext() );
         }
     }
 
     public void updated( Dictionary configuration )
         throws ConfigurationException
     {
         if( configuration == null )
         {
             configureDefaults();
             return;
         }
         Properties extracted = extractKeys( configuration );
 
         // If the updated() method is called without any log4j properties,
         // then keep the default/previous configuration.
         if( extracted.size() == 0 )
         {
             configureDefaults();
             return;
         }
         PaxLoggingConfigurator configurator = new PaxLoggingConfigurator( m_appenderTracker );
        LogManager.resetConfiguration();
         configurator.doConfigure( extracted, LogManager.getLoggerRepository() );
         setLevelToJavaLogging( configuration );
     }
 
     private Properties extractKeys( Dictionary configuration )
     {
         Properties extracted = new Properties();
         Enumeration list = configuration.keys();
         while( list.hasMoreElements() )
         {
             Object obj = list.nextElement();
             if( obj instanceof String )
             {
                 extractKey( extracted, configuration, obj );
             }
         }
         return extracted;
     }
 
     private void extractKey( Properties extracted, Dictionary configuration, Object obj )
     {
         String key = (String) obj;
         Object value = configuration.get( obj );
         if( key.startsWith( "log4j" ) )
         {
             extracted.put( key, value );
         }
         else if( key.startsWith( "pax." ) )
         {
             if( "pax.logging.entries.size".equals( key ) )
             {
                 try
                 {
                     m_logReader.setMaxEntries( Integer.parseInt( (String) value ) );
                 }
                 catch( Exception e )
                 {
                     e.printStackTrace();
                 }
             }
         }
     }
 
     private void configureDefaults()
     {
         String levelName;
         if( m_bundleContext == null )
         {
             levelName = System.getProperty( DEFAULT_SERVICE_LOG_LEVEL, "DEBUG" ).trim();
         }
         else
         {
             levelName = m_bundleContext.getProperty( DEFAULT_SERVICE_LOG_LEVEL );
             if( levelName == null )
             {
                 levelName = "DEBUG";
             }
             else
             {
                 levelName = levelName.trim();
             }
         }
         m_logLevel = convertLevel( levelName );
 
         PaxLoggingConfigurator configurator = new PaxLoggingConfigurator( m_appenderTracker );
         Properties defaultProperties = new Properties();
         // Extract System Properties prefixed with "pax.log4j", and drop the "pax." and include these
         extractSystemProperties( defaultProperties );
         defaultProperties.put( "log4j.rootLogger", convertLevel( m_logLevel ) + ", A1" );
         defaultProperties.put( "log4j.appender.A1", "org.apache.log4j.ConsoleAppender" );
         defaultProperties.put( "log4j.appender.A1.layout", "org.apache.log4j.TTCCLayout" );
         configurator.doConfigure( defaultProperties, LogManager.getLoggerRepository() );
         final java.util.logging.Logger rootLogger = java.util.logging.Logger.getLogger( "" );
         rootLogger.setLevel( Level.FINE );
     }
 
     private void extractSystemProperties( Properties output )
     {
         Iterator list = System.getProperties().entrySet().iterator();
         while( list.hasNext() )
         {
             Map.Entry entry = (Map.Entry) list.next();
             String key = (String) entry.getKey();
             String value = (String) entry.getValue();
             if( key.startsWith( "pax.log4j" ) )
             {
                 key = key.substring( 4 );
                 output.put( key, value );
             }
         }
     }
 
     /*
      * use local class to delegate calls to underlying instance while keeping bundle reference
      */
     public Object getService( final Bundle bundle, ServiceRegistration registration )
     {
         class ManagedPaxLoggingService
             implements PaxLoggingService, LogService, ManagedService
         {
 
             public void log( int level, String message )
             {
                 PaxLoggingServiceImpl.this.log( bundle, null, level, message, null );
             }
 
             public void log( int level, String message, Throwable exception )
             {
                 PaxLoggingServiceImpl.this.log( bundle, null, level, message, exception );
             }
 
             public void log( ServiceReference sr, int level, String message )
             {
                 PaxLoggingServiceImpl.this.log( bundle, sr, level, message, null );
             }
 
             public void log( ServiceReference sr, int level, String message, Throwable exception )
             {
                 PaxLoggingServiceImpl.this.log( bundle, sr, level, message, exception );
             }
 
             public int getLogLevel()
             {
                 return PaxLoggingServiceImpl.this.getLogLevel();
             }
 
             public PaxLogger getLogger( Bundle myBundle, String category, String fqcn )
             {
                 return PaxLoggingServiceImpl.this.getLogger( myBundle, category, fqcn );
             }
 
             public void updated( Dictionary configuration )
                 throws ConfigurationException
             {
                 PaxLoggingServiceImpl.this.updated( configuration );
             }
 
             public PaxContext getPaxContext()
             {
                 return PaxLoggingServiceImpl.this.getPaxContext();
             }
         }
 
         return new ManagedPaxLoggingService();
     }
 
     public void ungetService( Bundle bundle, ServiceRegistration registration, Object service )
     {
         // nothing to do...
     }
 
     public PaxContext getPaxContext()
     {
         return m_context;
     }
 
     private static int convertLevel( String levelName )
     {
         if( "DEBUG".equals( levelName ) )
         {
             return LOG_DEBUG;
         }
         else if( "INFO".equals( levelName ) )
         {
             return LOG_INFO;
         }
         else if( "ERROR".equals( levelName ) )
         {
             return LOG_ERROR;
         }
         else if( "WARN".equals( levelName ) )
         {
             return LOG_WARNING;
         }
         else
         {
             return LOG_DEBUG;
         }
     }
 
     private static String convertLevel( int level )
     {
         switch( level )
         {
         case LOG_DEBUG:
             return "DEBUG";
         case LOG_INFO:
             return "INFO";
         case LOG_ERROR:
             return "ERROR";
         case LOG_WARNING:
             return "WARN";
         default:
             return "DEBUG";
         }
     }
 
     //Here are added methods for setting level to root logger of the Java Logging API
 
     private static void setLevelToJavaLogging( final Dictionary configuration )
     {
         String levelProperty = null;
         final Enumeration en = configuration.keys();
         while( en.hasMoreElements() )
         {
             final Object key = en.nextElement();
             if( key != null && key instanceof String )
             {
                 final String keyString = ( (String) key ).trim().toLowerCase();
                 if( keyString.startsWith( "log4j" ) && keyString.indexOf( "rootlogger" ) != -1 )
                 {
                     final Object value = configuration.get( key );
                     if( value != null && value instanceof String )
                     {
                         levelProperty = ( (String) value ).trim().toUpperCase();
                     }
                 }
             }
         }
         if( levelProperty == null )
         {
             return;
         }
         setLevelToRootLogger( levelProperty );
     }
 
     private static void setLevelToRootLogger( final String levelProperty )
     {
         final java.util.logging.Logger rootLogger = java.util.logging.Logger.getLogger( "" );
 
         if( levelProperty.indexOf( "OFF" ) != -1 )
         {
             rootLogger.setLevel( Level.OFF );
         }
         else if( levelProperty.indexOf( "FATAL" ) != -1 )
         {
             rootLogger.setLevel( Level.SEVERE );
         }
         else if( levelProperty.indexOf( "ERROR" ) != -1 )
         {
             rootLogger.setLevel( Level.SEVERE );
         }
         else if( levelProperty.indexOf( "WARN" ) != -1 )
         {
             rootLogger.setLevel( Level.WARNING );
         }
         else if( levelProperty.indexOf( "INFO" ) != -1 )
         {
             rootLogger.setLevel( Level.INFO );
         }
         else if( levelProperty.indexOf( "DEBUG" ) != -1 )
         {
             rootLogger.setLevel( Level.FINE );
         }
         else if( levelProperty.indexOf( "TRACE" ) != -1 )
         {
             rootLogger.setLevel( Level.FINEST );
         }
     }
 }
