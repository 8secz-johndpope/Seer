 package com.microsoft.windowsazure.services.serviceBus;
 
 import static com.microsoft.windowsazure.services.serviceBus.Util.*;
 
 import org.junit.Before;
 import org.junit.BeforeClass;
 
 import com.microsoft.windowsazure.services.serviceBus.models.QueueInfo;
 import com.microsoft.windowsazure.services.serviceBus.models.ReceiveMessageOptions;
 import com.microsoft.windowsazure.services.serviceBus.models.TopicInfo;
 
 public abstract class IntegrationTestBase {
     @BeforeClass
     public static void initializeSystem() {
     }
 
     @Before
     public void initialize() throws Exception {
 
         boolean testAlphaExists = false;
         ServiceBusContract service = ServiceBusService.create();
         for (QueueInfo queue : iterateQueues(service)) {
             String queueName = queue.getPath();
             if (queueName.startsWith("Test") || queueName.startsWith("test")) {
                 if (queueName.equalsIgnoreCase("TestAlpha")) {
                     testAlphaExists = true;
                     long count = queue.getMessageCount();
                     for (long i = 0; i != count; ++i) {
                         service.receiveQueueMessage(queueName, new ReceiveMessageOptions().setTimeout(20));
                     }
                 }
                 else {
                     service.deleteQueue(queueName);
                 }
             }
         }
         for (TopicInfo topic : iterateTopics(service)) {
             String topicName = topic.getPath();
             if (topicName.startsWith("Test") || topicName.startsWith("test")) {
                 service.deleteQueue(topicName);
             }
         }
         if (!testAlphaExists) {
             service.createQueue(new QueueInfo("TestAlpha"));
         }
     }
 }
