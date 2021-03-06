 /**
  * Copyright 2013 Twitter, Inc.
  * Licensed under the Apache License, Version 2.0 (the "License");
  * you may not use this file except in compliance with the License.
  * You may obtain a copy of the License at
  * http://www.apache.org/licenses/LICENSE-2.0
  * Unless required by applicable law or agreed to in writing, software
  * distributed under the License is distributed on an "AS IS" BASIS,
  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  * See the License for the specific language governing permissions and
  * limitations under the License.
  **/
 
 package com.twitter.hbc;
 
 import com.google.common.base.Preconditions;
 import com.google.common.util.concurrent.ThreadFactoryBuilder;
 import com.twitter.hbc.core.Hosts;
 import com.twitter.hbc.core.HttpHosts;
 import com.twitter.hbc.core.endpoint.RawEndpoint;
 import com.twitter.hbc.core.endpoint.StreamingEndpoint;
 import com.twitter.hbc.core.event.Event;
 import com.twitter.hbc.core.processor.HosebirdMessageProcessor;
 import com.twitter.hbc.httpclient.BasicClient;
 import com.twitter.hbc.httpclient.auth.Authentication;
 
 import java.util.concurrent.*;
 import java.util.concurrent.atomic.AtomicInteger;
 
 /**
 * TODO: Some real comments
  */
 public class ClientBuilder {
 
   private static AtomicInteger clientNum = new AtomicInteger(0);
   protected Authentication auth;
   protected Hosts hosts;
   protected HosebirdMessageProcessor processor;
   protected StreamingEndpoint endpoint;
   protected boolean enableGZip;
   protected String name;
   protected RateTracker rateTracker;
   protected ExecutorService executorService;
   protected BlockingQueue<Event> eventQueue;
   protected ReconnectionManager reconnectionManager;
 
   public ClientBuilder() {
     enableGZip = true;
     name = "hosebird-client-" + clientNum.getAndIncrement();
     ThreadFactory threadFactory = new ThreadFactoryBuilder()
             .setDaemon(true)
             .setNameFormat("hosebird-client-io-thread-%d")
             .build();
     executorService = Executors.newSingleThreadExecutor(threadFactory);
 
     ThreadFactory rateTrackerThreadFactory = new ThreadFactoryBuilder()
             .setDaemon(true)
             .setNameFormat("hosebird-client-rateTracker-thread-%d")
             .build();
 
     ScheduledExecutorService scheduledExecutor = Executors.newScheduledThreadPool(1, rateTrackerThreadFactory);
     rateTracker = new RateTracker(30000, 100, true, scheduledExecutor);
     reconnectionManager = new ReconnectionManager(5);
   }
 
   public ClientBuilder name(String name) {
     this.name = Preconditions.checkNotNull(name);
     return this;
   }
 
   public ClientBuilder gzipEnabled(boolean gzip) {
     this.enableGZip = gzip;
     return this;
   }
 
   public ClientBuilder hosts(String host) {
     this.hosts = new HttpHosts(Preconditions.checkNotNull(host));
     return this;
   }
 
   public ClientBuilder hosts(Hosts hosts) {
     this.hosts = Preconditions.checkNotNull(hosts);
     return this;
   }
 
   public ClientBuilder endpoint(StreamingEndpoint endpoint){
     this.endpoint = Preconditions.checkNotNull(endpoint);
     return this;
   }
 
   public ClientBuilder authentication(Authentication auth) {
     this.auth = auth;
     return this;
   }
 
   public ClientBuilder processor(HosebirdMessageProcessor processor) {
     this.processor = processor;
     return this;
   }
 
   public ClientBuilder eventMessageQueue(BlockingQueue<Event> events) {
     this.eventQueue = events;
     return this;
   }
 
   public ClientBuilder retries(int retries) {
     this.reconnectionManager = new ReconnectionManager(retries);
     return this;
   }
 
   public ClientBuilder endpoint(String uri, String httpMethod) {
     Preconditions.checkNotNull(uri);
     this.endpoint = new RawEndpoint(uri, httpMethod);
     return this;
   }
 
   public BasicClient build() {
     return new BasicClient(name, hosts, endpoint, auth, enableGZip, processor, reconnectionManager,
             rateTracker, executorService, eventQueue);
   }
 }
 
