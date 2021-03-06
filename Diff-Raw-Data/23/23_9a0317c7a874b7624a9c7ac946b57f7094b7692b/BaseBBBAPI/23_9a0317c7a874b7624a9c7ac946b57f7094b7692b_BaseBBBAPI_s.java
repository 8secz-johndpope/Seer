 /**
  * Copyright (c) 2010-2009 The Sakai Foundation
  *
  * Licensed under the Educational Community License, Version 2.0 (the "License");
  * you may not use this file except in compliance with the License.
  * You may obtain a copy of the License at
  *
  *             http://www.osedu.org/licenses/ECL-2.0
  *
  * Unless required by applicable law or agreed to in writing, software
  * distributed under the License is distributed on an "AS IS" BASIS,
  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  * See the License for the specific language governing permissions and
  * limitations under the License.
  */
 
 package org.sakaiproject.bbb.impl.bbbapi;
 
 import java.io.BufferedReader;
 import java.io.IOException;
 import java.io.InputStreamReader;
 import java.io.StringReader;
 import java.io.UnsupportedEncodingException;
 import java.net.HttpURLConnection;
 import java.net.MalformedURLException;
 import java.net.ProtocolException;
 import java.net.URL;
 import java.net.URLEncoder;
 import java.util.ArrayList;
 import java.util.HashMap;
 import java.util.List;
 import java.util.Map;
 import java.util.Random;
 
 import javax.xml.parsers.DocumentBuilder;
 import javax.xml.parsers.DocumentBuilderFactory;
 import javax.xml.parsers.ParserConfigurationException;
 
 import org.apache.commons.codec.digest.DigestUtils;
 import org.apache.log4j.Logger;
 import org.sakaiproject.bbb.api.BBBException;
 import org.sakaiproject.bbb.api.BBBMeeting;
 import org.sakaiproject.bbb.api.BBBMeetingManager;
 import org.sakaiproject.component.api.ServerConfigurationService;
 import org.sakaiproject.component.cover.ComponentManager;
 import org.sakaiproject.user.api.User;
 import org.sakaiproject.util.ResourceLoader;
 import org.w3c.dom.Document;
 import org.w3c.dom.Node;
 import org.w3c.dom.NodeList;
 import org.xml.sax.InputSource;
 import org.xml.sax.SAXException;
 
 /**
  * Base class for interacting with any BigBlueButton API version.
  * 
  * @author Nuno Fernandes
  */
 public class BaseBBBAPI implements BBBAPI {
     protected final Logger logger = Logger.getLogger(getClass());
 
     // Sakai BBB configuration
     /**
      * BBB server url, including bigbluebutton webapp path. Will default to
      * http://localhost/bigbluebutton if not specified
      */
     protected String bbbUrl = "http://127.0.0.1/bigbluebutton";
     /** BBB security salt */
     protected String bbbSalt = null;
     /** Auto close BBB meeting window on exit? */
     protected boolean bbbAutocloseMeetingWindow = true;
 
     // API Server Path
     protected final static String API_SERVERPATH = "/api/";
 
     // API Calls
     protected final static String APICALL_CREATE = "create";
     protected final static String APICALL_ISMEETINGRUNNING = "isMeetingRunning";
     protected final static String APICALL_GETMEETINGINFO = "getMeetingInfo";
     protected final static String APICALL_GETMEETINGS = "getMeetings";
     protected final static String APICALL_JOIN = "join";
     protected final static String APICALL_END = "end";
     protected final static String APICALL_VERSION = "";
     protected final static String APICALL_GETRECORDINGS = "getRecordings";
     protected final static String APICALL_PUBLISHRECORDINGS = "publishRecordings";
     protected final static String APICALL_DELETERECORDINGS = "deleteRecordings";
 
     // API Response Codes
     protected final static String APIRESPONSE_SUCCESS = "SUCCESS";
     protected final static String APIRESPONSE_FAILED = "FAILED";
 
     // API Versions
     public final static String APIVERSION_063 = "0.63";
     public final static String APIVERSION_064 = "0.64";
     public final static String APIVERSION_070 = "0.70";
     public final static String APIVERSION_080 = "0.80";
     public final static String APIVERSION_MINIMUM = APIVERSION_063;
     public final static String APIVERSION_LATEST = APIVERSION_080;
 
     protected ServerConfigurationService config;
 
     protected DocumentBuilderFactory docBuilderFactory;
     protected DocumentBuilder docBuilder;
 
     protected Random randomGenerator = new Random(System.currentTimeMillis());
 
     // -----------------------------------------------------------------------
     // --- Initialization related methods ------------------------------------
     // -----------------------------------------------------------------------
     public BaseBBBAPI(String url, String salt) {
         this.bbbUrl = url;
 
         if (bbbUrl.endsWith("/") && bbbUrl.length() > 0)
             bbbUrl = bbbUrl.substring(0, bbbUrl.length() - 1);
 
         this.bbbSalt = salt;
 
         // read BBB settings from sakai.properties
         config = (ServerConfigurationService) ComponentManager.get(ServerConfigurationService.class);
 
         bbbAutocloseMeetingWindow = config.getBoolean(BBBMeetingManager.CFG_AUTOCLOSE_WIN, bbbAutocloseMeetingWindow);
 
         // Initialize XML libraries
         docBuilderFactory = DocumentBuilderFactory.newInstance();
         try {
             docBuilder = docBuilderFactory.newDocumentBuilder();
         } catch (ParserConfigurationException e) {
             logger.error("Failed to initialise BaseBBBAPI", e);
         }
     }
 
     public String getUrl() {
         return this.bbbUrl;
     }
 
     public String getSalt() {
         return this.bbbSalt;
     }
 
     // -----------------------------------------------------------------------
     // --- BBB API implementation methods ------------------------------------
     // -----------------------------------------------------------------------
     /** Create a meeting on BBB server */
     public BBBMeeting createMeeting(final BBBMeeting meeting)
             throws BBBException {
         try {
             // build query
             StringBuilder query = new StringBuilder();
             query.append("meetingID=");
             query.append(meeting.getId());
             query.append("&name=");
             query.append(URLEncoder.encode(meeting.getName(), getParametersEncoding()));
             query.append("&voiceBridge=");
             Integer voiceBridge = 70000 + new Random().nextInt(10000);
             query.append(voiceBridge);
             query.append("&dialNumber=");
             query.append(voiceBridge);
             query.append("&attendeePW=");
             String attendeePW = meeting.getAttendeePassword() != null && !"".equals(meeting.getAttendeePassword().trim()) 
                     ? meeting.getAttendeePassword()
                     : generatePassword();
             query.append(attendeePW);
             query.append("&moderatorPW=");
             String moderatorPW = meeting.getModeratorPassword() != null && !"".equals(meeting.getModeratorPassword().trim()) 
                     ? meeting.getModeratorPassword()
                     : generatePassword();
             query.append(moderatorPW);
             if (bbbAutocloseMeetingWindow) {
                 query.append("&logoutURL=");
                 StringBuilder logoutUrl = new StringBuilder(config.getServerUrl());
                 logoutUrl.append(BBBMeetingManager.TOOL_WEBAPP);
                 logoutUrl.append("/bbb-autoclose.html");
                 query.append(URLEncoder.encode(logoutUrl.toString(), getParametersEncoding()));
             }
 
             // BSN: Parameters required for playback recording
             query.append("&record=");
             String recording = meeting.getRecording() != null && meeting.getRecording().booleanValue() ? "true" : "false";
             query.append(recording);
 
             query.append("&duration=");
             String duration = meeting.getRecordingDuration() != null? meeting.getRecordingDuration().toString(): "0";
             query.append(duration);
 
             query.append("&meta_description=");
             String description = meeting.getRecordingDescription();
             query.append(URLEncoder.encode(description == null? "": description.trim(), getParametersEncoding()));
             // BSN: Parameters required for notification when recordings are done
 
             // BSN: Parameters required for monitoring
             ResourceLoader toolParameters = new ResourceLoader("Tool");
             query.append("&meta_originApp=");
             String originAppSakaiVersion = config.getString("version.sakai", "");
             query.append(URLEncoder.encode("Sakai[" + originAppSakaiVersion + "]" + BBBMeetingManager.TOOL_WEBAPP + "[" + toolParameters.getString("bbb.devBuild") + "]", getParametersEncoding()));
 
             query.append("&meta_originServerId=");
             String originServerId = config.getString("serverId", "");
             query.append(originServerId);
             
             query.append("&meta_originServerUrl=");
             StringBuilder serverUrl = new StringBuilder(config.getServerUrl());
             query.append(URLEncoder.encode(serverUrl.toString(), getParametersEncoding()));
 
             query.append("&meta_originServerName=");
             String originServerName = config.getServerName();
             query.append(URLEncoder.encode(originServerName, getParametersEncoding()));
             // BSN: Ends
 
             // Composed Welcome message
             String welcomeMessage = meeting.getProps().getWelcomeMessage();
             if (recording == "true")
                 welcomeMessage = welcomeMessage + "<br><br><b>This session is being recorded.</b>";
             if (duration.compareTo("0") != 0)
                 welcomeMessage = welcomeMessage.concat("<br><br><b>The maximum duration for this session is " + duration + " minutes.");
 
             query.append("&welcome=");
             query.append(URLEncoder.encode(welcomeMessage, getParametersEncoding()));
 
             query.append(getCheckSumParameterForQuery(APICALL_CREATE, query.toString()));
 
             // do API call
             Map<String, Object> response = doAPICall(APICALL_CREATE, query.toString());
             meeting.setAttendeePassword((String) response.get("attendeePW"));
             meeting.setModeratorPassword((String) response.get("moderatorPW"));
             meeting.setVoiceBridge(voiceBridge);
         } catch (BBBException e) {
             throw e;
         } catch (UnsupportedEncodingException e) {
             throw new BBBException(BBBException.MESSAGEKEY_INTERNALERROR, e.getMessage(), e);
         }
 
         return meeting;
     }
 
     /** Check if meeting is running on BBB server. */
     public boolean isMeetingRunning(String meetingID) 
             throws BBBException {
         try {
             StringBuilder query = new StringBuilder();
             query.append("meetingID=");
             query.append(meetingID);
             query.append(getCheckSumParameterForQuery(APICALL_ISMEETINGRUNNING, query.toString()));
 
             Map<String, Object> response = doAPICall(APICALL_ISMEETINGRUNNING, query.toString());
             return Boolean.parseBoolean((String) response.get("running"));
         } catch (Exception e) {
             throw new BBBException(BBBException.MESSAGEKEY_INTERNALERROR, e.getMessage(), e);
         }
     }
 
     /** Get live meeting information from BBB server */
     public Map<String, Object> getMeetings() 
             throws BBBException {
         try {
             StringBuilder query = new StringBuilder();
             query.append("random=xyz");
             query.append(getCheckSumParameterForQuery(APICALL_GETMEETINGS, query.toString()));
 
             Map<String, Object> response = doAPICall(APICALL_GETMEETINGS, query.toString());
 
             // nullify password fields
             for (String key : response.keySet()) {
                 if ("attendeePW".equals(key) || "moderatorPW".equals(key))
                     response.put(key, null);
             }
 
             return response;
         } catch (Exception e) {
             throw new BBBException(BBBException.MESSAGEKEY_INTERNALERROR, e.getMessage(), e);
         }
         
     }
     
     /** Get detailed live meeting information from BBB server */
     public Map<String, Object> getMeetingInfo(String meetingID, String password) 
             throws BBBException {
         
         try {
             StringBuilder query = new StringBuilder();
             query.append("meetingID=");
             query.append(meetingID);
             query.append("&password=");
             query.append(password);
             query.append(getCheckSumParameterForQuery(APICALL_GETMEETINGINFO, query.toString()));
 
             Map<String, Object> response = doAPICall(APICALL_GETMEETINGINFO, query.toString());
             
             // nullify password fields
             for (String key : response.keySet()) {
                 if ("attendeePW".equals(key) || "moderatorPW".equals(key))
                     response.put(key, null);
             }
 
             return response;
         } catch (BBBException e) {
             logger.debug("getMeetingInfo.Exception: MessageKey=" + e.getMessageKey() + ", Message=" + e.getMessage() );
             throw new BBBException(e.getMessageKey(), e.getMessage(), e);
         }
     }
 
     /** Get recordings from BBB server */
     public Map<String, Object> getRecordings(String meetingID) 
             throws BBBException {
         
     	Map<String, Object> response = null;
     	
     	try {
             StringBuilder query = new StringBuilder();
             query.append("meetingID=");
             query.append(meetingID);
             query.append(getCheckSumParameterForQuery(APICALL_GETRECORDINGS, query.toString()));
 
             response = doAPICall(APICALL_GETRECORDINGS, query.toString());
 
             return response;
         } catch (BBBException e) {
             logger.debug("getMeetingInfo.Exception: MessageKey=" + e.getMessageKey() + ", Message=" + e.getMessage() );
             throw new BBBException(e.getMessageKey(), e.getMessage(), e);
         }
         
     }
 
     /** End/delete a meeting on BBB server */
     public boolean endMeeting(String meetingID, String password) 
             throws BBBException {
         
         StringBuilder query = new StringBuilder();
         query.append("meetingID=");
         query.append(meetingID);
         query.append("&password=");
         query.append(password);
         query.append(getCheckSumParameterForQuery(APICALL_END, query.toString()));
 
         try {
             doAPICall(APICALL_END, query.toString());
 
         } catch (BBBException e) {
 			if(BBBException.MESSAGEKEY_NOTFOUND.equals(e.getMessageKey())) {
 				// we can safely ignore this one: the meeting is not running
 				return true;
 			}else{
 				throw e;
 			}
         }
 
         return true;
     }
 
     /** Delete a recording on BBB server */
     public boolean deleteRecordings(String meetingID, String recordID) 
             throws BBBException {
         StringBuilder query = new StringBuilder();
         query.append("recordID=");
         query.append(recordID);
         query.append(getCheckSumParameterForQuery(APICALL_DELETERECORDINGS, query.toString()));
 
         try {
             doAPICall(APICALL_DELETERECORDINGS, query.toString());
             
         } catch (BBBException e) {
             throw e;
         }
         
         return true;
     }
 
     /** Publish/Unpublish a recording on BBB server */
     public boolean publishRecordings(String meetingID, String recordID, String publish) 
             throws BBBException {
         StringBuilder query = new StringBuilder();
         query.append("recordID=");
         query.append(recordID);
         query.append("&publish=");
         query.append(publish);
         query.append(getCheckSumParameterForQuery(APICALL_PUBLISHRECORDINGS, query.toString()));
 
         try {
             doAPICall(APICALL_PUBLISHRECORDINGS, query.toString());
 
         } catch (BBBException e) {
             throw e;
         }
         
         return true;
     }
 
     /** Build the join meeting url based on user role */
     public String getJoinMeetingURL(String meetingID, User user, String password) {
         String userDisplayName, userId;
         try {
             userId = user.getId();
             userDisplayName = user.getDisplayName();
         } catch (Exception e) {
             userId = null;
             userDisplayName = "user";
         }
         StringBuilder joinQuery = new StringBuilder();
         joinQuery.append("meetingID=");
         joinQuery.append(meetingID);
         joinQuery.append("&fullName=");
         try {
             joinQuery.append(URLEncoder.encode(userDisplayName, getParametersEncoding()));
         } catch (UnsupportedEncodingException e) {
             joinQuery.append(userDisplayName);
         }
         joinQuery.append("&password=");
         joinQuery.append(password);
         if (userId != null) {
             joinQuery.append("&userID=");
             joinQuery.append(userId);
         }
         joinQuery.append(getCheckSumParameterForQuery(APICALL_JOIN, joinQuery.toString()));
 
         StringBuilder url = new StringBuilder(bbbUrl);
         url.append(API_SERVERPATH);
         url.append(APICALL_JOIN);
         url.append("?");
         url.append(joinQuery);
 
         return url.toString();
     }
 
     /** Make sure the meeting (still) exists on BBB server */
     public void makeSureMeetingExists(BBBMeeting meeting) 
             throws BBBException {
         // (re)create meeting in BBB
         createMeeting(meeting);
     }
 
     /** Get the BBB API version running on BBB server */
     public final String getAPIVersion() {
         String _version = null;
         try {
             Map<String, Object> response = doAPICall(APICALL_VERSION, null);
             _version = (String) response.get("version");
             _version = _version != null ? _version.trim() : null;
             if (_version == null || Float.valueOf(_version.substring(0, 3)) < 0.0) {
                 logger.warn("Invalid BigBlueButton version (" + _version + ")");
                 _version = null;
             }
             _version = _version.trim();
         } catch (BBBException e) {
             if (BBBException.MESSAGEKEY_NOACTION.equals(e.getMessageKey())) {
                 // we are clearly connecting to BBB < 0.70 => assuming minimum
                 // version (0.63)
                 _version = APIVERSION_MINIMUM;
             } else {
                 // something went wrong => warn user
                 logger.warn("Unable to check BigBlueButton version: " + e.getMessage());
                 _version = null;
             }
         } catch (Exception e) {
             // something went wrong => warn user
             logger.warn("Unable to check BigBlueButton version: " + e.getMessage());
             _version = null;
         }
         return _version;
     }
 
     // -----------------------------------------------------------------------
     // --- BBB API utility methods -------------------------------------------
     // -----------------------------------------------------------------------
     /** Compute the query string checksum based on the security salt */
     protected String getCheckSumParameterForQuery(String apiCall,
             String queryString) {
         if (bbbSalt != null)
             return "&checksum=" + DigestUtils.shaHex(apiCall + queryString + bbbSalt);
         else
             return "";
     }
 
     /** Encoding used when encoding url parameters */
     protected String getParametersEncoding() {
         return "UTF-8";
     }
 
     /** Make an API call */
     protected Map<String, Object> doAPICall(String apiCall, String query) 
             throws BBBException {
         StringBuilder urlStr = new StringBuilder(bbbUrl);
         urlStr.append(API_SERVERPATH);
         urlStr.append(apiCall);
         if (query != null) {
             urlStr.append("?");
             urlStr.append(query);
         }
 
         try {
             // open connection
             logger.debug("doAPICall.call: " + apiCall + (query != null ? query : ""));
            logger.info("JF: doAPICall.call: " + apiCall + (query != null ? query : ""));
             URL url = new URL(urlStr.toString());
             HttpURLConnection httpConnection = (HttpURLConnection) url.openConnection();
             httpConnection.setUseCaches(false);
             httpConnection.setDoOutput(true);
             httpConnection.setRequestMethod("GET");
             httpConnection.connect();
 
             int responseCode = httpConnection.getResponseCode();
             if (responseCode == HttpURLConnection.HTTP_OK) {
                 // read response
                 InputStreamReader isr = null;
                 BufferedReader reader = null;
                 StringBuilder xml = new StringBuilder();
                 try {
                     isr = new InputStreamReader(httpConnection.getInputStream(), "UTF-8");
                     reader = new BufferedReader(isr);
                     String line = reader.readLine();
                     while (line != null) {
                     	if( !line.startsWith("<?xml version=\"1.0\"?>"))
                     		xml.append(line.trim());
                         line = reader.readLine();
                     }
                 } finally {
                     if (reader != null)
                         reader.close();
                     if (isr != null)
                         isr.close();
                 }
                 httpConnection.disconnect();
 
                 // parse response
                 logger.debug("doAPICall.response: " + xml);
                logger.info("JF: doAPICall.response: " + xml);
                Document dom = docBuilder.parse(new InputSource( new StringReader(xml.toString())));
                 Map<String, Object> response = getNodesAsMap(dom, "response");
 
                 String returnCode = (String) response.get("returncode");
                 if (APIRESPONSE_FAILED.equals(returnCode)) {
                     throw new BBBException((String) response.get("messageKey"), (String) response.get("message"));
                 }
 
                 return response;
 
             } else {
                 throw new BBBException(BBBException.MESSAGEKEY_HTTPERROR, "BBB server responded with HTTP status code " + responseCode);
             }
 
 		} catch(BBBException e) {
             logger.debug("doAPICall.BBBException: MessageKey=" + e.getMessageKey() + ", Message=" + e.getMessage());
 			throw new BBBException( e.getMessageKey(), e.getMessage(), e);
         } catch(Exception e) {
             logger.debug("doAPICall.Exception: Message=" + e.getMessage());
             throw new BBBException(BBBException.MESSAGEKEY_UNREACHABLE, e.getMessage(), e);
         }
     }
 
     // -----------------------------------------------------------------------
     // --- BBB Other utility methods -----------------------------------------
     // -----------------------------------------------------------------------
     /** Get all nodes under the specified element tag name as a Java map */
     protected Map<String, Object> getNodesAsMap(Document dom, String elementTagName) {
         Node firstNode = dom.getElementsByTagName(elementTagName).item(0);
         return processNode(firstNode);
     }
 
     protected Map<String, Object> processNode(Node _node) {
         Map<String, Object> map = new HashMap<String, Object>();
         NodeList responseNodes = _node.getChildNodes();
         for (int i = 0; i < responseNodes.getLength(); i++) {
             Node node = responseNodes.item(i);
             String nodeName = node.getNodeName().trim();
             if (node.getChildNodes().getLength() == 1
                     && ( node.getChildNodes().item(0).getNodeType() == org.w3c.dom.Node.TEXT_NODE || node.getChildNodes().item(0).getNodeType() == org.w3c.dom.Node.CDATA_SECTION_NODE) ) {
                 String nodeValue = node.getTextContent();
                 map.put(nodeName, nodeValue != null ? nodeValue.trim() : null);
             
             } else if (node.getChildNodes().getLength() == 0
                     && node.getNodeType() != org.w3c.dom.Node.TEXT_NODE 
                     && node.getNodeType() != org.w3c.dom.Node.CDATA_SECTION_NODE) {
                 map.put(nodeName, "");
             
             } else if ( (node.getChildNodes().getLength() >= 1 
                     && node.getChildNodes().item(0).getChildNodes().item(0).getNodeType() != org.w3c.dom.Node.TEXT_NODE 
                     && node.getChildNodes().item(0).getChildNodes().item(0).getNodeType() != org.w3c.dom.Node.CDATA_SECTION_NODE) ) {
                 List<Object> list = new ArrayList<Object>();
                 for (int c = 0; c < node.getChildNodes().getLength(); c++) {
                     Node n = node.getChildNodes().item(c);
                     list.add(processNode(n));
                 }
                 map.put(nodeName, list);
             
             } else {
                 map.put(nodeName, processNode(node));
             }
         }
         return map;
     }
 
     /** Generate a random password */
     protected String generatePassword() {
         return Long.toHexString(randomGenerator.nextLong());
     }
 
 }
