 package it.eng.spagobi.engine.cockpit.api.crosstable;
 
 import it.eng.qbe.query.CriteriaConstants;
 import it.eng.qbe.query.WhereField;
 import it.eng.qbe.query.WhereField.Operand;
 import it.eng.qbe.serializer.SerializationManager;
 import it.eng.qbe.statement.AbstractStatement;
 import it.eng.spago.error.EMFUserError;
 import it.eng.spagobi.commons.bo.UserProfile;
 import it.eng.spagobi.commons.dao.DAOFactory;
 import it.eng.spagobi.engine.cockpit.api.AbstractCockpitEngineResource;
 import it.eng.spagobi.tools.dataset.bo.IDataSet;
 import it.eng.spagobi.tools.dataset.bo.JDBCDataSet;
 import it.eng.spagobi.tools.dataset.bo.JDBCHiveDataSet;
 import it.eng.spagobi.tools.dataset.common.behaviour.FilteringBehaviour;
 import it.eng.spagobi.tools.dataset.common.behaviour.SelectableFieldsBehaviour;
 import it.eng.spagobi.tools.dataset.common.datastore.DataStore;
 import it.eng.spagobi.tools.dataset.common.datastore.IDataStore;
 import it.eng.spagobi.tools.dataset.common.metadata.FieldMetadata;
 import it.eng.spagobi.tools.dataset.common.metadata.IFieldMetaData;
 import it.eng.spagobi.tools.dataset.common.metadata.IMetaData;
 import it.eng.spagobi.tools.dataset.common.metadata.MetaData;
 import it.eng.spagobi.tools.dataset.persist.DataSetTableDescriptor;
 import it.eng.spagobi.tools.dataset.persist.IDataSetTableDescriptor;
 import it.eng.spagobi.tools.datasource.bo.IDataSource;
 import it.eng.spagobi.tools.datasource.dao.IDataSourceDAO;
 import it.eng.spagobi.utilities.assertion.Assert;
 import it.eng.spagobi.utilities.database.temporarytable.TemporaryTable;
 import it.eng.spagobi.utilities.database.temporarytable.TemporaryTableManager;
 import it.eng.spagobi.utilities.database.temporarytable.TemporaryTableRecorder;
 import it.eng.spagobi.utilities.engines.EngineConstants;
 import it.eng.spagobi.utilities.engines.SpagoBIEngineRuntimeException;
 import it.eng.spagobi.utilities.exceptions.SpagoBIRuntimeException;
 import it.eng.spagobi.utilities.exceptions.SpagoBIServiceException;
 
 import java.util.ArrayList;
 import java.util.HashMap;
 import java.util.Iterator;
 import java.util.List;
 import java.util.Map;
 import java.util.Set;
 
 import javax.ws.rs.Consumes;
 import javax.ws.rs.GET;
 import javax.ws.rs.Path;
 import javax.ws.rs.Produces;
 import javax.ws.rs.core.MediaType;
 
 import org.apache.log4j.LogMF;
 import org.apache.log4j.Logger;
 import org.json.JSONArray;
 import org.json.JSONException;
 import org.json.JSONObject;
 import org.safehaus.uuid.UUIDGenerator;
 
 import com.jamonapi.Monitor;
 import com.jamonapi.MonitorFactory;
 
 /**
  * @author Alberto Alagna
  * 
  */
 @Path("/1.0/crosstab")
 public class StaticPivotResource extends AbstractCockpitEngineResource {
 
 	static private Logger logger = Logger.getLogger(StaticPivotResource.class);
 	
 	// INPUT PARAMETERS
 	private static final String CROSSTAB_DEFINITION = QbeEngineStaticVariables.CROSSTAB_DEFINITION;
 	
 	public static final String OUTPUT_TYPE = "OUTPUT_TYPE";
 	public enum OutputType {JSON, HTML};
 	
 	@GET
 	@Path("/")
 	@Consumes(MediaType.APPLICATION_JSON)
 	@Produces(MediaType.APPLICATION_JSON + "; charset=UTF-8")
 	public String getCrosstab(String jsonData){
 		logger.debug("IN");
 		try {
 			return createCrossTable(jsonData);
 		} catch(Throwable t) {
 			throw new SpagoBIServiceException(this.request.getPathInfo(), "An unexpected error occured while executing service", t);
 		} finally {			
 			logger.debug("OUT");
 		}	
 	}
 	
 	private String createCrossTable(String jsonData)  {				
 		
 		CrossTab crossTab;
 		IDataStore valuesDataStore = null;
 		CrosstabDefinition crosstabDefinition = null;
 		
 		Monitor totalTimeMonitor = null;
 		Monitor errorHitsMonitor = null;
 					
 		logger.debug("IN");
 		
 		String htmlCode = "";
 		
 		try {				
 			
 			totalTimeMonitor = MonitorFactory.start("WorksheetEngine.loadCrosstabAction.totalTime");
 			
 			JSONObject data = new JSONObject(jsonData);
 			
 			// start reading input parameters
 			
 			JSONObject crosstabDefinitionJSON = data.getJSONObject(CROSSTAB_DEFINITION);	
 			logger.debug("Parameter [" + crosstabDefinitionJSON + "] is equals to [" + crosstabDefinitionJSON.toString() + "]");
 			
 //			String sheetName = this.getAttributeAsString(SHEET);
 //			logger.debug("Parameter [" + SHEET + "] is equals to [" + sheetName + "]");
 						
 			JSONObject optionalFilters = data.getJSONObject(QbeEngineStaticVariables.FILTERS);
 			logger.debug("Parameter [" + QbeEngineStaticVariables.FILTERS + "] is equals to [" + optionalFilters + "]");
 			// end reading input parameters				
 			
 			String datasetLabel = data.getString("DATASET_LABEL");
 			IDataSet dataset = DAOFactory.getDataSetDAO().loadDataSetByLabel(datasetLabel);
 			
 			// persist dataset into temporary table	
 			IDataSetTableDescriptor descriptor = this.persistDataSet(dataset);
 						
 			// build SQL query against temporary table
 			List<WhereField> whereFields = new ArrayList<WhereField>();			
 
 			List<WhereField> temp = getOptionalFilters(optionalFilters);
 			whereFields.addAll(temp);
 
 			// deserialize crosstab definition
 			crosstabDefinition = (CrosstabDefinition) SerializationManager.deserialize(crosstabDefinitionJSON, "application/json", CrosstabDefinition.class);
 						
 			String worksheetQuery = null;
 			IDataSource dsForTheTemporaryTable = descriptor.getDataSource();
 			
 			worksheetQuery = this.buildSqlStatement(crosstabDefinition, descriptor, whereFields, dsForTheTemporaryTable);
 			// execute SQL query against temporary table
 			logger.debug("Executing query on temporary table : " + worksheetQuery);
 			valuesDataStore = this.executeWorksheetQuery(worksheetQuery, null, null, dataset);
 			LogMF.debug(logger, "Query on temporary table executed successfully; datastore obtained: {0}", valuesDataStore);
 			Assert.assertNotNull(valuesDataStore, "Datastore obatined is null!!");
 			/* since the datastore, at this point, is a JDBC datastore, 
 			* it does not contain information about measures/attributes, fields' name and alias...
 			* therefore we adjust its metadata
 			*/
 			this.adjustMetadata((DataStore) valuesDataStore, dataset, descriptor);
 			LogMF.debug(logger, "Adjusted metadata: {0}", valuesDataStore.getMetaData());
 			logger.debug("Decoding dataset ...");
 			this.applyOptions(valuesDataStore);
 			dataset.decode(valuesDataStore);
 			LogMF.debug(logger, "Dataset decoded: {0}", valuesDataStore);					
 			
 			// serialize crosstab
 			if(crosstabDefinition.isPivotTable()){
 				//load the crosstab for a crosstab widget (with headers, sum, ...)
 				if (crosstabDefinition.isStatic()) {
 					crossTab = new CrossTab(valuesDataStore, crosstabDefinition, null);
 				} else {
 					crossTab = new CrossTab(valuesDataStore, crosstabDefinition, null);
 				}
 			}else{
 				//load the crosstab data structure for all other widgets
 				crossTab= new CrossTab(valuesDataStore, crosstabDefinition);
 			}
 			
 			htmlCode = crossTab.getHTMLCrossTab(this.getLocale());//									
 									
 		} catch(Throwable t) {
 			errorHitsMonitor = MonitorFactory.start("WorksheetEngine.errorHits");
 			errorHitsMonitor.stop();			
 		} finally {
 			if (totalTimeMonitor != null) totalTimeMonitor.stop();
 			logger.debug("OUT");
 		}	
 		
 		return htmlCode;
 	}
 	
 	
 	public static List<WhereField> transformIntoWhereClauses(Map<String, List<String>> filters) throws JSONException {
 
 		List<WhereField> whereFields = new ArrayList<WhereField>();
 
 		Set<String> keys = filters.keySet();
 		Iterator<String> it = keys.iterator();
 		while (it.hasNext()) {
 			String aFilterName = it.next();
 			List<String> values = filters.get(aFilterName);
 			if (values != null && values.size() > 0) {
 				String operator = values.size() > 1 ? CriteriaConstants.IN : CriteriaConstants.EQUALS_TO;
 				Operand leftOperand = new Operand(new String[] {aFilterName}, null, AbstractStatement.OPERAND_TYPE_SIMPLE_FIELD, null, null);
 				String[] valuesArray = values.toArray(new String[0]);
 				Operand rightOperand = new Operand(valuesArray, null, AbstractStatement.OPERAND_TYPE_STATIC, null, null);
 				WhereField whereField = new WhereField(UUIDGenerator.getInstance().generateRandomBasedUUID().toString(), 
 						aFilterName, false, leftOperand, operator, rightOperand, "AND");
 
 				whereFields.add(whereField);
 			}
 		}
 
 		return whereFields;
 	}
 	
 	public static List<WhereField> transformIntoWhereClauses(JSONObject optionalUserFilters) throws JSONException {
 		String[] fields = JSONObject.getNames(optionalUserFilters);
 		List<WhereField> whereFields = new ArrayList<WhereField>();
 		for (int i = 0; i < fields.length; i++) {
 			String fieldName = fields[i];
 			Object valuesObject = optionalUserFilters.get(fieldName);
 			if(valuesObject instanceof JSONArray){
 				JSONArray valuesArray = optionalUserFilters.getJSONArray(fieldName);
 
 				// if the filter has some value
 				if (valuesArray.length() > 0) {
 					String[] values = new String[1];
 					values[0] = fieldName;
 
 					Operand leftOperand = new Operand(values, fieldName,
 							AbstractStatement.OPERAND_TYPE_SIMPLE_FIELD, values, values);
 
 					values = new String[valuesArray.length()];
 					for (int j = 0; j < valuesArray.length(); j++) {
 						values[j] = valuesArray.getString(j);
 					}
 
 					Operand rightOperand = new Operand(values, fieldName,
 							AbstractStatement.OPERAND_TYPE_STATIC, values, values);
 
 					String operator = "EQUALS TO";
 					if (valuesArray.length() > 1) {
 						operator = "IN";
 					}
 
 					whereFields.add(new WhereField("OptionalFilter" + i,
 							"OptionalFilter" + i, false, leftOperand, operator,
 							rightOperand, "AND"));
 				}
 			}else{
 				logger.debug("The values of the filter "+ fieldName +" are not a JSONArray but "+valuesObject);
 			}
 
 		}
 		return whereFields;
 	}
 
 	public IDataSetTableDescriptor persistDataSet(IDataSet dataset) {
 		
 		if (dataset.isPersisted() || dataset.isFlatDataset()) {
 			return getDescriptorFromDatasetMeta(dataset);
 		} else {
 //			String tableName = engineInstance.getTemporaryTableName();
 			return persistDataSetWithTemporaryTable(dataset, getTemporaryTableName());
 		}
 
 	}
 	
 	public List<WhereField> getOptionalFilters(JSONObject optionalUserFilters) throws JSONException {
 		if (optionalUserFilters != null) {
 			return transformIntoWhereClauses(optionalUserFilters);
 		} else {
 			return new ArrayList<WhereField>();
 		}
 	}
 	
 	/**
 	 * Build the sql statement to query the temporary table 
 	 * @param crosstabDefinition definition of the crosstab
 	 * @param descriptor the temporary table descriptor
 	 * @param dataSource the datasource
 	 * @param tableName the temporary table name
 	 * @return the sql statement to query the temporary table 
 	 */
 	protected String buildSqlStatement(CrosstabDefinition crosstabDefinition,
 			IDataSetTableDescriptor descriptor, List<WhereField> filters, IDataSource dataSource) {
 		return CrosstabQueryCreator.getCrosstabQuery(crosstabDefinition, descriptor, filters, dataSource);
 	}
 	
 	public IDataStore executeWorksheetQuery (String worksheetQuery, Integer start, Integer limit, IDataSet dataset) {
 
 		IDataStore dataStore = null;		
 
 		if (dataset.isFlatDataset() || dataset.isPersisted()) {
 			dataStore = useDataSetStrategy(worksheetQuery, dataset, start,
 					limit);
 		} else {
 			logger.debug("Using temporary table strategy....");
 			dataStore = useTemporaryTableStrategy(worksheetQuery, start, limit);
 		}
 
 		Assert.assertNotNull(dataStore, "The dataStore cannot be null");
 		logger.debug("Query executed succesfully");
 
 		Integer resultNumber = (Integer) dataStore.getMetaData().getProperty("resultNumber");
 		Assert.assertNotNull(resultNumber, "property [resultNumber] of the dataStore returned by queryTemporaryTable method of the class [" + TemporaryTableManager.class.getName()+ "] cannot be null");
 		logger.debug("Total records: " + resultNumber);			
 
 		UserProfile userProfile = (UserProfile)getEnv().get(EngineConstants.ENV_USER_PROFILE);
 		Integer maxSize = null; //QbeEngineConfig.getInstance().getResultLimit();
 		boolean overflow = maxSize != null && resultNumber >= maxSize;
 		if (overflow) {
 			logger.warn("Query results number [" + resultNumber + "] exceeds max result limit that is [" + maxSize + "]");			
 		}
 
 		return dataStore;
 	}
 	
 	protected void adjustMetadata(DataStore dataStore,IDataSet dataset,IDataSetTableDescriptor descriptor) {
 		adjustMetadata(dataStore, dataset, descriptor, null);
 	}
 	
 	public void applyOptions(IDataStore dataStore) {
 	
 //		IMetaData metadata = dataStore.getMetaData();
 //		int fieldsCount = metadata.getFieldCount();
 //		for (int i = 0 ; i < fieldsCount ; i++ ) {
 //			IFieldMetaData fieldMetadata = metadata.getFieldMeta(i);
 //			FieldOptions fieldOptions = options.getOptionsForFieldByFieldId(fieldMetadata.getName());
 //			if (fieldOptions != null) {
 //				// there are options for the field
 //				logger.debug("Field [name : " + fieldMetadata.getName() + " ; alias : " + fieldMetadata.getAlias() + "] has options set");
 //				Map properties = fieldMetadata.getProperties();
 //				List<FieldOption> list = fieldOptions.getOptions();
 //				Iterator<FieldOption> it = list.iterator();
 //				while (it.hasNext()) {
 //					FieldOption option = it.next();
 //					String name = option.getName();
 //					Object value = option.getValue();
 //					logger.debug("Putting option [name : " + name + " ; value : " + value + 
 //							"] into field [name : " + fieldMetadata.getName() + " ; alias : " + fieldMetadata.getAlias() + "]");
 //					properties.put(name, value);
 //				}
 //			} else {
 //				logger.debug("Field [name : " + fieldMetadata.getName() + " ; alias : " + fieldMetadata.getAlias() + "] has no options set");
 //			}
 //		}
 
 	}
 	
 	private IDataSetTableDescriptor getDescriptorFromDatasetMeta(IDataSet dataset){
 		logger.debug("Getting the TableDescriptor for the dataset with label [" + dataset.getLabel() + "]");
 		IDataSetTableDescriptor td = new DataSetTableDescriptor(dataset);
 		logger.debug("Table descriptor successully created : " + td);
 		return td;
 	}
 	
 	private String getTemporaryTableName() {
 //		logger.debug("IN");
 //		String temporaryTableNameRoot = (String) this.getEnv().get(SpagoBIConstants.TEMPORARY_TABLE_ROOT_NAME);
 //		logger.debug("Temporary table name root specified on the environment : [" + temporaryTableNameRoot + "]");
 //		// if temporaryTableNameRadix is not specified on the environment, create a new name using the user profile
 //		if (temporaryTableNameRoot == null) {
 //			logger.debug("Temporary table name root not specified on the environment, creating a new one using user identifier ...");
 //			UserProfile userProfile = (UserProfile) getEnv().get(EngineConstants.ENV_USER_PROFILE);
 //			temporaryTableNameRoot = userProfile.getUserId().toString();
 //		}
 //		logger.debug("Temporary table root name : [" + temporaryTableNameRoot + "]");
 //		String temporaryTableNameComplete = TemporaryTableManager.getTableName(temporaryTableNameRoot);
 //		logger.debug("Temporary table name : [" + temporaryTableNameComplete + "]. Putting it into the environment");
 //		this.getEnv().put(SpagoBIConstants.TEMPORARY_TABLE_NAME, temporaryTableNameComplete);
 //		logger.debug("OUT : temporaryTableName = [" + temporaryTableNameComplete + "]");
 //		this.temporaryTableName = temporaryTableNameComplete;
 		
 		return "TEMPORARY_TABLE";
 	}
 
 	private IDataSetTableDescriptor persistDataSetWithTemporaryTable(IDataSet dataset, String tableName){
 		// get temporary table name
 
 		logger.debug("Temporary table name is [" + tableName + "]");
 
 		// set all filters into dataset, because dataset's getSignature() and persist() methods may depend on them
 
 		Assert.assertNotNull(dataset, "The engine instance is missing the dataset!!");
 		Map<String, List<String>> filters = getFiltersOnDomainValues();
 		if (dataset.hasBehaviour(FilteringBehaviour.ID)) {
 			logger.debug("Dataset has FilteringBehaviour.");
 			FilteringBehaviour filteringBehaviour = (FilteringBehaviour) dataset.getBehaviour(FilteringBehaviour.ID);
 			logger.debug("Setting filters on domain values : " + filters);
 			filteringBehaviour.setFilters(filters);
 		}
 
 		if (dataset.hasBehaviour(SelectableFieldsBehaviour.ID)) {
 			logger.debug("Dataset has SelectableFieldsBehaviour.");
 			List<String> fields = getAllFields();
 			SelectableFieldsBehaviour selectableFieldsBehaviour = (SelectableFieldsBehaviour) dataset.getBehaviour(SelectableFieldsBehaviour.ID);
 			logger.debug("Setting list of fields : " + fields);
 			selectableFieldsBehaviour.setSelectedFields(fields);
 		}
 
 		String signature = dataset.getSignature();
 		logger.debug("Dataset signature : " + signature);
 		if (signature.equals(TemporaryTableManager.getLastDataSetSignature(tableName))) {
 			// signature matches: no need to create a TemporaryTable
 			logger.debug("Signature matches: no need to create a TemporaryTable");
 			return TemporaryTableManager.getLastDataSetTableDescriptor(tableName);
 		}
 
 		IDataSource dataSource = null;
 		
 		try{
 			IDataSourceDAO dataSourceDAO = DAOFactory.getDataSourceDAO();
 			dataSource = dataSourceDAO.loadDataSourceWriteDefault();
 		} catch (Throwable e) {
 			throw new SpagoBIEngineRuntimeException(e);
 		}
 		
 		//drop the temporary table if one exists
 		try {			
 			logger.debug("Signature does not match: dropping TemporaryTable " + tableName + " if it exists...");
 			TemporaryTableManager.dropTableIfExists(tableName, dataSource);
 		} catch (Exception e) {
 			logger.error("Impossible to drop the temporary table with name " + tableName, e);
 			throw new SpagoBIEngineRuntimeException("Impossible to drop the temporary table with name " + tableName, e);
 		}
 
 		IDataSetTableDescriptor td = null;
 
 		try {
 			logger.debug("Persisting dataset ...");
 			
 			td = dataset.persist(tableName, dataSource);
 			this.recordTemporaryTable(tableName, dataSource);
 			
 			/**
 			 * Do not remove comments from the following line: we cannot change
 			 * the datatset state, since we are only temporarily persisting the
 			 * dataset, but the dataset itself could change during next user
 			 * interaction (example: the user is using Qbe and he will change
 			 * the dataset itself). We will use TemporaryTableManager to store
 			 * this kind of information.
 			 * 
 			 * dataset.setDataSourceForReading(getEngineInstance().
 			 * getDataSourceForWriting()); dataset.setPersisted(true);
 			 * dataset.setPersistTableName(td.getTableName());
 			 */
 			
 			logger.debug("Dataset persisted");
 		} catch (Throwable t) {
 			logger.error("Error while persisting dataset", t);
 			throw new SpagoBIRuntimeException("Error while persisting dataset", t);
 		}
 		
 		logger.debug("Dataset persisted successfully. Table descriptor : " + td);
 		TemporaryTableManager.setLastDataSetSignature(tableName, signature);
 		TemporaryTableManager.setLastDataSetTableDescriptor(tableName, td);
 		return td;
 	}
 	
 	private IDataStore useDataSetStrategy(String worksheetQuery, IDataSet dataset, Integer start, Integer limit) {
 		IDataStore dataStore = null;
 
 		UserProfile userProfile = (UserProfile)getEnv().get(EngineConstants.ENV_USER_PROFILE);
 
 		logger.debug("Querying dataset's flat/persistence table: user [" + userProfile.getUserId() + "] (SQL): [" + worksheetQuery + "]");		
 
 		try {
 
 			logger.debug("SQL statement is [" + worksheetQuery + "]");
 			IDataSet newdataset;
 			if (dataset instanceof JDBCHiveDataSet) {
 				newdataset = new JDBCHiveDataSet();
 				((JDBCHiveDataSet) newdataset).setQuery(worksheetQuery);
 			} else {
 				newdataset = new JDBCDataSet();
 				((JDBCDataSet) newdataset).setQuery(worksheetQuery);
 			}
 
 			newdataset.setDataSource(dataset.getDataSourceForReading());
 			if (start == null && limit == null) {
 				newdataset.loadData();
 			} else {
 				newdataset.loadData(start, limit, -1);
 			}
 			dataStore = (DataStore) newdataset.getDataStore();
 			logger.debug("Data store retrieved successfully");
 			logger.debug("OUT");
 			return dataStore;
 		} catch (Exception e) {
 			logger.debug("Query execution aborted because of an internal exception");			
 			
 			throw new SpagoBIEngineRuntimeException(e);
 		}
 	}
 
 
 	private IDataStore useTemporaryTableStrategy(String worksheetQuery,Integer start, Integer limit) {
 
 		IDataSource dataSource = null;
 		
 		try{
 			IDataSourceDAO dataSourceDAO = DAOFactory.getDataSourceDAO();
 			dataSource = dataSourceDAO.loadDataSourceWriteDefault();
 		} catch (Throwable e) {
 			throw new SpagoBIEngineRuntimeException(e);
 		}
 		
 		IDataStore dataStore = null;
 
 		UserProfile userProfile = (UserProfile)getEnv().get(EngineConstants.ENV_USER_PROFILE);		
 
 		logger.debug("Querying temporary table: user [" + userProfile.getUserId() + "] (SQL): [" + worksheetQuery + "]");		
 
 		try {
 			dataStore = TemporaryTableManager.queryTemporaryTable(worksheetQuery, dataSource, start, limit);
 		} catch (Exception e) {
 			logger.debug("Query execution aborted because of an internal exception");
 			
 			throw new SpagoBIEngineRuntimeException(e);
 		}
 		return dataStore;
 	}
 	
 	protected void adjustMetadata(DataStore dataStore,
 			IDataSet dataset,
 			IDataSetTableDescriptor descriptor,
 			JSONArray fieldOptions) {
 
 		IMetaData dataStoreMetadata = dataStore.getMetaData();
 		IMetaData dataSetMetadata = dataset.getMetadata();
 		MetaData newdataStoreMetadata = new MetaData();
 		int fieldCount = dataStoreMetadata.getFieldCount();
 		for (int i = 0; i < fieldCount; i++) {
 			IFieldMetaData dataStoreFieldMetadata = dataStoreMetadata.getFieldMeta(i);
 			String columnName = dataStoreFieldMetadata.getName();
 			logger.debug("Column name : " + columnName);
 			String fieldName = descriptor.getFieldName(columnName);
 			logger.debug("Field name : " + fieldName);
 			int index = dataSetMetadata.getFieldIndex(fieldName);
 			logger.debug("Field index : " + index);
 			IFieldMetaData dataSetFieldMetadata = dataSetMetadata.getFieldMeta(index);
 			logger.debug("Field metadata : " + dataSetFieldMetadata);
 			FieldMetadata newFieldMetadata = new FieldMetadata();
 			String decimalPrecision = (String) dataSetFieldMetadata.getProperty(IFieldMetaData.DECIMALPRECISION);
 			if(decimalPrecision!=null){
 				newFieldMetadata.setProperty(IFieldMetaData.DECIMALPRECISION,decimalPrecision);
 			}
 			if(fieldOptions!=null){
 				addMeasuresScaleFactor(fieldOptions, dataSetFieldMetadata.getName(), newFieldMetadata);
 			}
 			newFieldMetadata.setAlias(dataSetFieldMetadata.getAlias());
 			newFieldMetadata.setFieldType(dataSetFieldMetadata.getFieldType());
 			newFieldMetadata.setName(dataSetFieldMetadata.getName());
 			newFieldMetadata.setType(dataStoreFieldMetadata.getType());
 			newdataStoreMetadata.addFiedMeta(newFieldMetadata);
 		}
 		newdataStoreMetadata.setProperties(dataStoreMetadata.getProperties());
 		dataStore.setMetaData(newdataStoreMetadata);
 	}
 	
 	public Map<String, List<String>> getFiltersOnDomainValues() {
 //		WorksheetEngineInstance engineInstance = this.getEngineInstance();
 //		WorkSheetDefinition workSheetDefinition = (WorkSheetDefinition) engineInstance.getAnalysisState();
 //		Map<String, List<String>> toReturn = null;
 //		try {
 //			toReturn = workSheetDefinition.getFiltersOnDomainValues();
 //		} catch (WrongConfigurationForFiltersOnDomainValuesException e) {
 //			throw new SpagoBIEngineServiceException(this.getActionName(), e.getMessage(), e);
 //		}
 		
 		Map<String, List<String>> toReturn = new HashMap();
 		
 		return toReturn;
 	}
 	
 	public List<String> getAllFields() {
		WorksheetEngineInstance engineInstance = this.getEngineInstance();
		WorkSheetDefinition workSheetDefinition = (WorkSheetDefinition) engineInstance.getAnalysisState();
		List<Field> fields = workSheetDefinition.getAllFields();
		Iterator<Field> it = fields.iterator();
 		List<String> toReturn = new ArrayList<String>();
		while (it.hasNext()) {
			Field field = it.next();
			toReturn.add(field.getEntityId());
		}
 		return toReturn;
 	}
 	
 	private void recordTemporaryTable(String tableName, IDataSource dataSource) {
 		String attributeName = TemporaryTableRecorder.class.getName();
 		TemporaryTableRecorder recorder = (TemporaryTableRecorder) this.getHttpSession().getAttribute(attributeName);
 		if (recorder == null) {
 			recorder = new TemporaryTableRecorder();
 		}
 		recorder.addTemporaryTable(new TemporaryTable(tableName, dataSource));
 		this.getHttpSession().setAttribute(attributeName, recorder);
 	}
 	
 	private void addMeasuresScaleFactor(JSONArray fieldOptions, String fieldId,
 			FieldMetadata newFieldMetadata) {
 		if (fieldOptions != null) {
 			for (int i = 0; i < fieldOptions.length(); i++) {
 				try {
 					JSONObject afield = fieldOptions.getJSONObject(i);
 					JSONObject aFieldOptions = afield
							.getJSONObject(WorkSheetSerializationUtils.WORKSHEETS_ADDITIONAL_DATA_FIELDS_OPTIONS_OPTIONS);
 					String afieldId = afield.getString("id");
 					String scaleFactor = aFieldOptions
							.optString(WorkSheetSerializationUtils.WORKSHEETS_ADDITIONAL_DATA_FIELDS_OPTIONS_SCALE_FACTOR);
 					if (afieldId.equals(fieldId) && scaleFactor != null) {
 						newFieldMetadata
 						.setProperty(
								WorkSheetSerializationUtils.WORKSHEETS_ADDITIONAL_DATA_FIELDS_OPTIONS_SCALE_FACTOR,
 								scaleFactor);
 						return;
 					}
 				} catch (Exception e) {
 					throw new RuntimeException(
 							"An unpredicted error occurred while adding measures scale factor",
 							e);
 				}
 			}
 		}
 	}
 }
