 package davmail.exchange;
 
 import davmail.Settings;
 import org.apache.commons.httpclient.*;
 import org.apache.commons.httpclient.methods.GetMethod;
 import org.apache.commons.httpclient.methods.HeadMethod;
 import org.apache.commons.httpclient.methods.PostMethod;
 import org.apache.commons.httpclient.methods.PutMethod;
 import org.apache.commons.httpclient.util.URIUtil;
 import org.apache.log4j.Logger;
 import org.apache.webdav.lib.Property;
 import org.apache.webdav.lib.ResponseEntity;
 import org.apache.webdav.lib.WebdavResource;
 import org.jdom.Attribute;
 import org.jdom.input.DOMBuilder;
 import org.w3c.dom.Document;
 import org.w3c.dom.Element;
 import org.w3c.dom.NamedNodeMap;
 import org.w3c.dom.Node;
 import org.w3c.tidy.Tidy;
 
 import javax.mail.MessagingException;
 import javax.mail.internet.MimeUtility;
 import java.io.*;
 import java.net.HttpURLConnection;
 import java.net.URL;
 import java.net.UnknownHostException;
 import java.text.ParseException;
 import java.text.SimpleDateFormat;
 import java.util.*;
 
 /**
  * Exchange session through Outlook Web Access (DAV)
  */
 public class ExchangeSession {
     protected static final Logger LOGGER = Logger.getLogger("davmail.exchange.ExchangeSession");
 
     /**
      * exchange message properties needed to rebuild mime message
      */
     protected static final Vector<String> MESSAGE_REQUEST_PROPERTIES = new Vector<String>();
 
     static {
         MESSAGE_REQUEST_PROPERTIES.add("DAV:uid");
         MESSAGE_REQUEST_PROPERTIES.add("urn:schemas:httpmail:subject");
         MESSAGE_REQUEST_PROPERTIES.add("urn:schemas:mailheader:mime-version");
         MESSAGE_REQUEST_PROPERTIES.add("urn:schemas:mailheader:content-class");
         MESSAGE_REQUEST_PROPERTIES.add("urn:schemas:httpmail:hasattachment");
 
         // needed only when full headers not found
         MESSAGE_REQUEST_PROPERTIES.add("urn:schemas:mailheader:received");
         MESSAGE_REQUEST_PROPERTIES.add("urn:schemas:mailheader:date");
         MESSAGE_REQUEST_PROPERTIES.add("urn:schemas:mailheader:message-id");
         MESSAGE_REQUEST_PROPERTIES.add("urn:schemas:mailheader:thread-topic");
         MESSAGE_REQUEST_PROPERTIES.add("urn:schemas:mailheader:thread-index");
         MESSAGE_REQUEST_PROPERTIES.add("urn:schemas:mailheader:from");
         MESSAGE_REQUEST_PROPERTIES.add("urn:schemas:mailheader:to");
         MESSAGE_REQUEST_PROPERTIES.add("urn:schemas:httpmail:priority");
 
         // full headers
         MESSAGE_REQUEST_PROPERTIES.add("http://schemas.microsoft.com/mapi/proptag/x0007D001E");
         // mail body
         MESSAGE_REQUEST_PROPERTIES.add("http://schemas.microsoft.com/mapi/proptag/x01000001E");
         // html body
         MESSAGE_REQUEST_PROPERTIES.add("urn:schemas:httpmail:htmldescription");
         // same as htmldescription, remove
         // MESSAGE_REQUEST_PROPERTIES.add("http://schemas.microsoft.com/mapi/proptag/x01013001E");
         // size
         MESSAGE_REQUEST_PROPERTIES.add("http://schemas.microsoft.com/mapi/proptag/x0e080003");
         // only for calendar events
         MESSAGE_REQUEST_PROPERTIES.add("urn:schemas:calendar:location");
         MESSAGE_REQUEST_PROPERTIES.add("urn:schemas:calendar:dtstart");
         MESSAGE_REQUEST_PROPERTIES.add("urn:schemas:calendar:dtend");
         MESSAGE_REQUEST_PROPERTIES.add("urn:schemas:calendar:instancetype");
         MESSAGE_REQUEST_PROPERTIES.add("urn:schemas:calendar:busystatus");
         MESSAGE_REQUEST_PROPERTIES.add("urn:schemas:calendar:meetingstatus");
         MESSAGE_REQUEST_PROPERTIES.add("urn:schemas:calendar:alldayevent");
         MESSAGE_REQUEST_PROPERTIES.add("urn:schemas:calendar:responserequested");
         MESSAGE_REQUEST_PROPERTIES.add("urn:schemas:mailheader:cc");
 
     }
 
     public static final HashMap<String, String> PRIORITIES = new HashMap<String, String>();
 
     static {
         PRIORITIES.put("-2", "5 (Lowest)");
         PRIORITIES.put("-1", "4 (Low)");
         PRIORITIES.put("1", "2 (High)");
         PRIORITIES.put("2", "1 (Highest)");
     }
 
     public static final String CONTENT_TYPE_HEADER = "content-type: ";
     public static final String CONTENT_TRANSFER_ENCODING_HEADER = "content-transfer-encoding: ";
     public static final String CONTENT_DISPOSITION_HEADER = "content-disposition: ";
     public static final String CONTENT_ID_HEADER = "content-id: ";
 
     private static final int DEFAULT_KEEP_DELAY = 30;
 
     /**
      * Date parser from Exchange format
      */
     private final SimpleDateFormat dateParser;
     /**
      * Date formatter to MIME format
      */
     private final SimpleDateFormat dateFormatter;
 
     /**
      * Various standard mail boxes Urls
      */
     private String inboxUrl;
     private String deleteditemsUrl;
     private String sendmsgUrl;
     private String draftsUrl;
 
     /**
      * Base user mailboxes path (used to select folder)
      */
     private String mailPath;
     private String currentFolderUrl;
     private WebdavResource wdr = null;
 
     /**
      * Create an exchange session for the given URL.
      * The session is not actually established until a call to login()
      */
     public ExchangeSession() {
         // SimpleDateFormat are not thread safe, need to create one instance for
         // each session
         dateParser = new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS");
         dateParser.setTimeZone(new SimpleTimeZone(0, "GMT"));
         dateFormatter = new java.text.SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z", Locale.ENGLISH);
     }
 
     /**
      * Update http client configuration (proxy)
      *
      * @param httpClient current Http client
      */
     protected static void configureClient(HttpClient httpClient) {
         // do not send basic auth automatically
         httpClient.getState().setAuthenticationPreemptive(false);
 
         boolean enableProxy = Settings.getBooleanProperty("davmail.enableProxy");
         String proxyHost = null;
         int proxyPort = 0;
         String proxyUser = null;
         String proxyPassword = null;
 
         if (enableProxy) {
             proxyHost = Settings.getProperty("davmail.proxyHost");
             proxyPort = Settings.getIntProperty("davmail.proxyPort");
             proxyUser = Settings.getProperty("davmail.proxyUser");
             proxyPassword = Settings.getProperty("davmail.proxyPassword");
         }
 
         // configure proxy
         if (proxyHost != null && proxyHost.length() > 0) {
             httpClient.getHostConfiguration().setProxy(proxyHost, proxyPort);
             if (proxyUser != null && proxyUser.length() > 0) {
 
 /*              // Only available in newer HttpClient releases, not compatible with slide library
                 List authPrefs = new ArrayList();
                 authPrefs.add(AuthPolicy.BASIC);
                 httpClient.getParams().setParameter(AuthPolicy.AUTH_SCHEME_PRIORITY,authPrefs);
 */
                 // instead detect ntlm authentication (windows domain name in user name)
                 int backslashindex = proxyUser.indexOf("\\");
                 if (backslashindex > 0) {
                     httpClient.getState().setProxyCredentials(null, proxyHost,
                             new NTCredentials(proxyUser.substring(backslashindex + 1),
                                     proxyPassword, null,
                                     proxyUser.substring(0, backslashindex)));
                 } else {
                     httpClient.getState().setProxyCredentials(null, proxyHost,
                             new UsernamePasswordCredentials(proxyUser, proxyPassword));
                 }
             }
         }
 
     }
 
     public static void checkConfig() throws IOException {
         try {
             String url = Settings.getProperty("davmail.url");
 
             // create an HttpClient instance
             HttpClient httpClient = new HttpClient();
             configureClient(httpClient);
 
             // get webmail root url (will not follow redirects)
             HttpMethod testMethod = new GetMethod(url);
             testMethod.setFollowRedirects(false);
             int status = httpClient.executeMethod(testMethod);
             testMethod.releaseConnection();
             LOGGER.debug("Test configuration status: " + status);
             if (status != HttpStatus.SC_OK && status != HttpStatus.SC_UNAUTHORIZED
                     && status != HttpStatus.SC_MOVED_TEMPORARILY) {
                 throw new IOException("Unable to connect to OWA at " + url + ", status code " +
                         status + ", check configuration");
             }
 
         } catch (UnknownHostException exc) {
             LOGGER.error("DavMail configuration exception: \n Unknown host " + exc.getMessage(), exc);
             throw new IOException("DavMail configuration exception: \n Unknown host " + exc.getMessage(), exc);
         } catch (Exception exc) {
             LOGGER.error("DavMail configuration exception: \n" + exc.getMessage(), exc);
             throw new IOException("DavMail configuration exception: \n" + exc.getMessage(), exc);
         }
 
     }
 
     public void login(String userName, String password) throws IOException {
         try {
             String url = Settings.getProperty("davmail.url");
 
             // get proxy configuration from setttings properties
             URL urlObject = new URL(url);
             // webdavresource is unable to create the correct url type
             HttpURL httpURL;
             if (url.startsWith("http://")) {
                 httpURL = new HttpURL(userName, password,
                         urlObject.getHost(), urlObject.getPort());
             } else if (url.startsWith("https://")) {
                 httpURL = new HttpsURL(userName, password,
                         urlObject.getHost(), urlObject.getPort());
             } else {
                 throw new IllegalArgumentException("Invalid URL: " + url);
             }
             wdr = new WebdavResource(httpURL, WebdavResource.NOACTION, 0);
 
             // set httpclient timeout to 30 seconds
             //wdr.retrieveSessionInstance().setTimeout(30000);
 
             // get the internal HttpClient instance
             HttpClient httpClient = wdr.retrieveSessionInstance();
 
             configureClient(httpClient);
 
             // get webmail root url (will follow redirects)
             // providing credentials
             HttpMethod initmethod = new GetMethod(url);
             wdr.executeHttpRequestMethod(httpClient,
                     initmethod);
             if (initmethod.getPath().indexOf("exchweb/bin") > 0) {
                 LOGGER.debug("Form based authentication detected");
 
                 PostMethod logonMethod = new PostMethod(
                         "/exchweb/bin/auth/owaauth.dll?" +
                                 "ForcedBasic=false&Basic=false&Private=true" +
                                 "&Language=No_Value"
                 );
                 logonMethod.addParameter("destination", url);
                 logonMethod.addParameter("flags", "4");
 //                logonMethod.addParameter("visusername", userName.substring(userName.lastIndexOf('\\')));
                 logonMethod.addParameter("username", userName);
                 logonMethod.addParameter("password", password);
 //                logonMethod.addParameter("SubmitCreds", "Log On");
 //                logonMethod.addParameter("forcedownlevel", "0");
                 logonMethod.addParameter("trusted", "4");
 
                 wdr.executeHttpRequestMethod(wdr.retrieveSessionInstance(),
                         logonMethod);
                 Header locationHeader = logonMethod.getResponseHeader(
                         "Location");
 
                 if (logonMethod.getStatusCode() != HttpURLConnection.HTTP_MOVED_TEMP ||
                         locationHeader == null ||
                         !url.equals(locationHeader.getValue())) {
                     throw new HttpException("Authentication failed");
                 }
 
             }
 
             // User now authenticated, get various session information
             HttpMethod method = new GetMethod(url);
             int status = wdr.executeHttpRequestMethod(wdr.
                     retrieveSessionInstance(), method);
             if (status != HttpStatus.SC_MULTI_STATUS
                     && status != HttpStatus.SC_OK) {
                 HttpException ex = new HttpException();
                 ex.setReasonCode(status);
                 ex.setReason(method.getStatusText());
                 throw ex;
             }
 
             // get user mail URL from html body (multi frame)
             String body = method.getResponseBodyAsString();
             int beginIndex = body.indexOf(url);
             if (beginIndex < 0) {
                 throw new HttpException(url + " not found in body");
             }
             body = body.substring(beginIndex);
             int endIndex = body.indexOf('"');
             if (endIndex < 0) {
                 throw new HttpException(url + " not found in body");
             }
             body = body.substring(url.length(), endIndex);
             // got base http mailbox http url
             mailPath = "/exchange/" + body;
             wdr.setPath(mailPath);
 
             // Retrieve inbox and trash URLs
             Vector<String> reqProps = new Vector<String>();
             reqProps.add("urn:schemas:httpmail:inbox");
             reqProps.add("urn:schemas:httpmail:deleteditems");
             reqProps.add("urn:schemas:httpmail:sendmsg");
             reqProps.add("urn:schemas:httpmail:drafts");
 
             Enumeration foldersEnum = wdr.propfindMethod(0, reqProps);
             if (!foldersEnum.hasMoreElements()) {
                 throw new IOException("Unable to get mail folders");
             }
             ResponseEntity inboxResponse = (ResponseEntity) foldersEnum.
                     nextElement();
             Enumeration inboxPropsEnum = inboxResponse.getProperties();
             if (!inboxPropsEnum.hasMoreElements()) {
                 throw new IOException("Unable to get mail folders");
             }
             while (inboxPropsEnum.hasMoreElements()) {
                 Property inboxProp = (Property) inboxPropsEnum.nextElement();
                 if ("inbox".equals(inboxProp.getLocalName())) {
                     inboxUrl = URIUtil.decode(inboxProp.getPropertyAsString());
                 }
                 if ("deleteditems".equals(inboxProp.getLocalName())) {
                     deleteditemsUrl = URIUtil.decode(inboxProp.
                             getPropertyAsString());
                 }
                 if ("sendmsg".equals(inboxProp.getLocalName())) {
                     sendmsgUrl = URIUtil.decode(inboxProp.
                             getPropertyAsString());
                 }
                 if ("drafts".equals(inboxProp.getLocalName())) {
                     draftsUrl = URIUtil.decode(inboxProp.
                             getPropertyAsString());
                 }
             }
 
             // set current folder to Inbox
             currentFolderUrl = inboxUrl;
 
             LOGGER.debug("Inbox URL : " + inboxUrl);
             LOGGER.debug("Trash URL : " + deleteditemsUrl);
             LOGGER.debug("Send URL : " + sendmsgUrl);
             LOGGER.debug("Drafts URL : " + draftsUrl);
             // TODO : sometimes path, sometimes Url ?
             deleteditemsUrl = URIUtil.getPath(deleteditemsUrl);
             wdr.setPath(URIUtil.getPath(inboxUrl));
 
         } catch (Exception exc) {
             StringBuffer message = new StringBuffer();
             message.append("DavMail login exception: ");
             if (exc.getMessage() != null) {
                 message.append(exc.getMessage());
             } else if (exc instanceof HttpException) {
                 message.append(((HttpException) exc).getReasonCode());
                 String httpReason = ((HttpException) exc).getReason();
                 if (httpReason != null) {
                     message.append(" ");
                     message.append(httpReason);
                 }
             } else {
                 message.append(exc);
             }
             try {
                 message.append("\nWebdav status:");
                 message.append(wdr.getStatusCode());
 
                 String webdavStatusMessage = wdr.getStatusMessage();
                 if (webdavStatusMessage != null) {
                     message.append(webdavStatusMessage);
                 }
             } catch (Exception e) {
                 LOGGER.error("Exception getting status from " + wdr);
             }
 
             LOGGER.error(message.toString());
             throw new IOException(message.toString());
         }
     }
 
     /**
      * Close session.
      * This will only close http client, not the actual Exchange session
      *
      * @throws IOException if unable to close Webdav context
      */
     public void close() throws IOException {
         wdr.close();
     }
 
     /**
      * Create message in current folder
      *
      * @param subject     message subject line
      * @param messageBody mail body
      * @throws java.io.IOException when unable to create message
      */
     public void createMessage(String subject, String messageBody) throws IOException {
         createMessage(currentFolderUrl, subject, messageBody);
     }
 
     /**
      * Create message in specified folder.
      * Will overwrite an existing message with same subject in the same folder
      *
      * @param folderUrl   Exchange folder URL
      * @param subject     message subject line
      * @param messageBody mail body
      * @throws java.io.IOException when unable to create message
      */
     public void createMessage(String folderUrl, String subject, String messageBody) throws IOException {
         String messageUrl = URIUtil.encodePathQuery(folderUrl + "/" + subject + ".EML");
 
         PutMethod putmethod = new PutMethod(messageUrl);
         putmethod.setRequestHeader("Content-Type", "message/rfc822");
         putmethod.setRequestBody(messageBody);
 
         int code = wdr.retrieveSessionInstance().executeMethod(putmethod);
 
         if (code == HttpURLConnection.HTTP_OK) {
             LOGGER.warn("Overwritten message " + messageUrl);
         } else if (code != HttpURLConnection.HTTP_CREATED) {
             throw new IOException("Unable to create message " + code + " " + putmethod.getStatusLine());
         }
     }
 
     protected Message buildMessage(ResponseEntity responseEntity) throws URIException {
         Message message = new Message();
         message.messageUrl = URIUtil.decode(responseEntity.getHref());
         Enumeration propertiesEnum = responseEntity.getProperties();
         while (propertiesEnum.hasMoreElements()) {
             Property prop = (Property) propertiesEnum.nextElement();
             String localName = prop.getLocalName();
             if ("x0007D001E".equals(localName)) {
                 message.fullHeaders = prop.getPropertyAsString();
             } else if ("x01000001E".equals(localName)) {
                 message.body = prop.getPropertyAsString();
             } else if ("x0e080003".equals(localName)) {
                 message.size = Integer.parseInt(prop.getPropertyAsString());
             } else if ("htmldescription".equals(localName)) {
                 message.htmlBody = prop.getPropertyAsString();
             } else if ("uid".equals(localName)) {
                 message.uid = prop.getPropertyAsString();
             } else if ("content-class".equals(prop.getLocalName())) {
                 message.contentClass = prop.getPropertyAsString();
             } else if (("hasattachment").equals(prop.getLocalName())) {
                 message.hasAttachment = "1".equals(prop.getPropertyAsString());
             } else if ("received".equals(prop.getLocalName())) {
                 message.received = prop.getPropertyAsString();
             } else if ("date".equals(prop.getLocalName())) {
                 message.date = prop.getPropertyAsString();
             } else if ("message-id".equals(prop.getLocalName())) {
                 message.messageId = prop.getPropertyAsString();
             } else if ("thread-topic".equals(prop.getLocalName())) {
                 message.threadTopic = prop.getPropertyAsString();
             } else if ("thread-index".equals(prop.getLocalName())) {
                 message.threadIndex = prop.getPropertyAsString();
             } else if ("from".equals(prop.getLocalName())) {
                 message.from = prop.getPropertyAsString();
             } else if ("to".equals(prop.getLocalName())) {
                 message.to = prop.getPropertyAsString();
             } else if ("cc".equals(prop.getLocalName())) {
                 message.cc = prop.getPropertyAsString();
             } else if ("subject".equals(prop.getLocalName())) {
                 message.subject = prop.getPropertyAsString();
             } else if ("priority".equals(prop.getLocalName())) {
                 String priorityLabel = PRIORITIES.get(prop.getPropertyAsString());
                 if (priorityLabel != null) {
                     message.priority = priorityLabel;
                 }
             }
         }
         message.preProcessHeaders();
 
         return message;
     }
 
     public Message getMessage(String messageUrl) throws IOException {
 
         // TODO switch according to Log4J log level
         //wdr.setDebug(4);
         //wdr.propfindMethod(messageUrl, 0);
         Enumeration messageEnum = wdr.propfindMethod(messageUrl, 0, MESSAGE_REQUEST_PROPERTIES);
         //wdr.setDebug(0);
 
         // 201 created in some cases ?!?
         if ((wdr.getStatusCode() != HttpURLConnection.HTTP_OK && wdr.getStatusCode() != HttpURLConnection.HTTP_CREATED)
                 || !messageEnum.hasMoreElements()) {
             throw new IOException("Unable to get message: " + wdr.getStatusCode()
                     + " " + wdr.getStatusMessage());
         }
         ResponseEntity entity = (ResponseEntity) messageEnum.nextElement();
 
         return buildMessage(entity);
 
     }
 
     public List<Message> getAllMessages() throws IOException {
         List<Message> messages = new ArrayList<Message>();
         //wdr.setDebug(4);
         //wdr.propfindMethod(currentFolderUrl, 1);
         // one level search
         Enumeration folderEnum = wdr.propfindMethod(currentFolderUrl, 1, MESSAGE_REQUEST_PROPERTIES);
         //wdr.setDebug(0);
         while (folderEnum.hasMoreElements()) {
             ResponseEntity entity = (ResponseEntity) folderEnum.nextElement();
 
             Message message = buildMessage(entity);
             if ("urn:content-classes:message".equals(message.contentClass) ||
                     "urn:content-classes:calendarmessage".equals(message.contentClass) ||
                     "urn:content-classes:recallmessage".equals(message.contentClass) ||
                     "urn:content-classes:dsn".equals(message.contentClass)) {
                 messages.add(message);
             }
         }
         return messages;
     }
 
     /**
      * Delete oldest messages in trash.
      * keepDelay is the number of days to keep messages in trash before delete
      *
      * @throws IOException when unable to purge messages
      */
     public void purgeOldestTrashMessages() throws IOException {
         int keepDelay = Settings.getIntProperty("davmail.keepDelay");
         if (keepDelay == 0) {
             keepDelay = DEFAULT_KEEP_DELAY;
         }
 
         Calendar cal = Calendar.getInstance();
         cal.add(Calendar.DAY_OF_MONTH, -keepDelay);
         LOGGER.debug("Delete messages in trash since " + cal.getTime());
         long keepTimestamp = cal.getTimeInMillis();
 
         Vector<String> deleteRequestProperties = new Vector<String>();
         deleteRequestProperties.add("DAV:getlastmodified");
         deleteRequestProperties.add("urn:schemas:mailheader:content-class");
 
         Enumeration folderEnum = wdr.propfindMethod(deleteditemsUrl, 1, deleteRequestProperties);
         while (folderEnum.hasMoreElements()) {
             ResponseEntity entity = (ResponseEntity) folderEnum.nextElement();
             String messageUrl = URIUtil.decode(entity.getHref());
             String lastModifiedString = null;
             String contentClass = null;
             Enumeration propertiesEnum = entity.getProperties();
             while (propertiesEnum.hasMoreElements()) {
                 Property prop = (Property) propertiesEnum.nextElement();
                 String localName = prop.getLocalName();
                 if ("getlastmodified".equals(localName)) {
                     lastModifiedString = prop.getPropertyAsString();
                 } else if ("content-class".equals(prop.getLocalName())) {
                     contentClass = prop.getPropertyAsString();
                 }
             }
             if ("urn:content-classes:message".equals(contentClass) &&
                     lastModifiedString != null && lastModifiedString.length() > 0) {
                 Date parsedDate;
                 try {
                     parsedDate = dateParser.parse(lastModifiedString);
                     if (parsedDate.getTime() < keepTimestamp) {
                         LOGGER.debug("Delete " + messageUrl + " last modified " + parsedDate);
                         wdr.deleteMethod(messageUrl);
                     }
                 } catch (ParseException e) {
                     LOGGER.warn("Invalid message modified date " + lastModifiedString + " on " + messageUrl);
                 }
             }
 
         }
     }
 
     public void sendMessage(BufferedReader reader) throws IOException {
         String subject = "davmailtemp";
         String line = reader.readLine();
         StringBuffer mailBuffer = new StringBuffer();
         while (!".".equals(line)) {
             mailBuffer.append(line);
             mailBuffer.append("\n");
             line = reader.readLine();
 
             // patch thunderbird html in reply for correct outlook display
             if (line.startsWith("<head>")) {
                 line += "\n  <style> blockquote { display: block; margin: 1em 0px; padding-left: 1em; border-left: solid; border-color: blue; border-width: thin;}</style>";
             }
             if (line.startsWith("Subject")) {
                 subject = MimeUtility.decodeText(line.substring(8).trim());
                 // '/' is invalid as message URL
                 subject = subject.replaceAll("/", "_xF8FF_");
                 // '?' is also invalid
                 subject = subject.replaceAll("\\?", "");
                 // TODO : test & in subject
             }
         }
 
         createMessage(draftsUrl, subject, mailBuffer.toString());
 
         // warning : slide library expects *unencoded* urls
         String tempUrl = draftsUrl + "/" + subject + ".eml";
         boolean sent = wdr.moveMethod(tempUrl, sendmsgUrl);
         if (!sent) {
             throw new IOException("Unable to send message: " + wdr.getStatusCode()
                     + " " + wdr.getStatusMessage());
         }
 
     }
 
     /**
      * Select current folder.
      * Folder name can be logical names INBOX or TRASH (translated to local names),
      * relative path to user base folder or absolute path.
      *
      * @param folderName folder name
      * @return Folder object
      * @throws IOException when unable to change folder
      */
     public Folder selectFolder(String folderName) throws IOException {
         Folder folder = new Folder();
         folder.folderUrl = null;
         if ("INBOX".equals(folderName)) {
             folder.folderUrl = inboxUrl;
         } else if ("TRASH".equals(folderName)) {
             folder.folderUrl = deleteditemsUrl;
             // absolute folder path
         } else if (folderName != null && folderName.startsWith("/")) {
             folder.folderUrl = folderName;
         } else {
             folder.folderUrl = mailPath + folderName;
         }
 
         Vector<String> reqProps = new Vector<String>();
         reqProps.add("urn:schemas:httpmail:unreadcount");
         reqProps.add("DAV:childcount");
         Enumeration folderEnum = wdr.propfindMethod(folder.folderUrl, 0, reqProps);
 
         if (folderEnum.hasMoreElements()) {
             ResponseEntity entity = (ResponseEntity) folderEnum.nextElement();
             Enumeration propertiesEnum = entity.getProperties();
             while (propertiesEnum.hasMoreElements()) {
                 Property prop = (Property) propertiesEnum.nextElement();
                 if ("unreadcount".equals(prop.getLocalName())) {
                     folder.unreadCount = Integer.parseInt(prop.getPropertyAsString());
                 }
                 if ("childcount".equals(prop.getLocalName())) {
                     folder.childCount = Integer.parseInt(prop.getPropertyAsString());
                 }
             }
 
         } else {
             throw new IOException("Folder not found: " + folder.folderUrl);
         }
         currentFolderUrl = folder.folderUrl;
         return folder;
     }
 
     public class Folder {
         public String folderUrl;
         public int childCount;
         public int unreadCount;
     }
 
     public class Message {
         public static final String CONTENT_TYPE_HEADER = "Content-Type: ";
         public static final String CONTENT_TRANSFER_ENCODING_HEADER = "Content-Transfer-Encoding: ";
         public String messageUrl;
         public String uid;
         public int size;
         public String fullHeaders;
         public String body;
         public String htmlBody;
         public String contentClass;
         public boolean hasAttachment;
         public boolean hasInlineAttachment;
         public String to;
         public String cc;
         public String date;
         public String messageId;
         public String received;
         public String threadIndex;
         public String threadTopic;
         public String from;
         public String subject;
         public String priority;
 
         protected List<Attachment> attachments;
 
         // attachment index used during write
         protected int attachmentIndex;
 
         protected String getReceived() {
             StringTokenizer st = new StringTokenizer(received, "\r\n");
             StringBuffer result = new StringBuffer();
             while (st.hasMoreTokens()) {
                 result.append("Received: ").append(st.nextToken()).append("\r\n");
             }
             return result.toString();
         }
 
         protected void preProcessHeaders() {
             // only handle exchange messages
             if (!"urn:content-classes:message".equals(contentClass) &&
                     !"urn:content-classes:calendarmessage".equals(contentClass) &&
                     !"urn:content-classes:recallmessage".equals(contentClass)
                     ) {
                 return;
             }
 
             // fullHeaders seem empty sometimes, rebuild it
             if (fullHeaders == null || fullHeaders.length() == 0) {
                 try {
                     if (date.length() > 0) {
                         Date parsedDate = dateParser.parse(date);
                         date = dateFormatter.format(parsedDate);
                     }
                     fullHeaders = "Skipped header\n" +
                             getReceived() +
                             "MIME-Version: 1.0\n" +
                             "Content-Type: application/ms-tnef;\n" +
                             "\tname=\"winmail.dat\"\n" +
                             "Content-Transfer-Encoding: binary\n" +
                             "Content-class: " + contentClass + "\n" +
                             "Subject: " + MimeUtility.encodeText(subject) + "\n" +
                             "Date: " + date + "\n" +
                             "Message-ID: " + messageId + "\n" +
                             "Thread-Topic: " + MimeUtility.encodeText(threadTopic) + "\n" +
                             "Thread-Index: " + threadIndex + "\n" +
                             "From: " + from + "\n" +
                             "To: " + to + "\n";
                     if (priority != null) {
                         fullHeaders += "X-Priority: " + priority + "\n";
                     }
                     if (cc != null && cc.length() > 0) {
                         fullHeaders += "CC: " + cc + "\n";
                     }
                 } catch (ParseException e) {
                     throw new RuntimeException("Unable to rebuild header " + e + " " + e.getMessage());
                 } catch (UnsupportedEncodingException e) {
                     throw new RuntimeException("Unable to rebuild header " + e + " " + e.getMessage());
                 }
             }
             StringBuffer result = new StringBuffer();
             boolean mstnefDetected = false;
             String boundary = null;
             BufferedReader reader = null;
             try {
                 reader = new BufferedReader(new StringReader(fullHeaders));
                 String line = reader.readLine();
                 while (line != null && line.length() > 0) {
                     // patch exchange Content type
                     if (line.equals(CONTENT_TYPE_HEADER + "application/ms-tnef;")) {
                         if (hasAttachment) {
                             // trigger attachments load
                             loadAttachments();
                             boundary = "----_=_NextPart_001_" + uid;
                             String contentType = "multipart/mixed";
                             // use multipart/related with inline images
                             if (hasInlineAttachment) {
                                 contentType = "multipart/related";
                             }
                             line = CONTENT_TYPE_HEADER + contentType + ";\n\tboundary=\"" + boundary + "\"";
                         } else {
                             line = CONTENT_TYPE_HEADER + "text/html; charset=UTF-8";
                         }
                         // skip winmail.dat
                         reader.readLine();
                         mstnefDetected = true;
 
                     } else if (line.startsWith(CONTENT_TRANSFER_ENCODING_HEADER)) {
                         if (hasAttachment && mstnefDetected) {
                             line = null;
                         }
                     }
 
                     if (line != null) {
                         result.append(line);
                         result.append("\n");
                     }
                     line = reader.readLine();
                 }
 
                 // exchange message : create mime part headers
                 if (boundary != null) {
                     loadAttachments();
                     // TODO : test actual header values
                     result.append("\n--").append(boundary)
                             .append("\nContent-Type: text/html; charset=UTF-8")
                             .append("\nContent-Transfer-Encoding: 7bit")
                             .append("\n\n");
 
                     for (Attachment attachment : attachments) {
                         String attachmentContentType = getAttachmentContentType(attachment.href);
                         String attachmentContentEncoding = "base64";
                         if (attachmentContentType.startsWith("text/")) {
                             attachmentContentEncoding = "quoted-printable";
                         } else if (attachmentContentType.startsWith("message/rfc822")) {
                             attachmentContentEncoding = "7bit";
                         }
 
                         result.append("\n--").append(boundary)
                                 .append("\nContent-Type: ")
                                 .append(attachmentContentType)
                                 .append(";")
                                 .append("\n\tname=\"").append(attachment.name).append("\"");
                         if (attachment.contentid != null) {
                             result.append("\nContent-ID: <")
                                     .append(attachment.contentid)
                                     .append(">");
                         }
 
                         result.append("\nContent-Transfer-Encoding: ").append(attachmentContentEncoding)
                                 .append("\n\n");
                     }
 
                     // end parts
                     result.append("--").append(boundary).append("--\n");
                 }
                 if (mstnefDetected) {
                     fullHeaders = result.toString();
                     // also fix invalid body characters
                     htmlBody = htmlBody.replaceAll("&#8217;", "'");
                 }
 
             } catch (IOException e) {
                 throw new RuntimeException("Unable to preprocess headers " + e + " " + e.getMessage());
             } finally {
                 if (reader != null) {
                     try {
                         reader.close();
                     } catch (IOException e) {
                         LOGGER.warn("Error closing header reader", e);
                     }
                 }
             }
         }
 
 
         public void write(OutputStream os) throws IOException {
             // TODO : filter submessage headers in fullHeaders
             BufferedReader reader = null;
             try {
                 reader = new BufferedReader(new StringReader(fullHeaders));
                 // skip first line
                 reader.readLine();
                 MimeHeader mimeHeader = new MimeHeader();
                 mimeHeader.processHeaders(reader, os, null);
                 // non MIME message without attachments, append body
                 if (mimeHeader.boundary == null) {
                     os.write('\r');
                     os.write('\n');
                     writeBody(os, mimeHeader);
 
                     if (hasAttachment) {
                         os.write("**warning : missing attachments**".getBytes());
                     }
                 } else {
                     attachmentIndex = 0;
 
                     loadAttachments();
                     writeMimeMessage(reader, os, mimeHeader);
                 }
                 os.flush();
             } finally {
                 if (reader != null) {
                     try {
                         reader.close();
                     } catch (IOException e) {
                         LOGGER.warn("Error closing header reader", e);
                     }
                 }
             }
         }
 
         public void writeMimeMessage(BufferedReader reader, OutputStream os, MimeHeader mimeHeader) throws IOException {
             String line;
             // with alternative, there are two body forms (plain+html)
             // with multipart/report server sends plain and delivery-status
             if ("multipart/alternative".equalsIgnoreCase(mimeHeader.contentType) ||
                     "multipart/report".equalsIgnoreCase(mimeHeader.contentType)) {
                 attachmentIndex--;
             }
 
             while (((line = reader.readLine()) != null) && !line.equals(mimeHeader.boundary + "--")) {
                 os.write(line.getBytes());
                 os.write('\r');
                 os.write('\n');
 
                 // detect part boundary start
                 if (line.equals(mimeHeader.boundary)) {
                     Attachment currentAttachment;
                     String currentAttachmentName = null;
                     if (attachmentIndex > 0 && attachmentIndex <= attachments.size()) {
                         currentAttachment = attachments.get(attachmentIndex - 1);
                         currentAttachmentName = currentAttachment.name;
                     }
 
                     // process current part header
                     MimeHeader partHeader = new MimeHeader();
                     partHeader.processHeaders(reader, os, currentAttachmentName);
 
                     // detect inner mime message
                     if (partHeader.contentType != null
                             && partHeader.contentType.startsWith("multipart")
                             && partHeader.boundary != null) {
                         writeMimeMessage(reader, os, partHeader);
                     } else if (attachmentIndex <= 0) {
                         // body part
                         attachmentIndex++;
                         writeBody(os, partHeader);
                     } else {
                         Attachment attachment = getAttachment(partHeader, attachmentIndex - 1);
 
                         attachmentIndex++;
                         if (attachment == null) {
                             // only warn, could happen depending on IIS config
                             //throw new IOException("Attachment " + partHeader.name + " not found in " + messageUrl);
                             LOGGER.warn("Attachment " + partHeader.name + " not found in " + messageUrl);
                         } else {
                             writeAttachment(os, partHeader, attachment);
                         }
                     }
                 }
             }
             // write mime end marker
             if (line != null) {
                 os.write(line.getBytes());
                 os.write('\r');
                 os.write('\n');
             }
             // failover for invalid mime headers : at least write body
             if (attachmentIndex == 0) {
                 MimeHeader bodyHeader = new MimeHeader();
                 bodyHeader.contentType = "text/html";
                 bodyHeader.charset = "utf-8";
                 StringBuffer headerBuffer = new StringBuffer();
                 headerBuffer.append(mimeHeader.boundary).append("\r\n");
                 headerBuffer.append("Content-Type: text/html; charset=UTF-8\r\n");
                 headerBuffer.append("Content-Transfer-Encoding: 7bit\r\n");
                 headerBuffer.append("\r\n");
                 os.write(headerBuffer.toString().getBytes());
                 writeBody(os, bodyHeader);
                 os.write((mimeHeader.boundary + "--\r\n").getBytes());
             }
         }
 
         protected void writeAttachment(OutputStream os, MimeHeader mimeHeader, Attachment attachment) throws IOException {
             try {
                 // quotedOs must not be closed : this would close the underlying outputstream
                 OutputStream quotedOs;
                 try {
                     // try another base64Encoder implementation
                     if ("base64".equalsIgnoreCase(mimeHeader.contentTransferEncoding)) {
                         //noinspection IOResourceOpenedButNotSafelyClosed
                         quotedOs = new BASE64EncoderStream(os);
                     } else {
                         quotedOs = (MimeUtility.encode(os, mimeHeader.contentTransferEncoding));
                     }
                 } catch (MessagingException e) {
                     throw new IOException(e + " " + e.getMessage());
                 }
 
                 if ("message/rfc822".equalsIgnoreCase(mimeHeader.contentType)) {
                     String messageAttachmentPath = null;
 
                     // Get real attachment path from owa page content
                     GetMethod method = new GetMethod(URIUtil.encodePath(attachment.href));
                     wdr.retrieveSessionInstance().executeMethod(method);
                     String body = method.getResponseBodyAsString();
                     final String URL_DECLARATION = "var g_szURL\t= \"";
                     int messageUrlBeginIndex = body.indexOf(URL_DECLARATION);
                     if (messageUrlBeginIndex >= 0) {
                         messageUrlBeginIndex = messageUrlBeginIndex + URL_DECLARATION.length();
                         int messageUrlEndIndex = body.indexOf("/\"", messageUrlBeginIndex);
                         if (messageUrlBeginIndex >= 0) {
                             messageAttachmentPath = URIUtil.decode(body.substring(messageUrlBeginIndex, messageUrlEndIndex));
                         }
                     }
 
                     // failover, exchange mail attachment
                     if (messageAttachmentPath == null) {
                         messageAttachmentPath = messageUrl + "/" + attachment.name;
                     }
 
                     Message attachedMessage = getMessage(messageAttachmentPath);
                     attachedMessage.write(quotedOs);
                 } else {
 
                     GetMethod method = new GetMethod(URIUtil.encodePath(attachment.href));
                     wdr.retrieveSessionInstance().executeMethod(method);
 
                     // encode attachment
                     BufferedInputStream bis = null;
                     try {
                         bis = new BufferedInputStream(method.getResponseBodyAsStream());
                         byte[] buffer = new byte[4096];
                         int count;
                         while ((count = bis.read(buffer)) >= 0) {
                             quotedOs.write(buffer, 0, count);
                         }
                     } finally {
                         if (bis != null) {
                             try {
                                 bis.close();
                             } catch (IOException e) {
                                 LOGGER.warn("Error closing message input stream", e);
                             }
                         }
                     }
                     quotedOs.flush();
                     os.write('\r');
                     os.write('\n');
                 }
 
             } catch (HttpException e) {
                 throw new IOException(e + " " + e.getMessage());
             }
         }
 
         protected void writeBody(OutputStream os, MimeHeader mimeHeader) throws IOException {
             OutputStream quotedOs;
             try {
                // double dot filter : avoid end of message in body
                quotedOs = new FilterOutputStream(os) {
                    byte state = 0;
                    public void write(int achar) throws IOException {
                            if (achar == 13 && state != 3) {
                                state = 1;
                            } else if (achar == 10 && state == 1) {
                                state = 2;
                            } else if (achar == '.' && state == 2) {
                                state = 3;
                            } else if (achar == 13) {
                                state = 0;
                                super.write('.');
                            } else {
                                state = 0;
                            }
                            super.write(achar);
                    }
                };
                quotedOs = (MimeUtility.encode(quotedOs, mimeHeader.contentTransferEncoding));
             } catch (MessagingException e) {
                 throw new IOException(e + " " + e.getMessage());
             }
             String currentBody = null;
             if ("text/html".equalsIgnoreCase(mimeHeader.contentType)) {
                 currentBody = htmlBody;
                 // patch charset if null and html body encoded
                 if (mimeHeader.charset == null) {
                     String delimiter = "<META HTTP-EQUIV=\"Content-Type\" CONTENT=\"text/html; charset=";
                     int delimiterIndex = htmlBody.indexOf(delimiter);
                     if (delimiterIndex < 0) {
                         delimiter = "<meta http-equiv=Content-Type content=\"text/html; charset=";
                         delimiterIndex = htmlBody.indexOf(delimiter);
                     }
                     if (delimiterIndex < 0) {
                         delimiter = "<META http-equiv=Content-Type content=\"text/html; charset=";
                         delimiterIndex = htmlBody.indexOf(delimiter);
                     }
 
                     if (delimiterIndex > 0) {
                         int startIndex = delimiterIndex + delimiter.length();
                         int endIndex = htmlBody.indexOf('"', startIndex);
                         if (endIndex > 0) {
                             mimeHeader.charset = htmlBody.substring(startIndex, endIndex);
                         }
                     }
                 }
 
             } else if (mimeHeader.contentType == null ||
                     "text/plain".equalsIgnoreCase(mimeHeader.contentType)) {
                 currentBody = body;
             }
             if (currentBody != null) {
                 if (mimeHeader.charset != null) {
                     try {
                         quotedOs.write(currentBody.getBytes(MimeUtility.javaCharset(mimeHeader.charset)));
                     } catch (UnsupportedEncodingException uee) {
                         // TODO : try to decode other encodings
                         quotedOs.write(currentBody.getBytes());
                     }
                 } else {
                     quotedOs.write(currentBody.getBytes());
                 }
                 quotedOs.flush();
                 os.write('\r');
                 os.write('\n');
             }
         }
 
         public void delete() throws IOException {
             // TODO : refactor
             String destination = deleteditemsUrl + messageUrl.substring(messageUrl.lastIndexOf("/"));
             LOGGER.debug("Deleting : " + messageUrl + " to " + destination);
 
             wdr.moveMethod(messageUrl, destination);
             if (wdr.getStatusCode() == HttpURLConnection.HTTP_PRECON_FAILED) {
                 int count = 2;
                 // name conflict, try another name
                 while (wdr.getStatusCode() == HttpURLConnection.HTTP_PRECON_FAILED) {
                     wdr.moveMethod(messageUrl, destination.substring(0, destination.lastIndexOf('.')) + "-" + count++ + ".eml");
                 }
             }
 
             LOGGER.debug("Deleted to :" + destination + " " + wdr.getStatusCode() + " " + wdr.getStatusMessage());
         }
 
         public void printHeaders(OutputStream os) throws IOException {
             String line;
             BufferedReader reader = null;
             try {
                 reader = new BufferedReader(new StringReader(fullHeaders));
                 // skip first line
                 reader.readLine();
                 line = reader.readLine();
                 while (line != null && line.length() > 0) {
                     os.write(line.getBytes());
                     os.write('\r');
                     os.write('\n');
                     line = reader.readLine();
                 }
             } finally {
                 if (reader != null) {
                     try {
                         reader.close();
                     } catch (IOException e) {
                         LOGGER.warn("Error closing header reader", e);
                     }
                 }
             }
         }
 
         /**
          * Custom head method to force connection close (needed when attachment filtered by IIS)
          */
         protected class ConnectionCloseHeadMethod extends HeadMethod {
             public ConnectionCloseHeadMethod(String url) {
                 super(url);
             }
 
             public boolean isConnectionCloseForced() {
                 // force connection if attachment not found
                 return getStatusCode() == HttpURLConnection.HTTP_NOT_FOUND;
             }
 
         }
 
         protected String getAttachmentContentType(String attachmentUrl) {
             String result;
             try {
                 String decodedPath = URIUtil.decode(attachmentUrl);
                 LOGGER.debug("Head " + decodedPath);
 
                 ConnectionCloseHeadMethod method = new ConnectionCloseHeadMethod(URIUtil.encodePathQuery(decodedPath));
                 wdr.retrieveSessionInstance().executeMethod(method);
                 if (method.getStatusCode() == HttpURLConnection.HTTP_NOT_FOUND) {
                     method.releaseConnection();
                     System.err.println("Unable to retrieve attachment");
                 }
                 result = method.getResponseHeader("Content-Type").getValue();
                 method.releaseConnection();
 
             } catch (Exception e) {
                 throw new RuntimeException("Exception retrieving " + attachmentUrl + " : " + e + " " + e.getCause());
             }
             // fix content type for known extension
             if ("application/octet-stream".equalsIgnoreCase(result) && attachmentUrl.endsWith(".pdf")) {
                 result = "application/pdf";
             }
             return result;
 
         }
 
         protected XmlDocument tidyDocument(InputStream inputStream) {
             Tidy tidy = new Tidy();
             tidy.setXmlTags(false); //treat input not XML
             tidy.setQuiet(true);
             tidy.setShowWarnings(false);
             tidy.setDocType("omit");
 
             DOMBuilder builder = new DOMBuilder();
             XmlDocument xmlDocument = new XmlDocument();
             try {
                 Document w3cDocument = tidy.parseDOM(inputStream, null);
                 Element documentElement = w3cDocument.getDocumentElement();
 
                 if (documentElement != null) {
                     // Fix broken Office xml document with empty namespace
                     NamedNodeMap namedNodeMap = w3cDocument.getDocumentElement().getAttributes();
                     for (int i = 0; i < namedNodeMap.getLength(); i++) {
                         Node node = namedNodeMap.item(i);
                         String nodeName = node.getNodeName();
                         String nodeValue = node.getNodeValue();
                         if (nodeName != null && nodeName.startsWith("xmlns")
                                 && (nodeValue == null || nodeValue.length() == 0)) {
                             w3cDocument.getDocumentElement().removeAttribute(nodeName);
                         }
                     }
                 }
                 xmlDocument.load(builder.build(w3cDocument));
             } catch (Exception ex1) {
                 LOGGER.error("Exception parsing document", ex1);
             }
             return xmlDocument;
         }
 
         public void loadAttachments() throws IOException {
             // do not load attachments twice
             if (attachments == null) {
 
                 GetMethod getMethod = new GetMethod(URIUtil.encodePathQuery(messageUrl + "?Cmd=Open&unfiltered=1"));
                 wdr.retrieveSessionInstance().executeMethod(getMethod);
                 if (getMethod.getStatusCode() != HttpURLConnection.HTTP_OK) {
                     throw new IOException("Unable to get attachments: " + getMethod.getStatusCode()
                             + " " + getMethod.getStatusLine());
                 }
 
                 XmlDocument xmlDocument = tidyDocument(getMethod.getResponseBodyAsStream());
                 // Release the connection.
                 getMethod.releaseConnection();
 
                 attachments = new ArrayList<Attachment>();
                 int attachmentIndex = 1;
                 // fix empty body in dsn report
                 if (body == null || body.length() == 0) {
                     body = xmlDocument.getValue("//textarea");
                 }
                 // list file attachments identified explicitly
                 List<Attribute> list = xmlDocument.getNodes("//table[@id='idAttachmentWell']//a/@href");
                 for (Attribute element : list) {
                     String attachmentHref = element.getValue();
                     // exclude empty links (owa buttons)
                     if (!"#".equals(attachmentHref)) {
                         final String ATTACH_QUERY = "?attach=1";
                         if (attachmentHref.endsWith(ATTACH_QUERY)) {
                             attachmentHref = attachmentHref.substring(0, attachmentHref.length() - ATTACH_QUERY.length());
                         }
                         // url is encoded
                         attachmentHref = URIUtil.decode(attachmentHref);
                         // exclude external URLs
                         if (attachmentHref.startsWith(messageUrl)) {
                             String attachmentName = attachmentHref.substring(messageUrl.length() + 1);
                             // yet another case, attached messages ?
                             final String ANOTHER_MULTIPART_STRING = "1_multipart/2_";
                             int anotherMultipartIndex = attachmentName.indexOf(ANOTHER_MULTIPART_STRING);
                             if (anotherMultipartIndex >= 0) {
                                 attachmentName = attachmentName.substring(anotherMultipartIndex + ANOTHER_MULTIPART_STRING.length());
                             }
 
                             int slashIndex = attachmentName.indexOf('/');
                             if (slashIndex >= 0) {
                                 attachmentName = attachmentName.substring(0, slashIndex);
                             }
                             // attachmentName is now right for Exchange messages, need to handle external MIME messages
                             final String MULTIPART_STRING = "1_multipart_xF8FF_";
                             if (attachmentName.startsWith(MULTIPART_STRING)) {
                                 attachmentName = attachmentName.substring(MULTIPART_STRING.length());
                                 int underscoreIndex = attachmentName.indexOf('_');
                                 if (underscoreIndex >= 0) {
                                     attachmentName = attachmentName.substring(underscoreIndex + 1);
                                 }
                             }
                             // decode slashes in attachment name
                             attachmentName = attachmentName.replaceAll("_xF8FF_", "/");
                             // decode tilde
                             attachmentName = attachmentName.replaceAll("_x007E_", "~");
                             // trim attachment name
                             attachmentName = attachmentName.trim();
 
                             Attachment attachment = new Attachment();
                             attachment.name = attachmentName;
                             attachment.href = attachmentHref;
 
                             attachments.add(attachment);
                             LOGGER.debug("Attachment " + (attachmentIndex++) + " : " + attachmentName);
                         } else {
                             LOGGER.warn("Message URL : " + messageUrl + " is not a substring of attachment URL : " + attachmentHref);
                         }
                     }
                 }
 
                 // HTML body may be empty
                 if (htmlBody != null && htmlBody.length() > 0) {
                     // get inline images from htmlBody (without OWA transformation)
                     ByteArrayInputStream bais = new ByteArrayInputStream(htmlBody
                             // quick fix remove default office namespace
                             .replaceFirst("xmlns=\".*\"", "")
                                     // quick fix remove inline processing instructions
                             .replaceAll("<\\?xml:namespace", "")
                             .replaceAll("<\\?XML:NAMESPACE", "")
                                     // quick fix invalid comments
                             .replaceAll("<!---", "<!-- -")
                             .getBytes("UTF-8"));
                     XmlDocument xmlBody = tidyDocument(bais);
                     List<Attribute> htmlBodyAllImgList = xmlBody.getNodes("//img/@src");
                     // remove absolute images from body img list
                     List<String> htmlBodyImgList = new ArrayList<String>();
                     for (Attribute imgAttribute : htmlBodyAllImgList) {
                         String value = imgAttribute.getValue();
                         if (!value.startsWith("http://") && !value.startsWith("https://")) {
                             htmlBodyImgList.add(value);
                         }
                     }
 
                     // set inline attachments flag
                     hasInlineAttachment = (htmlBodyImgList.size() > 0);
 
                     // use owa generated body to look for inline images
                     List<Attribute> imgList = xmlDocument.getNodes("//img/@src");
 
                     // TODO : add body background, does not work in thunderbird
                     // htmlBodyImgList.addAll(xmlBody.getNodes("//body/@background"));
                     // imgList.addAll(xmlDocument.getNodes("//td/@background"));
 
                     int inlineImageCount = 0;
                     for (Attribute element : imgList) {
                         String attachmentHref = element.getValue();
                         // filter external images
                         if (attachmentHref.startsWith("1_multipart")) {
                             attachmentHref = URIUtil.decode(attachmentHref);
                             if (attachmentHref.endsWith("?Security=3")) {
                                 attachmentHref = attachmentHref.substring(0, attachmentHref.indexOf('?'));
                             }
                             String attachmentName = attachmentHref.substring(attachmentHref.lastIndexOf('/') + 1);
                             // handle strange cases
                             if (attachmentName.charAt(1) == '_') {
                                 attachmentName = attachmentName.substring(2);
                             }
                             if (attachmentName.startsWith("%31_multipart%3F2_")) {
                                 attachmentName = attachmentName.substring(18);
                             }
 
                             // decode slashes
                             attachmentName = attachmentName.replaceAll("_xF8FF_", "/");
                             // exclude inline external images
                             if (!attachmentName.startsWith("http://") && !attachmentName.startsWith("https://")) {
                                 Attachment attachment = new Attachment();
                                 attachment.name = attachmentName;
                                 attachment.href = messageUrl + "/" + attachmentHref;
                                 if (htmlBodyImgList.size() > inlineImageCount) {
                                     String contentid = htmlBodyImgList.get(inlineImageCount++);
                                     if (contentid.startsWith("cid:")) {
                                         attachment.contentid = contentid.substring("cid:".length());
                                     } else {
                                         attachment.contentid = contentid;
                                         // must patch htmlBody for inline image without cid
                                         htmlBody = htmlBody.replaceFirst(attachment.contentid, "cid:" + attachment.contentid);
                                     }
                                 } else {
                                     LOGGER.warn("More images in OWA body !");
                                 }
                                 // add only inline images
                                 if (attachment.contentid != null) {
                                     attachments.add(attachment);
                                 }
                                 LOGGER.debug("Inline image attachment ID:" + attachment.contentid
                                         + " name: " + attachment.name + " href: " + attachment.href);
                             }
                         }
                     }
                 }
 
             }
         }
 
 
         protected Attachment getAttachment(MimeHeader partHeader, int attachmentIndex) {
             Attachment attachment = null;
 
             for (Attachment currentAttachment : attachments) {
                 if ((partHeader.name != null && partHeader.name.equals(currentAttachment.name)) ||
                         // TODO : test if .eml extension could be stripped from attachment name directly
                         // try to get email attachment with .eml extension
                         (partHeader.name != null && (partHeader.name + ".eml").equals(currentAttachment.name)) ||
                         // try to get attachment by content id
                         (partHeader.contentId != null && partHeader.contentId.equals(currentAttachment.contentid)) ||
                         // try to get attachment with Content-Disposition header
                         (partHeader.fileName != null && partHeader.fileName.equals(currentAttachment.name))
                         ) {
                     attachment = currentAttachment;
                     break;
                 }
             }
 
             // try to get attachment by index, only if no name found
             // or attachment renamed to winmail.dat by Exchange
             if (attachment == null && (partHeader.name == null || "winmail.dat".equals(partHeader.name))) {
                 attachment = attachments.get(attachmentIndex);
             }
 
             // try to get by index if attachment renamed to application
             if (attachment == null && partHeader.name != null) {
                 Attachment currentAttachment = attachments.get(attachmentIndex);
                 if (currentAttachment != null && currentAttachment.name.startsWith("application")) {
                     attachment = currentAttachment;
                 }
             }
             return attachment;
         }
     }
 
 
     class Attachment {
         /**
          * attachment file name
          */
         public String name = null;
         /**
          * Content ID
          */
         public String contentid = null;
         /**
          * Attachment URL
          */
         public String href = null;
     }
 
     class MimeHeader {
 
         public String contentType = null;
         public String charset = null;
         public String contentTransferEncoding = null;
         public String boundary = null;
         public String name = null;
         public String contentId = null;
         /**
          * filename in Content-Disposition header
          */
         public String fileName = null;
 
         protected void writeLine(OutputStream os, String line) throws IOException {
             if (os != null) {
                 os.write(line.getBytes());
                 os.write('\r');
                 os.write('\n');
             }
         }
 
         /**
          * Try to restore attachment names renamed by Exchange.
          *
          * @param line                  current header line
          * @param currentAttachmentName actual attachment name
          * @return patched header line
          */
         protected String fixRenamedAttachment(String line, String currentAttachmentName) {
             String result = line;
             final String WINMAIL_DAT = "name=\"winmail.dat\"";
             if (line != null && currentAttachmentName != null) {
                 int startIndex = line.indexOf(WINMAIL_DAT);
                 if (startIndex >= 0) {
                     result = line.substring(0, startIndex) +
                             "name=\"" +
                             currentAttachmentName +
                             "\"" +
                             line.substring(startIndex + WINMAIL_DAT.length());
                 }
             }
             return result;
         }
 
         public void processHeaders(BufferedReader reader, OutputStream os, String currentAttachmentName) throws IOException {
             // TODO implement a reliable header tokenizer
             // TODO : do not write header directly in outputstream, allow attachment name fix (winmail.dat)
             String line;
             line = fixRenamedAttachment(reader.readLine(), currentAttachmentName);
             while (line != null && line.length() > 0) {
 
                 writeLine(os, line);
 
                 String lineToCompare = line.toLowerCase();
 
                 if (lineToCompare.startsWith(CONTENT_TYPE_HEADER)) {
                     String contentTypeHeader = line.substring(CONTENT_TYPE_HEADER.length());
                     // handle multi-line header
                     StringBuffer header = new StringBuffer(contentTypeHeader);
                     while (line.trim().endsWith(";")) {
                         line = fixRenamedAttachment(reader.readLine(), currentAttachmentName);
                         header.append(line);
 
                         writeLine(os, line);
                     }
                     // decode header with accented file name (URL encoded)
                     int encodedIndex = header.indexOf("*=");
                     if (encodedIndex >= 0) {
                         StringBuffer decodedBuffer = new StringBuffer();
                         decodedBuffer.append(header.substring(0, header.indexOf("*")));
                         decodedBuffer.append('=');
                         // if attachment name enclosed in quotes, increase encodedIndex
                         if (header.charAt(encodedIndex + 2) == '"') {
                             decodedBuffer.append('"');
                             encodedIndex++;
                         }
                         int encodedDataIndex = header.indexOf("''");
                         if (encodedDataIndex >= 0) {
                             String encodedData = header.substring(encodedDataIndex + 2);
                             String encodedDataCharset = header.substring(encodedIndex + 2, encodedDataIndex);
                             decodedBuffer.append(URIUtil.decode(encodedData, encodedDataCharset));
                             header = decodedBuffer;
                         }
                     }
                     // internal header encoding ?
                     String decodedHeader = header.toString();
                     if (decodedHeader.indexOf("name*1*=") >= 0) {
                         decodedHeader = decodedHeader.replaceFirst("name\\*1\\*=", "");
                     }
 
                     StringTokenizer tokenizer = new StringTokenizer(decodedHeader, ";");
                     // first part is Content type
                     if (tokenizer.hasMoreTokens()) {
                         contentType = tokenizer.nextToken().trim();
                     }
                     while (tokenizer.hasMoreTokens()) {
                         String token = tokenizer.nextToken().trim();
                         int equalsIndex = token.indexOf('=');
                         if (equalsIndex > 0) {
                             String tokenName = token.substring(0, equalsIndex);
                             if ("charset".equalsIgnoreCase(tokenName)) {
                                 charset = token.substring(equalsIndex + 1);
                                 if (charset.startsWith("\"")) {
                                     charset = charset.substring(1, charset.lastIndexOf("\""));
                                 }
                             } else if ("name".equalsIgnoreCase(tokenName.toLowerCase())) {
                                 name = token.substring(equalsIndex + 1);
                                 if (name.startsWith("\"")) {
                                     // if name is on more than one line, current line does not end with "
                                     if (!name.endsWith("\"")) {
                                         // append next lines
                                         do {
                                             line = fixRenamedAttachment(reader.readLine(), currentAttachmentName);
                                             writeLine(os, line);
                                             name += line;
                                         } while (!line.trim().endsWith("\""));
                                     }
                                     name = name.substring(1, name.lastIndexOf("\""));
                                 }
                                 // name can be mime encoded
                                 name = MimeUtility.decodeText(name);
                             } else if ("boundary".equalsIgnoreCase(tokenName)) {
                                 boundary = token.substring(equalsIndex + 1);
                                 if (boundary.startsWith("\"")) {
                                     boundary = boundary.substring(1, boundary.lastIndexOf("\""));
                                 }
                                 boundary = "--" + boundary;
                             }
                         }
                     }
                 } else if (lineToCompare.startsWith(CONTENT_TRANSFER_ENCODING_HEADER)) {
                     contentTransferEncoding = line.substring(CONTENT_TRANSFER_ENCODING_HEADER.length()).trim();
                 } else if (lineToCompare.startsWith(CONTENT_DISPOSITION_HEADER)) {
                     // TODO : recode with header parser
                     int filenameIndex = line.indexOf("filename=\"");
                     if (filenameIndex >= 0) {
                         // handle multiline header
                         if (line.endsWith("\"")) {
                             fileName = line.substring(filenameIndex + 10, line.lastIndexOf("\""));
                         } else {
                             fileName = line.substring(filenameIndex + 10);
                             // read next file name line
                             line = fixRenamedAttachment(reader.readLine(), currentAttachmentName);
                             writeLine(os, line);
                             fileName += line.substring(1, line.lastIndexOf("\""));
                         }
                     }
                 } else if (lineToCompare.startsWith(CONTENT_ID_HEADER)) {
                     contentId = line.substring(CONTENT_ID_HEADER.length()).trim();
                     if (contentId.startsWith("<")) {
                         contentId = contentId.substring(1);
                     }
                     if (contentId.endsWith(">")) {
                         contentId = contentId.substring(0, contentId.length() - 1);
                     }
                 }
                 line = fixRenamedAttachment(reader.readLine(), currentAttachmentName);
             }
             writeLine(os, "");
 
             // strip header content for attached messages
             if ("message/rfc822".equalsIgnoreCase(contentType)) {
                 MimeHeader internalMimeHeader = new MimeHeader();
                 internalMimeHeader.processHeaders(reader, null, null);
                 if (internalMimeHeader.boundary != null) {
                     String endBoundary = internalMimeHeader.boundary + "--";
                     line = reader.readLine();
                     while (line != null && !endBoundary.equals(line)) {
                         line = reader.readLine();
                     }
                 }
             }
         }
     }
 }
