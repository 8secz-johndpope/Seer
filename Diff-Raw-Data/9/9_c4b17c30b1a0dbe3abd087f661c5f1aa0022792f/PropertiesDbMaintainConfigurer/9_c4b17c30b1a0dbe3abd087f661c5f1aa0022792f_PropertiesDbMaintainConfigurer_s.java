 /*
  * Copyright 2006-2007,  Unitils.org
  *
  * Licensed under the Apache License, Version 2.0 (the "License");
  * you may not use this file except in compliance with the License.
  * You may obtain a copy of the License at
  *
  *     http://www.apache.org/licenses/LICENSE-2.0
  *
  * Unless required by applicable law or agreed to in writing, software
  * distributed under the License is distributed on an "AS IS" BASIS,
  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  * See the License for the specific language governing permissions and
  * limitations under the License.
  */
 package org.dbmaintain.config;
 
 import static org.dbmaintain.config.DbMaintainProperties.PROPERTY_BACKSLASH_ESCAPING_ENABLED;
 import static org.dbmaintain.config.DbMaintainProperties.PROPERTY_CLEANDB_ENABLED;
 import static org.dbmaintain.config.DbMaintainProperties.PROPERTY_DISABLE_CONSTRAINTS_ENABLED;
 import static org.dbmaintain.config.DbMaintainProperties.PROPERTY_FROM_SCRATCH_ENABLED;
 import static org.dbmaintain.config.DbMaintainProperties.PROPERTY_IDENTIFIER_QUOTE_STRING;
 import static org.dbmaintain.config.DbMaintainProperties.PROPERTY_LOWEST_ACCEPTABLE_SEQUENCE_VALUE;
 import static org.dbmaintain.config.DbMaintainProperties.PROPERTY_PATCH_ALLOWOUTOFSEQUENCEEXECUTION;
 import static org.dbmaintain.config.DbMaintainProperties.PROPERTY_POSTPROCESSINGSCRIPTS_DIRNAME;
 import static org.dbmaintain.config.DbMaintainProperties.PROPERTY_PRESERVE_DATA_SCHEMAS;
 import static org.dbmaintain.config.DbMaintainProperties.PROPERTY_PRESERVE_DATA_TABLES;
 import static org.dbmaintain.config.DbMaintainProperties.PROPERTY_PRESERVE_MATERIALIZED_VIEWS;
 import static org.dbmaintain.config.DbMaintainProperties.PROPERTY_PRESERVE_SCHEMAS;
 import static org.dbmaintain.config.DbMaintainProperties.PROPERTY_PRESERVE_SEQUENCES;
 import static org.dbmaintain.config.DbMaintainProperties.PROPERTY_PRESERVE_SYNONYMS;
 import static org.dbmaintain.config.DbMaintainProperties.PROPERTY_PRESERVE_TABLES;
 import static org.dbmaintain.config.DbMaintainProperties.PROPERTY_PRESERVE_TRIGGERS;
 import static org.dbmaintain.config.DbMaintainProperties.PROPERTY_PRESERVE_TYPES;
 import static org.dbmaintain.config.DbMaintainProperties.PROPERTY_PRESERVE_VIEWS;
 import static org.dbmaintain.config.DbMaintainProperties.PROPERTY_SCRIPT_ENCODING;
 import static org.dbmaintain.config.DbMaintainProperties.PROPERTY_SCRIPT_EXTENSIONS;
 import static org.dbmaintain.config.DbMaintainProperties.PROPERTY_SCRIPT_LOCATIONS;
 import static org.dbmaintain.config.DbMaintainProperties.PROPERTY_SCRIPT_PATCH_QUALIFIERS;
 import static org.dbmaintain.config.DbMaintainProperties.PROPERTY_SCRIPT_QUALIFIER_PREFIX;
 import static org.dbmaintain.config.DbMaintainProperties.PROPERTY_SCRIPT_TARGETDATABASE_PREFIX;
 import static org.dbmaintain.config.DbMaintainProperties.PROPERTY_STORED_IDENTIFIER_CASE;
 import static org.dbmaintain.config.DbMaintainProperties.PROPERTY_UPDATE_SEQUENCES_ENABLED;
 import static org.dbmaintain.config.DbMaintainProperties.PROPERTY_USESCRIPTFILELASTMODIFICATIONDATES;
 import org.apache.commons.dbcp.BasicDataSource;
 import org.apache.commons.logging.Log;
 import org.apache.commons.logging.LogFactory;
 import org.dbmaintain.DbMaintainer;
 import org.dbmaintain.clean.DBCleaner;
 import org.dbmaintain.clear.DBClearer;
 import static org.dbmaintain.config.DbMaintainProperties.*;
 import org.dbmaintain.dbsupport.DbSupport;
 import org.dbmaintain.dbsupport.SQLHandler;
 import org.dbmaintain.dbsupport.StoredIdentifierCase;
 import org.dbmaintain.executedscriptinfo.ExecutedScriptInfoSource;
 import org.dbmaintain.script.Script;
 import org.dbmaintain.script.ScriptContainer;
 import org.dbmaintain.script.ScriptRunner;
 import org.dbmaintain.script.ScriptSource;
 import org.dbmaintain.script.impl.FileSystemScriptContainer;
 import org.dbmaintain.script.impl.JarScriptContainer;
 import org.dbmaintain.scriptparser.ScriptParser;
 import org.dbmaintain.scriptparser.ScriptParserFactory;
 import org.dbmaintain.structure.ConstraintsDisabler;
 import org.dbmaintain.structure.SequenceUpdater;
 import org.dbmaintain.structure.impl.DefaultConstraintsDisabler;
 import org.dbmaintain.structure.impl.DefaultSequenceUpdater;
 import org.dbmaintain.util.DbItemIdentifier;
 import org.dbmaintain.util.DbMaintainException;
 import org.dbmaintain.util.ReflectionUtils;
 import static org.dbmaintain.util.ReflectionUtils.createInstanceOfType;
 
 import javax.sql.DataSource;
 import java.io.File;
 import java.text.DateFormat;
 import java.text.SimpleDateFormat;
 import java.util.*;
 
 /**
  * todo javadoc
  *
  * @author Filip Neven
  * @author Tim Ducheyne
  */
 public class PropertiesDbMaintainConfigurer {
 
     /* The logger instance for this class */
     private static final Log logger = LogFactory.getLog(PropertiesDbMaintainConfigurer.class);
 
 
     protected Properties configuration;
 
     protected SQLHandler sqlHandler;
 
     protected Map<String, DbSupport> nameDbSupportMap;
 
     protected DbSupport defaultDbSupport;
 
     /**
      * @param configuration
      * @param sqlHandler
      */
     public PropertiesDbMaintainConfigurer(Properties configuration, SQLHandler sqlHandler) {
         this.configuration = configuration;
         this.sqlHandler = sqlHandler;
     }
 
 
     /**
      * Constructor for PropertiesDbMaintainConfigurer.
      *
      * @param configuration
      * @param defaultDbSupport
      * @param nameDbSupportMap
      * @param sqlHandler
      */
     public PropertiesDbMaintainConfigurer(Properties configuration,
                                           DbSupport defaultDbSupport, Map<String, DbSupport> nameDbSupportMap, SQLHandler sqlHandler) {
         this.configuration = configuration;
         this.sqlHandler = sqlHandler;
         this.defaultDbSupport = defaultDbSupport;
         this.nameDbSupportMap = nameDbSupportMap;
     }
 
 
     public DbMaintainer createDbMaintainer() {
         ScriptRunner scriptRunner = createScriptRunner();
         ScriptSource scriptSource = createScriptSource();
         ExecutedScriptInfoSource executedScriptInfoSource = createExecutedScriptInfoSource();
 
         boolean cleanDbEnabled = PropertyUtils.getBoolean(PROPERTY_CLEANDB_ENABLED, configuration);
         boolean fromScratchEnabled = PropertyUtils.getBoolean(PROPERTY_FROM_SCRATCH_ENABLED, configuration);
        boolean keepRetryingAfterError = PropertyUtils.getBoolean(PROPKEY_KEEP_RETRYING_AFTER_ERROR_ENABLED, configuration);
         boolean updateSequencesEnabled = PropertyUtils.getBoolean(PROPERTY_UPDATE_SEQUENCES_ENABLED, configuration);
 
         DBCleaner dbCleaner = createDbCleaner();
         DBClearer dbClearer = createDbClearer();
         ConstraintsDisabler constraintsDisabler = createConstraintsDisabler();
         SequenceUpdater sequenceUpdater = createSequenceUpdater();
 
         Class<DbMaintainer> clazz = ConfigUtils.getConfiguredClass(DbMaintainer.class, configuration);
         return createInstanceOfType(clazz, false,
                 new Class<?>[]{ScriptRunner.class, ScriptSource.class, ExecutedScriptInfoSource.class, boolean.class, boolean.class,
                         boolean.class, boolean.class, boolean.class, DBClearer.class, DBCleaner.class, ConstraintsDisabler.class, SequenceUpdater.class},
                 new Object[]{scriptRunner, scriptSource, executedScriptInfoSource, fromScratchEnabled, keepRetryingAfterError, cleanDbEnabled,
                         disableConstraintsEnabled, updateSequencesEnabled, dbClearer, dbCleaner, constraintsDisabler, sequenceUpdater});
     }
 
 
     public ScriptRunner createScriptRunner() {
         ScriptParserFactory scriptParserFactory = createScriptParserFactory();
         Class<ScriptRunner> clazz = ConfigUtils.getConfiguredClass(ScriptRunner.class, configuration);
         return createInstanceOfType(clazz, false,
                 new Class<?>[]{ScriptParserFactory.class, DbSupport.class, Map.class, SQLHandler.class},
                 new Object[]{scriptParserFactory, getDefaultDbSupport(), getNameDbSupportMap(), sqlHandler});
     }
 
     public ScriptParserFactory createScriptParserFactory() {
         Map<String, Class<? extends ScriptParser>> databaseDialectScriptParserClassMap = new HashMap<String, Class<? extends ScriptParser>>();
         for (String databaseDialect : getDatabaseDialects()) {
             String scriptParserClassName = ConfigUtils.getConfiguredClassName(ScriptParser.class, configuration, databaseDialect);
             Class<? extends ScriptParser> scriptParserClass = ReflectionUtils.getClassWithName(scriptParserClassName);
             databaseDialectScriptParserClassMap.put(databaseDialect, scriptParserClass);
         }
         boolean backSlashEscapingEnabled = PropertyUtils.getBoolean(PROPERTY_BACKSLASH_ESCAPING_ENABLED, configuration);
 
         Class<ScriptParserFactory> clazz = ConfigUtils.getConfiguredClass(ScriptParserFactory.class, configuration);
 
         return createInstanceOfType(clazz, false,
                 new Class<?>[]{Map.class, boolean.class},
                 new Object[]{databaseDialectScriptParserClassMap, backSlashEscapingEnabled});
     }
 
 
     protected Set<String> getDatabaseDialects() {
         Set<String> dialects = new HashSet<String>();
         for (DbSupport dbSupport : getNameDbSupportMap().values()) {
             if (dbSupport != null) {
                 dialects.add(dbSupport.getDatabaseDialect());
             }
         }
         return dialects;
     }
 
 
     /**
      * @return
      */
     public ScriptSource createScriptSource() {
         boolean useScriptFileLastModificationDates = PropertyUtils.getBoolean(PROPERTY_USESCRIPTFILELASTMODIFICATIONDATES, configuration);
         boolean fixScriptOutOfSequenceExecutionAllowed = PropertyUtils.getBoolean(PROPERTY_PATCH_ALLOWOUTOFSEQUENCEEXECUTION, configuration);
         Set<String> scriptLocations = new HashSet<String>(PropertyUtils.getStringList(PROPERTY_SCRIPT_LOCATIONS, configuration));
         Set<String> scriptFileExtensions = new HashSet<String>(PropertyUtils.getStringList(PROPERTY_SCRIPT_EXTENSIONS, configuration));
 
         Set<ScriptContainer> scriptContainers = new HashSet<ScriptContainer>();
         for (String scriptLocation : scriptLocations) {
             scriptContainers.add(createScriptContainer(scriptLocation));
         }
 
         Class<ScriptSource> clazz = ConfigUtils.getConfiguredClass(ScriptSource.class, configuration);
         return createInstanceOfType(clazz, false, new Class<?>[]{Set.class, boolean.class, Set.class, boolean.class},
                 new Object[]{scriptContainers, useScriptFileLastModificationDates, scriptFileExtensions, fixScriptOutOfSequenceExecutionAllowed});
     }
 
 
     public JarScriptContainer createJarScriptContainer(List<Script> scripts) {
         Set<String> scriptFileExtensions = new HashSet<String>(PropertyUtils.getStringList(PROPERTY_SCRIPT_EXTENSIONS, configuration));
         String targetDatabasePrefix = PropertyUtils.getString(PROPERTY_SCRIPT_TARGETDATABASE_PREFIX, configuration);
         String qualifierPefix = PropertyUtils.getString(PROPERTY_SCRIPT_QUALIFIER_PREFIX, configuration);
         Set<String> patchQualifiers = new HashSet<String>(PropertyUtils.getStringList(PROPERTY_SCRIPT_PATCH_QUALIFIERS, configuration));
         String postProcessingScriptDirName = PropertyUtils.getString(PROPERTY_POSTPROCESSINGSCRIPTS_DIRNAME, configuration);
         String scriptEncoding = PropertyUtils.getString(PROPERTY_SCRIPT_ENCODING, configuration);
         return new JarScriptContainer(scripts, scriptFileExtensions, targetDatabasePrefix, qualifierPefix, patchQualifiers, postProcessingScriptDirName, scriptEncoding);
     }
 
 
     public ScriptContainer createScriptContainer(String scriptLocation) {
         if (scriptLocation.endsWith(".jar")) {
             return new JarScriptContainer(new File(scriptLocation));
         } else {
             Set<String> scriptFileExtensions = new HashSet<String>(PropertyUtils.getStringList(PROPERTY_SCRIPT_EXTENSIONS, configuration));
             String targetDatabasePrefix = PropertyUtils.getString(PROPERTY_SCRIPT_TARGETDATABASE_PREFIX, configuration);
             String qualifierPefix = PropertyUtils.getString(PROPERTY_SCRIPT_QUALIFIER_PREFIX, configuration);
             Set<String> patchQualifiers = new HashSet<String>(PropertyUtils.getStringList(PROPERTY_SCRIPT_PATCH_QUALIFIERS, configuration));
             String postProcessingScriptDirName = PropertyUtils.getString(PROPERTY_POSTPROCESSINGSCRIPTS_DIRNAME, configuration);
             String scriptEncoding = PropertyUtils.getString(PROPERTY_SCRIPT_ENCODING, configuration);
             return new FileSystemScriptContainer(new File(scriptLocation), scriptFileExtensions, targetDatabasePrefix, qualifierPefix, patchQualifiers, postProcessingScriptDirName, scriptEncoding);
         }
     }
 
 
     /**
      * @return
      */
     public ExecutedScriptInfoSource createExecutedScriptInfoSource() {
 
         boolean autoCreateExecutedScriptsTable = PropertyUtils.getBoolean(PROPERTY_AUTO_CREATE_DBMAINTAIN_SCRIPTS_TABLE, configuration);
         String executedScriptsTableName = getDefaultDbSupport().toCorrectCaseIdentifier(PropertyUtils.getString(PROPERTY_EXECUTED_SCRIPTS_TABLE_NAME, configuration));
         String fileNameColumnName = getDefaultDbSupport().toCorrectCaseIdentifier(PropertyUtils.getString(PROPERTY_FILE_NAME_COLUMN_NAME, configuration));
         int fileNameColumnSize = PropertyUtils.getInt(PROPERTY_FILE_NAME_COLUMN_SIZE, configuration);
         String fileLastModifiedAtColumnName = getDefaultDbSupport().toCorrectCaseIdentifier(PropertyUtils.getString(PROPERTY_FILE_LAST_MODIFIED_AT_COLUMN_NAME, configuration));
         String checksumColumnName = getDefaultDbSupport().toCorrectCaseIdentifier(PropertyUtils.getString(PROPERTY_CHECKSUM_COLUMN_NAME, configuration));
         int checksumColumnSize = PropertyUtils.getInt(PROPERTY_CHECKSUM_COLUMN_SIZE, configuration);
         String executedAtColumnName = getDefaultDbSupport().toCorrectCaseIdentifier(PropertyUtils.getString(PROPERTY_EXECUTED_AT_COLUMN_NAME, configuration));
         int executedAtColumnSize = PropertyUtils.getInt(PROPERTY_EXECUTED_AT_COLUMN_SIZE, configuration);
         String succeededColumnName = getDefaultDbSupport().toCorrectCaseIdentifier(PropertyUtils.getString(PROPERTY_SUCCEEDED_COLUMN_NAME, configuration));
         DateFormat timestampFormat = new SimpleDateFormat(PropertyUtils.getString(PROPERTY_TIMESTAMP_FORMAT, configuration));
         String targetDatabasePrefix = PropertyUtils.getString(PROPERTY_SCRIPT_TARGETDATABASE_PREFIX, configuration);
         String qualifierPrefix = PropertyUtils.getString(PROPERTY_SCRIPT_QUALIFIER_PREFIX, configuration);
         Set<String> patchQualifiers = new HashSet<String>(PropertyUtils.getStringList(PROPERTY_SCRIPT_PATCH_QUALIFIERS, configuration));
         String postProcessingScriptsDirname = PropertyUtils.getString(PROPERTY_POSTPROCESSINGSCRIPTS_DIRNAME, configuration);
 
         Class<ExecutedScriptInfoSource> clazz = ConfigUtils.getConfiguredClass(ExecutedScriptInfoSource.class, configuration);
         return createInstanceOfType(clazz, false, new Class<?>[]{boolean.class, String.class, String.class, int.class, String.class, String.class,
                 int.class, String.class, int.class, String.class, DateFormat.class, DbSupport.class, SQLHandler.class, String.class, String.class,
                 Set.class, String.class},
 
                 new Object[]{autoCreateExecutedScriptsTable, executedScriptsTableName, fileNameColumnName, fileNameColumnSize,
                         fileLastModifiedAtColumnName, checksumColumnName, checksumColumnSize,
                         executedAtColumnName, executedAtColumnSize, succeededColumnName, timestampFormat, getDefaultDbSupport(),
                         sqlHandler, targetDatabasePrefix, qualifierPrefix, patchQualifiers, postProcessingScriptsDirname});
     }
 
 
     public DBCleaner createDbCleaner() {
         Set<DbItemIdentifier> schemasToPreserve = getSchemasToPreserve(PROPERTY_PRESERVE_SCHEMAS);
         schemasToPreserve.addAll(getSchemasToPreserve(PROPERTY_PRESERVE_DATA_SCHEMAS));
 
         Set<DbItemIdentifier> tablesToPreserve = getItemsToPreserve(PROPERTY_PRESERVE_TABLES);
         tablesToPreserve.addAll(getItemsToPreserve(PROPERTY_PRESERVE_DATA_TABLES));
         String executedScriptsTableName = PropertyUtils.getString(PROPERTY_EXECUTED_SCRIPTS_TABLE_NAME, configuration);
         tablesToPreserve.add(DbItemIdentifier.getItemIdentifier(getDefaultDbSupport().getDefaultSchemaName(), executedScriptsTableName, getDefaultDbSupport()));
 
         Class<DBCleaner> clazz = ConfigUtils.getConfiguredClass(DBCleaner.class, configuration);
 
         return createInstanceOfType(clazz, false,
                 new Class<?>[]{Map.class, Set.class, Set.class, SQLHandler.class},
                 new Object[]{getNameDbSupportMap(), schemasToPreserve, tablesToPreserve, sqlHandler});
     }
 
 
     public DBClearer createDbClearer() {
         Set<DbItemIdentifier> schemasToPreserve = getSchemasToPreserve(PROPERTY_PRESERVE_SCHEMAS);
         Set<DbItemIdentifier> tablesToPreserve = getItemsToPreserve(PROPERTY_PRESERVE_TABLES);
         String executedScriptsTableName = PropertyUtils.getString(PROPERTY_EXECUTED_SCRIPTS_TABLE_NAME, configuration);
         tablesToPreserve.add(DbItemIdentifier.getItemIdentifier(getDefaultDbSupport().getDefaultSchemaName(), executedScriptsTableName, getDefaultDbSupport()));
         Set<DbItemIdentifier> viewsToPreserve = getItemsToPreserve(PROPERTY_PRESERVE_VIEWS);
         Set<DbItemIdentifier> materializedViewsToPreserve = getItemsToPreserve(PROPERTY_PRESERVE_MATERIALIZED_VIEWS);
         Set<DbItemIdentifier> synonymsToPreserve = getItemsToPreserve(PROPERTY_PRESERVE_SYNONYMS);
         Set<DbItemIdentifier> sequencesToPreserve = getItemsToPreserve(PROPERTY_PRESERVE_SEQUENCES);
         Set<DbItemIdentifier> triggersToPreserve = getItemsToPreserve(PROPERTY_PRESERVE_TRIGGERS);
         Set<DbItemIdentifier> typesToPreserve = getItemsToPreserve(PROPERTY_PRESERVE_TYPES);
 
         Class<DBClearer> clazz = ConfigUtils.getConfiguredClass(DBClearer.class, configuration);
         return createInstanceOfType(clazz, false,
                 new Class<?>[]{Map.class, Set.class, Set.class,
                         Set.class, Set.class, Set.class,
                         Set.class, Set.class, Set.class},
                 new Object[]{getNameDbSupportMap(), schemasToPreserve, tablesToPreserve,
                         viewsToPreserve, materializedViewsToPreserve, synonymsToPreserve,
                         sequencesToPreserve, triggersToPreserve, typesToPreserve});
     }
 
 
     /**
      * Gets the list of schemas to preserve. The case is correct if necessary. Quoting an identifier
      * makes it case sensitive. If requested, the identifiers will be qualified with the default schema name if no
      * schema name is used as prefix.
      *
      * @param propertyName The name of the property that defines the items, not null
      * @return The set of items, not null
      */
     protected Set<DbItemIdentifier> getSchemasToPreserve(String propertyName) {
         Set<DbItemIdentifier> result = new HashSet<DbItemIdentifier>();
         List<String> schemasToPreserve = PropertyUtils.getStringList(propertyName, configuration);
         for (String schemaToPreserve : schemasToPreserve) {
             DbItemIdentifier itemIdentifier = DbItemIdentifier.parseSchemaIdentifier(schemaToPreserve, getDefaultDbSupport(), getNameDbSupportMap());
             result.add(itemIdentifier);
         }
         return result;
     }
 
 
     /**
      * Gets the list of items to preserve. The case is correct if necesSary. Quoting an identifier
      * makes it case sensitive. If requested, the identifiers will be qualified with the default schema name if no
      * schema name is used as prefix.
      *
      * @param propertyName The name of the property that defines the items, not null
      * @return The set of items, not null
      */
     protected Set<DbItemIdentifier> getItemsToPreserve(String propertyName) {
         Set<DbItemIdentifier> result = new HashSet<DbItemIdentifier>();
         List<String> itemsToPreserve = PropertyUtils.getStringList(propertyName, configuration);
         for (String itemToPreserve : itemsToPreserve) {
             DbItemIdentifier itemIdentifier = DbItemIdentifier.parseItemIdentifier(itemToPreserve, getDefaultDbSupport(), getNameDbSupportMap());
             result.add(itemIdentifier);
         }
         return result;
     }
 
 
     public ConstraintsDisabler createConstraintsDisabler() {
         return new DefaultConstraintsDisabler(nameDbSupportMap.values());
     }
 
 
     public SequenceUpdater createSequenceUpdater() {
         long lowestAcceptableSequenceValue = PropertyUtils.getLong(PROPERTY_LOWEST_ACCEPTABLE_SEQUENCE_VALUE, configuration);
         return new DefaultSequenceUpdater(lowestAcceptableSequenceValue, getNameDbSupportMap().values());
     }
 
 
     public DbSupport createDefaultDbSupport() {
         BasicDataSource dataSource = createDefaultDataSource();
 
         String databaseDialect = PropertyUtils.getString(PROPERTY_DATABASE_START + '.' + PROPERTY_DIALECT_END, configuration);
         List<String> schemaNamesList = PropertyUtils.getStringList(PROPERTY_DATABASE_START + '.' + PROPERTY_SCHEMA_NAMES_END, configuration);
         String defaultSchemaName = schemaNamesList.get(0);
         Set<String> schemaNames = new HashSet<String>(schemaNamesList);
         String customIdentifierQuoteString = getCustomIdentifierQuoteString(databaseDialect);
         StoredIdentifierCase customStoredIdentifierCase = getCustomStoredIdentifierCase(databaseDialect);
 
         Class<DbSupport> clazz = ConfigUtils.getConfiguredClass(DbSupport.class, configuration, databaseDialect);
         return createInstanceOfType(clazz, false,
                 new Class<?>[]{String.class, DataSource.class, String.class, Set.class, SQLHandler.class, String.class, StoredIdentifierCase.class},
                 new Object[]{null, dataSource, defaultSchemaName, schemaNames, sqlHandler, customIdentifierQuoteString, customStoredIdentifierCase}
         );
     }
 
 
     /**
      * Returns the dbms specific {@link DbSupport} as configured in the given
      * <code>Configuration</code>.
      *
      * @param databaseName
      * @return The dbms specific instance of {@link DbSupport}, not null
      */
     public DbSupport createDbSupport(String databaseName) {
         BasicDataSource dataSource = createDataSource(databaseName);
 
         String databaseDialect = PropertyUtils.getString(PROPERTY_DATABASE_START + '.' + databaseName + '.' + PROPERTY_DIALECT_END, configuration);
         List<String> schemaNamesList = PropertyUtils.getStringList(PROPERTY_DATABASE_START + '.' + databaseName + '.' + PROPERTY_SCHEMA_NAMES_END, configuration);
         String defaultSchemaName = schemaNamesList.get(0);
         Set<String> schemaNames = new HashSet<String>(schemaNamesList);
 
         return createDbSupport(databaseName, databaseDialect, dataSource, defaultSchemaName, schemaNames);
     }
 
 
     /**
      * @param databaseName
      * @param databaseDialect
      * @param dataSource
      * @param defaultSchemaName
      * @param schemaNames
      * @return
      */
     public DbSupport createDbSupport(String databaseName, String databaseDialect, DataSource dataSource, String defaultSchemaName, Set<String> schemaNames) {
         String customIdentifierQuoteString = getCustomIdentifierQuoteString(databaseDialect);
         StoredIdentifierCase customStoredIdentifierCase = getCustomStoredIdentifierCase(databaseDialect);
 
         Class<DbSupport> clazz = ConfigUtils.getConfiguredClass(DbSupport.class, configuration, databaseDialect);
         return createInstanceOfType(clazz, false,
                 new Class<?>[]{String.class, DataSource.class, String.class, Set.class, SQLHandler.class, String.class, StoredIdentifierCase.class},
                 new Object[]{databaseName, dataSource, defaultSchemaName, schemaNames, sqlHandler, customIdentifierQuoteString, customStoredIdentifierCase}
         );
     }
 
 
     protected StoredIdentifierCase getCustomStoredIdentifierCase(String databaseDialect) {
         String storedIdentifierCasePropertyValue = PropertyUtils.getString(PROPERTY_STORED_IDENTIFIER_CASE + "." + databaseDialect, configuration);
         if ("lower_case".equals(storedIdentifierCasePropertyValue)) {
             return StoredIdentifierCase.LOWER_CASE;
         } else if ("upper_case".equals(storedIdentifierCasePropertyValue)) {
             return StoredIdentifierCase.UPPER_CASE;
         } else if ("mixed_case".equals(storedIdentifierCasePropertyValue)) {
             return StoredIdentifierCase.MIXED_CASE;
         } else if ("auto".equals(storedIdentifierCasePropertyValue)) {
             return null;
         }
         throw new DbMaintainException("Unknown value " + storedIdentifierCasePropertyValue + " for property " + PROPERTY_STORED_IDENTIFIER_CASE
                 + ". It should be one of lower_case, upper_case, mixed_case or auto.");
     }
 
 
     protected String getCustomIdentifierQuoteString(String databaseDialect) {
         String identifierQuoteStringPropertyValue = PropertyUtils.getString(PROPERTY_IDENTIFIER_QUOTE_STRING + '.' + databaseDialect, configuration);
         if ("none".equals(identifierQuoteStringPropertyValue)) {
             return "";
         }
         if (!"auto".equals(identifierQuoteStringPropertyValue)) {
             return identifierQuoteStringPropertyValue;
         }
         return null;
     }
 
 
     public BasicDataSource createDefaultDataSource() {
         String driverClassName = PropertyUtils.getString(PROPERTY_DATABASE_START + '.' + PROPERTY_DRIVERCLASSNAME_END, configuration);
         String url = PropertyUtils.getString(PROPERTY_DATABASE_START + '.' + PROPERTY_URL_END, configuration);
         String userName = PropertyUtils.getString(PROPERTY_DATABASE_START + '.' + PROPERTY_USERNAME_END, configuration);
         String password = PropertyUtils.getString(PROPERTY_DATABASE_START + '.' + PROPERTY_PASSWORD_END, "", configuration);
         return createDataSource(driverClassName, url, userName, password);
     }
 
 
     /**
      * @param databaseName
      * @return
      */
     public BasicDataSource createDataSource(String databaseName) {
         String driverClassName = PropertyUtils.getString(PROPERTY_DATABASE_START + '.' + databaseName + '.' + PROPERTY_DRIVERCLASSNAME_END, configuration);
         String url = PropertyUtils.getString(PROPERTY_DATABASE_START + '.' + databaseName + '.' + PROPERTY_URL_END, configuration);
         String userName = PropertyUtils.getString(PROPERTY_DATABASE_START + '.' + databaseName + '.' + PROPERTY_USERNAME_END, configuration);
         String password = PropertyUtils.getString(PROPERTY_DATABASE_START + '.' + databaseName + '.' + PROPERTY_PASSWORD_END, configuration);
         return createDataSource(driverClassName, url, userName, password);
     }
 
 
     public BasicDataSource createDataSource(String driverClassName, String url, String userName, String password) {
         logger.info("Creating data source. Driver: " + driverClassName + ", url: " + url + ", user: " + userName
                 + ", password: <not shown>");
 
         BasicDataSource dataSource = getNewDataSource();
         dataSource.setDriverClassName(driverClassName);
         dataSource.setUrl(url);
         dataSource.setUsername(userName);
         dataSource.setPassword(password);
         return dataSource;
     }
 
     /**
      * Returns a concrete instance of <code>BasicDataSource</code>. This
      * method may be overridden e.g. to return a mock instance for testing
      *
      * @return An instance of <code>BasicDataSource</code>
      */
     protected BasicDataSource getNewDataSource() {
         return new BasicDataSource();
     }
 
 
     protected void initDbSupports() {
         nameDbSupportMap = new HashMap<String, DbSupport>();
         List<String> databaseNames = PropertyUtils.getStringList(PROPERTY_DATABASE_NAMES, configuration);
         if (databaseNames.isEmpty()) {
             defaultDbSupport = createDefaultDbSupport();
             nameDbSupportMap.put(null, defaultDbSupport);
         } else {
             for (String databaseName : databaseNames) {
                 DbSupport dbSupport = null;
                 if (isDatabaseEnabled(databaseName)) {
                     dbSupport = createDbSupport(databaseName);
                 }
                 nameDbSupportMap.put(databaseName, dbSupport);
                 if (defaultDbSupport == null) {
                     defaultDbSupport = dbSupport;
                 }
             }
         }
     }
 
 
     /**
      * @param databaseName
      * @return
      */
     protected boolean isDatabaseEnabled(String databaseName) {
         return PropertyUtils.getBoolean(PROPERTY_DATABASE_START + '.' + databaseName + '.' + PROPERTY_ENABLED_END, true, configuration);
     }
 
 
     public DbSupport getDbSupport(String name) {
         if (name == null) {
             return getDefaultDbSupport();
         }
         DbSupport dbSupport = getNameDbSupportMap().get(name);
         if (dbSupport == null) {
             throw new DbMaintainException("No test database configured with the name '" + name + "'");
         }
         return dbSupport;
     }
 
 
     public DbSupport getDefaultDbSupport() {
         if (defaultDbSupport == null) {
             initDbSupports();
         }
         return defaultDbSupport;
     }
 
 
     public Map<String, DbSupport> getNameDbSupportMap() {
         if (nameDbSupportMap == null) {
             initDbSupports();
         }
         return nameDbSupportMap;
     }
 
 }
