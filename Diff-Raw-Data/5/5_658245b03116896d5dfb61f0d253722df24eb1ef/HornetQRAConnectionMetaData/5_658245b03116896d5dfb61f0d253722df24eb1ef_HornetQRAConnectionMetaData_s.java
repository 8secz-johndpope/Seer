 /*
  * Copyright 2009 Red Hat, Inc.
  * Red Hat licenses this file to you under the Apache License, version
  * 2.0 (the "License"); you may not use this file except in compliance
  * with the License.  You may obtain a copy of the License at
  *    http://www.apache.org/licenses/LICENSE-2.0
  * Unless required by applicable law or agreed to in writing, software
  * distributed under the License is distributed on an "AS IS" BASIS,
  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
  * implied.  See the License for the specific language governing
  * permissions and limitations under the License.
  */
 
 package org.hornetq.ra;
 
 import java.util.Enumeration;
 import java.util.Vector;
 
 import javax.jms.ConnectionMetaData;
 
 import org.hornetq.core.logging.Logger;
 
 /**
  * This class implements javax.jms.ConnectionMetaData
  * 
  * @author <a href="mailto:adrian@jboss.org">Adrian Brock</a>
  * @author <a href="mailto:jesper.pedersen@jboss.org">Jesper Pedersen</a>
  * @version $Revision: $
  */
 public class HornetQRAConnectionMetaData implements ConnectionMetaData
 {
    /** The logger */
    private static final Logger log = Logger.getLogger(HornetQRAConnectionMetaData.class);
 
    /** Trace enabled */
    private static boolean trace = HornetQRAConnectionMetaData.log.isTraceEnabled();
 
    /**
     * Constructor
     */
    public HornetQRAConnectionMetaData()
    {
       if (HornetQRAConnectionMetaData.trace)
       {
          HornetQRAConnectionMetaData.log.trace("constructor()");
       }
    }
 
    /**
     * Get the JMS version
     * @return The version
     */
    public String getJMSVersion()
    {
       if (HornetQRAConnectionMetaData.trace)
       {
          HornetQRAConnectionMetaData.log.trace("getJMSVersion()");
       }
 
       return "1.1";
    }
 
    /**
     * Get the JMS major version
     * @return The major version
     */
    public int getJMSMajorVersion()
    {
       if (HornetQRAConnectionMetaData.trace)
       {
          HornetQRAConnectionMetaData.log.trace("getJMSMajorVersion()");
       }
 
       return 1;
    }
 
    /**
     * Get the JMS minor version
     * @return The minor version
     */
    public int getJMSMinorVersion()
    {
       if (HornetQRAConnectionMetaData.trace)
       {
          HornetQRAConnectionMetaData.log.trace("getJMSMinorVersion()");
       }
 
       return 1;
    }
 
    /**
     * Get the JMS provider name
     * @return The name
     */
    public String getJMSProviderName()
    {
       if (HornetQRAConnectionMetaData.trace)
       {
          HornetQRAConnectionMetaData.log.trace("getJMSProviderName()");
       }
 
       return "HornetQ";
    }
 
    /**
     * Get the provider version
     * @return The version
     */
    public String getProviderVersion()
    {
       if (HornetQRAConnectionMetaData.trace)
       {
          HornetQRAConnectionMetaData.log.trace("getJMSProviderName()");
       }
 
       return "2.0";
    }
 
    /**
     * Get the provider major version
     * @return The version
     */
    public int getProviderMajorVersion()
    {
       if (HornetQRAConnectionMetaData.trace)
       {
          HornetQRAConnectionMetaData.log.trace("getProviderMajorVersion()");
       }
 
       return 2;
    }
 
    /**
     * Get the provider minor version
     * @return The version
     */
    public int getProviderMinorVersion()
    {
       if (HornetQRAConnectionMetaData.trace)
       {
          HornetQRAConnectionMetaData.log.trace("getProviderMinorVersion()");
       }
 
       return 0;
    }
 
    /**
     * Get the JMS XPropertyNames
     * @return The names
     */
    public Enumeration<Object> getJMSXPropertyNames()
    {
      Vector<Object> v = new Vector<Object>();
       return v.elements();
    }
 }
