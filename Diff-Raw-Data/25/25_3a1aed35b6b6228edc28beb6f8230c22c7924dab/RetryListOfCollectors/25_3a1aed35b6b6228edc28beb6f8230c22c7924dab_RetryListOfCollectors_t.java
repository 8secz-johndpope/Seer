 /*
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
 
 package org.apache.hadoop.chukwa.datacollection.sender;
 
 
 import java.io.*;
 import java.net.URL;
 import java.util.*;
 import org.apache.hadoop.conf.Configuration;
 
 /***
  * An iterator returning a list of Collectors to try. This class is
  * nondeterministic, since it puts collectors back on the list after some
  * period.
  * 
  * No node will be polled more than once per maxRetryRateMs milliseconds.
  * hasNext() will continue return true if you have not called it recently.
  * 
  * 
  */
 public class RetryListOfCollectors implements Iterator<String> {
 
   int maxRetryRateMs;
   List<String> collectors;
   long lastLookAtFirstNode;
   int nextCollector = 0;
   private String portNo;
   Configuration conf;
 
   public RetryListOfCollectors(File collectorFile, int maxRetryRateMs, Configuration conf)
       throws IOException {
     this.maxRetryRateMs = maxRetryRateMs;
     lastLookAtFirstNode = 0;
     collectors = new ArrayList<String>();
     this.conf = conf;
     portNo = conf.get("chukwaCollector.http.port", "8080");
 
     try {
       BufferedReader br = new BufferedReader(new FileReader(collectorFile));
       String line, parsedline;
       while ((line = br.readLine()) != null) {
         if (!line.contains("://")) {
           // no protocol, assume http
          if (line.matches(".*:\\d+.*")) {
             parsedline = "http://" + line+"/";
           } else {
             parsedline = "http://" + line + ":" + portNo;
           }
         } else {
          if (line.matches(".*:\\d+.*")) {
             parsedline = line;
           } else {
             parsedline = line + ":" + portNo;
           }
         }
         if(!parsedline.matches(".*\\w/.*")) //no resource name
           parsedline = parsedline+"/";
         collectors.add(parsedline);
       }
       
       br.close();
     } catch (FileNotFoundException e) {
       System.err
           .println("Error in RetryListOfCollectors() opening file: collectors, double check that you have set the CHUKWA_CONF_DIR environment variable. Also, ensure file exists and is in classpath");
     } catch (IOException e) {
       System.err
           .println("I/O error in RetryListOfcollectors instantiation in readLine() from specified collectors file");
       throw e;
     }
     shuffleList();
   }
 
   /**
    * This is only used for debugging. Possibly it should sanitize urls the same way the other
    * constructor does.
    * @param collectors
    * @param maxRetryRateMs
    */
   public RetryListOfCollectors(final List<String> collectors, int maxRetryRateMs) {
     this.maxRetryRateMs = maxRetryRateMs;
     lastLookAtFirstNode = 0;
     this.collectors = new ArrayList<String>();
     this.collectors.addAll(collectors);
     shuffleList();
   }
 
   // for now, use a simple O(n^2) algorithm.
   // safe, because we only do this once, and on smalls list
   private void shuffleList() {
     ArrayList<String> newList = new ArrayList<String>();
     Random r = new java.util.Random();
     while (!collectors.isEmpty()) {
       int toRemove = r.nextInt(collectors.size());
       String next = collectors.remove(toRemove);
       newList.add(next);
     }
     collectors = newList;
   }
 
   public boolean hasNext() {
     return collectors.size() > 0
         && ((nextCollector != 0) || (System.currentTimeMillis()
             - lastLookAtFirstNode > maxRetryRateMs));
   }
 
   public String next() {
     if (hasNext()) {
       int currCollector = nextCollector;
       nextCollector = (nextCollector + 1) % collectors.size();
       if (currCollector == 0)
         lastLookAtFirstNode = System.currentTimeMillis();
       return collectors.get(currCollector);
     } else
       return null;
   }
 
   public String getRandomCollector() {
     return collectors.get((int) java.lang.Math.random() * collectors.size());
   }
 
   public void add(URL collector) {
     collectors.add(collector.toString());
   }
 
   public void remove() {
     throw new UnsupportedOperationException();
     // FIXME: maybe just remove a collector from our list and then
     // FIXME: make sure next doesn't break (i.e. reset nextCollector if
     // necessary)
   }
 
   /**
    * 
    * @return total number of collectors in list
    */
   int total() {
     return collectors.size();
   }
 
 }
