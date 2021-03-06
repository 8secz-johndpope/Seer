 /*
  * Licensed to the Apache Software Foundation (ASF) under one
  * or more contributor license agreements.  See the NOTICE file
  * distributed with this work for additional information
  * regarding copyright ownership.  The ASF licenses this file
  * to you under the Apache License, Version 2.0 (the
  * "License"); you may not use this file except in compliance
  * with the License.  You may obtain a copy of the License at
  * 
  *   http://www.apache.org/licenses/LICENSE-2.0
  * 
  * Unless required by applicable law or agreed to in writing,
  * software distributed under the License is distributed on an
  * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
  * KIND, either express or implied.  See the License for the
  * specific language governing permissions and limitations
  * under the License.    
  */
 package org.komusubi.feeder.web;
 
 import java.io.File;
 import java.io.PrintStream;
 
 import org.komusubi.feeder.aggregator.scraper.WeatherAnnouncementScraper;
 import org.komusubi.feeder.aggregator.scraper.WeatherContentScraper;
 import org.komusubi.feeder.aggregator.scraper.WeatherTitleScraper;
 import org.komusubi.feeder.aggregator.site.RssSite;
 import org.komusubi.feeder.aggregator.topic.FeedTopic;
 import org.komusubi.feeder.aggregator.topic.WeatherTopic;
 import org.komusubi.feeder.model.Topic;
 import org.komusubi.feeder.model.Topics;
 import org.komusubi.feeder.sns.Speaker;
 import org.komusubi.feeder.sns.twitter.HashTag;
 import org.komusubi.feeder.sns.twitter.TweetMessage;
 import org.komusubi.feeder.sns.twitter.TweetMessage.TimestampFragment;
 import org.komusubi.feeder.sns.twitter.Twitter4j;
 import org.komusubi.feeder.sns.twitter.strategy.SleepStrategy;
 import org.komusubi.feeder.sns.twitter.strategy.SleepStrategy.FilePageCache;
 import org.komusubi.feeder.sns.twitter.strategy.SleepStrategy.PageCache;
 import org.komusubi.feeder.sns.twitter.strategy.SleepStrategy.PartialMatchPageCache;
 
 /**
  * @author jun.ozeki
  */
 public class StandAlone {
 //    private static final Logger logger = LoggerFactory.getLogger(StandAlone.class);
 
     private static void usage(PrintStream stream) {
         stream.printf("arguments must be \"scraper\" or \"feeder\"");
     }
 
     public static void main(String[] args) {
         if (args.length < 1) {
             usage(System.err);
             return;
         } 
         StandAlone standAlone = new StandAlone();
 
         Topics<Topic> topics;
        File storeFile = new File(System.getProperty("java.io.tmpdir") + "/feeder-store.txt");
         PageCache pageCache;
         if ("scraper".equalsIgnoreCase(args[0])) {
             topics = standAlone.aggregateScraper();
             pageCache = new PartialMatchPageCache(storeFile);
         } else if("feeder".equalsIgnoreCase(args[0])) {
             topics = standAlone.aggregateFeeder();
             pageCache = new FilePageCache(storeFile);
         } else {
             throw new IllegalArgumentException("arguments must be \"scraper\" or \"feeder\"");
         }
         // topic topic 
         Speaker speaker = new Speaker(new Twitter4j(), new SleepStrategy(1L, pageCache));
         speaker.talk(topics);
     }
 
     private Topics<Topic> aggregateScraper() {
 
         WeatherTopic weather = new WeatherTopic(new WeatherContentScraper(),
                                                  new WeatherTitleScraper(),
                                                  new WeatherAnnouncementScraper(),
                                                  new TweetMessage(new TimestampFragment("HHmm")));
         HashTag jal = new HashTag("jal");
         weather.addTag(jal);
 
         Topics<Topic> topics = new Topics<>();
         topics.add(weather);
         
         return topics;
     }
     
     private Topics<Topic> aggregateFeeder() {
         
         HashTag jal = new HashTag("jal");
         FeedTopic jalInfo = new FeedTopic(new RssSite("jal.info"), new TweetMessage());
         jalInfo.addTag(jal);
         
         FeedTopic jalDomestic = new FeedTopic(new RssSite("jal.domestic"), new TweetMessage());
         jalDomestic.addTag(jal);
 
         Topics<Topic> topics = new Topics<>();
         topics.add(jalInfo);
         topics.add(jalDomestic);
 
         return topics;
     }
 
 }
