 /*
  * Copyright 2002-2010 the original author or authors.
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
 package org.springframework.integration.mail;
 
 import static junit.framework.Assert.assertEquals;
 import static junit.framework.Assert.assertNotNull;
 import static org.mockito.Mockito.doAnswer;
 import static org.mockito.Mockito.mock;
 import static org.mockito.Mockito.spy;
 import static org.mockito.Mockito.times;
 import static org.mockito.Mockito.verify;
 import static org.mockito.Mockito.when;
 
 import java.util.Properties;
 
 import javax.mail.Flags;
 import javax.mail.Flags.Flag;
 import javax.mail.Folder;
 import javax.mail.Message;
 import javax.mail.internet.MimeMessage;
 
 import org.junit.Test;
 import org.mockito.Mockito;
 import org.mockito.invocation.InvocationOnMock;
 import org.mockito.stubbing.Answer;
 import org.springframework.beans.DirectFieldAccessor;
 import org.springframework.context.ApplicationContext;
 import org.springframework.context.support.ClassPathXmlApplicationContext;
 import org.springframework.integration.core.PollableChannel;
 import org.springframework.integration.history.MessageHistory;
 import org.springframework.integration.mail.config.ImapIdleChannelAdapterParserTests;
 import org.springframework.integration.test.util.TestUtils;
 
 import com.sun.mail.imap.IMAPFolder;
 
 /**
  * @author Oleg Zhurakousky
  *
  */
 public class ImapMailReceiverTests {
 	
 	@Test
 	public void receiveAndMarkAsReadDontDelete() throws Exception{
 		AbstractMailReceiver receiver = new ImapMailReceiver();
 		((ImapMailReceiver)receiver).setShouldMarkMessagesAsRead(true);
 		receiver = spy(receiver);
 		receiver.afterPropertiesSet();
 		Message msg1 = mock(MimeMessage.class);
 		Message msg2 = mock(MimeMessage.class);
 		final Message[] messages = new Message[]{msg1, msg2};
 		
 		doAnswer(new Answer<Object>() {
 			public Object answer(InvocationOnMock invocation) throws Throwable {
 				DirectFieldAccessor accessor = new DirectFieldAccessor(invocation.getMock());
 				int folderOpenMode = (Integer) accessor.getPropertyValue("folderOpenMode");
 				if (folderOpenMode != Folder.READ_WRITE){
 					throw new IllegalArgumentException("Folder had to be open in READ_WRITE mode");
 				}
 				return null;
 			}
 		}).when(receiver).openFolder();
 		
 		doAnswer(new Answer<Object>() {
 			public Object answer(InvocationOnMock invocation) throws Throwable {
 				return messages;
 			}
 		}).when(receiver).searchForNewMessages();
 		
 		doAnswer(new Answer<Object>() {
 			public Object answer(InvocationOnMock invocation) throws Throwable {
 				return null;
 			}
 		}).when(receiver).fetchMessages(messages);
 		receiver.receive();
 		verify(msg1, times(1)).setFlag(Flag.SEEN, true);
 		verify(msg2, times(1)).setFlag(Flag.SEEN, true);
 		verify(receiver, times(0)).deleteMessages((Message[]) Mockito.any());
 	}
 	@Test
 	public void receiveMarkAsReadAndDelete() throws Exception{
 		AbstractMailReceiver receiver = new ImapMailReceiver();
 		((ImapMailReceiver)receiver).setShouldMarkMessagesAsRead(true);
 		receiver.setShouldDeleteMessages(true);
 		receiver = spy(receiver);
 		receiver.afterPropertiesSet();
 		Message msg1 = mock(MimeMessage.class);
 		Message msg2 = mock(MimeMessage.class);
 		final Message[] messages = new Message[]{msg1, msg2};
 		doAnswer(new Answer<Object>() {
 			public Object answer(InvocationOnMock invocation) throws Throwable {
 				DirectFieldAccessor accessor = new DirectFieldAccessor(invocation.getMock());
 				int folderOpenMode = (Integer) accessor.getPropertyValue("folderOpenMode");
 				if (folderOpenMode != Folder.READ_WRITE){
 					throw new IllegalArgumentException("Folder had to be open in READ_WRITE mode");
 				}
 				return null;
 			}
 		}).when(receiver).openFolder();
 		
 		doAnswer(new Answer<Object>() {
 			public Object answer(InvocationOnMock invocation) throws Throwable {
 				return messages;
 			}
 		}).when(receiver).searchForNewMessages();
 		
 		doAnswer(new Answer<Object>() {
 			public Object answer(InvocationOnMock invocation) throws Throwable {
 				return null;
 			}
 		}).when(receiver).fetchMessages(messages);
 		receiver.receive();
 		verify(msg1, times(1)).setFlag(Flag.SEEN, true);
 		verify(msg2, times(1)).setFlag(Flag.SEEN, true);
 		verify(receiver, times(1)).deleteMessages((Message[]) Mockito.any());
 	}
 	@Test
 	public void receiveAndDontMarkAsRead() throws Exception{
 		AbstractMailReceiver receiver = new ImapMailReceiver();
 		((ImapMailReceiver)receiver).setShouldMarkMessagesAsRead(false);
 		receiver = spy(receiver);
 		receiver.afterPropertiesSet();
 		Message msg1 = mock(MimeMessage.class);
 		Message msg2 = mock(MimeMessage.class);
 		final Message[] messages = new Message[]{msg1, msg2};
 		doAnswer(new Answer<Object>() {
 			public Object answer(InvocationOnMock invocation) throws Throwable {
 				DirectFieldAccessor accessor = new DirectFieldAccessor(invocation.getMock());
 				int folderOpenMode = (Integer) accessor.getPropertyValue("folderOpenMode");
 				if (folderOpenMode == Folder.READ_WRITE){
 					throw new IllegalArgumentException("Folder had to be open in READ_ONLY mode");
 				}
 				return null;
 			}
 		}).when(receiver).openFolder();
 		
 		doAnswer(new Answer<Object>() {
 			public Object answer(InvocationOnMock invocation) throws Throwable {
 				return messages;
 			}
 		}).when(receiver).searchForNewMessages();
 		
 		doAnswer(new Answer<Object>() {
 			public Object answer(InvocationOnMock invocation) throws Throwable {
 				return null;
 			}
 		}).when(receiver).fetchMessages(messages);
 		receiver.afterPropertiesSet();
 		receiver.receive();
 		verify(msg1, times(0)).setFlag(Flag.SEEN, true);
 		verify(msg2, times(0)).setFlag(Flag.SEEN, true);
 	}
 	@Test
 	public void receiveAndDontMarkAsReadButDelete() throws Exception{
		AbstractMailReceiver receiver = new ImapMailReceiver();
		((ImapMailReceiver)receiver).setShouldDeleteMessages(true);
		((ImapMailReceiver)receiver).setShouldMarkMessagesAsRead(false);
 		receiver = spy(receiver);
 		receiver.afterPropertiesSet();
 		Message msg1 = mock(MimeMessage.class);
 		Message msg2 = mock(MimeMessage.class);
 		final Message[] messages = new Message[]{msg1, msg2};
 		doAnswer(new Answer<Object>() {
 			public Object answer(InvocationOnMock invocation) throws Throwable {
 				DirectFieldAccessor accessor = new DirectFieldAccessor(invocation.getMock());
 				int folderOpenMode = (Integer) accessor.getPropertyValue("folderOpenMode");
 				if (folderOpenMode != Folder.READ_WRITE){
 					throw new IllegalArgumentException("Folder had to be open in READ_WRITE mode");
 				}
 				return null;
 			}
 		}).when(receiver).openFolder();
 		
 		doAnswer(new Answer<Object>() {
 			public Object answer(InvocationOnMock invocation) throws Throwable {
 				return messages;
 			}
 		}).when(receiver).searchForNewMessages();
 		
 		doAnswer(new Answer<Object>() {
 			public Object answer(InvocationOnMock invocation) throws Throwable {
 				return null;
 			}
 		}).when(receiver).fetchMessages(messages);
 		receiver.afterPropertiesSet();
 		receiver.receive();
 		verify(msg1, times(0)).setFlag(Flag.SEEN, true);
 		verify(msg2, times(0)).setFlag(Flag.SEEN, true);
 		verify(msg1, times(1)).setFlag(Flag.DELETED, true);
 		verify(msg2, times(1)).setFlag(Flag.DELETED, true);
 	}
 	@Test
 	public void receiveAndIgnoreMarkAsReadDontDelete() throws Exception{
 		AbstractMailReceiver receiver = new ImapMailReceiver();
 		receiver = spy(receiver);
 		receiver.afterPropertiesSet();
 		Message msg1 = mock(MimeMessage.class);
 		Message msg2 = mock(MimeMessage.class);
 		final Message[] messages = new Message[]{msg1, msg2};
 		doAnswer(new Answer<Object>() {
 			public Object answer(InvocationOnMock invocation) throws Throwable {
 				DirectFieldAccessor accessor = new DirectFieldAccessor(invocation.getMock());
 				int folderOpenMode = (Integer) accessor.getPropertyValue("folderOpenMode");
 				if (folderOpenMode != Folder.READ_WRITE){
 					throw new IllegalArgumentException("Folder had to be open in READ_WRITE mode");
 				}
 				return null;
 			}
 		}).when(receiver).openFolder();
 		
 		doAnswer(new Answer<Object>() {
 			public Object answer(InvocationOnMock invocation) throws Throwable {
 				return messages;
 			}
 		}).when(receiver).searchForNewMessages();
 		
 		doAnswer(new Answer<Object>() {
 			public Object answer(InvocationOnMock invocation) throws Throwable {
 				return null;
 			}
 		}).when(receiver).fetchMessages(messages);
 		receiver.receive();
 		verify(msg1, times(1)).setFlag(Flag.SEEN, true);
 		verify(msg2, times(1)).setFlag(Flag.SEEN, true);
 		verify(receiver, times(0)).deleteMessages((Message[]) Mockito.any());
 	}
 	@Test
 	public void testMessageHistory() throws Exception{
 		ApplicationContext context = 
 			new ClassPathXmlApplicationContext("ImapIdleChannelAdapterParserTests-context.xml", ImapIdleChannelAdapterParserTests.class);
 		ImapIdleChannelAdapter adapter = context.getBean("simpleAdapter", ImapIdleChannelAdapter.class);
 		
 		AbstractMailReceiver receiver = new ImapMailReceiver();
 		receiver = spy(receiver);
 		receiver.afterPropertiesSet();
 		DirectFieldAccessor adapterAccessor = new DirectFieldAccessor(adapter);
 		adapterAccessor.setPropertyValue("mailReceiver", receiver);
 	
 		MimeMessage mailMessage = mock(MimeMessage.class);
 		Flags flags = mock(Flags.class);
 		when(mailMessage.getFlags()).thenReturn(flags);
 		final Message[] messages = new Message[]{mailMessage};
 		
 		doAnswer(new Answer<Object>() {
 			public Object answer(InvocationOnMock invocation) throws Throwable {
 				DirectFieldAccessor accesor = new DirectFieldAccessor((invocation.getMock()));
                 IMAPFolder folder = mock(IMAPFolder.class);
 				accesor.setPropertyValue("folder", folder);
 				when(folder.hasNewMessages()).thenReturn(true);
 				return null;
 			}
 		}).when(receiver).openFolder();
 		
 		doAnswer(new Answer<Object>() {
 			public Object answer(InvocationOnMock invocation) throws Throwable {
 				return messages;
 			}
 		}).when(receiver).searchForNewMessages();
 		
 		doAnswer(new Answer<Object>() {
 			public Object answer(InvocationOnMock invocation) throws Throwable {
 				return null;
 			}
 		}).when(receiver).fetchMessages(messages);
 		
 		PollableChannel channel = context.getBean("channel", PollableChannel.class);
 
 		adapter.start();
 		org.springframework.integration.Message<?> replMessage = channel.receive(10000);
 		MessageHistory history = MessageHistory.read(replMessage);
 		assertNotNull(history);
 		Properties componentHistoryRecord = TestUtils.locateComponentInHistory(history, "simpleAdapter", 0);
 		assertNotNull(componentHistoryRecord);
 		assertEquals("mail:imap-idle-channel-adapter", componentHistoryRecord.get("type"));
 	}
 }
