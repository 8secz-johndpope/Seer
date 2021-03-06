 /**
  * Copyright 2007 The Apache Software Foundation
  *
  * Licensed to the Apache Software Foundation (ASF) under one
  * or more contributor license agreements.  See the NOTICE file
  * distributed with this work for additional information
  * regarding copyright ownership.  The ASF licenses this file
  * to you under the Apache License, Version 2.0 (the
  * "License"); you may not use this file except in compliance
  * with the License.  You may obtain a copy of the License at
  *
  *     http://www.apache.org/licenses/LICENSE-2.0
  *
  * Unless required by applicable law or agreed to in writing, software
  * distributed under the License is distributed on an "AS IS" BASIS,
  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  * See the License for the specific language governing permissions and
  * limitations under the License.
  */
 package org.apache.hadoop.hbase;
 
 import java.util.Iterator;
 import java.util.Map.Entry;
 
 import org.apache.commons.logging.Log;
 import org.apache.commons.logging.LogFactory;
 import org.apache.hadoop.conf.Configuration;
 
 /**
  * Adds HBase configuration files to a Configuration
  */
 public class HBaseConfiguration extends Configuration {
 
   private static final Log LOG = LogFactory.getLog(HBaseConfiguration.class);
 
   /**
    * Instantinating HBaseConfiguration() is deprecated. Please use
    * HBaseConfiguration#create() to construct a plain Configuration
    */
   @Deprecated
   public HBaseConfiguration() {
     //TODO:replace with private constructor, HBaseConfiguration should not extend Configuration
     super();
     addHbaseResources(this);
    LOG.warn("instantinating HBaseConfiguration() is deprecated. Please use" +
     		" HBaseConfiguration#create() to construct a plain Configuration");
   }
 
   /**
    * Instantiating HBaseConfiguration() is deprecated. Please use
    * HBaseConfiguration#create(conf) to construct a plain Configuration
    */
   @Deprecated
   public HBaseConfiguration(final Configuration c) {
     //TODO:replace with private constructor
     this();
     for (Entry<String, String>e: c) {
       set(e.getKey(), e.getValue());
     }
   }
 
   public static Configuration addHbaseResources(Configuration conf) {
     conf.addResource("hbase-default.xml");
     conf.addResource("hbase-site.xml");
     return conf;
   }
 
   /**
    * Creates a Configuration with HBase resources
    * @return a Configuration with HBase resources
    */
   public static Configuration create() {
     Configuration conf = new Configuration();
     return addHbaseResources(conf);
   }
 
   /**
    * Creates a clone of passed configuration.
    * @param that Configuration to clone.
    * @return a Configuration created with the hbase-*.xml files plus
    * the given configuration.
    */
   public static Configuration create(final Configuration that) {
     Configuration conf = create();
     for (Entry<String, String>e: that) {
       conf.set(e.getKey(), e.getValue());
     }
     return conf;
   }
 
   /**
    * Returns the hash code value for this HBaseConfiguration. The hash code of a
    * HBaseConfiguration is defined by the xor of the hash codes of its entries.
    *
    * @see Configuration#iterator() How the entries are obtained.
    */
   @Override
   @Deprecated
   public int hashCode() {
     return hashCode(this);
   }
 
   /**
    * Returns the hash code value for this HBaseConfiguration. The hash code of a
    * Configuration is defined by the xor of the hash codes of its entries.
    *
    * @see Configuration#iterator() How the entries are obtained.
    */
   public static int hashCode(Configuration conf) {
     int hash = 0;
 
     Iterator<Entry<String, String>> propertyIterator = conf.iterator();
     while (propertyIterator.hasNext()) {
       hash ^= propertyIterator.next().hashCode();
     }
     return hash;
   }
 
   @Override
   public boolean equals(Object obj) {
     if (this == obj)
       return true;
     if (obj == null)
       return false;
     if (!(obj instanceof HBaseConfiguration))
       return false;
     HBaseConfiguration otherConf = (HBaseConfiguration) obj;
     if (size() != otherConf.size()) {
       return false;
     }
     Iterator<Entry<String, String>> propertyIterator = this.iterator();
     while (propertyIterator.hasNext()) {
       Entry<String, String> entry = propertyIterator.next();
       String key = entry.getKey();
       String value = entry.getValue();
       if (!value.equals(otherConf.getRaw(key))) {
         return false;
       }
     }
 
     return true;
   }
 }
