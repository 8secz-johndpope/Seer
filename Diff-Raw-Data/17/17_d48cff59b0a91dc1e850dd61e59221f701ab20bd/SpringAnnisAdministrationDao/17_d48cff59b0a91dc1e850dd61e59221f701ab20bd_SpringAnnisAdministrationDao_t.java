 package annis.administration;
 
 import java.io.BufferedReader;
 import java.io.File;
 import java.io.FileReader;
 import java.io.IOException;
 import java.sql.Connection;
 import java.sql.SQLException;
 import java.util.ArrayList;
 import java.util.Arrays;
 import java.util.Collections;
 import java.util.HashMap;
 import java.util.List;
 import java.util.Map;
 import java.util.logging.Level;
 
 import javax.sql.DataSource;
 
 import org.apache.commons.io.FileUtils;
 import org.apache.commons.lang.StringUtils;
 import org.apache.log4j.Logger;
 import org.postgresql.PGConnection;
 import org.springframework.beans.factory.annotation.Autowired;
 import org.springframework.core.io.FileSystemResource;
 import org.springframework.core.io.Resource;
 import org.springframework.dao.DataAccessException;
 import org.springframework.jdbc.core.JdbcOperations;
 import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
 import org.springframework.jdbc.core.namedparam.SqlParameterSource;
 import org.springframework.jdbc.core.simple.ParameterizedSingleColumnRowMapper;
 import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;
 import org.springframework.jdbc.datasource.DataSourceUtils;
 
 import annis.externalFiles.ExternalFileMgrDAO;
 import javax.activation.MimetypesFileTypeMap;
 
 /**
  * - Transaktionen
  * - Datenbank-Zugriffsrechte für verschiedene Methoden
  * - Reihenfolge der Aufrufe
  * - Skripte in $ANNIS_HOME/scripts
  */
 // FIXME: nothing in SpringAnnisAdministrationDao is tested
 public class SpringAnnisAdministrationDao {
 
 	private Logger log = Logger.getLogger(this.getClass());
 	
 	// helper object to store external files
 	private ExternalFileMgrDAO externalFileMgrDao;
 	
 	// external files path
 	private String externalFilesPath;
 	
 	// script path
 	private String scriptPath;
 	
 	// use Spring's JDBC support
 	private SimpleJdbcTemplate simpleJdbcTemplate;
 	private JdbcOperations jdbcOperations;
 	
 	// save the datasource to manually retrieve connections (needed for bulk-import)
 	private DataSource dataSource;
 	
 	/**
 	 * The name of the file and the relation containing the resolver information.
 	 */
 	private static final String FILE_RESOLVER_VIS_MAP= "resolver_vis_map";
 	
 	// tables imported from bulk files
 	// DO NOT CHANGE THE ORDER OF THIS LIST!  Doing so may cause foreign key failures during import.
 	private String[] importedTables = {
 			"corpus", "corpus_annotation",
 			"text", "node", "node_annotation", 
 			"component", "rank", "edge_annotation",
 			FILE_RESOLVER_VIS_MAP
 	};
 
   private String[] tablesToCopyManually = {
     	"corpus", "corpus_annotation",
 			"text", 
 			FILE_RESOLVER_VIS_MAP,
       "corpus_stats"
   };
 	
 	// tables created during import
 	private String[] createdTables = { "corpus_stats" };
 	
 	///// Subtasks of creating the database
 	
 	void dropDatabase(String database) {
 		String sql = "SELECT count(*) FROM pg_database WHERE datname = :database";
 		SqlParameterSource args = makeArgs().addValue("database", database);
 		int count = simpleJdbcTemplate.queryForInt(sql, args);
 		if (count != 0) {
 			log.debug("dropping existing database");
 			jdbcOperations.execute("DROP DATABASE " + database);
 		}
 	}
 
 	void dropUser(String username) {
 		String sql = "SELECT count(*) FROM pg_user WHERE usename = :username";
 		SqlParameterSource args = makeArgs().addValue("username", username);
 		int count = simpleJdbcTemplate.queryForInt(sql, args);
 		if (count != 0) {
 			log.debug("dropping existing user");
 			jdbcOperations.execute("DROP USER " + username);
 		} 
 	}
 
 	void createUser(String username, String password) {
 		log.info("creating user: " + username);
 		jdbcOperations.execute("CREATE USER " + username + " PASSWORD '" + password + "'");
 	}
 	
 	void createDatabase(String database) {
 		log.info("creating database: " + database + " ENCODING = 'UTF8' TEMPLATE template0");
 		jdbcOperations.execute("CREATE DATABASE " + database + " ENCODING = 'UTF8' TEMPLATE template0");
 	}
 
 	void installPlPgSql() {
 		log.info("installing stored procedure language plpgsql");
     try
     {
       jdbcOperations.execute("CREATE LANGUAGE plpgsql");
     }
     catch(Exception ex)
     {
       log.warn("plpqsql was already installed: " + ex.getMessage());
     }
 	}
 	
 	void createFunctionUniqueToplevelCorpusName() {
 		log.info("creating trigger function: unique_toplevel_corpus_name");
 		executeSqlFromScript("unique_toplevel_corpus_name.sql");
 	}
 	
 	void createSchema() {
 		log.info("creating Annis database schema");
 		executeSqlFromScript("schema.sql");
 	}
 	
 	void populateSchema() {
 		log.info("populating the schemas with default values");
 		bulkloadTableFromResource("resolver_vis_map", new FileSystemResource(new File(scriptPath, "resolver_vis_map.tab")));
     // update the sequence
     executeSqlFromScript("update_resolver_sequence.sql");
 	}
 
 	///// Subtasks of importing a corpus
 	
 	void dropIndexes() {
 		log.info("dropping indexes");
 		for (String index : listIndexesOnTables(allTables())) {
 			log.debug("dropping index: " + index);
 			jdbcOperations.execute("DROP INDEX " + index);
 		}
 	}
 
 	void createStagingArea(boolean useTemporary) {
 		log.info("creating staging area");
     MapSqlParameterSource args = makeArgs().addValue(":tmp", useTemporary ? "TEMPORARY" : "");
 		executeSqlFromScript("staging_area.sql", args);
 	}
 
 	void bulkImport(String path) 
 	{
 		log.info("bulk-loading data");
 		for (String table : importedTables)
 		{	
 			if (table.equalsIgnoreCase(FILE_RESOLVER_VIS_MAP))
 			{
 				try {
 					bulkloadTableFromResource(tableInStagingArea(table), new FileSystemResource(new File(path, table + ".tab")));
 				} catch (FileAccessException e) 
 				{}
 			}
 			else
 				bulkloadTableFromResource(tableInStagingArea(table), new FileSystemResource(new File(path, table + ".tab")));
 		}
   }
 
   void createStagingAreaIndexes()
   {
     log.info("creating indexes for staging area");
     executeSqlFromScript("indexes_staging.sql");
   }
 
 	
 	void computeTopLevelCorpus() {
 		log.info("computing top-level corpus");
 		executeSqlFromScript("toplevel_corpus.sql");
 	}
 
 	void importBinaryData(String path) {
 		log.info("importing binary data");
 		
 		// pattern marks binary data in the bulk files and corresponding regexp
 		String extFilePattern = "\\[ExtFile\\]";
 		String extFileRegExp = "^" + extFilePattern + ".+$";
 
 		// search for annotations that have binary data
 		String selectSql = "SELECT DISTINCT value FROM _node_annotation WHERE value ~ :extFileRegExp";
 		SqlParameterSource selectArgs = makeArgs().addValue("extFileRegExp", extFileRegExp);
 		List<String> list = simpleJdbcTemplate.query(selectSql, stringRowMapper(), selectArgs);
 
 		for (String externalData : list) {
 			// get rid of marker
 			String filename = externalData.replaceFirst(extFilePattern, "");
 
 			// copy file to extData directory
 			File file = new File(filename);
 			try {
 				// XXX: equivalent to new File(parent = path, child = filename)?
 				File src = new File(path + "/ExtData/" + filename);
 				File dst = new File(externalFilesPath + "/" + filename);
 				log.debug("copying '" + src + "' to '" + dst + "'");
 				FileUtils.copyFile(src, dst);
 			} catch (IOException e) {
 				throw new FileAccessException(e);
 			}
 			
 			// store reference in database
 			// XXX: mp3 mime type hard-coded
 			String name = file.getName();
       String mimeType = new MimetypesFileTypeMap().getContentType(file);
 			String branch = file.getParent();
 			// FIXME: operates directly on the main database, can result orphaned entries in extdata when import fails
 			log.debug("externalFileMgr: " + externalFileMgrDao);
 			long id = externalFileMgrDao.putExtFile(name, branch, mimeType);
 			log.debug("external file '" + filename + "' inserted with id " + id);
 			
 			// update annotation value, set name to audio:audioFile
 			String updateValueSql = "UPDATE _node_annotation SET value = :id, name = 'externalFile', namespace = 'external' WHERE value = :externalData";
 			SqlParameterSource updateArgs = makeArgs().addValue("id", id).addValue("externalData", externalData);
 			simpleJdbcTemplate.update(updateValueSql, updateArgs);
 		}
 		
 	}
 	
 	void computeLeftTokenRightToken() {
 		log.info("computing values for struct.left_token and struct.right_token");
 		executeSqlFromScript("left_token_right_token.sql");
 	}
 	
 	void computeLevel() {
 		log.info("computing values for rank.level");
 		executeSqlFromScript("level.sql");
 	}
 
 	
 	void computeCorpusStatistics() {
 		log.info("computing statistics for top-level corpus");
 		executeSqlFromScript("corpus_stats.sql");
 	}
 
 
   /**
    *
    * @return the new corpus ID
    */
 	long updateIds() {
 		log.info("updating IDs in staging area");
 		executeSqlFromScript("update_ids.sql");
     log.info("query for the new corpus ID");
     long result = jdbcOperations.queryForLong("SELECT MAX(toplevel_corpus) FROM _node");
     log.info("new corpus ID is " + result);
     return result;
 	}
 
   void updateCorpusStatsId(long corpusId)
   {
 		log.info("updating corpus ID in corpus_stat");
     jdbcOperations.update("UPDATE _corpus_stats SET id = " + corpusId);
 	}
 	
 	void applyConstraints() {
 		log.info("activating relational constraints");
 		executeSqlFromScript("constraints.sql");
 	}
 	
 	void insertCorpus() {
 		log.info("moving corpus from staging area to main db");
 		for (String table : tablesToCopyManually)
 		{	
 			int numOfEntries= jdbcOperations.queryForInt("SELECT COUNT(*) from "+tableInStagingArea(table));
 			if (numOfEntries > 0)
 			{
 				StringBuffer sql= new StringBuffer();
 				if (table.equalsIgnoreCase(FILE_RESOLVER_VIS_MAP))
 				{
 					sql.append("INSERT INTO ");
 					sql.append(table);
 					//FIXME DIRTY!!! find a better way instead of naming the column-names in code 
 					sql.append("(corpus, version, namespace, element, vis_type, display_name, \"order\", mappings)");
 					sql.append(" (SELECT * FROM ");
 					sql.append(tableInStagingArea(table) + ")");
 				}
 				else
 				{	
 					sql.append("INSERT INTO ");
 					sql.append(table);
 					sql.append(" (SELECT * FROM ");
 					sql.append(tableInStagingArea(table) + ")");
 				}
 				jdbcOperations.execute(sql.toString());
 			}
 		}
 	}
 	
 	void dropStagingArea() {
 		log.info("dropping staging area");
 		
 		// tables must be dropped in reverse order
 		List<String> tables = importedAndCreatedTables();
 		Collections.reverse(tables);
 		
 		for (String table : tables)
 			jdbcOperations.execute("DROP TABLE " + tableInStagingArea(table));
 
 	}
 	
 	void dropMaterializedTables() {
 		log.info("dropping materialized tables");
 
     jdbcOperations.execute("DROP TABLE facts");
 			
 	}
 
   void analyzeStagingTables()
   {
     for(String t : importedTables)
     {      
       log.info("analyzing " + t);
       jdbcOperations.execute("ANALYZE " + tableInStagingArea(t));
     }
   }
 
   void createAnnotations(long corpusID)
   {
     MapSqlParameterSource args = makeArgs().addValue(":id", corpusID);
     log.info("creating annotations table for corpus with ID " + corpusID);
     executeSqlFromScript("annotations.sql", args);
 
     log.info("indexing annotations table for corpus with ID " + corpusID);
     executeSqlFromScript("indexes_annotations.sql", args);
   }
 
   void analyzeFacts(long corpusID)
   {
     log.info("analyzing facts table for corpus with ID " + corpusID);
     jdbcOperations.execute("ANALYZE facts_" + corpusID);
   }
 
 	void createFacts(long corpusID) 
   {
 
     MapSqlParameterSource args = makeArgs().addValue(":id", corpusID);
 
     log.info("creating materialized facts table for corpus with ID " + corpusID);
     executeSqlFromScript("facts.sql", args);
 
     log.info("updating values in materialized facts table for corpus with ID " + corpusID);
     executeSqlFromScript("update_facts.sql", args);
 
     log.info("clustering materialized facts table for corpus with ID " + corpusID);
     executeSqlFromScript("cluster.sql", args);
 
     log.info("indexing the new facts table (corpus with ID " + corpusID + ")");
     executeSqlFromScript("indexes_facts.sql", args);
 
 	}
 	
 	void rebuildIndexes() {
 		log.info("creating general indexes");
 		executeSqlFromScript("indexes.sql");
 	}
 	
 	///// Other sub tasks
 	
 	List<Long> listToplevelCorpora() {
 		String sql = "SELECT id FROM corpus WHERE top_level = 'y'";
 		return simpleJdbcTemplate.query(sql, ParameterizedSingleColumnRowMapper.newInstance(Long.class));
 	}
 	
 	void deleteCorpora(List<Long> ids)
   {
 
     log.debug("recursivly deleting corpora: " + ids);
 		executeSqlFromScript("delete_corpus.sql", makeArgs().addValue(":ids", StringUtils.join(ids, ", ")));
 
     for(long l : ids)
     {
       log.debug("dropping facts table for corpus " + l);
       jdbcOperations.execute("DROP TABLE facts_" + l);
 //      log.debug("dropping node annotation table for corpus " + l);
 //      jdbcOperations.execute("DROP TABLE node_annotation_" + l);
 //      log.debug("dropping node table for corpus " + l);
 //      jdbcOperations.execute("DROP TABLE node_" + l);
     }
 
 	}
 	
 	List<Map<String, Object>> listCorpusStats() {
 		return simpleJdbcTemplate.queryForList("SELECT * FROM corpus_info ORDER BY name");
 	}
 	
 	List<Map<String, Object>> listTableStats() {
 		return simpleJdbcTemplate.queryForList("SELECT * FROM table_stats");
 	}
 
 	List<String> listUsedIndexes() {
 		log.info("retrieving list of used indexes");
 		return listIndexDefinitions(true);
 	}
 
 	List<String> listUnusedIndexes() {
 		log.info("retrieving list of unused indexes");
 		return listIndexDefinitions(false);
 	}
 	
 	///// Helpers
 	
 	private List<String> importedAndCreatedTables() {
 		List<String> tables = new ArrayList<String>();
 		tables.addAll(Arrays.asList(importedTables));
 		tables.addAll(Arrays.asList(createdTables));
 		return tables;
 	}
 	
 	private List<String> allTables() {
 		List<String> tables = new ArrayList<String>();
 		tables.addAll(Arrays.asList(importedTables));
 		tables.addAll(Arrays.asList(createdTables));
 		//tables.addAll(Arrays.asList(materializedTables));
 		return tables;
 	}
 	
 	// tables in the staging area have their names prefixed with "_"
 	private String tableInStagingArea(String table) {
 		return "_" + table;
 	}
 	
 	private MapSqlParameterSource makeArgs() {
 		return new MapSqlParameterSource();
 	}
 	
 	private ParameterizedSingleColumnRowMapper<String> stringRowMapper() {
 		return ParameterizedSingleColumnRowMapper.newInstance(String.class);
 	}
 
 	// reads the content from a resource into a string
 	@SuppressWarnings("unchecked")
 	private String readSqlFromResource(Resource resource, MapSqlParameterSource args) {
 		// XXX: uses raw type, what are the parameters to Map in MapSqlParameterSource?
 		Map parameters = args != null ? args.getValues() : new HashMap();
     BufferedReader reader = null;
 		try
     {
       StringBuilder sqlBuf = new StringBuilder();
 			reader = new BufferedReader(new FileReader(resource.getFile()));
 			for (String line = reader.readLine(); line != null; line = reader.readLine())
       {
 				sqlBuf.append(line).append("\n");
       }
       String sql = sqlBuf.toString();
 			for (Object placeHolder : parameters.keySet())
       {
 				String key = placeHolder.toString();
 				String value = parameters.get(placeHolder).toString();
 				log.debug("substitution for parameter '" + key + "' in SQL script: " + value);
 
         sql = sql.replaceAll(key, value);
 			}
 			return sql;
 		} 
     catch (IOException e)
     {
 			log.error("Couldn't read SQL script from resource file.", e);
 			throw new FileAccessException("Couldn't read SQL script from resource file.", e);
 		}
     finally
     {
       if(reader != null)
       {
         try
         {
           reader.close();
         }
         catch (IOException ex)
         {
           java.util.logging.Logger.getLogger(SpringAnnisAdministrationDao.class.getName()).log(Level.SEVERE, null, ex);
         }
       }
     }
 	}
 	
 	// executes an SQL script from $ANNIS_HOME/scripts
 	public void executeSqlFromScript(String script) {
 		executeSqlFromScript(script, null);
 	}
 
 	// executes an SQL script from $ANNIS_HOME/scripts, substituting the parameters found in args
 	public void executeSqlFromScript(String script, MapSqlParameterSource args) {
 		Resource resource = new FileSystemResource(new File(scriptPath, script));
 		log.debug("executing SQL script: " + resource.getFilename());
 		String sql = readSqlFromResource(resource, args);
 		jdbcOperations.execute(sql);
 	}
 
 	// bulk-loads a table from a resource
 	private void bulkloadTableFromResource(String table, Resource resource) {
 		log.debug("bulk-loading data from '" + resource.getFilename() + "' into table '" + table + "'");
 		String sql = "COPY " + table + " FROM STDIN WITH DELIMITER E'\t' NULL AS 'NULL'";
 
 		try {
 			// retrieve the currently open connection if running inside a transaction
       Connection con = DataSourceUtils.getConnection(dataSource);
 
       // Postgres JDBC4 8.4 driver now supports the copy API
       PGConnection pgCon = (PGConnection) con;
       pgCon.getCopyAPI().copyIn(sql, resource.getInputStream());
 
       DataSourceUtils.releaseConnection(con, dataSource);
       
 		} catch (SQLException e) {
 			throw new DatabaseAccessException(e);
 		} catch (IOException e) {
 			throw new FileAccessException(e);
 		}
 	}
 
 	// get a list of indexes on the imported and created tables tables which are not
 	// auto-created by postgres (namely, primary key and unique constraints)
 	// exploits the fact that the index has the same name as the constraint
 	private List<String> listIndexesOnTables(List<String> tables) {
 		String sql = "" +
 				"SELECT indexname " +
 				"FROM pg_indexes " +
 				"WHERE tablename IN ( :tables ) " +
 				"AND lower(indexname) NOT IN " +
 				"	(SELECT lower(conname) FROM pg_constraint WHERE contype in ('p', 'u'))";
 		SqlParameterSource args = makeArgs().addValue("tables", tables);
 		return simpleJdbcTemplate.query(sql, stringRowMapper(), args);
 	}
 
 	private List<String> listIndexDefinitions(boolean used) {
 		return listIndexDefinitions(used, allTables());
 	}
 	
 	/*
 	 * Returns the CREATE INDEX statement for all indexes on the Annis tables,
 	 * that are not auto-created by PostgreSQL (primary keys and unique constraints).
 	 * 
 	 * @param used	If True, return used indexes.
 	 * 				If False, return unused indexes (scan count is 0).
 	 */
 	public List<String> listIndexDefinitions(boolean used, List<String> tables) {
 		String scansOp = used ? "!=" : "=";
 		String sql = 
 			"SELECT pg_get_indexdef(x.indexrelid) AS indexdef " +
 			"FROM pg_index x, pg_class c " +
 			"WHERE x.indexrelid = c.oid " +
 			"AND c.relname IN ( :indexes ) " +
 			"AND pg_stat_get_numscans(x.indexrelid) " + scansOp + " 0";
 		SqlParameterSource args = makeArgs().addValue("indexes", listIndexesOnTables(tables));
 		return simpleJdbcTemplate.query(sql, stringRowMapper(), args);
 	}
 	
 	private List<String> quotedArray(String... values) {
 		List<String> result = new ArrayList<String>();
 		for (String value : values) {
 			result.add("'" + value + "'");
 		}
 		return result;
 	}
 
 	public List<String> listIndexDefinitions(String... tables) {
 		String template = "" +
 				"SELECT pg_get_indexdef(x.indexrelid) AS indexdef " +
 				"FROM pg_index x, pg_class c, pg_indexes i " +
 				"WHERE x.indexrelid = c.oid " +
 				"AND c.relname = i.indexname " +
 				"AND i.tablename IN ( :tables )";
 		String sql = template.replaceAll(":tables", StringUtils.join(quotedArray(tables), ", "));
 		return simpleJdbcTemplate.query(sql, new ParameterizedSingleColumnRowMapper<String>());
 	}
 	
 	public List<String> listUsedIndexes(String... tables) {
 		String template = "" +
 		"SELECT pg_get_indexdef(x.indexrelid) AS indexdef " +
 		"FROM pg_index x, pg_class c, pg_indexes i " +
 		"WHERE x.indexrelid = c.oid " +
 		"AND c.relname = i.indexname " +
 		"AND i.tablename IN ( :tables ) " +
 		"AND pg_stat_get_numscans(x.indexrelid) != 0";
 		String sql = template.replaceAll(":tables", StringUtils.join(quotedArray(tables), ", "));
 		return simpleJdbcTemplate.query(sql, new ParameterizedSingleColumnRowMapper<String>());
 	}
 	
 	public boolean resetStatistics() {
 		try {
 			simpleJdbcTemplate.queryForList("SELECT pg_stat_reset()");
 			return true;
 		} catch (DataAccessException e) {
 			return false;
 		}
 	}
 	
 	///// Getter / Setter
 	
 	@Autowired
 	public void setDataSource(DataSource dataSource) {
 		this.dataSource = dataSource;
 		simpleJdbcTemplate = new SimpleJdbcTemplate(dataSource);
 		jdbcOperations = simpleJdbcTemplate.getJdbcOperations();
 	}
 
 	public String getScriptPath() {
 		return scriptPath;
 	}
 
 	public void setScriptPath(String scriptPath) {
 		this.scriptPath = scriptPath;
 	}
 
 	public String getExternalFilesPath() {
 		return externalFilesPath;
 	}
 
 	public void setExternalFilesPath(String externalFilesPath) {
 		this.externalFilesPath = externalFilesPath;
 	}
 
 	public ExternalFileMgrDAO getExternalFileMgrDao() {
 		return externalFileMgrDao;
 	}
 
 	public void setExternalFileMgrDao(ExternalFileMgrDAO externalFileMgrDao) {
 		this.externalFileMgrDao = externalFileMgrDao;
 	}
 
 }
