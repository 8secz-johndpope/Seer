 /*
  * Copyright 2002-2007 the original author or authors.
  *
  * Licensed under the Apache License, Version 2.0 (the "License");
  * you may not use this file except in compliance with the License.
  * You may obtain a copy of the License at
  *
  *      http://www.apache.org/licenses/LICENSE-2.0
  *
  * Unless required by applicable law or agreed to in writing, software
  * distributed under the License is distributed on an "AS IS" BASIS,
  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  * See the License for the specific language governing permissions and
  * limitations under the License.
  */
 
 package org.springframework.integration.bus;
 
 import static org.junit.Assert.assertEquals;
 import static org.junit.Assert.assertNull;
 import static org.junit.Assert.assertTrue;
 
 import org.junit.Test;
 
 import org.springframework.context.support.ClassPathXmlApplicationContext;
 import org.springframework.integration.channel.MessageChannel;
 import org.springframework.integration.channel.PointToPointChannel;
 import org.springframework.integration.endpoint.GenericMessageEndpoint;
 import org.springframework.integration.message.GenericMessage;
 import org.springframework.integration.message.Message;
 import org.springframework.integration.message.StringMessage;
 
 /**
  * @author Mark Fisher
  */
 public class MessageBusTests {
 
 	@Test
 	public void testChannelsConnectedWithEndpoint() {
 		MessageBus bus = new MessageBus();
 		MessageChannel sourceChannel = new PointToPointChannel();
 		MessageChannel targetChannel = new PointToPointChannel();
 		bus.registerChannel("sourceChannel", sourceChannel);
 		sourceChannel.send(new StringMessage("123", "test"));
 		bus.registerChannel("targetChannel", targetChannel);
 		GenericMessageEndpoint endpoint = new GenericMessageEndpoint();
 		endpoint.setInputChannelName("sourceChannel");
 		endpoint.setDefaultOutputChannelName("targetChannel");
 		bus.registerEndpoint("endpoint", endpoint);
 		bus.start();
 		Message<?> result = targetChannel.receive(1000);
 		assertEquals("test", result.getPayload());
 		bus.stop();
 	}
 
 	@Test
 	public void testChannelsWithoutEndpoint() {
 		MessageBus bus = new MessageBus();
 		MessageChannel sourceChannel = new PointToPointChannel();
 		sourceChannel.send(new StringMessage("123", "test"));
 		MessageChannel targetChannel = new PointToPointChannel();
 		bus.registerChannel("sourceChannel", sourceChannel);
 		bus.registerChannel("targetChannel", targetChannel);
 		bus.start();
		Message<?> result = targetChannel.receive(500);
 		assertNull(result);
 		bus.stop();
 	}
 
 	@Test
 	public void testAutodetectionWithApplicationContext() {
 		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext("messageBusTests.xml", this.getClass());
 		context.start();
 		MessageChannel sourceChannel = (MessageChannel) context.getBean("sourceChannel");
 		sourceChannel.send(new GenericMessage<String>("123", "test"));		
 		MessageChannel targetChannel = (MessageChannel) context.getBean("targetChannel");
 		MessageBus bus = (MessageBus) context.getBean("bus");
 		ConsumerPolicy policy = new ConsumerPolicy();
 		Subscription subscription = new Subscription();
 		subscription.setChannel("sourceChannel");
 		subscription.setEndpoint("endpoint");
 		subscription.setPolicy(policy);
 		bus.activateSubscription(subscription);
		Message<?> result = targetChannel.receive(500);
 		assertEquals("test", result.getPayload());
 	}
 
 	@Test
 	public void testExactlyOneEndpointReceivesUnicastMessage() {
 		PointToPointChannel inputChannel = new PointToPointChannel();
 		PointToPointChannel outputChannel1 = new PointToPointChannel();
 		PointToPointChannel outputChannel2 = new PointToPointChannel();
 		GenericMessageEndpoint endpoint1 = new GenericMessageEndpoint();
 		endpoint1.setDefaultOutputChannelName("output1");
 		endpoint1.setInputChannelName("input");
 		GenericMessageEndpoint endpoint2 = new GenericMessageEndpoint();
 		endpoint2.setDefaultOutputChannelName("output2");
 		endpoint2.setInputChannelName("input");
 		MessageBus bus = new MessageBus();
 		bus.registerChannel("input", inputChannel);
 		bus.registerChannel("output1", outputChannel1);
 		bus.registerChannel("output2", outputChannel2);
 		bus.registerEndpoint("endpoint1", endpoint1);
 		bus.registerEndpoint("endpoint2", endpoint2);
 		bus.start();
 		inputChannel.send(new StringMessage(1, "testing"));
 		Message<?> message1 = outputChannel1.receive(100);
 		Message<?> message2 = outputChannel2.receive(0);
 		bus.stop();
 		assertTrue("exactly one message should be null", message1 == null ^ message2 == null);
 	}
 
 }
