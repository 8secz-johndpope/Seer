 /*
  * JBoss, Home of Professional Open Source
  * Copyright 2008, Red Hat Middleware LLC, and individual contributors
  * by the @authors tag. See the copyright.txt in the distribution for a
  * full listing of individual contributors.
  *
  * Licensed under the Apache License, Version 2.0 (the "License");
  * you may not use this file except in compliance with the License.
  * You may obtain a copy of the License at
  * http://www.apache.org/licenses/LICENSE-2.0
  * Unless required by applicable law or agreed to in writing, software
  * distributed under the License is distributed on an "AS IS" BASIS,  
  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  * See the License for the specific language governing permissions and
  * limitations under the License.
  */
 
 package org.jboss.webbeans.contexts;
 
 import javax.webbeans.SessionScoped;
 
 import org.jboss.webbeans.log.LogProvider;
 import org.jboss.webbeans.log.Logging;
 import org.jboss.webbeans.util.Names;
 
 /**
  * The session context
  * 
  * @author Nicklas Karlsson
  */
 public class SessionContext extends AbstractContext
 {
    private static LogProvider log = Logging.getLogProvider(SessionContext.class);
 
    public static SessionContext INSTANCE = new SessionContext();
 
    // The beans
    private ThreadLocal<BeanMap> beanMap;
 
    /**
     * Constructor
     */
    protected SessionContext()
    {
       super(SessionScoped.class);
       log.trace("Created session context");
    }
 
    /**
     * Gets the bean map
     * 
     * @returns The bean map
     * @see org.jboss.webbeans.contexts.AbstractContext#getBeanMap()
     */
    @Override
    public BeanMap getBeanMap()
    {
       return beanMap.get();
    }
 
    /**
     * Sets the bean map
     * 
     * @param beanMap The bean map
     */
    public void setBeanMap(BeanMap beanMap)
    {
       this.beanMap.set(beanMap);
    }
 
    @Override
    public String toString()
    {
       String active = isActive() ? "Active " : "Inactive ";
       String count = getBeanMap() == null ? "" : "holding " + Names.count(getBeanMap().keySet()) + " instances ";
       String prefix = getBeanMap() == null ? "" : "with key prefix " + ((AbstractBeanMapAdaptor) getBeanMap()).getKeyPrefix();
       return active + "session context " + count + prefix; 
    }
 
 }
