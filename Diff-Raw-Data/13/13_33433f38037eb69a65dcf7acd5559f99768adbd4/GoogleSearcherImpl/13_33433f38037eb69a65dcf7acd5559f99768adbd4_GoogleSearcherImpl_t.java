 package com.sk.impl.search;
 
 import java.util.regex.Pattern;
 
 import com.sk.util.parse.search.GoogleSearcher;
 
 public enum GoogleSearcherImpl {
	LINKEDIN("linkedin.com", Pattern.compile("www.linkedin.com/(?:pub|in)/(?!dir)")),
	TWITTER("twitter.com", Pattern.compile("www.twitter.com/[^/]+$")),
	GOOGLE_PLUS("plus.google.com", Pattern.compile("plus.google.com/[0-9]+$"));
 
 	public final GoogleSearcher searcher;
 
 	private GoogleSearcherImpl(String site, Pattern accept) {
 		searcher = new GoogleSearcher(site, accept);
 	}
 
 	public GoogleSearcher getSearcher() {
 		return searcher;
 	}
 }
