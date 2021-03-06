 /**
  * Copyright (c) 2012-2012 Malhar, Inc.
  * All rights reserved.
  */
 package com.malhartech.demos.twitter;
 
 import com.malhartech.api.DAG;
 import com.malhartech.api.Operator.InputPort;
 import com.malhartech.lib.algo.UniqueCounter;
 import com.malhartech.lib.algo.WindowedTopCounter;
 import com.malhartech.lib.io.ConsoleOutputOperator;
 import com.malhartech.lib.io.HttpOutputOperator;
 import java.io.IOException;
 import java.net.URI;
 import java.util.Properties;
 import org.apache.hadoop.conf.Configuration;
 import org.slf4j.Logger;
 import org.slf4j.LoggerFactory;
 
 /**
  * Example of application configuration in Java.<p>
  */
 public class TwitterTopCounter extends DAG
 {
   private static final Logger logger = LoggerFactory.getLogger(TwitterTopCounter.class);
   private static final boolean inline = false;
   private static final long serialVersionUID = 201211201543L;
 
   private InputPort<Object> consoleOutput(String nodeName)
   {
     // hack to output to HTTP based on actual environment
     String serverAddr = System.getenv("MALHAR_AJAXSERVER_ADDRESS");
     if (serverAddr != null) {
       HttpOutputOperator<Object> operator = addOperator(nodeName, new HttpOutputOperator<Object>());
       operator.setResourceURL(URI.create("http://" + serverAddr + "/channel/" + nodeName));
       return operator.input;
     }
 
     ConsoleOutputOperator<Object> operator = addOperator(nodeName, new ConsoleOutputOperator<Object>());
     operator.setStringFormat(nodeName + ": %s");
     return operator.input;
   }
 
  public final TwitterSampleInput getTwitterFeed(String name)
   {
     final String propertyBase = "twitter4j";
     Properties properties = new Properties();
     try {
       properties.load(this.getClass().getResourceAsStream(propertyBase.concat(".properties")));
     }
     catch (IOException e) {
       logger.error("Could not read the much needed credentials file because of {}", e.getLocalizedMessage());
       return null;
     }
     /*
      * Setup the operator to get the data from twitter sample stream injected into the system.
      */
     TwitterSampleInput oper = addOperator(name, TwitterSampleInput.class);
     oper.setTwitterProperties(properties);
    oper.setFeedMultiplier(1);
     return oper;
   }
 
   public final TwitterStatusURLExtractor getTwitterUrlExtractor(String name)
   {
     TwitterStatusURLExtractor oper = addOperator(name, TwitterStatusURLExtractor.class);
     return oper;
   }
 
   public final UniqueCounter<String> getUniqueCounter(String name)
   {
     UniqueCounter<String> oper = addOperator(name, new UniqueCounter<String>());
     return oper;
   }
 
   public final WindowedTopCounter<String> getTopCounter(String name, int count)
   {
     WindowedTopCounter<String> oper = addOperator(name, new WindowedTopCounter<String>());
     oper.setTopCount(count);
     oper.setSlidingWindowWidth(600, 1);
     return oper;
   }
 
   public TwitterTopCounter(Configuration conf)
   {
     super(conf);
 
    TwitterSampleInput twitterFeed = getTwitterFeed("TweetSampler"); // Setup the operator to get the data from twitter sample stream injected into the system.
     TwitterStatusURLExtractor urlExtractor = getTwitterUrlExtractor("URLExtractor"); //  Setup the operator to get the URLs extracted from the twitter statuses
     UniqueCounter<String> uniqueCounter = getUniqueCounter("UniqueURLCounter"); // Setup a node to count the unique urls within a window.
     WindowedTopCounter<String> topCounts = getTopCounter("TopCounter", 10);  // Get the aggregated url counts and count them over the timeframe
 
     // Feed the statuses from feed into the input of the url extractor.
     addStream("TweetStream", twitterFeed.status, urlExtractor.input).setInline(true);
     //  Start counting the urls coming out of URL extractor
     addStream("TwittedURLs", urlExtractor.url, uniqueCounter.data).setInline(inline);
     // Count unique urls
     addStream("UniqueURLCounts", uniqueCounter.count, topCounts.input).setInline(inline);
     // Count top 10
     addStream("TopURLs", topCounts.output, consoleOutput("topURLs")).setInline(inline);
   }
 }
