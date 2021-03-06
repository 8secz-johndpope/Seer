 /* $This file is distributed under the terms of the license in /doc/license.txt$ */
 
 package edu.cornell.mannlib.vitro.webapp.visualization.freemarker.entitycomparison;
 
 import java.util.ArrayList;
 import java.util.HashMap;
 import java.util.HashSet;
 import java.util.List;
 import java.util.Map;
 import java.util.Set;
 
 import org.apache.commons.lang.StringEscapeUtils;
 import org.apache.commons.lang.StringUtils;
 import org.apache.commons.logging.Log;
 import org.apache.commons.logging.LogFactory;
 
 import com.google.gson.Gson;
 import com.hp.hpl.jena.iri.IRI;
 import com.hp.hpl.jena.iri.IRIFactory;
 import com.hp.hpl.jena.iri.Violation;
 import com.hp.hpl.jena.query.DataSource;
 import com.hp.hpl.jena.query.QuerySolution;
 import com.hp.hpl.jena.query.ResultSet;
 import com.hp.hpl.jena.rdf.model.RDFNode;
 
 import edu.cornell.mannlib.vitro.webapp.ConfigurationProperties;
 import edu.cornell.mannlib.vitro.webapp.beans.Portal;
 import edu.cornell.mannlib.vitro.webapp.controller.VitroRequest;
 import edu.cornell.mannlib.vitro.webapp.controller.freemarker.responsevalues.ResponseValues;
 import edu.cornell.mannlib.vitro.webapp.controller.freemarker.responsevalues.TemplateResponseValues;
 import edu.cornell.mannlib.vitro.webapp.controller.visualization.freemarker.VisualizationFrameworkConstants;
 import edu.cornell.mannlib.vitro.webapp.controller.visualization.freemarker.DataVisualizationController;
 import edu.cornell.mannlib.vitro.webapp.visualization.constants.QueryFieldLabels;
 import edu.cornell.mannlib.vitro.webapp.visualization.constants.VOConstants;
 import edu.cornell.mannlib.vitro.webapp.visualization.exceptions.MalformedQueryParametersException;
 import edu.cornell.mannlib.vitro.webapp.visualization.freemarker.valueobjects.Entity;
 import edu.cornell.mannlib.vitro.webapp.visualization.freemarker.valueobjects.GenericQueryMap;
 import edu.cornell.mannlib.vitro.webapp.visualization.freemarker.valueobjects.JsonObject;
 import edu.cornell.mannlib.vitro.webapp.visualization.freemarker.valueobjects.SubEntity;
 import edu.cornell.mannlib.vitro.webapp.visualization.freemarker.visutils.GenericQueryRunner;
 import edu.cornell.mannlib.vitro.webapp.visualization.freemarker.visutils.QueryRunner;
 import edu.cornell.mannlib.vitro.webapp.visualization.freemarker.visutils.UtilityFunctions;
 import edu.cornell.mannlib.vitro.webapp.visualization.freemarker.visutils.VisualizationRequestHandler;
 
 public class EntityPublicationCountRequestHandler implements
 		VisualizationRequestHandler {
 	
 	private Log log = LogFactory.getLog(EntityPublicationCountRequestHandler.class.getName());
 
 	@Override
 	public ResponseValues generateStandardVisualization(
 			VitroRequest vitroRequest, Log log, DataSource dataSource)
 			throws MalformedQueryParametersException {
 
 		String entityURI = vitroRequest
 				.getParameter(VisualizationFrameworkConstants.INDIVIDUAL_URI_KEY);
 		
 		if(StringUtils.isNotBlank(entityURI)){
 		
 			QueryRunner<Entity> queryManager = new EntityPublicationCountQueryRunner(
 					entityURI, dataSource, log);
 			
 			Entity entity = queryManager.getQueryResult();
 	 
 			if(entity.getEntityLabel().equals("no-label")){
 	
 				return prepareStandaloneErrorResponse(vitroRequest,entityURI);
 			
 			} else {
 		
 				QueryRunner<Map<String, Set<String>>> queryManagerForsubOrganisationTypes = new EntitySubOrganizationTypesQueryRunner(
 						entityURI, dataSource, log);
 		
 				Map<String, Set<String>> subOrganizationTypesResult = queryManagerForsubOrganisationTypes
 						.getQueryResult();
 		
 				return prepareStandaloneResponse(vitroRequest, entity, entityURI,
 						subOrganizationTypesResult);
 			}
 		} else {
 			
 			String staffProvidedHighestLevelOrganization = ConfigurationProperties.getProperty("visualization.topLevelOrg");
 			
 			/*
 			 * First checking if the staff has provided highest level organization in deploy.properties
 			 * if so use to temporal graph vis.
 			 */
 			if (StringUtils.isNotBlank(staffProvidedHighestLevelOrganization)) {
 				
 				/*
 	        	 * To test for the validity of the URI submitted.
 	        	 */
 	        	IRIFactory iRIFactory = IRIFactory.jenaImplementation();
 	    		IRI iri = iRIFactory.create(staffProvidedHighestLevelOrganization);
 	            
 	    		if (iri.hasViolation(false)) {
 	            	
 	                String errorMsg = ((Violation) iri.violations(false).next()).getShortMessage();
 	                log.error("Highest Level Organization URI provided is invalid " + errorMsg);
 	                
 	            } else {
 	            	
 	    			QueryRunner<Entity> queryManager = new EntityPublicationCountQueryRunner(
 	    					staffProvidedHighestLevelOrganization, dataSource, log);
 	    			
 	    			Entity entity = queryManager.getQueryResult();
 	    			
 	    			if(entity.getEntityLabel().equals("no-label")){
 	    				
 	    				return prepareStandaloneErrorResponse(vitroRequest,staffProvidedHighestLevelOrganization);
 	    				
 	    			} else {	
 	    			
 						QueryRunner<Map<String, Set<String>>> queryManagerForsubOrganisationTypes = new EntitySubOrganizationTypesQueryRunner(
 								staffProvidedHighestLevelOrganization, dataSource, log);
 						
 						Map<String, Set<String>> subOrganizationTypesResult = queryManagerForsubOrganisationTypes
 						.getQueryResult();
 						
 						return prepareStandaloneResponse(vitroRequest, entity, staffProvidedHighestLevelOrganization,
 								subOrganizationTypesResult);
 	    			}
 	            }
 			}
 			
 			Map<String, String> fieldLabelToOutputFieldLabel = new HashMap<String, String>();
 			fieldLabelToOutputFieldLabel.put("organization", 
 											  QueryFieldLabels.ORGANIZATION_URL);
 			fieldLabelToOutputFieldLabel.put("organizationLabel", QueryFieldLabels.ORGANIZATION_LABEL);
 			
 			String aggregationRules = "(count(?organization) AS ?numOfChildren)";
 			
 			String whereClause = "?organization rdf:type foaf:Organization ; rdfs:label ?organizationLabel . \n"  
 									+ "OPTIONAL { ?organization core:hasSubOrganization ?subOrg } . \n"
 									+ "OPTIONAL { ?organization core:subOrganizationWithin ?parent } . \n"
 									+ "FILTER ( !bound(?parent) ). \n";
 			
 			String groupOrderClause = "GROUP BY ?organization ?organizationLabel \n" 
 										+ "ORDER BY DESC(?numOfChildren)\n" 
 										+ "LIMIT 1\n";
 			
 			QueryRunner<ResultSet> highestLevelOrganizationQueryHandler = 
 					new GenericQueryRunner(fieldLabelToOutputFieldLabel,
 											aggregationRules,
 											whereClause,
 											groupOrderClause,
 											dataSource, log);
 			
 			
 			String highestLevelOrgURI = getHighestLevelOrganizationURI(
 					highestLevelOrganizationQueryHandler.getQueryResult(),
 					fieldLabelToOutputFieldLabel);
 			
 			QueryRunner<Entity> queryManager = new EntityPublicationCountQueryRunner(
 					highestLevelOrgURI, dataSource, log);
 			
 			Entity entity = queryManager.getQueryResult();
 			
 			if(entity.getEntityLabel().equals("no-label")){
 				
 				return prepareStandaloneErrorResponse(vitroRequest,highestLevelOrgURI);
 				
 			} else {	
 			
 				QueryRunner<Map<String, Set<String>>> queryManagerForsubOrganisationTypes = new EntitySubOrganizationTypesQueryRunner(
 						highestLevelOrgURI, dataSource, log);
 				
 				Map<String, Set<String>> subOrganizationTypesResult = queryManagerForsubOrganisationTypes
 				.getQueryResult();
 				
 				return prepareStandaloneResponse(vitroRequest, entity, highestLevelOrgURI,
 						subOrganizationTypesResult);	
 			}
 		}
 	
 	}
 	
 
 	@Override
 	public Map<String, String> generateDataVisualization(
 			VitroRequest vitroRequest, Log log, DataSource dataSource)
 			throws MalformedQueryParametersException {
 
 		String entityURI = vitroRequest
 				.getParameter(VisualizationFrameworkConstants.INDIVIDUAL_URI_KEY);
 				
 		QueryRunner<Entity> queryManager = new EntityPublicationCountQueryRunner(
 				entityURI, dataSource, log);	
 		
 		Entity entity = queryManager.getQueryResult();
 
 		
 		QueryRunner<Map<String, Set<String>>> queryManagerForsubOrganisationTypes = new EntitySubOrganizationTypesQueryRunner(
 				entityURI, dataSource, log);
 		
 		Map<String, Set<String>> subOrganizationTypesResult = queryManagerForsubOrganisationTypes.getQueryResult();
 
 		return prepareDataResponse(entity, entity.getSubEntities(),subOrganizationTypesResult);
 
 	}
 	
 	
 	@Override
 	public Object generateAjaxVisualization(VitroRequest vitroRequest, Log log,
 			DataSource dataSource) throws MalformedQueryParametersException {
 		throw new UnsupportedOperationException("Entity Pub Count does not provide Ajax Response.");
 	}
 
 	/**
 	 * Provides response when json file containing the publication count over the
 	 * years is requested.
 	 * 
 	 * @param entity
 	 * @param subentities
 	 * @param subOrganizationTypesResult
 	 */
 	private Map<String, String> prepareDataResponse(Entity entity, Set<SubEntity> subentities,
 			Map<String, Set<String>> subOrganizationTypesResult) {
 
 		String entityLabel = entity.getEntityLabel();
 
 		/*
 		* To make sure that null/empty records for entity names do not cause any mischief.
 		* */
 		if (StringUtils.isBlank(entityLabel)) {
 			entityLabel = "no-organization";
 		}
 		
 		String outputFileName = UtilityFunctions.slugify(entityLabel)
 				+ "_publications-per-year" + ".csv";
 		
 		
 		Map<String, String> fileData = new HashMap<String, String>();
 		
 		fileData.put(DataVisualizationController.FILE_NAME_KEY, 
 					 outputFileName);
 		fileData.put(DataVisualizationController.FILE_CONTENT_TYPE_KEY, 
 					 "application/octet-stream");
 		fileData.put(DataVisualizationController.FILE_CONTENT_KEY, 
 				getEntityPublicationsPerYearCSVContent(subentities, subOrganizationTypesResult));
 		return fileData;
 }
 	
 	/**
 	 * 
 	 * @param vreq
 	 * @param valueObjectContainer
 	 * @return
 	 */
 	private TemplateResponseValues prepareStandaloneResponse(VitroRequest vreq,
 			Entity entity, String entityURI, Map<String, Set<String>> subOrganizationTypesResult) {
 
         Portal portal = vreq.getPortal();
         String standaloneTemplate = "entityComparisonOnPublicationsStandalone.ftl";
 		
         String jsonContent = "";
 		jsonContent = writePublicationsOverTimeJSON(vreq, entity.getSubEntities(), subOrganizationTypesResult);
 
 		
 
         Map<String, Object> body = new HashMap<String, Object>();
         body.put("portalBean", portal);
         body.put("title", "Temporal Graph Visualization");
         body.put("organizationURI", entityURI);
         body.put("organizationLabel", entity.getEntityLabel());
         body.put("jsonContent", jsonContent);
         
         return new TemplateResponseValues(standaloneTemplate, body);
         
 	}
 	
 	
 	private ResponseValues prepareStandaloneErrorResponse(
 			VitroRequest vitroRequest, String entityURI) {
 		
         Portal portal = vitroRequest.getPortal();
        String visualization = "ENTITY_PUB_COUNT";
        String standaloneTemplate = "entityComparisonErrorActivator.ftl";
         
         Map<String, Object> body = new HashMap<String, Object>();
         body.put("portalBean", portal);
         body.put("title", "Temporal Graph Visualization");
         body.put("organizationURI", entityURI);
        body.put("visualization", visualization);
 
         return new TemplateResponseValues(standaloneTemplate, body);
 
 	}
 	
 	
 	/**
 	 * function to generate a json file for year <-> publication count mapping
 	 * @param vreq 
 	 * @param subentities
 	 * @param subOrganizationTypesResult  
 	 */
 	private String writePublicationsOverTimeJSON(VitroRequest vreq, Set<SubEntity> subentities, Map<String, Set<String>> subOrganizationTypesResult) {
 
 		Gson json = new Gson();
 		Set<JsonObject> subEntitiesJson = new HashSet<JsonObject>();
 
 		for (SubEntity subentity : subentities) {
 			JsonObject entityJson = new JsonObject(
 					subentity.getIndividualLabel());
 
 			List<List<Integer>> yearPubCount = new ArrayList<List<Integer>>();
 
 			for (Map.Entry<String, Integer> pubEntry : UtilityFunctions
 					.getYearToPublicationCount(subentity.getDocuments())
 					.entrySet()) {
 
 				List<Integer> currentPubYear = new ArrayList<Integer>();
 				if (pubEntry.getKey().equals(VOConstants.DEFAULT_PUBLICATION_YEAR)) {
 					currentPubYear.add(-1);
 				} else {
 					currentPubYear.add(Integer.parseInt(pubEntry.getKey()));
 				}
 					
 				currentPubYear.add(pubEntry.getValue());
 				yearPubCount.add(currentPubYear);
 			}
 			
 			log.info("entityJson.getLabel() : " + entityJson.getLabel() + " subOrganizationTypesResult " + subOrganizationTypesResult.toString());
 
 			entityJson.setYearToActivityCount(yearPubCount);
 			entityJson.getOrganizationType().addAll(subOrganizationTypesResult.get(entityJson.getLabel()));
 			
 			entityJson.setEntityURI(subentity.getIndividualURI());
 			
 			boolean isPerson = vreq.getWebappDaoFactory().getIndividualDao().getIndividualByURI(subentity.getIndividualURI()).isVClass("http://xmlns.com/foaf/0.1/Person");
 			
 			if(isPerson){
 				entityJson.setVisMode("PERSON");
 			} else{
 				entityJson.setVisMode("ORGANIZATION");
 			}
 		//	setEntityVisMode(entityJson);
 			subEntitiesJson.add(entityJson);
 		}
 		
 		return json.toJson(subEntitiesJson);
 
 	}
 	
 	private String getEntityPublicationsPerYearCSVContent(Set<SubEntity> subentities, Map<String, Set<String>> subOrganizationTypesResult) {
 
 		StringBuilder csvFileContent = new StringBuilder();
 		
 		csvFileContent.append("Entity Name, Publication Count, Entity Type\n");
 		
 		for(SubEntity subEntity : subentities){
 			
 			csvFileContent.append(StringEscapeUtils.escapeCsv(subEntity.getIndividualLabel()));
 			csvFileContent.append(", ");
 			csvFileContent.append(subEntity.getDocuments().size());
 			csvFileContent.append(", ");
 			
 			StringBuilder joinedTypes = new StringBuilder();
 			
 			for(String subOrganizationType : subOrganizationTypesResult.get(subEntity.getIndividualLabel())){
 				joinedTypes.append(subOrganizationType + "; ");
 			}
 			
 			csvFileContent.append(StringEscapeUtils.escapeCsv(joinedTypes.toString()));
 			csvFileContent.append("\n");
 
 		}
 
 		return csvFileContent.toString();
 
 	}
 
 	private String getHighestLevelOrganizationURI(ResultSet resultSet,
 			   Map<String, String> fieldLabelToOutputFieldLabel) {
 
 		GenericQueryMap queryResult = new GenericQueryMap();
 		
 		
 		while (resultSet.hasNext())  {
 			QuerySolution solution = resultSet.nextSolution();
 			
 			
 			RDFNode organizationNode = solution.get(
 									fieldLabelToOutputFieldLabel
 											.get("organization"));
 			
 			if (organizationNode != null) {
 				queryResult.addEntry(fieldLabelToOutputFieldLabel.get("organization"), organizationNode.toString());
 
 				return organizationNode.toString();
 							
 			}
 			
 			RDFNode organizationLabelNode = solution.get(
 									fieldLabelToOutputFieldLabel
 											.get("organizationLabel"));
 			
 			if (organizationLabelNode != null) {
 				queryResult.addEntry(fieldLabelToOutputFieldLabel.get("organizationLabel"), organizationLabelNode.toString());
 			}
 			
 			RDFNode numberOfChildrenNode = solution.getLiteral("numOfChildren");
 			
 			if (numberOfChildrenNode != null) {
 				queryResult.addEntry("numOfChildren", String.valueOf(numberOfChildrenNode.asLiteral().getInt()));
 			}
 		}
 		
 		return "";
 	}
 	
 	
 }	
