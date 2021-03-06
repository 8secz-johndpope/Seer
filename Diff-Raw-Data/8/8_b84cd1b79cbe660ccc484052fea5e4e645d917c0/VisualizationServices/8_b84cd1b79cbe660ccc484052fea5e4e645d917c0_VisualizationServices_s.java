 /*******************************************************************************
  * Copyright 2012 The Regents of the University of California
  * 
  * Licensed under the Apache License, Version 2.0 (the "License");
  * you may not use this file except in compliance with the License.
  * You may obtain a copy of the License at
  * 
  *   http://www.apache.org/licenses/LICENSE-2.0
  * 
  * Unless required by applicable law or agreed to in writing, software
  * distributed under the License is distributed on an "AS IS" BASIS,
  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  * See the License for the specific language governing permissions and
  * limitations under the License.
  ******************************************************************************/
 package org.ohmage.service;
 
 import java.io.ByteArrayOutputStream;
 import java.io.DataOutputStream;
 import java.io.IOException;
 import java.io.InputStream;
 import java.io.UnsupportedEncodingException;
 import java.net.HttpURLConnection;
 import java.net.InetAddress;
 import java.net.MalformedURLException;
 import java.net.URL;
 import java.net.URLEncoder;
 import java.net.UnknownHostException;
 import java.util.Map;
 
 import org.ohmage.annotator.Annotator.ErrorCode;
 import org.ohmage.cache.PreferenceCache;
 import org.ohmage.exception.CacheMissException;
 import org.ohmage.exception.ServiceException;
 
 /**
  * This class contains the services for visualization requests.
  * 
  * @author John Jenkins
  */
 public class VisualizationServices {
 	private static final String APPLICATION_PATH = "/app";
 	
 	/**
 	 * The token parameter key for the visualization server.
 	 */
 	private static final String PARAMETER_KEY_TOKEN = "token";
 	/**
 	 * The server parameter key for the visualization server.
 	 */
 	private static final String PARAMETER_KEY_SERVER = "server";
 	/**
 	 * The campaign ID parameter key for the visualization server.
 	 */
 	private static final String PARAMETER_KEY_CAMPAIGN_ID = "campaign_urn";
 	/**
 	 * The width parameter key for the visualization server.
 	 */
 	private static final String PARAMETER_KEY_WIDTH = "!width";
 	/**
 	 * The height parameter key for the visualization server.
 	 */
 	private static final String PARAMETER_KEY_HEIGHT = "!height";
 	
 	/**
 	 * The start date which limits the range of visible survey responses to 
 	 * those on or after this date.
 	 */
 	public static final String PARAMETER_KEY_START_DATE = "start_date";
 	/**
 	 * The end date which limits the range of visible survey responses to those
 	 * on or before this date.
 	 */
 	public static final String PARAMETER_KEY_END_DATE = "end_date";
 	/**
 	 * The privacy state which limits the survey responses to only those whose
 	 * privacy state matches this privacy state.
 	 */
 	public static final String PARAMETER_KEY_PRIVACY_STATE = "privacy_state";
 	
 	/**
 	 * The single prompt ID parameter key for the visualization server.
 	 */
 	public static final String PARAMETER_KEY_PROMPT_ID = "prompt_id";
 	/**
 	 * The second prompt ID parameter key for the visualization server.
 	 */
 	public static final String PARAMETER_KEY_PROMPT2_ID = "prompt2_id";
 	/**
 	 * The username parameter key for the visualization server.
 	 */
 	public static final String PARAMETER_KEY_USERNAME = "user_id";
 	
 	/**
 	 * The parameter to tell the visualization server over how many days to
 	 * aggregate the data.
 	 */
 	public static final String PARAMETER_KEY_AGGREGATE = "aggregate";
 	
 	private static final String ENCODING = "UTF-8";
 	
 	/**
 	 * Default constructor. Made private so that it cannot be instantiated.
 	 */
 	private VisualizationServices() {}
 
 	/**
 	 * Sends a request to the visualization server and returns the image as a
 	 * byte array that was returned by the visualization server.
 	 * 
 	 * @param requestPath The additional path beyond the base URI that is 
 	 * 					  stored in the database. An example would be, if the
 	 * 					  database stored the visualization server's address as
 	 * 					  "https://viz.server.com/R/visualzations" then this
 	 * 					  parameter could be "survey_response_count" which 
 	 * 					  would result in a URL of
 	 * 					  "https://viz.server/com/R/visualizations/survey_response_count?param1=...".
 	 * 
 	 * @param userToken The authentication token for the requesting user that
 	 * 					will be passed on to the visualization server to 
 	 * 					perform subsequent requests on our behalf.
 	 * 
 	 * @param campaignId The unique identifier for the campaign whose 
 	 * 					 information will be used in conjunction with this
 	 * 					 request and any subsequent parameters.
 	 * 
 	 * @param width The desired width of the resulting visualization.
 	 * 
 	 * @param height The desired height of the resulting visualization.
 	 * 
 	 * @param parameters Any additional parameters that should be passed to the
 	 * 					 visualization server. Their key values will be used as
 	 * 					 the key in the HTTP parameter and their actual value
 	 * 					 will be their single-quoted HTTP parameter value.
 	 * 
 	 * @return Returns a byte[] representation of the visualization image.
 	 * 
 	 * @throws ServiceException Thrown if there is an error.
 	 */
 	public static byte[] sendVisualizationRequest(final String requestPath, 
 			final String userToken, final String campaignId, final int width, 
 			final int height, final Map<String, String> parameters) 
 			throws ServiceException {
 		
 		// Build the request URL.
 		StringBuilder urlBuilder = new StringBuilder();
 		try {
 			String serverUrl = PreferenceCache.instance().lookup(PreferenceCache.KEY_VISUALIZATION_SERVER);
 			urlBuilder.append(serverUrl);
 			
 			if(! serverUrl.endsWith("/")) {
 				urlBuilder.append("/");
 			}
 		}
 		catch(CacheMissException e) {
 			throw new ServiceException(
 					"Cache doesn't know about 'known' key: " + 
 						PreferenceCache.KEY_VISUALIZATION_SERVER,
 					e);
 		}
 		urlBuilder.append(requestPath);
 		
 		// Generate the URL String.
 		String urlString = urlBuilder.toString();
 		
 		// Build this server's URL.
 		StringBuilder parameterBuilder = new StringBuilder();
 		try {
 			parameterBuilder
 				.append(URLEncoder.encode(PARAMETER_KEY_TOKEN, ENCODING))
 				.append("='")
 				.append(URLEncoder.encode(userToken, ENCODING))
 				.append("'");
 	
 			// Build this server's URL.
 			parameterBuilder.append("&")
 				.append(URLEncoder.encode(PARAMETER_KEY_SERVER, ENCODING))
 				.append("='");
 			
 			// Get the protocol string based on SSL being enabled.
 			try {
 				String sslEnabled = 
 						PreferenceCache.instance().lookup(
 								PreferenceCache.KEY_SSL_ENABLED);
 				if((sslEnabled != null) && (sslEnabled.equals("true"))) {
 					parameterBuilder.append("https");
 				}
 				else {
 					parameterBuilder.append("http");
 				}
 			}
 			catch(CacheMissException e) {
 				throw new ServiceException(
 						"Cache doesn't know about 'known' key: " + 
 							PreferenceCache.KEY_SSL_ENABLED, 
 						e);
 			}
 			parameterBuilder.append(URLEncoder.encode("://", ENCODING));
 			
 			// Get this machine's hostname.
 			try {
 				parameterBuilder.append(
 						URLEncoder.encode(
 								InetAddress.getLocalHost().getHostName(), 
 								ENCODING));
 			}
 			catch(UnknownHostException e) {
 				throw new ServiceException(
 						"The sky is falling! Oh, and our own hostname is unknown.",
 						e);
 			}
 			parameterBuilder
 				.append(URLEncoder.encode(APPLICATION_PATH, ENCODING))
 				.append("'");
 			
 			// Add the required parameters.
 			parameterBuilder.append("&")
 				.append(URLEncoder.encode(PARAMETER_KEY_CAMPAIGN_ID, ENCODING))
 				.append("='")
 				.append(URLEncoder.encode(campaignId, ENCODING))
 				.append("'");
 			
 			parameterBuilder.append("&")
 				.append(URLEncoder.encode(PARAMETER_KEY_WIDTH, ENCODING))
 				.append("=")
 				.append(width);
 			
 			parameterBuilder.append("&")
 				.append(URLEncoder.encode(PARAMETER_KEY_HEIGHT, ENCODING))
 				.append("=")
 				.append(height);
 			
 			// Add all of the non-required, request-specific parameters.
 			for(String key : parameters.keySet()) {
				urlBuilder.append('&');
 				
 				if(PARAMETER_KEY_AGGREGATE.equals(key)) {
					urlBuilder
 						.append(URLEncoder.encode(key, ENCODING))
 						.append('=')
 						.append(parameters.get(key));
 				}
 				else {
					urlBuilder
 						.append(URLEncoder.encode(key, ENCODING))
 						.append("='")
 						.append(URLEncoder.encode(parameters.get(key), ENCODING))
 						.append("'");
 				}
 			}
 		}
 		catch(UnsupportedEncodingException e) {
 			throw new ServiceException(
 					"UTF-8 encoding is unknown.",
 					e);
 		}
 		
 		try {
 			// Connect to the visualization server.
 			URL url = new URL(urlString);
 			HttpURLConnection urlConnection = 
 					(HttpURLConnection) url.openConnection();
 			
 			urlConnection.setDoOutput(true);
 			urlConnection.setDoInput(true);
 			urlConnection.setInstanceFollowRedirects(true);
 			urlConnection.setRequestMethod("POST");
 			urlConnection.setRequestProperty(
 					"Content-Length", 
 					Integer.toString(parameterBuilder.length()));
 			
 			DataOutputStream output = 
 					new DataOutputStream(urlConnection.getOutputStream());
 			output.writeBytes(parameterBuilder.toString());
 			output.flush();
 			output.close();
 			
 			// If a non-200 response was returned, get the text from the 
 			// response.
 			if(urlConnection.getResponseCode() != 200) {
 				// Get the error text.
 				ByteArrayOutputStream errorByteStream = new ByteArrayOutputStream();
 				InputStream errorStream = urlConnection.getErrorStream();
 				byte[] chunk = new byte[4096];
 				int amountRead;
 				while((amountRead = errorStream.read(chunk)) != -1) {
 					errorByteStream.write(chunk, 0, amountRead);
 				}
 				errorStream.close();
 				
 				// Echo the error.
 				throw new ServiceException(
 						ErrorCode.VISUALIZATION_GENERAL_ERROR,
 						"There was an error. Please, try again later.",
 						"The server returned the HTTP error code '" + 
 							urlConnection.getResponseCode() + 
 							"' with the error '" + 
 							errorByteStream.toString() + 
 							"': " + 
 							urlString);
 			}
 			
 			// Build the response.
 			InputStream reader = urlConnection.getInputStream();
 			
 			// Generate the byte array.
 			ByteArrayOutputStream byteArrayStream = new ByteArrayOutputStream();
 			byte[] chunk = new byte[4096];
 			int amountRead = 0;
 			while((amountRead = reader.read(chunk)) != -1) {
 				byteArrayStream.write(chunk, 0, amountRead);
 			}
 			reader.close();
 			
 			return byteArrayStream.toByteArray();
 		}
 		catch(MalformedURLException e) {
 			throw new ServiceException(
 					ErrorCode.VISUALIZATION_GENERAL_ERROR, 
 					"Built a malformed URL: " + urlString, 
 					e);
 		}
 		catch(IOException e) {
 			throw new ServiceException(
 					ErrorCode.VISUALIZATION_GENERAL_ERROR, 
 					"Error while communicating with the visualization server.",
 					e);
 		}
 	}
 }
