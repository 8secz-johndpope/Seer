 package eu.europeana.portal2.web.presentation.model.data.decorators.contextual;
 
 import java.util.ArrayList;
 import java.util.List;
 import java.util.Map;
 
 import org.apache.commons.lang.StringUtils;
 import org.bson.types.ObjectId;
 
 import eu.europeana.corelib.definitions.solr.entity.ContextualClass;
 import eu.europeana.portal2.web.presentation.model.data.decorators.FullBeanDecorator;
 
 public class ContextualItemDecorator implements ContextualClass {
 
 	protected FullBeanDecorator fullBeanDecorator;
 	private ContextualClass item;
 	protected String userLanguage;
 	protected String edmLanguage;
 	protected boolean showInContext = false;
 	protected boolean matchPrefLabel = false;
 	protected boolean matchUrl = false;
 	private List<String> labels;
 	private boolean labelsInitialized = false;
 
 	ContextualItemDecorator(FullBeanDecorator fullBeanDecorator, ContextualClass item,
 			String userLanguage, String edmLanguage) {
 		this.fullBeanDecorator = fullBeanDecorator;
 		this.item = item;
 		this.userLanguage = userLanguage;
 		this.edmLanguage = edmLanguage;
 	}
 
 	@Override
 	public ObjectId getId() {
 		return item.getId();
 	}
 
 	@Override
 	public String getAbout() {
 		return item.getAbout();
 	}
 
 	@Override
 	public Map<String, List<String>> getPrefLabel() {
 		return item.getPrefLabel();
 	}
 
 	public List<String> getPrefLabelLang() {
 		return getLanguageVersion(item.getPrefLabel());
 	}
 
 	@Override
 	public Map<String, List<String>> getAltLabel() {
 		return item.getAltLabel();
 	}
 
 	public List<String> getAltLabelLang() {
 		return getLanguageVersion(item.getAltLabel());
 	}
 
 	@Override
 	public Map<String, List<String>> getHiddenLabel() {
 		return item.getHiddenLabel();
 	}
 
 	@Override
 	public Map<String, List<String>> getNote() {
 		return item.getNote();
 	}
 
 	public List<String> getNoteLang() {
 		return getLanguageVersion(item.getNote());
 	}
 
 	@Override
 	public void setId(ObjectId id) {}
 
 	@Override
 	public void setAbout(String about) {}
 
 	@Override
 	public void setPrefLabel(Map<String, List<String>> prefLabel) {}
 
 	@Override
 	public void setAltLabel(Map<String, List<String>> altLabel) {}
 
 	@Override
 	public void setHiddenLabel(Map<String, List<String>> hiddenLabel) {}
 
 	@Override
 	public void setNote(Map<String, List<String>> note) {}
 
 	public List<String> getLabels() {
 		if (labelsInitialized == false) {
 			labels = getLanguageVersion(item.getPrefLabel());
 			if (labels == null) {
 				labels = getLanguageVersion(item.getAltLabel());
 			}
 			labelsInitialized = true;
 		}
 		return labels;
 	}
 
 	public String getLabel() {
		return getLabels().size() > 0 ? getLabels().get(0) : null;
 	}
 
 	public List<String> getLanguageVersion(Map<String, List<String>> map) {
 		List<String> list = null;
 		if (map == null) {
 			return list;
 		}
 		if (map.containsKey(userLanguage)) {
 			list = map.get(userLanguage);
 		} else if (map.containsKey("en")) {
 			list = map.get("en");
 		} else if (map.containsKey(edmLanguage)) {
 			list = map.get(edmLanguage);
 		} else {
 			list = map.get(map.keySet().iterator().next());
 		}
 		return clearList(list);
 	}
 
 	public List<String> clearList(List<String> original) {
 		List<String> cleared = new ArrayList<String>();
 		for (String item : original) {
 			if (StringUtils.isNotBlank(item)) {
 				cleared.add(item);
 			}
 		}
 		return cleared;
 	}
 
 	public boolean isShowInContext() {
 		return showInContext;
 	}
 
 	public void setShowInContext(boolean showInContext) {
 		this.showInContext = showInContext;
 	}
 
 	public boolean isMatchPrefLabel() {
 		return matchPrefLabel;
 	}
 
 	public void setMatchPrefLabel(boolean matchPrefLabel) {
 		this.matchPrefLabel = matchPrefLabel;
 	}
 
 	public boolean isMatchUrl() {
 		return matchUrl;
 	}
 
 	public void setMatchUrl(boolean matchUrl) {
 		this.matchUrl = matchUrl;
 	}
 
 	private boolean hasLabel(String label, Map<String, List<String>> labels) {
 		if (labels == null) {
 			return false;
 		}
 		for (List<String> labelList : labels.values()) {
 			if (labelList == null) {
 				continue;
 			}
 			for (String storedLabel : labelList) {
 				if (StringUtils.equalsIgnoreCase(label, storedLabel)) {
 					return true;
 				}
 			}
 		}
 		return false;
 	}
 
 	public boolean hasPrefLabel(String label) {
 		return hasLabel(label, getPrefLabel());
 	}
 
 	public boolean hasAltLabel(String label) {
 		return hasLabel(label, getAltLabel());
 	}
 
 	public boolean hasAnyLabel(String label) {
 		return hasLabel(label, getPrefLabel()) || hasLabel(label, getAltLabel());
 	}
 }
