 package eu.europeana.api2.v2.model.json.view;
 
 import java.io.UnsupportedEncodingException;
 import java.net.URLEncoder;
 import java.util.ArrayList;
 import java.util.Date;
 import java.util.HashMap;
 import java.util.List;
 import java.util.Map;
 
 import org.apache.commons.lang.StringUtils;
 import org.codehaus.jackson.map.annotate.JsonSerialize;
 import org.codehaus.jackson.map.annotate.JsonSerialize.Inclusion;
 
 import eu.europeana.api2.model.enums.Profile;
 import eu.europeana.corelib.definitions.model.ThumbSize;
 import eu.europeana.corelib.definitions.solr.DocType;
 import eu.europeana.corelib.definitions.solr.beans.BriefBean;
 import eu.europeana.corelib.solr.bean.impl.IdBeanImpl;
 
 @JsonSerialize(include = Inclusion.NON_EMPTY)
 public class BriefView extends IdBeanImpl implements BriefBean {
 
 	protected static final String RECORD_PATH = "/v2/record/";
 	protected static final String PORTAL_PATH = "/record/";
 	protected static final String RECORD_EXT = ".json";
 	protected static final String PORTAL_PARAMS = ".html?utm_source=api&utm_medium=api&utm_campaign=";
 	protected static final String WSKEY_PARAM = "?wskey=";
 	protected static final String IMAGE_SITE = "http://europeanastatic.eu/api/image";
 	protected static final String URI_PARAM = "?uri=";
 	protected static final String SIZE_PARAM = "&size=";
 	protected static final String TYPE_PARAM = "&type=";
 
 	protected static String apiUrl;
 	protected static String portalUrl;
 
 	protected String profile;
 	private String[] thumbnails;
 	protected String wskey;
 	protected BriefBean bean;
 
 	private boolean isOptedOut;
 
 	public BriefView(BriefBean bean, String profile, String wskey, boolean optOut) {
 		this.profile = profile;
 		this.wskey = wskey;
 		this.bean = bean;
 		this.isOptedOut = optOut;
 	}
 
 	public String getProfile() {
 		return null; // profile;
 	}
 
 	public void setProfile(String profile) {
 		this.profile = profile;
 	}
 
 	@Override
 	public String[] getTitle() {
 		return bean.getTitle();
 	}
 
 	@Override
 	public String[] getEdmObject() {
 		// return bean.getEdmObject();
 		return null;
 	}
 
 	@Override
 	public String[] getYear() {
 		return bean.getYear();
 	}
 
 	@Override
 	public String[] getProvider() {
 		return bean.getProvider();
 	}
 
 	@Override
 	public String[] getDataProvider() {
 		return bean.getDataProvider();
 	}
 
 	@Override
 	public String[] getLanguage() {
 		if (isProfile(Profile.MINIMAL)) {
 			return null;
 		}
 		return bean.getLanguage();
 	}
 
 	@Override
 	public String[] getRights() {
 		return bean.getRights();
 	}
 
 	@Override
 	public DocType getType() {
 		return bean.getType();
 	}
 
 	@Override
 	public String[] getDctermsSpatial() {
 		if (isProfile(Profile.MINIMAL) || isProfile(Profile.STANDARD)) {
 			return null;
 		}
 		return bean.getDctermsSpatial();
 	}
 
 	@Override
 	public int getEuropeanaCompleteness() {
 		return bean.getEuropeanaCompleteness();
 	}
 
 	@Override
 	public String[] getEdmPlace() {
 		if (isProfile(Profile.MINIMAL) || isProfile(Profile.STANDARD)) {
 			return null;
 		}
 		return bean.getEdmPlace();
 	}
 
 	@Override
 	public List<Map<String, String>> getEdmPlaceLabel() {
 		if (isProfile(Profile.MINIMAL)) {
 			return null;
 		}
 		return bean.getEdmPlaceLabel();
 	}
 
 	@Override
 	public List<String> getEdmPlaceLatitude() {
 		return bean.getEdmPlaceLatitude();
 	}
 
 	@Override
 	public List<String> getEdmPlaceLongitude() {
 		return bean.getEdmPlaceLongitude();
 	}
 
 	@Override
 	public String[] getEdmTimespan() {
 		if (isProfile(Profile.MINIMAL) || isProfile(Profile.STANDARD)) {
 			return null;
 		}
 		return bean.getEdmTimespan();
 	}
 
 	@Override
 	public List<Map<String, String>> getEdmTimespanLabel() {
 		if (isProfile(Profile.MINIMAL)) {
 			return null;
 		}
 		return bean.getEdmTimespanLabel();
 	}
 
 	@Override
 	public String[] getEdmTimespanBegin() {
 		if (isProfile(Profile.MINIMAL)) {
 			return null;
 		}
 		return bean.getEdmTimespanBegin();
 	}
 
 	@Override
 	public String[] getEdmTimespanEnd() {
 		if (isProfile(Profile.MINIMAL)) {
 			return null;
 		}
 		return bean.getEdmTimespanEnd();
 	}
 
 	@Override
 	public String[] getEdmAgent() {
 		if (isProfile(Profile.MINIMAL) || isProfile(Profile.STANDARD)) {
 			return null;
 		}
 		return bean.getEdmAgent();
 	}
 
 	@Override
 	public List<Map<String, String>> getEdmAgentLabel() {
 		if (isProfile(Profile.MINIMAL) || isProfile(Profile.STANDARD)) {
 			return null;
 		}
 		return transformToMap(bean.getEdmAgentLabel());
 	}
 
 	@Override
 	public String[] getDctermsHasPart() {
 		// bean.getDctermsHasPart()
 		return null;
 	}
 
 	@Override
 	public String[] getDcCreator() {
 		return bean.getDcCreator();
 	}
 
 	@Override
 	public String[] getDcContributor() {
 		if (isProfile(Profile.MINIMAL) || isProfile(Profile.STANDARD)) {
 			return null;
 		}
 		return bean.getDcContributor();
 	}
 
 	@Override
 	public Date getTimestamp() {
 		// bean.getTimestamp()
 		return null;
 	}
 
 	@Override
 	public String getId() {
 		return bean.getId();
 	}
 
 	@Override
 	public Boolean isOptedOut() {
 		// bean.isOptedOut()
 		return null;
 	}
 
 	private String[] getThumbnails() {
 		if (thumbnails == null) {
 			List<String> thumbs = new ArrayList<String>();
 
 			if (!isOptedOut && bean.getEdmObject() != null) {
 				for (String object : bean.getEdmObject()) {
 					String tn = StringUtils.defaultIfBlank(object, "");
 					StringBuilder url = new StringBuilder(IMAGE_SITE);
 					try {
 						url.append(URI_PARAM).append(URLEncoder.encode(tn, "UTF-8"));
 					} catch (UnsupportedEncodingException e) {
 						e.printStackTrace();
 					}
 					url.append(SIZE_PARAM).append(ThumbSize.LARGE);
 					url.append(TYPE_PARAM).append(getType().toString());
 					thumbs.add(url.toString());
 				}
 			}
 			thumbnails = thumbs.toArray(new String[thumbs.size()]);
 		}
 		return thumbnails;
 	}
 
 	public String getLink() {
 		StringBuilder url = new StringBuilder(apiUrl);
 		url.append(RECORD_PATH).append(getId().substring(1)).append(RECORD_EXT);
 		url.append(WSKEY_PARAM).append(wskey);
 		return url.toString();
 	}
 
 	public String getGuid() {
 		StringBuilder url = new StringBuilder(portalUrl);
 		url.append(PORTAL_PATH).append(getId().substring(1));
 		url.append(PORTAL_PARAMS).append(wskey);
 		return url.toString();
 	}
 
 	@Override
 	public String[] getEdmPreview() {
 		// bean.getEdmPreview()
 		return getThumbnails();
 		// return edmPreview;
 	}
 
 	protected boolean isProfile(Profile _profile) {
		return profile.toLowerCase().equals(_profile.getName());
 	}
 
 	public static void setApiUrl(String _apiUrl) {
 		apiUrl = _apiUrl;
 		if (StringUtils.endsWith(apiUrl, "/")) {
 			apiUrl = StringUtils.chop(apiUrl);
 		}
 	}
 
 	public static void setPortalUrl(String _portalUrl) {
 		portalUrl = _portalUrl;
 	}
 
 	@SuppressWarnings("unchecked")
 	private List<Map<String, String>> transformToMap(List<Map<String, String>> fieldValues) {
 		if (fieldValues == null) {
 			return null;
 		}
 
 		List<Map<String, String>> list = new ArrayList<Map<String, String>>();
 		if (fieldValues.size() > 0) {
 			for (int i = 0, max = fieldValues.size(); i < max; i++) {
 				Object label = fieldValues.get(i);
 				if (label instanceof String) {
 					Map<String, String> map = new HashMap<String, String>();
 					map.put("def", (String) label);
 					list.add(map);
 				} else {
 					list.add((Map<String, String>) label);
 				}
 			}
 		}
 		return list;
 	}
 
 }
