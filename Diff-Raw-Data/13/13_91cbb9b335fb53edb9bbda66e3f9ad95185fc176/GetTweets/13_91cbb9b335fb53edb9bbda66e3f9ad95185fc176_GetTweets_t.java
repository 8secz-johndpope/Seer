 /**
  * GetTweets.java
  * 
  * @author Gresham, Ryan, Everett, Pierce
  */
 
 package com.grep.gaugebackend;
 
 import twitter4j.*;
 import java.util.concurrent.*;
 import twitter4j.conf.ConfigurationBuilder;
 
 /**
  * public class GetTweets implements Runnable
  */
 public class GetTweets implements Runnable {
 	
 	// outgoing m_outQueue of tweets
 	protected BlockingQueue<Tweet> m_outQueue = null;
 	// outgoing m_outQueue of tweets
 	protected BlockingQueue<WebToast> m_webToasts = null;
 	// m_Keywords to search for
 	protected String[] m_Keywords = null;
 	// access token
 	protected String m_accessToken;
 	// access token secret
 	protected String m_accessTokenSecret;
 	// have we sent the track limitation notice to the user yet?
 	protected Boolean m_trackLimitNotified = false;
 	
 	/**
 	 * Constructor
 	 */
 	public GetTweets(BlockingQueue<Tweet> queue, BlockingQueue<WebToast> webToasts, String[] keywords, String accessToken, String accessTokenSecret) {
 		m_outQueue = queue;
 		m_webToasts = webToasts;
 		m_Keywords = keywords;
 		m_accessToken = accessToken;
 		m_accessTokenSecret = accessTokenSecret;
 	}
 	
 	/**
 	 * public void run
 	 */
     public void run() {
         
 		// login info
         ConfigurationBuilder cb = new ConfigurationBuilder();
         cb.setDebugEnabled(true)
                 .setOAuthAccessToken(m_accessToken)
 				.setOAuthAccessTokenSecret(m_accessTokenSecret)
 				.setOAuthConsumerKey("2RKMlxcy1cf1WGFfHJvpg")
 				.setOAuthConsumerSecret("35Ege9Yk1vkoZmk4koDDZj07e9CJZtkRaLycXZepqA");
     	
 		// create the stream
         TwitterStream twitterStream = new TwitterStreamFactory(cb.build()).getInstance();
         
 		// status listener for twitter4J streaming
         final class StatusListenerQueueing implements StatusListener {
         	
         	protected BlockingQueue<Tweet> queue = null;
         	
         	public StatusListenerQueueing(BlockingQueue<Tweet> queue) {
             	this.queue = queue;
             }
         	
             @Override
             public void onStatus(Status status) {
 				//System.out.println(String.format("getter thread running... %d", this.queue.size()));
 				
 				this.queue.offer(new Tweet(status));
             }
 
             @Override
             public void onDeletionNotice(StatusDeletionNotice statusDeletionNotice) {
                 System.out.println("Got a status deletion notice id:" + statusDeletionNotice.getStatusId());
             }
 
             @Override
             public void onTrackLimitationNotice(int numberOfLimitedStatuses) {
                 System.out.println("Got track limitation notice:" + numberOfLimitedStatuses);
 				if(!m_trackLimitNotified){
 					m_webToasts.offer(new WebToast("warning", "This is a hot topic on Twitter right now! If you want to analyze every tweet for this topic, try narrowing down your keywords.", "Warning", 0, 0, 0));
 					m_trackLimitNotified = true;
 				}
             }
 
             @Override
             public void onScrubGeo(long userId, long upToStatusId) {
                 System.out.println("Got scrub_geo event userId:" + userId + " upToStatusId:" + upToStatusId);
             }
 
             @Override
             public void onStallWarning(StallWarning warning) {
                 System.out.println("Got stall warning:" + warning);
             }
 
             @Override
             public void onException(Exception ex) {
 				Thread.currentThread().interrupt();
             	System.out.println(ex.toString());
 				m_webToasts.offer(new WebToast("error", "Looks like your internet connection is sketch... we'll keep trying."));
             }
         };
 
 		// start listening and queueing up outgoing tweets
         twitterStream.addListener(new StatusListenerQueueing(this.m_outQueue));
 		// add the keyword filtering
         twitterStream.filter(new FilterQuery(0, null, this.m_Keywords));
		
        for(String keyword: this.m_Keywords)
			System.out.println(keyword +": TEST");
        
        //twitterStream.sample();
 		
 		// wait until the thread is interrupted
 		while(!Thread.currentThread().isInterrupted()) {
 			try {
 				Thread.sleep(2000L);
 			} catch (InterruptedException ex) {
 				// immediately reset interrupt flag
 				Thread.currentThread().interrupt();
 			}
 		}
 		
 		// stop the twitter stream
 		twitterStream.shutdown();
 		twitterStream.cleanUp();
 		
 		twitterStream = null;
     }
 }
