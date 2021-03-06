 /*
    Copyright 2011 Janne Jalkanen
 
    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at
 
        http://www.apache.org/licenses/LICENSE-2.0
 
    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
 */
 package com.ecyrd.speed4j.log;
 
 import java.lang.management.ManagementFactory;
 import java.util.*;
 import java.util.concurrent.ConcurrentLinkedQueue;
 
 import javax.management.*;
 
 import com.ecyrd.speed4j.ConfigurationException;
 import com.ecyrd.speed4j.StopWatch;
 
 /**
  *  A Periodical log which can also expose its attributes via JMX.
  *  <p>
  *  The JMX name is based on the name of the Log.  So if you don't set
  *  it via {@link #setName(String)}, you'll end up something that Speed4J
  *  picks up on its own.  Normally, if you use the property file to
  *  configure Speed4J, this gets automatically assigned for you.
  *  <p>
  *  The PeriodicalLog starts a new Thread to collect the statistics. Don't forget to shut it down with a call to {@link #shutdown()},
  *  or else you might risk a memory leak.  This is a common problem with e.g. web applications, where redeployment
  *  regularly causes these.
  *  <p>
  *  In a web app, you could set up your own ServletContextListener to ensure the proper shutdown:
  *  <pre>
  *  class MyListener implements ServletContextListener {
  *     public void contextInitialized(ServletContextEvent sce) {}
  *     
  *     public void contextDestroyed(ServletContextEvent sce) {
  *         StopWatchFactory.getInstance("myLoggerName").shutdown();
  *     }
  *  }
  *  </pre>
  *  PeriodicalLog adds its own JVM-wide shutdown hook, so you don't need to prepare for that.3
  */
 public class PeriodicalLog extends Slf4jLog implements DynamicMBean
 {
     private static final int ATTRS_PER_ITEM = 6;
     private static final String ATTR_POSTFIX_MAX = "/max";
     private static final String ATTR_POSTFIX_MIN = "/min";
     private static final String ATTR_POSTFIX_STDDEV = "/stddev";
     private static final String ATTR_POSTFIX_AVG = "/avg (ms)";
     private static final String ATTR_POSTFIX_COUNT = "/count";
     private static final String ATTR_POSTFIX_95 = "/95";
     
     private Queue<StopWatch> m_queue              = new ConcurrentLinkedQueue<StopWatch>();
     private Thread           m_collectorThread;
     private boolean          m_running            = true;
     private int              m_periodSeconds      = 30;
     private MBeanServer      m_mbeanServer        = null;
     private String[]         m_jmxAttributes      = null;
     private MBeanInfo        m_beanInfo;
     private HashMap<String,CollectedStatistics> m_statistics;
     
     /**
      *  Creates an instance of PeriodicalLog.
      */
     public PeriodicalLog()
     {
         Runtime.getRuntime().addShutdownHook( new Thread() {
             @Override
             public void run()
             {
                 shutdown();
             }
         });
     }
 
     @Override
     public void log(StopWatch sw)
     {
         //
         //  Start the collector lazily.
         //
         if( m_collectorThread == null )
         {
             m_collectorThread = new CollectorThread();
 
             m_collectorThread.start();        
         }
         
         m_queue.add( sw.freeze() );
     }
     
     /**
      *  Set the tags which are shown via JMX.
      *  
      *  @param value A comma-separated list of tags which are shown via JMX.
      */
     public void setJmx( String value )
     {
         m_jmxAttributes = value.split(",");
         
         m_mbeanServer = ManagementFactory.getPlatformMBeanServer();
             
         try
         {
             buildMBeanInfo();
             
             //
             //  Remove and reinstall from the MBean registry if it already exists.
             //
             if( m_mbeanServer.isRegistered(getJMXName()) )
                 m_mbeanServer.unregisterMBean(getJMXName());
             
             m_mbeanServer.registerMBean( this, getJMXName() );
         }
         catch (InstanceAlreadyExistsException e)
         {
             // OK
         }
         catch (Exception e)
         {
             throw new ConfigurationException(e);
         }
     }
     
     /**
      *  Shuts down the collector thread and removes the JMX bean
      *  if it is registered.  It is <i>very</i> important to call this
      *  or else you risk a memory leak.
      */
     @Override
     public void shutdown()
     {
         m_running = false;
         if( m_collectorThread != null ) m_collectorThread.interrupt();
 
         try
         {
             //
             //  Remove MBean
             //
            if( m_mbeanServer != null && m_mbeanServer.isRegistered(getJMXName()) )
                 m_mbeanServer.unregisterMBean(getJMXName());
         }
         catch (Exception e)
         {
             // TODO Auto-generated catch block
             e.printStackTrace();
         }
     }
 
     /**
      *  The name under which this Log should be exposed as a JMX bean.
      *  
      *  @return A ready-to-use ObjectName.
      *  
      *  @throws MalformedObjectNameException If your name is faulty.
      */
     private ObjectName getJMXName() throws MalformedObjectNameException
     {
         return new ObjectName("Speed4J: name="+getName());
     }
     
     /**
      *  Empties the queue and calculates the results.
      */
     private void doLog(long lastRun)
     {
         if( m_log == null || !m_log.isInfoEnabled() ) return;
         
         StopWatch sw;
         
         m_statistics = new HashMap<String,CollectedStatistics>();
         
         while( null != (sw = m_queue.poll()) )
         {
             CollectedStatistics cs = m_statistics.get(sw.getTag());
             
             if( cs == null ) 
             {
                 cs = new CollectedStatistics();
                 m_statistics.put( sw.getTag(), cs );
             }
             
             cs.add( sw );
         }
         
         printf("Statistics from %tc to %tc", new Date(lastRun), new Date());
         
         printf("Tag                                       Avg(ms)      Min      Max  Std Dev     95th   Count");
         
         for( Map.Entry<String,CollectedStatistics> e : m_statistics.entrySet() )
         {
             CollectedStatistics cs = e.getValue();
             printf("%-40s %8.2f %8.2f %8.2f %8.2f %8.2f %7d", e.getKey(),cs.getAverageMS(), cs.getMin(), cs.getMax(), cs.getStdDev(), cs.getPercentile( 95 ), cs.getInvocations());
         }
         
         printf("");
     }
     
     /**
      *  Writes to the internal logger, just like ye goode olde C printf().
      *  
      *  @param pattern Pattern to write to (see {@link Formatter#format(String, Object...)}
      *  @param args Arguments for the pattern.
      */
     private void printf( String pattern, Object... args )
     {
         StringBuilder sb = new StringBuilder();
         Formatter formatter = new Formatter(sb);
 
         formatter.format(pattern, args);
         
         m_log.info(sb.toString());
     }
     
     /**
      *  An internal Thread which wakes up periodically and checks whether
      *  the data should be collected and dumped.
      */
     private class CollectorThread extends Thread
     {
         @Override
         public void run()
         {
             long lastRun = System.currentTimeMillis();
             
             // Round to the nearest periodSeconds
             lastRun = (lastRun / (1000*m_periodSeconds)) * (1000*m_periodSeconds);
             
             while(m_running)
             {
                 try
                 {
                     Thread.sleep(1000L);
                 }
                 catch(Throwable t)
                 {
                     // Ignore all nasties, keep this thing running until requested.
                 }                
                 
                 long now = System.currentTimeMillis();
                 
                 if( (now - lastRun)/1000 >= m_periodSeconds )
                 {
                     doLog(lastRun);
                     lastRun = now;
                 }
             }
             
             //
             // Do final log
             //
             doLog(lastRun);
         }
         
     }
 
     /**
      *  Set the logging period in seconds.  For example, a value of 5
      *  would log every 5 seconds, at 0,5,10,15,20,25,30,35,40,45,50, and 55 seconds
      *  after the full minute.
      *  
      *  @param periodSeconds The period in seconds.
      */
     public void setPeriod(int periodSeconds)
     {
         m_periodSeconds = periodSeconds;
     }
 
     //
     //  START MBEAN STUFF HERE
     //
     
     public Object getAttribute(String attribute) throws AttributeNotFoundException, MBeanException, ReflectionException
     {
         HashMap<String, CollectedStatistics> stats = m_statistics;
     
         if( stats != null )
         {
             String key     = attribute.substring(0,attribute.lastIndexOf('/'));
             String postfix = attribute.substring(attribute.lastIndexOf('/'));
             
             //System.out.println("Key="+key+" postfix="+postfix);
             
             CollectedStatistics cs = m_statistics.get(key);
             
             if( postfix.equals(ATTR_POSTFIX_AVG))             
                 return cs.getAverageMS();
             if( postfix.equals(ATTR_POSTFIX_MAX))
                 return cs.getMax();
             if( postfix.equals(ATTR_POSTFIX_MIN))
                 return cs.getMin();
             if( postfix.equals(ATTR_POSTFIX_STDDEV) )
                 return cs.getStdDev();
             if( postfix.equals(ATTR_POSTFIX_COUNT) )
                 return cs.getInvocations();
             if( postfix.equals(ATTR_POSTFIX_95) )
                 return cs.getPercentile( 95 );
             
             throw new AttributeNotFoundException(attribute);
         }
         
         return null;
     }
 
     public AttributeList getAttributes(String[] attributes)
     {
         AttributeList ls = new AttributeList();
         
         for( String s : attributes )
         {
             try
             {
                 ls.add( new Attribute(s,getAttribute(s)) );
             }
             catch (Exception e)
             {
                 // TODO Auto-generated catch block
                 e.printStackTrace();
             }
         }
         
         return ls;
     }
 
     public MBeanInfo getMBeanInfo()
     {
         return m_beanInfo;
     }
 
     public Object invoke(String actionName, Object[] params, String[] signature) throws MBeanException, ReflectionException
     {
         // This is a no-op, we don't set allow any ops
         return null;
     }
 
     public void setAttribute(Attribute attribute)
                                                  throws AttributeNotFoundException,
                                                      InvalidAttributeValueException,
                                                      MBeanException,
                                                      ReflectionException
     {
         // This is a no-op
     }
 
     public AttributeList setAttributes(AttributeList attributes)
     {
         // this is a no-op
         return null;
     }
     
     /**
      *  Builds the MBeanInfo for all the exposed attributes.
      *  
      *  @throws IntrospectionException
      */
     private void buildMBeanInfo() throws IntrospectionException
     {
         MBeanAttributeInfo[] attributes = null;
 
         if( m_jmxAttributes != null )
         {
             attributes = new MBeanAttributeInfo[m_jmxAttributes.length*ATTRS_PER_ITEM];
 
             for( int i = 0; i < m_jmxAttributes.length; i++ )
             {
                 String name = m_jmxAttributes[i].trim();
 
                 attributes[ATTRS_PER_ITEM*i]   = new MBeanAttributeInfo( name+ATTR_POSTFIX_AVG,    "double", "Average value (in milliseconds)", true, false, false );
                 attributes[ATTRS_PER_ITEM*i+1] = new MBeanAttributeInfo( name+ATTR_POSTFIX_STDDEV, "double", "Standard Deviation", true, false, false );
                 attributes[ATTRS_PER_ITEM*i+2] = new MBeanAttributeInfo( name+ATTR_POSTFIX_MIN,    "double", "Minimum value", true, false, false );
                 attributes[ATTRS_PER_ITEM*i+3] = new MBeanAttributeInfo( name+ATTR_POSTFIX_MAX,    "double", "Maximum value", true, false, false );
                 attributes[ATTRS_PER_ITEM*i+4] = new MBeanAttributeInfo( name+ATTR_POSTFIX_COUNT,  "int",    "Number of invocations", true, false, false );
                 attributes[ATTRS_PER_ITEM*i+5] = new MBeanAttributeInfo( name+ATTR_POSTFIX_95   ,  "double", "95th percentile", true, false, false );
             }
         }
         //
         //  Create the actual BeanInfo instance.
         //
         MBeanOperationInfo[] operations = null;
         MBeanConstructorInfo[] constructors = null;
         MBeanNotificationInfo[] notifications = null;
 
         m_beanInfo = new MBeanInfo( getClass().getName(),
                                     "PeriodicalLog for logger "+getName(),
                                     attributes,
                                     constructors,
                                     operations,
                                     notifications );
     }
 
 }
