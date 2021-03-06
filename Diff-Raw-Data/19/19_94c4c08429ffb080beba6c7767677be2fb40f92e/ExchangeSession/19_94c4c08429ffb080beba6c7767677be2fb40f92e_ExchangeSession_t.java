 /*
  * DavMail POP/IMAP/SMTP/CalDav/LDAP Exchange Gateway
  * Copyright (C) 2009  Mickael Guessant
  *
  * This program is free software; you can redistribute it and/or
  * modify it under the terms of the GNU General Public License
  * as published by the Free Software Foundation; either version 2
  * of the License, or (at your option) any later version.
  *
  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  * GNU General Public License for more details.
  *
  * You should have received a copy of the GNU General Public License
  * along with this program; if not, write to the Free Software
  * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
  */
 package davmail.exchange;
 
 import davmail.Settings;
 import davmail.BundleMessage;
 import davmail.exception.DavMailAuthenticationException;
 import davmail.exception.DavMailException;
 import davmail.http.DavGatewayHttpClientFacade;
 import org.apache.commons.httpclient.*;
 import org.apache.commons.httpclient.params.HttpClientParams;
 import org.apache.commons.httpclient.methods.ByteArrayRequestEntity;
 import org.apache.commons.httpclient.methods.GetMethod;
 import org.apache.commons.httpclient.methods.PostMethod;
 import org.apache.commons.httpclient.methods.PutMethod;
 import org.apache.commons.httpclient.util.URIUtil;
 import org.apache.jackrabbit.webdav.MultiStatusResponse;
 import org.apache.jackrabbit.webdav.client.methods.CopyMethod;
 import org.apache.jackrabbit.webdav.client.methods.MoveMethod;
 import org.apache.jackrabbit.webdav.client.methods.PropPatchMethod;
 import org.apache.jackrabbit.webdav.property.*;
 import org.apache.jackrabbit.webdav.xml.Namespace;
 import org.apache.log4j.Logger;
 import org.htmlcleaner.CommentToken;
 import org.htmlcleaner.HtmlCleaner;
 import org.htmlcleaner.TagNode;
 
 import javax.mail.MessagingException;
 import javax.mail.internet.MimeMessage;
 import javax.mail.internet.MimeMultipart;
 import javax.mail.internet.MimePart;
 import javax.mail.internet.MimeUtility;
 import java.io.*;
 import java.net.HttpURLConnection;
 import java.net.URL;
 import java.net.UnknownHostException;
 import java.net.NoRouteToHostException;
 import java.text.ParseException;
 import java.text.SimpleDateFormat;
 import java.util.*;
 
 /**
  * Exchange session through Outlook Web Access (DAV)
  */
 public class ExchangeSession {
     protected static final Logger LOGGER = Logger.getLogger("davmail.exchange.ExchangeSession");
 
     /**
      * Reference GMT timezone to format dates
      */
     public static final SimpleTimeZone GMT_TIMEZONE = new SimpleTimeZone(0, "GMT");
 
     protected static final int FREE_BUSY_INTERVAL = 15;
 
     protected static final Namespace URN_SCHEMAS_HTTPMAIL = Namespace.getNamespace("urn:schemas:httpmail:");
     protected static final Namespace SCHEMAS_MAPI_PROPTAG = Namespace.getNamespace("http://schemas.microsoft.com/mapi/proptag/");
 
     protected static final DavPropertyNameSet EVENT_REQUEST_PROPERTIES = new DavPropertyNameSet();
 
     static {
         EVENT_REQUEST_PROPERTIES.add(DavPropertyName.GETETAG);
     }
 
     protected static final DavPropertyNameSet WELL_KNOWN_FOLDERS = new DavPropertyNameSet();
 
     static {
         WELL_KNOWN_FOLDERS.add(DavPropertyName.create("inbox", URN_SCHEMAS_HTTPMAIL));
         WELL_KNOWN_FOLDERS.add(DavPropertyName.create("deleteditems", URN_SCHEMAS_HTTPMAIL));
         WELL_KNOWN_FOLDERS.add(DavPropertyName.create("sentitems", URN_SCHEMAS_HTTPMAIL));
         WELL_KNOWN_FOLDERS.add(DavPropertyName.create("sendmsg", URN_SCHEMAS_HTTPMAIL));
         WELL_KNOWN_FOLDERS.add(DavPropertyName.create("drafts", URN_SCHEMAS_HTTPMAIL));
         WELL_KNOWN_FOLDERS.add(DavPropertyName.create("calendar", URN_SCHEMAS_HTTPMAIL));
         WELL_KNOWN_FOLDERS.add(DavPropertyName.create("contacts", URN_SCHEMAS_HTTPMAIL));
     }
 
     protected static final DavPropertyNameSet DISPLAY_NAME = new DavPropertyNameSet();
 
     static {
         DISPLAY_NAME.add(DavPropertyName.DISPLAYNAME);
     }
 
     protected static final DavPropertyNameSet FOLDER_PROPERTIES = new DavPropertyNameSet();
 
     static {
         FOLDER_PROPERTIES.add(DavPropertyName.create("hassubs"));
         FOLDER_PROPERTIES.add(DavPropertyName.create("nosubs"));
         FOLDER_PROPERTIES.add(DavPropertyName.create("unreadcount", URN_SCHEMAS_HTTPMAIL));
         FOLDER_PROPERTIES.add(DavPropertyName.create("contenttag", Namespace.getNamespace("http://schemas.microsoft.com/repl/")));
     }
 
     protected static final DavPropertyNameSet CONTENT_TAG = new DavPropertyNameSet();
 
     static {
         CONTENT_TAG.add(DavPropertyName.create("contenttag", Namespace.getNamespace("http://schemas.microsoft.com/repl/")));
     }
 
     protected static final DavPropertyNameSet RESOURCE_TAG = new DavPropertyNameSet();
 
     static {
         RESOURCE_TAG.add(DavPropertyName.create("resourcetag", Namespace.getNamespace("http://schemas.microsoft.com/repl/")));
     }
 
     protected static final DavPropertyName DEFAULT_SCHEDULE_STATE_PROPERTY = DavPropertyName.create("schedule-state", Namespace.getNamespace("CALDAV:"));
     protected DavPropertyName scheduleStateProperty = DEFAULT_SCHEDULE_STATE_PROPERTY;
 
     /**
      * Various standard mail boxes Urls
      */
     private String inboxUrl;
     private String deleteditemsUrl;
     private String sentitemsUrl;
     private String sendmsgUrl;
     private String draftsUrl;
     private String calendarUrl;
     private String contactsUrl;
 
     /**
      * Base user mailboxes path (used to select folder)
      */
     private String mailPath;
     private String email;
     private String alias;
     private final HttpClient httpClient;
 
     private final ExchangeSessionFactory.PoolKey poolKey;
 
     private boolean disableGalLookup;
     private static final String YYYY_MM_DD_HH_MM_SS = "yyyy/MM/dd HH:mm:ss";
     private static final String YYYYMMDD_T_HHMMSS_Z = "yyyyMMdd'T'HHmmss'Z'";
     private static final String YYYY_MM_DD_T_HHMMSS_Z = "yyyy-MM-dd'T'HH:mm:ss'Z'";
     private static final String YYYY_MM_DD_T_HHMMSS_SSS_Z = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";
 
     /**
      * Logon form user name field, default is username.
      */
     private String userNameInput = "username";
     /**
      * Logon form password field, default is password.
      */
     private String passwordInput = "password";
 
     /**
      * Create an exchange session for the given URL.
      * The session is not actually established until a call to login()
      *
      * @param poolKey session pool key
      * @throws IOException on error
      */
     ExchangeSession(ExchangeSessionFactory.PoolKey poolKey) throws IOException {
         this.poolKey = poolKey;
         try {
             boolean isBasicAuthentication = isBasicAuthentication(poolKey.url);
 
             httpClient = DavGatewayHttpClientFacade.getInstance(poolKey.url, poolKey.userName, poolKey.password);
             // avoid 401 roundtrips
             httpClient.getParams().setParameter(HttpClientParams.PREEMPTIVE_AUTHENTICATION, true);
 
             // get webmail root url
             // providing credentials
             // manually follow redirect
             HttpMethod method = DavGatewayHttpClientFacade.executeFollowRedirects(httpClient, poolKey.url);
 
             if (isBasicAuthentication) {
                 int status = method.getStatusCode();
 
                 if (status == HttpStatus.SC_UNAUTHORIZED) {
                     method.releaseConnection();
                     throw new DavMailAuthenticationException("EXCEPTION_AUTHENTICATION_FAILED");
                 } else if (status != HttpStatus.SC_OK) {
                     method.releaseConnection();
                     throw DavGatewayHttpClientFacade.buildHttpException(method);
                 }
             } else {
                 method = formLogin(httpClient, method, poolKey.userName, poolKey.password);
             }
 
             buildMailPath(method);
 
             // got base http mailbox http url
             getWellKnownFolders();
 
         } catch (DavMailAuthenticationException exc) {
             LOGGER.error(exc.getLogMessage());
             throw exc;
         } catch (UnknownHostException exc) {
             BundleMessage message = new BundleMessage("EXCEPTION_CONNECT", exc.getClass().getName(), exc.getMessage());
             ExchangeSession.LOGGER.error(message);
             throw new DavMailException("EXCEPTION_DAVMAIL_CONFIGURATION", message);
         } catch (IOException exc) {
             LOGGER.error(BundleMessage.formatLog("EXCEPTION_EXCHANGE_LOGIN_FAILED", exc));
             throw new DavMailException("EXCEPTION_EXCHANGE_LOGIN_FAILED", exc);
         }
         LOGGER.debug("Session " + this + " created");
     }
 
     protected String formatSearchDate(Date date) {
         SimpleDateFormat dateFormatter = new SimpleDateFormat(YYYY_MM_DD_HH_MM_SS, Locale.ENGLISH);
         dateFormatter.setTimeZone(GMT_TIMEZONE);
         return dateFormatter.format(date);
     }
 
     protected SimpleDateFormat getZuluDateFormat() {
         SimpleDateFormat dateFormat = new SimpleDateFormat(YYYYMMDD_T_HHMMSS_Z, Locale.ENGLISH);
         dateFormat.setTimeZone(GMT_TIMEZONE);
         return dateFormat;
     }
 
     protected SimpleDateFormat getExchangeZuluDateFormat() {
         SimpleDateFormat dateFormat = new SimpleDateFormat(YYYY_MM_DD_T_HHMMSS_Z, Locale.ENGLISH);
         dateFormat.setTimeZone(GMT_TIMEZONE);
         return dateFormat;
     }
 
     protected SimpleDateFormat getExchangeZuluDateFormatMillisecond() {
         SimpleDateFormat dateFormat = new SimpleDateFormat(YYYY_MM_DD_T_HHMMSS_SSS_Z, Locale.ENGLISH);
         dateFormat.setTimeZone(GMT_TIMEZONE);
         return dateFormat;
     }
 
     protected Date parseDate(String dateString) throws ParseException {
         SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd");
         dateFormat.setTimeZone(GMT_TIMEZONE);
         return dateFormat.parse(dateString);
     }
 
 
     /**
      * Test if the session expired.
      *
      * @return true this session expired
      * @throws NoRouteToHostException on error
      * @throws UnknownHostException   on error
      */
     public boolean isExpired() throws NoRouteToHostException, UnknownHostException {
         boolean isExpired = false;
         try {
             DavGatewayHttpClientFacade.executePropFindMethod(
                     httpClient, URIUtil.encodePath(inboxUrl), 0, DISPLAY_NAME);
         } catch (UnknownHostException exc) {
             throw exc;
         } catch (NoRouteToHostException exc) {
             throw exc;
         } catch (IOException e) {
             isExpired = true;
         }
 
         return isExpired;
     }
 
     /**
      * Test authentication mode : form based or basic.
      *
      * @param url exchange base URL
      * @return true if basic authentication detected
      * @throws IOException unable to connect to exchange
      */
     protected boolean isBasicAuthentication(String url) throws IOException {
         return DavGatewayHttpClientFacade.getHttpStatus(url) == HttpStatus.SC_UNAUTHORIZED;
     }
 
     protected String getAbsoluteUri(HttpMethod method, String path) throws URIException {
         URI uri = method.getURI();
         if (path != null) {
             if (path.startsWith("/")) {
                 // path is absolute, replace method path
                 uri.setPath(path);
             } else {
                 // relative path, build new path
                 String currentPath = method.getPath();
                 int end = currentPath.lastIndexOf('/');
                 if (end >= 0) {
                     uri.setPath(currentPath.substring(0, end + 1) + path);
                 } else {
                     throw new URIException(uri.getURI());
                 }
             }
         }
         return uri.getURI();
     }
 
     /**
      * Try to find logon method path from logon form body.
      *
      * @param httpClient httpClient instance
      * @param initmethod form body http method
      * @return logon method
      * @throws IOException on error
      */
     protected PostMethod buildLogonMethod(HttpClient httpClient, HttpMethod initmethod) throws IOException {
 
         PostMethod logonMethod = null;
 
         // create an instance of HtmlCleaner
         HtmlCleaner cleaner = new HtmlCleaner();
 
         try {
             TagNode node = cleaner.clean(initmethod.getResponseBodyAsStream());
             List forms = node.getElementListByName("form", true);
             if (forms.size() == 1) {
                 TagNode form = (TagNode) forms.get(0);
                 String logonMethodPath = form.getAttributeByName("action");
 
                 logonMethod = new PostMethod(getAbsoluteUri(initmethod, logonMethodPath));
 
                 List inputList = form.getElementListByName("input", true);
                 for (Object input : inputList) {
                     String type = ((TagNode) input).getAttributeByName("type");
                     String name = ((TagNode) input).getAttributeByName("name");
                     String value = ((TagNode) input).getAttributeByName("value");
                     if ("hidden".equalsIgnoreCase(type) && name != null && value != null) {
                         logonMethod.addParameter(name, value);
                     }
                     // custom login form
                     if ("txtUserName".equals(name)) {
                         userNameInput = "txtUserName";
                     } else if ("txtUserPass".equals(name)) {
                         passwordInput = "txtUserPass";
                     }
                 }
             } else {
                 List frameList = node.getElementListByName("frame", true);
                 if (frameList.size() == 1) {
                     String src = ((TagNode) frameList.get(0)).getAttributeByName("src");
                     if (src != null) {
                         LOGGER.debug("Frames detected in form page, try frame content");
                         initmethod.releaseConnection();
                         HttpMethod newInitMethod = DavGatewayHttpClientFacade.executeFollowRedirects(httpClient, src);
                         logonMethod = buildLogonMethod(httpClient, newInitMethod);
                     }
                 } else {
                     // another failover for script based logon forms (Exchange 2007)
                     List scriptList = node.getElementListByName("script", true);
                     for (Object script : scriptList) {
                         List contents = ((TagNode) script).getChildren();
                         for (Object content : contents) {
                             if (content instanceof CommentToken) {
                                 String scriptValue = ((CommentToken) content).getCommentedContent();
                                 int a_sUrlIndex = scriptValue.indexOf("var a_sUrl = \"");
                                 int a_sLgnIndex = scriptValue.indexOf("var a_sLgn = \"");
                                 if (a_sUrlIndex >= 0 && a_sLgnIndex >= 0) {
                                     a_sUrlIndex += "var a_sUrl = \"".length();
                                     a_sLgnIndex += "var a_sLgn = \"".length();
                                     int a_sUrlEndIndex = scriptValue.indexOf('\"', a_sUrlIndex);
                                     int a_sLgnEndIndex = scriptValue.indexOf('\"', a_sLgnIndex);
                                     if (a_sUrlEndIndex >= 0 && a_sLgnEndIndex >= 0) {
                                         URI initmethodURI = initmethod.getURI();
                                         initmethodURI.setQuery(
                                                 scriptValue.substring(a_sLgnIndex + 1, a_sLgnEndIndex) +
                                                         scriptValue.substring(a_sUrlIndex, a_sUrlEndIndex));
                                         String src = initmethodURI.getURI();
                                         LOGGER.debug("Detected script based logon, redirect to form at " + src);
                                         HttpMethod newInitMethod = DavGatewayHttpClientFacade.executeFollowRedirects(httpClient, src);
                                         logonMethod = buildLogonMethod(httpClient, newInitMethod);
                                     }
                                 } else {
                                     a_sLgnIndex = scriptValue.indexOf("var a_sLgnQS = \"");
                                     if (a_sUrlIndex >= 0 && a_sLgnIndex >= 0) {
                                         a_sUrlIndex += "var a_sUrl = \"".length();
                                         a_sLgnIndex += "var a_sLgnQS = \"".length();
                                         int a_sUrlEndIndex = scriptValue.indexOf('\"', a_sUrlIndex);
                                         int a_sLgnEndIndex = scriptValue.indexOf('\"', a_sLgnIndex);
                                         if (a_sUrlEndIndex >= 0 && a_sLgnEndIndex >= 0) {
                                             URI initmethodURI = initmethod.getURI();
                                             initmethodURI.setQuery(
                                                     scriptValue.substring(a_sLgnIndex + 1, a_sLgnEndIndex) +
                                                             scriptValue.substring(a_sUrlIndex, a_sUrlEndIndex));
                                             String src = initmethodURI.getURI();
                                             LOGGER.debug("Detected script based logon, redirect to form at " + src);
                                             HttpMethod newInitMethod = DavGatewayHttpClientFacade.executeFollowRedirects(httpClient, src);
                                             logonMethod = buildLogonMethod(httpClient, newInitMethod);
                                         }
                                     }
                                 }
                             }
                         }
 
 
                     }
                 }
             }
         } catch (IOException e) {
             LOGGER.error("Error parsing login form at " + initmethod.getURI());
         } finally {
             initmethod.releaseConnection();
         }
 
         if (logonMethod == null) {
             throw new DavMailException("EXCEPTION_AUTHENTICATION_FORM_NOT_FOUND", initmethod.getURI());
         }
         return logonMethod;
     }
 
     protected HttpMethod formLogin(HttpClient httpClient, HttpMethod initmethod, String userName, String password) throws IOException {
         LOGGER.debug("Form based authentication detected");
 
         HttpMethod logonMethod = buildLogonMethod(httpClient, initmethod);
         ((PostMethod) logonMethod).addParameter(userNameInput, userName);
         ((PostMethod) logonMethod).addParameter(passwordInput, password);
         ((PostMethod) logonMethod).addParameter("trusted", "4");
         logonMethod = DavGatewayHttpClientFacade.executeFollowRedirects(httpClient, logonMethod);
 
         // test form based authentication
         checkFormLoginQueryString(logonMethod);
 
         // workaround for post logon script redirect
         if (httpClient.getState().getCookies().length == 0) {
             logonMethod = buildLogonMethod(httpClient, logonMethod);
             logonMethod = DavGatewayHttpClientFacade.executeFollowRedirects(httpClient, logonMethod);
             checkFormLoginQueryString(logonMethod);
         }
 
         return logonMethod;
     }
 
     protected void checkFormLoginQueryString(HttpMethod logonMethod) throws DavMailAuthenticationException {
         String queryString = logonMethod.getQueryString();
         if (queryString != null && queryString.contains("reason=2")) {
             logonMethod.releaseConnection();
             if (poolKey.userName != null && poolKey.userName.contains("\\")) {
                 throw new DavMailAuthenticationException("EXCEPTION_AUTHENTICATION_FAILED");
             } else {
                 throw new DavMailAuthenticationException("EXCEPTION_AUTHENTICATION_FAILED_RETRY");
             }
         }
     }
 
     protected void buildMailPath(HttpMethod method) throws DavMailAuthenticationException {
         // find base url
         final String BASE_HREF = "<base href=\"";
         String line;
 
         // get user mail URL from html body (multi frame)
         BufferedReader mainPageReader = null;
         try {
             mainPageReader = new BufferedReader(new InputStreamReader(method.getResponseBodyAsStream()));
             //noinspection StatementWithEmptyBody
             while ((line = mainPageReader.readLine()) != null && line.toLowerCase().indexOf(BASE_HREF) == -1) {
             }
             if (line != null) {
                 int start = line.toLowerCase().indexOf(BASE_HREF) + BASE_HREF.length();
                 int end = line.indexOf('\"', start);
                 String mailBoxBaseHref = line.substring(start, end);
                 URL baseURL = new URL(mailBoxBaseHref);
                 mailPath = baseURL.getPath();
                 LOGGER.debug("Base href found in body, mailPath is " + mailPath);
                 buildEmail(method.getURI().getHost(), method.getPath());
                 LOGGER.debug("Current user email is " + email);
             } else {
                 // failover for Exchange 2007 : build standard mailbox link with email
                 buildEmail(method.getURI().getHost(), method.getPath());
                 mailPath = "/exchange/" + email + '/';
                 LOGGER.debug("Current user email is " + email + ", mailPath is " + mailPath);
             }
         } catch (IOException e) {
             LOGGER.error("Error parsing main page at " + method.getPath(), e);
         } finally {
             if (mainPageReader != null) {
                 try {
                     mainPageReader.close();
                 } catch (IOException e) {
                     LOGGER.error("Error parsing main page at " + method.getPath());
                 }
             }
             method.releaseConnection();
         }
 
 
         if (mailPath == null || email == null) {
             throw new DavMailAuthenticationException("EXCEPTION_AUTHENTICATION_FAILED_PASSWORD_EXPIRED");
         }
     }
 
     protected String getPropertyIfExists(DavPropertySet properties, String name, Namespace namespace) {
         DavProperty property = properties.get(name, namespace);
         if (property == null) {
             return null;
         } else {
             return (String) property.getValue();
         }
     }
 
     protected String getPropertyIfExists(DavPropertySet properties, DavPropertyName davPropertyName) {
         DavProperty property = properties.get(davPropertyName);
         if (property == null) {
             return null;
         } else {
             return (String) property.getValue();
         }
     }
 
     protected int getIntPropertyIfExists(DavPropertySet properties, String name, Namespace namespace) {
         DavProperty property = properties.get(name, namespace);
         if (property == null) {
             return 0;
         } else {
             return Integer.parseInt((String) property.getValue());
         }
     }
 
     protected long getLongPropertyIfExists(DavPropertySet properties, String name, Namespace namespace) {
         DavProperty property = properties.get(name, namespace);
         if (property == null) {
             return 0;
         } else {
             return Long.parseLong((String) property.getValue());
         }
     }
 
     protected String getURIPropertyIfExists(DavPropertySet properties, String name, Namespace namespace) throws URIException {
         DavProperty property = properties.get(name, namespace);
         if (property == null) {
             return null;
         } else {
             return URIUtil.decode((String) property.getValue());
         }
     }
 
     protected void getWellKnownFolders() throws IOException {
         // Retrieve well known URLs
         MultiStatusResponse[] responses = DavGatewayHttpClientFacade.executePropFindMethod(
                 httpClient, URIUtil.encodePath(mailPath), 0, WELL_KNOWN_FOLDERS);
         if (responses.length == 0) {
             throw new DavMailException("EXCEPTION_UNABLE_TO_GET_MAIL_FOLDERS");
         }
         DavPropertySet properties = responses[0].getProperties(HttpStatus.SC_OK);
         inboxUrl = getURIPropertyIfExists(properties, "inbox", URN_SCHEMAS_HTTPMAIL);
         deleteditemsUrl = getURIPropertyIfExists(properties, "deleteditems", URN_SCHEMAS_HTTPMAIL);
         sentitemsUrl = getURIPropertyIfExists(properties, "sentitems", URN_SCHEMAS_HTTPMAIL);
         sendmsgUrl = getURIPropertyIfExists(properties, "sendmsg", URN_SCHEMAS_HTTPMAIL);
         draftsUrl = getURIPropertyIfExists(properties, "drafts", URN_SCHEMAS_HTTPMAIL);
         calendarUrl = getURIPropertyIfExists(properties, "calendar", URN_SCHEMAS_HTTPMAIL);
         contactsUrl = getURIPropertyIfExists(properties, "contacts", URN_SCHEMAS_HTTPMAIL);
         LOGGER.debug("Inbox URL : " + inboxUrl +
                 " Trash URL : " + deleteditemsUrl +
                 " Sent URL : " + sentitemsUrl +
                 " Send URL : " + sendmsgUrl +
                 " Drafts URL : " + draftsUrl +
                 " Calendar URL : " + calendarUrl +
                 " Contacts URL : " + contactsUrl
         );
     }
 
     /**
      * Create message in specified folder.
      * Will overwrite an existing message with same subject in the same folder
      *
      * @param folderPath  Exchange folder path
      * @param messageName message name
      * @param properties  message properties (flags)
      * @param messageBody mail body
      * @throws IOException when unable to create message
      */
     public void createMessage(String folderPath, String messageName, HashMap<String, String> properties, String messageBody) throws IOException {
         String messageUrl = URIUtil.encodePathQuery(getFolderPath(folderPath) + '/' + messageName + ".EML");
         PropPatchMethod patchMethod;
         // create the message first as draft
         if (properties.containsKey("draft")) {
             patchMethod = new PropPatchMethod(messageUrl, buildProperties(properties));
             try {
                 // update message with blind carbon copy and other flags
                 int statusCode = httpClient.executeMethod(patchMethod);
                 if (statusCode != HttpStatus.SC_MULTI_STATUS) {
                     throw new DavMailException("EXCEPTION_UNABLE_TO_CREATE_MESSAGE", messageUrl, statusCode, ' ', patchMethod.getStatusLine());
                 }
 
             } finally {
                 patchMethod.releaseConnection();
             }
         }
 
         PutMethod putmethod = new PutMethod(messageUrl);
         putmethod.setRequestHeader("Translate", "f");
         putmethod.setRequestHeader("Content-Type", "message/rfc822");
         try {
             // use same encoding as client socket reader
             putmethod.setRequestEntity(new ByteArrayRequestEntity(messageBody.getBytes()));
             int code = httpClient.executeMethod(putmethod);
 
             if (code != HttpStatus.SC_OK && code != HttpStatus.SC_CREATED) {
                 throw new DavMailException("EXCEPTION_UNABLE_TO_CREATE_MESSAGE", messageUrl, code, ' ', putmethod.getStatusLine());
             }
         } finally {
             putmethod.releaseConnection();
         }
 
         // add bcc and other properties
         if (!properties.isEmpty()) {
             patchMethod = new PropPatchMethod(messageUrl, buildProperties(properties));
             try {
                 // update message with blind carbon copy and other flags
                 int statusCode = httpClient.executeMethod(patchMethod);
                 if (statusCode != HttpStatus.SC_MULTI_STATUS) {
                     throw new DavMailException("EXCEPTION_UNABLE_TO_PATCH_MESSAGE", messageUrl, statusCode, ' ', patchMethod.getStatusLine());
                 }
 
             } finally {
                 patchMethod.releaseConnection();
             }
         }
     }
 
     protected Message buildMessage(MultiStatusResponse responseEntity) throws URIException {
         Message message = new Message();
         message.messageUrl = URIUtil.decode(responseEntity.getHref());
         DavPropertySet properties = responseEntity.getProperties(HttpStatus.SC_OK);
 
         message.size = getIntPropertyIfExists(properties, "x0e080003", SCHEMAS_MAPI_PROPTAG);
         message.uid = getPropertyIfExists(properties, "uid", Namespace.getNamespace("DAV:"));
         message.imapUid = getLongPropertyIfExists(properties, "x0e230003", SCHEMAS_MAPI_PROPTAG);
         message.read = "1".equals(getPropertyIfExists(properties, "read", URN_SCHEMAS_HTTPMAIL));
         message.junk = "1".equals(getPropertyIfExists(properties, "x10830003", SCHEMAS_MAPI_PROPTAG));
         message.flagged = "2".equals(getPropertyIfExists(properties, "x10900003", SCHEMAS_MAPI_PROPTAG));
         message.draft = "9".equals(getPropertyIfExists(properties, "x0E070003", SCHEMAS_MAPI_PROPTAG));
         String x10810003 = getPropertyIfExists(properties, "x10810003", SCHEMAS_MAPI_PROPTAG);
         message.answered = "102".equals(x10810003) || "103".equals(x10810003);
         message.forwarded = "104".equals(x10810003);
         message.date = getPropertyIfExists(properties, "date", Namespace.getNamespace("urn:schemas:mailheader:"));
         message.deleted = "1".equals(getPropertyIfExists(properties, "isdeleted", Namespace.getNamespace("DAV:")));
         message.messageId = getPropertyIfExists(properties, "message-id", Namespace.getNamespace("urn:schemas:mailheader:"));
         if (message.messageId != null && message.messageId.startsWith("<") && message.messageId.endsWith(">")) {
             message.messageId = message.messageId.substring(1, message.messageId.length() - 1);
         }
 
         return message;
     }
 
     protected List<DavProperty> buildProperties(Map<String, String> properties) {
         ArrayList<DavProperty> list = new ArrayList<DavProperty>();
         for (Map.Entry<String, String> entry : properties.entrySet()) {
             if ("read".equals(entry.getKey())) {
                 list.add(new DefaultDavProperty(DavPropertyName.create("read", URN_SCHEMAS_HTTPMAIL), entry.getValue()));
             } else if ("junk".equals(entry.getKey())) {
                 list.add(new DefaultDavProperty(DavPropertyName.create("x10830003", SCHEMAS_MAPI_PROPTAG), entry.getValue()));
             } else if ("flagged".equals(entry.getKey())) {
                 list.add(new DefaultDavProperty(DavPropertyName.create("x10900003", SCHEMAS_MAPI_PROPTAG), entry.getValue()));
             } else if ("answered".equals(entry.getKey())) {
                 list.add(new DefaultDavProperty(DavPropertyName.create("x10810003", SCHEMAS_MAPI_PROPTAG), entry.getValue()));
                 if ("102".equals(entry.getValue())) {
                     list.add(new DefaultDavProperty(DavPropertyName.create("x10800003", SCHEMAS_MAPI_PROPTAG), "261"));
                 }
             } else if ("forwarded".equals(entry.getKey())) {
                 list.add(new DefaultDavProperty(DavPropertyName.create("x10810003", SCHEMAS_MAPI_PROPTAG), entry.getValue()));
                 if ("104".equals(entry.getValue())) {
                     list.add(new DefaultDavProperty(DavPropertyName.create("x10800003", SCHEMAS_MAPI_PROPTAG), "262"));
                 }
             } else if ("bcc".equals(entry.getKey())) {
                 list.add(new DefaultDavProperty(DavPropertyName.create("bcc", Namespace.getNamespace("urn:schemas:mailheader:")), entry.getValue()));
             } else if ("draft".equals(entry.getKey())) {
                 list.add(new DefaultDavProperty(DavPropertyName.create("x0E070003", SCHEMAS_MAPI_PROPTAG), entry.getValue()));
             } else if ("deleted".equals(entry.getKey())) {
                 list.add(new DefaultDavProperty(DavPropertyName.create("isdeleted"), entry.getValue()));
             } else if ("datereceived".equals(entry.getKey())) {
                 list.add(new DefaultDavProperty(DavPropertyName.create("datereceived", URN_SCHEMAS_HTTPMAIL), entry.getValue()));
             }
         }
         return list;
     }
 
     /**
      * Update given properties on message.
      *
      * @param message    Exchange message
      * @param properties Webdav properties map
      * @throws IOException on error
      */
     public void updateMessage(Message message, Map<String, String> properties) throws IOException {
         PropPatchMethod patchMethod = new PropPatchMethod(URIUtil.encodePathQuery(message.messageUrl), buildProperties(properties));
         try {
             int statusCode = httpClient.executeMethod(patchMethod);
             if (statusCode != HttpStatus.SC_MULTI_STATUS) {
                 throw new DavMailException("EXCEPTION_UNABLE_TO_UPDATE_MESSAGE");
             }
 
         } finally {
             patchMethod.releaseConnection();
         }
     }
 
     /**
      * Return folder message list with id and size only (for POP3 listener).
      *
      * @param folderName Exchange folder name
      * @return folder message list
      * @throws IOException on error
      */
     public MessageList getAllMessageUidAndSize(String folderName) throws IOException {
         return searchMessages(folderName, "\"DAV:uid\", \"http://schemas.microsoft.com/mapi/proptag/x0e080003\"", "");
     }
 
     /**
      * Search folder for messages matching conditions, with attributes needed by IMAP listener.
      *
      * @param folderName Exchange folder name
      * @param conditions conditions string in Exchange SQL syntax
      * @return message list
      * @throws IOException on error
      */
     public MessageList searchMessages(String folderName, String conditions) throws IOException {
         return searchMessages(folderName, "\"DAV:uid\", \"http://schemas.microsoft.com/mapi/proptag/x0e080003\"" +
                 "                ,\"http://schemas.microsoft.com/mapi/proptag/x0e230003\"" +
                 "                ,\"http://schemas.microsoft.com/mapi/proptag/x10830003\", \"http://schemas.microsoft.com/mapi/proptag/x10900003\"" +
                 "                ,\"http://schemas.microsoft.com/mapi/proptag/x0E070003\", \"http://schemas.microsoft.com/mapi/proptag/x10810003\"" +
                 "                ,\"urn:schemas:mailheader:message-id\", \"urn:schemas:httpmail:read\", \"DAV:isdeleted\", \"urn:schemas:mailheader:date\"", conditions);
     }
 
     /**
      * Search folder for messages matching conditions, with given attributes.
      *
      * @param folderName Exchange folder name
      * @param attributes requested Webdav attributes
      * @param conditions conditions string in Exchange SQL syntax
      * @return message list
      * @throws IOException on error
      */
     public MessageList searchMessages(String folderName, String attributes, String conditions) throws IOException {
         String folderUrl = getFolderPath(folderName);
         MessageList messages = new MessageList();
         String searchRequest = "Select " + attributes +
                 "                FROM Scope('SHALLOW TRAVERSAL OF \"" + folderUrl + "\"')\n" +
                 "                WHERE \"DAV:ishidden\" = False AND \"DAV:isfolder\" = False\n";
         if (conditions != null) {
             searchRequest += conditions;
         }
         searchRequest += "       ORDER BY \"urn:schemas:httpmail:date\" ASC";
         MultiStatusResponse[] responses = DavGatewayHttpClientFacade.executeSearchMethod(
                 httpClient, URIUtil.encodePath(folderUrl), searchRequest);
 
         for (MultiStatusResponse response : responses) {
             Message message = buildMessage(response);
             messages.add(message);
         }
         Collections.sort(messages);
         return messages;
     }
 
     /**
      * Search folders under given folder.
      *
      * @param folderName Exchange folder name
      * @param recursive  deep search if true
      * @return list of folders
      * @throws IOException on error
      */
     public List<Folder> getSubFolders(String folderName, boolean recursive) throws IOException {
         return getSubFolders(folderName, "(\"DAV:contentclass\"='urn:content-classes:mailfolder' OR \"DAV:contentclass\"='urn:content-classes:folder')", recursive);
     }
 
     /**
      * Search calendar folders under given folder.
      *
      * @param folderName Exchange folder name
      * @param recursive  deep search if true
      * @return list of folders
      * @throws IOException on error
      */
     public List<Folder> getSubCalendarFolders(String folderName, boolean recursive) throws IOException {
         return getSubFolders(folderName, "\"DAV:contentclass\"='urn:content-classes:calendarfolder'", recursive);
     }
 
     /**
      * Search folders under given folder matching filter.
      *
      * @param folderName Exchange folder name
     * @param filter     search filter
      * @param recursive  deep search if true
      * @return list of folders
      * @throws IOException on error
      */
     public List<Folder> getSubFolders(String folderName, String filter, boolean recursive) throws IOException {
         String mode = recursive ? "DEEP" : "SHALLOW";
         List<Folder> folders = new ArrayList<Folder>();
         StringBuilder searchRequest = new StringBuilder();
         searchRequest.append("Select \"DAV:nosubs\", \"DAV:hassubs\", \"DAV:hassubs\"," +
                 "\"urn:schemas:httpmail:unreadcount\" FROM Scope('").append(mode).append(" TRAVERSAL OF \"").append(getFolderPath(folderName)).append("\"')\n" +
                 " WHERE \"DAV:ishidden\" = False AND \"DAV:isfolder\" = True \n");
         if (filter != null && filter.length() > 0) {
             searchRequest.append("                      AND ").append(filter);
         }
         MultiStatusResponse[] responses = DavGatewayHttpClientFacade.executeSearchMethod(
                 httpClient, URIUtil.encodePath(getFolderPath(folderName)), searchRequest.toString());
 
         for (MultiStatusResponse response : responses) {
             folders.add(buildFolder(response));
         }
         return folders;
     }
 
     protected Folder buildFolder(MultiStatusResponse entity) throws IOException {
         String href = URIUtil.decode(entity.getHref());
         Folder folder = new Folder();
         DavPropertySet properties = entity.getProperties(HttpStatus.SC_OK);
         folder.hasChildren = "1".equals(getPropertyIfExists(properties, "hassubs", Namespace.getNamespace("DAV:")));
         folder.noInferiors = "1".equals(getPropertyIfExists(properties, "nosubs", Namespace.getNamespace("DAV:")));
         folder.unreadCount = getIntPropertyIfExists(properties, "unreadcount", URN_SCHEMAS_HTTPMAIL);
         folder.contenttag = getPropertyIfExists(properties, "contenttag", Namespace.getNamespace("http://schemas.microsoft.com/repl/"));
 
         if (href.endsWith("/")) {
             href = href.substring(0, href.length() - 1);
         }
 
         // replace well known folder names
         if (href.startsWith(inboxUrl)) {
             folder.folderPath = href.replaceFirst(inboxUrl, "INBOX");
         } else if (href.startsWith(sentitemsUrl)) {
             folder.folderPath = href.replaceFirst(sentitemsUrl, "Sent");
         } else if (href.startsWith(draftsUrl)) {
             folder.folderPath = href.replaceFirst(draftsUrl, "Drafts");
         } else if (href.startsWith(deleteditemsUrl)) {
             folder.folderPath = href.replaceFirst(deleteditemsUrl, "Trash");
         } else {
             int index = href.indexOf(mailPath.substring(0, mailPath.length() - 1));
             if (index >= 0) {
                 if (index + mailPath.length() > href.length()) {
                     folder.folderPath = "";
                 } else {
                     folder.folderPath = href.substring(index + mailPath.length());
                 }
             } else {
                 throw new DavMailException("EXCEPTION_INVALID_FOLDER_URL", folder.folderPath);
             }
         }
         return folder;
     }
 
     /**
      * Delete oldest messages in trash.
      * keepDelay is the number of days to keep messages in trash before delete
      *
      * @throws IOException when unable to purge messages
      */
     public void purgeOldestTrashAndSentMessages() throws IOException {
         int keepDelay = Settings.getIntProperty("davmail.keepDelay");
         if (keepDelay != 0) {
             purgeOldestFolderMessages(deleteditemsUrl, keepDelay);
         }
         // this is a new feature, default is : do nothing
         int sentKeepDelay = Settings.getIntProperty("davmail.sentKeepDelay");
         if (sentKeepDelay != 0) {
             purgeOldestFolderMessages(sentitemsUrl, sentKeepDelay);
         }
     }
 
     protected void purgeOldestFolderMessages(String folderUrl, int keepDelay) throws IOException {
         Calendar cal = Calendar.getInstance();
         cal.add(Calendar.DAY_OF_MONTH, -keepDelay);
         LOGGER.debug("Delete messages in " + folderUrl + " since " + cal.getTime());
 
         String searchRequest = "Select \"DAV:uid\"" +
                 "                FROM Scope('SHALLOW TRAVERSAL OF \"" + folderUrl + "\"')\n" +
                 "                WHERE \"DAV:isfolder\" = False\n" +
                 "                   AND \"DAV:getlastmodified\" < '" + formatSearchDate(cal.getTime()) + "'\n";
         MultiStatusResponse[] responses = DavGatewayHttpClientFacade.executeSearchMethod(
                 httpClient, URIUtil.encodePath(folderUrl), searchRequest);
 
         for (MultiStatusResponse response : responses) {
             String messageUrl = URIUtil.decode(response.getHref());
 
             LOGGER.debug("Delete " + messageUrl);
             DavGatewayHttpClientFacade.executeDeleteMethod(httpClient, URIUtil.encodePath(messageUrl));
         }
     }
 
     /**
      * Send message in reader to recipients.
      * Detect visible recipients in message body to determine bcc recipients
      *
      * @param recipients recipients list
      * @param reader     message stream
      * @throws IOException on error
      */
     public void sendMessage(List<String> recipients, BufferedReader reader) throws IOException {
         String line = reader.readLine();
         StringBuilder mailBuffer = new StringBuilder();
         StringBuilder recipientBuffer = new StringBuilder();
         boolean inHeader = true;
         boolean inRecipientHeader = false;
         while (!".".equals(line)) {
             mailBuffer.append(line).append((char) 13).append((char) 10);
             line = reader.readLine();
             // Exchange 2007 : skip From: header
             if ((inHeader && line.length() >= 5)) {
                 String prefix = line.substring(0, 5).toLowerCase();
                 if ("from:".equals(prefix)) {
                     line = reader.readLine();
                 }
             }
 
             if (inHeader && line.length() == 0) {
                 inHeader = false;
             }
 
             inRecipientHeader = inRecipientHeader && line.startsWith(" ");
 
             if ((inHeader && line.length() >= 3) || inRecipientHeader) {
                 String prefix = line.substring(0, 3).toLowerCase();
                 if ("to:".equals(prefix) || "cc:".equals(prefix) || inRecipientHeader) {
                     inRecipientHeader = true;
                     recipientBuffer.append(line);
                 }
             }
         }
         // remove visible recipients from list
         List<String> visibleRecipients = new ArrayList<String>();
         for (String recipient : recipients) {
             if (recipientBuffer.indexOf(recipient) >= 0) {
                 visibleRecipients.add(recipient);
             }
         }
         recipients.removeAll(visibleRecipients);
 
         StringBuilder bccBuffer = new StringBuilder();
         for (String recipient : recipients) {
             if (bccBuffer.length() > 0) {
                 bccBuffer.append(',');
             }
             bccBuffer.append('<');
             bccBuffer.append(recipient);
             bccBuffer.append('>');
         }
 
         String bcc = bccBuffer.toString();
         HashMap<String, String> properties = new HashMap<String, String>();
         if (bcc.length() > 0) {
             properties.put("bcc", bcc);
         }
 
         String messageName = UUID.randomUUID().toString();
 
         createMessage("Drafts", messageName, properties, mailBuffer.toString());
 
         String tempUrl = draftsUrl + '/' + messageName + ".EML";
         MoveMethod method = new MoveMethod(URIUtil.encodePath(tempUrl), URIUtil.encodePath(sendmsgUrl), true);
         int status = DavGatewayHttpClientFacade.executeHttpMethod(httpClient, method);
         if (status != HttpStatus.SC_OK) {
             throw DavGatewayHttpClientFacade.buildHttpException(method);
         }
     }
 
     /**
      * Convert logical or relative folder path to absolute folder path.
      *
      * @param folderName folder name
      * @return folder path
      */
     public String getFolderPath(String folderName) {
         String folderPath;
         if (folderName.startsWith("INBOX")) {
             folderPath = folderName.replaceFirst("INBOX", inboxUrl);
         } else if (folderName.startsWith("Trash")) {
             folderPath = folderName.replaceFirst("Trash", deleteditemsUrl);
         } else if (folderName.startsWith("Drafts")) {
             folderPath = folderName.replaceFirst("Drafts", draftsUrl);
         } else if (folderName.startsWith("Sent")) {
             folderPath = folderName.replaceFirst("Sent", sentitemsUrl);
         } else if (folderName.startsWith("calendar")) {
             folderPath = folderName.replaceFirst("calendar", calendarUrl);
             // absolute folder path
         } else if (folderName.startsWith("/")) {
             folderPath = folderName;
         } else {
             folderPath = mailPath + folderName;
         }
         return folderPath;
     }
 
     /**
      * Get folder object.
      * Folder name can be logical names INBOX, Drafts, Trash or calendar,
      * or a path relative to user base folder or absolute path.
      *
      * @param folderName folder name
      * @return Folder object
      * @throws IOException on error
      */
     public Folder getFolder(String folderName) throws IOException {
         MultiStatusResponse[] responses = DavGatewayHttpClientFacade.executePropFindMethod(
                 httpClient, URIUtil.encodePath(getFolderPath(folderName)), 0, FOLDER_PROPERTIES);
         Folder folder = null;
         if (responses.length > 0) {
             folder = buildFolder(responses[0]);
             folder.folderName = folderName;
         }
         return folder;
     }
 
     /**
      * Check folder ctag and reload messages as needed.
      *
      * @param currentFolder current folder
      * @return current folder or new refreshed folder
      * @throws IOException on error
      * @deprecated no longer used: breaks Outlook IMAP
      */
     public Folder refreshFolder(Folder currentFolder) throws IOException {
         Folder newFolder = getFolder(currentFolder.folderName);
         if (currentFolder.contenttag == null || !currentFolder.contenttag.equals(newFolder.contenttag)) {
             if (LOGGER.isDebugEnabled()) {
                 LOGGER.debug("Contenttag changed on " + currentFolder.folderName + ' '
                         + currentFolder.contenttag + " => " + newFolder.contenttag + ", reloading messages");
             }
             newFolder.loadMessages();
             return newFolder;
         } else {
             return currentFolder;
         }
     }
 
     /**
      * Create Exchange folder.
      *
      * @param folderName logical folder name
      * @throws IOException on error
      */
     public void createFolder(String folderName) throws IOException {
         String folderPath = getFolderPath(folderName);
         ArrayList<DavProperty> list = new ArrayList<DavProperty>();
         list.add(new DefaultDavProperty(DavPropertyName.create("outlookfolderclass", Namespace.getNamespace("http://schemas.microsoft.com/exchange/")), "IPF.Note"));
         // standard MkColMethod does not take properties, override PropPatchMethod instead
         PropPatchMethod method = new PropPatchMethod(URIUtil.encodePath(folderPath), list) {
             @Override
             public String getName() {
                 return "MKCOL";
             }
         };
         int status = DavGatewayHttpClientFacade.executeHttpMethod(httpClient, method);
         // ok or alredy exists
         if (status != HttpStatus.SC_MULTI_STATUS && status != HttpStatus.SC_METHOD_NOT_ALLOWED) {
             throw DavGatewayHttpClientFacade.buildHttpException(method);
         }
     }
 
     /**
      * Delete Exchange folder.
      *
      * @param folderName logical folder name
      * @throws IOException on error
      */
     public void deleteFolder(String folderName) throws IOException {
         DavGatewayHttpClientFacade.executeDeleteMethod(httpClient, URIUtil.encodePath(getFolderPath(folderName)));
     }
 
     /**
      * Copy message to target folder
      *
      * @param message      Exchange message
      * @param targetFolder target folder
      * @throws IOException on error
      */
     public void copyMessage(Message message, String targetFolder) throws IOException {
         String messageUrl = message.messageUrl;
         String targetPath = getFolderPath(targetFolder) + messageUrl.substring(messageUrl.lastIndexOf('/'));
         CopyMethod method = new CopyMethod(URIUtil.encodePath(messageUrl), URIUtil.encodePath(targetPath), false);
         // allow rename if a message with the same name exists
         method.addRequestHeader("Allow-Rename", "t");
         try {
             int statusCode = httpClient.executeMethod(method);
             if (statusCode == HttpStatus.SC_PRECONDITION_FAILED) {
                 throw new DavMailException("EXCEPTION_UNABLE_TO_COPY_MESSAGE");
             } else if (statusCode != HttpStatus.SC_CREATED) {
                 throw DavGatewayHttpClientFacade.buildHttpException(method);
             }
         } finally {
             method.releaseConnection();
         }
     }
 
     /**
      * Move folder to target name.
      *
      * @param folderName current folder name/path
      * @param targetName target folder name/path
      * @throws IOException on error
      */
     public void moveFolder(String folderName, String targetName) throws IOException {
         String folderPath = getFolderPath(folderName);
         String targetPath = getFolderPath(targetName);
         MoveMethod method = new MoveMethod(URIUtil.encodePath(folderPath), URIUtil.encodePath(targetPath), false);
         try {
             int statusCode = httpClient.executeMethod(method);
             if (statusCode == HttpStatus.SC_PRECONDITION_FAILED) {
                 throw new DavMailException("EXCEPTION_UNABLE_TO_MOVE_FOLDER");
             } else if (statusCode != HttpStatus.SC_CREATED) {
                 throw DavGatewayHttpClientFacade.buildHttpException(method);
             }
         } finally {
             method.releaseConnection();
         }
     }
 
     protected void moveToTrash(String encodedPath, String encodedMessageName) throws IOException {
         String source = encodedPath + '/' + encodedMessageName;
         String destination = URIUtil.encodePath(deleteditemsUrl) + '/' + encodedMessageName;
         LOGGER.debug("Deleting : " + source + " to " + destination);
         MoveMethod method = new MoveMethod(source, destination, false);
         method.addRequestHeader("Allow-rename", "t");
 
         int status = DavGatewayHttpClientFacade.executeHttpMethod(httpClient, method);
         // do not throw error if already deleted
         if (status != HttpStatus.SC_CREATED && status != HttpStatus.SC_NOT_FOUND) {
             throw DavGatewayHttpClientFacade.buildHttpException(method);
         }
         if (method.getResponseHeader("Location") != null) {
             destination = method.getResponseHeader("Location").getValue();
         }
 
         LOGGER.debug("Deleted to :" + destination);
     }
 
     /**
      * Exchange folder with IMAP properties
      */
     public class Folder {
         /**
          * Logical (IMAP) folder path.
          */
         public String folderPath;
         /**
          * Folder unread message count.
          */
         public int unreadCount;
         /**
          * true if folder has subfolders (DAV:hassubs).
          */
         public boolean hasChildren;
         /**
          * true if folder has no subfolders (DAV:nosubs).
          */
         public boolean noInferiors;
         /**
          * Requested folder name
          */
         public String folderName;
         /**
          * Folder content tag (to detect folder content changes).
          */
         public String contenttag;
         /**
          * Folder message list, empty before loadMessages call.
          */
         public ExchangeSession.MessageList messages;
 
         /**
          * Get IMAP folder flags.
          *
          * @return folder flags in IMAP format
          */
         public String getFlags() {
             if (noInferiors) {
                 return "\\NoInferiors";
             } else if (hasChildren) {
                 return "\\HasChildren";
             } else {
                 return "\\HasNoChildren";
             }
         }
 
         /**
          * Load folder messages.
          *
          * @throws IOException on error
          */
         public void loadMessages() throws IOException {
             messages = searchMessages(folderPath, "");
         }
 
         /**
          * Folder message count.
          *
          * @return message count
          */
         public int count() {
             return messages.size();
         }
 
         /**
          * Compute IMAP uidnext.
          *
          * @return max(messageuids)+1
          */
         public long getUidNext() {
             return messages.get(messages.size() - 1).getImapUid() + 1;
         }
 
         /**
          * Get message uid at index.
          *
          * @param index message index
          * @return message uid
          */
         public long getImapUid(int index) {
             return messages.get(index).getImapUid();
         }
 
         /**
          * Get message at index.
          *
          * @param index message index
          * @return message
          */
         public Message get(int index) {
             return messages.get(index);
         }
     }
 
     /**
      * Exchange message.
      */
     public class Message implements Comparable {
         protected String messageUrl;
         /**
          * Message uid.
          */
         protected String uid;
         /**
          * Message IMAP uid, unique in folder (x0e230003).
          */
         protected long imapUid;
         /**
          * MAPI message size.
          */
         public int size;
         /**
          * Mail header message-id.
          */
         protected String messageId;
         /**
          * Message date (urn:schemas:mailheader:date).
          */
         public String date;
 
         /**
          * Message flag: read.
          */
         public boolean read;
         /**
          * Message flag: deleted.
          */
         public boolean deleted;
         /**
          * Message flag: junk.
          */
         public boolean junk;
         /**
          * Message flag: flagged.
          */
         public boolean flagged;
         /**
          * Message flag: draft.
          */
         public boolean draft;
         /**
          * Message flag: answered.
          */
         public boolean answered;
         /**
          * Message flag: fowarded.
          */
         public boolean forwarded;
 
         /**
          * IMAP uid , unique in folder (x0e230003)
          *
          * @return IMAP uid
          */
         public long getImapUid() {
             return imapUid;
         }
 
         /**
          * Exchange uid.
          *
          * @return uid
          */
         public String getUid() {
             return uid;
         }
 
         /**
          * Return message flags in IMAP format.
          *
          * @return IMAP flags
          */
         public String getImapFlags() {
             StringBuilder buffer = new StringBuilder();
             if (read) {
                 buffer.append("\\Seen ");
             }
             if (deleted) {
                 buffer.append("\\Deleted ");
             }
             if (flagged) {
                 buffer.append("\\Flagged ");
             }
             if (junk) {
                 buffer.append("Junk ");
             }
             if (draft) {
                 buffer.append("\\Draft ");
             }
             if (answered) {
                 buffer.append("\\Answered ");
             }
             if (forwarded) {
                 buffer.append("$Forwarded ");
             }
             return buffer.toString().trim();
         }
 
         /**
          * Write MIME message to os
          *
          * @param os output stream
          * @throws IOException on error
          */
         public void write(OutputStream os) throws IOException {
             HttpMethod method = null;
             BufferedReader reader = null;
             try {
                 method = new GetMethod(URIUtil.encodePath(messageUrl));
                 method.setRequestHeader("Content-Type", "text/xml; charset=utf-8");
                 method.setRequestHeader("Translate", "f");
                 // do not follow redirects in expired session
                 method.setFollowRedirects(false);
                 int status = httpClient.executeMethod(method);
                 if (status != HttpStatus.SC_OK) {
                     throw DavGatewayHttpClientFacade.buildHttpException(method);
                 }
 
                 reader = new BufferedReader(new InputStreamReader(method.getResponseBodyAsStream()));
                 OutputStreamWriter isoWriter = new OutputStreamWriter(os);
                 String line;
                 while ((line = reader.readLine()) != null) {
                     if (".".equals(line)) {
                         line = "..";
                         // patch text/calendar to include utf-8 encoding
                     } else if ("Content-Type: text/calendar;".equals(line)) {
                         StringBuilder headerBuffer = new StringBuilder();
                         headerBuffer.append(line);
                         while ((line = reader.readLine()) != null && line.startsWith("\t")) {
                             headerBuffer.append((char) 13);
                             headerBuffer.append((char) 10);
                             headerBuffer.append(line);
                         }
                         if (headerBuffer.indexOf("charset") < 0) {
                             headerBuffer.append(";charset=utf-8");
                         }
                         headerBuffer.append((char) 13);
                         headerBuffer.append((char) 10);
                         headerBuffer.append(line);
                         line = headerBuffer.toString();
                     }
                     isoWriter.write(line);
                     isoWriter.write((char) 13);
                     isoWriter.write((char) 10);
                 }
                 isoWriter.flush();
             } finally {
                 if (reader != null) {
                     try {
                         reader.close();
                     } catch (IOException e) {
                         LOGGER.warn("Error closing message input stream", e);
                     }
                 }
                 if (method != null) {
                     method.releaseConnection();
                 }
             }
         }
 
         /**
          * Delete message.
          *
          * @throws IOException on error
          */
         public void delete() throws IOException {
             DavGatewayHttpClientFacade.executeDeleteMethod(httpClient, URIUtil.encodePath(messageUrl));
         }
 
         /**
          * Move message to trash, mark message read.
          *
          * @throws IOException on error
          */
         public void moveToTrash() throws IOException {
             // mark message as read
             HashMap<String, String> properties = new HashMap<String, String>();
             properties.put("read", "1");
             updateMessage(this, properties);
 
             int index = messageUrl.lastIndexOf('/');
             if (index < 0) {
                 throw new DavMailException("EXCEPTION_INVALID_MESSAGE_URL", messageUrl);
             }
             String encodedPath = URIUtil.encodePath(messageUrl.substring(0, index));
             String encodedMessageName = URIUtil.encodePath(messageUrl.substring(index + 1));
             ExchangeSession.this.moveToTrash(encodedPath, encodedMessageName);
         }
 
         /**
          * Comparator to sort messages by IMAP uid
          *
          * @param message other message
          * @return imapUid comparison result
          */
         public int compareTo(Object message) {
             long compareValue = (imapUid - ((Message) message).imapUid);
             if (compareValue > 0) {
                 return 1;
             } else if (compareValue < 0) {
                 return -1;
             } else {
                 return 0;
             }
         }
 
         /**
          * Override equals, compare IMAP uids
          *
          * @param message other message
          * @return true if IMAP uids are equal
          */
         @Override
         public boolean equals(Object message) {
             return message instanceof Message && imapUid == ((Message) message).imapUid;
         }
 
         /**
          * Override hashCode, return imapUid hashcode.
          *
          * @return imapUid hashcode
          */
         @Override
         public int hashCode() {
             return (int) (imapUid ^ (imapUid >>> 32));
         }
     }
 
     /**
      * Message list
      */
     public static class MessageList extends ArrayList<Message> {
     }
 
     /**
      * Calendar event object
      */
     public class Event {
         protected String href;
         protected String etag;
 
         protected MimePart getCalendarMimePart(MimeMultipart multiPart) throws IOException, MessagingException {
             MimePart bodyPart = null;
             for (int i = 0; i < multiPart.getCount(); i++) {
                 String contentType = multiPart.getBodyPart(i).getContentType();
                 if (contentType.startsWith("text/calendar") || contentType.startsWith("application/ics")) {
                     bodyPart = (MimePart) multiPart.getBodyPart(i);
                     break;
                 } else if (contentType.startsWith("multipart")) {
                     Object content = multiPart.getBodyPart(i).getContent();
                     if (content instanceof MimeMultipart) {
                         bodyPart = getCalendarMimePart((MimeMultipart) content);
                     }
                 }
             }
 
             return bodyPart;
         }
 
         /**
          * Load ICS content from Exchange server.
          * User Translate: f header to get MIME event content and get ICS attachment from it
          *
          * @return ICS (iCalendar) event
          * @throws IOException on error
          */
         public String getICS() throws IOException {
             String result = null;
             LOGGER.debug("Get event: " + href);
             GetMethod method = new GetMethod(URIUtil.encodePath(href));
             method.setRequestHeader("Content-Type", "text/xml; charset=utf-8");
             method.setRequestHeader("Translate", "f");
             try {
                 int status = httpClient.executeMethod(method);
                 if (status != HttpStatus.SC_OK) {
                     LOGGER.warn("Unable to get event at " + href + " status: " + status);
                 }
                 MimeMessage mimeMessage = new MimeMessage(null, method.getResponseBodyAsStream());
                 Object mimeBody = mimeMessage.getContent();
                 MimePart bodyPart;
                 if (mimeBody instanceof MimeMultipart) {
                     bodyPart = getCalendarMimePart((MimeMultipart) mimeBody);
                 } else {
                     // no multipart, single body
                     bodyPart = mimeMessage;
                 }
 
                 if (bodyPart == null) {
                     ByteArrayOutputStream baos = new ByteArrayOutputStream();
                     mimeMessage.getDataHandler().writeTo(baos);
                     baos.close();
                     throw new DavMailException("EXCEPTION_INVALID_MESSAGE_CONTENT", new String(baos.toByteArray(), "UTF-8"));
                 }
                 ByteArrayOutputStream baos = new ByteArrayOutputStream();
                 bodyPart.getDataHandler().writeTo(baos);
                 baos.close();
                 result = fixICS(new String(baos.toByteArray(), "UTF-8"), true);
 
             } catch (MessagingException e) {
                 throw new DavMailException("EXCEPTION_INVALID_MESSAGE_CONTENT", e.getMessage());
             } finally {
                 method.releaseConnection();
             }
             return result;
         }
 
         /**
          * Get event name (file name part in URL).
          *
          * @return event name
          */
         public String getName() {
             int index = href.lastIndexOf('/');
             if (index >= 0) {
                 return href.substring(index + 1);
             } else {
                 return href;
             }
         }
 
         /**
          * Get event etag (last change tag).
          *
          * @return event etag
          */
         public String getEtag() {
             return etag;
         }
     }
 
     /**
      * Search calendar messages in provided folder.
      *
      * @param folderPath Exchange folder path
      * @return list of calendar messages as Event objects
      * @throws IOException on error
      */
     public List<Event> getEventMessages(String folderPath) throws IOException {
         List<Event> result;
         try {
             String searchQuery = "Select \"DAV:getetag\"" +
                     "                FROM Scope('SHALLOW TRAVERSAL OF \"" + folderPath + "\"')\n" +
                     "                WHERE \"DAV:contentclass\" = 'urn:content-classes:calendarmessage'\n" +
                     "                AND (NOT \"" + scheduleStateProperty.getNamespace().getURI() + scheduleStateProperty.getName() + "\" = 'CALDAV:schedule-processed')\n" +
                     "                ORDER BY \"urn:schemas:calendar:dtstart\" DESC\n";
             result = getEvents(folderPath, searchQuery);
         } catch (HttpException e) {
             // failover to DAV:content property on some Exchange servers
             if (DEFAULT_SCHEDULE_STATE_PROPERTY.equals(scheduleStateProperty)) {
                 scheduleStateProperty = DavPropertyName.create("comment", Namespace.getNamespace("DAV:"));
                 result = getEventMessages(folderPath);
             } else {
                 throw e;
             }
         }
         return result;
     }
 
     /**
      * Search calendar events in provided folder.
      *
      * @param folderPath Exchange folder path
      * @return list of calendar events
      * @throws IOException on error
      */
     public List<Event> getAllEvents(String folderPath) throws IOException {
         int caldavPastDelay = Settings.getIntProperty("davmail.caldavPastDelay", Integer.MAX_VALUE);
         String dateCondition = "";
         if (caldavPastDelay != Integer.MAX_VALUE) {
             Calendar cal = Calendar.getInstance();
             cal.add(Calendar.DAY_OF_MONTH, -caldavPastDelay);
             dateCondition = "                AND \"urn:schemas:calendar:dtstart\" > '" + formatSearchDate(cal.getTime()) + "'\n";
         }
 
         String searchQuery = "Select \"DAV:getetag\"" +
                 "                FROM Scope('SHALLOW TRAVERSAL OF \"" + folderPath + "\"')\n" +
                 "                WHERE (" +
                 "                       \"urn:schemas:calendar:instancetype\" is null OR" +
                 "                       \"urn:schemas:calendar:instancetype\" = 1\n" +
                 "                OR (\"urn:schemas:calendar:instancetype\" = 0\n" +
                 dateCondition +
                 "                )) AND \"DAV:contentclass\" = 'urn:content-classes:appointment'\n" +
                 "                ORDER BY \"urn:schemas:calendar:dtstart\" DESC\n";
         return getEvents(folderPath, searchQuery);
     }
 
 
     /**
      * Search calendar events or messages in provided folder matching the search query.
      *
      * @param folderPath  Exchange folder path
      * @param searchQuery Exchange search query
      * @return list of calendar messages as Event objects
      * @throws IOException on error
      */
     protected List<Event> getEvents(String folderPath, String searchQuery) throws IOException {
         List<Event> events = new ArrayList<Event>();
         MultiStatusResponse[] responses = DavGatewayHttpClientFacade.executeSearchMethod(httpClient, URIUtil.encodePath(folderPath), searchQuery);
         for (MultiStatusResponse response : responses) {
             events.add(buildEvent(response));
         }
         return events;
     }
 
     /**
      * Get event named eventName in folder
      *
      * @param folderPath Exchange folder path
      * @param eventName  event name
      * @return event object
      * @throws IOException on error
      */
     public Event getEvent(String folderPath, String eventName) throws IOException {
         String eventPath = URIUtil.encodePath(folderPath + '/' + eventName);
         MultiStatusResponse[] responses = DavGatewayHttpClientFacade.executePropFindMethod(httpClient, eventPath, 0, EVENT_REQUEST_PROPERTIES);
         if (responses.length == 0) {
             throw new DavMailException("EXCEPTION_EVENT_NOT_FOUND");
         }
         return buildEvent(responses[0]);
     }
 
     /**
      * Delete event named eventName in folder
      *
      * @param folderPath Exchange folder path
      * @param eventName  event name
      * @return HTTP status
      * @throws IOException on error
      */
     public int deleteEvent(String folderPath, String eventName) throws IOException {
         String eventPath = URIUtil.encodePath(folderPath + '/' + eventName);
         int status;
         if (inboxUrl.endsWith(folderPath)) {
             // do not delete calendar messages, mark read and processed
             ArrayList<DavProperty> list = new ArrayList<DavProperty>();
             list.add(new DefaultDavProperty(scheduleStateProperty, "CALDAV:schedule-processed"));
             list.add(new DefaultDavProperty(DavPropertyName.create("read", URN_SCHEMAS_HTTPMAIL), "1"));
             PropPatchMethod patchMethod = new PropPatchMethod(eventPath, list);
             DavGatewayHttpClientFacade.executeMethod(httpClient, patchMethod);
             status = HttpStatus.SC_OK;
         } else {
             status = DavGatewayHttpClientFacade.executeDeleteMethod(httpClient, eventPath);
         }
         return status;
     }
 
     protected Event buildEvent(MultiStatusResponse calendarResponse) throws URIException {
         Event event = new Event();
         String href = calendarResponse.getHref();
         event.href = URIUtil.decode(href);
         event.etag = getPropertyIfExists(calendarResponse.getProperties(HttpStatus.SC_OK), "getetag", Namespace.getNamespace("DAV:"));
         return event;
     }
 
     private static int dumpIndex = 1;
     private static String defaultSound = "Basso";
 
     protected void dumpICS(String icsBody, boolean fromServer, boolean after) {
         // additional setting to activate ICS dump (not available in GUI)
         if (Settings.getBooleanProperty("davmail.dumpICS")) {
             StringBuilder filePath = new StringBuilder();
             filePath.append(Settings.getLogFileDirectory()).append('/')
                     .append(dumpIndex)
                     .append(after ? "-to" : "-from")
                     .append((after ^ fromServer) ? "-server" : "-client")
                     .append(".ics");
             if ((icsBody != null) && (icsBody.length() > 0)) {
                 FileWriter fileWriter = null;
                 try {
                     fileWriter = new FileWriter(filePath.toString());
                     fileWriter.write(icsBody);
                 } catch (IOException e) {
                     LOGGER.error(e);
                 } finally {
                     if (fileWriter != null) {
                         try {
                             fileWriter.close();
                         } catch (IOException e) {
                             LOGGER.error(e);
                         }
                     }
                 }
 
 
             }
         }
 
     }
 
     protected String fixICS(String icsBody, boolean fromServer) throws IOException {
         // first pass : detect
         class AllDayState {
             boolean isAllDay;
             boolean hasCdoAllDay;
             boolean isCdoAllDay;
         }
 
         dumpIndex++;
         dumpICS(icsBody, fromServer, false);
 
         // Convert event class from and to iCal
         // See https://trac.calendarserver.org/browser/CalendarServer/trunk/doc/Extensions/caldav-privateevents.txt
         boolean isAppleiCal = false;
         boolean hasAttendee = false;
         boolean hasCdoBusyStatus = false;
         String transp = null;
         String validTimezoneId = null;
         String eventClass = null;
         String organizer = null;
         String action = null;
         boolean sound = false;
 
         List<AllDayState> allDayStates = new ArrayList<AllDayState>();
         AllDayState currentAllDayState = new AllDayState();
         BufferedReader reader = null;
         try {
             reader = new ICSBufferedReader(new StringReader(icsBody));
             String line;
             while ((line = reader.readLine()) != null) {
                 int index = line.indexOf(':');
                 if (index >= 0) {
                     String key = line.substring(0, index);
                     String value = line.substring(index + 1);
                     if ("DTSTART;VALUE=DATE".equals(key)) {
                         currentAllDayState.isAllDay = true;
                     } else if ("X-MICROSOFT-CDO-ALLDAYEVENT".equals(key)) {
                         currentAllDayState.hasCdoAllDay = true;
                         currentAllDayState.isCdoAllDay = "TRUE".equals(value);
                     } else if ("END:VEVENT".equals(line)) {
                         allDayStates.add(currentAllDayState);
                         currentAllDayState = new AllDayState();
                     } else if ("PRODID".equals(key) && line.contains("iCal")) {
                         // detect iCal created events
                         isAppleiCal = true;
                     } else if (isAppleiCal && "X-CALENDARSERVER-ACCESS".equals(key)) {
                         eventClass = value;
                     } else if (!isAppleiCal && "CLASS".equals(key)) {
                         eventClass = value;
                     } else if ("ACTION".equals(key)) {
                         action = value;
                     } else if ("ATTACH;VALUES=URI".equals(key)) {
                         // This is a marker that this event has an alarm with sound
                         sound = true;
                         // Set the default sound to whatever this event contains
                         // (under assumption that the user has the same sound set
                         //  for all events)
                         defaultSound = value;
                     } else if (key.startsWith("ORGANIZER")) {
                         if (value.startsWith("MAILTO:")) {
                             organizer = value.substring(7);
                         } else {
                             organizer = value;
                         }
                     } else if (key.startsWith("ATTENDEE")) {
                         hasAttendee = true;
                     } else if ("TRANSP".equals(key)) {
                         transp = value;
                     } else if (line.startsWith("TZID:(GMT") ||
                             // additional test for Outlook created recurring events
                             line.startsWith("TZID:GMT ")) {
                         try {
                             validTimezoneId = ResourceBundle.getBundle("timezones").getString(value);
                         } catch (MissingResourceException mre) {
                             LOGGER.warn(new BundleMessage("LOG_INVALID_TIMEZONE", value));
                         }
                     } else if ("X-MICROSOFT-CDO-BUSYSTATUS".equals(key)) {
                         hasCdoBusyStatus = true;
                     }
                 }
             }
         } finally {
             if (reader != null) {
                 reader.close();
             }
         }
         // second pass : fix
         int count = 0;
         ICSBufferedWriter result = new ICSBufferedWriter();
         try {
             reader = new ICSBufferedReader(new StringReader(icsBody));
             String line;
 
             while ((line = reader.readLine()) != null) {
                 // remove empty properties
                 if ("CLASS:".equals(line) || "LOCATION:".equals(line)) {
                     continue;
                 }
                 // fix invalid exchange timezoneid
                 if (validTimezoneId != null && line.indexOf(";TZID=") >= 0) {
                     line = fixTimezoneId(line, validTimezoneId);
                 }
                 if (!fromServer && currentAllDayState.isAllDay && "X-MICROSOFT-CDO-ALLDAYEVENT:FALSE".equals(line)) {
                     line = "X-MICROSOFT-CDO-ALLDAYEVENT:TRUE";
                 } else if (!fromServer && "END:VEVENT".equals(line) && !hasCdoBusyStatus) {
                     result.writeLine("X-MICROSOFT-CDO-BUSYSTATUS:" + (!"TRANSPARENT".equals(transp) ? "BUSY" : "FREE"));
                 } else if (!fromServer && line.startsWith("X-MICROSOFT-CDO-BUSYSTATUS:")) {
                     line = "X-MICROSOFT-CDO-BUSYSTATUS:" + (!"TRANSPARENT".equals(transp) ? "BUSY" : "FREE");
                 } else if (!fromServer && "END:VEVENT".equals(line) && currentAllDayState.isAllDay && !currentAllDayState.hasCdoAllDay) {
                     result.writeLine("X-MICROSOFT-CDO-ALLDAYEVENT:TRUE");
                 } else if (!fromServer && !currentAllDayState.isAllDay && "X-MICROSOFT-CDO-ALLDAYEVENT:TRUE".equals(line)) {
                     line = "X-MICROSOFT-CDO-ALLDAYEVENT:FALSE";
                 } else if (fromServer && currentAllDayState.isCdoAllDay && line.startsWith("DTSTART") && !line.startsWith("DTSTART;VALUE=DATE")) {
                     line = getAllDayLine(line);
                 } else if (fromServer && currentAllDayState.isCdoAllDay && line.startsWith("DTEND") && !line.startsWith("DTEND;VALUE=DATE")) {
                     line = getAllDayLine(line);
                 } else if (line.startsWith("TZID:") && validTimezoneId != null) {
                     line = "TZID:" + validTimezoneId;
                 } else if ("BEGIN:VEVENT".equals(line)) {
                     currentAllDayState = allDayStates.get(count++);
                 } else if (line.startsWith("X-CALENDARSERVER-ACCESS:")) {
                     if (!isAppleiCal) {
                         continue;
                     } else {
                         if ("CONFIDENTIAL".equalsIgnoreCase(eventClass)) {
                             result.writeLine("CLASS:PRIVATE");
                         } else if ("PRIVATE".equalsIgnoreCase(eventClass)) {
                             result.writeLine("CLASS:CONFIDENTIAL");
                         } else {
                             result.writeLine("CLASS:" + eventClass);
                         }
                     }
                 } else if (line.startsWith("EXDATE;TZID=") || line.startsWith("EXDATE:")) {
                     // Apple iCal doesn't support EXDATE with multiple exceptions
                     // on one line.  Split into multiple EXDATE entries (which is
                     // also legal according to the caldav standard).
                     splitExDate(result, line);
                     continue;
                 } else if (line.startsWith("X-ENTOURAGE_UUID:")) {
                     // Apple iCal doesn't understand this key, and it's entourage
                     // specific (i.e. not needed by any caldav client): strip it out
                     continue;
                 } else if (fromServer && line.startsWith("ATTENDEE;")
                         && (line.indexOf(email) >= 0)) {
                     // If this is coming from the server, strip out RSVP for this
                     // user as an attendee where the partstat is something other
                     // than PARTSTAT=NEEDS-ACTION since the RSVP confuses iCal4 into
                     // thinking the attendee has not replied
 
                     int rsvpSuffix = line.indexOf("RSVP=TRUE;");
                     int rsvpPrefix = line.indexOf(";RSVP=TRUE");
 
                     if (((rsvpSuffix >= 0) || (rsvpPrefix >= 0))
                             && (line.indexOf("PARTSTAT=") >= 0)
                             && (line.indexOf("PARTSTAT=NEEDS-ACTION") < 0)) {
 
                         // Strip out the "RSVP" line from the calendar entry
                         if (rsvpSuffix >= 0) {
                             line = line.substring(0, rsvpSuffix) + line.substring(rsvpSuffix + 10);
                         } else {
                             line = line.substring(0, rsvpPrefix) + line.substring(rsvpPrefix + 10);
                         }
 
                     }
                 } else if (line.startsWith("ACTION:")) {
                     if (fromServer && "DISPLAY".equals(action)) {
                         // Use the default iCal alarm action instead
                         // of the alarm Action exchange (and blackberry) understand.
                         // This is a bit of a hack because we don't know what type
                         // of alarm an iCal user really wants - but we know what the
                         // default is, and can setup the default action type
 
                         result.writeLine("ACTION:AUDIO");
 
                         if (!sound) {
                             // Add default sound into the audio alarm
                             result.writeLine("ATTACH;VALUE=URI:" + defaultSound);
                         }
 
                         continue;
                     } else if (!fromServer && "AUDIO".equals(action)) {
                         // Use the alarm action that exchange (and blackberry) understand
                         // (exchange and blackberry don't understand audio actions)
 
                         result.writeLine("ACTION:DISPLAY");
                         continue;
                     }
 
                     // Don't recognize this type of action: pass it through
 
                 } else if (line.startsWith("CLASS:")) {
                     if (isAppleiCal) {
                         continue;
                     } else {
                         if ("PRIVATE".equalsIgnoreCase(eventClass)) {
                             result.writeLine("X-CALENDARSERVER-ACCESS:CONFIDENTIAL");
                         } else if ("CONFIDENTIAL".equalsIgnoreCase(eventClass)) {
                             result.writeLine("X-CALENDARSERVER-ACCESS:PRIVATE");
                         } else {
                             result.writeLine("X-CALENDARSERVER-ACCESS:" + eventClass);
                         }
                     }
                     // remove organizer line if user is organizer for iPhone
                 } else if (fromServer && line.startsWith("ORGANIZER") && !hasAttendee) {
                     continue;
                     // add organizer line to all events created in Exchange for active sync
                 } else if (!fromServer && "END:VEVENT".equals(line) && organizer == null) {
                     result.writeLine("ORGANIZER:MAILTO:" + email);
                 } else if (organizer != null && line.startsWith("ATTENDEE") && line.contains(organizer)) {
                     // Ignore organizer as attendee
                     continue;
                 }
 
                 result.writeLine(line);
             }
         } finally {
             reader.close();
         }
 
         String resultString = result.toString();
         dumpICS(resultString, fromServer, true);
 
         return result.toString();
     }
 
     protected String fixTimezoneId(String line, String validTimezoneId) {
         int startIndex = line.indexOf("TZID=");
         int endIndex = line.indexOf(':', startIndex + 5);
         if (startIndex >= 0 && endIndex >= 0) {
             return line.substring(0, startIndex + 5) + validTimezoneId + line.substring(endIndex);
         } else {
             return line;
         }
     }
 
     protected void splitExDate(ICSBufferedWriter result, String line) {
         int cur = line.lastIndexOf(':') + 1;
         String start = line.substring(0, cur);
 
         for (int next = line.indexOf(',', cur); next != -1; next = line.indexOf(',', cur)) {
             String val = line.substring(cur, next);
             result.writeLine(start + val);
 
             cur = next + 1;
         }
 
         result.writeLine(start + line.substring(cur));
     }
 
     protected String getAllDayLine(String line) throws IOException {
         int keyIndex = line.indexOf(';');
         int valueIndex = line.lastIndexOf(':');
         int valueEndIndex = line.lastIndexOf('T');
         if (valueIndex < 0 || valueEndIndex < 0) {
             throw new DavMailException("EXCEPTION_INVALID_ICS_LINE", line);
         }
         String dateValue = line.substring(valueIndex + 1, valueEndIndex);
         String key = line.substring(0, Math.max(keyIndex, valueIndex));
         return key + ";VALUE=DATE:" + dateValue;
     }
 
 
     /**
      * Event result object to hold HTTP status and event etag from an event creation/update.
      */
     public static class EventResult {
         /**
          * HTTP status
          */
         public int status;
         /**
          * Event etag from response HTTP header
          */
         public String etag;
     }
 
     /**
      * Build and send the MIME message for the provided ICS event.
      *
      * @param icsBody event in iCalendar format
      * @return HTTP status
      * @throws IOException on error
      */
     public int sendEvent(String icsBody) throws IOException {
         String messageUrl = URIUtil.encodePathQuery(draftsUrl + '/' + UUID.randomUUID().toString() + ".EML");
         int status = internalCreateOrUpdateEvent(messageUrl, "urn:content-classes:calendarmessage", icsBody, null, null).status;
         if (status != HttpStatus.SC_CREATED) {
             return status;
         } else {
             MoveMethod method = new MoveMethod(URIUtil.encodePath(messageUrl), URIUtil.encodePath(sendmsgUrl), true);
             status = DavGatewayHttpClientFacade.executeHttpMethod(httpClient, method);
             if (status != HttpStatus.SC_OK) {
                 throw DavGatewayHttpClientFacade.buildHttpException(method);
             }
             return status;
         }
     }
 
     /**
      * Create or update event on the Exchange server
      *
      * @param folderPath Exchange folder path
      * @param eventName  event name
      * @param icsBody    event body in iCalendar format
      * @param etag       previous event etag to detect concurrent updates
      * @param noneMatch  if-none-match header value
      * @return HTTP response event result (status and etag)
      * @throws IOException on error
      */
     public EventResult createOrUpdateEvent(String folderPath, String eventName, String icsBody, String etag, String noneMatch) throws IOException {
         String messageUrl = URIUtil.encodePath(folderPath + '/' + eventName);
         return internalCreateOrUpdateEvent(messageUrl, "urn:content-classes:appointment", icsBody, etag, noneMatch);
     }
 
     protected String getICSMethod(String icsBody) {
         int methodIndex = icsBody.indexOf("METHOD:");
         if (methodIndex < 0) {
             return "REQUEST";
         }
         int startIndex = methodIndex + "METHOD:".length();
         int endIndex = icsBody.indexOf('\r', startIndex);
         if (endIndex < 0) {
             return "REQUEST";
         }
         return icsBody.substring(startIndex, endIndex);
     }
 
     protected String getICSValue(String icsBody, String prefix, String defval) throws IOException {
         BufferedReader reader = null;
 
         try {
             reader = new ICSBufferedReader(new StringReader(icsBody));
             String line;
             while ((line = reader.readLine()) != null) {
                 if (line.startsWith(prefix)) {
                     return line.substring(prefix.length());
                 }
             }
 
         } finally {
             if (reader != null) {
                 reader.close();
             }
         }
 
         return defval;
     }
 
     protected String getICSSummary(String icsBody) throws IOException {
         return getICSValue(icsBody, "SUMMARY:", BundleMessage.format("MEETING_REQUEST"));
     }
 
     protected String getICSDescription(String icsBody) throws IOException {
         String description = getICSValue(icsBody, "DESCRIPTION:", "");
 
         if ("reminder".equalsIgnoreCase(description)) {
             // Ignore this as a description text because
             // it's likely part of the valarm segment
             // (the default valarm description from outlook is "reminder")
             return "";
         }
 
         return description;
     }
 
     static class Participants {
         String attendees;
         String organizer;
     }
 
     /**
      * Parse ics event for attendees and organizer.
      * For notifications, only include attendees with RSVP=TRUE or PARTSTAT=NEEDS-ACTION
      *
      * @param icsBody        ics event
      * @param isNotification get only notified attendees
      * @return participants
      * @throws IOException on error
      */
     protected Participants getParticipants(String icsBody, boolean isNotification) throws IOException {
         HashSet<String> attendees = new HashSet<String>();
         String organizer = null;
         BufferedReader reader = null;
         try {
             reader = new ICSBufferedReader(new StringReader(icsBody));
             String line;
             while ((line = reader.readLine()) != null) {
                 int index = line.indexOf(':');
                 if (index >= 0) {
                     String key = line.substring(0, index);
                     String value = line.substring(index + 1);
                     int semiColon = key.indexOf(';');
                     if (semiColon >= 0) {
                         key = key.substring(0, semiColon);
                     }
                     if ("ORGANIZER".equals(key) || "ATTENDEE".equals(key)) {
                         int colonIndex = value.indexOf(':');
                         if (colonIndex >= 0) {
                             value = value.substring(colonIndex + 1);
                         }
                         if ("ORGANIZER".equals(key)) {
                             organizer = value;
                             // exclude current user and invalid values from recipients
                             // also exclude no action attendees
                         } else if (!email.equalsIgnoreCase(value) && value.indexOf('@') >= 0
                                 && (!isNotification
                                 || line.indexOf("RSVP=TRUE") >= 0
                                 || line.indexOf("PARTSTAT=NEEDS-ACTION") >= 0)) {
                             attendees.add(value);
                         }
                     }
                 }
             }
         } finally {
             if (reader != null) {
                 reader.close();
             }
         }
         Participants participants = new Participants();
         StringBuilder result = new StringBuilder();
         for (String recipient : attendees) {
             if (result.length() > 0) {
                 result.append(", ");
             }
             result.append(recipient);
         }
         participants.attendees = result.toString();
         participants.organizer = organizer;
         return participants;
     }
 
     protected EventResult internalCreateOrUpdateEvent(String messageUrl, String contentClass, String icsBody, String etag, String noneMatch) throws IOException {
         String uid = UUID.randomUUID().toString();
         ByteArrayOutputStream baos = new ByteArrayOutputStream();
         OutputStreamWriter writer = new OutputStreamWriter(baos, "ASCII");
         int status = 0;
         PutMethod putmethod = new PutMethod(messageUrl);
         putmethod.setRequestHeader("Translate", "f");
         putmethod.setRequestHeader("Overwrite", "f");
         if (etag != null) {
             putmethod.setRequestHeader("If-Match", etag);
         }
         if (noneMatch != null) {
             putmethod.setRequestHeader("If-None-Match", noneMatch);
         }
         putmethod.setRequestHeader("Content-Type", "message/rfc822");
         String method = getICSMethod(icsBody);
 
         writer.write("Content-Transfer-Encoding: 7bit\r\n" +
                 "Content-class: ");
         writer.write(contentClass);
         writer.write("\r\n");
         if ("urn:content-classes:calendarmessage".equals(contentClass)) {
             // need to parse attendees and organizer to build recipients
             Participants participants = getParticipants(icsBody, true);
             String recipients;
             if (email.equalsIgnoreCase(participants.organizer)) {
                 // current user is organizer => notify all
                 recipients = participants.attendees;
             } else {
                 // notify only organizer
                 recipients = participants.organizer;
             }
 
             // Make sure invites have a proper subject line
             writer.write("Subject: " + MimeUtility.encodeText(getICSSummary(icsBody), "UTF-8", null) + "\r\n");
 
             writer.write("To: ");
             writer.write(recipients);
             writer.write("\r\n");
             // do not send notification if no recipients found
             if (recipients.length() == 0) {
                 status = HttpStatus.SC_NO_CONTENT;
             }
         } else {
             // Make sure invites have a proper subject line
             writer.write("Subject: " + MimeUtility.encodeText(getICSSummary(icsBody), "UTF-8", null) + "\r\n");
 
             // need to parse attendees and organizer to build recipients
             Participants participants = getParticipants(icsBody, false);
             // storing appointment, full recipients header
             if (participants.attendees != null) {
                 writer.write("To: ");
                 writer.write(participants.attendees);
                 writer.write("\r\n");
             }
             if (participants.organizer != null) {
                 writer.write("From: ");
                 writer.write(participants.organizer);
                 writer.write("\r\n");
             }
             // if not organizer, set REPLYTIME to force Outlook in attendee mode
             if (participants.organizer != null && !email.equalsIgnoreCase(participants.organizer)) {
                 if (icsBody.indexOf("METHOD:") < 0) {
                     icsBody = icsBody.replaceAll("BEGIN:VCALENDAR", "BEGIN:VCALENDAR\r\nMETHOD:REQUEST");
                 }
                 if (icsBody.indexOf("X-MICROSOFT-CDO-REPLYTIME") < 0) {
                     icsBody = icsBody.replaceAll("END:VEVENT", "X-MICROSOFT-CDO-REPLYTIME:" +
                             getZuluDateFormat().format(new Date()) + "\r\nEND:VEVENT");
                 }
             }
         }
         writer.write("MIME-Version: 1.0\r\n" +
                 "Content-Type: multipart/alternative;\r\n" +
                 "\tboundary=\"----=_NextPart_");
         writer.write(uid);
         writer.write("\"\r\n" +
                 "\r\n" +
                 "This is a multi-part message in MIME format.\r\n" +
                 "\r\n" +
                 "------=_NextPart_");
         writer.write(uid);
 
         // Write a part of the message that contains the
         // ICS description so that invites contain the description text
         String description = getICSDescription(icsBody).replaceAll("\\\\[Nn]", "\r\n");
 
         if (description.length() > 0) {
             writer.write("\r\n" +
                     "Content-Type: text/plain;\r\n" +
                     "\tcharset=\"utf-8\"\r\n" +
                     "content-transfer-encoding: 8bit\r\n" +
                     "\r\n");
             writer.flush();
             baos.write(description.getBytes("UTF-8"));
             writer.write("\r\n" +
                     "------=_NextPart_" +
                     uid);
 
         }
 
         writer.write("\r\n" +
                 "Content-class: ");
         writer.write(contentClass);
         writer.write("\r\n" +
                 "Content-Type: text/calendar;\r\n" +
                 "\tmethod=");
         writer.write(method);
         writer.write(";\r\n" +
                 "\tcharset=\"utf-8\"\r\n" +
                 "Content-Transfer-Encoding: 8bit\r\n\r\n");
         writer.flush();
         baos.write(fixICS(icsBody, false).getBytes("UTF-8"));
         writer.write("------=_NextPart_");
         writer.write(uid);
         writer.write("--\r\n");
         writer.close();
         putmethod.setRequestEntity(new ByteArrayRequestEntity(baos.toByteArray(), "message/rfc822"));
         try {
             if (status == 0) {
                 status = httpClient.executeMethod(putmethod);
                 if (status == HttpURLConnection.HTTP_OK) {
                     if (etag != null) {
                         LOGGER.debug("Updated event " + messageUrl);
                     } else {
                         LOGGER.warn("Overwritten event " + messageUrl);
                     }
                 } else if (status != HttpURLConnection.HTTP_CREATED) {
                     LOGGER.warn("Unable to create or update message " + status + ' ' + putmethod.getStatusLine());
                 }
             }
         } finally {
             putmethod.releaseConnection();
         }
         EventResult eventResult = new EventResult();
         // 440 means forbidden on Exchange
         if (status == 440) {
             status = HttpStatus.SC_FORBIDDEN;
         }
         eventResult.status = status;
         if (putmethod.getResponseHeader("GetETag") != null) {
             eventResult.etag = putmethod.getResponseHeader("GetETag").getValue();
         }
         return eventResult;
     }
 
     /**
      * Get folder ctag (change tag).
      * This flag changes whenever folder or folder content changes
      *
      * @param folderPath Exchange folder path
      * @return folder ctag
      * @throws IOException on error
      */
     public String getFolderCtag(String folderPath) throws IOException {
         return getFolderProperty(folderPath, CONTENT_TAG);
     }
 
     /**
      * Get folder resource tag.
      * Same as etag for folders, changes when folder (not content) changes
      *
      * @param folderPath Exchange folder path
      * @return folder resource tag
      * @throws IOException on error
      */
     public String getFolderResourceTag(String folderPath) throws IOException {
         return getFolderProperty(folderPath, RESOURCE_TAG);
     }
 
     protected String getFolderProperty(String folderPath, DavPropertyNameSet davPropertyNameSet) throws IOException {
         String result;
         MultiStatusResponse[] responses = DavGatewayHttpClientFacade.executePropFindMethod(
                 httpClient, URIUtil.encodePath(folderPath), 0, davPropertyNameSet);
         if (responses.length == 0) {
             throw new DavMailException("EXCEPTION_UNABLE_TO_GET_FOLDER", folderPath);
         }
         DavPropertySet properties = responses[0].getProperties(HttpStatus.SC_OK);
         DavPropertyName davPropertyName = davPropertyNameSet.iterator().nextPropertyName();
         result = getPropertyIfExists(properties, davPropertyName);
         if (result == null) {
             throw new DavMailException("EXCEPTION_UNABLE_TO_GET_PROPERTY", davPropertyName);
         }
         return result;
     }
 
     /**
      * Get current Exchange alias name from login name
      *
      * @return user name
      */
     protected String getAliasFromLogin() {
         // Exchange 2007 : userName is login without domain
         String userName = poolKey.userName;
         int index = userName.indexOf('\\');
         if (index >= 0) {
             userName = userName.substring(index + 1);
         }
         return userName;
     }
 
     /**
      * Get current Exchange alias name from mailbox name
      *
      * @return user name
      */
     protected String getAliasFromMailPath() {
         if (mailPath == null) {
             return null;
         }
         int index = mailPath.lastIndexOf('/', mailPath.length() - 2);
         if (index >= 0 && mailPath.endsWith("/")) {
             return mailPath.substring(index + 1, mailPath.length() - 1);
         } else {
             LOGGER.warn(new BundleMessage("EXCEPTION_INVALID_MAIL_PATH", mailPath));
             return null;
         }
     }
 
     /**
      * Get user alias from mailbox display name over Webdav.
      *
      * @return user alias
      */
     public String getAliasFromMailboxDisplayName() {
         if (mailPath == null) {
             return null;
         }
         String displayName = null;
         try {
             MultiStatusResponse[] responses = DavGatewayHttpClientFacade.executePropFindMethod(
                     httpClient, URIUtil.encodePath(mailPath), 0, DISPLAY_NAME);
             if (responses.length == 0) {
                 LOGGER.warn(new BundleMessage("EXCEPTION_UNABLE_TO_GET_MAIL_FOLDER"));
             } else {
                 displayName = getPropertyIfExists(responses[0].getProperties(HttpStatus.SC_OK), "displayname", Namespace.getNamespace("DAV:"));
             }
         } catch (IOException e) {
             LOGGER.warn(new BundleMessage("EXCEPTION_UNABLE_TO_GET_MAIL_FOLDER"));
         }
         return displayName;
     }
 
     /**
      * Build Caldav calendar path for principal and folder name.
      * - prefix is current user mailbox path if principal is current user,
      * else prefix is parent folder of current user mailbox path followed by principal
      * - suffix according to well known folder names (internationalized on Exchange)
      *
      * @param principal  calendar principal
      * @param folderName requested folder name
      * @return Exchange folder path
      * @throws IOException on error
      */
     public String buildCalendarPath(String principal, String folderName) throws IOException {
         StringBuilder buffer = new StringBuilder();
        // other user calendar => replace principal folder name in mailPath
         if (principal != null && !alias.equals(principal) && !email.equals(principal)) {
             int index = mailPath.lastIndexOf('/', mailPath.length() - 2);
             if (index >= 0 && mailPath.endsWith("/")) {
                 buffer.append(mailPath.substring(0, index + 1)).append(principal).append('/');
             } else {
                 throw new DavMailException("EXCEPTION_INVALID_MAIL_PATH", mailPath);
             }
         } else if (principal != null) {
             buffer.append(mailPath);
         }

        if (folderName != null && folderName.startsWith("calendar")) {
            // replace 'calendar' folder name with i18n name
             buffer.append(calendarUrl.substring(calendarUrl.lastIndexOf('/') + 1));

            // sub calendar folder => append sub folder name
            int index = folderName.indexOf('/');
            if (index >= 0) {
                 buffer.append(folderName.substring(index));
            }
            // replace 'inbox' folder name with i18n name
         } else if ("inbox".equals(folderName)) {
             buffer.append(inboxUrl.substring(inboxUrl.lastIndexOf('/') + 1));
            // append folder name without replace (public folder)
         } else if (folderName != null && folderName.length() > 0) {
             buffer.append(folderName);
         }
         return buffer.toString();
     }
 
     /**
      * Build base path for cmd commands (galfind, gallookup).
      * This does not work with freebusy, which requires /public/
      *
      * @return cmd base path
      */
     public String getCmdBasePath() {
         if (mailPath == null) {
             return "/public/";
         } else {
             return mailPath;
         }
     }
 
     /**
      * Get user email from global address list (galfind).
      *
      * @param alias user alias
      * @return user email
      */
     public String getEmail(String alias) {
         String emailResult = null;
         if (alias != null) {
             GetMethod getMethod = null;
             String path = null;
             try {
                 path = getCmdBasePath() + "?Cmd=galfind&AN=" + URIUtil.encodeWithinQuery(alias);
                 getMethod = new GetMethod(path);
                 int status = httpClient.executeMethod(getMethod);
                 if (status != HttpStatus.SC_OK) {
                     throw new DavMailException("EXCEPTION_UNABLE_TO_GET_EMAIL", getMethod.getPath());
                 }
                 Map<String, Map<String, String>> results = XMLStreamUtil.getElementContentsAsMap(getMethod.getResponseBodyAsStream(), "item", "AN");
                 Map<String, String> result = results.get(alias.toLowerCase());
                 if (result != null) {
                     emailResult = result.get("EM");
                 }
             } catch (IOException e) {
                 LOGGER.debug("GET " + path + " failed: " + e + ' ' + e.getMessage());
             } finally {
                 if (getMethod != null) {
                     getMethod.releaseConnection();
                 }
             }
         }
         return emailResult;
     }
 
     /**
      * Determine user email through various means.
      *
      * @param hostName   Exchange server host name for last failover
      * @param methodPath current httpclient method path
      */
     public void buildEmail(String hostName, String methodPath) {
         // first try to get email from login name
         alias = getAliasFromLogin();
         email = getEmail(alias);
         // failover: use mailbox name as alias
         if (email == null) {
             alias = getAliasFromMailPath();
             email = getEmail(alias);
         }
         // another failover : get alias from mailPath display name
         if (email == null) {
             alias = getAliasFromMailboxDisplayName();
             email = getEmail(alias);
         }
         if (email == null) {
             // failover : get email from Exchange 2007 Options page
             alias = getAliasFromOptions(methodPath);
             email = getEmail(alias);
             // failover: get email from options
             if (alias != null && email == null) {
                 email = getEmailFromOptions(methodPath);
             }
         }
         if (email == null) {
             LOGGER.debug("Unable to get user email with alias " + getAliasFromLogin()
                     + " or " + getAliasFromMailPath()
                     + " or " + getAliasFromOptions(methodPath)
             );
             // last failover: build email from domain name and mailbox display name
             StringBuilder buffer = new StringBuilder();
             // most reliable alias
             alias = getAliasFromMailboxDisplayName();
             if (alias == null) {
                 alias = getAliasFromLogin();
             }
             if (alias != null) {
                 buffer.append(alias);
                 buffer.append('@');
                 int dotIndex = hostName.indexOf('.');
                 if (dotIndex >= 0) {
                     buffer.append(hostName.substring(dotIndex + 1));
                 }
             }
             email = buffer.toString();
         }
     }
 
     protected String getAliasFromOptions(String path) {
         String result = null;
         // get user mail URL from html body
         BufferedReader optionsPageReader = null;
         GetMethod optionsMethod = new GetMethod(path + "?ae=Options&t=About");
         try {
             httpClient.executeMethod(optionsMethod);
             optionsPageReader = new BufferedReader(new InputStreamReader(optionsMethod.getResponseBodyAsStream()));
             String line;
             // find mailbox full name
             final String MAILBOX_BASE = "cn=recipients/cn=";
             //noinspection StatementWithEmptyBody
             while ((line = optionsPageReader.readLine()) != null && line.toLowerCase().indexOf(MAILBOX_BASE) == -1) {
             }
             if (line != null) {
                 int start = line.toLowerCase().indexOf(MAILBOX_BASE) + MAILBOX_BASE.length();
                 int end = line.indexOf('<', start);
                 result = line.substring(start, end);
             }
         } catch (IOException e) {
             LOGGER.error("Error parsing options page at " + optionsMethod.getPath());
         } finally {
             if (optionsPageReader != null) {
                 try {
                     optionsPageReader.close();
                 } catch (IOException e) {
                     LOGGER.error("Error parsing options page at " + optionsMethod.getPath());
                 }
             }
             optionsMethod.releaseConnection();
         }
 
         return result;
     }
 
     protected String getEmailFromOptions(String path) {
         String result = null;
         // get user mail URL from html body
         BufferedReader optionsPageReader = null;
         GetMethod optionsMethod = new GetMethod(path + "?ae=Options&t=About");
         try {
             httpClient.executeMethod(optionsMethod);
             optionsPageReader = new BufferedReader(new InputStreamReader(optionsMethod.getResponseBodyAsStream()));
             String line;
             // find email
             //noinspection StatementWithEmptyBody
             while ((line = optionsPageReader.readLine()) != null
                     && (line.indexOf('[') == -1
                     || line.indexOf('@') == -1
                     || line.indexOf(']') == -1)) {
             }
             if (line != null) {
                 int start = line.toLowerCase().indexOf('[') + 1;
                 int end = line.indexOf(']', start);
                 result = line.substring(start, end);
             }
         } catch (IOException e) {
             LOGGER.error("Error parsing options page at " + optionsMethod.getPath());
         } finally {
             if (optionsPageReader != null) {
                 try {
                     optionsPageReader.close();
                 } catch (IOException e) {
                     LOGGER.error("Error parsing options page at " + optionsMethod.getPath());
                 }
             }
             optionsMethod.releaseConnection();
         }
 
         return result;
     }
 
     /**
      * Get current user email
      *
      * @return user email
      */
     public String getEmail() {
         return email;
     }
 
     /**
      * Get current user alias
      *
      * @return user email
      */
     public String getAlias() {
         return alias;
     }
 
     /**
      * Search users in global address book
      *
      * @param searchAttribute exchange search attribute
      * @param searchValue     search value
      * @return List of users
      * @throws IOException on error
      */
     public Map<String, Map<String, String>> galFind(String searchAttribute, String searchValue) throws IOException {
         Map<String, Map<String, String>> results;
         GetMethod getMethod = new GetMethod(URIUtil.encodePathQuery(getCmdBasePath() + "?Cmd=galfind&" + searchAttribute + '=' + searchValue));
         try {
             int status = httpClient.executeMethod(getMethod);
             if (status != HttpStatus.SC_OK) {
                 throw new DavMailException("EXCEPTION_UNABLE_TO_FIND_USERS", status, getMethod.getURI());
             }
             results = XMLStreamUtil.getElementContentsAsMap(getMethod.getResponseBodyAsStream(), "item", "AN");
         } finally {
             getMethod.releaseConnection();
         }
         LOGGER.debug("galfind " + searchAttribute + '=' + searchValue + ": " + results.size() + " result(s)");
         return results;
     }
 
     /**
      * Search users in contacts folder
      *
      * @param searchFilter search filter
      * @return List of users
      * @throws IOException on error
      */
     public Map<String, Map<String, String>> contactFind(String searchFilter) throws IOException {
         StringBuilder searchRequest = new StringBuilder();
         searchRequest.append("Select \"DAV:uid\", " +
                 "\"http://schemas.microsoft.com/exchange/extensionattribute1\"," +
                 "\"http://schemas.microsoft.com/exchange/extensionattribute2\"," +
                 "\"http://schemas.microsoft.com/exchange/extensionattribute3\"," +
                 "\"http://schemas.microsoft.com/exchange/extensionattribute4\"," +
                 "\"urn:schemas:contacts:bday\"," +
                 "\"urn:schemas:contacts:businesshomepage\"," +
                 "\"urn:schemas:contacts:c\"," +
                 "\"urn:schemas:contacts:cn\"," +
                 "\"urn:schemas:contacts:co\"," +
                 "\"urn:schemas:contacts:department\"," +
                 "\"urn:schemas:contacts:email1\"," +
                 "\"urn:schemas:contacts:email2\"," +
                 "\"urn:schemas:contacts:facsimiletelephonenumber\"," +
                 "\"urn:schemas:contacts:givenName\"," +
                 "\"urn:schemas:contacts:homeCity\"," +
                 "\"urn:schemas:contacts:homeCountry\"," +
                 "\"urn:schemas:contacts:homePhone\"," +
                 "\"urn:schemas:contacts:homePostalCode\"," +
                 "\"urn:schemas:contacts:homeState\"," +
                 "\"urn:schemas:contacts:homeStreet\"," +
                 "\"urn:schemas:contacts:l\"," +
                 "\"urn:schemas:contacts:manager\"," +
                 "\"urn:schemas:contacts:mobile\"," +
                 "\"urn:schemas:contacts:namesuffix\"," +
                 "\"urn:schemas:contacts:nickname\"," +
                 "\"urn:schemas:contacts:o\"," +
                 "\"urn:schemas:contacts:pager\"," +
                 "\"urn:schemas:contacts:personaltitle\"," +
                 "\"urn:schemas:contacts:postalcode\"," +
                 "\"urn:schemas:contacts:postofficebox\"," +
                 "\"urn:schemas:contacts:profession\"," +
                 "\"urn:schemas:contacts:roomnumber\"," +
                 "\"urn:schemas:contacts:secretarycn\"," +
                 "\"urn:schemas:contacts:sn\"," +
                 "\"urn:schemas:contacts:spousecn\"," +
                 "\"urn:schemas:contacts:st\"," +
                 "\"urn:schemas:contacts:street\"," +
                 "\"urn:schemas:contacts:telephoneNumber\"," +
                 "\"urn:schemas:contacts:title\"," +
                 "\"urn:schemas:httpmail:textdescription\"")
                 .append("                FROM Scope('SHALLOW TRAVERSAL OF \"").append(contactsUrl).append("\"')\n")
                 .append("                WHERE \"DAV:contentclass\" = 'urn:content-classes:person' \n");
         if (searchFilter != null && searchFilter.length() > 0) {
             searchRequest.append("                AND ").append(searchFilter);
         }
         MultiStatusResponse[] responses = DavGatewayHttpClientFacade.executeSearchMethod(
                 httpClient, URIUtil.encodePath(contactsUrl), searchRequest.toString());
 
         Map<String, Map<String, String>> results = new HashMap<String, Map<String, String>>();
         Map<String, String> item;
         for (MultiStatusResponse response : responses) {
             item = new HashMap<String, String>();
 
             DavPropertySet properties = response.getProperties(HttpStatus.SC_OK);
             DavPropertyIterator propertiesIterator = properties.iterator();
             while (propertiesIterator.hasNext()) {
                 DavProperty property = propertiesIterator.nextProperty();
                 String propertyName = property.getName().getName();
                 String propertyValue = (String) property.getValue();
                 if (propertyName.startsWith("email")) {
                     if (propertyValue != null && propertyValue.startsWith("\"")) {
                         int endIndex = propertyValue.indexOf('\"', 1);
                         if (endIndex > 0) {
                             propertyValue = propertyValue.substring(1, endIndex);
                         }
                     }
                 } else if ("bday".equals(propertyName)) {
                     SimpleDateFormat parser = getExchangeZuluDateFormatMillisecond();
                     try {
                         Calendar calendar = Calendar.getInstance();
                         calendar.setTime(parser.parse(propertyValue));
                         item.put("birthday", String.valueOf(calendar.get(Calendar.DAY_OF_MONTH)));
                         item.put("birthmonth", String.valueOf(calendar.get(Calendar.MONTH) + 1));
                         item.put("birthyear", String.valueOf(calendar.get(Calendar.YEAR)));
                         propertyValue = null;
                     } catch (ParseException e) {
                         throw new IOException(e);
                     }
                 }
                 if (propertyValue != null && propertyValue.length() > 0) {
                     item.put(propertyName, propertyValue);
                 }
             }
             results.put(item.get("uid"), item);
         }
 
         LOGGER.debug("contactFind " + ((searchFilter == null) ? "" : searchFilter) + ": " + results.size() + " result(s)");
         return results;
     }
 
     /**
      * Get extended address book information for person with gallookup.
      * Does not work with Exchange 2007
      *
      * @param person person attributes map
      */
     public void galLookup(Map<String, String> person) {
         if (!disableGalLookup) {
             GetMethod getMethod = null;
             try {
                 getMethod = new GetMethod(URIUtil.encodePathQuery(getCmdBasePath() + "?Cmd=gallookup&ADDR=" + person.get("EM")));
                 int status = httpClient.executeMethod(getMethod);
                 if (status != HttpStatus.SC_OK) {
                     throw new DavMailException("EXCEPTION_UNABLE_TO_FIND_USERS", status, getMethod.getURI());
                 }
                 Map<String, Map<String, String>> results = XMLStreamUtil.getElementContentsAsMap(getMethod.getResponseBodyAsStream(), "person", "alias");
                 // add detailed information
                 if (!results.isEmpty()) {
                     Map<String, String> fullperson = results.get(person.get("AN").toLowerCase());
                     if (fullperson != null) {
                         for (Map.Entry<String, String> entry : fullperson.entrySet()) {
                             person.put(entry.getKey(), entry.getValue());
                         }
                     }
                 }
             } catch (IOException e) {
                 LOGGER.warn("Unable to gallookup person: " + person + ", disable GalLookup");
                 disableGalLookup = true;
             } finally {
                 if (getMethod != null) {
                     getMethod.releaseConnection();
                 }
             }
         }
     }
 
     /**
      * Get freebusy info for attendee between start and end date.
      *
      * @param attendee       attendee email
      * @param startDateValue start date
      * @param endDateValue   end date
      * @return FreeBusy info
      * @throws IOException on error
      */
     public FreeBusy getFreebusy(String attendee, String startDateValue, String endDateValue) throws IOException {
 
         if (attendee.startsWith("mailto:")) {
             attendee = attendee.substring("mailto:".length());
         }
 
         SimpleDateFormat exchangeZuluDateFormat = getExchangeZuluDateFormat();
         SimpleDateFormat icalDateFormat = getZuluDateFormat();
 
         String url;
         Date startDate;
         Date endDate;
         try {
             if (startDateValue.length() == 8) {
                 startDate = parseDate(startDateValue);
             } else {
                 startDate = icalDateFormat.parse(startDateValue);
             }
             if (endDateValue.length() == 8) {
                 endDate = parseDate(endDateValue);
             } else {
                 endDate = icalDateFormat.parse(endDateValue);
             }
             url = "/public/?cmd=freebusy" +
                     "&start=" + exchangeZuluDateFormat.format(startDate) +
                     "&end=" + exchangeZuluDateFormat.format(endDate) +
                     "&interval=" + FREE_BUSY_INTERVAL +
                     "&u=SMTP:" + attendee;
         } catch (ParseException e) {
             throw new DavMailException("EXCEPTION_INVALID_DATES", e.getMessage());
         }
 
         FreeBusy freeBusy = null;
         GetMethod getMethod = new GetMethod(url);
         getMethod.setRequestHeader("Content-Type", "text/xml");
 
         try {
             int status = httpClient.executeMethod(getMethod);
             if (status != HttpStatus.SC_OK) {
                 throw new DavMailException("EXCEPTION_UNABLE_TO_GET_FREEBUSY", getMethod.getPath(),
                         status, getMethod.getResponseBodyAsString());
             }
             String body = getMethod.getResponseBodyAsString();
             int startIndex = body.lastIndexOf("<a:fbdata>");
             int endIndex = body.lastIndexOf("</a:fbdata>");
             if (startIndex >= 0 && endIndex >= 0) {
                 String fbdata = body.substring(startIndex + "<a:fbdata>".length(), endIndex);
                 freeBusy = new FreeBusy(icalDateFormat, startDate, fbdata);
             }
         } finally {
             getMethod.releaseConnection();
         }
 
         if (freeBusy != null && freeBusy.knownAttendee) {
             return freeBusy;
         } else {
             return null;
         }
     }
 
     /**
      * Exchange to iCalendar Free/Busy parser.
      * Free time returns 0, Tentative returns 1, Busy returns 2, and Out of Office (OOF) returns 3
      */
     public static final class FreeBusy {
         final SimpleDateFormat icalParser;
         boolean knownAttendee = true;
         static final HashMap<Character, String> FBTYPES = new HashMap<Character, String>();
 
         static {
             FBTYPES.put('1', "BUSY-TENTATIVE");
             FBTYPES.put('2', "BUSY");
             FBTYPES.put('3', "BUSY-UNAVAILABLE");
         }
 
         final HashMap<String, StringBuilder> busyMap = new HashMap<String, StringBuilder>();
 
         StringBuilder getBusyBuffer(char type) {
             String fbType = FBTYPES.get(Character.valueOf(type));
             StringBuilder buffer = busyMap.get(fbType);
             if (buffer == null) {
                 buffer = new StringBuilder();
                 busyMap.put(fbType, buffer);
             }
             return buffer;
         }
 
         void startBusy(char type, Calendar currentCal) {
             if (type == '4') {
                 knownAttendee = false;
             } else if (type != '0') {
                 StringBuilder busyBuffer = getBusyBuffer(type);
                 if (busyBuffer.length() > 0) {
                     busyBuffer.append(',');
                 }
                 busyBuffer.append(icalParser.format(currentCal.getTime()));
             }
         }
 
         void endBusy(char type, Calendar currentCal) {
             if (type != '0' && type != '4') {
                 getBusyBuffer(type).append('/').append(icalParser.format(currentCal.getTime()));
             }
         }
 
         FreeBusy(SimpleDateFormat icalParser, Date startDate, String fbdata) {
             this.icalParser = icalParser;
             if (fbdata.length() > 0) {
                 Calendar currentCal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
                 currentCal.setTime(startDate);
 
                 startBusy(fbdata.charAt(0), currentCal);
                 for (int i = 1; i < fbdata.length() && knownAttendee; i++) {
                     currentCal.add(Calendar.MINUTE, FREE_BUSY_INTERVAL);
                     char previousState = fbdata.charAt(i - 1);
                     char currentState = fbdata.charAt(i);
                     if (previousState != currentState) {
                         endBusy(previousState, currentCal);
                         startBusy(currentState, currentCal);
                     }
                 }
                 currentCal.add(Calendar.MINUTE, FREE_BUSY_INTERVAL);
                 endBusy(fbdata.charAt(fbdata.length() - 1), currentCal);
             }
         }
 
         /**
          * Append freebusy information to buffer.
          *
          * @param buffer String buffer
          */
         public void appendTo(StringBuilder buffer) {
             for (Map.Entry<String, StringBuilder> entry : busyMap.entrySet()) {
                 buffer.append("FREEBUSY;FBTYPE=").append(entry.getKey())
                         .append(':').append(entry.getValue()).append((char) 13).append((char) 10);
             }
         }
     }
 
     /**
      * Return internal HttpClient instance
      *
      * @return http client
      */
     public HttpClient getHttpClient() {
         return httpClient;
     }
 }
