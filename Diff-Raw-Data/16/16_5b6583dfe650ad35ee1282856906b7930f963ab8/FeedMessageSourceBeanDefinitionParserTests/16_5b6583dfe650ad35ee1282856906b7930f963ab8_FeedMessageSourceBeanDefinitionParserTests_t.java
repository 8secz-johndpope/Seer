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
 package org.springframework.integration.feed.config;
 
 import static junit.framework.Assert.assertEquals;
 import static junit.framework.Assert.assertTrue;
 import static org.mockito.Mockito.atLeast;
 import static org.mockito.Mockito.spy;
 import static org.mockito.Mockito.times;
 import static org.mockito.Mockito.verify;
 
 import java.io.File;
 import java.util.Properties;
 import java.util.concurrent.CountDownLatch;
 import java.util.concurrent.TimeUnit;
 
 import org.junit.Before;
 import org.junit.Ignore;
 import org.junit.Test;
 import org.mockito.Mockito;
 import org.springframework.context.ApplicationContext;
 import org.springframework.context.support.ClassPathXmlApplicationContext;
 import org.springframework.integration.Message;
 import org.springframework.integration.MessagingException;
 import org.springframework.integration.channel.DirectChannel;
 import org.springframework.integration.context.metadata.MetadataStore;
 import org.springframework.integration.core.MessageHandler;
 import org.springframework.integration.endpoint.SourcePollingChannelAdapter;
 import org.springframework.integration.feed.FeedEntryReaderMessageSource;
 import org.springframework.integration.feed.FeedReaderMessageSource;
 import org.springframework.integration.feed.FileUrlFeedFetcher;
 import org.springframework.integration.history.MessageHistory;
 import org.springframework.integration.test.util.TestUtils;
 
 import com.sun.syndication.feed.synd.SyndEntry;
 import com.sun.syndication.fetcher.impl.AbstractFeedFetcher;
 import com.sun.syndication.fetcher.impl.HttpURLFeedFetcher;
 
 /**
  * @author Oleg Zhurakousky
  *
  */
 public class FeedMessageSourceBeanDefinitionParserTests {
 	private static CountDownLatch latch;
 	@Before
 	public void prepare(){
		File persisterFile = new File(System.getProperty("java.io.tmpdir") + "/spring-integration/", "feedAdapter.last.entry");
 		if (persisterFile.exists()){
 			persisterFile.delete();
 		}
 	}
 
 	@Test
 	public void validateSuccessfullConfigurationWithCustomMetastore(){
 		ClassPathXmlApplicationContext context = 
 			new ClassPathXmlApplicationContext("FeedMessageSourceBeanDefinitionParserTests-file-context.xml", this.getClass());
 		SourcePollingChannelAdapter adapter = context.getBean("feedAdapter", SourcePollingChannelAdapter.class);
 		FeedEntryReaderMessageSource source = (FeedEntryReaderMessageSource) TestUtils.getPropertyValue(adapter, "source");
 		MetadataStore metaStore = (MetadataStore) TestUtils.getPropertyValue(source, "metadataStore");
 		assertTrue(metaStore instanceof SampleMetadataStore);
 		FeedReaderMessageSource feedReaderMessageSource = (FeedReaderMessageSource) TestUtils.getPropertyValue(source, "feedReaderMessageSource");
 		AbstractFeedFetcher fetcher = (AbstractFeedFetcher) TestUtils.getPropertyValue(feedReaderMessageSource, "fetcher");
 		assertTrue(fetcher instanceof FileUrlFeedFetcher);
 		
 		context = 
 			new ClassPathXmlApplicationContext("FeedMessageSourceBeanDefinitionParserTests-http-context.xml", this.getClass());
 		adapter = context.getBean("feedAdapter", SourcePollingChannelAdapter.class);
 		source = (FeedEntryReaderMessageSource) TestUtils.getPropertyValue(adapter, "source");
 		feedReaderMessageSource = (FeedReaderMessageSource) TestUtils.getPropertyValue(source, "feedReaderMessageSource");
 		fetcher = (AbstractFeedFetcher) TestUtils.getPropertyValue(feedReaderMessageSource, "fetcher");
 		assertTrue(fetcher instanceof HttpURLFeedFetcher);
 		context.destroy();
 	}
 	
 	@Test
 	public void validateSuccessfullNewsRetrievalWithFileUrlAndMessageHistory() throws Exception{
		File persisterFile = new File(System.getProperty("java.io.tmpdir") + "/spring-integration/", "feedAdapterUsage.last.entry");
 		if (persisterFile.exists()){
 			persisterFile.delete();
 		}
 		//Test file samples.rss has 3 news items
 		latch = spy(new CountDownLatch(3));
 		ClassPathXmlApplicationContext context = 
 			new ClassPathXmlApplicationContext("FeedMessageSourceBeanDefinitionParserTests-file-usage-context.xml", this.getClass());	
 		latch.await(5, TimeUnit.SECONDS);
 		verify(latch, times(3)).countDown();
 		context.destroy();
 		
 		// since we are not deleting the persister file
 		// in this iteration no new feeds will be received and the latch will timeout
 		latch = spy(new CountDownLatch(3));
 		context = 
 			new ClassPathXmlApplicationContext("FeedMessageSourceBeanDefinitionParserTests-file-usage-context.xml", this.getClass());	
 		latch.await(5, TimeUnit.SECONDS);
 		verify(latch, times(0)).countDown();
 		context.destroy();
 	}
 	@Test
 	public void validateSuccessfullNewsRetrievalWithFileUrlNoPersistentIdentifier() throws Exception{
 		//Test file samples.rss has 3 news items
 		latch = spy(new CountDownLatch(3));
 		ClassPathXmlApplicationContext context = 
 			new ClassPathXmlApplicationContext("FeedMessageSourceBeanDefinitionParserTests-file-usage-noid-context.xml", this.getClass());	
 		latch.await(5, TimeUnit.SECONDS);
 		verify(latch, times(3)).countDown();
 		context.destroy();
 		
 		// since we are not deleting the persister file
 		// in this iteration no new feeds will be received and the latch will timeout
 		latch = spy(new CountDownLatch(3));
 		context = 
 			new ClassPathXmlApplicationContext("FeedMessageSourceBeanDefinitionParserTests-file-usage-noid-context.xml", this.getClass());	
 		latch.await(5, TimeUnit.SECONDS);
 		verify(latch, times(3)).countDown();
 		context.destroy();
 	}
 	
 	@Test
 	@Ignore // goes against the real feed
 	public void validateSuccessfullNewsRetrievalWithHttpUrl() throws Exception{
 		final CountDownLatch latch = new CountDownLatch(3);
 		MessageHandler handler = spy(new MessageHandler() {		
 			public void handleMessage(Message<?> message) throws MessagingException {
 				latch.countDown();
 			}
 		});
 		ApplicationContext context = 
 			new ClassPathXmlApplicationContext("FeedMessageSourceBeanDefinitionParserTests-http-context.xml", this.getClass());
 		DirectChannel feedChannel = context.getBean("feedChannel", DirectChannel.class);	
 		feedChannel.subscribe(handler);
 		latch.await(5, TimeUnit.SECONDS);
 		verify(handler, atLeast(3)).handleMessage(Mockito.any(Message.class));
 	}
 	
 	public static class SampleService{
 		public void receiveFeedEntry(Message<?> message){
 			MessageHistory history = MessageHistory.read(message);
 			assertTrue(history.size() == 3);
 			Properties historyItem = history.get(0);
 			assertEquals("feedAdapterUsage", historyItem.get("name"));
 			assertEquals("feed:inbound-channel-adapter", historyItem.get("type"));
 			
 			historyItem = history.get(1);
 			assertEquals("feedChannelUsage", historyItem.get("name"));
 			assertEquals("channel", historyItem.get("type"));
 			
 			historyItem = history.get(2);
 			assertEquals("sampleActivator", historyItem.get("name"));
 			assertEquals("service-activator", historyItem.get("type"));
 			latch.countDown();
 		}
 	}
 	
 	public static class SampleServiceNoHistory{
 		public void receiveFeedEntry(SyndEntry entry){
 			latch.countDown();
 		}
 	}
 	
 	public static class SampleMetadataStore implements MetadataStore{
 
 		public void write(Properties metadata) {
 		}
 
 		public Properties load() {
 			return new Properties();
 		}
 	}
 }
