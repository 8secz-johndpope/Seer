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
 //   The Initial Developers of the Original Code are LShift Ltd.,
 //   Cohesive Financial Technologies LLC., and Rabbit Technologies Ltd.
 //
 //   Portions created by LShift Ltd., Cohesive Financial Technologies
 //   LLC., and Rabbit Technologies Ltd. are Copyright (C) 2007-2008
 //   LShift Ltd., Cohesive Financial Technologies LLC., and Rabbit
 //   Technologies Ltd.;
 //
 //   All Rights Reserved.
 //
 //   Contributor(s): ______________________________________.
 //
 
 package com.rabbitmq.client.test.functional;
 
 import java.io.IOException;
 
 public class PersisterRestart3 extends PersisterRestartBase
 {
 
     private static final String Q1 = "Restart3One";
     private static final String Q2 = "Restart3Two";
 
     protected void exercisePersister(String q) 
       throws IOException
     {
         basicPublishPersistent(q);
         basicPublishVolatile(q);
     }
 
     public void testRestart()
        throws IOException, InterruptedException
     {
         declareDurableQueue(Q1);
         declareDurableQueue(Q2);
         channel.txSelect();
         exercisePersister(Q1);
         exercisePersister(Q2);
         forceSnapshot();
         // removing messages which are in the snapshot
         channel.txRollback();
         // Those will be in the incremental snapshot then
         exercisePersister(Q1);
         exercisePersister(Q2);
         // and hopefully delivered
         // That's one persistent and one volatile per queue.
         channel.txCommit();
 
         restart();
         
         assertDelivered(Q1, 1);
         assertDelivered(Q2, 1);
         deleteQueue(Q2);
         deleteQueue(Q1);
     }
 
 }
