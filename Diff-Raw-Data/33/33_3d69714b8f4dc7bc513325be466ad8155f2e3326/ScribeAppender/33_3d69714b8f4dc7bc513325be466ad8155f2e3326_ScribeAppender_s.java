 /**
  * (c) Copyright 2009 Cloudera, Inc.
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
 
 package org.apache.hadoop.scribe_log4j;
 
 import com.facebook.thrift.protocol.TBinaryProtocol;
 import com.facebook.thrift.transport.TSocket;
 import com.facebook.thrift.transport.TFramedTransport;
 import com.facebook.thrift.transport.TTransportException;
 import com.facebook.thrift.TException;
 
 import org.apache.hadoop.scribe_log4j.thrift.scribe.Client;
 import org.apache.hadoop.scribe_log4j.thrift.LogEntry;
 import org.apache.hadoop.util.StringUtils;
 
 import org.apache.log4j.AppenderSkeleton;
 import org.apache.log4j.spi.LoggingEvent;
 import org.apache.log4j.spi.LocationInfo;
 
 import java.util.List;
 import java.util.ArrayList;
 import java.net.Socket;
 import java.net.UnknownHostException;
 import java.net.InetAddress;
 import java.io.IOException;
 import java.io.BufferedReader;
 import java.io.InputStreamReader;
 
 /*
  * A Log4j Appender that writes log entries to a Scribe server.
  * By default the Scribe server is expected to run on localhost, port 1463.
  * Messages are written with a category of "hadoop".
  */
 public class ScribeAppender extends AppenderSkeleton {
 
   public static final String DEFAULT_SCRIBE_HOST = "127.0.0.1";
   public static final int DEFAULT_SCRIBE_PORT = 1463;
   public static final String DEFAULT_SCRIBE_CATEGORY = "hadoop";
 
   private String hostname;
   private String scribeHost;
   private int scribePort;
   private String scribeCategory;
 
   private List<LogEntry> logEntries;
 
   private Client client;
   private TFramedTransport transport;
 
   public String getHostname() {
     return hostname;
   }
 
   public void setHostname(String hostname) {
     this.hostname = hostname;
   }
 
   public String getScribeHost() {
     return scribeHost;
   }
 
   public void setScribeHost(String scribeHost) {
     this.scribeHost = scribeHost;
   }
 
   public int getScribePort() {
     return scribePort;
   }
 
   public void setScribePort(int scribePort) {
     this.scribePort = scribePort;
   }
 
   public String getScribeCategory() {
     return scribeCategory;
   }
 
   public void setScribeCategory(String scribeCategory) {
     this.scribeCategory = scribeCategory;
   }
 
   /*
    * Activates this Appender by opening
    * a transport to the Scribe server.
    */
   @Override
   public void activateOptions() {
     try {
       synchronized(this) {
         if (hostname == null) {
           try {
             hostname = InetAddress.getLocalHost().getCanonicalHostName();
           } catch (UnknownHostException e) {
             // can't get hostname
           }
         }
         if (scribeHost == null) {
           scribeHost = DEFAULT_SCRIBE_HOST;
         }
         if (scribePort == 0) {
           scribePort = DEFAULT_SCRIBE_PORT;
         }
         if (scribeCategory == null) {
           scribeCategory = DEFAULT_SCRIBE_CATEGORY;
         }
         // Thrift boilerplate code
         logEntries = new ArrayList<LogEntry>(1);
         TSocket sock = new TSocket(new Socket(scribeHost, scribePort));
         transport = new TFramedTransport(sock);
         TBinaryProtocol protocol = new TBinaryProtocol(transport, false, false);
         client = new Client(protocol, protocol);
         // This is commented out because it was throwing Exceptions for no good reason.
 //        transport.open();
       }
     } catch (TTransportException e) {
       System.err.println(StringUtils.stringifyException(e));
     } catch (UnknownHostException e) {
       System.err.println(StringUtils.stringifyException(e));
     } catch (IOException e) {
       System.err.println(StringUtils.stringifyException(e));
     } catch (Exception e) {
       System.err.println(StringUtils.stringifyException(e));
     }
   }
 
   /*
    * Appends a log message to Scribe
    */
   @Override
   public void append(LoggingEvent event) {
     synchronized(this) {
      String message = String.format("%s %s", hostname, layout.format(event));
      LogEntry entry = new LogEntry(scribeCategory, message);

      logEntries.clear();
      logEntries.add(entry);

       try {
         client.Log(logEntries);
       } catch (Exception e) {
  System.err.println(StringUtils.stringifyException(e));
       }
     }
   }
 
   @Override
   public void close() {
     if (transport != null) {
       transport.close();
     }
   }
 
   @Override
   public boolean requiresLayout() {
     return true;
   }
}
