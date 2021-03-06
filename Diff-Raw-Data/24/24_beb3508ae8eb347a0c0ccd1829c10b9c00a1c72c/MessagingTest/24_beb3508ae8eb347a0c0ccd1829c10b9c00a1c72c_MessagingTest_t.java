 package de.caluga.test.mongo.suite;
 
 import de.caluga.morphium.MorphiumSingleton;
 import de.caluga.morphium.Query;
 import de.caluga.morphium.messaging.MessageListener;
 import de.caluga.morphium.messaging.Messaging;
 import de.caluga.morphium.messaging.Msg;
 import de.caluga.morphium.messaging.MsgType;
 import org.junit.Test;
 
 import java.util.*;
 
 /**
  * User: Stephan Bösebeck
  * Date: 26.05.12
  * Time: 17:34
  * <p/>
  * TODO: Add documentation here
  */
 public class MessagingTest extends MongoTest {
     public boolean gotMessage = false;
 
     public boolean gotMessage1 = false;
     public boolean gotMessage2 = false;
     public boolean gotMessage3 = false;
     public boolean gotMessage4 = false;
 
     public String lastMsgId;
 
     public int procCounter = 0;
 
     @Test
     public void testMsgLifecycle() throws Exception {
         Msg m = new Msg();
         m.setSender("Meine wunderbare ID " + System.currentTimeMillis());
         m.setMsgId("My wonderful id");
         m.setName("A name");
         MorphiumSingleton.get().store(m);
         Thread.sleep(5000);
 
         assert (m.getTimestamp() > 0) : "Timestamp not updated?";
         assert (m.getType().equals(MsgType.SINGLE)) : "Default should be single?";
 
     }
 
 
     @Test
     public void messageQueueTest() throws Exception {
         MorphiumSingleton.get().clearCollection(Msg.class);
         String id = "meine ID";
 
 
         Msg m = new Msg("name", MsgType.SINGLE, "Msgid1", "value", 5000);
         m.setSender(id);
        m.setExclusive(true);
         MorphiumSingleton.get().store(m);
 
         Query<Msg> q = MorphiumSingleton.get().createQueryFor(Msg.class);
         MorphiumSingleton.get().delete(q);
         //locking messages...
         q = q.f(Msg.Fields.sender).ne(id).f(Msg.Fields.lockedBy).eq(null).f(Msg.Fields.lstOfIdsAlreadyProcessed).ne(id);
         MorphiumSingleton.get().set(Msg.class, q, Msg.Fields.lockedBy, id);
 
         q = q.q();
         q = q.f(Msg.Fields.lockedBy).eq(id);
         q.sort(Msg.Fields.timestamp);
 
         List<Msg> messagesList = q.asList();
         assert (messagesList.size() == 0) : "Got my own message?!?!?!" + messagesList.get(0).toString();
 
         m = new Msg("name", MsgType.SINGLE, "msgid2", "value", 5000);
         m.setSender("sndId2");
        m.setExclusive(true);
         MorphiumSingleton.get().store(m);
 
         q = MorphiumSingleton.get().createQueryFor(Msg.class);
         //locking messages...
         q = q.f(Msg.Fields.sender).ne(id).f(Msg.Fields.lockedBy).eq(null).f(Msg.Fields.lstOfIdsAlreadyProcessed).ne(id);
         MorphiumSingleton.get().set(Msg.class, q, Msg.Fields.lockedBy, id);
 
         q = q.q();
         q = q.f(Msg.Fields.lockedBy).eq(id);
         q.sort(Msg.Fields.timestamp);
 
         messagesList = q.asList();
         assert (messagesList.size() == 1) : "should get annother id - did not?!?!?!";
 
         log.info("Got msg: " + messagesList.get(0).toString());
 
     }
 
     @Test
     public void messagingTest() throws Exception {
         MorphiumSingleton.get().clearCollection(Msg.class);
 
         final Messaging messaging = new Messaging(MorphiumSingleton.get(), 500, true);
         messaging.start();
 
         messaging.addMessageListener(new MessageListener() {
             @Override
             public void onMessage(Msg m) {
                 log.info("Got Message: " + m.toString());
                 gotMessage = true;
             }
 
             @Override
             public void setMessaging(Messaging msg) {
                 //To change body of implemented methods use File | Settings | File Templates.
             }
 
         });
         messaging.storeMessage(new Msg("Testmessage", MsgType.MULTI, "A message", "the value - for now", 5000));
 
         Thread.sleep(1000);
         assert (!gotMessage) : "Message recieved from self?!?!?!";
         log.info("Dig not get own message - cool!");
 
         Msg m = new Msg("meine Message", MsgType.SINGLE, "The Message", "value is a string", 5000);
         m.setMsgId(UUID.randomUUID().toString());
         m.setSender("Another sender");
 
         MorphiumSingleton.get().store(m);
 
         Thread.sleep(1000);
         assert (gotMessage) : "Message did not come?!?!?";
 
         gotMessage = false;
         Thread.sleep(1000);
         assert (!gotMessage) : "Got message again?!?!?!";
 
         Thread.sleep(5000);
         assert (MorphiumSingleton.get().readAll(Msg.class).size() == 0) : "Still messages left?!?!?";
         messaging.setRunning(false);
         Thread.sleep(1000);
         assert (!messaging.isAlive()) : "Messaging still running?!?";
     }
 
 
     @Test
     public void systemTest() throws Exception {
         MorphiumSingleton.get().clearCollection(Msg.class);
         final Messaging m1 = new Messaging(MorphiumSingleton.get(), 500, true);
         final Messaging m2 = new Messaging(MorphiumSingleton.get(), 500, true);
         m1.start();
         m2.start();
 
         m1.addMessageListener(new MessageListener() {
             @Override
             public void onMessage(Msg m) {
                 gotMessage1 = true;
                 log.info("M1 got message " + m.toString());
 //                assert (m.getSender().equals(m2.getSenderId())) : "Sender is not M2?!?!? m2_id: " + m2.getSenderId() + " - message sender: " + m.getSender();
             }
 
             @Override
             public void setMessaging(Messaging msg) {
                 //To change body of implemented methods use File | Settings | File Templates.
             }
         });
 
         m2.addMessageListener(new MessageListener() {
             @Override
             public void onMessage(Msg m) {
                 gotMessage2 = true;
                 log.info("M2 got message " + m.toString());
 //                assert (m.getSender().equals(m1.getSenderId())) : "Sender is not M1?!?!? m1_id: " + m1.getSenderId() + " - message sender: " + m.getSender();
             }
 
             @Override
             public void setMessaging(Messaging msg) {
                 //To change body of implemented methods use File | Settings | File Templates.
             }
         });
 
         m1.storeMessage(new Msg("testmsg1", "The message from M1", "Value"));
         Thread.sleep(1000);
         assert (gotMessage2) : "Message not recieved yet?!?!?";
         gotMessage2 = false;
 
         m2.storeMessage(new Msg("testmsg2", "The message from M2", "Value"));
         Thread.sleep(1000);
         assert (gotMessage1) : "Message not recieved yet?!?!?";
         gotMessage1 = false;
 
         m1.setRunning(false);
         m2.setRunning(false);
         Thread.sleep(1000);
         assert (!m1.isAlive()) : "m1 still running?";
         assert (!m2.isAlive()) : "m2 still running?";
 
     }
 
     @Test
     public void severalSystemsTest() throws Exception {
         MorphiumSingleton.get().clearCollection(Msg.class);
         final Messaging m1 = new Messaging(MorphiumSingleton.get(), 100, true);
         final Messaging m2 = new Messaging(MorphiumSingleton.get(), 100, true);
         final Messaging m3 = new Messaging(MorphiumSingleton.get(), 100, true);
         final Messaging m4 = new Messaging(MorphiumSingleton.get(), 100, true);
 
         m1.start();
         m2.start();
         m3.start();
         m4.start();
 
         m1.addMessageListener(new MessageListener() {
             @Override
             public void onMessage(Msg m) {
                 gotMessage1 = true;
                 log.info("M1 got message " + m.toString());
 //                assert (m.getSender().equals(m2.getSenderId())) : "Sender is not M2?!?!? m2_id: " + m2.getSenderId() + " - message sender: " + m.getSender();
             }
 
             @Override
             public void setMessaging(Messaging msg) {
                 //To change body of implemented methods use File | Settings | File Templates.
             }
         });
 
         m2.addMessageListener(new MessageListener() {
             @Override
             public void onMessage(Msg m) {
                 gotMessage2 = true;
                 log.info("M2 got message " + m.toString());
 //                assert (m.getSender().equals(m1.getSenderId())) : "Sender is not M1?!?!? m1_id: " + m1.getSenderId() + " - message sender: " + m.getSender();
             }
 
             @Override
             public void setMessaging(Messaging msg) {
                 //To change body of implemented methods use File | Settings | File Templates.
             }
         });
 
         m3.addMessageListener(new MessageListener() {
             @Override
             public void onMessage(Msg m) {
                 gotMessage3 = true;
                 log.info("M3 got message " + m.toString());
 //                assert (m.getSender().equals(m1.getSenderId())) : "Sender is not M1?!?!? m1_id: " + m1.getSenderId() + " - message sender: " + m.getSender();
             }
 
             @Override
             public void setMessaging(Messaging msg) {
                 //To change body of implemented methods use File | Settings | File Templates.
             }
         });
 
         m4.addMessageListener(new MessageListener() {
             @Override
             public void onMessage(Msg m) {
                 gotMessage4 = true;
                 log.info("M4 got message " + m.toString());
 //                assert (m.getSender().equals(m1.getSenderId())) : "Sender is not M1?!?!? m1_id: " + m1.getSenderId() + " - message sender: " + m.getSender();
             }
 
             @Override
             public void setMessaging(Messaging msg) {
                 //To change body of implemented methods use File | Settings | File Templates.
             }
         });
 
         m1.queueMessage(new Msg("testmsg1", "The message from M1", "Value"));
         Thread.sleep(5000);
         assert (gotMessage2) : "Message not recieved yet by m2?!?!?";
         assert (gotMessage3) : "Message not recieved yet by m3?!?!?";
         assert (gotMessage4) : "Message not recieved yet by m4?!?!?";
         gotMessage1 = false;
         gotMessage2 = false;
         gotMessage3 = false;
         gotMessage4 = false;
 
         m2.queueMessage(new Msg("testmsg2", "The message from M2", "Value"));
         Thread.sleep(5000);
         assert (gotMessage1) : "Message not recieved yet by m1?!?!?";
         assert (gotMessage3) : "Message not recieved yet by m3?!?!?";
         assert (gotMessage4) : "Message not recieved yet by m4?!?!?";
         m1.setRunning(false);
         m2.setRunning(false);
         m3.setRunning(false);
         m4.setRunning(false);
         Thread.sleep(1000);
         assert (!m1.isAlive()) : "M1 still running";
         assert (!m2.isAlive()) : "M2 still running";
         assert (!m3.isAlive()) : "M3 still running";
         assert (!m4.isAlive()) : "M4 still running";
 
 
     }
 
     @Test
     public void directedMessageTest() throws Exception {
         MorphiumSingleton.get().clearCollection(Msg.class);
         final Messaging m1 = new Messaging(MorphiumSingleton.get(), 100, true);
         final Messaging m2 = new Messaging(MorphiumSingleton.get(), 100, true);
         final Messaging m3 = new Messaging(MorphiumSingleton.get(), 100, true);
 
         m1.start();
         m2.start();
         m3.start();
 
         log.info("m1 ID: " + m1.getSenderId());
         log.info("m2 ID: " + m2.getSenderId());
         log.info("m3 ID: " + m3.getSenderId());
 
         m1.addMessageListener(new MessageListener() {
             @Override
             public void onMessage(Msg m) {
                 gotMessage1 = true;
                 assert (m.getTo() == null || m.getTo().contains(m1.getSenderId())) : "wrongly received message?";
                 log.info("M1 got message " + m.toString());
 //                assert (m.getSender().equals(m2.getSenderId())) : "Sender is not M2?!?!? m2_id: " + m2.getSenderId() + " - message sender: " + m.getSender();
             }
 
             @Override
             public void setMessaging(Messaging msg) {
                 //To change body of implemented methods use File | Settings | File Templates.
             }
         });
 
         m2.addMessageListener(new MessageListener() {
             @Override
             public void onMessage(Msg m) {
                 gotMessage2 = true;
                 assert (m.getTo() == null || m.getTo().contains(m2.getSenderId())) : "wrongly received message?";
                 log.info("M2 got message " + m.toString());
 //                assert (m.getSender().equals(m1.getSenderId())) : "Sender is not M1?!?!? m1_id: " + m1.getSenderId() + " - message sender: " + m.getSender();
             }
 
             @Override
             public void setMessaging(Messaging msg) {
                 //To change body of implemented methods use File | Settings | File Templates.
             }
         });
 
         m3.addMessageListener(new MessageListener() {
             @Override
             public void onMessage(Msg m) {
                 gotMessage3 = true;
                 assert (m.getTo() == null || m.getTo().contains(m3.getSenderId())) : "wrongly received message?";
                 log.info("M3 got message " + m.toString());
 //                assert (m.getSender().equals(m1.getSenderId())) : "Sender is not M1?!?!? m1_id: " + m1.getSenderId() + " - message sender: " + m.getSender();
             }
 
             @Override
             public void setMessaging(Messaging msg) {
                 //To change body of implemented methods use File | Settings | File Templates.
             }
         });
 
         //sending message to all
         log.info("Sending broadcast message");
         m1.queueMessage(new Msg("testmsg1", "The message from M1", "Value"));
         Thread.sleep(5000);
         assert (gotMessage2) : "Message not recieved yet by m2?!?!?";
         assert (gotMessage3) : "Message not recieved yet by m3?!?!?";
         gotMessage1 = false;
         gotMessage2 = false;
         gotMessage3 = false;
 
         Thread.sleep(1500);
         assert (!gotMessage1) : "Message recieved again by m1?!?!?";
         assert (!gotMessage2) : "Message not recieved again by m2?!?!?";
         assert (!gotMessage3) : "Message not recieved again by m3?!?!?";
 
         log.info("Sending direct message");
         Msg m = new Msg("testmsg1", "The message from M1", "Value");
         m.addRecipient(m2.getSenderId());
         m1.queueMessage(m);
         Thread.sleep(2000);
         assert (gotMessage2) : "Message not received by m2?";
         assert (!gotMessage1) : "Message recieved by m1?!?!?";
         assert (!gotMessage3) : "Message not recieved again by m3?!?!?";
         gotMessage1 = false;
         gotMessage2 = false;
         gotMessage3 = false;
         Thread.sleep(2000);
         assert (!gotMessage1) : "Message recieved again by m1?!?!?";
         assert (!gotMessage2) : "Message not recieved again by m2?!?!?";
         assert (!gotMessage3) : "Message not recieved again by m3?!?!?";
 
 
         log.info("Sending message to 2 recipients");
         log.info("Sending direct message");
         m = new Msg("testmsg1", "The message from M1", "Value");
         m.addRecipient(m2.getSenderId());
         m.addRecipient(m3.getSenderId());
         m1.queueMessage(m);
         Thread.sleep(2000);
         assert (gotMessage2) : "Message not received by m2?";
         assert (!gotMessage1) : "Message recieved by m1?!?!?";
         assert (gotMessage3) : "Message not recieved by m3?!?!?";
         gotMessage1 = false;
         gotMessage2 = false;
         gotMessage3 = false;
         Thread.sleep(2000);
         assert (!gotMessage1) : "Message recieved again by m1?!?!?";
         assert (!gotMessage2) : "Message not recieved again by m2?!?!?";
         assert (!gotMessage3) : "Message not recieved again by m3?!?!?";
 
 
         m1.setRunning(false);
         m2.setRunning(false);
         m3.setRunning(false);
         Thread.sleep(1000);
     }
 
     @Test
     public void answeringTest() throws Exception {
         gotMessage1 = false;
         gotMessage2 = false;
         gotMessage3 = false;
 
         MorphiumSingleton.get().clearCollection(Msg.class);
         final Messaging m1 = new Messaging(MorphiumSingleton.get(), 100, true);
         final Messaging m2 = new Messaging(MorphiumSingleton.get(), 100, true);
         final Messaging onlyAnswers = new Messaging(MorphiumSingleton.get(), 100, true);
 
         m1.start();
         m2.start();
         onlyAnswers.start();
 
         log.info("m1 ID: " + m1.getSenderId());
         log.info("m2 ID: " + m2.getSenderId());
         log.info("onlyAnswers ID: " + onlyAnswers.getSenderId());
 
         m1.addMessageListener(new MessageListener() {
             @Override
             public void onMessage(Msg m) {
                 gotMessage1 = true;
                 assert (m.getTo() == null || m.getTo().contains(m1.getSenderId())) : "wrongly received message?";
                 assert (m.getInAnswerTo() == null) : "M1 got an answer, but did not ask?";
                 log.info("M1 got message " + m.toString());
                 Msg answer = m.createAnswerMsg();
                 answer.setValue("This is the answer from m1");
                 answer.addValue("something", new Date());
                 answer.addAdditional("String message from m1");
                 m.sendAnswer(m1, answer);
             }
 
             @Override
             public void setMessaging(Messaging msg) {
                 //To change body of implemented methods use File | Settings | File Templates.
             }
         });
 
         m2.addMessageListener(new MessageListener() {
             @Override
             public void onMessage(Msg m) {
                 gotMessage2 = true;
                 assert (m.getTo() == null || m.getTo().contains(m2.getSenderId())) : "wrongly received message?";
                 log.info("M2 got message " + m.toString());
                 assert (m.getInAnswerTo() == null) : "M2 got an answer, but did not ask?";
                 Msg answer = m.createAnswerMsg();
                 answer.setValue("This is the answer from m2");
                 answer.addValue("when", System.currentTimeMillis());
                 answer.addAdditional("Additional Value von m2");
                 m.sendAnswer(m2, answer);
             }
 
             @Override
             public void setMessaging(Messaging msg) {
                 //To change body of implemented methods use File | Settings | File Templates.
             }
         });
 
         onlyAnswers.addMessageListener(new MessageListener() {
             @Override
             public void onMessage(Msg m) {
                 gotMessage3 = true;
                 assert (m.getTo() == null || m.getTo().contains(onlyAnswers.getSenderId())) : "wrongly received message?";
                 assert (m.getInAnswerTo() != null) : "was not an answer? " + m.toString();
 
                 log.info("M3 got answer " + m.toString());
                 assert (m.getInAnswerTo().equals(lastMsgId)) : "Wrong answer????";
 //                assert (m.getSender().equals(m1.getSenderId())) : "Sender is not M1?!?!? m1_id: " + m1.getSenderId() + " - message sender: " + m.getSender();
             }
 
             @Override
             public void setMessaging(Messaging msg) {
                 //To change body of implemented methods use File | Settings | File Templates.
             }
         });
 
         Msg question = new Msg("QMsg", "This is the message text", "A question param");
         lastMsgId = question.getMsgId();
         onlyAnswers.storeMessage(question);
 
         log.info("Send Message with id: " + question.getMsgId());
         Thread.sleep(3000);
         assert (gotMessage3) : "no answer got back?";
         assert (gotMessage1) : "Question not received by m1";
         assert (gotMessage2) : "Question not received by m2";
         gotMessage1 = false;
         gotMessage2 = false;
         gotMessage3 = false;
         Thread.sleep(2000);
 
         assert (!gotMessage3 && !gotMessage1 && !gotMessage2) : "Message processing repeat?";
 
         Query<Msg> q = MorphiumSingleton.get().createQueryFor(Msg.class);
         long cnt = 0;
         for (int i = 0; i < 40; i++) {
             cnt = q.countAll();
             System.out.println("Messages in queue: " + cnt);
             if (cnt == 0) {
                 break;
             }
             Thread.sleep(1000);
         }
         assert (cnt == 0) : "Messages not processed yet?!?!?" + cnt;
 
         m1.setRunning(false);
         m2.setRunning(false);
         onlyAnswers.setRunning(false);
         Thread.sleep(1000);
     }
 
 
     @Test
     public void massiveMessagingTest() throws Exception {
         int numberOfWorkers = 10;
         int numberOfMessages = 200;
         long ttl = 15000; //15 sec
 
 
         MorphiumSingleton.get().clearCollection(Msg.class);
         List<Messaging> systems = new ArrayList<Messaging>();
 
         final Map<String, Integer> processedMessages = new Hashtable<String, Integer>();
 
         for (int i = 0; i < numberOfWorkers; i++) {
             //creating messaging instances
             Messaging m = new Messaging(MorphiumSingleton.get(), 100, true);
             m.start();
             systems.add(m);
             MessageListener l = new MessageListener() {
                 Messaging msg;
                 List<String> ids = new Vector<String>();
 
                 @Override
                 public void onMessage(Msg m) {
                     assert (!ids.contains(msg.getSenderId() + "/" + m.getMsgId())) : "Re-getting message?!?!? " + m.getMsgId() + " MyId: " + msg.getSenderId();
                     ids.add(msg.getSenderId() + "/" + m.getMsgId());
                     assert (m.getTo() == null || m.getTo().contains(msg.getSenderId())) : "got message not for me?";
                     assert (!m.getSender().equals(msg.getSenderId())) : "Got message from myself?";
                     synchronized (processedMessages) {
                         Integer pr = processedMessages.get(m.getMsgId());
                         if (pr == null) pr = 0;
                         processedMessages.put(m.getMsgId(), pr + 1);
                         procCounter++;
                     }
                 }
 
                 @Override
                 public void setMessaging(Messaging msg) {
                     this.msg = msg;
                 }
             };
             m.addMessageListener(l);
         }
 
         long start = System.currentTimeMillis();
         for (int i = 0; i < numberOfMessages; i++) {
             int m = (int) Math.random() * systems.size();
             Msg msg = new Msg("test" + i, MsgType.MULTI, "The message for msg " + i, "a value", ttl);
             msg.addAdditional("Additional Value " + i);
             msg.setExclusive(false);
             systems.get(m).queueMessage(msg);
         }
 
         long dur = System.currentTimeMillis() - start;
         log.info("Queueing " + numberOfMessages + " messages took " + dur + " ms - now waiting for writes..");
         waitForWrites();
         log.info("...all messages persisted!");
         int last = 0;
         //See if whole number of messages processed is correct
         //keep in mind: a message is never recieved by the sender, hence numberOfWorkers-1
         while (true) {
             if (procCounter == numberOfMessages * (numberOfWorkers - 1)) {
                 break;
             }
             if (last == procCounter) {
                 log.info("No change in procCounter?! somethings wrong...");
                 break;
 
             }
             last = procCounter;
             log.info("Waiting for messages to be processed - procCounter: " + procCounter);
             Thread.sleep(2000);
         }
         Thread.sleep(1000);
         log.info("done");
 
         assert (processedMessages.size() == numberOfMessages) : "sent " + numberOfMessages + " messages, but only " + processedMessages.size() + " were recieved?";
         for (String id : processedMessages.keySet()) {
             assert (processedMessages.get(id) == numberOfWorkers - 1) : "Message " + id + " was not recieved by all " + (numberOfWorkers - 1) + " other workers? only by " + processedMessages.get(id);
         }
         assert (procCounter == numberOfMessages * (numberOfWorkers - 1)) : "Still processing messages?!?!?";
 
         //Waiting for all messages to be outdated and deleted
         Query<Msg> q = MorphiumSingleton.get().createQueryFor(Msg.class);
         long cnt = 0;
         for (int i = 0; i < ttl / 1000 * 2; i++) {
             cnt = q.countAll();
             System.out.println("Messages in queue: " + cnt);
             if (cnt == 0) {
                 break;
             }
             Thread.sleep(1000);
         }
         assert (cnt == 0) : "Messages not processed yet?!?!?" + cnt;
 
         //Stopping all
         for (Messaging m : systems) {
             m.setRunning(false);
         }
         Thread.sleep(1000);
         for (Messaging m : systems) {
             assert (!m.isAlive()) : "Thread still running?";
         }
 
 
     }
 
     @Test
     public void broadcastTest() throws Exception {
         MorphiumSingleton.get().clearCollection(Msg.class);
         final Messaging m1 = new Messaging(MorphiumSingleton.get(), 1000, true);
         final Messaging m2 = new Messaging(MorphiumSingleton.get(), 1000, true);
         final Messaging m3 = new Messaging(MorphiumSingleton.get(), 1000, true);
 
         m1.start();
         m2.start();
         m3.start();
 
         log.info("m1 ID: " + m1.getSenderId());
         log.info("m2 ID: " + m2.getSenderId());
         log.info("m3 ID: " + m3.getSenderId());
 
         m1.addMessageListener(new MessageListener() {
             @Override
             public void onMessage(Msg m) {
                 gotMessage1 = true;
                 assert (m.getTo() == null || m.getTo().contains(m1.getSenderId())) : "wrongly received message?";
                 log.info("M1 got message " + m.toString());
 //                assert (m.getSender().equals(m2.getSenderId())) : "Sender is not M2?!?!? m2_id: " + m2.getSenderId() + " - message sender: " + m.getSender();
             }
 
             @Override
             public void setMessaging(Messaging msg) {
                 //To change body of implemented methods use File | Settings | File Templates.
             }
         });
 
         m2.addMessageListener(new MessageListener() {
             @Override
             public void onMessage(Msg m) {
                 gotMessage2 = true;
                 assert (m.getTo() == null || m.getTo().contains(m2.getSenderId())) : "wrongly received message?";
                 log.info("M2 got message " + m.toString());
 //                assert (m.getSender().equals(m1.getSenderId())) : "Sender is not M1?!?!? m1_id: " + m1.getSenderId() + " - message sender: " + m.getSender();
             }
 
             @Override
             public void setMessaging(Messaging msg) {
                 //To change body of implemented methods use File | Settings | File Templates.
             }
         });
 
         m3.addMessageListener(new MessageListener() {
             @Override
             public void onMessage(Msg m) {
                 gotMessage3 = true;
                 assert (m.getTo() == null || m.getTo().contains(m3.getSenderId())) : "wrongly received message?";
                 log.info("M3 got message " + m.toString());
 //                assert (m.getSender().equals(m1.getSenderId())) : "Sender is not M1?!?!? m1_id: " + m1.getSenderId() + " - message sender: " + m.getSender();
             }
 
             @Override
             public void setMessaging(Messaging msg) {
                 //To change body of implemented methods use File | Settings | File Templates.
             }
         });
 
         Msg m = new Msg("test", "A message", "a value");
         m.setExclusive(false);
         m1.storeMessage(m);
 
         Thread.sleep(1200);
         assert (!gotMessage1) : "Got message again?";
         assert (gotMessage2) : "m2 did not get msg?";
         assert (gotMessage3) : "m3 did not get msg";
         gotMessage2 = false;
         gotMessage3 = false;
         Thread.sleep(1200);
         assert (!gotMessage1) : "Got message again?";
         assert (!gotMessage2) : "m2 did get msg again?";
         assert (!gotMessage3) : "m3 did get msg again?";

        m1.setRunning(false);
        m2.setRunning(false);
        m3.setRunning(false);
        Thread.sleep(1000);
     }
 
 
 }
