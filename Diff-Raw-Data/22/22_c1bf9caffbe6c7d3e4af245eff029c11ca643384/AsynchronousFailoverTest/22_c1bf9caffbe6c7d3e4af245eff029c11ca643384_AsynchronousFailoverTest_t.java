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
 
 package org.hornetq.tests.integration.cluster.failover;
 
 import java.util.ArrayList;
 import java.util.List;
 import java.util.concurrent.CountDownLatch;
 import java.util.concurrent.TimeUnit;
 
 import org.hornetq.core.client.ClientConsumer;
 import org.hornetq.core.client.ClientMessage;
 import org.hornetq.core.client.ClientProducer;
 import org.hornetq.core.client.ClientSession;
 import org.hornetq.core.client.SessionFailureListener;
 import org.hornetq.core.client.impl.ClientSessionFactoryInternal;
 import org.hornetq.core.client.impl.ClientSessionInternal;
 import org.hornetq.core.client.impl.DelegatingSession;
 import org.hornetq.core.config.TransportConfiguration;
 import org.hornetq.core.exception.HornetQException;
 import org.hornetq.core.logging.Logger;
 import org.hornetq.core.remoting.RemotingConnection;
 
 /**
  * A MultiThreadFailoverTest
  * 
  * Test Failover where failure is prompted by another thread
  *
  * @author Tim Fox
  *
  *
  */
 public class AsynchronousFailoverTest extends FailoverTestBase
 {
    private static final Logger log = Logger.getLogger(AsynchronousFailoverTest.class);
 
    private volatile MyListener listener;
 
    private volatile ClientSessionFactoryInternal sf;
 
    class MyListener implements SessionFailureListener
    {
       CountDownLatch latch = new CountDownLatch(1);
 
       public void connectionFailed(HornetQException me)
       {
          latch.countDown();
       }
       
       public void beforeReconnect(HornetQException me)
       {            
       }
    }
 
    public void testNonTransactional() throws Exception
    {
       runTest(new TestRunner()
       {
          public void run()
          {
             try
             {
                doTestNonTransactional(this);
             }
             catch (Exception e)
             {
                log.error("Test failed", e);
             }
          }
       });
    }
 
    public void testTransactional() throws Exception
    {
       runTest(new TestRunner()
       {
          public void run()
          {
             try
             {
                doTestTransactional(this);
             }
             catch (Exception e)
             {
                log.error("Test failed", e);
             }
          }
       });
    }
 
    abstract class TestRunner implements Runnable
    {
       volatile boolean failed;
 
       boolean isFailed()
       {
          return failed;
       }
 
       void setFailed()
       {
          failed = true;
       }
       
       void reset()
       {
          failed = false;
       }
    }
 
    private void runTest(final TestRunner runnable) throws Exception
    {
       final int numIts = 1;
       
       DelegatingSession.debug = true;
 
       try
       {
          for (int i = 0; i < numIts; i++)
          {
             log.info("Iteration " + i);
    
             sf = getSessionFactory();
    
             sf.setBlockOnNonPersistentSend(true);
             sf.setBlockOnPersistentSend(true);
    
             ClientSession createSession = sf.createSession(true, true);
    
             createSession.createQueue(ADDRESS, ADDRESS, null, true);
    
             RemotingConnection conn = ((ClientSessionInternal)createSession).getConnection();
    
             Thread t = new Thread(runnable);
    
             t.start();
    
             long randomDelay = (long)(2000 * Math.random());
    
             log.info("Sleeping " + randomDelay);
    
             Thread.sleep(randomDelay);
    
             log.info("Failing asynchronously");
    
             MyListener listener = this.listener;
    
             // Simulate failure on connection
             conn.fail(new HornetQException(HornetQException.NOT_CONNECTED));
    
             if (listener != null)
             {
                boolean ok = listener.latch.await(10000, TimeUnit.MILLISECONDS);
    
                assertTrue(ok);
             }
    
             runnable.setFailed();
    
             log.info("Fail complete");
    
             t.join();
    
             createSession.close();
    
             if (sf.numSessions() != 0)
             {
                DelegatingSession.dumpSessionCreationStacks();
             }
    
             assertEquals(0, sf.numSessions());
    
             assertEquals(0, sf.numConnections());
    
             if (i != numIts - 1)
             {
                tearDown();
                runnable.reset();
                setUp();
             }
          }
       }
       finally
       {
          DelegatingSession.debug = false;
       }
    }
 
    private void doTestNonTransactional(final TestRunner runner) throws Exception
    {
       while (!runner.isFailed())
       {
          log.info("looping");
 
          ClientSession session = sf.createSession(true, true, 0);
 
          MyListener listener = new MyListener();
 
          session.addFailureListener(listener);
 
          this.listener = listener;
 
          ClientProducer producer = session.createProducer(ADDRESS);
 
          final int numMessages = 1000;
 
          for (int i = 0; i < numMessages; i++)
          {
             ClientMessage message = session.createClientMessage(true);
 
             message.getBody().writeString("message" + i);
 
             message.putIntProperty("counter", i);
 
             boolean retry = false;
             do
             {
                try
                {
                   producer.send(message);
 
                   retry = false;
                }
                catch (HornetQException e)
                {
                   log.info("exception when sending message with counter " + i);
                   assertEquals(e.getCode(), HornetQException.UNBLOCKED);
 
                   retry = true;
                }
             }
             while (retry);
          }
         
         // create the consumer with retry if failover occurs during createConsumer call
         ClientConsumer consumer = null;
         boolean retry = false;
         do
         {
            try
            {
               consumer = session.createConsumer(ADDRESS);
               
               retry = false;
            }
            catch (HornetQException e)
            {
               log.info("exception when creating consumer");
               assertEquals(e.getCode(), HornetQException.UNBLOCKED);
 
               retry = true;
            }
         }
         while (retry);
 
          session.start();
 
          List<Integer> counts = new ArrayList<Integer>(1000);
          int lastCount = -1;
          boolean counterGap = false;
          while (true)
          {
             ClientMessage message = consumer.receive(500);
 
             if (message == null)
             {
                break;
             }
 
             // messages must remain ordered but there could be a "jump" if messages
             // are missing or duplicated
             int count = message.getIntProperty("counter");
             counts.add(count);
             if (count != lastCount + 1)
             {
                if (counterGap)
                {
                   fail("got a another counter gap at " + count + ": " + counts);
                }
                else
                {
                   if (lastCount != -1)
                   {
                      log.info("got first counter gap at " + count);                  
                      counterGap = true;
                   }
                }
             }
             
             lastCount = count;
 
             message.acknowledge();
          }
 
          session.close();
 
          this.listener = null;
       }
    }
 
    private void doTestTransactional(final TestRunner runner) throws Exception
    {
       while (!runner.isFailed())
       {
          ClientSession session = null;
 
          try
          {
             session = sf.createSession(false, false);
 
             MyListener listener = new MyListener();
 
             session.addFailureListener(listener);
 
             this.listener = listener;
 
             ClientProducer producer = session.createProducer(ADDRESS);
 
             final int numMessages = 1000;
 
             for (int i = 0; i < numMessages; i++)
             {
                ClientMessage message = session.createClientMessage(true);
 
                message.getBody().writeString("message" + i);
 
                message.putIntProperty("counter", i);
 
                boolean retry = false;
                do
                {
                   try
                   {
                      producer.send(message);
                   }
                   catch (HornetQException e)
                   {
                      assertEquals(e.getCode(), HornetQException.UNBLOCKED);
 
                      retry = true;
                   }
                }
                while (retry);
             }
 
             boolean retry = false;
             while (retry)
             {
                try
                {
                   session.commit();
 
                   retry = false;
                }
                catch (HornetQException e)
                {
                   if (e.getCode() == HornetQException.TRANSACTION_ROLLED_BACK)
                   {
                      // OK
                      session.close();
 
                      continue;
                   }
                   else if (e.getCode() == HornetQException.UNBLOCKED)
                   {
                      retry = true;
                   }
                   else
                   {
                      throw e;
                   }
                }
             }
 
             ClientConsumer consumer = session.createConsumer(ADDRESS);
 
             session.start();
 
             int lastCount = -1;
             while (true)
             {
                ClientMessage message = consumer.receive(500);
 
                if (message == null)
                {
                   break;
                }
 
                // There may be some missing or duplicate messages - but the order should be correct
 
                int count = message.getIntProperty("counter");
 
                assertTrue("count:" + count + " last count:" + lastCount, count >= lastCount);
 
                lastCount = count;
 
                message.acknowledge();
             }
 
             retry = false;
             while (retry)
             {
                try
                {
                   session.commit();
 
                   retry = false;
                }
                catch (HornetQException e)
                {
                   if (e.getCode() == HornetQException.TRANSACTION_ROLLED_BACK)
                   {
                      // OK
                      session.close();
 
                      continue;
                   }
                   else if (e.getCode() == HornetQException.UNBLOCKED)
                   {
                      retry = true;
                   }
                   else
                   {
                      throw e;
                   }
                }
             }
          }
          finally
          {
             if (session != null)
             {
                session.close();
             }
          }
 
          this.listener = null;
       }
    }
 
    @Override
    protected TransportConfiguration getAcceptorTransportConfiguration(boolean live)
    {
       return getInVMTransportAcceptorConfiguration(live);
    }
 
    @Override
    protected TransportConfiguration getConnectorTransportConfiguration(final boolean live)
    {
       return getInVMConnectorTransportConfiguration(live);
    }
    
 
 }
