 /*
  * DeliciousDroid - http://code.google.com/p/DeliciousDroid/
  *
  * Copyright (C) 2010 Matt Schmidt
  *
  * DeliciousDroid is free software; you can redistribute it and/or modify
  * it under the terms of the GNU General Public License as published
  * by the Free Software Foundation; either version 3 of the License,
  * or (at your option) any later version.
  *
  * DeliciousDroid is distributed in the hope that it will be useful, but
  * WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
  * General Public License for more details.
  *
  * You should have received a copy of the GNU General Public License
  * along with DeliciousDroid; if not, write to the Free Software
  * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307
  * USA
  */
 
 package com.deliciousdroid.client;
 
 import java.io.BufferedReader;
 import java.io.IOException;
 import java.io.InputStream;
 import java.io.InputStreamReader;
 import java.text.ParseException;
 import java.util.ArrayList;
 import java.util.TreeMap;
 import java.util.zip.GZIPInputStream;
 
 import org.apache.http.Header;
 import org.apache.http.HttpEntity;
 import org.apache.http.HttpResponse;
 import org.apache.http.HttpStatus;
 import org.apache.http.auth.AuthScope;
 import org.apache.http.auth.AuthenticationException;
 import org.apache.http.auth.Credentials;
 import org.apache.http.auth.UsernamePasswordCredentials;
 import org.apache.http.client.CredentialsProvider;
 import org.apache.http.client.methods.HttpGet;
 import org.apache.http.impl.client.DefaultHttpClient;
 
 import android.accounts.Account;
 import android.accounts.AccountManager;
 import android.accounts.AuthenticatorException;
 import android.accounts.OperationCanceledException;
 import android.content.Context;
 import android.net.Uri;
 import android.util.Log;
 
 import com.deliciousdroid.Constants;
 import com.deliciousdroid.providers.BookmarkContent.Bookmark;
 import com.deliciousdroid.providers.BundleContent.Bundle;
 import com.deliciousdroid.providers.TagContent.Tag;
 import com.deliciousdroid.xml.SaxBookmarkParser;
 import com.deliciousdroid.xml.SaxBundleParser;
 import com.deliciousdroid.xml.SaxTagParser;
 import com.deliciousdroid.client.TooManyRequestsException;
 import com.deliciousdroid.client.Update;
 
 public class DeliciousApi {
 	
     private static final String TAG = "DeliciousApi";
 
     public static final String USER_AGENT = "AuthenticationService/1.0";
     public static final int REGISTRATION_TIMEOUT = 30 * 1000; // ms
 
     public static final String FETCH_TAGS_URI = "v1/tags/get";
     public static final String FETCH_BUNDLES_URI = "v1/tags/bundles/all";
     public static final String FETCH_SUGGESTED_TAGS_URI = "v1/posts/suggest";
     public static final String FETCH_BOOKMARKS_URI = "v1/posts/all";
     public static final String FETCH_CHANGED_BOOKMARKS_URI = "v1/posts/all";
     public static final String FETCH_BOOKMARK_URI = "v1/posts/get";
     public static final String LAST_UPDATE_URI = "v1/posts/update";
     public static final String DELETE_BOOKMARK_URI = "v1/posts/delete";
     public static final String ADD_BOOKMARKS_URI = "v1/posts/add";
   
     private static final String SCHEME = "http";
     private static final String SCHEME_HTTP = "http";
     private static final String DELICIOUS_AUTHORITY = "api.del.icio.us";
     private static final int PORT = 80;
  
     private static final AuthScope SCOPE = new AuthScope(DELICIOUS_AUTHORITY, PORT);
 
     /**
      * Gets timestamp of last update to data on Delicious servers.
      * 
      * @param account The account being synced.
      * @param context The current application context.
      * @return An Update object containing the timestamp and the number of new bookmarks in the
      * inbox.
      * @throws IOException If a server error was encountered.
      * @throws AuthenticationException If an authentication error was encountered.
      */
     public static Update lastUpdate(Account account, Context context)
     	throws IOException, AuthenticationException, TooManyRequestsException {
 
     	String response = null;
     	InputStream responseStream = null;
     	TreeMap<String, String> params = new TreeMap<String, String>();
     	Update update = null;
     	
     	responseStream = DeliciousApiCall(LAST_UPDATE_URI, params, account, context);
     	response = convertStreamToString(responseStream);
     	responseStream.close();
     	
     	try{
 	        if (response.contains("<?xml")) {
 	        	update = Update.valueOf(response);
 	        } else {
 	            Log.e(TAG, "Server error in fetching bookmark list");
 	            throw new IOException();
 	        }
     	}
     	catch(Exception e) {
     		Log.e(TAG, "Server error in fetching bookmark list");
     		throw new IOException();
     	}
         return update;
     }
     
     /**
      * Sends a request to Delicious's Add Bookmark api.
      * 
      * @param bookmark The bookmark to be added.
      * @param account The account being synced.
      * @param context The current application context.
      * @return A boolean indicating whether or not the api call was successful.
      * @throws IOException If a server error was encountered.
      * @throws AuthenticationException If an authentication error was encountered.
      * @throws TokenRejectedException If the oauth token is reported to be expired.
      * @throws Exception If an unknown error is encountered.
      */
     public static Boolean addBookmark(Bookmark bookmark, Account account, Context context) 
     	throws IOException, AuthenticationException, TooManyRequestsException {
 
     	String url = bookmark.getUrl();
     	if(url.endsWith("/")) {
     		url = url.substring(0, url.lastIndexOf('/'));
     	}
     	
     	TreeMap<String, String> params = new TreeMap<String, String>();
     	  	
 		params.put("description", bookmark.getDescription());
 		params.put("extended", bookmark.getNotes());
		params.put("tags", bookmark.getTagString());
 		params.put("url", bookmark.getUrl());
 		
 		if(bookmark.getShared()){
 			params.put("shared", "yes");
 		} else params.put("shared", "no");
 		
 		String uri = ADD_BOOKMARKS_URI;
 		String response = null;
 		InputStream responseStream = null;
 
     	responseStream = DeliciousApiCall(uri, params, account, context);
     	response = convertStreamToString(responseStream);
     	responseStream.close();
 
         if (response.contains("<result code=\"done\"/>")) {
             return true;
         } else {
         	Log.e(TAG, "Server error in adding bookmark");
             throw new IOException();
         }
     }
     
     /**
      * Sends a request to Delicious's Delete Bookmark api.
      * 
      * @param bookmark The bookmark to be deleted.
      * @param account The account being synced.
      * @param context The current application context.
      * @return A boolean indicating whether or not the api call was successful.
      * @throws IOException If a server error was encountered.
      * @throws AuthenticationException If an authentication error was encountered.
      */
     public static Boolean deleteBookmark(Bookmark bookmark, Account account, Context context) 
     	throws IOException, AuthenticationException, TooManyRequestsException {
 
     	TreeMap<String, String> params = new TreeMap<String, String>();
     	String response = null;
     	InputStream responseStream = null;
     	String url = DELETE_BOOKMARK_URI;
 
     	params.put("url", bookmark.getUrl());
 
     	responseStream = DeliciousApiCall(url, params, account, context);
     	response = convertStreamToString(responseStream);
     	responseStream.close();
     	
         if (response.contains("<result code=\"done\"") || response.contains("<result code=\"item not found\"")) {
             return true;
         } else {
             Log.e(TAG, "Server error in fetching bookmark list");
             throw new IOException();
         }
     }
     
     /**
      * Retrieves a specific list of bookmarks from Delicious.
      * 
      * @param hashes A list of bookmark hashes to be retrieved.  
      * 	The hashes are MD5 hashes of the URL of the bookmark.
      * 
      * @param account The account being synced.
      * @param context The current application context.
      * @return A list of bookmarks received from the server.
      * @throws IOException If a server error was encountered.
      * @throws AuthenticationException If an authentication error was encountered.
      */
     public static ArrayList<Bookmark> getBookmark(ArrayList<String> hashes, Account account,
         Context context) throws IOException, AuthenticationException, TooManyRequestsException {
 
     	ArrayList<Bookmark> bookmarkList = new ArrayList<Bookmark>();
     	TreeMap<String, String> params = new TreeMap<String, String>();
     	String hashString = "";
     	InputStream responseStream = null;
     	String url = FETCH_BOOKMARK_URI;
 
     	for(String h : hashes){
     		if(hashes.get(0) != h){
     			hashString += "+";
     		}
     		hashString += h;
     	}
     	params.put("meta", "yes");
     	params.put("hashes", hashString);
 
     	responseStream = DeliciousApiCall(url, params, account, context);
     	SaxBookmarkParser parser = new SaxBookmarkParser(responseStream);
     	
     	try {
 			bookmarkList = parser.parse();
 		} catch (ParseException e) {
             Log.e(TAG, "Server error in fetching bookmark list");
             throw new IOException();
 		}
 
         responseStream.close();
         return bookmarkList;
     }
     
     /**
      * Retrieves the entire list of bookmarks for a user from Pinboard.
      * 
      * @param tagname If specified, will only retrieve bookmarks with a specific tag.
      * @param account The account being synced.
      * @param context The current application context.
      * @return A list of bookmarks received from the server.
      * @throws IOException If a server error was encountered.
      * @throws AuthenticationException If an authentication error was encountered.
      * @throws TooManyRequestsException 
      */
     public static ArrayList<Bookmark> getAllBookmarks(String tagName, Account account, Context context) 
     	throws IOException, AuthenticationException, TooManyRequestsException {
 
         return getAllBookmarks(tagName, 0, 0, account, context);
     }
         
     /**
      * Retrieves the entire list of bookmarks for a user from Delicious.  Warning:  Overuse of this 
      * api call will get your account throttled.
      * 
      * @param tagname If specified, will only retrieve bookmarks with a specific tag.
      * @param account The account being synced.
      * @param context The current application context.
      * @return A list of bookmarks received from the server.
      * @throws IOException If a server error was encountered.
      * @throws AuthenticationException If an authentication error was encountered.
      */
     public static ArrayList<Bookmark> getAllBookmarks(String tagName, int start, int count, Account account, Context context) 
 	throws IOException, AuthenticationException, TooManyRequestsException {
     	ArrayList<Bookmark> bookmarkList = new ArrayList<Bookmark>();
 
     	InputStream responseStream = null;
     	TreeMap<String, String> params = new TreeMap<String, String>();
     	String url = FETCH_BOOKMARKS_URI;
 
     	if(tagName != null && tagName != ""){
     		params.put("tag", tagName);
     	}
     	
     	if(start != 0){
     		params.put("start", Integer.toString(start));
     	}
     	
     	if(count != 0){
     		params.put("results", Integer.toString(count));
     	}
     	
     	params.put("meta", "yes");
 
     	responseStream = DeliciousApiCall(url, params, account, context);
     	SaxBookmarkParser parser = new SaxBookmarkParser(responseStream);
     	
     	//Log.d("kdf", convertStreamToString(responseStream));
     	
     	try {
 			bookmarkList = parser.parse();
 		} catch (ParseException e) {
             Log.e(TAG, "Server error in fetching bookmark list");
             throw new IOException();
 		}
 
         responseStream.close();
         
         return bookmarkList;
     }
     
     /**
      * Retrieves a list of all bookmarks, with only their URL hash and a change (meta) hash,
      * to determine what bookmarks have changed since the last update.
      * 
      * @param account The account being synced.
      * @param context The current application context.
      * @return A list of bookmarks received from the server with only the URL hash and meta hash.
      * @throws IOException If a server error was encountered.
      * @throws AuthenticationException If an authentication error was encountered.
      */
     public static ArrayList<Bookmark> getChangedBookmarks(Account account, Context context) 
     	throws IOException, AuthenticationException {
     	
     	ArrayList<Bookmark> bookmarkList = new ArrayList<Bookmark>();
     	InputStream responseStream = null;
     	TreeMap<String, String> params = new TreeMap<String, String>();
     	String url = FETCH_CHANGED_BOOKMARKS_URI;
 
     	params.put("hashes", "yes");
 
     	responseStream = DeliciousApiCall(url, params, account, context);
     	SaxBookmarkParser parser = new SaxBookmarkParser(responseStream);
 
         try {
         	bookmarkList = parser.parse();
         } catch (ParseException e) {
         	Log.e(TAG, "Server error in fetching bookmark list");
         	throw new IOException();
         }
 
         responseStream.close();
         return bookmarkList;
     }
     
     /**
      * Retrieves a list of suggested tags for a URL.
      * 
      * @param suggestUrl The URL to get suggested tags for.
      * @param account The account being synced.
      * @param context The current application context.
      * @return A list of tags suggested for the provided url.
      * @throws IOException If a server error was encountered.
      * @throws AuthenticationException If an authentication error was encountered.
      */
     public static ArrayList<Tag> getSuggestedTags(String suggestUrl, Account account, Context context) 
     	throws IOException, AuthenticationException, TooManyRequestsException {
     	
     	ArrayList<Tag> tagList = new ArrayList<Tag>();
     	
 		if(!suggestUrl.startsWith("http")){
 			suggestUrl = "http://" + suggestUrl;
 		}
 
     	InputStream responseStream = null;
     	TreeMap<String, String> params = new TreeMap<String, String>();
     	params.put("url", suggestUrl);
     	
     	String url = FETCH_SUGGESTED_TAGS_URI;
     	  	
     	responseStream = DeliciousApiCall(url, params, account, context);
     	SaxTagParser parser = new SaxTagParser(responseStream);
     	
     	try {
 			tagList = parser.parseSuggested();
 		} catch (ParseException e) {
             Log.e(TAG, "Server error in fetching bookmark list");
             throw new IOException();
 		}
 
         responseStream.close();
         return tagList;
     }
     
     /**
      * Retrieves a list of all tags for a user from Delicious.
      * 
      * @param account The account being synced.
      * @param context The current application context.
      * @return A list of the users tags.
      * @throws IOException If a server error was encountered.
      * @throws AuthenticationException If an authentication error was encountered.
      */
     public static ArrayList<Tag> getTags(Account account, Context context) 
     	throws IOException, AuthenticationException, TooManyRequestsException {
     	
     	ArrayList<Tag> tagList = new ArrayList<Tag>();
 
     	InputStream responseStream = null;
     	final TreeMap<String, String> params = new TreeMap<String, String>();
     	  	
     	responseStream = DeliciousApiCall(FETCH_TAGS_URI, params, account, context);
     	final SaxTagParser parser = new SaxTagParser(responseStream);
     	
     	try {
 			tagList = parser.parse();
 		} catch (ParseException e) {
             Log.e(TAG, "Server error in fetching bookmark list");
             throw new IOException();
 		}
 
         responseStream.close();
         return tagList;
     }
     
     /**
      * Retrieves a list of all tag bundles for a user from Delicious.
      * 
      * @param account The account being synced.
      * @param context The current application context.
      * @return A list of the users bundles.
      * @throws IOException If a server error was encountered.
      * @throws AuthenticationException If an authentication error was encountered.
      */
     public static ArrayList<Bundle> getBundles(Account account, Context context) 
     	throws IOException, AuthenticationException {
     	
     	ArrayList<Bundle> bundleList = new ArrayList<Bundle>();
     	InputStream responseStream = null;
     	TreeMap<String, String> params = new TreeMap<String, String>();
     	String url = FETCH_BUNDLES_URI;
     	  	
     	responseStream = DeliciousApiCall(url, params, account, context);
     	SaxBundleParser parser = new SaxBundleParser(responseStream);
     	
         try {
         	bundleList = parser.parse();
         } catch (ParseException e) {
         	Log.e(TAG, "Server error in fetching bundle list");
         	throw new IOException();
         }
 
         responseStream.close();
         return bundleList;
     }
     
     /**
      * Performs an api call to Delicious's http based api methods.
      * 
      * @param url URL of the api method to call.
      * @param params Extra parameters included in the api call, as specified by different methods.
      * @param account The account being synced.
      * @param context The current application context.
      * @return A String containing the response from the server.
      * @throws IOException If a server error was encountered.
      * @throws AuthenticationException If an authentication error was encountered.
      */
     private static InputStream DeliciousApiCall(String url, TreeMap<String, String> params, 
     		Account account, Context context) throws IOException, AuthenticationException{
     	
     	
     	final AccountManager am = AccountManager.get(context);
     	
 		if(account == null)
 			throw new AuthenticationException();
 
     	final String username = account.name;
     	String authtoken = null;
     	
     	try {
 			authtoken = am.blockingGetAuthToken(account, Constants.AUTHTOKEN_TYPE, false);
 		} catch (OperationCanceledException e) {
 			// TODO Auto-generated catch block
 			e.printStackTrace();
 		} catch (AuthenticatorException e) {
 			// TODO Auto-generated catch block
 			e.printStackTrace();
 		}
 
 		Uri.Builder builder = new Uri.Builder();
 		builder.scheme(SCHEME);
 		builder.authority(DELICIOUS_AUTHORITY);
 		builder.appendEncodedPath(url);
 		for(String key : params.keySet()){
 			builder.appendQueryParameter(key, params.get(key));
 		}
 		
 		String apiCallUrl = builder.build().toString();
 		
 		Log.d("apiCallUrl", apiCallUrl);
 		final HttpGet post = new HttpGet(apiCallUrl);
 
 		post.setHeader("User-Agent", "DeliciousDroid");
 		post.setHeader("Accept-Encoding", "gzip");
     		
 		DefaultHttpClient client = (DefaultHttpClient)HttpClientFactory.getThreadSafeClient();
         CredentialsProvider provider = client.getCredentialsProvider();
         Credentials credentials = new UsernamePasswordCredentials(username, authtoken);
         provider.setCredentials(SCOPE, credentials);
         
         client.addRequestInterceptor(new PreemptiveAuthInterceptor(), 0);
                
         final HttpResponse resp = client.execute(post);
         
         final int statusCode = resp.getStatusLine().getStatusCode();
 
     	if (statusCode == HttpStatus.SC_OK) {
     		
     		final HttpEntity entity = resp.getEntity();
     		
     		InputStream instream = entity.getContent();
     		
     		final Header encoding = entity.getContentEncoding();
     		
     		if(encoding != null && encoding.getValue().equalsIgnoreCase("gzip")) {
     			instream = new GZIPInputStream(instream);
     		}
     		
     		return instream;
     	} else if (statusCode == HttpStatus.SC_UNAUTHORIZED) {
     		throw new AuthenticationException();
     	} else {
     		throw new IOException();
     	}
     }
     
     /**
      * Converts an InputStream to a string.
      * 
      * @param is The InputStream to convert.
      * @return The String retrieved from the InputStream.
      */
     private static String convertStreamToString(InputStream is) {
         /*
          * To convert the InputStream to String we use the BufferedReader.readLine()
          * method. We iterate until the BufferedReader return null which means
          * there's no more data to read. Each line will appended to a StringBuilder
          * and returned as String.
          */
         BufferedReader reader = new BufferedReader(new InputStreamReader(is));
         StringBuilder sb = new StringBuilder();
  
         String line = null;
         try {
             while ((line = reader.readLine()) != null) {
                 sb.append(line + "\n");
             }
         } catch (IOException e) {
             e.printStackTrace();
         } finally {
             try {
                 is.close();
             } catch (IOException e) {
                 e.printStackTrace();
             }
         }
         return sb.toString();
     }
 
 }
