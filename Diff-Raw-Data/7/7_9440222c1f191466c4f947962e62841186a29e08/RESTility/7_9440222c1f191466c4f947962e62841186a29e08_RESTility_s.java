 /*
  * Copyright (c) 2010 Unpublished Work of Novell, Inc. All Rights Reserved.
  *
  * THIS WORK IS AN UNPUBLISHED WORK AND CONTAINS CONFIDENTIAL,
  * PROPRIETARY AND TRADE SECRET INFORMATION OF NOVELL, INC. ACCESS TO
  * THIS WORK IS RESTRICTED TO (I) NOVELL, INC. EMPLOYEES WHO HAVE A NEED
  * TO KNOW HOW TO PERFORM TASKS WITHIN THE SCOPE OF THEIR ASSIGNMENTS AND
  * (II) ENTITIES OTHER THAN NOVELL, INC. WHO HAVE ENTERED INTO
  * APPROPRIATE LICENSE AGREEMENTS. NO PART OF THIS WORK MAY BE USED,
  * PRACTICED, PERFORMED, COPIED, DISTRIBUTED, REVISED, MODIFIED,
  * TRANSLATED, ABRIDGED, CONDENSED, EXPANDED, COLLECTED, COMPILED,
  * LINKED, RECAST, TRANSFORMED OR ADAPTED WITHOUT THE PRIOR WRITTEN
  * CONSENT OF NOVELL, INC. ANY USE OR EXPLOITATION OF THIS WORK WITHOUT
  * AUTHORIZATION COULD SUBJECT THE PERPETRATOR TO CRIMINAL AND CIVIL
  * LIABILITY.
  *
  * ========================================================================
  */
 package org.spiffyui.client.rest;
 
 import java.util.ArrayList;
 import java.util.HashMap;
 
 import com.google.gwt.core.client.GWT;
 import com.google.gwt.core.client.JavaScriptException;
 import com.google.gwt.http.client.Request;
 import com.google.gwt.http.client.RequestBuilder;
 import com.google.gwt.http.client.RequestCallback;
 import com.google.gwt.http.client.RequestException;
 import com.google.gwt.http.client.Response;
 import com.google.gwt.http.client.URL;
 import com.google.gwt.json.client.JSONObject;
 import com.google.gwt.json.client.JSONParser;
 import com.google.gwt.json.client.JSONValue;
 import com.google.gwt.user.client.Cookies;
 import com.google.gwt.user.client.Window;
 
 import org.spiffyui.client.JSUtil;
 import org.spiffyui.client.MessageUtil;
 import org.spiffyui.client.SpiffyUIStrings;
 
 /**
  * A set of utilities for calling REST from GWT.
  */
 public final class RESTility
 {
     private static final SpiffyUIStrings STRINGS = (SpiffyUIStrings) GWT.create(SpiffyUIStrings.class);
     private static final String BASIC_AUTH = "X-OPAQUE";
     private static final String SESSION_COOKIE = "Spiffy_Session";
     private static final String LOCALE_COOKIE = "Spiffy_Locale";
 
     private static final RESTility RESTILITY = new RESTility();
 
     /**
      * This method represents an HTTP GET request
      */
     public static final HTTPMethod GET = RESTILITY.new HTTPMethod("GET");
 
     /**
      * This method represents an HTTP PUT request
      */
     public static final HTTPMethod PUT = RESTILITY.new HTTPMethod("PUT");
 
     /**
      * This method represents an HTTP POST request
      */
     public static final HTTPMethod POST = RESTILITY.new HTTPMethod("POST");
 
     /**
      * This method represents an HTTP DELETE request
      */
     public static final HTTPMethod DELETE = RESTILITY.new HTTPMethod("DELETE");
 
     private static boolean g_inLoginProcess = false;
 
     private static ArrayList<RESTLoginCallBack> g_loginListeners = new ArrayList<RESTLoginCallBack>();
 
     private int m_callCount = 0;
 
     private boolean m_hasLoggedIn = false;
     
     private boolean m_logInListenerCalled = false;
 
     private static RESTAuthProvider g_authProvider;
 
     /**
      * This is a helper type class so we can pass the HTTP method as a type safe
      * object instead of a string.
      */
     public final class HTTPMethod
     {
         private String m_method;
 
         /**
          * Create a new HTTPMethod object
          *
          * @param method the method
          */
         private HTTPMethod(String method)
         {
             m_method = method;
         }
 
         /**
          * Get the method string for this object
          *
          * @return the method string
          */
         private String getMethod()
         {
             return m_method;
         }
     }
 
     private HashMap<RESTCallback, RESTCallStruct> m_restCalls = new HashMap<RESTCallback, RESTCallStruct>();
     private String m_userToken = null;
     private String m_tokenServerUrl = null;
     private String m_username = null;
     private String m_tokenServerLogoutUrl = null;
     private String m_bestLocale = null;
 
     /**
      * Just to make sure that nobody else can instatiate this object.
      */
     private RESTility()
     {
     }
 
     private static final String URI_KEY = "uri";
     private static final String SIGNOFF_URI_KEY = "signoffuri";
 
     static {
         RESTAuthProvider authUtil = new AuthUtil();
         RESTility.setAuthProvider(authUtil);
     }
 
     /**
      * <p>
      * Sets the authentication provider used for future REST requests.
      * </p>
      * 
      * <p>
      * By default authentication is provided by the AuthUtil class, but this
      * class may be replaced to provide support for custom authentication schemes.
      * </p>
      * 
      * @param authProvider
      *               the new authentication provider
      * 
      * @see AuthUtil
      */
     public static final void setAuthProvider(RESTAuthProvider authProvider)
     {
         g_authProvider = authProvider;
     }
 
     /**
      * Gets the current auth provider which will be used for future REST calls.
      * 
      * @return The current auth provider
      */
     public static final RESTAuthProvider getAuthProvider()
     {
         return g_authProvider;
     }
 
     /**
      * Make a login request using RESTility authentication framework.
      * 
      * @param callback  the rest callback called when the login is complete
      * @param response  the response from the server requiring the login
      * @param url       the URL of the authentication server
      * @param errorCode the error code from the server returned with the 401
      * 
      * @exception RESTException
      */
     public static void login(RESTCallback callback, Response response, String url, String errorCode)
         throws RESTException
     {
         RESTILITY.doLogin(callback, response, url, errorCode);
     }
 
     private void doLogin(RESTCallback callback, Response response, String url, String errorCode)
         throws RESTException
     {
         g_inLoginProcess = true;
 
         /*
          When the server returns a status code 401 they are required
          to send back the WWW-Authenticate header to tell us how to
          authenticate.
          */
         String auth = response.getHeader("WWW-Authenticate");
         if (auth == null) {
             throw new RESTException(RESTException.NO_AUTH_HEADER,
                                     "", STRINGS.noAuthHeader(),
                                     new HashMap<String, String>(),
                                     response.getStatusCode(),
                                     url);
         }
 
         /*
          * Now we have to parse out the token server URL and other information.
          *
          * The String should look like this:
          *
          * X-OPAUQUE uri=<token server URI>,signoffUri=<token server logout url>
          *
          * First we'll remove the token type
          */
         auth = auth.substring(auth.indexOf(' ') + 1);
 
         String props[] = auth.split(",");
 
         String loginUri = null;
         String logoutUri = null;
 
         for (String prop : props) {
             if (prop.trim().toLowerCase().startsWith(URI_KEY)) {
                 loginUri = prop.substring(prop.indexOf('=') + 2, prop.length() - 1).trim();
             } else if (prop.trim().toLowerCase().startsWith(SIGNOFF_URI_KEY)) {
                 logoutUri = prop.substring(prop.indexOf('=') + 2, prop.length() - 1).trim();
             }
         }
 
         if (loginUri == null || logoutUri == null) {
             throw new RESTException(RESTException.INVALID_AUTH_HEADER,
                                     "", STRINGS.invalidAuthHeader(response.getHeader("WWW-Authenticate")),
                                     new HashMap<String, String>(),
                                     response.getStatusCode(),
                                     url);
         }
 
         if (logoutUri.trim().length() == 0) {
             logoutUri = loginUri;
         }
 
         setTokenServerURL(loginUri);
         setTokenServerLogoutURL(logoutUri);
 
         Cookies.removeCookie(SESSION_COOKIE);
         Cookies.removeCookie(LOCALE_COOKIE);
         g_authProvider.showLogin(callback, loginUri, errorCode);
     }
 
     /**
      * Returns HTTPMethod corresponding to method name.
      * If the passed in method does not match any, GET is returned.
      *
      * @param method a String representation of a http method
      * @return the HTTPMethod corresponding to the passed in String method representation.
      */
     public static HTTPMethod parseString(String method)
     {
         if (POST.getMethod().equalsIgnoreCase(method)) {
             return POST;
         } else if (PUT.getMethod().equalsIgnoreCase(method)) {
             return PUT;
         } else if (DELETE.getMethod().equalsIgnoreCase(method)) {
             return DELETE;
         } else {
             //Otherwise return GET
             return GET;
         }
     }
 
     /**
      * Upon logout, delete cookie and clear out all member variables
      */
     public static void doLocalLogout()
     {
         RESTILITY.m_hasLoggedIn = false;
         RESTILITY.m_logInListenerCalled = false;
         RESTILITY.m_callCount = 0;
         RESTILITY.m_userToken = null;
         RESTILITY.m_tokenServerUrl = null;
         RESTILITY.m_tokenServerLogoutUrl = null;
         Cookies.removeCookie(SESSION_COOKIE);
         Cookies.removeCookie(LOCALE_COOKIE);
     }
 
     /**
      * In some cases, like login, the original REST call returns an error and we
      * need to run it again.  This call gets the same REST request information and
      * tries the request again.
      */
     protected static void finishRESTCalls()
     {
         for (RESTCallback callback : RESTILITY.m_restCalls.keySet()) {
             RESTCallStruct struct = RESTILITY.m_restCalls.get(callback);
             if (struct != null && struct.shouldReplay()) {
                 callREST(struct.getUrl(), struct.getData(), struct.getMethod(), callback);
             }
         }
     }
 
     /**
      * Make a rest call using an HTTP GET to the specified URL.
      *
      * @param callback the callback to invoke
      * @param url    the REST url to call
      */
     public static void callREST(String url, RESTCallback callback)
     {
         callREST(url, "", RESTility.GET, callback);
     }
 
     /**
      * Make a rest call using an HTTP GET to the specified URL including
      * the specified data..
      *
      * @param url    the REST url to call
      * @param data   the data to pass to the URL
      * @param callback the callback to invoke
      */
     public static void callREST(String url, String data, RESTCallback callback)
     {
         callREST(url, data, RESTility.GET, callback);
     }
 
     /**
      * Set the user token in JavaScript memory and and saves it in a cookie.
      * @param token  user token
      */
     protected static void setUserToken(String token)
     {
         g_inLoginProcess = false;
         RESTILITY.m_userToken = token;
         setSessionToken();
     }
 
     /**
      * Set the authentication server url in JavaScript memory and and saves it in a cookie.
      * @param url  authentication server url
      */
     protected static void setTokenServerURL(String url)
     {
         RESTILITY.m_tokenServerUrl = url;
         setSessionToken();
     }
 
     /**
      * Set the user name in JavaScript memory and and saves it in a cookie.
      * @param username  user name
      */
     protected static void setUsername(String username)
     {
         RESTILITY.m_username = username;
         setSessionToken();
     }
 
     /**
      * Set the authentication server logout url in JavaScript memory and and saves it in a cookie.
      * @param url  authentication server logout url
      */
     protected static void setTokenServerLogoutURL(String url)
     {
         RESTILITY.m_tokenServerLogoutUrl = url;
         setSessionToken();
     }
 
 
     /**
      * We can't know the best locale until after the user logs in because we need to consider
      * their locale from the identity vault.  So we get the locale as part of the identity
      * information and then store this in a cookie.  If the cookie doesn't match the current
      * locale then we need to refresh the page so we can reload the JavaScript libraries.
      *
      * @param locale the locale
      */
     public static void setBestLocale(String locale)
     {
         if (getBestLocale() != null) {
             if (!getBestLocale().equals(locale)) {
                 /*
                  If the best locale from the server doesn't
                  match the cookie.  That means we set the cookie
                  with the new locale and refresh the page.
                  */
                 RESTILITY.m_bestLocale = locale;
                 setSessionToken();
                 Window.Location.reload();
             } else {
                 /*
                  If the best locale from the server matches
                  the best locale from the cookie then we are
                  done.
                  */
             }
         } else {
             /*
              This means they didn't have a best locale from
              the server stored as a cookie in the client.  So
              we set the locale from the server into the cookie.
              */
             RESTILITY.m_bestLocale = locale;
             setSessionToken();
 
             /*
              * If there are any REST requests in process when we refresh
              * the page they will cause errors that show up before the
              * page reloads.  Hiding the page makes those errors invisible.
              */
             JSUtil.hide("body", "");
             Window.Location.reload();
         }
     }
 
     private static void setSessionToken()
     {
         Cookies.setCookie(SESSION_COOKIE, RESTILITY.m_userToken + "," + RESTILITY.m_tokenServerUrl + "," +
                           RESTILITY.m_tokenServerLogoutUrl + "," + RESTILITY.m_username);
         if (RESTILITY.m_bestLocale != null) {
             Cookies.setCookie(LOCALE_COOKIE, RESTILITY.m_bestLocale);
         }
     }
 
     /**
      * Returns a boolean flag indicating whether user has logged in or not
      *
      * @return boolean indicating whether user has logged in or not
      */
     public static boolean hasUserLoggedIn()
     {
         return RESTILITY.m_hasLoggedIn;
     }
 
     /**
      * Returns user's full authentication token, prefixed with "X-OPAQUE"
      *
      * @return user's full authentication token prefixed with "X-OPAQUE"
      */
     public static String getFullAuthToken()
     {
         return BASIC_AUTH + " " + getUserToken();
     }
 
     /**
      * Returns user's authentication token
      *
      * @return user's authentication token
      */
     public static String getUserToken()
     {
         if (RESTILITY.m_userToken != null) {
             return RESTILITY.m_userToken;
         } else {
             String session = Cookies.getCookie(SESSION_COOKIE);
             if (session == null) {
                 return null;
             } else {
                 return session.split(",")[0];
             }
         }
     }
 
     /**
      * Returns the authentication server url
      *
      * @return authentication server url
      */
     protected static String getTokenServerUrl()
     {
         if (RESTILITY.m_tokenServerUrl != null) {
             return RESTILITY.m_tokenServerUrl;
         } else {
             String session = Cookies.getCookie(SESSION_COOKIE);
             if (session == null) {
                 return null;
             } else {
                 return session.split(",")[1];
             }
         }
     }
 
     /**
      * Returns authentication server logout url
      *
      * @return authentication server logout url
      */
     protected static String getTokenServerLogoutUrl()
     {
         if (RESTILITY.m_tokenServerLogoutUrl != null) {
             return RESTILITY.m_tokenServerLogoutUrl;
         } else {
             String session = Cookies.getCookie(SESSION_COOKIE);
             if (session == null) {
                 return null;
             } else {
                 return session.split(",")[2];
             }
         }
     }
 
     /**
      * Returns the name of the currently logged in user or null
      * if the current user is not logged in.
      * 
      * @return  user name
      */
     public static String getUsername()
     {
         if (RESTILITY.m_username != null) {
             return RESTILITY.m_username;
         } else {
             String session = Cookies.getCookie(SESSION_COOKIE);
             if (session == null) {
                 return null;
             } else {
                 return session.split(",")[3];
             }
         }
     }
 
     /**
      * Returns best matched locale
      *
      * @return best matched locale
      */
     protected static String getBestLocale()
     {
         if (RESTILITY.m_bestLocale != null) {
             return RESTILITY.m_bestLocale;
         } else {
             RESTILITY.m_bestLocale = Cookies.getCookie(LOCALE_COOKIE);
             return RESTILITY.m_bestLocale;
         }
     }
 
     public static ArrayList<RESTLoginCallBack> getLoginListeners()
     {
         return  g_loginListeners;
     }
 
     /**
      * Make an HTTP call and get the results as a JSON object.  This method handles
      * cases like login, error parsing, and configuration requests.
      *
      * @param url      the REST url to call
      * @param data     the data to pass to the URL
      * @param method   the HTTP method, defaults to GET
      * @param callback the callback object for handling the request results
      */
     public static void callREST(String url, String data, RESTility.HTTPMethod method, RESTCallback callback)
     {
         callREST(url, data, method, callback, false, null);
     }
 
     /**
      * Make an HTTP call and get the results as a JSON object.  This method handles
      * cases like login, error parsing, and configuration requests.
      *
      * @param url      the REST url to call
      * @param data     the data to pass to the URL
      * @param method   the HTTP method, defaults to GET
      * @param callback the callback object for handling the request results
      * @param etag     the option etag for this request
      */
     public static void callREST(String url, String data, RESTility.HTTPMethod method,
                                 RESTCallback callback, String etag)
     {
         callREST(url, data, method, callback, false, etag);
     }
 
     private static boolean hasPotentialXss(String data)
     {
         if (data == null) {
             return false;
         }
 
         data = data.toUpperCase();
 
         return (
             data.indexOf("<SCRIPT") > -1 ||
             data.indexOf("DELETE") > -1 ||
             data.indexOf("DROP") > -1 ||
             data.indexOf("UPDATE") > -1 ||
             data.indexOf("INSERT") > -1);
     }
 
 
     /**
      * Make an HTTP call and get the results as a JSON object.  This method handles
      * cases like login, error parsing, and configuration requests.
      *
      * @param url      the REST url to call
      * @param data     the data to pass to the URL
      * @param method   the HTTP method, defaults to GET
      * @param callback the callback object for handling the request results
      * @param isLoginRequest
      *                 true if this is a request to login and false otherwise
      * @param etag     the option etag for this request
      *
      */
     protected static void callREST(String url, String data, RESTility.HTTPMethod method,
                                    RESTCallback callback, boolean isLoginRequest, String etag)
     {
         callREST(url, data, method, callback, isLoginRequest, true, etag);
     }
 
     /**
      * Make an HTTP call and get the results as a JSON object.  This method handles
      * cases like login, error parsing, and configuration requests.
      *
      * @param url      the REST url to call
      * @param data     the data to pass to the URL
      * @param method   the HTTP method, defaults to GET
      * @param callback the callback object for handling the request results
      * @param isLoginRequest
      *                 true if this is a request to login and false otherwise
      * @param shouldReplay true if this request should repeat after a login request
      *                 if this request returns a 401
      * @param etag     the option etag for this request
      */
     public static void callREST(String url, String data, RESTility.HTTPMethod method,
                                  RESTCallback callback, boolean isLoginRequest,
                                  boolean shouldReplay, String etag)
     {
         if (hasPotentialXss(data)) {
             callback.onError(new RESTException(RESTException.XSS_ERROR,
                                                "", STRINGS.noServerContact(),
                                                new HashMap<String, String>(),
                                                -1, url));
             return;
         }
 
         RESTILITY.m_restCalls.put(callback, new RESTCallStruct(url, data, method, shouldReplay, etag));
 
         RequestBuilder builder = new RESTRequestBuilder(method.getMethod(), URL.encode(url));
         /*
          Set our headers
          */
         builder.setHeader("Accept", "application/json");
         builder.setHeader("Accept-Charset", "UTF-8");
 
         if (RESTILITY.m_bestLocale != null) {
             /*
              * The REST end points use the Accept-Language header to determine
              * the locale to use for the contents of the REST request.  Normally
              * the browser will fill this in with the browser locale and that
              * doesn't always match the preferred locale from the Identity Vault
              * so we need to set this value with the correct preferred locale.
              */
             builder.setHeader("Accept-Language", RESTILITY.m_bestLocale);
         }
 
         if (getUserToken() != null &&
             getTokenServerUrl() != null) {
             builder.setHeader("Authorization", getFullAuthToken());
             builder.setHeader("TS-URL", getTokenServerUrl());
         }
 
         if (etag != null) {
             builder.setHeader("If-Match", etag);
         }
 
         if (data != null) {
             /*
              Set our request data
              */
             builder.setRequestData(data);
 
             //b/c jaxb/jersey chokes when there is no data when content-type is json
             builder.setHeader("Content-Type", "application/json");
         }
 
         builder.setCallback(RESTILITY.new RESTRequestCallback(callback));
 
         try {
             /*
              If we are in the process of logging in then all other
              requests will just return with a 401 until the login
              is finished.  We want to delay those requests until
              the login is complete when we will replay all of them.
              */
             if (isLoginRequest || !g_inLoginProcess) {
                 builder.send();
             }
         } catch (RequestException e) {
             MessageUtil.showFatalError(e.getMessage());
         }
 
     }
 
     /**
      * The RESTCallBack object that implements the RequestCallback interface
      */
     private class RESTRequestCallback implements RequestCallback
     {
         private RESTCallback m_origCallback;
 
         public RESTRequestCallback(RESTCallback callback)
         {
             m_origCallback = callback;
         }
 
         public void onResponseReceived(Request request, Response response)
         {
             if (response.getStatusCode() == 0) {
                 /*
                  This means we couldn't contact the server.  It might be that the
                  server is down or that we have a network timeout
                  */
                 RESTCallStruct struct = RESTILITY.m_restCalls.remove(m_origCallback);
                 m_origCallback.onError(new RESTException(RESTException.NO_SERVER_RESPONSE,
                                                          "", STRINGS.noServerContact(),
                                                          new HashMap<String, String>(),
                                                          response.getStatusCode(),
                                                          struct.getUrl()));
                 return;
             }
 
             if (!checkJSON(response.getText())) {
                 RESTCallStruct struct = RESTILITY.m_restCalls.remove(m_origCallback);
                 m_origCallback.onError(new RESTException(RESTException.UNPARSABLE_RESPONSE,
                                                          "", "", new HashMap<String, String>(),
                                                          response.getStatusCode(),
                                                          struct.getUrl()));
                 return;
             }
 
             JSONValue val = null;
             RESTException exception = null;
 
 
             if (response.getText() != null &&
                 response.getText().trim().length() > 1) {
                 try {
                     val = JSONParser.parseStrict(response.getText());
                 } catch (JavaScriptException e) {
                     /*
                      This means we couldn't parse the response this is unlikely
                      because we have already checked it, but it is possible.
                      */
                     RESTCallStruct struct = RESTILITY.m_restCalls.get(m_origCallback);
                     exception = new RESTException(RESTException.UNPARSABLE_RESPONSE,
                                                   "", "", new HashMap<String, String>(),
                                                   response.getStatusCode(),
                                                   struct.getUrl());
                 }
 
                 if (val.isObject() != null &&
                     val.isObject().containsKey("Fault")) {
 
                     JSONObject fault = val.isObject().get("Fault").isObject();
                     String code = fault.get("Code").isObject().get("Value").isString().stringValue();
                     String subcode = null;
                     if (fault.get("Code").isObject().get("Subcode") != null) {
                         subcode = fault.get("Code").isObject().get("Subcode").isObject().get("Value").isString().stringValue();
                     }
                     String reason = null;
                     if (fault.get("Reason") != null && fault.get("Reason").isObject() != null &&
                         fault.get("Reason").isObject().get("Text") != null) {
                         reason = fault.get("Reason").isObject().get("Text").isString().stringValue();
                     }
                     HashMap<String, String> detailMap = new HashMap<String, String>();
                     if (fault.get("Detail") != null) {
                         JSONObject details = fault.get("Detail").isObject();
                         for (String key : details.keySet()) {
                             detailMap.put(key, details.get(key).isString().stringValue());
                         }
                     }
                     if (RESTException.AUTH_SERVER_UNAVAILABLE.equals(subcode)) {
                         /*
                          * This is a special case where the server can't connect to the
                          * authentication server to validate the token.
                          */
                         MessageUtil.showFatalError(STRINGS.unabledAuthServer());
                     }
 
                     RESTCallStruct struct = RESTILITY.m_restCalls.get(m_origCallback);
                     exception = new RESTException(code, subcode, reason,
                                                   detailMap, response.getStatusCode(),
                                                   struct.getUrl());
                 }
             }
 
             RESTCallStruct struct = RESTILITY.m_restCalls.get(m_origCallback);
 
             if (response.getStatusCode() == Response.SC_UNAUTHORIZED) {
                 /*
                  * For return values of 401 we need to show the login dialog
                  */
                 try {
                     for (RESTLoginCallBack listener : g_loginListeners) {
                         listener.loginPrompt();
                     }
 
                     String code = null;
                     if (exception != null) {
                         code = exception.getSubcode();
                     }
 
                     doLogin(m_origCallback, response, struct.getUrl(), code);
                 } catch (RESTException e) {
                     RESTILITY.m_restCalls.remove(m_origCallback);
                     m_origCallback.onError(e);
                 }
                 return;
             }
 
             RESTILITY.m_restCalls.remove(m_origCallback);
 
             if (exception != null) {
                 m_origCallback.onError(exception);
             } else {
                 RESTILITY.m_callCount++;
                 /*
                  * You have to have at least three valid REST calls before
                  * we show the application UI.  This covers the build info
                  * and the successful login with and invalid token.  It
                  * would be really nice if we didn't have to worry about
                  * this at this level, but then the UI will flash sometimes
                  * before the user has logged in.  Hackito Ergo Sum.
                  */
                 if (RESTILITY.m_callCount > 2) {
                     RESTILITY.m_hasLoggedIn = true;
                     if (!RESTILITY.m_logInListenerCalled) {
                         for (RESTLoginCallBack listener : g_loginListeners) {
                             listener.onLoginSuccess();
                         }
                         RESTILITY.m_logInListenerCalled = true;
                     }
                 }
 
                 if (response.getHeader("ETag") != null &&
                     m_origCallback instanceof ConcurrentRESTCallback) {
                     ((ConcurrentRESTCallback) m_origCallback).setETag(response.getHeader("ETag"));
                 }
 
                 m_origCallback.onSuccess(val);
             }
         }
 
 
         public void onError(Request request, Throwable exception)
         {
             MessageUtil.showFatalError(exception.getMessage());
         }
     }
 
     /**
      * The GWT parser calls the JavaScript eval function.  This is dangerous since it
      * can execute arbitrary JavaScript.  This method does a simple check to make sure
      * the JSON data we get back from the server is safe to parse.
      *
      * This parsing scheme is taken from RFC 4627 - http://www.ietf.org/rfc/rfc4627.txt
      *
      * @param json   the JSON string to test
      *
      * @return true if it is safe to parse and false otherwise
      */
     private static native boolean checkJSON(String json)  /*-{
         return !(/[^,:{}\[\]0-9.\-+Eaeflnr-u \n\r\t]/.test(
              json.replace(/"(\\.|[^"\\])*"/g, '')));
     }-*/;
 
     /**
      * Add login listeners
      *
      * @param callback listeners to be added
      */
     public static void addLoginListener(RESTLoginCallBack callback)
     {
         RESTility.g_loginListeners.add(callback);
     }
 
     /**
      * Remove login listeners
      *
      * @param callback listeners to be removed
      */
     public static void removeLoginListener(RESTLoginCallBack callback)
     {
         RESTility.g_loginListeners.remove(callback);
     }
 }
 
 /**
  * A struct for holding data about a REST request
  */
 class RESTCallStruct
 {
     private String m_url;
     private String m_data;
     private RESTility.HTTPMethod m_method;
     private boolean m_shouldReplay;
     private String m_etag;
 
     /**
      * Creates a new RESTCallStruct
      *
      * @param url    the URL for this REST call
      * @param data   the data for this REST call
      * @param method the method for this REST call
      * @param shouldReplay should this request be repeated if we get a 401
      */
     protected RESTCallStruct(String url, String data, RESTility.HTTPMethod method,
                              boolean shouldReplay, String etag)
     {
         m_url = url;
         m_data = data;
         m_method = method;
         m_shouldReplay = shouldReplay;
         m_etag = etag;
     }
 
     /**
      * Gets the URL
      *
      * @return the URL
      */
     public String getUrl()
     {
         return m_url;
     }
 
     /**
      * Gets the data
      *
      * @return the data
      */
     public String getData()
     {
          return m_data;
     }
 
     /**
      * Gets the HTTP method
      *
      * @return the method
      */
     public RESTility.HTTPMethod getMethod()
     {
         return m_method;
     }
 
     /**
      * Gets the ETag for this call
      *
      * @return the ETag
      */
     public String getETag()
     {
         return m_etag;
     }
 
     /**
      * Should this request repeat
      *
      * @return true if it should repeat, false otherwise
      */
     public boolean shouldReplay()
     {
         return m_shouldReplay;
     }
 }
 
 /**
  * This class extends RequestBuilder so we can call PUT and DELETE
  */
 class RESTRequestBuilder extends RequestBuilder
 {
     /**
      * Creates a new RESTRequestBuilder
      *
      * @param method the HTTP method
      * @param url    the request URL
      */
     public RESTRequestBuilder(String method, String url)
     {
         super(method, url);
     }
 }
 
