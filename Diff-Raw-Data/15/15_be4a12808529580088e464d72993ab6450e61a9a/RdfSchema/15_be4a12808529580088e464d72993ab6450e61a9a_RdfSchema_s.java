 package com.google.refine.rdf;
 
 import java.io.IOException;
 import java.net.URI;
 import java.util.ArrayList;
 import java.util.HashMap;
 import java.util.List;
 import java.util.Map;
 import java.util.Map.Entry;
 import java.util.Properties;
 
 import org.json.JSONArray;
 import org.json.JSONException;
 import org.json.JSONObject;
 import org.json.JSONWriter;
 import org.slf4j.Logger;
 import org.slf4j.LoggerFactory;
 
 import com.google.refine.model.OverlayModel;
 import com.google.refine.model.Project;
 import com.google.refine.rdf.ResourceNode.RdfType;
 import com.google.refine.rdf.app.ApplicationContext;
 import com.google.refine.rdf.vocab.PrefixExistException;
 import com.google.refine.rdf.vocab.VocabularyIndexException;
 
 public class RdfSchema implements OverlayModel {
 
 	final static Logger logger = LoggerFactory.getLogger("RdfSchema");
 	
     final protected List<Node> _rootNodes = new ArrayList<Node>();
     final protected List<ConstantBlankNode> _blanks = new ArrayList<ConstantBlankNode>();
     
     protected URI baseUri;
     /**
      * keys are the short name, values are the full URIs e.g. foaf --> http://xmlns.com/foaf/0.1/
      */
     protected Map<String,String> prefixesMap;
 
     public List<ConstantBlankNode> get_blanks() {
         return _blanks;
     }
     
     @Override
     public void onBeforeSave(Project project) {
     }
     
     @Override
     public void onAfterSave(Project project) {
     }
     
     
    @Override
     public void dispose(Project project) {
 	   /*try {
 			ApplicationContext.instance().getVocabularySearcher().deleteProjectVocabularies(String.valueOf(project.id));
 		} catch (ParseException e) {
 			//log
 			logger.error("Unable to delete index for project " + project.id, e);
 		} catch (IOException e) {
 			//log
 			logger.error("Unable to delete index for project " + project.id, e);
 		}*/
     }
 
     public void setBaseUri(URI baseUri) {
         this.baseUri = baseUri;
     }
 
     public RdfSchema(){
     	this.prefixesMap = new HashMap<String, String>();
     }
     
     public RdfSchema(ApplicationContext ctxt, Project project) throws VocabularyIndexException, IOException{
     	this(ctxt,project,true);
     }
     
     public RdfSchema(ApplicationContext ctxt, Project project, boolean getPredefinedPrefixes) throws VocabularyIndexException, IOException {
         // this value will never be used, just set
         this.baseUri = Util.buildURI("http://localhost:3333/");
         if(getPredefinedPrefixes){
         	this.prefixesMap = clone(ctxt.getPredefinedVocabularyManager().getPredefinedPrefixesMap());
         	//copy the index of predefined vocabularies
         	//each project will have its own copy of these predefined vocabs to enable delete, update ....
         	ctxt.getVocabularySearcher().addPredefinedVocabulariesToProject(project.id);
         }else{
         	this.prefixesMap = new HashMap<String, String>();
         }
     }
 
     public void addPrefix(String name, String uri) throws PrefixExistException{
     	if(this.prefixesMap.containsKey(name)){
     		throw new PrefixExistException(name + " already defined");
     	}
     	this.prefixesMap.put(name, uri);
     }
     
     public void removePrefix(String name){
     	this.prefixesMap.remove(name);
     }
     
     public URI getBaseUri() {
         return baseUri;
     }
 
     public Map<String, String> getPrefixesMap() {
 		return prefixesMap;
 	}
 
 	public List<Node> getRoots() {
         return _rootNodes;
     }
 
     static public RdfSchema reconstruct(JSONObject o) throws JSONException {
         RdfSchema s = new RdfSchema();
         s.baseUri = Util.buildURI(o.getString("baseUri"));
         
         JSONArray prefixesArr;
         //for backward compatibility
         if(o.has("prefixes")){
         	prefixesArr = o.getJSONArray("prefixes");
         }else{
         	prefixesArr = new JSONArray();
         }
         for (int i = 0; i < prefixesArr.length(); i++) {
         	JSONObject prefixObj = prefixesArr.getJSONObject(i);
         	s.prefixesMap.put(prefixObj.getString("name"),prefixObj.getString("uri"));
         }
         
         JSONArray rootNodes = o.getJSONArray("rootNodes");
         int count = rootNodes.length();
 
         for (int i = 0; i < count; i++) {
             JSONObject o2 = rootNodes.getJSONObject(i);
             Node node = reconstructNode(o2, s);
             if (node != null) {
                 s._rootNodes.add(node);
             }
         }
 
         return s;
     }
 
     static protected Node reconstructNode(JSONObject o, RdfSchema s)
             throws JSONException {
         Node node = null;
         int blanksCount = 0;
         String nodeType = o.getString("nodeType");
         if (nodeType.startsWith("cell-as-")) {
         	
         	boolean isRowNumberCell;
         	try{
         		isRowNumberCell = o.getBoolean("isRowNumberCell");
         	}catch(JSONException e){
             	//should never arrive here
             	//but for backward compatibility
         		isRowNumberCell = false;
             }        	
             String columnName = null;
             if(!isRowNumberCell){
             	columnName = o.getString("columnName");
             }
             if ("cell-as-resource".equals(nodeType)) {
                 String exp = o.getString("expression");
                 node = new CellResourceNode(columnName, exp,isRowNumberCell);
                 reconstructTypes((CellResourceNode)node,o);
             } else if ("cell-as-literal".equals(nodeType)) {
                String valueType = o.has("valueType")?Util.getDataType(o.getString("valueType")):null;
                 String lang = o.has("lang") ? o.getString("lang"):null;
                 String exp;
                 if (o.has("expression")){
                 	exp = o.getString("expression");
                 }else{
                 	//TODO backward compatibility 
                 	exp = "value";
                 }
                 node = new CellLiteralNode(columnName, exp, valueType, lang,isRowNumberCell);
             } else if ("cell-as-blank".equals(nodeType)) {
                 node = new CellBlankNode(columnName,isRowNumberCell);
                 reconstructTypes((CellBlankNode)node,o);
             }
         } else if ("resource".equals(nodeType)) {
             node = new ConstantResourceNode(o.getString("value"));
             reconstructTypes((ConstantResourceNode)node,o);
         } else if ("literal".equals(nodeType)) {
            String valueType = o.has("valueType")?Util.getDataType(o.getString("valueType")):null;
             String lang = o.has("lang") ? o.getString("lang"):null;
             node = new ConstantLiteralNode(o.getString("value"), valueType,lang);
         } else if ("blank".equals(nodeType)) {
             node = new ConstantBlankNode(blanksCount);
             blanksCount += 1;
             s._blanks.add((ConstantBlankNode) node);
             reconstructTypes((ConstantBlankNode)node,o);
         }
 
         if (node != null && node instanceof ResourceNode && o.has("links")) {
             ResourceNode node2 = (ResourceNode) node;
 
             JSONArray links = o.getJSONArray("links");
             int linkCount = links.length();
 
             for (int j = 0; j < linkCount; j++) {
                 JSONObject oLink = links.getJSONObject(j);
 
                 node2.addLink(new Link(oLink.getString("uri"), oLink.getString("curie"),oLink
                         .has("target")
                         && !oLink.isNull("target") ? reconstructNode(oLink
                         .getJSONObject("target"), s) : null));
             }
         }
 
         return node;
     }
 
     static private void reconstructTypes(ResourceNode node, JSONObject o)
             throws JSONException {
     	
     	if (o.has("rdfTypes")) {
     		JSONArray arr = o.getJSONArray("rdfTypes");
     		List<RdfType> types = new ArrayList<RdfType>();
             for (int i = 0; i < arr.length(); i++) {
                 String uri = arr.getJSONObject(i).getString("uri");
                 String curie = arr.getJSONObject(i).getString("curie");
                 types.add(new RdfType(uri, curie));
             }            
             node.setTypes(types);
         }
     }
 
     @Override
     public void write(JSONWriter writer, Properties options)
             throws JSONException {
         writer.object();
         writer.key("baseUri"); writer.value(baseUri);
         
         writer.key("prefixes");
         Util.writePrefixes(getPrefixesMap(), writer);
         
         writer.key("rootNodes");
         writer.array();
        	for (Node node : _rootNodes) {
         	node.write(writer, options);
        	}
 
        	writer.endArray();
         writer.endObject();
     }
     
     static public RdfSchema load(Project project, JSONObject obj) throws Exception {
         return reconstruct(obj);
     }
     
     private Map<String,String> clone(Map<String,String> original){
     	Map<String,String> copy = new HashMap<String, String>();
     	for(Entry<String, String> entry : original.entrySet()){
     		copy.put(entry.getKey(), entry.getValue());
     	}
     	
     	return copy;
     }
 }
