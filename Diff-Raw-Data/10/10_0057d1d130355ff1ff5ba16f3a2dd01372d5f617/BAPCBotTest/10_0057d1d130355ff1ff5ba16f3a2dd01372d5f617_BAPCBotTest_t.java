 /*
  * Copyright 2012  Research Studios Austria Forschungsges.m.b.H.
  *
  *    Licensed under the Apache License, Version 2.0 (the "License");
  *    you may not use this file except in compliance with the License.
  *    You may obtain a copy of the License at
  *
  *        http://www.apache.org/licenses/LICENSE-2.0
  *
  *    Unless required by applicable law or agreed to in writing, software
  *    distributed under the License is distributed on an "AS IS" BASIS,
  *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  *    See the License for the specific language governing permissions and
  *    limitations under the License.
  */
 
 package won.bot.integrationtest;
 
 import org.junit.Assert;
 import org.junit.Before;
 import org.junit.Test;
 import org.junit.runner.RunWith;
 import org.springframework.beans.factory.annotation.Autowired;
 import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
 import org.springframework.context.ApplicationContext;
 import org.springframework.scheduling.support.PeriodicTrigger;
 import org.springframework.test.context.ContextConfiguration;
 import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
 import won.bot.framework.events.event.WorkDoneEvent;
 import won.bot.framework.events.listener.ExecuteOnEventListener;
 import won.bot.framework.manager.impl.SpringAwareBotManagerImpl;
import won.bot.impl.BAPCBot;
 
 import java.util.concurrent.CyclicBarrier;
 import java.util.concurrent.TimeUnit;
 
 /**
  * Integration test.
  */
 @RunWith(SpringJUnit4ClassRunner.class)
 @ContextConfiguration(locations = {"classpath:/spring/app/botRunner.xml"})
 
 public class BAPCBotTest{
     private static final int RUN_ONCE = 1;
     private static final long ACT_LOOP_TIMEOUT_MILLIS = 1000;
     private static final long ACT_LOOP_INITIAL_DELAY_MILLIS = 1000;
 
     MyBot bot;
 
     @Autowired
     ApplicationContext applicationContext;
 
     @Autowired
     SpringAwareBotManagerImpl botManager;
 
     /**
      * This is run before each @Test method.
      */
     @Before
     public void before(){
         //create a bot instance and auto-wire it
         this.bot = (MyBot) applicationContext.getAutowireCapableBeanFactory().autowire(MyBot.class, AutowireCapableBeanFactory.AUTOWIRE_BY_TYPE, false);
         //the bot also needs a trigger so its act() method is called regularly.
         // (there is no trigger bean in the context)
         PeriodicTrigger trigger = new PeriodicTrigger(ACT_LOOP_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);
         trigger.setInitialDelay(ACT_LOOP_INITIAL_DELAY_MILLIS);
         this.bot.setTrigger(trigger);
     }
 
     /**
      * The main test method.
      * @throws Exception
      */
     @Test
     public void testBAPCBot() throws Exception
     {
         //adding the bot to the bot manager will cause it to be initialized.
         //at that point, the trigger starts.
         botManager.addBot(this.bot);
         //the bot should now be running. We have to wait for it to finish before we
         //can check the results:
         //Together with the barrier.await() in the bot's listener, this trips the barrier
         //and both threads continue.
         this.bot.getBarrier().await();
         //now check the results!
         this.bot.executeAsserts();
     }
 
     /**
      * We create a subclass of the bot we want to test here so that we can
      * add a listener to its internal event bus and to access its listeners, which
      * record information during the run that we later check with asserts.
      */
     public static class MyBot extends BAPCBot
     {
         /**
          * Used for synchronization with the @Test method: it should wait at the
          * barrier until our bot is done, then execute the asserts.
          */
         CyclicBarrier barrier = new CyclicBarrier(2);
 
         /**
          * Default constructor is required for instantiation through Spring.
          */
         public MyBot(){
         }
 
         @Override
         protected void initializeEventListeners()
         {
             //of course, let the real bot implementation initialize itself
             super.initializeEventListeners();
             //now, add a listener to the WorkDoneEvent.
             //its only purpose is to trip the CyclicBarrier instance that
             // the test method is waiting on
             getEventBus().subscribe(WorkDoneEvent.class, new ExecuteOnEventListener(getEventListenerContext(), new Runnable(){
                 @Override
                 public void run()
                 {
                     try {
                         //together with the barrier.await() in the @Test method, this trips the barrier
                         //and both threads continue.
                         barrier.await();
                     } catch (Exception e) {
                         logger.warn("caught exception while waiting on barrier", e);
                     }
                 }
             }, RUN_ONCE));
         }
 
         public CyclicBarrier getBarrier()
         {
             return barrier;
         }
 
         /**
          * Here we check the results of the bot's execution.
          */
         public void executeAsserts()
         {
             //5 act events
             Assert.assertEquals(5, this.needCreator.getEventCount());
             Assert.assertEquals(0, this.needCreator.getExceptionCount());
             //5 create need events
             Assert.assertEquals(4, this.needConnector.getEventCount());
             Assert.assertEquals(0, this.needConnector.getExceptionCount());
             //4 connect, 4 open
             Assert.assertEquals(4, this.autoOpener.getEventCount());
             Assert.assertEquals(0, this.autoOpener.getExceptionCount());
             //10 messages
             Assert.assertEquals(10, this.autoResponder.getEventCount());
             Assert.assertEquals(0, this.autoResponder.getExceptionCount());
             //10 messages
             Assert.assertEquals(10, this.connectionCloser.getEventCount());
             Assert.assertEquals(0, this.connectionCloser.getExceptionCount());
             //1 close (one sent, one received - but for sending we create no event)
             Assert.assertEquals(1, this.needDeactivator.getEventCount());
             Assert.assertEquals(0, this.needDeactivator.getExceptionCount());
             //2 needs deactivated
             Assert.assertEquals(2, this.workDoneSignaller.getEventCount());
             Assert.assertEquals(0, this.workDoneSignaller.getExceptionCount());
 
             //TODO: there is more to check:
             //* what does the RDF look like?
             // --> pull it from the needURI/ConnectionURI and check contents
             //* what does the database look like?
         }
 
     }
 }
