 package de.metalcon.autocompleteServer.Create;
 
 import javax.servlet.ServletContext;
 
 import org.json.simple.JSONObject;
 
 public class ProcessCreateResponse {
 
 	private final ServletContext context;
 	private final JSONObject jsonResponse;
 
 	public ProcessCreateResponse(ServletContext context) {
 		this.context = context;
 		this.jsonResponse = new JSONObject();
 	}
 
 	// FIXME: bad JSON keys!
 
 	public void addQueryNameWarning(String querynameNotGiven) {
 		this.jsonResponse.put("Warning:queryName", querynameNotGiven);
 
 	}
 
 	public void addIndexNameWarning(String indexnameNotGiven) {
 		this.jsonResponse.put("Warning:indexName", indexnameNotGiven);
 	}
 
 	public void addError(String querynameNotGiven) {
 		this.jsonResponse.put("Error:queryName", querynameNotGiven);
 	}
 
 	/**
 	 * Adds the suggestion String to the container object. Expects the String to
 	 * be not NULL and correctly formatted.
 	 * 
 	 * @param suggestionString
 	 */
 	public void addSuggestStringToContainer(String suggestionString) {
 		this.jsonResponse.put("term", suggestionString);
 	}
 
 	public void addSuggestionKeyWarning(String suggestionKeyNotGiven) {
 		this.jsonResponse.put("Warning:suggestionKey", suggestionKeyNotGiven);
 	}
 
 	public void addDefaultIndexWarning(String indexnameNotGiven) {
 		this.jsonResponse.put("Warning:defaultIndex", indexnameNotGiven);
 	}
 
 	public void addNoImageWarning(String noImage) {
 		this.jsonResponse.put("Warning:noImage", noImage);
 	}
 
 	public JSONObject getResponse() {
 		return this.jsonResponse;
 	}
 	/*
 	 * public void addIndexToContainer(String indexName) {
 	 * this.container.setIndexName(indexName); }
 	 * 
 	 * public void addSuggestionKeyToContainer(String suggestionKey) {
 	 * this.container.setKey(suggestionKey); }
 	 * 
 	 * public void addWeightToContainer(Integer weight) {
 	 * this.container.setWeight(weight);
 	 * 
 	 * }
 	 */
 }
