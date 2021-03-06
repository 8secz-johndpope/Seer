 /*
  * Copyright 2010 the original author or authors.
  *
  * Licensed under the Apache License, Version 2.0 (the "License");
  * you may not use this file except in compliance with the License.
  * You may obtain a copy of the License at
  *
  *      http://www.apache.org/licenses/LICENSE-2.0
  *
  * Unless required by applicable law or agreed to in writing, software
  * distributed under the License is distributed on an "AS IS" BASIS,
  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  * See the License for the specific language governing permissions and
  * limitations under the License.
  */
 package org.springframework.social.connect;
 
 import java.io.Serializable;
 import java.util.Collection;
 import java.util.HashMap;
 import java.util.List;
 import java.util.Map;
 
 import org.scribe.extractors.BaseStringExtractorImpl;
 import org.scribe.extractors.HeaderExtractorImpl;
 import org.scribe.extractors.TokenExtractorImpl;
 import org.scribe.model.OAuthConfig;
 import org.scribe.model.Token;
 import org.scribe.model.Verb;
 import org.scribe.model.Verifier;
 import org.scribe.oauth.OAuth10aServiceImpl;
 import org.scribe.oauth.OAuthService;
 import org.scribe.services.HMACSha1SignatureService;
 import org.scribe.services.TimestampServiceImpl;
 import org.springframework.transaction.annotation.Transactional;
 import org.springframework.web.client.RestTemplate;
 
 /**
  * General-purpose base class for ServiceProvider implementations.
  * @author Keith Donald
  * @param <S> The service API hosted by this service provider.
  */
 public abstract class AbstractServiceProvider<S> implements ServiceProvider<S> {
 	
 	protected final ServiceProviderParameters parameters;
 
 	private final AccountConnectionRepository connectionRepository;
 	
 	/**
 	 * Creates a ServiceProvider.
 	 * @param parameters the parameters needed to implement the behavior in this class
 	 * @param connectionRepository a data access interface for managing account connection records
 	 */
 	public AbstractServiceProvider(ServiceProviderParameters parameters,
 			AccountConnectionRepository connectionRepository) {
 		this.parameters = parameters;
 		this.connectionRepository = connectionRepository;
 	}
 
 	// provider meta-data
 	
 	public String getName() {
 		return parameters.getName();
 	}
 	
 	public String getDisplayName() {
 		return parameters.getDisplayName();
 	}
 	
 	public String getApiKey() {
 		return parameters.getApiKey();
 	}
 
 	public Long getAppId() {
 		return parameters.getAppId();
 	}
 	
 	// connection management
 	
 	public OAuthToken fetchNewRequestToken(String callbackUrl) {
 		if (getOAuthVersion() == OAuthVersion.OAUTH_2) {
 			throw new IllegalStateException("You may not fetch a request token for an OAuth 2-based service provider");
 		}
 
 		Token requestToken = getOAuthService(callbackUrl).getRequestToken();
 		return new OAuthToken(requestToken.getToken(), requestToken.getSecret());
 	}
 
 	public String buildAuthorizeUrl(String requestToken) {
 		if (getOAuthVersion() == OAuthVersion.OAUTH_1) {
 			return parameters.getAuthorizeUrl().expand(requestToken).toString();
 		}
 
 		return parameters.getAuthorizeUrl().expand(parameters.getApiKey(), requestToken).toString();
 	}
 
 	public void connect(Serializable accountId, AuthorizedRequestToken requestToken) {
 		OAuthToken accessToken = getAccessToken(requestToken);
 		S serviceOperations = createServiceOperations(accessToken);
 		String providerAccountId = fetchProviderAccountId(serviceOperations);
 		connectionRepository.addConnection(accountId, getName(), accessToken,
 				providerAccountId, buildProviderProfileUrl(providerAccountId, serviceOperations));
 	}
 
 	public void connect(Serializable accountId, String redirectUri, String code) {
 		RestTemplate rest = new RestTemplate();
 		Map<String, String> request = new HashMap<String, String>();
 		request.put("client_id", parameters.getApiKey());
 		request.put("client_secret", parameters.getSecret());
 		request.put("code", code);
 		request.put("redirect_uri", redirectUri);
 
 		String accessToken = null;
		// TODO: Consider pushing this special case down into FacebookServiceProvider
 		if (getOAuthVersion() == OAuthVersion.FB_OAUTH_2) {
 			accessToken = getAccessTokenForFacebook(rest, request, accessToken);
 		} else {
 			@SuppressWarnings("unchecked")
 			Map<String, String> result = rest.postForObject(parameters.getAccessTokenUrl(), request, Map.class);
 			accessToken = result.get("access_token");
 		}
 
 		OAuthToken oauthAccessToken = new OAuthToken(accessToken);
 		S serviceOperations = createServiceOperations(oauthAccessToken);
 		String username = fetchProviderAccountId(serviceOperations);
 
 		connectionRepository.addConnection(accountId, getName(), oauthAccessToken, username,
 				buildProviderProfileUrl(username, serviceOperations));
 	}
 
 	public void addConnection(Serializable accountId, String accessToken, String providerAccountId) {
 		OAuthToken oauthAccessToken = new OAuthToken(accessToken);
 		S serviceOperations = createServiceOperations(oauthAccessToken);
 		connectionRepository.addConnection(accountId, getName(), oauthAccessToken,
 				providerAccountId,
 				buildProviderProfileUrl(providerAccountId, serviceOperations));
 	}
 
 	public boolean isConnected(Serializable accountId) {
 		return connectionRepository.isConnected(accountId, getName());
 	}
 
 	public void disconnect(Serializable accountId) {
 		connectionRepository.disconnect(accountId, getName());
 	}
 	
 	public void disconnect(Serializable accountId, String providerAccountId) {
 		connectionRepository.disconnect(accountId, getName(), providerAccountId);
 	}
 
 	@Transactional
 	public S getServiceOperations(Serializable accountId) {
 		if (accountId == null || !isConnected(accountId)) {
 			return createServiceOperations(null);
 		}
 		OAuthToken accessToken = connectionRepository.getAccessToken(accountId, getName());
 		return createServiceOperations(accessToken);
 	}
 
 	public S getServiceOperations(OAuthToken accessToken) {
 		return createServiceOperations(accessToken);
 	}
 
 	public S getServiceOperations(Serializable accountId, String providerAccountId) {
 		OAuthToken accessToken = connectionRepository.getAccessToken(accountId, getName(), providerAccountId);
 		return createServiceOperations(accessToken);
 	}
 
 	public Collection<AccountConnection> getConnections(Serializable accountId) {
 		return connectionRepository.getAccountConnections(accountId, getName());
 	}
 
 	public OAuthVersion getOAuthVersion() {
 		return parameters.getRequestTokenUrl() == null ? OAuthVersion.OAUTH_2 : OAuthVersion.OAUTH_1;
 	}
 
 	// additional finders
 	
 	public String getProviderAccountId(Serializable accountId) {
 		return connectionRepository.getProviderAccountId(accountId, getName());
 	}
 
 	// subclassing hooks
 	
 	/**
 	 * Construct the strongly-typed service API template that callers may use to invoke the service offered by this service provider.
 	 * Subclasses should override to return their concrete service implementation.
 	 * @param accessToken the granted access token needed to make authorized requests for protected resources
 	 */
 	protected abstract S createServiceOperations(OAuthToken accessToken);
 
 	/**
 	 * Use the service API to fetch the id the member has been assigned in the provider's system.
 	 * This id is stored locally to support linking to the user's connected profile page.
 	 * It is also used for finding connected friends, see {@link #findMembersConnectedTo(List)}.
 	 */
 	protected abstract String fetchProviderAccountId(S serviceOperations);
 
 	/**
 	 * Build the URL pointing to the member's public profile on the provider's system.
 	 * @param providerAccountId the id the member is known by in the provider's system.
 	 * @param serviceOperations the service API
 	 */
 	protected abstract String buildProviderProfileUrl(String providerAccountId, S serviceOperations);
 
 	/**
 	 * The {@link #getApiKey() apiKey} secret.
 	 */
 	protected String getSecret() {
 		return parameters.getSecret();
 	}
 	
 
 	// internal helpers
 
 	/*
 	 * This method is necessary because Facebook fails to properly adhere to the
 	 * latest OAuth 2 specification draft (draft 11). In particular Facebook
 	 * differs from the spec in the following 2 ways:
 	 * 
 	 * 1. The OAuth 2 spec says requests for access tokens should be made via
 	 * POST requests. Facebook only supports GET requests.
 	 * 
 	 * 2. The OAuth 2 spec limits the access token response to application/json
 	 * content type. Facebook responds with what appears to be form-encoded data
 	 * but with Content-Type set to "text/plain".
 	 * 
 	 * In addition, the OAuth 2 spec speaks of a refresh token that may be
 	 * returned so that the client can refresh the access token after it
 	 * expires. Facebook does not support refresh tokens.
 	 */
 	private String getAccessTokenForFacebook(RestTemplate rest, Map<String, String> request, String accessToken) {
 		String result = rest.getForObject(parameters.getAccessTokenUrl() + ACCESS_TOKEN_QUERY_PARAMETERS, String.class,
 				request);
 		String[] nameValuePairs = result.split("\\&");
 		for (String nameValuePair : nameValuePairs) {
 			String[] nameAndValue = nameValuePair.split("=");
 			if (nameAndValue[0].equals("access_token")) {
 				accessToken = nameAndValue[1];
 				break;
 			}
 		}
 		return accessToken;
 	}
 	
 	private static final String ACCESS_TOKEN_QUERY_PARAMETERS = "?client_id={client_id}&client_secret={client_secret}&code={code}&redirect_uri={redirect_uri}";
 
 	private OAuthService getOAuthService() {
 		return getOAuthService(null);
 	}
 	
 	private OAuthService getOAuthService(String callbackUrl) {
 		OAuthConfig config = new OAuthConfig();
 		config.setRequestTokenEndpoint(parameters.getRequestTokenUrl());
 		config.setAccessTokenEndpoint(parameters.getAccessTokenUrl());
 		config.setAccessTokenVerb(Verb.POST);
 		config.setRequestTokenVerb(Verb.POST);
 		config.setApiKey(parameters.getApiKey());
 		config.setApiSecret(parameters.getSecret());
 		if (callbackUrl != null) {
 			config.setCallback(callbackUrl);
 		}
 		return new OAuth10aServiceImpl(new HMACSha1SignatureService(), new TimestampServiceImpl(), new BaseStringExtractorImpl(), new HeaderExtractorImpl(), new TokenExtractorImpl(), new TokenExtractorImpl(), config);
 	}
 
 	private OAuthToken getAccessToken(AuthorizedRequestToken requestToken) {
 		Token accessToken = getOAuthService().getAccessToken(new Token(requestToken.getValue(), requestToken.getSecret()), new Verifier(requestToken.getVerifier()));
 		return new OAuthToken(accessToken.getToken(), accessToken.getSecret());
 	}
 
 }
