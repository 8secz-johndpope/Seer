 package com.lookfirst.wepay;
 
 import java.io.IOException;
 import java.io.InputStream;
 import java.io.OutputStreamWriter;
 import java.io.UnsupportedEncodingException;
 import java.lang.reflect.ParameterizedType;
 import java.net.HttpURLConnection;
 import java.net.URL;
 import java.net.URLEncoder;
 import java.util.Arrays;
 import java.util.List;
 
 import lombok.Getter;
 import lombok.NoArgsConstructor;
 import lombok.Setter;
 import lombok.extern.slf4j.Slf4j;
 
 import org.apache.commons.io.IOUtils;
 import org.apache.commons.lang3.StringUtils;
 import org.codehaus.jackson.JsonNode;
 import org.codehaus.jackson.map.DeserializationConfig;
 import org.codehaus.jackson.map.DeserializationConfig.Feature;
 import org.codehaus.jackson.map.ObjectMapper;
 import org.codehaus.jackson.map.PropertyNamingStrategy;
 import org.codehaus.jackson.map.SerializationConfig;
 import org.codehaus.jackson.type.JavaType;
 
 import com.lookfirst.wepay.api.Token;
 import com.lookfirst.wepay.api.WePayException;
 import com.lookfirst.wepay.api.req.TokenRequest;
 import com.lookfirst.wepay.api.req.WePayRequest;
 
 /**
  * Implements a way to communicate with the WePayApi.
  *
  * https://www.wepay.com/developer/reference
  *
  * @author Jon Scott Stevens
  * @author Jeff Schnitzer
  */
 @Slf4j
 @NoArgsConstructor
