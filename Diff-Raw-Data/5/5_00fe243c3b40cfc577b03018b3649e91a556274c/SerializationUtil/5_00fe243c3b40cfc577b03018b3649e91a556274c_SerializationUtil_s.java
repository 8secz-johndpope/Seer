 /**
  * Copyright 2008 The University of North Carolina at Chapel Hill
  *
  * Licensed under the Apache License, Version 2.0 (the "License");
  * you may not use this file except in compliance with the License.
  * You may obtain a copy of the License at
  *
  *         http://www.apache.org/licenses/LICENSE-2.0
  *
  * Unless required by applicable law or agreed to in writing, software
  * distributed under the License is distributed on an "AS IS" BASIS,
  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  * See the License for the specific language governing permissions and
  * limitations under the License.
  */
 package edu.unc.lib.dl.ui.util;
 
 import java.io.IOException;
 import java.text.ParseException;
 import java.util.Collection;
 import java.util.Map;
 import java.util.Map.Entry;
 
 import org.codehaus.jackson.JsonGenerationException;
 import org.codehaus.jackson.map.JsonMappingException;
 import org.codehaus.jackson.map.ObjectMapper;
 import org.slf4j.Logger;
 import org.slf4j.LoggerFactory;
 
 import edu.unc.lib.dl.acl.util.AccessGroupSet;
 import edu.unc.lib.dl.search.solr.model.BriefObjectMetadata;
 import edu.unc.lib.dl.search.solr.model.HierarchicalBrowseResultResponse;
 import edu.unc.lib.dl.search.solr.model.SearchResultResponse;
 import edu.unc.lib.dl.search.solr.model.Tag;
 import edu.unc.lib.dl.util.DateTimeUtil;
 
 public class SerializationUtil {
 	private static final Logger log = LoggerFactory.getLogger(SerializationUtil.class);
 
 	private static ObjectMapper jsonMapper = new ObjectMapper();
 	
 	public static String structureToJSON(HierarchicalBrowseResultResponse response, AccessGroupSet groups) {
 		StringBuilder result = new StringBuilder();
 		
 		result.append('{');
 		result.append("\"root\":");
 		structureStep(response.getRootNode(), groups, result, true);
 		result.append('}');
 		
 		return result.toString();
 	}
 	
 	private static void structureStep(HierarchicalBrowseResultResponse.ResultNode node, AccessGroupSet groups, StringBuilder result, boolean firstInTier) {
 		if (!firstInTier)
 			result.append(',');
 		result.append('{');
 		result.append("\"entry\":");
 		result.append(metadataToJSON(node.getMetadata(), groups));
 		if (node.getChildren().size() > 0){
 			result.append(", \"children\":[");
 			for (int i = 0; i < node.getChildren().size(); i++) {
 				structureStep(node.getChildren().get(i), groups, result, i == 0);
 			}
 			result.append(']');
 		}
 		if (node.getMetadata().getAncestorNames() != null && (node.getMetadata().getAncestorPath() == null || node.getMetadata().getAncestorPath().size() == 0)) {
 			result.append(',');
 			result.append("\"isTopLevel\":true");
 		}
 		result.append('}');
 	}
 	
 	public static String resultsToJSON(SearchResultResponse resultResponse, AccessGroupSet groups) {
 		StringBuilder result = new StringBuilder();
 		result.append('[');
 		boolean firstEntry = true;
 		for (BriefObjectMetadata metadata: resultResponse.getResultList()) {
 			if (firstEntry)
 				firstEntry = false;
 			else result.append(',');
 			result.append(metadataToJSON(metadata, groups));
 		}
 		result.append(']');
 		return result.toString();
 	}
 	
 	public static String metadataToJSON(BriefObjectMetadata metadata, AccessGroupSet groups) {
 		StringBuilder result = new StringBuilder();
 		result.append('{');
 		result.append("\"id\":\"").append(metadata.getId()).append('"');
 		if (metadata.getTitle() != null) {
 			result.append(',');
			result.append("\"title\":\"").append(metadata.getTitle().replace("\"", "\\\"")).append('"');
 		}
 		if (metadata.get_version_() != null) {
 			result.append(',');
 			result.append("\"_version_\":\"").append(metadata.get_version_()).append('"');
 		}
 		if (metadata.getStatus() != null) {
 			result.append(',');
 			result.append("\"status\":").append(joinArray(metadata.getStatus()));
 		}
 		if (metadata.getResourceType() != null) {
 			result.append(',');
 			result.append("\"type\":\"").append(metadata.getResourceType()).append('"');
 		}
 		if (metadata.getContentModel() != null && metadata.getContentModel().size() > 0) {
 			result.append(',');
 			result.append("\"models\":").append(joinArray(metadata.getContentModel()));
 		}
 		if (metadata.getCreator() != null) {
 			result.append(',');
 			result.append("\"creators\":").append(joinArray(metadata.getCreator()));
 		}
 		if (metadata.getDatastream() != null) {
 			result.append(',');
 			result.append("\"datastreams\":").append(joinArray(metadata.getDatastream()));
 		}
 		if (metadata.getTags() != null) {
 			result.append(',');
 			result.append("\"tags\":").append(joinTags(metadata.getTags()));
 		}
 		if (metadata.getCountMap() != null && metadata.getCountMap().size() > 0) {
 			result.append(',');
 			result.append("\"counts\":").append(joinMap(metadata.getCountMap()));
 		}
 		try {
 			if (metadata.getDateAdded() != null) {
 				String dateAdded = DateTimeUtil.formatDateToUTC(metadata.getDateAdded());
 				result.append(',');
 				result.append("\"dateAdded\":\"").append(dateAdded).append('"');
 			}
 			if (metadata.getDateUpdated() != null) {
 				String dateUpdated = DateTimeUtil.formatDateToUTC(metadata.getDateUpdated());
 				result.append(',');
 				result.append("\"dateUpdated\":\"").append(dateUpdated).append('"');
 			}
 		} catch (ParseException e) {
 			log.debug("Failed to parse date field for " + metadata.getId(), e);
 		}
 		if (groups != null && metadata.getAccessControlBean() != null) {
 			result.append(',');
 			result.append("\"permissions\":").append(joinArray(metadata.getAccessControlBean().getPermissionsByGroups(groups)));
 		}
 		result.append('}');
 		return result.toString();
 	}
 	
 	private static String joinArray(Collection<String> collection) {
 		StringBuilder result = new StringBuilder();
 		result.append('[');
 		for (String value : collection) {
 			if (result.length() > 1)
 				result.append(',');
			result.append('"').append(value.replace("\"", "\\\"")).append('"');
 		}
 		result.append(']');
 		return result.toString();
 	}
 	
 	private static String joinTags(Collection<Tag> collection) {
 		StringBuilder result = new StringBuilder();
 		result.append('[');
 		for (Tag value : collection) {
 			if (result.length() > 1)
 				result.append(',');
 			result.append("{\"label\":\"").append(value.getLabel()).append('"');
 			result.append(",\"text\":\"").append(value.getText()).append('"').append('}');
 		}
 		result.append(']');
 		return result.toString();
 	}
 	
 	private static String joinMap(Map<?, ?> map) {
 		StringBuilder result = new StringBuilder();
 		result.append('{');
 		for (Entry<?, ?> entry : map.entrySet()) {
 			if (result.length() > 1)
 				result.append(',');
 			result.append('"').append(entry.getKey().toString()).append('"').append(':');
 			if (entry.getValue() instanceof Number)
 				result.append(entry.getValue().toString());
 			else result.append('"').append(entry.getValue().toString()).append('"');
 		}
 		result.append('}');
 		return result.toString();
 	}
 	
 	public static String objectToJSON(Object object) {
 		try {
 			return jsonMapper.writeValueAsString(object);
 		} catch (JsonGenerationException e) {
 			log.error("Unable to serialize object of type " + object.getClass().getName() + " to json", e);
 		} catch (JsonMappingException e) {
 			log.error("Unable to serialize object of type " + object.getClass().getName() + " to json", e);
 		} catch (IOException e) {
 			log.error("Unable to serialize object of type " + object.getClass().getName() + " to json", e);
 		}
 		return "";
 	}
 }
