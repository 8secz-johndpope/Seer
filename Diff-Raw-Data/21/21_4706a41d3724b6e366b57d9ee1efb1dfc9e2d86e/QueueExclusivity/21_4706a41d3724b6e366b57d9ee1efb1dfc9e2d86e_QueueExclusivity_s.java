 //   The contents of this file are subject to the Mozilla Public License
 //   Version 1.1 (the "License"); you may not use this file except in
 //   compliance with the License. You may obtain a copy of the License at
 //   http://www.mozilla.org/MPL/
 //
 //   Software distributed under the License is distributed on an "AS IS"
 //   basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
 //   License for the specific language governing rights and limitations
 //   under the License.
 //
 //   The Original Code is RabbitMQ.
 //
 //   The Initial Developers of the Original Code are LShift Ltd,
 //   Cohesive Financial Technologies LLC, and Rabbit Technologies Ltd.
 //
 //   Portions created before 22-Nov-2008 00:00:00 GMT by LShift Ltd,
 //   Cohesive Financial Technologies LLC, or Rabbit Technologies Ltd
 //   are Copyright (C) 2007-2008 LShift Ltd, Cohesive Financial
 //   Technologies LLC, and Rabbit Technologies Ltd.
 //
 //   Portions created by LShift Ltd are Copyright (C) 2007-2009 LShift
 //   Ltd. Portions created by Cohesive Financial Technologies LLC are
 //   Copyright (C) 2007-2009 Cohesive Financial Technologies
 //   LLC. Portions created by Rabbit Technologies Ltd are Copyright
 //   (C) 2007-2009 Rabbit Technologies Ltd.
 //
 //   All Rights Reserved.
 //
 //   Contributor(s): ______________________________________.
 //
 
 package com.rabbitmq.client.test.functional;
 
 import java.util.Map;
 import java.util.HashMap;
 import java.io.IOException;
 
 import com.rabbitmq.client.AMQP;
 import com.rabbitmq.client.Connection;
 import com.rabbitmq.client.Channel;
 import com.rabbitmq.client.QueueingConsumer;
 
 // Test queue auto-delete and exclusive semantics.
 public class QueueExclusivity extends BrokerTestCase
 {
 
   HashMap<String,Object> noArgs = new HashMap();
 
   public Connection altConnection;
   public Channel altChannel;
   String q = "exclusiveQ";
   
   void verifyQueueExists(String name) throws IOException {
     channel.queueDeclare(name, true,
                          // these are ignored, since it's passive
                          false, false, false, noArgs);
   }
 
   protected void createResources() throws IOException {
     altConnection = connectionFactory.newConnection("localhost");
     altChannel = altConnection.createChannel();
     AMQP.Queue.DeclareOk ok = altChannel.queueDeclare(q, false,
                                                       // not durable, exclusive, not auto-delete
                                                       false, true, false, noArgs);
   }
 
   protected void releaseResources() throws IOException {
    altConnection.close();
   }
   
   public void testQueueExclusiveForPassiveDeclare() throws Exception {
     try {
       AMQP.Queue.DeclareOk ok2 = channel.queueDeclare(q, true, false, true, false, noArgs);
     }
     catch (IOException ioe) {
       // TODO test the particular error
       return;
     }
     fail("Passive queue declaration of an exclusive queue from another connection should fail");
   }
 
   // This is a different scenario because active declare takes notice of
   // the all the arguments
   public void testQueueExclusiveForDeclare() throws Exception {
     try {
       AMQP.Queue.DeclareOk ok2 = channel.queueDeclare(q, false, false, true, false, noArgs);
     }
     catch (IOException ioe) {
       // TODO test the particular error
       return;
     }
     fail("Active queue declaration of an exclusive queue from another connection should fail");
   }
 
   public void testQueueExclusiveForConsume() throws Exception {
     QueueingConsumer c = new QueueingConsumer(channel);
     try {
       channel.basicConsume(q, c);
     }
     catch (IOException ioe) {
       return;
     }
     fail("Exclusive queue should be locked for basic consume from another connection");
   }
 
   public void testQueueExclusiveForPurge() throws Exception {
    QueueingConsumer c = new QueueingConsumer(channel);
     try {
       channel.queuePurge(q);
     }
     catch (IOException ioe) {
       return;
     }
     fail("Exclusive queue should be locked for queue purge from another connection");
   }
 
   public void testQueueExclusiveForDelete() throws Exception {
    QueueingConsumer c = new QueueingConsumer(channel);
     try {
       channel.queueDelete(q);
     }
     catch (IOException ioe) {
       return;
     }
     fail("Exclusive queue should be locked for queue delete from another connection");
   }
 
 }