@SuppressWarnings("deprecation")
 public class WePayApi {
 
 	/**
 	 * Scope fields
 	 * Passed into Wepay::getAuthorizationUri as array
 	 */
 	public enum Scope {
 		SCOPE_MANAGE_ACCOUNTS ("manage_accounts"),   // Open and interact with accounts
 		SCOPE_VIEW_BALANCE    ("view_balance"),      // View account balances
 		SCOPE_COLLECT_PAYMENTS("collect_payments"),  // Create and interact with checkouts
 		SCOPE_REFUND_PAYMENTS ("refund_payments"),   // Refund checkouts
 		SCOPE_VIEW_USER       ("view_user");         // Get details about authenticated user
 
 		private String scope;
 		
 		private Scope(String scope) {
 			this.scope = scope;
 		}
 
 		public String getScope() {
 			return scope;
 		}
 
 		public static List<Scope> getAll() {
 			return Arrays.asList(values());
 		}
 
 		@Override
 		public String toString() {
 			return this.scope;
 		}
 
 	}
 
 	/** */
 	private static final ObjectMapper MAPPER = new ObjectMapper();
 	static {
 		// For the UserDetails bean (an others), we send an empty bean.
 		MAPPER.disable(SerializationConfig.Feature.FAIL_ON_EMPTY_BEANS);
 		// Makes for nice java property/method names
 		MAPPER.setPropertyNamingStrategy(PropertyNamingStrategy.CAMEL_CASE_TO_LOWER_CASE_WITH_UNDERSCORES);
 		// If wepay adds properties, we shouldn't blow up
 		MAPPER.configure(Feature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		// I cann't figure out what the nondeprecated solution to this is
		MAPPER.configure(SerializationConfig.Feature.WRITE_NULL_PROPERTIES, false);
 		
 		// This saves us the mess of enums that conflict with java keywords (eg Checkout.State.new_)
 		MAPPER.configure(SerializationConfig.Feature.WRITE_ENUMS_USING_TO_STRING, true);
 		MAPPER.configure(DeserializationConfig.Feature.READ_ENUMS_USING_TO_STRING, true);
 	}
 
 	/** */
 	private static final String STAGING_URL = "https://stage.wepay.com/v2";
 	private static final String PROD_URL = "https://wepay.com/v2";
 
 	/** */
 	private String currentUrl;
 
 	/** */
 	@Getter @Setter
 	private WePayKey key;
 
 	/** */
 	public WePayApi(WePayKey key) {
 		this.key = key;
 		this.currentUrl = key.isProduction() ? PROD_URL : STAGING_URL;
 	}
 
 	/**
 	 * Generate URI used during oAuth authorization
 	 * Redirect your user to this URI where they can grant your application
 	 * permission to make API calls
 	 * @see <a href="https://www.wepay.com/developer/reference/oauth2">https://www.wepay.com/developer/reference/oauth2</a>
 	 * @param scopes             List of scope fields for which your application wants access
 	 * @param redirectUri      Where user goes after logging in at WePay (domain must match application settings)
 	 * @return string URI to which you must redirect your user to grant access to your application
 	 */
 	public String getAuthorizationUri(List<Scope> scopes, String redirectUri, String state) {
 		return getAuthorizationUri(scopes, redirectUri, state, null, null);
 	}
 
 	/**
 	 * Generate URI used during oAuth authorization
 	 * Redirect your user to this URI where they can grant your application
 	 * permission to make API calls
 	 * @see <a href="https://www.wepay.com/developer/reference/oauth2">https://www.wepay.com/developer/reference/oauth2</a>
 	 * @param scopes             List of scope fields for which your application wants access
 	 * @param redirectUri      Where user goes after logging in at WePay (domain must match application settings)
 	 * @param userName  user_name,user_email which will be pre-filled on login form, state to be returned in querystring of redirect_uri
 	 * @return string URI to which you must redirect your user to grant access to your application
 	 */
 	public String getAuthorizationUri(List<Scope> scopes, String redirectUri, String state, String userName, String userEmail) {
 		// this method must use www instead of just naked domain for security reasons.
 		String host = key.isProduction() ? "https://www.wepay.com" : "https://stage.wepay.com";
 		String uri = host + "/v2/oauth2/authorize?";
 
 		uri += "client_id=" +  urlEncode(key.getClientId()) + "&";
 		uri += "redirect_uri=" +  urlEncode(redirectUri) + "&";
 		uri += "scope=" + urlEncode(StringUtils.join(scopes, ","));
 		if (state != null || userName != null || userEmail != null)
 			uri += "&";
 		uri += state != null ? "state=" +  urlEncode(state) + "&" : "";
 		uri += userName != null ? "user_name=" +  urlEncode(userName) + "&" : "";
 		uri += userEmail != null ? "user_email=" +  urlEncode(userEmail) : "";
 
 		return uri;
 	}
 
 	/**
 	 * Exchange a temporary access code for a (semi-)permanent access token
 	 * @param code          'code' field from query string passed to your redirect_uri page
 	 * @param redirectUrl  Where user went after logging in at WePay (must match value from getAuthorizationUri)
 	 * @return json {"user_id":"123456","access_token":"1337h4x0rzabcd12345","token_type":"BEARER"}
 	 */
 	public Token getToken(String code, String redirectUrl) throws WePayException {
 
 		TokenRequest request = new TokenRequest();
 		request.setClientId(key.getClientId());
 		request.setClientSecret(key.getClientSecret());
 		request.setRedirectUri(redirectUrl);
 		request.setCode(code);
 
 		return execute(null, request);
 	}
 
 	/**
 	 * Make API calls against authenticated user.
 	 * Turn up logging to trace level to see the request / response.
 	 */
 	public <T> T execute(String token, WePayRequest<T> req) throws WePayException {
 
 		String uri = currentUrl + req.getEndpoint();
 
 		JsonNode resp = null;
 
 		try {
 			String post = MAPPER.writeValueAsString(req);
 
 			if (log.isTraceEnabled()) {
 				log.trace("request to {}:  {}", uri, post);
 			}
 
 			HttpURLConnection conn = getConnection(uri, post, token);
 			InputStream is = conn.getInputStream();
 
 			if (log.isTraceEnabled()) {
 				String results = IOUtils.toString(is);
 				log.trace("response: " + results);
 				resp = MAPPER.readTree(results);
 			} else {
 				resp = MAPPER.readTree(is);
 			}
 
 			// if there is an error in the response from wepay, it'll get thrown in this call.
 			this.checkForError(resp);
 
 			// This is a little bit of black magic with jackson.  We know that any request passed extends
 			// the abstract WePayRequest and de-genericizes it.  This means the concrete class has full
 			// generic type information, and we can use this to determine what type to deserialize.  The
 			// trickiest case is WePayAccountFindRequest, whose response type is List<AccountWithUri>.
 			ParameterizedType paramType = (ParameterizedType)req.getClass().getGenericSuperclass();
 			JavaType type = MAPPER.constructType(paramType.getActualTypeArguments()[0]);
 
 			return MAPPER.readValue(resp, type);
 
 		} catch (IOException e) {
 			throw new WePayException(e.getMessage(), e);
 		}
 	}
 
 	/**
 	 * If the response node is recognized as an error, throw a WePayException
 	 * @throws WePayException if the node is an error node
 	 */
 	private void checkForError(JsonNode resp) throws WePayException
 	{
 		if (resp.get("error") != null)
 			throw new WePayException(resp.path("error").asText() + " : " + resp.path("error_description").asText());
 	}
 
 	/**
 	 * Common functionality for posting data.
 	 *
 	 * WePay's API is not strictly RESTful, so all requests are sent as POST unless there are no request values
 	 */
 	private HttpURLConnection getConnection(String uri, String postJson, String token) throws IOException {
 
 		URL url = new URL(uri);
 		HttpURLConnection conn = (HttpURLConnection) url.openConnection();
 
 		if (postJson != null && postJson.equals("{}"))
 			postJson = null;
 
 		if (postJson != null) {
 			conn.setDoOutput(true); // Triggers POST.
 		}
		conn.setRequestProperty("Content-Type", "application/json; charset=utf-8");
 
 		if (token != null) {
 			conn.setRequestProperty("Authorization", "Bearer " + token);
 		}
 
 		if (postJson != null) {
			OutputStreamWriter writer = new OutputStreamWriter(conn.getOutputStream(), "UTF-8");
 			writer.write(postJson);
 			writer.close();
 		}
 		return conn;
 	}
 
 	/**
 	 * An interface to URLEncoder.encode() that isn't inane
 	 */
 	private String urlEncode(Object value)
 	{
 		try {
 			return URLEncoder.encode(value.toString(), "utf-8");
 		} catch (UnsupportedEncodingException e) { throw new RuntimeException(e); }
 	}
 }
