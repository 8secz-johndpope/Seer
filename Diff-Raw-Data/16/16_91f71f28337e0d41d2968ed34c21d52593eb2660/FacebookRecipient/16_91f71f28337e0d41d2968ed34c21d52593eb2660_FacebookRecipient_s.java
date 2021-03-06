 package org.soluvas.web.login.facebook;
 
 import java.io.IOException;
 import java.net.URISyntaxException;
 import java.util.ArrayList;
 import java.util.List;
 import java.util.Scanner;
 import java.util.Set;
 
 import javax.annotation.Nullable;
 
 import org.apache.http.HttpResponse;
 import org.apache.http.NameValuePair;
 import org.apache.http.client.methods.HttpGet;
 import org.apache.http.client.utils.URIBuilder;
 import org.apache.http.client.utils.URLEncodedUtils;
 import org.apache.http.impl.client.DefaultHttpClient;
 import org.apache.shiro.SecurityUtils;
 import org.apache.shiro.authc.AuthenticationException;
 import org.apache.shiro.authc.AuthenticationToken;
 import org.apache.shiro.subject.Subject;
 import org.apache.wicket.Application;
 import org.apache.wicket.Page;
 import org.apache.wicket.RestartResponseException;
 import org.apache.wicket.markup.html.WebPage;
 import org.apache.wicket.markup.html.basic.Label;
 import org.apache.wicket.request.flow.RedirectToUrlException;
 import org.apache.wicket.request.mapper.parameter.PageParameters;
 import org.apache.wicket.spring.injection.annot.SpringBean;
 import org.ops4j.pax.wicket.api.PaxWicketBean;
 import org.ops4j.pax.wicket.api.PaxWicketMountPoint;
 import org.slf4j.Logger;
 import org.slf4j.LoggerFactory;
 import org.soluvas.commons.SlugUtils;
 import org.soluvas.commons.WebAddress;
 import org.soluvas.facebook.FacebookUtilsImpl;
 import org.soluvas.image.store.ImageRepository;
 import org.soluvas.json.JsonUtils;
 import org.soluvas.ldap.LdapRepository;
 import org.soluvas.ldap.SocialPerson;
 import org.soluvas.security.AutologinToken;
 import org.soluvas.security.NotLoggedWithFacebookException;
 import org.soluvas.web.login.FacebookManager;
 import org.soluvas.web.login.LoginException;
 import org.soluvas.web.site.SoluvasWebSession;
 
 import com.google.common.base.Preconditions;
 import com.google.common.base.Predicate;
 import com.google.common.base.Strings;
 import com.google.common.collect.Sets;
 import com.restfb.DefaultFacebookClient;
 import com.restfb.FacebookClient;
 import com.restfb.types.User;
 
 /**
  * Get Facebook Login , token Access, FB Photo profile
  * @author haidar
  *
  */
 @SuppressWarnings("serial")
 @PaxWicketMountPoint(mountPoint="fb_recipient/")
 public class FacebookRecipient extends WebPage {
 
 
 	private static final Logger log = LoggerFactory.getLogger(FacebookRecipient.class);
 	
 	@PaxWicketBean(name="personLdapRepo") @SpringBean(name="personLdapRepo")
 	private LdapRepository<SocialPerson> personLdapRepo;
 	@PaxWicketBean(name="personImageRepo") @SpringBean(name="personImageRepo")
 	private ImageRepository personImageRepo;
 	@PaxWicketBean(name="facebookMgr") @SpringBean(name="facebookMgr")
 	private FacebookManager facebookMgr;
 	@PaxWicketBean(name="webAddress") @SpringBean(name="webAddress")
 	private WebAddress webAddress;
 	
 	public FacebookRecipient(PageParameters params) {
 		super();
 		try {
 			final String code = params.get("code").toOptionalString();
 			if (Strings.isNullOrEmpty(code)) {
 				throw new NotLoggedWithFacebookException();
 			}
 	//		Preconditions.checkArgument(!Strings.isNullOrEmpty(code), "Code Parameter must be provided");
 			final String token_url = "https://graph.facebook.com/oauth/access_token?";
 			final String myUrl = webAddress.getBaseUri() + "fb_recipient/";
 			final String appId = facebookMgr.getAppId();
 			final String appSecret = facebookMgr.getAppSecret();
 	
 			log.debug("Code parameter url is {}", code);
 			add(new Label("code" ,code));
 			URIBuilder fbLoginUri = new URIBuilder(token_url);
 			fbLoginUri.addParameter("client_id", appId);
 			fbLoginUri.addParameter("client_secret", appSecret);
 			fbLoginUri.addParameter("redirect_uri", myUrl);
 			fbLoginUri.addParameter("code", code);
 			
 			log.info("Getting access token");
 			final String accessToken = fetchAccessToken(fbLoginUri);
 			log.debug("fetching access token {}", accessToken);
 			final FacebookClient facebookClient = new DefaultFacebookClient(accessToken);
 			final User user = facebookClient.fetchObject("me", User.class);
 			Preconditions.checkNotNull("User should not be null", user);
 			log.debug("Got user and user details{}", JsonUtils.asJson(user));
 			
 			SocialPerson person = personLdapRepo.findOneByAttribute("fbId", user.getId());
 			if (person == null) {
 				person = personLdapRepo.findOneByAttribute("fbUser", user.getUsername());
 			}
 			if (person == null) {
 				person = personLdapRepo.findOneByAttribute("mail", user.getEmail());
 			}
 			
 			final SocialPerson modifiedPerson;
 			if (person != null) {
 				// Direct Login
 				log.debug("person is exist, update the Access Token");
 				final Set<String> emails = Sets.newHashSet();
 				if (person.getEmails() != null) {
 					emails.addAll(person.getEmails());
 				}
 				if (!Strings.isNullOrEmpty(user.getEmail())) {
 					emails.add(user.getEmail());
 				}
 				person.setEmails(emails);
 				person.setFacebookUsername(user.getUsername());
 				person.setFacebookId(Long.valueOf(user.getId()));
 				person.setFacebookAccessToken(accessToken);
 				modifiedPerson = personLdapRepo.modify(person);
 			} else {
 				Preconditions.checkNotNull(user.getName(), "Facebook User's Name cannot be empty");
 				final String personId = SlugUtils.generateValidId(user.getName(), new Predicate<String>() {
 					@Override
 					public boolean apply(@Nullable String input) {
 						return !personLdapRepo.exists(input);
 					}
 				});
 				
 				final String personSlug = SlugUtils.generateValidScreenName(user.getName(), new Predicate<String>() {
 					@Override
 					public boolean apply(@Nullable String input) {
 						return !personLdapRepo.existsByAttribute("uniqueIdentifier", input);
 					}
 				});
 				//https://graph.facebook.com/USER_ID/picture?type=large HTTPCLIENT
 				final SocialPerson newPerson = new SocialPerson(personId, personSlug, user.getFirstName(), user.getLastName());
 				log.debug("User's email is {}", user.getEmail());
 				newPerson.setPrimaryEmail(user.getEmail());
 				newPerson.setFacebookUsername(user.getUsername());
 				newPerson.setFacebookId(Long.valueOf(user.getId()));
 				newPerson.setFacebookAccessToken(accessToken);
 				
 				//Set photo from Facebook.
 				try {
 					final String imageId = FacebookUtilsImpl.refreshPhotoFromFacebook(newPerson.getFacebookId(), newPerson.getName(), personImageRepo);
 					newPerson.setPhotoId(imageId);
 				} catch (Exception e) {
 					log.error("Cannot refresh photo from Facebook for person " + newPerson.getId() + " " + newPerson.getName(), e);
 				}
 				modifiedPerson = personLdapRepo.add(newPerson);
 
 			}
 			
 			// Set Token And Set Session
 			final AuthenticationToken token = new AutologinToken(
 					Strings.nullToEmpty(modifiedPerson.getId()));
 			log.debug("Logging in using AutologinToken {}", token.getPrincipal() );
 			try {
 				final Subject currentUser = SecurityUtils.getSubject();
 				currentUser.login(token);
 				final String personId = Preconditions.checkNotNull(modifiedPerson.getId(),
 						"Cannot get current user as person ID");
				info(String.format("Your are now logged in as %s", personId));
 				log.debug("Current user is now {}", personId);
 				log.debug("{} permitted to edit person? {}", personId, currentUser.isPermitted("person:edit:*"));
 //				onLoginSuccess(personId);
 			} catch (final AuthenticationException e) {
 				error(String.format("Invalid credentials for %s", token.getPrincipal()));
 				log.debug(String.format("Invalid credentials for %s", token.getPrincipal()), e);
 			}
 			
 			// Redirect Url
 			final SoluvasWebSession solWebSession = (SoluvasWebSession) getSession();
 			if (solWebSession.getOriginalUrl() != null) {
 				final String destUri = webAddress.getBaseUri() + solWebSession.getOriginalUrl().toString();
 				solWebSession.setOriginalUrl(null);
 				log.debug("Session has originalUrl, redirecting to {}", destUri);
 				throw new RedirectToUrlException(destUri);
 			} else {
 				// If Regular User
 				final Class<? extends Page> homePage = Application.get().getHomePage();
 //				RedirectByUserTyperType;
 				
 				
 				log.debug("Session has no, redirecting to {}", homePage.getName()); 
 				throw new RestartResponseException(homePage);
 			}
 		} catch (final URISyntaxException e) {
 			throw new NotLoggedWithFacebookException();
 		}
 	}
 
 	public String fetchAccessToken(URIBuilder accessTokenUri) {
 		try {
 			final HttpGet accessTokenUriRequest = new HttpGet(accessTokenUri.build());
 			final DefaultHttpClient client = new DefaultHttpClient();
 			final HttpResponse responseAccessTokenReq = client.execute(accessTokenUriRequest);
 			if (responseAccessTokenReq.getStatusLine().getStatusCode() != 200)
 				throw new IOException(String.format(
 						"GET %s throws HTTP Error %d: %s", accessTokenUriRequest, responseAccessTokenReq
 								.getStatusLine().getStatusCode(), responseAccessTokenReq
 								.getStatusLine().getReasonPhrase()));
 			final Scanner scanner = new Scanner(responseAccessTokenReq.getEntity().getContent());
 			final List<NameValuePair> data = new ArrayList<>();
 			URLEncodedUtils.parse(data, scanner, "UTF-8");
 			
 			final String accessToken = data.get(0).getValue();
 			return accessToken;
 			//throw new RedirectToUrlException(myUrl.toString());
 		} catch (final Exception ex) {
 			throw new LoginException("Error when building Facebook URI", ex);
 		}
 	}
 	
 //	protected void onLoginSuccess(String personId) {
 //	}
 }
